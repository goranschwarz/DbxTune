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
package com.dbxtune.loadgen.scenario;

import java.io.PrintStream;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import com.dbxtune.loadgen.LgSession;
import com.dbxtune.loadgen.Vendor;

/**
 * A piece of workload.
 * <ul>
 *   <li>OLTP / OLAP / BATCH: {@link #execute} is called over and over by the worker threads, picked by weight.
 *       OLTP scenarios run on the OLTP workers, OLAP and BATCH on the OLAP workers.</li>
 *   <li>PROBLEM: {@link #runProblem} is called every N minutes (with a random start offset).
 *       It opens its own sessions and writes one "problem" event to the journal, with what DbxTune should show.</li>
 * </ul>
 * The SQL is written in-line in each scenario; where the vendors differ it is picked with {@link #pick}.
 * Every statement starts with a comment naming the scenario, so it can be found in DbxTune's SQL text.
 */
public abstract class Scenario
{
	public enum Kind { OLTP, OLAP, BATCH, PROBLEM }

	private final String _name;
	private final Kind   _kind;
	private final int    _defaultWeight;
	private final int    _defaultEveryMinutes;
	private final String _description;

	protected Scenario(String name, Kind kind, int defaultWeightOrEveryMinutes, String description)
	{
		_name                = name;
		_kind                = kind;
		_defaultWeight       = kind == Kind.PROBLEM ? 0 : defaultWeightOrEveryMinutes;
		_defaultEveryMinutes = kind == Kind.PROBLEM ? defaultWeightOrEveryMinutes : 0;
		_description         = description;
	}

	public String getName()                { return _name; }
	public Kind   getKind()                { return _kind; }
	public int    getDefaultWeight()       { return _defaultWeight; }
	public int    getDefaultEveryMinutes() { return _defaultEveryMinutes; }
	public String getDescription()         { return _description; }

	/** The comment every statement of this scenario starts with */
	protected String tag() { return "/* dbxt:" + _name + " */ "; }

	/**
	 * How long a problem stays active: the setting <code>scenario.&lt;name&gt;.&lt;key&gt;</code> (default <code>defaultSec</code>)
	 * + <code>problem.extraSeconds</code> (default 35: one more DbxTune sample of 30 s + ~5 s to take the sample) + 0-9 s random.
	 * So a problem never stops at a round number, and it is still active in the DbxTune sample after the one that first saw it.
	 */
	protected int activeSeconds(RunContext ctx, String key, int defaultSec)
	{
		return ctx.getLoadGen().getScenarioIntProp(this, key, defaultSec)
				+ ctx.getLoadGen().getIntProp("problem.extraSeconds", 35)
				+ ctx.rnd().nextInt(10);
	}

	/** OLTP/OLAP/BATCH: one unit of work on the worker's session */
	public void execute(RunContext ctx, LgSession s)
	throws Exception
	{
		throw new UnsupportedOperationException(_name + " is not an OLTP/OLAP/BATCH scenario");
	}

	/** PROBLEM: one event; open sessions with ctx.openSession(), describe what happened in 'ev' */
	public void runProblem(RunContext ctx, ProblemEvent ev)
	throws Exception
	{
		throw new UnsupportedOperationException(_name + " is not a PROBLEM scenario");
	}

	/** PROBLEM: what DbxTune should show (CMs, alarms, a note). Written to the journal with each event. */
	public Map<String, Object> getExpected(Vendor v)
	{
		return null;
	}

	/** Helper for {@link #getExpected} */
	protected static Map<String, Object> expected(List<String> cms, List<String> alarms, String note)
	{
		return expected(cms, alarms, null, note);
	}

	/**
	 * @param graphs DbxCentral graphs that should go up during the problem, checked by 'compare':
	 *               "CmName.graphName" (its first series), "CmName.graphName:series label", or "CmName.graphName:*" (sum of all series)
	 */
	/**
	 * Add row checks to an expected block: during the problem, a CM should have a row like this (checked by 'compare').
	 * "CmName:column=value" (equal) or "CmName:column~value" (contains); value = a literal,
	 * "@sessions" (one of the problem's session ids) or "@tag" ("dbxt:&lt;scenario&gt;", the SQL comment / context_info prefix).
	 */
	protected static Map<String, Object> withRows(Map<String, Object> expected, String... rows)
	{
		expected.put("rows", List.of(rows));
		return expected;
	}

	protected static Map<String, Object> expected(List<String> cms, List<String> alarms, List<String> graphs, String note)
	{
		Map<String, Object> m = new LinkedHashMap<>();
		m.put("cms",    cms);
		m.put("alarms", alarms);
		m.put("graphs", graphs != null ? graphs : List.of()); // always there: 'compare' treats a journal without it as written by an older version
		m.put("rows",   List.of());
		m.put("note",   note);
		return m;
	}

	/** Pick the SQL for a vendor */
	protected static String pick(Vendor v, String sqlServer, String ase, String postgres, String oracle, String mysql, String db2)
	{
		switch (v)
		{
		case SQLSERVER: return sqlServer;
		case ASE:       return ase;
		case POSTGRES:  return postgres;
		case ORACLE:    return oracle;
		case MYSQL:     return mysql;
		case DB2:       return db2;
		}
		throw new IllegalStateException("Unknown vendor " + v);
	}

	//------------------------------------------------------------------
	// All scenarios
	//------------------------------------------------------------------
	public static List<Scenario> all()
	{
		return Arrays.asList(
				// OLTP
				new NewOrder(),
				new Payment(),
				new OrderStatus(),
				new StockCheck(),
				new CustomerSearch(),
				new Delivery(),
				// OLAP
				new SalesByMonth(),
				new TopCustomers(),
				new RollingWindow(),
				new OrderReport(),
				new EtlFactSales(),
				// BATCH
				new PurgeAudit(),
				new UpdateStats(),
				new BulkEvents(),
				// PROBLEM
				new ProblemBlocking(),
				new ProblemDeadlock(),
				new ProblemLongTran(),
				new ProblemSortSpill(),
				new ProblemLiteralSql(),
				new ProblemNonSargable(),
				new ProblemHotSpot(),
				new ProblemRbarCursor(),
				new ProblemNPlusOne(),
				new ProblemTempRecompile(),
				new ProblemConnChurn(),
				new ProblemErrors()
				);
	}

	public static void printList(PrintStream out)
	{
		out.println();
		out.println(String.format("%-20s %-8s %-14s %s", "Name", "Kind", "Weight/Every", "Description"));
		out.println(String.format("%-20s %-8s %-14s %s", "--------------------", "--------", "--------------", "-----------"));
		for (Scenario s : all())
		{
			String w = s.getKind() == Kind.PROBLEM ? "every " + s.getDefaultEveryMinutes() + " min" : "weight " + s.getDefaultWeight();
			out.println(String.format("%-20s %-8s %-14s %s", s.getName(), s.getKind(), w, s.getDescription()));
		}
		out.println();
		out.println("Change in a properties file (-c): scenario.<name>.enabled=false, scenario.<name>.weight=N, scenario.<name>.everyMinutes=N");
		out.println();
	}
}
