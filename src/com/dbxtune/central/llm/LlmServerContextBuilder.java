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
package com.dbxtune.central.llm;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import com.dbxtune.utils.Configuration;
import com.dbxtune.utils.StringUtil;
import com.fasterxml.jackson.databind.JsonNode;

/**
 * The "Server configuration and hardware" block of the LLM prompt: how the server is configured, and what it runs on.
 * <p>
 * Built from what the SQL Server Collector stored at its start (the answer of the Collector's {@code /api/mgt/dbms-config}):
 * <ul>
 *   <li>the configuration ({@code params}): only the settings that are NOT at their default</li>
 *   <li>Host Info ({@code sys.dm_os_host_info}) and System Info ({@code sys.dm_os_sys_info}, the columns about CPU and memory)</li>
 *   <li>for the statement's database: its {@code sp_helpdb} row, its options from {@code sys.databases}, and its non-default
 *       database scoped configurations</li>
 * </ul>
 * ASE: see {@link #buildAse(JsonNode, JsonNode, JsonNode, JsonNode, String)}.
 * The texts are stored as the Collector printed the result sets: ASCII tables (see {@link #parseTables(String)}).
 * Kept small on purpose: it is sent with every request.
 */
public class LlmServerContextBuilder
{
	/** Send the server configuration and hardware with the prompt (SQL Server and ASE, when the Collector is known). */
	public static final String  PROPKEY_ENABLED = "DbxCentral.llm.serverContext";
	public static final boolean DEFAULT_ENABLED = true;

	public static boolean isEnabled()
	{
		return Configuration.getCombinedConfiguration().getBooleanProperty(PROPKEY_ENABLED, DEFAULT_ENABLED);
	}

	/** sys.dm_os_sys_info: the columns about CPU, memory and the machine (the rest are ticks, counters, ...) */
	private static final List<String> SYS_INFO_COLUMNS = Arrays.asList(
			"cpu_count", "hyperthread_ratio", "socket_count", "cores_per_socket", "numa_node_count",
			"physical_memory_kb", "committed_target_kb", "visible_target_kb", "max_workers_count", "scheduler_count",
			"affinity_type_desc", "virtual_machine_type_desc", "softnuma_configuration_desc", "sql_memory_model_desc", "container_type_desc");

	/** sp_helpdb: the database row, without what sys.databases says better (dbid, owner) */
	private static final List<String> HELPDB_COLUMNS = Arrays.asList("db_size", "created", "status", "compatibility_level");

	/** sys.databases: the options that change how a query is optimized or runs (the full row has ~90 columns) */
	private static final List<String> SYS_DATABASES_COLUMNS = Arrays.asList(
			"compatibility_level", "collation_name", "recovery_model_desc",
			"snapshot_isolation_state_desc", "is_read_committed_snapshot_on",
			"is_auto_create_stats_on", "is_auto_create_stats_incremental_on", "is_auto_update_stats_on", "is_auto_update_stats_async_on",
			"is_parameterization_forced", "is_query_store_on",
			"is_ansi_nulls_on", "is_ansi_padding_on", "is_ansi_warnings_on", "is_arithabort_on", "is_concat_null_yields_null_on",
			"is_quoted_identifier_on", "is_numeric_roundabort_on",
			"delayed_durability_desc", "is_memory_optimized_elevate_to_snapshot_on", "is_memory_optimized_enabled",
			"is_accelerated_database_recovery_on", "is_result_set_caching_on", "is_encrypted", "is_cdc_enabled", "is_published",
			"containment_desc", "page_verify_option_desc");

	/** ASE sp_configure sections that say nothing about how one statement runs (Monitoring alone is a quarter of the non-default rows), or should not leave the server (Security Related) */
	private static final List<String> ASE_SKIP_SECTIONS = Arrays.asList(
			"Monitoring", "Rep Agent Thread Administration", "DTM Administration", "Backup/Recovery", "Component Integration Services", "Security Related");

	/** ASE settings that are always shown, also at their default: without them the size of the server is unknown */
	private static final List<String> ASE_ALWAYS_SHOW = Arrays.asList(
			"max online engines", "number of engines at startup", "max memory", "total logical memory");

