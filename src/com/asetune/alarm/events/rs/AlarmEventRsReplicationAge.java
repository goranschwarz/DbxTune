package com.asetune.alarm.events.rs;

import com.asetune.Version;
import com.asetune.alarm.events.AlarmEvent;
import com.asetune.cm.CountersModel;

public class AlarmEventRsReplicationAge
extends AlarmEvent 
{
	private static final long serialVersionUID = 1L;

	/**
	 * When any age has expired
	 * 
	 * @param cm
	 * @param threshold
	 * @param logicalName
	 * @param colName
	 */
	public AlarmEventRsReplicationAge(CountersModel cm, int threshold, String logicalName, String colName, int age)
	{
		super(
				Version.getAppName(), // serviceType
				logicalName,          // serviceName
				cm.getName(),         // serviceInfo
				colName,              // extraInfo
				AlarmEvent.Category.OTHER,
				AlarmEvent.Severity.WARNING, 
				AlarmEvent.ServiceState.AFFECTED, 
				"Replication Age in Server '" + cm.getServerName() + "' for Logical Connection '" + logicalName + "', colName='"+colName+"', age="+age+". (threshold="+threshold+")",
				threshold
				);

		// Set: Time To Live if postpone is enabled
		setTimeToLive(cm);

		// Set the raw data carier
		setData(age);
	}

}
