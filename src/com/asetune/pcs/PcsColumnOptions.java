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
package com.asetune.pcs;

/**
 * Options for how to store specific columns in the Persistence Counter Store 
 * <p>
 * Here we specify for example: If the column needs to be (dictionary) compressed... (if the back end DBMS support that)
 * <p>
 * Set in any: CountersModel
 */
public class PcsColumnOptions
{
	public enum ColumnType
	{
		NORMAL,
		DICTIONARY_COMPRESSION
	};

	private ColumnType _colType = ColumnType.NORMAL;

	public PcsColumnOptions()
	{
	}

	public PcsColumnOptions(ColumnType colType)
	{
		_colType = colType;
	}
	
	
	public void setColumnType(ColumnType colType)
	{
		_colType = colType;
	}
	
	
	public ColumnType getColumnType()
	{
		return _colType;
	}
}
