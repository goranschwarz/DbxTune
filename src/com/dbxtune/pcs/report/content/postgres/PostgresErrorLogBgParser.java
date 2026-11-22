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
package com.dbxtune.pcs.report.content.postgres;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.sql.Types;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.lang3.StringUtils;

import com.dbxtune.gui.ResultSetTableModel;
import com.dbxtune.sql.conn.DbxConnection;
import com.dbxtune.utils.StringUtil;

/**
 * Parses operational {@code LOG} entries from PostgreSQL {@code CmErrorLog_abs}
 * (works identically for on-prem CSV/JSON logs and Cloud SQL logs) into
 * {@code pgbadger}-style summary {@link ResultSetTableModel} tables:
 * <ol>
 *   <li><b>Checkpoints</b> ({@code checkpoint starting} / {@code checkpoint complete})</li>
 *   <li><b>Autovacuum &amp; Autoanalyze</b> ({@code automatic [aggressive] vacuum ...} / {@code automatic analyze ...})</li>
 *   <li><b>Temporary Files</b> ({@code temporary file: path "...", size N})</li>
 *   <li><b>Lock Waits</b> ({@code process N still waiting for / acquired <LockMode> on <Target> after X ms})</li>
 *   <li><b>Slow Statements &amp; auto_explain</b> ({@code duration: X ms ...})</li>
 * </ol>
 */
public class PostgresErrorLogBgParser
{
	//-------------------------------------------------------------------------
	// 1. Checkpoint regexes (PostgreSQL 12 - 18+)
	//-------------------------------------------------------------------------
	private static final Pattern CKPT_START_PATTERN      = Pattern.compile("^(?:checkpoint|restartpoint)\\s+starting:\\s+(.+)$", Pattern.CASE_INSENSITIVE);
	private static final Pattern CKPT_COMPLETE_PREFIX    = Pattern.compile("^(checkpoint|restartpoint)\\s+complete:\\s+", Pattern.CASE_INSENSITIVE);
	private static final Pattern CKPT_BUFFERS_PATTERN    = Pattern.compile("wrote\\s+(\\d+)\\s+buffers\\s+\\(([0-9.]+)%\\)");
	private static final Pattern CKPT_TIMING_PATTERN     = Pattern.compile("write=([0-9.]+)\\s*s,\\s*sync=([0-9.]+)\\s*s,\\s*total=([0-9.]+)\\s*s");
	private static final Pattern CKPT_SYNC_FILES_PATTERN = Pattern.compile("sync files=(\\d+),\\s*longest=([0-9.]+)\\s*s,\\s*average=([0-9.]+)\\s*s");
	private static final Pattern CKPT_DISTANCE_PATTERN   = Pattern.compile("distance=(\\d+)\\s*kB,\\s*estimate=(\\d+)\\s*kB");

	//-------------------------------------------------------------------------
	// 2. Autovacuum & Autoanalyze regexes (PostgreSQL 12 - 18+)
	//-------------------------------------------------------------------------
	private static final Pattern AUTOVAC_HEADER_PATTERN     = Pattern.compile("^automatic\\s+(aggressive\\s+)?vacuum\\s+(to\\s+prevent\\s+wraparound\\s+)?of\\s+table\\s+\"([^\"]+)\"(?::\\s+index\\s+scans:\\s+(\\d+))?", Pattern.CASE_INSENSITIVE | Pattern.MULTILINE);
	private static final Pattern AUTOANALYZE_HEADER_PATTERN = Pattern.compile("^automatic\\s+analyze\\s+of\\s+table\\s+\"([^\"]+)\"", Pattern.CASE_INSENSITIVE | Pattern.MULTILINE);
	private static final Pattern VAC_PAGES_PATTERN          = Pattern.compile("pages:\\s+(\\d+)\\s+removed,\\s+(\\d+)\\s+remain");
	private static final Pattern VAC_TUPLES_PATTERN         = Pattern.compile("tuples:\\s+(\\d+)\\s+removed,\\s+(\\d+)\\s+remain(?:,\\s+(\\d+)\\s+are\\s+dead\\s+but\\s+not\\s+yet\\s+removable)?");
	private static final Pattern VAC_BUFFER_PATTERN         = Pattern.compile("buffer\\s+usage:\\s+(\\d+)\\s+hits,\\s+(\\d+)\\s+misses,\\s+(\\d+)\\s+dirtied");
	private static final Pattern VAC_IO_RATE_PATTERN        = Pattern.compile("avg\\s+read\\s+rate:\\s+([0-9.]+)\\s+MB/s,\\s+avg\\s+write\\s+rate:\\s+([0-9.]+)\\s+MB/s");
	private static final Pattern VAC_WAL_PATTERN            = Pattern.compile("WAL\\s+usage:\\s+(\\d+)\\s+records,\\s+(\\d+)\\s+full\\s+page\\s+images,\\s+(\\d+)\\s+bytes");
	private static final Pattern VAC_SYS_USAGE_PATTERN      = Pattern.compile("system\\s+usage:\\s+CPU:\\s+user:\\s+([0-9.]+)\\s*s,\\s+system:\\s+([0-9.]+)\\s*s,\\s+elapsed:\\s+([0-9.]+)\\s*s");

	//-------------------------------------------------------------------------
	// 3. Temporary file regex
	//-------------------------------------------------------------------------
	private static final Pattern TEMP_FILE_PATTERN = Pattern.compile("^temporary\\s+file:\\s+path\\s+\"([^\"]+)\",\\s+size\\s+(\\d+)", Pattern.CASE_INSENSITIVE);

	//-------------------------------------------------------------------------
	// 4. Lock wait regex
	//    Examples:
	//      process 12345 still waiting for ShareLock on transaction 987654 after 1000.123 ms
	//      process 12345 acquired AccessExclusiveLock on relation 16384 of database 16380 after 2450.500 ms
	//      process 12345 avoided deadlock for ShareLock on transaction 987654 after 1000.123 ms
	//-------------------------------------------------------------------------
	private static final Pattern LOCK_WAIT_PATTERN = Pattern.compile("^process\\s+(\\d+)\\s+(still\\s+waiting\\s+for|acquired|avoided\\s+deadlock\\s+for)\\s+(\\S+)\\s+on\\s+(.+?)\\s+after\\s+([0-9.]+)\\s*ms", Pattern.CASE_INSENSITIVE);

	//-------------------------------------------------------------------------
	// 5. Slow query / auto_explain duration regex
	//    Examples:
	//      duration: 1234.567 ms  statement: SELECT ...
	//      duration: 1234.567 ms  execute <unnamed>: SELECT ...
	//      duration: 1234.567 ms  plan:\nQuery Text: SELECT ...
	//      duration: 1234.567 ms
	//-------------------------------------------------------------------------
	private static final Pattern DURATION_PATTERN                = Pattern.compile("^duration:\\s+([0-9.]+)\\s*ms(?:\\s+(statement|execute[^:]*|bind[^:]*|parse[^:]*|plan):\\s*(.*))?$", Pattern.CASE_INSENSITIVE | Pattern.DOTALL);
	private static final Pattern AUTO_EXPLAIN_QUERY_TEXT_PATTERN = Pattern.compile("Query\\s+Text:\\s*(.+?)(?:\\r?\\n|$)", Pattern.CASE_INSENSITIVE);

