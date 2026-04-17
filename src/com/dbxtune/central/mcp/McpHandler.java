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
package com.dbxtune.central.mcp;

import java.io.File;
import java.lang.invoke.MethodHandles;
import java.net.ConnectException;
import java.time.Instant;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.dbxtune.Version;
import com.dbxtune.central.DbxTuneCentral;
import com.dbxtune.central.alarm.AlarmMuteManager;
import com.dbxtune.central.controllers.OverviewServlet;
import com.dbxtune.central.pcs.CentralPersistReader;
import com.dbxtune.utils.Configuration;
import com.dbxtune.utils.HtmlQueryString;
import com.dbxtune.central.pcs.CentralPersistReader.SampleType;
import com.dbxtune.central.pcs.objects.DbxAlarmActive;
import com.dbxtune.central.pcs.objects.DbxAlarmHistory;
import com.dbxtune.central.pcs.objects.DbxCentralSessions;
import com.dbxtune.central.pcs.objects.DbxGraphData;
import com.dbxtune.central.pcs.objects.DbxGraphProperties;
import com.dbxtune.sql.SqlParserUtils;
import com.dbxtune.utils.TimeUtils;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

/**
 * Implements the MCP (Model Context Protocol) message handling for DbxCentral.
 * <p>
 * Handles all MCP methods:
 * <ul>
 *   <li>{@code initialize}    — protocol handshake</li>
 *   <li>{@code tools/list}    — list available tools</li>
 *   <li>{@code tools/call}    — execute a tool</li>
 *   <li>{@code resources/list} — list data resources</li>
 *   <li>{@code resources/read} — read a resource by URI</li>
 *   <li>{@code prompts/list}  — list guided analysis prompts</li>
 *   <li>{@code prompts/get}   — get a specific prompt template</li>
 * </ul>
 * <p>
 * Daily Summary Report (DSR) tools:
 * <ul>
 *   <li>{@code list_daily_reports}       — list saved DSR files for a server</li>
 *   <li>{@code get_daily_report_section} — extract a section from a DSR as plain text</li>
 * </ul>
 */
public class McpHandler
{
	private static final Logger _logger = LogManager.getLogger(MethodHandles.lookup().lookupClass());

	/** MCP protocol version this server implements */
	private static final String MCP_PROTOCOL_VERSION = "2024-11-05";

	/** Max plain-text characters returned per DSR section (avoid overwhelming context windows) */
	private static final int DSR_SECTION_MAX_CHARS = 120_000;

	/** Regex that marks the start of a new DSR section in the HTML */
	private static final Pattern DSR_SECTION_SEPARATOR = Pattern.compile(
		"<!-- =====+? -->\r?\n<!-- (.+?) -->\r?\n<!-- =====+? -->",
		Pattern.DOTALL
	);

	private final int     _maxTimeRangeHours;
	private final boolean _exposeDbmsConfig;
	private final ObjectMapper _om = new ObjectMapper();

	/** Shared HTTP client for collector proxy calls (e.g. get_dbms_config). */
	private final HttpClient _httpClient = HttpClient.newBuilder()
			.followRedirects(HttpClient.Redirect.NORMAL)
			.build();

	// -------------------------------------------------------------------------
	// Custom exceptions for JSON-RPC error mapping
	// -------------------------------------------------------------------------

	public static class McpMethodNotFoundException 
	extends RuntimeException
	{
		private static final long serialVersionUID = 1L;

		public McpMethodNotFoundException(String method) { super(method); }
	}

	public static class McpInvalidParamsException 
	extends RuntimeException
	{
		private static final long serialVersionUID = 1L;

		public McpInvalidParamsException(String msg) { super(msg); }
	}

	// -------------------------------------------------------------------------

	public McpHandler(int maxTimeRangeHours, boolean exposeDbmsConfig)
	{
		_maxTimeRangeHours = maxTimeRangeHours;
		_exposeDbmsConfig  = exposeDbmsConfig;
	}

	// -------------------------------------------------------------------------
	// Main dispatch
	// -------------------------------------------------------------------------

	public JsonNode handle(String method, JsonNode params) 
	throws Exception
	{
		switch (method)
		{
			case "initialize":                return handleInitialize(params);
			case "notifications/initialized": return _om.createObjectNode(); // client notification, no response needed
			case "tools/list":                return handleToolsList();
			case "tools/call":                return handleToolsCall(params);
			case "resources/list":            return handleResourcesList();
			case "resources/read":            return handleResourcesRead(params);
			case "prompts/list":              return handlePromptsList();
			case "prompts/get":               return handlePromptsGet(params);
			default:                          throw new McpMethodNotFoundException(method);
		}
	}

	// =========================================================================
	// initialize
	// =========================================================================

	private JsonNode handleInitialize(JsonNode params)
	{
		ObjectNode capabilities = _om.createObjectNode();
		capabilities.set("tools",     _om.createObjectNode());
		capabilities.set("resources", _om.createObjectNode());
		capabilities.set("prompts",   _om.createObjectNode());

		ObjectNode serverInfo = _om.createObjectNode();
		serverInfo.put("name",    "DbxCentral");
		serverInfo.put("version", Version.getVersionStr());

		ObjectNode result = _om.createObjectNode();
		result.put("protocolVersion", MCP_PROTOCOL_VERSION);
		result.set("capabilities",    capabilities);
		result.set("serverInfo",      serverInfo);
		result.put("instructions",
				"DbxCentral monitors database performance across multiple servers (SQL Server, PostgreSQL, MySQL, Oracle, Sybase ASE, and more). "
				+ "Use list_servers to discover monitored servers, list_metrics to see available performance counters, "
				+ "and the analysis tools to investigate performance issues. "
				+ "Time ranges use relative syntax like '-2h' (last 2 hours), '-30m' (last 30 minutes), '-7d' (last 7 days), "
				+ "or absolute ISO-8601 timestamps.");
		return result;
	}

	// =========================================================================
	// tools/list
	// =========================================================================

	private JsonNode handleToolsList()
	{
		ArrayNode tools = _om.createArrayNode();

		// -- Discovery --------------------------------------------------------
		tools.add(buildTool("list_servers",
				"List all monitored database servers with their current status, DBMS type, version, and last sample time.",
				buildSchema()));

		tools.add(buildTool("list_metrics",
				"List all available performance counter graphs for a specific server. Returns counter model names, graph names, labels, and categories.",
				buildSchema(
					"serverName", "string", "Name of the monitored database server (as returned by list_servers)", true
				)));

		// -- Performance data -------------------------------------------------
		tools.add(buildTool("get_metric_data",
				"Fetch time-series performance data for a specific metric graph. Use list_metrics to discover available cmName/graphName combinations.",
				buildSchema(
					"serverName",   "string", "Name of the monitored server",                                            true,
					"cmName",       "string", "Counter model name (e.g. CmWaitStats, CmExecQueryStats, CmSummary)",      true,
					"graphName",    "string", "Graph name within the counter model (e.g. aaCpuGraph, aaWaitGraph)",      true,
					"startTime",    "string", "Start of time range. Relative: '-2h', '-30m', '-7d'. Default: '-2h'",     false,
					"endTime",      "string", "End of time range. Default: 'now'",                                        false,
					"aggregation",  "string", "Aggregation mode: ALL, AUTO, AVG_OVER_MINUTES, MAX_OVER_MINUTES, SUM_OVER_MINUTES. Default: AUTO", false
				)));

		tools.add(buildTool("get_performance_summary",
				"Get a high-level performance overview for a server: CPU, active sessions, memory, and I/O from the CmSummary counter.",
				buildSchema(
					"serverName",  "string", "Name of the monitored server",                         true,
					"startTime",   "string", "Start of time range. Default: '-1h'",                  false,
					"endTime",     "string", "End of time range. Default: 'now'",                    false
				)));

		// -- Wait & Blocking --------------------------------------------------
		tools.add(buildTool("get_wait_stats",
				"Get wait statistics ranked by total wait time. This is the primary starting point for SQL Server performance root-cause analysis. "
				+ "Key wait types: PAGEIOLATCH (I/O), CXPACKET/CXCONSUMER (parallelism), LCK_M_* (locking/blocking), "
				+ "SOS_SCHEDULER_YIELD (CPU pressure), RESOURCE_SEMAPHORE (memory grants). "
				+ "Returns an aggregated summary by default (total/avg/max per wait type). Set topN=0 for raw time-series.",
				buildSchema(
					"serverName",  "string", "Name of the monitored server",                                          true,
					"startTime",   "string", "Start of time range. Default: '-1h'",                                   false,
					"endTime",     "string", "End of time range. Default: 'now'",                                     false,
					"topN",        "number", "Max entries to return per graph, ranked by total. Default: 25. Set 0 for raw time-series.", false
				)));

		tools.add(buildTool("get_blocking_info",
				"Get blocking chains and deadlock information. Shows sessions that are blocked and the sessions causing the blocks.",
				buildSchema(
					"serverName",  "string", "Name of the monitored server",    true,
					"startTime",   "string", "Start of time range. Default: '-1h'",   false,
					"endTime",     "string", "End of time range. Default: 'now'",      false
				)));

		// -- Query performance ------------------------------------------------
		tools.add(buildTool("get_top_queries",
				"Get the most expensive queries ranked by elapsed time, CPU, logical reads, or execution count. "
				+ "Returns an aggregated summary by default (total/avg/max per query identifier). Set topN=0 for raw time-series.",
				buildSchema(
					"serverName",  "string", "Name of the monitored server",                                                          true,
					"startTime",   "string", "Start of time range. Default: '-1h'",                                                   false,
					"endTime",     "string", "End of time range. Default: 'now'",                                                     false,
					"rankBy",      "string", "Ranking metric: elapsed, cpu, reads, executions. Default: elapsed",                     false,
					"topN",        "number", "Max entries to return per graph, ranked by total. Default: 25. Set 0 for raw time-series.", false
				)));

		tools.add(buildTool("get_active_statements",
				"Get active SQL statements with full session context, referenced tables, and execution plans.\n"
				+ "Data is stored as JSON snapshots in DbxCmLastSampleJson (live) / DbxCmHistorySampleJson (up to ~1 month history).\n"
				+ "Each statement is returned as a named-field object (not raw columns/rows) and is enriched with:\n"
				+ "  • referencedTables — table names extracted from the SQL text via SqlParserUtils (use with get_table_schema or get_query_context)\n"
				+ "  • Execution plan fields (controlled by includePlan):\n"
				+ "      SQL Server: query_plan = estimated XML plan, LiveQueryPlan = actual plan captured at snapshot time\n"
				+ "      Sybase ASE: text plan in JSON; XML plan stored separately in MonDdlStorage — use get_object_ddl(type=SS, dbname=statement_cache) to retrieve it\n"
				+ "      PostgreSQL/MySQL/other: plan format varies by DBMS\n"
				+ "Omit startTime to get the latest snapshot only. Provide startTime for historical analysis (last sample in range is returned).",
				buildSchema(
					"serverName",   "string", "Name of the monitored server",  true,
					"startTime",    "string", "Start of time range: '-2h', '-30m', 'yyyy-MM-dd HH:mm:ss'. Omit for latest snapshot only.", false,
					"endTime",      "string", "End of time range. Default: 'now'", false,
					"includePlan",  "string", "true/false — include execution plan XML/text in each statement. Can be large. Default: true.", false
				)));

		// -- Resource usage ---------------------------------------------------
		tools.add(buildTool("get_memory_analysis",
				"Analyze memory pressure: memory clerk allocations by component and memory grant requests/waits. "
				+ "Returns an aggregated summary by default. Set topN=0 for raw time-series.",
				buildSchema(
					"serverName",  "string", "Name of the monitored server",                                          true,
					"startTime",   "string", "Start of time range. Default: '-1h'",                                   false,
					"endTime",     "string", "End of time range. Default: 'now'",                                     false,
					"topN",        "number", "Max entries to return per graph, ranked by total. Default: 25. Set 0 for raw time-series.", false
				)));

		tools.add(buildTool("get_io_analysis",
				"Analyze disk I/O performance: reads, writes, and latency by device and database file. "
				+ "Returns an aggregated summary by default. Set topN=0 for raw time-series.",
				buildSchema(
					"serverName",  "string", "Name of the monitored server",                                          true,
					"startTime",   "string", "Start of time range. Default: '-1h'",                                   false,
					"endTime",     "string", "End of time range. Default: 'now'",                                     false,
					"topN",        "number", "Max entries to return per graph, ranked by total. Default: 25. Set 0 for raw time-series.", false
				)));

		tools.add(buildTool("list_cms",
				"List all Counter Models (CMs) available on the collector, with their names, descriptions, "
				+ "group, and whether they currently have data. "
				+ "Use this to discover what metrics are available before calling get_metric_data. "
				+ "Each CM description explains what it measures and what SQL source it reads from.",
				buildSchema(
					"serverName", "string", "Name of the monitored server", true
				)));

		tools.add(buildTool("get_spid_waits",
				"Get the current per-session (SPID) wait breakdown live from the collector via CmSpidWait. "
				+ "Drill-down companion to get_wait_stats: once you know the dominant instance-level wait type, "
				+ "use this to see which sessions are contributing. "
				+ "Data comes from sys.dm_exec_session_wait_stats (SQL Server) or equivalent. "
				+ "Optionally filter to a specific SPID.",
				buildSchema(
					"serverName",  "string", "Name of the monitored server",                                  true,
					"spid",        "number", "Filter to a specific session ID (SPID). Omit for all sessions.", false,
					"type",        "string", "Sample type: abs, diff, or rate. Default: abs",                  false
				)));

		tools.add(buildTool("get_query_context",
				"One-call context lookup for a table: combines DSR historical data with live collector CM data.\n"
				+ "Returns three sections:\n"
				+ "  • tableSchema   — DSR schema sections: table size, index stats, missing/unused indexes\n"
				+ "  • sqlActivity   — DSR SQL sections: procedures/triggers/functions whose name references the table\n"
				+ "  • collectorData — live CM data (if collector online): all active CMs whose name contains 'table', "
				+ "'index', or 'object', filtered to rows mentioning the table. "
				+ "Covers CmTableSize, CmIndexUsage, CmObjectActivity (ASE), and equivalents across all DBMS types. "
				+ "hasData is intentionally ignored — expensive CMs run every 5–10 min.\n"
				+ "Use after get_wait_stats shows PAGEIOLATCH or after get_top_queries identifies a hot table.",
				buildSchema(
					"serverName",  "string", "Name of the monitored server",                                   true,
					"tableName",   "string", "Table name to look up (case-insensitive, partial match)",         true,
					"date",        "string", "DSR date: 'latest' or 'YYYY-MM-DD'. Default: 'latest'",          false
				)));

		tools.add(buildTool("get_tempdb_usage",
				"Get TempDB space usage by session. High TempDB usage is a common SQL Server performance bottleneck caused by sorts, spills, row versioning, and temp tables. "
				+ "Returns an aggregated summary by default. Set topN=0 for raw time-series.",
				buildSchema(
					"serverName",  "string", "Name of the monitored server",                                          true,
					"startTime",   "string", "Start of time range. Default: '-1h'",                                   false,
					"endTime",     "string", "End of time range. Default: 'now'",                                     false,
					"topN",        "number", "Max entries to return per graph, ranked by total. Default: 25. Set 0 for raw time-series.", false
				)));

		// -- Schema lookup --------------------------------------------------------
		tools.add(buildTool("get_table_schema",
				"Look up table size, index details, missing indexes, and unused indexes for a specific table. "
				+ "Works across all supported DBMS types (SQL Server, Sybase ASE, PostgreSQL, MySQL, Oracle, etc.). "
				+ "Combines two data sources:\n"
				+ "  1. DSR (Daily Summary Report): pre-computed snapshot with table size, missing/unused index sections. "
				+ "     Returned under 'dsrSchema' as structured rows from each matching section.\n"
				+ "  2. Collector live CMs (if collector is online): automatically discovers all active CMs whose name contains "
				+ "'table' or 'index' (DBMS-agnostic — e.g. CmTableSize/CmIndexUsage for SQL Server, equivalent CMs for ASE/PostgreSQL/MySQL). "
				+ "     Only active CMs are queried (hasData is intentionally ignored since expensive CMs may run every 5–10 min). Returned under 'collectorData'.\n"
				+ "If the collector is offline, only DSR data is returned. "
				+ "Use this tool whenever a query or alarm references a specific table to check its size, index health, and missing/unused index recommendations.",
				buildSchema(
					"serverName", "string", "Name of the monitored server",                                      true,
					"tableName",  "string", "Table name to look up (case-insensitive, partial match supported)",  true,
					"date",       "string", "DSR date: 'latest' or 'YYYY-MM-DD'. Default: 'latest'",             false
				)));

		// -- Daily Summary Reports --------------------------------------------
		tools.add(buildTool("get_object_ddl",
				"Retrieve DDL definitions, table metadata, and execution plans stored in MonDdlStorage on the collector.\n"
				+ "MonDdlStorage is automatically populated during monitoring whenever DbxTune encounters a referenced object "
				+ "(table, view, procedure, trigger, or SQL statement). It stores:\n"
				+ "  • objectText    — full CREATE statement (CREATE TABLE, CREATE VIEW, CREATE PROC, etc.)\n"
				+ "  • extraInfoText — table size, row counts, index list, space usage\n"
				+ "  • dependsText   — object dependencies (e.g. tables referenced by a view or proc)\n"
				+ "  • optdiagText   — optimizer diagnostics (Sybase ASE)\n"
				+ "Execution plans are stored with type='SS' and dbname='statement_cache'; objectName is the plan handle (e.g. '0x06000500...').\n"
				+ "This tool proxies live data from the collector — the collector must be online.\n"
				+ "Use get_table_schema for a DSR-enriched view; use this tool for raw DDL and live metadata.",
				buildSchema(
					"serverName",  "string", "Name of the monitored server (collector must be online)",                                       true,
					"objectName",  "string", "Object name filter (case-insensitive partial match). Omit to list all.",                         false,
					"dbname",      "string", "Database name filter (partial match). Use 'statement_cache' for execution plans.",               false,
					"type",        "string", "Comma-separated object type codes: U=Table, V=View, P=Procedure/Function, TR=Trigger, SS=Execution plan. Omit for all types.", false,
					"includeText", "string", "true/false — include DDL text and metadata blobs. Default: true. Set false for a lightweight index/listing call.", false,
					"limit",       "string", "Max items to return. Default: 200.",                                                             false,
					"ts",          "string", "Timestamp 'YYYY-MM-DD HH:mm:ss' selecting which recording to query. With H2 date-rolling each day is a separate database file. Default: current time (live recording).", false
				)));

		tools.add(buildTool("list_daily_reports",
				"List available Daily Summary Report (DSR) files for a server. "
				+ "DSRs are generated once per day and contain a rich pre-computed snapshot: "
				+ "top SQL, wait stats, blocking, I/O, configuration issues, alarms, database sizes, and more. "
				+ "Reports are kept for ~60 days. Use get_daily_report_section to read specific sections.",
				buildSchema(
					"serverName", "string", "Filter by server name (optional — omit to list reports for all servers)", false
				)));

		tools.add(buildTool("get_daily_report_section",
				"Extract and read a section from a Daily Summary Report (DSR) as plain text. "
				+ "Call with section=null (or omit section) to get the Table of Contents showing all available sections. "
				+ "Then call again with a specific section name to read that section's data.\n\n"
				+ "Key sections for performance analysis:\n"
				+ "  'Recording Information'              — session/recording metadata\n"
				+ "  'Active Alarms' / 'Alarm History'    — threshold breaches\n"
				+ "  'Wait Statistics'                    — top waits for the day\n"
				+ "  'CPU Usage'                          — CPU trend for the day\n"
				+ "  'Top ... CPU_TIME'                   — most expensive queries by CPU\n"
				+ "  'Top ... EST_WAIT_TIME'              — queries with most wait time\n"
				+ "  'Top ... LOGICAL_READS'              — highest I/O queries\n"
				+ "  'Top ... EXECUTION_COUNT'            — most frequently executed\n"
				+ "  'Device IO' / 'Slow Device IO'       — disk I/O performance\n"
				+ "  'Index Operational Statistics'       — index usage patterns\n"
				+ "  'Missing Indexes'                    — recommended indexes\n"
				+ "  'Unused Indexes'                     — indexes to consider dropping\n"
				+ "  'DBMS Configuration' / 'Config Issues' — settings and issues",
				buildSchema(
					"serverName", "string", "Name of the monitored server",                                  true,
					"date",       "string", "Report date: 'latest' or 'YYYY-MM-DD'. Default: 'latest'",      false,
					"section",    "string", "Section subject to extract (from Table of Contents). Omit to get the TOC.", false
				)));

		// -- Comparison -------------------------------------------------------
		tools.add(buildTool("compare_baseline",
				"Compare performance between two time windows to detect regressions or improvements.\n"
				+ "Fetches aggregated metrics for a 'baseline' window and a 'compare' window, then diffs them.\n"
				+ "Three aspects can be compared (controlled by the 'aspects' parameter):\n"
				+ "  • summary  — CmSummary metrics: CPU, I/O, memory, connections (avg/max per series, delta%)\n"
				+ "  • waits    — CmWaitStats: top wait types ranked by total time, with rank changes and delta%\n"
				+ "  • queries  — CmExecQueryStats: top queries ranked by cost, with 'new'/'gone' detection\n"
				+ "Each entry shows baselineTotal/compareTotal, deltaPct, and a trend indicator (↑ higher, ↓ lower, new, gone).\n"
				+ "A 'verdict' summary highlights the biggest movers for quick AI consumption.\n"
				+ "If baselineEnd is omitted, the baseline window is auto-sized to match the compare window duration.\n"
				+ "Typical usage: compare_baseline(baselineStart='-25h', compareStart='-1h') to compare this hour vs same hour yesterday.",
				buildSchema(
					"serverName",    "string", "Name of the monitored server",                                                                      true,
					"baselineStart", "string", "Start of baseline window: '-25h', '-7d', 'yyyy-MM-dd HH:mm:ss', etc.",                              true,
					"baselineEnd",   "string", "End of baseline window. Default: auto-sized to match compare window duration.",                      false,
					"compareStart",  "string", "Start of compare window. Default: '-1h'",                                                           false,
					"compareEnd",    "string", "End of compare window. Default: 'now'",                                                             false,
					"aspects",       "string", "Comma-separated: summary, waits, queries. Default: 'summary,waits,queries' (all three).",           false,
					"topN",          "number", "Max entries per graph in each window. Default: 25.",                                                 false
				)));

		// -- Query Store (SQL Server) -----------------------------------------
		tools.add(buildTool("get_query_store_databases",
				"List all SQL Server databases for which Query Store data was captured during the last nightly extraction.\n"
				+ "Returns health information per database: Query Store state, storage usage, number of captured queries/plans,\n"
				+ "date range of captured data, and count of automatic tuning recommendations.\n"
				+ "Use this first to see which databases have QS data and to check QS health before calling the other query-store tools.\n"
				+ "NOTE: QS data is extracted once per day at midnight during the database rollover. "
				+ "If today's data is missing, call get_query_store_extract to trigger a fresh extraction now.\n"
				+ "Data is read from the collector's H2 database (collector must be online).",
				buildSchema(
					"serverName", "string", "Name of the monitored SQL Server (collector must be online)",              true,
					"dbname",     "string", "Optional: filter to a specific database name (partial match)",             false,
					"ts",         "string", "Timestamp 'YYYY-MM-DD HH:mm:ss' to query a specific H2 recording. Default: current recording.", false
				)));

		tools.add(buildTool("get_query_store_top_queries",
				"Get the top N most expensive queries from SQL Server Query Store for a specific database.\n"
				+ "Queries are aggregated across all plans and all captured intervals, then ranked by the chosen metric.\n"
				+ "Each query entry includes:\n"
				+ "  • Aggregated stats: totalExecutions, avgDurationUs/avgCpuUs/avgLogicalReads (weighted averages)\n"
				+ "  • avgRowcount — average rows returned per execution (useful for detecting point-lookup N+1 patterns)\n"
				+ "  • hasMultiplePlans / planCount — signal for plan regression investigation\n"
				+ "  • objectContext — schema.procedure name if the query is inside a stored procedure\n"
				+ "  • querySqlTextPreview — first 500 characters of the SQL text\n"
				+ "  • firstSeen / lastSeen — time range of captured data\n"
				+ "Follow up with get_query_store_query_detail for a specific queryId to see plan breakdown and wait stats.\n"
				+ "Data is read from the collector's H2 database (collector must be online).",
				buildSchema(
					"serverName",    "string", "Name of the monitored SQL Server (collector must be online)",                       true,
					"dbname",        "string", "Database name to query (exact, as returned by get_query_store_databases)",          true,
					"rankBy",        "string", "Ranking metric: duration (default) | cpu | reads | executions | memory",            false,
					"topN",          "number", "Max queries to return. Default: 25",                                                 false,
					"minExecutions", "number", "Filter out queries with fewer total executions (noise reduction). Default: 2",       false,
					"ts",            "string", "Timestamp 'YYYY-MM-DD HH:mm:ss' for H2 date-rolling. Default: current recording.", false
				)));

		tools.add(buildTool("get_query_store_query_detail",
				"Get full Query Store detail for a specific query_id: all plans with aggregated stats, wait statistics, "
				+ "hourly execution timeline, plan regression analysis, and automatic tuning recommendations.\n"
				+ "Key outputs:\n"
				+ "  • queryText      — full SQL text\n"
				+ "  • plans          — per-plan aggregated stats (duration, CPU, reads, execution count, forced status)\n"
				+ "  • regressionSignal — detected/maxMinRatio: flags plan regression when one plan is ≥1.5× slower\n"
				+ "  • waitStats      — wait categories breakdown for this query (SQL Server 2017+ only)\n"
				+ "  • timeline       — last 24 hourly intervals showing execution count and avg duration trend\n"
				+ "  • recommendations — sys.dm_db_tuning_recommendations for the database (e.g. missing index suggestions)\n"
				+ "Set includePlan=true to include the full XML execution plan for each plan_id (can be very large).\n"
				+ "Data is read from the collector's H2 database (collector must be online).",
				buildSchema(
					"serverName",  "string", "Name of the monitored SQL Server (collector must be online)",                       true,
					"dbname",      "string", "Database name (as returned by get_query_store_databases)",                          true,
					"queryId",     "number", "The query_id to inspect (from get_query_store_top_queries)",                        true,
					"includePlan", "string", "true/false — include XML execution plan text for each plan. Default: false",         false,
					"ts",          "string", "Timestamp 'YYYY-MM-DD HH:mm:ss' for H2 date-rolling. Default: current recording.", false
				)));

		tools.add(buildTool("trigger_query_store_extract",
				"Trigger an on-demand SQL Server Query Store extraction on the collector.\n"
				+ "Normally extraction runs automatically at midnight during the database rollover. "
				+ "Use this tool when today's Query Store data is not yet available "
				+ "(e.g. it's 2pm and you want to analyse queries from today).\n"
				+ "The extraction runs asynchronously in the background — call this tool once, "
				+ "then poll with action=status until lastStatus='ok'. "
				+ "Extraction typically takes 1-3 minutes depending on database size.\n"
				+ "Supported extraction types: QUERY_STORE, DEADLOCK, JOB_SCHEDULER, BACKUP_HISTORY.\n"
				+ "The collector must be online and local.",
				buildSchema(
					"serverName",     "string", "Name of the monitored SQL Server (collector must be online)",                    true,
					"action",         "string", "trigger (default) — start extraction; or status — poll progress",                false,
					"extractionType", "string", "QUERY_STORE (default) | DEADLOCK | JOB_SCHEDULER | BACKUP_HISTORY",             false
				)));

		// -- Deadlocks (SQL Server) ------------------------------------------
		tools.add(buildTool("get_deadlocks",
				"Get SQL Server deadlock information captured by the collector.\n"
				+ "Returns the total deadlock count over the recording period, a timeline of when deadlocks occurred,\n"
				+ "and optionally the full sp_BlitzLock JSON report.\n"
				+ "Deadlock data is extracted once per day during the nightly database rollover (or on-demand via trigger_query_store_extract with type=DEADLOCK).\n"
				+ "  • action=summary  — count + hasReport flag + per-sample timeline from CmSummary\n"
				+ "  • action=report   — full sp_BlitzLock JSON (can be large; process/parse as needed)\n"
				+ "Data is read from the collector's H2 database (collector must be online).",
				buildSchema(
					"serverName",  "string", "Name of the monitored SQL Server (collector must be online)",                      true,
					"action",      "string", "summary (default) — count + timeline; report — full sp_BlitzLock JSON",            false,
					"ts",          "string", "Timestamp 'YYYY-MM-DD HH:mm:ss' selecting which recording to query. Default: current recording.", false
				)));

		// -- SQL Server Agent job history -----------------------------------
		tools.add(buildTool("get_job_history",
				"Get SQL Server Agent job history captured by the collector.\n"
				+ "Job history is extracted once per day during the nightly rollover (or on-demand via trigger_query_store_extract with type=JOB_SCHEDULER).\n"
				+ "Available actions:\n"
				+ "  • overview  — aggregated per-job stats: run count, success/failure counts, avg/max duration, last status\n"
				+ "  • jobs      — list of SQL Server Agent jobs with description, owner, schedule, and enabled flag\n"
				+ "  • history   — individual run records with start time, duration, and result message; optionally filtered by jobName or status\n"
				+ "  • errors    — failed runs only (shortcut for history + status=failed)\n"
				+ "  • outliers  — unusually long runs that exceeded the p95 duration for that job\n"
				+ "Start with overview to see the big picture, then use history or errors to drill into specific jobs.\n"
				+ "Data is read from the collector's H2 database (collector must be online).",
				buildSchema(
					"serverName",  "string", "Name of the monitored SQL Server (collector must be online)",                      true,
					"action",      "string", "overview (default) | jobs | history | errors | outliers",                          false,
					"jobName",     "string", "Filter history/errors by job name (partial match, case-insensitive)",               false,
					"status",      "string", "Filter history by run status: succeeded | failed | retry | cancelled",             false,
					"limit",       "number", "Max rows to return. Default: 200",                                                  false,
					"ts",          "string", "Timestamp 'YYYY-MM-DD HH:mm:ss' selecting which recording to query. Default: current recording.", false
				)));

		// -- Alarms -----------------------------------------------------------
		tools.add(buildTool("get_active_alarms",
				"Get currently active performance alarms. Alarms are triggered when metrics exceed configured thresholds.",
				buildSchema(
					"serverName",  "string", "Filter by server name (optional — omit to get alarms from all servers)", false
				)));

		tools.add(buildTool("get_alarm_history",
				"Get historical alarm events in a time range. Shows when alarms were raised and cancelled.",
				buildSchema(
					"serverName",  "string", "Filter by server name (optional)", false,
					"startTime",   "string", "Start of time range. Default: '-24h'",   false,
					"endTime",     "string", "End of time range. Default: 'now'",       false,
					"type",        "string", "Filter by alarm type/class (optional)",   false,
					"category",    "string", "Filter by alarm category (optional)",     false
				)));

		// -- Config (gated by exposeDbmsConfig) --------------------------------
		if (_exposeDbmsConfig)
		{
			tools.add(buildTool("get_dbms_config",
					"Get the full database configuration captured at collector startup: all sp_configure values (SQL Server), "
					+ "database options, trace flags, and any detected configuration issues. "
					+ "Data is proxied live from the collector — same source as the DBMS Config view in graph.html.",
					buildSchema(
						"serverName",  "string", "Name of the monitored server",  true
					)));
		}

		ObjectNode result = _om.createObjectNode();
		result.set("tools", tools);
		return result;
	}

