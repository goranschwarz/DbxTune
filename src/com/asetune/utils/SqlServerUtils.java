/*******************************************************************************
 * Copyright (C) 2010-2020 Goran Schwarz
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
import java.sql.Statement;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeoutException;

import org.apache.log4j.Logger;

import com.asetune.CounterController;
import com.asetune.ICounterController.DbmsOption;
import com.asetune.cm.ase.CmLockTimeout;
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

		String dm_exec_query_plan = "dm_exec_query_plan";
		
		// Check if we can use Actual-Query-Plan instead of Estimated-QueryPlan
		if (CounterController.hasInstance())
		{
			if (CounterController.getInstance().isDbmsOptionEnabled(DbmsOption.SQL_SERVER__LAST_QUERY_PLAN_STATS))
			{
				dm_exec_query_plan = "dm_exec_query_plan_stats";
			}
		}
		else if (conn.getDbmsVersionNumber() > Ver.ver(2019))
		{
			dm_exec_query_plan = "dm_exec_query_plan_stats";
		}


		// convert(varbinary(64), '0x...', 1) in SQL-Server 2008, convert with style 1 (last param) is supported
		String sql = "select * from sys." + dm_exec_query_plan + "( convert(varbinary(64), ?, 1) ) \n";

		// RS> Col# Label      JDBC Type Name              Guessed DBMS type Source Table
		// RS> ---- ---------- --------------------------- ----------------- ------------
		// RS> 1    dbid       java.sql.Types.SMALLINT     smallint          -none-       // ID of the context database that was in effect when the Transact-SQL statement corresponding to this plan was compiled. For ad hoc and prepared SQL statements, the ID of the database where the statements were compiled. Column is nullable.
		// RS> 2    objectid   java.sql.Types.INTEGER      int               -none-       // ID of the object (for example, stored procedure or user-defined function) for this query plan. For ad hoc and prepared batches, this column is null. Column is nullable.
		// RS> 3    number     java.sql.Types.SMALLINT     smallint          -none-       // Numbered stored procedure integer. For example, a group of procedures for the orders application may be named orderproc;1, orderproc;2, and so on. For ad hoc and prepared batches, this column is null. Column is nullable.
		// RS> 4    encrypted  java.sql.Types.BIT          bit               -none-       // Indicates whether the corresponding stored procedure is encrypted. 0 = not encrypted, 1 = encrypted.   Column is not nullable.
		// RS> 5    query_plan java.sql.Types.LONGNVARCHAR xml               -none-       // Contains the compile-time Showplan representation of the query execution plan that is specified with plan_handle. The Showplan is in XML format. One plan is generated for each batch that contains, for example ad hoc Transact-SQL statements, stored procedure calls, and user-defined function calls. Column is nullable.

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

	public static Timestamp getStartDate(DbxConnection conn)
	throws SQLException
	{
		String sql = "SELECT sqlserver_start_time FROM sys.dm_os_sys_info";

		Timestamp ts = null;
		try ( PreparedStatement stmnt = conn.prepareStatement(sql) )
		{
			stmnt.setQueryTimeout(10);
			try ( ResultSet rs = stmnt.executeQuery() )
			{
				while (rs.next())
				{
					ts = rs.getTimestamp(1);
				}
			}
		}
		return ts;
	}

	/**
	 * Get LIVE Query plan from SQL-Server
	 * 
	 * @param conn
	 * @param spid
	 * @return
	 */
	public static String getLiveQueryPlanNoThrow(DbxConnection conn, int spid)
	{
		try
		{
			return getLiveQueryPlan(conn, spid);
		}
		catch (Exception e)
		{
			_logger.info("Problems executing sql='select query_plan from sys.dm_exec_query_statistics_xml("+spid+")'. Caught: " + e);
			return null;
		}
	}

	/**
	 * Get LIVE Query plan from SQL-Server
	 * 
	 * @param conn
	 * @param spid
	 * @return
	 * @throws SQLException
	 */
	public static String getLiveQueryPlan(DbxConnection conn, int spid)
	throws SQLException
	{
		String sql = "select query_plan from sys.dm_exec_query_statistics_xml(?)";
		String str = "";
		
		try (PreparedStatement pstmnt = conn.prepareStatement(sql))
		{
			pstmnt.setInt(1, spid);
			try (ResultSet rs = pstmnt.executeQuery())
			{
				while(rs.next())
					str = str + rs.getString(1);
			}
		}
		
		return str;
	}
	
	/**
	 * Get global TraceFlags from SQL-Server
	 * 
	 * @param conn
	 * @return
	 * @throws SQLException
	 */
	public static List<Integer> getGlobalTraceFlags(DbxConnection conn)
	throws SQLException
	{
		List<Integer> list = new ArrayList<>();

		// Get GLOBAL TRACEFLAG's
		// Note: 'dbcc tracestatus(-1)' doesn't even return a ResultSet if there are NO trace flags set... hence the Table Variable
		String sql = ""
			    + "declare @tfInfo table(traceflag int, status int, global int, session int) \n"
			    + "INSERT INTO @tfInfo \n"
			    + "exec('dbcc tracestatus(-1) with no_infomsgs') \n"
			    + "SELECT * FROM @tfInfo \n"
			    + "";

		try (Statement stmnt = conn.createStatement(); ResultSet rs = stmnt.executeQuery(sql))
		{
			while(rs.next())
				list.add(rs.getInt(1));
		}

		return list;
	}