	//-------------------------------------------------------------------------
	// Accumulator state
	//-------------------------------------------------------------------------
	private final Map<String, CkptGroup>      _ckptGroups      = new LinkedHashMap<>();
	private final Map<String, AutovacGroup>   _autovacGroups   = new LinkedHashMap<>();
	private final Map<String, TempFileGroup>  _tempFileGroups  = new LinkedHashMap<>();
	private final Map<String, LockWaitGroup>  _lockWaitGroups  = new LinkedHashMap<>();
	private final List<LockWaitEvent>         _lockWaitEvents  = new ArrayList<>();
	private final Map<String, SlowQueryGroup> _slowQueryGroups = new LinkedHashMap<>();

	private String _pendingCkptReason = null;
	private int    _totalLogRowsRead  = 0;

	public int getTotalLogRowsRead()
	{
		return _totalLogRowsRead;
	}

	/**
	 * Query {@code CmErrorLog_abs} for {@code error_severity = 'LOG'} rows in the report period
	 * and parse all operational events.
	 */
	public void parseFromPcs(DbxConnection conn, String reportPeriodSqlWhere)
	throws SQLException
	{
		String sql = ""
				+ "select \n"
				+ "     [log_time] \n"
				+ "    ,[user_name] \n"
				+ "    ,[database_name] \n"
				+ "    ,[process_id] \n"
				+ "    ,[application_name] \n"
				+ "    ,[message] \n"
				+ "    ,[detail] \n"
				+ "    ,[query] \n"
				+ "from [CmErrorLog_abs] \n"
				+ "where [error_severity] = 'LOG' \n"
				+ (reportPeriodSqlWhere != null ? reportPeriodSqlWhere : "")
				+ "order by [log_time] asc \n";

		sql = conn.quotifySqlString(sql);
		try (Statement stmnt = conn.createStatement())
		{
			stmnt.setQueryTimeout(0);
			try (ResultSet rs = stmnt.executeQuery(sql))
			{
				while (rs.next())
				{
					Timestamp logTime  = rs.getTimestamp(1);
					String    userName = rs.getString(2);
					String    dbName   = rs.getString(3);
					int       pid      = rs.getInt(4);
					String    appName  = rs.getString(5);
					String    message  = rs.getString(6);
					String    detail   = rs.getString(7);
					String    query    = rs.getString(8);

					parseLogRow(logTime, userName, dbName, pid, appName, message, detail, query);
				}
			}
		}
	}

	/**
	 * Parse a single {@code LOG} row from {@code CmErrorLog_abs}.
	 */
	public void parseLogRow(
			Timestamp logTime,
			String userName,
			String dbName,
			int processId,
			String appName,
			String message,
			String detail,
			String query)
	{
		if (StringUtil.isNullOrBlank(message))
			return;

		_totalLogRowsRead++;
		String trimmedMsg = message.trim();

		// 1. Checkpoints
		if (tryParseCheckpoint(logTime, trimmedMsg))
			return;

		// 2. Autovacuum / Autoanalyze
		if (tryParseAutovacuum(logTime, trimmedMsg))
			return;

		// 3. Temporary files
		if (tryParseTempFile(logTime, userName, dbName, appName, trimmedMsg, query))
			return;

		// 4. Lock waits
		if (tryParseLockWait(logTime, userName, dbName, processId, appName, trimmedMsg, detail, query))
			return;

		// 5. Slow queries / auto_explain duration
		tryParseSlowQuery(logTime, userName, dbName, appName, trimmedMsg, query);
	}

	//=========================================================================
	// 1. Checkpoints
	//=========================================================================
	private boolean tryParseCheckpoint(Timestamp logTime, String msg)
	{
		Matcher startM = CKPT_START_PATTERN.matcher(msg);
		if (startM.matches())
		{
			String reason = normalizeCheckpointReason(startM.group(1));
			_pendingCkptReason = reason;
			CkptGroup g = _ckptGroups.computeIfAbsent(reason, CkptGroup::new);
			g.startedCount++;
			g.touch(logTime);
			return true;
		}

		Matcher compM = CKPT_COMPLETE_PREFIX.matcher(msg);
		if (compM.find())
		{
			boolean isRestartpoint = "restartpoint".equalsIgnoreCase(compM.group(1));
			String reason = _pendingCkptReason != null
					? _pendingCkptReason
					: (isRestartpoint ? "restartpoint" : "completed (start not in period)");
			_pendingCkptReason = null;

			CkptGroup g = _ckptGroups.computeIfAbsent(reason, CkptGroup::new);
			g.completedCount++;
			g.touch(logTime);

			Matcher bufM = CKPT_BUFFERS_PATTERN.matcher(msg);
			if (bufM.find())
			{
				g.buffersWrittenSum += parseLongSafe(bufM.group(1));
				g.buffersPctSum     += parseDoubleSafe(bufM.group(2));
			}

			Matcher timM = CKPT_TIMING_PATTERN.matcher(msg);
			if (timM.find())
			{
				double writeSec = parseDoubleSafe(timM.group(1));
				double syncSec  = parseDoubleSafe(timM.group(2));
				double totalSec = parseDoubleSafe(timM.group(3));
				g.writeSecSum += writeSec;
				g.writeSecMax  = Math.max(g.writeSecMax, writeSec);
				g.syncSecSum  += syncSec;
				g.syncSecMax   = Math.max(g.syncSecMax, syncSec);
				g.totalSecSum += totalSec;
				g.totalSecMax  = Math.max(g.totalSecMax, totalSec);
			}

			Matcher syncM = CKPT_SYNC_FILES_PATTERN.matcher(msg);
			if (syncM.find())
			{
				long   syncFiles  = parseLongSafe(syncM.group(1));
				double longestSec = parseDoubleSafe(syncM.group(2));
				g.syncFilesSum      += syncFiles;
				g.longestSyncSecMax  = Math.max(g.longestSyncSecMax, longestSec);
			}

			Matcher distM = CKPT_DISTANCE_PATTERN.matcher(msg);
			if (distM.find())
			{
				long distKb = parseLongSafe(distM.group(1));
				g.walDistanceKbSum += distKb;
			}
			return true;
		}
		return false;
	}

	private static String normalizeCheckpointReason(String raw)
	{
		if (raw == null)
			return "unknown";
		String s = raw.trim().toLowerCase();
		if (s.equals("xlog"))
			return "wal";
		return s;
	}

	//=========================================================================
	// 2. Autovacuum & Autoanalyze
	//=========================================================================
	private boolean tryParseAutovacuum(Timestamp logTime, String msg)
	{
		Matcher vacM = AUTOVAC_HEADER_PATTERN.matcher(msg);
		if (vacM.find())
		{
			boolean isAggressive = vacM.group(1) != null;
			boolean isWraparound = vacM.group(2) != null;
			String  tableName    = vacM.group(3);
			long    indexScans   = parseLongSafe(vacM.group(4));

			AutovacGroup g = _autovacGroups.computeIfAbsent(tableName, AutovacGroup::new);
			g.vacuumCount++;
			if (isAggressive)
				g.aggressiveCount++;
			if (isWraparound)
				g.wraparoundCount++;
			g.indexScansSum += indexScans;
			g.touch(logTime);

			Matcher pagesM = VAC_PAGES_PATTERN.matcher(msg);
			if (pagesM.find())
			{
				g.pagesRemovedSum += parseLongSafe(pagesM.group(1));
				g.pagesRemainLast  = parseLongSafe(pagesM.group(2));
			}

			Matcher tupM = VAC_TUPLES_PATTERN.matcher(msg);
			if (tupM.find())
			{
				g.tuplesRemovedSum += parseLongSafe(tupM.group(1));
				g.tuplesRemainLast  = parseLongSafe(tupM.group(2));
				if (tupM.group(3) != null)
					g.deadNotRemovableMax = Math.max(g.deadNotRemovableMax, parseLongSafe(tupM.group(3)));
			}

			parseCommonVacuumMetrics(msg, g);
			return true;
		}

		Matcher anlM = AUTOANALYZE_HEADER_PATTERN.matcher(msg);
		if (anlM.find())
		{
			String tableName = anlM.group(1);
			AutovacGroup g = _autovacGroups.computeIfAbsent(tableName, AutovacGroup::new);
			g.analyzeCount++;
			g.touch(logTime);
			parseCommonVacuumMetrics(msg, g);
			return true;
		}

		return false;
	}

