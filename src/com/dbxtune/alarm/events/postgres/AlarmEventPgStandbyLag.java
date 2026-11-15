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
 * Seen from a standby: it is behind in applying (replaying) the WAL it received from the primary
 */
public class AlarmEventPgStandbyLag
extends AlarmEvent
{
	private static final long serialVersionUID = 1L;

	public AlarmEventPgStandbyLag(CountersModel cm, String senderHost, long replayDelaySec, long behindKb, long thresholdSec)
	{
		super(
				Version.getAppName(), // serviceType
				cm.getServerName(),   // serviceName
				cm.getName(),         // serviceInfo
				senderHost,           // extraInfo
				AlarmEvent.Category.HADR,
				AlarmEvent.Severity.WARNING,
				AlarmEvent.ServiceState.UP,
				"Standby '" + cm.getServerName() + "' is " + replayDelaySec + " seconds behind in applying the WAL from '" + senderHost + "' (" + String.format("%,d", behindKb) + " KB received but not yet applied). "
				  + "Queries on the standby see old data, and a failover would take longer. (thresholdInSec=" + thresholdSec + ")",
				thresholdSec
				);

		setTimeToLive(cm);
		setData("senderHost=" + senderHost + ", replayDelaySec=" + replayDelaySec + ", behindKb=" + behindKb);
	}
}
