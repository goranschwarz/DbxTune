/**
 * @author <a href="mailto:goran_schwarz@hotmail.com">Goran Schwarz</a>
 */
package asemon.gui;

import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.sql.SQLWarning;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;

import javax.swing.event.TableModelListener;
import javax.swing.table.TableModel;

import asemon.utils.StringUtil;

/**
 * This class takes a JDBC ResultSet object and implements the TableModel
 * interface in terms of it so that a Swing JTable component can display the
 * contents of the ResultSet.  
 * 
 * @author Goran Schwarz
 */
public class ResultSetTableModel
    implements TableModel
{
	int	_numcols;

	private ArrayList<String>            _type        = new ArrayList<String>();
	private ArrayList<String>            _cols        = new ArrayList<String>();
	private ArrayList<Integer>           _displaySize = new ArrayList<Integer>();
	private ArrayList<ArrayList<Object>> _rows        = new ArrayList<ArrayList<Object>>();
	private ArrayList<SQLWarning>        _sqlWarnList = new ArrayList<SQLWarning>();
	private boolean                      _allowEdit   = true; 
	/**
	 * This constructor creates a TableModel from a ResultSet.  
	 **/
	public ResultSetTableModel(ResultSet rs) 
	throws SQLException
	{
		this(rs, true);
	}
	public ResultSetTableModel(ResultSet rs, boolean editable) 
	throws SQLException
	{
		_allowEdit = editable;

		ResultSetMetaData rsmd = rs.getMetaData();
		_numcols = rsmd.getColumnCount() + 1;
		for (int c=1; c<_numcols; c++)
		{
			_cols       .add(rsmd.getColumnLabel(c));
			_type       .add(rsmd.getColumnClassName(c));
			_displaySize.add(new Integer( Math.max(rsmd.getColumnDisplaySize(c), rsmd.getColumnLabel(c).length()) ) );

//			System.out.println("name='"+_cols.get(c-1)+"', getColumnClassName("+c+")='"+_type.get(c-1)+"'.");
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
				Object o = rs.getObject(c);
				
				if (o instanceof String)
					row.add(((String)o).trim());
				else
					row.add(o);
//				if (o!=null)
//					System.out.println("ResultSetTableModel: Row="+rowCount+", Col="+c+", Class="+o.getClass()+", Comparable="+((o instanceof Comparable)?"true":"false"));
//				else
//					System.out.println("ResultSetTableModel: Row="+rowCount+", Col="+c+", ---NULL--");
			}
			_rows.add(row);
			rowCount++;
		}
//		rs.getStatement().close();
//		rs.close();
	}

	public List<SQLWarning> getSQLWarningList()
	{
		return _sqlWarnList;
	}

	public int getColumnCount()
	{
		return _cols.size();
	}

	public int getRowCount()
	{
		return _rows.size();
	}

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
	public boolean isCellEditable(int row, int column)
	{
		return _allowEdit;
	}

	// Since its not editable, we don't need to implement these methods
	public void setValueAt(Object value, int row, int column)
	{
	}

	public void addTableModelListener(TableModelListener l)
	{
	}

	public void removeTableModelListener(TableModelListener l)
	{
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