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

import java.util.Arrays;
import java.util.Map;

import com.dbxtune.loadgen.LgSession;
import com.dbxtune.loadgen.Vendor;

/**
 * Row by row: the procedure p_rbar_price_adjust loops over the products in a category with a cursor and updates
 * one product + its stock rows (no index on stock.product_id) per row.
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
		case SQLSERVER: return expected(Arrays.asList("CmExecProcedureStats", "CmExecQueryStats", "CmExecCursors", "CmActiveStatements"), Arrays.asList(),
				"One long procedure call; the statements inside execute once per product (high execution count).");
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
		String sql = pick(ctx.getVendor(), "{call p_rbar_price_adjust(?)}", "{call p_rbar_price_adjust(?)}", "select p_rbar_price_adjust(?)");
		ev.put("category_id", category).put("sql", sql);

		try (LgSession s = ctx.openSession(this))
		{
			ev.addSession(s, "cursor procedure");
			s.setQueryTimeout(900);
			long start = System.currentTimeMillis();
			s.call(sql, category);
			ev.put("durationMs", System.currentTimeMillis() - start);
		}
	}
}
