/*******************************************************************************
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
package com.dbxtune.test;

import java.io.FileInputStream;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.SQLWarning;
import java.sql.Statement;
import java.util.Properties;

/**
 * 
 */
public class JdbcPerfTest
{

	public static void main(String[] args)
	{
		if (args.length == 0)
		{
			System.out.println("");
			System.out.println("Usage: jdbcUrl|propFile user passwd [seconds] [sql]");
			System.out.println("");
			return;
		}
		
		String url  = "jdbc:sybase:Tds:localhost:5000";
		String user = "sa";
		String pawd = "password";
		String tSec = "60";
		String sql  = "";
		
		if (args.length > 0) url  = args[0];
		if (args.length > 1) user = args[1];
		if (args.length > 2) pawd = args[2];
		if (args.length > 3) tSec = args[3];
		if (args.length > 4) sql  = args[4];

		System.out.println("url  = '" + url  + "'");
		System.out.println("user = '" + user + "'");
		System.out.println("pawd = '" + pawd + "'");
		System.out.println("tSec = '" + tSec + "'");
		System.out.println("sql  = '" + sql  + "'");
		
//		String jdbcDriver    = "com.sybase.jdbc42.jdbc.SybDriver";
//		String jdbcUrl       = "jdbc:sybase:Tds:" + host + ":" + port;
//		String jdbcUrl       = "jdbc:sqlserver://"+host+":"+port;
//		String jdbcUrl       = "jdbc:jtds:sqlserver://"+host+":"+port;

		// if URL is a file, then read properties from that file
		if ( ! url.startsWith("jdbc:") )
		{
			System.out.println("Reading properties from file '" + url + "'.");
			
			Properties props = new Properties();
			try
			{
				props.load( new FileInputStream(url) );

				url  = props.getProperty("jdbc.url"             , "");
				user = props.getProperty("jdbc.username"        , "");
				pawd = props.getProperty("jdbc.password"        , "");
				tSec = props.getProperty("jdbc.testTime.seconds", "60");
				sql  = props.getProperty("jdbc.sql"             , "");

				if ("".equals(url )) throw new Exception("Missing property 'url'.");
				if ("".equals(user)) throw new Exception("Missing property 'username'.");
				if ("".equals(pawd)) throw new Exception("Missing property 'password'.");
			}
			catch (Exception ex)
			{
				System.out.println("");
				System.out.println("Problems Reading file '" + url + "'. Caught: " + ex);
				System.out.println("");
				System.out.println("Example File content:");
				System.out.println("-------------------------------------------------------------------------");
				System.out.println("jdbc.url              = jdbc:sybase:Tds:localhost:5000");
				System.out.println("jdbc.username         = sa");
				System.out.println("jdbc.password         = secretPassword");
				System.out.println("jdbc.testTime.seconds = 60");
				System.out.println("jdbc.sql              = select 'SQL Statement you want to execute...'");
				System.out.println("-------------------------------------------------------------------------");
				System.out.println("");
				return;
			}
			
		}
		String jdbcUrl    = url;
		String jdbcUser   = user;
		String jdbcPasswd = pawd;

		// Make up some dummy SQL
		if ("".equals(sql))
		{
			String s1 = "xxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxx";
			sql = "select '" + s1 + "', '" + s1 + "', '" + s1 + "', '" + s1 + "', '" + s1 + "', '" + s1 + "'";
		}

		
		Properties jdbcProps = null;
		
//		if ( ! "".equals(fUrl) )
//			jdbcUrl = fUrl;

		System.out.println("---------------------------------------------------------------------------------------");
		System.out.println("Using URL '" + jdbcUrl + "' when connectiong to DBMS.");
		System.out.println("---------------------------------------------------------------------------------------");

		try
		{
			Properties props = new Properties(jdbcProps);
			if (props.getProperty("user")                   == null) props.put("user",            jdbcUser);
			if (props.getProperty("password")               == null) props.put("password",        jdbcPasswd);
			
			// Add some for SQL Server
			if (jdbcUrl.startsWith("jdbc:sqlserver:"))
			{
				if (props.getProperty("encrypt")                == null) props.put("encrypt",                true+"");
				if (props.getProperty("trustServerCertificate") == null) props.put("trustServerCertificate", true+"");
			}

			// Add some for Sybase ASE
			if (jdbcUrl.startsWith("jdbc:sybase:Tds:"))
			{
				if (props.getProperty("APPLICATIONNAME") == null) props.put("APPLICATIONNAME", "JdbPerfTest");
//				if (props.getProperty("HOSTNAME")        == null) props.put("HOSTNAME",        hostname);
			}
			
			Connection conn = null;
			conn = DriverManager.getConnection(jdbcUrl, props);

			DatabaseMetaData dbmd = conn.getMetaData();
			if (dbmd != null)
			{
				System.out.println("JDBC driver name:     " + dbmd.getDriverName());
				System.out.println("JDBC driver version:  " + dbmd.getDriverVersion());
				System.out.println("DBMS Product Name:    " + dbmd.getDatabaseProductName());
				System.out.println("DBMS Product Version: " + dbmd.getDatabaseProductVersion());
			}

			long execCountTotal     = 0;
			long execCountIntervall = 0;
			long execTimeMsTotal    = 0;
			long testTimeMs         = 60 * 1000;
			long reportIntervall    = 1_000;
			long nextRptIntervall   = reportIntervall;
			
			// Parse Test Seconds
			try 
			{
				testTimeMs = Integer.parseInt(tSec) * 1000;
			} 
			catch (NumberFormatException ex) 
			{
				System.out.println("Problems parsing 'test time'. Value='" + tSec + "' is NOT an integer.");
				return;
			}

			System.out.println("");
			System.out.println("===========================================================");
			System.out.println(">> Starting: ");
			System.out.println(">> TestTimeSec: " + testTimeMs);
			System.out.println(">> SQL:         " + sql);
			System.out.println(">> SQL Size:    " + sql.length());
			System.out.println("===========================================================");
			
			long startTimeTotal     = System.currentTimeMillis();
			long startTimeIntervall = System.currentTimeMillis();

			boolean doRun = true; 
			while(doRun)
			{
				long now = System.currentTimeMillis();
				execTimeMsTotal = now - startTimeTotal;
				if (execTimeMsTotal >= testTimeMs)
				{
					doRun = false;
					break;
				}
				
				execCountTotal++;
				execCountIntervall++;

				execSql(conn, sql);
				
				if (execTimeMsTotal >= nextRptIntervall)
				{
					nextRptIntervall += reportIntervall;
					long execTimeMsIntervall = now - startTimeIntervall;
					
					double xPerSec = (execCountTotal * 1.0) / (execTimeMsTotal / 1000.0);
					System.out.println("    -- execTimeSec=" + (execTimeMsTotal / 1_000) + ", execPerSec=" + round(xPerSec, 1) + ", avgMsPerCmd=" + round( (execTimeMsIntervall * 1.0) / (execCountIntervall * 1.0), 3) );
					
					startTimeIntervall = System.currentTimeMillis();
					execCountIntervall = 0;
				}
			}
			
			double xPerSec     = (execCountTotal * 1.0) / (execTimeMsTotal / 1000.0);
			double avgMsPerCmd = (execTimeMsTotal * 1.0) / (execCountTotal * 1.0);

			System.out.println("");
			System.out.println("===========================================================");
			System.out.println("<< Report:      " + jdbcUrl);
			System.out.println("<< ExecCount:   " + execCountTotal);
			System.out.println("<< execTimeMs:  " + execTimeMsTotal);
			System.out.println("<< execPerSec:  " + round(xPerSec, 1));
			System.out.println("<< avgMsPerCmd: " + round(avgMsPerCmd, 3));
			System.out.println("===========================================================");
			System.out.println("");
		}
		catch (SQLException ex)
		{
			ex.printStackTrace();
		}
	}

