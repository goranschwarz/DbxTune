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

public class JdbcMsSqlAgFailoverTest
{
	private Connection _conn;
	private int    _numOfExecutions; 
	private String _jdbcUrl;
	private String _jdbcUser;
	private String _jdbcPasswd;

	private int    _lastInsertedId = -1; 
	private String _currentServerName = ""; 
	
	private int    _sleepBetweenInserts  = 1;
	private int    _numOfInsertsPerBatch = 1000;
	private int    _sleepBetweenBatches  = 0;

	public JdbcMsSqlAgFailoverTest(int num, String jdbcUrl, String jdbcUser, String jdbcPasswd)
	{
		_numOfExecutions = num;
		_jdbcUrl    = jdbcUrl;
		_jdbcUser   = jdbcUser;
		_jdbcPasswd = jdbcPasswd;
	}
	
	private void start()
	{
		for (int cnt=0; cnt<_numOfExecutions; cnt++)
		{
			try
			{
				if (_conn == null)
					connect();

				if (_conn != null)
				{
					execAction(_sleepBetweenInserts, _numOfInsertsPerBatch);
				}
			}
			catch (SQLException ex)
			{
				System.out.println("Problem (closing conn and retry)... " + ex.getMessage());
				ex.printStackTrace();
				close();
			}
			catch (Exception ex)
			{
				System.out.println("EXITING... " + ex.getMessage());
				//ex.printStackTrace();
				close();
				return;
			}

			if (_sleepBetweenBatches > 0)
			{
				System.out.println("Sleeping for " + _sleepBetweenBatches + " ms after competing a 'insrt-batch'...");
				try { Thread.sleep(_sleepBetweenBatches); } catch(InterruptedException ignore) {}
			}
		}
	}

	private void execAction(int sleepTime, int count)
	throws Exception
	{
		// check if last inserted entry exists
		// add a new record
		
		System.out.print("Starting to insert " + count + " with " + sleepTime + " ms between inserts... ");

		long startTime = System.currentTimeMillis();
		
		for (int i=0; i<count; i++)
		{
			int lastId = getLastId();
			if (lastId != _lastInsertedId)
				throw new Exception("Expected ID=" + _lastInsertedId +", but last inserted id was: " + lastId);

			_lastInsertedId = insertNewId(_lastInsertedId + 1);
			
			if (sleepTime > 0)
				try { Thread.sleep(sleepTime); } catch(InterruptedException ignore) {}
		}

		long execTime = System.currentTimeMillis() - startTime;

		long rowsPerSec = count * 1000 / execTime; 
		System.out.println("DONE inserting " + count + " records (in " + execTime + " ms, " + rowsPerSec + " rowsPerSec, at='" + _currentServerName + "'). last ID = " + _lastInsertedId);
	}
	
	private int getLastId()
	throws SQLException
	{
		int id = -1;

		String sql = "select max(id) from dbo.test_ag_table";
		
		Statement stmnt = _conn.createStatement();
		ResultSet rs = stmnt.executeQuery(sql);
		while (rs.next())
			id = rs.getInt(1);
		rs.close();
		stmnt.close();

		return id;
	}

	private int insertNewId(int newId)
	throws SQLException
	{
		String sql = "insert into dbo.test_ag_table values(" + newId + ", getdate(), 'record: " + newId + "', 'method: insertNewId()', '')";
		
		Statement stmnt = _conn.createStatement();
		stmnt.executeUpdate(sql);
		stmnt.close();

		return newId;
	}

	private void connect()
	throws Exception
	{
		Properties props = new Properties();
		if (props.getProperty("user")            == null) props.put("user",            _jdbcUser);
		if (props.getProperty("password")        == null) props.put("password",        _jdbcPasswd);
		
		System.out.println("Using URL '"+_jdbcUrl+"' when connectiong to DBMS.");

		_conn = DriverManager.getConnection(_jdbcUrl, props);

		printSystemInfo();
		if (false)
			throw new Exception("Dummy: after connect(), before createTableIfNotExists()");

		createTableIfNotExists();
	}