	// =========================================================================
	// tools/call
	// =========================================================================

	private JsonNode handleToolsCall(JsonNode params)
	throws Exception
	{
		String   name = getRequiredString(params, "name");
		JsonNode args = params.has("arguments") ? params.get("arguments") : _om.createObjectNode();

		_logger.debug("MCP tools/call: name='{}', args={}", name, args);

		String resultText;

		switch (name)
		{
			case "list_servers":                 resultText = toolListServers(args);              break;
			case "list_metrics":                 resultText = toolListMetrics(args);              break;
			case "get_metric_data":              resultText = toolGetMetricData(args);            break;
			case "get_performance_summary":      resultText = toolGetPerfSummary(args);           break;
			case "get_wait_stats":               resultText = toolGetWaitStats(args);             break;
			case "get_blocking_info":            resultText = toolGetBlockingInfo(args);          break;
			case "get_top_queries":              resultText = toolGetTopQueries(args);            break;
			case "get_active_statements":        resultText = toolGetActiveStatements(args);      break;
			case "get_memory_analysis":          resultText = toolGetMemoryAnalysis(args);        break;
			case "get_io_analysis":              resultText = toolGetIoAnalysis(args);            break;
			case "get_tempdb_usage":             resultText = toolGetTempdbUsage(args);           break;
			case "list_cms":                     resultText = toolListCms(args);                  break;
			case "get_spid_waits":               resultText = toolGetSpidWaits(args);             break;
			case "get_query_context":            resultText = toolGetQueryContext(args);          break;
			case "get_table_schema":             resultText = toolGetTableSchema(args);           break;
			case "get_object_ddl":               resultText = toolGetObjectDdl(args);             break;
			case "list_daily_reports":           resultText = toolListDailyReports(args);         break;
			case "get_daily_report_section":     resultText = toolGetDailyReportSection(args);    break;
			case "compare_baseline":             resultText = toolCompareBaseline(args);          break;
			case "trigger_query_store_extract":  resultText = toolTriggerQueryStoreExtract(args); break;
			case "get_query_store_databases":    resultText = toolGetQueryStoreDatabases(args);   break;
			case "get_query_store_top_queries":  resultText = toolGetQueryStoreTopQueries(args);  break;
			case "get_query_store_query_detail": resultText = toolGetQueryStoreQueryDetail(args); break;
			case "get_deadlocks":                resultText = toolGetDeadlocks(args);             break;
			case "get_job_history":              resultText = toolGetJobHistory(args);            break;
			case "get_active_alarms":            resultText = toolGetActiveAlarms(args);          break;
			case "get_alarm_history":            resultText = toolGetAlarmHistory(args);          break;
			case "get_dbms_config":
				if (!_exposeDbmsConfig)
					throw new McpInvalidParamsException("Tool 'get_dbms_config' is disabled. Set DbxTuneCentral.mcp.exposeDbmsConfig=true to enable.");
				resultText = toolGetDbmsConfig(args);
				break;
			default:
				throw new McpInvalidParamsException("Unknown tool: '" + name + "'. Use tools/list to see available tools.");
		}

		return buildToolResult(resultText);
	}

	// =========================================================================
	// Tool implementations
	// =========================================================================

	// -- list_servers ---------------------------------------------------------

	private String toolListServers(JsonNode args) 
	throws Exception
	{
		requireReader();
		List<DbxCentralSessions> sessions = CentralPersistReader.getInstance().getSessions(true, -1);
		return _om.writeValueAsString(sessions);
	}

	// -- list_metrics ---------------------------------------------------------

	private String toolListMetrics(JsonNode args) 
	throws Exception
	{
		String serverName = getRequiredString(args, "serverName");
		requireServerSession(serverName);
		List<DbxGraphProperties> props = CentralPersistReader.getInstance().getGraphProperties(serverName, null);
		return _om.writeValueAsString(props);
	}

	// -- get_metric_data -------------------------------------------------------

	private String toolGetMetricData(JsonNode args) 
	throws Exception
	{
		String serverName  = getRequiredString(args, "serverName");
		String cmName      = getRequiredString(args, "cmName");
		String graphName   = getRequiredString(args, "graphName");
		String startTime   = getOptionalString(args, "startTime",   "-2h");
		String endTime     = getOptionalString(args, "endTime",     "now");
		String aggStr      = getOptionalString(args, "aggregation", "AUTO");

		requireServerSession(serverName);
		validateTimeRange(startTime, endTime);

		SampleType sampleType;
		try { sampleType = SampleType.fromString(aggStr); }
		catch (Exception e) { sampleType = SampleType.AUTO; }

		List<DbxGraphData> data = CentralPersistReader.getInstance()
				.getGraphData(serverName, cmName, graphName, startTime, endTime, sampleType, -1, null);
		return _om.writeValueAsString(data);
	}

	// -- get_performance_summary -----------------------------------------------

	private String toolGetPerfSummary(JsonNode args) 
	throws Exception
	{
		String serverName = getRequiredString(args, "serverName");
		String startTime  = getOptionalString(args, "startTime", "-1h");
		String endTime    = getOptionalString(args, "endTime",   "now");

		requireServerSession(serverName);
		validateTimeRange(startTime, endTime);

		CentralPersistReader reader = CentralPersistReader.getInstance();

		// Look up which CmSummary graphs exist for this server (names are DBMS-specific)
		List<DbxGraphProperties> props = reader.getGraphProperties(serverName, null);
		ArrayNode combined = _om.createArrayNode();
		for (DbxGraphProperties p : props)
		{
			if ("CmSummary".equals(p.getCmName()) && p.isVisibleAtStartup())
				appendGraphData(combined, reader, serverName, "CmSummary", p.getGraphName(), startTime, endTime);
		}
		return _om.writeValueAsString(combined);
	}

	// -- get_wait_stats --------------------------------------------------------

	private String toolGetWaitStats(JsonNode args)
	throws Exception
	{
		String serverName = getRequiredString(args, "serverName");
		String startTime  = getOptionalString(args, "startTime", "-1h");
		String endTime    = getOptionalString(args, "endTime",   "now");
		int    topN       = getOptionalInt   (args, "topN",      25);

		requireServerSession(serverName);
		validateTimeRange(startTime, endTime);

		CentralPersistReader reader = CentralPersistReader.getInstance();
		List<DbxGraphProperties> props = reader.getGraphProperties(serverName, null);

		ArrayNode combined = _om.createArrayNode();
		if (topN > 0)
		{
			appendAllGraphsForCmAggregated(combined, reader, serverName, "CmWaitStats",      props, startTime, endTime, topN);
			appendAllGraphsForCmAggregated(combined, reader, serverName, "CmWaitStatsPerDb", props, startTime, endTime, topN);
		}
		else
		{
			appendAllGraphsForCm(combined, reader, serverName, "CmWaitStats",      props, startTime, endTime);
			appendAllGraphsForCm(combined, reader, serverName, "CmWaitStatsPerDb", props, startTime, endTime);
		}

		return _om.writeValueAsString(combined);
	}

	// -- get_blocking_info -----------------------------------------------------

	private String toolGetBlockingInfo(JsonNode args) 
	throws Exception
	{
		String serverName = getRequiredString(args, "serverName");
		String startTime  = getOptionalString(args, "startTime", "-1h");
		String endTime    = getOptionalString(args, "endTime",   "now");

		requireServerSession(serverName);
		validateTimeRange(startTime, endTime);

		CentralPersistReader reader = CentralPersistReader.getInstance();
		List<DbxGraphProperties> props = reader.getGraphProperties(serverName, null);

		ArrayNode combined = _om.createArrayNode();
		appendAllGraphsForCm(combined, reader, serverName, "CmBlocking", props, startTime, endTime);
		appendAllGraphsForCm(combined, reader, serverName, "CmDeadlock", props, startTime, endTime);

		return _om.writeValueAsString(combined);
	}

