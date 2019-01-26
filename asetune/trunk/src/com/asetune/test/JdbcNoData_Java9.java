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
// NOTE: Remove the below package declaration if you want to compile it in current directory
package com.asetune.test;
// 2015-01-07 - moddet for ny test

import java.net.URL;
import java.security.CodeSource;
import java.security.ProtectionDomain;
import java.sql.Connection;
import java.sql.Driver;
import java.sql.DriverManager;
import java.sql.DriverPropertyInfo;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;
import java.util.Properties;

import com.asetune.utils.JdbcDriverHelper.DriverInfoEntry;

/*
java -cp .\classes;.\lib2\* com.asetune.test.JdbcNoData_Java9 sa Wq26kl73 jdbc:sybase:Tds:mig1-sybase:5000 "select * from sysobjects" N


java -cp .\classes;.\lib\jconn4.jar                                                                                                                                                                       com.asetune.test.JdbcNoData_Java9 sa Wq26kl73 jdbc:sybase:Tds:mig1-sybase:5000 "select * from sysobjects" N
java -cp .\classes;.\lib\jconn4.jar;.\lib\jdbc_drivers\sqljdbc41.jar;                                                                                                                                     com.asetune.test.JdbcNoData_Java9 sa Wq26kl73 jdbc:sybase:Tds:mig1-sybase:5000 "select * from sysobjects" N
java -cp .\classes;.\lib\jconn4.jar;.\lib\jdbc_drivers\sqljdbc41.jar;.\lib\jdbc_drivers\postgresql-9.4.1209.jre7.jar                                                                                      com.asetune.test.JdbcNoData_Java9 sa Wq26kl73 jdbc:sybase:Tds:mig1-sybase:5000 "select * from sysobjects" N
java -cp .\classes;.\lib\jconn4.jar;.\lib\jdbc_drivers\sqljdbc41.jar;.\lib\jdbc_drivers\postgresql-9.4.1209.jre7.jar;.\lib\jdbc_drivers\mysql-connector-java-5.1.44-bin.jar                               com.asetune.test.JdbcNoData_Java9 sa Wq26kl73 jdbc:sybase:Tds:mig1-sybase:5000 "select * from sysobjects" N
java -cp .\classes;.\lib\jconn4.jar;.\lib\jdbc_drivers\sqljdbc41.jar;.\lib\jdbc_drivers\postgresql-9.4.1209.jre7.jar;.\lib\jdbc_drivers\mysql-connector-java-5.1.44-bin.jar;.\lib\jdbc_drivers\ojdbc6.jar com.asetune.test.JdbcNoData_Java9 sa Wq26kl73 jdbc:sybase:Tds:mig1-sybase:5000 "select * from sysobjects" N

java -cp .\classes;.\lib2\jconn4.jar                                                                                                                                                              com.asetune.test.JdbcNoData_Java9 sa Wq26kl73 jdbc:sybase:Tds:mig1-sybase:5000 "select * from sysobjects" N
java -cp .\classes;.\lib2\jconn4.jar;.\lib2\jtds-1.3.1.jar                                                                                                                                        com.asetune.test.JdbcNoData_Java9 sa Wq26kl73 jdbc:sybase:Tds:mig1-sybase:5000 "select * from sysobjects" N
java -cp .\classes;.\lib2\jconn4.jar;.\lib2\jtds-1.3.1.jar;.\lib2\ngdbc.jar                                                                                                                       com.asetune.test.JdbcNoData_Java9 sa Wq26kl73 jdbc:sybase:Tds:mig1-sybase:5000 "select * from sysobjects" N
java -cp .\classes;.\lib2\jconn4.jar;.\lib2\jtds-1.3.1.jar;.\lib2\postgresql-9.4.1209.jre7.jar;.\lib2\sqljdbc41.jar                                                                               com.asetune.test.JdbcNoData_Java9 sa Wq26kl73 jdbc:sybase:Tds:mig1-sybase:5000 "select * from sysobjects" N
java -cp .\classes;.\lib2\jconn4.jar;.\lib2\jtds-1.3.1.jar;.\lib2\ngdbc.jar;.\lib2\postgresql-9.4.1209.jre7.jar;.\lib2\sqljdbc41.jar                                                              com.asetune.test.JdbcNoData_Java9 sa Wq26kl73 jdbc:sybase:Tds:mig1-sybase:5000 "select * from sysobjects" N
java -cp .\classes;.\lib2\jconn4.jar;.\lib2\jtds-1.3.1.jar;.\lib2\mysql-connector-java-5.1.44-bin.jar;.\lib2\ngdbc.jar;.\lib2\postgresql-9.4.1209.jre7.jar;.\lib2\sqljdbc41.jar                   com.asetune.test.JdbcNoData_Java9 sa Wq26kl73 jdbc:sybase:Tds:mig1-sybase:5000 "select * from sysobjects" N
java -cp .\classes;.\lib2\jconn4.jar;.\lib2\jtds-1.3.1.jar;.\lib2\mysql-connector-java-5.1.44-bin.jar;.\lib2\ngdbc.jar;.\lib2\ojdbc7.jar;.\lib2\postgresql-9.4.1209.jre7.jar;.\lib2\sqljdbc41.jar com.asetune.test.JdbcNoData_Java9 sa Wq26kl73 jdbc:sybase:Tds:mig1-sybase:5000 "select * from sysobjects" N

java -cp .\lib2\ojdbc6.jar;.\classes;.\lib2\jconn4.jar;.\lib2\jtds-1.3.1.jar;.\lib2\mysql-connector-java-5.1.44-bin.jar;.\lib2\postgresql-9.4.1209.jre7.jar;.\lib2\sqljdbc41.jar com.asetune.test.JdbcNoData_Java9 sa Wq26kl73 jdbc:sybase:Tds:mig1-sybase:5000 "select * from sysobjects" N


java -cp .\classes;.\lib\jconn4.jar;.\lib\jdbc_drivers\mysql-connector-java-5.1.44-bin.jar com.asetune.test.JdbcNoData_Java9 sa Wq26kl73 jdbc:sybase:Tds:mig1-sybase:5000 "select * from sysobjects" N
java -cp .\classes;.\lib\jconn4.jar;.\lib\jdbc_drivers\sqljdbc41.jar;.\lib\jdbc_drivers\postgresql-9.4.1209.jre7.jar .\lib\jdbc_drivers\mysql-connector-java-5.1.44-bin.jar com.asetune.test.JdbcNoData_Java9 sa Wq26kl73 jdbc:sybase:Tds:mig1-sybase:5000 "select * from sysobjects" N
java -cp .\classes;.\lib\jconn4.jar;.\lib\jdbc_drivers\sqljdbc41.jar;.\lib\jdbc_drivers\postgresql-9.4.1209.jre7.jar .\lib\jdbc_drivers\mysql-connector-java-5.1.44-bin.jar com.asetune.test.JdbcNoData_Java9 sa Wq26kl73 jdbc:sybase:Tds:mig1-sybase:5000 "select * from sysobjects" N
java -cp .\classes;.\lib\jconn4.jar;.\lib\jdbc_drivers\sqljdbc41.jar;.\lib\jdbc_drivers\postgresql-9.4.1209.jre7.jar .\lib\jdbc_drivers\mysql-connector-java-5.1.44-bin.jar com.asetune.test.JdbcNoData_Java9 sa Wq26kl73 jdbc:sybase:Tds:mig1-sybase:5000 "select * from sysobjects" N





29/08/2017  03:13           999,635 mysql-connector-java-5.1.44-bin.jar
31/07/2009  18:32         2,111,220 ojdbc6.jar
07/09/2016  09:53           685,271 postgresql-9.4.1209.jre7.jar
18/11/2015  00:36           586,192 sqljdbc41.jar

 Directory of C:\projects\AseTune\lib2
10/01/2017  11:00         1,873,815 jconn4.jar
08/06/2013  11:27           317,816 jtds-1.3.1.jar
29/08/2017  03:13           999,635 mysql-connector-java-5.1.44-bin.jar
25/06/2014  17:04           795,503 ngdbc.jar
31/07/2009  18:32         2,111,220 ojdbc6.jar
07/09/2016  09:53           685,271 postgresql-9.4.1209.jre7.jar
18/11/2015  00:36           586,192 sqljdbc41.jar





java -cp .\classes;.\lib2\jconn4.jar;.\lib2\jtds-1.3.1.jar;.\lib2\mysql-connector-java-5.1.44-bin.jar;.\lib2\ngdbc.jar;.\lib2\ojdbc7.jar;.\lib2\postgresql-9.4.1209.jre7.jar;.\lib2\sqljdbc41.jar com.asetune.test.JdbcNoData_Java9 sa Wq26kl73 jdbc:sybase:Tds:mig1-sybase:5000 "select * from sysobjects" N


## wildcard = FAIL
java -cp .\classes;.\lib2\* com.asetune.test.JdbcNoData_Java9 sa Wq26kl73 jdbc:sybase:Tds:mig1-sybase:5000 "select * from sysobjects" N

## all jars = FAIL
java -cp .\classes;.\lib2\jconn4.jar;.\lib2\jtds-1.3.1.jar;.\lib2\mysql-connector-java-5.1.44-bin.jar;.\lib2\ngdbc.jar;.\lib2\ojdbc7.jar;.\lib2\postgresql-9.4.1209.jre7.jar;.\lib2\sqljdbc41.jar com.asetune.test.JdbcNoData_Java9 sa Wq26kl73 jdbc:sybase:Tds:mig1-sybase:5000 "select * from sysobjects" N

## Oracle ONLY = OK
java -cp .\classes;.\lib2\ojdbc7.jar com.asetune.test.JdbcNoData_Java9 sa Wq26kl73 jdbc:sybase:Tds:mig1-sybase:5000 "select * from sysobjects" N

## all excpet Oracle = OK
java -cp .\classes;.\lib2\jconn4.jar;.\lib2\jtds-1.3.1.jar;.\lib2\mysql-connector-java-5.1.44-bin.jar;.\lib2\ngdbc.jar;.\lib2\postgresql-9.4.1209.jre7.jar;.\lib2\sqljdbc41.jar com.asetune.test.JdbcNoData_Java9 sa Wq26kl73 jdbc:sybase:Tds:mig1-sybase:5000 "select * from sysobjects" N

## MySQL and Oracle = OK
java -cp .\classes;.\lib2\mysql-connector-java-5.1.44-bin.jar;.\lib2\ojdbc7.jar; com.asetune.test.JdbcNoData_Java9 sa Wq26kl73 jdbc:sybase:Tds:mig1-sybase:5000 "select * from sysobjects" N

## MySQL, HANA, Oracle = OK
java -cp .\classes;.\lib2\mysql-connector-java-5.1.44-bin.jar;.\lib2\ngdbc.jar;.\lib2\ojdbc7.jar; com.asetune.test.JdbcNoData_Java9 sa Wq26kl73 jdbc:sybase:Tds:mig1-sybase:5000 "select * from sysobjects" N

## jConnect, MySQL, HANA, Oracle, Postgres, MS = FAIL
java -cp .\classes;.\lib2\jconn4.jar;.\lib2\mysql-connector-java-5.1.44-bin.jar;.\lib2\ngdbc.jar;.\lib2\ojdbc7.jar;.\lib2\postgresql-9.4.1209.jre7.jar;.\lib2\sqljdbc41.jar com.asetune.test.JdbcNoData_Java9 sa Wq26kl73 jdbc:sybase:Tds:mig1-sybase:5000 "select * from sysobjects" N

## jConnect, Oracle = FAIL
java -cp .\classes;.\lib2\jconn4.jar;.\lib2\ojdbc7.jar; com.asetune.test.JdbcNoData_Java9 sa Wq26kl73 jdbc:sybase:Tds:mig1-sybase:5000 "select * from sysobjects" N

## Oracle, jConnect = OK
java -cp .\classes;.\lib2\ojdbc7.jar;.\lib2\jconn4.jar; com.asetune.test.JdbcNoData_Java9 sa Wq26kl73 jdbc:sybase:Tds:mig1-sybase:5000 "select * from sysobjects" N

## ALL jars, but Oracle first = OK
java -cp .\classes;.\lib2\ojdbc7.jar;.\lib2\jconn4.jar;.\lib2\jtds-1.3.1.jar;.\lib2\mysql-connector-java-5.1.44-bin.jar;.\lib2\ngdbc.jar;.\lib2\postgresql-9.4.1209.jre7.jar;.\lib2\sqljdbc41.jar com.asetune.test.JdbcNoData_Java9 sa Wq26kl73 jdbc:sybase:Tds:mig1-sybase:5000 "select * from sysobjects" N

## ALL jars, but Oracle first = OK
java -cp .\classes;.\lib2\ojdbc7.jar;.\lib2\* com.asetune.test.JdbcNoData_Java9 sa Wq26kl73 jdbc:sybase:Tds:mig1-sybase:5000 "select * from sysobjects" N

## Wildcard jars, but Oracle first = OK
java -cp .\classes;.\lib2\ojdbc7.jar;.\lib2\* com.asetune.test.JdbcNoData_Java9 sa Wq26kl73 jdbc:sybase:Tds:mig1-sybase:5000 "select * from sysobjects" N

## Wildcard jars, ojdbc.jar->_ojdbc.jar = OK (but some exceptions)
java -cp .\classes;.\lib2\* com.asetune.test.JdbcNoData_Java9 sa Wq26kl73 jdbc:sybase:Tds:mig1-sybase:5000 "select * from sysobjects" N

## Wildcard jars, but '_' Oracle first = OK
java -cp .\classes;.\lib2\_ojdbc7.jar;.\lib2\* com.asetune.test.JdbcNoData_Java9 sa Wq26kl73 jdbc:sybase:Tds:mig1-sybase:5000 "select * from sysobjects" N

## Wildcard jars, ojdbc.jar->_ojdbc.jar and ojdbc.jar = FAIL
java -cp .\classes;.\lib2\* com.asetune.test.JdbcNoData_Java9 sa Wq26kl73 jdbc:sybase:Tds:mig1-sybase:5000 "select * from sysobjects" N


java -cp .\classes;
.\lib2\jconn4.jar;
.\lib2\jtds-1.3.1.jar;
.\lib2\mysql-connector-java-5.1.44-bin.jar;
.\lib2\ngdbc.jar;
.\lib2\ojdbc7.jar;
.\lib2\postgresql-9.4.1209.jre7.jar;
.\lib2\sqljdbc41.jar 
com.asetune.test.JdbcNoData_Java9 sa Wq26kl73 jdbc:sybase:Tds:mig1-sybase:5000 "select * from sysobjects" N

*/


