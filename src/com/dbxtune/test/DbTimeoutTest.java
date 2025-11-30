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
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Properties;

import com.dbxtune.utils.AseConnectionUtils;

public class DbTimeoutTest
{
	public static void main(String[] args)
	{
		// Set Log4j Log Level
//		Configurator.setRootLevel(Level.TRACE);

//		Configuration conf1 = new Configuration("c:\\projects\\dbxtune\\asetune.save.properties");
//		Configuration.setInstance(Configuration.USER_TEMP, conf1);
//
//		Configuration conf2 = new Configuration("c:\\projects\\dbxtune\\dbxtune.properties");
//		Configuration.setInstance(Configuration.SYSTEM_CONF, conf2);


		// DO THE THING
		try
		{
			System.out.println("Open DB connection.");
//			Connection conn = AseConnectionFactory.getConnection("localhost", 5000, null, "sa", "", "DbTimeoutTest", null);

			Properties props = new Properties();
			props.setProperty("user",                      "sa");
			props.setProperty("password",                  "");
			props.setProperty("QUERY_TIMEOUT_CANCELS_ALL", "true");
//			props.setProperty("QUERY_TIMEOUT_CANCELS_ALL", "true");
//			props.setProperty("QUERY_TIMEOUT_CANCELS_ALL", "false");
//			Connection conn = AseConnectionFactory.getConnection("localhost:5000", null, "sa", "", "DbTimeoutTest", "dummy", props, null);

			String url = "jdbc:sybase:Tds:localhost:5000";
			Class.forName("com.sybase.jdbc42.jdbc.SybDriver");
			Connection conn = DriverManager.getConnection(url, props);

			int aseSpid = AseConnectionUtils.getAseSpid(conn);
			System.out.println("ASE SPID="+aseSpid);

			try
			{
//				String sql = "waitfor delay '00:01:00'";
				String sql = "select * from tempdb..t1";
				System.out.println("DO SQL: "+sql);

				Statement stmt = conn.createStatement();
//				stmt.executeUpdate("use tempdb");

//				CallableStatement stmt = conn.prepareCall("{?=call sp_who}");
//				CallableStatement stmt = conn.prepareCall("{?=call tempdb..p1 2}");
//				CallableStatement stmt = conn.prepareCall("{?=call p1 2}");
//				CallableStatement stmt = conn.prepareCall("{?=call p1(?)}");

				int timeout = 10;
				System.out.println("Setting setQueryTimeout("+timeout+")");
				stmt.setQueryTimeout(timeout);

				System.out.println("Executing SQL '"+sql+"'.");
				ResultSet rs = stmt.executeQuery(sql);
				int row=0;
				while(rs.next())
				{
					row++;
					System.out.println("reading result set, row "+row);
				}
				rs.close();
			}
			catch (SQLException e)
			{
				e.printStackTrace();
			}
			
			try
			{
//				String sql = "waitfor delay '00:01:00'";
				String sql = "select @@spid";
				System.out.println("DO SQL: "+sql);

				Statement stmt = conn.createStatement();

				int timeout = 10;
				System.out.println("Setting setQueryTimeout("+timeout+")");
				stmt.setQueryTimeout(timeout);

				System.out.println("Executing SQL '"+sql+"'.");
				ResultSet rs = stmt.executeQuery(sql);
				int row=0;
				while(rs.next())
				{
					row++;
					System.out.println("reading result set, row "+row+": spid="+rs.getString(1));
				}
				rs.close();
			}
			catch (SQLException e)
			{
				e.printStackTrace();
			}

			long sleepTime = 1*60*1000;
			System.out.println("SLEEPING to not terminate the login. sleep="+sleepTime);
			Thread.sleep(sleepTime);
		}
		catch (Exception e)
		{
			e.printStackTrace();
		}
		
		System.out.println("----ENDING----");
	}
}
