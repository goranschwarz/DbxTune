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
package com.asetune.alarm.events;

import java.sql.Timestamp;

import com.asetune.Version;
import com.asetune.cm.CountersModel;

public class AlarmEventFullTranLog
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
	 * Alarm for full transaction logs. Just the count of databases with a full tran log.
	 * @param cm
	 * @param val
	 */
	public AlarmEventFullTranLog(CountersModel cm, Number threshold, Number val)
	{
		super(
				Version.getAppName(), // serviceType
				cm.getServerName(),   // serviceName
				cm.getName(),         // serviceInfo
				null,                 // extraInfo
				AlarmEvent.Category.SPACE,
				AlarmEvent.Severity.ERROR, 
				AlarmEvent.ServiceState.AFFECTED, 
				"Full transaction log in some database(s) in '" + cm.getServerName() + "'. Count=" + val + ". (threshold="+threshold+")",
				threshold);

		// Set: Time To Live if postpone is enabled
		setTimeToLive(cm);

		// Set the raw data carier: current cpu usage
		setData(val);
	}

	/**
	 * Alarm for full transaction log for a specific database.
	 * @param cm
	 * @param dbname
	 * @param errorlogTs 
	 */
	public AlarmEventFullTranLog(CountersModel cm, Number threshold, String dbname, Timestamp errorlogTs)
	{
		super(
				Version.getAppName(), // serviceType
				cm.getServerName(),   // serviceName
				cm.getName(),         // serviceInfo
				dbname,               // extraInfo
				AlarmEvent.Category.SPACE,
				AlarmEvent.Severity.ERROR, 
				AlarmEvent.ServiceState.AFFECTED, 
				"Full transaction log in '" + cm.getServerName() + "', at '" + errorlogTs + "', dbname='" + dbname + "'. (threshold="+threshold+")",
				threshold);

		// Set: Time To Live if postpone is enabled
		setTimeToLive(cm);

		// Set the raw data carier: current cpu usage
		setData(dbname);
	}
}
