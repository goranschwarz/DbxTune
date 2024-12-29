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
// NOTE: Remove the below package declaration if you want to compile it in current directory 
package com.dbxtune.test;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

public class JdbcDummyTest
{
	public static void main(String[] args)
	{
		if (args.length != 7)
		{
			System.out.println();
			System.out.println("Usage: user passwd url threads sqlCnt sleepMs \"select * from t1\"");
			System.out.println("       URL example: jdbc:vendor://hostname:port");
			System.out.println();
			System.out.println("example: java -cp jdbcdriver.jar com.dbxtune.test.JdbcDummyTest sa secret jdbc:sqlserver://prod-2a-mssql:1433 \"select * from t1\"");
			System.out.println();
			System.out.println("Below are some more examples (for Windows/DOS, On Unix/Linux/Mac replace ';' with ':' and '\\' with '/')");
			System.out.println(" SQL-Server - java -cp \"lib\\dbxtune.jar;lib\\jdbc_drivers\\mssql-jdbc-7.2.2.jre8.jar\"  com.dbxtune.test.JdbcDummyTest sa       \"secret\" \"jdbc:sqlserver://prod-2a-mssql:1433\"             20 10000 0 \"select getdate()\"                    ");
			System.out.println(" Oracle     - java -cp \"lib\\dbxtune.jar;lib\\jdbc_drivers\\ojdbc7.jar\"                 com.dbxtune.test.JdbcDummyTest system   \"secret\" \"jdbc:oracle:thin:@//gs-11-ora:1521/XE\"           20 10000 0 \"select CURRENT_TIMESTAMP from dual\"  ");
			System.out.println(" Postgres   - java -cp \"lib\\dbxtune.jar;lib\\jdbc_drivers\\postgresql-42.2.5.jre7.jar\" com.dbxtune.test.JdbcDummyTest postgres \"secret\" \"jdbc:postgresql://192.168.0.110:5432/postgres\"   20 10000 0 \"select now()\"                        ");
			System.out.println(" Sybase ASE - java -cp \"lib\\dbxtune.jar;lib\\jconn42.jar\"                              com.dbxtune.test.JdbcDummyTest sa       \"secret\" \"jdbc:sybase:Tds:192.168.0.110:1600\"              20 10000 0 \"select getdate()\"                    ");
			System.out.println();
			System.exit(1);

			// java -cp "classes;lib\jdbc_drivers\mssql-jdbc-7.2.2.jre8.jar"  com.dbxtune.test.JdbcDummyTest sa "dx7Nvk+XKH0ugrbB" "jdbc:sqlserver://prod-2a-mssql:1433"             20 10000 0 "select getdate()"
			// java -cp "classes;lib\jdbc_drivers\ojdbc7.jar"                 com.dbxtune.test.JdbcDummyTest system "sybase123"    "jdbc:oracle:thin:@//gs-11-ora:1521/XE"           20 10000 0 "select CURRENT_TIMESTAMP from dual"
			// java -cp "classes;lib\jdbc_drivers\postgresql-42.2.5.jre7.jar" com.dbxtune.test.JdbcDummyTest gorans "1niss2e"      "jdbc:postgresql://192.168.0.110:5432/postgres"   20 10000 0 "select now()"
			// java -cp "classes;lib\jconn4.jar"                              com.dbxtune.test.JdbcDummyTest sa     "sybase"       "jdbc:sybase:Tds:192.168.0.110:1600"              20 10000 0 "select getdate()"
		}
		
		CmdOptions cmdOptions = new CmdOptions();
		cmdOptions.username    = args[0];
		cmdOptions.password    = args[1];
		cmdOptions.url         = args[2];
		cmdOptions.threadCount = new Integer(args[3]);
		cmdOptions.sqlCount    = new Integer(args[4]);
		cmdOptions.sleepMs     = new Integer(args[5]);
		cmdOptions.sql         = args[6];

		if ("null".equalsIgnoreCase(cmdOptions.sql))
			cmdOptions.sql = "";

		System.out.println("-------------------------------------------------------------------");
		System.out.println(cmdOptions.toString());
		System.out.println("-------------------------------------------------------------------");
		System.out.println();
		
		List<DbClient> dbClients = new ArrayList<>();

		//------------------------------------------------------------
		// Create X number of clients
		// And connect to the DBMS
		System.out.println("Creating " + cmdOptions.threadCount + " DB Clients, and connectiong to the DBMS");
		try
		{
			for (int i=0; i<cmdOptions.threadCount; i++)
			{
				DbClient client = new DbClient(i, cmdOptions);
				client.connect();
				dbClients.add(client);
			}
		}
		catch(SQLException ex)
		{
			System.out.println("Problems connection to DBMS. Caught: " + ex);
			for (DbClient dbClient : dbClients)
			{
				dbClient.close();
			}

			System.out.println("EXITING...");
			System.exit(1);
		}

		//------------------------------------------------------------
		// Start all threads
		System.out.println();
		System.out.println("-------------------------------------------------------------------");
		long outerStartTime = System.currentTimeMillis();
		for (DbClient dbClient : dbClients)
		{
			dbClient.start();
		}
		
		//------------------------------------------------------------
		// Wait for all clients to END
		for (DbClient dbClient : dbClients)
		{
			try { dbClient.join(); }
			catch (InterruptedException ignore) {}
		}
		long outerExecTime = System.currentTimeMillis() - outerStartTime;
		System.out.println("-------------------------------------------------------------------");
		System.out.println();


		//------------------------------------------------------------
		// Close the DBMS connections
		int totalRowCount = 0;
		long minThExecTime  = Long.MAX_VALUE;
		long maxThExecTime  = Long.MIN_VALUE;
		long maxSqlExecTime = Long.MIN_VALUE;
		long sumSqlExecTime = 0;
		long avgSqlExecTime = 0;

		for (DbClient dbClient : dbClients)
		{
			totalRowCount += dbClient._rowCount;
			minThExecTime  = Math.min(minThExecTime,  dbClient._execTime);
			maxThExecTime  = Math.max(maxThExecTime,  dbClient._execTime);
			maxSqlExecTime = Math.max(maxSqlExecTime, dbClient._maxSqlExecTime);
			sumSqlExecTime += dbClient._execTime;

			dbClient.close();
		}
		avgSqlExecTime = (sumSqlExecTime - (cmdOptions.sleepMs * cmdOptions.threadCount)) / cmdOptions.threadCount / cmdOptions.sqlCount;

		//------------------------------------------------------------
		// Print some statistics
		System.out.println();
		System.out.println("-------------------------------------------------------------------");
		System.out.println("Total SQL cmds executed: " + cmdOptions.threadCount * cmdOptions.sqlCount);
		System.out.println("Total rows read:         " + totalRowCount);
		System.out.println("Total Time in ms:        " + outerExecTime);
		System.out.println("  Min worker Time in ms: " + minThExecTime);
		System.out.println("  Max worker Time in ms: " + maxThExecTime);
		System.out.println("  Max SQL    Time in ms: " + maxSqlExecTime);
		System.out.println("  Avg SQL    Time in ms: " + avgSqlExecTime);
		System.out.println("-------------------------------------------------------------------");
	}
	
