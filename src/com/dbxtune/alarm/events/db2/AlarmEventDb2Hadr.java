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
import com.dbxtune.utils.StringUtil;

/**
 * A Db2 HADR standby is not connected, not in PEER state, misses heartbeats, or is behind.
 * <p>
 * In all the constructors <code>standbyLabel</code> is the standby host (and instance)
 */
public class AlarmEventDb2Hadr
extends AlarmEvent
{
	private static final long serialVersionUID = 1L;

	/**
	 * The state of the HADR connection is not what it should be.
	 *
	 * @param cm
	 * @param alarmName      <code>NotConnected</code>, <code>NotPeer</code> or <code>HeartbeatMissed</code>
	 * @param standbyLabel
	 * @param hadrState      HADR_STATE (PEER, REMOTE_CATCHUP, DISCONNECTED_PEER, DISCONNECTED...)
	 * @param connectStatus  HADR_CONNECT_STATUS (CONNECTED, DISCONNECTED, CONGESTED)
	 * @param syncMode       HADR_SYNCMODE
	 * @param detail         For example the number of missed heartbeats, or since when it is disconnected
	 */
	public AlarmEventDb2Hadr(CountersModel cm, String alarmName, String standbyLabel, String hadrState, String connectStatus, String syncMode, String detail)
	{
		super(
				Version.getAppName(), // serviceType
				cm.getServerName(),   // serviceName
				cm.getName(),         // serviceInfo
				StringUtil.truncate(alarmName + ":" + standbyLabel, 80, true, null), // extraInfo (max 80 characters)
				AlarmEvent.Category.HADR,
				"NotConnected".equals(alarmName) ? AlarmEvent.Severity.ERROR : AlarmEvent.Severity.WARNING,
				AlarmEvent.ServiceState.UP,
				"NotConnected".equals(alarmName)
					? "HADR standby '" + standbyLabel + "' is NOT connected to the primary in Server '" + cm.getServerName() + "' (HADR_CONNECT_STATUS=" + connectStatus + ", HADR_STATE=" + hadrState + "). "
						+ "The changes are not sent to the standby: a failover now loses them. " + StringUtil.nullToValue(detail, "")
					: "HeartbeatMissed".equals(alarmName)
						? "HADR standby '" + standbyLabel + "' misses heartbeats in Server '" + cm.getServerName() + "' (" + StringUtil.nullToValue(detail, "") + "). The network between primary and standby may have problems."
						: "HADR standby '" + standbyLabel + "' is NOT in PEER state in Server '" + cm.getServerName() + "' (HADR_STATE=" + hadrState + ", HADR_SYNCMODE=" + syncMode + "). "
							+ "In " + syncMode + " the standby should be in PEER: it is behind, and a failover can lose changes. " + StringUtil.nullToValue(detail, ""),
				null
				);

		// Set: Time To Live if postpone is enabled
		setTimeToLive(cm);

		// Set the raw data carrier
		setData("standby=" + standbyLabel + ",state=" + hadrState + ",connectStatus=" + connectStatus + ",syncMode=" + syncMode);
	}

	/**
	 * The standby is behind in replaying the log.
	 *
	 * @param cm
	 * @param standbyLabel
	 * @param replayLagInSec  How old the newest log is that the standby has replayed, compared to the primary
	 * @param logGapMb        The log the standby has received but not replayed, in MB (null if not known)
	 * @param thresholdInSec
	 */
	public AlarmEventDb2Hadr(CountersModel cm, String standbyLabel, double replayLagInSec, Double logGapMb, double thresholdInSec)
	{
		super(
				Version.getAppName(), // serviceType
				cm.getServerName(),   // serviceName
				cm.getName(),         // serviceInfo
				StringUtil.truncate("ReplayLagInSec:" + standbyLabel, 80, true, null), // extraInfo (max 80 characters)
				AlarmEvent.Category.HADR,
				AlarmEvent.Severity.WARNING,
				AlarmEvent.ServiceState.UP,
				"HADR standby '" + standbyLabel + "' is behind in REPLAY in Server '" + cm.getServerName() + "': the newest log it has replayed is " + replayLagInSec + " seconds older than the primary's"
					+ (logGapMb != null ? " (" + logGapMb + " MB received but not replayed)" : "") + ". "
					+ "A takeover will take longer, and reads on the standby see old data. (thresholdInSec=" + thresholdInSec + ")",
				thresholdInSec
				);

		// Set: Time To Live if postpone is enabled
		setTimeToLive(cm);

		// Set the raw data carrier
		setData("standby=" + standbyLabel + ",replayLagInSec=" + replayLagInSec + ",logGapMb=" + logGapMb);
	}
}
