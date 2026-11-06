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
package com.dbxtune.pcs.report.content.oracle;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.sql.Timestamp;
import java.util.Arrays;

import org.junit.Test;

/** Index Analysis for Oracle, from table info as the DDL Storage gives it (no database needed). The round trip on a real Oracle is a separate check. */
public class OracleAbstractIndexAnalysisTest
{
	/**
	 * @param newRecording  true = the lookup stored CREATED and the FOREIGN KEYs
	 * @param tracked       true = 12.2 and above (DBA_INDEX_USAGE)
	 */
	private static OracleAbstract.OraTableInfo table(boolean newRecording, boolean tracked)
	{
		OracleAbstract.OraTableInfo ti = new OracleAbstract.OraTableInfo();
		ti.owner      = "HR";
		ti.name       = "T";
		ti.type       = "TABLE";
		ti.sampleTime = Timestamp.valueOf("2026-11-06 12:00:00");
		ti.iotType    = "(NULL)";
		ti.temporary  = "N";
		if (newRecording)
		{
			ti.foreignKeysLookedUp = true;
			ti.foreignKeys.add(Arrays.asList("P_ID"));
		}

		ti.indexes.add(idx("T_PK"     , "NORMAL", "UNIQUE"   , "P"     , "ID"           , 100, "2026-11-06 10:00", "2026-01-01 10:00", newRecording, tracked));
		ti.indexes.add(idx("IX_A"     , "NORMAL", "NONUNIQUE", "(NULL)", "A"            ,  -1, "(NULL)"          , "2026-01-01 10:00", newRecording, tracked)); // never used, prefix of IX_AB
		ti.indexes.add(idx("IX_AB"    , "NORMAL", "NONUNIQUE", "(NULL)", "A, B DESC"    ,  50, "2026-11-06 09:00", "2026-01-01 10:00", newRecording, tracked));
		ti.indexes.add(idx("IX_P"     , "NORMAL", "NONUNIQUE", "(NULL)", "P_ID"         ,  -1, "(NULL)"          , "2026-01-01 10:00", newRecording, tracked)); // the only FK index
		ti.indexes.add(idx("IX_OLD"   , "NORMAL", "NONUNIQUE", "(NULL)", "X"            ,   3, "2026-08-01 10:00", "2026-01-01 10:00", newRecording, tracked)); // last used long ago
		ti.indexes.add(idx("IX_NEW"   , "NORMAL", "NONUNIQUE", "(NULL)", "Y"            ,  -1, "(NULL)"          , "2026-11-05 10:00", newRecording, tracked)); // made yesterday
		ti.indexes.add(idx("IX_FN"    , "FUNCTION-BASED NORMAL", "NONUNIQUE", "(NULL)", "UPPER(\"NAME\")", -1, "(NULL)", "2026-01-01 10:00", newRecording, tracked)); // never used
		ti.indexes.add(idx("IX_BM"    , "BITMAP", "NONUNIQUE", "(NULL)", "B"            ,  -1, "(NULL)"          , "2026-01-01 10:00", newRecording, tracked));
		return ti;
	}

	private static OracleAbstract.OraIndexInfo idx(String name, String type, String uniqueness, String constraintType, String keys, long accessCount, String lastUsed, String created, boolean newRecording, boolean tracked)
	{
		OracleAbstract.OraIndexInfo ii = new OracleAbstract.OraIndexInfo();
		ii.owner          = "HR";
		ii.name           = name;
		ii.type           = type;
		ii.uniqueness     = uniqueness;
		ii.constraintType = constraintType;
		ii.keys           = keys;
		ii.status         = "VALID";
		ii.visibility     = "VISIBLE";
		ii.partitioned    = "NO";
		ii.tablespace     = "USERS";
		ii.compression    = "DISABLED";
		if (tracked)
		{
			ii.usageTracked   = true;
			ii.absAccessCount = accessCount;
			ii.lastUsed       = lastUsed;
		}
		if (newRecording)
		{
			ii.created      = created;
			ii.prefixLength = -1;
		}
		return ii;
	}

	@Test public void newRecording()
	{
		String html = OracleAbstract.getRedundantIndexInfoAsHtml(table(true, true));
		assertTrue (html, html.contains("DROP <code>IX_A</code>, keep <code>IX_AB</code>"));
		assertTrue (html, html.contains("<code>IX_A</code> has not been used for at least 7 days."));
		assertTrue (html, html.contains("DROP <code>IX_OLD</code></b>"));
		assertTrue (html, html.contains("DROP <code>IX_FN</code></b>"));
		assertTrue (html, html.contains("CREATE INDEX &quot;HR&quot;.&quot;IX_FN&quot; ON &quot;HR&quot;.&quot;T&quot; (UPPER(&quot;NAME&quot;)) TABLESPACE &quot;USERS&quot;;")); // an expression as Oracle has it
		assertFalse(html, html.contains("DROP <code>IX_P</code>"));
		assertTrue (html, html.contains("FOREIGN KEY is not listed (1 on this table)"));
		assertTrue (html, html.contains("is not listed (1 on this table).")); // IX_NEW, too new
		assertFalse(html, html.contains("DROP <code>IX_NEW</code>"));
		assertFalse(html, html.contains("DROP <code>T_PK</code>"));
		assertTrue (html, html.contains("IX_BM: BITMAP index"));
	}

	@Test public void olderRecording()
	{
		String html = OracleAbstract.getRedundantIndexInfoAsHtml(table(false, true));
		assertTrue (html, html.contains("DROP <code>IX_A</code>, keep <code>IX_AB</code>")); // de-duplication needs neither
		assertTrue (html, html.contains("Unused Indexes</b> - not checked"));                // FOREIGN KEYs not known
		assertTrue (html, html.contains("recorded by an older version, which did not store the FOREIGN KEYs"));
	}

	@Test public void below122()
	{
		String html = OracleAbstract.getRedundantIndexInfoAsHtml(table(true, false));
		assertTrue (html, html.contains("DROP <code>IX_A</code>, keep <code>IX_AB</code>"));
		assertTrue (html, html.contains("Unused Indexes</b> - not checked"));
		assertTrue (html, html.contains("usage unknown (below 12.2)"));
	}

	@Test public void notCheckedTables()
	{
		OracleAbstract.OraTableInfo ti = table(true, true);
		ti.iotType = "IOT";
		assertTrue(OracleAbstract.getRedundantIndexInfoAsHtml(ti).contains("an index-organized table"));

		ti = table(true, true);
		ti.temporary = "Y";
		assertTrue(OracleAbstract.getRedundantIndexInfoAsHtml(ti).contains("a temporary table"));
	}
}
