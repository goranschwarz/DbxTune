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
import com.dbxtune.utils.StringUtil;

/**
 * A replication thread on this replica is not running: the receiver (IO thread) that gets the transactions from the source,
 * or the applier (SQL thread) that applies them.
 * <p>
 * If the thread stopped because of an error it's an ERROR, otherwise (someone stopped it, or it is reconnecting) a WARNING.
 */
public class AlarmEventMyReplicationStopped
extends AlarmEvent
{
	private static final long serialVersionUID = 1L;

	/**
	 * @param cm
	 * @param alarmName     <code>IoThreadNotRunning</code> or <code>SqlThreadNotRunning</code>
	 * @param channelName   The replication channel, '(default)' for the default channel
	 * @param sourceHost    The source server (host:port)
	 * @param serviceState  SERVICE_STATE of the thread: OFF or CONNECTING
	 * @param errorNumber   The last error of the thread (0 if none)
	 * @param errorMessage  The last error of the thread (empty if none)
	 * @param errorTime     When the last error happened (null if none)
	 */
	public AlarmEventMyReplicationStopped(CountersModel cm, String alarmName, String channelName, String sourceHost, String serviceState, int errorNumber, String errorMessage, String errorTime)
	{
		super(
				Version.getAppName(), // serviceType
				cm.getServerName(),   // serviceName
				cm.getName(),         // serviceInfo
				StringUtil.truncate(alarmName + ":" + channelName, 80, true, null), // extraInfo (max 80 characters)
				AlarmEvent.Category.HADR,
				errorNumber != 0 ? AlarmEvent.Severity.ERROR : AlarmEvent.Severity.WARNING,
				AlarmEvent.ServiceState.AFFECTED,
				// The description is stored as max 512 characters, and the error message of the applier can be long (it has the GTID, the binlog position, the table...)
				StringUtil.truncate(
					("IoThreadNotRunning".equals(alarmName)
						? "Replication RECEIVER (IO thread) is " + serviceState + " for channel '" + channelName + "' in Server '" + cm.getServerName() + "'. No new transactions are received from the source '" + sourceHost + "'. "
						: "Replication APPLIER (SQL thread) is " + serviceState + " for channel '" + channelName + "' in Server '" + cm.getServerName() + "'. The transactions from the source '" + sourceHost + "' are not applied. "
					)
					+ (errorNumber != 0
						? "Last error " + errorNumber + " at " + errorTime + ": " + (errorMessage == null ? "" : errorMessage.replace('\n', ' '))
						: "There is no error: the thread was stopped (STOP REPLICA) or is reconnecting."),
					500, true, null),
				null
				);

		// Set: Time To Live if postpone is enabled
		setTimeToLive(cm);

		// Set the raw data carrier
		setData("channel=" + channelName + ",state=" + serviceState + ",errorNumber=" + errorNumber);
	}
}