	/**
	 * Round a double value at some decimals
	 * @param val
	 * @param places
	 */
	public static double round(double value, int places)
	{
		if (places < 0) 
			throw new IllegalArgumentException();

		BigDecimal bd = new BigDecimal(value);
		bd = bd.setScale(places, RoundingMode.HALF_UP);
	    return bd.doubleValue();

//	    return new BigDecimal( value ).setScale(places, RoundingMode.HALF_UP).doubleValue();
	}

	public static void execSql(Connection conn, String sql) 
	throws SQLException
	{
		try (Statement stmnt = conn.createStatement())
		{
			boolean hasRs = stmnt.execute(sql);
			int rowsAffected = 0;

			// iterate through each result set
			do
			{
				if(hasRs)
				{
					// Get next ResultSet to work with
					ResultSet rs = stmnt.getResultSet();

					while(rs.next())
					{
					}

					// Close it
					try { rs.close(); } 
					catch (SQLException ignore) { ignore.printStackTrace();	}
				}
				else
				{
					rowsAffected = stmnt.getUpdateCount();
				}

				// Check if we have more ResultSets
				hasRs = stmnt.getMoreResults();
			}
			while (hasRs || rowsAffected != -1);

			SQLWarning ex = stmnt.getWarnings();
			while(ex != null)
			{
				System.out.println(">>> WARNING: Msg=" + ex.getErrorCode() + ", xxx=" + ex.getSQLState() + ", Text=|" + ex.getMessage() + "|.");
				ex = ex.getNextWarning();
			}
		
		}
		catch (SQLException ex)
		{
			SQLException originEx = ex;

			while(ex != null)
			{
				System.out.println(">>> EXCEPTION: Msg=" + ex.getErrorCode() + ", xxx=" + ex.getSQLState() + ", Text=|" + ex.getMessage() + "|.");
				ex = ex.getNextException();
			}

//			ex = stmnt.getWarnings();
//			while(ex != null)
//			{
//				System.out.println(">>> WARNING: Msg=" + ex.getErrorCode() + ", xxx=" + ex.getSQLState() + ", Text=|" + ex.getMessage() + "|.");
//				ex = ex.getNextException();
//			}

			throw originEx;
		}
	}
}
