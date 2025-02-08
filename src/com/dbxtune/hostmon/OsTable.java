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
package com.dbxtune.hostmon;

import java.lang.invoke.MethodHandles;
import java.sql.Timestamp;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.dbxtune.cm.CounterTableModel;
import com.dbxtune.utils.StringUtil;
import com.dbxtune.utils.TimeUtils;


public class OsTable
extends CounterTableModel
//implements ResultSet
{
	private static final Logger _logger = LogManager.getLogger(MethodHandles.lookup().lookupClass());
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


	//----------------------------------------------------------------------------------
	//-- getValueAsString
	//----------------------------------------------------------------------------------
	public String getValueAsString (int    rowId, int    colPos)                          { Object o = getValueAsObject     (rowId, colPos);  return (o==null)?"":o.toString(); }
	public String getValueAsString (int    rowId, String colname)                         { Object o = getValueAsObject     (rowId, colname, true); return (o==null)?"":o.toString(); }
	public String getValueAsString (int    rowId, String colname, boolean cs)             { Object o = getValueAsObject     (rowId, colname,   cs); return (o==null)?"":o.toString(); }
//	public String getValueAsString (String pkStr, String colname)                         { Object o = getValueAsObject     (pkStr, colname, true); return (o==null)?"":o.toString(); }
//	public String getValueAsString (String pkStr, String colname, boolean cs)             { Object o = getValueAsObject     (pkStr, colname,   cs); return (o==null)?"":o.toString(); }


	//----------------------------------------------------------------------------------
	//-- getValueAsObject
	//----------------------------------------------------------------------------------
	// Return the value of a cell by ROWID (rowId, ColumnName)
	public Object getValueAsObject(int rowId, int colId)
	{
		return getValueAsObject(rowId, colId, null);
	}

	// Return the value of a cell by ROWID (rowId, ColumnName)
	public Object getValueAsObject(int rowId, int colId, Object def)
	{
		if (colId < 0 || colId > getColumnCount())
		{
			if (_logger.isDebugEnabled()) 
				_logger.debug("getValue: column id " + colId + " is out of range. column Count is " + getColumnCount());
			return null;
		}
		if (getRowCount() <= rowId)
			return null;

		return getValueAt(rowId, colId);
	}

	// Return the value of a cell by ROWID (rowId, ColumnName)
	public Object getValueAsObject(int rowId, String colname, boolean caseSensitive)
	{
		return getValueAsObject(rowId, colname, caseSensitive, null);
	}

	// Return the value of a cell by ROWID (rowId, ColumnName)
	public Object getValueAsObject(int rowId, String colname, boolean caseSensitive, Object def)
	{
		int idCol = findColumn(colname, caseSensitive);
		if (idCol == -1)
		{
			if (_logger.isDebugEnabled()) 
				_logger.debug("getValue: Can't find the column '" + colname + "'.");
			return null;
		}
		if (getRowCount() <= rowId)
			return null;

		return getValueAt(rowId, idCol);
	}

	// 
	public Object getValueAsObject(String pkStr, String colname, boolean caseSensitive)
	{
		return getValueAsObject(pkStr, colname, caseSensitive, null);
	}

	/**
	 *  Return the value of a cell by ROWID (rowId, colId)
	 *  rowId starts at 0
	 *  colId starts at 
	 *  NOTE: note tested (2007-07-13)
	 */
	// Return the value of a cell by keyVal, (keyVal, ColumnName)
	public Object getValueAsObject(String pkStr, String colname, boolean caseSensitive, Object def)
	{
		// Get the rowId, if not found, return null
		int rowId = getRowNumberForPkValue(pkStr);
		if (rowId < 0)
		{
			if (_logger.isDebugEnabled())
				_logger.debug("getValue(pkStr='"+pkStr+"', colname='"+colname+"'): rowId="+rowId+": rowId < 0; return null");
			return def;
		}

		// Got object for the RowID and column name
		Object o = getValueAsObject(rowId, colname, caseSensitive);
		if (o == null)
		{
			if (_logger.isDebugEnabled()) 
				_logger.debug("getValue(pkStr='"+pkStr+"', colname='"+colname+"'): rowId="+rowId+": o==null; return null");
			return def;
		}

		return o;
	}




	//----------------------------------------------------------------------------------
	//-- getValueAsDouble
	//----------------------------------------------------------------------------------
	
	// 
	public Double getValueAsDouble(int rowId, int colPos)
	{
		return getValueAsDouble(rowId, colPos, null);
	}

	// 
	public Double getValueAsDouble(int rowId, int colPos, Double def)
	{
		Object o = getValueAsObject(rowId, colPos);
		if (o == null)
			return def;

		if (o instanceof Number)
			return Double.valueOf(((Number) o).doubleValue());
		else
			return Double.valueOf(Double.parseDouble(o.toString()));
	}

	// 
	public Double getValueAsDouble(int rowId, String colname, boolean caseSensitive)
	{
		return getValueAsDouble(rowId, colname, caseSensitive, null);
	}

	// 
	public Double getValueAsDouble(int rowId, String colname, boolean caseSensitive, Double def)
	{
		Object o = getValueAsObject(rowId, colname, caseSensitive);
		if (o == null)
			return def;

		if (o instanceof Number)
			return Double.valueOf(((Number) o).doubleValue());
		else
			return Double.valueOf(Double.parseDouble(o.toString()));
	}

	// 
	public Double getValueAsDouble(String pkStr, String colname, boolean caseSensitive)
	{
		return getValueAsDouble(pkStr, colname, caseSensitive, null);
	}

	// 
	public Double getValueAsDouble(String pkStr, String colname, boolean caseSensitive, Double def)
	{
		Object o = getValueAsObject(pkStr, colname, caseSensitive);
		if (o == null)
			return def;

		if (o instanceof Number)
			return Double.valueOf(((Number) o).doubleValue());
		else
			return Double.valueOf(Double.parseDouble(o.toString()));
	}


	//----------------------------------------------------------------------------------
	//-- getValueAsInteger
	//----------------------------------------------------------------------------------
	
	// 
	public Integer getValueAsInteger(int rowId, int colPos)
	{
		return getValueAsInteger(rowId, colPos, null);
	}

	// 
	public Integer getValueAsInteger(int rowId, int colPos, Integer def)
	{
		Object o = getValueAsObject(rowId, colPos);
		if (o == null)
			return def;

		if (o instanceof Number)
			return Integer.valueOf(((Number) o).intValue());
		else
			return Integer.valueOf(Integer.parseInt(o.toString()));
	}

	// 
	public Integer getValueAsInteger(int rowId, String colname, boolean caseSensitive)
	{
		return getValueAsInteger(rowId, colname, caseSensitive, null);
	}

	// 
	public Integer getValueAsInteger(int rowId, String colname, boolean caseSensitive, Integer def)
	{
		Object o = getValueAsObject(rowId, colname, caseSensitive);
		if (o == null)
			return def;

		if (o instanceof Number)
			return Integer.valueOf(((Number) o).intValue());
		else
			return Integer.valueOf(Integer.parseInt(o.toString()));
	}

	// 
	public Integer getValueAsInteger(String pkStr, String colname, boolean caseSensitive)
	{
		return getValueAsInteger(pkStr, colname, caseSensitive, null);
	}

	// 
	public Integer getValueAsInteger(String pkStr, String colname, boolean caseSensitive, Integer def)
	{
		Object o = getValueAsObject(pkStr, colname, caseSensitive);
		if (o == null)
			return def;

		if (o instanceof Number)
			return Integer.valueOf(((Number) o).intValue());
		else
			return Integer.valueOf(Integer.parseInt(o.toString()));
	}


	//----------------------------------------------------------------------------------
	//-- getValueAsLong
	//----------------------------------------------------------------------------------
	
	// 
	public Long getValueAsLong(int rowId, int colPos)
	{
		return getValueAsLong(rowId, colPos, null);
	}

	// 
	public Long getValueAsLong(int rowId, int colPos, Long def)
	{
		Object o = getValueAsObject(rowId, colPos);
		if (o == null)
			return def;

		if (o instanceof Number)
			return Long.valueOf(((Number) o).longValue());
		else
			return Long.valueOf(Long.parseLong(o.toString()));
	}

	// 
	public Long getValueAsLong(int rowId, String colname, boolean caseSensitive)
	{
		return getValueAsLong(rowId, colname, caseSensitive, null);
	}

	// 
	public Long getValueAsLong(int rowId, String colname, boolean caseSensitive, Long def)
	{
		Object o = getValueAsObject(rowId, colname, caseSensitive);
		if (o == null)
			return def;

		if (o instanceof Number)
			return Long.valueOf(((Number) o).longValue());
		else
			return Long.valueOf(Long.parseLong(o.toString()));
	}

	// 
	public Long getValueAsLong(String pkStr, String colname, boolean caseSensitive)
	{
		return getValueAsLong(pkStr, colname, caseSensitive, null);
	}

	// 
	public Long getValueAsLong(String pkStr, String colname, boolean caseSensitive, Long def)
	{
		Object o = getValueAsObject(pkStr, colname, caseSensitive);
		if (o == null)
			return def;

		if (o instanceof Number)
			return Long.valueOf(((Number) o).longValue());
		else
			return Long.valueOf(Long.parseLong(o.toString()));
	}


	//----------------------------------------------------------------------------------
	//-- getValueAsTimestamp
	//----------------------------------------------------------------------------------
	
	// 
	public Timestamp getValueAsTimestamp(int rowId, int colPos)
	{
		return getValueAsTimestamp(rowId, colPos, null);
	}

	// 
	public Timestamp getValueAsTimestamp(int rowId, int colPos, Timestamp def)
	{
		Object o = getValueAsObject(rowId, colPos);
		if (o == null)
			return def;

		if (o instanceof Timestamp)
			return (Timestamp) o;
		else
		{
			try { 
				return TimeUtils.parseToTimestamp(o.toString()); 
			} catch(ParseException ex) {
				_logger.warn("Problems parsing value '" + o + "' for rowId=" + rowId + ", colPos=" + colPos + ". Returning default value: " + def);
				return def;
			}
		}
	}

	// 
	public Timestamp getValueAsTimestamp(int rowId, String colname, boolean caseSensitive)
	{
		return getValueAsTimestamp(rowId, colname, caseSensitive, null);
	}

	// 
	public Timestamp getValueAsTimestamp(int rowId, String colname, boolean caseSensitive, Timestamp def)
	{
		Object o = getValueAsObject(rowId, colname, caseSensitive);
		if (o == null)
			return def;

		if (o instanceof Timestamp)
			return (Timestamp) o;
		else
		{
			try { 
				return TimeUtils.parseToTimestamp(o.toString()); 
			} catch(ParseException ex) {
				_logger.warn("Problems parsing value '" + o + "' for rowId=" + rowId + ", colname=" + colname + ". Returning default value: " + def);
				return def;
			}
		}
	}

	// 
	public Timestamp getValueAsTimestamp(String pkStr, String colname, boolean caseSensitive)
	{
		return getValueAsTimestamp(pkStr, colname, caseSensitive, null);
	}

	// 
	public Timestamp getValueAsTimestamp(String pkStr, String colname, boolean caseSensitive, Timestamp def)
	{
		Object o = getValueAsObject(pkStr, colname, caseSensitive);
		if (o == null)
			return def;

		if (o instanceof Timestamp)
			return (Timestamp) o;
		else
		{
			try { 
				return TimeUtils.parseToTimestamp(o.toString()); 
			} catch(ParseException ex) {
				_logger.warn("Problems parsing value '" + o + "' for pkStr=" + pkStr + ", colname=" + colname + ". Returning default value: " + def);
				return def;
			}
		}
	}


	//----------------------------------------------------------------------------------
	//-- getValueAsXXX
	//----------------------------------------------------------------------------------
	
	
	
	//---------------------------------------------------------
	// BEGIN implementing - ResultSet
	//---------------------------------------------------------
	//---------------------------------------------------------
	// END implementing - ResultSet
	//---------------------------------------------------------
}
