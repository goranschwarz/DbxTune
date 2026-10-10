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

import java.sql.Timestamp;

import com.dbxtune.Version;
import com.dbxtune.cm.CmSummaryAbstract;
import com.dbxtune.cm.CountersModel;
import com.dbxtune.utils.StringUtil;
import com.dbxtune.utils.TimeUtils;

public class AlarmEventSrvDown
extends AlarmEvent 
{
	private static final long serialVersionUID = 1L;

	/**
	 * <b>always</b> send this alarm<br>
	 * The the "filter" functionality will be discarded/short-circuited 
	 * 
	 * @return true if we should always send, false if we should look at the "filter" settings.
	 */
	@Override
	public boolean alwaysSend()
	{
		return true;
	}

	// Can and SHOULD be cancelled by: AlarmHandler.checkForCancelations()
	// Meaning we do NOT need any CM to be "refreshed" for this alarm to be cancelled
	// How long the Alarms will live is dictated by TimeToLive...
	@Override
	public boolean isAlwaysCancelable()
	{
		return true;
	}
	
	public AlarmEventSrvDown(CountersModel cm)
	{
		super(
				Version.getAppName(), // serviceType
				cm.getServerName(),   // serviceName
				cm.getName(),         // serviceInfo
				null,                 // extraInfo
				AlarmEvent.Category.DOWN, 
				AlarmEvent.Severity.ERROR, 
				AlarmEvent.ServiceState.DOWN, 
				"Server is DOWN. Name='" + cm.getServerName() + "'.",
				null);

		// Set: Time To Live if postpone is enabled
		setTimeToLive(cm);
	}

	public AlarmEventSrvDown(String serverName, String url, Exception connectException, String connectInfoMsg)
	{
		super(
				Version.getAppName(), // serviceType
				serverName,           // serviceName
				url,                  // serviceInfo
				null,                 // extraInfo
				AlarmEvent.Category.DOWN, 
				AlarmEvent.Severity.ERROR, 
				AlarmEvent.ServiceState.DOWN, 
				"Server is DOWN. Name='" + serverName + "', url='" + url + "'.",
				null);
		
		setExtendedDescription("Connect Info Message: " + connectInfoMsg, null);
		setData( StringUtil.isNullOrBlank(serverName) ? url : serverName );
	}

	/** Max difference between two start times that are still "the same" (MySQL calculates it as 'now - Uptime', so it may move a second between samples) */
	private static final long SAME_START_TIME_TOLERANCE_MS = 10 * 1000;

	/**
	 * When the alarm is CANCELLED: set the cancel description to the DBMS start time, and if the DBMS was restarted or not.
	 * <p>
	 * Call this when the alarm is created: the Summary CM is not refreshed while the DBMS is down, so it still holds the start time from BEFORE the outage.
	 *
	 * @param cmSummary  The Summary CM (can be null, then nothing is done at CANCEL)
	 */
	public void setDbmsStartTimeOnCancel(CmSummaryAbstract cmSummary)
	{
		Timestamp startTimeBeforeOutage = cmSummary == null ? null : cmSummary.getDbmsStartTime();

		// Create a callback that may change the AlarmEvent descriptions
		setAlarmDescriptionProvider(cmSummary, new AlarmDescriptionProvider()
		{
			@Override
			public void setValues(CountersModel cm, AlarmEvent alarmEvent, AlarmPhase phase)
			{
				// RAISE and RE-RAISE: keep the connect info (set in the constructor)
				if (AlarmDescriptionProvider.AlarmPhase.CANCEL.equals(phase))
				{
					setCancelValues((CmSummaryAbstract) cm, alarmEvent, startTimeBeforeOutage);
				}
			}
		});
	}

	/**
	 * At CANCEL: was the DBMS restarted? Compare the DBMS start time now, with the one from BEFORE the outage.<br>
	 * The answer goes into the "cancel description" (the extended description keeps the connect info).
	 */
	private static void setCancelValues(CmSummaryAbstract cmSummary, AlarmEvent alarmEvent, Timestamp startTimeBeforeOutage)
	{
		// Only if the Summary CM got data in THIS sample, otherwise it is still the data from before the outage
		if (cmSummary == null || ! cmSummary.hasValidSampleData())
			return;

		Timestamp startTimeNow = cmSummary.getDbmsStartTime();
		if (startTimeNow == null)
			return;

		String startTimeNowStr = TimeUtils.toStringYmdHms(startTimeNow);
		String line1;
		String line2 = null;

		if (startTimeBeforeOutage == null)
		{
			// The collector was (re)started during the outage, so we do not know the start time from before it.
			// Do NOT guess from the alarm time: the DBMS and the collector may not have the same clock (or time zone)
			line1 = "DBMS start time: " + startTimeNowStr;
			line2 = "The start time from before the outage is not known (the collector was started during the outage), so it can not be told if the DBMS was restarted. The alarm was raised at " + TimeUtils.toStringYmdHms(alarmEvent.getCrTime()) + ".";
		}
		else if (Math.abs(startTimeNow.getTime() - startTimeBeforeOutage.getTime()) > SAME_START_TIME_TOLERANCE_MS)
		{
			line1 = "The DBMS was RESTARTED. DBMS start time: " + startTimeNowStr + " (before the outage: " + TimeUtils.toStringYmdHms(startTimeBeforeOutage) + ")";
		}
		else
		{
			line1 = "The DBMS was NOT restarted. DBMS start time: " + startTimeNowStr + " (same as before the outage)";
			line2 = "So it was probably a network problem, or the DBMS did not accept new connections.";
		}

		alarmEvent.setCancelDescription(line1 + (line2 == null ? "" : ". " + line2));
	}
}
