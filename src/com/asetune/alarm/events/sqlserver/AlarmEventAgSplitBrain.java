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
package com.asetune.alarm.events.sqlserver;

import com.asetune.Version;
import com.asetune.alarm.events.AlarmEvent;
import com.asetune.cm.CountersModel;

public class AlarmEventAgSplitBrain
extends AlarmEvent 
{
	private static final long serialVersionUID = 1L;

	/**
	 * Alarm when a Availability Group Role is changed, typically from 'PRIMARY' -> 'SECONDARY' or wise verse
	 * 
	 * @param cm
	 * @param agName              Name of the Availability Group
	 * @param serverName          For what server was did this happen for
	 * @param dbname              Name of the database
	 */
	public AlarmEventAgSplitBrain(CountersModel cm, String agName, String serverName, String dbname, String remoteLiveDataServername)
	{
		super(
				Version.getAppName(), // serviceType
				cm.getServerName(),   // serviceName
				cm.getName(),         // serviceInfo
				"agName='" + agName + "', serverName='" + serverName + "'", // extraInfo
				AlarmEvent.Category.HADR,
				AlarmEvent.Severity.ERROR, 
				AlarmEvent.ServiceState.AFFECTED, 
				"SPLIT-BRAIN At server '" + cm.getServerName() + "', found role 'PRIMARY' at both the LOCAL server '" + serverName + "' and the REMOTE-LIVE-DATA server '"+remoteLiveDataServername+"' for the Availability group '" + agName + "'.",
				null
				);

		// Set: Time To Live if postpone is enabled
		setTimeToLive(cm);

		// Set the raw data carrier
		setData("agName='" + agName + "', serverName='" + serverName + "', remoteLiveDataServername='" + remoteLiveDataServername + "'.");
	}

}
