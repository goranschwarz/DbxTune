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
package com.dbxtune.alarm.events.postgres;

import com.dbxtune.Version;
import com.dbxtune.alarm.events.AlarmEvent;
import com.dbxtune.cm.CountersModel;

/**
 * A logical replication subscription got errors: applying changes (the replication stops and retries), or the initial table/sequence synchronization
 */
public class AlarmEventPgSubscriptionError
extends AlarmEvent
{
	private static final long serialVersionUID = 1L;

	/**
	 * @param cm
	 * @param dbname       the database of the subscription
	 * @param subname      the subscription name
	 * @param applyErrors  new apply errors in the last sample
	 * @param syncErrors   new table/sequence synchronization errors in the last sample
	 * @param threshold
	 */
	public AlarmEventPgSubscriptionError(CountersModel cm, String dbname, String subname, long applyErrors, long syncErrors, long threshold)
	{
		super(
				Version.getAppName(),            // serviceType
				cm.getServerName(),              // serviceName
				cm.getName(),                    // serviceInfo
				dbname + "." + subname,          // extraInfo
				AlarmEvent.Category.HADR,
				applyErrors > 0 ? AlarmEvent.Severity.ERROR : AlarmEvent.Severity.WARNING,
				AlarmEvent.ServiceState.AFFECTED,
				"Logical replication subscription '" + subname + "' in database '" + dbname + "' at Server '" + cm.getServerName() + "' got errors: "
				  + applyErrors + " apply errors and " + syncErrors + " table/sequence synchronization errors in the last sample. "
				  + (applyErrors > 0 ? "The changes from the publisher are NOT applied: the apply worker stops at the failing change and retries it (often a duplicate key or a missing table/column on the subscriber). " : "")
				  + "The error text is in the subscriber's error log. With disable_on_error the subscription is now disabled (ALTER SUBSCRIPTION ... ENABLE after the fix). "
				  + "(threshold=" + threshold + ")",
				threshold
				);

		setTimeToLive(cm);
		setData("dbname=" + dbname + ", subname=" + subname + ", applyErrors=" + applyErrors + ", syncErrors=" + syncErrors);
	}
}
