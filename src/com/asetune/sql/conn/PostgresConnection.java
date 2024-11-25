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

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import org.apache.log4j.Logger;

import com.asetune.gui.ResultSetTableModel;
import com.asetune.sql.conn.info.DbmsVersionInfo;
import com.asetune.sql.conn.info.DbmsVersionInfoPostgres;
import com.asetune.sql.conn.info.DbxConnectionStateInfo;
import com.asetune.sql.conn.info.DbxConnectionStateInfoPostgres;
import com.asetune.ui.autocomplete.completions.TableExtraInfo;
import com.asetune.utils.DbUtils;
import com.asetune.utils.StringUtil;
import com.asetune.utils.Ver;

public class PostgresConnection extends DbxConnection
{
	private static Logger _logger = Logger.getLogger(PostgresConnection.class);

	// Cached values
	private List<String> _getActiveServerRolesOrPermissions = null;

	@Override
	public void clearCachedValues()
	{
		super.clearCachedValues();

		_getActiveServerRolesOrPermissions = null;
	}

	
	public PostgresConnection(Connection conn)
	{
		super(conn);
		Ver.majorVersion_mustBeTenOrAbove = true;
//System.out.println("constructor::PostgresConnection(conn): conn="+conn);
	}

	@Override
	public DbmsVersionInfo createDbmsVersionInfo()
	{
		return new DbmsVersionInfoPostgres(this);
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
		// Before Version 13, check if we have Exclusive locks
		if (getDbmsVersionNumber() < Ver.ver(13))
		{
			// workbench.db.postgresql.opentransaction.query=select count(*) from pg_locks where pid = pg_backend_pid() and locktype in ('transactionid') and mode = 'ExclusiveLock'

			int rowc = 0;
			String sql = ""
				    + "SELECT count(*) \n"
				    + "FROM pg_locks \n"
				    + "WHERE pid = pg_backend_pid() \n"
				    + "  AND mode like '%Exclusive%' \n"
				    + "  AND locktype != 'virtualxid' \n"
				    + "";

			try (Statement stmnt = this.createStatement(); ResultSet rs = stmnt.executeQuery(sql))
			{
				while (rs.next())
					rowc = rs.getInt(1);
			}
			return rowc > 0;
		}
		else // in Version 13 or ABOVE
		{
			String sql = "SELECT pg_current_xact_id_if_assigned()";
			String xactid = null;

			try (Statement stmnt = this.createStatement(); ResultSet rs = stmnt.executeQuery(sql))
			{
				while (rs.next())
					xactid = rs.getString(1);
			}
			return StringUtil.hasValue(xactid);
		}
	}

	


