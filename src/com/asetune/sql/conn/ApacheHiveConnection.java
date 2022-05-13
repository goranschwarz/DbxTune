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
import java.sql.SQLException;

import com.asetune.sql.conn.info.DbmsVersionInfo;
import com.asetune.sql.conn.info.DbmsVersionInfoApacheHive;
import com.asetune.sql.conn.info.DbxConnectionStateInfo;
import com.asetune.sql.conn.info.DbxConnectionStateInfoGenericJdbc;
import com.asetune.utils.Ver;

public class ApacheHiveConnection
extends DbxConnection
{
	public ApacheHiveConnection(Connection conn)
	{
		super(conn);
		Ver.majorVersion_mustBeTenOrAbove = false;
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
	protected int getDbmsSessionId_impl() throws SQLException
	{
		// I don't know how to get Session/connection ID... so lets just return -1 for unknown 
		return -1;
	}

	@Override
	public DbmsVersionInfo createDbmsVersionInfo()
	{
		return new DbmsVersionInfoApacheHive(this);
	}
}
