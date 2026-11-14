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
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
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

import java.lang.invoke.MethodHandles;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.dbxtune.pcs.report.content.IndexAnalysisHtml;
import com.dbxtune.sql.conn.DbxConnection;
import com.dbxtune.utils.Configuration;
import com.dbxtune.utils.DbUtils;
import com.dbxtune.utils.StringUtil;

/**
 * Is a table vacuumed (and analyzed) often enough? Used by the Table Information (PostgresAbstract) and by "Top Dead Rows" (PostgresTopDeadRows).
 * <p>
 * From the recording: the table's samples in CmPgTables_abs (every sample of the day), its reloptions, and the server settings (MonSessionDbmsConfig).<br>
 * The trigger points are computed as Postgres does it, with n_live_tup in place of pg_class.reltuples:
 * <ul>
 *   <li>vacuum:  autovacuum_vacuum_threshold + autovacuum_vacuum_scale_factor * rows (18: at most autovacuum_vacuum_max_threshold)</li>
 *   <li>insert:  autovacuum_vacuum_insert_threshold + autovacuum_vacuum_insert_scale_factor * rows (13 and above)</li>
 *   <li>analyze: autovacuum_analyze_threshold + autovacuum_analyze_scale_factor * rows</li>
 * </ul>
 * The table's reloptions override the server settings.
 * <p>
 * Nothing in here changes anything: the script is a suggestion, with its rollback.
 */
public class PostgresAutovacuumAdvice
{
	private static final Logger _logger = LogManager.getLogger(MethodHandles.lookup().lookupClass());

	public static final String PROPKEY_pctSamplesAboveTrigger = "DailySummaryReport.Postgres.Autovacuum.pctSamplesAboveTrigger";
	public static final int    DEFAULT_pctSamplesAboveTrigger = 25;

	public static final String PROPKEY_bigTableRows           = "DailySummaryReport.Postgres.Autovacuum.bigTableRows";
	public static final long   DEFAULT_bigTableRows           = 1_000_000;

	public static final String PROPKEY_freezeWarnPct          = "DailySummaryReport.Postgres.Autovacuum.freezeWarnPct";
	public static final int    DEFAULT_freezeWarnPct          = 50;

	//--------------------------------------------------------------------------------
	// The settings that are used, and where they come from
	//--------------------------------------------------------------------------------
	/** One autovacuum setting: the table's value (reloptions, null if not set), the server's value, and the one that is used */
	public static class Setting
	{
		public String name;        // the reloption name, for example 'autovacuum_vacuum_scale_factor'
		public String tableValue;  // null = not set on the table
		public String serverValue; // null = the server do not have it (an older version)
		public String effective()  { return tableValue != null ? tableValue : serverValue; }
	}

	/** reloption name -> server setting name (the same name, except 'autovacuum_enabled') */
	private static final String[][] SETTING_NAMES = {
			{ "autovacuum_enabled",                    "autovacuum" },
			{ "autovacuum_vacuum_threshold",           "autovacuum_vacuum_threshold" },
			{ "autovacuum_vacuum_scale_factor",        "autovacuum_vacuum_scale_factor" },
			{ "autovacuum_vacuum_max_threshold",       "autovacuum_vacuum_max_threshold" },        // 18
			{ "autovacuum_vacuum_insert_threshold",    "autovacuum_vacuum_insert_threshold" },     // 13
			{ "autovacuum_vacuum_insert_scale_factor", "autovacuum_vacuum_insert_scale_factor" },  // 13
			{ "autovacuum_analyze_threshold",          "autovacuum_analyze_threshold" },
			{ "autovacuum_analyze_scale_factor",       "autovacuum_analyze_scale_factor" },
			{ "autovacuum_vacuum_cost_limit",          "autovacuum_vacuum_cost_limit" },
			{ "autovacuum_vacuum_cost_delay",          "autovacuum_vacuum_cost_delay" },
			{ "autovacuum_freeze_max_age",             "autovacuum_freeze_max_age" },
			{ "autovacuum_multixact_freeze_max_age",   "autovacuum_multixact_freeze_max_age" },
	};

	public Map<String, Setting> settings = new LinkedHashMap<>();

	//--------------------------------------------------------------------------------
	// What the recording says about the table (over the whole day)
	//--------------------------------------------------------------------------------
	public String    schemaName;
	public String    tableName;

