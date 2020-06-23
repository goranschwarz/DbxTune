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

import java.sql.Timestamp;
import java.util.LinkedList;

import javax.swing.table.AbstractTableModel;

public class Log4jTableModel
extends AbstractTableModel
{
    private static final long serialVersionUID = 2152458182894425275L;

    //	LogManager.
	private LinkedList<Log4jLogRecord> _records = new LinkedList<Log4jLogRecord>();
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
		
//		if (MainFrame.hasInstance())
//			MainFrame.getInstance().actionPerformed(new ActionEvent(this, 0, MainFrame.ACTION_OPEN_LOG_VIEW));
	}

	public Log4jLogRecord getRecord(int row)
	{
		return _records.get(row);
	}

	public void clear()
	{
		_records.clear();
		fireTableDataChanged();
	}


	@Override
	public Class<?> getColumnClass(int col)
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
	@Override
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
	@Override
	public boolean isCellEditable(int rowIndex, int columnIndex)
	{
		return false;
	}
	@Override
	public int getColumnCount()
	{
		return 8;
	}
	@Override
	public int getRowCount()
	{
		return _records.size();
	}
	@Override
	public Object getValueAt(int row, int col)
	{
		Log4jLogRecord r = _records.get(row);
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
