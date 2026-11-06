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
import java.util.concurrent.CopyOnWriteArrayList;

import com.dbxtune.loadgen.LgSession;
import com.dbxtune.loadgen.Vendor;

/**
 * A blocking chain: a holder updates a stock row and keeps the transaction open (idle, waiting for the "client"),
 * 3 waiters try to update the same row and wait.
 * <p>
 * Settings: scenario.blocking.holdSeconds (default 120), scenario.blocking.waiters (default 3)
 */
public class ProblemBlocking extends Scenario
{
	public ProblemBlocking()
	{
		super("blocking", Kind.PROBLEM, 10, "A transaction holds a row lock for 120 s (idle in transaction), 3 sessions wait for it");
	}

	@Override
	public Map<String, Object> getExpected(Vendor v)
	{
		switch (v)
		{
		case SQLSERVER: return expected(Arrays.asList("CmActiveStatements", "CmWaitingTasks", "CmSummary", "CmWho"),
				Arrays.asList("AlarmEventBlockingLockAlarm", "AlarmEventHoldingLocksWhileWaitForClientInput"),
				"Waiters blocked for holdSeconds (blocking alarm at 60 s by default). The holder is idle in an open transaction (that alarm at 180 s by default).");
		case ASE:       return expected(Arrays.asList("CmBlocking", "CmActiveStatements", "CmSummary", "CmLocks"),
				Arrays.asList("AlarmEventBlockingLockAlarm"),
				"Waiters blocked for holdSeconds (blocking alarm at 60 s by default). The holder is AWAITING COMMAND with an open transaction.");
		case POSTGRES:  return expected(Arrays.asList("CmPgBlockingLocks", "CmActiveStatements", "CmPgLocks", "CmSummary"),
				Arrays.asList("AlarmEventBlockingLockAlarm", "AlarmEventHoldingLocksWhileWaitForClientInput"),
				"Waiters blocked for holdSeconds (blocking alarm at 60 s by default). The holder is 'idle in transaction'.");
		}
		return null;
	}

	@Override
	public void runProblem(RunContext ctx, ProblemEvent ev)
	throws Exception
	{
		int holdSec = (int) Math.min(ctx.getLoadGen().getScenarioIntProp(this, "holdSeconds", 120), Math.max(10, ctx.getRemainingMs() / 1000 - 10));
		int waiters = ctx.getLoadGen().getScenarioIntProp(this, "waiters", 3);
		int product = ctx.getProducts() > 200 ? 100 + ctx.rnd().nextInt(ctx.getProducts() - 100) : 2 + ctx.rnd().nextInt(ctx.getProducts() - 1);
		int wh      = 1 + ctx.rnd().nextInt(ctx.getWarehouses());
		String holdSql = tag() + "update stock set reserved = reserved + 1 where warehouse_id = ? and product_id = ?";
		String waitSql = tag() + "update stock set reserved = reserved - 1 where warehouse_id = ? and product_id = ?";
		ev.setNote("OLTP statements that scan stock (stock_check) or touch the same row also wait behind the holder, so the chain can be longer than the waiters listed here.");
		ev.put("table", "stock").put("warehouse_id", wh).put("product_id", product).put("holdSeconds", holdSec).put("holderSql", holdSql).put("waiterSql", waitSql);

		List<LgSession> sessions = new ArrayList<>();
		List<Thread>    threads  = new ArrayList<>();
		List<String>    waits    = new CopyOnWriteArrayList<>();
		try
		{
			LgSession holder = ctx.openSession(this);
			sessions.add(holder);
			ev.addSession(holder, "holder");

			holder.begin();
			holder.update(holdSql, wh, product);

			for (int i=0; i<waiters; i++)
			{
				LgSession w = ctx.openSession(this);
				sessions.add(w);
				ev.addSession(w, "waiter");
				w.setQueryTimeout(holdSec + 60);
				Thread t = new Thread(() -> {
					long start = System.currentTimeMillis();
					try
					{
						w.update(waitSql, wh, product);
						waits.add("spid " + w.getSpid() + " waited " + (System.currentTimeMillis() - start) + " ms");
					}
					catch (SQLException ex)
					{
						waits.add("spid " + w.getSpid() + " after " + (System.currentTimeMillis() - start) + " ms: " + ex.getMessage());
					}
				}, "lg-blocking-waiter-" + i);
				t.setDaemon(true);
				t.start();
				threads.add(t);
				ctx.sleep(2000);
			}

			ctx.sleep(holdSec * 1000L - waiters * 2000L);
			holder.commit();

			for (Thread t : threads)
				t.join(30_000);
			ev.put("waiterResults", new ArrayList<>(waits));
		}
		finally
		{
			for (LgSession s : sessions)
				s.close();
		}
	}
}
