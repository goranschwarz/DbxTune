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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicLong;

import com.dbxtune.loadgen.LgSession;
import com.dbxtune.loadgen.Vendor;

/**
 * For ~100 s (60 s + extra, see Scenario.activeSeconds): 6 sessions update the same counter row in a loop (each holds the lock ~20 ms): lock contention on one row.
 */
public class ProblemHotSpot extends Scenario
{
	public ProblemHotSpot()
	{
		super("hot_spot", Kind.PROBLEM, 12, "~100 s: 6 sessions increment the same counter row (lock contention on one row)");
	}

	@Override
	public Map<String, Object> getExpected(Vendor v)
	{
		switch (v)
		{
		case SQLSERVER: return withRows(expected(Arrays.asList("CmWaitStats", "CmWaitingTasks", "CmActiveStatements", "CmSpidWait"), Arrays.asList(), Arrays.asList("CmWaitStats.LockCount:*"),
				"Many short LCK_M_U / LCK_M_X waits on counter_table; Lock Waits/sec (CmPerfCounters) up. Seen 2026-11-07: 0 -> 40/s."), "CmActiveStatements:session_id=@sessions");
		case ASE:       return expected(Arrays.asList("CmSpidWait", "CmSysWaits", "CmLocks", "CmActiveStatements", "CmObjectActivity"), Arrays.asList(),
				"Many short lock waits on counter_table.");
		case POSTGRES:  return expected(Arrays.asList("CmPgPidWait", "CmPgSrvWait", "CmPgLocks", "CmPgBlockingLocks"), Arrays.asList(),
				"Many short Lock/transactionid or tuple waits on counter_table.");
		}
		return null;
	}

	@Override
	public void runProblem(RunContext ctx, ProblemEvent ev)
	throws Exception
	{
		int threadCnt   = ctx.getLoadGen().getScenarioIntProp(this, "sessions", 6);
		int durationSec = activeSeconds(ctx, "durationSeconds", 60);
		long end = System.currentTimeMillis() + durationSec * 1000L;
		String sql = tag() + "update counter_table set next_val = next_val + 1 where name = 'hotspot'";
		ev.put("sql", sql).put("sessions", threadCnt).put("durationSeconds", durationSec);

		AtomicLong ops = new AtomicLong(), waitedMs = new AtomicLong(), slow = new AtomicLong();
		java.util.concurrent.atomic.AtomicReference<String> firstError = new java.util.concurrent.atomic.AtomicReference<>();
		List<LgSession> sessions = new ArrayList<>();
		List<Thread>    threads  = new ArrayList<>();
		try
		{
			for (int i=0; i<threadCnt; i++)
			{
				LgSession s = ctx.openSession(this);
				sessions.add(s);
				ev.addSession(s, "incrementer");
				Thread t = new Thread(() -> {
					while (System.currentTimeMillis() < end && !ctx.isStopping())
					{
						try
						{
							s.begin();
							long start = System.currentTimeMillis();
							s.update(sql);
							long ms = System.currentTimeMillis() - start;
							waitedMs.addAndGet(ms);
							if (ms > 10)
								slow.incrementAndGet();
							Thread.sleep(20);
							s.commit();
							ops.incrementAndGet();
						}
						catch (Exception ex)
						{
							firstError.compareAndSet(null, ex.toString());
							s.rollbackQuietly();
						}
					}
				}, "lg-hotspot-" + i);
				t.start();
				threads.add(t);
			}
			for (Thread t : threads)
				t.join(durationSec * 1000L + 60_000);
		}
		finally
		{
			for (LgSession s : sessions)
				s.close();
		}
		if (firstError.get() != null)
			ev.put("firstError", firstError.get());
		ev.put("updates", ops.get()).put("updatesWaitingOver10ms", slow.get()).put("avgUpdateMs", ops.get() == 0 ? 0 : waitedMs.get() / ops.get());
	}
}
