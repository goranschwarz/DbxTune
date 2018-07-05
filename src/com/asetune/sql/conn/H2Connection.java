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
	public Map<String, TableExtraInfo> getTableExtraInfo(String cat, String schema, String table)
	{
		LinkedHashMap<String, TableExtraInfo> extraInfo = new LinkedHashMap<>();

//		cat    = StringUtil.isNullOrBlank(cat)    ? "" : cat    + ".";
//		schema = StringUtil.isNullOrBlank(schema) ? "" : schema + ".";

		
		String sql = 
				  "select ROW_COUNT_ESTIMATE \n"
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
				extraInfo.put(TableExtraInfo.TableRowCount, new TableExtraInfo(TableExtraInfo.TableRowCount, "Row Count", rs.getLong(1), "Number of rows in the table. 'ROW_COUNT_ESTIMATE' from 'INFORMATION_SCHEMA.TABLES'", null));
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
