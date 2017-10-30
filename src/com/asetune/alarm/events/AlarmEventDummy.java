package com.asetune.alarm.events;

import com.asetune.Version;

public class AlarmEventDummy 
extends AlarmEvent
{
	private static final long serialVersionUID = 1L;

	public AlarmEventDummy(String serviceName, String serviceInfo, Object extraInfo, Severity severity, ServiceState state, int timeToLive, Object data, String description, String extendedDesc)
	{
		super(
				Version.getAppName(), // serviceType
				serviceName,   // serviceName
				serviceInfo,         // serviceInfo
				extraInfo,                 // extraInfo
				severity, 
				state, 
				description);

		setExtendedDescription(extendedDesc);

		// Set: Time To Live if postpone is enabled
		setTimeToLive(timeToLive);

		// Set the raw data carier: current cpu usage
		setData(data);
	}

}
