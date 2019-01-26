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

public class AlarmEventLowLogFreeSpace
extends AlarmEvent 
{
	private static final long serialVersionUID = 1L;

	/**
	 * Alarm when database LOG space is starting to get full. Threshold in MB
	 * 
	 * @param cm
	 * @param dbname
	 * @param freeSpaceInMb
	 * @param usedSpaceInPct
	 * @param thresholdInMb
	 */
	public AlarmEventLowLogFreeSpace(CountersModel cm, String dbname, int freeSpaceInMb, double usedSpaceInPct, int thresholdInMb)
	{
		super(
				Version.getAppName(), // serviceType
				cm.getServerName(),   // serviceName
				cm.getName(),         // serviceInfo
				dbname,               // extraInfo
				AlarmEvent.Category.SPACE,
				AlarmEvent.Severity.WARNING, 
				AlarmEvent.ServiceState.UP, 
				"Low DB LOG Free Space (MB) in Server '" + cm.getServerName() + "' and dbname '" + dbname + "', FreeSpaceInMB="+freeSpaceInMb+", UsedSpaceInPcs="+usedSpaceInPct+". (thresholdInMb="+thresholdInMb+")",
				thresholdInMb
				);

		// Set: Time To Live if postpone is enabled
		setTimeToLive(cm);

		// Set the raw data carier
		setData("dbname="+dbname+",freeMb="+freeSpaceInMb+",usedPct="+usedSpaceInPct);
	}

	/**
	 * Alarm when database LOG space is starting to get full. Threshold in Percent
	 * 
	 * @param cm
	 * @param dbname
	 * @param freeSpaceInMb
	 * @param usedSpaceInPct
	 * @param thresholdInPct
	 */
	public AlarmEventLowLogFreeSpace(CountersModel cm, String dbname, int freeSpaceInMb, double usedSpaceInPct, double thresholdInPct)
	{
		super(
				Version.getAppName(), // serviceType
				cm.getServerName(),   // serviceName
				cm.getName(),         // serviceInfo
				dbname,               // extraInfo
				AlarmEvent.Category.SPACE,
				AlarmEvent.Severity.WARNING, 
				AlarmEvent.ServiceState.UP, 
				"Low DB LOG Free Space (PCT) in Server '" + cm.getServerName() + "' and dbname '" + dbname + "', FreeSpaceInMB="+freeSpaceInMb+", UsedSpaceInPcs="+usedSpaceInPct+". (thresholdInPct="+thresholdInPct+")",
				thresholdInPct
				);

		// Set: Time To Live if postpone is enabled
		setTimeToLive(cm);

		// Set the raw data carier
		setData("dbname="+dbname+",freeMb="+freeSpaceInMb+",usedPct="+usedSpaceInPct);
	}
}
