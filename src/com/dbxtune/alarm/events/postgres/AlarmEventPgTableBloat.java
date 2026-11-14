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
 * A table has a lot of unused space (bloat), from the estimate in CmPgTableBloat
 */
public class AlarmEventPgTableBloat
extends AlarmEvent
{
	private static final long serialVersionUID = 1L;

	public AlarmEventPgTableBloat(CountersModel cm, String dbname, String schemaName, String tableName, double bloatPct, double bloatMb, double thresholdPct)
	{
		super(
				Version.getAppName(),                            // serviceType
				cm.getServerName(),                              // serviceName
				cm.getName(),                                    // serviceInfo
				dbname + "." + schemaName + "." + tableName,     // extraInfo
				AlarmEvent.Category.SPACE,
				AlarmEvent.Severity.INFO,
				AlarmEvent.ServiceState.UP,
				"Table '" + schemaName + "." + tableName + "' in database '" + dbname + "' at Server '" + cm.getServerName() + "' is estimated to be " + Math.round(bloatPct) + "% unused space ("
				  + Math.round(bloatMb) + " MB). Scans read the empty space too. Vacuum makes the space reusable, but does not give it back: that needs a rewrite of the table "
				  + "(pg_repack, VACUUM FULL or CLUSTER, which lock it). Check the autovacuum settings of the table first. (thresholdPct=" + thresholdPct + ", an estimate)",
				thresholdPct
				);

		setTimeToLive(cm);
		setData("table=" + dbname + "." + schemaName + "." + tableName + ", bloatPct=" + bloatPct + ", bloatMb=" + bloatMb);
	}
}