	private static void parseCommonVacuumMetrics(String msg, AutovacGroup g)
	{
		Matcher bufM = VAC_BUFFER_PATTERN.matcher(msg);
		if (bufM.find())
		{
			g.bufferHitsSum    += parseLongSafe(bufM.group(1));
			g.bufferMissesSum  += parseLongSafe(bufM.group(2));
			g.bufferDirtiedSum += parseLongSafe(bufM.group(3));
		}

		Matcher ioM = VAC_IO_RATE_PATTERN.matcher(msg);
		if (ioM.find())
		{
			g.readRateMbsSum  += parseDoubleSafe(ioM.group(1));
			g.writeRateMbsSum += parseDoubleSafe(ioM.group(2));
			g.ioRateCount++;
		}

		Matcher walM = VAC_WAL_PATTERN.matcher(msg);
		if (walM.find())
		{
			g.walRecordsSum += parseLongSafe(walM.group(1));
			g.walFpiSum     += parseLongSafe(walM.group(2));
			g.walBytesSum   += parseLongSafe(walM.group(3));
		}

		Matcher sysM = VAC_SYS_USAGE_PATTERN.matcher(msg);
		if (sysM.find())
		{
			double userSec    = parseDoubleSafe(sysM.group(1));
			double sysSec     = parseDoubleSafe(sysM.group(2));
			double elapsedSec = parseDoubleSafe(sysM.group(3));
			g.cpuSecSum     += (userSec + sysSec);
			g.elapsedSecSum += elapsedSec;
			g.elapsedSecMax  = Math.max(g.elapsedSecMax, elapsedSec);
		}
	}

	//=========================================================================
	// 3. Temporary Files
	//=========================================================================
	private boolean tryParseTempFile(
			Timestamp logTime,
			String userName,
			String dbName,
			String appName,
			String msg,
			String query)
	{
		Matcher m = TEMP_FILE_PATTERN.matcher(msg);
		if (!m.find())
			return false;

		long sizeBytes = parseLongSafe(m.group(2));
		String db   = defaultStr(dbName,   "-unknown-");
		String user = defaultStr(userName, "-unknown-");
		String app  = defaultStr(appName,  "");
		String normQuery = normalizeQueryKey(query);

		String key = db + "|" + user + "|" + app + "|" + normQuery;
		TempFileGroup g = _tempFileGroups.computeIfAbsent(key, k -> new TempFileGroup(db, user, app, query));
		g.fileCount++;
		g.sizeBytesSum += sizeBytes;
		g.sizeBytesMax  = Math.max(g.sizeBytesMax, sizeBytes);
		g.touch(logTime);
		if (StringUtil.isNullOrBlank(g.exampleQuery) && StringUtil.hasValue(query))
			g.exampleQuery = StringUtils.left(query.trim(), 1000);

		return true;
	}

	//=========================================================================
	// 4. Lock Waits
	//=========================================================================
	private boolean tryParseLockWait(
			Timestamp logTime,
			String userName,
			String dbName,
			int processId,
			String appName,
			String msg,
			String detail,
			String query)
	{
		Matcher m = LOCK_WAIT_PATTERN.matcher(msg);
		if (!m.find())
			return false;

		int    pid       = (int) parseLongSafe(m.group(1));
		if (processId <= 0)
			processId = pid;
		String rawStatus = m.group(2).trim().toLowerCase().replaceAll("\\s+", " ");
		String status    = rawStatus.startsWith("still waiting") ? "still waiting"
		                 : rawStatus.startsWith("acquired")      ? "acquired"
		                 : rawStatus;
		String lockMode  = m.group(3).trim();
		String target    = m.group(4).trim();
		double waitMs    = parseDoubleSafe(m.group(5));

		String targetType = extractLockTargetType(target);
		String db         = defaultStr(dbName,   "-unknown-");
		String user       = defaultStr(userName, "-unknown-");

		String key = db + "|" + lockMode + "|" + targetType + "|" + status;
		LockWaitGroup g = _lockWaitGroups.computeIfAbsent(key, k -> new LockWaitGroup(db, lockMode, targetType, status));
		g.eventCount++;
		g.waitMsSum += waitMs;
		g.waitMsMax  = Math.max(g.waitMsMax, waitMs);
		g.touch(logTime);
		if (StringUtil.isNullOrBlank(g.exampleQuery) && StringUtil.hasValue(query))
			g.exampleQuery = StringUtils.left(query.trim(), 1000);

		LockWaitEvent ev = new LockWaitEvent();
		ev.logTime    = logTime;
		ev.dbName     = db;
		ev.userName   = user;
		ev.appName    = defaultStr(appName, "");
		ev.processId  = processId;
		ev.status     = status;
		ev.lockMode   = lockMode;
		ev.lockTarget = target;
		ev.waitMs     = waitMs;
		ev.detail     = StringUtils.left(defaultStr(detail, ""), 1000);
		ev.query      = StringUtils.left(defaultStr(query,  ""), 1000);
		_lockWaitEvents.add(ev);

		return true;
	}

	private static String extractLockTargetType(String target)
	{
		if (StringUtil.isNullOrBlank(target))
			return "unknown";
		int sp = target.indexOf(' ');
		return sp > 0 ? target.substring(0, sp).toLowerCase() : target.toLowerCase();
	}

	//=========================================================================
	// 5. Slow Statements / auto_explain
	//=========================================================================
	private boolean tryParseSlowQuery(
			Timestamp logTime,
			String userName,
			String dbName,
			String appName,
			String msg,
			String query)
	{
		Matcher m = DURATION_PATTERN.matcher(msg);
		if (!m.matches())
			return false;

		double durationMs = parseDoubleSafe(m.group(1));
		String rawKind    = m.group(2);
		String body       = m.group(3);

		String kind;
		boolean hasPlan = false;
		if (rawKind == null)
		{
			kind = "duration";
		}
		else
		{
			String rk = rawKind.trim().toLowerCase();
			if (rk.startsWith("plan"))
			{
				kind    = "auto_explain plan";
				hasPlan = true;
			}
			else if (rk.startsWith("execute"))
			{
				kind = "execute";
			}
			else if (rk.startsWith("bind"))
			{
				kind = "bind";
			}
			else if (rk.startsWith("parse"))
			{
				kind = "parse";
			}
			else
			{
				kind = "statement";
			}
		}

		// Determine SQL statement text
		String sqlText = query;
		if (StringUtil.isNullOrBlank(sqlText) && StringUtil.hasValue(body))
		{
			if (hasPlan)
			{
				Matcher qm = AUTO_EXPLAIN_QUERY_TEXT_PATTERN.matcher(body);
				if (qm.find())
					sqlText = qm.group(1).trim();
				else
					sqlText = body.trim();
			}
			else
			{
				sqlText = body.trim();
			}
		}

		String db   = defaultStr(dbName,   "-unknown-");
		String user = defaultStr(userName, "-unknown-");
		String app  = defaultStr(appName,  "");
		String normSql = normalizeQueryKey(sqlText);

		final String finalSqlText = sqlText;
		String key = db + "|" + user + "|" + kind + "|" + normSql;
		SlowQueryGroup g = _slowQueryGroups.computeIfAbsent(key, k -> new SlowQueryGroup(db, user, app, kind, finalSqlText));
		g.execCount++;
		if (hasPlan)
			g.planCount++;
		g.durationMsSum += durationMs;
		g.durationMsMax  = Math.max(g.durationMsMax, durationMs);
		g.touch(logTime);
		if (StringUtil.isNullOrBlank(g.exampleQuery) && StringUtil.hasValue(sqlText))
			g.exampleQuery = StringUtils.left(sqlText.trim(), 1000);

		return true;
	}

