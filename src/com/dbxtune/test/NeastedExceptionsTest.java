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

import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.SQLWarning;
import java.sql.Statement;
import java.util.Properties;

public class NeastedExceptionsTest
{
	// C:\projects\DbxTune>java -cp .\classes;lib\jdbc_drivers\mssql-jdbc-12.5.0-SNAPSHOT.jre8-preview.jar com.dbxtune.test.NeastedExceptionsTest
	// C:\projects\DbxTune>java -cp .\classes;lib\jdbc_drivers\jtds-1.3.1.jar com.dbxtune.test.NeastedExceptionsTest
	public static void main(String[] args)
	{
		System.out.println("Usage: hostname port user passwd [full-jdbc-url]");
		String host = "localhost";
		String port = "1433";
		String user = "gorans_sa";
		String pawd = "1niss2e";
		String fUrl = "";
		
		if (args.length > 0) host = args[0];
		if (args.length > 1) port = args[1];
		if (args.length > 2) user = args[2];
		if (args.length > 3) pawd = args[3];
		if (args.length > 4) fUrl = args[4];

		System.out.println("host = '"+host+"'");
		System.out.println("port = '"+port+"'");
		System.out.println("user = '"+user+"'");
		System.out.println("pawd = '"+pawd+"'");
		System.out.println("fUrl = '"+fUrl+"'");
		
//		String jdbcDriver    = "com.sybase.jdbc42.jdbc.SybDriver";
//		String jdbcUrl       = "jdbc:sybase:Tds:"+host+":"+port;
		String jdbcUrl       = "jdbc:sqlserver://"+host+":"+port;
//		String jdbcUrl       = "jdbc:jtds:sqlserver://"+host+":"+port;
		String jdbcUser      = user;
		String jdbcPasswd    = pawd;
		Properties jdbcProps = null;
		
		if ( ! "".equals(fUrl) )
			jdbcUrl = fUrl;

		System.out.println("---------------------------------------------------------------------------------------");
		System.out.println("Using URL '"+jdbcUrl+"' when connectiong to DBMS.");
		System.out.println("---------------------------------------------------------------------------------------");

		try
		{
//			Class.forName(jdbcDriver);

			Properties props = new Properties(jdbcProps);
			if (props.getProperty("user")                   == null) props.put("user",            jdbcUser);
			if (props.getProperty("password")               == null) props.put("password",        jdbcPasswd);
			
			if (jdbcUrl.startsWith("jdbc:sqlserver:"))
			{
				if (props.getProperty("encrypt")                == null) props.put("encrypt",                true+"");
				if (props.getProperty("trustServerCertificate") == null) props.put("trustServerCertificate", true+"");
			}

//			if (props.getProperty("APPLICATIONNAME") == null) props.put("APPLICATIONNAME", _appname);
//			if (props.getProperty("HOSTNAME")        == null) props.put("HOSTNAME",        hostname);
			
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

			
			String sql = "raiserror('AAAAAA sev=16', 16, 1) with nowait; raiserror('BBBBBB. sev=16', 16, 1) with nowait; raiserror('CCCCC. sev=10', 10, 1) with nowait";

			// With simple 'executeUpdate'
			System.out.println("");
			System.out.println("#####################################################");
			System.out.println("### executeUpdate('" + sql + "')");
			System.out.println("#####################################################");
			Statement stmnt = conn.createStatement();
			try //(Statement stmnt = conn.createStatement())
			{
				stmnt.executeUpdate(sql);

				SQLWarning ex = stmnt.getWarnings();
				while(ex != null)
				{
					System.out.println(">>> WARNING: Msg=" + ex.getErrorCode() + ", xxx=" + ex.getSQLState() + ", Text=|" + ex.getMessage() + "|.");
					ex = ex.getNextWarning();
				}
			}
			catch (SQLException ex)
			{
				while(ex != null)
				{
					System.out.println(">>> EXCEPTION: Msg=" + ex.getErrorCode() + ", xxx=" + ex.getSQLState() + ", Text=|" + ex.getMessage() + "|.");
					ex = ex.getNextException();
				}

				ex = stmnt.getWarnings();
				while(ex != null)
				{
					System.out.println(">>> WARNING: Msg=" + ex.getErrorCode() + ", xxx=" + ex.getSQLState() + ", Text=|" + ex.getMessage() + "|.");
					ex = ex.getNextException();
				}
			}
			finally {
				System.out.println("### stmnt.close()");
				stmnt.close();
			}
			
			
			
			sql = "exec p1 @doSelect = 0";

			// With simple 'executeUpdate'
			System.out.println("");
			System.out.println("#####################################################");
			System.out.println("### executeUpdate('" + sql + "')");
			System.out.println("#####################################################");
			stmnt = conn.createStatement();
			try //(Statement stmnt = conn.createStatement())
			{
				stmnt.executeUpdate(sql);

				SQLWarning ex = stmnt.getWarnings();
				while(ex != null)
				{
					System.out.println(">>> WARNING: Msg=" + ex.getErrorCode() + ", xxx=" + ex.getSQLState() + ", Text=|" + ex.getMessage() + "|.");
					ex = ex.getNextWarning();
				}
			}
			catch (SQLException ex)
			{
				while(ex != null)
				{
					System.out.println(">>> EXCEPTION: Msg=" + ex.getErrorCode() + ", xxx=" + ex.getSQLState() + ", Text=|" + ex.getMessage() + "|.");
					ex = ex.getNextException();
				}

				ex = stmnt.getWarnings();
				while(ex != null)
				{
					System.out.println(">>> WARNING: Msg=" + ex.getErrorCode() + ", xxx=" + ex.getSQLState() + ", Text=|" + ex.getMessage() + "|.");
					ex = ex.getNextException();
				}
			}
			finally {
				System.out.println("### stmnt.close()");
				stmnt.close();
			}
			
//			sql = "exec p1 @doSelect = 1";

			// With simple 'executeQuery'
			System.out.println("");
			System.out.println("#####################################################");
			System.out.println("### executeQuery('" + sql + "')");
			System.out.println("#####################################################");
//			try (Statement stmnt = conn.createStatement(); ResultSet rs = stmnt.executeQuery(sql))
			try
			{
				stmnt = conn.createStatement();
				try (ResultSet rs = stmnt.executeQuery(sql))
				{
					while(rs.next())
					{
						System.out.println("RS: " + rs.getString(1));
					}
					SQLWarning ex = stmnt.getWarnings();
					while(ex != null)
					{
						System.out.println(">>> WARNING: Msg=" + ex.getErrorCode() + ", xxx=" + ex.getSQLState() + ", Text=|" + ex.getMessage() + "|.");
						ex = ex.getNextWarning();
					}
				}
			}
			catch (SQLException ex)
			{
				while(ex != null)
				{
					System.out.println(">>> EXCEPTION: Msg=" + ex.getErrorCode() + ", xxx=" + ex.getSQLState() + ", Text=|" + ex.getMessage() + "|.");
					ex = ex.getNextException();
				}

				ex = stmnt.getWarnings();
				while(ex != null)
				{
					System.out.println(">>> WARNING: Msg=" + ex.getErrorCode() + ", xxx=" + ex.getSQLState() + ", Text=|" + ex.getMessage() + "|.");
					ex = ex.getNextException();
				}
			}
			finally {
				System.out.println("### stmnt.close()");
				stmnt.close();
			}
			
			// With simple 'execute'
			System.out.println("");
			System.out.println("#####################################################");
			System.out.println("### execute('" + sql + "')");
			System.out.println("#####################################################");
//			try (Statement stmnt = conn.createStatement())
			try
			{
				stmnt = conn.createStatement();

				boolean hasRs = stmnt.execute(sql);
				int rowsAffected = 0;
				int rsNum = 0;

				// iterate through each result set
				do
				{
					System.out.println(" --- loop-start-");
					if(hasRs)
					{
						rsNum++;
						
						// Get next ResultSet to work with
						ResultSet rs = stmnt.getResultSet();

						while(rs.next())
						{
							System.out.println("RS[" + rsNum + "]: " + rs.getString(1));
						}

						// Close it
						try {
							rs.close();
						} catch (SQLException ignore) {
							ignore.printStackTrace();
						}
					}
					else
					{
						rowsAffected = stmnt.getUpdateCount();
					}

					// Check if we have more ResultSets
					hasRs = stmnt.getMoreResults();

					System.out.println(" --- loop-check-at-end: hasRs=" + hasRs + ", rowsAffected=" + rowsAffected);
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
				while(ex != null)
				{
					System.out.println(">>> EXCEPTION: Msg=" + ex.getErrorCode() + ", xxx=" + ex.getSQLState() + ", Text=|" + ex.getMessage() + "|.");
					ex = ex.getNextException();
				}

				ex = stmnt.getWarnings();
				while(ex != null)
				{
					System.out.println(">>> WARNING: Msg=" + ex.getErrorCode() + ", xxx=" + ex.getSQLState() + ", Text=|" + ex.getMessage() + "|.");
					ex = ex.getNextException();
				}
			}
			finally {
				System.out.println("### stmnt.close()");
				stmnt.close();
			}
			
			System.out.println("");
			System.out.println("Disconnecting from DBMS...");
			conn.close();
		}
		catch (Exception e)
		{
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
	}
}