	public long      samples;               // samples of the table
	public Timestamp firstSample;
	public Timestamp lastSample;
	public long      liveRows;              // n_live_tup in the newest sample
	public long      maxDeadRows;
	public long      minDeadRows;
	public double    maxDeadPct;            // max of n_dead_tup / n_live_tup * 100
	public long      samplesAboveVacuum;    // n_dead_tup above the vacuum trigger
	public long      samplesAboveInsert  = -1; // n_ins_since_vacuum above the insert trigger (-1 = not available, before 13)
	public long      maxInsSinceVacuum   = -1;
	public long      samplesAboveAnalyze;   // n_mod_since_analyze above the analyze trigger
	public long      maxModSinceAnalyze;
	public long      frozenxidAge        = -1; // newest sample (-1 = not recorded)
	public long      minmxidAge          = -1;
	public List<Timestamp> autovacuums   = new ArrayList<>(); // the autovacuums that ended during the recording
	public long      longestGapSec       = -1; // the longest time without an autovacuum, in the recording
	public double    totalAutovacuumTimeMs = -1; // 18: the time autovacuum spent on the table during the recording (-1 = not available)

	/** Why there is no analysis (the table is not in CmPgTables, ...). null when there is one */
	public String    notAnalyzed;

	//--------------------------------------------------------------------------------
	// The result
	//--------------------------------------------------------------------------------
	public static class Finding
	{
		public String rule;    // R0..R6
		public String verdict; // short text
		public String why;     // plain text, the numbers
		public Finding(String rule, String verdict, String why) { this.rule = rule; this.verdict = verdict; this.why = why; }
	}
	public List<Finding>       findings   = new ArrayList<>();
	/** reloption -> suggested value (the ALTER TABLE ... SET) */
	public Map<String, String> suggested  = new LinkedHashMap<>();
	/** Statements to run by hand (VACUUM FREEZE) */
	public List<String>        manualSql  = new ArrayList<>();

	//================================================================================
	// Read from the recording
	//================================================================================

	/** The newest server settings that matter here: name -> value. Read once per report. */
	public static Map<String, String> readServerSettings(DbxConnection conn)
	{
		Map<String, String> map = new LinkedHashMap<>();
		String sql = conn.quotifySqlString(""
				+ "select [ParameterName], [CurrentValue] \n"
				+ "from [MonSessionDbmsConfig] \n"
				+ "where [SessionStartTime] = (select max([SessionStartTime]) from [MonSessionDbmsConfig]) \n"
				+ "  and ([ParameterName] like 'autovacuum%' or [ParameterName] in ('vacuum_cost_limit', 'vacuum_cost_delay')) \n");
		try (Statement stmnt = conn.createStatement(); ResultSet rs = stmnt.executeQuery(sql))
		{
			while (rs.next())
				map.put(rs.getString(1), rs.getString(2));
		}
		catch (SQLException ex)
		{
			_logger.info("Problems reading the autovacuum settings from MonSessionDbmsConfig, the autovacuum analysis uses the Postgres defaults. Caught: " + ex);
		}
		return map;
	}

