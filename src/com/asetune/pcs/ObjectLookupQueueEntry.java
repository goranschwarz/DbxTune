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
package com.asetune.pcs;

public class ObjectLookupQueueEntry
{
	public String _dbname;
	public String _objectName;
	public String _source;
	public String _dependParent;
	public int    _dependLevel;

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
}
