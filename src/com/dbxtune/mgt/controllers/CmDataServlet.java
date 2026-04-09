/*******************************************************************************
 * Copyright (C) 2010-2025 Goran Schwarz
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
package com.dbxtune.mgt.controllers;

import java.io.IOException;
import java.io.PrintWriter;
import java.io.Reader;
import java.lang.invoke.MethodHandles;
import java.sql.Blob;
import java.sql.Clob;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.Timestamp;
import java.sql.Types;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Set;
import java.util.Iterator;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.dbxtune.CounterController;
import com.dbxtune.ICounterController;
import com.dbxtune.cm.CmChartDescriptor;
import com.dbxtune.cm.CmHighlighterDescriptor;
import com.dbxtune.gui.swing.ColumnHeaderPropsEntry;
import com.dbxtune.cm.CountersModel;
import com.dbxtune.cm.CountersModelAppend;
import com.dbxtune.config.dict.MonTablesDictionaryManager;
import com.dbxtune.pcs.PersistentCounterHandler;
import com.dbxtune.pcs.PersistWriterJdbc;
import com.dbxtune.sql.conn.DbxConnection;
import com.dbxtune.utils.TimeUtils;

import com.dbxtune.central.controllers.Helper;
import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * Collector-side servlet: GET /api/mgt/cm/data?cm=CmName&amp;time=YYYY-MM-DD HH:mm:ss&amp;type=abs|diff|rate
 * <p>
 * Returns all rows from the CmName_abs (or _diff / _rate) table whose
 * SessionSampleTime is closest to the requested time (within ±60 seconds).
 *
 * <h3>SQL portability / identifier quoting</h3>
 * All SQL strings are written with {@code [bracket]} quoting (SQL Server convention
 * used as the internal standard throughout DbxTune).  Before each SQL string is
 * passed to {@code prepareStatement()}, it is run through
 * {@link DbxConnection#quotifySqlString(String)} which translates the brackets to
 * the correct quote character for the actual DBMS in use
 * (e.g. {@code "} for PostgreSQL, back-tick for MySQL, {@code []} kept for H2/SQL Server).
 *
 * <h3>Row-limiting</h3>
 * Instead of vendor-specific {@code TOP 1} / {@code LIMIT 1} /
 * {@code FETCH FIRST 1 ROWS ONLY}, we use the JDBC-standard
 * {@link java.sql.PreparedStatement#setMaxRows(int)} where only one row is needed.
 *
 * <h3>"Find closest row" strategy</h3>
 * Vendor-specific date-diff functions ({@code DATEDIFF}, {@code TIMESTAMPDIFF}, …)
 * are intentionally avoided.  Instead:
 * <ol>
 *   <li>Fetch all distinct {@code SessionSampleTime} values in the ±60 s window
 *       (standard {@code BETWEEN} predicate, ordered by the timestamp).</li>
 *   <li>Pick the value with minimum {@code |t − requestedTime|} in Java.</li>
 *   <li>Fetch all rows for that exact timestamp.</li>
 * </ol>
 */