	/**
	 * Read the table's samples and analyze them.
	 * @param serverSettings from {@link #readServerSettings(DbxConnection)}
	 */
	public static PostgresAutovacuumAdvice create(DbxConnection conn, Map<String, String> serverSettings, String dbname, String schemaName, String tableName)
	{
		PostgresAutovacuumAdvice a = new PostgresAutovacuumAdvice();
		a.schemaName = schemaName;
		a.tableName  = tableName;

		String where = ""
				+ "where [dbname]     = " + DbUtils.safeStr(dbname    ) + " \n"
				+ "  and [schemaname] = " + DbUtils.safeStr(schemaName) + " \n"
				+ "  and [relname]    = " + DbUtils.safeStr(tableName ) + " \n";

		try (Statement stmnt = conn.createStatement())
		{
			stmnt.setQueryTimeout(30);

			// What columns do we have (they depend on the Postgres version, and on when the recording was made)
			Set<String> cols = new HashSet<>();
			try (ResultSet rs = stmnt.executeQuery(conn.quotifySqlString("select * from [CmPgTables_abs] where 1 = 2")))
			{
				ResultSetMetaData md = rs.getMetaData();
				for (int c = 1; c <= md.getColumnCount(); c++)
					cols.add(md.getColumnLabel(c).toLowerCase());
			}

			// The newest sample: reloptions (they decide the settings), rows, freeze age
			String newestSql = "select [reloptions], [n_live_tup]"
					+ (cols.contains("frozenxid_age") ? ", [frozenxid_age], [minmxid_age]" : ", -1, -1")
					+ " from [CmPgTables_abs] \n" + where + "order by [SessionSampleTime] desc";
			String reloptions = null;
			boolean found = false;
			try (ResultSet rs = stmnt.executeQuery(conn.quotifySqlString(newestSql)))
			{
				if (rs.next())
				{
					found        = true;
					reloptions   = rs.getString(1);
					a.liveRows   = rs.getLong(2);
					a.frozenxidAge = rs.getObject(3) == null ? -1 : rs.getLong(3);
					a.minmxidAge   = rs.getObject(4) == null ? -1 : rs.getLong(4);
				}
			}
			if ( ! found )
			{
				a.notAnalyzed = "The table is not in the recorded table statistics (CmPgTables): it was not sampled (no reads or writes in the recording, or the Performance Counter was not enabled).";
				return a;
			}

			a.setSettings(serverSettings, parseReloptions(reloptions));

			// The whole day, with the trigger points computed per sample (n_live_tup changes)
			String hasIns = cols.contains("n_ins_since_vacuum") && a.has("autovacuum_vacuum_insert_threshold") ? "" : null;
			String aggSql = ""
					+ "select count(*) \n"
					+ "      ,min([SessionSampleTime]), max([SessionSampleTime]) \n"
					+ "      ,max([n_dead_tup]), min([n_dead_tup]) \n"
					+ "      ,max(case when [n_live_tup] > 0 then [n_dead_tup] * 100.0 / [n_live_tup] else 0 end) \n"
					+ "      ,sum(case when [n_dead_tup] > " + a.vacuumTriggerSql("[n_live_tup]") + " then 1 else 0 end) \n"
					+ "      ,sum(case when [n_mod_since_analyze] > " + a.analyzeTriggerSql("[n_live_tup]") + " then 1 else 0 end) \n"
					+ "      ,max([n_mod_since_analyze]) \n"
					+ (hasIns == null ? "      ,-1, -1 \n"
					                  : "      ,sum(case when [n_ins_since_vacuum] > " + a.insertTriggerSql("[n_live_tup]") + " then 1 else 0 end), max([n_ins_since_vacuum]) \n")
					+ (cols.contains("total_autovacuum_time") ? "      ,max([total_autovacuum_time]) - min([total_autovacuum_time]) \n" : "      ,-1 \n")
					+ "from [CmPgTables_abs] \n" + where;
			try (ResultSet rs = stmnt.executeQuery(conn.quotifySqlString(aggSql)))
			{
				if (rs.next())
				{
					a.samples             = rs.getLong(1);
					a.firstSample         = rs.getTimestamp(2);
					a.lastSample          = rs.getTimestamp(3);
					a.maxDeadRows         = rs.getLong(4);
					a.minDeadRows         = rs.getLong(5);
					a.maxDeadPct          = rs.getDouble(6);
					a.samplesAboveVacuum  = rs.getLong(7);
					a.samplesAboveAnalyze = rs.getLong(8);
					a.maxModSinceAnalyze  = rs.getLong(9);
					a.samplesAboveInsert  = rs.getLong(10);
					a.maxInsSinceVacuum   = rs.getLong(11);
					a.totalAutovacuumTimeMs = rs.getDouble(12);
				}
			}

			// The autovacuums that ended during the recording (last_autovacuum is the end time of the newest one)
			String avSql = "select distinct [last_autovacuum] from [CmPgTables_abs] \n" + where
					+ "  and [last_autovacuum] >= (select min([SessionSampleTime]) from [CmPgTables_abs] " + where.replace("\n", " ") + ") \n"
					+ "order by 1";
			try (ResultSet rs = stmnt.executeQuery(conn.quotifySqlString(avSql)))
			{
				while (rs.next())
				{
					Timestamp ts = rs.getTimestamp(1);
					if (ts != null)
						a.autovacuums.add(ts);
				}
			}
		}
		catch (SQLException ex)
		{
			_logger.warn("Problems reading the autovacuum information for '" + dbname + "." + schemaName + "." + tableName + "'. Caught: " + ex);
			a.notAnalyzed = "Problems reading the recording: " + ex.getMessage();
			return a;
		}

		a.longestGapSec = longestGapSec(a.firstSample, a.lastSample, a.autovacuums);
		a.analyze(Configuration.getCombinedConfiguration().getIntProperty (PROPKEY_pctSamplesAboveTrigger, DEFAULT_pctSamplesAboveTrigger),
		          Configuration.getCombinedConfiguration().getLongProperty(PROPKEY_bigTableRows,           DEFAULT_bigTableRows),
		          Configuration.getCombinedConfiguration().getIntProperty (PROPKEY_freezeWarnPct,          DEFAULT_freezeWarnPct));
		return a;
	}

