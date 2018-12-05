package com.asetune.alarm.events;

import com.asetune.Version;
import com.asetune.cm.CountersModel;

public class AlarmEventOldTranLogBackup
extends AlarmEvent 
{
	private static final long serialVersionUID = 1L;

	/**
	 * Alarm for full transaction log for a specific database.
	 * @param cm
	 * @param dbname
	 */
	public AlarmEventOldTranLogBackup(CountersModel cm, Number threshold, String dbname, int age)
	{
		super(
				Version.getAppName(), // serviceType
				cm.getServerName(),   // serviceName
				cm.getName(),         // serviceInfo
				dbname,               // extraInfo
				AlarmEvent.Category.OTHER,
				AlarmEvent.Severity.WARNING, 
				AlarmEvent.ServiceState.UP, 
				"Old or No Transaction Log Backup found in '" + cm.getServerName() + "', dbname='" + dbname + "', Backup Age in Hours '" + age + "'" 
						+ (age == -1 ? ", (where -1 means: Since ASE was started)" : "") 
						+ ". (threshold="+threshold+")",
				threshold);

		// Set: Time To Live if postpone is enabled
		setTimeToLive(cm);

		// Set the raw data carier: current cpu usage
		setData(dbname);
	}
}
