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
import com.dbxtune.pcs.inspection.ObjectLookupInspectorMySql;
import com.dbxtune.sql.conn.DbxConnection;
import com.dbxtune.sql.conn.info.DbmsVersionInfo;
import com.dbxtune.utils.Configuration;
import com.dbxtune.utils.StringUtil;

/**
 * Cheap statements that are executed very often ("chatty" statements), the ones that {@link CmStatements} loses
 * (it only keeps the top 1000 by SUM_TIMER_WAIT since the server started).
 * <p>
 * Used to find <b>N+1 queries</b>: one parent statement returns N rows, and the application then executes one cheap
 * child lookup per row (1 row per call, sub millisecond, but N round trips).
 * <p>
 * How it works (same as SQL Server's, ASE's, Oracle's and Db2's CmChattyStatements):
 * <ul>
 *   <li>Reads only key and counter columns from <code>performance_schema.events_statements_summary_by_digest</code> for cheap statements (no statement text).</li>
 *   <li>A full read at start and every ## minutes, in between only statements executed since the previous sample (<code>LAST_SEEN</code>).</li>
 *   <li>Keeps the previous counters per <code>SCHEMA_NAME + DIGEST</code> in memory, and calculates what happened <b>in this sample</b>
 *       (so bursts on statements that have been in the digest table for days are caught).
 *       A digest is already the statement with the literals replaced by '?' (the same as SQL Server's query_hash), so there is nothing more to group.</li>
 *   <li>Keeps only the top ## by executions and the top ## by rows sent in this sample.</li>
 *   <li>The statement text (DIGEST_TEXT, and QUERY_SAMPLE_TEXT) is fetched only for the kept rows (and cached).</li>
 * </ul>
 * The values in the rows are already "per sample", so there are no diff columns (only the ABS table is saved).
 * <p>
 * The digest table is never truncated by DbxTune (TRUNCATE would reset the counters for every other tool too).
 * When it is full (performance_schema_digests_size), new statements are counted in the row with a NULL DIGEST: that row is not used here (it is many statements).
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
		"<p>Cheap statements that are executed very often (the ones the 'Statements' tab loses: it keeps the top 1000 by time).</p>" +
		"<p>Values are for <b>this sample only</b>, per schema and digest (literal SQL is folded together), top ## by executions and top ## by rows sent.</p>" +
		"<p>Used to find <b>N+1 queries</b>: a child lookup with about 1 row per execution, executed once for every row of a parent statement.</p>" +
		"<p><b>Note</b>: a collector configuration that lists its Counter Models (like <code>mysql.GENERIC.conf</code> for DbxCentral) must have this Counter Model in it, otherwise it is not sampled.</p>" +
		"</html>";

	public static final String   GROUP_NAME       = MainFrame.TCP_GROUP_OBJECT_ACCESS;
	public static final String   GUI_ICON_FILE    = "images/CmStatements.png";

	public static final long     NEED_SRV_VERSION = 0;
	public static final long     NEED_CE_VERSION  = 0;

	public static final String[] MON_TABLES       = new String[] {"events_statements_summary_by_digest"};
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

	public static final String  PROPKEY_sample_maxRowsExamined = PROP_PREFIX + ".sample.maxRowsExamined";
	public static final int     DEFAULT_sample_maxRowsExamined = 100;

	public static final String  PROPKEY_sample_resyncMinutes   = PROP_PREFIX + ".sample.resyncMinutes";
	public static final int     DEFAULT_sample_resyncMinutes   = 60;

	public static final String  PROPKEY_roundTripMs            = PROP_PREFIX + ".roundTripMs";
	public static final double  DEFAULT_roundTripMs            = 0.3;

	public static final String  TOOLTIP_sample_topRows         = "Keep the top ## statements by executions in this sample, and the top ## by rows sent in this sample.";
	public static final String  TOOLTIP_sample_maxRowsExamined = "Only statements that examine at most ## rows per execution (average since the statement came into the digest table).";
	public static final String  TOOLTIP_sample_resyncMinutes   = "Read ALL cheap statements (not only the ones executed since the previous sample) every ## minutes, and forget statements that are gone from the digest table.";
	public static final String  TOOLTIP_roundTripMs            = "Estimated cost in milliseconds of one client/server round trip, used for column 'EstRoundTripMs' (executions * this value).";

	@Override
	protected void registerDefaultValues()
	{
		super.registerDefaultValues();

		Configuration.registerDefaultValue(PROPKEY_sample_topRows,         DEFAULT_sample_topRows);
		Configuration.registerDefaultValue(PROPKEY_sample_maxRowsExamined, DEFAULT_sample_maxRowsExamined);
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
		list.add(new CmSettingsHelper("Max Rows Examined",           PROPKEY_sample_maxRowsExamined, Integer.class, conf.getIntProperty   (PROPKEY_sample_maxRowsExamined, DEFAULT_sample_maxRowsExamined), DEFAULT_sample_maxRowsExamined, TOOLTIP_sample_maxRowsExamined));
		list.add(new CmSettingsHelper("Full Read Every ## Minutes",  PROPKEY_sample_resyncMinutes  , Integer.class, conf.getIntProperty   (PROPKEY_sample_resyncMinutes  , DEFAULT_sample_resyncMinutes  ), DEFAULT_sample_resyncMinutes  , TOOLTIP_sample_resyncMinutes  ));
		list.add(new CmSettingsHelper("Round Trip Cost in ms",       PROPKEY_roundTripMs           , Double .class, conf.getDoubleProperty(PROPKEY_roundTripMs           , DEFAULT_roundTripMs           ), DEFAULT_roundTripMs           , TOOLTIP_roundTripMs           ));

		return list;
	}

	@Override
	public String[] getDependsOnConfigForVersion(DbxConnection conn, DbmsVersionInfo versionInfo)
	{
		return NEED_CONFIG;
	}

	/** If we can't read the view: disable the CM, and tell what to do about it */
	@Override
	public boolean checkDependsOnOther(DbxConnection conn)
	{
		if (MySqlCmUtils.disableOnMariaDb(this, conn, "its 'events_statements_summary_by_digest' does not have the MySQL 8.0 columns (QUERY_SAMPLE_TEXT...)"))
			return false;

		return MySqlCmUtils.checkViewsAreReadable(this, conn, "performance_schema.events_statements_summary_by_digest");
	}

	@Override
	public List<String> getPkForVersion(DbxConnection conn, DbmsVersionInfo versionInfo)
	{
		List <String> pkCols = new LinkedList<String>();

		pkCols.add("SCHEMA_NAME");
		pkCols.add("DIGEST");

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
			map.put("STMT_TEXT",         new PcsColumnOptions(ColumnType.DICTIONARY_COMPRESSION));
			map.put("QUERY_SAMPLE_TEXT", new PcsColumnOptions(ColumnType.DICTIONARY_COMPRESSION));

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

			mtd.addColumn(cmName, "SCHEMA_NAME"         , "<html>The default schema of the session when the statement ran ('' = none). The same text in another schema is another statement.</html>");
			mtd.addColumn(cmName, "DIGEST"              , "<html>The digest (a hash) of the normalized statement: the same statement with different literal values has the same digest. The same as 'Digest' in the 'Statements' tab.</html>");
			mtd.addColumn(cmName, "IS_SYSTEM_STMT"      , "<html>Y = MySQL's own SQL, or DbxTune's: all tables in the statement are in mysql, sys, performance_schema or information_schema. <br>The Daily Summary Report lists them, but does not take them as N+1.</html>");
			mtd.addColumn(cmName, "ExecCount"           , "<html>Number of executions <b>in this sample</b> (COUNT_STAR).</html>");
			mtd.addColumn(cmName, "ExecPerSec"          , "<html>Executions per second in this sample.</html>");
			mtd.addColumn(cmName, "RowCount"            , "<html>Number of rows sent to the client <b>in this sample</b> (SUM_ROWS_SENT).</html>");
			mtd.addColumn(cmName, "RowsPerSec"          , "<html>Rows sent per second in this sample. <br>For a N+1 parent, this follows the child's <code>ExecPerSec</code>.</html>");
			mtd.addColumn(cmName, "RowsPerExec"         , "<html>Rows sent per execution in this sample. <br>About 1 for a N+1 child (a lookup on a key).</html>");
			mtd.addColumn(cmName, "RowsExaminedPerExec" , "<html>Rows examined (read by the storage engine) per execution in this sample (SUM_ROWS_EXAMINED).</html>");
			mtd.addColumn(cmName, "AvgElapsedMs"        , "<html>Average execution time per execution in milliseconds (SUM_TIMER_WAIT).</html>");
			mtd.addColumn(cmName, "ElapsedMs"           , "<html>Total execution time in milliseconds in this sample (inside MySQL only).</html>");
			mtd.addColumn(cmName, "LockTimeMs"          , "<html>Time waiting for table locks in milliseconds in this sample (SUM_LOCK_TIME).</html>");
			mtd.addColumn(cmName, "RowsExamined"        , "<html>Total rows examined in this sample.</html>");
			mtd.addColumn(cmName, "RowsAffected"        , "<html>Total rows inserted, updated or deleted in this sample.</html>");
			mtd.addColumn(cmName, "EstRoundTripMs"      , "<html>Estimated time the <b>client</b> spent on round trips in this sample: <code>ExecCount * " + PROPKEY_roundTripMs + "</code>. <br>This time is not visible in MySQL, but it is what the application pays for N+1.</html>");
			mtd.addColumn(cmName, "FirstSeen"           , "<html>When the statement was first seen (since the server started, or the digest table was truncated).</html>");
			mtd.addColumn(cmName, "STMT_TEXT"           , "<html>The normalized statement (DIGEST_TEXT: the values are replaced by '?'). Max performance_schema_max_digest_length characters (1024 by default).</html>");
			mtd.addColumn(cmName, "QUERY_SAMPLE_TEXT"   , "<html>One example of the statement, with the values it had. <br>NULL if <code>" + CmStatements.PROPKEY_sample_querySampleText + "</code> is false (the same setting as the 'Statements' tab).</html>");
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
		return "-- reads 'performance_schema.events_statements_summary_by_digest' (cheap statements), see CmChattyStatements.createResultSet()";
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

	/** Server time (NOW(6), the same clock as FIRST_SEEN and LAST_SEEN) when the previous read STARTED (null = no read yet) */
	private Timestamp _lastReadSrvTime;

	/** Client time in ms when the previous read STARTED (for the interval) */
	private long _lastReadClientTime;

	/** Server time of the last FULL read */
	private Timestamp _lastFullReadSrvTime;

	/** Previous counters per digest; key = SCHEMA_NAME:DIGEST */
	private Map<String, DigestCounters> _digestCounters = new HashMap<>();

	/** Cached [STMT_TEXT, QUERY_SAMPLE_TEXT, IS_SYSTEM_STMT] per SCHEMA_NAME:DIGEST: the same statement, so it does not change (QUERY_SAMPLE_TEXT is one of the values) */
	private Map<String, String[]> _textCache = new HashMap<>();

	private static final int TEXT_CACHE_MAX_SIZE = 10_000;

	/** We only warn once (per connect) that the digest table is full */
	private boolean _warnedDigestTableFull = false;

	private static class DigestCounters
	{
		long executions;
		long rowsSent;
		long rowsExamined;
		long rowsAffected;
		long timerWait;    // picoseconds
		long lockTime;     // picoseconds
		long lastSeenTime; // client time in ms of the read where it was last seen
	}

	/** One SCHEMA_NAME:DIGEST in this sample */
	private static class StmtSum
	{
		String    key;
		String    schemaName;
		String    digest;
		long      execCount;
		long      rowsSent;
		long      rowsExamined;
		long      rowsAffected;
		long      timerWait;
		long      lockTime;
		Timestamp firstSeen;
	}

	@Override
	public void close()
	{
		_lastReadSrvTime     = null;
		_lastFullReadSrvTime = null;
		_digestCounters.clear();
		_textCache.clear();
		_warnedDigestTableFull = false;

		super.close();
	}

	/**
	 * Read the cheap statements, calculate what happened since the previous read, keep top rows,
	 * and return them as a ResultSet (read by the framework like any other ResultSet).
	 */
	private ResultSet createResultSet(DbxConnection conn)
	throws SQLException
	{
		Configuration conf = Configuration.getCombinedConfiguration();
		int     topRows         = conf.getIntProperty    (PROPKEY_sample_topRows,         DEFAULT_sample_topRows);
		int     maxRowsExamined = conf.getIntProperty    (PROPKEY_sample_maxRowsExamined, DEFAULT_sample_maxRowsExamined);
		int     resyncMinutes   = conf.getIntProperty    (PROPKEY_sample_resyncMinutes,   DEFAULT_sample_resyncMinutes);
		double  roundTripMs     = conf.getDoubleProperty (PROPKEY_roundTripMs,            DEFAULT_roundTripMs);
		boolean querySampleText = conf.getBooleanProperty(CmStatements.PROPKEY_sample_querySampleText, CmStatements.DEFAULT_sample_querySampleText);

		long thisReadClientTime = System.currentTimeMillis();

		// FIRST_SEEN and LAST_SEEN are in the server's clock (and the session time zone): so compare them with NOW(6) (not the collector's clock)
		Timestamp thisReadSrvTime = null;
		try (Statement stmnt = conn.createStatement(); ResultSet rs = stmnt.executeQuery("select NOW(6)"))
		{
			while (rs.next())
				thisReadSrvTime = rs.getTimestamp(1);
		}
		if (thisReadSrvTime == null)
			thisReadSrvTime = new Timestamp(thisReadClientTime);

		Timestamp prevReadSrvTime    = _lastReadSrvTime;
		long      prevReadClientTime = _lastReadClientTime;

		// FULL read: first time, and every ## minutes (to forget statements that are gone: the digest table was truncated)
		boolean fullRead = prevReadSrvTime == null
				|| _lastFullReadSrvTime == null
				|| (thisReadSrvTime.getTime() - _lastFullReadSrvTime.getTime()) >= resyncMinutes * 60_000L;

		// 5 seconds before the previous read, so we do not miss anything (a digest that is read twice has delta 0 the second time)
		Timestamp prevReadMinus5 = prevReadSrvTime == null ? null : new Timestamp(prevReadSrvTime.getTime() - 5_000);

		// Cheap statements: rows examined per execution (average since the statement came into the digest table) at or below the limit
		// A NULL DIGEST is the row for "all statements that did not fit" when the table is full: read (to warn), but not used
		// The timers are in picoseconds
		String sql = ""
				+ "select /* ${cmCollectorName} */ \n"
				+ "     IFNULL(SCHEMA_NAME, '') \n"
				+ "    ,DIGEST \n"
				+ "    ,COUNT_STAR \n"
				+ "    ,SUM_ROWS_SENT \n"
				+ "    ,SUM_ROWS_EXAMINED \n"
				+ "    ,SUM_ROWS_AFFECTED \n"
				+ "    ,SUM_TIMER_WAIT \n"
				+ "    ,SUM_LOCK_TIME \n"
				+ "    ,FIRST_SEEN \n"
				+ "from performance_schema.events_statements_summary_by_digest \n"
				+ "where COUNT_STAR > 0 \n"
				+ "  and SUM_ROWS_EXAMINED <= ? * COUNT_STAR \n"
				+ (fullRead ? "" : "  and LAST_SEEN >= ? \n")
				;
		sql = sql.replace("${cmCollectorName}", getName());

		Map<String, StmtSum> sumMap = new HashMap<>();
		int digestRowsRead = 0;
		int newDigests     = 0;
		long nullDigestExec = -1;

		try (PreparedStatement pstmnt = conn.prepareStatement(sql))
		{
			pstmnt.setQueryTimeout(getQueryTimeout());
			pstmnt.setInt(1, maxRowsExamined);
			if ( ! fullRead )
				pstmnt.setTimestamp(2, prevReadMinus5);

			try (ResultSet rs = pstmnt.executeQuery())
			{
				while (rs.next())
				{
					digestRowsRead++;

					String    schemaName   = rs.getString   (1);
					String    digest       = rs.getString   (2);
					long      executions   = rs.getLong     (3);
					long      rowsSent     = rs.getLong     (4);
					long      rowsExamined = rs.getLong     (5);
					long      rowsAffected = rs.getLong     (6);
					long      timerWait    = rs.getLong     (7);
					long      lockTime     = rs.getLong     (8);
					Timestamp firstSeen    = rs.getTimestamp(9);

					// The digest table is full: new statements are counted in the NULL digest row. Many statements in one row: not used
					if (digest == null)
					{
						nullDigestExec = executions;
						continue;
					}

					String key = schemaName + ":" + digest;

					// Calculate what happened since previous read
					DigestCounters prev = _digestCounters.get(key);

					long dExec = 0, dRowsSent = 0, dRowsExamined = 0, dRowsAffected = 0, dTimer = 0, dLock = 0;
					if (prev != null && executions >= prev.executions)
					{
						dExec         = executions   - prev.executions;
						dRowsSent     = rowsSent     - prev.rowsSent;
						dRowsExamined = rowsExamined - prev.rowsExamined;
						dRowsAffected = rowsAffected - prev.rowsAffected;
						dTimer        = timerWait    - prev.timerWait;
						dLock         = lockTime     - prev.lockTime;
					}
					else if (prev == null && prevReadMinus5 != null && firstSeen != null && ! firstSeen.before(prevReadMinus5))
					{
						// Not seen before, and first seen after the previous read: a new digest, so the counters are the delta.
						// (A digest that was NOT cheap before and now is has an older FIRST_SEEN: it is only remembered.)
						dExec         = executions;
						dRowsSent     = rowsSent;
						dRowsExamined = rowsExamined;
						dRowsAffected = rowsAffected;
						dTimer        = timerWait;
						dLock         = lockTime;
						newDigests++;
					}
					// else: first time we see this (old) digest, or the counters started over (the table was truncated): just remember the counters

					if (prev == null)
					{
						prev = new DigestCounters();
						_digestCounters.put(key, prev);
					}
					prev.executions   = executions;
					prev.rowsSent     = rowsSent;
					prev.rowsExamined = rowsExamined;
					prev.rowsAffected = rowsAffected;
					prev.timerWait    = timerWait;
					prev.lockTime     = lockTime;
					prev.lastSeenTime = thisReadClientTime;

					if (dExec <= 0)
						continue;

					StmtSum sum = new StmtSum();
					sum.key          = key;
					sum.schemaName   = schemaName;
					sum.digest       = digest;
					sum.execCount    = dExec;
					sum.rowsSent     = Math.max(0, dRowsSent);
					sum.rowsExamined = Math.max(0, dRowsExamined);
					sum.rowsAffected = Math.max(0, dRowsAffected);
					sum.timerWait    = Math.max(0, dTimer);
					sum.lockTime     = Math.max(0, dLock);
					sum.firstSeen    = firstSeen;
					sumMap.put(key, sum);
				}
			}
		}

		if (nullDigestExec > 0 && ! _warnedDigestTableFull)
		{
			_warnedDigestTableFull = true;
			_logger.warn(getName() + ": The performance_schema digest table is FULL (performance_schema_digests_size), new statements are counted in a row with an empty Digest, and are not seen here. "
					+ "Fix: increase performance_schema_digests_size in my.cnf (needs a restart).");
		}

		// After a FULL read: forget digests that are no longer in the table (or no longer cheap)
		int removedDigests = 0;
		if (fullRead)
		{
			for (Iterator<DigestCounters> it = _digestCounters.values().iterator(); it.hasNext();)
			{
				if (it.next().lastSeenTime != thisReadClientTime)
				{
					it.remove();
					removedDigests++;
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

		all.sort(Comparator.comparingLong((StmtSum s) -> s.rowsSent).reversed());
		for (int i = 0; i < Math.min(topRows, all.size()); i++)
			if (all.get(i).rowsSent > 0)
				keep.put(all.get(i).key, all.get(i));

		// Get the statement text for the digests we don't have in the cache: one read with an IN list
		List<String> lookupDigests = new ArrayList<>();
		for (StmtSum sum : keep.values())
		{
			if ( ! _textCache.containsKey(sum.key) && ! lookupDigests.contains(sum.digest) )
				lookupDigests.add(sum.digest);
		}

		int textLookups = lookupDigests.size();
		if ( ! lookupDigests.isEmpty() )
		{
			StringBuilder inList = new StringBuilder();
			for (int i = 0; i < lookupDigests.size(); i++)
				inList.append(i == 0 ? "" : ", ").append("?");

			String textSql = ""
					+ "select /* " + getName() + " */ IFNULL(SCHEMA_NAME, ''), DIGEST, DIGEST_TEXT, " + (querySampleText ? "QUERY_SAMPLE_TEXT" : "CAST(NULL as CHAR)") + " \n"
					+ "from performance_schema.events_statements_summary_by_digest \n"
					+ "where DIGEST in (" + inList + ") \n"
					;

			try (PreparedStatement pstmnt = conn.prepareStatement(textSql))
			{
				pstmnt.setQueryTimeout(getQueryTimeout());
				for (int i = 0; i < lookupDigests.size(); i++)
					pstmnt.setString(i + 1, lookupDigests.get(i));

				try (ResultSet rs = pstmnt.executeQuery())
				{
					while (rs.next())
					{
						String schemaName = rs.getString(1);
						String key        = schemaName + ":" + rs.getString(2);
						String text       = rs.getString(3);

						// The same digest can be in other schemas, that we did not keep: only cache the kept ones
						if (keep.containsKey(key) && text != null)
							_textCache.putIfAbsent(key, new String[] {text, rs.getString(4), isSystemStmt(schemaName, text) ? "Y" : "N"});
					}
				}
			}
			catch (SQLException ex)
			{
				_logger.warn(getName() + ": Problems getting the statement text for " + lookupDigests.size() + " statements. Continuing without them. Caught: " + ex);
			}
		}

		// Build the ResultSet
		double intervalSec = prevReadSrvTime == null ? 0 : (thisReadClientTime - prevReadClientTime) / 1000.0;

		SimpleResultSet srs = new SimpleResultSet();
		srs.addColumn("SCHEMA_NAME",         Types.VARCHAR,  64, 0);
		srs.addColumn("DIGEST",              Types.VARCHAR,  64, 0);
		srs.addColumn("IS_SYSTEM_STMT",      Types.VARCHAR,   1, 0);
		srs.addColumn("ExecCount",           Types.BIGINT,    0, 0);
		srs.addColumn("ExecPerSec",          Types.NUMERIC,  12, 1);
		srs.addColumn("RowCount",            Types.BIGINT,    0, 0);
		srs.addColumn("RowsPerSec",          Types.NUMERIC,  12, 1);
		srs.addColumn("RowsPerExec",         Types.NUMERIC,  12, 2);
		srs.addColumn("RowsExaminedPerExec", Types.NUMERIC,  12, 1);
		srs.addColumn("AvgElapsedMs",        Types.NUMERIC,  14, 3);
		srs.addColumn("ElapsedMs",           Types.NUMERIC,  16, 1);
		srs.addColumn("LockTimeMs",          Types.NUMERIC,  16, 1);
		srs.addColumn("RowsExamined",        Types.BIGINT,    0, 0);
		srs.addColumn("RowsAffected",        Types.BIGINT,    0, 0);
		srs.addColumn("EstRoundTripMs",      Types.NUMERIC,  14, 1);
		srs.addColumn("FirstSeen",           Types.TIMESTAMP, 0, 0);
		srs.addColumn("STMT_TEXT",           Types.VARCHAR, Integer.MAX_VALUE, 0);
		srs.addColumn("QUERY_SAMPLE_TEXT",   Types.VARCHAR, Integer.MAX_VALUE, 0);

		for (StmtSum sum : keep.values())
		{
			String[] text = _textCache.get(sum.key);
			double elapsedMs = sum.timerWait / 1_000_000_000.0; // picoseconds -> milliseconds
			srs.addRow(
				sum.schemaName,
				sum.digest,
				text == null ? null : text[2],
				sum.execCount,
				intervalSec <= 0 ? null : num(sum.execCount / intervalSec, 1),
				sum.rowsSent,
				intervalSec <= 0 ? null : num(sum.rowsSent / intervalSec, 1),
				num(sum.rowsSent     * 1.0 / sum.execCount, 2),
				num(sum.rowsExamined * 1.0 / sum.execCount, 1),
				num(elapsedMs / sum.execCount, 3),
				num(elapsedMs, 1),
				num(sum.lockTime / 1_000_000_000.0, 1),
				sum.rowsExamined,
				sum.rowsAffected,
				num(sum.execCount * roundTripMs, 1),
				sum.firstSeen,
				text == null ? null : text[0],
				text == null ? null : text[1]
			);
		}

		if (_logger.isDebugEnabled())
			_logger.debug(getName() + ": fullRead=" + fullRead + ", digestRowsRead=" + digestRowsRead + ", newDigests=" + newDigests + ", digestCounters=" + _digestCounters.size() + ", removedDigests=" + removedDigests
					+ ", digestsWithExec=" + sumMap.size() + ", keptRows=" + keep.size() + ", textLookups=" + textLookups + ", intervalSec=" + intervalSec);

		return srs;
	}

	/**
	 * MySQL's own SQL (or DbxTune's own): a statement where ALL tables are in mysql, sys, performance_schema or information_schema
	 * (see CmStatements.getTablesOfDigestText() and ObjectLookupInspectorMySql.isSystemObject()).<br>
	 * A table without a schema is in the default schema of the statement (SCHEMA_NAME).<br>
	 * A statement without tables (SET, SELECT @@var, COMMIT, CALL) is not a system statement.
	 */
	private static boolean isSystemStmt(String schemaName, String digestText)
	{
		Set<String> tables = CmStatements.getTablesOfDigestText(digestText);
		if (tables.isEmpty())
			return false;

		for (String table : tables)
		{
			String[] schemaAndTable = ObjectLookupInspectorMySql.splitSchemaAndTable(schemaName, table);
			if (StringUtil.isNullOrBlank(schemaAndTable[0]) || ! ObjectLookupInspectorMySql.isSystemObject(schemaAndTable[0], schemaAndTable[1]) )
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
}
