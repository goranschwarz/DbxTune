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

import com.dbxtune.alarm.events.AlarmEvent;
import com.dbxtune.cm.CmSettingsHelper;
import com.dbxtune.utils.Configuration;


public class AlarmWriterToStdout 
extends AlarmWriterAbstract
{
	private static final Logger _logger = LogManager.getLogger(MethodHandles.lookup().lookupClass());

	private String _name = "AlarmWriterToStdout";
	
	@Override
	public boolean isCallReRaiseEnabled()
	{
//		return true;
		return false;
	}

	@Override
	public String getDescription()
	{
		return "Simply write all Events to stdout, can be used for debugging.";
	}
	
	/**
	 * Initialize the component
	 */
	@Override
	public void init(Configuration conf) 
	throws Exception 
	{
		super.init(conf);

		String propPrefix = "AlarmWriterToStdout";
		String propname = null;

		// property: name
		propname = propPrefix+".name";
		_name = conf.getProperty(propname, _name);
	}

	@Override
	public void printConfig()
	{
		_logger.info("Initializing the AlarmHandler.AlarmWriter component named '"+getName()+"'.");
		_logger.info("                                          This component has no configuration ");
	}

	@Override
	public List<CmSettingsHelper> getAvailableSettings()
	{
		ArrayList<CmSettingsHelper> list = new ArrayList<>();
		
		return list;
	}

	/**
	 * A alarm has been raised by the AlarmHandler
	 */
	@Override
	public void raise(AlarmEvent alarmEvent) 
	{
		System.out.println(getName()+": -----RAISE-----: "+alarmEvent);
		_logger.debug     (getName()+": -----RAISE-----: "+alarmEvent);
	}

	@Override
	public void reRaise(AlarmEvent alarmEvent)
	{
		System.out.println(getName()+": -----RE-RAISE-----: "+alarmEvent);
		_logger.debug     (getName()+": -----RE-RAISE-----: "+alarmEvent);
	}

	/**
	 * A alarm has been canceled by the AlarmHandler
	 */
	@Override
	public void cancel(AlarmEvent alarmEvent) 
	{
		System.out.println(getName()+": -----CANCEL-----: "+alarmEvent);
		_logger.debug(     getName()+": -----CANCEL-----: "+alarmEvent);
	}

	/**
	 * At the end of a scan for suspected components, this method is called 
	 * and it can be used for "sending of a batch" of events or flushing a file
	 * or wahtever you want to do.
	 * Even if no alarms was raised or canceled during the "scan", this method
	 * will always be called after all components has been checked.
	 */
	@Override
	public void endOfScan(List<AlarmEvent> activeAlarms)
	{
		System.out.println(getName()+": -----END-OF-SCAN-----.");
		_logger.debug     (getName()+": -----END-OF-SCAN-----.");
	}

	/**
	 * When the AlarmHandler initiates, it restores old alarms from privious sessions
	 * this method is called from the AlarmHandler.init() after the old alarms has been
	 * restored. 
	 * <p>
	 * @param activeAlarms a list of alarms that was restored 
	 *                     Note: do NOT remove entriws from the list, 
	 *                     it should only be read. If you want to manipulate it.
	 *                     Create your own copy of the list.
	 */
	@Override
	public void restoredAlarms(List<AlarmEvent> restoredAlarms)
	{
		System.out.println(getName()+": -----RESTORE-ALARMS-----.");
		_logger.debug     (getName()+": -----RESTORE-ALARMS-----.");
	}
	
	/**
	 * What is this AlarmWriter named to...
	 */
	@Override
	public String getName() 
	{
		return _name;
	}
}
