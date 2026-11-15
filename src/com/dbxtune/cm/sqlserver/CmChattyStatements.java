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
package com.dbxtune.cm.sqlserver;

import java.lang.invoke.MethodHandles;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.sql.Types;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import javax.naming.NameNotFoundException;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.h2.tools.SimpleResultSet;

import com.dbxtune.ICounterController;
import com.dbxtune.IGuiController;
import com.dbxtune.cm.CmSettingsHelper;
import com.dbxtune.cm.CounterSample;
import com.dbxtune.cm.CounterSetTemplates;
import com.dbxtune.cm.CounterSetTemplates.Type;
import com.dbxtune.cm.CountersModel;
import com.dbxtune.cm.NoValidRowsInSample;
import com.dbxtune.config.dict.MonTablesDictionary;
import com.dbxtune.config.dict.MonTablesDictionaryManager;
import com.dbxtune.gui.MainFrame;
import com.dbxtune.gui.ResultSetTableModel;
import com.dbxtune.pcs.PcsColumnOptions;
import com.dbxtune.pcs.PcsColumnOptions.ColumnType;
import com.dbxtune.sql.conn.DbxConnection;
import com.dbxtune.sql.conn.info.DbmsVersionInfo;
import com.dbxtune.sql.conn.info.DbmsVersionInfoSqlServer;
import com.dbxtune.utils.Configuration;
import com.dbxtune.utils.StringUtil;

/**
 * Cheap statements that are executed very often ("chatty" statements), the ones that {@link CmExecQueryStats} filters away
 * (it only keeps <code>last_logical_reads &gt; 100</code>).
 * <p>
 * Used to find <b>N+1 queries</b>: one parent statement returns N rows, and the application then executes one cheap
 * child lookup per row (1 row per call, sub millisecond, but N round trips).
 * <p>
 * How it works:
 * <ul>
 *   <li>Reads only key and counter columns from <code>dm_exec_query_stats</code> for cheap statements (no SQL text, no plan attributes).</li>
 *   <li>A full read at start and every ## minutes, in between only statements that completed since the previous sample.</li>
 *   <li>Keeps the previous counters per plan in memory, and calculates what happened <b>in this sample</b> (so bursts on plans
 *       that have been cached for days are caught).</li>
 *   <li>Groups the plans by <code>query_hash</code> (so literal SQL is folded together), and keeps only the top ## by
 *       executions and the top ## by rows in this sample.</li>
 *   <li>SQL text and database name are fetched only for the kept rows (and cached per query_hash).</li>
 * </ul>
 * The values in the rows are already "per sample", so there are no diff columns (only the ABS table is saved).
 */
