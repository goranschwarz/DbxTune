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

import java.io.File;
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
import com.asetune.utils.AseConnectionUtils;
import com.asetune.utils.StringUtil;
import com.asetune.utils.Ver;


public class IqConnection 
extends TdsConnection
{
	private static Logger _logger = Logger.getLogger(IqConnection.class);

	public IqConnection(Connection conn)
	{
		super(conn);
		Ver.majorVersion_mustBeTenOrAbove = true;
//System.out.println("constructor::IqConnection(conn): conn="+conn);
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
//		return AseConnectionUtils.getAseVersionNumber(this);
		return AseConnectionUtils.getIqVersionNumber(this);
	}

	@Override
	public boolean isDbmsClusterEnabled()
	{
		return false; // FIXME: check if we are in Multiplex mode...
	}

	public String getPlatform()
	{
		String sql = "select Value from sa_eng_properties() where PropName = 'Platform'";
		try
		{
			Statement stmnt = createStatement();
			ResultSet rs = stmnt.executeQuery(sql);

			String platform = "";
			while(rs.next())
				platform = rs.getString(1);
			rs.close();
			stmnt.close();

			return platform;
		}
		catch (SQLException e)
		{
			_logger.warn("Problems getting IQ Platform. SQL='"+sql+"', Caught: "+e);
			return null;
		}
	}

	public String getIqMsgFilname()
	{
		String filename = null;
		String sql = "";

		try
		{
			// Something like this will be returned: C:\Sybase\IQ-16_0\GORAN_16_IQ\errorlog.iqmsg.log
			// But it may just be the filename without a full path... 
			// If that's the case we fix that with a second SQL query (see below)
			sql = "select file_name from sysfile where dbspace_name = 'IQ_SYSTEM_MSG'";
			Statement stmnt = createStatement();
			ResultSet rs = stmnt.executeQuery(sql);
			
			while(rs.next())
				filename = rs.getString(1);
			rs.close();

			boolean hasFullPath = (filename != null && (filename.indexOf('/')>=0 || filename.indexOf('\\')>=0));
			if ( ! hasFullPath )
			{
				String dbFileName = "";
				
				// Something like this will be returned: C:\Sybase\IQ-16_0\GORAN_16_IQ\GORAN_16_IQ.db
				sql = "select Value from sa_db_properties() where PropName = 'File'";
				rs = stmnt.executeQuery(sql);
				while(rs.next())
					dbFileName = rs.getString(1);
				
				// Get only the PATH part of the file
				File f = new File(dbFileName);
//FIXME: the below wont work.... so fix this...
				filename = f.getPath() + filename;
			}
			stmnt.close();
			
			return filename;
		}
		catch (SQLException e)
		{
			_logger.warn("Problems getting IQ Msg Log filename. SQL='"+sql+"', Caught: "+e);
			return null;
		}
	}

	@Override
	public Map<String, TableExtraInfo> getTableExtraInfo(String cat, String schema, String table)
	{
		LinkedHashMap<String, TableExtraInfo> extraInfo = new LinkedHashMap<>();

//		cat    = StringUtil.isNullOrBlank(cat)    ? "" : cat    + ".";
		schema = StringUtil.isNullOrBlank(schema) ? "" : schema + ".";

		
		String sql = "call sp_iqtablesize('" + schema + table + "')";

		try
		{
			Statement stmnt = _conn.createStatement();
			ResultSet rs = stmnt.executeQuery(sql);
			while(rs.next())
			{
//				extraInfo.put(TableExtraInfo.TableRowCount,       new TableExtraInfo(TableExtraInfo.TableRowCount,       "Row Count",       rs.getLong(1), "Number of rows in the table. Note: fetched from statistics using 'WHERE oid = 'schema.table'::regclass'", null));
				extraInfo.put(TableExtraInfo.TableTotalSizeInKb,  new TableExtraInfo(TableExtraInfo.TableTotalSizeInKb,  "Total Size",      rs.getInt(4),  "Physical table size in KB. Note: sp_iqtablesize, column 'KBytes'.", null));
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

}