	//=========================================================================
	// Build ResultSetTableModels for Daily Summary Report
	//=========================================================================

	public ResultSetTableModel buildCheckpointsRstm()
	{
		ResultSetTableModel rstm = ResultSetTableModel.createEmpty("PostgresErrorLogCheckpoints");
		rstm.addColumn("checkpoint_reason",     0,  Types.VARCHAR,   "varchar", "varchar(60)",    60, -1, "",   String.class);
		rstm.addColumn("started_count",         1,  Types.INTEGER,   "int",     "int",            10, -1, 0,    Integer.class);
		rstm.addColumn("completed_count",       2,  Types.INTEGER,   "int",     "int",            10, -1, 0,    Integer.class);
		rstm.addColumn("buffers_written__sum",  3,  Types.BIGINT,    "bigint",  "bigint",         19, -1, 0L,   Long.class);
		rstm.addColumn("written_mb__sum",       4,  Types.DECIMAL,   "numeric", "numeric(12,1)",  12,  1, null, BigDecimal.class);
		rstm.addColumn("buffers_pct__avg",      5,  Types.DECIMAL,   "numeric", "numeric(6,1)",    6,  1, null, BigDecimal.class);
		rstm.addColumn("write_sec__avg",        6,  Types.DECIMAL,   "numeric", "numeric(10,2)",  10,  2, null, BigDecimal.class);
		rstm.addColumn("write_sec__max",        7,  Types.DECIMAL,   "numeric", "numeric(10,2)",  10,  2, null, BigDecimal.class);
		rstm.addColumn("sync_sec__avg",         8,  Types.DECIMAL,   "numeric", "numeric(10,3)",  10,  3, null, BigDecimal.class);
		rstm.addColumn("sync_sec__max",         9,  Types.DECIMAL,   "numeric", "numeric(10,3)",  10,  3, null, BigDecimal.class);
		rstm.addColumn("total_sec__avg",        10, Types.DECIMAL,   "numeric", "numeric(10,2)",  10,  2, null, BigDecimal.class);
		rstm.addColumn("total_sec__max",        11, Types.DECIMAL,   "numeric", "numeric(10,2)",  10,  2, null, BigDecimal.class);
		rstm.addColumn("longest_sync_sec__max", 12, Types.DECIMAL,   "numeric", "numeric(10,3)",  10,  3, null, BigDecimal.class);
		rstm.addColumn("sync_files__avg",       13, Types.DECIMAL,   "numeric", "numeric(10,1)",  10,  1, null, BigDecimal.class);
		rstm.addColumn("wal_distance_mb__avg",  14, Types.DECIMAL,   "numeric", "numeric(12,1)",  12,  1, null, BigDecimal.class);
		rstm.addColumn("wal_distance_mb__sum",  15, Types.DECIMAL,   "numeric", "numeric(12,1)",  12,  1, null, BigDecimal.class);
		rstm.addColumn("first_entry",           16, Types.TIMESTAMP, "timestamp", "timestamp",    23, -1, null, Timestamp.class);
		rstm.addColumn("last_entry",            17, Types.TIMESTAMP, "timestamp", "timestamp",    23, -1, null, Timestamp.class);

		List<CkptGroup> sorted = new ArrayList<>(_ckptGroups.values());
		sorted.sort(Comparator.comparingInt((CkptGroup g) -> Math.max(g.completedCount, g.startedCount)).reversed());

		for (CkptGroup g : sorted)
		{
			int c = Math.max(1, g.completedCount);
			ArrayList<Object> row = new ArrayList<>(18);
			row.add(g.reason);
			row.add(g.startedCount);
			row.add(g.completedCount);
			row.add(g.buffersWrittenSum);
			row.add(bd(g.buffersWrittenSum * 8.0 / 1024.0, 1));
			row.add(g.completedCount > 0 ? bd(g.buffersPctSum / c, 1) : bd(0, 1));
			row.add(g.completedCount > 0 ? bd(g.writeSecSum / c, 2)   : bd(0, 2));
			row.add(bd(g.writeSecMax, 2));
			row.add(g.completedCount > 0 ? bd(g.syncSecSum / c, 3)    : bd(0, 3));
			row.add(bd(g.syncSecMax, 3));
			row.add(g.completedCount > 0 ? bd(g.totalSecSum / c, 2)   : bd(0, 2));
			row.add(bd(g.totalSecMax, 2));
			row.add(bd(g.longestSyncSecMax, 3));
			row.add(g.completedCount > 0 ? bd((double) g.syncFilesSum / c, 1) : bd(0, 1));
			row.add(g.completedCount > 0 ? bd((g.walDistanceKbSum / 1024.0) / c, 1) : bd(0, 1));
			row.add(bd(g.walDistanceKbSum / 1024.0, 1));
			row.add(g.firstEntry);
			row.add(g.lastEntry);
			rstm.addRow(row);
		}

		rstm.setHighlightSortColumns("completed_count");
		rstm.setDescription(
				"Checkpoints logged in the server log (when <code>log_checkpoints = on</code>, the default since Postgres 15), grouped by checkpoint trigger reason.<br>" +
				"Look for: frequent <code>wal</code> checkpoints (<code>max_wal_size</code> is too small), high <code>sync_sec__max</code> or <code>longest_sync_sec__max</code> (slow disk fsync spikes).<br>");
		rstm.setColumnDescription("checkpoint_reason",     "What triggered the checkpoint: time (checkpoint_timeout), wal (max_wal_size reached), immediate/force (CHECKPOINT command or backup)");
		rstm.setColumnDescription("started_count",         "Number of 'checkpoint starting' messages");
		rstm.setColumnDescription("completed_count",       "Number of 'checkpoint complete' messages");
		rstm.setColumnDescription("buffers_written__sum",  "Total 8 KB shared buffers written by completed checkpoints");
		rstm.setColumnDescription("written_mb__sum",       "Total megabytes written by completed checkpoints (buffers * 8 KB)");
		rstm.setColumnDescription("buffers_pct__avg",      "Average percent of shared_buffers written per checkpoint");
		rstm.setColumnDescription("write_sec__avg",        "Average write phase duration in seconds");
		rstm.setColumnDescription("write_sec__max",        "Maximum write phase duration in seconds");
		rstm.setColumnDescription("sync_sec__avg",         "Average fsync phase duration in seconds");
		rstm.setColumnDescription("sync_sec__max",         "Maximum fsync phase duration in seconds");
		rstm.setColumnDescription("total_sec__avg",        "Average total checkpoint duration in seconds");
		rstm.setColumnDescription("total_sec__max",        "Maximum total checkpoint duration in seconds");
		rstm.setColumnDescription("longest_sync_sec__max", "Longest single file fsync call in seconds across all checkpoints");
		rstm.setColumnDescription("sync_files__avg",       "Average number of relation files synced per checkpoint");
		rstm.setColumnDescription("wal_distance_mb__avg",  "Average WAL distance (MB) generated between checkpoints");
		rstm.setColumnDescription("wal_distance_mb__sum",  "Total WAL distance (MB) covered by completed checkpoints");
		rstm.setColumnDescription("first_entry",           "Timestamp of the first checkpoint message in this group");
		rstm.setColumnDescription("last_entry",            "Timestamp of the last checkpoint message in this group");
		return rstm;
	}

