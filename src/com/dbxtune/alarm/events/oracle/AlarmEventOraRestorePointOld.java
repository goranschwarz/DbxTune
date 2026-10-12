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

public class AlarmEventOraRestorePointOld
extends AlarmEvent
{
	private static final long serialVersionUID = 1L;

	/**
	 * A GUARANTEED restore point is old: someone made it before a change (an upgrade, a release) and forgot to drop it.<br>
	 * Oracle keeps ALL flashback logs since it was made, until it is dropped. That fills the recovery area, and when that is full the database stops.
	 *
	 * @param cm
	 * @param restorePointLabel   [container:]name
	 * @param created             When the restore point was made
	 * @param ageInHours          Hours since then
	 * @param storageMb           Space in the recovery area that is kept because of it
	 * @param thresholdInHours
	 */
	public AlarmEventOraRestorePointOld(CountersModel cm, String restorePointLabel, String created, double ageInHours, double storageMb, double thresholdInHours)
	{
		super(
				Version.getAppName(), // serviceType
				cm.getServerName(),   // serviceName
				cm.getName(),         // serviceInfo
				StringUtil.truncate(restorePointLabel, 80, true, null), // extraInfo (max 80 characters)
				AlarmEvent.Category.SPACE,
				AlarmEvent.Severity.WARNING,
				AlarmEvent.ServiceState.UP,
				"The GUARANTEED restore point '" + restorePointLabel + "' is " + ageInHours + " hours old (made at '" + created + "') in Server '" + cm.getServerName() + "'. "
						+ "Oracle keeps all flashback logs since then (now " + storageMb + " MB) until it is dropped, and a full recovery area stops the database. "
						+ "Not needed any more? DROP RESTORE POINT <name>. (thresholdInHours=" + thresholdInHours + ")",
				thresholdInHours
				);

		// Set: Time To Live if postpone is enabled
		setTimeToLive(cm);

		// Set the raw data carrier
		setData("restorePoint=" + restorePointLabel + ",ageInHours=" + ageInHours + ",storageMb=" + storageMb);
	}
}
