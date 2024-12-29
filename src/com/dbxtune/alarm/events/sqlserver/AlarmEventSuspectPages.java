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
package com.dbxtune.alarm.events.sqlserver;

import com.dbxtune.Version;
import com.dbxtune.alarm.events.AlarmEvent;
import com.dbxtune.cm.CountersModel;

public class AlarmEventSuspectPages
extends AlarmEvent 
{
	private static final long serialVersionUID = 1L;

	/**
	 * Alarm when records are found in 'msdb.dbo.suspect_pages'
	 * 
	 * @param cm
	 * @param threshold         
	 * @param suspectPageCount
	 */
	public AlarmEventSuspectPages(CountersModel cm, int threshold, int suspectPageCount, int suspectPageErrors)
	{
		super(
				Version.getAppName(), // serviceType
				cm.getServerName(),   // serviceName
				cm.getName(),         // serviceInfo
				null,                 // extraInfo
				AlarmEvent.Category.OTHER,
				AlarmEvent.Severity.ERROR, 
				AlarmEvent.ServiceState.AFFECTED, 
				"At server '" + cm.getServerName() + "', Integrity of the server is threatened. Found " + suspectPageCount + " pages with " + suspectPageErrors + " Errors in 'msdb.dbo.suspect_pages'. Check the detailes for more information about what suspected pages. Possibly follow: https://www.brentozar.com/go/corruption",
				null
				);

		// Set: Time To Live if postpone is enabled
		setTimeToLive(cm);

		// Set the raw data carrier
		setData("suspectPageCount=" + suspectPageCount);
	}
}
