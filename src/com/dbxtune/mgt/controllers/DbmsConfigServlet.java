/*******************************************************************************
 * Copyright (C) 2010-2025 Goran Schwarz
 *
 * This file is part of DbxTune
 * DbxTune is a family of sub-products *Tune, hence the Dbx
 *
 * DbxTune is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, version 3 of the License.
 ******************************************************************************/
package com.dbxtune.mgt.controllers;

import java.io.IOException;
import java.io.PrintWriter;
import java.lang.invoke.MethodHandles;
import java.sql.Clob;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.Timestamp;
import java.sql.Types;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.dbxtune.config.dbms.DbmsConfigTextManager;
import com.dbxtune.config.dbms.IDbmsConfigText;
import com.dbxtune.pcs.PersistentCounterHandler;
import com.dbxtune.pcs.PersistWriterBase;
import com.dbxtune.pcs.PersistWriterJdbc;
import com.dbxtune.sql.conn.DbxConnection;

import com.dbxtune.central.controllers.Helper;
import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * Serves DBMS configuration data stored in the PCS for a given server and timestamp.
 *
 * <pre>GET /api/dbms-config?srv=SERVERNAME&amp;ts=YYYY-MM-DD+HH:mm:ss</pre>
 *
 * <p>Response JSON:
 * <pre>{
 *   "srv": "...",
 *   "ts": "...",
 *   "resolvedTs": "...",
 *   "params":  { "columns": [...], "rows": [[...], ...] },
 *   "texts":   [ {"configName":"...", "configText":"..."}, ... ],
 *   "issues":  { "columns": [...], "rows": [[...], ...] }
 * }</pre>
 *
 * <p>Routes to the live H2 connection for today's date, or to a historical
 * read-only H2 file for past dates — exactly as {@link CmDataServlet} does.
 *
 * <p>Table names:
 * <ul>
 *   <li>MonSessionDbmsConfig       — DBMS configuration parameters (one row per parameter)</li>
 *   <li>MonSessionDbmsConfigText   — Text-based configuration snippets (CLOB per configName)</li>
 *   <li>MonSessionDbmsConfigIssues — Configuration issues detected by the collector</li>
 * </ul>
 * Older recordings may use the legacy names MonSessionAseConfig / MonSessionAseConfigText;
 * the servlet tries the new name first and falls back automatically.
 */
public class DbmsConfigServlet extends HttpServlet
{
	private static final long serialVersionUID = 1L;
	private static final Logger _logger = LogManager.getLogger(MethodHandles.lookup().lookupClass());

	// NOTE: The below is OVERKILL since, fallback wont happen (but keep it for now)
	// --- table-name fallback chains (try primary name first, then legacy) ----------
	private static final String[] PARAM_TABLES  = { "MonSessionDbmsConfig" };      // OR: Should we get the table name from PCS -- PersistWriterBase.getTableName(PersistWriterBase.SESSION_DBMS_CONFIG)
	private static final String[] TEXT_TABLES   = { "MonSessionDbmsConfigText" };  // OR: Should we get the table name from PCS -- PersistWriterBase.getTableName(PersistWriterBase.SESSION_DBMS_CONFIG_TEXT)
	private static final String[] ISSUES_TABLES = { "MonSessionDbmsConfigIssues" };// OR: Should we get the table name from PCS -- PersistWriterBase.getTableName(PersistWriterBase.SESSION_DBMS_CONFIG_ISSUES)

	// NOTE: The below is OVERKILL since the PCS Storage will probably hold just 24 Hours of data (and will most likely just hold 1 DBMS Configuration)
	// How far back (in ms) to look for the most-recent SessionStartTime <= requested ts.
	// DBMS Config is only captured at collector restart, so a session can run for many
	// days. We look back up to 30 days to find the last session start before the
	// requested timestamp (and accept up to +5 min ahead to handle clock skew / live mode).
	private static final long SEARCH_WINDOW_BACK_MS    = 30L * 24 * 3600 * 1000; // 30 days back
	private static final long SEARCH_WINDOW_FORWARD_MS =       5  *   60 * 1000; // 5 min  ahead

	/**
	 * Housekeeping columns present in PCS tables that are never interesting to display.
	 * All comparisons are done case-insensitively.
	 */
	private static final java.util.Set<String> SKIP_COLUMNS = new java.util.HashSet<>(java.util.Arrays.asList(
			"sessionstarttime",
			"srvrestartdate",
			"discarded"
	));

