package com.asetune.alarm.events;

import com.asetune.Version;
import com.asetune.cm.CountersModel;

public class AlarmEventStatementCacheAboveConfig
extends AlarmEvent 
{
	private static final long serialVersionUID = 1L;

	public AlarmEventStatementCacheAboveConfig(CountersModel cm, int configuredSpaceInMb, int usedSpaceInMb, double usedSpaceInPct, double usedPctOfProcMemory, int threshold)
	{
		super(
				Version.getAppName(), // serviceType
				cm.getServerName(),   // serviceName
				cm.getName(),         // serviceInfo
				null,                 // extraInfo
				AlarmEvent.Category.SRV_CONFIG,
				AlarmEvent.Severity.WARNING,
				AlarmEvent.ServiceState.UP, 
				"Statement Cache is above the configured value in server '" + cm.getServerName() + "'. configuredSpaceInMb="+configuredSpaceInMb+", usedSpaceInMb="+usedSpaceInMb+", usedSpaceInPct="+usedSpaceInPct+", usedPercentOfProcedurMemory="+usedPctOfProcMemory+". (threshold="+threshold+")",
				null);

		// Set: Time To Live if postpone is enabled
		setTimeToLive(cm);
	}
}
