package com.asetune.alarm.events;

import com.asetune.Version;
import com.asetune.cm.CountersModel;
import com.asetune.utils.Configuration;

public class AlarmEventErrorLogEntry
extends AlarmEvent 
{
	private static final long serialVersionUID = 1L;

	public static final String  PROPKEY_alarm_timeToLiveInSeconds = "AlarmEventErrorLogEntry.timeToLive.seconds";
	public static final int     DEFAULT_alarm_timeToLiveInSeconds = 1800; // 30 minutes

	/**
	 * 
	 * @param cm
	 * @param alarmSeverity
	 * @param errorNumber
	 * @param errorSeverity
	 * @param errorMessage
	 * @param threshold
	 */
	public AlarmEventErrorLogEntry(CountersModel cm, Severity alarmSeverity, int errorNumber, int errorSeverity, String errorMessage, int severityThreshold)
	{
		super(
				Version.getAppName(), // serviceType
				cm.getServerName(),   // serviceName
				cm.getName(),         // serviceInfo
				errorNumber,          // extraInfo
				AlarmEvent.Category.OTHER,
				alarmSeverity, 
				AlarmEvent.ServiceState.UP, 
				"done later in constructor...",
				severityThreshold
				);

		// Set: FIXED Time To Live
		// Errorlog entries are a "one time shot" (it's only seen once)... so how long should that entry be "active"
		int ttlSec = Configuration.getCombinedConfiguration().getIntProperty(PROPKEY_alarm_timeToLiveInSeconds, DEFAULT_alarm_timeToLiveInSeconds);
		setTimeToLive( ttlSec * 1000 );

		// Set the description 
		setDescription("Msg="+errorNumber+" Severity="+alarmSeverity+": " + errorMessage.trim()+ ". ["+PROPKEY_alarm_timeToLiveInSeconds+"="+ttlSec+", severityThreshold="+severityThreshold+"]");
	}
}
