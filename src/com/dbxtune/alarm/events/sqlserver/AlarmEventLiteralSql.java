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
package com.dbxtune.alarm.events.sqlserver;

import com.dbxtune.Version;
import com.dbxtune.alarm.events.AlarmEvent;
import com.dbxtune.cm.CountersModel;
import com.dbxtune.utils.StringUtil;

public class AlarmEventLiteralSql
extends AlarmEvent 
{
	private static final long serialVersionUID = 1L;

	/**
	 * Many cached plans that only differ in their literals (same 'query_hash'), created since the previous sample.
	 * 
	 * @param cm                  The CM (CmPerfCounters)
	 * @param threshold           Number of plans with the same 'query_hash'
	 * @param planCount           Number of plans for this 'query_hash'
	 * @param queryHash           The 'query_hash' (as a hex string)
	 * @param dbname              Database name of the example statement<br>
	 *                            extraInfo = 'dbname/query_hash': one alarm per statement
	 * @param compilationsPerSec  'SQL Compilations/sec'
	 * @param exampleSqlText      One of the statements (the full text is in the extended description; here it is cut in the middle to fit the description)
	 */
	public AlarmEventLiteralSql(CountersModel cm, int threshold, int planCount, String queryHash, String dbname, double compilationsPerSec, String exampleSqlText)
	{
		super(
				Version.getAppName(), // serviceType
				cm.getServerName(),   // serviceName
				cm.getName(),         // serviceInfo
				StringUtil.truncate(dbname + "/" + queryHash, 80, false, null), // extraInfo (max 80 in the alarm tables)
				AlarmEvent.Category.OTHER,
				AlarmEvent.Severity.INFO, 
				AlarmEvent.ServiceState.UP, 
				fitDescription("At server '" + cm.getServerName() + "', " + planCount + " cached plans created since the last sample only differ in their literals (non-parameterized SQL). "
					+ "query_hash=" + queryHash + ", dbname='" + dbname + "', SQL Compilations/sec=" + compilationsPerSec + ". "
					+ "Example: ", exampleSqlText),
				threshold
				);

		// Set: Time To Live if postpone is enabled
		setTimeToLive(cm);

		// Set the raw data carrier
		setData(planCount);
	}

	/**
	 * The description is max 512 chars (column 'description' in the alarm tables): cut the example SQL in the MIDDLE,
	 * keeping the start and more of the end (statements that only differ in their literals often differ in the WHERE clause, at the end)
	 */
	private static String fitDescription(String prefix, String sqlText)
	{
		if (sqlText == null)
			sqlText = "";
		int room = 512 - prefix.length();
		if (sqlText.length() <= room)
			return prefix + sqlText;

		String cut  = " ... ";
		int    keep = Math.max(0, room - cut.length());
		int    head = keep / 3;
		return prefix + sqlText.substring(0, head) + cut + sqlText.substring(sqlText.length() - (keep - head));
	}

	/** A design finding to fix in the code (non-parameterized SQL), not an incident: only recorded (alarm history, Daily Summary Report). Property 'AlarmEventLiteralSql.action' overrides it. */
	@Override
	public Action getDefaultAction()
	{
		return Action.REPORT;
	}
}