	/** The longest time without an autovacuum in the recording: from the start, between them, and to the end */
	static long longestGapSec(Timestamp first, Timestamp last, List<Timestamp> autovacuums)
	{
		if (first == null || last == null)
			return -1;
		long prev = first.getTime();
		long max  = 0;
		for (Timestamp ts : autovacuums)
		{
			max  = Math.max(max, ts.getTime() - prev);
			prev = ts.getTime();
		}
		max = Math.max(max, last.getTime() - prev);
		return max / 1000;
	}

	/** 'a=1, b=2' (array_to_string(reloptions, ', ')) -> map */
	static Map<String, String> parseReloptions(String reloptions)
	{
		Map<String, String> map = new LinkedHashMap<>();
		if (StringUtil.isNullOrBlank(reloptions))
			return map;
		for (String opt : reloptions.split(","))
		{
			int eq = opt.indexOf('=');
			if (eq > 0)
				map.put(opt.substring(0, eq).trim(), opt.substring(eq + 1).trim());
		}
		return map;
	}

	/** Set the settings (server values + the table's reloptions). Postgres defaults when the server value was not recorded. */
	void setSettings(Map<String, String> serverSettings, Map<String, String> tableOptions)
	{
		for (String[] n : SETTING_NAMES)
		{
			Setting s = new Setting();
			s.name        = n[0];
			s.tableValue  = tableOptions.get(n[0]);
			s.serverValue = serverSettings.get(n[1]);
			settings.put(n[0], s);
		}
		// Postgres defaults (when the server value is missing, for example an old recording without MonSessionDbmsConfig)
		defaultIfNull("autovacuum_enabled",             "on");
		defaultIfNull("autovacuum_vacuum_threshold",    "50");
		defaultIfNull("autovacuum_vacuum_scale_factor", "0.2");
		defaultIfNull("autovacuum_analyze_threshold",   "50");
		defaultIfNull("autovacuum_analyze_scale_factor","0.1");
		defaultIfNull("autovacuum_freeze_max_age",      "200000000");
		defaultIfNull("autovacuum_multixact_freeze_max_age", "400000000");
		// autovacuum_vacuum_cost_limit -1 = vacuum_cost_limit
		Setting cl = settings.get("autovacuum_vacuum_cost_limit");
		if (cl.serverValue == null || "-1".equals(cl.serverValue.trim()))
			cl.serverValue = serverSettings.getOrDefault("vacuum_cost_limit", "200");
	}
	private void defaultIfNull(String name, String value)
	{
		Setting s = settings.get(name);
		if (s.serverValue == null)
			s.serverValue = value;
	}

	/** The server has the setting (the version supports it), or the table has it */
	boolean has(String name)
	{
		Setting s = settings.get(name);
		return s != null && s.effective() != null;
	}
	double num(String name, double def)
	{
		Setting s = settings.get(name);
		if (s == null || s.effective() == null)
			return def;
		try { return Double.parseDouble(s.effective().trim()); }
		catch (NumberFormatException ex) { return def; }
	}
	boolean isEnabled()
	{
		String v = settings.get("autovacuum_enabled").effective();
		return v == null || ! (v.equalsIgnoreCase("false") || v.equalsIgnoreCase("off") || v.equals("0"));
	}

