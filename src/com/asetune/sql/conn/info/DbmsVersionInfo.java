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
	private   long          _longVersion   = -1;
//	private   int           _shortVersion  = -1;

	// Is this a GCP (Google Cloud Platform) Managed Instance
	protected boolean _isGcpManagedDbms = false;

	// Is this a AWS Managed Instance
	protected boolean _isAwsManagedDbms = false;

	// Is this a Azure Managed Instance
	protected boolean _isAzureManagedDbms = false;

	
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

	public void setLongVersion(long v)
	{ 
		_longVersion  = v; 
	}
	public long getLongVersion()   
	{
		if (_longVersion != -1)
			return _longVersion;

		// FIXME: move the long version number from DbxConnection to "here"
		return _conn.getDbmsVersionNumber();   
	}


	/** Is this a Cloud Managed DBMS Instance (AWS, GCP, Azure) */
	public boolean isCloudManagedDbms()
	{
		return isGcpManagedDbms() || isAwsManagedDbms() || isAzureManagedDbms();
	}

	/** Is this a Google Cloud Managed DBMS Instance */
	public boolean isGcpManagedDbms()
	{
		return _isGcpManagedDbms;
	}

	/** Is this a AWS Cloud Managed DBMS Instance */
	public boolean isAwsManagedDbms()
	{
		return _isAwsManagedDbms;
	}

	/** Is this a Azure Cloud Managed DBMS Instance */
	public boolean isAzureManagedDbms()
	{
		return _isAzureManagedDbms;
	}
	
}
