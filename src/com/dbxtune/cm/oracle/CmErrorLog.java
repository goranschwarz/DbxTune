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
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * DbxTune is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with DbxTune.  If not, see <http://www.gnu.org/licenses/>.
 ******************************************************************************/
package com.dbxtune.cm.oracle;

import java.lang.invoke.MethodHandles;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.naming.NameNotFoundException;

import org.apache.commons.text.StringEscapeUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.dbxtune.ICounterController;
import com.dbxtune.IGuiController;
import com.dbxtune.alarm.AlarmHandler;
import com.dbxtune.alarm.events.AlarmEvent;
import com.dbxtune.alarm.events.AlarmEventConfigResourceIsUsedUp;
import com.dbxtune.alarm.events.AlarmEventErrorLogEntry;
import com.dbxtune.cm.CmSettingsHelper;
import com.dbxtune.cm.CmSettingsHelper.RegExpInputValidator;
import com.dbxtune.cm.CounterSetTemplates;
import com.dbxtune.cm.CounterSetTemplates.Type;
import com.dbxtune.cm.CountersModel;
import com.dbxtune.cm.CountersModelAppend;
import com.dbxtune.config.dict.MonTablesDictionary;
import com.dbxtune.config.dict.MonTablesDictionaryManager;
import com.dbxtune.gui.MainFrame;
import com.dbxtune.gui.TabularCntrPanel;
import com.dbxtune.gui.TabularCntrPanelAppend;
import com.dbxtune.sql.conn.DbxConnection;
import com.dbxtune.sql.conn.info.DbmsVersionInfo;
import com.dbxtune.utils.Configuration;
import com.dbxtune.utils.DbUtils;
import com.dbxtune.utils.StringUtil;
import com.dbxtune.utils.Ver;

/**
 * @author Goran Schwarz (goran_schwarz@hotmail.com)
 */
