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
package com.asetune.alarm.events.dbxc;

import com.asetune.Version;
import com.asetune.alarm.events.AlarmEvent;
import com.asetune.utils.TimeUtils;

public class AlarmEventHttpDestinationDown
extends AlarmEvent 
{
	private static final long serialVersionUID = 1L;


	/**
	 * HTTP Destination seems to be down
	 * 
	 * @param name
	 * @param url
	 * @param secSinceLastSuccessSend
	 * @param threshold
	 */
	public AlarmEventHttpDestinationDown(String collectorName, String name, String url, long secSinceLastSuccessSend, int threshold)
	{
		super(
				Version.getAppName(), // serviceType
				name,                 // serviceName
				collectorName,        // serviceInfo
				url,                  // extraInfo
				AlarmEvent.Category.OTHER,
				AlarmEvent.Severity.WARNING, 
				AlarmEvent.ServiceState.UP, 
				"HTTP Destination could not send data to '" + name + "' with URL '" + url + "' for " + TimeUtils.msToTimeStr("%HH:%MM:%SS", secSinceLastSuccessSend*1000)+" (HH:MM:SS) (thresholdInSec="+threshold+")",
				threshold
				);

		// Set: Time To Live if postpone is enabled
		//setTimeToLive(cm);

		// Set the raw data carrier
		setData("name="+name+", url='"+url+"', secSinceLastSuccessSend="+secSinceLastSuccessSend); // note: limit is 80 characters...
	}
}
