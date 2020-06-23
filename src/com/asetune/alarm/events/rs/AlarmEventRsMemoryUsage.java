/*******************************************************************************
 * Copyright (C) 2010-2020 Goran Schwarz
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
package com.asetune.alarm.events.rs;

import com.asetune.Version;
import com.asetune.alarm.events.AlarmEvent;
import com.asetune.cm.CountersModel;

public class AlarmEventRsMemoryUsage
extends AlarmEvent 
{
	private static final long serialVersionUID = 1L;

	/**
	 * Alarm when a RepServer Memory Usage goes above any given threshold
	 * 
	 * @param cm
	 * @param threshold
	 * @param usedSpaceInMb
	 * @param freeSpaceInMb
	 * @param usedPct
	 */
	public AlarmEventRsMemoryUsage(CountersModel cm, int threshold, int thresholdLevel, int usedMemoryInMb, int freeMemoryInMb, double usedPct, int memoryLimit)
	{
		super(
				Version.getAppName(), // serviceType
				cm.getServerName(),   // serviceName
				cm.getName(),         // serviceInfo
				"THRESHOLD-"+thresholdLevel,       // extraInfo
				AlarmEvent.Category.SRV_CONFIG,
				thresholdLevel < 3 ? AlarmEvent.Severity.WARNING : AlarmEvent.Severity.ERROR, 
				thresholdLevel < 3 ? AlarmEvent.ServiceState.UP  : AlarmEvent.ServiceState.AFFECTED,
				"Replication Server Memory Usage in Server '" + cm.getServerName() + "' is starting to get high, type='THRESHOLD-"+thresholdLevel+"', usedMemoryInMb="+usedMemoryInMb+", freeMemoryInMb="+freeMemoryInMb+", usedPct="+usedPct+", memoryLimit="+memoryLimit+". (threshold="+threshold+")",
				threshold
				);

		// Set: Time To Live if postpone is enabled
		setTimeToLive(cm);

		// Set the raw data carier
		setData("usedMemoryInMb="+usedMemoryInMb+", freeMemoryInMb="+freeMemoryInMb+", usedPct="+usedPct+", memoryLimit="+memoryLimit);
	}
}
