package com.asetune.alarm.events;

import com.asetune.Version;
import com.asetune.cm.CountersModel;

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
	public AlarmEventHighCpuUtilization(CountersModel cm, Number totalCpuUsagePct)
	{
		super(
				Version.getAppName(), // serviceType
				cm.getServerName(),   // serviceName
				cm.getName(),         // serviceInfo
				null,                 // extraInfo
				AlarmEvent.Severity.WARNING, 
				AlarmEvent.ServiceState.UP, 
				"High CPU Utilization in '" + cm.getServerName() + "'. CPU at " + totalCpuUsagePct + ".");

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
	public AlarmEventHighCpuUtilization(CountersModel cm, CpuType cpuType, Number totalCpuUsagePct, Number userCpuUsagePct, Number systemCpuUsagePct, Number idleCpuUsagePct)
	{
		super(
				Version.getAppName(), // serviceType
				cm.getServerName(),   // serviceName
				cm.getName(),         // serviceInfo
				cpuType+"",           // extraInfo
				AlarmEvent.Severity.WARNING, 
				AlarmEvent.ServiceState.UP, 
				"High " + cpuType + " Utilization in '" + cm.getServerName() + "'. CPU at " + totalCpuUsagePct + " percent (total="+totalCpuUsagePct+"%, user="+userCpuUsagePct+"%, system="+systemCpuUsagePct+"%, idle="+idleCpuUsagePct+"%).");

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
