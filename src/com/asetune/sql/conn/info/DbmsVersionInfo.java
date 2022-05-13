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
package com.asetune.sql.conn.info;

import com.asetune.sql.conn.DbxConnection;

public abstract class DbmsVersionInfo
{
	protected DbxConnection _conn          = null;
//	private   String        _versionString = "";
//	private   long          _longVersion   = -1;
//	private   int           _shortVersion  = -1;

	public DbmsVersionInfo(DbxConnection conn)
	{
		_conn = conn;
	}

//	public String getVersionString() { return _versionString; }
//	public long   getLongVersion()   { return _longVersion;   }
//	public int    getShortVersion()  { return _shortVersion;  }
//
//	public void setLongVersion(long v) { _longVersion  = v; }
//	public void setShortVersion(int v) { _shortVersion = v; }

//	public abstract long parseVersionString(String versionString);

	public long getLongVersion()   
	{
		// FIXME: move the long version number from DbxConnection to "here"
		return _conn.getDbmsVersionNumber();   
	}
}
