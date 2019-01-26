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
package com.asetune.alarm.events.rs;

import com.asetune.Version;
import com.asetune.alarm.events.AlarmEvent;
import com.asetune.cm.CountersModel;

public class AlarmEventRsWsState
extends AlarmEvent 
{
	private static final long serialVersionUID = 1L;

	/**
	 * Alarm when a WarmStandby Connection State is not normal
	 * 
	 * @param cm
	 * @param logicalName
	 * @param colName
	 * @param state
	 */
	public AlarmEventRsWsState(CountersModel cm, String logicalName, String colName, String state, String regexp)
	{
		super(
				Version.getAppName(), // serviceType
				logicalName,          // serviceName
				cm.getName(),         // serviceInfo
				colName,              // extraInfo
				AlarmEvent.Category.DOWN,
				AlarmEvent.Severity.WARNING, 
				AlarmEvent.ServiceState.AFFECTED, 
				"Not a normal State in Server '" + cm.getServerName() + "' for Logical Connection '" + logicalName + "', colName='"+colName+"', state='"+state+"'. (regexp="+regexp+")",
				null
				);

		// Set: Time To Live if postpone is enabled
		setTimeToLive(cm);

		// Set the raw data carier
//		setData("logicalName="+logicalName+",colName="+colName+",state="+state);
		setData(state);
	}

}