	public ResultSetTableModel buildAutovacuumRstm(int topRows)
	{
		ResultSetTableModel rstm = ResultSetTableModel.createEmpty("PostgresErrorLogAutovacuum");
		rstm.addColumn("table_name",              0,  Types.VARCHAR,   "varchar",   "varchar(200)",  200, -1, "",   String.class);
		rstm.addColumn("vacuum_count",            1,  Types.INTEGER,   "int",       "int",            10, -1, 0,    Integer.class);
		rstm.addColumn("wraparound_count",        2,  Types.INTEGER,   "int",       "int",            10, -1, 0,    Integer.class);
		rstm.addColumn("analyze_count",           3,  Types.INTEGER,   "int",       "int",            10, -1, 0,    Integer.class);
		rstm.addColumn("elapsed_sec__sum",        4,  Types.DECIMAL,   "numeric",   "numeric(12,2)",  12,  2, null, BigDecimal.class);
		rstm.addColumn("elapsed_sec__max",        5,  Types.DECIMAL,   "numeric",   "numeric(12,2)",  12,  2, null, BigDecimal.class);
		rstm.addColumn("cpu_sec__sum",            6,  Types.DECIMAL,   "numeric",   "numeric(12,2)",  12,  2, null, BigDecimal.class);
		rstm.addColumn("tuples_removed__sum",     7,  Types.BIGINT,    "bigint",    "bigint",         19, -1, 0L,   Long.class);
		rstm.addColumn("dead_not_removable__max", 8,  Types.BIGINT,    "bigint",    "bigint",         19, -1, 0L,   Long.class);
		rstm.addColumn("pages_removed__sum",      9,  Types.BIGINT,    "bigint",    "bigint",         19, -1, 0L,   Long.class);
		rstm.addColumn("index_scans__sum",        10, Types.BIGINT,    "bigint",    "bigint",         19, -1, 0L,   Long.class);
		rstm.addColumn("buffer_hits__sum",        11, Types.BIGINT,    "bigint",    "bigint",         19, -1, 0L,   Long.class);
		rstm.addColumn("buffer_misses__sum",      12, Types.BIGINT,    "bigint",    "bigint",         19, -1, 0L,   Long.class);
		rstm.addColumn("buffer_dirtied__sum",     13, Types.BIGINT,    "bigint",    "bigint",         19, -1, 0L,   Long.class);
		rstm.addColumn("read_rate_mbs__avg",      14, Types.DECIMAL,   "numeric",   "numeric(10,2)",  10,  2, null, BigDecimal.class);
		rstm.addColumn("write_rate_mbs__avg",     15, Types.DECIMAL,   "numeric",   "numeric(10,2)",  10,  2, null, BigDecimal.class);
		rstm.addColumn("wal_mb__sum",             16, Types.DECIMAL,   "numeric",   "numeric(12,2)",  12,  2, null, BigDecimal.class);
		rstm.addColumn("wal_fpi__sum",            17, Types.BIGINT,    "bigint",    "bigint",         19, -1, 0L,   Long.class);
		rstm.addColumn("last_entry",              18, Types.TIMESTAMP, "timestamp", "timestamp",      23, -1, null, Timestamp.class);

		List<AutovacGroup> sorted = new ArrayList<>(_autovacGroups.values());
		sorted.sort(Comparator.comparingDouble((AutovacGroup g) -> g.elapsedSecSum).reversed());

		int limit = topRows > 0 ? Math.min(topRows, sorted.size()) : sorted.size();
		for (int i = 0; i < limit; i++)
		{
			AutovacGroup g = sorted.get(i);
			ArrayList<Object> row = new ArrayList<>(19);
			row.add(g.tableName);
			row.add(g.vacuumCount);
			row.add(g.wraparoundCount);
			row.add(g.analyzeCount);
			row.add(bd(g.elapsedSecSum, 2));
			row.add(bd(g.elapsedSecMax, 2));
			row.add(bd(g.cpuSecSum, 2));
			row.add(g.tuplesRemovedSum);
			row.add(g.deadNotRemovableMax);
			row.add(g.pagesRemovedSum);
			row.add(g.indexScansSum);
			row.add(g.bufferHitsSum);
			row.add(g.bufferMissesSum);
			row.add(g.bufferDirtiedSum);
			row.add(g.ioRateCount > 0 ? bd(g.readRateMbsSum  / g.ioRateCount, 2) : bd(0, 2));
			row.add(g.ioRateCount > 0 ? bd(g.writeRateMbsSum / g.ioRateCount, 2) : bd(0, 2));
			row.add(bd(g.walBytesSum / 1048576.0, 2));
			row.add(g.walFpiSum);
			row.add(g.lastEntry);
			rstm.addRow(row);
		}

		rstm.setHighlightSortColumns("elapsed_sec__sum");
		rstm.setDescription(
				"Autovacuum and autoanalyze runs logged in the server log (controlled by <code>log_autovacuum_min_duration</code>), aggregated per table.<br>" +
				"Look for: high <code>dead_not_removable__max</code> (an old transaction / snapshot kept dead rows from being cleaned up), " +
				"<code>index_scans__sum</code> &gt; 1 per vacuum (<code>maintenance_work_mem</code> or <code>autovacuum_work_mem</code> was too small to hold all dead tuple IDs in one pass), " +
				"and <code>wraparound_count</code> &gt; 0 (forced anti-wraparound vacuum).<br>");
		rstm.setColumnDescription("table_name",              "Database.schema.table from the autovacuum/autoanalyze log message");
		rstm.setColumnDescription("vacuum_count",            "Number of automatic vacuum runs logged for this table");
		rstm.setColumnDescription("wraparound_count",        "How many of the vacuums were forced to prevent transaction ID wraparound");
		rstm.setColumnDescription("analyze_count",           "Number of automatic analyze runs logged for this table");
		rstm.setColumnDescription("elapsed_sec__sum",        "Total elapsed seconds spent by autovacuum/autoanalyze on this table");
		rstm.setColumnDescription("elapsed_sec__max",        "Longest single autovacuum/autoanalyze run in seconds");
		rstm.setColumnDescription("cpu_sec__sum",            "Total CPU seconds (user + system) used");
		rstm.setColumnDescription("tuples_removed__sum",     "Total dead tuples removed across all logged vacuums");
		rstm.setColumnDescription("dead_not_removable__max", "Highest number of dead tuples that could not yet be removed (held back by oldest xmin)");
		rstm.setColumnDescription("pages_removed__sum",      "Total pages truncated/removed from the end of the table");
		rstm.setColumnDescription("index_scans__sum",        "Total index scan passes performed during vacuum");
		rstm.setColumnDescription("buffer_hits__sum",        "Total shared buffer hits");
		rstm.setColumnDescription("buffer_misses__sum",      "Total shared buffer misses (disk reads)");
		rstm.setColumnDescription("buffer_dirtied__sum",     "Total buffers dirtied by vacuum/analyze");
		rstm.setColumnDescription("read_rate_mbs__avg",      "Average read rate in MB/s during vacuum/analyze");
		rstm.setColumnDescription("write_rate_mbs__avg",     "Average write rate in MB/s during vacuum/analyze");
		rstm.setColumnDescription("wal_mb__sum",             "Total megabytes of WAL generated by vacuum on this table");
		rstm.setColumnDescription("wal_fpi__sum",            "Total WAL full-page images written by vacuum on this table");
		rstm.setColumnDescription("last_entry",              "When the last autovacuum/autoanalyze finished");
		return rstm;
	}