	public double vacuumTrigger(long rows)
	{
		double t = num("autovacuum_vacuum_threshold", 50) + num("autovacuum_vacuum_scale_factor", 0.2) * rows;
		double max = num("autovacuum_vacuum_max_threshold", -1); // 18: -1 = no max
		return max > 0 ? Math.min(t, max) : t;
	}
	public double insertTrigger (long rows) { return num("autovacuum_vacuum_insert_threshold", 1000) + num("autovacuum_vacuum_insert_scale_factor", 0.2) * rows; }
	public double analyzeTrigger(long rows) { return num("autovacuum_analyze_threshold", 50) + num("autovacuum_analyze_scale_factor", 0.1) * rows; }

	private String vacuumTriggerSql(String rowsCol)
	{
		String t = num("autovacuum_vacuum_threshold", 50) + " + " + num("autovacuum_vacuum_scale_factor", 0.2) + " * " + rowsCol;
		double max = num("autovacuum_vacuum_max_threshold", -1);
		return max > 0 ? "least(" + t + ", " + max + ")" : "(" + t + ")";
	}
	private String insertTriggerSql (String rowsCol) { return "(" + num("autovacuum_vacuum_insert_threshold", 1000) + " + " + num("autovacuum_vacuum_insert_scale_factor", 0.2) + " * " + rowsCol + ")"; }
	private String analyzeTriggerSql(String rowsCol) { return "(" + num("autovacuum_analyze_threshold", 50) + " + " + num("autovacuum_analyze_scale_factor", 0.1) + " * " + rowsCol + ")"; }

	//================================================================================
	// The rules
	//================================================================================

	/** The scale factor to suggest for a big table: the trigger point at ~2% / 1% / 0.5% of the rows */
	static String suggestedScaleFactor(long rows)
	{
		if (rows > 100_000_000) return "0.005";
		if (rows >  10_000_000) return "0.01";
		return "0.02";
	}

