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

public class AlarmEventPgArchiveRate
extends AlarmEvent 
{
	private static final long serialVersionUID = 1L;

	/**
	 * When archiving rate is "high"
	 * 
	 * @param cm
	 * @param archivedCountInLastHour 
	 * @param threshold
	 * @param logicalName
	 * @param colName
	 */
	public AlarmEventPgArchiveRate(CountersModel cm, double thresholdInMb, double archivedMbInLastHour, int archivedCountInLastHour)
	{
		super(
				Version.getAppName(), // serviceType
				cm.getServerName(),   // serviceName
				cm.getName(),         // serviceInfo
				null,                 // extraInfo
				AlarmEvent.Category.SPACE,
				AlarmEvent.Severity.INFO, 
				AlarmEvent.ServiceState.UP, 
				"Archive MB/Count in Server '" + cm.getServerName() + "', archivedMbInLastHour=" + archivedMbInLastHour + ", archivedCountInLastHour=" + archivedCountInLastHour + ". (thresholdInMb=" + thresholdInMb + ")",
				thresholdInMb
				);

//		// Adjust the Alarm Full Duration with X seconds
//		setFullDurationAdjustmentInSec( thresholdInSec );
		
		// Set: Time To Live if postpone is enabled
		setTimeToLive(cm);

		// Set the raw data carrier
		setData("archivedMbInLastHour=" + archivedMbInLastHour + ", archivedCountInLastHour=" + archivedCountInLastHour);
	}

}
