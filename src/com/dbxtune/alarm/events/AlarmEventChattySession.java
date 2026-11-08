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
package com.dbxtune.alarm.events;

import com.dbxtune.Version;
import com.dbxtune.cm.CountersModel;

/**
 * One session sends very many tiny requests (a possible N+1 / "chatty" application).
 * <p>
 * extraInfo is the session (for example 'spid=45'), so every session has it's own alarm.
 */
public class AlarmEventChattySession
extends AlarmEvent
{
	private static final long serialVersionUID = 1L;

	/**
	 * @param cm                  The CM that raised it
	 * @param sessionId           For example 'spid=45' (used as extraInfo: one alarm per session)
	 * @param sessionDesc         Who it is, for example "spid=45, login='sa', application='xxx', host='yyy'"
	 * @param requestsPerSec      Requests (batches/executions) per second
	 * @param readsPerRequest     Logical reads per request
	 * @param samples             Number of consecutive samples above the threshold
	 * @param thresholdPerSec     Threshold for requests per second
	 * @param maxReadsPerRequest  Threshold for logical reads per request
	 */
	public AlarmEventChattySession(CountersModel cm, String sessionId, String sessionDesc, double requestsPerSec, double readsPerRequest, int samples, Number thresholdPerSec, Number maxReadsPerRequest)
	{
		super(
				Version.getAppName(), // serviceType
				cm.getServerName(),   // serviceName
				cm.getName(),         // serviceInfo
				sessionId,            // extraInfo
				AlarmEvent.Category.OTHER,
				AlarmEvent.Severity.WARNING,
				AlarmEvent.ServiceState.UP,
				// Note: max length for the below message is 512
				"Chatty session (possible N+1) in '" + cm.getServerName() + "': " + sessionDesc + " sends " + Math.round(requestsPerSec) + " requests/s"
					+ " with " + (Math.round(readsPerRequest * 10) / 10.0) + " logical reads each, for " + samples + " samples."
					+ " (threshold: > " + toStr(thresholdPerSec) + " requests/s and < " + toStr(maxReadsPerRequest) + " reads/request)",
				thresholdPerSec);

		// Set: Time To Live if postpone is enabled
		setTimeToLive(cm);

		// Set the raw data carrier
		setData(Math.round(requestsPerSec));
	}

	/** 200.0 -> "200", 2.5 -> "2.5" */
	private static String toStr(Number num)
	{
		if (num == null)
			return "null";
		double d = num.doubleValue();
		return d == Math.rint(d) ? Long.toString((long) d) : Double.toString(d);
	}
}