	@Override
	protected void doGet(HttpServletRequest req, HttpServletResponse resp)
	throws ServletException, IOException
	{
		resp.setContentType("application/json");
		resp.setCharacterEncoding("UTF-8");
		ObjectMapper om  = Helper.createObjectMapper();
		PrintWriter  out = resp.getWriter();

		String srvParam = Helper.getParameter(req, "srv", "");
		String tsParam  = Helper.getParameter(req, "ts");

		if (tsParam == null)
		{
			om.writeValue(out, errMap("missing-param", "Missing required parameter: ts"));
			out.flush(); out.close();
			return;
		}

		srvParam = srvParam.trim();
		tsParam  = tsParam.trim().replace('+', ' ');

		Timestamp ts = null;
		try
		{
			Date d = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").parse(tsParam);
			ts = new Timestamp(d.getTime());
		}
		catch (ParseException ex)
		{
			om.writeValue(out, errMap("bad-param", "Cannot parse ts: " + tsParam));
			out.flush(); out.close();
			return;
		}

		// Resolve connection (live or historical H2)
		DbxConnection conn = null;
		if (PersistentCounterHandler.hasInstance())
		{
			PersistWriterJdbc writer = PersistentCounterHandler.getInstance().getPersistWriterJdbc();
			if (writer != null)
				conn = CmDataServlet.getConnectionForTimestamp(ts, writer);
		}
		if (conn == null)
		{
			om.writeValue(out, errMap("no-data", "No storage connection available"));
			out.flush(); out.close();
			return;
		}

		try
		{
			// Find closest SessionStartTime in the ±6-hour window
			Timestamp resolvedTs = findClosestSessionStart(conn, ts);
			if (resolvedTs == null)
			{
				om.writeValue(out, errMap("no-data",
						"No DBMS configuration data found near " + tsParam));
				out.flush(); out.close();
				return;
			}

			String outTs = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(resolvedTs);

			Map<String, Object> root = new LinkedHashMap<>();
			root.put("srv",        srvParam);
			root.put("ts",         tsParam);
			root.put("resolvedTs", outTs);
			root.put("params",     buildParamsMap (conn, resolvedTs));
			root.put("texts",      buildTextsList (conn, resolvedTs));
			root.put("issues",     buildIssuesMap (conn, resolvedTs));

			om.writeValue(out, root);
			out.flush(); out.close();
		}
		catch (Exception ex)
		{
			_logger.warn("DbmsConfigServlet: error: " + ex.getMessage(), ex);
			om.writeValue(out, errMap("query-error", ex.getMessage()));
			out.flush(); out.close();
		}
	}

	// -------------------------------------------------------------------------
	// Find closest SessionStartTime
	// -------------------------------------------------------------------------

	/**
	 * Find the most-recent SessionStartTime that is &lt;= the requested timestamp ts
	 * (with a small forward tolerance for clock skew / live-mode).
	 * <p>
	 * DBMS Config is only captured at collector restart, so the same SessionStartTime
	 * may be the correct answer for a slider position that is days after the restart.
	 * We look back up to 30 days and up to 5 minutes ahead.
	 */
	private Timestamp findClosestSessionStart(DbxConnection conn, Timestamp ts) throws Exception
	{
		Timestamp tsFrom = new Timestamp(ts.getTime() - SEARCH_WINDOW_BACK_MS);
		Timestamp tsTo   = new Timestamp(ts.getTime() + SEARCH_WINDOW_FORWARD_MS);

		for (String tableName : PARAM_TABLES)
		{
			try
			{
				// Return the LATEST SessionStartTime that is still <= ts (+tolerance).
				// "ORDER BY SessionStartTime DESC" + "LIMIT 1 / FETCH FIRST 1 ROWS ONLY"
				// is not portable, so we fetch all candidates and pick in Java.
				String sql = conn.quotifySqlString(
						  "SELECT DISTINCT [SessionStartTime]"
						+ " FROM [" + tableName + "]"
						+ " WHERE [SessionStartTime] BETWEEN ? AND ?"
						+ " ORDER BY [SessionStartTime]");

				try (PreparedStatement ps = conn.prepareStatement(sql))
				{
					ps.setTimestamp(1, tsFrom);
					ps.setTimestamp(2, tsTo);
					try (ResultSet rs = ps.executeQuery())
					{
						// Pick the most-recent start that is still <= ts.
						// If none is <= ts (rare clock-skew case), fall back to the closest one.
						Timestamp best     = null;
						Timestamp fallback = null;
						long minDiff = Long.MAX_VALUE;
						while (rs.next())
						{
							Timestamp t = rs.getTimestamp(1);
							if (!t.after(ts)) // t <= ts  →  prefer the latest such value
							{
								if (best == null || t.after(best))
									best = t;
							}
							else // t > ts  →  only use as fallback
							{
								long diff = t.getTime() - ts.getTime();
								if (diff < minDiff) { minDiff = diff; fallback = t; }
							}
						}
						Timestamp result = (best != null) ? best : fallback;
						if (result != null)
							return result;
					}
				}
			}
			catch (Exception ex)
			{
				_logger.debug("DbmsConfigServlet.findClosestSessionStart: table '{}' unavailable: {}",
						tableName, ex.getMessage());
			}
		}
		return null;
	}

