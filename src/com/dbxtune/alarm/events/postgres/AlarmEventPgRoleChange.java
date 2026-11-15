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
 * The server switched between primary and standby (a failover or switchover: pg_is_in_recovery() changed since the previous sample)
 */
public class AlarmEventPgRoleChange
extends AlarmEvent
{
	private static final long serialVersionUID = 1L;

	public AlarmEventPgRoleChange(CountersModel cm, String previousRole, String newRole)
	{
		super(
				Version.getAppName(), // serviceType
				cm.getServerName(),   // serviceName
				cm.getName(),         // serviceInfo
				null,                 // extraInfo
				AlarmEvent.Category.HADR,
				AlarmEvent.Severity.WARNING,
				AlarmEvent.ServiceState.UP,
				"Server '" + cm.getServerName() + "' changed role from " + previousRole + " to " + newRole + " (a failover or switchover). "
				  + "Check that the applications, the standbys and the backups follow the new primary.",
				null
				);

		setTimeToLive(cm);
		setData("previousRole=" + previousRole + ", newRole=" + newRole);
	}
}
