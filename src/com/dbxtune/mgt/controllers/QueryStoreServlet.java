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
import java.sql.Clob;
import java.sql.DatabaseMetaData;
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
import java.util.Set;

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
import com.dbxtune.utils.DbUtils;
import com.dbxtune.utils.StringUtil;
import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * Reads SQL Server Query Store data captured in the collector's H2 database.
 *
 * <p>During the daily rollover ({@code doLastRecordingActionBeforeDatabaseRollover}),
 * the collector runs {@code SqlServerQueryStoreExtractor} which copies Query Store
 * data from SQL Server into H2 schemas named {@code qs:<DatabaseName>}.
 *
 * <pre>GET /api/mgt/query-store</pre>
 *
 * <p>Query parameters:
 * <ul>
 *   <li>{@code action}        – required: {@code listDatabases} | {@code topQueries} | {@code queryDetail}</li>
 *   <li>{@code dbname}        – required for topQueries/queryDetail; for listDatabases: optional filter</li>
 *   <li>{@code queryId}       – required for queryDetail: the {@code query_id} to inspect</li>
 *   <li>{@code rankBy}        – topQueries: {@code duration} (default) | {@code cpu} | {@code reads} | {@code physical_reads} | {@code executions} | {@code memory} | {@code compile} (SQL 2022+)</li>
 *   <li>{@code topN}          – topQueries: max rows. Default: 25</li>
 *   <li>{@code minExecutions} – topQueries: filter out queries with fewer executions. Default: 2</li>
 *   <li>{@code includePlan}   – queryDetail: include XML execution plan text. Default: {@code false} (can be large)</li>
 *   <li>{@code ts}            – timestamp ({@code YYYY-MM-DD HH:mm:ss}) for H2 date-rolling. Default: current recording.</li>
 * </ul>
 *
 * <h3>listDatabases response</h3>
 * <pre>{
 *   "count": N,
 *   "databases": [
 *     { "dbname": "...", "actualState": "...", "currentStorageMb": N, "maxStorageMb": N,
 *       "storageUsedPct": N, "daysInStorage": N, "stateIsOk": "YES",
 *       "willExceedMaxStorage": "NO", "queryCount": N, "planCount": N,
 *       "earliest": "...", "latest": "...", "recommendationCount": N }
 *   ]
 * }</pre>
 *
 * <h3>topQueries response</h3>
 * <pre>{
 *   "dbname": "...", "rankBy": "...", "count": N,
 *   "hasCompileDuration": true,
 *   "queries": [
 *     { "queryId": N, "objectContext": "schema.proc", "planCount": N,
 *       "totalExecutions": N, "avgDurationUs": N, "avgCpuUs": N, "avgLogicalReads": N,
 *       "avgPhysicalReads": N, "totalDurationUs": N, "totalCpuUs": N, "totalLogicalReads": N,
 *       "totalPhysicalReads": N, "avgMemoryMb": N, "totalMemoryMb": N,
 *       "maxDurationUs": N, "avgRowcount": N,
 *       "avgCompileDurationUs": N, "totalCompileDurationUs": N (only when hasCompileDuration=true),
 *       "firstSeen": "...", "lastSeen": "...",
 *       "hasMultiplePlans": true, "querySqlTextPreview": "..." }
 *   ]
 * }</pre>
 *
 * <h3>queryDetail response</h3>
 * <pre>{
 *   "dbname": "...", "queryId": N,
 *   "queryText": "SELECT ...",
 *   "objectContext": "schema.proc",
 *   "regressionSignal": { "detected": true, "maxMinRatio": 3.2, "note": "..." },
 *   "plans": [ { "planId": N, "isForcedPlan": false, "planForcingType": "NONE",
 *                "totalExecutions": N, "avgDurationUs": N, "avgCpuUs": N,
 *                "avgLogicalReads": N, "maxDurationUs": N,
 *                "firstSeen": "...", "lastSeen": "...",
 *                "queryPlan": "<ShowPlanXML...>" } ],
 *   "waitStats": [ { "waitCategory": "...", "totalWaitMs": N, "avgWaitMs": N, "maxWaitMs": N } ],
 *   "timeline": [ { "intervalStart": "...", "intervalEnd": "...",
 *                   "totalExecutions": N, "avgDurationUs": N, "avgCpuUs": N } ],
 *   "recommendations": [ { "name": "...", "type": "...", "reason": "...", "score": N, "state": "..." } ]
 * }</pre>
 */
