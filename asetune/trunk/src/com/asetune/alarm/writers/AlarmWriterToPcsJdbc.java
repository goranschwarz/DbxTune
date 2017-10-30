package com.asetune.alarm.writers;

import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.apache.log4j.Logger;

import com.asetune.Version;
import com.asetune.alarm.events.AlarmEvent;
import com.asetune.cm.CmSettingsHelper;
import com.asetune.utils.Configuration;

/**
 * Internal writer that will add records to the Persistent Counter Storage <br>
 * NOTE: Not yet implemented, just copied from AlarmWriterToTableModel
 */
public class AlarmWriterToPcsJdbc 
extends AlarmWriterAbstract
{
	private static Logger _logger = Logger.getLogger(AlarmWriterToPcsJdbc.class);

	private ArrayList<AlarmEventWrapper> _rows = new ArrayList<>();

	private boolean _enabled = true;
	public  boolean isEnabled()                 { return _enabled; }
	public  void    setEnabled(boolean enabled) { _enabled = enabled; }

	public static class AlarmEventWrapper
	{
		private Timestamp  _dateAdded;
		private String     _action;
		private AlarmEvent _alarmEvent;

		public Timestamp  getAddDate()    { return _dateAdded;  } 
		public String     getAction()     { return _action;     } 
		public AlarmEvent getAlarmEvent() { return _alarmEvent; } 

		public AlarmEventWrapper(AlarmEvent alarmEvent, String action)
		{
			_alarmEvent = alarmEvent;
			_action     = action;
			_dateAdded  = new Timestamp( System.currentTimeMillis() );
		}
	}

	//////////////////////////////////////////////
	//// Instance
	//////////////////////////////////////////////
	// implements singleton pattern
	private static AlarmWriterToPcsJdbc _instance = null;

	public static AlarmWriterToPcsJdbc getInstance()
	{
		return _instance;
	}

	public static boolean hasInstance()
	{
		return (_instance != null);
	}

	public static void setInstance(AlarmWriterToPcsJdbc inst)
	{
		_instance = inst;
	}

	
	/*---------------------------------------------------
	** Methods
	**---------------------------------------------------
	*/
	public synchronized void addEntry(AlarmEvent entry, String action)
	{
		if ( ! isEnabled() )
			return;

		_rows.add( new AlarmEventWrapper(entry, action) );
	}

	/**
	 * Get a List of all entries
	 * 
	 * @param makeNewList purge the old list (actually returns current list, and creates a new List for new events)
	 * @return
	 */
	public synchronized List<AlarmEventWrapper> getList(boolean makeNewList)
	{
		// If empty... exit early
		if (_rows.isEmpty())
			return Collections.emptyList();
		
		List<AlarmEventWrapper> retList = _rows;

		// start a new list
		if (makeNewList)
			_rows = new ArrayList<>();

		return retList;
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

	@Override
	public String getDescription()
	{
		return "Internally used by the 'Persistent Counter Storage' when using NO-GUI Mode of "+Version.getAppName();
	}
	
	@Override
	public void init(Configuration conf) 
	throws Exception 
	{
		super.init(conf);

		_logger.info("Initializing the AlarmHandler.AlarmWriter component named '"+getName()+"'.");
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
		_logger.debug     (getName()+": -----RAISE-----: "+alarmEvent);

		addEntry(alarmEvent, ACTION_RAISE);
	}

	@Override
	public void reRaise(AlarmEvent alarmEvent) 
	{
		_logger.debug     (getName()+": -----RE-RAISE-----: "+alarmEvent);

		addEntry(alarmEvent, ACTION_RE_RAISE);
	}

	/**
	 * A alarm has been canceled by the AlarmHandler
	 */
	@Override
	public void cancel(AlarmEvent alarmEvent) 
	{
		_logger.debug(     getName()+": -----CANCEL-----: "+alarmEvent);

		// hmmm...
		if (isCallReRaiseEnabled())
		{
			//_historyTableModel.markReRaisedAsCancel(alarmEvent);
		}

		addEntry(alarmEvent, ACTION_CANCEL);
	}

	@Override 
	public void endOfScan(List<AlarmEvent> activeAlarms) 
	{
		_logger.debug     (getName()+": -----END-OF-SCAN-----: activeAlarms Count="+activeAlarms.size());

		AlarmEvent eosEvent = new AlarmEventEndOfScan(activeAlarms.size());
		addEntry(eosEvent, "END-OF-SCAN");
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
				AlarmEvent.Severity.INFO, 
				AlarmEvent.ServiceState.UNKNOWN, 
				"EndOfScan activeAlarmSize="+activeAlarmSize);
			
			setData(activeAlarmSize);
		}

		@Override public boolean isActive()  { return false; }
		@Override public long getCrAgeInMs() { return 0; }
	}
}
