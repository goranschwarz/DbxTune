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
package com.asetune.alarm.events;

import java.sql.Timestamp;

import com.asetune.Version;
import com.asetune.cm.CountersModel;

public class AlarmEventConfigChanges
extends AlarmEvent 
{
	private static final long serialVersionUID = 1L;

	/**
	 * Alarm for full transaction log for a specific database.
	 * @param cm
	 * @param errorlogTs 
	 * @param dbname
	 */
	public AlarmEventConfigChanges(CountersModel cm, String configName, String errorlogMsg, Timestamp errorlogTs)
	{
		super(
				Version.getAppName(), // serviceType
				cm.getServerName(),   // serviceName
				cm.getName(),         // serviceInfo
				configName,           // extraInfo
				AlarmEvent.Category.SRV_CONFIG,
				AlarmEvent.Severity.INFO, 
				AlarmEvent.ServiceState.UP, 
				"ErrorlogTs='" + errorlogTs + "', " + errorlogMsg,
				null);

		// Set: Time To Live if postpone is enabled
		setTimeToLive(cm);

		// Set the raw data carrier:
		setData(configName);
	}
}
