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
package com.dbxtune.gui.swing;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import javax.swing.table.AbstractTableModel;
import javax.swing.table.TableModel;

public class TableModelSortable
extends AbstractTableModel
{
	private static final long serialVersionUID = 1L;

	private List<Row> _data = new ArrayList<>();
	private String[]  _columnNames;

	private static class Row
	{
		private Object[] values;

		public Row(Object[] values)
		{
			this.values = values.clone();
		}

		public Object getValue(int column)
		{
			return values[column];
		}
	}

	public TableModelSortable(String[] columnNames)
	{
		_columnNames = columnNames.clone();
	}

	// Copy constructor that accepts any TableModel
	public TableModelSortable(TableModel sourceModel)
	{
		// Copy column names
		_columnNames = new String[sourceModel.getColumnCount()];
		for (int col = 0; col < sourceModel.getColumnCount(); col++)
		{
			_columnNames[col] = sourceModel.getColumnName(col);
		}

		// Copy _data
		for (int row = 0; row < sourceModel.getRowCount(); row++)
		{
			Object[] rowData = new Object[sourceModel.getColumnCount()];
			for (int col = 0; col < sourceModel.getColumnCount(); col++)
			{
				rowData[col] = sourceModel.getValueAt(row, col);
			}
			_data.add(new Row(rowData));
		}
	}

	// Copy constructor specifically for another SortableTableModel (more efficient)
	public TableModelSortable(TableModelSortable sourceModel)
	{
		this._columnNames = sourceModel._columnNames.clone();
		for (Row row : sourceModel._data)
		{
			this._data.add(new Row(row.values));
		}
	}

	public void addRow(Object[] rowData)
	{
		_data.add(new Row(rowData));
		fireTableRowsInserted(_data.size() - 1, _data.size() - 1);
	}

	@Override
	public int getRowCount()
	{
		return _data.size();
	}

	@Override
	public int getColumnCount()
	{
		return _columnNames.length;
	}

	@Override
	public String getColumnName(int column)
	{
		return _columnNames[column];
	}

	@Override
	public Object getValueAt(int row, int column)
	{
		return _data.get(row).getValue(column);
	}

	@Override
	public Class<?> getColumnClass(int column)
	{
		if ( _data.size() > 0 )
		{
			Object value = getValueAt(0, column);
			if ( value != null )
			{
				return value.getClass();
			}
		}
		return Object.class;
	}

	public void sortByColumn(String columnName, boolean ascending)
	{
		int colPos = findColumn(columnName);
		
		if (colPos == -1)
			throw new RuntimeException("Column name '" + columnName + "' was not found in the table model.");

		sortByColumn(colPos, ascending);
	}

	public void sortByColumn(int column, boolean ascending)
	{
		Collections.sort(_data, new Comparator<Row>()
		{
			@SuppressWarnings({ "unchecked", "rawtypes" })
			@Override
			public int compare(Row r1, Row r2)
			{
				Object v1 = r1.getValue(column);
				Object v2 = r2.getValue(column);

				if ( v1 == null && v2 == null )
				{
					return 0;
				}
				else if ( v1 == null )
				{
					return ascending ? -1 : 1;
				}
				else if ( v2 == null )
				{
					return ascending ? 1 : -1;
				}

				int comparison;
				if ( v1 instanceof Comparable )
				{
					comparison = ((Comparable) v1).compareTo(v2);
				}
				else
				{
					comparison = v1.toString().compareTo(v2.toString());
				}

				return ascending ? comparison : -comparison;
			}
		});

		fireTableDataChanged();
	}
}
