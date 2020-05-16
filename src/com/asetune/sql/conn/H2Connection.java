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
import java.util.LinkedHashMap;
import java.util.Map;

import org.apache.log4j.Logger;

import com.asetune.sql.conn.info.DbxConnectionStateInfo;
import com.asetune.sql.conn.info.DbxConnectionStateInfoGenericJdbc;
import com.asetune.ui.autocomplete.completions.TableExtraInfo;
import com.asetune.utils.StringUtil;
import com.asetune.utils.Ver;

public class H2Connection extends DbxConnection
{
	private static Logger _logger = Logger.getLogger(H2Connection.class);

	public H2Connection(Connection conn)
	{
		super(conn);
		Ver.majorVersion_mustBeTenOrAbove = false;
//System.out.println("constructor::H2Connection(conn): conn="+conn);
	}

	@Override
	public DbxConnectionStateInfo refreshConnectionStateInfo()
	{
		DbxConnectionStateInfo csi = new DbxConnectionStateInfoGenericJdbc(this);
		setConnectionStateInfo(csi);
		return csi;
	}

	@Override
	public boolean isInTransaction() throws SQLException
	{
		return false; // FIXME: Don't know how to check this, so lets assume FALSE
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
			srvVersionNum = Ver.h2VersionStringToNumber(versionStr);
		}
		catch (SQLException ex)
		{
			//_logger.error("SqlServerConnection.getDbmsVersionNumber(), '"+sql+"'", ex);
		}
		
		return srvVersionNum;
	}

	@Override
	public String getDbmsVersionStr() 
	throws SQLException
	{
		final String UNKNOWN = "";

		if ( ! isConnectionOk() )
			return UNKNOWN;

		String sql = "select h2version()";

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
	public Map<String, Object> getDbmsExtraInfo()
	{
		String sql = "select DATABASE(), DATABASE_PATH()";
		LinkedHashMap<String, Object> map = new LinkedHashMap<>();
		
		try (Statement stmnt = _conn.createStatement(); ResultSet rs = stmnt.executeQuery(sql) )
		{
			while (rs.next())
			{
				map.put("DATABASE",      rs.getString(1));
				map.put("DATABASE_PATH", rs.getString(2));
			}
		}
		catch (SQLException ex)
		{
			_logger.error("getDbmsExtraInfo(): Problems executing sql '"+sql+"'. Caught="+ex);
			if (_logger.isDebugEnabled())
				_logger.debug("getDbmsExtraInfo(): Problems executing sql '"+sql+"'. Caught="+ex, ex);
		}
		
		return map;
	}
	
	
	@Override
	public Map<String, TableExtraInfo> getTableExtraInfo(String cat, String schema, String table)
	{
		LinkedHashMap<String, TableExtraInfo> extraInfo = new LinkedHashMap<>();

//		cat    = StringUtil.isNullOrBlank(cat)    ? "" : cat    + ".";
//		schema = StringUtil.isNullOrBlank(schema) ? "" : schema + ".";

		
		String sql = 
				  "select ROW_COUNT_ESTIMATE \n"
				+ "      ,DISK_SPACE_USED('\"'||TABLE_SCHEMA||'\".\"'||TABLE_NAME||'\"')/1024 \n"
				+ "from INFORMATION_SCHEMA.TABLES \n"
				+ "where 1=1 \n"
				+ (StringUtil.hasValue(schema) ? "  and upper(TABLE_SCHEMA)   = upper('" + schema + "') \n" : "")
				+ "  and upper(TABLE_NAME)   = upper('" + table  + "') \n";

		try
		{
			Statement stmnt = _conn.createStatement();
			ResultSet rs = stmnt.executeQuery(sql);
			while(rs.next())
			{
				long rowcount    = rs.getLong(1);
				long tabSizeInKb = rs.getLong(2);
				
				extraInfo.put(TableExtraInfo.TableRowCount,     new TableExtraInfo(TableExtraInfo.TableRowCount,     "Row Count",      rowcount,    "Number of rows in the table. 'ROW_COUNT_ESTIMATE' from 'INFORMATION_SCHEMA.TABLES'", null));
				extraInfo.put(TableExtraInfo.TableDataSizeInKb, new TableExtraInfo(TableExtraInfo.TableDataSizeInKb, "Tab Size In KB", tabSizeInKb, "Table size in KB. using DISK_SPACE_USED(), see: http://www.h2database.com/html/functions.html#disk_space_used", null));
			}
			rs.close();
		}
		catch (SQLException ex)
		{
			_logger.error("getTableExtraInfo(): Problems executing sql '"+sql+"'. Caught="+ex);
			if (_logger.isDebugEnabled())
				_logger.debug("getTableExtraInfo(): Problems executing sql '"+sql+"'. Caught="+ex, ex);
		}
		
		return extraInfo;
	}

	@Override
	public long getRowCountEstimate(String catalog, String schema, String table)
	throws SQLException
	{
		long rowCount = -1;
		
		String whereCat = StringUtil.isNullOrBlank(catalog) ? "" : "  and upper(TABLE_CATALOG) = upper('" + catalog + "') \n";
		String whereSch = StringUtil.isNullOrBlank(schema)  ? "" : "  and upper(TABLE_SCHEMA)  = upper('" + schema  + "') \n";
		String whereTab = StringUtil.isNullOrBlank(table)   ? "" : "  and upper(TABLE_NAME)    = upper('" + table   + "') \n";
		
		String sql = "select ROW_COUNT_ESTIMATE \n"
				+ "from INFORMATION_SCHEMA.TABLES \n"
				+ "where TABLE_TYPE    = 'TABLE' \n"
				+ whereCat
				+ whereSch
				+ whereTab
				+ "";
		
		try (Statement stmnt = this.createStatement(); ResultSet rs = stmnt.executeQuery(sql))
		{
			while(rs.next())
				rowCount = rs.getLong(1);
		}
		
		return rowCount;
	}

	/** 
	 * Drop a schema 
	 * @param schemaName
	 * @throws SQLException 
	 */
	@Override
	public void dropSchema(String schemaName) throws SQLException
	{
//		String qic = getMetaData().getIdentifierQuoteString();
		dbExec("drop schema " + getLeftQuote() + schemaName + getRightQuote() + " CASCADE "); // Cascade to drop all object...
	}

	//	/**
//	 * Create a schema if not already exists
//	 * @param schemaName
//	 * @throws SQLException 
//	 */
//	@Override
//	public void createSchemaIfNotExists(String schemaName) throws SQLException
//	{
//		String qic = getMetaData().getIdentifierQuoteString();
//		dbExec("create schema if not exists "+qic+schemaName+qic);
//	}
}
