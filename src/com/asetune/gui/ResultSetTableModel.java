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

/**
 * @author <a href="mailto:goran_schwarz@hotmail.com">Goran Schwarz</a>
 */
package com.asetune.gui;

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.Date;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.sql.SQLWarning;
import java.sql.Statement;
import java.sql.Time;
import java.sql.Timestamp;
import java.sql.Types;
import java.text.NumberFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Properties;
import java.util.regex.Pattern;

import javax.swing.table.AbstractTableModel;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableModel;

import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.text.StringEscapeUtils;
import org.apache.log4j.Logger;
import org.apache.log4j.PropertyConfigurator;
import org.jdesktop.swingx.decorator.Highlighter;

import com.asetune.cm.ResultSetTableComparator;
import com.asetune.cm.SortOptions;
import com.asetune.cm.SortOptions.ColumnNameSensitivity;
import com.asetune.cm.SortOptions.DataSortSensitivity;
import com.asetune.cm.SortOptions.SortOrder;
import com.asetune.sql.ResultSetMetaDataCached;
import com.asetune.sql.SqlProgressDialog;
import com.asetune.sql.conn.DbxConnection;
import com.asetune.sql.pipe.PipeCommand;
import com.asetune.sql.pipe.PipeCommandConvert;
import com.asetune.sql.pipe.PipeCommandGrep;
import com.asetune.sql.showplan.transform.SqlServerShowPlanXmlTransformer;
import com.asetune.utils.Configuration;
import com.asetune.utils.DbUtils;
import com.asetune.utils.StringUtil;
import com.asetune.utils.SwingUtils;


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

	public static final String  PROPKEY_StringRtrim = "ResultSetTableModel.string.rtrim";
	public static final boolean DEFAULT_StringRtrim = true;

	public static final String  PROPKEY_StringTrim = "ResultSetTableModel.string.trim";
	public static final boolean DEFAULT_StringTrim = false;

	public static final String  PROPKEY_ShowRowNumber = "ResultSetTableModel.show.rowNumber";
	public static final boolean DEFAULT_ShowRowNumber = false;

	public static final String  PROPKEY_HtmlToolTip_stripeColor = "ResultSetTableModel.tooltip.stripe.color";
	public static final String  DEFAULT_HtmlToolTip_stripeColor = "#ffffff"; // White
//	public static final String  DEFAULT_HtmlToolTip_stripeColor = "#f2f2f2"; // Light gary

	public static final String  PROPKEY_HtmlToolTip_maxCellLength = "ResultSetTableModel.tooltip.cell.maxLen";
	public static final int     DEFAULT_HtmlToolTip_maxCellLength = 256;

	private static final String  BINARY_PREFIX  = Configuration.getCombinedConfiguration().getProperty(       PROPKEY_BINERY_PREFIX,  DEFAULT_BINERY_PREFIX);
	private static final boolean BINARY_TOUPPER = Configuration.getCombinedConfiguration().getBooleanProperty(PROPKEY_BINARY_TOUPPER, DEFAULT_BINARY_TOUPPER);
	private static final String  NULL_REPLACE   = Configuration.getCombinedConfiguration().getProperty(       PROPKEY_NULL_REPLACE,   DEFAULT_NULL_REPLACE);
	public  static final String  ROW_NUMBER_COLNAME = "row#";
	
	public static final String  SQLSERVER_JSON_COLUMN_LABEL     = "JSON_F52E2B61-18A1-11d1-B105-00805F49916B";
	public static final String  PROPKEY_SqlServerJconConcatRows = "ResultSetTableModel.sqlserver.json.concat.rows";
	public static final boolean DEFAULT_SqlServerJconConcatRows = true;

	public static final String  PROPKEY_TimestampToStringFmt = "ResultSetTableModel.Timestamp.toString.format";
	public static final String  DEFAULT_TimestampToStringFmt = null; // According to SimpleDateFormat... if null just use Timestamp default 
	public static final String  DEFAULT_TimestampToStringFmt_YMD_HMS = "yyyy-MM-dd HH:mm:ss";

	public static final String  PROPKEY_NumberToStringFmt = "ResultSetTableModel.Number.toString.format";
	public static final boolean DEFAULT_NumberToStringFmt = false; 
//	public static final boolean DEFAULT_NumberToStringFmt_local = true;

	public static final String  PROPKEY_TimeWithTimezone_to_time           = "ResultSetTableModel.timeWithTimezone.to.time";
	public static final boolean DEFAULT_TimeWithTimezone_to_time           = true;

	public static final String  PROPKEY_TimestampWithTimezone_to_timestamp = "ResultSetTableModel.timestampWithTimezone.to.timestamp";
	public static final boolean DEFAULT_TimestampWithTimezone_to_timestamp = true;

	private boolean _timeWithTimezone_to_time           = Configuration.getCombinedConfiguration().getBooleanProperty(PROPKEY_TimeWithTimezone_to_time,           DEFAULT_TimeWithTimezone_to_time);
	private boolean _timestampWithTimezone_to_timestamp = Configuration.getCombinedConfiguration().getBooleanProperty(PROPKEY_TimestampWithTimezone_to_timestamp, DEFAULT_TimestampWithTimezone_to_timestamp);

	private int	_numcols;

//	FIXME: cleanup the below members and INSTEAD use: ResultSetMetaDataCached _rsmdCached
	
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
	private int                          _discardedXRows        = -1;

	private boolean                      _stringRtrim           = Configuration.getCombinedConfiguration().getBooleanProperty(PROPKEY_StringRtrim,    DEFAULT_StringRtrim);
	private boolean                      _stringTrim            = Configuration.getCombinedConfiguration().getBooleanProperty(PROPKEY_StringTrim,     DEFAULT_StringTrim);
	private boolean                      _showRowNumber         = Configuration.getCombinedConfiguration().getBooleanProperty(PROPKEY_ShowRowNumber,  DEFAULT_ShowRowNumber);

	private int                          _readResultSetTime     = -1;

	/** when using getValueAsXxx: if null values return a "empty" string or in numbers return a 0 */
	private boolean                      _nullValuesAsEmptyInGetValueAsType = false;
	
	private String                       _toStringTimestampFormat = Configuration.getCombinedConfiguration().getProperty(PROPKEY_TimestampToStringFmt, DEFAULT_TimestampToStringFmt);
	private boolean                      _toStringNumberFormat    = false;
	
	private String                       _originSqlText;

	private String                       _dbmsProductName = "-unknown-";
	private ResultSetMetaDataCached      _rsmdCached;
//	private IDbmsDdlResolver             _dbmsDdlResolver;
	
	private boolean _handleColumnNotFoundAsNullValueInGetValues = false;

	/** Set the name of this table model, could be used for debugging or other tracking purposes */
	public void setName(String name) { _name = name; }

	/** Get the name of this table model, could be used for debugging or other tracking purposes */
	public String getName() { return _name; }

	public int getResultSetReadTime() { return _readResultSetTime; }

	public void    setNullValuesAsEmptyInGetValuesAsType(boolean val) { _nullValuesAsEmptyInGetValueAsType = val; } 
	public boolean getNullValuesAsEmptyInGetValuesAsType()            { return _nullValuesAsEmptyInGetValueAsType; } 

	public void    setSqlText(String sql) { _originSqlText = sql; }
	public String  getSqlText()           { return _originSqlText; }
	

	public ResultSetMetaDataCached getResultSetMetaDataCached() { return _rsmdCached; }
	public String                  getOriginDbmsProductName()   { return _dbmsProductName; }

	public void    setHandleColumnNotFoundAsNullValueInGetValues(boolean b) { _handleColumnNotFoundAsNullValueInGetValues = b; }
	public boolean isHandleColumnNotFoundAsNullValueInGetValues()           { return _handleColumnNotFoundAsNullValueInGetValues; }
	
	
	/**
	 * INTERNAL: used by: <code>public static String getResultSetInfo(ResultSetTableModel rstm)</code>
	 * @param rsmd
	 * @throws SQLException
	 */
	private ResultSetTableModel(ResultSetMetaData rsmd) 
	throws SQLException
	{
		this(null, rsmd, false, "getResultSetInfo", null, -1, -1, false, null, null);
	}

	private ResultSetTableModel(String name) 
	{
		setName(name);		
	}
	public static ResultSetTableModel createEmpty(String name)
	{
		return new ResultSetTableModel(name);
	}

	/**
	 * This constructor creates a TableModel from a ResultSet.  
	 **/
	public ResultSetTableModel(ResultSet rs, String name) 
	throws SQLException
	{
		this(rs, true, name, null);
	}
	public ResultSetTableModel(ResultSet rs, String name, String sqlText) 
	throws SQLException
	{
		this(rs, true, name, sqlText);
	}
	public ResultSetTableModel(ResultSet rs, boolean editable, String name, String sqlText) 
	throws SQLException
	{
		this(rs, editable, name, sqlText, -1, -1, false, null, null);
	}
	public ResultSetTableModel(ResultSet rs, boolean editable, String name, String sqlText, int stopAfterXrows, int onlyLastXrows, boolean noData, PipeCommand pipeCommand, SqlProgressDialog progress) 
	throws SQLException
	{
		this(rs, rs.getMetaData(), editable, name, sqlText, stopAfterXrows, onlyLastXrows, noData, pipeCommand, progress);
	}
	public ResultSetTableModel(ResultSet rs, ResultSetMetaData rsmd, boolean editable, String name, String sqlText, int stopAfterXrows, int onlyLastXrows, boolean noData, PipeCommand pipeCommand, SqlProgressDialog progress) 
	throws SQLException
	{
		long startTime = System.currentTimeMillis();

		setSqlText(sqlText);

		_allowEdit     = editable;
		setName(name);
		_pipeCmd       = pipeCommand;
//		_showRowNumber = showRowNumber;

		if (getName() != null)
			setName(getName().replace('\n', ' ')); // remove newlines in name

//		int maxDisplaySize = 32768;
		int maxDisplaySize = 65536;
		try { maxDisplaySize = Integer.parseInt( System.getProperty("ResultSetTableModel.maxDisplaySize", Integer.toString(maxDisplaySize)) ); }
		catch (NumberFormatException ignore) {};

		if (rsmd == null)
			rsmd = rs.getMetaData();
		
		_rsmdCached = new ResultSetMetaDataCached(rs, rsmd);

		// Check if this is a DbxConnection that was responsible for the call... then we can resolve the data types using that
		// Note: The below did not work since rs.getStatement().getConnection() returns the initial DBMS Vendor JDBC driver... com.sybase.jdbc4.jdbc.SybConnection or org.postgresql.jdbc.PgConnection
//		DbxConnection dbxConn = null;
//		if (rs.getStatement() != null)
//		{
//			Connection conn = rs.getStatement().getConnection();
////System.out.println("ResultSetTableModel: rs.getStatement().getConnection() == " + conn);
//			if (conn != null && conn instanceof DbxConnection)
//				dbxConn = (DbxConnection) conn;
//		}
		if (rs.getStatement() != null)
		{
			try
			{
				Connection conn = rs.getStatement().getConnection();
				_dbmsProductName = conn.getMetaData().getDatabaseProductName();
			}
			catch (SQLException ignore) {}
		}
		
		
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
			_rsmdRefTableName     .add("-none-");
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
//			try { refCatName        = rsmd.getCatalogName(c); } catch(SQLException ignore) {}
//			try { refSchName        = rsmd.getSchemaName(c);  } catch(SQLException ignore) {}
//			try { refTabName        = rsmd.getTableName(c);   } catch(SQLException ignore) {}
			try { refCatName        = rsmd.getCatalogName(c); } catch(Exception ignore) {}
			try { refSchName        = rsmd.getSchemaName(c);  } catch(Exception ignore) {}
			try { refTabName        = rsmd.getTableName(c);   } catch(Exception ignore) {}
			refCatName = StringUtil.isNullOrBlank(refCatName) ? "" : refCatName + ".";
			refSchName = StringUtil.isNullOrBlank(refSchName) ? "" : refSchName + ".";
			refTabName = StringUtil.isNullOrBlank(refTabName) ? "-none-" : refTabName;
			String fullRefTableName  = refCatName + refSchName + refTabName;
			
			String columnLabel       = rsmd.getColumnLabel(c);
			String columnName        = rsmd.getColumnName(c);
			String columnClassName   = rsmd.getColumnClassName(c);
//			String columnTypeNameGen = dbxConn == null ? getColumnTypeName(rsmd, c) : dbxConn.getColumnTypeName(rsmd, c);
			String columnTypeNameGen = getColumnTypeName(_dbmsProductName, rsmd, c);
			
			String columnTypeNameRaw = "-unknown-";
			try {  columnTypeNameRaw = rsmd.getColumnTypeName(c); } catch(SQLException ignore) {}; // sometimes this caused SQLException, especially for 'compute by' 
			int    columnType        = rsmd.getColumnType(c);
//			int    columnDisplaySize = Math.max(rsmd.getColumnDisplaySize(c), rsmd.getColumnLabel(c).length());
			int    columnDisplaySize = Math.max(Math.max(rsmd.getColumnDisplaySize(c), rsmd.getColumnLabel(c).length()), rsmd.getPrecision(c));

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
//					_logger.info("For column '"+columnLabel+"', columnDisplaySize is '"+columnDisplaySize+"', which is above max value of '"+maxDisplaySize+"', using max value. The max value can be changed with java parameter '-DResultSetTableModel.maxDisplaySize=sizeInBytes'. ResultSetTableModel.name='"+getName()+"'");
					_logger.debug("For column '"+columnLabel+"', columnDisplaySize is '"+columnDisplaySize+"', which is above max value of '"+maxDisplaySize+"', using max value. The max value can be changed with java parameter '-DResultSetTableModel.maxDisplaySize=sizeInBytes'. ResultSetTableModel.name='"+getName()+"'");
					columnDisplaySize = maxDisplaySize;
				}
			}

			_rsmdColumnLabel      .add(columnLabel);
			_rsmdColumnName       .add(columnName);
			_rsmdColumnType       .add(new Integer(columnType));
			_rsmdColumnTypeStr    .add(getColumnJavaSqlTypeName(columnType));
			_rsmdColumnClassName  .add(columnClassName);
			_rsmdColumnTypeName   .add(columnTypeNameRaw);
			_rsmdColumnTypeNameStr.add(columnTypeNameGen);
			_displaySize          .add(new Integer(columnDisplaySize));
			_rsmdRefTableName     .add(fullRefTableName);
			
