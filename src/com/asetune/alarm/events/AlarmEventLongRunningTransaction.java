package com.asetune.alarm.events;

import org.apache.commons.lang3.StringUtils;

import com.asetune.Version;
import com.asetune.cm.CountersModel;

public class AlarmEventLongRunningTransaction
extends AlarmEvent 
{
	private static final long serialVersionUID = 1L;

	public AlarmEventLongRunningTransaction(CountersModel cm, Number val)
	{
		super(
				Version.getAppName(), // serviceType
				cm.getServerName(),   // serviceName
				cm.getName(),         // serviceInfo
				null,                 // extraInfo
				AlarmEvent.Severity.WARNING, 
				AlarmEvent.ServiceState.UP, 
				"Found Long running transaction in '" + cm.getServerName() + "'. Seconds=" + val + ".");

		// Set: Time To Live if postpone is enabled
		setTimeToLive(cm);

		// Set the raw data carier: current cpu usage
		setData(val);
	}

	public AlarmEventLongRunningTransaction(CountersModel cm, String dbname, Number oldestTranInSeconds, String oldestTranName)
	{
		super(
				Version.getAppName(), // serviceType
				cm.getServerName(),   // serviceName
				cm.getName(),         // serviceInfo
				dbname,               // extraInfo
				AlarmEvent.Severity.WARNING, 
				AlarmEvent.ServiceState.UP, 
				"Found Long running transaction in '" + cm.getServerName() + "', dbname='" + dbname +"'. Seconds=" + oldestTranInSeconds + ", TranName='"+StringUtils.trim(oldestTranName)+"'.");

		// Set: Time To Live if postpone is enabled
		setTimeToLive(cm);

		// Set the raw data carier: current cpu usage
		setData(oldestTranInSeconds);
	}
}
