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

public class AlarmEventOraRedoLogProblem
extends AlarmEvent
{
	private static final long serialVersionUID = 1L;

	/**
	 * A member (file) of an online redo log group is INVALID: Oracle can't use that file.<br>
	 * With only one member in the group that is serious, with several members the group still works (but without the protection)
	 *
	 * @param cm
	 * @param thread           Redo thread (instance)
	 * @param group            Redo log group
	 * @param invalidMembers   Number of members in the group that are INVALID
	 * @param members          Number of members in the group
	 * @param firstMember      File name of the first member in the group
	 */
	public AlarmEventOraRedoLogProblem(CountersModel cm, int thread, int group, int invalidMembers, int members, String firstMember)
	{
		super(
				Version.getAppName(), // serviceType
				cm.getServerName(),   // serviceName
				cm.getName(),         // serviceInfo
				"InvalidMember:thread=" + thread + ",group=" + group, // extraInfo
				AlarmEvent.Category.OTHER,
				invalidMembers >= members ? AlarmEvent.Severity.ERROR : AlarmEvent.Severity.WARNING,
				AlarmEvent.ServiceState.AFFECTED,
				"Redo log group " + group + " (thread " + thread + ") has " + invalidMembers + " INVALID member(s) of " + members + " in Server '" + cm.getServerName() + "'. FirstMember='" + firstMember + "'. Oracle can not use an INVALID file. (Note: a member that was just added is INVALID until the group has been used once)",
				null
				);

		// Set: Time To Live if postpone is enabled
		setTimeToLive(cm);

		// Set the raw data carrier
		setData("thread=" + thread + ",group=" + group + ",invalidMembers=" + invalidMembers + ",members=" + members);
	}

	/**
	 * ALL online redo log groups, except the one being written to, are waiting to be archived: at the next log switch the database 'hangs' until the archiver has caught up.
	 *
	 * @param cm
	 * @param thread             Redo thread (instance)
	 * @param waitingForArchive  Number of groups that are waiting to be archived
	 * @param groups             Number of groups in the thread
	 * @param minWaitingSec      The shortest time one of the waiting groups has waited (so they have ALL waited at least this long)
	 * @param thresholdInSec
	 */
	public AlarmEventOraRedoLogProblem(CountersModel cm, int thread, int waitingForArchive, int groups, int minWaitingSec, int thresholdInSec)
	{
		super(
				Version.getAppName(), // serviceType
				cm.getServerName(),   // serviceName
				cm.getName(),         // serviceInfo
				"WaitingForArchive:thread=" + thread, // extraInfo
				AlarmEvent.Category.RPO,
				AlarmEvent.Severity.ERROR,
				AlarmEvent.ServiceState.AFFECTED,
				"All redo log groups except the current one have been waiting to be archived for at least " + minWaitingSec + " seconds (" + waitingForArchive + " of " + groups + " groups, thread " + thread + ") in Server '" + cm.getServerName() + "'. At the next log switch the database will 'hang' until the archiver has caught up. Check the archive destinations (the tab 'Archive Dest') and the recovery area. (thresholdInSec=" + thresholdInSec + ")",
				thresholdInSec
				);

		// Set: Time To Live if postpone is enabled
		setTimeToLive(cm);

		// Set the raw data carrier
		setData("thread=" + thread + ",waitingForArchive=" + waitingForArchive + ",groups=" + groups);
	}
}
