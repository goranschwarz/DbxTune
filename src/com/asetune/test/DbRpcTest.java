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

import java.sql.CallableStatement;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Types;
import java.util.Properties;

import org.apache.log4j.PropertyConfigurator;

import com.asetune.utils.AseConnectionFactory;

public class DbRpcTest
{
	public static void main(String[] args)
	{
		Properties log4jProps = new Properties();
		//log4jProps.setProperty("log4j.rootLogger", "INFO, A1");
		log4jProps.setProperty("log4j.rootLogger", "INFO, A1");
		log4jProps.setProperty("log4j.appender.A1", "org.apache.log4j.ConsoleAppender");
		log4jProps.setProperty("log4j.appender.A1.layout", "org.apache.log4j.PatternLayout");
		log4jProps.setProperty("log4j.appender.A1.layout.ConversionPattern", "%d - %-5p - %-30c{1} - %m%n");
		PropertyConfigurator.configure(log4jProps);

//		Configuration conf1 = new Configuration("c:\\projects\\asetune\\asetune.save.properties");
//		Configuration.setInstance(Configuration.USER_TEMP, conf1);
//
//		Configuration conf2 = new Configuration("c:\\projects\\asetune\\dbxtune.properties");
//		Configuration.setInstance(Configuration.SYSTEM_CONF, conf2);


		// DO THE THING
		try
		{
			System.out.println("Open DB connection.");
			Connection conn = AseConnectionFactory.getConnection("localhost", 5000, null, "sa", "", "DbRpcTest", null, null);

			String sql = "exec master..sp_help 'dbo.monLocks' ";
			System.out.println("DO SQL: "+sql);

//			AseSqlScript ss = new AseSqlScript(conn, 10);
//			try	{ 
//				System.out.println("NORMAL:" + ss.executeSqlStr(sql, true) ); 
//			} catch (SQLException e) { 
//				System.out.println("EXCEPTION:" + ss.executeSqlStr(sql, true) ); 
//				e.printStackTrace();
//			} finally {
//				ss.close();
//			}
			
			try
			{
				Statement stmt1 = conn.createStatement();
				stmt1.executeUpdate("use tempdb");

//				CallableStatement stmt = conn.prepareCall("{?=call sp_who}");
//				CallableStatement stmt = conn.prepareCall("{?=call tempdb..p1 2}");
//				CallableStatement stmt = conn.prepareCall("{?=call p1 2}");
				CallableStatement stmt = conn.prepareCall("{?=call p1(?)}");

				int intParam = 3;
	            stmt.setInt(2, intParam);

	            stmt.registerOutParameter(1, Types.INTEGER);

				ResultSet rs = stmt.executeQuery();
				int row=0;
				while(rs.next())
				{
					row++;
					System.out.println("reading result set, row "+row);
				}
				rs.close();
	            int returnStat = stmt.getInt(1);
	            System.out.println("PROC RC="+returnStat);

			}
			catch (SQLException e)
			{
				e.printStackTrace();
			}
		}
		catch (Exception e)
		{
			e.printStackTrace();
		}
		
	}
}
