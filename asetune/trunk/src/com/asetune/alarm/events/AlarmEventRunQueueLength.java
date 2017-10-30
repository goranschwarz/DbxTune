package com.asetune.alarm.events;

import com.asetune.Version;
import com.asetune.alarm.events.AlarmEventOsLoadAverage.RangeType;
import com.asetune.cm.CountersModel;

public class AlarmEventRunQueueLength
extends AlarmEvent 
{
	private static final long serialVersionUID = 1L;

	public enum RangeType
	{
		RANGE_1_MINUTE, RANGE_5_MINUTE, RANGE_15_MINUTE
	};
	
	public AlarmEventRunQueueLength(CountersModel cm, RangeType rangeType, Number avg_1min, Number avg_5min, Number avg_15min)
	{
		super(
				Version.getAppName(), // serviceType
				cm.getServerName(),   // serviceName
				cm.getName(),         // serviceInfo
				rangeType+"",         // extraInfo
				AlarmEvent.Severity.WARNING, 
				AlarmEvent.ServiceState.UP, 
				"Exhausting CPU Scheduling resources in '" + cm.getServerName() + "'. RunQueueLength: avg_1min=" + avg_1min + ", avg_5min="+avg_5min+", avg_15min="+avg_15min+".");

		// Set: Time To Live if postpone is enabled
		setTimeToLive(cm);

		// Set the raw data carier: run queue length average 1 minute
		// Set the raw data carier: run queue length average 1 minute
		if      (RangeType.RANGE_1_MINUTE .equals(rangeType)) setData(avg_1min);
		else if (RangeType.RANGE_5_MINUTE .equals(rangeType)) setData(avg_5min);
		else if (RangeType.RANGE_15_MINUTE.equals(rangeType)) setData(avg_15min);
		else                                                  setData(-1);
	}
}
