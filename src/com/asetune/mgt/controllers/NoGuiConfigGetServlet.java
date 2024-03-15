/*******************************************************************************
 * Copyright (C) 2010-2020 Goran Schwarz
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
package com.asetune.mgt.controllers;

import java.io.IOException;
import java.io.StringWriter;
import java.lang.invoke.MethodHandles;
import java.util.Base64;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

import javax.servlet.ServletException;
import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.log4j.Logger;

import com.asetune.CounterController;
import com.asetune.alarm.ui.config.AlarmWritersTableModel;
import com.asetune.cm.CmSettingsHelper;
import com.asetune.cm.CountersModel;
import com.asetune.utils.Configuration;
import com.asetune.utils.CronUtils;
import com.asetune.utils.StringUtil;
import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.util.DefaultPrettyPrinter;
import com.fasterxml.jackson.databind.ObjectMapper;

public class NoGuiConfigGetServlet 
extends HttpServlet
//extends AbstractHandler
{
	private static final long serialVersionUID = 1L;
	private static final Logger _logger = Logger.getLogger(MethodHandles.lookup().lookupClass());

	@Override
	protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException
	{
		ServletOutputStream out = resp.getOutputStream();
//		resp.setContentType("text/html");
//		resp.setCharacterEncoding("UTF-8");
		resp.setContentType("application/json");
		resp.setCharacterEncoding("UTF-8");
		resp.setHeader("Access-Control-Allow-Origin", "*");

		// Check for known input parameters
//		if (Helper.hasUnKnownParameters(req, resp, "sessionName", "srv", "srvName",    "cmName", "graphName", "startTime", "endTime", "sampleType", "sampleValue"))
//			return;

//		String sessionName    = Helper.getParameter(req, new String[] {"sessionName", "srv", "srvName"} );
//		String cmName         = Helper.getParameter(req, "cmName");
//		String graphName      = Helper.getParameter(req, "graphName");
//		String startTime      = Helper.getParameter(req, "startTime");
//		String endTime        = Helper.getParameter(req, "endTime");
//		String sampleTypeStr  = Helper.getParameter(req, "sampleType");
//		String sampleValueStr = Helper.getParameter(req, "sampleValue");

System.out.println();
System.out.println("XXXXXXXXXXXXXXXXXXXXXXXXXXXXXX: getContextPath()=" + req.getContextPath());
System.out.println("XXXXXXXXXXXXXXXXXXXXXXXXXXXXXX: getPathInfo   ()=" + req.getPathInfo());
System.out.println("XXXXXXXXXXXXXXXXXXXXXXXXXXXXXX: getQueryString()=" + req.getQueryString());
System.out.println("XXXXXXXXXXXXXXXXXXXXXXXXXXXXXX: getParameterMap()=" + req.getParameterMap());

for(String header : Collections.list(req.getHeaderNames()))
	System.out.println("HEADER |" + header + "| = |" + req.getHeader(header) + "|.");

final String auth = req.getHeader("Authorization");
System.out.println(">>>>>>>>>>>> NoGuiConfigGetServlet.GET.HEADER: Authorization = |" + auth + "|.");
if (auth != null)
{
	int firstSpace = auth.indexOf(" ");
	String authMethod = auth.substring(0, firstSpace).trim();
	String authToken  = auth.substring(firstSpace).trim();
	
	if ("Basic".equals(authMethod))
	{
		byte[] decodedBytes = Base64.getDecoder().decode(authToken);
		authToken = new String(decodedBytes);
	}
	
//	String decoded = new String( Base64.getDecoder().decode( auth.substring(firstSpace) ), StandardCharsets.UTF_8 );
	System.out.println(">>>>>>>>>>>> NoGuiConfigGetServlet.GET.HEADER: Authorization.method = |" + authMethod + "|.");
	System.out.println(">>>>>>>>>>>> NoGuiConfigGetServlet.GET.HEADER: Authorization.token  = |" + authToken  + "|.");
}

		String payload = "";

		StringWriter sw = new StringWriter();

		JsonFactory jfactory = new JsonFactory();
		JsonGenerator gen = jfactory.createGenerator(sw);
		gen.setPrettyPrinter(new DefaultPrettyPrinter());
		gen.setCodec(new ObjectMapper(jfactory));

		
//ae._icon                 = cm.getTabPanel() == null ? null : cm.getTabPanel().getIcon();
//ae._tabName              = cm.getDisplayName();
//ae._cmName               = cm.getName();
//ae._groupName            = cm.getGroupName();
//ae._isEnabled            = cm.isAlarmEnabled();
//ae._hasSystem            = cm.hasSystemAlarms();
//ae._isSystemEnabled      = cm.isSystemAlarmsEnabled();
//ae._hasUserDefined       = cm.hasUserDefinedAlarmInterrogator();
//ae._isUserDefinedEnabled = cm.isUserDefinedAlarmsEnabled();
//ae._alarmInterrogator    = cm.getUserDefinedAlarmInterrogator();

		gen.writeStartObject(); // OUTER Object
		gen.writeStringField("srvName"        , CounterController.getInstance().getServerName());
		gen.writeStringField("srvDbmsName"    , CounterController.getInstance().getDbmsServerName());
		gen.writeStringField("srvAliasName"   , CounterController.getInstance().getServerAliasName());
		gen.writeStringField("srvDisplayName" , CounterController.getInstance().getServerDisplayName());
//		gen.writeStringField("srvTimeCmd"     , CounterController.getInstance().getServerTimeCmd());
		//NOTE: Should we add more server info here...

		gen.writeFieldName("cmList");
		gen.writeStartArray();
		
		List<CountersModel> cmList = CounterController.getInstance().getCmList();
		for (CountersModel cm : cmList)
		{
			List<CmSettingsHelper> settingsList = null;

			gen.writeStartObject(); // CM
			gen.writeStringField("cmName"        , cm.getName());
			gen.writeStringField("displayName"   , cm.getDisplayName());
			gen.writeStringField("groupName"     , cm.getGroupName());

			gen.writeObjectField("isCmEnabled"    , cm.isActive());

			gen.writeObjectField("isAlarmEnabled"                 , cm.isAlarmEnabled());
			gen.writeObjectField("hasSystemAlarms"                , cm.hasSystemAlarms());
			gen.writeObjectField("isSystemAlarmsEnabled"          , cm.isSystemAlarmsEnabled());
			gen.writeObjectField("hasUserDefinedAlarmInterrogator", cm.hasUserDefinedAlarmInterrogator());
			gen.writeObjectField("isUserDefinedAlarmsEnabled"     , cm.isUserDefinedAlarmsEnabled());
			
			gen.writeObjectField("pkCols"        , cm.getPk());
			gen.writeObjectField("diffCols"      , cm.getDiffColumns());
			gen.writeObjectField("pctCols"       , cm.getPctColumns());
			gen.writeObjectField("needSrvConfig" , cm.getDependsOnConfig());
			gen.writeObjectField("needSrvRoles"  , cm.getDependsOnRole());
			gen.writeStringField("needSrvVersion", cm.getDependsOnVersionStr());
			gen.writeObjectField("dependsOnCm"   , cm.getDependsOnCm());
			gen.writeObjectField("sqlInit"       , cm.getSqlInit());
			gen.writeObjectField("sqlClose"      , cm.getSqlClose());
			gen.writeObjectField("sqlRefresh"    , cm.getSql());
System.out.println("----------: CM name()               = |" + cm.getName() + ", displayName=|" + cm.getDisplayName() + "|.");

//			// OBJECT: options
//			gen.writeFieldName("options");
//			gen.writeStartObject();
////			gen.writeBooleanField("isBgPollingEnabled"   , cm.isBackgroundDataPollingEnabled());
//			gen.writeBooleanField("isPaused"             , cm.isDataPollingPaused());
//			gen.writeNumberField ("postponeTime"         , cm.getPostponeTime());
//			gen.writeNumberField ("queryTimeout"         , cm.getQueryTimeout());
//			gen.writeBooleanField("pcsSaveCounters"      , cm.isPersistCountersEnabled());
//			gen.writeBooleanField("pcsSaveAbs"           , cm.isPersistCountersAbsEnabled());
//			gen.writeBooleanField("pcsSaveDiff"          , cm.isPersistCountersDiffEnabled());
//			gen.writeBooleanField("pcsSaveRate"          , cm.isPersistCountersRateEnabled());
//			gen.writeBooleanField("isAlarmsEnabled"      , cm.isAlarmEnabled());
//			gen.writeBooleanField("isSystemAlarmsEnabled", cm.isSystemAlarmsEnabled());
//			gen.writeEndObject();

			// OBJECT: options
			settingsList = cm.getCollectorOptions();
			if (settingsList != null && !settingsList.isEmpty())
			{
				gen.writeFieldName("options");
				gen.writeStartArray();
				for (CmSettingsHelper settings : settingsList)
				{
					gen.writeStartObject();
					gen.writeStringField ("name"          , settings.getName());
					gen.writeStringField ("datatype"      , settings.getDataTypeString());
					gen.writeStringField ("property"      , settings.getPropName());
					gen.writeStringField ("value"         , settings.getStringValue());
					gen.writeStringField ("defaultValue"  , settings.getDefaultValue());
					gen.writeBooleanField("isDefaultValue", settings.isDefaultValue());
					gen.writeStringField ("description"   , StringUtil.stripHtmlStartEnd(settings.getDescription()));
					gen.writeEndObject();
				}
				gen.writeEndArray();
			}

			
			// OBJECT: settings (localSettings)
			settingsList = cm.getLocalSettings();
			if (settingsList != null && !settingsList.isEmpty())
			{
				gen.writeFieldName("settings");
				gen.writeStartArray();
				for (CmSettingsHelper settings : settingsList)
				{
					gen.writeStartObject();
					gen.writeStringField ("name"          , settings.getName());
					gen.writeStringField ("datatype"      , settings.getDataTypeString());
					gen.writeStringField ("property"      , settings.getPropName());
					gen.writeStringField ("value"         , settings.getStringValue());
					gen.writeStringField ("defaultValue"  , settings.getDefaultValue());
					gen.writeBooleanField("isDefaultValue", settings.isDefaultValue());
					gen.writeStringField ("description"   , StringUtil.stripHtmlStartEnd(settings.getDescription()));
					gen.writeEndObject();
				}
				gen.writeEndArray();
			}

			// OBJECT: alarmSettings
			settingsList = cm.getLocalAlarmSettings();
			if (settingsList != null && !settingsList.isEmpty())
			{
				List<CmSettingsHelper> onlyPreChecks     = settingsList.stream().filter(e -> e.isPreCheck()   ).collect(Collectors.toList());
				List<CmSettingsHelper> onlyAlarmSwitches = settingsList.stream().filter(e -> e.isAlarmSwitch()).collect(Collectors.toList());

System.out.println(" - ALARM: onlyPreChecks.size()    =" + onlyPreChecks.size());
System.out.println(" - ALARM: onlyAlarmSwitches.size()=" + onlyAlarmSwitches.size());

				gen.writeFieldName("alarmSettings");
				gen.writeStartObject();
				
				// OBJECT: preChecks
				if ( ! onlyPreChecks.isEmpty() )
				{
					gen.writeFieldName("preChecks");
					gen.writeStartArray();

					for (CmSettingsHelper entry : onlyPreChecks)
					{
						gen.writeStartObject();
						gen.writeStringField ("name"          , entry.getName());
						gen.writeStringField ("datatype"      , entry.getDataTypeString());
						gen.writeStringField ("property"      , entry.getPropName());
						gen.writeStringField ("value"         , entry.getStringValue());
						gen.writeStringField ("defaultValue"  , entry.getDefaultValue());
						gen.writeBooleanField("isDefaultValue", entry.isDefaultValue());
						gen.writeStringField ("description"   , StringUtil.stripHtmlStartEnd(entry.getDescription()));
						gen.writeStringField ("validatorName" , entry.getInputValidatorClassName());
						gen.writeEndObject();
					}
					
					gen.writeEndArray();
				}

				// OBJECT: alarms
				if ( ! onlyAlarmSwitches.isEmpty() )
				{
					gen.writeFieldName("alarms");
					gen.writeStartArray();

					for (CmSettingsHelper entry : onlyAlarmSwitches)
					{
						String colname = entry.getName();
						String cronStr = Configuration.getCombinedConfiguration().getProperty(cm.replaceCmAndColName(CountersModel.PROPKEY_ALARM_isSystemAlarmsForColumnInTimeRange, colname), CountersModel.DEFAULT_ALARM_isSystemAlarmsForColumnInTimeRange);
						String cronStrDesc = "";

						// Check that the 'cron' sceduling pattern is valid...
						String cronPatStr = cronStr;
						if (cronPatStr.startsWith("!"))
						{
							cronPatStr = cronPatStr.substring(1);
							cronStrDesc += "NOT within: ";
						}
//						if ( ! SchedulingPattern.validate(cronPatStr) )
//							_logger.error("The cron scheduling pattern '"+cronPatStr+"' is NOT VALID. for the property '"+replaceCmAndColName(PROPKEY_ALARM_isSystemAlarmsForColumnInTimeRange, colname)+"', this will not be used at runtime, furter warnings will also be issued.");

						cronStrDesc += CronUtils.getCronExpressionDescriptionForAlarms(cronPatStr);
						
						
						gen.writeStartObject();
						gen.writeStringField ("name"          , colname);
						gen.writeBooleanField("isAlarmEnabled", cm.isSystemAlarmsForColumnEnabled(colname));
						gen.writeStringField ("timeRangeCron" , cronStr);
						gen.writeStringField ("timeRangeDescrption" , cronStrDesc);
						gen.writeStringField ("description"   , StringUtil.stripHtmlStartEnd(entry.getDescription()));

						
						// Insert "extra" settings: 'isAlarmEnabled', 'timeRangeCron'
						insertExtraAlarmSettings(cm, colname, settingsList);
						
						// ARRAY: parameters
						getAlarmParametersFor(gen, entry, settingsList);

						gen.writeEndObject();
					}
					
					gen.writeEndArray(); // end: parameters
				}
				
				gen.writeEndObject(); // end: alarmSettings
			}
				
//... INFO - CountersModel  - System Defined Alarms properties are listed below for 'CmActiveStatements'.
//... INFO - CountersModel  -        CmActiveStatements.alarm.enabled = true
//... INFO - CountersModel  -        CmActiveStatements.alarm.system.enabled = true
//... INFO - CountersModel  -        CmActiveStatements.alarm.system.enabled.ImBlockingOthersMaxTimeInSec = true
//... INFO - CountersModel  -        CmActiveStatements.alarm.system.enabled.ImBlockingOthersMaxTimeInSec.timeRange.cron = * * * * *   #-- desc: Alarms is allowed 'any-time'.
//... INFO - CountersModel  -        CmActiveStatements.alarm.system.enabled.HoldingLocksWhileWaitForClientInputInSec = true
//... INFO - CountersModel  -        CmActiveStatements.alarm.system.enabled.HoldingLocksWhileWaitForClientInputInSec.timeRange.cron = * * * * *   #-- desc: Alarms is allowed 'any-time'.
//... INFO - CountersModel  -        CmActiveStatements.alarm.system.enabled.TempdbUsageMb = true
//... INFO - CountersModel  -        CmActiveStatements.alarm.system.enabled.TempdbUsageMb.timeRange.cron = * * * * *   #-- desc: Alarms is allowed 'any-time'.
//... INFO - CountersModel  -        CmActiveStatements.alarm.system.if.ImBlockingOthersMaxTimeInSec.gt = 60
//... INFO - CountersModel  -        CmActiveStatements.alarm.system.if.HoldingLocksWhileWaitForClientInputInSec.gt = 180
//... INFO - CountersModel  -        CmActiveStatements.alarm.system.if.HoldingLocksWhileWaitForClientInputInSec.exclusiveLocksOnly = true
//... INFO - CountersModel  -        CmActiveStatements.alarm.system.if.TempdbUsageMb.gt = 16384

//    /**
//     * Print how any alarms are configured
//     */
//    public void printSystemAlarmConfig()
//    {
//    	List<CmSettingsHelper> alarms = getLocalAlarmSettings();
//    	if (alarms.isEmpty())
//    	{
//    		_logger.info("System Defined Alarms are NOT enabled for '"+getName()+"'.");
//    		return;
//    	}
//    
//    	Configuration conf = Configuration.getCombinedConfiguration();
//    	
//    	
//    	boolean isAlarmEnabled        = isAlarmEnabled();
//    	boolean isSystemAlarmsEnabled = isSystemAlarmsEnabled();
//    	String  prefix = "       ";
//    	
//    	_logger.info("System Defined Alarms properties are listed below for '"+getName()+"'.");
//    
//    	_logger.info(prefix + replaceCmName(PROPKEY_ALARM_isAlarmsEnabled) + " = " + isAlarmEnabled + (isAlarmEnabled ? "" : "        ##### NOTE: all Alarms are DISABLED, so below propertirs wont be used."));
//    
//    	_logger.info(prefix + replaceCmName(PROPKEY_ALARM_isSystemAlarmsEnabled) + " = " + isSystemAlarmsEnabled + (isSystemAlarmsEnabled ? "" : "        ##### NOTE: System Alarms are DISABLED, so below propertirs wont be used."));
//    	
//    	for (CmSettingsHelper sh : alarms)
//    	{
//    		String colname = sh.getName();
//    		
//    		// <CMNAME>.alarm.system.enabled.<COLNAME>
//    		// Only for names that do not contain ' ' spaces... FIXME: This is UGGLY, create a type/property which describes if we should write the '*.enable.*' property or not...
//    		if ( sh.isAlarmSwitch() )
//    		{
//    			String propName = replaceCmAndColName(getName(), PROPKEY_ALARM_isSystemAlarmsForColumnEnabled, colname);
//    			String propVal  = conf.getProperty(propName, DEFAULT_ALARM_isSystemAlarmsForColumnEnabled+"");
//    
//    			// At what times can the alarms be triggered
//    			String cronProp = replaceCmAndColName(PROPKEY_ALARM_isSystemAlarmsForColumnInTimeRange, colname);
//    			String cronPat  = Configuration.getCombinedConfiguration().getProperty(cronProp, DEFAULT_ALARM_isSystemAlarmsForColumnInTimeRange);
//    
//    			// Check that the 'cron' sceduling pattern is valid...
//    			String cronPatStr = cronPat;
//    			if (cronPatStr.startsWith("!"))
//    			{
//    				cronPatStr = cronPatStr.substring(1);
//    			}
//    			if ( ! SchedulingPattern.validate(cronPatStr) )
//    				_logger.error("The cron scheduling pattern '"+cronPatStr+"' is NOT VALID. for the property '"+replaceCmAndColName(PROPKEY_ALARM_isSystemAlarmsForColumnInTimeRange, colname)+"', this will not be used at runtime, furter warnings will also be issued.");
//    
//    			String cronPatDesc = CronUtils.getCronExpressionDescriptionForAlarms(cronPat);
//    
//    			_logger.info(prefix + propName + " = " + propVal);
//    			_logger.info(prefix + cronProp + " = " + cronPat + "   #-- desc: " + cronPatDesc);
//    			
//    		}
//    	}
//    	
//    	for (CmSettingsHelper sh : alarms)
//    	{
//    		// Various properties defined by the CounterModel
//    		// probably looks like: <CMNAME>.alarm.system.if.<COLNAME>.gt
//    		String propName = sh.getPropName();
//    		String propVal  = conf.getProperty(propName, sh.getDefaultValue());
//    
//    		_logger.info(prefix + propName + " = " + propVal);
//    	}
//    }

			
			// Enabled Graphs ???
			
			// Normal Options ???

			gen.writeEndObject(); // end: CM
		}

		gen.writeEndArray(); // end: cmList


		
		//----------------------------------
		// Start: alarmWriters
		// alarmWriters: 
		// [
		//     {
		//         isActive: true|false,
		//         name: "...",
		//         className: "...",
		//         description: "...",
		//         settings:
		//         [
		//             {
		//                 isSelected: true|false,
		//                 isMandatory: true|false,
		//                 name: "...",
		//                 datatype: "...",
		//                 property: "...",
		//                 value: "...",
		//                 defaultValue: "...",
		//                 isDefaultValue: "...",
		//                 description: "...",
		//                 validatorName: "..."
		//             },{
		//                 ...
		//             }
		//         ],
		//         filters:
		//         [
		//             {
		//                 isSelected: true|false,
		//                 name: "...",
		//                 datatype: "...",
		//                 property: "...",
		//                 value: "...",
		//                 defaultValue: "...",
		//                 isDefaultValue: "...",
		//                 description: "...",
		//                 validatorName: "..."
		//             },{
		//                 ...
		//             }
		//         ]
		//     },{
		//         ...
		//     }
		// ]
		gen.writeFieldName("alarmWriters");
		gen.writeStartArray(); // START: array: alarmWriters[]
		
		AlarmWritersTableModel alarmWritersTableModel = new AlarmWritersTableModel();
		alarmWritersTableModel.refreshTable();

		for (int r = 0; r < alarmWritersTableModel.getRowCount(); r++)
		{
			AlarmWritersTableModel.AlarmWriterEntry awe = alarmWritersTableModel.getWriterEntryForRow(r);

			gen.writeStartObject(); // BEGIN object: under alarmWriters[]
			
			gen.writeBooleanField("isActive"   , awe.isSelected());
			gen.writeStringField ("name"       , awe.getName());
			gen.writeStringField ("className"  , awe.getClassName());
			gen.writeStringField ("description", awe.getDescription());

			// SETTINGS
			List<CmSettingsHelper> settings = awe.getSettings();
			if (settings != null && !settings.isEmpty())
			{
				gen.writeFieldName("settings");
				gen.writeStartArray(); // START: array: settings[]
				for (CmSettingsHelper entry : settings)
				{
					gen.writeStartObject(); // BEGIN: object under settings[]
					gen.writeBooleanField("isSelected"    , entry.isSelected());
					gen.writeBooleanField("isMandatory"   , entry.isMandatory());
					gen.writeStringField ("name"          , entry.getName());
					gen.writeStringField ("datatype"      , entry.getDataTypeString());
					gen.writeStringField ("property"      , entry.getPropName());
					gen.writeStringField ("value"         , entry.getStringValue());
					gen.writeStringField ("defaultValue"  , entry.getDefaultValue());
					gen.writeBooleanField("isDefaultValue", entry.isDefaultValue());
					gen.writeStringField ("description"   , StringUtil.stripHtmlStartEnd(entry.getDescription()));
					gen.writeStringField ("validatorName" , entry.getInputValidatorClassName());
					gen.writeEndObject(); // END: object under settings[]
				}
				gen.writeEndArray(); // END: array: settings[]
			}

			// FILTERS
			List<CmSettingsHelper> filters  = awe.getFilters();
			if (filters != null && !filters.isEmpty())
			{
				gen.writeFieldName("filters");
				gen.writeStartArray(); // START: array: filters[]
				for (CmSettingsHelper entry : filters)
				{
					gen.writeStartObject(); // BEGIN: object under filters[]
					gen.writeBooleanField("isSelected"    , entry.isSelected());
					gen.writeStringField ("name"          , entry.getName());
					gen.writeStringField ("datatype"      , entry.getDataTypeString());
					gen.writeStringField ("property"      , entry.getPropName());
					gen.writeStringField ("value"         , entry.getStringValue());
					gen.writeStringField ("defaultValue"  , entry.getDefaultValue());
					gen.writeBooleanField("isDefaultValue", entry.isDefaultValue());
					gen.writeStringField ("description"   , StringUtil.stripHtmlStartEnd(entry.getDescription()));
					gen.writeStringField ("validatorName" , entry.getInputValidatorClassName());
					gen.writeEndObject(); // END: object under filters[]
				}
				gen.writeEndArray(); // END: array: filters[]
			}

			gen.writeEndObject(); // END: object: under alarmWriters[]
		}

		gen.writeEndArray(); // END: array: alarmWriters[]


		//----------------------------------
		gen.writeFieldName("dailySummaryReport");
