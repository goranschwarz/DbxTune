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
package com.dbxtune.sql.conn.ddl.model;

import static org.junit.Assert.assertEquals;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import com.dbxtune.sql.conn.DbxConnection;
import com.dbxtune.sql.ddl.model.Table;

public class TableTest
{

	private static final String DB_URL        = "jdbc:h2:mem:testdb";
	private static final String DB_USER       = "sa";
	private static final String DB_PASSWORD   = "";

	private DbxConnection _dbxConn; 
			

	@BeforeClass
	public static void init() 
	{
		//System.out.println("@BeforeClass - init():");

		// Set Log4j Log Level
//		Configurator.setRootLevel(Level.TRACE);
	}

	@Before
	public void setup()
	throws SQLException
	{
		//System.out.println("@Before - setup: ");
		Connection jdbcConn = DriverManager.getConnection(DB_URL, DB_USER, DB_PASSWORD);
		_dbxConn = DbxConnection.createDbxConnection(jdbcConn);
	}
	
	@After
	public void close()
	throws SQLException
	{
		//System.out.println("@After - close():");
		if (_dbxConn != null) _dbxConn.close();
	}
	
	@AfterClass
	public static void tearDown()
	{
		//System.out.println("@AfterClass - tearDown():");
	}


	@Test
	public void basicTest()
	throws Exception
	{
		String crTab1 = "create table t1 "
				+ "("
				+ "    c1       int"
				+ "   ,c2       int"
				+ "   ,c3       int"
				+ "   ,c4       int"
				+ "   ,c5       int"
				+ "   ,c6       int"
				+ "   ,c7       int"
				+ "   ,c8       int"
				+ "   ,c9       int"
				+ "   ,primary key(c1, c2, c3)"
				+ ")";
		String crTab1Ix1 = "create unique index ix1_unique_c2345 on t1(c2, c3, c4, c5)";
		String crTab1Ix2 = "create        index ix2_c9           on t1(c9)";
		String crTab1Ix3 = "create        index ix3_c456789      on t1(c4, c5, c6, c7, c8 desc, c9 desc)";
		
		_dbxConn.dbExec(crTab1);
		_dbxConn.dbExec(crTab1Ix1);
		_dbxConn.dbExec(crTab1Ix2);
		_dbxConn.dbExec(crTab1Ix3);

		Table t1 = Table.create(_dbxConn, null, null, "t1");  // The passed "t1" is in LowerCase... the create() method should figure out what the DBMS uses to store it in the metadata tables.
		
		assertEquals("Expected table name 'T1'",  t1.getTableName(),             "T1");
		assertEquals("Expected Columns to be 9",  t1.getColumns()       .size(), 9);
		assertEquals("Expected PK cols to be 3",  t1.getPkColumns()     .size(), 3);
		assertEquals("Expected Index Count be 4", t1.getIndexes()       .size(), 4);
		assertEquals("Expected FK count to be 0", t1.getForeignKeysOut().size(), 0);
		
//		System.out.println("pkName='" + t1.getPkName() + "', and Cols="+t1.getPkColumns());
		
		
		_dbxConn.dbExec("drop table t1");
	}

	@Test
	public void basicFkTest()
	throws Exception
	{
		// TAB-1
		String crTab1 = "create table OrderHead "
				+ "("
				+ "    oid      int"
				+ "   ,c1       int"
				+ "   ,c2       int"
				+ "   ,c3       int"
				+ "   ,primary key(oid)"
				+ ")";
		String crTab1Ix1 = "create unique index ix1_unique_c12 on OrderHead(c1,c2)";
		String crTab1Ix2 = "create        index ix2_c3         on OrderHead(c3)";
		
		_dbxConn.dbExec(crTab1);
		_dbxConn.dbExec(crTab1Ix1);
		_dbxConn.dbExec(crTab1Ix2);

		// TAB-2
		String crTab2 = "create table [OrderRow] "
				+ "("
				+ "    oid      int"
				+ "   ,rid      int"
				+ "   ,c1       int"
				+ "   ,c2       int"
				+ "   ,c3       int"
				+ "   ,c4       int"
				+ "   ,c5       int"
				+ "   ,c6       int"
				+ "   ,primary key(oid, rid)"
				+ "   ,foreign key(oid) references OrderHead(oid)"
				+ ")";
		String crTab2Ix1 = "create unique index ix1_unique_c2345 on [OrderRow](c2, c3, c4, c5)";
		String crTab2Ix2 = "create        index ix2_c6           on [OrderRow](c6)";
		String crTab2Ix3 = "create        index ix3_c456         on [OrderRow](c4, c5, c6 desc)";
		
		_dbxConn.dbExec(_dbxConn.quotifySqlString(crTab2));
		_dbxConn.dbExec(_dbxConn.quotifySqlString(crTab2Ix1));
		_dbxConn.dbExec(_dbxConn.quotifySqlString(crTab2Ix2));
		_dbxConn.dbExec(_dbxConn.quotifySqlString(crTab2Ix3));


//		DatabaseMetaData dbmd = _dbxConn.getMetaData();
//		ResultSetTableModel mdPk    = new ResultSetTableModel( dbmd.getPrimaryKeys (null, null, "OrderRow"), "getPrimaryKeys");
//		ResultSetTableModel mdIndex = new ResultSetTableModel( dbmd.getIndexInfo   (null, null, "OrderRow", false, false), "getIndexInfo");
//		System.out.println(mdPk.toAsciiTableString());
//		System.out.println(mdIndex.toAsciiTableString());

		
		Table orderHead = Table.create(_dbxConn, null, null, "orderHead");     // The passed "tab" is in LowerCase... the create() method should figure out what the DBMS uses to store it in the metadata tables.
		Table orderRow  = Table.create(_dbxConn, null, null, "OrderRow");  // The passed "tab" is in LowerCase... the create() method should figure out what the DBMS uses to store it in the metadata tables.
		
		assertEquals("Expected table name 'ORDERHEAD'",  orderHead.getTableName(),    "ORDERHEAD");
		assertEquals("Expected Columns to be 4",         orderHead.getColumns()       .size(), 4);
		assertEquals("Expected PK cols to be 1",         orderHead.getPkColumns()     .size(), 1);
		assertEquals("Expected Index Count be 3",        orderHead.getIndexes()       .size(), 3);
		assertEquals("Expected FK count to be 0",        orderHead.getForeignKeysOut().size(), 0);
		
		assertEquals("Expected table name 'OrderRow'",   orderRow.getTableName(),    "OrderRow");
		assertEquals("Expected Columns to be 4",         orderRow.getColumns()       .size(), 8);
		assertEquals("Expected PK cols to be 2",         orderRow.getPkColumns()     .size(), 2);
		assertEquals("Expected Index Count be 5",        orderRow.getIndexes()       .size(), 5); // I expect 4.. but H2 adds a index for the OrderHead->OID... keee... 
		assertEquals("Expected FK count to be 1",        orderRow.getForeignKeysOut().size(), 1);
		

		_dbxConn.dbExec(_dbxConn.quotifySqlString("drop table [OrderRow]"));
		_dbxConn.dbExec(_dbxConn.quotifySqlString("drop table orderHead"));
	}
}