	// -- get_top_queries -------------------------------------------------------

	private String toolGetTopQueries(JsonNode args)
	throws Exception
	{
		String serverName = getRequiredString(args, "serverName");
		String startTime  = getOptionalString(args, "startTime", "-1h");
		String endTime    = getOptionalString(args, "endTime",   "now");
		String rankBy     = getOptionalString(args, "rankBy",    "elapsed");
		int    topN       = getOptionalInt   (args, "topN",      25);

		requireServerSession(serverName);
		validateTimeRange(startTime, endTime);

		CentralPersistReader reader = CentralPersistReader.getInstance();
		List<DbxGraphProperties> props = reader.getGraphProperties(serverName, null);

		ArrayNode combined = _om.createArrayNode();
		if (topN > 0)
		{
			appendAllGraphsForCmAggregated(combined, reader, serverName, "CmExecQueryStats",     props, startTime, endTime, topN);
			appendAllGraphsForCmAggregated(combined, reader, serverName, "CmExecQueryStatPerDb", props, startTime, endTime, topN);
		}
		else
		{
			appendAllGraphsForCm(combined, reader, serverName, "CmExecQueryStats",     props, startTime, endTime);
			appendAllGraphsForCm(combined, reader, serverName, "CmExecQueryStatPerDb", props, startTime, endTime);
		}

		ObjectNode meta = _om.createObjectNode();
		meta.put("requestedRankBy", rankBy);
		meta.put("hint", "Entries are pre-summed across the time range. The 'series' field is the query identifier. 'total' is the sum, 'avg' per sample, 'max' peak. Rank by '" + rankBy + "' using the graph whose name matches that metric.");

		ObjectNode result = _om.createObjectNode();
		result.set("meta", meta);
		result.set("data", combined);
		return _om.writeValueAsString(result);
	}

	// -- get_active_statements -------------------------------------------------

	/**
	 * Column names that contain execution plan data (can be large XML or text).
	 * Excluded from output when {@code includePlan=false}.
	 */
	private static final Set<String> PLAN_COLUMNS = new java.util.HashSet<>(Arrays.asList(
		"query_plan",       // SQL Server: estimated XML plan from dm_exec_query_plan
		"LiveQueryPlan",    // SQL Server: actual plan captured at snapshot time (dm_exec_query_plan_stats)
		"optdiag"           // Sybase ASE: text execution plan
	));

	/**
	 * Candidate column names (in priority order) that hold the SQL text of the statement.
	 * Used to extract referenced table names via SqlParserUtils.
	 */
	private static final List<String> SQL_TEXT_COLUMN_CANDIDATES = Arrays.asList(
		"lastKnownSql",   // SQL Server / ASE: SQL text captured by CmActiveStatements
		"sql_text",       // PostgreSQL / MySQL style
		"SqlText",
		"query",
		"statement"
	);

	private String toolGetActiveStatements(JsonNode args)
	throws Exception
	{
		String  serverName  = getRequiredString(args, "serverName");
		String  startTime   = getOptionalString(args, "startTime",   null); // null = latest only
		String  endTime     = getOptionalString(args, "endTime",     "now");
		boolean includePlan = !"false".equalsIgnoreCase(getOptionalString(args, "includePlan", "true"));

		requireServerSession(serverName);
		if (startTime != null) validateTimeRange(startTime, endTime);

		CentralPersistReader reader = CentralPersistReader.getInstance();

		// CmActiveStatements is stored as JSON in DbxCmLastSampleJson (live) or
		// DbxCmHistorySampleJson (historical, up to ~1 month).
		Map<String, Map<String, String>> map;
		if (startTime == null || "now".equalsIgnoreCase(startTime))
		{
			map = reader.getLastSampleForCm(serverName, "CmActiveStatements");
		}
		else
		{
			Timestamp startTs = resolveTimestamp(startTime);
			Timestamp endTs   = "now".equalsIgnoreCase(endTime)
					? new Timestamp(System.currentTimeMillis()) : resolveTimestamp(endTime);
			// addSampleTimeToJson=true so sampleTime is embedded in each JSON blob
			map = reader.getHistorySampleForCm(serverName, "CmActiveStatements", null, startTs, endTs, true, true);
		}

		if (map.isEmpty())
		{
			ObjectNode note = _om.createObjectNode();
			note.put("note", "No CmActiveStatements data found. "
				+ "The collector may not support active statement capture for this DBMS type, "
				+ "or no statements were active during this period.");
			return _om.writeValueAsString(note);
		}

		// Build result: one entry per server
		ArrayNode result = _om.createArrayNode();
		for (Map.Entry<String, Map<String, String>> srvEntry : map.entrySet())
		{
			String srv     = srvEntry.getKey();
			String appName = "";
			try
			{
				DbxCentralSessions srvInfo = reader.getLastSession(srv);
				if (srvInfo != null) appName = srvInfo.getProductString();
			}
			catch (Exception ignored) {}

			ObjectNode srvNode = _om.createObjectNode();
			srvNode.put("srvName",  srv);
			srvNode.put("appName",  appName);

			// Each entry in the inner map is one CM-snapshot (cmName -> jsonString).
			// For a time range, getHistorySampleForCm returns the LAST matching sample per cmName.
			ArrayNode snapshots = _om.createArrayNode();
			for (Map.Entry<String, String> cmEntry : srvEntry.getValue().entrySet())
			{
				try
				{
					JsonNode parsed = _om.readTree(cmEntry.getValue());
					ObjectNode snap = _om.createObjectNode();
					snap.put("cmName", cmEntry.getKey());

					// sampleTime is injected into the JSON by getHistorySampleForCm (addSampleTimeToJson=true)
					// It lives inside the counter sections (absCounters.sampleTime etc.)
					String sampleTime = extractSampleTime(parsed);
					if (sampleTime != null)
						snap.put("sampleTime", sampleTime);

					// Convert columns/rows → named objects, add referencedTables enrichment
					ArrayNode statements = enrichActiveStatements(parsed, includePlan);
					snap.put("statementCount", statements.size());
					snap.set("statements", statements);

					snapshots.add(snap);
				}
				catch (Exception e)
				{
					_logger.debug("MCP get_active_statements: failed to process JSON for {}/{}: {}",
							srv, cmEntry.getKey(), e.getMessage());
				}
			}
			srvNode.set("snapshots", snapshots);
			result.add(srvNode);
		}
		return _om.writeValueAsString(result);
	}

	/**
	 * Convert a CmActiveStatements JSON blob from columns/rows format into an array of
	 * named-field objects, and enrich each statement with {@code referencedTables} extracted
	 * from its SQL text.
	 * <p>
	 * The JSON blob may wrap data in {@code absCounters} (typical CM structure) or expose
	 * {@code columns}/{@code rows} directly at the root level.
	 */
	private ArrayNode enrichActiveStatements(JsonNode cmJson, boolean includePlan)
	{
		ArrayNode result = _om.createArrayNode();

		// Locate the columns/rows payload — prefer absCounters, fall back to root
		JsonNode payload = cmJson;
		if (cmJson.has("absCounters") && cmJson.get("absCounters").has("columns"))
			payload = cmJson.get("absCounters");

		if (!payload.has("columns") || !payload.has("rows"))
			return result;

		JsonNode columns = payload.get("columns");
		JsonNode rows    = payload.get("rows");

		// Build column-name → index map (preserving order)
		Map<String, Integer> colIdx = new LinkedHashMap<>();
		for (int c = 0; c < columns.size(); c++)
			colIdx.put(columns.get(c).asText(), c);

		// Find SQL text column for table extraction
		String sqlTextCol = null;
		for (String candidate : SQL_TEXT_COLUMN_CANDIDATES)
		{
			if (colIdx.containsKey(candidate)) { sqlTextCol = candidate; break; }
		}

		for (JsonNode row : rows)
		{
			ObjectNode obj = _om.createObjectNode();

			for (Map.Entry<String, Integer> col : colIdx.entrySet())
			{
				String colName = col.getKey();
				int    idx     = col.getValue();
				if (!includePlan && PLAN_COLUMNS.contains(colName))
					continue;
				if (idx < row.size())
					obj.set(colName, row.get(idx));
			}

			// Enrich: extract referenced table names from SQL text
			if (sqlTextCol != null && obj.has(sqlTextCol))
			{
				String sqlText = obj.get(sqlTextCol).asText("");
				if (!sqlText.isEmpty() && !"null".equalsIgnoreCase(sqlText))
				{
					Set<String> tables = SqlParserUtils.getTables(sqlText);
					if (!tables.isEmpty())
					{
						ArrayNode tablesNode = _om.createArrayNode();
						tables.forEach(tablesNode::add);
						obj.set("referencedTables", tablesNode);
					}
				}
			}

			result.add(obj);
		}
		return result;
	}

	/**
	 * Extract the {@code sampleTime} string that {@code getHistorySampleForCm} injects
	 * into counter sections when {@code addSampleTimeToJson=true}.
	 * Checks {@code absCounters.sampleTime}, then {@code diffCounters.sampleTime},
	 * then a top-level {@code sampleTime} field.
	 */
	private static String extractSampleTime(JsonNode cmJson)
	{
		for (String section : Arrays.asList("absCounters", "diffCounters", "rateCounters"))
		{
			JsonNode sec = cmJson.get(section);
			if (sec != null && sec.has("sampleTime"))
				return sec.get("sampleTime").asText();
		}
		if (cmJson.has("sampleTime"))
			return cmJson.get("sampleTime").asText();
		return null;
	}

	/**
	 * Convert a relative or absolute time string to a SQL Timestamp.
	 * <p>
	 * Relative formats handled here: {@code now}, {@code -2h}, {@code -30m}, {@code -7d}.<br>
	 * Absolute formats delegated to {@link TimeUtils#parseToTimestampX(String)} which handles
	 * ISO-8601 and all common {@code yyyy-MM-dd} variants.
	 */
	private Timestamp resolveTimestamp(String timeStr)
	{
		if (timeStr == null || "now".equalsIgnoreCase(timeStr))
			return new Timestamp(System.currentTimeMillis());

		// Relative: optional leading '-', then digits + unit (m/h/d)
		Matcher m = Pattern.compile("^-?(\\d+)([hHmMdD])$").matcher(timeStr.trim());
		if (m.matches())
		{
			long amount = Long.parseLong(m.group(1));
			String unit = m.group(2).toLowerCase();
			long millis;
			if      ("h".equals(unit)) millis = amount * 3_600_000L;
			else if ("d".equals(unit)) millis = amount * 86_400_000L;
			else                       millis = amount * 60_000L;   // "m" and any fallback
			return new Timestamp(System.currentTimeMillis() - millis);
		}

		// Absolute: delegate to TimeUtils which handles ISO-8601 and yyyy-MM-dd variants
		try
		{
			return TimeUtils.parseToTimestampX(timeStr);
		}
		catch (ParseException ignored) {}

		throw new McpInvalidParamsException("Cannot parse time value: '" + timeStr + "'. Use relative (-2h, -30m, -7d) or ISO-8601 (yyyy-MM-dd HH:mm:ss).");
	}

	// -- get_memory_analysis ---------------------------------------------------

	private String toolGetMemoryAnalysis(JsonNode args)
	throws Exception
	{
		String serverName = getRequiredString(args, "serverName");
		String startTime  = getOptionalString(args, "startTime", "-1h");
		String endTime    = getOptionalString(args, "endTime",   "now");
		int    topN       = getOptionalInt   (args, "topN",      25);

		requireServerSession(serverName);
		validateTimeRange(startTime, endTime);

		CentralPersistReader reader = CentralPersistReader.getInstance();
		List<DbxGraphProperties> props = reader.getGraphProperties(serverName, null);

		ArrayNode combined = _om.createArrayNode();
		if (topN > 0)
		{
			appendAllGraphsForCmAggregated(combined, reader, serverName, "CmMemoryClerks",    props, startTime, endTime, topN);
			appendAllGraphsForCmAggregated(combined, reader, serverName, "CmMemoryGrants",    props, startTime, endTime, topN);
			appendAllGraphsForCmAggregated(combined, reader, serverName, "CmMemoryGrantsSum", props, startTime, endTime, topN);
		}
		else
		{
			appendAllGraphsForCm(combined, reader, serverName, "CmMemoryClerks",    props, startTime, endTime);
			appendAllGraphsForCm(combined, reader, serverName, "CmMemoryGrants",    props, startTime, endTime);
			appendAllGraphsForCm(combined, reader, serverName, "CmMemoryGrantsSum", props, startTime, endTime);
		}

		return _om.writeValueAsString(combined);
	}

	// -- get_io_analysis -------------------------------------------------------

	private String toolGetIoAnalysis(JsonNode args)
	throws Exception
	{
		String serverName = getRequiredString(args, "serverName");
		String startTime  = getOptionalString(args, "startTime", "-1h");
		String endTime    = getOptionalString(args, "endTime",   "now");
		int    topN       = getOptionalInt   (args, "topN",      25);

		requireServerSession(serverName);
		validateTimeRange(startTime, endTime);

		CentralPersistReader reader = CentralPersistReader.getInstance();
		List<DbxGraphProperties> props = reader.getGraphProperties(serverName, null);

		ArrayNode combined = _om.createArrayNode();
		if (topN > 0)
		{
			appendAllGraphsForCmAggregated(combined, reader, serverName, "CmDeviceIo", props, startTime, endTime, topN);
			appendAllGraphsForCmAggregated(combined, reader, serverName, "CmDbIo",     props, startTime, endTime, topN);
		}
		else
		{
			appendAllGraphsForCm(combined, reader, serverName, "CmDeviceIo", props, startTime, endTime);
			appendAllGraphsForCm(combined, reader, serverName, "CmDbIo",     props, startTime, endTime);
		}

		return _om.writeValueAsString(combined);
	}

	// -- get_tempdb_usage ------------------------------------------------------

	private String toolGetTempdbUsage(JsonNode args)
	throws Exception
	{
		String serverName = getRequiredString(args, "serverName");
		String startTime  = getOptionalString(args, "startTime", "-1h");
		String endTime    = getOptionalString(args, "endTime",   "now");
		int    topN       = getOptionalInt   (args, "topN",      25);

		requireServerSession(serverName);
		validateTimeRange(startTime, endTime);

		CentralPersistReader reader = CentralPersistReader.getInstance();
		List<DbxGraphProperties> props = reader.getGraphProperties(serverName, null);

		ArrayNode combined = _om.createArrayNode();
		if (topN > 0)
		{
			appendAllGraphsForCmAggregated(combined, reader, serverName, "CmTempdbUsage",     props, startTime, endTime, topN);
			appendAllGraphsForCmAggregated(combined, reader, serverName, "CmTempdbSpidUsage", props, startTime, endTime, topN);
		}
		else
		{
			appendAllGraphsForCm(combined, reader, serverName, "CmTempdbUsage",     props, startTime, endTime);
			appendAllGraphsForCm(combined, reader, serverName, "CmTempdbSpidUsage", props, startTime, endTime);
		}

		return _om.writeValueAsString(combined);
	}

	// -- list_cms --------------------------------------------------------------

	private String toolListCms(JsonNode args)
	throws Exception
	{
		String serverName = getRequiredString(args, "serverName");
		requireServerSession(serverName);

		String[] collectorInfo = resolveCollectorInfo(serverName);
		String baseUrl = collectorInfo[0];
		String auth    = collectorInfo[1];

		// CmListServlet requires a time param — pass current time
		String timeNow = TimeUtils.toStringYmdHms(Instant.now());
		HtmlQueryString qs0 = new HtmlQueryString(baseUrl + "/api/mgt/cm/list");
		qs0.add("time", timeNow);
		String url = qs0.toString();

		_logger.debug("MCP list_cms: proxying to '{}'", url);

		HttpRequest.Builder reqBuilder = HttpRequest.newBuilder().uri(URI.create(url)).GET();
		if (auth != null && !auth.isEmpty())
			reqBuilder.header("Authorization", auth);

		try
		{
			HttpResponse<String> resp = _httpClient.send(reqBuilder.build(), HttpResponse.BodyHandlers.ofString());
			if (resp.statusCode() != 200)
			{
				ObjectNode err = _om.createObjectNode();
				err.put("error",      "collector-http-error");
				err.put("httpStatus", resp.statusCode());
				err.put("body",       resp.body());
				return _om.writeValueAsString(err);
			}

			// Re-shape to a flat list sorted by group for easier AI consumption:
			// [ { groupName, cmName, displayName, description, hasData, absRows, diffRows, rateRows }, ... ]
			JsonNode root = _om.readTree(resp.body());
			ArrayNode flat = _om.createArrayNode();

			if (root.has("groups"))
			{
				for (JsonNode group : root.get("groups"))
				{
					String groupName = group.has("groupName") ? group.get("groupName").asText() : "";
					if (group.has("cms"))
					{
						for (JsonNode cm : group.get("cms"))
						{
							ObjectNode entry = _om.createObjectNode();
							entry.put("groupName",   groupName);
							entry.put("cmName",      cm.has("cmName")      ? cm.get("cmName").asText()      : "");
							entry.put("displayName", cm.has("displayName") ? cm.get("displayName").asText() : "");
							entry.put("description", cm.has("description") ? cm.get("description").asText() : "");
							entry.put("hasData",     cm.has("hasData")     ? cm.get("hasData").asBoolean()  : false);
							entry.put("absRows",     cm.has("absRows")     ? cm.get("absRows").asInt()      : 0);
							entry.put("diffRows",    cm.has("diffRows")    ? cm.get("diffRows").asInt()     : 0);
							entry.put("rateRows",    cm.has("rateRows")    ? cm.get("rateRows").asInt()     : 0);
							if (cm.has("isActive"))  entry.put("isActive", cm.get("isActive").asBoolean());
							flat.add(entry);
						}
					}
				}
			}

			ObjectNode result = _om.createObjectNode();
			result.put("serverName",  serverName);
			result.put("sampleTime",  root.has("resolvedTime") ? root.get("resolvedTime").asText() : timeNow);
			result.put("cmCount",     flat.size());
			result.put("hint",        "Use cmName with get_metric_data to fetch time-series, or /api/mgt/cm/data for the live snapshot.");
			result.set("cms",         flat);
			return _om.writeValueAsString(result);
		}
		catch (ConnectException ex)
		{
			ObjectNode err = _om.createObjectNode();
			err.put("error",   "collector-offline");
			err.put("message", "Collector at " + baseUrl + " is not reachable: " + ex.getMessage());
			return _om.writeValueAsString(err);
		}
	}

	// -- get_spid_waits --------------------------------------------------------

