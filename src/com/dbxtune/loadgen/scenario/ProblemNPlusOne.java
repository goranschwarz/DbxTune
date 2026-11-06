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
import java.util.List;
import java.util.Map;

import com.dbxtune.loadgen.LgSession;
import com.dbxtune.loadgen.Vendor;

/**
 * For 60 s: an "ORM style" page: orders for a customer, then one query per order for its lines, then one query per line for the product name.
 */
public class ProblemNPlusOne extends Scenario
{
	public ProblemNPlusOne()
	{
		super("n_plus_one", Kind.PROBLEM, 5, "60 s of N+1 queries: one query per order, then one per order line");
	}

	@Override
	public Map<String, Object> getExpected(Vendor v)
	{
		String note = "Very many executions of three tiny statements from one session.";
		switch (v)
		{
		case SQLSERVER: return expected(Arrays.asList("CmExecQueryStats", "CmExecSessions", "CmPerfCounters"), Arrays.asList(), note + " Batch Requests/sec up.");
		case ASE:       return expected(Arrays.asList("CmSqlStatement", "CmStmntCacheDetails", "CmProcessActivity"), Arrays.asList(), note);
		case POSTGRES:  return expected(Arrays.asList("CmPgStatements", "CmPgDatabase"), Arrays.asList(), note);
		}
		return null;
	}

	@Override
	public void runProblem(RunContext ctx, ProblemEvent ev)
	throws Exception
	{
		String ordersSql = tag() + "select order_id from orders where customer_id = ?";
		String linesSql  = tag() + "select line_no, product_id, qty from order_line where order_id = ?";
		String prodSql   = tag() + "select name from product where product_id = ?";
		ev.put("sql", Arrays.asList(ordersSql, linesSql, prodSql));

		int durationSec = ctx.getLoadGen().getScenarioIntProp(this, "durationSeconds", 60);
		long end = System.currentTimeMillis() + durationSec * 1000L;
		int pages = 0, stmts = 0;
		try (LgSession s = ctx.openSession(this))
		{
			ev.addSession(s, "N+1 queries");
			while (System.currentTimeMillis() < end && !ctx.isStopping())
			{
				int customer = 2 + ctx.rnd().nextInt(ctx.getCustomers() - 1);
				List<Object[]> orders = s.query(ordersSql, customer);
				stmts++;
				for (Object[] o : orders)
				{
					for (Object[] l : s.query(linesSql, o[0]))
					{
						s.query(prodSql, l[1]);
						stmts++;
					}
					stmts++;
				}
				pages++;
				ctx.sleep(100);
			}
		}
		ev.put("pages", pages).put("statements", stmts).put("durationSeconds", durationSec);
	}
}
