package com.asetune.alarm.events;

import com.asetune.Version;
import com.asetune.cm.CountersModel;

public class AlarmEventLastBackupFailed
extends AlarmEvent 
{
	private static final long serialVersionUID = 1L;

	/**
	 * Alarm for full transaction log for a specific database.
	 * @param cm
	 * @param dbname
	 * @param threshold 
	 */
	public AlarmEventLastBackupFailed(CountersModel cm, String dbname, int threshold)
	{
		super(
				Version.getAppName(), // serviceType
				cm.getServerName(),   // serviceName
				cm.getName(),         // serviceInfo
				dbname,               // extraInfo
				AlarmEvent.Category.OTHER,
				AlarmEvent.Severity.WARNING, 
				AlarmEvent.ServiceState.UP, 
				"Last Backup FAILED in Server '" + cm.getServerName() + "' and dbname '" + dbname + "'. (threshold="+threshold+")",
				threshold
				);

		// Set: Time To Live if postpone is enabled
		setTimeToLive(cm);

		// Set the raw data carier: current cpu usage
		setData(dbname);
	}
}
