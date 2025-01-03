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
package com.dbxtune.sql.conn;

import static org.junit.Assert.assertEquals;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.core.config.Configurator;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

public class DbxConnectionTest
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
		Configurator.setRootLevel(Level.TRACE);
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
	public void testQuotifyStringSimple()
	throws Exception
	{
		String source = "select * from [t1] where [c1] = 'xxx[]yyy'";
		String expect = "select * from \"t1\" where \"c1\" = 'xxx[]yyy'";
		
		String result = _dbxConn.quotifySqlString(source);
		
//		System.out.println("");
//		System.out.println("source=|"+source+"|");
//		System.out.println("expect=|"+expect+"|");
//		System.out.println("result=|"+result+"|");
		
		assertEquals("Square Brackets In SQL-String-constant not correct", result, expect);

	}

	@Test
	public void testQuotifyStringDoubleSingleQuoteInString()
	throws Exception
	{
		String source = "select * from [t1] where [c1] = 'xx''x[]y\"yy'";
		String expect = "select * from \"t1\" where \"c1\" = 'xx''x[]y\"yy'";
		
		String result = _dbxConn.quotifySqlString(source);

//		System.out.println("");
//		System.out.println("source=|"+source+"|");
//		System.out.println("expect=|"+expect+"|");
//		System.out.println("result=|"+result+"|");
		
		assertEquals("Escaped Single Quotes in SQL-String-constant not correct", result, expect);

	}

	@Test
	public void testQuotifyStringWithSquareBracketInIdentifier()
	throws Exception
	{
		String source = "create table [t1] ([id] int, [[150]] waiting for a lock] varchar(10), [c2] varbinary(10), primary key([id]))";
		String expect = "create table #t1# (#id# int, #[150] waiting for a lock# varchar(10), #c2# varbinary(10), primary key(#id#))".replace('#', '"');

		String result = _dbxConn.quotifySqlString(source);
		
//		System.out.println("");
//		System.out.println("source=|"+source+"|");
//		System.out.println("expect=|"+expect+"|");
//		System.out.println("result=|"+result+"|");
		
		assertEquals("Square Brackets In SQL-String-constant not correct", result, expect);
	}

	@Test
	public void testQuotifyStringWithAdvanced()
	throws Exception
	{
		String source = "create table [t'1] ([id] int, []]] int, [[] int, [[150]] 12\" lock, and two \"\" quotes] varchar(10), [c2] varbinary(10), primary key([id]))";
		String expect = "create table #t'1# (#id# int, #]# int, #[# int, #[150] 12\" lock, and two \"\" quotes# varchar(10), #c2# varbinary(10), primary key(#id#))".replace('#', '"');

		// Note: This would cause an Syntax error in H2 due to "...12"..." (the not escaped " in the middle)  
		//       but I just wanted to test...  
		
		String result = _dbxConn.quotifySqlString(source);
		
//		System.out.println("");
//		System.out.println("source=|"+source+"|");
//		System.out.println("expect=|"+expect+"|");
//		System.out.println("result=|"+result+"|");
		
		assertEquals("Square Brackets In SQL-String-constant not correct", result, expect);
	}
}
