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
package com.asetune.sql.conn;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.text.NumberFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.commons.lang3.StringUtils;
import org.apache.log4j.Logger;

import com.asetune.gui.ResultSetTableModel;
import com.asetune.sql.conn.info.DbmsVersionInfo;
import com.asetune.sql.conn.info.DbmsVersionInfoSqlServer;
import com.asetune.sql.conn.info.DbxConnectionStateInfo;
import com.asetune.sql.conn.info.DbxConnectionStateInfoSqlServer;
import com.asetune.ui.autocomplete.completions.TableExtraInfo;
import com.asetune.utils.Configuration;
import com.asetune.utils.DbUtils;
import com.asetune.utils.StringUtil;
import com.asetune.utils.Ver;

public class SqlServerConnection 
extends DbxConnection
{
	private static Logger _logger = Logger.getLogger(SqlServerConnection.class);

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
								SimpleDateFormat sdf = new SimpleDateFormat("MMM dd yyyy HH:mm:ss");
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

//	protected JtdsMessageHandler _oldMsgHandlerJtds = null;
//	protected SQLServerMessageHandler _oldMsgHandlerSqlServer = null;

//	public void setMessageHandler(SQLServerMessageHandler messageHandler)
//	{
//		if (_conn instanceof SQLServerConnection)
//		{
//    		// Save the current message handler so we can restore it later
//			_oldMsgHandlerSqlServer = ((SQLServerConnection)_conn).getSQLServerMessageHandler();
//    
//    		((SQLServerConnection)_conn).setSQLServerMessageHandler(messageHandler);
//		}
//	}

//	public void setMessageHandler(JtdsMessageHandler messageHandler)
//	{
//		if (_conn instanceof JtdsConnection)
//		{
//    		// Save the current message handler so we can restore it later
//			_oldMsgHandlerJtds = ((JtdsConnection)_conn).getJtdsMessageHandler();
//    
//    		((JtdsConnection)_conn).setJtdsMessageHandler(messageHandler);
//		}
//	}

	public void restoreMessageHandler()
	{
//		if (_conn instanceof JtdsConnection)
//		{
//			((JtdsConnection)_conn).setJtdsMessageHandler(_oldMsgHandlerJtds);
//		}
//		if (_conn instanceof SQLServerConnection)
//		{
//			((SQLServerConnection)_conn).setSQLServerMessageHandler(_oldMsgHandlerSqlServer);
//		}
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
		LinkedHashMap<String, TableExtraInfo> extraInfo = new LinkedHashMap<>();

		cat    = StringUtil.isNullOrBlank(cat)    ? "" : cat    + ".";
		schema = StringUtil.isNullOrBlank(schema) ? "" : schema + ".";

		boolean getIndexTypeInfo = Configuration.getCombinedConfiguration().getBooleanProperty(PROPKEY_getTableExtraInfo_getIndexTypeInfo, DEFAULT_getTableExtraInfo_getIndexTypeInfo);
		if (getIndexTypeInfo)
		{
			String sql = "exec "+cat+".sp_helpindex '" + schema + table + "'";
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
		
		boolean useSpSpaceused = Configuration.getCombinedConfiguration().getBooleanProperty(PROPKEY_getTableExtraInfo_useSpSpaceused, DEFAULT_getTableExtraInfo_useSpSpaceused);
		if (useSpSpaceused)
		{
			String sql = "exec "+cat+".sp_spaceused '" + schema + table + "'"; 
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
				//extraInfo.put(TableExtraInfo.TableLobSizeInKb,   new TableExtraInfo(TableExtraInfo.TableLobSizeInKb,   "LOB Size In KB",   sumLobSize  , "From 'sp_spaceued', index section, 'size' of columns name 't"+table+"'. Details: size="+nf.format(sumLobSize)+" KB, reserved="+nf.format(sumLobReserved)+" KB, unused="+nf.format(sumLobUnused)+" KB", null));
			}
			catch (SQLException ex)
			{
				_logger.error("getTableExtraInfo(): Problems executing sql '"+sql+"'. Caught="+ex);
				if (_logger.isDebugEnabled())
					_logger.debug("getTableExtraInfo(): Problems executing sql '"+sql+"'. Caught="+ex, ex);
			}
		}
		else
		{
//			String sql = "select row_count(db_id('"+cat+"'), object_id('"+schema+"."+table+"'))";
			String sql = "SELECT SUM(row_count) as RowCnt, count(*) as partitionCnt \n"
					+ "FROM " + cat + "sys.dm_db_partition_stats \n"
					+ "WHERE object_id=OBJECT_ID('" + cat + schema + table + "') \n"
					+ "AND (index_id=0 or index_id=1)";
			try
			{
				Statement stmnt = _conn.createStatement();
				ResultSet rs = stmnt.executeQuery(sql);
				while(rs.next())
				{
					extraInfo.put(TableExtraInfo.TableRowCount,       new TableExtraInfo(TableExtraInfo.TableRowCount,       "Row Count",       rs.getLong(1), "Number of rows in the table. Note: fetched from statistics using DMV 'dm_db_partition_stats'", null));
					extraInfo.put(TableExtraInfo.TablePartitionCount, new TableExtraInfo(TableExtraInfo.TablePartitionCount, "Partition Count", rs.getLong(2), "Number of table partition(s). Note: fetched from statistics using DMV 'dm_db_partition_stats'", null));
				}
				rs.close();
			}
			catch (SQLException ex)
			{
				_logger.error("getTableExtraInfo(): Problems executing sql '"+sql+"'. Caught="+ex);
				if (_logger.isDebugEnabled())
					_logger.debug("getTableExtraInfo(): Problems executing sql '"+sql+"'. Caught="+ex, ex);
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
