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
 * DW: daily sales with a 7 day moving average for one quarter.
 * SQL Server and Postgres use a window function, ASE (no window functions) a correlated subquery.
 */
public class RollingWindow extends Scenario
{
	public RollingWindow()
	{
		super("rolling_window", Kind.OLAP, 20, "DW: daily sales + 7 day moving average for a quarter (window function / correlated subquery on ASE)");
	}

	@Override
	public void execute(RunContext ctx, LgSession s)
	throws Exception
	{
		int year    = LocalDate.now().getYear() - ctx.rnd().nextInt(2);
		int quarter = 1 + ctx.rnd().nextInt(4);

		String windowSql = "select cal_date, daily_amount, avg(daily_amount) over (order by cal_date rows between 6 preceding and current row) as avg_7d "
				+ "from (select d.cal_date, sum(f.amount) as daily_amount "
				+ "      from fact_sales f join dim_date d on d.date_key = f.date_key "
				+ "      where d.cal_year = ? and d.cal_quarter = ? "
				+ "      group by d.cal_date) t "
				+ "order by cal_date";

		String aseSql = "select d1.cal_date, "
				+ "       (select sum(f.amount) from fact_sales f, dim_date d2 "
				+ "        where d2.date_key = f.date_key and d2.cal_date between dateadd(dd, -6, d1.cal_date) and d1.cal_date) / 7 as avg_7d "
				+ "from dim_date d1 "
				+ "where d1.cal_year = ? and d1.cal_quarter = ? "
				+ "order by d1.cal_date";

		s.setQueryTimeout(600);
		s.query(tag() + pick(ctx.getVendor(), windowSql, aseSql, windowSql), year, quarter);
	}
}
