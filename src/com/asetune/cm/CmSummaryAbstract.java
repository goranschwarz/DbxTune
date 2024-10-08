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
package com.asetune.cm;

import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;

import javax.jws.soap.SOAPBinding;

import com.asetune.ICounterController;
import com.asetune.alarm.AlarmHandler;
import com.asetune.alarm.events.AlarmEvent;
import com.asetune.alarm.events.AlarmEventDbmsVersionString;
import com.asetune.central.pcs.CentralPersistReader;
import com.asetune.graph.TrendGraphDataPoint;
import com.asetune.graph.TrendGraphDataPoint.LabelType;
import com.asetune.utils.Configuration;
import com.asetune.utils.StringUtil;

public abstract class CmSummaryAbstract
extends CountersModel
{
	private static final long serialVersionUID = 1L;

	/**
	 * @param name                        Name of the Counter Model
	 * @param groupName                   Name of the Group this counter belongs to, can be null
	 * @param sql                         SQL Used to grab a sample from the counter data
	 * @param pkList                      A list of columns that will be used during diff calculations to lookup values in previous samples
	 * @param diffColumns                 Columns to do diff calculations on
	 * @param pctColumns                  Columns that is to be considered as Percent calculated columns, (they still need to be apart of diffColumns)
	 * @param monTables                   What monitor tables are accessed in this query, used for TOOLTIP lookups
	 * @param dependsOnRole               Needs following role(s)
	 * @param dependsOnConfig             Check that these configurations are above 0
	 * @param dependsOnVersion            What version of ASE do we need to sample for this CounterModel
	 * @param dependsOnCeVersion          What version of ASE-CE do we need to sample for this CounterModel
	 * @param negativeDiffCountersToZero  if diff calculations is negative, reset the counter to zero.
	 * @param systemCm                    Is this a system CM
	 * @param defaultPostponeTime         default postpone time
	 */
	public CmSummaryAbstract
	(
			ICounterController counterController,
			String       name,
			String       groupName,
			String       sql,
			List<String> pkList,
			String[]     diffColumns,
			String[]     pctColumns,
			String[]     monTables,
			String[]     dependsOnRole,
			String[]     dependsOnConfig,
			long         dependsOnVersion,
			long         dependsOnCeVersion,
			boolean      negativeDiffCountersToZero,
			boolean      systemCm,
			int          defaultPostponeTime
	)
	{
		super(counterController, name, groupName, sql, pkList, diffColumns, pctColumns, monTables, dependsOnRole, dependsOnConfig, dependsOnVersion, dependsOnCeVersion, negativeDiffCountersToZero, systemCm, defaultPostponeTime);

//		addPostRefreshTrendGraphs(); // This needs to be done later... after the 'guiController' has been assigned (which is done in the CmSummary's constructor)
	}

	public static final String GRAPH_NAME_CM_REFRESH_TIME = "CmRefreshTime";
	
	protected void addPostRefreshTrendGraphs()
	{
		addTrendGraph(GRAPH_NAME_CM_REFRESH_TIME,
			"Counter Model Refresh Time in milliseconds", 	                                // Menu CheckBox text
			"Counter Model Refresh Time in milliseconds (Summary)", // Label 
			TrendGraphDataPoint.createGraphProps(TrendGraphDataPoint.Y_AXIS_SCALE_LABELS_MILLISEC, CentralPersistReader.SampleType.MAX_OVER_SAMPLES),
			null, 
			LabelType.Dynamic,
			TrendGraphDataPoint.Category.OTHER,
			false, // is Percent Graph
			false, // visible at start
			0,     // graph is valid from Server Version. 0 = All Versions; >0 = Valid from this version and above 
			-1);   // minimum height
	}

	public void postAllRefreshUpdateGraphData(LinkedHashMap<String, CountersModel> refreshedCms)
	{
		TrendGraphDataPoint cmRefrshTimeTgdp = getTrendGraphData(GRAPH_NAME_CM_REFRESH_TIME);

		if (cmRefrshTimeTgdp != null)
		{
			// Should we use 'refreshedCms' or should we try to figure out some other way (meaning: some CM's are not always refreshed due to postpone, and should they still be reported...) 
			// We could "remember" all CM names that has ever been refreshed and add 0 for the ones that was NOT in the "refreshedCms"... But then we need to clear that cache on "disconnect"
			// For the moment: just go with "refreshedCms" if that is passed...
			
			Collection<CountersModel> cmList = refreshedCms != null ? refreshedCms.values() : getCounterController().getCmList();

			// get number of "slots" we should allocate for: 'data' and 'labels'
			int slotCount = 0;
			for (CountersModel cm : cmList)
			{
				if ( cm == null )
					continue;

				if ( ! cm.isActive() )
					continue;
				
//				if ( ! cm.refresh() )
//					continue;
				
				slotCount++;
			}

			
			Double[] data  = new Double[ slotCount ];
			String[] label = new String[ slotCount ];

			int i = 0;
			for (CountersModel cm : cmList)
			{
				if ( cm == null )
					continue;

				if ( ! cm.isActive() )
					continue;

				long refreshTimeMs = cm.getSqlRefreshTime() + cm.getLcRefreshTime();

				label[i] = cm.getName();
				data [i] = new Double(refreshTimeMs);

				i++;
			}
			
			// Set the values
			cmRefrshTimeTgdp.setDataPoint(this.getTimestamp(), label, data);
		}
	}
	

	//-------------------------------------------------------------------------
	//-- Below is stuff for: sendAlarmRequest()
	//-------------------------------------------------------------------------
	
	/** simple member to save the DBMS version string, so we can check for changes... NOTE: this should be cleared from: reset() */
	private String _lastVersionString = ""; 
	
	@Override
	public void reset()
	{
		super.reset();
		_lastVersionString = "";
	}

	/** Alarm property name */
	public static final String  PROPKEY_alarm_DbmsVersionStringChanged = "CmSummary.alarm.system.if.dbmsVersionString.changed";
	public static final boolean DEFAULT_alarm_DbmsVersionStringChanged = true;

	/**
	 * 
	 * @param colName   Name of the column that holds the DBMS Version String
	 */
	protected void doAlarmIfDbmsVersionStringWasChanged(String colName)
	{
		if (StringUtil.isNullOrBlank(colName))
			throw new RuntimeException("doAlarmIfDbmsVersionStringWasChanged(): colName='" + colName + "'. It cant be empty...");

		if (isSystemAlarmsForColumnEnabledAndInTimeRange("DbmsVersionStringChanged"))
		{
			boolean isCheckEnabled = Configuration.getCombinedConfiguration().getBooleanProperty(PROPKEY_alarm_DbmsVersionStringChanged, DEFAULT_alarm_DbmsVersionStringChanged);
			if (isCheckEnabled)
			{
				String currentVersionString = getAbsString(0, colName, true, "");
				String tmpLastVersionString = _lastVersionString;

				boolean doAlarm = false;
				if (StringUtil.hasValue(currentVersionString) && StringUtil.hasValue(_lastVersionString))
				{
					doAlarm = currentVersionString.equals(_lastVersionString);
					_lastVersionString = currentVersionString;
				}

				if (doAlarm)
				{
					AlarmEvent ae = new AlarmEventDbmsVersionString(this, tmpLastVersionString, currentVersionString);
					AlarmHandler.getInstance().addAlarm( ae );
				}
			}
		}
	}

	/**
	 * Add "DbmsVersionStringChanged" alarm to the list of known alarms
	 * 
	 * @param list        The list to which we want to add it to (in null, then a new List will be created and returned, hence the caller needs to read the returned list)
	 * @param colName     Name of the column name where 'DBMS Version String' is located
	 * @return
	 */
	public List<CmSettingsHelper> addAlarmSettings_DbmsVersionStringChanged(List<CmSettingsHelper> list, String colName)
	{
		Configuration conf = Configuration.getCombinedConfiguration();
		// If the input list is NOT passed, then create a new one (note the caller needs to read the *returned* list...
		if (list == null)
			list = new ArrayList<>();
		
		list.add(new CmSettingsHelper("DbmsVersionStringChanged", CmSettingsHelper.Type.IS_ALARM_SWITCH, PROPKEY_alarm_DbmsVersionStringChanged, Boolean.class, conf.getBooleanProperty(PROPKEY_alarm_DbmsVersionStringChanged, DEFAULT_alarm_DbmsVersionStringChanged), DEFAULT_alarm_DbmsVersionStringChanged, "If '" + colName + "' string was changed, then send 'AlarmEventDbmsVersionString'." ));

		return list;
	}
}