	// -------------------------------------------------------------------------
	// Parameters: SELECT * FROM MonSessionDbmsConfig WHERE SessionStartTime = ?
	// Skip SessionStartTime column in the JSON output (housekeeping, already in resolvedTs)
	// -------------------------------------------------------------------------

	private Map<String, Object> buildParamsMap(DbxConnection conn, Timestamp ts) throws Exception
	{
		for (String tableName : PARAM_TABLES)
		{
			try
			{
				String sql = conn.quotifySqlString(
						  "SELECT * FROM [" + tableName + "]"
						+ " WHERE [SessionStartTime] = ?");
						// No ORDER BY — column names differ per DBMS vendor; user can sort via column headers

				List<String>       columns = new ArrayList<>();
				List<List<Object>> rows    = new ArrayList<>();

				try (PreparedStatement ps = conn.prepareStatement(sql))
				{
					ps.setTimestamp(1, ts);
					try (ResultSet rs = ps.executeQuery())
					{
						ResultSetMetaData meta     = rs.getMetaData();
						int               colCount = meta.getColumnCount();

						// Build column list — skip SessionStartTime
						List<Integer> useIdx = new ArrayList<>();
						for (int i = 1; i <= colCount; i++)
						{
							String label = meta.getColumnLabel(i);
							if (SKIP_COLUMNS.contains(label.toLowerCase())) continue;
							columns.add(label);
							useIdx.add(i);
						}

						while (rs.next())
						{
							List<Object> row = new ArrayList<>();
							for (int idx : useIdx)
							{
								int sqlType = meta.getColumnType(idx);
								row.add(normalizeValue(readValue(rs, idx, sqlType)));
							}
							rows.add(row);
						}
					}
				}

				Map<String, Object> result = new LinkedHashMap<>();
				result.put("columns", columns);
				result.put("rows",    rows);
				return result;
			}
			catch (Exception ex)
			{
				_logger.debug("DbmsConfigServlet.buildParamsMap: table '{}' unavailable: {}",
						tableName, ex.getMessage());
			}
		}
		Map<String, Object> empty = new LinkedHashMap<>();
		empty.put("columns", new ArrayList<>());
		empty.put("rows",    new ArrayList<>());
		return empty;
	}

	// -------------------------------------------------------------------------
	// Text snippets: SELECT configName, configText FROM MonSessionDbmsConfigText
	// -------------------------------------------------------------------------

	private List<Map<String, Object>> buildTextsList(DbxConnection conn, Timestamp ts) throws Exception
	{
		List<Map<String, Object>> list = new ArrayList<>();

		for (String tableName : TEXT_TABLES)
		{
			try
			{
				String sql = conn.quotifySqlString(
						  "SELECT [configName], [configText]"
						+ " FROM [" + tableName + "]"
						+ " WHERE [SessionStartTime] = ?");
						// Note: no ORDER BY — rows come back in collector-insertion order,
						// which matches the DbmsConfigTextManager registration order.

				try (PreparedStatement ps = conn.prepareStatement(sql))
				{
					ps.setTimestamp(1, ts);
					try (ResultSet rs = ps.executeQuery())
					{
						while (rs.next())
						{
							String configName = rs.getString(1);
							String configText = readClob(rs, 2);

							// Translate configName to human-readable tabLabel via the registry.
							// Falls back to configName if the instance isn't registered (e.g. different DBMS type).
							String tabLabel = configName;
							IDbmsConfigText inst = DbmsConfigTextManager.getInstance(configName);
							if (inst != null)
								tabLabel = inst.getTabLabel();

							Map<String, Object> entry = new LinkedHashMap<>();
							entry.put("configName", configName);
							entry.put("tabLabel",   tabLabel);
							entry.put("configText", configText);
							list.add(entry);
						}
					}
				}
				break; // found working table — stop
			}
			catch (Exception ex)
			{
				_logger.debug("DbmsConfigServlet.buildTextsList: table '{}' unavailable: {}",
						tableName, ex.getMessage());
			}
		}

		return list;
	}

