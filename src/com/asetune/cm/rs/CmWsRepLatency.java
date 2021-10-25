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
package com.asetune.cm.rs;

import java.io.File;
import java.sql.Connection;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import javax.naming.NameNotFoundException;

import org.apache.log4j.Logger;

import com.asetune.ICounterController;
import com.asetune.IGuiController;
import com.asetune.RsTune;
import com.asetune.alarm.AlarmHandler;
import com.asetune.alarm.events.AlarmEvent;
import com.asetune.alarm.events.rs.AlarmEventRsMissingLogicalConnection;
import com.asetune.alarm.events.rs.AlarmEventRsReplicationAge;
import com.asetune.alarm.events.rs.AlarmEventRsWsState;
import com.asetune.cm.CmSettingsHelper;
import com.asetune.cm.CmSettingsHelper.RegExpInputValidator;
import com.asetune.cm.CounterSample;
import com.asetune.cm.CounterSetTemplates;
import com.asetune.cm.CounterSetTemplates.Type;
import com.asetune.cm.CountersModel;
import com.asetune.cm.rs.gui.CmWsRepLatencyPanel;
import com.asetune.config.dict.MonTablesDictionary;
import com.asetune.config.dict.MonTablesDictionaryManager;
import com.asetune.graph.TrendGraphDataPoint;
import com.asetune.graph.TrendGraphDataPoint.LabelType;
import com.asetune.gui.MainFrame;
import com.asetune.gui.TabularCntrPanel;
import com.asetune.utils.Configuration;
import com.asetune.utils.StringUtil;

/**
 * @author Goran Schwarz (goran_schwarz@hotmail.com)
 */
