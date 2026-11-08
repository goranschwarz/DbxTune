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
 * For ~100 s (60 s + extra, see Scenario.activeSeconds): ~20 statements per second with literals instead of parameters (a new statement text every time).
 */
public class ProblemLiteralSql extends Scenario
{
	public ProblemLiteralSql()
	{
		super("literal_sql", Kind.PROBLEM, 6, "~100 s of SQL with literals instead of parameters (plan cache / statement cache pollution, compiles)");
	}

	@Override
	public Map<String, Object> getExpected(Vendor v)
	{
		switch (v)
		{
		case SQLSERVER: return expected(Arrays.asList("CmPlanCacheHistory", "CmPerfCounters", "CmExecQueryStats", "CmMemoryClerks"), Arrays.asList("AlarmEventLiteralSql"), Arrays.asList("CmPerfCounters.Compile"),
				"Many single-use ad hoc plans; SQL Compilations/sec up (joins prevent simple parameterization). Seen 2026-11-07: compilations 4 -> 15-19/s, ~250 distinct statements per run with the same query_hash in CmExecQueryStats.");
		case ASE:       return expected(Arrays.asList("CmStatementCache", "CmStmntCacheDetails", "CmSqlStatement", "CmSysmon"), Arrays.asList(),
				"Statement cache churn, unless 'enable literal autoparam' is on.");
		case POSTGRES:  return expected(Arrays.asList("CmPgStatements"), Arrays.asList(),
				"pg_stat_statements normalizes literals, so expect ONE entry with a high call count (each call is parsed + planned).");
		}
		return null;
	}

	@Override
	public void runProblem(RunContext ctx, ProblemEvent ev)
	throws Exception
	{
		int durationSec = activeSeconds(ctx, "durationSeconds", 60);
		long end = System.currentTimeMillis() + durationSec * 1000L;
		int cnt = 0;

		try (LgSession s = ctx.openSession(this))
		{
			ev.addSession(s, "literal SQL");
			while (System.currentTimeMillis() < end && !ctx.isStopping())
			{
				int orderId  = 1 + ctx.rnd().nextInt(ctx.getMaxOrderId());
				String sql = (cnt % 2 == 0)
						? tag() + "select o.order_id, o.status, c.last_name from orders o join customer c on c.customer_id = o.customer_id where o.order_id = " + orderId + " and c.segment = 'RETAIL'"
						: tag() + "select c.customer_id, c.email, count(*) from customer c join orders o on o.customer_id = c.customer_id where c.customer_id = " + ctx.randomCustomer() + " and o.status = 'SHIPPED' group by c.customer_id, c.email";
				s.exec(sql);
				cnt++;
				ctx.sleep(50);
			}
		}
		ev.put("statements", cnt).put("durationSeconds", durationSec);
	}
}
