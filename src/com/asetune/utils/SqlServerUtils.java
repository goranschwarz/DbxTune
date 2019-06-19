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
package com.asetune.utils;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

import org.apache.log4j.Logger;

import com.asetune.sql.conn.DbxConnection;

public class SqlServerUtils
{
	private static Logger _logger = Logger.getLogger(SqlServerUtils.class);


	/**
	 * Get XML Showplan 
	 * <p>
	 * Do NOT thow any exception, instead log error and return null
	 * 
	 * @param conn                   A Connection
	 * @param planHandleHexStr       SQL-Server plan handle, example: <code>0x060001007e490b0050a8abd50700000001000000000000000000000000000000000000000000000000000000</code>
	 * @return null on error, else String with the plan: <code>&lt;ShowPlanXML xmlns="http://schemas.microsoft.com/sqlserver/2004/07/showplan" ... </code>
	 */
	public static String getXmlQueryPlanNoThrow(DbxConnection conn, String planHandleHexStr)
	{
		try
		{
			return getXmlQueryPlan(conn, planHandleHexStr);
		}
		catch(SQLException e)
		{
			String msg = "Problems getting text from sys.dm_exec_query_plan, about '"+planHandleHexStr+"'. Msg="+e.getErrorCode()+", Text='" + e.getMessage() + "'. Caught: "+e;
			_logger.warn(msg); 

			return null;
		}
	}

	/**
	 * Get XML Showplan 
	 * @param conn                   A Connection
	 * @param planHandleHexStr       SQL-Server plan handle, example: <code>0x060001007e490b0050a8abd50700000001000000000000000000000000000000000000000000000000000000</code>
	 * @return String with the plan: <code>&lt;ShowPlanXML xmlns="http://schemas.microsoft.com/sqlserver/2004/07/showplan" ... </code>
	 * @throws SQLException
	 */
	public static String getXmlQueryPlan(DbxConnection conn, String planHandleHexStr)
	throws SQLException
	{
		//String sql = "select * from sys.dm_exec_query_plan("+planHandleHexStr+") \n";

		// convert(varbinary(64), '0x...', 1) in SQL-Server 2008, convert with style 1 (last param) is supported
		String sql = "select * from sys.dm_exec_query_plan( convert(varbinary(64), ?, 1) ) \n";

		// RS> Col# Label      JDBC Type Name              Guessed DBMS type Source Table
		// RS> ---- ---------- --------------------------- ----------------- ------------
		// RS> 1    dbid       java.sql.Types.SMALLINT     smallint          -none-      
		// RS> 2    objectid   java.sql.Types.INTEGER      int               -none-      
		// RS> 3    number     java.sql.Types.SMALLINT     smallint          -none-      
		// RS> 4    encrypted  java.sql.Types.BIT          bit               -none-      
		// RS> 5    query_plan java.sql.Types.LONGNVARCHAR xml               -none-      

		String str = null;
		try ( PreparedStatement stmnt = conn.prepareStatement(sql) )
		{
			stmnt.setQueryTimeout(10);
			stmnt.setString(1, planHandleHexStr);
			try ( ResultSet rs = stmnt.executeQuery() )
			{
				while (rs.next())
				{
					str = rs.getString(5);
				}
			}
		}
		return str;
	}


	/**
	 * Get SQL-Text for a SQL-Server "sql_handle"
	 *  
	 * @param conn                   A Connection
	 * @param sqlHandleHexStr        SQL-Server sql_handle, example: <code>0x02000000e3e9cb00e2359cd6a3450ed834ba421db1c25e6e0000000000000000000000000000000000000000</code>
	 * 
	 * @return null on errors, else: String with the SQL Text
	 */
	public static String getSqlTextNoThrow(DbxConnection conn, String sqlHandleHexStr)
	{
		try
		{
			return getSqlText(conn, sqlHandleHexStr);
		}
		catch(SQLException e)
		{
			String msg = "Problems getting text from sys.dm_exec_sql_text, about '"+sqlHandleHexStr+"'. Msg="+e.getErrorCode()+", Text='" + e.getMessage() + "'. Caught: "+e;
			_logger.warn(msg); 

			return null;
		}
	}
	
	/**
	 * Get SQL-Text for a SQL-Server "sql_handle"
	 *  
	 * @param conn                   A Connection
	 * @param sqlHandleHexStr        SQL-Server sql_handle, example: <code>0x02000000e3e9cb00e2359cd6a3450ed834ba421db1c25e6e0000000000000000000000000000000000000000</code>
	 * 
	 * @return String with the SQL Text
	 * @throws SQLException
	 */
	public static String getSqlText(DbxConnection conn, String sqlHandleHexStr)
	throws SQLException
	{
//		String sql = "select text from sys.dm_exec_sql_text("+sqlHandleHexStr+")";

		// convert(varbinary(64), '0x...', 1) in SQL-Server 2008, convert with style 1 (last param) is supported
		String sql = "select text from sys.dm_exec_sql_text( convert(varbinary(64), ?, 1) ) \n";

		String str = null;
		try ( PreparedStatement stmnt = conn.prepareStatement(sql) )
		{
			stmnt.setQueryTimeout(10);
			stmnt.setString(1, sqlHandleHexStr);
			try ( ResultSet rs = stmnt.executeQuery() )
			{
				while (rs.next())
				{
					str = rs.getString(1);
				}
			}
		}
		return str;
	}
}
