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
package com.dbxtune.alarm.events.dbxc;

import com.dbxtune.Version;
import com.dbxtune.alarm.events.AlarmEvent;
import com.dbxtune.utils.TimeUtils;

public class AlarmEventDbxCollectorNoData
extends AlarmEvent 
{
	private static final long serialVersionUID = 1L;


	/**
	 * DBX Central PCS Receiver has not received any data from a specific collector in X seconds
	 * 
	 * @param srvName
	 * @param secSinceLastRecv
	 * @param threshold
	 */
	public AlarmEventDbxCollectorNoData(String srvName, long secSinceLastRecv, long thresholdInSec)
	{
		super(
				Version.getAppName(), // serviceType
				srvName,              // serviceName
				"DbxCollector",       // serviceInfo
				null,                 // extraInfo
				AlarmEvent.Category.OTHER,
				AlarmEvent.Severity.WARNING, 
				AlarmEvent.ServiceState.UP, 
				"DbxCentral Receiver has not received any data from instance '" + srvName + "' for " + TimeUtils.secToTimeStrLong(secSinceLastRecv)+" (HH:MM:SS) (thresholdInSec="+thresholdInSec+")",
				thresholdInSec
				);

		// Adjust the Alarm Full Duration with X seconds
		setFullDurationAdjustmentInSec( (int) thresholdInSec );
		
		// Set: Time To Live if postpone is enabled
		//setTimeToLive(cm);

		// Set the raw data carrier
		setData("srvName="+srvName+",secSinceLastRecv="+secSinceLastRecv);
	}
}
