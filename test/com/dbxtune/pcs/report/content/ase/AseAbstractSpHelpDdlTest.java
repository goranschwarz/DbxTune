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
package com.dbxtune.pcs.report.content.ase;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

public class AseAbstractSpHelpDdlTest
{
	private static final String COLS_HEADER = ""
		+ " Column_name Type           Length Prec Scale Nulls Not_compressed Default_name   Rule_name Access_Rule_name Computed_Column_object Identity  \n"
		+ " ----------- -------------- ------ ---- ----- ----- -------------- -------------- --------- ---------------- ---------------------- ----------\n";

	private static String col(String name, String type, int length, String prec, String scale, int nulls, int identity)
	{
		return String.format(" %-11s %-14s %6d %-4s %-5s %5d %14d %-14s %-9s %-16s %-22s %10d\n", name, type, length, prec, scale, nulls, 0, "NULL", "NULL", "NULL", "NULL", identity);
	}

	private static final String TAIL = ""
		+ "No defined keys for this object.\n"
		+ " name        type       partition_type partitions partition_keys\n"
		+ " ----------- ---------- -------------- ---------- --------------\n"
		+ " dbo.Setting base table roundrobin              1 NULL          \n"
		+ " \n"
		+ " partition_name    partition_id compression_level pages row_count segment create_date        \n"
		+ " ----------------- ------------ ----------------- ----- --------- ------- -------------------\n"
		+ " Setting_924527296    924527296 none                  2       123 default Mar 24 2017  1:38AM\n"
		+ " \n"
		+ " Partition_Conditions\n"
		+ " --------------------\n"
		+ " NULL                \n"
		+ " \n"
		+ " Avg_pages   Max_pages   Min_pages   Ratio(Max/Avg) Ratio(Min/Avg)\n"
		+ " ----------- ----------- ----------- -------------- --------------\n"
		+ "           2           2           2              1              1\n"
		+ "Table LOB compression level 0\n"
		+ "Lock scheme Datarows\n"
		+ "The 'ascinserts' attribute is not applicable to tables with datarow or datapage lock schemes.\n"
		+ " \n"
		+ " exp_row_size reservepagegap fillfactor max_rows_per_page identity_gap ascinserts \n"
		+ " ------------ -------------- ---------- ----------------- ------------ -----------\n"
		+ "          839              0          0                 1            0           0\n"
		+ " concurrency_opt_threshold optimistic_index_lock dealloc_first_txtpg recompile_factor plldegree read_only   unused_pages_threshold\n"
		+ " ------------------------- --------------------- ------------------- ---------------- --------- ----------- ----------------------\n"
		+ "                        15                     0                   0 NULL                     0           0 NULL                  \n";

	private static final String INDEXES = ""
		+ "Object has the following indexes\n"
		+ " \n"
		+ " index_name index_keys          index_description               index_max_rows_per_page index_fillfactor index_reservepagegap index_created       index_local  plldegree disabled\n"
		+ " ---------- ------------------- ------------------------------- ----------------------- ---------------- -------------------- ------------------- ------------ --------- --------\n"
		+ " PK_SETTING  xID                nonclustered, unique                                  0                0                    0 Mar 24 2017  1:38AM Global Index         0        0\n"
		+ " mkl_Name    Efternamn, Fornamn clustered, allow duplicate rows                       1                0                    0 Jan  7 2026 12:21PM Global Index         0        0\n"
		+ " index_ptn_name       index_ptn_seg index_ptn_comp    \n"
		+ " -------------------- ------------- ------------------\n"
		+ " PK_SETTING_924527296 default       inherit from index\n"
		+ " mkl_Name_924527296   system        inherit from index\n";

	private static String ddl(String text)
	{
		return AseAbstract.formatObjectTextAsDdl("PML.dbo.Setting", text, null);
	}

	@Test
	public void testFallbackToRawText()
	{
		assertNull(ddl(null));
		assertNull(ddl("java.sql.SQLException: Some problem"));
	}