	// -------------------------------------------------------------------------
	// Issues: SELECT ... FROM MonSessionDbmsConfigIssues WHERE SessionStartTime = ?
	//         AND Discarded = 0
	// -------------------------------------------------------------------------

	private Map<String, Object> buildIssuesMap(DbxConnection conn, Timestamp ts) throws Exception
	{
		for (String tableName : ISSUES_TABLES)
		{
			try
			{
				// Try with Discarded filter first; fall back to plain WHERE if the column doesn't exist
				List<String>       columns = new ArrayList<>();
				List<List<Object>> rows    = new ArrayList<>();
				boolean            loaded  = false;

				for (String whereClause : new String[] {
						" WHERE [SessionStartTime] = ? AND [Discarded] = 0",
						" WHERE [SessionStartTime] = ?" })
				{
					if (loaded) break;
					try
					{
						String sql = conn.quotifySqlString(
								"SELECT * FROM [" + tableName + "]" + whereClause);

						columns.clear();
						rows.clear();

						try (PreparedStatement ps = conn.prepareStatement(sql))
						{
							ps.setTimestamp(1, ts);
							try (ResultSet rs = ps.executeQuery())
							{
								ResultSetMetaData meta     = rs.getMetaData();
								int               colCount = meta.getColumnCount();

								// Build column list — skip housekeeping columns
								List<Integer> useIdx = new ArrayList<>();
								for (int i = 1; i <= colCount; i++)
								{
									String label = meta.getColumnLabel(i);
									if (SKIP_COLUMNS.contains(label.toLowerCase())) continue;
									columns.add(label);
									useIdx.add(i);
								}

								while (rs.next())
								{
									List<Object> row = new ArrayList<>();
									for (int idx : useIdx)
									{
										int sqlType = meta.getColumnType(idx);
										row.add(normalizeValue(readValue(rs, idx, sqlType)));
									}
									rows.add(row);
								}
							}
						}
						loaded = true;
					}
					catch (Exception inner)
					{
						_logger.debug("DbmsConfigServlet.buildIssuesMap: query variant failed for '{}': {}",
								tableName, inner.getMessage());
					}
				}

				if (loaded)
				{
					Map<String, Object> result = new LinkedHashMap<>();
					result.put("columns", columns);
					result.put("rows",    rows);
					return result;
				}
			}
			catch (Exception ex)
			{
				_logger.debug("DbmsConfigServlet.buildIssuesMap: table '{}' unavailable: {}",
						tableName, ex.getMessage());
			}
		}
		Map<String, Object> empty = new LinkedHashMap<>();
		empty.put("columns", new ArrayList<>());
		empty.put("rows",    new ArrayList<>());
		return empty;
	}

	// -------------------------------------------------------------------------
	// Helpers
	// -------------------------------------------------------------------------

	/**
	 * Normalises a raw DB value to a type Jackson will serialise correctly.
	 * <ul>
	 *   <li>null     → null  (JSON null)</li>
	 *   <li>Boolean  → Boolean (JSON true/false)</li>
	 *   <li>Number   → Number  (JSON number)</li>
	 *   <li>anything else (Timestamp, Date, …) → String via toString()</li>
	 * </ul>
	 */
	private static Object normalizeValue(Object v)
	{
		if (v == null)            return null;
		if (v instanceof Boolean) return v;
		if (v instanceof Number)  return v;
		return v.toString();
	}

	private static Object readValue(ResultSet rs, int col, int sqlType) throws Exception
	{
		switch (sqlType)
		{
			case Types.BOOLEAN:
			case Types.BIT:
				boolean b = rs.getBoolean(col);
				return rs.wasNull() ? null : b;
			case Types.CLOB:
			case Types.NCLOB:
				return readClob(rs, col);
			default:
				return rs.getObject(col);
		}
	}

	private static String readClob(ResultSet rs, int col) throws Exception
	{
		Object obj = rs.getObject(col);
		if (obj == null) return null;
		if (obj instanceof Clob)
		{
			Clob clob = (Clob) obj;
			try
			{
				// Read full CLOB with no size cap
				StringBuilder sb2 = new StringBuilder((int) Math.min(clob.length(), 65536L));
				char[] buf = new char[8192];
				int read;
				try (java.io.Reader r = clob.getCharacterStream())
				{
					while ((read = r.read(buf)) != -1)
						sb2.append(buf, 0, read);
				}
				return sb2.toString();
			}
			finally { try { clob.free(); } catch (Exception ignored) {} }
		}
		return obj.toString();
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
