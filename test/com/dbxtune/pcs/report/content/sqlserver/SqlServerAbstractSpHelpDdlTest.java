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
package com.dbxtune.pcs.report.content.sqlserver;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

/** The shape of the text is from real stored sp_help output, the names are made up */
public class SqlServerAbstractSpHelpDdlTest
{
	private static final String TEXT = ""
		+ "+------+-----+----------+----------------------+\n"
		+ "|Name  |Owner|Type      |Created_datetime      |\n"
		+ "+------+-----+----------+----------------------+\n"
		+ "|orders|dbo  |user table|2020-11-26 16:01:48.41|\n"
		+ "+------+-----+----------+----------------------+\n"
		+ "Rows 1\n"
		+ " \n"
		+ " \n"
		+ "+-----------+----------------+--------+------+----+-----+--------+------------------+--------------------+--------------------------------+\n"
		+ "|Column_name|Type            |Computed|Length|Prec|Scale|Nullable|TrimTrailingBlanks|FixedLenNullInSource|Collation                       |\n"
		+ "+-----------+----------------+--------+------+----+-----+--------+------------------+--------------------+--------------------------------+\n"
		+ "|id         |bigint          |no      |     8|19  |0    |no      |(n/a)             |(n/a)               |(NULL)                          |\n"
		+ "|created    |datetime2       |no      |     8|27  |3    |yes     |(n/a)             |(n/a)               |(NULL)                          |\n"
		+ "|code       |varchar         |no      |   255|    |     |yes     |no                |yes                 |Latin1_General_100_CI_AS_SC_UTF8|\n"
		+ "|note       |varchar         |no      |    -1|    |     |yes     |no                |yes                 |Latin1_General_100_CI_AS_SC_UTF8|\n"
		+ "|name       |nvarchar        |no      |   200|    |     |yes     |no                |yes                 |Finnish_Swedish_CI_AS           |\n"
		+ "|big_name   |nvarchar        |no      |    -1|    |     |yes     |no                |yes                 |Latin1_General_100_CI_AS_SC_UTF8|\n"
		+ "|flag       |nchar           |no      |     2|    |     |yes     |no                |yes                 |Latin1_General_100_CI_AS_SC_UTF8|\n"
		+ "|price      |numeric         |no      |     9|18  |2    |yes     |(n/a)             |(n/a)               |(NULL)                          |\n"
		+ "|uid        |uniqueidentifier|no      |    16|    |     |no      |(n/a)             |(n/a)               |(NULL)                          |\n"
		+ "|code_uniq  |varchar         |yes     |   255|    |     |yes     |no                |yes                 |Latin1_General_100_CI_AS_SC_UTF8|\n"
		+ "+-----------+----------------+--------+------+----+-----+--------+------------------+--------------------+--------------------------------+\n"
		+ "Rows 10\n"
		+ " \n"
		+ "+--------+----+---------+-------------------+\n"
		+ "|Identity|Seed|Increment|Not For Replication|\n"
		+ "+--------+----+---------+-------------------+\n"
		+ "|id      |   1|        1|                  0|\n"
		+ "+--------+----+---------+-------------------+\n"
		+ "Rows 1\n"
		+ " \n"
		+ "+----------+\n"
		+ "|RowGuidCol|\n"
		+ "+----------+\n"
		+ "|uid       |\n"
		+ "+----------+\n"
		+ "Rows 1\n"
		+ " \n"
		+ "+-------------------------+\n"
		+ "|Data_located_on_filegroup|\n"
		+ "+-------------------------+\n"
		+ "|FG_DATA                  |\n"
		+ "+-------------------------+\n"
		+ "Rows 1\n"
		+ " \n"
		+ "+-----------------------+----------------+-------------+-------------+--------------+----------------------+------------------------------+\n"
		+ "|constraint_type        |constraint_name |delete_action|update_action|status_enabled|status_for_replication|constraint_keys               |\n"
		+ "+-----------------------+----------------+-------------+-------------+--------------+----------------------+------------------------------+\n"
		+ "|CHECK Table Level      |ck_price        |(n/a)        |(n/a)        |Enabled       |Is_For_Replication    |([price]>(0))                 |\n"
		+ "|DEFAULT on column note |df_note         |(n/a)        |(n/a)        |(n/a)         |(n/a)                 |('[]')                        |\n"
		+ "|FOREIGN KEY            |fk_cust         |Cascade      |No Action    |Enabled       |Is_For_Replication    |cust_id                       |\n"
		+ "|                       |                |             |             |              |                      |REFERENCES shop.dbo.cust (id) |\n"
		+ "|FOREIGN KEY            |fk_item         |No Action    |No Action    |Disabled      |Is_For_Replication    |item_id, item_ver             |\n"
		+ "|                       |                |             |             |              |                      |REFERENCES shop.dbo.item (a, b)|\n"
		+ "|PRIMARY KEY (clustered)|pk_orders       |(n/a)        |(n/a)        |(n/a)         |(n/a)                 |id                            |\n"
		+ "+-----------------------+----------------+-------------+-------------+--------------+----------------------+------------------------------+\n"
		+ "Rows 7\n"
		+ " \n"
		+ "+----------------------------------+\n"
		+ "|Table is referenced by foreign key|\n"
		+ "+----------------------------------+\n"
		+ "|shop.dbo.order_line: fk_order1    |\n"
		+ "|shop.dbo.order_line: fk_order2    |\n"
		+ "|shop.dbo.invoice: fk_order3       |\n"
		+ "+----------------------------------+\n"
		+ "Rows 3\n";