	private String toolGetSpidWaits(JsonNode args)
	throws Exception
	{
		String serverName = getRequiredString(args, "serverName");
		String type       = getOptionalString(args, "type", "abs");
		String spidStr    = getOptionalString(args, "spid", null);

		requireServerSession(serverName);

		String[] collectorInfo = resolveCollectorInfo(serverName);
		String baseUrl = collectorInfo[0];
		String auth    = collectorInfo[1];

		HtmlQueryString qs1 = new HtmlQueryString(baseUrl + "/api/mgt/cm/data");
		qs1.add("cm",   "CmSpidWait");
		qs1.add("type", type);
		String url = qs1.toString();

		_logger.debug("MCP get_spid_waits: proxying to '{}'", url);

		HttpRequest.Builder reqBuilder = HttpRequest.newBuilder().uri(URI.create(url)).GET();
		if (auth != null && !auth.isEmpty())
			reqBuilder.header("Authorization", auth);

		try
		{
			HttpResponse<String> resp = _httpClient.send(reqBuilder.build(), HttpResponse.BodyHandlers.ofString());
			if (resp.statusCode() != 200)
			{
				ObjectNode err = _om.createObjectNode();
				err.put("error",      "collector-http-error");
				err.put("httpStatus", resp.statusCode());
				err.put("body",       resp.body());
				return _om.writeValueAsString(err);
			}

			// If spid filter requested, post-filter the JSON rows
			if (spidStr != null && !spidStr.isEmpty())
			{
				JsonNode root = _om.readTree(resp.body());
				// CmDataServlet returns { columns:[...], rows:[[...],[...]] } or similar
				// Filter rows where the spid column matches
				if (root.isObject() && root.has("rows") && root.has("columns"))
				{
					JsonNode columns = root.get("columns");
					JsonNode rows    = root.get("rows");

					// Find spid column index (case-insensitive)
					int spidCol = -1;
					for (int c = 0; c < columns.size(); c++)
					{
						if ("spid".equalsIgnoreCase(columns.get(c).asText()))
						{
							spidCol = c;
							break;
						}
					}

					if (spidCol >= 0)
					{
						ArrayNode filtered = _om.createArrayNode();
						for (JsonNode row : rows)
						{
							if (row.isArray() && row.size() > spidCol
								&& spidStr.equals(row.get(spidCol).asText()))
								filtered.add(row);
						}
						ObjectNode result = _om.createObjectNode();
						result.set("columns", columns);
						result.set("rows",    filtered);
						result.put("filteredBySpid", spidStr);
						result.put("matchCount", filtered.size());
						return _om.writeValueAsString(result);
					}
				}
				// spid column not found or unexpected format — return raw with note
				ObjectNode wrapper = _om.createObjectNode();
				wrapper.set("data", root);
				wrapper.put("note", "spid filter requested but 'spid' column not found in response — returning all rows");
				return _om.writeValueAsString(wrapper);
			}

			return resp.body(); // no filter — pass through directly
		}
		catch (ConnectException ex)
		{
			ObjectNode err = _om.createObjectNode();
			err.put("error",   "collector-offline");
			err.put("message", "Collector at " + baseUrl + " is not reachable: " + ex.getMessage());
			return _om.writeValueAsString(err);
		}
	}

	// -- get_query_context -----------------------------------------------------

	/**
	 * DSR section name keywords that contain SQL activity (procedures, functions, triggers)
	 * whose ObjectName / name columns may reference the target table.
	 */
	private static final List<String> SQL_ACTIVITY_SECTION_KEYWORDS = Arrays.asList(
		"top procedure",          // CmExecProcedureStats — ObjectName = stored procedure name
		"top function",           // CmExecFunctionStats  — ObjectName = function name
		"top trigger",            // CmExecTriggerStats   — ObjectName = trigger name (often named after its table)
		"top cursor",             // CmExecCursors
		"query store",            // Query Store Reports
		"top table/index",        // ASE: CmObjectActivity
		"top table size"          // ASE: Top TABLE Size sections (for cross-check)
	);

	private String toolGetQueryContext(JsonNode args)
	throws Exception
	{
		String serverName = getRequiredString(args, "serverName");
		String tableName  = getRequiredString(args, "tableName");
		String date       = getOptionalString(args, "date",      getOptionalString(args, "reportDate", "latest"));

		requireServerSession(serverName);

		String reportsDir = DbxTuneCentral.getAppReportsDir();
		if (reportsDir == null || reportsDir.isEmpty())
			throw new McpInvalidParamsException("DbxCentral reports directory is not configured.");

		File reportFile = resolveDsrFile(new File(reportsDir), serverName, date);
		if (reportFile == null)
			throw new McpInvalidParamsException(
				"No Daily Summary Report found for server '" + serverName + "' with date='" + date + "'.");

		_logger.debug("MCP get_query_context: reading '{}', table='{}'", reportFile.getName(), tableName);

		String html = new String(Files.readAllBytes(reportFile.toPath()), StandardCharsets.UTF_8);

		// Parse all section boundaries once — shared by both schema and SQL scans
		Matcher m = DSR_SECTION_SEPARATOR.matcher(html);
		List<int[]>  boundaries = new ArrayList<>();
		List<String> subjects   = new ArrayList<>();
		while (m.find())
		{
			boundaries.add(new int[]{ m.start(), m.end() });
			subjects.add(m.group(1).trim());
		}

		String tableNameLower = tableName.toLowerCase();
		ArrayNode schemaMatches = _om.createArrayNode();
		ArrayNode sqlMatches    = _om.createArrayNode();

		for (int i = 0; i < subjects.size(); i++)
		{
			String subjectLower = subjects.get(i).toLowerCase();
			boolean isSchema = SCHEMA_SECTION_KEYWORDS.stream().anyMatch(subjectLower::contains);
			boolean isSql    = SQL_ACTIVITY_SECTION_KEYWORDS.stream().anyMatch(subjectLower::contains);
			if (!isSchema && !isSql) continue;

			int contentStart = boundaries.get(i)[1];
			int contentEnd   = (i + 1 < boundaries.size())
				? boundaries.get(i + 1)[0]
				: html.lastIndexOf("</body>");
			if (contentEnd <= contentStart) contentEnd = html.length();

			String sectionHtml = html.substring(contentStart, contentEnd);

			Matcher tblM = Pattern.compile("(?si)<table[^>]*>(.*?)</table>").matcher(sectionHtml);
			while (tblM.find())
			{
				List<List<String>> rows = parseHtmlTableRows(tblM.group(1));
				if (rows.size() < 2) continue;

				List<String> headers = rows.get(0);

				// Prefer matching on known name columns; fall back to any cell
				int nameColIdx = -1;
				for (int c = 0; c < headers.size(); c++)
				{
					String h = headers.get(c).toLowerCase();
					if (h.equals("tablename") || h.equals("objectname") || h.equals("table_name") || h.equals("name"))
					{
						nameColIdx = c;
						break;
					}
				}

				List<List<String>> matchedRows = new ArrayList<>();
				for (int r = 1; r < rows.size(); r++)
				{
					List<String> row = rows.get(r);
					boolean matched = (nameColIdx >= 0 && nameColIdx < row.size())
						? row.get(nameColIdx).toLowerCase().contains(tableNameLower)
						: row.stream().anyMatch(cell -> cell.toLowerCase().contains(tableNameLower));
					if (matched) matchedRows.add(row);
				}
				if (matchedRows.isEmpty()) continue;

				ArrayNode rowsNode = _om.createArrayNode();
				for (List<String> row : matchedRows)
				{
					ObjectNode rowNode = _om.createObjectNode();
					for (int c = 0; c < headers.size(); c++)
						rowNode.put(headers.get(c), c < row.size() ? row.get(c) : "");
					rowsNode.add(rowNode);
				}

				ObjectNode section = _om.createObjectNode();
				section.put("section",    subjects.get(i));
				section.put("matchCount", matchedRows.size());
				section.set("rows",       rowsNode);

				if (isSchema) schemaMatches.add(section);
				else          sqlMatches.add(section);
			}
		}

		ObjectNode result = _om.createObjectNode();
		result.put("tableName",  tableName);
		result.put("server",     serverName);
		result.put("source",     reportFile.getName());
		result.set("tableSchema",    schemaMatches);
		result.set("sqlActivity",    sqlMatches);

		if (schemaMatches.isEmpty() && sqlMatches.isEmpty())
			result.put("note", "Table '" + tableName + "' not found in any schema or SQL activity section. "
				+ "It may not be large enough to appear in size rankings, or no procedures/triggers reference it by name.");

		// ---- Phase 2: live CM data from the collector ----------------------------
		// Discover active CMs whose name contains "table", "index", or "object"
		// (covers CmTableSize, CmIndexUsage, CmObjectActivity for ASE, etc. across all DBMS types).
		// Gracefully degrade: if collector is offline, return DSR data only.
		ArrayNode collectorData = _om.createArrayNode();
		try
		{
			String[] collectorInfo = resolveCollectorInfo(serverName);
			String   baseUrl       = collectorInfo[0];
			String   auth          = collectorInfo[1];

			String timeNow = TimeUtils.toStringYmdHms(Instant.now());

			// Step 1: discover relevant CMs — isActive=true, name contains table/index/object
			// hasData is intentionally NOT checked — expensive CMs run every 5–10 min ("postpone")
			List<String> cmNames = new ArrayList<>();
			{
				HtmlQueryString lqs = new HtmlQueryString(baseUrl + "/api/mgt/cm/list");
				lqs.add("time", timeNow);
				String listUrl = lqs.toString();
				HttpRequest.Builder listReqBuilder = HttpRequest.newBuilder()
						.uri(URI.create(listUrl)).GET();
				if (auth != null && !auth.isEmpty())
					listReqBuilder.header("Authorization", auth);

				HttpResponse<String> listResp = _httpClient.send(
						listReqBuilder.build(), HttpResponse.BodyHandlers.ofString());

				if (listResp.statusCode() == 200)
				{
					JsonNode listRoot = _om.readTree(listResp.body());
					if (listRoot.has("groups"))
					{
						for (JsonNode group : listRoot.get("groups"))
						{
							if (!group.has("cms")) continue;
							for (JsonNode cm : group.get("cms"))
							{
								String  cmName   = cm.has("cmName")   ? cm.get("cmName").asText()      : "";
								boolean isActive = cm.has("isActive") ? cm.get("isActive").asBoolean() : false;
								if (!isActive) continue;
								String cmNameLc = cmName.toLowerCase();
								if (cmNameLc.contains("table") || cmNameLc.contains("index") || cmNameLc.contains("object"))
									cmNames.add(cmName);
							}
						}
					}
				}
				_logger.debug("MCP get_query_context Phase2: discovered {} table/index/object CMs on collector", cmNames.size());
			}

			// Step 2: query each CM and filter rows by table name
			for (String cmName : cmNames)
			{
				HtmlQueryString dqs = new HtmlQueryString(baseUrl + "/api/mgt/cm/data");
				dqs.add("cm",   cmName);
				dqs.add("time", timeNow);
				dqs.add("type", "abs");
				String url = dqs.toString();

				HttpRequest.Builder reqBuilder = HttpRequest.newBuilder()
						.uri(URI.create(url)).GET();
				if (auth != null && !auth.isEmpty())
					reqBuilder.header("Authorization", auth);

				HttpResponse<String> httpResp = _httpClient.send(
						reqBuilder.build(), HttpResponse.BodyHandlers.ofString());

				if (httpResp.statusCode() != 200)
				{
					_logger.debug("MCP get_query_context Phase2: {} returned HTTP {}", cmName, httpResp.statusCode());
					continue;
				}

				JsonNode cmRoot = _om.readTree(httpResp.body());
				if (!cmRoot.has("columns") || !cmRoot.has("rows"))
					continue;

				JsonNode columns = cmRoot.get("columns");
				JsonNode rows    = cmRoot.get("rows");

				// Find the table-name column index (common names across CMs and DBMS types)
				int tableNameColIdx = -1;
				for (int c = 0; c < columns.size(); c++)
				{
					String colName = columns.get(c).asText().toLowerCase();
					if (colName.equals("tablename") || colName.equals("objectname")
							|| colName.equals("table_name") || colName.equals("name"))
					{
						tableNameColIdx = c;
						break;
					}
				}

				// Filter rows referencing our table
				ArrayNode matchedRows = _om.createArrayNode();
				for (int r = 0; r < rows.size(); r++)
				{
					JsonNode row = rows.get(r);
					boolean matched = false;
					if (tableNameColIdx >= 0 && tableNameColIdx < row.size())
					{
						matched = row.get(tableNameColIdx).asText().toLowerCase().contains(tableNameLower);
					}
					else
					{
						for (int c = 0; c < row.size(); c++)
						{
							if (row.get(c).asText().toLowerCase().contains(tableNameLower))
							{
								matched = true;
								break;
							}
						}
					}
					if (matched) matchedRows.add(row);
				}

				if (matchedRows.isEmpty()) continue;

				// Build structured rows with column names as keys
				ArrayNode structuredRows = _om.createArrayNode();
				for (int r = 0; r < matchedRows.size(); r++)
				{
					JsonNode   rawRow    = matchedRows.get(r);
					ObjectNode structRow = _om.createObjectNode();
					for (int c = 0; c < columns.size(); c++)
						structRow.put(columns.get(c).asText(), c < rawRow.size() ? rawRow.get(c).asText() : "");
					structuredRows.add(structRow);
				}

				ObjectNode cmResult = _om.createObjectNode();
				cmResult.put("cmName",     cmName);
				cmResult.put("matchCount", matchedRows.size());
				cmResult.set("rows",       structuredRows);
				collectorData.add(cmResult);
			}
		}
		catch (McpInvalidParamsException ex)
		{
			_logger.debug("MCP get_query_context Phase2: collector not available for '{}': {}", serverName, ex.getMessage());
		}
		catch (java.net.ConnectException ex)
		{
			_logger.debug("MCP get_query_context Phase2: collector offline for '{}': {}", serverName, ex.getMessage());
		}
		catch (Exception ex)
		{
			_logger.warn("MCP get_query_context Phase2: unexpected error querying collector CMs: {}", ex.getMessage());
		}

		if (!collectorData.isEmpty())
			result.set("collectorData", collectorData);

		return _om.writeValueAsString(result);
	}

	// -- get_table_schema ------------------------------------------------------

	/**
	 * DSR section name keywords that are likely to contain per-table schema data.
	 * Matched case-insensitively against section subject lines.
	 */
	private static final List<String> SCHEMA_SECTION_KEYWORDS = Arrays.asList(
		"table size",    // CmTableSize and CmIndexPhysical table-size sections (SQL Server + ASE)
		"missing index", // CmIndexMissing
		"unused index",  // CmIndexUnused
		"page usage",    // CmIndexPhysical avg page space used
		"table/index"    // ASE: Top TABLE/INDEX Activity, Blocking Lock Wait Statistics, etc.
	);

	private String toolGetTableSchema(JsonNode args)
	throws Exception
	{
		String serverName = getRequiredString(args, "serverName");
		String tableName  = getRequiredString(args, "tableName");
		String date       = getOptionalString(args, "date", "latest");

		requireServerSession(serverName);

		String reportsDir = DbxTuneCentral.getAppReportsDir();
		if (reportsDir == null || reportsDir.isEmpty())
			throw new McpInvalidParamsException("DbxCentral reports directory is not configured.");

		File reportFile = resolveDsrFile(new File(reportsDir), serverName, date);
		if (reportFile == null)
			throw new McpInvalidParamsException(
				"No Daily Summary Report found for server '" + serverName + "' with date='" + date + "'.");

		_logger.debug("MCP get_table_schema: reading '{}', table='{}'", reportFile.getName(), tableName);

		String html = new String(Files.readAllBytes(reportFile.toPath()), StandardCharsets.UTF_8);

		// Parse all section boundaries
		Matcher m = DSR_SECTION_SEPARATOR.matcher(html);
		List<int[]>  boundaries = new ArrayList<>();
		List<String> subjects   = new ArrayList<>();
		while (m.find())
		{
			boundaries.add(new int[]{ m.start(), m.end() });
			subjects.add(m.group(1).trim());
		}

		String tableNameLower = tableName.toLowerCase();
		ArrayNode result = _om.createArrayNode();

		for (int i = 0; i < subjects.size(); i++)
		{
			String subjectLower = subjects.get(i).toLowerCase();
			boolean isSchemaSection = SCHEMA_SECTION_KEYWORDS.stream()
				.anyMatch(subjectLower::contains);
			if (!isSchemaSection) continue;

			int contentStart = boundaries.get(i)[1];
			int contentEnd   = (i + 1 < boundaries.size())
				? boundaries.get(i + 1)[0]
				: html.lastIndexOf("</body>");
			if (contentEnd <= contentStart) contentEnd = html.length();

			String sectionHtml = html.substring(contentStart, contentEnd);

			// Parse all HTML tables in this section
			Matcher tblM = Pattern.compile("(?si)<table[^>]*>(.*?)</table>").matcher(sectionHtml);
			while (tblM.find())
			{
				List<List<String>> rows = parseHtmlTableRows(tblM.group(1));
				if (rows.size() < 2) continue; // need at least header + one data row

				List<String> headers = rows.get(0);

				// Find the column index that holds the table name
				// Common column names: TableName, ObjectName, objectName, table_name
				int tableNameColIdx = -1;
				for (int c = 0; c < headers.size(); c++)
				{
					String h = headers.get(c).toLowerCase();
					if (h.equals("tablename") || h.equals("objectname") || h.equals("table_name"))
					{
						tableNameColIdx = c;
						break;
					}
				}

				// Collect matching data rows
				List<List<String>> matchedRows = new ArrayList<>();
				for (int r = 1; r < rows.size(); r++)
				{
					List<String> row = rows.get(r);
					if (tableNameColIdx >= 0 && tableNameColIdx < row.size())
					{
						// Precise match on the TableName column
						if (row.get(tableNameColIdx).toLowerCase().contains(tableNameLower))
							matchedRows.add(row);
					}
					else
					{
						// Fallback: any cell contains the table name
						if (row.stream().anyMatch(cell -> cell.toLowerCase().contains(tableNameLower)))
							matchedRows.add(row);
					}
				}

				if (matchedRows.isEmpty()) continue;

				// Build structured result: [ { col: val, col: val, ... }, ... ]
				ArrayNode rowsNode = _om.createArrayNode();
				for (List<String> row : matchedRows)
				{
					ObjectNode rowNode = _om.createObjectNode();
					for (int c = 0; c < headers.size(); c++)
						rowNode.put(headers.get(c), c < row.size() ? row.get(c) : "");
					rowsNode.add(rowNode);
				}

				ObjectNode section = _om.createObjectNode();
				section.put("section",    subjects.get(i));
				section.put("source",     reportFile.getName());
				section.put("matchCount", matchedRows.size());
				section.set("rows",       rowsNode);
				result.add(section);
			}
		}

		if (result.isEmpty())
		{
			ObjectNode notFound = _om.createObjectNode();
			notFound.put("note",    "Table '" + tableName + "' not found in any schema section of the DSR.");
			notFound.put("server",  serverName);
			notFound.put("source",  reportFile.getName());
			notFound.put("hint",    "Only the top-N tables appear in each DSR section. "
				+ "The table may not be large enough to appear in size rankings, "
				+ "or may have no missing/unused index recommendations.");
			return _om.writeValueAsString(notFound);
		}

		// ---- Phase 2: enrich with live CM data from the collector ---------------
		// Dynamically discover table/index CMs from the collector (DBMS-agnostic:
		// works for SQL Server, ASE, PostgreSQL, MySQL, Oracle, etc.).
		// We gracefully degrade: if the collector is offline, we return DSR data only.
		ArrayNode collectorData = _om.createArrayNode();
		try
		{
			String[] collectorInfo = resolveCollectorInfo(serverName);
			String   baseUrl       = collectorInfo[0];
			String   auth          = collectorInfo[1];

			String timeNow = TimeUtils.toStringYmdHms(Instant.now());

			// Step 1: discover which table/index CMs exist on this collector
			// Filter: cmName contains "table" or "index" (case-insensitive), and hasData=true
			List<String> cmNames = new ArrayList<>();
			{
				HtmlQueryString lqs = new HtmlQueryString(baseUrl + "/api/mgt/cm/list");
				lqs.add("time", timeNow);
				String listUrl = lqs.toString();
				HttpRequest.Builder listReqBuilder = HttpRequest.newBuilder()
					.uri(URI.create(listUrl)).GET();
				if (auth != null && !auth.isEmpty())
					listReqBuilder.header("Authorization", auth);

				HttpResponse<String> listResp = _httpClient.send(
					listReqBuilder.build(), HttpResponse.BodyHandlers.ofString());

				if (listResp.statusCode() == 200)
				{
					JsonNode listRoot = _om.readTree(listResp.body());
					if (listRoot.has("groups"))
					{
						for (JsonNode group : listRoot.get("groups"))
						{
							if (!group.has("cms")) continue;
							for (JsonNode cm : group.get("cms"))
							{
								String  cmName   = cm.has("cmName")   ? cm.get("cmName").asText()    : "";
								boolean isActive = cm.has("isActive") ? cm.get("isActive").asBoolean() : false;
								if (!isActive) continue;
								String cmNameLc = cmName.toLowerCase();
								if (cmNameLc.contains("table") || cmNameLc.contains("index"))
									cmNames.add(cmName);
							}
						}
					}
				}
				_logger.debug("MCP get_table_schema Phase2: discovered {} table/index CMs on collector", cmNames.size());
			}

			for (String cmName : cmNames)
			{
				HtmlQueryString dqs = new HtmlQueryString(baseUrl + "/api/mgt/cm/data");
				dqs.add("cm",   cmName);
				dqs.add("time", timeNow);
				dqs.add("type", "abs");
				String url = dqs.toString();

				HttpRequest.Builder reqBuilder = HttpRequest.newBuilder()
					.uri(URI.create(url))
					.GET();
				if (auth != null && !auth.isEmpty())
					reqBuilder.header("Authorization", auth);

				HttpResponse<String> httpResp = _httpClient.send(
					reqBuilder.build(), HttpResponse.BodyHandlers.ofString());

				if (httpResp.statusCode() != 200)
				{
					_logger.debug("MCP get_table_schema Phase2: {} returned HTTP {}", cmName, httpResp.statusCode());
					continue;
				}

				JsonNode cmRoot = _om.readTree(httpResp.body());
				if (!cmRoot.has("columns") || !cmRoot.has("rows"))
					continue;

				JsonNode columns = cmRoot.get("columns");
				JsonNode rows    = cmRoot.get("rows");

				// Find the table-name column index (common names across CMs)
				int tableNameColIdx = -1;
				for (int c = 0; c < columns.size(); c++)
				{
					String colName = columns.get(c).asText().toLowerCase();
					if (colName.equals("tablename") || colName.equals("objectname")
					        || colName.equals("table_name") || colName.equals("name"))
					{
						tableNameColIdx = c;
						break;
					}
				}

				// Filter rows that reference our table
				ArrayNode matchedRows = _om.createArrayNode();
				for (int r = 0; r < rows.size(); r++)
				{
					JsonNode row = rows.get(r);
					boolean matched = false;
					if (tableNameColIdx >= 0 && tableNameColIdx < row.size())
					{
						matched = row.get(tableNameColIdx).asText().toLowerCase()
							.contains(tableNameLower);
					}
					else
					{
						// Fallback: scan all cells
						for (int c = 0; c < row.size(); c++)
						{
							if (row.get(c).asText().toLowerCase().contains(tableNameLower))
							{
								matched = true;
								break;
							}
						}
					}
					if (matched)
						matchedRows.add(row);
				}

				if (matchedRows.isEmpty())
					continue;

				// Build structured result with column names as keys
				ArrayNode structuredRows = _om.createArrayNode();
				for (int r = 0; r < matchedRows.size(); r++)
				{
					JsonNode   rawRow    = matchedRows.get(r);
					ObjectNode structRow = _om.createObjectNode();
					for (int c = 0; c < columns.size(); c++)
						structRow.put(columns.get(c).asText(), c < rawRow.size() ? rawRow.get(c).asText() : "");
					structuredRows.add(structRow);
				}

				ObjectNode cmResult = _om.createObjectNode();
				cmResult.put("cmName",     cmName);
				cmResult.put("matchCount", matchedRows.size());
				cmResult.set("rows",       structuredRows);
				collectorData.add(cmResult);
			}
		}
		catch (McpInvalidParamsException ex)
		{
			// Collector not reachable or not configured — DSR data is sufficient
			_logger.debug("MCP get_table_schema Phase2: collector not available for '{}': {}", serverName, ex.getMessage());
		}
		catch (java.net.ConnectException ex)
		{
			_logger.debug("MCP get_table_schema Phase2: collector offline for '{}': {}", serverName, ex.getMessage());
		}
		catch (Exception ex)
		{
			_logger.warn("MCP get_table_schema Phase2: unexpected error querying collector CMs: {}", ex.getMessage());
		}

		// Wrap DSR data + collector data in a single top-level object
		ObjectNode finalResult = _om.createObjectNode();
		finalResult.put("tableName",  tableName);
		finalResult.put("server",     serverName);
		finalResult.put("source",     "DSR: " + reportFile.getName());
		finalResult.set("dsrSchema",  result);
		if (!collectorData.isEmpty())
			finalResult.set("collectorData", collectorData);

		return _om.writeValueAsString(finalResult);
	}

