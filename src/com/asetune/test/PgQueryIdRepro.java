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
package com.asetune.test;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Properties;

public class PgQueryIdRepro 
{
	public static void main(String[] args) throws Exception
	{
		String url = "jdbc:postgresql://pg-3a-cos9:5432/postgres"; // Postgres 16.2

		Properties props = new Properties();
		props.setProperty("user"    , "postgres");
		props.setProperty("password", "pg-3a-cos9__postgres__CYtXQHYi8dzHjVCtgNIqikGr");
		
//		props.setProperty("preferQueryMode", "simple");

		try (Connection conn = DriverManager.getConnection(url, props))
		{
			// Then get records from pg_stat_activity
			try (Statement statement = conn.createStatement())
			{
				try (ResultSet rs = statement.executeQuery("select query_id, pid, query, version() from pg_stat_activity where pid = pg_backend_pid()"))
				{
					int colCount = rs.getMetaData().getColumnCount();
					int row = 0;

					while( rs.next() )
					{
						row++;
						String queryId = rs.getString(1);
						
						if (queryId == null)
						{
							System.out.println(">>>> ERROR: row[" + row + "], column 'query_id', shouldn't be NULL, it has a value if the statement was initiated from 'psql'...");
						}

						for (int c = 1; c <= colCount; c++)
						{
							System.out.println("PG-STAT-ACTIVITY: row[" + row + "], col[" + c + "] = |" + rs.getString(c) + "|");
						}
					}
				}
			}
		}
	}

	public static void main_xxx(String[] args) throws Exception
	{
		String url = "jdbc:postgresql://pg-3a-cos9:5432/postgres"; // Postgres 16.2

		Properties props = new Properties();
		props.setProperty("user"    , "dbxtune");
		props.setProperty("password", "pg-3a-cos9__dbxtune__sezn2UuzfUAiI8MSxuOgIdim");

//		String url = "jdbc:postgresql://gorans-ub3.home:5432/gorans"; // Postgres 12.17
//
//		Properties props = new Properties();
//		props.setProperty("user"    , "gorans");
//		props.setProperty("password", "1niss2e");
		
			
		// Kick of a 'pg_sleep'
		PgSleep pgSleep = new PgSleep(3, url, props);
		pgSleep.start();

		try (Connection conn = DriverManager.getConnection(url, props))
		{
			// Then get records from pg_stat_activity
			try (Statement statement = conn.createStatement())
			{
				try (ResultSet rs = statement.executeQuery("select query_id, * from pg_stat_activity where query like '%pg_sleep%'"))
				{
					int colCount = rs.getMetaData().getColumnCount();
					int row = 0;

					while( rs.next() )
					{
						row++;
						int     queryId       = rs.getInt(1);
						boolean queryIdIsNull = rs.wasNull();
						
						if (queryIdIsNull)
						{
							System.out.println(">>>> ERROR: row[" + row + "], column 'query_id', shouldn't be NULL, it has a value if the statement was initiated from 'psql'...");
						}

						for (int c = 1; c <= colCount; c++)
						{
							System.out.println("PG-STAT-ACTIVITY: row[" + row + "], col[" + c + "] = |" + rs.getString(c) + "|");
						}
					}
				}
			}
		}
	}
	
	
	private static class PgSleep
	extends Thread
	{
		private int        _sleepTime;
		private String     _url;
		private Properties _props;

		public PgSleep(int sleepTime, String url, Properties props)
		{
			_sleepTime = sleepTime;
			_url       = url;
			_props     = props;
		}

		@Override
		public void run()
		{
			System.out.println("Starting PgSleep...");
			try
			{
				try (Connection conn = DriverManager.getConnection(_url, _props))
				{
					try (Statement statement = conn.createStatement())
					{
						try (ResultSet rs = statement.executeQuery("select pg_sleep(" + _sleepTime + ")"))
						{
							int colCount = rs.getMetaData().getColumnCount();
							int row = 0;

							while( rs.next() )
							{
								row++;
								for (int c = 1; c <= colCount; c++)
								{
									System.out.println("PG-SLEEP-RS: row[" + row + "], col[" + c + "] = |" + rs.getString(c) + "|");
								}
							}
						}
					}
				}
			}
			catch (SQLException ex)
			{
				ex.printStackTrace();
			}
		}
	}
}
