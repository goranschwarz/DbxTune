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
import com.dbxtune.alarm.events.AlarmEventLongRunningTransaction;
import com.dbxtune.central.pcs.CentralPersistReader;
import com.dbxtune.cm.CmSettingsHelper;
import com.dbxtune.cm.CmSummaryAbstract;
import com.dbxtune.cm.CounterSample;
import com.dbxtune.cm.CounterSetTemplates;
import com.dbxtune.cm.CounterSetTemplates.Type;
import com.dbxtune.cm.CountersModel;
import com.dbxtune.cm.db2.gui.CmSummaryPanel;
import com.dbxtune.graph.TrendGraphDataPoint;
import com.dbxtune.graph.TrendGraphDataPoint.LabelType;
import com.dbxtune.sql.conn.DbxConnection;
import com.dbxtune.sql.conn.info.DbmsVersionInfo;
import com.dbxtune.utils.Configuration;

/**
 * One row that tells how the database is doing: connections, transactions, rows, buffer pool, locks, the log, the oldest open transaction...
 * <p>
 * Db2Tune monitors ONE database, so this is the database (MON_GET_DATABASE), summed over the members if there are more than one.
 *
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
		"Overview of how the database performs." +
		"</html>";

	public static final String   GROUP_NAME       = null;
	public static final String   GUI_ICON_FILE    = "images/" + CM_NAME + ".png";

	public static final long     NEED_SRV_VERSION = 0;
	public static final long     NEED_CE_VERSION  = 0;

	public static final String[] MON_TABLES       = new String[] {"MON_GET_DATABASE", "MON_GET_INSTANCE", "MON_GET_TRANSACTION_LOG", "MON_GET_APPL_LOCKWAIT", "MON_GET_UNIT_OF_WORK"};
	public static final String[] NEED_ROLES       = new String[] {};
	public static final String[] NEED_CONFIG      = new String[] {};

	public static final String[] PCT_COLUMNS      = new String[] {};
	public static final String[] DIFF_COLUMNS     = new String[] {
		"TOTAL_CONS",
		"TOTAL_APP_COMMITS",
		"TOTAL_APP_ROLLBACKS",
		"ACT_COMPLETED_TOTAL",
		"FAILED_SQL_STMTS",
		"ROWS_READ",
		"ROWS_RETURNED",
		"ROWS_MODIFIED",
		"BP_LOGICAL_READS",
		"BP_PHYSICAL_READS",
		"POOL_WRITES",
		"LOCK_WAITS",
		"LOCK_WAIT_TIME",
		"LOCK_TIMEOUTS",
		"DEADLOCKS",
		"LOCK_ESCALS",
		"TOTAL_SORTS",
		"SORT_OVERFLOWS",
		"TOTAL_CPU_TIME",
		"TOTAL_RQST_TIME",
		"TOTAL_WAIT_TIME",
		"PKG_CACHE_LOOKUPS",
		"PKG_CACHE_INSERTS",
		"PKG_CACHE_NUM_OVERFLOWS",
		"LOG_WRITE_MB"
	};

	// The counters starts from 0 when the database is activated again
	public static final boolean  NEGATIVE_DIFF_COUNTERS_TO_ZERO = true;
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
	public static final String GRAPH_NAME_TRANSACTION        = "TransGraph";
	public static final String GRAPH_NAME_STATEMENTS         = "StatementsGraph";
	public static final String GRAPH_NAME_ROWS               = "RowsGraph";
	public static final String GRAPH_NAME_READS              = "ReadsGraph";
	public static final String GRAPH_NAME_BP_HIT_PCT         = "BpHitPctGraph";
	public static final String GRAPH_NAME_LOCKS              = "LocksGraph";
	public static final String GRAPH_NAME_BLOCKING_LOCKS     = "BlockingLocksGraph";
	public static final String GRAPH_NAME_OLDEST_TRAN_IN_SEC = "OldestTranInSecGraph";
	public static final String GRAPH_NAME_LOG_USED_PCT       = "LogUsedPctGraph";
	public static final String GRAPH_NAME_CPU                = "CpuGraph";
	public static final String GRAPH_NAME_WAIT_PCT           = "WaitPctGraph";
	public static final String GRAPH_NAME_SORTS              = "SortsGraph";

	private void addTrendGraphs()
	{
		addTrendGraph(GRAPH_NAME_CONNECTION,
			"Connections and Executing Applications",    // Menu CheckBox text
			"Connections and Executing Applications (" + SHORT_NAME + ")", // Label
			TrendGraphDataPoint.createGraphProps(TrendGraphDataPoint.Y_AXIS_SCALE_LABELS_NORMAL, CentralPersistReader.SampleType.MAX_OVER_SAMPLES),
			new String[] { "Connections", "Executing in Db2" },
			LabelType.Static,
			TrendGraphDataPoint.Category.OPERATIONS,
			false, // is Percent Graph
			true,  // visible at start
			0,     // graph is valid from Server Version. 0 = All Versions; >0 = Valid from this version and above
			-1);   // minimum height

		addTrendGraph(GRAPH_NAME_TRANSACTION,
			"Commits and Rollbacks per Second",    // Menu CheckBox text
			"Commits and Rollbacks per Second, from the applications (" + SHORT_NAME + ")", // Label
			TrendGraphDataPoint.createGraphProps(TrendGraphDataPoint.Y_AXIS_SCALE_LABELS_PERSEC, CentralPersistReader.SampleType.MAX_OVER_SAMPLES),
			new String[] { "Commits", "Rollbacks" },
			LabelType.Static,
			TrendGraphDataPoint.Category.OPERATIONS,
			false, // is Percent Graph
			true,  // visible at start
			0,     // graph is valid from Server Version. 0 = All Versions; >0 = Valid from this version and above
			-1);   // minimum height

		addTrendGraph(GRAPH_NAME_STATEMENTS,
			"Statements per Second",    // Menu CheckBox text
			"Statements (Activities) Completed and Failed per Second (" + SHORT_NAME + ")", // Label
			TrendGraphDataPoint.createGraphProps(TrendGraphDataPoint.Y_AXIS_SCALE_LABELS_PERSEC, CentralPersistReader.SampleType.MAX_OVER_SAMPLES),
			new String[] { "Completed", "Failed" },
			LabelType.Static,
			TrendGraphDataPoint.Category.OPERATIONS,
			false, // is Percent Graph
			true,  // visible at start
			0,     // graph is valid from Server Version. 0 = All Versions; >0 = Valid from this version and above
			-1);   // minimum height

		addTrendGraph(GRAPH_NAME_ROWS,
			"Rows Read, Returned and Modified per Second",    // Menu CheckBox text
			"Rows Read, Returned and Modified per Second (" + SHORT_NAME + ")", // Label
			TrendGraphDataPoint.createGraphProps(TrendGraphDataPoint.Y_AXIS_SCALE_LABELS_PERSEC, CentralPersistReader.SampleType.MAX_OVER_SAMPLES),
			new String[] { "Read", "Returned", "Modified" },
			LabelType.Static,
			TrendGraphDataPoint.Category.OPERATIONS,
			false, // is Percent Graph
			true,  // visible at start
			0,     // graph is valid from Server Version. 0 = All Versions; >0 = Valid from this version and above
			-1);   // minimum height

		addTrendGraph(GRAPH_NAME_READS,
			"Buffer Pool Reads and Writes per Second",    // Menu CheckBox text
			"Buffer Pool: Logical Reads, Physical Reads (from disk) and Page Writes (to disk) per Second (" + SHORT_NAME + ")", // Label
			TrendGraphDataPoint.createGraphProps(TrendGraphDataPoint.Y_AXIS_SCALE_LABELS_PERSEC, CentralPersistReader.SampleType.MAX_OVER_SAMPLES),
			new String[] { "Logical Reads", "Physical Reads", "Page Writes" },
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

		addTrendGraph(GRAPH_NAME_LOCKS,
			"Lock Waits, Timeouts, Deadlocks and Escalations per Second",    // Menu CheckBox text
			"Lock Waits, Lock Timeouts, Deadlocks and Lock Escalations per Second (" + SHORT_NAME + ")", // Label
			TrendGraphDataPoint.createGraphProps(TrendGraphDataPoint.Y_AXIS_SCALE_LABELS_PERSEC, CentralPersistReader.SampleType.MAX_OVER_SAMPLES),
			new String[] { "Lock Waits", "Lock Timeouts", "Deadlocks", "Lock Escalations" },
			LabelType.Static,
			TrendGraphDataPoint.Category.LOCK,
			false, // is Percent Graph
			true,  // visible at start
			0,     // graph is valid from Server Version. 0 = All Versions; >0 = Valid from this version and above
			-1);   // minimum height

		addTrendGraph(GRAPH_NAME_BLOCKING_LOCKS,
			"Blocking Locks",    // Menu CheckBox text
			"Number of Applications that Waits for a Lock (" + SHORT_NAME + ")", // Label
			TrendGraphDataPoint.createGraphProps(TrendGraphDataPoint.Y_AXIS_SCALE_LABELS_NORMAL, CentralPersistReader.SampleType.MAX_OVER_SAMPLES),
			new String[] { "Lock Waiters" },
			LabelType.Static,
			TrendGraphDataPoint.Category.LOCK,
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

		addTrendGraph(GRAPH_NAME_LOG_USED_PCT,
			"Transaction Log Used in Percent",    // Menu CheckBox text
			"Transaction Log Used in Percent (" + SHORT_NAME + ")", // Label
			TrendGraphDataPoint.createGraphProps(TrendGraphDataPoint.Y_AXIS_SCALE_LABELS_PERCENT, CentralPersistReader.SampleType.MAX_OVER_SAMPLES),
			new String[] { "Log Used Pct" },
			LabelType.Static,
			TrendGraphDataPoint.Category.SPACE,
			true,  // is Percent Graph
			true,  // visible at start
			0,     // graph is valid from Server Version. 0 = All Versions; >0 = Valid from this version and above
			-1);   // minimum height

		addTrendGraph(GRAPH_NAME_CPU,
			"CPU Seconds Used per Second",    // Menu CheckBox text
			"CPU Seconds Used by the Database per Second (user + system, all agents) (" + SHORT_NAME + ")", // Label
			TrendGraphDataPoint.createGraphProps(TrendGraphDataPoint.Y_AXIS_SCALE_LABELS_SECONDS, CentralPersistReader.SampleType.MAX_OVER_SAMPLES),
			new String[] { "CPU Seconds" },
			LabelType.Static,
			TrendGraphDataPoint.Category.CPU,
			false, // is Percent Graph
			true,  // visible at start
			0,     // graph is valid from Server Version. 0 = All Versions; >0 = Valid from this version and above
			-1);   // minimum height

		addTrendGraph(GRAPH_NAME_WAIT_PCT,
			"Wait Time in Percent of Request Time",    // Menu CheckBox text
			"Wait Time in Percent of the Time Spent on Requests (" + SHORT_NAME + ")", // Label
			TrendGraphDataPoint.createGraphProps(TrendGraphDataPoint.Y_AXIS_SCALE_LABELS_PERCENT, CentralPersistReader.SampleType.MAX_OVER_SAMPLES),
			new String[] { "Wait Pct" },
			LabelType.Static,
			TrendGraphDataPoint.Category.WAITS,
			true,  // is Percent Graph
			false, // visible at start
			0,     // graph is valid from Server Version. 0 = All Versions; >0 = Valid from this version and above
			-1);   // minimum height

		addTrendGraph(GRAPH_NAME_SORTS,
			"Sorts and Sort Overflows per Second",    // Menu CheckBox text
			"Sorts and Sort Overflows (sorts that did not fit in the sort heap) per Second (" + SHORT_NAME + ")", // Label
			TrendGraphDataPoint.createGraphProps(TrendGraphDataPoint.Y_AXIS_SCALE_LABELS_PERSEC, CentralPersistReader.SampleType.MAX_OVER_SAMPLES),
			new String[] { "Sorts", "Sort Overflows" },
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

	/** When the Db2 instance was started (DB2START), for example used when the alarm 'SrvDown' is cancelled: was it restarted? */
	@Override
	protected String getDbmsStartTimeColumnName()
	{
		return "START_TIME";
	}

	@Override
	public String getSqlForVersion(DbxConnection conn, DbmsVersionInfo versionInfo)
	{
		Configuration conf = Configuration.getCombinedConfiguration();
		int oldestOpenTranInSecThreshold = conf.getIntProperty(PROPKEY_alarm_oldestOpenTranInSec, DEFAULT_alarm_oldestOpenTranInSec);
		int blockingWaitInSecThreshold   = (int) conf.getDoubleProperty(PROPKEY_alarm_BlockingLockCount_minWaitTimeSec, DEFAULT_alarm_BlockingLockCount_minWaitTimeSec);

		// NOTE: MON_GET_DATABASE(-2) has one row per member (pureScale / DPF), the counters are summed
		// NOTE: An OPEN transaction is a unit of work without UOW_STOP_TIME (an idle connection after a commit still shows its last unit of work, with a stop time)
		//       Our own connection is not counted (it always has an open unit of work while it runs this)
		// NOTE: TOTAL_CPU_TIME is in microseconds, the other times are in milliseconds
		String sql = ""
			+ "select /* ${cmCollectorName} */ \n"
			+ "     (select SERVICE_LEVEL from SYSIBMADM.ENV_INST_INFO)                        as VERSION \n"
			+ "    ,CURRENT TIMESTAMP                                                          as TIME_NOW \n"
			+ "    ,INT(CURRENT TIMEZONE / 10000) * 60 + MOD(INT(CURRENT TIMEZONE / 100), 100) as UTC_MINUTE_DIFF \n"
			+ "    ,(select HOST_NAME from table(SYSPROC.ENV_GET_SYS_INFO()) x)               as HOST \n"
			+ "    ,(select INST_NAME from SYSIBMADM.ENV_INST_INFO)                            as INSTANCE_NAME \n"
			+ "    ,CURRENT SERVER                                                             as DATABASE_NAME \n"
			+ "    ,(select min(DB2START_TIME) from table(MON_GET_INSTANCE(-2)) x)            as START_TIME \n"
			+ "    ,d.DB_ACTIVATION_TIME \n"
			+ "    ,d.DB_STATUS \n"
			+ "    ,d.DB_ACTIVATION_STATE \n"
			+ "    ,(select CAST(VALUE AS VARCHAR(20)) from SYSIBMADM.DBCFG where NAME = 'hadr_db_role' order by MEMBER fetch first 1 rows only) as HADR_ROLE \n"
			+ "    ,NULLIF(d.LAST_BACKUP, TIMESTAMP('0001-01-01-00.00.00'))                    as LAST_BACKUP \n"
			+ "    ,d.APPLS_CUR_CONS \n"
			+ "    ,d.APPLS_IN_DB2 \n"
			+ "    ,d.CONNECTIONS_TOP \n"
			+ "    ,d.TOTAL_CONS \n"
			+ "    ,d.TOTAL_APP_COMMITS \n"
			+ "    ,d.TOTAL_APP_ROLLBACKS \n"
			+ "    ,d.ACT_COMPLETED_TOTAL \n"
			+ "    ,d.FAILED_SQL_STMTS \n"
			+ "    ,d.ROWS_READ \n"
			+ "    ,d.ROWS_RETURNED \n"
			+ "    ,d.ROWS_MODIFIED \n"
			+ "    ,CAST(CASE WHEN d.ROWS_RETURNED > 0 THEN d.ROWS_READ * 1.0 / d.ROWS_RETURNED END AS DECIMAL(15,1)) as ROWS_READ_PER_RETURNED \n"
			+ "    ,d.BP_LOGICAL_READS \n"
			+ "    ,d.BP_PHYSICAL_READS \n"
			+ "    ,CAST(CASE WHEN d.BP_LOGICAL_READS > 0 THEN 100.0 - d.BP_PHYSICAL_READS * 100.0 / d.BP_LOGICAL_READS END AS DECIMAL(5,1)) as BP_HIT_PCT \n"
			+ "    ,d.POOL_WRITES \n"
			+ "    ,d.NUM_LOCKS_HELD \n"
			+ "    ,d.NUM_LOCKS_WAITING \n"
			+ "    ,d.LOCK_WAITS \n"
			+ "    ,d.LOCK_WAIT_TIME \n"
			+ "    ,d.LOCK_TIMEOUTS \n"
			+ "    ,d.DEADLOCKS \n"
			+ "    ,d.LOCK_ESCALS \n"
			+ "    ,d.TOTAL_SORTS \n"
			+ "    ,d.SORT_OVERFLOWS \n"
			+ "    ,d.TOTAL_CPU_TIME \n"
			+ "    ,d.TOTAL_RQST_TIME \n"
			+ "    ,d.TOTAL_WAIT_TIME \n"
			+ "    ,CAST(CASE WHEN d.TOTAL_RQST_TIME > 0 THEN d.TOTAL_WAIT_TIME * 100.0 / d.TOTAL_RQST_TIME END AS DECIMAL(5,1)) as WAIT_TIME_PCT \n"
			+ "    ,d.PKG_CACHE_LOOKUPS \n"
			+ "    ,d.PKG_CACHE_INSERTS \n"
			+ "    ,CAST(CASE WHEN d.PKG_CACHE_LOOKUPS > 0 THEN 100.0 - d.PKG_CACHE_INSERTS * 100.0 / d.PKG_CACHE_LOOKUPS END AS DECIMAL(5,1)) as PKG_CACHE_HIT_PCT \n"
			+ "    ,d.PKG_CACHE_NUM_OVERFLOWS \n"
			+ "    ,l.LOG_USED_PCT \n"
			+ "    ,l.LOG_WRITE_MB \n"
			+ "    ,w.LOCK_WAITERS \n"
			+ "    ,w.LOCK_BLOCKERS \n"
			+ "    ,w.MAX_LOCK_WAIT_SEC \n"
			+ "    ," + blockingWaitInSecThreshold + "                                         as BLOCKING_WAIT_IN_SEC_TH \n" // TH = THRESHOLD
			+ "    ,t.OPEN_TRAN_COUNT \n"
			+ "    ,COALESCE(SECONDS_BETWEEN(CURRENT TIMESTAMP, o.UOW_START_TIME), 0)          as OLDEST_OPEN_TRAN_IN_SEC \n"
			+ "    ," + oldestOpenTranInSecThreshold + "                                       as OLDEST_OPEN_TRAN_IN_SEC_TH \n" // TH = THRESHOLD
			+ "    ,o.APPLICATION_HANDLE                                                       as OLDEST_OPEN_TRAN_APPL_HANDLE \n"
			+ "    ,o.SESSION_AUTH_ID                                                          as OLDEST_OPEN_TRAN_USER \n"
			+ "    ,o.APPLICATION_NAME                                                         as OLDEST_OPEN_TRAN_APPL_NAME \n"
			+ "    ,o.CLIENT_HOSTNAME                                                          as OLDEST_OPEN_TRAN_HOST \n"
			+ "    ,o.WORKLOAD_OCCURRENCE_STATE                                                as OLDEST_OPEN_TRAN_STATE \n"
			+ "    ,o.UOW_START_TIME                                                           as OLDEST_OPEN_TRAN_TIME \n"
			+ "    ,CAST(o.UOW_LOG_SPACE_USED / 1048576.0 AS DECIMAL(15,1))                    as OLDEST_OPEN_TRAN_LOG_MB \n"
			// NOTE: LAST_BACKUP is NULL for a database that has never been backed up: max() over a NULL gives the warning SQLSTATE 01003 (every sample), so it is COALESCE:d
			+ "from (select min(DB_CONN_TIME) as DB_ACTIVATION_TIME, max(DB_STATUS) as DB_STATUS, max(DB_ACTIVATION_STATE) as DB_ACTIVATION_STATE, max(COALESCE(LAST_BACKUP, TIMESTAMP('0001-01-01-00.00.00'))) as LAST_BACKUP \n"
			+ "            ,sum(APPLS_CUR_CONS) as APPLS_CUR_CONS, sum(APPLS_IN_DB2) as APPLS_IN_DB2, sum(CONNECTIONS_TOP) as CONNECTIONS_TOP, sum(TOTAL_CONS) as TOTAL_CONS \n"
			+ "            ,sum(TOTAL_APP_COMMITS) as TOTAL_APP_COMMITS, sum(TOTAL_APP_ROLLBACKS) as TOTAL_APP_ROLLBACKS, sum(ACT_COMPLETED_TOTAL) as ACT_COMPLETED_TOTAL, sum(FAILED_SQL_STMTS) as FAILED_SQL_STMTS \n"
			+ "            ,sum(ROWS_READ) as ROWS_READ, sum(ROWS_RETURNED) as ROWS_RETURNED, sum(ROWS_MODIFIED) as ROWS_MODIFIED \n"
			+ "            ,sum(POOL_DATA_L_READS + POOL_INDEX_L_READS + POOL_XDA_L_READS + POOL_COL_L_READS + POOL_TEMP_DATA_L_READS + POOL_TEMP_INDEX_L_READS + POOL_TEMP_XDA_L_READS + POOL_TEMP_COL_L_READS) as BP_LOGICAL_READS \n"
			+ "            ,sum(POOL_DATA_P_READS + POOL_INDEX_P_READS + POOL_XDA_P_READS + POOL_COL_P_READS + POOL_TEMP_DATA_P_READS + POOL_TEMP_INDEX_P_READS + POOL_TEMP_XDA_P_READS + POOL_TEMP_COL_P_READS) as BP_PHYSICAL_READS \n"
			+ "            ,sum(POOL_DATA_WRITES + POOL_INDEX_WRITES + POOL_XDA_WRITES + POOL_COL_WRITES) as POOL_WRITES \n"
			+ "            ,sum(NUM_LOCKS_HELD) as NUM_LOCKS_HELD, sum(NUM_LOCKS_WAITING) as NUM_LOCKS_WAITING \n"
			+ "            ,sum(LOCK_WAITS) as LOCK_WAITS, sum(LOCK_WAIT_TIME) as LOCK_WAIT_TIME, sum(LOCK_TIMEOUTS) as LOCK_TIMEOUTS, sum(DEADLOCKS) as DEADLOCKS, sum(LOCK_ESCALS) as LOCK_ESCALS \n"
			+ "            ,sum(TOTAL_SORTS) as TOTAL_SORTS, sum(SORT_OVERFLOWS) as SORT_OVERFLOWS \n"
			+ "            ,sum(TOTAL_CPU_TIME) as TOTAL_CPU_TIME, sum(TOTAL_RQST_TIME) as TOTAL_RQST_TIME, sum(TOTAL_WAIT_TIME) as TOTAL_WAIT_TIME \n"
			+ "            ,sum(PKG_CACHE_LOOKUPS) as PKG_CACHE_LOOKUPS, sum(PKG_CACHE_INSERTS) as PKG_CACHE_INSERTS, sum(PKG_CACHE_NUM_OVERFLOWS) as PKG_CACHE_NUM_OVERFLOWS \n"
			+ "      from table(MON_GET_DATABASE(-2)) x) d \n"
			+ "cross join (select CAST(max(CASE WHEN TOTAL_LOG_AVAILABLE >= 0 AND (TOTAL_LOG_USED + TOTAL_LOG_AVAILABLE) > 0 THEN TOTAL_LOG_USED * 100.0 / (TOTAL_LOG_USED + TOTAL_LOG_AVAILABLE) ELSE 0 END) AS DECIMAL(5,1)) as LOG_USED_PCT \n"
			+ "                  ,CAST(sum(LOG_WRITES) * 4096 / 1048576.0 AS DECIMAL(19,1)) as LOG_WRITE_MB \n"
			+ "            from table(MON_GET_TRANSACTION_LOG(-2)) x) l \n"
			+ "cross join (select count(distinct REQ_APPLICATION_HANDLE) as LOCK_WAITERS \n"
			+ "                  ,count(distinct HLD_APPLICATION_HANDLE) as LOCK_BLOCKERS \n"
			+ "                  ,COALESCE(max(SECONDS_BETWEEN(CURRENT TIMESTAMP, LOCK_WAIT_START_TIME)), 0) as MAX_LOCK_WAIT_SEC \n"
			+ "            from table(MON_GET_APPL_LOCKWAIT(NULL, -2)) x) w \n"
			+ "cross join (select count(*) as OPEN_TRAN_COUNT \n"
			+ "            from table(MON_GET_UNIT_OF_WORK(NULL, -2)) x \n"
			+ "            where UOW_STOP_TIME is NULL and UOW_START_TIME is not NULL and APPLICATION_HANDLE <> MON_GET_APPLICATION_HANDLE()) t \n"
			+ "left join (select APPLICATION_HANDLE, SESSION_AUTH_ID, APPLICATION_NAME, CLIENT_HOSTNAME, WORKLOAD_OCCURRENCE_STATE, UOW_START_TIME, UOW_LOG_SPACE_USED \n"
			+ "           from table(MON_GET_UNIT_OF_WORK(NULL, -2)) x \n"
			+ "           where UOW_STOP_TIME is NULL and UOW_START_TIME is not NULL and APPLICATION_HANDLE <> MON_GET_APPLICATION_HANDLE() \n"
			+ "           order by UOW_START_TIME \n"
			+ "           fetch first 1 rows only) o on 1 = 1 \n"
			+ "";

		return sql;
	}

	/** If we can't use the functions: disable the CM, and tell why (the summary can't be disabled for real, but the log and the tooltip says what is wrong) */
	@Override
	public boolean checkDependsOnOther(DbxConnection conn)
	{
		return Db2CmUtils.checkFunctionsAreExecutable(this, conn, "MON_GET_DATABASE(-2)", "MON_GET_INSTANCE(-2)", "MON_GET_TRANSACTION_LOG(-2)", "MON_GET_APPL_LOCKWAIT(NULL, -2)", "MON_GET_UNIT_OF_WORK(NULL, -2)");
	}

	/**
	 * Hit ratios: the absolute value is "since the database was activated", in diff (and rate) it is for the sample:<br>
	 * BP_HIT_PCT        = 100 - 100 * (diff BP_PHYSICAL_READS / diff BP_LOGICAL_READS)<br>
	 * PKG_CACHE_HIT_PCT = 100 - 100 * (diff PKG_CACHE_INSERTS / diff PKG_CACHE_LOOKUPS)<br>
	 * WAIT_TIME_PCT     = 100 * diff TOTAL_WAIT_TIME / diff TOTAL_RQST_TIME<br>
	 * ROWS_READ_PER_RETURNED = diff ROWS_READ / diff ROWS_RETURNED
	 */
	@Override
	public void localCalculation(CounterSample prevSample, CounterSample newSample, CounterSample diffData)
	{
		if (diffData.getRowCount() == 0)
			return;

		setPctInDiff(diffData, "BP_HIT_PCT",             "BP_PHYSICAL_READS", "BP_LOGICAL_READS",  true);
		setPctInDiff(diffData, "PKG_CACHE_HIT_PCT",      "PKG_CACHE_INSERTS", "PKG_CACHE_LOOKUPS", true);
		setPctInDiff(diffData, "WAIT_TIME_PCT",          "TOTAL_WAIT_TIME",   "TOTAL_RQST_TIME",   false);

		int pos_perReturned = diffData.findColumn("ROWS_READ_PER_RETURNED");
		int pos_read        = diffData.findColumn("ROWS_READ");
		int pos_returned    = diffData.findColumn("ROWS_RETURNED");
		if (pos_perReturned != -1 && pos_read != -1 && pos_returned != -1)
		{
			double returned = diffData.getValueAsDouble(0, pos_returned, 0d);
			double read     = diffData.getValueAsDouble(0, pos_read,     0d);
			diffData.setValueAt(returned <= 0 ? null : new BigDecimal(read / returned).setScale(1, RoundingMode.HALF_EVEN), 0, pos_perReturned);
		}
	}

	/** In the diff data: pctCol = (hitRatio ? 100 - 100 * part / total : 100 * part / total), NULL if total is 0 */
	private static void setPctInDiff(CounterSample diffData, String pctCol, String partCol, String totalCol, boolean hitRatio)
	{
		int pos_pct   = diffData.findColumn(pctCol);
		int pos_part  = diffData.findColumn(partCol);
		int pos_total = diffData.findColumn(totalCol);
		if (pos_pct == -1 || pos_part == -1 || pos_total == -1)
			return;

		double part  = diffData.getValueAsDouble(0, pos_part,  0d);
		double total = diffData.getValueAsDouble(0, pos_total, 0d);

		BigDecimal pct = null; // nothing in the sample: no ratio
		if (total > 0)
		{
			double val = hitRatio ? 100.0 - 100.0 * part / total : 100.0 * part / total;
			pct = new BigDecimal(Math.max(0, Math.min(100, val))).setScale(1, RoundingMode.HALF_EVEN);
		}
		diffData.setValueAt(pct, 0, pos_pct);
	}

	@Override
	public void updateGraphData(TrendGraphDataPoint tgdp)
	{
		if (GRAPH_NAME_CONNECTION.equals(tgdp.getName()))
		{
			Double[] arr = new Double[2];
			arr[0] = this.getAbsValueAsDouble(0, "APPLS_CUR_CONS");
			arr[1] = this.getAbsValueAsDouble(0, "APPLS_IN_DB2");
			tgdp.setDataPoint(this.getTimestamp(), arr);
		}

		if (GRAPH_NAME_TRANSACTION.equals(tgdp.getName()))
		{
			Double[] arr = new Double[2];
			arr[0] = this.getRateValueAsDouble(0, "TOTAL_APP_COMMITS");
			arr[1] = this.getRateValueAsDouble(0, "TOTAL_APP_ROLLBACKS");
			tgdp.setDataPoint(this.getTimestamp(), arr);
		}

		if (GRAPH_NAME_STATEMENTS.equals(tgdp.getName()))
		{
			Double[] arr = new Double[2];
			arr[0] = this.getRateValueAsDouble(0, "ACT_COMPLETED_TOTAL");
			arr[1] = this.getRateValueAsDouble(0, "FAILED_SQL_STMTS");
			tgdp.setDataPoint(this.getTimestamp(), arr);
		}

		if (GRAPH_NAME_ROWS.equals(tgdp.getName()))
		{
			Double[] arr = new Double[3];
			arr[0] = this.getRateValueAsDouble(0, "ROWS_READ");
			arr[1] = this.getRateValueAsDouble(0, "ROWS_RETURNED");
			arr[2] = this.getRateValueAsDouble(0, "ROWS_MODIFIED");
			tgdp.setDataPoint(this.getTimestamp(), arr);
		}

		if (GRAPH_NAME_READS.equals(tgdp.getName()))
		{
			Double[] arr = new Double[3];
			arr[0] = this.getRateValueAsDouble(0, "BP_LOGICAL_READS");
			arr[1] = this.getRateValueAsDouble(0, "BP_PHYSICAL_READS");
			arr[2] = this.getRateValueAsDouble(0, "POOL_WRITES");
			tgdp.setDataPoint(this.getTimestamp(), arr);
		}

		if (GRAPH_NAME_BP_HIT_PCT.equals(tgdp.getName()))
		{
			// No logical reads in the sample: no point in the graph
			Double hitPct = this.getDiffValueAsDouble(0, "BP_HIT_PCT");
			if (hitPct != null)
				tgdp.setDataPoint(this.getTimestamp(), new Double[] { hitPct });
		}

		if (GRAPH_NAME_LOCKS.equals(tgdp.getName()))
		{
			Double[] arr = new Double[4];
			arr[0] = this.getRateValueAsDouble(0, "LOCK_WAITS");
			arr[1] = this.getRateValueAsDouble(0, "LOCK_TIMEOUTS");
			arr[2] = this.getRateValueAsDouble(0, "DEADLOCKS");
			arr[3] = this.getRateValueAsDouble(0, "LOCK_ESCALS");
			tgdp.setDataPoint(this.getTimestamp(), arr);
		}

		if (GRAPH_NAME_BLOCKING_LOCKS.equals(tgdp.getName()))
		{
			tgdp.setDataPoint(this.getTimestamp(), new Double[] { this.getAbsValueAsDouble(0, "LOCK_WAITERS") });
		}

		if (GRAPH_NAME_OLDEST_TRAN_IN_SEC.equals(tgdp.getName()))
		{
			tgdp.setDataPoint(this.getTimestamp(), new Double[] { this.getAbsValueAsDouble(0, "OLDEST_OPEN_TRAN_IN_SEC") });
		}

		if (GRAPH_NAME_LOG_USED_PCT.equals(tgdp.getName()))
		{
			tgdp.setDataPoint(this.getTimestamp(), new Double[] { this.getAbsValueAsDouble(0, "LOG_USED_PCT") });
		}

		if (GRAPH_NAME_CPU.equals(tgdp.getName()))
		{
			// TOTAL_CPU_TIME is in microseconds
			Double cpuUsPerSec = this.getRateValueAsDouble(0, "TOTAL_CPU_TIME");
			tgdp.setDataPoint(this.getTimestamp(), new Double[] { cpuUsPerSec == null ? null : cpuUsPerSec / 1_000_000.0 });
		}

		if (GRAPH_NAME_WAIT_PCT.equals(tgdp.getName()))
		{
			// No requests in the sample: no point in the graph
			Double waitPct = this.getDiffValueAsDouble(0, "WAIT_TIME_PCT");
			if (waitPct != null)
				tgdp.setDataPoint(this.getTimestamp(), new Double[] { waitPct });
		}

		if (GRAPH_NAME_SORTS.equals(tgdp.getName()))
		{
			Double[] arr = new Double[2];
			arr[0] = this.getRateValueAsDouble(0, "TOTAL_SORTS");
			arr[1] = this.getRateValueAsDouble(0, "SORT_OVERFLOWS");
			tgdp.setDataPoint(this.getTimestamp(), arr);
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
		doAlarmIfDbmsVersionStringWasChanged("VERSION");

		//-------------------------------------------------------
		// BlockingLockCount
		//-------------------------------------------------------
		if (isSystemAlarmsForColumnEnabledAndInTimeRange("BlockingLockCount"))
		{
			Integer lockWaiters      = cm.getAbsValueAsInteger(0, "LOCK_WAITERS");
			Integer maxWaitTimeInSec = cm.getAbsValueAsInteger(0, "MAX_LOCK_WAIT_SEC");

			if (lockWaiters != null && maxWaitTimeInSec != null)
			{
				int    threshold     = Configuration.getCombinedConfiguration().getIntProperty   (PROPKEY_alarm_BlockingLockCount               , DEFAULT_alarm_BlockingLockCount);
				double waitThreshold = Configuration.getCombinedConfiguration().getDoubleProperty(PROPKEY_alarm_BlockingLockCount_minWaitTimeSec, DEFAULT_alarm_BlockingLockCount_minWaitTimeSec);

				if (debugPrint || _logger.isDebugEnabled())
					System.out.println("##### sendAlarmRequest(" + cm.getName() + "): BlockingLockCount: threshold=" + threshold + ", LOCK_WAITERS='" + lockWaiters + "', waitThreshold=" + waitThreshold + ", MAX_LOCK_WAIT_SEC='" + maxWaitTimeInSec + "'.");

				if (lockWaiters > threshold && maxWaitTimeInSec > waitThreshold)
				{
					AlarmEvent ae = new AlarmEventBlockingLockAlarm(cm, waitThreshold, lockWaiters, maxWaitTimeInSec);

					// The below is called from AlarmHandler on: Raise, RE-RAISE & CANCEL
					ae.setAlarmDescriptionProvider(this, new AlarmDescriptionProviderBase(null, null, waitThreshold)
					{
						@Override
						public void setValues(CountersModel cm, AlarmEvent alarmEvent, AlarmPhase phase)
						{
							// Always: (RAISE, RE-RAISE, CANCEL) - Set graph values
							Integer LOCK_WAITERS  = cm.getAbsValueAsInteger(0, "LOCK_WAITERS");
							Integer LOCK_BLOCKERS = cm.getAbsValueAsInteger(0, "LOCK_BLOCKERS");
							String  what = "Applications waiting for a lock: " + LOCK_WAITERS + ", applications that hold the locks they wait for: " + LOCK_BLOCKERS + ". See the tab 'Active Statements' for who blocks who.";
							alarmEvent.setExtendedDescription(what, what + "<br><br>" + cm.getGraphDataHistoryAsHtmlImage(GRAPH_NAME_BLOCKING_LOCKS));

							// Set CANCEL message (values that was found AFTER last raise/re-raise event)
							if (AlarmPhase.CANCEL.equals(phase))
							{
								Integer MAX_LOCK_WAIT_SEC = cm.getAbsValueAsInteger(0, "MAX_LOCK_WAIT_SEC");
								String cancelMsg = getAlarmCancelText("Applications waiting for a lock", LOCK_WAITERS + ", max wait " + (MAX_LOCK_WAIT_SEC == null ? 0 : MAX_LOCK_WAIT_SEC) + " seconds", " seconds wait");
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
					String dbname  = cm.getAbsString(0, "DATABASE_NAME");
					String tranDesc = "applHandle=" + cm.getAbsString(0, "OLDEST_OPEN_TRAN_APPL_HANDLE")
					                + ", user="     + cm.getAbsString(0, "OLDEST_OPEN_TRAN_USER")
					                + ", appl="     + cm.getAbsString(0, "OLDEST_OPEN_TRAN_APPL_NAME")
					                + ", host="     + cm.getAbsString(0, "OLDEST_OPEN_TRAN_HOST")
					                + ", state="    + cm.getAbsString(0, "OLDEST_OPEN_TRAN_STATE");

					AlarmEvent ae = new AlarmEventLongRunningTransaction(cm, threshold, dbname, oldestOpenTranInSec, tranDesc);

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
							           : "The oldest open transaction: applHandle=" + cm.getAbsString(0, "OLDEST_OPEN_TRAN_APPL_HANDLE")
							           + ", user='"       + cm.getAbsString(0, "OLDEST_OPEN_TRAN_USER") + "'"
							           + ", application='" + cm.getAbsString(0, "OLDEST_OPEN_TRAN_APPL_NAME") + "'"
							           + ", host='"       + cm.getAbsString(0, "OLDEST_OPEN_TRAN_HOST") + "'"
							           + ", state='"      + cm.getAbsString(0, "OLDEST_OPEN_TRAN_STATE") + "'"
							           + ", started at '" + cm.getAbsString(0, "OLDEST_OPEN_TRAN_TIME") + "'"
							           + ", log written " + cm.getAbsString(0, "OLDEST_OPEN_TRAN_LOG_MB") + " MB"
							           + ", open transactions: " + OPEN_TRAN_COUNT + "."
							           + " (state UOWWAIT = the application is not running anything: a forgotten commit?)";

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

		addAlarmSettings_DbmsVersionStringChanged(list, "VERSION");

		list.add(new CmSettingsHelper("BlockingLockCount",             isAlarmSwitch, PROPKEY_alarm_BlockingLockCount               , Integer.class, conf.getIntProperty   (PROPKEY_alarm_BlockingLockCount               , DEFAULT_alarm_BlockingLockCount               ), DEFAULT_alarm_BlockingLockCount               , "If 'LOCK_WAITERS' (number of applications that waits for a lock) is greater than ## then send 'AlarmEventBlockingLockAlarm'." ));
		list.add(new CmSettingsHelper("BlockingLockCount MinWaitTime",                PROPKEY_alarm_BlockingLockCount_minWaitTimeSec, Double .class, conf.getDoubleProperty(PROPKEY_alarm_BlockingLockCount_minWaitTimeSec, DEFAULT_alarm_BlockingLockCount_minWaitTimeSec), DEFAULT_alarm_BlockingLockCount_minWaitTimeSec, "If 'BlockingLockCount' is true; then 'MAX_LOCK_WAIT_SEC' (the application that has waited the longest) must also be greater than ## seconds." ));
		list.add(new CmSettingsHelper("oldestOpenTranInSec",           isAlarmSwitch, PROPKEY_alarm_oldestOpenTranInSec             , Integer.class, conf.getIntProperty   (PROPKEY_alarm_oldestOpenTranInSec             , DEFAULT_alarm_oldestOpenTranInSec             ), DEFAULT_alarm_oldestOpenTranInSec             , "If 'OLDEST_OPEN_TRAN_IN_SEC' is greater than ## seconds then send 'AlarmEventLongRunningTransaction'." ));

		return list;
	}

	/** What the columns are: tooltips in the GUI, and the 'Description' column in DbxCentral (Counter Details, the "unpivot" view). DB2 returns the column names in upper case */
	@Override
	public void initLocalToolTipTextOnTableColumnHeader()
	{
		setLocalToolTipTextOnTableColumnHeader("VERSION",                      "<html>The Db2 version (SERVICE_LEVEL). SYSIBMADM.ENV_INST_INFO</html>");
		setLocalToolTipTextOnTableColumnHeader("TIME_NOW",                     "<html>Time of the sample (CURRENT TIMESTAMP on the server).</html>");
		setLocalToolTipTextOnTableColumnHeader("UTC_MINUTE_DIFF",              "<html>UTC Time Difference in minutes (positive east of UK, negative west of UK). From CURRENT TIMEZONE.</html>");
		setLocalToolTipTextOnTableColumnHeader("HOST",                         "<html>The host that Db2 runs on. SYSPROC.ENV_GET_SYS_INFO()</html>");
		setLocalToolTipTextOnTableColumnHeader("INSTANCE_NAME",                "<html>The Db2 instance (INST_NAME). SYSIBMADM.ENV_INST_INFO</html>");
		setLocalToolTipTextOnTableColumnHeader("DATABASE_NAME",                "<html>The database that Db2Tune monitors (CURRENT SERVER).<br><b>Note</b>: Before 2026, this column had the instance name (it is now in INSTANCE_NAME).</html>");
		setLocalToolTipTextOnTableColumnHeader("START_TIME",                   "<html>When the Db2 instance was started (DB2START). MON_GET_INSTANCE.DB2START_TIME</html>");
		setLocalToolTipTextOnTableColumnHeader("DB_ACTIVATION_TIME",           "<html>When the database was activated (MON_GET_DATABASE.DB_CONN_TIME). The counters start from 0 at this time.</html>");
		setLocalToolTipTextOnTableColumnHeader("DB_STATUS",                    "<html>Status of the database: ACTIVE, QUIESCE_PEND, QUIESCED, ROLLFWD (roll forward), ACTIVE_STANDBY (HADR standby with reads), STANDBY...</html>");
		setLocalToolTipTextOnTableColumnHeader("DB_ACTIVATION_STATE",          "<html><b>EXPLICIT</b>: activated with ACTIVATE DATABASE (recommended).<br><b>NONE</b>: activated by the first connection, and deactivated when the last connection leaves (then all counters and the package cache are lost).</html>");
		setLocalToolTipTextOnTableColumnHeader("HADR_ROLE",                    "<html>HADR role of the database: STANDARD (no HADR), PRIMARY or STANDBY. Database configuration hadr_db_role</html>");
		setLocalToolTipTextOnTableColumnHeader("LAST_BACKUP",                  "<html>When the last backup of the database was taken (MON_GET_DATABASE.LAST_BACKUP).</html>");
		setLocalToolTipTextOnTableColumnHeader("APPLS_CUR_CONS",               "<html>Applications connected to the database right now.</html>");
		setLocalToolTipTextOnTableColumnHeader("APPLS_IN_DB2",                 "<html>Applications that are executing in Db2 right now (not waiting for the client).</html>");
		setLocalToolTipTextOnTableColumnHeader("CONNECTIONS_TOP",              "<html>The most connections at the same time since the database was activated.</html>");
		setLocalToolTipTextOnTableColumnHeader("TOTAL_CONS",                   "<html>Connects to the database.</html>");
		setLocalToolTipTextOnTableColumnHeader("TOTAL_APP_COMMITS",            "<html>Commits issued by the applications (also autocommit).</html>");
		setLocalToolTipTextOnTableColumnHeader("TOTAL_APP_ROLLBACKS",          "<html>Rollbacks issued by the applications.</html>");
		setLocalToolTipTextOnTableColumnHeader("ACT_COMPLETED_TOTAL",          "<html>Activities (statements, also the ones in routines) that completed successfully.</html>");
		setLocalToolTipTextOnTableColumnHeader("FAILED_SQL_STMTS",             "<html>Statements that failed (got a negative SQLCODE).</html>");
		setLocalToolTipTextOnTableColumnHeader("ROWS_READ",                    "<html>Rows read from tables (rows that had to be looked at, not only the ones returned).</html>");
		setLocalToolTipTextOnTableColumnHeader("ROWS_RETURNED",                "<html>Rows returned to the applications.</html>");
		setLocalToolTipTextOnTableColumnHeader("ROWS_MODIFIED",                "<html>Rows inserted, updated or deleted.</html>");
		setLocalToolTipTextOnTableColumnHeader("ROWS_READ_PER_RETURNED",       "<html>Rows read for each row returned. A high value means table scans, or indexes that do not match the queries.<br><b>Formula</b>: ROWS_READ / ROWS_RETURNED (Abs: since activation. Diff and Rate: in the sample)</html>");
		setLocalToolTipTextOnTableColumnHeader("BP_LOGICAL_READS",             "<html>Logical reads: pages read from the buffer pools (data + index + xda + column organized, also temporary).</html>");
		setLocalToolTipTextOnTableColumnHeader("BP_PHYSICAL_READS",            "<html>Physical reads: pages that were not in a buffer pool and had to be read from disk.</html>");
		setLocalToolTipTextOnTableColumnHeader("BP_HIT_PCT",                   "<html>Buffer pool hit ratio: logical reads that did not need a disk read.<br><b>Formula</b>: 100 - 100 * BP_PHYSICAL_READS / BP_LOGICAL_READS<br>Abs: since the database was activated. Diff and Rate: in the sample.</html>");
		setLocalToolTipTextOnTableColumnHeader("POOL_WRITES",                  "<html>Pages written from the buffer pools to disk.</html>");
		setLocalToolTipTextOnTableColumnHeader("NUM_LOCKS_HELD",               "<html>Locks held right now.</html>");
		setLocalToolTipTextOnTableColumnHeader("NUM_LOCKS_WAITING",            "<html>Agents that wait for a lock right now.</html>");
		setLocalToolTipTextOnTableColumnHeader("LOCK_WAITS",                   "<html>Times an application had to wait for a lock.<br><b>Note</b>: A wait is counted when it <b>ends</b> (tested): a wait that is going on right now is in LOCK_WAITERS / MAX_LOCK_WAIT_SEC.</html>");
		setLocalToolTipTextOnTableColumnHeader("LOCK_WAIT_TIME",               "<html>Time spent waiting for locks, in milliseconds. Added when the wait ends.</html>");
		setLocalToolTipTextOnTableColumnHeader("LOCK_TIMEOUTS",                "<html>Lock requests that timed out (SQL0911N reason 68). Never with LOCKTIMEOUT -1 (wait forever).</html>");
		setLocalToolTipTextOnTableColumnHeader("DEADLOCKS",                    "<html>Deadlocks (SQL0911N reason 2: one of the transactions was rolled back).</html>");
		setLocalToolTipTextOnTableColumnHeader("LOCK_ESCALS",                  "<html>Lock escalations: many row locks were replaced by a table lock (LOCKLIST or MAXLOCKS was reached). This can block other applications.</html>");
		setLocalToolTipTextOnTableColumnHeader("TOTAL_SORTS",                  "<html>Sorts.</html>");
		setLocalToolTipTextOnTableColumnHeader("SORT_OVERFLOWS",               "<html>Sorts that did not fit in the sort heap and were written to temporary space.</html>");
		setLocalToolTipTextOnTableColumnHeader("TOTAL_CPU_TIME",               "<html>CPU time used by the database agents (user + system), in <b>microseconds</b>.</html>");
		setLocalToolTipTextOnTableColumnHeader("TOTAL_RQST_TIME",              "<html>Time spent on requests from the applications, in milliseconds (processing + waiting).</html>");
		setLocalToolTipTextOnTableColumnHeader("TOTAL_WAIT_TIME",              "<html>Time spent waiting in Db2 (locks, I/O, log, network...), in milliseconds. The tab 'Time Spent' has the details.</html>");
		setLocalToolTipTextOnTableColumnHeader("WAIT_TIME_PCT",                "<html>Wait time in percent of the request time.<br><b>Formula</b>: 100 * TOTAL_WAIT_TIME / TOTAL_RQST_TIME (Abs: since activation. Diff and Rate: in the sample)</html>");
		setLocalToolTipTextOnTableColumnHeader("PKG_CACHE_LOOKUPS",            "<html>Lookups of a statement (section) in the package cache.</html>");
		setLocalToolTipTextOnTableColumnHeader("PKG_CACHE_INSERTS",            "<html>Statements (sections) that were not in the package cache, and had to be compiled and inserted.</html>");
		setLocalToolTipTextOnTableColumnHeader("PKG_CACHE_HIT_PCT",            "<html>Package cache hit ratio.<br><b>Formula</b>: 100 - 100 * PKG_CACHE_INSERTS / PKG_CACHE_LOOKUPS (Abs: since activation. Diff and Rate: in the sample)<br>A low value: statements are not reused (literals instead of parameter markers?), or the package cache is too small.</html>");
		setLocalToolTipTextOnTableColumnHeader("PKG_CACHE_NUM_OVERFLOWS",      "<html>Times the package cache was full and used memory beyond PCKCACHESZ.</html>");
		setLocalToolTipTextOnTableColumnHeader("LOG_USED_PCT",                 "<html>Transaction log used, in percent (the highest of the members). See the tab 'Transaction Log'. At 100% all changes fail with SQL0964C.</html>");
		setLocalToolTipTextOnTableColumnHeader("LOG_WRITE_MB",                 "<html>Transaction log written, in MB.</html>");
		setLocalToolTipTextOnTableColumnHeader("LOCK_WAITERS",                 "<html>Applications that wait for a lock right now. MON_GET_APPL_LOCKWAIT<br><b>Alarm</b>: BlockingLockCount</html>");
		setLocalToolTipTextOnTableColumnHeader("LOCK_BLOCKERS",                "<html>Applications that hold a lock that someone waits for right now.</html>");
		setLocalToolTipTextOnTableColumnHeader("MAX_LOCK_WAIT_SEC",            "<html>The longest time an application has waited for a lock right now, in seconds.</html>");
		setLocalToolTipTextOnTableColumnHeader("BLOCKING_WAIT_IN_SEC_TH",      "<html>The threshold (in seconds) of the alarm 'BlockingLockCount'.</html>");
		setLocalToolTipTextOnTableColumnHeader("OPEN_TRAN_COUNT",              "<html>Open transactions (units of work) right now, not counting Db2Tune's own. MON_GET_UNIT_OF_WORK, UOW_STOP_TIME is NULL</html>");
		setLocalToolTipTextOnTableColumnHeader("OLDEST_OPEN_TRAN_IN_SEC",      "<html>How long the oldest open transaction has been open, in seconds. 0 = no open transaction.<br><b>Alarm</b>: oldestOpenTranInSec</html>");
		setLocalToolTipTextOnTableColumnHeader("OLDEST_OPEN_TRAN_IN_SEC_TH",   "<html>The threshold (in seconds) of the alarm 'oldestOpenTranInSec'.</html>");
		setLocalToolTipTextOnTableColumnHeader("OLDEST_OPEN_TRAN_APPL_HANDLE", "<html>The application (handle) that has the oldest open transaction.</html>");
		setLocalToolTipTextOnTableColumnHeader("OLDEST_OPEN_TRAN_USER",        "<html>User of the application that has the oldest open transaction.</html>");
		setLocalToolTipTextOnTableColumnHeader("OLDEST_OPEN_TRAN_APPL_NAME",   "<html>Name of the application that has the oldest open transaction.</html>");
		setLocalToolTipTextOnTableColumnHeader("OLDEST_OPEN_TRAN_HOST",        "<html>Client host of the application that has the oldest open transaction.</html>");
		setLocalToolTipTextOnTableColumnHeader("OLDEST_OPEN_TRAN_STATE",       "<html>What the application with the oldest open transaction does: UOWEXEC (runs a statement), UOWWAIT (waits for the client: the transaction is open, but nothing runs).</html>");
		setLocalToolTipTextOnTableColumnHeader("OLDEST_OPEN_TRAN_TIME",        "<html>When the oldest open transaction started.</html>");
		setLocalToolTipTextOnTableColumnHeader("OLDEST_OPEN_TRAN_LOG_MB",      "<html>Log written by the oldest open transaction, in MB. Even when it is small, the log written AFTER it can not be reused until it ends.</html>");
	}
}
