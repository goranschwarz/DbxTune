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
package com.dbxtune.sql.conn.info;

import com.dbxtune.sql.conn.DbxConnection;

public class DbmsVersionInfoOracle
extends DbmsVersionInfo
{
	private boolean _isRac = false;

	public DbmsVersionInfoOracle(DbxConnection conn)
	{
		super(conn);
		create(conn);
	}

	public DbmsVersionInfoOracle(long longVersion)
	{
		super(null);
		setLongVersion(longVersion);
	}

	/**
	 * Do the work in here
	 */
	private void create(DbxConnection conn)
	{
		// Check if we are connected to a RAC enabled Oracle
		//FIXME: setRac(OracleConnectionUtils.isRac(conn));
	}

	public void    setRac(boolean b) { _isRac = b; }
	public boolean  isRac()          { return _isRac; }
}
