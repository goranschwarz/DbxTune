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
 * A replication slot is not healthy: inactive for a long time (it keeps WAL, which can fill the disk), or its WAL is about to be (or was) removed
 */
public class AlarmEventPgReplicationSlot
extends AlarmEvent
{
	private static final long serialVersionUID = 1L;

	/**
	 * @param cm
	 * @param alarmName  "SlotWalStatus" or "SlotInactive" (part of the identity: both can be active for the same slot)
	 * @param slotName
	 * @param problem    the text: what is wrong
	 * @param severe     true = ERROR (the slot is lost)
	 * @param threshold
	 */
	public AlarmEventPgReplicationSlot(CountersModel cm, String alarmName, String slotName, String problem, boolean severe, Number threshold)
	{
		super(
				Version.getAppName(),            // serviceType
				cm.getServerName(),              // serviceName
				cm.getName(),                    // serviceInfo
				slotName + ":" + alarmName,      // extraInfo
				AlarmEvent.Category.HADR,
				severe ? AlarmEvent.Severity.ERROR : AlarmEvent.Severity.WARNING,
				AlarmEvent.ServiceState.UP,
				"Replication slot '" + slotName + "' at Server '" + cm.getServerName() + "': " + problem,
				threshold
				);

		setTimeToLive(cm);
		setData("slot=" + slotName + ", alarm=" + alarmName + ", problem=" + problem);
	}
}
