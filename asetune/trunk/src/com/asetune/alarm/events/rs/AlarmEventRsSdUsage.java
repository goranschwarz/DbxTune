package com.asetune.alarm.events.rs;

import com.asetune.Version;
import com.asetune.alarm.events.AlarmEvent;
import com.asetune.cm.CountersModel;

public class AlarmEventRsSdUsage
extends AlarmEvent 
{
	private static final long serialVersionUID = 1L;

	/**
	 * Alarm when a Stable Device Usage goes above any given threshold
	 * 
	 * @param cm
	 * @param threshold
	 * @param usedSpaceInMb
	 * @param freeSpaceInMb
	 * @param usedPct
	 */
	public AlarmEventRsSdUsage(CountersModel cm, int threshold, String alarmType, int usedSpaceInMb, int freeSpaceInMb, double usedPct)
	{
		super(
				Version.getAppName(), // serviceType
				cm.getServerName(),   // serviceName
				cm.getName(),         // serviceInfo
				alarmType,            // extraInfo
				AlarmEvent.Category.SPACE,
				AlarmEvent.Severity.WARNING, 
				AlarmEvent.ServiceState.UP, 
				"Replication Server Stable device (Queuing system) in Server '" + cm.getServerName() + "' is starting to get full, type='"+alarmType+"', usedSpaceInMb="+usedSpaceInMb+", freeSpaceInMb="+freeSpaceInMb+", usedPct="+usedPct+". (threshold="+threshold+")",
				threshold
				);

		// Set: Time To Live if postpone is enabled
		setTimeToLive(cm);

		// Set the raw data carier
		setData("usedSpaceInMb="+usedSpaceInMb+", freeSpaceInMb="+freeSpaceInMb+", usedPct="+usedPct);
	}
}
