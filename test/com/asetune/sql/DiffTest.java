/**********************************"*********************************************
 * Copyright (C) 2010-2019 Goran Schwarz
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
package com.asetune.sql;

import static org.junit.Assert.assertEquals;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

import org.apache.log4j.PropertyConfigurator;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import com.asetune.gui.ResultSetTableModel;
import com.asetune.sql.conn.DbxConnection;
import com.asetune.sql.diff.DiffContext;
import com.asetune.sql.diff.DiffContext.DiffSide;
import com.asetune.sql.diff.actions.GenerateSqlText;
import com.asetune.sql.diff.DiffTable;
import com.asetune.tools.sqlw.msg.JPipeMessage;
import com.asetune.tools.sqlw.msg.Message;

public class DiffTest
{
//	private static final String DB_DRIVER     = "org.h2.Driver";
//	private static final String DB_URL        = "jdbc:h2:mem:test;DB_CLOSE_DELAY=-1";
	private static final String DB_URL        = "jdbc:h2:mem:test";
	private static final String DB_USER       = "sa";
	private static final String DB_PASSWORD   = "";

	private DbxConnection _conn; 
			

	@BeforeClass
	public static void init() 
	{
		//System.out.println("@BeforeClass - init():");

		Properties log4jProps = new Properties();
		log4jProps.setProperty("log4j.rootLogger", "INFO, A1");
//		log4jProps.setProperty("log4j.rootLogger", "TRACE, A1");
		log4jProps.setProperty("log4j.appender.A1", "org.apache.log4j.ConsoleAppender");
		log4jProps.setProperty("log4j.appender.A1.layout", "org.apache.log4j.PatternLayout");
		log4jProps.setProperty("log4j.appender.A1.layout.ConversionPattern", "%d - %-5p - %-30c{1} - %m%n");
		PropertyConfigurator.configure(log4jProps);
	}

	@Before
	public void setup()
	throws SQLException
	{
		//System.out.println("@Before - setup: ");
		Connection conn = DriverManager.getConnection(DB_URL, DB_USER, DB_PASSWORD);
		_conn = DbxConnection.createDbxConnection(conn);
	}
	
	@After
	public void close()
	throws SQLException
	{
		//System.out.println("@After - close():");
		if (_conn != null)
			_conn.close();
	}
	
	@AfterClass
	public static void tearDown()
	{
		//System.out.println("@AfterClass - tearDown():");
	}


	/**
	 * Simple diff (int, varchar, varchar, varchar) pk=id <br>
	 * - NO Difference
	 * - no debugging 
	 * - no nothing...
	 * 
	 * @throws Exception
	 */
	@Test
	public void test_noDiff_int_varchar()
	throws Exception
	{
		Statement stmnt = _conn.createStatement();
		
		stmnt.executeUpdate("drop table IF EXISTS t1_l");
		stmnt.executeUpdate("drop table IF EXISTS t1_r");
		
		stmnt.executeUpdate("create table t1_l (id int, c1 varchar(60), c2 varchar(60), c3 varchar(60), c4 varchar(60), primary key(id))");
		stmnt.executeUpdate("create table t1_r (id int, c1 varchar(60), c2 varchar(60), c3 varchar(60), c4 varchar(60), primary key(id))");

		stmnt.executeUpdate("insert into t1_l (id, c1, c2, c3, c4) values(00, '00-aaa', '00-bbb', '00-ccc', '00-ddd')");
		stmnt.executeUpdate("insert into t1_l (id, c1, c2, c3, c4) values(10, '10-aaa', '10-bbb', '10-ccc', '10-ddd')");
		stmnt.executeUpdate("insert into t1_l (id, c1, c2, c3, c4) values(20, '20-aaa', '20-bbb', '20-ccc', '20-ddd')");
		stmnt.executeUpdate("insert into t1_l (id, c1, c2, c3, c4) values(30, '30-aaa', '30-bbb', '30-ccc', '30-ddd')");
		stmnt.executeUpdate("insert into t1_l (id, c1, c2, c3, c4) values(40, '40-aaa', '40-bbb', '40-ccc', '40-ddd')");
		stmnt.executeUpdate("insert into t1_l (id, c1, c2, c3, c4) values(50, '50-aaa', '50-bbb', '50-ccc', '50-ddd')");
		stmnt.executeUpdate("insert into t1_l (id, c1, c2, c3, c4) values(60, '60-aaa', '60-bbb', '60-ccc', '60-ddd')");
		stmnt.executeUpdate("insert into t1_l (id, c1, c2, c3, c4) values(70, '70-aaa', '70-bbb', '70-ccc', '70-ddd')");
		stmnt.executeUpdate("insert into t1_l (id, c1, c2, c3, c4) values(80, '80-aaa', '80-bbb', '80-ccc', '80-ddd')");
		stmnt.executeUpdate("insert into t1_l (id, c1, c2, c3, c4) values(90, '90-aaa', '90-bbb', '90-ccc', '90-ddd')");
		                                      
		stmnt.executeUpdate("insert into t1_r (id, c1, c2, c3, c4) values(00, '00-aaa', '00-bbb', '00-ccc', '00-ddd')");
		stmnt.executeUpdate("insert into t1_r (id, c1, c2, c3, c4) values(10, '10-aaa', '10-bbb', '10-ccc', '10-ddd')");
		stmnt.executeUpdate("insert into t1_r (id, c1, c2, c3, c4) values(20, '20-aaa', '20-bbb', '20-ccc', '20-ddd')");
		stmnt.executeUpdate("insert into t1_r (id, c1, c2, c3, c4) values(30, '30-aaa', '30-bbb', '30-ccc', '30-ddd')");
		stmnt.executeUpdate("insert into t1_r (id, c1, c2, c3, c4) values(40, '40-aaa', '40-bbb', '40-ccc', '40-ddd')");
		stmnt.executeUpdate("insert into t1_r (id, c1, c2, c3, c4) values(50, '50-aaa', '50-bbb', '50-ccc', '50-ddd')");
		stmnt.executeUpdate("insert into t1_r (id, c1, c2, c3, c4) values(60, '60-aaa', '60-bbb', '60-ccc', '60-ddd')");
		stmnt.executeUpdate("insert into t1_r (id, c1, c2, c3, c4) values(70, '70-aaa', '70-bbb', '70-ccc', '70-ddd')");
		stmnt.executeUpdate("insert into t1_r (id, c1, c2, c3, c4) values(80, '80-aaa', '80-bbb', '80-ccc', '80-ddd')");
		stmnt.executeUpdate("insert into t1_r (id, c1, c2, c3, c4) values(90, '90-aaa', '90-bbb', '90-ccc', '90-ddd')");

		stmnt.close();

		List<String> pkList = new ArrayList<>();
		pkList.add("id");
		
		DiffContext context = new DiffContext();
		context.setPkColumns(pkList);
		context.setDiffTable(DiffSide.LEFT,  _conn.createStatement().executeQuery("select * from t1_l"), null);
		context.setDiffTable(DiffSide.RIGHT, _conn.createStatement().executeQuery("select * from t1_r"), null);
		context.setMessageDebugLevel(0); // 0=NO-DEBUG, 1=DEBUG, 2=TRACE
		context.validate();
		int diffCount = context.doDiff();

		assertEquals("Expected 0 in diffcount", 0, diffCount);
	}
	

	/**
	 * Simple diff (int, varchar, varchar, varchar) pk=id <br>
	 * - NO Difference
	 * - no debugging 
	 * - no nothing...
	 * 
	 * @throws Exception
	 */
	@Test
	public void test_noDiff_int_varchar_pk2cols()
	throws Exception
	{
		Statement stmnt = _conn.createStatement();
		
		stmnt.executeUpdate("drop table IF EXISTS t1_l");
		stmnt.executeUpdate("drop table IF EXISTS t1_r");
		
		stmnt.executeUpdate("create table t1_l (id int, c1 varchar(60), c2 varchar(60), c3 varchar(60), c4 varchar(60), primary key(id, c1))");
		stmnt.executeUpdate("create table t1_r (id int, c1 varchar(60), c2 varchar(60), c3 varchar(60), c4 varchar(60), primary key(id, c1))");

		stmnt.executeUpdate("insert into t1_l (id, c1, c2, c3, c4) values(00, '00-aaa', '00-bbb', '00-ccc', '00-ddd')");
		stmnt.executeUpdate("insert into t1_l (id, c1, c2, c3, c4) values(10, '10-aaa', '10-bbb', '10-ccc', '10-ddd')");
		stmnt.executeUpdate("insert into t1_l (id, c1, c2, c3, c4) values(20, '20-aaa', '20-bbb', '20-ccc', '20-ddd')");
		stmnt.executeUpdate("insert into t1_l (id, c1, c2, c3, c4) values(30, '30-aaa', '30-bbb', '30-ccc', '30-ddd')");
		stmnt.executeUpdate("insert into t1_l (id, c1, c2, c3, c4) values(40, '40-aaa', '40-bbb', '40-ccc', '40-ddd')");
		stmnt.executeUpdate("insert into t1_l (id, c1, c2, c3, c4) values(50, '50-aaa', '50-bbb', '50-ccc', '50-ddd')");
		stmnt.executeUpdate("insert into t1_l (id, c1, c2, c3, c4) values(60, '60-aaa', '60-bbb', '60-ccc', '60-ddd')");
		stmnt.executeUpdate("insert into t1_l (id, c1, c2, c3, c4) values(70, '70-aaa', '70-bbb', '70-ccc', '70-ddd')");
		stmnt.executeUpdate("insert into t1_l (id, c1, c2, c3, c4) values(80, '80-aaa', '80-bbb', '80-ccc', '80-ddd')");
		stmnt.executeUpdate("insert into t1_l (id, c1, c2, c3, c4) values(90, '90-aaa', '90-bbb', '90-ccc', '90-ddd')");
		                                      
		stmnt.executeUpdate("insert into t1_r (id, c1, c2, c3, c4) values(00, '00-aaa', '00-bbb', '00-ccc', '00-ddd')");
		stmnt.executeUpdate("insert into t1_r (id, c1, c2, c3, c4) values(10, '10-aaa', '10-bbb', '10-ccc', '10-ddd')");
		stmnt.executeUpdate("insert into t1_r (id, c1, c2, c3, c4) values(20, '20-aaa', '20-bbb', '20-ccc', '20-ddd')");
		stmnt.executeUpdate("insert into t1_r (id, c1, c2, c3, c4) values(30, '30-aaa', '30-bbb', '30-ccc', '30-ddd')");
		stmnt.executeUpdate("insert into t1_r (id, c1, c2, c3, c4) values(40, '40-aaa', '40-bbb', '40-ccc', '40-ddd')");
		stmnt.executeUpdate("insert into t1_r (id, c1, c2, c3, c4) values(50, '50-aaa', '50-bbb', '50-ccc', '50-ddd')");
		stmnt.executeUpdate("insert into t1_r (id, c1, c2, c3, c4) values(60, '60-aaa', '60-bbb', '60-ccc', '60-ddd')");
		stmnt.executeUpdate("insert into t1_r (id, c1, c2, c3, c4) values(70, '70-aaa', '70-bbb', '70-ccc', '70-ddd')");
		stmnt.executeUpdate("insert into t1_r (id, c1, c2, c3, c4) values(80, '80-aaa', '80-bbb', '80-ccc', '80-ddd')");
		stmnt.executeUpdate("insert into t1_r (id, c1, c2, c3, c4) values(90, '90-aaa', '90-bbb', '90-ccc', '90-ddd')");

		stmnt.close();

		List<String> pkList = new ArrayList<>();
		pkList.add("id");
		pkList = null;
		
		DiffContext context = new DiffContext();
		context.setPkColumns(pkList);
		context.setDiffTable(DiffSide.LEFT,  _conn.createStatement().executeQuery("select * from t1_l"), null);
		context.setDiffTable(DiffSide.RIGHT, _conn.createStatement().executeQuery("select * from t1_r"), null);
		context.setMessageDebugLevel(0); // 0=NO-DEBUG, 1=DEBUG, 2=TRACE
		context.validate();
		int diffCount = context.doDiff();

		//System.out.println("test_noDiff_int_varchar_pk2cols: " + context.getLeftDt().getPkColumnNamesCsv());
		
		assertEquals("Expected 0 in diffcount", 0, diffCount);
	}
	

	/**
	 * Simple diff, with some NULL values  (int, varchar, varchar, varchar) pk=id <br>
	 * - NO Difference
	 * - no debugging 
	 * - no nothing...
	 * - NO PK in the DiffContext, it will be looked up and assigned 
	 * 
	 * @throws Exception
	 */
	@Test
	public void test_noDiff_int_varchar_withNullValues()
	throws Exception
	{
		Statement stmnt = _conn.createStatement();
		
		stmnt.executeUpdate("drop table IF EXISTS t1_l");
		stmnt.executeUpdate("drop table IF EXISTS t1_r");
		
		stmnt.executeUpdate("create table t1_l (id int, c1 varchar(60), c2 varchar(60), c3 varchar(60), c4 varchar(60), primary key(id))");
		stmnt.executeUpdate("create table t1_r (id int, c1 varchar(60), c2 varchar(60), c3 varchar(60), c4 varchar(60), primary key(id))");

		stmnt.executeUpdate("insert into t1_l (id, c1, c2, c3, c4) values(10, NULL,     '10-bbb', '10-ccc', '10-ddd')");
		stmnt.executeUpdate("insert into t1_l (id, c1, c2, c3, c4) values(20, '20-aaa', NULL,     '20-ccc', '20-ddd')");
		stmnt.executeUpdate("insert into t1_l (id, c1, c2, c3, c4) values(30, '30-aaa', '30-bbb', NULL,     '30-ddd')");
		stmnt.executeUpdate("insert into t1_l (id, c1, c2, c3, c4) values(40, '40-aaa', '40-bbb', '40-ccc', NULL    )");
		stmnt.executeUpdate("insert into t1_l (id, c1, c2, c3, c4) values(50, NULL,     NULL,     '50-ccc', '50-ddd')");
		stmnt.executeUpdate("insert into t1_l (id, c1, c2, c3, c4) values(60, NULL,     NULL,     NULL,     '50-ddd')");
		stmnt.executeUpdate("insert into t1_l (id, c1, c2, c3, c4) values(70, NULL,     NULL,     NULL,     NULL    )");
		                                      
		stmnt.executeUpdate("insert into t1_r (id, c1, c2, c3, c4) values(10, NULL,     '10-bbb', '10-ccc', '10-ddd')");
		stmnt.executeUpdate("insert into t1_r (id, c1, c2, c3, c4) values(20, '20-aaa', NULL,     '20-ccc', '20-ddd')");
		stmnt.executeUpdate("insert into t1_r (id, c1, c2, c3, c4) values(30, '30-aaa', '30-bbb', NULL,     '30-ddd')");
		stmnt.executeUpdate("insert into t1_r (id, c1, c2, c3, c4) values(40, '40-aaa', '40-bbb', '40-ccc', NULL    )");
		stmnt.executeUpdate("insert into t1_r (id, c1, c2, c3, c4) values(50, NULL,     NULL,     '50-ccc', '50-ddd')");
		stmnt.executeUpdate("insert into t1_r (id, c1, c2, c3, c4) values(60, NULL,     NULL,     NULL,     '50-ddd')");
		stmnt.executeUpdate("insert into t1_r (id, c1, c2, c3, c4) values(70, NULL,     NULL,     NULL,     NULL    )");

		stmnt.close();

		List<String> pkList = new ArrayList<>();
		pkList.add("id");
		pkList = null;
		
		DiffContext context = new DiffContext();
		context.setPkColumns(pkList);
		context.setDiffTable(DiffSide.LEFT,  _conn.createStatement().executeQuery("select * from t1_l"), null);
		context.setDiffTable(DiffSide.RIGHT, _conn.createStatement().executeQuery("select * from t1_r"), null);
		context.setMessageDebugLevel(0); // 0=NO-DEBUG, 1=DEBUG, 2=TRACE
		context.validate();
		int diffCount = context.doDiff();

		assertEquals("Expected 0 in diffcount", 0, diffCount);
	}
	

	/**
	 * Simple diff (int, varchar(60), timestamp with time zone, timestamp, date, time, varbinary(10), decimal(10,2), clob, blob) pk=id <br>
	 * - NO Difference
	 * - no debugging 
	 * - no nothing...
	 * 
	 * @throws Exception
	 */
	@Test
	public void test_noDiff_manyDataTypes()
	throws Exception
	{
		Statement stmnt = _conn.createStatement();
		
		stmnt.executeUpdate("drop table IF EXISTS t1_l");
		stmnt.executeUpdate("drop table IF EXISTS t1_r");
		
		stmnt.executeUpdate("create table t1_l (id int, c1 varchar(60), c2 timestamp with time zone, c3 timestamp, c4 date, c5 time, c6 varbinary(10), c7 decimal(10,2), c8 clob, c9 blob, primary key(id))");
		stmnt.executeUpdate("create table t1_r (id int, c1 varchar(60), c2 timestamp with time zone, c3 timestamp, c4 date, c5 time, c6 varbinary(10), c7 decimal(10,2), c8 clob, c9 blob, primary key(id))");

		//                                                                                    id  c1        c2                        c3                     c4            c5          c6                c7    c8                                 c9
		stmnt.executeUpdate("insert into t1_l (id, c1, c2, c3, c4, c5, c6, c7, c8, c9) values(10, '10-aaa', '2019-01-01T11:11:11+01', '2019-01-01 11:11:11', '2019-01-01', '11:11:11', 0102030405060708, 1.11, '"+crClob("Clob-1-[${cnt}]: ")+"', RAWTOHEX('"+crClob("Blob-1-[${cnt}]: ")+"'))");
		stmnt.executeUpdate("insert into t1_l (id, c1, c2, c3, c4, c5, c6, c7, c8, c9) values(20, '20-aaa', '2019-01-02T12:12:12+02', '2019-01-02 12:12:12', '2019-01-02', '12:12:12', 0102030405060708, 2.22, '"+crClob("Clob-2-[${cnt}]: ")+"', RAWTOHEX('"+crClob("Blob-2-[${cnt}]: ")+"'))");
		stmnt.executeUpdate("insert into t1_l (id, c1, c2, c3, c4, c5, c6, c7, c8, c9) values(30, '30-aaa', '2019-01-03T13:13:13+03', '2019-01-03 13:13:13', '2019-01-03', '13:13:13', 0102030405060708, 3.33, '"+crClob("Clob-3-[${cnt}]: ")+"', RAWTOHEX('"+crClob("Blob-3-[${cnt}]: ")+"'))");
		stmnt.executeUpdate("insert into t1_l (id, c1, c2, c3, c4, c5, c6, c7, c8, c9) values(40, '40-aaa', '2019-01-04T14:14:14+04', '2019-01-04 14:14:14', '2019-01-04', '14:14:14', 0102030405060708, 4.44, '"+crClob("Clob-4-[${cnt}]: ")+"', RAWTOHEX('"+crClob("Blob-4-[${cnt}]: ")+"'))");
		stmnt.executeUpdate("insert into t1_l (id, c1, c2, c3, c4, c5, c6, c7, c8, c9) values(50, '50-aaa', '2019-01-05T15:15:15+05', '2019-01-05 15:15:15', '2019-01-05', '15:15:15', 0102030405060708, 5.55, '"+crClob("Clob-5-[${cnt}]: ")+"', RAWTOHEX('"+crClob("Blob-5-[${cnt}]: ")+"'))");
		stmnt.executeUpdate("insert into t1_l (id, c1, c2, c3, c4, c5, c6, c7, c8, c9) values(60, '60-aaa', '2019-01-06T16:16:16+06', '2019-01-06 16:16:16', '2019-01-06', '16:16:16', 0102030405060708, 6.66, '"+crClob("Clob-6-[${cnt}]: ")+"', RAWTOHEX('"+crClob("Blob-6-[${cnt}]: ")+"'))");
		stmnt.executeUpdate("insert into t1_l (id, c1, c2, c3, c4, c5, c6, c7, c8, c9) values(70, '70-aaa', '2019-01-07T17:17:17+07', '2019-01-07 17:17:17', '2019-01-07', '17:17:17', 0102030405060708, 7.77, '"+crClob("Clob-7-[${cnt}]: ")+"', RAWTOHEX('"+crClob("Blob-7-[${cnt}]: ")+"'))");
		stmnt.executeUpdate("insert into t1_l (id, c1, c2, c3, c4, c5, c6, c7, c8, c9) values(80, '80-aaa', '2019-01-08T18:18:18+08', '2019-01-08 18:18:18', '2019-01-08', '18:18:18', 0102030405060708, 8.88, '"+crClob("Clob-8-[${cnt}]: ")+"', RAWTOHEX('"+crClob("Blob-8-[${cnt}]: ")+"'))");
		stmnt.executeUpdate("insert into t1_l (id, c1, c2, c3, c4, c5, c6, c7, c8, c9) values(90, '90-aaa', '2019-01-09T19:19:19+09', '2019-01-09 19:19:19', '2019-01-09', '19:19:19', 0102030405060708, 9.99, '"+crClob("Clob-9-[${cnt}]: ")+"', RAWTOHEX('"+crClob("Blob-9-[${cnt}]: ")+"'))");

		stmnt.executeUpdate("insert into t1_r (id, c1, c2, c3, c4, c5, c6, c7, c8, c9) values(10, '10-aaa', '2019-01-01T11:11:11+01', '2019-01-01 11:11:11', '2019-01-01', '11:11:11', 0102030405060708, 1.11, '"+crClob("Clob-1-[${cnt}]: ")+"', RAWTOHEX('"+crClob("Blob-1-[${cnt}]: ")+"'))");
		stmnt.executeUpdate("insert into t1_r (id, c1, c2, c3, c4, c5, c6, c7, c8, c9) values(20, '20-aaa', '2019-01-02T12:12:12+02', '2019-01-02 12:12:12', '2019-01-02', '12:12:12', 0102030405060708, 2.22, '"+crClob("Clob-2-[${cnt}]: ")+"', RAWTOHEX('"+crClob("Blob-2-[${cnt}]: ")+"'))");
		stmnt.executeUpdate("insert into t1_r (id, c1, c2, c3, c4, c5, c6, c7, c8, c9) values(30, '30-aaa', '2019-01-03T13:13:13+03', '2019-01-03 13:13:13', '2019-01-03', '13:13:13', 0102030405060708, 3.33, '"+crClob("Clob-3-[${cnt}]: ")+"', RAWTOHEX('"+crClob("Blob-3-[${cnt}]: ")+"'))");
		stmnt.executeUpdate("insert into t1_r (id, c1, c2, c3, c4, c5, c6, c7, c8, c9) values(40, '40-aaa', '2019-01-04T14:14:14+04', '2019-01-04 14:14:14', '2019-01-04', '14:14:14', 0102030405060708, 4.44, '"+crClob("Clob-4-[${cnt}]: ")+"', RAWTOHEX('"+crClob("Blob-4-[${cnt}]: ")+"'))");
		stmnt.executeUpdate("insert into t1_r (id, c1, c2, c3, c4, c5, c6, c7, c8, c9) values(50, '50-aaa', '2019-01-05T15:15:15+05', '2019-01-05 15:15:15', '2019-01-05', '15:15:15', 0102030405060708, 5.55, '"+crClob("Clob-5-[${cnt}]: ")+"', RAWTOHEX('"+crClob("Blob-5-[${cnt}]: ")+"'))");
		stmnt.executeUpdate("insert into t1_r (id, c1, c2, c3, c4, c5, c6, c7, c8, c9) values(60, '60-aaa', '2019-01-06T16:16:16+06', '2019-01-06 16:16:16', '2019-01-06', '16:16:16', 0102030405060708, 6.66, '"+crClob("Clob-6-[${cnt}]: ")+"', RAWTOHEX('"+crClob("Blob-6-[${cnt}]: ")+"'))");
		stmnt.executeUpdate("insert into t1_r (id, c1, c2, c3, c4, c5, c6, c7, c8, c9) values(70, '70-aaa', '2019-01-07T17:17:17+07', '2019-01-07 17:17:17', '2019-01-07', '17:17:17', 0102030405060708, 7.77, '"+crClob("Clob-7-[${cnt}]: ")+"', RAWTOHEX('"+crClob("Blob-7-[${cnt}]: ")+"'))");
		stmnt.executeUpdate("insert into t1_r (id, c1, c2, c3, c4, c5, c6, c7, c8, c9) values(80, '80-aaa', '2019-01-08T18:18:18+08', '2019-01-08 18:18:18', '2019-01-08', '18:18:18', 0102030405060708, 8.88, '"+crClob("Clob-8-[${cnt}]: ")+"', RAWTOHEX('"+crClob("Blob-8-[${cnt}]: ")+"'))");
		stmnt.executeUpdate("insert into t1_r (id, c1, c2, c3, c4, c5, c6, c7, c8, c9) values(90, '90-aaa', '2019-01-09T19:19:19+09', '2019-01-09 19:19:19', '2019-01-09', '19:19:19', 0102030405060708, 9.99, '"+crClob("Clob-9-[${cnt}]: ")+"', RAWTOHEX('"+crClob("Blob-9-[${cnt}]: ")+"'))");

		stmnt.close();

		List<String> pkList = new ArrayList<>();
		pkList.add("id");
		
		DiffContext context = new DiffContext();
		context.setPkColumns(pkList);
		context.setDiffTable(DiffSide.LEFT,  _conn.createStatement().executeQuery("select * from t1_l"), null);
		context.setDiffTable(DiffSide.RIGHT, _conn.createStatement().executeQuery("select * from t1_r"), null);
		context.setMessageDebugLevel(0); // 0=NO-DEBUG, 1=DEBUG, 2=TRACE
		context.validate();
		int diffCount = context.doDiff();

		assertEquals("Expected 0 in diffcount", 0, diffCount);
	}
	private String crClob(String val)
	{
		int cnt = 1000;
		
		StringBuilder sb = new StringBuilder();
		for (int i=0; i<cnt; i++)
			sb.append(val.replace("${cnt}", Integer.toString(i)));

		return sb.toString();
	}


	/**
	 * Simple diff, with some NULL values  (int, varchar, varchar, varchar) pk=id <br>
	 * - NO Difference
	 * - no debugging 
	 * - no nothing...
	 * 
	 * @throws Exception
	 */
	@Test
	public void test_missingRhs_withNullValues()
	throws Exception
	{
		Statement stmnt = _conn.createStatement();
		
		stmnt.executeUpdate("drop table IF EXISTS t1_l");
		stmnt.executeUpdate("drop table IF EXISTS t1_r");
		
		stmnt.executeUpdate("create table t1_l (id int, c1 varchar(60), c2 varchar(60), c3 varchar(60), c4 varchar(60), primary key(id))");
		stmnt.executeUpdate("create table t1_r (id int, c1 varchar(60), c2 varchar(60), c3 varchar(60), c4 varchar(60), primary key(id))");

		stmnt.executeUpdate("insert into t1_l (id, c1, c2, c3, c4) values(10, NULL,     '10-bbb', '10-ccc', '10-ddd')");
		stmnt.executeUpdate("insert into t1_l (id, c1, c2, c3, c4) values(20, '20-aaa', NULL,     '20-ccc', '20-ddd')");
		stmnt.executeUpdate("insert into t1_l (id, c1, c2, c3, c4) values(30, '30-aaa', '30-bbb', NULL,     '30-ddd')");
		stmnt.executeUpdate("insert into t1_l (id, c1, c2, c3, c4) values(40, '40-aaa', '40-bbb', '40-ccc', NULL    )");
		stmnt.executeUpdate("insert into t1_l (id, c1, c2, c3, c4) values(50, NULL,     NULL,     '50-ccc', '50-ddd')");
		stmnt.executeUpdate("insert into t1_l (id, c1, c2, c3, c4) values(60, NULL,     NULL,     NULL,     '50-ddd')");
		stmnt.executeUpdate("insert into t1_l (id, c1, c2, c3, c4) values(70, NULL,     NULL,     NULL,     NULL    )");
		                                      
		stmnt.executeUpdate("insert into t1_r (id, c1, c2, c3, c4) values(10, NULL,     '10-bbb', '10-ccc', '10-ddd')");
		stmnt.executeUpdate("insert into t1_r (id, c1, c2, c3, c4) values(20, '20-aaa', NULL,     '20-ccc', '20-ddd')");
		stmnt.executeUpdate("insert into t1_r (id, c1, c2, c3, c4) values(30, '30-aaa', '30-bbb', NULL,     '30-ddd')");
		stmnt.executeUpdate("insert into t1_r (id, c1, c2, c3, c4) values(40, '40-aaa', '40-bbb', '40-ccc', NULL    )");
		stmnt.executeUpdate("insert into t1_r (id, c1, c2, c3, c4) values(50, NULL,     NULL,     '50-ccc', '50-ddd')");

		stmnt.close();

		List<String> pkList = new ArrayList<>();
		pkList.add("id");
		
		DiffContext context = new DiffContext();
		context.setPkColumns(pkList);
		context.setDiffTable(DiffSide.LEFT,  _conn.createStatement().executeQuery("select * from t1_l"), null);
		context.setDiffTable(DiffSide.RIGHT, _conn.createStatement().executeQuery("select * from t1_r"), null);
		context.setMessageDebugLevel(0); // 0=NO-DEBUG, 1=DEBUG, 2=TRACE
		context.validate();
		int diffCount = context.doDiff();

		assertEquals("Expected 2 in diffcount", 2, diffCount);
		

		GenerateSqlText lSqlGen = new GenerateSqlText(context, _conn);
		GenerateSqlText rSqlGen = new GenerateSqlText(context, _conn);
		
		List<String> leftFix  = lSqlGen.getSql(DiffSide.LEFT);
		List<String> rightFix = rSqlGen.getSql(DiffSide.RIGHT);
		
		assertEquals("Expected 1 in left-fix-comment",  1, lSqlGen.getCommentCount());
		assertEquals("Expected 0 in left-fix-insert",   0, lSqlGen.getInsertCount());
		assertEquals("Expected 2 in left-fix-delete",   2, lSqlGen.getDeleteCount());
		assertEquals("Expected 0 in left-fix-update",   0, lSqlGen.getUpdateCount());
		assertEquals("Expected 3 in left-fix-sql-list", 3, leftFix.size());
		
		assertEquals("Expected 1 in right-fix-comment",  1, rSqlGen.getCommentCount());
		assertEquals("Expected 2 in right-fix-insert",   2, rSqlGen.getInsertCount());
		assertEquals("Expected 0 in right-fix-delete",   0, rSqlGen.getDeleteCount());
		assertEquals("Expected 0 in right-fix-update",   0, rSqlGen.getUpdateCount());
		assertEquals("Expected 3 in right-fix-sql-list", 3, rightFix.size());

		for (String dml : leftFix)  System.out.println("<<< LEFT--FIX: " + dml);
		for (String dml : rightFix) System.out.println(">>> RIGHT-FIX: " + dml);
	}
	

	