	public ResultSetTableModel buildTempFilesRstm(int topRows)
	{
		ResultSetTableModel rstm = ResultSetTableModel.createEmpty("PostgresErrorLogTempFiles");
		rstm.addColumn("database_name",    0, Types.VARCHAR,   "varchar",   "varchar(100)",  100, -1, "",   String.class);
		rstm.addColumn("user_name",        1, Types.VARCHAR,   "varchar",   "varchar(100)",  100, -1, "",   String.class);
		rstm.addColumn("application_name", 2, Types.VARCHAR,   "varchar",   "varchar(100)",  100, -1, "",   String.class);
		rstm.addColumn("temp_file_count",  3, Types.INTEGER,   "int",       "int",            10, -1, 0,    Integer.class);
		rstm.addColumn("size_mb__sum",     4, Types.DECIMAL,   "numeric",   "numeric(12,1)",  12,  1, null, BigDecimal.class);
		rstm.addColumn("size_mb__avg",     5, Types.DECIMAL,   "numeric",   "numeric(12,1)",  12,  1, null, BigDecimal.class);
		rstm.addColumn("size_mb__max",     6, Types.DECIMAL,   "numeric",   "numeric(12,1)",  12,  1, null, BigDecimal.class);
		rstm.addColumn("first_entry",      7, Types.TIMESTAMP, "timestamp", "timestamp",      23, -1, null, Timestamp.class);
		rstm.addColumn("last_entry",       8, Types.TIMESTAMP, "timestamp", "timestamp",      23, -1, null, Timestamp.class);
		rstm.addColumn("example_query",    9, Types.VARCHAR,   "varchar",   "varchar(1000)",1000, -1, "",   String.class);

		List<TempFileGroup> sorted = new ArrayList<>(_tempFileGroups.values());
		sorted.sort(Comparator.comparingLong((TempFileGroup g) -> g.sizeBytesSum).reversed());

		int limit = topRows > 0 ? Math.min(topRows, sorted.size()) : sorted.size();
		for (int i = 0; i < limit; i++)
		{
			TempFileGroup g = sorted.get(i);
			double sumMb = g.sizeBytesSum / 1048576.0;
			double avgMb = g.fileCount > 0 ? sumMb / g.fileCount : 0.0;
			double maxMb = g.sizeBytesMax / 1048576.0;

			ArrayList<Object> row = new ArrayList<>(10);
			row.add(g.dbName);
			row.add(g.userName);
			row.add(g.appName);
			row.add(g.fileCount);
			row.add(bd(sumMb, 1));
			row.add(bd(avgMb, 1));
			row.add(bd(maxMb, 1));
			row.add(g.firstEntry);
			row.add(g.lastEntry);
			row.add(defaultStr(g.exampleQuery, ""));
			rstm.addRow(row);
		}

		rstm.setHighlightSortColumns("size_mb__sum");
		rstm.setDescription(
				"Temporary files logged in the server log (controlled by <code>log_temp_files</code>), grouped by database, user, application and query.<br>" +
				"Queries spill to disk when sorts, hashes, CTEs or materialized subqueries exceed <code>work_mem</code>.<br>");
		rstm.setColumnDescription("database_name",    "The database where the query ran");
		rstm.setColumnDescription("user_name",        "The user who ran the query");
		rstm.setColumnDescription("application_name", "The client's application_name");
		rstm.setColumnDescription("temp_file_count",  "Number of temporary files created by this group");
		rstm.setColumnDescription("size_mb__sum",     "Total temporary file size in MB");
		rstm.setColumnDescription("size_mb__avg",     "Average temporary file size in MB");
		rstm.setColumnDescription("size_mb__max",     "Largest single temporary file in MB");
		rstm.setColumnDescription("first_entry",      "First temporary file logged in this group");
		rstm.setColumnDescription("last_entry",       "Last temporary file logged in this group");
		rstm.setColumnDescription("example_query",    "The SQL statement that created the temporary file");
		return rstm;
	}

	public ResultSetTableModel buildLockWaitsSummaryRstm()
	{
		ResultSetTableModel rstm = ResultSetTableModel.createEmpty("PostgresErrorLogLockWaitsSummary");
		rstm.addColumn("database_name",    0, Types.VARCHAR,   "varchar",   "varchar(100)",  100, -1, "",   String.class);
		rstm.addColumn("lock_mode",        1, Types.VARCHAR,   "varchar",   "varchar(60)",    60, -1, "",   String.class);
		rstm.addColumn("lock_target_type", 2, Types.VARCHAR,   "varchar",   "varchar(60)",    60, -1, "",   String.class);
		rstm.addColumn("status",           3, Types.VARCHAR,   "varchar",   "varchar(40)",    40, -1, "",   String.class);
		rstm.addColumn("wait_count",       4, Types.INTEGER,   "int",       "int",            10, -1, 0,    Integer.class);
		rstm.addColumn("wait_sec__sum",    5, Types.DECIMAL,   "numeric",   "numeric(12,2)",  12,  2, null, BigDecimal.class);
		rstm.addColumn("wait_ms__avg",     6, Types.DECIMAL,   "numeric",   "numeric(12,1)",  12,  1, null, BigDecimal.class);
		rstm.addColumn("wait_ms__max",     7, Types.DECIMAL,   "numeric",   "numeric(12,1)",  12,  1, null, BigDecimal.class);
		rstm.addColumn("first_entry",      8, Types.TIMESTAMP, "timestamp", "timestamp",      23, -1, null, Timestamp.class);
		rstm.addColumn("last_entry",       9, Types.TIMESTAMP, "timestamp", "timestamp",      23, -1, null, Timestamp.class);
		rstm.addColumn("example_query",   10, Types.VARCHAR,   "varchar",   "varchar(1000)",1000, -1, "",   String.class);

		List<LockWaitGroup> sorted = new ArrayList<>(_lockWaitGroups.values());
		sorted.sort(Comparator.comparingDouble((LockWaitGroup g) -> g.waitMsSum).reversed());

		for (LockWaitGroup g : sorted)
		{
			ArrayList<Object> row = new ArrayList<>(11);
			row.add(g.dbName);
			row.add(g.lockMode);
			row.add(g.targetType);
			row.add(g.status);
			row.add(g.eventCount);
			row.add(bd(g.waitMsSum / 1000.0, 2));
			row.add(g.eventCount > 0 ? bd(g.waitMsSum / g.eventCount, 1) : bd(0, 1));
			row.add(bd(g.waitMsMax, 1));
			row.add(g.firstEntry);
			row.add(g.lastEntry);
			row.add(defaultStr(g.exampleQuery, ""));
			rstm.addRow(row);
		}

		rstm.setHighlightSortColumns("wait_sec__sum");
		rstm.setDescription(
				"Lock waits longer than <code>deadlock_timeout</code> logged in the server log (when <code>log_lock_waits = on</code>), grouped by database, lock mode, target type and status.<br>" +
				"<code>still waiting</code> is logged once when the wait passes <code>deadlock_timeout</code>; <code>acquired</code> is logged when the lock is finally granted.<br>");
		rstm.setColumnDescription("database_name",    "The database where the lock wait occurred");
		rstm.setColumnDescription("lock_mode",        "PostgreSQL lock mode (e.g. ShareLock, ExclusiveLock, AccessExclusiveLock)");
		rstm.setColumnDescription("lock_target_type", "What was locked: transaction, relation, tuple, object, virtualxid, ...");
		rstm.setColumnDescription("status",           "'still waiting' (at deadlock_timeout) or 'acquired' (when lock was granted)");
		rstm.setColumnDescription("wait_count",       "Number of lock wait log messages in this group");
		rstm.setColumnDescription("wait_sec__sum",    "Total wait time in seconds across messages in this group");
		rstm.setColumnDescription("wait_ms__avg",     "Average wait time in milliseconds");
		rstm.setColumnDescription("wait_ms__max",     "Longest single wait time in milliseconds");
		rstm.setColumnDescription("first_entry",      "First lock wait logged in this group");
		rstm.setColumnDescription("last_entry",       "Last lock wait logged in this group");
		rstm.setColumnDescription("example_query",    "An example SQL statement that waited for the lock");
		return rstm;
	}