	@Test
	public void testColumnsTableOptionsAndIndexes()
	{
		String out = ddl(COLS_HEADER
				+ col("xID"  , "int"        , 4 , "NULL", "NULL", 0, 1)
				+ col("name" , "varchar"    , 80, "NULL", "NULL", 1, 0)
				+ col("price", "numeric"    , 7 , "10"  , "2"   , 1, 0)
				+ INDEXES + TAIL);

		assertTrue(out, out.contains("-- Data pages: 2\n"));
		assertTrue(out, out.contains("CREATE TABLE PML.dbo.Setting (\n  xID int IDENTITY,\n  name varchar(80) NULL,\n  price numeric(10,2) NULL\n)"));
		assertTrue(out, out.contains(") LOCK DATAROWS WITH exp_row_size=839, max_rows_per_page=1\n"));
		assertTrue(out, out.contains("CREATE UNIQUE NONCLUSTERED INDEX PK_SETTING ON Setting (xID)\n"));
		assertTrue(out, out.contains("CREATE CLUSTERED INDEX mkl_Name ON Setting (Efternamn, Fornamn) WITH allow_dup_row, max_rows_per_page=1 ON system"));

		assertFalse(out, out.contains("PARTITION BY"));
		assertFalse(out, out.contains("not applicable"));
		assertFalse(out, out.contains("No defined keys"));
		assertFalse(out, out.contains("Avg_pages"));
		assertFalse(out, out.contains("concurrency_opt_threshold"));
	}

	private static void assertGuess(String expected, String userType, int length)
	{
		String out = ddl(COLS_HEADER + col("c1", userType, length, "NULL", "NULL", 1, 0) + TAIL);
		assertTrue(out, out.contains("  c1 " + userType + " /*guess: " + expected + "*/ NULL\n"));
	}

	@Test
	public void testGuessUserDatatypes()
	{
		// A word in the name, and the length fits
		assertGuess("int"        , "udt_id"       , 4);
		assertGuess("int"        , "MyLongInt"    , 4);
		assertGuess("bit"        , "t_boolean"    , 1);
		assertGuess("datetime"   , "x_datetime"   , 8);
		assertGuess("text"       , "t_text"       , 16);

		// The length rules out what the name says
		assertGuess("smallint"   , "t_integer"    , 2);
		assertGuess("varchar(10)", "t_date"       , 10);
		assertGuess("varchar(8)" , "t_time"       , 8);
		assertGuess("varchar(12)", "t_timestamp"  , 12);

		// A string word wins over the length, and parts of words don't count
		assertGuess("varchar(4)" , "t_str04"      , 4);
		assertGuess("varchar(4)" , "update_code"  , 4);

		// Nothing in the name: varchar
		assertGuess("varchar(4)" , "customer_code", 4);
		assertGuess("varchar(30)", "t_user"       , 30);
	}

	@Test
	public void testGuessUserDatatypeWithPrecision()
	{
		String out = ddl(COLS_HEADER + col("c1", "t_amount", 7, "12", "2", 1, 0) + TAIL);
		assertTrue(out, out.contains("  c1 t_amount /*guess: numeric(12,2)*/ NULL\n"));
	}

	@Test
	public void testRangePartitions()
	{
		String tail = ""
			+ " name   type       partition_type partitions partition_keys\n"
			+ " ------ ---------- -------------- ---------- --------------\n"
			+ " titles base table range                   2 pubdate       \n"
			+ " \n"
			+ " partition_name partition_id compression_level pages row_count segment create_date        \n"
			+ " -------------- ------------ ----------------- ----- --------- ------- -------------------\n"
			+ " q1                937051343 none                  5        10 default Oct 13 2005  5:20PM\n"
			+ " q2                953051400 none                  7        10 seg2    Oct 13 2005  5:20PM\n"
			+ " \n"
			+ " Partition_Conditions   \n"
			+ " -----------------------\n"
			+ " VALUES <= (\"3/31/2006\")\n"
			+ " VALUES <= (\"6/30/2006\")\n"
			+ "Lock scheme Allpages\n";

		String out = ddl(COLS_HEADER + col("pubdate", "datetime", 8, "NULL", "NULL", 0, 0) + tail);

		assertEquals("-- Data pages: 12\n"
				+ "CREATE TABLE PML.dbo.Setting (\n  pubdate datetime NOT NULL\n) LOCK ALLPAGES\n"
				+ "PARTITION BY RANGE (pubdate) (\n"
				+ "  q1 VALUES <= (\"3/31/2006\") /*pages=5*/,\n"
				+ "  q2 VALUES <= (\"6/30/2006\") ON seg2 /*pages=7*/\n"
				+ ")", out);
	}

