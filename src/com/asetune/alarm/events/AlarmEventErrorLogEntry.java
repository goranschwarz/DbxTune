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
package com.asetune.alarm.events;

import com.asetune.Version;
import com.asetune.cm.CountersModel;
import com.asetune.utils.Configuration;

public class AlarmEventErrorLogEntry
extends AlarmEvent 
{
	private static final long serialVersionUID = 1L;

	public static final String  PROPKEY_alarm_timeToLiveInSeconds = "AlarmEventErrorLogEntry.timeToLive.seconds";
	public static final int     DEFAULT_alarm_timeToLiveInSeconds = 1800; // 30 minutes

	/**
	 * 
	 * @param cm
	 * @param alarmSeverity
	 * @param errorNumber
	 * @param errorSeverity
	 * @param errorMessage
	 * @param threshold
	 */
	public AlarmEventErrorLogEntry(CountersModel cm, Severity alarmSeverity, int errorNumber, int errorSeverity, String errorMessage, int severityThreshold)
	{
		super(
				Version.getAppName(), // serviceType
				cm.getServerName(),   // serviceName
				cm.getName(),         // serviceInfo
				errorNumber,          // extraInfo
				AlarmEvent.Category.OTHER,
				alarmSeverity, 
				AlarmEvent.ServiceState.UP, 
				"done later in constructor...",
				severityThreshold
				);

		// Set: FIXED Time To Live
		// Errorlog entries are a "one time shot" (it's only seen once)... so how long should that entry be "active"
		int ttlSec = Configuration.getCombinedConfiguration().getIntProperty(PROPKEY_alarm_timeToLiveInSeconds, DEFAULT_alarm_timeToLiveInSeconds);
		setTimeToLive( ttlSec * 1000 );

		// Set data
		setData("Num="+errorNumber+", Severity="+errorSeverity+", Text=" + errorMessage.trim());

		// Set the description 
		setDescription("Num="+errorNumber+", Severity="+errorSeverity+", Text=" + errorMessage.trim()+ ", ExtraInfo=[AlarmEventSeverity="+alarmSeverity+", "+PROPKEY_alarm_timeToLiveInSeconds+"="+ttlSec+", severityThreshold="+severityThreshold+"]");
	}

	/**
	 * Various error messages the can be of interest
	 * 
	 * @param cmErrorLog
	 * @param severity
	 * @param errorTxt
	 */
	public AlarmEventErrorLogEntry(CountersModel cm, Severity alarmSeverity, String searchFor, String errorTxt)
	{
		super(
				Version.getAppName(), // serviceType
				cm.getServerName(),   // serviceName
				cm.getName(),         // serviceInfo
				searchFor,            // extraInfo
				AlarmEvent.Category.OTHER,
				alarmSeverity, 
				AlarmEvent.ServiceState.UP, 
				errorTxt,
				null
				);

		// Set: FIXED Time To Live
		// Errorlog entries are a "one time shot" (it's only seen once)... so how long should that entry be "active"
		int ttlSec = Configuration.getCombinedConfiguration().getIntProperty(PROPKEY_alarm_timeToLiveInSeconds, DEFAULT_alarm_timeToLiveInSeconds);
		setTimeToLive( ttlSec * 1000 );
	}
}
