/*******************************************************************************
 * Copyright (C) 2010-2020 Goran Schwarz
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
package com.asetune.pcs;

import static org.junit.Assert.assertEquals;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Types;
import java.util.Properties;

import org.apache.log4j.PropertyConfigurator;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import com.asetune.sql.conn.DbxConnection;

public class DictCompressionTest
{
	private static final String DB_URL        = "jdbc:h2:mem:testdb";
	private static final String DB_USER       = "sa";
	private static final String DB_PASSWORD   = "";

	private DbxConnection _conn; 
			

	@BeforeClass
	public static void init() 
	{
		//System.out.println("@BeforeClass - init():");

		Properties log4jProps = new Properties();
		log4jProps.setProperty("log4j.rootLogger", "INFO, A1");
//		log4jProps.setProperty("log4j.rootLogger", "DEBUG, A1");
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
		Connection jdbcConn = DriverManager.getConnection(DB_URL, DB_USER, DB_PASSWORD);
		_conn = DbxConnection.createDbxConnection(jdbcConn);

		// Create an instance for: Dictionary Compression 
		// This is done on any calls to: DictCompression.getInstance()
//		DictCompression dcc = new DictCompression(DictCompression.DigestType.MD5);
//		DictCompression.setInstance(dcc);
	}
	
	@After
	public void close()
	throws SQLException
	{
		//System.out.println("@After - close():");
		if (_conn != null) _conn.close();
	}
	
	@AfterClass
	public static void tearDown()
	{
		//System.out.println("@AfterClass - tearDown():");
	}

	@Test
	public void testQuotifyStringSimple()
	throws Exception
	{
		DictCompression dcc = DictCompression.getInstance();
		
		dcc.createTable(_conn, null, "CmDummy", "sql", Types.CLOB, -1, true);

		// first time
		String digest_null  = dcc.addToBatch(_conn, "CmDummy", "sql", null);
		String digest_1_1_1 = dcc.addToBatch(_conn, "CmDummy", "sql", "dummy val 1");
		String digest_1_1_2 = dcc.addToBatch(_conn, "CmDummy", "sql", "dummy val 2");
		String digest_1_1_3 = dcc.addToBatch(_conn, "CmDummy", "sql", "dummy val 3");

		// first time, should only return digest, but no records to add
		String digest_1_2_1 = dcc.addToBatch(_conn, "CmDummy", "sql", "dummy val 1");
		String digest_1_2_2 = dcc.addToBatch(_conn, "CmDummy", "sql", "dummy val 2");
		String digest_1_2_3 = dcc.addToBatch(_conn, "CmDummy", "sql", "dummy val 3");

		assertEquals("Digest hash check null", digest_null, null);
		assertEquals("Digest hash check 1-1" , digest_1_1_1, digest_1_2_1);
		assertEquals("Digest hash check 1-2" , digest_1_1_2, digest_1_2_2);
		assertEquals("Digest hash check 1-3" , digest_1_1_3, digest_1_2_3);

		// After store: So 3 rows in table
		dcc.storeBatch(_conn);
		
		checkExpectedRowCount(_conn, 3, "CmDummy", "sql", "after FIRST batch");

		
		// 
		String digest_2_1_1 = dcc.addToBatch(_conn, "CmDummy", "sql", "dummy val 1");
		String digest_2_1_2 = dcc.addToBatch(_conn, "CmDummy", "sql", "dummy val 2");
		String digest_2_1_3 = dcc.addToBatch(_conn, "CmDummy", "sql", "dummy val 3");

		// 
		String digest_2_2_1 = dcc.addToBatch(_conn, "CmDummy", "sql", "dummy val 1");
		String digest_2_2_2 = dcc.addToBatch(_conn, "CmDummy", "sql", "dummy val 2");
		String digest_2_2_3 = dcc.addToBatch(_conn, "CmDummy", "sql", "dummy val 3");

		assertEquals("Digest hash check 2-1", digest_1_1_1, digest_2_2_1);
		assertEquals("Digest hash check 2-2", digest_1_1_2, digest_2_2_2);
		assertEquals("Digest hash check 2-3", digest_1_1_3, digest_2_2_3);

		// After store: So 3 rows in table
		dcc.storeBatch(_conn);

		checkExpectedRowCount(_conn, 3, "CmDummy", "sql", "after SECOND batch");
	}
	
	private void checkExpectedRowCount(DbxConnection conn, int expRowsInTab, String tabName, String colName, String desc)
	throws SQLException
	{
		int actRowsInTab = 0;
		String sql = "select count(*) from [" + tabName + DictCompression.DCC_MARKER + colName + "]";
		sql = conn.quotifySqlString(sql);
		
		try (Statement stmnt = conn.createStatement(); ResultSet rs = stmnt.executeQuery(sql))
		{
			while(rs.next())
				actRowsInTab = rs.getInt(1);
		}

		assertEquals("Not correct number of rows in DictCompress table: " + desc, expRowsInTab, actRowsInTab);
	}
}
