/*******************************************************************************
 * Copyright (C) 2010-2025 Goran Schwarz
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
package com.dbxtune.alarm.writers;

import java.lang.invoke.MethodHandles;
import java.util.ArrayList;
import java.util.List;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.dbxtune.Version;
import com.dbxtune.alarm.events.AlarmEvent;
import com.dbxtune.alarm.ui.view.AlarmActiveTableModel;
import com.dbxtune.alarm.ui.view.AlarmHistoryTableModel;
import com.dbxtune.cm.CmSettingsHelper;
import com.dbxtune.utils.Configuration;
import com.dbxtune.utils.SwingUtils;


public class AlarmWriterToTableModel 
extends AlarmWriterAbstract
{
	private static final Logger _logger = LogManager.getLogger(MethodHandles.lookup().lookupClass());

	private AlarmHistoryTableModel _historyTableModel;
	private AlarmActiveTableModel  _activeTableModel;
	
	//////////////////////////////////////////////
	//// Instance
	//////////////////////////////////////////////
	// implements singleton pattern
	private static AlarmWriterToTableModel _instance = null;

	public static AlarmWriterToTableModel getInstance()
	{
		return _instance;
	}

	public static boolean hasInstance()
	{
		return (_instance != null);
	}

	public static void setInstance(AlarmWriterToTableModel inst)
	{
		_instance = inst;
	}

	
	/*---------------------------------------------------
	** Methods
	**---------------------------------------------------
	*/
	public AlarmActiveTableModel getActiveTableModel()
	{
		return _activeTableModel;
	}

	public AlarmHistoryTableModel getHistoryTableModel()
	{
		return _historyTableModel;
	}

	
	/*---------------------------------------------------
	** PRIVATE Methods
	**---------------------------------------------------
	*/

	/*---------------------------------------------------
	** IAlarmWriter Methods
	**---------------------------------------------------
	*/
	@Override public boolean isCallReRaiseEnabled() { return true; }
	@Override public void    printConfig() {}
	@Override public void    printFilterConfig() {}
	@Override public boolean doAlarm(AlarmEvent ae) { return true; }
	@Override public List<CmSettingsHelper> getAvailableFilters() { return new ArrayList<CmSettingsHelper>(); }

	@Override
	public String getDescription()
	{
		return "Internally used by the 'Alarm View' dialog when using GUI Mode of "+Version.getAppName();
	}
	
	@Override
	public void init(Configuration conf) 
	throws Exception 
	{
		super.init(conf);

		_logger.info("Initializing the AlarmHandler.AlarmWriter component named '"+getName()+"'.");

		_historyTableModel = new AlarmHistoryTableModel();
		_activeTableModel  = new AlarmActiveTableModel();
	}

	@Override
	public List<CmSettingsHelper> getAvailableSettings()
	{
		ArrayList<CmSettingsHelper> list = new ArrayList<>();
		return list;
	}

	@Override
	public void raise(AlarmEvent alarmEvent) 
	{
//		System.out.println(getName()+": -----RAISE-----: "+alarmEvent);
		_logger.debug     (getName()+": -----RAISE-----: "+alarmEvent);

		_historyTableModel.addEntry(alarmEvent, ACTION_RAISE);
		SwingUtils.fireTableDataChanged(_activeTableModel);
		SwingUtils.fireTableDataChanged(_historyTableModel);
	}

	@Override
	public void reRaise(AlarmEvent alarmEvent) 
	{
//		System.out.println(getName()+": -----RE-RAISE-----: "+alarmEvent);
		_logger.debug     (getName()+": -----RE-RAISE-----: "+alarmEvent);

		_historyTableModel.addEntry(alarmEvent, ACTION_RE_RAISE);
		SwingUtils.fireTableDataChanged(_activeTableModel);
		SwingUtils.fireTableDataChanged(_historyTableModel);
	}

	/**
	 * A alarm has been canceled by the AlarmHandler
	 */
	@Override
	public void cancel(AlarmEvent alarmEvent) 
	{
//		System.out.println(getName()+": -----CANCEL-----: "+alarmEvent);
		_logger.debug(     getName()+": -----CANCEL-----: "+alarmEvent);

		// hmmm...
		if (isCallReRaiseEnabled())
		{
			_historyTableModel.markReRaisedAsCancel(alarmEvent);
		}

		_historyTableModel.addEntry(alarmEvent, ACTION_CANCEL);
		SwingUtils.fireTableDataChanged(_activeTableModel);
		SwingUtils.fireTableDataChanged(_historyTableModel);
	}

	@Override 
	public void endOfScan(List<AlarmEvent> activeAlarms) 
	{
//		System.out.println(getName()+": -----END-OF-SCAN-----: activeAlarms Count="+activeAlarms.size());
		_logger.debug     (getName()+": -----END-OF-SCAN-----: activeAlarms Count="+activeAlarms.size());

		AlarmEvent eosEvent = new AlarmEventEndOfScan(activeAlarms.size());
		_historyTableModel.addEntry(eosEvent, "END-OF-SCAN");

		SwingUtils.fireTableDataChanged(_activeTableModel);
		SwingUtils.fireTableDataChanged(_historyTableModel);
	}
	
	private class AlarmEventEndOfScan
	extends AlarmEvent
	{
		private static final long serialVersionUID = 1L;

		public AlarmEventEndOfScan(int activeAlarmSize)
		{
			super(
				"", // serviceType
				"", // serviceName
				"", // serviceInfo
				"", // extraInfo
				AlarmEvent.Category.INTERNAL,
				AlarmEvent.Severity.INFO, 
				AlarmEvent.ServiceState.UNKNOWN, 
				"EndOfScan activeAlarmSize="+activeAlarmSize,
				null);
			
			setData(activeAlarmSize);
		}

		@Override public boolean isActive()  { return false; }
		@Override public long getCrAgeInMs() { return 0; }
	}
}