//		gen.writeStartArray(); // START: array: alarmWriters[]
		gen.writeStartObject(); // BEGIN object
		gen.writeStringField("xxx", "dummy x");
		gen.writeStringField("yyy", "dummy y");
		gen.writeStringField("zzz", "dummy z");
		gen.writeEndObject(); // END object
//		gen.writeEndArray(); // END: array: alarmWriters[]


		//----------------------------------
		gen.writeEndObject(); // end: SERVER

		gen.close();

		// Generate PAYLOAD
		payload = sw.toString();
		out.println(payload);
		
		out.flush();
		out.close();
	}

	// TODO: Possibly MOVE this to "somewhere", so we can add it using... getLocalAlarmSettings()
	//       1 or 2 static methods (in CmSettingsHelper) which creates
	//       OR: Rewrite in some way that when we call getLocalAlarmSettings(...IS_ALARM_SWITCH...) it automatically adds it...
	private void insertExtraAlarmSettings(CountersModel cm, String colname, List<CmSettingsHelper> alarmSettingsList)
	{
		// Add it first in the list...
		alarmSettingsList.add(0, CmSettingsHelper.create_isAlarmEnabled(cm, colname));
		alarmSettingsList.add(1, CmSettingsHelper.create_timeRangeCron(cm, colname));
	}

	private void getAlarmParametersFor(JsonGenerator gen, CmSettingsHelper alarmSwitch, List<CmSettingsHelper> settingsList)
	throws IOException
	{
		String colname = alarmSwitch.getName();
		String searchFor = colname + " ";
		
		// Write parameters
		gen.writeFieldName("parameters");
		gen.writeStartArray();

		// Write the "main" parameter, which is "attached" to this alarm
		getAlarmParametersFor(gen, alarmSwitch);

		// Then write every "extra parameters", which might be used for "filtering/refine" the alarm
		for (CmSettingsHelper settings : settingsList)
		{
			String keyname = settings.getName();

			if (keyname.startsWith(searchFor) && !settings.isAlarmSwitch())
			{
				getAlarmParametersFor(gen, settings);
			}
		}
		gen.writeEndArray();
	}
	private void getAlarmParametersFor(JsonGenerator gen, CmSettingsHelper entry)
	throws IOException
	{
		gen.writeStartObject();
		gen.writeStringField ("name"          , entry.getName());
		gen.writeBooleanField("isMainParam"   , entry.isAlarmSwitch());
		gen.writeStringField ("datatype"      , entry.getDataTypeString());
		gen.writeStringField ("property"      , entry.getPropName());
		gen.writeStringField ("value"         , entry.getStringValue());
		gen.writeStringField ("defaultValue"  , entry.getDefaultValue());
		gen.writeBooleanField("isDefaultValue", entry.isDefaultValue());
		gen.writeStringField ("description"   , StringUtil.stripHtmlStartEnd(entry.getDescription()));
		gen.writeStringField ("validatorName" , entry.getInputValidatorClassName());
		gen.writeEndObject();
	}
}