	/** Values from: sp_help on a real ASE 16.0 SP04 table created with: lock datarows partition by roundrobin 3 */
	@Test
	public void testRoundRobinPartitions()
	{
		String tail = ""
			+ "Object does not have any indexes.\n"
			+ "No defined keys for this object.\n"
			+ " name      type       partition_type partitions partition_keys\n"
			+ " --------- ---------- -------------- ---------- --------------\n"
			+ " zz_ddl_rr base table roundrobin              3 NULL          \n"
			+ " \n"
			+ " partition_name       partition_id compression_level pages row_count segment create_date        \n"
			+ " -------------------- ------------ ----------------- ----- --------- ------- -------------------\n"
			+ " zz_ddl_rr_988771840     988771840 none                  1         0 default Nov  3 2026  8:00PM\n"
			+ " zz_ddl_rr_1004771897   1004771897 none                  1         0 default Nov  3 2026  8:00PM\n"
			+ " zz_ddl_rr_1020771954   1020771954 none                  1         0 default Nov  3 2026  8:00PM\n"
			+ " \n"
			+ " Partition_Conditions\n"
			+ " --------------------\n"
			+ " NULL                \n"
			+ " \n"
			+ " Avg_pages Max_pages Min_pages Ratio(Max/Avg) Ratio(Min/Avg)\n"
			+ " --------- --------- --------- -------------- --------------\n"
			+ "         1         1         1              1              1\n"
			+ "Table LOB compression level 0\n"
			+ "Lock scheme Datarows\n"
			+ "The 'ascinserts' attribute is not applicable to tables with datarow or datapage lock schemes.\n"
			+ " \n"
			+ " exp_row_size reservepagegap fillfactor max_rows_per_page identity_gap ascinserts\n"
			+ " ------------ -------------- ---------- ----------------- ------------ ----------\n"
			+ "            1              0          0                 0            0          0\n"
			+ " concurrency_opt_threshold optimistic_index_lock dealloc_first_txtpg cached_index_root_page recompile_factor plldegree\n"
			+ " ------------------------- --------------------- ------------------- ---------------------- ---------------- ---------\n"
			+ "                        15                     0                   0                      0 NULL                     0\n";

		String out = ddl(COLS_HEADER + col("id", "int", 4, "NULL", "NULL", 0, 0) + tail);

		assertEquals("-- Data pages: 3\n"
				+ "CREATE TABLE PML.dbo.Setting (\n  id int NOT NULL\n) LOCK DATAROWS\n"
				+ "PARTITION BY ROUNDROBIN (\n"
				+ "  zz_ddl_rr_988771840 /*pages=1*/,\n"
				+ "  zz_ddl_rr_1004771897 /*pages=1*/,\n"
				+ "  zz_ddl_rr_1020771954 /*pages=1*/\n"
				+ ")", out);
	}

