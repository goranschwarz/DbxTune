/*******************************************************************************
 * Copyright (C) 2010-2019 Goran Schwarz
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
package com.dbxtune.config.dict;

import java.util.HashMap;

import com.dbxtune.utils.StringUtil;

public class SqlServerOptimizerCounterDictionary
{
	/** Instance variable */
	private static SqlServerOptimizerCounterDictionary _instance = null;

	private HashMap<String, OptimizerCounterRecord> _opttimizerEntries = new HashMap<String, OptimizerCounterRecord>();

	public class OptimizerCounterRecord
	{
		private String _id              = null;
		private String _description     = null;

		public OptimizerCounterRecord(String id, String description)
		{
			_id          = id;
			_description = description;
		}
		
		@Override
		public String toString()
		{
			return StringUtil.left(_id, 50) + " - " + _description;
		}
	}


	public SqlServerOptimizerCounterDictionary()
	{
		init();
	}

	public static SqlServerOptimizerCounterDictionary getInstance()
	{
		if (_instance == null)
			_instance = new SqlServerOptimizerCounterDictionary();
		return _instance;
	}

	/**
	 * Strips out all HTML and return it as a "plain" text
	 * @param waitName
	 * @return
	 */
	public String getDescriptionPlain(String waitName)
	{
		OptimizerCounterRecord rec = _opttimizerEntries.get(waitName);
		if (rec != null)
			return StringUtil.stripHtml(rec._description);

		// Compose an empty one
		return "";
	}


	public String getDescriptionHtml(String name)
	{
		OptimizerCounterRecord rec = _opttimizerEntries.get(name);
		if (rec != null)
		{
			String str = rec._description;
			return str;
		}

		// Compose an empty one
		return "<html><code>" +name + "</code> not found in dictionary.</html>";
	}


	private void set(OptimizerCounterRecord rec)
	{
		if ( _opttimizerEntries.containsKey(rec._id))
			System.out.println("Key '" + rec._id + "' already exists. It will be overwritten.");

		_opttimizerEntries.put(rec._id, rec);
	}

	private void add(String id, String description)
	{
		set(new OptimizerCounterRecord(id, description));
	}

	private void init()
	{
		add("optimizations",                "<html>Total number of optimizations.</html>");
		add("elapsed time",                 "<html>Average elapsed time per optimization of an individual statement (query), in seconds.</html>");
		add("final cost",                   "<html>Average estimated cost for an optimized plan in internal cost units.</html>");
		add("trivial plan",                 "<html>A trivial plan in SQL Server is a quick, simple execution plan for basic queries, like a single-table SELECT. SQL Server skips detailed optimization for these straightforward queries, saving time and resources by using the most obvious execution path</html>");
		add("tasks",                        "<html>Shows the number of tasks the optimizer created during query evaluation. Higher values indicate more complex optimization efforts, often due to complex queries or larger data sets, giving insight into the workload and parallelism involved</html>");
		add("no plan",                      "<html>the Query Optimizer didn’t generate an execution plan for a query. This can happen due to: <ul><li><b>Query Errors</b>: The query contains errors that prevent it from reaching the optimization phase.</li><li><b>Timeouts</b>: The optimizer exceeded its time limit on a complex query and couldn’t complete a plan.</li><li><b>Resource Constraints</b>: Insufficient resources can prevent plan generation.</li></ul>Without an execution plan, the query can’t run efficiently, potentially leading to performance issues.</html>");
		add("search 0",                     "<html><b>search 0</b>: Indicates a very basic or simple optimization phase. (trivial optimization)</html>");
		add("search 0 time",                "<html>Indicates the time spent on this level of optimization.</html>");
		add("search 0 tasks",               "<html>Indicates individual units of work created for this optimization level.</html>");
		add("search 1",                     "<html><b>search 1</b>: Indicates a more complex optimization phase. (basic optimization)</html>");
		add("search 1 time",                "<html>Indicates the time spent on this level of optimization.</html>");
		add("search 1 tasks",               "<html>Indicates individual units of work created for this optimization level.</html>");
		add("search 2",                     "<html><b>search 2</b>: Indicates an even more exhaustive optimization phase. (more thorough optimization)</html>");
		add("search 2 time",                "<html>Indicates the time spent on this level of optimization.</html>");
		add("search 2 tasks",               "<html>Indicates individual units of work created for this optimization level.</html>");
		add("gain stage 0 to stage 1",      "<html>optimizer's improvement or gain in efficiency as it moves from 'stage 0' (trivial optimization) to 'stage 1' (basic optimization).</html>");
		add("gain stage 1 to stage 2",      "<html>optimizer's increased efficiency when advancing from 'stage 1' (basic optimization) to 'stage 2' (more thorough optimization).</html>");
		add("timeout",                      "<html>The number of times the optimizer timed out while optimizing a query. If this value is high, it could indicate that the optimizer is frequently timing out and thus not fully optimizing queries.</html>");
		add("memory limit exceeded",        "<html>The number of times optimization was constrained by memory limitations.</html>");
		add("insert stmt",                  "<html>Number of optimizations that are for <code>INSERT</code> statements.</html>");
		add("delete stmt",                  "<html>Number of optimizations that are for <code>DELETE</code> statements.</html>");
		add("update stmt",                  "<html>Number of optimizations that are for <code>UPDATE</code> statements.</html>");
		add("contains subquery",            "<html>Number of optimizations for a query that contains at least one subquery.</html>");
		add("unnest failed",                "<html><b>Unnest failed</b> in SQL Server means the Query Optimizer couldn't flatten a nested query or subquery into a simpler form. <br>This typically happens due to complex subqueries, incompatible operations, or optimizer limitations. <br>When unnesting fails, the query might still run but may not be fully optimized, leading to slower performance. <br>Simplifying the query or breaking down subqueries can often help resolve this.</html>");
		add("tables",                       "<html>Average number of tables referenced per query optimized.</html>");
		add("hints",                        "<html>Number of times some hint was specified. Hints counted include: <code>JOIN</code>, <code>GROUP</code>, <code>UNION</code> and <code>FORCE ORDER</code> query hints, <code>FORCE PLAN</code> set option, and join hints.</html>");
		add("order hint",                   "<html>Number of times where join order was forced. This counter isn't restricted to the <code>FORCE ORDER</code> hint. Specifying a join algorithm within a query, such as an <code>INNER HASH JOIN</code>, also forces the join order, which increments the counter.</html>");
		add("join hint",                    "<html>Number of times the join algorithm was forced by a join hint. The <code>FORCE ORDER</code> query hint doesn't increment this counter.</html>");
		add("view reference",               "<html>Number of times a view is referenced in a query.</html>");
		add("remote query",                 "<html>Number of optimizations where the query referenced at least one remote data source, such as a table with a four-part name or an <code>OPENROWSET</code> result.</html>");
		add("maximum DOP",                  "<html>Average effective MAXDOP value for an optimized plan. By default, effective <code>MAXDOP</code> is determined by the max degree of parallelism server configuration option, and might be overridden for a specific query by the value of the <code>MAXDOP</code> query hint.</html>");
		add("maximum recursion level",      "<html>Number of optimizations in which a <code>MAXRECURSION</code> level greater than 0 was specified with the query hint</html>");
		add("indexed views loaded",         "<html>The Query Optimizer has incorporated an indexed view into the execution plan. This improves performance by retrieving precomputed, stored data rather than recalculating it from underlying tables.</html>");
		add("indexed views matched",        "<html>Number of optimizations where one or more indexed views are matched.</html>");
		add("indexed views used",           "<html>Number of optimizations where one or more indexed views are used in the output plan after being matched.</html>");
		add("indexed views updated",        "<html>Number of optimizations of a DML statement that produce a plan that maintains one or more indexed views.</html>");
		add("dynamic cursor request",       "<html>Number of optimizations in which a dynamic cursor request was specified.</html>");
		add("fast forward cursor request",  "<html>Number of optimizations in which a fast-forward cursor request was specified.</html>");
		add("merge stmt",                   "<html>Number of optimizations that are for <code>MERGE</code> statements.</html>");
	}
}
