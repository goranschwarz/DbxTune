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

public class AlarmEventOldIncrementalBackup
extends AlarmEvent 
{
	private static final long serialVersionUID = 1L;

	/**
	 * Alarm for full transaction log for a specific database.
	 * @param cm
	 * @param dbname
	 */
	public AlarmEventOldIncrementalBackup(CountersModel cm, Number thresholdInHours, String dbname, String lastBackupStartTime, int age)
	{
		super(
				Version.getAppName(), // serviceType
				cm.getServerName(),   // serviceName
				cm.getName(),         // serviceInfo
				dbname,               // extraInfo
				AlarmEvent.Category.OTHER,
				AlarmEvent.Severity.WARNING, 
				AlarmEvent.ServiceState.UP, 
				"Old or No Incremental Database Backup found in '" + cm.getServerName() + "', dbname='" + dbname + "', Backup Age in Hours '" + age + "'" 
						+ (age == -1 ? ", (where -1 means: Since ASE was started)" : "") 
						+ ", lastBackupStartTime='" + lastBackupStartTime + "'. (thresholdInHours="+thresholdInHours+")",
						thresholdInHours);

		// Adjust the Alarm Full Duration with X seconds
		setFullDurationAdjustmentInSec( thresholdInHours == null ? 0 : thresholdInHours.intValue() * 60 );

		// Set: Time To Live if postpone is enabled
		setTimeToLive(cm);

		// Set the raw data carier: current cpu usage
		setData(dbname);
	}
}
