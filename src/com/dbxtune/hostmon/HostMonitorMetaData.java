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
import java.math.BigDecimal;
import java.sql.ResultSetMetaData;
import java.sql.Time;
import java.sql.Timestamp;
import java.sql.Types;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.core.config.Configurator;

import com.dbxtune.gui.ResultSetTableModel;
import com.dbxtune.utils.Configuration;
import com.dbxtune.utils.MandatoryPropertyException;
import com.dbxtune.utils.PropPropEntry;
import com.dbxtune.utils.StringUtil;


public class HostMonitorMetaData
implements ResultSetMetaData
{
	private static final Logger _logger = LogManager.getLogger(MethodHandles.lookup().lookupClass());

	/** "\\s+" - \s+ Any Whitespace character: [ \t\n\x0B\f\r], then the '+' is just multiple times */
	public final static String REGEXP_IS_SPACE = "\\s+"; 

	/** This mean that it's a counter, used by summary which will tell you how many records were used when doing average/summary calculation */
	public static final int STATUS_COL_SUB_SAMPLE  = 1;
	/** This mean that it's the value System.currentTime() when the individual record/row was created, this is used when multiple rows will be displayed. meaning only 1 row per parser sample, while the output is not average/summary calculated... */
	public static final int STATUS_COL_SAMPLE_TIME = 2;

	/** A Map where we store values to be "skipped" for a specific column */
	private Map<String, Integer> _skipRowsList = null;

	/** A Map where we store values to be "allowed" for a specific column */
	private Map<String, Integer> _allowRowsList = null;
	
	
	private String  _schemaName    = "";
	private String  _tableName     = "";
	private String  _catalogName   = "";

	private String  _osCommand            = null;
	private boolean _osCommandIsStreaming = true;

	private SourceRowAdjuster _sourceRowAdjuster	= null;

	private String[] _descriptionGroups = new String[] {}; // empty array
	
	/** Class that holds a entry */
	public static class ColumnEntry implements Comparable<ColumnEntry>
	{
		int     _sqlColNum     = -1;
		int     _parseColNum   = -1;
		String  _colName       = null;
		int     _sqlType       = 0; // a value: java.sql.Types;
		String  _sqlDataType   = null;
		Class<?>_javaClass     = null;
		boolean _isNumber      = false;
		int     _displayLength = 0;
		boolean _isNullable    = true;
		boolean _isPartOfPk    = false;
		boolean _isStatColumn  = false; // is a statistics column

		boolean _isPct         = false; // if this column has Percent data, then it could be presented in another color
		boolean _isDiff        = false; // If this column is command is a -NON-streamimg-command- and we need to do DIFF calculations on this column

		String  _schemaName    = null;
		String  _tableName     = null;
		String  _catalogName   = null;

		int     _precision     = -1;
		int     _scale         = -1;

		String  _description   = "";

		int     _status        = 0;
		SimpleDateFormat _dateParseFormat = null;

		@Override
		public int compareTo(ColumnEntry ce)
		{
			return (this._sqlColNum < ce._sqlColNum ? -1 : (this._sqlColNum == ce._sqlColNum ? 0 : 1));
		}
	}

	/** Regexp to use when parsing */
	private String _regexpOnParse = REGEXP_IS_SPACE;

	/** 
	 * How many samples can be samples, the implemetor of the queue needs to read this value, 
	 * for HostMonitor this would be the <code>OsTableSampleHolder</code> class 
	 * */
	private int _maxQueueSize = 1000;

	/** A list of columns in the MetaData */
	private ArrayList<ColumnEntry> _columns = new ArrayList<ColumnEntry>();
	
	/** A shortcut to what column names that are available, maintained by addColumn() */
	private ArrayList<String> _columnNames = new ArrayList<String>();

	/** number of SQL columns, which can(will be delivered to caller). */
	private int _numOfSqlCols   = 0;

	/** number of columns to Parse from the OS Command */
	private int _numOfParseCols = 0;

	/**
	 * What regular expression to use when splitting a input row
	 * @return The regexp String
	 */
	public String getParseRegexp()
	{
		return _regexpOnParse;
	}

	/**
	 * What regular expression to use when splitting a input row
	 * @param regexp The regexp String
	 */
	public void setParseRegexp(String regexp)
	{
		if ( regexp == null || (regexp != null && regexp.equalsIgnoreCase("DEFAULT")) )
			_regexpOnParse = REGEXP_IS_SPACE;
		else
			_regexpOnParse = regexp;
	}

	/** 
	 * How many samples can be samples, the implemetor of the queue needs to read this value, 
	 * for HostMonitor this would be the <code>OsTableSampleHolder</code> class 
	 * */
	public int getMaxQueueSize()
	{
		return _maxQueueSize;
	}
	
	/** 
	 * Set how many samples can be samples, the implemetor of the queue needs to read this value, 
	 * via <code>getMaxQueueSize()</code> for HostMonitor this would be the <code>OsTableSampleHolder</code> class 
	 * */
	public void setMaxQueueSixe(int maxQueueSize)
	{
		_maxQueueSize = maxQueueSize;
	}

	/**
	 * Get a list of all available Column Entries in the Metadata<br>
	 * This makes looping all columns a bit simpler.
	 * @return a List of ColumnEntry. If not entries it would be a empty List.
	 */
	public List<ColumnEntry> getColumns()
	{
		return _columns;
	}
	
	/**
	 * Check if there is a SQL Column name 
	 * @param name SQL Column name
	 * @return true or false
	 */
	public boolean hasColumn(String name)
	{
		return findColumn(name) != -1;
	}

	/**
	 * Get index (starting at 0) of the SQL Position for a column name!
	 * @param name SQL Column name
	 * @return The position (starting at 0)  or -1 if not found.
	 */
	public int findColumn(String name)
	{
		for (ColumnEntry e: _columns)
		{
			if (e._colName.equals(name))
				return e._sqlColNum;
		}
		return -1;
	}

	/**
	 * Get a ColumnEntry for a specific column NAME
	 * @param name Name of the column to get info about
	 * @return a Meta Data ColumnEntry 
	 * @throws RuntimeException if the column could NOT be found.
	 */
	protected ColumnEntry getColumn(String name)
	{
		for (ColumnEntry e: _columns)
		{
			if (e._colName.equals(name))
				return e;
		}
		throw new RuntimeException("The getColumn("+name+") could not be found.");
	}

	/**
	 * Get a ColumnEntry for a specific column number that is a part of the <b>SQL</b> resultset.
	 * @param col ID, NOTE: starts at 1. So 1=firstcol, 2=secondcol...
	 * @return a Meta Data ColumnEntry 
	 * @throws RuntimeException if the column could NOT be found.
	 */
	public ColumnEntry getSqlColumn(int col)
	{
		for (ColumnEntry e: _columns)
		{
			if (e._sqlColNum == col)
				return e;
		}
		throw new RuntimeException("The getSqlColumn("+col+") could not be found.");
	}

	/**
	 * Get a ColumnEntry for a specific column number that is a part of the <b>Parse</b> set.
	 * @param col ID, NOTE: starts at 1. So 1=firstcol, 2=secondcol...
	 * @return a Meta Data ColumnEntry 
	 * @throws RuntimeException if the column could NOT be found.
	 */
	public ColumnEntry getParseColumn(int col)
	{
		for (ColumnEntry e: _columns)
		{
			if (e._parseColNum == col)
				return e;
		}
		throw new RuntimeException("The getParseColumn("+col+") could not be found.");
	}

	/**
	 * Get the position in the array where to find data for a specific column name 
	 * @param colname the column to serach for
	 * @return The array position of the String[] array (this could be used in parseRow()...)
	 * @throws RuntimeException if the column could NOT be found.
	 */
	protected int getParseColumnArrayPos(String colname)
	{
		ColumnEntry ce = getColumn(colname);
		return ce._parseColNum - 1;
	}

	/**
	 * Check name and other stuff...
	 * 
	 * @param entry
	 * @throws RuntimeException in case of any problems
	 */
	private void checkColumnEntry(ColumnEntry entry)
	throws RuntimeException
	{
		if (entry._colName == null)                               throw new RuntimeException("Column name can't be null");
		if (entry._colName.equalsIgnoreCase(""))                  throw new RuntimeException("Column name can't be empty, named ''.");
		if (entry._colName.equalsIgnoreCase("SessionStartTime"))  throw new RuntimeException("Column name 'SessionStartTime' is reserved by the PCS, Persistence Counter Storage.");
		if (entry._colName.equalsIgnoreCase("SessionSampleTime")) throw new RuntimeException("Column name 'SessionSampleTime' is reserved by the PCS, Persistence Counter Storage.");
		if (entry._colName.equalsIgnoreCase("CmSampleTime"))      throw new RuntimeException("Column name 'CmSampleTime' is reserved by the PCS, Persistence Counter Storage.");
		if (entry._colName.equalsIgnoreCase("CmSampleMs"))        throw new RuntimeException("Column name 'CmSampleMs' is reserved by the PCS, Persistence Counter Storage.");
	}

	/**
	 * Add a specific ColumnEntry to the Meta Data
	 * @param entry a Meta Data ColumnEntry
	 */
	private void addColumn(ColumnEntry entry)
	{
		checkColumnEntry(entry);
		
		if (entry._schemaName  == null) entry._schemaName  = getSchemaName();
		if (entry._tableName   == null) entry._tableName   = getTableName();
		if (entry._catalogName == null) entry._catalogName = getCatalogName();

		_columns.add(entry);
		
		// Fix the internal counters.
		_numOfSqlCols   = 0;
		_numOfParseCols = 0;
		for (ColumnEntry e: _columns)
		{
			_numOfSqlCols   = Math.max(e._sqlColNum,   _numOfSqlCols);
			_numOfParseCols = Math.max(e._parseColNum, _numOfParseCols);
		}
		
		// If the columns are added in the "wrong" order, sort them here by SQL COlumn
		Collections.sort(_columns);

		// Create the List of column names
		_columnNames = new ArrayList<String>();
		for (ColumnEntry e: _columns)
			_columnNames.add(e._colName);
	}

	/**
	 * Add a column of type String
	 * @param colName           Name of the column
	 * @param sqlColumnNumber   SQL Column number. 0 or < 0 = not part of the SQL results NOTE: 1=col1, 2=col2...
	 * @param parseColumnNumber Column number to parse from the OS command.  0 or < 0 = not part of the columns to parse NOTE: 1=parseCol1, 2=parseCol2...
	 * @param isNullable        Is can this be a NULL result
	 * @param length            The MAX length of the string
	 * @param description       A description of the column that can be used a s a graphical tooltip
	 */
	public void addStrColumn(String colName, 
			int sqlColumnNumber, int parseColumnNumber, 
			boolean isNullable, 
			int length, String description)
	{
		ColumnEntry entry = new ColumnEntry();

		entry._colName       = colName;

		entry._sqlColNum     = sqlColumnNumber;
		entry._parseColNum   = parseColumnNumber;

		entry._isPartOfPk    = false;
		entry._isNullable    = isNullable;

		entry._isStatColumn  = false;
		entry._sqlType       = Types.VARCHAR;
		entry._sqlDataType   = "varchar";
		entry._javaClass     = String.class;
		entry._isNumber      = false;

		entry._displayLength = length;
		entry._precision     = length;
		entry._scale         = -1;

		entry._description   = description;
		
		addColumn(entry);
	}

	/**
	 * Add a column of type Integer
	 * @param colName           Name of the column
	 * @param sqlColumnNumber   SQL Column number. 0 or < 0 = not part of the SQL results NOTE: 1=col1, 2=col2...
	 * @param parseColumnNumber Column number to parse from the OS command.  0 or < 0 = not part of the columns to parse NOTE: 1=parseCol1, 2=parseCol2...
	 * @param isNullable        Is can this be a NULL result
	 * @param description       A description of the column that can be used a s a graphical tooltip
	 */
	public void addIntColumn(String colName, 
			int sqlColumnNumber, int parseColumnNumber, 
			boolean isNullable, 
			String description)
	{
		ColumnEntry entry = new ColumnEntry();

		entry._colName       = colName;

		entry._sqlColNum     = sqlColumnNumber;
		entry._parseColNum   = parseColumnNumber;

		entry._isPartOfPk    = false;
		entry._isNullable    = isNullable;

		entry._isStatColumn  = false;
		entry._sqlType       = Types.INTEGER;
		entry._sqlDataType   = "int";
		entry._javaClass     = Integer.class;
		entry._isNumber      = true;

		entry._displayLength = Math.max(colName.length(), Integer.toString(Integer.MAX_VALUE).length());
		entry._precision     = 4; // 4 bytes...
		entry._scale         = -1;

		entry._description   = description;
		
		addColumn(entry);
	}

	/**
	 * Add a column of type Integer
	 * @param colName           Name of the column
	 * @param sqlColumnNumber   SQL Column number. 0 or < 0 = not part of the SQL results NOTE: 1=col1, 2=col2...
	 * @param parseColumnNumber Column number to parse from the OS command.  0 or < 0 = not part of the columns to parse NOTE: 1=parseCol1, 2=parseCol2...
	 * @param isNullable        Is can this be a NULL result
	 * @param description       A description of the column that can be used a s a graphical tooltip
	 */
	public void addLongColumn(String colName, 
			int sqlColumnNumber, int parseColumnNumber, 
			boolean isNullable, 
			String description)
	{
		ColumnEntry entry = new ColumnEntry();

		entry._colName       = colName;

		entry._sqlColNum     = sqlColumnNumber;
		entry._parseColNum   = parseColumnNumber;

		entry._isPartOfPk    = false;
		entry._isNullable    = isNullable;

		entry._isStatColumn  = false;
		entry._sqlType       = Types.BIGINT;
		entry._sqlDataType   = "bigint";
		entry._javaClass     = Long.class;
		entry._isNumber      = true;

		entry._displayLength = Math.max(colName.length(), Long.toString(Long.MAX_VALUE).length());
		entry._precision     = 8; // 8 bytes
		entry._scale         = -1;

		entry._description   = description;
		
		addColumn(entry);
	}

	/**
	 * Add a column of type Decimal
	 * @param colName           Name of the column
	 * @param sqlColumnNumber   SQL Column number. 0 or < 0 = not part of the SQL results NOTE: 1=col1, 2=col2...
	 * @param parseColumnNumber Column number to parse from the OS command.  0 or < 0 = not part of the columns to parse NOTE: 1=parseCol1, 2=parseCol2...
	 * @param isNullable        Is can this be a NULL result
	 * @param precision         Precision of the number 
	 * @param scale             Scale of the number 
	 * @param description       A description of the column that can be used a s a graphical tooltip
	 */
	public void addDecColumn(String colName,
			int sqlColumnNumber, int parseColumnNumber,
			boolean isNullable,
			int precision, int scale, 
			String description)
	{
		ColumnEntry entry = new ColumnEntry();

		entry._colName       = colName;

		entry._sqlColNum     = sqlColumnNumber;
		entry._parseColNum   = parseColumnNumber;

		entry._isPartOfPk    = false;
		entry._isNullable    = isNullable;

		entry._isStatColumn  = false;
		entry._sqlType       = Types.NUMERIC;
		entry._sqlDataType   = "numeric";  // "numeric" or "decimal"
		entry._javaClass     = BigDecimal.class;
		entry._isNumber      = true;

		entry._displayLength = Math.max(colName.length(), precision + 1);
		entry._precision     = precision;
		entry._scale         = scale;

		entry._description       = description;
		
		addColumn(entry);
	}

	/**
	 * Add a column of type Datetime...
	 * @param colName           Name of the column
	 * @param sqlColumnNumber   SQL Column number. 0 or < 0 = not part of the SQL results NOTE: 1=col1, 2=col2...
	 * @param parseColumnNumber Column number to parse from the OS command.  0 or < 0 = not part of the columns to parse NOTE: 1=parseCol1, 2=parseCol2...
	 * @param isNullable        Is can this be a NULL result
	 * @param description       A description of the column that can be used a s a graphical tooltip
	 */
	public void addDatetimeColumn(String colName, 
			int sqlColumnNumber, int parseColumnNumber, 
			boolean isNullable, 
			String description)
	{
		ColumnEntry entry = new ColumnEntry();

		entry._colName       = colName;

		entry._sqlColNum     = sqlColumnNumber;
		entry._parseColNum   = parseColumnNumber;

		entry._isPartOfPk    = false;
		entry._isNullable    = isNullable;

		entry._isStatColumn  = false;
		entry._sqlType       = Types.TIMESTAMP;
		entry._sqlDataType   = "datetime";
		entry._javaClass     = Timestamp.class;
		entry._isNumber      = false;

		entry._displayLength = Math.max(colName.length(), "2011-01-01 22:22:22.333".length());
		entry._precision     = "yyyy-MM-dd HH:mm:ss.SSS".length();
		entry._scale         = -1;

		entry._description   = description;
		
		addColumn(entry);
	}

	/**
	 * Add a column That should be a part of the calculated output. 
	 * @param colName           Name of the column
	 * @param sqlColumnNumber   SQL Column number. 0 or < 0 = not part of the SQL results NOTE: 1=col1, 2=col2...
	 * @param parseColumnNumber Column number to parse from the OS command.  0 or < 0 = not part of the columns to parse NOTE: 1=parseCol1, 2=parseCol2...
	 * @param isNullable        Is can this be a NULL result
	 * @param precision         Precision of the number 
	 * @param scale             Scale of the number 
	 * @param description       A description of the column that can be used a s a graphical tooltip
	 */
	public void addStatColumn(String colName,
			int sqlColumnNumber, int parseColumnNumber,
			boolean isNullable,
			int precision, int scale, 
			String description)
	{
		ColumnEntry entry = new ColumnEntry();

		entry._colName       = colName;

		entry._sqlColNum     = sqlColumnNumber;
		entry._parseColNum   = parseColumnNumber;

		entry._isPartOfPk    = false;
		entry._isNullable    = isNullable;

		entry._isStatColumn  = true;
		entry._sqlType       = Types.NUMERIC;
		entry._sqlDataType   = "numeric";  // "numeric" or "decimal"
		entry._javaClass     = BigDecimal.class;
		entry._isNumber      = true;

		entry._displayLength = Math.max(colName.length(), precision + 1);
		entry._precision     = precision;
		entry._scale         = scale;

		entry._description       = description;
		
		addColumn(entry);
	}

	/**
	 * This will be used by the parser to discard "header rows", or other unwanted rows.<br>
	 * If the row value count matches the "parse count", the it can still be a invalid column header that we want to skip
	 * 
	 * @param colname the columns where where a specific value to search for
	 * @param valueToSkip The regexp value in the specified column name
	 */
	public void setSkipRows(String colname, String valueToSkip)
	{
		if (_skipRowsList == null)
			_skipRowsList = new HashMap<String, Integer>();

		ColumnEntry entry = getColumn(colname);
		
		_skipRowsList.put(valueToSkip, entry._parseColNum - 1);
	}

	/**
	 * Get a Map of values to be skipped for a specific row and column.
	 * @return
	 */
	public Map<String, Integer> getSkipRowsMap()
	{
		return _skipRowsList;
	}

	/**
	 * This will be used by the parser to only allow "a specific set of records"<br>
	 * 
	 * 
	 * @param colname the columns where where a specific value to search for
	 * @param valueToAllow The regexp value in the specified column name
	 */
	public void setAllowRows(String colname, String valueToAllow)
	{
		if (_allowRowsList == null)
			_allowRowsList = new HashMap<String, Integer>();

		ColumnEntry entry = getColumn(colname);
		
		_allowRowsList.put(valueToAllow, entry._parseColNum - 1);
	}

	/**
	 * Get a Map of values to be allowed for a specific row and column.
	 * @return
	 */
	public Map<String, Integer> getAllowRowsMap()
	{
		return _allowRowsList;
	}

	/**
	 * Set a specific column to be used as a Primary Key, this is used when doing summary and average calculation
	 * @param colname name(s)
	 * @throws RuntimeException if the column name couldn't be found.
	 */
	public void setPkCol(String... colNames)
	{
		// First reset all...
		for (ColumnEntry entry: _columns)
		{
			entry._isPartOfPk = false;
		}
		// Then set all the columns
		for (String colname : colNames)
		{
			ColumnEntry entry = getColumn(colname);
			entry._isPartOfPk = true;
			entry._isNullable = false;
		}
	}

	public boolean hasPkCols()
	{
		for (ColumnEntry e: _columns)
		{
			if (e._isPartOfPk)
				return true;
		}
		return false;
	}

	/**
	 * Set specific statuses to a specific column
	 * @param colname
	 * @param status
	 * @throws RuntimeException if the column name couldn't be found.
	 */
	public void setStatusCol(String colname, int status)
	{
		ColumnEntry entry = getColumn(colname);
		entry._status = entry._status | status;
	}
	
	/**
	 * Marks this column as it contains percentage data, then the viewer can present the data in another color
	 * @param colname names()
	 * @throws RuntimeException if the column name couldn't be found.
	 */
	public void setPercentCol(String... colNames)
	{
		for (String colname : colNames)
		{
			ColumnEntry entry = getColumn(colname);
			entry._isPct = true;
		}
	}
	
	/**
	 * Does this column contain Percentage data
	 * @param column column index: NOTE starts at 1: 1=firstcol, 2=secondcol...
	 * @return true if its a Percent column
	 * @throws RuntimeException if the column name couldn't be found.
	 */
	public boolean isPctColumn(int column)
	{
		return getSqlColumn(column)._isPct;
	}


	/**
	 * Marks this column as it contains DIFF data, then the viewer can present the data in another color
	 * @param colname names()
	 * @throws RuntimeException if the column name couldn't be found.
	 */
	public void setDiffCol(String... colNames)
	{
		for (String colname : colNames)
		{
			ColumnEntry entry = getColumn(colname);
			entry._isDiff = true;
		}
	}
	
	/**
	 * Does this column contain DIFF data
	 * @param column column index: NOTE starts at 1: 1=firstcol, 2=secondcol...
	 * @return true if its a Percent column
	 * @throws RuntimeException if the column name couldn't be found.
	 */
	public boolean isDiffColumn(int column)
	{
		return getSqlColumn(column)._isDiff;
	}


	/**
	 * If any columns (in non-streaming-mode needs to be diff/rate calculated...
	 * @return
	 */
	public boolean isDiffEnabled()
	{
		for (ColumnEntry e: _columns)
		{
			if (e._isDiff)
				return true;
		}
		return false;
	}

	/**
	 * Set date parse format for this column
	 * @param colname
	 * @param pattern How the parsed datetime format looks like, see {@link SimpleDateFormat} for more info
	 * @throws RuntimeException if the column name couldn't be found.
	 */
	public void setDateParseFormatCol(String colname, String pattern)
	{
		ColumnEntry entry = getColumn(colname);
		SimpleDateFormat sdf = new SimpleDateFormat(pattern);
		entry._dateParseFormat = sdf;
	}
	
	/**
	 * Get number of Columns that will be parsed
	 * @return
	 */
	public int getParseColumnCount()
	{
		return _numOfParseCols;
	}

	/**
	 * Get the description of the column, this could for example be used as a GUI ToolTip
	 * @param colName Name of the column
	 * @return The description
	 */
	public String getDescription(String colname)
	{
		ColumnEntry entry = getColumn(colname);
		return entry._description;
	}

	/** 
	 * If any tooltip or description needs to have one or several "group" names we can find the description in
	 * @return
	 */
	public String[] getDescriptionGroup()
	{
		if (_descriptionGroups == null)
			_descriptionGroups = new String[] {}; // empty array;

		return _descriptionGroups;
	}
	/** 
	 * If any tooltip or description needs to have one or several "group" names we can find the description in
	 * @return
	 */
	public void setDescriptionGroup(String[] sa)
	{
		if (sa == null)
			sa = new String[] {}; // empty array;
		
		_descriptionGroups = sa;
	}
	
	/**
	 * Create a new Object, the type of Object is located in the passed ColumnEntry
	 * @param ce ColumnEntry which includes what Object type that will be created
	 * @param val The String representation of the new Object that will be created.
	 * @return A new instantiated Object of the data type that is specified by the ColumnEntry.
	 */
	public Object newObject(ColumnEntry ce, String val)
	{
		Object newObject = null;
		// Handle some data types in a special way (needs special parsing)
		if (    ce._sqlType == Types.TIMESTAMP 
		     || ce._sqlType == Types.DATE 
		     || ce._sqlType == Types.TIME
		   ) 
		{
			if (ce._dateParseFormat == null)
				throw new RuntimeException("The column '"+ce._colName+"' has not set the date parsing format.");
			else
			{
				if (val == null)
				{
					// Not 100% sure if this is correct (or if we should just return null)
					if (ce._isNullable)
						newObject = null;
					else
						newObject = getDefaultValForNull(ce._sqlType, val, 0);
				}
				else
				{
					try 
					{
						Date date = ce._dateParseFormat.parse(val);
						if (ce._sqlType == Types.TIMESTAMP) newObject = new Timestamp( date.getTime() ); 
						if (ce._sqlType == Types.DATE)      newObject = new Date( date.getTime() );
						if (ce._sqlType == Types.TIME)      newObject = new Time( date.getTime() );
					}
					catch (ParseException e) 
					{
						throw new RuntimeException("The column '"+ce._colName+"' can't parsing using the format '"+ce._dateParseFormat+"'.", e);
					}
				}
			}
		}
		// ALL other data types is handled in a sub method... 
		else
		{
			try
			{
				// note: if unsupported data type a RuntimeExecption will be thrown by createJavaObject()
				newObject = createJavaObject(ce, val);
			}
			catch (RuntimeException e)
			{
				throw new RuntimeException("Problems when creating a new Object for the column '"+ce._colName+"'. Caught: "+e.getMessage(), e);
			}
		}

		return newObject;
	}

	/**
	 * Get rid of internationalazation for numbers.<br>
	 * Meaning '12,123' is translated to '12.123'
	 * @param str input string
	 * @return the translated string
	 */
	private String normalizeNumber(String str)
	{
		if (str == null)
			return null;

		int commaPos = str.indexOf(',');
		int dotPos   = str.indexOf('.');
		
		// No commas or dots, get out of here
		if (commaPos < 0 && dotPos < 0)
			return str;

		// Only dots, do nothing
		if (commaPos < 0 && dotPos >= 0)
			return str;

		// Only commas: translate ',' to '.'
		if (commaPos >= 0 && dotPos < 0)
			return str.replace(',', '.');

		// Both commas and Dots
		if (commaPos >= 0 && dotPos >= 0)
		{
			// if number is "fancy formated" as number: 123,456.0  ==>> 123456.0
			if (commaPos < dotPos)
				return str.replace(",", "");

			// if number is "fancy formated" as number: 123.456,0  ==>> 123456.0
			if (dotPos < commaPos)
			{
				str = str.replace(".", "");
				return str.replace(',', '.');
			}
		}

		// we probably never reach here, but if we do, just return the input
		return str;
	}

	/**
	 * Internal SQL type -> Java Object method
	 * @param sqlType
	 * @param val
	 * @return
	 * @throws RuntimeException on unsupported data types (and parse exceptions for DATE/TIME/TIMESTAMP)
	 */
	private Object createJavaObject(ColumnEntry ce, Object val)
	{
		int sqlType = ce._sqlType;
		int scale = ce._scale < 0 ? 0 : ce._scale;

		// fix NULL values
		if (val == null)
			return getDefaultValForNull(sqlType, val, scale);
		
		String strVal = val.toString();

		try
		{
			switch (sqlType)
			{
			case java.sql.Types.BIT:          return Boolean.valueOf(strVal);
			case java.sql.Types.TINYINT:      return Byte   .valueOf(Byte.parseByte(strVal));
			case java.sql.Types.SMALLINT:     return Short  .valueOf(Short.parseShort(strVal));
			case java.sql.Types.INTEGER:      return Integer.valueOf(strVal);
			case java.sql.Types.BIGINT:       return Long   .valueOf(strVal);
			case java.sql.Types.FLOAT:        return Float  .valueOf(strVal);
			case java.sql.Types.REAL:         return Float  .valueOf(strVal);
			case java.sql.Types.DOUBLE:       return Double .valueOf(normalizeNumber(strVal));
			case java.sql.Types.NUMERIC:      return new BigDecimal(normalizeNumber(strVal)).setScale(scale, BigDecimal.ROUND_HALF_EVEN);
			case java.sql.Types.DECIMAL:      return new BigDecimal(normalizeNumber(strVal)).setScale(scale, BigDecimal.ROUND_HALF_EVEN);
			case java.sql.Types.CHAR:         return new String(strVal);
			case java.sql.Types.VARCHAR:      return new String(strVal);
			case java.sql.Types.LONGVARCHAR:  return new String(strVal);
			case java.sql.Types.DATE:         return new Date( SimpleDateFormat.getInstance().parse(strVal).getTime() );
			case java.sql.Types.TIME:         return new Time( SimpleDateFormat.getInstance().parse(strVal).getTime() );
			case java.sql.Types.TIMESTAMP:    return new Timestamp( SimpleDateFormat.getInstance().parse(strVal).getTime() );
			case java.sql.Types.BINARY:       return new byte[0];
			case java.sql.Types.VARBINARY:    return new byte[0];
			case java.sql.Types.LONGVARBINARY:return new byte[0];
			case java.sql.Types.NULL:         return null;
			case java.sql.Types.OTHER:        return new Object();
			case java.sql.Types.JAVA_OBJECT:  return new Object();
//			case java.sql.Types.DISTINCT:     return "-DISTINCT-";
//			case java.sql.Types.STRUCT:       return "-STRUCT-";
//			case java.sql.Types.ARRAY:        return "-ARRAY-";
			case java.sql.Types.BLOB:         return val;
			case java.sql.Types.CLOB:         return val;
//			case java.sql.Types.REF:          return "-REF-";
//			case java.sql.Types.DATALINK:     return "-DATALINK-";
			case java.sql.Types.BOOLEAN:      return Boolean.valueOf(strVal);
			default:
				throw new RuntimeException("Unsupported data type was found in the dictionary java.sql.Types '"+sqlType+"', value='"+val+"'.");
			}
		}
		catch (NumberFormatException e)
		{
			_logger.warn("When creating a new Object for column '"+ce._colName+"' of java.sql.Types='"+sqlType+"' ["+ResultSetTableModel.getColumnJavaSqlTypeName(sqlType)+"] with value '"+val+"', for osCommand='"+_osCommand+"'. A NumberFormatException was thrown, which implies that the value is not a number. Creating the object with a zero value instead.");
			return createJavaObject(ce, null);
		}
		catch (ParseException e) // for: SimpleDateFormat.parse()
		{
			throw new RuntimeException("Problems parsing value '"+strVal+"' of java.sql.Types '"+sqlType+"'. Caught: "+e.getMessage(), e);
		}
	}

	/**
	 * Internal: if data is a null pointer, go and get some default value, based on SQL type -> Java Object
	 * 
	 * @param sqlType
	 * @param val
	 * @return
	 * @throws RuntimeException on unsupported data types
	 */
	private Object getDefaultValForNull(int sqlType, Object val, int scale)
	{
		if (val != null)
			return val;

		if ( scale < 0 )
			scale = 0;

		switch (sqlType)
		{
		case java.sql.Types.BIT:          return Boolean.valueOf(false);
		case java.sql.Types.TINYINT:      return Byte   .valueOf(Byte.parseByte("0"));
		case java.sql.Types.SMALLINT:     return Short  .valueOf(Short.parseShort("0"));
		case java.sql.Types.INTEGER:      return Integer.valueOf(0);
		case java.sql.Types.BIGINT:       return Long   .valueOf(0);
		case java.sql.Types.FLOAT:        return Float  .valueOf(0);
		case java.sql.Types.REAL:         return Float  .valueOf(0);
		case java.sql.Types.DOUBLE:       return Double .valueOf(0);
		case java.sql.Types.NUMERIC:      return new BigDecimal(0).setScale(scale, BigDecimal.ROUND_HALF_EVEN);
		case java.sql.Types.DECIMAL:      return new BigDecimal(0).setScale(scale, BigDecimal.ROUND_HALF_EVEN);
		case java.sql.Types.CHAR:         return "";
		case java.sql.Types.VARCHAR:      return "";
		case java.sql.Types.LONGVARCHAR:  return "";
		case java.sql.Types.DATE:         return new Date(0);
		case java.sql.Types.TIME:         return new Time(0);
		case java.sql.Types.TIMESTAMP:    return new Timestamp(0);
		case java.sql.Types.BINARY:       return new byte[0];
		case java.sql.Types.VARBINARY:    return new byte[0];
		case java.sql.Types.LONGVARBINARY:return new byte[0];
		case java.sql.Types.NULL:         return null;
		case java.sql.Types.OTHER:        return null;
		case java.sql.Types.JAVA_OBJECT:  return new Object();
//		case java.sql.Types.DISTINCT:     return "-DISTINCT-";
//		case java.sql.Types.STRUCT:       return "-STRUCT-";
//		case java.sql.Types.ARRAY:        return "-ARRAY-";
		case java.sql.Types.BLOB:         return "";
		case java.sql.Types.CLOB:         return "";
//		case java.sql.Types.REF:          return "-REF-";
//		case java.sql.Types.DATALINK:     return "-DATALINK-";
		case java.sql.Types.BOOLEAN:      return Boolean.valueOf(false);
		default:
			throw new RuntimeException("Unsupported data type was found in the dictionary java.sql.Types '"+sqlType+"', value='"+val+"'.");
		}
	}
	
	
	public void setSchemaName(String schemaName)   { _schemaName = schemaName; }
	public void setTableName(String tableName)     { _tableName = tableName; }
	public void setCatalogName(String catalogName) { _catalogName = catalogName; }

	public String getSchemaName()  { return _schemaName; }
	public String getTableName()   { return _tableName; }
	public String getCatalogName() { return _catalogName; }


	/**
	 * What Operating System command should be executed
	 * @param osCommand
	 */
	public void setOsCommand(String osCommand) 
	{ 
		_osCommand = osCommand; 
	}
	/**
	 * @return The Operating System command that will be executed
	 */
	public String  getOsCommand()         
	{
		return _osCommand; 
	}

	/**
	 * Set whether Operating System Command is streaming or non-streaming command.
	 * <p>
	 * Commands like 'iostat 2', 'vmstat 1' and 'mpstat 2' is continuing to deliver 
	 * rows until it's terminated, then it's a streaming command.<br>
	 * For streaming commands a background thread is started that reads the never ending streaming 
	 * rows from the command.
	 * <p>
	 * For Commands like 'ls -l' or 'cat filename' is a non-streaming commands and is 
	 * executed every time the client/caller is requesting data. 
	 * 
	 * @param isStreaming true or false
	 */
	public void setOsCommandStreaming(boolean isStreaming)
	{ 
		_osCommandIsStreaming = isStreaming; 
	}
	/**
	 * Is this Operating System Command a streaming command.
	 * <p>
	 * Commands like 'iostat 2', 'vmstat 1' and 'mpstat 2' is continuing to deliver 
	 * rows until it's terminated, then it's a streaming command.<br>
	 * For streaming commands a background thread is started that reads the never ending streaming 
	 * rows from the command.
	 * <p>
	 * For Commands like 'ls -l' or 'cat filename' is a non-streaming commands and is 
	 * executed every time the client/caller is requesting data. 
	 * @return true for streaming commands, false for non-streaming commands
	 */
	public boolean isOsCommandStreaming() 
	{
		return _osCommandIsStreaming; 
	}

	/**
	 * Get a List of Column names
	 * @return
	 */
	public List<String> getColumnNames()
	{
		return _columnNames;
	}
	
	/**
	 * Get a some debug info about the row.<br>
	 * Probably Used for debugging
	 * @return a String 
	 */
	public String toTraceString()
	{
		StringBuilder sb = new StringBuilder();

		sb.append("ModuleName/TabName       = '").append(getTableName())        .append("'\n");
		sb.append("OS Command               = '").append(getOsCommand())        .append("'\n");
		sb.append("OS Command is streaming  = '").append(isOsCommandStreaming()).append("'\n");
		sb.append("Split Regular Expression = '").append(getParseRegexp())      .append("'\n");
		sb.append("SQL Column Count         = '").append(getColumnCount())      .append("'\n");
		sb.append("Parse Column Count       = '").append(getParseColumnCount()) .append("'\n");
		sb.append("Max Queue Size           = '").append(getMaxQueueSize())     .append("'\n");
		sb.append("Skip Rows Map            = '").append(getSkipRowsMap())      .append("'\n");
		int e=0;
		for (HostMonitorMetaData.ColumnEntry ce : getColumns())
		{
			e++;
			sb.append("entry ").append(StringUtil.right(e+"",2)).append(": ");
			sb.append("colName='")     .append(StringUtil.left(ce._colName      +"', ",3+ 30));
			sb.append("isPartOfPk='")  .append(StringUtil.left(ce._isPartOfPk   +"', ",3+ 5));
			sb.append("isStatColumn='").append(StringUtil.left(ce._isStatColumn +"', ",3+ 5));
			sb.append("sqlDataType='") .append(StringUtil.left(ce._sqlDataType  +"', ",3+ 10));
			sb.append("dispLength='")  .append(StringUtil.left(ce._displayLength+"', ",3+ 2));
			sb.append("precision='")   .append(StringUtil.left(ce._precision    +"', ",3+ 2));
			sb.append("scale='")       .append(StringUtil.left(ce._scale        +"', ",3+ 2));
			sb.append("isNullable='")  .append(StringUtil.left(ce._isNullable   +"', ",3+ 5));
			sb.append("sqlColNum='")   .append(StringUtil.left(ce._sqlColNum    +"', ",3+ 2));
			sb.append("parseColNum='") .append(StringUtil.left(ce._parseColNum  +"', ",3+ 2));
			sb.append("isNumber='")    .append(StringUtil.left(ce._isNumber     +"', ",3+ 5));
			sb.append("isPct='")       .append(StringUtil.left(ce._isPct        +"', ",3+ 5));
			sb.append("status='")      .append(StringUtil.left(ce._status       +"', ",3+ 2));

			sb.append("schemaName='")  .append(StringUtil.left(ce._schemaName   +"', ",3+ 30));
			sb.append("tableName='")   .append(StringUtil.left(ce._tableName    +"', ",3+ 30));
			sb.append("catalogName='") .append(StringUtil.left(ce._catalogName  +"', ",3+ 30));

			sb.append("description='") .append(ce._description).append("'");
			sb.append(".\n");
		}
		return sb.toString();
	}

	

	//-------------------------------------------------------
	// BEGIN - implements: ResultSetMetaData
	//-------------------------------------------------------
	@Override
	public <T> T unwrap(Class<T> iface)
	{
		return null;
	}

	@Override
	public boolean isWrapperFor(Class<?> iface) //throws SQLException
	{
		return false;
	}

	@Override
	public int getColumnCount() //throws SQLException
	{
		return _numOfSqlCols;
	}

	@Override
	public boolean isAutoIncrement(int column) //throws SQLException
	{
		return false;
	}

	@Override
	public boolean isCaseSensitive(int column) //throws SQLException
	{
		return true;
	}

	@Override
	public boolean isSearchable(int column) //throws SQLException
	{
		return false;
	}

	@Override
	public boolean isCurrency(int column) //throws SQLException
	{
		return false;
	}

	@Override
	public int isNullable(int column) //throws SQLException
	{
		return getSqlColumn(column)._isNullable ?  ResultSetMetaData.columnNullable : ResultSetMetaData.columnNoNulls;
	}

	@Override
	public boolean isSigned(int column) //throws SQLException
	{
		// FIXME: ???
		return false;
	}

	@Override
	public int getColumnDisplaySize(int column) //throws SQLException
	{
		return getSqlColumn(column)._displayLength;
	}

	@Override
	public String getColumnLabel(int column) //throws SQLException
	{
		return getSqlColumn(column)._colName;
	}

	@Override
	public String getColumnName(int column) //throws SQLException
	{
		return getSqlColumn(column)._colName;
	}

	@Override
	public String getSchemaName(int column) //throws SQLException
	{
		return getSqlColumn(column)._schemaName;
	}

	@Override
	public int getPrecision(int column) //throws SQLException
	{
		return getSqlColumn(column)._precision;
	}

	@Override
	public int getScale(int column) //throws SQLException
	{
		return getSqlColumn(column)._scale;
	}

	@Override
	public String getTableName(int column) //throws SQLException
	{
		return getSqlColumn(column)._tableName;
	}

	@Override
	public String getCatalogName(int column) //throws SQLException
	{
		return getSqlColumn(column)._catalogName;
	}

	@Override
	public int getColumnType(int column) //throws SQLException
	{
		return getSqlColumn(column)._sqlType;
	}

	@Override
	public String getColumnTypeName(int column) //throws SQLException
	{
		return getSqlColumn(column)._sqlDataType;
	}

	@Override
	public boolean isReadOnly(int column) //throws SQLException
	{
		return false;
	}

	@Override
	public boolean isWritable(int column) //throws SQLException
	{
		return false;
	}

	@Override
	public boolean isDefinitelyWritable(int column) //throws SQLException
	{
		return false;
	}

    //--------------------------JDBC 2.0-----------------------------------

	@Override
	public String getColumnClassName(int column) //throws SQLException
	{
		Class<?> clazz = getSqlColumn(column)._javaClass;
		if (clazz != null)
			return clazz.getName();
		return null;
	}
	//-------------------------------------------------------
	// END - implements: ResultSetMetaData
	//-------------------------------------------------------

	
	// This is to support TableModel, which is called from the OsTable, which ise used by CounterModelHostMonitor, which extends CountersModel that implements TableModel 
	public Class<?> getColumnClass(int column) //throws SQLException
	{
		return getSqlColumn(column)._javaClass;
	}
	
	
	
	//-------------------------------------------------------
	// BEGIN - Parse Properties
	//-------------------------------------------------------
	/**
	 * <p>
	 * Example of how a Configuration could look like (this would be 'iostat' on Linux)
	 * <pre>
	 * -----------------------------------------------------------------------------
	 * hostmon.udc.ModuleName.[osname.]osCommand                  = iostat -xdzk 1
	 * hostmon.udc.ModuleName.[osname.]osCommand.isStreaming      = true
	 * hostmon.udc.ModuleName.[osname.]addStrColumn.device        = {length=30,             sqlColumnNumber=1,  parseColumnNumber=1,  isNullable=false, description=Disk device name}
	 * hostmon.udc.ModuleName.[osname.]addIntColumn.sample        = {                       sqlColumnNumber=2,  parseColumnNumber=0,  isNullable=true,  description=Number of 'sub' sample entries of iostat this value is based on}
	 * hostmon.udc.ModuleName.[osname.]addStatColumn.rrqmPerSec   = {precision=10, scale=1, sqlColumnNumber=3,  parseColumnNumber=2,  isNullable=true,  description=The number of read requests merged per second that were queued to the device}
	 * hostmon.udc.ModuleName.[osname.]addStatColumn.wrqmPerSec   = {precision=10, scale=1, sqlColumnNumber=4,  parseColumnNumber=3,  isNullable=true,  description=The number of write requests merged per second that were queued to the device.}
	 * hostmon.udc.ModuleName.[osname.]addStatColumn.readsPerSec  = {precision=10, scale=1, sqlColumnNumber=5,  parseColumnNumber=4,  isNullable=true,  description=The number of read requests that were issued to the device per second.}
	 * hostmon.udc.ModuleName.[osname.]addStatColumn.writesPerSec = {precision=10, scale=1, sqlColumnNumber=6,  parseColumnNumber=5,  isNullable=true,  description=The number of write requests that were issued to the device per second.}
	 * hostmon.udc.ModuleName.[osname.]addStatColumn.kbReadPerSec = {precision=10, scale=1, sqlColumnNumber=7,  parseColumnNumber=6,  isNullable=true,  description=The number of kilobytes read from the device per second.}
	 * hostmon.udc.ModuleName.[osname.]addStatColumn.kbWritePerSec= {precision=10, scale=1, sqlColumnNumber=8,  parseColumnNumber=7,  isNullable=true,  description=The number of kilobytes writ to the device per second.}
	 * hostmon.udc.ModuleName.[osname.]addStatColumn.avgrq-sz     = {precision=10, scale=1, sqlColumnNumber=9,  parseColumnNumber=8,  isNullable=true,  description=The average size (in  sectors) of the requests that were issued to the device.}
	 * hostmon.udc.ModuleName.[osname.]addStatColumn.avgqu-sz     = {precision=10, scale=1, sqlColumnNumber=10, parseColumnNumber=9,  isNullable=true,  description=The average queue length of the requests that were issued to the device.}
	 * hostmon.udc.ModuleName.[osname.]addStatColumn.await        = {precision=10, scale=1, sqlColumnNumber=11, parseColumnNumber=10, isNullable=true,  description=The average time (in milliseconds) for I/O requests issued to the device to be served. This includes the time spent by the requests in queue and the time spent servicing them.}
	 * hostmon.udc.ModuleName.[osname.]addStatColumn.svctm        = {precision=10, scale=1, sqlColumnNumber=12, parseColumnNumber=11, isNullable=true,  description=The average service time (in milliseconds) for I/O requests that were issued to the device.}
	 * hostmon.udc.ModuleName.[osname.]addStatColumn.utilPct      = {precision=10, scale=1, sqlColumnNumber=13, parseColumnNumber=12, isNullable=true,  description=Percentage of CPU time during which I/O requests were issued to the device (bandwidth utilization for the device). Device saturation occurs when this value is close to 100%.}
	 * hostmon.udc.ModuleName.[osname.]setPkColumns               = device
	 * hostmon.udc.ModuleName.[osname.]setPercentColumns          = utilPct
	 * hostmon.udc.ModuleName.[osname.]setSubSampleColumn         = samples
	 * hostmon.udc.ModuleName.[osname.]setParseRegexp             = theRegExp
	 * hostmon.udc.ModuleName.[osname.]skipRows.device            = Device:
	 * -----------------------------------------------------------------------------
	 * </pre>
	 * <ul>
	 * <li><code>addStrColumn.{colname}</code>  The column contains a string</li>
	 * <li><code>addIntColumn.{colname}</code>  The column contains a integer</li>
	 * <li><code>addStatColumn.{colname}</code> The column contains a number value, which should be apart of the summary/average calculation. The SQL data type will automatically by <code>numeric</code>, the Java type is <code>BigDecimal</code> </li>
	 * <li><code>setPkColumns</code>       Is a comma separated list of columns that should be considered as Primary Keys, the default is NO Primary Key</li>
	 * <li><code>setPercentColumns</code>  Is a comma separated list of columns that should be considered as Percent Columns (a GUI can then highlight those values), the default is NO Percent Columns</li>
	 * <li><code>setSubSampleColumn</code> Single column that is filled in when doing average/summary calculation with the number of records the sum/avg was based on</li>
	 * <li><code>setParseRegexp</code>     What regexp to use when parsing the input row to split it up into several sub strings</li>
	 * <li><code>skipRows</code>           If there are column headers (same split count as dataValues), then when this column has this value, the row will be "skipped" by the parser.</li>
	 * </ul>
	 * When you add a column (add*Column), you will also have to give some sub properties to, see the example above...
	 *
	 * @param conf
	 * @param moduleName
	 * @return
	 * @throws MandatoryPropertyException
	 */
	public static HostMonitorMetaData create(Configuration conf, String moduleName)
	throws MandatoryPropertyException
	{
		return create(conf, moduleName, null);
	}
	public static HostMonitorMetaData create(Configuration conf, String moduleName, String osname)
	throws MandatoryPropertyException
	{
		if (conf == null)
			throw new IllegalArgumentException("The passed Configuration can't be null");

		// fix empty osname
		if ( osname != null && osname.trim().equals("") )
			osname = null;

		// Create a new MetaData entry
		HostMonitorMetaData md = new HostMonitorMetaData();
		md.setTableName(moduleName);

		String prefix = "hostmon.udc." + moduleName + "." + (osname == null ? "-THIS-key-WILL-never-BE-found-" : osname);

		// Figure out if there is a OS Specific UserDefined HostMonitor
		int methodArrPos = 4;
		int optionArrPos = 5;
		List<String> keyList = conf.getKeys(prefix);
		if (keyList.isEmpty())
		{
			String osIndependentPrefix = "hostmon.udc." + moduleName;
			if (osname != null)
				_logger.info("No Operating System specific property entries for '"+prefix+"' can be found, revering to OS independent settings by using the prefix '"+osIndependentPrefix+"'.");

			methodArrPos--;
			optionArrPos--;
			prefix = osIndependentPrefix;
			keyList = conf.getKeys(prefix);
			if (keyList.isEmpty())
				throw new MandatoryPropertyException("No property entries for '"+prefix+"' can be found.");
		}

		// Loop all entries in the Configuration that matches
		for (String key : keyList)
		{
			String propValue = conf.getProperty(key);
			_logger.debug("READING: key="+StringUtil.left(key,60)+" val='"+propValue+"'.");

			// split the key into it's individual parts
			String[] keySplit = key.split("\\.");
			for (int i=0; i<keySplit.length; i++)
				keySplit[i] = keySplit[i].trim();

			// get some parts from the key, which is used below when deciding how to parse
			// hostmon.udc.ModuleName.<method>.<option>
			String method = null;
			String option = null;
			if (keySplit.length >= (methodArrPos+1)) method = keySplit[methodArrPos];
			if (keySplit.length >= (optionArrPos+1)) option = keySplit[optionArrPos];
			

			// Set initial values for some temporary variables used later
			String  columnName        = option;
			int     sqlColumnNumber   = -1;
			int     parseColumnNumber = -1;
			boolean isNullable        = true;
			String  description       = "";

			// If first char is a '{' then we have "sub" properties in the propValue
			// so lets parse that info and set to local variable.
			// NOTE: the "DUMMY" thing is a workaround to use PropPropEntry, which normally looks like:
			//       prop1={subProp1=val, subProp2=val}; prop2={subProp1=val, subProp2=val}; ...
			//       so the DUMMY is just to fool the PropPropEntry...
			PropPropEntry ppe = null;
			if (propValue.startsWith("{"))
			{
				ppe = new PropPropEntry("DUMMY="+propValue);

				columnName        = ppe.getProperty       ("DUMMY", "columnName",        option);
				sqlColumnNumber   = ppe.getIntProperty    ("DUMMY", "sqlColumnNumber",   -1);
				parseColumnNumber = ppe.getIntProperty    ("DUMMY", "parseColumnNumber", -1);
				isNullable        = ppe.getBooleanProperty("DUMMY", "isNullable",        true);
				description       = ppe.getProperty       ("DUMMY", "description",       "");
			}


			if (method.equals("osCommand") && option == null)
			{
				md.setOsCommand(propValue);
			}
			else if (method.equals("osCommand") && (option != null && option.equals("isStreaming")) )
			{
				boolean isStreaming = propValue.equalsIgnoreCase("true");
				md.setOsCommandStreaming(isStreaming);
			}
			else if (method.equals("addStrColumn"))
			{
				if (columnName.equals(""))   throw new MandatoryPropertyException("Checking property for '"+moduleName+"': Can't find parameter '"+ "columnName"        +"' for method '"+method+"' which is mandatory.");
				if (sqlColumnNumber   == -1) throw new MandatoryPropertyException("Checking property for '"+moduleName+"': Can't find parameter '"+ "sqlColumnNumber"   +"' for method '"+method+"' which is mandatory.");
				if (parseColumnNumber == -1) throw new MandatoryPropertyException("Checking property for '"+moduleName+"': Can't find parameter '"+ "parseColumnNumber" +"' for method '"+method+"' which is mandatory.");

				int length = ppe.getIntProperty("DUMMY", "length", -1);
				if (length < 0)
				{
					length = 30;
					_logger.warn("In '"+moduleName+"', the 'length' for String column "+columnName+" was not specified, setting this to '"+length+"'.");
				}
				md.addStrColumn(columnName, sqlColumnNumber, parseColumnNumber, isNullable, length, description);
			}
			else if (method.equals("addIntColumn"))
			{
				if (columnName.equals(""))   throw new MandatoryPropertyException("Checking property for '"+moduleName+"': Can't find parameter '"+ "columnName"        +"' for method '"+method+"' which is mandatory.");
				if (sqlColumnNumber   == -1) throw new MandatoryPropertyException("Checking property for '"+moduleName+"': Can't find parameter '"+ "sqlColumnNumber"   +"' for method '"+method+"' which is mandatory.");
				if (parseColumnNumber == -1) throw new MandatoryPropertyException("Checking property for '"+moduleName+"': Can't find parameter '"+ "parseColumnNumber" +"' for method '"+method+"' which is mandatory.");

				md.addIntColumn(columnName, sqlColumnNumber, parseColumnNumber, isNullable, description);
			}
			else if (method.equals("addDatetimeColumn"))
			{
				if (columnName.equals(""))   throw new MandatoryPropertyException("Checking property for '"+moduleName+"': Can't find parameter '"+ "columnName"        +"' for method '"+method+"' which is mandatory.");
				if (sqlColumnNumber   == -1) throw new MandatoryPropertyException("Checking property for '"+moduleName+"': Can't find parameter '"+ "sqlColumnNumber"   +"' for method '"+method+"' which is mandatory.");
				if (parseColumnNumber == -1) throw new MandatoryPropertyException("Checking property for '"+moduleName+"': Can't find parameter '"+ "parseColumnNumber" +"' for method '"+method+"' which is mandatory.");

				md.addIntColumn(columnName, sqlColumnNumber, parseColumnNumber, isNullable, description);
			}
			else if (method.equals("addStatColumn"))
			{
				if (columnName.equals(""))   throw new MandatoryPropertyException("Checking property for '"+moduleName+"': Can't find parameter '"+ "columnName"        +"' for method '"+method+"' which is mandatory.");
				if (sqlColumnNumber   == -1) throw new MandatoryPropertyException("Checking property for '"+moduleName+"': Can't find parameter '"+ "sqlColumnNumber"   +"' for method '"+method+"' which is mandatory.");
				if (parseColumnNumber == -1) throw new MandatoryPropertyException("Checking property for '"+moduleName+"': Can't find parameter '"+ "parseColumnNumber" +"' for method '"+method+"' which is mandatory.");

				int precision = ppe.getIntProperty("DUMMY", "precision", -1);
				int scale     = ppe.getIntProperty("DUMMY", "scale",     -1);
				if (precision < 0)
				{
					precision = 10;
					_logger.warn("In '"+moduleName+"', the 'precision' for Statistics column "+columnName+" was not specified, setting this to '"+precision+"'.");
				}
				if (scale < 0)
				{
					scale = 1;
					_logger.warn("In '"+moduleName+"', the 'scale' for column "+columnName+" was not specified, setting this to '"+scale+"'.");
				}
				md.addStatColumn(columnName, sqlColumnNumber, parseColumnNumber, isNullable, precision, scale, description);
			}
			else if (method.equals("setPkColumns"))
			{
				String[] cols = propValue.split(",");
				for (int i=0; i<cols.length; i++)
					cols[i] = cols[i].trim();

				md.setPkCol(cols);
			}
			else if (method.equals("setPercentColumns"))
			{
				String[] cols = propValue.split(",");
				for (int i=0; i<cols.length; i++)
					cols[i] = cols[i].trim();

				md.setPercentCol(cols);
			}
			else if (method.equals("setSubSampleColumn"))
			{
				md.setStatusCol(propValue, STATUS_COL_SUB_SAMPLE);
			}
			else if (method.equals("setParseRegexp"))
			{
				md.setParseRegexp(propValue);
			}
			else if (method.equals("skipRows"))
			{
				String colNameToSkipValueFor = option;
				String valueToSkip           = propValue;
				
				md.setSkipRows(colNameToSkipValueFor, valueToSkip);
			}
			else if (method.equals("allowRows"))
			{
				String colNameToAllowValueFor = option;
				String valueToAllow           = propValue;
				
				md.setAllowRows(colNameToAllowValueFor, valueToAllow);
			}
			// Some properties are handled by code that calls this initialization, so discard those...
			else if (method.equals("displayName")) { /* handled in by outer code */ }
			else if (method.equals("description")) { /* handled in by outer code */ }
			else if (method.equals("graph"))       { /* handled in by outer code */ }
			else
			{
				_logger.warn("Unknown method type '"+method+"' when creating MetaData for module '"+moduleName+"'.");
				//throw new Exception("Unknown method type '"+method+"' when creating MetaData for module '"+moduleName+"'.");
			}	
		}

		// CHECK that mandatory fields has been set
		if ( md.getOsCommand() == null) 
			throw new MandatoryPropertyException("The property 'hostmon.udc."+moduleName+".osCommand' is mandatory.");

		if (_logger.isDebugEnabled())
			_logger.debug("HostMonitorMetaData create() method returns: \n" + md.toTraceString());

		return md;
	}

	//-------------------------------------------------------
	// END - Parse Properties
	//-------------------------------------------------------

	/**
	 * Get skipRows and allowRows properties from the Configuration and set those in the MetaData 
	 * <p>
	 * Syntax:<br>
	 * <code>  hostmon.MonitorIo.{skipRows|allowRows}.<i>colName</i>[.x] = <i>javaRegexp</i>   </code>
	 * <p>
	 * Explanation:
	 * <table border=1 cellspacing=0>
	 * <tr> <td><code>hostmon.MonitorIo</code></td> <td>The prefix you specified.</td> </tr>
	 * <tr> <td><code>skipRows         </code></td> <td>Records that you want to discard, FILTER OUT</td> </tr>
	 * <tr> <td><code>allowRows        </code></td> <td>Records that you want to keep, FILTER IN</td> </tr>
	 * <tr> <td><code>colName          </code></td> <td>The name of the column you want to filter on</td> </tr>
	 * <tr> <td><code>x                </code></td> <td>Only used if you want to apply several regexp values to the same column, this is only used to make the property unique, it will not be used for anything, except the uniqueness</td> </tr>
	 * <tr> <td><code>javaRegexp       </code></td> <td>Any Java regular expression string that you want to filter on</td> </tr>
	 * </table>
	 * <p>
	 * Here is a configuration Example (disregard the # and everyting after):
	 * <pre>
	 * hostmon.MonitorIo.skipRows.device = sdb.+   # Skip values "sdb.+" in column "device"
	 * hostmon.MonitorIo.allowRows.device = sd.+   # Allow values "sd.+" in column "device"
	 * </pre>
	 * 
	 * @param prefix the Configuration prefix, if null: use "hostmon."+getTableName()+"."
	 * @param conf The config to use, if null: use Configuration.getCombinedConfiguration()
	 */
	public void setSkipAndAllowRows(String prefix, Configuration conf)
	{
		if (prefix == null)
			prefix = "hostmon."+getTableName()+".";

		if (conf == null)
			conf = Configuration.getCombinedConfiguration();

		for (String key : conf.getKeys(prefix))
		{
			String propValue = conf.getProperty(key);

			// split the key into it's individual parts
			String[] keySplit = key.split("\\.");
			for (int i=0; i<keySplit.length; i++)
				keySplit[i] = keySplit[i].trim();

			int methodArrPos = 2;
			int optionArrPos = 3;

			// get some parts from the key, which is used below when deciding how to parse
			// hostmon.udc.ModuleName.<method>.<option>
			String method = null;
			String option = null;
			if (keySplit.length >= (methodArrPos+1)) method = keySplit[methodArrPos];
			if (keySplit.length >= (optionArrPos+1)) option = keySplit[optionArrPos];


			if (method.equals("skipRows"))
			{
				String colNameToSkipValueFor = option;
				String valueToSkip           = propValue;

				setSkipRows(colNameToSkipValueFor, valueToSkip);
			}
			else if (method.equals("allowRows"))
			{
				String colNameToAllowValueFor = option;
				String valueToAllow           = propValue;

				setAllowRows(colNameToAllowValueFor, valueToAllow);
			}
		}
	}

	public boolean hasSourceRowAdjuster()
	{
		return _sourceRowAdjuster != null;
	}

	public void setSourceRowAdjuster(SourceRowAdjuster sourceRowAdjuster)
	{
		_sourceRowAdjuster = sourceRowAdjuster;
	}
	
	public SourceRowAdjuster getSourceRowAdjuster()
	{
		return _sourceRowAdjuster;
	}
	

	//---------------------------------------------------------------------------------
	//-- dynamic initialization, or init-on-first-row... like a CSV header
	//---------------------------------------------------------------------------------
	private boolean _initializeUsingFirstRow  = false;
	private boolean _firstRowInitDone         = false;
	
	public void setInitializeUsingFirstRow(boolean b) { _initializeUsingFirstRow = b; }
	public void setFirstRowInitDone(boolean b)        { _firstRowInitDone        = b; }

	public boolean isInitializeUsingFirstRowEnabled() { return _initializeUsingFirstRow; }
	public boolean isFirstRowInitDone()               { return _firstRowInitDone; }

	public void doInitializeUsingFirstRow(String row)
	{
		// Add any user defined columns in dynamic initialization
		addUserDefinedCountersInInitializeUsingFirstRow();

		// Mark it as "done"
		setFirstRowInitDone(true);
	}

	/** This method is called "at the end" of <code>doInitializeUsingFirstRow(String row)</code> so you can add any extra columns in dynamic initialization */
	public void addUserDefinedCountersInInitializeUsingFirstRow()
	{
	}


	
	//---------------------------------------------------------------------------------
	//-- UnPivoting enabled or not... meaning: One input row is producing several "table rows"
	//---------------------------------------------------------------------------------
	private boolean _isUnPivot = false;
	public void setUnPivot(boolean b) { _isUnPivot = b; }
	public boolean isUnPivot() { return _isUnPivot; }

	public String[][] parseRowOneToMany(HostMonitorMetaData md, String row, String[] preParsed, int type)
	{
		return null;
	}


	//-------------------------------------------------------------------------------
	//-------------------------------------------------------------------------------
	//-------------------------------------------------------------------------------
	//---- TEST CODE ---- TEST CODE ---- TEST CODE ---- TEST CODE ---- TEST CODE ----
	//-------------------------------------------------------------------------------
	//-------------------------------------------------------------------------------
	//-------------------------------------------------------------------------------
	
	public static void main(String[] args)
	{
		// Set Log4j Log Level
		Configurator.setRootLevel(Level.DEBUG);

		Configuration conf = new Configuration();
		conf.setProperty("hostmon.udc.TestXXX.osCommand",                  "iostat -xdzk 3");
		conf.setProperty("hostmon.udc.TestXXX.osCommand.isStreaming",      "true");

		conf.setProperty("hostmon.udc.TestXXX.addStrColumn.device",        "{length=30,             sqlColumnNumber=1,  parseColumnNumber=1,  isNullable=false, description=Disk device name}");
		conf.setProperty("hostmon.udc.TestXXX.addIntColumn.numOfSamples",  "{                       sqlColumnNumber=2,  parseColumnNumber=0,  isNullable=true,  description=Number of 'sub' sample entries of iostat this value is based on}");
		conf.setProperty("hostmon.udc.TestXXX.addStatColumn.rrqmPerSec",   "{precision=10, scale=1, sqlColumnNumber=3,  parseColumnNumber=2,  isNullable=true,  description=The number of read requests merged per second that were queued to the device}");
		conf.setProperty("hostmon.udc.TestXXX.addStatColumn.wrqmPerSec",   "{precision=10, scale=1, sqlColumnNumber=4,  parseColumnNumber=3,  isNullable=true,  description=The number of write requests merged per second that were queued to the device.}");
		conf.setProperty("hostmon.udc.TestXXX.addStatColumn.readsPerSec",  "{precision=10, scale=1, sqlColumnNumber=5,  parseColumnNumber=4,  isNullable=true,  description=The number of read requests that were issued to the device per second.}");
		conf.setProperty("hostmon.udc.TestXXX.addStatColumn.writesPerSec", "{precision=10, scale=1, sqlColumnNumber=6,  parseColumnNumber=5,  isNullable=true,  description=The number of write requests that were issued to the device per second.}");
		conf.setProperty("hostmon.udc.TestXXX.addStatColumn.kbReadPerSec", "{precision=10, scale=1, sqlColumnNumber=7,  parseColumnNumber=6,  isNullable=true,  description=The number of kilobytes read from the device per second.}");
		conf.setProperty("hostmon.udc.TestXXX.addStatColumn.kbWritePerSec","{precision=10, scale=1, sqlColumnNumber=8,  parseColumnNumber=7,  isNullable=true,  description=The number of kilobytes writ to the device per second.}");
		conf.setProperty("hostmon.udc.TestXXX.addStatColumn.avgrq-sz",     "{precision=10, scale=1, sqlColumnNumber=9,  parseColumnNumber=8,  isNullable=true,  description=The average size (in  sectors) of the requests that were issued to the device.}");
		conf.setProperty("hostmon.udc.TestXXX.addStatColumn.avgqu-sz",     "{precision=10, scale=1, sqlColumnNumber=10, parseColumnNumber=9,  isNullable=true,  description=The average queue length of the requests that were issued to the device.}");
		conf.setProperty("hostmon.udc.TestXXX.addStatColumn.await",        "{precision=10, scale=1, sqlColumnNumber=11, parseColumnNumber=10, isNullable=true,  description=The average time (in milliseconds) for I/O requests issued to the device to be served. This includes the time spent by the requests in queue and the time spent servicing them.}");
		conf.setProperty("hostmon.udc.TestXXX.addStatColumn.svctm",        "{precision=10, scale=1, sqlColumnNumber=12, parseColumnNumber=11, isNullable=true,  description=The average service time (in milliseconds) for I/O requests that were issued to the device.}");
		conf.setProperty("hostmon.udc.TestXXX.addStatColumn.utilPct",      "{precision=10, scale=1, sqlColumnNumber=13, parseColumnNumber=12, isNullable=true,  description=Percentage of CPU time during which I/O requests were issued to the device (bandwidth utilization for the device). Device saturation occurs when this value is close to 100%.}");

		conf.setProperty("hostmon.udc.TestXXX.setPkColumns",               "device");
		conf.setProperty("hostmon.udc.TestXXX.setPercentColumns",          "utilPct, await,svctm");
		conf.setProperty("hostmon.udc.TestXXX.setSubSampleColumn",         "numOfSamples");
		conf.setProperty("hostmon.udc.TestXXX.setParseRegexp",             "theRegExp");
		conf.setProperty("hostmon.udc.TestXXX.skipRows.device",            "Device:");
		
		try
		{
			HostMonitorMetaData md = HostMonitorMetaData.create(conf, "TestXXX");
			System.out.println("====================================================");
			System.out.println("MD=\n"+md.toTraceString());
			System.out.println("----------------------------------------------------");
		}
		catch (MandatoryPropertyException e)
		{
			e.printStackTrace();
		}
	}

}
