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
 * The texts are stored as the Collector printed the result sets: ASCII tables (see {@link #parseTables(String)}).
 * Kept small on purpose: it is sent with every request.
 */
public class LlmServerContextBuilder
{
	/** Send the server configuration and hardware with the prompt (SQL Server, when the Collector is known). */
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
		for (Table t : parseTables(text))
		{
			int nameCol = t.columns.indexOf("name");
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
