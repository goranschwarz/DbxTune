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
 * A Db2 table space is not in state NORMAL, for example BACKUP_PENDING (after a LOAD ... COPY NO), OFFLINE (a container is missing),
 * ROLLFORWARD_PENDING or QUIESCED_EXCLUSIVE. The tables in it can usually not be used (or not be changed) until it is fixed.
 */
public class AlarmEventDb2TablespaceState
extends AlarmEvent
{
	private static final long serialVersionUID = 1L;

	/**
	 * @param cm
	 * @param dbname     The database
	 * @param tbspName   The table space
	 * @param state      TBSP_STATE from MON_GET_TABLESPACE
	 */
	public AlarmEventDb2TablespaceState(CountersModel cm, String dbname, String tbspName, String state)
	{
		super(
				Version.getAppName(), // serviceType
				cm.getServerName(),   // serviceName
				cm.getName(),         // serviceInfo
				dbname + ":" + tbspName, // extraInfo
				AlarmEvent.Category.OTHER,
				AlarmEvent.Severity.ERROR,
				AlarmEvent.ServiceState.AFFECTED,
				"Table space '" + tbspName + "' is in state '" + state + "' in Server '" + cm.getServerName() + "', dbname '" + dbname + "'. "
					+ "The tables in it may not be usable (or not changeable) until it is back in state NORMAL.",
				null
				);

		// Set: Time To Live if postpone is enabled
		setTimeToLive(cm);

		// Set the raw data carrier
		setData("dbname=" + dbname + ",tbsp=" + tbspName + ",state=" + state);
	}
}