public class CmDataServlet
extends HttpServlet
{
	private static final long serialVersionUID = 1L;
	private static final Logger _logger = LogManager.getLogger(MethodHandles.lookup().lookupClass());

	/**
	 * Half-window around the requested time used to search for nearby samples (milliseconds).
	 * <p>
	 * The caller (JS) now derives the timestamp from {@code MonSessionSampleDetailes} which
	 * gives the exact {@code SessionSampleTime} master key.  We keep a small ±1 s window to
	 * absorb any microsecond-level rounding differences between the timestamp the JS received
	 * (millisecond precision) and what the database actually stored (potentially microseconds).
	 */
	private static final long WINDOW_MS = 1_000L;

	/**
	 * Housekeeping columns that are always present in every CM table but carry no
	 * per-row counter value.  They are stripped from the JSON response so the UI
	 * shows only actual CM data columns — mirroring what the native GUI does
	 * (see {@code PersistReader.loadSessionCm}: {@code startColPos = 5}).
	 * <ul>
	 *   <li>{@code SessionStartTime}  — session-level start timestamp</li>
	 *   <li>{@code SessionSampleTime} — shown separately in the panel header as resolvedTime</li>
	 *   <li>{@code SampleTime}        — redundant with SessionSampleTime</li>
	 *   <li>{@code SampleMs}          — sample collection duration (internal metadata)</li>
	 *   <li>{@code CmNewDiffRateRow}  — internal row flag (older schema)</li>
	 *   <li>{@code CmRowState}        — internal row state bitmap (newer schema)</li>
	 * </ul>
	 */
	private static final Set<String> SKIP_COLS;
	static {
		Set<String> s = new HashSet<>(Arrays.asList(
				"sessionstarttime",
				"sessionsampletime",
				"sampletime",
				"samplems",
				"cmsampletime",
				"cmsamplems",
				"cmnewdiffratrow",
				"cmrowstate"
		));
		SKIP_COLS = Collections.unmodifiableSet(s);
	}

	// -------------------------------------------------------------------------
	// Historical H2 connection cache
	//
	// Opening a new H2 file-database connection on every request is expensive.
	// We keep one read-only DbxConnection per historical date key alive for
	// HIST_CONN_TTL_MS of idle time, then close it automatically.
	//
	// The cache is keyed by the date string produced by PersistWriterJdbc
	// (e.g. "2026-03-14" for a daily-rolling database).
	// -------------------------------------------------------------------------

	/** Idle timeout for cached historical connections (default: 10 minutes). */
	private static final long HIST_CONN_TTL_MS = 10 * 60_000L;

	private static class HistConnEntry
	{
		final DbxConnection conn;
		final String        dateKey;
		volatile long       lastUsed = System.currentTimeMillis();

		HistConnEntry(DbxConnection conn, String dateKey)
		{
			this.conn    = conn;
			this.dateKey = dateKey;
		}

		void touch() { lastUsed = System.currentTimeMillis(); }

		boolean isExpired() { return System.currentTimeMillis() - lastUsed > HIST_CONN_TTL_MS; }
	}

	private static final ConcurrentHashMap<String, HistConnEntry> _histConnCache
			= new ConcurrentHashMap<>();

	private static final ScheduledExecutorService _histConnReaper
			= Executors.newSingleThreadScheduledExecutor(r -> {
				Thread t = new Thread(r, "CmDataServlet-HistConnReaper");
				t.setDaemon(true);
				return t;
			});

	static {
		// Reap idle historical connections once per minute
		_histConnReaper.scheduleAtFixedRate(
				CmDataServlet::reapExpiredHistoricalConns, 1, 1, TimeUnit.MINUTES);
	}

	private static void reapExpiredHistoricalConns()
	{
		for (Iterator<Map.Entry<String, HistConnEntry>> it = _histConnCache.entrySet().iterator(); it.hasNext(); )
		{
			Map.Entry<String, HistConnEntry> entry = it.next();
			if (entry.getValue().isExpired())
			{
				it.remove();
				entry.getValue().conn.closeNoThrow();
				_logger.info("CmDataServlet: closed idle historical H2 connection for dateKey='" + entry.getKey() + "'");
			}
		}
	}

	/**
	 * Returns the appropriate DbxConnection for the requested sample timestamp.
	 * <ul>
	 *   <li>If the timestamp falls on the same date as the live database, the writer's
	 *       active connection is returned (no overhead).</li>
	 *   <li>For any other date, a cached read-only connection to the corresponding
	 *       historical H2 file is returned, opening one if necessary.</li>
	 * </ul>
	 * Returns {@code null} if no suitable connection is available.
	 *
	 * @param ts      the requested sample timestamp
	 * @param writer  the active PersistWriterJdbc instance
	 * @return        a DbxConnection, or null
	 */
	static DbxConnection getConnectionForTimestamp(Timestamp ts, PersistWriterJdbc writer)
	{
		// When date-rolling is not configured there is only one database; always use the live conn.
		if (!writer.isH2DateRolling())
			return writer.getStorageConnection();

		String dateKey = writer.formatDateKeyForDb(new Date(ts.getTime()));

		// Same date as the live database — use the live connection directly
		if (dateKey != null && dateKey.equals(writer.getCurrentDbDateKey()))
			return writer.getStorageConnection();

		// Historical date — try the cache first
		if (dateKey != null)
		{
			HistConnEntry cached = _histConnCache.get(dateKey);
			if (cached != null)
			{
				try
				{
					if (!cached.conn.isClosed())
					{
						cached.touch();
						return cached.conn;
					}
				}
				catch (Exception ex) { /* fall through and re-open */ }
				_histConnCache.remove(dateKey);
			}

			// Not cached (or was closed) — open a fresh historical connection
			try
			{
				DbxConnection histConn = writer.openHistoricalConnection(new Date(ts.getTime()));
				if (histConn != null)
				{
					_histConnCache.put(dateKey, new HistConnEntry(histConn, dateKey));
					_logger.info("CmDataServlet: opened historical H2 connection for dateKey='" + dateKey + "'");
					return histConn;
				}
			}
			catch (Exception ex)
			{
				_logger.warn("CmDataServlet: failed to open historical connection for dateKey='" + dateKey + "': " + ex.getMessage(), ex);
			}
		}

		return null;
	}

	@Override
	protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException
	{
		resp.setContentType("application/json");
		resp.setCharacterEncoding("UTF-8");
		ObjectMapper om  = Helper.createObjectMapper();
		PrintWriter  out = resp.getWriter();

		String cmName       = Helper.getParameter(req, "cm");
		String timeParam    = Helper.getParameter(req, "time");
		String typeParam    = Helper.getParameter(req, "type", "abs");
		String showAllParam = Helper.getParameter(req, "showAll", "false");

		if (cmName == null)
		{
			om.writeValue(out, errMap("missing-param", "Missing required parameter: cm"));
			out.flush(); out.close();
			return;
		}
		if (timeParam == null)
		{
			om.writeValue(out, errMap("missing-param", "Missing required parameter: time"));
			out.flush(); out.close();
			return;
		}
		cmName    = cmName.trim();
		timeParam = timeParam.trim();
		typeParam = typeParam.trim().toLowerCase();

		// Validate type
		if (!typeParam.equals("abs") && !typeParam.equals("diff") && !typeParam.equals("rate"))
			typeParam = "abs";

		// Validate CM name — only safe identifier characters allowed
		if (!cmName.matches("[A-Za-z0-9_]+"))
		{
			om.writeValue(out, errMap("invalid-param", "Invalid CM name"));
			out.flush(); out.close();
			return;
		}

		// Parse timestamp — delegates to TimeUtils.parseToTimestampX() which tries
		// multiple formats (ISO-8601, with/without millis, date-only, …) so we
		// handle whatever navSample or the JS slider hands us.
		Timestamp ts;
		try
		{
			ts = TimeUtils.parseToTimestampX(timeParam);
		}
		catch (ParseException ex)
		{
			om.writeValue(out, errMap("invalid-param", "Invalid time format: " + timeParam));
			out.flush(); out.close();
			return;
		}

		// Detect Append CM early — affects SQL strategy and SKIP_COLS behaviour
		boolean isAppend = false;
		if (CounterController.hasInstance())
		{
			CountersModel earlyMeta = CounterController.getInstance().getCmByName(cmName);
			if (earlyMeta instanceof CountersModelAppend) isAppend = true;
		}
		// Append CMs only have _abs tables; force type to abs
		if (isAppend) typeParam = "abs";
		// Trust the client's showAll flag — JS only sends it for Append CMs.
		// Do NOT gate on isAppend here: CounterController may not have the CM
		// registered at request time (e.g. CM not yet active), which would
		// silently disable showAll even when the user has it switched on.
		boolean showAll = "true".equalsIgnoreCase(showAllParam);

		// Get storage connection — live (today) or cached read-only (historical date)
		DbxConnection conn     = null;
		String        noConnMsg = "No storage connection available";
		if (PersistentCounterHandler.hasInstance())
		{
			PersistWriterJdbc writer = PersistentCounterHandler.getInstance().getPersistWriterJdbc();
			if (writer != null)
			{
				conn = getConnectionForTimestamp(ts, writer);
				if (conn == null && writer.isH2DateRolling())
				{
					String dateKey = writer.formatDateKeyForDb(new Date(ts.getTime()));
					String liveKey = writer.getCurrentDbDateKey();
					if (dateKey != null && !dateKey.equals(liveKey))
						noConnMsg = "No historical database found for date '" + dateKey + "'. "
								+ "The .mv.db file may have been deleted or is beyond the retention period.";
				}
			}
		}

		if (conn == null)
		{
			om.writeValue(out, errMap("no-data", noConnMsg));
			out.flush(); out.close();
			return;
		}

		// Table name with [bracket] quoting — translated by quotifySqlString() below
		String tableName = "[" + cmName + "_" + typeParam + "]";

		// Variables populated inside the try block, used when building the JSON response
		List<String>  columns      = null;
		List<Boolean> colTypes     = null;  // true = numeric SQL type (right-align in UI)
		Timestamp     cmSampleTime = null;
		Object        cmSampleMs   = null;

		Timestamp tsFrom = new Timestamp(ts.getTime() - WINDOW_MS);
		Timestamp tsTo   = new Timestamp(ts.getTime() + WINDOW_MS);

		try
		{
			// ------------------------------------------------------------------
			// Step 1: find the SessionSampleTime to use as the query anchor.
			//
			// Regular / showLast: closest timestamp within ±WINDOW_MS.
			// Append + showAll  : latest timestamp at or before 'ts' (covers
			//                     samples where no row was logged at exactly ts).
			// ------------------------------------------------------------------
			Timestamp closest = null;
			if (showAll)
			{
				// Use tsTo (ts + WINDOW_MS) so stored timestamps with sub-second
				// precision (e.g. 14:30:15.234) are not missed when ts has 0 ms.
				String sqlMax = conn.quotifySqlString(
						  "SELECT MAX([SessionSampleTime])"
						+ " FROM " + tableName
						+ " WHERE [SessionSampleTime] <= ?");
				try (PreparedStatement pstmt = conn.prepareStatement(sqlMax))
				{
					pstmt.setTimestamp(1, tsTo);
					try (ResultSet rs = pstmt.executeQuery())
					{
						if (rs.next()) closest = rs.getTimestamp(1);
					}
				}
			}
			else
			{
				String sqlTimes = conn.quotifySqlString(
						  "SELECT DISTINCT [SessionSampleTime]"
						+ " FROM " + tableName
						+ " WHERE [SessionSampleTime] BETWEEN ? AND ?"
						+ " ORDER BY [SessionSampleTime]");
				long minDiff = Long.MAX_VALUE;
				try (PreparedStatement pstmt = conn.prepareStatement(sqlTimes))
				{
					pstmt.setTimestamp(1, tsFrom);
					pstmt.setTimestamp(2, tsTo);
					try (ResultSet rs = pstmt.executeQuery())
					{
						while (rs.next())
						{
							Timestamp t    = rs.getTimestamp(1);
							long      diff = Math.abs(t.getTime() - ts.getTime());
							if (diff < minDiff)
							{
								minDiff = diff;
								closest = t;
							}
						}
					}
				}
			}

			if (closest == null)
			{
				Map<String, Object> noData = errMap("no-data-in-window", "No rows found for this CM at the requested sample time");
				noData.put("cmName",        cmName);
				noData.put("type",          typeParam);
				noData.put("requestedTime", timeParam);
				om.writeValue(out, noData);
				out.flush(); out.close();
				return;
			}

			// ------------------------------------------------------------------
			// Step 2: fetch rows.
			//
			// Regular / showLast: all rows for the exact closest timestamp.
			// Append + showAll  : all rows from session-start up to closest,
			//                     in chronological order (mirrors PersistReader).
			// ------------------------------------------------------------------
			String sqlData;
			if (showAll)
				// Mirror PersistReader.loadSessionCm(): scope to the session that owns 'closest'
				// (SessionStartTime subquery is safe here because 'closest' is an exact value
				// returned by the MAX query above — guaranteed to find rows).
				sqlData = conn.quotifySqlString(
						  "SELECT * FROM " + tableName
						+ " WHERE [SessionStartTime] = (SELECT MIN([SessionStartTime]) FROM " + tableName + " WHERE [SessionSampleTime] = ?)"
						+ "   AND [SessionSampleTime] <= ?"
						+ " ORDER BY [SessionSampleTime]");
			else
				sqlData = conn.quotifySqlString(
						  "SELECT * FROM " + tableName
						+ " WHERE [SessionSampleTime] = ?"
						+ " ORDER BY [SessionSampleTime]");

			List<List<Object>> rows      = new ArrayList<>();
			Map<String, List<String>> rowStates = new LinkedHashMap<>();

			try (PreparedStatement pstmt = conn.prepareStatement(sqlData))
			{
				if (showAll)
				{
					pstmt.setTimestamp(1, closest); // subquery: find SessionStartTime for this sample
					pstmt.setTimestamp(2, closest); // main filter: SessionSampleTime <= closest
				}
				else
					pstmt.setTimestamp(1, closest);
				try (ResultSet rs = pstmt.executeQuery())
				{
					ResultSetMetaData rsMeta = rs.getMetaData();
					int colCount = rsMeta.getColumnCount();

					// Build column list; skip housekeeping columns (see SKIP_COLS javadoc).
					// CmSampleTime and CmSampleMs are captured as info-bar fields, not table cols.
					columns  = new ArrayList<>(colCount);
					colTypes = new ArrayList<>(colCount);
					// Raw JDBC SQL type per non-skipped column — used for type-aware value extraction
					List<Integer> colSqlTypes = new ArrayList<>(colCount);
					Set<Integer> skipIdx         = new HashSet<>();
					int          cmSampleTimeIdx = -1;
					int          cmSampleMsIdx   = -1;
					int          cmRowStateIdx   = -1;
					for (int i = 1; i <= colCount; i++)
					{
						String label = rsMeta.getColumnLabel(i);
						String lower = label.toLowerCase();
						if      ("cmsampletime".equals(lower)) { cmSampleTimeIdx = i; skipIdx.add(i); }
						else if ("cmsamplems"  .equals(lower)) { cmSampleMsIdx   = i; skipIdx.add(i); }
						else if ("cmrowstate"  .equals(lower) || "cmnewdiffratrow".equals(lower)) { cmRowStateIdx = i; skipIdx.add(i); }
						// For Append CMs keep SessionSampleTime as a visible column so
						// the user can see when each event was logged
						else if (SKIP_COLS.contains(lower) && !(isAppend && "sessionsampletime".equals(lower))) { skipIdx.add(i); }
						else
						{
							columns.add(label);
							int sqlType = rsMeta.getColumnType(i);
							colTypes.add(isNumericSqlType(sqlType));
							colSqlTypes.add(sqlType);
						}
					}

					// Map each ResultSet column index to its position in colSqlTypes
					int[] colIdxToSqlTypeIdx = new int[colCount + 1]; // 1-based
					{
						int listIdx = 0;
						for (int i = 1; i <= colCount; i++)
							colIdxToSqlTypeIdx[i] = skipIdx.contains(i) ? -1 : listIdx++;
					}

					// ------------------------------------------------------------------
					// DCC (Dictionary Compressed Columns) resolution.
					// Columns whose label ends with "$dcc$" store a hash ID instead
					// of the real value.  The actual values live in a lookup table
					// named "{cmName}$dcc${originColName}" with columns hashId / colVal.
					// We pre-fetch each lookup table once before iterating rows, then
					// substitute on the fly.  The "$dcc$" suffix is stripped from the
					// column name so the UI sees the plain column name.
					// ------------------------------------------------------------------
					// colIdx (0-based position in `columns`) -> hashId->colVal map
					Map<Integer, Map<String, String>> dccByColIdx = new HashMap<>();
					for (int colIdx = 0; colIdx < columns.size(); colIdx++)
					{
						String colName = columns.get(colIdx);
						if (colName.endsWith("$dcc$"))
						{
							String originName = colName.substring(0, colName.length() - 5);
							String dccTable   = cmName + "$dcc$" + originName;
							Map<String, String> lookup = loadDccLookup(conn, dccTable);
							dccByColIdx.put(colIdx, lookup);
							columns.set(colIdx, originName); // strip suffix for display
						}
					}

					boolean firstRow = true;
					while (rs.next())
					{
						if (firstRow)
						{
							firstRow = false;
							if (cmSampleTimeIdx > 0) cmSampleTime = rs.getTimestamp(cmSampleTimeIdx);
							if (cmSampleMsIdx   > 0) cmSampleMs   = rs.getObject(cmSampleMsIdx);
						}
						List<Object> row = new ArrayList<>(colCount);
						for (int i = 1; i <= colCount; i++)
						{
							if (skipIdx.contains(i)) continue;
							int    colListIdx = colIdxToSqlTypeIdx[i];
							int    sqlType    = colSqlTypes.get(colListIdx);
							Object val        = extractColValue(rs, i, sqlType);
							// DCC substitution: replace hash ID with real value if available
							if (val instanceof String && dccByColIdx.containsKey(colListIdx))
							{
								String resolved = dccByColIdx.get(colListIdx).get((String) val);
								if (resolved != null) val = resolved;
							}
							row.add(val);
						}
						// Capture CmRowState as a sparse rowStates entry (only when non-zero)
						if (cmRowStateIdx > 0)
						{
							int cmRowState = rs.getInt(cmRowStateIdx);
							if (cmRowState != 0)
							{
								List<String> states = new ArrayList<>();
								if ((cmRowState & CountersModel.ROW_STATE__IS_DIFF_OR_RATE_ROW) != 0) states.add("firstDiffOrRateRow");
								if ((cmRowState & CountersModel.ROW_STATE__IS_AGGREGATED_ROW)   != 0) states.add("aggRow");
								if (!states.isEmpty()) rowStates.put(String.valueOf(rows.size()), states);
							}
						}
						rows.add(row);
					}
				}
			}

			// CM metadata: diff/pct columns + per-column tooltips
			//
			// Diff/Pct columns:
			//   Regular CMs store column names in String[] arrays (getDiffColumns / getPctColumns).
			//   HostMonitor CMs (CounterModelHostMonitor) don't populate those arrays — they
			//   override isDiffColumn(int) / isPctColumn(int) to delegate to HostMonitorMetaData.
			//   Strategy: use String[] getters first (fast, index-safe); if empty, fall back to
			//   the polymorphic per-column methods with the CM's own column model indices.
			//
			// Tooltips:
			//   getToolTipTextOnTableColumnHeader(String) is polymorphic and takes a name (not
			//   an index), so it's safe for all CM types. CounterModelHostMonitor overrides it
			//   to read from HostMonitorMetaData.getDescription().
			//   For regular CMs _cmToolTipSupplier may be null in NoGUI/collector mode, so we
			//   add a manual fallback via getLocalToolTipTextOnTableColumnHeader + MonTablesDictionary.
			SimpleDateFormat outFmt = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS");
			List<String> diffCols = new ArrayList<>();
			List<String> pctCols  = new ArrayList<>();
			List<String> tooltips = new ArrayList<>(columns.size());
			CountersModel cmMeta  = null;
			if (CounterController.hasInstance())
			{
				ICounterController cc2 = CounterController.getInstance();
				cmMeta = cc2.getCmByName(cmName);
				if (cmMeta != null)
				{
					// --- Diff/Pct columns ---
					// Try String[] getters first (works for regular CMs, returns null for HostMonitor)
					Set<String> diffSet = new HashSet<>();
					Set<String> pctSet  = new HashSet<>();
					if (cmMeta.getDiffColumns() != null)
						for (String d : cmMeta.getDiffColumns()) diffSet.add(d);
					if (cmMeta.getPctColumns() != null)
						for (String p : cmMeta.getPctColumns()) pctSet.add(p);

					if (diffSet.isEmpty() && pctSet.isEmpty())
					{
						// Fallback for HostMonitor CMs: use the polymorphic per-column methods.
						// These take an int index into the CM's internal column model. For
						// HostMonitor, the columns list returned by the DB query matches the
						// HostMonitorMetaData column order (no housekeeping prefix to strip).
						// For safety, iterate over the CM's own column list and match by name.
						try
						{
							List<String> cmColNames = cmMeta.getColNames(CountersModel.DATA_ABS);
							if (cmColNames != null)
							{
								for (int ci = 0; ci < cmColNames.size(); ci++)
								{
									String cn = cmColNames.get(ci);
									try { if (cmMeta.isDiffColumn(ci)) diffSet.add(cn); } catch (Exception ignored) {}
									try { if (cmMeta.isPctColumn(ci))  pctSet.add(cn);  } catch (Exception ignored) {}
								}
							}
						}
						catch (Exception ignored) {}
					}

					// Intersect with the actual columns returned by the query
					for (String col : columns)
					{
						if (diffSet.contains(col)) diffCols.add(col);
						if (pctSet.contains(col))  pctCols.add(col);
					}

					// --- Tooltips ---
					for (String col : columns)
					{
						// Polymorphic: HostMonitor overrides to use HostMonitorMetaData.getDescription()
						String tt = null;
						try { tt = cmMeta.getToolTipTextOnTableColumnHeader(col); } catch (Exception ignored) {}
						// Fallback for regular CMs where _cmToolTipSupplier is null in NoGUI mode
						if (tt == null || tt.trim().isEmpty())
						{
							tt = cmMeta.getLocalToolTipTextOnTableColumnHeader(col);
							if (tt == null || tt.trim().isEmpty())
							{
								try { tt = MonTablesDictionaryManager.getInstance().getDescription(cmMeta.getMonTablesInQuery(), col); }
								catch (Exception ignored) {}
							}
						}
						tooltips.add(tt);
					}
				}
			}
			// Pad tooltips list so it always has the same length as columns (null = no tooltip)
			while (tooltips.size() < columns.size()) tooltips.add(null);

			// Aggregate columns: per-column aggregation type declared by the CM
			Map<String, String> aggregateCols = null;
			if (cmMeta != null)
			{
				Map<String, CountersModel.AggregationType> aggMap = cmMeta.getAggregateColumns();
				if (aggMap != null && !aggMap.isEmpty())
				{
					aggregateCols = new LinkedHashMap<>();
					for (Map.Entry<String, CountersModel.AggregationType> e : aggMap.entrySet())
						aggregateCols.put(e.getKey(), e.getValue().getAggregationType().name());
				}
			}

			// Normalise rows: Timestamp → formatted string, byte[] → hex, null → null
			List<List<Object>> normRows = new ArrayList<>(rows.size());
			for (List<Object> row : rows)
			{
				List<Object> normRow = new ArrayList<>(row.size());
				for (Object val : row)
					normRow.add(normalizeRowValue(val, outFmt));
				normRows.add(normRow);
			}

			// Chart / highlighter descriptors (declared by the CM for the web UI)
			List<CmChartDescriptor>       chartDescriptors       = null;
			List<CmHighlighterDescriptor> highlighterDescriptors = null;
			String description = null;
			if (CounterController.hasInstance())
			{
				CountersModel cmMeta2 = CounterController.getInstance().getCmByName(cmName);
				if (cmMeta2 != null)
				{
					description            = cmMeta2.getDescription();
					chartDescriptors       = cmMeta2.getChartDescriptors();
					highlighterDescriptors = cmMeta2.getHighlighterDescriptors();
				}
			}

			// Build JSON response via Jackson
			Map<String, Object> root = new LinkedHashMap<>();
			root.put("cmName",        cmName);
			root.put("isAppend",      isAppend);
			root.put("showAll",       showAll);
			root.put("type",          typeParam);
			root.put("requestedTime", timeParam);
			root.put("resolvedTime",  outFmt.format(closest));
			root.put("cmSampleTime",  cmSampleTime != null ? outFmt.format(cmSampleTime) : null);
			root.put("cmSampleMs",    cmSampleMs);
			root.put("rowCount",      rows.size());
			root.put("diffColumns",   diffCols);
			root.put("pctColumns",    pctCols);
			root.put("isNumeric",     colTypes);
			root.put("tooltips",      tooltips);
			root.put("columns",       columns);
			root.put("rows",          normRows);
			if (!rowStates.isEmpty())  root.put("rowStates",        rowStates);
			if (aggregateCols != null) root.put("aggregateColumns", aggregateCols);
			if (description            != null) root.put("description",            description);
			if (chartDescriptors       != null) root.put("chartDescriptors",       chartDescriptors);
			if (highlighterDescriptors != null) root.put("highlighterDescriptors", highlighterDescriptors);

			// Preferred column order (declared by the CM for a better visual layout)
			if (cmMeta != null)
			{
				LinkedHashMap<String, ColumnHeaderPropsEntry> prefCols = cmMeta.getPreferredColumnProps();
				if (prefCols != null && !prefCols.isEmpty())
				{
					List<Map<String, Object>> prefList = new ArrayList<>();
					for (ColumnHeaderPropsEntry e : prefCols.values())
					{
						Map<String, Object> entry = new LinkedHashMap<>();
						entry.put("columnName", e.getColumnName());
						entry.put("viewPos",    e.getViewPos());
						entry.put("visible",    e.isVisible());
						prefList.add(entry);
					}
					root.put("preferredColumnOrder", prefList);
				}
			}

			om.writeValue(out, root);
			out.flush();
			out.close();
		}
		catch (Exception ex)
		{
			String msg = ex.getMessage();
			Map<String, Object> errResp;
			if (msg != null && (msg.contains("not found") || msg.contains("Table") || msg.contains("table")))
			{
				_logger.debug("CmDataServlet: table " + tableName + " not found: " + msg);
				errResp = errMap("table-not-found", "Table " + tableName + " not found");
			}
			else
			{
				_logger.warn("CmDataServlet: error querying " + tableName + ": " + msg, ex);
				errResp = errMap("query-error", msg);
			}
			errResp.put("cmName",        cmName);
			errResp.put("type",          typeParam);
			errResp.put("requestedTime", timeParam);
			om.writeValue(out, errResp);
			out.flush();
			out.close();
		}
	}

	/**
	 * Type-aware ResultSet column extractor.
	 * Mirrors {@code ResultSetTableModel.getDataValue()} so all JDBC types are handled
	 * consistently across the GUI and the CM Data servlet.
	 *
	 * <ul>
	 *   <li>BINARY / VARBINARY / LONGVARBINARY / BLOB  → hex string (e.g. {@code 0xDEADBEEF})</li>
	 *   <li>CLOB / NCLOB                               → plain string</li>
	 *   <li>BOOLEAN / BIT                              → {@link Boolean}</li>
	 *   <li>DATE / TIME / TIMESTAMP                    → typed java.sql objects (formatted later)</li>
	 *   <li>SQL_VARIANT (-156, MS SQL Server)          → hex if byte[], else string</li>
	 *   <li>Everything else                            → {@code rs.getObject()}</li>
	 * </ul>
	 */
	private static Object extractColValue(ResultSet rs, int col, int sqlType) throws java.sql.SQLException, java.io.IOException
	{
		switch (sqlType)
		{
			case Types.BINARY:
			case Types.VARBINARY:
			case Types.LONGVARBINARY:
			{
				byte[] bytes = rs.getBytes(col);
				return bytes == null ? null : bytesToHex(bytes);
			}
			case Types.BLOB:
			{
				Blob blob = rs.getBlob(col);
				if (blob == null) return null;
				try { return bytesToHex(blob.getBytes(1, (int) Math.min(blob.length(), 65536))); }
				finally { try { blob.free(); } catch (Exception ignored) {} }
			}
			case Types.CLOB:
			case Types.NCLOB:
			{
				Clob clob = rs.getClob(col);
				if (clob == null) return null;
				try
				{
					long len = Math.min(clob.length(), 65536);
					char[] buf = new char[(int) len];
					try (Reader r = clob.getCharacterStream()) { r.read(buf); }
					return new String(buf, 0, (int) len);
				}
				finally { try { clob.free(); } catch (Exception ignored) {} }
			}
			case Types.BOOLEAN:
			case Types.BIT:
				return rs.wasNull() ? null : rs.getBoolean(col);
			case -156: // microsoft.sql.Types.SQL_VARIANT (SQL Server)
			{
				Object obj = rs.getObject(col);
				if (obj == null)            return null;
				if (obj instanceof byte[])  return bytesToHex((byte[]) obj);
				return obj.toString();
			}
			default:
				return rs.getObject(col);
		}
	}

	/**
	 * Converts a byte array to a hex string with {@code 0x} prefix, lowercase.
	 * Mirrors {@code StringUtil.bytesToHex()} to avoid a cross-package dependency.
	 */
	private static String bytesToHex(byte[] bytes)
	{
		if (bytes == null) return null;
		StringBuilder sb = new StringBuilder(2 + bytes.length * 2);
		sb.append("0x");
		for (byte b : bytes)
			sb.append(String.format("%02x", b));
		return sb.toString();
	}

	/** Returns true if the given {@link java.sql.Types} value represents a numeric type. */
	private static boolean isNumericSqlType(int sqlType)
	{
		switch (sqlType)
		{
			case java.sql.Types.TINYINT:
			case java.sql.Types.SMALLINT:
			case java.sql.Types.INTEGER:
			case java.sql.Types.BIGINT:
			case java.sql.Types.FLOAT:
			case java.sql.Types.REAL:
			case java.sql.Types.DOUBLE:
			case java.sql.Types.NUMERIC:
			case java.sql.Types.DECIMAL:
				return true;
			default:
				return false;
		}
	}

	/**
	 * Loads a DCC (Dictionary Compressed Column) lookup table into a hash map.
	 *
	 * <p>The lookup table is named {@code {cmName}$dcc${originColName}} and has
	 * two columns: {@code hashId} (the hash stored in the main table) and
	 * {@code colVal} (the actual string value).
	 *
	 * @param conn      active DB connection (live or historical H2)
	 * @param tableName the raw DCC lookup table name (e.g. {@code CmSysmon$dcc$SomeName}); brackets are added here
	 * @return map from hashId to colVal; empty map if table is missing or on error
	 */
	private static Map<String, String> loadDccLookup(DbxConnection conn, String tableName)
	{
		Map<String, String> map = new LinkedHashMap<>();
		String sql = conn.quotifySqlString("SELECT [hashId], [colVal] FROM [" + tableName + "]");
		try (PreparedStatement pstmt = conn.prepareStatement(sql);
		     ResultSet rs = pstmt.executeQuery())
		{
			while (rs.next())
			{
				String hashId = rs.getString(1);
				String colVal = rs.getString(2);
				if (hashId != null)
					map.put(hashId, colVal != null ? colVal : "");
			}
		}
		catch (Exception ex)
		{
			// DCC table missing or inaccessible — fall back to showing raw hash IDs
			_logger.debug("loadDccLookup: could not load '{}': {}", tableName, ex.getMessage());
		}
		return map;
	}

	/**
	 * Normalises a raw row value to a type Jackson will serialise correctly.
	 * Preserves the same output format as the previous manual StringBuilder approach.
	 */
	private static Object normalizeRowValue(Object val, SimpleDateFormat fmt)
	{
		if (val == null)               return null;
		if (val instanceof Timestamp)  return fmt.format((Timestamp) val);
		if (val instanceof Number)     return val;
		if (val instanceof Boolean)    return val;
		if (val instanceof byte[])     return bytesToHex((byte[]) val);
		return val.toString();
	}

	/** Builds a simple error response map for Jackson serialisation. */
	private static Map<String, Object> errMap(String code, String message)
	{
		Map<String, Object> m = new LinkedHashMap<>();
		m.put("error",   code);
		m.put("message", message);
		return m;
	}
}
