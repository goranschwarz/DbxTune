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
package com.dbxtune.cm.rs;

import java.lang.invoke.MethodHandles;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import javax.naming.NameNotFoundException;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.dbxtune.ICounterController;
import com.dbxtune.IGuiController;
import com.dbxtune.cm.CmSettingsHelper;
import com.dbxtune.cm.CmSybMessageHandler;
import com.dbxtune.cm.CounterSample;
import com.dbxtune.cm.CounterSetTemplates;
import com.dbxtune.cm.CounterSetTemplates.Type;
import com.dbxtune.cm.CountersModel;
import com.dbxtune.cm.DbxTuneResultSetMetaData;
import com.dbxtune.cm.rs.gui.CmAdminStatsPanel;
import com.dbxtune.config.dict.MonTablesDictionary;
import com.dbxtune.config.dict.MonTablesDictionaryManager;
import com.dbxtune.gui.MainFrameRs;
import com.dbxtune.gui.TabularCntrPanel;
import com.dbxtune.sql.conn.DbxConnection;
import com.dbxtune.sql.conn.info.DbmsVersionInfo;
import com.dbxtune.utils.Configuration;
import com.dbxtune.utils.Ver;

/**
 * @author Goran Schwarz (goran_schwarz@hotmail.com)
 */
public class CmAdminStats
extends CountersModel
{
    /** Log4j logging. */
	private static final Logger _logger = LogManager.getLogger(MethodHandles.lookup().lookupClass());
	private static final long    serialVersionUID = 1L;

	public static final String   CM_NAME          = CmAdminStats.class.getSimpleName();
	public static final String   SHORT_NAME       = "RS Counters";
	public static final String   HTML_DESC        = 
		"<html>" +
		"<p>RepServer Monitor And Performance Counters</p>" +
		"Fetched using: <code>admin statistics,'ALL'</code>" +
		"<br>" +
		"<b>Note</b>: In Experimental Status (it may work)" +
		"</html>";

	public static final String   GROUP_NAME       = MainFrameRs.TCP_GROUP_MC;
	public static final String   GUI_ICON_FILE    = "images/"+CM_NAME+".png";

	public static final long     NEED_SRV_VERSION = 0;
	public static final long     NEED_CE_VERSION  = 0;

	public static final String[] MON_TABLES       = new String[] {"stats"};
	public static final String[] NEED_ROLES       = new String[] {};
	public static final String[] NEED_CONFIG      = new String[] {};

	public static final String[] PCT_COLUMNS      = new String[] {};
	public static final String[] DIFF_COLUMNS     = new String[] {
		 "Obs"
		,"Total"
		,"Last"
		,"Max"
//		,"AvgTtlObs"
//		,"RateXsec"
		};

	public static final boolean  NEGATIVE_DIFF_COUNTERS_TO_ZERO = false;
	public static final boolean  IS_SYSTEM_CM                   = true;
	public static final int      DEFAULT_POSTPONE_TIME          = 0;
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

		return new CmAdminStats(counterController, guiController);
	}

	public CmAdminStats(ICounterController counterController, IGuiController guiController)
	{
		super(counterController, guiController,
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

	public static final String  PROPKEY_sample_resetAfter          = PROP_PREFIX + ".sample.reset.after";
	public static final boolean DEFAULT_sample_resetAfter          = false;

	@Override
	protected void registerDefaultValues()
	{
		super.registerDefaultValues();

		Configuration.registerDefaultValue(PROPKEY_sample_resetAfter,  DEFAULT_sample_resetAfter);
	}


	@Override
	protected CmSybMessageHandler createSybMessageHandler()
	{
		CmSybMessageHandler msgHandler = super.createSybMessageHandler();

//		msgHandler.addDiscardMsgStr("===============================================================================");
		msgHandler.addDiscardMsgNum(0);
		msgHandler.addDiscardMsgNum(15539); // Gateway connection to 'GORAN_1_ERSSD.GORAN_1_ERSSD' is created.
		msgHandler.addDiscardMsgNum(15540); // Gateway connection to 'GORAN_1_ERSSD.GORAN_1_ERSSD' is dropped.

		// RS Configuration might be changed in the "sql init" section
		msgHandler.addDiscardMsgNum(15357); // configure replication server...: Config parameter 'stats_sampling' is modified.
		msgHandler.addDiscardMsgNum(56040); // admin stats, cancel:             Failed to cancel, there is no command in progress.

		return msgHandler;
	}

	/** Used by the: Create 'Offline Session' Wizard */
	@Override
	public List<CmSettingsHelper> getLocalSettings()
	{
		Configuration conf = Configuration.getCombinedConfiguration();
		List<CmSettingsHelper> list = new ArrayList<>();
		
		list.add(new CmSettingsHelper("Clear Counters", PROPKEY_sample_resetAfter , Boolean.class, conf.getBooleanProperty(PROPKEY_sample_resetAfter  , DEFAULT_sample_resetAfter  ), DEFAULT_sample_resetAfter, CmAdminStatsPanel.TOOLTIP_sample_resetAfter ));

		return list;
	}


	private void addTrendGraphs()
	{
	}

	@Override
	protected TabularCntrPanel createGui()
	{
		return new CmAdminStatsPanel(this);
	}

	@Override
	public String[] getDependsOnConfigForVersion(DbxConnection conn, DbmsVersionInfo versionInfo)
	{
		return NEED_CONFIG;
	}

	@Override
	public void addMonTableDictForVersion(DbxConnection conn, DbmsVersionInfo versionInfo)
	{
		try 
		{
			MonTablesDictionary mtd = MonTablesDictionaryManager.getInstance();
			mtd.addTable("stats",  "");

			mtd.addColumn("stats", "Instance",       "<html>A specific occurrence of a module</html>");
			mtd.addColumn("stats", "InstanceId",     "<html>The numeric identifier for a given module instance. <br>"
			                                             + "For example, two different SQM instances may have instance IDs 102 and 103</html>");
			mtd.addColumn("stats", "ModTypeInstVal", "<html>In some cases, an instance may have multiple versions or module types. <br>"
			                                             + "For example, a given SQM instance may have an inbound type and an outbound type. <br>"
			                                             + "For SQM instances, inbound versions have a module type of 1 and outbound versions have a module type of 0</html>");
			mtd.addColumn("stats", "Type",           "<html>Monitor, Observer, or Counter - displays the name of the statistics collector being observed. For example, SleepsWriteQ</html>");
			mtd.addColumn("stats", "Name",           "<html>The name of the Counter. See Column 'CounterDescr' for a more detailed description.</html>");
			mtd.addColumn("stats", "Obs",            "<html>The number of observations of a statistics collector during an observation period</html>");
			mtd.addColumn("stats", "Total",          "<html>The sum of observed values during an observation period</html>");
			mtd.addColumn("stats", "Last",           "<html>The last value observed during an observation period.</html>");
			mtd.addColumn("stats", "Max",            "<html>The maximum value observed during an observation period.</html>");
			mtd.addColumn("stats", "AvgTtlObs",      "<html>The average value observed in an observation period. This is calculated as Total/Obs</html>");
			mtd.addColumn("stats", "RateXsec",       "<html>The change, in a period of 1 second, observed during the given observation period. <br>"
			                                             + "Observers calculate this as Obs/seconds in an observation period. <br>"
			                                             + "Monitors and counters calculate this as Total/second in an observation period</html>");
		}
		catch (NameNotFoundException e) 
		{
		//	_logger.warn("Problems in cm='" + CM_NAME + "', adding addMonTableDictForVersion. Caught: " + e); 
			System.out.println("Problems in cm='" + CM_NAME + "', adding addMonTableDictForVersion. Caught: " + e); 
		}
	}

	@Override
	public List<String> getPkForVersion(DbxConnection conn, DbmsVersionInfo versionInfo)
	{
		List <String> pkCols = new LinkedList<String>();

		pkCols.add("Instance");
		pkCols.add("InstanceId"); // We might need to add this as well... dsi_workload... might return not return unique enough values in 'Instance'
		pkCols.add("Type");
		pkCols.add("Name");

		return pkCols;
		
		//------------------------------------------------------------------------
		// Below is some examples (from web error log) where it looks strange
		//------------------------------------------------------------------------
		// NEW	53491	53491	53491	3	2015-11-18 08:48:44	2015-11-18 09:47:32	RsTune	pszyndela	not-conne	3.5.0	WARN	CounterCollectorThreadGui	com.dbxtune.cm.CounterSample	com.dbxtune.cm.CounterSample.addRow(CounterSample.java:1411)	Internal Counter Duplicate key in ResultSet for CM 'CmAdminStats', 
		// pk='[Instance, Type, Name]', a row with the key values 'AOBJ, dbo.ImageHist|MONITOR|AOBJEstRowSize|' already exists. 
		// CurrentRow='[AOBJ, dbo.ImageHist, 11705, 11, MONITOR, AOBJEstRowSize, 1, null, 204, 204, 204, null, AOBJ, 65015, 0x80='keep old (previous value of counter retained, usually to aid calculation during next observation period)', 0x400='monitor', , In terms of bytes, this is the estimated size of a row associated with this object.]'. 
		//     NewRow='[AOBJ, dbo.ImageHist, 11605, 11, MONITOR, AOBJEstRowSize, 1, null, 204, 204, 204, null, AOBJ, 65015, 0x80='keep old (previous value of counter retained, usually to aid calculation during next observation period)', 0x400='monitor', , In terms of bytes, this is the estimated size of a row associated with this object.]'.	53491	53491	
		//                                   ^^^^^
		// 
		// NEW	53491	53491	53491	4	2015-11-18 08:48:45	2015-11-18 09:47:32	RsTune	pszyndela	not-conne	3.5.0	WARN	CounterCollectorThreadGui	com.dbxtune.cm.CounterSample	com.dbxtune.cm.CounterSample.addRow(CounterSample.java:1411)	Internal Counter Duplicate key in ResultSet for CM 'CmAdminStats', 
		// pk='[Instance, Type, Name]', a row with the key values 'AOBJ, dbo.ImageHist|OBSERVER|AOBJInsertCommand2|' already exists. 
		// CurrentRow='[AOBJ, dbo.ImageHist, 11705, 11, OBSERVER, AOBJInsertCommand2, 1, null, null, null, null, 0, AOBJ, 65005, 0x04='sysmon (counter flushed as output of admin statistics, sysmon)', 0x80='keep old (previous value of counter retained, usually to aid calculation during next observation period)', 0x200='observer', , Insert command on active object.]'. 
		//     NewRow='[AOBJ, dbo.ImageHist, 11605, 11, OBSERVER, AOBJInsertCommand2, 1, null, null, null, null, 0, AOBJ, 65005, 0x04='sysmon (counter flushed as output of admin statistics, sysmon)', 0x80='keep old (previous value of counter retained, usually to aid calculation during next observation period)', 0x200='observer', , Insert command on active object.]'.	53491	53491	
		//                                   ^^^^^
		// 
		// NEW	53491	53491	53491	5	2015-11-18 08:48:51	2015-11-18 09:47:32	RsTune	pszyndela	not-conne	3.5.0	WARN	CounterCollectorThreadGui	com.dbxtune.cm.CounterSample	com.dbxtune.cm.CounterSample.addRow(CounterSample.java:1411)	Internal Counter Duplicate key in ResultSet for CM 'CmAdminStats', 
		// pk='[Instance, Type, Name]', a row with the key values 'AOBJ, dbo.Image|MONITOR|AOBJEstRowSize|' already exists. 
		// CurrentRow='[AOBJ, dbo.Image, 11705, 21, MONITOR, AOBJEstRowSize, 1, null, 249, 249, 249, null, AOBJ, 65015, 0x80='keep old (previous value of counter retained, usually to aid calculation during next observation period)', 0x400='monitor', , In terms of bytes, this is the estimated size of a row associated with this object.]'. 
		//     NewRow='[AOBJ, dbo.Image, 11605, 21, MONITOR, AOBJEstRowSize, 1, null, 249, 249, 249, null, AOBJ, 65015, 0x80='keep old (previous value of counter retained, usually to aid calculation during next observation period)', 0x400='monitor', , In terms of bytes, this is the estimated size of a row associated with this object.]'.	53491	53491	
		//                               ^^^^^
		//
		// NEW	53491	53491	53491	6	2015-11-18 08:48:52	2015-11-18 09:47:32	RsTune	pszyndela	not-conne	3.5.0	WARN	CounterCollectorThreadGui	com.dbxtune.cm.CounterSample	com.dbxtune.cm.CounterSample.addRow(CounterSample.java:1411)	Internal Counter Duplicate key in ResultSet for CM 'CmAdminStats', 
		// pk='[Instance, Type, Name]', a row with the key values 'AOBJ, dbo.Image|OBSERVER|AOBJDeleteCommand2|' already exists. 
		// CurrentRow='[AOBJ, dbo.Image, 11705, 21, OBSERVER, AOBJDeleteCommand2, 1, null, null, null, null, 0, AOBJ, 65009, 0x04='sysmon (counter flushed as output of admin statistics, sysmon)', 0x80='keep old (previous value of counter retained, usually to aid calculation during next observation period)', 0x200='observer', , Delete command on active object.]'. 
		//     NewRow='[AOBJ, dbo.Image, 11605, 21, OBSERVER, AOBJDeleteCommand2, 1, null, null, null, null, 0, AOBJ, 65009, 0x04='sysmon (counter flushed as output of admin statistics, sysmon)', 0x80='keep old (previous value of counter retained, usually to aid calculation during next observation period)', 0x200='observer', , Delete command on active object.]'.	53491	53491	
		//                               ^^^^^
		// 
		// NEW	53491	53491	53491	7	2015-11-18 08:48:45	2015-11-18 09:47:32	RsTune	pszyndela	not-conne	3.5.0	WARN	CounterCollectorThreadGui	com.dbxtune.cm.CounterSample	com.dbxtune.cm.CounterSample.addRow(CounterSample.java:1411)	Internal Counter Duplicate key in ResultSet for CM 'CmAdminStats', 
		// pk='[Instance, Type, Name]', a row with the key values 'AOBJ, dbo.NewImageRef|MONITOR|AOBJEstRowSize|' already exists. 
		// CurrentRow='[AOBJ, dbo.NewImageRef, 11705, 31, MONITOR, AOBJEstRowSize, 1, null, 30, 30, 30, null, AOBJ, 65015, 0x80='keep old (previous value of counter retained, usually to aid calculation during next observation period)', 0x400='monitor', , In terms of bytes, this is the estimated size of a row associated with this object.]'. 
		//     NewRow='[AOBJ, dbo.NewImageRef, 11605, 31, MONITOR, AOBJEstRowSize, 1, null, 30, 30, 30, null, AOBJ, 65015, 0x80='keep old (previous value of counter retained, usually to aid calculation during next observation period)', 0x400='monitor', , In terms of bytes, this is the estimated size of a row associated with this object.]'.	53491	53491	
		//                                     ^^^^^
		// 
		// NEW	53491	53491	53491	8	2015-11-18 08:48:45	2015-11-18 09:47:32	RsTune	pszyndela	not-conne	3.5.0	WARN	CounterCollectorThreadGui	com.dbxtune.cm.CounterSample	com.dbxtune.cm.CounterSample.addRow(CounterSample.java:1411)	Internal Counter Duplicate key in ResultSet for CM 'CmAdminStats', 
		// pk='[Instance, Type, Name]', a row with the key values 'AOBJ, dbo.NewImageRef|OBSERVER|AOBJDeleteCommand2|' already exists. 
		// CurrentRow='[AOBJ, dbo.NewImageRef, 11705, 31, OBSERVER, AOBJDeleteCommand2, 1, null, null, null, null, 0, AOBJ, 65009, 0x04='sysmon (counter flushed as output of admin statistics, sysmon)', 0x80='keep old (previous value of counter retained, usually to aid calculation during next observation period)', 0x200='observer', , Delete command on active object.]'. 
		//     NewRow='[AOBJ, dbo.NewImageRef, 11605, 31, OBSERVER, AOBJDeleteCommand2, 1, null, null, null, null, 0, AOBJ, 65009, 0x04='sysmon (counter flushed as output of admin statistics, sysmon)', 0x80='keep old (previous value of counter retained, usually to aid calculation during next observation period)', 0x200='observer', , Delete command on active object.]'.	53491	53491	
		//                                     ^^^^^

	}

	@Override
	public String getSqlForVersion(DbxConnection conn, DbmsVersionInfo versionInfo)
	{
		boolean sample_resetAfter = Configuration.getCombinedConfiguration().getBooleanProperty(PROPKEY_sample_resetAfter, DEFAULT_sample_resetAfter);

		String sql = "admin statistics, 'ALL' \n";

		if (sample_resetAfter)
			sql += "admin statistics, 'RESET' \n";

		//sql = "admin statistics, 'sysmon'";

		return sql;
	}
	
	@Override
	public String getSqlInitForVersion(DbxConnection conn, DbmsVersionInfo versionInfo)
	{
		long srvVersion = versionInfo.getLongVersion();

		String statsOn = 
			  "--admin stats, cancel -- cancel might crach RS in some cases...\n"
			+ "go \n"
			+ "configure replication server set 'stats_sampling' to 'on' \n"
			+ "go \n"
			+ "configure replication server set 'stats_engineering_counters' to 'on' \n"
			+ "go \n"
//			+ "configure replication server set 'stats_show_zero_counters' to 'on' \n" // Should we do this or not ???  -- By default, admin stats does not report counters that show 0 (zero) OBSERVATION. To change this behavior, set the stats_show_zero_counters configuration parameter on.
//			+ "go \n"
			+ "--admin statistics, reset \n"
			+ "go \n";

		// Add "stuff" for different versions of RepServer
		String AOBJ = "";
		if (srvVersion >= Ver.ver(15, 6))
			AOBJ = "trace 'on', 'dsi', 'dsi_workload' \n"
			     + "go \n";

		return statsOn + AOBJ;
	}

//	@Override
//	public String getSqlCloseForVersion(DbxConnection conn, DbmsVersionInfo versionInfo)
//	{
//		String statsOff = 
//			  "--admin stats, cancel -- cancel might crach RS in some cases...\n"
//			+ "go \n"
//			+ "--configure replication server set 'stats_sampling' to 'off' \n"
//			+ "--go \n"
//			+ "--configure replication server set 'stats_engineering_counters' to 'off' \n"
//			+ "--go \n";
//
//		// NOTE: this should be empty when we pass a proper srvVersion number
//		String AOBJ = "--trace 'off','dsi','dsi_workload' \n";
//		if (srvVersion >= Ver.ver(15, 6))
//			AOBJ = "--trace 'off','dsi','dsi_workload' \n";
//			
//		return statsOff + AOBJ;
//	}
	
	@Override
	public CounterSample createCounterSample(String name, boolean negativeDiffCountersToZero, String[] diffColumns, CounterSample prevSample)
	{
		return new CounterSampleAdminStats(name, negativeDiffCountersToZero, diffColumns, prevSample);
	}

	
	/**
	 * Create a new Sample based on the values in CmAdminStats object
	 * <p>
	 * All Counters in the table for a specific module will be a PIVOT table (one "module" for one "instance" will be one row, with many columns)
	 * 
	 * @param moduleName
	 * @param xrstm 
	 * @return
	 */
	protected List<List<Object>> getModuleCounters(String moduleName, DbxTuneResultSetMetaData xrstm)
	{
		// How it's done
		// Loop all rows in the CounterSample (of "CmAdminStats -- Rs Counters")
		// only for the passed "moduleName"
		//
		// NOTE: Each 'INSTANCE-Name' may have *different* number of Counters 
		//       And at the end we need to "merge" them together...
		// 
		// Lets have a Map where we keep key='INSTANCE-Name', val='Map<cntName, cntVal>'
		// and at the END: We will have to loop everything to came up with "columnNames" for ALL rows
		// Then put them into one row for each 'INSTANCE-Name' but counter-names in a "slot" for that instance.
	
		// Keep InstanceName -> Map<CounterName, cntVal>
		Map<String, Map<String, Long>> instanceToCounter = new LinkedHashMap<>();
		
		// Get the 'CmAdminStats' counters
		CounterSample cs = getCounterSampleAbs();

		// Find column Id's
		List<String> colNames = cs.getColNames();
		if (colNames == null)
			return null;

		int module_pos     = cs.findColumn("Module");
		int instance_pos   = cs.findColumn("Instance");
		int instanceId_pos = cs.findColumn("InstanceId");
		int type_pos       = cs.findColumn("Type");
		int name_pos       = cs.findColumn("Name");
		int  obs_pos       = cs.findColumn("Obs");
		int  total_pos     = cs.findColumn("Total");
		int  last_pos      = cs.findColumn("Last");
//		int  rateXsec_pos  = cs.findColumn("RateXsec");

		// Loop on all CounterSample rows
		for (int rowId = 0; rowId < cs.getRowCount(); rowId++)
		{
			String  module       = cs.getValueAsString (rowId, module_pos);
			
			// SKIP if NOT Correct module
			if ( ! moduleName.equals(module) )
				continue;

			String  instance     = cs.getValueAsString (rowId, instance_pos);
//			Integer instanceId   = cs.getValueAsInteger(rowId, instanceId_pos);
			Long    instanceId   = cs.getValueAsLong   (rowId, instanceId_pos);
			String  type         = cs.getValueAsString (rowId, type_pos);
			String  name         = cs.getValueAsString (rowId, name_pos);
			Long    obs          = cs.getValueAsLong   (rowId, obs_pos);
			Long    total        = cs.getValueAsLong   (rowId, total_pos);
			Long    last         = cs.getValueAsLong   (rowId, last_pos);;
//			Long    rateXsec     = cs.getValueAsLong   (rowId, rateXsec_pos);

			
			Map<String, Long> cntMap = instanceToCounter.get(instance);
			if (cntMap == null)
			{
				cntMap = new LinkedHashMap<>();
				instanceToCounter.put(instance, cntMap);

				// FIXME: Add this as a "CounterName" for now, but remove it at the end when creating ROWS 
				cntMap.put("InstanceId", instanceId);
			}

			// Add: CounterName and CounterValue, depending on the "source" OBSERVER or COUNTER
			Long val;
			if      ("OBSERVER".equals(type)) val = obs;
			else if ("MONITOR" .equals(type)) val = obs; // Previously I used 'last' here... Which I **THINK** was wrong... But we might need to deep dive into that "some" Counters needs to grab from 'obs' and some from 'last' ??? I really do not know... 
			else if ("COUNTER" .equals(type)) val = total;
			else
			{
				val = -99L;
				_logger.info("Unknown type of '" + type + "' for: instance='" + instance + "', instanceId='" + instanceId + "', name='" + name + "'. Adding the value -99");
			}
			
			// Put the CounterName and it's value
			Long oldValue = cntMap.put(name, val);
			if (oldValue != null)
			{
				System.out.println("For module='" + module + "', CounterName='" + name + "', replacing oldValue=" + oldValue + ", newValue=" + val);
			}
		}
		
		// Create ResultSetMetaData for the returned ROWS
		if (xrstm.addStrColumn ("Instance",   -1, false, 255, "")) { if (_logger.isDebugEnabled()) _logger.debug("    > module='" + moduleName + "': addColumn='Instance'."); }
		if (xrstm.addIntColumn ("InstanceId", -1, false,      "")) { if (_logger.isDebugEnabled()) _logger.debug("    > module='" + moduleName + "': addColumn='InstanceId'."); }
		for (Map<String, Long> cntMap : instanceToCounter.values())
		{
			for (String cntName : cntMap.keySet())
			{
				if (xrstm.addLongColumn(cntName, -1, true, "")) { if (_logger.isDebugEnabled()) _logger.debug("    > module='" + moduleName + "': addColumn='" + cntName + "'."); }
			}
		}
		int colSize = xrstm.getColumnCount();
		
		// Create the ROWS to be returned
		List<List<Object>> rows = new ArrayList<List<Object>>(instanceToCounter.size());

		// Loop instances: Create a new ROW and write Counters at a specific ArrayList position
		for (Entry<String, Map<String, Long>> instanceEntry : instanceToCounter.entrySet())
		{
			String instance          = instanceEntry.getKey();
			Map<String, Long> cntMap = instanceEntry.getValue();

			// Initialize the ROW, with NULL values... Just so we can do: row.set(atIndex, value);
			List<Object> row  = new ArrayList<Object>(colSize);
			for (int c = 0; c < colSize; c++)
			{
				row.add(null);
			}
			
			// Set 'Instance' and 'InstanceId'
			row.set(0, instance);
			row.set(1, cntMap.get("InstanceId").intValue());
			cntMap.remove("InstanceId"); // Remove from CounterMap (so it wont be part of below loop)

			// Loop all Counters and add at a specific List Index Position
			for (Entry<String, Long> cntEntry : cntMap.entrySet())
			{
				String cntName = cntEntry.getKey();
				Long   cntVal  = cntEntry.getValue();

				// Get at what List INDEX to set the counter
				// NOTE: SqlPos starts at 1, and ListIndex at 0
				int addPos = xrstm.getColumnSqlPos(cntName) - 1;

				try
				{
					// Set at Position
					row.set(addPos, cntVal);
				}
				catch (RuntimeException rte)
				{
					if (_logger.isDebugEnabled())
					{
						_logger.debug("        --------->>> RuntimeException[" + rte + "]: moduleName='" + moduleName + "': Instance='" + instance + "', cntName='" + cntName + "'... addPos=" + addPos + ", cntVal='" + cntVal + "', row.size()=" + row.size() + ", row=" + row);
					}
				}
			}
			
			// Finally add the ROW to the output ROWS
			rows.add(row);
		}

		if (_logger.isDebugEnabled())
		{
			_logger.debug("getModuleCounters(moduleName='" + moduleName + "'): colCnt=" + xrstm.getColumnCount() + ", colNames=" + xrstm.getColumnNames());
			_logger.debug("getModuleCounters(moduleName='" + moduleName + "'): returns rows.size()=" + rows.size());
			for (int r = 0; r < rows.size(); r++)
			{
				_logger.debug("      row[" + r + "],size[" + rows.get(r).size() + "]: " + rows.get(r));
			}
		}
		
		return rows;
	}
	/**
	 * Create a new Sample based on the values in CmAdminStats object
	 * <p>
	 * All Counters in the table for a specific module will be a pivot table (one "module" for one "instance" will be one row, with many columns)
	 * 
	 * @param moduleName
	 * @param xrstm 
	 * @return
	 */
	protected List<List<Object>> getModuleCounters_OLD(String moduleName, DbxTuneResultSetMetaData xrstm)
	{
		String module     = "";
		int    module_pos = -1;

		String instance     = "";
		int    instance_pos = -1;

		Integer instanceId;
		int     instanceId_pos = -1;

		String type     = "";
		int    type_pos = -1;

		String name     = "";
		int    name_pos = -1;

		Long obs     = null;
		int  obs_pos = -1;

		Long total     = null;
		int  total_pos = -1;

		Long last     = null;
		int  last_pos = -1;

//		Long rateXsec     = null;
//		int  rateXsec_pos = -1;

		CounterSample cs = getCounterSampleAbs();

		// Find column Id's
		List<String> colNames = cs.getColNames();
		if (colNames == null)
			return null;

		for (int colId = 0; colId < colNames.size(); colId++)
		{
			String colName = (String) colNames.get(colId);
			if      (colName.equals("Module"))      module_pos      = colId;
			else if (colName.equals("Instance"))    instance_pos    = colId;
			else if (colName.equals("InstanceId"))  instanceId_pos  = colId;
			else if (colName.equals("Type"))        type_pos        = colId;
			else if (colName.equals("Name"))        name_pos        = colId;
			else if (colName.equals("Obs"))         obs_pos         = colId;
			else if (colName.equals("Total"))       total_pos       = colId;
			else if (colName.equals("Last"))        last_pos        = colId;
//			else if (colName.equals("RateXsec"))    rateXsec_pos    = colId;
		}

		List<List<Object>> rows = new ArrayList<List<Object>>();
		List<Object>       row = new ArrayList<Object>();

		String currentInstance = "";

		// Loop on all CounterSample rows
		for (int rowId = 0; rowId < cs.getRowCount(); rowId++)
		{
			module     = (String)  cs.getValueAt(rowId, module_pos);

			if (! moduleName.equals(module))
				continue;

			instance   = (String)  cs.getValueAt(rowId, instance_pos);
			instanceId = (Integer) cs.getValueAt(rowId, instanceId_pos);
			type       = (String)  cs.getValueAt(rowId, type_pos);
			name       = (String)  cs.getValueAt(rowId, name_pos);
			obs        = (Long)    cs.getValueAt(rowId, obs_pos);
			total      = (Long)    cs.getValueAt(rowId, total_pos);
			last       = (Long)    cs.getValueAt(rowId, last_pos);
//			rateXsec   = (Long)    cs.getValueAt(rowId, rateXsec_pos);

			System.out.println("=========>>> rowId=" + rowId + ": module='" + module + "', rows.size()=" + rows.size() + ", row.size()=" + row.size() + ": --- instance='" + instance + "', type='" + type + "', name='" + name + "'.");
			
			if ( ! currentInstance.equals(instance) )
			{
				System.out.println("     ====>>> NEW-INSTANCE. rowId=" + rowId + ": module='" + module + "': --- instance='" + instance + ".");
				currentInstance = instance;

				if (xrstm.addStrColumn ("Instance",   -1, false, 255, "")) System.out.println("    1>> module='"+module+"': Instance='"+currentInstance+"', addColumn='Instance'.");
				if (xrstm.addIntColumn ("InstanceId", -1, false,      "")) System.out.println("    1>> module='"+module+"': Instance='"+currentInstance+"', addColumn='InstanceId'.");
				if (xrstm.addLongColumn(name,         -1, true,       "")) System.out.println("    1>> module='"+module+"': Instance='"+currentInstance+"', addColumn='"+name+"'.");

				row = new ArrayList<Object>(xrstm.getColumnCount());
				row.add(instance);
				row.add(instanceId);
				
				rows.add(row);

				Long val;
				if      ("OBSERVER".equals(type)) val = obs;
				else if ("COUNTER" .equals(type)) val = total;
				else                              val = last;

				row.add(val);
//				System.out.println("    > -addValue='"+val+"'.");
			}
			else
			{
				if (xrstm.addLongColumn(name, -1, true, "")) System.out.println("    2>> module='" + module + "': Instance='" + currentInstance + "', addColumn='" + name + "'.");

				int addPos = xrstm.getColumnSqlPos(name) - 1;
				
				Long val;
				if      ("OBSERVER".equals(type)) val = obs;
				else if ("COUNTER" .equals(type)) val = total;
				else                              val = last;

//				row.add(addPos, val);
//				System.out.println("    > -addValue='"+val+"'.");

				try
				{
//					if (row.size() < addPos)
//					{
//						row.add("-dummy-value-: row.size()=" + row.size() + ", addPos=" + addPos);
//						System.out.println(">>> ADD-INSTEAD-OF-SET: addPos=" + addPos + ", val='" + val + "', row.size()=" + row.size() + ", row=" + row + ": --- currentInstance='" + currentInstance + "', instance='" + instance + "', type='" + type + "', name='" + name + "', obs='" + obs + "', total='" + total + "', last='" + last + "'.");
//					}

					row.add(addPos, val);
				}
				catch (RuntimeException rte)
				{
					System.out.println("        --------->>> RuntimeException[" + rte + "]: module='" + module + "': Instance='" + currentInstance + "', columnName='" + name + "'... addPos=" + addPos + ", val='" + val + "', row.size()=" + row.size() + ", row=" + row + ": --- currentInstance='" + currentInstance + "', instance='" + instance + "', type='" + type + "', name='" + name + "', obs='" + obs + "', total='" + total + "', last='" + last + "'.");
				}
				
			}
		}
//		System.out.println("=========================================================");
//		System.out.println("COLS("+xrstm.getColumnNames().size()+"): "+ xrstm.getColumnNames());
//		System.out.println("---------------------------------------------------------");
//		for (List<Object> r : rows)
//			System.out.println("ROW("+r.size()+"): "+r);
//		System.out.println("---------------------------------------------------------");
		
		return rows;
	}




	/**
	 * Get all rows that is in the input Set
	 * 
	 * @param counterIdSet a set of CounterId's that we want to get
	 * @return
	 */
	protected List<List<Object>> getCounterIds(int whatData, Set<Integer> counterIdSet)
	{
		Integer counterId;
		int     counterId_pos = -1;

		CounterSample cs;
		if      (whatData == DATA_ABS)  cs = getCounterSampleAbs();
		else if (whatData == DATA_DIFF) cs = getCounterSampleDiff();
		else if (whatData == DATA_RATE) cs = getCounterSampleRate();
		else
			throw new RuntimeException("Only ABS, DIFF, or RATE data is available. you passed whatData="+whatData);

		// Find column Id's
		List<String> colNames = cs.getColNames();
		if (colNames == null)
			return null;

		for (int colId = 0; colId < colNames.size(); colId++)
		{
			String colName = (String) colNames.get(colId);
			if (colName.equals("CounterId"))   counterId_pos   = colId;
		}

		List<List<Object>> rows = new ArrayList<List<Object>>();
		List<Object>       row = null;

		// Get all rows which is in the counterIdSet
		for (int rowId = 0; rowId < cs.getRowCount(); rowId++)
		{
			row = cs.getRow(rowId);
			
			counterId = (Integer)  row.get(counterId_pos);

			if (! counterIdSet.contains(counterId))
				continue;

			rows.add(row);
		}

		return rows;
	}

//	/**
//	 * Create a new Sample based on the values in CmAdminStats object
//	 * <p>
//	 * All Counters in the table for a specific module will be a pivot table (one "module" for one "instance" will be one row, with many columns)
//	 * 
//	 * @param moduleName
//	 * @param xrstm 
//	 * @return
//	 */
//	protected List<List<Object>> getModuleSourceToDest(DbxTuneResultSetMetaData xrstm)
//	{
//		String module     = "";
//		int    module_pos = -1;
//
//		String instance     = "";
//		int    instance_pos = -1;
//
//		Integer instanceId;
//		int     instanceId_pos = -1;
//
//		Integer counterId;
//		int     counterId_pos = -1;
//
//		String type     = "";
//		int    type_pos = -1;
//
//		String name     = "";
//		int    name_pos = -1;
//
//		Long obs     = null;
//		int  obs_pos = -1;
//
//		Long total     = null;
//		int  total_pos = -1;
//
//		Long last     = null;
//		int  last_pos = -1;
//
////		Long rateXsec     = null;
////		int  rateXsec_pos = -1;
//
//		CounterSample cs = getCounterSampleAbs();
//
//		// Find column Id's
//		List<String> colNames = cs.getColNames();
//		if (colNames == null)
//			return null;
//
//		for (int colId = 0; colId < colNames.size(); colId++)
//		{
//			String colName = (String) colNames.get(colId);
//			if      (colName.equals("Module"))      module_pos      = colId;
//			else if (colName.equals("Instance"))    instance_pos    = colId;
//			else if (colName.equals("InstanceId"))  instanceId_pos  = colId;
//			else if (colName.equals("CounterId"))   counterId_pos   = colId;
//			else if (colName.equals("Type"))        type_pos        = colId;
//			else if (colName.equals("Name"))        name_pos        = colId;
//			else if (colName.equals("Obs"))         obs_pos         = colId;
//			else if (colName.equals("Total"))       total_pos       = colId;
//			else if (colName.equals("Last"))        last_pos        = colId;
////			else if (colName.equals("RateXsec"))    rateXsec_pos    = colId;
//		}
//
//		List<List<Object>> rows = new ArrayList<List<Object>>();
//		List<Object>       row = new ArrayList<Object>();
//
////		db2db
////		--	58000 (@src_dbid)				Repagent: Commands received	REPAGENT	CmdsRecv
////		--	6000  (@src_ldbid)				SQM: Commands written to queue	SQM	CmdsWritten
////		--	6020  (@src_ldbid)				SQM: Active queue segments	SQM	SegsActive
////		--	62000 (special: @src_ldbid, @sqt_reader)	SQMR: Commands read from queue	SQMR	cmds	CmdsRead
////		--	24000 (special: see code)			SQT: Commands read from queue	SQT	cmds	CmdsRead
////		--	30000 (@src_dbid)				DIST: Commands read from inbound queue	DIST	cmds	CmdsRead
////		--	6000  (@dest_ldbid)				SQM: Commands written to queue	SQM	CmdsWritten
////		--	6020  (@dest_ldbid)				SQM: Active queue segments	SQM	SegsActive
////		--	62013 (@src_ldbid, @sqt_reader)			SQMR: Unread segments	SQMR	SQMRBacklogSeg
////		--	62013 (@dest_dbid, 10)				SQMR: Unread segments	SQMR	SQMRBacklogSeg
////		--	5030  (@dest_dbid)				DSI: Commands read from outbound queue	DSI	cmds	DSICmdsRead
////		rsi2db
////		--	59001	(src_rsid)	RSIUSER: RSI messages received	RSIUSER	RSIUMsgRecv
////		--	6000	(dest_ldbid)	SQM: Commands written to queue	SQM	CmdsWritten
////		--	6020	(dest_dbid)	SQM: Active queue segments	SQM	SegsActive
////		--	62013	(dest_dbid)	SQMR: Unread segments	SQMR	SQMRBacklogSeg
////		--	5030	(dest_dbid)	DSI: Commands read from outbound queue	DSI	cmds	DSICmdsRead
////		db2rsi
////		--	58000	(src_dbid)		Repagent: Commands received	REPAGENT	CmdsRecv
////		--	6000	(src_dbid)		SQM: Commands written to queue	SQM	CmdsWritten
////		--	6020	(src_dbid)		SQM: Active queue segments	SQM	SegsActive
////		--	62000	(src_ldbid, sqt_reader)	SQMR: Commands read from queue	SQMR	cmds	CmdsRead
////		--	24000	(src_ldbid)		SQT: Commands read from queue	SQT	cmds	CmdsRead
////		--	30000	(src_dbid)		DIST: Commands read from inbound queue	DIST	cmds	CmdsRead
////		--	6000	(dest_rsid)		SQM: Commands written to queue	SQM	CmdsWritten
////		--	6020	(dest_rsid)		SQM: Active queue segments	SQM	SegsActive
////		--	62013	(src_ldbid)		SQMR: Unread segments	SQMR	SQMRBacklogSeg
////		-- route backlog SQMR rs_statdetail.label looks like: SQMR, 16777320:0 BARCELONA15_RS_1, 0, GLOBAL RS
////		-- but the easy way to tell is that the instance_id is the @dest_rsid
////		--	62013	(dest_rsid)		SQMR: Unread segments	SQMR	SQMRBacklogSeg
////		--	4004	(dest_rsid)		RSI: Messages sent with type RSI_MESSAGE	RSI	MsgsSent
//		
//		Set<Integer> counterIdSet = new HashSet<Integer>();
//		counterIdSet.add(58000); // 58000	Repagent: Commands received	REPAGENT	CmdsRecv
//		counterIdSet.add(6000);  // 6000	SQM: Commands written to queue	SQM	CmdsWritten
//		counterIdSet.add(6020);  // 6020	SQM: Active queue segments	SQM	SegsActive
//		counterIdSet.add(62000); // 62000	SQMR: Commands read from queue	SQMR	cmds	CmdsRead
//		counterIdSet.add(24000); // 24000	SQT: Commands read from queue	SQT	cmds	CmdsRead
//		counterIdSet.add(30000); // 30000	DIST: Commands read from inbound queue	DIST	cmds	CmdsRead
////		counterIdSet.add(6000);  // 6000	SQM: Commands written to queue	SQM	CmdsWritten
////		counterIdSet.add(6020);  // 6020	SQM: Active queue segments	SQM	SegsActive
//		counterIdSet.add(62013); // 62013	SQMR: Unread segments	SQMR	SQMRBacklogSeg
//		counterIdSet.add(5030);  // 5030	DSI: Commands read from outbound queue	DSI	cmds	DSICmdsRead
//
//		// rsi2db
//		counterIdSet.add(59001); // 59001	RSIUSER: RSI messages received	RSIUSER	RSIUMsgRecv
//
//		// db2rsi
//		counterIdSet.add(4004);  // 4004	RSI: Messages sent with type RSI_MESSAGE	RSI	MsgsSent
//
//		
//		xrstm.addStrColumn("type",               1,  false, 10, "what type is this: db2db, rsi2db, db2rsi");
//		xrstm.addStrColumn("source",             2,  false, 62, "Source of the data");
//		xrstm.addStrColumn("destination",        3,  false, 62, "Destination");
//
//		xrstm.addLongColumn("RACmdsPerSec",      4,  true, ""); // db2db
//		xrstm.addLongColumn("SQMInCmdsPerSec",   5,  true, ""); // db2db
//		xrstm.addLongColumn("SegsActiveInMB",    6,  true, ""); // db2db
//		xrstm.addLongColumn("BacklogInMB",       7,  true, ""); // db2db
//		xrstm.addLongColumn("SQMRCmdsPerSec",    8,  true, ""); // db2db
//		xrstm.addLongColumn("SQTCmdsPerSec",     9,  true, ""); // db2db
//		xrstm.addLongColumn("DISTCmdsPerSec",    10, true, ""); // db2db
//		xrstm.addLongColumn("SQMOutCmdsPerSec",  11, true, ""); // db2db
//		xrstm.addLongColumn("SegsActiveOutMB",   12, true, ""); // db2db
//		xrstm.addLongColumn("DSICmdsSec",        13, true, ""); // db2db
//
////		xrstm.addLongColumn("RSIUMsgSec",        4, true, ""); // rsi2db
////		xrstm.addLongColumn("SQMOutCmdsPerSec",  5, true, ""); // rsi2db
////		xrstm.addLongColumn("SegsActiveOut",     6, true, ""); // rsi2db
////		xrstm.addLongColumn("SegsActiveOutMB",   7, true, ""); // rsi2db
////		xrstm.addLongColumn("DSICmdsSec",        8, true, ""); // rsi2db
////
////		xrstm.addLongColumn("RACmdsPerSec",      4, true, ""); // db2rsi
////		xrstm.addLongColumn("SQMInCmdsPerSec",   5, true, ""); // db2rsi
////		xrstm.addLongColumn("SegsActiveInMB",    6, true, ""); // db2rsi
////		xrstm.addLongColumn("SQMRCmdsPerSec",    7, true, ""); // db2rsi
////		xrstm.addLongColumn("SQTCmdsPerSec",     8, true, ""); // db2rsi
////		xrstm.addLongColumn("DISTCmdsPerSec",    9, true, ""); // db2rsi
////		xrstm.addLongColumn("SQMOutCmdsPerSec", 10, true, ""); // db2rsi
////		xrstm.addLongColumn("SegsActiveOutMB",  11, true, ""); // db2rsi
////		xrstm.addLongColumn("BacklogOutMB",     12, true, ""); // db2rsi
////		xrstm.addLongColumn("RSIMsgsSec",       13, true, ""); // db2rsi
//
////			values ('RS15.7', 'RateSummary db2db', 'RS Performance Summary (cmds/sec, backlog in MB):','RepAgent', 'SQM(i)', 'Active(i)',  'Backlog(i)', 'SQMR(SQT)', 'SQT',  'DIST',   'SQM(o)',    'Active(o)',  'DSI')
////			values ('RS15.7', 'RateSummary db2rsi','RS Performance Summary (cmds/sec, backlog in MB):','RepAgent', 'SQM(i)', 'Backlog(i)', 'SQMR(SQT)',  'SQT',       'DIST', 'SQM(o)', 'Active(r)', 'Backlog(r)', 'RSI')
////			values ('RS15.7', 'RateSummary rsi2db','RS Performance Summary (cmds/sec, backlog in MB):','RSIUser',  'SQM(i)', 'Active(i)',  'ActiveMB',   'DSI',       null,   null,     null,        null,         null)
//
//		
//		// Get all rows which is in the counterIdSet
//		for (int rowId = 0; rowId < cs.getRowCount(); rowId++)
//		{
//			row = cs.getRow(rowId);
//			
//			counterId = (Integer)  row.get(counterId_pos);
//
//			if (! counterIdSet.contains(counterId))
//				continue;
//
//			rows.add(row);
//		}
//
//		// Loop 
//		for (int rowId = 0; rowId < rows.size(); rowId++)
//		{
//			row = rows.get(rowId);
//			counterId = (Integer)  row.get(counterId_pos);
//
//			instance   = (String)  row.get(instance_pos);
//			instanceId = (Integer) row.get(instanceId_pos);
//			type       = (String)  row.get(type_pos);
//			name       = (String)  row.get(name_pos);
//			obs        = (Long)    row.get(obs_pos);
//			total      = (Long)    row.get(total_pos);
//			last       = (Long)    row.get(last_pos);
////			rateXsec   = (Long)    row.get(rateXsec_pos);
//			
//			System.out.println("instance='"+instance+"', instanceId="+instanceId+", type='"+type+"', name='"+name+"', obs="+obs+", total="+total+", last="+last+".");
//		}
//		
//		return rows;
//	}
}








//protected List<List<Object>> getModuleCounters(String moduleName, DbxTuneResultSetMetaData xrstm)
//{
//	CounterSample csTmp = getCounterSampleAbs();
//
//	if (csTmp == null)
//		throw new RuntimeException("getCounterSampleAbs() returned null");
//
//	if ( ! (csTmp instanceof CounterSampleAdminStats) )
//		throw new RuntimeException("getCounterSampleAbs() expected Object of 'CounterSampleAdminStats' but we got '"+csTmp.getClass().getName()+"'.");
//
//	CounterSampleAdminStats cs = (CounterSampleAdminStats) csTmp;
//	
//	RsStatCounterDictionary dict = RsStatCounterDictionary.getInstance();
//	for (Instance i : cs.getInstanceList())
//	{
//		// SKIP empty instances, it looks like it's only on the RSSD RepAgent (if the RSSD isn't primary)
//		if ("".equals(i._name))
//			continue;
//
//		for (Counter counter : i._counterMap.values())
//		{
//			List<Object> row = new ArrayList<Object>();
//			row.add(i._name);
//			row.add(i._id);
//			row.add(i._val);
//			row.add("COUNTER");
//			row.add(counter._name);
//			row.add(counter._obs);
//			row.add(counter._total);
//			row.add(counter._last);
//			row.add(counter._max);
//			row.add(counter._avg_ttl_obs);
//			row.add(counter._rate_x_sec);
//
//			StatCounterEntry c = dict.getCounter(i._name, counter._name);
//			row.add( c == null ? null : c._moduleName);
//			row.add( c == null ? null : c._counterId);
//			row.add( c == null ? null : c.getStatusDesc());
//			row.add( c == null ? null : c._description);
//
//			addRow(row);
//		}
//
//		for (Monitor monitor : i._monitorMap.values())
//		{
//			List<Object> row = new ArrayList<Object>();
//			row.add(i._name);
//			row.add(i._id);
//			row.add(i._val);
//			row.add("MONITOR");
//			row.add(monitor._name);
//			row.add(monitor._obs);
//			row.add(null);
//			row.add(monitor._last);
//			row.add(monitor._max);
//			row.add(monitor._avg_ttl_obs);
//			row.add(null);
//
//			StatCounterEntry c = dict.getCounter(i._name, monitor._name);
//			row.add( c == null ? null : c._moduleName);
//			row.add( c == null ? null : c._counterId);
//			row.add( c == null ? null : c.getStatusDesc());
//			row.add( c == null ? null : c._description);
//
//			addRow(row);
//		}
//
//		for (Observer observer : i._observerMap.values())
//		{
//			List<Object> row = new ArrayList<Object>();
//			row.add(i._name);
//			row.add(i._id);
//			row.add(i._val);
//			row.add("OBSERVER");
//			row.add(observer._name);
//			row.add(observer._obs);
//			row.add(null);
//			row.add(null);
//			row.add(null);
//			row.add(null);
//			row.add(observer._rate_x_sec);
//
//			StatCounterEntry c = dict.getCounter(i._name, observer._name);
//			row.add( c == null ? null : c._moduleName);
//			row.add( c == null ? null : c._counterId);
//			row.add( c == null ? null : c.getStatusDesc());
//			row.add( c == null ? null : c._description);
//
//			addRow(row);
//		}
//	}
//
//	return rows;
//}