//	/**
//	 * Check what databases have option 'LAST_QUERY_PLAN_STATS' set or TRACEFLAG 2451 set
//	 * @param conn
//	 * @return
//	 * @throws SQLException
//	 */
//	public static List<String> getDatabasesWithLastActualQueryPlansCapability(DbxConnection conn)
//	throws SQLException
//	{
//		List<String> list = new ArrayList<>();
//
//		// Fist get database options 'LAST_QUERY_PLAN_STATS' from ALL databases
//		String sql = ""
//			    + "declare @dbOptions table(dbame varchar(128), dboption varchar(60), value int) \n"
//			    + "INSERT INTO @dbOptions \n"
//			    + "exec sys.sp_MSforeachdb 'select ''?'', name, cast(value as int) from ?.sys.database_scoped_configurations where name = ''LAST_QUERY_PLAN_STATS'' and value != 0' \n"
//			    + "SELECT * FROM @dbOptions \n"
//			    + "";
//
//		try (Statement stmnt = conn.createStatement(); ResultSet rs = stmnt.executeQuery(sql))
//		{
//			while(rs.next())
//				list.add(rs.getString(1));
//		}
//		
//		// Get trace flags
//		List<Integer> traceFlags = getGlobalTraceFlags(conn);
//		if (traceFlags.contains(2451))
//			list.add("TRACEFLAG:2451:ENABLED");
//		
//		return list;
//	}

	/**
	 * Get Database Scoped Configurations for ALL database
	 * 
	 * @param conn
	 * @return A Map&lt;DBName, Map&lt;OptionName, optionValue&gt;&gt;
	 * @throws SQLException
	 */
	public static Map<String, Map<String, Object>> getDatabasesScopedConfig(DbxConnection conn)
	throws SQLException
	{
		String sql = ""
			    + "declare @dbOptions table(dbame nvarchar(128), dboption nvarchar(60), value sql_variant) \n"
			    + "INSERT INTO @dbOptions \n"
			    + "exec sys.sp_MSforeachdb 'select ''?'', name, value from ?.sys.database_scoped_configurations' \n"
			    + "SELECT * FROM @dbOptions \n"
			    + "";

		return internal_getDatabasesScopedConfig(conn, sql);
	}

	/**
	 * Get Database Scoped Configurations for ALL database
	 * 
	 * @param conn
	 * @param optionName name of the configuration
	 * 
	 * @return A Map&lt;DBName, Map&lt;OptionName, optionValue&gt;&gt;
	 * @throws SQLException
	 */
	public static Map<String, Map<String, Object>> getDatabasesScopedConfig(DbxConnection conn, String optionName)
	throws SQLException
	{
		String sql = ""
			    + "declare @dbOptions table(dbame nvarchar(128), dboption nvarchar(60), value sql_variant) \n"
			    + "INSERT INTO @dbOptions \n"
			    + "exec sys.sp_MSforeachdb 'select ''?'', name, value from ?.sys.database_scoped_configurations where name = ''" + optionName + "''' \n"
			    + "SELECT * FROM @dbOptions \n"
			    + "";

		return internal_getDatabasesScopedConfig(conn, sql);
	}

	/**
	 * Get NON-Defaults for Database Scoped Configurations for ALL database
	 * 
	 * @param conn
	 * @param optionName name of the configuration
	 * 
	 * @return A Map&lt;DBName, Map&lt;OptionName, optionValue&gt;&gt;
	 * @throws SQLException
	 */
	public static Map<String, Map<String, Object>> getDatabasesScopedConfigNonDefaults(DbxConnection conn, String optionName)
	throws SQLException
	{
		String sql = ""
			    + "declare @dbOptions table(dbame nvarchar(128), dboption nvarchar(60), value sql_variant) \n"
			    + "INSERT INTO @dbOptions \n"
			    + "exec sys.sp_MSforeachdb 'select ''?'', name, value from ?.sys.database_scoped_configurations where name = ''" + optionName + "'' and is_value_default = 0' \n"
			    + "SELECT * FROM @dbOptions \n"
			    + "";

		return internal_getDatabasesScopedConfig(conn, sql);
	}

	/**
	 * Get NON-Defaults for Database Scoped Configurations for ALL database
	 * 
	 * @param conn
	 * @param optionName name of the configuration
	 * 
	 * @return A Map&lt;DBName, Map&lt;OptionName, optionValue&gt;&gt;
	 * @throws SQLException
	 */
	public static Map<String, Map<String, Object>> internal_getDatabasesScopedConfig(DbxConnection conn, String sql)
	throws SQLException
	{
		Map<String, Map<String, Object>> dbMap = new LinkedHashMap<>();

		try (Statement stmnt = conn.createStatement(); ResultSet rs = stmnt.executeQuery(sql))
		{
			while(rs.next())
			{
				String dbName   = rs.getString(1);
				String dbOption = rs.getString(2);
				Object optVal   = rs.getObject(3);

				// Get CFG Map for a database (if not there create it)
				Map<String, Object> cfgMap = dbMap.get(dbName);
				if (cfgMap == null)
				{
					cfgMap = new LinkedHashMap<>();
					dbMap.put(dbName, cfgMap);
				}

				// put the DB-OPTION in the Options Map
				cfgMap.put(dbOption, optVal);
			}
		}
		
		return dbMap;
	}


	/**
	 * Get a lock summary for a SPID
	 * 
	 * @param conn           The connection to use 
	 * @param spid           The SPID we want to get locks for
	 * @return List&lt;LockRecord&gt; never null
	 * @throws TimeoutException 
	 */
	public static List<LockRecord> getLockSummaryForSpid(DbxConnection conn, int spid) 
	throws TimeoutException
	{
		String sql = ""
			    + "select \n"
			    + "     spid       = req_spid \n"
			    + "    ,dbid       = rsc_dbid \n"
			    + "    ,dbname     = db_name(rsc_dbid) \n"
			    + "    ,objectid   = rsc_objid \n"
			    + "    ,schemaName = '' \n"            // filled in at a second pass because object_name() blocks in many cases
			    + "    ,tableName  = '' \n"            // filled in at a second pass because object_name() blocks in many cases
			    + "    ,indexId    = rsc_indid \n"
			    + "    ,indexName  = '' \n"            // filled in at a second pass because object_name() blocks in many cases
			    + "    ,type       = CASE WHEN rsc_type =  0 THEN 'LOCK RESOURCES' \n"
			    + "                       WHEN rsc_type =  1 THEN 'NULL' \n"
			    + "                       WHEN rsc_type =  2 THEN 'DB' \n"
			    + "                       WHEN rsc_type =  3 THEN 'FILE' \n"
			    + "                       WHEN rsc_type =  4 THEN 'INDEX' \n"
			    + "                       WHEN rsc_type =  5 THEN 'TABLE' \n"
			    + "                       WHEN rsc_type =  6 THEN 'PAGE ' \n"
			    + "                       WHEN rsc_type =  7 THEN 'KEY' \n"
			    + "                       WHEN rsc_type =  8 THEN 'EXT' \n"
			    + "                       WHEN rsc_type =  9 THEN 'RID' \n"
			    + "                       WHEN rsc_type = 10 THEN 'APP' \n"
			    + "                       WHEN rsc_type = 11 THEN 'MD' \n"
			    + "                       WHEN rsc_type = 12 THEN 'HBT' \n"
			    + "                       WHEN rsc_type = 13 THEN 'AU' \n"
			    + "                       ELSE '(UNKNOWN) - ' + cast(rsc_type as varchar(10)) \n"
			    + "                  END \n"
			    + "    ,mode       = CASE WHEN req_mode =  0 THEN 'NULL (placeholder)' \n"
			    + "                       WHEN req_mode =  1 THEN 'Sch-S (Schema stability)' \n"
			    + "                       WHEN req_mode =  2 THEN 'Sch-M (Schema modification)' \n"
			    + "                       WHEN req_mode =  3 THEN 'S (Shared)' \n"
			    + "                       WHEN req_mode =  4 THEN 'U (Update)' \n"
			    + "                       WHEN req_mode =  5 THEN 'X (Exclusive)' \n"
			    + "                       WHEN req_mode =  6 THEN 'IS (Intent Shared)' \n"
			    + "                       WHEN req_mode =  7 THEN 'IU (Intent Update)' \n"
			    + "                       WHEN req_mode =  8 THEN 'IX (Intent Exclusive)' \n"
			    + "                       WHEN req_mode =  9 THEN 'SIU (Shared Intent Update)' \n"
			    + "                       WHEN req_mode = 10 THEN 'SIX (Shared Intent Exclusive)' \n"
			    + "                       WHEN req_mode = 11 THEN 'UIX (Update Intent Exclusive)' \n"
			    + "                       WHEN req_mode = 12 THEN 'BU (Used by bulk operations)' \n"
			    + "                       WHEN req_mode = 13 THEN 'RangeS-S (Shared Key-Range and Shared Resource lock)' \n"
			    + "                       WHEN req_mode = 14 THEN 'RangeS-U (Shared Key-Range and Update Resource lock)' \n"
			    + "                       WHEN req_mode = 15 THEN 'RangeIn-N (Insert Key-Range and Null Resource lock)' \n"
			    + "                       WHEN req_mode = 16 THEN 'RangeIn-S (created by an overlap of RangeI_N and S locks)' \n"
			    + "                       WHEN req_mode = 17 THEN 'RangeIn-U (created by an overlap of RangeI_N and U locks)' \n"
			    + "                       WHEN req_mode = 18 THEN 'RangeIn-X (created by an overlap of RangeI_N and X locks)' \n"
			    + "                       WHEN req_mode = 19 THEN 'RangeX-S (created by an overlap of RangeI_N and RangeS_S locks)' \n"
			    + "                       WHEN req_mode = 20 THEN 'RangeX-U (created by an overlap of RangeI_N and RangeS_U locks)' \n"
			    + "                       WHEN req_mode = 21 THEN 'RangeX-X (Exclusive Key-Range and Exclusive Resource lock)' \n"
			    + "                       ELSE '(UNKNOWN) - ' + cast(req_mode as varchar(10)) \n"
			    + "                  END \n"
			    + "    ,status     = CASE WHEN req_status = 0 THEN 'LOCK REQ STATUS' \n"
			    + "                       WHEN req_status = 1 THEN 'GRANT' \n"
			    + "                       WHEN req_status = 2 THEN 'CNVT' \n"
			    + "                       WHEN req_status = 3 THEN 'WAIT' \n"
			    + "                       WHEN req_status = 4 THEN 'RELN' \n"
			    + "                       WHEN req_status = 5 THEN 'BLCKN' \n"
			    + "                       ELSE '(UNKNOWN) - ' + cast(req_status as varchar(10)) \n"
			    + "                  END \n"
			    + "    ,lockCount  = count(*) \n"
			    + "from master.dbo.syslockinfo \n"
			    + "where rsc_type != 2 -- DB \n"
			    + "  and req_spid = ? \n"
			    + "group by req_spid, rsc_dbid, rsc_objid, rsc_indid, rsc_type, req_mode, req_status \n"
			    + "";

		List<LockRecord> lockList = new ArrayList<>();

		try (PreparedStatement pstmnt = conn.prepareStatement(sql)) // Auto CLOSE
		{
			// Timeout after 1 second --- if we get blocked when doing: object_name()
			pstmnt.setQueryTimeout(1);
			
			// set SPID
			pstmnt.setInt(1, spid);
			
			try (ResultSet rs = pstmnt.executeQuery()) // Auto CLOSE
			{
				while(rs.next())
				{
					int    SPID       = rs.getInt   (1);
					int    dbid       = rs.getInt   (2);
					String dbname     = rs.getString(3);
					int    objectid   = rs.getInt   (4);
					String schemaName = rs.getString(5);
					String tableName  = rs.getString(6);
					int    indexId    = rs.getInt   (7);
					String indexName  = rs.getString(8);
					String lockType   = rs.getString(9);
					String lockMode   = rs.getString(10);
					String lockStatus = rs.getString(11);
					int    lockCount  = rs.getInt   (12);

					lockList.add( new LockRecord(SPID, dbid, dbname, objectid, schemaName, tableName, indexId, indexName, lockType, lockMode, lockStatus, lockCount) );
				}
			}
		}
		catch (SQLException ex)
		{
			if (ex.getMessage() != null && ex.getMessage().contains("query has timed out"))
			{
				_logger.warn("getLockSummaryForSpid: Problems getting Lock List (from syslockinfo). The query has timed out.");
				throw new TimeoutException();
			}
			else
			{
				_logger.warn("Problems when executing sql: " + sql + ". SQLException Error=" + ex.getErrorCode() + ", Msg='" + StringUtil.stripNewLine(ex.getMessage()) + "'.", ex);
			}
		}

		// Add all databases to a SET, which we later loops to get Schema, Table and Index Names
		Set<String> dbnames = new LinkedHashSet<>();
		for (LockRecord r : lockList)
			dbnames.add(r._dbname);

		// Loop above DB's
		for (String dbname : dbnames)
		{
			if (StringUtil.isNullOrBlank(dbname))
				continue;

			// Lookup SchemaName and TableName for the object id's
			// This because object_name(objid, dbid) is BLOCKING
			sql = ""
				    + "select \n"
				    + "     SchemaName = (select s.name from " + dbname + ".sys.objects o inner join " + dbname + ".sys.schemas s ON s.schema_id = o.schema_id where o.object_id = ?) \n"
				    + "    ,ObjectName = (select o.name from " + dbname + ".sys.objects o where o.object_id = ?) \n"
				    + "    ,IndexName  = (select i.name FROM " + dbname + ".sys.indexes i where i.object_id = ? and i.index_id = ?) \n"
				    + "";
			try (PreparedStatement pstmnt = conn.prepareStatement(sql)) // Auto CLOSE
			{
				// Timeout after 1 second --- if we get blocked when doing: object_name()
				pstmnt.setQueryTimeout(1);
				
				for (LockRecord r : lockList)
				{
					// set SPID
					pstmnt.setInt(1, r._objectid);
					pstmnt.setInt(2, r._objectid);
					pstmnt.setInt(3, r._objectid);
					pstmnt.setInt(4, r._indexId);
				
					try (ResultSet rs = pstmnt.executeQuery()) // Auto CLOSE
					{
						while(rs.next())
						{
							String schemaName = rs.getString(1);
							String tableName  = rs.getString(2);
							String indexName  = rs.getString(3);

							r._schemaName = schemaName == null ? "" : schemaName;
							r._tableName  = tableName  == null ? "" : tableName;
							r._indexName  = indexName  == null ? "" : indexName;
							
							if (r._objectid != 0 && r._indexId == 0)
								r._indexName = "-DATA-";
						}
					}
				}
			}
			catch (SQLException ex)
			{
				if (ex.getMessage() != null && ex.getMessage().contains("query has timed out"))
				{
					_logger.warn("getLockSummaryForSpid: Problems getting schema/table/index name. The query has timed out. But the lock information will still be returned (but without the schema/table/index name.");
					throw new TimeoutException();
				}
				else
				{
					_logger.warn("Problems when executing sql: " + sql + ". SQLException Error=" + ex.getErrorCode() + ", Msg='" + StringUtil.stripNewLine(ex.getMessage()) + "'.", ex);
				}
			}
		}
		
		return lockList;
	}

	/** 
	 * @return "" if no locks, otherwise a HTML TABLE, with the headers: DB, Table, Type, Count
	 */
	public static String getLockListTableAsHtmlTable(List<LockRecord> list)
	{
		if (list.isEmpty())
			return "";

		StringBuilder sb = new StringBuilder("<TABLE BORDER=1>");
		sb.append("<TR> <TH>spid</TH> <TH>dbid</TH> <TH>dbname</TH> <TH>ObjectID</TH> <TH>Schema</TH> <TH>Table</TH> <TH>IndexID</TH> <TH>IndexName</TH> <TH>Type</TH> <TH>Mode</TH> <TH>Status</TH> <TH>Count</TH> </TR>");
		for (LockRecord lr : list)
		{
			if (lr._lockStatus != null && lr._lockStatus.equals("WAIT"))
				sb.append("<TR style='color: red'>");
//			else if (lr._lockStatus != null && lr._lockStatus.equals("BLCKN???")) // FIXME: hmmm there is NO BLOCKING status... how the hell do we know if this row is "root cause" for BLOCKING lock or not 
//				sb.append("<TR style='color: red'>");
			else
				sb.append("<TR>");

			sb.append("<TD>").append(lr._spid      ).append("</TD>");
			sb.append("<TD>").append(lr._dbid      ).append("</TD>");
			sb.append("<TD>").append(lr._dbname    ).append("</TD>");
			sb.append("<TD>").append(lr._objectid  ).append("</TD>");
			sb.append("<TD>").append(lr._schemaName).append("</TD>");
			sb.append("<TD>").append(lr._tableName ).append("</TD>");
			sb.append("<TD>").append(lr._indexId   ).append("</TD>");
			sb.append("<TD>").append(lr._indexName ).append("</TD>");
			sb.append("<TD>").append(lr._lockType  ).append("</TD>");
			sb.append("<TD>").append(lr._lockMode  ).append("</TD>");
			sb.append("<TD>").append(lr._lockStatus).append("</TD>");
			sb.append("<TD>").append(lr._lockCount ).append("</TD>");
			sb.append("</TR>");
		}
		sb.append("</TABLE>");
		return sb.toString();
	}

	/** 
	 * @return "" if no locks, otherwise a ASCII TABLE, with the headers: DBName, TableName, LockType, LockCount
	 */
	public static String getLockListTableAsAsciiTable(List<LockRecord> list)
	{
		if (list.isEmpty())
			return "";

		// Table HEAD
		String[] tHead = new String[] {"spid", "dbid", "dbname", "ObjectID", "SchemaName", "TableName", "IndexId", "IndexName", "LockType", "LockMode", "LockStatus", "LockCount"};

		// Table DATA
		List<List<Object>> tData = new ArrayList<>();
		for (LockRecord lr : list)
		{
			List<Object> row = new ArrayList<>();
			
			row.add(lr._spid      );
			row.add(lr._dbid      );
			row.add(lr._dbname    );
			row.add(lr._objectid  );
			row.add(lr._schemaName);
			row.add(lr._tableName );
			row.add(lr._indexId   );
			row.add(lr._indexName );
			row.add(lr._lockType  );
			row.add(lr._lockMode  );
			row.add(lr._lockStatus);
			row.add(lr._lockCount );
			
			tData.add(row);
		}

		return StringUtil.toTableString(Arrays.asList(tHead), tData);
	}

	/**
	 * Get a lock summary for a SPID
	 * 
	 * @param lockList       The lockList produced by: getLockSummaryForSpid(DbxConnection conn, int spid)
	 * @param asHtml         Produce a HTML table (if false a ASCII table will be produced)
	 * @param htmlBeginEnd   (if asHtml=true) should we wrap the HTML with begin/end tags
	 * @return
	 */
	public static String getLockSummaryForSpid(List<LockRecord> lockList, boolean asHtml, boolean htmlBeginEnd)
	{
		if (lockList.isEmpty())
			return null;
	
		if (asHtml)
		{
			String htmlTable = getLockListTableAsHtmlTable(lockList);
			if (htmlBeginEnd)
				return "<html>" + htmlTable + "</html>";
			else
				return htmlTable;
		}
		else
			return getLockListTableAsAsciiTable(lockList);
	}

	/**
	 * Get a lock summary for a SPID
	 * 
	 * @param conn           The connection to use 
	 * @param spid           The SPID we want to get locks for
	 * @param asHtml         Produce a HTML table (if false a ASCII table will be produced)
	 * @param htmlBeginEnd   (if asHtml=true) should we wrap the HTML with begin/end tags
	 * @return
	 * @throws TimeoutException 
	 */
	public static String getLockSummaryForSpid(DbxConnection conn, int spid, boolean asHtml, boolean htmlBeginEnd) 
	throws TimeoutException
	{
		List<LockRecord> lockList = getLockSummaryForSpid(conn, spid);

		return getLockSummaryForSpid(lockList, asHtml, htmlBeginEnd);
	}

	/**
	 * LockRecord used by: getLockSummaryForSpid(spid)
	 */
	public static class LockRecord
	{
		public int    _spid       = 0;
		public int    _dbid       = 0;
		public String _dbname     = "";
		public int    _objectid   = 0;
		public String _schemaName = "";
		public String _tableName  = "";
		public int    _indexId    = 0;
		public String _indexName  = "";
		public String _lockType   = "";
		public String _lockMode   = "";
		public String _lockStatus = "";
		public int    _lockCount  = 0;

		public LockRecord(int spid, int dbid, String dbname, int objectid, String schemaName, String tableName, int indexId, String indexName, String lockType, String lockMode, String lockStatus, int lockCount)
		{
			_spid       = spid       ;
			_dbid       = dbid       ;
			_dbname     = dbname     == null ? "" : dbname;
			_objectid   = objectid   ;
			_schemaName = schemaName == null ? "" : schemaName;
			_tableName  = tableName  == null ? "" : tableName;
			_indexId    = indexId    ;
			_indexName  = indexName  == null ? "" : indexName;
			_lockType   = lockType   == null ? "" : lockType;
			_lockMode   = lockMode   == null ? "" : lockMode;
			_lockStatus = lockStatus == null ? "" : lockStatus;
			_lockCount  = lockCount  ;
		}
	}

	/**
	 * ObjectName details
	 */
	public static class ObjectName
	{
		public int    _dbid       = 0;
		public int    _objectid   = 0;

		public String _dbname     = "";
		public String _schemaName = "";
		public String _tableName  = "";
	}

	/**
	 * Get object information
	 * 
	 * @param conn
	 * @param dbid
	 * @param objectid
	 * @return ObjectName   null if problems
	 */
	public static ObjectName getObjectName(DbxConnection conn, int dbid, int objectid)
	{
		String sql    = "select db_name(?)";
		String dbname = null;

		try (PreparedStatement pstmnt = conn.prepareStatement(sql)) // Auto CLOSE
		{
			// Timeout after 1 second --- if we get blocked when doing: object_name()
			pstmnt.setQueryTimeout(1);

			// set SPID
			pstmnt.setInt(1, dbid);
		
			try (ResultSet rs = pstmnt.executeQuery()) // Auto CLOSE
			{
				while(rs.next())
				{
					dbname = rs.getString(1);
				}
			}
		}
		catch (SQLException ex)
		{
			_logger.warn("Problems when executing sql: " + sql + ". SQLException Error=" + ex.getErrorCode() + ", Msg='" + StringUtil.stripNewLine(ex.getMessage()) + "'.", ex);
		}

		if (StringUtil.isNullOrBlank(dbname))
			return null;
		
		return getObjectName(conn, dbname, objectid);
	}
	
	/**
	 * Get object information
	 * 
	 * @param conn
	 * @param dbname
	 * @param objectid
	 * @return ObjectName   null if problems
	 */
	public static ObjectName getObjectName(DbxConnection conn, String dbname, int objectid)
	{
		String sql = ""
			    + "select \n"
			    + "     dbid       = (select db_id(?)) \n"
			    + "    ,SchemaName = (select s.name from " + dbname + ".sys.objects o inner join " + dbname + ".sys.schemas s ON s.schema_id = o.schema_id where o.object_id = ?) \n"
			    + "    ,ObjectName = (select o.name from " + dbname + ".sys.objects o where o.object_id = ?) \n"
			    + "";

		ObjectName objName = new ObjectName();

		try (PreparedStatement pstmnt = conn.prepareStatement(sql)) // Auto CLOSE
		{
			// Timeout after 1 second --- if we get blocked when doing: object_name()
			pstmnt.setQueryTimeout(1);

			// set SPID
			pstmnt.setString(1, dbname);
			pstmnt.setInt   (2, objectid);
			pstmnt.setInt   (3, objectid);
		
			try (ResultSet rs = pstmnt.executeQuery()) // Auto CLOSE
			{
				while(rs.next())
				{
					int    dbid       = rs.getInt   (1);
					String schemaName = rs.getString(2);
					String tableName  = rs.getString(3);

					objName._dbid       = dbid;
					objName._dbname     = dbname;
					
					objName._objectid   = objectid;
					objName._schemaName = schemaName;
					objName._tableName  = tableName;
				}
			}
		}
		catch (SQLException ex)
		{
			_logger.warn("Problems when executing sql: " + sql + ". SQLException Error=" + ex.getErrorCode() + ", Msg='" + StringUtil.stripNewLine(ex.getMessage()) + "'.", ex);
		}

		if (StringUtil.isNullOrBlank(objName._dbname))
			return null;
		
		return objName;
	}

}
