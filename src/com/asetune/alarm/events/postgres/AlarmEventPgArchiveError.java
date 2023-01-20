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
package com.asetune.alarm.events.postgres;

import com.asetune.Version;
import com.asetune.alarm.events.AlarmEvent;
import com.asetune.cm.CountersModel;

public class AlarmEventPgArchiveError
extends AlarmEvent 
{
	private static final long serialVersionUID = 1L;

	/**
	 * When any age has expired
	 * 
	 * @param cm
	 * @param threshold
	 * @param logicalName
	 * @param colName
	 */
	public AlarmEventPgArchiveError(CountersModel cm, int thresholdCount, int failed_count_diff, int failed_count_abs)
	{
		super(
				Version.getAppName(), // serviceType
				cm.getServerName(),   // serviceName
				cm.getName(),         // serviceInfo
				null,                 // extraInfo
				AlarmEvent.Category.RPO,
				AlarmEvent.Severity.WARNING, 
				AlarmEvent.ServiceState.UP, 
				"Archive Error in Server '" + cm.getServerName() + "', failed_count_diff=" + failed_count_diff + " (failed_count_abs=" + failed_count_abs + "). (thresholdCount=" + thresholdCount + ")",
				thresholdCount
				);

		// Set: Time To Live if postpone is enabled
		setTimeToLive(cm);

		// Set the raw data carrier
		setData("failed_count_diff=" + failed_count_diff + ", failed_count_abs=" + failed_count_abs);
	}

	public AlarmEventPgArchiveError(CountersModel cm, int thresholdInSec, int last_archived_in_seconds, String last_archived_time)
	{
		super(
				Version.getAppName(), // serviceType
				cm.getServerName(),   // serviceName
				cm.getName(),         // serviceInfo
				null,                 // extraInfo
				AlarmEvent.Category.RPO,
				AlarmEvent.Severity.WARNING, 
				AlarmEvent.ServiceState.UP, 
				"Archive AGE in Server '" + cm.getServerName() + "', last_archived_in_seconds=" + last_archived_in_seconds + " (last_archived_time=" + last_archived_time + "). (thresholdInSec=" + thresholdInSec + ")",
				thresholdInSec
				);

		// Adjust the Alarm Full Duration with X seconds
		setFullDurationAdjustmentInSec( thresholdInSec );
		
		// Set: Time To Live if postpone is enabled
		setTimeToLive(cm);

		// Set the raw data carrier
		setData("last_archived_in_seconds=" + last_archived_in_seconds);
	}

}
