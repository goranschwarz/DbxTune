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
import com.dbxtune.cm.CounterSetTemplates;
import com.dbxtune.cm.CounterSetTemplates.Type;
import com.dbxtune.cm.CountersModel;
import com.dbxtune.cm.oracle.gui.CmSummaryPanel;
import com.dbxtune.graph.TrendGraphDataPoint;
import com.dbxtune.graph.TrendGraphDataPoint.LabelType;
import com.dbxtune.sql.conn.DbxConnection;
import com.dbxtune.sql.conn.info.DbmsVersionInfo;
import com.dbxtune.utils.Configuration;
import com.dbxtune.utils.Ver;

/**
 * @author Goran Schwarz (goran_schwarz@hotmail.com)
 */
public class CmSummary
//extends CountersModel
extends CmSummaryAbstract
{
	private static final Logger _logger = LogManager.getLogger(MethodHandles.lookup().lookupClass());
	private static final long    serialVersionUID = 1L;

	public static final String   CM_NAME          = CmSummary.class.getSimpleName();
	public static final String   SHORT_NAME       = "Summary";
	public static final String   HTML_DESC        =
		"<html>" +
		"Overview of how the system performs." +
		"</html>";

	public static final String   GROUP_NAME       = null;
	public static final String   GUI_ICON_FILE    = "images/" + CM_NAME + ".png";

	public static final long     NEED_SRV_VERSION = 0;
	public static final long     NEED_CE_VERSION  = 0;

	public static final String[] MON_TABLES       = new String[] {};
	public static final String[] NEED_ROLES       = new String[] {};
	public static final String[] NEED_CONFIG      = new String[] {};

	public static final String[] PCT_COLUMNS      = new String[] {};
	public static final String[] DIFF_COLUMNS     = new String[] {
		// "gauges", the DIFF is how much it has changed since last sample (can be negative), shown in the Summary Panel
		"DISTINCT_LOGINS",
		"CONNECTED_USERS",
		"COUNT_BLOCKING_WAIT",
		"MAX_BLOCKING_WAIT_IN_SEC",

		// counters: from V$SYSSTAT
		"TRANSACTIONS",
		"ROLLBACKS",
		"USER_CALLS",
		"EXECUTIONS",
		"LOGICAL_READS",
		"PHYSICAL_READS",
		"PHYSICAL_WRITES",
		"REDO_SIZE",
		"PARSE_TOTAL",
		"PARSE_HARD",
		"LOGONS_CUM",
		"DEADLOCKS",

		// counters: from V$SYS_TIME_MODEL (microseconds)
		"DB_TIME",
		"DB_CPU",
		"BG_CPU_TIME",

		// counters: from V$OSSTAT (centiseconds, for all CPU's)
		"OS_BUSY_TIME",
		"OS_IDLE_TIME",
		"OS_USER_TIME",
		"OS_SYS_TIME",
		"OS_IOWAIT_TIME"
	};

	// false: since some DIFF columns are "gauges", where a negative diff is correct (fewer connections than last sample)
	//        For the counters (which are reset when the instance is restarted) the graphs treats a negative value as 0
	public static final boolean  NEGATIVE_DIFF_COUNTERS_TO_ZERO = false;
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
	public static final String GRAPH_NAME_AAS                = "AasGraph";
	public static final String GRAPH_NAME_HOST_CPU           = "HostCpuGraph";
	public static final String GRAPH_NAME_CALLS              = "CallsGraph";
	public static final String GRAPH_NAME_READS              = "ReadsGraph";
	public static final String GRAPH_NAME_REDO               = "RedoGraph";
	public static final String GRAPH_NAME_PARSE              = "ParseGraph";
	public static final String GRAPH_NAME_BLOCKING_LOCKS     = "BlockingLocksGraph";
	public static final String GRAPH_NAME_CONNECTION         = "ConnectionsGraph";   // String x=GetCounters.CM_GRAPH_NAME__SUMMARY__CONNECTION;
	public static final String GRAPH_NAME_OLDEST_TRAN_IN_SEC = "OldestTranInSecGraph";
	public static final String GRAPH_NAME_TRANSACTION        = "TransGraph";               // Transactions, Rollbacks

	private void addTrendGraphs()
	{
		addTrendGraph(GRAPH_NAME_AAS,
			"Average Active Sessions",    // Menu CheckBox text
			"Average Active Sessions: how many sessions are working (on CPU or waiting) (" + SHORT_NAME + ")",    // Label
			TrendGraphDataPoint.createGraphProps(TrendGraphDataPoint.Y_AXIS_SCALE_LABELS_NORMAL, CentralPersistReader.SampleType.MAX_OVER_SAMPLES),
			new String[] { "Active Sessions [DB time]", "On CPU [DB CPU]", "Waiting [DB time - DB CPU]", "Background CPU" },
			LabelType.Static,
			TrendGraphDataPoint.Category.CPU,
			false, // is Percent Graph
			true,  // visible at start
			0,     // graph is valid from Server Version. 0 = All Versions; >0 = Valid from this version and above
			-1);   // minimum height

		addTrendGraph(GRAPH_NAME_HOST_CPU,
			"Host CPU Usage in Percent",    // Menu CheckBox text
			"Host CPU Usage in Percent, for ALL processes on the host (" + SHORT_NAME + ")",    // Label
			TrendGraphDataPoint.createGraphProps(TrendGraphDataPoint.Y_AXIS_SCALE_LABELS_PERCENT, CentralPersistReader.SampleType.MAX_OVER_SAMPLES),
			new String[] { "User", "System", "IO Wait" },
			LabelType.Static,
			TrendGraphDataPoint.Category.CPU,
			true,  // is Percent Graph
			true,  // visible at start
			0,     // graph is valid from Server Version. 0 = All Versions; >0 = Valid from this version and above
			-1);   // minimum height

		addTrendGraph(GRAPH_NAME_CALLS,
			"User Calls and Executions per Second",    // Menu CheckBox text
			"User Calls and SQL Executions per Second (" + SHORT_NAME + ")",    // Label
			TrendGraphDataPoint.createGraphProps(TrendGraphDataPoint.Y_AXIS_SCALE_LABELS_PERSEC, CentralPersistReader.SampleType.MAX_OVER_SAMPLES),
			new String[] { "User Calls", "Executions" },
			LabelType.Static,
			TrendGraphDataPoint.Category.OPERATIONS,
			false, // is Percent Graph
			true,  // visible at start
			0,     // graph is valid from Server Version. 0 = All Versions; >0 = Valid from this version and above
			-1);   // minimum height

		addTrendGraph(GRAPH_NAME_TRANSACTION,
			"Transaction per second",    // Menu CheckBox text
			"Transaction per Second (" + SHORT_NAME + ")",    // Label
			TrendGraphDataPoint.createGraphProps(TrendGraphDataPoint.Y_AXIS_SCALE_LABELS_PERSEC, CentralPersistReader.SampleType.MAX_OVER_SAMPLES),
			null,
			LabelType.Dynamic,
			TrendGraphDataPoint.Category.OPERATIONS,
			false,   // is Percent Graph
			true,   // visible at start
			0, // graph is valid from Server Version. 0 = All Versions; >0 = Valid from this version and above
			-1);     // minimum height

		addTrendGraph(GRAPH_NAME_READS,
			"Logical and Physical IO per Second",    // Menu CheckBox text
			"Logical and Physical IO, in Blocks per Second (" + SHORT_NAME + ")",    // Label
			TrendGraphDataPoint.createGraphProps(TrendGraphDataPoint.Y_AXIS_SCALE_LABELS_PERSEC, CentralPersistReader.SampleType.MAX_OVER_SAMPLES),
			new String[] { "Logical Reads", "Physical Reads", "Physical Writes" },
			LabelType.Static,
			TrendGraphDataPoint.Category.DISK,
			false, // is Percent Graph
			true,  // visible at start
			0,     // graph is valid from Server Version. 0 = All Versions; >0 = Valid from this version and above
			-1);   // minimum height

		addTrendGraph(GRAPH_NAME_REDO,
			"Redo Generated in KB per Second",    // Menu CheckBox text
			"Redo Generated in KB per Second (" + SHORT_NAME + ")",    // Label
			TrendGraphDataPoint.createGraphProps(TrendGraphDataPoint.Y_AXIS_SCALE_LABELS_KB, CentralPersistReader.SampleType.MAX_OVER_SAMPLES),
			new String[] { "Redo KB" },
			LabelType.Static,
			TrendGraphDataPoint.Category.DISK,
			false, // is Percent Graph
			true,  // visible at start
			0,     // graph is valid from Server Version. 0 = All Versions; >0 = Valid from this version and above
			-1);   // minimum height

		addTrendGraph(GRAPH_NAME_PARSE,
			"Parses per Second",    // Menu CheckBox text
			"SQL Parses per Second (" + SHORT_NAME + ")",    // Label
			TrendGraphDataPoint.createGraphProps(TrendGraphDataPoint.Y_AXIS_SCALE_LABELS_PERSEC, CentralPersistReader.SampleType.MAX_OVER_SAMPLES),
			new String[] { "Total", "Hard" },
			LabelType.Static,
			TrendGraphDataPoint.Category.OPERATIONS,
			false, // is Percent Graph
			false, // visible at start
			0,     // graph is valid from Server Version. 0 = All Versions; >0 = Valid from this version and above
			-1);   // minimum height

		addTrendGraph(GRAPH_NAME_BLOCKING_LOCKS,
			"Blocking Locks", 	                     // Menu CheckBox text
			"Number of Concurrently Blocking Locks (" + SHORT_NAME + ")", // Label
			TrendGraphDataPoint.createGraphProps(TrendGraphDataPoint.Y_AXIS_SCALE_LABELS_NORMAL, CentralPersistReader.SampleType.MAX_OVER_SAMPLES),
			new String[] { "Blocking Locks" },
			LabelType.Static,
			TrendGraphDataPoint.Category.WAITS,
			false, // is Percent Graph
			true, // visible at start
			0,     // graph is valid from Server Version. 0 = All Versions; >0 = Valid from this version and above
			-1);   // minimum height

		addTrendGraph(GRAPH_NAME_CONNECTION,
			"Connections/Users", 	          // Menu CheckBox text
			"Connections/Users connected to Oracle (" + SHORT_NAME + ")", // Label
			TrendGraphDataPoint.createGraphProps(TrendGraphDataPoint.Y_AXIS_SCALE_LABELS_NORMAL, CentralPersistReader.SampleType.MAX_OVER_SAMPLES),
			new String[] { "UserConnections (abs)", "distinctLogins (abs)" },
			LabelType.Static,
			TrendGraphDataPoint.Category.OPERATIONS,
			false, // is Percent Graph
			true, // visible at start
			0,     // graph is valid from Server Version. 0 = All Versions; >0 = Valid from this version and above
			-1);   // minimum height

		addTrendGraph(GRAPH_NAME_OLDEST_TRAN_IN_SEC,
			"Oldest Open Transaction",     // Menu CheckBox text
			"Oldest Open Transaction, in Seconds (" + SHORT_NAME + ")", // Label
			TrendGraphDataPoint.createGraphProps(TrendGraphDataPoint.Y_AXIS_SCALE_LABELS_SECONDS, CentralPersistReader.SampleType.MAX_OVER_SAMPLES),
			new String[] { "Seconds" },
			LabelType.Static,
			TrendGraphDataPoint.Category.OPERATIONS,
			false, // is Percent Graph
			true, // visible at start
			0,     // graph is valid from Server Version. 0 = All Versions; >0 = Valid from this version and above
			-1);   // minimum height

	}

	@Override
	protected JPanel createGui()
	{
		setTabPanel(null); // Don't think this is necessary, but lets do it anyway...

		CmSummaryPanel summaryPanel = new CmSummaryPanel(this);

		// THIS IS THE SUMMARY CM, so set this
//		CounterController.getInstance().setSummaryPanel( summaryPanel );
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

	/** Used for example when the alarm 'SrvDown' is cancelled, to tell if the DBMS was restarted or not */
	@Override
	protected String getDbmsStartTimeColumnName()
	{
		return "STARTUP_TIME";
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

	@Override
	public String getSqlForVersion(DbxConnection conn, DbmsVersionInfo versionInfo)
	{
		Configuration conf = Configuration.getCombinedConfiguration();

		// The thresholds shown in the Summary Panel are the ones used by the alarms
		int oldestOpenTranInSecThreshold = conf.getIntProperty(PROPKEY_alarm_oldestOpenTranInSec, DEFAULT_alarm_oldestOpenTranInSec);
		int blockingWaitInSecThreshold   = (int) conf.getDoubleProperty(PROPKEY_alarm_BlockingLockCount_minWaitTimeSec, DEFAULT_alarm_BlockingLockCount_minWaitTimeSec);

		// 11.2 and above
		String VERSION  = "i.VERSION";
		String CDB      = "CAST('NO' as VARCHAR2(3))";
		String CON_NAME = "CAST(NULL as VARCHAR2(128))";

		String sql = getSql(VERSION, CDB, CON_NAME, oldestOpenTranInSecThreshold, blockingWaitInSecThreshold);

		// 12.1 and above: is it a Container Database, and what container are we connected to
		// 18   and above: VERSION_FULL also has the Release Update (19.21.0.0.0), while VERSION is always 19.0.0.0.0
		if (versionInfo.getLongVersion() >= Ver.ver(12,1))
		{
			CDB      = "d.CDB";
			CON_NAME = "CAST(SYS_CONTEXT('USERENV','CON_NAME') as VARCHAR2(128))";

			if (versionInfo.getLongVersion() >= Ver.ver(18))
				VERSION = "i.VERSION_FULL";

			String sqlNewer = getSql(VERSION, CDB, CON_NAME, oldestOpenTranInSecThreshold, blockingWaitInSecThreshold);

			// The SQL for 12.1 and above can NOT be tested on all versions: so "dry-run" it, and if it fails use the 11.2 SQL (which works on all versions)
			// conn is null when we just want to "view" the SQL (the CM Properties dialog)
			if (conn == null)
			{
				sql = sqlNewer;
			}
			else
			{
				try (Statement stmnt = conn.createStatement(); ResultSet rs = stmnt.executeQuery("select * from (" + sqlNewer + ") where 1=2"))
				{
					sql = sqlNewer;
				}
				catch (SQLException ex)
				{
					_logger.warn(CM_NAME + ": The SQL for version " + Ver.versionNumToStr(versionInfo.getLongVersion()) + " failed, using the SQL for 11.2 instead. Caught: " + ex.getMessage().trim());
				}
			}
		}

		return sql;
	}

	/** One row. Every "sub select" (ss, tx, st, tm, os) returns exactly one row, so the CROSS JOIN's is just a way to put them next to each other */
	private String getSql(String VERSION, String CDB, String CON_NAME, int oldestOpenTranInSecThreshold, int blockingWaitInSecThreshold)
	{
		return ""
			+ "select \n"
			+ "     i.INSTANCE_NAME \n"
			+ "    ,i.HOST_NAME \n"
			+ "    ," + VERSION + " as VERSION \n"
			+ "    ,i.STARTUP_TIME \n"
			+ "    ,CAST(SYSDATE - i.STARTUP_TIME as INT)                                          as DAYS_RUNNING \n"
			+ "    ,SYSDATE                                                                        as TIME_NOW \n"
			+ "    ,CAST(SYS_EXTRACT_UTC(SYSTIMESTAMP) as DATE)                                    as UTC_DATE \n"
			+ "    ,CAST((24*60) * (SYSDATE - CAST(SYS_EXTRACT_UTC(SYSTIMESTAMP) as DATE)) as INT) as UTC_MINUTE_DIFF \n"
			+ "    ,d.NAME                                                                         as DB_NAME \n"
			+ "    ,d.DATABASE_ROLE \n"
			+ "    ,d.OPEN_MODE \n"
			+ "    ,d.LOG_MODE \n"
			+ "    ," + CDB      + " as CDB \n"
			+ "    ," + CON_NAME + " as CON_NAME \n"

			+ "    ,ss.DISTINCT_LOGINS \n"
			+ "    ,ss.CONNECTED_USERS \n"
			+ "    ,ss.ACTIVE_USERS \n"

			+ "    ,tx.OLDEST_OPEN_TRAN_IN_SEC \n"
			+ "    ,tx.OLDEST_OPEN_TRAN_TIME \n"
			+ "    ,CAST(" + oldestOpenTranInSecThreshold + " as INT)                              as OLDEST_OPEN_TRAN_IN_SEC_TH \n" // TH = THRESHOLD
			+ "    ,tx.OLDEST_OPEN_TRAN_SID \n"
			+ "    ,tx.OLDEST_OPEN_TRAN_USERNAME \n"
			+ "    ,tx.OPEN_TRAN_COUNT \n"

			+ "    ,ss.MAX_BLOCKING_WAIT_IN_SEC \n"
			+ "    ,ss.COUNT_BLOCKING_WAIT \n"
			+ "    ,CAST(" + blockingWaitInSecThreshold + " as INT)                                as BLOCKING_WAIT_IN_SEC_TH \n"  // TH = THRESHOLD

			+ "    ,st.TRANSACTIONS \n"
			+ "    ,st.ROLLBACKS \n"
			+ "    ,st.USER_CALLS \n"
			+ "    ,st.EXECUTIONS \n"
			+ "    ,st.LOGICAL_READS \n"
			+ "    ,st.PHYSICAL_READS \n"
			+ "    ,st.PHYSICAL_WRITES \n"
			+ "    ,st.REDO_SIZE \n"
			+ "    ,st.PARSE_TOTAL \n"
			+ "    ,st.PARSE_HARD \n"
			+ "    ,st.LOGONS_CUM \n"
			+ "    ,st.DEADLOCKS \n"

			+ "    ,tm.DB_TIME \n"
			+ "    ,tm.DB_CPU \n"
			+ "    ,tm.BG_CPU_TIME \n"

			+ "    ,os.NUM_CPUS \n"
			+ "    ,os.OS_BUSY_TIME \n"
			+ "    ,os.OS_IDLE_TIME \n"
			+ "    ,os.OS_USER_TIME \n"
			+ "    ,os.OS_SYS_TIME \n"
			+ "    ,os.OS_IOWAIT_TIME \n"
			+ "    ,os.OS_LOAD \n"
			+ "from V$INSTANCE i \n"
			+ "cross join V$DATABASE d \n"

			// Sessions: USERNAME is null for background processes
			+ "cross join (select count(distinct USERNAME)                                                     as DISTINCT_LOGINS \n"
			+ "                  ,count(USERNAME)                                                              as CONNECTED_USERS \n"
			+ "                  ,sum(case when TYPE = 'USER' and STATUS = 'ACTIVE' then 1 else 0 end)         as ACTIVE_USERS \n"
			+ "                  ,sum(case when BLOCKING_SESSION is not null then 1 else 0 end)                as COUNT_BLOCKING_WAIT \n"
			+ "                  ,max(case when BLOCKING_SESSION is not null then TRUNC(WAIT_TIME_MICRO / 1000000) end) as MAX_BLOCKING_WAIT_IN_SEC \n"
			+ "            from V$SESSION) ss \n"

			// Open transactions: and who has the oldest one
			+ "cross join (select CAST(86400 * (SYSDATE - min(t.START_DATE)) as INT)                           as OLDEST_OPEN_TRAN_IN_SEC \n"
			+ "                  ,min(t.START_DATE)                                                            as OLDEST_OPEN_TRAN_TIME \n"
			+ "                  ,min(s.SID)      keep (dense_rank first order by t.START_DATE)                as OLDEST_OPEN_TRAN_SID \n"
			+ "                  ,min(s.USERNAME) keep (dense_rank first order by t.START_DATE)                as OLDEST_OPEN_TRAN_USERNAME \n"
			+ "                  ,count(*)                                                                     as OPEN_TRAN_COUNT \n"
			+ "            from V$TRANSACTION t \n"
			+ "            left join V$SESSION s on s.SADDR = t.SES_ADDR) tx \n"

			// System Statistics: counters since the instance was started
			+ "cross join (select max(decode(NAME, 'user commits'         , VALUE)) as TRANSACTIONS \n"
			+ "                  ,max(decode(NAME, 'user rollbacks'       , VALUE)) as ROLLBACKS \n"
			+ "                  ,max(decode(NAME, 'user calls'           , VALUE)) as USER_CALLS \n"
			+ "                  ,max(decode(NAME, 'execute count'        , VALUE)) as EXECUTIONS \n"
			+ "                  ,max(decode(NAME, 'session logical reads', VALUE)) as LOGICAL_READS \n"
			+ "                  ,max(decode(NAME, 'physical reads'       , VALUE)) as PHYSICAL_READS \n"
			+ "                  ,max(decode(NAME, 'physical writes'      , VALUE)) as PHYSICAL_WRITES \n"
			+ "                  ,max(decode(NAME, 'redo size'            , VALUE)) as REDO_SIZE \n"
			+ "                  ,max(decode(NAME, 'parse count (total)'  , VALUE)) as PARSE_TOTAL \n"
			+ "                  ,max(decode(NAME, 'parse count (hard)'   , VALUE)) as PARSE_HARD \n"
			+ "                  ,max(decode(NAME, 'logons cumulative'    , VALUE)) as LOGONS_CUM \n"
			+ "                  ,max(decode(NAME, 'enqueue deadlocks'    , VALUE)) as DEADLOCKS \n"
			+ "            from V$SYSSTAT) st \n"

			// Time Model: microseconds since the instance was started
			+ "cross join (select max(decode(STAT_NAME, 'DB time'            , VALUE)) as DB_TIME \n"
			+ "                  ,max(decode(STAT_NAME, 'DB CPU'             , VALUE)) as DB_CPU \n"
			+ "                  ,max(decode(STAT_NAME, 'background cpu time', VALUE)) as BG_CPU_TIME \n"
			+ "            from V$SYS_TIME_MODEL) tm \n"

			// Operating System: CPU time is in centiseconds, for all CPU's. (some of the rows are not available on all platforms, then the value is NULL)
			+ "cross join (select max(decode(STAT_NAME, 'NUM_CPUS'   , VALUE)) as NUM_CPUS \n"
			+ "                  ,max(decode(STAT_NAME, 'BUSY_TIME'  , VALUE)) as OS_BUSY_TIME \n"
			+ "                  ,max(decode(STAT_NAME, 'IDLE_TIME'  , VALUE)) as OS_IDLE_TIME \n"
			+ "                  ,max(decode(STAT_NAME, 'USER_TIME'  , VALUE)) as OS_USER_TIME \n"
			+ "                  ,max(decode(STAT_NAME, 'SYS_TIME'   , VALUE)) as OS_SYS_TIME \n"
			+ "                  ,max(decode(STAT_NAME, 'IOWAIT_TIME', VALUE)) as OS_IOWAIT_TIME \n"
			+ "                  ,CAST(max(decode(STAT_NAME, 'LOAD'  , VALUE)) as NUMBER(10,2)) as OS_LOAD \n"
			+ "            from V$OSSTAT) os \n"
			+ "";
	}

	/**
	 * RATE (per second) for a counter column, in the one and only row.
	 *
	 * @return 0.0 if there is no value, or if it is negative (the instance was restarted, so the counters started from 0 again)
	 */
	private double getCounterRate(String colName)
	{
		Double val = this.getRateValueAsDouble(0, colName);

		return (val == null || val < 0) ? 0.0 : val;
	}

	/** Same as getCounterRate() but the DIFF value */
	private double getCounterDiff(String colName)
	{
		Double val = this.getDiffValueAsDouble(0, colName);

		return (val == null || val < 0) ? 0.0 : val;
	}

	@Override
	public void updateGraphData(TrendGraphDataPoint tgdp)
	{
		//---------------------------------
		// GRAPH: Average Active Sessions
		// DB_TIME and DB_CPU is microseconds, so: microseconds per second / 1 000 000 = "seconds per second" = how many sessions was active
		//---------------------------------
		if (GRAPH_NAME_AAS.equals(tgdp.getName()))
		{
			Double[] arr = new Double[4];

			double dbTime = getCounterRate("DB_TIME")     / 1_000_000.0;
			double dbCpu  = getCounterRate("DB_CPU")      / 1_000_000.0;
			double bgCpu  = getCounterRate("BG_CPU_TIME") / 1_000_000.0;

			arr[0] = dbTime;
			arr[1] = dbCpu;
			arr[2] = Math.max(0.0, dbTime - dbCpu);
			arr[3] = bgCpu;

			// Set the values
			tgdp.setDataPoint(this.getTimestamp(), arr);
		}

		//---------------------------------
		// GRAPH: Host CPU
		// BUSY + IDLE is all the CPU time there was in the sample (for all CPU's). Note: on Linux IOWAIT is a part of IDLE
		//---------------------------------
		if (GRAPH_NAME_HOST_CPU.equals(tgdp.getName()))
		{
			Double[] arr = new Double[3];

			double total = getCounterDiff("OS_BUSY_TIME") + getCounterDiff("OS_IDLE_TIME");

			arr[0] = (total <= 0) ? 0.0 : getCounterDiff("OS_USER_TIME")   / total * 100.0;
			arr[1] = (total <= 0) ? 0.0 : getCounterDiff("OS_SYS_TIME")    / total * 100.0;
			arr[2] = (total <= 0) ? 0.0 : getCounterDiff("OS_IOWAIT_TIME") / total * 100.0;

			// Set the values
			tgdp.setDataPoint(this.getTimestamp(), arr);
		}

		//---------------------------------
		// GRAPH: Calls
		//---------------------------------
		if (GRAPH_NAME_CALLS.equals(tgdp.getName()))
		{
			Double[] arr = new Double[2];

			arr[0] = getCounterRate("USER_CALLS");
			arr[1] = getCounterRate("EXECUTIONS");

			// Set the values
			tgdp.setDataPoint(this.getTimestamp(), arr);
		}

		//---------------------------------
		// GRAPH: Transactions
		//---------------------------------
		if (GRAPH_NAME_TRANSACTION.equals(tgdp.getName()))
		{
			Double[] dArray = new Double[2];
			String[] lArray = new String[] { "user commits", "user rollbacks" };

			dArray[0] = getCounterRate("TRANSACTIONS");
			dArray[1] = getCounterRate("ROLLBACKS");

			// Set the values
			tgdp.setDataPoint(this.getTimestamp(), lArray, dArray);
		}

		//---------------------------------
		// GRAPH: Reads
		//---------------------------------
		if (GRAPH_NAME_READS.equals(tgdp.getName()))
		{
			Double[] arr = new Double[3];

			arr[0] = getCounterRate("LOGICAL_READS");
			arr[1] = getCounterRate("PHYSICAL_READS");
			arr[2] = getCounterRate("PHYSICAL_WRITES");

			// Set the values
			tgdp.setDataPoint(this.getTimestamp(), arr);
		}

		//---------------------------------
		// GRAPH: Redo (REDO_SIZE is in bytes)
		//---------------------------------
		if (GRAPH_NAME_REDO.equals(tgdp.getName()))
		{
			Double[] arr = new Double[1];

			arr[0] = getCounterRate("REDO_SIZE") / 1024.0;

			// Set the values
			tgdp.setDataPoint(this.getTimestamp(), arr);
		}

		//---------------------------------
		// GRAPH: Parse
		//---------------------------------
		if (GRAPH_NAME_PARSE.equals(tgdp.getName()))
		{
			Double[] arr = new Double[2];

			arr[0] = getCounterRate("PARSE_TOTAL");
			arr[1] = getCounterRate("PARSE_HARD");

			// Set the values
			tgdp.setDataPoint(this.getTimestamp(), arr);
		}

		//---------------------------------
		// GRAPH: Blocking Locks
		//---------------------------------
		if (GRAPH_NAME_BLOCKING_LOCKS.equals(tgdp.getName()))
		{
			Double[] arr = new Double[1];

			arr[0] = this.getAbsValueAsDouble (0, "COUNT_BLOCKING_WAIT");
			_logger.debug("updateGraphData(" + tgdp.getName() + "): LockWait='" + arr[0] + "'.");

			// Set the values
			tgdp.setDataPoint(this.getTimestamp(), arr);
		}

		//---------------------------------
		// GRAPH: Connections
		//---------------------------------
		if (GRAPH_NAME_CONNECTION.equals(tgdp.getName()))
		{
			Double[] arr = new Double[2];

			arr[0] = this.getAbsValueAsDouble (0, "CONNECTED_USERS");
			arr[1] = this.getAbsValueAsDouble (0, "DISTINCT_LOGINS");
			_logger.debug("updateGraphData(" + tgdp.getName() + "): Connections(Abs)='" + arr[0] + "', distinctLogins(Abs)='" + arr[1] + "'.");

			// Set the values
			tgdp.setDataPoint(this.getTimestamp(), arr);
		}

		//---------------------------------
		// GRAPH: Oldest Open Transaction
		//---------------------------------
		if (GRAPH_NAME_OLDEST_TRAN_IN_SEC.equals(tgdp.getName()))
		{
			Double[] arr = new Double[1];

			// 0 (or NULL) when there are no open transactions
			Double oldestOpenTranInSec = this.getAbsValueAsDouble(0, "OLDEST_OPEN_TRAN_IN_SEC");

			arr[0] = (oldestOpenTranInSec == null) ? 0.0 : oldestOpenTranInSec;
			_logger.debug("updateGraphData(" + tgdp.getName() + "): oldestOpenTranInSec='" + arr[0] + "'.");

			// Set the values
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
			Integer blockingCount   = cm.getAbsValueAsInteger(0, "COUNT_BLOCKING_WAIT");
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
							alarmEvent.setExtendedDescription("", cm.getGraphDataHistoryAsHtmlImage(GRAPH_NAME_BLOCKING_LOCKS));

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
					// In a Container Database: the container we are connected to (CDB$ROOT or a PDB), otherwise the name of the database
					String conName = cm.getAbsString(0, "CON_NAME");
					String dbname  = (conName == null || conName.isEmpty()) ? cm.getAbsString(0, "DB_NAME") : conName;

					AlarmEvent ae = new AlarmEventLongRunningTransaction(cm, threshold, dbname, oldestOpenTranInSec, "-unknown-");

					// The below is called from AlarmHandler on: Raise, RE-RAISE & CANCEL
					ae.setAlarmDescriptionProvider(this, new AlarmDescriptionProviderBase(null, null, threshold)
					{
						@Override
						public void setValues(CountersModel cm, AlarmEvent alarmEvent, AlarmPhase phase)
						{
							// Always: (RAISE, RE-RAISE, CANCEL) - Who has the oldest transaction right now + graph values
							// Note: when there are no open transactions, the OLDEST_OPEN_TRAN_* columns are 0 (or empty), so check OPEN_TRAN_COUNT
							Integer OPEN_TRAN_COUNT = cm.getAbsValueAsInteger(0, "OPEN_TRAN_COUNT");
							boolean hasOpenTran     = OPEN_TRAN_COUNT != null && OPEN_TRAN_COUNT > 0;

							String who = ! hasOpenTran ? "There are no open transactions."
							           : "The oldest open transaction: SID=" + cm.getAbsString(0, "OLDEST_OPEN_TRAN_SID")
							           + ", USERNAME='"   + cm.getAbsString(0, "OLDEST_OPEN_TRAN_USERNAME") + "'"
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

		addAlarmSettings_DbmsVersionStringChanged(list, "VERSION");

		list.add(new CmSettingsHelper("BlockingLockCount",             isAlarmSwitch, PROPKEY_alarm_BlockingLockCount               , Integer.class, conf.getIntProperty   (PROPKEY_alarm_BlockingLockCount               , DEFAULT_alarm_BlockingLockCount               ), DEFAULT_alarm_BlockingLockCount               , "If 'COUNT_BLOCKING_WAIT' (number of sessions that are blocked by another session) is greater than ## then send 'AlarmEventBlockingLockAlarm'." ));
		list.add(new CmSettingsHelper("BlockingLockCount MinWaitTime",                PROPKEY_alarm_BlockingLockCount_minWaitTimeSec, Double .class, conf.getDoubleProperty(PROPKEY_alarm_BlockingLockCount_minWaitTimeSec, DEFAULT_alarm_BlockingLockCount_minWaitTimeSec), DEFAULT_alarm_BlockingLockCount_minWaitTimeSec, "If 'BlockingLockCount' is true; then 'MAX_BLOCKING_WAIT_IN_SEC' (the session that has been blocked the longest) must also be greater than ## seconds." ));
		list.add(new CmSettingsHelper("oldestOpenTranInSec",           isAlarmSwitch, PROPKEY_alarm_oldestOpenTranInSec             , Integer.class, conf.getIntProperty   (PROPKEY_alarm_oldestOpenTranInSec             , DEFAULT_alarm_oldestOpenTranInSec             ), DEFAULT_alarm_oldestOpenTranInSec             , "If 'OLDEST_OPEN_TRAN_IN_SEC' is greater than ## seconds then send 'AlarmEventLongRunningTransaction'." ));

		return list;
	}

	/** What the columns are: tooltips in the GUI, and the 'Description' column in DbxCentral (Counter Details, the "unpivot" view) */
	@Override
	public void initLocalToolTipTextOnTableColumnHeader()
	{
		setLocalToolTipTextOnTableColumnHeader("INSTANCE_NAME",              "<html>Name of the instance. V$INSTANCE</html>");
		setLocalToolTipTextOnTableColumnHeader("HOST_NAME",                  "<html>The host that the instance runs on. V$INSTANCE</html>");
		setLocalToolTipTextOnTableColumnHeader("VERSION",                    "<html>Oracle version. From 18c with the Release Update (VERSION_FULL). V$INSTANCE</html>");
		setLocalToolTipTextOnTableColumnHeader("STARTUP_TIME",               "<html>When the instance was started. V$INSTANCE</html>");
		setLocalToolTipTextOnTableColumnHeader("DAYS_RUNNING",               "<html>Days since the instance was started.</html>");
		setLocalToolTipTextOnTableColumnHeader("TIME_NOW",                   "<html>The time on the database server (SYSDATE) when the sample was taken.</html>");
		setLocalToolTipTextOnTableColumnHeader("UTC_DATE",                   "<html>The same time in UTC.</html>");
		setLocalToolTipTextOnTableColumnHeader("UTC_MINUTE_DIFF",            "<html>Minutes between the time of the database server and UTC.</html>");
		setLocalToolTipTextOnTableColumnHeader("DB_NAME",                    "<html>Name of the database. V$DATABASE</html>");
		setLocalToolTipTextOnTableColumnHeader("DATABASE_ROLE",              "<html>PRIMARY, or a kind of standby database (PHYSICAL STANDBY, LOGICAL STANDBY, SNAPSHOT STANDBY). V$DATABASE</html>");
		setLocalToolTipTextOnTableColumnHeader("OPEN_MODE",                  "<html>How the database is open: READ WRITE, READ ONLY, READ ONLY WITH APPLY, MOUNTED. V$DATABASE</html>");
		setLocalToolTipTextOnTableColumnHeader("LOG_MODE",                   "<html>ARCHIVELOG or NOARCHIVELOG. In NOARCHIVELOG mode the database can only be restored to the time of the last backup. V$DATABASE</html>");
		setLocalToolTipTextOnTableColumnHeader("CDB",                        "<html>YES if it is a Container Database (12.1 and above).</html>");
		setLocalToolTipTextOnTableColumnHeader("CON_NAME",                   "<html>The container we are connected to: CDB$ROOT or a Pluggable Database (PDB). Empty when it is not a Container Database.</html>");

		setLocalToolTipTextOnTableColumnHeader("DISTINCT_LOGINS",            "<html>Number of different USERNAME's that are connected. V$SESSION</html>");
		setLocalToolTipTextOnTableColumnHeader("CONNECTED_USERS",            "<html>Number of user sessions (the background processes are not counted). V$SESSION</html>");
		setLocalToolTipTextOnTableColumnHeader("ACTIVE_USERS",               "<html>Number of user sessions that are doing something right now (STATUS = ACTIVE). The session of the collector is one of them. V$SESSION</html>");

		setLocalToolTipTextOnTableColumnHeader("OLDEST_OPEN_TRAN_IN_SEC",    "<html>How long the oldest open transaction has been open, in seconds. V$TRANSACTION</html>");
		setLocalToolTipTextOnTableColumnHeader("OLDEST_OPEN_TRAN_TIME",      "<html>When the oldest open transaction started.</html>");
		setLocalToolTipTextOnTableColumnHeader("OLDEST_OPEN_TRAN_IN_SEC_TH", "<html>The threshold (in seconds) of the alarm 'oldestOpenTranInSec'.</html>");
		setLocalToolTipTextOnTableColumnHeader("OLDEST_OPEN_TRAN_SID",       "<html>The session (SID) that has the oldest open transaction.</html>");
		setLocalToolTipTextOnTableColumnHeader("OLDEST_OPEN_TRAN_USERNAME",  "<html>The USERNAME that has the oldest open transaction.</html>");
		setLocalToolTipTextOnTableColumnHeader("OPEN_TRAN_COUNT",            "<html>Number of open transactions. V$TRANSACTION</html>");

		setLocalToolTipTextOnTableColumnHeader("MAX_BLOCKING_WAIT_IN_SEC",   "<html>The session that has been blocked the longest: how many seconds it has waited. V$SESSION</html>");
		setLocalToolTipTextOnTableColumnHeader("COUNT_BLOCKING_WAIT",        "<html>Number of sessions that are blocked by another session (they wait for a lock). V$SESSION</html>");
		setLocalToolTipTextOnTableColumnHeader("BLOCKING_WAIT_IN_SEC_TH",    "<html>The threshold (in seconds) of the alarm 'BlockingLockCount'.</html>");

		setLocalToolTipTextOnTableColumnHeader("TRANSACTIONS",               "<html>Number of commits. V$SYSSTAT: user commits</html>");
		setLocalToolTipTextOnTableColumnHeader("ROLLBACKS",                  "<html>Number of rollbacks. V$SYSSTAT: user rollbacks</html>");
		setLocalToolTipTextOnTableColumnHeader("USER_CALLS",                 "<html>Number of calls from the applications: logon, parse, execute, fetch... V$SYSSTAT: user calls</html>");
		setLocalToolTipTextOnTableColumnHeader("EXECUTIONS",                 "<html>Number of SQL statements executed, also the recursive ones. V$SYSSTAT: execute count</html>");
		setLocalToolTipTextOnTableColumnHeader("LOGICAL_READS",              "<html>Number of blocks read from the buffer cache. V$SYSSTAT: session logical reads</html>");
		setLocalToolTipTextOnTableColumnHeader("PHYSICAL_READS",             "<html>Number of blocks read from disk. V$SYSSTAT: physical reads</html>");
		setLocalToolTipTextOnTableColumnHeader("PHYSICAL_WRITES",            "<html>Number of blocks written to disk. V$SYSSTAT: physical writes</html>");
		setLocalToolTipTextOnTableColumnHeader("REDO_SIZE",                  "<html>Redo generated, in bytes. V$SYSSTAT: redo size</html>");
		setLocalToolTipTextOnTableColumnHeader("PARSE_TOTAL",                "<html>Number of parse calls, hard and soft. V$SYSSTAT: parse count (total)</html>");
		setLocalToolTipTextOnTableColumnHeader("PARSE_HARD",                 "<html>Number of hard parses: the statement was not in the shared pool, so it had to be optimized. Many of them = the applications does not use bind variables. V$SYSSTAT: parse count (hard)</html>");
		setLocalToolTipTextOnTableColumnHeader("LOGONS_CUM",                 "<html>Number of logons. A high rate = the applications does not use a connection pool. V$SYSSTAT: logons cumulative</html>");
		setLocalToolTipTextOnTableColumnHeader("DEADLOCKS",                  "<html>Number of deadlocks between sessions (ORA-00060). V$SYSSTAT: enqueue deadlocks</html>");

		setLocalToolTipTextOnTableColumnHeader("DB_TIME",                    "<html>Time the user sessions have spent working (on CPU or waiting), in microseconds. The rate / 1 000 000 = Average Active Sessions. V$SYS_TIME_MODEL: DB time</html>");
		setLocalToolTipTextOnTableColumnHeader("DB_CPU",                     "<html>CPU time used by the user sessions, in microseconds. The rate / 1 000 000 = sessions on CPU. V$SYS_TIME_MODEL: DB CPU</html>");
		setLocalToolTipTextOnTableColumnHeader("BG_CPU_TIME",                "<html>CPU time used by the background processes, in microseconds. V$SYS_TIME_MODEL: background cpu time</html>");

		setLocalToolTipTextOnTableColumnHeader("NUM_CPUS",                   "<html>Number of CPU's (or CPU threads) on the host, as Oracle sees it. V$OSSTAT</html>");
		setLocalToolTipTextOnTableColumnHeader("OS_BUSY_TIME",               "<html>Time the CPU's of the host have been busy, for ALL processes, in centiseconds. V$OSSTAT: BUSY_TIME</html>");
		setLocalToolTipTextOnTableColumnHeader("OS_IDLE_TIME",               "<html>Time the CPU's of the host have been idle, in centiseconds. BUSY + IDLE = all the CPU time there was. V$OSSTAT: IDLE_TIME</html>");
		setLocalToolTipTextOnTableColumnHeader("OS_USER_TIME",               "<html>CPU time of the host in user mode, in centiseconds. V$OSSTAT: USER_TIME</html>");
		setLocalToolTipTextOnTableColumnHeader("OS_SYS_TIME",                "<html>CPU time of the host in system (kernel) mode, in centiseconds. V$OSSTAT: SYS_TIME</html>");
		setLocalToolTipTextOnTableColumnHeader("OS_IOWAIT_TIME",             "<html>Time the CPU's of the host have waited for IO, in centiseconds (not available on all platforms). V$OSSTAT: IOWAIT_TIME</html>");
		setLocalToolTipTextOnTableColumnHeader("OS_LOAD",                    "<html>The load average of the host: processes that are running or waiting to run (not available on all platforms). V$OSSTAT: LOAD</html>");
	}
}
