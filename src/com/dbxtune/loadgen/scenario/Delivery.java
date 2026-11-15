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
import java.util.List;

import com.dbxtune.loadgen.LgSession;

/**
 * Ship the oldest paid orders (PAID -> SHIPPED + shipment row), and refill stock that runs low.
 */
public class Delivery extends Scenario
{
	public Delivery()
	{
		super("delivery", Kind.OLTP, 5, "Ship the 10 oldest PAID orders, one transaction per order; refill low stock");
	}

	@Override
	public void execute(RunContext ctx, LgSession s)
	throws Exception
	{
		List<Object[]> rows = s.query(tag() + pick(ctx.getVendor(),
				"select top 10 order_id from orders where status = 'PAID' order by order_ts",
				"select top 10 order_id from orders where status = 'PAID' order by order_ts",
				"select order_id from orders where status = 'PAID' order by order_ts limit 10",
				"select order_id from orders where status = 'PAID' order by order_ts fetch first 10 rows only",
				"select order_id from orders where status = 'PAID' order by order_ts limit 10",
				"select order_id from orders where status = 'PAID' order by order_ts fetch first 10 rows only"));

		String[] carriers = {"PostNord", "DHL", "UPS", "Bring"};
		for (Object[] row : rows)
		{
			if (ctx.isStopping())
				return;
			Timestamp now = new Timestamp(System.currentTimeMillis());
			s.begin();
			try
			{
				if (s.update(tag() + "update orders set status = 'SHIPPED' where order_id = ? and status = 'PAID'", row[0]) == 1)
					s.update(tag() + "insert into shipment (order_id, ship_ts, carrier, status) values (?, ?, ?, 'SENT')", row[0], now, carriers[ctx.rnd().nextInt(carriers.length)]);
				s.commit();
			}
			catch (Exception ex)
			{
				s.rollbackQuietly();
				throw ex;
			}
		}

		if (ctx.rnd().nextInt(10) == 0)
			s.update(tag() + "update stock set qty = qty + 10000 where warehouse_id = ? and qty < 1000", 1 + ctx.rnd().nextInt(ctx.getWarehouses()));
	}
}
