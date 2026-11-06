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
package com.dbxtune.pcs.report.content.mysql;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.util.Arrays;

import org.junit.Test;

/** The parser for SHOW CREATE TABLE, used by Index Analysis. The texts are from MySQL 8.0 */
public class MySqlAbstractShowCreateTableTest
{
	private static final String TABLE_C = ""
		+ "CREATE TABLE `c` (\n"
		+ "  `id` int NOT NULL AUTO_INCREMENT,\n"
		+ "  `p_id` int DEFAULT NULL,\n"
		+ "  `a` varchar(100) DEFAULT NULL COMMENT 'has KEY `x` (y), text',\n"
		+ "  `b` int DEFAULT NULL,\n"
		+ "  `g` geometry NOT NULL /*!80003 SRID 0 */,\n"
		+ "  `txt` text,\n"
		+ "  PRIMARY KEY (`id`),\n"
		+ "  UNIQUE KEY `uq_b` (`b`),\n"
		+ "  KEY `ix_p` (`p_id`),\n"
		+ "  KEY `ix a,b` (`a`(10),`b` DESC) COMMENT 'it''s (a) comment',\n"
		+ "  KEY `ix_inv` (`b`) /*!80000 INVISIBLE */,\n"
		+ "  SPATIAL KEY `sp` (`g`),\n"
		+ "  KEY `ix_fn` ((lower(`a`)),`b`),\n"
		+ "  FULLTEXT KEY `ft` (`txt`),\n"
		+ "  CONSTRAINT `fk_p` FOREIGN KEY (`p_id`) REFERENCES `p` (`id`) ON DELETE CASCADE\n"
		+ ") ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci";

	private static MySqlAbstract.MyCreateTable p(String ddl) { return MySqlAbstract.parseShowCreateTable(ddl); }

	@Test public void allIndexKinds()
	{
		MySqlAbstract.MyCreateTable ct = p(TABLE_C);
		assertEquals("InnoDB", ct.engine);
		assertFalse(ct.partitioned);
		assertEquals(8, ct.keys.size()); // the column comment with "KEY `x` (y)" is not an index

		MySqlAbstract.MyKeyDdl pk = ct.get("PRIMARY");
		assertEquals("PRIMARY", pk.kind);
		assertEquals(Arrays.asList("`id`"), pk.keys);

		assertEquals("UNIQUE"  , ct.get("uq_b").kind);
		assertEquals("KEY"     , ct.get("ix_p").kind);
		assertEquals("SPATIAL" , ct.get("sp").kind);
		assertEquals("FULLTEXT", ct.get("ft").kind);
		assertEquals(0, pk.pos);
		assertEquals(7, ct.get("ft").pos);
	}

	@Test public void prefixDescCommentAndQuotedName()
	{
		MySqlAbstract.MyKeyDdl k = p(TABLE_C).get("ix a,b");
		assertEquals(Arrays.asList("`a`(10)", "`b` DESC"), k.keys);
		assertFalse(k.functional);
		assertFalse(k.invisible);
		assertEquals("KEY `ix a,b` (`a`(10),`b` DESC) COMMENT 'it''s (a) comment'", k.line); // the comma at the end is gone: the line is the rollback
	}

	@Test public void invisibleAndFunctional()
	{
		MySqlAbstract.MyCreateTable ct = p(TABLE_C);
		assertTrue(ct.get("ix_inv").invisible);
		assertFalse(ct.get("ix_inv").functional);

		MySqlAbstract.MyKeyDdl fn = ct.get("ix_fn");
		assertTrue(fn.functional);
		assertEquals(Arrays.asList("(lower(`a`))", "`b`"), fn.keys);
	}

	@Test public void foreignKey()
	{
		MySqlAbstract.MyCreateTable ct = p(TABLE_C);
		assertEquals(1, ct.foreignKeys.size());
		assertEquals(Arrays.asList("`p_id`"), ct.foreignKeys.get(0));
	}

	@Test public void caseOfIndexNameAndBacktickInName()
	{
		MySqlAbstract.MyCreateTable ct = p("CREATE TABLE `t` (\n  `a` int,\n  KEY `Ix``1` (`a`)\n) ENGINE=InnoDB");
		assertEquals("Ix`1", ct.keys.get(0).name);
		assertEquals("Ix`1", ct.get("ix`1").name);
	}

	@Test public void partitionedAndEngine()
	{
		MySqlAbstract.MyCreateTable ct = p(""
			+ "CREATE TABLE `pt` (\n"
			+ "  `id` int NOT NULL,\n"
			+ "  `a` int DEFAULT NULL,\n"
			+ "  PRIMARY KEY (`id`),\n"
			+ "  KEY `ix_a` (`a`)\n"
			+ ") ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci\n"
			+ "/*!50100 PARTITION BY RANGE (`id`)\n"
			+ "(PARTITION p0 VALUES LESS THAN (10) ENGINE = InnoDB,\n"
			+ " PARTITION p1 VALUES LESS THAN MAXVALUE ENGINE = InnoDB) */");
		assertTrue(ct.partitioned);
		assertEquals("InnoDB", ct.engine);
		assertEquals(2, ct.keys.size());

		assertEquals("MyISAM", p("CREATE TABLE `m` (\n  `a` int\n) ENGINE=MyISAM DEFAULT CHARSET=latin1").engine);
	}

	@Test public void notShowCreateTable()
	{
		assertNull(p(null));
		assertNull(p("-- The login can't run SHOW CREATE TABLE (...)"));
	}
}
