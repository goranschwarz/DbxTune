/*******************************************************************************
 * Copyright (C) 2010-2020 Goran Schwarz
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
package com.asetune.alarm.events.sqlserver;

import com.asetune.Version;
import com.asetune.alarm.events.AlarmEvent;
import com.asetune.cm.CountersModel;

public class AlarmEventPerfCounter
extends AlarmEvent 
{
	private static final long serialVersionUID = 1L;

	/**
	 * If a PerfCounter value is "out of whack"...
	 * 
	 * @param cm               The CM Object
	 * @param alarmSeverity    What severity is this off: INFO, WARNING or ERROR
	 * @param counterName      Name of the PerfMon counter
	 * @param counterVal       Value of the counter
	 * @param warningText      Text provided by the caller
	 * @param threshold        The threshold value that was crossed
	 */
	public AlarmEventPerfCounter(CountersModel cm, Severity alarmSeverity, String counterName, double counterVal, String warningText, Number threshold)
	{
		super(
				Version.getAppName(), // serviceType
				cm.getServerName(),   // serviceName
				cm.getName(),         // serviceInfo
				counterName,              // extraInfo
				AlarmEvent.Category.OTHER,
				alarmSeverity, 
				AlarmEvent.ServiceState.UP, 
				warningText,
				threshold);

		setData("counterVal="+counterVal);

		// Set: Time To Live if postpone is enabled
		setTimeToLive(cm);
	}
}
