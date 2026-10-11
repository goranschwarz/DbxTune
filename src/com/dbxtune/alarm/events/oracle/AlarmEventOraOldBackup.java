/*******************************************************************************
 * Copyright (C) 2010-2027 Goran Schwarz
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
package com.dbxtune.alarm.events.oracle;

import com.dbxtune.Version;
import com.dbxtune.alarm.events.AlarmEvent;
import com.dbxtune.cm.CountersModel;

public class AlarmEventOraOldBackup
extends AlarmEvent
{
	private static final long serialVersionUID = 1L;

	/**
	 * Alarm when the last successful RMAN database backup (full or incremental) is to old, or when no successful database backup was found at all.
	 *
	 * @param cm
	 * @param thresholdInHours
	 * @param lastBackupType       INPUT_TYPE of the last successful database backup, like 'DB FULL' or 'DB INCR' (null if no backup was found)
	 * @param lastBackupEndTime    When the last successful database backup ended (null if no backup was found)
	 * @param ageInHours           How old the last successful database backup is (null if no backup was found)
	 */
	public AlarmEventOraOldBackup(CountersModel cm, int thresholdInHours, String lastBackupType, String lastBackupEndTime, Double ageInHours)
	{
		super(
				Version.getAppName(), // serviceType
				cm.getServerName(),   // serviceName
				cm.getName(),         // serviceInfo
				null,                 // extraInfo
				AlarmEvent.Category.RPO,
				AlarmEvent.Severity.WARNING,
				AlarmEvent.ServiceState.UP,
				ageInHours == null
					? "No successful RMAN Database Backup found in Server '" + cm.getServerName() + "' (among the backup jobs the database knows about). (thresholdInHours=" + thresholdInHours + ")"
					: "Old RMAN Database Backup in Server '" + cm.getServerName() + "'. The last successful backup is " + ageInHours + " hours old, type='" + lastBackupType + "', endTime='" + lastBackupEndTime + "'. (thresholdInHours=" + thresholdInHours + ")",
				thresholdInHours
				);

		// Adjust the Alarm Full Duration with X seconds
		setFullDurationAdjustmentInSec( thresholdInHours * 3600 );

		// Set: Time To Live if postpone is enabled
		setTimeToLive(cm);

		// Set the raw data carrier
		setData("lastBackupType=" + lastBackupType + ",lastBackupEndTime=" + lastBackupEndTime + ",ageInHours=" + ageInHours);
	}
}
