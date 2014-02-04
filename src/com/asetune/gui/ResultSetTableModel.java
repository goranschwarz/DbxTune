/**
 * @author <a href="mailto:goran_schwarz@hotmail.com">Goran Schwarz</a>
 */
package com.asetune.gui;

import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.sql.SQLWarning;
import java.sql.Timestamp;
import java.sql.Types;
import java.util.ArrayList;

import javax.swing.table.AbstractTableModel;

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

	public static final String  PROPKEY_PrintResultSetInfoLong = "ResultSetTableModel.print.rs.info.long";
	public static final boolean DEFAULT_PrintResultSetInfoLong = false;

	public static final String  PROPKEY_NULL_REPLACE = "ResultSetTableModel.replace.null.with";
	public static final String  DEFAULT_NULL_REPLACE = "(NULL)";

	private static final String BINARY_PREFIX = Configuration.getCombinedConfiguration().getProperty(PROPKEY_BINERY_PREFIX, DEFAULT_BINERY_PREFIX);;
	private static final String NULL_REPLACE  = Configuration.getCombinedConfiguration().getProperty(PROPKEY_NULL_REPLACE,  DEFAULT_NULL_REPLACE);

	private int	_numcols;

	private ArrayList<String>            _rsmdColumnName        = new ArrayList<String>();  // rsmd.getColumnName(c); 
	private ArrayList<String>            _rsmdColumnLabel       = new ArrayList<String>();  // rsmd.getColumnLabel(c); 
	private ArrayList<Integer>           _rsmdColumnType        = new ArrayList<Integer>(); // rsmd.getColumnType(c); 
	private ArrayList<String>            _rsmdColumnTypeStr     = new ArrayList<String>();  // getColumnJavaSqlTypeName(_rsmdColumnType.get(index)) 
	private ArrayList<String>            _rsmdColumnTypeName    = new ArrayList<String>();  // rsmd.getColumnTypeName(c);
	private ArrayList<String>            _rsmdColumnTypeNameStr = new ArrayList<String>();  // kind of 'SQL' datatype: this.getColumnTypeName(rsmd, c);
	private ArrayList<String>            _rsmdColumnClassName   = new ArrayList<String>();  // rsmd.getColumnClassName(c);
	private ArrayList<Integer>           _displaySize           = new ArrayList<Integer>(); // Math.max(rsmd.getColumnDisplaySize(c), rsmd.getColumnLabel(c).length());
	private ArrayList<ArrayList<Object>> _rows                  = new ArrayList<ArrayList<Object>>();
	private SQLWarning                   _sqlWarning            = null;
	private boolean                      _allowEdit             = true; 
	private String                       _name                  = null;
	private PipeCommand                  _pipeCmd               = null;
	private boolean                      _cancelled             = false;
	private int                          _abortedAfterXRows     = -1;

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
		this(rs, true, name, -1, null, null);
	}
	public ResultSetTableModel(ResultSet rs, boolean editable, String name, int stopAfterXrows, PipeCommand pipeCommand, SqlProgressDialog progress) 
	throws SQLException
	{
		_allowEdit = editable;
		_name      = name;
		_pipeCmd   = pipeCommand;

		if (_name != null)
			_name = _name.replace('\n', ' '); // remove newlines in name

		int maxDisplaySize = 32768;
		try { maxDisplaySize = Integer.parseInt( System.getProperty("ResultSetTableModel.maxDisplaySize", Integer.toString(maxDisplaySize)) ); }
		catch (NumberFormatException ignore) {};

		ResultSetMetaData rsmd = rs.getMetaData();
		_numcols = rsmd.getColumnCount() + 1;
		for (int c=1; c<_numcols; c++)
		{
			String columnLabel       = rsmd.getColumnLabel(c);
			String columnName        = rsmd.getColumnName(c);
			String columnClassName   = rsmd.getColumnClassName(c);
			String columnTypeNameGen = getColumnTypeName(rsmd, c);

			String columnTypeNameRaw = rsmd.getColumnTypeName(c);
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
					_logger.info("For column '"+columnLabel+"', columnDisplaySize is '"+columnDisplaySize+"', which is above max value of '"+maxDisplaySize+"', using max value. The max value can be changed with java parameter '-DResultSetTableModel.maxDisplaySize=sizeInBytes'. ResultSetTableModel.name='"+_name+"'");
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
			
//			System.out.println("name='"+_cols.get(c-1)+"', getColumnClassName("+c+")='"+_type.get(c-1)+"', getColumnTypeName("+c+")='"+_sqlType.get(c-1)+"'.");
		}

		String originProgressState = null;
		if (progress != null)
			originProgressState = progress.getState();

		int rowCount = 0;
		int readCount = 0;
		while(rs.next())
		{
			if ( progress != null && progress.isCancelled() )
			{
				_cancelled = true;
				break;
			}
				
			if ( stopAfterXrows > 0 )
			{
				if (readCount >= stopAfterXrows)
				{
					_abortedAfterXRows = readCount;
					break;
				}
			}
				
			readCount++;
			if (progress != null)
			{
				if ( (readCount % 100) == 0 )
					progress.setState(originProgressState + " row "+readCount);
			}

			// read any eventual SQLWarnings that is part of the row
			_sqlWarning = rs.getWarnings();
//			for (SQLWarning sqlw = rs.getWarnings(); sqlw != null; sqlw = sqlw.getNextWarning())
//				_sqlWarnList.add(sqlw);
			rs.clearWarnings();
				
			ArrayList<Object> row = new ArrayList<Object>();
			for (int c=1; c<_numcols; c++)
			{
//				Object o = rs.getObject(c);
				Object o = null;
				int type = _rsmdColumnType.get(c-1);

				switch(type)
				{
				case Types.CLOB:
					o = rs.getString(c);
					break;

				case Types.BINARY:
				case Types.VARBINARY:
				case Types.LONGVARBINARY:
					o = rs.getString(c);
					if (o != null)
						o = BINARY_PREFIX + o;
					break;

				case Types.DATE:
					o = rs.getDate(c);
					break;

				case Types.TIME:
					o = rs.getTime(c);
					break;

				case Types.TIMESTAMP:
					o = rs.getTimestamp(c);
					break;

				default:
						o = rs.getObject(c);
						break;
				}
				
				if (o instanceof String)
					row.add(((String)o).trim());
				else
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
			int type = _rsmdColumnType.get(c);

			switch(type)
			{
			case Types.BINARY:
			case Types.VARBINARY:
			case Types.LONGVARBINARY:
				int size = _displaySize.get(c);
				_displaySize.set(c, new Integer(size + BINARY_PREFIX.length()));
				break;
			}

		}
//		rs.getStatement().close();
//		rs.close();

		if (progress != null)
			progress.setState(originProgressState + " Read done, rows "+readCount);
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

		String regexpStr = ".*" + grep.getConfig() + ".*";
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
				if ( colObj.toString().matches(regexpStr) )
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
				     || columnType == java.sql.Types.BINARY
				     || columnType == java.sql.Types.VARBINARY )
				{
					int columnDisplaySize = rsmd.getColumnDisplaySize(col);

					columnTypeName += "("+columnDisplaySize+")";
				}
			}
			catch (SQLException ignore) {}
		}
		catch (SQLException e)
		{
			// Can we "compose" the datatype from the JavaType and DisplaySize???
			//columnTypeName = guessDataType(columnType, rsmd.getColumnDisplaySize(c));
			columnTypeName = "unhandled-datatype";

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
				case java.sql.Types.BINARY:       return "binary("+columnDisplaySize+")";
				case java.sql.Types.VARBINARY:    return "varbinary("+columnDisplaySize+")";
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
					columnTypeName = "unhandled-datatype";
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
		switch (columnType)
		{
		case java.sql.Types.BIT:          return "java.sql.Types.BIT";
		case java.sql.Types.TINYINT:      return "java.sql.Types.TINYINT";
		case java.sql.Types.SMALLINT:     return "java.sql.Types.SMALLINT";
		case java.sql.Types.INTEGER:      return "java.sql.Types.INTEGER";
		case java.sql.Types.BIGINT:       return "java.sql.Types.BIGINT";
		case java.sql.Types.FLOAT:        return "java.sql.Types.FLOAT";
		case java.sql.Types.REAL:         return "java.sql.Types.REAL";
		case java.sql.Types.DOUBLE:       return "java.sql.Types.DOUBLE";
		case java.sql.Types.NUMERIC:      return "java.sql.Types.NUMERIC";
		case java.sql.Types.DECIMAL:      return "java.sql.Types.DECIMAL";
		case java.sql.Types.CHAR:         return "java.sql.Types.CHAR";
		case java.sql.Types.VARCHAR:      return "java.sql.Types.VARCHAR";
		case java.sql.Types.LONGVARCHAR:  return "java.sql.Types.LONGVARCHAR";
		case java.sql.Types.DATE:         return "java.sql.Types.DATE";
		case java.sql.Types.TIME:         return "java.sql.Types.TIME";
		case java.sql.Types.TIMESTAMP:    return "java.sql.Types.TIMESTAMP";
		case java.sql.Types.BINARY:       return "java.sql.Types.BINARY";
		case java.sql.Types.VARBINARY:    return "java.sql.Types.VARBINARY";
		case java.sql.Types.LONGVARBINARY:return "java.sql.Types.LONGVARBINARY";
		case java.sql.Types.NULL:         return "java.sql.Types.NULL";
		case java.sql.Types.OTHER:        return "java.sql.Types.OTHER";
		case java.sql.Types.JAVA_OBJECT:  return "java.sql.Types.JAVA_OBJECT";
		case java.sql.Types.DISTINCT:     return "java.sql.Types.DISTINCT";
		case java.sql.Types.STRUCT:       return "java.sql.Types.STRUCT";
		case java.sql.Types.ARRAY:        return "java.sql.Types.ARRAY";
		case java.sql.Types.BLOB:         return "java.sql.Types.BLOB";
		case java.sql.Types.CLOB:         return "java.sql.Types.CLOB";
		case java.sql.Types.REF:          return "java.sql.Types.REF";
		case java.sql.Types.DATALINK:     return "java.sql.Types.DATALINK";
		case java.sql.Types.BOOLEAN:      return "java.sql.Types.BOOLEAN";
		default:
			return "unknown-datatype("+columnType+")";
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

		Object o = getValueAt(0, colid);
		Class<?> clazz = o!=null ? o.getClass() : Object.class;
		if (o instanceof Timestamp)
			return Object.class;
		else
			return clazz;
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
		if (ld) { header1.add("TableModel Class Name");       header2.add("TableModel.getColumnClass()"); }

		int headCols = header1.size();
		int numColsInRs = _numcols - 1; // _numcols starts at 1

		// Fill in 1 row for each column in the ResultSet
		ArrayList<ArrayList<String>> rows = new ArrayList<ArrayList<String>>();
		for (int c=0; c<numColsInRs; c++)
		{
			ArrayList<String> row = new ArrayList<String>();
			rows.add(row);

			        row.add( "" + (c+1) );
			        row.add( "" + _rsmdColumnLabel      .get(c) );
			if (ld) row.add( "" + _rsmdColumnName       .get(c)  );
			        row.add( "" + _rsmdColumnTypeStr    .get(c) );
			if (ld) row.add( "" + _rsmdColumnType       .get(c) );
			if (ld) row.add( "" + _rsmdColumnClassName  .get(c) );
			if (ld) row.add( "" + _rsmdColumnTypeName   .get(c) );
			        row.add( "" + _rsmdColumnTypeNameStr.get(c) );
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
	
	//////////////////////////////////////////////////////////////////////////////
	//////////////////////////////////////////////////////////////////////////////
	//////////////////////////////////////////////////////////////////////////////
	// Some extra stuff to make it printable in a column spaced order
	//////////////////////////////////////////////////////////////////////////////
	//////////////////////////////////////////////////////////////////////////////
	//////////////////////////////////////////////////////////////////////////////

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

		sb.append("<table border='1'>\n");
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
}
