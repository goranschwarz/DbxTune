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

public class AlarmEventOraInvalidObjects
extends AlarmEvent
{
	private static final long serialVersionUID = 1L;

	/**
	 * A schema has UNUSABLE indexes (or index partitions), or INVALID objects.<br>
	 * An unusable index is not maintained and not used: queries get slow (full scans), and DML on the table can fail with ORA-01502.
	 *
	 * @param cm
	 * @param problem        'UNUSABLE INDEX' or 'INVALID OBJECT'
	 * @param owner          The schema (in a CDB: 'containerName:owner')
	 * @param objectType     Type of object, for example: INDEX, INDEX PARTITION, PACKAGE BODY, VIEW
	 * @param objectCount    How many objects of that type that has the problem
	 * @param exampleName    Name of one of them
	 * @param threshold
	 */
	public AlarmEventOraInvalidObjects(CountersModel cm, String problem, String owner, String objectType, int objectCount, String exampleName, int threshold)
	{
		super(
				Version.getAppName(), // serviceType
				cm.getServerName(),   // serviceName
				cm.getName(),         // serviceInfo
				problem + ":" + owner + ":" + objectType, // extraInfo
				AlarmEvent.Category.OTHER,
				AlarmEvent.Severity.WARNING,
				AlarmEvent.ServiceState.UP,
				"Schema '" + owner + "' has " + objectCount + " " + problem + "(s) of type '" + objectType + "' in Server '" + cm.getServerName() + "'. One of them is '" + exampleName + "'."
					+ ("UNUSABLE INDEX".equals(problem) ? " An unusable index is not used by queries, and DML on the table can fail with ORA-01502. Fix: ALTER INDEX ... REBUILD." : "")
					+ " (threshold=" + threshold + ")",
				threshold
				);

		// Set: Time To Live if postpone is enabled
		setTimeToLive(cm);

		// Set the raw data carrier
		setData("problem=" + problem + ",owner=" + owner + ",objectType=" + objectType + ",objectCount=" + objectCount);
	}
}
