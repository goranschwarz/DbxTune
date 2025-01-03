/**********************************"*********************************************
 * Copyright (C) 2010-2025 Goran Schwarz
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
package com.dbxtune.sql;

import static org.junit.Assert.assertEquals;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.core.config.Configurator;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import com.dbxtune.sql.conn.DbxConnection;
import com.dbxtune.sql.diff.DiffContext;
import com.dbxtune.sql.diff.DiffContext.DiffSide;
import com.dbxtune.sql.diff.actions.GenerateSqlText;

public class DiffTest
{
//	private static final String DB_DRIVER     = "org.h2.Driver";
//	private static final String DB_URL        = "jdbc:h2:mem:test;DB_CLOSE_DELAY=-1";

	private static final String L_DB_URL        = "jdbc:h2:mem:leftDB";
	private static final String L_DB_USER       = "sa";
	private static final String L_DB_PASSWORD   = "";

	private static final String R_DB_URL        = "jdbc:h2:mem:rightDB";
	private static final String R_DB_USER       = "sa";
	private static final String R_DB_PASSWORD   = "";

	private DbxConnection _lConn; 
	private DbxConnection _rConn; 
			

	@BeforeClass
	public static void init() 
	{
		//System.out.println("@BeforeClass - init():");

		// Set Log4j Log Level
		Configurator.setRootLevel(Level.TRACE);
	}

	@Before
	public void setup()
	throws SQLException
	{
		//System.out.println("@Before - setup: ");
		Connection lConn = DriverManager.getConnection(L_DB_URL, L_DB_USER, L_DB_PASSWORD);
		_lConn = DbxConnection.createDbxConnection(lConn);

		Connection rConn = DriverManager.getConnection(R_DB_URL, R_DB_USER, R_DB_PASSWORD);
		_rConn = DbxConnection.createDbxConnection(rConn);
	}
	
	@After
	public void close()
	throws SQLException
	{
		//System.out.println("@After - close():");
		if (_lConn != null) _lConn.close();
		if (_rConn != null) _rConn.close();
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
		Statement lStmnt = _lConn.createStatement();
		Statement rStmnt = _rConn.createStatement();
		
		lStmnt.executeUpdate("drop table IF EXISTS t1_l");
		rStmnt.executeUpdate("drop table IF EXISTS t1_r");
		
		lStmnt.executeUpdate("create table t1_l (id int, c1 varchar(60), c2 varchar(60), c3 varchar(60), c4 varchar(60), primary key(id))");
		rStmnt.executeUpdate("create table t1_r (id int, c1 varchar(60), c2 varchar(60), c3 varchar(60), c4 varchar(60), primary key(id))");

		lStmnt.executeUpdate("insert into t1_l (id, c1, c2, c3, c4) values(00, '00-aaa', '00-bbb', '00-ccc', '00-ddd')");
		lStmnt.executeUpdate("insert into t1_l (id, c1, c2, c3, c4) values(10, '10-aaa', '10-bbb', '10-ccc', '10-ddd')");
		lStmnt.executeUpdate("insert into t1_l (id, c1, c2, c3, c4) values(20, '20-aaa', '20-bbb', '20-ccc', '20-ddd')");
		lStmnt.executeUpdate("insert into t1_l (id, c1, c2, c3, c4) values(30, '30-aaa', '30-bbb', '30-ccc', '30-ddd')");
		lStmnt.executeUpdate("insert into t1_l (id, c1, c2, c3, c4) values(40, '40-aaa', '40-bbb', '40-ccc', '40-ddd')");
		lStmnt.executeUpdate("insert into t1_l (id, c1, c2, c3, c4) values(50, '50-aaa', '50-bbb', '50-ccc', '50-ddd')");
		lStmnt.executeUpdate("insert into t1_l (id, c1, c2, c3, c4) values(60, '60-aaa', '60-bbb', '60-ccc', '60-ddd')");
		lStmnt.executeUpdate("insert into t1_l (id, c1, c2, c3, c4) values(70, '70-aaa', '70-bbb', '70-ccc', '70-ddd')");
		lStmnt.executeUpdate("insert into t1_l (id, c1, c2, c3, c4) values(80, '80-aaa', '80-bbb', '80-ccc', '80-ddd')");
		lStmnt.executeUpdate("insert into t1_l (id, c1, c2, c3, c4) values(90, '90-aaa', '90-bbb', '90-ccc', '90-ddd')");
		                                      
		rStmnt.executeUpdate("insert into t1_r (id, c1, c2, c3, c4) values(00, '00-aaa', '00-bbb', '00-ccc', '00-ddd')");
		rStmnt.executeUpdate("insert into t1_r (id, c1, c2, c3, c4) values(10, '10-aaa', '10-bbb', '10-ccc', '10-ddd')");
		rStmnt.executeUpdate("insert into t1_r (id, c1, c2, c3, c4) values(20, '20-aaa', '20-bbb', '20-ccc', '20-ddd')");
		rStmnt.executeUpdate("insert into t1_r (id, c1, c2, c3, c4) values(30, '30-aaa', '30-bbb', '30-ccc', '30-ddd')");
		rStmnt.executeUpdate("insert into t1_r (id, c1, c2, c3, c4) values(40, '40-aaa', '40-bbb', '40-ccc', '40-ddd')");
		rStmnt.executeUpdate("insert into t1_r (id, c1, c2, c3, c4) values(50, '50-aaa', '50-bbb', '50-ccc', '50-ddd')");
		rStmnt.executeUpdate("insert into t1_r (id, c1, c2, c3, c4) values(60, '60-aaa', '60-bbb', '60-ccc', '60-ddd')");
		rStmnt.executeUpdate("insert into t1_r (id, c1, c2, c3, c4) values(70, '70-aaa', '70-bbb', '70-ccc', '70-ddd')");
		rStmnt.executeUpdate("insert into t1_r (id, c1, c2, c3, c4) values(80, '80-aaa', '80-bbb', '80-ccc', '80-ddd')");
		rStmnt.executeUpdate("insert into t1_r (id, c1, c2, c3, c4) values(90, '90-aaa', '90-bbb', '90-ccc', '90-ddd')");

		lStmnt.close();
		rStmnt.close();

		List<String> pkList = new ArrayList<>();
		pkList.add("id");
		
		DiffContext context = new DiffContext();
		context.setPkColumns(pkList);
		context.setDiffTable(DiffSide.LEFT,  _lConn.createStatement().executeQuery("select * from t1_l"), "select * from t1_l", null);
		context.setDiffTable(DiffSide.RIGHT, _rConn.createStatement().executeQuery("select * from t1_r"), "select * from t1_r", null);
		context.setMessageDebugLevel(0); // 0=NO-DEBUG, 1=DEBUG, 2=TRACE
		context.validate();
		int diffCount = context.doDiff();

		assertEquals("Expected 0 in diffcount", 0, diffCount);
	}
	

	/**
	 * Simple diff (int, varchar, varchar, varchar) pk=id <br>
	 * - SMALL Difference
	 * - no debugging 
	 * - no nothing...
	 * 
	 * @throws Exception
	 */
	@Test
	public void test_diff_simple()
	throws Exception
	{
		Statement lStmnt = _lConn.createStatement();
		Statement rStmnt = _rConn.createStatement();
		
		lStmnt.executeUpdate("drop table IF EXISTS t1");
		rStmnt.executeUpdate("drop table IF EXISTS t1");
		
		lStmnt.executeUpdate("create table t1 (id int, c1 varchar(60), c2 varchar(60), c3 varchar(60), c4 varchar(60), primary key(id))");
		rStmnt.executeUpdate("create table t1 (id int, c1 varchar(60), c2 varchar(60), c3 varchar(60), c4 varchar(60), primary key(id))");

		lStmnt.executeUpdate("insert into t1 (id, c1, c2, c3, c4) values(00, '00-aaa', '00-bbb', '00-ccc', '00-ddd')");
		lStmnt.executeUpdate("insert into t1 (id, c1, c2, c3, c4) values(10, '10-aaa', '10-bbb', '10-ccc', '10-ddd')");
		lStmnt.executeUpdate("insert into t1 (id, c1, c2, c3, c4) values(20, '20-aaa', '20-bbb', '20-ccc', '20-ddd')");

		rStmnt.executeUpdate("insert into t1 (id, c1, c2, c3, c4) values(00, '00-aaa', '00-bbb', '00-ccc', '00-ddd')");
		rStmnt.executeUpdate("insert into t1 (id, c1, c2, c3, c4) values(10, '10--aaa', '10-bbb', '10-ccc', '10-ddd')");
		rStmnt.executeUpdate("insert into t1 (id, c1, c2, c3, c4) values(20, '20--aaa', '20-bbb', '20-ccc', '20-ddd')");

		lStmnt.close();
		rStmnt.close();

		String sql = "select * from t1";
		
		Statement leftStmt = _lConn.createStatement();
		ResultSet leftRs = leftStmt.executeQuery(sql);

		Statement rightStmt = _rConn.createStatement();
		ResultSet rightRs = rightStmt.executeQuery(sql);

		DiffContext context = new DiffContext();
		context.setPkColumns(null);
		context.setDiffTable(DiffSide.LEFT,  leftRs,  sql, null);
		context.setDiffTable(DiffSide.RIGHT, rightRs, sql, null);
		context.setMessageDebugLevel(0); // 0=NO-DEBUG, 1=DEBUG, 2=TRACE
		context.validate();
		int diffCount = context.doDiff();

		assertEquals("Expected 2 in diffcount", 2, diffCount);
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
		Statement lStmnt = _lConn.createStatement();
		Statement rStmnt = _rConn.createStatement();
		
		lStmnt.executeUpdate("drop table IF EXISTS t1_l");
		rStmnt.executeUpdate("drop table IF EXISTS t1_r");
		
		lStmnt.executeUpdate("create table t1_l (id int, c1 varchar(60), c2 varchar(60), c3 varchar(60), c4 varchar(60), primary key(id, c1))");
		rStmnt.executeUpdate("create table t1_r (id int, c1 varchar(60), c2 varchar(60), c3 varchar(60), c4 varchar(60), primary key(id, c1))");

		lStmnt.executeUpdate("insert into t1_l (id, c1, c2, c3, c4) values(00, '00-aaa', '00-bbb', '00-ccc', '00-ddd')");
		lStmnt.executeUpdate("insert into t1_l (id, c1, c2, c3, c4) values(10, '10-aaa', '10-bbb', '10-ccc', '10-ddd')");
		lStmnt.executeUpdate("insert into t1_l (id, c1, c2, c3, c4) values(20, '20-aaa', '20-bbb', '20-ccc', '20-ddd')");
		lStmnt.executeUpdate("insert into t1_l (id, c1, c2, c3, c4) values(30, '30-aaa', '30-bbb', '30-ccc', '30-ddd')");
		lStmnt.executeUpdate("insert into t1_l (id, c1, c2, c3, c4) values(40, '40-aaa', '40-bbb', '40-ccc', '40-ddd')");
		lStmnt.executeUpdate("insert into t1_l (id, c1, c2, c3, c4) values(50, '50-aaa', '50-bbb', '50-ccc', '50-ddd')");
		lStmnt.executeUpdate("insert into t1_l (id, c1, c2, c3, c4) values(60, '60-aaa', '60-bbb', '60-ccc', '60-ddd')");
		lStmnt.executeUpdate("insert into t1_l (id, c1, c2, c3, c4) values(70, '70-aaa', '70-bbb', '70-ccc', '70-ddd')");
		lStmnt.executeUpdate("insert into t1_l (id, c1, c2, c3, c4) values(80, '80-aaa', '80-bbb', '80-ccc', '80-ddd')");
		lStmnt.executeUpdate("insert into t1_l (id, c1, c2, c3, c4) values(90, '90-aaa', '90-bbb', '90-ccc', '90-ddd')");
		                                      
		rStmnt.executeUpdate("insert into t1_r (id, c1, c2, c3, c4) values(00, '00-aaa', '00-bbb', '00-ccc', '00-ddd')");
		rStmnt.executeUpdate("insert into t1_r (id, c1, c2, c3, c4) values(10, '10-aaa', '10-bbb', '10-ccc', '10-ddd')");
		rStmnt.executeUpdate("insert into t1_r (id, c1, c2, c3, c4) values(20, '20-aaa', '20-bbb', '20-ccc', '20-ddd')");
		rStmnt.executeUpdate("insert into t1_r (id, c1, c2, c3, c4) values(30, '30-aaa', '30-bbb', '30-ccc', '30-ddd')");
		rStmnt.executeUpdate("insert into t1_r (id, c1, c2, c3, c4) values(40, '40-aaa', '40-bbb', '40-ccc', '40-ddd')");
		rStmnt.executeUpdate("insert into t1_r (id, c1, c2, c3, c4) values(50, '50-aaa', '50-bbb', '50-ccc', '50-ddd')");
		rStmnt.executeUpdate("insert into t1_r (id, c1, c2, c3, c4) values(60, '60-aaa', '60-bbb', '60-ccc', '60-ddd')");
		rStmnt.executeUpdate("insert into t1_r (id, c1, c2, c3, c4) values(70, '70-aaa', '70-bbb', '70-ccc', '70-ddd')");
		rStmnt.executeUpdate("insert into t1_r (id, c1, c2, c3, c4) values(80, '80-aaa', '80-bbb', '80-ccc', '80-ddd')");
		rStmnt.executeUpdate("insert into t1_r (id, c1, c2, c3, c4) values(90, '90-aaa', '90-bbb', '90-ccc', '90-ddd')");

		lStmnt.close();
		rStmnt.close();

		List<String> pkList = new ArrayList<>();
		pkList.add("id");
		pkList = null;
		
		DiffContext context = new DiffContext();
		context.setPkColumns(pkList);
		context.setDiffTable(DiffSide.LEFT,  _lConn.createStatement().executeQuery("select * from t1_l"), "select * from t1_l", null);
		context.setDiffTable(DiffSide.RIGHT, _rConn.createStatement().executeQuery("select * from t1_r"), "select * from t1_r", null);
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
		Statement lStmnt = _lConn.createStatement();
		Statement rStmnt = _rConn.createStatement();
		
		lStmnt.executeUpdate("drop table IF EXISTS t1_l");
		rStmnt.executeUpdate("drop table IF EXISTS t1_r");
		
		lStmnt.executeUpdate("create table t1_l (id int, c1 varchar(60), c2 varchar(60), c3 varchar(60), c4 varchar(60), primary key(id))");
		rStmnt.executeUpdate("create table t1_r (id int, c1 varchar(60), c2 varchar(60), c3 varchar(60), c4 varchar(60), primary key(id))");

		lStmnt.executeUpdate("insert into t1_l (id, c1, c2, c3, c4) values(10, NULL,     '10-bbb', '10-ccc', '10-ddd')");
		lStmnt.executeUpdate("insert into t1_l (id, c1, c2, c3, c4) values(20, '20-aaa', NULL,     '20-ccc', '20-ddd')");
		lStmnt.executeUpdate("insert into t1_l (id, c1, c2, c3, c4) values(30, '30-aaa', '30-bbb', NULL,     '30-ddd')");
		lStmnt.executeUpdate("insert into t1_l (id, c1, c2, c3, c4) values(40, '40-aaa', '40-bbb', '40-ccc', NULL    )");
		lStmnt.executeUpdate("insert into t1_l (id, c1, c2, c3, c4) values(50, NULL,     NULL,     '50-ccc', '50-ddd')");
		lStmnt.executeUpdate("insert into t1_l (id, c1, c2, c3, c4) values(60, NULL,     NULL,     NULL,     '50-ddd')");
		lStmnt.executeUpdate("insert into t1_l (id, c1, c2, c3, c4) values(70, NULL,     NULL,     NULL,     NULL    )");
		                                      
		rStmnt.executeUpdate("insert into t1_r (id, c1, c2, c3, c4) values(10, NULL,     '10-bbb', '10-ccc', '10-ddd')");
		rStmnt.executeUpdate("insert into t1_r (id, c1, c2, c3, c4) values(20, '20-aaa', NULL,     '20-ccc', '20-ddd')");
		rStmnt.executeUpdate("insert into t1_r (id, c1, c2, c3, c4) values(30, '30-aaa', '30-bbb', NULL,     '30-ddd')");
		rStmnt.executeUpdate("insert into t1_r (id, c1, c2, c3, c4) values(40, '40-aaa', '40-bbb', '40-ccc', NULL    )");
		rStmnt.executeUpdate("insert into t1_r (id, c1, c2, c3, c4) values(50, NULL,     NULL,     '50-ccc', '50-ddd')");
		rStmnt.executeUpdate("insert into t1_r (id, c1, c2, c3, c4) values(60, NULL,     NULL,     NULL,     '50-ddd')");
		rStmnt.executeUpdate("insert into t1_r (id, c1, c2, c3, c4) values(70, NULL,     NULL,     NULL,     NULL    )");

		lStmnt.close();
		rStmnt.close();

		List<String> pkList = new ArrayList<>();
		pkList.add("id");
		pkList = null;
		
		DiffContext context = new DiffContext();
		context.setPkColumns(pkList);
		context.setDiffTable(DiffSide.LEFT,  _lConn.createStatement().executeQuery("select * from t1_l"), "select * from t1_l", null);
		context.setDiffTable(DiffSide.RIGHT, _rConn.createStatement().executeQuery("select * from t1_r"), "select * from t1_r", null);
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
		Statement lStmnt = _lConn.createStatement();
		Statement rStmnt = _rConn.createStatement();
		
		lStmnt.executeUpdate("drop table IF EXISTS t1_l");
		rStmnt.executeUpdate("drop table IF EXISTS t1_r");
		
		lStmnt.executeUpdate("create table t1_l (id int, c1 varchar(60), c2 timestamp with time zone, c3 timestamp, c4 date, c5 time, c6 varbinary(10), c7 decimal(10,2), c8 clob, c9 blob, primary key(id))");
		rStmnt.executeUpdate("create table t1_r (id int, c1 varchar(60), c2 timestamp with time zone, c3 timestamp, c4 date, c5 time, c6 varbinary(10), c7 decimal(10,2), c8 clob, c9 blob, primary key(id))");

		//                                                                                    id  c1        c2                        c3                     c4            c5          c6                c7    c8                                 c9
		lStmnt.executeUpdate("insert into t1_l (id, c1, c2, c3, c4, c5, c6, c7, c8, c9) values(10, '10-aaa', '2019-01-01T11:11:11+01', '2019-01-01 11:11:11', '2019-01-01', '11:11:11', 0102030405060708, 1.11, '"+crClob("Clob-1-[${cnt}]: ")+"', RAWTOHEX('"+crClob("Blob-1-[${cnt}]: ")+"'))");
		lStmnt.executeUpdate("insert into t1_l (id, c1, c2, c3, c4, c5, c6, c7, c8, c9) values(20, '20-aaa', '2019-01-02T12:12:12+02', '2019-01-02 12:12:12', '2019-01-02', '12:12:12', 0102030405060708, 2.22, '"+crClob("Clob-2-[${cnt}]: ")+"', RAWTOHEX('"+crClob("Blob-2-[${cnt}]: ")+"'))");
		lStmnt.executeUpdate("insert into t1_l (id, c1, c2, c3, c4, c5, c6, c7, c8, c9) values(30, '30-aaa', '2019-01-03T13:13:13+03', '2019-01-03 13:13:13', '2019-01-03', '13:13:13', 0102030405060708, 3.33, '"+crClob("Clob-3-[${cnt}]: ")+"', RAWTOHEX('"+crClob("Blob-3-[${cnt}]: ")+"'))");
		lStmnt.executeUpdate("insert into t1_l (id, c1, c2, c3, c4, c5, c6, c7, c8, c9) values(40, '40-aaa', '2019-01-04T14:14:14+04', '2019-01-04 14:14:14', '2019-01-04', '14:14:14', 0102030405060708, 4.44, '"+crClob("Clob-4-[${cnt}]: ")+"', RAWTOHEX('"+crClob("Blob-4-[${cnt}]: ")+"'))");
		lStmnt.executeUpdate("insert into t1_l (id, c1, c2, c3, c4, c5, c6, c7, c8, c9) values(50, '50-aaa', '2019-01-05T15:15:15+05', '2019-01-05 15:15:15', '2019-01-05', '15:15:15', 0102030405060708, 5.55, '"+crClob("Clob-5-[${cnt}]: ")+"', RAWTOHEX('"+crClob("Blob-5-[${cnt}]: ")+"'))");
		lStmnt.executeUpdate("insert into t1_l (id, c1, c2, c3, c4, c5, c6, c7, c8, c9) values(60, '60-aaa', '2019-01-06T16:16:16+06', '2019-01-06 16:16:16', '2019-01-06', '16:16:16', 0102030405060708, 6.66, '"+crClob("Clob-6-[${cnt}]: ")+"', RAWTOHEX('"+crClob("Blob-6-[${cnt}]: ")+"'))");
		lStmnt.executeUpdate("insert into t1_l (id, c1, c2, c3, c4, c5, c6, c7, c8, c9) values(70, '70-aaa', '2019-01-07T17:17:17+07', '2019-01-07 17:17:17', '2019-01-07', '17:17:17', 0102030405060708, 7.77, '"+crClob("Clob-7-[${cnt}]: ")+"', RAWTOHEX('"+crClob("Blob-7-[${cnt}]: ")+"'))");
		lStmnt.executeUpdate("insert into t1_l (id, c1, c2, c3, c4, c5, c6, c7, c8, c9) values(80, '80-aaa', '2019-01-08T18:18:18+08', '2019-01-08 18:18:18', '2019-01-08', '18:18:18', 0102030405060708, 8.88, '"+crClob("Clob-8-[${cnt}]: ")+"', RAWTOHEX('"+crClob("Blob-8-[${cnt}]: ")+"'))");
		lStmnt.executeUpdate("insert into t1_l (id, c1, c2, c3, c4, c5, c6, c7, c8, c9) values(90, '90-aaa', '2019-01-09T19:19:19+09', '2019-01-09 19:19:19', '2019-01-09', '19:19:19', 0102030405060708, 9.99, '"+crClob("Clob-9-[${cnt}]: ")+"', RAWTOHEX('"+crClob("Blob-9-[${cnt}]: ")+"'))");

		rStmnt.executeUpdate("insert into t1_r (id, c1, c2, c3, c4, c5, c6, c7, c8, c9) values(10, '10-aaa', '2019-01-01T11:11:11+01', '2019-01-01 11:11:11', '2019-01-01', '11:11:11', 0102030405060708, 1.11, '"+crClob("Clob-1-[${cnt}]: ")+"', RAWTOHEX('"+crClob("Blob-1-[${cnt}]: ")+"'))");
		rStmnt.executeUpdate("insert into t1_r (id, c1, c2, c3, c4, c5, c6, c7, c8, c9) values(20, '20-aaa', '2019-01-02T12:12:12+02', '2019-01-02 12:12:12', '2019-01-02', '12:12:12', 0102030405060708, 2.22, '"+crClob("Clob-2-[${cnt}]: ")+"', RAWTOHEX('"+crClob("Blob-2-[${cnt}]: ")+"'))");
		rStmnt.executeUpdate("insert into t1_r (id, c1, c2, c3, c4, c5, c6, c7, c8, c9) values(30, '30-aaa', '2019-01-03T13:13:13+03', '2019-01-03 13:13:13', '2019-01-03', '13:13:13', 0102030405060708, 3.33, '"+crClob("Clob-3-[${cnt}]: ")+"', RAWTOHEX('"+crClob("Blob-3-[${cnt}]: ")+"'))");
		rStmnt.executeUpdate("insert into t1_r (id, c1, c2, c3, c4, c5, c6, c7, c8, c9) values(40, '40-aaa', '2019-01-04T14:14:14+04', '2019-01-04 14:14:14', '2019-01-04', '14:14:14', 0102030405060708, 4.44, '"+crClob("Clob-4-[${cnt}]: ")+"', RAWTOHEX('"+crClob("Blob-4-[${cnt}]: ")+"'))");
		rStmnt.executeUpdate("insert into t1_r (id, c1, c2, c3, c4, c5, c6, c7, c8, c9) values(50, '50-aaa', '2019-01-05T15:15:15+05', '2019-01-05 15:15:15', '2019-01-05', '15:15:15', 0102030405060708, 5.55, '"+crClob("Clob-5-[${cnt}]: ")+"', RAWTOHEX('"+crClob("Blob-5-[${cnt}]: ")+"'))");
		rStmnt.executeUpdate("insert into t1_r (id, c1, c2, c3, c4, c5, c6, c7, c8, c9) values(60, '60-aaa', '2019-01-06T16:16:16+06', '2019-01-06 16:16:16', '2019-01-06', '16:16:16', 0102030405060708, 6.66, '"+crClob("Clob-6-[${cnt}]: ")+"', RAWTOHEX('"+crClob("Blob-6-[${cnt}]: ")+"'))");
		rStmnt.executeUpdate("insert into t1_r (id, c1, c2, c3, c4, c5, c6, c7, c8, c9) values(70, '70-aaa', '2019-01-07T17:17:17+07', '2019-01-07 17:17:17', '2019-01-07', '17:17:17', 0102030405060708, 7.77, '"+crClob("Clob-7-[${cnt}]: ")+"', RAWTOHEX('"+crClob("Blob-7-[${cnt}]: ")+"'))");
		rStmnt.executeUpdate("insert into t1_r (id, c1, c2, c3, c4, c5, c6, c7, c8, c9) values(80, '80-aaa', '2019-01-08T18:18:18+08', '2019-01-08 18:18:18', '2019-01-08', '18:18:18', 0102030405060708, 8.88, '"+crClob("Clob-8-[${cnt}]: ")+"', RAWTOHEX('"+crClob("Blob-8-[${cnt}]: ")+"'))");
		rStmnt.executeUpdate("insert into t1_r (id, c1, c2, c3, c4, c5, c6, c7, c8, c9) values(90, '90-aaa', '2019-01-09T19:19:19+09', '2019-01-09 19:19:19', '2019-01-09', '19:19:19', 0102030405060708, 9.99, '"+crClob("Clob-9-[${cnt}]: ")+"', RAWTOHEX('"+crClob("Blob-9-[${cnt}]: ")+"'))");

		lStmnt.close();
		rStmnt.close();

		List<String> pkList = new ArrayList<>();
		pkList.add("id");
		
		DiffContext context = new DiffContext();
		context.setPkColumns(pkList);
		context.setDiffTable(DiffSide.LEFT,  _lConn.createStatement().executeQuery("select * from t1_l"), "select * from t1_l", null);
		context.setDiffTable(DiffSide.RIGHT, _rConn.createStatement().executeQuery("select * from t1_r"), "select * from t1_r", null);
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
		Statement lStmnt = _lConn.createStatement();
		Statement rStmnt = _rConn.createStatement();
		
		lStmnt.executeUpdate("drop table IF EXISTS t1_l");
		rStmnt.executeUpdate("drop table IF EXISTS t1_r");
		
		lStmnt.executeUpdate("create table t1_l (id int, c1 varchar(60), c2 varchar(60), c3 varchar(60), c4 varchar(60), primary key(id))");
		rStmnt.executeUpdate("create table t1_r (id int, c1 varchar(60), c2 varchar(60), c3 varchar(60), c4 varchar(60), primary key(id))");

		lStmnt.executeUpdate("insert into t1_l (id, c1, c2, c3, c4) values(10, NULL,     '10-bbb', '10-ccc', '10-ddd')");
		lStmnt.executeUpdate("insert into t1_l (id, c1, c2, c3, c4) values(20, '20-aaa', NULL,     '20-ccc', '20-ddd')");
		lStmnt.executeUpdate("insert into t1_l (id, c1, c2, c3, c4) values(30, '30-aaa', '30-bbb', NULL,     '30-ddd')");
		lStmnt.executeUpdate("insert into t1_l (id, c1, c2, c3, c4) values(40, '40-aaa', '40-bbb', '40-ccc', NULL    )");
		lStmnt.executeUpdate("insert into t1_l (id, c1, c2, c3, c4) values(50, NULL,     NULL,     '50-ccc', '50-ddd')");
		lStmnt.executeUpdate("insert into t1_l (id, c1, c2, c3, c4) values(60, NULL,     NULL,     NULL,     '50-ddd')");
		lStmnt.executeUpdate("insert into t1_l (id, c1, c2, c3, c4) values(70, NULL,     NULL,     NULL,     NULL    )");
		                                      
		rStmnt.executeUpdate("insert into t1_r (id, c1, c2, c3, c4) values(10, NULL,     '10-bbb', '10-ccc', '10-ddd')");
		rStmnt.executeUpdate("insert into t1_r (id, c1, c2, c3, c4) values(20, '20-aaa', NULL,     '20-ccc', '20-ddd')");
		rStmnt.executeUpdate("insert into t1_r (id, c1, c2, c3, c4) values(30, '30-aaa', '30-bbb', NULL,     '30-ddd')");
		rStmnt.executeUpdate("insert into t1_r (id, c1, c2, c3, c4) values(40, '40-aaa', '40-bbb', '40-ccc', NULL    )");
		rStmnt.executeUpdate("insert into t1_r (id, c1, c2, c3, c4) values(50, NULL,     NULL,     '50-ccc', '50-ddd')");

		lStmnt.close();
		rStmnt.close();

		List<String> pkList = new ArrayList<>();
		pkList.add("id");
		
		DiffContext context = new DiffContext();
		context.setPkColumns(pkList);
		context.setDiffTable(DiffSide.LEFT,  _lConn.createStatement().executeQuery("select * from t1_l"), "select * from t1_l", null);
		context.setDiffTable(DiffSide.RIGHT, _rConn.createStatement().executeQuery("select * from t1_r"), "select * from t1_r", null);
		context.setMessageDebugLevel(0); // 0=NO-DEBUG, 1=DEBUG, 2=TRACE
		context.validate();
		int diffCount = context.doDiff();

		assertEquals("Expected 2 in diffcount", 2, diffCount);
		

		GenerateSqlText lSqlGen = new GenerateSqlText(context, _lConn);
		GenerateSqlText rSqlGen = new GenerateSqlText(context, _rConn);

		List<String> leftFix  = lSqlGen.getSql(DiffSide.LEFT, null);
		List<String> rightFix = rSqlGen.getSql(DiffSide.RIGHT, null);
		
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
