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

import com.dbxtune.cm.CountersModel;

/**
 * Sets the values of an {@link AlarmEvent} (extended description, description, data...) from the <b>current</b> data in a CM.
 * <p>
 * Set it with {@link AlarmEvent#setAlarmDescriptionProvider(CountersModel, AlarmDescriptionProvider)}.
 * The AlarmHandler calls {@link #setValues(CountersModel, AlarmEvent, AlarmPhase)}:
 * <ul>
 *   <li>{@link AlarmPhase#RAISE}:    when the alarm is raised (for an alarm with a raise delay: when the delay has passed)</li>
 *   <li>{@link AlarmPhase#RE_RAISE}: when the alarm is raised again while it is active (on the NEW AlarmEvent object,
 *                               its values are copied to the active alarm as the "reRaise" values, as always)</li>
 *   <li>{@link AlarmPhase#CANCEL}:   when the alarm is cancelled, at the end of the first sample where the alarm condition is no longer true.
 *                               The CM then holds the data from that sample, so the alarm can show how things look when the problem is over.
 *                               What is set here <b>replaces</b> the values of the cancelled alarm (so the CANCEL message shows them).<br>
 *                               Use {@link AlarmEvent#setCancelDescription(String)} for a short text about that (template variable <code>${cancelDescription}</code>).</li>
 * </ul>
 * <h3>How it works (what calls what, and when)</h3>
 * An alarm is cancelled because it was <b>not raised again</b>: nobody tells the AlarmHandler "the problem is gone".
 * So the code that found the problem (the CM's sendAlarmRequest()) never runs at CANCEL. The provider is the way for
 * that code to leave a callback on the alarm, which the AlarmHandler calls later, when the CM's hold the data from the
 * sample where the problem went away.
 * <pre>
 * CounterCollectorThread, one sample:
 *  |
 *  |-- for each CM: refresh data, then cm.sendAlarmRequest()
 *  |      |-- ae = new AlarmEventXxx(...)
 *  |      |-- ae.setAlarmDescriptionProvider(cm, provider)     -- only STORED on the alarm (transient), nothing is called yet
 *  |      |-- AlarmHandler.addAlarm(ae) -&gt; raiseInternal()
 *  |             |-- a new alarm:      ae.callAlarmDescriptionProvider(RAISE)
 *  |             |-- already active:   ae.callAlarmDescriptionProvider(RE_RAISE)
 *  |                                   + AlarmContainer.handleReRaise(): the ACTIVE alarm takes over the newest provider + CM
 *  |
 *  |-- AlarmHandler.endOfScan() -&gt; checkForCancelations()
 *         for each active alarm that was NOT raised in this sample:
 *           |-- mark it as cancelled
 *           |-- alarm.callAlarmDescriptionProvider(CANCEL)      -- provider.setValues(cm, alarm, CANCEL)
 *           |-- send it to the writers (Mail, Slack, Teams...), the PCS and DbxCentral
 * </pre>
 * All providers are written in-line (an anonymous class) where the alarm is created:
 * <ul>
 *   <li>Most CM alarms use {@link AlarmDescriptionProviderBase}: it holds the threshold and (optionally) the PK of the row the alarm
 *       is about, and has helpers for the CANCEL text (getAlarmCancelText(), getAlarmCancelTextGone(), toAlarmCancelValue()).
 *       The provider finds the row again (by PK, or by a column for CM's without a PK), sets the extended description
 *       (graphs from the CURRENT data; for "offender" alarms like statements/sessions the table is captured when the alarm was raised)
 *       and at CANCEL sets the cancel description.</li>
 *   <li>A few implement this interface directly: sqlserver CmSummary, CmOsMpstat, AlarmEventSrvDown.</li>
 * </ul>
 * <p>
 * Rules for an implementation:
 * <ul>
 *   <li><b>Read</b> the values from the CM when called (graphs, the row, the value for the CANCEL text).
 *       Exception: a table that describes the "offender" (a statement, session, SQL text...) is captured in a <code>final</code>
 *       variable when the alarm is (re)raised, since at CANCEL that row may be gone or describe something else.</li>
 *   <li>Find the row again by its PK (or a column), never by the row index 'r' (it is only valid in that sample).</li>
 *   <li>At CANCEL the row the alarm was about may be gone: then keep the extended description, and set the cancel description
 *       to "... is no longer present" (AlarmDescriptionProviderBase.getAlarmCancelTextGone()).</li>
 *   <li>Exceptions are caught and logged, the alarm is still sent.</li>
 *   <li>The provider and the CM are NOT saved with the alarm (active alarms are serialized to disk between restarts).
 *       An alarm restored after a restart gets them back the next time it is re-raised.</li>
 *   <li>It is called on the collector thread, after the CM's are refreshed (AlarmHandler.addAlarm() and AlarmHandler.endOfScan()).</li>
 * </ul>
 * Example (the template, from sqlserver CmDatabases 'LowDbFreeSpaceInMb'):
 * <pre>
 * AlarmEvent ae = new AlarmEventLowDbFreeSpace(cm, dbname, freeMb.intValue(), usedPct, threshold.intValue());
 *
 * // The below is called from AlarmHandler on: Raise, RE-RAISE &amp; CANCEL
 * ae.setAlarmDescriptionProvider(this, new AlarmDescriptionProviderBase(cm.getPk(), cm.getAbsPkValue(r), threshold)
 * {
 *     &#64;Override
 *     public void setValues(CountersModel cm, AlarmEvent alarmEvent, AlarmPhase phase)
 *     {
 *         String rowPk = getRowPk();
 *         int    rowId = cm.getAbsRowIdForPkValue(rowPk);
 *
 *         String ldbname = (rowId == -1) ? dbname : cm.getAbsString(rowId, "DBName");
 *         String label   = "Database '" + ldbname + "'";
 *
 *         if (rowId != -1)
 *         {
 *             // Always: (RAISE, RE-RAISE, CANCEL) - Set graph values
 *             String extendedDescText = cm.toTextTableString(DATA_RATE, rowId);
 *             String extendedDescHtml = cm.toHtmlTableString(DATA_RATE, rowId, true, false, false);
 *             extendedDescHtml += "&lt;br&gt;&lt;br&gt;" + cm.getGraphDataHistoryAsHtmlImage(GRAPH_NAME_DATASIZE_LEFT_MB, ldbname);
 *             alarmEvent.setExtendedDescription(extendedDescText, extendedDescHtml);
 *
 *             // Set CANCEL message (values that was found AFTER last raise/re-raise event)
 *             if (AlarmPhase.CANCEL.equals(phase))
 *             {
 *                 Double DataSizeFreeInMb = cm.getAbsValueAsDouble(rowId, "DataSizeFreeInMb");
 *                 Double DataSizeUsedPct  = cm.getAbsValueAsDouble(rowId, "DataSizeUsedPct");
 *
 *                 String cancelMsg = getAlarmCancelText(label + ": free data space", toAlarmCancelValue(DataSizeFreeInMb) + " MB, " + toAlarmCancelValue(DataSizeUsedPct) + "% used", " MB");
 *                 alarmEvent.setCancelDescription(cancelMsg);
 *             }
 *         }
 *         else
 *         {
 *             if (AlarmPhase.CANCEL.equals(phase))
 *                 alarmEvent.setCancelDescription(getAlarmCancelTextGone(label));
 *         }
 *     }
 * });
 * </pre>
 * Variants of the template (see {@link AlarmDescriptionProviderBase}): no row (single row CM's, sums, averages),
 * a row found by a column (CM's without a PK), and "offender" alarms (the table captured at (re)raise).
 */
@FunctionalInterface
public interface AlarmDescriptionProvider
{
	/** When the provider is called */
	public enum AlarmPhase
	{
		/** The alarm is raised */
		RAISE,

		/** The alarm is raised again, while it is active */
		RE_RAISE,

		/** The alarm is cancelled */
		CANCEL
	};

	/**
	 * Set values in the AlarmEvent (for example with setExtendedDescription()) from the current data in the CM
	 *
	 * @param cm          The CM passed to setAlarmDescriptionProvider() (can be null if null was passed)
	 * @param alarmEvent  The alarm to set values in. At CANCEL it is already marked as cancelled (getCancelTime() has a value)
	 * @param phase       RAISE, RE_RAISE or CANCEL
	 */
	public void setValues(CountersModel cm, AlarmEvent alarmEvent, AlarmPhase phase);
}
