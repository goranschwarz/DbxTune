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
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 * 
 * DbxTune is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License
 * along with DbxTune.  If not, see <http://www.gnu.org/licenses/>.
 ******************************************************************************/
package com.dbxtune.central.pcs.objects;

import java.util.Collections;
import java.util.List;

import com.dbxtune.central.check.ReceiverAlarmCheck;
import com.dbxtune.utils.Configuration;
import com.dbxtune.utils.StringUtil;
import com.dbxtune.utils.TimeUtils;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;

@JsonInclude(JsonInclude.Include.NON_EMPTY)
@JsonPropertyOrder(value = {"serverName", "healthy", "message", "serverInfo", "alarms"})
public class HealthCheck
{
	private String               _srvName;
	private boolean              _healthy;
	private String               _message;
	private DbxCentralSessions   _serverInfo;
	private List<DbxAlarmActive> _alarms ;

	// ---------------------------
	// ----- CONSTRUCTOR
	// ---------------------------
	public HealthCheck(String srvName)
	{
		_srvName = srvName;
	}


	// ---------------------------
	// ----- GET
	// ---------------------------
	public boolean isHealthy()
	{
		return _healthy;
	}

	public String getServerName()
	{
		return _srvName;
	}

	public String getMessage()
	{
		return _message;
	}

	public DbxCentralSessions getServerInfo()
	{
		return _serverInfo;
	}

	public List<DbxAlarmActive> getAlarms()
	{
		if (_alarms == null)
			return Collections.emptyList();

		return _alarms;
	}


	// ---------------------------
	// ----- SET
	// ---------------------------
	public void setMessage(String message)
	{
		_message = message;
	}

	public void setServerInfo(DbxCentralSessions serverInfo)
	{
		_serverInfo = serverInfo;
	}

	public void setAlarms(List<DbxAlarmActive> alarmList)
	{
		_alarms = alarmList;
		
		if (_alarms == null)
			_alarms = Collections.emptyList();
	}


	// ---------------------------
	// ----- methods
	// ---------------------------
	/**
	 * Executed to determine if the field "healthy" should be set to true or false
	 * 
	 * @param ageInSec 
	 * 					<0 = do not check the receive age
	 *                   0 = calculate age by: getCollectorSampleInterval * 20 (or property: ReceiverAlarmCheck.PROPKEY_ALARM_INTERVAL_MULTIPLIER)
	 *                  >0 = receive age in seconds
 	 */
	public void checkHealth(int ageInSec)
	{
		_healthy = false;
		_message = "";
		
		if (_alarms.isEmpty())
		{
			_healthy = true;
		}
		else
		{
			for (DbxAlarmActive alarm : _alarms)
			{
				_message += alarm.getAlarmClass() + "[" + alarm.getLastData()+ "]; ";
			}
			_message = StringUtil.removeLastStr(_message, ";");
		}

		if (_serverInfo == null)
			throw new NullPointerException("_serverInfo has net yet been set.");

		// Calculate a default value for 'age'
		if (ageInSec == 0)
		{
			int sampleInterval = _serverInfo.getCollectorSampleInterval();
			int multiplier     = Configuration.getCombinedConfiguration().getIntProperty(ReceiverAlarmCheck.PROPKEY_ALARM_INTERVAL_MULTIPLIER, ReceiverAlarmCheck.DEFAULT_ALARM_INTERVAL_MULTIPLIER); // default = 20

			ageInSec = sampleInterval * multiplier;
		}
		
		// Check if we have received any data from the collector
		if (ageInSec > 0)
		{
			long lastSampleAgeInSec = _serverInfo.getLastSampleAgeInSec();
			if (lastSampleAgeInSec > ageInSec)
			{
				_healthy = false;
				if ( StringUtil.hasValue(_message) )
					_message += "; ";
				_message += "DbxCollectorNoData[No Data has been received from the collector for " + _serverInfo.getLastSampleAgeInSec() + " seconds. ("+TimeUtils.secToTimeStrLong(lastSampleAgeInSec)+") HH:MM:SS]";
			}
		}
	}

}
