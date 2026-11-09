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

import java.util.List;

import com.dbxtune.loadgen.LgSession;

/**
 * A customer looks at the order history: half of the time through the procedure p_order_status (parameter sniffing:
 * customer 1 owns ~30% of the orders), the other half with plain SQL.
 */
public class OrderStatus extends Scenario
{
	public OrderStatus()
	{
		super("order_status", Kind.OLTP, 20, "Order history for a customer: procedure p_order_status (parameter sniffing) or plain SQL");
	}

	@Override
	public void execute(RunContext ctx, LgSession s)
	throws Exception
	{
		int customer = ctx.randomCustomer();
		if (ctx.rnd().nextBoolean())
		{
			s.call(pick(ctx.getVendor(),
					"{call p_order_status(?)}",
					"{call p_order_status(?)}",
					"select * from p_order_status(?)",
					"{call p_order_status(?)}"), customer); // Oracle: the rows come back as an implicit result (DBMS_SQL.RETURN_RESULT)
			return;
		}

		List<Object[]> orders = s.query(tag() + pick(ctx.getVendor(),
				"select top 10 order_id, order_ts, status, total_amount from orders where customer_id = ? order by order_ts desc",
				"select top 10 order_id, order_ts, status, total_amount from orders where customer_id = ? order by order_ts desc",
				"select order_id, order_ts, status, total_amount from orders where customer_id = ? order by order_ts desc limit 10",
				"select order_id, order_ts, status, total_amount from orders where customer_id = ? order by order_ts desc fetch first 10 rows only"), customer);
		if (!orders.isEmpty())
		{
			s.query(tag() + "select ol.line_no, p.name, ol.qty, ol.price from order_line ol join product p on p.product_id = ol.product_id where ol.order_id = ? order by ol.line_no",
					orders.get(0)[0]);
		}
	}
}
