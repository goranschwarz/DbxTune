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
 * Reporting straight on the OLTP tables (mixed workload): orders per status/warehouse and the best selling products the last 30 days.
 */
public class OrderReport extends Scenario
{
	public OrderReport()
	{
		super("order_report", Kind.OLAP, 15, "Report on the OLTP tables: last 30 days per status/warehouse, top products");
	}

	@Override
	public void execute(RunContext ctx, LgSession s)
	throws Exception
	{
		Timestamp from = new Timestamp(System.currentTimeMillis() - 30L * 86400L * 1000L);
		s.setQueryTimeout(600);
		s.query(tag() + "select o.status, o.warehouse_id, count(*), sum(o.total_amount) from orders o where o.order_ts >= ? group by o.status, o.warehouse_id order by 1, 2", from);

		String body = "ol.product_id, sum(ol.qty) as qty from order_line ol join orders o on o.order_id = ol.order_id where o.order_ts >= ? group by ol.product_id order by 2 desc";
		s.query(tag() + pick(ctx.getVendor(), "select top 10 " + body, "select top 10 " + body, "select " + body + " limit 10", "select " + body + " fetch first 10 rows only"), from);
	}
}
