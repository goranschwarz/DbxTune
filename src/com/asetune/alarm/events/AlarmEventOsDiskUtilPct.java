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

public class AlarmEventOsDiskUtilPct
extends AlarmEvent 
{
	private static final long serialVersionUID = 1L;

	/**
	 * Alarm for heavily loaded OS Devices.
	 * @param cm
	 * @param dbname
	 */
	public AlarmEventOsDiskUtilPct(CountersModel cm, double threshold, String hostname, String device, Double utilPct, int raiseDelayInMinutes)
	{
		super(
				Version.getAppName(), // serviceType
				cm.getServerName(),   // serviceName
				cm.getName(),         // serviceInfo
				device,               // extraInfo
				AlarmEvent.Category.OTHER,
				AlarmEvent.Severity.WARNING, 
				AlarmEvent.ServiceState.UP, 
				"Slow or heavily used OS Disk found in '" + cm.getServerName() + "', device='" + device + "', pctUsed=" + utilPct + ", raiseDelay=" + raiseDelayInMinutes + ". (threshold="+threshold+")",
				threshold);

		// Set: Time To Live if postpone is enabled
		setTimeToLive(cm);

		// Set the raw data
		setData("device='" + device + "', pctUsed=" + utilPct + ", raiseDelay=" + raiseDelayInMinutes);

		// The alarm has to be active for ## minutes before it will be RAISED
		setRaiseDelayInSec(raiseDelayInMinutes * 60);
	}
}