	void analyze(int pctSamplesAboveTrigger, long bigTableRows, int freezeWarnPct)
	{
		NumberFormat nf = NumberFormat.getInstance(Locale.US);
		long   autovacs     = autovacuums.size();
		double abovePctVac  = samples > 0 ? samplesAboveVacuum  * 100.0 / samples : 0;
		double abovePctAna  = samples > 0 ? samplesAboveAnalyze * 100.0 / samples : 0;
		boolean big         = liveRows >= bigTableRows;
		String  rowsTxt     = nf.format(liveRows) + " rows";

		// R0: autovacuum is off for the table
		if ( ! isEnabled() )
		{
			findings.add(new Finding("R0", "autovacuum is OFF for this table",
					"The table has autovacuum_enabled=" + settings.get("autovacuum_enabled").effective() + ": dead rows are only removed by a manual VACUUM "
					+ "(the highest number of dead rows in the recording was " + nf.format(maxDeadRows) + "). A forced (anti-wraparound) vacuum still runs when the table gets old enough."));
			suggested.put("autovacuum_enabled", "on");
		}
		else
		{
			// R1: above the trigger most of the time, and autovacuum never came
			if (abovePctVac >= pctSamplesAboveTrigger && autovacs == 0)
			{
				findings.add(new Finding("R1", "autovacuum is not keeping up",
						"The dead rows were above the point where autovacuum should start (" + nf.format(Math.round(vacuumTrigger(liveRows))) + ") in " + Math.round(abovePctVac) + "% of the samples, "
						+ "and autovacuum did not vacuum the table during the recording. All autovacuum workers may be busy with other tables (autovacuum_max_workers), "
						+ "or each of them is slowed down too much (autovacuum_vacuum_cost_limit / cost_delay). A higher cost limit for this table lets autovacuum work faster on it."));
				if (num("autovacuum_vacuum_cost_limit", 200) < 1000)
					suggested.put("autovacuum_vacuum_cost_limit", "1000");
			}

			// R4: autovacuum runs, but the dead rows do not go away: something holds the xmin horizon
			if (autovacs >= 2 && minDeadRows > vacuumTrigger(liveRows))
			{
				findings.add(new Finding("R4", "dead rows are not removed (cleanup is blocked)",
						"Autovacuum ran " + autovacs + " times, but the dead rows never went below " + nf.format(Math.round(vacuumTrigger(liveRows))) + " (lowest: " + nf.format(minDeadRows) + "). "
						+ "Vacuum can only remove rows that no transaction can see any more: a long running (or 'idle in transaction') session, a prepared transaction, "
						+ "a replication slot or a standby with hot_standby_feedback holds them. Look at 'oldest transaction' in the Summary graphs and at the replication slots. "
						+ "Changing the autovacuum settings does not help here."));
			}

			// R2: a big table with a trigger point that is too high
			if (big && num("autovacuum_vacuum_scale_factor", 0.2) >= 0.1 && maxDeadPct >= 10)
			{
				findings.add(new Finding("R2", "the vacuum trigger point is too high for a big table",
						"With autovacuum_vacuum_scale_factor=" + settings.get("autovacuum_vacuum_scale_factor").effective() + " on " + rowsTxt + ", autovacuum starts first at "
						+ nf.format(Math.round(vacuumTrigger(liveRows))) + " dead rows; the dead rows reached " + Math.round(maxDeadPct) + "% of the table. "
						+ "A lower scale factor vacuums more often, in smaller (faster) runs, and keeps the table and its indexes smaller."));
				suggested.put("autovacuum_vacuum_scale_factor", suggestedScaleFactor(liveRows));
				suggested.put("autovacuum_vacuum_threshold",    "1000");
			}

			// R3: an append-only big table: inserts trigger vacuum too late
			if (big && samplesAboveInsert >= 0 && maxInsSinceVacuum >= 0
					&& num("autovacuum_vacuum_insert_scale_factor", 0.2) >= 0.1 && maxInsSinceVacuum >= liveRows * 0.1 && maxDeadPct < 2)
			{
				findings.add(new Finding("R3", "inserts trigger vacuum too late (append-only table)",
						"The table mostly gets inserts (up to " + nf.format(maxInsSinceVacuum) + " rows since the last vacuum, very few dead rows). "
						+ "Vacuum of an append-only table sets the visibility map (index-only scans) and freezes rows, which avoids one big anti-wraparound vacuum later. "
						+ "With autovacuum_vacuum_insert_scale_factor=" + settings.get("autovacuum_vacuum_insert_scale_factor").effective() + " that starts first after "
						+ nf.format(Math.round(insertTrigger(liveRows))) + " inserts (the calculation is an approximation from Postgres 18, which counts the unfrozen part of the table)."));
				suggested.put("autovacuum_vacuum_insert_scale_factor", suggestedScaleFactor(liveRows));
			}

			// R5: statistics are often stale on a big table
			if (big && num("autovacuum_analyze_scale_factor", 0.1) >= 0.1 && abovePctAna >= pctSamplesAboveTrigger)
			{
				findings.add(new Finding("R5", "the statistics are often stale",
						"The rows changed since the last analyze were above the point where autovacuum analyzes the table (" + nf.format(Math.round(analyzeTrigger(liveRows))) + ") in "
						+ Math.round(abovePctAna) + "% of the samples (with autovacuum_analyze_scale_factor=" + settings.get("autovacuum_analyze_scale_factor").effective() + " on " + rowsTxt + "). "
						+ "Stale statistics can give bad execution plans, for example for new values in date or id columns."));
				suggested.put("autovacuum_analyze_scale_factor", suggestedScaleFactor(liveRows));
			}
		}

		// R6: close to a forced (anti-wraparound) vacuum
		double xidMax  = num("autovacuum_freeze_max_age", 200_000_000);
		double mxidMax = num("autovacuum_multixact_freeze_max_age", 400_000_000);
		double xidPct  = frozenxidAge >= 0 && xidMax  > 0 ? frozenxidAge * 100.0 / xidMax  : -1;
		double mxidPct = minmxidAge   >= 0 && mxidMax > 0 ? minmxidAge   * 100.0 / mxidMax : -1;
		double agePct  = Math.max(xidPct, mxidPct);
		if (agePct >= freezeWarnPct)
		{
			boolean soon = agePct >= 80;
			findings.add(new Finding("R6", soon ? "a forced (anti-wraparound) vacuum is near (or running)" : "the table is getting old (freeze)",
					"The oldest unfrozen transaction id of the table is " + nf.format(frozenxidAge) + " transactions old (" + Math.round(Math.max(xidPct, 0)) + "% of autovacuum_freeze_max_age=" + nf.format(Math.round(xidMax)) + ")"
					+ (minmxidAge >= 0 ? ", the multixact age is " + nf.format(minmxidAge) + " (" + Math.round(Math.max(mxidPct, 0)) + "% of autovacuum_multixact_freeze_max_age)" : "") + ". "
					+ "At 100% Postgres starts a forced vacuum of the whole table, which can not be skipped and can take long on a big table. "
					+ (soon ? "Do it yourself in a quiet period, before that happens." : "A VACUUM (FREEZE) in a quiet period avoids that it happens at a bad time.")));
			manualSql.add("VACUUM (FREEZE, VERBOSE) " + qualifiedName() + ";");
		}
	}

