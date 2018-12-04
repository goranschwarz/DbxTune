package com.asetune.sql;

import static org.junit.Assert.assertEquals;

import java.util.Properties;

import org.apache.log4j.PropertyConfigurator;
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
//		Properties log4jProps = new Properties();
//		log4jProps.setProperty("log4j.rootLogger", "INFO, A1");
//		//log4jProps.setProperty("log4j.rootLogger", "DEBUG, A1");
//		log4jProps.setProperty("log4j.appender.A1", "org.apache.log4j.ConsoleAppender");
//		log4jProps.setProperty("log4j.appender.A1.layout", "org.apache.log4j.PatternLayout");
//		log4jProps.setProperty("log4j.appender.A1.layout.ConversionPattern", "%d - %-5p - %-30c{1} - %m%n");
//		PropertyConfigurator.configure(log4jProps);

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
