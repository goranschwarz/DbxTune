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
package com.dbxtune.alarm.events.mysql;

import com.dbxtune.Version;
import com.dbxtune.alarm.events.AlarmEvent;
import com.dbxtune.cm.CountersModel;

/**
 * The oldest open InnoDB transaction has been open longer than the threshold.
 * <p>
 * MySQL has no "database" for a transaction (it can touch any schema), so the text says who has it instead (the generic AlarmEventLongRunningTransaction says dbname).
 */
public class AlarmEventMyLongRunningTransaction
extends AlarmEvent
{
	private static final long serialVersionUID = 1L;

	/**
	 * @param cm
	 * @param thresholdInSec
	 * @param oldestTranInSec      How long the oldest transaction has been open
	 * @param pid                  The connection (processlist id) that has it
	 * @param user                 Its user
	 * @param host                 Its client host
	 * @param state                The state of the transaction: RUNNING, LOCK WAIT, ROLLING BACK, COMMITTING
	 * @param historyListLength    InnoDB history list length (undo that purge can't remove while the transaction is open)
	 */
	public AlarmEventMyLongRunningTransaction(CountersModel cm, Number thresholdInSec, Number oldestTranInSec, Number pid, String user, String host, String state, Number historyListLength)
	{
		super(
				Version.getAppName(), // serviceType
				cm.getServerName(),   // serviceName
				cm.getName(),         // serviceInfo
				"pid=" + pid,         // extraInfo
				AlarmEvent.Category.OTHER,
				AlarmEvent.Severity.INFO,
				AlarmEvent.ServiceState.UP,
				"Long running transaction in Server '" + cm.getServerName() + "': open for " + oldestTranInSec + " seconds. "
						+ "pid=" + pid + ", user='" + user + "', host='" + host + "', state='" + state + "'. "
						+ "It keeps its locks, and purge can't remove the old row versions (history list length=" + historyListLength + "). (thresholdInSec=" + thresholdInSec + ")",
				thresholdInSec);

		// Adjust the Alarm Full Duration with X seconds
		setFullDurationAdjustmentInSec( thresholdInSec == null ? 0 : thresholdInSec.intValue() );

		// Set: Time To Live if postpone is enabled
		setTimeToLive(cm);

		// Set the raw data carrier
		setData(oldestTranInSec);
	}
}
