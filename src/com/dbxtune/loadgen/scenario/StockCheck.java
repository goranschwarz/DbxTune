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
 * Stock for a product in all warehouses (the stock primary key starts with warehouse_id: a scan),
 * and now and then the sold quantity of a product (order_line has no index on product_id: a scan).
 */
public class StockCheck extends Scenario
{
	public StockCheck()
	{
		super("stock_check", Kind.OLTP, 10, "Stock per product (PK does not start with product_id) and sold qty per product (no index): scans");
	}

	@Override
	public void execute(RunContext ctx, LgSession s)
	throws Exception
	{
		int product = ctx.randomProduct();
		s.query(tag() + "select warehouse_id, qty, reserved from stock where product_id = ?", product);

		if (ctx.rnd().nextInt(5) == 0)
			s.query(tag() + "select count(*), sum(qty) from order_line where product_id = ?", product);
	}
}
