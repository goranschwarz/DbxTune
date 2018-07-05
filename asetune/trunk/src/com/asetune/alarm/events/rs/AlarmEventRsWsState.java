package com.asetune.alarm.events.rs;

import com.asetune.Version;
import com.asetune.alarm.events.AlarmEvent;
import com.asetune.cm.CountersModel;

public class AlarmEventRsWsState
extends AlarmEvent 
{
	private static final long serialVersionUID = 1L;

	/**
	 * Alarm when a WarmStandby Connection State is not normal
	 * 
	 * @param cm
	 * @param logicalName
	 * @param colName
	 * @param state
	 */
	public AlarmEventRsWsState(CountersModel cm, String logicalName, String colName, String state, String regexp)
	{
		super(
				Version.getAppName(), // serviceType
				logicalName,          // serviceName
				cm.getName(),         // serviceInfo
				colName,              // extraInfo
				AlarmEvent.Category.DOWN,
				AlarmEvent.Severity.WARNING, 
				AlarmEvent.ServiceState.AFFECTED, 
				"Not a normal State in Server '" + cm.getServerName() + "' for Logical Connection '" + logicalName + "', colName='"+colName+"', state='"+state+"'. (regexp="+regexp+")",
				null
				);

		// Set: Time To Live if postpone is enabled
		setTimeToLive(cm);

		// Set the raw data carier
//		setData("logicalName="+logicalName+",colName="+colName+",state="+state);
		setData(state);
	}

}