	/** Text from: sp_help on a real ASE 16.0 SP04, for an allpages table with user datatypes (also has the "rows affected" lines) */
	@Test
	public void testRealAllpagesWithUserDatatypes()
	{
		String text = ""
			+ " Name       Owner Object_type Object_status Create_date        \n"
			+ " ---------- ----- ----------- ------------- -------------------\n"
			+ " zz_ddl_apl dbo   user table   -- none --   Nov  3 2026  8:06PM\n"
			+ "\n"
			+ "(1 rows affected)\n"
			+ "\n"
			+ " Column_name Type            Length Prec Scale Nulls Not_compressed Default_name Rule_name Access_Rule_name Computed_Column_object Identity  \n"
			+ " ----------- --------------- ------ ---- ----- ----- -------------- ------------ --------- ---------------- ---------------------- ----------\n"
			+ " id          zz_ddl_t_id          4 NULL  NULL     0              0 NULL         NULL      NULL             NULL                            0\n"
			+ " d1          zz_ddl_t_date       10 NULL  NULL     1              0 NULL         NULL      NULL             NULL                            0\n"
			+ " amount      zz_ddl_t_amount      6   12     2     1              0 NULL         NULL      NULL             NULL                            0\n"
			+ "Object does not have any indexes.\n"
			+ "No defined keys for this object.\n"
			+ "(3 rows affected)\n"
			+ " name       type       partition_type partitions partition_keys\n"
			+ " ---------- ---------- -------------- ---------- --------------\n"
			+ " zz_ddl_apl base table roundrobin              1 NULL          \n"
			+ " \n"
			+ "(1 rows affected)\n"
			+ " partition_name       partition_id compression_level pages row_count segment create_date        \n"
			+ " -------------------- ------------ ----------------- ----- --------- ------- -------------------\n"
			+ " zz_ddl_apl_341817540    341817540 none                  1         0 default Nov  3 2026  8:06PM\n"
			+ " \n"
			+ "(1 rows affected)\n"
			+ " Partition_Conditions\n"
			+ " --------------------\n"
			+ " NULL                \n"
			+ " \n"
			+ "(1 rows affected)\n"
			+ "Table LOB compression level 0\n"
			+ "Lock scheme Allpages\n"
			+ "The attribute 'exp_row_size' is not applicable to tables with allpages lock scheme.\n"
			+ "The attribute 'concurrency_opt_threshold' is not applicable to tables with allpages lock scheme.\n"
			+ " \n"
			+ "(1 rows affected)\n"
			+ " exp_row_size reservepagegap fillfactor max_rows_per_page identity_gap ascinserts \n"
			+ " ------------ -------------- ---------- ----------------- ------------ -----------\n"
			+ "            0              0          0                 0            0           0\n"
			+ "\n"
			+ "(1 rows affected)\n"
			+ "\n"
			+ " concurrency_opt_threshold optimistic_index_lock dealloc_first_txtpg cached_index_root_page recompile_factor                       plldegree\n"
			+ " ------------------------- --------------------- ------------------- ---------------------- -------------------------------------- ---------\n"
			+ "                         0                     0                   0                      0 NULL                                           0\n"
			+ "\n"
			+ "(1 rows affected)\n";

		assertEquals("-- Data pages: 1\n"
				+ "CREATE TABLE tempdb.dbo.zz_ddl_apl (\n"
				+ "  id zz_ddl_t_id /*guess: int*/ NOT NULL,\n"
				+ "  d1 zz_ddl_t_date /*guess: varchar(10)*/ NULL,\n"
				+ "  amount zz_ddl_t_amount /*guess: numeric(12,2)*/ NULL\n"
				+ ") LOCK ALLPAGES", AseAbstract.formatObjectTextAsDdl("tempdb.dbo.zz_ddl_apl", text, null));
	}

