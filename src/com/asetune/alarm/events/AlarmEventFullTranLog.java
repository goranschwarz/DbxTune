package com.asetune.alarm.events;

import com.asetune.Version;
import com.asetune.cm.CountersModel;

public class AlarmEventFullTranLog
extends AlarmEvent 
{
	private static final long serialVersionUID = 1L;

	/**
	 * Alarm for full transaction logs. Just the count of databases with a full tran log.
	 * @param cm
	 * @param val
	 */
	public AlarmEventFullTranLog(CountersModel cm, Number threshold, Number val)
	{
		super(
				Version.getAppName(), // serviceType
				cm.getServerName(),   // serviceName
				cm.getName(),         // serviceInfo
				null,                 // extraInfo
				AlarmEvent.Category.SPACE,
				AlarmEvent.Severity.ERROR, 
				AlarmEvent.ServiceState.AFFECTED, 
				"Full transaction log in some database(s) in '" + cm.getServerName() + "'. Count=" + val + ". (threshold="+threshold+")",
				threshold);

		// Set: Time To Live if postpone is enabled
		setTimeToLive(cm);

		// Set the raw data carier: current cpu usage
		setData(val);
	}

	/**
	 * Alarm for full transaction log for a specific database.
	 * @param cm
	 * @param dbname
	 */
	public AlarmEventFullTranLog(CountersModel cm, Number threshold, String dbname)
	{
		super(
				Version.getAppName(), // serviceType
				cm.getServerName(),   // serviceName
				cm.getName(),         // serviceInfo
				dbname,               // extraInfo
				AlarmEvent.Category.SPACE,
				AlarmEvent.Severity.ERROR, 
				AlarmEvent.ServiceState.AFFECTED, 
				"Full transaction log in '" + cm.getServerName() + "', dbname='" + dbname + "'. (threshold="+threshold+")",
				threshold);

		// Set: Time To Live if postpone is enabled
		setTimeToLive(cm);

		// Set the raw data carier: current cpu usage
		setData(dbname);
	}
}
