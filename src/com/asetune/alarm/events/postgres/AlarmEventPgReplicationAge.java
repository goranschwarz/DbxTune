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
package com.asetune.alarm.events.postgres;

import com.asetune.Version;
import com.asetune.alarm.events.AlarmEvent;
import com.asetune.cm.CountersModel;

public class AlarmEventPgReplicationAge
extends AlarmEvent 
{
	private static final long serialVersionUID = 1L;

	/**
	 * When any age has expired
	 * 
	 * @param cm
	 * @param threshold
	 * @param logicalName
	 * @param colName
	 */
	public AlarmEventPgReplicationAge(CountersModel cm, String client_addr, int reply_time_seconds, int threshold)
	{
		super(
				Version.getAppName(), // serviceType
				client_addr,          // serviceName
				cm.getName(),         // serviceInfo
				null,                 // extraInfo
				AlarmEvent.Category.RPO,
				AlarmEvent.Severity.WARNING, 
				AlarmEvent.ServiceState.AFFECTED, 
				"Replication Age in Server '" + cm.getServerName() + "' to '" + client_addr + "', age="+reply_time_seconds+". (threshold="+threshold+")",
				threshold
				);

		// Set: Time To Live if postpone is enabled
		setTimeToLive(cm);

		// Set the raw data carrier
		setData(reply_time_seconds);
	}

}
