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
package com.dbxtune.cm.sqlserver;

import java.awt.event.MouseEvent;
import java.lang.invoke.MethodHandles;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import javax.naming.NameNotFoundException;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.dbxtune.CounterController;
import com.dbxtune.ICounterController;
import com.dbxtune.IGuiController;
import com.dbxtune.alarm.AlarmHandler;
import com.dbxtune.alarm.events.AlarmEvent;
import com.dbxtune.alarm.events.sqlserver.AlarmEventMemoryClerkWarning;
import com.dbxtune.central.pcs.CentralPersistReader;
import com.dbxtune.cm.CmSettingsHelper;
import com.dbxtune.cm.CounterSample;
import com.dbxtune.cm.CounterSetTemplates;
import com.dbxtune.cm.CounterSetTemplates.Type;
import com.dbxtune.cm.CountersModel;
import com.dbxtune.cm.sqlserver.gui.CmMemoryClerksPanel;
import com.dbxtune.config.dict.MonTablesDictionary;
import com.dbxtune.config.dict.MonTablesDictionaryManager;
import com.dbxtune.config.dict.SqlServerMemoryClerksDictionary;
import com.dbxtune.graph.TrendGraphDataPoint;
import com.dbxtune.graph.TrendGraphDataPoint.LabelType;
import com.dbxtune.gui.MainFrame;
import com.dbxtune.gui.ResultSetTableModel;
import com.dbxtune.gui.TabularCntrPanel;
import com.dbxtune.sql.conn.DbxConnection;
import com.dbxtune.sql.conn.info.DbmsVersionInfo;
import com.dbxtune.utils.Configuration;

/**
 * @author Goran Schwarz (goran_schwarz@hotmail.com)
 */
