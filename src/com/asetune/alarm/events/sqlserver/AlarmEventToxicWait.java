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
package com.asetune.alarm.events.sqlserver;

import com.asetune.Version;
import com.asetune.alarm.events.AlarmEvent;
import com.asetune.cm.CountersModel;

public class AlarmEventToxicWait
extends AlarmEvent 
{
	private static final long serialVersionUID = 1L;

	/**
	 * SQL Server has a "toxic" wait type that has been waiting for more than the specified threshold
	 * 
	 * @param cm
	 * @param threshold
	 * @param waitType
	 * @param wait_time_ms
	 */
	public AlarmEventToxicWait(CountersModel cm, int threshold, AlarmEvent.Severity severity, AlarmEvent.ServiceState serviceState, String waitType, int waitCount, int waitTimeMs, double avgMsPerWait)
	{
		super(
		Version.getAppName(), // serviceType
		cm.getServerName(),   // serviceName
		cm.getName(),         // serviceInfo
		null,                 // extraInfo
		AlarmEvent.Category.OTHER,
		severity, 
		serviceState, // AlarmEvent.ServiceState.UP, 
		"At server '" + cm.getServerName() + "', there are has been " + waitCount + " Toxic waits for '" + waitType + "' with a wait time of " + waitTimeMs + " ms. (avgMsPerWait=" + avgMsPerWait + ")",
		threshold
		);

		// Set: Time To Live if postpone is enabled
		setTimeToLive(cm);

		// Set the raw data carrier
		setData("waitType=" + waitType + ", waitCount=" + waitCount + ", waitTimeMs=" + waitTimeMs + ", avgMsPerWait=" + avgMsPerWait);
	}

}
