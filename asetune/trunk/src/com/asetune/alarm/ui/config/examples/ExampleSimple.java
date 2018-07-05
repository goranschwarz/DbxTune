package com.asetune.alarm.ui.config.examples;

import com.asetune.alarm.AlarmHandler;
import com.asetune.alarm.IUserDefinedAlarmInterrogator;
import com.asetune.alarm.events.AlarmEvent;
import com.asetune.alarm.events.AlarmEventHighCpuUtilization;
import com.asetune.cm.CountersModel;

public class ExampleSimple
implements IUserDefinedAlarmInterrogator
{
	/**
	 * Check values for this CM (Counter Model) and generate any desired alarms
	 */
	@Override
	public void interrogateCounterData(CountersModel cm)
	{
		// No RATE data, get out of here (on first sample we will only have ABS data)
		if ( ! cm.hasRateData() )
			return;

		// If we havn't got any alarm handler; exit
		if ( ! AlarmHandler.hasInstance() )
			return;

		// If we havn't got all desired column names; exit
		if ( ! cm.hasColumns("ColName1") )
			return;
		
		// Your logic
		Double colName1 = cm.getRateValueAvg("ColName1");
		if (colName1 > 50.0)
		{
			// Create the alarm and add/send it to the AlrmHandler
			AlarmEvent alarm = new AlarmEventHighCpuUtilization(cm, 50.0, colName1);
			AlarmHandler.getInstance().addAlarm(alarm);
		}
	}
}
