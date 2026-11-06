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
 * Customer search: by the start of the last name (index), or by e-mail (no index: a scan).
 */
public class CustomerSearch extends Scenario
{
	private static final String[] PREFIX = {"Sm", "And", "Joh", "Kar", "Nil", "Er", "Lar", "Ol", "Per", "Sv", "Br", "Jo", "Mi", "Da", "Wi", "Ta", "Mo", "Cl", "Le", "Wa"};
	private static final String[] FIRST  = {"anna", "erik", "maria", "lars", "karin", "johan", "eva", "per", "sara", "anders"};

	public CustomerSearch()
	{
		super("customer_search", Kind.OLTP, 10, "Search customers by last name prefix (index) or e-mail (no index)");
	}

	@Override
	public void execute(RunContext ctx, LgSession s)
	throws Exception
	{
		if (ctx.rnd().nextInt(10) < 7)
		{
			s.query(tag() + pick(ctx.getVendor(),
					"select top 20 customer_id, first_name, last_name, email, city from customer where last_name like ? order by last_name, first_name",
					"select top 20 customer_id, first_name, last_name, email, city from customer where last_name like ? order by last_name, first_name",
					"select customer_id, first_name, last_name, email, city from customer where last_name like ? order by last_name, first_name limit 20"),
					PREFIX[ctx.rnd().nextInt(PREFIX.length)] + "%");
		}
		else
		{
			int id = 2 + ctx.rnd().nextInt(ctx.getCustomers() - 1);
			s.query(tag() + "select customer_id, first_name, last_name, city from customer where email like ?", FIRST[ctx.rnd().nextInt(FIRST.length)] + ".%." + id + "@example.com");
		}
	}
}
