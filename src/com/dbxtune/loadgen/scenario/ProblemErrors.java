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
		case DB2:      return withRows(expected(Arrays.asList("CmMonGetConnection", "CmStatements", "CmSummary"), Arrays.asList(),
				"Client errors SQL0803N, SQL0801N, a cancelled DBMS_ALERT.SLEEP (SQL0952N) and SQL0204N: not in db2diag.log, so no alarm. The session's FAILED_SQL_STMTS in CmMonGetConnection, NUM_EXEC_WITH_ERROR in CmStatements."
				+ " Seen 2026-11-13 on DB2 11.5.8."), "CmMonGetConnection:APPLICATION_HANDLE=@sessions");
		case MYSQL:    return withRows(expected(Arrays.asList("CmSessionActivity", "CmStatements"), Arrays.asList(),
				"Client errors 1062, 1242, a cancelled SLEEP() and 1146: they are not written to the error log, so no alarm. CmStatements counts them per digest (Errors). Seen 2026-11-13 on MySQL 8.4."), "CmSessionActivity:PROCESSLIST_ID=@sessions");
		case SQLSERVER: return expected(Arrays.asList("CmPerfCounters", "CmErrorLog"), Arrays.asList(), Arrays.asList("CmPerfCounters.SqlErrors:User Errors"),
				"Client errors (2627, 8134, 208) and an attention/cancel; these are not in the SQL Server error log. Errors/sec (CmPerfCounters, instance User Errors) up.");
		case ASE:       return expected(Arrays.asList("CmSqlStatement", "CmSqlStatementPerDb", "CmErrorLog"), Arrays.asList(),
				"Client errors (2601, 3607, 208) and a cancel; CmSqlStatement counts errors per statement. No alarm: AseTune's ClientErrorMsg alarm"
				+ " is only for the error numbers in CmSqlStatement.alarm.system.if.ErrorNumbers (default 701, 713, 971, 1105, 1204, 1205),"
				+ " ClientErrorMsgRate only above 20 errors/s.");
		case POSTGRES:  return expected(Arrays.asList("CmPgDatabase", "CmErrorLog", "CmPgStatements"), Arrays.asList(),
				"Errors (23505, 22012, 57014, 42P01): xact_rollback grows; in the server log only if log_min_error_statement allows.");
		case ORACLE:    return withRows(expected(Arrays.asList("CmSysStat", "CmSqlStats"), Arrays.asList(),
				"Client errors ORA-00001, ORA-01476, ORA-01013 (the cancel), ORA-00942: none of them goes to the alert log, so CmErrorLog shows nothing and no alarm is expected."
				+ " ORA-00942 is a failed parse: CmSysStat 'parse count (failures)' ~0.25/s (too small for a graph check). The statements that parse are in CmSqlStats."), "CmSqlStats:SQL_FULLTEXT~@tag");
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
				// MySQL: a division by zero in a SELECT is NULL (no error): a scalar subquery with 2 rows instead (error 1242)
				tag() + pick(v, "select count(*) / 0 from warehouse", "select count(*) / 0 from warehouse", "select count(*) / 0 from warehouse", "select count(*) / 0 from warehouse",
				                "select count(*) from warehouse where warehouse_id = (select warehouse_id from warehouse)", "select count(*) / 0 from warehouse"),
				tag() + pick(v, "waitfor delay '00:00:05'", "waitfor delay '00:00:05'", "select pg_sleep(5)", "begin dbms_session.sleep(5); end;", // Oracle: dbms_session.sleep is 18c+
				                "select sleep(5)", "call dbms_alert.sleep(5)"),
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
