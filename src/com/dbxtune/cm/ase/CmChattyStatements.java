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
package com.dbxtune.cm.ase;

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
import com.dbxtune.cm.CmSybMessageHandler;
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
import com.dbxtune.utils.Configuration;
import com.dbxtune.utils.Ver;

/**
 * Cheap statements that are executed very often ("chatty" statements), from the statement cache (<code>monCachedStatement</code>).
 * <p>
 * Used to find <b>N+1 queries</b>: one parent statement returns N rows, and the application then executes one cheap
 * child lookup per row (1 row per call, sub millisecond, but N round trips).
 * <p>
 * How it works (same as SQL Server's CmChattyStatements):
 * <ul>
 *   <li>Reads only key and counter columns from <code>monCachedStatement</code> for cheap statements (no SQL text, no plans).</li>
 *   <li>A full read at start and every ## minutes, in between only statements used since the previous sample (<code>LastUsedDate</code>).</li>
 *   <li>Keeps the previous counters per cache entry in memory, and calculates what happened <b>in this sample</b> (so bursts on
 *       statements that have been cached for days are caught).</li>
 *   <li>Groups the entries by <code>DBID + Hashkey</code> (the statement cache keeps one entry per login for the same SQL text),
 *       and keeps only the top ## by executions and the top ## by rows in this sample.</li>
 *   <li>SQL text (<code>show_cached_text()</code>) is fetched only for the kept rows (and cached per group).</li>
 * </ul>
 * ASE has no "rows returned" counter: the rows are estimated from <code>AvgQualifyingReadRows</code> (the average over all executions)
 * times the executions in this sample.
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
		"<p>Cheap statements from the statement cache that are executed very often.</p>" +
		"<p>Values are for <b>this sample only</b>, grouped by <code>DBID + Hashkey</code> (the statement cache has one entry per login), top ## by executions and top ## by rows.</p>" +
		"<p>Used to find <b>N+1 queries</b>: a child lookup with about 1 row per execution, executed once for every row of a parent statement.</p>" +
		"<p>Note: prepared statements (dynamic SQL, for example JDBC PreparedStatement) are only in the statement cache when <code>sp_configure 'streamlined dynamic SQL'</code> is 1.</p>" +
		"</html>";

	public static final String   GROUP_NAME       = MainFrame.TCP_GROUP_CACHE;
	public static final String   GUI_ICON_FILE    = "images/CmStmntCacheDetails.png";

	public static final long     NEED_SRV_VERSION = Ver.ver(15,7,0,130); // TotalLIO, TotalCpuTime, TotalElapsedTime (AvgQualifyingReadRows is from 15.7.0 ESD#2)
	public static final long     NEED_CE_VERSION  = 0;

	public static final String[] MON_TABLES       = new String[] {"monCachedStatement"};
	public static final String[] NEED_ROLES       = new String[] {"mon_role"};
	public static final String[] NEED_CONFIG      = new String[] {"enable monitoring=1", "enable stmt cache monitoring=1", "statement cache size"}; // NO default for 'statement cache size' configuration

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
	public static final String  TOOLTIP_sample_maxLogicalReads = "Only statements with 'AvgLIO' (average logical reads per execution) at or below this value.";
	public static final String  TOOLTIP_sample_resyncMinutes   = "Read ALL cheap statements (not only the ones used since the previous sample) every ## minutes, and forget entries that are gone from the statement cache.";
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
	protected CmSybMessageHandler createSybMessageHandler()
	{
		CmSybMessageHandler msgHandler = super.createSybMessageHandler();

		// Server Message:  Number  11096, Severity  10
		// The statement id is not valid. (show_cached_text() on an entry that was just removed from the cache)
		msgHandler.addDiscardMsgNum(11096);

		return msgHandler;
	}

	@Override
	public List<String> getPkForVersion(DbxConnection conn, DbmsVersionInfo versionInfo)
	{
		List <String> pkCols = new LinkedList<String>();

		pkCols.add("DBID");
		pkCols.add("Hashkey");

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

			mtd.addColumn(cmName, "DBName"          , "<html>Database name.</html>");
			mtd.addColumn(cmName, "DBID"            , "<html>Database ID.</html>");
			mtd.addColumn(cmName, "Hashkey"         , "<html>The hashkey over the statement's text. All cache entries with the same text (one per login) are grouped on it.</html>");
			mtd.addColumn(cmName, "ExecCount"       , "<html>Number of executions <b>in this sample</b> (UseCount).</html>");
			mtd.addColumn(cmName, "ExecPerSec"      , "<html>Executions per second in this sample.</html>");
			mtd.addColumn(cmName, "RowCount"        , "<html>Estimated number of rows <b>in this sample</b>: <code>AvgQualifyingReadRows</code> (average over all executions) * executions in this sample. <br>ASE has no 'rows returned' counter per statement.</html>");
			mtd.addColumn(cmName, "RowsPerSec"      , "<html>Estimated rows per second in this sample. <br>For a N+1 parent, this follows the child's <code>ExecPerSec</code>.</html>");
			mtd.addColumn(cmName, "RowsPerExec"     , "<html>Estimated rows per execution (<code>AvgQualifyingReadRows</code>). <br>About 1 for a N+1 child (a lookup on a key).</html>");
			mtd.addColumn(cmName, "AvgElapsedMs"    , "<html>Average elapsed time per execution in milliseconds (ASE measures in ms, so tiny statements show 0).</html>");
			mtd.addColumn(cmName, "AvgCpuMs"        , "<html>Average CPU time per execution in milliseconds.</html>");
			mtd.addColumn(cmName, "AvgLogicalReads" , "<html>Average logical reads per execution.</html>");
			mtd.addColumn(cmName, "ElapsedMs"       , "<html>Total elapsed time in milliseconds in this sample (inside ASE only).</html>");
			mtd.addColumn(cmName, "CpuMs"           , "<html>Total CPU time in milliseconds in this sample.</html>");
			mtd.addColumn(cmName, "LogicalReads"    , "<html>Total logical reads in this sample.</html>");
			mtd.addColumn(cmName, "EstRoundTripMs"  , "<html>Estimated time the <b>client</b> spent on round trips in this sample: <code>ExecCount * " + PROPKEY_roundTripMs + "</code>. <br>This time is not visible in ASE, but it is what the application pays for N+1.</html>");
			mtd.addColumn(cmName, "EntryCount"      , "<html>Number of statement cache entries (with this Hashkey) that were executed in this sample. The cache has one entry per login (and per session settings).</html>");
			mtd.addColumn(cmName, "SSQLID"          , "<html>SSQLID of the cache entry with the most executions in this sample.</html>");
			mtd.addColumn(cmName, "SqlText"         , "<html>SQL text of the cache entry with the most executions in this sample: <code>show_cached_text(SSQLID)</code>.</html>");
		}
		catch (NameNotFoundException e)
		{
			_logger.warn("Problems in cm='" + CM_NAME + "', adding addMonTableDictForVersion. Caught: " + e);
		}
	}

	@Override
	public String getSqlForVersion(DbxConnection conn, DbmsVersionInfo versionInfo)
	{
		// The SQL is executed in CounterSamplePrivate.getSample() -> createResultSet()
		return "-- reads 'master..monCachedStatement' (cheap statements), see CmChattyStatements.createResultSet()";
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

	/** Server time when the previous read STARTED (null = no read yet) */
	private Timestamp _lastReadSrvTime;

	/** Server time of the last FULL read */
	private Timestamp _lastFullReadSrvTime;

	/** Previous counters per cache entry; key = InstanceID:SSQLID */
	private Map<String, EntryCounters> _entryCounters = new HashMap<>();

	/** Cached SqlText per DBID:Hashkey (text does not change) */
	private Map<String, String> _textCache = new HashMap<>();

	private static final int TEXT_CACHE_MAX_SIZE = 10_000;

	private static class EntryCounters
	{
		long useCount;
		long metricsCount;
		long logicalReads;
		long elapsedMs;
		long cpuMs;
		long lastSeenTime; // server time in ms of the read where it was last seen
	}

	/** Summary for one DBID:Hashkey in this sample */
	private static class HashkeySum
	{
		String key;
		int    dbid;
		int    hashkey;
		String dbname;
		long   execCount;
		double rows;
		long   logicalReads;
		long   elapsedMs;
		long   cpuMs;
		int    entryCount;

		// The entry with most executions in this sample (used for SQL text)
		long   topEntryExecCount = -1;
		int    topSsqlId;
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
	 * Read the cheap statements, calculate what happened since the previous read, group by DBID + Hashkey, keep top rows,
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

		// FULL read: first time, and every ## minutes (to forget entries that has been removed from the statement cache)
		boolean fullRead = prevReadSrvTime == null
				|| _lastFullReadSrvTime == null
				|| (thisReadSrvTime.getTime() - _lastFullReadSrvTime.getTime()) >= resyncMinutes * 60_000L;

		// Subtract 5 seconds from previous read time, so we do not miss anything (an entry that is read twice has delta 0 the second time)
		String sql = ""
				+ "SELECT /* ${cmCollectorName} */ \n"
				+ "     InstanceID \n"
				+ "    ,SSQLID \n"
				+ "    ,DBID \n"
				+ "    ,DBName \n"
				+ "    ,Hashkey \n"
				+ "    ,CachedDate \n"
				+ "    ,UseCount \n"
				+ "    ,MetricsCount \n"
				+ "    ,AvgQualifyingReadRows \n"
				+ "    ,TotalLIO \n"
				+ "    ,TotalElapsedTime \n"
				+ "    ,TotalCpuTime \n"
				+ "FROM master..monCachedStatement \n"
				+ "WHERE AvgLIO <= ? \n"
				+ (fullRead ? "" : "  AND LastUsedDate >= dateadd(ss, -5, ?) \n")
				;
		sql = sql.replace("${cmCollectorName}", getName());

		long thisReadTimeMs = thisReadSrvTime.getTime();
		Map<String, HashkeySum> sumMap = new HashMap<>();
		int entryRowsRead = 0;

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

					int       instanceId     = rs.getInt      (1);
					int       ssqlId         = rs.getInt      (2);
					int       dbid           = rs.getInt      (3);
					String    dbname         = rs.getString   (4);
					int       hashkey        = rs.getInt      (5);
					Timestamp cachedDate     = rs.getTimestamp(6);
					long      useCount       = rs.getLong     (7);
					long      metricsCount   = rs.getLong     (8);
					long      avgQualRows    = rs.getLong     (9);
					long      logicalReads   = rs.getLong     (10);
					long      elapsedMs      = rs.getLong     (11);
					long      cpuMs          = rs.getLong     (12);

					String entryKey = instanceId + ":" + ssqlId;

					// Calculate what happened since previous read
					EntryCounters prev = _entryCounters.get(entryKey);
					boolean createdSincePrevRead = prevReadSrvTime != null && cachedDate != null && ! cachedDate.before(prevReadSrvTime);

					long dExec = 0, dMetrics = 0, dReads = 0, dElapsed = 0, dCpu = 0;
					if (prev != null && useCount >= prev.useCount)
					{
						dExec    = useCount     - prev.useCount;
						dMetrics = metricsCount - prev.metricsCount;
						dReads   = logicalReads - prev.logicalReads;
						dElapsed = elapsedMs    - prev.elapsedMs;
						dCpu     = cpuMs        - prev.cpuMs;
					}
					else if (createdSincePrevRead)
					{
						// New entry since previous read: the counters are the delta
						dExec    = useCount;
						dMetrics = metricsCount;
						dReads   = logicalReads;
						dElapsed = elapsedMs;
						dCpu     = cpuMs;
					}
					// else: first time we see this (old) entry, just remember the counters

					if (prev == null)
					{
						prev = new EntryCounters();
						_entryCounters.put(entryKey, prev);
					}
					prev.useCount     = useCount;
					prev.metricsCount = metricsCount;
					prev.logicalReads = logicalReads;
					prev.elapsedMs    = elapsedMs;
					prev.cpuMs        = cpuMs;
					prev.lastSeenTime = thisReadTimeMs;

					if (dExec <= 0)
						continue;

					// Group on DBID + Hashkey (the statement cache has one entry per login for the same text)
					String groupKey = dbid + ":" + hashkey;

					HashkeySum sum = sumMap.get(groupKey);
					if (sum == null)
					{
						sum = new HashkeySum();
						sum.key     = groupKey;
						sum.dbid    = dbid;
						sum.hashkey = hashkey;
						sum.dbname  = dbname;
						sumMap.put(groupKey, sum);
					}
					sum.execCount    += dExec;
					// No "rows returned" in ASE: average qualifying rows (over all executions) * executions with metrics in this sample
					sum.rows         += Math.max(0, dMetrics) * (double) avgQualRows;
					sum.logicalReads += dReads;
					sum.elapsedMs    += dElapsed;
					sum.cpuMs        += dCpu;
					sum.entryCount++;

					if (dExec > sum.topEntryExecCount)
					{
						sum.topEntryExecCount = dExec;
						sum.topSsqlId         = ssqlId;
					}
				}
			}
		}

		// After a FULL read: forget entries that are no longer in the statement cache (or no longer cheap)
		int removedEntries = 0;
		if (fullRead)
		{
			for (Iterator<EntryCounters> it = _entryCounters.values().iterator(); it.hasNext();)
			{
				if (it.next().lastSeenTime != thisReadTimeMs)
				{
					it.remove();
					removedEntries++;
				}
			}
			_lastFullReadSrvTime = thisReadSrvTime;

			if (_textCache.size() > TEXT_CACHE_MAX_SIZE)
				_textCache.clear();
		}
		_lastReadSrvTime = thisReadSrvTime;

		// Keep: top ## by executions + top ## by rows
		List<HashkeySum> all = new ArrayList<>(sumMap.values());
		Map<String, HashkeySum> keep = new LinkedHashMap<>();

		all.sort(Comparator.comparingLong((HashkeySum s) -> s.execCount).reversed());
		for (int i = 0; i < Math.min(topRows, all.size()); i++)
			keep.put(all.get(i).key, all.get(i));

		all.sort(Comparator.comparingDouble((HashkeySum s) -> s.rows).reversed());
		for (int i = 0; i < Math.min(topRows, all.size()); i++)
			if (all.get(i).rows > 0)
				keep.put(all.get(i).key, all.get(i));

		// Get SQL Text for the ones we don't have in the cache
		List<HashkeySum> lookup = new ArrayList<>();
		for (HashkeySum sum : keep.values())
			if ( ! _textCache.containsKey(sum.key) )
				lookup.add(sum);

		if ( ! lookup.isEmpty() )
		{
			// One SELECT per entry in one batch (show_cached_text() takes a SSQLID, no need to read monCachedStatement again)
			StringBuilder textSql = new StringBuilder();
			for (int i = 0; i < lookup.size(); i++)
				textSql.append("select id = ").append(i).append(", SqlText = convert(text, show_cached_text(").append(lookup.get(i).topSsqlId).append(")) /* ").append(getName()).append(" */ \n");

			try (Statement stmnt = conn.createStatement())
			{
				stmnt.setQueryTimeout(getQueryTimeout());
				boolean hasRs = stmnt.execute(textSql.toString());
				while (true)
				{
					if (hasRs)
					{
						try (ResultSet rs = stmnt.getResultSet())
						{
							while (rs.next())
							{
								int    id      = rs.getInt   (1);
								String sqlText = rs.getString(2);

								if (id >= 0 && id < lookup.size() && sqlText != null)
									_textCache.put(lookup.get(id).key, sqlText);
							}
						}
					}
					else if (stmnt.getUpdateCount() == -1)
						break;

					hasRs = stmnt.getMoreResults();
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
		srs.addColumn("DBName",          Types.VARCHAR,  30, 0);
		srs.addColumn("DBID",            Types.INTEGER,   0, 0);
		srs.addColumn("Hashkey",         Types.INTEGER,   0, 0);
		srs.addColumn("ExecCount",       Types.BIGINT,    0, 0);
		srs.addColumn("ExecPerSec",      Types.NUMERIC,  12, 1);
		srs.addColumn("RowCount",        Types.BIGINT,    0, 0);
		srs.addColumn("RowsPerSec",      Types.NUMERIC,  12, 1);
		srs.addColumn("RowsPerExec",     Types.NUMERIC,  12, 2);
		srs.addColumn("AvgElapsedMs",    Types.NUMERIC,  14, 2);
		srs.addColumn("AvgCpuMs",        Types.NUMERIC,  14, 2);
		srs.addColumn("AvgLogicalReads", Types.NUMERIC,  12, 1);
		srs.addColumn("ElapsedMs",       Types.BIGINT,    0, 0);
		srs.addColumn("CpuMs",           Types.BIGINT,    0, 0);
		srs.addColumn("LogicalReads",    Types.BIGINT,    0, 0);
		srs.addColumn("EstRoundTripMs",  Types.NUMERIC,  14, 1);
		srs.addColumn("EntryCount",      Types.INTEGER,   0, 0);
		srs.addColumn("SSQLID",          Types.INTEGER,   0, 0);
		srs.addColumn("SqlText",         Types.VARCHAR, Integer.MAX_VALUE, 0);

		for (HashkeySum sum : keep.values())
		{
			long rows = Math.round(sum.rows);

			srs.addRow(
				sum.dbname,
				sum.dbid,
				sum.hashkey,
				sum.execCount,
				intervalSec <= 0 ? null : num(sum.execCount / intervalSec, 1),
				rows,
				intervalSec <= 0 ? null : num(rows / intervalSec, 1),
				num(rows              * 1.0 / sum.execCount, 2),
				num(sum.elapsedMs     * 1.0 / sum.execCount, 2),
				num(sum.cpuMs         * 1.0 / sum.execCount, 2),
				num(sum.logicalReads  * 1.0 / sum.execCount, 1),
				sum.elapsedMs,
				sum.cpuMs,
				sum.logicalReads,
				num(sum.execCount * roundTripMs, 1),
				sum.entryCount,
				sum.topSsqlId,
				_textCache.get(sum.key)
			);
		}

		if (_logger.isDebugEnabled())
			_logger.debug(getName() + ": fullRead=" + fullRead + ", entryRowsRead=" + entryRowsRead + ", entryCounters=" + _entryCounters.size() + ", removedEntries=" + removedEntries
					+ ", hashkeysWithExec=" + sumMap.size() + ", keptRows=" + keep.size() + ", textLookups=" + lookup.size() + ", intervalSec=" + intervalSec);

		return srs;
	}

	private static BigDecimal num(double val, int scale)
	{
		if (Double.isNaN(val) || Double.isInfinite(val))
			return null;
		return new BigDecimal(val).setScale(scale, RoundingMode.HALF_EVEN);
	}
}
