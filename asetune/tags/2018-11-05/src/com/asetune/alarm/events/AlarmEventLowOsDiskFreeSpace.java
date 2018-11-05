package com.asetune.alarm.events;

import com.asetune.Version;
import com.asetune.cm.CountersModel;

public class AlarmEventLowOsDiskFreeSpace
extends AlarmEvent 
{
	private static final long serialVersionUID = 1L;

	/**
	 * Alarm when Operating System Disk space (using df -k) is starting to get full. Threshold in MB
	 * 
	 * @param cm
	 * @param mountPoint
	 * @param freeSpaceInMb
	 * @param usedSpaceInPct
	 * @param thresholdInMb
	 */
	public AlarmEventLowOsDiskFreeSpace(CountersModel cm, String mountPoint, int freeSpaceInMb, double usedSpaceInPct, int thresholdInMb)
	{
		super(
				Version.getAppName(), // serviceType
				cm.getServerName(),   // serviceName
				cm.getName(),         // serviceInfo
				mountPoint,           // extraInfo
				AlarmEvent.Category.SPACE,
				AlarmEvent.Severity.WARNING, 
				AlarmEvent.ServiceState.UP, 
				"Low Operating System Disk Free Space in Server '" + cm.getServerName() + "' at MountPoint '" + mountPoint + "', FreeSpaceInMB="+freeSpaceInMb+", UsedSpaceInPcs="+usedSpaceInPct+". (thresholdInMb="+thresholdInMb+")",
				thresholdInMb
				);

		// Set: Time To Live if postpone is enabled
		setTimeToLive(cm);

		// Set the raw data carier
		setData("mountPoint="+mountPoint+",mb="+freeSpaceInMb+",pct="+usedSpaceInPct);
	}

	/**
	 * Alarm when Operating System Disk space (using df -k) is starting to get full. Threshold in Percent
	 * 
	 * @param cm
	 * @param mountPoint
	 * @param freeSpaceInMb
	 * @param usedSpaceInPct
	 * @param thresholdInPct
	 */
	public AlarmEventLowOsDiskFreeSpace(CountersModel cm, String mountPoint, int freeSpaceInMb, double usedSpaceInPct, double thresholdInPct)
	{
		super(
				Version.getAppName(), // serviceType
				cm.getServerName(),   // serviceName
				cm.getName(),         // serviceInfo
				mountPoint,           // extraInfo
				AlarmEvent.Category.SPACE,
				AlarmEvent.Severity.WARNING, 
				AlarmEvent.ServiceState.UP, 
				"Low Operating System Disk Free Space in Server '" + cm.getServerName() + "' at MountPoint '" + mountPoint + "', FreeSpaceInMB="+freeSpaceInMb+", UsedSpaceInPcs="+usedSpaceInPct+". (thresholdInPct="+thresholdInPct+")",
				thresholdInPct
				);

		// Set: Time To Live if postpone is enabled
		setTimeToLive(cm);

		// Set the raw data carier
		setData("mountPoint="+mountPoint+",mb="+freeSpaceInMb+",pct="+usedSpaceInPct);
	}
}
