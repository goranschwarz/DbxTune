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
import com.dbxtune.utils.StringUtil;

public class AlarmEventSrvDown
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

	public AlarmEventSrvDown(CountersModel cm)
	{
		super(
				Version.getAppName(), // serviceType
				cm.getServerName(),   // serviceName
				cm.getName(),         // serviceInfo
				null,                 // extraInfo
				AlarmEvent.Category.DOWN, 
				AlarmEvent.Severity.ERROR, 
				AlarmEvent.ServiceState.DOWN, 
				"Server is DOWN. Name='" + cm.getServerName() + "'.",
				null);

		// Set: Time To Live if postpone is enabled
		setTimeToLive(cm);
	}

	public AlarmEventSrvDown(String serverName, String url, Exception connectException, String connectInfoMsg)
	{
		super(
				Version.getAppName(), // serviceType
				serverName,           // serviceName
				url,                  // serviceInfo
				null,                 // extraInfo
				AlarmEvent.Category.DOWN, 
				AlarmEvent.Severity.ERROR, 
				AlarmEvent.ServiceState.DOWN, 
				"Server is DOWN. Name='" + serverName + "', url='" + url + "'.",
				null);
		
		setExtendedDescription("Connect Info Message: "+connectInfoMsg, null);
		setData( StringUtil.isNullOrBlank(serverName) ? url : serverName );
	}
}
