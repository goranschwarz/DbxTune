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

import java.sql.SQLException;
import java.util.Arrays;
import java.util.Map;

import com.dbxtune.loadgen.LgSession;
import com.dbxtune.loadgen.Vendor;

/**
 * Row by row: the procedure p_rbar_price_adjust loops over the products in a category with a cursor and updates
 * one product + its stock rows (no index on stock.product_id) per row.
 * <p>
 * Each row scans stock, so the rows are limited: <code>max_rows</code> = 20 000 000 / stock rows (10-500),
 * about 20 million stock rows read per call whatever the size (one call ran 15 minutes at size large before the limit).
 * Setting: scenario.rbar_cursor.maxRows
 */
public class ProblemRbarCursor extends Scenario
{
	public ProblemRbarCursor()
	{
		super("rbar_cursor", Kind.PROBLEM, 10, "Procedure with a cursor: 2 updates per product row, one of them scans stock");
	}

	@Override
	public Map<String, Object> getExpected(Vendor v)
	{
		switch (v)
		{
		case SQLSERVER: return withRows(expected(Arrays.asList("CmExecProcedureStats", "CmExecQueryStats", "CmExecCursors", "CmActiveStatements"), Arrays.asList(),
				"One long procedure call; the statements inside execute once per product (high execution count)."), "CmActiveStatements:session_id=@sessions");
		case ASE:       return expected(Arrays.asList("CmActiveStatements", "CmProcCallStack", "CmCachedProcs", "CmSqlStatement", "CmObjectActivity"), Arrays.asList(),
				"One long procedure call with a cursor; table scans of stock per row.");
		case POSTGRES:  return expected(Arrays.asList("CmPgFunctions", "CmPgStatements", "CmActiveStatements", "CmPgTables"), Arrays.asList(),
				"One long function call (track_functions must be on for CmPgFunctions); seq scans of stock per row.");
		}
		return null;
	}

	@Override
	public void runProblem(RunContext ctx, ProblemEvent ev)
	throws Exception
	{
		int category = 1 + ctx.rnd().nextInt(ctx.getCategories());
		long stockRows = (long) ctx.getWarehouses() * ctx.getProducts();
		int  maxRows   = ctx.getLoadGen().getScenarioIntProp(this, "maxRows", (int) Math.max(10, Math.min(500, 20_000_000L / Math.max(1, stockRows))));
		String sql = pick(ctx.getVendor(), "{call p_rbar_price_adjust(?, ?)}", "{call p_rbar_price_adjust(?, ?)}", "select p_rbar_price_adjust(?, ?)");
		ev.put("category_id", category).put("maxRows", maxRows).put("sql", sql);

		try (LgSession s = ctx.openSession(this))
		{
			ev.addSession(s, "cursor procedure");
			s.setQueryTimeout(900);
			long start = System.currentTimeMillis();
			try
			{
				s.call(sql, category, maxRows);
			}
			catch (SQLException ex)
			{
				// Installed before max_rows was added (2026-11-07): the old one-argument procedure, all rows of the category
				if (ctx.isStopping() || s.isCancelled() || ex instanceof java.sql.SQLTimeoutException)
					throw ex;
				ev.setNote("The procedure has no max_rows parameter (installed before 2026-11-07, re-install to get it): called without a limit. First try: " + ex.getMessage());
				s.call(pick(ctx.getVendor(), "{call p_rbar_price_adjust(?)}", "{call p_rbar_price_adjust(?)}", "select p_rbar_price_adjust(?)"), category);
			}
			ev.put("durationMs", System.currentTimeMillis() - start);
		}
	}
}
