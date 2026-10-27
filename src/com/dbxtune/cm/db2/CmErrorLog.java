/*******************************************************************************
 * Copyright (C) 2010-2027 Goran Schwarz
 *
 * This file is part of DbxTune
 * DbxTune is a family of sub-products *Tune, hence the Dbx
 * Here are some of the tools: AseTune, IqTune, RsTune, RaxTune, HanaTune,
 *          SqlServerTune, PostgresTune, MySqlTune, MariaDbTune, Db2Tune, ...
 *
 * DbxTune is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, version 3 of the License.
 *
 * DbxTune is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with DbxTune.  If not, see <http://www.gnu.org/licenses/>.
 ******************************************************************************/
package com.dbxtune.cm.db2;

import java.lang.invoke.MethodHandles;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

import javax.naming.NameNotFoundException;

import org.apache.commons.text.StringEscapeUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.dbxtune.ICounterController;
import com.dbxtune.IGuiController;
import com.dbxtune.alarm.AlarmHandler;
import com.dbxtune.alarm.events.AlarmEvent;
import com.dbxtune.alarm.events.AlarmEventErrorLogEntry;
import com.dbxtune.cm.CmSettingsHelper;
import com.dbxtune.cm.CmSettingsHelper.RegExpInputValidator;
import com.dbxtune.cm.CounterSetTemplates;
import com.dbxtune.cm.CounterSetTemplates.Type;
import com.dbxtune.cm.CountersModel;
import com.dbxtune.cm.CountersModelAppend;
import com.dbxtune.cm.db2.gui.CmErrorLogPanel;
import com.dbxtune.config.dict.MonTablesDictionary;
import com.dbxtune.config.dict.MonTablesDictionaryManager;
import com.dbxtune.gui.MainFrame;
import com.dbxtune.gui.TabularCntrPanel;
import com.dbxtune.sql.conn.DbxConnection;
import com.dbxtune.sql.conn.info.DbmsVersionInfo;
import com.dbxtune.utils.Configuration;
import com.dbxtune.utils.StringUtil;

/**
 * The Db2 administration notification log (the ADMxxxx messages): log full, table space full, backup failed, HADR, archive failures...
 * <p>
 * The first sample gets the last ## hours, after that only what is new since the last row we have seen (the time string of the server, never converted).
 *
 * @author Goran Schwarz (goran_schwarz@hotmail.com)
 */
