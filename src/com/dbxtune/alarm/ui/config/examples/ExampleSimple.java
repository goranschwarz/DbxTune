/*******************************************************************************
 * Copyright (C) 2010-2025 Goran Schwarz
 * 
 * This file is part of DbxTune
 * DbxTune is a family of sub-products *Tune, hence the Dbx
 * Here are some of the tools: AseTune, IqTune, RsTune, RaxTune, HanaTune, 
 *          SqlServerTune, PostgresTune, MySqlTune, MariaDbTune, Db2Tune, ...
 * 
 * DbxTune is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, version 3 of the License.
 * 
 * DbxTune is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License
 * along with DbxTune.  If not, see <http://www.gnu.org/licenses/>.
 ******************************************************************************/
package com.dbxtune.alarm.ui.config.examples;

import com.dbxtune.alarm.AlarmHandler;
import com.dbxtune.alarm.IUserDefinedAlarmInterrogator;
import com.dbxtune.alarm.events.AlarmEvent;
import com.dbxtune.alarm.events.AlarmEventHighCpuUtilization;
import com.dbxtune.cm.CountersModel;

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
