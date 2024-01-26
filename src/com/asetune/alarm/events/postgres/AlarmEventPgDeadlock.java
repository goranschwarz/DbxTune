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
package com.asetune.alarm.events.postgres;

import java.sql.Timestamp;

import com.asetune.Version;
import com.asetune.alarm.events.AlarmEvent;
import com.asetune.cm.CountersModel;

public class AlarmEventPgDeadlock
extends AlarmEvent 
{
	private static final long serialVersionUID = 1L;

	/**
	 * From Postgres Errorlog -- Deadlock
	 * <ul>
	 *   <li>40P01 == deadlock_detected   </li>
	 * </ul>
	 * 
	 * @param cm
	 * @param stateCode
	 * @param stateCodeDesc
	 * @param logMessage
	 * @param logTimestamp
	 */
	public AlarmEventPgDeadlock(CountersModel cm, String stateCode, String stateCodeDesc, String logMessage, Timestamp logTimestamp)
	{
		super(
				Version.getAppName(), // serviceType
				cm.getServerName(),   // serviceName
				cm.getName(),         // serviceInfo
				stateCodeDesc + ":" + logMessage, // extraInfo
				AlarmEvent.Category.LOCK,
				AlarmEvent.Severity.INFO, 
				AlarmEvent.ServiceState.UP, 
				"Deadlock found in Server '" + cm.getServerName() + "' stateCode='" + stateCode + "', stateCodeDesc=" + stateCodeDesc + ", logMessage='" + logMessage + "', ts='" + logTimestamp + "'.)",
				null
				);

		// Set: Time To Live if postpone is enabled
		setTimeToLive(cm);

		// Set the raw data carrier
		setData(stateCode);
	}

}
