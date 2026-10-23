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
package com.dbxtune.alarm.events.db2;

import com.dbxtune.Version;
import com.dbxtune.alarm.events.AlarmEvent;
import com.dbxtune.cm.CountersModel;

/**
 * Db2 can't archive its transaction log files (LOGARCHMETH1 or LOGARCHMETH2).
 * <p>
 * The log files that can't be archived stay in the active log path. Db2 keeps retrying, but until it works:
 * <ul>
 *   <li>the logs are not saved anywhere else, so a restore + roll forward can't go past the first failing log (the recovery point is at risk)</li>
 *   <li>the active log path fills up, and when it is full the database stops (SQL0964C, log full) or hangs</li>
 * </ul>
 */
public class AlarmEventDb2LogArchiveFailed
extends AlarmEvent
{
	private static final long serialVersionUID = 1L;

	/**
	 * @param cm
	 * @param dbname           The database
	 * @param archiveMethod    1 or 2 (LOGARCHMETH1 or LOGARCHMETH2)
	 * @param archiveTarget    The value of LOGARCHMETH1/2, for example DISK:/db2/archive/ (null if not known)
	 * @param firstFailedLog   The first log file number that could not be archived
	 * @param currentActiveLog The log file number Db2 writes to right now (how many logs are waiting = current - first failed)
	 */
	public AlarmEventDb2LogArchiveFailed(CountersModel cm, String dbname, int archiveMethod, String archiveTarget, Long firstFailedLog, Long currentActiveLog)
	{
		super(
				Version.getAppName(), // serviceType
				cm.getServerName(),   // serviceName
				cm.getName(),         // serviceInfo
				dbname + ":LOGARCHMETH" + archiveMethod, // extraInfo
				AlarmEvent.Category.RPO,
				AlarmEvent.Severity.ERROR,
				AlarmEvent.ServiceState.AFFECTED,
				"Log archiving FAILS for LOGARCHMETH" + archiveMethod + (archiveTarget == null ? "" : " (" + archiveTarget + ")") + " in Server '" + cm.getServerName() + "', dbname '" + dbname + "'. "
					+ (firstFailedLog == null ? "" : "First log that could not be archived: S" + String.format("%07d", firstFailedLog) + ".LOG")
					+ (firstFailedLog != null && currentActiveLog != null && currentActiveLog >= firstFailedLog ? ", " + (currentActiveLog - firstFailedLog) + " log file(s) waiting. " : (firstFailedLog == null ? "" : ". "))
					+ "The logs stay in the active log path until it works (it can fill up), and roll forward can't go past them. See the Db2 diagnostic log (db2diag) for the reason.",
				null
				);

		// Set: Time To Live if postpone is enabled
		setTimeToLive(cm);

		// Set the raw data carrier
		setData("dbname=" + dbname + ",method=" + archiveMethod + ",firstFailedLog=" + firstFailedLog + ",currentActiveLog=" + currentActiveLog);
	}
}
