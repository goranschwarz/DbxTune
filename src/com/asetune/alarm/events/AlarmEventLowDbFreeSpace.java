package com.asetune.alarm.events;

import com.asetune.Version;
import com.asetune.cm.CountersModel;

public class AlarmEventLowDbFreeSpace
extends AlarmEvent 
{
	private static final long serialVersionUID = 1L;

	/**
	 * Alarm when database DATA space is starting to get full. Threshold in MB
	 * 
	 * @param cm
	 * @param dbname
	 * @param freeSpaceInMb
	 * @param usedSpaceInPct
	 * @param thresholdInMb
	 */
	public AlarmEventLowDbFreeSpace(CountersModel cm, String dbname, int freeSpaceInMb, double usedSpaceInPct, int thresholdInMb)
	{
		super(
				Version.getAppName(), // serviceType
				cm.getServerName(),   // serviceName
				cm.getName(),         // serviceInfo
				dbname,               // extraInfo
				AlarmEvent.Category.SPACE,
				AlarmEvent.Severity.WARNING, 
				AlarmEvent.ServiceState.UP, 
				"Low DB DATA Free Space (MB) in Server '" + cm.getServerName() + "' and dbname '" + dbname + "', FreeSpaceInMB="+freeSpaceInMb+", UsedSpaceInPcs="+usedSpaceInPct+". (thresholdInMb="+thresholdInMb+")",
				thresholdInMb
				);

		// Set: Time To Live if postpone is enabled
		setTimeToLive(cm);

		// Set the raw data carier
		setData("dbname="+dbname+",freeMb="+freeSpaceInMb+",usedPct="+usedSpaceInPct);
	}

	/**
	 * Alarm when database DATA space is starting to get full. Threshold in Percent
	 * 
	 * @param cm
	 * @param dbname
	 * @param freeSpaceInMb
	 * @param usedSpaceInPct
	 * @param thresholdInPct
	 */
	public AlarmEventLowDbFreeSpace(CountersModel cm, String dbname, int freeSpaceInMb, double usedSpaceInPct, double thresholdInPct)
	{
		super(
				Version.getAppName(), // serviceType
				cm.getServerName(),   // serviceName
				cm.getName(),         // serviceInfo
				dbname,               // extraInfo
				AlarmEvent.Category.SPACE,
				AlarmEvent.Severity.WARNING, 
				AlarmEvent.ServiceState.UP, 
				"Low DB DATA Free Space (PCT) in Server '" + cm.getServerName() + "' and dbname '" + dbname + "', FreeSpaceInMB="+freeSpaceInMb+", UsedSpaceInPcs="+usedSpaceInPct+". (thresholdInPct="+thresholdInPct+")",
				thresholdInPct
				);

		// Set: Time To Live if postpone is enabled
		setTimeToLive(cm);

		// Set the raw data carier
		setData("dbname="+dbname+",freeMb="+freeSpaceInMb+",usedPct="+usedSpaceInPct);
	}
}
