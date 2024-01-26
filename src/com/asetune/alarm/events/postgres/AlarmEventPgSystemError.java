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
import com.asetune.utils.Configuration;

public class AlarmEventPgSystemError
extends AlarmEvent 
{
	private static final long serialVersionUID = 1L;

	public static final String  PROPKEY_alarm_timeToLiveInSeconds = "AlarmEventPgSystemError.timeToLive.seconds";
	public static final int     DEFAULT_alarm_timeToLiveInSeconds = 1800; // 30 minutes

	private static String getType(String type)
	{
		if ("58030".equals(type)) return "[IO ERROR]";
		return "";
	}

	/**
	 * From Postgres Errorlog -- System Error
	 * <ul>
	 *   <li>58000 == system_error   </li>
	 *   <li>58030 == io_error       </li>
	 *   <li>58P01 == undefined_file </li>
	 *   <li>58P02 == duplicate_file </li>
	 * </ul>
	 * 
	 * @param cm
	 * @param stateCode
	 * @param stateCodeDesc
	 * @param logMessage
	 * @param logTimestamp
	 */
	public AlarmEventPgSystemError(CountersModel cm, String stateCode, String stateCodeDesc, String logMessage, Timestamp logTimestamp)
	{
		super(
				Version.getAppName(), // serviceType
				cm.getServerName(),   // serviceName
				cm.getName(),         // serviceInfo
				stateCodeDesc + ":" + logMessage, // extraInfo
				AlarmEvent.Category.OTHER,
				AlarmEvent.Severity.ERROR, 
				AlarmEvent.ServiceState.AFFECTED, 
				"System Error" + getType(stateCode) + " found in Server '" + cm.getServerName() + "' stateCode='" + stateCode + "', stateCodeDesc=" + stateCodeDesc + ", logMessage='" + logMessage + "', ts='" + logTimestamp + "'.)",
				null
				);

		// Set: FIXED Time To Live
		// Errorlog entries are a "one time shot" (it's only seen once)... so how long should that entry be "active"
		int ttlSec = Configuration.getCombinedConfiguration().getIntProperty(PROPKEY_alarm_timeToLiveInSeconds, DEFAULT_alarm_timeToLiveInSeconds);
		setTimeToLive( ttlSec * 1000 );

		// Set the raw data carrier
		setData(stateCode);
	}

}
