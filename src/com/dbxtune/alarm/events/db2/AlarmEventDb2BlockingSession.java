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

public class AlarmEventDb2BlockingSession
extends AlarmEvent
{
	private static final long serialVersionUID = 1L;

	/**
	 * A connection is blocking other connections (it holds a lock that others are waiting for), and is not blocked itself
	 * <p>
	 * Note: In Db2 the connection is identified by its application handle (what FORCE APPLICATION takes)
	 *
	 * @param cm
	 * @param thresholdInSec
	 * @param applHandle                  The connection that is blocking others
	 * @param authId                      Who is it (SESSION_AUTH_ID)
	 * @param applName                    What program does it use
	 * @param clientHost                  From where
	 * @param blockingOthersMaxTimeInSec  How long has the connection that has waited the longest been blocked
	 * @param blockingOtherApplHandles    The connections that are blocked
	 * @param blockCount                  How many connections that are blocked
	 */
	public AlarmEventDb2BlockingSession(CountersModel cm, Number thresholdInSec, long applHandle, String authId, String applName, String clientHost, Number blockingOthersMaxTimeInSec, String blockingOtherApplHandles, Number blockCount)
	{
		super(
				Version.getAppName(),            // serviceType
				cm.getServerName(),              // serviceName
				cm.getName(),                    // serviceInfo
				"applHandle=" + applHandle,      // extraInfo
				AlarmEvent.Category.LOCK,
				AlarmEvent.Severity.WARNING,
				AlarmEvent.ServiceState.AFFECTED,
				"Found Blocking connection in '" + cm.getServerName() + "'. ApplHandle=" + applHandle + ", AuthId='" + authId + "', ApplName='" + applName + "', ClientHost='" + clientHost + "', BlockingOthersMaxTimeInSec=" + blockingOthersMaxTimeInSec + ", BlockingOtherApplHandles=" + blockingOtherApplHandles + ", BlockCount=" + blockCount + ". (thresholdInSec=" + thresholdInSec + ")",
				thresholdInSec);

		// Adjust the Alarm Full Duration with X seconds
		setFullDurationAdjustmentInSec( thresholdInSec == null ? 0 : thresholdInSec.intValue() );

		// Set: Time To Live if postpone is enabled
		setTimeToLive(cm);

		// Set the raw data carrier
		setData("applHandle=" + applHandle + ",blockingOthersMaxTimeInSec=" + blockingOthersMaxTimeInSec + ",blockCount=" + blockCount);
	}
}
