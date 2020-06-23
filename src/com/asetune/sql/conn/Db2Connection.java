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

import com.asetune.sql.conn.info.DbxConnectionStateInfo;
import com.asetune.sql.conn.info.DbxConnectionStateInfoGenericJdbc;
import com.asetune.utils.StringUtil;
import com.asetune.utils.Ver;

public class Db2Connection
extends DbxConnection
{

	public Db2Connection(Connection conn)
	{
		super(conn);
		Ver.majorVersion_mustBeTenOrAbove = true;
//System.out.println("constructor::Db2Connection(conn): conn="+conn);
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

}
