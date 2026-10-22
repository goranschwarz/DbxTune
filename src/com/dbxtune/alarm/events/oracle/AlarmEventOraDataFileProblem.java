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
import com.dbxtune.utils.StringUtil;

/**
 * Something is wrong with a data file (or a temp file).
 * <p>
 * In all the constructors:
 * <ul>
 *   <li><code>fileKey</code>   is what identifies the file in the alarm: file type + number, like <code>DATA#21</code> (short: it is part of 'extraInfo', which is max 80 characters)</li>
 *   <li><code>fileLabel</code> is how the file is written in the text: <code>#21 '/path/name.dbf' (tablespace [container:]NAME)</code></li>
 * </ul>
 */
public class AlarmEventOraDataFileProblem
extends AlarmEvent
{
	private static final long serialVersionUID = 1L;

	/**
	 * A data file (or a temp file) is not ONLINE, or it needs media recovery: what is in it can not be read.
	 *
	 * @param cm
	 * @param alarmName    <code>FileNotOnline</code> = STATUS is OFFLINE, RECOVER or SYSOFF. <code>FileNeedsRecovery</code> = Oracle says that the file needs recovery, or that it can't read it
	 * @param fileKey
	 * @param fileLabel
	 * @param fileType     DATA or TEMP
	 * @param status       STATUS of the file
	 * @param fileError    What Oracle says is wrong: 'FILE NOT FOUND', 'CANNOT OPEN FILE'... (can be empty)
	 */
	public AlarmEventOraDataFileProblem(CountersModel cm, String alarmName, String fileKey, String fileLabel, String fileType, String status, String fileError)
	{
		super(
				Version.getAppName(), // serviceType
				cm.getServerName(),   // serviceName
				cm.getName(),         // serviceInfo
				alarmName + ":" + fileKey, // extraInfo
				AlarmEvent.Category.DOWN,
				AlarmEvent.Severity.ERROR,
				AlarmEvent.ServiceState.AFFECTED,
				"FileNeedsRecovery".equals(alarmName)
					? "Data file " + fileLabel + " needs media recovery in Server '" + cm.getServerName() + "'. STATUS='" + status + "'" + (StringUtil.hasValue(fileError) ? ", Error='" + fileError + "'" : "") + ". "
						+ "See V$RECOVER_FILE. Fix: RMAN> RESTORE DATAFILE n; RECOVER DATAFILE n; (only RECOVER if the file is there), then bring it ONLINE."
					: "TEMP".equals(fileType)
						? "Temp file " + fileLabel + " has STATUS='" + status + "' in Server '" + cm.getServerName() + "': sorts that needs it will fail. Fix: ALTER DATABASE TEMPFILE '<name>' ONLINE"
						: "Data file " + fileLabel + " has STATUS='" + status + "' in Server '" + cm.getServerName() + "': what is in it can not be read (ORA-00376). "
							+ "Offline on purpose? Add the tablespace to the skip list of this alarm. Otherwise: recover the file and bring it ONLINE.",
				null
				);

		// Set: Time To Live if postpone is enabled
		setTimeToLive(cm);

		// Set the raw data carrier
		setData("file=" + fileKey + ",status=" + status + ",error=" + fileError);
	}

	/**
	 * RMAN (a backup or a validate) has found corrupt blocks in a data file.
	 *
	 * @param cm
	 * @param fileKey
	 * @param fileLabel
	 * @param corruptBlocks   Number of blocks in V$DATABASE_BLOCK_CORRUPTION for the file
	 * @param threshold
	 */
	public AlarmEventOraDataFileProblem(CountersModel cm, String fileKey, String fileLabel, long corruptBlocks, int threshold)
	{
		super(
				Version.getAppName(), // serviceType
				cm.getServerName(),   // serviceName
				cm.getName(),         // serviceInfo
				"CorruptBlocks:" + fileKey, // extraInfo
				AlarmEvent.Category.OTHER,
				AlarmEvent.Severity.ERROR,
				AlarmEvent.ServiceState.AFFECTED,
				"Data file " + fileLabel + " has " + corruptBlocks + " corrupt block(s) in Server '" + cm.getServerName() + "', found by RMAN (see V$DATABASE_BLOCK_CORRUPTION). "
						+ "A query that reads one of them gets ORA-01578. Fix: RMAN> RECOVER CORRUPTION LIST; (threshold=" + threshold + ")",
				threshold
				);

		// Set: Time To Live if postpone is enabled
		setTimeToLive(cm);

		// Set the raw data carrier
		setData("file=" + fileKey + ",corruptBlocks=" + corruptBlocks);
	}

	/**
	 * A data file has been in backup mode (ALTER DATABASE / TABLESPACE BEGIN BACKUP) for a long time: someone, or a script, forgot END BACKUP.
	 *
	 * @param cm
	 * @param fileKey
	 * @param fileLabel
	 * @param backupModeSince     When BEGIN BACKUP was done
	 * @param backupModeInHours   Hours since then
	 * @param thresholdInHours
	 */
	public AlarmEventOraDataFileProblem(CountersModel cm, String fileKey, String fileLabel, String backupModeSince, double backupModeInHours, double thresholdInHours)
	{
		super(
				Version.getAppName(), // serviceType
				cm.getServerName(),   // serviceName
				cm.getName(),         // serviceInfo
				"BackupMode:" + fileKey, // extraInfo
				AlarmEvent.Category.OTHER,
				AlarmEvent.Severity.WARNING,
				AlarmEvent.ServiceState.UP,
				"Data file " + fileLabel + " has been in BACKUP MODE for " + backupModeInHours + " hours (since '" + backupModeSince + "') in Server '" + cm.getServerName() + "'. "
						+ "That gives much more redo, and the instance can not be shut down normally. If no backup is running: ALTER DATABASE END BACKUP. (thresholdInHours=" + thresholdInHours + ")",
				thresholdInHours
				);

		// Set: Time To Live if postpone is enabled
		setTimeToLive(cm);

		// Set the raw data carrier
		setData("file=" + fileKey + ",backupModeInHours=" + backupModeInHours);
	}
}
