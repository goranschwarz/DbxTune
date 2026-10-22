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

public class AlarmEventOraLastBackupFailed
extends AlarmEvent
{
	private static final long serialVersionUID = 1L;

	/**
	 * Alarm when the last RMAN backup job of a type (DB FULL, DB INCR, ARCHIVELOG...) did not succeed.
	 *
	 * @param cm
	 * @param inputType     INPUT_TYPE of the backup job, like: 'DB FULL', 'DB INCR', 'ARCHIVELOG'
	 * @param status        STATUS of the backup job, like: 'FAILED', 'COMPLETED WITH ERRORS'
	 * @param startTime     When the backup job started
	 * @param endTime       When the backup job ended
	 * @param sessionKey    SESSION_KEY of the backup job (to find the output in V$RMAN_OUTPUT)
	 */
	public AlarmEventOraLastBackupFailed(CountersModel cm, String inputType, String status, String startTime, String endTime, long sessionKey)
	{
		super(
				Version.getAppName(), // serviceType
				cm.getServerName(),   // serviceName
				cm.getName(),         // serviceInfo
				inputType,            // extraInfo
				AlarmEvent.Category.RPO,
				AlarmEvent.Severity.WARNING,
				AlarmEvent.ServiceState.UP,
				"Last RMAN Backup of type '" + inputType + "' has STATUS='" + status + "' in Server '" + cm.getServerName() + "'. StartTime='" + startTime + "', EndTime='" + endTime + "', SessionKey=" + sessionKey + ". (for details see the RMAN log, or: select OUTPUT from V$RMAN_OUTPUT where SESSION_KEY = " + sessionKey + " order by RECID)",
				null
				);

		// Set: Time To Live if postpone is enabled
		setTimeToLive(cm);

		// Set the raw data carrier
		setData("inputType=" + inputType + ",status=" + status + ",sessionKey=" + sessionKey);
	}
}
