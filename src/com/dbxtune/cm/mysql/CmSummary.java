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
import java.util.LinkedList;
import java.util.List;

import javax.swing.JPanel;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.dbxtune.ICounterController;
import com.dbxtune.IGuiController;
import com.dbxtune.alarm.AlarmHandler;
import com.dbxtune.alarm.events.AlarmDescriptionProviderBase;
import com.dbxtune.alarm.events.AlarmEvent;
import com.dbxtune.alarm.events.AlarmEventBlockingLockAlarm;
import com.dbxtune.alarm.events.mysql.AlarmEventMyLongRunningTransaction;
import com.dbxtune.central.pcs.CentralPersistReader;
import com.dbxtune.cm.CmSettingsHelper;
import com.dbxtune.cm.CmSummaryAbstract;
import com.dbxtune.cm.CounterSample;
import com.dbxtune.cm.CounterSetTemplates;
import com.dbxtune.cm.CounterSetTemplates.Type;
import com.dbxtune.cm.CountersModel;
import com.dbxtune.cm.mysql.gui.CmSummaryPanel;
import com.dbxtune.graph.TrendGraphDataPoint;
import com.dbxtune.graph.TrendGraphDataPoint.LabelType;
import com.dbxtune.sql.conn.DbxConnection;
import com.dbxtune.sql.conn.info.DbmsVersionInfo;
import com.dbxtune.sql.conn.info.DbmsVersionInfoMySql;
import com.dbxtune.utils.Configuration;

/**
 * @author Goran Schwarz (goran_schwarz@hotmail.com)
 */
