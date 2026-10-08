/*******************************************************************************
 * Copyright (C) 2010-2026 Goran Schwarz
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
package com.dbxtune.alarm.events;

import com.dbxtune.Version;
import com.dbxtune.cm.CountersModel;

/**
 * Windows: The "Commit Charge" (Committed Bytes) is getting close to the "Commit Limit" (RAM + Page Files).<br>
 * When the Commit Limit is reached, memory allocations FAIL, even if 'Available MBytes' looks OK.
 */
public class AlarmEventOsCommitChargeHigh
extends AlarmEvent 
{
	private static final long serialVersionUID = 1L;

	public AlarmEventOsCommitChargeHigh(CountersModel cm, int thresholdPct, int thresholdHeadroomMb, String hostname, String note, double commitPctAvg, double headroomMbAvg, double committedMb, double commitLimitMb)
	{
		super(
				Version.getAppName(), // serviceType
				cm.getServerName(),   // serviceName
				cm.getName(),         // serviceInfo
				null,                 // extraInfo
				AlarmEvent.Category.OTHER,
				AlarmEvent.Severity.WARNING, 
				AlarmEvent.ServiceState.UP, 
				"High Commit Charge on hostname '" + hostname + "' " + note + ". "
					+ "% Committed Bytes In Use=" + String.format("%.1f", commitPctAvg) + "%, Commit Headroom=" + String.format("%.0f", headroomMbAvg) + " MB. "
					+ "Currently: Committed=" + String.format("%.0f", committedMb) + " MB, Commit Limit=" + String.format("%.0f", commitLimitMb) + " MB. "
					+ "When the Commit Limit is reached, memory allocations FAIL (out-of-memory), even if 'Available MBytes' looks OK. "
					+ "Find the process with growing 'Private Bytes', or increase RAM or the Page File. "
					+ "(thresholdPct=" + thresholdPct + ", thresholdHeadroomMb=" + thresholdHeadroomMb + ")",
				thresholdPct);

		// Set: Time To Live if postpone is enabled
		setTimeToLive(cm);

		// Set the raw data
		setData("commitPctAvg=" + commitPctAvg + ", headroomMbAvg=" + headroomMbAvg + ", committedMb=" + committedMb + ", commitLimitMb=" + commitLimitMb);
	}
}
