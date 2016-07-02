/**
 * @author <a href="mailto:goran_schwarz@hotmail.com">Goran Schwarz</a>
 */
package com.asetune.gui;

import java.math.BigDecimal;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.sql.SQLWarning;
import java.sql.Timestamp;
import java.sql.Types;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.regex.Pattern;

import javax.swing.table.AbstractTableModel;
import javax.swing.table.TableModel;

import org.apache.log4j.Logger;

import com.asetune.sql.SqlProgressDialog;
import com.asetune.sql.pipe.PipeCommand;
import com.asetune.sql.pipe.PipeCommandGrep;
import com.asetune.utils.Configuration;
import com.asetune.utils.StringUtil;


/**
 * This class takes a JDBC ResultSet object and implements the TableModel
 * interface in terms of it so that a Swing JTable component can display the
 * contents of the ResultSet.  
 * 
 * @author Goran Schwarz
 */
public class ResultSetTableModel
    extends AbstractTableModel
{
	private static final long serialVersionUID = 1L;
	private static Logger _logger = Logger.getLogger(ResultSetTableModel.class);
	
	public static final String  PROPKEY_BINERY_PREFIX = "ResultSetTableModel.binary.prefix";
	public static final String  DEFAULT_BINERY_PREFIX = "0x";

	public static final String  PROPKEY_BINARY_TOUPPER = "ResultSetTableModel.binary.toUpper";
	public static final boolean DEFAULT_BINARY_TOUPPER = false;

	public static final String  PROPKEY_PrintResultSetInfoLong = "ResultSetTableModel.print.rs.info.long";
	public static final boolean DEFAULT_PrintResultSetInfoLong = false;

	public static final String  PROPKEY_NULL_REPLACE = "ResultSetTableModel.replace.null.with";
	public static final String  DEFAULT_NULL_REPLACE = "(NULL)";

	public static final String  PROPKEY_StringTrim = "ResultSetTableModel.string.trim";
	public static final boolean DEFAULT_StringTrim = true;

	public static final String  PROPKEY_ShowRowNumber = "ResultSetTableModel.show.rowNumber";
	public static final boolean DEFAULT_ShowRowNumber = false;

	private static final String  BINARY_PREFIX  = Configuration.getCombinedConfiguration().getProperty(       PROPKEY_BINERY_PREFIX,  DEFAULT_BINERY_PREFIX);
	private static final boolean BINARY_TOUPPER = Configuration.getCombinedConfiguration().getBooleanProperty(PROPKEY_BINARY_TOUPPER, DEFAULT_BINARY_TOUPPER);
	private static final String  NULL_REPLACE   = Configuration.getCombinedConfiguration().getProperty(       PROPKEY_NULL_REPLACE,   DEFAULT_NULL_REPLACE);
	public  static final String  ROW_NUMBER_COLNAME = "row#";
	
	private int	_numcols;
	
	private ArrayList<String>            _rsmdRefTableName      = new ArrayList<String>();  // rsmd.getXXX(c); 
	private ArrayList<String>            _rsmdColumnName        = new ArrayList<String>();  // rsmd.getColumnName(c); 
	private ArrayList<String>            _rsmdColumnLabel       = new ArrayList<String>();  // rsmd.getColumnLabel(c); 
	private ArrayList<Integer>           _rsmdColumnType        = new ArrayList<Integer>(); // rsmd.getColumnType(c); 
	private ArrayList<String>            _rsmdColumnTypeStr     = new ArrayList<String>();  // getColumnJavaSqlTypeName(_rsmdColumnType.get(index)) 
	private ArrayList<String>            _rsmdColumnTypeName    = new ArrayList<String>();  // rsmd.getColumnTypeName(c);
	private ArrayList<String>            _rsmdColumnTypeNameStr = new ArrayList<String>();  // kind of 'SQL' datatype: this.getColumnTypeName(rsmd, c);
	private ArrayList<String>            _rsmdColumnClassName   = new ArrayList<String>();  // rsmd.getColumnClassName(c);
	private ArrayList<Integer>           _displaySize           = new ArrayList<Integer>(); // Math.max(rsmd.getColumnDisplaySize(c), rsmd.getColumnLabel(c).length());
	private Class[]          _classType             = null; // first found class of value, which wasn't null
	private ArrayList<ArrayList<Object>> _rows                  = new ArrayList<ArrayList<Object>>();
	private int                          _readCount             = 0;
	private SQLWarning                   _sqlWarning            = null;
	private boolean                      _allowEdit             = true; 
	private String                       _name                  = null;
	private PipeCommand                  _pipeCmd               = null;
	private boolean                      _cancelled             = false;
	private int                          _abortedAfterXRows     = -1;

	private boolean                      _stringTrim            = Configuration.getCombinedConfiguration().getBooleanProperty(PROPKEY_StringTrim,     DEFAULT_StringTrim);
	private boolean                      _showRowNumber         = Configuration.getCombinedConfiguration().getBooleanProperty(PROPKEY_ShowRowNumber,  DEFAULT_ShowRowNumber);

	private int                          _readResultSetTime     = -1;
	
	/** Set the name of this table model, could be used for debugging or other tracking purposes */
	public void setName(String name) { _name = name; }

	/** Get the name of this table model, could be used for debugging or other tracking purposes */
	public String getName() { return _name; }

	public int getResultSetReadTime() { return _readResultSetTime; }
	
	/**
	 * INTERNAL: used by: <code>public static String getResultSetInfo(ResultSetTableModel rstm)</code>
	 * @param rsmd
	 * @throws SQLException
	 */
	private ResultSetTableModel(ResultSetMetaData rsmd) 
	throws SQLException
	{
		this(null, rsmd, false, "getResultSetInfo", -1, false, null, null);
	}

	/**
	 * This constructor creates a TableModel from a ResultSet.  
	 **/
	public ResultSetTableModel(ResultSet rs, String name) 
	throws SQLException
	{
		this(rs, true, name);
	}
	public ResultSetTableModel(ResultSet rs, boolean editable, String name) 
	throws SQLException
	{
		this(rs, editable, name, -1, false, null, null);
	}
	public ResultSetTableModel(ResultSet rs, boolean editable, String name, int stopAfterXrows, boolean noData, PipeCommand pipeCommand, SqlProgressDialog progress) 
	throws SQLException
	{
		this(rs, rs.getMetaData(), editable, name, stopAfterXrows, noData, pipeCommand, progress);
	}
	public ResultSetTableModel(ResultSet rs, ResultSetMetaData rsmd, boolean editable, String name, int stopAfterXrows, boolean noData, PipeCommand pipeCommand, SqlProgressDialog progress) 
	throws SQLException
	{
		long startTime = System.currentTimeMillis();

		_allowEdit     = editable;
		setName(name);
		_pipeCmd       = pipeCommand;
//		_showRowNumber = showRowNumber;

		if (getName() != null)
			setName(getName().replace('\n', ' ')); // remove newlines in name

		int maxDisplaySize = 32768;
		try { maxDisplaySize = Integer.parseInt( System.getProperty("ResultSetTableModel.maxDisplaySize", Integer.toString(maxDisplaySize)) ); }
		catch (NumberFormatException ignore) {};

//		ResultSetMetaData rsmd = rs.getMetaData();
		_numcols = rsmd.getColumnCount() + 1;
		_classType = new Class[_numcols + (_showRowNumber ? 1 : 0)];

		if (_showRowNumber)
		{
			_rsmdColumnLabel      .add(ROW_NUMBER_COLNAME);
			_rsmdColumnName       .add(ROW_NUMBER_COLNAME);
			_rsmdColumnType       .add(new Integer(java.sql.Types.INTEGER));
			_rsmdColumnTypeStr    .add("--sqlw-generated-rowid--");
			_rsmdColumnClassName  .add("java.lang.Integer");
			_rsmdColumnTypeName   .add("int");
			_rsmdColumnTypeNameStr.add("int");
			_displaySize          .add(new Integer(10));
			_classType[0]         = Integer.class;
		}

		for (int c=1; c<_numcols; c++)
		{
//			String refCatName        = rsmd.getCatalogName(c);
//			String refSchName        = rsmd.getSchemaName(c);
//			String refTabName        = rsmd.getTableName(c);
			String refCatName = "";
			String refSchName = "";
			String refTabName = "";
			try { refCatName        = rsmd.getCatalogName(c); } catch(SQLException ignore) {}
			try { refSchName        = rsmd.getSchemaName(c);  } catch(SQLException ignore) {}
			try { refTabName        = rsmd.getTableName(c);   } catch(SQLException ignore) {}
			refCatName = StringUtil.isNullOrBlank(refCatName) ? "" : refCatName + ".";
			refSchName = StringUtil.isNullOrBlank(refSchName) ? "" : refSchName + ".";
			refTabName = StringUtil.isNullOrBlank(refTabName) ? "-none-" : refTabName;
			String fullRefTableName  = refCatName + refSchName + refTabName;
			
			String columnLabel       = rsmd.getColumnLabel(c);
			String columnName        = rsmd.getColumnName(c);
			String columnClassName   = rsmd.getColumnClassName(c);
			String columnTypeNameGen = getColumnTypeName(rsmd, c);
			
			String columnTypeNameRaw = "-unknown-";
			try {  columnTypeNameRaw = rsmd.getColumnTypeName(c); } catch(SQLException ignore) {}; // sometimes this caused SQLException, especially for 'compute by' 
			int    columnType        = rsmd.getColumnType(c);
			int    columnDisplaySize = Math.max(rsmd.getColumnDisplaySize(c), rsmd.getColumnLabel(c).length());

			if (columnDisplaySize > maxDisplaySize)
			{
				// ok me guessing it's a blob
				// if we only have ONE column, lets hardcode the length to a small value
				if (_numcols == 2 && (columnType == Types.LONGVARCHAR || columnType == Types.CLOB || columnType == Types.LONGVARBINARY || columnType == Types.BLOB))
//				if (false)
				{
					columnDisplaySize = 80;
				}
				else
				{
					_logger.info("For column '"+columnLabel+"', columnDisplaySize is '"+columnDisplaySize+"', which is above max value of '"+maxDisplaySize+"', using max value. The max value can be changed with java parameter '-DResultSetTableModel.maxDisplaySize=sizeInBytes'. ResultSetTableModel.name='"+getName()+"'");
					columnDisplaySize = maxDisplaySize;
				}
			}

			_rsmdRefTableName     .add(fullRefTableName);
			_rsmdColumnLabel      .add(columnLabel);
			_rsmdColumnName       .add(columnName);
			_rsmdColumnType       .add(new Integer(columnType));
			_rsmdColumnTypeStr    .add(getColumnJavaSqlTypeName(columnType));
			_rsmdColumnClassName  .add(columnClassName);
			_rsmdColumnTypeName   .add(columnTypeNameRaw);
			_rsmdColumnTypeNameStr.add(columnTypeNameGen);
			_displaySize          .add(new Integer(columnDisplaySize));
			
//			System.out.println("name='"+_cols.get(c-1)+"', getColumnClassName("+c+")='"+_type.get(c-1)+"', getColumnTypeName("+c+")='"+_sqlType.get(c-1)+"'.");
		}
		
		// If there is no ResultSet, then this object is just used to get a ResultSet Information String
		// see: public static String getResultSetInfo(ResultSetTableModel rstm)
		if (rs == null)
			return;

		String originProgressState = null;
		if (progress != null)
			originProgressState = progress.getState();

		_readCount = 0;
		int rowCount = 0;
		while(rs.next())
		{
			if ( progress != null && progress.isCancelled() )
			{
				_cancelled = true;
				break;
			}
				
			if ( stopAfterXrows > 0 )
			{
				if (_readCount >= stopAfterXrows)
				{
					_abortedAfterXRows = _readCount;
					break;
				}
			}
				
			_readCount++;
			if (progress != null)
			{
				if ( (_readCount % 100) == 0 )
					progress.setState(originProgressState + " row "+_readCount);
			}

			// read any eventual SQLWarnings that is part of the row
			_sqlWarning = rs.getWarnings();
//			for (SQLWarning sqlw = rs.getWarnings(); sqlw != null; sqlw = sqlw.getNextWarning())
//				_sqlWarnList.add(sqlw);
			rs.clearWarnings();

			// If we do not read data, just go to the top and position for next potential read
			if ( noData )
				continue;

			// Read all columns for a row and add it to the structure
			ArrayList<Object> row = new ArrayList<Object>();
			if (_showRowNumber)
				row.add(new Integer(_readCount));

			for (int c=1; c<_numcols; c++)
			{
//				Object o = rs.getObject(c);
				int type = _rsmdColumnType.get( _showRowNumber ? c : c-1);  // if _showRowNumber entry 0 is "row#" entry
				Object o = getDataValue(rs, c, type);

				// Set class to be returned with the method: getColumnClass()
				if (o != null)
				{
					int c2 = c + (_showRowNumber ? 1 : 0);
					if (_classType[c2] == null)
						_classType[c2] = o.getClass();
				}

//				switch(type)
//				{
//				case Types.CLOB:
//					o = rs.getString(c);
//					break;
//
//				case Types.BINARY:
//				case Types.VARBINARY:
//				case Types.LONGVARBINARY:
//					o = StringUtil.bytesToHex(BINARY_PREFIX, rs.getBytes(c), BINARY_TOUPPER);
//					break;
//
//				case Types.DATE:
//					o = rs.getDate(c);
//					break;
//
//				case Types.TIME:
//					o = rs.getTime(c);
//					break;
//
//				case Types.TIMESTAMP:
//					o = rs.getTimestamp(c);
//					break;
//
//				default:
//						o = rs.getObject(c);
//						break;
//				}

                // Do we want to remove leading/trailing blanks
				if (o instanceof String && _stringTrim)
					o = ((String)o).trim();

				// Add the column data to the current row array list
				row.add(o);
				
				// NOTE: What about oracles Types.XXXX that is not supported, should we trust that they can do toString(), which I know some can't do.
				//       WHAT TO DO with those things ?????
				//       can we inspect to object, to check... and use some other method. 

//				if (o!=null)
//					System.out.println("ResultSetTableModel: Row="+rowCount+", Col="+c+", Class="+o.getClass()+", Comparable="+((o instanceof Comparable)?"true":"false"));
//				else
//					System.out.println("ResultSetTableModel: Row="+rowCount+", Col="+c+", ---NULL--");
			}
			// apply pipe filter
			if (addRow(_rsmdColumnLabel, row))
			{
				_rows.add(row);
				rowCount++;
			}			
		}

		// add 2 chars for BINARY types
		for (int c=0; c<(_numcols-1); c++)
		{
			int type = _rsmdColumnType.get( _showRowNumber ? c+1 : c );

			switch(type)
			{
			case Types.BINARY:
			case Types.VARBINARY:
			case Types.LONGVARBINARY:
				int size = _displaySize.get( _showRowNumber ? c+1 : c );
				_displaySize.set( (_showRowNumber ? c+1 : c), new Integer(size + BINARY_PREFIX.length()));
				break;
			}

		}
//		rs.getStatement().close();
//		rs.close();

		if (progress != null)
			progress.setState(originProgressState + " Read done, rows "+_readCount + (_abortedAfterXRows<0 ? "" : ", then stopped, due to 'top' restriction.") );
		
		_readResultSetTime = (int) (System.currentTimeMillis() - startTime);
	}

	private Object getDataValue(ResultSet rs, int col, int jdbcSqlType) 
	throws SQLException
	{
		// Return the "object" via getXXX method for "known" datatypes
		switch (jdbcSqlType)
		{
		case java.sql.Types.BIT:           return rs.getBoolean(col);
		case java.sql.Types.TINYINT:       return rs.getObject(col);   // use OBJECT
		case java.sql.Types.SMALLINT:      return rs.getObject(col);   // use OBJECT
		case java.sql.Types.INTEGER:       return rs.getObject(col);   // use OBJECT
		case java.sql.Types.BIGINT:        return rs.getObject(col);   // use OBJECT
		case java.sql.Types.FLOAT:         return rs.getObject(col);   // use OBJECT
		case java.sql.Types.REAL:          return rs.getObject(col);   // use OBJECT
		case java.sql.Types.DOUBLE:        return rs.getObject(col);   // use OBJECT
		case java.sql.Types.NUMERIC:       return rs.getObject(col);   // use OBJECT
		case java.sql.Types.DECIMAL:       return rs.getObject(col);   // use OBJECT
		case java.sql.Types.CHAR:          return rs.getString(col);
		case java.sql.Types.VARCHAR:       return rs.getString(col);
		case java.sql.Types.LONGVARCHAR:   return rs.getString(col);
		case java.sql.Types.DATE:          return rs.getDate(col);
		case java.sql.Types.TIME:          return rs.getTime(col);
		case java.sql.Types.TIMESTAMP:     return rs.getTimestamp(col);
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

	/** 
	 * How many rows did we read from the network<br>
	 * NOTE: the rows might have been discarded in some way, so it's basically number of calls to rs.next()
	 */
	public int getReadCount()
	{
		return _readCount;
	}

	public boolean isCancelled()
	{
		return _cancelled;
	}

	public boolean wasAbortedAfterXRows()
	{
		return _abortedAfterXRows >= 0;
	}
	public int getAbortedAfterXRows()
	{
		return _abortedAfterXRows;
	}
    
	/**
	 * Set new data content for the table model.
	 * 
	 * @param rstm the new Data rows
	 * @param merge If you want to "merge" current data with the one supplied
	 * 
	 * @throws ModelMissmatchException if the data model doesn't has the same structure (number of columns, column names, etc) 
	 */
	public void setModelData(ResultSetTableModel rstm, boolean merge)
	throws ModelMissmatchException
	{
		// Check column count
		if (getColumnCount() != rstm.getColumnCount())
			throw new ModelMissmatchException("Column COUNT missmatch. current count="+getColumnCount()+", passed count="+rstm.getColumnCount());
		
		// Check column name differences
		for (int i=0; i<getColumnCount(); i++)
		{
			if ( ! _rsmdColumnLabel.get(i).equals(rstm._rsmdColumnLabel.get(i)) )
				throw new ModelMissmatchException("Column NAME missmatch. current columns="+_rsmdColumnLabel+", passed columns="+rstm._rsmdColumnLabel);
		}

		// Check column data type differences
		for (int i=0; i<getColumnCount(); i++)
		{
			if ( ! _rsmdColumnTypeStr.get(i).equals(rstm._rsmdColumnTypeStr.get(i)) )
				throw new ModelMissmatchException("Column DATATYPE missmatch. current jdbcDataTypes="+_rsmdColumnTypeStr+", passed jdbcDataTypes="+rstm._rsmdColumnTypeStr);
		}

		if (merge == false)
		{
//			fireTableRowsDeleted(0, _rows.size()-1);
//			_rows = rstm._rows;
//			fireTableRowsInserted(0, _rows.size()-1);
			
//			_rows = rstm._rows;
//			fireTableRowsUpdated(0, _rows.size()-1);

			_rows = rstm._rows;
//			_rows.clear();
//			_rows.addAll(rstm._rows);
			fireTableDataChanged();
		}
		else
		{
//			int firstRow = _rows.size() + 1;
//			int lastRow  = _rows.size() + rstm._rows.size();

			_rows.addAll(rstm._rows);

//			fireTableRowsInserted(firstRow, lastRow);
			fireTableDataChanged();
		}
	}

    
//	/** apply pipe cmd filter */
//	private boolean addRow(ArrayList<Object> row)
//	{
//		return true;
//	}
	/** apply pipe cmd filter 
	 * @param cols */
	private boolean addRow(ArrayList<String> cols, ArrayList<Object> row)
	{
		if (   _pipeCmd == null  ) return true;
		if ( ! _pipeCmd.isGrep() ) return true;

		PipeCommandGrep grep = (PipeCommandGrep)_pipeCmd.getCmd();
		
		// If NOT VALID for ResultSet, ADD the row
		if ( ! grep.isValidForType(PipeCommandGrep.TYPE_RESULTSET) )
			return true;

		String regexpStr = ".*" + grep.getConfig() + ".*";  // NOTE: maybe use: Pattern.compile("someRegExpPattern").matcher(str).find()
//		String regexpStr = ".*" + _pipeCmd.getRegExp() + ".*";
//System.out.println("ResultSetTableModel: applying filter: java-regexp '"+regexpStr+"', on row: "+row);

		// get check a specific column
		// TODO: check if this is working
		String grepColName = grep.getColName();
		if (grepColName != null && cols != null)
		{
			int colNumber = cols.indexOf(grepColName);
			if (colNumber < 0)
			{
				_logger.warn("PipeGrep: no column named '"+grepColName+"' in current ResultSet. it has following columns '"+cols+"'.");
				return true;
			}
			else
			{
				Object colObj = row.get(colNumber);
				boolean matches = colObj.toString().matches(regexpStr);
				if (grep.isOptV())
					return !matches;
				return matches;
			}
		}

		// FIXME: THIS NEEDS A LOT MORE WORK
		// _pipeStr needs to be "compiled" info a PipeFilter object and used in the below somehow
		// Pattern.compile(regex).matcher(input).matches()
		boolean aMatch = false;
		for (Object colObj : row)
		{
			if (colObj != null)
			{
				if (Pattern.compile(grep.getConfig()).matcher(colObj.toString()).find())
//				if ( colObj.toString().matches(regexpStr) )
				{
					aMatch = true;
					break;
				}
			}
		}
		if (grep.isOptV())
			return !aMatch;
		return aMatch;
	}

	public static String getColumnTypeName(ResultSetMetaData rsmd, int col)
	{
		String columnTypeName;
		// in RepServer, the getColumnTypeName() throws exception: JZ0SJ ... wants metadata 
		try
		{
			columnTypeName = rsmd.getColumnTypeName(col);

			try
			{
				int columnType = rsmd.getColumnType(col);
				if (    columnType == java.sql.Types.NUMERIC 
				     || columnType == java.sql.Types.DECIMAL )
				{
					int precision = rsmd.getPrecision(col);
					int scale     = rsmd.getScale(col);
					
					columnTypeName += "("+precision+","+scale+")";
				}
				if (    columnType == java.sql.Types.CHAR 
				     || columnType == java.sql.Types.VARCHAR 
				     || columnType == java.sql.Types.NCHAR
				     || columnType == java.sql.Types.NVARCHAR
				     || columnType == java.sql.Types.BINARY
				     || columnType == java.sql.Types.VARBINARY
				   )
				{
					int columnDisplaySize = rsmd.getColumnDisplaySize(col);
					
//					if (columnType == java.sql.Types.BINARY || columnType == java.sql.Types.VARBINARY)
//						columnDisplaySize += 2; // just in case we need the extra length when storing a binary with the prefix '0x'

					columnTypeName += "("+columnDisplaySize+")";
				}
			}
			catch (SQLException ignore) {}
			
			if (columnTypeName == null)
				throw new SQLException("Dummy exception to get static resolution for the SQL Type based on the JDBC Type");
		}
		catch (SQLException e)
		{
			// Can we "compose" the datatype from the JavaType and DisplaySize???
			//columnTypeName = guessDataType(columnType, rsmd.getColumnDisplaySize(c));
			columnTypeName = "unhandled-jdbc-datatype";

			try
			{
				int columnType        = rsmd.getColumnType(col);
				int columnDisplaySize = rsmd.getColumnDisplaySize(col);
				int precision         = 0;
				int scale             = 0;

				if ( columnType == java.sql.Types.NUMERIC || columnType == java.sql.Types.DECIMAL )
				{
					precision = rsmd.getPrecision(col);
					scale     = rsmd.getScale(col);
				}

				//-------------------------------------------------
				// NOTE: this is JUST FOR REPSERVER
				// or if main test fails (se top of method)
				//-------------------------------------------------
				switch (columnType)
				{
				case java.sql.Types.BIT:          return "bit";
				case java.sql.Types.TINYINT:      return "tinyint";
				case java.sql.Types.SMALLINT:     return "smallint";
				case java.sql.Types.INTEGER:      return "int";
				case java.sql.Types.BIGINT:       return "bigint";
				case java.sql.Types.FLOAT:        return "float";
				case java.sql.Types.REAL:         return "real";
				case java.sql.Types.DOUBLE:       return "double";
				case java.sql.Types.NUMERIC:      return "numeric("+precision+","+scale+")";
				case java.sql.Types.DECIMAL:      return "decimal("+precision+","+scale+")";
				case java.sql.Types.CHAR:         return "char("+columnDisplaySize+")";
				case java.sql.Types.VARCHAR:      return "varchar("+columnDisplaySize+")";
				case java.sql.Types.LONGVARCHAR:  return "text";
				case java.sql.Types.DATE:         return "date";
				case java.sql.Types.TIME:         return "time";
				case java.sql.Types.TIMESTAMP:    return "datetime";
				case java.sql.Types.BINARY:       return "binary("+columnDisplaySize+")";    // just in case we need the extra length when storing a binary with the prefix '0x'
				case java.sql.Types.VARBINARY:    return "varbinary("+columnDisplaySize+")"; // just in case we need the extra length when storing a binary with the prefix '0x'
//				case java.sql.Types.BINARY:       return "binary("+(columnDisplaySize+2)+")";    // just in case we need the extra length when storing a binary with the prefix '0x'
//				case java.sql.Types.VARBINARY:    return "varbinary("+(columnDisplaySize+2)+")"; // just in case we need the extra length when storing a binary with the prefix '0x'
				case java.sql.Types.LONGVARBINARY:return "image";
				case java.sql.Types.NULL:         return "-null-";
				case java.sql.Types.OTHER:        return "-other-";
				case java.sql.Types.JAVA_OBJECT:  return "-java_object";
//				case java.sql.Types.DISTINCT:     return "-DISTINCT-";
//				case java.sql.Types.STRUCT:       return "-STRUCT-";
//				case java.sql.Types.ARRAY:        return "-ARRAY-";
				case java.sql.Types.BLOB:         return "image";
				case java.sql.Types.CLOB:         return "text";
//				case java.sql.Types.REF:          return "-REF-";
//				case java.sql.Types.DATALINK:     return "-DATALINK-";
				case java.sql.Types.BOOLEAN:      return "bit";
				default:
					columnTypeName = "unknown-jdbc-datatype("+columnType+")";;
				}
			}
			catch (SQLException e1)
			{
			}
		}
		return columnTypeName;
	}

	public static String getColumnJavaSqlTypeName(int columnType)
	{
//		switch (columnType)
//		{
//		case java.sql.Types.BIT:          return "java.sql.Types.BIT";
//		case java.sql.Types.TINYINT:      return "java.sql.Types.TINYINT";
//		case java.sql.Types.SMALLINT:     return "java.sql.Types.SMALLINT";
//		case java.sql.Types.INTEGER:      return "java.sql.Types.INTEGER";
//		case java.sql.Types.BIGINT:       return "java.sql.Types.BIGINT";
//		case java.sql.Types.FLOAT:        return "java.sql.Types.FLOAT";
//		case java.sql.Types.REAL:         return "java.sql.Types.REAL";
//		case java.sql.Types.DOUBLE:       return "java.sql.Types.DOUBLE";
//		case java.sql.Types.NUMERIC:      return "java.sql.Types.NUMERIC";
//		case java.sql.Types.DECIMAL:      return "java.sql.Types.DECIMAL";
//		case java.sql.Types.CHAR:         return "java.sql.Types.CHAR";
//		case java.sql.Types.VARCHAR:      return "java.sql.Types.VARCHAR";
//		case java.sql.Types.LONGVARCHAR:  return "java.sql.Types.LONGVARCHAR";
//		case java.sql.Types.DATE:         return "java.sql.Types.DATE";
//		case java.sql.Types.TIME:         return "java.sql.Types.TIME";
//		case java.sql.Types.TIMESTAMP:    return "java.sql.Types.TIMESTAMP";
//		case java.sql.Types.BINARY:       return "java.sql.Types.BINARY";
//		case java.sql.Types.VARBINARY:    return "java.sql.Types.VARBINARY";
//		case java.sql.Types.LONGVARBINARY:return "java.sql.Types.LONGVARBINARY";
//		case java.sql.Types.NULL:         return "java.sql.Types.NULL";
//		case java.sql.Types.OTHER:        return "java.sql.Types.OTHER";
//		case java.sql.Types.JAVA_OBJECT:  return "java.sql.Types.JAVA_OBJECT";
//		case java.sql.Types.DISTINCT:     return "java.sql.Types.DISTINCT";
//		case java.sql.Types.STRUCT:       return "java.sql.Types.STRUCT";
//		case java.sql.Types.ARRAY:        return "java.sql.Types.ARRAY";
//		case java.sql.Types.BLOB:         return "java.sql.Types.BLOB";
//		case java.sql.Types.CLOB:         return "java.sql.Types.CLOB";
//		case java.sql.Types.REF:          return "java.sql.Types.REF";
//		case java.sql.Types.DATALINK:     return "java.sql.Types.DATALINK";
//		case java.sql.Types.BOOLEAN:      return "java.sql.Types.BOOLEAN";
//		default:
//			return "unknown-datatype("+columnType+")";
//		}
		switch (columnType)
		{
		case java.sql.Types.BIT:           return "java.sql.Types.BIT";
		case java.sql.Types.TINYINT:       return "java.sql.Types.TINYINT";
		case java.sql.Types.SMALLINT:      return "java.sql.Types.SMALLINT";
		case java.sql.Types.INTEGER:       return "java.sql.Types.INTEGER";
		case java.sql.Types.BIGINT:        return "java.sql.Types.BIGINT";
		case java.sql.Types.FLOAT:         return "java.sql.Types.FLOAT";
		case java.sql.Types.REAL:          return "java.sql.Types.REAL";
		case java.sql.Types.DOUBLE:        return "java.sql.Types.DOUBLE";
		case java.sql.Types.NUMERIC:       return "java.sql.Types.NUMERIC";
		case java.sql.Types.DECIMAL:       return "java.sql.Types.DECIMAL";
		case java.sql.Types.CHAR:          return "java.sql.Types.CHAR";
		case java.sql.Types.VARCHAR:       return "java.sql.Types.VARCHAR";
		case java.sql.Types.LONGVARCHAR:   return "java.sql.Types.LONGVARCHAR";
		case java.sql.Types.DATE:          return "java.sql.Types.DATE";
		case java.sql.Types.TIME:          return "java.sql.Types.TIME";
		case java.sql.Types.TIMESTAMP:     return "java.sql.Types.TIMESTAMP";
		case java.sql.Types.BINARY:        return "java.sql.Types.BINARY";
		case java.sql.Types.VARBINARY:     return "java.sql.Types.VARBINARY";
		case java.sql.Types.LONGVARBINARY: return "java.sql.Types.LONGVARBINARY";
		case java.sql.Types.NULL:          return "java.sql.Types.NULL";
		case java.sql.Types.OTHER:         return "java.sql.Types.OTHER";
		case java.sql.Types.JAVA_OBJECT:   return "java.sql.Types.JAVA_OBJECT";
		case java.sql.Types.DISTINCT:      return "java.sql.Types.DISTINCT";
		case java.sql.Types.STRUCT:        return "java.sql.Types.STRUCT";
		case java.sql.Types.ARRAY:         return "java.sql.Types.ARRAY";
		case java.sql.Types.BLOB:          return "java.sql.Types.BLOB";
		case java.sql.Types.CLOB:          return "java.sql.Types.CLOB";
		case java.sql.Types.REF:           return "java.sql.Types.REF";
		case java.sql.Types.DATALINK:      return "java.sql.Types.DATALINK";
		case java.sql.Types.BOOLEAN:       return "java.sql.Types.BOOLEAN";

		//------------------------- JDBC 4.0 -----------------------------------
		case java.sql.Types.ROWID:         return "java.sql.Types.ROWID";
		case java.sql.Types.NCHAR:         return "java.sql.Types.NCHAR";
		case java.sql.Types.NVARCHAR:      return "java.sql.Types.NVARCHAR";
		case java.sql.Types.LONGNVARCHAR:  return "java.sql.Types.LONGNVARCHAR";
		case java.sql.Types.NCLOB:         return "java.sql.Types.NCLOB";
		case java.sql.Types.SQLXML:        return "java.sql.Types.SQLXML";

		//------------------------- VENDOR SPECIFIC TYPES ---------------------------
		case -10:                          return "oracle.jdbc.OracleTypes.CURSOR";

		//------------------------- UNHANDLED TYPES  ---------------------------
		default:
			return "unknown-jdbc-datatype("+columnType+")";
		}
	}

// The below has not been tested... probably needs more work
//	private Class<?> getColumnJavaSqlClass(int colid)
//	{
//		int columnType = _rsmdColumnType.get(colid);
//
//		switch (columnType)
//		{
//		case java.sql.Types.BIT:           return Boolean.class;
//		case java.sql.Types.TINYINT:       return Integer.class;
//		case java.sql.Types.SMALLINT:      return Integer.class;
//		case java.sql.Types.INTEGER:       return Integer.class;
//		case java.sql.Types.BIGINT:        return Long.class;
//		case java.sql.Types.FLOAT:         return Float.class;
//		case java.sql.Types.REAL:          return Float.class;
//		case java.sql.Types.DOUBLE:        return Double.class;
//		case java.sql.Types.NUMERIC:       return BigDecimal.class;
//		case java.sql.Types.DECIMAL:       return BigDecimal.class;
//		case java.sql.Types.CHAR:          return String.class;
//		case java.sql.Types.VARCHAR:       return String.class;
//		case java.sql.Types.LONGVARCHAR:   return String.class;
//		case java.sql.Types.DATE:          return java.sql.Date.class;
//		case java.sql.Types.TIME:          return java.sql.Time.class;
//		case java.sql.Types.TIMESTAMP:     return java.sql.Timestamp.class;
//		case java.sql.Types.BINARY:        return String.class;
//		case java.sql.Types.VARBINARY:     return String.class;
//		case java.sql.Types.LONGVARBINARY: return String.class;
//		case java.sql.Types.NULL:          return Object.class;
//		case java.sql.Types.OTHER:         return Object.class;
//		case java.sql.Types.JAVA_OBJECT:   return Object.class;
//		case java.sql.Types.DISTINCT:      return Object.class;
//		case java.sql.Types.STRUCT:        return Object.class;
//		case java.sql.Types.ARRAY:         return Object.class;
//		case java.sql.Types.BLOB:          return java.sql.Blob.class;
//		case java.sql.Types.CLOB:          return java.sql.Clob.class;
//		case java.sql.Types.REF:           return Object.class;
//		case java.sql.Types.DATALINK:      return Object.class;
//		case java.sql.Types.BOOLEAN:       return Boolean.class;
//
//		//------------------------- JDBC 4.0 -----------------------------------
//		case java.sql.Types.ROWID:         return Object.class;
//		case java.sql.Types.NCHAR:         return String.class;
//		case java.sql.Types.NVARCHAR:      return String.class;
//		case java.sql.Types.LONGNVARCHAR:  return String.class;
//		case java.sql.Types.NCLOB:         return String.class;
//		case java.sql.Types.SQLXML:        return String.class;
//
//		//------------------------- VENDOR SPECIFIC TYPES ---------------------------
//		case -10:                          return Object.class;
//
//		//------------------------- UNHANDLED TYPES  ---------------------------
//		default:
//			return Object.class;
//		}
//	}


//	public List<SQLWarning> getSQLWarningList()
//	{
//		return _sqlWarnList;
//	}
	public SQLWarning getSQLWarning()
	{
		return _sqlWarning;
	}

	@Override
	public int getColumnCount()
	{
		return _rsmdColumnLabel.size();
	}

	@Override
	public int getRowCount()
	{
		return _rows.size();
	}

	@Override
	public String getColumnName(int column)
	{
		return (String)_rsmdColumnLabel.get(column);
	}

	// This TableModel method specifies the data type for each column.
	// We could map SQL types to Java types, but for this example, we'll just
	// convert all the returned data to strings.
//	public Class<?> getColumnClass(int column)
//	{
////		System.out.println("getColumnClass("+column+")");
//		try
//		{
//			String className = (String) _type.get(column);
//			if (className.equals("java.sql.Timestamp")) return String.class;
////			if (className.equals("java.sql.Timestamp")) return java.util.Date.class;
//			return Class.forName(className);
//		} 
//		catch (Exception e) 
//		{
//			e.printStackTrace();
//		}
//		return String.class;
//	}
	@Override
	public Class<?> getColumnClass(int colid)
	{
		if (getRowCount() == 0)
			return Object.class;

		// Get _classType (which is first row with NOT NULL values... if all rows is NULL, then Object.class will be returned...
		Class<?> clazz = _classType[colid+1];
		if (clazz == null)
			clazz = Object.class;
		return clazz;

		// So maybe the best thing would be to return a hard coded value based on the java.sql.Types
//		return getColumnJavaSqlClass(colid);
		
		// Get first row... and the type of that... but if it's null... it will return object, which isn't good enough
//		Object o = getValueAt(0, colid);
//		Class<?> clazz = o!=null ? o.getClass() : Object.class;
//		return clazz;
//		if (o instanceof Timestamp)
//			return Object.class;
//		else
//			return clazz;
//		return o!=null ? o.getClass() : Object.class;
	}


	@Override
	public Object getValueAt(int r, int c)
	{
		ArrayList<Object> row = _rows.get(r);
		Object o = row.get(c);
//		Object o = _rows.get(r).get(c);
		return (o != null) ? o : NULL_REPLACE;
//		if (o == null)
//			return null;//return "(NULL)";
//		else
//			return o.toString(); // Convert it to a string
	}

	// Table can be editable, but only for copy+paste use...
	@Override
	public boolean isCellEditable(int row, int column)
	{
		return _allowEdit;
	}

	// Since its not editable, we don't need to implement these methods
	@Override
	public void setValueAt(Object value, int row, int column)
	{
	}

//	public void addTableModelListener(TableModelListener l)
//	{
//	}
//
//	public void removeTableModelListener(TableModelListener l)
//	{
//	}

	

	/**
	 * Produce a HTML String with information about the current ResultSet
	 * @param index The column ID you want to disaply information about 
	 * @return a HTML String with a TABLE
	 *	<TABLE BORDER=1 CELLSPACING=1 CELLPADDING=1">
	 *	<TR> <TH>Description               </TH> <TH>From Java Method                     </TH> <TH>Value</TH> </TR>
	 *	<TR> <TD>Label                     </TD> <TD>rsmd.getColumnLabel()                </TD> <TD>xxx</TD> </TR>
	 *	<TR> <TD>Name                      </TD> <TD>rsmd.getColumnName()                 </TD> <TD>xxx</TD> </TR>
	 *	<TR> <TD>JDBC Type Name            </TD> <TD>rsmd.getColumnType()<b>->String</b>  </TD> <TD>java.sql.Types.VARCHAR</TD> </TR>
	 *	<TR> <TD>JDBC Type Number          </TD> <TD>rsmd.getColumnType()                 </TD> <TD>12</TD> </TR>
	 *	<TR> <TD>Java Class Name           </TD> <TD>rsmd.getColumnClassName()            </TD> <TD>java.lang.String</TD> </TR>
	 *	<TR> <TD>Raw DBMS Datatype         </TD> <TD>rsmd.getColumnTypeName()             </TD> <TD>varchar</TD> </TR>
	 *	<TR> <TD>Guessed DBMS Type         </TD> <TD>rsmd.getColumnTypeName()<b>+size</b> </TD> <TD>varchar(30)</TD> </TR>
	 *	<TR> <TD>TableModel Class Name     </TD> <TD>TableModel.getColumnClass()          </TD> <TD>class java.lang.String</TD> </TR>
	 *	</TABLE>
	 */
	public String getToolTipTextForTableHeader(int index)
	{
		if (_showRowNumber && index == 0)
			return "<html>Column '<b><code>"+ROW_NUMBER_COLNAME+"</code></b>' is not part of the actual Result Set, it has been added when reading the data.</html>";

		StringBuilder sb = new StringBuilder();
		sb.append("<HTML>");
		
		sb.append("Below is information about the Column based on the JDBC ResultSetMetaData object.<br>");
		sb.append("<TABLE ALIGN=\"left\" BORDER=1 CELLSPACING=1 CELLPADDING=1 WIDTH=\"100%\">");

		sb.append("<TR ALIGN=\"left\" VALIGN=\"middle\"> <TH>Description               </TH> <TH>From Java Method                     </TH> <TH>Value</TH> </TR>");

		sb.append("<TR ALIGN=\"left\" VALIGN=\"middle\"> <TD>Label                     </TD> <TD>rsmd.getColumnLabel()                </TD> <TD>").append(_rsmdColumnLabel.get(index))      .append("</TD> </TR>");
		sb.append("<TR ALIGN=\"left\" VALIGN=\"middle\"> <TD>Name                      </TD> <TD>rsmd.getColumnName()                 </TD> <TD>").append(_rsmdColumnName.get(index))       .append("</TD> </TR>");
		sb.append("<TR ALIGN=\"left\" VALIGN=\"middle\"> <TD>JDBC Type Name            </TD> <TD>rsmd.getColumnType()<b>->String</b>  </TD> <TD>").append(_rsmdColumnTypeStr.get(index))    .append("</TD> </TR>");
		sb.append("<TR ALIGN=\"left\" VALIGN=\"middle\"> <TD>JDBC Type Number          </TD> <TD>rsmd.getColumnType()                 </TD> <TD>").append(_rsmdColumnType.get(index))       .append("</TD> </TR>");
		sb.append("<TR ALIGN=\"left\" VALIGN=\"middle\"> <TD>Java Class Name           </TD> <TD>rsmd.getColumnClassName()            </TD> <TD>").append(_rsmdColumnClassName.get(index))  .append("</TD> </TR>");
		sb.append("<TR ALIGN=\"left\" VALIGN=\"middle\"> <TD>Raw DBMS Datatype         </TD> <TD>rsmd.getColumnTypeName()             </TD> <TD>").append(_rsmdColumnTypeName.get(index))   .append("</TD> </TR>");
		sb.append("<TR ALIGN=\"left\" VALIGN=\"middle\"> <TD>Guessed DBMS Type         </TD> <TD>rsmd.getColumnTypeName()<b>+size</b> </TD> <TD>").append(_rsmdColumnTypeNameStr.get(index)).append("</TD> </TR>");
		sb.append("<TR ALIGN=\"left\" VALIGN=\"middle\"> <TD>TableModel Class Name     </TD> <TD>TableModel.getColumnClass()          </TD> <TD>").append(getColumnClass(index))            .append("</TD> </TR>");
//		sb.append("<TR ALIGN=\"left\" VALIGN=\"middle\"> <TD>Display size max(disp/len)</TD> <TD>_displaySize                         </TD> <TD>").append(_displaySize.get(index))          .append("</TD> </TR>");
		sb.append("<TR ALIGN=\"left\" VALIGN=\"middle\"> <TD>Table Name                </TD> <TD>rsmd.getTableName()                  </TD> <TD>").append(_rsmdRefTableName.get(index))     .append("</TD> </TR>");
		
		sb.append("</TABLE>");

		sb.append("</HTML>");
		return sb.toString();
	}
	
	/**
	 * Produce a string with information about the current ResultSet
	 * @return a string looking like (for <code>select * from sysobjects</code>):
	 * <pre>
	 * RS> Col# Label        JDBC Type Name           Guessed DBMS type
	 * RS> ---- ------------ ------------------------ -----------------
	 * RS> 1    name         java.sql.Types.VARCHAR   varchar(255)     
	 * RS> 2    id           java.sql.Types.INTEGER   int              
	 * RS> 3    uid          java.sql.Types.INTEGER   int              
	 * RS> 4    type         java.sql.Types.CHAR      char(2)          
	 * RS> 5    userstat     java.sql.Types.SMALLINT  smallint         
	 * RS> 6    sysstat      java.sql.Types.SMALLINT  smallint         
	 * RS> 7    indexdel     java.sql.Types.SMALLINT  smallint         
	 * RS> 8    schemacnt    java.sql.Types.SMALLINT  smallint         
	 * RS> 9    sysstat2     java.sql.Types.INTEGER   int              
	 * RS> 10   crdate       java.sql.Types.TIMESTAMP datetime         
	 * RS> 11   expdate      java.sql.Types.TIMESTAMP datetime         
	 * RS> 12   deltrig      java.sql.Types.INTEGER   int              
	 * RS> 13   instrig      java.sql.Types.INTEGER   int              
	 * RS> 14   updtrig      java.sql.Types.INTEGER   int              
	 * RS> 15   seltrig      java.sql.Types.INTEGER   int              
	 * RS> 16   ckfirst      java.sql.Types.INTEGER   int              
	 * RS> 17   cache        java.sql.Types.SMALLINT  smallint         
	 * RS> 18   audflags     java.sql.Types.INTEGER   int              
	 * RS> 19   objspare     java.sql.Types.SMALLINT  unsigned smallint
	 * RS> 20   versionts    java.sql.Types.BINARY    binary(24)       
	 * RS> 21   loginame     java.sql.Types.VARCHAR   varchar(30)      
	 * RS> 22   identburnmax java.sql.Types.NUMERIC   numeric(38,0)    
	 * RS> 23   spacestate   java.sql.Types.SMALLINT  smallint         
	 * RS> 24   erlchgts     java.sql.Types.BINARY    binary(16)       
	 * RS> 25   sysstat3     java.sql.Types.SMALLINT  unsigned smallint
	 * RS> 26   lobcomp_lvl  java.sql.Types.TINYINT   tinyint          
	 * </pre>
	 * If the property 'ResultSetTableModel.print.rs.info.long' is 'true', 
	 * the following ResultSet information will be returned 
	 * <pre>
	 * RS> Col# Label                 Name                 JDBC Type Name               JDBC Type Number     JDBC Class Name           Raw DBMS Datatype        Guessed DBMS type             TableModel Class Name      
	 * RS>      rsmd.getColumnLabel() rsmd.getColumnName() rsmd.getColumnType()->String rsmd.getColumnType() rsmd.getColumnClassName() rsmd.getColumnTypeName() rsmd.getColumnTypeName()+size TableModel.getColumnClass()
	 * RS> ---- --------------------- -------------------- ---------------------------- -------------------- ------------------------- ------------------------ ----------------------------- ---------------------------
	 * RS> 1    name                  name                 java.sql.Types.VARCHAR       12                   java.lang.String          varchar                  varchar(255)                  class java.lang.String     
	 * RS> 2    id                    id                   java.sql.Types.INTEGER       4                    java.lang.Integer         int                      int                           class java.lang.Integer    
	 * RS> 3    uid                   uid                  java.sql.Types.INTEGER       4                    java.lang.Integer         int                      int                           class java.lang.Integer    
	 * RS> 4    type                  type                 java.sql.Types.CHAR          1                    java.lang.String          char                     char(2)                       class java.lang.String     
	 * RS> 5    userstat              userstat             java.sql.Types.SMALLINT      5                    java.lang.Integer         smallint                 smallint                      class java.lang.Integer    
	 * RS> 6    sysstat               sysstat              java.sql.Types.SMALLINT      5                    java.lang.Integer         smallint                 smallint                      class java.lang.Integer    
	 * RS> 7    indexdel              indexdel             java.sql.Types.SMALLINT      5                    java.lang.Integer         smallint                 smallint                      class java.lang.Integer    
	 * RS> 8    schemacnt             schemacnt            java.sql.Types.SMALLINT      5                    java.lang.Integer         smallint                 smallint                      class java.lang.Integer    
	 * RS> 9    sysstat2              sysstat2             java.sql.Types.INTEGER       4                    java.lang.Integer         int                      int                           class java.lang.Integer    
	 * RS> 10   crdate                crdate               java.sql.Types.TIMESTAMP     93                   java.sql.Timestamp        datetime                 datetime                      class java.lang.Object     
	 * RS> 11   expdate               expdate              java.sql.Types.TIMESTAMP     93                   java.sql.Timestamp        datetime                 datetime                      class java.lang.Object     
	 * RS> 12   deltrig               deltrig              java.sql.Types.INTEGER       4                    java.lang.Integer         int                      int                           class java.lang.Integer    
	 * RS> 13   instrig               instrig              java.sql.Types.INTEGER       4                    java.lang.Integer         int                      int                           class java.lang.Integer    
	 * RS> 14   updtrig               updtrig              java.sql.Types.INTEGER       4                    java.lang.Integer         int                      int                           class java.lang.Integer    
	 * RS> 15   seltrig               seltrig              java.sql.Types.INTEGER       4                    java.lang.Integer         int                      int                           class java.lang.Integer    
	 * RS> 16   ckfirst               ckfirst              java.sql.Types.INTEGER       4                    java.lang.Integer         int                      int                           class java.lang.Integer    
	 * RS> 17   cache                 cache                java.sql.Types.SMALLINT      5                    java.lang.Integer         smallint                 smallint                      class java.lang.Integer    
	 * RS> 18   audflags              audflags             java.sql.Types.INTEGER       4                    java.lang.Integer         int                      int                           class java.lang.Integer    
	 * RS> 19   objspare              objspare             java.sql.Types.SMALLINT      5                    java.lang.Integer         unsigned smallint        unsigned smallint             class java.lang.Integer    
	 * RS> 20   versionts             versionts            java.sql.Types.BINARY        -2                   [B                        binary                   binary(24)                    class java.lang.String     
	 * RS> 21   loginame              loginame             java.sql.Types.VARCHAR       12                   java.lang.String          varchar                  varchar(30)                   class java.lang.String     
	 * RS> 22   identburnmax          identburnmax         java.sql.Types.NUMERIC       2                    java.math.BigDecimal      numeric                  numeric(38,0)                 class java.lang.String     
	 * RS> 23   spacestate            spacestate           java.sql.Types.SMALLINT      5                    java.lang.Integer         smallint                 smallint                      class java.lang.Integer    
	 * RS> 24   erlchgts              erlchgts             java.sql.Types.BINARY        -2                   [B                        binary                   binary(16)                    class java.lang.String     
	 * RS> 25   sysstat3              sysstat3             java.sql.Types.SMALLINT      5                    java.lang.Integer         unsigned smallint        unsigned smallint             class java.lang.Integer    
	 * RS> 26   lobcomp_lvl           lobcomp_lvl          java.sql.Types.TINYINT       -6                   java.lang.Integer         tinyint                  tinyint                       class java.lang.Integer    
	 * </pre>
	 */
	public String getResultSetInfo()
	{
		StringBuilder sb = new StringBuilder();

		// LongDescription
		boolean ld = Configuration.getCombinedConfiguration().getBooleanProperty(PROPKEY_PrintResultSetInfoLong, DEFAULT_PrintResultSetInfoLong);
		
		ArrayList<String> header1 = new ArrayList<String>();
		ArrayList<String> header2 = new ArrayList<String>();
		          header1.add("Col#");                        header2.add(" ");
		          header1.add("Label");                       header2.add("rsmd.getColumnLabel()");
		if (ld) { header1.add("Name");                        header2.add("rsmd.getColumnName()"); }
		          header1.add("JDBC Type Name");              header2.add("rsmd.getColumnType()->String");
		if (ld) { header1.add("JDBC Type Number");            header2.add("rsmd.getColumnType()"); }
		if (ld) { header1.add("JDBC Class Name");             header2.add("rsmd.getColumnClassName()"); }
		if (ld) { header1.add("Raw DBMS Datatype");           header2.add("rsmd.getColumnTypeName()"); }
		          header1.add("Guessed DBMS type");           header2.add("rsmd.getColumnTypeName()+size");
		          header1.add("Source Table");                header2.add("rsmd.getTableName()");
		if (ld) { header1.add("TableModel Class Name");       header2.add("TableModel.getColumnClass()"); }

		int headCols = header1.size();
		int numColsInRs = _numcols - 1; // _numcols starts at 1

		int startAt = 0;
		if (_showRowNumber)
		{
			startAt = 1;
			numColsInRs++;
		}
		
		// Fill in 1 row for each column in the ResultSet
		ArrayList<ArrayList<String>> rows = new ArrayList<ArrayList<String>>();
		for (int c=startAt; c<numColsInRs; c++)
		{
			ArrayList<String> row = new ArrayList<String>();
			rows.add(row);

			        row.add( "" + (_showRowNumber ? c : c+1) ); // _showRowNumber == skip first column... 
			        row.add( "" + _rsmdColumnLabel      .get(c) );
			if (ld) row.add( "" + _rsmdColumnName       .get(c)  );
			        row.add( "" + _rsmdColumnTypeStr    .get(c) );
			if (ld) row.add( "" + _rsmdColumnType       .get(c) );
			if (ld) row.add( "" + _rsmdColumnClassName  .get(c) );
			if (ld) row.add( "" + _rsmdColumnTypeName   .get(c) );
			        row.add( "" + _rsmdColumnTypeNameStr.get(c) );
			        row.add( "" + _rsmdRefTableName     .get(c) );
			if (ld) row.add( "" + getColumnClass(c) );
		}

		// Get max length for each column
		int[] maxLen = new int[header1.size()];
		// Header 1
		for (int c=0; c<headCols; c++)
			maxLen[c] = Math.max(maxLen[c], header1.get(c).length());
		// Header 2
		if (ld)
			for (int c=0; c<headCols; c++)
				maxLen[c] = Math.max(maxLen[c], header2.get(c).length());
		// Rows
		for (int c=0; c<headCols; c++)
			for (ArrayList<String> r : rows)
				maxLen[c] = Math.max(maxLen[c], r.get(c).length());

		// Now build table (header)

		// header1
		// header2 (if longDescription)
		// -------- 
		// data 

		String prefix = "RS>";
		// header 1
		sb.append(prefix);
		for (int c=0; c<headCols; c++)
			sb.append(" ").append(StringUtil.left(header1.get(c), maxLen[c]));
		sb.append("\n");

		// header 2
		if (ld)
		{
			sb.append(prefix);
			for (int c=0; c<headCols; c++)
				sb.append(" ").append(StringUtil.left(header2.get(c), maxLen[c]));
			sb.append("\n");
		}

		// -------- 
		sb.append(prefix);
		for (int c=0; c<headCols; c++)
			sb.append(" ").append(StringUtil.replicate("-", maxLen[c]));
		sb.append("\n");

		// data 
		for (ArrayList<String> r : rows)
		{
			sb.append(prefix);
			for (int c=0; c<headCols; c++)
				sb.append(" ").append(StringUtil.left(r.get(c), maxLen[c]));
			sb.append("\n");
		}
		// remove last newline
		sb.replace(sb.length()-1, sb.length(), "");

		return sb.toString();
	}
	
	public static String getResultSetInfo(ResultSetMetaData rsmd)
	{
		try
		{
    		ResultSetTableModel rstm = new ResultSetTableModel(rsmd);
    		return rstm.getResultSetInfo();
		}
		catch (SQLException e)
		{
			return e.toString();
		}
	}

	
	//////////////////////////////////////////////////////////////////////////////
	//////////////////////////////////////////////////////////////////////////////
	//////////////////////////////////////////////////////////////////////////////
	// Some extra stuff to make it printable in a column spaced order
	//////////////////////////////////////////////////////////////////////////////
	//////////////////////////////////////////////////////////////////////////////
	//////////////////////////////////////////////////////////////////////////////

	public int getSqlType(int column)
	{
		return _rsmdColumnType.get(column).intValue();
	}

	public int getColumnDisplaySize(int column)
	{
		return _displaySize.get(column).intValue();
	}

	public String getColumnNameFullSize(int column)
	{
		int fullSize = getColumnDisplaySize(column);
		return StringUtil.left(getColumnName(column), fullSize);
	}

	/** get a '----' with the display size of the column */
	public String getColumnLineFullSize(int column)
	{
		int fullSize = getColumnDisplaySize(column);
		return StringUtil.replicate("-", fullSize);
	}

	public String getValueAtFullSize(int r, int c)
	{
		ArrayList<Object> row = _rows.get(r);
		Object o = row.get(c);

		String str;
		if (o == null)
			str = "NULL";//return "(NULL)";
		else
			str = o.toString(); // Convert it to a string

		int fullSize = getColumnDisplaySize(c);
		
		if (o instanceof Number)
			return StringUtil.right(str, fullSize);
		else
			return StringUtil.left(str, fullSize);
	}

	public String toTableString()
	{
		return toTableString(" ");
	}
	public String toTableString(String colSep)
	{
		StringBuilder sb = new StringBuilder(1024);

		int cols = getColumnCount();
		int rows = getRowCount();
		
		// build header names
		for (int c=0; c<cols; c++)
		{
			sb.append(colSep).append(getColumnNameFullSize(c));
		}
		sb.append("\n");

		// build header lines ------
		for (int c=0; c<cols; c++)
		{
			sb.append(colSep).append(getColumnLineFullSize(c));
		}
		sb.append("\n");

		// build all data rows...
		for (int r=0; r<rows; r++)
		{
			for (int c=0; c<cols; c++)
			{
				sb.append(colSep).append(getValueAtFullSize(r,c));
			}
			sb.append("\n");
		}

		return sb.toString();
	}

	public String toHtmlTableString()
	{
		StringBuilder sb = new StringBuilder(1024);

		sb.append("<table border=1>\n");
		int cols = getColumnCount();
		int rows = getRowCount();
		
		// build header names
		sb.append("<tr>");
		for (int c=0; c<cols; c++)
		{
			sb.append("<td nowrap>").append(getColumnName(c)).append("</td>");
		}
		sb.append("</tr>\n");

		sb.append("<tr>");
		// build all data rows...
		for (int r=0; r<rows; r++)
		{
			for (int c=0; c<cols; c++)
			{
				sb.append("<td nowrap>").append(getValueAtFullSize(r,c)).append("</td>");
			}
			sb.append("\n");
		}
		sb.append("</tr>\n");
		sb.append("</table>\n");

		return sb.toString();
	}

	/**
	 * Get 1 rows as a HTML Table. <br>
	 * Left column is column names (in bold)<br>
	 * Right column is cow content
	 * 
	 * @param mrow
	 * @return
	 */
	public String toHtmlTableString(int mrow, boolean borders)
	{
		StringBuilder sb = new StringBuilder(1024);

		int cols = getColumnCount();
		String border = borders ? " border=1" : " border=0";

		// One row for every column
		sb.append("<table").append(border).append(">\n");
		for (int c=0; c<cols; c++)
		{
			sb.append("<tr>");
			sb.append("<td nowrap><b>").append(getColumnName(c))  .append("</b></td>");
			sb.append("<td nowrap>")   .append(getValueAt(mrow,c)).append("</td>");
			sb.append("</tr>\n");
		}
		sb.append("</table>\n");

		return sb.toString();
	}
	

	
	//------------------------------------------------------------
	//-- BEGIN: getValueAsXXXXX using column name
	//          more methods will be added as they are needed
	//------------------------------------------------------------
	public String getValueAsString(int mrow, String colName)
	{
		return getValueAsString(mrow, colName, true);
	}
	public String getValueAsString(int mrow, String colName, boolean caseSensitive)
	{
		Object o = getValueAsObject(mrow, colName, caseSensitive);

		if (o == null)
			return null;

		return o.toString();
	}

	public Short getValueAsShort(int mrow, String colName)
	{
		return getValueAsShort(mrow, colName, true);
	}
	public Short getValueAsShort(int mrow, String colName, boolean caseSensitive)
	{
		Object o = getValueAsObject(mrow, colName, caseSensitive);

		if (o == null)
			return null;

		if (o instanceof Number)
			return ((Number)o).shortValue();

		try
		{
			return Short.parseShort(o.toString());
		}
		catch(NumberFormatException e)
		{
			_logger.warn("Problem reading Short value for mrow="+mrow+", column='"+colName+"', TableModelNamed='"+getName()+"', returning null. Caught: "+e);
			return null;
		}
	}

	public Integer getValueAsInteger(int mrow, String colName)
	{
		return getValueAsInteger(mrow, colName, true);
	}
	public Integer getValueAsInteger(int mrow, String colName, boolean caseSensitive)
	{
		Object o = getValueAsObject(mrow, colName, caseSensitive);

		if (o == null)
			return null;

		if (o instanceof Number)
			return ((Number)o).intValue();

		try
		{
			return Integer.parseInt(o.toString());
		}
		catch(NumberFormatException e)
		{
			_logger.warn("Problem reading Integer value for mrow="+mrow+", column='"+colName+"', TableModelNamed='"+getName()+"', returning null. Caught: "+e);
			return null;
		}
	}

	public Long getValueAsLong(int mrow, String colName)
	{
		return getValueAsLong(mrow, colName, true);
	}
	public Long getValueAsLong(int mrow, String colName, boolean caseSensitive)
	{
		Object o = getValueAsObject(mrow, colName, caseSensitive);

		if (o == null)
			return null;

		if (o instanceof Number)
			return ((Number)o).longValue();

		try
		{
			return Long.parseLong(o.toString());
		}
		catch(NumberFormatException e)
		{
			_logger.warn("Problem reading Long value for mrow="+mrow+", column='"+colName+"', TableModelNamed='"+getName()+"', returning null. Caught: "+e);
			return null;
		}
	}

	public Timestamp getValueAsTimestamp(int mrow, String colName)
	{
		return getValueAsTimestamp(mrow, colName, true);
	}
	public Timestamp getValueAsTimestamp(int mrow, String colName, boolean caseSensitive)
	{
		Object o = getValueAsObject(mrow, colName, caseSensitive);

		if (o == null)
			return null;

		if (o instanceof Timestamp)
			return ((Timestamp)o);

		try
		{
			SimpleDateFormat sdf = new SimpleDateFormat();
			java.util.Date date = sdf.parse(o.toString());
			return new Timestamp(date.getTime());
		}
		catch(ParseException e)
		{
			_logger.warn("Problem reading Timestamp value for mrow="+mrow+", column='"+colName+"', TableModelNamed='"+getName()+"', returning null. Caught: "+e);
			return null;
		}
	}

	public BigDecimal getValueAsBigDecimal(int mrow, String colName)
	{
		return getValueAsBigDecimal(mrow, colName, true);
	}
	public BigDecimal getValueAsBigDecimal(int mrow, String colName, boolean caseSensitive)
	{
		Object o = getValueAsObject(mrow, colName, caseSensitive);

		if (o == null)
			return null;

		if (o instanceof BigDecimal)
			return ((BigDecimal)o);

		try
		{
			return new BigDecimal(o.toString());
		}
		catch(NumberFormatException e)
		{
			_logger.warn("Problem reading BigDecimal value for mrow="+mrow+", column='"+colName+"', TableModelNamed='"+getName()+"', returning null. Caught: "+e);
			return null;
		}
	}

	public Object getValueAsObject(int mrow, String colName)
	{
		return getValueAsObject(mrow, colName, true);
	}
	public Object getValueAsObject(int mrow, String colName, boolean caseSensitive)
	{
//		int col_pos = findViewColumn(colName, caseSensitive);
//		if (col_pos < 0)
//			throw new RuntimeException("Can't find column '"+colName+"' in JTable named '"+getName()+"'.");

		TableModel tm = this;
//		int mrow = convertRowIndexToModel(mrow);
//		int mcol = convertColumnIndexToModel(col_pos);
		int mcol = -1;

		// get column pos from the model, if it's hidden in the JXTable
		for (int c=0; c<tm.getColumnCount(); c++) 
		{
			if ( caseSensitive ? colName.equals(tm.getColumnName(c)) : colName.equalsIgnoreCase(tm.getColumnName(c)) ) 
			{
				mcol = c;
				break;
			}
		}
		if (mcol < 0)
			throw new RuntimeException("Can't find column '"+colName+"' in TableModel named '"+getName()+"'.");
		
//System.out.println("getValueAsObject(mrow="+mrow+", colName='"+colName+"'): col_pos="+col_pos+", mrow="+mrow+", mcol="+mcol+".");
		Object o = tm.getValueAt(mrow, mcol);

		if (tm instanceof ResultSetTableModel)
		{
			if (o != null && o instanceof String)
			{
				if (ResultSetTableModel.DEFAULT_NULL_REPLACE.equals(o))
					return null;
			}
		}
		return o;
	}
	//------------------------------------------------------------
	//-- END: getValueAsXXXXX using column name
	//------------------------------------------------------------
}