	/** ASE units that add nothing after the value */
	private static final List<String> ASE_NO_UNIT = Arrays.asList("name", "not applicable", "number", "switch");

	private static final List<String> ASE_HELPCACHE_COLUMNS = Arrays.asList("Cache Name", "Config Size", "Run Size", "Cache Type");
	private static final List<String> ASE_THREADPOOL_COLUMNS = Arrays.asList("ThreadPoolName", "Size", "Type");
	private static final List<String> ASE_HELPDB_COLUMNS = Arrays.asList("db_size", "durability", "status");

	/**
	 * ASE: from what the Collector stored at its start: the configuration (not at its default, or in {@link #ASE_ALWAYS_SHOW}),
	 * the caches, thread pools, server wide trace flags and the row of the statement's database. And from its Counter Models:
	 * the page size (not a configuration setting), and the cores and memory when host monitoring is on.
	 *
	 * @param dbmsConfig  the Collector's /api/mgt/dbms-config answer, see {@link #buildSqlServer(JsonNode, String)}
	 * @param cmSummary   the Collector's /api/mgt/cm/data answer for CmSummary {columns, rows}, or null
	 * @param cmMpstat    same for CmOsMpstat, or null (no host monitoring)
	 * @param cmMeminfo   same for CmOsMeminfo, or null (no host monitoring)
	 * @param dbname      the statement's database, may be blank
	 * @return the block, or "" when there is nothing
	 */
	public static String buildAse(JsonNode dbmsConfig, JsonNode cmSummary, JsonNode cmMpstat, JsonNode cmMeminfo, String dbname)
	{
		if (dbmsConfig == null)
			return "";

		dbname = StringUtil.nullToValue(dbname, "").trim().replaceAll("^\\[|\\]$", "");

		Map<String, String> texts = new LinkedHashMap<>();
		for (JsonNode t : dbmsConfig.path("texts"))
			texts.put(t.path("configName").asText(""), t.path("configText").asText(""));

		StringBuilder sb = new StringBuilder();

		String pageSize = firstRowValue(cmSummary, "asePageSize");
		if (StringUtil.hasValue(pageSize))
			sb.append("Page size (@@maxpagesize): ").append(pageSize).append(" bytes\n\n");

		appendHostFromMonitoring(sb, cmMpstat, cmMeminfo);

		// Configuration. 'CfgValStr' is the value: 'ConfigValue' is 0 for settings with a name (optimization goal = allrows_oltp).
		// 'PendingValue' is 0 when nothing is pending, so only look at it when 'Pending' says so.
		Params p = new Params(dbmsConfig);
		if (p.has("NonDefault") && p.has("ConfigName") && p.has("CfgValStr") && p.rows.size() > 0)
		{
			// With thread pools, their sizes tell the engines: 'number of engines at startup' only means something on an older ASE, without them
			boolean hasThreadPools = false;
			for (Table t : parseTables(texts.get("AseThreadPool")))
				hasThreadPools |= t.columns.contains("ThreadPoolName") && ! t.rows.isEmpty();

			StringBuilder cfg = new StringBuilder();
			for (JsonNode row : p.rows)
			{
				String cfgName = p.str(row, "ConfigName");
				if (hasThreadPools && "number of engines at startup".equals(cfgName))
					continue;

				boolean isNonDefault = p.bool(row, "NonDefault") && ! ASE_SKIP_SECTIONS.contains(p.str(row, "SectionName"));
				if ( ! isNonDefault && ! ASE_ALWAYS_SHOW.contains(cfgName) )
					continue;

				String unitStr = p.str(row, "Unit");
				String pending = null;
				if (p.bool(row, "Pending"))
					pending = StringUtil.hasValue(p.str(row, "CfgValStrPending")) ? p.str(row, "CfgValStrPending") : p.str(row, "PendingValue");
				appendSetting(cfg, cfgName,
						p.str(row, "CfgValStr") + (StringUtil.hasValue(unitStr) && ! ASE_NO_UNIT.contains(unitStr) ? " " + unitStr : ""),
						p.str(row, "DefaultValue"), pending);
			}
			sb.append("Server configuration, the settings that are not at their default and the size of the server (sp_configure):\n");
			sb.append(cfg);
			sb.append("\n");
		}

		appendAllRows(sb, "Data caches (sp_helpcache)", texts.get("AseCacheConfig"), "Cache Name",     ASE_HELPCACHE_COLUMNS);
		appendAllRows(sb, "Thread pools",               texts.get("AseThreadPool"),  "ThreadPoolName", ASE_THREADPOOL_COLUMNS);

		String traceFlags = aseServerWideTraceFlags(texts.get("AseTraceflags"));
		if (StringUtil.hasValue(traceFlags))
			sb.append("Trace flags, server wide: ").append(traceFlags).append("\n\n");

		if (StringUtil.hasValue(dbname))
			appendDatabaseRow(sb, "Database '" + dbname + "' (sp_helpdb)", texts.get("AseHelpDb"), dbname, ASE_HELPDB_COLUMNS);

		return sb.toString().trim();
	}