public class CmChattyStatements
extends CountersModel
{
	private static final Logger  _logger          = LogManager.getLogger(MethodHandles.lookup().lookupClass());
	private static final long    serialVersionUID = 1L;

	public static final String   CM_NAME          = CmChattyStatements.class.getSimpleName();
	public static final String   SHORT_NAME       = "Chatty Stmnts";
	public static final String   HTML_DESC        =
		"<html>" +
		"<p>Cheap statements that are executed very often (the ones the 'Query Stat' tab filters away).</p>" +
		"<p>Values are for <b>this sample only</b>, grouped by <code>query_hash</code>, top ## by executions and top ## by rows.</p>" +
		"<p>Used to find <b>N+1 queries</b>: a child lookup with about 1 row per execution, executed once for every row of a parent statement.</p>" +
		"</html>";

	public static final String   GROUP_NAME       = MainFrame.TCP_GROUP_OBJECT_ACCESS;
	public static final String   GUI_ICON_FILE    = "images/CmExecQueryStats.png";

	public static final long     NEED_SRV_VERSION = 0;
	public static final long     NEED_CE_VERSION  = 0;

	public static final String[] MON_TABLES       = new String[] {"dm_exec_query_stats"};
	public static final String[] NEED_ROLES       = new String[] {"VIEW SERVER STATE"};
	public static final String[] NEED_CONFIG      = new String[] {};

	public static final String[] PCT_COLUMNS      = new String[] {};
	public static final String[] DIFF_COLUMNS     = new String[] {}; // values are already "per sample"

	public static final boolean  NEGATIVE_DIFF_COUNTERS_TO_ZERO = false;
	public static final boolean  IS_SYSTEM_CM                   = true;
	public static final int      DEFAULT_POSTPONE_TIME          = 0;
	public static final int      DEFAULT_QUERY_TIMEOUT          = CountersModel.DEFAULT_sqlQueryTimeout;

	@Override public int     getDefaultPostponeTime()                 { return DEFAULT_POSTPONE_TIME; }
	@Override public int     getDefaultQueryTimeout()                 { return DEFAULT_QUERY_TIMEOUT; }
	@Override public boolean getDefaultIsNegativeDiffCountersToZero() { return NEGATIVE_DIFF_COUNTERS_TO_ZERO; }
	@Override public Type    getTemplateLevel()                       { return Type.MEDIUM; }

	// No diff columns, so DIFF and RATE would be copies of ABS
	@Override public boolean getDefaultIsPersistCountersDiffEnabled() { return false; }
	@Override public boolean getDefaultIsPersistCountersRateEnabled() { return false; }

	/**
	 * FACTORY  method to create the object
	 */
	public static CountersModel create(ICounterController counterController, IGuiController guiController)
	{
		if (guiController != null && guiController.hasGUI())
			guiController.splashWindowProgress("Loading: Counter Model '" + CM_NAME + "'");

		return new CmChattyStatements(counterController, guiController);
	}

	public CmChattyStatements(ICounterController counterController, IGuiController guiController)
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
	private static final String  PROP_PREFIX                   = CM_NAME;

	public static final String  PROPKEY_sample_topRows         = PROP_PREFIX + ".sample.topRows";
	public static final int     DEFAULT_sample_topRows         = 20;

	public static final String  PROPKEY_sample_maxLogicalReads = PROP_PREFIX + ".sample.maxLogicalReads";
	public static final int     DEFAULT_sample_maxLogicalReads = 100; // the opposite of CmExecQueryStats 'qs.last_logical_reads > 100'

	public static final String  PROPKEY_sample_resyncMinutes   = PROP_PREFIX + ".sample.resyncMinutes";
	public static final int     DEFAULT_sample_resyncMinutes   = 60;

	public static final String  PROPKEY_roundTripMs            = PROP_PREFIX + ".roundTripMs";
	public static final double  DEFAULT_roundTripMs            = 0.3;

	public static final String  TOOLTIP_sample_topRows         = "Keep the top ## statements by executions in this sample, and the top ## by rows in this sample.";
	public static final String  TOOLTIP_sample_maxLogicalReads = "Only statements with 'last_logical_reads' at or below this value (CmExecQueryStats collects the ones above 100).";
	public static final String  TOOLTIP_sample_resyncMinutes   = "Read ALL cheap statements (not only the ones executed since the previous sample) every ## minutes, and forget plans that are gone from the plan cache.";
	public static final String  TOOLTIP_roundTripMs            = "Estimated cost in milliseconds of one client/server round trip, used for column 'EstRoundTripMs' (executions * this value).";

	@Override
	protected void registerDefaultValues()
	{
		super.registerDefaultValues();

		Configuration.registerDefaultValue(PROPKEY_sample_topRows,         DEFAULT_sample_topRows);
		Configuration.registerDefaultValue(PROPKEY_sample_maxLogicalReads, DEFAULT_sample_maxLogicalReads);
		Configuration.registerDefaultValue(PROPKEY_sample_resyncMinutes,   DEFAULT_sample_resyncMinutes);
		Configuration.registerDefaultValue(PROPKEY_roundTripMs,            DEFAULT_roundTripMs);
	}

	/** Used by the: Create 'Offline Session' Wizard, and the collector settings */
	@Override
	public List<CmSettingsHelper> getLocalSettings()
	{
		Configuration conf = Configuration.getCombinedConfiguration();
		List<CmSettingsHelper> list = new ArrayList<>();

		list.add(new CmSettingsHelper("Top Rows",                    PROPKEY_sample_topRows        , Integer.class, conf.getIntProperty   (PROPKEY_sample_topRows        , DEFAULT_sample_topRows        ), DEFAULT_sample_topRows        , TOOLTIP_sample_topRows        ));
		list.add(new CmSettingsHelper("Max Logical Reads",           PROPKEY_sample_maxLogicalReads, Integer.class, conf.getIntProperty   (PROPKEY_sample_maxLogicalReads, DEFAULT_sample_maxLogicalReads), DEFAULT_sample_maxLogicalReads, TOOLTIP_sample_maxLogicalReads));
		list.add(new CmSettingsHelper("Full Read Every ## Minutes",  PROPKEY_sample_resyncMinutes  , Integer.class, conf.getIntProperty   (PROPKEY_sample_resyncMinutes  , DEFAULT_sample_resyncMinutes  ), DEFAULT_sample_resyncMinutes  , TOOLTIP_sample_resyncMinutes  ));
		list.add(new CmSettingsHelper("Round Trip Cost in ms",       PROPKEY_roundTripMs           , Double .class, conf.getDoubleProperty(PROPKEY_roundTripMs           , DEFAULT_roundTripMs           ), DEFAULT_roundTripMs           , TOOLTIP_roundTripMs           ));

		return list;
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

		pkCols.add("query_hash");

		return pkCols;
	}

	@Override
	public Map<String, PcsColumnOptions> getPcsColumnOptions()
	{
		Map<String, PcsColumnOptions> map = super.getPcsColumnOptions();

		// No settings in the super, create one, and set it at super
		if (map == null)
		{
			map = new HashMap<>();
			map.put("SqlText", new PcsColumnOptions(ColumnType.DICTIONARY_COMPRESSION));

			// Set the map in the super
			setPcsColumnOptions(map);
		}

		return map;
	}

	@Override
	public void addMonTableDictForVersion(DbxConnection conn, DbmsVersionInfo versionInfo)
	{
		try
		{
			MonTablesDictionary mtd = MonTablesDictionaryManager.getInstance();

			String cmName = this.getName();
			mtd.addTable(cmName, HTML_DESC);

			mtd.addColumn(cmName, "dbname"          , "<html>Database name (from the plan attribute 'dbid' of the plan with the most executions in this sample).</html>");
			mtd.addColumn(cmName, "query_hash"      , "<html>The <code>query_hash</code> all plans are grouped on. Literal SQL (same statement with different values) has the same query_hash.</html>");
			mtd.addColumn(cmName, "ExecCount"       , "<html>Number of executions <b>in this sample</b>.</html>");
			mtd.addColumn(cmName, "ExecPerSec"      , "<html>Executions per second in this sample.</html>");
			mtd.addColumn(cmName, "RowCount"        , "<html>Number of rows returned <b>in this sample</b>.</html>");
			mtd.addColumn(cmName, "RowsPerSec"      , "<html>Rows per second in this sample. <br>For a N+1 parent, this follows the child's <code>ExecPerSec</code>.</html>");
			mtd.addColumn(cmName, "RowsPerExec"     , "<html>Rows per execution in this sample. <br>About 1 for a N+1 child (a lookup on a key).</html>");
			mtd.addColumn(cmName, "AvgElapsedUs"    , "<html>Average elapsed time per execution in microseconds.</html>");
			mtd.addColumn(cmName, "AvgWorkerUs"     , "<html>Average CPU time per execution in microseconds.</html>");
			mtd.addColumn(cmName, "AvgLogicalReads" , "<html>Average logical reads per execution.</html>");
			mtd.addColumn(cmName, "ElapsedMs"       , "<html>Total elapsed time in milliseconds in this sample (inside SQL Server only).</html>");
			mtd.addColumn(cmName, "WorkerMs"        , "<html>Total CPU time in milliseconds in this sample.</html>");
			mtd.addColumn(cmName, "LogicalReads"    , "<html>Total logical reads in this sample.</html>");
			mtd.addColumn(cmName, "EstRoundTripMs"  , "<html>Estimated time the <b>client</b> spent on round trips in this sample: <code>ExecCount * " + PROPKEY_roundTripMs + "</code>. <br>This time is not visible in SQL Server, but it is what the application pays for N+1.</html>");
			mtd.addColumn(cmName, "PlanCount"       , "<html>Number of plans (with this query_hash) that were executed in this sample. Many plans = literal SQL.</html>");
			mtd.addColumn(cmName, "plan_handle"     , "<html>plan_handle of the plan with the most executions in this sample.</html>");
			mtd.addColumn(cmName, "SqlText"         , "<html>SQL text of the plan with the most executions in this sample.</html>");
		}
		catch (NameNotFoundException e)
		{
			_logger.warn("Problems in cm='" + CM_NAME + "', adding addMonTableDictForVersion. Caught: " + e);
		}
	}

	@Override
	public String getSqlForVersion(DbxConnection conn, DbmsVersionInfo versionInfo)
	{
		DbmsVersionInfoSqlServer ssVersionInfo = (DbmsVersionInfoSqlServer) versionInfo;

		_dm_exec_query_stats = ssVersionInfo.isAzureSynapseAnalytics() ? "dm_pdw_nodes_exec_query_stats" : "dm_exec_query_stats";

		// The SQL is executed in CounterSamplePrivate.getSample() -> createResultSet()
		return "-- reads 'sys." + _dm_exec_query_stats + "' (cheap statements), see CmChattyStatements.createResultSet()";
	}

	@Override
	public CounterSample createCounterSample(String name, boolean negativeDiffCountersToZero, String[] diffColumns, CounterSample prevSample)
	{
		return new CounterSamplePrivate(name, negativeDiffCountersToZero, diffColumns, prevSample);
	}

	private static class CounterSamplePrivate
	extends CounterSample
	{
		private static final long serialVersionUID = 1L;

		public CounterSamplePrivate(String name, boolean negativeDiffCountersToZero, String[] diffColumns, CounterSample prevSample)
		{
			super(name, negativeDiffCountersToZero, diffColumns, prevSample);
		}

		@Override
		public boolean getSample(CountersModel cm, DbxConnection conn, String sql, List<String> pkList)
		throws SQLException, NoValidRowsInSample
		{
			// set the sample time (from the server) and interval
			updateSampleTime(conn, cm);

			_rows = new ArrayList<List<Object>>();

			ResultSet rs = ((CmChattyStatements)cm).createResultSet(conn, getSampleTime());

			ResultSetMetaData originRsmd = rs.getMetaData();
			if ( ! cm.hasResultSetMetaData() )
				cm.setResultSetMetaData( cm.createResultSetMetaData(originRsmd) );

			if (readResultset(cm, rs, cm.getResultSetMetaData(), originRsmd, pkList, 0))
				rs.close();

			return true;
		}
	}


	//------------------------------------------------------------
	// State between samples
	//------------------------------------------------------------
	private String _dm_exec_query_stats = "dm_exec_query_stats";

	/** Server time when the previous read STARTED (null = no read yet) */
	private Timestamp _lastReadSrvTime;

	/** Server time of the last FULL read */
	private Timestamp _lastFullReadSrvTime;

	/** Previous counters per plan; key = plan_handle:start:end:plan_generation_num */
	private Map<String, PlanCounters> _planCounters = new HashMap<>();

	/** Cached [dbname, SqlText] per query_hash (text does not change) */
	private Map<String, String[]> _textCache = new HashMap<>();

	private static final int TEXT_CACHE_MAX_SIZE = 10_000;

	// Same binary format as the framework uses for BINARY/VARBINARY columns (so query_hash matches CmExecQueryStats)
	private static final String  BINARY_PREFIX  = Configuration.getCombinedConfiguration().getProperty(       ResultSetTableModel.PROPKEY_BINERY_PREFIX,  ResultSetTableModel.DEFAULT_BINERY_PREFIX);
	private static final boolean BINARY_TOUPPER = Configuration.getCombinedConfiguration().getBooleanProperty(ResultSetTableModel.PROPKEY_BINARY_TOUPPER, ResultSetTableModel.DEFAULT_BINARY_TOUPPER);

	private static class PlanCounters
	{
		long execCount;
		long rows;
		long elapsedUs;
		long workerUs;
		long logicalReads;
		long lastSeenTime; // server time in ms of the read where it was last seen
	}

	/** Summary for one query_hash in this sample */
	private static class QueryHashSum
	{
		String queryHash;
		long   execCount;
		long   rows;
		long   elapsedUs;
		long   workerUs;
		long   logicalReads;
		int    planCount;

		// The plan with most executions in this sample (used for SQL text and dbname)
		long   topPlanExecCount = -1;
		byte[] topSqlHandle;
		byte[] topPlanHandle;
		int    topStartOffset;
		int    topEndOffset;
	}

	@Override
	public void close()
	{
		_lastReadSrvTime     = null;
		_lastFullReadSrvTime = null;
		_planCounters.clear();
		_textCache.clear();

		super.close();
	}

	/**
	 * Read the cheap statements, calculate what happened since the previous read, group by query_hash, keep top rows,
	 * and return them as a ResultSet (read by the framework like any other ResultSet).
	 */
	private ResultSet createResultSet(DbxConnection conn, Timestamp thisReadSrvTime)
	throws SQLException
	{
		Configuration conf = Configuration.getCombinedConfiguration();
		int    topRows         = conf.getIntProperty   (PROPKEY_sample_topRows,         DEFAULT_sample_topRows);
		int    maxLogicalReads = conf.getIntProperty   (PROPKEY_sample_maxLogicalReads, DEFAULT_sample_maxLogicalReads);
		int    resyncMinutes   = conf.getIntProperty   (PROPKEY_sample_resyncMinutes,   DEFAULT_sample_resyncMinutes);
		double roundTripMs     = conf.getDoubleProperty(PROPKEY_roundTripMs,            DEFAULT_roundTripMs);

		if (thisReadSrvTime == null)
			thisReadSrvTime = new Timestamp(System.currentTimeMillis());

		Timestamp prevReadSrvTime = _lastReadSrvTime;

		// FULL read: first time, and every ## minutes (to forget plans that has been removed from the plan cache)
		boolean fullRead = prevReadSrvTime == null
				|| _lastFullReadSrvTime == null
				|| (thisReadSrvTime.getTime() - _lastFullReadSrvTime.getTime()) >= resyncMinutes * 60_000L;

		// Statements are added to dm_exec_query_stats when they complete, so use completion time = last_execution_time + last_elapsed_time
		// Subtract 5 seconds from previous read time, so we do not miss anything (a statement that is read twice has delta 0 the second time)
		String sql = ""
				+ "SELECT /* ${cmCollectorName} */ \n"
				+ "     qs.sql_handle \n"
				+ "    ,qs.plan_handle \n"
				+ "    ,qs.statement_start_offset \n"
				+ "    ,qs.statement_end_offset \n"
				+ "    ,qs.plan_generation_num \n"
				+ "    ,qs.query_hash \n"
				+ "    ,qs.creation_time \n"
				+ "    ,qs.execution_count \n"
				+ "    ,qs.total_rows \n"
				+ "    ,qs.total_elapsed_time \n"
				+ "    ,qs.total_worker_time \n"
				+ "    ,qs.total_logical_reads \n"
				+ "FROM sys." + _dm_exec_query_stats + " qs \n"
				+ "WHERE qs.last_logical_reads <= ? \n"
				+ (fullRead ? "" : "  AND DATEADD(ms, qs.last_elapsed_time/1000, qs.last_execution_time) >= DATEADD(ss, -5, ?) \n")
				;
		sql = sql.replace("${cmCollectorName}", getName());

		long thisReadTimeMs = thisReadSrvTime.getTime();
		Map<String, QueryHashSum> sumMap = new HashMap<>();
		int planRowsRead = 0;

		try (PreparedStatement pstmnt = conn.prepareStatement(sql))
		{
			pstmnt.setQueryTimeout(getQueryTimeout());
			pstmnt.setInt(1, maxLogicalReads);
			if ( ! fullRead )
				pstmnt.setTimestamp(2, prevReadSrvTime);

			try (ResultSet rs = pstmnt.executeQuery())
			{
				while (rs.next())
				{
					planRowsRead++;

					byte[]    sqlHandle      = rs.getBytes    (1);
					byte[]    planHandle     = rs.getBytes    (2);
					int       startOffset    = rs.getInt      (3);
					int       endOffset      = rs.getInt      (4);
					long      planGenNum     = rs.getLong     (5);
					byte[]    queryHashBytes = rs.getBytes    (6);
					Timestamp creationTime   = rs.getTimestamp(7);
					long      execCount      = rs.getLong     (8);
					long      rows           = rs.getLong     (9);
					long      elapsedUs      = rs.getLong     (10);
					long      workerUs       = rs.getLong     (11);
					long      logicalReads   = rs.getLong     (12);

					String planKey = StringUtil.bytesToHex("", planHandle) + ":" + startOffset + ":" + endOffset + ":" + planGenNum;

					// Calculate what happened since previous read
					PlanCounters prev = _planCounters.get(planKey);
					boolean createdSincePrevRead = prevReadSrvTime != null && creationTime != null && ! creationTime.before(prevReadSrvTime);

					long dExec = 0, dRows = 0, dElapsed = 0, dWorker = 0, dReads = 0;
					if (prev != null && execCount >= prev.execCount)
					{
						dExec    = execCount    - prev.execCount;
						dRows    = rows         - prev.rows;
						dElapsed = elapsedUs    - prev.elapsedUs;
						dWorker  = workerUs     - prev.workerUs;
						dReads   = logicalReads - prev.logicalReads;
					}
					else if (createdSincePrevRead)
					{
						// New plan since previous read: the counters are the delta
						dExec    = execCount;
						dRows    = rows;
						dElapsed = elapsedUs;
						dWorker  = workerUs;
						dReads   = logicalReads;
					}
					// else: first time we see this (old) plan, just remember the counters

					if (prev == null)
					{
						prev = new PlanCounters();
						_planCounters.put(planKey, prev);
					}
					prev.execCount    = execCount;
					prev.rows         = rows;
					prev.elapsedUs    = elapsedUs;
					prev.workerUs     = workerUs;
					prev.logicalReads = logicalReads;
					prev.lastSeenTime = thisReadTimeMs;

					if (dExec <= 0)
						continue;

					// Group on query_hash (if it's missing or 0x0000000000000000, use the statement itself)
					String queryHash = StringUtil.bytesToHex(BINARY_PREFIX, queryHashBytes, BINARY_TOUPPER);
					if (queryHashBytes == null || isAllZero(queryHashBytes))
						queryHash = StringUtil.bytesToHex(BINARY_PREFIX, sqlHandle, BINARY_TOUPPER) + ":" + startOffset + ":" + endOffset;

					QueryHashSum sum = sumMap.get(queryHash);
					if (sum == null)
					{
						sum = new QueryHashSum();
						sum.queryHash = queryHash;
						sumMap.put(queryHash, sum);
					}
					sum.execCount    += dExec;
					sum.rows         += dRows;
					sum.elapsedUs    += dElapsed;
					sum.workerUs     += dWorker;
					sum.logicalReads += dReads;
					sum.planCount++;

					if (dExec > sum.topPlanExecCount)
					{
						sum.topPlanExecCount = dExec;
						sum.topSqlHandle     = sqlHandle;
						sum.topPlanHandle    = planHandle;
						sum.topStartOffset   = startOffset;
						sum.topEndOffset     = endOffset;
					}
				}
			}
		}

		// After a FULL read: forget plans that are no longer in the plan cache (or no longer cheap)
		int removedPlans = 0;
		if (fullRead)
		{
			for (Iterator<PlanCounters> it = _planCounters.values().iterator(); it.hasNext();)
			{
				if (it.next().lastSeenTime != thisReadTimeMs)
				{
					it.remove();
					removedPlans++;
				}
			}
			_lastFullReadSrvTime = thisReadSrvTime;

			if (_textCache.size() > TEXT_CACHE_MAX_SIZE)
				_textCache.clear();
		}
		_lastReadSrvTime = thisReadSrvTime;

		// Keep: top ## by executions + top ## by rows
		List<QueryHashSum> all = new ArrayList<>(sumMap.values());
		Map<String, QueryHashSum> keep = new LinkedHashMap<>();

		all.sort(Comparator.comparingLong((QueryHashSum s) -> s.execCount).reversed());
		for (int i = 0; i < Math.min(topRows, all.size()); i++)
			keep.put(all.get(i).queryHash, all.get(i));

		all.sort(Comparator.comparingLong((QueryHashSum s) -> s.rows).reversed());
		for (int i = 0; i < Math.min(topRows, all.size()); i++)
			if (all.get(i).rows > 0)
				keep.put(all.get(i).queryHash, all.get(i));

		// Get dbname + SQL Text for the ones we don't have in the cache
		List<QueryHashSum> lookup = new ArrayList<>();
		for (QueryHashSum sum : keep.values())
			if ( ! _textCache.containsKey(sum.queryHash) )
				lookup.add(sum);

		if ( ! lookup.isEmpty() )
		{
			StringBuilder values = new StringBuilder();
			for (int i = 0; i < lookup.size(); i++)
			{
				QueryHashSum sum = lookup.get(i);
				values.append(i == 0 ? "     " : "    ,")
					.append("(").append(i)
					.append(", ").append(StringUtil.bytesToHex("0x", sum.topSqlHandle))
					.append(", ").append(StringUtil.bytesToHex("0x", sum.topPlanHandle))
					.append(", ").append(sum.topStartOffset)
					.append(", ").append(sum.topEndOffset)
					.append(")\n");
			}

			String textSql = ""
					+ "SELECT /* " + getName() + " */ \n"
					+ "     v.id \n"
					+ "    ,(select isnull(db_name(CONVERT(int, pa.value)), CONVERT(nvarchar(10), pa.value)) from sys.dm_exec_plan_attributes(v.plan_handle) pa where pa.attribute = N'dbid') AS dbname \n"
					+ "    ,SUBSTRING(txt.text, (v.so/2)+1, ((CASE WHEN v.eo = -1 THEN DATALENGTH(txt.text) ELSE v.eo END - v.so)/2) + 1) AS SqlText \n"
					+ "FROM (VALUES \n"
					+ values
					+ ") v(id, sql_handle, plan_handle, so, eo) \n"
					+ "OUTER APPLY sys.dm_exec_sql_text(v.sql_handle) txt \n"
					;

			try (Statement stmnt = conn.createStatement())
			{
				stmnt.setQueryTimeout(getQueryTimeout());
				try (ResultSet rs = stmnt.executeQuery(textSql))
				{
					while (rs.next())
					{
						int    id      = rs.getInt   (1);
						String dbname  = rs.getString(2);
						String sqlText = rs.getString(3);

						if (id >= 0 && id < lookup.size())
							_textCache.put(lookup.get(id).queryHash, new String[] {dbname, sqlText});
					}
				}
			}
			catch (SQLException ex)
			{
				_logger.warn(getName() + ": Problems getting SQL Text for " + lookup.size() + " statements. Continuing without them. Caught: " + ex);
			}
		}

		// Build the ResultSet
		double intervalSec = prevReadSrvTime == null ? 0 : (thisReadTimeMs - prevReadSrvTime.getTime()) / 1000.0;

		SimpleResultSet srs = new SimpleResultSet();
		srs.addColumn("dbname",          Types.VARCHAR, 128, 0);
		srs.addColumn("query_hash",      Types.VARCHAR, 200, 0);
		srs.addColumn("ExecCount",       Types.BIGINT,    0, 0);
		srs.addColumn("ExecPerSec",      Types.NUMERIC,  12, 1);
		srs.addColumn("RowCount",        Types.BIGINT,    0, 0);
		srs.addColumn("RowsPerSec",      Types.NUMERIC,  12, 1);
		srs.addColumn("RowsPerExec",     Types.NUMERIC,  12, 2);
		srs.addColumn("AvgElapsedUs",    Types.NUMERIC,  14, 1);
		srs.addColumn("AvgWorkerUs",     Types.NUMERIC,  14, 1);
		srs.addColumn("AvgLogicalReads", Types.NUMERIC,  12, 1);
		srs.addColumn("ElapsedMs",       Types.NUMERIC,  14, 1);
		srs.addColumn("WorkerMs",        Types.NUMERIC,  14, 1);
		srs.addColumn("LogicalReads",    Types.BIGINT,    0, 0);
		srs.addColumn("EstRoundTripMs",  Types.NUMERIC,  14, 1);
		srs.addColumn("PlanCount",       Types.INTEGER,   0, 0);
		srs.addColumn("plan_handle",     Types.VARCHAR, 130, 0);
		srs.addColumn("SqlText",         Types.VARCHAR, Integer.MAX_VALUE, 0);

		for (QueryHashSum sum : keep.values())
		{
			String[] text = _textCache.get(sum.queryHash);

			srs.addRow(
				text == null ? null : text[0],
				sum.queryHash,
				sum.execCount,
				intervalSec <= 0 ? null : num(sum.execCount / intervalSec, 1),
				sum.rows,
				intervalSec <= 0 ? null : num(sum.rows / intervalSec, 1),
				num(sum.rows         * 1.0 / sum.execCount, 2),
				num(sum.elapsedUs    * 1.0 / sum.execCount, 1),
				num(sum.workerUs     * 1.0 / sum.execCount, 1),
				num(sum.logicalReads * 1.0 / sum.execCount, 1),
				num(sum.elapsedUs / 1000.0, 1),
				num(sum.workerUs  / 1000.0, 1),
				sum.logicalReads,
				num(sum.execCount * roundTripMs, 1),
				sum.planCount,
				StringUtil.bytesToHex(BINARY_PREFIX, sum.topPlanHandle, BINARY_TOUPPER),
				text == null ? null : text[1]
			);
		}

		if (_logger.isDebugEnabled())
			_logger.debug(getName() + ": fullRead=" + fullRead + ", planRowsRead=" + planRowsRead + ", planCounters=" + _planCounters.size() + ", removedPlans=" + removedPlans
					+ ", queryHashesWithExec=" + sumMap.size() + ", keptRows=" + keep.size() + ", textLookups=" + lookup.size() + ", intervalSec=" + intervalSec);

		return srs;
	}

	private static boolean isAllZero(byte[] bytes)
	{
		for (byte b : bytes)
			if (b != 0)
				return false;
		return true;
	}

	private static BigDecimal num(double val, int scale)
	{
		if (Double.isNaN(val) || Double.isInfinite(val))
			return null;
		return new BigDecimal(val).setScale(scale, RoundingMode.HALF_EVEN);
	}
}