	/** Text from: sp_help on a real ASE 16.0 SP04 (the segment 'seg1' is changed by hand), for a local index with one partition per table partition */
	@Test
	public void testRealRoundRobinWithLocalAndGlobalIndex()
	{
		String text = ""
			+ " Column_name Type    Length Prec Scale Nulls Not_compressed Default_name Rule_name Access_Rule_name Computed_Column_object Identity  \n"
			+ " ----------- ------- ------ ---- ----- ----- -------------- ------------ --------- ---------------- ---------------------- ----------\n"
			+ " id          int          4 NULL  NULL     0              0 NULL         NULL      NULL             NULL                            0\n"
			+ "Object has the following indexes\n"
			+ " \n"
			+ "(2 rows affected)\n"
			+ " index_name   index_keys index_description index_max_rows_per_page index_fillfactor index_reservepagegap index_created       index_local  plldegree disabled\n"
			+ " ------------ ---------- ----------------- ----------------------- ---------------- -------------------- ------------------- ------------ --------- --------\n"
			+ " zz_ix_local   id        nonclustered                            0                0                    0 Nov  3 2026  8:06PM Local Index          0        0\n"
			+ " zz_ix_global  name      nonclustered                            0                0                    0 Nov  3 2026  8:06PM Global Index         0        0\n"
			+ "\n"
			+ "(2 rows affected)\n"
			+ "\n"
			+ " index_ptn_name         index_ptn_seg index_ptn_comp    \n"
			+ " ---------------------- ------------- ------------------\n"
			+ " zz_ix_local_677818737  seg1          inherit from index\n"
			+ " zz_ix_local_693818794  seg1          inherit from index\n"
			+ " zz_ix_local_709818851  seg1          inherit from index\n"
			+ " zz_ix_global_725818908 default       inherit from index\n"
			+ "No defined keys for this object.\n";

		String out = AseAbstract.formatObjectTextAsDdl("tempdb.dbo.zz_ddl_rr", text, null);

		assertTrue(out, out.contains("CREATE NONCLUSTERED INDEX zz_ix_local ON zz_ddl_rr (id) LOCAL INDEX ON seg1\n"));
		assertTrue(out, out.endsWith("CREATE NONCLUSTERED INDEX zz_ix_global ON zz_ddl_rr (name)"));
	}