public class CmErrorLog
extends CountersModelAppend
{
	private static final Logger _logger = LogManager.getLogger(MethodHandles.lookup().lookupClass());
	private static final long    serialVersionUID = 1L;

	public static final String   CM_NAME          = CmErrorLog.class.getSimpleName();
	public static final String   SHORT_NAME       = "Alert Log";
	public static final String   HTML_DESC        =
		"<html>" +
		"The alert log of the database: errors (ORA-...), log switches, startup/shutdown, changed parameters...<br>" +
		"<br>" +
		"The first sample gets the last 24 hours, after that only what is new since the previous sample.<br>" +
		"<code>ORA_ERROR</code> is the first ORA- error in the message, which makes it easy to search for (or count) errors.<br>" +
		"<br>" +
		"<b>Note</b>: The login needs it's own grant for this (it is not part of SELECT_CATALOG_ROLE). As SYS: <code>GRANT SELECT ON SYS.V_$DIAG_ALERT_EXT TO &lt;user&gt;</code><br>" +
		"<b>Note</b>: In older Oracle versions (11.2) the database reads ALL logs in the diagnostic directory to answer this, including the listener log.<br>" +
		"If that log is big, this gets slow. Then the alert log is only read once an hour (see the DbxTune log for how to fix it).<br>" +
		"</html>";

	public static final String   GROUP_NAME       = MainFrame.TCP_GROUP_SERVER;
	public static final String   GUI_ICON_FILE    = "images/" + CM_NAME + ".png";

	public static final long     NEED_SRV_VERSION = 0;
	public static final long     NEED_CE_VERSION  = 0;

	public static final String[] MON_TABLES       = new String[] {CM_NAME, "V$DIAG_ALERT_EXT"};
	public static final String[] NEED_ROLES       = new String[] {};
	public static final String[] NEED_CONFIG      = new String[] {};

	public static final boolean  IS_SYSTEM_CM                   = true;
	public static final int      DEFAULT_POSTPONE_TIME          = 300;
	public static final int      DEFAULT_QUERY_TIMEOUT          = 30;

	@Override public int     getDefaultPostponeTime()                 { return DEFAULT_POSTPONE_TIME; }
	@Override public int     getDefaultQueryTimeout()                 { return DEFAULT_QUERY_TIMEOUT; }
	@Override public Type    getTemplateLevel()                       { return Type.MEDIUM; }

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
	public static final String  PROPKEY_sample_firstSampleHours       = CM_NAME + ".sample.firstSample.hours";
	public static final int     DEFAULT_sample_firstSampleHours       = 24;

	public static final String  PROPKEY_sample_slowQueryThresholdInMs = CM_NAME + ".sample.slowQuery.thresholdInMs";
	public static final int     DEFAULT_sample_slowQueryThresholdInMs = 2000;

	public static final String  PROPKEY_sample_slowQueryPostponeTime  = CM_NAME + ".sample.slowQuery.postponeTime";
	public static final int     DEFAULT_sample_slowQueryPostponeTime  = 3600;

	/** Where we are in the alert log (the last row we have seen), the next sample gets what's after this. The time is a string with time zone, straight from the DBMS, so we never convert it */
	private String _lastLogTimeTz = null;
	private long   _lastRecordId  = -1;

	@Override
	protected void registerDefaultValues()
	{
		super.registerDefaultValues();

		Configuration.registerDefaultValue(PROPKEY_sample_firstSampleHours,       DEFAULT_sample_firstSampleHours);
		Configuration.registerDefaultValue(PROPKEY_sample_slowQueryThresholdInMs, DEFAULT_sample_slowQueryThresholdInMs);
		Configuration.registerDefaultValue(PROPKEY_sample_slowQueryPostponeTime,  DEFAULT_sample_slowQueryPostponeTime);
	}

	/** Used by the: Create 'Offline Session' Wizard */
	@Override
	public List<CmSettingsHelper> getLocalSettings()
	{
		Configuration conf = Configuration.getCombinedConfiguration();
		List<CmSettingsHelper> list = new ArrayList<>();

		list.add(new CmSettingsHelper("First Sample, get last ## Hours", PROPKEY_sample_firstSampleHours      , Integer.class, conf.getIntProperty(PROPKEY_sample_firstSampleHours      , DEFAULT_sample_firstSampleHours      ), DEFAULT_sample_firstSampleHours      , "The first sample (and the first after a reconnect) gets the alert log for the last ## hours. After that: only what is new since the previous sample." ));
		list.add(new CmSettingsHelper("Slow Query, Threshold in ms"    , PROPKEY_sample_slowQueryThresholdInMs, Integer.class, conf.getIntProperty(PROPKEY_sample_slowQueryThresholdInMs, DEFAULT_sample_slowQueryThresholdInMs), DEFAULT_sample_slowQueryThresholdInMs, "If reading the alert log takes longer than ## milliseconds, the 'Postpone Time' is changed to the value below (to not put load on the DBMS)." ));
		list.add(new CmSettingsHelper("Slow Query, Postpone Time"      , PROPKEY_sample_slowQueryPostponeTime , Integer.class, conf.getIntProperty(PROPKEY_sample_slowQueryPostponeTime , DEFAULT_sample_slowQueryPostponeTime ), DEFAULT_sample_slowQueryPostponeTime , "If reading the alert log is slow (see above), only read it every ## seconds." ));

		return list;
	}

	@Override
	protected TabularCntrPanel createGui()
	{
		return new TabularCntrPanelAppend(this);
	}

	@Override
	public void reset()
	{
		super.reset();

		// Start over (get the last ## hours) at next sample
		_lastLogTimeTz = null;
		_lastRecordId  = -1;
	}

	/** If we can't read the view: disable the CM, and tell what to do about it */
	@Override
	public boolean checkDependsOnOther(DbxConnection conn)
	{
		if (DbUtils.checkIfTableIsSelectable(conn, "V$DIAG_ALERT_EXT"))
			return true;

		String username = "<user>";
		if (conn.getConnPropOrDefault() != null && StringUtil.hasValue(conn.getConnPropOrDefault().getUsername()))
			username = conn.getConnPropOrDefault().getUsername();

		String msg = "The login can't read 'V$DIAG_ALERT_EXT' (the alert log). It is NOT part of SELECT_CATALOG_ROLE, so it needs it's own grant.";
		String fix = "(as SYS) GRANT SELECT ON SYS.V_$DIAG_ALERT_EXT TO " + username + ";";

		_logger.warn("When trying to initialize Counters Model '" + getName() + "', named '" + getDisplayName() + "': " + msg + " Fix: " + fix);

		setActive(false, msg + "\n" + "Fix: " + fix);

		TabularCntrPanel tcp = getTabPanel();
		if (tcp != null)
		{
			tcp.setUseFocusableTips(true);
			tcp.setToolTipText("<html>" + msg + "<br>Fix: <code>" + fix.replace("<", "&lt;").replace(">", "&gt;") + "</code></html>");
		}

		return false;
	}

	/** Can we select those column(s) from V$DIAG_ALERT_EXT. (where 1=2: so the alert log is never read) */
	private static boolean hasAlertLogColumns(DbxConnection conn, String columns)
	{
		try (Statement stmnt = conn.createStatement(); ResultSet rs = stmnt.executeQuery("select " + columns + " from V$DIAG_ALERT_EXT where 1=2"))
		{
			return true;
		}
		catch (SQLException ex)
		{
			return false;
		}
	}

	@Override
	public String getSqlForVersion(DbxConnection conn, DbmsVersionInfo versionInfo)
	{
		// V$DIAG_ALERT_EXT looks different in different versions, and only some of them can be tested: so check what columns the view has (and only guess on the version when we have no connection)
		//   ADR_HOME       - 11.2 and 18 has it (23 has not). In 11.2 the view shows ALL logs under the ADR base (other databases, the listener...), so we have to pick out the alert log of THIS database
		//                    It's relative to the ADR base, and looks different: 11.2 'diag/rdbms/xe/XE', 18 'diag/rdbms/xe/XE/'. So we check that the full 'ADR Home' ENDS with it.
		//   CONTAINER_NAME - 18 and 23 has it (11.2 has not). The PDB the message is about
		// conn is null when we just want to "view" the SQL (the CM Properties dialog)
		boolean hasAdrHome       = versionInfo.getLongVersion() <  Ver.ver(12,2);
		boolean hasContainerName = versionInfo.getLongVersion() >= Ver.ver(12,2);
		if (conn != null)
		{
			hasAdrHome       = hasAlertLogColumns(conn, "ADR_HOME");
			hasContainerName = hasAlertLogColumns(conn, "CONTAINER_NAME");
		}

		String CON_NAME    = hasContainerName ? "CAST(a.CONTAINER_NAME as VARCHAR2(128))" : "CAST(NULL             as VARCHAR2(128))";
		String onlyThisAdr = hasAdrHome       ? "  and (select VALUE from V$DIAG_INFO where NAME = 'ADR Home') like '%/' || RTRIM(a.ADR_HOME, '/') \n" : "";

		// ${afterLastLogTime} is replaced in getSql(): the last ## hours at the first sample, then "after the last row we have seen"
		// Note: LOG_TIME is the time on the DBMS host (a TIMESTAMP WITH TIME ZONE can't be stored, so the time zone is dropped), LOG_TIME_TZ is the same time as a string with the time zone
		//       The time predicate is always there: on 11.2 the view returns NO rows if COMPONENT_ID is the only predicate
		String sql = ""
			+ "select \n"
			+ "     CAST(a.ORIGINATING_TIMESTAMP as TIMESTAMP(3))                                                                                                              as LOG_TIME \n"
			+ "    ,a.RECORD_ID \n"
			+ "    ,CAST(decode(a.MESSAGE_TYPE,  1, 'UNKNOWN', 2, 'INCIDENT_ERROR', 3, 'ERROR', 4, 'WARNING', 5, 'NOTIFICATION', 6, 'TRACE', TO_CHAR(a.MESSAGE_TYPE)) as VARCHAR2(20)) as MESSAGE_TYPE \n"
			+ "    ,CAST(decode(a.MESSAGE_LEVEL, 1, 'CRITICAL', 2, 'SEVERE', 8, 'IMPORTANT', 16, 'NORMAL', TO_CHAR(a.MESSAGE_LEVEL))                          as VARCHAR2(20)) as MESSAGE_LEVEL \n"
			+ "    ,CAST(case when REGEXP_LIKE(a.MESSAGE_TEXT, 'ORA-[0-9]+') then 'ORA-' || LPAD(REGEXP_SUBSTR(a.MESSAGE_TEXT, 'ORA-0*([0-9]+)', 1, 1, NULL, 1), 5, '0') end as VARCHAR2(12)) as ORA_ERROR \n"
			+ "    ,a.PROBLEM_KEY \n"
			+ "    ," + CON_NAME + "                                                                                                                    as CON_NAME \n"
			+ "    ,a.PROCESS_ID \n"
			+ "    ,a.MODULE_ID \n"
			+ "    ,a.MESSAGE_TEXT \n"
			+ "    ,TO_CHAR(a.ORIGINATING_TIMESTAMP, 'YYYY-MM-DD HH24:MI:SS.FF6 TZH:TZM')                                                                                      as LOG_TIME_TZ \n"
			+ "from V$DIAG_ALERT_EXT a \n"
			+ "where a.COMPONENT_ID = 'rdbms' \n"
			+ onlyThisAdr
			+ "  and ${afterLastLogTime} \n"
			+ "order by a.ORIGINATING_TIMESTAMP, a.RECORD_ID \n"
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
		if (_lastLogTimeTz == null)
		{
			int hours = Configuration.getCombinedConfiguration().getIntProperty(PROPKEY_sample_firstSampleHours, DEFAULT_sample_firstSampleHours);

			return sqlText.replace("${afterLastLogTime}", "a.ORIGINATING_TIMESTAMP > SYSTIMESTAMP - NUMTODSINTERVAL(" + hours + ", 'HOUR')");
		}

		// After that: what comes after the last row we have seen (many rows can have the same time, so RECORD_ID is also needed)
		String lastLogTime = "TO_TIMESTAMP_TZ('" + _lastLogTimeTz + "', 'YYYY-MM-DD HH24:MI:SS.FF6 TZH:TZM')";

		return sqlText.replace("${afterLastLogTime}", "(a.ORIGINATING_TIMESTAMP > " + lastLogTime + " or (a.ORIGINATING_TIMESTAMP = " + lastLogTime + " and a.RECORD_ID > " + _lastRecordId + "))");
	}

	@Override
	public void hookInNearEndOfRefreshGetData()
	{
		super.hookInNearEndOfRefreshGetData();

		// Remember the last row in this sample (the rows are ordered by time)
		List<List<Object>> lastRefreshRows = getDataCollectionForLastRefresh();
		int pos_LOG_TIME_TZ = findColumn("LOG_TIME_TZ");
		int pos_RECORD_ID   = findColumn("RECORD_ID");

		if (lastRefreshRows != null && ! lastRefreshRows.isEmpty() && pos_LOG_TIME_TZ >= 0 && pos_RECORD_ID >= 0)
		{
			List<Object> lastRow = lastRefreshRows.get(lastRefreshRows.size() - 1);

			if (lastRow.get(pos_LOG_TIME_TZ) != null)
			{
				_lastLogTimeTz = lastRow.get(pos_LOG_TIME_TZ).toString();
				_lastRecordId  = lastRow.get(pos_RECORD_ID) instanceof Number ? ((Number) lastRow.get(pos_RECORD_ID)).longValue() : -1;
			}
		}

		// If it was slow to read the alert log: do it less often
		Configuration conf = Configuration.getCombinedConfiguration();
		int  slowThresholdMs  = conf.getIntProperty(PROPKEY_sample_slowQueryThresholdInMs, DEFAULT_sample_slowQueryThresholdInMs);
		int  slowPostponeTime = conf.getIntProperty(PROPKEY_sample_slowQueryPostponeTime , DEFAULT_sample_slowQueryPostponeTime);
		long sqlRefreshTimeMs = getSqlRefreshTime();

		if (sqlRefreshTimeMs > slowThresholdMs && getPostponeTime() < slowPostponeTime)
		{
			_logger.warn(CM_NAME + ": Reading the alert log took " + sqlRefreshTimeMs + " ms (the limit is " + slowThresholdMs + " ms), so from now on it's only read every " + slowPostponeTime + " seconds (instead of every " + getPostponeTime() + " seconds). "
					+ "Reason: In this Oracle version 'V$DIAG_ALERT_EXT' reads ALL logs in the diagnostic directory (the ADR base) every time, and one of them is probably big (typically the listener log). "
					+ "Fix: Purge that log, for example with adrci: 'set homepath diag/tnslsnr/<host>/listener', then 'purge -age <minutes> -type ALERT'. "
					+ "The limits can be changed with: " + PROPKEY_sample_slowQueryThresholdInMs + "=" + slowThresholdMs + ", " + PROPKEY_sample_slowQueryPostponeTime + "=" + slowPostponeTime);

			setPostponeTime(slowPostponeTime, false);
		}
	}

	@Override
	public void addMonTableDictForVersion(DbxConnection conn, DbmsVersionInfo versionInfo)
	{
		try
		{
			MonTablesDictionary mtd = MonTablesDictionaryManager.getInstance();

			mtd.addTable(CM_NAME, "The alert log of the database: errors (ORA-...), log switches, startup/shutdown, changed parameters...");

			mtd.addColumn(CM_NAME, "LOG_TIME",      "<html>When the message was written, in the time of the DBMS host.</html>");
			mtd.addColumn(CM_NAME, "RECORD_ID",     "<html>Row number in the alert log.</html>");
			mtd.addColumn(CM_NAME, "MESSAGE_TYPE",  "<html>Type of message: UNKNOWN, INCIDENT_ERROR, ERROR, WARNING, NOTIFICATION, TRACE.<br><b>INCIDENT_ERROR</b> is an internal error (like ORA-00600 or ORA-07445), where Oracle has created an incident with trace files.<br><b>Note</b>: Most messages (also most errors) are written as UNKNOWN, so do not trust this to find errors, use ORA_ERROR for that.</html>");
			mtd.addColumn(CM_NAME, "MESSAGE_LEVEL", "<html>How important Oracle thinks the message is: CRITICAL, SEVERE, IMPORTANT, NORMAL.<br><b>Note</b>: Most messages (also most errors) are written as NORMAL.</html>");
			mtd.addColumn(CM_NAME, "ORA_ERROR",     "<html>The first ORA- error in MESSAGE_TEXT, always with 5 digits (ORA-01653 even if the text says ORA-1653). NULL if there is no ORA- error in the text.</html>");
			mtd.addColumn(CM_NAME, "PROBLEM_KEY",   "<html>For an incident: what kind of problem it is, like 'ORA 600 [kdsgrp1]'. All incidents with the same key are the same problem.</html>");
			mtd.addColumn(CM_NAME, "CON_NAME",      "<html>Name of the Container (PDB) the message is about. NULL in versions where the alert log does not have it.</html>");
			mtd.addColumn(CM_NAME, "PROCESS_ID",    "<html>Operating system process id of the process that wrote the message.</html>");
			mtd.addColumn(CM_NAME, "MODULE_ID",     "<html>The module (often the program name) of the session that wrote the message.</html>");
			mtd.addColumn(CM_NAME, "MESSAGE_TEXT",  "<html>The message.</html>");
			mtd.addColumn(CM_NAME, "LOG_TIME_TZ",   "<html>Same as LOG_TIME, but as a string with the time zone of the DBMS host. (DbxTune uses this to know where to continue at the next sample)</html>");
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
			_logger.info("First time we check the alert log, Alarms will NOT be checked, because it will include old error messages. (Rows in first refresh was " + (lastRefreshRows == null ? "-null-" : lastRefreshRows.size()) + ")");
			return;
		}

		// Exit early if NO ROWS
		if (lastRefreshRows == null || lastRefreshRows.isEmpty())
			return;

		int pos_LOG_TIME     = findColumn("LOG_TIME");
		int pos_MESSAGE_TYPE = findColumn("MESSAGE_TYPE");
		int pos_PROBLEM_KEY  = findColumn("PROBLEM_KEY");
		int pos_CON_NAME     = findColumn("CON_NAME");
		int pos_MESSAGE_TEXT = findColumn("MESSAGE_TEXT");

		if (pos_LOG_TIME < 0 || pos_MESSAGE_TYPE < 0 || pos_PROBLEM_KEY < 0 || pos_CON_NAME < 0 || pos_MESSAGE_TEXT < 0)
		{
			_logger.error("When checking for alarms, could not find all columns. skipping this. [LOG_TIME_pos=" + pos_LOG_TIME + ", MESSAGE_TYPE_pos=" + pos_MESSAGE_TYPE + ", PROBLEM_KEY_pos=" + pos_PROBLEM_KEY + ", CON_NAME_pos=" + pos_CON_NAME + ", MESSAGE_TEXT_pos=" + pos_MESSAGE_TEXT + "]");
			return;
		}

		boolean checkMaxProcessesOrSessions = isSystemAlarmsForColumnEnabledAndInTimeRange("MaxProcessesOrSessions") && conf.getBooleanProperty(PROPKEY_alarm_MaxProcessesOrSessions, DEFAULT_alarm_MaxProcessesOrSessions);
		boolean checkInternalError          = isSystemAlarmsForColumnEnabledAndInTimeRange("InternalError")          && conf.getBooleanProperty(PROPKEY_alarm_InternalError         , DEFAULT_alarm_InternalError);
		boolean checkOraError               = isSystemAlarmsForColumnEnabledAndInTimeRange("OraError")               && conf.getBooleanProperty(PROPKEY_alarm_OraError              , DEFAULT_alarm_OraError);

		// Each of them finds an 'ORA-#####' in the message text, and group(1) is the error number
		Pattern maxProcessesOrSessionsPattern = Pattern.compile("ORA-0*(18|20)(?![0-9])");
		Pattern internalErrorPattern          = Pattern.compile(conf.getProperty(PROPKEY_alarm_InternalErrorRegex    , DEFAULT_alarm_InternalErrorRegex));
		Pattern internalErrorSkipPattern      = Pattern.compile(conf.getProperty(PROPKEY_alarm_InternalErrorSkipRegex, DEFAULT_alarm_InternalErrorSkipRegex));
		Pattern oraErrorPattern               = Pattern.compile(conf.getProperty(PROPKEY_alarm_OraErrorRegex         , DEFAULT_alarm_OraErrorRegex));
		Pattern anyOraErrorPattern            = Pattern.compile("ORA-0*([0-9]+)");

		for (List<Object> row : lastRefreshRows)
		{
			Object o_LOG_TIME     = row.get(pos_LOG_TIME);
			Object o_MESSAGE_TEXT = row.get(pos_MESSAGE_TEXT);
			if ( ! (o_LOG_TIME instanceof Timestamp) || ! (o_MESSAGE_TEXT instanceof String) )
				continue;

			Timestamp logTime     = (Timestamp) o_LOG_TIME;
			String    messageText = ((String) o_MESSAGE_TEXT).trim();
			String    messageType = row.get(pos_MESSAGE_TYPE) + "";
			String    problemKey  = row.get(pos_PROBLEM_KEY) == null ? "" : row.get(pos_PROBLEM_KEY).toString();
			String    conName     = row.get(pos_CON_NAME)    == null ? "" : row.get(pos_CON_NAME)   .toString();

			String extendedDescText = messageText;
			String extendedDescHtml = "<pre>\n" + StringEscapeUtils.escapeHtml4(messageText) + "\n</pre>";
			String alarmText        = "Alert log entry at '" + logTime + "'" + (StringUtil.hasValue(conName) ? " in container '" + conName + "'" : "") + (StringUtil.hasValue(problemKey) ? ", ProblemKey='" + problemKey + "'" : "") + ": " + StringUtil.truncate(messageText.replace('\n', ' '), 1000, true, null);

			Matcher m;

			//-------------------------------------------------------
			// MaxProcessesOrSessions: ORA-00020: maximum number of processes (%s) exceeded, ORA-00018: maximum number of sessions exceeded
			//-------------------------------------------------------
			if (checkMaxProcessesOrSessions && (m = maxProcessesOrSessionsPattern.matcher(messageText)).find())
			{
				int errorNumber = StringUtil.parseInt(m.group(1), -1);

				AlarmEvent ae = new AlarmEventConfigResourceIsUsedUp(this, (errorNumber == 20 ? "processes" : "sessions"), errorNumber, messageText, logTime);
				ae.setExtendedDescription(extendedDescText, extendedDescHtml);

				// Information about how to disable this alarm
				ae.createAlarmOptionsMessage(this, "MaxProcessesOrSessions");

				AlarmHandler.getInstance().addAlarm(ae);
				continue;
			}

			//-------------------------------------------------------
			// InternalError: an incident (ORA-00600, ORA-07445...), but not the "soft" ones (ORA-00700, ORA-00800)
			//-------------------------------------------------------
			if (checkInternalError)
			{
				boolean isInternalError = "INCIDENT_ERROR".equals(messageType) || internalErrorPattern.matcher(messageText).find();
				boolean isSkipped       = internalErrorSkipPattern.matcher(messageText).find() || internalErrorSkipPattern.matcher(problemKey).find();

				if (debugPrint || _logger.isDebugEnabled())
					System.out.println("##### sendAlarmRequest(" + getName() + "): InternalError: isInternalError=" + isInternalError + ", isSkipped=" + isSkipped + ", MESSAGE_TYPE='" + messageType + "', PROBLEM_KEY='" + problemKey + "', MESSAGE_TEXT='" + StringUtil.truncate(messageText.replace('\n', ' '), 120, true, null) + "'.");

				if (isInternalError && ! isSkipped)
				{
					// What to call it: the internal error that matched, otherwise the first ORA- error in the text, otherwise the problem key
					String errorName = StringUtil.hasValue(problemKey) ? problemKey : "INCIDENT";
					if      ((m = internalErrorPattern.matcher(messageText)).find()) errorName = String.format("ORA-%05d", StringUtil.parseInt(m.group(1), 0));
					else if ((m = anyOraErrorPattern  .matcher(messageText)).find()) errorName = String.format("ORA-%05d", StringUtil.parseInt(m.group(1), 0));

					AlarmEvent ae = new AlarmEventErrorLogEntry(this, AlarmEvent.Severity.ERROR, errorName, alarmText);
					ae.setExtendedDescription(extendedDescText, extendedDescHtml);

					// Information about how to disable this alarm
					ae.createAlarmOptionsMessage(this, "InternalError");

					AlarmHandler.getInstance().addAlarm(ae);
					continue;
				}
			}

			//-------------------------------------------------------
			// OraError: the errors a DBA wants to know about (out of memory, corruption, archiver stuck, can't extend...)
			//-------------------------------------------------------
			if (checkOraError && (m = oraErrorPattern.matcher(messageText)).find())
			{
				String errorName = String.format("ORA-%05d", StringUtil.parseInt(m.group(1), 0));

				if (debugPrint || _logger.isDebugEnabled())
					System.out.println("##### sendAlarmRequest(" + getName() + "): OraError: errorName='" + errorName + "', MESSAGE_TEXT='" + StringUtil.truncate(messageText.replace('\n', ' '), 120, true, null) + "'.");

				AlarmEvent ae = new AlarmEventErrorLogEntry(this, AlarmEvent.Severity.WARNING, errorName, alarmText);
				ae.setExtendedDescription(extendedDescText, extendedDescHtml);

				// Information about how to disable this alarm
				ae.createAlarmOptionsMessage(this, "OraError");

				AlarmHandler.getInstance().addAlarm(ae);
			}
		} // end: rows loop
	}

	public static final String  PROPKEY_alarm_MaxProcessesOrSessions = CM_NAME + ".alarm.system.on.MaxProcessesOrSessions";
	public static final boolean DEFAULT_alarm_MaxProcessesOrSessions = true;

	public static final String  PROPKEY_alarm_InternalError          = CM_NAME + ".alarm.system.on.InternalError";
	public static final boolean DEFAULT_alarm_InternalError          = true;

	// Note: "(?![0-9])" = not followed by a digit (so 'ORA-600' does not match 'ORA-6000'). It's used instead of "\b", since a backslash is a pain to write in a properties file
	//       group(1) must be the error number
	public static final String  PROPKEY_alarm_InternalErrorRegex     = CM_NAME + ".alarm.system.on.InternalError.regex";
	public static final String  DEFAULT_alarm_InternalErrorRegex     = "ORA-0*(600|603|7445)(?![0-9])";

	// ORA-00700: soft internal error,  ORA-00800: soft external error (for example: can't set the priority of a process, which is normal in a container)
	public static final String  PROPKEY_alarm_InternalErrorSkipRegex = CM_NAME + ".alarm.system.on.InternalError.skip.regex";
	public static final String  DEFAULT_alarm_InternalErrorSkipRegex = "ORA[- ]0*(700|800)(?![0-9])";

	public static final String  PROPKEY_alarm_OraError               = CM_NAME + ".alarm.system.on.OraError";
	public static final boolean DEFAULT_alarm_OraError               = true;

	//   1555           - snapshot too old
	//   4030,4031,4036 - out of process memory, out of shared memory, PGA_AGGREGATE_LIMIT exceeded
	//   1578           - data block corrupted
	//   376,1113,1114,1115,1122,1157 - problems to read/write/identify a data file
	//   257,16014,16038,19809,19815  - the archiver can't archive, the recovery area is full (or getting full)
	//   1652-1655,1688,30036         - unable to extend (temp, table, index, cluster, partition, undo)
	//   1092,470-479   - the instance (or a background process) was terminated
	//   27072,27102,27300-27303      - operating system: I/O error, out of memory, a system call failed
	public static final String  PROPKEY_alarm_OraErrorRegex          = CM_NAME + ".alarm.system.on.OraError.regex";
	public static final String  DEFAULT_alarm_OraErrorRegex          = "ORA-0*(1555|4030|4031|4036|1578|376|1113|1114|1115|1122|1157|257|16014|16038|19809|19815|165[2-5]|1688|30036|1092|47[0-9]|27072|27102|2730[0-3])(?![0-9])";

	@Override
	public List<CmSettingsHelper> getLocalAlarmSettings()
	{
		Configuration conf = Configuration.getCombinedConfiguration();
		List<CmSettingsHelper> list = new ArrayList<>();

		CmSettingsHelper.Type isAlarmSwitch = CmSettingsHelper.Type.IS_ALARM_SWITCH;

		list.add(new CmSettingsHelper("MaxProcessesOrSessions"  , isAlarmSwitch, PROPKEY_alarm_MaxProcessesOrSessions, Boolean.class, conf.getBooleanProperty(PROPKEY_alarm_MaxProcessesOrSessions, DEFAULT_alarm_MaxProcessesOrSessions), DEFAULT_alarm_MaxProcessesOrSessions, "On ORA-00020 (maximum number of processes exceeded) or ORA-00018 (maximum number of sessions exceeded), send 'AlarmEventConfigResourceIsUsedUp'." ));
		list.add(new CmSettingsHelper("InternalError"           , isAlarmSwitch, PROPKEY_alarm_InternalError         , Boolean.class, conf.getBooleanProperty(PROPKEY_alarm_InternalError         , DEFAULT_alarm_InternalError         ), DEFAULT_alarm_InternalError         , "On an incident (MESSAGE_TYPE is INCIDENT_ERROR), or when the message matches the regexp below, send 'AlarmEventErrorLogEntry' with severity ERROR." ));
		list.add(new CmSettingsHelper("InternalError Regex"     ,                PROPKEY_alarm_InternalErrorRegex    , String .class, conf.getProperty       (PROPKEY_alarm_InternalErrorRegex    , DEFAULT_alarm_InternalErrorRegex    ), DEFAULT_alarm_InternalErrorRegex    , "If 'InternalError' is true; The message is an internal error if it matches this regexp. (the first group must be the error number)", new RegExpInputValidator()));
		list.add(new CmSettingsHelper("InternalError Skip Regex",                PROPKEY_alarm_InternalErrorSkipRegex, String .class, conf.getProperty       (PROPKEY_alarm_InternalErrorSkipRegex, DEFAULT_alarm_InternalErrorSkipRegex), DEFAULT_alarm_InternalErrorSkipRegex, "If 'InternalError' is true; Skip it if the message or PROBLEM_KEY matches this regexp. Default is the 'soft' errors ORA-00700 and ORA-00800.", new RegExpInputValidator()));
		list.add(new CmSettingsHelper("OraError"                , isAlarmSwitch, PROPKEY_alarm_OraError              , Boolean.class, conf.getBooleanProperty(PROPKEY_alarm_OraError              , DEFAULT_alarm_OraError              ), DEFAULT_alarm_OraError              , "When the message matches the regexp below, send 'AlarmEventErrorLogEntry' with severity WARNING." ));
		list.add(new CmSettingsHelper("OraError Regex"          ,                PROPKEY_alarm_OraErrorRegex         , String .class, conf.getProperty       (PROPKEY_alarm_OraErrorRegex         , DEFAULT_alarm_OraErrorRegex         ), DEFAULT_alarm_OraErrorRegex         , "If 'OraError' is true; The ORA- errors to send an alarm for. (the first group must be the error number)", new RegExpInputValidator()));

		return list;
	}
}
