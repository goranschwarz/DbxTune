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
 * The AlarmHandler calls {@link #setValues(CountersModel, AlarmEvent, Phase)}:
 * <ul>
 *   <li>{@link Phase#RAISE}:    when the alarm is raised (for an alarm with a raise delay: when the delay has passed)</li>
 *   <li>{@link Phase#RE_RAISE}: when the alarm is raised again while it is active (on the NEW AlarmEvent object,
 *                               its values are copied to the active alarm as the "reRaise" values, as always)</li>
 *   <li>{@link Phase#CANCEL}:   when the alarm is cancelled, at the end of the first sample where the alarm condition is no longer true.
 *                               The CM then holds the data from that sample, so the alarm can show how things look when the problem is over.
 *                               What is set here <b>replaces</b> the values of the cancelled alarm (so the CANCEL message shows them).<br>
 *                               Use {@link AlarmEvent#setCancelDescription(String)} for a short text about that (template variable <code>${cancelDescription}</code>).</li>
 * </ul>
 * Rules for an implementation:
 * <ul>
 *   <li><b>Read</b> the values from the CM when called, do not use values from when the alarm was created.</li>
 *   <li>At CANCEL the row the alarm was about may be gone, then simply do not change anything.</li>
 *   <li>Exceptions are caught and logged, the alarm is still sent.</li>
 *   <li>The provider and the CM are NOT saved with the alarm (active alarms are serialized to disk between restarts).
 *       An alarm restored after a restart gets them back the next time it is re-raised.</li>
 *   <li>It is called on the collector thread, after the CM's are refreshed (AlarmHandler.addAlarm() and AlarmHandler.endOfScan()).</li>
 * </ul>
 * Example:
 * <pre>
 * AlarmEvent ae = new AlarmEventLowOnWorkerThreads(cm, threshold, availableWorkers, maxWorkers, allocatedWorkers);
 * ae.setAlarmDescriptionProvider(cm, new AlarmDescriptionProvider()
 * {
 *     &#64;Override
 *     public void setValues(CountersModel cm, AlarmEvent alarmEvent, Phase phase)
 *     {
 *         alarmEvent.setExtendedDescription("", cm.getGraphDataHistoryAsHtmlImage(GRAPH_NAME_WORKER_THREAD_USAGE));
 *     }
 * });
 * </pre>
 */
@FunctionalInterface
public interface AlarmDescriptionProvider
{
	/** When the provider is called */
	public enum Phase
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
	public void setValues(CountersModel cm, AlarmEvent alarmEvent, Phase phase);
}
