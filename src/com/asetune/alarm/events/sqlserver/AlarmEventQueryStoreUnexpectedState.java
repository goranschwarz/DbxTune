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

public class AlarmEventQueryStoreUnexpectedState
extends AlarmEvent 
{
	private static final long serialVersionUID = 1L;

	/**
	 * Alarm when a Unexpected state has been found for a Query Store 
	 * 
	 * @param cm
	 * @param dbname
	 * @param expectedStr
	 * @param qsDesiredState
	 * @param qsActualState
	 * @param qsReadOnlyReason
	 */
	public AlarmEventQueryStoreUnexpectedState(CountersModel cm, String dbname, String qsDesiredState, String qsActualState, Double qsReadOnlyReason, String expectedStr)
	{
		super(
				Version.getAppName(), // serviceType
				cm.getServerName(),   // serviceName
				cm.getName(),         // serviceInfo
				dbname,               // extraInfo
				AlarmEvent.Category.OTHER,
				AlarmEvent.Severity.WARNING, 
				AlarmEvent.ServiceState.UP, 
				"Query Store in an Unexpected state. DesiredState='" + qsDesiredState + "', ActualState='" + qsActualState + "', ReadOnlyReason=" + qsReadOnlyReason + ".",
				null
				);

		// Set: Time To Live if postpone is enabled
		setTimeToLive(cm);

		// Set the raw data carrier
		setData("DesiredState='" + qsDesiredState + "', ActualState='" + qsActualState + "', ReadOnlyReason=" + qsReadOnlyReason + ".");
	}

}
