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

public class AlarmEventRsSdUsage
extends AlarmEvent 
{
	private static final long serialVersionUID = 1L;

	/**
	 * Alarm when a Stable Device Usage goes above any given threshold
	 * 
	 * @param cm
	 * @param threshold
	 * @param usedSpaceInMb
	 * @param freeSpaceInMb
	 * @param usedPct
	 */
	public AlarmEventRsSdUsage(CountersModel cm, int threshold, String alarmType, int usedSpaceInMb, int freeSpaceInMb, double usedPct)
	{
		super(
				Version.getAppName(), // serviceType
				cm.getServerName(),   // serviceName
				cm.getName(),         // serviceInfo
				alarmType,            // extraInfo
				AlarmEvent.Category.SPACE,
				AlarmEvent.Severity.WARNING, 
				AlarmEvent.ServiceState.UP, 
				"Replication Server Stable device (Queuing system) in Server '" + cm.getServerName() + "' is starting to get full, type='"+alarmType+"', usedSpaceInMb="+usedSpaceInMb+", freeSpaceInMb="+freeSpaceInMb+", usedPct="+usedPct+". (threshold="+threshold+")",
				threshold
				);

		// Set: Time To Live if postpone is enabled
		setTimeToLive(cm);

		// Set the raw data carier
		setData("usedSpaceInMb="+usedSpaceInMb+", freeSpaceInMb="+freeSpaceInMb+", usedPct="+usedPct);
	}
}