	private void createTableIfNotExists()
	throws SQLException
	{
		String sql = 
			  "create table dbo.test_ag_table \n"
			+ "( \n"
			+ "     id      int          not null\n"
			+ "    ,ts      datetime     not null\n"
			+ "    ,c1      varchar(100) not null\n"
			+ "    ,c2      varchar(100) not null\n"
			+ "    ,c3      varchar(100) not null\n"
			+ "    ,primary key(id) \n"
			+ ") \n"
			+ "";

		boolean tabExists = false;
		{
			ResultSet rs = _conn.getMetaData().getTables(null, "dbo", "test_ag_table", null);
			while (rs.next())
				tabExists = true;
			rs.close();
		}
		
		
		if ( tabExists )
		{
			if (_lastInsertedId == -1)
			{
				int id = getLastId();
				if (id != -1)
				{
					_lastInsertedId = id;
					System.out.println("INITAIL STARTUP/Connection: Fetched MAX(id) from dbo.test_ag_table: " + id);
				}
			}
			else
			{
				System.out.println("This must be a *FAILED-OVER* connection, lets continue with ID: " + _lastInsertedId);
			}
		}
		else
		{
			Statement stmnt = _conn.createStatement();
			stmnt.executeUpdate(sql);
			
			System.out.println("CREATED TABLE: dbo.test_ag_table");
			
			stmnt.executeUpdate("insert into dbo.test_ag_table select 0, getdate(), 'first record after create table', '', '' ");
			_lastInsertedId = 0;
			
			System.out.println("Inserted record id=0 into: dbo.test_ag_table");
			
			stmnt.close();
		}
	}

	private void close()
	{
		if (_conn == null)
			return;

		try
		{
			System.out.println("Disconnecting from DBMS...");
			_conn.close();
			_conn = null;
		}
		catch (SQLException ex)
		{
			_conn = null;
			ex.printStackTrace();
		}
	}

	public void printSystemInfo()
	{
		String sql = "select @@servername, db_name(), @@version, @@spid";

		try
		{
			DatabaseMetaData dbmd = _conn.getMetaData();			

			Statement stmnt = _conn.createStatement();
			ResultSet rs = stmnt.executeQuery(sql);
			
			String servername = "";
			String dbname     = "";
			String version    = "";
			int    spid       = -1;

			while (rs.next())
			{
				servername = rs.getString(1);
				dbname     = rs.getString(2);
				version    = rs.getString(3);
				spid       = rs.getInt   (4);
			}

			_currentServerName = servername;

			System.out.println();
			System.out.println("====================================================");
			System.out.println("-- DBMS Info: ");
			System.out.println("----------------------------------------------------");
			System.out.println(" @@servername        : " + servername);
			System.out.println(" dbname              : " + dbname);
			System.out.println(" @@spid              : " + spid);
			System.out.println(" @@version           : " + version.replace('\n', ' '));
			System.out.println(" JDBC driver version : " + dbmd.getDriverVersion());
			System.out.println(" DBMS Product Name   : " + dbmd.getDatabaseProductName());
			System.out.println(" DBMS Product Version: " + dbmd.getDatabaseProductVersion());
			System.out.println("----------------------------------------------------");
			System.out.println();
			
			rs.close();
			stmnt.close();
		}
		catch (SQLException e)
		{
			System.err.println("Problems printSystemInfo(). sql="+sql);
			e.printStackTrace();
		}
	}


/*
c: 
cd C:\projects\asetune
java -cp classes;lib/jconn3.jar com.asetune.test.JdbcMsSqlAgTest ston60238837a 15702 sa sybase
 */
	public static void main(String[] args)
	{
		System.out.println("Usage: hostname port user passwd [dbname] [full-jdbc-url]");
		
//		String host = "prod-2a-mssql";
		String host = "prod-2v-mssql.home";
		String port = "1433";
//		String user = "gs1_app";
//		String pawd = "Sc6eK2JMSSO+Zuum";
		String user = "gs1_owner";
		String pawd = "ge10V8zGQY9Zg7im";
		String dbname = "gs1";
		String fUrl = "";
		
		if (args.length > 0) host   = args[0];
		if (args.length > 1) port   = args[1];
		if (args.length > 2) user   = args[2];
		if (args.length > 3) pawd   = args[3];
		if (args.length > 4) dbname = args[4];
		if (args.length > 5) fUrl   = args[5];

		System.out.println("host   = '"+host+"'");
		System.out.println("port   = '"+port+"'");
		System.out.println("user   = '"+user+"'");
		System.out.println("pawd   = '"+pawd+"'");
		System.out.println("dbname = '"+dbname+"'");
		System.out.println("fUrl   = '"+fUrl+"'");
		
//		String jdbcDriver    = "com.sybase.jdbc42.jdbc.SybDriver";
		String jdbcUrl       = "jdbc:sqlserver://" + host + ":" + port + (dbname==null?"":";databaseName="+dbname);
		String jdbcUser      = user;
		String jdbcPasswd    = pawd;
		String jdbcDbname    = dbname;
		Properties jdbcProps = null;
		
		if ( ! "".equals(fUrl) )
			jdbcUrl = fUrl;

		JdbcMsSqlAgFailoverTest t = new JdbcMsSqlAgFailoverTest(1000, jdbcUrl, jdbcUser, jdbcPasswd);
		t.start();
		
	}
}
