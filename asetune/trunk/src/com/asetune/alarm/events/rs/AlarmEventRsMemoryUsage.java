package com.asetune.alarm.events.rs;

import com.asetune.Version;
import com.asetune.alarm.events.AlarmEvent;
import com.asetune.cm.CountersModel;

public class AlarmEventRsMemoryUsage
extends AlarmEvent 
{
	private static final long serialVersionUID = 1L;

	/**
	 * Alarm when a RepServer Memory Usage goes above any given threshold
	 * 
	 * @param cm
	 * @param threshold
	 * @param usedSpaceInMb
	 * @param freeSpaceInMb
	 * @param usedPct
	 */
	public AlarmEventRsMemoryUsage(CountersModel cm, int threshold, int thresholdLevel, int usedMemoryInMb, int freeMemoryInMb, double usedPct, int memoryLimit)
	{
		super(
				Version.getAppName(), // serviceType
				cm.getServerName(),   // serviceName
				cm.getName(),         // serviceInfo
				"THRESHOLD-"+thresholdLevel,       // extraInfo
				AlarmEvent.Category.SRV_CONFIG,
				thresholdLevel < 3 ? AlarmEvent.Severity.WARNING : AlarmEvent.Severity.ERROR, 
				thresholdLevel < 3 ? AlarmEvent.ServiceState.UP  : AlarmEvent.ServiceState.AFFECTED,
				"Replication Server Memory Usage in Server '" + cm.getServerName() + "' is starting to get high, type='THRESHOLD-"+thresholdLevel+"', usedMemoryInMb="+usedMemoryInMb+", freeMemoryInMb="+freeMemoryInMb+", usedPct="+usedPct+", memoryLimit="+memoryLimit+". (threshold="+threshold+")",
				threshold
				);

		// Set: Time To Live if postpone is enabled
		setTimeToLive(cm);

		// Set the raw data carier
		setData("usedMemoryInMb="+usedMemoryInMb+", freeMemoryInMb="+freeMemoryInMb+", usedPct="+usedPct+", memoryLimit="+memoryLimit);
	}
}