	/** Postgres categories (the start of it) that can change how a statement runs. An allow list: the others hold paths, commands, addresses and logging. */
	private static final List<String> PG_KEEP_CATEGORIES = Arrays.asList(
			"Query Tuning", "Resource Usage", "Autovacuum", "Client Connection Defaults / Statement Behavior", "Client Connection Defaults / Shared Library Preloading");

	/** Postgres settings that are always shown, also at their default: the memory and the planner's view of the server */
	private static final List<String> PG_ALWAYS_SHOW = Arrays.asList(
			"shared_buffers", "work_mem", "effective_cache_size", "maintenance_work_mem", "random_page_cost", "seq_page_cost",
			"max_parallel_workers_per_gather", "max_parallel_workers", "jit", "default_statistics_target", "effective_io_concurrency", "max_connections");

	private static final List<String> PG_DATABASE_COLUMNS = Arrays.asList("datcollate", "datctype", "daticulocale", "datconnlimit");

	/**
	 * Postgres: from what the Collector stored at its start: the configuration (see {@link #PG_KEEP_CATEGORIES},
	 * {@link #PG_ALWAYS_SHOW}), the installed extensions and the row of the statement's database. Cores and memory are
	 * only known from host monitoring.
	 * <p>
	 * 'CurrentValue' is what the Collector's OWN connection sees, and it sets some for itself (work_mem, Source 'session').
	 * So for those 'ResetVal' is the server's value, and 'NonDefault' (false for them) says nothing.
	 *
	 * @param dbmsConfig  the Collector's /api/mgt/dbms-config answer, see {@link #buildSqlServer(JsonNode, String)}
	 * @param cmMpstat    the Collector's /api/mgt/cm/data answer for CmOsMpstat {columns, rows}, or null (no host monitoring)
	 * @param cmMeminfo   same for CmOsMeminfo, or null (no host monitoring)
	 * @param dbname      the statement's database, may be blank
	 * @return the block, or "" when there is nothing
	 */
	public static String buildPostgres(JsonNode dbmsConfig, JsonNode cmMpstat, JsonNode cmMeminfo, String dbname)
	{
		if (dbmsConfig == null)
			return "";

		dbname = StringUtil.nullToValue(dbname, "").trim();

		Map<String, String> texts = new LinkedHashMap<>();
		for (JsonNode t : dbmsConfig.path("texts"))
			texts.put(t.path("configName").asText(""), t.path("configText").asText(""));

		StringBuilder sb = new StringBuilder();

		appendHostFromMonitoring(sb, cmMpstat, cmMeminfo);

		Params p = new Params(dbmsConfig);
		if (p.has("NonDefault") && p.has("ParameterName") && p.has("CurrentValue") && p.rows.size() > 0)
		{
			StringBuilder cfg = new StringBuilder();
			for (JsonNode row : p.rows)
			{
				String  cfgName   = p.str(row, "ParameterName");
				boolean isSession = "session".equals(p.str(row, "Source"));
				String  value     = isSession ? p.str(row, "ResetVal") : p.str(row, "CurrentValue");
				boolean isNonDefault = isSession ? ! value.equals(p.str(row, "BootVal")) : p.bool(row, "NonDefault");

				boolean keepCategory = false;
				for (String category : PG_KEEP_CATEGORIES)
					keepCategory |= p.str(row, "Category").startsWith(category);

				if ( ! (isNonDefault && keepCategory) && ! PG_ALWAYS_SHOW.contains(cfgName) )
					continue;

				String unit = p.str(row, "ParameterType");
				appendSetting(cfg, cfgName, pgValue(value, unit), pgValue(p.str(row, "BootVal"), unit), null);
			}
			sb.append("Server configuration, the settings that are not at their default and the memory and planner settings (pg_settings):\n");
			sb.append(cfg);
			sb.append("\n");
		}

		for (Table t : parseTables(texts.get("PostgresExtentions")))
		{
			int name    = t.columns.indexOf("Name");
			int version = t.columns.indexOf("Version");
			if (name < 0 || version < 0 || t.rows.isEmpty())
				continue;
			List<String> extensions = new ArrayList<>();
			for (List<String> row : t.rows)
				extensions.add(row.get(name) + " " + row.get(version));
			sb.append("Installed extensions: ").append(String.join(", ", extensions)).append("\n\n");
			break;
		}

		if (StringUtil.hasValue(dbname))
			appendDatabaseRow(sb, "Database '" + dbname + "' (pg_database)", texts.get("PostgresDbInfo"), "datname", dbname, PG_DATABASE_COLUMNS);

		return sb.toString().trim();
	}

