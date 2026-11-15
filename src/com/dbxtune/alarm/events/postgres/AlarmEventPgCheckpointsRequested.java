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
 * Many checkpoints are "requested" (started because the WAL since the last checkpoint reached max_wal_size), not "timed" (checkpoint_timeout)
 */
public class AlarmEventPgCheckpointsRequested
extends AlarmEvent
{
	private static final long serialVersionUID = 1L;

	public AlarmEventPgCheckpointsRequested(CountersModel cm, long requested, long timed, int windowMinutes, double thresholdPct)
	{
		super(
				Version.getAppName(), // serviceType
				cm.getServerName(),   // serviceName
				cm.getName(),         // serviceInfo
				null,                 // extraInfo
				AlarmEvent.Category.SRV_CONFIG,
				AlarmEvent.Severity.INFO,
				AlarmEvent.ServiceState.UP,
				"Server '" + cm.getServerName() + "': " + requested + " of " + (requested + timed) + " checkpoints in the last " + windowMinutes + " minutes were requested "
				  + "(started because the WAL reached max_wal_size), not timed. Frequent checkpoints write more (full page images) and cause I/O peaks: "
				  + "max_wal_size is probably too small for the write load. (thresholdPct=" + thresholdPct + ")",
				thresholdPct
				);

		setTimeToLive(cm);
		setData("requested=" + requested + ", timed=" + timed + ", windowMinutes=" + windowMinutes);
	}
}