	public ResultSetTableModel buildLockWaitDetailsRstm(int topRows)
	{
		ResultSetTableModel rstm = ResultSetTableModel.createEmpty("PostgresErrorLogLockWaitRows");
		rstm.addColumn("log_time",      0, Types.TIMESTAMP, "timestamp", "timestamp",      23, -1, null, Timestamp.class);
		rstm.addColumn("database_name", 1, Types.VARCHAR,   "varchar",   "varchar(100)",  100, -1, "",   String.class);
		rstm.addColumn("user_name",     2, Types.VARCHAR,   "varchar",   "varchar(100)",  100, -1, "",   String.class);
		rstm.addColumn("process_id",    3, Types.INTEGER,   "int",       "int",            10, -1, 0,    Integer.class);
		rstm.addColumn("status",        4, Types.VARCHAR,   "varchar",   "varchar(40)",    40, -1, "",   String.class);
		rstm.addColumn("lock_mode",     5, Types.VARCHAR,   "varchar",   "varchar(60)",    60, -1, "",   String.class);
		rstm.addColumn("lock_target",   6, Types.VARCHAR,   "varchar",   "varchar(200)",  200, -1, "",   String.class);
		rstm.addColumn("wait_ms",       7, Types.DECIMAL,   "numeric",   "numeric(12,1)",  12,  1, null, BigDecimal.class);
		rstm.addColumn("detail",        8, Types.VARCHAR,   "varchar",   "varchar(1000)",1000, -1, "",   String.class);
		rstm.addColumn("query",         9, Types.VARCHAR,   "varchar",   "varchar(1000)",1000, -1, "",   String.class);

		List<LockWaitEvent> sorted = new ArrayList<>(_lockWaitEvents);
		sorted.sort(Comparator.comparingDouble((LockWaitEvent e) -> e.waitMs).reversed());

		int limit = topRows > 0 ? Math.min(topRows, sorted.size()) : sorted.size();
		for (int i = 0; i < limit; i++)
		{
			LockWaitEvent e = sorted.get(i);
			ArrayList<Object> row = new ArrayList<>(10);
			row.add(e.logTime);
			row.add(e.dbName);
			row.add(e.userName);
			row.add(e.processId);
			row.add(e.status);
			row.add(e.lockMode);
			row.add(e.lockTarget);
			row.add(bd(e.waitMs, 1));
			row.add(e.detail);
			row.add(e.query);
			rstm.addRow(row);
		}

		rstm.setHighlightSortColumns("wait_ms");
		rstm.setDescription("The longest individual lock waits logged in the server log, ordered by <code>wait_ms</code> descending.<br>");
		rstm.setColumnDescription("log_time",      "When the lock wait message was written");
		rstm.setColumnDescription("database_name", "The database");
		rstm.setColumnDescription("user_name",     "The user");
		rstm.setColumnDescription("process_id",    "Backend PID that waited for the lock");
		rstm.setColumnDescription("status",        "'still waiting' or 'acquired'");
		rstm.setColumnDescription("lock_mode",     "PostgreSQL lock mode");
		rstm.setColumnDescription("lock_target",   "The target object / transaction / tuple being locked");
		rstm.setColumnDescription("wait_ms",       "Time spent waiting for the lock in milliseconds");
		rstm.setColumnDescription("detail",        "Process holding the lock and wait queue details");
		rstm.setColumnDescription("query",         "The statement that waited for the lock");
		return rstm;
	}

