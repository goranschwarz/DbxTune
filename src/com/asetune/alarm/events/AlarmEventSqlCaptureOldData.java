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
package com.asetune.alarm.events;

import com.asetune.Version;
import com.asetune.cm.CountersModel;

public class AlarmEventSqlCaptureOldData
extends AlarmEvent 
{
	private static final long serialVersionUID = 1L;

	// Set: Time To Live if postpone is enabled
	public AlarmEventSqlCaptureOldData(CountersModel cm, int ageInSec, int thresholdInSec)
	{
		super(
				Version.getAppName(), // serviceType
				cm.getServerName(),   // serviceName
				cm.getName(),         // serviceInfo
				null,                 // extraInfo
				AlarmEvent.Category.OTHER,
				AlarmEvent.Severity.INFO, 
				AlarmEvent.ServiceState.UP, 
				// Note: max length for the below message is 512
				"The internal DbxTune Subsystem 'SqlCapture' has not been updated for " + ageInSec + " seconds in server '" + cm.getServerName() + "' please investigate or simply restart the DbxTune Collector. ThresholdInSec='"+thresholdInSec+"')",
				thresholdInSec // crossedThreshold... well this one do not have a number.
				);

		setTimeToLive(cm);

		// Set the raw data
		setData(ageInSec);
	}
}