	//================================================================================
	// Output
	//================================================================================

	/** "schema"."table" (quoted only when needed) */
	public String qualifiedName()
	{
		return quoteIdent(schemaName) + "." + quoteIdent(tableName);
	}
	static String quoteIdent(String name)
	{
		if (name != null && name.matches("[a-z_][a-z0-9_$]*"))
			return name;
		return "\"" + (name == null ? "" : name.replace("\"", "\"\"")) + "\"";
	}

	/** The suggested change and its rollback: the table's old values are SET back, the rest RESET */
	public String getScript()
	{
		StringBuilder sb = new StringBuilder();
		if ( ! suggested.isEmpty() )
		{
			List<String> set = new ArrayList<>();
			List<String> setBack = new ArrayList<>();
			List<String> reset = new ArrayList<>();
			for (Map.Entry<String, String> e : suggested.entrySet())
			{
				set.add(e.getKey() + " = " + e.getValue());
				Setting s = settings.get(e.getKey());
				if (s != null && s.tableValue != null)
					setBack.add(e.getKey() + " = " + s.tableValue);
				else
					reset.add(e.getKey());
			}
			sb.append("-- CHANGE (it takes effect at once, no restart; autovacuum uses it from its next round)\n");
			sb.append("ALTER TABLE ").append(qualifiedName()).append(" SET (").append(String.join(", ", set)).append(");\n");
			sb.append("\n-- ROLLBACK (back to the values before the change)\n");
			if ( ! setBack.isEmpty() )
				sb.append("ALTER TABLE ").append(qualifiedName()).append(" SET (").append(String.join(", ", setBack)).append(");\n");
			if ( ! reset.isEmpty() )
				sb.append("ALTER TABLE ").append(qualifiedName()).append(" RESET (").append(String.join(", ", reset)).append(");\n");
		}
		if ( ! manualSql.isEmpty() )
		{
			if (sb.length() > 0)
				sb.append("\n");
			sb.append("-- RUN BY HAND, in a quiet period (it reads the whole table)\n");
			for (String s : manualSql)
				sb.append(s).append("\n");
		}
		return sb.toString();
	}

	/** One line: "OK" or the verdicts (for the Top Dead Rows column and the plain text) */
	public String getShortVerdict()
	{
		if (notAnalyzed != null)
			return "not analyzed";
		if (findings.isEmpty())
			return "OK";
		List<String> list = new ArrayList<>();
		for (Finding f : findings)
			list.add(f.verdict);
		return String.join("; ", list);
	}

	public boolean hasFindings()
	{
		return notAnalyzed == null && ! findings.isEmpty();
	}

