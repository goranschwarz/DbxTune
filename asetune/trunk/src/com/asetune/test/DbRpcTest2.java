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
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Types;

public class DbRpcTest2
{
	public static void main(String[] args)
	{
		String url    = "jdbc:sybase:Tds:localhost:15702";
		String user   = "sa";
		String passwd = "sybase";

		if (args.length >= 1) url    = args[0];
		if (args.length >= 2) user   = args[1];
		if (args.length >= 3) passwd = args[2];

		if (passwd != null && passwd.equalsIgnoreCase("null"))
			passwd = "";
		
		System.out.println("-----------------------------------------");
		System.out.println("Usage: url user passwd");
		System.out.println("-----------------------------------------");
		System.out.println("URL:    "+url);
		System.out.println("USER:   "+user);
		System.out.println("PASSWD: "+passwd);
		System.out.println("-----------------------------------------");

		// DO THE THING
		try
		{
			System.out.println("Open DB connection.");
			Connection conn = DriverManager.getConnection(url, user, passwd);

			try
			{
				// get DB Version and print it
				System.out.println("-------------------------------------------------------------------------------");
				System.out.println("getDatabaseProductName:    " + conn.getMetaData().getDatabaseProductName() );
				System.out.println("getDatabaseProductVersion: " + conn.getMetaData().getDatabaseProductVersion() );
				System.out.println("getDriverName:             " + conn.getMetaData().getDriverName());
				System.out.println("getDriverVersion:          " + conn.getMetaData().getDriverVersion());
				System.out.println("-------------------------------------------------------------------------------");
				
				String sql;
				Statement stmt = conn.createStatement();

				sql = "use tempdb";
				System.out.println("EXECUTE: "+sql);
				stmt.executeUpdate(sql);

				sql = "if ((select object_id('get_xxx')) is not null ) drop PROCEDURE get_xxx";
				System.out.println("EXECUTE: "+sql);
				stmt.executeUpdate(sql);

				System.out.println("EXECUTE: create the proc...");
				sql = "CREATE PROCEDURE get_xxx \n"
						+ "( \n"
						+ "    @p_propname  VARCHAR(75),         \n"
						+ "    @p_int_value INT         OUTPUT,  \n"
						+ "    @p_str_value VARCHAR(75) OUTPUT   \n"
						+ ") \n"
						+ "--WITH EXECUTE AS OWNER \n"
						+ "AS \n"
						+ "BEGIN \n"
						+ "    select getdate() as dummyResultSet, @p_propname as p_propname \n"
						+ "    set @p_int_value = 99 \n"
						+ "    set @p_str_value = convert(varchar(30), getdate(), 109) \n"
						+ "    return 0 \n"
						+ "END \n"
						+ "";
				stmt.executeUpdate(sql);

				System.out.println("##### Now do database calls");


				
				sql = "{?=call get_xxx('use_package_variables', ?, ?)}";
				System.out.println("");
				System.out.println("############################################################################");
				System.out.println("EXECUTE: "+sql);
				try
				{
					// Now prepare and execute the call
					CallableStatement cstmt = conn.prepareCall(sql);

					cstmt.registerOutParameter(1, Types.INTEGER);
					cstmt.registerOutParameter(2, Types.INTEGER);
					cstmt.registerOutParameter(3, Types.VARCHAR);

					ResultSet rs = cstmt.executeQuery();
					int row=0;
					while(rs.next())
					{
						row++;
						System.out.println("reading result set, row "+row);
					}
					rs.close();
//					cstmt.executeUpdate();

					int returnStat = cstmt.getInt(1);
					System.out.println("PROC RC="+returnStat);

					int p_int_value = cstmt.getInt(2);
					System.out.println("OUT: p_int_value="+p_int_value);

					String p_str_value = cstmt.getString(3);
					System.out.println("OUT: p_str_value="+p_str_value);
				}
				catch (SQLException e)
				{
					System.out.println("=============================================");
					System.out.println("FAILED EXECUTE: "+sql);
					System.out.println("---------------------------------------------");
					e.printStackTrace();

					Thread.sleep(50); // otherwise the stacktrace will be written strange 
				}

				
				
				sql = "{?=call get_xxx(?, ?, ?)}";
				System.out.println();
				System.out.println("############################################################################");
				System.out.println("EXECUTE: "+sql);
				try
				{
					// Now prepare and execute the call
					CallableStatement cstmt = conn.prepareCall(sql);

					cstmt.registerOutParameter(1, Types.INTEGER);
					cstmt.           setString(2, "use_package_variables");
					cstmt.registerOutParameter(3, Types.INTEGER);
					cstmt.registerOutParameter(4, Types.VARCHAR);

					ResultSet rs = cstmt.executeQuery();
					int row=0;
					while(rs.next())
					{
						row++;
						System.out.println("reading result set, row "+row);
					}
					rs.close();
//					cstmt.executeUpdate();

					int returnStat = cstmt.getInt(1);
					System.out.println("PROC RC="+returnStat);

					int p_int_value = cstmt.getInt(3);
					System.out.println("OUT: p_int_value="+p_int_value);

					String p_str_value = cstmt.getString(4);
					System.out.println("OUT: p_str_value="+p_str_value);
				}
				catch (SQLException e)
				{
					System.out.println("=============================================");
					System.out.println("FAILED EXECUTE: "+sql);
					System.out.println("---------------------------------------------");
					e.printStackTrace();
				}
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
