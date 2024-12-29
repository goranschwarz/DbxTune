/*******************************************************************************
 * Copyright (C) 2010-2025 Goran Schwarz
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
package com.dbxtune.alarm.events.postgres;

import com.dbxtune.Version;
import com.dbxtune.alarm.events.AlarmEvent;
import com.dbxtune.cm.CountersModel;

public class AlarmEventPgChecksumFailure
extends AlarmEvent 
{
	private static final long serialVersionUID = 1L;

	/**
	 * When we see checksum errors (in pg_stat_database)
	 * 
	 * @param cm
	 * @param dbname
	 * @param checksumFailuresDiff
	 * @param checksumLastFailure
	 * @param checksumFailuresTotal
	 * @param threshold 
	 */
	public AlarmEventPgChecksumFailure(CountersModel cm, String dbname, int checksumFailuresDiff, String checksumLastFailure, int checksumFailuresTotal, int threshold)
	{
		super(
				Version.getAppName(), // serviceType
				cm.getServerName(),   // serviceName
				cm.getName(),         // serviceInfo
				null,                 // extraInfo
				AlarmEvent.Category.SPACE,
				AlarmEvent.Severity.INFO, 
				AlarmEvent.ServiceState.UP, 
				"Data Checksum failure DATA-CURRUPTION in Server '" + cm.getServerName() + "', dbname='" + dbname + "', checksumFailuresDiff=" + checksumFailuresDiff + ", lastFailure='" + checksumLastFailure+ "', total=" + checksumFailuresTotal + " (threshold=" + threshold + ")",
				threshold
				);

		// Set: Time To Live if postpone is enabled
		setTimeToLive(cm);

		// Set the raw data carrier
		setData("dbname='" + dbname + "', checksumFailuresDiff=" + checksumFailuresDiff + ", total=" + checksumFailuresTotal);
	}

}