public class JdbcNoData_Java9
{
	private static void printDrivers()
	{
		//-------------------------------------------------------
		// When they are Registered in the DriverManager
		// Get the info from there...
		for (Enumeration<Driver> drivers = DriverManager.getDrivers(); drivers.hasMoreElements();)
		{
			boolean inSystemPath   = true;
			Driver driver          = drivers.nextElement();
			String className       = driver.getClass().getName();
			String jarFile         = "<in system classpath>";
			String desc            = "";
			String homePage        = "";
			String version         = "";
			List<String> templates = new ArrayList<String>();

//			System.out.println("+ DriverManager-Entry: classname='"+className+"', driver='"+driver+"'.");

			// Try to get JAR file which a class exists in (even for the ones in the class path))
			ProtectionDomain protDom = driver.getClass().getProtectionDomain();
			if (protDom != null)
			{
				CodeSource src = protDom.getCodeSource();
//System.out.println("  DriverManager-Entry: CLASSLOADER='"+protDom.getClassLoader()+"', classname='"+className+"', driver='"+driver+"'.");

				if (src != null)
				{
					URL jar = src.getLocation();
					jarFile = jar + "";

					System.out.println("  DriverManager-Entry: classname='"+className+"', driver='"+driver+"' can be located in JAR File '"+jarFile+"'.");
				}
			}

			// Try to find out version of the driver in some way
			// The only way I can come up with is PropertyInfo and search for VERSION
			try
			{
				DriverPropertyInfo[] dpi = driver.getPropertyInfo("", null);
				if (dpi != null)
				{
					for (int i=0; i<dpi.length; i++)
					{
						String dpiName  = dpi[i].name;
						String dpiValue = dpi[i].value;

						if (dpiName != null && dpiName.toLowerCase().startsWith("version"))
						{
							version = dpiValue;
							if (version != null && version.indexOf('\n') >= 0)
								version = version.substring(0, version.indexOf('\n'));
						}
					}
				}
			}
			catch (Throwable ignore) {}

			//----------------------------------------------------
			// Add a DriverInfoEntry to the model
			DriverInfoEntry entry = new DriverInfoEntry();

			entry.setInSystemPath(inSystemPath);
			entry.setClassName(className);
			entry.setDescription(desc);
			entry.setHomePage(homePage);
			entry.setVersion(version);
			entry.setJarFile(jarFile);
			entry.setToString(driver.toString());
			entry.setUrlTemplateList(templates);

//			System.out.println("entry: "+entry);
		}
	}

