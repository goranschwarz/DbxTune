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
 * For ~100 s (60 s + extra, see Scenario.activeSeconds): call p_temp_report (temp table + index created after the insert) twice a second.
 */
public class ProblemTempRecompile extends Scenario
{
	public ProblemTempRecompile()
	{
		super("temp_recompile", Kind.PROBLEM, 8, "~100 s of a procedure that creates a temp table + index after DML (recompiles / catalog churn)");
	}

	@Override
	public Map<String, Object> getExpected(Vendor v)
	{
		switch (v)
		{
		case SQLSERVER: return withRows(expected(Arrays.asList("CmPerfCounters", "CmExecProcedureStats", "CmTempdbUsage"), Arrays.asList(), Arrays.asList("CmPerfCounters.ReCompile"),
				"SQL Re-Compilations/sec up (2 per call: the temp table gets an index between two runs of the same select); tempdb object create/drop."), "CmExecQueryStats:object_name=p_temp_report");
		case ASE:       return expected(Arrays.asList("CmSysmon", "CmCachedProcs", "CmTempdbActivity", "CmProcCacheLoad"), Arrays.asList(),
				"Procedure recompiles (temp table created inside the procedure).");
		case POSTGRES:  return expected(Arrays.asList("CmPgStatements", "CmPgTables", "CmPgFunctions"), Arrays.asList(),
				"Temp table create/drop on every call: catalog churn (pg_class/pg_attribute bloat).");
		case ORACLE:    return withRows(expected(Arrays.asList("CmSysStat", "CmLibraryCache", "CmSqlStats"), Arrays.asList(), Arrays.asList("CmSysStat.Parse:parse count (hard)", "CmLibraryCache.LibCacheReloads:Invalidations"),
				"No temp tables in procedures in Oracle: p_temp_report creates a private temporary table (18c+) with dynamic SQL on every call (a T-SQL habit) -> ~4 hard parses and library cache invalidations per call. "
				+ "Seen 2026-11-09 on 23ai Free (size small, run low): 67 calls: +270 hard parses; Invalidations 0 -> ~8/s."), "CmSqlStats:SQL_FULLTEXT~p_temp_report");
		}
		return null;
	}

	@Override
	public void runProblem(RunContext ctx, ProblemEvent ev)
	throws Exception
	{
		String sql = pick(ctx.getVendor(), "{call p_temp_report(?)}", "{call p_temp_report(?)}", "select p_temp_report(?)", "{call p_temp_report(?)}");
		ev.put("sql", sql);

		int durationSec = activeSeconds(ctx, "durationSeconds", 60);
		long end = System.currentTimeMillis() + durationSec * 1000L;
		int calls = 0;
		try (LgSession s = ctx.openSession(this))
		{
			ev.addSession(s, "procedure calls");
			while (System.currentTimeMillis() < end && !ctx.isStopping())
			{
				s.call(sql, 2 + ctx.rnd().nextInt(ctx.getCustomers() - 1));
				calls++;
				ctx.sleep(500);
			}
		}
		ev.put("calls", calls).put("durationSeconds", durationSec);
	}
}