//			System.out.println("name='"+_cols.get(c-1)+"', getColumnClassName("+c+")='"+_type.get(c-1)+"', getColumnTypeName("+c+")='"+_sqlType.get(c-1)+"'.");
		}
		
		// If there is no ResultSet, then this object is just used to get a ResultSet Information String
		// see: public static String getResultSetInfo(ResultSetTableModel rstm)
		if (rs == null)
			return;

		String originProgressState = null;
		if (progress != null)
			originProgressState = progress.getState();

		//---------------------------------------------------
		// Special thing for SQL-Server and JSON column...
		boolean isJsonColumn = false;
		if (_rsmdColumnLabel.size() == 1)
		{
			if (SQLSERVER_JSON_COLUMN_LABEL.equals(_rsmdColumnLabel.get(0)))
				isJsonColumn = true;

			// Set to false if the config is DISABLED 
			if (isJsonColumn && !Configuration.getCombinedConfiguration().getBooleanProperty(PROPKEY_SqlServerJconConcatRows, DEFAULT_SqlServerJconConcatRows))
				isJsonColumn = false;
		}
		if (isJsonColumn)
		{
			_logger.info("Special code path for (SQL-Server) JSON Concat of several rows into a single row... This can be disabled with the property '"+PROPKEY_SqlServerJconConcatRows+"=false'.");
			int rowCount = 0;
			StringBuilder sb = new StringBuilder();
			while(rs.next())
			{
				String str = rs.getString(1);

				if (_logger.isDebugEnabled())
					_logger.debug("JSON Concat row("+rowCount+"), value=|"+str+"|");

				sb.append(str);
				rowCount++;
			}
			rs.close();
			ArrayList<Object> row = new ArrayList<Object>();
			row.add(sb.toString());
			_rows.add(row);
			return;
		}
		// Yes this looks a bit ODD, but lets do it better later
		//---------------------------------------------------
			
		_readCount = 0;
		int rowCount = 0;
		
		long lastProgresUpdate = 0;
		long progresUpdatePeriodMs = 100;
		NumberFormat nf = NumberFormat.getInstance();

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
				
			if ( onlyLastXrows > 0 )
			{
				if (_readCount >= onlyLastXrows)
				{
					if (_discardedXRows < 0)
						_discardedXRows = 0;

					_discardedXRows++;
					
					// Remove "first" row in the returned results
					_rows.remove(0);
				}
			}
				
			_readCount++;
			if (progress != null)
			{
				if ( _readCount < 100 ||  System.currentTimeMillis() - lastProgresUpdate > progresUpdatePeriodMs )
				{
					lastProgresUpdate = System.currentTimeMillis();
					progress.setState(originProgressState + " row " + nf.format(_readCount));
				}
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
				if (o instanceof String && (_stringTrim || _stringRtrim) )
				{
					if (_stringTrim)
						o = ((String)o).trim();
					else
						o = StringUtil.rtrim((String)o);
				}

				// Should we try to convert "faulty" characters to... (if ISO chars has been saved faulty in the DB using UTF8 or similar)
				boolean convertIsEnabled = true;
				if (convertIsEnabled && o instanceof String)
				{
					if (_pipeCmd != null && _pipeCmd.getCmd() instanceof PipeCommandConvert)
					{
						PipeCommandConvert pcc = (PipeCommandConvert) _pipeCmd.getCmd();

						o = pcc.convert(rowCount+1, c, (String) o);
					}
				}

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
		rs.close();

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

	public ResultSetTableModel(ResultSetTableModel rstm, String name, boolean copyRows)
	{
		_rsmdRefTableName      = new ArrayList<String> (rstm._rsmdRefTableName     );  // rsmd.getXXX(c); 
		_rsmdColumnName        = new ArrayList<String> (rstm._rsmdColumnName       );  // rsmd.getColumnName(c); 
		_rsmdColumnLabel       = new ArrayList<String> (rstm._rsmdColumnLabel      );  // rsmd.getColumnLabel(c); 
		_rsmdColumnType        = new ArrayList<Integer>(rstm._rsmdColumnType       ); // rsmd.getColumnType(c); 
		_rsmdColumnTypeStr     = new ArrayList<String> (rstm._rsmdColumnTypeStr    );  // getColumnJavaSqlTypeName(_rsmdColumnType.get(index)) 
		_rsmdColumnTypeName    = new ArrayList<String> (rstm._rsmdColumnTypeName   );  // rsmd.getColumnTypeName(c);
		_rsmdColumnTypeNameStr = new ArrayList<String> (rstm._rsmdColumnTypeNameStr);  // kind of 'SQL' datatype: this.getColumnTypeName(rsmd, c);
		_rsmdColumnClassName   = new ArrayList<String> (rstm._rsmdColumnClassName  );  // rsmd.getColumnClassName(c);
		_displaySize           = new ArrayList<Integer>(rstm._displaySize          ); // Math.max(rsmd.getColumnDisplaySize(c), rsmd.getColumnLabel(c).length());
		_classType             = rstm._classType; // first found class of value, which wasn't null
		_rows                  = new ArrayList<ArrayList<Object>>();
		_readCount             = 0;
		_sqlWarning            = null;
		_allowEdit             = rstm._allowEdit; 
		_name                  = name;
		_pipeCmd               = null;
		_cancelled             = false;
		_abortedAfterXRows     = -1;
		_discardedXRows        = -1;

		_stringRtrim           = rstm._stringRtrim;
		_stringTrim            = rstm._stringTrim;
		_showRowNumber         = rstm._showRowNumber;

		_readResultSetTime     = -1;

		_nullValuesAsEmptyInGetValueAsType = rstm._nullValuesAsEmptyInGetValueAsType;
		
		_toStringTimestampFormat = rstm._toStringTimestampFormat;
		_toStringNumberFormat    = rstm._toStringNumberFormat;
		
		_originSqlText           = rstm._originSqlText;

		_dbmsProductName         = rstm._dbmsProductName;
		_rsmdCached              = rstm._rsmdCached;
		
//		if (copyRows)
//			_rows.addAll(rstm._rows);

		// Deep copy... at least on the RowList level (if the RowList object is "complex/mutable" it might not be a "deep copy" on that specific object)
		if (copyRows)
		{
			for (ArrayList<Object> rowList : rstm._rows)
			{
				_rows.add(new ArrayList<>(rowList));
			}
		}
	}

	/**
	 * Set format (SimpleDateFormat) to format <code>yyyy-MM-dd HH:mm:ss</code> to use when calling toTableString() etc... 
	 * @param format
	 */
	public void setToStringTimestampFormat_YMD_HMS()
	{
		_toStringTimestampFormat = DEFAULT_TimestampToStringFmt_YMD_HMS;
	}
	/**
	 * Set format (SimpleDateFormat) to use when calling toTableString() etc... 
	 * @param format
	 */
	public void setToStringTimestampFormat(String format)
	{
		_toStringTimestampFormat = format;
	}
	/**
	 * Get format (SimpleDateFormat) to use when calling toTableString() etc... 
	 */
	public String getToStringTimestampFormat()
	{
		return _toStringTimestampFormat;
	}
	/** Get a instance of SimpleDateFormat, with the format string set by: setToStringTimestampFormat(fmtStr) */
	public SimpleDateFormat getToStringTimestampSdf()
	{
		String timestampStrFormat = getToStringTimestampFormat();

		SimpleDateFormat sdf = null;
		if (StringUtil.hasValue(timestampStrFormat))
			sdf = new SimpleDateFormat(timestampStrFormat);
		
		return sdf;
	}
	
	/**
	 * Set format (NumberFormat) to use when calling toTableString() etc... 
	 * @param format
	 */
	public void setToStringNumberFormat(boolean useLocal)
	{
		_toStringNumberFormat = useLocal;
	}
	/**
	 * Get format just true/false (NumberFormat) to use when calling toTableString() etc... 
	 */
	public boolean getToStringNumberFormat()
	{
		return _toStringNumberFormat;
	}
	/** Get a instance of NumberFormat (default localization), with the format string set by: setToStringNumberFormat(true|false) */
	public NumberFormat getToStringNumberFormatter()
	{
		if (_toStringNumberFormat)
			return NumberFormat.getInstance();
		
		return null;
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

	    //--------------------------JDBC 4.2 -----------------------------
//		case java.sql.Types.REF_CURSOR:                     return rs.getString(col);
		case java.sql.Types.TIME_WITH_TIMEZONE:             return _timeWithTimezone_to_time           ? rs.getTime(col)      : rs.getString(col);
		case java.sql.Types.TIMESTAMP_WITH_TIMEZONE:        return _timestampWithTimezone_to_timestamp ? rs.getTimestamp(col) : rs.getString(col);
//xxx;         Leta upp alla ställen som vi har ... case: Types.XXXX and check if we handle *_WITH_TIMEZONE
//yyy;         Horizonal Scrollbar on "SQL Text" bootstrap modal popup...
//DONE:   zzz; QsWaitDetails -- Make a new "linebreak" after each 'runtime_stats_interval_id' group
//DONE??  ååå; OsHostMon -- Chart -- add new "line" -- "userPct+sysPct+ioWaitPct"
		
		case -156: // case microsoft.sql.Types.SQL_VARIANT:
		{
			// variant can be many things (possibly all object types), so try to get it in different ways
			// most would works as getString()... but handle special in case of:
			//   - null    <<-- Return as null
			//   - byte[]  <<-- Return as a Hexadecimal string... toString on byte[] wont work, as it returns '[B@405217f8' or similar
			Object rowObj = rs.getObject(col);
			if      (rowObj == null)           return null;
			else if (rowObj instanceof byte[]) return StringUtil.bytesToHex(BINARY_PREFIX, rs.getBytes(col), BINARY_TOUPPER);
			else                               return rs.getString(col);
		}
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

	public boolean wasBottomApplied()
	{
		return _discardedXRows >= 0;
	}
	public int getBottomXRowsDiscarded()
	{
		return _discardedXRows;
	}
    
	/**
	 * Checks if the ResultSetTableModel is <b>mergeable</b> with another ResultSetTableModel
	 * 
	 * @param rstm the ResultSetTableModel to check if it's compatible with the current
	 * @return true if OK
	 */
	public boolean isMergeable(ResultSetTableModel rstm, boolean checkColumnNames)
	{
		// Check column count
		if (getColumnCount() != rstm.getColumnCount())
			return false;
		
		// Check column name differences
		if (checkColumnNames)
		{
			for (int i=0; i<getColumnCount(); i++)
			{
				if ( ! _rsmdColumnLabel.get(i).equals(rstm._rsmdColumnLabel.get(i)) )
					return false;
			}
		}

		// Check column data type differences
		for (int i=0; i<getColumnCount(); i++)
		{
			if ( ! _rsmdColumnTypeStr.get(i).equals(rstm._rsmdColumnTypeStr.get(i)) )
				return false;
		}
		
		return true;
	}

    
	/**
	 * Set new data content for the table model.
	 * 
	 * @param rstm the new Data rows
	 * @param merge If you want to "merge" current data with the one supplied
	 * 
	 * @throws ModelMissmatchException if the data model doesn't has the same structure (number of columns, column names, etc) 
	 */
	public void setModelData(ResultSetTableModel rstm, boolean merge, boolean checkColumnNames)
	throws ModelMissmatchException
	{
		// Check column count
		if (getColumnCount() != rstm.getColumnCount())
			throw new ModelMissmatchException("Column COUNT missmatch. current count="+getColumnCount()+", passed count="+rstm.getColumnCount());
		
		// Check column name differences
		if (checkColumnNames)
		{
			for (int i=0; i<getColumnCount(); i++)
			{
				if ( ! _rsmdColumnLabel.get(i).equals(rstm._rsmdColumnLabel.get(i)) )
					throw new ModelMissmatchException("Column NAME missmatch. current columns="+_rsmdColumnLabel+", passed columns="+rstm._rsmdColumnLabel);
			}
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

	/**
	 * Add/Merge a ResultSetTableModel into the current<br>
	 * NOTE: This will simply call setModelData(rstm, true);
	 * @param rstm
	 * 
	 * @throws 
	 */
	public void add(ResultSetTableModel rstm)
	throws ModelMissmatchException
	{
		setModelData(rstm, true, false);
	}
	public void add(ResultSetTableModel rstm, boolean checkColumnNames )
	throws ModelMissmatchException
	{
		setModelData(rstm, true, checkColumnNames );
	}

	public static String getColumnTypeName(ResultSetMetaData rsmd, int col)
	{
		return getColumnTypeName(null, rsmd, col);
	}
	public static String getColumnTypeName(String dbmsProductName, ResultSetMetaData rsmd, int col)
	{
		String columnTypeName;
		// in RepServer, the getColumnTypeName() throws exception: JZ0SJ ... wants metadata 
		try
		{
			columnTypeName = rsmd.getColumnTypeName(col);

			try
			{
				int columnType = rsmd.getColumnType(col);

				//---------------------------------
				// SQL-Server
				//---------------------------------
				if (DbUtils.isProductName(dbmsProductName, DbUtils.DB_PROD_NAME_MSSQL))
				{
					if (columnType == java.sql.Types.NUMERIC || columnType == java.sql.Types.DECIMAL)
					{
						int precision = rsmd.getPrecision(col);
						int scale     = rsmd.getScale(col);

						columnTypeName += "("+precision+","+scale+")";
					}
					else if (
					        columnType == java.sql.Types.CHAR 
					     || columnType == java.sql.Types.VARCHAR 
					     || columnType == java.sql.Types.BINARY
					     || columnType == java.sql.Types.VARBINARY
					   )
					{
						int columnDisplaySize = Math.max(rsmd.getColumnDisplaySize(col), rsmd.getPrecision(col));
						columnTypeName += (columnDisplaySize >= 2_147_483_647) ? "(max)" : "("+columnDisplaySize+")";
					}
					else if ( columnType == java.sql.Types.NCHAR || columnType == java.sql.Types.NVARCHAR )
					{
						int columnDisplaySize = Math.max(rsmd.getColumnDisplaySize(col), rsmd.getPrecision(col));
						columnTypeName += (columnDisplaySize >= 1_073_741_823) ? "(max)" : "("+columnDisplaySize+")";
					}
				}
				//---------------------------------
				// POSTGRES
				//---------------------------------
				else if (DbUtils.isProductName(dbmsProductName, DbUtils.DB_PROD_NAME_POSTGRES))
				{
					if (columnType == java.sql.Types.NUMERIC || columnType == java.sql.Types.DECIMAL)
					{
						int precision = rsmd.getPrecision(col);
						int scale     = rsmd.getScale(col);
						
						columnTypeName += "("+precision+","+scale+")";
					}
		
					// Binary goes as datatype 'bytea' and does NOT have a length specification
					//if ( columnType == java.sql.Types.BINARY || columnType == java.sql.Types.VARBINARY)
					//{
					//	int columnDisplaySize = Math.max(rsmd.getColumnDisplaySize(col), rsmd.getPrecision(col));
					//		
					//	columnTypeName += (columnDisplaySize == 2147483647) ? "(max)" : "("+columnDisplaySize+")";
					//}
		
					if (    columnType == java.sql.Types.CHAR 
					     || columnType == java.sql.Types.VARCHAR 
					     || columnType == java.sql.Types.NCHAR
					     || columnType == java.sql.Types.NVARCHAR
					   )
					{
						int columnDisplaySize = Math.max(rsmd.getColumnDisplaySize(col), rsmd.getPrecision(col));
							
						columnTypeName += "("+columnDisplaySize+")";
		
						if (columnDisplaySize >= 2_147_483_647)
							columnTypeName = "text";
					}
				}
				//---------------------------------
				// EVERYTHING ELSE
				//---------------------------------
				else
				{
					if (columnType == java.sql.Types.NUMERIC || columnType == java.sql.Types.DECIMAL)
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
						int columnDisplaySize = Math.max(rsmd.getColumnDisplaySize(col), rsmd.getPrecision(col));
						columnTypeName += "("+columnDisplaySize+")";
					}
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
				int columnDisplaySize = Math.max(rsmd.getColumnDisplaySize(col), rsmd.getPrecision(col));
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

		//------------------------- JDBC 4.0 (java 1.6) -----------------------------------
		case java.sql.Types.ROWID:         return "java.sql.Types.ROWID";
		case java.sql.Types.NCHAR:         return "java.sql.Types.NCHAR";
		case java.sql.Types.NVARCHAR:      return "java.sql.Types.NVARCHAR";
		case java.sql.Types.LONGNVARCHAR:  return "java.sql.Types.LONGNVARCHAR";
		case java.sql.Types.NCLOB:         return "java.sql.Types.NCLOB";
		case java.sql.Types.SQLXML:        return "java.sql.Types.SQLXML";

		//------------------------- JDBC 4.2 (java 1.8) -----------------------------------
		case java.sql.Types.REF_CURSOR:              return "java.sql.Types.REF_CURSOR";
		case java.sql.Types.TIME_WITH_TIMEZONE:      return "java.sql.Types.TIME_WITH_TIMEZONE";
		case java.sql.Types.TIMESTAMP_WITH_TIMEZONE: return "java.sql.Types.TIMESTAMP_WITH_TIMEZONE";
//		case 2012:                                   return "java.sql.Types.REF_CURSOR";
//		case 2013:                                   return "java.sql.Types.TIME_WITH_TIMEZONE";
//		case 2014:                                   return "java.sql.Types.TIMESTAMP_WITH_TIMEZONE";
		

		//------------------------- VENDOR SPECIFIC TYPES --------------------------- (grabbed from ojdbc7.jar)
		case -100:                                   return "oracle.jdbc.OracleTypes.TIMESTAMPNS";
		case -101:                                   return "oracle.jdbc.OracleTypes.TIMESTAMPTZ";
		case -102:                                   return "oracle.jdbc.OracleTypes.TIMESTAMPLTZ";
		case -103:                                   return "oracle.jdbc.OracleTypes.INTERVALYM";
		case -104:                                   return "oracle.jdbc.OracleTypes.INTERVALDS";
		case  -10:                                   return "oracle.jdbc.OracleTypes.CURSOR";
		case  -13:                                   return "oracle.jdbc.OracleTypes.BFILE";
		case 2007:                                   return "oracle.jdbc.OracleTypes.OPAQUE";
		case 2008:                                   return "oracle.jdbc.OracleTypes.JAVA_STRUCT";
		case  -14:                                   return "oracle.jdbc.OracleTypes.PLSQL_INDEX_TABLE";
		case  100:                                   return "oracle.jdbc.OracleTypes.BINARY_FLOAT";
		case  101:                                   return "oracle.jdbc.OracleTypes.BINARY_DOUBLE";
//		case    2:                                   return "oracle.jdbc.OracleTypes.NUMBER";             // same as: java.sql.Types.NUMERIC
//		case   -2:                                   return "oracle.jdbc.OracleTypes.RAW";                // same as: java.sql.Types.BINARY
		case  999:                                   return "oracle.jdbc.OracleTypes.FIXED_CHAR";

	    case -155:                                   return "microsoft.sql.DATETIMEOFFSET";
	    case -153:                                   return "microsoft.sql.STRUCTURED";
	    case -151:                                   return "microsoft.sql.DATETIME";
	    case -150:                                   return "microsoft.sql.SMALLDATETIME";
	    case -148:                                   return "microsoft.sql.MONEY";
	    case -146:                                   return "microsoft.sql.SMALLMONEY";
	    case -145:                                   return "microsoft.sql.GUID";
	    case -156:                                   return "microsoft.sql.SQL_VARIANT";
	    case -157:                                   return "microsoft.sql.GEOMETRY";
	    case -158:                                   return "microsoft.sql.GEOGRAPHY";

	    //------------------------- UNHANDLED TYPES  ---------------------------
		default:
			return "unknown-jdbc-datatype("+columnType+")";
		}
	}

	/**
	 * The string representation of "java.sql.Types.INTEGER" -> java.sql.Types.INTEGER the java.sql.Types.XXXX value
	 * @return
	 */
	public static int getColumnJavaSqlTypeNameToInt(String name)
	{
		if ("java.sql.Types.BIT"           .equals(name)) return java.sql.Types.BIT;
		if ("java.sql.Types.TINYINT"       .equals(name)) return java.sql.Types.TINYINT;
		if ("java.sql.Types.SMALLINT"      .equals(name)) return java.sql.Types.SMALLINT;
		if ("java.sql.Types.INTEGER"       .equals(name)) return java.sql.Types.INTEGER;
		if ("java.sql.Types.BIGINT"        .equals(name)) return java.sql.Types.BIGINT;
		if ("java.sql.Types.FLOAT"         .equals(name)) return java.sql.Types.FLOAT;
		if ("java.sql.Types.REAL"          .equals(name)) return java.sql.Types.REAL;
		if ("java.sql.Types.DOUBLE"        .equals(name)) return java.sql.Types.DOUBLE;
		if ("java.sql.Types.NUMERIC"       .equals(name)) return java.sql.Types.NUMERIC;
		if ("java.sql.Types.DECIMAL"       .equals(name)) return java.sql.Types.DECIMAL;
		if ("java.sql.Types.CHAR"          .equals(name)) return java.sql.Types.CHAR;
		if ("java.sql.Types.VARCHAR"       .equals(name)) return java.sql.Types.VARCHAR;
		if ("java.sql.Types.LONGVARCHAR"   .equals(name)) return java.sql.Types.LONGVARCHAR;
		if ("java.sql.Types.DATE"          .equals(name)) return java.sql.Types.DATE;
		if ("java.sql.Types.TIME"          .equals(name)) return java.sql.Types.TIME;
		if ("java.sql.Types.TIMESTAMP"     .equals(name)) return java.sql.Types.TIMESTAMP;
		if ("java.sql.Types.BINARY"        .equals(name)) return java.sql.Types.BINARY;
		if ("java.sql.Types.VARBINARY"     .equals(name)) return java.sql.Types.VARBINARY;
		if ("java.sql.Types.LONGVARBINARY" .equals(name)) return java.sql.Types.LONGVARBINARY;
		if ("java.sql.Types.NULL"          .equals(name)) return java.sql.Types.NULL;
		if ("java.sql.Types.OTHER"         .equals(name)) return java.sql.Types.OTHER;
		if ("java.sql.Types.JAVA_OBJECT"   .equals(name)) return java.sql.Types.JAVA_OBJECT;
		if ("java.sql.Types.DISTINCT"      .equals(name)) return java.sql.Types.DISTINCT;
		if ("java.sql.Types.STRUCT"        .equals(name)) return java.sql.Types.STRUCT;
		if ("java.sql.Types.ARRAY"         .equals(name)) return java.sql.Types.ARRAY;
		if ("java.sql.Types.BLOB"          .equals(name)) return java.sql.Types.BLOB;
		if ("java.sql.Types.CLOB"          .equals(name)) return java.sql.Types.CLOB;
		if ("java.sql.Types.REF"           .equals(name)) return java.sql.Types.REF;
		if ("java.sql.Types.DATALINK"      .equals(name)) return java.sql.Types.DATALINK;
		if ("java.sql.Types.BOOLEAN"       .equals(name)) return java.sql.Types.BOOLEAN;

		//------------------------- JDBC 4.0 -----------------------------------
		if ("java.sql.Types.ROWID"         .equals(name)) return java.sql.Types.ROWID;
		if ("java.sql.Types.NCHAR"         .equals(name)) return java.sql.Types.NCHAR;
		if ("java.sql.Types.NVARCHAR"      .equals(name)) return java.sql.Types.NVARCHAR;
		if ("java.sql.Types.LONGNVARCHAR"  .equals(name)) return java.sql.Types.LONGNVARCHAR;
		if ("java.sql.Types.NCLOB"         .equals(name)) return java.sql.Types.NCLOB;
		if ("java.sql.Types.SQLXML"        .equals(name)) return java.sql.Types.SQLXML;

		//------------------------- JDBC 4.2 -----------------------------------
//		if ("java.sql.Types.REF_CURSOR"             .equals(name)) return java.sql.Types.REF_CURSOR;
//		if ("java.sql.Types.TIME_WITH_TIMEZONE"     .equals(name)) return java.sql.Types.TIME_WITH_TIMEZONE;
//		if ("java.sql.Types.TIMESTAMP_WITH_TIMEZONE".equals(name)) return java.sql.Types.TIMESTAMP_WITH_TIMEZONE;
		if ("java.sql.Types.REF_CURSOR"             .equals(name)) return 2012;
		if ("java.sql.Types.TIME_WITH_TIMEZONE"     .equals(name)) return 2013;
		if ("java.sql.Types.TIMESTAMP_WITH_TIMEZONE".equals(name)) return 2014;
		
		
		//------------------------- VENDOR SPECIFIC TYPES ---------------------------
		if ("oracle.jdbc.OracleTypes.TIMESTAMPNS"       .equals(name)) return -100;
		if ("oracle.jdbc.OracleTypes.TIMESTAMPTZ"       .equals(name)) return -101;
		if ("oracle.jdbc.OracleTypes.TIMESTAMPLTZ"      .equals(name)) return -102;
		if ("oracle.jdbc.OracleTypes.INTERVALYM"        .equals(name)) return -103;
		if ("oracle.jdbc.OracleTypes.INTERVALDS"        .equals(name)) return -104;
		if ("oracle.jdbc.OracleTypes.CURSOR"            .equals(name)) return  -10;
		if ("oracle.jdbc.OracleTypes.BFILE"             .equals(name)) return  -13;
		if ("oracle.jdbc.OracleTypes.OPAQUE"            .equals(name)) return 2007;
		if ("oracle.jdbc.OracleTypes.JAVA_STRUCT"       .equals(name)) return 2008;
		if ("oracle.jdbc.OracleTypes.PLSQL_INDEX_TABLE" .equals(name)) return  -14;
		if ("oracle.jdbc.OracleTypes.BINARY_FLOAT"      .equals(name)) return  100;
		if ("oracle.jdbc.OracleTypes.BINARY_DOUBLE"     .equals(name)) return  101;
		if ("oracle.jdbc.OracleTypes.NUMBER"            .equals(name)) return  java.sql.Types.NUMERIC;
		if ("oracle.jdbc.OracleTypes.RAW"               .equals(name)) return  java.sql.Types.BINARY;
		if ("oracle.jdbc.OracleTypes.FIXED_CHAR"        .equals(name)) return  999;

		if ("microsoft.sql.DATETIMEOFFSET"              .equals(name)) return -155;
		if ("microsoft.sql.STRUCTURED"                  .equals(name)) return -153;
		if ("microsoft.sql.DATETIME"                    .equals(name)) return -151;
		if ("microsoft.sql.SMALLDATETIME"               .equals(name)) return -150;
		if ("microsoft.sql.MONEY"                       .equals(name)) return -148;
		if ("microsoft.sql.SMALLMONEY"                  .equals(name)) return -146;
		if ("microsoft.sql.GUID"                        .equals(name)) return -145;
		if ("microsoft.sql.SQL_VARIANT"                 .equals(name)) return -156;
		if ("microsoft.sql.GEOMETRY"                    .equals(name)) return -157;
		if ("microsoft.sql.GEOGRAPHY"                   .equals(name)) return -158;

		//------------------------- UNHANDLED TYPES  ---------------------------
		_logger.warn("The string JDBC Datatype '"+name+"' is unknown, returning Integer.MIN_VALUE = "+ Integer.MIN_VALUE);
		return Integer.MIN_VALUE;
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

	/** Can for example be used to quote, a value if it looks like a string */
	public static boolean shouldBeQuoted(int columnType)
	{
		switch (columnType)
		{
		case java.sql.Types.BIT:           return false;
		case java.sql.Types.TINYINT:       return false;
		case java.sql.Types.SMALLINT:      return false;
		case java.sql.Types.INTEGER:       return false;
		case java.sql.Types.BIGINT:        return false;
		case java.sql.Types.FLOAT:         return false;
		case java.sql.Types.REAL:          return false;
		case java.sql.Types.DOUBLE:        return false;
		case java.sql.Types.NUMERIC:       return false;
		case java.sql.Types.DECIMAL:       return false;
		case java.sql.Types.CHAR:          return true;
		case java.sql.Types.VARCHAR:       return true;
		case java.sql.Types.LONGVARCHAR:   return true;
		case java.sql.Types.DATE:          return true;
		case java.sql.Types.TIME:          return true;
		case java.sql.Types.TIMESTAMP:     return true;
		case java.sql.Types.BINARY:        return false;
		case java.sql.Types.VARBINARY:     return false;
		case java.sql.Types.LONGVARBINARY: return false;
		case java.sql.Types.NULL:          return false;
		case java.sql.Types.OTHER:         return false;
		case java.sql.Types.JAVA_OBJECT:   return true;
		case java.sql.Types.DISTINCT:      return false;
		case java.sql.Types.STRUCT:        return false;
		case java.sql.Types.ARRAY:         return false;
		case java.sql.Types.BLOB:          return false;
		case java.sql.Types.CLOB:          return true;
		case java.sql.Types.REF:           return false;
		case java.sql.Types.DATALINK:      return false;
		case java.sql.Types.BOOLEAN:       return false;

		//------------------------- JDBC 4.0 (java 1.6) -----------------------------------
		case java.sql.Types.ROWID:         return false;
		case java.sql.Types.NCHAR:         return true;
		case java.sql.Types.NVARCHAR:      return true;
		case java.sql.Types.LONGNVARCHAR:  return true;
		case java.sql.Types.NCLOB:         return true;
		case java.sql.Types.SQLXML:        return false;

		//------------------------- JDBC 4.2 (java 1.8) -----------------------------------
//		case java.sql.Types.REF_CURSOR:              return false;
//		case java.sql.Types.TIME_WITH_TIMEZONE:      return false;
//		case java.sql.Types.TIMESTAMP_WITH_TIMEZONE: return false;
		case 2012:                                   return false;
		case 2013:                                   return true;
		case 2014:                                   return true;
		

		//------------------------- VENDOR SPECIFIC TYPES --------------------------- (grabbed from ojdbc7.jar)
		case -100:                         return true;  // "oracle.jdbc.OracleTypes.TIMESTAMPNS";
		case -101:                         return true;  // "oracle.jdbc.OracleTypes.TIMESTAMPTZ";
		case -102:                         return true;  // "oracle.jdbc.OracleTypes.TIMESTAMPLTZ";
		case -103:                         return true;  // "oracle.jdbc.OracleTypes.INTERVALYM";
		case -104:                         return true;  // "oracle.jdbc.OracleTypes.INTERVALDS";
		case  -10:                         return false; // "oracle.jdbc.OracleTypes.CURSOR";
		case  -13:                         return false; // "oracle.jdbc.OracleTypes.BFILE";
		case 2007:                         return false; // "oracle.jdbc.OracleTypes.OPAQUE";
		case 2008:                         return false; // "oracle.jdbc.OracleTypes.JAVA_STRUCT";
		case  -14:                         return false; // "oracle.jdbc.OracleTypes.PLSQL_INDEX_TABLE";
		case  100:                         return false; // "oracle.jdbc.OracleTypes.BINARY_FLOAT";
		case  101:                         return false; // "oracle.jdbc.OracleTypes.BINARY_DOUBLE";
//		case    2:                         return false; // "oracle.jdbc.OracleTypes.NUMBER";             // same as: java.sql.Types.NUMERIC
//		case   -2:                         return false; // "oracle.jdbc.OracleTypes.RAW";                // same as: java.sql.Types.BINARY
		case  999:                         return true;  // "oracle.jdbc.OracleTypes.FIXED_CHAR";

	    case -155:                         return true ; //  "microsoft.sql.DATETIMEOFFSET";
	    case -153:                         return false; //  "microsoft.sql.STRUCTURED";
	    case -151:                         return true ; //  "microsoft.sql.DATETIME";
	    case -150:                         return true ; //  "microsoft.sql.SMALLDATETIME";
	    case -148:                         return false; //  "microsoft.sql.MONEY";
	    case -146:                         return false; //  "microsoft.sql.SMALLMONEY";
	    case -145:                         return true ; //  "microsoft.sql.GUID";
	    case -156:                         return false; //  "microsoft.sql.SQL_VARIANT";
	    case -157:                         return false; //  "microsoft.sql.GEOMETRY";
	    case -158:                         return false; //  "microsoft.sql.GEOGRAPHY";

	    //------------------------- UNHANDLED TYPES  ---------------------------
		default:
			return false;
		}
	}

	/** Transforms a JDBC Type to a java class */
	public static Class<?> getJavaClassFromJdbcType(int jdbcType)
	{
		switch (jdbcType)
		{
		case java.sql.Types.BIT:           return Boolean.class;
		case java.sql.Types.TINYINT:       return Integer.class;
		case java.sql.Types.SMALLINT:      return Integer.class;
		case java.sql.Types.INTEGER:       return Integer.class;
		case java.sql.Types.BIGINT:        return Long.class;
		case java.sql.Types.FLOAT:         return Double.class;
		case java.sql.Types.REAL:          return Float.class;
		case java.sql.Types.DOUBLE:        return Double.class;
		case java.sql.Types.NUMERIC:       return BigDecimal.class;
		case java.sql.Types.DECIMAL:       return BigDecimal.class;
		case java.sql.Types.CHAR:          return String.class;
		case java.sql.Types.VARCHAR:       return String.class;
		case java.sql.Types.LONGVARCHAR:   return String.class;
		case java.sql.Types.DATE:          return Date.class;
		case java.sql.Types.TIME:          return Time.class;
		case java.sql.Types.TIMESTAMP:     return Timestamp.class;
		case java.sql.Types.BINARY:        return byte[].class;
		case java.sql.Types.VARBINARY:     return byte[].class;
		case java.sql.Types.LONGVARBINARY: return byte[].class;
		case java.sql.Types.NULL:          return Object.class;
		case java.sql.Types.OTHER:         return Object.class;
		case java.sql.Types.JAVA_OBJECT:   return byte[].class;
		case java.sql.Types.DISTINCT:      return Object.class;
		case java.sql.Types.STRUCT:        return Object.class;
		case java.sql.Types.ARRAY:         return Object.class;
		case java.sql.Types.BLOB:          return byte[].class;
		case java.sql.Types.CLOB:          return String.class;
		case java.sql.Types.REF:           return Object.class;
		case java.sql.Types.DATALINK:      return Object.class;
		case java.sql.Types.BOOLEAN:       return Boolean.class;

		//------------------------- JDBC 4.0 (java 1.6) -----------------------------------
		case java.sql.Types.ROWID:         return Object.class;
		case java.sql.Types.NCHAR:         return String.class;
		case java.sql.Types.NVARCHAR:      return String.class;
		case java.sql.Types.LONGNVARCHAR:  return String.class;
		case java.sql.Types.NCLOB:         return String.class;
		case java.sql.Types.SQLXML:        return String.class;

		//------------------------- JDBC 4.2 (java 1.8) -----------------------------------
//		case java.sql.Types.REF_CURSOR:              return false;
//		case java.sql.Types.TIME_WITH_TIMEZONE:      return false;
//		case java.sql.Types.TIMESTAMP_WITH_TIMEZONE: return false;
		case 2012:                                   return Object.class;
		case 2013:                                   return Object.class;
		case 2014:                                   return Object.class;
		

		//------------------------- VENDOR SPECIFIC TYPES --------------------------- (grabbed from ojdbc7.jar)
		case -100:                         return Object.class; // "oracle.jdbc.OracleTypes.TIMESTAMPNS";
		case -101:                         return Object.class; // "oracle.jdbc.OracleTypes.TIMESTAMPTZ";
		case -102:                         return Object.class; // "oracle.jdbc.OracleTypes.TIMESTAMPLTZ";
		case -103:                         return Object.class; // "oracle.jdbc.OracleTypes.INTERVALYM";
		case -104:                         return Object.class; // "oracle.jdbc.OracleTypes.INTERVALDS";
		case  -10:                         return Object.class; // "oracle.jdbc.OracleTypes.CURSOR";
		case  -13:                         return Object.class; // "oracle.jdbc.OracleTypes.BFILE";
		case 2007:                         return Object.class; // "oracle.jdbc.OracleTypes.OPAQUE";
		case 2008:                         return Object.class; // "oracle.jdbc.OracleTypes.JAVA_STRUCT";
		case  -14:                         return Object.class; // "oracle.jdbc.OracleTypes.PLSQL_INDEX_TABLE";
		case  100:                         return Object.class; // "oracle.jdbc.OracleTypes.BINARY_FLOAT";
		case  101:                         return Object.class; // "oracle.jdbc.OracleTypes.BINARY_DOUBLE";
//		case    2:                         return false; // "oracle.jdbc.OracleTypes.NUMBER";             // same as: java.sql.Types.NUMERIC
//		case   -2:                         return false; // "oracle.jdbc.OracleTypes.RAW";                // same as: java.sql.Types.BINARY
		case  999:                         return Object.class;  // "oracle.jdbc.OracleTypes.FIXED_CHAR";

	    case -155:                         return Object.class; //  "microsoft.sql.DATETIMEOFFSET";
	    case -153:                         return Object.class; //  "microsoft.sql.STRUCTURED";
	    case -151:                         return Timestamp.class; //  "microsoft.sql.DATETIME";
	    case -150:                         return Timestamp.class; //  "microsoft.sql.SMALLDATETIME";
	    case -148:                         return BigDecimal.class; //  "microsoft.sql.MONEY";
	    case -146:                         return BigDecimal.class; //  "microsoft.sql.SMALLMONEY";
	    case -145:                         return Object.class; //  "microsoft.sql.GUID";
	    case -156:                         return Object.class; //  "microsoft.sql.SQL_VARIANT";
	    case -157:                         return Object.class; //  "microsoft.sql.GEOMETRY";
	    case -158:                         return Object.class; //  "microsoft.sql.GEOGRAPHY";

	    //------------------------- UNHANDLED TYPES  ---------------------------
		default:
			return Object.class;
		}
	}
	

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

	/** 
	 * Remove all rows except the first # rows 
	 * @return number of rows deleted;
	 */
	public int setRowCount(int newRowCount)
	{
		int cnt = 0;

		while (_rows.size() > newRowCount)
		{
			removeRow(_rows.size()-1);
			cnt++;
		}
		
		return cnt;
	}

	/** @return true if number of rows is 0 */
	public boolean isEmpty()
	{
		return _rows.size() == 0;
	}

	/** @return true if number of rows is greater than 0 */
	public boolean hasRows()
	{
		return _rows.size() > 0;
	}

	@Override
	public String getColumnName(int column)
	{
		return (String)_rsmdColumnLabel.get(column);
	}

	public List<String> getColumnNames()
	{
		return _rsmdColumnLabel;
	}
	
	public String getRsmdReferencedTableName(int col)
	{
		return _rsmdRefTableName.get(col);	
	}
	public List<String> getRsmdReferencedTableNames()
	{
		ArrayList<String> uniqueTables = new ArrayList<>();
		for (String name : _rsmdRefTableName)
			if ( ! uniqueTables.contains(name) )
				uniqueTables.add(name);
		return uniqueTables;
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

//		if (_rsmdColumnType.get(colid) == Types.TIMESTAMP)
//			clazz = Timestamp.class;
//System.out.println("getColumnClass(colid="+colid+"): <<< "+clazz);

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

	public void setValueAtWithOverride(Object value, int r, int c)
	{
		ArrayList<Object> row = _rows.get(r);
		if (row != null)
			row.set(c, value);
		else
			_logger.warn("setValueAtWithOverride::The row returned NULL. (row="+r+", col="+c+", val='"+value+"')");
	}

//	public void addTableModelListener(TableModelListener l)
//	{
//	}
//
//	public void removeTableModelListener(TableModelListener l)
//	{
//	}

	

//	public String getDbmsDdlDataTypeTargetResolved(int index)
//	{
//		int col = index + 1;
//		if (_dbmsDdlResolver == null)
//		{
//			_dbmsDdlResolver = DbxConnection.createDbmsDdlResolver(_dbmsProductName);
//		}
//		
////		try 
////		{
//			int javaSqlType = _rsmdColumnType.get(index);
////			int length      = Math.max(_rsmdCached.getColumnDisplaySize(col), _rsmdCached.getPrecision(col));
//			int length      = _rsmdCached.getPrecision(col);
//			int scale       = _rsmdCached.getScale(col);
//
//			if (length <= 0)
//			{
////				length = _rsmdCached.getColumnDisplaySize(col);
//				switch (javaSqlType)
//				{
//				case Types.CHAR:
//				case Types.LONGNVARCHAR:
//				case Types.LONGVARBINARY:
//				case Types.LONGVARCHAR:
//				case Types.NCHAR:
//				case Types.NVARCHAR:
//				case Types.VARBINARY:
//				case Types.VARCHAR:
//					length = _rsmdCached.getColumnDisplaySize(col);
//					break;
//
//				default:
//					break;
//				}
//			}
//			
//			return _dbmsDdlResolver.dataTypeResolverTarget(javaSqlType, length, scale);
////		}
////		catch (SQLException ex)
////		{
////			return ex.toString();
////		}
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

//		if (_showRowNumber)
//			index--;

		StringBuilder sb = new StringBuilder();
		sb.append("<HTML>");
		
		sb.append("Below is information about the Column based on the JDBC ResultSetMetaData object.<br>");
		sb.append("<TABLE ALIGN=\"left\" BORDER=1 CELLSPACING=1 CELLPADDING=1 WIDTH=\"100%\">");

		sb.append("<TR ALIGN=\"left\" VALIGN=\"middle\"> <TH>Description               </TH> <TH>From Java Method                     </TH> <TH>Value</TH> </TR>");

		sb.append("<TR ALIGN=\"left\" VALIGN=\"middle\"> <TD>Label                     </TD> <TD>rsmd.getColumnLabel()                </TD> <TD>").append(_rsmdColumnLabel.get(index)      ).append("</TD> </TR>");
		sb.append("<TR ALIGN=\"left\" VALIGN=\"middle\"> <TD>Name                      </TD> <TD>rsmd.getColumnName()                 </TD> <TD>").append(_rsmdColumnName.get(index)       ).append("</TD> </TR>");
		sb.append("<TR ALIGN=\"left\" VALIGN=\"middle\"> <TD>JDBC Type Name            </TD> <TD>rsmd.getColumnType()<b>->String</b>  </TD> <TD>").append(_rsmdColumnTypeStr.get(index)    ).append("</TD> </TR>");
		sb.append("<TR ALIGN=\"left\" VALIGN=\"middle\"> <TD>JDBC Type Number          </TD> <TD>rsmd.getColumnType()                 </TD> <TD>").append(_rsmdColumnType.get(index)       ).append("</TD> </TR>");
		sb.append("<TR ALIGN=\"left\" VALIGN=\"middle\"> <TD>Java Class Name           </TD> <TD>rsmd.getColumnClassName()            </TD> <TD>").append(_rsmdColumnClassName.get(index)  ).append("</TD> </TR>");
		sb.append("<TR ALIGN=\"left\" VALIGN=\"middle\"> <TD>Raw DBMS Datatype         </TD> <TD>rsmd.getColumnTypeName()             </TD> <TD>").append(_rsmdColumnTypeName.get(index)   ).append("</TD> </TR>");
		sb.append("<TR ALIGN=\"left\" VALIGN=\"middle\"> <TD>Guessed DBMS Type         </TD> <TD>rsmd.getColumnTypeName()<b>+size</b> </TD> <TD>").append(_rsmdColumnTypeNameStr.get(index)).append("</TD> </TR>");
//		sb.append("<TR ALIGN=\"left\" VALIGN=\"middle\"> <TD>DDL Resolver DBMS Type    </TD> <TD>DbmsDdlResolver.dataTypeResolver()   </TD> <TD>").append(getDbmsDdlDataTypeTargetResolved(index)).append("</TD> </TR>");
		sb.append("<TR ALIGN=\"left\" VALIGN=\"middle\"> <TD>TableModel Class Name     </TD> <TD>TableModel.getColumnClass()          </TD> <TD>").append(getColumnClass(index)            ).append("</TD> </TR>");
//		sb.append("<TR ALIGN=\"left\" VALIGN=\"middle\"> <TD>Display size max(disp/len)</TD> <TD>_displaySize                         </TD> <TD>").append(_displaySize.get(index)          ).append("</TD> </TR>");
		sb.append("<TR ALIGN=\"left\" VALIGN=\"middle\"> <TD>Table Name                </TD> <TD>rsmd.getTableName()                  </TD> <TD>").append(_rsmdRefTableName.get(index)     ).append("</TD> </TR>");
		
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
//		          header1.add("DDL Resolver DBMS Type");      header2.add("DbmsDdlResolver.dataTypeResolver()");
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

//			int pos = (_showRowNumber ? c-1 : c);
			int pos = c;
			
			        row.add( "" + (_showRowNumber ? c : c+1) ); // _showRowNumber == skip first column... 
			        row.add( "" + _rsmdColumnLabel      .get(pos) );
			if (ld) row.add( "" + _rsmdColumnName       .get(pos)  );
			        row.add( "" + _rsmdColumnTypeStr    .get(pos) );
			if (ld) row.add( "" + _rsmdColumnType       .get(pos) );
			if (ld) row.add( "" + _rsmdColumnClassName  .get(pos) );
			if (ld) row.add( "" + _rsmdColumnTypeName   .get(pos) );
			        row.add( "" + _rsmdColumnTypeNameStr.get(pos) );
//			        row.add( "" + getDbmsDdlDataTypeTargetResolved(pos) );
			        row.add( "" + _rsmdRefTableName     .get(pos) );
			if (ld) row.add( "" + getColumnClass(pos) );
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

	public String getGuessedDbmsDatatype(int column)
	{
		return _rsmdColumnTypeNameStr.get(column);
	}

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

		String str = toString(o, "NULL");
//		String str;
//		if (o == null)
//			str = "NULL";//return "(NULL)";
//		else
//			str = o.toString(); // Convert it to a string

		int fullSize = getColumnDisplaySize(c);
		
		if (o instanceof Number)
		{
			if (o != null)
			{
				NumberFormat nf = NumberFormat.getInstance();
				str = nf.format(o);
			}
			
			return StringUtil.right(str, fullSize);
		}
		else
		{
			return StringUtil.left(str, fullSize);
		}
	}

//	public static Object tableToString(ResultSetTableModel tm)
//	{
//		// TODO Auto-generated method stub
//		return null;
//	}

	/**
	 * Print all columns for a specific row
	 * 
	 * @param row Row number (starting at 0)
	 * @return
	 */
	public String toStringRow(int row)
	{
		if (getRowCount() == 0)
			return "Table has 0 rows";
		if (row >= getRowCount())
			return "Table has "+getRowCount()+" rows, and you wanted to look at row "+row;

		StringBuilder sb = new StringBuilder();
		
		ArrayList<Object> rowObj = _rows.get(row);
		for (int c=0; c<rowObj.size(); c++)
		{
			Object  colObj  = rowObj.get(c);
			String  strVal  = toString(colObj, NULL_REPLACE);
			String  colName = _rsmdColumnLabel.get(c);
			int     colType = _rsmdColumnType.get(c);
			
			String quote = shouldBeQuoted(colType) ? "'" : "";

			if (colObj == null)
			{
				quote = "";
				//colObj = NULL_REPLACE;
			}
			
//			sb.append(colName).append("=").append(quote).append(colObj).append(quote).append(", ");
			sb.append(colName).append("=").append(quote).append(strVal).append(quote).append(", ");
		}
		if (sb.length() > 2)
			sb.delete(sb.length()-2, sb.length());
		
		return sb.toString();
	}
	
	/**
	 * Create a ASCII Table
	 * @return
	 */
	public String toAsciiTableString()
	{
		return SwingUtils.tableToString(this);
	}

	public String toAsciiTableString(String newLineWhenValueChangesForColName)
	{
		return SwingUtils.tableToString(this, newLineWhenValueChangesForColName);
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

	/**
	 * Return a text string with one table for each row. The Table will only have 2 columns. 1=Column Name, 2=Column Value
	 * @return
	 */
	public String toTablesVerticalString() //toAsciiTablesVerticalString()
	{
		StringBuilder sb = new StringBuilder(1024);

		int cols = getColumnCount();
		int rows = getRowCount();
		
		String ColHead = "Column Name";
		int colNameSizeMax = ColHead.length();
		for (int c=0; c<cols; c++)
			colNameSizeMax = Math.max(colNameSizeMax, getColumnName(c).length());
		
		for (int r=0; r<rows; r++)
		{
			sb.append("\n");
			sb.append("Row: ").append(r+1).append(" (").append(rows).append(")").append("\n");
			
			sb.append(StringUtil.left(ColHead, colNameSizeMax)).append(": ").append("Column Value").append("\n");
			sb.append(StringUtil.replicate("-", colNameSizeMax)).append(": ").append(StringUtil.replicate("-", 50)).append("\n");
			
			// build all data rows...
			for (int c=0; c<cols; c++)
			{
				String colName = getColumnName(c);
				Object objVal  = getValueAt(r,c);
				String strVal  = toString(objVal, "NULL");
//				String strVal = "";
//				if (objVal != null)
//				{
//					strVal = objVal.toString();
//				}
//				else
//					strVal = "NULL";

				sb.append(StringUtil.left(colName, colNameSizeMax)).append(": ");
				sb.append(strVal).append("\n");
//				sb.append("    <td").append(tBodyNoWrapStr).append("><b>").append(colName).append("</b></td>\n");
//				sb.append("    <td").append(tBodyNoWrapStr).append(">").append(strVal).append("</td>\n");
			}
		}
		
		return sb.toString();
	}

//test the DailyReport with a connection to MAXM_DW
	public String toAsciiTablesVerticalString()
	{
		try
		{
			return toAsciiTablesVerticalString_fixmeCanCause_OutOfMemoryError();
		}
		catch (RuntimeException rte)
		{
			_logger.error("Problems in: toAsciiTablesVerticalString()", rte);
			return "Problems in: toAsciiTablesVerticalString()";
		}
	}
	private String toAsciiTablesVerticalString_fixmeCanCause_OutOfMemoryError()
	{
		StringBuilder sb = new StringBuilder(1024);

		int cols = getColumnCount();
		int rows = getRowCount();
		
		String[] colArr = {"Column Name", "Column Value"};
		for (int r=0; r<rows; r++)
		{
			DefaultTableModel tm = new DefaultTableModel(colArr, 0);
			
			sb.append("\n");
			sb.append("Row: ").append(r+1).append(" (").append(rows).append(")").append("\n");

			// build all data rows...
			for (int c=0; c<cols; c++)
			{
				String colName = getColumnName(c);
				Object objVal  = getValueAt(r,c);
				String strVal = toString(objVal, "NULL");
//				String strVal = "";
//				if (objVal != null)
//				{
//					strVal = objVal.toString();
//				}
//				else
//					strVal = "NULL";

				String[] row = new String[2];
				row[0] = colName;
				row[1] = strVal;
				tm.addRow(row);
			}

			sb.append(SwingUtils.tableToString(tm, false));
		}
		
		return sb.toString();
	}

	/** renderer used in toHtmlTableString() */
	public interface TableStringRenderer
	{
		/** called to create Attributes for any TABLE attribute */
		default String tagTableAttr(ResultSetTableModel rstm)
		{
			return "border='1'";
		}

		/** called to create Attributes for any TR-TableData attribute */
		default String tagTrAttr(ResultSetTableModel rstm, int row)
		{
			return null;
		}

		/** called to create Attributes for any TH-TableHead attribute */
		default String tagThAttr(ResultSetTableModel rstm, int col, String colName, boolean nowrapPreferred)
		{
			return nowrapPreferred ? "nowrap" : null;
		}

		/** called to create Attributes for any TD-TableData attribute */
		default String tagTdAttr(ResultSetTableModel rstm, int row, int col, String colName, Object objVal, String strVal, boolean nowrapPreferred)
		{
			return nowrapPreferred ? "nowrap" : null;
		}

		/** returns a string that should be used as a value, this could be decorated with HTML tags to highlight various parts of the table cells */
		default String cellValue(ResultSetTableModel rstm, int row, int col, String colName, Object objVal, String strVal)
		{
			return strVal;
		}

		/** returns a string that should be used as a cell tool tip, for example translate milliseconds into a readable string */
		default String cellToolTip(ResultSetTableModel rstm, int row, int col, String colName, Object objVal, String strVal)
		{
			return null;
		}
	}
	
	public String toHtmlTableString(String className)
	{
		return toHtmlTableString(className, true, true, null, null);
	}
	public String toHtmlTableString(String className, Map<String, String> colNameValueTagMap)
	{
		return toHtmlTableString(className, true, true, colNameValueTagMap, null);
	}
	public String toHtmlTableString(String className, boolean tHeadNoWrap, boolean tBodyNoWrap)
	{
		return toHtmlTableString(className, tHeadNoWrap, tBodyNoWrap, null, null);
	}
	public String toHtmlTableString(String className, boolean tHeadNoWrap, boolean tBodyNoWrap, Map<String, String> colNameValueTagMap, TableStringRenderer tsRenderer)
	{
		StringBuilder sb = new StringBuilder(1024);

		// Use renderer to set <table ATTRIBUTES>
		String tableAttr = tsRenderer == null ? "" : tsRenderer.tagTableAttr(this);
		if (tableAttr == null)
			tableAttr = "";

		// If tableAttr holds any 'class' information, merge in the method parameter 'className'
		if (StringUtil.hasValue(className))
		{
			int pos = tableAttr.indexOf("class=");
			if (pos != -1)
			{
				List<String> list = StringUtil.splitOnWhitespaceRespectQuotes(tableAttr);
				tableAttr = "";
				for (String entry : list)
				{
					if (entry.startsWith("class="))
					{
						// inject the methods parameter 'className' into the: class='METHOD_PARAMETER_className_GOES_HERE CLASS_NAMES_THAT_WE_PREVIOUSLY_HAD_GOES_HERE'
						String tmp = entry.substring("class=".length()+1, entry.length()-1);
						entry = "class='" + className + " " + tmp + "'";
					}
					tableAttr += " " + entry;
				}
			}
			else
			{
				tableAttr += " class='" + className + "'";
			}
		}
		// If we have attribute it has to start with a space " "
		if (StringUtil.hasValue(tableAttr) && !tableAttr.startsWith(" "))
			tableAttr = " " + tableAttr;

		// TABLE start
		sb.append("<table").append(tableAttr).append(">\n");

		// Get column and row count
		int cols = getColumnCount();
		int rows = getRowCount();
		
		// build header names
		sb.append("<thead>\n");
		sb.append("<tr>\n");
		for (int c=0; c<cols; c++)
		{
			String colName = getColumnName(c);
			String tooltip = "";
			if (hasColumnDescriptions())
			{
				String colDesc = getColumnDescription(colName);
				if (colDesc != null)
					colDesc = colDesc.replace("'", "&apos;");
				tooltip = " title='" + colDesc + "'";
			}

			// Use renderer to set <th ATTRIBUTES>
			String thAttr = tsRenderer == null ? "" : tsRenderer.tagThAttr(this, c, colName, tHeadNoWrap);
			if (thAttr == null)
				thAttr = "";

			// If we have attribute it has to start with a space " "
			if (StringUtil.hasValue(thAttr) && !thAttr.startsWith(" "))
				thAttr = " " + thAttr;

			sb.append("<th").append(thAttr).append(tooltip).append(">").append(colName).append("</th>\n");
		}
		sb.append("</tr>\n");
		sb.append("</thead>\n");

		// build all data rows...
		sb.append("<tbody>\n");
		for (int r=0; r<rows; r++)
		{
			// Use renderer to set <td ATTRIBUTES>  ... for example to set the row background: <tr style='background-color:#7f96ff;color:#ffffff;'>
			//                                                                                     ^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^
			String trAttr = tsRenderer == null ? "" : tsRenderer.tagTrAttr(this, r);
			if (trAttr == null)
				trAttr = "";
			else
			{
				// Add " " (space) if the attribute doesn't start with it!
				if ( StringUtil.hasValue(trAttr) && ! trAttr.startsWith(" "))
					trAttr = " " + trAttr;
			}

//			sb.append("  <tr>\n");
			sb.append("  <tr").append(trAttr).append("> \n");
			for (int c=0; c<cols; c++)
			{
				Object objVal  = getValueAt(r,c);
				String strVal  = toString(objVal, "&nbsp;", "&nbsp;");
				String toolTip = null;
//				String strVal = "";
//				if (objVal != null)
//				{
//					strVal = objVal.toString();
//					
//					if (objVal instanceof Timestamp && tsSdf != null)
//					{
//						Timestamp ts = (Timestamp) objVal;
//						strVal = tsSdf.format(ts);
//					}
//				}
//				if (StringUtil.isNullOrBlank(strVal))
//					strVal = "&nbsp;";

				String colName = getColumnName(c);
				String beginTag = "";
				String endTag   = "";
				
				String tdAlignRight = "";
				if (objVal != null && objVal instanceof Number)
					tdAlignRight = " align='right'";
				
				if (colNameValueTagMap != null && colNameValueTagMap.containsKey(colName))
				{
					String tagName = colNameValueTagMap.get(colName);
					if (StringUtil.hasValue(tagName))
					{
						beginTag = "<"  + tagName + ">";
						endTag   = "</" + tagName + ">";
					}
				}
				
				if (tsRenderer != null)
				{
					strVal  = tsRenderer.cellValue  (this, r, c, colName, objVal, strVal);
					toolTip = tsRenderer.cellToolTip(this, r, c, colName, objVal, strVal);
				}

				// Is part of "highlighted" sorted column (mark them as "bold" font)
				if (isHighlightSortColumn(colName))
					strVal = renderHighlightSortColumnForHtml(colName, strVal);
				
				// Set the tool tip value (if we have any)
				toolTip = toolTip == null ? "" : " title='" + toolTip + "'";

				// Use renderer to set <td ATTRIBUTES>
				String tdAttr = tsRenderer == null ? "" : tsRenderer.tagTdAttr(this, r, c, colName, objVal, strVal, tBodyNoWrap);
				if (tdAttr == null)
					tdAttr = "";

				// If we have attribute it has to start with a space " "
				if (StringUtil.hasValue(tdAttr) && !tdAttr.startsWith(" "))
					tdAttr = " " + tdAttr;

//				sb.append("<td").append(tBodyNoWrapStr).append(">").append(strVal).append("</td>");
				sb.append("    <td").append(tdAlignRight).append(tdAttr).append(toolTip).append(">").append(beginTag).append(strVal).append(endTag).append("</td>\n");
			}
			sb.append("  </tr>\n");
			//sb.append("\n");
		}
		sb.append("</tbody>\n");
		sb.append("</table>\n");

		return sb.toString();
	}

	/** Is column part of the sorted columns list, which should be highlighted */
	public boolean isHighlightSortColumn(String colName)
	{
		if (_highlightSortColumnsList == null)
			return false;
		
		return _highlightSortColumnsList.contains(colName);
	}

	/** List of columns we should "highlight" as sorted when we create a HTML Table */
	private List<String> _highlightSortColumnsList;

	/** Set column(s) that is part of the sorted columns and should should be highlighted */
	public void setHighlightSortColumns(String... colNames)
	{
		_highlightSortColumnsList = Arrays.asList(colNames);
	}

	/** Set column(s) that is part of the sorted columns and should should be highlighted */
	public List<String> getHighlightSortColumns()
	{
		if (_highlightSortColumnsList == null)
			return Collections.emptyList();

		return _highlightSortColumnsList;
	}

	/** Implementation of HOW to highlight any sorted column */
	public String renderHighlightSortColumnForHtml(String colName, String strVal)
	{
		return "<b>" + strVal + "</b>";
	}


	public String toString(Object objVal)
	{
		return toString(objVal, null, "");
	}
	public String toString(Object objVal, String nullReplace)
	{
		return toString(objVal, nullReplace, "");
	}
	public String toString(Object objVal, String nullReplace, String blankReplace)
	{
		String strVal = null;
		if (objVal != null)
		{
			if (objVal instanceof Timestamp)
			{
				SimpleDateFormat sdf = getToStringTimestampSdf();
				if (sdf != null)
				{
					strVal = sdf.format( (Timestamp) objVal );
				}
			}
			else if (objVal instanceof Number)
			{
				NumberFormat nf = getToStringNumberFormatter();
				if (nf != null)
				{
					strVal = nf.format( (Number) objVal );
				}
			}

			// Nothing is assigned by above logic (timestamp + others)... make a simple "toString()"
			if (strVal == null)
			{
				strVal = objVal.toString();
			}
		}

		// Replacement of null values
		if (strVal == null)
			strVal = nullReplace;

		// Replacement of null or "blank"
		if (StringUtil.isNullOrBlank(strVal))
			strVal = blankReplace;

		return strVal;
	}

	/**
	 * Get 1 rows as a HTML Table. <br>
	 * Left column is column names (in bold)<br>
	 * Right column is row content
	 * 
	 * @param mrow               The row number
	 * @param startMsg           Add a message string at the beginning
	 * @param endMsg             Add a message string at the end
	 * @param borders            Add borders to the HTML Table
	 * @param stripedRows        Make striped rows in the HTML Table
	 * @param addOuterHtmlTags   If we should surround the content with &lt;html&gt;...&lt;/html&gt; tags
	 * @return
	 */
	public String toHtmlTableString(int mrow, String startMsg, String endMsg, boolean borders, boolean stripedRows, boolean addOuterHtmlTags)
	{
		StringBuilder sb = new StringBuilder(1024);

		if (addOuterHtmlTags)
			sb.append("<html>");

		if (StringUtil.hasValue(startMsg))
			sb.append(startMsg);

		int cols = getColumnCount();
		String border      = borders ? " border=1"      : " border=0";
		String cellPadding = borders ? ""               : " cellpadding=1";
		String cellSpacing = borders ? ""               : " cellspacing=0";

		String stripeColor = Configuration.getCombinedConfiguration().getProperty(   PROPKEY_HtmlToolTip_stripeColor,   DEFAULT_HtmlToolTip_stripeColor);
		int    maxStrLen   = Configuration.getCombinedConfiguration().getIntProperty(PROPKEY_HtmlToolTip_maxCellLength, DEFAULT_HtmlToolTip_maxCellLength);
		
		// One row for every column
		sb.append("<table").append(border).append(cellPadding).append(cellSpacing).append(">\n");
		for (int c=0; c<cols; c++)
		{
			String stripeTag = "";
			if (stripedRows && ((c % 2) == 0) )
				stripeTag = " bgcolor='" + stripeColor + "'";
			
			Object objVal = getValueAt(mrow,c);
			String strVal = "";
			if (objVal != null)
			{
//				strVal = objVal.toString();
				strVal = toString(objVal, NULL_REPLACE);
				if (strVal != null)
				{
					int strValLen = strVal.length(); 
					if (strValLen > maxStrLen)
					{
						strVal =  strVal.substring(0, maxStrLen);
						strVal += "...<br><font color='orange'><i><b>NOTE:</b> content is truncated after " + maxStrLen + " chars (actual length is "+strValLen+"), tooltip on this cell might show full content.</i></font>";
					}
				}
			}

			sb.append("<tr").append(stripeTag).append(">");
			sb.append("<td nowrap><b>").append(getColumnName(c)).append("</b>&nbsp;</td>");
			sb.append("<td nowrap>")   .append(strVal)          .append("</td>");
			sb.append("</tr>\n");
		}
		sb.append("</table>\n");

		if (StringUtil.hasValue(endMsg))
			sb.append(endMsg);

		if (addOuterHtmlTags)
			sb.append("</html>");

		return sb.toString();
	}
	
	/**
	 * Return a html string with one table for each row. The Table will only have 2 columns. 1=Column Name, 2=Column Value
	 * @return
	 */
	public String toHtmlTablesVerticalString(String className, Map<String, String> colNameValueTagMap)
	{
		return toHtmlTablesVerticalString(className, true, true, colNameValueTagMap);
	}
	public String toHtmlTablesVerticalString(String className, boolean tHeadNoWrap, boolean tBodyNoWrap, Map<String, String> colNameValueTagMap)
	{
		StringBuilder sb = new StringBuilder(1024);

//		String tHeadNoWrapStr = tHeadNoWrap ? " nowrap" : "";
		String tBodyNoWrapStr = tBodyNoWrap ? " nowrap" : "";
		
		int cols = getColumnCount();
		int rows = getRowCount();

		for (int r=0; r<rows; r++)
		{
			sb.append("<br>\n");
			sb.append("Row: ").append(r+1).append(" (").append(rows).append(")").append("<br>\n");

			if (StringUtil.hasValue(className))
			{
				sb.append("<table border='1' class='").append(className).append("'>\n");
			}
			else
			{
				sb.append("<table border='1'>\n");
//				sb.append("<table border=1 style='min-width: 1024px; width: 100%;' width='100%'>\n");
			}
			
			// build header names
			sb.append("<thead>\n");
			sb.append("  <tr> <th>Column</th> <th>Value</th> </tr>\n");
			sb.append("</thead>\n");

			// build all data rows...
			sb.append("<tbody>\n");
			for (int c=0; c<cols; c++)
			{
				String colName = getColumnName(c);
				Object objVal  = getValueAt(r,c);
				String strVal = toString(objVal, "&nbsp;", "&nbsp;");
				
				String beginTag = "";
				String endTag   = "";
				
				if (colNameValueTagMap != null && colNameValueTagMap.containsKey(colName))
				{
					String tagName = colNameValueTagMap.get(colName);
					if (StringUtil.hasValue(tagName))
					{
						beginTag = "<"  + tagName + ">";
						endTag   = "</" + tagName + ">";
					}
				}
				
//				String strVal = "";
//				if (objVal != null)
//				{
//					strVal = objVal.toString();
//				}
//				if (StringUtil.isNullOrBlank(strVal))
//					strVal = "&nbsp;";

				// Translate NL to <br> it it's not already a HTML string.
				// or maybe: wrap it with <pre>"strVal"</pre> instead
				if ( ! StringUtil.containsHtml(strVal) )
					strVal = strVal.replace("\n", "<br>");

				//----------------------------------------------------------------
				// BEGIN -- grabbed from CounterModel.toHtmlTableString(...)
				//----------------------------------------------------------------
				// Remove any leading/ending HTML tags
				if (StringUtils.startsWithIgnoreCase(strVal, "<html>"))
				{
					strVal = strVal.substring("<html>".length());
					strVal = strVal.trim();
					if (StringUtils.endsWithIgnoreCase(strVal, "</html>"))
						strVal = strVal.substring(0, strVal.length() - "</html>".length() );
				}
				else
				{
					int xml_StartPos         = strVal.indexOf("<?xml");
					int ShowPlanXML_startPos = strVal.indexOf("<ShowPlanXML xmlns=");

					// check for XML content "somewhere" in the string
//					if (xml_StartPos >= 0)
					if (xml_StartPos >= 0 && xml_StartPos < 30)
					{
						String conf = Configuration.getCombinedConfiguration().getProperty("toHtmlTableString.xml", "TO_ESCAPED_TEXT");

						if ("TO_ESCAPED_TEXT".equals(conf))
						{
							// if there are any XML tag in the field... Then surround the value with a '<pre>' tag and escape all "xml" tags etc...
							strVal = "<pre>" + StringEscapeUtils.escapeHtml4(strVal) + "</pre>";
//							strVal = "<pre>" + StringEscapeUtils.escapeXml10(strVal) + "</pre>";
						}
					}
//					else if (ShowPlanXML_startPos >= 0)
					else if (ShowPlanXML_startPos >= 0 && ShowPlanXML_startPos < 30)
					{
						String originXmlPlan = strVal;
						SqlServerShowPlanXmlTransformer t = new SqlServerShowPlanXmlTransformer();
						try
						{
							// Get HTML (what type/look-and-feel) is decided by configuration in: SqlServerShowPlanXmlTransformer.PROKKEY_transform
							strVal = t.toHtml(strVal);
						}
						catch (Exception ex)
						{
							strVal = "For columnName='" + colName + "', ShowPlanXML_startPos=" + ShowPlanXML_startPos + ". Could not translate SQL-Server ShowPlanXML to HTML text. Caught: " + ex;
							_logger.error(strVal, ex);
							_logger.warn("DEBUG-AS-WARNING: For columnName='" + colName + "', ShowPlanXML_startPos=" + ShowPlanXML_startPos + ". XML Showplan Text caused the above error: |" + originXmlPlan + "|.");
						}
					}
					else
					{
						// make '\n' into '<br>'
						strVal = strVal.replace("\n", "<br>");
					}
				}
				//----------------------------------------------------------------
				// END -- grabbed from CounterModel.toHtmlTableString(...)
				//----------------------------------------------------------------
				
				sb.append("  <tr>\n");
				sb.append("    <td").append(tBodyNoWrapStr).append("><b>").append(colName).append("</b></td>\n");
				sb.append("    <td").append(tBodyNoWrapStr).append(">").append(beginTag).append(strVal).append(endTag).append("</td>\n");
				sb.append("  </tr>\n");
			}
			
			sb.append("</tbody>\n");
			
			sb.append("</table>\n");
		}

		return sb.toString();
	}

	
	//------------------------------------------------------------
	//-- BEGIN: Highlighters
	//-- This shouldn't really be in here, it's not part of the model, it's part of the view...
	//------------------------------------------------------------
	private List<Highlighter> _jxTableHighlighters = null;

	public void addHighlighter(Highlighter highlighter)
	{
		if (_jxTableHighlighters == null)
			_jxTableHighlighters = new ArrayList<>();
		
		_jxTableHighlighters.add(highlighter);
	}

	public void removeHighlighter(Highlighter highlighter)
	{
		if (_jxTableHighlighters == null)
			return;

		_jxTableHighlighters.remove(highlighter);
	}

	public List<Highlighter> getHighlighters()
	{
		return _jxTableHighlighters;
	}

	public boolean hasHighlighters()
	{
		if (_jxTableHighlighters == null)
			return false;

		return ! _jxTableHighlighters.isEmpty();
	}
	//------------------------------------------------------------
	//-- END: Highlighters
	//------------------------------------------------------------

	public static class ColumnNameNotFoundException
	extends RuntimeException
	{
		private static final long serialVersionUID = 1L;

		public ColumnNameNotFoundException(String colName, String rstmName)
		{
			super("Could not find column '" + colName + "' in table '" + rstmName + "'.");
		}
	}

	//------------------------------------------------------------
	//-- BEGIN: sort
	//------------------------------------------------------------
	/**
	 * Find the column position for a column name. (starting at 0)
	 * 
	 * @param colName
	 * @return index pos (0 index)
	 * @throws ColumnNameNotFoundException if the column name wasn't found.
	 */
	public int findColumnMandatory(String colName)
	{
		int index = findColumn(colName);
		
		if (index == -1)
			throw new ColumnNameNotFoundException(colName, getName());

		return index;
	}

	/**
	 * Find the column position for a column name. (starting at 0)
	 * 
	 * @param colName
	 * @return index pos (0 index)
	 * @throws ColumnNameNotFoundException if the column name wasn't found.
	 */
	public int findColumnNoCaseMandatory(String colName)
	{
		int index = findColumnNoCase(colName);
		
		if (index == -1)
			throw new ColumnNameNotFoundException(colName, getName());

		return index;
	}

	/**
	 * Find the Position (starting at 0)
	 * @param colName Name to search for
	 * @return -1 if not found
	 */
	@Override
	public int findColumn(String colName)
	{
		return super.findColumn(colName);
	}

	/**
	 * Find the Position (starting at 0)
	 * @param colName Name to search for
	 * @return -1 if not found
	 */
	public int findColumnNoCase(String colName)
	{
		for (int i = 0; i < getColumnCount(); i++) 
		{
			if (colName.equalsIgnoreCase(getColumnName(i))) 
			{
				return i;
			}
		}
		return -1;
	}

	/** true if the column exists */
	public boolean hasColumnNoCase(String colName)
	{
		return findColumnNoCase(colName) != -1;
	}

	/** true if the column exists */
	public boolean hasColumn(String colName)
	{
		return findColumn(colName) != -1;
	}


	/**
	 * REMOVE This column from the TableModel 
	 * @param colname    Name of the column (case Sensitive)
	 * 
	 * @return index of the column name we just removed. (starting at 0, -1 == column was NOT found)
	 */
	public int removeColumn(String colname)
	{
		return removeColumn(colname, false);
	}

	/**
	 * REMOVE This column from the TableModel 
	 * @param colname    Name of the column (case IN-Sensitive)
	 * 
	 * @return index of the column name we just removed. (starting at 0, -1 == column was NOT found)
	 */
	public int removeColumnNoCase(String colname)
	{
		return removeColumn(colname, true);
	}

	/**
	 * REMOVE This column from the TableModel 
	 * @param colname
	 * @param noCase    The passed column name should be CASE-SENSITIVE or NO-CASE-SENSITIVE 
	 * 
	 * @return index of the column name we just removed. (starting at 0, -1 == column was NOT found)
	 */
	public int removeColumn(String colname, boolean noCase)
	{
		int colIndex = findColumn(colname);
		if (colIndex == -1 && noCase)
			colIndex = findColumnNoCase(colname);

		if (colIndex == -1)
			return -1;

		// - Remove all the meta data fields for this column
		// - Remove all column data for all rows
		synchronized (this)
		{
			// Remove all MetaData
			_rsmdRefTableName     .remove(colIndex);
			_rsmdColumnName       .remove(colIndex);
			_rsmdColumnLabel      .remove(colIndex);
			_rsmdColumnType       .remove(colIndex);
			_rsmdColumnTypeStr    .remove(colIndex);
			_rsmdColumnTypeName   .remove(colIndex);
			_rsmdColumnTypeNameStr.remove(colIndex);
			_rsmdColumnClassName  .remove(colIndex);
			_displaySize          .remove(colIndex);
			
			_classType = ArrayUtils.remove(_classType, colIndex);


			// For every row remove the column
			for (ArrayList<Object> row : _rows)
			{
				row.remove(colIndex);
			}

			// Increment number of cols
			_numcols--;
		}
		
		return colIndex;
	}

	public void addColumn(String colname, int pos, int jdbcType, String sqlTypeShort, String sqlTypeLong, int length, int scale, Object defaultValue, Class<?> clazz)
	{
		if (pos < 0)
			throw new RuntimeException("Column position can't be negative. pos="+pos);
		if (pos > getColumnCount())
			throw new RuntimeException("Column position can't be larger than current column count. colCount=" + getColumnCount() + ", pos=" + pos);

		if (_classType == null)
			_classType = new Class[ getColumnCount() + 1 ];

		// - Remove all the meta data fields for this column
		// - Remove all column data for all rows
		synchronized (this)
		{
			// Remove all MetaData
			_rsmdRefTableName     .add(pos, "-unknown-");
			_rsmdColumnName       .add(pos, colname);
			_rsmdColumnLabel      .add(pos, colname);
			_rsmdColumnType       .add(pos, jdbcType);
			_rsmdColumnTypeStr    .add(pos, getColumnJavaSqlTypeName(jdbcType));
			_rsmdColumnTypeName   .add(pos, sqlTypeShort);
			_rsmdColumnTypeNameStr.add(pos, sqlTypeLong);
			_rsmdColumnClassName  .add(pos, clazz.getName());
			_displaySize          .add(pos, (length > 0) ? length : 12);
			
			_classType = ArrayUtils.insert(pos+1, _classType, clazz);
//System.out.println("_classType="+StringUtil.toCommaStr(_classType));


			// For every row remove the column
			for (ArrayList<Object> row : _rows)
			{
				row.add(pos, defaultValue);
			}

			// Increment number of cols
			_numcols++;
		}
		
	}
	public void addRow(ArrayList<Object> row)
	{
		if (getColumnCount() != row.size())
			throw new IllegalArgumentException("addRow(): Table and Row column count do not match. Table column is " + getColumnCount() + ". Columns in added row is " + row.size() + ".");

		_rows.add(row);
	}

	public void addRows(List<ArrayList<Object>> rows)
	{
		ArrayList<Object> row = rows.get(0);

		if (getColumnCount() != row.size())
			throw new IllegalArgumentException("addRow(): Table and Row column count do not match. Table column is " + getColumnCount() + ". Columns in added row is " + row.size() + ".");

		_rows.addAll(rows);
	}

	/**
	 * Remove a single row
	 * @param row Index of the row (starting at 0)
	 * @return the row that was returned.
	 */
	public ArrayList<Object> removeRow(int row)
	{
		return _rows.remove(row);
	}

	/**
	 * 
	 * @param colName
	 * @param values   A collection of values that matches the column names (all values in the collection will be deleted, so basically a OR clause) 
	 * 
	 * @return The rows that was deleted (if 0 rows was deleted, a empty list will be returned)
	 * @throws ColumnNameNotFound if the passed column name wasn't found
	 */
	public List<ArrayList<Object>> removeRows(String colName, Collection<String> values)
	{
		List<ArrayList<Object>> removedRows = new ArrayList<>();
		
		int col_pos = findColumnNoCase(colName);
		if (col_pos == -1)
		{
//			throw new ColumnNameNotFound("Column name '" + colName + "' not found in table '" + getName() +"'.");
			_logger.error("Column name '" + colName + "' not found in table '" + getName() +"'. Not possible to remove any rows for this column.");
			return removedRows;
		}

		for (Iterator<ArrayList<Object>> iter = _rows.iterator(); iter.hasNext();)
		{
			ArrayList<Object> row = iter.next();

			Object val = row.get(col_pos);
			if (val == null)
				continue;
			
			String valStr = val.toString();
			//System.out.println("  ?? values=|"+values+"|: contains valStr=|"+valStr+"|");
			
			if (values.contains(valStr))
			{
				//System.out.println("    >>>> REMOVING: row="+row);
				removedRows.add(row);
				iter.remove();
			}
		}

		return removedRows;
	}

	/**
	 * @param colName  (ColumnName is IN-SENSITIVE, Sorting data ASCENDING (in CASE-SENSITIVE order when it's a String) 
	 */
	public void sort(String colName)
	{
		List<SortOptions> sortOptions = new ArrayList<>();
		
		sortOptions.add(new SortOptions(colName, ColumnNameSensitivity.IN_SENSITIVE, SortOrder.ASCENDING, DataSortSensitivity.SENSITIVE));

		sort(sortOptions);
	}

	/**
	 * Sort by multiple columns
	 * 
	 * @param sortOptions  How you want 
	 */
	public void sort(List<SortOptions> sortOptions)
	{
		Collections.sort(_rows, new ResultSetTableComparator(sortOptions, this, getName()));
	}


//	/**
//	 * 
//	 * @param colName
//	 * @return false if column was NOT found. true if we did a sort
//	 */
//	public boolean sort(final String colName)
//	{
//		final boolean isCaseInSensitive = false;
//
//		final int colPos = findColumnNoCase(colName);
//		if (colPos == -1)
//			return false;
//				
//		Comparator<List<Object>> comparator = new Comparator<List<Object>>()
//		{
//			@Override
//			public int compare(List<Object> leftList, List<Object> rightList)
//			{
//				Object left  = leftList .get(colPos);
//				Object right = rightList.get(colPos);
//				
//				if (left == right)
//					return 0;
//
//				if (left == null)
//					return -1;
//
//				if (right == null)
//					return 1;
//
//				if ( isCaseInSensitive )
//				{
//					if (left instanceof String && right instanceof String)
//						return String.CASE_INSENSITIVE_ORDER.compare( (String) left, (String) right );
//				}
//				
//				// COMPARABLE, which would be the normal thing
//				if (left instanceof Comparable)
//					return ((Comparable)left).compareTo(right);
//				
//				if ( left instanceof byte[] && right instanceof byte[] )
//					return byteCompare((byte[])left, (byte[])right);
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
//		};
//		
//		Collections.sort(_rows, comparator);
//		return true;
//	}
	//------------------------------------------------------------
	//-- END: sort
	//------------------------------------------------------------
	

	//------------------------------------------------------------
	//-- BEGIN: getValueAsXXXXX using column name
	//          more methods will be added as they are needed
	//------------------------------------------------------------

	//-------------------------
	//---- STRING
	//-------------------------
	public String getValueAsString(int mrow, String colName)
	{
		return getValueAsString(mrow, colName, true, null);
	}
	public String getValueAsString(int mrow, String colName, boolean caseSensitive)
	{
		return getValueAsString(mrow, colName, caseSensitive, null);
	}
	public String getValueAsString(int mrow, String colName, boolean caseSensitive, String defaultNullValue)
	{
		Object o = getValueAsObject(mrow, colName, caseSensitive);

		if (o == null)
			return _nullValuesAsEmptyInGetValueAsType ? "" : defaultNullValue;

		return o.toString();
	}
	public String getValueAsString(int mrow, int mcol)
	{
		return getValueAsString(mrow, mcol, null);
	}
	public String getValueAsString(int mrow, int mcol, String defaultNullValue)
	{
		Object o = getValueAsObject(mrow, mcol);

		if (o == null)
			return _nullValuesAsEmptyInGetValueAsType ? "" : defaultNullValue;

		return o.toString();
	}

	//-------------------------
	//---- SHORT
	//-------------------------
	public Short getValueAsShort(int mrow, String colName)
	{
		return getValueAsShort(mrow, colName, true);
	}
	public Short getValueAsShort(int mrow, String colName, boolean caseSensitive)
	{
		return getValueAsShort(mrow, colName, caseSensitive, null);
	}
	public Short getValueAsShort(int mrow, String colName, boolean caseSensitive, Short defaultNullValue)
	{
		Object o = getValueAsObject(mrow, colName, caseSensitive);

		if (o == null)
			return _nullValuesAsEmptyInGetValueAsType ? new Short((short)0) : defaultNullValue;

		if (o instanceof Number)
			return ((Number)o).shortValue();

		try
		{
			return Short.parseShort(o.toString());
		}
		catch(NumberFormatException e)
		{
			try
			{
				return NumberFormat.getInstance().parse(o.toString()).shortValue();
			}
			catch (ParseException pe)
			{
				_logger.warn("Problem reading Short value for mrow="+mrow+", column='"+colName+"', TableModelNamed='"+getName()+"', returning null. Caught: "+pe);
				return null;
			}
		}
	}
	public Short getValueAsShort(int mrow, int mcol)
	{
		return getValueAsShort(mrow, mcol, null);
	}
	public Short getValueAsShort(int mrow, int mcol, Short defaultNullValue)
	{
		Object o = getValueAsObject(mrow, mcol);

		if (o == null)
			return _nullValuesAsEmptyInGetValueAsType ? new Short((short)0) : defaultNullValue;

		if (o instanceof Number)
			return ((Number)o).shortValue();

		try
		{
			return Short.parseShort(o.toString());
		}
		catch(NumberFormatException e)
		{
			try
			{
				return NumberFormat.getInstance().parse(o.toString()).shortValue();
			}
			catch (ParseException pe)
			{
				_logger.warn("Problem reading Short value for mrow="+mrow+", mcol='"+mcol+"', TableModelNamed='"+getName()+"', returning null. Caught: "+pe);
				return null;
			}
		}
	}

	//-------------------------
	//---- INTEGER
	//-------------------------
	public Integer getValueAsInteger(int mrow, String colName)
	{
		return getValueAsInteger(mrow, colName, true);
	}
	public Integer getValueAsInteger(int mrow, String colName, boolean caseSensitive)
	{
		return getValueAsInteger(mrow, colName, caseSensitive, null);
	}
	public Integer getValueAsInteger(int mrow, String colName, boolean caseSensitive, Integer defaultNullValue)
	{
		Object o = getValueAsObject(mrow, colName, caseSensitive);

		if (o == null)
			return _nullValuesAsEmptyInGetValueAsType ? new Integer(0) : defaultNullValue;

		if (o instanceof Number)
			return ((Number)o).intValue();

		try
		{
			return Integer.parseInt(o.toString());
		}
		catch(NumberFormatException e)
		{
			try
			{
				return NumberFormat.getInstance().parse(o.toString()).intValue();
			}
			catch (ParseException pe)
			{
				_logger.warn("Problem reading Integer value for mrow="+mrow+", column='"+colName+"', TableModelNamed='"+getName()+"', returning null. Caught: "+pe);
				return null;
			}
		}
	}
	public Integer getValueAsInteger(int mrow, int mcol)
	{
		return getValueAsInteger(mrow, mcol, null);
	}
	public Integer getValueAsInteger(int mrow, int mcol, Integer defaultNullValue)
	{
		Object o = getValueAsObject(mrow, mcol);

		if (o == null)
			return _nullValuesAsEmptyInGetValueAsType ? new Integer(0) : defaultNullValue;

		if (o instanceof Number)
			return ((Number)o).intValue();

		try
		{
			return Integer.parseInt(o.toString());
		}
		catch(NumberFormatException e)
		{
			try
			{
				return NumberFormat.getInstance().parse(o.toString()).intValue();
			}
			catch (ParseException pe)
			{
				_logger.warn("Problem reading Integer value for mrow="+mrow+", mcol='"+mcol+"', TableModelNamed='"+getName()+"', returning null. Caught: "+pe);
				return null;
			}
		}
	}

	//-------------------------
	//---- LONG
	//-------------------------
	public Long getValueAsLong(int mrow, String colName)
	{
		return getValueAsLong(mrow, colName, true);
	}
	public Long getValueAsLong(int mrow, String colName, boolean caseSensitive)
	{
		return getValueAsLong(mrow, colName, caseSensitive, null);
	}
	public Long getValueAsLong(int mrow, String colName, boolean caseSensitive, Long defaultNullValue)
	{
		Object o = getValueAsObject(mrow, colName, caseSensitive);

		if (o == null)
			return _nullValuesAsEmptyInGetValueAsType ? new Long(0) : defaultNullValue;

		if (o instanceof Number)
			return ((Number)o).longValue();

		try
		{
			return Long.parseLong(o.toString());
		}
		catch(NumberFormatException e)
		{
			try
			{
				return NumberFormat.getInstance().parse(o.toString()).longValue();
			}
			catch (ParseException pe)
			{
				_logger.warn("Problem reading Long value for mrow="+mrow+", column='"+colName+"', TableModelNamed='"+getName()+"', returning null. Caught: "+pe);
				return null;
			}
		}
	}
	public Long getValueAsLong(int mrow, int mcol)
	{
		return getValueAsLong(mrow, mcol, null);
	}
	public Long getValueAsLong(int mrow, int mcol, Long defaultNullValue)
	{
		Object o = getValueAsObject(mrow, mcol);

		if (o == null)
			return _nullValuesAsEmptyInGetValueAsType ? new Long(0) : defaultNullValue;

		if (o instanceof Number)
			return ((Number)o).longValue();

		try
		{
			return Long.parseLong(o.toString());
		}
		catch(NumberFormatException e)
		{
			try
			{
				return NumberFormat.getInstance().parse(o.toString()).longValue();
			}
			catch (ParseException pe)
			{
				_logger.warn("Problem reading Long value for mrow="+mrow+", mcol='"+mcol+"', TableModelNamed='"+getName()+"', returning null. Caught: "+pe);
				return null;
			}
		}
	}

	//-------------------------
	//---- TIMESTAMP
	//-------------------------
	public Timestamp getValueAsTimestamp(int mrow, String colName)
	{
		return getValueAsTimestamp(mrow, colName, true);
	}
	public Timestamp getValueAsTimestamp(int mrow, String colName, boolean caseSensitive)
	{
		return getValueAsTimestamp(mrow, colName, caseSensitive, null);
	}
	public Timestamp getValueAsTimestamp(int mrow, String colName, boolean caseSensitive, Timestamp defaultNullValue)
	{
		Object o = getValueAsObject(mrow, colName, caseSensitive);

		if (o == null)
			return _nullValuesAsEmptyInGetValueAsType ? new Timestamp(0) : defaultNullValue;

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
	public Timestamp getValueAsTimestamp(int mrow, int mcol)
	{
		Object o = getValueAsObject(mrow, mcol);

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
			String colName = getColumnName(mcol);
			_logger.warn("Problem reading Timestamp value for mrow="+mrow+", mcol="+mcol+", column='"+colName+"', TableModelNamed='"+getName()+"', returning null. Caught: "+e);
			return null;
		}
	}

	//-------------------------
	//---- BIG DECIMAL
	//-------------------------
	public Double getValueAsDouble(int mrow, String colName)
	{
		return getValueAsDouble(mrow, colName, true);
	}
	public Double getValueAsDouble(int mrow, String colName, boolean caseSensitive)
	{
		return getValueAsDouble(mrow, colName, caseSensitive, null);
	}
	public Double getValueAsDouble(int mrow, String colName, boolean caseSensitive, Double defaultNullValue)
	{
		Object o = getValueAsObject(mrow, colName, caseSensitive);

		if (o == null)
			return _nullValuesAsEmptyInGetValueAsType ? new Double(0) : defaultNullValue;

		if (o instanceof Double)
			return ((Double)o);

		try
		{
			return new Double(o.toString());
		}
		catch(NumberFormatException e)
		{
			try
			{
				return NumberFormat.getInstance().parse(o.toString()).doubleValue();
			}
			catch (ParseException pe)
			{
				_logger.warn("Problem reading Double value for mrow="+mrow+", column='"+colName+"', TableModelNamed='"+getName()+"', returning null. Caught: "+pe);
				return null;
			}
		}
	}
	public Double getValueAsDouble(int mrow, int mcol)
	{
		return getValueAsDouble(mrow, mcol, null);
	}
	public Double getValueAsDouble(int mrow, int mcol, Double defaultNullValue)
	{
		Object o = getValueAsObject(mrow, mcol);

		if (o == null)
			return _nullValuesAsEmptyInGetValueAsType ? new Double(0) : defaultNullValue;

		if (o instanceof Double)
			return ((Double)o);

		try
		{
			return new Double(o.toString());
		}
		catch(NumberFormatException e)
		{
			try
			{
				return NumberFormat.getInstance().parse(o.toString()).doubleValue();
			}
			catch (ParseException pe)
			{
				_logger.warn("Problem reading Double value for mrow="+mrow+", mcol='"+mcol+"', TableModelNamed='"+getName()+"', returning null. Caught: "+pe);
				return null;
			}
		}
	}

	//-------------------------
	//---- BIG DECIMAL
	//-------------------------
	public BigDecimal getValueAsBigDecimal(int mrow, String colName)
	{
		return getValueAsBigDecimal(mrow, colName, true);
	}
	public BigDecimal getValueAsBigDecimal(int mrow, String colName, boolean caseSensitive)
	{
		return getValueAsBigDecimal(mrow, colName, caseSensitive, null);
	}
	public BigDecimal getValueAsBigDecimal(int mrow, String colName, boolean caseSensitive, BigDecimal defaultNullValue)
	{
		Object o = getValueAsObject(mrow, colName, caseSensitive);

		if (o == null)
			return _nullValuesAsEmptyInGetValueAsType ? new BigDecimal(0) : defaultNullValue;

		if (o instanceof BigDecimal)
			return ((BigDecimal)o);

		try
		{
			return new BigDecimal(o.toString());
		}
		catch(NumberFormatException e)
		{
			try
			{
				return new BigDecimal(NumberFormat.getInstance().parse(o.toString()).doubleValue());
			}
			catch (ParseException pe)
			{
				_logger.warn("Problem reading BigDecimal value for mrow="+mrow+", column='"+colName+"', TableModelNamed='"+getName()+"', returning null. Caught: "+pe);
				return null;
			}
		}
	}
	public BigDecimal getValueAsBigDecimal(int mrow, int mcol)
	{
		return getValueAsBigDecimal(mrow, mcol, null);
	}
	public BigDecimal getValueAsBigDecimal(int mrow, int mcol, BigDecimal defaultNullValue)
	{
		Object o = getValueAsObject(mrow, mcol);

		if (o == null)
			return _nullValuesAsEmptyInGetValueAsType ? new BigDecimal(0) : defaultNullValue;

		if (o instanceof BigDecimal)
			return ((BigDecimal)o);

		try
		{
			return new BigDecimal(o.toString());
		}
		catch(NumberFormatException e)
		{
			try
			{
				return new BigDecimal(NumberFormat.getInstance().parse(o.toString()).doubleValue());
			}
			catch (ParseException pe)
			{
				_logger.warn("Problem reading BigDecimal value for mrow="+mrow+", mcol='"+mcol+"', TableModelNamed='"+getName()+"', returning null. Caught: "+pe);
				return null;
			}
		}
	}

	//-------------------------
	//---- OBJECT
	//-------------------------
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
		{
			if (_handleColumnNotFoundAsNullValueInGetValues)
				return null;
			throw new RuntimeException("Can't find column '"+colName+"' in TableModel named '"+getName()+"'.");
		}
		
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
	public Object getValueAsObject(int mrow, int mcol)
	{
		TableModel tm = this;

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

	/**
	 * Get all values that matches
	 * 
	 * @param colName  column name to search for
	 * @param value    value to search for
	 * @return            List of row numbers.   empty if no rows was found.
	 * @throws NoColumnNameWasFound if any of the column names you searched for was not part of the ResultSet
	 */
	public List<Integer> getRowIdsWhere(String colName, Object value)
	{
		Map<String, Object> nameVal = new HashMap<>();
		nameVal.put(colName, value);
		
		return getRowIdsWhere(nameVal);
	}

	/**
	 * Get all values that matches
	 * 
	 * @param nameValue   a map of column names/values that we are searching for
	 * @return            List of row numbers.   empty if no rows was found.
	 * @throws NoColumnNameWasFound if any of the column names you searched for was not part of the ResultSet
	 */
	public List<Integer> getRowIdsWhere(Map<String, Object> nameValue)
	{
		boolean nocase = false;

		List<Integer> foundRows = new ArrayList<Integer>();
		
		int rowc = getRowCount();
		for (int r=0; r<rowc; r++)
		{
			int matchColCount = 0;
			for (Entry<String, Object> e : nameValue.entrySet())
			{
				String whereColName = e.getKey();
				Object whereColVal  = e.getValue();
				
				int colId = nocase ? findColumnNoCase(whereColName) : findColumn(whereColName);
				if (colId == -1)
					throw new RuntimeException("Can't find column '"+whereColName+"' in TableModel named '"+getName()+"'.");

				Object rowColVal = getValueAt(r, colId);
				
				if (whereColVal == null && rowColVal == NULL_REPLACE)
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
	
	public List<Object> getRowList(int rowId)
	{
		return _rows.get(rowId);
	}

//	/**
//	 *
//	 */
//	public static class NoColumnNameWasFound
//	extends Exception
//	{
//		private static final long serialVersionUID = 1L;
//
//		public NoColumnNameWasFound(String colname)
//		{
//			super("The column name '" + colname + "' was found.");
//		}
//	}
//
//	/**
//	 *
//	 */
//	public static class MoreThanOneRowWasFound
//	extends Exception
//	{
//		private static final long serialVersionUID = 1L;
//
//		public MoreThanOneRowWasFound(int numberOfFoundRows, Map<String, Object> nameValue)
//		{
//			super("Expected to find one row, but " + numberOfFoundRows + "rows was found that matched: " + nameValue);
//		}
//	}
	
	//------------------------------------------------------------
	//-- BEGIN: description
	//------------------------------------------------------------
	private String _tableDescription = null;
	private Map<String, String> _columnDescriptionMap = null;

	/** Set a descriptive text for this table */
	public void setDescription(String desc)
	{
		_tableDescription = desc;
	}

	/** Get a descriptive text for this table */
	public String getDescription()
	{
		return _tableDescription;
	}

	/** If we have got any Column Descriptions */
	public boolean hasColumnDescriptions()
	{
		if (_columnDescriptionMap == null)
			return false;
		
		if (_columnDescriptionMap.isEmpty())
			return false;
		
		return true;
	}
	/** Set a descriptive text for a specific column @returns The previous description. or null if no previous description */
	public String setColumnDescription(String colName, String desc)
	{
		if (_columnDescriptionMap == null)
			_columnDescriptionMap = new LinkedHashMap<>();
		
		return _columnDescriptionMap.put(colName, desc);
	}

	/** Get a descriptive text for a specific column, if the column can't be found NULL is returned */
	public String getColumnDescription(String colName)
	{
		if (_columnDescriptionMap == null)
			return "";

		String desc = _columnDescriptionMap.get(colName);
		if (desc == null)
			desc = "";
		
		return desc;
	}
	//------------------------------------------------------------
	//-- END: description
	//------------------------------------------------------------


	/**
	 * Check all columns and rows, and truncate "cells" that are above #KB
	 * @param overKb
	 */
	public int truncateColumnsWithSizeInKbOver(int overKb)
	{
		int truncCount = 0;
		int colCount = getColumnCount();
		int rowCount = getRowCount();

		// Exit early if there is nothing to do
		if (colCount <= 0 || rowCount <= 0)
			return truncCount;

		for (int r=0; r<rowCount; r++)
		{
			for (int c=0; c<colCount; c++)
			{
				Object cell = getValueAt(r, c);
				if (cell != null && cell instanceof String)
				{
					String cellStr = (String) cell;
					int cellLength = cellStr.length();
					
					boolean doTruncate = cellLength > overKb*1024;

					// If the value starts with some kind of "tag" HTML or XML... do NOT truncate
					if (cellStr.startsWith("<"))
						doTruncate = false;
					
					if (doTruncate)
					{
						_logger.info("Truncating row=" + r + ", column=" + c + " (colName='" + getColumnName(c) + "'), after " + overKb + " KB, origin size was " + (cellLength/1024) + " KB, in rstm named '" + getName() + "' ");
						String truncCellStr = cellStr.substring(0, overKb*1024) + "... NOTE: [truncated after " + overKb + " KB, originSize=" + (cellLength/1024) + " KB].";
						
						setValueAtWithOverride(truncCellStr, r, c);
						truncCount++;
					}
				}
			}
		}
		return truncCount;
	}

	/**
	 * Get approximate size for a table.<br>
	 * How object size are counted.
	 * <ul>
	 *    <li>String - get length of string.</li>
	 *    <li>all others - assume 16 bytes</li>
	 * </ul>
	 * @return
	 */
	public long getApproxSize()
	{
		long size = 0;
		int colCount = getColumnCount();
		int rowCount = getRowCount();

		// Exit early if there is nothing to do
		if (colCount <= 0 || rowCount <= 0)
			return size;

		for (int r=0; r<rowCount; r++)
		{
			for (int c=0; c<colCount; c++)
			{
				Object cell = getValueAt(r, c);
				if (cell != null)
				{
					if (cell instanceof String)
					{
						size += ((String) cell).length();
					}
					else
					{
						size += 16;
					}
				}
			}
		}
		return size;
	}


	/**
	 * Copy a cell content from one table to this table, where we have a matching column name/value
	 * 
	 * @param fromRstm                  Source table to copy from
	 * @param fromWhereColName          Column name in Source table we should use to qualify the correct row (like a where clause)
	 * @param fromContentColName        Column name in Source table we should copy 
	 * @param thisKeyColName            Column name in "this" table we should use to qualify the correct row from the source table (possibly same column name as 'srcWhereColName')
	 * @param thisContentColName        Column Name in "this" table to *put* the copied data 
	 * 
	 * @return number of rows we just copied!
	 */
	public int copyCellContentFrom(ResultSetTableModel fromRstm, String fromWhereColName, String fromContentColName, String thisKeyColName, String thisContentColName)
	{
		if (StringUtil.isNullOrBlank(thisKeyColName))  thisKeyColName  = fromWhereColName;
		if (StringUtil.isNullOrBlank(thisContentColName)) thisContentColName = fromContentColName;

		if (fromRstm == null)                             throw new IllegalArgumentException("sourceRstm can't be null");
		if (StringUtil.isNullOrBlank(fromWhereColName))   throw new IllegalArgumentException("srcWhereColName can't be null or blank");
		if (StringUtil.isNullOrBlank(fromContentColName)) throw new IllegalArgumentException("srcContentColName can't be null or blank");

		int copyCount = 0;
		
		List<String> fromWhereColNameList = StringUtil.parseCommaStrToList(fromWhereColName);
		List<String> thisKeyColNameList   = StringUtil.parseCommaStrToList(thisKeyColName);

		// Check input
		if (fromWhereColNameList.size() != thisKeyColNameList.size())
			throw new RuntimeException("Expecting from and this 'where column count' to be of same size. from.size=" + fromWhereColNameList.size() + ", this.size=" + thisKeyColNameList.size());
		
		// check existence for all column in the "from"
		for (String colName : fromWhereColNameList)
			fromRstm.findColumnMandatory(colName);
		int pos_fromContentColName = fromRstm.findColumnMandatory(fromContentColName);
			
		// check existence for all column in the "this"
		for (String colName : thisKeyColNameList)
			this.findColumnMandatory(colName);
		int pos_thisContentColName = this.findColumnMandatory(thisContentColName);

//		int pos_fromWhereColName    = fromRstm.findColumnMandatory(fromWhereColName);
//		int pos_fromContentColName  = fromRstm.findColumnMandatory(fromContentColName);
//
//		int pos_thisKeyColName     = findColumnMandatory(thisKeyColName);
//		int pos_thisContentColName = findColumnMandatory(thisContentColName);
		
		if (fromRstm.getRowCount() == 0)
			return copyCount;

		// loop this table
		//  - get Key column from this table (get the where clause)
		//  - get value from "from table" (using above where clause)
		//  - set fetched value in this table
		for (int r=0; r<this.getRowCount(); r++)
		{
			// Build "where" clause
			Map<String, Object> keyVal = new LinkedHashMap<>();
			for (int c=0; c<thisKeyColNameList.size(); c++)
			{
				String fromColName = fromWhereColNameList.get(c);
				String thisColName = thisKeyColNameList.get(c);

				// Get object from THIS table
				Object this_keyColVal = this.getValueAsObject(r, thisColName);

				// and put the ColName and value in a map, for later lookup
				keyVal.put(fromColName, this_keyColVal);
			}
			
			// Get row id's (using above map)
			List<Integer> rowIds = fromRstm.getRowIdsWhere(keyVal);
			
			if (_logger.isDebugEnabled())
				_logger.debug("copyCellContentFrom(): nameValMap=" + keyVal + ", FOUND: count=" + rowIds.size() + ", rowIds=" + rowIds);

			if (rowIds.size() >= 1)
			{
				// Get value (only from first row found)
				Object from_contentVal = fromRstm.getValueAsObject(rowIds.get(0), pos_fromContentColName);

				// Set value
				this.setValueAtWithOverride(from_contentVal, r, pos_thisContentColName);
				copyCount++;

				// WARNING: If we found MORE than one row... 
				if (rowIds.size() > 1)
				{
					_logger.warn("Found " + rowIds.size() + " from table '" + fromRstm.getName() + "', expected to find 1 row. (only using first row)");
				}
			}
			else
			{
				// No rows was found
			}
		}

		return copyCount;
	}
	
	

	/**
	 * Create a Static CTR (Common Table Expression) using all columns in this ResultSetTableModel
	 * 
	 * Below is a example of what will be created if we have a table with 3 columns
	 * <pre>
	 * +--+-----+----------------------+
	 * |id|col1 |col2                  |
	 * +--+-----+----------------------+
	 * |1 |one  |this is the first row |
	 * |2 |two  |this is the second row|
	 * |3 |three|this is the third row |
	 * +--+-----+----------------------+
	 * </pre>
	 *
	 * The below will be created
	 * <pre>
	 * with cteName as
	 * (
	 *               select '1' as [id], 'one'   as [col1], 'this is the first row'  as [col2]
	 *     union all select '2' as [id], 'two'   as [col1], 'this is the second row' as [col2]
	 *     union all select '3' as [id], 'three' as [col1], 'this is the third row'  as [col2]
	 * )
	 * </pre>
	 * 
	 * @param cteName       Name of the CTR to be created
	 * @return
	 */
	public String createStaticCte(String cteName)
	{
		StringBuilder sb = new StringBuilder();
		
		sb.append("with ").append(cteName).append(" as \n");
		sb.append("( \n");
		
		for (int r=0; r<getRowCount(); r++)
		{
			if (r == 0)
				sb.append("              select ");
			else
				sb.append("    union all select ");
			
			String comma = "";
			for (int c=0; c<getColumnCount(); c++)
			{
				Object obj = getValueAsObject(r, c);
				sb.append(comma)
					.append(DbUtils.safeStr(obj)) // value with or without single quotes (and NULL if it's a null value)
					.append(" as [")
					.append(getColumnName(c))
					.append("]");

				comma = ", ";
			}
			sb.append("\n");
		}
		
		sb.append(") \n");
		
		return sb.toString();
	}
	
	
	
	/**
	 * Create a HTML Table from a ResultSetTableModel.<br>
	 * The parameter 'rowSpec' will create one row in the created table for each columns fetched from the 'sourceRstm'.
	 * 
	 * @param row           Row number in the sourceRstm
	 * @param rowSpec       A LinkedHashMap<CoulumnNameOfTheRow, RowLabelName>
	 * @param htmlClassname Table className
	 * @return
	 */
	public String createHtmlKeyValueTableFromRow(int row, LinkedHashMap<String, String> rowSpec, String htmlClassname)
	{
		return createHtmlKeyValueTableFromRow(this, row, rowSpec, htmlClassname);
	}

	/**
	 * Create a HTML Table from a ResultSetTableModel.<br>
	 * The parameter 'rowSpec' will create one row in the created table for each columns fetched from the 'sourceRstm'.
	 * 
	 * @param sourceRstm    The ResultSetTableModel that we are going to fetch data from
	 * @param row           Row number in the sourceRstm
	 * @param rowSpec       A LinkedHashMap<CoulumnNameOfTheRow, RowLabelName>
	 * @param htmlClassname Table className
	 * @return
	 */
	public static String createHtmlKeyValueTableFromRow(ResultSetTableModel sourceRstm, int row, LinkedHashMap<String, String> rowSpec, String htmlClassname)
	{
		StringBuilder sb = new StringBuilder();

		String className = "";
		if (StringUtil.hasValue(htmlClassname))
			className = " class='" + htmlClassname + "'";
		
		sb.append("<table" + className + "> \n");
		sb.append("<tbody> \n");

		for (Entry<String, String> entry : rowSpec.entrySet())
		{
			String srcColName = entry.getKey();
			String rowLabel   = entry.getValue();
			String rowContent = sourceRstm.getValueAsString(row, srcColName, false, "'" + srcColName + "' column not found in table." );

			if (rowLabel == null)
				rowLabel = srcColName;

			sb.append("<tr> \n");
			sb.append("  <td><b>").append( rowLabel   ).append("</b></td> \n");
			sb.append("  <td>")   .append( rowContent ).append(    "</td> \n");
			sb.append("</tr> \n");
		}
		
		sb.append("</tbody> \n");
		sb.append("</table> \n");
		
		return sb.toString();
	}


	/**
	 * Column name not found
	 */
	public static class ColumnNameNotFound
	extends RuntimeException
	{
		private static final long serialVersionUID = 1L;
	
		public ColumnNameNotFound(String msg)
		{
			super(msg);
		}
	}

	
	
	//-------------------------------------------------------------------------------------------------------------
	// BEGIN: Parse text table (produced by method xxx) and create a ResultSetTableModel (without data types)
	//-------------------------------------------------------------------------------------------------------------
	
	/**
	 * Get the first ResultSetTableModel which contains the passed column names from the passed List of ResultSetTableModels
	 * 
	 * @param rstmList         List of ResultSetTableModels that we want to find a specififc table in.
	 * @param inExactOrder     <br>&emsp; &emsp; true  = The passed 'colNames' must be in <b>exact order</b> and position as the ResultSetTableModel
	 *                         <br>&emsp; &emsp; false = The passed 'colNames' can be in <b>any order</b>, just as long as any ResultSetTableModel has the names.
	 * @param colNames         The column names that we are looking for
	 * @return null if not found
	 */
	public static ResultSetTableModel getTableWithColumnNames(List<ResultSetTableModel> rstmList, boolean inExactOrder, String... colNames)
	{
		if (rstmList == null)
			return null;

		if (rstmList.isEmpty())
			return null;
		
		for (ResultSetTableModel entry : rstmList)
		{
			// If rstm columns is smaller than passed colNames... continue
			if (entry.getColumnCount() < colNames.length)
				continue;

			int foundCount = 0;
			if (inExactOrder)
			{
				for (int c=0; c<colNames.length; c++)
				{
					String colName = colNames[c];
					int    colPos  = entry.findColumn(colName);

					if (c == colPos)
						foundCount++;
				}
			}
			else
			{
				for (String colName : colNames)
				{
					if (entry.hasColumn(colName))
						foundCount++;
				}
			}
			
			// Did we find all the columns
			if (foundCount == colNames.length)
				return entry;
		}

		return null;
	}

	/**
	 * Parse a Text Table into a ResultSetTableModel
	 * <p>
	 * A Text table input looks like
	 * <pre>
     * +------+----------------+-----------------------------------+-----+-------------------+----------------+
     * |dbname|configuration_id|name                               |value|value_for_secondary|is_value_default|
     * +------+----------------+-----------------------------------+-----+-------------------+----------------+
     * |db1   |               1|MAXDOP                             |0    |(NULL)             |true            |
     * |db1   |               2|LEGACY_CARDINALITY_ESTIMATION      |0    |(NULL)             |true            |
     * |db1   |               3|PARAMETER_SNIFFING                 |1    |(NULL)             |true            |
     * +------+----------------+-----------------------------------+-----+-------------------+----------------+
     * Rows 3
	 * </pre>
	 * 
	 * If there are several Tables in the text, only the first will be returned.
	 * 
	 * @param textTable 
	 * @return a ResultSetTableModel
	 * @throws IllegalArgumentException in case of problems with input or parsing
	 */
	public static ResultSetTableModel parseTextTable(String textTable)
	throws IllegalArgumentException
	{
		if (StringUtil.isNullOrBlank(textTable))
			throw new IllegalArgumentException("textTable can't be null or empty.");

//		if (! StringUtil.startsWithIgnoreBlankIgnoreCase(textTable, "+-"))
//			throw new IllegalArgumentException("textTable must start with '+-'.");
		
		List<ResultSetTableModel> rstmList = parseTextTables( StringUtil.readLines(textTable) );

		if (rstmList == null)
			return null;
		
		if (rstmList.isEmpty())
			return null;

		return rstmList.get(0);
	}

	/**
	 * Parse a Text Table into a ResultSetTableModel
	 * <p>
	 * A Text table input looks like
	 * <pre>
     * +------+----------------+-----------------------------------+-----+-------------------+----------------+
     * |dbname|configuration_id|name                               |value|value_for_secondary|is_value_default|
     * +------+----------------+-----------------------------------+-----+-------------------+----------------+
     * |db1   |               1|MAXDOP                             |0    |(NULL)             |true            |
     * |db1   |               2|LEGACY_CARDINALITY_ESTIMATION      |0    |(NULL)             |true            |
     * |db1   |               3|PARAMETER_SNIFFING                 |1    |(NULL)             |true            |
     * +------+----------------+-----------------------------------+-----+-------------------+----------------+
     * Rows 3
	 * </pre>
	 * 
	 * If there are several Tables in the text, only the first will be returned.
	 * 
	 * @param textTableList  and since it's a list, one row for each line
	 * @return a ResultSetTableModel
	 * @throws IllegalArgumentException in case of problems with input or parsing
	 */
	public static ResultSetTableModel parseTextTable(List<String> textTableList)
	throws IllegalArgumentException
	{
		List<ResultSetTableModel> rstmList = parseTextTables( textTableList );

		if (rstmList == null)
			return null;
		
		if (rstmList.isEmpty())
			return null;

		return rstmList.get(0);
	}



	/**
	 * Parse a Text Table into a ResultSetTableModel
	 * <p>
	 * A Text table input looks like
	 * <pre>
     * +------+----------------+-----------------------------------+-----+-------------------+----------------+
     * |dbname|configuration_id|name                               |value|value_for_secondary|is_value_default|
     * +------+----------------+-----------------------------------+-----+-------------------+----------------+
     * |db1   |               1|MAXDOP                             |0    |(NULL)             |true            |
     * |db1   |               2|LEGACY_CARDINALITY_ESTIMATION      |0    |(NULL)             |true            |
     * |db1   |               3|PARAMETER_SNIFFING                 |1    |(NULL)             |true            |
     * +------+----------------+-----------------------------------+-----+-------------------+----------------+
     * Rows 3
     * 
     * +------+----------------+
     * |dbname|configuration_id|
     * +------+----------------+
     * |db2   |               1|
     * |db2   |               2|
     * +------+----------------+
     * Rows 2
	 * </pre>
	 * 
	 * @param textTable 
	 * @return a list of ResultSetTableModel
	 * @throws IllegalArgumentException in case of problems with input or parsing
	 */
	public static List<ResultSetTableModel> parseTextTables(String textTable)
	throws IllegalArgumentException
	{
		if (StringUtil.isNullOrBlank(textTable))
			throw new IllegalArgumentException("textTable can't be null or empty.");

//		if (! StringUtil.startsWithIgnoreBlankIgnoreCase(textTable, "+-"))
//			throw new IllegalArgumentException("textTable must start with '+-'.");
		
		return parseTextTables( StringUtil.readLines(textTable));
	}

	/**
	 * Parse a Text Table into a ResultSetTableModel
	 * <p>
	 * A Text table input looks like
	 * <pre>
     * +------+----------------+-----------------------------------+-----+-------------------+----------------+
     * |dbname|configuration_id|name                               |value|value_for_secondary|is_value_default|
     * +------+----------------+-----------------------------------+-----+-------------------+----------------+
     * |db1   |               1|MAXDOP                             |0    |(NULL)             |true            |
     * |db1   |               2|LEGACY_CARDINALITY_ESTIMATION      |0    |(NULL)             |true            |
     * |db1   |               3|PARAMETER_SNIFFING                 |1    |(NULL)             |true            |
     * +------+----------------+-----------------------------------+-----+-------------------+----------------+
     * Rows 3
     * 
     * +------+----------------+
     * |dbname|configuration_id|
     * +------+----------------+
     * |db2   |               1|
     * |db2   |               2|
     * +------+----------------+
     * Rows 2
	 * </pre>
	 * 
	 * @param textTableList  and since it's a list, one row for each line
	 * @return a list of ResultSetTableModel
	 * @throws IllegalArgumentException in case of problems with input or parsing
	 */
	public static List<ResultSetTableModel> parseTextTables(List<String> textTableList)
	throws IllegalArgumentException
	{
		if (textTableList == null)   throw new IllegalArgumentException("textTableList can't be null.");
		if (textTableList.isEmpty()) throw new IllegalArgumentException("textTableList can't be empty.");

		List<ResultSetTableModel> rstmList = new ArrayList<>();
		ResultSetTableModel rstm = null;
		
		int rstmCount = 0;
		int hrCount = 0;
		for (int l=0; l<textTableList.size(); l++)
		{
			String line = textTableList.get(l);
			
			// Skip empty lines
			if (StringUtil.isNullOrBlank(line))
				continue;

			// Any "Horizontal Rule/Row" 
			if (line.startsWith("+-"))
			{
				hrCount++;

				// End of table
				if (hrCount == 3)
				{
					hrCount = 0;
					rstmList.add(rstm);
					rstm = null;
				}
				continue;
			}

			// We have read FIRST Horizontal Row Separator, then we need to read ColumnNames
			if (hrCount == 1)
			{
				rstmCount++;
				rstm = new ResultSetTableModel("rstm-" + rstmCount);

				// Read the column headers
				String[] sa = line.split("\\|");

				// Add columns (skip first, since it's empty)
				for (int c=1; c<sa.length; c++)
					rstm.addColumn(sa[c].trim(), c-1, Types.VARCHAR, "varchar", "varchar(16389)", 16389, -1, "", String.class);

				continue;
			}
			// Read data
			else if (hrCount == 2)
			{
				if ( ! line.startsWith("|") )
				{
					if (_logger.isDebugEnabled())
						_logger.debug("parseTextTable(): skipping line[" + l + "] '" + line + "'. Reason: a 'data' line should start with '|'.");
					continue;
				}
				
				if (rstm == null)
					throw new IllegalArgumentException("rstm==null. Reason: Column headers has not yet been read.");

				// Read data and add it to RSTM
				String[] sa = line.split("\\|");
				if (rstm.getColumnCount() != sa.length-1) // -1 to skip slot 0, since it will be blank
				{
					_logger.warn("parseTextTable(): skipping line[" + l + "] '" + line + "'. Reason: rstm.getColumnCount=" + rstm.getColumnCount() + " is NOT equal parsedColumns=" + sa.length);
					continue;
				}

				// Create a row, and add it to the RSTM
				ArrayList<Object> row = new ArrayList<>(sa.length);
				for (int c=1; c<sa.length; c++)
				{
					String str = sa[c].trim();
					if (NULL_REPLACE.equals(str))
						str = null;
					row.add( str );
				}
				rstm.addRow( row );
				
//				ArrayList<Object> row = new ArrayList<>( Arrays.asList( (Object[]) sa ) );
//				rstm.addRow( row );
			}
			else
			{
				if (line.startsWith("Rows "))
				{
					// Should we compare last Produced ResultSetTableModel rowcount and the value in "Rows ##"
				}
				else
				{
					if (_logger.isDebugEnabled())
						_logger.debug("parseTextTable(): skipping line[" + l + "] '" + line + "'. No column definition has yet been read, skipping row.");
				}
			}
		}
		
		return rstmList;
	}
	//-------------------------------------------------------------------------------------------------------------
	// END: Parse text table (produced by method xxx) and create a ResultSetTableModel (without data types)
	//-------------------------------------------------------------------------------------------------------------
	
	
	//-------------------------------------------------------------------------------------------------------------
	// BEGIN: Some static methods to execute sql etc
	//-------------------------------------------------------------------------------------------------------------
	public static ResultSetTableModel createResultSetTableModel(ResultSet rs, String name, String sql)
	throws SQLException
	{
		return createResultSetTableModel(rs, name, sql, false);
	}
	
	private static ResultSetTableModel createResultSetTableModel(ResultSet rs, String name, String sql, boolean doTruncate)
	throws SQLException
	{
		ResultSetTableModel rstm = new ResultSetTableModel(rs, name, sql);

		// Set toString format for Timestamp to "yyyy-MM-dd HH:mm:ss"
		rstm.setToStringTimestampFormat_YMD_HMS();

		// use localized numbers to easier see big numbers (for example: 12345 -> 12,345)
		rstm.setToStringNumberFormat(true);

//		// Truncate *long* columns
//		if (doTruncate)
//		{
//			int truncLongCellSize = Configuration.getCombinedConfiguration().getIntProperty(DailySummaryReportFactory.PROPKEY_maxTableCellSizeKb, DailySummaryReportFactory.DEFAULT_maxTableCellSizeKb);
//			rstm.truncateColumnsWithSizeInKbOver(truncLongCellSize);
//		}

		return rstm;
	}

	/**
	 * Execute a SQL Query and return the Results as a ResultSetTableModel
	 * <p>
	 * NOTE: All Square Bracket Quoted Identifiers will be translated to DBMS specific quotes.<br>
	 * So SQL <code>select [c1] from [dbo].[t1]</code> will be translated into <code>select "c1" from "dbo"."t1"</code> if the Quote char for the DBMS is " (double quote)
	 * 
	 * @param conn      The JDBC Connection
	 * @param sql       SQL Statement to execute
	 * @param name      If you want to give the "table" a name!
	 * 
	 * @return a ResultSetTableModel 
	 * @throws SQLException on errors
	 */
	public static ResultSetTableModel executeQuery(DbxConnection conn, String sql, String name)
	throws SQLException
	{
		// transform all "[" and "]" to DBMS Vendor Quoted Identifier Chars 
		sql = conn.quotifySqlString(sql);

		try ( Statement stmnt = conn.createStatement() )
		{
			// Unlimited execution time
			stmnt.setQueryTimeout(0);
			try ( ResultSet rs = stmnt.executeQuery(sql) )
			{
				ResultSetTableModel rstm = createResultSetTableModel(rs, name, sql, false);
				
				if (_logger.isDebugEnabled())
					_logger.debug(name + "rstm.getRowCount()="+ rstm.getRowCount());
				
				return rstm;
			}
		}
	}
	
	/**
	 * Execute a SQL Query and return the Results as a ResultSetTableModel
	 * <p>
	 * NOTE: All Square Bracket Quoted Identifiers will be translated to DBMS specific quotes.<br>
	 * So SQL <code>select [c1] from [dbo].[t1]</code> will be translated into <code>select "c1" from "dbo"."t1"</code> if the Quote char for the DBMS is " (double quote)
	 * 
	 * @param conn                         The JDBC Connection
	 * @param sql                          SQL Statement to execute
	 * @param onErrorCreateEmptyRstm       If there are errors still return an "empty" ResultSetTableModel... If this is FALSE, then a null will be returned on errors
	 * @param name                         If you want to give the "table" a name!
	 * 
	 * @return a ResultSetTableModel (or null if statement fail <b>and</b> onErrorCreateEmptyRstm=false)
	 */
	public static ResultSetTableModel executeQuery(DbxConnection conn, String sql, boolean onErrorCreateEmptyRstm, String name)
	{
		return executeQuery(conn, sql, onErrorCreateEmptyRstm, name, false);
	}

	/**
	 * Execute a SQL Query and return the Results as a ResultSetTableModel
	 * <p>
	 * NOTE: All Square Bracket Quoted Identifiers will be translated to DBMS specific quotes.<br>
	 * So SQL <code>select [c1] from [dbo].[t1]</code> will be translated into <code>select "c1" from "dbo"."t1"</code> if the Quote char for the DBMS is " (double quote)
	 * 
	 * @param conn                         The JDBC Connection
	 * @param sql                          SQL Statement to execute
	 * @param onErrorCreateEmptyRstm       If there are errors still return an "empty" ResultSetTableModel... If this is FALSE, then a null will be returned on errors
	 * @param name                         If you want to give the "table" a name!
	 * @param doTruncate                   Truncate String column content if they are longer than #### characters
	 * 
	 * @return a ResultSetTableModel (or null if statement fail <b>and</b> onErrorCreateEmptyRstm=false)
	 */
	public static ResultSetTableModel executeQuery(DbxConnection conn, String sql, boolean onErrorCreateEmptyRstm, String name, boolean doTruncate)
	{
		// transform all "[" and "]" to DBMS Vendor Quoted Identifier Chars 
		sql = conn.quotifySqlString(sql);

		try ( Statement stmnt = conn.createStatement() )
		{
			// Unlimited execution time
			stmnt.setQueryTimeout(0);
			try ( ResultSet rs = stmnt.executeQuery(sql) )
			{
				ResultSetTableModel rstm = createResultSetTableModel(rs, name, sql, doTruncate);
				
				if (_logger.isDebugEnabled())
					_logger.debug(name + "rstm.getRowCount()="+ rstm.getRowCount());
				
				return rstm;
			}
		}
		catch(SQLException ex)
		{
//			setProblemException(ex);
			
			//_fullRstm = ResultSetTableModel.createEmpty(name);
			_logger.warn("Problems getting '" + name + "': " + ex);
			
			if (onErrorCreateEmptyRstm)
				return ResultSetTableModel.createEmpty(name);
			else
				return null;
		}
	}
	//-------------------------------------------------------------------------------------------------------------
	// END: Some static methods to execute sql etc
	//-------------------------------------------------------------------------------------------------------------
	
	public static void main(String[] args)
	{
		Properties log4jProps = new Properties();
		log4jProps.setProperty("log4j.rootLogger", "INFO, A1");
		//log4jProps.setProperty("log4j.rootLogger", "DEBUG, A1");
		log4jProps.setProperty("log4j.appender.A1", "org.apache.log4j.ConsoleAppender");
		log4jProps.setProperty("log4j.appender.A1.layout", "org.apache.log4j.PatternLayout");
		log4jProps.setProperty("log4j.appender.A1.layout.ConversionPattern", "%d - %-5p - %-30c{1} - %m%n");
		PropertyConfigurator.configure(log4jProps);

		String str = ""
    		+ "+------+----------------+-----------------------------------+-----+-------------------+----------------+\n"
    		+ "|dbname|configuration_id|name                               |value|value_for_secondary|is_value_default|\n"
    		+ "+------+----------------+-----------------------------------+-----+-------------------+----------------+\n"
    		+ "|db1   |               1|MAXDOP                             |0    |(NULL)             |true            |\n"
    		+ "|db1   |               2|LEGACY_CARDINALITY_ESTIMATION      |0    |(NULL)             |true            |\n"
    		+ "|db1   |               3|PARAMETER_SNIFFING                 |1    |(NULL)             |true            |\n"
    		+ "+------+----------------+-----------------------------------+-----+-------------------+----------------+\n"
    		+ "Rows 3\n"
    		+ "\n"
    		+ "+------+----------------+\n"
    		+ "|dbname|configuration_id|\n"
    		+ "+------+----------------+\n"
    		+ "|db2   |               1|\n"
    		+ "|db2   |               2|\n"
    		+ "+------+----------------+\n"
    		+ "Rows 2\n"
    		+"";
		List<ResultSetTableModel> rstmList = parseTextTables(str);
		System.out.println("rstmList.size=" + rstmList.size());

		for (ResultSetTableModel rstm : rstmList)
		{
			System.out.println("name='" + rstm.getName() + "', colCount=" + rstm.getColumnCount() + ", rowCount=" + rstm.getRowCount() + ", columns=" + rstm.getColumnNames() + ".");
			for (int r=0; r<rstm.getRowCount(); r++)
				System.out.println("    ROW[" + r + "] >> " + rstm.getRowList(r));
		}
		
	}

//	public static void main(String[] args)
//	{
//		Properties log4jProps = new Properties();
//		log4jProps.setProperty("log4j.rootLogger", "INFO, A1");
//		//log4jProps.setProperty("log4j.rootLogger", "DEBUG, A1");
//		log4jProps.setProperty("log4j.appender.A1", "org.apache.log4j.ConsoleAppender");
//		log4jProps.setProperty("log4j.appender.A1.layout", "org.apache.log4j.PatternLayout");
//		log4jProps.setProperty("log4j.appender.A1.layout.ConversionPattern", "%d - %-5p - %-30c{1} - %m%n");
//		PropertyConfigurator.configure(log4jProps);
//		
//		try
//		{
//			Connection conn = DriverManager.getConnection("jdbc:sybase:Tds:192.168.0.110:1600", "sa", "sybase");
//			
//			Statement stmnt = conn.createStatement();
//			ResultSet rs = stmnt.executeQuery("select top 5 * from sysobjects");
//			
//			ResultSetTableModel rstm = new ResultSetTableModel(rs, "dummy");
////			System.out.println(rstm.toHtmlTablesVerticalString());
//			System.out.println(rstm.toAsciiTableString());
//			System.out.println(rstm.toAsciiTablesVerticalString());
//			
//		}
//		catch(Exception ex)
//		{
//			ex.printStackTrace();
//		}
//	}
}
