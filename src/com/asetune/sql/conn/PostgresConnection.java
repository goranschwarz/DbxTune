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

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.LinkedHashMap;
import java.util.Map;

import org.apache.log4j.Logger;

import com.asetune.sql.conn.info.DbxConnectionStateInfo;
import com.asetune.sql.conn.info.DbxConnectionStateInfoGenericJdbc;
import com.asetune.sql.conn.info.DbxConnectionStateInfoPostgres;
import com.asetune.ui.autocomplete.completions.TableExtraInfo;
import com.asetune.utils.StringUtil;
import com.asetune.utils.Ver;

public class PostgresConnection extends DbxConnection
{
	private static Logger _logger = Logger.getLogger(PostgresConnection.class);

	public PostgresConnection(Connection conn)
	{
		super(conn);
		Ver.majorVersion_mustBeTenOrAbove = true;
//System.out.println("constructor::PostgresConnection(conn): conn="+conn);
	}

	@Override
	public DbxConnectionStateInfo refreshConnectionStateInfo()
	{
		DbxConnectionStateInfo csi = new DbxConnectionStateInfoPostgres(this);
		setConnectionStateInfo(csi);
		return csi;
	}

	@Override
	public boolean isInTransaction() throws SQLException
	{
		return false;
	}


//	NOTE: The below didn't work as expected... find a better way!!!
//	@Override
//	public boolean isInTransaction()
//	throws SQLException
//	{
//		String sql = 
//			"select count(*) \n" +
//			"from pg_locks \n" +
//			"where pid = pg_backend_pid() \n" +
//			"  and locktype in ('transactionid') \n" +
//			"  and mode = 'ExclusiveLock' \n";
//
//		boolean retVal = false;
//		
//		Statement stmt = createStatement();
//		ResultSet rs = stmt.executeQuery(sql);
//		while (rs.next())
//		{
//			retVal = rs.getInt(1) == 1;
//		}
//		rs.close();
//		stmt.close();
//
//		return retVal;
//	}
// workbench.db.postgresql.opentransaction.query=select count(*) from pg_locks where pid = pg_backend_pid() and locktype in ('transactionid') and mode = 'ExclusiveLock'

	@Override
	public Map<String, TableExtraInfo> getTableExtraInfo(String cat, String schema, String table)
	{
		LinkedHashMap<String, TableExtraInfo> extraInfo = new LinkedHashMap<>();

//		cat    = StringUtil.isNullOrBlank(cat)    ? "" : cat    + ".";
		schema = StringUtil.isNullOrBlank(schema) ? "" : schema + ".";

		
//		String sql = "SELECT reltuples::BIGINT AS estimate FROM pg_class WHERE relname='" + schema + table + "'";
//		String sql = "SELECT reltuples::BIGINT AS estimate FROM pg_class WHERE relname='" + table + "'";
		String sql = 
			"SELECT reltuples::bigint                                     AS estimate,  \n" +
			"       pg_size_pretty(pg_total_relation_size(oid))           AS totalSize, \n" +
			"       pg_size_pretty(pg_total_relation_size(oid) - pg_indexes_size(oid) - COALESCE(pg_total_relation_size(reltoastrelid),0)) AS dataSize, \n" +
			"       pg_size_pretty(pg_indexes_size(oid))                  AS indexSize,  \n" +
			"       pg_size_pretty(pg_total_relation_size(reltoastrelid)) AS toastSize  \n" +
			"FROM pg_class \n" + 
			"WHERE oid = '" + schema + table + "'::regclass";

		try
		{
			Statement stmnt = _conn.createStatement();
			ResultSet rs = stmnt.executeQuery(sql);
			while(rs.next())
			{
				extraInfo.put(TableExtraInfo.TableRowCount,       new TableExtraInfo(TableExtraInfo.TableRowCount,       "Row Count",       rs.getLong(1),   "Number of rows in the table. Note: fetched from statistics using 'WHERE oid = 'schema.table'::regclass'", null));
				extraInfo.put(TableExtraInfo.TableTotalSizeInKb,  new TableExtraInfo(TableExtraInfo.TableTotalSizeInKb,  "Total Size",      rs.getString(2), "Table size (data + indexes). Note: pg_size_pretty(pg_total_relation_size(oid))", null));
				extraInfo.put(TableExtraInfo.TableDataSizeInKb,   new TableExtraInfo(TableExtraInfo.TableDataSizeInKb,   "Data Size",       rs.getString(3), "Data Size. Note: pg_size_pretty(total_bytes - index_bytes - toast_bytes)", null));
				extraInfo.put(TableExtraInfo.TableIndexSizeInKb,  new TableExtraInfo(TableExtraInfo.TableIndexSizeInKb,  "Index Size",      rs.getString(4), "Index size (all indexes). Note: pg_size_pretty(pg_indexes_size(oid))", null));
				extraInfo.put(TableExtraInfo.TableLobSizeInKb,    new TableExtraInfo(TableExtraInfo.TableLobSizeInKb,    "Toast Size",      rs.getString(5), "Toast Size. Note: pg_size_pretty(pg_total_relation_size(reltoastrelid))", null));
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

//		String sql = "select inet_server_addr() as onIp, inet_server_port() as portNum, current_database() as dbname";
		String sql = "select inet_server_addr() as onIp, inet_server_port() as portNum";
		try
		{
			Statement stmt = _conn.createStatement();
			ResultSet rs = stmt.executeQuery(sql);
			while (rs.next())
			{
				String ip     = rs.getString(1).trim();
				String port   = rs.getString(2).trim();
//				String dbname = rs.getString(3).trim();

				String hostname = ip;
				try
				{
					InetAddress addr = InetAddress.getByName(ip);
					hostname = addr.getHostName();
				}
				catch(UnknownHostException ex)
				{
				}

//				serverName = DbxTune.stripSrvName(hostname + ":" + port);
//				serverName = hostname + ":" + port + "/" + dbname;
				serverName = hostname + ":" + port;
			}
			rs.close();
			stmt.close();
		}
		catch (SQLException ex)
		{
			_logger.warn("Problem getting Postgres DBMS Server Name, setting this to 'unknown'. SQL='"+sql+"', caught: "+ex);
			serverName = "unknown";
		}
		
		_databaseServerName = serverName;
		return serverName;
	}
}