public class CmWsRepLatency
extends CountersModel
{
	private static Logger        _logger          = Logger.getLogger(CmWsRepLatency.class);
	private static final long    serialVersionUID = 1L;

	public static final String   CM_NAME          = CmWsRepLatency.class.getSimpleName();
	public static final String   SHORT_NAME       = "WarmStandby Latency";
	public static final String   HTML_DESC        = 
		"<html>" +
		"<p>Warm Standby Latency: How long does it take to replicate a record from Active to Standby</p>" +
		"</html>";

	public static final String   GROUP_NAME       = MainFrame.TCP_GROUP_SERVER;
	public static final String   GUI_ICON_FILE    = "images/"+CM_NAME+".png";

	public static final long     NEED_SRV_VERSION = 0;
	public static final long     NEED_CE_VERSION  = 0;

	public static final String[] MON_TABLES       = new String[] {"logical_status"};
	public static final String[] NEED_ROLES       = new String[] {};
	public static final String[] NEED_CONFIG      = new String[] {};

	public static final String[] PCT_COLUMNS      = new String[] {};
	public static final String[] DIFF_COLUMNS     = new String[] {};

	public static final boolean  NEGATIVE_DIFF_COUNTERS_TO_ZERO = false;
	public static final boolean  IS_SYSTEM_CM                   = true;
	public static final int      DEFAULT_POSTPONE_TIME          = 300;
	public static final int      DEFAULT_QUERY_TIMEOUT          = CountersModel.DEFAULT_sqlQueryTimeout;;

	@Override public int     getDefaultPostponeTime()                 { return DEFAULT_POSTPONE_TIME; }
	@Override public int     getDefaultQueryTimeout()                 { return DEFAULT_QUERY_TIMEOUT; }
	@Override public boolean getDefaultIsNegativeDiffCountersToZero() { return NEGATIVE_DIFF_COUNTERS_TO_ZERO; }
	@Override public Type    getTemplateLevel()                       { return Type.ALL; }

	/**
	 * FACTORY  method to create the object
	 */
	public static CountersModel create(ICounterController counterController, IGuiController guiController)
	{
		if (guiController != null && guiController.hasGUI())
			guiController.splashWindowProgress("Loading: Counter Model '"+CM_NAME+"'");

		return new CmWsRepLatency(counterController, guiController);
	}

	public CmWsRepLatency(ICounterController counterController, IGuiController guiController)
	{
		super(counterController,
				CM_NAME, GROUP_NAME, /*sql*/null, /*pkList*/null, 
				DIFF_COLUMNS, PCT_COLUMNS, MON_TABLES, 
				NEED_ROLES, NEED_CONFIG, NEED_SRV_VERSION, NEED_CE_VERSION, 
				NEGATIVE_DIFF_COUNTERS_TO_ZERO, IS_SYSTEM_CM, DEFAULT_POSTPONE_TIME);

		setDisplayName(SHORT_NAME);
		setDescription(HTML_DESC);

		setIconFile(GUI_ICON_FILE);

		setCounterController(counterController);
		setGuiController(guiController);
		
		addTrendGraphs();
		
		CounterSetTemplates.register(this);
	}

	//------------------------------------------------------------
	// Implementation
	//------------------------------------------------------------
	private static final String  PROP_PREFIX                       = CM_NAME;

	public static final String  PROPKEY_update_active              = PROP_PREFIX + ".update.active";
	public static final boolean DEFAULT_update_active              = true;
	
	public static final String  PROPKEY_update_activeIntervalInSec = PROP_PREFIX + ".update.active.intervalInSec";
	public static final long    DEFAULT_update_activeIntervalInSec = 300;
	
	public static final String  PROPKEY_update_maxWaitTimeMs       = PROP_PREFIX + ".update.maxWaitTimeMs";
	public static final long    DEFAULT_update_maxWaitTimeMs       = 500;

	// GRAPHS
	public static final String GRAPH_NAME_LATENCY_IN_SEC   = "LatencyInSec";
	public static final String GRAPH_NAME_APPLY_AGE_IN_SEC = "ApplyAgeInSec";
	public static final String GRAPH_NAME_DATA_AGE_IN_SEC  = "DataAgeInSec";

	private void addTrendGraphs()
	{
		addTrendGraph(GRAPH_NAME_LATENCY_IN_SEC,
			"Latency In Seconds, from Active->Standby", // Menu CheckBox text
			"Latency In Seconds, from Active->Standby", // Label 
			TrendGraphDataPoint.Y_AXIS_SCALE_LABELS_SECONDS,
			null, 
			LabelType.Dynamic,
			TrendGraphDataPoint.Category.OTHER,
			false, // is Percent Graph
			false, // visible at start
			0,     // graph is valid from Server Version. 0 = All Versions; >0 = Valid from this version and above 
			-1);   // minimum height

		addTrendGraph(GRAPH_NAME_APPLY_AGE_IN_SEC,
			"Apply Age In Seconds, from Active->Standby", // Menu CheckBox text
			"Apply Age In Seconds, from Active->Standby", // Label 
			TrendGraphDataPoint.Y_AXIS_SCALE_LABELS_SECONDS,
			null, 
			LabelType.Dynamic,
			TrendGraphDataPoint.Category.OTHER,
			false, // is Percent Graph
			false, // visible at start
			0,     // graph is valid from Server Version. 0 = All Versions; >0 = Valid from this version and above 
			-1);   // minimum height

		addTrendGraph(GRAPH_NAME_DATA_AGE_IN_SEC,
			"Data Age In Seconds, from Active->Standby", // Menu CheckBox text
			"Data Age In Seconds, from Active->Standby", // Label 
			TrendGraphDataPoint.Y_AXIS_SCALE_LABELS_SECONDS,
			null, 
			LabelType.Dynamic,
			TrendGraphDataPoint.Category.OTHER,
			false, // is Percent Graph
			false, // visible at start
			0,     // graph is valid from Server Version. 0 = All Versions; >0 = Valid from this version and above 
			-1);   // minimum height
	}

	@Override
	public void updateGraphData(TrendGraphDataPoint tgdp)
	{
		if (GRAPH_NAME_LATENCY_IN_SEC.equals(tgdp.getName()))
		{
			// Write 1 "line" for record
			Double[] dArray = new Double[this.size()];
			String[] lArray = new String[dArray.length];

			for (int i = 0; i < this.size(); i++)
			{
				lArray[i] = this.getAbsString       (i, "LogicalName");
				dArray[i] = this.getAbsValueAsDouble(i, "LatencyInSec");
			}

			// Set the values
			tgdp.setDataPoint(this.getTimestamp(), lArray, dArray);
		}

		if (GRAPH_NAME_APPLY_AGE_IN_SEC.equals(tgdp.getName()))
		{
			// Write 1 "line" for record
			Double[] dArray = new Double[this.size()];
			String[] lArray = new String[dArray.length];

			for (int i = 0; i < this.size(); i++)
			{
				lArray[i] = this.getAbsString       (i, "LogicalName");
				dArray[i] = this.getAbsValueAsDouble(i, "ApplyAgeInSec");
			}

			// Set the values
			tgdp.setDataPoint(this.getTimestamp(), lArray, dArray);
		}

		if (GRAPH_NAME_DATA_AGE_IN_SEC.equals(tgdp.getName()))
		{
			// Write 1 "line" for record
			Double[] dArray = new Double[this.size()];
			String[] lArray = new String[dArray.length];

			for (int i = 0; i < this.size(); i++)
			{
				lArray[i] = this.getAbsString       (i, "LogicalName");
				dArray[i] = this.getAbsValueAsDouble(i, "DataAgeInSec");
			}

			// Set the values
			tgdp.setDataPoint(this.getTimestamp(), lArray, dArray);
		}
	}
	
	@Override
	protected TabularCntrPanel createGui()
	{
		return new CmWsRepLatencyPanel(this);
	}


	@Override
	public void addMonTableDictForVersion(Connection conn, long srvVersion, boolean isClusterEnabled)
	{
		try 
		{
			MonTablesDictionary mtd = MonTablesDictionaryManager.getInstance();
			mtd.addTable("logical_status",  "");

			mtd.addColumn("logical_status", "LogicalId",           "<html>ID for the logical connection</html>");
			mtd.addColumn("logical_status", "LogicalName",         "<html>Name of the logical connection</html>");

			mtd.addColumn("logical_status", "ActiveId",            "<html>ID of the Active Connection</html>");
			mtd.addColumn("logical_status", "ActiveName",          "<html>Name of the Active Connection</html>");
			mtd.addColumn("logical_status", "ActiveState",         "<html>Status of the Active Connection</html>");

			mtd.addColumn("logical_status", "StandbyId",           "<html>ID of the Standby Connection</html>");
			mtd.addColumn("logical_status", "StandbyName",         "<html>Name of the Standby Connection</html>");
			mtd.addColumn("logical_status", "StandbyState",        "<html>Status of the Standby Connection</html>");
			
			mtd.addColumn("logical_status", "LatencyInSec",        "<html> <b>Algorithm:</b> datediff(ss,     origin_time, dest_commit_time)  from rs_lastcommit </html>");
			mtd.addColumn("logical_status", "ApplyAgeInSec",       "<html> <b>Algorithm:</b> datediff(ss,     dest_commit_time, getdate())    from rs_lastcommit </html>");
			mtd.addColumn("logical_status", "ApplyAgeInMinutes",   "<html> <b>Algorithm:</b> datediff(minute, dest_commit_time, getdate())    from rs_lastcommit </html>");
			mtd.addColumn("logical_status", "DataAgeInSec",        "<html> <b>Algorithm:</b> datediff(ss,     origin_time, getdate())         from rs_lastcommit </html>");
			mtd.addColumn("logical_status", "DataAgeInMinutes",    "<html> <b>Algorithm:</b> datediff(minute, origin_time, getdate())         from rs_lastcommit </html>");
			mtd.addColumn("logical_status", "OriginCommitTime",    "<html> <b>Algorithm:</b> origin_time                                      from rs_lastcommit </html>");
			mtd.addColumn("logical_status", "DestCommitTime",      "<html> <b>Algorithm:</b> dest_commit_time                                 from rs_lastcommit </html>");
			mtd.addColumn("logical_status", "ActiveLocalTime",     "<html> <b>Algorithm:</b> getdate() <br> <b>Note:</b> This is only updated if/when we issue a dummy update on the Active Side, otherwise it will be NULL.</html>");
			mtd.addColumn("logical_status", "StandbyLocalTime",    "<html> <b>Algorithm:</b> getdate() </html>");
			mtd.addColumn("logical_status", "StandbyMsg",          "<html> Some information why some columns for this logical connection is missing.<br>Might be 'problems' connecting to the Standby DBMS or a 'faulty' status. </html>");
			
			mtd.addColumn("logical_status", "RsId",                "<html>ID of the Controlling Replication Server</html>");
			mtd.addColumn("logical_status", "RsName",              "<html>Name of the Controlling Replication Server</html>");
			
			mtd.addColumn("logical_status", "OpInProgress",        "<html>A description of the operation in progress. Can be None, Switch Active, or Create Standby.</html>");
			mtd.addColumn("logical_status", "StateOfOpInProgress", "<html>The current step in the operation.</html>");
			mtd.addColumn("logical_status", "Spid",                "<html>The process ID for the server thread that is executing the operation.</html>");
		}
		catch (NameNotFoundException e) {/*ignore*/}
	}

	@Override
	protected void registerDefaultValues()
	{
		super.registerDefaultValues();

		Configuration.registerDefaultValue(PROPKEY_update_active,              DEFAULT_update_active);
		Configuration.registerDefaultValue(PROPKEY_update_activeIntervalInSec, DEFAULT_update_activeIntervalInSec);
		Configuration.registerDefaultValue(PROPKEY_update_maxWaitTimeMs,       DEFAULT_update_maxWaitTimeMs);
	}
	
	/** Used by the: Create 'Offline Session' Wizard */
	@Override
	public List<CmSettingsHelper> getLocalSettings()
	{
		Configuration conf = Configuration.getCombinedConfiguration();
		List<CmSettingsHelper> list = new ArrayList<>();
		
		list.add(new CmSettingsHelper("Update Active DB",                 PROPKEY_update_active              , Boolean.class, conf.getBooleanProperty(PROPKEY_update_active               , DEFAULT_update_active               ), DEFAULT_update_active,              "Update Active DB" ));
		list.add(new CmSettingsHelper("Update Active DB Interval",        PROPKEY_update_activeIntervalInSec , Long   .class, conf.getLongProperty   (PROPKEY_update_activeIntervalInSec  , DEFAULT_update_activeIntervalInSec  ), DEFAULT_update_activeIntervalInSec, "Update Active DB, Every X second." ));
		list.add(new CmSettingsHelper("Sleep Time Between Update/Select", PROPKEY_update_maxWaitTimeMs       , Long   .class, conf.getLongProperty   (PROPKEY_update_maxWaitTimeMs        , DEFAULT_update_maxWaitTimeMs        ), DEFAULT_update_maxWaitTimeMs,       "Sleep X milliseconds between the update and the select (so data have time to replicate)" ));

		return list;
	}


	@Override
	public List<String> getPkForVersion(Connection conn, long srvVersion, boolean isClusterEnabled)
	{
//		List <String> pkCols = new LinkedList<String>();
//
//		pkCols.add("Partition");
//
//		return pkCols;
		return null;
	}

	/**
	 * Create a special CounterSample, that will iterate over all databases that we will interrogate
	 */
	@Override
	public CounterSample createCounterSample(String name, boolean negativeDiffCountersToZero, String[] diffColumns, CounterSample prevSample)
	{
		return new CounterSampleWsIterator(name, negativeDiffCountersToZero, diffColumns, prevSample);
	}

	@Override
	public String getSqlForVersion(Connection conn, long srvVersion, boolean isClusterEnabled)
	{
		// The SQL ins actually in: CounterSampleWsIterator
		String sql = 
				"-- Sorry, this is hardcoded in the Performance Counter \n" +
				"-- But here is what it does: \n" +
				"-- - admin logical_status: \n" +
				"--   for each LogicalConnection: \n" +
				"--   - ACTIVE  SIDE: delete/insert into table 'rsTune_ws_dummy_update' (if not exists it will be created) \n" +
				"--   - STANDBY SIDE: select ... origin_time, dest_commit_time, getdate() from rs_lastcommit where origin = ${activeConnId} \n" +
				"";
		return sql;
	}
	
	
	//--------------------------------------------------------------
	// Alarm handling
	//--------------------------------------------------------------
	@Override
	public void sendAlarmRequest()
	{
		if ( ! hasAbsData() )
			return;
		
		if ( ! AlarmHandler.hasInstance() )
			return;

		CountersModel cm = this;

		boolean debugPrint = Configuration.getCombinedConfiguration().getBooleanProperty("sendAlarmRequest.debug", _logger.isDebugEnabled());

		//-------------------------------------------------------
		// not in MandatoryLogicalConnections
		//-------------------------------------------------------
		if (isSystemAlarmsForColumnEnabledAndInTimeRange("MandatoryLogicalConnections"))
		{
			// Check if we should reset saved '_mandatoryLogicalConnections'
			String tmpDir = System.getProperty("java.io.tmpdir");
			File f = new File(RESET_LOGICAL_CONNECTION_FILE_NAME.replace("${tmpdir}", tmpDir));
			if (f.exists())
			{
				_logger.info("Resetting Saved Mandatory Logical Connections Set, due to: Found file '" + f + "', which will be removed.");
				_mandatoryLogicalConnections = null;
				f.delete();
			}

			// Add all LogicalName's to a set of "connections that exists"
			// This set will later on be checked for existence against PROPKEY_alarm_MandatoryLogicalConnections
			// New connections will automatically be added added to the "mandatory set", if they are missing at at next check an alarm will be raised!
			Set<String> thisSampleLogicalConnections = new HashSet<>();
			if (_mandatoryLogicalConnections == null)
				_mandatoryLogicalConnections = new LinkedHashSet<>();

			for (int r=0; r<cm.getAbsRowCount(); r++)
			{
				String lName = cm.getAbsString(r, "LogicalName");

				if (StringUtil.isNullOrBlank(lName))
					continue;
				
				thisSampleLogicalConnections.add(lName);
				_mandatoryLogicalConnections.add(lName);
			}

// SOME TIMES it seems that getAbsRowCount() is 0 ... when the B node is DOWN ... 
// But I don't understand... since a "dummy" row is still added in CounterSampleWsIterator.getSample(...) catch(StandbyErrors) _rows.add(createSkipRow(wsEntry, standbyMsg));  
if (debugPrint || _logger.isDebugEnabled())
{
	_logger.debug("##### sendAlarmRequest("+cm.getName()+"): getAbsRowCount()=" + cm.getAbsRowCount() + ", thisSampleLogicalConnections=" + thisSampleLogicalConnections);
	System.out.println("##### sendAlarmRequest("+cm.getName()+"): getAbsRowCount()=" + cm.getAbsRowCount() + ", thisSampleLogicalConnections=" + thisSampleLogicalConnections);
}

			// Get the configuration
			String configMandatoryLogicalConnections = Configuration.getCombinedConfiguration().getProperty(PROPKEY_alarm_MandatoryLogicalConnections,  DEFAULT_alarm_MandatoryLogicalConnections);
			
			if (debugPrint || _logger.isDebugEnabled())
				System.out.println("##### sendAlarmRequest("+cm.getName()+"): MandatoryLogicalConnections -- configMandatoryLogicalConnections='"+configMandatoryLogicalConnections+"', thisSampleLogicalConnections="+thisSampleLogicalConnections+", _mandatoryLogicalConnections="+_mandatoryLogicalConnections+".");


			// Check ALL previous existing Logical Connections (since RsTune was started) 
			Set<String> mandatoryLogicalConnections = _mandatoryLogicalConnections;
			// Or what's in the Config file...
			if ( ! configMandatoryLogicalConnections.equals(DEFAULT_alarm_MandatoryLogicalConnections) )
				mandatoryLogicalConnections = StringUtil.commaStrToSet(configMandatoryLogicalConnections, true);

			// Now loop any of the 2 above: previously-existing-connections  OR  values-from-the-config-file
			for (String mandatoryLogicalName : mandatoryLogicalConnections)
			{
				if (StringUtil.isNullOrBlank(mandatoryLogicalName))
					continue;

				if ( ! thisSampleLogicalConnections.contains(mandatoryLogicalName) )
				{
					AlarmEvent alarm = new AlarmEventRsMissingLogicalConnection(cm, mandatoryLogicalName, thisSampleLogicalConnections, mandatoryLogicalConnections, configMandatoryLogicalConnections);
					AlarmHandler.getInstance().addAlarm(alarm);
				}
			}
		}

		// Loop all rows
		for (int r=0; r<cm.getAbsRowCount(); r++)
		{
			String lName = cm.getAbsString(r, "LogicalName");
			
			// Skip rows where LogicalName is "blank" (not sure how this happens, but it does...)
			if (StringUtil.isNullOrBlank(lName))
				continue;

			//-------------------------------------------------------
			// not in status 'Active/' (ActiveState & StandbyState)
			//-------------------------------------------------------
			if (isSystemAlarmsForColumnEnabledAndInTimeRange("ActiveState"))
			{
				String state = cm.getAbsString(r, "ActiveState");
				if (state != null)
				{
					String regexp = Configuration.getCombinedConfiguration().getProperty(PROPKEY_alarm_ActiveState,  DEFAULT_alarm_ActiveState);

					if (debugPrint || _logger.isDebugEnabled())
						System.out.println("##### sendAlarmRequest("+cm.getName()+"): ActiveState -- regexp='"+regexp+"', state='"+state+"'.");

					if ( ! state.matches(regexp) ) // default is 'Active/'
						AlarmHandler.getInstance().addAlarm( new AlarmEventRsWsState(cm, lName, "ActiveState", state, "", regexp) );
				}
			}

			if (isSystemAlarmsForColumnEnabledAndInTimeRange("StandbyState"))
			{
				String state      = cm.getAbsString(r, "StandbyState");
				String standbyMsg = cm.getAbsString(r, "StandbyMsg");
				
				if (state != null)
				{
					String regexp = Configuration.getCombinedConfiguration().getProperty(PROPKEY_alarm_StandbyState,  DEFAULT_alarm_StandbyState);

					if (debugPrint || _logger.isDebugEnabled())
						System.out.println("##### sendAlarmRequest("+cm.getName()+"): StandbyState -- regexp='"+regexp+"', state='"+state+"'.");

					if ( (! state.matches(regexp)) || StringUtil.hasValue(standbyMsg)) // default is 'Active/'
						AlarmHandler.getInstance().addAlarm( new AlarmEventRsWsState(cm, lName, "StandbyState", state, standbyMsg, regexp) );
				}
			}

			
			//-------------------------------------------------------
			// ApplyAgeInMinutes
			//-------------------------------------------------------
			if (isSystemAlarmsForColumnEnabledAndInTimeRange("ApplyAgeInMinutes"))
			{
				Double ApplyAgeInMinutes = cm.getAbsValueAsDouble(r, "ApplyAgeInMinutes");
				if (ApplyAgeInMinutes != null)
				{
					int threshold = Configuration.getCombinedConfiguration().getIntProperty(PROPKEY_alarm_ApplyAgeInMinutes, DEFAULT_alarm_ApplyAgeInMinutes);

					if (debugPrint || _logger.isDebugEnabled())
						System.out.println("##### sendAlarmRequest("+cm.getName()+"): threshold="+threshold+", lName='"+lName+"', ApplyAgeInMinutes='"+ApplyAgeInMinutes+"'.");

					if (ApplyAgeInMinutes.intValue() > threshold)
					{
						// Get config 'skip some transaction names'
						String keepLogicalNameRegExp = Configuration.getCombinedConfiguration().getProperty(PROPKEY_alarm_ApplyAgeInMinutes_ForLogicalName,  DEFAULT_alarm_ApplyAgeInMinutes_ForLogicalName);
						String skipLogicalNameRegExp = Configuration.getCombinedConfiguration().getProperty(PROPKEY_alarm_ApplyAgeInMinutes_SkipLogicalName, DEFAULT_alarm_ApplyAgeInMinutes_SkipLogicalName);

						// The below could have been done with neasted if(keep-db), if(keep-srv), if(!skipDb), if(!skipSrv) doAlarm=true; 
						// Below is more readable, from a variable context point-of-view, but HARDER to understand
						boolean doAlarm = true; // note: this must be set to true at start, otherwise all below rules will be disabled (it "stops" processing at first doAlarm==false)
						doAlarm = (doAlarm && (StringUtil.isNullOrBlank(keepLogicalNameRegExp) ||   lName.matches(keepLogicalNameRegExp ))); //     matches the KEEP regexp
						doAlarm = (doAlarm && (StringUtil.isNullOrBlank(skipLogicalNameRegExp) || ! lName.matches(skipLogicalNameRegExp)));  // NO match in the SKIP regexp

						// NO match in the SKIP regexp
						if (doAlarm)
						{
							AlarmHandler.getInstance().addAlarm( new AlarmEventRsReplicationAge(cm, threshold, lName, "ApplyAgeInMinutes", ApplyAgeInMinutes.intValue()) );
						}
					}
				}
			}

			//-------------------------------------------------------
			// DataAgeInMinutes
			//-------------------------------------------------------
			if (isSystemAlarmsForColumnEnabledAndInTimeRange("DataAgeInMinutes"))
			{
				Double DataAgeInMinutes = cm.getAbsValueAsDouble(r, "DataAgeInMinutes");
				if (DataAgeInMinutes != null)
				{
					int threshold = Configuration.getCombinedConfiguration().getIntProperty(PROPKEY_alarm_DataAgeInMinutes, DEFAULT_alarm_DataAgeInMinutes);

					if (debugPrint || _logger.isDebugEnabled())
						System.out.println("##### sendAlarmRequest("+cm.getName()+"): threshold="+threshold+", lName='"+lName+"', DataAgeInMinutes='"+DataAgeInMinutes+"'.");

					if (DataAgeInMinutes.intValue() > threshold)
					{
						// Get config 'skip some transaction names'
						String keepLogicalNameRegExp = Configuration.getCombinedConfiguration().getProperty(PROPKEY_alarm_DataAgeInMinutes_ForLogicalName,  DEFAULT_alarm_DataAgeInMinutes_ForLogicalName);
						String skipLogicalNameRegExp = Configuration.getCombinedConfiguration().getProperty(PROPKEY_alarm_DataAgeInMinutes_SkipLogicalName, DEFAULT_alarm_DataAgeInMinutes_SkipLogicalName);

						// The below could have been done with neasted if(keep-db), if(keep-srv), if(!skipDb), if(!skipSrv) doAlarm=true; 
						// Below is more readable, from a variable context point-of-view, but HARDER to understand
						boolean doAlarm = true; // note: this must be set to true at start, otherwise all below rules will be disabled (it "stops" processing at first doAlarm==false)
						doAlarm = (doAlarm && (StringUtil.isNullOrBlank(keepLogicalNameRegExp) ||   lName.matches(keepLogicalNameRegExp ))); //    matches the KEEP regexp
						doAlarm = (doAlarm && (StringUtil.isNullOrBlank(skipLogicalNameRegExp) || ! lName.matches(skipLogicalNameRegExp))); // NO match in the SKIP regexp

						// NO match in the SKIP regexp
						if (doAlarm)
						{
							AlarmHandler.getInstance().addAlarm( new AlarmEventRsReplicationAge(cm, threshold, lName, "DataAgeInMinutes", DataAgeInMinutes.intValue()) );
						}
					}
				}
			}
		} // end: loop all rows
	} // end: method

	public static final String  PROPKEY_alarm_MandatoryLogicalConnections       = CM_NAME + ".alarm.system.MandatoryLogicalConnections.list";
	public static final String  DEFAULT_alarm_MandatoryLogicalConnections       = "ALL"; // Remember every logical connection... if any are dropped: raise alarm

	public static final String  PROPKEY_alarm_ActiveState                       = CM_NAME + ".alarm.system.if.ActiveState.ne";
	public static final String  DEFAULT_alarm_ActiveState                       = "Active/";
	
	public static final String  PROPKEY_alarm_StandbyState                      = CM_NAME + ".alarm.system.if.StandbyState.ne";
	public static final String  DEFAULT_alarm_StandbyState                      = "Active/";
	

	public static final String  PROPKEY_alarm_ApplyAgeInMinutes                 = CM_NAME + ".alarm.system.if.ApplyAgeInMinutes.gt";
	public static final int     DEFAULT_alarm_ApplyAgeInMinutes                 = 20;
	public static final String  PROPKEY_alarm_ApplyAgeInMinutes_ForLogicalName  = CM_NAME + ".alarm.system.if.ApplyAgeInMinutes.for.logicalName";
	public static final String  DEFAULT_alarm_ApplyAgeInMinutes_ForLogicalName  = "";
	public static final String  PROPKEY_alarm_ApplyAgeInMinutes_SkipLogicalName = CM_NAME + ".alarm.system.if.ApplyAgeInMinutes.skip.logicalName";
	public static final String  DEFAULT_alarm_ApplyAgeInMinutes_SkipLogicalName = "";

	public static final String  PROPKEY_alarm_DataAgeInMinutes                  = CM_NAME + ".alarm.system.if.DataAgeInMinutes.gt";
	public static final int     DEFAULT_alarm_DataAgeInMinutes                  = 60;
	public static final String  PROPKEY_alarm_DataAgeInMinutes_ForLogicalName   = CM_NAME + ".alarm.system.if.DataAgeInMinutes.for.logicalName";
	public static final String  DEFAULT_alarm_DataAgeInMinutes_ForLogicalName   = "";
	public static final String  PROPKEY_alarm_DataAgeInMinutes_SkipLogicalName  = CM_NAME + ".alarm.system.if.DataAgeInMinutes.skip.logicalName";
	public static final String  DEFAULT_alarm_DataAgeInMinutes_SkipLogicalName  = "";

//	public static final String  PROPKEY_alarm_OldestTranInSecondsSkipTranName   = CM_NAME + ".alarm.system.if.OldestTranInSeconds.skip.tranName";
//	public static final String  DEFAULT_alarm_OldestTranInSecondsSkipTranName   = "^(DUMP |\\$dmpxact).*";
	
	// Class members that has to do with Alarm Handling
	private Set<String> _mandatoryLogicalConnections;
	private static final String RESET_LOGICAL_CONNECTION_FILE_NAME = "${tmpdir}/RsTune.reset.saved.MandatoryLogicalConnections";

	@Override
	public List<CmSettingsHelper> getLocalAlarmSettings()
	{
		Configuration conf = Configuration.getCombinedConfiguration();
		List<CmSettingsHelper> list = new ArrayList<>();

		CmSettingsHelper.Type isAlarmSwitch = CmSettingsHelper.Type.IS_ALARM_SWITCH;

		list.add(new CmSettingsHelper("MandatoryLogicalConnections",       isAlarmSwitch, PROPKEY_alarm_MandatoryLogicalConnections      , String.class,  conf.getProperty   (PROPKEY_alarm_MandatoryLogicalConnections      , DEFAULT_alarm_MandatoryLogicalConnections      ), DEFAULT_alarm_MandatoryLogicalConnections      , "Logical Connection names that MUST be present, if not send 'AlarmEventRsMissingLogicalConnection'. This can be a comma separated list of Logical Connection Names. If config is '" + DEFAULT_alarm_MandatoryLogicalConnections + "', it means: Remember ALL present logical connections. if any are dropped: raise alarm. To reset this 'in-memory' list, restart " + RsTune.APP_NAME + " (or create the file '" + RESET_LOGICAL_CONNECTION_FILE_NAME + "'). "));
		
		list.add(new CmSettingsHelper("ActiveState",                       isAlarmSwitch, PROPKEY_alarm_ActiveState                      , String.class,  conf.getProperty   (PROPKEY_alarm_ActiveState                      , DEFAULT_alarm_ActiveState                      ), DEFAULT_alarm_ActiveState                      , "If 'ActiveState' does NOT match the regexp send 'AlarmEventRsWsState'.",  new RegExpInputValidator()));
		list.add(new CmSettingsHelper("StandbyState",                      isAlarmSwitch, PROPKEY_alarm_StandbyState                     , String.class,  conf.getProperty   (PROPKEY_alarm_StandbyState                     , DEFAULT_alarm_StandbyState                     ), DEFAULT_alarm_StandbyState                     , "If 'StandbyState' does NOT match the regexp send 'AlarmEventRsWsState'.", new RegExpInputValidator()));

		list.add(new CmSettingsHelper("ApplyAgeInMinutes",                 isAlarmSwitch, PROPKEY_alarm_ApplyAgeInMinutes                , Integer.class, conf.getIntProperty(PROPKEY_alarm_ApplyAgeInMinutes                , DEFAULT_alarm_ApplyAgeInMinutes                ), DEFAULT_alarm_ApplyAgeInMinutes                , "If 'ApplyAgeInMinutes' is greater than ## then send 'AlarmEventRsReplicationAge'." ));
		list.add(new CmSettingsHelper("ApplyAgeInMinutes ForLogicalName",                 PROPKEY_alarm_ApplyAgeInMinutes_ForLogicalName , String .class, conf.getProperty   (PROPKEY_alarm_ApplyAgeInMinutes_ForLogicalName , DEFAULT_alarm_ApplyAgeInMinutes_ForLogicalName ), DEFAULT_alarm_ApplyAgeInMinutes_ForLogicalName , "If 'ApplyAgeInMinutes' is true; Only for the LogicalName listed (regexp is used, blank=for-all). After this rule the 'skip' rule is evaluated.", new RegExpInputValidator()));
		list.add(new CmSettingsHelper("ApplyAgeInMinutes SkipLogicalName",                PROPKEY_alarm_ApplyAgeInMinutes_SkipLogicalName, String .class, conf.getProperty   (PROPKEY_alarm_ApplyAgeInMinutes_SkipLogicalName, DEFAULT_alarm_ApplyAgeInMinutes_SkipLogicalName), DEFAULT_alarm_ApplyAgeInMinutes_SkipLogicalName, "If 'ApplyAgeInMinutes' is true; Discard LogicalName listed (regexp is used). Before this rule the 'for/keep' rule is evaluated",                 new RegExpInputValidator()));

		list.add(new CmSettingsHelper("DataAgeInMinutes",                  isAlarmSwitch, PROPKEY_alarm_DataAgeInMinutes                 , Integer.class, conf.getIntProperty(PROPKEY_alarm_DataAgeInMinutes                 , DEFAULT_alarm_DataAgeInMinutes                 ), DEFAULT_alarm_DataAgeInMinutes                 , "If 'DataAgeInMinutes' is greater than ## then send 'AlarmEventRsReplicationAge'." ));
		list.add(new CmSettingsHelper("DataAgeInMinutes ForLogicalName",                  PROPKEY_alarm_DataAgeInMinutes_ForLogicalName  , String .class, conf.getProperty   (PROPKEY_alarm_DataAgeInMinutes_ForLogicalName  , DEFAULT_alarm_DataAgeInMinutes_ForLogicalName  ), DEFAULT_alarm_DataAgeInMinutes_ForLogicalName  , "If 'DataAgeInMinutes' is true; Only for the LogicalName listed (regexp is used, blank=for-all). After this rule the 'skip' rule is evaluated.", new RegExpInputValidator()));
		list.add(new CmSettingsHelper("DataAgeInMinutes SkipLogicalName",                 PROPKEY_alarm_DataAgeInMinutes_SkipLogicalName , String .class, conf.getProperty   (PROPKEY_alarm_DataAgeInMinutes_SkipLogicalName , DEFAULT_alarm_DataAgeInMinutes_SkipLogicalName ), DEFAULT_alarm_DataAgeInMinutes_SkipLogicalName , "If 'DataAgeInMinutes' is true; Discard LogicalName listed (regexp is used). Before this rule the 'for/keep' rule is evaluated",                 new RegExpInputValidator()));

		return list;
	}
	
}
