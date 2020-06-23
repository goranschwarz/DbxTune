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
package com.asetune.alarm.ui.view;

import java.util.List;

import javax.swing.table.AbstractTableModel;

import com.asetune.alarm.AlarmHandler;
import com.asetune.alarm.events.AlarmEvent;

public class AlarmActiveTableModel
extends AbstractTableModel
{
	private static final long serialVersionUID = 1L;

//	private static final String[] TAB_HEADER = {"AlarmClass", "serviceType", "serviceName", "serviceInfo", "extraInfo", "category", "severity", "state", "repeatCnt", "duration", "crTime", "reRaiseTime", "timeToLive", "data", "LastData", "description", "LastDescription", "extendedDescription", "LastExtendedDescription"};
	private static final String[] TAB_HEADER = {"alarmClass", "serviceType", "serviceName", "serviceInfo", "extraInfo", "category", "severity", "state", "repeatCnt", "duration", "crTime", "reRaiseTime", "timeToLive", "data", "lastData", "description", "lastDescription", "extendedDescription", "lastExtendedDescription"};
	public static final int TAB_POS_ALARM_CLASS               = 0;
	public static final int TAB_POS_SERVICE_TYPE              = 1;
	public static final int TAB_POS_SERVICE_NAME              = 2;
	public static final int TAB_POS_SERVICE_INFO              = 3;
	public static final int TAB_POS_EXTRA_INFO                = 4;
	public static final int TAB_POS_CATEGORY                  = 5;
	public static final int TAB_POS_SEVERITY                  = 6;
	public static final int TAB_POS_STATE                     = 7;
	public static final int TAB_POS_REPEAT_COUNT              = 8;
	public static final int TAB_POS_DURATION                  = 9;
	public static final int TAB_POS_CR_TIME                   = 10;
	public static final int TAB_POS_RE_RAISE_TIME             = 11;
	public static final int TAB_POS_TIME_TO_LIVE              = 12;
	public static final int TAB_POS_DATA                      = 13;
	public static final int TAB_POS_LAST_DATA                 = 14;
	public static final int TAB_POS_DESCRIPTION               = 15;
	public static final int TAB_POS_LAST_DESCRIPTION          = 16;
	public static final int TAB_POS_EXTENDED_DESCRIPTION      = 17;
	public static final int TAB_POS_LAST_EXTENDED_DESCRIPTION = 18;

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
	}

	@Override
	public int getRowCount()
	{
		if (AlarmHandler.hasInstance())
		{
			return AlarmHandler.getInstance().getAlarmList().size();
		}
		return 0;
	}

	@Override
	public Object getValueAt(int row, int column)
	{
		if (AlarmHandler.hasInstance())
		{
			List<AlarmEvent> list = AlarmHandler.getInstance().getAlarmList();

			// Get the Alarm in a "safe" way... if the ActiveAlarm List rowcount must have changed size the JTable got the rowcount.
			AlarmEvent ae;
			try { ae = list.get(row); }
			catch(RuntimeException rte)
			{
				System.out.println("AlarmActiveTableModel: getValueAt(row="+row+", column="+column+"): list.size()="+list.size()+"  The ActiveAlarm List rowcount must have changed size the JTable got the rowcount. Caught: "+ rte);
				return null;
			}


			switch (column)
			{
			case TAB_POS_ALARM_CLASS               : return ae.getAlarmClassAbriviated();
			case TAB_POS_SERVICE_TYPE              : return ae.getServiceType();
			case TAB_POS_SERVICE_NAME              : return ae.getServiceName();
			case TAB_POS_SERVICE_INFO              : return ae.getServiceInfo();
			case TAB_POS_EXTRA_INFO                : return ae.getExtraInfo();
			case TAB_POS_CATEGORY                  : return ae.getCategory();
			case TAB_POS_SEVERITY                  : return ae.getSeverity();
			case TAB_POS_STATE                     : return ae.getState();
			case TAB_POS_REPEAT_COUNT              : return ae.getReRaiseCount();
			case TAB_POS_DURATION                  : return ae.getDuration();
			case TAB_POS_CR_TIME                   : return ae.getCrTimeStr();
			case TAB_POS_RE_RAISE_TIME             : return ae.getReRaiseTimeStr();
			case TAB_POS_TIME_TO_LIVE              : return ae.getTimeToLive();
			case TAB_POS_DATA                      : return ae.getData();
			case TAB_POS_LAST_DATA                 : return ae.getReRaiseData();
			case TAB_POS_DESCRIPTION               : return ae.getDescription();
			case TAB_POS_LAST_DESCRIPTION          : return ae.getReRaiseDescription();
			case TAB_POS_EXTENDED_DESCRIPTION      : return ae.getExtendedDescription();
			case TAB_POS_LAST_EXTENDED_DESCRIPTION : return ae.getReRaiseExtendedDescription();
			}
		}
		return null;
	}

	@Override
	public Class<?> getColumnClass(int column)
	{
		switch (column)
		{
//		case TAB_POS_ALARM_CLASS               : return String.class;
//		case TAB_POS_SERVICE_TYPE              : return String.class;
//		case TAB_POS_SERVICE_NAME              : return String.class;
//		case TAB_POS_SERVICE_INFO              : return String.class;
//		case TAB_POS_EXTRA_INFO                : return Object.class;
//		case TAB_POS_SEVERITY                  : return Object.class;
//		case TAB_POS_STATE                     : return Object.class;
		case TAB_POS_REPEAT_COUNT              : return Number.class;
//		case TAB_POS_DURATION                  : return Number.class;
//		case TAB_POS_CR_TIME                   : return Number.class;
//		case TAB_POS_RE_RAISE_TIME             : return String.class;
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
