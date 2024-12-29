/*******************************************************************************
 * Copyright (C) 2010-2025 Goran Schwarz
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
package com.dbxtune.pcs;

import java.util.Objects;

import com.dbxtune.sql.SqlObjectName;

public class ObjectLookupQueueEntry
{
	public String _dbname;
	public String _objectName;
	public String _source;
	public String _dependParent;
	public int    _dependLevel;

	public SqlObjectName    _sqlObject; // set this in the ObjectLookupInspector.
	
	// Special option that might be set 
	boolean _isStatementCacheEntry = false;

	public boolean isStatementCacheEntry()           { return _isStatementCacheEntry; }
	public void    setStatementCacheEntry(boolean b) { _isStatementCacheEntry = b; }

	// Used for debugging or print extra information
	private boolean _isPrintInfo = false;
	public void    setPrintInfo(boolean b)	{ _isPrintInfo = b; }
	public boolean isPrintInfo()	        { return _isPrintInfo; }
	
	public ObjectLookupQueueEntry(String dbname, String objectName, String source, String dependParent, int dependLevel)
	{
		_dbname       = dbname;
		_objectName   = objectName;
		_source       = source;
		_dependParent = dependParent;
		_dependLevel  = dependLevel;
	}
	
	@Override
	public String toString()
	{
		StringBuilder sb = new StringBuilder();
		sb.append(_dbname).append(":").append(_objectName);
		return sb.toString(); 
	}

	
	
	@Override
	public int hashCode()
	{
		return Objects.hash(_dbname, _objectName);
	}

	@Override
	public boolean equals(Object obj)
	{
		if ( this == obj ) return true;
		if ( obj == null ) return false;
		if ( getClass() != obj.getClass() ) return false;

		ObjectLookupQueueEntry other = (ObjectLookupQueueEntry) obj;
		return Objects.equals(_dbname, other._dbname) && Objects.equals(_objectName, other._objectName);
	}
}
