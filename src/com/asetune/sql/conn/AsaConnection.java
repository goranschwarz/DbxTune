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
import com.asetune.utils.AseConnectionUtils;
import com.asetune.utils.Ver;

public class AsaConnection 
extends TdsConnection
{

	public AsaConnection(Connection conn)
	{
		super(conn);
		Ver.majorVersion_mustBeTenOrAbove = true;
//System.out.println("constructor::AsaConnection(conn): conn="+conn);
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
		return AseConnectionUtils.getAseVersionNumber(this);
	}
	@Override
	public boolean isDbmsClusterEnabled()
	{
		return false;
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
