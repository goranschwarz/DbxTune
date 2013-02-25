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
import java.util.List;

import javax.swing.table.AbstractTableModel;

import org.apache.log4j.Logger;

import com.asetune.sql.pipe.PipeCommand;
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
	
	private static String BINARY_PREFIX = "0x";

	int	_numcols;

	private ArrayList<String>            _type        = new ArrayList<String>();
	private ArrayList<String>            _sqlTypeStr  = new ArrayList<String>();
	private ArrayList<Integer>           _sqlTypeInt  = new ArrayList<Integer>();
	private ArrayList<String>            _cols        = new ArrayList<String>();
	private ArrayList<Integer>           _displaySize = new ArrayList<Integer>();
	private ArrayList<ArrayList<Object>> _rows        = new ArrayList<ArrayList<Object>>();
	private ArrayList<SQLWarning>        _sqlWarnList = new ArrayList<SQLWarning>();
	private boolean                      _allowEdit   = true; 
	private String                       _name        = null;
	private PipeCommand                  _pipeCmd     = null;

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
		this(rs, true, name, null);
	}
	public ResultSetTableModel(ResultSet rs, boolean editable, String name, PipeCommand pipeCommand) 
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
			String columnClassName   = rsmd.getColumnClassName(c);
			String columnTypeName    = getColumnTypeName(rsmd, c);
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

			_cols       .add(columnLabel);
			_type       .add(columnClassName);
			_sqlTypeStr .add(columnTypeName);
			_sqlTypeInt .add(new Integer(columnType));
			_displaySize.add(new Integer(columnDisplaySize));
			
//			System.out.println("name='"+_cols.get(c-1)+"', getColumnClassName("+c+")='"+_type.get(c-1)+"', getColumnTypeName("+c+")='"+_sqlType.get(c-1)+"'.");
		}


		int rowCount = 0;
		while(rs.next())
		{
			// read any eventual SQLWarnings that is apart of the row
			for (SQLWarning sqlw = rs.getWarnings(); sqlw != null; sqlw = sqlw.getNextWarning())
				_sqlWarnList.add(sqlw);
			rs.clearWarnings();
				
			ArrayList<Object> row = new ArrayList<Object>();
			for (int c=1; c<_numcols; c++)
			{
//				Object o = rs.getObject(c);
				Object o = null;
				int type = _sqlTypeInt.get(c-1);

				switch(type)
				{
				case Types.CLOB:
					o = rs.getString(c);
					break;

				case Types.BINARY:
				case Types.VARBINARY:
				case Types.LONGVARBINARY:
					o = BINARY_PREFIX + rs.getString(c);	
					break;

				default:
						o = rs.getObject(c);
						break;
				}
				
				if (o instanceof String)
					row.add(((String)o).trim());
				else
					row.add(o);
//				if (o!=null)
//					System.out.println("ResultSetTableModel: Row="+rowCount+", Col="+c+", Class="+o.getClass()+", Comparable="+((o instanceof Comparable)?"true":"false"));
//				else
//					System.out.println("ResultSetTableModel: Row="+rowCount+", Col="+c+", ---NULL--");
			}
			// apply pipe filter
			if (addRow(row))
			{
				_rows.add(row);
				rowCount++;
			}
		}

		// add 2 chars for BINARY types
		for (int c=0; c<(_numcols-1); c++)
		{
			int type = _sqlTypeInt.get(c);

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
	}

//	/** apply pipe cmd filter */
//	private boolean addRow(ArrayList<Object> row)
//	{
//		return true;
//	}
	/** apply pipe cmd filter */
	private boolean addRow(ArrayList<Object> row)
	{
		if (_pipeCmd == null)
			return true;

		String regexpStr = ".*" + _pipeCmd.getRegExp() + ".*";
System.out.println("ResultSetTableModel: applying filter: java-regexp '"+regexpStr+"', on row: "+row);

		// FIXME: THIS NEEDS A LOT MORE WORK
		// _pipeStr needs to be "compiled" info a PipeFilter object and used in the below somehow
		// Pattern.compile(regex).matcher(input).matches()
		for (Object colObj : row)
		{
			if (colObj != null)
				if ( colObj.toString().matches(regexpStr) )
					return true;
		}
		return false;
	}

	public static String getColumnTypeName(ResultSetMetaData rsmd, int col)
	{
		String columnTypeName;
		// in RepServer, the getColumnTypeName() throws exception: JZ0SJ ... wants metadata 
		try
		{
			columnTypeName = rsmd.getColumnTypeName(col);
		}
		catch (SQLException e)
		{
			// Can we "compose" the datatype from the JavaType and DisplaySize???
			//columnTypeName = guessDataType(columnType, rsmd.getColumnDisplaySize(c));
			columnTypeName = "unknown-datatype";

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
					columnTypeName = "unknown-datatype";
				}
			}
			catch (SQLException e1)
			{
			}
		}
		return columnTypeName;
	}
	public List<SQLWarning> getSQLWarningList()
	{
		return _sqlWarnList;
	}

	@Override
	public int getColumnCount()
	{
		return _cols.size();
	}

	@Override
	public int getRowCount()
	{
		return _rows.size();
	}

	@Override
	public String getColumnName(int column)
	{
		return (String)_cols.get(column);
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
		return o;
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
