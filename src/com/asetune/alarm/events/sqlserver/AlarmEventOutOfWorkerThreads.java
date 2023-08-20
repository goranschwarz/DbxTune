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
package com.asetune.alarm.events.sqlserver;

import com.asetune.Version;
import com.asetune.alarm.events.AlarmEvent;
import com.asetune.cm.CountersModel;

public class AlarmEventOutOfWorkerThreads
extends AlarmEvent 
{
	private static final long serialVersionUID = 1L;

	/**
	 * Alarm when records are found in 'msdb.dbo.suspect_pages'
	 * 
	 * @param cm
	 * @param threshold         
	 * @param suspectPageCount
	 */
	public AlarmEventOutOfWorkerThreads(CountersModel cm, int threshold, long requestsWaitingForWorkers)
	{
		super(
				Version.getAppName(), // serviceType
				cm.getServerName(),   // serviceName
				cm.getName(),         // serviceInfo
				null,                 // extraInfo
				AlarmEvent.Category.OTHER,
				AlarmEvent.Severity.ERROR, 
				AlarmEvent.ServiceState.AFFECTED, 
				"At server '" + cm.getServerName() + "', We are OUT OFF Worker Threads. Current number of Requests that are Waiting for Worker Thread(s) are " + requestsWaitingForWorkers + ". "
						+ "The server might be unresponsive... (not even possible to connect to it, except from the DAC 'Dedicated Admin Connection' to trouble shoot the issue). "
						+ "HINT: This may be a massive blocking lock situation, if so; kill the lead blocker.",
				threshold
				);

		// Set: Time To Live if postpone is enabled
		setTimeToLive(cm);

		// Set the raw data carrier
		setData("requestsWaitingForWorkers=" + requestsWaitingForWorkers);
	}
}
