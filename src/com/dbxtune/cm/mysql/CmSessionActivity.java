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
package com.dbxtune.cm.mysql;

import java.lang.invoke.MethodHandles;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import javax.naming.NameNotFoundException;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.dbxtune.ICounterController;
import com.dbxtune.IGuiController;
import com.dbxtune.alarm.AlarmHandler;
import com.dbxtune.alarm.events.AlarmDescriptionProviderBase;
import com.dbxtune.alarm.events.AlarmEvent;
import com.dbxtune.alarm.events.AlarmEventChattySession;
import com.dbxtune.cm.CmSettingsHelper;
import com.dbxtune.cm.CmSettingsHelper.RegExpInputValidator;
import com.dbxtune.cm.CounterSample;
import com.dbxtune.cm.CounterSetTemplates;
import com.dbxtune.cm.CounterSetTemplates.Type;
import com.dbxtune.cm.CountersModel;
import com.dbxtune.config.dict.MonTablesDictionary;
import com.dbxtune.config.dict.MonTablesDictionaryManager;
import com.dbxtune.gui.MainFrame;
import com.dbxtune.sql.conn.DbxConnection;
import com.dbxtune.sql.conn.info.DbmsVersionInfo;
import com.dbxtune.utils.Configuration;
import com.dbxtune.utils.StringUtil;

/**
 * What each session (connection) has done: client statements, rows read, bytes... per sample (performance_schema.status_by_thread, joined to threads).
 * <p>
 * The 'Sessions' tab (CmSysSession) shows what a session does <b>right now</b> (the values of sys.x$session are for the current statement),
 * this one has counters that only grow while the session is connected, so Diff and Rate are what the session did in the sample.
 * <p>
 * Used for the alarm 'ChattySession': one session sends very many tiny statements (a possible N+1 application).
 */
