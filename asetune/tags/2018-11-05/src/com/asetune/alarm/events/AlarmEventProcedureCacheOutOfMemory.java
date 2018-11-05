package com.asetune.alarm.events;

import com.asetune.Version;
import com.asetune.cm.CountersModel;

public class AlarmEventProcedureCacheOutOfMemory
extends AlarmEvent 
{
	private static final long serialVersionUID = 1L;

	public AlarmEventProcedureCacheOutOfMemory(CountersModel cm)
	{
		super(
				Version.getAppName(), // serviceType
				cm.getServerName(),   // serviceName
				cm.getName(),         // serviceInfo    // Note: use cm.getName() will cause one event for every CM. If we set this to "", then there will only be one Alarm for the whole server
				null,                 // extraInfo
				AlarmEvent.Category.SRV_CONFIG,
				AlarmEvent.Severity.ERROR,              // ERROR = When there is no more prcedure cache, Normal users cant work. (If this is just temporary the Alarm will go away at next sample)
				AlarmEvent.ServiceState.AFFECTED, 
				"Out Of Stored Procedure Cache Memory in server '" + cm.getServerName() + "'.",
				null);

		// Set: Time To Live if postpone is enabled
		setTimeToLive(cm);
	}
}
