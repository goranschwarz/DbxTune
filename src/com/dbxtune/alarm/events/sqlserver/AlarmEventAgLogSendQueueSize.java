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
package com.dbxtune.alarm.events.sqlserver;

import com.dbxtune.Version;
import com.dbxtune.alarm.events.AlarmEvent;
import com.dbxtune.cm.CountersModel;

public class AlarmEventAgLogSendQueueSize
extends AlarmEvent 
{
	private static final long serialVersionUID = 1L;

	/**
	 * Alarm when "LogSendQueueSize" is above a threshold for an Availability Group 
	 * 
	 * @param cm
	 * @param agName                    Name of the Availability Group
	 * @param serverName                For what server was did this happen for
	 * @param dbname                    Name of the database
	 * @param queueSizeInMb             Queue Size In MB
	 * @param threshold                 threshold that was crossed
	 */
	public AlarmEventAgLogSendQueueSize(CountersModel cm, String agName, String serverName, String dbname, int queueSizeInMb, int thresholdInMb)
	{
		super(
				Version.getAppName(), // serviceType
				cm.getServerName(),   // serviceName
				cm.getName(),         // serviceInfo
				"agName='" + agName + "', serverName='" + serverName + "'", // extraInfo
				AlarmEvent.Category.SPACE,
				AlarmEvent.Severity.WARNING, 
				AlarmEvent.ServiceState.UP, 
				"Log Send Queue Size is getting HIGH, in Server '" + cm.getServerName() + "' for Availability group '" + agName + "', serverName='" + serverName + "'. queueSizeInMb='"+queueSizeInMb+"'. (thresholdInMb="+thresholdInMb+")",
				thresholdInMb
				);

		// Set: Time To Live if postpone is enabled
		setTimeToLive(cm);

		// Set the raw data carrier
		setData("agName='" + agName + "', serverName='" + serverName + "': queueSizeInMb='" + queueSizeInMb + "'.");
	}
}
