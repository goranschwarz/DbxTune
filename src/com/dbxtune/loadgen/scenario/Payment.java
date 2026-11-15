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

/**
 * Pay one of the newest orders: NEW -> PAID, a payment row and an audit row.
 */
public class Payment extends Scenario
{
	public Payment()
	{
		super("payment", Kind.OLTP, 20, "Pay a recent order: update order status, insert payment + audit row");
	}

	@Override
	public void execute(RunContext ctx, LgSession s)
	throws Exception
	{
		int orderId = ctx.randomRecentOrder();
		long payId  = ctx.nextVal(s, "payment");
		String[] methods = {"CARD", "INVOICE", "SWISH"};

		Timestamp now = new Timestamp(System.currentTimeMillis());
		s.begin();
		try
		{
			int cnt = s.update(tag() + "update orders set status = 'PAID' where order_id = ? and status = 'NEW'", orderId);
			if (cnt == 1)
			{
				Object amount = s.queryValue(tag() + "select total_amount from orders where order_id = ?", orderId);
				s.update(tag() + "insert into payment (payment_id, order_id, pay_ts, amount, method) values (?, ?, ?, ?, ?)", (int) payId, orderId, now, amount, methods[ctx.rnd().nextInt(3)]);
				s.update(tag() + "insert into audit_log (log_ts, login_name, action, object_id, details) values (?, ?, 'PAYMENT', ?, null)", now, s.getLogin(), orderId);
			}
			s.commit();
		}
		catch (Exception ex)
		{
			s.rollbackQuietly();
			throw ex;
		}
	}
}
