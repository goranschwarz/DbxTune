package com.asetune.alarm.writers;

import java.util.ArrayList;
import java.util.List;

import org.apache.log4j.Logger;

import com.asetune.Version;
import com.asetune.alarm.events.AlarmEvent;
import com.asetune.alarm.ui.view.AlarmActiveTableModel;
import com.asetune.alarm.ui.view.AlarmHistoryTableModel;
import com.asetune.cm.CmSettingsHelper;
import com.asetune.utils.Configuration;
import com.asetune.utils.SwingUtils;


public class AlarmWriterToTableModel 
extends AlarmWriterAbstract
{
	private static Logger _logger = Logger.getLogger(AlarmWriterToTableModel.class);

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
