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
package com.dbxtune.gui.swing;

import javax.swing.table.AbstractTableModel;

/**
 * Simply an empty table model which wont throw IndexOutOfBounds exceptions.
 * <p>
 * The idea is to use this one instead of DefaultTableModel
 * 
 * @author gorans
 *
 */
@SuppressWarnings("serial")
public class EmptyTableModel 
extends AbstractTableModel
{
	@Override
	public int getRowCount()
	{
		return 0;
	}

	@Override
	public int getColumnCount()
	{
		return 0;
	}

	@Override
	public Object getValueAt(int rowIndex, int columnIndex)
	{
		return null;
	}
}