	@Override
	public List<String> getActiveServerRolesOrPermissions()
	{
		if (_getActiveServerRolesOrPermissions != null)
			return _getActiveServerRolesOrPermissions;

		// Get current active roles
		String sql = ""	
				+ "WITH RECURSIVE cte AS ( \n"
				+ "    SELECT oid \n"
				+ "    FROM   pg_roles \n"
				+ "    WHERE  rolname = session_user \n"
				+ " \n"
				+ "    UNION ALL \n"
				+ " \n"
				+ "    SELECT m.roleid \n"
				+ "    FROM   cte \n"
				+ "    JOIN   pg_auth_members m ON m.member = cte.oid \n"
				+ ") \n"
				+ "SELECT oid, \n"
				+ "       oid::regrole::text AS rolename \n"
				+ "FROM cte \n"
				+ " \n"
				+ "UNION ALL \n"
				+ " \n"
				+ "SELECT oid, 'superuser' AS rolename \n" // get if we are "superuser"
				+ "FROM pg_roles \n"
				+ "WHERE rolname = session_user \n"
				+ "  AND rolsuper = true \n"
				+ "";

		try (Statement stmnt = this.createStatement(); ResultSet rs = stmnt.executeQuery(sql))
		{
			List<String> permissionList = new LinkedList<String>();

			while (rs.next())
			{
				String role = rs.getString(2);
				if ( ! permissionList.contains(role) )
					permissionList.add(role);
			}

			if (_logger.isDebugEnabled())
				_logger.debug("getActiveServerRolesOrPermissions() returns, permissionList='"+permissionList+"'.");

			// Cache the value for next execution
			_getActiveServerRolesOrPermissions = permissionList;
			return permissionList;
		}
		catch (SQLException ex)
		{
			_logger.warn("getActiveServerRolesOrPermissions(): Problems when executing sql: "+sql, ex);
			return null;
		}
	}
	
	
	@Override
	public Map<String, TableExtraInfo> getTableExtraInfo(String cat, String schema, String table)
	{
//		String catOrigin    = cat;
		String schemaOrigin = schema;
		String tableOrigin  = table;
		
		LinkedHashMap<String, TableExtraInfo> extraInfo = new LinkedHashMap<>();
		
		String qic = "\"";

//		cat    = StringUtil.isNullOrBlank(cat)    ? "" : cat    + ".";
		schema = StringUtil.isNullOrBlank(schema) ? "" : qic + schema + qic + ".";

		
//		String sql = "SELECT reltuples::BIGINT AS estimate FROM pg_class WHERE relname='" + schema + table + "'";
//		String sql = "SELECT reltuples::BIGINT AS estimate FROM pg_class WHERE relname='" + table + "'";
		String sql = 
			"SELECT reltuples::bigint                                     AS estimate,  \n" +
			"       pg_size_pretty(pg_total_relation_size(oid))           AS totalSize, \n" +
			"       pg_size_pretty(pg_total_relation_size(oid) - pg_indexes_size(oid) - COALESCE(pg_total_relation_size(reltoastrelid),0)) AS dataSize, \n" +
			"       pg_size_pretty(pg_indexes_size(oid))                  AS indexSize,  \n" +
			"       pg_size_pretty(pg_total_relation_size(reltoastrelid)) AS toastSize  \n" +
			"FROM pg_class \n" + 
			"WHERE oid = '" + schema + qic + table + qic + "'::regclass"; // with Quoted Identifier

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
			// ERROR: relation "GORANS_UB1_DS.DbxSessionSampleDetaxxiles" does not exist
			// Then try without Quoted Identifier
			if ("42P01".equals(ex.getSQLState()))
			{
				// Remove any " chars
				sql = sql.replace(qic, "");
				
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
				catch (SQLException ex2)
				{
					_logger.error("getTableExtraInfo(): Problems executing sql '"+sql+"'. Caught="+ex2);
					if (_logger.isDebugEnabled())
						_logger.debug("getTableExtraInfo(): Problems executing sql '"+sql+"'. Caught="+ex2, ex2);
				}
			}
			else
			{
				_logger.error("getTableExtraInfo(): Problems executing sql '"+sql+"'. Caught="+ex);
				if (_logger.isDebugEnabled())
					_logger.debug("getTableExtraInfo(): Problems executing sql '"+sql+"'. Caught="+ex, ex);
			}
		}

