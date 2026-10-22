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
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * DbxTune is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with DbxTune.  If not, see <http://www.gnu.org/licenses/>.
 ******************************************************************************/
package com.dbxtune.alarm.events.mysql;

import com.dbxtune.Version;
import com.dbxtune.alarm.events.AlarmEvent;
import com.dbxtune.cm.CountersModel;

/**
 * A statement has been running for longer than the threshold.
 * <p>
 * MySQL calls a connection a "processlist id" (CONNECTION_ID(), the Id of SHOW PROCESSLIST): the generic alarm says SPID.
 */
public class AlarmEventMyLongRunningStatement
extends AlarmEvent
{
	private static final long serialVersionUID = 1L;

	/**
	 * @param cm
	 * @param thresholdInSec
	 * @param pid             The connection
	 * @param inSeconds       How long the statement has been running
	 * @param user            Its user
	 * @param host            Its client host
	 * @param schema          The current schema of the connection (can be null)
	 * @param digest          The digest of the statement (the same digest as in the tab 'Statements')
	 */
	public AlarmEventMyLongRunningStatement(CountersModel cm, Number thresholdInSec, long pid, Number inSeconds, String user, String host, String schema, String digest)
	{
		super(
				Version.getAppName(), // serviceType
				cm.getServerName(),   // serviceName
				cm.getName(),         // serviceInfo
				"pid=" + pid,         // extraInfo
				AlarmEvent.Category.OTHER,
				AlarmEvent.Severity.INFO,
				AlarmEvent.ServiceState.UP,
				"Found Long running SQL Statement in '" + cm.getServerName() + "'. pid=" + pid + ", Seconds=" + inSeconds + ", user='" + user + "', host='" + host + "', schema='" + schema + "', digest='" + digest + "'. (thresholdInSec=" + thresholdInSec + ")",
				thresholdInSec);

		// Adjust the Alarm Full Duration with X seconds
		setFullDurationAdjustmentInSec( thresholdInSec == null ? 0 : thresholdInSec.intValue() );

		// Set: Time To Live if postpone is enabled
		setTimeToLive(cm);

		// Set the raw data carrier
		setData("pid=" + pid + ",seconds=" + inSeconds + ",digest=" + digest);
	}
}
