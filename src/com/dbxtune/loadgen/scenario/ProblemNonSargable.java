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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import com.dbxtune.loadgen.LgSession;
import com.dbxtune.loadgen.Vendor;

/**
 * For ~100 s (60 s + extra, see Scenario.activeSeconds): queries that can't use an index because of the predicate (function on the column, implicit conversion,
 * leading wildcard).
 */
public class ProblemNonSargable extends Scenario
{
	public ProblemNonSargable()
	{
		super("non_sargable", Kind.PROBLEM, 4, "~100 s of predicates that can't use an index: function on column, implicit conversion, leading wildcard");
	}

	@Override
	public Map<String, Object> getExpected(Vendor v)
	{
		switch (v)
		{
		case SQLSERVER: return withRows(expected(Arrays.asList("CmExecQueryStats", "CmIndexMissing", "CmIndexUsage", "CmActiveStatements"), Arrays.asList(),
				"Index scans instead of seeks; phone = NVARCHAR parameter against a VARCHAR column (CONVERT_IMPLICIT)."), "CmExecQueryStats:SqlText~@tag", "CmIndexMissing:TableName=customer");
		case ASE:       return expected(Arrays.asList("CmSqlStatement", "CmStmntCacheDetails", "CmObjectActivity", "CmActiveStatements"), Arrays.asList(),
				"Table scans on customer and orders (high logical reads per execution).");
		case POSTGRES:  return expected(Arrays.asList("CmPgStatements", "CmPgTables", "CmPgIndexes"), Arrays.asList(),
				"Sequential scans on customer and orders (seq_scan / seq_tup_read grow).");
		case ORACLE:    return withRows(expected(Arrays.asList("CmSqlStats", "CmSegmentStats", "CmSysStat"), Arrays.asList(),
				"Full scans of CUSTOMER and ORDERS: phone = a NUMBER bind (TO_NUMBER(phone), implicit conversion of the column), UPPER(last_name), EXTRACT(YEAR ...), a leading wildcard. High BUFFER_GETS per execution in CmSqlStats."), "CmSqlStats:SQL_FULLTEXT~@tag");
		}
		return null;
	}

	@Override
	public void runProblem(RunContext ctx, ProblemEvent ev)
	throws Exception
	{
		Vendor v = ctx.getVendor();
		String phoneSql = tag() + pick(v,
				"select count(*) from customer where phone = ?",                       // NVARCHAR parameter -> implicit conversion
				"select count(*) from customer where substring(phone, 1, 10) = ?",
				"select count(*) from customer where cast(phone as bigint) = ?",
				"select count(*) from customer where phone = ?",                       // NUMBER bind -> TO_NUMBER(phone) = :1 (implicit conversion of the column)
				"select count(*) from customer where phone = ?",                       // MySQL: number bind -> the VARCHAR column is converted to a number, no index use
				"select count(*) from customer where phone = ?");                      // DB2: BIGINT bind -> the VARCHAR column is cast (implicit casting), no index use
		String upperSql = tag() + "select count(*) from customer where upper(last_name) = ?";
		String yearSql  = tag() + pick(v,
				"select count(*), sum(total_amount) from orders where year(order_ts) = ? and status = 'NEW'",
				"select count(*), sum(total_amount) from orders where datepart(yy, order_ts) = ? and status = 'NEW'",
				"select count(*), sum(total_amount) from orders where extract(year from order_ts) = ? and status = 'NEW'",
				"select count(*), sum(total_amount) from orders where extract(year from order_ts) = ? and status = 'NEW'",
				"select count(*), sum(total_amount) from orders where year(order_ts) = ? and status = 'NEW'",
				"select count(*), sum(total_amount) from orders where year(order_ts) = ? and status = 'NEW'");
		String likeSql  = tag() + "select count(*) from customer where email like ?";
		ev.put("sql", Arrays.asList(phoneSql, upperSql, yearSql, likeSql));

		int durationSec = activeSeconds(ctx, "durationSeconds", 60);
		long end = System.currentTimeMillis() + durationSec * 1000L;
		int cnt = 0;
		List<String> lastNames = new ArrayList<>(Arrays.asList("SMITH", "ANDERSSON", "JOHANSSON", "BROWN", "WILSON"));
		try (LgSession s = ctx.openSession(this))
		{
			ev.addSession(s, "non-sargable queries");
			s.setQueryTimeout(300);
			while (System.currentTimeMillis() < end && !ctx.isStopping())
			{
				String phone = "07" + (10_000_000 + ctx.rnd().nextInt(90_000_000));
				switch (cnt % 4)
				{
				case 0: s.query(phoneSql, v == Vendor.SQLSERVER ? new LgSession.NString(phone) : v == Vendor.POSTGRES || v == Vendor.ORACLE || v == Vendor.MYSQL || v == Vendor.DB2 ? (Object) Long.parseLong(phone) : phone); break;
				case 1: s.query(upperSql, lastNames.get(ctx.rnd().nextInt(lastNames.size()))); break;
				case 2: s.query(yearSql, java.time.LocalDate.now().getYear() - ctx.rnd().nextInt(2)); break;
				case 3: s.query(likeSql, "%." + (2 + ctx.rnd().nextInt(ctx.getCustomers() - 1)) + "@example.com"); break;
				}
				cnt++;
				ctx.sleep(500);
			}
		}
		ev.put("statements", cnt).put("durationSeconds", durationSec);
	}
}
