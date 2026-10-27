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
package com.dbxtune.alarm.events.db2;

import com.dbxtune.Version;
import com.dbxtune.alarm.events.AlarmEvent;
import com.dbxtune.cm.CountersModel;

/**
 * A Db2 table can not be used (or not changed): REORG PENDING (after some ALTER TABLE), SET INTEGRITY PENDING (after a LOAD or ALTER on a table with constraints),
 * LOAD PENDING, NOT AVAILABLE...
 */
public class AlarmEventDb2TableNotUsable
extends AlarmEvent
{
	private static final long serialVersionUID = 1L;

	/**
	 * @param cm
	 * @param schema    The schema of the table
	 * @param table     The table
	 * @param problem   What is wrong (REORG PENDING, SET INTEGRITY PENDING, ...)
	 */
	public AlarmEventDb2TableNotUsable(CountersModel cm, String schema, String table, String problem)
	{
		super(
				Version.getAppName(), // serviceType
				cm.getServerName(),   // serviceName
				cm.getName(),         // serviceInfo
				schema + "." + table, // extraInfo
				AlarmEvent.Category.OTHER,
				AlarmEvent.Severity.ERROR,
				AlarmEvent.ServiceState.AFFECTED,
				"Table '" + schema + "." + table + "' is in state '" + problem + "' in Server '" + cm.getServerName() + "'. "
					+ "It can not be used (or not changed) until it is fixed.",
				null
				);

		// Set: Time To Live if postpone is enabled
		setTimeToLive(cm);

		// Set the raw data carrier
		setData("table=" + schema + "." + table + ",problem=" + problem);
	}
}
