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

public class AlarmEventDb2LongRunningStatement
extends AlarmEvent
{
	private static final long serialVersionUID = 1L;

	/**
	 * A connection has been executing the same statement for a long time
	 *
	 * @param cm
	 * @param thresholdInSec
	 * @param applHandle    The connection (application handle)
	 * @param inSeconds     For how long has it been executing
	 * @param startTime     When did the statement start
	 * @param authId        Who is it (SESSION_AUTH_ID)
	 * @param applName      What program does it use
	 * @param executableId  EXECUTABLE_ID of the statement (the same as in the tab 'Statements')
	 */
	public AlarmEventDb2LongRunningStatement(CountersModel cm, Number thresholdInSec, long applHandle, Number inSeconds, String startTime, String authId, String applName, String executableId)
	{
		super(
				Version.getAppName(),            // serviceType
				cm.getServerName(),              // serviceName
				cm.getName(),                    // serviceInfo
				"applHandle=" + applHandle,      // extraInfo
				AlarmEvent.Category.OTHER,
				AlarmEvent.Severity.INFO,
				AlarmEvent.ServiceState.UP,
				"Found Long running SQL Statement in '" + cm.getServerName() + "'. ApplHandle=" + applHandle + ", Seconds=" + inSeconds + ", StartTime='" + startTime + "', AuthId='" + authId + "', ApplName='" + applName + "', ExecutableId='" + executableId + "'. (thresholdInSec=" + thresholdInSec + ")",
				thresholdInSec);

		// Adjust the Alarm Full Duration with X seconds
		setFullDurationAdjustmentInSec( thresholdInSec == null ? 0 : thresholdInSec.intValue() );

		// Set: Time To Live if postpone is enabled
		setTimeToLive(cm);

		// Set the raw data carrier
		setData("applHandle=" + applHandle + ",seconds=" + inSeconds + ",executableId=" + executableId);
	}
}
