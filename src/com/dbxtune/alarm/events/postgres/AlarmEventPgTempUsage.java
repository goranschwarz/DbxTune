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
package com.dbxtune.alarm.events.postgres;

import com.dbxtune.Version;
import com.dbxtune.alarm.events.AlarmEvent;
import com.dbxtune.cm.CountersModel;

/**
 * A database writes a lot of temporary files (sorts and hashes that did not fit in work_mem)
 */
public class AlarmEventPgTempUsage
extends AlarmEvent
{
	private static final long serialVersionUID = 1L;

	public AlarmEventPgTempUsage(CountersModel cm, String dbname, double mbPerMinute, long files, double thresholdMbPerMinute)
	{
		super(
				Version.getAppName(), // serviceType
				cm.getServerName(),   // serviceName
				cm.getName(),         // serviceInfo
				dbname,               // extraInfo
				AlarmEvent.Category.SPACE,
				AlarmEvent.Severity.INFO,
				AlarmEvent.ServiceState.UP,
				"Database '" + dbname + "' at Server '" + cm.getServerName() + "' writes " + Math.round(mbPerMinute) + " MB of temporary files per minute (" + files + " files in the last sample): "
				  + "sorts and hashes that did not fit in work_mem. Look at the statements with temp_blks_written in 'Statements', or at log_temp_files. (thresholdMbPerMinute=" + thresholdMbPerMinute + ")",
				thresholdMbPerMinute
				);

		setTimeToLive(cm);
		setData("dbname=" + dbname + ", mbPerMinute=" + mbPerMinute + ", files=" + files);
	}
}