public class CmSessionActivity
extends CountersModel
{
	private static final Logger _logger = LogManager.getLogger(MethodHandles.lookup().lookupClass());
	private static final long    serialVersionUID = 1L;

	public static final String   CM_NAME          = CmSessionActivity.class.getSimpleName();
	public static final String   SHORT_NAME       = "Session Activity";
	public static final String   HTML_DESC        =
		"<html>"
		+ "<h4>Session Activity</h4>"
		+ "What each session (connection) has done: statements sent by the client, rows read, bytes sent and received... (performance_schema.status_by_thread).<br>"
		+ "The values are counters for the session since it connected, so Diff and Rate are what the session did in the sample.<br>"
		+ "See the tab 'Sessions' for what the sessions run <b>right now</b>.<br>"
		+ "<br>"
		+ "Used for the alarm <b>ChattySession</b>: one session sends very many tiny statements (a possible <b>N+1</b> application).<br>"
		+ "<b>Note</b>: a collector configuration that lists its Counter Models (like <code>mysql.GENERIC.conf</code> for DbxCentral) must have this Counter Model in it, otherwise it is not sampled (and there is no alarm).<br>"
		+ "</html>";

	public static final String   GROUP_NAME       = MainFrame.TCP_GROUP_SERVER;
	public static final String   GUI_ICON_FILE    = "images/CmSysSession.png";

	public static final long     NEED_SRV_VERSION = 0;
	public static final long     NEED_CE_VERSION  = 0;

	public static final String[] MON_TABLES       = new String[] {CM_NAME, "status_by_thread", "threads"};
	public static final String[] NEED_ROLES       = new String[] {};
	public static final String[] NEED_CONFIG      = new String[] {};

	public static final String[] PCT_COLUMNS      = new String[] {};
	public static final String[] DIFF_COLUMNS     = new String[] {
			"Questions",
			"HandlerReads",
			"HandlerWrites",
			"BytesReceived",
			"BytesSent",
			"SlowQueries",
			"SelectScan",
			"SelectFullJoin",
			"TmpDiskTables",
			"SortMergePasses"
			};

	public static final boolean  NEGATIVE_DIFF_COUNTERS_TO_ZERO = true;
	public static final boolean  IS_SYSTEM_CM                   = true;
	public static final int      DEFAULT_POSTPONE_TIME          = 0;
	public static final int      DEFAULT_QUERY_TIMEOUT          = CountersModel.DEFAULT_sqlQueryTimeout;

	@Override public int     getDefaultPostponeTime()                 { return DEFAULT_POSTPONE_TIME; }
	@Override public int     getDefaultQueryTimeout()                 { return DEFAULT_QUERY_TIMEOUT; }
	@Override public boolean getDefaultIsNegativeDiffCountersToZero() { return NEGATIVE_DIFF_COUNTERS_TO_ZERO; }
	@Override public Type    getTemplateLevel()                       { return Type.MEDIUM; }

	/**
	 * FACTORY  method to create the object
	 */
	public static CountersModel create(ICounterController counterController, IGuiController guiController)
	{
		if (guiController != null && guiController.hasGUI())
			guiController.splashWindowProgress("Loading: Counter Model '" + CM_NAME + "'");

		return new CmSessionActivity(counterController, guiController);
	}

	public CmSessionActivity(ICounterController counterController, IGuiController guiController)
	{
		super(counterController, guiController,
				CM_NAME, GROUP_NAME, /*sql*/null, /*pkList*/null,
				DIFF_COLUMNS, PCT_COLUMNS, MON_TABLES,
				NEED_ROLES, NEED_CONFIG, NEED_SRV_VERSION, NEED_CE_VERSION,
				NEGATIVE_DIFF_COUNTERS_TO_ZERO, IS_SYSTEM_CM, DEFAULT_POSTPONE_TIME);

		setDisplayName(SHORT_NAME);
		setDescription(HTML_DESC);

		setIconFile(GUI_ICON_FILE);

		setShowClearTime(false);

		setCounterController(counterController);
		setGuiController(guiController);

		CounterSetTemplates.register(this);
	}


	//------------------------------------------------------------
	// Implementation
	//------------------------------------------------------------

	@Override
	public String[] getDependsOnConfigForVersion(DbxConnection conn, DbmsVersionInfo versionInfo)
	{
		return NEED_CONFIG;
	}

	@Override
	public void addMonTableDictForVersion(DbxConnection conn, DbmsVersionInfo versionInfo)
	{
		try
		{
			MonTablesDictionary mtd = MonTablesDictionaryManager.getInstance();
			mtd.addTable(CM_NAME, "What each session (connection) has done, from performance_schema.status_by_thread and threads.");

			mtd.addColumn(CM_NAME, "THREAD_ID",       "<html>The performance_schema thread id (not reused by a new connection).</html>");
			mtd.addColumn(CM_NAME, "PROCESSLIST_ID",  "<html>The connection id: the same as in SHOW PROCESSLIST, and CONNECTION_ID() in the session.</html>");
			mtd.addColumn(CM_NAME, "USER",            "<html>The user the session is logged in as.</html>");
			mtd.addColumn(CM_NAME, "HOST",            "<html>The host the client connects from.</html>");
			mtd.addColumn(CM_NAME, "DB",              "<html>The default database (schema) of the session, or NULL.</html>");
			mtd.addColumn(CM_NAME, "COMMAND",         "<html>What the session does: Sleep (waits for the client), Query (runs a statement), Binlog Dump...</html>");
			mtd.addColumn(CM_NAME, "TIME",            "<html>Seconds in the current command.</html>");
			mtd.addColumn(CM_NAME, "CONNECTION_TYPE", "<html>TCP/IP, SSL/TLS, Socket, Named Pipe, Shared Memory.</html>");
			mtd.addColumn(CM_NAME, "PROGRAM_NAME",    "<html>The client program name (connection attribute 'program_name', set by the client: for example <code>connectionAttributes=program_name:myapp</code> in Connector/J). NULL if the client does not send it.</html>");
			mtd.addColumn(CM_NAME, "IS_OWN_SESSION",  "<html>Y = this is the session DbxTune collects the counters with.</html>");
			mtd.addColumn(CM_NAME, "Questions",       "<html>Statements <b>sent by the client</b> (status 'Questions'): with server side prepared statements, every execute is one. <br>Statements run inside stored procedures, functions and triggers are NOT counted (one CALL is one).</html>");
			mtd.addColumn(CM_NAME, "HandlerReads",    "<html>Rows read from the storage engines (sum of the status 'Handler_read_*': key, next, prev, first, last, rnd, rnd_next). <br>The nearest thing to 'logical reads' in MySQL.</html>");
			mtd.addColumn(CM_NAME, "HandlerWrites",   "<html>Rows inserted, updated and deleted (sum of the status 'Handler_write', 'Handler_update', 'Handler_delete'). <br>Note: Handler_write also counts rows written to internal temporary tables.</html>");
			mtd.addColumn(CM_NAME, "ReadsPerQuestion","<html>HandlerReads / Questions. In Diff and Rate: for the statements in the sample. <br>A few = cheap lookups on a key (one N+1 child statement is about 1).</html>");
			mtd.addColumn(CM_NAME, "BytesReceived",   "<html>Bytes received from the client.</html>");
			mtd.addColumn(CM_NAME, "BytesSent",       "<html>Bytes sent to the client.</html>");
			mtd.addColumn(CM_NAME, "SlowQueries",     "<html>Statements that took more than long_query_time seconds.</html>");
			mtd.addColumn(CM_NAME, "SelectScan",      "<html>Joins that did a full scan of the first table.</html>");
			mtd.addColumn(CM_NAME, "SelectFullJoin",  "<html>Joins that did table scans because they do not use indexes.</html>");
			mtd.addColumn(CM_NAME, "TmpDiskTables",   "<html>Internal temporary tables created on disk.</html>");
			mtd.addColumn(CM_NAME, "SortMergePasses", "<html>Merge passes the sort algorithm had to do (sort_buffer_size too small for the sort).</html>");
		}
		catch (NameNotFoundException e)
		{
			_logger.warn("Problems in cm='" + CM_NAME + "', adding addMonTableDictForVersion. Caught: " + e);
		}
	}

	/** If we can't read the views: disable the CM, and tell what to do about it */
	@Override
	public boolean checkDependsOnOther(DbxConnection conn)
	{
		return MySqlCmUtils.checkViewsAreReadable(this, conn, "performance_schema.status_by_thread", "performance_schema.threads", "performance_schema.session_connect_attrs");
	}

	@Override
	public List<String> getPkForVersion(DbxConnection conn, DbmsVersionInfo versionInfo)
	{
		List <String> pkCols = new LinkedList<String>();

		pkCols.add("THREAD_ID");

		return pkCols;
	}

	@Override
	public String getSqlForVersion(DbxConnection conn, DbmsVersionInfo versionInfo)
	{
		// status_by_thread has one row per session and status variable: pivot the ones we want (measured on 8.4 with 143 sessions: 4-5 ms per sample)
		// Note: events_statements_summary_by_thread_by_event_name has the statements per session too, but it was 3 times as expensive (15 ms at 143 sessions),
		//       and with server side prepared statements the executes are 'statement/com/Execute', not 'statement/sql/...'. 'Questions' counts both.
		// Note: Com_* are not in status_by_thread (MySQL 8)
		String sql = ""
			+ "select /* ${cmCollectorName} */ \n"
			+ "     t.THREAD_ID \n"
			+ "    ,t.PROCESSLIST_ID \n"
			+ "    ,t.PROCESSLIST_USER                                        as USER \n"
			+ "    ,t.PROCESSLIST_HOST                                        as HOST \n"
			+ "    ,t.PROCESSLIST_DB                                          as DB \n"
			+ "    ,t.PROCESSLIST_COMMAND                                     as COMMAND \n"
			+ "    ,t.PROCESSLIST_TIME                                        as TIME \n"
			+ "    ,t.CONNECTION_TYPE \n"
			+ "    ,(select a.ATTR_VALUE from performance_schema.session_connect_attrs a where a.PROCESSLIST_ID = t.PROCESSLIST_ID and a.ATTR_NAME = 'program_name' limit 1) as PROGRAM_NAME \n"
			+ "    ,CASE WHEN t.PROCESSLIST_ID = CONNECTION_ID() THEN 'Y' ELSE 'N' END as IS_OWN_SESSION \n"
			+ "    ,IFNULL(s.Questions,       0)                              as Questions \n"
			+ "    ,IFNULL(s.HandlerReads,    0)                              as HandlerReads \n"
			+ "    ,IFNULL(s.HandlerWrites,   0)                              as HandlerWrites \n"
			+ "    ,CAST(IFNULL(s.HandlerReads, 0) / NULLIF(s.Questions, 0) as DECIMAL(18,1)) as ReadsPerQuestion \n"
			+ "    ,IFNULL(s.BytesReceived,   0)                              as BytesReceived \n"
			+ "    ,IFNULL(s.BytesSent,       0)                              as BytesSent \n"
			+ "    ,IFNULL(s.SlowQueries,     0)                              as SlowQueries \n"
			+ "    ,IFNULL(s.SelectScan,      0)                              as SelectScan \n"
			+ "    ,IFNULL(s.SelectFullJoin,  0)                              as SelectFullJoin \n"
			+ "    ,IFNULL(s.TmpDiskTables,   0)                              as TmpDiskTables \n"
			+ "    ,IFNULL(s.SortMergePasses, 0)                              as SortMergePasses \n"
			+ "from performance_schema.threads t \n"
			+ "left join ( \n"
			+ "    select THREAD_ID \n"
			+ "        ,CAST(SUM(CASE WHEN VARIABLE_NAME = 'Questions'                                          THEN VARIABLE_VALUE END) as SIGNED) as Questions \n"
			+ "        ,CAST(SUM(CASE WHEN VARIABLE_NAME LIKE 'Handler_read%'                                   THEN VARIABLE_VALUE END) as SIGNED) as HandlerReads \n"
			+ "        ,CAST(SUM(CASE WHEN VARIABLE_NAME IN ('Handler_write','Handler_update','Handler_delete') THEN VARIABLE_VALUE END) as SIGNED) as HandlerWrites \n"
			+ "        ,CAST(SUM(CASE WHEN VARIABLE_NAME = 'Bytes_received'                                     THEN VARIABLE_VALUE END) as SIGNED) as BytesReceived \n"
			+ "        ,CAST(SUM(CASE WHEN VARIABLE_NAME = 'Bytes_sent'                                         THEN VARIABLE_VALUE END) as SIGNED) as BytesSent \n"
			+ "        ,CAST(SUM(CASE WHEN VARIABLE_NAME = 'Slow_queries'                                       THEN VARIABLE_VALUE END) as SIGNED) as SlowQueries \n"
			+ "        ,CAST(SUM(CASE WHEN VARIABLE_NAME = 'Select_scan'                                        THEN VARIABLE_VALUE END) as SIGNED) as SelectScan \n"
			+ "        ,CAST(SUM(CASE WHEN VARIABLE_NAME = 'Select_full_join'                                   THEN VARIABLE_VALUE END) as SIGNED) as SelectFullJoin \n"
			+ "        ,CAST(SUM(CASE WHEN VARIABLE_NAME = 'Created_tmp_disk_tables'                            THEN VARIABLE_VALUE END) as SIGNED) as TmpDiskTables \n"
			+ "        ,CAST(SUM(CASE WHEN VARIABLE_NAME = 'Sort_merge_passes'                                  THEN VARIABLE_VALUE END) as SIGNED) as SortMergePasses \n"
			+ "    from performance_schema.status_by_thread \n"
			+ "    where VARIABLE_NAME in ('Questions', 'Handler_read_first', 'Handler_read_key', 'Handler_read_last', 'Handler_read_next', 'Handler_read_prev', 'Handler_read_rnd', 'Handler_read_rnd_next', \n"
			+ "                            'Handler_write', 'Handler_update', 'Handler_delete', 'Bytes_received', 'Bytes_sent', 'Slow_queries', 'Select_scan', 'Select_full_join', 'Created_tmp_disk_tables', 'Sort_merge_passes') \n"
			+ "    group by THREAD_ID \n"
			+ ") s on s.THREAD_ID = t.THREAD_ID \n"
			+ "where t.TYPE = 'FOREGROUND' \n"
			+ "  and t.PROCESSLIST_ID is not null \n"
			+ "";

		return sql;
	}

	/** ReadsPerQuestion in Diff and Rate: for the statements in the sample (not since the session connected) */
	@Override
	public void localCalculation(CounterSample prevSample, CounterSample newSample, CounterSample diffData)
	{
		int pos_Questions        = diffData.findColumn("Questions");
		int pos_HandlerReads     = diffData.findColumn("HandlerReads");
		int pos_ReadsPerQuestion = diffData.findColumn("ReadsPerQuestion");
		if (pos_Questions == -1 || pos_HandlerReads == -1 || pos_ReadsPerQuestion == -1)
			return;

		for (int r = 0; r < diffData.getRowCount(); r++)
		{
			long questions = diffData.getValueAsLong(r, pos_Questions, 0L);
			BigDecimal val = null;
			if (questions > 0)
				val = new BigDecimal(diffData.getValueAsDouble(r, pos_HandlerReads, 0d) / questions).setScale(1, RoundingMode.HALF_EVEN);
			diffData.setValueAt(val, r, pos_ReadsPerQuestion);
		}
	}

	@Override
	public void sendAlarmRequest()
	{
		if ( ! hasRateData() )
			return;

		if ( ! AlarmHandler.hasInstance() )
			return;

		if ( ! isSystemAlarmsForColumnEnabledAndInTimeRange("ChattySession") )
		{
			_chattySessionSamples.clear();
			return;
		}

		boolean debugPrint = Configuration.getCombinedConfiguration().getBooleanProperty("sendAlarmRequest.debug", _logger.isDebugEnabled());

		AlarmHandler alarmHandler = AlarmHandler.getInstance();

		CountersModel cm = this;

		//-------------------------------------------------------
		// ChattySession: one session sends very many tiny statements (possible N+1)
		// Requests = status 'Questions': the statements the client sends (also every execute of a server side prepared statement).
		// 'Queries' and the statement summaries per thread also count the statements in stored procedures (measured on 8.4: one CALL of
		// a procedure doing 1000 SELECTs = Questions +1, statement/sp/stmt +1000), so a loop in a procedure is not taken as a chatty client.
		// Above the threshold for ## samples in a row, and few rows read (Handler_read_*) per statement
		//-------------------------------------------------------
		Configuration conf = Configuration.getCombinedConfiguration();
		double threshold          = conf.getDoubleProperty(PROPKEY_alarm_ChattySession,                   DEFAULT_alarm_ChattySession);
		double maxReadsPerRequest = conf.getDoubleProperty(PROPKEY_alarm_ChattySessionMaxReadsPerRequest, DEFAULT_alarm_ChattySessionMaxReadsPerRequest);
		int    minSamples         = conf.getIntProperty   (PROPKEY_alarm_ChattySessionSamples,            DEFAULT_alarm_ChattySessionSamples);
		String skipUserRegExp     = conf.getProperty      (PROPKEY_alarm_ChattySessionSkipUser,           DEFAULT_alarm_ChattySessionSkipUser);
		String skipProgramRegExp  = conf.getProperty      (PROPKEY_alarm_ChattySessionSkipProgram,        DEFAULT_alarm_ChattySessionSkipProgram);

		// THREAD_ID -> samples in a row above the threshold (sessions that are below now are forgotten)
		Map<String, Integer> samplesAbove = new HashMap<>();

		for (int r=0; r<cm.getRateRowCount(); r++)
		{
			Double requestsPerSec = cm.getRateValueAsDouble(r, "Questions");
			if (requestsPerSec == null || requestsPerSec <= threshold)
				continue;

			// The values as they are (getRateString() formats numbers with digit grouping: '1 234')
			String threadId = String.valueOf(cm.getRateValue(r, "THREAD_ID"));
			String connId   = String.valueOf(cm.getRateValue(r, "PROCESSLIST_ID"));
			String user     = cm.getRateString(r, "USER");
			String host     = cm.getRateString(r, "HOST");
			String program  = cm.getRateString(r, "PROGRAM_NAME");

			// No user = a server thread. And DbxTune's own session
			if (StringUtil.isNullOrBlank(user) || "system user".equals(user) || "event_scheduler".equals(user))
				continue;
			if ("Y".equals(cm.getRateString(r, "IS_OWN_SESSION")))
				continue;
			if (StringUtil.hasValue(skipUserRegExp) && user.matches(skipUserRegExp))
				continue;
			if (StringUtil.hasValue(skipProgramRegExp) && program != null && program.matches(skipProgramRegExp))
				continue;

			// Cheap statements only: a session with many statements that each do real work is a batch job, not N+1
			Double readsPerSec = cm.getRateValueAsDouble(r, "HandlerReads");
			double readsPerRequest = readsPerSec == null ? 0 : readsPerSec / requestsPerSec;
			if (readsPerRequest >= maxReadsPerRequest)
				continue;

			Integer prev  = _chattySessionSamples.get(threadId);
			int     count = (prev == null ? 0 : prev) + 1;
			samplesAbove.put(threadId, count);

			if (debugPrint || _logger.isDebugEnabled())
				System.out.println("##### sendAlarmRequest(" + cm.getName() + "): ChattySession: THREAD_ID=" + threadId + ", PROCESSLIST_ID=" + connId + ", Questions/s=" + requestsPerSec + ", readsPerRequest=" + readsPerRequest + ", samplesAbove=" + count + " (threshold=" + threshold + ", maxReadsPerRequest=" + maxReadsPerRequest + ", minSamples=" + minSamples + ").");

			if (count < minSamples)
				continue;

			String db = cm.getRateString(r, "DB");
			String sessionDesc = "connection_id=" + connId + ", user='" + user + "', host='" + host + "'"
					+ (StringUtil.hasValue(program) ? ", program_name='" + program + "'" : "")
					+ (StringUtil.hasValue(db)      ? ", db='" + db + "'" : "");

			AlarmEvent ae = new AlarmEventChattySession(cm, "connection_id=" + connId, sessionDesc, requestsPerSec, readsPerRequest, count, threshold, maxReadsPerRequest);
			final String alarmThreadId = threadId;
			final String alarmConnId   = connId;

			String extendedDescText = cm.toTextTableString(DATA_RATE, r);
			String extendedDescHtml = cm.toHtmlTableString(DATA_RATE, r, true, false, false);

			// The below is called from AlarmHandler on: Raise, RE-RAISE & CANCEL
			ae.setAlarmDescriptionProvider(this, new AlarmDescriptionProviderBase(null, null, threshold)
			{
				@Override
				public void setValues(CountersModel cm, AlarmEvent alarmEvent, AlarmPhase phase)
				{
					// Always: (RAISE, RE-RAISE, CANCEL) - the session row from when it was (re)raised
					alarmEvent.setExtendedDescription(extendedDescText, extendedDescHtml);

					// Set CANCEL message (values that was found AFTER last raise/re-raise event)
					if (AlarmPhase.CANCEL.equals(phase))
					{
						int    rowId = cm.getRowIdWhere(DATA_RATE, "THREAD_ID", alarmThreadId);
						String label = "connection_id " + alarmConnId;

						if (rowId == -1)
							alarmEvent.setCancelDescription(getAlarmCancelTextGone(label));
						else
							alarmEvent.setCancelDescription(getAlarmCancelText(label + ": statements per second", toAlarmCancelValue(cm.getRateValueAsDouble(rowId, "Questions")), "/s"));
					}
				}
			});

			// Information about how to disable this alarm
			ae.createAlarmOptionsMessage(this, "ChattySession");

			alarmHandler.addAlarm( ae );
		}
		_chattySessionSamples = samplesAbove;
	}

	/** ChattySession alarm: THREAD_ID -> number of samples in a row above the threshold */
	private Map<String, Integer> _chattySessionSamples = new HashMap<>();

	public static final String  PROPKEY_alarm_ChattySession                    = CM_NAME + ".alarm.system.if.ChattySession.gt";
	public static final double  DEFAULT_alarm_ChattySession                    = 100;

	public static final String  PROPKEY_alarm_ChattySessionMaxReadsPerRequest  = CM_NAME + ".alarm.system.if.ChattySession.maxReadsPerRequest";
	public static final double  DEFAULT_alarm_ChattySessionMaxReadsPerRequest  = 10;

	public static final String  PROPKEY_alarm_ChattySessionSamples             = CM_NAME + ".alarm.system.if.ChattySession.samples";
	public static final int     DEFAULT_alarm_ChattySessionSamples             = 3;

	public static final String  PROPKEY_alarm_ChattySessionSkipUser            = CM_NAME + ".alarm.system.if.ChattySession.skip.user";
	public static final String  DEFAULT_alarm_ChattySessionSkipUser            = "";

	public static final String  PROPKEY_alarm_ChattySessionSkipProgram         = CM_NAME + ".alarm.system.if.ChattySession.skip.program";
	public static final String  DEFAULT_alarm_ChattySessionSkipProgram         = "";

	@Override
	public List<CmSettingsHelper> getLocalAlarmSettings()
	{
		Configuration conf = Configuration.getCombinedConfiguration();
		CmSettingsHelper.Type isAlarmSwitch = CmSettingsHelper.Type.IS_ALARM_SWITCH;

		List<CmSettingsHelper> list = new ArrayList<>();

		list.add(new CmSettingsHelper("ChattySession",                isAlarmSwitch, PROPKEY_alarm_ChattySession                   , Double .class, conf.getDoubleProperty(PROPKEY_alarm_ChattySession                  , DEFAULT_alarm_ChattySession                  ), DEFAULT_alarm_ChattySession                  , "If any session sends more than ## statements per second (column 'Questions') with few rows read each, for several samples in a row, then send alarm 'AlarmEventChattySession'. This is often an application doing N+1 (one tiny query per row of another query). It can also be a busy normal session (a connection pool, a queue consumer): confirm N+1 in the Daily Summary Report section 'Possible N+1 / Chatty Statements', which matches the repeated statement to the statement whose rows drive it." ));
		list.add(new CmSettingsHelper("ChattySession MaxReadsPerRequest",            PROPKEY_alarm_ChattySessionMaxReadsPerRequest , Double .class, conf.getDoubleProperty(PROPKEY_alarm_ChattySessionMaxReadsPerRequest, DEFAULT_alarm_ChattySessionMaxReadsPerRequest), DEFAULT_alarm_ChattySessionMaxReadsPerRequest, "If 'ChattySession' is true; Only if the session reads fewer than ## rows (HandlerReads) per statement (many cheap statements, not a batch job doing real work)." ));
		list.add(new CmSettingsHelper("ChattySession Samples",                       PROPKEY_alarm_ChattySessionSamples            , Integer.class, conf.getIntProperty   (PROPKEY_alarm_ChattySessionSamples           , DEFAULT_alarm_ChattySessionSamples           ), DEFAULT_alarm_ChattySessionSamples           , "If 'ChattySession' is true; The session must be above the threshold in ## samples in a row." ));
		list.add(new CmSettingsHelper("ChattySession SkipUsers",                     PROPKEY_alarm_ChattySessionSkipUser           , String .class, conf.getProperty      (PROPKEY_alarm_ChattySessionSkipUser          , DEFAULT_alarm_ChattySessionSkipUser          ), DEFAULT_alarm_ChattySessionSkipUser          , "If 'ChattySession' is true; Discard USERs listed (regexp is used)." , new RegExpInputValidator()));
		list.add(new CmSettingsHelper("ChattySession SkipPrograms",                  PROPKEY_alarm_ChattySessionSkipProgram        , String .class, conf.getProperty      (PROPKEY_alarm_ChattySessionSkipProgram       , DEFAULT_alarm_ChattySessionSkipProgram       ), DEFAULT_alarm_ChattySessionSkipProgram       , "If 'ChattySession' is true; Discard PROGRAM_NAMEs listed (regexp is used). DbxTune's own session is always discarded." , new RegExpInputValidator()));

		return list;
	}
}
