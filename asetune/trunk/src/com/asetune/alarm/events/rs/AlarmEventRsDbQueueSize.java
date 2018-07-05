package com.asetune.alarm.events.rs;

import com.asetune.Version;
import com.asetune.alarm.events.AlarmEvent;
import com.asetune.cm.CountersModel;

public class AlarmEventRsDbQueueSize
extends AlarmEvent 
{
	private static final long serialVersionUID = 1L;

	/**
	 * Alarm when a Database Queue Size (Inbound or Outbound) goes above any given threshold
	 * 
	 * @param cm
	 * @param threshold
	 * @param name
	 * @param type
	 * @param size
	 */
	public AlarmEventRsDbQueueSize(CountersModel cm, int threshold, String name, String type, int size)
	{
		super(
				Version.getAppName(), // serviceType
				cm.getServerName(),   // serviceName
				cm.getName(),         // serviceInfo
				name + "-" + type,    // extraInfo
				AlarmEvent.Category.SPACE,
				AlarmEvent.Severity.WARNING, 
				AlarmEvent.ServiceState.UP, 
				"A "+type+" Queue for '"+name+"' in Server '" + cm.getServerName() + "' is above the configured threshold. current size = "+size+". (threshold="+threshold+")",
				threshold
				);

		// Set: Time To Live if postpone is enabled
		setTimeToLive(cm);

		// Set the raw data carier
		setData(size);
	}
}
