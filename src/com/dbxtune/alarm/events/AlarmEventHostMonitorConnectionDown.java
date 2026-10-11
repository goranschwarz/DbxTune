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
package com.dbxtune.alarm.events;

import java.sql.Timestamp;

import com.dbxtune.Version;
import com.dbxtune.cm.CountersModel;
import com.dbxtune.cm.os.CmOsUptime;
import com.dbxtune.utils.TimeUtils;

public class AlarmEventHostMonitorConnectionDown
extends AlarmEvent
{
	private static final long serialVersionUID = 1L;

	// Can and SHOULD be cancelled by: AlarmHandler.checkForCancelations()
	// Meaning we do NOT need any CM to be "refreshed" for this alarm to be cancelled
	// The Collector re-raises it on every sample while the SSH connection fails, when it's no longer re-raised it will be cancelled
	@Override
	public boolean isAlwaysCancelable()
	{
		return true;
	}

	/**
	 * Host Monitoring (SSH) can't connect to the monitored host
	 *
	 * @param srvName                  - Name of the monitored DBMS server
	 * @param sshHostname              - SSH hostname we try to connect to
	 * @param sshPort                  - SSH port
	 * @param sshUsername              - SSH username
	 * @param secSinceFailStart        - Number of seconds since the connection started to fail
	 * @param thresholdInSec           - The threshold that was used/crossed
	 * @param lastConnectException     - Last Exception when trying to connect (can be null)
	 */
	public AlarmEventHostMonitorConnectionDown(String srvName, String sshHostname, int sshPort, String sshUsername, long secSinceFailStart, int thresholdInSec, Exception lastConnectException)
	{
		super(
				Version.getAppName(), // serviceType
				srvName,              // serviceName
				"HostMonitor",        // serviceInfo
				sshHostname,          // extraInfo
				AlarmEvent.Category.DOWN,
				AlarmEvent.Severity.WARNING,
				AlarmEvent.ServiceState.UP,
				"Host Monitoring can't connect to host '" + sshHostname + "' on port " + sshPort + " with user '" + sshUsername + "' for " + TimeUtils.msToTimeStr("%HH:%MM:%SS", secSinceFailStart*1000) + " (HH:MM:SS) (thresholdInSec=" + thresholdInSec + "). OS Counters are NOT collected.",
				thresholdInSec
				);

		// Adjust the Alarm Full Duration with X seconds
		setFullDurationAdjustmentInSec(thresholdInSec);

		setExtendedDescription("Last connect error: " + lastConnectException, null);

		// Set the raw data carrier
		setData(sshHostname + ":" + sshPort); // note: limit is 80 characters...
	}

	/** Max difference between two OS start times that are still "the same" (it is calculated as 'sample time - up time': Unix 'uptime' only has minutes, the Windows value is an average over the sample) */
	private static final long SAME_START_TIME_TOLERANCE_MS = 5 * 60 * 1000;

	/**
	 * When the alarm is CANCELLED: set the cancel description to the OS start time, and if the OS was restarted or not.
	 * <p>
	 * CmOsUptime is refreshed also when the connection is down (and then has no data, or old data), so the start time
	 * from BEFORE the outage can not be read from it here. The Collector remembers it while it is connected, and passes it in.
	 *
	 * @param cmOsUptime             The CmOsUptime CM (can be null, then nothing is done at CANCEL)
	 * @param startTimeBeforeOutage  CmOsUptime.getOsStartTime() from the last sample where the connection was OK (null if not known)
	 */
	public void setOsStartTimeOnCancel(CmOsUptime cmOsUptime, Timestamp startTimeBeforeOutage)
	{
		// Create a callback that may change the AlarmEvent descriptions
		setAlarmDescriptionProvider(cmOsUptime, new AlarmDescriptionProvider()
		{
			@Override
			public void setValues(CountersModel cm, AlarmEvent alarmEvent, AlarmPhase phase)
			{
				// RAISE and RE-RAISE: keep the last connect error (set in the constructor)
				if (AlarmDescriptionProvider.AlarmPhase.CANCEL.equals(phase))
				{
					setCancelValues((CmOsUptime) cm, alarmEvent, startTimeBeforeOutage);
				}
			}
		});
	}

	/**
	 * At CANCEL: was the OS restarted? Compare the OS start time now, with the one from BEFORE the outage.<br>
	 * The answer goes into the "cancel description" (the extended description keeps the last connect error).
	 */
	private static void setCancelValues(CmOsUptime cmOsUptime, AlarmEvent alarmEvent, Timestamp startTimeBeforeOutage)
	{
		if (cmOsUptime == null)
			return;

		// Only if CmOsUptime got data AFTER the alarm was last raised, otherwise it is still the data from before (or during) the outage
		Timestamp sampleTime    = cmOsUptime.getTimestamp();
		long      lastRaiseTime = Math.max(alarmEvent.getCrTime(), alarmEvent.getReRaiseTime());
		if (sampleTime == null || sampleTime.getTime() <= lastRaiseTime)
			return;

		Timestamp startTimeNow = cmOsUptime.getOsStartTime();
		if (startTimeNow == null)
			return;

		String startTimeNowStr = "OS start time: " + TimeUtils.toStringYmdHms(startTimeNow) + ", up time: " + TimeUtils.msToTimeStrDHMS(sampleTime.getTime() - startTimeNow.getTime());
		String line1;
		String line2 = null;

		if (startTimeBeforeOutage == null)
		{
			// The collector was (re)started during the outage, so we do not know the start time from before it.
			line1 = startTimeNowStr;
			line2 = "The start time from before the outage is not known (the collector was started during the outage), so it can not be told if the OS was restarted. The alarm was raised at " + TimeUtils.toStringYmdHms(alarmEvent.getCrTime()) + ".";
		}
		else if (Math.abs(startTimeNow.getTime() - startTimeBeforeOutage.getTime()) > SAME_START_TIME_TOLERANCE_MS)
		{
			line1 = "The OS was RESTARTED. " + startTimeNowStr + " (before the outage: " + TimeUtils.toStringYmdHms(startTimeBeforeOutage) + ")";
		}
		else
		{
			line1 = "The OS was NOT restarted. " + startTimeNowStr + " (same as before the outage)";
			line2 = "So it was probably a network problem, or the SSH server did not accept new connections.";
		}

		alarmEvent.setCancelDescription(line1 + (line2 == null ? "" : ". " + line2));
	}
}
