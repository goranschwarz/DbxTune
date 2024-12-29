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
package com.dbxtune.test;

import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Properties;
import java.util.logging.ConsoleHandler;
import java.util.logging.Level;
import java.util.logging.LogManager;
import java.util.logging.Logger;

public class JdbcCursorTest
{
	public static void srvWaitfor(Connection conn, String time)
	{
		System.out.println("");
		System.out.println("#### SRV WAIT -- " + time);

		String sql = "waitfor delay '" + time + "'";

		try (Statement stmnt = conn.createStatement())
		{
			stmnt.executeUpdate(sql);
		}
		catch (SQLException e)
		{
			System.err.println("Problems srvWaitfor(). sql="+sql);
			e.printStackTrace();
		}
	}

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

	public static void jconnCursorTest_noClose(Connection conn)
	{
		String sql = "select name from sysobjects";

		try
		{
			Statement stmnt = conn.createStatement();

			stmnt.setFetchSize(1); // Forces jConnect to use a Cursor instead of a normal TDS Stream
			
			ResultSet rs = stmnt.executeQuery(sql);
			int row = 0;
			while (rs.next())
			{
				row++;
				String name = rs.getString(1);
//				System.out.println("   [" + row + "] -- name=|" + name + "|.");
			}
			System.out.println("#### Read " + row + " rows from the ResultSet executed by SQL='" + sql + "'");

//			printLocks(conn, "before ResultSet.close()");
//			rs.close();
//			printLocks(conn, "before Statement.close()");
//			stmnt.close();
//			printLocks(conn, "After closed all");

			printLocks(conn, "Missed to close ResultSet and Statement");
		}
		catch (SQLException e)
		{
			System.err.println("Problems getsrvVersion(). sql="+sql);
			e.printStackTrace();
		}
		
		printMonSysSQLText(conn);
		
//		srvWaitfor(conn, "00:10:00");
	}

	public static void jconnCursorTest_withClose(Connection conn)
	{
		String sql = "select name from sysobjects";

		try
		{
			Statement stmnt = conn.createStatement();

			stmnt.setFetchSize(1); // Forces jConnect to use a Cursor instead of a normal TDS Stream
			
			ResultSet rs = stmnt.executeQuery(sql);
			int row = 0;
			while (rs.next())
			{
				row++;
				String name = rs.getString(1);
//				System.out.println("   [" + row + "] -- name=|" + name + "|.");
			}
			System.out.println("#### Read " + row + " rows from the ResultSet executed by SQL='" + sql + "'");

			printLocks(conn, "before ResultSet.close()");
			rs.close();
			printLocks(conn, "before Statement.close()");
			stmnt.close();
			printLocks(conn, "After closed all");
		}
		catch (SQLException e)
		{
			System.err.println("Problems getsrvVersion(). sql="+sql);
			e.printStackTrace();
		}
		
		printMonSysSQLText(conn);
		
//		srvWaitfor(conn, "00:10:00");
	}

	public static void printLocks(Connection conn, String msg)
	{
		System.out.println("");
		System.out.println("#### Print locks -- " + msg);

		String sql = ""
			    + "select"
			    + "  fid"
			    + " ,spid"
			    + " ,locktype = v1.name"
			    + " ,table_name = convert(varchar(30),object_name(id,dbid))"
			    + " ,page"
			    + " ,row"
			    + " ,dbname = db_name(dbid)"
			    + " ,class"
			    + " ,context = v2.name "
			    + "from master..syslocks l, master..spt_values v1, master..spt_values v2 "
			    + "where l.type = v1.number "
			    + "  and v1.type = 'L' "
			    + "  and (l.context+2049) = v2.number "
			    + "  and v2.type = 'L2' "
			    + "order by fid, spid, dbname, table_name, locktype, page, row "
			    + "";

		try (Statement stmnt = conn.createStatement(); ResultSet rs = stmnt.executeQuery(sql))
		{
			int row = 0;
			while (rs.next())
			{
				row++;
				String rs_fid        = rs.getString(1);
				String rs_spid       = rs.getString(2);
				String rs_locktype   = rs.getString(3);
				String rs_table_name = rs.getString(4);
				String rs_page       = rs.getString(5);
				String rs_row        = rs.getString(6);
				String rs_dbname     = rs.getString(7);
				String rs_class      = rs.getString(8);

				System.out.println("   [" + row +"] "
						+ "fid="          + rs_fid 
						+ ", spid="       + rs_spid 
						+ ", locktype="   + rs_locktype 
						+ ", table_name=" + rs_table_name
						+ ", page="       + rs_page
						+ ", row="        + rs_row
						+ ", dbname="     + rs_dbname
						+ ", class="      + rs_class
						);
			}
		}
		catch (Exception ex)
		{
			ex.printStackTrace();
		}
	}
	
	public static void printMonSysSQLText(Connection conn)
	{
		System.out.println("");
		System.out.println("#### Print monSysSQLText");

		String sql = "select SPID, BatchID, SequenceInBatch, SQLText from monSysSQLText where SPID = @@spid";
		try (Statement stmnt = conn.createStatement(); ResultSet rs = stmnt.executeQuery(sql))
		{
			int row = 0;
			while (rs.next())
			{
				row++;
				String SPID            = rs.getString(1);
				String BatchID         = rs.getString(2);
				String SequenceInBatch = rs.getString(3);
				String SQLText         = rs.getString(4);

				System.out.println("   [" + row +"] SPID=" + SPID + ", BatchID=" + BatchID + ", seq=" + SequenceInBatch + ", SQLText=|" + SQLText + "|.");
			}
		}
		catch (Exception ex)
		{
			ex.printStackTrace();
		}
	}

/*
c: 
cd C:\projects\dbxtune
java -cp classes;lib/jconn4.jar com.dbxtune.test.JdbcCursorTest gorans-ub3 1600 sa sybase 
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
		if (args.length > 4) fUrl = args[4];

		System.out.println("host = '"+host+"'");
		System.out.println("port = '"+port+"'");
		System.out.println("user = '"+user+"'");
		System.out.println("pawd = '"+pawd+"'");
		System.out.println("fUrl = '"+fUrl+"'");
		
//		String jdbcDriver    = "com.sybase.jdbc42.jdbc.SybDriver";
		String jdbcUrl       = "jdbc:sybase:Tds:"+host+":"+port;
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
			
			if (jdbcUrl.startsWith("jdbc:sybase:Tds") || jdbcUrl.startsWith("jdbc:sqlserver:"))
			{
				System.out.println("@@spid:    " + getSpid(conn));
				System.out.println("@@version: " + getsrvVersion(conn));

				System.out.println("");
				System.out.println(">>>>>> ==================================================");
				System.out.println(">>>>>> Executing WITH-RS-CLOSE *** GOOD Code ***");
				System.out.println(">>>>>> ==================================================");
				jconnCursorTest_withClose(conn);
				System.out.println(">>>>>> ===== END: Good Code =============================");

				
				System.out.println("");
				System.out.println(">>>>>> ==================================================");
				System.out.println(">>>>>> Executing NO-RS-CLOSE *** BAD CODE ***");
				System.out.println(">>>>>> ==================================================");
				jconnCursorTest_noClose(conn);
				System.out.println(">>>>>> ===== END: BAD CAODE =============================");

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
