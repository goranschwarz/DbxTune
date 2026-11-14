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
package com.dbxtune.alarm.events.postgres;

import com.dbxtune.Version;
import com.dbxtune.alarm.events.AlarmEvent;
import com.dbxtune.cm.CountersModel;

/**
 * Sessions in a database spend a lot of time 'idle in transaction' (pg_stat_database.idle_in_transaction_time, Postgres 14 and above)
 */
public class AlarmEventPgIdleInTransaction
extends AlarmEvent
{
	private static final long serialVersionUID = 1L;

	public AlarmEventPgIdleInTransaction(CountersModel cm, String dbname, double sessions, double thresholdSessions)
	{
		super(
				Version.getAppName(), // serviceType
				cm.getServerName(),   // serviceName
				cm.getName(),         // serviceInfo
				dbname,               // extraInfo
				AlarmEvent.Category.LOCK,
				AlarmEvent.Severity.WARNING,
				AlarmEvent.ServiceState.UP,
				"Database '" + dbname + "' at Server '" + cm.getServerName() + "': " + String.format("%.0f", sessions) + " sessions are 'idle in transaction' for more than 60 seconds. "
				  + "An open transaction keeps its locks, and stops vacuum from removing dead rows: the application does not commit (or roll back) in time. "
				  + "See 'Active Statements' for the sessions; idle_in_transaction_session_timeout can end them. (thresholdSessions=" + thresholdSessions + ")",
				thresholdSessions
				);

		setTimeToLive(cm);
		setData("dbname=" + dbname + ", sessions=" + sessions);
	}
}
