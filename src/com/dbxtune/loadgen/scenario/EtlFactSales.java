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
import com.dbxtune.loadgen.Vendor;

/**
 * DW load: move new orders (above the watermark) into fact_sales, 5000 orders at a time.
 */
public class EtlFactSales extends Scenario
{
	public EtlFactSales()
	{
		super("etl_fact_sales", Kind.OLAP, 10, "DW load: orders above the watermark into fact_sales (insert-select, 5000 orders)");
	}

	/** The insert-select for orders with id in (fromOrderId, toOrderId] - also used by the install */
	public static String insertSql(Vendor v, int fromOrderId, int toOrderId)
	{
		String dateKey = pick(v,
				"convert(int, convert(char(8), o.order_ts, 112))",
				"convert(int, convert(char(8), o.order_ts, 112))",
				"cast(to_char(o.order_ts, 'YYYYMMDD') as int)",
				"to_number(to_char(o.order_ts, 'YYYYMMDD'))",
				"year(o.order_ts) * 10000 + month(o.order_ts) * 100 + day(o.order_ts)",
				"year(o.order_ts) * 10000 + month(o.order_ts) * 100 + day(o.order_ts)");

		return "insert into fact_sales (order_id, line_no, date_key, customer_key, product_key, qty, amount) "
				+ "select ol.order_id, ol.line_no, " + dateKey + ", o.customer_id, ol.product_id, ol.qty, ol.qty * ol.price "
				+ "from orders o join order_line ol on ol.order_id = o.order_id "
				+ "where o.order_id > " + fromOrderId + " and o.order_id <= " + toOrderId;
	}

	@Override
	public void execute(RunContext ctx, LgSession s)
	throws Exception
	{
		int last = s.queryInt(tag() + "select last_order_id from etl_watermark where name = 'fact_sales'");
		int to   = Math.min(last + 5000, ctx.getMaxOrderId() - 100); // leave the newest orders (lines may still be written)
		if (to <= last)
			return;

		s.setQueryTimeout(600);
		s.begin();
		try
		{
			// The watermark update first: it locks the row, so two workers never load the same orders
			if (s.update(tag() + "update etl_watermark set last_order_id = ? where name = 'fact_sales' and last_order_id = ?", to, last) == 1)
				s.exec(tag() + insertSql(ctx.getVendor(), last, to));
			s.commit();
		}
		catch (Exception ex)
		{
			s.rollbackQuietly();
			throw ex;
		}
	}
}
