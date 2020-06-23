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
package com.asetune.test;

import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Properties;

public class JdbcConnectTest
{
	public static int getSpid(Connection conn)
	{
		int spid = -1;
		String sql = "select @@spid";

		try
		{
			Statement stmnt = conn.createStatement();

			ResultSet rs = stmnt.executeQuery(sql);
			while (rs.next())
			{
				spid = rs.getInt(1);
			}

			rs.close();
			stmnt.close();
		}
		catch (SQLException e)
		{
			System.err.println("Problems getSpid(). sql="+sql);
			e.printStackTrace();
		}
		
		return spid;
	}

	public static String getsrvVersion(Connection conn)
	{
		String version = "";
		String sql = "select @@version";

		try
		{
			Statement stmnt = conn.createStatement();

			ResultSet rs = stmnt.executeQuery(sql);
			while (rs.next())
			{
				version = rs.getString(1);
			}

			rs.close();
			stmnt.close();
		}
		catch (SQLException e)
		{
			System.err.println("Problems getsrvVersion(). sql="+sql);
			e.printStackTrace();
		}
		
		return version;
	}



/*
c: 
cd C:\projects\asetune
java -cp classes;lib/jconn3.jar com.asetune.test.JdbcPreparedStatementsTest ston60238837a 15702 sa sybase
 */
	public static void main(String[] args)
	{
		System.out.println("Usage: hostname port user passwd [full-jdbc-url]");
		String host = "localhost";
		String port = "5000";
		String user = "sa";
		String pawd = "";
		String fUrl = "";
		
		if (args.length > 0) host = args[0];
		if (args.length > 1) port = args[1];
		if (args.length > 2) user = args[2];
		if (args.length > 3) pawd = args[3];
		if (args.length > 4) pawd = args[4];

		System.out.println("host = '"+host+"'");
		System.out.println("port = '"+port+"'");
		System.out.println("user = '"+user+"'");
		System.out.println("pawd = '"+pawd+"'");
		System.out.println("fUrl = '"+fUrl+"'");
		
//		String jdbcDriver    = "com.sybase.jdbc4.jdbc.SybDriver";
		String jdbcUrl       = "jdbc:sybase:Tds:"+host+":"+port;
		String jdbcUser      = user;
		String jdbcPasswd    = pawd;
		Properties jdbcProps = null;
		
		if ( ! "".equals(fUrl) )
			jdbcUrl = fUrl;

		System.out.println("Using URL '"+jdbcUrl+"' when connectiong to DBMS.");

		try
		{
//			Class.forName(jdbcDriver);

			Properties props = new Properties(jdbcProps);
			if (props.getProperty("user")            == null) props.put("user",            jdbcUser);
			if (props.getProperty("password")        == null) props.put("password",        jdbcPasswd);
//			if (props.getProperty("APPLICATIONNAME") == null) props.put("APPLICATIONNAME", _appname);
//			if (props.getProperty("HOSTNAME")        == null) props.put("HOSTNAME",        hostname);
			
			Connection conn = null;
			conn = DriverManager.getConnection(jdbcUrl, props);

			DatabaseMetaData dbmd = conn.getMetaData();
			if (dbmd != null)
			{
				System.out.println("JDBC driver version:  " + dbmd.getDriverVersion());
				System.out.println("DBMS Product Name:    " + dbmd.getDatabaseProductName());
				System.out.println("DBMS Product Version: " + dbmd.getDatabaseProductVersion());
			}
			
			if (jdbcUrl.startsWith("jdbc:sybase:Tds"))
			{
				System.out.println("Sybase SPID:      "+getSpid(conn));
				System.out.println("Sybase @@version: "+getsrvVersion(conn));
			}

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
