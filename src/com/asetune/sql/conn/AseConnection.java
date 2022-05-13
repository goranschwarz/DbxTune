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
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.apache.log4j.Logger;

import com.asetune.gui.ResultSetTableModel;
import com.asetune.sql.conn.info.DbmsVersionInfo;
import com.asetune.sql.conn.info.DbmsVersionInfoSybaseAse;
import com.asetune.sql.conn.info.DbxConnectionStateInfo;
import com.asetune.sql.conn.info.DbxConnectionStateInfoAse;
import com.asetune.ui.autocomplete.completions.TableExtraInfo;
import com.asetune.utils.AseConnectionUtils;
import com.asetune.utils.Configuration;
import com.asetune.utils.DbUtils;
import com.asetune.utils.StringUtil;
import com.asetune.utils.Ver;

public class AseConnection 
extends TdsConnection
{
	private static final Logger	_logger	= Logger.getLogger(AseConnection.class);

	public static final String  PROPKEY_getTableExtraInfo_useSpSpaceused = "AseConnection.getTableExtraInfo.useSpSpaceused";
	public static final boolean DEFAULT_getTableExtraInfo_useSpSpaceused = true;

	public static final String  PROPKEY_getTableExtraInfo_getIndexTypeInfo = "AseConnection.getTableExtraInfo.getIndexTypeInfo";
	public static final boolean DEFAULT_getTableExtraInfo_getIndexTypeInfo = true;

	public AseConnection(Connection conn)
	{
		super(conn);
		Ver.majorVersion_mustBeTenOrAbove = true;
//System.out.println("constructor::AseConnection(conn): conn="+conn);
	}

	@Override
	public DbmsVersionInfo createDbmsVersionInfo()
	{
		return new DbmsVersionInfoSybaseAse(this);
	}

	@Override
	public DbxConnectionStateInfo refreshConnectionStateInfo()
	{
		DbxConnectionStateInfo csi = new DbxConnectionStateInfoAse(this);
		setConnectionStateInfo(csi);
		return csi;
	}

	@Override
	public boolean isInTransaction() throws SQLException
	{
		String sql = "select trancount = @@trancount, lock_count=count(*) from master.dbo.syslocks where spid = @@spid";

		int tranCount = 0;
		int lockCount = 0;
		try (Statement stmnt = createStatement(); ResultSet rs = stmnt.executeQuery(sql))
		{
			while(rs.next())
			{
				tranCount = rs.getInt(1);
				lockCount = rs.getInt(2);
			}
		}

		return (tranCount > 0 || lockCount > 0);

		// probably check @@trancount && select count(*) from syslocks where spid = @@spid
		//return false; // FIXME: Don't know how to check this, so lets assume FALSE
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
		return AseConnectionUtils.getAseVersionNumber(this);
	}

	@Override
	public boolean isDbmsClusterEnabled()
	{
		return AseConnectionUtils.isClusterEnabled(this);
	}

	@Override
	public List<String> getActiveServerRolesOrPermissions()
	{
		return AseConnectionUtils.getActiveRoles(this);
	}
	
	@Override
	public Map<String, TableExtraInfo> getTableExtraInfo(String cat, String schema, String table)
	{
		String sql = "";
		LinkedHashMap<String, TableExtraInfo> extraInfo = new LinkedHashMap<>();

//		cat    = StringUtil.isNullOrBlank(cat)    ? "" : cat    + ".dbo.";
//		schema = StringUtil.isNullOrBlank(schema) ? "" : schema + ".";
// FIXME: do the above... at least if we start to use: dbname.sp_spaceused owner.tabname, 1
// see: getViewReferences

		String lockingScheme = "-unknown-";
		try
		{
			// Get locking schema
			sql = "select lockscheme(object_id('" + cat + "." + schema + "." + table + "'), db_id('"+cat+"'))";
			Statement stmnt = _conn.createStatement();
			ResultSet rs = stmnt.executeQuery(sql);
			while (rs.next())
				lockingScheme = rs.getString(1);
			rs.close();
			stmnt.close();
		}
		catch(SQLException ex)
		{
			_logger.error("getTableExtraInfo(): Problems executing sql '"+sql+"'. Caught="+ex);
		}

		boolean getIndexTypeInfo = Configuration.getCombinedConfiguration().getBooleanProperty(PROPKEY_getTableExtraInfo_getIndexTypeInfo, DEFAULT_getTableExtraInfo_getIndexTypeInfo);
		if (getIndexTypeInfo)
		{
			sql = "exec "+cat+"..sp_helpindex '" + schema + "." + table + "'";
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
		
		boolean useSpSpaceused = Configuration.getCombinedConfiguration().getBooleanProperty(PROPKEY_getTableExtraInfo_useSpSpaceused, DEFAULT_getTableExtraInfo_useSpSpaceused);
		if (useSpSpaceused)
		{
			sql = "exec "+cat+"..sp_spaceused '" + schema + "." + table + "', 1";
			try
			{
				List<ResultSetTableModel> rstmList = DbUtils.exec(_conn, sql, 2);
				
				if (rstmList.size() >= 1)
				{
					ResultSetTableModel indexInfo = null;
					ResultSetTableModel tableInfo = null;

					if (rstmList.size() == 1)
					{
						tableInfo = rstmList.get(0);
					}
					else
					{
						indexInfo = rstmList.get(0);
						tableInfo = rstmList.get(1);
					}

					NumberFormat nf = NumberFormat.getInstance();

					int sumIndexSize     = 0;
					int sumIndexReserved = 0;
					int sumIndexUnused   = 0;
					int sumLobSize       = 0;
					int sumLobReserved   = 0;
					int sumLobUnused     = 0;

					// Get Index Info
					if (indexInfo != null)
					{
						Map<String, String> extIndexInfo = new HashMap<>();
						for (int r=0; r<indexInfo.getRowCount(); r++)
						{
							String index_name = indexInfo.getValueAsString (r, "index_name", true, "");
							if (index_name.equalsIgnoreCase("t"+table))
							{
								sumLobSize     += StringUtil.parseInt( indexInfo.getValueAsString(r, "size"    , true, "").replace(" KB", ""), 0);
								sumLobReserved += StringUtil.parseInt( indexInfo.getValueAsString(r, "reserved", true, "").replace(" KB", ""), 0);
								sumLobUnused   += StringUtil.parseInt( indexInfo.getValueAsString(r, "unused"  , true, "").replace(" KB", ""), 0);
							}
							else
							{
								int indexSize     = StringUtil.parseInt( indexInfo.getValueAsString(r, "size"    , true, "").replace(" KB", ""), 0);
								int indexReserved = StringUtil.parseInt( indexInfo.getValueAsString(r, "reserved", true, "").replace(" KB", ""), 0);
								int indexUnused   = StringUtil.parseInt( indexInfo.getValueAsString(r, "unused"  , true, "").replace(" KB", ""), 0);

								sumIndexSize     += indexSize;
								sumIndexReserved += indexReserved;
								sumIndexUnused   += indexUnused;

								extIndexInfo.put(index_name, "size="+nf.format(indexSize)+" KB, reserved="+nf.format(indexReserved)+" KB, unused="+nf.format(indexUnused)+" KB");
							}
						}

						// ADD INFO
						extraInfo.put(TableExtraInfo.IndexExtraInfo, new TableExtraInfo(TableExtraInfo.IndexExtraInfo, "IndexInfo", extIndexInfo, "extended Index Information", null));
					}
					
					// Get Table Info
					int rowtotal = StringUtil.parseInt( tableInfo.getValueAsString(0, "rowtotal"  , true, "").replace(" KB", ""), 0);
					int reserved = StringUtil.parseInt( tableInfo.getValueAsString(0, "reserved"  , true, "").replace(" KB", ""), 0);
					int data     = StringUtil.parseInt( tableInfo.getValueAsString(0, "data"      , true, "").replace(" KB", ""), 0);
					int index    = StringUtil.parseInt( tableInfo.getValueAsString(0, "index_size", true, "").replace(" KB", ""), 0);
					int unused   = StringUtil.parseInt( tableInfo.getValueAsString(0, "unused"    , true, "").replace(" KB", ""), 0);		

					// ADD INFO
					extraInfo.put(TableExtraInfo.TableRowCount,      new TableExtraInfo(TableExtraInfo.TableRowCount,      "Row Count",        rowtotal     , "Number of rows in the table. Note: exec dbname..sp_spaceused 'schema.tabname', 1", null));
					extraInfo.put(TableExtraInfo.TableTotalSizeInKb, new TableExtraInfo(TableExtraInfo.TableTotalSizeInKb, "Total Size In KB", data+index   , "Details from sp_spaceused: reserved="+nf.format(reserved)+" KB, data="+nf.format(data)+" KB, index_size="+nf.format(index)+" KB, unused="+nf.format(unused)+" KB", null));
					extraInfo.put(TableExtraInfo.TableDataSizeInKb,  new TableExtraInfo(TableExtraInfo.TableDataSizeInKb,  "Data Size In KB",  data         , "From 'sp_spaceued', columns 'data'.", null));
					extraInfo.put(TableExtraInfo.TableIndexSizeInKb, new TableExtraInfo(TableExtraInfo.TableIndexSizeInKb, "Index Size In KB", sumIndexSize , "From 'sp_spaceued', index section, sum of 'size'. Details: size="+nf.format(sumIndexSize)+" KB, reserved="+nf.format(sumIndexReserved)+" KB, unused="+nf.format(sumIndexUnused)+" KB", null));
					extraInfo.put(TableExtraInfo.TableLobSizeInKb,   new TableExtraInfo(TableExtraInfo.TableLobSizeInKb,   "LOB Size In KB",   sumLobSize   , "From 'sp_spaceued', index section, 'size' of columns name 't"+table+"'. Details: size="+nf.format(sumLobSize)+" KB, reserved="+nf.format(sumLobReserved)+" KB, unused="+nf.format(sumLobUnused)+" KB", null));
					extraInfo.put(TableExtraInfo.TableLockScheme,    new TableExtraInfo(TableExtraInfo.TableLockScheme,    "Locking Scheme",   lockingScheme, "Table locking Scheme (allpages, datapages, datarows)", null));
				}
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
//			sql = "select row_count(db_id('"+cat+"'), object_id('"+schema+"."+table+"'))";
			sql = "select"
					+ "    rowCnt  = row_count(db_id('" + cat + "'), object_id('" + cat + "." + schema + "." + table + "')), \n"
					+ "    partCnt = (select count(*) from " + (cat==null ? "" : cat+".") + "dbo.syspartitions where id = object_id('" + cat + "." + schema + "." + table + "') and indid in(0,1)) \n"
					+ "";
			
			long dbmsVersion = getCachedDbmsVersionNumber();
			if (dbmsVersion < Ver.ver(15,0))
			{
				_logger.warn("getTableExtraInfo() isn't yet implemented for version '"+dbmsVersion+"', It needs to be at least 15.0"); 
				return null;
			}

			//System.out.println("AseConnection.getTableExtraInfo(cat='"+cat+"', schema='"+schema+"', table='"+table+"'): sql="+sql);
			try
			{
				Statement stmnt = _conn.createStatement();
				ResultSet rs = stmnt.executeQuery(sql);
				while(rs.next())
				{
					extraInfo.put(TableExtraInfo.TableRowCount,       new TableExtraInfo(TableExtraInfo.TableRowCount,       "Row Count",       rs.getLong(1), "Number of rows in the table. Note: fetched from statistics using the function: row_count(dbid, objid)", null));
					extraInfo.put(TableExtraInfo.TableLockScheme,     new TableExtraInfo(TableExtraInfo.TableLockScheme,     "Locking Scheme",  lockingScheme, "Table locking Scheme (allpages, datapages, datarows)", null));
					extraInfo.put(TableExtraInfo.TablePartitionCount, new TableExtraInfo(TableExtraInfo.TablePartitionCount, "Partition Count", rs.getLong(2), "Number of table partition(s). Note: fetched from 'syspartitions'", null));
				}
				rs.close();
				stmnt.close();
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
		
		String tmpCatalog = StringUtil.isNullOrBlank(catalog) ? "" : catalog + ".";
		String tmpSchema  = StringUtil.isNullOrBlank(schema)  ? "" : schema  + ".";
		
		String tmpDbId = StringUtil.isNullOrBlank(catalog) ? "db_id()" : "db_id('" + catalog + "')";

		String sql = "select rowCnt = row_count(" + tmpDbId + ", object_id('" + tmpCatalog + tmpSchema + table + "'))";

		long dbmsVersion = getCachedDbmsVersionNumber();
		if (dbmsVersion < Ver.ver(15,0))
		{
			_logger.warn("getTableExtraInfo() isn't yet implemented for version '"+dbmsVersion+"', It needs to be at least 15.0"); 
			return -1;
		}

		try (Statement stmnt = this.createStatement(); ResultSet rs = stmnt.executeQuery(sql))
		{
			while(rs.next())
				rowCount = rs.getLong(1);
		}
//System.out.println("ASE:getRowCountEstimate(catalog='"+catalog+"', schema='"+schema+"', table='"+table+"'): rowCoun=" + rowCount + ", SQL=" + sql);		
		return rowCount;
	}
	
	long _cachedDbmsVersion = -1;
	private long getCachedDbmsVersionNumber()
	{
		if (_cachedDbmsVersion == -1)
			_cachedDbmsVersion = getDbmsVersionNumber();

		return _cachedDbmsVersion;
	}

	@Override
	public void close() throws SQLException
	{
		_cachedDbmsVersion = -1;
		super.close();
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
			_logger.error("Problems executing sql '"+sql+"'. Caught="+ex);
			if (_logger.isDebugEnabled())
				_logger.debug("Problems executing sql '"+sql+"'. Caught="+ex, ex);
		}

		return list;
	}

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
