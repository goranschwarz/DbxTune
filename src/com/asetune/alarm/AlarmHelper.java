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
package com.asetune.alarm;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.log4j.Logger;

import com.asetune.alarm.events.AlarmEvent;
import com.asetune.alarm.events.AlarmEventMissingMandatoryContent;
import com.asetune.cm.CmSettingsHelper;
import com.asetune.cm.CountersModel;
import com.asetune.utils.Configuration;
import com.asetune.utils.StringUtil;

public class AlarmHelper
{
	private static Logger _logger = Logger.getLogger(AlarmHelper.class);

	public static void sendAlarmRequestForColumn(CountersModel cm, String colName)
	{
		if ( cm == null )
			throw new IllegalArgumentException("Passed CountersModel can't be null");

		if ( ! cm.hasAbsData() )
			return;
		
		if ( ! AlarmHandler.hasInstance() )
			return;

		// EXIT EARLY: no value
		if (StringUtil.isNullOrBlank(colName))
			return;

		// EXIT EARLY: Column name not found
		int pos_colName = cm.findColumn(colName);
		if (pos_colName == -1)
		{
			_logger.info("Unable to get desired clumn name '"+colName+"' in cm='"+cm.getName()+"'.");
			return;
		}

		// EXIT EARLY if no alarm properties has been specified (since there can be *many* logins)
		//
		// The isSystemAlarmsForColumnEnabledAndInTimeRange() depends on the following 2 properties
		//            CountersModel.PROPKEY_ALARM_isSystemAlarmsForColumnEnabled     = "<CMNAME>.alarm.system.enabled.<COLNAME>";
		//            CountersModel.PROPKEY_ALARM_isSystemAlarmsForColumnInTimeRange = "<CMNAME>.alarm.system.enabled.<COLNAME>.timeRange.cron";
		//
		// Thats why we add "mandatory.contentList.forColName." to the beginning of the columnName
		//            this         .PROPKEY_alarm_MandatoryColNameContent            = "<CM_NAME>.alarm.system.mandatory.contentList.forColName.<COL_NAME>";

		if ( ! cm.isSystemAlarmsForColumnEnabledAndInTimeRange("mandatory.contentList.forColName."+colName) ) 
			return;

		// Get a list of values we want the column to contain
		List<String> contentList = StringUtil.commaStrToList(Configuration.getCombinedConfiguration().getProperty(fixCmName(cm, colName, PROPKEY_alarm_MandatoryColNameContent), DEFAULT_alarm_MandatoryColNameContent), true);

		// EXIT EARLY if no content was provided
		if (contentList.size() == 0)
			return;

		AlarmHandler alarmHandler = AlarmHandler.getInstance();

		// keep a count for every distinct value in the column
		Map<String, Integer> colContentCountMap   = new HashMap<>();

		// Count 'colName' instances
		for (int r=0; r<cm.getAbsRowCount(); r++)
		{
			// Get value and increment Value Counter
			String appName = StringUtil.fixNull(cm.getAbsString(r, pos_colName), "");
			colContentCountMap.put(appName, colContentCountMap.getOrDefault(appName, 0) + 1 );
		}

		//-------------------------------------------------------
		// Distinct value count in colName
		//-------------------------------------------------------
//		List<String> contentList = StringUtil.commaStrToList(Configuration.getCombinedConfiguration().getProperty(fixCmName(cm, colName, PROPKEY_alarm_MandatoryColNameContent), DEFAULT_alarm_MandatoryColNameContent), true);

		for (String name : contentList)
		{
			if (StringUtil.isNullOrBlank(name))
				continue;

			int count = colContentCountMap.getOrDefault(name, 0);
			
			if (count == 0)
			{
				AlarmEvent ae = new AlarmEventMissingMandatoryContent(cm, colName, name);
				alarmHandler.addAlarm( ae );
			}
		}
	}

	public static final String  PROPKEY_alarm_MandatoryColNameContent = "<CM_NAME>.alarm.system.mandatory.contentList.forColName.<COL_NAME>";
	public static final String  DEFAULT_alarm_MandatoryColNameContent = "";

	public static String fixCmName(CountersModel cm, String colName, String propkey)
	{
		return propkey.replace("<CM_NAME>", cm.getName()).replace("<COL_NAME>", colName);
	}
	
	public static List<CmSettingsHelper> getLocalAlarmSettingsForColumn(CountersModel cm, String colName)
	{
		Configuration conf = Configuration.getCombinedConfiguration();
		List<CmSettingsHelper> list = new ArrayList<>();
		
		if (StringUtil.hasValue(colName)) 
		{
			list.add(new CmSettingsHelper(
					"Mandatory-ColumnNameContent-List",   
					fixCmName(cm, colName, PROPKEY_alarm_MandatoryColNameContent), 
					String.class, 
					conf.getProperty(fixCmName(cm, colName, PROPKEY_alarm_MandatoryColNameContent), DEFAULT_alarm_MandatoryColNameContent), 
					DEFAULT_alarm_MandatoryColNameContent, 
					"If any of the specified 'names/values' in the list is missing from the column '"+colName+"' then send alarm 'AlarmEventMissingMandatoryContent'." 
				));
		}

		return list;
	}
}

//------------------------------------------------------------------------
// The below might be possible to use if we want to have a more generic approach
// Meaning: Count "any" column content
// I want for a more simple solution for now... but when we want a more generic solution this might be a start
//------------------------------------------------------------------------
//    private static class LoginInfoCheckEntry
//    {
//    	String _jsonStr; // original JSON description... possibly null if not parsed
//    	String _colName;
//    	String _colContent;
//    	int    _operator; 
//    	int    _operatorValue; 
//    
//    	int    _occurrences;
//    }
//    
//    private List<LoginInfoCheckEntry> _list_alarm_loginInfoGeneric;  // Note: do NOT initialize this here... since the initAlarms() is done in super, if initialized it will be overwritten here...
//    
//    /**
//     * Initialize stuff that has to do with alarms
//     */
//    @Override
//    public void initAlarms()
//    {
//    	Configuration conf = Configuration.getCombinedConfiguration();
//    	String cfgVal;
//    
//    	_list_alarm_loginInfoGeneric = new ArrayList();
//    
//    	//--------------------------------------
//    	// Login Info Generic
//    	cfgVal = conf.getProperty(PROPKEY_alarm_LoginInfo, DEFAULT_alarm_LoginInfo);
//    	if (StringUtil.hasValue(cfgVal))
//    	{
//    		_list_alarm_loginInfoGeneric = parseLoginInfoGeneric(cfgVal);
//    	}
//    }
//    /** 
//     * Parse the JSON string:
//     * <pre>
//     * [ 
//     *     {"colName":"program_name", "colValue":"BlockingLocks-check",       "operation":"lt", "value":1},
//     *     {"colName":"program_name", "colValue":"StatementCacheUsage-check", "operation":"lt", "value":1}
//     * ]
//     * </pre>
//     */
//    private List<LoginInfoCheckEntry> parseLoginInfoGeneric(String cfgVal)
//    {
//    	List<LoginInfoCheckEntry> list = new ArrayList();
//    	return list;
//    }
//------------------------------------------------------------------------
