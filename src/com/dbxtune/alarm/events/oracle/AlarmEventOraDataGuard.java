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
 * A standby database (Data Guard) is behind, or the primary can't send redo to it.
 * <p>
 * NOTE: These alarms have NOT been tested against a real standby database (only with made up rows). They are off by default, see CmDataGuard.
 * <p>
 * In all the constructors <code>standbyLabel</code> is the name of the standby: DB_UNIQUE_NAME of the destination, or the destination name (LOG_ARCHIVE_DEST_n)
 */
public class AlarmEventOraDataGuard
extends AlarmEvent
{
	private static final long serialVersionUID = 1L;

	/**
	 * The standby is behind in time: in what it has received (transport) or in what it has applied (apply).
	 *
	 * @param cm
	 * @param alarmName      <code>ApplyLagInSec</code> or <code>TransportLagInSec</code>
	 * @param standbyLabel
	 * @param lagInSec       How old the redo is that the standby has applied / received
	 * @param seqGap         Number of archived logs that the standby has not applied / received (null if not known)
	 * @param thresholdInSec
	 */
	public AlarmEventOraDataGuard(CountersModel cm, String alarmName, String standbyLabel, double lagInSec, Double seqGap, double thresholdInSec)
	{
		super(
				Version.getAppName(), // serviceType
				cm.getServerName(),   // serviceName
				cm.getName(),         // serviceInfo
				StringUtil.truncate(alarmName + ":" + standbyLabel, 80, true, null), // extraInfo (max 80 characters)
				AlarmEvent.Category.HADR,
				AlarmEvent.Severity.WARNING,
				AlarmEvent.ServiceState.UP,
				"ApplyLagInSec".equals(alarmName)
					? "Standby '" + standbyLabel + "' is behind in APPLY in Server '" + cm.getServerName() + "': the newest redo it has applied is " + lagInSec + " seconds old"
						+ (seqGap != null ? " (" + seqGap.intValue() + " archived logs not applied)" : "") + ". "
						+ "A failover to it will take longer, and queries on it sees old data. Is the apply (MRP) running on the standby? (thresholdInSec=" + thresholdInSec + ")"
					: "Standby '" + standbyLabel + "' is behind in TRANSPORT in Server '" + cm.getServerName() + "': the newest redo it has received is " + lagInSec + " seconds old"
						+ (seqGap != null ? " (" + seqGap.intValue() + " archived logs not received)" : "") + ". "
						+ "If the primary is lost now, that redo is lost. Check the network and the archive destination. (thresholdInSec=" + thresholdInSec + ")",
				thresholdInSec
				);

		// Set: Time To Live if postpone is enabled
		setTimeToLive(cm);

		// Set the raw data carrier
		setData("standby=" + standbyLabel + ",lagInSec=" + lagInSec + ",seqGap=" + seqGap);
	}

	/**
	 * The standby is many log sequences behind the primary.
	 *
	 * @param cm
	 * @param standbyLabel
	 * @param primarySeq    The last complete log sequence on the primary
	 * @param shippedSeq    The last log sequence that the standby has received (null if not known)
	 * @param appliedSeq    The last log sequence that the standby has applied (null if not known)
	 * @param seqGap        The biggest of: logs not received, logs not applied
	 * @param threshold
	 */
	public AlarmEventOraDataGuard(CountersModel cm, String standbyLabel, long primarySeq, Double shippedSeq, Double appliedSeq, long seqGap, int threshold)
	{
		super(
				Version.getAppName(), // serviceType
				cm.getServerName(),   // serviceName
				cm.getName(),         // serviceInfo
				StringUtil.truncate("GapSequences:" + standbyLabel, 80, true, null), // extraInfo (max 80 characters)
				AlarmEvent.Category.HADR,
				AlarmEvent.Severity.WARNING,
				AlarmEvent.ServiceState.UP,
				"Standby '" + standbyLabel + "' is " + seqGap + " log sequences behind the primary in Server '" + cm.getServerName() + "'. "
						+ "Primary=" + primarySeq + ", received=" + (shippedSeq == null ? "unknown" : "" + shippedSeq.longValue()) + ", applied=" + (appliedSeq == null ? "unknown" : "" + appliedSeq.longValue()) + ". "
						+ "The archived logs it needs must be kept on the primary until it has them. (threshold=" + threshold + ")",
				threshold
				);

		// Set: Time To Live if postpone is enabled
		setTimeToLive(cm);

		// Set the raw data carrier
		setData("standby=" + standbyLabel + ",primarySeq=" + primarySeq + ",shippedSeq=" + shippedSeq + ",appliedSeq=" + appliedSeq);
	}

	/**
	 * The destination of the standby is in a state where no redo is sent, or the standby has a gap that it can't fill by itself.
	 *
	 * @param cm
	 * @param standbyLabel
	 * @param status        STATUS of the destination: ERROR, DISABLED, FULL, BAD PARAM...
	 * @param gapStatus     GAP_STATUS of the destination: NO GAP, RESOLVABLE GAP, UNRESOLVABLE GAP...
	 * @param error         The error message of the destination (can be empty)
	 * @param statusRegex   The states that we send an alarm for
	 * @param gapRegex      The gap states that we send an alarm for
	 */
	public AlarmEventOraDataGuard(CountersModel cm, String standbyLabel, String status, String gapStatus, String error, String statusRegex, String gapRegex)
	{
		super(
				Version.getAppName(), // serviceType
				cm.getServerName(),   // serviceName
				cm.getName(),         // serviceInfo
				StringUtil.truncate("StandbyDestError:" + standbyLabel, 80, true, null), // extraInfo (max 80 characters)
				AlarmEvent.Category.HADR,
				AlarmEvent.Severity.ERROR,
				AlarmEvent.ServiceState.AFFECTED,
				"The redo transport to standby '" + standbyLabel + "' has a problem in Server '" + cm.getServerName() + "': STATUS='" + status + "', GAP_STATUS='" + gapStatus + "'"
						+ (StringUtil.hasValue(error) ? ", ERROR='" + StringUtil.truncate(error.replace('\n', ' '), 150, true, null) + "'" : "") + ". "
						+ "The standby gets no redo until this is fixed. (states=" + statusRegex + ", gapStates=" + gapRegex + ")",
				null
				);

		// Set: Time To Live if postpone is enabled
		setTimeToLive(cm);

		// Set the raw data carrier
		setData("standby=" + standbyLabel + ",status=" + status + ",gapStatus=" + gapStatus);
	}
}
