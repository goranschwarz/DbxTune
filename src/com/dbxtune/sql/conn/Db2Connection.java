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

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

import org.apache.log4j.Logger;

import com.dbxtune.sql.conn.info.DbmsVersionInfo;
import com.dbxtune.sql.conn.info.DbmsVersionInfoDb2;
import com.dbxtune.sql.conn.info.DbxConnectionStateInfo;
import com.dbxtune.sql.conn.info.DbxConnectionStateInfoGenericJdbc;
import com.dbxtune.utils.StringUtil;
import com.dbxtune.utils.Ver;

public class Db2Connection
extends DbxConnection
{
	private static Logger _logger = Logger.getLogger(Db2Connection.class);

	public Db2Connection(Connection conn)
	{
		super(conn);
		Ver.majorVersion_mustBeTenOrAbove = true;
//System.out.println("constructor::Db2Connection(conn): conn="+conn);
	}

	@Override
	public DbmsVersionInfo createDbmsVersionInfo()
	{
		return new DbmsVersionInfoDb2(this);
	}

	@Override
	public boolean isInTransaction()
	throws SQLException
	{
		// Hopefully this works, but NOT certain
		String sql = 
			  "select UOW_LOG_SPACE_USED "
			+ "FROM TABLE(MON_GET_UNIT_OF_WORK((select agent_id from sysibmadm.applications where appl_id = application_id()), -1))";

		boolean retVal = false;
		
		Statement stmt = createStatement();
		ResultSet rs = stmt.executeQuery(sql);
		while (rs.next())
		{
			retVal = rs.getInt(1) > 0;
		}
		rs.close();
		stmt.close();

		return retVal;
	}

	@Override
	public DbxConnectionStateInfo refreshConnectionStateInfo()
	{
		DbxConnectionStateInfo csi = new DbxConnectionStateInfoGenericJdbc(this);
		setConnectionStateInfo(csi);
		return csi;
	}
	
	
	@Override
	public long getRowCountEstimate(String catalog, String schema, String table)
	throws SQLException
	{
		long rowCount = -1;
		
//		String whereCat = StringUtil.isNullOrBlank(catalog) ? "" : "  and upper(TABLE_CATALOG) = upper('" + catalog + "') \n";
//		String whereSch = StringUtil.isNullOrBlank(schema)  ? "" : "  and upper(TABLE_SCHEMA)  = upper('" + schema  + "') \n";
//		String whereTab = StringUtil.isNullOrBlank(table)   ? "" : "  and upper(TABLE_NAME)    = upper('" + table   + "') \n";
		String whereCat = StringUtil.isNullOrBlank(catalog) ? "" : "  and TABSCHEMA  = '" + catalog + "' \n";
		String whereSch = StringUtil.isNullOrBlank(schema)  ? "" : "  and TABSCHEMA  = '" + schema  + "' \n";
		String whereTab = StringUtil.isNullOrBlank(table)   ? "" : "  and TABNAME    = '" + table   + "' \n";
		
		String sql = "SELECT CARD \n"
				+ "from syscat.tables \n"
				+ "where 1 = 1 \n"
//				+ "  and TYPE in('T', 'U') \n"
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
	 * Get the connected database server/instance name.
	 * @return null if not connected else: Retrieves the name of this database server/instance name.
	 * @see java.sql.DatabaseMetaData.getDatabaseProductName
	 */
	@Override
	public String getDbmsServerName() 
	throws SQLException
	{
		if (_databaseServerName != null)
			return _databaseServerName;
		
		if (_conn == null)
			return null;
		
		String serverName = "";

		String sql = "";

		// Get the hostname
		sql = "SELECT HOST_NAME FROM SYSIBMADM.ENV_SYS_INFO";
		String db2HostName = "";
		try (Statement stmt = _conn.createStatement(); ResultSet rs = stmt.executeQuery(sql))
		{
			while (rs.next())
			{
				db2HostName = rs.getString(1).trim();
			}
		}
		catch (SQLException ex)
		{
			_logger.warn("Problem getting DB2 DBMS Server Name, setting this to 'unknownHostName'. SQL='"+sql+"', caught: "+ex);
			db2HostName = "unknownHostName";
		}

		// Get the instanceName and databaseName
		// serverName -->> 'srvName/dbname@hostname'
		sql = "SELECT INST_NAME as srvName, current server as dbname FROM SYSIBMADM.ENV_INST_INFO";
		try (Statement stmt = _conn.createStatement(); ResultSet rs = stmt.executeQuery(sql))
		{
			while (rs.next())
			{
				String srvName = rs.getString(1).trim();
				String dbname  = rs.getString(2).trim();

				serverName = srvName + "/" + dbname + "@" + db2HostName;
			}
		}
		catch (SQLException ex)
		{
			_logger.warn("Problem getting DB2 DBMS Server Name, setting this to 'unknown'. SQL='"+sql+"', caught: "+ex);
			serverName = "unknown";
		}

		_databaseServerName = serverName;
		return serverName;
	}

	@Override
	protected int getDbmsSessionId_impl() throws SQLException
	{
		// Not 100% sure if this is the correct one to use... and it also returns a BIGINT... but when doing a select I can only see small numbers
		String sql = "select sysproc.mon_get_application_handle() from SYSIBM.SYSDUMMY1";
		
		int spid = -1;
		try (Statement stmnt = _conn.createStatement(); ResultSet rs = stmnt.executeQuery(sql))
		{
			while(rs.next())
				spid = rs.getInt(1);
		}
		
		return spid;
	}
}
