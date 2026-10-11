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

public class AlarmEventOraLongRunningStatement
extends AlarmEvent
{
	private static final long serialVersionUID = 1L;

	/**
	 * A session has been executing the same call for a long time
	 *
	 * @param cm
	 * @param thresholdInSec
	 * @param sid          The session
	 * @param inSeconds    For how long has it been executing
	 * @param startTime    When did the SQL statement start
	 * @param username     Who is it
	 * @param program      What program does it use
	 * @param sqlId        SQL_ID of the statement it is executing
	 */
	public AlarmEventOraLongRunningStatement(CountersModel cm, Number thresholdInSec, long sid, Number inSeconds, String startTime, String username, String program, String sqlId)
	{
		super(
				Version.getAppName(), // serviceType
				cm.getServerName(),   // serviceName
				cm.getName(),         // serviceInfo
				"sid=" + sid,         // extraInfo
				AlarmEvent.Category.OTHER,
				AlarmEvent.Severity.INFO,
				AlarmEvent.ServiceState.UP,
				"Found Long running SQL Statement in '" + cm.getServerName() + "'. SID=" + sid + ", Seconds=" + inSeconds + ", StartTime='" + startTime + "', Username='" + username + "', Program='" + program + "', SQL_ID='" + sqlId + "'. (thresholdInSec=" + thresholdInSec + ")",
				thresholdInSec);

		// Adjust the Alarm Full Duration with X seconds
		setFullDurationAdjustmentInSec( thresholdInSec == null ? 0 : thresholdInSec.intValue() );

		// Set: Time To Live if postpone is enabled
		setTimeToLive(cm);

		// Set the raw data carrier
		setData("sid=" + sid + ",seconds=" + inSeconds + ",sqlId=" + sqlId);
	}
}
