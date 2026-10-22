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
package com.dbxtune.alarm.events.oracle;

import com.dbxtune.Version;
import com.dbxtune.alarm.events.AlarmEvent;
import com.dbxtune.cm.CountersModel;

public class AlarmEventOraArchiveDestError
extends AlarmEvent
{
	private static final long serialVersionUID = 1L;

	/**
	 * Alarm when an archive log destination is not working (STATUS is for example: ERROR, DISABLED, FULL).<br>
	 * If the destination is MANDATORY the online redo logs can't be reused, and the database "hangs" when all of them are full: so that is an ERROR, otherwise it's a WARNING
	 *
	 * @param cm
	 * @param destName       Name of the destination, like: LOG_ARCHIVE_DEST_2
	 * @param status         STATUS from V$ARCHIVE_DEST
	 * @param binding        MANDATORY or OPTIONAL
	 * @param target         PRIMARY (a local directory) or STANDBY (a remote database)
	 * @param destination    Directory or service name
	 * @param error          The error message from the last failure (can be null)
	 * @param failDate       When the last failure happened (can be null)
	 * @param failureCount   Number of failures in a row
	 * @param statusRegex    The regular expression that STATUS was matched against
	 */
	public AlarmEventOraArchiveDestError(CountersModel cm, String destName, String status, String binding, String target, String destination, String error, String failDate, int failureCount, String statusRegex)
	{
		super(
				Version.getAppName(), // serviceType
				cm.getServerName(),   // serviceName
				cm.getName(),         // serviceInfo
				destName,             // extraInfo
				AlarmEvent.Category.RPO,
				"MANDATORY".equalsIgnoreCase(binding) ? AlarmEvent.Severity.ERROR : AlarmEvent.Severity.WARNING,
				AlarmEvent.ServiceState.AFFECTED,
				"Archive destination '" + destName + "' has STATUS='" + status + "' in Server '" + cm.getServerName() + "'. Binding=" + binding + ", Target=" + target + ", Destination='" + destination + "', FailDate='" + failDate + "', FailureCount=" + failureCount + ", Error='" + (error == null ? "" : error.trim()) + "'. (statusRegex=" + statusRegex + ")",
				null
				);

		// Set: Time To Live if postpone is enabled
		setTimeToLive(cm);

		// Set the raw data carrier
		setData("destName=" + destName + ",status=" + status + ",failureCount=" + failureCount);
	}
}
