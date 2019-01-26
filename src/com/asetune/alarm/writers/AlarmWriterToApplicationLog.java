/*******************************************************************************
 * Copyright (C) 2010-2019 Goran Schwarz
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
