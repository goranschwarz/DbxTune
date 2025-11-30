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
 * the Free Software Foundation, version 3 of the License.
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

import org.junit.Test;

public class JdbcUrlParserTest
{
	@Test(expected = IllegalArgumentException.class)
	public void testBlankUrl()
	{
		JdbcUrlParser.parse("");
	}
	
	@Test
	public void test()
	{
		JdbcUrlParser p;
		String url = "jdbc:sybase:Tds:host1:5000?ENCRYPT_PASSWORD=true";
		assertEquals(5000,         JdbcUrlParser.parse(url).getPort());
		assertEquals("host1",      JdbcUrlParser.parse(url).getHost());
		assertEquals("host1:5000", JdbcUrlParser.parse(url).getHostPortStr());


		url = "jdbc:h2:file:C:\\Users\\gorans\\.dbxtune\\data/DBXTUNE_CENTRAL_DB;DATABASE_TO_UPPER=false;MAX_COMPACT_TIME=2000;COMPRESS=TRUE;WRITE_DELAY=30000;DB_CLOSE_ON_EXIT=FALSE";
		p = JdbcUrlParser.parse(url);
//		System.out.println("--- x:" + p);
		assertEquals(-1,   JdbcUrlParser.parse(url).getPort());
		assertEquals(null, JdbcUrlParser.parse(url).getHost());
		assertEquals(null, JdbcUrlParser.parse(url).getHostPortStr());
		
		url = "jdbc:h2:file:C:/Users/gorans/.dbxtune/data/DBXTUNE_CENTRAL_DB;DATABASE_TO_UPPER=false;MAX_COMPACT_TIME=2000;COMPRESS=TRUE;WRITE_DELAY=30000;DB_CLOSE_ON_EXIT=FALSE";
		p = JdbcUrlParser.parse(url);
//		System.out.println("--- y:" + p);
		assertEquals(-1,   JdbcUrlParser.parse(url).getPort());
		assertEquals(null, JdbcUrlParser.parse(url).getHost());
		assertEquals(null, JdbcUrlParser.parse(url).getHostPortStr());

		url = "jdbc:h2:tcp://localhost/C:\\Users\\gorans\\.dbxtune\\data/DBXTUNE_CENTRAL_DB";
		p = JdbcUrlParser.parse(url);
//		System.out.println("--- z1:" + p);
		assertEquals(9092,         JdbcUrlParser.parse(url).getPort());
		assertEquals("localhost",  JdbcUrlParser.parse(url).getHost());
		assertEquals("localhost",  JdbcUrlParser.parse(url).getHostPortStr());
		
		url = "jdbc:h2:tcp://localhost:12345/C:\\Users\\gorans\\.dbxtune\\data/DBXTUNE_CENTRAL_DB";
		p = JdbcUrlParser.parse(url);
//		System.out.println("--- z2:" + p);
		assertEquals(12345,             JdbcUrlParser.parse(url).getPort());
		assertEquals("localhost",       JdbcUrlParser.parse(url).getHost());
		assertEquals("localhost:12345", JdbcUrlParser.parse(url).getHostPortStr());
		
		url = "jdbc:postgresql://host1:5432/postgres";
		p = JdbcUrlParser.parse(url);
//		System.out.println("--- x:" + p);
		assertEquals(5432,         JdbcUrlParser.parse(url).getPort());
		assertEquals("host1",      JdbcUrlParser.parse(url).getHost());
		assertEquals("host1:5432", JdbcUrlParser.parse(url).getHostPortStr());
		
		url = "jdbc:postgresql://host1/postgres";
		p = JdbcUrlParser.parse(url);
//		System.out.println("--- x:" + p);
		assertEquals(-1,      JdbcUrlParser.parse(url).getPort());
		assertEquals("host1", JdbcUrlParser.parse(url).getHost());
		assertEquals("host1", JdbcUrlParser.parse(url).getHostPortStr());
		
	}

}
