/*******************************************************************************
 * Copyright (C) 2010-2027 Goran Schwarz
 *
 * This file is part of DbxTune
 * DbxTune is a family of sub-products *Tune, hence the Dbx
 * Here are some of the tools: AseTune, IqTune, RsTune, RaxTune, HanaTune,
 *          SqlServerTune, PostgresTune, MySqlTune, MariaDbTune, Db2Tune, ...
 *
 * DbxTune is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * DbxTune is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with DbxTune.  If not, see <http://www.gnu.org/licenses/>.
 ******************************************************************************/
package com.dbxtune.pcs.report.content.db2;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.sql.Timestamp;

import org.junit.Test;

/** Index Analysis for Db2, from table info as the DDL Storage gives it (no database needed). The round trip on a real Db2 is a separate check. */
public class Db2AbstractIndexAnalysisTest
{
	private static final String CREATE_TABLE = ""
		+ "CREATE TABLE \"S\".\"T\" (\n"
		+ "     \"ID\" INTEGER NOT NULL\n"
		+ "    ,\"A\" INTEGER\n"
		+ "    ,\"P_ID\" INTEGER\n"
		+ "    ,\"Q_ID\" INTEGER\n"
		+ ")\nIN \"USERSPACE1\"\nORGANIZE BY ROW;\n"
		+ "ALTER TABLE \"S\".\"T\" ADD CONSTRAINT \"PK\" PRIMARY KEY (ID);\n"
		+ "ALTER TABLE \"S\".\"T\" ADD CONSTRAINT \"FK_P\" FOREIGN KEY (P_ID) REFERENCES \"S\".\"P\" (ID);\n"
		+ "ALTER TABLE \"S\".\"T\" ADD CONSTRAINT \"FK_Q\" FOREIGN KEY (Q_ID, A) REFERENCES \"S\".\"Q\" (ID, A);\n";

	private static Db2Abstract.Db2TableInfo table(boolean withCreateTime)
	{
		Db2Abstract.Db2TableInfo ti = new Db2Abstract.Db2TableInfo();
		ti.schema     = "S";
		ti.name       = "T";
		ti.type       = "TABLE";
		ti.db2Type    = "T";
		ti.tableOrg   = "R";
		ti.partitions = 1;
		ti.sampleTime = Timestamp.valueOf("2026-11-06 12:00:00");
		ti.objectText = CREATE_TABLE;

		ti.indexes.add(idx("PK"      , "PRIMARY KEY", "ID"      , "REG", "2026-11-05"           , "2026-01-01-10.00.00.000000", withCreateTime));
		ti.indexes.add(idx("IX_P"    , "INDEX"      , "P_ID"    , "REG", "0001-01-01"           , "2026-01-01-10.00.00.000000", withCreateTime)); // the only FK index
		ti.indexes.add(idx("IX_A_Q"  , "INDEX"      , "A, Q_ID" , "REG", "0001-01-01"           , "2026-01-01-10.00.00.000000", withCreateTime)); // the FK (Q_ID, A) in another order
		ti.indexes.add(idx("IX_OLD"  , "INDEX"      , "A DESC"  , "REG", "2026-09-01"           , "2026-01-01-10.00.00.000000", withCreateTime)); // last used long ago
		ti.indexes.add(idx("IX_NEW"  , "INDEX"      , "ID, A"   , "REG", "0001-01-01"           , "2026-11-05-10.00.00.000000", withCreateTime)); // made yesterday
		ti.indexes.add(idx("IX_FN"   , "INDEX"      , "K00, A"  , "REG", "0001-01-01"           , "2026-01-01-10.00.00.000000", withCreateTime)); // expression
		return ti;
	}

	private static Db2Abstract.Db2IndexInfo idx(String name, String type, String keys, String indexType, String lastUsed, String createTime, boolean withCreateTime)
	{
		Db2Abstract.Db2IndexInfo ii = new Db2Abstract.Db2IndexInfo();
		ii.name       = name;
		ii.schema     = "S";
		ii.type       = type;
		ii.keys       = keys;
		ii.indexType  = indexType;
		ii.lastUsed   = lastUsed;
		ii.atLookup.rowsRead     = 0;
		ii.atLookup.rowsInserted = 0;
		if (withCreateTime)
		{
			ii.createTime   = createTime;
			ii.reverseScans = "Y";
			ii.pctFree      = "-1";
			ii.compression  = "N";
		}
		return ii;
	}

	@Test public void unusedWithCreateTime()
	{
		String html = Db2Abstract.getRedundantIndexInfoAsHtml(table(true));
		assertTrue (html, html.contains("DROP <code>IX_OLD</code></b>"));              // last used before the 7 days
		assertFalse(html, html.contains("DROP <code>IX_P</code>"));                    // the only index of FK_P
		assertFalse(html, html.contains("DROP <code>IX_A_Q</code>"));                  // the only index of FK_Q (its columns in another order)
		assertTrue (html, html.contains("FOREIGN KEY is not listed (2 on this table)"));
		assertFalse(html, html.contains("DROP <code>IX_NEW</code>"));                  // too new
		assertTrue (html, html.contains("is not listed (1 on this table)."));
		assertTrue (html, html.contains("IX_FN: a key is an expression"));
		assertFalse(html, html.contains("DROP <code>PK</code>"));
		assertTrue (html, html.contains("CREATE INDEX &quot;S&quot;.&quot;IX_OLD&quot; ON &quot;S&quot;.&quot;T&quot; (&quot;A&quot; DESC) ALLOW REVERSE SCANS COMPRESS NO"));
	}

	@Test public void olderRecordingWithoutCreateTime()
	{
		String html = Db2Abstract.getRedundantIndexInfoAsHtml(table(false));
		assertTrue (html, html.contains("DROP <code>IX_OLD</code></b>"));              // a real LASTUSED date: it existed then
		assertFalse(html, html.contains("DROP <code>IX_NEW</code>"));                  // never used, age unknown
		assertTrue (html, html.contains("the age is not known (recorded by an older version): 3 not listed")); // IX_P, IX_A_Q, IX_NEW
		assertTrue (html, html.contains("Db2&#39;s defaults are used") || html.contains("Db2's defaults are used"));
		assertTrue (html, html.contains("(&quot;A&quot; DESC)&#39;;") || html.contains("(&quot;A&quot; DESC)';"));  // no options in the CREATE INDEX
	}

	@Test public void notCheckedTables()
	{
		Db2Abstract.Db2TableInfo ti = table(true);
		ti.tableOrg = "C";
		assertTrue(Db2Abstract.getRedundantIndexInfoAsHtml(ti).contains("a column-organized table"));

		ti = table(true);
		ti.partitions = 3;
		assertTrue(Db2Abstract.getRedundantIndexInfoAsHtml(ti).contains("a partitioned table"));
	}
}
