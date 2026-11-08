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

import com.dbxtune.loadgen.LgSession;
import com.dbxtune.loadgen.Vendor;

/**
 * Purge the oldest 30 days of audit_log in ONE delete (big transaction, log growth), but keep at least one year.
 * From size xl, 30 days are more rows than scenario.purge_audit.maxRowsPerDelete (default 200 000, ~60 MB of log):
 * then it deletes in chunks of that many rows, so one transaction can't fill a normally sized log.
 */
public class PurgeAudit extends Scenario
{
	public PurgeAudit()
	{
		super("purge_audit", Kind.BATCH, 3, "Delete the oldest 30 days of audit_log in one statement, max 200 000 rows per delete (keeps 1 year)");
	}

	@Override
	public void execute(RunContext ctx, LgSession s)
	throws Exception
	{
		Object min = s.queryValue(tag() + "select min(log_ts) from audit_log");
		if (!(min instanceof Timestamp))
			return;
		long cutoff = ((Timestamp) min).getTime() + 30L * 86400L * 1000L;
		if (cutoff > System.currentTimeMillis() - 365L * 86400L * 1000L)
			return;

		int chunk = ctx.getLoadGen().getScenarioIntProp(this, "maxRowsPerDelete", 200_000);
		String sql = pick(ctx.getVendor(),
				"delete top (" + chunk + ") from audit_log where log_ts < ?",
				"delete from audit_log where log_ts < ?", // 'set rowcount' below
				"delete from audit_log where ctid in (select ctid from audit_log where log_ts < ? limit " + chunk + ")");

		s.setQueryTimeout(600);
		if (ctx.getVendor() == Vendor.ASE)
			s.exec("set rowcount " + chunk);
		try
		{
			int rows;
			do
			{
				rows = s.update(tag() + sql, new Timestamp(cutoff));
			}
			while (rows >= chunk && !ctx.isStopping());
		}
		finally
		{
			if (ctx.getVendor() == Vendor.ASE)
				s.exec("set rowcount 0");
		}
	}
}
