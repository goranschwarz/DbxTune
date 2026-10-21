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
 * This replica is behind its source: the data on it is older than the threshold.
 */
public class AlarmEventMyReplicationLag
extends AlarmEvent
{
	private static final long serialVersionUID = 1L;

	/**
	 * @param cm
	 * @param channelName     The replication channel, '(default)' for the default channel
	 * @param sourceHost      The source server (host:port)
	 * @param applyLagInSec   How old (on the source) the transaction is that the applier is working on (or the last applied one, if the applier is stopped)
	 * @param sqlThreadState  SERVICE_STATE of the applier (SQL thread): ON or OFF
	 * @param desiredDelaySec A delayed replica (SOURCE_DELAY) is behind on purpose: the lag is compared to the threshold + this
	 * @param thresholdInSec
	 */
	public AlarmEventMyReplicationLag(CountersModel cm, String channelName, String sourceHost, double applyLagInSec, String sqlThreadState, int desiredDelaySec, int thresholdInSec)
	{
		super(
				Version.getAppName(), // serviceType
				cm.getServerName(),   // serviceName
				cm.getName(),         // serviceInfo
				StringUtil.truncate("ApplyLagInSec:" + channelName, 80, true, null), // extraInfo (max 80 characters)
				AlarmEvent.Category.HADR,
				AlarmEvent.Severity.WARNING,
				AlarmEvent.ServiceState.UP,
				"Replica is " + applyLagInSec + " seconds behind the source '" + sourceHost + "' for channel '" + channelName + "' in Server '" + cm.getServerName() + "'. "
						+ ("ON".equalsIgnoreCase(sqlThreadState)
							? "The applier (SQL thread) is running, but is behind (a big transaction, waiting for a lock, or the replica is too slow). "
							: "The applier (SQL thread) is " + sqlThreadState + ", so nothing is applied. ")
						+ (desiredDelaySec > 0 ? "This is a delayed replica (SOURCE_DELAY=" + desiredDelaySec + "), the delay is added to the threshold. " : "")
						+ "(thresholdInSec=" + thresholdInSec + ")",
				thresholdInSec
				);

		// Set: Time To Live if postpone is enabled
		setTimeToLive(cm);

		// Set the raw data carrier
		setData("channel=" + channelName + ",applyLagInSec=" + applyLagInSec + ",sqlThread=" + sqlThreadState);
	}
}
