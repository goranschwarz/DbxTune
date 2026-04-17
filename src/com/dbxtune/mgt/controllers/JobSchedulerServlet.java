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
import java.sql.ResultSetMetaData;
import java.sql.Timestamp;
import java.sql.Types;
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
import com.dbxtune.ExtractionType;
import com.dbxtune.ICounterController;
import com.dbxtune.central.controllers.Helper;
import com.dbxtune.pcs.PersistentCounterHandler;
import com.dbxtune.pcs.PersistWriterJdbc;
import com.dbxtune.sql.conn.DbxConnection;
import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * Exposes SQL Server Agent (Job Scheduler) data captured during the nightly
 * database rollover by {@code SqlServerJobSchedulerExtractor}.
 *
 * <p>The extractor populates a {@code job_scheduler} schema in the collector's
 * H2 database with the following tables:
 * <ul>
 *   <li>{@code sysjobs}               — job definitions</li>
 *   <li>{@code sysjobhistory}         — full execution history with decoded status/timestamp</li>
 *   <li>{@code job_history_overview_all} — aggregated stats for every job</li>
 *   <li>{@code job_history_overview}  — stats for jobs that ran in the recording period</li>
 *   <li>{@code job_history_outliers}  — jobs/steps running unusually long</li>
 *   <li>{@code job_history_errors}    — failed job steps with error messages</li>
 * </ul>
 *
 * <pre>GET /api/mgt/job-scheduler?action=overview[&amp;ts=YYYY-MM-DD+HH:mm:ss]</pre>
 * <pre>GET /api/mgt/job-scheduler?action=jobs[&amp;ts=...]</pre>
 * <pre>GET /api/mgt/job-scheduler?action=history[&amp;jobName=X][&amp;status=failed][&amp;limit=100][&amp;ts=...]</pre>
 * <pre>GET /api/mgt/job-scheduler?action=errors[&amp;ts=...]</pre>
 * <pre>GET /api/mgt/job-scheduler?action=outliers[&amp;ts=...]</pre>
 * <pre>GET /api/mgt/job-scheduler?action=extractStatus</pre>
 * <pre>POST /api/mgt/job-scheduler?action=extract[&amp;type=JOB_SCHEDULER]</pre>
 */