	@Test
	public void testTableDdl()
	{
		assertEquals(""
				+ "-- Collation of text columns: Latin1_General_100_CI_AS_SC_UTF8\n"
				+ "CREATE TABLE shop.dbo.orders (\n"
				+ "  id bigint NOT NULL IDENTITY(1,1),\n"
				+ "  created datetime2(3) NULL,\n"
				+ "  code varchar(255) NULL,\n"
				+ "  note varchar(max) NULL,\n"
				+ "  name nvarchar(100) COLLATE Finnish_Swedish_CI_AS NULL,\n"
				+ "  big_name nvarchar(max) NULL,\n"
				+ "  flag nchar(1) NULL,\n"
				+ "  price numeric(18,2) NULL,\n"
				+ "  uid uniqueidentifier NOT NULL ROWGUIDCOL,\n"
				+ "  code_uniq varchar(255) NULL /*computed*/\n"
				+ ") ON FG_DATA\n"
				+ "CHECK ([price]>(0))\n"
				+ "FOREIGN KEY (cust_id) REFERENCES shop.dbo.cust (id) ON DELETE CASCADE\n"
				+ "FOREIGN KEY (item_id, item_ver) REFERENCES shop.dbo.item (a, b) /*disabled*/\n"
				+ "-- Referenced by foreign keys in: shop.dbo.order_line, shop.dbo.invoice",
				SqlServerAbstract.formatObjectTextAsDdl("shop.dbo.orders", TEXT));
	}

	/** The Collector now stores the expression of a computed column, and an empty 'Identity' etc isn't stored */
	@Test
	public void testComputedDefinition()
	{
		String text = ""
			+ "+-----------+------+--------+------+----+-----+--------+------------------+--------------------+---------+-------------------+\n"
			+ "|Column_name|Type  |Computed|Length|Prec|Scale|Nullable|TrimTrailingBlanks|FixedLenNullInSource|Collation|Computed_definition|\n"
			+ "+-----------+------+--------+------+----+-----+--------+------------------+--------------------+---------+-------------------+\n"
			+ "|a          |int   |no      |     4|10  |0    |no      |(n/a)             |(n/a)               |(NULL)   |(NULL)             |\n"
			+ "|b          |int   |yes     |     4|10  |0    |yes     |(n/a)             |(n/a)               |(NULL)   |([a]*(2))          |\n"
			+ "+-----------+------+--------+------+----+-----+--------+------------------+--------------------+---------+-------------------+\n"
			+ "Rows 2\n";

		assertEquals("CREATE TABLE shop.dbo.t (\n  a int NOT NULL,\n  b int NULL /*computed: ([a]*(2))*/\n)",
				SqlServerAbstract.formatObjectTextAsDdl("shop.dbo.t", text));
	}

