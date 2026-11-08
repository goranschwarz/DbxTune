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
 * A big DISTINCT over orders x order_line with too little memory: spills to tempdb / temp files.
 */
public class ProblemSortSpill extends Scenario
{
	public ProblemSortSpill()
	{
		super("sort_spill", Kind.PROBLEM, 10, "Big DISTINCT with minimal memory (max_grant_percent=0 / work_mem=64kB): tempdb or temp file spill");
	}

	@Override
	public Map<String, Object> getExpected(Vendor v)
	{
		switch (v)
		{
		case SQLSERVER: return expected(Arrays.asList("CmTempdbUsage", "CmTempdbSpidUsage", "CmMemoryGrants", "CmExecQueryStats", "CmActiveStatements"), Arrays.asList(), Arrays.asList("CmTempdbUsage.TempdbUsageSmall:*"),
				"Hash/sort spill to tempdb (query hint max_grant_percent = 0): Workfiles Created/sec (CmPerfCounters) up. Together with the OLAP traffic it can also give RESOURCE_SEMAPHORE waits (AlarmEventToxicWait, AlarmEventMemoryGrantWait).");
		case ASE:       return expected(Arrays.asList("CmTempdbActivity", "CmActiveStatements", "CmSqlStatement", "CmStmntCacheDetails"), Arrays.asList(),
				"Work table in tempdb for the DISTINCT.");
		case POSTGRES:  return expected(Arrays.asList("CmPgDatabase", "CmPgStatements", "CmActiveStatements"), Arrays.asList(),
				"Temp files (work_mem = 64kB): pg_stat_database temp_files/temp_bytes, pg_stat_statements temp_blks_written.");
		}
		return null;
	}

	@Override
	public void runProblem(RunContext ctx, ProblemEvent ev)
	throws Exception
	{
		String body = "select count(*) from (select distinct o.customer_id, o.ship_city, ol.product_id, ol.qty, o.order_ts "
				+ "from orders o join order_line ol on ol.order_id = o.order_id) t";
		String sql = tag() + pick(ctx.getVendor(), body + " option (max_grant_percent = 0)", body, body);
		ev.put("sql", sql);

		try (LgSession s = ctx.openSession(this))
		{
			ev.addSession(s, "spilling query");
			s.setQueryTimeout(900);
			long start = System.currentTimeMillis();
			if (ctx.getVendor() == Vendor.POSTGRES)
			{
				s.begin();
				s.exec("set local work_mem = '64kB'");
				s.exec(sql);
				s.commit();
			}
			else
			{
				s.exec(sql);
			}
			ev.put("durationMs", System.currentTimeMillis() - start);
		}
	}
}
