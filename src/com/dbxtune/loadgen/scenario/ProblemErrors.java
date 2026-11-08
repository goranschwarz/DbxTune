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
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import com.dbxtune.loadgen.LgSession;
import com.dbxtune.loadgen.Vendor;

/**
 * Statements that fail: duplicate key, divide by zero, query timeout, missing table. One round every ~4 s for ~100 s (60 s + extra, see Scenario.activeSeconds).
 */
public class ProblemErrors extends Scenario
{
	public ProblemErrors()
	{
		super("errors", Kind.PROBLEM, 5, "Failing statements: duplicate key, divide by zero, query timeout, missing table (a round every ~4 s for ~100 s)");
	}

	@Override
	public Map<String, Object> getExpected(Vendor v)
	{
		switch (v)
		{
		case SQLSERVER: return expected(Arrays.asList("CmPerfCounters", "CmErrorLog"), Arrays.asList(), Arrays.asList("CmPerfCounters.SqlErrors:User Errors"),
				"Client errors (2627, 8134, 208) and an attention/cancel; these are not in the SQL Server error log. Errors/sec (CmPerfCounters, instance User Errors) up.");
		case ASE:       return expected(Arrays.asList("CmSqlStatement", "CmSqlStatementPerDb", "CmErrorLog"), Arrays.asList(),
				"Client errors (2601, 3607, 208) and a cancel; CmSqlStatement counts errors per statement. No alarm: AseTune's ClientErrorMsg alarm"
				+ " is only for the error numbers in CmSqlStatement.alarm.system.if.ErrorNumbers (default 701, 713, 971, 1105, 1204, 1205),"
				+ " ClientErrorMsgRate only above 20 errors/s.");
		case POSTGRES:  return expected(Arrays.asList("CmPgDatabase", "CmErrorLog", "CmPgStatements"), Arrays.asList(),
				"Errors (23505, 22012, 57014, 42P01): xact_rollback grows; in the server log only if log_min_error_statement allows.");
		}
		return null;
	}

	@Override
	public void runProblem(RunContext ctx, ProblemEvent ev)
	throws Exception
	{
		Vendor v = ctx.getVendor();
		String[] sqls = {
				tag() + "insert into counter_table (name, next_val) values ('order', 0)",
				tag() + "select count(*) / 0 from warehouse",
				tag() + pick(v, "waitfor delay '00:00:05'", "waitfor delay '00:00:05'", "select pg_sleep(5)"),
				tag() + "select * from dbxt_no_such_table"
		};
		int  durationSec = activeSeconds(ctx, "durationSeconds", 60);
		long end = System.currentTimeMillis() + durationSec * 1000L;
		ev.put("sql", Arrays.asList(sqls)).put("durationSeconds", durationSec);

		List<String> results = new ArrayList<>();
		try (LgSession s = ctx.openSession(this))
		{
			ev.addSession(s, "failing statements");
			int round = 0;
			for (; System.currentTimeMillis() < end && !ctx.isStopping(); round++)
			{
				for (int i=0; i<sqls.length; i++)
				{
					s.setQueryTimeout(i == 2 ? 2 : 30);
					try
					{
						s.exec(sqls[i]);
						if (round == 0)
							results.add("stmt " + i + ": no error");
					}
					catch (SQLException ex)
					{
						if (round == 0)
							results.add("stmt " + i + ": " + ex.getErrorCode() + "/" + ex.getSQLState() + " " + ex.getMessage());
					}
				}
				ctx.sleep(2000);
			}
			ev.put("rounds", round);
		}
		ev.put("firstRoundResults", results);
	}
}
