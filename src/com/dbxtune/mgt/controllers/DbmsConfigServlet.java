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
import java.util.List;

import javax.servlet.ServletException;
import javax.servlet.ServletOutputStream;
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
		ServletOutputStream out = resp.getOutputStream();

		String srvParam = req.getParameter("srv");
		String tsParam  = req.getParameter("ts");

		// srv is optional — the collector echoes it back in the response for the UI
		if (srvParam == null) srvParam = "";
		if (tsParam == null || tsParam.trim().isEmpty())
		{
			out.println("{\"error\":\"missing-param\",\"message\":\"Missing required parameter: ts\"}");
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
			out.println("{\"error\":\"bad-param\",\"message\":" + jsonStr("Cannot parse ts: " + tsParam) + "}");
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
			out.println("{\"error\":\"no-data\",\"message\":\"No storage connection available\"}");
			out.flush(); out.close();
			return;
		}

		try
		{
			// Find closest SessionStartTime in the ±6-hour window
			Timestamp resolvedTs = findClosestSessionStart(conn, ts);
			if (resolvedTs == null)
			{
				out.println("{\"error\":\"no-data\",\"message\":"
						+ jsonStr("No DBMS configuration data found near " + tsParam) + "}");
				out.flush(); out.close();
				return;
			}

			String outTs = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(resolvedTs);

			StringBuilder sb = new StringBuilder(65536);
			sb.append("{");
			sb.append("\"srv\":").append(jsonStr(srvParam)).append(",");
			sb.append("\"ts\":").append(jsonStr(tsParam)).append(",");
			sb.append("\"resolvedTs\":").append(jsonStr(outTs)).append(",");

			sb.append("\"params\":");  appendParamsJson (sb, conn, resolvedTs); sb.append(",");
			sb.append("\"texts\":");   appendTextsJson  (sb, conn, resolvedTs); sb.append(",");
			sb.append("\"issues\":");  appendIssuesJson (sb, conn, resolvedTs);
			sb.append("}");

			out.println(sb.toString());
			out.flush(); out.close();
		}
		catch (Exception ex)
		{
			_logger.warn("DbmsConfigServlet: error: " + ex.getMessage(), ex);
			out.println("{\"error\":\"query-error\",\"message\":" + jsonStr(ex.getMessage()) + "}");
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

	private void appendParamsJson(StringBuilder sb, DbxConnection conn, Timestamp ts) throws Exception
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
								row.add(readValue(rs, idx, sqlType));
							}
							rows.add(row);
						}
					}
				}

				sb.append("{\"columns\":[");
				for (int i = 0; i < columns.size(); i++) { if (i > 0) sb.append(","); sb.append(jsonStr(columns.get(i))); }
				sb.append("],\"rows\":[");
				appendRowsJson(sb, rows);
				sb.append("]}");
				return; // success — stop trying fallback names
			}
			catch (Exception ex)
			{
				_logger.debug("DbmsConfigServlet.appendParamsJson: table '{}' unavailable: {}",
						tableName, ex.getMessage());
			}
		}
		sb.append("{\"columns\":[],\"rows\":[]}");
	}

	// -------------------------------------------------------------------------
	// Text snippets: SELECT configName, configText FROM MonSessionDbmsConfigText
	// -------------------------------------------------------------------------

	private void appendTextsJson(StringBuilder sb, DbxConnection conn, Timestamp ts) throws Exception
	{
		sb.append("[");
		boolean first = true;

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

							if (!first) sb.append(",");
							first = false;
							sb.append("{\"configName\":").append(jsonStr(configName))
							  .append(",\"tabLabel\":"  ).append(jsonStr(tabLabel))
							  .append(",\"configText\":").append(jsonStr(configText))
							  .append("}");
						}
					}
				}
				break; // found working table — stop
			}
			catch (Exception ex)
			{
				_logger.debug("DbmsConfigServlet.appendTextsJson: table '{}' unavailable: {}",
						tableName, ex.getMessage());
			}
		}

		sb.append("]");
	}

	// -------------------------------------------------------------------------
	// Issues: SELECT ... FROM MonSessionDbmsConfigIssues WHERE SessionStartTime = ?
	//         AND Discarded = 0
	// -------------------------------------------------------------------------

	private void appendIssuesJson(StringBuilder sb, DbxConnection conn, Timestamp ts) throws Exception
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
										row.add(readValue(rs, idx, sqlType));
									}
									rows.add(row);
								}
							}
						}
						loaded = true;
					}
					catch (Exception inner)
					{
						_logger.debug("DbmsConfigServlet.appendIssuesJson: query variant failed for '{}': {}",
								tableName, inner.getMessage());
					}
				}

				if (loaded)
				{
					sb.append("{\"columns\":[");
					for (int i = 0; i < columns.size(); i++) { if (i > 0) sb.append(","); sb.append(jsonStr(columns.get(i))); }
					sb.append("],\"rows\":[");
					appendRowsJson(sb, rows);
					sb.append("]}");
					return;
				}
			}
			catch (Exception ex)
			{
				_logger.debug("DbmsConfigServlet.appendIssuesJson: table '{}' unavailable: {}",
						tableName, ex.getMessage());
			}
		}
		sb.append("{\"columns\":[],\"rows\":[]}");
	}

	// -------------------------------------------------------------------------
	// Helpers
	// -------------------------------------------------------------------------

	private static void appendRowsJson(StringBuilder sb, List<List<Object>> rows)
	{
		for (int r = 0; r < rows.size(); r++)
		{
			if (r > 0) sb.append(",");
			sb.append("[");
			List<Object> row = rows.get(r);
			for (int c = 0; c < row.size(); c++)
			{
				if (c > 0) sb.append(",");
				Object v = row.get(c);
				if      (v == null)              sb.append("null");
				else if (v instanceof Number)    sb.append(v.toString());
				else if (v instanceof Boolean)   sb.append(v.toString());
				else                             sb.append(jsonStr(v.toString()));
			}
			sb.append("]");
		}
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

	/** Serialises a Java string as a JSON string literal. */
	private static String jsonStr(String s)
	{
		if (s == null) return "null";
		StringBuilder sb = new StringBuilder(s.length() + 2);
		sb.append('"');
		for (int i = 0; i < s.length(); i++)
		{
			char c = s.charAt(i);
			switch (c)
			{
				case '"':  sb.append("\\\""); break;
				case '\\': sb.append("\\\\"); break;
				case '\n': sb.append("\\n");  break;
				case '\r': sb.append("\\r");  break;
				case '\t': sb.append("\\t");  break;
				case '\f': sb.append("\\f");  break;
				case '\b': sb.append("\\b");  break;
				default:
					if (c < 0x20) sb.append(String.format("\\u%04x", (int) c));
					else sb.append(c);
					break;
			}
		}
		sb.append('"');
		return sb.toString();
	}
}
