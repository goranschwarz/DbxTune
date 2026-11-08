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

import java.sql.Timestamp;
import java.util.Arrays;
import java.util.Map;

import com.dbxtune.loadgen.LgSession;
import com.dbxtune.loadgen.Vendor;

/**
 * A long running open transaction: insert one row, then keep the transaction open (idle) for ~340 s (300 s + extra, see Scenario.activeSeconds).
 * <p>
 * Settings: scenario.long_tran.holdSeconds (default 300)
 */
public class ProblemLongTran extends Scenario
{
	public ProblemLongTran()
	{
		super("long_tran", Kind.PROBLEM, 15, "Insert a row and keep the transaction open (idle) for ~340 s");
	}

	@Override
	public Map<String, Object> getExpected(Vendor v)
	{
		switch (v)
		{
		case SQLSERVER: return withRows(expected(Arrays.asList("CmOpenTransactions", "CmDatabases", "CmSummary", "CmActiveStatements"),
				Arrays.asList("AlarmEventLongRunningTransaction", "AlarmEventHoldingLocksWhileWaitForClientInput"),
				"Oldest open transaction grows to holdSeconds (alarm at 60 s by default; holding locks while waiting for client at 180 s)."), "CmOpenTransactions:session_id=@sessions");
		case ASE:       return expected(Arrays.asList("CmOpenDatabases", "CmSummary", "CmActiveStatements"),
				Arrays.asList("AlarmEventLongRunningTransaction"),
				"Oldest open transaction grows to holdSeconds (alarm at 60 s by default); the log can't be truncated past it.");
		case POSTGRES:  return expected(Arrays.asList("CmActiveStatements", "CmPgActivity", "CmSummary"),
				Arrays.asList(),
				"'idle in transaction' for holdSeconds. The open transaction alarm defaults to 30 minutes in PostgresTune, so no alarm is expected with the default 300 s.");
		}
		return null;
	}

	@Override
	public void runProblem(RunContext ctx, ProblemEvent ev)
	throws Exception
	{
		long holdSec = Math.min(activeSeconds(ctx, "holdSeconds", 300), ctx.getRemainingMs() / 1000 - 15);
		if (holdSec < 60)
		{
			ev.setNote("Skipped: less than 60 s of the run left");
			return;
		}
		String sql = tag() + "insert into audit_log (log_ts, login_name, action, object_id, details) values (?, ?, 'LONG_TRAN', null, 'open transaction test')";
		ev.put("holdSeconds", holdSec).put("sql", sql);

		try (LgSession s = ctx.openSession(this))
		{
			ev.addSession(s, "open transaction");
			s.begin();
			s.update(sql, new Timestamp(System.currentTimeMillis()), s.getLogin());
			ctx.sleep(holdSec * 1000L);
			s.commit();
		}
	}
}
