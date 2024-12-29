/*******************************************************************************
 * Copyright (C) 2010-2025 Goran Schwarz
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

public class AlarmEventHighCpuUtilization
extends AlarmEvent 
{
	private static final long serialVersionUID = 1L;

	public enum CpuType
	{
		TOTAL_CPU,  // ALL Types of CPU cyckles (usually: USER+SYSTEM(+IO))
		USER_CPU,   // User   cyckles spent on CPU
		SYSTEM_CPU, // System cyckles spent on CPU (system calls and usually IO's)
		IO_CPU,     // IO     cyckles spent on CPU (this is probably the same as SystemCPU...)
		UNKNOWN     // 
	};

	/**
	 * Alarm on CPU Utilization
	 * 
	 * @param cm                    CounterModel which this happened on
	 * @param totalCpuUsagePct      Total CPU Usage Percent (user + system [+ IO])
	 */
	public AlarmEventHighCpuUtilization(CountersModel cm, Number threshold, Number totalCpuUsagePct)
	{
		super(
				Version.getAppName(), // serviceType
				cm.getServerName(),   // serviceName
				cm.getName(),         // serviceInfo
				null,                 // extraInfo
				AlarmEvent.Category.CPU,
				AlarmEvent.Severity.WARNING, 
				AlarmEvent.ServiceState.UP, 
				"High CPU Utilization in '" + cm.getServerName() + "'. CPU at " + totalCpuUsagePct + ". (threshold="+threshold+")",
				threshold);

		// Set: Time To Live if postpone is enabled
		setTimeToLive(cm);

		// Set the raw data carier: current cpu usage
		setData(totalCpuUsagePct);
	}

	/**
	 * Alarm on CPU Utilization
	 * 
	 * @param cm                    CounterModel which this happened on
	 * @param cpuType               What type of CPU does this alarm describe
	 * @param totalCpuUsagePct      Total CPU Usage Percent (user + system [+ IO])
	 * @param userCpuUsagePct       User CPU Percent
	 * @param systemCpuUsagePct     System CPU Percent
	 * @param idleCpuUsagePct       Idle CPU Percent
	 */
	public AlarmEventHighCpuUtilization(CountersModel cm, Number threshold, CpuType cpuType, Number totalCpuUsagePct, Number userCpuUsagePct, Number systemCpuUsagePct, Number idleCpuUsagePct)
	{
		super(
				Version.getAppName(), // serviceType
				cm.getServerName(),   // serviceName
				cm.getName(),         // serviceInfo
				cpuType+"",           // extraInfo
				AlarmEvent.Category.CPU,
				AlarmEvent.Severity.WARNING, 
				AlarmEvent.ServiceState.UP, 
				"High " + cpuType + " Utilization in '" + cm.getServerName() + "'. CPU at " + totalCpuUsagePct + " percent (total="+totalCpuUsagePct+"%, user="+userCpuUsagePct+"%, system="+systemCpuUsagePct+"%, idle="+idleCpuUsagePct+"%). (threshold="+threshold+")",
				threshold);

		// Set: Time To Live if postpone is enabled
		setTimeToLive(cm);

		// Set the raw data carier: current cpu usage
		Number cpuUsagePct;
		if      ( CpuType.TOTAL_CPU .equals(cpuType) ) cpuUsagePct = totalCpuUsagePct;
		else if ( CpuType.USER_CPU  .equals(cpuType) ) cpuUsagePct = userCpuUsagePct;
		else if ( CpuType.SYSTEM_CPU.equals(cpuType) ) cpuUsagePct = systemCpuUsagePct;
		else if ( CpuType.IO_CPU    .equals(cpuType) ) cpuUsagePct = systemCpuUsagePct; // = ioCpuUsagePct;
		else                                           cpuUsagePct = -1;
		
		setData(cpuUsagePct);
	}
}
