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
package com.asetune.sql.ddl.model;

import java.sql.ResultSetMetaData;

import com.asetune.sql.ddl.DataTypeNotResolvedException;

public class TableColumn
{
	protected Table   _table;

	protected String  _colName        = null;
	protected int     _colPos         = -1;
	protected int     _colJdbcType    = -1;
	protected String  _colType        = null;
	protected int     _colLength      = -1;
	protected int     _colIsNullable  = -1;
	protected String  _colRemark      = null;
	protected String  _colDefault     = null;
	protected int     _colScale       = -1;
	protected boolean _isColAutoInc   = false;
	protected boolean _isColGenerated = false;


	
	public Table getParent() { return _table; }



	public boolean isColumnNullable()       { return _colIsNullable != ResultSetMetaData.columnNoNulls; }
	public String  getColumnJdbcTypeStr()   { return DataTypeNotResolvedException.getJdbcTypeAsString(_colJdbcType); }
	public int     getColumnJdbcType()      { return _colJdbcType; }
	public String  getColumnLabel()         { return _colName; }
	public int     getColumnLength()        { return _colLength; }
	public int     getColumnScale()         { return _colScale; }
	public int     getColumnDecimalDigits() { return getColumnScale(); }
	public String  getColumnDbmsDataType()  { return _colType; }
}