	public static void main(String[] args)
	{
		if (args.length != 5)
		{
			System.out.println();
			System.out.println("Usage: user passwd url \"select * from t1\" option[O|S|N]");
			System.out.println("       URL example: jdbc:sap://hostname:port");
			System.out.println();
			System.out.println("example: java -cp ... com.asetune.test.JdbcNoData_java9 sa secret jdbc:sybase:Tds:mig1-sybase:5000 \"select * from sysobjects\" N");
			System.out.println();
			System.exit(1);
		}
		String user   = args[0];
		String passwd = args[1];
		String url    = args[2];
		String sql    = args[3];

		String option = args[4]; // BK - valid paramteres are S(tring), O(bject), N(one)

		if ("null".equalsIgnoreCase(passwd))
			passwd = "";

		System.out.println("");
		System.out.println( "EVRY version");
		System.out.println( "------------" );
		String oText = "";
		if ( "o".equalsIgnoreCase(option) )
			oText = "Fetch Objects";
		else if ( "s".equalsIgnoreCase(option) )
			oText = "Fetch Strings";
		else
			oText = "Just counter";


		System.out.println();
		System.out.println("###############################################################");
		printDrivers();
		System.out.println("###############################################################");
		System.out.println();

		try
		{
//			Driver driverC = (Driver)Class.forName("com.sybase.jdbc.SybDriver").newInstance();
//			DriverManager.registerDriver(driverC);

			Properties props = new Properties();
			props.put("user",      user);
			props.put("password",  passwd);

			String driverClassName = "com.sybase.jdbc4.jdbc.SybDriver";
			try
			{
				System.out.println("before: DriverManager.getDriver(url)... driverClassName='"+driverClassName+"', url='"+url+"')");
				Driver jdbcDriver = DriverManager.getDriver(url);
				System.out.println(" after: DriverManager.getDriver(url)... jdbcDriver='"+jdbcDriver+"', driverClassName='"+driverClassName+"', url='"+url+"')");
				
				if (jdbcDriver == null)
					Class.forName(driverClassName).newInstance();
			}
			catch (Throwable ex)
			{
				System.out.println( "Can't locate JDBC driver '"+driverClassName+"' for URL='"+url+"' using 'DriverManager.getDriver(url)' Lets continue, but first try to load the class '"+driverClassName+"' using 'Class.forName(driver).newInstance()' then connect to it using: DriverManager.getConnection(url, props); Caught="+ex);

				try { Class.forName(driverClassName).newInstance(); }
				catch( Throwable ex2 )
//				catch( ClassNotFoundException | InstantiationException | IllegalAccessException ex2 )
				{
					System.out.println("DriverManager.getDriver(url), threw Exception '"+ex+"', so we did 'Class.forName(driverClass).newInstance()', and that caused: "+ex2);
				}
			}
			
			System.out.println("Connectiong to URL: "+url);
			Connection conn = DriverManager.getConnection(url, props);

			System.out.println("Using Java Runtime Environment Version: "+ System.getProperty("java.version"));
			System.out.println("Using Driver Version: "                  + conn.getMetaData().getDriverVersion());
			System.out.println("DBMS Product: "                          + conn.getMetaData().getDatabaseProductName());
			System.out.println("DBMS Version: "                          + conn.getMetaData().getDatabaseProductVersion());


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
				System.out.println("Columns..." + colCount);
				System.out.println( "MODE: " + oText );

				// colCount = 4; // (bk) Modifying to fewer columns than actual
				int rowCount = 0; int jj=0;
				String str = "";
				Object obj;
				while (rs.next())
				{
					rowCount++;
					for (int c=1; c<=colCount; c++)
					{
						jj++;
						if ( "o".equalsIgnoreCase(option) )
							obj = rs.getObject(c);
						if ( "s".equalsIgnoreCase(option) )
							str = rs.getString(c);
						//if (str == null)
						//	str="(NULL)";

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