	@Test
	public void testIndexes()
	{
		assertEquals("CREATE UNIQUE CLUSTERED INDEX pk ON t ([id]) /*primary key*/ -- 22.5 MB",
				SqlServerAbstract.formatIndexAsDdl("t", "pk", "clustered, unique, primary key located on PRIMARY", "[id]", "(NULL)", "(NULL)", 0, "NONE", 1, null, false, 23040));

		assertEquals("CREATE NONCLUSTERED INDEX ix ON t ([a], [b]) INCLUDE ([c]) WHERE ([d] IS NOT NULL) WITH (FILLFACTOR = 90) ON FG_IDX /*disabled*/ -- <0.1 MB",
				SqlServerAbstract.formatIndexAsDdl("t", "ix", "nonclustered located on FG_IDX", "[a], [b]", "[c]", "([d] IS NOT NULL)", 90, null, 1, null, true, 8));

		assertEquals("CREATE UNIQUE NONCLUSTERED INDEX uq ON t ([a]) /*unique constraint*/ -- 1.0 MB",
				SqlServerAbstract.formatIndexAsDdl("t", "uq", "nonclustered, unique, unique key located on PRIMARY", "[a]", "-", "-", 0, null, 1, null, false, 1024));

		// A clustered columnstore has all columns as "include": no use repeating them, and it's always compressed
		assertEquals("CREATE CLUSTERED COLUMNSTORE INDEX cci ON t -- 1.0 MB",
				SqlServerAbstract.formatIndexAsDdl("t", "cci", "clustered, columnstore located on PRIMARY", "(NULL)", "[a], [b], [c]", "(NULL)", 0, "COLUMNSTORE", 1, null, false, 1024));
	}

	@Test
	public void testColumnstoreIndexes()
	{
		// Nonclustered: the columns are stored as "include", and there are no keys
		assertEquals("CREATE NONCLUSTERED COLUMNSTORE INDEX ncci ON t ([a], [b]) -- 1.0 MB",
				SqlServerAbstract.formatIndexAsDdl("t", "ncci", "nonclustered, columnstore located on PRIMARY", "-", "[a], [b]", "(NULL)", 0, "COLUMNSTORE", 1, null, false, 1024));

		// Archive compression is a choice, so it's worth telling
		assertEquals("CREATE CLUSTERED COLUMNSTORE INDEX cci ON t WITH (DATA_COMPRESSION = COLUMNSTORE_ARCHIVE) /*partitions=4*/ -- 1.0 MB",
				SqlServerAbstract.formatIndexAsDdl("t", "cci", "clustered, columnstore located on PRIMARY", "(NULL)", "[a], [b]", "(NULL)", 0, "COLUMNSTORE_ARCHIVE", 4, null, false, 1024));

		// Only the Collector's IndexType: sp_help had no row for the index
		assertEquals("CREATE CLUSTERED COLUMNSTORE INDEX cci ON t -- 1.0 MB",
				SqlServerAbstract.formatIndexAsDdl("t", "cci", "CLUSTERED COLUMNSTORE", "(NULL)", "[a], [b]", "(NULL)", 0, null, 1, null, false, 1024));
		assertEquals("CREATE NONCLUSTERED COLUMNSTORE INDEX ncci ON t ([a]) -- 1.0 MB",
				SqlServerAbstract.formatIndexAsDdl("t", "ncci", "NONCLUSTERED COLUMNSTORE", "(NULL)", "[a]", "(NULL)", 0, null, 1, null, false, 1024));
	}

	@Test
	public void testIndexOptions()
	{
		// The options that the Collector only writes when they are not default, and the rest of its text says nothing
		String ddlText = "CREATE  UNIQUE NONCLUSTERED INDEX [ix] ON [db].[dbo].[t]([a]) WITH (PAD_INDEX = ON, FILLFACTOR = 100, SORT_IN_TEMPDB = OFF, IGNORE_DUP_KEY = ON, ALLOW_ROW_LOCKS = OFF, ALLOW_PAGE_LOCKS = OFF, DATA_COMPRESSION = PAGE, MAXDOP = 0, ONLINE = OFF)";

		assertEquals("CREATE UNIQUE NONCLUSTERED INDEX ix ON t ([a]) WITH (PAD_INDEX = ON, IGNORE_DUP_KEY = ON, ALLOW_ROW_LOCKS = OFF, ALLOW_PAGE_LOCKS = OFF, DATA_COMPRESSION = PAGE) /*partitions=4*/ -- 1.0 MB",
				SqlServerAbstract.formatIndexAsDdl("t", "ix", "nonclustered, unique located on PRIMARY", "[a]", "(NULL)", "(NULL)", 0, "PAGE", 4, ddlText, false, 1024));

		// Text stored by an older Collector: has the question mark, and no compression value
		assertEquals("CREATE NONCLUSTERED INDEX ix ON t ([a]) -- 1.0 MB",
				SqlServerAbstract.formatIndexAsDdl("t", "ix", "nonclustered located on PRIMARY", "[a]", "(NULL)", "(NULL)", 0, null, 1, "CREATE NONCLUSTERED INDEX [ix] ON [db].[dbo].[t]([a]) WITH (FILLFACTOR = 100, SORT_IN_TEMPDB = OFF, DATA_COMPRESSION = ?, MAXDOP = 0, ONLINE = OFF)", false, 1024));
	}
}
