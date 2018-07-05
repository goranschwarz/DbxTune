package com.asetune.alarm.events;

import org.apache.commons.lang3.StringUtils;

import com.asetune.Version;
import com.asetune.cm.CountersModel;

public class AlarmEventLongRunningDetachedTransaction
extends AlarmEvent 
{
	private static final long serialVersionUID = 1L;

	public AlarmEventLongRunningDetachedTransaction(CountersModel cm, Number threshold, String dbname, Number inSeconds, String tranName)
	{
		super(
				Version.getAppName(), // serviceType
				cm.getServerName(),   // serviceName
				cm.getName(),         // serviceInfo
				dbname,               // extraInfo
				AlarmEvent.Category.OTHER,
				AlarmEvent.Severity.WARNING, 
				AlarmEvent.ServiceState.UP, 
				"Found Long running transaction, with state 'Detached' in '" + cm.getServerName() + "', dbname='" + dbname +"'. Seconds=" + inSeconds + ", TranName='"+StringUtils.trim(tranName)+"'. (threshold="+threshold+")",
				threshold);

		// Set: Time To Live if postpone is enabled
		setTimeToLive(cm);

		// Set the raw data carier: current cpu usage
		setData(inSeconds);
	}
}
