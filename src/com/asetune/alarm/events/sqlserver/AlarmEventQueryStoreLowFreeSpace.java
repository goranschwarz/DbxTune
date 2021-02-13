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

public class AlarmEventQueryStoreLowFreeSpace
extends AlarmEvent 
{
	private static final long serialVersionUID = 1L;

	public enum Type
	{
		PCT, MB
	};
	
	/**
	 * Alarm when database LOG space is starting to get full. Threshold in Percent
	 * 
	 * @param cm
	 * @param dbname
	 * @param qsUsedSpaceInPct
	 * @param qsMaxSizeInMb
	 * @param qsUsedSpaceInMb
	 * @param qsFreeSpaceInMb
	 * @param type                 PCT or MB
	 * @param threshold
	 */
	public AlarmEventQueryStoreLowFreeSpace(CountersModel cm, String dbname, Double qsUsedSpaceInPct, Double qsMaxSizeInMb, Double qsUsedSpaceInMb, Double qsFreeSpaceInMb, Type type, Double threshold)
	{
		super(
				Version.getAppName(), // serviceType
				cm.getServerName(),   // serviceName
				cm.getName(),         // serviceInfo
				dbname,               // extraInfo
				AlarmEvent.Category.SPACE,
				AlarmEvent.Severity.WARNING, 
				AlarmEvent.ServiceState.UP, 
				"Low Free Space in Query Store (" + type + ") in Server '" + cm.getServerName() + "' and dbname '" + dbname + "', UsedSpaceInPct="+qsUsedSpaceInPct+", FreeSpaceInMB="+qsFreeSpaceInMb+", UsedSpaceInMb="+qsUsedSpaceInMb+", MaxSizeInMb="+qsMaxSizeInMb+". (thresholdInPct="+threshold+")",
				threshold
				);

		// Set: Time To Live if postpone is enabled
		setTimeToLive(cm);

		// Set the raw data 
		setData("dbname '" + dbname + "', UsedSpaceInPct="+qsUsedSpaceInPct+", FreeSpaceInMB="+qsFreeSpaceInMb+", UsedSpaceInMb="+qsUsedSpaceInMb+", MaxSizeInMb="+qsMaxSizeInMb);
	}
}