	/**
	 * Parse all rows from an HTML table fragment (content between {@code <table>} tags).
	 * Returns a list of rows; each row is a list of cell text values.
	 * The first row is the header (th cells), subsequent rows are data (td cells).
	 */
	private static List<List<String>> parseHtmlTableRows(String tableInnerHtml)
	{
		List<List<String>> rows = new ArrayList<>();
		Matcher trM = Pattern.compile("(?si)<tr[^>]*>(.*?)</tr>").matcher(tableInnerHtml);
		while (trM.find())
		{
			List<String> cells = new ArrayList<>();
			Matcher cellM = Pattern.compile("(?si)<t[hd][^>]*>(.*?)</t[hd]>").matcher(trM.group(1));
			while (cellM.find())
			{
				String cellHtml = cellM.group(1);
				// Strip inner tags, then decode common entities
				String text = cellHtml
					.replaceAll("<[^>]+>", "")
					.replace("&amp;",  "&")
					.replace("&lt;",   "<")
					.replace("&gt;",   ">")
					.replace("&nbsp;", " ")
					.replace("&quot;", "\"")
					.replace("&#39;",  "'")
					.replace("&apos;", "'")
					.replace("&emsp;", " ")
					.replaceAll("\\s+", " ")
					.trim();
				cells.add(text);
			}
			if (!cells.isEmpty())
				rows.add(cells);
		}
		return rows;
	}

	// -- get_object_ddl --------------------------------------------------------

	private String toolGetObjectDdl(JsonNode args)
	throws Exception
	{
		String serverName  = getRequiredString(args, "serverName");
		String objectName  = getOptionalString(args, "objectName",  "");
		String dbname      = getOptionalString(args, "dbname",      "");
		String type        = getOptionalString(args, "type",        "");
		String includeText = getOptionalString(args, "includeText", "true");
		String limit       = getOptionalString(args, "limit",       "200");
		String ts          = getOptionalString(args, "ts",          "");

		requireServerSession(serverName);

		String[] collectorInfo = resolveCollectorInfo(serverName);
		String   baseUrl       = collectorInfo[0];
		String   auth          = collectorInfo[1];

		HtmlQueryString qs = new HtmlQueryString(baseUrl + "/api/mgt/ddl-storage");
		qs.add("includeText", includeText);
		qs.add("limit",       limit);
		qs.addIfNotEmpty("objectName", objectName);
		qs.addIfNotEmpty("dbname",     dbname);
		qs.addIfNotEmpty("type",       type);
		qs.addIfNotEmpty("ts",         ts);
		String url = qs.toString();
		_logger.debug("MCP get_object_ddl: proxying to '{}'", url);

		HttpRequest.Builder reqBuilder = HttpRequest.newBuilder().uri(URI.create(url)).GET();
		if (auth != null && !auth.isEmpty())
			reqBuilder.header("Authorization", auth);

		try
		{
			HttpResponse<String> resp = _httpClient.send(reqBuilder.build(), HttpResponse.BodyHandlers.ofString());
			if (resp.statusCode() == 200)
				return resp.body(); // already JSON — pass through directly

			ObjectNode err = _om.createObjectNode();
			err.put("error",      "collector-http-error");
			err.put("httpStatus", resp.statusCode());
			err.put("body",       resp.body());
			return _om.writeValueAsString(err);
		}
		catch (ConnectException ex)
		{
			ObjectNode err = _om.createObjectNode();
			err.put("error",   "collector-offline");
			err.put("message", "Collector at " + baseUrl + " is not reachable: " + ex.getMessage());
			return _om.writeValueAsString(err);
		}
	}

	// -- list_daily_reports ----------------------------------------------------

	private String toolListDailyReports(JsonNode args)
	throws Exception
	{
		String serverNameFilter = getOptionalString(args, "serverName", null);

		String reportsDir = DbxTuneCentral.getAppReportsDir();
		if (reportsDir == null || reportsDir.isEmpty())
			throw new McpInvalidParamsException("DbxCentral reports directory is not configured.");

		File dir = new File(reportsDir);
		if (!dir.exists() || !dir.isDirectory())
		{
			ObjectNode result = _om.createObjectNode();
			result.put("reportsDir",  reportsDir);
			result.put("note",        "Reports directory does not exist or contains no reports yet.");
			result.set("reports",     _om.createArrayNode());
			return _om.writeValueAsString(result);
		}

		// filename pattern: {serverName}.{yyyy-MM-dd_HHmm}[.-NTR].html
		File[] files = dir.listFiles(f -> f.getName().endsWith(".html"));
		if (files == null) 
			files = new File[0];

		// Sort newest first
		Arrays.sort(files, (a, b) -> Long.compare(b.lastModified(), a.lastModified()));

		ArrayNode reports = _om.createArrayNode();
		SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd_HHmm");

		for (File f : files)
		{
			String name = f.getName();

			// Extract server name: everything before the first '.'
			int firstDot = name.indexOf('.');
			if (firstDot < 0) 
				continue;
			String srvName = name.substring(0, firstDot);

			// Apply server name filter
			if (serverNameFilter != null && !serverNameFilter.isEmpty())
			{
				if (!srvName.equalsIgnoreCase(serverNameFilter) && !srvName.contains(serverNameFilter))
					continue;
			}

			boolean nothingToReport = name.contains(".-NTR.");

			// Extract date string: between first '.' and either '.-NTR.' or '.html'
			String remainder = name.substring(firstDot + 1); // e.g. "2026-04-10_0003.html" or "2026-04-10_0003.-NTR.html"
			remainder = remainder.replace(".-NTR.html", "").replace(".html", "");
			String dateStr = remainder; // e.g. "2026-04-10_0003"

			Date reportDate = null;
			try { reportDate = sdf.parse(dateStr); } catch (ParseException ignored) {}

			ObjectNode entry = _om.createObjectNode();
			entry.put("filename",        name);
			entry.put("serverName",      srvName);
			entry.put("date",            dateStr);
			entry.put("nothingToReport", nothingToReport);
			entry.put("sizeKB",          f.length() / 1024);
			if (reportDate != null)
				entry.put("reportDateIso", new SimpleDateFormat("yyyy-MM-dd'T'HH:mm").format(reportDate));
			reports.add(entry);
		}

		ObjectNode result = _om.createObjectNode();
		result.put("reportsDir", reportsDir);
		result.put("count",      reports.size());
		result.set("reports",    reports);
		return _om.writeValueAsString(result);
	}

	// -- get_daily_report_section ----------------------------------------------

	private String toolGetDailyReportSection(JsonNode args)
	throws Exception
	{
		String serverName = getRequiredString(args, "serverName");
		// Accept aliases: date/reportDate, section/sectionName
		String date    = getOptionalString(args, "date",        getOptionalString(args, "reportDate",   "latest"));
		String section = getOptionalString(args, "section",     getOptionalString(args, "sectionName",  null));

		String reportsDir = DbxTuneCentral.getAppReportsDir();
		if (reportsDir == null || reportsDir.isEmpty())
			throw new McpInvalidParamsException("DbxCentral reports directory is not configured.");

		File reportFile = resolveDsrFile(new File(reportsDir), serverName, date);
		if (reportFile == null)
		{
			throw new McpInvalidParamsException(
				"No Daily Summary Report found for server '" + serverName
				+ "' with date='" + date + "'. Use list_daily_reports to see available reports.");
		}

		_logger.debug("MCP get_daily_report_section: reading '{}', section='{}'", reportFile.getName(), section);

		// Read the HTML (may be large — stream line by line for section extraction)
		String html = new String(Files.readAllBytes(reportFile.toPath()), StandardCharsets.UTF_8);

		// No section requested → return Table of Contents
		if (section == null || section.isEmpty())
			return extractDsrToc(html, reportFile.getName());

		// Section requested → extract and return as plain text
		return extractDsrSection(html, section, reportFile.getName());
	}

	/**
	 * Resolve the DSR file for a given server name and date specifier.
	 * @param dir        the reports directory
	 * @param serverName server name (prefix of filename)
	 * @param date       "latest", "YYYY-MM-DD", or full date-time "YYYY-MM-DD_HHmm"
	 */
	private File resolveDsrFile(File dir, String serverName, String date)
	{
		if (!dir.exists()) 
			return null;

		// Safe server name prefix (same logic as FileUtils.toSafeFileName — strip path chars)
		String safeServer = serverName.replaceAll("[/\\\\:*?\"<>|]", "_");

		File[] candidates = dir.listFiles(f -> f.getName().endsWith(".html") && f.getName().startsWith(safeServer + "."));

		if (candidates == null || candidates.length == 0) 
			return null;

		if ("latest".equalsIgnoreCase(date) || date == null || date.isEmpty())
		{
			// Pick the most recently modified file
			return Arrays.stream(candidates)
				.max((a, b) -> Long.compare(a.lastModified(), b.lastModified()))
				.orElse(null);
		}

		// Filter by date prefix (YYYY-MM-DD or YYYY-MM-DD_HHmm)
		String datePrefix = safeServer + "." + date;
		return Arrays.stream(candidates)
			.filter(f -> f.getName().startsWith(datePrefix))
			.max((a, b) -> Long.compare(a.lastModified(), b.lastModified()))
			.orElse(null);
	}

	/**
	 * Extract the Table of Contents from a DSR HTML file.
	 * Returns a structured plain-text summary with section names and their sizes.
	 */
	private String extractDsrToc(String html, String filename)
	throws Exception
	{
		// Parse section subjects from the HTML comment pattern:
		//   <!-- ====...==== -->
		//   <!-- {subject}   -->
		//   <!-- ====...==== -->
		Matcher m = DSR_SECTION_SEPARATOR.matcher(html);

		List<String> sections = new ArrayList<>();
		while (m.find())
			sections.add(m.group(1).trim());

		// Also extract the report title from <h5 class='card-header'..><b>...</b>
		String title = "";
		Matcher titleM = Pattern.compile("<b>(Daily Summary Report for Servername: [^<]+)</b>").matcher(html);
		if (titleM.find()) title = titleM.group(1).trim();

		ObjectNode toc = _om.createObjectNode();
		toc.put("filename",    filename);
		toc.put("title",       title);
		toc.put("sectionCount", sections.size());
		toc.put("fileSizeKB",  html.length() / 1024);
		toc.put("hint",        "Call get_daily_report_section with one of the section names below to read that section's data.");

		ArrayNode sectionList = _om.createArrayNode();
		for (String s : sections)
			sectionList.add(s);
		toc.set("sections", sectionList);

		return _om.writeValueAsString(toc);
	}

	/**
	 * Extract a named section from the DSR HTML and return it as clean plain text.
	 * The section name is matched case-insensitively and as a substring (partial match works).
	 */
	private String extractDsrSection(String html, String sectionQuery, String filename)
	{
		// Find all section boundaries
		Matcher m = DSR_SECTION_SEPARATOR.matcher(html);

		List<int[]> boundaries = new ArrayList<>();  // [matchStart, groupStart, subject]
		List<String> subjects  = new ArrayList<>();

		while (m.find())
		{
			boundaries.add(new int[]{ m.start(), m.end() });
			subjects.add(m.group(1).trim());
		}

		if (boundaries.isEmpty())
			return "No sections found in report. The HTML structure may be in an older format.";

		// Find the best matching section (case-insensitive substring match)
		int matchIdx = -1;
		String queryLower = sectionQuery.toLowerCase();
		for (int i = 0; i < subjects.size(); i++)
		{
			if (subjects.get(i).toLowerCase().contains(queryLower))
			{
				matchIdx = i;
				break;
			}
		}

		if (matchIdx < 0)
		{
			StringBuilder sb = new StringBuilder("Section '").append(sectionQuery).append("' not found.\n\nAvailable sections:\n");
			for (String s : subjects)
				sb.append("  - ").append(s).append("\n");
			return sb.toString();
		}

		// Extract HTML from end of matching separator to start of next separator (or end of body)
		int contentStart = boundaries.get(matchIdx)[1];
		int contentEnd   = (matchIdx + 1 < boundaries.size())
			? boundaries.get(matchIdx + 1)[0]
			: html.lastIndexOf("</body>");
		if (contentEnd <= contentStart) contentEnd = html.length();

		String sectionHtml = html.substring(contentStart, contentEnd);

		// Convert HTML to readable plain text
		String plainText = htmlToPlainText(sectionHtml);

		// Enforce size limit
		String header = "=== " + subjects.get(matchIdx) + " ===\n"
			+ "Source: " + filename + "\n\n";

		String content = header + plainText;
		if (content.length() > DSR_SECTION_MAX_CHARS)
		{
			content = content.substring(0, DSR_SECTION_MAX_CHARS)
				+ "\n\n[... TRUNCATED at " + DSR_SECTION_MAX_CHARS + " chars. "
				+ "Section had " + plainText.length() + " chars total. "
				+ "Consider requesting a more specific sub-section.]";
		}
		return content;
	}

	/**
	 * Convert HTML to readable plain text suitable for an AI assistant.
	 * Preserves table structure by converting cells/rows to delimited lines.
	 */
	private static String htmlToPlainText(String html)
	{
		// 1. Remove script and style blocks entirely
		String text = html
			.replaceAll("(?si)<script[^>]*>.*?</script>", "")
			.replaceAll("(?si)<style[^>]*>.*?</style>",   "")
			.replaceAll("(?si)<svg[^>]*>.*?</svg>",        "")
			// 2. Remove image tags (charts are PNG images — not useful as text)
			.replaceAll("(?si)<img[^>]*/?>",               "[chart]")
			// 3. Table structure → readable delimited text
			.replaceAll("(?si)</th>",                      " |")
			.replaceAll("(?si)</td>",                      " |")
			.replaceAll("(?si)<tr[^>]*>",                  "")
			.replaceAll("(?si)</tr>",                      "\n")
			// 4. Block elements → newlines
			.replaceAll("(?si)<br\\s*/?>",                 "\n")
			.replaceAll("(?si)</p>",                       "\n")
			.replaceAll("(?si)</li>",                      "\n")
			.replaceAll("(?si)</div>",                     "\n")
			.replaceAll("(?si)</h[1-6]>",                  "\n")
			// 5. Strip all remaining tags
			.replaceAll("<[^>]+>",                         "")
			// 6. Decode common HTML entities
			.replace("&amp;",  "&")
			.replace("&lt;",   "<")
			.replace("&gt;",   ">")
			.replace("&nbsp;", " ")
			.replace("&quot;", "\"")
			.replace("&#39;",  "'")
			.replace("&apos;", "'")
			// 7. Clean up whitespace: collapse multiple spaces/tabs on a line
			.replaceAll("[ \t]{2,}", " ")
			// 8. Collapse 3+ consecutive blank lines → double blank line
			.replaceAll("(\r?\n[ \t]*){3,}", "\n\n");

		// Trim each line
		StringBuilder sb = new StringBuilder();
		for (String line : text.split("\n"))
		{
			String trimmed = line.stripTrailing();
			sb.append(trimmed).append("\n");
		}
		return sb.toString().trim();
	}

	// -- compare_baseline ------------------------------------------------------

