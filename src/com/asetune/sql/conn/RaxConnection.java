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

import org.apache.log4j.Logger;

import com.asetune.sql.conn.info.DbxConnectionStateInfo;
import com.asetune.sql.conn.info.DbxConnectionStateInfoRax;
import com.asetune.utils.Ver;

public class RaxConnection
extends TdsConnection
{
	private static Logger _logger = Logger.getLogger(RaxConnection.class);

	public RaxConnection(Connection conn)
	{
		super(conn);
		Ver.majorVersion_mustBeTenOrAbove = true;
//System.out.println("constructor::RaxConnection(conn): conn="+conn);
	}

	@Override
	public DbxConnectionStateInfo refreshConnectionStateInfo()
	{
		DbxConnectionStateInfo csi = new DbxConnectionStateInfoRax(this);
		setConnectionStateInfo(csi);
		return csi;
	}

	@Override
	public boolean isInTransaction() throws SQLException
	{
		return false; // FIXME: Don't know how to check this, so lets assume FALSE
	}

	/**
	 * If it's a Unknown TDS connection, it's probably some jTDS (JavaOpenServer implementation) where isValid() do not seem to work, so lets fall back to use isClosed()
	 */
	@Override
	public boolean isValid(int timeout) throws SQLException
	{
		return ! _conn.isClosed();
	}

	//---------------------------------------
	// Lets cache some stuff
	//---------------------------------------
	private String _cached_srvName = null;

	@Override
	public String getDbmsServerName()
	{
		if (_cached_srvName != null)
			return _cached_srvName;

		final String UNKNOWN = "";

		if ( ! isConnectionOk(false, null) )
			return UNKNOWN;

		try
		{
            // 1> ra_version_all
            // RS> Col# Label     JDBC Type Name      Guessed DBMS type
            // RS> ---- --------- ------------------- -----------------
            // RS> 1    Component java.sql.Types.CHAR char(21)         
            // RS> 2    Version   java.sql.Types.CHAR char(148)        

			String name = UNKNOWN;

			Statement stmt = createStatement();
			ResultSet rs = stmt.executeQuery("ra_version_all");
			while (rs.next())
			{
				String comp = rs.getString(1);
				String ver  = rs.getString(2);
				
				if (comp != null) comp = comp.trim();
				if (ver  != null) ver  = ver .trim();

				if ("Instance:".equals(comp))
				{
					name = ver;
					int dashPos = name.indexOf('-');
					if (dashPos >= 0)
						name = name.substring(0, dashPos).trim();
				}
			}
			rs.close();
			stmt.close();

			_cached_srvName = name;
			return name;
		}
		catch (SQLException e)
		{
			_logger.debug("When getting 'server name', Caught exception.", e);

			return UNKNOWN;
		}
	}

	@Override
	protected int getDbmsSessionId_impl() throws SQLException
	{
		// I don't know how to get SPID... so lets just return -1 for unknown 
		return -1;
	}
}
