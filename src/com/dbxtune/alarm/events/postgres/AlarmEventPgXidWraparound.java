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
 * A database's oldest unfrozen transaction id (or multixact id) is getting old.
 * At about 2.1 billion Postgres stops accepting writes (to protect the data); an anti-wraparound vacuum must freeze the rows before that.
 */
public class AlarmEventPgXidWraparound
extends AlarmEvent
{
	private static final long serialVersionUID = 1L;

	/**
	 * @param cm
	 * @param dbname
	 * @param what       "transaction id" or "multixact id"
	 * @param age        age(datfrozenxid) or mxid_age(datminmxid)
	 * @param threshold
	 */
	public AlarmEventPgXidWraparound(CountersModel cm, String dbname, String what, long age, long threshold)
	{
		super(
				Version.getAppName(), // serviceType
				cm.getServerName(),   // serviceName
				cm.getName(),         // serviceInfo
				dbname + ":" + what,  // extraInfo
				AlarmEvent.Category.SPACE,
				age > 1_600_000_000L ? AlarmEvent.Severity.ERROR : AlarmEvent.Severity.WARNING,
				AlarmEvent.ServiceState.UP,
				"The oldest " + what + " in database '" + dbname + "' at Server '" + cm.getServerName() + "' is " + String.format("%,d", age)
				  + " old (" + Math.round(age * 100.0 / 2_147_483_647L) + "% of the ~2.1 billion limit where Postgres stops accepting writes). "
				  + "An anti-wraparound vacuum is not getting done: look for long running transactions, replication slots, or a vacuum that can't finish. (threshold=" + String.format("%,d", threshold) + ")",
				threshold
				);

		setTimeToLive(cm);
		setData("dbname=" + dbname + ", what=" + what + ", age=" + age);
	}
}