	/**
	 * Compare performance between two time windows (baseline vs compare).
	 * Reuses the existing graph reader and aggregation engine — no new SQL.
	 */
	private String toolCompareBaseline(JsonNode args)
	throws Exception
	{
		String serverName    = getRequiredString(args, "serverName");
		String baselineStart = getRequiredString(args, "baselineStart");
		String baselineEnd   = getOptionalString(args, "baselineEnd",   null);
		String compareStart  = getOptionalString(args, "compareStart",  "-1h");
		String compareEnd    = getOptionalString(args, "compareEnd",    "now");
		String aspects       = getOptionalString(args, "aspects",       "summary,waits,queries");
		int    topN          = getOptionalInt   (args, "topN",          25);

		requireServerSession(serverName);

		// Resolve compare window timestamps
		Timestamp compStartTs = resolveTimestamp(compareStart);
		Timestamp compEndTs   = resolveTimestamp(compareEnd);

		// Resolve baseline window; auto-size if baselineEnd is omitted
		Timestamp baseStartTs = resolveTimestamp(baselineStart);
		Timestamp baseEndTs;
		if (baselineEnd != null && !baselineEnd.isEmpty())
		{
			baseEndTs = resolveTimestamp(baselineEnd);
		}
		else
		{
			long durationMs = compEndTs.getTime() - compStartTs.getTime();
			baseEndTs = new Timestamp(baseStartTs.getTime() + durationMs);
		}

		// Format as absolute strings for getGraphData()
		SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
		String baseStartStr = sdf.format(baseStartTs);
		String baseEndStr   = sdf.format(baseEndTs);
		String compStartStr = sdf.format(compStartTs);
		String compEndStr   = sdf.format(compEndTs);

		CentralPersistReader reader = CentralPersistReader.getInstance();
		List<DbxGraphProperties> props = reader.getGraphProperties(serverName, null);

		java.util.Set<String> aspectSet = new java.util.HashSet<>(
				Arrays.asList(aspects.toLowerCase().trim().split("\\s*,\\s*")));

		ObjectNode result = _om.createObjectNode();
		result.put("server", serverName);

		ObjectNode baseWindow = _om.createObjectNode();
		baseWindow.put("start",       baseStartStr);
		baseWindow.put("end",         baseEndStr);
		baseWindow.put("durationMin", (baseEndTs.getTime() - baseStartTs.getTime()) / 60000);
		result.set("baseline", baseWindow);

		ObjectNode compWindow = _om.createObjectNode();
		compWindow.put("start",       compStartStr);
		compWindow.put("end",         compEndStr);
		compWindow.put("durationMin", (compEndTs.getTime() - compStartTs.getTime()) / 60000);
		result.set("compare", compWindow);

		// -- Compare each requested aspect ----------------------------------------
		List<String> verdictParts = new ArrayList<>();

		if (aspectSet.contains("summary"))
		{
			ArrayNode summaryDiff = compareGraphsForCm(reader, serverName, "CmSummary", props,
					baseStartStr, baseEndStr, compStartStr, compEndStr, topN, verdictParts);
			if (summaryDiff.size() > 0)
				result.set("summary", summaryDiff);
		}

		if (aspectSet.contains("waits"))
		{
			ArrayNode waitsDiff = compareGraphsForCm(reader, serverName, "CmWaitStats", props,
					baseStartStr, baseEndStr, compStartStr, compEndStr, topN, verdictParts);
			// Also include per-DB waits if available
			ArrayNode waitsPerDbDiff = compareGraphsForCm(reader, serverName, "CmWaitStatsPerDb", props,
					baseStartStr, baseEndStr, compStartStr, compEndStr, topN, null);
			for (JsonNode n : waitsPerDbDiff) waitsDiff.add(n);
			if (waitsDiff.size() > 0)
				result.set("waitStats", waitsDiff);
		}

		if (aspectSet.contains("queries"))
		{
			ArrayNode queriesDiff = compareGraphsForCm(reader, serverName, "CmExecQueryStats", props,
					baseStartStr, baseEndStr, compStartStr, compEndStr, topN, verdictParts);
			ArrayNode queriesPerDbDiff = compareGraphsForCm(reader, serverName, "CmExecQueryStatPerDb", props,
					baseStartStr, baseEndStr, compStartStr, compEndStr, topN, null);
			for (JsonNode n : queriesPerDbDiff) queriesDiff.add(n);
			if (queriesDiff.size() > 0)
				result.set("topQueries", queriesDiff);
		}

		// Build verdict
		if (verdictParts.isEmpty())
			result.put("verdict", "No significant changes detected between the two windows.");
		else
			result.put("verdict", String.join(" ", verdictParts));

		return _om.writeValueAsString(result);
	}

	/**
	 * For a given CM, compare all its graphs between baseline and compare windows.
	 * Returns an ArrayNode of graph-level diffs, each containing diffed entries.
	 *
	 * @param verdictParts if non-null, append notable findings for the verdict summary
	 */
	private ArrayNode compareGraphsForCm(
			CentralPersistReader reader, String serverName, String cmName,
			List<DbxGraphProperties> props,
			String baseStart, String baseEnd, String compStart, String compEnd,
			int topN, List<String> verdictParts)
	{
		ArrayNode result = _om.createArrayNode();

		for (DbxGraphProperties p : props)
		{
			if (!cmName.equals(p.getCmName())) continue;
			String graphName = p.getGraphName();

			try
			{
				// Fetch + aggregate both windows using the existing engine
				List<DbxGraphData> baseRows = reader.getGraphData(
						serverName, cmName, graphName, baseStart, baseEnd, SampleType.AUTO, -1, null);
				List<DbxGraphData> compRows = reader.getGraphData(
						serverName, cmName, graphName, compStart, compEnd, SampleType.AUTO, -1, null);

				// Skip if both empty
				if ((baseRows == null || baseRows.isEmpty()) && (compRows == null || compRows.isEmpty()))
					continue;

				// Use topN=0 for aggregation to get ALL entries (we'll rank the diff ourselves)
				ObjectNode baseAgg = (baseRows != null && !baseRows.isEmpty())
						? aggregateGraphData(baseRows, 0) : null;
				ObjectNode compAgg = (compRows != null && !compRows.isEmpty())
						? aggregateGraphData(compRows, 0) : null;

				// Extract series → {total, avg, max, rank} maps
				Map<String, double[]> baseMap = extractSeriesMap(baseAgg);
				Map<String, double[]> compMap = extractSeriesMap(compAgg);

				if (baseMap.isEmpty() && compMap.isEmpty())
					continue;

				// Compute diff entries — union of both series sets
				java.util.Set<String> allSeries = new java.util.LinkedHashSet<>();
				allSeries.addAll(compMap.keySet());
				allSeries.addAll(baseMap.keySet());

				List<ObjectNode> diffEntries = new ArrayList<>();
				int newCount  = 0;
				int goneCount = 0;

				for (String series : allSeries)
				{
					double[] base = baseMap.get(series);
					double[] comp = compMap.get(series);

					ObjectNode entry = _om.createObjectNode();
					entry.put("series", series);

					if (base != null && comp != null)
					{
						// Present in both windows
						double deltaTotal = comp[0] - base[0];
						double deltaPct   = (base[0] != 0) ? (deltaTotal / base[0]) * 100.0
								: (comp[0] != 0 ? 999999.0 : 0.0);

						entry.put("baselineTotal", round2(base[0]));
						entry.put("baselineAvg",   round2(base[1]));
						entry.put("baselineMax",   round2(base[2]));
						entry.put("baselineRank",  (int) base[3]);
						entry.put("compareTotal",  round2(comp[0]));
						entry.put("compareAvg",    round2(comp[1]));
						entry.put("compareMax",    round2(comp[2]));
						entry.put("compareRank",   (int) comp[3]);
						entry.put("deltaTotal",    round2(deltaTotal));
						entry.put("deltaPct",      round2(deltaPct));
						entry.put("trend", deltaTotal > 0 ? "↑ higher"
								: deltaTotal < 0 ? "↓ lower" : "= same");
					}
					else if (comp != null)
					{
						// New in compare window
						entry.put("compareTotal", round2(comp[0]));
						entry.put("compareAvg",   round2(comp[1]));
						entry.put("compareMax",   round2(comp[2]));
						entry.put("compareRank",  (int) comp[3]);
						entry.put("trend", "new");
						newCount++;
					}
					else
					{
						// Gone from compare window
						entry.put("baselineTotal", round2(base[0]));
						entry.put("baselineAvg",   round2(base[1]));
						entry.put("baselineMax",   round2(base[2]));
						entry.put("baselineRank",  (int) base[3]);
						entry.put("trend", "gone");
						goneCount++;
					}

					diffEntries.add(entry);
				}

				// Sort by |deltaPct| descending — biggest movers first
				diffEntries.sort((a, b) -> {
					double absA = Math.abs(a.has("deltaPct") ? a.get("deltaPct").asDouble() : 0);
					double absB = Math.abs(b.has("deltaPct") ? b.get("deltaPct").asDouble() : 0);
					// "new" and "gone" go to the top after the big movers
					if (!a.has("deltaPct") && !b.has("deltaPct")) return 0;
					if (!a.has("deltaPct")) return 1;
					if (!b.has("deltaPct")) return -1;
					return Double.compare(absB, absA);
				});

				// Limit to topN entries
				if (topN > 0 && diffEntries.size() > topN)
					diffEntries = diffEntries.subList(0, topN);

				ArrayNode entriesNode = _om.createArrayNode();
				diffEntries.forEach(entriesNode::add);

				ObjectNode graphDiff = _om.createObjectNode();
				graphDiff.put("cmName",     cmName);
				graphDiff.put("graphName",  graphName);
				graphDiff.put("graphLabel", p.getGraphLabel() != null ? p.getGraphLabel() : "");
				graphDiff.put("baselineSamples", baseAgg != null && baseAgg.has("sampleCount") ? baseAgg.get("sampleCount").asInt() : 0);
				graphDiff.put("compareSamples",  compAgg != null && compAgg.has("sampleCount") ? compAgg.get("sampleCount").asInt() : 0);
				graphDiff.set("entries", entriesNode);
				result.add(graphDiff);

				// Contribute to verdict: report the single biggest mover per graph
				if (verdictParts != null && !diffEntries.isEmpty())
				{
					ObjectNode top = diffEntries.get(0);
					String label = p.getGraphLabel() != null ? p.getGraphLabel() : graphName;
					if (top.has("deltaPct"))
					{
						double pct = top.get("deltaPct").asDouble();
						if (Math.abs(pct) >= 10.0) // only report meaningful changes
						{
							String dir = pct > 0 ? "↑" : "↓";
							verdictParts.add(String.format("%s: %s %s%.0f%% (%s).",
									label, top.get("series").asText(), dir, Math.abs(pct), top.get("trend").asText()));
						}
					}
					if (newCount > 0)
						verdictParts.add(String.format("%s: %d new entries.", label, newCount));
				}
			}
			catch (Exception e)
			{
				_logger.debug("MCP compare_baseline: error comparing {}/{} on {}: {}",
						cmName, graphName, serverName, e.getMessage());
			}
		}

		return result;
	}

	/**
	 * Extract a series → {total, avg, max, rank} map from an aggregated graph result.
	 * Returns an empty map if the aggregation is null or has no entries.
	 */
	private Map<String, double[]> extractSeriesMap(ObjectNode agg)
	{
		Map<String, double[]> map = new LinkedHashMap<>();
		if (agg == null || !agg.has("entries"))
			return map;

		for (JsonNode entry : agg.get("entries"))
		{
			String series = entry.get("series").asText();
			double total  = entry.has("total") ? entry.get("total").asDouble() : 0;
			double avg    = entry.has("avg")   ? entry.get("avg").asDouble()   : 0;
			double max    = entry.has("max")   ? entry.get("max").asDouble()   : 0;
			double rank   = entry.has("rank")  ? entry.get("rank").asDouble()  : 0;
			map.put(series, new double[]{ total, avg, max, rank });
		}
		return map;
	}

	// -- trigger_query_store_extract ------------------------------------------

	private String toolTriggerQueryStoreExtract(JsonNode args)
	throws Exception
	{
		String serverName     = getRequiredString(args, "serverName");
		String action         = getOptionalString(args, "action",         "trigger").trim().toLowerCase();
		String extractionType = getOptionalString(args, "extractionType", "QUERY_STORE").trim().toUpperCase();
		requireServerSession(serverName);

		String[] collectorInfo = resolveCollectorInfo(serverName);
		String   baseUrl       = collectorInfo[0];
		String   auth          = collectorInfo[1];

		String url;
		HttpRequest.Builder reqBuilder;

		if ("status".equals(action))
		{
			// GET extractStatus
			HtmlQueryString sqs = new HtmlQueryString(baseUrl + "/api/mgt/query-store");
			sqs.add("action", "extractStatus");
			sqs.add("type",   extractionType);
			url = sqs.toString();
			reqBuilder = HttpRequest.newBuilder().uri(URI.create(url)).GET();
		}
		else
		{
			// POST to trigger extraction
			HtmlQueryString sqs = new HtmlQueryString(baseUrl + "/api/mgt/query-store");
			sqs.add("action", "extract");
			sqs.add("type",   extractionType);
			url = sqs.toString();
			reqBuilder = HttpRequest.newBuilder()
					.uri(URI.create(url))
					.POST(HttpRequest.BodyPublishers.noBody());
		}

		if (auth != null && !auth.isEmpty())
			reqBuilder.header("Authorization", auth);

		_logger.debug("MCP trigger_query_store_extract: action={} url='{}'", action, url);

		try
		{
			HttpResponse<String> resp = _httpClient.send(reqBuilder.build(), HttpResponse.BodyHandlers.ofString());
			if (resp.statusCode() == 200 || resp.statusCode() == 409 /* already running — not a hard error */)
				return resp.body();

			ObjectNode err = _om.createObjectNode();
			err.put("error",      "collector-http-error");
			err.put("httpStatus", resp.statusCode());
			err.put("body",       resp.body());
			return _om.writeValueAsString(err);
		}
		catch (ConnectException ex)
		{
			ObjectNode err = _om.createObjectNode();
			err.put("error",   "collector-offline");
			err.put("message", "Collector at " + baseUrl + " is not reachable: " + ex.getMessage());
			return _om.writeValueAsString(err);
		}
	}

	// -- get_query_store_databases --------------------------------------------

	private String toolGetQueryStoreDatabases(JsonNode args)
	throws Exception
	{
		String serverName = getRequiredString(args, "serverName");
		String dbname     = getOptionalString(args, "dbname", "");
		String ts         = getOptionalString(args, "ts",     "");
		requireServerSession(serverName);
		return proxyQueryStore(serverName, "listDatabases", dbname, "", "", "", "", ts);
	}

	// -- get_query_store_top_queries -------------------------------------------

	private String toolGetQueryStoreTopQueries(JsonNode args)
	throws Exception
	{
		String serverName    = getRequiredString(args, "serverName");
		String dbname        = getRequiredString(args, "dbname");
		String rankBy        = getOptionalString(args, "rankBy",        "duration");
		String topN          = getOptionalString(args, "topN",          "25");
		String minExecutions = getOptionalString(args, "minExecutions", "2");
		String ts            = getOptionalString(args, "ts",            "");
		requireServerSession(serverName);
		return proxyQueryStore(serverName, "topQueries", dbname, "", rankBy, topN, minExecutions, ts);
	}

	// -- get_query_store_query_detail ------------------------------------------

	private String toolGetQueryStoreQueryDetail(JsonNode args)
	throws Exception
	{
		String serverName  = getRequiredString(args, "serverName");
		String dbname      = getRequiredString(args, "dbname");
		String queryId     = getRequiredString(args, "queryId");
		String includePlan = getOptionalString(args, "includePlan", "false");
		String ts          = getOptionalString(args, "ts",          "");
		requireServerSession(serverName);

		// queryId comes from the JSON as either a number or a string
		if (!queryId.matches("\\d+"))
			throw new McpInvalidParamsException("queryId must be a positive integer, got: " + queryId);

		return proxyQueryStore(serverName, "queryDetail", dbname, queryId, "", "", includePlan, ts);
	}

	/**
	 * Shared proxy helper for all query-store tool calls.
	 * Maps directly to the collector's {@code /api/mgt/query-store} endpoint.
	 */
	private String proxyQueryStore(
			String serverName,
			String action,
			String dbname,
			String queryId,
			String rankBy,
			String topN,
			String minExecutionsOrIncludePlan,
			String ts)
	throws Exception
	{
		String[] collectorInfo = resolveCollectorInfo(serverName);
		String   baseUrl       = collectorInfo[0];
		String   auth          = collectorInfo[1];

		HtmlQueryString qs = new HtmlQueryString(baseUrl + "/api/mgt/query-store");
		qs.add("action", action);
		qs.addIfNotEmpty("dbname",   dbname);
		qs.addIfNotEmpty("queryId",  queryId);
		qs.addIfNotEmpty("rankBy",   rankBy);
		if (!topN.isEmpty() && action.equals("topQueries"))
			qs.add("topN", topN);
		if (!minExecutionsOrIncludePlan.isEmpty())
			qs.add(action.equals("queryDetail") ? "includePlan" : "minExecutions", minExecutionsOrIncludePlan);
		qs.addIfNotEmpty("ts", ts);
		String url = qs.toString();
		_logger.debug("MCP {}: proxying to '{}'", action, url);

		HttpRequest.Builder reqBuilder = HttpRequest.newBuilder().uri(URI.create(url)).GET();
		if (auth != null && !auth.isEmpty())
			reqBuilder.header("Authorization", auth);

		try
		{
			HttpResponse<String> resp = _httpClient.send(reqBuilder.build(), HttpResponse.BodyHandlers.ofString());
			if (resp.statusCode() == 200)
				return resp.body();

			ObjectNode err = _om.createObjectNode();
			err.put("error",      "collector-http-error");
			err.put("httpStatus", resp.statusCode());
			err.put("body",       resp.body());
			return _om.writeValueAsString(err);
		}
		catch (ConnectException ex)
		{
			ObjectNode err = _om.createObjectNode();
			err.put("error",   "collector-offline");
			err.put("message", "Collector at " + baseUrl + " is not reachable: " + ex.getMessage());
			return _om.writeValueAsString(err);
		}
	}

	// -- get_active_alarms -----------------------------------------------------

	private String toolGetActiveAlarms(JsonNode args)
	throws Exception
	{
		String serverName = getOptionalString(args, "serverName", null);

		requireReader();

		if (serverName != null && !serverName.isEmpty())
			requireServerSession(serverName);

		List<DbxAlarmActive> alarms = CentralPersistReader.getInstance().getAlarmActive(serverName);

		// Enrich with mute state (same as AlarmActiveController does)
		AlarmMuteManager muteMgr = AlarmMuteManager.getInstance();
		for (DbxAlarmActive alarm : alarms)
		{
			String alarmId = alarm.getAlarmId();
			if (alarmId != null && muteMgr.isMuted(alarmId))
			{
				AlarmMuteManager.MuteRecord rec = muteMgr.getMute(alarmId);
				if (rec != null)
				{
					alarm.setIsMuted(true);
					alarm.setMuteReason(rec.reason);
					alarm.setMutedByUser(rec.mutedByUser);
					alarm.setMutedTime(rec.mutedTime);
					alarm.setMuteExpiresAt(rec.expiresAt);
				}
			}
		}

		return _om.writeValueAsString(alarms);
	}

	// -- get_alarm_history -----------------------------------------------------

	private String toolGetAlarmHistory(JsonNode args)
	throws Exception
	{
		String serverName = getOptionalString(args, "serverName", null);
		String startTime  = getOptionalString(args, "startTime",  "-24h");
		String endTime    = getOptionalString(args, "endTime",    null);
		String type       = getOptionalString(args, "type",       null);
		String category   = getOptionalString(args, "category",   null);

		requireReader();

		if (serverName != null && !serverName.isEmpty())
			requireServerSession(serverName);

		List<DbxAlarmHistory> history = CentralPersistReader.getInstance()
				.getAlarmHistory(serverName, startTime, endTime, type, category);
		return _om.writeValueAsString(history);
	}

