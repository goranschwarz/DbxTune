// NOTE: Remove the below package declaration if you want to compile it in current directory 
package com.asetune.test;

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
			System.out.println("example: java -cp ngdbc.jar com.asetune.test.JdbcNoData SYSTEM secret jdbc:sap://hanags1:30015 \"select * from t1\"");
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
