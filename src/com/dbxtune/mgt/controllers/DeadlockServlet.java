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
import java.lang.invoke.MethodHandles;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Timestamp;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
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

import com.dbxtune.CounterController;
import com.dbxtune.CounterControllerSqlServer;
import com.dbxtune.ExtractionType;
import com.dbxtune.ICounterController;
import com.dbxtune.central.controllers.Helper;
import com.dbxtune.pcs.PersistentCounterHandler;
import com.dbxtune.pcs.PersistWriterJdbc;
import com.dbxtune.sql.conn.DbxConnection;
import com.dbxtune.utils.StringUtil;
import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * Exposes SQL Server deadlock data captured during the nightly database rollover.
 *
 * <p>During rollover, {@code CounterControllerSqlServer.deadlockExtractor()} runs
 * {@code sp_BlitzLock} if deadlocks were observed in the recording period, storing:
 * <ul>
 *   <li>{@code deadlock.count.over.recording.period} — total deadlock count (integer)</li>
 *   <li>{@code deadlock.report.blitzLock} — sp_BlitzLock JSON output (multi-resultset)</li>
 * </ul>
 * in the collector's PCS key-value store.
 *
 * <pre>GET /api/mgt/deadlock?action=summary[&amp;ts=YYYY-MM-DD+HH:mm:ss]</pre>
 * <pre>GET /api/mgt/deadlock?action=report[&amp;ts=YYYY-MM-DD+HH:mm:ss]</pre>
 * <pre>GET /api/mgt/deadlock?action=extractStatus</pre>
 * <pre>POST /api/mgt/deadlock?action=extract[&amp;type=DEADLOCK]</pre>
 *
 * <h3>summary response</h3>
 * <pre>{
 *   "deadlockCount": 5,
 *   "hasReport": true,
 *   "reportError": null
 * }</pre>
 *
 * <h3>report response</h3>
 * <pre>{
 *   "deadlockCount": 5,
 *   "blitzLockJson": "[{...}, {...}, {...}]"
 * }</pre>
 */
