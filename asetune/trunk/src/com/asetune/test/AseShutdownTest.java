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
import java.sql.SQLWarning;
import java.sql.Statement;
import java.sql.Timestamp;
import java.util.Properties;

public class AseShutdownTest
{
	private Connection _conn = null;

	/**
	 * Simply connect to dataserver
	 * @return
	 * @throws Exception
	 */
	public Connection connect(String jdbcDriver, String jdbcUrl, String user, String passwd, Properties jdbcProps )
	throws ClassNotFoundException, SQLException
	{
		Class.forName(jdbcDriver);

		Properties props = new Properties(jdbcProps);
		if (props.getProperty("user")            == null) props.put("user",            user);
		if (props.getProperty("password")        == null) props.put("password",        passwd);
//		if (props.getProperty("APPLICATIONNAME") == null) props.put("APPLICATIONNAME", _appname);
//		if (props.getProperty("HOSTNAME")        == null) props.put("HOSTNAME",        hostname);
		
		Connection conn = null;
		conn = DriverManager.getConnection(jdbcUrl, props);

		DatabaseMetaData dbmd = conn.getMetaData();
		if (dbmd != null)
		{
			System.out.println("JDBC driver version: " + dbmd.getDriverVersion());
		}

		// make strings unaware of quotation ""
		// meaning SQL statements like: print "some string" will work...
		try
		{
			if (conn.getAutoCommit() == false)
			{
				//_logger.info("Autocommit was turned 'off'. I will turn this to 'on' for this connection.");
				conn.setAutoCommit(true);
			}

			// If this is not set to 'off', things like (print "any string") wont work
			conn.createStatement().execute("set quoted_identifier off");
		}
		catch (SQLException sqle)
		{
			String errStr = "";
			while (sqle != null)
			{
				errStr += sqle.getMessage() + " ";
				sqle = sqle.getNextException();
			}
			System.out.println("Failed to execute 'set quoted_identifier off' when connecting. Problem: "+errStr);
		}

		_conn = conn;
		return conn;
	}

	private Connection getConnection()
	{
		return _conn;
	}

	public void close()
	{
		try
		{
			if (_conn != null)
				_conn.close();
		}
		catch(SQLException ignore) {}
		
		_conn = null;
	}

	public void doDummySql()
	{
		String sql = 
			"select crdate \n" +
			"from sysobjects \n" +
			"where name = 'sysobjects' \n";

		try
		{
			Connection conn = getConnection();
			Statement stmnt = conn.createStatement();

			Timestamp crdate = null;

			ResultSet rs = stmnt.executeQuery(sql);
			while (rs.next())
			{
				crdate = rs.getTimestamp(1);
			}
			rs.close();

			System.out.println("crdate='"+crdate+"'.");

			boolean aseInShutdown = false;
			SQLWarning w = stmnt.getWarnings();
			while(w != null)
			{
				// Msg=6002: A SHUTDOWN command is already in progress. Please log off.
				if (w.getErrorCode() == 6002)
				{
					System.out.println("======SQLWarning: Msg="+w.getErrorCode()+", str='"+w.getMessage()+"', sqlState="+w.getSQLState()+", class="+w.getClass().getName());
					aseInShutdown = true;
					break;
				}
				System.out.println("SQLWarning: Msg="+w.getErrorCode()+", str='"+w.getMessage()+"', sqlState="+w.getSQLState()+", class="+w.getClass().getName());
				
				w = w.getNextWarning();
			}
			
			stmnt.close();
		}
		catch (SQLException e)
		{
			System.err.println("Msg="+e.getErrorCode()+", sqlState="+e.getSQLState()+", class="+e.getClass().getName());
			e.printStackTrace();
		}
	}

	public static void main(String[] args)
	{
		System.out.println("Usage: hostname port user passwd");
		String host = "localhost";
		String port = "5000";
		String user = "sa";
		String pawd = "";
		
		if (args.length > 0) host = args[0];
		if (args.length > 1) port = args[1];
		if (args.length > 2) user = args[2];
		if (args.length > 3) pawd = args[3];

		System.out.println("host = '"+host+"'");
		System.out.println("port = '"+port+"'");
		System.out.println("user = '"+user+"'");
		System.out.println("pwd  = '"+pawd+"'");
		
		String jdbcDriver    = "com.sybase.jdbc4.jdbc.SybDriver";
		String jdbcUrl       = "jdbc:sybase:Tds:"+host+":"+port;
		String jdbcUser      = user;
		String jdbcPasswd    = pawd;
		Properties jdbcProps = null;

		try
		{
			AseShutdownTest xxx = new AseShutdownTest();
			xxx.connect(jdbcDriver, jdbcUrl, jdbcUser, jdbcPasswd, jdbcProps);
			
			xxx.doDummySql();
		}
		catch (Exception e)
		{
			e.printStackTrace();
		}
	}
}
