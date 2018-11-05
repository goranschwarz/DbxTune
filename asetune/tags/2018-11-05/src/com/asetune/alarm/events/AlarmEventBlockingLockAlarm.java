package com.asetune.alarm.events;

import com.asetune.Version;
import com.asetune.cm.CountersModel;

public class AlarmEventBlockingLockAlarm
extends AlarmEvent 
{
	private static final long serialVersionUID = 1L;

	public AlarmEventBlockingLockAlarm(CountersModel cm, Number threshold, Number count)
	{
		super(
				Version.getAppName(), // serviceType
				cm.getServerName(),   // serviceName
				cm.getName(),         // serviceInfo
				null,                 // extraInfo
				AlarmEvent.Category.LOCK,
				AlarmEvent.Severity.WARNING, 
				AlarmEvent.ServiceState.AFFECTED, 
				"Found Blocking locks in '" + cm.getServerName() + "'. Count=" + count + ". (threshold="+threshold+")",
				threshold);

		// Set: Time To Live if postpone is enabled
		setTimeToLive(cm);

		// Set the raw data carier: current cpu usage
		setData(count);
	}
}