	/** Text from: sp_help on a real ASE 16.0 SP04, with most column kinds, a function-based index and sp_primarykey */
	@Test
	public void testRealColumnKinds()
	{
		String text = ""
			+ " Name        Owner Object_type Object_status        Create_date        \n"
			+ " ----------- ----- ----------- -------------------- -------------------\n"
			+ " zz_ddl_cols dbo   user table  keep first text page Nov  3 2026  8:08PM\n"
			+ "\n"
			+ "(1 rows affected)\n"
			+ "\n"
			+ " Column_name Type          Length In_row_Len Prec Scale Nulls Not_compressed Default_name   Rule_name   Access_Rule_name Computed_Column_object                             Identity   Object_storage Varbinary_is_truncated\n"
			+ " ----------- ------------- ------ ---------- ---- ----- ----- -------------- -------------- ----------- ---------------- -------------------------------------------------- ---------- -------------- ----------------------\n"
			+ " id          numeric            6       NULL   10     0     0              0 NULL           NULL        NULL             NULL                                                        1 NULL                             NULL\n"
			+ " code        zz_ddl_t_code      4       NULL NULL  NULL     1              0 NULL           NULL        NULL             NULL                                                        0 NULL                             NULL\n"
			+ " n_name      nvarchar          20       NULL NULL  NULL     1              0 NULL           NULL        NULL             NULL                                                        0 NULL                             NULL\n"
			+ " u_name      univarchar        20       NULL NULL  NULL     1              0 NULL           NULL        NULL             NULL                                                        0 NULL                             NULL\n"
			+ " u_fix       unichar            5       NULL NULL  NULL     1              0 NULL           NULL        NULL             NULL                                                        0 NULL                             NULL\n"
			+ " nc_fix      nchar              5       NULL NULL  NULL     1              0 NULL           NULL        NULL             NULL                                                        0 NULL                             NULL\n"
			+ " bin         varbinary         16       NULL NULL  NULL     1              0 NULL           NULL        NULL             NULL                                                        0 NULL                             NULL\n"
			+ " price       money              8       NULL NULL  NULL     1              0 NULL           NULL        NULL             NULL                                                        0 NULL                             NULL\n"
			+ " d1          bigdatetime        8       NULL NULL  NULL     1              0 NULL           NULL        NULL             NULL                                                        0 NULL                             NULL\n"
			+ " t1          bigtime            8       NULL NULL  NULL     1              0 NULL           NULL        NULL             NULL                                                        0 NULL                             NULL\n"
			+ " ui          unsigned int       4       NULL NULL  NULL     1              0 NULL           NULL        NULL             NULL                                                        0 NULL                             NULL\n"
			+ " ts          timestamp          8       NULL NULL  NULL     1              0 NULL           NULL        NULL             NULL                                                        0 NULL                             NULL\n"
			+ " note        text              16        500 NULL  NULL     1              0 NULL           NULL        NULL             NULL                                                        0 in row                           NULL\n"
			+ " pic         image             16       NULL NULL  NULL     1              0 NULL           NULL        NULL             NULL                                                        0 NULL                             NULL\n"
			+ " calc        numeric           10       NULL   21     0     1              0 NULL           NULL        NULL             zz_ddl_col_calc_482342043 (virtual)                         0 NULL                             NULL\n"
			+ " calc_mat    numeric           10       NULL   21     0     1              0 NULL           NULL        NULL             zz_ddl_col_calc_m_498342100 (materialized)                  0 NULL                             NULL\n"
			+ " ruled       int                4       NULL NULL  NULL     1              0 NULL           zz_ddl_rule NULL             NULL                                                        0 NULL                             NULL\n"
			+ " defaulted   int                4       NULL NULL  NULL     1              0 zz_ddl_default NULL        NULL             NULL                                                        0 NULL                             NULL\n"
			+ " parent_id   int                4       NULL NULL  NULL     1              0 NULL           NULL        NULL             NULL                                                        0 NULL                             NULL\n"
			+ " sybfi3_1    int                4       NULL NULL  NULL     1              0 NULL           NULL        NULL             zz_ddl_col_sybfi3_578342385 (functional index key)          0 NULL                             NULL\n"
			+ " \n"
			+ "Object has the following computed columns\n"
			+ " \n"
			+ "(20 rows affected)\n"
			+ " Column_Name   Property     \n"
			+ " ------------- -------------\n"
			+ " calc          virtual      \n"
			+ " \n"
			+ "(1 rows affected)\n"
			+ " Text      \n"
			+ " ----------\n"
			+ " AS  id * 2\n"
			+ " \n"
			+ "(1 rows affected)\n"
			+ " Column_Name   Property     \n"
			+ " ------------- -------------\n"
			+ " calc_mat      materialized \n"
			+ " \n"
			+ "(1 rows affected)\n"
			+ " Text                   \n"
			+ " -----------------------\n"
			+ " AS  id * 3 materialized\n"
			+ " \n"
			+ "Object has the following indexes\n"
			+ " \n"
			+ "(1 rows affected)\n"
			+ " index_name index_keys     index_description                           index_max_rows_per_page index_fillfactor index_reservepagegap index_created       index_local  plldegree disabled\n"
			+ " ---------- -------------- ------------------------------------------- ----------------------- ---------------- -------------------- ------------------- ------------ --------- --------\n"
			+ " zz_ix_opts  code DESC, id nonclustered, unique, ignore duplicate keys                       0                0                    8 Nov  3 2026  8:08PM Global Index         0        0\n"
			+ " zz_ix_func  sybfi3_1      nonclustered                                                      0                0                    0 Nov  3 2026  8:08PM Global Index         0        0\n"
			+ "\n"
			+ "(2 rows affected)\n"
			+ "\n"
			+ " index_ptn_name       index_ptn_seg index_ptn_comp    \n"
			+ " -------------------- ------------- ------------------\n"
			+ " zz_ix_opts_466341986 default       inherit from index\n"
			+ " zz_ix_func_466341986 default       inherit from index\n"
			+ " \n"
			+ "Object has the following functional index keys\n"
			+ " \n"
			+ "(2 rows affected)\n"
			+ " Internal_Index_Key_Name  \n"
			+ " -------------------------\n"
			+ " sybfi3_1                 \n"
			+ " \n"
			+ "(1 rows affected)\n"
			+ " Expression\n"
			+ " ----------\n"
			+ " ruled * 2 \n"
			+ " \n"
			+ "(1 rows affected)\n"
			+ " keytype object      related_object object_keys             related_keys          \n"
			+ " ------- ----------- -------------- ----------------------- ----------------------\n"
			+ " primary zz_ddl_cols  -- none --    id, *, *, *, *, *, *, * *, *, *, *, *, *, *, *\n"
			+ "\n"
			+ "(1 rows affected)\n"
			+ "\n"
			+ " name        type       partition_type partitions partition_keys\n"
			+ " ----------- ---------- -------------- ---------- --------------\n"
			+ " zz_ddl_cols base table roundrobin              1 NULL          \n"
			+ " \n"
			+ "(1 rows affected)\n"
			+ " partition_name        partition_id compression_level pages row_count segment create_date        \n"
			+ " --------------------- ------------ ----------------- ----- --------- ------- -------------------\n"
			+ " zz_ddl_cols_466341986    466341986 none                  1         0 default Nov  3 2026  8:08PM\n"
			+ " \n"
			+ "(1 rows affected)\n"
			+ " Partition_Conditions\n"
			+ " --------------------\n"
			+ " NULL                \n"
			+ " \n"
			+ "(1 rows affected)\n"
			+ "Table LOB compression level 0\n"
			+ "Lock scheme Datarows\n"
			+ "The 'ascinserts' attribute is not applicable to tables with datarow or datapage lock schemes.\n"
			+ " \n"
			+ "(1 rows affected)\n"
			+ " exp_row_size reservepagegap fillfactor max_rows_per_page identity_gap ascinserts \n"
			+ " ------------ -------------- ---------- ----------------- ------------ -----------\n"
			+ "          100             16          0                 0           10           0\n"
			+ "\n"
			+ "(1 rows affected)\n"
			+ "\n"
			+ " concurrency_opt_threshold optimistic_index_lock dealloc_first_txtpg cached_index_root_page recompile_factor                       plldegree\n"
			+ " ------------------------- --------------------- ------------------- ---------------------- -------------------------------------- ---------\n"
			+ "                        20                     0                   0                      0 NULL                                           0\n"
			+ "\n"
			+ "(1 rows affected)\n";

		assertEquals("-- Object status: keep first text page\n"
				+ "-- Data pages: 1\n"
				+ "CREATE TABLE tempdb.dbo.zz_ddl_cols (\n"
				+ "  id numeric(10,0) IDENTITY,\n"
				+ "  code zz_ddl_t_code /*guess: varchar(4)*/ NULL,\n"
				+ "  n_name nvarchar(20) NULL,\n"
				+ "  u_name univarchar(20) NULL,\n"
				+ "  u_fix unichar(5) NULL,\n"
				+ "  nc_fix nchar(5) NULL,\n"
				+ "  bin varbinary(16) NULL,\n"
				+ "  price money NULL,\n"
				+ "  d1 bigdatetime NULL,\n"
				+ "  t1 bigtime NULL,\n"
				+ "  ui unsigned int NULL,\n"
				+ "  ts timestamp NULL,\n"
				+ "  note text NULL IN ROW (500),\n"
				+ "  pic image NULL,\n"
				+ "  calc numeric(21,0) NULL /*computed: id * 2*/,\n"
				+ "  calc_mat numeric(21,0) NULL /*computed: id * 3 materialized*/,\n"
				+ "  ruled int NULL /*rule: zz_ddl_rule*/,\n"
				+ "  defaulted int NULL,\n"
				+ "  parent_id int NULL\n"
				+ ") LOCK DATAROWS WITH exp_row_size=100, reservepagegap=16, identity_gap=10, concurrency_opt_threshold=20\n"
				+ "CREATE UNIQUE NONCLUSTERED INDEX zz_ix_opts ON zz_ddl_cols (code DESC, id) WITH ignore_dup_key, reservepagegap=8\n"
				+ "CREATE NONCLUSTERED INDEX zz_ix_func ON zz_ddl_cols (ruled * 2)\n"
				+ "-- primary key: zz_ddl_cols (id)", AseAbstract.formatObjectTextAsDdl("tempdb.dbo.zz_ddl_cols", text, null));
	}

	@Test
	public void testUnknownOutputIsPassedOn()
	{
		String out = ddl(COLS_HEADER + col("c1", "int", 4, "NULL", "NULL", 0, 0) + TAIL
				+ "Some new message in a later ASE version\n"
				+ " new_col other_col\n"
				+ " ------- ---------\n"
				+ " a       b        \n");

		assertTrue(out, out.contains("Some new message in a later ASE version\n"));
		assertTrue(out, out.contains(" new_col other_col\n"));
		assertTrue(out, out.contains(" a       b"));
	}
}