	/** A Postgres value in its unit (pg_settings.unit), memory as MB: "632704", "8kB" gives "4943 MB". Anything else is left as it is. */
	private static String pgValue(String value, String unit)
	{
		if (StringUtil.isNullOrBlank(unit) || ! value.matches("\\d+(\\.\\d+)?"))
			return value; // also -1: "not set, use another setting"

		double kb = Double.parseDouble(value);
		switch (unit)
		{
			case "B":   kb = kb / 1024; break;
			case "kB":  break;
			case "8kB": kb = kb * 8; break;
			case "MB":  kb = kb * 1024; break;
			default:    return value + " " + unit;
		}
		return kb >= 1024 ? Math.round(kb / 1024) + " MB" : Math.round(kb) + " kB";
	}

	/** The settings of a /api/mgt/dbms-config answer: its columns differ per DBMS */
	private static class Params
	{
		final List<String> columns = new ArrayList<>();
		final JsonNode     rows;

		Params(JsonNode dbmsConfig)
		{
			JsonNode params = dbmsConfig.path("params");
			for (JsonNode c : params.path("columns"))
				columns.add(c.asText());
			rows = params.path("rows");
		}
		boolean has (String column)                { return columns.contains(column); }
		/** "" for a null or a column that is not there */
		String  str (JsonNode row, String column)  { int i = columns.indexOf(column); return i < 0 ? "" : row.path(i).asText(""); }
		boolean bool(JsonNode row, String column)  { int i = columns.indexOf(column); return i >= 0 && row.path(i).asBoolean(false); }
	}

	/**
	 * "- name = value (default x) (configured y, pending a restart)". A value that is a path is left out: it says nothing
	 * about a statement, and the layout of the server should not leave it.
	 * @param defaultValue  blank: no default is known
	 * @param pending       the value after the next restart, or null
	 */
	private static void appendSetting(StringBuilder cfg, String name, String value, String defaultValue, String pending)
	{
		if (value.matches("^(/|\\\\\\\\|[A-Za-z]:[\\\\/]|\\+[A-Z]).*"))
			return;

		cfg.append("- ").append(name).append(" = ").append(value);
		if (StringUtil.hasValue(defaultValue))
			cfg.append(" (default ").append(defaultValue).append(")");
		if (pending != null)
			cfg.append(" (configured ").append(pending).append(", pending a restart)");
		cfg.append("\n");
	}

	private static void appendHostFromMonitoring(StringBuilder sb, JsonNode cmMpstat, JsonNode cmMeminfo)
	{
		String host = hostLine(cmMpstat, cmMeminfo);
		if (StringUtil.hasValue(host))
			sb.append("Host, from host monitoring (a VM or container may give the DBMS less): ").append(host).append("\n\n");
	}