//	/**
//	 * Simple diff (int, varchar, varchar, varchar) pk=id <br>
//	 * - NO Difference
//	 * - no debugging 
//	 * - no nothing...
//	 * 
//	 * @throws Exception
//	 */
//	@Test
//	public void test_smallDiff()
//	throws Exception
//	{
//		Statement stmnt = _conn.createStatement();
//		
//		stmnt.executeUpdate("drop table IF EXISTS t1_l");
//		stmnt.executeUpdate("drop table IF EXISTS t1_r");
//		
//		stmnt.executeUpdate("create table t1_l (id int, c1 varchar(60), c2 varchar(60), c3 varchar(60), c4 varchar(60), primary key(id))");
//		stmnt.executeUpdate("create table t1_r (id int, c1 varchar(60), c2 varchar(60), c3 varchar(60), c4 varchar(60), primary key(id))");
//
//		stmnt.executeUpdate("insert into t1_l (id, c1, c2, c3, c4) values(00, '00-aaa', '00-bbb', '00-ccc', '00-ddd')");
//		stmnt.executeUpdate("insert into t1_l (id, c1, c2, c3, c4) values(10, '10-aaa', '10-bbb', '10-ccc', '10-ddd')");
//		stmnt.executeUpdate("insert into t1_l (id, c1, c2, c3, c4) values(11, '11-aaa', '11-bbb', '11-ccc', '11-ddd')"); // EXTRA
//		stmnt.executeUpdate("insert into t1_l (id, c1, c2, c3, c4) values(20, '20-aaa', '20-bbb', '20-ccc', '20-ddd')");
//		stmnt.executeUpdate("insert into t1_l (id, c1, c2, c3, c4) values(30, '30-aaa', '30-bbb', '30-ccc', '30-ddd')");
//		stmnt.executeUpdate("insert into t1_l (id, c1, c2, c3, c4) values(40, '40-aaa', '40-bbb', '40-ccc', '40-ddd')");
//		stmnt.executeUpdate("insert into t1_l (id, c1, c2, c3, c4) values(50, '50-aaa', '50-bbb', '50-ccc', '50-ddd')");
//		stmnt.executeUpdate("insert into t1_l (id, c1, c2, c3, c4) values(60, '60-aaa', '60-bbb', '60-ccc', '60-ddd')");
//		stmnt.executeUpdate("insert into t1_l (id, c1, c2, c3, c4) values(70, '70-aaa', '70-bbb', '70-ccc', '70-ddd')");
//		stmnt.executeUpdate("insert into t1_l (id, c1, c2, c3, c4) values(80, '80-aaa', '80-bbb', '80-ccc', '80-ddd')");
//		stmnt.executeUpdate("insert into t1_l (id, c1, c2, c3, c4) values(90, '90-aaa', '90-bbb', '90-ccc', '90-ddd')");
//		                                      
//		stmnt.executeUpdate("insert into t1_r (id, c1, c2, c3, c4) values(00, '00-aaa', '00-bbb', '00-ccc', '00-ddd')");
//		stmnt.executeUpdate("insert into t1_r (id, c1, c2, c3, c4) values(10, '10-AAA', '10-bbb', '10-ccc', '10-ddd')"); // 1-col-DIFF
//		stmnt.executeUpdate("insert into t1_r (id, c1, c2, c3, c4) values(20, '20-aaa', '20-bbb', '20-ccc', '20-ddd')");
//		stmnt.executeUpdate("insert into t1_r (id, c1, c2, c3, c4) values(30, '30-aaa', '30-bbb', '30-ccc', '30-ddd')");
//		stmnt.executeUpdate("insert into t1_r (id, c1, c2, c3, c4) values(40, '40-aaa', '40-bbb', '40-ccc', '40-ddd')");
////		stmnt.executeUpdate("insert into t1_r (id, c1, c2, c3, c4) values(50, '50-aaa', '50-bbb', '50-ccc', '50-ddd')"); // MISSING
//		stmnt.executeUpdate("insert into t1_r (id, c1, c2, c3, c4) values(51, '51-aaa', '51-bbb', '51-ccc', '51-ddd')"); // EXTRA
//		stmnt.executeUpdate("insert into t1_r (id, c1, c2, c3, c4) values(60, '60-aaa', '60-bbb', '60-ccc', '60-ddd')");
//		stmnt.executeUpdate("insert into t1_r (id, c1, c2, c3, c4) values(70, '70-aaa', '70-bbb', '70-ccc', '70-ddd')");
//		stmnt.executeUpdate("insert into t1_r (id, c1, c2, c3, c4) values(80, '80-aaa', '80-xxx', '80-yyy', '80-ddd')"); // 2-col-DIFF
//		stmnt.executeUpdate("insert into t1_r (id, c1, c2, c3, c4) values(90, '90-aaa', '90-bbb', '90-ccc', '90-zzz')"); // 1-col-DIFF
//		stmnt.executeUpdate("insert into t1_r (id, c1, c2, c3, c4) values(91, '91-aaa', '91-bbb', '91-ccc', '91-ddd')"); // EXTRA
//
//		stmnt.close();
//
////		Statement stmnt = _conn.createStatement();
//
//		List<String> pkList = new ArrayList<>();
//		pkList.add("id");
//		pkList = null;
//		
////		DiffTable l_dt = new DiffTable(DiffSide.LEFT,  null, _conn.createStatement().executeQuery("select * from t1_l"));
////		DiffTable r_dt = new DiffTable(DiffSide.RIGHT, null, _conn.createStatement().executeQuery("select * from t1_r"));
//
////		DiffContext context = new DiffContext(l_dt, r_dt);
//
//		DiffContext context = new DiffContext();
//		context.setPkColumns(pkList);
//		context.setDiffTable(DiffSide.LEFT,  _conn.createStatement().executeQuery("select * from t1_l"));
//		context.setDiffTable(DiffSide.RIGHT, _conn.createStatement().executeQuery("select * from t1_r"));
//		context.setDebugLevel(2); // 0=NO-DEBUG, 1=DEBUG, 2=TRACE
//		context.validate();
//		context.doDiff();
//
//		System.out.println("DONE -- compare");
//
//		List<String> leftFix  = new GenerateSqlText(context).generateSqlFix(DiffSide.LEFT);
//		List<String> rightFix = new GenerateSqlText(context).generateSqlFix(DiffSide.RIGHT);
//
//		System.out.println("");
//		System.out.println("==== BEGIN LEFT FIX ====");
//		for (String sql : leftFix) { System.out.println(sql); }
//		System.out.println("====== END LEFT FIX ====");
//		
//		System.out.println("");
//		System.out.println("==== BEGIN RIGHT FIX ====");
//		for (String sql : rightFix) { System.out.println(sql); }
//		System.out.println("====== END RIGHT FIX ====");
//		
//		System.out.println("");
//		System.out.println("-END-");
//
//		
//		for (Message msg : context.getMessages())
//			System.out.println(msg.toString());
//
//	}
}