public class DeadlockServlet
extends HttpServlet
{
	private static final long serialVersionUID = 1L;
	private static final Logger _logger = LogManager.getLogger(MethodHandles.lookup().lookupClass());

	@Override
	protected void doGet(HttpServletRequest req, HttpServletResponse resp)
	throws ServletException, IOException
	{
		resp.setContentType("application/json");
		resp.setCharacterEncoding("UTF-8");
		ObjectMapper om  = Helper.createObjectMapper();
		PrintWriter  out = resp.getWriter();

		String action  = Helper.getParameter(req, "action",  "").trim().toLowerCase();
		String tsParam = Helper.getParameter(req, "ts",      null);

		if (action.isBlank())
		{
			resp.setStatus(HttpServletResponse.SC_BAD_REQUEST);
			om.writeValue(out, errMap("bad-param", "Parameter 'action' is required: summary | report | extractStatus"));
			out.flush(); out.close();
			return;
		}

		// extractStatus does not need a DB connection
		if ("extractstatus".equals(action))
		{
			String typeParam = Helper.getParameter(req, "type", "DEADLOCK").trim().toUpperCase();
			om.writeValue(out, actionExtractStatus(typeParam));
			out.flush(); out.close();
			return;
		}

		// All other actions need the H2 connection
		Timestamp ts = new Timestamp(System.currentTimeMillis());
		if (tsParam != null && !tsParam.isBlank())
		{
			try
			{
				Date d = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").parse(tsParam.trim().replace('+', ' '));
				ts = new Timestamp(d.getTime());
			}
			catch (ParseException ex)
			{
				resp.setStatus(HttpServletResponse.SC_BAD_REQUEST);
				om.writeValue(out, errMap("bad-param", "Cannot parse ts: " + tsParam));
				out.flush(); out.close();
				return;
			}
		}

		DbxConnection conn = getConnectionForTimestamp(ts);
		if (conn == null)
		{
			resp.setStatus(HttpServletResponse.SC_SERVICE_UNAVAILABLE);
			om.writeValue(out, errMap("no-data", "No storage connection available for timestamp: "
					+ new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(ts)));
			out.flush(); out.close();
			return;
		}

		try
		{
			switch (action)
			{
				case "summary":
				{
					om.writeValue(out, actionSummary(conn));
					break;
				}

				case "report":
				{
					om.writeValue(out, actionReport(conn));
					break;
				}

				default:
					resp.setStatus(HttpServletResponse.SC_BAD_REQUEST);
					om.writeValue(out, errMap("bad-param", "Unknown action '" + action
							+ "'. Use: summary | report | extractStatus"));
					break;
			}

			out.flush(); out.close();
		}
		catch (Exception ex)
		{
			_logger.warn("DeadlockServlet: action=" + action + " error: " + ex.getMessage(), ex);
			resp.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
			om.writeValue(out, errMap("query-error", ex.getMessage()));
			out.flush(); out.close();
		}
	}

	@Override
	protected void doPost(HttpServletRequest req, HttpServletResponse resp)
	throws ServletException, IOException
	{
		resp.setContentType("application/json");
		resp.setCharacterEncoding("UTF-8");
		ObjectMapper om  = Helper.createObjectMapper();
		PrintWriter  out = resp.getWriter();

		String action    = Helper.getParameter(req, "action", "").trim().toLowerCase();
		String typeParam = Helper.getParameter(req, "type",   "DEADLOCK").trim().toUpperCase();

		if (!"extract".equals(action))
		{
			resp.setStatus(HttpServletResponse.SC_METHOD_NOT_ALLOWED);
			om.writeValue(out, errMap("method-not-allowed",
					"POST is only supported for action=extract. Use GET for read actions."));
			out.flush(); out.close();
			return;
		}

		ExtractionType extractionType;
		try
		{
			extractionType = ExtractionType.valueOf(typeParam);
		}
		catch (IllegalArgumentException ex)
		{
			resp.setStatus(HttpServletResponse.SC_BAD_REQUEST);
			om.writeValue(out, errMap("bad-param",
					"Unknown extraction type '" + typeParam + "'. Valid values: "
					+ Arrays.toString(ExtractionType.values())));
			out.flush(); out.close();
			return;
		}

		try
		{
			Map<String, Object> result = actionExtract(extractionType);
			om.writeValue(out, result);
		}
		catch (IllegalStateException ex)
		{
			resp.setStatus(HttpServletResponse.SC_CONFLICT);
			om.writeValue(out, errMap("conflict", ex.getMessage()));
		}
		catch (UnsupportedOperationException ex)
		{
			resp.setStatus(HttpServletResponse.SC_BAD_REQUEST);
			om.writeValue(out, errMap("not-supported", ex.getMessage()));
		}
		catch (Exception ex)
		{
			_logger.warn("DeadlockServlet.doPost: extract error: " + ex.getMessage(), ex);
			resp.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
			om.writeValue(out, errMap("error", ex.getMessage()));
		}
		out.flush(); out.close();
	}

	// =========================================================================
	// Action: summary
	// =========================================================================

	private Map<String, Object> actionSummary(DbxConnection conn)
	throws Exception
	{
		int    count       = PersistWriterJdbc.getKeyValueStoreAsInteger(conn, CounterControllerSqlServer.PCS_KEY_VALUE_deadlockCountOverRecordingPeriod, 0);
		String blitzReport = PersistWriterJdbc.getKeyValueStoreAsString (conn, CounterControllerSqlServer.PCS_KEY_VALUE_deadlockReport_blitzLock);

		boolean hasReport   = StringUtil.hasValue(blitzReport)
				&& !blitzReport.startsWith("ERROR:")
				&& !blitzReport.startsWith("Msg ");
		String  reportError = null;
		if (StringUtil.hasValue(blitzReport) && !hasReport)
			reportError = blitzReport;

		// Also return per-recording-period deadlock count from CmSummary if available
		List<Map<String, Object>> timeline = getDeadlockTimeline(conn);

		Map<String, Object> result = new LinkedHashMap<>();
		result.put("deadlockCount", count);
		result.put("hasReport",     hasReport);
		if (reportError != null)
			result.put("reportError", reportError);
		if (!timeline.isEmpty())
			result.put("timeline", timeline);
		return result;
	}

	// =========================================================================
	// Action: report
	// =========================================================================

	private Map<String, Object> actionReport(DbxConnection conn)
	throws Exception
	{
		int    count       = PersistWriterJdbc.getKeyValueStoreAsInteger(conn, CounterControllerSqlServer.PCS_KEY_VALUE_deadlockCountOverRecordingPeriod, 0);
		String blitzReport = PersistWriterJdbc.getKeyValueStoreAsString (conn, CounterControllerSqlServer.PCS_KEY_VALUE_deadlockReport_blitzLock);

		Map<String, Object> result = new LinkedHashMap<>();
		result.put("deadlockCount", count);

		if (!StringUtil.hasValue(blitzReport))
		{
			result.put("hasReport",    false);
			result.put("note", "No sp_BlitzLock report stored. Either no deadlocks occurred, "
					+ "or extraction has not run yet. Use action=extract to trigger on-demand.");
		}
		else if (blitzReport.startsWith("ERROR:") || blitzReport.startsWith("Msg "))
		{
			result.put("hasReport",    false);
			result.put("reportError",  blitzReport);
		}
		else
		{
			result.put("hasReport",    true);
			// The raw value is multi-resultset JSON as produced by ResultSetTableModel.toJson()
			// Return it as a raw JSON string — the caller can parse the sections
			result.put("blitzLockJson", blitzReport);
		}
		return result;
	}

	// =========================================================================
	// Deadlock timeline from CmSummary (best-effort, table may not exist)
	// =========================================================================

	private List<Map<String, Object>> getDeadlockTimeline(DbxConnection conn)
	{
		List<Map<String, Object>> rows = new ArrayList<>();
		try
		{
			// CmSummary_diff stores DeadlockCountSum — one row per sample
			String sql = "SELECT SessionStartTime, DeadlockCountSum "
					+ "FROM \"CmSummary_diff\" "
					+ "WHERE DeadlockCountSum > 0 "
					+ "ORDER BY SessionStartTime";
			PreparedStatement ps = conn.prepareStatement(sql);
			try
			{
				ResultSet rs = ps.executeQuery();
				while (rs.next())
				{
					Map<String, Object> row = new LinkedHashMap<>();
					row.put("ts",             rs.getString("SessionStartTime"));
					row.put("deadlockCount",  rs.getInt("DeadlockCountSum"));
					rows.add(row);
				}
				rs.close();
			}
			finally { ps.close(); }
		}
		catch (Exception ignored) {} // table may not exist in all H2 files
		return rows;
	}

	// =========================================================================
	// Action: extract / extractStatus (shared with QueryStoreServlet)
	// =========================================================================

	private Map<String, Object> actionExtract(ExtractionType type)
	{
		if (!CounterController.hasInstance())
			throw new IllegalStateException(
					"CounterController is not running — the collector must be active to trigger an extraction.");

		ICounterController cc = CounterController.getInstance();

		if (!cc.getSupportedExtractionTypes().contains(type))
			throw new UnsupportedOperationException(
					"Extraction type " + type + " is not supported by "
					+ cc.getClass().getSimpleName()
					+ ". Supported: " + cc.getSupportedExtractionTypes());

		cc.triggerExtractionOf(type);

		Map<String, Object> result = new LinkedHashMap<>();
		result.put("status", "started");
		result.put("type",   type.name());
		result.put("note",   "Extraction is running in the background. "
				+ "Poll action=extractStatus&type=" + type.name() + " for progress.");
		return result;
	}

	private Map<String, Object> actionExtractStatus(String typeName)
	{
		if (!CounterController.hasInstance())
		{
			Map<String, Object> m = new LinkedHashMap<>();
			m.put("error",   "controller-not-running");
			m.put("message", "CounterController is not running.");
			return m;
		}
		ExtractionType type;
		try
		{
			type = ExtractionType.valueOf(typeName);
		}
		catch (IllegalArgumentException ex)
		{
			Map<String, Object> m = new LinkedHashMap<>();
			m.put("error",   "bad-type");
			m.put("message", "Unknown type '" + typeName + "'. Valid: "
					+ Arrays.toString(ExtractionType.values()));
			return m;
		}
		ICounterController  cc     = CounterController.getInstance();
		Map<String, Object> status = cc.getExtractionStatus(type);
		status.put("type",      type.name());
		status.put("supported", cc.getSupportedExtractionTypes().contains(type));
		return status;
	}

	// =========================================================================
	// Helpers
	// =========================================================================

	private static DbxConnection getConnectionForTimestamp(Timestamp ts)
	{
		if (!PersistentCounterHandler.hasInstance())
			return null;
		PersistWriterJdbc writer = PersistentCounterHandler.getInstance().getPersistWriterJdbc();
		if (writer == null)
			return null;
		return CmDataServlet.getConnectionForTimestamp(ts, writer);
	}

	private static Map<String, Object> errMap(String code, String message)
	{
		Map<String, Object> m = new LinkedHashMap<>();
		m.put("error",   code);
		m.put("message", message);
		return m;
	}
}
