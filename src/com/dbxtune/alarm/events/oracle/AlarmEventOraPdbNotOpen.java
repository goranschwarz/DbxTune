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

public class AlarmEventOraPdbNotOpen
extends AlarmEvent
{
	private static final long serialVersionUID = 1L;

	/**
	 * A Pluggable Database (PDB) is not open for normal use: it is closed (MOUNTED), or it is open in RESTRICTED mode (only for administrators).<br>
	 * RESTRICTED typically happens after patching, when the PDB has unresolved 'plug in violations' (see PDB_PLUG_IN_VIOLATIONS).
	 *
	 * @param cm
	 * @param pdbName          Name of the PDB
	 * @param openMode         OPEN_MODE, for example: MOUNTED, READ WRITE, READ ONLY
	 * @param restricted       YES or NO
	 * @param openModeRegex    The regular expression that OPEN_MODE was matched against
	 */
	public AlarmEventOraPdbNotOpen(CountersModel cm, String pdbName, String openMode, String restricted, String openModeRegex)
	{
		super(
				Version.getAppName(), // serviceType
				cm.getServerName(),   // serviceName
				cm.getName(),         // serviceInfo
				pdbName,              // extraInfo
				AlarmEvent.Category.DOWN,
				AlarmEvent.Severity.ERROR,
				AlarmEvent.ServiceState.AFFECTED,
				"The Pluggable Database '" + pdbName + "' is not open for normal use in Server '" + cm.getServerName() + "'. OpenMode='" + openMode + "', Restricted='" + restricted + "'."
					+ ("YES".equalsIgnoreCase(restricted) ? " Only administrators can connect. Check: select * from PDB_PLUG_IN_VIOLATIONS where STATUS <> 'RESOLVED'." : " Nobody can connect to it.")
					+ " (openModeRegex=" + openModeRegex + ")",
				null
				);

		// Set: Time To Live if postpone is enabled
		setTimeToLive(cm);

		// Set the raw data carrier
		setData("pdbName=" + pdbName + ",openMode=" + openMode + ",restricted=" + restricted);
	}
}
