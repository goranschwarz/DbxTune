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
package com.dbxtune.alarm.events;

import com.dbxtune.Version;
import com.dbxtune.cm.CountersModel;

public class AlarmEventMissingMandatoryContent
extends AlarmEvent 
{
	private static final long serialVersionUID = 1L;

	/**
	 * For the moment this is used by CmProcessActivity to check if we are missing any mandatory application that has NOT been started.
	 * <p>
	 * An example of this could be: that we want alarm(s); if the application "BlockingLocks-check" hasn't been started.<br>
	 * and there
	 * 
	 * @param cm
	 * @param colName
	 * @param colContent
	 */
	public AlarmEventMissingMandatoryContent(CountersModel cm, String colName, String colContent)
	{
		super(
				Version.getAppName(),       // serviceType
				cm.getServerName(),         // serviceName
				cm.getName(),               // serviceInfo
				colName + ":" + colContent, // extraInfo
				AlarmEvent.Category.OTHER,
				AlarmEvent.Severity.WARNING, 
				AlarmEvent.ServiceState.UP, 
				"Missing Mandatory Content for CM '" + cm.getName() + "' in column '" + colName + "' with missing content of '" + colContent + "', in Server '" + cm.getServerName() + "'.",
				1                           // threshold
				);

		// If we want this Event to be "delayed" for X seconds... so we don't Raise the Alarm TO FAST and causes "flickering" alarms
		//setRaiseDelayInSec(sec);
		
		// Set: Time To Live if postpone is enabled
		setTimeToLive(cm);

		// Set the raw data carrier
		setData("colName="+colName+",colContent="+colContent);
	}
}
