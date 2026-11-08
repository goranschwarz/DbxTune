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
import java.util.Set;

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
import com.dbxtune.pcs.PcsColumnOptions;
import com.dbxtune.pcs.PcsColumnOptions.ColumnType;
import com.dbxtune.pcs.inspection.ObjectLookupInspectorDb2;
import com.dbxtune.sql.conn.DbxConnection;
import com.dbxtune.sql.conn.info.DbmsVersionInfo;
import com.dbxtune.utils.Configuration;
import com.dbxtune.utils.Ver;

/**
 * Cheap statements that are executed very often ("chatty" statements), the ones that {@link CmStatements} loses
 * (it only keeps the top 1000 by execution time since the statement came into the package cache, every 5 minutes).
 * <p>
 * Used to find <b>N+1 queries</b>: one parent statement returns N rows, and the application then executes one cheap
 * child lookup per row (1 row per call, sub millisecond, but N round trips).
 * <p>
 * How it works (same as SQL Server's, ASE's and Oracle's CmChattyStatements):
 * <ul>
 *   <li>Reads only key and counter columns from <code>MON_GET_PKG_CACHE_STMT</code> for cheap statements (no statement text).</li>
 *   <li>A full read at start and every ## minutes, in between only statements updated since the previous sample (<code>LAST_METRICS_UPDATE</code>).</li>
 *   <li>Keeps the previous counters per package cache entry (EXECUTABLE_ID) in memory, and calculates what happened <b>in this sample</b>
 *       (so bursts on statements that have been in the package cache for days are caught).</li>
 *   <li>Groups the entries by <code>STMTID + SEMANTIC_ENV_ID</code> (STMTID is the same for a statement with different literal values,
 *       as SQL Server's query_hash and Oracle's FORCE_MATCHING_SIGNATURE; SEMANTIC_ENV_ID is the default schema and path),
 *       and keeps only the top ## by executions and the top ## by rows returned in this sample.</li>
 *   <li>The statement text is fetched only for the kept rows (and cached per group).</li>
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
		"<p>Cheap statements that are executed very often (the ones the 'Statements' tab loses: it keeps the top 1000 by execution time).</p>" +
		"<p>Values are for <b>this sample only</b>, grouped by <code>STMTID</code> (literal SQL is folded together), top ## by executions and top ## by rows.</p>" +
		"<p>Used to find <b>N+1 queries</b>: a child lookup with about 1 row per execution, executed once for every row of a parent statement.</p>" +
		"</html>";

	public static final String   GROUP_NAME       = MainFrame.TCP_GROUP_OBJECT_ACCESS;
	public static final String   GUI_ICON_FILE    = "images/CmStatements.png";

	public static final long     NEED_SRV_VERSION = 0;
	public static final long     NEED_CE_VERSION  = 0;

	public static final String[] MON_TABLES       = new String[] {"MON_GET_PKG_CACHE_STMT"};
	public static final String[] NEED_ROLES       = new String[] {};
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
	// CountersModel.isPersistCounters{Diff,Rate}Enabled() return the ABS setting, so the defaults above are not enough (same as CmIqMsgLogParser)
	@Override public boolean isPersistCountersDiffEnabled()           { return false; }
	@Override public boolean isPersistCountersRateEnabled()           { return false; }

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
	public static final int     DEFAULT_sample_maxLogicalReads = 100;

	public static final String  PROPKEY_sample_resyncMinutes   = PROP_PREFIX + ".sample.resyncMinutes";
	public static final int     DEFAULT_sample_resyncMinutes   = 60;

	public static final String  PROPKEY_roundTripMs            = PROP_PREFIX + ".roundTripMs";
	public static final double  DEFAULT_roundTripMs            = 0.3;

	public static final String  TOOLTIP_sample_topRows         = "Keep the top ## statements by executions in this sample, and the top ## by rows returned in this sample.";
	public static final String  TOOLTIP_sample_maxLogicalReads = "Only statements with at most ## buffer pool logical reads per execution (average since the statement came into the package cache).";
	public static final String  TOOLTIP_sample_resyncMinutes   = "Read ALL cheap statements (not only the ones updated since the previous sample) every ## minutes, and forget statements that are gone from the package cache.";
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

	/** If we can't execute the function: disable the CM, and tell what to do about it */
	@Override
	public boolean checkDependsOnOther(DbxConnection conn)
	{
		return Db2CmUtils.checkFunctionsAreExecutable(this, conn, "MON_GET_PKG_CACHE_STMT(NULL, NULL, NULL, -2)");
	}

	@Override
	public List<String> getPkForVersion(DbxConnection conn, DbmsVersionInfo versionInfo)
	{
		List <String> pkCols = new LinkedList<String>();

		pkCols.add("MEMBER");
		pkCols.add("STMTID");
		pkCols.add("SEMANTIC_ENV_ID");

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
			map.put("STMT_TEXT", new PcsColumnOptions(ColumnType.DICTIONARY_COMPRESSION));

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

			mtd.addColumn(cmName, "MEMBER"          , "<html>The database member (0 if this is not pureScale or DPF).</html>");
			mtd.addColumn(cmName, "STMTID"          , "<html>What the statements are grouped on: the same statement with different literal values has the same STMTID. <br>Below Db2 10.5 FP4 (no STMTID) it is the EXECUTABLE_ID.</html>");
			mtd.addColumn(cmName, "SEMANTIC_ENV_ID" , "<html>The default schema and function path the statement was compiled with (the same text in another schema is another statement).</html>");
			mtd.addColumn(cmName, "EXECUTABLE_ID"   , "<html>EXECUTABLE_ID (hex) of the package cache entry with the most executions in this sample (with this STMTID). The same id as in the 'Statements' and 'Active Statements' tabs.</html>");
			mtd.addColumn(cmName, "SECTION_TYPE"    , "<html>D = dynamic SQL, S = static SQL (in a package).</html>");
			mtd.addColumn(cmName, "PACKAGE_SCHEMA"  , "<html>The schema of the package (static SQL). NULL for dynamic SQL.</html>");
			mtd.addColumn(cmName, "IS_SYSTEM_STMT"  , "<html>Y = Db2's own SQL, or DbxTune's: the package schema starts with SYS, or all tables in the statement are Db2's own (SYS* schemas, table functions like MON_GET_*, SYSIBM.SYSDUMMY1). <br>The Daily Summary Report lists them, but does not take them as N+1.</html>");
			mtd.addColumn(cmName, "ExecCount"       , "<html>Number of executions <b>in this sample</b> (NUM_COORD_EXEC: executions by the coordinator agent, so DPF does not count a statement once per member).</html>");
			mtd.addColumn(cmName, "ExecPerSec"      , "<html>Executions per second in this sample.</html>");
			mtd.addColumn(cmName, "RowCount"        , "<html>Number of rows returned <b>in this sample</b> (ROWS_RETURNED).</html>");
			mtd.addColumn(cmName, "RowsPerSec"      , "<html>Rows returned per second in this sample. <br>For a N+1 parent, this follows the child's <code>ExecPerSec</code>.</html>");
			mtd.addColumn(cmName, "RowsPerExec"     , "<html>Rows returned per execution in this sample. <br>About 1 for a N+1 child (a lookup on a key).</html>");
			mtd.addColumn(cmName, "RowsReadPerExec" , "<html>Rows read per execution in this sample (ROWS_READ).</html>");
			mtd.addColumn(cmName, "AvgElapsedMs"    , "<html>Average execution time per execution in milliseconds (STMT_EXEC_TIME).</html>");
			mtd.addColumn(cmName, "AvgCpuMs"        , "<html>Average CPU time per execution in milliseconds (TOTAL_CPU_TIME).</html>");
			mtd.addColumn(cmName, "AvgLogicalReads" , "<html>Average buffer pool logical reads per execution (data, index, temp, XML and column-organized).</html>");
			mtd.addColumn(cmName, "ElapsedMs"       , "<html>Total execution time in milliseconds in this sample (inside Db2 only).</html>");
			mtd.addColumn(cmName, "CpuMs"           , "<html>Total CPU time in milliseconds in this sample.</html>");
			mtd.addColumn(cmName, "LogicalReads"    , "<html>Total buffer pool logical reads in this sample.</html>");
			mtd.addColumn(cmName, "EstRoundTripMs"  , "<html>Estimated time the <b>client</b> spent on round trips in this sample: <code>ExecCount * " + PROPKEY_roundTripMs + "</code>. <br>This time is not visible in Db2, but it is what the application pays for N+1.</html>");
			mtd.addColumn(cmName, "ExecIdCount"     , "<html>Number of package cache entries (EXECUTABLE_ID) with this STMTID that were executed in this sample. Many of them = literal SQL (no parameter markers).</html>");
			mtd.addColumn(cmName, "STMT_TEXT"       , "<html>The statement text (of one package cache entry with this STMTID: with literal SQL, one of the literal values).</html>");
		}
		catch (NameNotFoundException e)
		{
			_logger.warn("Problems in cm='" + CM_NAME + "', adding addMonTableDictForVersion. Caught: " + e);
		}
	}

	/** STMTID and SEMANTIC_ENV_ID came in Db2 10.5 FP4 */
	private boolean _hasStmtId = true;

	@Override
	public String getSqlForVersion(DbxConnection conn, DbmsVersionInfo versionInfo)
	{
		_hasStmtId = versionInfo.getLongVersion() >= Ver.ver(10,5,0,4);

		// The SQL is executed in CounterSamplePrivate.getSample() -> createResultSet()
		return "-- reads 'MON_GET_PKG_CACHE_STMT' (cheap statements), see CmChattyStatements.createResultSet()";
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

			ResultSet rs = ((CmChattyStatements)cm).createResultSet(conn);

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

	/** Server time (CURRENT TIMESTAMP, the same clock as LAST_METRICS_UPDATE) when the previous read STARTED (null = no read yet) */
	private Timestamp _lastReadSrvTime;

	/** Client time in ms when the previous read STARTED (for the interval) */
	private long _lastReadClientTime;

	/** Server time of the last FULL read */
	private Timestamp _lastFullReadSrvTime;

	/** Previous counters per package cache entry; key = MEMBER:EXECUTABLE_ID */
	private Map<String, EntryCounters> _entryCounters = new HashMap<>();

	/** Cached [STMT_TEXT, PACKAGE_SCHEMA, SECTION_TYPE, IS_SYSTEM_STMT] per group (MEMBER:STMTID:SEMANTIC_ENV_ID): the same statement, so it does not change (with literals: one of the values) */
	private Map<String, String[]> _textCache = new HashMap<>();

	private static final int TEXT_CACHE_MAX_SIZE     = 10_000;

	/** Literal SQL makes a new package cache entry per value: do a FULL read (it forgets the entries that are gone) before the map gets too big */
	private static final int ENTRY_COUNTERS_MAX_SIZE = 100_000;

	private static class EntryCounters
	{
		long executions;
		long rows;
		long rowsRead;
		long logicalReads;
		long elapsedMs;
		long cpuUs;
		long lastSeenTime; // client time in ms of the read where it was last seen
	}

	/** Summary for one MEMBER:STMTID:SEMANTIC_ENV_ID in this sample */
	private static class StmtSum
	{
		String key;
		int    member;
		String stmtId;
		long   semEnvId;
		long   execCount;
		long   rows;
		long   rowsRead;
		long   logicalReads;
		long   elapsedMs;
		long   cpuUs;
		int    entryCount;

		// The EXECUTABLE_ID with most executions in this sample (used for the statement text)
		long   topEntryExecCount = -1;
		byte[] topExecId;
	}

	@Override
	public void close()
	{
		_lastReadSrvTime     = null;
		_lastFullReadSrvTime = null;
		_entryCounters.clear();
		_textCache.clear();

		super.close();
	}

	/**
	 * Read the cheap statements, calculate what happened since the previous read, group by MEMBER + STMTID + SEMANTIC_ENV_ID, keep top rows,
	 * and return them as a ResultSet (read by the framework like any other ResultSet).
	 */
	private ResultSet createResultSet(DbxConnection conn)
	throws SQLException
	{
		Configuration conf = Configuration.getCombinedConfiguration();
		int    topRows         = conf.getIntProperty   (PROPKEY_sample_topRows,         DEFAULT_sample_topRows);
		int    maxLogicalReads = conf.getIntProperty   (PROPKEY_sample_maxLogicalReads, DEFAULT_sample_maxLogicalReads);
		int    resyncMinutes   = conf.getIntProperty   (PROPKEY_sample_resyncMinutes,   DEFAULT_sample_resyncMinutes);
		double roundTripMs     = conf.getDoubleProperty(PROPKEY_roundTripMs,            DEFAULT_roundTripMs);

		long thisReadClientTime = System.currentTimeMillis();

		// LAST_METRICS_UPDATE is in the server's clock: so compare it with CURRENT TIMESTAMP (not the collector's clock)
		Timestamp thisReadSrvTime = null;
		try (Statement stmnt = conn.createStatement(); ResultSet rs = stmnt.executeQuery("values CURRENT TIMESTAMP"))
		{
			while (rs.next())
				thisReadSrvTime = rs.getTimestamp(1);
		}
		if (thisReadSrvTime == null)
			thisReadSrvTime = new Timestamp(thisReadClientTime);

		Timestamp prevReadSrvTime    = _lastReadSrvTime;
		long      prevReadClientTime = _lastReadClientTime;

		// FULL read: first time, and every ## minutes (to forget statements that are gone from the package cache)
		boolean fullRead = prevReadSrvTime == null
				|| _lastFullReadSrvTime == null
				|| (thisReadSrvTime.getTime() - _lastFullReadSrvTime.getTime()) >= resyncMinutes * 60_000L
				|| _entryCounters.size() > ENTRY_COUNTERS_MAX_SIZE;

		// Cheap statements: logical reads per execution (average since the statement came into the package cache) at or below the limit
		// LAST_METRICS_UPDATE: subtract 5 seconds from previous read time, so we do not miss anything (an entry that is read twice has delta 0 the second time)
		// NUM_COORD_EXEC: executions by the coordinator agent (with DPF the other members also count NUM_EXECUTIONS for their part of the statement)
		// Times: STMT_EXEC_TIME is in milliseconds, TOTAL_CPU_TIME in microseconds
		String logicalReads = "(POOL_DATA_L_READS + POOL_INDEX_L_READS + POOL_TEMP_DATA_L_READS + POOL_TEMP_INDEX_L_READS + POOL_XDA_L_READS + POOL_COL_L_READS)";
		String sql = ""
				+ "select /* ${cmCollectorName} */ \n"
				+ "     MEMBER \n"
				+ "    ,EXECUTABLE_ID \n"
				+ "    ," + (_hasStmtId ? "STMTID"          : "CAST(NULL AS BIGINT)") + " \n"
				+ "    ," + (_hasStmtId ? "SEMANTIC_ENV_ID" : "CAST(NULL AS BIGINT)") + " \n"
				+ "    ,NUM_COORD_EXEC \n"
				+ "    ,ROWS_RETURNED \n"
				+ "    ,ROWS_READ \n"
				+ "    ," + logicalReads + " \n"
				+ "    ,STMT_EXEC_TIME \n"
				+ "    ,TOTAL_CPU_TIME \n"
				+ "from table(MON_GET_PKG_CACHE_STMT(NULL, NULL, NULL, -2)) t \n"
				+ "where NUM_EXECUTIONS > 0 \n"
				+ "  and " + logicalReads + " <= ? * NUM_EXECUTIONS \n"
				+ (fullRead ? "" : "  and LAST_METRICS_UPDATE >= CAST(? AS TIMESTAMP) - 5 SECONDS \n")
				;
		sql = sql.replace("${cmCollectorName}", getName());

		Map<String, StmtSum> sumMap = new HashMap<>();
		int entryRowsRead = 0;
		int newEntries    = 0;

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
					entryRowsRead++;

					int    member       = rs.getInt  (1);
					byte[] execId       = rs.getBytes(2);
					long   stmtIdNum    = rs.getLong (3);
					boolean noStmtId    = rs.wasNull();
					long   semEnvId     = rs.getLong (4);
					long   executions   = rs.getLong (5);
					long   rowsReturned = rs.getLong (6);
					long   rowsRead     = rs.getLong (7);
					long   lReads       = rs.getLong (8);
					long   elapsedMs    = rs.getLong (9);
					long   cpuUs        = rs.getLong (10);

					String execIdHex = hex(execId);
					String entryKey  = member + ":" + execIdHex;

					// Calculate what happened since previous read
					EntryCounters prev = _entryCounters.get(entryKey);

					long dExec = 0, dRows = 0, dRowsRead = 0, dReads = 0, dElapsed = 0, dCpu = 0;
					if (prev != null && executions >= prev.executions)
					{
						dExec     = executions   - prev.executions;
						dRows     = rowsReturned - prev.rows;
						dRowsRead = rowsRead     - prev.rowsRead;
						dReads    = lReads       - prev.logicalReads;
						dElapsed  = elapsedMs    - prev.elapsedMs;
						dCpu      = cpuUs        - prev.cpuUs;
					}
					else if (prev == null && ! fullRead)
					{
						// Not seen before, but updated since the previous read: a new package cache entry (the previous FULL read saw all cheap entries)
						// So the counters are the delta.
						// Note: an entry that was NOT cheap before, and now is, will also end up here (all it's history in one sample). Should be rare.
						dExec     = executions;
						dRows     = rowsReturned;
						dRowsRead = rowsRead;
						dReads    = lReads;
						dElapsed  = elapsedMs;
						dCpu      = cpuUs;
						newEntries++;
					}
					// else: first time we see this (old) entry, or the counters started over: just remember the counters

					if (prev == null)
					{
						prev = new EntryCounters();
						_entryCounters.put(entryKey, prev);
					}
					prev.executions   = executions;
					prev.rows         = rowsReturned;
					prev.rowsRead     = rowsRead;
					prev.logicalReads = lReads;
					prev.elapsedMs    = elapsedMs;
					prev.cpuUs        = cpuUs;
					prev.lastSeenTime = thisReadClientTime;

					if (dExec <= 0)
						continue;

					// Group on STMTID (the same statement with different literals) + SEMANTIC_ENV_ID (default schema), without STMTID use the EXECUTABLE_ID
					String stmtId   = noStmtId ? execIdHex : Long.toString(stmtIdNum);
					String groupKey = member + ":" + stmtId + ":" + semEnvId;

					StmtSum sum = sumMap.get(groupKey);
					if (sum == null)
					{
						sum = new StmtSum();
						sum.key      = groupKey;
						sum.member   = member;
						sum.stmtId   = stmtId;
						sum.semEnvId = semEnvId;
						sumMap.put(groupKey, sum);
					}
					sum.execCount    += dExec;
					sum.rows         += Math.max(0, dRows);
					sum.rowsRead     += Math.max(0, dRowsRead);
					sum.logicalReads += Math.max(0, dReads);
					sum.elapsedMs    += Math.max(0, dElapsed);
					sum.cpuUs        += Math.max(0, dCpu);
					sum.entryCount++;

					if (dExec > sum.topEntryExecCount)
					{
						sum.topEntryExecCount = dExec;
						sum.topExecId         = execId;
					}
				}
			}
		}

		// After a FULL read: forget entries that are no longer in the package cache (or no longer cheap)
		int removedEntries = 0;
		if (fullRead)
		{
			for (Iterator<EntryCounters> it = _entryCounters.values().iterator(); it.hasNext();)
			{
				if (it.next().lastSeenTime != thisReadClientTime)
				{
					it.remove();
					removedEntries++;
				}
			}
			_lastFullReadSrvTime = thisReadSrvTime;

			if (_textCache.size() > TEXT_CACHE_MAX_SIZE)
				_textCache.clear();
		}
		_lastReadSrvTime    = thisReadSrvTime;
		_lastReadClientTime = thisReadClientTime;

		// Keep: top ## by executions + top ## by rows
		List<StmtSum> all = new ArrayList<>(sumMap.values());
		Map<String, StmtSum> keep = new LinkedHashMap<>();

		all.sort(Comparator.comparingLong((StmtSum s) -> s.execCount).reversed());
		for (int i = 0; i < Math.min(topRows, all.size()); i++)
			keep.put(all.get(i).key, all.get(i));

		all.sort(Comparator.comparingLong((StmtSum s) -> s.rows).reversed());
		for (int i = 0; i < Math.min(topRows, all.size()); i++)
			if (all.get(i).rows > 0)
				keep.put(all.get(i).key, all.get(i));

		// Get the statement text for the groups we don't have in the cache
		// One read with an IN list (measured on 11.5 with 1900 entries in the package cache: 40 ids in one IN list 18-25 ms,
		// one MON_GET_PKG_CACHE_STMT(NULL, <executable_id>, ...) call per id 60-70 ms)
		Map<String, String> lookup = new LinkedHashMap<>(); // EXECUTABLE_ID hex -> group key
		List<byte[]> lookupIds = new ArrayList<>();
		for (StmtSum sum : keep.values())
		{
			if ( ! _textCache.containsKey(sum.key) && sum.topExecId != null )
			{
				lookup.put(hex(sum.topExecId), sum.key);
				lookupIds.add(sum.topExecId);
			}
		}

		int textLookups = lookupIds.size();
		if ( ! lookupIds.isEmpty() )
		{
			StringBuilder inList = new StringBuilder();
			for (int i = 0; i < lookupIds.size(); i++)
				inList.append(i == 0 ? "" : ", ").append("?");

			String textSql = ""
					+ "select /* " + getName() + " */ EXECUTABLE_ID, STMT_TEXT, RTRIM(PACKAGE_SCHEMA), SECTION_TYPE \n"
					+ "from table(MON_GET_PKG_CACHE_STMT(NULL, NULL, NULL, -2)) t \n"
					+ "where EXECUTABLE_ID in (" + inList + ") \n"
					;

			try (PreparedStatement pstmnt = conn.prepareStatement(textSql))
			{
				pstmnt.setQueryTimeout(getQueryTimeout());
				for (int i = 0; i < lookupIds.size(); i++)
					pstmnt.setBytes(i + 1, lookupIds.get(i));

				try (ResultSet rs = pstmnt.executeQuery())
				{
					while (rs.next())
					{
						String groupKey = lookup.get(hex(rs.getBytes(1)));
						String text     = rs.getString(2);

						if (groupKey != null && text != null)
						{
							String packageSchema = rs.getString(3);
							_textCache.putIfAbsent(groupKey, new String[] {text, packageSchema, rs.getString(4), isSystemStmt(packageSchema, text) ? "Y" : "N"});
						}
					}
				}
			}
			catch (SQLException ex)
			{
				_logger.warn(getName() + ": Problems getting the statement text for " + lookupIds.size() + " statements. Continuing without them. Caught: " + ex);
			}
		}

		// Build the ResultSet
		double intervalSec = prevReadSrvTime == null ? 0 : (thisReadClientTime - prevReadClientTime) / 1000.0;

		SimpleResultSet srs = new SimpleResultSet();
		srs.addColumn("MEMBER",          Types.SMALLINT,  0, 0);
		srs.addColumn("STMTID",          Types.VARCHAR,  64, 0);
		srs.addColumn("SEMANTIC_ENV_ID", Types.BIGINT,    0, 0);
		srs.addColumn("EXECUTABLE_ID",   Types.VARCHAR,  64, 0);
		srs.addColumn("SECTION_TYPE",    Types.VARCHAR,   1, 0);
		srs.addColumn("PACKAGE_SCHEMA",  Types.VARCHAR, 128, 0);
		srs.addColumn("IS_SYSTEM_STMT",  Types.VARCHAR,   1, 0);
		srs.addColumn("ExecCount",       Types.BIGINT,    0, 0);
		srs.addColumn("ExecPerSec",      Types.NUMERIC,  12, 1);
		srs.addColumn("RowCount",        Types.BIGINT,    0, 0);
		srs.addColumn("RowsPerSec",      Types.NUMERIC,  12, 1);
		srs.addColumn("RowsPerExec",     Types.NUMERIC,  12, 2);
		srs.addColumn("RowsReadPerExec", Types.NUMERIC,  12, 1);
		srs.addColumn("AvgElapsedMs",    Types.NUMERIC,  14, 3);
		srs.addColumn("AvgCpuMs",        Types.NUMERIC,  14, 3);
		srs.addColumn("AvgLogicalReads", Types.NUMERIC,  12, 1);
		srs.addColumn("ElapsedMs",       Types.NUMERIC,  16, 1);
		srs.addColumn("CpuMs",           Types.NUMERIC,  16, 1);
		srs.addColumn("LogicalReads",    Types.BIGINT,    0, 0);
		srs.addColumn("EstRoundTripMs",  Types.NUMERIC,  14, 1);
		srs.addColumn("ExecIdCount",     Types.INTEGER,   0, 0);
		srs.addColumn("STMT_TEXT",       Types.VARCHAR, Integer.MAX_VALUE, 0);

		for (StmtSum sum : keep.values())
		{
			String[] text = _textCache.get(sum.key);
			srs.addRow(
				sum.member,
				sum.stmtId,
				sum.semEnvId,
				sum.topExecId == null ? null : hex(sum.topExecId),
				text == null ? null : text[2],
				text == null ? null : text[1],
				text == null ? null : text[3],
				sum.execCount,
				intervalSec <= 0 ? null : num(sum.execCount / intervalSec, 1),
				sum.rows,
				intervalSec <= 0 ? null : num(sum.rows / intervalSec, 1),
				num(sum.rows         * 1.0 / sum.execCount, 2),
				num(sum.rowsRead     * 1.0 / sum.execCount, 1),
				num(sum.elapsedMs    * 1.0 / sum.execCount, 3),
				num(sum.cpuUs   / 1000.0 / sum.execCount, 3),
				num(sum.logicalReads * 1.0 / sum.execCount, 1),
				num(sum.elapsedMs    * 1.0, 1),
				num(sum.cpuUs   / 1000.0, 1),
				sum.logicalReads,
				num(sum.execCount * roundTripMs, 1),
				sum.entryCount,
				text == null ? null : text[0]
			);
		}

		if (_logger.isDebugEnabled())
			_logger.debug(getName() + ": fullRead=" + fullRead + ", entryRowsRead=" + entryRowsRead + ", newEntries=" + newEntries + ", entryCounters=" + _entryCounters.size() + ", removedEntries=" + removedEntries
					+ ", stmtIdsWithExec=" + sumMap.size() + ", keptRows=" + keep.size() + ", textLookups=" + textLookups + ", intervalSec=" + intervalSec);

		return srs;
	}

	/**
	 * Db2's own SQL (or DbxTune's own): static SQL in a package of a SYS* schema (SYSIBMINTERNAL...), or a statement where ALL tables are Db2's own
	 * (the catalog, SYSTOOLS, table functions like MON_GET_*, SYSIBM.SYSDUMMY1: see CmStatements.getTablesOfStmtText() and ObjectLookupInspectorDb2.isSystemObject()).<br>
	 * Dynamic SQL has no package schema of it's own (NULL), so Db2's own dynamic SQL (health monitor, automatic maintenance: SYSTOOLS.POLICY, SYSCAT...) is found by the tables.<br>
	 * A statement without tables (SET, VALUES, CALL) is not a system statement.
	 */
	private static boolean isSystemStmt(String packageSchema, String stmtText)
	{
		if (packageSchema != null && packageSchema.toUpperCase().startsWith("SYS"))
			return true;

		Set<String> tables = CmStatements.getTablesOfStmtText(stmtText);
		if (tables.isEmpty())
			return false;

		for (String table : tables)
		{
			String[] schemaAndTable = ObjectLookupInspectorDb2.splitSchemaAndTable(ObjectLookupInspectorDb2.ANY_SCHEMA, table);
			if ( ! ObjectLookupInspectorDb2.isSystemObject(schemaAndTable[0], schemaAndTable[1]) )
				return false;
		}
		return true;
	}

	private static BigDecimal num(double val, int scale)
	{
		if (Double.isNaN(val) || Double.isInfinite(val))
			return null;
		return new BigDecimal(val).setScale(scale, RoundingMode.HALF_EVEN);
	}

	private static final char[] HEX_DIGITS = "0123456789ABCDEF".toCharArray();

	/** EXECUTABLE_ID as hex, upper case (the same as HEX(EXECUTABLE_ID) in CmStatements). Called for every row of the read: so no String.format() per byte */
	private static String hex(byte[] bytes)
	{
		if (bytes == null)
			return null;

		char[] chars = new char[bytes.length * 2];
		for (int i = 0; i < bytes.length; i++)
		{
			chars[i * 2]     = HEX_DIGITS[(bytes[i] >> 4) & 0x0F];
			chars[i * 2 + 1] = HEX_DIGITS[ bytes[i]       & 0x0F];
		}
		return new String(chars);
	}
}
