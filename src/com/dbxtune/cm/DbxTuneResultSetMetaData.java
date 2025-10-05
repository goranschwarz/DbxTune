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
package com.dbxtune.cm;

import java.lang.invoke.MethodHandles;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.sql.ResultSetMetaData;
import java.sql.Time;
import java.sql.Timestamp;
import java.sql.Types;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.dbxtune.utils.StringUtil;


public class DbxTuneResultSetMetaData
implements ResultSetMetaData
{
	private static final Logger _logger = LogManager.getLogger(MethodHandles.lookup().lookupClass());

	private String  _schemaName    = "";
	private String  _tableName     = "";
	private String  _catalogName   = "";

	/** Class that holds a entry */
	public static class ColumnEntry implements Comparable<ColumnEntry>
	{
		int     _sqlColNum     = -1;
//		int     _parseColNum   = -1;
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

	/** A list of columns in the MetaData */
	private ArrayList<ColumnEntry> _columns = new ArrayList<ColumnEntry>();
	
	/** A shortcut to what column names that are available, maintained by addColumn() */
	private ArrayList<String> _columnNames = new ArrayList<String>();

	/** number of SQL columns, which can(will be delivered to caller). */
//	private int _numOfSqlCols   = 0;

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
	 * Check if the column exists
	 * 
	 * @param name
	 * @return
	 */
	public boolean hasColumn(String name)
	{
		for (ColumnEntry e: _columns)
		{
			if (e._colName.equals(name))
				return true;
		}
		return false;
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
	private boolean addColumn(ColumnEntry entry)
	{
		checkColumnEntry(entry);
		
		if (entry._schemaName  == null) entry._schemaName  = getSchemaName();
		if (entry._tableName   == null) entry._tableName   = getTableName();
		if (entry._catalogName == null) entry._catalogName = getCatalogName();

		if (entry._sqlColNum < 0)
			entry._sqlColNum = _columns.size() + 1;

		_columns.add(entry);
		
//		// Fix the internal counters.
//		_numOfSqlCols   = 0;
//		for (ColumnEntry e: _columns)
//		{
//			_numOfSqlCols   = Math.max(e._sqlColNum,   _numOfSqlCols);
//		}
		
		// If the columns are added in the "wrong" order, sort them here by SQL COlumn
		Collections.sort(_columns);

		// Create the List of column names
		_columnNames = new ArrayList<String>();
		for (ColumnEntry e: _columns)
			_columnNames.add(e._colName);

		return true;
	}

	/**
	 * Add a column of type String
	 * @param colName           Name of the column
	 * @param sqlColumnNumber   SQL Column number. 0 or < 0 = not part of the SQL results NOTE: 1=col1, 2=col2...
	 * @param isNullable        Is can this be a NULL result
	 * @param length            The MAX length of the string
	 * @param description       A description of the column that can be used a s a graphical tooltip
	 */
	public boolean addStrColumn(String colName, int sqlColumnNumber, boolean isNullable, int length, String description)
	{
		if (sqlColumnNumber < 0 && hasColumn(colName))
			return false;

		ColumnEntry entry = new ColumnEntry();

		entry._colName       = colName;

		entry._sqlColNum     = sqlColumnNumber;

		entry._isPartOfPk    = false;
		entry._isNullable    = isNullable;

		entry._sqlType       = Types.VARCHAR;
		entry._sqlDataType   = "varchar";
		entry._javaClass     = String.class;
		entry._isNumber      = false;

		entry._displayLength = Math.max(colName.length(), length);
		entry._precision     = -1;
		entry._scale         = -1;

		entry._description   = description;
		
		return addColumn(entry);
	}

	/**
	 * Add a column of type Integer
	 * @param colName           Name of the column
	 * @param sqlColumnNumber   SQL Column number. 0 or < 0 = not part of the SQL results NOTE: 1=col1, 2=col2...
	 * @param isNullable        Is can this be a NULL result
	 * @param description       A description of the column that can be used a s a graphical tooltip
	 */
	public boolean addIntColumn(String colName, int sqlColumnNumber, boolean isNullable, String description)
	{
		if (sqlColumnNumber < 0 && hasColumn(colName))
			return false;

		ColumnEntry entry = new ColumnEntry();

		entry._colName       = colName;

		entry._sqlColNum     = sqlColumnNumber;

		entry._isPartOfPk    = false;
		entry._isNullable    = isNullable;

		entry._sqlType       = Types.INTEGER;
		entry._sqlDataType   = "int";
		entry._javaClass     = Integer.class;
		entry._isNumber      = true;

		entry._displayLength = Math.max(colName.length(), Integer.toString(Integer.MAX_VALUE).length());
		entry._precision     = -1;
		entry._scale         = -1;

		entry._description   = description;
		
		return addColumn(entry);
	}

	/**
	 * Add a column of type Long
	 * @param colName           Name of the column
	 * @param sqlColumnNumber   SQL Column number. 0 or < 0 = not part of the SQL results NOTE: 1=col1, 2=col2...
	 * @param isNullable        Is can this be a NULL result
	 * @param description       A description of the column that can be used a s a graphical tooltip
	 */
	public boolean addLongColumn(String colName, int sqlColumnNumber, boolean isNullable, String description)
	{
		if (sqlColumnNumber < 0 && hasColumn(colName))
			return false;

		ColumnEntry entry = new ColumnEntry();

		entry._colName       = colName;

		entry._sqlColNum     = sqlColumnNumber;

		entry._isPartOfPk    = false;
		entry._isNullable    = isNullable;

		entry._sqlType       = Types.BIGINT;
		entry._sqlDataType   = "bigint";
		entry._javaClass     = Long.class;
		entry._isNumber      = true;

		entry._displayLength = Math.max(colName.length(), Long.toString(Long.MAX_VALUE).length());
		entry._precision     = -1;
		entry._scale         = -1;

		entry._description   = description;
		
		return addColumn(entry);
	}

	/**
	 * Add a column of type BigDecimal
	 * @param colName           Name of the column
	 * @param sqlColumnNumber   SQL Column number. 0 or < 0 = not part of the SQL results NOTE: 1=col1, 2=col2...
	 * @param isNullable        Is can this be a NULL result
	 * @param description       A description of the column that can be used a s a graphical tooltip
	 */
	public boolean addBigDecimalColumn(String colName, int sqlColumnNumber, boolean isNullable, int precision, int scale, String description)
	{
		if (sqlColumnNumber < 0 && hasColumn(colName))
			return false;

		ColumnEntry entry = new ColumnEntry();

		entry._colName       = colName;

		entry._sqlColNum     = sqlColumnNumber;

		entry._isPartOfPk    = false;
		entry._isNullable    = isNullable;

		entry._sqlType       = Types.DECIMAL;
		entry._precision     = precision;
		entry._scale         = scale;
//		entry._sqlDataType   = "numeric("+precision+","+scale+")";
		entry._sqlDataType   = "numeric";
		entry._javaClass     = BigDecimal.class;
		entry._isNumber      = true;

		entry._displayLength = Math.max(colName.length(), precision + 1 + scale);  // 10.1
		entry._precision     = -1;
		entry._scale         = -1;

		entry._description   = description;
		
		return addColumn(entry);
	}

	/**
	 * Add a column of type Datetime...
	 * @param colName           Name of the column
	 * @param sqlColumnNumber   SQL Column number. 0 or < 0 = not part of the SQL results NOTE: 1=col1, 2=col2...
	 * @param isNullable        Is can this be a NULL result
	 * @param description       A description of the column that can be used a s a graphical tooltip
	 */
	public boolean addDatetimeColumn(String colName, int sqlColumnNumber, boolean isNullable, String description)
	{
		if (sqlColumnNumber < 0 && hasColumn(colName))
			return false;

		ColumnEntry entry = new ColumnEntry();

		entry._colName       = colName;

		entry._sqlColNum     = sqlColumnNumber;

		entry._isPartOfPk    = false;
		entry._isNullable    = isNullable;

		entry._sqlType       = Types.TIMESTAMP;
		entry._sqlDataType   = "datetime";
		entry._javaClass     = Timestamp.class;
		entry._isNumber      = false;

		entry._displayLength = Math.max(colName.length(), "2011-01-01 22:22:22.333".length());
		entry._precision     = -1;
		entry._scale         = -1;

		entry._description   = description;
		
		return addColumn(entry);
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
	public void setPkCol(List<String> colNames)
	{
		// First reset all...
		for (ColumnEntry entry: _columns)
		{
			entry._isPartOfPk = false;
		}
		// Then set all the columns
		for (String colname : colNames)
		{
//			if (hasColumn(colname))
//			{
    			ColumnEntry entry = getColumn(colname);
    			entry._isPartOfPk = true;
    			entry._isNullable = false;
//			}
		}
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
	 * Get the SQL Position of the column, Note this always start at 1
	 * @param colName Name of the column
	 * @return The description
	 */
	public int getColumnSqlPos(String colname)
	{
		ColumnEntry entry = getColumn(colname);
		return entry._sqlColNum;
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
			case java.sql.Types.DOUBLE:       return Double .valueOf(strVal);
			case java.sql.Types.NUMERIC:      return new BigDecimal(strVal).setScale(scale, RoundingMode.HALF_EVEN);
			case java.sql.Types.DECIMAL:      return new BigDecimal(strVal).setScale(scale, RoundingMode.HALF_EVEN);
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
			_logger.warn("When creating a new Object for column '"+ce._colName+"' with value '"+val+"', a NumberFormatException was thrown, which implies that the value is not a number. Creating the object with a zero value instead.");
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
		case java.sql.Types.NUMERIC:      return new BigDecimal(0).setScale(scale, RoundingMode.HALF_EVEN);
		case java.sql.Types.DECIMAL:      return new BigDecimal(0).setScale(scale, RoundingMode.HALF_EVEN);
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
	 * Get a List of Column names
	 * @return
	 */
	public List<String> getColumnNames()
	{
		List<String> list = new ArrayList<String>(getColumnCount());

		for (ColumnEntry entry: _columns)
			list.add(entry._colName);

		return list;
	}
	
	public List<Integer> getSqlTypes()
	{
		List<Integer> list = new ArrayList<Integer>(getColumnCount());

		for (ColumnEntry entry: _columns)
			list.add(entry._sqlType);

		return list;
	}

	public List<String> getSqlTypeNames()
	{
		List<String> list = new ArrayList<String>(getColumnCount());

		for (ColumnEntry entry: _columns)
			list.add(entry._sqlDataType);

		return list;
	}

	public List<String> getClassNames()
	{
		List<String> list = new ArrayList<String>(getColumnCount());

		for (ColumnEntry entry: _columns)
			list.add(entry._javaClass.getName());

		return list;
	}

	public boolean[] getPkColArray()
	{
		boolean[] colIsPk = new boolean[getColumnCount()];

		for (int i=0; i<colIsPk.length; i++)
			colIsPk[i] = getSqlColumn(i+1)._isPartOfPk;

		return colIsPk;
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
		sb.append("SQL Column Count         = '").append(getColumnCount())      .append("'\n");
		int e=0;
		for (DbxTuneResultSetMetaData.ColumnEntry ce : getColumns())
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
//		return _numOfSqlCols;
		return _columns.size();
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
}
