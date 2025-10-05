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

public class AlarmEventConfigResourceIsReused
extends AlarmEvent 
{
	private static final long serialVersionUID = 1L;

	public AlarmEventConfigResourceIsReused(CountersModel cm, String cfgName, Double reuse_cnt, int threshold)
	{
		super(
				Version.getAppName(), // serviceType
				cm.getServerName(),   // serviceName
				cm.getName(),         // serviceInfo    // Note: use cm.getName() will cause one event for every CM. If we set this to "", then there will only be one Alarm for the whole server
				cfgName,              // extraInfo
				AlarmEvent.Category.SRV_CONFIG,
				AlarmEvent.Severity.WARNING,
				AlarmEvent.ServiceState.UP, 
				"Configuration resource '" +cfgName + "' is reuse_cnt=" + reuse_cnt + " in the last sample at server '" + cm.getServerName() + "'.",
				null);

		setData("reuse_cnt=" + reuse_cnt);

		// Set: Time To Live if postpone is enabled
		setTimeToLive(cm);
	}
}
