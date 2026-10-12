/*******************************************************************************
 * Copyright (C) 2010-2027 Goran Schwarz
 *
 * This file is part of DbxTune
 * DbxTune is a family of sub-products *Tune, hence the Dbx
 * Here are some of the tools: AseTune, IqTune, RsTune, RaxTune, HanaTune,
 *          SqlServerTune, PostgresTune, MySqlTune, MariaDbTune, Db2Tune, ...
 *
 * DbxTune is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * DbxTune is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with DbxTune.  If not, see <http://www.gnu.org/licenses/>.
 ******************************************************************************/
package com.dbxtune.alarm.events.oracle;

import com.dbxtune.Version;
import com.dbxtune.alarm.events.AlarmEvent;
import com.dbxtune.cm.CountersModel;
import com.dbxtune.utils.StringUtil;

public class AlarmEventOraJobFailed
extends AlarmEvent
{
	private static final long serialVersionUID = 1L;

	/**
	 * The LAST run of a job failed (a Scheduler job, an automatic maintenance task, or an old DBMS_JOB).
	 *
	 * @param cm
	 * @param jobKind       SCHEDULER, AUTOTASK or DBMS_JOB
	 * @param jobLabel      [container:]owner.jobName
	 * @param lastRunTime   When the run started (null if not known, which it is for a DBMS_JOB)
	 * @param ageInHours    Hours since the run started (null if not known)
	 * @param errorNumber   The ORA- error number of the run (null if not known)
	 * @param errorInfo     The error text of the run (null if not known)
	 * @param thresholdInHours
	 */
	public AlarmEventOraJobFailed(CountersModel cm, String jobKind, String jobLabel, String lastRunTime, Double ageInHours, Integer errorNumber, String errorInfo, int thresholdInHours)
	{
		super(
				Version.getAppName(), // serviceType
				cm.getServerName(),   // serviceName
				cm.getName(),         // serviceInfo
				StringUtil.truncate("JobFailed:" + jobLabel, 80, true, null), // extraInfo (max 80 characters)
				AlarmEvent.Category.OTHER,
				AlarmEvent.Severity.WARNING,
				AlarmEvent.ServiceState.UP,
				"The last run of job '" + jobLabel + "' (" + jobKind + ") FAILED in Server '" + cm.getServerName() + "'."
						+ (StringUtil.hasValue(lastRunTime) ? " It started at '" + lastRunTime + "' (" + ageInHours + " hours ago)." : "")
						+ (errorNumber != null && errorNumber != 0 ? " Error=ORA-" + String.format("%05d", errorNumber) + "." : "")
						+ (StringUtil.hasValue(errorInfo) ? " Info='" + StringUtil.truncate(errorInfo.replace('\n', ' '), 150, true, null) + "'." : "")
						+ " (it goes away when a later run succeeds, or after " + thresholdInHours + " hours)",
				thresholdInHours
				);

		// Set: Time To Live if postpone is enabled
		setTimeToLive(cm);

		// Set the raw data carrier
		setData("jobKind=" + jobKind + ",job=" + jobLabel + ",error=" + errorNumber);
	}

	/**
	 * A job is in a state where it will not run again by itself: BROKEN (it failed too many times, or someone marked it), FAILED, CHAIN_STALLED...
	 *
	 * @param cm
	 * @param jobKind        SCHEDULER or DBMS_JOB
	 * @param jobLabel       [container:]owner.jobName
	 * @param state          STATE of the job
	 * @param failureCount   Number of times the job has failed
	 * @param stateRegex     The states that we send an alarm for
	 */
	public AlarmEventOraJobFailed(CountersModel cm, String jobKind, String jobLabel, String state, long failureCount, String stateRegex)
	{
		super(
				Version.getAppName(), // serviceType
				cm.getServerName(),   // serviceName
				cm.getName(),         // serviceInfo
				StringUtil.truncate("JobBroken:" + jobLabel, 80, true, null), // extraInfo (max 80 characters)
				AlarmEvent.Category.OTHER,
				AlarmEvent.Severity.WARNING,
				AlarmEvent.ServiceState.UP,
				"Job '" + jobLabel + "' (" + jobKind + ") has STATE='" + state + "' in Server '" + cm.getServerName() + "': it will NOT run again until someone fixes it. FailureCount=" + failureCount + "."
						+ ("DBMS_JOB".equals(jobKind) ? " Fix what is wrong, then: exec DBMS_JOB.BROKEN(<job>, FALSE)" : " Fix what is wrong, then: exec DBMS_SCHEDULER.ENABLE('<owner>.<job>')")
						+ " (states=" + stateRegex + ")",
				null
				);

		// Set: Time To Live if postpone is enabled
		setTimeToLive(cm);

		// Set the raw data carrier
		setData("jobKind=" + jobKind + ",job=" + jobLabel + ",state=" + state + ",failureCount=" + failureCount);
	}
}
