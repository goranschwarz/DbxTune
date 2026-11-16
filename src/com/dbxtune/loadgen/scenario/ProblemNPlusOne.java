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
 * For ~160 s (120 s + extra, see Scenario.activeSeconds; 120 s so the ChattySession alarm's 3 samples of 30 s fit with a sample to spare): an "ORM style" page: orders for a customer, then one query per order for its lines, then one query per line for the product name.
 */
public class ProblemNPlusOne extends Scenario
{
	public ProblemNPlusOne()
	{
		super("n_plus_one", Kind.PROBLEM, 5, "~160 s of N+1 queries: one query per order, then one per order line");
	}

	@Override
	public Map<String, Object> getExpected(Vendor v)
	{
		String note = "Very many executions of three tiny statements from one session.";
		switch (v)
		{
		case DB2:      return withRows(expected(Arrays.asList("CmChattyStatements", "CmMonGetConnection", "CmStatements"), Arrays.asList("AlarmEventChattySession"), note
				+ " CmMonGetConnection: the connection's statements/s (DYNAMIC_SQL_STMTS) raise ChattySession (over 100/s for 2 samples)."
				+ " Seen 2026-11-13 on DB2 11.5.8 (size small, run low): ChattySession for every event on the right application_handle; the N+1 session ~190 statements/s,"
				+ " the busiest OLTP worker ~17/s, so no false positives (with the earlier counter APP_RQSTS_COMPLETED_TOTAL, ~3.5-4 requests per statement, they had them)."), "CmMonGetConnection:APPLICATION_HANDLE=@sessions");
		case MYSQL:   return withRows(expected(Arrays.asList("CmChattyStatements", "CmSessionActivity", "CmStatements"), Arrays.asList("AlarmEventChattySession"), note
				+ " CmSessionActivity: the session's Questions/s raise ChattySession (over 100/s for 2 samples)."
				+ " Seen 2026-11-13 on MySQL 8.4 (size small, run low): ChattySession raised for every event, on the right connection_id."), "CmSessionActivity:PROCESSLIST_ID=@sessions");
		case SQLSERVER: return withRows(expected(Arrays.asList("CmExecQueryStats", "CmExecSessions", "CmPerfCounters"), Arrays.asList("AlarmEventChattySession"), Arrays.asList("CmPerfCounters.SqlBatch"), note + " Batch Requests/sec up."
				+ " CmSessions: the session's network packets received/s (1 per tiny request) raise ChattySession (over 100/s for 3 samples)."
				+ " Seen 2026-11-16 on SQL Server 2022 (size small, run low, 30 s samples): ChattySession for 6/6 events on the right session_id (~230-240/s, 2.3 reads each),"
				+ " no other session; raised 98-102 s after the start, 4-23 s before the end (3 samples just fit the ~100 s burst of then: the default is 120 s since)."
				+ " Seen 2026-11-16 with 120 s (~160 s) on 2019 CU32 and 2025 CU10 (size small, run low, 20 min): 4/4 events on the right session_id each,"
				+ " no other session; raised 74-98 s after the start, 62-80 s before the end."),"CmExecQueryStats:SqlText~@tag", "CmChattyStatements:SqlText~@tag", "CmSessions:session_id=@sessions");
		case ASE:       return withRows(expected(Arrays.asList("CmChattyStatements", "CmProcessActivity", "CmStmntCacheDetails"), Arrays.asList("AlarmEventChattySession"), note + " CmProcessActivity: the session's BatchIdDiff very high."), "CmChattyStatements:SqlText~@tag");
		case POSTGRES:  return expected(Arrays.asList("CmPgStatements", "CmPgDatabase"), Arrays.asList(), note);
		case ORACLE:    return withRows(expected(Arrays.asList("CmChattyStatements", "CmSessions", "CmSqlStats"), Arrays.asList("AlarmEventChattySession"), note
				+ " CmSessions: the session's round trips/s (ROUNDTRIPS) raise ChattySession. Seen 2026-11-09 on 23ai Free (size small, run low): ChattySession raised for every event, ~45 s after the start."), "CmChattyStatements:SqlText~@tag");
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

		int durationSec = activeSeconds(ctx, "durationSeconds", 120);
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
