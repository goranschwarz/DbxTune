package com.asetune.alarm.events;

import com.asetune.Version;
import com.asetune.cm.CountersModel;

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
	public AlarmEventConfigResourceIsLow(CountersModel cm, String cfgName, double cfgVal, String warningText, Number threshold)
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

		setData("cfgVal="+cfgVal);

		// Set: Time To Live if postpone is enabled
		setTimeToLive(cm);
	}

	/**
	 * Alarm from sp_monitorconfig 'all'
	 * 
	 * @param cm
	 * @param cfgName
	 * @param numFree
	 * @param numActive
	 * @param pctAct
	 * @param thresholdInPct
	 */
	public AlarmEventConfigResourceIsLow(CountersModel cm, String cfgName, double numFree, double numActive, double pctAct, double thresholdInPct)
	{
		super(
				Version.getAppName(), // serviceType
				cm.getServerName(),   // serviceName
				cm.getName(),         // serviceInfo
				cfgName,              // extraInfo
				AlarmEvent.Category.SRV_CONFIG,
				AlarmEvent.Severity.WARNING,
				AlarmEvent.ServiceState.UP, 
				"Configuration resource '"+cfgName+"' is getting low in server '" + cm.getServerName() + "'. NumFree="+numFree+", NumActive="+numActive+", PercentActive="+pctAct+". (thresholdInPct="+thresholdInPct+")",
				null);

		setData("NumFree="+numFree+", NumActive="+numActive+", PercentActive="+pctAct);

		// Set: Time To Live if postpone is enabled
		setTimeToLive(cm);
	}
}
