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
package com.dbxtune.cm.mysql;

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
import com.dbxtune.config.dict.MonTablesDictionary;
import com.dbxtune.config.dict.MonTablesDictionaryManager;
import com.dbxtune.gui.MainFrame;
import com.dbxtune.gui.TabularCntrPanel;
import com.dbxtune.gui.TabularCntrPanelAppend;
import com.dbxtune.sql.conn.DbxConnection;
import com.dbxtune.sql.conn.info.DbmsVersionInfo;
import com.dbxtune.utils.Configuration;
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
	public static final String   SHORT_NAME       = "Error Log";
	public static final String   HTML_DESC        =
		"<html>" +
		"The error log of the server: errors, warnings, startup/shutdown, replication... (performance_schema.error_log, MySQL 8.0.22 and above)<br>" +
		"<br>" +
		"The first sample gets the last 24 hours, after that only what is new since the previous sample.<br>" +
		"<br>" +
		"<b>Note</b>: What is written depends on <code>log_error_verbosity</code>: 1 = errors, 2 = errors and warnings (the default), 3 = also notes (information).<br>" +
		"<b>Note</b>: Deadlocks are only written if <code>innodb_print_all_deadlocks = ON</code> <b>and</b> <code>log_error_verbosity = 3</code> (InnoDB writes them as notes).<br>" +
		"<b>Note</b>: The table is in memory, with a fixed size: when it is full, the oldest messages are removed (the error log FILE still has them).<br>" +
		"</html>";

	public static final String   GROUP_NAME       = MainFrame.TCP_GROUP_SERVER;
	public static final String   GUI_ICON_FILE    = "images/" + CM_NAME + ".png";

	public static final long     NEED_SRV_VERSION = Ver.ver(8,0,22);
	public static final long     NEED_CE_VERSION  = 0;

	public static final String[] MON_TABLES       = new String[] {CM_NAME, "error_log"};
	public static final String[] NEED_ROLES       = new String[] {};
	public static final String[] NEED_CONFIG      = new String[] {};

	public static final boolean  IS_SYSTEM_CM                   = true;
	public static final int      DEFAULT_POSTPONE_TIME          = 60;
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

	/** Where we are in the error log (the time of the last row we have seen), the next sample gets what's after this. It's a string in the session time zone, straight from the DBMS, so we never convert it */
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

		list.add(new CmSettingsHelper("First Sample, get last ## Hours", PROPKEY_sample_firstSampleHours, Integer.class, conf.getIntProperty(PROPKEY_sample_firstSampleHours, DEFAULT_sample_firstSampleHours), DEFAULT_sample_firstSampleHours, "The first sample (and the first after a reconnect) gets the error log for the last ## hours. After that: only what is new since the previous sample." ));

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
		_lastLogTimeStr = null;
	}

	/** If we can't read the view: disable the CM, and tell what to do about it (MariaDB does not have it) */
	@Override
	public boolean checkDependsOnOther(DbxConnection conn)
	{
		return MySqlCmUtils.checkViewsAreReadable(this, conn, "performance_schema.error_log");
	}

	@Override
	public String getSqlForVersion(DbxConnection conn, DbmsVersionInfo versionInfo)
	{
		// ${afterLastLogTime} is replaced in getSql(): the last ## hours at the first sample, then "after the last row we have seen"
		// Note: LOGGED is a TIMESTAMP, shown in the time zone of the session. LogTimeStr is the same time as a string (with microseconds), so the next sample can continue exactly there
		String sql = ""
			+ "select /* ${cmCollectorName} */ \n"
			+ "     LOGGED                                         as LogTime \n"
			+ "    ,PRIO                                           as Prio \n"
			+ "    ,ERROR_CODE                                     as ErrorCode \n"
			+ "    ,SUBSYSTEM                                      as Subsystem \n"
			+ "    ,THREAD_ID                                      as ThreadId \n"
			+ "    ,DATA                                           as Message \n"
			+ "    ,DATE_FORMAT(LOGGED, '%Y-%m-%d %H:%i:%s.%f')    as LogTimeStr \n"
			+ "from performance_schema.error_log \n"
			+ "where ${afterLastLogTime} \n"
			+ "order by LOGGED \n"
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

			return sqlText.replace("${afterLastLogTime}", "LOGGED > NOW(6) - INTERVAL " + hours + " HOUR");
		}

		// After that: what comes after the last row we have seen
		// NOTE: There is no row number. Two messages with the same microsecond, where the second is written after we read the first, would be missed (it has not been seen in the tests)
		return sqlText.replace("${afterLastLogTime}", "LOGGED > '" + _lastLogTimeStr + "'");
	}

	@Override
	public void hookInNearEndOfRefreshGetData()
	{
		super.hookInNearEndOfRefreshGetData();

		// Remember the last row in this sample (the rows are ordered by time)
		List<List<Object>> lastRefreshRows = getDataCollectionForLastRefresh();
		int pos_LogTimeStr = findColumn("LogTimeStr");

		if (lastRefreshRows != null && ! lastRefreshRows.isEmpty() && pos_LogTimeStr >= 0)
		{
			Object lastLogTimeStr = lastRefreshRows.get(lastRefreshRows.size() - 1).get(pos_LogTimeStr);
			if (lastLogTimeStr != null)
				_lastLogTimeStr = lastLogTimeStr.toString();
		}
		// NOTE: If there was nothing, the next sample reads the last ## hours again (it's an in-memory table, so that is cheap)
		//       The sample time is NOT used as a starting point: it's converted by the JDBC driver, and the JVM and the session may have different time zones
	}

	@Override
	public void addMonTableDictForVersion(DbxConnection conn, DbmsVersionInfo versionInfo)
	{
		try
		{
			MonTablesDictionary mtd = MonTablesDictionaryManager.getInstance();

			mtd.addTable(CM_NAME, "The error log of the server: errors, warnings, startup/shutdown, replication...");

			mtd.addColumn(CM_NAME, "LogTime",    "<html>When the message was written (the time zone of the session, as NOW()).</html>");
			mtd.addColumn(CM_NAME, "Prio",       "<html>System, Error, Warning or Note.<br><b>System</b> is startup, shutdown and other important information (always written, whatever <code>log_error_verbosity</code> is).</html>");
			mtd.addColumn(CM_NAME, "ErrorCode",  "<html>The message number, for example <code>MY-010584</code>. Look it up in the MySQL 'Error Message Reference', or with: <code>perror 10584</code></html>");
			mtd.addColumn(CM_NAME, "Subsystem",  "<html>Server, InnoDB, Repl (replication)...</html>");
			mtd.addColumn(CM_NAME, "ThreadId",   "<html>The connection (processlist id) that wrote the message. 0 = the server itself.</html>");
			mtd.addColumn(CM_NAME, "Message",    "<html>The message.</html>");
			mtd.addColumn(CM_NAME, "LogTimeStr", "<html>Same as LogTime, but as a string with microseconds. (DbxTune uses this to know where to continue at the next sample)</html>");
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
			_logger.info("First time we check the error log, Alarms will NOT be checked, because it will include old error messages. (Rows in first refresh was " + (lastRefreshRows == null ? "-null-" : lastRefreshRows.size()) + ")");
			return;
		}

		// Exit early if NO ROWS
		if (lastRefreshRows == null || lastRefreshRows.isEmpty())
			return;

		int pos_LogTime   = findColumn("LogTime");
		int pos_Prio      = findColumn("Prio");
		int pos_ErrorCode = findColumn("ErrorCode");
		int pos_Subsystem = findColumn("Subsystem");
		int pos_Message   = findColumn("Message");

		if (pos_LogTime < 0 || pos_Prio < 0 || pos_ErrorCode < 0 || pos_Subsystem < 0 || pos_Message < 0)
		{
			_logger.error("When checking for alarms, could not find all columns. skipping this. [LogTime_pos=" + pos_LogTime + ", Prio_pos=" + pos_Prio + ", ErrorCode_pos=" + pos_ErrorCode + ", Subsystem_pos=" + pos_Subsystem + ", Message_pos=" + pos_Message + "]");
			return;
		}

		boolean checkReplicationError = isSystemAlarmsForColumnEnabledAndInTimeRange("ReplicationError") && conf.getBooleanProperty(PROPKEY_alarm_ReplicationError, DEFAULT_alarm_ReplicationError);
		boolean checkDeadlock         = isSystemAlarmsForColumnEnabledAndInTimeRange("Deadlock")         && conf.getBooleanProperty(PROPKEY_alarm_Deadlock        , DEFAULT_alarm_Deadlock);
		boolean checkError            = isSystemAlarmsForColumnEnabledAndInTimeRange("Error")            && conf.getBooleanProperty(PROPKEY_alarm_Error           , DEFAULT_alarm_Error);

		// An empty skip regex skips nothing (an empty Pattern would match everything)
		String  errorSkipRegex   = conf.getProperty(PROPKEY_alarm_ErrorSkipRegex, DEFAULT_alarm_ErrorSkipRegex);
		Pattern errorSkipPattern = StringUtil.hasValue(errorSkipRegex) ? Pattern.compile(errorSkipRegex) : null;

		for (List<Object> row : lastRefreshRows)
		{
			Object o_LogTime = row.get(pos_LogTime);
			Object o_Message = row.get(pos_Message);
			if ( ! (o_LogTime instanceof Timestamp) || o_Message == null )
				continue;

			Timestamp logTime   = (Timestamp) o_LogTime;
			String    message   = o_Message.toString().trim();
			String    prio      = row.get(pos_Prio)      + "";
			String    errorCode = row.get(pos_ErrorCode) + "";
			String    subsystem = row.get(pos_Subsystem) + "";

			String extendedDescText = message;
			String extendedDescHtml = "<pre>\n" + StringEscapeUtils.escapeHtml4(message) + "\n</pre>";
			String alarmText        = "Error log entry at '" + logTime + "', " + prio + " " + errorCode + " [" + subsystem + "]: " + StringUtil.truncate(message.replace('\n', ' '), 1000, true, null);

			//-------------------------------------------------------
			// ReplicationError: the replica stopped, or a transaction failed (duplicate key, row not found...). The tab 'Replication' also has alarms for the stopped threads
			//-------------------------------------------------------
			if ("Error".equals(prio) && "Repl".equals(subsystem))
			{
				if (checkReplicationError)
				{
					if (debugPrint || _logger.isDebugEnabled())
						System.out.println("##### sendAlarmRequest(" + getName() + "): ReplicationError: errorCode='" + errorCode + "', Message='" + StringUtil.truncate(message.replace('\n', ' '), 120, true, null) + "'.");

					AlarmEvent ae = new AlarmEventErrorLogEntry(this, AlarmEvent.Severity.WARNING, errorCode, alarmText);
					ae.setExtendedDescription(extendedDescText, extendedDescHtml);

					// Information about how to disable this alarm
					ae.createAlarmOptionsMessage(this, "ReplicationError");

					AlarmHandler.getInstance().addAlarm(ae);
				}
				continue;
			}

			//-------------------------------------------------------
			// Deadlock: MY-012468 'Transactions deadlock detected, dumping detailed information.' (the details are in the rows after it, MY-012469)
			// Only written if innodb_print_all_deadlocks=ON and log_error_verbosity=3
			//-------------------------------------------------------
			if ("MY-012468".equals(errorCode))
			{
				if (checkDeadlock)
				{
					if (debugPrint || _logger.isDebugEnabled())
						System.out.println("##### sendAlarmRequest(" + getName() + "): Deadlock: Message='" + StringUtil.truncate(message.replace('\n', ' '), 120, true, null) + "'.");

					AlarmEvent ae = new AlarmEventErrorLogEntry(this, AlarmEvent.Severity.WARNING, errorCode, alarmText);
					ae.setExtendedDescription(extendedDescText, extendedDescHtml);

					// Information about how to disable this alarm
					ae.createAlarmOptionsMessage(this, "Deadlock");

					AlarmHandler.getInstance().addAlarm(ae);
				}
				continue;
			}

			//-------------------------------------------------------
			// Error: any other message with Prio 'Error' (out of disk, can't open a table, a crashed table...). Skip the ones that matches the regex
			//-------------------------------------------------------
			if (checkError && "Error".equals(prio))
			{
				boolean isSkipped = errorSkipPattern != null && (errorSkipPattern.matcher(errorCode).find() || errorSkipPattern.matcher(message).find());

				if (debugPrint || _logger.isDebugEnabled())
					System.out.println("##### sendAlarmRequest(" + getName() + "): Error: isSkipped=" + isSkipped + ", errorCode='" + errorCode + "', Message='" + StringUtil.truncate(message.replace('\n', ' '), 120, true, null) + "'.");

				if ( ! isSkipped )
				{
					AlarmEvent ae = new AlarmEventErrorLogEntry(this, AlarmEvent.Severity.WARNING, errorCode, alarmText);
					ae.setExtendedDescription(extendedDescText, extendedDescHtml);

					// Information about how to disable this alarm
					ae.createAlarmOptionsMessage(this, "Error");

					AlarmHandler.getInstance().addAlarm(ae);
				}
			}
		} // end: rows loop
	}

	public static final String  PROPKEY_alarm_ReplicationError = CM_NAME + ".alarm.system.on.ReplicationError";
	public static final boolean DEFAULT_alarm_ReplicationError = true;

	public static final String  PROPKEY_alarm_Deadlock         = CM_NAME + ".alarm.system.on.Deadlock";
	public static final boolean DEFAULT_alarm_Deadlock         = true;

	public static final String  PROPKEY_alarm_Error            = CM_NAME + ".alarm.system.on.Error";
	public static final boolean DEFAULT_alarm_Error            = true;

	// The error code (MY-######) or the message. Empty = skip nothing
	public static final String  PROPKEY_alarm_ErrorSkipRegex   = CM_NAME + ".alarm.system.on.Error.skip.regex";
	public static final String  DEFAULT_alarm_ErrorSkipRegex   = "";

	@Override
	public List<CmSettingsHelper> getLocalAlarmSettings()
	{
		Configuration conf = Configuration.getCombinedConfiguration();
		List<CmSettingsHelper> list = new ArrayList<>();

		CmSettingsHelper.Type isAlarmSwitch = CmSettingsHelper.Type.IS_ALARM_SWITCH;

		list.add(new CmSettingsHelper("ReplicationError", isAlarmSwitch, PROPKEY_alarm_ReplicationError, Boolean.class, conf.getBooleanProperty(PROPKEY_alarm_ReplicationError, DEFAULT_alarm_ReplicationError), DEFAULT_alarm_ReplicationError, "On a message with Prio 'Error' from replication (Subsystem 'Repl'), send 'AlarmEventErrorLogEntry' with severity WARNING." ));
		list.add(new CmSettingsHelper("Deadlock"        , isAlarmSwitch, PROPKEY_alarm_Deadlock        , Boolean.class, conf.getBooleanProperty(PROPKEY_alarm_Deadlock        , DEFAULT_alarm_Deadlock        ), DEFAULT_alarm_Deadlock        , "On a deadlock (MY-012468), send 'AlarmEventErrorLogEntry' with severity WARNING. Only written if innodb_print_all_deadlocks=ON and log_error_verbosity=3." ));
		list.add(new CmSettingsHelper("Error"           , isAlarmSwitch, PROPKEY_alarm_Error           , Boolean.class, conf.getBooleanProperty(PROPKEY_alarm_Error           , DEFAULT_alarm_Error           ), DEFAULT_alarm_Error           , "On any other message with Prio 'Error', send 'AlarmEventErrorLogEntry' with severity WARNING." ));
		list.add(new CmSettingsHelper("Error Skip Regex",                PROPKEY_alarm_ErrorSkipRegex  , String .class, conf.getProperty       (PROPKEY_alarm_ErrorSkipRegex  , DEFAULT_alarm_ErrorSkipRegex  ), DEFAULT_alarm_ErrorSkipRegex  , "If 'Error' is true; Skip it if the error code (MY-######) or the message matches this regexp. Empty = skip nothing.", new RegExpInputValidator()));

		return list;
	}
}
