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
package com.asetune.sql.ddl;

import java.sql.Types;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;

/**
 * Hold a table definition (all columns and datatypes) to make it easier to check if a table is correctly defined in the database
 */
public class TableDefinition
{
	private String                                  _tabName;
	private LinkedHashMap<String, TableColumnEntry> _columns = new LinkedHashMap<>();
	private List<String>                            _pk = new ArrayList<>();
	
	private static class TableColumnEntry
	{
		private String  _name;
		private int     _jdbcType;
		private int     _len;
		private int     _prec;
		private int     _scale;
		private boolean _isNullable;
		
		public TableColumnEntry(String name, int jdbcType, int len, int prec, int scale, boolean allowNulls)
		{
			_name       = name;
			_jdbcType   = jdbcType;
			_len        = len;
			_prec       = prec;
			_scale      = scale;
			_isNullable = allowNulls;
		}
		
	}

	private void addColumn(TableColumnEntry entry)
	{
		if (_columns.containsKey(entry._name))
			throw new RuntimeException("Column name '"+entry._name+"' is already taken in table '"+_tabName+"'.");
		
		_columns.put(entry._name, entry);
	}

	public void addPkColumn(String colname)
	{
		if (_pk.contains(colname))
			throw new RuntimeException("Column name '"+colname+"' is already part of the primary key in table '"+_tabName+"'.");
		
		_pk.add(colname);
	}

	public void addStringColumn  (String name, int len, boolean n)             { addColumn( new TableColumnEntry(name, Types.VARCHAR,  len, -1, -1, n) ); }
	public void addClobColumn    (String name, boolean n)                      { addColumn( new TableColumnEntry(name, Types.CLOB,      -1, -1, -1, n) ); }
	public void addBlobColumn    (String name, boolean n)                      { addColumn( new TableColumnEntry(name, Types.BLOB,      -1, -1, -1, n) ); }
	public void addIntColumn     (String name, boolean n)                      { addColumn( new TableColumnEntry(name, Types.INTEGER,   -1, -1, -1, n) ); }
	public void addBigIntColumn  (String name, boolean n)                      { addColumn( new TableColumnEntry(name, Types.BIGINT,    -1, -1, -1, n) ); }
	public void addDatetimeColumn(String name, boolean n)                      { addColumn( new TableColumnEntry(name, Types.TIMESTAMP, -1, -1, -1, n) ); }
	public void addNumericColumn (String name, int prec, int scale, boolean n) { addColumn( new TableColumnEntry(name, Types.NUMERIC, -1, prec, scale, n) ); }

}
