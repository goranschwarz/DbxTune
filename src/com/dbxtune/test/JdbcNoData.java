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
import java.sql.Statement;
import java.util.Properties;

public class JdbcNoData
{
	public static void main(String[] args)
	{
		if (args.length != 4)
		{
			System.out.println();
			System.out.println("Usage: user passwd url \"select * from t1\"");
			System.out.println("       URL example: jdbc:sap://hostname:port");
			System.out.println();
			System.out.println("example: java -cp ngdbc.jar com.dbxtune.test.JdbcNoData SYSTEM secret jdbc:sap://hanags1:30015 \"select * from t1\"");
			System.out.println();
			System.exit(1);
		}
		String user   = args[0];
		String passwd = args[1];
		String url    = args[2];
		String sql    = args[3];
		
		if ("null".equalsIgnoreCase(passwd))
			passwd = "";

//		if ( ! url.startsWith("jdbc:sap://") )
//			url = "jdbc:sap://" + url;
		
		try
		{
//			Driver driverC = (Driver)Class.forName("com.sybase.jdbc.SybDriver").newInstance();
//			DriverManager.registerDriver(driverC);

			Properties props = new Properties();
			props.put("user",      user);
			props.put("password",  passwd);

			System.out.println("Connectiong to URL: "+url);
			Connection conn = DriverManager.getConnection(url, props);
			
			System.out.println("Using Java Runtime Environment Version: "+System.getProperty("java.version"));
			System.out.println("Using Driver Version: "+conn.getMetaData().getDriverVersion());
			System.out.println("DBMS Product: "+conn.getMetaData().getDatabaseProductName());
			System.out.println("DBMS Version: "+conn.getMetaData().getDatabaseProductVersion());

			
			/////////////////////////////////////////////////////////////
			try
			{
				System.out.println();
				System.out.println("###############################################################");
				System.out.println("SQL: "+sql);
				System.out.println("Data is NOT printed");
				System.out.println("###############################################################");
				
				Statement stmnt = conn.createStatement();

				long execStartTime = System.currentTimeMillis();
				
				System.out.println("executeQuery...");
				ResultSet rs = stmnt.executeQuery(sql);

				System.out.println("Reading ResultSet...");
				long rsStartTime = System.currentTimeMillis();
				int colCount = rs.getMetaData().getColumnCount();
				int rowCount = 0;
				while (rs.next())
				{
					rowCount++;
					for (int c=1; c<=colCount; c++)
					{
						String str = rs.getString(c);
						if (str == null)
							str="(NULL)";
						
						//System.out.println("COL["+c+"]=|"+str+"|.");
					}
				}
				rs.close();
				stmnt.close();
				
				long endTime = System.currentTimeMillis();
				
				System.out.println("-------------------------------------------------------------------");
				System.out.println("Total rows read:      " + rowCount);
				System.out.println("Total     Time in ms: " + (endTime     - execStartTime));
				System.out.println("Execution Time in ms: " + (rsStartTime - execStartTime));
				System.out.println("ResultSet Time in ms: " + (endTime     - rsStartTime));
				System.out.println("-------------------------------------------------------------------");
			}
			catch (Exception e)
			{
				e.printStackTrace();
			}

			
			System.out.println("Closing connection");
			conn.close();
		}
		catch (Exception e)
		{
			e.printStackTrace();
		}

		System.out.println("Exiting...");
		
	}
}