	/** "cores=4, memTotal=125.2 GB", what is known of it, from the host monitoring CMs. Windows has no total memory in its counters. */
	private static String hostLine(JsonNode cmMpstat, JsonNode cmMeminfo)
	{
		List<String> parts = new ArrayList<>();

		// One row per core, plus a total: 'all' in the column CPU (cpu on AIX), '_Total' in the column Instance on Windows
		if (cmMpstat != null)
		{
			int cores = 0;
			int col   = columnIndex(cmMpstat, "CPU", "cpu", "Instance");
			if (col >= 0)
			{
				for (JsonNode row : cmMpstat.path("rows"))
				{
					String cpu = row.path(col).asText("");
					if ( ! "all".equalsIgnoreCase(cpu) && ! "_Total".equalsIgnoreCase(cpu) )
						cores++;
				}
			}
			if (cores > 0)
				parts.add("cores=" + cores);
		}

		if (cmMeminfo != null)
		{
			int type = columnIndex(cmMeminfo, "memoryType");
			int used = columnIndex(cmMeminfo, "used");
			int unit = columnIndex(cmMeminfo, "unit");
			if (type >= 0 && used >= 0)
			{
				for (JsonNode row : cmMeminfo.path("rows"))
				{
					if ( ! "MemTotal".equals(row.path(type).asText("")) )
						continue;
					double val = row.path(used).asDouble(0);
					if (val > 0)
					{
						boolean isKb = unit >= 0 && "kB".equalsIgnoreCase(row.path(unit).asText(""));
						parts.add("memTotal=" + (isKb ? String.format(Locale.ROOT, "%.1f GB", val / 1024 / 1024) : row.path(used).asText("") + (unit >= 0 ? " " + row.path(unit).asText("") : "")));
					}
					break;
				}
			}
		}
		return String.join(", ", parts);
	}

	/** The switches of the line "Serverwide switches set :  3604 (print_output_to_client).", or "" when none */
	private static String aseServerWideTraceFlags(String text)
	{
		if (StringUtil.isNullOrBlank(text))
			return "";
		for (String line : text.split("\\r?\\n"))
		{
			if (line.startsWith("Serverwide switches set"))
			{
				String flags = line.substring(line.indexOf(':') + 1).trim().replaceAll("\\.$", "");
				return "none".equalsIgnoreCase(flags) ? "" : flags;
			}
		}
		return "";
	}

	/** One line per row, of the first table that has the column 'mustHaveColumn' */
	private static void appendAllRows(StringBuilder sb, String label, String text, String mustHaveColumn, List<String> onlyColumns)
	{
		for (Table t : parseTables(text))
		{
			if ( ! t.columns.contains(mustHaveColumn) || t.rows.isEmpty() )
				continue;
			sb.append(label).append(":\n");
			for (List<String> row : t.rows)
				sb.append("- ").append(toNameValues(t, row, onlyColumns)).append("\n");
			sb.append("\n");
			return;
		}
	}

	/** Position of the first of these columns in a /api/mgt/cm/data answer, or -1 */
	private static int columnIndex(JsonNode cm, String... names)
	{
		List<String> cols = new ArrayList<>();
		for (JsonNode c : cm.path("columns"))
			cols.add(c.asText());
		for (String name : names)
		{
			if (cols.contains(name))
				return cols.indexOf(name);
		}
		return -1;
	}

	/** The value of a column in the first row of a /api/mgt/cm/data answer (null = no answer), or "" */
	private static String firstRowValue(JsonNode cm, String column)
	{
		if (cm == null)
			return "";
		int col = columnIndex(cm, column);
		return col < 0 ? "" : cm.path("rows").path(0).path(col).asText("");
	}

