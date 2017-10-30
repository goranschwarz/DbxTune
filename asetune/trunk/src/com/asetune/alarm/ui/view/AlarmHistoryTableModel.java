package com.asetune.alarm.ui.view;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;

import javax.swing.table.AbstractTableModel;

import com.asetune.alarm.events.AlarmEvent;
import com.asetune.alarm.writers.AlarmWriterAbstract;
import com.asetune.utils.SwingUtils;


public class AlarmHistoryTableModel
extends AbstractTableModel
//implements TableModel
{
	private static final long serialVersionUID = 1L;

//	@Override
//	public int getRowCount()
//	{
//		return 0;
//	}
//
//	@Override
//	public int getColumnCount()
//	{
//		return 0;
//	}
//
//	@Override
//	public Object getValueAt(int rowIndex, int columnIndex)
//	{
//		return null;
//	}
//
//	@Override
//	public String getColumnName(int columnIndex)
//	{
//		return null;
//	}
//
//	@Override
//	public Class<?> getColumnClass(int columnIndex)
//	{
//		return null;
//	}
//
//	@Override
//	public boolean isCellEditable(int rowIndex, int columnIndex)
//	{
//		return false;
//	}
//
//	@Override
//	public void setValueAt(Object aValue, int rowIndex, int columnIndex)
//	{
//	}
//
//	@Override
//	public void addTableModelListener(TableModelListener l)
//	{
//	}
//
//	@Override
//	public void removeTableModelListener(TableModelListener l)
//	{
//	}
	
	private static class AlarmEventWrapper
	{
		private String     _dateAdded;
		private String     _action;
		private AlarmEvent _alarmEvent;

		private static SimpleDateFormat _dateFormater = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS");
		
		public AlarmEventWrapper(AlarmEvent alarmEvent, String action)
		{
			_alarmEvent = alarmEvent;
			_action     = action;
			_dateAdded  = _dateFormater.format( new Date(System.currentTimeMillis()) );
		}
	}
	private static final String[] TAB_HEADER = {"Event Time", "Action", "isActive", "AlarmClass", "serviceType", "serviceName", "serviceInfo", "extraInfo", "severity", "state", "repeatCnt", "duration", "createTime", "cancelTime", "timeToLive", "Data", "LastData", "description", "LastDescription", "extendedDescription", "LastExtendedDescription"};
	public static final int TAB_POS_EVENT_TIME                = 0;
	public static final int TAB_POS_ACTION                    = 1;
	public static final int TAB_POS_IS_ACTIVE                 = 2;
	public static final int TAB_POS_ALARM_CLASS               = 3;
	public static final int TAB_POS_SERVICE_TYPE              = 4;
	public static final int TAB_POS_SERVICE_NAME              = 5;
	public static final int TAB_POS_SERVICE_INFO              = 6;
	public static final int TAB_POS_EXTRA_INFO                = 7;
	public static final int TAB_POS_SEVERITY                  = 8;
	public static final int TAB_POS_STATE                     = 9;
	public static final int TAB_POS_REPEAT_COUNT              = 10;
	public static final int TAB_POS_DURATION                  = 11;
	public static final int TAB_POS_CR_TIME                   = 12;
	public static final int TAB_POS_CANCEL_TIME               = 13;
	public static final int TAB_POS_TIME_TO_LIVE              = 14;
	public static final int TAB_POS_DATA                      = 15;
	public static final int TAB_POS_LAST_DATA                 = 16;
	public static final int TAB_POS_DESCRIPTION               = 17;
	public static final int TAB_POS_LAST_DESCRIPTION          = 18;
	public static final int TAB_POS_EXTENDED_DESCRIPTION      = 19;
	public static final int TAB_POS_LAST_EXTENDED_DESCRIPTION = 20;

	private ArrayList<AlarmEventWrapper> _rows = new ArrayList<>();
	private boolean _hasChanged = false;

	public AlarmHistoryTableModel()
	{
	}

	public boolean isChanged()
	{
		return _hasChanged;
	}

	public void setChanged(boolean changed)
	{
		_hasChanged = changed;
	}

	public void markReRaisedAsCancel(AlarmEvent alarmEvent)
	{
		for (AlarmEventWrapper aew : _rows)
		{
			if (AlarmWriterAbstract.ACTION_RE_RAISE.equals(aew._action) && aew._alarmEvent.equals(alarmEvent))
				aew._alarmEvent.markCancel();
		}
	}

	public void clear(boolean fireChange)
	{
		_rows.clear();
		setChanged(true);
		if (fireChange)
			fireTableDataChanged();
	}

	public void reload()
	{
		clear(false);
		//loadModel(true);
	}

	public void addEntry(AlarmEvent entry, String action)
	{
		_rows.add( new AlarmEventWrapper(entry, action));
		setChanged(true);
		
		int rowNum = _rows.size()-1;
		SwingUtils.fireTableRowsInserted(this, rowNum, rowNum);
	}

	@Override
	public int getColumnCount()
	{
		return TAB_HEADER.length;
	}

	@Override
	public String getColumnName(int column)
	{
		if (column > TAB_HEADER.length-1)
			return null;
		
		return TAB_HEADER[column];
	}

	@Override
	public boolean isCellEditable(int row, int column)
	{
		return false;
//		if (column == TAB_POS_SYSTEM)
//			return false;
//		return true;
	}

	@Override
	public int getRowCount()
	{
		return _rows.size();
	}

	@Override
	public Object getValueAt(int row, int column)
	{
		AlarmEventWrapper entry = _rows.get(row);
		switch (column)
		{
		case TAB_POS_EVENT_TIME                : return entry._dateAdded;
		case TAB_POS_ACTION                    : return entry._action;
                                               
		case TAB_POS_IS_ACTIVE                 : return entry._alarmEvent.isActive();
		case TAB_POS_ALARM_CLASS               : return entry._alarmEvent.getAlarmClassAbriviated();
		case TAB_POS_SERVICE_TYPE              : return entry._alarmEvent.getServiceType();
		case TAB_POS_SERVICE_NAME              : return entry._alarmEvent.getServiceName();
		case TAB_POS_SERVICE_INFO              : return entry._alarmEvent.getServiceInfo();
		case TAB_POS_EXTRA_INFO                : return entry._alarmEvent.getExtraInfo();
		case TAB_POS_SEVERITY                  : return entry._alarmEvent.getSeverity();
		case TAB_POS_STATE                     : return entry._alarmEvent.getState();
		case TAB_POS_REPEAT_COUNT              : return entry._alarmEvent.getReRaiseCount();
		case TAB_POS_DURATION                  : return entry._alarmEvent.getDuration();
		case TAB_POS_CR_TIME                   : return entry._alarmEvent.getCrTimeStr();
		case TAB_POS_CANCEL_TIME               : return entry._alarmEvent.getCancelTimeStr();
		case TAB_POS_TIME_TO_LIVE              : return entry._alarmEvent.getTimeToLive();
		case TAB_POS_DATA                      : return entry._alarmEvent.getData();
		case TAB_POS_LAST_DATA                 : return entry._alarmEvent.getReRaiseData();
		case TAB_POS_DESCRIPTION               : return entry._alarmEvent.getDescription();
		case TAB_POS_LAST_DESCRIPTION          : return entry._alarmEvent.getReRaiseDescription();
		case TAB_POS_EXTENDED_DESCRIPTION      : return entry._alarmEvent.getExtendedDescription();
		case TAB_POS_LAST_EXTENDED_DESCRIPTION : return entry._alarmEvent.getReRaiseExtendedDescription();
		}
		return null;
	}

	@Override
	public Class<?> getColumnClass(int column)
	{
		switch (column)
		{
//		case TAB_POS_EVENT_TIME                : return String.class;
//		case TAB_POS_ACTION                    : return String.class;
                                               
		case TAB_POS_IS_ACTIVE                 : return Boolean.class;
//		case TAB_POS_ALARM_CLASS               : return String.class;
//		case TAB_POS_SERVICE_TYPE              : return String.class;
//		case TAB_POS_SERVICE_NAME              : return String.class;
//		case TAB_POS_SERVICE_INFO              : return String.class;
//		case TAB_POS_EXTRA_INFO                : return Object.class;
//		case TAB_POS_SEVERITY                  : return Object.class;
//		case TAB_POS_STATE                     : return Object.class;
		case TAB_POS_REPEAT_COUNT              : return Number.class;
//		case TAB_POS_DURATION                  : return Number.class;
//		case TAB_POS_CR_TIME                   : return String.class;
//		case TAB_POS_CANCEL_TIME               : return String.class;
		case TAB_POS_TIME_TO_LIVE              : return Number.class;
//		case TAB_POS_DATA                      : return Object.class;
//		case TAB_POS_LAST_DATA                 : return Object.class;
//		case TAB_POS_DESCRIPTION               : return String.class;
//		case TAB_POS_LAST_DESCRIPTION          : return String.class;
//		case TAB_POS_EXTENDED_DESCRIPTION      : return String.class;
//		case TAB_POS_LAST_EXTENDED_DESCRIPTION : return String.class;
		}

		return super.getColumnClass(column);
	}
}