public class CmErrorLog
extends CountersModelAppend
{
	private static final Logger _logger = LogManager.getLogger(MethodHandles.lookup().lookupClass());
	private static final long    serialVersionUID = 1L;

	public static final String   CM_NAME          = CmErrorLog.class.getSimpleName();
	public static final String   SHORT_NAME       = "Errorlog";
	public static final String   HTML_DESC        =
		"<html>" +
		"The Db2 <b>administration notification log</b>: the ADMxxxx messages (log full, table space full, backup and archive failures, HADR, start/stop...).<br>" +
		"It is the notification part of db2diag.log, read with PD_GET_LOG_MSGS().<br>" +
		"<br>" +
		"The first sample gets the last 24 hours, after that only what is new since the previous sample.<br>" +
		"The log is per instance: messages for <i>other</i> databases in the same instance are shown, but they never raise an alarm here (only this database, and messages for the whole instance).<br>" +
		"<br>" +
		"<b>Note</b>: What is written depends on the instance configuration NOTIFYLEVEL (default 3: errors and warnings, plus some information).<br>" +
		"</html>";

	public static final String   GROUP_NAME       = MainFrame.TCP_GROUP_SERVER;
	public static final String   GUI_ICON_FILE    = "images/" + CM_NAME + ".png";

	public static final long     NEED_SRV_VERSION = 0;
	public static final long     NEED_CE_VERSION  = 0;

	public static final String[] MON_TABLES       = new String[] {"PDLOGMSGS_LAST24HOURS", CM_NAME};
	public static final String[] NEED_ROLES       = new String[] {};
	public static final String[] NEED_CONFIG      = new String[] {};

	public static final boolean  IS_SYSTEM_CM                   = true;
	public static final int      DEFAULT_POSTPONE_TIME          = 60;
	public static final int      DEFAULT_QUERY_TIMEOUT          = 30;

	@Override public int     getDefaultPostponeTime()                 { return DEFAULT_POSTPONE_TIME; }
	@Override public int     getDefaultQueryTimeout()                 { return DEFAULT_QUERY_TIMEOUT; }
	@Override public Type    getTemplateLevel()                       { return Type.SMALL; }

	/**
	 * FACTORY  method to create the object
	 */
	public static CountersModel create(ICounterController counterController, IGuiController guiController)
	{
		if (guiController != null && guiController.hasGUI())
			guiController.splashWindowProgress("Loading: Counter Model '" + CM_NAME + "'");

		return new CmErrorLog(counterController, guiController);
	}

	public CmErrorLog(ICounterController counterController, IGuiController guiController)
	{
		super(counterController, guiController, CM_NAME, GROUP_NAME, null, MON_TABLES, NEED_ROLES, NEED_CONFIG, NEED_SRV_VERSION, NEED_CE_VERSION, IS_SYSTEM_CM);

		setDisplayName(SHORT_NAME);
		setDescription(HTML_DESC);

		setIconFile(GUI_ICON_FILE);

		setCounterController(counterController);
		setGuiController(guiController);

		CounterSetTemplates.register(this);
	}


	//------------------------------------------------------------
	// Implementation
	//------------------------------------------------------------
	public static final String  PROPKEY_sample_firstSampleHours = CM_NAME + ".sample.firstSample.hours";
	public static final int     DEFAULT_sample_firstSampleHours = 24;

	/** Where we are in the log (the time of the last row we have seen), the next sample gets what's after this. It's a string straight from Db2 (the server's local time), so it is never converted */
	private String _lastLogTimeStr = null;

	@Override
	protected void registerDefaultValues()
	{
		super.registerDefaultValues();

		Configuration.registerDefaultValue(PROPKEY_sample_firstSampleHours, DEFAULT_sample_firstSampleHours);
	}

	/** Used by the: Create 'Offline Session' Wizard */
	@Override
	public List<CmSettingsHelper> getLocalSettings()
	{
		Configuration conf = Configuration.getCombinedConfiguration();
		List<CmSettingsHelper> list = new ArrayList<>();

		list.add(new CmSettingsHelper("First Sample, get last ## Hours", PROPKEY_sample_firstSampleHours, Integer.class, conf.getIntProperty(PROPKEY_sample_firstSampleHours, DEFAULT_sample_firstSampleHours), DEFAULT_sample_firstSampleHours, "The first sample (and the first after a reconnect) gets the notification log for the last ## hours. After that: only what is new since the previous sample." ));

		return list;
	}

	@Override
	protected TabularCntrPanel createGui()
	{
		return new CmErrorLogPanel(this);
	}

	@Override
	public void reset()
	{
		super.reset();

		// Start over (get the last ## hours) at next sample
		_lastLogTimeStr = null;
	}

	@Override
	public String[] getDependsOnConfigForVersion(DbxConnection conn, DbmsVersionInfo versionInfo)
	{
		return NEED_CONFIG;
	}

	/** PD_GET_LOG_MSGS() can be used by PUBLIC (tested on 11.5.8), but check it anyway */
	@Override
	public boolean checkDependsOnOther(DbxConnection conn)
	{
		return Db2CmUtils.checkFunctionsAreExecutable(this, conn, "PD_GET_LOG_MSGS(CURRENT TIMESTAMP - 1 HOUR)");
	}

	@Override
	public String getSqlForVersion(DbxConnection conn, DbmsVersionInfo versionInfo)
	{
		// ${startTime} and ${afterLastLogTime} are replaced in getSql()
		// NOTE: TIMESTAMP is the server's local time (TIMEZONE is the offset from UTC, in minutes). LOG_TIME_STR is the same as a string, so the next sample can continue exactly there
		// NOTE: MSG is a CLOB with trailing blanks and line breaks
		String sql = ""
			+ "select /* ${cmCollectorName} */ \n"
			+ "     TIMESTAMP \n"
			+ "    ,TIMEZONE \n"
			+ "    ,RTRIM(MSGTYPE) || CASE WHEN MSGNUM < 10000 THEN RIGHT('0000' || RTRIM(CHAR(MSGNUM)), 4) ELSE RTRIM(CHAR(MSGNUM)) END || MSGSEVERITY as MSG_ID \n"
			+ "    ,MSGSEVERITY \n"
			+ "    ,DBNAME \n"
			+ "    ,CAST(RTRIM(CAST(MSG AS VARCHAR(4096))) AS VARCHAR(4096)) as MESSAGE_TEXT \n"
			+ "    ,APPL_ID \n"
			+ "    ,COMPONENT \n"
			+ "    ,FUNCTION \n"
			+ "    ,PROBE \n"
			+ "    ,MSGNUM \n"
			+ "    ,MSGTYPE \n"
			+ "    ,INSTANCENAME \n"
			+ "    ,DBPARTITIONNUM \n"
			+ "    ,PID \n"
			+ "    ,PROCESSNAME \n"
			+ "    ,TID \n"
			+ "    ,CHAR(TIMESTAMP) as LOG_TIME_STR \n"
			+ "from table(PD_GET_LOG_MSGS(${startTime})) t \n"
			+ "where ${afterLastLogTime} \n"
			+ "order by TIMESTAMP \n"
			+ "";

		return sql;
	}

	@Override
	public String getSql()
	{
		String sqlText = super.getSql();
		if (sqlText == null)
			return null;

		// First sample (or the first after a reconnect): the last ## hours
		if (_lastLogTimeStr == null)
		{
			int hours = Configuration.getCombinedConfiguration().getIntProperty(PROPKEY_sample_firstSampleHours, DEFAULT_sample_firstSampleHours);
			return sqlText
					.replace("${startTime}"       , "CURRENT TIMESTAMP - " + hours + " HOURS")
					.replace("${afterLastLogTime}", "1 = 1");
		}

		// After that: what comes after the last row we have seen (the function reads the log from the given time, so it is cheap)
		// NOTE: There is no row number. Two messages with the same microsecond, where the second is written after we read the first, would be missed
		return sqlText
				.replace("${startTime}"       , "TIMESTAMP('" + _lastLogTimeStr + "') - 1 SECOND")
				.replace("${afterLastLogTime}", "TIMESTAMP > '" + _lastLogTimeStr + "'");
	}

	@Override
	public void hookInNearEndOfRefreshGetData()
	{
		super.hookInNearEndOfRefreshGetData();

		// Remember the last row in this sample (the rows are ordered by time)
		List<List<Object>> lastRefreshRows = getDataCollectionForLastRefresh();
		int pos_LogTimeStr = findColumn("LOG_TIME_STR");
		if (lastRefreshRows != null && ! lastRefreshRows.isEmpty() && pos_LogTimeStr >= 0)
		{
			Object lastLogTimeStr = lastRefreshRows.get(lastRefreshRows.size() - 1).get(pos_LogTimeStr);
			if (lastLogTimeStr != null)
				_lastLogTimeStr = lastLogTimeStr.toString().trim();
		}
		// NOTE: If there was nothing, the start point stays where it was (it is only moved forward while connected, and reset() starts over)
		//       The sample time is NOT used as a starting point: it's converted by the JDBC driver, and the JVM and the server may have different time zones
	}

	@Override
	public void addMonTableDictForVersion(DbxConnection conn, DbmsVersionInfo versionInfo)
	{
		try
		{
			MonTablesDictionary mtd = MonTablesDictionaryManager.getInstance();
			mtd.addTable(CM_NAME, "The Db2 administration notification log (ADMxxxx messages).");

			mtd.addColumn(CM_NAME, "TIMESTAMP",      "<html>When the message was written (the server's local time).</html>");
			mtd.addColumn(CM_NAME, "TIMEZONE",       "<html>The difference from UTC, in minutes.</html>");
			mtd.addColumn(CM_NAME, "MSG_ID",         "<html>The message id, for example <code>ADM1823E</code> (the active log is full). Look it up in the Db2 documentation, or with: <code>db2 ? ADM1823</code></html>");
			mtd.addColumn(CM_NAME, "MSGSEVERITY",    "<html><b>C</b> = Critical, <b>E</b> = Error, <b>W</b> = Warning, <b>I</b> = Information.<br><b>Alarm</b>: Error (C and E)</html>");
			mtd.addColumn(CM_NAME, "DBNAME",         "<html>The database the message is about. NULL = the whole instance.<br>Messages for other databases in the instance never raise an alarm here.</html>");
			mtd.addColumn(CM_NAME, "MESSAGE_TEXT",   "<html>The message.</html>");
			mtd.addColumn(CM_NAME, "APPL_ID",        "<html>The application that caused the message (if any).</html>");
			mtd.addColumn(CM_NAME, "COMPONENT",      "<html>The Db2 component that wrote the message.</html>");
			mtd.addColumn(CM_NAME, "FUNCTION",       "<html>The Db2 function that wrote the message.</html>");
			mtd.addColumn(CM_NAME, "PROBE",          "<html>Probe point in the Db2 function.</html>");
			mtd.addColumn(CM_NAME, "MSGNUM",         "<html>The message number.</html>");
			mtd.addColumn(CM_NAME, "MSGTYPE",        "<html>The message type: ADM (administration), DIA (diagnostic)...</html>");
			mtd.addColumn(CM_NAME, "INSTANCENAME",   "<html>The Db2 instance.</html>");
			mtd.addColumn(CM_NAME, "DBPARTITIONNUM", "<html>The database partition (member).</html>");
			mtd.addColumn(CM_NAME, "PID",            "<html>Operating system process id.</html>");
			mtd.addColumn(CM_NAME, "PROCESSNAME",    "<html>Operating system process name.</html>");
			mtd.addColumn(CM_NAME, "TID",            "<html>Operating system thread id.</html>");
			mtd.addColumn(CM_NAME, "LOG_TIME_STR",   "<html>Same as TIMESTAMP, as a string. (DbxTune uses this to know where to continue at the next sample)</html>");
		}
		catch (NameNotFoundException e)
		{
			_logger.warn("Problems in cm='" + CM_NAME + "', adding addMonTableDictForVersion. Caught: " + e);
		}
	}

	@Override
	public void sendAlarmRequest()
	{
		if ( ! AlarmHandler.hasInstance() )
			return;

		boolean debugPrint = Configuration.getCombinedConfiguration().getBooleanProperty("sendAlarmRequest.debug", _logger.isDebugEnabled());
		Configuration conf = Configuration.getCombinedConfiguration();

		// ABS Data from LAST refresh only
		List<List<Object>> lastRefreshRows = getDataCollectionForLastRefresh();

		// DO NOTHING on FIRST refresh
		if ( isFirstTimeSample() )
		{
			_logger.info("First time we check the notification log, Alarms will NOT be checked, because it will include old messages. (Rows in first refresh was " + (lastRefreshRows == null ? "-null-" : lastRefreshRows.size()) + ")");
			return;
		}

		// Exit early if NO ROWS
		if (lastRefreshRows == null || lastRefreshRows.isEmpty())
			return;

		int pos_Timestamp   = findColumn("TIMESTAMP");
		int pos_MsgId       = findColumn("MSG_ID");
		int pos_MsgSeverity = findColumn("MSGSEVERITY");
		int pos_DbName      = findColumn("DBNAME");
		int pos_Message     = findColumn("MESSAGE_TEXT");

		if (pos_Timestamp < 0 || pos_MsgId < 0 || pos_MsgSeverity < 0 || pos_DbName < 0 || pos_Message < 0)
		{
			_logger.error("When checking for alarms, could not find all columns. skipping this. [TIMESTAMP_pos=" + pos_Timestamp + ", MSG_ID_pos=" + pos_MsgId + ", MSGSEVERITY_pos=" + pos_MsgSeverity + ", DBNAME_pos=" + pos_DbName + ", MESSAGE_TEXT_pos=" + pos_Message + "]");
			return;
		}

		boolean checkError = isSystemAlarmsForColumnEnabledAndInTimeRange("Error") && conf.getBooleanProperty(PROPKEY_alarm_Error, DEFAULT_alarm_Error);
		if ( ! checkError )
			return;

		// An empty skip regex skips nothing (an empty Pattern would match everything)
		String  errorSkipRegex   = conf.getProperty(PROPKEY_alarm_ErrorSkipRegex, DEFAULT_alarm_ErrorSkipRegex);
		Pattern errorSkipPattern = StringUtil.hasValue(errorSkipRegex) ? Pattern.compile(errorSkipRegex) : null;

		// Only this database (or the whole instance): the log is shared by all databases in the instance
		String thisDbName = null;
		if (getCounterController() != null && getCounterController().getMonConnection() != null && getCounterController().getMonConnection().getDbmsVersionInfo() instanceof com.dbxtune.sql.conn.info.DbmsVersionInfoDb2)
			thisDbName = ((com.dbxtune.sql.conn.info.DbmsVersionInfoDb2) getCounterController().getMonConnection().getDbmsVersionInfo()).getDbName();

		for (List<Object> row : lastRefreshRows)
		{
			Object o_Timestamp = row.get(pos_Timestamp);
			Object o_Message   = row.get(pos_Message);

			if ( ! (o_Timestamp instanceof Timestamp) || o_Message == null )
				continue;

			Timestamp logTime  = (Timestamp) o_Timestamp;
			String    message  = o_Message.toString().trim();
			String    msgId    = (row.get(pos_MsgId) + "").trim();
			String    severity = (row.get(pos_MsgSeverity) + "").trim();
			Object    o_dbname = row.get(pos_DbName);
			String    dbname   = o_dbname == null ? null : o_dbname.toString().trim();

			// C = Critical, E = Error
			if ( ! ("C".equals(severity) || "E".equals(severity)) )
				continue;

			// Another database in the same instance: its own collector reports it
			if (dbname != null && ! dbname.isEmpty() && thisDbName != null && ! thisDbName.equalsIgnoreCase(dbname))
				continue;

			boolean isSkipped = errorSkipPattern != null && (errorSkipPattern.matcher(msgId).find() || errorSkipPattern.matcher(message).find());

			if (debugPrint || _logger.isDebugEnabled())
				System.out.println("##### sendAlarmRequest(" + getName() + "): Error: isSkipped=" + isSkipped + ", msgId='" + msgId + "', dbname='" + dbname + "', Message='" + StringUtil.truncate(message.replace('\n', ' '), 120, true, null) + "'.");

			if (isSkipped)
				continue;

			String extendedDescText = message;
			String extendedDescHtml = "<pre>\n" + StringEscapeUtils.escapeHtml4(message) + "\n</pre>";
			// The alarm description is stored in 512 characters (the full message is in the extended description)
			String alarmText        = "Notification log entry at '" + logTime + "'" + (dbname == null ? "" : ", database " + dbname) + ": " + StringUtil.truncate(message.replace('\n', ' ').replaceAll(" +", " "), 380, true, null);

			AlarmEvent ae = new AlarmEventErrorLogEntry(this, "C".equals(severity) ? AlarmEvent.Severity.ERROR : AlarmEvent.Severity.WARNING, msgId, alarmText);
			ae.setExtendedDescription(extendedDescText, extendedDescHtml);

			// Information about how to disable this alarm
			ae.createAlarmOptionsMessage(this, "Error");

			AlarmHandler.getInstance().addAlarm(ae);
		} // end: rows loop
	}

	public static final String  PROPKEY_alarm_Error          = CM_NAME + ".alarm.system.on.Error";
	public static final boolean DEFAULT_alarm_Error          = true;

	// The message id (ADM####x) or the message. Empty = skip nothing
	public static final String  PROPKEY_alarm_ErrorSkipRegex = CM_NAME + ".alarm.system.on.Error.skip.regex";
	public static final String  DEFAULT_alarm_ErrorSkipRegex = "";

	@Override
	public List<CmSettingsHelper> getLocalAlarmSettings()
	{
		Configuration conf = Configuration.getCombinedConfiguration();
		List<CmSettingsHelper> list = new ArrayList<>();

		CmSettingsHelper.Type isAlarmSwitch = CmSettingsHelper.Type.IS_ALARM_SWITCH;

		list.add(new CmSettingsHelper("Error"           , isAlarmSwitch, PROPKEY_alarm_Error         , Boolean.class, conf.getBooleanProperty(PROPKEY_alarm_Error         , DEFAULT_alarm_Error         ), DEFAULT_alarm_Error         , "On a message with severity C (Critical, severity ERROR) or E (Error, severity WARNING) for this database or the whole instance, send 'AlarmEventErrorLogEntry'." ));
		list.add(new CmSettingsHelper("Error Skip Regex",                PROPKEY_alarm_ErrorSkipRegex, String .class, conf.getProperty       (PROPKEY_alarm_ErrorSkipRegex, DEFAULT_alarm_ErrorSkipRegex), DEFAULT_alarm_ErrorSkipRegex, "If 'Error' is true; Skip it if the message id (ADM####x) or the message matches this regexp. Empty = skip nothing.", new RegExpInputValidator()));

		return list;
	}
}
