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
 * DW: sales per month and category for one year (star join on fact_sales).
 */
public class SalesByMonth extends Scenario
{
	public SalesByMonth()
	{
		super("sales_by_month", Kind.OLAP, 30, "DW: sales per month and category for a year (star join, group by)");
	}

	@Override
	public void execute(RunContext ctx, LgSession s)
	throws Exception
	{
		int year = LocalDate.now().getYear() - ctx.rnd().nextInt(3);
		s.setQueryTimeout(600);
		s.query(tag() + "select d.cal_year, d.cal_month, p.category_name, count(*), sum(f.qty), sum(f.amount) "
				+ "from fact_sales f "
				+ "join dim_date d on d.date_key = f.date_key "
				+ "join dim_product p on p.product_key = f.product_key "
				+ "where d.cal_year = ? "
				+ "group by d.cal_year, d.cal_month, p.category_name "
				+ "order by d.cal_year, d.cal_month, p.category_name", year);
	}
}