		// Get included indexes...
		// NOTE: JDBC MetaData getIndexInfo(...) already includes the include columns, but as "keys" (there is no way to see that it's a INCLUDED column)
		//       So we need to do this a bit different...
		boolean getIndexInclude = true;
		if (getIndexInclude)
		{
			sql = ""
				    + "WITH index_columns AS \n"
				    + "( \n"
				    + "    SELECT \n"
				    + "        i.relname AS index_name, \n"
				    + "        a.attname AS column_name, \n"
				    + "        a.attnum  AS column_pos, \n"
				    + "        CASE WHEN x.indisunique             THEN 'UNIQUE' ELSE 'NON-UNIQUE' END AS index_type, \n"
				    + "        CASE WHEN a.attnum <= x.indnkeyatts THEN 'KEY'    ELSE 'INCLUDED'   END AS column_role \n"
				    + "    FROM pg_index x \n"
				    + "    JOIN pg_class i ON i.oid = x.indexrelid \n"
				    + "    JOIN pg_class t ON t.oid = x.indrelid \n"
				    + "    JOIN pg_attribute a ON a.attrelid = i.oid AND a.attnum > 0 \n"
				    + "    JOIN pg_namespace n ON n.oid = t.relnamespace \n"
				    + "    WHERE 1 = 1 \n"
				    + "      AND n.nspname = " + DbUtils.safeStr(schemaOrigin) + " \n"
				    + "      AND t.relname = " + DbUtils.safeStr(tableOrigin)  + " \n"
				    + "    ORDER BY i.relname, column_pos, column_role \n"
				    + ") \n"
				    + "SELECT * \n"
				    + "FROM index_columns \n"
				    + "WHERE column_role = 'INCLUDED' \n"
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
						String indexName   = indexInfo.getValueAsString(r, "index_name",  false, "");
						String indexColumn = indexInfo.getValueAsString(r, "column_name", false, "");

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

		return extraInfo;
	}

	@Override
	public long getRowCountEstimate(String catalog, String schema, String table)
	throws SQLException
	{
		long rowCount = -1;
		String qic = "\"";
		
//		cat    = StringUtil.isNullOrBlank(cat)    ? "" : cat    + ".";
		schema = StringUtil.isNullOrBlank(schema) ? "" : qic + schema + qic + ".";

		
		// check with Quoted Identifiers
//		String sql = "SELECT reltuples::BIGINT AS estimate FROM pg_class WHERE relname='" + schema + qic + table + qic + "'";
		String sql = "SELECT reltuples::BIGINT AS estimate FROM pg_class WHERE oid = '" + schema + qic + table + qic + "'::regclass";

		try (Statement stmnt = this.createStatement(); ResultSet rs = stmnt.executeQuery(sql))
		{
			while(rs.next())
				rowCount = rs.getLong(1);
		}
		
		// Check one again, but WITHOUT Quoted Identifiers
		if (rowCount == -1)
		{
			sql = sql.replace(qic, "");

			try (Statement stmnt = this.createStatement(); ResultSet rs = stmnt.executeQuery(sql))
			{
				while(rs.next())
					rowCount = rs.getLong(1);
			}
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
//			if ( columnType == java.sql.Types.NUMERIC || columnType == java.sql.Types.DECIMAL )
//			{
//				int precision = rsmd.getPrecision(col);
//				int scale     = rsmd.getScale(col);
//				
//				columnTypeName += "("+precision+","+scale+")";
//			}
//
//			// Binary goes as datatype 'bytea' and does NOT have a length specification
//			//if ( columnType == java.sql.Types.BINARY || columnType == java.sql.Types.VARBINARY)
//			//{
//			//	int columnDisplaySize = Math.max(rsmd.getColumnDisplaySize(col), rsmd.getPrecision(col));
//			//		
//			//	columnTypeName += (columnDisplaySize == 2147483647) ? "(max)" : "("+columnDisplaySize+")";
//			//}
//
//			if (    columnType == java.sql.Types.CHAR 
//			     || columnType == java.sql.Types.VARCHAR 
//			     || columnType == java.sql.Types.NCHAR
//			     || columnType == java.sql.Types.NVARCHAR
//			   )
//			{
//				int columnDisplaySize = Math.max(rsmd.getColumnDisplaySize(col), rsmd.getPrecision(col));
//					
//				columnTypeName += "("+columnDisplaySize+")";
//
//				if (columnDisplaySize == 2147483647)
//					columnTypeName = "text";
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
		String sql = "select pg_backend_pid()";

		int spid = -1;
		try (Statement stmnt = _conn.createStatement(); ResultSet rs = stmnt.executeQuery(sql))
		{
			while(rs.next())
				spid = rs.getInt(1);
		}
		
		return spid;
	}
}
