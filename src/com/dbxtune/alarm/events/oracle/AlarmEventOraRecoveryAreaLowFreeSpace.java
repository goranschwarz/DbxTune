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

public class AlarmEventOraRecoveryAreaLowFreeSpace
extends AlarmEvent
{
	private static final long serialVersionUID = 1L;

	/**
	 * Alarm when the Fast Recovery Area (FRA) is starting to get full.<br>
	 * When it is full, the archiver can't write archived logs, and the database "hangs" (ORA-19809, ORA-00257)
	 *
	 * @param cm
	 * @param destName          Name (directory or ASM disk group) of the recovery area
	 * @param netFreeMb         Free space in MB, including the space that can be reclaimed
	 * @param netUsedPct        Used space in Percent, that can NOT be reclaimed (this is what the threshold is compared to)
	 * @param usedPct           Used space in Percent (including space that can be reclaimed)
	 * @param reclaimablePct    Space in Percent that Oracle can reclaim (delete obsolete files) when it needs space
	 * @param thresholdInPct
	 */
	public AlarmEventOraRecoveryAreaLowFreeSpace(CountersModel cm, String destName, int netFreeMb, double netUsedPct, double usedPct, double reclaimablePct, double thresholdInPct)
	{
		super(
				Version.getAppName(), // serviceType
				cm.getServerName(),   // serviceName
				cm.getName(),         // serviceInfo
				destName,             // extraInfo
				AlarmEvent.Category.SPACE,
				AlarmEvent.Severity.WARNING,
				AlarmEvent.ServiceState.UP,
				"Low RECOVERY AREA Free Space in Server '" + cm.getServerName() + "' and destination '" + destName + "', NetUsedSpaceInPct=" + netUsedPct + " (UsedSpaceInPct=" + usedPct + " - ReclaimableInPct=" + reclaimablePct + "), NetFreeSpaceInMB=" + netFreeMb + ". When it is full the archiver stops, and the database hangs. (thresholdInPct=" + thresholdInPct + ")",
				thresholdInPct
				);

		// Set: Time To Live if postpone is enabled
		setTimeToLive(cm);

		// Set the raw data carrier
		setData("destName=" + destName + ",netFreeMb=" + netFreeMb + ",netUsedPct=" + netUsedPct + ",usedPct=" + usedPct + ",reclaimablePct=" + reclaimablePct);
	}
}
