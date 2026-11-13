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

import com.dbxtune.loadgen.LgSession;

/**
 * Update statistics on one of the busy tables.
 */
public class UpdateStats extends Scenario
{
	private static final String[] TABLES = {"orders", "order_line", "customer", "stock", "payment"};

	public UpdateStats()
	{
		super("update_stats", Kind.BATCH, 2, "Update statistics on one of the busy tables");
	}

	@Override
	public void execute(RunContext ctx, LgSession s)
	throws Exception
	{
		String tab = TABLES[ctx.rnd().nextInt(TABLES.length)];
		s.setQueryTimeout(600);
		s.exec(tag() + pick(ctx.getVendor(), "update statistics " + tab, "update statistics " + tab, "analyze " + tab, "begin p_gather_stats('" + tab.toUpperCase() + "'); end;",
				"analyze table " + tab,
				"call sysproc.admin_cmd('runstats on table " + ctx.getLoadGen().getDbname().toUpperCase() + "." + tab.toUpperCase() + " with distribution and indexes all')"));
				// Oracle: DBMS_STATS in a definer's rights procedure of the schema owner. DB2: RUNSTATS is a CLP command, through ADMIN_CMD
	}
}
