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
package com.dbxtune.alarm.events.mysql;

import com.dbxtune.Version;
import com.dbxtune.alarm.events.AlarmEvent;
import com.dbxtune.cm.CountersModel;

/**
 * A MySQL connection blocks other connections (row locks or metadata locks), and is not blocked itself (it is the root cause).
 * <p>
 * MySQL calls a connection a "processlist id" (CONNECTION_ID(), the Id of SHOW PROCESSLIST): the generic AlarmEventBlockingLockAlarm says SPID.
 */
public class AlarmEventMyBlockingSession
extends AlarmEvent
{
	private static final long serialVersionUID = 1L;

	/**
	 * @param cm
	 * @param thresholdInSec
	 * @param pid                   The connection that blocks others
	 * @param user                  Its user
	 * @param host                  Its client host
	 * @param command               What it does: Query, Sleep (= it waits for the client, and holds its locks)...
	 * @param blockingOthersMaxSec  The longest time one of the blocked connections has waited
	 * @param blockingOtherPids     The connections that waits for it
	 * @param blockCount            How many connections that waits for it
	 */
	public AlarmEventMyBlockingSession(CountersModel cm, Number thresholdInSec, long pid, String user, String host, String command, Number blockingOthersMaxSec, String blockingOtherPids, int blockCount)
	{
		super(
				Version.getAppName(), // serviceType
				cm.getServerName(),   // serviceName
				cm.getName(),         // serviceInfo
				"pid=" + pid,         // extraInfo
				AlarmEvent.Category.LOCK,
				AlarmEvent.Severity.WARNING,
				AlarmEvent.ServiceState.AFFECTED,
				"Found Blocking locks in '" + cm.getServerName() + "'. Connection pid=" + pid + " (user='" + user + "', host='" + host + "', command='" + command + "') blocks "
						+ blockCount + " other connection(s) " + blockingOtherPids + ", the longest wait is " + blockingOthersMaxSec + " seconds. (thresholdInSec=" + thresholdInSec + ")",
				thresholdInSec);

		// Adjust the Alarm Full Duration with X seconds
		setFullDurationAdjustmentInSec( thresholdInSec == null ? 0 : thresholdInSec.intValue() );

		// Set: Time To Live if postpone is enabled
		setTimeToLive(cm);

		// Set the raw data carrier
		setData("pid=" + pid + ",blockingOthersMaxSec=" + blockingOthersMaxSec + ",blockCount=" + blockCount);
	}
}
