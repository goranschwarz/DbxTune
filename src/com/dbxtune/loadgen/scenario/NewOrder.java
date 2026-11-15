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

import java.math.BigDecimal;
import java.sql.Timestamp;
import java.util.Arrays;
import java.util.Random;

import com.dbxtune.loadgen.LgSession;

/**
 * A new order with 1-5 lines: reads prices, updates stock, writes the order, lines and an audit row - one transaction.
 * Products are updated in product_id order (like a well written application), so these never deadlock each other.
 */
public class NewOrder extends Scenario
{
	public NewOrder()
	{
		super("new_order", Kind.OLTP, 30, "Insert an order with 1-5 lines, update stock, audit row (one transaction)");
	}

	@Override
	public void execute(RunContext ctx, LgSession s)
	throws Exception
	{
		Random r = ctx.rnd();
		int orderId  = (int) ctx.nextVal(s, "order");
		long eventId = r.nextInt(20) == 0 ? ctx.nextVal(s, "event") : -1;
		int customer = ctx.randomCustomer();
		int wh       = 1 + r.nextInt(ctx.getWarehouses());
		int[] products = new int[1 + r.nextInt(5)];
		for (int i=0; i<products.length; i++)
			products[i] = ctx.randomProduct();
		Arrays.sort(products);

		Timestamp now = new Timestamp(System.currentTimeMillis());
		s.begin();
		try
		{
			s.update(tag() + "insert into orders (order_id, customer_id, order_ts, status, total_amount, warehouse_id, ship_city, comment_txt) values (?, ?, ?, 'NEW', 0, ?, ?, null)",
					orderId, customer, now, wh, "Stockholm");

			BigDecimal total = BigDecimal.ZERO;
			for (int l=0; l<products.length; l++)
			{
				int qty = 1 + r.nextInt(5);
				BigDecimal price = (BigDecimal) s.queryValue(tag() + "select price from product where product_id = ?", products[l]);
				if (price == null)
					price = BigDecimal.ONE;
				s.update(tag() + "update stock set qty = qty - ?, last_upd_ts = ? where warehouse_id = ? and product_id = ?", qty, now, wh, products[l]);
				s.update(tag() + "insert into order_line (order_id, line_no, product_id, qty, price) values (?, ?, ?, ?, ?)", orderId, l+1, products[l], qty, price);
				total = total.add(price.multiply(BigDecimal.valueOf(qty)));
			}
			s.update(tag() + "update orders set total_amount = ? where order_id = ?", total, orderId);
			s.update(tag() + "insert into audit_log (log_ts, login_name, action, object_id, details) values (?, ?, 'NEW_ORDER', ?, ?)", now, s.getLogin(), orderId, "lines=" + products.length + ", total=" + total);

			if (eventId > 0)
				s.update(tag() + "insert into session_event (event_id, customer_id, event_ts, event_type, payload) values (?, ?, ?, 'CHECKOUT', ?)",
						eventId, customer, now, "checkout of order " + orderId + " " + "x".repeat(500 + r.nextInt(2000)));
			s.commit();
			ctx.orderCreated(orderId);
		}
		catch (Exception ex)
		{
			s.rollbackQuietly();
			throw ex;
		}
	}
}
