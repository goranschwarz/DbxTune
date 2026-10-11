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
package com.dbxtune.alarm.events.oracle;

import com.dbxtune.Version;
import com.dbxtune.alarm.events.AlarmEvent;
import com.dbxtune.cm.CountersModel;

public class AlarmEventOraBlockingSession
extends AlarmEvent
{
	private static final long serialVersionUID = 1L;

	/**
	 * A session is blocking other sessions (it holds a lock that others are waiting for)
	 * <p>
	 * Note: In Oracle the session is identified by SID (the term SPID is the Operating System process id)
	 *
	 * @param cm
	 * @param thresholdInSec
	 * @param sid                         The session that is blocking others
	 * @param username                    Who is it
	 * @param program                     What program does it use
	 * @param blockingOthersMaxTimeInSec  How long has the session that has waited the longest been blocked
	 * @param blockingOtherSids           The sessions that are blocked
	 * @param blockCount                  How many sessions that are blocked
	 */
	public AlarmEventOraBlockingSession(CountersModel cm, Number thresholdInSec, long sid, String username, String program, Number blockingOthersMaxTimeInSec, String blockingOtherSids, Number blockCount)
	{
		super(
				Version.getAppName(), // serviceType
				cm.getServerName(),   // serviceName
				cm.getName(),         // serviceInfo
				"sid=" + sid,         // extraInfo
				AlarmEvent.Category.LOCK,
				AlarmEvent.Severity.WARNING,
				AlarmEvent.ServiceState.AFFECTED,
				"Found Blocking session in '" + cm.getServerName() + "'. SID=" + sid + ", Username='" + username + "', Program='" + program + "', BlockingOthersMaxTimeInSec=" + blockingOthersMaxTimeInSec + ", BlockingOtherSids=" + blockingOtherSids + ", BlockCount=" + blockCount + ". (thresholdInSec=" + thresholdInSec + ")",
				thresholdInSec);

		// Adjust the Alarm Full Duration with X seconds
		setFullDurationAdjustmentInSec( thresholdInSec == null ? 0 : thresholdInSec.intValue() );

		// Set: Time To Live if postpone is enabled
		setTimeToLive(cm);

		// Set the raw data carrier
		setData("sid=" + sid + ",blockingOthersMaxTimeInSec=" + blockingOthersMaxTimeInSec + ",blockCount=" + blockCount);
	}
}
