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

/**
*/


package com.dbxtune.cm;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.StringReader;
import java.lang.invoke.MethodHandles;
import java.math.BigDecimal;
import java.sql.Date;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.sql.SQLWarning;
import java.sql.Statement;
import java.sql.Time;
import java.sql.Timestamp;
import java.sql.Types;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.dbxtune.gui.ResultSetTableModel;
import com.dbxtune.sql.ResultSetMetaDataCached;
import com.dbxtune.sql.conn.DbxConnection;
import com.dbxtune.utils.AseConnectionUtils;
import com.dbxtune.utils.AseSqlScript;
import com.dbxtune.utils.Configuration;
import com.dbxtune.utils.StringUtil;
import com.dbxtune.utils.TimeUtils;


public  class CounterSample 
extends CounterTableModel
{
    /** Log4j logging. */
	private static final Logger _logger = LogManager.getLogger(MethodHandles.lookup().lookupClass());
    private static final long serialVersionUID = 4786911833477005253L;

    public static final String PK_STR_DELIMITER = "|"; 
    
	private   boolean            _negativeDiffCountersToZero = true;
	protected String             _name           = null; // Used for debugging
	private   String[]           _diffColNames   = null; // if we need to do PK merging...
	protected CounterSample      _prevSample     = null; // used to calculate sample interval etc
	private   List<String>       _colNames       = null;
	private   List<Integer>      _colSqlType     = null;
	private   List<String>       _colSqlTypeName = null;
	private   List<String>       _colClassName   = null;
	private   boolean[]          _colIsPk        = null; // one boolean for each column
	private   int[]              _pkPosArray     = null; // one boolean for each column
	protected List<List<Object>> _rows           = null;

	/** List contains key<String>. position in List is the rowId */
	private ArrayList<String>    _rowidToKey     = null; 

	/** Map containing <Integer>, which is the rowId for this key<String> */
	private HashMap<String,Integer> _keysToRowid = null;

	/** Map that holds number of rows (merges) into this PrimaryKey, will be null if no PK duplicates exists
	 * NOTE: column name that reflects the merge count MUST be named 'dupMergeCount'/'dupRowCount'. */
	private HashMap<String,Integer> _pkDupCountMap = null; 
	
	protected Timestamp     _samplingTime  = null;
	protected long          _interval      = 0;

	/** every time the CounterSample is refreshed set this to System.currentTimeMillis() */
	protected long          _lastLocalSampleTime = 0;

	// If messages inside a ResultSet should be treated and appended to a column
	// this is used for the StatementCache query: 
	//     select show_plan(-1,SSQLID,-1,-1) as Showplan, * from monCachedStatement
	// The column name should be: msgAsColValue
	private static final String SPECIAL_COLUMN_msgAsColValue = "msgAsColValue";
	private int _pos_msgAsColValue = -1;

	// If the column name is 'SampleTimeInMs', simply fill in the time in ms between samples.
	// NOTE: not yet implemented.
	private static final String SPECIAL_COLUMN_sampleTimeInMs = "sampleTimeInMs";
	private int _pos_sampleTimeInMs = -1;

	private static final String SPECIAL_COLUMN_dupMergeCount = "dupMergeCount";
	private static final String SPECIAL_COLUMN_dupRowCount   = "dupRowCount";


//	static
//	{
//		_logger.setLevel(Level.TRACE);
//	}

	public CounterSample(String name, boolean negativeDiffCountersToZero, String[] diffColNames, CounterSample prevSample)
	{
		_name                       = name;
		_negativeDiffCountersToZero = negativeDiffCountersToZero;
		_diffColNames               = diffColNames;
		_prevSample                 = prevSample;
	}
	/**
	 * Used to clone the object
	 * @param sc the object to clone
	 * @param cloneRows Should we clear or copy the data rows
	 */
	public CounterSample(CounterSample sc, boolean cloneRows)
	{
		this(sc, cloneRows, sc._name);
	}
	/**
	 * Used internally to clone the object with a new name...
	 * @param cs the object to clone
	 * @param cloneRows Should we clear or copy the data rows
	 * @param name Set a new name to the created object 
	 */
	protected CounterSample(CounterSample cs, boolean cloneRows, String name)
	{
		_name                       = name;
		_keysToRowid                = cs._keysToRowid;
		_colNames                   = cs._colNames;
		_colSqlType                 = cs._colSqlType;
		_colSqlTypeName             = cs._colSqlTypeName;
		_colClassName               = cs._colClassName;
		_colIsPk                    = cs._colIsPk;
		_pkPosArray                 = cs._pkPosArray;
		_rowidToKey                 = cs._rowidToKey;
		_samplingTime               = cs._samplingTime;
		_interval                   = cs._interval;
		_lastLocalSampleTime        = cs._lastLocalSampleTime;
		_negativeDiffCountersToZero = cs._negativeDiffCountersToZero;
		_diffColNames               = cs._diffColNames;
//		_prevSample                 = sc._prevSample;     // should we really copy this one...
		_prevSample                 = null;               // copy this causes memory leaks...

		if (cloneRows)
		{
			_rows        = cs._rows        == null ? null : new ArrayList<List<Object>>(cs._rows);
			_keysToRowid = cs._keysToRowid == null ? null : new HashMap<String,Integer>(cs._keysToRowid);
			_rowidToKey  = cs._rowidToKey  == null ? null : new ArrayList<String>      (cs._rowidToKey);
		}
		else
		{
			_rows        = cs._rows        == null ? null : new ArrayList<List<Object>>();
			_keysToRowid = cs._keysToRowid == null ? null : new HashMap<String,Integer>();
			_rowidToKey  = cs._rowidToKey  == null ? null : new ArrayList<String>();
		}
	}


	
	/*---------------------------------------------------
	** BEGIN: implementing TableModel or overriding AbstractTableModel
	**---------------------------------------------------
	* public int getRowCount();
	* public int getColumnCount();
	* public String getColumnName(int i);
	* public Class getColumnClass(int i);
	* public boolean isCellEditable(int i, int j);
	* public Object getValueAt(int i, int j);
	* public void setValueAt(Object obj, int i, int j);
	* public void addTableModelListener(TableModelListener tablemodellistener);
	* public void removeTableModelListener(TableModelListener tablemodellistener);
	*/
	@Override
	public int getRowCount()
	{
		return (_rows == null) ? 0 : _rows.size();
	}

	@Override
	public int getColumnCount()
	{
		return _colNames == null ? 0 : _colNames.size();
	}

	@Override
	public String getColumnName(int colid)
	{
		if (_colNames == null || colid < 0 || colid >= getColumnCount())
			return null;
		return _colNames.get(colid);
	}

	/**
	 * Index starting at 0
	 */
	@Override 
	public Class<?> getColumnClass(int colid)
	{
		if (getRowCount() == 0)
			return Object.class;

		Object o = getValueAt(0, colid);
		if (o == null)
		{
			if (_colSqlType == null)
			{
				if (_logger.isDebugEnabled())
					_logger.debug("getColumnClass(colid="+colid+"): _colSqlType is null... returning Object.class intead");

				return Object.class;
			}
			else
			{
				int jdbcType = _colSqlType.get(colid);
				return ResultSetTableModel.getJavaClassFromJdbcType(jdbcType);
			}
		}
		else
		{
			// return class from the underlying data
			
			Class<?> clazz = o!=null ? o.getClass() : Object.class;
			if (o instanceof Timestamp)
				return Object.class;

			return clazz;
		}
	}

	@Override
	public boolean isCellEditable(int i, int j)
	{
		return false;
	}

	@Override
	public Object getValueAt(int row, int col)
	{
		if (_rows == null || row < 0 || col < 0)
			return null;

		//
		// JTable/JXTable in some cases, when:
		// - refreshing tables
		// - and rows has been deleted (from the model)
		// - and Sorter has been installed
		// - and Highlighters are used
		// - and doing JXTable.packAll() from within tableChanged() method for TableModelListener's
		// probably in some other cases as well....
		//
		// In some circumstances it tries to get a rows that is still in the view,
		// but has been deleted/removed from the model.
		// So lets be prepared and check for this... (less Exception creation, 
		// which could be expensive) and just return null in those cases.
		//
//if (row==0&&col==0)
//	System.out.println("Accessing: (0,0), CounterSample.name="+_name+", getRowCount=()"+getRowCount()+", getColumnCount()="+getColumnCount()+", Thread='"+Thread.currentThread().getName()+"'.");

		if (row >= _rows.size())
		{
			// Write a ';' might give me a clue that something is still not correct...
			// But writing a whole exception to the errorlog might be overkill in "normal" mode.
			System.out.print(";");
//			new Exception("getValueAt(r="+row+",c="+col+")-name="+_name+": Dummy exception from thread '("+Thread.currentThread().getName()+")'.").printStackTrace();
			
			if (_logger.isDebugEnabled())
			{
				try {_rows.get(row);}
				catch(IndexOutOfBoundsException e) 
				{
					_logger.debug(getName()+": ERROR Accessing: getValueAt(row="+row+", col="+col+"), CounterSample.name="+_name+", model.size()="+_rows.size()+", Thread='"+Thread.currentThread().getName()+"', Exception="+e, e);
//					System.out.println("ERROR Accessing: getValueAt(row="+row+", col="+col+"), CounterSample.name="+_name+", model.size()="+_rows.size()+", Thread='"+Thread.currentThread().getName()+"', Exception="+e);
//					e.printStackTrace();
				}
			}
			return null;
		}

		// GET THE ROW NOW
		try
		{
			return _rows.get(row).get(col);
		}
		catch (IndexOutOfBoundsException e)
		{
			_logger.warn(getName()+": getValueAt(row="+row+", col="+col+"): _rows.size()="+_rows.size()+", CounterSample.name="+_name+", Thread='"+Thread.currentThread().getName()+"', returning NULL, IndexOutOfBoundsException... "+e.getMessage());
		}
		catch (NullPointerException e)
		{
			_logger.warn(getName()+": getValueAt(row="+row+", col="+col+"): CounterSample.name="+_name+", Thread='"+Thread.currentThread().getName()+"', returning NULL, NullPointerException... "+e.getMessage());
		}
		return null;
	}

	/** sets value in a cell, if the cell isn't there, "create" it... */
	@Override
	public void setValueAt(Object value, int row, int col)
	{
		if (_rows == null)
			_rows = new ArrayList<List<Object>>();

		// Expand rows to correct numbers, if there wasn't enough
		if ( ! (row < _rows.size()) )
			for (int i=_rows.size(); i<=row; i++)
				_rows.add(new ArrayList<Object>(getColumnCount()));

		// Expand cols to correct numbers, if there wasn't enough
		List<Object> r = _rows.get(row);
		if ( ! (col < r.size()) )
			for (int i=r.size(); i<=col; i++)
				r.add("-empty-");

		// Set the value
		r.set(col, value);
	}

	/**
	 * Set value <br>
	 * If column name or row is NOT found, a error message will be printed (no Exception will be thrown) 
	 * 
	 * @param value        The value to set 
	 * @param row          Row number (starting at 0)
	 * @param colName      Column name to set (This is case SENSITIVE)
	 */
	public void setValueAt(Object value, int row, String colName)
	{
		setValueAt(value, row, colName, true, false);
	}
	
	/**
	 * Set value 
	 * If column name or row is NOT found, a error message will be printed (no Exception will be thrown) 
	 * 
	 * @param value             The value to set 
	 * @param row               Row number (starting at 0)
	 * @param colName           Column name to set
	 * @param caseSensitive     If the column name search should be Case Sensitive)
	 */
	public void setValueAt(Object value, int row, String colName, boolean caseSensitive)
	{
		setValueAt(value, row, colName, true, false);
	}
	
	/**
	 * Set value 
	 * If column name or row is NOT found 
	 * 
	 * @param value             The value to set 
	 * @param row               Row number (starting at 0)
	 * @param colName           Column name to set
	 * @param caseSensitive     If the column name search should be Case Sensitive)
	 * @param throwException    if ColumnName wasn't found; throw an exception, otherwise a error message will be printed
	 */
	public void setValueAt(Object value, int row, String colName, boolean caseSensitive, boolean throwException)
	throws ColumnNameNotFoundException
	{
		int col = findColumn(colName, caseSensitive);
		
		if (col == -1)
		{
			ColumnNameNotFoundException ex = new ColumnNameNotFoundException(colName, this);
			if (throwException)
				throw ex;
			else
				_logger.warn(ex.getMessage(), ex);

			return;
		}

		// Set the value
		setValueAt(value, row, col);
	}
	
	/**
	 * Exception used in setValueAt(...) if column names wasn't found
	 */
	public static class ColumnNameNotFoundException
	extends RuntimeException
	{
		private static final long serialVersionUID = 1L;

		public ColumnNameNotFoundException(String colName, CounterSample cs)
		{
			super("Column name '" + colName + "' could not be found in CounterSample named '" + cs.getName() + "'.");
		}
	}
	
//	public void addTableModelListener(TableModelListener tablemodellistener)
//	{
//	}
//	public void removeTableModelListener(TableModelListener tablemodellistener)
//	{
//	}

	/*---------------------------------------------------
	** END: implementing TableModel or overriding AbstractTableModel
	**---------------------------------------------------
	*/

	public String getName()
	{
		return _name;
	}

	public void setSampleTime(Timestamp ts)
	{
		_samplingTime = ts;
	}

	public Timestamp getSampleTime()
	{
		return _samplingTime;
	}

	public long getSampleTimeAsLong()
	{
		return _samplingTime.getTime();
	}

	public int getSampleInterval()
	{
		return (int) _interval;
	}

	public void setSampleInterval(long interval)
	{
		_interval = interval;
	}

	public long getLastLocalSampleTime()
	{ 
		return _lastLocalSampleTime; 
	}

	public boolean getNegativeDiffCountersToZero()
	{
		return _negativeDiffCountersToZero;
	}
	
	public void setColumnNames(List<String> cols)
	{
		_colNames = new ArrayList<String>(cols);
	}

	public void setSqlType(List<Integer> sqlTypes)
	{
		_colSqlType = new ArrayList<Integer>(sqlTypes);
	}

	public void setSqlTypeNames(List<String> types)
	{
		_colSqlTypeName = new ArrayList<String>(types);
	}

	public void setColClassName(List<String> classes)
	{
		_colClassName = new ArrayList<String>(classes);
	}
	
	public void setPkColArray(boolean[] ba)
	{
		_colIsPk = ba;
	}

	public void initPkStructures()
	{
		_keysToRowid    = new HashMap<String,Integer>();
		_rowidToKey     = new ArrayList<String>();
	}

	@Override
	public List<String> getColNames()
	{
		return _colNames;
	}

	public int getColId(String colName)
	{
		if (_colNames == null)
			return -1;

		return _colNames.indexOf(colName);
	}

	public String getColClassName(int colId)
	{
		return _colClassName.get(colId);
	}

	public int getColSqlType(int colId)
	{
		return _colSqlType.get(colId);
	}

	public String getColSqlTypeName(int colId)
	{
		return _colSqlTypeName.get(colId);
	}

	@Override
	public String getPkValue(int row)
	{
		if (_rowidToKey != null && row >= 0 && row < _rowidToKey.size())
			return _rowidToKey.get(row);

		return null;
	}
	@Override
	public int getRowNumberForPkValue(String pkStr)
	{
//System.out.println();
//System.out.println();
//System.out.println("getRowNumberForPkValue(): pkStr='"+pkStr+"', _keysToRowid="+StringUtil.toCommaStr(_keysToRowid, "=", "\n"));
		if (_keysToRowid == null)
			return -1;

		Integer i = _keysToRowid.get(pkStr);
		if (i == null)
		{
//System.out.println("XXXXXXX: first pk lookup on '"+pkStr+"' failed, try fallback.");
			// PK has ':' appended to the END of the str, so lets check for this as well
			// NOTE: this might change in the future...
			i = _keysToRowid.get(pkStr+PK_STR_DELIMITER);
			if (i == null)
			{
//System.out.println("XXXXXXX: <<< SECOND pk lookup on '"+pkStr+"' FAILED.... RETURNING null");
				//throw new RuntimeException("Couldn't find pk '"+pkStr+"'.");
				return -1;
			}
		}
		if (_logger.isDebugEnabled())
			_logger.debug(getName()+": getRowNumberForPkValue(pk='"+pkStr+"'), returns="+i);

//System.out.println("<<< "+getName()+"-getRowNumberForPkValue("+pkStr+"): returns="+i);
		return i;
	}

	/**
	 * Helper method to create a PK String/Object, which is used in the HashMap to identify unique rows.<br>
	 * 
	 * @param pk
	 * @return
	 */
	public static String createPkStr(String... pk)
	{
		StringBuilder sb = new StringBuilder();
		for (String pkPart : pk)
			sb.append(pkPart).append(PK_STR_DELIMITER);

		return sb.toString();
	}

	/** colId starts at 0 */
	public boolean isColPartOfPk(int colId)
	{
		if (_colIsPk == null)
			return false;

		if (colId < 0 || colId > getColumnCount() )
			return false; // or should we: throw new IndexOutOfBoundsException("description");

		return _colIsPk[colId];
	}

	public boolean hasPkCols()
	{
		if (_colIsPk == null)
			return false;
		return true;
	}

	protected void checkWarnings(CountersModel cm, Statement st) 
	throws SQLException
	{
		boolean hasWarning = false;
		StringBuilder sb = new StringBuilder();
		try
		{
			List<IgnoreSqlWarning> cmIgnoreWarnings = cm.getIgnoreSqlWarnings();
			
			SQLWarning w = st.getWarnings();
			while (w != null)
			{
				int    errorCode = w.getErrorCode();
				String sqlState  = w.getSQLState();

				boolean ignoreWarning = false;
				
				// Check the CM's list of Warnings to be "discarded"
				for (IgnoreSqlWarning ignoreEntry : cmIgnoreWarnings)
				{
					if (ignoreEntry.ignore(w))
					{
						ignoreWarning = true;
					}
				}
				
				// skip/disregard: 010P4: An output parameter was received and ignored.
				if (sqlState != null && sqlState.equals("010P4")) 
				{
					ignoreWarning = true;
				}

				// skip/disregard: 6002: A SHUTDOWN command is already in progress. Please log off.
				// This will be handled at the start on next refresh...
				if (errorCode == 6002) 
				{
					ignoreWarning = true;
				}

				// Not handled...
				if ( ! ignoreWarning )
				{
					hasWarning = true;
	
					String wStr = w.toString();
					if (wStr == null)
						hasWarning = false;
					else if (wStr.trim().equals(""))
						hasWarning = false;
					else
					{
						wStr = "DbmsMsgNum="+w.getErrorCode()+", " + w.toString();
						_logger.warn("CounterSample("+getName()+").Warning : " + wStr);
						sb.append(wStr).append("\n");
					}
				}

				w = w.getNextWarning();
			}
		}
		catch (SQLException ex)
		{
			_logger.warn("CounterSample("+getName()+").getWarnings : " + ex);
//			ex.printStackTrace();
		}
		if (hasWarning)
		{
			throw new SQLException("SQL Warning in("+getName()+") Messages: "+sb);
		}
	}

	/** Backward compatibility if the rs.getXXX(col) is not working as expected */
	private static boolean _resultsSet_getObject = Configuration.getCombinedConfiguration().getBooleanProperty("CounterSample.ResultsSet.getObject", false);
	/**
	 * Get datavalue for a specific column in the ResultSet<br>
	 * This is done to try to workaround problem that the JDBC driver in some cases seems to 
	 * return the wrong Java Object when using rs.getObject(col)<br>
	 * <pre>
	 * I guess it's the case when:
	 * java.lang.ClassCastException: java.lang.Integer cannot be cast to java.lang.Short
	 * at java.lang.Short.compareTo(Unknown Source)
	 * at org.jdesktop.swingx.sort.DefaultSortController$ComparableComparator.compare(DefaultSortController.java:294)
	 * at javax.swing.DefaultRowSorter.compare(Unknown Source)
	 * at javax.swing.DefaultRowSorter.access$100(Unknown Source)
	 * at java.swing.DefaultRowSorter$Row.compareTo(Unknown Source)
	 * at javax.swing.DefaultRowSorter$Row.compareTo(Unknown Source)
	 * at java.util.Arrays.mergeSort(Unknown Source)
	 * at ...
	 * </pre>
	 */
	private Object getDataValue(ResultSet rs, int col, ResultSetMetaData rsmd) 
	throws SQLException
	{
		// Backward compatibility if the rs.getXXX(col) is not working as expected  
		if (_resultsSet_getObject)
		{
			return rs.getObject(col);
		}

		if (_colSqlType == null)
		{
			_logger.error(getName()+": colSqlType are null, this should not happen, returning Object via 'rs.getObject(col)'.");
			return rs.getObject(col);
		}

		// For Primitive Data types (int, etc...) return a Java "null" instead of what getXxx() method dictates.
		boolean pdt2null = false; // Lets TEST how much this will *break* before we start to enable this.
//		pdt2null = true; // Only for testing so far

		// Return the "object" via getXXX method for "known" datatypes
		int objSqlType = rsmd == null ? _colSqlType.get(col - 1) : rsmd.getColumnType(col);
		switch (objSqlType)
		{
//		case java.sql.Types.BIT:          return rs.getBoolean(col);
//		case java.sql.Types.TINYINT:      return rs.getByte(col);
//		case java.sql.Types.SMALLINT:     return rs.getShort(col);
//		case java.sql.Types.INTEGER:      return rs.getInt(col);
//		case java.sql.Types.BIGINT:       return rs.getLong(col);
//		case java.sql.Types.FLOAT:        return rs.getFloat(col);
//		case java.sql.Types.REAL:         return rs.getFloat(col);
//		case java.sql.Types.DOUBLE:       return rs.getDouble(col);
		
		// Primitive data types
		case java.sql.Types.BIT:          {boolean val = rs.getBoolean(col); return pdt2null && rs.wasNull() ? null : val; }
		case java.sql.Types.TINYINT:      {byte    val = rs.getByte(col);    return pdt2null && rs.wasNull() ? null : val; }
		case java.sql.Types.SMALLINT:     {short   val = rs.getShort(col);   return pdt2null && rs.wasNull() ? null : val; }
		case java.sql.Types.INTEGER:      {int     val = rs.getInt(col);     return pdt2null && rs.wasNull() ? null : val; }
		case java.sql.Types.BIGINT:       {long    val = rs.getLong(col);    return pdt2null && rs.wasNull() ? null : val; }
		case java.sql.Types.FLOAT:        {float   val = rs.getFloat(col);   return pdt2null && rs.wasNull() ? null : val; }
		case java.sql.Types.REAL:         {float   val = rs.getFloat(col);   return pdt2null && rs.wasNull() ? null : val; }
		case java.sql.Types.DOUBLE:       {double  val = rs.getDouble(col);  return pdt2null && rs.wasNull() ? null : val; }

		// Data types where getXxx returns Objects, or "null" when when it's a SQL-NULL 
		case java.sql.Types.NUMERIC:      return rs.getBigDecimal(col);
		case java.sql.Types.DECIMAL:      return rs.getBigDecimal(col);
		case java.sql.Types.CHAR:         return rs.getString(col);
		case java.sql.Types.VARCHAR:      return rs.getString(col);
		case java.sql.Types.LONGVARCHAR:  return rs.getString(col);
		case java.sql.Types.DATE:         return rs.getDate(col);
		case java.sql.Types.TIME:         return rs.getTime(col);

		// Reading Timestamp value could problematic from monStatementCacheDetails...
		// So if it fails, write som trace information about this and, return a "default value"
		case java.sql.Types.TIMESTAMP:
		{
			try 
			{
				return rs.getTimestamp(col);
			}
			catch (Throwable t)
			{
				String colName  = getColumnName(col);
				String cmName   = getName();
				Object rawValue = null;
				// Check if we can read the raw value...
				try {                  rawValue = rs.getObject(col); }
				catch (Throwable t2) { rawValue = "Caught Exception reading RawValue using rs.getObject(col), My guess it's a corrupt Timestamp value."; }

				_logger.warn(getName()+": Problems reading column pos="+col+", name='"+colName+"', RawValue='"+rawValue+"'. in CM '"+cmName+"'. returning a 'default' value instead: return new Timestamp(0); Caught="+t); 
				return new Timestamp(0);
			}
		}
		case java.sql.Types.BINARY:        return StringUtil.bytesToHex(BINARY_PREFIX, rs.getBytes(col), BINARY_TOUPPER);
		case java.sql.Types.VARBINARY:     return StringUtil.bytesToHex(BINARY_PREFIX, rs.getBytes(col), BINARY_TOUPPER);
		case java.sql.Types.LONGVARBINARY: return StringUtil.bytesToHex(BINARY_PREFIX, rs.getBytes(col), BINARY_TOUPPER);
		case java.sql.Types.NULL:          return rs.getObject(col);   // use OBJECT
		case java.sql.Types.OTHER:         return rs.getObject(col);   // use OBJECT
		case java.sql.Types.JAVA_OBJECT:   return rs.getObject(col);   // use OBJECT
		case java.sql.Types.DISTINCT:      return rs.getObject(col);   // use OBJECT
		case java.sql.Types.STRUCT:        return rs.getObject(col);   // use OBJECT
		case java.sql.Types.ARRAY:         return rs.getObject(col);   // use OBJECT
		case java.sql.Types.BLOB:          return StringUtil.bytesToHex(BINARY_PREFIX, rs.getBytes(col), BINARY_TOUPPER);
		case java.sql.Types.CLOB:          return rs.getString(col);
		case java.sql.Types.REF:           return rs.getObject(col);   // use OBJECT
		case java.sql.Types.DATALINK:      return rs.getObject(col);   // use OBJECT
		case java.sql.Types.BOOLEAN:       return rs.getBoolean(col);

		//------------------------- JDBC 4.0 -----------------------------------
		case java.sql.Types.ROWID:         return rs.getObject(col);   // use OBJECT
		case java.sql.Types.NCHAR:         return rs.getString(col);
		case java.sql.Types.NVARCHAR:      return rs.getString(col);
		case java.sql.Types.LONGNVARCHAR:  return rs.getString(col);
		case java.sql.Types.NCLOB:         return rs.getString(col);
		case java.sql.Types.SQLXML:        return rs.getString(col);

		//------------------------- UNHANDLED TYPES  ---------------------------
		default:
			//return rs.getObject(col);
			return rs.getString(col);
		}
	}
	private static final String  BINARY_PREFIX  = Configuration.getCombinedConfiguration().getProperty(       ResultSetTableModel.PROPKEY_BINERY_PREFIX,  ResultSetTableModel.DEFAULT_BINERY_PREFIX);
	private static final boolean BINARY_TOUPPER = Configuration.getCombinedConfiguration().getBooleanProperty(ResultSetTableModel.PROPKEY_BINARY_TOUPPER, ResultSetTableModel.DEFAULT_BINARY_TOUPPER);

	@SuppressWarnings("unused")
	private Object fixNullValue(int col)
	{
		if (_colSqlType == null)
		{
			_logger.error(getName()+": colSqlType are null, this should not happen.");
			return new Object();
		}
		int objSqlType = _colSqlType.get(col - 1);

		switch (objSqlType)
		{
		case java.sql.Types.BIT:          return Boolean.valueOf(false);
		case java.sql.Types.TINYINT:      return Byte.valueOf(Byte.parseByte("0"));
		case java.sql.Types.SMALLINT:     return Short.valueOf(Short.parseShort("0"));
		case java.sql.Types.INTEGER:      return Integer.valueOf(0);
		case java.sql.Types.BIGINT:       return Long.valueOf(0);
		case java.sql.Types.FLOAT:        return Float.valueOf(0);
		case java.sql.Types.REAL:         return Float.valueOf(0);
		case java.sql.Types.DOUBLE:       return Double.valueOf(0);
		case java.sql.Types.NUMERIC:      return BigDecimal.valueOf(0);
		case java.sql.Types.DECIMAL:      return BigDecimal.valueOf(0);
		case java.sql.Types.CHAR:         return "";
		case java.sql.Types.VARCHAR:      return "";
		case java.sql.Types.LONGVARCHAR:  return "";
		case java.sql.Types.DATE:         return new Date(0);
		case java.sql.Types.TIME:         return new Time(0);
		case java.sql.Types.TIMESTAMP:    return new Timestamp(0);
//		case java.sql.Types.BINARY:       return new byte[0];
//		case java.sql.Types.VARBINARY:    return new byte[0];
//		case java.sql.Types.LONGVARBINARY:return new byte[0];
		case java.sql.Types.BINARY:       return null;
		case java.sql.Types.VARBINARY:    return null;
		case java.sql.Types.LONGVARBINARY:return null;
		case java.sql.Types.NULL:         return null;
		case java.sql.Types.OTHER:        return null;
		case java.sql.Types.JAVA_OBJECT:  return new Object();
		case java.sql.Types.DISTINCT:     return "-DISTINCT-";
		case java.sql.Types.STRUCT:       return "-STRUCT-";
		case java.sql.Types.ARRAY:        return "-ARRAY-";
		case java.sql.Types.BLOB:         return "";
		case java.sql.Types.CLOB:         return "";
		case java.sql.Types.REF:          return "-REF-";
		case java.sql.Types.DATALINK:     return "-DATALINK-";
		case java.sql.Types.BOOLEAN:      return Boolean.valueOf(false);
		default:
			_logger.error(getName()+": Unknow SQL datatype, when translating a NULL value.");
			return new Object();
		}
	}

//	public boolean getCnt(CountersModel cm, Connection conn, String sql, List<String> pkList)
//	throws SQLException
//	{
//		int queryTimeout = cm.getQueryTimeout();
//		if (_logger.isDebugEnabled())
//			_logger.debug(_name+": queryTimeout="+queryTimeout);
//
//		try
//		{
//			String sendSql = "select getdate() \n" + sql;
//
//			Statement stmnt = conn.createStatement();
//			ResultSet rs;
//
//			stmnt.setQueryTimeout(queryTimeout); // XX seconds query timeout
//			if (_logger.isDebugEnabled())
//				_logger.debug("QUERY_TIMEOUT="+queryTimeout+", for SampleCnt='"+_name+"'.");
//
//			_rows   = new ArrayList<List<Object>>();
//
//			//FIXME: allow 'go' in the string, then we should send multiple batches
//			//       this will take care about creating tempdb tables prior to executing a batch that depends on it. 
//
//			if (_logger.isTraceEnabled())
//			{
//				_logger.trace("Sending SQL: "+sendSql);
//			}
//
//			int rsNum = 0;
//			int rowsAffected = 0;
//			boolean hasRs = stmnt.execute(sendSql);
//			checkWarnings(stmnt);
//			do
//			{
//				if (hasRs)
//				{
//					// Get next result set to work with
//					rs = stmnt.getResultSet();
//					checkWarnings(stmnt);
//
//					if (rsNum == 0)
//					{
//						while(rs.next())
//							samplingTime = rs.getTimestamp(1);
//					}
//					else
//					{
//						ResultSetMetaData rsmd = rs.getMetaData();
//						if ( ! cm.hasResultSetMetaData() )
//							cm.setResultSetMetaData(rsmd);
//
//						if (readResultset(cm, rs, rsmd, pkList, rsNum))
//							rs.close();
//
//						checkWarnings(stmnt);
//					}
//
//					rsNum++;
//				}
//
//				// Treat update/row count(s)
//				rowsAffected = stmnt.getUpdateCount();
//				if (rowsAffected >= 0)
//				{
//				}
//
//				// Check if we have more result sets
//				hasRs = stmnt.getMoreResults();
//
//				_logger.trace( "--hasRs="+hasRs+", rsNum="+rsNum+", rowsAffected="+rowsAffected );
//			}
//			while (hasRs || rowsAffected != -1);
//
//			checkWarnings(stmnt);
//
//			// Close the statement
//			stmnt.close();
//
//			return true;
//		}
//		catch (SQLException sqlEx)
//		{
//			_logger.warn("CounterSample("+_name+").getCnt : ErrorCode=" + sqlEx.getErrorCode() + ", Message=|" + sqlEx.getMessage() + "|. SQL: "+sql, sqlEx);
//			if (sqlEx.toString().indexOf("SocketTimeoutException") > 0)
//			{
//				_logger.info("QueryTimeout in '"+_name+"', with query timeout '"+queryTimeout+"'. This can be changed with the config option '"+_name+".queryTimeout=seconds' in the config file.");
//			}
//
//			//return false;
//			throw sqlEx;
//		}
//	}

	// special stuff:
	// - Messages on the ResultSet is stored in a column named 'msgAsColValue'
	// - check the variable "_pos_msgAsColValue" for more...
	//
	// if The ResultSet contains a column named "msgAsColValue" or similar, stuff the message in that column
	// The intention for this is to fix: 
	//    select show_plan(-1,SSQLID,-1,-1) as Showplan, * from monCachedStatement
	// the above query delivers: msg + resultset for row row 1, msg + resultset for row row 2...
	// Example:
	//
	// SELECT InstanceID, SSQLID, 
	//        show_plan(-1,SSQLID,-1,-1) as Showplan, 
	//        Hashkey, UserID, SUserID, DBID, 
	//        show_cached_text(SSQLID) as sqltext 
	// FROM monCachedStatement
	// 
	// Msg: QUERY PLAN FOR STATEMENT 1 (at line 1).
	// Msg:
	// Msg:
	// Msg:    STEP 1
	// Msg:        The type of query is SELECT.
	// Msg:
	// Msg:        1 operator(s) under root
	// Msg:
	// Msg:       |ROOT:EMIT Operator (VA = 1)
	// Msg:       |
	// Msg:       |   |SCAN Operator (VA = 0)
	// Msg:       |   |  FROM TABLE
	// Msg:       |   |  monDeadLock
	// Msg:       |   | External Definition: $monCachedStatement
	// 
	// Rs row 1: InstanceID SSQLID      Showplan    Hashkey     UserID      SUserID     DBID
	// Rs row 1:         sqltext
	// Rs row 1: ---------- ----------- ----------- ----------- ----------- ----------- ------
	// Rs row 1:         --------------------------------------------------------------------------------------------------------------------
	// Rs row 1:          0    71220424           0  1836678972           1           1      1
	// Rs row 1:         select * from monDeadLock
	// 
	// Msg: QUERY PLAN FOR STATEMENT 1 (at line 1).
	// Msg:
	// Msg:
	// Msg:    STEP 1
	// Msg:        The type of query is SELECT.
	// Msg:
	// Msg:        2 operator(s) under root
	// Msg:
	// Msg:       |ROOT:EMIT Operator (VA = 2)
	// Msg:       |
	// Msg:       |   |SCALAR AGGREGATE Operator (VA = 1)
	// Msg:       |   |  Evaluate Ungrouped MAXIMUM AGGREGATE.
	// Msg:       |   |
	// Msg:       |   |   |SCAN Operator (VA = 0)
	// Msg:       |   |   |  FROM TABLE
	// Msg:       |   |   |  monWaitEventInfo
	// Msg:       |   |   | External Definition: $monCachedStatement
	// 
	// Rs row 2: InstanceID SSQLID      Showplan    Hashkey     UserID      SUserID     DBID
	// Rs row 2:         sqltext
	// Rs row 2: ---------- ----------- ----------- ----------- ----------- ----------- ------
	// Rs row 2:         --------------------------------------------------------------------------------------------------------------------
	// Rs row 2:          0   954699569           0   286605408           1           1      1
	// Rs row 2:         select max(WaitEventID) from monWaitEventInfo

	protected Timestamp getNewSampleTime()
	{
		// FIXME: this should really be doing
		// 1: get server and client time difference (and "cache" that)
		// 2: then + or - adjust client-time with the server-time difference...
		return new Timestamp(System.currentTimeMillis());
	}

	/**
	 * Update the Sample time and interval.<br>
	 * This can/should be used if SQL Batching isn't supported, or we we shouldn't use SQL to determine the time.
	 * @param conn
	 * @param cm
	 * @throws SQLException
	 */
	protected void updateSampleTime(DbxConnection conn, CountersModel cm)
	throws SQLException
	{
		int queryTimeout = cm.getQueryTimeout();

		// Get SQL Command for how to get current time
		String srvTimeCmd = cm.getServerTimeCmd();

		// get the sample time
		if (StringUtil.hasValue(srvTimeCmd))
		{
			Statement stmnt = conn.createStatement();
			stmnt.setQueryTimeout(queryTimeout);

			ResultSet rs = stmnt.executeQuery(srvTimeCmd);
			while(rs.next())
				setSampleTime(rs.getTimestamp(1));
			rs.close();
			stmnt.close();
		}
		else
		{
			setSampleTime(getNewSampleTime());
		}

		setSampleInterval(0);
		if (_prevSample != null)
		{
			setSampleInterval( getSampleTimeAsLong() - _prevSample.getSampleTimeAsLong() );
			
			// if _prevSample is not used any further, reset this pointer here
			// If this is NOT done we will have a memory leek...
			// If _prevSample is used somewhere else, please reset this pointer later
			//    and check memory consumption under 24 hours sampling...
			_prevSample = null;
		}
	}
	
	/**
	 * Get counters...
	 */
	public boolean getSample(CountersModel cm, DbxConnection conn, String sql, List<String> pkList)
	throws SQLException, NoValidRowsInSample
	{
		int queryTimeout = cm.getQueryTimeout();
		if (_logger.isDebugEnabled())
			_logger.debug(getName()+": queryTimeout="+queryTimeout);

		Statement stmnt = null;
		ResultSet rs = null;

		long execStartTime = System.currentTimeMillis();

		try
		{
			// Get SQL Command for how to get current time
			String srvTimeCmd = cm.getServerTimeCmd();

			// if SQL batching is supported and we have a SQL Time Command, concatenate the values for efficiency
			String sendSql = sql;
			if (cm.isSqlBatchingSupported() && StringUtil.hasValue(srvTimeCmd))
			{
				sendSql = srvTimeCmd + sql;
			}
			else
			{
				updateSampleTime(conn, cm);
			}

			stmnt = conn.createStatement();

			stmnt.setQueryTimeout(queryTimeout); // XX seconds query timeout
			if (_logger.isDebugEnabled())
				_logger.debug("QUERY_TIMEOUT="+queryTimeout+", for SampleCnt='"+getName()+"'.");

			_rows   = new ArrayList<List<Object>>();

			// Allow 'go' in the string, then we should send multiple batches
			// this will take care about dropping tempdb tables prior to executing a batch that depends on it.
			// is a query batch we can't do:
			//     if ((select object_id('#cacheInfo')) is not null) drop table #cacheInfo 
			//     select CacheName, CacheID into #cacheInfo from master..monCachePool 
			// The second row will fail...
			//     Msg 12822, Level 16, State 1:
			//     Server 'GORAN_1_DS', Line 5, Status 0, TranState 0:
			//     Cannot create temporary table '#cacheInfo'. Prefix name '#cacheInfo' is already in use by another temporary table '#cacheInfo'.
			// So we need to send the statemenmts in two separate batches
			// so instead do:
			//     if ((select object_id('#cacheInfo')) is not null) drop table #cacheInfo 
			//     go
			//     select CacheName, CacheID into #cacheInfo from master..monCachePool 
			// Then it works...


			// treat each 'go' rows as a individual execution
			// readCommand(), does the job
			//int batchCount = AseSqlScript.countSqlGoBatches(sendSql);
			int batchCounter = 0;
			BufferedReader br = new BufferedReader( new StringReader(sendSql) );
			for(String sqlBatch=AseSqlScript.readCommand(br); sqlBatch!=null; sqlBatch=AseSqlScript.readCommand(br))
			{
				sendSql = sqlBatch;

				if (_logger.isDebugEnabled())
				{
					_logger.debug("##### BEGIN (send sql), batchCounter="+batchCounter+" ############################### "+ getName());
					_logger.debug(sendSql);
					_logger.debug("##### END   (send sql), batchCounter="+batchCounter+" ############################### "+ getName());
					_logger.debug("");
				}

	
				int rsNum = 0;
				int rowsAffected = 0;
				boolean hasRs = stmnt.execute(sendSql);
				checkWarnings(cm, stmnt);
				do
				{
					if (hasRs)
					{
						// Get next result set to work with
						rs = stmnt.getResultSet();
						checkWarnings(cm, stmnt);

						// first result set in first command batch, will be the "select getdate()"
						if (rsNum == 0 && batchCounter == 0 && cm.isSqlBatchingSupported() && StringUtil.hasValue(srvTimeCmd))
						{
							while(rs.next())
								setSampleTime(rs.getTimestamp(1));
							rs.close();
							
							setSampleInterval(0);
							if (_prevSample != null)
							{
								setSampleInterval( getSampleTimeAsLong() - _prevSample.getSampleTimeAsLong() );
								
								// if _prevSample is not used any further, reset this pointer here
								// If this is NOT done we will have a memory leek...
								// If _prevSample is used somewhere else, please reset this pointer later
								//    and check memory consumption under 24 hours sampling...
								_prevSample = null;
							}
						}
						else
						{
							ResultSetMetaData originRsmd = rs.getMetaData();
							if ( ! cm.hasResultSetMetaData() )
								cm.setResultSetMetaData( cm.createResultSetMetaData(originRsmd) );

							// The above "remapps" some things...
							//  - Like in Oracle 'NUMBER(0,-127)' is mapped to INTEGER
							// So we should use this when calling readResultset()...
							ResultSetMetaData translatedRsmd = cm.getResultSetMetaData();

							if (readResultset(cm, rs, translatedRsmd, originRsmd, pkList, rsNum))
								rs.close();
	
							checkWarnings(cm, stmnt);
						}
	
						rsNum++;
					}
					else
					{
						// Treat update/row count(s)
						rowsAffected = stmnt.getUpdateCount();

						if (rowsAffected >= 0)
						{
							_logger.debug(getName()+": DDL or DML rowcount = "+rowsAffected);
						}
						else
						{
							_logger.debug(getName()+": No more results to process.");
						}
					}
	
					// Check if we have more result sets
					hasRs = stmnt.getMoreResults();
	
					_logger.trace(getName()+": --hasRs="+hasRs+", rsNum="+rsNum+", rowsAffected="+rowsAffected );
				}
				while (hasRs || rowsAffected != -1);
	
				checkWarnings(cm, stmnt);
				batchCounter++;
			}
			br.close();

			// Set last refresh time
			_lastLocalSampleTime = System.currentTimeMillis();

			return true;
		}
		catch (SQLException sqlEx)
		{
			long execTime = TimeUtils.msDiffNow(execStartTime);

			_logger.warn("CounterSample("+getName()+").getCnt : ErrorCode=" + sqlEx.getErrorCode() + ", SqlState=" + sqlEx.getSQLState() + ", Message=|" + sqlEx.getMessage() + "|. execTimeInMs=" + execTime + ", SQL: " + sql, sqlEx);
			if (sqlEx.toString().indexOf("SocketTimeoutException") > 0)
			{
				_logger.info("QueryTimeout in '"+getName()+"', with query timeout '"+queryTimeout+"'. This can be changed with the config option '"+getName()+".queryTimeout=seconds' in the config file.");
			}

			//return false;
			throw sqlEx;
		}
		catch (IOException ex)
		{
			_logger.error(getName()+": While reading the input SQL 'go' String, caught: "+ex, ex);
			throw new SQLException(getName()+": While reading the input SQL 'go' String, caught: "+ex, ex);
		}
		finally
		{
			// Close the statement, and do not raise
			if (rs != null)
			{
				try { rs.close(); }
				catch(SQLException ex) { _logger.debug("Problems closing ResultSet", ex); }
			}
			
			if (stmnt != null)
			{
				try { stmnt.close(); }
				catch(SQLException ex) { _logger.debug("Problems closing Statement", ex); }
			}
		}
	}

	/**
	 * Initialize local metadata column information
	 * @param rsmd
	 * @param pkList
	 * @param rsNum
	 * @return
	 * @throws SQLException
	 */
	protected boolean initColumnInfo(ResultSetMetaData rsmd, List<String> pkList, int rsNum)
	throws SQLException
	{
		if (_colNames == null)
		{
			// Initialize column names
			_colNames       = new ArrayList<String>();
			_colSqlType     = new ArrayList<Integer>();
			_colSqlTypeName = new ArrayList<String>();
			_colClassName   = new ArrayList<String>();
			int colCount    = rsmd.getColumnCount();
			_colIsPk        = new boolean[colCount];

			_keysToRowid    = new HashMap<String,Integer>();
			_rowidToKey     = new ArrayList<String>();

			List<Integer> tmpPkPos = new LinkedList<Integer>();

			for (int i=1; i<=colCount; i++)
			{
				String colName      = rsmd.getColumnLabel(i); 
				int    colType      = rsmd.getColumnType(i);
//				String colTypeName  = rsmd.getColumnTypeName(i); // The generic one doesn't handle RepServer due to MetaData issues
				String colTypeName  = ResultSetTableModel.getColumnTypeName(rsmd, i); // This one handles RepServer better, NOTE: this delivers length, precision & scale
				String colClassName = rsmd.getColumnClassName(i);

				_colNames      .add(colName);
				_colSqlType    .add(Integer.valueOf(colType));
				_colSqlTypeName.add(colTypeName);
				_colClassName  .add(colClassName);

				// pkList could contain the ColumnName
				// or a column position
				_colIsPk[i-1] = false;
				if ( pkList != null && pkList.size() > 0)
				{
					if ( pkList.contains(colName) ) 
						_colIsPk[i-1] = true;
					if ( pkList.contains( Integer.toString(i) ) )
						_colIsPk[i-1] = true;
					
					if (_colIsPk[i-1])
						tmpPkPos.add(Integer.valueOf(i));
				}

				// Special... if colname is "msgAsColValue", 
				// all TDS Msg on the ResultSet should be added to this column
				if ( SPECIAL_COLUMN_msgAsColValue.equals(colName) )
					_pos_msgAsColValue = i;
				// Note on the above:
				// It would be nice to do, something like: 
				// - strip away the msgAsColValue::theRealColName part and replace it with a "real" column name
				// - but since the ResultSetMetaData is not writable, it can't be done
				// - and the PersistWriterXXX uses the RSMD to get the column name... so the persist table would be faulty

				// Special... if colname is "sampleTimeInMs", 
				// Simply substitute/replace with the "last sample sample time" in this column
				if ( SPECIAL_COLUMN_sampleTimeInMs.equals(colName) )
				{
					if (colType == Types.INTEGER)
						_pos_sampleTimeInMs = i;
					else
					{
						_logger.warn(getName()+": found column '"+SPECIAL_COLUMN_sampleTimeInMs+"', but it's not a INTEGER, so it will be treated as a normal column.");
					}
				}
			}

			// Double check for "duplicate" column names in the ResultSet
			// If we have that: Simply write a WARNING message, because if we try to save it in PCS it wont work (a table acn't have 2 columns with the same name) 
			Set<String> dupColumnCheck = new HashSet<>(colCount);
			for (String tmpColName : _colNames)
			{
				if ( dupColumnCheck.add(tmpColName) == false )
				{
					_logger.warn("When reading ResultSet for '" + getName() + "' the column '" + tmpColName + "' has been specified more that once in the ResultSet. This will cause any PCS, Persistent Counter Stores that saves the data into a DBMS to fail. A Table must have unique column names.");
				}
			}

			if (pkList != null && (pkList.size() != tmpPkPos.size()) )
			{
				throw new RuntimeException("sample, can't find all the primary keys in the ResultSet. pkList='"+pkList+"', _colNames='"+_colNames+"', tmpPkPos='"+tmpPkPos+"'.");
			}
				
			_pkPosArray = new int[tmpPkPos.size()];
			for(int i= 0; i<tmpPkPos.size(); i++)
			{
//				_pkPosArray[i] = ((Integer)tmpPkPos.get(i)).intValue();
				_pkPosArray[i] = tmpPkPos.get(i);
			}
			tmpPkPos = null;
		}
		else // check previous result set for match
		{
			int cols = rsmd.getColumnCount();
			if (getColumnCount() != cols)
			{
				_logger.error(getName()+": ResultSet number "+rsNum+" has "+cols+" while it was expected to have "+getColumnCount()+". Skipping this result set.");
				return false;
			}

			// Check data types match for all columns.
			for (int i=1; i<=cols; i++)
			{
				String oldType = _colClassName.get(i-1);
				String newType = rsmd.getColumnClassName(i);
				if ( ! oldType.equals(newType) )
				{
					_logger.error(getName()+": ResultSet number "+rsNum+" column number "+i+" (colName='"+rsmd.getColumnLabel(i)+"') has SQL datatype "+newType+", while we expected datatype "+oldType+".  Skipping this result set.");
					return false;
				}
			}
		}
		
		return true;
	}

	/**
	 * NOTE: if you change in this one, do not forger to 
	 *       replicate the change into method: 	public boolean addRow(List<Object> row)
	 *       
	 * @param rs
	 * @param rsmd
	 * @param pkList
	 * @param rsNum
	 * @return
	 * @throws SQLException
	 */
	protected boolean readResultset(CountersModel cm, ResultSet rs, ResultSetMetaData rsmd, ResultSetMetaData originRsmd, List<String> pkList, int rsNum)
	throws SQLException
	{
		if ( ! initColumnInfo(rsmd, pkList, rsNum) )
		{
			rs.close();
			return false;
		}

		ResultSetMetaDataCached rsmdCached = null;
		if (rsmd instanceof ResultSetMetaDataCached)
		{
			rsmdCached = (ResultSetMetaDataCached) rsmd;
		}
		
		// Load counters in memory
		int rsRowNum = 0;
		List<Object> row;
		List<Object> prevRow = null;
		Object val;
		StringBuilder key;
		
		if (_logger.isDebugEnabled())
			_logger.debug("-------------------------------------------------------");

		while (rs.next())
		{
			int colCount = getColumnCount();
			key = new StringBuilder();
			row = new ArrayList<Object>(colCount); // Use column names to size the array

			// get any Messages that are attached to the ResultSet
			String msgAsColValue = "";
			SQLWarning sqlwInRs = rs.getWarnings();
			if (sqlwInRs != null)
			{
				if (_pos_msgAsColValue >= 0)
					msgAsColValue = AseConnectionUtils.getSqlWarningMsgs(sqlwInRs);
				else
					_logger.warn(getName()+": Received a Msg while reading the resultset from '"+getName()+"', This could be mapped to a column by using a column name 'msgAsColValue' in the SELECT statement. Right now it's discarded. The Msg="+sqlwInRs.getErrorCode()+", message text: " + AseConnectionUtils.getSqlWarningMsgs(sqlwInRs));
			}

			// Get one row
			for (int i = 1; i <= colCount; i++)
			{
				// If it's a "faked" column inserted by the CM, do not read the value... it's NOT part of the ResultSet
				if (rsmdCached != null && rsmdCached.isFakedColumn(i))
				{
					Object defaultValue = rsmdCached.getFakedColumnDefaultValue(i);
					row.add(defaultValue);
					continue;
				}

				// Get values... 
				// - First try with the "fixed/modified" ResultSetMetaData
				// - If it failes, use the "origin" ResultSetMetaData
				try
				{
					val = getDataValue(rs, i, null);
				}
				catch(SQLException ex)
				{
					val = getDataValue(rs, i, originRsmd);
					_logger.warn("Failed reading object for cm='"+cm.getName()+"', row="+_rows.size()+", col="+i+", colName='"+_colNames.get(i-1)+"'. Trying to read it again but with the ORIGIN ResultSetMetatData. values using Origin rsmd = '"+val+"'.", ex);
				}

				// Trim strings
				if (val != null && val instanceof String)
				{
					if (cm.isStringTrimEnabled())
						val = StringUtil.trim( (String)val );
					else
						val = StringUtil.rtrim( (String)val );
				}

				if (rsRowNum == 0 && _logger.isTraceEnabled() )
					_logger.trace(getName()+": READ_RESULTSET(rsnum "+rsNum+", row 0): col=" + i + ", colName=" + (_colNames.get(i - 1) + "                                   ").substring(0, 25) + ", ObjectType=" + (val == null ? "NULL-VALUE" : val.getClass().getName()));

//				if (val == null)
//					val = fixNullValue(i);

				// IF we had TDS Msg at the ResultSet level
				// AND we had a column named 'msgAsColValue'
				// THEN add the Msg fields instead of the column object
				if (_pos_msgAsColValue == i)
					val = msgAsColValue;

				// Set the "sampleTimeInMs" in the resultset.
				if (_pos_sampleTimeInMs == i)
					val = Integer.valueOf( getSampleInterval() );

				row.add(val);

				// If this column is part of the PrimaryKey
				if (_colIsPk != null && _colIsPk[i-1] )
				{
					if (val != null)
					{
						key.append(val).append(PK_STR_DELIMITER);
					}
					else
					{
						val = cm.pkNullValueResolver(this, rsRowNum, i, _colNames.get(i-1));
						if (val != null)
						{
							key.append(val).append(PK_STR_DELIMITER);
							
							// Set the new value in the "last added" position.
							row.set(row.size()-1, val);
						}
						else
						{
							_logger.warn(getName() + ": Key containes a NULL value, rsNum=" + rsNum + ", row=" + rsRowNum + ", col=" + i + " colName=" + _colNames.get(i-1) + ". Continuing without adding the NULL value to the key.");
						}
					}
				}
			}
			if (_logger.isTraceEnabled())
			{
				for (int c=0; c<colCount; c++)
					_logger.trace(getName()+":    > rsNum="+rsNum+", rsRowNum="+rsRowNum+", getRowCount()="+getRowCount()+", c="+c+", className='"+row.get(c).getClass().getName()+"', value="+row.get(c));
			}
//			if (getName().startsWith(SummaryPanel.CM_NAME))
//			{
//				for (int c=0; c<colCount; c++)
//				System.out.println(SummaryPanel.CM_NAME+":   > rsNum="+rsNum+", rsRowNum="+rsRowNum+", getRowCount()="+getRowCount()+", c="+c+", className='"+row.get(c).getClass().getName()+"', colName="+StringUtil.left(getColumnName(c),20)+", value="+row.get(c));
//			}

			String keyStr = key.toString();

//			if (_colIsPk != null)
			if (pkList != null)
			{
				// Check for DUPLICATES
				Integer intObj = _keysToRowid.get(keyStr);
				if (intObj != null)
				{
					List<Object> curRow = getRow(keyStr);

					int dupMergeCountPos = findColumn(SPECIAL_COLUMN_dupMergeCount);
					int dupRowCountPos   = findColumn(SPECIAL_COLUMN_dupRowCount);

					// Decide what action to take
					int pkDuplicateAction = 0;
					if (dupRowCountPos   >= 0) pkDuplicateAction = 2;
					if (dupMergeCountPos >= 0) pkDuplicateAction = 1;

					if (pkDuplicateAction != 0 && _diffColNames == null)
					{
						_logger.warn("Internal Counter Duplicate key in ResultSet for CM '"+getName()+"', pk='"+pkList+"', pkDuplicateAction="+pkDuplicateAction+", BUT _diffColNames is null, this should never happen.");

						// Read next row
						continue;
					}
					if (pkDuplicateAction == 0)
					{
						// Possibly change this in the future to check for DuplicateKey action in the CM
						// meaning instead of just returning true|false ... move this up a bit in the logic to also check for "pkDuplicateAction" etc... 
						if (cm.actionForSampleDuplicateKey(this, keyStr, curRow, row))
							_logger.warn("Internal Counter Duplicate key in ResultSet for CM '"+getName()+"', pk='"+pkList+"', a row with the key values '"+key+"' already exists. CurrentRow='"+curRow+"'. NewRow='"+row+"'.");

						// Read next row
						continue;
						//throw new DuplicateKeyException(key, curRow, row);
					}

					// [FIXME] here is where I would put in row merge
					//if (allowRowMerge)
					//	doRowMerge(keyStr, curRow, row);

					if (_pkDupCountMap == null)
						_pkDupCountMap = new HashMap<String, Integer>();
					Integer dupMergeCount = _pkDupCountMap.get(keyStr);
					if (dupMergeCount == null)
						dupMergeCount = Integer.valueOf(1); // initial value is/should_be 0
					else
						dupMergeCount = Integer.valueOf(dupMergeCount.intValue() + 1);
					_pkDupCountMap.put(keyStr, dupMergeCount);

					// set/increment column 'dupMergeCount'/'dupRowCount'
					if (pkDuplicateAction == 1)
						curRow.set(dupMergeCountPos, dupMergeCount);
					else
						row.set(dupRowCountPos, dupMergeCount);

					// merge the two values
					if (pkDuplicateAction == 1)
					{
						for (String diffColName : _diffColNames) // FIXME _colNames is not the correct list.
						{
							int diffCollNamePos = findColumn(diffColName);
							if (diffCollNamePos == -1)
							{
								if (_logger.isTraceEnabled())
									_logger.trace(getName()+":    >> MERGE for key='"+key+"', rowId="+intObj+", colName='"+diffColName+"', colPos="+diffCollNamePos+". COLUMN NOT FOUND... Skipping this column.");
							}
							else
							{
								Number prevColVal = (Number) curRow.get(diffCollNamePos);
								Number thisColVal = (Number)    row.get(diffCollNamePos);

								// Merge the columns and set the new value.
								// On failure, it returns the prevColVal
								Number mergeColVal = mergeColumnValue(prevColVal, thisColVal);
								curRow.set(diffCollNamePos, mergeColVal);

								if (_logger.isTraceEnabled())
									_logger.trace(getName()+":    >> MERGE for key='"+key+"', rowId="+intObj+", colName='"+diffColName+"', colPos="+diffCollNamePos+", prevValue="+prevColVal+", thisVal="+thisColVal+", mergedVal="+mergeColVal+".");
							}
						}
						
						// on MERGE no need to add the row, so just read next row
						_logger.trace(getName()+":    >> MERGE continue and red next row from ResultSet.");
						continue;
					}
					// NOT IMPLEMENTED
					if (pkDuplicateAction == 2)
					{
						_logger.warn("Internal Counter Duplicate key in ResultSet for CM '"+getName()+"', pk='"+pkList+"', pkDuplicateAction=2 IS NOT IMPLEMENTED.");

						// Read next row
						continue;
					}
				}
			}

			// in this hook you can choose to discard the row
			// also you can aggregate the row into one "super" row
			// check CM '', which is doing SQL Statement "collapsing" of several SQL text rows into a "super" row
			// if the method returns FALSE, then we want to SKIP the row
			if ( ! cm.hookInSqlRefreshBeforeAddRow(this, row, prevRow) )
				continue;

			// save PrimaryKey with corresponding row
//			if (_colIsPk != null)
			if (pkList != null)
			{
				// This is current size, because _rows.add() is done AFTER this
				int rowId = _rows.size();

				_keysToRowid.put(keyStr, Integer.valueOf(rowId));
				_rowidToKey.add(keyStr);
				if (_logger.isTraceEnabled())
					_logger.trace(getName()+":    >> key='"+key+"', rowId="+rowId+", _rowidToKey.addPos="+(_rowidToKey.size()-1));
			}

			if (_logger.isDebugEnabled())
				_logger.debug(getName()+":    >> rowAdded: rowId="+getRowCount()+", key="+key+", row="+row);

			// ADD the row
			_rows.add(row);

			prevRow = row;

			rsRowNum++;
		}
		return true;
	}

	/** re-make new row id's. This since we have made deletions of  */
	public void newRowIds()
	{
		// Update the hashtable (since rowid's have changed)
		_keysToRowid = new HashMap<String,Integer>();
		_rowidToKey  = new ArrayList<String>();

		for (int r=0; r<_rows.size(); r++)
		{
			List<Object> row = _rows.get(r);
			StringBuilder key = new StringBuilder();
			int colCount = getColumnCount();
			for (int c=0; c<colCount; c++)
			{
				// If this column is part of the PrimaryKey
				if (_colIsPk != null && _colIsPk[c] )
				{
					Object val = row.get(c);
					if (val != null)
					{
						key.append(val).append(PK_STR_DELIMITER);
					}
				}
			}
			if (_colIsPk != null)
			{
				String keyStr = key.toString();
				_keysToRowid.put(keyStr, Integer.valueOf(r));
				_rowidToKey.add(keyStr);
			}
		}
	}

	/**
	 * This just <b>OVERWRITES a row</b> so be carefull (no PK etc are checked/changed maintained)
	 * @param cm
	 * @param rowNum
	 * @param row
	 * @return
	 */
	public int setRow(CountersModel cm, int rowNum, List<Object> row)
	{
		if (row == null)
			return -1;

		int	colCount = getColumnCount();

		// Check input, throw if problems
		if (row.size() != colCount)  throw new IndexOutOfBoundsException("The number of columns in current structure is " + colCount + ", while the row we attempt to set has " + row.size() + " columns, name='" + getName() + "'."); 
		if (rowNum < 0)              throw new IndexOutOfBoundsException("rowNum=" + rowNum + " Can't be below 0, name='" + getName() + "'.");
		if (rowNum >= getRowCount()) throw new IndexOutOfBoundsException("rowNum=" + rowNum + " is above/equal rowCount=" + getRowCount() + ", name='" + getName() + "'."); 

		// Set the row
		_rows.set(rowNum, row);

		return rowNum;
	}

	/**
	 * NOTE: if you change in this one, do not forger to 
	 *       replicate the change into method: readResultset
	 * 
	 * @param row
	 * @return the RowID where the row was inserted. (-1 on failure)
	 */
	public int addRow(CountersModel cm, List<Object> row)
	{
		if (row == null)
			return -1;
		
		int	colCount = getColumnCount();

		if (row.size() != colCount)
		{
			throw new IndexOutOfBoundsException("The number of columns in current structure is "+colCount+", while the row we attempt to add has "+row.size()+" columns."); 
		}
		
		if (_rows == null)
			_rows = new ArrayList<List<Object>>();

		// Load counters in memory
		Object val;
		StringBuilder key = new StringBuilder();

		// Fix null values 
		// fix PK and ROWID
		for (int c=0; c<colCount; c++)
		{
			val = row.get(c);
//			if (val == null)
//			{
//				val = fixNullValue(c);
//				row.set(c, val);
//			}

			// If this column is part of the PrimaryKey
			if (_colIsPk != null && _colIsPk[c] )
			{
				if (val != null)
					key.append(val).append(PK_STR_DELIMITER);
				else
					_logger.warn(getName()+": Key containes NULL value, row="+getRowCount()+", col="+c+".");
			}
		}
		String keyStr = key.toString();

		// Check for DUPLICATES
		if (_colIsPk != null)
		{
			Integer io = _keysToRowid.get(keyStr);
			if (io != null)
			{
				List<Object> curRow = getRow(keyStr);

				// [FIXME] here is where I would put in row merge
				//if (allowRowMerge)
				//	doRowMerge(curRow, row);

				// Possibly change this in the future to check for DuplicateKey action in the CM
				// meaning instead of just returning true|false ... move this up a bit in the logic to also check for "pkDuplicateAction" etc... 
				if (cm.actionForSampleDuplicateKey(this, keyStr, curRow, row))
					_logger.warn("Internal Counter Duplicate key in ResultSet for CM '"+getName()+"', pk='"+getPkCols(_colIsPk)+"', a row with the key values '"+key+"' already exists. CurrentRow='"+curRow+"'. NewRow='"+row+"'.");
				return -1;
				//throw new DuplicateKeyException(key, curRow, row);
			}
		}

		// ADD the row
		_rows.add(row);

		// This is current size-1, because _rows.add() is done BEFORE this
		int rowId = _rows.size()-1;

		if (_logger.isDebugEnabled())
			_logger.debug(getName()+": addRow(): rowId="+rowId+", key="+key+", row="+row);

		// save PKEY with corresponding row
		if (_colIsPk != null)
		{
			_keysToRowid.put(keyStr, Integer.valueOf(rowId));
			_rowidToKey.add(keyStr);
		}

		return rowId;
	}

	private List<String> getPkCols(boolean[] pkColsArr)
	{
		List<String> pkList = new ArrayList<String>();
		if (pkColsArr == null)
			return pkList;

		for (int c=0; c<pkColsArr.length; c++)
		{
			if (pkColsArr[c] && c<_colNames.size())
				pkList.add(_colNames.get(c));
		}
		return pkList;
	}
	
	public void remove(String key)
	{
		removeRow(key);
	}

	public void removeRow(String key)
	{
		int row = getRowNumberForPkValue(key);
		removeRow(row);
	}

	public synchronized void removeRow(int rowId)
	{
		int maxRowId = _rows.size() - 1;

		// Get the key for this rowId
		String key = _rowidToKey.get(rowId);

		if (_logger.isDebugEnabled())
			_logger.debug(getName()+": removeRow(rowId="+rowId+"): key="+key);

		// Removes the row from listOfRows
		_rows.remove(rowId);

		// Remove the rowId->key
		_rowidToKey.remove(rowId);

		// remove the key from the hash map
		_keysToRowid.remove(key);

		// if we delete in the middle of a list, we need to
		// change the rowId's for the rest of the list
		if (rowId < maxRowId )
		{
			for (int r=rowId; r<_rows.size(); r++)
			{
				// Get the key for row
				// rows has moved when "_rowidToKey.remove(rowId)" was done
				key = _rowidToKey.get(r);

				// Set the new rowId for the PrimaryKey
				_keysToRowid.put(key, Integer.valueOf(r)) ;
			}
			
		}
	}

	public synchronized void removeAllRows()
	{
		// Removes the row from listOfRows
		if (_rows != null) 
			_rows.clear();

		// Remove the rowId->key
		if (_rowidToKey != null) 
			_rowidToKey.clear();

		// remove the key from the hash map
		if (_keysToRowid != null) 
			_keysToRowid.clear();
	}


	

	public void setPk(String key, int row)
	{
		_keysToRowid.put(key, Integer.valueOf(row));

		while ( _rowidToKey.size() <= row )
		{
			_rowidToKey.add("-setPk:uninitialized-");
		}
		_rowidToKey.set(row, key);
	}

	public List<List<Object>> getDataCollection()
	{
		return _rows;
	}

	public List<Object> getRow(String key)
	{
		int row = getRowNumberForPkValue(key);
		if (row == -1)
			return null;

		return getRow(row);
	}

	public List<Object> getRow(int row)
	{
		return _rows.get(row);
	}

	public Object getRowValue(String key, String columnName)
	{
		// Get column position
		int colId = findColumn(columnName);
		if (colId < 0)
			return null;

		// Get the row
		List<Object> row = getRow(key);
		if (row == null)
			return null;

		return row.get(colId);
	}


//	/**
//	 * [FIXME] Describe me
//	 * 
//	 * @param diff
//	 * @param isDiffCol
//	 * @param isPctCol
//	 * @return
//	 */
//	static public CounterSample computeRatePerSec(CounterSample diff, boolean[] isDiffCol, boolean[] isPctCol) 
//	{
//		// Initialize result structure
//		CounterSample rate  = new CounterSample(diff, false, diff._name+"-rate");
//
//		// - Loop on all rows in the DIFF structure
//		// - Do calculations on them
//		// - And add them to the RATE structure
//		for (int rowId=0; rowId < diff.getRowCount(); rowId++) 
//		{
//			// Get the row from the DIFF structure
//			List<Object> diffRow = diff.getRow(rowId);
//
//			// Create a new ROW "structure" for each row in the DIFF
//			List<Object> newRow = new ArrayList<Object>();
//
//			for (int i=0; i<diff.getColumnCount(); i++) 
//			{
//				// Get the RAW object from the DIFF structure
//				Object originObject = diffRow.get(i);
//
//				// If the below IF statements is not true... keep the same object
//				Object newObject    = originObject;
//
//				// If PCT column DO nothing.
//				if ( isPctCol[i] ) 
//				{
//				}
//				// If this is a column that has DIFF calculation.
//				else if ( isDiffCol[i] ) 
//				{
//					double val = 0;
//
//					// What to do if we CANT DO DIVISION
//					if (diff._interval == 0)
//						newObject = "N/A";
//
//					// Calculate rate
//					if (originObject instanceof Number)
//					{
//						// Get the object as a Double value
//						if ( originObject instanceof Number )
//							val = ((Number)originObject).doubleValue();
//						else
//							val = Double.parseDouble( originObject.toString() );
//
//						// interval is in MilliSec, so val has to be multiplied by 1000
//						val = (val * 1000) / diff._interval;
//						BigDecimal newVal = new BigDecimal( val ).setScale(1, BigDecimal.ROUND_HALF_EVEN);
//
//						// Set the new object
//						newObject = newVal;
//					}
//					// Unsupported columns, skip the calculation
//					else
//					{
//						String colName = diff.getColumnName(i);
//						_logger.warn("CounterSampleSetName='"+diff._name+"', className='"+originObject.getClass().getName()+"' columns can't be 'rate' calculated. colName='"+colName+"', originObject='"+originObject+"', keeping this object.");
//						newObject = originObject;
//					}
//				}
//
//				// set the data in the new row
//				newRow.add(newObject);
//
//			} // end: row loop
//
//			rate.addRow(newRow);
//
//		} // end: all rows loop
//		
//		return rate;
//	}	
	
//	public CounterSample computeDiffCnt(CounterSample newSample)
//	{
//		return CounterSample.computeDiffCnt(this, newSample);
//	}

	// computeDiffCnt : generate a new CounterSample, with computed differences on some columns
	// bimapColsCalcDiff : Vector of Integers. For each col : 0 if not to compute, 1 if col must be computed
//	static public CounterSample computeDiffCnt(CounterSample oldSample, CounterSample newSample, int idKey1, int idKey2, int idKey3, Vector bitmapColsCalcDiff)
//	/**
//	 * [FIXME] Describe me
//	 * @param isCountersCleared 
//	 */
//	static public CounterSample computeDiffCnt(CounterSample oldSample, CounterSample newSample, List<Integer> deletedRows, List<String> pkList, boolean[] isDiffCol, boolean isCountersCleared)
//	{
//		// Initialize result structure
//		CounterSample diffCnt = new CounterSample(newSample, false, newSample._name+"-diff");
//
//		long newTsMilli      = newSample._samplingTime.getTime();
//		long oldTsMilli      = oldSample._samplingTime.getTime();
//		int newTsNano        = newSample._samplingTime.getNanos();
//		int oldTsNano        = oldSample._samplingTime.getNanos();
//
//		// Check if TsMilli has really ms precision (not the case before JDK 1.4)
//		if ((newTsMilli - (newTsMilli / 1000) * 1000) == newTsNano / 1000000)
//			// JDK > 1.3.1
//			diffCnt._interval = newTsMilli - oldTsMilli;
//		else
//			diffCnt._interval = newTsMilli - oldTsMilli + (newTsNano - oldTsNano) / 1000000;
//
//		List<Object> newRow;
//		List<Object> oldRow;
//		List<Object> diffRow;
//		int oldRowId;
//
//		if (diffCnt._colIsPk == null)
//		{
//			// Special case, only one row for each sample, no key
//			oldRow = oldSample._rows.get(0);
//			newRow = newSample._rows.get(0);
//			diffRow = new ArrayList<Object>();
//			for (int i = 0; i < newSample.getColumnCount(); i++)
//			{
//				diffRow.add(Integer.valueOf(((Integer) (newRow.get(i))).intValue() - ((Integer) (oldRow.get(i))).intValue()));
//			}
//			diffCnt._rows.add(diffRow);
//			return diffCnt;
//		}
//
//		// Keep a array of what rows that we access of the old values
//		// this will help us find out what rows we "deleted" from the previous to the new sample
//		// or actually rows that are no longer available in the new sample...
//		boolean oldSampleAccessArr[] = new boolean[oldSample.getRowCount()]; // default values: false
//
//		// Loop on all rows from the NEW sample
//		for (int newRowId = 0; newRowId < newSample.getRowCount(); newRowId++)
//		{
//			newRow = newSample.getRow(newRowId);
//			diffRow = new ArrayList<Object>();
//			
//			// get PK of the new row
//			String newPk = newSample.getPkValue(newRowId);
//
//			// Retreive old same row
//			oldRowId = oldSample.getRowNumberForPkValue(newPk);
//			
//			// if old Row EXISTS, we can do diff calculation
//			if (oldRowId != -1)
//			{
//				// Mark the row as "not deleted" / or "accessed"
//				if (oldRowId >= 0 && oldRowId < oldSampleAccessArr.length)
//					oldSampleAccessArr[oldRowId] = true;
//				
//				// Old row found, compute the diffs
//				oldRow = oldSample.getRow(oldRowId);
//				for (int i = 0; i < newSample.getColumnCount(); i++)
//				{
//					if ( ! isDiffCol[i] )
//						diffRow.add(newRow.get(i));
//					else
//					{
//						//checkType(oldSample, oldRowId, i, newSample, newRowId, i);
//						//if ((newRow.get(i)).getClass().toString().equals("class java.math.BigDecimal"))
//						Object oldRowObj = oldRow.get(i);
//						Object newRowObj = newRow.get(i);
//
//						String colName = newSample.getColumnName(i);
//
//						if ( newRowObj instanceof Number )
//						{
//							Number diffValue = diffColumnValue((Number)oldRowObj, (Number)newRowObj, diffCnt._negativeDiffCountersToZero, newSample._name, colName, isCountersCleared);
//							diffRow.add(diffValue);
//						}
//						else
//						{
//							_logger.warn("CounterSampleSetName='"+newSample._name+"', className='"+newRowObj.getClass().getName()+"' columns can't be 'diff' calculated. colName='"+colName+"', key='"+newPk+"', oldObj='"+oldRowObj+"', newObj='"+newRowObj+"'.");
//							diffRow.add(newRowObj);
//						}
//					}
//				}
//			} // end: old row was found
//			else
//			{
//				// Row was NOT found in previous sample, which means it's a "new" row for this sample.
//				// So we do not need to do DIFF calculation, just add the raw data...
//				for (int i = 0; i < newSample.getColumnCount(); i++)
//				{
//					diffRow.add(newRow.get(i));
//				}
//			}
//
//			diffCnt.addRow(diffRow);
//
//		} // end: row loop
//		
//		// What rows was DELETED from previous sample.
//		// meaning, rows in the previous sample that was NOT part of the new sample.
//		if (deletedRows != null)
//		{
//			for (int i=0; i<oldSampleAccessArr.length; i++)
//			{
//				if (oldSampleAccessArr[i] == false)
//				{
//					deletedRows.add(i);
//				}
//			}
//		}
//
//		return diffCnt;
//	}

//	/**
//	 * Do difference calculations newColVal - prevColVal
//	 * @param prevColVal previous sample value
//	 * @param newColVal current/new sample value
//	 * @param negativeDiffCountersToZero if the counter is less than 0, reset it to 0
//	 * @param counterSetName Used as a prefix for messages
//	 * @param isCountersCleared if counters has been cleared
//	 * @return the difference of the correct subclass of Number
//	 */
//	private static Number diffColumnValue(Number prevColVal, Number newColVal, boolean negativeDiffCountersToZero, String counterSetName, String colName, boolean isCountersCleared)
//	{
//		Number diffColVal = null;
//
////if (counterSetName.startsWith(SummaryPanel.CM_NAME))
////{
////	System.out.println(counterSetName+":   > colName="+StringUtil.left(colName,20)+", prevColVal="+prevColVal );
////	System.out.println(counterSetName+":     colName="+StringUtil.left(colName,20)+", newColVal ="+newColVal );
////}
////System.out.println("diffColumnValue(): counterSetName='"+counterSetName+"', colName='"+colName+"'.");
//
//		if (newColVal instanceof BigDecimal)
//		{
//			diffColVal = new BigDecimal(newColVal.doubleValue() - prevColVal.doubleValue());
//			if (diffColVal.doubleValue() < 0)
//			{
//				// Do special stuff for diff counters on CmSpinlockSum and ASE is 12.5.x, then counters will be delivered as numeric(19,0)
//				// but is really signed int, then we need to check for wrapped signed int values
//				// prevColVal is "near" UNSIGNED-INT-MAX and newColVal is "near" 0
//				// Then do special calculation: (UNSIGNED-INT-MAX - prevColVal) + newColVal + 1      (+1 to handle passing value 0)
//				// NOTE: we might also want to check COUNTER-RESET-DATE (if it has been done since last sample, then we can't trust the counters)
//				if (CmSpinlockSum.CM_NAME.equals(counterSetName) && !isCountersCleared)
//				{
//// FIXME: move this code... or implement something that is more generic that check for a CM_NAME
//					Number beforeReCalc = diffColVal;
////					int  threshold      = 10000000;    // 10 000 000
//					long maxUnsignedInt = 4294967295L; // 4 294 967 295
//
////					if (prevColVal.doubleValue() > (maxUnsignedInt - threshold) && newColVal.doubleValue() < threshold)
//						diffColVal = new BigDecimal((maxUnsignedInt - prevColVal.doubleValue()) + newColVal.doubleValue() + 1);
//					_logger.debug("diffColumnValue(): CM='"+counterSetName+"', BigDecimal(ASE-numeric) : CmSpinlockSum(colName='"+colName+"', isCountersCleared="+isCountersCleared+"):  AFTER: do special calc. newColVal.doubleValue()='"+newColVal.doubleValue()+"', prevColVal.doubleValue()='"+prevColVal.doubleValue()+"', beforeReCalc.doubleValue()='"+beforeReCalc.doubleValue()+"', diffColVal.doubleValue()='"+diffColVal.doubleValue()+"'.");
//				}
//
//				if (diffColVal.doubleValue() < 0)
//					if (negativeDiffCountersToZero)
//						diffColVal = new BigDecimal(0);
//			}
//		}
//		else if (newColVal instanceof Byte)
//		{
//			diffColVal = Byte.valueOf((byte) (newColVal.byteValue() - prevColVal.byteValue()));
//			if (diffColVal.intValue() < 0)
//				if (negativeDiffCountersToZero)
//					diffColVal = Byte.valueOf("0");
//		}
//		else if (newColVal instanceof Double)
//		{
//			diffColVal = Double.valueOf(newColVal.doubleValue() - prevColVal.doubleValue());
//			if (diffColVal.doubleValue() < 0)
//				if (negativeDiffCountersToZero)
//					diffColVal = Double.valueOf(0);
//		}
//		else if (newColVal instanceof Float)
//		{
//			diffColVal = Float.valueOf(newColVal.floatValue() - prevColVal.floatValue());
//			if (diffColVal.floatValue() < 0)
//				if (negativeDiffCountersToZero)
//					diffColVal = Float.valueOf(0);
//		}
//		else if (newColVal instanceof Integer)
//		{
//// Saving this code for future, the test shows that calculations is OK even with overflow counters...
//// 1: either I miss something here
//// 2: or Java handles this "auto-magically"
////			// Deal with counter counter overflows by calculating the: prevSample(numbers-up-to-INT-MAX-value) + newSample(negativeVal - INT-MIN)
////			if (newColVal.intValue() < 0 && prevColVal.intValue() >= 0)
////			{
//////				// example prevColVal=2147483646, newColVal=-2147483647: The difference SHOULD BE: 3  
//////				int restVal = Integer.MAX_VALUE - prevColVal.intValue(); // get changes up to the overflow: 2147483647 - 2147483646 == 7 
//////				int overflv = newColVal.intValue() - Integer.MIN_VALUE;  // get changes after the overflow: -2147483647 - -2147483648 = 8
//////				diffColVal = Integer.valueOf( restVal + overflv );
//////System.out.println("restVal: "+restVal);
//////System.out.println("overflv: "+overflv);
//////System.out.println("=result: "+diffColVal);
////
////				// Or simplified (one-line)
//////				diffColVal = Integer.valueOf( (Integer.MAX_VALUE - prevColVal.intValue()) + (newColVal.intValue() - Integer.MIN_VALUE) );
////
////				// Deal with counter overflows by bumping up to a higher data type: and adding the negative delta on top of Integer.MAX_VALUE -------*/ 
////				long newColVal_bumped = Integer.MAX_VALUE + (newColVal.longValue() - Integer.MIN_VALUE);
////				diffColVal = Integer.valueOf( (int) newColVal_bumped - prevColVal.intValue() );
////			}
////			else
////				diffColVal = Integer.valueOf(newColVal.intValue() - prevColVal.intValue());
//			
//			diffColVal = Integer.valueOf(newColVal.intValue() - prevColVal.intValue());
//			if (diffColVal.intValue() < 0)
//				if (negativeDiffCountersToZero)
//					diffColVal = Integer.valueOf(0);
//		}
//		else if (newColVal instanceof Long)
//		{
//			diffColVal = Long.valueOf(newColVal.longValue() - prevColVal.longValue());
//			if (diffColVal.longValue() < 0)
//			{
//				// Do special stuff for diff counters on CmSpinlockSum and ASE is above 15.x, then counters will be delivered as bigint
//				// but is really signed int, then we need to check for wrapped signed int values
//				// prevColVal is "near" UNSIGNED-INT-MAX and newColVal is "near" 0
//				// Then do special calculation: (UNSIGNED-INT-MAX - prevColVal) + newColVal + 1      (+1 to handle passing value 0)
//				// NOTE: we might also want to check COUNTER-RESET-DATE (if it has been done since last sample, then we can't trust the counters)
//				if (CmSpinlockSum.CM_NAME.equals(counterSetName) && !isCountersCleared)
//				{
//// FIXME: move this code... or implement something that is more generic that check for a CM_NAME
//					Number beforeReCalc = diffColVal;
////					int  threshold      = 10000000;    // 10 000 000
//					long maxUnsignedInt = 4294967295L; // 4 294 967 295
//					
////					if (prevColVal.longValue() > (maxUnsignedInt - threshold) && newColVal.longValue() < threshold)
//						diffColVal = Long.valueOf((maxUnsignedInt - prevColVal.longValue()) + newColVal.longValue() + 1);
//					_logger.debug("diffColumnValue(): CM='"+counterSetName+"', Long(ASE-bigint) : CmSpinlockSum(colName='"+colName+"', isCountersCleared="+isCountersCleared+"):  AFTER: do special calc. newColVal.longValue()='"+newColVal.longValue()+"', prevColVal.longValue()='"+prevColVal.longValue()+"', beforeReCalc.longValue()='"+beforeReCalc.longValue()+"', diffColVal.longValue()='"+diffColVal.longValue()+"'.");
//				}
//
//				if (diffColVal.longValue() < 0)
//					if (negativeDiffCountersToZero)
//						diffColVal = Long.valueOf(0);
//			}
//		}
//		else if (newColVal instanceof Short)
//		{
//			diffColVal = Short.valueOf((short) (newColVal.shortValue() - prevColVal.shortValue()));
//			if (diffColVal.shortValue() < 0)
//				if (negativeDiffCountersToZero)
//					diffColVal = Short.valueOf("0");
//		}
//		else if (newColVal instanceof AtomicInteger)
//		{
//			diffColVal = new AtomicInteger(newColVal.intValue() - prevColVal.intValue());
//			if (diffColVal.intValue() < 0)
//				if (negativeDiffCountersToZero)
//					diffColVal = new AtomicInteger(0);
//		}
//		else if (newColVal instanceof AtomicLong)
//		{
//			diffColVal = new AtomicLong(newColVal.longValue() - prevColVal.longValue());
//			if (diffColVal.longValue() < 0)
//				if (negativeDiffCountersToZero)
//					diffColVal = new AtomicLong(0);
//		}
//		else
//		{
//			_logger.warn(counterSetName+": failure in diffColumnValue(colName='"+colName+"', prevColVal='"+prevColVal+"', newColVal='"+newColVal+"'), with prevColVal='"+prevColVal.getClass().getName()+"', newColVal='"+newColVal.getClass().getName()+"'. Returning the new value instead.");
//			return newColVal;
//		}
//
//		return diffColVal;
//	}

	
	/**
	 * Merge column values...
	 * NOTE: this could fail with overflow...
	 * @param prevColVal
	 * @param thisColVal
	 * @return
	 */
	private Number mergeColumnValue(Number prevColVal, Number thisColVal)
	{
		Number mergeColVal = null;

		if      (thisColVal instanceof BigDecimal)
		{
			mergeColVal = BigDecimal.valueOf(prevColVal.doubleValue() + thisColVal.doubleValue());
		}
		else if (thisColVal instanceof Byte)
		{
			mergeColVal = Byte.valueOf((byte) (prevColVal.byteValue() + thisColVal.byteValue()));
		}
		else if (thisColVal instanceof Double)
		{
			mergeColVal = Double.valueOf(prevColVal.doubleValue() + thisColVal.doubleValue());
		}
		else if (thisColVal instanceof Float)
		{
			mergeColVal = Float.valueOf(prevColVal.floatValue() + thisColVal.floatValue());
		}
		else if (thisColVal instanceof Integer)
		{
			mergeColVal = Integer.valueOf(prevColVal.intValue() + thisColVal.intValue());
		}
		else if (thisColVal instanceof Long)
		{
			mergeColVal = Long.valueOf(prevColVal.longValue() + thisColVal.longValue());
		}
		else if (thisColVal instanceof Short)
		{
			mergeColVal = Short.valueOf((short) (prevColVal.shortValue() + thisColVal.shortValue()));
		}
		else if (thisColVal instanceof AtomicInteger)
		{
			mergeColVal = new AtomicInteger(prevColVal.intValue() + thisColVal.intValue());
		}
		else if (thisColVal instanceof AtomicLong)
		{
			mergeColVal = new AtomicLong(prevColVal.longValue() + thisColVal.longValue());
		}
		else
		{
			_logger.warn(getName()+": failure in mergeColumnValue(prevColVal='"+prevColVal+"', thisColVal='"+thisColVal+"'), with prevColVal='"+prevColVal.getClass().getName()+"', thisColVal='"+thisColVal.getClass().getName()+"'. Returning the origin value instead.");
			return prevColVal;
		}
		return mergeColVal;
	}
	
	
//	public static boolean isDiffAllowedForDatatype(String datatype)
//	{
//		boolean enabled = false;
//		if      ("tinyint"          .equalsIgnoreCase(datatype)) enabled = true;
//		else if ("unsigned smallint".equalsIgnoreCase(datatype)) enabled = true;
//		else if ("smallint"         .equalsIgnoreCase(datatype)) enabled = true;
//		else if ("unsigned int"     .equalsIgnoreCase(datatype)) enabled = true;
//		else if ("int"              .equalsIgnoreCase(datatype)) enabled = true;
//		else if ("integer"          .equalsIgnoreCase(datatype)) enabled = true;
//		else if ("unsigned bigint"  .equalsIgnoreCase(datatype)) enabled = true;
//		else if ("bigint"           .equalsIgnoreCase(datatype)) enabled = true;
//		else if ("decimal"          .equalsIgnoreCase(datatype)) enabled = true;
//		else if ("numeric"          .equalsIgnoreCase(datatype)) enabled = true;
//		else if ("float"            .equalsIgnoreCase(datatype)) enabled = true;
//		else if ("real"             .equalsIgnoreCase(datatype)) enabled = true;
//		else if ("double precision" .equalsIgnoreCase(datatype)) enabled = true;
//
//		return enabled;
//	}

	public static boolean isDiffAllowedForDatatype(int jdbcType)
	{
		switch (jdbcType)
		{
		case java.sql.Types.BIT:           return false;
		case java.sql.Types.TINYINT:       return true; // allow
		case java.sql.Types.SMALLINT:      return true; // allow
		case java.sql.Types.INTEGER:       return true; // allow
		case java.sql.Types.BIGINT:        return true; // allow
		case java.sql.Types.FLOAT:         return true; // allow
		case java.sql.Types.REAL:          return true; // allow
		case java.sql.Types.DOUBLE:        return true; // allow
		case java.sql.Types.NUMERIC:       return true; // allow
		case java.sql.Types.DECIMAL:       return true; // allow
		case java.sql.Types.CHAR:          return false;
		case java.sql.Types.VARCHAR:       return false;
		case java.sql.Types.LONGVARCHAR:   return false;
		case java.sql.Types.DATE:          return false;
		case java.sql.Types.TIME:          return false;
		case java.sql.Types.TIMESTAMP:     return false;
		case java.sql.Types.BINARY:        return false;
		case java.sql.Types.VARBINARY:     return false;
		case java.sql.Types.LONGVARBINARY: return false;
		case java.sql.Types.NULL:          return false;
		case java.sql.Types.OTHER:         return false;
		case java.sql.Types.JAVA_OBJECT:   return false;
		case java.sql.Types.DISTINCT:      return false;
		case java.sql.Types.STRUCT:        return false;
		case java.sql.Types.ARRAY:         return false;
		case java.sql.Types.BLOB:          return false;
		case java.sql.Types.CLOB:          return false;
		case java.sql.Types.REF:           return false;
		case java.sql.Types.DATALINK:      return false;
		case java.sql.Types.BOOLEAN:       return false;

		//------------------------- JDBC 4.0 -----------------------------------
		case java.sql.Types.ROWID:         return false;
		case java.sql.Types.NCHAR:         return false;
		case java.sql.Types.NVARCHAR:      return false;
		case java.sql.Types.LONGNVARCHAR:  return false;
		case java.sql.Types.NCLOB:         return false;
		case java.sql.Types.SQLXML:        return false;

		//------------------------- VENDOR SPECIFIC TYPES ---------------------------
		case -10:                          return false; // "oracle.jdbc.OracleTypes.CURSOR";

		//------------------------- UNHANDLED TYPES  ---------------------------
		default:
			return false;
		}
	}

	public String debugToString()
	{
		StringBuilder sb = new StringBuilder();
		sb.append("\n");

		// Calculate with size for display
		int[] dlen = new int[getColumnCount()];
		// col headers
		List<String> colNames = getColNames();
		for (int i=0; i<dlen.length; i++)
		{
			if (i<colNames.size())
				dlen[i] = colNames.get(i).length();
		}
		// rows
		for (List<Object> rl : _rows)
		{
			for (int i=0; i<rl.size(); i++)
				dlen[i] = Math.max(dlen[i], rl.get(i).toString().length());
		}

		sb.append("#########################################################################\n");
		sb.append("name     = '").append(getName()       ).append("'\n");
		sb.append("colCount = ").append(getColumnCount() ).append("\n");
		sb.append("rowCount = ").append(getRowCount()    ).append("\n");
		sb.append("\n");
		sb.append("ColNames = ").append(getColNames()    ).append("\n");

		sb.append("\n");
		sb.append("RowId -> PkStr\n");
		for (int i=0; i<_rowidToKey.size(); i++)
			sb.append(i).append(" -> ").append(_rowidToKey.get(i)).append("\n");

		sb.append("\n");
		sb.append("PkStr -> RowId\n");
		for (String key : _keysToRowid.keySet())
			sb.append(StringUtil.left(key, 30, true)).append(" -> ").append(_keysToRowid.get(key)).append("\n");

		sb.append("\n");
		sb.append("Table Content\n");
		// colnames
		for (int i=0; i<colNames.size(); i++)
			sb.append(" ").append(StringUtil.left(colNames.get(i), dlen[i]));
		sb.append("\n");
		// -------
		for (int i=0; i<colNames.size(); i++)
			sb.append(" ").append(StringUtil.replicate("-", dlen[i]));
		sb.append("\n");
		for (List<Object> rl : _rows)
		{
			for (int i=0; i<rl.size(); i++)
				sb.append(" ").append(StringUtil.left(rl.get(i).toString(), dlen[i]));
			sb.append("\n");
		}
		sb.append("#########################################################################\n");
		
		sb.append("\n");
		return sb.toString();
	}

//	public Object getValueAsObject (int    rowId, int    colPos)                          { Object o = getValue     (rowId, colPos);  return o; }
//	public Object getValueAsObject (int    rowId, String colname)                         { return getValue         (rowId, colname, true); }
//	public Object getValueAsObject (int    rowId, String colname, boolean cs)             { return getValue         (rowId, colname,   cs); }
//	public Object getValueAsObject (String pkStr, String colname)                         { return getValue         (pkStr, colname, true); }
//	public Object getValueAsObject (String pkStr, String colname, boolean cs)             { return getValue         (pkStr, colname,   cs); }

//	public String getValueAsString (int    rowId, int    colPos)                          { Object o = getValueAsObject     (rowId, colPos);  return (o==null)?"":o.toString(); }
//	public String getValueAsString (int    rowId, String colname)                         { Object o = getValueAsObject     (rowId, colname, true); return (o==null)?"":o.toString(); }
//	public String getValueAsString (int    rowId, String colname, boolean cs)             { Object o = getValueAsObject     (rowId, colname,   cs); return (o==null)?"":o.toString(); }
//	public String getValueAsString (String pkStr, String colname)                         { Object o = getValueAsObject     (pkStr, colname, true); return (o==null)?"":o.toString(); }
//	public String getValueAsString (String pkStr, String colname, boolean cs)             { Object o = getValueAsObject     (pkStr, colname,   cs); return (o==null)?"":o.toString(); }
	
//	public Double getValueAsDouble (int    rowId, int    colPos)                          { return getValueAsDouble (DATA_RATE, rowId, colPos);             }
//	public Double getValueAsDouble (int    rowId, int    colPos, Double def)              { return getValueAsDouble (DATA_RATE, rowId, colPos, def);        }
//	public Double getValueAsDouble (int    rowId, String colname)                         { return getValueAsDouble (DATA_RATE, rowId, colname, true);      }
//	public Double getValueAsDouble (int    rowId, String colname, boolean cs)             { return getValueAsDouble (DATA_RATE, rowId, colname,   cs);      }
//	public Double getValueAsDouble (int    rowId, String colname, Double def)             { return getValueAsDouble (DATA_RATE, rowId, colname, true, def); }
//	public Double getValueAsDouble (int    rowId, String colname, boolean cs, Double def) { return getValueAsDouble (DATA_RATE, rowId, colname,   cs, def); }
//	public Double getValueAsDouble (String pkStr, String colname)                         { return getValueAsDouble (DATA_RATE, pkStr, colname, true);      }
//	public Double getValueAsDouble (String pkStr, String colname, boolean cs)             { return getValueAsDouble (DATA_RATE, pkStr, colname,   cs);      }
//	public Double getValueAsDouble (String pkStr, String colname, Double def)             { return getValueAsDouble (DATA_RATE, pkStr, colname, true, def); }
//	public Double getValueAsDouble (String pkStr, String colname, boolean cs, Double def) { return getValueAsDouble (DATA_RATE, pkStr, colname,   cs, def); }
//	
//	public String getRatePkValue       (int    rowId)                                         { return getPkValue       (DATA_RATE, rowId  ); }
//    
//	public int[]         getRateRowIdsWhere(String colname, String colval)                        { return getRowIdsWhere   (DATA_RATE, colname, true, colval); }
//	public int[]         getRateRowIdsWhere(String colname, boolean cs, String colval)            { return getRowIdsWhere   (DATA_RATE, colname,   cs, colval); }
//
//	public List<Integer> getRateRowIdsWhere(Map<String, Object> nameValue, boolean caseSensitive) { return getRowIdsWhere(DATA_RATE, nameValue, caseSensitive); }
//	public List<Integer> getRateRowIdsWhere(Map<String, Object> nameValue)                        { return getRowIdsWhere(DATA_RATE, nameValue, true); }

	

	//----------------------------------------------------------------------------------
	//-- getValueAsString
	//----------------------------------------------------------------------------------
	public String getValueAsString (int    rowId, int    colPos)                          { Object o = getValueAsObject     (rowId, colPos);  return (o==null)?"":o.toString(); }
	public String getValueAsString (int    rowId, String colname)                         { Object o = getValueAsObject     (rowId, colname, true); return (o==null)?"":o.toString(); }
	public String getValueAsString (int    rowId, String colname, boolean cs)             { Object o = getValueAsObject     (rowId, colname,   cs); return (o==null)?"":o.toString(); }
	public String getValueAsString (String pkStr, String colname)                         { Object o = getValueAsObject     (pkStr, colname, true); return (o==null)?"":o.toString(); }
	public String getValueAsString (String pkStr, String colname, boolean cs)             { Object o = getValueAsObject     (pkStr, colname,   cs); return (o==null)?"":o.toString(); }


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
				_logger.debug(getName()+".getValue(pkStr='"+pkStr+"', colname='"+colname+"'): rowId="+rowId+": rowId < 0; return null");
			return def;
		}

		// Got object for the RowID and column name
		Object o = getValueAsObject(rowId, colname, caseSensitive);
		if (o == null)
		{
			if (_logger.isDebugEnabled()) 
				_logger.debug(getName()+".getValue(pkStr='"+pkStr+"', colname='"+colname+"'): rowId="+rowId+": o==null; return null");
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
	//-- getValueAsInt
	//----------------------------------------------------------------------------------

//	// 
//	public int getValueAsInt(int rowId, int colPos)
//	{
//		return getValueAsInt(rowId, colPos, -1);
//	}
//
//	// 
//	public int getValueAsInt(int rowId, int colPos, int def)
//	{
//		Object o = getValueAsObject(rowId, colPos);
//		if (o == null)
//			return def;
//
//		if (o instanceof Number)
//			return ((Number) o).intValue();
//		else
//			return Integer.valueOf(Integer.parseInt(o.toString()));
//	}
//
//	// 
//	public int getValueAsInt(int rowId, String colname, boolean caseSensitive)
//	{
//		return getValueAsInt(rowId, colname, caseSensitive, -1);
//	}
//
//	// 
//	public int getValueAsInt(int rowId, String colname, boolean caseSensitive, int def)
//	{
//		Object o = getValueAsObject(rowId, colname, caseSensitive);
//		if (o == null)
//			return def;
//
//		if (o instanceof Number)
//			return ((Number) o).intValue();
//		else
//			return Integer.valueOf(Integer.parseInt(o.toString()));
//	}
//
//	// 
//	public int getValueAsInt(String pkStr, String colname, boolean caseSensitive)
//	{
//		return getValueAsInt(pkStr, colname, caseSensitive, -1);
//	}
//
//	// 
//	public int getValueAsInt(String pkStr, String colname, boolean caseSensitive, int def)
//	{
//		Object o = getValueAsObject(pkStr, colname, caseSensitive);
//		if (o == null)
//			return def;
//
//		if (o instanceof Number)
//			return ((Number) o).intValue();
//		else
//			return Integer.valueOf(Integer.parseInt(o.toString()));
//	}

	
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
	
	
	
	//----------------------------------------------------------------------------------
	//-- getRowIdsWhere
	//----------------------------------------------------------------------------------

	public List<Integer> getRowIdsWhere(String colname, String colvalue)
	{
		return getRowIdsWhere(colname, true, colvalue);
	}

	/**
	 * Get a int array of rows where colValue matches values in the column name
	 * 
	 * @param whatData DATA_ABS or DATA_DIFF or DATA_RATE
	 * @param colname Name of the column to search in
	 * @param colvalue Value to search for in the above column name
	 * @return int[] an array of integers where rows meets above search criteria, if nothing was found return null
	 */
	public List<Integer> getRowIdsWhere(String colname, boolean caseSensitive, String colvalue)
	{
		int idCol = findColumn(colname, caseSensitive);
		if (idCol == -1)
		{
			_logger.info("getRowIdsWhere: Can't find the column '" + colname + "'.");
			return null;
		}
		if (getRowCount() == 0)
			return null;

		ArrayList<Integer> rowsList = new ArrayList<Integer>();
		for (int i = 0; i < getRowCount(); i++)
		{
			Object o = getValueAt(i, idCol);
			if (o == null)
				continue;

			if (o.equals(colvalue))
				rowsList.add(i);
		}
//		if (rowsList.isEmpty())
//			return null;
//
//		// Convert the list into a array
//		int ia[] = new int[rowsList.size()];
//		for (int i=0; i<rowsList.size(); i++)
//			ia[i] = rowsList.get(i);
//
//		return ia;
		
		return rowsList;
	}

	public List<Integer> getRowIdsWhere(Map<String, Object> nameValue) 
	{ 
		return getRowIdsWhere(nameValue, true); 
	}

	/**
	 * Get all values that matches
	 * 
	 * @param nameValue   a map of column names/values that we are searching for
	 * @return            List of row numbers.   empty if no rows was found.
	 * @throws RuntimeException if any of the column names you searched for was not found
	 */
	public List<Integer> getRowIdsWhere(Map<String, Object> nameValue, boolean caseSensitive)
	{
		List<Integer> foundRows = new ArrayList<Integer>();
		
		int rowc = getRowCount();
		for (int r=0; r<rowc; r++)
		{
			int matchColCount = 0;
			for (Entry<String, Object> e : nameValue.entrySet())
			{
				String whereColName = e.getKey();
				Object whereColVal  = e.getValue();
				
				int colId = findColumn(whereColName, caseSensitive);
				if (colId == -1)
					throw new RuntimeException("Can't find column '"+whereColName+"' in TableModel named '"+getName()+"'.");

				Object rowColVal = getValueAt(r, colId);
				
//				if (whereColVal == null && rowColVal == NULL_REPLACE)
				if (whereColVal == null && rowColVal == null)
				{
					matchColCount++;
				}
				else if (whereColVal != null && whereColVal.equals(rowColVal))
				{
					matchColCount++;
				}
			}
			
			if (matchColCount == nameValue.size())
				foundRows.add(r);
		}
		
		return foundRows;
	}

	
	
	/**
	 * Sort on Many columns 
	 * 
	 * @param a List of sortOptions 
	 * 
	 * @return false if column(s) was NOT found. true if we did a sort
	 */
	public void sort(List<SortOptions> sortOptions)
	{
		Collections.sort(_rows, new ResultSetTableComparator(sortOptions, this, this.getName()));
		
		// Since we changed the underlying '_rows' List, then we need to adjust all "pointers" in: _rowidToKey and _keysToRowid
		newRowIds();
	}

//	/**
//	 * Sort on one column only. TODO: Many columns 
//	 * 
//	 * @param colName
//	 * @return false if column was NOT found. true if we did a sort
//	 */
//	public boolean sort(SortOptions sortOptions)
//	{
//		final String  colName                  = sortOptions.getColumnName();
//		final boolean isColNameCaseInSensitive = sortOptions.isColumnNameCaseInSensitive();
//
//		final boolean isAscending              = sortOptions.isAscending();
//		final boolean isCaseInSensitive        = sortOptions.isCaseInSensitive();
//		
//		final int colPos = findColumn(colName, isColNameCaseInSensitive);
//		if (colPos == -1)
//			return false;
//				
//		Comparator<List<Object>> comparator = new Comparator<List<Object>>()
//		{
//			@SuppressWarnings({ "rawtypes", "unchecked" })
//			@Override
//			public int compare(List<Object> leftList, List<Object> rightList)
//			{
//				Object left  = leftList .get(colPos);
//				Object right = rightList.get(colPos);
//				
//				if (left == right)
//					return sortType( 0 );
//
//				if (left == null)
//					return sortType( -1 );
//
//				if (right == null)
//					return sortType( 1 );
//
//				if ( isCaseInSensitive )
//				{
//					if (left instanceof String && right instanceof String)
//						return sortType( String.CASE_INSENSITIVE_ORDER.compare( (String) left, (String) right ) );
//				}
//				
//				// COMPARABLE, which would be the normal thing
//				if (left instanceof Comparable)
//					return sortType( ((Comparable)left).compareTo(right) );
//				
//				if ( left instanceof byte[] && right instanceof byte[] )
//					return sortType( byteCompare((byte[])left, (byte[])right) );
//
//				// End of line...
//				throw new RuntimeException("Comparator on object, colName='"+colName+"', problem: Left do not implement 'Comparable' and is not equal to right. Left.obj=|"+left.getClass().getCanonicalName()+"|, Right.obj=|"+right.getClass().getCanonicalName()+"|, Left.toString=|"+left+"|, Right.toString=|"+right+"|.");
//			}
//
//			private int byteCompare(byte[] left, byte[] right)
//			{
//				for (int i = 0, j = 0; i < left.length && j < right.length; i++, j++)
//				{
//					int a = (left[i] & 0xff);
//					int b = (right[j] & 0xff);
//					if ( a != b )
//					{
//						return a - b;
//					}
//				}
//				return left.length - right.length;
//			}
//			
//			private int sortType(int order)
//			{
//				if (isAscending)
//					return order;
//				
//				// Negate the number
//				// * negative number to positive
//				// * positive numbers negative
//				// * leave 0 as 0
//				return order *= -1;
//			}
//		};
//		
//		Collections.sort(_rows, comparator);
//		return true;
//	}


//	/**
//	 * Sort on Many columns 
//	 * 
//	 * @param a List of sortOptions 
//	 * 
//	 * @return false if column(s) was NOT found. true if we did a sort
//	 */
//	public boolean sort(final List<SortOptions> sortOptions)
//	{
//		Comparator<List<Object>> comparator = new Comparator<List<Object>>()
//		{
//			@Override
//			public int compare(List<Object> leftList, List<Object> rightList)
//			{
//				int result = 0;
//
//				for (SortOptions so : sortOptions)
//				{
//					String  colName                  = so.getColumnName();
//					boolean isColNameCaseInSensitive = so.isColumnNameCaseInSensitive();
//
//					boolean isAscending              = so.isAscending();
//					boolean isCaseInSensitive        = so.isCaseInSensitive();
//					
//					int colPos = findColumn(colName, isColNameCaseInSensitive);
//					if (colPos == -1)
//						throw new RuntimeException("Sorting CounterSample '" + getName() + "', cant find column name '" + colName + "'.");
//
//					result = compare(colName, colPos, isAscending, isCaseInSensitive, leftList, rightList);
//					if (result != 0)
//						return result;
//				}
//				return result;
//			}
//
//			@SuppressWarnings({ "rawtypes", "unchecked" })
//			private int compare(String colName, int colPos, boolean isAscending, boolean isCaseInSensitive, List<Object> leftList, List<Object> rightList)
//			{
//				Object left  = leftList .get(colPos);
//				Object right = rightList.get(colPos);
//				
//				if (left == right)
//					return sortType( 0, isAscending );
//
//				if (left == null)
//					return sortType( -1, isAscending );
//
//				if (right == null)
//					return sortType( 1, isAscending );
//
//				if ( isCaseInSensitive )
//				{
//					if (left instanceof String && right instanceof String)
//						return sortType( String.CASE_INSENSITIVE_ORDER.compare( (String) left, (String) right ), isAscending );
//				}
//				
//				// COMPARABLE, which would be the normal thing
//				if (left instanceof Comparable)
//					return sortType( ((Comparable)left).compareTo(right), isAscending );
//				
//				if ( left instanceof byte[] && right instanceof byte[] )
//					return sortType( byteCompare((byte[])left, (byte[])right), isAscending );
//
//				// End of line...
//				throw new RuntimeException("Comparator on object, colName='"+colName+"', problem: Left do not implement 'Comparable' and is not equal to right. Left.obj=|"+left.getClass().getCanonicalName()+"|, Right.obj=|"+right.getClass().getCanonicalName()+"|, Left.toString=|"+left+"|, Right.toString=|"+right+"|.");
//			}
//
//			private int byteCompare(byte[] left, byte[] right)
//			{
//				for (int i = 0, j = 0; i < left.length && j < right.length; i++, j++)
//				{
//					int a = (left[i] & 0xff);
//					int b = (right[j] & 0xff);
//					if ( a != b )
//					{
//						return a - b;
//					}
//				}
//				return left.length - right.length;
//			}
//			
//			private int sortType(int order, boolean isAscending)
//			{
//				if (isAscending)
//					return order;
//				
//				// Negate the number
//				// * negative number to positive
//				// * positive numbers negative
//				// * leave 0 as 0
//				return order *= -1;
//			}
//		};
//		
//		Collections.sort(_rows, comparator);
//		return true;
//	}
	
	
	
	//---------------------------------------------------------------------------------------------------
	//---------------------------------------------------------------------------------------------------
	//---------------------------------------------------------------------------------------------------
	// Basic test code for DIFF function
	//---------------------------------------------------------------------------------------------------
	//---------------------------------------------------------------------------------------------------
	//---------------------------------------------------------------------------------------------------
//	private static boolean testDiff(String test, Number expRes, Number prevVal, Number newVal)
//	{
//		Number res = diffColumnValue(prevVal, newVal, false, test, test, false);
//		
//		boolean ret = true;
//
//		// Check Expected VALUE
//		if ( ! expRes.equals(res) )
//		{
//			System.out.println("testDiff(): " + test + ": FAULTY VALUE: returned value='"+res+"', expected return value='"+expRes+"'. prevVal='"+prevVal+"', newVal='"+newVal+"'.");
//			ret = false;
//		}
//
//		// Check Expected DATATYPE
//		if ( ! expRes.getClass().getName().equals(res.getClass().getName()) )
//		{
//			System.out.println("testDiff(): " + test + ": FAULTY OBJECT TYPE: returned obj='"+res.getClass().getName()+"', expected return obj='"+expRes.getClass().getName()+"'.");
//			ret = false;
//		}
//		
//		if (ret)
//			System.out.println("testDiff(): " + test + ": OK.");
//
//		return ret;
//	}
//	
//	public static void main(String[] args)
//	{ // 3646 + -3647
//		//                 expected value     firstSampleValue             secondSampleValue
//		testDiff("test-int-1", Integer.valueOf(10),   Integer.valueOf(10),             Integer.valueOf(20));
//		testDiff("test-int-2", Integer.valueOf(1),    Integer.valueOf(2147483647),     Integer.valueOf(-2147483648));
//		testDiff("test-int-3", Integer.valueOf(3),    Integer.valueOf(2147483646),     Integer.valueOf(-2147483647));
//		testDiff("test-int-4", Integer.valueOf(7296), Integer.valueOf(2147480000),     Integer.valueOf(-2147480000));
//		testDiff("test-int-5", Integer.valueOf(10),   Integer.valueOf(-2000),          Integer.valueOf(-1990));
//		
//		testDiff("test-long-1", Long.valueOf(21),      Long.valueOf(Long.MAX_VALUE-10), Long.valueOf(Long.MIN_VALUE+10));
//		
//		testDiff("test-bd-1", new BigDecimal(10), new BigDecimal(10),    new BigDecimal(20));
//		
//		long maxUnsignedInt = 4294967295L; // 4 294 967 295
//		testDiff("this-should-fail",    new BigDecimal(2001), new BigDecimal(maxUnsignedInt-1000), new BigDecimal(1000));
//		testDiff(CmSpinlockSum.CM_NAME, new BigDecimal(2001), new BigDecimal(maxUnsignedInt-1000), new BigDecimal(1000)); // Special logic for CmSpinlockSum
//		testDiff("this-should-fail",    Long.valueOf(2001),       Long.valueOf(      maxUnsignedInt-1000), Long.valueOf(1000));
//		testDiff(CmSpinlockSum.CM_NAME, Long.valueOf(2001),       Long.valueOf(      maxUnsignedInt-1000), Long.valueOf(1000));       // Special logic for CmSpinlockSum
//		
//		// Basic algorithm: diff = SecondSample - FirstSample
//		// so 100 - 10 = 90
//		System.out.println("dummy test: this should result in 90 = " + Integer.valueOf( Integer.valueOf(100) - Integer.valueOf(10) ));
//
//		// lets try basic diff with counters that has overflow values ---          SecondSample               FirstSample
//		// ASE will cause 'Msg 3606: Arithmetic overflow occurred.'
//		System.out.println("dummy test: this should result in 3 = " + Integer.valueOf( Integer.valueOf(-2147483647) - Integer.valueOf(2147483646) ));
//	}
}
