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
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 * 
 * DbxTune is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License
 * along with DbxTune.  If not, see <http://www.gnu.org/licenses/>.
 ******************************************************************************/
package com.asetune.utils;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;

import com.asetune.sql.conn.DbxConnection;

public class PostgresUtils
{
//	private static Logger _logger = Logger.getLogger(PostgresUtils.class);

	public static Timestamp getStartDate(DbxConnection conn)
	throws SQLException
	{
		String sql = "SELECT pg_postmaster_start_time()";

		Timestamp ts = null;
		try ( PreparedStatement stmnt = conn.prepareStatement(sql) )
		{
			stmnt.setQueryTimeout(10);
			try ( ResultSet rs = stmnt.executeQuery() )
			{
				while (rs.next())
				{
					ts = rs.getTimestamp(1);
				}
			}
		}
		return ts;
	}
}