	public ResultSetTableModel buildSlowQueriesRstm(int topRows)
	{
		ResultSetTableModel rstm = ResultSetTableModel.createEmpty("PostgresErrorLogSlowQueries");
		rstm.addColumn("database_name",     0,  Types.VARCHAR,   "varchar",   "varchar(100)",  100, -1, "",   String.class);
		rstm.addColumn("user_name",         1,  Types.VARCHAR,   "varchar",   "varchar(100)",  100, -1, "",   String.class);
		rstm.addColumn("application_name",  2,  Types.VARCHAR,   "varchar",   "varchar(100)",  100, -1, "",   String.class);
		rstm.addColumn("log_kind",          3,  Types.VARCHAR,   "varchar",   "varchar(40)",    40, -1, "",   String.class);
		rstm.addColumn("exec_count",        4,  Types.INTEGER,   "int",       "int",            10, -1, 0,    Integer.class);
		rstm.addColumn("duration_sec__sum", 5,  Types.DECIMAL,   "numeric",   "numeric(12,2)",  12,  2, null, BigDecimal.class);
		rstm.addColumn("duration_ms__avg",  6,  Types.DECIMAL,   "numeric",   "numeric(12,1)",  12,  1, null, BigDecimal.class);
		rstm.addColumn("duration_ms__max",  7,  Types.DECIMAL,   "numeric",   "numeric(12,1)",  12,  1, null, BigDecimal.class);
		rstm.addColumn("has_plan_count",    8,  Types.INTEGER,   "int",       "int",            10, -1, 0,    Integer.class);
		rstm.addColumn("first_entry",       9,  Types.TIMESTAMP, "timestamp", "timestamp",      23, -1, null, Timestamp.class);
		rstm.addColumn("last_entry",        10, Types.TIMESTAMP, "timestamp", "timestamp",      23, -1, null, Timestamp.class);
		rstm.addColumn("example_query",     11, Types.VARCHAR,   "varchar",   "varchar(1000)",1000, -1, "",   String.class);

		List<SlowQueryGroup> sorted = new ArrayList<>(_slowQueryGroups.values());
		sorted.sort(Comparator.comparingDouble((SlowQueryGroup g) -> g.durationMsSum).reversed());

		int limit = topRows > 0 ? Math.min(topRows, sorted.size()) : sorted.size();
		for (int i = 0; i < limit; i++)
		{
			SlowQueryGroup g = sorted.get(i);
			ArrayList<Object> row = new ArrayList<>(12);
			row.add(g.dbName);
			row.add(g.userName);
			row.add(g.appName);
			row.add(g.kind);
			row.add(g.execCount);
			row.add(bd(g.durationMsSum / 1000.0, 2));
			row.add(g.execCount > 0 ? bd(g.durationMsSum / g.execCount, 1) : bd(0, 1));
			row.add(bd(g.durationMsMax, 1));
			row.add(g.planCount);
			row.add(g.firstEntry);
			row.add(g.lastEntry);
			row.add(defaultStr(g.exampleQuery, ""));
			rstm.addRow(row);
		}

		rstm.setHighlightSortColumns("duration_sec__sum");
		rstm.setDescription(
				"Slow statements and <code>auto_explain</code> execution plans logged in the server log (controlled by <code>log_min_duration_statement</code> and <code>auto_explain.log_min_duration</code>), grouped by normalized SQL text.<br>");
		rstm.setColumnDescription("database_name",     "The database");
		rstm.setColumnDescription("user_name",         "The user");
		rstm.setColumnDescription("application_name",  "The client's application_name");
		rstm.setColumnDescription("log_kind",          "'statement', 'execute', 'auto_explain plan', ...");
		rstm.setColumnDescription("exec_count",        "Number of times this slow query was logged");
		rstm.setColumnDescription("duration_sec__sum", "Total duration in seconds across logged executions");
		rstm.setColumnDescription("duration_ms__avg",  "Average duration in milliseconds");
		rstm.setColumnDescription("duration_ms__max",  "Maximum duration in milliseconds");
		rstm.setColumnDescription("has_plan_count",    "How many entries included an auto_explain execution plan");
		rstm.setColumnDescription("first_entry",       "First logged occurrence");
		rstm.setColumnDescription("last_entry",        "Last logged occurrence");
		rstm.setColumnDescription("example_query",     "Example SQL statement text");
		return rstm;
	}

	public boolean hasAnyOperationalEvents()
	{
		return !_ckptGroups.isEmpty()
			|| !_autovacGroups.isEmpty()
			|| !_tempFileGroups.isEmpty()
			|| !_lockWaitGroups.isEmpty()
			|| !_slowQueryGroups.isEmpty();
	}

	//=========================================================================
	// Helpers & Inner Accumulator Classes
	//=========================================================================
	private static String normalizeQueryKey(String sql)
	{
		if (StringUtil.isNullOrBlank(sql))
			return "";
		String s = sql.trim()
				.replaceAll("'[^']*'", "'?'")
				.replaceAll("\\b\\d+\\b", "?")
				.replaceAll("\\s+", " ");
		return StringUtils.left(s, 200);
	}

	private static String defaultStr(String val, String def)
	{
		return StringUtil.isNullOrBlank(val) ? def : val;
	}

	private static long parseLongSafe(String s)
	{
		if (s == null)
			return 0L;
		try { return Long.parseLong(s.trim()); }
		catch (NumberFormatException ex) { return 0L; }
	}

	private static double parseDoubleSafe(String s)
	{
		if (s == null)
			return 0.0;
		try { return Double.parseDouble(s.trim()); }
		catch (NumberFormatException ex) { return 0.0; }
	}

	private static BigDecimal bd(double val, int scale)
	{
		if (Double.isNaN(val) || Double.isInfinite(val))
			val = 0.0;
		return BigDecimal.valueOf(val).setScale(scale, RoundingMode.HALF_UP);
	}

	private static abstract class TimeTrackedGroup
	{
		Timestamp firstEntry;
		Timestamp lastEntry;

		void touch(Timestamp ts)
		{
			if (ts == null)
				return;
			if (firstEntry == null || ts.before(firstEntry))
				firstEntry = ts;
			if (lastEntry == null || ts.after(lastEntry))
				lastEntry = ts;
		}
	}

	private static class CkptGroup extends TimeTrackedGroup
	{
		final String reason;
		int    startedCount;
		int    completedCount;
		long   buffersWrittenSum;
		double buffersPctSum;
		double writeSecSum;
		double writeSecMax;
		double syncSecSum;
		double syncSecMax;
		double totalSecSum;
		double totalSecMax;
		long   syncFilesSum;
		double longestSyncSecMax;
		long   walDistanceKbSum;

		CkptGroup(String reason)
		{
			this.reason = reason;
		}
	}

	private static class AutovacGroup extends TimeTrackedGroup
	{
		final String tableName;
		int    vacuumCount;
		int    aggressiveCount;
		int    wraparoundCount;
		int    analyzeCount;
		long   indexScansSum;
		long   pagesRemovedSum;
		long   pagesRemainLast;
		long   tuplesRemovedSum;
		long   tuplesRemainLast;
		long   deadNotRemovableMax;
		long   bufferHitsSum;
		long   bufferMissesSum;
		long   bufferDirtiedSum;
		double readRateMbsSum;
		double writeRateMbsSum;
		int    ioRateCount;
		long   walRecordsSum;
		long   walFpiSum;
		long   walBytesSum;
		double cpuSecSum;
		double elapsedSecSum;
		double elapsedSecMax;

		AutovacGroup(String tableName)
		{
			this.tableName = tableName;
		}
	}

	private static class TempFileGroup extends TimeTrackedGroup
	{
		final String dbName;
		final String userName;
		final String appName;
		String exampleQuery;
		int    fileCount;
		long   sizeBytesSum;
		long   sizeBytesMax;

		TempFileGroup(String dbName, String userName, String appName, String query)
		{
			this.dbName       = dbName;
			this.userName     = userName;
			this.appName      = appName;
			this.exampleQuery = query != null ? StringUtils.left(query.trim(), 1000) : "";
		}
	}

	private static class LockWaitGroup extends TimeTrackedGroup
	{
		final String dbName;
		final String lockMode;
		final String targetType;
		final String status;
		String exampleQuery;
		int    eventCount;
		double waitMsSum;
		double waitMsMax;

		LockWaitGroup(String dbName, String lockMode, String targetType, String status)
		{
			this.dbName     = dbName;
			this.lockMode   = lockMode;
			this.targetType = targetType;
			this.status     = status;
		}
	}

	private static class LockWaitEvent
	{
		Timestamp logTime;
		String    dbName;
		String    userName;
		String    appName;
		int       processId;
		String    status;
		String    lockMode;
		String    lockTarget;
		double    waitMs;
		String    detail;
		String    query;
	}

	private static class SlowQueryGroup extends TimeTrackedGroup
	{
		final String dbName;
		final String userName;
		final String appName;
		final String kind;
		String exampleQuery;
		int    execCount;
		int    planCount;
		double durationMsSum;
		double durationMsMax;

		SlowQueryGroup(String dbName, String userName, String appName, String kind, String query)
		{
			this.dbName       = dbName;
			this.userName     = userName;
			this.appName      = appName;
			this.kind         = kind;
			this.exampleQuery = query != null ? StringUtils.left(query.trim(), 1000) : "";
		}
	}
}
