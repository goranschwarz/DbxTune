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
package com.dbxtune.alarm.events.sqlserver;

import com.dbxtune.Version;
import com.dbxtune.alarm.events.AlarmEvent;
import com.dbxtune.cm.CountersModel;

public class AlarmEventTempdbSpidUsage
extends AlarmEvent 
{
	private static final long serialVersionUID = 1L;

	/**
	 * Alarm when a SPID is using excessive amount of memory in tempdb
	 * 
	 * @param cm
	 * @param threshold
	 * @param session_id
	 * @param totalUsageMb
	 * @param last_request_start_time
	 * @param last_request_in_sec
	 * @param login
	 * @param program_name
	 */
	public AlarmEventTempdbSpidUsage(CountersModel cm, int threshold, int session_id, int totalUsageMb, String last_request_start_time, String last_request_in_sec, String login_name, String program_name)
	{
		super(
				Version.getAppName(), // serviceType
				cm.getServerName(),   // serviceName
				cm.getName(),         // serviceInfo
				session_id,           // extraInfo
				AlarmEvent.Category.SPACE,
				AlarmEvent.Severity.WARNING, 
				AlarmEvent.ServiceState.UP, 
				"At server '" + cm.getServerName() + "', A SPID is using a lot of space (" + totalUsageMb + " MB) in tempdb.  spid=" + session_id + ", login_name='" + login_name + "', program_name='" + program_name + "', last_request_start_time='" + last_request_start_time + "', last_request_in_sec='" + last_request_in_sec + "'.",
				threshold
				);

		// Set: Time To Live if postpone is enabled
		setTimeToLive(cm);

		// Set the raw data carrier
		setData("spid=" + session_id + ", login_name='" + login_name + "', program_name='" + program_name + "', last_request_start_time='" + last_request_start_time + "', last_request_in_sec='" + last_request_in_sec + "'.");
	}
}
