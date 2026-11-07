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
 * For ~100 s (60 s + extra, see Scenario.activeSeconds): connect, run one query, disconnect - about 4 times a second (an application without a connection pool).
 */
public class ProblemConnChurn extends Scenario
{
	public ProblemConnChurn()
	{
		super("conn_churn", Kind.PROBLEM, 10, "~100 s of connect / one query / disconnect, ~4 per second (no connection pool)");
	}

	@Override
	public Map<String, Object> getExpected(Vendor v)
	{
		switch (v)
		{
		case SQLSERVER: return expected(Arrays.asList("CmPerfCounters", "CmSummary", "CmExecSessions"), Arrays.asList(), "Logins/sec and Logouts/sec up.");
		case ASE:       return expected(Arrays.asList("CmSummary", "CmSysmon", "CmProcessActivity"), Arrays.asList(), "Connections per second up (many short spids).");
		case POSTGRES:  return expected(Arrays.asList("CmSummary", "CmPgActivity", "CmPgDatabase"), Arrays.asList(), "Many short backends (sessions per second up).");
		}
		return null;
	}

	@Override
	public void runProblem(RunContext ctx, ProblemEvent ev)
	throws Exception
	{
		int durationSec = activeSeconds(ctx, "durationSeconds", 60);
		long end = System.currentTimeMillis() + durationSec * 1000L;
		String sql = tag() + "select count(*) from warehouse";
		int conns = 0, errors = 0;
		while (System.currentTimeMillis() < end && !ctx.isStopping())
		{
			try (LgSession s = ctx.openSession(this))
			{
				s.query(sql);
				conns++;
			}
			catch (Exception ex)
			{
				errors++;
			}
			ctx.sleep(250);
		}
		ev.put("sql", sql).put("connections", conns).put("connectErrors", errors).put("durationSeconds", durationSec);
		ev.setNote("Each connection is also a 'session' event in the journal (connect + disconnect).");
	}
}