	private static class CmdOptions
	{
		public String username;
		public String password;
		public String url;
		public String sql;

		public int    threadCount;
		public int    sqlCount;
		public int    sleepMs;
		
		@Override
		public String toString()
		{
			StringBuilder sb = new StringBuilder();
			sb.append("username    = ").append(username).append("\n");
			sb.append("password    = ").append(password).append("\n");
			sb.append("url         = ").append(url).append("\n");
			sb.append("sql         = ").append(sql).append("\n");
			sb.append("threadCount = ").append(threadCount).append("\n");
			sb.append("sqlCount    = ").append(sqlCount).append("\n");
			sb.append("sleepMs     = ").append(sleepMs).append("\n");

			return sb.toString();
		}
	}

	private static class DbClient
	extends Thread
	{
		private int        _workerId;
		private CmdOptions _cmdOptions;
		private Connection _conn;
		private long       _execTime = -1;
		private long       _maxSqlExecTime = 0;
		private int        _rowCount = 0;


		public DbClient(int workerId, CmdOptions cmdOptions)
		{
			_workerId   = workerId;
			_cmdOptions = cmdOptions;
			
			setName("DbWorker[" + _workerId + "]");
		}

		public void close()
		{
			try
			{
				if (_conn != null)
				{
					System.out.println(getName() + ": Closing connection for worker-.");
					_conn.close();
					_conn = null;
				}
			}
			catch (SQLException ignore) 
			{
				// do nothing
			}
		}
		
		public void connect()
		throws SQLException
		{
			Properties props = new Properties();
			props.put("user",      _cmdOptions.username);
			props.put("password",  _cmdOptions.password);

			if (_workerId == 0)
				System.out.println(getName() + ": Connecting to URL: " + _cmdOptions.url);

			_conn = DriverManager.getConnection(_cmdOptions.url, props);

			if (_workerId == 0)
			{
				System.out.println(getName() + ": Using Driver Version: " + _conn.getMetaData().getDriverVersion());
				System.out.println(getName() + ": DBMS Product: "         + _conn.getMetaData().getDatabaseProductName());
				System.out.println(getName() + ": DBMS Version: "         + _conn.getMetaData().getDatabaseProductVersion());
			}
		}

		@Override
		public void run()
		{
			System.out.println(getName() + ": Starting to work");
			long execStartTime = System.currentTimeMillis();
			
			for (int cnt=0; cnt<_cmdOptions.sqlCount; cnt++)
			{
				// Sleep if desired
				if (cnt > 0 && _cmdOptions.sleepMs > 0)
				{
					try { Thread.sleep(_cmdOptions.sleepMs); }
					catch (InterruptedException ignore) 
					{
						// If we are interrupted, leave the execution loop.
						break;
					}
				}

				// Execute
				long sqlExecStartTime = System.currentTimeMillis();
				try (Statement stmnt = _conn.createStatement(); ResultSet rs = stmnt.executeQuery(_cmdOptions.sql))
				{
					int colCount = rs.getMetaData().getColumnCount();

					while(rs.next())
					{
						_rowCount++;
						for (int c=1; c<=colCount; c++)
						{
							String str = rs.getString(c);
							if (str == null)
								str="(NULL)";
							
							//System.out.println(getName() + ": COL[" + c + "]=|" + str + "|.");
						}
					}
				}
				catch(SQLException ex)
				{
					System.out.println(getName() + ": Problems executing sql '" + _cmdOptions.sql + "', stopping this thread. Caught: " + ex);
					break;
				}
				_maxSqlExecTime = Math.max(_maxSqlExecTime, System.currentTimeMillis() - sqlExecStartTime);
			}
			_execTime = System.currentTimeMillis() - execStartTime;

			System.out.println(getName() + ": Ending...");
		}
	}
}