	/**
	 * @param dbmsConfig  the Collector's /api/mgt/dbms-config answer: {params:{columns,rows}, texts:[{configName, configText}]}
	 * @param dbname      the statement's database, may be blank (then only the server level parts)
	 * @return the block, or "" when there is nothing
	 */
	public static String buildSqlServer(JsonNode dbmsConfig, String dbname)
	{
		if (dbmsConfig == null)
			return "";

		dbname = StringUtil.nullToValue(dbname, "").trim().replaceAll("^\\[|\\]$", "");

		Map<String, String> texts = new LinkedHashMap<>();
		for (JsonNode t : dbmsConfig.path("texts"))
			texts.put(t.path("configName").asText(""), t.path("configText").asText(""));

		StringBuilder sb = new StringBuilder();

		// Configuration: only what is not at its default
		JsonNode params  = dbmsConfig.path("params");
		List<String> pCols = new ArrayList<>();
		for (JsonNode c : params.path("columns"))
			pCols.add(c.asText());
		int nonDefault = pCols.indexOf("NonDefault");
		int name       = pCols.indexOf("Name");
		int runValue   = pCols.indexOf("RunValue");
		int cfgValue   = pCols.indexOf("ConfigValue");
		int defValue   = pCols.indexOf("DefaultValue");
		// No rows: nothing was stored (not "all at default")
		if (nonDefault >= 0 && name >= 0 && runValue >= 0 && params.path("rows").size() > 0)
		{
			StringBuilder cfg = new StringBuilder();
			for (JsonNode row : params.path("rows"))
			{
				if ( ! row.path(nonDefault).asBoolean(false) )
					continue;
				String run = row.path(runValue).asText("");
				cfg.append("- ").append(row.path(name).asText("")).append(" = ").append(run);
				if (defValue >= 0)
					cfg.append(" (default ").append(row.path(defValue).asText("")).append(")");
				if (cfgValue >= 0 && ! run.equals(row.path(cfgValue).asText(run)))
					cfg.append(" (configured ").append(row.path(cfgValue).asText("")).append(", pending a restart)");
				cfg.append("\n");
			}
			sb.append("Server configuration, the settings that are not at their default (sp_configure):\n");
			sb.append(cfg.length() > 0 ? cfg : "- all settings are at their default\n");
			sb.append("\n");
		}

		appendFirstRow(sb, "Host (sys.dm_os_host_info)",   texts.get("SqlServerHostInfo"), null);
		appendFirstRow(sb, "System (sys.dm_os_sys_info)",  texts.get("SqlServerSysInfo"),  SYS_INFO_COLUMNS);

		if (StringUtil.hasValue(dbname))
		{
			appendDatabaseRow(sb, "Database '" + dbname + "' (sp_helpdb)",           texts.get("SqlServerHelpDb"),       dbname, HELPDB_COLUMNS);
			appendDatabaseRow(sb, "Database '" + dbname + "' options (sys.databases)", texts.get("SqlServerSysDatabases"), dbname, SYS_DATABASES_COLUMNS);
			appendScopedConfig(sb, texts.get("SqlServerSysDatabases"), dbname);
		}

		return sb.toString().trim();
	}

	/** One line "label: col=value, col=value" from the first row of the first table in the text */
	private static void appendFirstRow(StringBuilder sb, String label, String text, List<String> onlyColumns)
	{
		List<Table> tables = parseTables(text);
		if (tables.isEmpty() || tables.get(0).rows.isEmpty())
			return;
		sb.append(label).append(": ").append(toNameValues(tables.get(0), tables.get(0).rows.get(0), onlyColumns)).append("\n\n");
	}

	/** One line from the first table (with a 'name' column) that has the database */
	private static void appendDatabaseRow(StringBuilder sb, String label, String text, String dbname, List<String> onlyColumns)
	{
		appendDatabaseRow(sb, label, text, "name", dbname, onlyColumns);
	}

	/** One line from the first table with the column 'nameColumn' that has the database */
	private static void appendDatabaseRow(StringBuilder sb, String label, String text, String nameColumn, String dbname, List<String> onlyColumns)
	{
		for (Table t : parseTables(text))
		{
			int nameCol = t.columns.indexOf(nameColumn);
			if (nameCol < 0)
				continue;
			for (List<String> row : t.rows)
			{
				if (dbname.equalsIgnoreCase(row.get(nameCol)))
				{
					sb.append(label).append(": ").append(toNameValues(t, row, onlyColumns)).append("\n\n");
					return;
				}
			}
		}
	}

