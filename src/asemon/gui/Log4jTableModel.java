/**
 * @author <a href="mailto:goran_schwarz@hotmail.com">Goran Schwarz</a>
 */
package asemon.gui;

import java.sql.Timestamp;
import java.util.LinkedList;

import javax.swing.table.AbstractTableModel;

public class Log4jTableModel
extends AbstractTableModel
{
    private static final long serialVersionUID = 2152458182894425275L;

    //	LogManager.
	private LinkedList _records     = new LinkedList();
	private int        _maxRecords  = 500;

	public void setMaxRecords(int max) { _maxRecords = max; }
	public int  getMaxRecords()        { return _maxRecords; }

	public void addMessage(Log4jLogRecord record)
	{
		_records.add(record);
		fireTableRowsInserted(_records.size()-1, _records.size()-1);

		if (_records.size() > _maxRecords)
		{
			_records.removeFirst();
			fireTableRowsDeleted(0, 0);
		}
//		System.out.println("addMessage(): "+record);
	}

	public Log4jLogRecord getRecord(int row)
	{
		return (Log4jLogRecord) _records.get(row);
	}

	public void clear()
	{
		_records.clear();
		fireTableDataChanged();
	}


	public Class getColumnClass(int col)
	{
		switch (col)
		{
		case 0: return Long.class;
		case 1: return String.class;
		case 2: return String.class;
		case 3: return String.class;
		case 4: return String.class;
		case 5: return String.class;
		case 6: return Boolean.class;
		case 7: return String.class;
		}
		return super.getColumnClass(col);
	}
	public String getColumnName(int col)
	{
		switch (col)
		{
		case 0: return "Seq";
		case 1: return "Time";
		case 2: return "Level";
		case 3: return "Thread Name";
		case 4: return "Class Name";
		case 5: return "Location";
		case 6: return "Thrown";
		case 7: return "Message";
		}
		return super.getColumnName(col);
	}
	public boolean isCellEditable(int rowIndex, int columnIndex)
	{
		return false;
	}
	public int getColumnCount()
	{
		return 8;
	}
	public int getRowCount()
	{
		return _records.size();
	}
	public Object getValueAt(int row, int col)
	{
		Log4jLogRecord r = (Log4jLogRecord) _records.get(row);
		switch (col)
		{
		case 0: return Long.toString(r.getSequenceNumber());
		case 1: return new Timestamp(r.getMillis());
		case 2: return r.getLevel();
		case 3: return r.getThreadDescription();
		case 4: return r.getCategory();
		case 5: return r.getLocation();
		case 6: return new Boolean( r.getThrownStackTrace() != null ? true : false);
		case 7: return r.getMessage();
//		case 4: return r.getNDC();
		}
		return null;
	}

	
}
