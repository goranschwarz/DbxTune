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
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 * 
 * DbxTune is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License
 * along with DbxTune.  If not, see <http://www.gnu.org/licenses/>.
 ******************************************************************************/
package com.dbxtune.alarm.events.ase;

import com.dbxtune.Version;
import com.dbxtune.alarm.events.AlarmEvent;

public class AlarmEventAseLicensExpiration
extends AlarmEvent 
{
	private static final long serialVersionUID = 1L;

	/**
	 * <b>always</b> send this alarm<br>
	 * The the "filter" functionality will be discarded/short-circuited 
	 * 
	 * @return true if we should always send, false if we should look at the "filter" settings.
	 */
	@Override
	public boolean alwaysSend()
	{
		return true;
	}

	/**
	 * License is about to expire
	 * 
	 * @param warningText text from AseConnectionUtil.getAseGracePeriodWarning(conn)
	 */
	public AlarmEventAseLicensExpiration(String serverName, String warningText)
	{
		super(
				Version.getAppName(), // serviceType
				serverName,           // serviceName
				"LicensInfo",         // serviceInfo
				null,                 // extraInfo
				AlarmEvent.Category.OTHER,
				AlarmEvent.Severity.WARNING, 
				AlarmEvent.ServiceState.UP, 
				warningText,
				0 // threshold
				);

		// Set: Time To Live == A-LONG-TIME ... since it's only checked during "connect"!
		long oneYearInMs = 3600 * 24 * 365 * 1000; // 3600=1h * 24h * 365days * millisec
		setTimeToLive(oneYearInMs);
	}
}
