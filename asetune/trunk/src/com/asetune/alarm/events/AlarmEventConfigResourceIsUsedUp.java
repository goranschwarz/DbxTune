package com.asetune.alarm.events;

import com.asetune.Version;
import com.asetune.cm.CountersModel;

public class AlarmEventConfigResourceIsUsedUp
extends AlarmEvent 
{
	private static final long serialVersionUID = 1L;

	public AlarmEventConfigResourceIsUsedUp(CountersModel cm, String cfgName, int errorNumber, String errorMessage)
	{
		super(
				Version.getAppName(), // serviceType
				cm.getServerName(),   // serviceName
				cm.getName(),         // serviceInfo    // Note: use cm.getName() will cause one event for every CM. If we set this to "", then there will only be one Alarm for the whole server
				cfgName,              // extraInfo
				AlarmEvent.Category.SRV_CONFIG,
				errorNumber > 0 ? AlarmEvent.Severity.ERROR : AlarmEvent.Severity.WARNING,
				AlarmEvent.ServiceState.AFFECTED, 
				"set later in constructor",
				null);

		String errorNumStr = errorNumber <= 0 ? "" : "Error Number = " + errorNumber + ", ";
		String desc = "Configuration resource '"+cfgName+"' is used up in server '" + cm.getServerName() + "'. " + errorNumStr + "Error Message: " + errorMessage.trim();
		setDescription(desc);
		
		// Set: Time To Live if postpone is enabled
		setTimeToLive(cm);
	}
}