public class JobSchedulerServlet
extends HttpServlet
{
	private static final long   serialVersionUID  = 1L;
	private static final Logger _logger           = LogManager.getLogger(MethodHandles.lookup().lookupClass());
	private static final String SCHEMA            = "job_scheduler";
	private static final int    DEFAULT_LIMIT     = 200;

	@Override
	protected void doGet(HttpServletRequest req, HttpServletResponse resp)
	throws ServletException, IOException
	{
		resp.setContentType("application/json");
		resp.setCharacterEncoding("UTF-8");
		ObjectMapper om  = Helper.createObjectMapper();
		PrintWriter  out = resp.getWriter();

		String action   = Helper.getParameter(req, "action",  "").trim().toLowerCase();
		String tsParam  = Helper.getParameter(req, "ts",      null);

		if (action.isBlank())
		{
			resp.setStatus(HttpServletResponse.SC_BAD_REQUEST);
			om.writeValue(out, errMap("bad-param",
					"Parameter 'action' is required: overview | jobs | history | errors | outliers | extractStatus"));
			out.flush(); out.close();
			return;
		}

		if ("extractstatus".equals(action))
		{
			String typeParam = Helper.getParameter(req, "type", "JOB_SCHEDULER").trim().toUpperCase();
			om.writeValue(out, actionExtractStatus(typeParam));
			out.flush(); out.close();
			return;
		}

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
				case "overview":
				{
					List<Map<String, Object>> rows = queryTable(conn, "job_history_overview_all", null, null, 0);
					Map<String, Object> root = new LinkedHashMap<>();
					root.put("count", rows.size());
					root.put("jobs",  rows);
					om.writeValue(out, root);
					break;
				}

				case "jobs":
				{
					List<Map<String, Object>> rows = queryTable(conn, "sysjobs", null, "name", 0);
					Map<String, Object> root = new LinkedHashMap<>();
					root.put("count", rows.size());
					root.put("jobs",  rows);
					om.writeValue(out, root);
					break;
				}

				case "history":
				{
					String jobName    = Helper.getParameter(req, "jobName", "").trim();
					String statusFlt  = Helper.getParameter(req, "status",  "").trim().toUpperCase();
					int    limit      = DEFAULT_LIMIT;
					try { limit = Integer.parseInt(Helper.getParameter(req, "limit", String.valueOf(DEFAULT_LIMIT)).trim()); }
					catch (NumberFormatException ignored) {}

					List<Map<String, Object>> rows = queryHistory(conn, jobName, statusFlt, limit);
					Map<String, Object> root = new LinkedHashMap<>();
					root.put("count",   rows.size());
					if (!jobName.isEmpty())   root.put("jobNameFilter",  jobName);
					if (!statusFlt.isEmpty()) root.put("statusFilter",   statusFlt);
					root.put("history", rows);
					om.writeValue(out, root);
					break;
				}

				case "errors":
				{
					List<Map<String, Object>> rows = queryTable(conn, "job_history_errors", null, null, 0);
					Map<String, Object> root = new LinkedHashMap<>();
					root.put("count",  rows.size());
					root.put("errors", rows);
					om.writeValue(out, root);
					break;
				}

				case "outliers":
				{
					List<Map<String, Object>> rows = queryTable(conn, "job_history_outliers", null, null, 0);
					Map<String, Object> root = new LinkedHashMap<>();
					root.put("count",    rows.size());
					root.put("outliers", rows);
					om.writeValue(out, root);
					break;
				}

				default:
					resp.setStatus(HttpServletResponse.SC_BAD_REQUEST);
					om.writeValue(out, errMap("bad-param", "Unknown action '" + action
							+ "'. Use: overview | jobs | history | errors | outliers | extractStatus"));
					break;
			}

			out.flush(); out.close();
		}
		catch (Exception ex)
		{
			_logger.warn("JobSchedulerServlet: action=" + action + " error: " + ex.getMessage(), ex);
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
		String typeParam = Helper.getParameter(req, "type",   "JOB_SCHEDULER").trim().toUpperCase();

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
			_logger.warn("JobSchedulerServlet.doPost: extract error: " + ex.getMessage(), ex);
			resp.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
			om.writeValue(out, errMap("error", ex.getMessage()));
		}
		out.flush(); out.close();
	}

	// =========================================================================
	// Query helpers
	// =========================================================================

	/** Generic: SELECT * FROM "job_scheduler"."table" [ORDER BY orderCol] [LIMIT limit] */
	private static List<Map<String, Object>> queryTable(DbxConnection conn, String table,
			String whereClause, String orderCol, int limit)
	throws Exception
	{
		StringBuilder sql = new StringBuilder("SELECT * FROM \"")
				.append(SCHEMA).append("\".\"").append(table).append("\"");
		if (whereClause != null && !whereClause.isBlank())
			sql.append(" WHERE ").append(whereClause);
		if (orderCol != null && !orderCol.isBlank())
			sql.append(" ORDER BY \"").append(orderCol).append("\"");
		if (limit > 0)
			sql.append(" LIMIT ").append(limit);

		return executeQuery(conn, sql.toString());
	}

	/** history with optional jobName / status filters, newest-first */
	private static List<Map<String, Object>> queryHistory(DbxConnection conn,
			String jobName, String statusFilter, int limit)
	throws Exception
	{
		// Join with sysjobs to get the job name — sysjobhistory only stores job_id
		StringBuilder sql = new StringBuilder(
				"SELECT h.*, j.name AS job_name "
				+ "FROM \"" + SCHEMA + "\".\"sysjobhistory\" h "
				+ "LEFT JOIN \"" + SCHEMA + "\".\"sysjobs\" j ON h.job_id = j.job_id "
				+ "WHERE 1=1 ");

		if (!jobName.isEmpty())
			sql.append(" AND lower(j.name) LIKE lower('%").append(jobName.replace("'", "''")).append("%')");

		if (!statusFilter.isEmpty())
			sql.append(" AND upper(h.run_status_desc) = '").append(statusFilter.replace("'", "''")).append("'");

		sql.append(" ORDER BY h.run_ts DESC");

		if (limit > 0)
			sql.append(" LIMIT ").append(limit);

		return executeQuery(conn, sql.toString());
	}

	/** Execute any SQL and convert every row to a LinkedHashMap preserving column order. */
	private static List<Map<String, Object>> executeQuery(DbxConnection conn, String sql)
	throws Exception
	{
		List<Map<String, Object>> rows = new ArrayList<>();
		PreparedStatement ps = conn.prepareStatement(sql);
		try
		{
			ResultSet         rs   = ps.executeQuery();
			ResultSetMetaData meta = rs.getMetaData();
			int               cols = meta.getColumnCount();

			while (rs.next())
			{
				Map<String, Object> row = new LinkedHashMap<>();
				for (int i = 1; i <= cols; i++)
				{
					String colName = meta.getColumnLabel(i);
					int    sqlType = meta.getColumnType(i);
					Object value;
					switch (sqlType)
					{
						case Types.INTEGER:
						case Types.SMALLINT:
						case Types.TINYINT:
							value = rs.wasNull() ? null : rs.getInt(i);
							break;
						case Types.BIGINT:
							value = rs.wasNull() ? null : rs.getLong(i);
							break;
						case Types.FLOAT:
						case Types.DOUBLE:
						case Types.REAL:
						case Types.NUMERIC:
						case Types.DECIMAL:
							value = rs.wasNull() ? null : rs.getDouble(i);
							break;
						case Types.BIT:
						case Types.BOOLEAN:
							value = rs.wasNull() ? null : rs.getBoolean(i);
							break;
						case Types.TIMESTAMP:
						{
							Timestamp t = rs.getTimestamp(i);
							value = (t == null) ? null : t.toString();
							break;
						}
						default:
							value = rs.getString(i);
							if (rs.wasNull()) value = null;
							break;
					}
					row.put(colName, value);
				}
				rows.add(row);
			}
			rs.close();
		}
		finally { ps.close(); }
		return rows;
	}

	// =========================================================================
	// Action: extract / extractStatus
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