	// -- get_dbms_config -------------------------------------------------------

	private String toolGetDbmsConfig(JsonNode args)
	throws Exception
	{
		String serverName = getRequiredString(args, "serverName");
		requireServerSession(serverName);

		String[] collectorInfo = resolveCollectorInfo(serverName);
		String baseUrl = collectorInfo[0];
		String auth    = collectorInfo[1]; // may be null

		HtmlQueryString qs = new HtmlQueryString(baseUrl + "/api/mgt/dbms-config");
		qs.add("srv", serverName);
		String url = qs.toString();

		_logger.debug("MCP get_dbms_config: proxying to '{}'", url);

		HttpRequest.Builder reqBuilder = HttpRequest.newBuilder().uri(URI.create(url)).GET();
		if (auth != null && !auth.isEmpty())
			reqBuilder.header("Authorization", auth);

		try
		{
			HttpResponse<String> resp = _httpClient.send(reqBuilder.build(), HttpResponse.BodyHandlers.ofString());
			if (resp.statusCode() == 200)
				return resp.body(); // already JSON — pass through directly

			// Non-200: wrap in an error object so the AI understands what happened
			ObjectNode err = _om.createObjectNode();
			err.put("error",      "collector-http-error");
			err.put("httpStatus", resp.statusCode());
			err.put("body",       resp.body());
			return _om.writeValueAsString(err);
		}
		catch (ConnectException ex)
		{
			ObjectNode err = _om.createObjectNode();
			err.put("error",   "collector-offline");
			err.put("message", "Collector at " + baseUrl + " is not reachable: " + ex.getMessage());
			return _om.writeValueAsString(err);
		}
	}

	// -- get_deadlocks --------------------------------------------------------

	private String toolGetDeadlocks(JsonNode args)
	throws Exception
	{
		String serverName = getRequiredString(args, "serverName");
		String action     = getOptionalString(args, "action", "summary");
		String ts         = getOptionalString(args, "ts",     "");
		requireServerSession(serverName);
		return proxyCollector(serverName, "/api/mgt/deadlock", "action", action, "ts", ts);
	}

	// -- get_job_history -------------------------------------------------------

	private String toolGetJobHistory(JsonNode args)
	throws Exception
	{
		String serverName = getRequiredString(args, "serverName");
		String action     = getOptionalString(args, "action",  "overview");
		String jobName    = getOptionalString(args, "jobName", "");
		String status     = getOptionalString(args, "status",  "");
		String limit      = getOptionalString(args, "limit",   "");
		String ts         = getOptionalString(args, "ts",      "");
		requireServerSession(serverName);

		String[] collectorInfo = resolveCollectorInfo(serverName);
		String   baseUrl       = collectorInfo[0];
		String   auth          = collectorInfo[1];

		HtmlQueryString qs = new HtmlQueryString(baseUrl + "/api/mgt/job-scheduler");
		qs.add          ("action",  action);
		qs.addIfNotEmpty("jobName", jobName);
		qs.addIfNotEmpty("status",  status);
		qs.addIfNotEmpty("limit",   limit);
		qs.addIfNotEmpty("ts",      ts);
		String url = qs.toString();
		_logger.debug("MCP get_job_history: proxying to '{}'", url);

		HttpRequest.Builder reqBuilder = HttpRequest.newBuilder().uri(URI.create(url)).GET();
		if (auth != null && !auth.isEmpty())
			reqBuilder.header("Authorization", auth);

		try
		{
			HttpResponse<String> resp = _httpClient.send(reqBuilder.build(), HttpResponse.BodyHandlers.ofString());
			if (resp.statusCode() == 200)
				return resp.body();

			ObjectNode err = _om.createObjectNode();
			err.put("error",      "collector-http-error");
			err.put("httpStatus", resp.statusCode());
			err.put("body",       resp.body());
			return _om.writeValueAsString(err);
		}
		catch (ConnectException ex)
		{
			ObjectNode err = _om.createObjectNode();
			err.put("error",   "collector-offline");
			err.put("message", "Collector at " + baseUrl + " is not reachable: " + ex.getMessage());
			return _om.writeValueAsString(err);
		}
	}

	/**
	 * Generic single-path proxy helper: resolves collector info, builds the URL with the supplied
	 * key/value pairs (odd-indexed = key, even-indexed = value), and returns the response body.
	 * Empty-string values are skipped (added with addIfNotEmpty semantics for all except the first pair).
	 */
	private String proxyCollector(String serverName, String path, String... kvPairs)
	throws Exception
	{
		String[] collectorInfo = resolveCollectorInfo(serverName);
		String   baseUrl       = collectorInfo[0];
		String   auth          = collectorInfo[1];

		HtmlQueryString qs = new HtmlQueryString(baseUrl + path);
		for (int i = 0; i + 1 < kvPairs.length; i += 2)
		{
			String k = kvPairs[i];
			String v = kvPairs[i + 1];
			if (i == 0)
				qs.add(k, v); // first param always included (typically 'action')
			else
				qs.addIfNotEmpty(k, v);
		}
		String url = qs.toString();
		_logger.debug("MCP proxyCollector: proxying to '{}'", url);

		HttpRequest.Builder reqBuilder = HttpRequest.newBuilder().uri(URI.create(url)).GET();
		if (auth != null && !auth.isEmpty())
			reqBuilder.header("Authorization", auth);

		try
		{
			HttpResponse<String> resp = _httpClient.send(reqBuilder.build(), HttpResponse.BodyHandlers.ofString());
			if (resp.statusCode() == 200)
				return resp.body();

			ObjectNode err = _om.createObjectNode();
			err.put("error",      "collector-http-error");
			err.put("httpStatus", resp.statusCode());
			err.put("body",       resp.body());
			return _om.writeValueAsString(err);
		}
		catch (ConnectException ex)
		{
			ObjectNode err = _om.createObjectNode();
			err.put("error",   "collector-offline");
			err.put("message", "Collector at " + baseUrl + " is not reachable: " + ex.getMessage());
			return _om.writeValueAsString(err);
		}
	}

	/**
	 * Look up the collector management URL and optional Authorization header for a given server.
	 * Reads the DbxTune info files written to disk when collectors start
	 * (same mechanism as {@code ProxyHelper.getSrvInfo()}).
	 *
	 * @return String[2]: [0] = base URL (e.g. "http://host:9292"), [1] = Authorization header value (may be null)
	 * @throws McpInvalidParamsException if no info file is found for the server (collector offline / not local)
	 */
	private String[] resolveCollectorInfo(String serverName)
	{
		for (String file : OverviewServlet.getInfoFilesDbxTune())
		{
			File f        = new File(file);
			String fSrvName = f.getName().split("\\.")[0];
			if (!fSrvName.equals(serverName)) continue;

			Configuration conf = new Configuration(file);
			String host = conf.getProperty   ("dbxtune.management.host", null);
			int    port = conf.getIntProperty ("dbxtune.management.port", -1);
			String info = conf.getProperty   ("dbxtune.management.info", null);

			if (host == null || port == -1)
				throw new McpInvalidParamsException(
					"Collector for '" + serverName + "' has no management endpoint configured in its info file.");

			String baseUrl = "http://" + host + ":" + port;
			String auth    = null;
			if (info != null && !info.isEmpty())
			{
				try
				{
					JsonNode infoNode = _om.readTree(info);
					if (infoNode.has("authorization"))
						auth = infoNode.get("authorization").asText();
				}
				catch (Exception ignored) {}
			}
			return new String[]{ baseUrl, auth };
		}
		throw new McpInvalidParamsException(
			"No running collector found for server '" + serverName + "'. "
			+ "The collector must be online and local for get_dbms_config to work.");
	}

	// =========================================================================
	// resources/list
	// =========================================================================

	private JsonNode handleResourcesList()
	throws Exception
	{
		ArrayNode resources = _om.createArrayNode();

		// Static resource: server list
		ObjectNode serversResource = _om.createObjectNode();
		serversResource.put("uri",         "dbxcentral://servers");
		serversResource.put("name",        "Monitored Servers");
		serversResource.put("description", "List of all database servers currently monitored by DbxCentral");
		serversResource.put("mimeType",    "application/json");
		resources.add(serversResource);

		// Dynamic resources: per-server metric catalogs
		if (CentralPersistReader.hasInstance())
		{
			try
			{
				List<DbxCentralSessions> sessions = CentralPersistReader.getInstance().getSessions(true, -1);
				for (DbxCentralSessions s : sessions)
				{
					String srvName = s.getServerName();

					ObjectNode metricsResource = _om.createObjectNode();
					metricsResource.put("uri",         "dbxcentral://servers/" + srvName + "/metrics");
					metricsResource.put("name",        srvName + " — Available Metrics");
					metricsResource.put("description", "Performance counter graphs available for " + srvName + " (" + s.getProductString() + ")");
					metricsResource.put("mimeType",    "application/json");
					resources.add(metricsResource);

					ObjectNode summaryResource = _om.createObjectNode();
					summaryResource.put("uri",         "dbxcentral://servers/" + srvName + "/summary");
					summaryResource.put("name",        srvName + " — Performance Summary");
					summaryResource.put("description", "Latest performance snapshot for " + srvName);
					summaryResource.put("mimeType",    "application/json");
					resources.add(summaryResource);
				}
			}
			catch (SQLException e)
			{
				_logger.warn("MCP resources/list: failed to enumerate sessions: {}", e.getMessage());
			}
		}

		ObjectNode result = _om.createObjectNode();
		result.set("resources", resources);
		return result;
	}

	// =========================================================================
	// resources/read
	// =========================================================================

	private JsonNode handleResourcesRead(JsonNode params)
	throws Exception
	{
		String uri = getRequiredString(params, "uri");

		String content;

		if ("dbxcentral://servers".equals(uri))
		{
			content = toolListServers(_om.createObjectNode());
		}
		else if (uri.startsWith("dbxcentral://servers/") && uri.endsWith("/metrics"))
		{
			String serverName = uri.substring("dbxcentral://servers/".length(), uri.length() - "/metrics".length());
			ObjectNode fakeArgs = _om.createObjectNode();
			fakeArgs.put("serverName", serverName);
			content = toolListMetrics(fakeArgs);
		}
		else if (uri.startsWith("dbxcentral://servers/") && uri.endsWith("/summary"))
		{
			String serverName = uri.substring("dbxcentral://servers/".length(), uri.length() - "/summary".length());
			ObjectNode fakeArgs = _om.createObjectNode();
			fakeArgs.put("serverName", serverName);
			content = toolGetPerfSummary(fakeArgs);
		}
		else
		{
			throw new McpInvalidParamsException("Unknown resource URI: '" + uri + "'");
		}

		ObjectNode textContent = _om.createObjectNode();
		textContent.put("uri",      uri);
		textContent.put("mimeType", "application/json");
		textContent.put("text",     content);

		ArrayNode contents = _om.createArrayNode();
		contents.add(textContent);

		ObjectNode result = _om.createObjectNode();
		result.set("contents", contents);
		return result;
	}

	// =========================================================================
	// prompts/list
	// =========================================================================

	private JsonNode handlePromptsList()
	{
		ArrayNode prompts = _om.createArrayNode();

		prompts.add(buildPromptMeta("diagnose_performance",
				"Guided performance diagnosis — detects DBMS type and routes to the correct vendor-specific investigation workflow",
				buildPromptArg("serverName", "Name of the server to diagnose (omit to be prompted)", false),
				buildPromptArg("timeRange",  "Time range to analyze, e.g. '-2h', '-6h', '-1d'. Default: '-2h'", false)));

		prompts.add(buildPromptMeta("diagnose_sqlserver",
				"SQL Server-specific performance diagnosis: waits → root cause mapping → queries → TempDB → jobs → missing indexes → config",
				buildPromptArg("serverName", "Name of the SQL Server instance to diagnose", true),
				buildPromptArg("timeRange",  "Time range to analyze, e.g. '-2h', '-6h', '-1d'. Default: '-2h'", false)));

		prompts.add(buildPromptMeta("diagnose_ase",
				"Sybase ASE-specific performance diagnosis: engine utilization → wait events → procedure cache → locks → config",
				buildPromptArg("serverName", "Name of the ASE server to diagnose", true),
				buildPromptArg("timeRange",  "Time range to analyze, e.g. '-2h', '-6h', '-1d'. Default: '-2h'", false)));

		prompts.add(buildPromptMeta("diagnose_postgres",
				"PostgreSQL-specific performance diagnosis: active queries → top SQL → autovacuum → locks → config",
				buildPromptArg("serverName", "Name of the PostgreSQL server to diagnose", true),
				buildPromptArg("timeRange",  "Time range to analyze, e.g. '-2h', '-6h', '-1d'. Default: '-2h'", false)));

		prompts.add(buildPromptMeta("analyze_waits",
				"Deep-dive wait statistics analysis — maps top wait types to their root causes and recommends next steps",
				buildPromptArg("serverName", "Name of the server to analyze", true),
				buildPromptArg("timeRange",  "Time range to analyze (e.g. '-2h', '-1d'). Default: '-2h'", false)));

		prompts.add(buildPromptMeta("find_top_queries",
				"Identify the most expensive queries and analyze their resource consumption patterns",
				buildPromptArg("serverName", "Name of the server to analyze", true),
				buildPromptArg("rankBy",     "Ranking metric: elapsed, cpu, reads, executions. Default: elapsed", false)));

		ObjectNode result = _om.createObjectNode();
		result.set("prompts", prompts);
		return result;
	}

	// =========================================================================
	// prompts/get
	// =========================================================================

	private JsonNode handlePromptsGet(JsonNode params)
	throws Exception
	{
		String name = getRequiredString(params, "name");
		JsonNode arguments = params.has("arguments") ? params.get("arguments") : _om.createObjectNode();

		String serverName = getOptionalString(arguments, "serverName", "{serverName}");
		String timeRange  = getOptionalString(arguments, "timeRange",  "-2h");
		String rankBy     = getOptionalString(arguments, "rankBy",     "elapsed");

		String promptText;

		switch (name)
		{
			case "diagnose_performance":
				promptText = buildDiagnosePrompt(serverName, timeRange);
				break;
			case "diagnose_sqlserver":
				promptText = buildDiagnoseSqlServerPrompt(serverName, timeRange);
				break;
			case "diagnose_ase":
				promptText = buildDiagnoseAsePrompt(serverName, timeRange);
				break;
			case "diagnose_postgres":
				promptText = buildDiagnosePostgresPrompt(serverName, timeRange);
				break;
			case "analyze_waits":
				promptText = buildAnalyzeWaitsPrompt(serverName, timeRange);
				break;
			case "find_top_queries":
				promptText = buildFindTopQueriesPrompt(serverName, rankBy);
				break;
			default:
				throw new McpInvalidParamsException("Unknown prompt: '" + name + "'");
		}

		ObjectNode userMessage = _om.createObjectNode();
		userMessage.put("role", "user");
		ObjectNode messageContent = _om.createObjectNode();
		messageContent.put("type", "text");
		messageContent.put("text", promptText);
		userMessage.set("content", messageContent);

		ArrayNode messages = _om.createArrayNode();
		messages.add(userMessage);

		ObjectNode result = _om.createObjectNode();
		result.set("messages", messages);
		return result;
	}

	// =========================================================================
	// Prompt text builders
	// =========================================================================

	private String buildDiagnosePrompt(String serverName, String timeRange)
	{
		boolean hasSrv = !"{serverName}".equals(serverName);
		String srvClause = hasSrv ? "server **" + serverName + "**" : "the target server";
		return "Please diagnose the performance of " + srvClause + " over the last " + timeRange + ".\n\n"
			+ "## Step 1 — Identify the server and its DBMS type\n"
			+ (hasSrv
				? "Server is already specified: **" + serverName + "**.\n"
				: "Call `list_servers` to see all monitored servers. Pick the one to investigate.\n")
			+ "Check the `productString` field to determine the DBMS type, then follow the matching workflow:\n\n"
			+ "  - **SqlServerTune** → follow the `diagnose_sqlserver` prompt workflow\n"
			+ "  - **AseTune**       → follow the `diagnose_ase` prompt workflow\n"
			+ "  - **PostgresTune**  → follow the `diagnose_postgres` prompt workflow\n"
			+ "  - **MySqlTune** / **MariaDbTune** → use get_wait_stats + get_top_queries + get_active_alarms + get_dbms_config\n\n"
			+ "## Step 2 — Always start with alarms\n"
			+ "Call `get_active_alarms` (and `get_alarm_history` startTime='" + timeRange + "') "
			+ "to see what thresholds have been breached — this often points directly to the problem area.\n\n"
			+ "## Step 3 — DBMS-specific deep dive\n"
			+ "Proceed with the vendor-specific workflow above based on the DBMS type found in Step 1.\n\n"
			+ "## Final output\n"
			+ "Synthesize findings into:\n"
			+ "  (a) What the problem is and which metric confirms it\n"
			+ "  (b) Which queries, sessions, or jobs are causing it\n"
			+ "  (c) Concrete fix recommendations with priority order";
	}

	private String buildDiagnoseSqlServerPrompt(String serverName, String timeRange)
	{
		String s = serverName;
		return "Diagnose SQL Server performance for **" + s + "** over the last " + timeRange + ".\n\n"
			+ "## Step 1 — Active alarms\n"
			+ "Call `get_active_alarms` serverName='" + s + "' and `get_alarm_history` startTime='" + timeRange + "'.\n"
			+ "Note any alarm types — they often point directly to the problem.\n\n"

			+ "## Step 2 — Wait statistics (primary root-cause signal)\n"
			+ "Call `get_wait_stats` serverName='" + s + "' startTime='" + timeRange + "'.\n"
			+ "Map the top wait types to root causes and call the matching drill-down tool:\n\n"
			+ "| Wait type               | Meaning                          | Next tool                          |\n"
			+ "|-------------------------|----------------------------------|------------------------------------- |\n"
			+ "| PAGEIOLATCH_SH/EX       | Data page I/O (missing index / full scan) | `get_io_analysis`, `get_table_schema` |\n"
			+ "| LCK_M_* / DEADLOCK      | Lock contention or deadlock      | `get_blocking_info`                 |\n"
			+ "| CXPACKET / CXCONSUMER   | Parallel query overhead          | `get_top_queries`, `get_dbms_config` (check MAXDOP) |\n"
			+ "| RESOURCE_SEMAPHORE      | Memory grant queue               | `get_memory_analysis`, `get_dbms_config` (max server memory) |\n"
			+ "| SOS_SCHEDULER_YIELD     | CPU pressure                     | `get_top_queries` rankBy=cpu        |\n"
			+ "| WRITELOG                | Log write latency                | `get_io_analysis` (log files)       |\n"
			+ "| ASYNC_NETWORK_IO        | Slow client / large result sets  | `get_top_queries` (high row counts) |\n"
			+ "| THREADPOOL              | Thread starvation                | `get_dbms_config` (check max worker threads) |\n\n"

			+ "## Step 3 — Top queries\n"
			+ "Call `get_top_queries` serverName='" + s + "' startTime='" + timeRange + "'.\n"
			+ "For high-read queries: call `get_table_schema` for the tables involved — check for missing indexes.\n"
			+ "Interpret entries: high total/low avg = infrequent expensive query; high avg/high count = hot path.\n\n"

			+ "## Step 4 — TempDB spills\n"
			+ "Call `get_tempdb_usage` serverName='" + s + "' startTime='" + timeRange + "'.\n"
			+ "High TempDB usage → look for table variable usage, missing index causing sort spill, or large hash joins.\n"
			+ "Cross-reference with top queries and check DSR 'Top ... TEMPDB_SPILLS' section.\n\n"

			+ "## Step 5 — SQL Agent jobs\n"
			+ "Call `get_daily_report_section` serverName='" + s + "' section='Job Agent'.\n"
			+ "Scheduled jobs often cause periodic load spikes. Look for jobs running during the problem window.\n\n"

			+ "## Step 6 — Missing indexes\n"
			+ "Call `get_daily_report_section` serverName='" + s + "' section='Top Missing Indexes'.\n"
			+ "High-impact missing indexes are the most common fix for PAGEIOLATCH and sort spills.\n"
			+ "Call `get_table_schema` for any table with Impact > 100,000.\n\n"

			+ "## Step 7 — Configuration\n"
			+ "Call `get_dbms_config` serverName='" + s + "'.\n"
			+ "Key settings to check:\n"
			+ "  - `max server memory` — should leave 10-20% for OS. Too low → RESOURCE_SEMAPHORE waits.\n"
			+ "  - `max degree of parallelism` (MAXDOP) — should be ≤ number of cores per NUMA node.\n"
			+ "  - `cost threshold for parallelism` — default 5 is too low; 50 is a common starting point.\n"
			+ "  - `optimize for ad hoc workloads` — should be 1 on OLTP servers.\n\n"

			+ "## Final output\n"
			+ "Summarize: (a) dominant wait type and what it means, (b) top 3 queries by resource cost, "
			+ "(c) any missing indexes with their impact scores, (d) config issues, "
			+ "(e) prioritized fix recommendations.";
	}