	/** The database scoped configurations of the database (a table per database, from sp_MSforeachdb): only the non-default ones */
	private static void appendScopedConfig(StringBuilder sb, String text, String dbname)
	{
		StringBuilder cfg   = new StringBuilder();
		boolean       found = false;
		boolean       all   = false; // no is_value_default (before SQL Server 2017): every row is listed
		for (Table t : parseTables(text))
		{
			int db  = t.columns.indexOf("dbname");
			int nm  = t.columns.indexOf("name");
			int val = t.columns.indexOf("value");
			int sec = t.columns.indexOf("value_for_secondary");
			int def = t.columns.indexOf("is_value_default"); // SQL Server 2017 and later
			if (db < 0 || nm < 0 || val < 0)
				continue;
			if (def < 0)
				all = true;
			for (List<String> row : t.rows)
			{
				if ( ! dbname.equalsIgnoreCase(row.get(db)) )
					continue;
				found = true;
				if (def >= 0 && ! "false".equalsIgnoreCase(row.get(def)) && ! "0".equals(row.get(def)))
					continue;
				cfg.append("- ").append(row.get(nm)).append(" = ").append(row.get(val));
				if (sec >= 0 && StringUtil.hasValue(row.get(sec)))
					cfg.append(" (on a secondary: ").append(row.get(sec)).append(")");
				cfg.append("\n");
			}
		}
		if ( ! found )
			return;
		sb.append("Database '").append(dbname).append("' scoped configurations (sys.database_scoped_configurations)").append(all ? ":\n" : ", the ones that are not at their default:\n");
		sb.append(cfg.length() > 0 ? cfg : "- all are at their default\n");
		sb.append("\n");
	}

	/** "col=value, col=value" - empty and (NULL) values left out; digit groups ("2 331 648") joined */
	private static String toNameValues(Table t, List<String> row, List<String> onlyColumns)
	{
		List<String> parts = new ArrayList<>();
		List<String> cols  = onlyColumns != null ? onlyColumns : t.columns;
		for (String col : cols)
		{
			int i = t.columns.indexOf(col);
			if (i < 0)
				continue;
			String val = row.get(i);
			if (StringUtil.isNullOrBlank(val))
				continue;
			if (val.matches("-?\\d{1,3}([  ]\\d{3})+"))
				val = val.replaceAll("[  ]", "");
			parts.add(col + "=" + val);
		}
		return String.join(", ", parts);
	}

	/** One result set, as the Collector printed it */
	static class Table
	{
		final List<String>       columns = new ArrayList<>();
		final List<List<String>> rows    = new ArrayList<>();
	}

	/**
	 * The ASCII tables in a stored config text (other lines - print messages, "Rows N" - are skipped):
	 * <pre>
	 * +------+--------+
	 * |name  |db_size |
	 * +------+--------+
	 * |gs1   | 2816 MB|
	 * +------+--------+
	 * </pre>
	 * The '+' of the border line give the column bounds, so a '|' inside a value does no harm. "(NULL)" is returned as "".
	 */
	static List<Table> parseTables(String text)
	{
		List<Table> tables = new ArrayList<>();
		if (StringUtil.isNullOrBlank(text))
			return tables;

		String[] lines = text.split("\\r?\\n");
		for (int i = 0; i + 2 < lines.length; i++)
		{
			// border, header, border
			if ( ! isBorder(lines[i]) || ! lines[i+1].startsWith("|") || ! isBorder(lines[i+2]) )
				continue;

			List<Integer> bounds = new ArrayList<>();
			for (int p = 0; p < lines[i].length(); p++)
				if (lines[i].charAt(p) == '+')
					bounds.add(p);

			Table t = new Table();
			t.columns.addAll(cells(lines[i+1], bounds));
			int r = i + 3;
			for (; r < lines.length && lines[r].startsWith("|"); r++)
				t.rows.add(cells(lines[r], bounds));
			tables.add(t);
			i = r; // the bottom border
		}
		return tables;
	}

	private static boolean isBorder(String line)
	{
		return line.startsWith("+-") && line.replaceAll("[+\\-]", "").trim().isEmpty();
	}

	private static List<String> cells(String line, List<Integer> bounds)
	{
		List<String> cells = new ArrayList<>();
		for (int c = 0; c + 1 < bounds.size(); c++)
		{
			int from = bounds.get(c) + 1;
			int to   = Math.min(bounds.get(c + 1), line.length());
			String cell = from < to ? line.substring(from, to).trim() : "";
			cells.add("(NULL)".equals(cell) ? "" : cell);
		}
		return cells;
	}
}
