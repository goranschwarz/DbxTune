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

public class AlarmEventDummy 
extends AlarmEvent
{
	private static final long serialVersionUID = 1L;

	public AlarmEventDummy(String serviceName, String serviceInfo, Object extraInfo, Category category, Severity severity, ServiceState state, int timeToLive, Object data, String description, String extendedDesc, int thresholdInSec)
	{
		super(
				Version.getAppName(), // serviceType
				serviceName,   // serviceName
				serviceInfo,         // serviceInfo
				extraInfo,                 // extraInfo
				category,
				severity, 
				state, 
				description,
				null);

		// Adjust the Alarm Full Duration with X seconds
		setFullDurationAdjustmentInSec( thresholdInSec );

		setExtendedDescription(extendedDesc, "");

		// Set: Time To Live if postpone is enabled
		setTimeToLive(timeToLive);

		// Set the raw data carier: current cpu usage
		setData(data);
	}

}
