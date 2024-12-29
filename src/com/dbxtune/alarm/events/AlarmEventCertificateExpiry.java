/*******************************************************************************
 * Copyright (C) 2010-2025 Goran Schwarz
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
package com.dbxtune.alarm.events;

import java.sql.Timestamp;

import com.dbxtune.Version;

public class AlarmEventCertificateExpiry
extends AlarmEvent 
{
	private static final long serialVersionUID = 1L;

	public AlarmEventCertificateExpiry(String srvName, String name, int days_to_expiry, Timestamp expiry_date, long ttl, int thresholdInDays)
	{
		super(
				Version.getAppName(), // serviceType
				srvName,              // serviceName
				name,                 // serviceInfo
				null,                 // extraInfo
				AlarmEvent.Category.OTHER,
				AlarmEvent.Severity.WARNING, 
				AlarmEvent.ServiceState.UP, 
				"Certificate named '" + name + "' at server '" + srvName + "' will exire in " + days_to_expiry + " days, at '" + expiry_date + "'.",
				thresholdInDays
				);

		// Set: Time To Live if postpone is enabled
		setTimeToLive(ttl);

		// Set the raw data
		setData("certName="+name+", daysLeft=" + days_to_expiry + ", expiryDate='" + expiry_date + "'.");
	}
}