public class CmMemoryClerks
extends CountersModel
{
	private static final Logger _logger = LogManager.getLogger(MethodHandles.lookup().lookupClass());
	private static final long    serialVersionUID = 1L;

	public static final String   CM_NAME          = CmMemoryClerks.class.getSimpleName();
	public static final String   SHORT_NAME       = "Memory";
	public static final String   HTML_DESC        = 
		"<html>" +
			"<p>Information about the memory clerks.</p>" +
		"</html>";

	public static final String   GROUP_NAME       = MainFrame.TCP_GROUP_SERVER;
	public static final String   GUI_ICON_FILE    = "images/"+CM_NAME+".png";

	public static final long     NEED_SRV_VERSION = 0;
	public static final long     NEED_CE_VERSION  = 0;

	public static final String[] MON_TABLES       = new String[] {CM_NAME, "dm_os_memory_clerks"};
	public static final String[] NEED_ROLES       = new String[] {"VIEW SERVER STATE"};
	public static final String[] NEED_CONFIG      = new String[] {};

	public static final String[] PCT_COLUMNS      = new String[] {};
	public static final String[] DIFF_COLUMNS     = new String[] {
		"SizeMb_diff"
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

		return new CmMemoryClerks(counterController, guiController);
	}

	public CmMemoryClerks(ICounterController counterController, IGuiController guiController)
	{
		super(counterController, guiController,
				CM_NAME, GROUP_NAME, /*sql*/null, /*pkList*/null, 
				DIFF_COLUMNS, PCT_COLUMNS, MON_TABLES, 
				NEED_ROLES, NEED_CONFIG, NEED_SRV_VERSION, NEED_CE_VERSION, 
				NEGATIVE_DIFF_COUNTERS_TO_ZERO, IS_SYSTEM_CM, DEFAULT_POSTPONE_TIME);

		setDisplayName(SHORT_NAME);
		setDescription(HTML_DESC);

		setIconFile(GUI_ICON_FILE);

		setShowClearTime(false);

		setCounterController(counterController);
		setGuiController(guiController);
		
		addTrendGraphs();
		
		CounterSetTemplates.register(this);
	}

//TODO; -- POSSIBLY -- Add Aggregate(SUM) to some columns... NOTE: Make sure local graph does NOT include the AGGREGATED row in the local Pie Chart;
	

	//------------------------------------------------------------
	// Implementation
	//------------------------------------------------------------
	private static final String  PROP_PREFIX                       = CM_NAME;

	public static final String GRAPH_NAME_MEMORY_CLERK_BUFFER_POOL = "MemoryClerkBp";
	public static final String GRAPH_NAME_MEMORY_CLERKS_TOP        = "MemoryClerksTop";
	public static final String GRAPH_NAME_MEMORY_TTM_VS_ALL_CLERKS = "TTMemVsAllClerks"; // Target and Total Memory vs ALL memory Clerks

	private void addTrendGraphs()
	{
		//-----
		addTrendGraph(GRAPH_NAME_MEMORY_CLERK_BUFFER_POOL,
			"Buffer Pool Memory Clerk, in MB", // Menu CheckBox text
			"Buffer Pool Memory Clerk, in MB ("+GROUP_NAME+"->"+SHORT_NAME+")", // Label 
			TrendGraphDataPoint.createGraphProps(TrendGraphDataPoint.Y_AXIS_SCALE_LABELS_MB, CentralPersistReader.SampleType.MAX_OVER_SAMPLES),
			new String[] {"Buffer Pool (MEMORYCLERK_SQLBUFFERPOOL)"}, 
			LabelType.Static,
			TrendGraphDataPoint.Category.MEMORY,
			false,  // is Percent Graph
			true,  // visible at start
			0,     // graph is valid from Server Version. 0 = All Versions; >0 = Valid from this version and above 
			-1);   // minimum height

		//-----
		addTrendGraph(GRAPH_NAME_MEMORY_CLERKS_TOP,
			"Top 10 Memory Clerks, in MB", // Menu CheckBox text
			"Top 10 Memory Clerks, in MB ("+GROUP_NAME+"->"+SHORT_NAME+")", // Label 
			TrendGraphDataPoint.createGraphProps(TrendGraphDataPoint.Y_AXIS_SCALE_LABELS_MB, CentralPersistReader.SampleType.MAX_OVER_SAMPLES),
			null, 
			LabelType.Dynamic,
			TrendGraphDataPoint.Category.MEMORY,
			false,  // is Percent Graph
			true,  // visible at start
			0,     // graph is valid from Server Version. 0 = All Versions; >0 = Valid from this version and above 
			-1);   // minimum height

		//-----
		addTrendGraph(GRAPH_NAME_MEMORY_TTM_VS_ALL_CLERKS,
			"All Memory Clerks vs Target & Total Memory, in MB", // Menu CheckBox text
			"All Memory Clerks vs Target & Total Memory, in MB ("+GROUP_NAME+"->"+SHORT_NAME+")", // Label 
			TrendGraphDataPoint.createGraphProps(TrendGraphDataPoint.Y_AXIS_SCALE_LABELS_MB, CentralPersistReader.SampleType.MAX_OVER_SAMPLES),
			new String[] {"SUM of All Memory Clerks", "Target Server Memory MB", "Total Server Memory MB", "Buffer Pool Memory Clerk"}, 
			LabelType.Static,
			TrendGraphDataPoint.Category.MEMORY,
			false,  // is Percent Graph
			true,  // visible at start
			0,     // graph is valid from Server Version. 0 = All Versions; >0 = Valid from this version and above 
			-1);   // minimum height
	}

	@Override
	public void updateGraphData(TrendGraphDataPoint tgdp)
	{
		// -----------------------------------------------------------------------------------------
		if (GRAPH_NAME_MEMORY_CLERK_BUFFER_POOL.equals(tgdp.getName()))
		{
			Double[] dArray = new Double[1];

			dArray[0] = this.getAbsValueAsDouble("MEMORYCLERK_SQLBUFFERPOOL", "SizeMb");

			// Set the values
			tgdp.setDataPoint(this.getTimestamp(), dArray);
		}

		// -----------------------------------------------------------------------------------------
		if (GRAPH_NAME_MEMORY_CLERKS_TOP.equals(tgdp.getName()))
		{
			int top = 10;
			
			// Write 1 "line" for every database
			Double[] dArray = new Double[top];
			String[] lArray = new String[top];
			
			int ap = 0;
			for (int row = 0; row < top; row++)
			{
				String type = this.getAbsString(row, "type");
				
				// SKIP: Buffer Pool -- It's handled in another graph (since it's usually BIG)
				if ("MEMORYCLERK_SQLBUFFERPOOL".equals(type))
				{
					top++;
					continue;
				}

				lArray[ap] = type;
				dArray[ap] = this.getAbsValueAsDouble(row, "SizeMb");
				ap++; // increment after... otherwise we will start at: 1
			}

			// Set the values
			tgdp.setDataPoint(this.getTimestamp(), lArray, dArray);
		}

		// -----------------------------------------------------------------------------------------
		if (GRAPH_NAME_MEMORY_TTM_VS_ALL_CLERKS.equals(tgdp.getName()))
		{
			Double[] dArray = new Double[4];

			CmSummary cmSummary = (CmSummary) CounterController.getSummaryCm();
			
			dArray[0] = this.getAbsValueSum("SizeMb");
			dArray[1] = Double.valueOf( cmSummary.getLastTargetServerMemoryMb() );
			dArray[2] = Double.valueOf( cmSummary.getLastTotalServerMemoryMb() );
			dArray[3] = this.getAbsValueAsDouble("MEMORYCLERK_SQLBUFFERPOOL", "SizeMb");

			// Set the values
			tgdp.setDataPoint(this.getTimestamp(), dArray);
		}

	}



	@Override
	public void addMonTableDictForVersion(DbxConnection conn, DbmsVersionInfo versionInfo)
	{
		try 
		{
			MonTablesDictionary mtd = MonTablesDictionaryManager.getInstance();

			String cmName = this.getName();
			mtd.addTable(cmName, HTML_DESC);

			mtd.addColumn(cmName, "type"            ,"<html>Specifies the type of memory clerk.</html>");
			mtd.addColumn(cmName, "name"            ,"<html>Specifies the internally assigned name of this memory clerk. A component can have several memory clerks of a specific type. A component might choose to use specific names to identify memory clerks of the same type</html>");
			mtd.addColumn(cmName, "InstanceCount"   ,"<html>The Clerk might have several instances, this is the Instance Count.</html>");
			mtd.addColumn(cmName, "SizeMb"          ,"<html>Amount of MB this clerk is using</html>");
			mtd.addColumn(cmName, "SizeMb_diff"     ,"<html>Amount of MB this clerk is using</html>");
			mtd.addColumn(cmName, "AvgMb"           ,"<html>If the Clerk has many instances, this will be the AVERAGE memory in MB</html>");
			mtd.addColumn(cmName, "MaxMb"           ,"<html>If the Clerk has many instances, this will be the MAX memory in MB</html>");
			mtd.addColumn(cmName, "MinMb"           ,"<html>If the Clerk has many instances, this will be the MIN memory in MB</html>");

			mtd.addColumn(cmName, "pages_MB"                     ,"<html>Specifies the amount of page memory allocated in megabytes (MB) for this memory clerk</html>");
			mtd.addColumn(cmName, "virtual_memory_reserved_MB"   ,"<html>Specifies the amount of virtual memory that is reserved by a memory clerk</html>");
			mtd.addColumn(cmName, "virtual_memory_committed_MB"  ,"<html>Specifies the amount of virtual memory that is committed by a memory clerk. The amount of committed memory should always be less than the amount of reserved memory.</html>");
			mtd.addColumn(cmName, "awe_allocated_MB"             ,"<html>Specifies the amount of memory in megabytes (MB) locked in the physical memory and not paged out by the operating system.</html>");
			mtd.addColumn(cmName, "shared_memory_reserved_MB"    ,"<html>Specifies the amount of shared memory that is reserved by a memory clerk. The amount of memory reserved for use by shared memory and file mapping.</html>");
			mtd.addColumn(cmName, "shared_memory_committed_MB"   ,"<html>Specifies the amount of shared memory that is committed by the memory clerk.</html>");
		}
		catch (NameNotFoundException e) 
		{
		//	_logger.warn("Problems in cm='" + CM_NAME + "', adding addMonTableDictForVersion. Caught: " + e); 
			System.out.println("Problems in cm='" + CM_NAME + "', adding addMonTableDictForVersion. Caught: " + e); 
		}
	}

	@Override
	public String getToolTipTextOnTableCell(MouseEvent e, String colName, Object cellValue, int modelRow, int modelCol) 
	{
		if ("type".equals(colName) )
		{
			int pos_type = findColumn("type");
			if (pos_type >= 0)
			{
				Object cellVal = getValueAt(modelRow, pos_type);
				if (cellVal instanceof String)
				{
					return SqlServerMemoryClerksDictionary.getInstance().getDescriptionHtml((String) cellVal);
				}
			}
		}
		
		return super.getToolTipTextOnTableCell(e, colName, cellValue, modelRow, modelCol);
	}
	
	@Override
	protected TabularCntrPanel createGui()
	{
		return new CmMemoryClerksPanel(this);
	}

	@Override
	public String[] getDependsOnConfigForVersion(DbxConnection conn, DbmsVersionInfo versionInfo)
	{
		return NEED_CONFIG;
	}

	@Override
	public List<String> getPkForVersion(DbxConnection conn, DbmsVersionInfo versionInfo)
	{
		List <String> pkCols = new LinkedList<String>();

		pkCols.add("type");

		return pkCols;
	}

	@Override
	public String getSqlForVersion(DbxConnection conn, DbmsVersionInfo versionInfo)
	{
		ResultSetTableModel rstm_dummy = null;
		if (conn != null)
			rstm_dummy = ResultSetTableModel.executeQuery(conn, "select * from sys.dm_os_memory_clerks where 1=2", true, "dummy");
		
		// in Version 2012 ??? the columns 'pages_kb' was introduced
		String pages_kb = "([single_pages_kb] + [multi_pages_kb])";
		if (rstm_dummy  != null && rstm_dummy.hasColumn("pages_kb"))
		{
			pages_kb = "[pages_kb]";
		}

		String calculation_cols = "([single_pages_kb] + [multi_pages_kb] + [virtual_memory_committed_kb] + [awe_allocated_kb])";
		if (rstm_dummy  != null && rstm_dummy.hasColumn("pages_kb"))
		{
			calculation_cols = "([pages_kb] + [virtual_memory_committed_kb] + [awe_allocated_kb])";
		}
		
		String sql = ""
			    + "SELECT /* ${cmCollectorName} */ \n"
			    + "     [type]                                          AS [type]\n"
			    + "    ,max([name])                                     AS [name] \n"
			    + "    ,count(*)                                        AS [InstanceCount] \n"
			    + "    ,cast(SUM(" + calculation_cols + ") / 1024.0 as numeric(12,1)) AS [SizeMb] \n"
			    + "    ,cast(SUM(" + calculation_cols + ") / 1024.0 as numeric(12,1)) AS [SizeMb_diff] \n"
			    + "    ,cast(AVG(" + calculation_cols + ") / 1024.0 as numeric(12,1)) AS [AvgMb] \n"
			    + "    ,cast(MAX(" + calculation_cols + ") / 1024.0 as numeric(12,1)) AS [MaxMb] \n"
			    + "    ,cast(MIN(" + calculation_cols + ") / 1024.0 as numeric(12,1)) AS [MinMb] \n"

			    // Some extra columns (not knowing if they are important or not)
			    + "    ,cast(SUM(" + pages_kb             +  ") / 1024.0 as numeric(12,1)) AS [pages_MB] \n"
			    + "    ,cast(SUM([virtual_memory_reserved_kb] ) / 1024.0 as numeric(12,1)) AS [virtual_memory_reserved_MB] \n"
			    + "    ,cast(SUM([virtual_memory_committed_kb]) / 1024.0 as numeric(12,1)) AS [virtual_memory_committed_MB] \n"
			    + "    ,cast(SUM([awe_allocated_kb]           ) / 1024.0 as numeric(12,1)) AS [awe_allocated_MB] \n"
			    + "    ,cast(SUM([shared_memory_reserved_kb]  ) / 1024.0 as numeric(12,1)) AS [shared_memory_reserved_MB] \n"
			    + "    ,cast(SUM([shared_memory_committed_kb] ) / 1024.0 as numeric(12,1)) AS [shared_memory_committed_MB] \n"
			    
			    + "FROM sys.dm_os_memory_clerks \n"
			    + "GROUP BY [type] \n"
			    + "ORDER BY [SizeMb] DESC \n"
			    + "";

		return sql;

		// POSSIBLY IF WE WANT TO HAVE A SUMMARY... use "GROUPING SETS ( ([type]), () )" and isnull([type], '--SUMMARY--') ...
//		String sql = ""
//			    + "SELECT /* SqlServerTune:CmMemoryClerks */ \n"
//			    + "     isnull([type], '--SUMMARY--')                                      AS [type] \n"
//			    + "    ,CASE WHEN [type] IS NULL THEN '--SUMMARY--' ELSE max([name]) END   AS [name] \n"
//			    + "    ,count(*)                                                           AS [InstanceCount] \n"
//			    + "    ,cast(SUM([pages_kb])                    / 1024.0 as numeric(12,1)) AS [SizeMb] \n"
//			    + "    ,cast(SUM([pages_kb])                    / 1024.0 as numeric(12,1)) AS [SizeMb_diff] \n"
//			    + "    ,cast(AVG([pages_kb])                    / 1024.0 as numeric(12,1)) AS [AvgMb] \n"
//			    + "    ,cast(MAX([pages_kb])                    / 1024.0 as numeric(12,1)) AS [MaxMb] \n"
//			    + "    ,cast(MIN([pages_kb])                    / 1024.0 as numeric(12,1)) AS [MinMb] \n"
//			    + "    ,cast(SUM([virtual_memory_reserved_kb] ) / 1024.0 as numeric(12,1)) AS [virtual_memory_reserved_MB] \n"
//			    + "    ,cast(SUM([virtual_memory_committed_kb]) / 1024.0 as numeric(12,1)) AS [virtual_memory_committed_MB] \n"
//			    + "    ,cast(SUM([awe_allocated_kb]           ) / 1024.0 as numeric(12,1)) AS [awe_allocated_MB] \n"
//			    + "    ,cast(SUM([shared_memory_reserved_kb]  ) / 1024.0 as numeric(12,1)) AS [shared_memory_reserved_MB] \n"
//			    + "    ,cast(SUM([shared_memory_committed_kb] ) / 1024.0 as numeric(12,1)) AS [shared_memory_committed_MB] \n"
//			    + "FROM sys.dm_os_memory_clerks \n"
//			    + "--GROUP BY [type] \n"
//			    + "GROUP BY GROUPING SETS ( ([type]), () ) \n"
//			    + "ORDER BY SUM([pages_kb]) DESC \n"
//			    + "";

		// Get "max memory" from configuration... (so we can see if we are "near/over" the max configured memory.
//		IDbmsConfigEntry maxMemoryMbEntry = DbmsConfigManager.getInstance().getDbmsConfigEntry("max server memory (MB)");
//		if (maxMemoryMbEntry != null)
//		{
//			int maxMemoryMb = StringUtil.parseInt(maxMemoryMbEntry.getConfigValue(), -1);
//		}

		// or possibly: select value_in_use from sys.configurations where configuration_id = 1544 /*max server memory (MB)*/
		// Only use this if you can STORE the value somewhere (so we can read it if we are watching a "replay" (saved in offline storage)
	}

	
	@Override
	public Map<String, AggregationType> createAggregateColumns()
	{
		HashMap<String, AggregationType> aggColumns = new HashMap<>(getColumnCount());

		AggregationType tmp;
		
		// Create the columns :::::::::::::::::::::::::::::::::::::::::::::::::::::: And ADD it to the return Map 
		tmp = new AggregationType("SizeMb"                     , AggregationType.Agg.SUM);   aggColumns.put(tmp.getColumnName(), tmp);
		tmp = new AggregationType("SizeMb_diff"                , AggregationType.Agg.SUM);   aggColumns.put(tmp.getColumnName(), tmp);
		tmp = new AggregationType("AvgMb"                      , AggregationType.Agg.AVG);   aggColumns.put(tmp.getColumnName(), tmp);
		tmp = new AggregationType("MaxMb"                      , AggregationType.Agg.MAX);   aggColumns.put(tmp.getColumnName(), tmp);
		tmp = new AggregationType("MinMb"                      , AggregationType.Agg.MIN);   aggColumns.put(tmp.getColumnName(), tmp);

		tmp = new AggregationType("pages_MB"                   , AggregationType.Agg.SUM);   aggColumns.put(tmp.getColumnName(), tmp);
		tmp = new AggregationType("virtual_memory_reserved_MB" , AggregationType.Agg.SUM);   aggColumns.put(tmp.getColumnName(), tmp);
		tmp = new AggregationType("virtual_memory_committed_MB", AggregationType.Agg.SUM);   aggColumns.put(tmp.getColumnName(), tmp);
		tmp = new AggregationType("awe_allocated_MB"           , AggregationType.Agg.SUM);   aggColumns.put(tmp.getColumnName(), tmp);
		tmp = new AggregationType("shared_memory_reserved_MB"  , AggregationType.Agg.SUM);   aggColumns.put(tmp.getColumnName(), tmp);
		tmp = new AggregationType("shared_memory_committed_MB" , AggregationType.Agg.SUM);   aggColumns.put(tmp.getColumnName(), tmp);
		
		return aggColumns;
	}

	@Override
	public Object calculateAggregateRow_nonAggregatedColumnDataProvider(CounterSample newSample, String colName, int c, int jdbcType, Object addValue)
	{
		if ("name".equalsIgnoreCase(colName))
			return "_Total";

		if ("InstanceCount".equalsIgnoreCase(colName))
			return Integer.valueOf(-1);

		
		return null;
	}




	@Override
	public boolean isGraphDataHistoryEnabled(String name)
	{
		// ENABLED for the following graphs
		if (GRAPH_NAME_MEMORY_CLERKS_TOP       .equals(name)) return true;
		if (GRAPH_NAME_MEMORY_TTM_VS_ALL_CLERKS.equals(name)) return true;

		// default: DISABLED
		return false;
	}
	@Override
	public int getGraphDataHistoryTimeInterval(String name)
	{
		// Keep interval: default is 60 minutes
		return super.getGraphDataHistoryTimeInterval(name);
	}

	//--------------------------------------------------------------------------------------
	//--------------------------------------------------------------------------------------
	//-- Alarm Handling
	//--------------------------------------------------------------------------------------
	//--------------------------------------------------------------------------------------
	@Override
	public void sendAlarmRequest()
	{
//		if ( ! hasDiffData() )
//			return;
		
		if ( ! AlarmHandler.hasInstance() )
			return;

		AlarmHandler alarmHandler = AlarmHandler.getInstance();
		
		CountersModel cm = this;

		boolean debugPrint = Configuration.getCombinedConfiguration().getBooleanProperty("sendAlarmRequest.debug", _logger.isDebugEnabled());

		
		//-------------------------------------------------------
		// USERSTORE_TOKENPERM 
		//-------------------------------------------------------
		if (isSystemAlarmsForColumnEnabledAndInTimeRange("USERSTORE_TOKENPERM"))
		{
			int sizeMb = cm.getAbsValueAsInteger("USERSTORE_TOKENPERM", "SizeMb", -1);
			int threshold = Configuration.getCombinedConfiguration().getIntProperty(PROPKEY_alarm_USERSTORE_TOKENPERM, DEFAULT_alarm_USERSTORE_TOKENPERM);

			if (debugPrint || _logger.isDebugEnabled())
				System.out.println("##### sendAlarmRequest(" + cm.getName() + "): threshold=" + threshold + ", sizeMb='" + sizeMb);

			if (sizeMb > threshold)
			{
				String extendedDescText = "Possibly resolved by executing: DBCC FREESYSTEMCACHE('TokenAndPermUserStore'), also you may look at Startup traceflag: 4610, 4618";
				String extendedDescHtml = extendedDescText;

				// Possibly: Create a dedicated graph for 'USERSTORE_TOKENPERM' (if 'USERSTORE_TOKENPERM' is NOT part of the top clerks)
				extendedDescHtml += "<br><br>" + cm.getGraphDataHistoryAsHtmlImage(GRAPH_NAME_MEMORY_CLERKS_TOP, "USERSTORE_TOKENPERM");
				extendedDescHtml += "<br><br>" + cm.getGraphDataHistoryAsHtmlImage(GRAPH_NAME_MEMORY_TTM_VS_ALL_CLERKS);

				// Create the alarm
				AlarmEvent ae = new AlarmEventMemoryClerkWarning(cm, threshold, sizeMb, "USERSTORE_TOKENPERM");

				ae.setExtendedDescription(extendedDescText, extendedDescHtml);
				
				alarmHandler.addAlarm( ae );
			}
		}
	}

	public static final String  PROPKEY_alarm_USERSTORE_TOKENPERM  = CM_NAME + ".alarm.system.if.USERSTORE_TOKENPERM.gt";
	public static final int     DEFAULT_alarm_USERSTORE_TOKENPERM  = 2 * 1024;
	
	@Override
	public List<CmSettingsHelper> getLocalAlarmSettings()
	{
		Configuration conf = Configuration.getCombinedConfiguration();
		List<CmSettingsHelper> list = new ArrayList<>();

		CmSettingsHelper.Type isAlarmSwitch = CmSettingsHelper.Type.IS_ALARM_SWITCH;
		
		list.add(new CmSettingsHelper("USERSTORE_TOKENPERM", isAlarmSwitch, PROPKEY_alarm_USERSTORE_TOKENPERM, Integer.class, conf.getIntProperty(PROPKEY_alarm_USERSTORE_TOKENPERM, DEFAULT_alarm_USERSTORE_TOKENPERM), DEFAULT_alarm_USERSTORE_TOKENPERM, "If 'SizeMB' for type='USERSTORE_TOKENPERM', name='TokenAndPermUserStore' is greater than ## then send 'AlarmEventMemoryClerkWarning'." ));

		return list;
	}
}
