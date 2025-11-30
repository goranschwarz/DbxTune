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

import org.apache.commons.lang3.StringUtils;

import com.dbxtune.Version;
import com.dbxtune.cm.CountersModel;

public class AlarmEventLongRunningTransaction
extends AlarmEvent 
{
	private static final long serialVersionUID = 1L;

	public AlarmEventLongRunningTransaction(CountersModel cm, Number thresholdInSec, Number oldestTranInSeconds, Number spid, String dbname, String tranName, String tranCmd, String waitType, String loginName, Double tempdbUsageMb)
	{
		super(
				Version.getAppName(), // serviceType
				cm.getServerName(),   // serviceName
				cm.getName(),         // serviceInfo
				"CmSummary".equals(cm.getName()) ? "[\u03A3]:" + dbname : dbname, // extraInfo -- &sum; or &#8721; or \u03A3 is Greek symbol for Summary
				AlarmEvent.Category.OTHER,
				AlarmEvent.Severity.INFO, 
				AlarmEvent.ServiceState.UP, 
				"Found Long running transaction in '" + cm.getServerName() + "'. DBName='" + dbname + "', Seconds=" + oldestTranInSeconds + ", SPID=" + spid + ", tranName='" + tranName + "', cmd='" + tranCmd + "', waitType='" + waitType + "', login='" + loginName + "', tempdbUsageMb=" + tempdbUsageMb + ". (thresholdInSec="+thresholdInSec+")",
				thresholdInSec);

		// Adjust the Alarm Full Duration with X seconds
		setFullDurationAdjustmentInSec( thresholdInSec == null ? 0 : thresholdInSec.intValue() );

		// Set: Time To Live if postpone is enabled
		setTimeToLive(cm);

		// Set the raw data carier: current cpu usage
		setData(oldestTranInSeconds);
	}

	public AlarmEventLongRunningTransaction(CountersModel cm, Number thresholdInSec, String dbname, Number oldestTranInSeconds, String oldestTranName)
	{
		super(
				Version.getAppName(), // serviceType
				cm.getServerName(),   // serviceName
				cm.getName(),         // serviceInfo
				dbname,               // extraInfo
				AlarmEvent.Category.OTHER,
				AlarmEvent.Severity.INFO, 
				AlarmEvent.ServiceState.UP, 
				"Found Long running transaction in '" + cm.getServerName() + "', dbname='" + dbname +"'. Seconds=" + oldestTranInSeconds + ", TranName='"+StringUtils.trim(oldestTranName)+"'. (thresholdInSec="+thresholdInSec+")",
				thresholdInSec);

		// Adjust the Alarm Full Duration with X seconds
		setFullDurationAdjustmentInSec( thresholdInSec == null ? 0 : thresholdInSec.intValue() );

		// Set: Time To Live if postpone is enabled
		setTimeToLive(cm);

		// Set the raw data carier: current cpu usage
		setData(oldestTranInSeconds);
	}
}