	private String buildDiagnoseAsePrompt(String serverName, String timeRange)
	{
		String s = serverName;
		return "Diagnose Sybase ASE performance for **" + s + "** over the last " + timeRange + ".\n\n"
			+ "## Step 1 — Active alarms\n"
			+ "Call `get_active_alarms` serverName='" + s + "' and `get_alarm_history` startTime='" + timeRange + "'.\n\n"

			+ "## Step 2 — Engine utilization\n"
			+ "Call `get_performance_summary` serverName='" + s + "' startTime='" + timeRange + "'.\n"
			+ "Look for `CmSummary` graphs showing engine busy %, runnable queue length, and I/O rates.\n"
			+ "High runnable queue (> number of engines) → CPU-bound. High I/O rates → I/O-bound.\n\n"

			+ "## Step 3 — Wait events\n"
			+ "Call `get_wait_stats` serverName='" + s + "' startTime='" + timeRange + "'.\n"
			+ "Key ASE wait events:\n"
			+ "  - `sleep_xxx` waits are normal (ENGINE_IDLE, etc.) — exclude from analysis.\n"
			+ "  - High `lock_*` waits → lock contention → call `get_blocking_info`.\n"
			+ "  - High `io_*` waits (e.g. io_log_sleep, io_other) → I/O bottleneck → call `get_io_analysis`.\n"
			+ "  - High `net_*` waits → network or client bottleneck.\n\n"

			+ "## Step 4 — Top queries\n"
			+ "Call `get_top_queries` serverName='" + s + "' startTime='" + timeRange + "'.\n"
			+ "High logical reads → missing index or outdated statistics → call `get_table_schema`.\n\n"

			+ "## Step 5 — Blocking and deadlocks\n"
			+ "Call `get_blocking_info` serverName='" + s + "' startTime='" + timeRange + "'.\n"
			+ "In ASE, blocking chains can cascade quickly. Look for the head blocker SPID.\n\n"

			+ "## Step 6 — Memory: procedure cache and data cache\n"
			+ "Call `get_memory_analysis` serverName='" + s + "' startTime='" + timeRange + "'.\n"
			+ "Call `get_dbms_config` serverName='" + s + "'.\n"
			+ "Key ASE memory settings: `procedure cache size`, `total memory`, named data caches.\n"
			+ "Low procedure cache hit ratio (< 90%) → procedure cache too small or too many ad-hoc queries.\n\n"

			+ "## Step 7 — Configuration issues\n"
			+ "Check `get_dbms_config` for flagged issues. Common ASE problems:\n"
			+ "  - `number of locks` too low → lock table overflow under peak load.\n"
			+ "  - `number of open objects` too low → object table overflow.\n"
			+ "  - `max online engines` vs actual engine count — are engines idle?\n\n"

			+ "## Final output\n"
			+ "Summarize: (a) engine utilization picture, (b) dominant wait events, "
			+ "(c) top queries by logical reads, (d) any blocking issues, "
			+ "(e) memory and config concerns, (f) prioritized recommendations.";
	}

	private String buildDiagnosePostgresPrompt(String serverName, String timeRange)
	{
		String s = serverName;
		return "Diagnose PostgreSQL performance for **" + s + "** over the last " + timeRange + ".\n\n"
			+ "## Step 1 — Active alarms\n"
			+ "Call `get_active_alarms` serverName='" + s + "' and `get_alarm_history` startTime='" + timeRange + "'.\n\n"

			+ "## Step 2 — Active statements (what is running right now or was running)\n"
			+ "Call `get_active_statements` serverName='" + s + "' startTime='" + timeRange + "'.\n"
			+ "Look for: long-running queries (high elapsed time), queries in 'idle in transaction' state "
			+ "(they hold locks), and queries blocked waiting for locks.\n\n"

			+ "## Step 3 — Top queries\n"
			+ "Call `get_top_queries` serverName='" + s + "' startTime='" + timeRange + "'.\n"
			+ "PostgreSQL stores query stats in pg_stat_statements. High `total_time` queries are the primary target.\n"
			+ "For high-reads queries, call `get_table_schema` to check for missing indexes.\n\n"

			+ "## Step 4 — Wait statistics\n"
			+ "Call `get_wait_stats` serverName='" + s + "' startTime='" + timeRange + "'.\n"
			+ "Key PostgreSQL wait categories:\n"
			+ "  - `Lock` waits → lock contention → call `get_blocking_info`.\n"
			+ "  - `IO` waits (relation read, data file read) → missing index or full scans.\n"
			+ "  - `Client` waits → slow client reads or idle-in-transaction sessions.\n"
			+ "  - `LWLock` waits → internal contention (buffer mapping, WAL write).\n\n"

			+ "## Step 5 — Autovacuum and table bloat\n"
			+ "Check `get_daily_report_section` serverName='" + s + "' section='Autovacuum'.\n"
			+ "Tables with high dead tuple counts need `VACUUM`. Bloated tables cause slow sequential scans.\n"
			+ "Check `get_daily_report_section` section='Table Size' for tables with high dead tuple ratios.\n\n"

			+ "## Step 6 — Connection pressure\n"
			+ "Call `get_performance_summary` serverName='" + s + "' startTime='" + timeRange + "'.\n"
			+ "Check connection count vs `max_connections`. Near-limit → connection pooler needed (pgBouncer).\n\n"

			+ "## Step 7 — Configuration\n"
			+ "Call `get_dbms_config` serverName='" + s + "'.\n"
			+ "Key PostgreSQL settings to check:\n"
			+ "  - `shared_buffers` — typically 25% of RAM.\n"
			+ "  - `work_mem` — per-sort/hash allocation; too low → disk spills; too high → OOM.\n"
			+ "  - `checkpoint_completion_target` — should be 0.9.\n"
			+ "  - `autovacuum_vacuum_scale_factor` — default 0.2 is often too high for large tables.\n"
			+ "  - `max_connections` — high values waste memory; use a connection pooler instead.\n\n"

			+ "## Final output\n"
			+ "Summarize: (a) any currently blocked/long-running queries, (b) top SQL by total time, "
			+ "(c) vacuum/bloat concerns, (d) connection pressure, "
			+ "(e) config issues, (f) prioritized recommendations.";
	}

	private String buildAnalyzeWaitsPrompt(String serverName, String timeRange)
	{
		return "Analyze the wait statistics for server '" + serverName + "' over the last " + timeRange + ":\n\n"
			+ "1. Call get_wait_stats for serverName='" + serverName + "', startTime='" + timeRange + "'.\n"
			+ "2. Identify the top 5 wait types by total wait time.\n"
			+ "3. For each significant wait type, explain:\n"
			+ "   - What resource it represents\n"
			+ "   - What typically causes it\n"
			+ "   - What to investigate next\n\n"
			+ "Wait type reference:\n"
			+ "  PAGEIOLATCH_SH/EX   → Reading/writing data pages from disk. Investigate I/O subsystem or missing indexes.\n"
			+ "  LCK_M_*             → Lock contention. Investigate blocking chains with get_blocking_info.\n"
			+ "  CXPACKET            → Parallel query coordinator waiting. May indicate parallelism tuning needed.\n"
			+ "  CXCONSUMER          → Parallel query thread waiting. Usually paired with CXPACKET.\n"
			+ "  SOS_SCHEDULER_YIELD → CPU pressure. Threads yielding the scheduler. Check get_top_queries ranked by cpu.\n"
			+ "  RESOURCE_SEMAPHORE  → Memory grant queue. Queries waiting for workspace memory. Check get_memory_analysis.\n"
			+ "  WRITELOG            → Transaction log write latency. Investigate log disk I/O.\n"
			+ "  ASYNC_NETWORK_IO    → Sending results to clients. May indicate slow clients or large result sets.\n"
			+ "  SLEEP               → Session sleeping (normal, ignore in analysis).\n"
			+ "  BROKER_*            → Service Broker activity (usually benign).\n\n"
			+ "Provide actionable recommendations based on your findings.";
	}

	private String buildFindTopQueriesPrompt(String serverName, String rankBy)
	{
		return "Find and analyze the top queries on server '" + serverName + "' ranked by " + rankBy + ":\n\n"
			+ "1. Call get_top_queries for serverName='" + serverName + "', rankBy='" + rankBy + "'.\n"
			+ "2. Identify the top queries by " + rankBy + ".\n"
			+ "3. For each top query, analyze:\n"
			+ "   - Resource consumption (CPU, reads, elapsed time, execution count)\n"
			+ "   - Frequency — is it called once or thousands of times?\n"
			+ "   - Cost per execution vs total cost\n"
			+ "4. Call get_metric_data for CmExecQueryStats to see trends over time.\n"
			+ "5. If available, use get_active_statements to see if any are currently running.\n\n"
			+ "Provide recommendations for the top offenders:\n"
			+ "  - High logical reads → likely missing index or inefficient query plan\n"
			+ "  - High CPU + many executions → optimize or cache results\n"
			+ "  - High elapsed time but low CPU → waiting (check wait stats)\n"
			+ "  - High execution count → consider query result caching or parameter sniffing issues";
	}

	// =========================================================================
	// Private helpers
	// =========================================================================

	private void requireReader()
	{
		if (!CentralPersistReader.hasInstance())
			throw new McpInvalidParamsException("DbxCentral database reader is not available.");
	}

	private void requireServerSession(String serverName) 
	throws SQLException
	{
		requireReader();
		if (serverName == null || serverName.isEmpty())
			throw new McpInvalidParamsException("serverName is required.");
		if (!CentralPersistReader.getInstance().hasServerSession(serverName))
			throw new McpInvalidParamsException("Server '" + serverName + "' not found. Use list_servers to see available servers.");
	}

	private void validateTimeRange(String startTime, String endTime)
	{
		// Enforce max time range
		// We do a rough check: if relative (starts with '-'), parse the hours
		if (startTime != null && startTime.matches("-\\d+[hH]"))
		{
			int hours = Integer.parseInt(startTime.substring(1, startTime.length() - 1));
			if (hours > _maxTimeRangeHours)
				throw new McpInvalidParamsException(
					"Requested time range " + hours + "h exceeds DbxTuneCentral.mcp.maxTimeRangeHours=" + _maxTimeRangeHours + "h.");
		}
	}

	private void appendGraphData(ArrayNode target, CentralPersistReader reader,
			String serverName, String cmName, String graphName, String startTime, String endTime)
	{
		try
		{
			List<DbxGraphData> data = reader.getGraphData(serverName, cmName, graphName, startTime, endTime, SampleType.AUTO, -1, null);
			for (JsonNode item : _om.valueToTree(data))
				target.add(item);
		}
		catch (Exception e)
		{
			_logger.debug("MCP appendGraphData: no data for {}/{} on {}: {}", cmName, graphName, serverName, e.getMessage());
		}
	}

	/** Append data for ALL graphs belonging to a given CM, looking up graph names from props. */
	private void appendAllGraphsForCm(ArrayNode target, CentralPersistReader reader,
			String serverName, String cmName, List<DbxGraphProperties> props, String startTime, String endTime)
	{
		for (DbxGraphProperties p : props)
		{
			if (cmName.equals(p.getCmName()))
				appendGraphData(target, reader, serverName, cmName, p.getGraphName(), startTime, endTime);
		}
	}

	/**
	 * Aggregate all graphs for a CM into ranked summaries and append to target.
	 * Each graph produces one ObjectNode:
	 * <pre>
	 * { cmName, graphName, graphLabel, sampleCount, fromTime, toTime,
	 *   entries: [ {rank, series, total, avg, max}, ... ] }
	 * </pre>
	 * Entries are sorted descending by total and limited to {@code topN}.
	 */
	private void appendAllGraphsForCmAggregated(ArrayNode target, CentralPersistReader reader,
			String serverName, String cmName, List<DbxGraphProperties> props,
			String startTime, String endTime, int topN)
	{
		for (DbxGraphProperties p : props)
		{
			if (!cmName.equals(p.getCmName())) continue;
			try
			{
				List<DbxGraphData> rows = reader.getGraphData(serverName, cmName, p.getGraphName(), startTime, endTime, SampleType.AUTO, -1, null);
				if (rows == null || rows.isEmpty()) continue;
				ObjectNode agg = aggregateGraphData(rows, topN);
				if (agg != null) target.add(agg);
			}
			catch (Exception e)
			{
				_logger.debug("MCP aggregated: no data for {}/{} on {}: {}", cmName, p.getGraphName(), serverName, e.getMessage());
			}
		}
	}

	/**
	 * Reduce a list of time-series rows into a ranked summary.
	 * For each series key found in any row's data map, accumulate sum/count/max
	 * across all sample times, then return the top {@code topN} entries sorted by total.
	 */
	private ObjectNode aggregateGraphData(List<DbxGraphData> rows, int topN)
	{
		if (rows == null || rows.isEmpty()) return null;

		DbxGraphData first = rows.get(0);

		// series key → [sum, max, count]
		Map<String, double[]> acc = new HashMap<>();
		Timestamp minTs = null, maxTs = null;

		for (DbxGraphData row : rows)
		{
			Timestamp ts = row.getSessionSampleTime();
			if (ts != null)
			{
				if (minTs == null || ts.before(minTs)) minTs = ts;
				if (maxTs == null || ts.after(maxTs))  maxTs = ts;
			}
			if (row.getData() == null) continue;
			for (Map.Entry<String, Double> e : row.getData().entrySet())
			{
				double v = (e.getValue() == null) ? 0.0 : e.getValue();
				double[] a = acc.computeIfAbsent(e.getKey(), k -> new double[]{0.0, 0.0, 0.0});
				a[0] += v;             // sum
				if (v > a[1]) a[1] = v; // max
				a[2]++;                 // count
			}
		}

		// Sort by total descending
		List<Map.Entry<String, double[]>> sorted = new ArrayList<>(acc.entrySet());
		sorted.sort((a, b) -> Double.compare(b.getValue()[0], a.getValue()[0]));
		if (topN > 0 && sorted.size() > topN)
			sorted = sorted.subList(0, topN);

		ObjectNode result = _om.createObjectNode();
		result.put("cmName",      first.getCmName());
		result.put("graphName",   first.getGraphName());
		result.put("graphLabel",  first.getGraphLabel() != null ? first.getGraphLabel() : "");
		result.put("sampleCount", rows.size());
		if (minTs != null) result.put("fromTime", minTs.toString());
		if (maxTs != null) result.put("toTime",   maxTs.toString());

		ArrayNode entries = _om.createArrayNode();
		int rank = 1;
		for (Map.Entry<String, double[]> e : sorted)
		{
			double[] a    = e.getValue();
			double   avg  = (a[2] > 0) ? (a[0] / a[2]) : 0.0;
			ObjectNode en = _om.createObjectNode();
			en.put("rank",   rank++);
			en.put("series", e.getKey());
			en.put("total",  round2(a[0]));
			en.put("avg",    round2(avg));
			en.put("max",    round2(a[1]));
			entries.add(en);
		}
		result.set("entries", entries);
		return result;
	}

	private static double round2(double v) { return Math.round(v * 100.0) / 100.0; }

	private String getRequiredString(JsonNode node, String field)
	{
		if (!node.has(field) || node.get(field).isNull() || node.get(field).asText().isEmpty())
			throw new McpInvalidParamsException("Required parameter '" + field + "' is missing or empty.");
		return node.get(field).asText();
	}

	private String getOptionalString(JsonNode node, String field, String defaultValue)
	{
		if (!node.has(field) || node.get(field).isNull())
			return defaultValue;
		String val = node.get(field).asText();
		return val.isEmpty() ? defaultValue : val;
	}

	private int getOptionalInt(JsonNode node, String field, int defaultValue)
	{
		if (!node.has(field) || node.get(field).isNull())
			return defaultValue;
		try { return node.get(field).asInt(defaultValue); }
		catch (Exception ignored) { return defaultValue; }
	}

	// =========================================================================
	// JSON-building helpers
	// =========================================================================

	private JsonNode buildToolResult(String text)
	{
		ObjectNode textContent = _om.createObjectNode();
		textContent.put("type", "text");
		textContent.put("text", text);

		ArrayNode content = _om.createArrayNode();
		content.add(textContent);

		ObjectNode result = _om.createObjectNode();
		result.set("content",  content);
		result.put("isError",  false);
		return result;
	}

	/**
	 * Build an empty input schema (no parameters).
	 */
	private ObjectNode buildSchema()
	{
		ObjectNode schema = _om.createObjectNode();
		schema.put("type", "object");
		schema.set("properties", _om.createObjectNode());
		schema.set("required",   _om.createArrayNode());
		return schema;
	}

	/**
	 * Build an input schema from alternating tuples: (name, type, description, required).
	 */
	private ObjectNode buildSchema(Object... fieldDefs)
	{
		ObjectNode properties = _om.createObjectNode();
		ArrayNode  required   = _om.createArrayNode();

		for (int i = 0; i < fieldDefs.length; i += 4)
		{
			String  fieldName = (String)  fieldDefs[i];
			String  fieldType = (String)  fieldDefs[i + 1];
			String  fieldDesc = (String)  fieldDefs[i + 2];
			boolean isReq     = (Boolean) fieldDefs[i + 3];

			ObjectNode prop = _om.createObjectNode();
			prop.put("type",        fieldType);
			prop.put("description", fieldDesc);
			properties.set(fieldName, prop);

			if (isReq)
				required.add(fieldName);
		}

		ObjectNode schema = _om.createObjectNode();
		schema.put("type",       "object");
		schema.set("properties", properties);
		schema.set("required",   required);
		return schema;
	}

	private ObjectNode buildTool(String name, String description, ObjectNode inputSchema)
	{
		ObjectNode tool = _om.createObjectNode();
		tool.put("name",        name);
		tool.put("description", description);
		tool.set("inputSchema", inputSchema);
		return tool;
	}

	private ObjectNode buildPromptMeta(String name, String description, ObjectNode... args)
	{
		ObjectNode prompt = _om.createObjectNode();
		prompt.put("name",        name);
		prompt.put("description", description);

		ArrayNode argArray = _om.createArrayNode();
		for (ObjectNode a : args)
			argArray.add(a);
		prompt.set("arguments", argArray);

		return prompt;
	}

	private ObjectNode buildPromptArg(String name, String description, boolean required)
	{
		ObjectNode arg = _om.createObjectNode();
		arg.put("name",        name);
		arg.put("description", description);
		arg.put("required",    required);
		return arg;
	}
}
