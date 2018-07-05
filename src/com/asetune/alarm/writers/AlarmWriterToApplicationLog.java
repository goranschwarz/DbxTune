package com.asetune.alarm.writers;

import java.util.ArrayList;
import java.util.List;

import org.apache.log4j.Logger;

import com.asetune.Version;
import com.asetune.alarm.events.AlarmEvent;
import com.asetune.cm.CmSettingsHelper;
import com.asetune.utils.Configuration;


public class AlarmWriterToApplicationLog 
extends AlarmWriterAbstract
{
	private static Logger _logger = Logger.getLogger(AlarmWriterToApplicationLog.class);

	/*---------------------------------------------------
	** PRIVATE Methods
	**---------------------------------------------------
	*/

	/*---------------------------------------------------
	** IAlarmWriter Methods
	**---------------------------------------------------
	*/
	@Override public boolean isCallReRaiseEnabled() { return false; }
	@Override public void    printConfig() {}
	@Override public void    printFilterConfig() {}
	@Override public boolean doAlarm(AlarmEvent ae) { return true; }
	@Override public List<CmSettingsHelper> getAvailableFilters() { return new ArrayList<CmSettingsHelper>(); }

	@Override
	public String getDescription()
	{
		return "Internally used to write to the application log of "+Version.getAppName();
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
		if (alarmEvent == null)
			return;
		_logger.info("AlarmHandler: -----RAISE-----: "+alarmEvent.getMessage());
	}

	@Override
	public void reRaise(AlarmEvent alarmEvent) 
	{
		if (alarmEvent == null)
			return;
		_logger.info("AlarmHandler: -----RE-RAISE--: "+alarmEvent.getMessage());
	}

	/**
	 * A alarm has been canceled by the AlarmHandler
	 */
	@Override
	public void cancel(AlarmEvent alarmEvent) 
	{
		if (alarmEvent == null)
			return;
		_logger.info("AlarmHandler: -----CANCEL----: "+alarmEvent.getMessage());
	}

	@Override 
	public void endOfScan(List<AlarmEvent> activeAlarms) 
	{
	}
}