	/** The box in Table Information. No JavaScript (it also goes into the mail): a collapsed &lt;details&gt; like the Index Analysis */
	public String toHtml()
	{
		NumberFormat nf = NumberFormat.getInstance(Locale.US);
		StringBuilder sb = new StringBuilder();
		sb.append("<!--[if !mso]><!--> \n"); // BEGIN: IGNORE THIS SECTION FOR OUTLOOK
		sb.append("<br> \n");
		sb.append("<details class='dbx-autovacuum' style='max-width: 1200px;'> \n");

		String verdict = getShortVerdict();
		sb.append("<summary>Show/Hide: Autovacuum - " + (hasFindings() ? "<b>" + IndexAnalysisHtml.esc(verdict) + "</b>" : IndexAnalysisHtml.esc(verdict)) + "</summary> \n");

		if (notAnalyzed != null)
		{
			sb.append("<p>" + IndexAnalysisHtml.esc(notAnalyzed) + "</p> \n");
		}
		else
		{
			// What the recording says
			sb.append("<p>From " + samples + " samples, " + IndexAnalysisHtml.esc(firstSample) + " to " + IndexAnalysisHtml.esc(lastSample) + ". These are suggestions: nothing has been changed.</p> \n");
			sb.append("<table class='dbx-autovacuum-numbers'> \n");
			sb.append("<thead><tr><th>What</th><th>Starts at</th><th>Highest in the recording</th><th>Samples above the start point</th></tr></thead> \n<tbody> \n");
			sb.append("<tr><td>Vacuum (dead rows)</td><td>" + nf.format(Math.round(vacuumTrigger(liveRows))) + "</td><td>" + nf.format(maxDeadRows) + " (" + Math.round(maxDeadPct) + "% of " + nf.format(liveRows) + " rows)</td><td>" + pctTxt(samplesAboveVacuum) + "</td></tr> \n");
			if (samplesAboveInsert >= 0)
				sb.append("<tr><td>Vacuum (inserts)</td><td>" + nf.format(Math.round(insertTrigger(liveRows))) + "</td><td>" + nf.format(maxInsSinceVacuum) + "</td><td>" + pctTxt(samplesAboveInsert) + "</td></tr> \n");
			sb.append("<tr><td>Analyze (changed rows)</td><td>" + nf.format(Math.round(analyzeTrigger(liveRows))) + "</td><td>" + nf.format(maxModSinceAnalyze) + "</td><td>" + pctTxt(samplesAboveAnalyze) + "</td></tr> \n");
			sb.append("</tbody></table> \n");

			sb.append("<ul> \n");
			sb.append("<li>Autovacuums in the recording: " + autovacuums.size()
					+ (autovacuums.isEmpty() ? "" : ", the last " + IndexAnalysisHtml.esc(autovacuums.get(autovacuums.size() - 1)))
					+ (longestGapSec >= 0 ? ", longest time without one: " + formatSec(longestGapSec) : "")
					+ (totalAutovacuumTimeMs > 0 ? ", time spent by autovacuum: " + formatSec(Math.round(totalAutovacuumTimeMs / 1000)) : "") + "</li> \n");
			if (frozenxidAge >= 0)
				sb.append("<li>Freeze age: " + nf.format(frozenxidAge) + " transactions (forced vacuum at " + nf.format(Math.round(num("autovacuum_freeze_max_age", 200_000_000))) + ")"
						+ (minmxidAge >= 0 ? ", multixact age " + nf.format(minmxidAge) : "") + "</li> \n");
			sb.append("</ul> \n");

			// The settings
			sb.append("<table class='dbx-autovacuum-settings'> \n");
			sb.append("<thead><tr><th>Setting</th><th>Table (reloptions)</th><th>Server</th><th>Used</th></tr></thead> \n<tbody> \n");
			for (Setting s : settings.values())
			{
				if (s.effective() == null)
					continue; // not in this version
				sb.append("<tr><td><code>" + IndexAnalysisHtml.esc(s.name) + "</code></td><td>" + (s.tableValue == null ? "" : "<b>" + IndexAnalysisHtml.esc(s.tableValue) + "</b>")
						+ "</td><td>" + IndexAnalysisHtml.esc(s.serverValue) + "</td><td>" + IndexAnalysisHtml.esc(s.effective()) + "</td></tr> \n");
			}
			sb.append("</tbody></table> \n");

			// The findings
			for (Finding f : findings)
			{
				sb.append("<hr style='border-top: 1px dashed #bbb;'> \n");
				sb.append("<p><b>" + IndexAnalysisHtml.esc(f.verdict) + "</b></p> \n");
				sb.append("<p>Why: " + IndexAnalysisHtml.esc(f.why) + "</p> \n");
			}

			String script = getScript();
			if ( ! script.isEmpty() )
			{
				sb.append("<details class='dbx-autovacuum-script'><summary>Show/Hide: Change and Rollback</summary> \n");
				sb.append("<pre style='white-space: pre-wrap;'><code class='language-sql'>").append(IndexAnalysisHtml.esc(script)).append("</code></pre> \n");
				sb.append("</details> \n");
			}
		}

		sb.append("</details> \n");
		sb.append("<!--<![endif]-->    \n"); // END: IGNORE THIS SECTION FOR OUTLOOK
		return sb.toString();
	}

	private String pctTxt(long n)
	{
		return samples > 0 ? n + " of " + samples + " (" + Math.round(n * 100.0 / samples) + "%)" : "";
	}
	static String formatSec(long sec)
	{
		if (sec < 120)   return sec + " s";
		if (sec < 7200)  return Math.round(sec / 60.0) + " min";
		return String.format(Locale.US, "%.1f h", sec / 3600.0);
	}
}
