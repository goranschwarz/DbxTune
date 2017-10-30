package com.asetune.alarm.events.internal;

import com.asetune.alarm.events.AlarmEvent;

public class AlarmEvent_Stop
extends AlarmEvent 
{
	private static final long serialVersionUID = 1L;

	/**
	 * Putting this at the Alarm Queue will do the same as calling method endOfScan()
	 */
	public AlarmEvent_Stop()
	{
		super(
				"AlarmHandler", // dbmsVendor, 
				"Stop",    // serviceName, 
				"Stop",    // serviceInfo,
				null,
				AlarmEvent.Severity.UNKNOWN, 
				AlarmEvent.ServiceState.UNKNOWN, 
				"Stop"     // description
			); 
	}
}
