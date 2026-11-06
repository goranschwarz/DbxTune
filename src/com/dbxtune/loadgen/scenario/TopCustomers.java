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

import java.time.LocalDate;

import com.dbxtune.loadgen.LgSession;

/**
 * DW: the top 20 customers by amount in a random 90 day window.
 */
public class TopCustomers extends Scenario
{
	public TopCustomers()
	{
		super("top_customers", Kind.OLAP, 25, "DW: top 20 customers by amount in a 90 day window");
	}

	@Override
	public void execute(RunContext ctx, LgSession s)
	throws Exception
	{
		LocalDate from = LocalDate.now().minusDays(90 + ctx.rnd().nextInt(600));
		LocalDate to   = from.plusDays(90);
		int fromKey = from.getYear() * 10000 + from.getMonthValue() * 100 + from.getDayOfMonth();
		int toKey   = to  .getYear() * 10000 + to  .getMonthValue() * 100 + to  .getDayOfMonth();

		String body = "c.customer_key, c.segment, c.country_code, count(distinct f.order_id) as orders, sum(f.amount) as amount "
				+ "from fact_sales f join dim_customer c on c.customer_key = f.customer_key "
				+ "where f.date_key between ? and ? "
				+ "group by c.customer_key, c.segment, c.country_code "
				+ "order by 5 desc";
		s.setQueryTimeout(600);
		s.query(tag() + pick(ctx.getVendor(),
				"select top 20 " + body,
				"select top 20 " + body,
				"select " + body + " limit 20"), fromKey, toKey);
	}
}
