/**
 * @author <a href="mailto:goran_schwarz@hotmail.com">Goran Schwarz</a>
 */
package asemon.gui;

import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.util.ArrayList;

import javax.swing.event.TableModelListener;
import javax.swing.table.TableModel;

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

//	private ArrayList<String>            _type = new ArrayList<String>();
//	private ArrayList<String>            _cols = new ArrayList<String>();
//	private ArrayList<ArrayList<Object>> _rows = new ArrayList<ArrayList<Object>>();
	private ArrayList _type = new ArrayList();
	private ArrayList _cols = new ArrayList();
	private ArrayList _rows = new ArrayList();
	private boolean   _allowEdit = true; 
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
			_cols.add(rsmd.getColumnLabel(c));
			_type.add(rsmd.getColumnClassName(c));
//			System.out.println("name='"+_cols.get(c-1)+"', getColumnClassName("+c+")='"+_type.get(c-1)+"'.");
		}


		int rowCount = 0;
		while(rs.next())
		{
//			ArrayList<Object> row = new ArrayList<Object>();
			ArrayList row = new ArrayList();
			for (int c=1; c<_numcols; c++)
			{
				Object o = rs.getObject(c);
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
	public Class getColumnClass(int column)
	{
//		System.out.println("getColumnClass("+column+")");
		try
		{
			String className = (String) _type.get(column);
			if (className.equals("java.sql.Timestamp")) return String.class;
//			if (className.equals("java.sql.Timestamp")) return java.util.Date.class;
			return Class.forName(className);
		} 
		catch (Exception e) 
		{
			e.printStackTrace();
		}
		return String.class;
	}

	public Object getValueAt(int r, int c)
	{
		ArrayList row = (ArrayList) _rows.get(r);
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
}