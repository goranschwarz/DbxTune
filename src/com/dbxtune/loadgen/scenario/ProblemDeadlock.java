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
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.CountDownLatch;

import com.dbxtune.loadgen.LgSession;
import com.dbxtune.loadgen.Vendor;

/**
 * A deadlock: session A updates row 1 then row 2, session B updates row 2 then row 1.
 */
public class ProblemDeadlock extends Scenario
{
	public ProblemDeadlock()
	{
		super("deadlock", Kind.PROBLEM, 7, "Two sessions update two stock rows in opposite order: one becomes the deadlock victim");
	}

	@Override
	public Map<String, Object> getExpected(Vendor v)
	{
		switch (v)
		{
		case SQLSERVER: return expected(Arrays.asList("CmSummary", "CmPerfCounters"), Arrays.asList("AlarmEventDeadlock"),
				"One deadlock (error 1205 for the victim).");
		case ASE:       return expected(Arrays.asList("CmDeadlock", "CmSummary", "CmSysmon"), Arrays.asList(),
				"One deadlock (error 1205 for the victim). Note: no deadlock alarm is known in AseTune, check what CmDeadlock shows.");
		case POSTGRES:  return expected(Arrays.asList("CmPgDatabase"), Arrays.asList(),
				"One deadlock (SQLState 40P01 for the victim) - pg_stat_database.deadlocks. Note: no deadlock alarm is known in PostgresTune.");
		case ORACLE:    return expected(Arrays.asList("CmErrorLog", "CmSysStat"), Arrays.asList("AlarmEventErrorLogEntry:ORA-00060"),
				"One deadlock (ORA-00060 for the victim; only its statement is rolled back, the scenario then rolls back its transaction). Oracle writes 'ORA-00060: Deadlock detected' to the alert log:"
				+ " CmErrorLog raises ErrorLogEntry (extraInfo ORA-00060). CmSysStat 'enqueue deadlocks' +1 (too small for a graph check). " + "Seen 2026-11-09 on 23ai Free (size small, run low): the alarm was raised for the first deadlock and stayed active for the next ones.");
		}
		return null;
	}

	@Override
	public void runProblem(RunContext ctx, ProblemEvent ev)
	throws Exception
	{
		int base = ctx.getProducts() > 200 ? 100 + ctx.rnd().nextInt(ctx.getProducts() - 102) : 2 + ctx.rnd().nextInt(ctx.getProducts() - 3);
		int p1 = base, p2 = base + 1;
		int wh = 1 + ctx.rnd().nextInt(ctx.getWarehouses());
		String sql = tag() + "update stock set last_upd_ts = last_upd_ts where warehouse_id = ? and product_id = ?";
		ev.put("table", "stock").put("warehouse_id", wh).put("product_ids", Arrays.asList(p1, p2)).put("sql", sql);
		// Oracle: ORA-00060 rolls back only the victim's STATEMENT; its transaction (and first row lock) stays until it rolls back, which twoUpdates() does

		List<String> results = new CopyOnWriteArrayList<>();
		CountDownLatch bothHaveFirstLock = new CountDownLatch(2);

		try (LgSession a = ctx.openSession(this); LgSession b = ctx.openSession(this))
		{
			ev.addSession(a, "A: " + p1 + " then " + p2);
			ev.addSession(b, "B: " + p2 + " then " + p1);
			a.setQueryTimeout(60);
			b.setQueryTimeout(60);

			Thread ta = new Thread(() -> twoUpdates(a, sql, wh, p1, p2, bothHaveFirstLock, results), "lg-deadlock-A");
			Thread tb = new Thread(() -> twoUpdates(b, sql, wh, p2, p1, bothHaveFirstLock, results), "lg-deadlock-B");
			ta.start();
			tb.start();
			ta.join(90_000);
			tb.join(90_000);
		}
		ev.put("results", results);
		if (results.stream().noneMatch(r -> r.contains("VICTIM")))
			ev.setError("No deadlock victim was seen");
	}

	private void twoUpdates(LgSession s, String sql, int wh, int first, int second, CountDownLatch latch, List<String> results)
	{
		try
		{
			s.begin();
			s.update(sql, wh, first);
			latch.countDown();
			latch.await();
			Thread.sleep(500);
			s.update(sql, wh, second);
			s.commit();
			results.add("spid " + s.getSpid() + " committed");
		}
		catch (SQLException ex)
		{
			s.rollbackQuietly();
			boolean victim = (ex.getErrorCode() == 1205 && s.getVendor() != Vendor.MYSQL) || "40P01".equals(ex.getSQLState())
					|| (s.getVendor() == Vendor.ORACLE && ex.getErrorCode() == 60)     // ORA-00060
					|| (s.getVendor() == Vendor.MYSQL  && ex.getErrorCode() == 1213)   // ER_LOCK_DEADLOCK (1205 is a lock wait timeout there)
					|| (s.getVendor() == Vendor.DB2    && ex.getErrorCode() == -911 && "40001".equals(ex.getSQLState())); // SQL0911N reason code 2
			results.add("spid " + s.getSpid() + (victim ? " VICTIM" : " error") + ": " + ex.getErrorCode() + "/" + ex.getSQLState() + " " + ex.getMessage());
		}
		catch (InterruptedException ex)
		{
			s.rollbackQuietly();
		}
	}
}
