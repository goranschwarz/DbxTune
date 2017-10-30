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
		DbxConnectionStateInfo csi = new DbxConnectionStateInfoGenericJdbc(this);
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
