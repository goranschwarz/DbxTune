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
package com.dbxtune.sql.conn;

import java.lang.invoke.MethodHandles;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.text.NumberFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.dbxtune.gui.ResultSetTableModel;
import com.dbxtune.sql.conn.info.DbmsVersionInfo;
import com.dbxtune.sql.conn.info.DbmsVersionInfoSqlServer;
import com.dbxtune.sql.conn.info.DbxConnectionStateInfo;
import com.dbxtune.sql.conn.info.DbxConnectionStateInfoSqlServer;
import com.dbxtune.ui.autocomplete.completions.TableExtraInfo;
import com.dbxtune.utils.Configuration;
import com.dbxtune.utils.DbUtils;
import com.dbxtune.utils.MathUtils;
import com.dbxtune.utils.StringUtil;
import com.dbxtune.utils.Ver;
import com.microsoft.sqlserver.jdbc.ISQLServerMessageHandler;
import com.microsoft.sqlserver.jdbc.SQLServerConnection;

public class SqlServerConnection 
extends DbxConnection
{
	private static final Logger _logger = LogManager.getLogger(MethodHandles.lookup().lookupClass());

	public static final String  PROPKEY_getTableExtraInfo_useSpSpaceused = "SqlServerConnection.getTableExtraInfo.useSpSpaceused";
	public static final boolean DEFAULT_getTableExtraInfo_useSpSpaceused = true;

	public static final String  PROPKEY_getTableExtraInfo_getIndexTypeInfo = "SqlServerConnection.getTableExtraInfo.getIndexTypeInfo";
	public static final boolean DEFAULT_getTableExtraInfo_getIndexTypeInfo = true;

	public SqlServerConnection(Connection conn)
	{
		super(conn);
		Ver.majorVersion_mustBeTenOrAbove = false;
//System.out.println("constructor::SqlServerConnection(conn): conn="+conn);
	}

	// Cached values
	private List<String> _getActiveServerRolesOrPermissions = null;

	@Override
	public DbmsVersionInfo createDbmsVersionInfo()
	{
		return new DbmsVersionInfoSqlServer(this);
	}

	@Override
	public void clearCachedValues()
	{
		_getActiveServerRolesOrPermissions = null;
		super.clearCachedValues();
	}

	@Override
	public DbxConnectionStateInfo refreshConnectionStateInfo()
	{
		DbxConnectionStateInfo csi = new DbxConnectionStateInfoSqlServer(this);
		setConnectionStateInfo(csi);
		return csi;
	}

	@Override
	public boolean isInTransaction() throws SQLException
	{
		return false; // FIXME: Don't know how to check this, so lets assume FALSE
	}

	/**
	 * If the server handles databases like MS SQL_Server and Sybase ASE
	 * @return true or false
	 */
	@Override
	public boolean isDatabaseAware()
	{
		return true;
	}

	public static String getEngineEdition(int engineEditionInt)
	{
		if      (engineEditionInt == 1) return "Personal/Desktop";
		else if (engineEditionInt == 2) return "Standard";        
		else if (engineEditionInt == 3) return "Enterprise";      
		else if (engineEditionInt == 4) return "Express";         
		else if (engineEditionInt == 5) return "Azure SQL Database";
		else if (engineEditionInt == 6) return "Azure SQL Data Warehouse";
	//	else if (engineEditionInt == 7) return "";
		else if (engineEditionInt == 8) return "Azure Managed Instance";
		else                            return "-unknown-";
	}

	public int getEngineEdition()
	{
		int engineEditionInt = -1;
		String sql = "SELECT CAST(SERVERPROPERTY('EngineEdition') as int)";
		try (Statement stmnt = _conn.createStatement(); ResultSet rs = stmnt.executeQuery(sql))
		{
			while(rs.next())
			{
				engineEditionInt = rs.getInt(1);
			}
		}
		catch (SQLException ex)
		{
			_logger.error("Problem discovering SQL-Server EngineEdition using SQL=|" + sql + "|. Caught: " + ex);
		}
		return engineEditionInt;
	}

	@Override
	public long getDbmsVersionNumber()
	{
		long srvVersionNum = 0;

		// version
		try
		{
			int engineEditionInt = getEngineEdition();

			long baseAzureVersion = 0;
			if      (engineEditionInt == 5) baseAzureVersion = DbmsVersionInfoSqlServer.VERSION_AZURE_SQL_DB;             // "Azure SQL Database";
			else if (engineEditionInt == 6) baseAzureVersion = DbmsVersionInfoSqlServer.VERSION_AZURE_SYNAPSE_ANALYTICS;  // "Azure SQL Data Warehouse";
			else if (engineEditionInt == 8) baseAzureVersion = DbmsVersionInfoSqlServer.VERSION_AZURE_MANAGED_INSTANCE;   // "Azure Managed Instance";
			
//			DbmsVersionInfoSqlServer dbmsVersion = (DbmsVersionInfoSqlServer) getDbmsVersionInfo();
			
			// If any AZURE Instance
			if (baseAzureVersion != 0)
			{
				// 5 == Azure SQL Database
				if (engineEditionInt == 5) 
				{
					srvVersionNum = DbmsVersionInfoSqlServer.VERSION_AZURE_SQL_DB;

					// Azure SQL Database version String looks like this: Microsoft SQL Azure (RTM) - 12.0.2000.8 Feb 23 2022 11:32:53 Copyright (C) 2021 Microsoft Corporation
					// Lets grab "month day year" and keep that as of what Azure "version" we are connected to 
					String versionStr = getDbmsVersionStr();
					
					if (StringUtil.hasValue(versionStr))
					{
						// Microsoft SQL Azure (RTM) - 12.0.2000.8 Feb 23 2022 11:32:53 Copyright (C) 2021 Microsoft Corporation
						String buildDateStr = StringUtils.trim( StringUtils.substringBetween(versionStr, " - ", "Copyright") );
						if (StringUtil.hasValue(buildDateStr))
							buildDateStr = StringUtils.trim( StringUtils.substringAfter(buildDateStr, " ") );
						if (StringUtil.hasValue(buildDateStr))
						{
							try
							{
								SimpleDateFormat sdf = new SimpleDateFormat("MMM dd yyyy HH:mm:ss", Locale.ENGLISH); // (Use Locale.ENGLISH to ensure English month parsing.)
								Date buildDate = sdf.parse(buildDateStr);

								Calendar calendar = new GregorianCalendar();
								calendar.setTime(buildDate);

								int year  = calendar.get(Calendar.YEAR);
								int month = calendar.get(Calendar.MONTH);
								int day   = calendar.get(Calendar.DAY_OF_MONTH);
								
								int newVersionNum = 0;
								newVersionNum += year  * 1_0000;
								newVersionNum += month * 1_00;
								newVersionNum += day;

								srvVersionNum = srvVersionNum + newVersionNum;
								
								// System.out.println(">>>> ---- AZURE-SQL-DATABASE: buildStr=|"+buildDateStr+"|, year="+year+", month="+month+", day="+day+", newVersionNum="+srvVersionNum);
							}
							catch (ParseException ex)
							{
								_logger.warn("Problem parsing the buildDate='" + buildDateStr + "' from the Version String '" + versionStr + "'. Caught: " + ex);
							}
						}
					}
				}
				
				// 6 == Azure Synapse Analytics
				if (engineEditionInt == 6) 
				{
					// TODO: parse some more info to build "subversion" (on release date)
					srvVersionNum = DbmsVersionInfoSqlServer.VERSION_AZURE_SYNAPSE_ANALYTICS;
				}

				// 8 == Azure SQL Server Managed Instance
				if (engineEditionInt == 8)
				{
					// TODO: parse some more info to build "subversion" (on release date)
					srvVersionNum = DbmsVersionInfoSqlServer.VERSION_AZURE_MANAGED_INSTANCE;
				}
			}
			else
			{
				String versionStr = getDbmsVersionStr();
			//	srvVersionNum = VersionSqlServer.parseVersionStringToNumber(versionStr);
				srvVersionNum = Ver.sqlServerVersionStringToNumber(versionStr);
			}
		}
		catch (SQLException ex)
		{
			//_logger.error("SqlServerConnection.getDbmsVersionNumber(), '"+sql+"'", ex);
		}
		
		return srvVersionNum;
	}
//	public static long getDbmsVersionNumber()
//	{
//		final int UNKNOWN = -1;
//
//		String versionStr = getDbmsVersionStr();
//
//		long versionNum = Ver.sqlServerVersionStringToNumber(versionStr);
//		return versionNum;
//	}

	@Override
	public String getDbmsVersionStr() 
	throws SQLException
	{
		final String UNKNOWN = "";

		if ( ! isConnectionOk() )
			return UNKNOWN;

		String sql = "select @@version";

		try
		{
			String verStr = UNKNOWN;

			Statement stmt = _conn.createStatement();
			ResultSet rs = stmt.executeQuery(sql);
			while (rs.next())
			{
				verStr = rs.getString(1);
			}
			rs.close();
			stmt.close();

			if (verStr != null)
				verStr = verStr.replace('\n', ' ');
			return verStr;
		}
		catch (SQLException e)
		{
			_logger.debug("When getting DBMS Version ('"+sql+"'), Caught exception.", e);

			return UNKNOWN;
		}
	}

	@Override
	public List<String> getActiveServerRolesOrPermissions()
	{
		if (_getActiveServerRolesOrPermissions != null)
			return _getActiveServerRolesOrPermissions;

//		RS> Col# Label           JDBC Type Name          Guessed DBMS type Source Table
//		RS> ---- --------------- ----------------------- ----------------- ------------
//		RS> 1    entity_name     java.sql.Types.NVARCHAR nvarchar(128)     -none-      
//		RS> 2    subentity_name  java.sql.Types.NVARCHAR nvarchar(128)     -none-      
//		RS> 3    permission_name java.sql.Types.NVARCHAR nvarchar(60)      -none-      
//		+-----------+--------------+-------------------------------+
//		|entity_name|subentity_name|permission_name                |
//		+-----------+--------------+-------------------------------+
//		|server     |              |CONNECT SQL                    |
//		|server     |              |SHUTDOWN                       |
//		|server     |              |CREATE ENDPOINT                |
//		|server     |              |CREATE ANY DATABASE            |
//		|server     |              |ALTER ANY LOGIN                |
//		|server     |              |ALTER ANY CREDENTIAL           |
//		|server     |              |ALTER ANY ENDPOINT             |
//		|server     |              |ALTER ANY LINKED SERVER        |
//		|server     |              |ALTER ANY CONNECTION           |
//		|server     |              |ALTER ANY DATABASE             |
//		|server     |              |ALTER RESOURCES                |
//		|server     |              |ALTER SETTINGS                 |
//		|server     |              |ALTER TRACE                    |
//		|server     |              |ADMINISTER BULK OPERATIONS     |
//		|server     |              |AUTHENTICATE SERVER            |
//		|server     |              |EXTERNAL ACCESS ASSEMBLY       |
//		|server     |              |VIEW ANY DATABASE              |
//		|server     |              |VIEW ANY DEFINITION            |
//		|server     |              |VIEW SERVER STATE              |
//		|server     |              |CREATE DDL EVENT NOTIFICATION  |
//		|server     |              |CREATE TRACE EVENT NOTIFICATION|
//		|server     |              |ALTER ANY EVENT NOTIFICATION   |
//		|server     |              |ALTER SERVER STATE             |
//		|server     |              |UNSAFE ASSEMBLY                |
//		|server     |              |ALTER ANY SERVER AUDIT         |
//		|server     |              |CONTROL SERVER                 |
//		+-----------+--------------+-------------------------------+
//		(26 rows affected)
		
		String sql = "select * from sys.fn_my_permissions(default,default)";
		try
		{
			List<String> permissionList = new LinkedList<String>();
			Statement stmt = this.createStatement();
			ResultSet rs = stmt.executeQuery(sql);
			while (rs.next())
			{
				String role = rs.getString(3);
				if ( ! permissionList.contains(role) )
					permissionList.add(role);
			}
			rs.close();
			stmt.close();

			if (_logger.isDebugEnabled())
				_logger.debug("getActiveServerRolesOrPermissions() returns, permissionList='"+permissionList+"'.");

			// Cache the value for next execution
			_getActiveServerRolesOrPermissions = permissionList;
			return permissionList;
		}
		catch (SQLException ex)
		{
			_logger.warn("Problems when executing sql: "+sql, ex);
			return null;
		}
	}

	protected ISQLServerMessageHandler _oldMsgHandlerSqlServer = null;

	public void setMessageHandler(ISQLServerMessageHandler messageHandler)
	{
		if (_conn instanceof SQLServerConnection)
		{
    		// Save the current message handler so we can restore it later
			_oldMsgHandlerSqlServer = ((SQLServerConnection)_conn).getServerMessageHandler();
    
    		((SQLServerConnection)_conn).setServerMessageHandler(messageHandler);
		}
	}

	public void restoreMessageHandler()
	{
		if (_conn instanceof SQLServerConnection)
		{
			((SQLServerConnection)_conn).setServerMessageHandler(_oldMsgHandlerSqlServer);
		}
	}

	/**
	 * Get (procedure) text about an object
	 * 
	 * @param conn       Connection to the database
	 * @param dbname     Name of the database (if null, current db will be used)
	 * @param objectName Name of the procedure/view/trigger...
	 * @param owner      Name of the owner, if null is passed, it will be set to 'dbo'
	 * @param srvVersion Version of the ASE, if 0, the version will be fetched from ASE
	 * @return Text of the procedure/view/trigger...
	 */
	public String getObjectText(String dbname, String objectName, String owner, long dbmsVersion)
	{
		if (StringUtil.isNullOrBlank(owner))
			owner = "dbo.";
		else
			owner = owner + ".";

//		if (dbmsVersion <= 0)
//		{
//			dbmsVersion = getDbmsVersionNumber();
//		}

		String returnText = null;
		
		String dbnameStr = dbname;
		if (dbnameStr == null)
			dbnameStr = "";
		else
			dbnameStr = dbname + ".dbo.";
			
		//--------------------------------------------
		// GET OBJECT TEXT
		// 
		// TODO: Probably use 'sys.sql_modules' instead
		// select definition from ${dbname}.sys.sql_modules where object_id = object_id('schema.objname')
		// +-----------+--------------------------------------------------------------+---------------+----------------------+---------------+-----------------------+-------------+------------------+-----------------------+-----------------------+-----------+-------------+
		// |object_id  |definition                                                    |uses_ansi_nulls|uses_quoted_identifier|is_schema_bound|uses_database_collation|is_recompiled|null_on_null_input|execute_as_principal_id|uses_native_compilation|inline_type|is_inlineable|
		// +-----------+--------------------------------------------------------------+---------------+----------------------+---------------+-----------------------+-------------+------------------+-----------------------+-----------------------+-----------+-------------+
		// |814625945  |CREATE PROCEDURE Sequences.ReseedSequenceBeyondTableValues ...|true           |true                  |false          |false                  |false        |false             |(NULL)                 |false                  |false      |false        |
		// +-----------+--------------------------------------------------------------+---------------+----------------------+---------------+-----------------------+-------------+------------------+-----------------------+-----------------------+-----------+-------------+


		String sql = dbnameStr + "sp_helptext '" + owner + objectName + "'";

		try
		{
			StringBuilder sb = new StringBuilder();

			Statement statement = createStatement();
			ResultSet rs = statement.executeQuery(sql);
			while(rs.next())
			{
				String textPart = rs.getString(1);
				sb.append(textPart);
			}
			rs.close();
			statement.close();

			if (sb.length() > 0)
				returnText = sb.toString();
		}
		catch (SQLException e)
		{
			returnText = null;
			_logger.warn("Problems getting text for object '"+objectName+"', with owner '"+owner+"', in db '"+dbname+"'. Caught: "+e); 
		}

		return returnText;
	}

	@Override
	public Map<String, TableExtraInfo> getTableExtraInfo(String cat, String schema, String table)
	{
		String catOrigin    = cat;
//		String schemaOrigin = schema;
//		String tableOrigin  = table;

		LinkedHashMap<String, TableExtraInfo> extraInfo = new LinkedHashMap<>();

		cat    = StringUtil.isNullOrBlank(cat)    ? "" : cat    + ".";
		schema = StringUtil.isNullOrBlank(schema) ? "" : schema + ".";

		int indexCount = 0;

		// When was the SQL Server restarted (used when displaying index "usage")
		// We could have used... 'select sqlserver_start_time from sys.dm_os_sys_info' 
		// But tempdb will always be recreated... and we don't need any special privileges for querying 'sys.databases'
		String srvUptime = null;
		boolean getSrvUpTime = true;
		if (getSrvUpTime)
		{
			String sql = ""
					+ "SELECT srvUpTime = CAST(datediff(day,  create_date, getdate())      AS varchar(10)) + 'd:' \n"
					+ "                 + CAST(datediff(hour, create_date, getdate()) % 24 AS varchar(10)) + 'h'  \n"
					+ "FROM sys.databases \n"
					+ "WHERE database_id = 2"
					;

			try (Statement stmnt = _conn.createStatement(); ResultSet rs = stmnt.executeQuery(sql))
			{
				while(rs.next())
				{
					srvUptime = rs.getString(1);
				}
			}
			catch (SQLException ex)
			{
				_logger.warn("Problems getting 'Server uptime', ErrorCode=" + ex.getErrorCode() + ", SqlState=" + ex.getSQLState() + ", Message='" + ex.getMessage() + "', SQL=|" + sql + "|");
			}
			
		}
		
		boolean getIndexTypeInfo = Configuration.getCombinedConfiguration().getBooleanProperty(PROPKEY_getTableExtraInfo_getIndexTypeInfo, DEFAULT_getTableExtraInfo_getIndexTypeInfo);
		if (getIndexTypeInfo)
		{
			// TODO; Add OTHER information here:
			//        * Definition: [Property] ...         -- [CX] [1 KEY]    ...   
			//        * Usage Stats                        -- Reads: 14 (13 scan 1 lookup) Writes: 12
			//        * Op Stats                           -- 38,205 singleton lookups; 10 scans/seeks; 0 deletes; 0 updates;
			//        * Size                               -- 35,908,423 rows; 1.1GB
			//        * Compression Type                   -- Partition 1 uses PAGE
			//        * Lock Waits                         -- 0 lock waits;
			//        * Referenced by FK?                  -- false
			//        * FK Covered by Index?               -- 
			//        * Last User Seek                     -- 2024-11-21 05:33:15.88
			//        * Last User Scan                     -- (NULL)
			//        * Last User Lookup                   -- (NULL)
			//        * Last User Write                    -- (NULL)
			//        * Created                            -- 2019-09-08 20:35:26.693
			//        * Last Modified                      -- 2024-11-21 14:55:13.41
			//        * Page Latch Wait Count              -- 1 432
			//        * Page Latch Wait Time (D:H:M:S)     -- 0:00:00:00
			//        * Page IO Latch Wait Count           -- 3
			//        * Page IO Latch Wait Time (D:H:M:S)  -- 0:00:00:00
			//      The above is from sp_BlitzIndex ... probably to much to get into here... But lets get SOME additional info
			//        * Definition:       -- from ??? but should contain: HEAP / CLIX / IX-VIEW / XML / SPATIAL / COLUMNSTORE / IN-MEMORY / DISABLED / UNIQUE / UNIQUE CONSTRAINT / FILTER 
			//        * Usage Stats       -- from: sys.dm_db_index_usage_stats
			//        * Op Stats          -- from: sys.dm_db_index_operational_stats
			//        * Size              -- from: sys.dm_db_partition_stats
			//        * Compression Type  -- from: sys.partitions
			//        * ??? Missing Indexes...

			boolean getExtendedIndexInfo = true;
			boolean getExtendedIndexInfoOperationalDetails = true;
			if (getExtendedIndexInfo)
			{
				// SELECT * FROM sys.indexes                   WHERE object_id = @objid
				// +----------+-----------------+--------+----+------------+---------+-------------+--------------+--------------+--------------------+-----------+---------+-----------+---------------+--------------------------+---------------+----------------+----------+-----------------+-----------------+-------------------------+------------+---------------------------+
				// |object_id |name             |index_id|type|type_desc   |is_unique|data_space_id|ignore_dup_key|is_primary_key|is_unique_constraint|fill_factor|is_padded|is_disabled|is_hypothetical|is_ignored_in_optimization|allow_row_locks|allow_page_locks|has_filter|filter_definition|compression_delay|suppress_dup_key_messages|auto_created|optimize_for_sequential_key|
				// +----------+-----------------+--------+----+------------+---------+-------------+--------------+--------------+--------------------+-----------+---------+-----------+---------------+--------------------------+---------------+----------------+----------+-----------------+-----------------+-------------------------+------------+---------------------------+
				// |72 853 680|idx_Kapital_Datum|       7|   2|NONCLUSTERED|false    |            1|false         |false         |false               |          0|false    |false      |false          |false                     |true           |true            |false     |(NULL)           |(NULL)           |false                    |false       |false                      |
				// |72 853 680|gorans_fix1      |      18|   2|NONCLUSTERED|false    |            1|false         |false         |false               |          0|false    |false      |false          |false                     |true           |true            |false     |(NULL)           |(NULL)           |false                    |false       |false                      |
				// +----------+-----------------+--------+----+------------+---------+-------------+--------------+--------------+--------------------+-----------+---------+-----------+---------------+--------------------------+---------------+----------------+----------+-----------------+-----------------+-------------------------+------------+---------------------------+
				// 
				// SELECT * FROM sys.partitions                WHERE object_id = @objid
				// +----------------------+----------+--------+----------------+----------------------+-----------+-----------------------+----------------+---------------------+
				// |partition_id          |object_id |index_id|partition_number|hobt_id               |rows       |filestream_filegroup_id|data_compression|data_compression_desc|
				// +----------------------+----------+--------+----------------+----------------------+-----------+-----------------------+----------------+---------------------+
				// |72 057 710 507 393 024|72 853 680|       7|               1|72 057 710 507 393 024|395 683 243|                      0|               0|NONE                 |
				// |72 057 710 507 786 240|72 853 680|      18|               1|72 057 710 507 786 240|395 683 243|                      0|               0|NONE                 |
				// +----------------------+----------+--------+----------------+----------------------+-----------+-----------------------+----------------+---------------------+
				// 
				// SELECT * FROM sys.dm_db_partition_stats     WHERE object_id = @objid
				// +----------------------+----------+--------+----------------+----------------------+----------------------+--------------------------+-------------------+-----------------------+----------------------------+--------------------------------+---------------+-------------------+-----------+
				// |partition_id          |object_id |index_id|partition_number|in_row_data_page_count|in_row_used_page_count|in_row_reserved_page_count|lob_used_page_count|lob_reserved_page_count|row_overflow_used_page_count|row_overflow_reserved_page_count|used_page_count|reserved_page_count|row_count  |
				// +----------------------+----------+--------+----------------+----------------------+----------------------+--------------------------+-------------------+-----------------------+----------------------------+--------------------------------+---------------+-------------------+-----------+
				// |72 057 710 507 393 024|72 853 680|       7|               1|               831 269|               833 827|                   834 000|                  0|                      0|                           0|                               0|        833 827|            834 000|395 683 243|
				// |72 057 710 507 786 240|72 853 680|      18|               1|             4 631 647|             4 660 299|                 4 660 535|                  0|                      0|                           0|                               0|      4 660 299|          4 660 535|395 683 243|
				// +----------------------+----------+--------+----------------+----------------------+----------------------+--------------------------+-------------------+-----------------------+----------------------------+--------------------------------+---------------+-------------------+-----------+
				// 
				// SELECT * FROM sys.dm_db_index_usage_stats   WHERE object_id = @objid
				// +-----------+----------+--------+----------+----------+------------+------------+-----------------------+-----------------------+----------------+-----------------------+------------+------------+--------------+--------------+----------------+-----------------------+------------------+------------------+
				// |database_id|object_id |index_id|user_seeks|user_scans|user_lookups|user_updates|last_user_seek         |last_user_scan         |last_user_lookup|last_user_update       |system_seeks|system_scans|system_lookups|system_updates|last_system_seek|last_system_scan       |last_system_lookup|last_system_update|
				// +-----------+----------+--------+----------+----------+------------+------------+-----------------------+-----------------------+----------------+-----------------------+------------+------------+--------------+--------------+----------------+-----------------------+------------------+------------------+
				// |         15|72 853 680|       7|        11|         0|           0|           4|2024-11-21 03:55:49.813|(NULL)                 |(NULL)          |2024-11-20 08:31:19.917|           0|           0|             0|             0|(NULL)          |(NULL)                 |(NULL)            |(NULL)            |
				// |         15|72 853 680|      18|        15|         0|           0|           4|2024-11-21 06:32:45.75 |(NULL)                 |(NULL)          |2024-11-20 08:31:19.917|           0|           0|             0|             0|(NULL)          |(NULL)                 |(NULL)            |(NULL)            |
				// +-----------+----------+--------+----------+----------+------------+------------+-----------------------+-----------------------+----------------+-----------------------+------------+------------+--------------+--------------+----------------+-----------------------+------------------+------------------+
				// 
				// SELECT * FROM sys.dm_db_index_operational_stats(@dbid, @objid, null, null)
				// +-----------+----------+--------+----------------+----------------------+-----------------+-----------------+-----------------+----------------+--------------------+--------------------+--------------------+---------------------+------------------------+---------------------+------------------------+----------------+----------------------+---------------------+------------------+------------------+-----------------------+-----------------------+---------------------------+---------------------------+-------------------------------+------------------------------+--------------+-------------------+-------------------+---------------+--------------------+--------------------+----------------------------------+--------------------------+---------------------+---------------------+------------------------+------------------------+--------------------------+--------------------------+-----------------------------+-----------------------------+------------------------------+------------------------------+-----------------------+------------------------+-------------------+--------------------+-------------------------------+--------------------------------+
				// |database_id|object_id |index_id|partition_number|hobt_id               |leaf_insert_count|leaf_delete_count|leaf_update_count|leaf_ghost_count|nonleaf_insert_count|nonleaf_delete_count|nonleaf_update_count|leaf_allocation_count|nonleaf_allocation_count|leaf_page_merge_count|nonleaf_page_merge_count|range_scan_count|singleton_lookup_count|forwarded_fetch_count|lob_fetch_in_pages|lob_fetch_in_bytes|lob_orphan_create_count|lob_orphan_insert_count|row_overflow_fetch_in_pages|row_overflow_fetch_in_bytes|column_value_push_off_row_count|column_value_pull_in_row_count|row_lock_count|row_lock_wait_count|row_lock_wait_in_ms|page_lock_count|page_lock_wait_count|page_lock_wait_in_ms|index_lock_promotion_attempt_count|index_lock_promotion_count|page_latch_wait_count|page_latch_wait_in_ms|page_io_latch_wait_count|page_io_latch_wait_in_ms|tree_page_latch_wait_count|tree_page_latch_wait_in_ms|tree_page_io_latch_wait_count|tree_page_io_latch_wait_in_ms|page_compression_attempt_count|page_compression_success_count|version_generated_inrow|version_generated_offrow|ghost_version_inrow|ghost_version_offrow|insert_over_ghost_version_inrow|insert_over_ghost_version_offrow|	
				// +-----------+----------+--------+----------------+----------------------+-----------------+-----------------+-----------------+----------------+--------------------+--------------------+--------------------+---------------------+------------------------+---------------------+------------------------+----------------+----------------------+---------------------+------------------+------------------+-----------------------+-----------------------+---------------------------+---------------------------+-------------------------------+------------------------------+--------------+-------------------+-------------------+---------------+--------------------+--------------------+----------------------------------+--------------------------+---------------------+---------------------+------------------------+------------------------+--------------------------+--------------------------+-----------------------------+-----------------------------+------------------------------+------------------------------+-----------------------+------------------------+-------------------+--------------------+-------------------------------+--------------------------------+
				// |         15|72 853 680|       7|               1|72 057 710 507 393 024|        1 845 114|                0|                0|               0|               3 876|                   0|                   0|                    0|                      24|                    0|                       0|               6|                     0|                    0|                 0|                 0|                      0|                      0|                          0|                          0|                              0|                             0|             0|                  0|                  0|        831 514|                   0|                   0|                                 6|                         0|              115 302|                  512|                     185|                   2 472|                   115 302|                       512|                          184|                        2 438|                             0|                             0|                      0|                       0|                  0|                   0|                              0|                               0|
				// |         15|72 853 680|      18|               1|72 057 710 507 786 240|        1 845 114|                0|                0|               0|              12 309|                   0|                   0|                    0|                      24|                    0|                       0|          20 416|                     0|                    0|                 0|                 0|                      0|                      0|                          0|                          0|                              0|                             0|     2 845 314|                  0|                  0|         52 953|                   0|                   0|                                 5|                         1|                  601|                    4|                  24 245|                 377 229|                       601|                         4|                       24 225|                      374 963|                             0|                             0|                      0|                       0|                  0|                   0|                              0|                               0|
				// +-----------+----------+--------+----------------+----------------------+-----------------+-----------------+-----------------+----------------+--------------------+--------------------+--------------------+---------------------+------------------------+---------------------+------------------------+----------------+----------------------+---------------------+------------------+------------------+-----------------------+-----------------------+---------------------------+---------------------------+-------------------------------+------------------------------+--------------+-------------------+-------------------+---------------+--------------------+--------------------+----------------------------------+--------------------------+---------------------+---------------------+------------------------+------------------------+--------------------------+--------------------------+-----------------------------+-----------------------------+------------------------------+------------------------------+-----------------------+------------------------+-------------------+--------------------+-------------------------------+--------------------------------+

				// Get info info in 2 different steps (due to dm_db_* requires 'VIEW DATABASE PERFORMANCE STATE') which most users do not have
				List<ResultSetTableModel> extIxInfoList1 = Collections.emptyList();
				List<ResultSetTableModel> extIxInfoList2 = Collections.emptyList();
				
				boolean success_extIxInfoList1 = true;
				boolean success_extIxInfoList2 = true;

				String sql  = "";
				String sql1 = ""
						+ "DECLARE @dbid  int = COALESCE(db_id('" + catOrigin + "'), db_id()) \n"
						+ "DECLARE @objid int = OBJECT_ID('" + cat + schema + table + "') \n"
						+ "\n"
						+ "SELECT * FROM " + cat + "sys.indexes                   WHERE object_id = @objid       ORDER BY index_id \n"
						+ "SELECT * FROM " + cat + "sys.partitions                WHERE object_id = @objid       ORDER BY index_id \n"
						+ "SELECT "
						+ "     p.index_id \n"
						+ "    ,total_pages = sum(a.total_pages) \n"
						+ "    ,used_pages  = sum(a.used_pages) \n"
						+ "    ,data_pages  = sum(a.data_pages) \n"
						+ "FROM " + cat + "sys.partitions p \n"
						+ "INNER join " + cat + "sys.allocation_units a ON p.partition_id = a.container_id \n"
						+ "WHERE p.object_id = @objid \n"
						+ "GROUP BY p.index_id \n"
						+ "ORDER BY index_id \n"
						;                                                                             

				String sql2 = ""
						+ "DECLARE @dbid  int = COALESCE(db_id('" + catOrigin + "'), db_id()) \n"
						+ "DECLARE @objid int = OBJECT_ID('" + cat + schema + table + "') \n"
						+ "\n"
						+ "SELECT * FROM " + cat + "sys.dm_db_partition_stats     WHERE object_id = @objid       ORDER BY index_id \n"
						+ "SELECT * FROM " + cat + "sys.dm_db_index_usage_stats   WHERE object_id = @objid       ORDER BY index_id \n"
						+ "SELECT * FROM " + cat + "sys.dm_db_index_operational_stats(@dbid, @objid, null, null) ORDER BY index_id \n"
						;                                                                             

				sql = sql1; 
				try { extIxInfoList1 = DbUtils.exec(_conn, sql, 1); }
				catch (SQLException ex)
				{
					success_extIxInfoList1 = false;
					_logger.warn("Problems getting 'Table INDEX Extra Info, step 1'. ErrorCode=" + ex.getErrorCode() + ", SqlState=" + ex.getSQLState() + ", Message='" + ex.getMessage() + "'. sql=|" + sql + "|, Caught: " + ex);
				}

				sql = sql2; 
				try	{ extIxInfoList2 = DbUtils.exec(_conn, sql, 1); }
				catch (SQLException ex)
				{
					success_extIxInfoList2 = false;
					_logger.warn("Problems getting 'Table INDEX Extra Info, step 2'. ErrorCode=" + ex.getErrorCode() + ", SqlState=" + ex.getSQLState() + ", Message='" + ex.getMessage() + "'. sql=|" + sql + "|, Caught: " + ex);
				}

				ResultSetTableModel indexInfo                 = success_extIxInfoList1 ? extIxInfoList1.get(0) : ResultSetTableModel.createEmpty("indexInfo");
				ResultSetTableModel partitionInfo             = success_extIxInfoList1 ? extIxInfoList1.get(1) : ResultSetTableModel.createEmpty("partitionInfo");
				ResultSetTableModel partitionInfoSpace        = success_extIxInfoList1 ? extIxInfoList1.get(2) : ResultSetTableModel.createEmpty("partitionInfoSpace");

				ResultSetTableModel dmDbPartitionStats        = success_extIxInfoList2 ? extIxInfoList2.get(0) : ResultSetTableModel.createEmpty("dmDbPartitionStats");
				ResultSetTableModel dmDbIndexUsageStats       = success_extIxInfoList2 ? extIxInfoList2.get(1) : ResultSetTableModel.createEmpty("dmDbIndexUsageStats");
				ResultSetTableModel dmDbIndexOperationalStats = success_extIxInfoList2 ? extIxInfoList2.get(2) : ResultSetTableModel.createEmpty("dmDbIndexOperationalStats");

				// Return NULL (or default value) if column was not found...
				indexInfo                .setHandleColumnNotFoundAsNullValueInGetValues(true);
				partitionInfo            .setHandleColumnNotFoundAsNullValueInGetValues(true);
				partitionInfoSpace       .setHandleColumnNotFoundAsNullValueInGetValues(true);
				dmDbPartitionStats       .setHandleColumnNotFoundAsNullValueInGetValues(true);
				dmDbIndexUsageStats      .setHandleColumnNotFoundAsNullValueInGetValues(true);
				dmDbIndexOperationalStats.setHandleColumnNotFoundAsNullValueInGetValues(true);

//				// Return NULL (or default value) if row was OutOfBounds...
//				indexInfo                .setHandleRowOutOfBoundInGetValues(true);
//				partitionInfo            .setHandleRowOutOfBoundInGetValues(true);
//				partitionInfoSpace       .setHandleRowOutOfBoundInGetValues(true);
//				dmDbPartitionStats       .setHandleRowOutOfBoundInGetValues(true);
//				dmDbIndexUsageStats      .setHandleRowOutOfBoundInGetValues(true);
//				dmDbIndexOperationalStats.setHandleRowOutOfBoundInGetValues(true);

				
				Map<String, String> extIndexInfo = new HashMap<>(); // <indexName, description>
				Map<String, String> extIndexType = new HashMap<>(); // <indexName, indexType>

				List<Integer> rowIds;
				int indexIdRow;
				
				// Now loop the INDEX Part (which SHOULD be available for less authorized users as well)
				for (int r=0; r<indexInfo.getRowCount(); r++)
				{
					//----------------------------------------------------
					String  index_name                 = indexInfo.getValueAsString (r, "name");
					String  type_desc                  = indexInfo.getValueAsString (r, "type_desc");
					Integer index_id                   = indexInfo.getValueAsInteger(r, "index_id");
//					boolean is_unique                  = indexInfo.getValueAsBoolean(r, "is_unique");
					boolean ignore_dup_key             = indexInfo.getValueAsBoolean(r, "ignore_dup_key");
					boolean is_primary_key             = indexInfo.getValueAsBoolean(r, "is_primary_key");
					boolean is_unique_constraint       = indexInfo.getValueAsBoolean(r, "is_unique_constraint");
					Integer fill_factor                = indexInfo.getValueAsInteger(r, "fill_factor");
					boolean is_disabled                = indexInfo.getValueAsBoolean(r, "is_disabled");
					boolean has_filter                 = indexInfo.getValueAsBoolean(r, "has_filter");
					String  filter_definition          = indexInfo.getValueAsString (r, "filter_definition");

					if (index_id == 0)
					{
						index_name = "HEAP";
					}

					if (index_id != 0)
						indexCount++;
					
					//----------------------------------------------------
					rowIds = partitionInfo.getRowIdsWhere("index_id", index_id);
					indexIdRow = rowIds.isEmpty() ? -1 : rowIds.get(0);
//					if (rowIds.size() > 1)
//						_logger.warn("IndexExtraInfo: found more than one row in 'partitionInfo' for index_id=" + index_id + ", index_name='" + index_name + "'. Table/Index is probably Partitioned, which is NOT yet handled. Just using first entry.");

					String  data_compression_desc      = indexIdRow < 0 ? "UNKNOWN" : partitionInfo.getValueAsString(indexIdRow, "data_compression_desc");
					Integer row_count                  = indexIdRow < 0 ? -1 : partitionInfo.getValueAsIntegerRowIdsSum(rowIds, "rows");


					//----------------------------------------------------
					rowIds = partitionInfoSpace.getRowIdsWhere("index_id", index_id);
					indexIdRow = rowIds.isEmpty() ? -1 : rowIds.get(0);
//					if (rowIds.size() > 1)
//						_logger.warn("IndexExtraInfo: found more than one row in 'partitionInfoSpace' for index_id=" + index_id + ", index_name='" + index_name + "'. Table/Index is probably Partitioned, which is NOT yet handled. Just using first entry.");

					Integer   used_page_count          = indexIdRow < 0 ? -1 : partitionInfoSpace.getValueAsIntegerRowIdsSum(rowIds, "used_pages");
					Integer   reserved_page_count      = indexIdRow < 0 ? -1 : partitionInfoSpace.getValueAsIntegerRowIdsSum(rowIds, "total_pages");


					//----------------------------------------------------
					if (success_extIxInfoList2)
					{
						rowIds = dmDbPartitionStats.getRowIdsWhere("index_id", index_id);
						indexIdRow = rowIds.isEmpty() ? -1 : rowIds.get(0);
//						if (rowIds.size() > 1)
//							_logger.warn("IndexExtraInfo: found more than one row in 'dmDbPartitionStats' for index_id=" + index_id + ", index_name='" + index_name + "'. Table/Index is probably Partitioned, which is NOT yet handled. Just using first entry.");

						used_page_count          = indexIdRow < 0 ? -1 : dmDbPartitionStats.getValueAsIntegerRowIdsSum(rowIds, "used_page_count");
						reserved_page_count      = indexIdRow < 0 ? -1 : dmDbPartitionStats.getValueAsIntegerRowIdsSum(rowIds, "reserved_page_count");
//						row_count                = indexIdRow < 0 ? -1 : dmDbPartitionStats.getValueAsIntegerRowIdsSum(rowIds, "row_count");
					}


					//----------------------------------------------------
					rowIds = dmDbIndexUsageStats.getRowIdsWhere("index_id", index_id);
					indexIdRow = rowIds.isEmpty() ? -1 : rowIds.get(0);
//					if (rowIds.size() > 1)
//						_logger.warn("IndexExtraInfo: found more than one row in 'dmDbIndexUsageStats' for index_id=" + index_id + ", index_name='" + index_name + "'. Table/Index is probably Partitioned, which is NOT yet handled. Just using first entry.");

					Integer   user_seeks               = indexIdRow < 0 ? -1   : dmDbIndexUsageStats.getValueAsIntegerRowIdsSum  (rowIds, "user_seeks");
					Integer   user_scans               = indexIdRow < 0 ? -1   : dmDbIndexUsageStats.getValueAsIntegerRowIdsSum  (rowIds, "user_scans");
					Integer   user_lookups             = indexIdRow < 0 ? -1   : dmDbIndexUsageStats.getValueAsIntegerRowIdsSum  (rowIds, "user_lookups");
					Integer   user_updates             = indexIdRow < 0 ? -1   : dmDbIndexUsageStats.getValueAsIntegerRowIdsSum  (rowIds, "user_updates");
//					Timestamp last_user_seek           = indexIdRow < 0 ? null : dmDbIndexUsageStats.getValueAsTimestamp(indexIdRow, "last_user_seek");
//					Timestamp last_user_scan           = indexIdRow < 0 ? null : dmDbIndexUsageStats.getValueAsTimestamp(indexIdRow, "last_user_scan");
//					Timestamp last_user_lookup         = indexIdRow < 0 ? null : dmDbIndexUsageStats.getValueAsTimestamp(indexIdRow, "last_user_lookup");
					Timestamp last_user_update         = indexIdRow < 0 ? null : dmDbIndexUsageStats.getValueAsTimestamp(indexIdRow, "last_user_update");


					//----------------------------------------------------
					rowIds = dmDbIndexOperationalStats.getRowIdsWhere("index_id", index_id);
					indexIdRow = rowIds.isEmpty() ? -1 : rowIds.get(0);
//					if (rowIds.size() > 1)
//						_logger.warn("IndexExtraInfo: found more than one row in 'dmDbIndexOperationalStats' for index_id=" + index_id + ", index_name='" + index_name + "'. Table/Index is probably Partitioned, which is NOT yet handled. Just using first entry.");

					Integer   range_scan_count               = indexIdRow < 0 ? -1 : dmDbIndexOperationalStats.getValueAsIntegerRowIdsSum(rowIds, "range_scan_count");
					Integer   singleton_lookup_count         = indexIdRow < 0 ? -1 : dmDbIndexOperationalStats.getValueAsIntegerRowIdsSum(rowIds, "singleton_lookup_count");
					Integer   forwarded_fetch_count          = indexIdRow < 0 ? -1 : dmDbIndexOperationalStats.getValueAsIntegerRowIdsSum(rowIds, "forwarded_fetch_count");
					Integer   page_latch_wait_count          = indexIdRow < 0 ? -1 : dmDbIndexOperationalStats.getValueAsIntegerRowIdsSum(rowIds, "page_latch_wait_count");
					Integer   page_latch_wait_in_ms          = indexIdRow < 0 ? -1 : dmDbIndexOperationalStats.getValueAsIntegerRowIdsSum(rowIds, "page_latch_wait_in_ms");
					Integer   page_io_latch_wait_count       = indexIdRow < 0 ? -1 : dmDbIndexOperationalStats.getValueAsIntegerRowIdsSum(rowIds, "page_io_latch_wait_count");
					Integer   page_io_latch_wait_in_ms       = indexIdRow < 0 ? -1 : dmDbIndexOperationalStats.getValueAsIntegerRowIdsSum(rowIds, "page_io_latch_wait_in_ms");

					Integer   leaf_insert_count              = indexIdRow < 0 ? -1 : dmDbIndexOperationalStats.getValueAsIntegerRowIdsSum(rowIds, "leaf_insert_count");
					Integer   leaf_update_count              = indexIdRow < 0 ? -1 : dmDbIndexOperationalStats.getValueAsIntegerRowIdsSum(rowIds, "leaf_update_count");
					Integer   leaf_delete_count              = indexIdRow < 0 ? -1 : dmDbIndexOperationalStats.getValueAsIntegerRowIdsSum(rowIds, "leaf_delete_count");
					Integer   leaf_ghost_count               = indexIdRow < 0 ? -1 : dmDbIndexOperationalStats.getValueAsIntegerRowIdsSum(rowIds, "leaf_ghost_count");
					Integer   lob_fetch_in_pages             = indexIdRow < 0 ? -1 : dmDbIndexOperationalStats.getValueAsIntegerRowIdsSum(rowIds, "lob_fetch_in_pages");
					Integer   row_overflow_fetch_in_pages    = indexIdRow < 0 ? -1 : dmDbIndexOperationalStats.getValueAsIntegerRowIdsSum(rowIds, "row_overflow_fetch_in_pages");
					Integer   row_lock_wait_count            = indexIdRow < 0 ? -1 : dmDbIndexOperationalStats.getValueAsIntegerRowIdsSum(rowIds, "row_lock_wait_count");
					Integer   row_lock_wait_in_ms            = indexIdRow < 0 ? -1 : dmDbIndexOperationalStats.getValueAsIntegerRowIdsSum(rowIds, "row_lock_wait_in_ms");
					Integer   page_lock_wait_count           = indexIdRow < 0 ? -1 : dmDbIndexOperationalStats.getValueAsIntegerRowIdsSum(rowIds, "page_lock_wait_count");
					Integer   page_lock_wait_in_ms           = indexIdRow < 0 ? -1 : dmDbIndexOperationalStats.getValueAsIntegerRowIdsSum(rowIds, "page_lock_wait_in_ms");
					Integer   page_compression_attempt_count = indexIdRow < 0 ? -1 : dmDbIndexOperationalStats.getValueAsIntegerRowIdsSum(rowIds, "page_compression_attempt_count");
					Integer   page_compression_success_count = indexIdRow < 0 ? -1 : dmDbIndexOperationalStats.getValueAsIntegerRowIdsSum(rowIds, "page_compression_success_count");

					String ixDesc = "<BR>";
					List<String> prefixes = new ArrayList<>();
					
					if (is_disabled)                            prefixes.add("<FONT color='red'>DISABLED</FONT>");
					if ( ! StringUtil.equalsAny(type_desc, "CLUSTERED", "NONCLUSTERED") ) prefixes.add("type=" + type_desc);
					if (ignore_dup_key)                         prefixes.add("IGNORE_DUP_KEY");
					if (is_primary_key)                         prefixes.add("PRIMARY KEY");
					if (is_unique_constraint)                   prefixes.add("UNIQUE CONSTRAINT");
					if (fill_factor != null && fill_factor > 0) prefixes.add("FILLFACTOR=" + fill_factor);
					if (has_filter)                             prefixes.add("FILTER='" + filter_definition + "'");

					if ( ! prefixes.isEmpty() )
						ixDesc += StringUtil.toCommaStr(prefixes) + ", ";
					
					int freeMb = (reserved_page_count - used_page_count) / 128;

					ixDesc += "SizeMb="          + (reserved_page_count / 128);
//					ixDesc += ", UsedMb="        + (used_page_count / 128);
					if (freeMb > 0)
						ixDesc += ", FreeMb="    + freeMb;
					ixDesc += ", Rows="          + row_count;
					ixDesc += ", IndexId="       + index_id;
					ixDesc += ", Compression="   + data_compression_desc;
					if (getExtendedIndexInfoOperationalDetails && success_extIxInfoList2)
					{
						if (dmDbIndexUsageStats.hasRows())
						{
							String unused = "";
							if (user_seeks == 0 && user_scans == 0 && user_lookups == 0)
								unused = "<FONT color='red'>UNUSED</FONT>: </b>";

							ixDesc += "<BR>Usage[" + unused + "seeks=" + user_seeks + ", scans=" + user_scans + ", lookups=" + user_lookups + ", ins_upd_del=" + user_updates + ", last_ins_upd_del=" + last_user_update + ", srvUptime=" + srvUptime + "]";
						}
						else
						{
							ixDesc += "<BR>Usage[no-info]";
						}

						if (dmDbIndexOperationalStats.hasRows())
						{
							ixDesc += "<BR>OP-READ[range_scan=" + range_scan_count + ", singleton_lookup=" + singleton_lookup_count + ", forwarded_fetch=" + forwarded_fetch_count + "]";
							ixDesc += "<BR>OP-IUD[leaf_insert_count=" + leaf_insert_count + ", leaf_update_count=" + leaf_update_count + ", leaf_delete_count=" + leaf_delete_count + ", leaf_ghost_count=" + leaf_ghost_count + "]";

							if (index_id == 0)
							{
								if (lob_fetch_in_pages > 0 || row_overflow_fetch_in_pages > 0)
								{
									ixDesc += "<BR>OP-LOB[lob_fetch_in_pages=" + lob_fetch_in_pages + ", row_overflow_fetch_in_pages=" + row_overflow_fetch_in_pages + "]";
								}
							}

							if (page_latch_wait_count > 0)
							{
								double avgMsPerWait = 0.0;
								avgMsPerWait = MathUtils.round((page_latch_wait_in_ms*1.0) / (page_latch_wait_count*1.0), 1);
								ixDesc += "<BR>OP-LATCH[page_latch_wait_count=" + page_latch_wait_count + ", page_latch_wait_in_ms=" + page_latch_wait_in_ms + ", avgMsPerWait=" + avgMsPerWait + "]";
							}

							if (row_lock_wait_count > 0 || page_lock_wait_count > 0)
							{
								ixDesc += "<BR>OP-LCK[row_lock_wait_count=" + row_lock_wait_count + ", row_lock_wait_in_ms=" + row_lock_wait_in_ms + ", page_lock_wait_count=" + page_lock_wait_count + ", page_lock_wait_in_ms=" + page_lock_wait_in_ms + "]";
							}

							if (page_io_latch_wait_count > 0)
							{
								double msPerIo = 0.0;
								msPerIo = MathUtils.round((page_io_latch_wait_in_ms*1.0) / (page_io_latch_wait_count*1.0), 1);
								ixDesc += "<BR>OP-IO[page_io_latch_wait_count=" + page_io_latch_wait_count + ", page_io_latch_wait_in_ms=" + page_io_latch_wait_in_ms + ", avgMsPerIo=" + msPerIo + "]";
							}
							
							if ("PAGE".equals(data_compression_desc) || "ROW".equals(data_compression_desc))
							{
								if (page_compression_attempt_count > 0)
								{
									double pct = 0;
									if (page_compression_attempt_count > 0)
										pct = MathUtils.round((((page_compression_success_count*1.0)/(page_compression_attempt_count*1.0))*100.0), 1);
									ixDesc += "<BR>OP-COMP[page_compression_attempt_count=" + page_compression_attempt_count + ", page_compression_success_count=" + page_compression_success_count + ", success_pct=" + pct + "]";
								}
							}
						}
						else
						{
							ixDesc += "<BR>OP[no-info]";
						}
					}
					else
					{
						ixDesc += "<BR>Usage[not-authorized]";
					}
					

//System.out.println("extIndexInfo -- index_name=" + index_name + ", ixDesc   =" + ixDesc);
//System.out.println("extIndexType -- index_name=" + index_name + ", type_desc=" + type_desc);
					extIndexInfo.put(index_name, ixDesc);
					extIndexType.put(index_name, type_desc);
				}
				
				// ADD INFO
				extraInfo.put(TableExtraInfo.IndexExtraInfoDescription, new TableExtraInfo(TableExtraInfo.IndexExtraInfoDescription, "IndexInfoDescription", extIndexInfo, "extended Index Description Information", null));
				extraInfo.put(TableExtraInfo.IndexType                , new TableExtraInfo(TableExtraInfo.IndexType                , "IndexType"           , extIndexType, "Index Type"                            , null));
			}
			else
			{
				String sql = "exec " + cat + ".sp_helpindex '" + schema + table + "'";
				try
				{
					List<ResultSetTableModel> rstmList = DbUtils.exec(_conn, sql, 2);
					
					if (rstmList.size() >= 1)
					{
						ResultSetTableModel indexInfo = rstmList.get(0);
						
						Map<String, String> extIndexInfo = new HashMap<>(); // <indexName, description>
						for (int r=0; r<indexInfo.getRowCount(); r++)
						{
							String indexName        = indexInfo.getValueAsString(r, "index_name",        false, "");
						//	String indexKeys        = indexInfo.getValueAsString(r, "index_keys",        false, "");
							String indexDescription = indexInfo.getValueAsString(r, "index_description", false, "");
							
							// SYBASE also has: index_max_rows_per_page, index_fillfactor, index_reservepagegap, index_created, index_local
							// But we do not read that... for the moment!

							extIndexInfo.put(indexName, "Desc=["+indexDescription+"]");
						}

						// ADD INFO
						extraInfo.put(TableExtraInfo.IndexExtraInfoDescription, new TableExtraInfo(TableExtraInfo.IndexExtraInfoDescription, "IndexInfoDescription", extIndexInfo, "extended Index Description Information", null));
					}
				}
				catch (SQLException ex)
				{
					_logger.error("getTableExtraInfo(): Problems executing sql '"+sql+"'. Caught="+ex);
					if (_logger.isDebugEnabled())
						_logger.debug("getTableExtraInfo(): Problems executing sql '"+sql+"'. Caught="+ex, ex);
				}
			}
		}
		
		boolean getIndexInclude = true;
		if (getIndexInclude)
		{
			String sql = ""
				    + "SELECT i.name as IndexName, c.name as ColumnName \n"
				    + "FROM       " + cat + "sys.indexes        i \n"
				    + "INNER JOIN " + cat + "sys.index_columns ic ON ic.object_id = i .object_id AND ic.index_id  = i .index_id \n"
				    + "INNER JOIN " + cat + "sys.columns        c ON c .object_id = ic.object_id AND c .column_id = ic.column_id \n"
				    + "WHERE i.object_id = object_id('" + cat + schema + table + "') \n"
				    + "  AND ic.is_included_column = 1 \n"
				    + "  AND  i.type_desc NOT LIKE '%COLUMNSTORE%' \n"
				    + "ORDER BY ic.index_id, ic.index_column_id \n"
				    + "";
			try
			{
				List<ResultSetTableModel> rstmList = DbUtils.exec(_conn, sql, 2);
				
				if (rstmList.size() >= 1)
				{
					ResultSetTableModel indexInfo = rstmList.get(0);
					
					Map<String, String> extIndexInfo = new HashMap<>(); // <indexName, colName(s)>
				//	Map<String, List<String>> extIndexInfo = new HashMap<>(); // <indexName, colName(s)> // Maybe we can have this as a List instead of a CSV String...
					for (int r=0; r<indexInfo.getRowCount(); r++)
					{
						String indexName   = indexInfo.getValueAsString(r, "IndexName",  false, "");
						String indexColumn = indexInfo.getValueAsString(r, "ColumnName", false, "");

						String includeColumns = extIndexInfo.get(indexName);
						if (includeColumns == null)
							includeColumns = indexColumn;
						else
							includeColumns += ", " + indexColumn;

						extIndexInfo.put(indexName, includeColumns);
					}

					// ADD INFO
					extraInfo.put(TableExtraInfo.IndexIncludeColumns, new TableExtraInfo(TableExtraInfo.IndexIncludeColumns, "IndexIncludeColumns", extIndexInfo, "Index Include Columns", null));
				}
			}
			catch (SQLException ex)
			{
				_logger.error("getTableExtraInfo(): Problems executing sql '"+sql+"'. Caught="+ex);
				if (_logger.isDebugEnabled())
					_logger.debug("getTableExtraInfo(): Problems executing sql '"+sql+"'. Caught="+ex, ex);
			}
		}
		
//		boolean useSpSpaceused = Configuration.getCombinedConfiguration().getBooleanProperty(PROPKEY_getTableExtraInfo_useSpSpaceused, DEFAULT_getTableExtraInfo_useSpSpaceused);
//		boolean useSpSpaceused = false;
//		if (useSpSpaceused)
//		{
//			String sql = "exec "+cat+".sp_spaceused '" + schema + table + "'"; 
//			try
//			{
//				List<ResultSetTableModel> rstmList = DbUtils.exec(_conn, sql, 2);
//				ResultSetTableModel tableInfo = rstmList.get(0);
//
//				NumberFormat nf = NumberFormat.getInstance();
//
//				// Get Table Info
//				int rowtotal = StringUtil.parseInt( tableInfo.getValueAsString(0, "rows"      , true, ""), 0);
//				int reserved = StringUtil.parseInt( tableInfo.getValueAsString(0, "reserved"  , true, "").replace(" KB", ""), 0);
//				int data     = StringUtil.parseInt( tableInfo.getValueAsString(0, "data"      , true, "").replace(" KB", ""), 0);
//				int index    = StringUtil.parseInt( tableInfo.getValueAsString(0, "index_size", true, "").replace(" KB", ""), 0);
//				int unused   = StringUtil.parseInt( tableInfo.getValueAsString(0, "unused"    , true, "").replace(" KB", ""), 0);		
//
//				// ADD INFO
//				extraInfo.put(TableExtraInfo.TableRowCount,      new TableExtraInfo(TableExtraInfo.TableRowCount,      "Row Count",        rowtotal    , "Number of rows in the table. Note: exec dbname..sp_spaceused 'schema.tabname'", null));
//				extraInfo.put(TableExtraInfo.TableTotalSizeInKb, new TableExtraInfo(TableExtraInfo.TableTotalSizeInKb, "Total Size In KB", data+index  , "Details from sp_spaceused: reserved="+nf.format(reserved)+" KB, data="+nf.format(data)+" KB, index_size="+nf.format(index)+" KB, unused="+nf.format(unused)+" KB", null));
//				extraInfo.put(TableExtraInfo.TableDataSizeInKb,  new TableExtraInfo(TableExtraInfo.TableDataSizeInKb,  "Data Size In KB",  data        , "From 'sp_spaceued', column 'data'.", null));
//				extraInfo.put(TableExtraInfo.TableIndexSizeInKb, new TableExtraInfo(TableExtraInfo.TableIndexSizeInKb, "Index Size In KB", index       , "From 'sp_spaceued', column 'index_size'.", null));
//				//extraInfo.put(TableExtraInfo.TableLobSizeInKb,   new TableExtraInfo(TableExtraInfo.TableLobSizeInKb,   "LOB Size In KB",   sumLobSize  , "From 'sp_spaceued', index section, 'size' of columns name 't"+table+"'. Details: size="+nf.format(sumLobSize)+" KB, reserved="+nf.format(sumLobReserved)+" KB, unused="+nf.format(sumLobUnused)+" KB", null));
//			}
//			catch (SQLException ex)
//			{
//				_logger.error("getTableExtraInfo(): Problems executing sql '"+sql+"'. Caught="+ex);
//				if (_logger.isDebugEnabled())
//					_logger.debug("getTableExtraInfo(): Problems executing sql '"+sql+"'. Caught="+ex, ex);
//			}
//		}
//		else
		{
//			String sql = "select row_count(db_id('"+cat+"'), object_id('"+schema+"."+table+"'))";
//			String sql = "SELECT SUM(row_count) as RowCnt, count(*) as partitionCnt \n"
//					+ "FROM " + cat + "sys.dm_db_partition_stats \n"
//					+ "WHERE object_id=OBJECT_ID('" + cat + schema + table + "') \n"
//					+ "AND (index_id=0 or index_id=1)";
			String sql = ""
				    + "SELECT \n"
				    + "     'DATA'                                   as type \n"
				    + "    ,SUM(in_row_data_page_count)*8            as in_row_data_kb \n"
				    + "    ,SUM(in_row_used_page_count)*8            as in_row_used_kb \n"
				    + "    ,SUM(in_row_reserved_page_count)*8        as in_row_reserved_kb \n"
				    + " \n"
				    + "    ,SUM(lob_used_page_count)*8               as lob_used_kb \n"
				    + "    ,SUM(lob_reserved_page_count)*8           as lob_reserved_kb \n"
				    + " \n"
				    + "    ,SUM(row_overflow_used_page_count)*8      as row_overflow_used_kb \n"
				    + "    ,SUM(row_overflow_reserved_page_count)*8  as row_overflow_reserved_kb \n"
				    + " \n"
				    + "    ,SUM(used_page_count)*8                   as used_kb \n"
				    + "    ,SUM(reserved_page_count)*8               as reserved_kb \n"
				    + " \n"
				    + "    ,SUM(row_count)                           as row_cnt \n"
				    + "    ,count(*)                                 as partition_cnt \n"
					+ "FROM " + cat + "sys.dm_db_partition_stats \n"
					+ "WHERE object_id=OBJECT_ID('" + cat + schema + table + "') \n"
				    + "AND (index_id=0 or index_id=1) \n"

				    + "UNION ALL \n"

				    + "SELECT \n"
				    + "     'INDEX'                                  as type \n"
				    + "    ,SUM(in_row_data_page_count)*8            as in_row_data_kb \n"
				    + "    ,SUM(in_row_used_page_count)*8            as in_row_used_kb \n"
				    + "    ,SUM(in_row_reserved_page_count)*8        as in_row_reserved_kb \n"
				    + " \n"
				    + "    ,SUM(lob_used_page_count)*8               as lob_used_kb \n"
				    + "    ,SUM(lob_reserved_page_count)*8           as lob_reserved_kb \n"
				    + " \n"
				    + "    ,SUM(row_overflow_used_page_count)*8      as row_overflow_used_kb \n"
				    + "    ,SUM(row_overflow_reserved_page_count)*8  as row_overflow_reserved_kb \n"
				    + " \n"
				    + "    ,SUM(used_page_count)*8                   as used_kb \n"
				    + "    ,SUM(reserved_page_count)*8               as reserved_kb \n"
				    + " \n"
				    + "    ,SUM(row_count)                           as row_cnt \n"
				    + "    ,count(*)                                 as partition_cnt \n"
				    + "FROM sys.dm_db_partition_stats \n"
					+ "WHERE object_id=OBJECT_ID('" + cat + schema + table + "') \n"
				    + "AND index_id >= 2 \n"
				    + "";

			try (Statement stmnt = _conn.createStatement(); ResultSet rs = stmnt.executeQuery(sql);)
			{
				String type; // DATA or INDEX
				long in_row_data_kb           = 0;
				long in_row_used_kb           = 0;
				long in_row_reserved_kb       = 0;

				long lob_used_kb              = 0;
				long lob_reserved_kb          = 0;

				long row_overflow_used_kb     = 0;
				long row_overflow_reserved_kb = 0;

				long used_kb                  = 0;
				long reserved_kb              = 0;
				long index_used_kb            = 0;
				long index_reserved_kb        = 0;

				long row_cnt                  = 0;
				long partition_cnt            = 0;

				while(rs.next())
				{
					type = rs.getString(1);
					if ("DATA".equals(type))
					{
						in_row_data_kb           = rs.getLong(2);
						in_row_used_kb           = rs.getLong(3);
						in_row_reserved_kb       = rs.getLong(4);

						lob_used_kb              = rs.getLong(5);
						lob_reserved_kb          = rs.getLong(6);

						row_overflow_used_kb     = rs.getLong(7);
						row_overflow_reserved_kb = rs.getLong(8);

						used_kb                  = rs.getLong(9);
						reserved_kb              = rs.getLong(10);

						row_cnt                  = rs.getLong(11);
						partition_cnt            = rs.getLong(12);
					}
					if ("INDEX".equals(type))
					{
						index_used_kb            = rs.getLong(9);
						index_reserved_kb        = rs.getLong(10);
					}
				}

				long in_row_data_unused_kb      = in_row_reserved_kb       - in_row_used_kb;
				long lob_unused_kb              = lob_reserved_kb          - lob_used_kb;
				long row_overflow_unused_kb     = row_overflow_reserved_kb - row_overflow_used_kb;
				long data_unused_kb             = reserved_kb              - used_kb;
				long index_unused_kb            = index_reserved_kb        - index_used_kb;
				long total_unused_kb            = data_unused_kb + index_unused_kb;

				NumberFormat nf = NumberFormat.getInstance();
				
				extraInfo.put(TableExtraInfo.TableRowCount,             new TableExtraInfo(TableExtraInfo.TableRowCount,            "Row Count",          row_cnt                         , "Number of rows in the table. From: dm_db_partition_stats", null));
				extraInfo.put(TableExtraInfo.TableTotalSizeInKb,        new TableExtraInfo(TableExtraInfo.TableTotalSizeInKb,       "Total Size In KB",   reserved_kb + index_reserved_kb , "Details: reserved_kb=" + nf.format(reserved_kb) + " + index_reserved_kb=" + nf.format(index_reserved_kb) + ", total_unused_kb=" + nf.format(total_unused_kb) + ".", null));
				extraInfo.put(TableExtraInfo.TableDataSizeInKb,         new TableExtraInfo(TableExtraInfo.TableDataSizeInKb,        "Data Size In KB",    reserved_kb                     , "Details: used_kb=" + nf.format(used_kb) + ", reserved_kb=" + nf.format(reserved_kb) + ", data_unused_kb=" + nf.format(data_unused_kb) + "", null));
				extraInfo.put(TableExtraInfo.TableInRowSizeInKb,        new TableExtraInfo(TableExtraInfo.TableInRowSizeInKb,       "In-Row Data In KB",  in_row_reserved_kb              , "Details: in_row_data_kb=" + nf.format(in_row_data_kb) + ", in_row_used_kb=" + nf.format(in_row_used_kb) + ", in_row_reserved_kb=" + nf.format(in_row_reserved_kb) + ", in_row_data_unused_kb=" + nf.format(in_row_data_unused_kb) + ".", null));
				extraInfo.put(TableExtraInfo.TableOverflowRowSizeInKb,  new TableExtraInfo(TableExtraInfo.TableOverflowRowSizeInKb, "Row Overflow In KB", row_overflow_reserved_kb        , "Details: row_overflow_used_kb=" + nf.format(row_overflow_used_kb) + ", row_overflow_reserved_kb=" + nf.format(row_overflow_unused_kb) + ", index_unused_kb=" + nf.format(row_overflow_unused_kb) + ".", null));
				extraInfo.put(TableExtraInfo.TableLobSizeInKb,          new TableExtraInfo(TableExtraInfo.TableLobSizeInKb,         "LOB Size In KB",     lob_reserved_kb                 , "Details: lob_used_kb=" + nf.format(lob_used_kb) + ", lob_reserved_kb=" + nf.format(lob_reserved_kb) + ", lob_unused_kb=" + nf.format(lob_unused_kb) + ".", null));
				extraInfo.put(TableExtraInfo.TableIndexSizeInKb,        new TableExtraInfo(TableExtraInfo.TableIndexSizeInKb,       "Index Size In KB",   index_reserved_kb               , "Details: index_used_kb=" + nf.format(index_used_kb) + ", index_reserved_kb=" + nf.format(index_reserved_kb) + ", index_unused_kb=" + nf.format(index_unused_kb) + ".", null));
				extraInfo.put(TableExtraInfo.TableIndexCount,           new TableExtraInfo(TableExtraInfo.TableIndexCount,          "Index Count",        indexCount                      , "Number of indexes on the table", null));
				extraInfo.put(TableExtraInfo.TablePartitionCount,       new TableExtraInfo(TableExtraInfo.TablePartitionCount,      "Partition Count",    partition_cnt                   , "Number of table partition(s). From: dm_db_partition_stats", null));
			}
			catch (SQLException ex)
			{
				_logger.error("getTableExtraInfo(): Problems executing sql '"+sql+"'. Caught="+ex);
				if (_logger.isDebugEnabled())
					_logger.debug("getTableExtraInfo(): Problems executing sql '"+sql+"'. Caught="+ex, ex);

				// IF 'dm_db_partition_stats' FAILS, try with 'sp_spaceused'
				sql = "exec "+cat+".sp_spaceused '" + schema + table + "'"; 
				try
				{
					List<ResultSetTableModel> rstmList = DbUtils.exec(_conn, sql, 2);
					ResultSetTableModel tableInfo = rstmList.get(0);

					NumberFormat nf = NumberFormat.getInstance();

					// Get Table Info
					int rowtotal = StringUtil.parseInt( tableInfo.getValueAsString(0, "rows"      , true, ""), 0);
					int reserved = StringUtil.parseInt( tableInfo.getValueAsString(0, "reserved"  , true, "").replace(" KB", ""), 0);
					int data     = StringUtil.parseInt( tableInfo.getValueAsString(0, "data"      , true, "").replace(" KB", ""), 0);
					int index    = StringUtil.parseInt( tableInfo.getValueAsString(0, "index_size", true, "").replace(" KB", ""), 0);
					int unused   = StringUtil.parseInt( tableInfo.getValueAsString(0, "unused"    , true, "").replace(" KB", ""), 0);		

					// ADD INFO
					extraInfo.put(TableExtraInfo.TableRowCount,      new TableExtraInfo(TableExtraInfo.TableRowCount,      "Row Count",        rowtotal    , "Number of rows in the table. Note: exec dbname..sp_spaceused 'schema.tabname'", null));
					extraInfo.put(TableExtraInfo.TableTotalSizeInKb, new TableExtraInfo(TableExtraInfo.TableTotalSizeInKb, "Total Size In KB", data+index  , "Details from sp_spaceused: reserved="+nf.format(reserved)+" KB, data="+nf.format(data)+" KB, index_size="+nf.format(index)+" KB, unused="+nf.format(unused)+" KB", null));
					extraInfo.put(TableExtraInfo.TableDataSizeInKb,  new TableExtraInfo(TableExtraInfo.TableDataSizeInKb,  "Data Size In KB",  data        , "From 'sp_spaceued', column 'data'.", null));
					extraInfo.put(TableExtraInfo.TableIndexSizeInKb, new TableExtraInfo(TableExtraInfo.TableIndexSizeInKb, "Index Size In KB", index       , "From 'sp_spaceued', column 'index_size'.", null));
					extraInfo.put(TableExtraInfo.TableIndexCount,    new TableExtraInfo(TableExtraInfo.TableIndexCount,    "Index Count",      indexCount  , "Number of indexes on the table", null));
					//extraInfo.put(TableExtraInfo.TableLobSizeInKb,   new TableExtraInfo(TableExtraInfo.TableLobSizeInKb,   "LOB Size In KB",   sumLobSize  , "From 'sp_spaceued', index section, 'size' of columns name 't"+table+"'. Details: size="+nf.format(sumLobSize)+" KB, reserved="+nf.format(sumLobReserved)+" KB, unused="+nf.format(sumLobUnused)+" KB", null));
				}
				catch (SQLException ex2)
				{
					_logger.error("getTableExtraInfo(): Problems executing sql '"+sql+"'. Caught="+ex2);
					if (_logger.isDebugEnabled())
						_logger.debug("getTableExtraInfo(): Problems executing sql '"+sql+"'. Caught="+ex2, ex2);
				}
			}
		}
		
		return extraInfo;
	}

	@Override
	public long getRowCountEstimate(String catalog, String schema, String table)
	throws SQLException
	{
		long rowCount = -1;
		
		catalog = StringUtil.isNullOrBlank(catalog) ? "" : catalog + ".";
		schema  = StringUtil.isNullOrBlank(schema)  ? "" : schema  + ".";

		String sql = "SELECT SUM(row_count) as RowCnt, count(*) as partitionCnt \n"
				+ "FROM " + catalog + "sys.dm_db_partition_stats \n"
				+ "WHERE object_id=OBJECT_ID('" + catalog + schema + table + "') \n"
				+ "AND (index_id=0 or index_id=1)";
		
		try (Statement stmnt = this.createStatement(); ResultSet rs = stmnt.executeQuery(sql))
		{
			while(rs.next())
				rowCount = rs.getLong(1);
		}
		
		return rowCount;
	}

	@Override
	public List<String> getViewReferences(String cat, String schema, String viewName)
	{
		Set<String> set = new LinkedHashSet<>();

		cat    = StringUtil.isNullOrBlank(cat)    ? "" : cat    + ".dbo.";
		schema = StringUtil.isNullOrBlank(schema) ? "" : schema + ".";

		String sql = "exec " + cat + "sp_depends '" + schema + viewName + "'";
		try
		{
			Statement stmnt = _conn.createStatement();
			ResultSet rs = stmnt.executeQuery(sql);
			while(rs.next())
			{
				String object = rs.getString(1);
				String type   = rs.getString(2);
				
				set.add(type + " - " + object);
			}
			rs.close();
			stmnt.close();
		}
		catch (SQLException ex)
		{
			_logger.error("getViewReferences(): Problems executing sql '"+sql+"'. Caught="+ex);
			if (_logger.isDebugEnabled())
				_logger.debug("getViewReferences(): Problems executing sql '"+sql+"'. Caught="+ex, ex);
		}

		return new ArrayList<>(set);
	}

//	@Override
//	public String getColumnTypeName(ResultSetMetaData rsmd, int col)
//	{
//		String columnTypeName = "-unknown-";
//
//		try
//		{
//			columnTypeName = rsmd.getColumnTypeName(col);
//			int columnType = rsmd.getColumnType(col);
//
//			if (    columnType == java.sql.Types.NUMERIC 
//			     || columnType == java.sql.Types.DECIMAL )
//			{
//				int precision = rsmd.getPrecision(col);
//				int scale     = rsmd.getScale(col);
//				
//				columnTypeName += "("+precision+","+scale+")";
//			}
//			if (    columnType == java.sql.Types.CHAR 
//			     || columnType == java.sql.Types.VARCHAR 
//			     || columnType == java.sql.Types.NCHAR
//			     || columnType == java.sql.Types.NVARCHAR
//			     || columnType == java.sql.Types.BINARY
//			     || columnType == java.sql.Types.VARBINARY
//			   )
//			{
//				int columnDisplaySize = Math.max(rsmd.getColumnDisplaySize(col), rsmd.getPrecision(col));
//					
//				columnTypeName += (columnDisplaySize == 2147483647) ? "(max)" : "("+columnDisplaySize+")";
//			}
//		}
//		catch (SQLException ignore) 
//		{
//		}
//
//		return columnTypeName;
//	}

	@Override
	protected int getDbmsSessionId_impl() throws SQLException
	{
		String sql = "select @@spid";
		
		int spid = -1;
		try (Statement stmnt = _conn.createStatement(); ResultSet rs = stmnt.executeQuery(sql))
		{
			while(rs.next())
				spid = rs.getInt(1);
		}
		
		return spid;
	}
}
