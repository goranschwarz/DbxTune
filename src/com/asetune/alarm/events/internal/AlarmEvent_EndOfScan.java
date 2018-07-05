package com.asetune.alarm.events.internal;

import com.asetune.alarm.events.AlarmEvent;

public class AlarmEvent_EndOfScan
extends AlarmEvent 
{
	private static final long serialVersionUID = 1L;

	/**
	 * Putting this at the Alarm Queue will do the same as calling method endOfScan()
	 */
	public AlarmEvent_EndOfScan()
	{
		super(
				"AlarmHandler", // dbmsVendor, 
				"EndOfScan",    // serviceName, 
				"EndOfScan",    // serviceInfo,
				null,
				AlarmEvent.Category.INTERNAL,
				AlarmEvent.Severity.UNKNOWN, 
				AlarmEvent.ServiceState.UNKNOWN, 
				"EndOfScan",    // description
				null
			); 
	}
}