public class CmSummary
extends CmSummaryAbstract
{
	private static final Logger _logger = LogManager.getLogger(MethodHandles.lookup().lookupClass());
	private static final long    serialVersionUID = 1L;

	public static final String   CM_NAME          = CmSummary.class.getSimpleName();
	public static final String   SHORT_NAME       = "Summary";
	public static final String   HTML_DESC        =
		"<html>" +
		"Overview of how the system performs: connections, statements, InnoDB rows and buffer pool, redo, blocking and the oldest open transaction." +
		"</html>";

	public static final String   GROUP_NAME       = null;
	public static final String   GUI_ICON_FILE    = "images/" + CM_NAME + ".png";

	public static final long     NEED_SRV_VERSION = 0;
	public static final long     NEED_CE_VERSION  = 0;

	public static final String[] MON_TABLES       = new String[] {CM_NAME, "global_status", "INNODB_TRX", "INNODB_METRICS", "metadata_locks"};
	public static final String[] NEED_ROLES       = new String[] {};
	public static final String[] NEED_CONFIG      = new String[] {};

	public static final String[] PCT_COLUMNS      = new String[] {};
	// Counters (they only grow) + a few gauges where the panel shows the change (like the other DbxTune's), the graphs reads the absolute value of the gauges
	public static final String[] DIFF_COLUMNS     = new String[] {
		"THREADS_CONNECTED",
		"QUESTIONS", "COM_SELECT", "COM_INSERT", "COM_UPDATE", "COM_DELETE", "HANDLER_COMMIT", "HANDLER_ROLLBACK",
		"CONNECTIONS", "ABORTED_CONNECTS", "ABORTED_CLIENTS", "CONN_ERRORS_MAX_CONN",
		"SLOW_QUERIES", "CREATED_TMP_TABLES", "CREATED_TMP_DISK_TABLES", "SELECT_FULL_JOIN", "SELECT_SCAN", "SORT_MERGE_PASSES",
		"ROWS_READ", "ROWS_INSERTED", "ROWS_UPDATED", "ROWS_DELETED",
		"BP_READ_REQUESTS", "BP_READS", "BP_WAIT_FREE", "BP_PAGES_FLUSHED",
		"REDO_BYTES", "DATA_READ_BYTES", "DATA_WRITTEN_BYTES",
		"ROW_LOCK_WAITS", "ROW_LOCK_TIME_MS", "DEADLOCKS",
		"BYTES_RECEIVED", "BYTES_SENT", "TABLE_OPEN_CACHE_MISSES", "BINLOG_CACHE_DISK_USE",
		"COUNT_BLOCKING_WAIT", "MAX_BLOCKING_WAIT_IN_SEC"
	};

	public static final boolean  NEGATIVE_DIFF_COUNTERS_TO_ZERO = false; // some gauges are in DIFF (for the panel), there a negative diff is correct
	public static final boolean  IS_SYSTEM_CM                   = true;
	public static final int      DEFAULT_POSTPONE_TIME          = 0;
	public static final int      DEFAULT_QUERY_TIMEOUT          = CountersModel.DEFAULT_sqlQueryTimeout;

	@Override public int     getDefaultPostponeTime()                 { return DEFAULT_POSTPONE_TIME; }
	@Override public int     getDefaultQueryTimeout()                 { return DEFAULT_QUERY_TIMEOUT; }
	@Override public boolean getDefaultIsNegativeDiffCountersToZero() { return NEGATIVE_DIFF_COUNTERS_TO_ZERO; }
	@Override public Type    getTemplateLevel()                       { return Type.SMALL; }

	/**
	 * FACTORY  method to create the object
	 */
	public static CountersModel create(ICounterController counterController, IGuiController guiController)
	{
		if (guiController != null && guiController.hasGUI())
			guiController.splashWindowProgress("Loading: Counter Model '" + CM_NAME + "'");

		return new CmSummary(counterController, guiController);
	}

	public CmSummary(ICounterController counterController, IGuiController guiController)
	{
		super(counterController, guiController,
				CM_NAME, GROUP_NAME, /*sql*/null, /*pkList*/null,
				DIFF_COLUMNS, PCT_COLUMNS, MON_TABLES,
				NEED_ROLES, NEED_CONFIG, NEED_SRV_VERSION, NEED_CE_VERSION,
				NEGATIVE_DIFF_COUNTERS_TO_ZERO, IS_SYSTEM_CM, DEFAULT_POSTPONE_TIME);

		setDisplayName(SHORT_NAME);
		setDescription(HTML_DESC);

		setIconFile(GUI_ICON_FILE);

		setCounterController(counterController);
		setGuiController(guiController);

		// THIS IS THE SUMMARY CM, so set this
		counterController.setSummaryCm(this);

		addTrendGraphs();
		addPostRefreshTrendGraphs();

		CounterSetTemplates.register(this);
	}


	//------------------------------------------------------------
	// Implementation
	//------------------------------------------------------------
	public static final String GRAPH_NAME_CONNECTION         = "ConnectionsGraph";
	public static final String GRAPH_NAME_STATEMENTS         = "StatementsGraph";
	public static final String GRAPH_NAME_TRANSACTION        = "TransGraph";
	public static final String GRAPH_NAME_INNODB_ROWS        = "InnoDbRowsGraph";
	public static final String GRAPH_NAME_READS              = "ReadsGraph";
	public static final String GRAPH_NAME_BP_HIT_PCT         = "BpHitPctGraph";
	public static final String GRAPH_NAME_REDO               = "RedoGraph";
	public static final String GRAPH_NAME_SQL_PROBLEMS       = "SqlProblemsGraph";
	public static final String GRAPH_NAME_CONN_PROBLEMS      = "ConnProblemsGraph";
	public static final String GRAPH_NAME_BLOCKING_LOCKS     = "BlockingLocksGraph";
	public static final String GRAPH_NAME_OLDEST_TRAN_IN_SEC = "OldestTranInSecGraph";
	public static final String GRAPH_NAME_HISTORY_LIST       = "HistoryListGraph";

	private void addTrendGraphs()
	{
		addTrendGraph(GRAPH_NAME_CONNECTION,
			"Connections and Running Threads",    // Menu CheckBox text
			"Connections and Running Threads (" + SHORT_NAME + ")", // Label
			TrendGraphDataPoint.createGraphProps(TrendGraphDataPoint.Y_AXIS_SCALE_LABELS_NORMAL, CentralPersistReader.SampleType.MAX_OVER_SAMPLES),
			new String[] { "Threads_connected", "Threads_running" },
			LabelType.Static,
			TrendGraphDataPoint.Category.OPERATIONS,
			false, // is Percent Graph
			true,  // visible at start
			0,     // graph is valid from Server Version. 0 = All Versions; >0 = Valid from this version and above
			-1);   // minimum height

		addTrendGraph(GRAPH_NAME_STATEMENTS,
			"Statements per Second",    // Menu CheckBox text
			"Statements per Second: all, and SELECT/INSERT/UPDATE/DELETE (" + SHORT_NAME + ")", // Label
			TrendGraphDataPoint.createGraphProps(TrendGraphDataPoint.Y_AXIS_SCALE_LABELS_PERSEC, CentralPersistReader.SampleType.MAX_OVER_SAMPLES),
			new String[] { "Questions", "Select", "Insert", "Update", "Delete" },
			LabelType.Static,
			TrendGraphDataPoint.Category.OPERATIONS,
			false, // is Percent Graph
			true,  // visible at start
			0,     // graph is valid from Server Version. 0 = All Versions; >0 = Valid from this version and above
			-1);   // minimum height

		addTrendGraph(GRAPH_NAME_TRANSACTION,
			"Commits and Rollbacks per Second",    // Menu CheckBox text
			"Commits and Rollbacks per Second, also autocommit (" + SHORT_NAME + ")", // Label
			TrendGraphDataPoint.createGraphProps(TrendGraphDataPoint.Y_AXIS_SCALE_LABELS_PERSEC, CentralPersistReader.SampleType.MAX_OVER_SAMPLES),
			new String[] { "Commits", "Rollbacks" },
			LabelType.Static,
			TrendGraphDataPoint.Category.OPERATIONS,
			false, // is Percent Graph
			true,  // visible at start
			0,     // graph is valid from Server Version. 0 = All Versions; >0 = Valid from this version and above
			-1);   // minimum height

		addTrendGraph(GRAPH_NAME_INNODB_ROWS,
			"InnoDB Rows per Second",    // Menu CheckBox text
			"InnoDB Rows Read/Inserted/Updated/Deleted per Second (" + SHORT_NAME + ")", // Label
			TrendGraphDataPoint.createGraphProps(TrendGraphDataPoint.Y_AXIS_SCALE_LABELS_PERSEC, CentralPersistReader.SampleType.MAX_OVER_SAMPLES),
			new String[] { "Read", "Inserted", "Updated", "Deleted" },
			LabelType.Static,
			TrendGraphDataPoint.Category.OPERATIONS,
			false, // is Percent Graph
			true,  // visible at start
			0,     // graph is valid from Server Version. 0 = All Versions; >0 = Valid from this version and above
			-1);   // minimum height

		addTrendGraph(GRAPH_NAME_READS,
			"Buffer Pool Reads and Writes per Second",    // Menu CheckBox text
			"Buffer Pool: Logical Reads, Physical Reads (from disk) and Pages Flushed (to disk) per Second (" + SHORT_NAME + ")", // Label
			TrendGraphDataPoint.createGraphProps(TrendGraphDataPoint.Y_AXIS_SCALE_LABELS_PERSEC, CentralPersistReader.SampleType.MAX_OVER_SAMPLES),
			new String[] { "Logical Reads", "Physical Reads", "Pages Flushed" },
			LabelType.Static,
			TrendGraphDataPoint.Category.DISK,
			false, // is Percent Graph
			true,  // visible at start
			0,     // graph is valid from Server Version. 0 = All Versions; >0 = Valid from this version and above
			-1);   // minimum height

		addTrendGraph(GRAPH_NAME_BP_HIT_PCT,
			"Buffer Pool Hit Ratio",    // Menu CheckBox text
			"Buffer Pool Hit Ratio in Percent, logical reads that did not need a disk read (" + SHORT_NAME + ")", // Label
			TrendGraphDataPoint.createGraphProps(TrendGraphDataPoint.Y_AXIS_SCALE_LABELS_PERCENT, CentralPersistReader.SampleType.MIN_OVER_SAMPLES),
			new String[] { "Hit Ratio" },
			LabelType.Static,
			TrendGraphDataPoint.Category.CACHE,
			true,  // is Percent Graph
			false, // visible at start
			0,     // graph is valid from Server Version. 0 = All Versions; >0 = Valid from this version and above
			-1);   // minimum height

		addTrendGraph(GRAPH_NAME_REDO,
			"Redo Written in KB per Second",    // Menu CheckBox text
			"Redo Log Written in KB per Second (" + SHORT_NAME + ")", // Label
			TrendGraphDataPoint.createGraphProps(TrendGraphDataPoint.Y_AXIS_SCALE_LABELS_KB, CentralPersistReader.SampleType.MAX_OVER_SAMPLES),
			new String[] { "Redo KB" },
			LabelType.Static,
			TrendGraphDataPoint.Category.DISK,
			false, // is Percent Graph
			true,  // visible at start
			0,     // graph is valid from Server Version. 0 = All Versions; >0 = Valid from this version and above
			-1);   // minimum height

		addTrendGraph(GRAPH_NAME_SQL_PROBLEMS,
			"Expensive SQL Signs per Second",    // Menu CheckBox text
			"Expensive SQL Signs per Second: slow queries, temp tables on disk, joins without index, sort merge passes (" + SHORT_NAME + ")", // Label
			TrendGraphDataPoint.createGraphProps(TrendGraphDataPoint.Y_AXIS_SCALE_LABELS_PERSEC, CentralPersistReader.SampleType.MAX_OVER_SAMPLES),
			new String[] { "Slow Queries", "Tmp Disk Tables", "Full Joins", "Sort Merge Passes" },
			LabelType.Static,
			TrendGraphDataPoint.Category.OPERATIONS,
			false, // is Percent Graph
			false, // visible at start
			0,     // graph is valid from Server Version. 0 = All Versions; >0 = Valid from this version and above
			-1);   // minimum height

		addTrendGraph(GRAPH_NAME_CONN_PROBLEMS,
			"Failed and Aborted Connections per Second",    // Menu CheckBox text
			"Failed and Aborted Connections per Second (" + SHORT_NAME + ")", // Label
			TrendGraphDataPoint.createGraphProps(TrendGraphDataPoint.Y_AXIS_SCALE_LABELS_PERSEC, CentralPersistReader.SampleType.MAX_OVER_SAMPLES),
			new String[] { "Aborted Connects", "Aborted Clients", "Refused, max_connections" },
			LabelType.Static,
			TrendGraphDataPoint.Category.OPERATIONS,
			false, // is Percent Graph
			false, // visible at start
			0,     // graph is valid from Server Version. 0 = All Versions; >0 = Valid from this version and above
			-1);   // minimum height

		addTrendGraph(GRAPH_NAME_BLOCKING_LOCKS,
			"Blocking Locks",    // Menu CheckBox text
			"Number of Sessions that Waits for a Lock: row locks and metadata locks (" + SHORT_NAME + ")", // Label
			TrendGraphDataPoint.createGraphProps(TrendGraphDataPoint.Y_AXIS_SCALE_LABELS_NORMAL, CentralPersistReader.SampleType.MAX_OVER_SAMPLES),
			new String[] { "Row Lock Waits", "Metadata Lock Waits" },
			LabelType.Static,
			TrendGraphDataPoint.Category.WAITS,
			false, // is Percent Graph
			true,  // visible at start
			0,     // graph is valid from Server Version. 0 = All Versions; >0 = Valid from this version and above
			-1);   // minimum height

		addTrendGraph(GRAPH_NAME_OLDEST_TRAN_IN_SEC,
			"Oldest Open Transaction",    // Menu CheckBox text
			"Oldest Open Transaction, in Seconds (" + SHORT_NAME + ")", // Label
			TrendGraphDataPoint.createGraphProps(TrendGraphDataPoint.Y_AXIS_SCALE_LABELS_SECONDS, CentralPersistReader.SampleType.MAX_OVER_SAMPLES),
			new String[] { "Seconds" },
			LabelType.Static,
			TrendGraphDataPoint.Category.OPERATIONS,
			false, // is Percent Graph
			true,  // visible at start
			0,     // graph is valid from Server Version. 0 = All Versions; >0 = Valid from this version and above
			-1);   // minimum height

		addTrendGraph(GRAPH_NAME_HISTORY_LIST,
			"InnoDB History List Length",    // Menu CheckBox text
			"InnoDB History List Length: old row versions that purge has not removed yet (" + SHORT_NAME + ")", // Label
			TrendGraphDataPoint.createGraphProps(TrendGraphDataPoint.Y_AXIS_SCALE_LABELS_NORMAL, CentralPersistReader.SampleType.MAX_OVER_SAMPLES),
			new String[] { "History List Length" },
			LabelType.Static,
			TrendGraphDataPoint.Category.OPERATIONS,
			false, // is Percent Graph
			false, // visible at start
			0,     // graph is valid from Server Version. 0 = All Versions; >0 = Valid from this version and above
			-1);   // minimum height
	}

	@Override
	protected JPanel createGui()
	{
		setTabPanel(null); // Don't think this is necessary, but lets do it anyway...

		CmSummaryPanel summaryPanel = new CmSummaryPanel(this);

		// THIS IS THE SUMMARY CM, so set this
		getCounterController().setSummaryPanel( summaryPanel );

		// add listener, so that the GUI gets updated when data changes in the CM
		addTableModelListener( summaryPanel );

		return summaryPanel;
	}

	@Override
	public boolean isRefreshable()
	{
		// The SUMMARY should ALWAYS be refreshed
		return true;
	}

	@Override
	public String[] getDependsOnConfigForVersion(DbxConnection conn, DbmsVersionInfo versionInfo)
	{
		return NEED_CONFIG;
	}

	@Override
	public List<String> getPkForVersion(DbxConnection conn, DbmsVersionInfo versionInfo)
	{
		List <String> pkCols = new LinkedList<String>();
		return pkCols;
	}

	/** now() - 'Uptime': when MySQL was started (for example used when the alarm 'SrvDown' is cancelled: was it restarted?) */
	@Override
	protected String getDbmsStartTimeColumnName()
	{
		return "start_time";
	}

	@Override
	public String getSqlForVersion(DbxConnection conn, DbmsVersionInfo versionInfo)
	{
		Configuration conf = Configuration.getCombinedConfiguration();
		int oldestOpenTranInSecThreshold = conf.getIntProperty(PROPKEY_alarm_oldestOpenTranInSec, DEFAULT_alarm_oldestOpenTranInSec);
		int blockingWaitInSecThreshold   = (int) conf.getDoubleProperty(PROPKEY_alarm_BlockingLockCount_minWaitTimeSec, DEFAULT_alarm_BlockingLockCount_minWaitTimeSec);

		// MariaDB: the status variables are in information_schema.GLOBAL_STATUS (with UPPER case names), and it has no: super_read_only,
		//          performance_schema.replication_*, statement counters per event name, metadata_locks
		//          (this is "detect and explain": the summary must work, the other Counter Models are disabled with the reason)
		boolean isMariaDb = (versionInfo instanceof DbmsVersionInfoMySql) && ((DbmsVersionInfoMySql) versionInfo).isMariaDb();

		String statusTab        = isMariaDb ? "information_schema.GLOBAL_STATUS" : "performance_schema.global_status";
		String superReadOnly    = isMariaDb ? "0"                                : "@@super_read_only";
		String replicaChannels  = isMariaDb ? "0"                                : "(select count(*) from performance_schema.replication_connection_configuration where CHANNEL_NAME not like 'group_replication%')";

		// The statement counters: in MySQL 8 the 'Com_xxx' status variables are NOT in performance_schema.global_status (they are in this summary table)
		String comCounters = isMariaDb
			? "cross join (select max(CASE WHEN upper(VARIABLE_NAME) = 'COM_SELECT' THEN CAST(VARIABLE_VALUE as SIGNED) END) as COM_SELECT \n"
			+ "                  ,max(CASE WHEN upper(VARIABLE_NAME) = 'COM_INSERT' THEN CAST(VARIABLE_VALUE as SIGNED) END) as COM_INSERT \n"
			+ "                  ,max(CASE WHEN upper(VARIABLE_NAME) = 'COM_UPDATE' THEN CAST(VARIABLE_VALUE as SIGNED) END) as COM_UPDATE \n"
			+ "                  ,max(CASE WHEN upper(VARIABLE_NAME) = 'COM_DELETE' THEN CAST(VARIABLE_VALUE as SIGNED) END) as COM_DELETE \n"
			+ "            from information_schema.GLOBAL_STATUS) es \n"
			: "cross join (select max(CASE WHEN EVENT_NAME = 'statement/sql/select' THEN COUNT_STAR END) as COM_SELECT \n"
			+ "                  ,max(CASE WHEN EVENT_NAME = 'statement/sql/insert' THEN COUNT_STAR END) as COM_INSERT \n"
			+ "                  ,max(CASE WHEN EVENT_NAME = 'statement/sql/update' THEN COUNT_STAR END) as COM_UPDATE \n"
			+ "                  ,max(CASE WHEN EVENT_NAME = 'statement/sql/delete' THEN COUNT_STAR END) as COM_DELETE \n"
			+ "            from performance_schema.events_statements_summary_global_by_event_name \n"
			+ "            where EVENT_NAME in ('statement/sql/select', 'statement/sql/insert', 'statement/sql/update', 'statement/sql/delete')) es \n";

		// Sessions that waits for a metadata lock (for example: an ALTER TABLE that waits for an open transaction, and everybody else waits for the ALTER)
		String mdlWaits = isMariaDb
			? "cross join (select 0 as COUNT_MDL_WAIT, 0 as MAX_MDL_WAIT_IN_SEC) md \n"
			: "cross join (select count(*)                as COUNT_MDL_WAIT \n"
			+ "                  ,max(th.PROCESSLIST_TIME) as MAX_MDL_WAIT_IN_SEC \n"
			+ "            from performance_schema.metadata_locks ml \n"
			+ "            left join performance_schema.threads th on th.THREAD_ID = ml.OWNER_THREAD_ID \n"
			+ "            where ml.LOCK_STATUS = 'PENDING') md \n";

		// One status variable (the value is a string)
		String UPPER_NAME = isMariaDb ? "upper(VARIABLE_NAME)" : "VARIABLE_NAME";

		String sql = ""
			+ "select /* ${cmCollectorName} */ \n"
			+ "     cast(version() as char(255))                                as version \n"
			+ "    ,@@version_comment                                           as VERSION_COMMENT \n"
			+ "    ,CURRENT_TIMESTAMP()                                         as time_now \n"
			+ "    ,TIMESTAMPDIFF(MINUTE, UTC_TIMESTAMP(), CURRENT_TIMESTAMP()) as utc_minute_diff \n"
			+ "    ,@@hostname                                                  as host \n"
			+ "    ,DATE_SUB(CURRENT_TIMESTAMP, INTERVAL s.UPTIME SECOND)       as start_time \n" // note: this is a SQL-TIMESTAMP data type
			+ "    ,CAST(s.UPTIME / 86400.0 as DECIMAL(10,1))                   as DAYS_RUNNING \n"
			+ "    ,@@read_only                                                 as READ_ONLY \n"
			+ "    ," + superReadOnly + "                                       as SUPER_READ_ONLY \n"
			+ "    ," + replicaChannels + " as REPLICA_CHANNELS \n"
			+ "    ,@@max_connections                                           as MAX_CONNECTIONS \n"
			+ "    ,s.MAX_USED_CONNECTIONS \n"
			+ "    ,s.THREADS_CONNECTED \n"
			+ "    ,s.THREADS_RUNNING \n"
			+ "    ,s.QUESTIONS \n"
			+ "    ,es.COM_SELECT \n"
			+ "    ,es.COM_INSERT \n"
			+ "    ,es.COM_UPDATE \n"
			+ "    ,es.COM_DELETE \n"
			+ "    ,s.HANDLER_COMMIT \n"
			+ "    ,s.HANDLER_ROLLBACK \n"
			+ "    ,s.CONNECTIONS \n"
			+ "    ,s.ABORTED_CONNECTS \n"
			+ "    ,s.ABORTED_CLIENTS \n"
			+ "    ,s.CONN_ERRORS_MAX_CONN \n"
			+ "    ,s.SLOW_QUERIES \n"
			+ "    ,s.CREATED_TMP_TABLES \n"
			+ "    ,s.CREATED_TMP_DISK_TABLES \n"
			+ "    ,s.SELECT_FULL_JOIN \n"
			+ "    ,s.SELECT_SCAN \n"
			+ "    ,s.SORT_MERGE_PASSES \n"
			+ "    ,s.ROWS_READ \n"
			+ "    ,s.ROWS_INSERTED \n"
			+ "    ,s.ROWS_UPDATED \n"
			+ "    ,s.ROWS_DELETED \n"
			+ "    ,s.BP_READ_REQUESTS \n"
			+ "    ,s.BP_READS \n"
			+ "    ,CAST(100.0 - 100.0 * s.BP_READS / NULLIF(s.BP_READ_REQUESTS, 0) as DECIMAL(5,1)) as BP_HIT_PCT \n"
			+ "    ,s.BP_WAIT_FREE \n"
			+ "    ,s.BP_PAGES_FLUSHED \n"
			+ "    ,s.BP_PAGES_TOTAL \n"
			+ "    ,s.BP_PAGES_FREE \n"
			+ "    ,s.BP_PAGES_DIRTY \n"
			+ "    ,s.REDO_BYTES \n"
			+ "    ,s.DATA_READ_BYTES \n"
			+ "    ,s.DATA_WRITTEN_BYTES \n"
			+ "    ,s.ROW_LOCK_WAITS \n"
			+ "    ,s.ROW_LOCK_TIME_MS \n"
			+ "    ,m.DEADLOCKS \n"
			+ "    ,m.HISTORY_LIST_LENGTH \n"
			+ "    ,s.BYTES_RECEIVED \n"
			+ "    ,s.BYTES_SENT \n"
			+ "    ,s.TABLE_OPEN_CACHE_MISSES \n"
			+ "    ,s.BINLOG_CACHE_DISK_USE \n"
			+ "    ,t.OPEN_TRAN_COUNT \n"
			+ "    ,CAST(COALESCE(t.OLDEST_OPEN_TRAN_IN_SEC, 0) as SIGNED)      as OLDEST_OPEN_TRAN_IN_SEC \n"
			+ "    ," + oldestOpenTranInSecThreshold + "                        as OLDEST_OPEN_TRAN_IN_SEC_TH \n" // TH = THRESHOLD
			+ "    ,o.trx_mysql_thread_id                                       as OLDEST_OPEN_TRAN_PID \n"
			+ "    ,o.trx_started                                               as OLDEST_OPEN_TRAN_TIME \n"
			+ "    ,o.trx_state                                                 as OLDEST_OPEN_TRAN_STATE \n"
			+ "    ,th.PROCESSLIST_USER                                         as OLDEST_OPEN_TRAN_USER \n"
			+ "    ,th.PROCESSLIST_HOST                                         as OLDEST_OPEN_TRAN_HOST \n"
			+ "    ,COALESCE(t.COUNT_ROW_LOCK_WAIT, 0)                          as COUNT_ROW_LOCK_WAIT \n"
			+ "    ,md.COUNT_MDL_WAIT \n"
			+ "    ,COALESCE(t.COUNT_ROW_LOCK_WAIT, 0) + md.COUNT_MDL_WAIT      as COUNT_BLOCKING_WAIT \n"
			+ "    ,CAST(GREATEST(COALESCE(t.MAX_ROW_LOCK_WAIT_IN_SEC, 0), COALESCE(md.MAX_MDL_WAIT_IN_SEC, 0)) as SIGNED) as MAX_BLOCKING_WAIT_IN_SEC \n"
			+ "    ," + blockingWaitInSecThreshold + "                          as BLOCKING_WAIT_IN_SEC_TH \n" // TH = THRESHOLD
			+ "from (select \n"
			+ "         max(CASE WHEN " + UPPER_NAME + " = " + name(isMariaDb, "Uptime"                           ) + " THEN CAST(VARIABLE_VALUE as SIGNED) END) as UPTIME \n"
			+ "        ,max(CASE WHEN " + UPPER_NAME + " = " + name(isMariaDb, "Max_used_connections"             ) + " THEN CAST(VARIABLE_VALUE as SIGNED) END) as MAX_USED_CONNECTIONS \n"
			+ "        ,max(CASE WHEN " + UPPER_NAME + " = " + name(isMariaDb, "Threads_connected"                ) + " THEN CAST(VARIABLE_VALUE as SIGNED) END) as THREADS_CONNECTED \n"
			+ "        ,max(CASE WHEN " + UPPER_NAME + " = " + name(isMariaDb, "Threads_running"                  ) + " THEN CAST(VARIABLE_VALUE as SIGNED) END) as THREADS_RUNNING \n"
			+ "        ,max(CASE WHEN " + UPPER_NAME + " = " + name(isMariaDb, "Questions"                        ) + " THEN CAST(VARIABLE_VALUE as SIGNED) END) as QUESTIONS \n"
			+ "        ,max(CASE WHEN " + UPPER_NAME + " = " + name(isMariaDb, "Handler_commit"                   ) + " THEN CAST(VARIABLE_VALUE as SIGNED) END) as HANDLER_COMMIT \n"
			+ "        ,max(CASE WHEN " + UPPER_NAME + " = " + name(isMariaDb, "Handler_rollback"                 ) + " THEN CAST(VARIABLE_VALUE as SIGNED) END) as HANDLER_ROLLBACK \n"
			+ "        ,max(CASE WHEN " + UPPER_NAME + " = " + name(isMariaDb, "Connections"                      ) + " THEN CAST(VARIABLE_VALUE as SIGNED) END) as CONNECTIONS \n"
			+ "        ,max(CASE WHEN " + UPPER_NAME + " = " + name(isMariaDb, "Aborted_connects"                 ) + " THEN CAST(VARIABLE_VALUE as SIGNED) END) as ABORTED_CONNECTS \n"
			+ "        ,max(CASE WHEN " + UPPER_NAME + " = " + name(isMariaDb, "Aborted_clients"                  ) + " THEN CAST(VARIABLE_VALUE as SIGNED) END) as ABORTED_CLIENTS \n"
			+ "        ,max(CASE WHEN " + UPPER_NAME + " = " + name(isMariaDb, "Connection_errors_max_connections") + " THEN CAST(VARIABLE_VALUE as SIGNED) END) as CONN_ERRORS_MAX_CONN \n"
			+ "        ,max(CASE WHEN " + UPPER_NAME + " = " + name(isMariaDb, "Slow_queries"                     ) + " THEN CAST(VARIABLE_VALUE as SIGNED) END) as SLOW_QUERIES \n"
			+ "        ,max(CASE WHEN " + UPPER_NAME + " = " + name(isMariaDb, "Created_tmp_tables"               ) + " THEN CAST(VARIABLE_VALUE as SIGNED) END) as CREATED_TMP_TABLES \n"
			+ "        ,max(CASE WHEN " + UPPER_NAME + " = " + name(isMariaDb, "Created_tmp_disk_tables"          ) + " THEN CAST(VARIABLE_VALUE as SIGNED) END) as CREATED_TMP_DISK_TABLES \n"
			+ "        ,max(CASE WHEN " + UPPER_NAME + " = " + name(isMariaDb, "Select_full_join"                 ) + " THEN CAST(VARIABLE_VALUE as SIGNED) END) as SELECT_FULL_JOIN \n"
			+ "        ,max(CASE WHEN " + UPPER_NAME + " = " + name(isMariaDb, "Select_scan"                      ) + " THEN CAST(VARIABLE_VALUE as SIGNED) END) as SELECT_SCAN \n"
			+ "        ,max(CASE WHEN " + UPPER_NAME + " = " + name(isMariaDb, "Sort_merge_passes"                ) + " THEN CAST(VARIABLE_VALUE as SIGNED) END) as SORT_MERGE_PASSES \n"
			+ "        ,max(CASE WHEN " + UPPER_NAME + " = " + name(isMariaDb, "Innodb_rows_read"                 ) + " THEN CAST(VARIABLE_VALUE as SIGNED) END) as ROWS_READ \n"
			+ "        ,max(CASE WHEN " + UPPER_NAME + " = " + name(isMariaDb, "Innodb_rows_inserted"             ) + " THEN CAST(VARIABLE_VALUE as SIGNED) END) as ROWS_INSERTED \n"
			+ "        ,max(CASE WHEN " + UPPER_NAME + " = " + name(isMariaDb, "Innodb_rows_updated"              ) + " THEN CAST(VARIABLE_VALUE as SIGNED) END) as ROWS_UPDATED \n"
			+ "        ,max(CASE WHEN " + UPPER_NAME + " = " + name(isMariaDb, "Innodb_rows_deleted"              ) + " THEN CAST(VARIABLE_VALUE as SIGNED) END) as ROWS_DELETED \n"
			+ "        ,max(CASE WHEN " + UPPER_NAME + " = " + name(isMariaDb, "Innodb_buffer_pool_read_requests" ) + " THEN CAST(VARIABLE_VALUE as SIGNED) END) as BP_READ_REQUESTS \n"
			+ "        ,max(CASE WHEN " + UPPER_NAME + " = " + name(isMariaDb, "Innodb_buffer_pool_reads"         ) + " THEN CAST(VARIABLE_VALUE as SIGNED) END) as BP_READS \n"
			+ "        ,max(CASE WHEN " + UPPER_NAME + " = " + name(isMariaDb, "Innodb_buffer_pool_wait_free"     ) + " THEN CAST(VARIABLE_VALUE as SIGNED) END) as BP_WAIT_FREE \n"
			+ "        ,max(CASE WHEN " + UPPER_NAME + " = " + name(isMariaDb, "Innodb_buffer_pool_pages_flushed" ) + " THEN CAST(VARIABLE_VALUE as SIGNED) END) as BP_PAGES_FLUSHED \n"
			+ "        ,max(CASE WHEN " + UPPER_NAME + " = " + name(isMariaDb, "Innodb_buffer_pool_pages_total"   ) + " THEN CAST(VARIABLE_VALUE as SIGNED) END) as BP_PAGES_TOTAL \n"
			+ "        ,max(CASE WHEN " + UPPER_NAME + " = " + name(isMariaDb, "Innodb_buffer_pool_pages_free"    ) + " THEN CAST(VARIABLE_VALUE as SIGNED) END) as BP_PAGES_FREE \n"
			+ "        ,max(CASE WHEN " + UPPER_NAME + " = " + name(isMariaDb, "Innodb_buffer_pool_pages_dirty"   ) + " THEN CAST(VARIABLE_VALUE as SIGNED) END) as BP_PAGES_DIRTY \n"
			+ "        ,max(CASE WHEN " + UPPER_NAME + " = " + name(isMariaDb, "Innodb_os_log_written"            ) + " THEN CAST(VARIABLE_VALUE as SIGNED) END) as REDO_BYTES \n"
			+ "        ,max(CASE WHEN " + UPPER_NAME + " = " + name(isMariaDb, "Innodb_data_read"                 ) + " THEN CAST(VARIABLE_VALUE as SIGNED) END) as DATA_READ_BYTES \n"
			+ "        ,max(CASE WHEN " + UPPER_NAME + " = " + name(isMariaDb, "Innodb_data_written"              ) + " THEN CAST(VARIABLE_VALUE as SIGNED) END) as DATA_WRITTEN_BYTES \n"
			+ "        ,max(CASE WHEN " + UPPER_NAME + " = " + name(isMariaDb, "Innodb_row_lock_waits"            ) + " THEN CAST(VARIABLE_VALUE as SIGNED) END) as ROW_LOCK_WAITS \n"
			+ "        ,max(CASE WHEN " + UPPER_NAME + " = " + name(isMariaDb, "Innodb_row_lock_time"             ) + " THEN CAST(VARIABLE_VALUE as SIGNED) END) as ROW_LOCK_TIME_MS \n"
			+ "        ,max(CASE WHEN " + UPPER_NAME + " = " + name(isMariaDb, "Bytes_received"                   ) + " THEN CAST(VARIABLE_VALUE as SIGNED) END) as BYTES_RECEIVED \n"
			+ "        ,max(CASE WHEN " + UPPER_NAME + " = " + name(isMariaDb, "Bytes_sent"                       ) + " THEN CAST(VARIABLE_VALUE as SIGNED) END) as BYTES_SENT \n"
			+ "        ,max(CASE WHEN " + UPPER_NAME + " = " + name(isMariaDb, "Table_open_cache_misses"          ) + " THEN CAST(VARIABLE_VALUE as SIGNED) END) as TABLE_OPEN_CACHE_MISSES \n"
			+ "        ,max(CASE WHEN " + UPPER_NAME + " = " + name(isMariaDb, "Binlog_cache_disk_use"            ) + " THEN CAST(VARIABLE_VALUE as SIGNED) END) as BINLOG_CACHE_DISK_USE \n"
			+ "      from " + statusTab + " \n"
			+ "     ) s \n"
			+ comCounters
			+ "cross join (select max(CASE WHEN NAME = 'lock_deadlocks'       THEN COUNT END) as DEADLOCKS \n"
			+ "                  ,max(CASE WHEN NAME = 'trx_rseg_history_len' THEN COUNT END) as HISTORY_LIST_LENGTH \n"
			+ "            from information_schema.INNODB_METRICS \n"
			+ "            where NAME in ('lock_deadlocks', 'trx_rseg_history_len')) m \n"
			+ "cross join (select count(*)                                                                as OPEN_TRAN_COUNT \n"
			+ "                  ,max(TIMESTAMPDIFF(SECOND, trx_started, NOW()))                          as OLDEST_OPEN_TRAN_IN_SEC \n"
			+ "                  ,sum(CASE WHEN trx_state = 'LOCK WAIT' THEN 1 ELSE 0 END)                 as COUNT_ROW_LOCK_WAIT \n"
			+ "                  ,max(CASE WHEN trx_state = 'LOCK WAIT' THEN TIMESTAMPDIFF(SECOND, trx_wait_started, NOW()) END) as MAX_ROW_LOCK_WAIT_IN_SEC \n"
			+ "            from information_schema.INNODB_TRX) t \n"
			+ mdlWaits
			+ "left join (select trx_mysql_thread_id, trx_started, trx_state from information_schema.INNODB_TRX order by trx_started limit 1) o on 1 = 1 \n"
			+ "left join performance_schema.threads th on th.PROCESSLIST_ID = o.trx_mysql_thread_id \n"
			+ "";

		return sql;
	}

	/** The name of a status variable as a SQL string: MariaDB's information_schema.GLOBAL_STATUS has the names in UPPER case */
	private static String name(boolean isMariaDb, String varName)
	{
		return "'" + (isMariaDb ? varName.toUpperCase() : varName) + "'";
	}

	/**
	 * BP_HIT_PCT: the absolute value is "since the server started", in diff (and rate) it is for the sample:
	 * 100 - 100 * (diff BP_READS / diff BP_READ_REQUESTS)
	 */
	@Override
	public void localCalculation(CounterSample prevSample, CounterSample newSample, CounterSample diffData)
	{
		int pos_hitPct   = diffData.findColumn("BP_HIT_PCT");
		int pos_reads    = diffData.findColumn("BP_READS");
		int pos_requests = diffData.findColumn("BP_READ_REQUESTS");
		if (pos_hitPct == -1 || pos_reads == -1 || pos_requests == -1 || diffData.getRowCount() == 0)
			return;

		Object reads    = diffData.getValueAt(0, pos_reads);
		Object requests = diffData.getValueAt(0, pos_requests);
		if ( !(reads instanceof Number) || !(requests instanceof Number) )
			return;

		double requestsVal = ((Number)requests).doubleValue();
		BigDecimal hitPct = null; // no logical reads in the sample: no hit ratio
		if (requestsVal > 0)
			hitPct = new BigDecimal(100.0 - 100.0 * ((Number)reads).doubleValue() / requestsVal).setScale(1, RoundingMode.HALF_EVEN);

		diffData.setValueAt(hitPct, 0, pos_hitPct);
	}

	@Override
	public void updateGraphData(TrendGraphDataPoint tgdp)
	{
		if (GRAPH_NAME_CONNECTION.equals(tgdp.getName()))
		{
			Double[] arr = new Double[2];
			arr[0] = this.getAbsValueAsDouble(0, "THREADS_CONNECTED");
			arr[1] = this.getAbsValueAsDouble(0, "THREADS_RUNNING");
			tgdp.setDataPoint(this.getTimestamp(), arr);
		}

		if (GRAPH_NAME_STATEMENTS.equals(tgdp.getName()))
		{
			Double[] arr = new Double[5];
			arr[0] = this.getRateValueAsDouble(0, "QUESTIONS");
			arr[1] = this.getRateValueAsDouble(0, "COM_SELECT");
			arr[2] = this.getRateValueAsDouble(0, "COM_INSERT");
			arr[3] = this.getRateValueAsDouble(0, "COM_UPDATE");
			arr[4] = this.getRateValueAsDouble(0, "COM_DELETE");
			tgdp.setDataPoint(this.getTimestamp(), arr);
		}

		if (GRAPH_NAME_TRANSACTION.equals(tgdp.getName()))
		{
			Double[] arr = new Double[2];
			arr[0] = this.getRateValueAsDouble(0, "HANDLER_COMMIT");
			arr[1] = this.getRateValueAsDouble(0, "HANDLER_ROLLBACK");
			tgdp.setDataPoint(this.getTimestamp(), arr);
		}

		if (GRAPH_NAME_INNODB_ROWS.equals(tgdp.getName()))
		{
			Double[] arr = new Double[4];
			arr[0] = this.getRateValueAsDouble(0, "ROWS_READ");
			arr[1] = this.getRateValueAsDouble(0, "ROWS_INSERTED");
			arr[2] = this.getRateValueAsDouble(0, "ROWS_UPDATED");
			arr[3] = this.getRateValueAsDouble(0, "ROWS_DELETED");
			tgdp.setDataPoint(this.getTimestamp(), arr);
		}

		if (GRAPH_NAME_READS.equals(tgdp.getName()))
		{
			Double[] arr = new Double[3];
			arr[0] = this.getRateValueAsDouble(0, "BP_READ_REQUESTS");
			arr[1] = this.getRateValueAsDouble(0, "BP_READS");
			arr[2] = this.getRateValueAsDouble(0, "BP_PAGES_FLUSHED");
			tgdp.setDataPoint(this.getTimestamp(), arr);
		}

		if (GRAPH_NAME_BP_HIT_PCT.equals(tgdp.getName()))
		{
			// No logical reads in the sample: no point in the graph
			Double hitPct = this.getDiffValueAsDouble(0, "BP_HIT_PCT");
			if (hitPct != null)
				tgdp.setDataPoint(this.getTimestamp(), new Double[] { hitPct });
		}

		if (GRAPH_NAME_REDO.equals(tgdp.getName()))
		{
			Double redoBytes = this.getRateValueAsDouble(0, "REDO_BYTES");
			tgdp.setDataPoint(this.getTimestamp(), new Double[] { redoBytes == null ? null : redoBytes / 1024.0 });
		}

		if (GRAPH_NAME_SQL_PROBLEMS.equals(tgdp.getName()))
		{
			Double[] arr = new Double[4];
			arr[0] = this.getRateValueAsDouble(0, "SLOW_QUERIES");
			arr[1] = this.getRateValueAsDouble(0, "CREATED_TMP_DISK_TABLES");
			arr[2] = this.getRateValueAsDouble(0, "SELECT_FULL_JOIN");
			arr[3] = this.getRateValueAsDouble(0, "SORT_MERGE_PASSES");
			tgdp.setDataPoint(this.getTimestamp(), arr);
		}

		if (GRAPH_NAME_CONN_PROBLEMS.equals(tgdp.getName()))
		{
			Double[] arr = new Double[3];
			arr[0] = this.getRateValueAsDouble(0, "ABORTED_CONNECTS");
			arr[1] = this.getRateValueAsDouble(0, "ABORTED_CLIENTS");
			arr[2] = this.getRateValueAsDouble(0, "CONN_ERRORS_MAX_CONN");
			tgdp.setDataPoint(this.getTimestamp(), arr);
		}

		if (GRAPH_NAME_BLOCKING_LOCKS.equals(tgdp.getName()))
		{
			Double[] arr = new Double[2];
			arr[0] = this.getAbsValueAsDouble(0, "COUNT_ROW_LOCK_WAIT");
			arr[1] = this.getAbsValueAsDouble(0, "COUNT_MDL_WAIT");
			tgdp.setDataPoint(this.getTimestamp(), arr);
		}

		if (GRAPH_NAME_OLDEST_TRAN_IN_SEC.equals(tgdp.getName()))
		{
			tgdp.setDataPoint(this.getTimestamp(), new Double[] { this.getAbsValueAsDouble(0, "OLDEST_OPEN_TRAN_IN_SEC") });
		}

		if (GRAPH_NAME_HISTORY_LIST.equals(tgdp.getName()))
		{
			tgdp.setDataPoint(this.getTimestamp(), new Double[] { this.getAbsValueAsDouble(0, "HISTORY_LIST_LENGTH") });
		}
	}

	@Override
	public boolean isGraphDataHistoryEnabled(String name)
	{
		// ENABLED for the following graphs (used in the alarms)
		if (GRAPH_NAME_BLOCKING_LOCKS    .equals(name)) return true;
		if (GRAPH_NAME_OLDEST_TRAN_IN_SEC.equals(name)) return true;

		// default: DISABLED
		return false;
	}


	//----------------------------------------------------------------
	// ALARMS
	//----------------------------------------------------------------
	@Override
	public void sendAlarmRequest()
	{
		if ( ! hasAbsData() )
			return;

		if ( ! AlarmHandler.hasInstance() )
			return;

		CountersModel cm = this;

		boolean debugPrint = Configuration.getCombinedConfiguration().getBooleanProperty("sendAlarmRequest.debug", _logger.isDebugEnabled());

		//-------------------------------------------------------
		// DbmsVersionStringChanged
		//-------------------------------------------------------
		doAlarmIfDbmsVersionStringWasChanged("version");

		//-------------------------------------------------------
		// BlockingLockCount
		//-------------------------------------------------------
		if (isSystemAlarmsForColumnEnabledAndInTimeRange("BlockingLockCount"))
		{
			Integer blockingCount    = cm.getAbsValueAsInteger(0, "COUNT_BLOCKING_WAIT");
			Integer maxWaitTimeInSec = cm.getAbsValueAsInteger(0, "MAX_BLOCKING_WAIT_IN_SEC");

			if (blockingCount != null && maxWaitTimeInSec != null)
			{
				int    threshold     = Configuration.getCombinedConfiguration().getIntProperty   (PROPKEY_alarm_BlockingLockCount               , DEFAULT_alarm_BlockingLockCount);
				double waitThreshold = Configuration.getCombinedConfiguration().getDoubleProperty(PROPKEY_alarm_BlockingLockCount_minWaitTimeSec, DEFAULT_alarm_BlockingLockCount_minWaitTimeSec);

				if (debugPrint || _logger.isDebugEnabled())
					System.out.println("##### sendAlarmRequest(" + cm.getName() + "): BlockingLockCount: threshold=" + threshold + ", COUNT_BLOCKING_WAIT='" + blockingCount + "', waitThreshold=" + waitThreshold + ", MAX_BLOCKING_WAIT_IN_SEC='" + maxWaitTimeInSec + "'.");

				if (blockingCount > threshold && maxWaitTimeInSec > waitThreshold)
				{
					AlarmEvent ae = new AlarmEventBlockingLockAlarm(cm, waitThreshold, blockingCount, maxWaitTimeInSec);

					// The below is called from AlarmHandler on: Raise, RE-RAISE & CANCEL
					ae.setAlarmDescriptionProvider(this, new AlarmDescriptionProviderBase(null, null, waitThreshold)
					{
						@Override
						public void setValues(CountersModel cm, AlarmEvent alarmEvent, AlarmPhase phase)
						{
							// Always: (RAISE, RE-RAISE, CANCEL) - Set graph values
							Integer COUNT_ROW_LOCK_WAIT = cm.getAbsValueAsInteger(0, "COUNT_ROW_LOCK_WAIT");
							Integer COUNT_MDL_WAIT      = cm.getAbsValueAsInteger(0, "COUNT_MDL_WAIT");
							String  what = "Waiting for a row lock: " + COUNT_ROW_LOCK_WAIT + ", waiting for a metadata lock: " + COUNT_MDL_WAIT + ". See the tab 'Active Statements' for who blocks who.";
							alarmEvent.setExtendedDescription(what, what + "<br><br>" + cm.getGraphDataHistoryAsHtmlImage(GRAPH_NAME_BLOCKING_LOCKS));

							// Set CANCEL message (values that was found AFTER last raise/re-raise event)
							if (AlarmPhase.CANCEL.equals(phase))
							{
								Integer COUNT_BLOCKING_WAIT      = cm.getAbsValueAsInteger(0, "COUNT_BLOCKING_WAIT");
								Integer MAX_BLOCKING_WAIT_IN_SEC = cm.getAbsValueAsInteger(0, "MAX_BLOCKING_WAIT_IN_SEC");

								String cancelMsg = getAlarmCancelText("Blocked sessions", COUNT_BLOCKING_WAIT + ", max wait " + (MAX_BLOCKING_WAIT_IN_SEC == null ? 0 : MAX_BLOCKING_WAIT_IN_SEC) + " seconds", " seconds wait");
								alarmEvent.setCancelDescription(cancelMsg);
							}
						}
					});

					// Information about how to disable this alarm
					ae.createAlarmOptionsMessage(this, "BlockingLockCount");

					AlarmHandler.getInstance().addAlarm(ae);
				}
			}
		}

		//-------------------------------------------------------
		// oldestOpenTranInSec
		//-------------------------------------------------------
		if (isSystemAlarmsForColumnEnabledAndInTimeRange("oldestOpenTranInSec"))
		{
			// 0 when there are no open transactions
			Integer oldestOpenTranInSec = cm.getAbsValueAsInteger(0, "OLDEST_OPEN_TRAN_IN_SEC");

			if (oldestOpenTranInSec != null)
			{
				int threshold = Configuration.getCombinedConfiguration().getIntProperty(PROPKEY_alarm_oldestOpenTranInSec, DEFAULT_alarm_oldestOpenTranInSec);

				if (debugPrint || _logger.isDebugEnabled())
					System.out.println("##### sendAlarmRequest(" + cm.getName() + "): oldestOpenTranInSec: threshold=" + threshold + ", OLDEST_OPEN_TRAN_IN_SEC='" + oldestOpenTranInSec + "'.");

				if (oldestOpenTranInSec > threshold)
				{
					AlarmEvent ae = new AlarmEventMyLongRunningTransaction(cm, threshold, oldestOpenTranInSec,
							cm.getAbsValueAsLong(0, "OLDEST_OPEN_TRAN_PID"),
							cm.getAbsString     (0, "OLDEST_OPEN_TRAN_USER"),
							cm.getAbsString     (0, "OLDEST_OPEN_TRAN_HOST"),
							cm.getAbsString     (0, "OLDEST_OPEN_TRAN_STATE"),
							cm.getAbsValueAsLong(0, "HISTORY_LIST_LENGTH"));

					// The below is called from AlarmHandler on: Raise, RE-RAISE & CANCEL
					ae.setAlarmDescriptionProvider(this, new AlarmDescriptionProviderBase(null, null, threshold)
					{
						@Override
						public void setValues(CountersModel cm, AlarmEvent alarmEvent, AlarmPhase phase)
						{
							// Always: (RAISE, RE-RAISE, CANCEL) - Who has the oldest transaction right now + graph values
							Integer OPEN_TRAN_COUNT = cm.getAbsValueAsInteger(0, "OPEN_TRAN_COUNT");
							boolean hasOpenTran     = OPEN_TRAN_COUNT != null && OPEN_TRAN_COUNT > 0;

							String who = ! hasOpenTran ? "There are no open transactions."
							           : "The oldest open transaction: pid=" + cm.getAbsString(0, "OLDEST_OPEN_TRAN_PID")
							           + ", user='"       + cm.getAbsString(0, "OLDEST_OPEN_TRAN_USER") + "'"
							           + ", host='"       + cm.getAbsString(0, "OLDEST_OPEN_TRAN_HOST") + "'"
							           + ", state='"      + cm.getAbsString(0, "OLDEST_OPEN_TRAN_STATE") + "'"
							           + ", started at '" + cm.getAbsString(0, "OLDEST_OPEN_TRAN_TIME") + "'"
							           + ", open transactions: " + OPEN_TRAN_COUNT + ".";

							alarmEvent.setExtendedDescription(who, who + "<br><br>" + cm.getGraphDataHistoryAsHtmlImage(GRAPH_NAME_OLDEST_TRAN_IN_SEC));

							// Set CANCEL message (values that was found AFTER last raise/re-raise event)
							if (AlarmPhase.CANCEL.equals(phase))
							{
								Integer OLDEST_OPEN_TRAN_IN_SEC = cm.getAbsValueAsInteger(0, "OLDEST_OPEN_TRAN_IN_SEC");

								String cancelMsg = ! hasOpenTran ? getAlarmCancelTextGone("The open transaction")
								                 : getAlarmCancelText("The oldest open transaction", OLDEST_OPEN_TRAN_IN_SEC + " seconds", " seconds");
								alarmEvent.setCancelDescription(cancelMsg);
							}
						}
					});

					// Information about how to disable this alarm
					ae.createAlarmOptionsMessage(this, "oldestOpenTranInSec");

					AlarmHandler.getInstance().addAlarm(ae);
				}
			}
		}
	}

	public static final String  PROPKEY_alarm_BlockingLockCount                = CM_NAME + ".alarm.system.if.blocking_lock_count.gt";
	public static final int     DEFAULT_alarm_BlockingLockCount                = 0;

	public static final String  PROPKEY_alarm_BlockingLockCount_minWaitTimeSec = CM_NAME + ".alarm.system.if.blocking_lock_count.minWaitTimeSec";
	public static final double  DEFAULT_alarm_BlockingLockCount_minWaitTimeSec = 60.0;

	public static final String  PROPKEY_alarm_oldestOpenTranInSec              = CM_NAME + ".alarm.system.if.oldestOpenTranInSec.gt";
	public static final int     DEFAULT_alarm_oldestOpenTranInSec              = 60 * 30; // 30 minutes

	@Override
	public List<CmSettingsHelper> getLocalAlarmSettings()
	{
		Configuration conf = Configuration.getCombinedConfiguration();
		List<CmSettingsHelper> list = new ArrayList<>();

		CmSettingsHelper.Type isAlarmSwitch = CmSettingsHelper.Type.IS_ALARM_SWITCH;

		addAlarmSettings_DbmsVersionStringChanged(list, "version");

		list.add(new CmSettingsHelper("BlockingLockCount",             isAlarmSwitch, PROPKEY_alarm_BlockingLockCount               , Integer.class, conf.getIntProperty   (PROPKEY_alarm_BlockingLockCount               , DEFAULT_alarm_BlockingLockCount               ), DEFAULT_alarm_BlockingLockCount               , "If 'COUNT_BLOCKING_WAIT' (number of sessions that waits for a row lock or a metadata lock) is greater than ## then send 'AlarmEventBlockingLockAlarm'." ));
		list.add(new CmSettingsHelper("BlockingLockCount MinWaitTime",                PROPKEY_alarm_BlockingLockCount_minWaitTimeSec, Double .class, conf.getDoubleProperty(PROPKEY_alarm_BlockingLockCount_minWaitTimeSec, DEFAULT_alarm_BlockingLockCount_minWaitTimeSec), DEFAULT_alarm_BlockingLockCount_minWaitTimeSec, "If 'BlockingLockCount' is true; then 'MAX_BLOCKING_WAIT_IN_SEC' (the session that has waited the longest) must also be greater than ## seconds." ));
		list.add(new CmSettingsHelper("oldestOpenTranInSec",           isAlarmSwitch, PROPKEY_alarm_oldestOpenTranInSec             , Integer.class, conf.getIntProperty   (PROPKEY_alarm_oldestOpenTranInSec             , DEFAULT_alarm_oldestOpenTranInSec             ), DEFAULT_alarm_oldestOpenTranInSec             , "If 'OLDEST_OPEN_TRAN_IN_SEC' is greater than ## seconds then send 'AlarmEventMyLongRunningTransaction'." ));

		return list;
	}

	/** What the columns are: tooltips in the GUI, and the 'Description' column in DbxCentral (Counter Details, the "unpivot" view) */
	@Override
	public void initLocalToolTipTextOnTableColumnHeader()
	{
		setLocalToolTipTextOnTableColumnHeader("version",                    "<html>The MySQL version. version()</html>");
		setLocalToolTipTextOnTableColumnHeader("VERSION_COMMENT",            "<html>The MySQL edition/distribution. @@version_comment</html>");
		setLocalToolTipTextOnTableColumnHeader("time_now",                   "<html>Time of the sample (CURRENT_TIMESTAMP() on the server).</html>");
		setLocalToolTipTextOnTableColumnHeader("utc_minute_diff",            "<html>UTC Time Difference in minutes (positive east of UK, negative west of UK).</html>");
		setLocalToolTipTextOnTableColumnHeader("host",                       "<html>The host that MySQL runs on. @@hostname</html>");
		setLocalToolTipTextOnTableColumnHeader("start_time",                 "<html>Date and time that MySQL was started (now - status variable 'Uptime').</html>");
		setLocalToolTipTextOnTableColumnHeader("DAYS_RUNNING",               "<html>Number of days since MySQL was started.</html>");
		setLocalToolTipTextOnTableColumnHeader("READ_ONLY",                  "<html>@@read_only: 1 = only users with CONNECTION_ADMIN (or SUPER) can change data. Normal on a replica.</html>");
		setLocalToolTipTextOnTableColumnHeader("SUPER_READ_ONLY",            "<html>@@super_read_only: 1 = nobody can change data (except the replication applier).</html>");
		setLocalToolTipTextOnTableColumnHeader("REPLICA_CHANNELS",           "<html>Number of replication channels: above 0 = this server is a replica (see the tab 'Replication').</html>");
		setLocalToolTipTextOnTableColumnHeader("MAX_CONNECTIONS",            "<html>@@max_connections: the most connections that are allowed.</html>");
		setLocalToolTipTextOnTableColumnHeader("MAX_USED_CONNECTIONS",       "<html>The most connections at the same time since the server started. Status 'Max_used_connections'</html>");
		setLocalToolTipTextOnTableColumnHeader("THREADS_CONNECTED",          "<html>Connections right now. Status 'Threads_connected'</html>");
		setLocalToolTipTextOnTableColumnHeader("THREADS_RUNNING",            "<html>Connections that are working (not sleeping) right now. Status 'Threads_running'</html>");
		setLocalToolTipTextOnTableColumnHeader("QUESTIONS",                  "<html>Statements sent by the clients (not the ones in stored procedures). Status 'Questions'</html>");
		setLocalToolTipTextOnTableColumnHeader("COM_SELECT",                 "<html>SELECT statements. performance_schema.events_statements_summary_global_by_event_name 'statement/sql/select' (in MySQL 8 the status variable Com_select is not in performance_schema.global_status)</html>");
		setLocalToolTipTextOnTableColumnHeader("COM_INSERT",                 "<html>INSERT statements. 'statement/sql/insert'</html>");
		setLocalToolTipTextOnTableColumnHeader("COM_UPDATE",                 "<html>UPDATE statements. 'statement/sql/update'</html>");
		setLocalToolTipTextOnTableColumnHeader("COM_DELETE",                 "<html>DELETE statements. 'statement/sql/delete'</html>");
		setLocalToolTipTextOnTableColumnHeader("HANDLER_COMMIT",             "<html>Commits inside the server, also autocommit (COMMIT statements only counts the explicit ones). Status 'Handler_commit'</html>");
		setLocalToolTipTextOnTableColumnHeader("HANDLER_ROLLBACK",           "<html>Rollbacks inside the server. Status 'Handler_rollback'</html>");
		setLocalToolTipTextOnTableColumnHeader("CONNECTIONS",                "<html>Connection attempts (successful or not). Status 'Connections'</html>");
		setLocalToolTipTextOnTableColumnHeader("ABORTED_CONNECTS",           "<html>Failed attempts to connect (wrong password, no privileges, timeout...). Status 'Aborted_connects'</html>");
		setLocalToolTipTextOnTableColumnHeader("ABORTED_CLIENTS",            "<html>Connections that ended without a proper close (the client died, or wait_timeout). Status 'Aborted_clients'</html>");
		setLocalToolTipTextOnTableColumnHeader("CONN_ERRORS_MAX_CONN",       "<html>Connections that were refused because max_connections was reached. Status 'Connection_errors_max_connections'</html>");
		setLocalToolTipTextOnTableColumnHeader("SLOW_QUERIES",               "<html>Statements that took longer than long_query_time. Status 'Slow_queries'</html>");
		setLocalToolTipTextOnTableColumnHeader("CREATED_TMP_TABLES",         "<html>Internal temporary tables. Status 'Created_tmp_tables'</html>");
		setLocalToolTipTextOnTableColumnHeader("CREATED_TMP_DISK_TABLES",    "<html>Internal temporary tables that had to be written to disk (too big, or a column type that the memory engine can't hold). Status 'Created_tmp_disk_tables'</html>");
		setLocalToolTipTextOnTableColumnHeader("SELECT_FULL_JOIN",           "<html>Joins without an index (every row of the joined table is read). Status 'Select_full_join'</html>");
		setLocalToolTipTextOnTableColumnHeader("SELECT_SCAN",                "<html>Joins/selects that scanned the whole first table. Status 'Select_scan'</html>");
		setLocalToolTipTextOnTableColumnHeader("SORT_MERGE_PASSES",          "<html>Sorts that did not fit in sort_buffer_size. Status 'Sort_merge_passes'</html>");
		setLocalToolTipTextOnTableColumnHeader("ROWS_READ",                  "<html>Rows read from InnoDB tables. Status 'Innodb_rows_read'</html>");
		setLocalToolTipTextOnTableColumnHeader("ROWS_INSERTED",              "<html>Rows inserted in InnoDB tables. Status 'Innodb_rows_inserted'</html>");
		setLocalToolTipTextOnTableColumnHeader("ROWS_UPDATED",               "<html>Rows updated in InnoDB tables. Status 'Innodb_rows_updated'</html>");
		setLocalToolTipTextOnTableColumnHeader("ROWS_DELETED",               "<html>Rows deleted from InnoDB tables. Status 'Innodb_rows_deleted'</html>");
		setLocalToolTipTextOnTableColumnHeader("BP_READ_REQUESTS",           "<html>Logical reads: pages read from the buffer pool. Status 'Innodb_buffer_pool_read_requests'</html>");
		setLocalToolTipTextOnTableColumnHeader("BP_READS",                   "<html>Physical reads: pages that were not in the buffer pool and had to be read from disk. Status 'Innodb_buffer_pool_reads'</html>");
		setLocalToolTipTextOnTableColumnHeader("BP_HIT_PCT",                 "<html>Buffer pool hit ratio: logical reads that did not need a disk read.<br><b>Formula</b>: 100 - 100 * BP_READS / BP_READ_REQUESTS<br>Abs: since the server started. Diff and Rate: in the sample.</html>");
		setLocalToolTipTextOnTableColumnHeader("BP_WAIT_FREE",               "<html>Times InnoDB had to wait for a free page in the buffer pool (it should be 0: the buffer pool is too small, or the flushing can't keep up). Status 'Innodb_buffer_pool_wait_free'</html>");
		setLocalToolTipTextOnTableColumnHeader("BP_PAGES_FLUSHED",           "<html>Pages written from the buffer pool to disk. Status 'Innodb_buffer_pool_pages_flushed'</html>");
		setLocalToolTipTextOnTableColumnHeader("BP_PAGES_TOTAL",             "<html>Size of the buffer pool, in pages. Status 'Innodb_buffer_pool_pages_total'</html>");
		setLocalToolTipTextOnTableColumnHeader("BP_PAGES_FREE",              "<html>Free pages in the buffer pool. Status 'Innodb_buffer_pool_pages_free'</html>");
		setLocalToolTipTextOnTableColumnHeader("BP_PAGES_DIRTY",             "<html>Pages in the buffer pool that are changed and not yet written to disk. Status 'Innodb_buffer_pool_pages_dirty'</html>");
		setLocalToolTipTextOnTableColumnHeader("REDO_BYTES",                 "<html>Bytes written to the redo log. Status 'Innodb_os_log_written'</html>");
		setLocalToolTipTextOnTableColumnHeader("DATA_READ_BYTES",            "<html>Bytes read from the InnoDB data files. Status 'Innodb_data_read'</html>");
		setLocalToolTipTextOnTableColumnHeader("DATA_WRITTEN_BYTES",         "<html>Bytes written to the InnoDB data files. Status 'Innodb_data_written'</html>");
		setLocalToolTipTextOnTableColumnHeader("ROW_LOCK_WAITS",             "<html>Times a row lock had to be waited for. Status 'Innodb_row_lock_waits'</html>");
		setLocalToolTipTextOnTableColumnHeader("ROW_LOCK_TIME_MS",           "<html>Time spent waiting for row locks, in milliseconds. Status 'Innodb_row_lock_time'</html>");
		setLocalToolTipTextOnTableColumnHeader("DEADLOCKS",                  "<html>Deadlocks. information_schema.INNODB_METRICS 'lock_deadlocks'<br>To see them all in the error log (tab 'Error Log'): innodb_print_all_deadlocks=ON</html>");
		setLocalToolTipTextOnTableColumnHeader("HISTORY_LIST_LENGTH",        "<html>InnoDB history list length: old row versions (undo) that purge has not removed yet. It grows when a transaction is open for a long time. information_schema.INNODB_METRICS 'trx_rseg_history_len'</html>");
		setLocalToolTipTextOnTableColumnHeader("BYTES_RECEIVED",             "<html>Bytes received from the clients. Status 'Bytes_received'</html>");
		setLocalToolTipTextOnTableColumnHeader("BYTES_SENT",                 "<html>Bytes sent to the clients. Status 'Bytes_sent'</html>");
		setLocalToolTipTextOnTableColumnHeader("TABLE_OPEN_CACHE_MISSES",    "<html>Table opens that were not found in the table cache (table_open_cache may be too small). Status 'Table_open_cache_misses'</html>");
		setLocalToolTipTextOnTableColumnHeader("BINLOG_CACHE_DISK_USE",      "<html>Transactions that were too big for binlog_cache_size and used a temporary file. Status 'Binlog_cache_disk_use'</html>");
		setLocalToolTipTextOnTableColumnHeader("OPEN_TRAN_COUNT",            "<html>Open InnoDB transactions right now (also a SELECT that is running). information_schema.INNODB_TRX</html>");
		setLocalToolTipTextOnTableColumnHeader("OLDEST_OPEN_TRAN_IN_SEC",    "<html>How long the oldest open transaction has been open, in seconds. 0 = no open transaction.<br><b>Alarm</b>: oldestOpenTranInSec</html>");
		setLocalToolTipTextOnTableColumnHeader("OLDEST_OPEN_TRAN_IN_SEC_TH", "<html>The threshold (in seconds) of the alarm 'oldestOpenTranInSec'.</html>");
		setLocalToolTipTextOnTableColumnHeader("OLDEST_OPEN_TRAN_PID",       "<html>The connection (processlist id) that has the oldest open transaction.</html>");
		setLocalToolTipTextOnTableColumnHeader("OLDEST_OPEN_TRAN_TIME",      "<html>When the oldest open transaction started.</html>");
		setLocalToolTipTextOnTableColumnHeader("OLDEST_OPEN_TRAN_STATE",     "<html>State of the oldest open transaction: RUNNING, LOCK WAIT, ROLLING BACK, COMMITTING.</html>");
		setLocalToolTipTextOnTableColumnHeader("OLDEST_OPEN_TRAN_USER",      "<html>User of the connection that has the oldest open transaction.</html>");
		setLocalToolTipTextOnTableColumnHeader("OLDEST_OPEN_TRAN_HOST",      "<html>Client host of the connection that has the oldest open transaction.</html>");
		setLocalToolTipTextOnTableColumnHeader("COUNT_ROW_LOCK_WAIT",        "<html>Sessions that waits for a row lock right now. information_schema.INNODB_TRX, trx_state = 'LOCK WAIT'</html>");
		setLocalToolTipTextOnTableColumnHeader("COUNT_MDL_WAIT",             "<html>Sessions that waits for a metadata lock right now (for example an ALTER TABLE that waits for an open transaction, and everybody after it). performance_schema.metadata_locks, LOCK_STATUS = 'PENDING'</html>");
		setLocalToolTipTextOnTableColumnHeader("COUNT_BLOCKING_WAIT",        "<html>Sessions that waits for a lock right now: COUNT_ROW_LOCK_WAIT + COUNT_MDL_WAIT.<br><b>Alarm</b>: BlockingLockCount</html>");
		setLocalToolTipTextOnTableColumnHeader("MAX_BLOCKING_WAIT_IN_SEC",   "<html>The longest time a session has waited for a lock right now, in seconds.</html>");
		setLocalToolTipTextOnTableColumnHeader("BLOCKING_WAIT_IN_SEC_TH",    "<html>The threshold (in seconds) of the alarm 'BlockingLockCount'.</html>");
	}
}
