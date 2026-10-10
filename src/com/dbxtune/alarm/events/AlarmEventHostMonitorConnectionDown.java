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

import com.dbxtune.Version;
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
}
