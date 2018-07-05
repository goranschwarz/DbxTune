package com.asetune.alarm.events;

import com.asetune.Version;
import com.asetune.cm.CountersModel;
import com.asetune.utils.StringUtil;

public class AlarmEventSrvDown
extends AlarmEvent 
{
	private static final long serialVersionUID = 1L;

	public AlarmEventSrvDown(CountersModel cm)
	{
		super(
				Version.getAppName(), // serviceType
				cm.getServerName(),   // serviceName
				cm.getName(),         // serviceInfo
				null,                 // extraInfo
				AlarmEvent.Category.DOWN, 
				AlarmEvent.Severity.ERROR, 
				AlarmEvent.ServiceState.DOWN, 
				"Server is DOWN. Name='" + cm.getServerName() + "'.",
				null);

		// Set: Time To Live if postpone is enabled
		setTimeToLive(cm);
	}

	public AlarmEventSrvDown(String serverName, String url)
	{
		super(
				Version.getAppName(), // serviceType
				serverName,           // serviceName
				url,                  // serviceInfo
				null,                 // extraInfo
				AlarmEvent.Category.DOWN, 
				AlarmEvent.Severity.ERROR, 
				AlarmEvent.ServiceState.DOWN, 
				"Server is DOWN. Name='" + serverName + "', url='"+url+"'.",
				null);
		
		setData( StringUtil.isNullOrBlank(serverName) ? url : serverName );
	}
}
