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

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.sql.Statement;

public class JdbcTest1
{
	public static void main(String[] args)
	{
		String url    = "jdbc:sybase:Tds:localhost:5000";
		String user   = "sa";
		String passwd = "sybase123";
		String inSql  = "select version = @@version";
		
		if (args.length >= 1) url    = args[0];
		if (args.length >= 2) user   = args[1];
		if (args.length >= 3) passwd = args[2];
		if (args.length >= 4) inSql  = args[3];

		if ("PROD".equalsIgnoreCase(url))
		{
			url    = "jdbc:sybase:Tds:pmldb.maxm.se:5000/PML";
			url    = "jdbc:sybase:Tds:pmldb.maxm.se:5000/PML?charset=iso_1&APPLICATIONNAME=JdbcTest1";
			user   = "pml_api";
			passwd = "pqvZnRseCBs3Q2v7"; // PROD

			inSql  = "select * from Foretag f WHERE f.raderas != 'j' AND (f.Foretag LIKE 'släp%')";
			inSql  = "file:c:\\tmp\\MAXM_PML_test1.sql";
		}
		
		if ("DEV".equalsIgnoreCase(url))
		{
			url    = "jdbc:sybase:Tds:dev-sybase.maxm.se:5000/PML";
			url    = "jdbc:sybase:Tds:dev-sybase.maxm.se:5000/PML?charset=iso_1&APPLICATIONNAME=JdbcTest1";
			user   = "pml_api";
			passwd = "TqJhKT2j56z7NdXc"; // DEV

			inSql  = "select * from Foretag f WHERE f.raderas != 'j' AND (f.Foretag LIKE 'släp%')";
			inSql  = "file:c:\\tmp\\MAXM_PML_test1.sql";
		}

		if (passwd != null && passwd.equalsIgnoreCase("null"))
			passwd = "";
		
		// If the SQL Starts with 'file:'... then read SQL Content from the file
		if (inSql.startsWith("file:"))
		{
			String filename = inSql.substring("file:".length());
			System.out.println("INFO: Reading SQL From File: " + filename);
			Path path = Paths.get(filename);
			try
			{
				inSql = Files.readString(path);
			}
			catch (IOException ex) 
			{
				System.out.println("ERROR: Problems reading file '" + filename + "'. Caught: " + ex);
				ex.printStackTrace();
				return;
			}
		}
		
		System.out.println("-----------------------------------------");
		System.out.println("Usage: url user passwd sqlStatement");
		System.out.println("-----------------------------------------");
		System.out.println("URL:    " + url);
		System.out.println("USER:   " + user);
		System.out.println("PASSWD: " + passwd);
		System.out.println("SQL:    " + inSql);
		System.out.println("-----------------------------------------");

		// DO THE THING
		try
		{
			System.out.println("Open DB connection url: " + url);
			Connection conn = DriverManager.getConnection(url, user, passwd);

			// get DB Version and print it
			System.out.println("-------------------------------------------------------------------------------");
			System.out.println("Sybase-Server-Name:        " + getAseServerName(conn) );
			System.out.println("getDatabaseProductName:    " + conn.getMetaData().getDatabaseProductName() );
			System.out.println("getDatabaseProductVersion: " + conn.getMetaData().getDatabaseProductVersion() );
			System.out.println("getDriverName:             " + conn.getMetaData().getDriverName());
			System.out.println("getDriverVersion:          " + conn.getMetaData().getDriverVersion());
			System.out.println("-------------------------------------------------------------------------------");
			
			String sql;

			sql = inSql;
			System.out.println("");
			System.out.println("############################################################################");
			System.out.println("EXECUTE: " + sql);

			try (Statement stmnt = conn.createStatement(); ResultSet rs = stmnt.executeQuery(sql))
			{
				printRs(rs);
			}
			catch (SQLException e)
			{
				System.out.println("=============================================");
				System.out.println("FAILED EXECUTE: " + sql);
				System.out.println("---------------------------------------------");
				e.printStackTrace();
			}

			// Also test it with a PREPARED Statement if that changes things... But it did NOT
//			boolean tryPreparedStatement = false;
//			if (tryPreparedStatement)
//			{
//				sql = "select * from Foretag f WHERE f.raderas != 'j' AND (f.Foretag LIKE ?)";
//				try (PreparedStatement pstmnt = conn.prepareStatement(sql))
//				{
//					pstmnt.setString(1, "släp%"); 
//					try( ResultSet rs = pstmnt.executeQuery() )
//					{
//						printRs(rs);
//					}
//				}
//				catch (SQLException e)
//				{
//					System.out.println("=============================================");
//					System.out.println("FAILED EXECUTE: " + sql);
//					System.out.println("---------------------------------------------");
//					e.printStackTrace();
//				}
//			}
		}
		catch (Exception e)
		{
			e.printStackTrace();
		}
	}
	
	private static void printRs(ResultSet rs)
	throws SQLException
	{
		ResultSetMetaData rsmd = rs.getMetaData();

		int row=0;
		while(rs.next())
		{
			row++;
//			System.out.println("reading result set, row "+row);
			for (int i = 0; i < rsmd.getColumnCount(); i++)
			{
				int c = i + 1;

				if (i == 0)
					System.out.println("------ row: " + row);
				
				System.out.println(String.format("[%d] - %-30.30s : %s", c, rsmd.getColumnLabel(c), rs.getString(c)));
			}
		}
		
	}

	private static String getAseServerName(Connection conn)
	{
		try (Statement stmnt = conn.createStatement(); ResultSet rs = stmnt.executeQuery("select @@servername"))
		{
			String srvname = "-unknown-";
			while(rs.next())
			{
				srvname = rs.getString(1);
			}
			return srvname;
		}
		catch (SQLException ex)
		{
			return "problems getting Sybase DBMS Name. Caught: " + ex; 
		}
	}
}
