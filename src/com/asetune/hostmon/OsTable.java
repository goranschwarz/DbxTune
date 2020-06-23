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
package com.asetune.hostmon;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import com.asetune.cm.CounterTableModel;
import com.asetune.utils.StringUtil;


public class OsTable
extends CounterTableModel
//implements ResultSet
{
	private static final long serialVersionUID = 1L;

	/** Column description of the table */
	private HostMonitorMetaData _metaData = null;

	/** When was this object created */
	private long _time       = System.currentTimeMillis();

	/** If a summary/average was made on several OsTables in TableSampleHolder, for how many ms did the span cover (first-last entry time)*/
	private long _sampleSpanTime = 0;

	/** Keep PrimaryKeys in it's own map */
	LinkedHashMap<String, OsTableRow> _pkMap   = new LinkedHashMap<String, OsTableRow>();
	/** The rows are kept in a separate list, has to be coordinated with the PK Map */
	ArrayList<OsTableRow>             _rowList = new ArrayList<OsTableRow>();

	/**
	 * Create a new OsTable
	 * @param md The column description of what's inside the table
	 */
	public OsTable(HostMonitorMetaData md)
	{
		_metaData = md;
	}

	/**
	 * If the MetaData was unknown/null when creating the object, then we have a chance to set it here. 
	 * @param metaData
	 */
	public void setMetaData(HostMonitorMetaData metaData)
	{
		_metaData = metaData;
	}

	/**
	 * Add a row/entry to the table
	 * @param rowEntry
	 */
	synchronized public void addRow(OsTableRow rowEntry)
	{
		String pk = rowEntry.getPk();
		if (pk != null && ! pk.equals(""))
			_pkMap.put(rowEntry.getPk(), rowEntry);
		_rowList.add(rowEntry);
	}

	/**
	 * Get a OsTableRow for a specific Primary Key
	 * @param pk the PK to fetch
	 * @return null if the PK was not found, otherwise the a OsTableRow object
	 */
	public OsTableRow getRowByPk(String pk)
	{
		return _pkMap.get(pk);
	}

	/**
	 * remove a record for a specific Primary Key
	 * @param pk the PK to remove
	 * @return null if the PK was not found, otherwise the a OsTableRow object removed
	 */
	public OsTableRow removeRowByPk(String pk)
	{
		OsTableRow entry = _pkMap.remove(pk);
		if (entry != null)
			_rowList.remove(entry);
		
		return entry;
	}

	/**
	 * Get a row based on the row number
	 * @param row row number
	 * @return a OsTableRow
     * @throws IndexOutOfBoundsException if the row is out of range (row < 0 || row >= size()).
	 */
	public OsTableRow getRow(int row)
	{
		return _rowList.get(row);
	}

	/**
	 * Get the PK Map, this so you can iterate over the Primary Keys
	 * @return
	 */
	public Map<String, OsTableRow> getPkMap()
	{
		return _pkMap;
	}

	/**
	 * Get when this Object was created
	 * @return
	 */
	public long getTime()
	{
		return _time;
	}

	/**
	 * If this is a summary table, then it means that several subsamples has been summarized into
	 * one table, this is the time differance between first and last entries that has been summarized.
	 * @param timeSpanInMs number of millisecond between first and last
	 */
	public void setSampleSpanTime(long timeSpanInMs)
	{
		_sampleSpanTime = timeSpanInMs;
	}

	/**
	 * If this is a summary table, then it means that several subsamples has been summarized into
	 * one table, this is the time differance between first and last entries that has been summarized.
	 * @return number of millisecond between first and last summary entries
	 */
	public long getSampleSpanTime()
	{
		return _sampleSpanTime;
	}

	/**
	 * Get the meta data for this table
	 * @return
	 */
	public HostMonitorMetaData getMetaData()
	{
		return _metaData;
	}

	/**
	 * Get a preview of what a table would look like.<br>
	 * Probably Used for debugging
	 * @return a String looking like a "table"
	 */
	public String toTableString()
	{
		StringBuilder sb = new StringBuilder();
		// Colnames
		for (HostMonitorMetaData.ColumnEntry ce : _metaData.getColumns())
		{
			if (ce._sqlColNum > 0)
				sb.append(StringUtil.left(ce._colName, ce._displayLength, false)).append(" ");
		}
		sb.append("\n");
		// ---- under the column name
		for (HostMonitorMetaData.ColumnEntry ce : _metaData.getColumns())
		{
			if (ce._sqlColNum > 0)
				sb.append(StringUtil.replicate("-", ce._displayLength)).append(" ");
		}
		sb.append("\n");

		// values
		for (OsTableRow row : _rowList)
		{
			for (HostMonitorMetaData.ColumnEntry ce : _metaData.getColumns())
			{
				if (ce._sqlColNum > 0)
				{
					String str = ce._isNumber ? 
							StringUtil.right(""+row.getValue(ce._sqlColNum), ce._displayLength) :
							StringUtil.left(""+row.getValue(ce._sqlColNum), ce._displayLength, false);
					sb.append(str).append(" ");
				}
			}
			sb.append("\n");
		}
		sb.append("Number of row(s)in table ").append(getRowCount()).append(".\n");

		return sb.toString();
	}

	//---------------------------------------------------------
	// BEGIN implementing - TableModel / AbstactTableModel / CounterTableModel
	//---------------------------------------------------------
	@Override
	synchronized public int getRowCount()
	{
		return _rowList.size();
	}

	@Override
	public int getColumnCount()
	{
		return _metaData.getColumnCount();
	}

	@Override
	public String getColumnName(int column)
	{
		// TableModel start at 0, ResultSetMetaData starts at 1
		// so fetch column + 1, since we use underlying ResultSetMetaData interface 
		return _metaData.getColumnName( column + 1 );
	}

	@Override
	public Class<?> getColumnClass(int column)
	{
		// TableModel start at 0, ResultSetMetaData starts at 1
		// so fetch column + 1, since we use underlying ResultSetMetaData interface 
		return _metaData.getColumnClass( column + 1 );
	}

	@Override
	public boolean isCellEditable(int row, int column)
	{
		return false;
	}

	@Override
	public Object getValueAt(int row, int column)
	{
		OsTableRow entry = getRow(row);      // rows    starts at 0
		return entry.getValue( column + 1 ); // columns starts at 1
	}

	@Override
	public void setValueAt(Object val, int row, int column)
	{
		// NOT implemented, this is a read-only storage
		OsTableRow entry = getRow(row);      // rows    starts at 0
		entry.setValue( column + 1 , val);   // columns starts at 1
	}

	@Override // CounterTableModel
	public int getRowNumberForPkValue(String pkStr)
	{
		OsTableRow row = getRowByPk(pkStr);
		return _rowList.indexOf(row);
	}

	@Override // CounterTableModel
	public String getPkValue(int rowId)
	{
		OsTableRow row = getRow(rowId);
		return row.getPk();
	}

	@Override // CounterTableModel
	public List<String> getColNames()
	{
		return _metaData.getColumnNames();
	}
	
	@Override
	public int findColumn(String columnName)
	{
		List<String> columns = _metaData.getColumnNames();
		return columns.indexOf(columnName);
	}
	//---------------------------------------------------------
	// END implementing - Table Model
	//---------------------------------------------------------


	
	
	
	//---------------------------------------------------------
	// BEGIN implementing - ResultSet
	//---------------------------------------------------------
	//---------------------------------------------------------
	// END implementing - ResultSet
	//---------------------------------------------------------
}
