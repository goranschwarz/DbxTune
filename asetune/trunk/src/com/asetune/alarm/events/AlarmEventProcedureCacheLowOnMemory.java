package com.asetune.alarm.events;

import com.asetune.Version;
import com.asetune.cm.CountersModel;

public class AlarmEventProcedureCacheLowOnMemory
extends AlarmEvent 
{
	private static final long serialVersionUID = 1L;

//	public AlarmEventProcedureCacheLowOnMemory(CountersModel cm, int freeSpaceInMb, double usedSpaceInPct, int thresholdInMb)
	public AlarmEventProcedureCacheLowOnMemory(CountersModel cm, double freeSpaceInMb, double usedSpaceInPct, double thresholdInPct)
	{
		super(
				Version.getAppName(), // serviceType
				cm.getServerName(),   // serviceName
				cm.getName(),         // serviceInfo
				null,                 // extraInfo
				AlarmEvent.Category.SRV_CONFIG,
				AlarmEvent.Severity.WARNING,
				AlarmEvent.ServiceState.UP, 
				"Stored Procedure Cache Memory is getting low in server '" + cm.getServerName() + "'. FreeSpaceInMB="+freeSpaceInMb+", UsedSpaceInPcs="+usedSpaceInPct+". (thresholdInPct="+thresholdInPct+")",
				null);

		setData("FreeSpaceInMB="+freeSpaceInMb+", UsedSpaceInPcs="+usedSpaceInPct);

		// Set: Time To Live if postpone is enabled
		setTimeToLive(cm);
	}
}
