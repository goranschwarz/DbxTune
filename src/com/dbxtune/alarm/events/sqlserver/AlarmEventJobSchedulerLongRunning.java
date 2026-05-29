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
package com.dbxtune.alarm.events.sqlserver;

import com.dbxtune.Version;
import com.dbxtune.alarm.events.AlarmEvent;
import com.dbxtune.cm.CountersModel;

public class AlarmEventJobSchedulerLongRunning
extends AlarmEvent 
{
	private static final long serialVersionUID = 1L;

	/**
	 * Alarm when A Scheduled job has been running abnormally long.
	 * 
	 * @param cm
	 * @param job_name
	 * @param current_step_name
	 * @param start_execution_date
	 * @param current_runtime_hms
	 */
	public AlarmEventJobSchedulerLongRunning(CountersModel cm, String job_name, String current_step_name, String start_execution_date, String current_runtime_hms)
	{
		super(
				Version.getAppName(), // serviceType
				cm.getServerName(),   // serviceName
				cm.getName(),         // serviceInfo
//				job_name,             // extraInfo
				job_name + " @start_exec='" + start_execution_date + "'", // extraInfo
				AlarmEvent.Category.OTHER,
				AlarmEvent.Severity.WARNING, 
				AlarmEvent.ServiceState.UP, 
				"At server '" + cm.getServerName() + "', The Job '" + job_name + "' and StepName '" + current_step_name + "' has been running for a long time. start_execution_date='" + start_execution_date + "', current_runtime_hms='" + current_runtime_hms + "'.",
				null
				);

		// Set: Time To Live if postpone is enabled
		setTimeToLive(cm);

		// Set the raw data carrier
		setData("job_name='" + job_name + "', current_step_name='" + current_step_name + "', start_execution_date='" + start_execution_date + "', current_runtime_hms='" + current_runtime_hms + "'.");
	}
}
