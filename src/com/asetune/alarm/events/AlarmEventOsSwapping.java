/*******************************************************************************
 * Copyright (C) 2010-2020 Goran Schwarz
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
package com.asetune.alarm.events;

import java.sql.Timestamp;

import com.asetune.Version;
import com.asetune.cm.CountersModel;

public class AlarmEventOsSwapping
extends AlarmEvent 
{
	private static final long serialVersionUID = 1L;

	public AlarmEventOsSwapping(CountersModel cm, int threshold, String hostname, String note, int swapIn_avg, int swapOut_avg)
	{
		super(
				Version.getAppName(), // serviceType
				cm.getServerName(),   // serviceName
				cm.getName(),         // serviceInfo
				null,                 // extraInfo
				AlarmEvent.Category.OTHER,
				AlarmEvent.Severity.WARNING, 
				AlarmEvent.ServiceState.UP, 
				"Extensive Usage of OS Swapping on hostname '" + hostname + "' " + note + ". Time to increase memory or move some processes. swapInAvg=" + swapIn_avg + ", swapOutAvg=" + swapOut_avg + ". (threshold="+threshold+")",
				threshold);

		// Set: Time To Live if postpone is enabled
		setTimeToLive(cm);

		// Set the raw data
		setData("swapInAvg=" + swapIn_avg + ", swapOutAvg=" + swapOut_avg);
	}

	public AlarmEventOsSwapping(CountersModel cm, int threshold, String hostname, String note, int swapIn_avg, Timestamp swapIn_peakTimestamp, double swapIn_peakVal, int swapOut_avg, Timestamp swapOut_peakTimestamp, double swapOut_peakVal)
	{
		super(
				Version.getAppName(), // serviceType
				cm.getServerName(),   // serviceName
				cm.getName(),         // serviceInfo
				null,                 // extraInfo
				AlarmEvent.Category.OTHER,
				AlarmEvent.Severity.WARNING, 
				AlarmEvent.ServiceState.UP, 
				"Extensive Usage of OS Swapping on hostname '" + hostname + "' " + note + ". Time to increase memory or move some processes. swapIn=[avgVal=" + swapIn_avg + ", PeakTs='" + swapIn_peakTimestamp + "', peakVal=" + swapIn_peakVal + "], swapOut=[avgVal=" + swapIn_avg + ", peakTs='" + swapOut_peakTimestamp + "', peakVal=" + swapOut_peakVal + "]. (threshold="+threshold+")",
				threshold);

		// Set: Time To Live if postpone is enabled
		setTimeToLive(cm);

		// Set the raw data
		setData("swapInAvg=" + swapIn_avg + ", swapOutAvg=" + swapOut_avg);
	}
}
