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
package com.asetune.cm;

import java.util.List;

import javax.swing.table.AbstractTableModel;

public abstract class CounterTableModel
extends AbstractTableModel
{
	private static final long serialVersionUID = 1L;

	abstract public int    getRowNumberForPkValue(String pkStr);
	abstract public String getPkValue(int row);
	abstract public List<String> getColNames();

	public int findColumn(String colName, boolean caseSensitive)
	{
		for (int c=0; c < getColumnCount(); c++) 
		{
			if ( caseSensitive ? colName.equals(getColumnName(c)) : colName.equalsIgnoreCase(getColumnName(c)) ) 
				return c;
		}
		return -1;
	}
	

}
