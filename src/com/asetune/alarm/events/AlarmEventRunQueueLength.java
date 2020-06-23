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

import com.asetune.Version;
import com.asetune.cm.CountersModel;

public class AlarmEventRunQueueLength
extends AlarmEvent 
{
	private static final long serialVersionUID = 1L;

	public enum RangeType
	{
		RANGE_1_MINUTE, RANGE_5_MINUTE, RANGE_15_MINUTE
	};
	
	public AlarmEventRunQueueLength(CountersModel cm, Number threshold, RangeType rangeType, Number avg_1min, Number avg_5min, Number avg_15min)
	{
		super(
				Version.getAppName(), // serviceType
				cm.getServerName(),   // serviceName
				cm.getName(),         // serviceInfo
				rangeType+"",         // extraInfo
				AlarmEvent.Category.CPU,
				AlarmEvent.Severity.WARNING, 
				AlarmEvent.ServiceState.UP, 
				"Exhausting CPU Scheduling resources in '" + cm.getServerName() + "'. RunQueueLength: avg_1min=" + avg_1min + ", avg_5min="+avg_5min+", avg_15min="+avg_15min+". (threshold="+threshold+")",
				threshold);

		// Set: Time To Live if postpone is enabled
		setTimeToLive(cm);

		// Set the raw data carier: run queue length average 1 minute
		// Set the raw data carier: run queue length average 1 minute
		if      (RangeType.RANGE_1_MINUTE .equals(rangeType)) setData(avg_1min);
		else if (RangeType.RANGE_5_MINUTE .equals(rangeType)) setData(avg_5min);
		else if (RangeType.RANGE_15_MINUTE.equals(rangeType)) setData(avg_15min);
		else                                                  setData(-1);
	}
}
