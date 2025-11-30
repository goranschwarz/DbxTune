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

import com.dbxtune.Version;
import com.dbxtune.cm.CountersModel;

public class AlarmEventConfigResourceIsLow
extends AlarmEvent 
{
	private static final long serialVersionUID = 1L;

	/**
	 * Can be used from various places to indicate that something is to low...
	 * @param cm
	 * @param cfgName
	 * @param cfgVal
	 * @param warningText      Text provided by the caller
	 * @param threshold
	 */
	public AlarmEventConfigResourceIsLow(CountersModel cm, String cfgName, double cfgVal, String warningText, Number threshold, int raiseDelayInSec)
	{
		super(
				Version.getAppName(), // serviceType
				cm.getServerName(),   // serviceName
				cm.getName(),         // serviceInfo
				cfgName,              // extraInfo
				AlarmEvent.Category.SRV_CONFIG,
				AlarmEvent.Severity.WARNING,
				AlarmEvent.ServiceState.UP, 
				warningText,
				null);

		// Adjust the Alarm Full Duration with X seconds
		setFullDurationAdjustmentInSec( raiseDelayInSec );

		setRaiseDelayInSec(raiseDelayInSec);
		
		setData("cfgVal="+cfgVal);

		// Set: Time To Live if postpone is enabled
		setTimeToLive(cm);
	}

	/**
	 * Alarm from sp_monitorconfig 'all' (or some other place)
	 * 
	 * @param cm
	 * @param cfgName
	 * @param numFree
	 * @param numActive
	 * @param pctAct
	 * @param thresholdInPct
	 */
//	public AlarmEventConfigResourceIsLow(CountersModel cm, String cfgName, double numFree, double numActive, double pctAct, double thresholdInPct)
	public AlarmEventConfigResourceIsLow(CountersModel cm, String cfgName, double numFree, double numActive, double pctAct, Number threshold)
	{
		super(
				Version.getAppName(), // serviceType
				cm.getServerName(),   // serviceName
				cm.getName(),         // serviceInfo
				cfgName,              // extraInfo
				AlarmEvent.Category.SRV_CONFIG,
				AlarmEvent.Severity.WARNING,
				AlarmEvent.ServiceState.UP, 
				"Configuration resource '"+cfgName+"' is getting low in server '" + cm.getServerName() + "'. NumFree="+numFree+", NumActive="+numActive+", PercentActive="+pctAct+". (threshold="+threshold+")",
				null);

		setData("NumFree="+numFree+", NumActive="+numActive+", PercentActive="+pctAct);

		// Set: Time To Live if postpone is enabled
		setTimeToLive(cm);
	}
}
