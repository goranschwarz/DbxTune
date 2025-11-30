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
package com.dbxtune.alarm.events.internal;

import com.dbxtune.Version;
import com.dbxtune.alarm.events.AlarmEvent;
import com.dbxtune.utils.Configuration;

public class AlarmEventMissingH2DbFile
extends AlarmEvent 
{
	private static final long serialVersionUID = 1L;

	public static final String  PROPKEY_alarm_timeToLiveInSeconds = "AlarmEventMissingH2DbFile.timeToLive.seconds";
	public static final int     DEFAULT_alarm_timeToLiveInSeconds = 1800; // 30 minutes


//	/**
//	 * Putting this at the Alarm Queue will do the same as calling method endOfScan()
//	 */
//	public AlarmEventMissingH2DbFile()
//	{
//		super(
//				"AlarmHandler", // dbmsVendor, 
//				"Stop",    // serviceName, 
//				"Stop",    // serviceInfo,
//				null,
//				AlarmEvent.Category.INTERNAL,
//				AlarmEvent.Severity.UNKNOWN, 
//				AlarmEvent.ServiceState.UNKNOWN, 
//				"Stop",    // description
//				null
//			); 
//	}
	public AlarmEventMissingH2DbFile(String srvName, String subSystemName, String currentUrl, String message)
	{
		super(
				Version.getAppName(), // serviceType
				srvName,              // serviceName
				subSystemName,        // serviceInfo
				null,                 // extraInfo
				AlarmEvent.Category.OTHER,
				AlarmEvent.Severity.WARNING, 
				AlarmEvent.ServiceState.UP, 
				message,
				0
				);

		// Set: FIXED Time To Live
		// Entries are a "one time shot" (it's only seen once)... so how long should that entry be "active"
		int ttlSec = Configuration.getCombinedConfiguration().getIntProperty(PROPKEY_alarm_timeToLiveInSeconds, DEFAULT_alarm_timeToLiveInSeconds);
		setTimeToLive( ttlSec * 1000 );

		// Set data
		setData("url="+currentUrl);

		// Set the description 
//		setDescription("");
	}
}
