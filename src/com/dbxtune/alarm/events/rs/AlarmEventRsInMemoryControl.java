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
package com.dbxtune.alarm.events.rs;

import com.dbxtune.Version;
import com.dbxtune.alarm.events.AlarmEvent;
import com.dbxtune.cm.CountersModel;

public class AlarmEventRsInMemoryControl
extends AlarmEvent 
{
	private static final long serialVersionUID = 1L;

	/**
	 * 
	 * @param cm
	 * @param module
	 * @param memoryLimit
	 */
	public AlarmEventRsInMemoryControl(CountersModel cm, String module, int memoryLimit)
	{
		super(
				Version.getAppName(), // serviceType
				cm.getServerName(),   // serviceName
				cm.getName(),         // serviceInfo
				module,               // extraInfo
				AlarmEvent.Category.SRV_CONFIG,
				AlarmEvent.Severity.ERROR, 
				AlarmEvent.ServiceState.AFFECTED,
				"Replication Server In Memory Control mode for module '" + module + "' in Server '" + cm.getServerName() + "'. Memory is exausted and in MEMEORY CONTROL. Some module(s) has STOPPED working until memory is reclaimed. memoryLimit=" + memoryLimit + ". Fix: configure replication server set memory_limit to '##'",
				0 // there is no threshold...
				);

		// Set: Time To Live if postpone is enabled
		setTimeToLive(cm);

		// Set the raw data carrier
		setData("state="+module);
	}


//	/**
//	 * 
//	 * @param cm
//	 * @param module
//	 * @param timeInSec
//	 * @param memoryLimit
//	 * @param threshold
//	 */
//	public AlarmEventRsInMemoryControl(CountersModel cm, String module, int timeInSec, int memoryLimit, int thresholdInSec)
//	{
//		super(
//				Version.getAppName(), // serviceType
//				cm.getServerName(),   // serviceName
//				cm.getName(),         // serviceInfo
//				module,               // extraInfo
//				AlarmEvent.Category.SRV_CONFIG,
//				AlarmEvent.Severity.ERROR, 
//				AlarmEvent.ServiceState.AFFECTED,
//				"Replication Server is In-Memory-Control mode for module '" + module + "' in Server '" + cm.getServerName() + "'. Memory is exausted and in MEMORY CONTROL. Some module(s) has STOPPED working until memory is reclaimed. module='" + module + "', timeInSec=" + timeInSec + ", memoryLimit=" + memoryLimit + ", thresholdInSec=" + thresholdInSec + ". Fix: configure replication server set memory_limit to '##-in-mb'",
//				thresholdInSec
//				);
//
//		// Adjust the Alarm Full Duration with X seconds
//		setFullDurationAdjustmentInSec( thresholdInSec );
//		
//		// Set: Time To Live if postpone is enabled
//		setTimeToLive(cm);
//
//		// Set the raw data carrier
//		setData("module='" + module + "', timeInSec=" + timeInSec + ", memoryLimit=" + memoryLimit);
//	}
}
