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
package com.asetune.sql.conn;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import org.apache.log4j.Logger;

import com.asetune.gui.ResultSetTableModel;
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

	public SqlServerConnection(Connection conn)
	{
		super(conn);
		Ver.majorVersion_mustBeTenOrAbove = false;
//System.out.println("constructor::SqlServerConnection(conn): conn="+conn);
	}

	// Cached values
	private List<String> _getActiveServerRolesOrPermissions = null;

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
	
	@Override
	public long getDbmsVersionNumber()
	{
		long srvVersionNum = 0;

		// version
		try
		{
			String versionStr = "";

			versionStr    = getDbmsVersionStr();
		//	srvVersionNum = VersionSqlServer.parseVersionStringToNumber(versionStr);
			srvVersionNum = Ver.sqlServerVersionStringToNumber(versionStr);
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
	public List<String> getViewReferences(String cat, String schema, String viewName)
	{
		List<String> list = new ArrayList<>();

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
				
				list.add(type + " - " + object);
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

		return list;
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
}
