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

public class AlarmEventRsReplicationAge
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
	public AlarmEventRsReplicationAge(CountersModel cm, int thresholdInMinutes, String logicalName, String colName, int age)
	{
		super(
				Version.getAppName(), // serviceType
				logicalName,          // serviceName
				cm.getName(),         // serviceInfo
				colName,              // extraInfo
				AlarmEvent.Category.RPO,
				AlarmEvent.Severity.WARNING, 
				AlarmEvent.ServiceState.AFFECTED, 
				"Replication Age in Server '" + cm.getServerName() + "' for Logical Connection '" + logicalName + "', colName='"+colName+"', age="+age+". (thresholdInMinutes="+thresholdInMinutes+")",
				thresholdInMinutes
				);

		// Adjust the Alarm Full Duration with X seconds
		setFullDurationAdjustmentInSec( thresholdInMinutes * 60 );
		
		// Set: Time To Live if postpone is enabled
		setTimeToLive(cm);

		// Set the raw data carier
		setData(age);
	}

}
