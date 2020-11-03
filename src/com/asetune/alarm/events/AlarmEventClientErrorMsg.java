/*******************************************************************************
 * Copyright (C) 2010-2019 Goran Schwarz
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
package com.asetune.alarm.events;

import com.asetune.AseTune;
import com.asetune.Version;
import com.asetune.cm.CountersModel;

public class AlarmEventClientErrorMsg
extends AlarmEvent 
{
	private static final long serialVersionUID = 1L;

	private boolean _alwaysSend = false;

	/**
	 * <b>always</b> send this alarm<br>
	 * The the "filter" functionality will be discarded/short-circuited 
	 * 
	 * @return true if we should always send, false if we should look at the "filter" settings.
	 */
	@Override
	public boolean alwaysSend()
	{
		return _alwaysSend;
	}


	private static AlarmEvent.Severity getSeverity(int errorNum)
	{
		if (AseTune.APP_NAME.equals(Version.getAppName()))
		{
			// 1105 = Can't allocate space for object '%.*s' in database '%.*s' because '%.*s' segment is full/has no free extents. If you ran out of space in syslogs, dump the transaction log. Otherwise, use ALTER DATABASE to increase the size of the segment.
			if (errorNum == 1105) return AlarmEvent.Severity.ERROR;
		}
		return AlarmEvent.Severity.WARNING;
	}

	private static AlarmEvent.ServiceState getServiceState(int errorNum)
	{
		if (AseTune.APP_NAME.equals(Version.getAppName()))
		{
			if (errorNum == 1105) return AlarmEvent.ServiceState.AFFECTED;
		}
		return AlarmEvent.ServiceState.UP;
	}

	// Set: Time To Live if postpone is enabled
	public AlarmEventClientErrorMsg(CountersModel cm, int errorNum, long errorCount, String errorDesc, int threshold)
	{
		super(
				Version.getAppName(), // serviceType
				cm.getServerName(),   // serviceName
				cm.getName(),         // serviceInfo
				errorNum,             // extraInfo
				AlarmEvent.Category.OTHER,
				getSeverity(errorNum), 
				getServiceState(errorNum), 
				// Note: max length for the below message is 512
				"The Client Error Message " + errorNum + " has been raised " + errorCount + " times in last sample interval of " + cm.getSampleInterval() + " ms to client connection(s) from server '" + cm.getServerName() + "'. Threshold='"+threshold,
				threshold // crossedThreshold... well this one do not have a number.
				);

		_alwaysSend = false;

		// Special handling for some *severe* error messages
		// if it's severe enough, then we *always* want to send events to Alarm Writers (bypass filter handling)
		if (AseTune.APP_NAME.equals(Version.getAppName()))
		{
			// 1105 = Can't allocate space for object '%.*s' in database '%.*s' because '%.*s' segment is full/has no free extents. If you ran out of space in syslogs, dump the transaction log. Otherwise, use ALTER DATABASE to increase the size of the segment.
			if (errorNum == 1105) 
				_alwaysSend = true; // see method 'alwaysSend()' for more info
		}
		
		setTimeToLive(cm);

		// Set the raw data
		setData(errorCount);
	}
}