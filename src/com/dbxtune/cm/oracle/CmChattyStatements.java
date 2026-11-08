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
package com.dbxtune.cm.oracle;

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
import com.dbxtune.pcs.PcsColumnOptions;
import com.dbxtune.pcs.PcsColumnOptions.ColumnType;
import com.dbxtune.sql.conn.DbxConnection;
import com.dbxtune.sql.conn.info.DbmsVersionInfo;
import com.dbxtune.sql.conn.info.DbmsVersionInfoOracle;
import com.dbxtune.utils.Configuration;
import com.dbxtune.utils.Ver;

/**
 * Cheap statements that are executed very often ("chatty" statements), the ones that {@link CmSqlStats} filters away
 * (it only keeps <code>BUFFER_GETS &gt; 100</code>, the top 1000 by buffer gets, every 5 minutes).
 * <p>
 * Used to find <b>N+1 queries</b>: one parent statement returns N rows, and the application then executes one cheap
 * child lookup per row (1 row per call, sub millisecond, but N round trips).
 * <p>
 * How it works (same as SQL Server's and ASE's CmChattyStatements):
 * <ul>
 *   <li>Reads only key and counter columns from <code>V$SQLSTATS</code> for cheap statements (no SQL text).</li>
 *   <li>A full read at start and every ## minutes, in between only statements active since the previous sample (<code>LAST_ACTIVE_TIME</code>).</li>
 *   <li>Keeps the previous counters per SQL_ID + plan in memory, and calculates what happened <b>in this sample</b> (so bursts on
 *       statements that have been in the shared pool for days are caught).</li>
 *   <li>Groups the statements by <code>FORCE_MATCHING_SIGNATURE</code> (so literal SQL is folded together, as SQL Server's query_hash),
 *       and keeps only the top ## by executions and the top ## by rows in this sample.</li>
 *   <li>SQL text is fetched from <code>V$SQL</code> only for the kept rows (and cached per SQL_ID).
 *       <code>V$SQLSTATS.SQL_FULLTEXT</code> is only the first 1000 characters.</li>
 * </ul>
 * Only V$ views that are part of the database (no Diagnostics/Tuning Pack: no ASH, AWR or DBA_HIST).
 * <p>
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
		"<p>Cheap statements that are executed very often (the ones the 'SQL Stats' tab filters away).</p>" +
		"<p>Values are for <b>this sample only</b>, grouped by <code>FORCE_MATCHING_SIGNATURE</code> (literal SQL is folded together), top ## by executions and top ## by rows.</p>" +
		"<p>Used to find <b>N+1 queries</b>: a child lookup with about 1 row per execution, executed once for every row of a parent statement.</p>" +
		"</html>";

	public static final String   GROUP_NAME       = MainFrame.TCP_GROUP_OBJECT_ACCESS;
	public static final String   GUI_ICON_FILE    = "images/CmSqlStats.png";

	public static final long     NEED_SRV_VERSION = 0;
	public static final long     NEED_CE_VERSION  = 0;

	public static final String[] MON_TABLES       = new String[] {"V$SQLSTATS"};
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

	public static final String  TOOLTIP_sample_topRows         = "Keep the top ## statements by executions in this sample, and the top ## by rows in this sample.";
	public static final String  TOOLTIP_sample_maxLogicalReads = "Only statements with at most ## BUFFER_GETS (logical reads) per execution (average since the statement was loaded).";
	public static final String  TOOLTIP_sample_resyncMinutes   = "Read ALL cheap statements (not only the ones active since the previous sample) every ## minutes, and forget statements that are gone from V$SQLSTATS.";
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

	/** If we can't read the views: disable the CM, and tell what to do about it */
	@Override
	public boolean checkDependsOnOther(DbxConnection conn)
	{
		return OracleCmUtils.checkViewsAreReadable(this, conn, "V$SQLSTATS", "V$SQL");
	}

	@Override
	public List<String> getPkForVersion(DbxConnection conn, DbmsVersionInfo versionInfo)
	{
		List <String> pkCols = new LinkedList<String>();

		pkCols.add("CON_ID");
		pkCols.add("SIGNATURE");

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

			mtd.addColumn(cmName, "CON_ID"          , "<html>Container ID. 0 = not a Container Database (or the version is below 12.1).</html>");
			mtd.addColumn(cmName, "CON_NAME"        , "<html>Name of the Container (PDB). NULL if this is not a Container Database.</html>");
			mtd.addColumn(cmName, "SIGNATURE"       , "<html>What the statements are grouped on: <code>FORCE_MATCHING_SIGNATURE</code> (the same statement with different literal values has the same signature). <br>If the signature is 0 (for example PL/SQL blocks) it is the SQL_ID.</html>");
			mtd.addColumn(cmName, "SQL_ID"          , "<html>SQL_ID of the statement with the most executions in this sample (with this SIGNATURE).</html>");
			mtd.addColumn(cmName, "PARSING_SCHEMA"  , "<html>The schema the statement was parsed in (V$SQL.PARSING_SCHEMA_NAME). <br>SYS is mostly Oracle's own recursive SQL (dictionary lookups), which the Daily Summary Report does not take as N+1.</html>");
			mtd.addColumn(cmName, "ExecCount"       ,"<html>Number of executions <b>in this sample</b> (V$SQLSTATS.EXECUTIONS).</html>");
			mtd.addColumn(cmName, "ExecPerSec"      , "<html>Executions per second in this sample.</html>");
			mtd.addColumn(cmName, "RowCount"        , "<html>Number of rows <b>in this sample</b> (V$SQLSTATS.ROWS_PROCESSED: rows fetched by a SELECT, rows changed by DML).</html>");
			mtd.addColumn(cmName, "RowsPerSec"      , "<html>Rows per second in this sample. <br>For a N+1 parent, this follows the child's <code>ExecPerSec</code>.</html>");
			mtd.addColumn(cmName, "RowsPerExec"     , "<html>Rows per execution in this sample. <br>About 1 for a N+1 child (a lookup on a key).</html>");
			mtd.addColumn(cmName, "AvgElapsedMs"    , "<html>Average elapsed time per execution in milliseconds.</html>");
			mtd.addColumn(cmName, "AvgCpuMs"        , "<html>Average CPU time per execution in milliseconds.</html>");
			mtd.addColumn(cmName, "AvgLogicalReads" , "<html>Average logical reads (BUFFER_GETS) per execution.</html>");
			mtd.addColumn(cmName, "ElapsedMs"       , "<html>Total elapsed time in milliseconds in this sample (inside Oracle only).</html>");
			mtd.addColumn(cmName, "CpuMs"           , "<html>Total CPU time in milliseconds in this sample.</html>");
			mtd.addColumn(cmName, "LogicalReads"    , "<html>Total logical reads (BUFFER_GETS) in this sample.</html>");
			mtd.addColumn(cmName, "EstRoundTripMs"  , "<html>Estimated time the <b>client</b> spent on round trips in this sample: <code>ExecCount * " + PROPKEY_roundTripMs + "</code>. <br>This time is not visible in Oracle, but it is what the application pays for N+1.</html>");
			mtd.addColumn(cmName, "SqlIdCount"      , "<html>Number of SQL_ID + plan rows (with this SIGNATURE) that were executed in this sample. Many of them = literal SQL (no bind variables).</html>");
			mtd.addColumn(cmName, "SqlText"         , "<html>SQL text of <code>SQL_ID</code>, from V$SQL.SQL_FULLTEXT (V$SQLSTATS only has the first 1000 characters).</html>");
		}
		catch (NameNotFoundException e)
		{
			_logger.warn("Problems in cm='" + CM_NAME + "', adding addMonTableDictForVersion. Caught: " + e);
		}
	}

	/** 12.1 and above has CON_ID in V$SQLSTATS */
	private boolean _hasConId = false;

	/** A Container Database: get the container names from V$CONTAINERS */
	private boolean _isCdb    = false;

	@Override
	public String getSqlForVersion(DbxConnection conn, DbmsVersionInfo versionInfo)
	{
		_hasConId = versionInfo.getLongVersion() >= Ver.ver(12,1);
		_isCdb    = _hasConId && versionInfo instanceof DbmsVersionInfoOracle && ((DbmsVersionInfoOracle) versionInfo).isCdb();

		// The SQL is executed in CounterSamplePrivate.getSample() -> createResultSet()
		return "-- reads 'V$SQLSTATS' (cheap statements), see CmChattyStatements.createResultSet()";
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

	/** Server time (SYSDATE, the same clock as LAST_ACTIVE_TIME) when the previous read STARTED (null = no read yet) */
	private Timestamp _lastReadSrvTime;

	/** Client time in ms when the previous read STARTED (for the interval: SYSDATE has whole seconds only) */
	private long _lastReadClientTime;

	/** Server time of the last FULL read */
	private Timestamp _lastFullReadSrvTime;

	/** Previous counters per statement; key = CON_ID:SQL_ID:PLAN_HASH_VALUE */
	private Map<String, EntryCounters> _entryCounters = new HashMap<>();

	/** Cached [SqlText, PARSING_SCHEMA_NAME] per SQL_ID (they do not change) */
	private Map<String, String[]> _textCache = new HashMap<>();

	/** Container names: CON_ID -> NAME (read at every FULL read) */
	private Map<Integer, String> _conNames = new HashMap<>();

	private static final int TEXT_CACHE_MAX_SIZE = 10_000;

	private static class EntryCounters
	{
		long executions;
		long rows;
		long bufferGets;
		long elapsedUs;
		long cpuUs;
		long lastSeenTime; // client time in ms of the read where it was last seen
	}

	/** Summary for one CON_ID:SIGNATURE in this sample */
	private static class SignatureSum
	{
		String key;
		int    conId;
		String signature;
		long   execCount;
		long   rows;
		long   bufferGets;
		long   elapsedUs;
		long   cpuUs;
		int    entryCount;

		// The SQL_ID with most executions in this sample (used for SQL text)
		long   topEntryExecCount = -1;
		String topSqlId;
	}

	@Override
	public void close()
	{
		_lastReadSrvTime     = null;
		_lastFullReadSrvTime = null;
		_entryCounters.clear();
		_textCache.clear();
		_conNames.clear();

		super.close();
	}

	/**
	 * Read the cheap statements, calculate what happened since the previous read, group by CON_ID + FORCE_MATCHING_SIGNATURE, keep top rows,
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

		// LAST_ACTIVE_TIME is a DATE in the server's clock: so compare it with SYSDATE (not the collector's clock)
		Timestamp thisReadSrvTime = null;
		try (Statement stmnt = conn.createStatement(); ResultSet rs = stmnt.executeQuery("select SYSDATE from dual"))
		{
			while (rs.next())
				thisReadSrvTime = rs.getTimestamp(1);
		}
		if (thisReadSrvTime == null)
			thisReadSrvTime = new Timestamp(thisReadClientTime);

		Timestamp prevReadSrvTime    = _lastReadSrvTime;
		long      prevReadClientTime = _lastReadClientTime;

		// FULL read: first time, and every ## minutes (to forget statements that are gone from V$SQLSTATS)
		boolean fullRead = prevReadSrvTime == null
				|| _lastFullReadSrvTime == null
				|| (thisReadSrvTime.getTime() - _lastFullReadSrvTime.getTime()) >= resyncMinutes * 60_000L;

		// Container names
		if (fullRead && _isCdb)
		{
			_conNames.clear();
			try (Statement stmnt = conn.createStatement(); ResultSet rs = stmnt.executeQuery("select CON_ID, NAME from V$CONTAINERS"))
			{
				while (rs.next())
					_conNames.put(rs.getInt(1), rs.getString(2));
			}
			catch (SQLException ex)
			{
				_logger.warn(getName() + ": Problems reading container names from V$CONTAINERS. Continuing without them. Caught: " + ex);
			}
		}

		// Cheap statements: BUFFER_GETS per execution (average since loaded) at or below the limit
		// LAST_ACTIVE_TIME: subtract 5 seconds from previous read time, so we do not miss anything (a statement that is read twice has delta 0 the second time)
		// FORCE_MATCHING_SIGNATURE is a NUMBER up to 2^64 (bigger than a Java long): read it as a string
		String sql = ""
				+ "SELECT /* ${cmCollectorName} */ \n"
				+ "     " + (_hasConId ? "s.CON_ID" : "0") + " \n"
				+ "    ,s.SQL_ID \n"
				+ "    ,s.PLAN_HASH_VALUE \n"
				+ "    ,TO_CHAR(s.FORCE_MATCHING_SIGNATURE) \n"
				+ "    ,s.EXECUTIONS \n"
				+ "    ,s.ROWS_PROCESSED \n"
				+ "    ,s.BUFFER_GETS \n"
				+ "    ,s.ELAPSED_TIME \n"
				+ "    ,s.CPU_TIME \n"
				+ "FROM V$SQLSTATS s \n"
				+ "WHERE s.EXECUTIONS > 0 \n"
				+ "  AND s.BUFFER_GETS <= ? * s.EXECUTIONS \n"
				+ (fullRead ? "" : "  AND s.LAST_ACTIVE_TIME >= CAST(? AS DATE) - INTERVAL '5' SECOND \n")
				;
		sql = sql.replace("${cmCollectorName}", getName());

		Map<String, SignatureSum> sumMap = new HashMap<>();
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

					int    conId          = rs.getInt   (1);
					String sqlId          = rs.getString(2);
					long   planHashValue  = rs.getLong  (3);
					String forceSignature = rs.getString(4);
					long   executions     = rs.getLong  (5);
					long   rowsProcessed  = rs.getLong  (6);
					long   bufferGets     = rs.getLong  (7);
					long   elapsedUs      = rs.getLong  (8);
					long   cpuUs          = rs.getLong  (9);

					String entryKey = conId + ":" + sqlId + ":" + planHashValue;

					// Calculate what happened since previous read
					EntryCounters prev = _entryCounters.get(entryKey);

					long dExec = 0, dRows = 0, dGets = 0, dElapsed = 0, dCpu = 0;
					if (prev != null && executions >= prev.executions)
					{
						dExec    = executions    - prev.executions;
						dRows    = rowsProcessed - prev.rows;
						dGets    = bufferGets    - prev.bufferGets;
						dElapsed = elapsedUs     - prev.elapsedUs;
						dCpu     = cpuUs         - prev.cpuUs;
					}
					else if (prev == null && ! fullRead)
					{
						// Not seen before, but active since the previous read: a new statement (the previous FULL read saw all cheap statements)
						// So the counters are the delta.
						// Note: a statement that was NOT cheap before, and now is, will also end up here (all it's history in one sample). Should be rare.
						dExec    = executions;
						dRows    = rowsProcessed;
						dGets    = bufferGets;
						dElapsed = elapsedUs;
						dCpu     = cpuUs;
						newEntries++;
					}
					// else: first time we see this (old) statement, or the counters started over: just remember the counters

					if (prev == null)
					{
						prev = new EntryCounters();
						_entryCounters.put(entryKey, prev);
					}
					prev.executions   = executions;
					prev.rows         = rowsProcessed;
					prev.bufferGets   = bufferGets;
					prev.elapsedUs    = elapsedUs;
					prev.cpuUs        = cpuUs;
					prev.lastSeenTime = thisReadClientTime;

					if (dExec <= 0)
						continue;

					// Group on FORCE_MATCHING_SIGNATURE (the same statement with different literals), if it's 0 (PL/SQL blocks...) use the SQL_ID
					String signature = (forceSignature == null || "0".equals(forceSignature)) ? sqlId : forceSignature;
					String groupKey  = conId + ":" + signature;

					SignatureSum sum = sumMap.get(groupKey);
					if (sum == null)
					{
						sum = new SignatureSum();
						sum.key       = groupKey;
						sum.conId     = conId;
						sum.signature = signature;
						sumMap.put(groupKey, sum);
					}
					sum.execCount  += dExec;
					sum.rows       += Math.max(0, dRows);
					sum.bufferGets += Math.max(0, dGets);
					sum.elapsedUs  += Math.max(0, dElapsed);
					sum.cpuUs      += Math.max(0, dCpu);
					sum.entryCount++;

					if (dExec > sum.topEntryExecCount)
					{
						sum.topEntryExecCount = dExec;
						sum.topSqlId          = sqlId;
					}
				}
			}
		}

		// After a FULL read: forget statements that are no longer in V$SQLSTATS (or no longer cheap)
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
		List<SignatureSum> all = new ArrayList<>(sumMap.values());
		Map<String, SignatureSum> keep = new LinkedHashMap<>();

		all.sort(Comparator.comparingLong((SignatureSum s) -> s.execCount).reversed());
		for (int i = 0; i < Math.min(topRows, all.size()); i++)
			keep.put(all.get(i).key, all.get(i));

		all.sort(Comparator.comparingLong((SignatureSum s) -> s.rows).reversed());
		for (int i = 0; i < Math.min(topRows, all.size()); i++)
			if (all.get(i).rows > 0)
				keep.put(all.get(i).key, all.get(i));

		// Get SQL Text for the ones we don't have in the cache
		List<String> lookup = new ArrayList<>();
		for (SignatureSum sum : keep.values())
			if ( ! _textCache.containsKey(sum.topSqlId) && ! lookup.contains(sum.topSqlId) )
				lookup.add(sum.topSqlId);

		int textLookups = lookup.size();

		// First: V$SQL.SQL_FULLTEXT (the whole text), one child per SQL_ID
		// Then, only for the ones that are no longer in V$SQL: V$SQLSTATS.SQL_TEXT (first 1000 characters)
		// (Measured on 23ai: one query with a UNION ALL + NOT EXISTS for the fallback took twice the CPU of the V$SQL part alone)
		for (int pass = 0; pass < 2 && ! lookup.isEmpty(); pass++)
		{
			StringBuilder inList = new StringBuilder();
			for (int i = 0; i < lookup.size(); i++)
				inList.append(i == 0 ? "" : ", ").append("?");

			String textSql = pass == 0
					? "SELECT /* " + getName() + " */ q.SQL_ID, q.SQL_FULLTEXT, q.PARSING_SCHEMA_NAME \n"
					+ "FROM V$SQL q \n"
					+ "WHERE q.SQL_ID IN (" + inList + ") \n"
					+ "  AND q.CHILD_NUMBER = (SELECT MIN(c.CHILD_NUMBER) FROM V$SQL c WHERE c.SQL_ID = q.SQL_ID) \n"

					: "SELECT /* " + getName() + " */ s.SQL_ID, s.SQL_TEXT, CAST(NULL AS VARCHAR2(128)) \n"
					+ "FROM V$SQLSTATS s \n"
					+ "WHERE s.SQL_ID IN (" + inList + ") \n"
					;

			try (PreparedStatement pstmnt = conn.prepareStatement(textSql))
			{
				pstmnt.setQueryTimeout(getQueryTimeout());
				for (int i = 0; i < lookup.size(); i++)
					pstmnt.setString(i + 1, lookup.get(i));

				try (ResultSet rs = pstmnt.executeQuery())
				{
					while (rs.next())
					{
						String sqlId   = rs.getString(1);
						String sqlText = rs.getString(2);
						String schema  = rs.getString(3);

						if (sqlId != null && sqlText != null)
							_textCache.putIfAbsent(sqlId, new String[] {sqlText, schema});
					}
				}
			}
			catch (SQLException ex)
			{
				_logger.warn(getName() + ": Problems getting SQL Text for " + lookup.size() + " statements. Continuing without them. Caught: " + ex);
			}

			// Only the ones we did not find goes to the next pass
			lookup.removeIf(_textCache::containsKey);
		}

		// Build the ResultSet
		double intervalSec = prevReadSrvTime == null ? 0 : (thisReadClientTime - prevReadClientTime) / 1000.0;

		SimpleResultSet srs = new SimpleResultSet();
		srs.addColumn("CON_ID",          Types.INTEGER,   0, 0);
		srs.addColumn("CON_NAME",        Types.VARCHAR, 128, 0);
		srs.addColumn("SIGNATURE",       Types.VARCHAR,  30, 0);
		srs.addColumn("SQL_ID",          Types.VARCHAR,  13, 0);
		srs.addColumn("PARSING_SCHEMA",  Types.VARCHAR, 128, 0);
		srs.addColumn("ExecCount",       Types.BIGINT,    0, 0);
		srs.addColumn("ExecPerSec",      Types.NUMERIC,  12, 1);
		srs.addColumn("RowCount",        Types.BIGINT,    0, 0);
		srs.addColumn("RowsPerSec",      Types.NUMERIC,  12, 1);
		srs.addColumn("RowsPerExec",     Types.NUMERIC,  12, 2);
		srs.addColumn("AvgElapsedMs",    Types.NUMERIC,  14, 3);
		srs.addColumn("AvgCpuMs",        Types.NUMERIC,  14, 3);
		srs.addColumn("AvgLogicalReads", Types.NUMERIC,  12, 1);
		srs.addColumn("ElapsedMs",       Types.NUMERIC,  16, 1);
		srs.addColumn("CpuMs",           Types.NUMERIC,  16, 1);
		srs.addColumn("LogicalReads",    Types.BIGINT,    0, 0);
		srs.addColumn("EstRoundTripMs",  Types.NUMERIC,  14, 1);
		srs.addColumn("SqlIdCount",      Types.INTEGER,   0, 0);
		srs.addColumn("SqlText",         Types.VARCHAR, Integer.MAX_VALUE, 0);

		for (SignatureSum sum : keep.values())
		{
			srs.addRow(
				sum.conId,
				_conNames.get(sum.conId),
				sum.signature,
				sum.topSqlId,
				_textCache.containsKey(sum.topSqlId) ? _textCache.get(sum.topSqlId)[1] : null,
				sum.execCount,
				intervalSec <= 0 ? null : num(sum.execCount / intervalSec, 1),
				sum.rows,
				intervalSec <= 0 ? null : num(sum.rows / intervalSec, 1),
				num(sum.rows        * 1.0 / sum.execCount, 2),
				num(sum.elapsedUs   / 1000.0 / sum.execCount, 3),
				num(sum.cpuUs       / 1000.0 / sum.execCount, 3),
				num(sum.bufferGets  * 1.0 / sum.execCount, 1),
				num(sum.elapsedUs   / 1000.0, 1),
				num(sum.cpuUs       / 1000.0, 1),
				sum.bufferGets,
				num(sum.execCount * roundTripMs, 1),
				sum.entryCount,
				_textCache.containsKey(sum.topSqlId) ? _textCache.get(sum.topSqlId)[0] : null
			);
		}

		if (_logger.isDebugEnabled())
			_logger.debug(getName() + ": fullRead=" + fullRead + ", entryRowsRead=" + entryRowsRead + ", newEntries=" + newEntries + ", entryCounters=" + _entryCounters.size() + ", removedEntries=" + removedEntries
					+ ", signaturesWithExec=" + sumMap.size() + ", keptRows=" + keep.size() + ", textLookups=" + textLookups + ", textNotFound=" + lookup.size() +", intervalSec=" + intervalSec);

		return srs;
	}

	private static BigDecimal num(double val, int scale)
	{
		if (Double.isNaN(val) || Double.isInfinite(val))
			return null;
		return new BigDecimal(val).setScale(scale, RoundingMode.HALF_EVEN);
	}
}
