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
package com.dbxtune.alarm.events;

import com.dbxtune.Version;
import com.dbxtune.cm.CountersModel;

public class AlarmEventStatementCacheAboveConfig
extends AlarmEvent 
{
	private static final long serialVersionUID = 1L;

	public AlarmEventStatementCacheAboveConfig(CountersModel cm, int configuredSpaceInMb, int usedSpaceInMb, double usedSpaceInPct, double usedPctOfProcMemory, int threshold)
	{
		super(
				Version.getAppName(), // serviceType
				cm.getServerName(),   // serviceName
				cm.getName(),         // serviceInfo
				null,                 // extraInfo
				AlarmEvent.Category.SRV_CONFIG,
				AlarmEvent.Severity.WARNING,
				AlarmEvent.ServiceState.UP, 
				"Statement Cache is above the configured value in server '" + cm.getServerName() + "'. configuredSpaceInMb="+configuredSpaceInMb+", usedSpaceInMb="+usedSpaceInMb+", usedSpaceInPct="+usedSpaceInPct+", usedPercentOfProcedurMemory="+usedPctOfProcMemory+". (threshold="+threshold+")",
				null);

		// Set: Time To Live if postpone is enabled
		setTimeToLive(cm);
	}
}
