package com.asetune.alarm.events;

import com.asetune.Version;
import com.asetune.cm.CountersModel;

public class AlarmEventBlockingLockAlarm
extends AlarmEvent 
{
	private static final long serialVersionUID = 1L;

	public AlarmEventBlockingLockAlarm(CountersModel cm, Number count)
	{
		super(
				Version.getAppName(), // serviceType
				cm.getServerName(),   // serviceName
				cm.getName(),         // serviceInfo
				null,                 // extraInfo
				AlarmEvent.Severity.WARNING, 
				AlarmEvent.ServiceState.UP, 
				"Found Blocking locks in '" + cm.getServerName() + "'. Count=" + count + ".");

		// Set: Time To Live if postpone is enabled
		setTimeToLive(cm);

		// Set the raw data carier: current cpu usage
		setData(count);
	}
}