public class QueryStoreServlet
extends HttpServlet
{
	private static final long serialVersionUID = 1L;
	private static final Logger _logger = LogManager.getLogger(MethodHandles.lookup().lookupClass());

	private static final int DEFAULT_TOP_N          = 25;
	private static final int DEFAULT_MIN_EXECUTIONS = 2;
	private static final int TIMELINE_INTERVALS     = 24;
	private static final int SQL_PREVIEW_LEN        = 500;

	@Override
	protected void doGet(HttpServletRequest req, HttpServletResponse resp)
	throws ServletException, IOException
	{
		resp.setContentType("application/json");
		resp.setCharacterEncoding("UTF-8");
		ObjectMapper om  = Helper.createObjectMapper();
		PrintWriter  out = resp.getWriter();

		// ---- Parse parameters ------------------------------------------------
		String action         = Helper.getParameter(req, "action",         "");
		String dbnameParam    = Helper.getParameter(req, "dbname",         "");
		String queryIdParam   = Helper.getParameter(req, "queryId",        "");
		String planIdParam    = Helper.getParameter(req, "planId",         "");
		String rankByParam    = Helper.getParameter(req, "rankBy",         "duration");
		String topNParam      = Helper.getParameter(req, "topN",           String.valueOf(DEFAULT_TOP_N));
		String minExecParam   = Helper.getParameter(req, "minExecutions",  String.valueOf(DEFAULT_MIN_EXECUTIONS));
		String includePlanPrm = Helper.getParameter(req, "includePlan",    "false");
		String tsParam        = Helper.getParameter(req, "ts",             null);

		if (action.isBlank())
		{
			resp.setStatus(HttpServletResponse.SC_BAD_REQUEST);
			om.writeValue(out, errMap("bad-param", "Parameter 'action' is required: listDatabases | topQueries | queryDetail | lastActualPlan | extractStatus"));
			out.flush(); out.close();
			return;
		}

		boolean includePlan = "true".equalsIgnoreCase(includePlanPrm.trim());
		int     topN        = DEFAULT_TOP_N;
		int     minExec     = DEFAULT_MIN_EXECUTIONS;
		try { topN    = Integer.parseInt(topNParam.trim());    } catch (NumberFormatException ignored) {}
		try { minExec = Integer.parseInt(minExecParam.trim()); } catch (NumberFormatException ignored) {}

		// Resolve timestamp for H2 date-rolling
		Timestamp ts = new Timestamp(System.currentTimeMillis());
		if (StringUtil.hasValue(tsParam))
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
//System.out.println("XXX: getConnectionForTimestamp(tsParam='"+tsParam+"', ts='"+ts+"'): " + conn);
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
			switch (action.trim().toLowerCase())
			{
				case "listdatabases":
				{
					List<Map<String, Object>> dbs = actionListDatabases(conn, dbnameParam.trim());
					Map<String, Object> root = new LinkedHashMap<>();
					root.put("count",     dbs.size());
					root.put("databases", dbs);
					om.writeValue(out, root);
					break;
				}

				case "topqueries":
				{
					if (dbnameParam.isBlank())
					{
						resp.setStatus(HttpServletResponse.SC_BAD_REQUEST);
						om.writeValue(out, errMap("bad-param", "Parameter 'dbname' is required for action=topQueries"));
						break;
					}
					Map<String, Object> tqResult = actionTopQueries(conn, dbnameParam.trim(), rankByParam.trim().toLowerCase(), topN, minExec);
					@SuppressWarnings("unchecked")
					List<Map<String, Object>> queries = (List<Map<String, Object>>) tqResult.get("queries");
					Map<String, Object> root = new LinkedHashMap<>();
					root.put("dbname",             dbnameParam.trim());
					root.put("rankBy",             rankByParam.trim());
					root.put("count",              queries.size());
					root.put("has2017Cols",        tqResult.get("has2017Cols"));
					root.put("hasMemoryCols",      tqResult.get("hasMemoryCols"));
					root.put("hasCompileDuration", tqResult.get("hasCompileDuration"));
					root.put("queries",            queries);
					om.writeValue(out, root);
					break;
				}

				case "querydetail":
				{
					if (dbnameParam.isBlank())
					{
						resp.setStatus(HttpServletResponse.SC_BAD_REQUEST);
						om.writeValue(out, errMap("bad-param", "Parameter 'dbname' is required for action=queryDetail"));
						break;
					}
					if (queryIdParam.isBlank())
					{
						resp.setStatus(HttpServletResponse.SC_BAD_REQUEST);
						om.writeValue(out, errMap("bad-param", "Parameter 'queryId' is required for action=queryDetail"));
						break;
					}
					long queryId;
					try { queryId = Long.parseLong(queryIdParam.trim()); }
					catch (NumberFormatException ex)
					{
						resp.setStatus(HttpServletResponse.SC_BAD_REQUEST);
						om.writeValue(out, errMap("bad-param", "queryId must be a number: " + queryIdParam));
						break;
					}
					Map<String, Object> detail = actionQueryDetail(conn, dbnameParam.trim(), queryId, includePlan);
					om.writeValue(out, detail);
					break;
				}

				case "lastactualplan":
				{
					if (dbnameParam.isBlank())
					{
						resp.setStatus(HttpServletResponse.SC_BAD_REQUEST);
						om.writeValue(out, errMap("bad-param", "Parameter 'dbname' is required for action=lastActualPlan"));
						break;
					}
					if (planIdParam.isBlank())
					{
						resp.setStatus(HttpServletResponse.SC_BAD_REQUEST);
						om.writeValue(out, errMap("bad-param", "Parameter 'planId' is required for action=lastActualPlan"));
						break;
					}
					long planId;
					try { planId = Long.parseLong(planIdParam.trim()); }
					catch (NumberFormatException ex)
					{
						resp.setStatus(HttpServletResponse.SC_BAD_REQUEST);
						om.writeValue(out, errMap("bad-param", "planId must be a number: " + planIdParam));
						break;
					}
					Map<String, Object> lastActual = actionLastActualPlan(conn, dbnameParam.trim(), planId);
					om.writeValue(out, lastActual);
					break;
				}

				case "extractstatus":
				{
					// Return the current status of a given extraction type (no H2 conn needed)
					String typeParam = Helper.getParameter(req, "type", "QUERY_STORE").trim().toUpperCase();
					Map<String, Object> statusResult = actionExtractStatus(typeParam);
					om.writeValue(out, statusResult);
					break;
				}

				default:
					resp.setStatus(HttpServletResponse.SC_BAD_REQUEST);
					om.writeValue(out, errMap("bad-param", "Unknown action '" + action
							+ "'. Use: listDatabases | topQueries | queryDetail | lastActualPlan | extractStatus"));
					break;
			}

			out.flush(); out.close();
		}
		catch (Exception ex)
		{
			_logger.warn("QueryStoreServlet: action=" + action + " error: " + ex.getMessage(), ex);
			resp.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
			om.writeValue(out, errMap("query-error", ex.getMessage()));
			out.flush(); out.close();
		}
	}

	/**
	 * POST /api/mgt/query-store  (action in request body or query string)
	 * <p>
	 * Handles the {@code extract} action which triggers an asynchronous on-demand
	 * extraction. All other actions are read-only and answered via GET.
	 */
	@Override
	protected void doPost(HttpServletRequest req, HttpServletResponse resp)
	throws ServletException, IOException
	{
		resp.setContentType("application/json");
		resp.setCharacterEncoding("UTF-8");
		ObjectMapper om  = Helper.createObjectMapper();
		PrintWriter  out = resp.getWriter();

		String action   = Helper.getParameter(req, "action", "").trim().toLowerCase();
		String typeParam = Helper.getParameter(req, "type",   "QUERY_STORE").trim().toUpperCase();

		if (!"extract".equals(action))
		{
			resp.setStatus(HttpServletResponse.SC_METHOD_NOT_ALLOWED);
			om.writeValue(out, errMap("method-not-allowed",
					"POST is only supported for action=extract. Use GET for all read actions."));
			out.flush(); out.close();
			return;
		}

		// Resolve the ExtractionType enum value
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
					+ java.util.Arrays.toString(ExtractionType.values())));
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
			// Already running or no connection
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
			_logger.warn("QueryStoreServlet.doPost: extract error: " + ex.getMessage(), ex);
			resp.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
			om.writeValue(out, errMap("error", ex.getMessage()));
		}
		out.flush(); out.close();
	}

	// =========================================================================
	// Action: extract (trigger on-demand)
	// =========================================================================

	/**
	 * Triggers an asynchronous on-demand extraction via {@link ICounterController#triggerExtractionOf}.
	 * Returns immediately with {@code status="started"} so the caller can poll
	 * {@code action=extractStatus} for completion.
	 */
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
					+ ". Supported types: " + cc.getSupportedExtractionTypes());

		// Throws IllegalStateException if already running
		cc.triggerExtractionOf(type);

		Map<String, Object> result = new LinkedHashMap<>();
		result.put("status",  "started");
		result.put("type",    type.name());
		result.put("note",    "Extraction is running in the background. "
				+ "Poll action=extractStatus&type=" + type.name() + " for progress. "
				+ "Query Store extraction typically takes 1-3 minutes depending on database size.");
		return result;
	}

	// =========================================================================
	// Action: extractStatus (poll running/lastRun/lastStatus)
	// =========================================================================

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
			m.put("message", "Unknown type '" + typeName + "'. Valid: " + Arrays.toString(ExtractionType.values()));
			return m;
		}

		ICounterController cc      = CounterController.getInstance();
		Map<String, Object> status = cc.getExtractionStatus(type);
		status.put("type",      type.name());
		status.put("supported", cc.getSupportedExtractionTypes().contains(type));
		return status;
	}

	// =========================================================================
	// Action: listDatabases
	// =========================================================================

	private List<Map<String, Object>> actionListDatabases(DbxConnection conn, String dbnameFilter)
	throws Exception
	{
		List<Map<String, Object>> result = new ArrayList<>();

		// Discover all qs: schemas using standard JDBC metadata helper
		Set<String> schemaSet = DbUtils.getSchemaNames(conn);
		List<String> schemaNames = new ArrayList<>();
		for (String schema : schemaSet)
		{
			if (schema != null && schema.toLowerCase().startsWith("qs:"))
				schemaNames.add(schema);
		}
		java.util.Collections.sort(schemaNames);

		for (String schemaName : schemaNames)
		{
			String dbname = schemaName.substring("qs:".length()); // strip prefix

			// Apply optional filter
			if (!dbnameFilter.isEmpty() && !dbname.toLowerCase().contains(dbnameFilter.toLowerCase()))
				continue;

			Map<String, Object> row = new LinkedHashMap<>();
			row.put("dbname", dbname);

			// Options row (may not exist on very old extractions)
			try
			{
				String optSql = conn.quotifySqlString(""
						+ "SELECT \n"
						+ "    [actual_state_desc] \n"
						+ "   ,[current_storage_size_mb] \n"
						+ "   ,[max_storage_size_mb] \n"
						+ "   ,[stateIsOk] \n"
						+ "   ,[storageUsedPct] \n"
						+ "   ,[daysInStorage] \n"
						+ "   ,[stale_query_threshold_days] \n"
						+ "   ,[willExceedMaxStorageSizeMb] \n"
						+ "FROM " + t(dbname, "database_query_store_options"));
				PreparedStatement ps = conn.prepareStatement(optSql);
				try
				{
					ResultSet rs = ps.executeQuery();
					if (rs.next())
					{
						row.put("actualState",             safeGetStr   (rs, "actual_state_desc"));
						row.put("currentStorageMb",        safeGetLong  (rs, "current_storage_size_mb"));
						row.put("maxStorageMb",            safeGetLong  (rs, "max_storage_size_mb"));
						row.put("stateIsOk",               safeGetStr   (rs, "stateIsOk"));
						row.put("storageUsedPct",          safeGetDouble(rs, "storageUsedPct"));
						row.put("daysInStorage",           safeGetLong  (rs, "daysInStorage"));
						row.put("staleQueryThresholdDays", safeGetLong  (rs, "stale_query_threshold_days"));
						row.put("willExceedMaxStorage",    safeGetStr   (rs, "willExceedMaxStorageSizeMb"));
					}
					rs.close();
				}
				finally { ps.close(); }
			}
			catch (Exception ex)
			{
				_logger.debug("QueryStoreServlet.listDatabases: cannot read options for {}: {}", dbname, ex.getMessage());
			}

			// Query and plan counts
			row.put("queryCount", safeCountTable(conn, dbname, "query_store_query"));
			row.put("planCount",  safeCountTable(conn, dbname, "query_store_plan"));

			// Date range from intervals
			try
			{
				String rangeSql = conn.quotifySqlString(""
						+ "SELECT \n"
						+ "     MIN([start_time]) AS [earliest] \n"
						+ "    ,MAX([end_time])   AS [latest] \n"
						+ "FROM " + t(dbname, "query_store_runtime_stats_interval"));
				PreparedStatement ps = conn.prepareStatement(rangeSql);
				try
				{
					ResultSet rs = ps.executeQuery();
					if (rs.next())
					{
						SimpleDateFormat fmt = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
						Timestamp earliest = rs.getTimestamp("earliest");
						Timestamp latest   = rs.getTimestamp("latest");
						row.put("earliest", earliest != null ? fmt.format(earliest) : null);
						row.put("latest",   latest   != null ? fmt.format(latest)   : null);
					}
					rs.close();
				}
				finally { ps.close(); }
			}
			catch (Exception ex)
			{
				_logger.debug("QueryStoreServlet.listDatabases: cannot read interval range for {}: {}", dbname, ex.getMessage());
			}

			// Recommendation count (SQL Server 2017+ only)
			try
			{
				row.put("recommendationCount", safeCountTable(conn, dbname, "dm_db_tuning_recommendations"));
			}
			catch (Exception ignored) { row.put("recommendationCount", null); }

			result.add(row);
		}

		return result;
	}

	// =========================================================================
	// Action: topQueries
	// =========================================================================

	/**
	 * Returns a {@code Map} with metadata keys and a {@code queries} list:
	 * <ul>
	 *   <li>{@code has2017Cols}       – SQL Server 2017+ columns present (tempdb, log, DOP).</li>
	 *   <li>{@code hasCompileDuration}– SQL Server 2022+ {@code avg_compile_duration} present.</li>
	 *   <li>{@code queries}           – {@code List<Map<String,Object>>} of query rows.</li>
	 * </ul>
	 */
	private Map<String, Object> actionTopQueries(
			DbxConnection conn, String dbname, String rankBy, int topN, int minExecutions)
	throws Exception
	{
		// Detect optional columns introduced in newer SQL Server versions.
		boolean hasMemoryCols      = hasColumn(conn, dbname, "query_store_runtime_stats", "avg_query_max_used_memory");
		boolean has2017Cols        = hasColumn(conn, dbname, "query_store_runtime_stats", "avg_tempdb_space_used");
		boolean hasCompileDuration = hasColumn(conn, dbname, "query_store_query",          "avg_compile_duration");

		// Map rankBy keyword to the computed column expression
		String orderCol;
		switch (rankBy)
		{
			case "cpu":            orderCol = "[total_cpu_us]";          break;
			case "reads":          orderCol = "[total_logical_reads]";    break;
			case "physical_reads": orderCol = "[total_physical_reads]";   break;
			case "executions":     orderCol = "[total_executions]";       break;
			case "rowcount":       orderCol = "[total_rowcount]";         break;
			case "memory":         orderCol = hasMemoryCols
			                                  ? "[total_memory_mb]"
			                                  : "[total_duration_us]";    break;
			case "writes":         orderCol = "[total_logical_writes]";  break;
			case "tempdb":         orderCol = has2017Cols
			                                  ? "[total_tempdb_mb]"
			                                  : "[total_duration_us]";    break;
			case "log":            orderCol = has2017Cols
			                                  ? "[total_log_mb]"
			                                  : "[total_duration_us]";    break;
			case "compile":        orderCol = hasCompileDuration
			                                  ? "[avg_compile_duration_us]"
			                                  : "[total_duration_us]";    break;
			default:               orderCol = "[total_duration_us]";      break; // "duration" / default
		}

		StringBuilder sql = new StringBuilder();
		sql.append("SELECT \n");
		sql.append(getTopSql(conn, topN));
		sql.append("    q.[query_id] \n");
		sql.append("   ,q.[query_text_id] \n");
		sql.append("   ,q.[object_name] \n");
		sql.append("   ,q.[schema_name] \n");
		sql.append("   ,COUNT(DISTINCT p.[plan_id]) AS [plan_count] \n");
		sql.append("   ,SUM(rs.[count_executions]) AS [total_executions] \n");
		// weighted averages
		sql.append("   ,SUM(CAST(rs.[count_executions] AS BIGINT) * rs.[avg_duration])           / NULLIF(SUM(rs.[count_executions]), 0) AS [avg_duration_us] \n");
		sql.append("   ,SUM(CAST(rs.[count_executions] AS BIGINT) * rs.[avg_cpu_time])           / NULLIF(SUM(rs.[count_executions]), 0) AS [avg_cpu_us] \n");
		sql.append("   ,SUM(CAST(rs.[count_executions] AS BIGINT) * rs.[avg_logical_io_reads])   / NULLIF(SUM(rs.[count_executions]), 0) AS [avg_logical_reads] \n");
		sql.append("   ,SUM(CAST(rs.[count_executions] AS BIGINT) * rs.[avg_physical_io_reads])  / NULLIF(SUM(rs.[count_executions]), 0) AS [avg_physical_reads] \n");
		sql.append("   ,SUM(CAST(rs.[count_executions] AS BIGINT) * rs.[avg_logical_io_writes])  / NULLIF(SUM(rs.[count_executions]), 0) AS [avg_logical_writes] \n");
		sql.append("   ,SUM(CAST(rs.[count_executions] AS BIGINT) * rs.[avg_rowcount])           / NULLIF(SUM(rs.[count_executions]), 0) AS [avg_rowcount] \n");
		// totals for ranking
		sql.append("   ,SUM(CAST(rs.[count_executions] AS BIGINT) * rs.[avg_duration])           AS [total_duration_us] \n");
		sql.append("   ,SUM(CAST(rs.[count_executions] AS BIGINT) * rs.[avg_cpu_time])           AS [total_cpu_us] \n");
		sql.append("   ,SUM(CAST(rs.[count_executions] AS BIGINT) * rs.[avg_logical_io_reads])   AS [total_logical_reads] \n");
		sql.append("   ,SUM(CAST(rs.[count_executions] AS BIGINT) * rs.[avg_physical_io_reads])  AS [total_physical_reads] \n");
		sql.append("   ,SUM(CAST(rs.[count_executions] AS BIGINT) * rs.[avg_logical_io_writes])  AS [total_logical_writes] \n");
		sql.append("   ,SUM(CAST(rs.[count_executions] AS BIGINT) * rs.[avg_rowcount])           AS [total_rowcount] \n");
		// avg_query_max_used_memory is in 8-KB pages → /128.0 = MB
		if (hasMemoryCols)
		{
			sql.append("   ,SUM(CAST(rs.[count_executions] AS BIGINT) * rs.[avg_query_max_used_memory]) / NULLIF(SUM(rs.[count_executions]), 0) / 128.0 AS [avg_memory_mb] \n");
			sql.append("   ,SUM(CAST(rs.[count_executions] AS BIGINT) * rs.[avg_query_max_used_memory]) / 128.0 AS [total_memory_mb] \n");
		}
		// max values
		sql.append("   ,MAX(rs.[avg_duration])        AS [max_duration_us] \n");
		sql.append("   ,MAX(rs.[avg_cpu_time])         AS [max_cpu_us] \n");
		sql.append("   ,MAX(rs.[max_logical_io_reads]) AS [max_logical_reads] \n");
		// SQL Server 2017+ columns (tempdb in 8-KB pages → /128.0 = MB; log in bytes → /1048576.0 = MB)
		if (has2017Cols)
		{
			sql.append("   ,SUM(CAST(rs.[count_executions] AS BIGINT) * rs.[avg_dop])               / NULLIF(SUM(rs.[count_executions]), 0)           AS [avg_dop] \n");
			sql.append("   ,SUM(CAST(rs.[count_executions] AS BIGINT) * rs.[avg_tempdb_space_used]) / NULLIF(SUM(rs.[count_executions]), 0) / 128.0    AS [avg_tempdb_mb] \n");
			sql.append("   ,SUM(CAST(rs.[count_executions] AS BIGINT) * rs.[avg_tempdb_space_used]) / 128.0                                          AS [total_tempdb_mb] \n");
			sql.append("   ,SUM(CAST(rs.[count_executions] AS BIGINT) * rs.[avg_log_bytes_used])    / NULLIF(SUM(rs.[count_executions]), 0) / 1048576.0 AS [avg_log_mb] \n");
			sql.append("   ,SUM(CAST(rs.[count_executions] AS BIGINT) * rs.[avg_log_bytes_used])    / 1048576.0                                       AS [total_log_mb] \n");
		}
		// SQL Server 2019+ compile stats — live in query_store_query (per-query, not per-interval)
		if (hasCompileDuration)
		{
			sql.append("   ,MAX(q.[avg_compile_duration])  AS [avg_compile_duration_us] \n");
			sql.append("   ,MAX(q.[count_compiles])        AS [count_compiles] \n");
		}
		// time range
		sql.append("   ,MIN(i.[start_time]) AS [first_seen] \n");
		sql.append("   ,MAX(i.[end_time])   AS [last_seen] \n");
		sql.append(" FROM ").append(t(dbname, "query_store_query")).append(" q \n");
		sql.append(" INNER JOIN ").append(t(dbname, "query_store_plan")).append(" p ON q.[query_id] = p.[query_id] \n");
		sql.append(" INNER JOIN ").append(t(dbname, "query_store_runtime_stats")).append(" rs ON p.[plan_id] = rs.[plan_id] \n");
		sql.append(" INNER JOIN ").append(t(dbname, "query_store_runtime_stats_interval")).append(" i ON rs.[runtime_stats_interval_id] = i.[runtime_stats_interval_id] \n");
		sql.append(" GROUP BY q.[query_id], q.[query_text_id], q.[object_name], q.[schema_name] \n");
		sql.append(" HAVING SUM(rs.[count_executions]) >= ? \n");
		sql.append(" ORDER BY ").append(orderCol).append(" DESC ").append(getNullsLastSql(conn)).append(" \n");
		sql.append(getLimitSql(conn, topN));

		String finalSql = conn.quotifySqlString(sql.toString());
		_logger.debug("QueryStoreServlet.topQueries: dbname={} rankBy={} has2017Cols={} hasCompileDuration={} sql={}",
				dbname, rankBy, has2017Cols, hasCompileDuration, finalSql);

		SimpleDateFormat tsFmt = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
		List<Map<String, Object>> rows = new ArrayList<>();

		PreparedStatement pstmt = conn.prepareStatement(finalSql);
		try
		{
			pstmt.setInt(1, minExecutions);
			ResultSet rs = pstmt.executeQuery();
			while (rs.next())
			{
				long queryId     = rs.getLong("query_id");
				long queryTextId = rs.getLong("query_text_id");
				String objName   = rs.getString("object_name");
				String schName   = rs.getString("schema_name");

				Map<String, Object> row = new LinkedHashMap<>();
				row.put("queryId",           queryId);
				row.put("objectContext",     buildObjectContext(schName, objName));
				row.put("planCount",         rs.getInt("plan_count"));
				row.put("hasMultiplePlans",  rs.getInt("plan_count") > 1);
				row.put("totalExecutions",   rs.getLong("total_executions"));
				row.put("avgDurationUs",     safeGetLong(rs, "avg_duration_us"));
				row.put("avgCpuUs",          safeGetLong(rs, "avg_cpu_us"));
				row.put("avgLogicalReads",    safeGetLong  (rs, "avg_logical_reads"));
				row.put("avgPhysicalReads",   safeGetLong  (rs, "avg_physical_reads"));
				row.put("avgLogicalWrites",   safeGetLong  (rs, "avg_logical_writes"));
				row.put("avgRowcount",        safeGetDouble(rs, "avg_rowcount"));
				row.put("totalDurationUs",    safeGetLong  (rs, "total_duration_us"));
				row.put("totalCpuUs",         safeGetLong  (rs, "total_cpu_us"));
				row.put("totalLogicalReads",  safeGetLong  (rs, "total_logical_reads"));
				row.put("totalPhysicalReads", safeGetLong  (rs, "total_physical_reads"));
				row.put("totalLogicalWrites", safeGetLong  (rs, "total_logical_writes"));
				row.put("totalRowcount",      safeGetDouble(rs, "total_rowcount"));
				if (hasMemoryCols)
				{
					row.put("avgMemoryMb",    safeGetDouble(rs, "avg_memory_mb"));
					row.put("totalMemoryMb",  safeGetDouble(rs, "total_memory_mb"));
				}
				row.put("maxDurationUs",      safeGetLong  (rs, "max_duration_us"));
				row.put("maxCpuUs",           safeGetLong  (rs, "max_cpu_us"));
				row.put("maxLogicalReads",    safeGetLong  (rs, "max_logical_reads"));
				if (has2017Cols)
				{
					row.put("avgDop",        safeGetDouble(rs, "avg_dop"));
					row.put("avgTempdbMb",   safeGetDouble(rs, "avg_tempdb_mb"));
					row.put("totalTempdbMb", safeGetDouble(rs, "total_tempdb_mb"));
					row.put("avgLogMb",      safeGetDouble(rs, "avg_log_mb"));
					row.put("totalLogMb",    safeGetDouble(rs, "total_log_mb"));
				}
				if (hasCompileDuration)
				{
					row.put("avgCompileDurationUs", safeGetLong(rs, "avg_compile_duration_us"));
					row.put("countCompiles",        safeGetLong(rs, "count_compiles"));
				}

				Timestamp firstSeen = rs.getTimestamp("first_seen");
				Timestamp lastSeen  = rs.getTimestamp("last_seen");
				row.put("firstSeen", firstSeen != null ? tsFmt.format(firstSeen) : null);
				row.put("lastSeen",  lastSeen  != null ? tsFmt.format(lastSeen)  : null);

				// Fetch SQL text preview (separate query to avoid CLOB in GROUP BY)
				row.put("querySqlTextPreview", fetchSqlTextPreview(conn, dbname, queryTextId));

				rows.add(row);
			}
			rs.close();
		}
		finally { pstmt.close(); }

		Map<String, Object> result = new LinkedHashMap<>();
		result.put("hasMemoryCols",      hasMemoryCols);
		result.put("has2017Cols",        has2017Cols);
		result.put("hasCompileDuration", hasCompileDuration);
		result.put("queries",            rows);
		return result;
	}

	// =========================================================================
	// Action: queryDetail
	// =========================================================================

	private Map<String, Object> actionQueryDetail(
			DbxConnection conn, String dbname, long queryId, boolean includePlan)
	throws Exception
	{
		SimpleDateFormat tsFmt = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
		Map<String, Object> result = new LinkedHashMap<>();
		result.put("dbname",  dbname);
		result.put("queryId", queryId);

		// Full SQL text + object context
		String fullText    = null;
		String objectCtx   = null;
		String queryTextId = null;

		String qSql = conn.quotifySqlString(""
				+ "SELECT \n"
				+ "    q.[query_text_id] \n"
				+ "   ,q.[object_name] \n"
				+ "   ,q.[schema_name] \n"
				+ "   ,qt.[query_sql_text] \n"
				+ "FROM " + t(dbname, "query_store_query") + " q \n"
				+ "INNER JOIN " + t(dbname, "query_store_query_text") + " qt ON q.[query_text_id] = qt.[query_text_id] \n"
				+ "WHERE q.[query_id] = ? \n");

		PreparedStatement ps = conn.prepareStatement(qSql);
		try
		{
			ps.setLong(1, queryId);
			ResultSet rs = ps.executeQuery();
			if (rs.next())
			{
				queryTextId = rs.getString("query_text_id");
				fullText    = clobToString(rs, "query_sql_text");
				objectCtx   = buildObjectContext(rs.getString("schema_name"), rs.getString("object_name"));
			}
			rs.close();
		}
		finally { ps.close(); }

		result.put("queryText",     fullText);
		result.put("objectContext", objectCtx);

		// ---- Detect SQL Server schema (extra columns in query_store_runtime_stats) ----
		boolean hasMemoryCols       = hasColumn(conn, dbname, "query_store_runtime_stats", "avg_query_max_used_memory");
		// Check each 2017+ column independently — they may appear in different SQL Server patch levels
		boolean hasDopCol           = hasColumn(conn, dbname, "query_store_runtime_stats", "avg_dop");
		boolean hasTempdbCol        = hasColumn(conn, dbname, "query_store_runtime_stats", "avg_tempdb_space_used");
		boolean hasLogCol           = hasColumn(conn, dbname, "query_store_runtime_stats", "avg_log_bytes_used");
		boolean hasLastCompileCol   = hasColumn(conn, dbname, "query_store_plan",          "last_compile_start_time");
		boolean hasQueryPlanHashCol = hasColumn(conn, dbname, "query_store_plan",          "query_plan_hash");
		result.put("schemaHasMemoryCols", hasMemoryCols);
		result.put("schemaHasDopCol",     hasDopCol);
		result.put("schemaHas2017Cols",   hasTempdbCol);   // true only when tempdb+log cols exist

		// ---- Plans with aggregated stats ----------------------------------------
		List<Map<String, Object>> plans = new ArrayList<>();

		StringBuilder planSql = new StringBuilder();
		planSql.append("SELECT \n");
		planSql.append("    p.[plan_id] \n");
		planSql.append("   ,p.[is_forced_plan] \n");
		planSql.append("   ,SUM(rs.[count_executions]) AS [total_executions] \n");
		planSql.append("   ,SUM(CAST(rs.[count_executions] AS BIGINT) * rs.[avg_duration])              / NULLIF(SUM(rs.[count_executions]), 0) AS [avg_duration_us] \n");
		planSql.append("   ,MIN(rs.[min_duration]) AS [min_duration_us] \n");
		planSql.append("   ,MAX(rs.[max_duration]) AS [max_duration_us] \n");
		planSql.append("   ,SUM(CAST(rs.[count_executions] AS BIGINT) * rs.[avg_cpu_time])              / NULLIF(SUM(rs.[count_executions]), 0) AS [avg_cpu_us] \n");
		planSql.append("   ,MAX(rs.[max_cpu_time]) AS [max_cpu_us] \n");
		planSql.append("   ,SUM(CAST(rs.[count_executions] AS BIGINT) * rs.[avg_logical_io_reads])      / NULLIF(SUM(rs.[count_executions]), 0) AS [avg_logical_reads] \n");
		planSql.append("   ,MAX(rs.[max_logical_io_reads]) AS [max_logical_reads] \n");
		planSql.append("   ,SUM(CAST(rs.[count_executions] AS BIGINT) * rs.[avg_physical_io_reads])     / NULLIF(SUM(rs.[count_executions]), 0) AS [avg_physical_reads] \n");
		planSql.append("   ,MAX(rs.[max_physical_io_reads])                                                                                    AS [max_physical_reads] \n");
		planSql.append("   ,SUM(CAST(rs.[count_executions] AS BIGINT) * rs.[avg_logical_io_writes])     / NULLIF(SUM(rs.[count_executions]), 0) AS [avg_logical_writes] \n");
		planSql.append("   ,MAX(rs.[max_logical_io_writes])                                                                                    AS [max_logical_writes] \n");
		planSql.append("   ,SUM(CAST(rs.[count_executions] AS BIGINT) * rs.[avg_rowcount])              / NULLIF(SUM(rs.[count_executions]), 0) AS [avg_rowcount] \n");
		// avg_query_max_used_memory is in 8-KB pages → /128.0 = MB
		if (hasMemoryCols)
		{
			planSql.append("   ,SUM(CAST(rs.[count_executions] AS BIGINT) * rs.[avg_query_max_used_memory]) / NULLIF(SUM(rs.[count_executions]), 0) / 128.0 AS [avg_memory_mb] \n");
		}
		// totals per plan — same weighted sums but without dividing by exec count
		planSql.append("   ,SUM(CAST(rs.[count_executions] AS BIGINT) * rs.[avg_duration])              AS [total_duration_us] \n");
		planSql.append("   ,SUM(CAST(rs.[count_executions] AS BIGINT) * rs.[avg_cpu_time])              AS [total_cpu_us] \n");
		planSql.append("   ,SUM(CAST(rs.[count_executions] AS BIGINT) * rs.[avg_logical_io_reads])      AS [total_logical_reads] \n");
		planSql.append("   ,SUM(CAST(rs.[count_executions] AS BIGINT) * rs.[avg_physical_io_reads])     AS [total_physical_reads] \n");
		planSql.append("   ,SUM(CAST(rs.[count_executions] AS BIGINT) * rs.[avg_logical_io_writes])     AS [total_logical_writes] \n");
		planSql.append("   ,SUM(CAST(rs.[count_executions] AS BIGINT) * rs.[avg_rowcount])              AS [total_rowcount] \n");
		if (hasMemoryCols)
		{
			planSql.append("   ,SUM(CAST(rs.[count_executions] AS BIGINT) * rs.[avg_query_max_used_memory]) / 128.0 AS [total_memory_mb] \n");
		}
		// 2017+ columns — checked independently: avg_dop may exist without avg_tempdb_space_used
		if (hasDopCol)
			planSql.append("   ,SUM(CAST(rs.[count_executions] AS BIGINT) * rs.[avg_dop]) / NULLIF(SUM(rs.[count_executions]), 0) AS [avg_dop] \n");
		if (hasTempdbCol)
			// avg_tempdb_space_used is in 8-KB pages → /128.0 = MB
			planSql.append("   ,SUM(CAST(rs.[count_executions] AS BIGINT) * rs.[avg_tempdb_space_used]) / NULLIF(SUM(rs.[count_executions]), 0) / 128.0 AS [avg_tempdb_mb] \n");
		if (hasLogCol)
			// avg_log_bytes_used is in bytes → /1048576.0 = MB
			planSql.append("   ,SUM(CAST(rs.[count_executions] AS BIGINT) * rs.[avg_log_bytes_used]) / NULLIF(SUM(rs.[count_executions]), 0) / 1048576.0 AS [avg_log_mb] \n");
		planSql.append("   ,MIN(i.[start_time]) AS [first_seen] \n");
		planSql.append("   ,MAX(i.[end_time])   AS [last_seen] \n");
		if (hasLastCompileCol)   planSql.append("   ,MIN(p.[last_compile_start_time]) AS [last_compile_start_time] \n");
		if (hasQueryPlanHashCol) planSql.append("   ,MIN(p.[query_plan_hash])       AS [query_plan_hash] \n");
		planSql.append(" FROM ").append(t(dbname, "query_store_plan")).append(" p \n");
		planSql.append(" INNER JOIN ").append(t(dbname, "query_store_runtime_stats")).append(" rs ON p.[plan_id] = rs.[plan_id] \n");
		planSql.append(" INNER JOIN ").append(t(dbname, "query_store_runtime_stats_interval")).append(" i ON rs.[runtime_stats_interval_id] = i.[runtime_stats_interval_id] \n");
		planSql.append(" WHERE p.[query_id] = ? \n");
		planSql.append(" GROUP BY p.[plan_id], p.[is_forced_plan] \n");
		planSql.append(" ORDER BY [avg_duration_us] DESC ").append(getNullsLastSql(conn)).append(" \n");

		String finalPlanSql = conn.quotifySqlString(planSql.toString());
		ps = conn.prepareStatement(finalPlanSql);
		try
		{
			ps.setLong(1, queryId);
			ResultSet rs = ps.executeQuery();
			while (rs.next())
			{
				Map<String, Object> planRow = new LinkedHashMap<>();
				long planId = rs.getLong("plan_id");
				planRow.put("planId",            planId);
				planRow.put("isForcedPlan",      safeGetBoolean(rs, "is_forced_plan"));
				planRow.put("planForcingType",   fetchPlanForcingTypeDesc(conn, dbname, planId));
				planRow.put("totalExecutions",   rs.getLong("total_executions"));
				planRow.put("avgDurationUs",     safeGetLong  (rs, "avg_duration_us"));
				planRow.put("minDurationUs",     safeGetLong  (rs, "min_duration_us"));
				planRow.put("maxDurationUs",     safeGetLong  (rs, "max_duration_us"));
				planRow.put("avgCpuUs",          safeGetLong  (rs, "avg_cpu_us"));
				planRow.put("maxCpuUs",          safeGetLong  (rs, "max_cpu_us"));
				planRow.put("avgLogicalReads",   safeGetLong  (rs, "avg_logical_reads"));
				planRow.put("maxLogicalReads",   safeGetLong  (rs, "max_logical_reads"));
				planRow.put("avgPhysicalReads",  safeGetLong  (rs, "avg_physical_reads"));
				planRow.put("maxPhysicalReads",  safeGetLong  (rs, "max_physical_reads"));
				planRow.put("avgLogicalWrites",  safeGetLong  (rs, "avg_logical_writes"));
				planRow.put("maxLogicalWrites",  safeGetLong  (rs, "max_logical_writes"));
				planRow.put("avgRowcount",       safeGetLong  (rs, "avg_rowcount"));
				if (hasMemoryCols) {
					planRow.put("avgMemoryMb",   safeGetDouble(rs, "avg_memory_mb"));
				}
				planRow.put("totalDurationUs",   safeGetLong  (rs, "total_duration_us"));
				planRow.put("totalCpuUs",        safeGetLong  (rs, "total_cpu_us"));
				planRow.put("totalLogicalReads",  safeGetLong  (rs, "total_logical_reads"));
				planRow.put("totalPhysicalReads", safeGetLong  (rs, "total_physical_reads"));
				planRow.put("totalLogicalWrites", safeGetLong  (rs, "total_logical_writes"));
				planRow.put("totalRowcount",      safeGetDouble(rs, "total_rowcount"));
				if (hasMemoryCols) {
					planRow.put("totalMemoryMb", safeGetDouble(rs, "total_memory_mb"));
				}
				if (hasDopCol)    planRow.put("avgDop",      safeGetLong  (rs, "avg_dop"));
				if (hasTempdbCol) planRow.put("avgTempdbMb", safeGetDouble(rs, "avg_tempdb_mb"));
				if (hasLogCol)    planRow.put("avgLogMb",    safeGetDouble(rs, "avg_log_mb"));

				Timestamp firstSeen = rs.getTimestamp("first_seen");
				Timestamp lastSeen  = rs.getTimestamp("last_seen");
				planRow.put("firstSeen", firstSeen != null ? tsFmt.format(firstSeen) : null);
				planRow.put("lastSeen",  lastSeen  != null ? tsFmt.format(lastSeen)  : null);

				if (hasLastCompileCol) {
					Timestamp compiledAt = rs.getTimestamp("last_compile_start_time");
					planRow.put("lastCompileStartTime", compiledAt != null ? tsFmt.format(compiledAt) : null);
				}
				if (hasQueryPlanHashCol) {
					byte[] qph = rs.getBytes("query_plan_hash");
					planRow.put("queryPlanHash", qph != null ? bytesToHex(qph) : null);
				}

				if (includePlan)
					planRow.put("queryPlan", fetchQueryPlan(conn, dbname, planId));

				plans.add(planRow);
			}
			rs.close();
		}
		finally { ps.close(); }

		result.put("plans", plans);

		// ---- Top-level summary (aggregated across all plans) --------------------
		{
			long   totalExec = 0, sumDurW = 0, sumCpuW = 0, sumReadsW = 0, sumRowsW = 0;
			double sumMemW = 0;
			long   maxDurUs = 0;
			String firstSeen = null, lastSeen = null;
			for (Map<String, Object> p : plans)
			{
				long exec = p.get("totalExecutions") instanceof Number ? ((Number) p.get("totalExecutions")).longValue() : 0;
				totalExec += exec;
				sumDurW   += exec * (p.get("avgDurationUs")   instanceof Number ? ((Number) p.get("avgDurationUs"  )).longValue()   : 0);
				sumCpuW   += exec * (p.get("avgCpuUs")        instanceof Number ? ((Number) p.get("avgCpuUs"       )).longValue()   : 0);
				sumReadsW += exec * (p.get("avgLogicalReads") instanceof Number ? ((Number) p.get("avgLogicalReads")).longValue()   : 0);
				sumRowsW  += exec * (p.get("avgRowcount")     instanceof Number ? ((Number) p.get("avgRowcount"    )).longValue()   : 0);
				if (hasMemoryCols) {
					sumMemW   += exec * (p.get("avgMemoryMb") instanceof Number ? ((Number) p.get("avgMemoryMb")).doubleValue() : 0.0);
				}
				long pd = p.get("maxDurationUs") instanceof Number ? ((Number) p.get("maxDurationUs")).longValue() : 0;
				if (pd > maxDurUs) maxDurUs = pd;
				String fs = (String) p.get("firstSeen");
				String ls = (String) p.get("lastSeen");
				if (fs != null && (firstSeen == null || fs.compareTo(firstSeen) < 0)) firstSeen = fs;
				if (ls != null && (lastSeen  == null || ls.compareTo(lastSeen)  > 0)) lastSeen  = ls;
			}
			result.put("planCount",        plans.size());
			result.put("totalExecutions",  totalExec);
			if (totalExec > 0)
			{
				result.put("avgDurationUs",    sumDurW   / totalExec);
				result.put("avgCpuUs",         sumCpuW   / totalExec);
				result.put("avgLogicalReads",  sumReadsW / totalExec);
				result.put("avgRowcount",      sumRowsW  / totalExec);
				if (hasMemoryCols) {
					result.put("avgMemoryMb",  Math.round(sumMemW / totalExec * 10.0) / 10.0);
				}
			}
			result.put("maxDurationUs",    maxDurUs);
			result.put("firstSeen",        firstSeen);
			result.put("lastSeen",         lastSeen);
		}

		// ---- Regression signal --------------------------------------------------
		if (plans.size() >= 2)
		{
			long   maxAvg = 0;
			long   minAvg = Long.MAX_VALUE;
			for (Map<String, Object> p : plans)
			{
				Object v = p.get("avgDurationUs");
				if (v instanceof Number)
				{
					long lv = ((Number) v).longValue();
					if (lv > maxAvg) maxAvg = lv;
					if (lv < minAvg) minAvg = lv;
				}
			}
			Map<String, Object> reg = new LinkedHashMap<>();
			double ratio = minAvg > 0 ? (double) maxAvg / minAvg : 0;
			reg.put("detected",   ratio >= 1.5);
			reg.put("maxMinRatio", Math.round(ratio * 100.0) / 100.0);
			reg.put("note", ratio >= 1.5
					? "Plan regression detected: slowest plan is " + String.format("%.1f", ratio) + "x slower than fastest. Consider forcing the faster plan."
					: "No significant regression between plans.");
			result.put("regressionSignal", reg);
		}
		else
		{
			Map<String, Object> reg = new LinkedHashMap<>();
			reg.put("detected",    false);
			reg.put("maxMinRatio", 1.0);
			reg.put("note",        "Single plan — no regression possible.");
			result.put("regressionSignal", reg);
		}

		// ---- Wait statistics per query ------------------------------------------
		List<Map<String, Object>> waits = new ArrayList<>();
		try
		{
			String waitSql = conn.quotifySqlString(""
					+ "SELECT \n"
					+ "     w.[wait_category_desc] \n"
					+ "    ,SUM(w.[total_query_wait_time_ms])  AS [total_wait_ms] \n"
					+ "    ,AVG(w.[avg_query_wait_time_ms])    AS [avg_wait_ms] \n"
					+ "    ,MAX(w.[max_query_wait_time_ms])    AS [max_wait_ms] \n"
					+ "FROM " + t(dbname, "query_store_wait_stats") + " w \n"
					+ "INNER JOIN " + t(dbname, "query_store_plan") + " p ON w.[plan_id] = p.[plan_id] \n"
					+ "WHERE p.[query_id] = ? \n"
					+ "GROUP BY w.[wait_category_desc] \n"
					+ "ORDER BY [total_wait_ms] DESC " + getNullsLastSql(conn) + " \n");

			ps = conn.prepareStatement(waitSql);
			try
			{
				ps.setLong(1, queryId);
				ResultSet rs = ps.executeQuery();
				while (rs.next())
				{
					Map<String, Object> w = new LinkedHashMap<>();
					w.put("waitCategory", safeGetStr   (rs, "wait_category_desc"));
					w.put("totalWaitMs",  safeGetLong  (rs, "total_wait_ms"));
					w.put("avgWaitMs",    safeGetDouble(rs, "avg_wait_ms"));
					w.put("maxWaitMs",    safeGetLong  (rs, "max_wait_ms"));
					waits.add(w);
				}
				rs.close();
			}
			finally { ps.close(); }
		}
		catch (Exception ex)
		{
			_logger.debug("QueryStoreServlet.queryDetail: wait stats not available for {}: {}", dbname, ex.getMessage());
		}
		result.put("waitStats", waits);

		// ---- Hourly timeline (last TIMELINE_INTERVALS intervals) ----------------
		List<Map<String, Object>> timeline = new ArrayList<>();
		try
		{
			StringBuilder tlSql = new StringBuilder();
			tlSql.append("SELECT \n");
			tlSql.append(getTopSql(conn, TIMELINE_INTERVALS));
			tlSql.append("    i.[start_time] \n");
			tlSql.append("   ,i.[end_time] \n");
			tlSql.append("   ,SUM(rs.[count_executions]) AS [total_executions] \n");
			tlSql.append("   ,SUM(CAST(rs.[count_executions] AS BIGINT) * rs.[avg_duration])              / NULLIF(SUM(rs.[count_executions]), 0) AS [avg_duration_us] \n");
			tlSql.append("   ,MIN(rs.[min_duration]) AS [min_duration_us] \n");
			tlSql.append("   ,MAX(rs.[max_duration]) AS [max_duration_us] \n");
			tlSql.append("   ,SUM(CAST(rs.[count_executions] AS BIGINT) * rs.[avg_cpu_time])              / NULLIF(SUM(rs.[count_executions]), 0) AS [avg_cpu_us] \n");
			tlSql.append("   ,SUM(CAST(rs.[count_executions] AS BIGINT) * rs.[avg_logical_io_reads])      / NULLIF(SUM(rs.[count_executions]), 0) AS [avg_logical_reads] \n");
			tlSql.append("   ,SUM(CAST(rs.[count_executions] AS BIGINT) * rs.[avg_physical_io_reads])     / NULLIF(SUM(rs.[count_executions]), 0) AS [avg_physical_reads] \n");
			tlSql.append("   ,SUM(CAST(rs.[count_executions] AS BIGINT) * rs.[avg_logical_io_writes])     / NULLIF(SUM(rs.[count_executions]), 0) AS [avg_logical_writes] \n");
			tlSql.append("   ,SUM(CAST(rs.[count_executions] AS BIGINT) * rs.[avg_rowcount])              / NULLIF(SUM(rs.[count_executions]), 0) AS [avg_rowcount] \n");
			// avg_query_max_used_memory is in 8-KB pages → /128.0 = MB
			if (hasMemoryCols)
			{
				tlSql.append("   ,SUM(CAST(rs.[count_executions] AS BIGINT) * rs.[avg_query_max_used_memory]) / NULLIF(SUM(rs.[count_executions]), 0) / 128.0 AS [avg_memory_mb] \n");
			}
			// SQL Server 2017+ columns — checked independently (may appear in different patch levels)
			if (hasDopCol)
				tlSql.append("   ,SUM(CAST(rs.[count_executions] AS BIGINT) * rs.[avg_dop])               / NULLIF(SUM(rs.[count_executions]), 0) AS [avg_dop] \n");
			if (hasTempdbCol)
				tlSql.append("   ,SUM(CAST(rs.[count_executions] AS BIGINT) * rs.[avg_tempdb_space_used]) / NULLIF(SUM(rs.[count_executions]), 0) / 128.0 AS [avg_tempdb_mb] \n");
			if (hasLogCol)
				tlSql.append("   ,SUM(CAST(rs.[count_executions] AS BIGINT) * rs.[avg_log_bytes_used])    / NULLIF(SUM(rs.[count_executions]), 0) / 1048576.0 AS [avg_log_mb] \n");
			tlSql.append("   ,1 AS [dummy_eol] \n");
			tlSql.append("FROM ").append(t(dbname, "query_store_runtime_stats")).append(" rs \n");
			tlSql.append("INNER JOIN ").append(t(dbname, "query_store_runtime_stats_interval")).append(" i ON rs.[runtime_stats_interval_id] = i.[runtime_stats_interval_id] \n");
			tlSql.append("INNER JOIN ").append(t(dbname, "query_store_plan")).append(" p ON rs.[plan_id] = p.[plan_id] \n");
			tlSql.append("WHERE p.[query_id] = ? \n");
			tlSql.append("GROUP BY i.[runtime_stats_interval_id], i.[start_time], i.[end_time] \n");
			tlSql.append("ORDER BY i.[start_time] DESC ").append(getNullsLastSql(conn)).append(" \n");
			tlSql.append(getLimitSql(conn, TIMELINE_INTERVALS));

			String finalTlSql = conn.quotifySqlString(tlSql.toString());
			ps = conn.prepareStatement(finalTlSql);
			try
			{
				ps.setLong(1, queryId);
				ResultSet rs = ps.executeQuery();
				while (rs.next())
				{
					Map<String, Object> tl = new LinkedHashMap<>();
					Timestamp tStart = rs.getTimestamp("start_time");
					Timestamp tEnd   = rs.getTimestamp("end_time");
					tl.put("intervalStart",  tStart != null ? tsFmt.format(tStart) : null);
					tl.put("intervalEnd",     tEnd   != null ? tsFmt.format(tEnd)   : null);
					tl.put("totalExecutions", rs.getLong("total_executions"));
					tl.put("avgDurationUs",   safeGetLong(rs, "avg_duration_us"));
					tl.put("minDurationUs",   safeGetLong(rs, "min_duration_us"));
					tl.put("maxDurationUs",   safeGetLong(rs, "max_duration_us"));
					tl.put("avgCpuUs",        safeGetLong(rs, "avg_cpu_us"));
					tl.put("avgLogicalReads",  safeGetLong(rs, "avg_logical_reads"));
					tl.put("avgPhysicalReads", safeGetLong(rs, "avg_physical_reads"));
					tl.put("avgLogicalWrites", safeGetLong(rs, "avg_logical_writes"));
					tl.put("avgRowcount",      safeGetLong(rs, "avg_rowcount"));
					if (hasMemoryCols) {
						tl.put("avgMemoryMb", safeGetDouble(rs, "avg_memory_mb"));
					}
					if (hasDopCol)    tl.put("avgDop",      safeGetLong  (rs, "avg_dop"));
					if (hasTempdbCol) tl.put("avgTempdbMb", safeGetDouble(rs, "avg_tempdb_mb"));
					if (hasLogCol)    tl.put("avgLogMb",    safeGetDouble(rs, "avg_log_mb"));
					timeline.add(tl);
				}
				rs.close();
			}
			finally { ps.close(); }
		}
		catch (Exception ex)
		{
			_logger.debug("QueryStoreServlet.queryDetail: timeline query failed for {}: {}", dbname, ex.getMessage());
		}
		result.put("timeline", timeline);

		// ---- Tuning recommendations for this database ---------------------------
		List<Map<String, Object>> recs = new ArrayList<>();
		try
		{
			StringBuilder recSql = new StringBuilder();
			recSql.append("SELECT \n");
			recSql.append(getTopSql(conn, 20));
			recSql.append("    [name] \n");
			recSql.append("   ,[type] \n");
			recSql.append("   ,[reason] \n");
			recSql.append("   ,[score] \n");
			recSql.append("   ,[state] \n");
			recSql.append("   ,[details] \n");
			recSql.append(" FROM ").append(t(dbname, "dm_db_tuning_recommendations")).append(" \n");
			recSql.append(" ORDER BY [score] DESC ").append(getNullsLastSql(conn)).append(" \n");
			recSql.append(getLimitSql(conn, 20));

			String finalRecSql = conn.quotifySqlString(recSql.toString());
			ps = conn.prepareStatement(finalRecSql);
			try
			{
				ResultSet rs = ps.executeQuery();
				while (rs.next())
				{
					Map<String, Object> rec = new LinkedHashMap<>();
					rec.put("name",    safeGetStr   (rs, "name"));
					rec.put("type",    safeGetStr   (rs, "type"));
					rec.put("reason",  safeGetStr   (rs, "reason"));
					rec.put("score",   safeGetDouble(rs, "score"));
					rec.put("state",   safeGetStr   (rs, "state"));
					rec.put("details", clobToString (rs, "details")); // JSON blob from SQL Server
					recs.add(rec);
				}
				rs.close();
			}
			finally { ps.close(); }
		}
		catch (Exception ex)
		{
			_logger.debug("QueryStoreServlet.queryDetail: recommendations not available for {}: {}", dbname, ex.getMessage());
		}
		result.put("recommendations", recs);

		return result;
	}

	// =========================================================================
	// Helpers
	// =========================================================================

	/**
	 * Returns the DBMS-independent bracketed schema+table reference for a qs: schema.
	 * e.g. t("AdventureWorks", "query_store_query") → [qs:AdventureWorks].[query_store_query]
	 * This MUST be passed through conn.quotifySqlString(sql) before execution.
	 */
	private static String t(String dbname, String table)
	{
		String safeDb    = dbname.replace("[", "").replace("]", "").replace(";", "");
		String safeTable = table.replace("[", "").replace("]", "");
		if (safeTable.isEmpty())
			return "[qs:" + safeDb + "]";
		return "[qs:" + safeDb + "].[" + safeTable + "]";
	}

	/** Returns " TOP N " for SQL Server/Sybase, or blank for others (which use LIMIT) */
	private static String getTopSql(DbxConnection conn, int topN)
	{
		if (topN <= 0) 
			return "";

		String prod = DbUtils.getProductName(conn);
		if (DbUtils.isProductName(prod, DbUtils.DB_PROD_NAME_MSSQL, DbUtils.DB_PROD_NAME_SYBASE_ASE))
			return " TOP " + topN + " ";

		return "";
	}

	/** Returns " LIMIT N" for H2/Postgres/MySQL, or blank for SQL Server/Sybase (which use TOP) */
	private static String getLimitSql(DbxConnection conn, int topN)
	{
		if (topN <= 0) 
			return "";

		String prod = DbUtils.getProductName(conn);
		if (DbUtils.isProductName(prod, DbUtils.DB_PROD_NAME_MSSQL, DbUtils.DB_PROD_NAME_SYBASE_ASE))
			return "";

		return " LIMIT " + topN;
	}

	/** Returns " NULLS LAST" if supported (H2, Postgres, etc), or blank (SQL Server) */
	private static String getNullsLastSql(DbxConnection conn)
	{
		String prod = DbUtils.getProductName(conn);
		if (DbUtils.isProductName(prod, DbUtils.DB_PROD_NAME_MSSQL, DbUtils.DB_PROD_NAME_SYBASE_ASE))
			return "";

		return " NULLS LAST";
	}

	private static String buildObjectContext(String schemaName, String objectName)
	{
		if (objectName == null || objectName.isEmpty())
			return null;
		if (schemaName != null && !schemaName.isEmpty())
			return schemaName + "." + objectName;
		return objectName;
	}

	private String fetchSqlTextPreview(DbxConnection conn, String dbname, long queryTextId)
	{
		try
		{
			String sql = conn.quotifySqlString(
					"SELECT [query_sql_text] FROM " + t(dbname, "query_store_query_text") + " WHERE [query_text_id] = ?");
			PreparedStatement ps = conn.prepareStatement(sql);
			try
			{
				ps.setLong(1, queryTextId);
				ResultSet rs = ps.executeQuery();
				if (rs.next())
				{
					String text = clobToString(rs, "query_sql_text");
					rs.close();
					if (text != null && text.length() > SQL_PREVIEW_LEN)
						return text.substring(0, SQL_PREVIEW_LEN) + "…";
					return text;
				}
				rs.close();
			}
			finally { ps.close(); }
		}
		catch (Exception ex)
		{
			_logger.debug("QueryStoreServlet.fetchSqlTextPreview: {}", ex.getMessage());
		}
		return null;
	}

	private String fetchPlanForcingTypeDesc(DbxConnection conn, String dbname, long planId)
	{
		try
		{
			String sql = conn.quotifySqlString(
					"SELECT [plan_forcing_type_desc] FROM " + t(dbname, "query_store_plan") + " WHERE [plan_id] = ?");
			PreparedStatement ps = conn.prepareStatement(sql);
			try
			{
				ps.setLong(1, planId);
				ResultSet rs = ps.executeQuery();
				String desc = rs.next() ? rs.getString(1) : null;
				rs.close();
				return desc;
			}
			finally { ps.close(); }
		}
		catch (Exception ignored) {}
		return null;
	}

	// =========================================================================
	// Last Actual Plan (dm_exec_query_plan_stats)
	// =========================================================================

	/**
	 * Tries to fetch the last-known actual execution plan for a Query Store plan_id.
	 *
	 * <p>Strategy:
	 * <ol>
	 *   <li>Read {@code query_plan_hash} from the H2 Query Store extract
	 *       ({@code plan_handle} is not stored in the extract).</li>
	 *   <li>On the live SQL Server, look up the corresponding {@code plan_handle} from
	 *       {@code sys.dm_exec_query_stats WHERE query_plan_hash = ?}, then call
	 *       {@code sys.dm_exec_query_plan_stats(plan_handle)}.
	 *       Requires SQL Server 2019+ (or trace flag 2451 on 2016/2017) and
	 *       {@code LAST_QUERY_PLAN_STATS=ON} on the database.</li>
	 *   <li>If not found or the plan is no longer in cache, query {@code MonDdlStorage}
	 *       for a plan captured by the active-statements collector using the matching
	 *       {@code query_plan_hash}.</li>
	 * </ol>
	 *
	 * @return JSON-serialisable map with {@code found}, {@code source}, {@code xml} on success
	 *         or {@code found=false} with diagnostic keys on failure.
	 */
	private Map<String, Object> actionLastActualPlan(DbxConnection h2Conn, String dbname, long planId)
	{
		Map<String, Object> result = new LinkedHashMap<>();
		result.put("planId", planId);
		result.put("dbname", dbname);

		boolean triedPlanStats  = false;
		boolean triedDdlStorage = false;

		// ── Step 1: read query_plan_hash from H2 ──────────────────────────────
		// plan_handle is NOT stored in the QS extract; query_plan_hash is used to
		// map back to an active plan_handle via sys.dm_exec_query_stats on the live server.
		String queryPlanHashHex = fetchQueryPlanHashHex(h2Conn, dbname, planId);
		if (queryPlanHashHex == null)
		{
			result.put("planStatsError", "query_plan_hash not available in the Query Store extract for plan_id=" + planId);
		}
		else
		{
			// ── Step 2: resolve plan_handle → dm_exec_query_plan_stats ────────
			triedPlanStats = true;
			DbxConnection liveConn = null;
			try
			{
				liveConn = CounterController.getInstance().cloneMonConnection("QueryStore-lastActualPlan");
				String xml = fetchLastActualPlanFromSqlServer(liveConn, queryPlanHashHex);
				if (xml != null && !xml.isBlank())
				{
					result.put("found",  true);
					result.put("source", "dm_exec_query_plan_stats");
					result.put("xml",    xml);
					return result;
				}
				// NULL: plan_handle not in dm_exec_query_stats (evicted) or feature not enabled
				result.put("planStatsError",
						"plan_handle not found in sys.dm_exec_query_stats for query_plan_hash=" + queryPlanHashHex
						+ " — query may have been evicted from the plan cache, "
						+ "or requires SQL Server 2019+ / trace flag 2451 / LAST_QUERY_PLAN_STATS=ON");
			}
			catch (Exception ex)
			{
				_logger.warn("actionLastActualPlan: dm_exec_query_plan_stats failed for plan_id={}: {}", planId, ex.getMessage());
				result.put("planStatsError", ex.getMessage());
			}
			finally
			{
				if (liveConn != null) liveConn.closeNoThrow();
			}
		}

		// ── Step 3: DDL Storage fallback — look for a live-captured plan ──────
		triedDdlStorage = true;
		try
		{
			// MonDdlStorage stores plans captured from active statements with type='SS'.
			// The objectName used by the collector is the hex query_plan_hash so we can
			// look up by the hash we already have from the H2 extract.
			String queryPlanHash = fetchQueryPlanHashHex(h2Conn, dbname, planId);
			if (queryPlanHash != null)
			{
				String ddlXml = fetchPlanFromDdlStorage(h2Conn, queryPlanHash);
				if (ddlXml != null && !ddlXml.isBlank())
				{
					result.put("found",  true);
					result.put("source", "ddl_storage");
					result.put("xml",    ddlXml);
					return result;
				}
			}
		}
		catch (Exception ex)
		{
			_logger.debug("actionLastActualPlan: DDL Storage fallback failed for plan_id={}: {}", planId, ex.getMessage());
			result.put("ddlStorageError", ex.getMessage());
		}

		// ── Not found ─────────────────────────────────────────────────────────
		result.put("found",           false);
		result.put("triedPlanStats",  triedPlanStats);
		result.put("triedDdlStorage", triedDdlStorage);
		result.put("message",
				"Last actual plan not found."
				+ (triedPlanStats  ? " Tried dm_exec_query_plan_stats (plan not in cache or SQL Server < 2019)." : "")
				+ (triedDdlStorage ? " Tried DDL Storage (no matching live-captured plan)." : ""));
		return result;
	}

	/** Read the query_plan_hash as a hex string for a given plan_id from the H2 extract. */
	private String fetchQueryPlanHashHex(DbxConnection conn, String dbname, long planId)
	{
//System.out.println("fetchQueryPlanHashHex(dbname='"+dbname+"', planId="+planId+")--START. conn=" + conn);
		try
		{
			String sql = conn.quotifySqlString(""
					+ "SELECT [query_plan_hash] \n"
					+ "FROM " + t(dbname, "query_store_plan") + " \n"
					+ "WHERE [plan_id] = ?");

			try (PreparedStatement ps = conn.prepareStatement(sql))
			{
				ps.setLong(1, planId);

				String retStr = null;
				try (ResultSet rs = ps.executeQuery())
				{
					while(rs.next())
					{
						retStr = bytesToHex(rs.getBytes("query_plan_hash"));
//System.out.println("fetchQueryPlanHashHex(dbname='"+dbname+"', planId="+planId+"): query_plan_hash='" + retStr + "'.");
					}
				}
				return retStr;
			}
		}
		catch (Exception ex)
		{
			_logger.error("fetchQueryPlanHashHex: plan_id={}, conn=|{}|: {}", planId, conn, ex.getMessage());
		}
		return null;
	}

	/**
	 * Resolve a {@code query_plan_hash} to a {@code plan_handle} via
	 * {@code sys.dm_exec_query_stats}, then call {@code sys.dm_exec_query_plan_stats(plan_handle)}
	 * to retrieve the last-known actual execution plan XML.
	 *
	 * <p>Returns {@code null} when the hash is not found in the plan cache (query evicted)
	 * or when {@code dm_exec_query_plan_stats} returns no row (feature not enabled).
	 * The caller is responsible for opening and closing {@code liveConn}.
	 *
	 * @param liveConn       open connection to the monitored SQL Server
	 * @param queryPlanHashHex  hex string of the query_plan_hash, e.g. {@code "0xAF3473A807A1935B"}
	 */
	private String fetchLastActualPlanFromSqlServer(DbxConnection liveConn, String queryPlanHashHex)
	throws Exception
	{
		_logger.info("QueryStoreServlet: resolving query_plan_hash={} via dm_exec_query_stats", queryPlanHashHex);

		// ── Step 1: map query_plan_hash → plan_handle ─────────────────────────
		// Multiple entries may share the same hash; take the most recently executed one.
		String findHandleSql = ""
				+ "SELECT TOP 1 [plan_handle] \n"
				+ "FROM sys.dm_exec_query_stats \n"
				+ "WHERE [query_plan_hash] = CONVERT(VARBINARY(8), ?, 1) \n"
				+ "ORDER BY [last_execution_time] DESC \n";
		byte[] planHandle = null;
		PreparedStatement ps = liveConn.prepareStatement(findHandleSql);
		try
		{
			ps.setString(1, queryPlanHashHex);
			ResultSet rs = ps.executeQuery();
			if (rs.next()) planHandle = rs.getBytes("plan_handle");
			rs.close();
		}
		finally { ps.close(); }

		if (planHandle == null)
		{
			_logger.debug("QueryStoreServlet: query_plan_hash={} not found in dm_exec_query_stats (plan evicted?)", queryPlanHashHex);
			return null;
		}
		_logger.info("QueryStoreServlet: found plan_handle={}, calling dm_exec_query_plan_stats", StringUtil.bytesToHex("0x", planHandle));

		// ── Step 2: fetch last actual plan XML ───────────────────────────────
		String sql = "SELECT [query_plan] FROM sys.dm_exec_query_plan_stats(?)";
		ps = liveConn.prepareStatement(sql);
		try
		{
			ps.setBytes(1, planHandle);
			ResultSet rs = ps.executeQuery();
			String xml = rs.next() ? clobToString(rs, "query_plan") : null;
			rs.close();
			return xml;
		}
		finally { ps.close(); }
	}

	/**
	 * Search MonDdlStorage for an execution plan (type='SS') whose objectName matches
	 * the given query_plan_hash hex string — these are stored by the active-statements
	 * collector when it captures live plans.
	 */
	private String fetchPlanFromDdlStorage(DbxConnection h2Conn, String queryPlanHashHex)
	throws Exception
	{
		// MonDdlStorage lives in the root H2 schema (no qs: prefix).
		String sql = h2Conn.quotifySqlString(""
				+ "SELECT TOP 1 [objectText] \n"
				+ "FROM [MonDdlStorage] \n"
				+ "WHERE [type] = 'SS' AND upper([objectName]) = upper(?) \n"
				+ "ORDER BY [sampleTime] DESC \n");
		PreparedStatement ps = h2Conn.prepareStatement(sql);
		try
		{
			ps.setString(1, queryPlanHashHex);
			ResultSet rs = ps.executeQuery();
			String xml = rs.next() ? clobToString(rs, "objectText") : null;
			rs.close();
			return xml;
		}
		finally { ps.close(); }
	}

	/** Convert a byte array to an uppercase hex string prefixed with "0x". */
	private static String bytesToHex(byte[] bytes)
	{
		if (bytes == null) 
			return null;

		StringBuilder sb = new StringBuilder(bytes.length * 2 + 2);
		sb.append("0x");
		for (byte b : bytes)
			sb.append(String.format("%02X", b));

		return sb.toString();
	}

	private String fetchQueryPlan(DbxConnection conn, String dbname, long planId)
	{
		try
		{
			String sql = conn.quotifySqlString(
					"SELECT [query_plan] FROM " + t(dbname, "query_store_plan") + " WHERE [plan_id] = ?");
			PreparedStatement ps = conn.prepareStatement(sql);
			try
			{
				ps.setLong(1, planId);
				ResultSet rs = ps.executeQuery();
				String plan = rs.next() ? clobToString(rs, "query_plan") : null;
				rs.close();
				return plan;
			}
			finally { ps.close(); }
		}
		catch (Exception ex)
		{
			_logger.debug("QueryStoreServlet.fetchQueryPlan: plan_id={}: {}", planId, ex.getMessage());
		}
		return null;
	}

	private static long safeCountTable(DbxConnection conn, String dbname, String table)
	{
		try
		{
			String sql = conn.quotifySqlString("SELECT COUNT(*) FROM " + t(dbname, table));
			PreparedStatement ps = conn.prepareStatement(sql);
			try
			{
				ResultSet rs = ps.executeQuery();
				long n = rs.next() ? rs.getLong(1) : 0;
				rs.close();
				return n;
			}
			finally { ps.close(); }
		}
		catch (Exception ex) { return -1; } // -1 = table doesn't exist in this extraction
	}

	private static DbxConnection getConnectionForTimestamp(Timestamp ts)
	{
		if (!PersistentCounterHandler.hasInstance())
			return null;
		PersistWriterJdbc writer = PersistentCounterHandler.getInstance().getPersistWriterJdbc();
		if (writer == null)
			return null;
		return CmDataServlet.getConnectionForTimestamp(ts, writer);
	}

	private static String clobToString(ResultSet rs, String col)
	throws Exception
	{
		Object obj = rs.getObject(col);
		if (obj == null)
			return null;
		if (obj instanceof Clob)
		{
			Clob clob = (Clob) obj;
			return clob.getSubString(1, (int) clob.length());
		}
		return obj.toString();
	}

	/**
	 * Returns true if {@code columnName} exists in the H2 table identified by
	 * {@code schemaName.tableName}.  Uses JDBC metadata so it works regardless
	 * of SQL Server version — the H2 schema mirrors what the collector extracted.
	 * Results are NOT cached; callers should check once and reuse the boolean.
	 */
	private static boolean hasColumn(DbxConnection conn, String dbname, String tableName, String columnName)
	{
		try
		{
			// Schema in H2 is created with quotes as "qs:dbname" by SqlServerQueryStoreExtractor.
			// Quoted identifiers in H2 are case-sensitive.
			String schemaName = "qs:" + dbname;
			
			DatabaseMetaData meta = conn.getMetaData();
			// We try the original case (which is what our extractor uses for schema and tables)
			try (ResultSet rs = meta.getColumns(null, schemaName, tableName, columnName))
			{
				if (rs.next()) return true;
			}
			// Fallback: try upper case (some DBMS or older H2 versions might normalize to upper)
			try (ResultSet rs = meta.getColumns(null, schemaName.toUpperCase(), tableName.toUpperCase(), columnName.toUpperCase()))
			{
				return rs.next();
			}
		}
		catch (Exception e) { return false; }
	}

	private Object safeGetStr(ResultSet rs, String col)
	{
		try { String v = rs.getString(col); return rs.wasNull() ? null : v; }
		catch (Exception e) { _logger.error("Problems reading String value for column '" + col + "', returning NULL.", e); return null; }
	}

	private static Long safeGetLong(ResultSet rs, String col)
	{
		try { long v = rs.getLong(col); return rs.wasNull() ? null : v; }
		catch (Exception e) { _logger.error("Problems reading Long value for column '" + col + "', returning NULL.", e); return null; }
	}

	private static Double safeGetDouble(ResultSet rs, String col)
	{
		try { double v = rs.getDouble(col); return rs.wasNull() ? null : v; }
		catch (Exception e) { _logger.error("Problems reading Double value for column '" + col + "', returning NULL.", e); return null; }
	}

	private static Boolean safeGetBoolean(ResultSet rs, String col)
	{
		try { boolean v = rs.getBoolean(col); return rs.wasNull() ? null : v; }
		catch (Exception e) { _logger.error("Problems reading Boolean value for column '" + col + "', returning NULL.", e); return null; }
	}

	private static Map<String, Object> errMap(String code, String message)
	{
		Map<String, Object> m = new LinkedHashMap<>();
		m.put("error",   code);
		m.put("message", message);
		return m;
	}
}
