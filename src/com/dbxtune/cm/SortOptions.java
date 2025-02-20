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
package com.dbxtune.cm;

public class SortOptions
{
	private String  _colName           = null;
//	private boolean _isAssending       = true;
//	private boolean _isCaseInSensitive = false;
	private ColumnNameSensitivity _columnNameSensitivity = ColumnNameSensitivity.SENSITIVE;
	private SortOrder             _sortOrder             = SortOrder.ASCENDING;
	private DataSortSensitivity   _dataSortSensitivity   = DataSortSensitivity.SENSITIVE;
	
	public enum SortOrder
	{
		ASCENDING, DESCENDING
	};

	public enum DataSortSensitivity
	{
		SENSITIVE, IN_SENSITIVE
	};

	public enum ColumnNameSensitivity
	{
		SENSITIVE, IN_SENSITIVE
	};

	
//	public SortOptions(String  colName, boolean isAssending, boolean isCaseInSensitive)
//	{
//		_colName           = colName;          
//		_isAssending       = isAssending;      
//		_isCaseInSensitive = isCaseInSensitive;
//	}
	
	public SortOptions(String colName, ColumnNameSensitivity columnNameSensitivity, SortOrder sortOrder, DataSortSensitivity dataSortSensitivity)
	{
		_colName               = colName;
		_columnNameSensitivity = columnNameSensitivity;
		_sortOrder             = sortOrder;
		_dataSortSensitivity   = dataSortSensitivity;
	}

	public String getColumnName()
	{
		return _colName;
	}

	public boolean isColumnNameCaseSensitive()
	{
		return ColumnNameSensitivity.SENSITIVE.equals(_columnNameSensitivity);
	}
	
	public boolean isColumnNameCaseInSensitive()
	{
		return ! isColumnNameCaseSensitive();
	}

	public boolean isAscending()
	{
		return SortOrder.ASCENDING.equals(_sortOrder);
	}

	public boolean isCaseSensitive()
	{
		return DataSortSensitivity.SENSITIVE.equals(_dataSortSensitivity);
	}

	public boolean isCaseInSensitive()
	{
		return ! isCaseSensitive();
	}
}
