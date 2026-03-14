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
import com.dbxtune.utils.SpaceForecast.SpaceForecastResult.SpaceType;

public class AlarmEventSpaceFullPrediction 
extends AlarmEvent
{
	private static final long serialVersionUID = 1L;

//	public AlarmEventSpaceFullPrediction(CountersModel cm, AlarmEvent.Severity severity, String mountedOn, String filesystem, int hoursToDiskFull, int threshold)
//	{
//		this(cm.getServerName(), cm.getName(), severity, mountedOn, filesystem, hoursToDiskFull, threshold);
//	}

	/**
	 * Alarm for (Disk/DBMS-Data/DBMS-Wal) Space will become full in X days
	 * 
	 * @param srvName
	 * @param sectionName
	 * @param severity
	 * @param mountedOn
	 * @param filesystem
	 * @param hoursUntilFull
	 * @param threshold
	 */
	public AlarmEventSpaceFullPrediction(String srvName, String sectionName, AlarmEvent.Severity severity, SpaceType spaceType, String name, String extraName, long hoursUntilFull, int threshold)
	{
		super(
				Version.getAppName(), // serviceType
				srvName,              // serviceName
				sectionName,          // serviceInfo
				name,                 // extraInfo
				AlarmEvent.Category.SPACE,
				severity, 
				AlarmEvent.ServiceState.UP, 
				"Full Space " + spaceType + " Prediction at server '" + srvName + "', name='" + name + "', extraName='" + extraName + "', hoursToDiskFull=" + hoursUntilFull + ". (threshold="+threshold+")",
				threshold);

//		// Set: Time To Live if postpone is enabled
//		setTimeToLive(cm);

		// Set the raw data
		setData("name='" + name + "', extraName='" + extraName + "', spaceType='" + spaceType.name() + "', hoursToDiskFull=" + hoursUntilFull);
	}

	// Can and SHOULD be cancelled by: AlarmHandler.checkForCancelations()
	// Meaning we do NOT need any CM to be "refreshed" for this alarm to be cancelled
	// How long the Alarms will live is dictated by TimeToLive...
	@Override
	public boolean isAlwaysCancelable()
	{
		return true;
	}
}
