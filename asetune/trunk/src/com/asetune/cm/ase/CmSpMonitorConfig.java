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
package com.asetune.cm.ase;

import java.math.BigDecimal;
import java.sql.Connection;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

import org.apache.log4j.Logger;

import com.asetune.ICounterController;
import com.asetune.IGuiController;
import com.asetune.alarm.AlarmHandler;
import com.asetune.alarm.events.AlarmEventConfigResourceIsLow;
import com.asetune.alarm.events.AlarmEventConfigResourceIsUsedUp;
import com.asetune.alarm.events.AlarmEventProcedureCacheLowOnMemory;
import com.asetune.cm.CmSettingsHelper;
import com.asetune.cm.CmSybMessageHandler;
import com.asetune.cm.CounterSetTemplates;
import com.asetune.cm.CounterSetTemplates.Type;
import com.asetune.cm.CountersModel;
import com.asetune.graph.TrendGraphDataPoint;
import com.asetune.graph.TrendGraphDataPoint.LabelType;
import com.asetune.gui.MainFrame;
import com.asetune.gui.TrendGraph;
import com.asetune.utils.Configuration;

/**
 * @author Goran Schwarz (goran_schwarz@hotmail.com)
 */
public class CmSpMonitorConfig
extends CountersModel
{
	private static Logger        _logger          = Logger.getLogger(CmSpMonitorConfig.class);
	private static final long    serialVersionUID = 1L;

	public static final String   CM_NAME          = CmSpMonitorConfig.class.getSimpleName();
	public static final String   SHORT_NAME       = "sp_monitorconfig";
	public static final String   HTML_DESC        = 
		"<html>" +
		"Executes: sp_monitorconfig 'all'... <br>" +
		"<b>Note</b>: set postpone time to 10 minutes or so, we don't need to sample this that often." +
		"</html>";

	public static final String   GROUP_NAME       = MainFrame.TCP_GROUP_SERVER;
	public static final String   GUI_ICON_FILE    = "images/"+CM_NAME+".png";

	public static final long     NEED_SRV_VERSION = 0;
	public static final long     NEED_CE_VERSION  = 0;

	public static final String[] MON_TABLES       = new String[] {"monitorconfig"};
	public static final String[] NEED_ROLES       = new String[] {"sa_role"};
	public static final String[] NEED_CONFIG      = new String[] {};

	public static final String[] PCT_COLUMNS      = new String[] {};
	public static final String[] DIFF_COLUMNS     = new String[] {};

	public static final boolean  NEGATIVE_DIFF_COUNTERS_TO_ZERO = false;
	public static final boolean  IS_SYSTEM_CM                   = true;
	public static final int      DEFAULT_POSTPONE_TIME          = 60;
	public static final int      DEFAULT_QUERY_TIMEOUT          = CountersModel.DEFAULT_sqlQueryTimeout;

	@Override public int     getDefaultPostponeTime()                 { return DEFAULT_POSTPONE_TIME; }
	@Override public int     getDefaultQueryTimeout()                 { return DEFAULT_QUERY_TIMEOUT; }
	@Override public boolean getDefaultIsNegativeDiffCountersToZero() { return NEGATIVE_DIFF_COUNTERS_TO_ZERO; }
	@Override public Type    getTemplateLevel()                       { return Type.MEDIUM; }

	/**
	 * FACTORY  method to create the object
	 */
	public static CountersModel create(ICounterController counterController, IGuiController guiController)
	{
		if (guiController != null && guiController.hasGUI())
			guiController.splashWindowProgress("Loading: Counter Model '"+CM_NAME+"'");

		return new CmSpMonitorConfig(counterController, guiController);
	}

	public CmSpMonitorConfig(ICounterController counterController, IGuiController guiController)
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
	
	public static final String GRAPH_NAME_PROC_CACHE_PCT_USAGE = "ProcCachePctUsage";
	public static final String GRAPH_NAME_PROC_CACHE_MEM_USAGE = "ProcCacheMemUsage";
	public static final String GRAPH_NAME_METADATA_PCT_USAGE   = "MetaDataPctUsage";
	public static final String GRAPH_NAME_METADATA_ACTIVE      = "MetaDataActive";

//	@Override
//	protected TabularCntrPanel createGui()
//	{
//		return new CmSpMonitorConfigPanel(this);
//	}

	@Override
	public String[] getDependsOnConfigForVersion(Connection conn, long srvVersion, boolean isClusterEnabled)
	{
		return NEED_CONFIG;
	}

	@Override
	protected CmSybMessageHandler createSybMessageHandler()
	{
		CmSybMessageHandler msgHandler = super.createSybMessageHandler();

		//msgHandler.addDiscardMsgStr("Usage information at date and time");
		msgHandler.addDiscardMsgNum(0);

		return msgHandler;
	}

	@Override
	public List<String> getPkForVersion(Connection conn, long srvVersion, boolean isClusterEnabled)
	{
//		// NO PK is needed, because NO diff calc is done.
//		return null;

		List <String> pkCols = new LinkedList<String>();

		pkCols.add("Name");

		return pkCols;
	}

	@Override
	public String getSqlForVersion(Connection conn, long srvVersion, boolean isClusterEnabled)
	{
		String sql = "exec sp_monitorconfig 'all'";
		return sql;
	}

//	/**
//	 * Fix ResultSetMetaData to use minimum of 30 chars for 'Name'
//	 */
//	@Override
//	public void localCalculation(CounterSample newSample)
//	{
//		ResultSetMetaData rsmd = getResultSetMetaData();
//		if (rsmd instanceof ResultSetMetaDataCached)
//		{
//			ResultSetMetaDataCached rsmdc = (ResultSetMetaDataCached) rsmd;
//
//			try
//			{
//    			int pos = rsmdc.findColumn("Name");
//    			if (pos >= 0)
//    			{
//    				int curLen = rsmdc.getColumnDisplaySize(pos);
//    				int desLen = 30;
//    				if (curLen < desLen)
//    				{
//    					rsmdc.setColumnDisplaySize(pos, 30);
//    					_logger.debug(getName()+": Adjusting lenth of ResultSet for column name 'Name' at position "+pos+" from "+curLen+" to "+desLen+".");
//    					_logger.info(getName()+": Adjusting lenth of ResultSet for column name 'Name' at position "+pos+" from "+curLen+" to "+desLen+".");
//    				}
//    			}
//			}
//			catch(SQLException ignore) { /* ignore */ }
//		}
//	}

	private void addTrendGraphs()
	{
//		String[] pctLabels = new String[] { "Percent Usage" };
////		String[] memLabels = new String[] { "Total Memory", "Free", "Used", "Max Ever Used", "Reused Count" };
//		String[] memLabels = new String[] { "Total Memory MB", "Free MB", "Used MB", "Max Ever Used MB"};
//
//		addTrendGraphData(GRAPH_NAME_PROC_CACHE_PCT_USAGE, new TrendGraphDataPoint(GRAPH_NAME_PROC_CACHE_PCT_USAGE, pctLabels, LabelType.Static));
//		addTrendGraphData(GRAPH_NAME_PROC_CACHE_MEM_USAGE, new TrendGraphDataPoint(GRAPH_NAME_PROC_CACHE_MEM_USAGE, memLabels, LabelType.Static));

		addTrendGraph(GRAPH_NAME_PROC_CACHE_PCT_USAGE,
			"Procedure Cache Usage in Percent", 	                                 // Menu CheckBox text
			"Procedure Cache Usage in Percent ("+GROUP_NAME+"->"+SHORT_NAME+")", // Label 
			new String[] { "Percent Usage" }, 
			LabelType.Static,
			TrendGraphDataPoint.Category.SRV_CONFIG,
			true, // is Percent Graph
			false, // visible at start
			0,     // graph is valid from Server Version. 0 = All Versions; >0 = Valid from this version and above 
			-1);   // minimum height

		// GRAPH
		addTrendGraph(GRAPH_NAME_PROC_CACHE_MEM_USAGE,
			"Procedure Cache Usage in MB", 	                                 // Menu CheckBox text
			"Procedure Cache Usage in MB ("+GROUP_NAME+"->"+SHORT_NAME+")", // Label 
			new String[] { "Total Memory MB", "Free MB", "Used MB", "Max Ever Used MB"},
			LabelType.Static,
			TrendGraphDataPoint.Category.SRV_CONFIG,
			false, // is Percent Graph
			false, // visible at start
			0,     // graph is valid from Server Version. 0 = All Versions; >0 = Valid from this version and above 
			-1);   // minimum height

		// GRAPH
		addTrendGraph(GRAPH_NAME_METADATA_PCT_USAGE,
			"MetaData Cache Usage in Percent", 	                                 // Menu CheckBox text
			"MetaData Cache Usage in Percent ("+GROUP_NAME+"->"+SHORT_NAME+")", // Label 
			new String[] { "number of open objects", "number of open indexes", "number of open partitions", "number of locks", "number of sort buffers", "number of user connections", "number of worker processes"},
			LabelType.Static,
			TrendGraphDataPoint.Category.SRV_CONFIG,
			true, // is Percent Graph
			false, // visible at start
			0,     // graph is valid from Server Version. 0 = All Versions; >0 = Valid from this version and above 
			-1);   // minimum height

		// GRAPH
		addTrendGraph(GRAPH_NAME_METADATA_ACTIVE,
			"MetaData Cache Active Count", 	                                 // Menu CheckBox text
			"MetaData Cache Active Count ("+GROUP_NAME+"->"+SHORT_NAME+")", // Label 
			new String[] { "number of open objects", "number of open indexes", "number of open partitions", "number of locks", "number of sort buffers", "number of user connections", "number of worker processes"},
			LabelType.Static,
			TrendGraphDataPoint.Category.SRV_CONFIG,
			false, // is Percent Graph
			false, // visible at start
			0,     // graph is valid from Server Version. 0 = All Versions; >0 = Valid from this version and above 
			-1);   // minimum height


//		// if GUI
//		if (getGuiController() != null && getGuiController().hasGUI())
//		{
//			// GRAPH
//			TrendGraph tg = null;
//			tg = new TrendGraph(GRAPH_NAME_PROC_CACHE_PCT_USAGE,
//				"Procedure Cache Usage in Percent", 	                                 // Menu CheckBox text
//				"Procedure Cache Usage in Percent ("+GROUP_NAME+"->"+SHORT_NAME+")", // Label 
//				pctLabels, 
//				true, // is Percent Graph
//				this, 
//				false, // visible at start
//				0,     // graph is valid from Server Version. 0 = All Versions; >0 = Valid from this version and above 
//				-1);   // minimum height
//			addTrendGraph(tg.getName(), tg, true);
//
//			// GRAPH
//			tg = new TrendGraph(GRAPH_NAME_PROC_CACHE_MEM_USAGE,
//				"Procedure Cache Usage in MB", 	                                 // Menu CheckBox text
//				"Procedure Cache Usage in MB ("+GROUP_NAME+"->"+SHORT_NAME+")", // Label 
//				memLabels, 
//				false, // is Percent Graph
//				this, 
//				false, // visible at start
//				0,     // graph is valid from Server Version. 0 = All Versions; >0 = Valid from this version and above 
//				-1);   // minimum height
//			addTrendGraph(tg.getName(), tg, true);
//		}
	}

	@Override
	public void updateGraphData(TrendGraphDataPoint tgdp)
	{
		if (GRAPH_NAME_PROC_CACHE_PCT_USAGE.equals(tgdp.getName()))
		{
			// Get a array of rowId's where the column 'Name' has the value 'procedure cache size'
			int[] rqRows = this.getAbsRowIdsWhere("Name", "procedure cache size");
			if (rqRows == null)
				_logger.warn("When updateGraphData for '"+tgdp.getName()+"', getAbsRowIdsWhere('Name', 'procedure cache size'), retuned null, so I can't do more here.");
			else
			{
				Double pctAct   = this.getAbsValueAsDouble(rqRows[0], "Pct_act");

				Double[] data  = new Double[1];
				data[0]  = pctAct;

				if (_logger.isDebugEnabled())
					_logger.debug(tgdp.getName()+": pctAct="+pctAct);

				// Set the values
				tgdp.setDataPoint(this.getTimestamp(), data);
//				tgdp.setDate(this.getTimestamp());
//				tgdp.setData(data);
			}
		}

		if (GRAPH_NAME_PROC_CACHE_MEM_USAGE.equals(tgdp.getName()))
		{
			// Get a array of rowId's where the column 'Name' has the value 'procedure cache size'
			int[] rqRows = this.getAbsRowIdsWhere("Name", "procedure cache size");
			if (rqRows == null)
				_logger.warn("When updateGraphData for '"+tgdp.getName()+"', getAbsRowIdsWhere('Name', 'procedure cache size'), retuned null, so I can't do more here.");
			else
			{
				Double numFree   = this.getAbsValueAsDouble(rqRows[0], "Num_free");
				Double numActive = this.getAbsValueAsDouble(rqRows[0], "Num_active");
				Double maxUsed   = this.getAbsValueAsDouble(rqRows[0], "Max_Used");
				//Double reuseCnt  = this.getAbsValueAsDouble(rqRows[0], "Reuse_cnt");
				
				if (_logger.isDebugEnabled())
					_logger.debug(tgdp.getName()+": numFree="+numFree+", numActive="+numActive+", maxUsed="+maxUsed+".");

				if (numFree   != null) numFree   = numFree   / 512.0;
				if (numActive != null) numActive = numActive / 512.0;
				if (maxUsed   != null) maxUsed   = maxUsed   / 512.0;
				
				//Double[] data  = new Double[5];
				Double[] data  = new Double[4];
				data[0]  = new BigDecimal( numFree + numActive ).setScale(1, BigDecimal.ROUND_HALF_EVEN).doubleValue();
				data[1]  = new BigDecimal( numFree             ).setScale(1, BigDecimal.ROUND_HALF_EVEN).doubleValue();
				data[2]  = new BigDecimal( numActive           ).setScale(1, BigDecimal.ROUND_HALF_EVEN).doubleValue();
				data[3]  = new BigDecimal( maxUsed             ).setScale(1, BigDecimal.ROUND_HALF_EVEN).doubleValue();
				//data[4]  = reuseCnt;

				if (_logger.isDebugEnabled())
					_logger.debug(tgdp.getName()+": Total Memory MB="+data[0]+", Free MB="+data[1]+", Used MB="+data[2]+", Max Ever Used MB="+data[3]);

				// Set the values
				tgdp.setDataPoint(this.getTimestamp(), data);
//				tgdp.setDate(this.getTimestamp());
//				tgdp.setData(data);
			}
		}

		if (GRAPH_NAME_METADATA_PCT_USAGE.equals(tgdp.getName()))
		{
			Double[] arr = new Double[7];
			
			// Note the prefix: 'SQLServer' or 'MSSQL$@@servicename' is removed in SQL query
			String pk1 = createPkStr("number of open objects"    );
			String pk2 = createPkStr("number of open indexes"    );
			String pk3 = createPkStr("number of open partitions" );
			String pk4 = createPkStr("number of locks"           );
			String pk5 = createPkStr("number of sort buffers"    );
			String pk6 = createPkStr("number of user connections");
			String pk7 = createPkStr("number of worker processes");
			
			Double val1 = this.getAbsValueAsDouble(pk1, "Pct_act");
			Double val2 = this.getAbsValueAsDouble(pk2, "Pct_act");
			Double val3 = this.getAbsValueAsDouble(pk3, "Pct_act");
			Double val4 = this.getAbsValueAsDouble(pk3, "Pct_act");
			Double val5 = this.getAbsValueAsDouble(pk3, "Pct_act");
			Double val6 = this.getAbsValueAsDouble(pk3, "Pct_act");
			Double val7 = this.getAbsValueAsDouble(pk3, "Pct_act");
			
			if (val1 != null && val2 != null && val3 != null && val4 != null && val5 != null && val6 != null && val7 != null)
			{
				arr[0] = val1;
				arr[1] = val2;
				arr[2] = val3;
				arr[3] = val4;
				arr[4] = val5;
				arr[5] = val6;
				arr[6] = val7;

				// Set the values
				tgdp.setDataPoint(this.getTimestamp(), arr);
			}
			else
			{
				TrendGraph tg = getTrendGraph(tgdp.getName());
				if (tg != null)
					tg.setWarningLabel("Failed to get value(s) for pk-row: '"+pk1+"'='"+val1+"', '"+pk2+"'='"+val2+"', '"+pk3+"'='"+val3+"', '"+pk4+"'='"+val4+"', '"+pk5+"'='"+val5+"', '"+pk6+"'='"+val6+"', '"+pk7+"'='"+val7+"'.");
			}
		}

		if (GRAPH_NAME_METADATA_ACTIVE.equals(tgdp.getName()))
		{
			Double[] arr = new Double[7];
			
			// Note the prefix: 'SQLServer' or 'MSSQL$@@servicename' is removed in SQL query
			String pk1 = createPkStr("number of open objects"    );
			String pk2 = createPkStr("number of open indexes"    );
			String pk3 = createPkStr("number of open partitions" );
			String pk4 = createPkStr("number of locks"           );
			String pk5 = createPkStr("number of sort buffers"    );
			String pk6 = createPkStr("number of user connections");
			String pk7 = createPkStr("number of worker processes");
			
			Double val1 = this.getAbsValueAsDouble(pk1, "Num_active");
			Double val2 = this.getAbsValueAsDouble(pk2, "Num_active");
			Double val3 = this.getAbsValueAsDouble(pk3, "Num_active");
			Double val4 = this.getAbsValueAsDouble(pk3, "Num_active");
			Double val5 = this.getAbsValueAsDouble(pk3, "Num_active");
			Double val6 = this.getAbsValueAsDouble(pk3, "Num_active");
			Double val7 = this.getAbsValueAsDouble(pk3, "Num_active");
			
			if (val1 != null && val2 != null && val3 != null && val4 != null && val5 != null && val6 != null && val7 != null)
			{
				arr[0] = val1;
				arr[1] = val2;
				arr[2] = val3;
				arr[3] = val4;
				arr[4] = val5;
				arr[5] = val6;
				arr[6] = val7;

				// Set the values
				tgdp.setDataPoint(this.getTimestamp(), arr);
			}
			else
			{
				TrendGraph tg = getTrendGraph(tgdp.getName());
				if (tg != null)
					tg.setWarningLabel("Failed to get value(s) for pk-row: '"+pk1+"'='"+val1+"', '"+pk2+"'='"+val2+"', '"+pk3+"'='"+val3+"', '"+pk4+"'='"+val4+"', '"+pk5+"'='"+val5+"', '"+pk6+"'='"+val6+"', '"+pk7+"'='"+val7+"'.");
			}
		}
	}


	@Override
	public void sendAlarmRequest()
	{
		if ( ! hasAbsData() )
			return;
		
		if ( ! AlarmHandler.hasInstance() )
			return;

		CountersModel cm = this;

		boolean debugPrint = System.getProperty("sendAlarmRequest.debug", "false").equalsIgnoreCase("true");
//debugPrint = true;

//		// Get a array of rowId's where the column 'Name' has the value 'procedure cache size'
//		int[] rqRows = this.getAbsRowIdsWhere("Name", "procedure cache size");
//		if (rqRows == null)
//			_logger.warn("When checking for alarms in '"+getName()+"', getAbsRowIdsWhere('Name', 'procedure cache size'), retuned null, so I can't do more here.");
//		else
//		{
//			//-------------------------------------------------------
//			// Procedure Cache Usage
//			//-------------------------------------------------------
//			if (isSystemAlarmsForColumnEnabledAndInTimeRange("ProcedureCacheUsage"))
//			{
//				Double pctAct    = this.getAbsValueAsDouble(rqRows[0], "Pct_act");
//				Double numFree   = this.getAbsValueAsDouble(rqRows[0], "Num_free");
////				Double numActive = this.getAbsValueAsDouble(rqRows[0], "Num_active");
////				Double maxUsed   = this.getAbsValueAsDouble(rqRows[0], "Max_Used");
////				Double reuseCnt  = this.getAbsValueAsDouble(rqRows[0], "Reuse_cnt");
//				
//				if (numFree   != null) numFree   = numFree   / 512.0;
////				if (numActive != null) numActive = numActive / 512.0;
////				if (maxUsed   != null) maxUsed   = maxUsed   / 512.0;
//				
//				if (pctAct != null)
//				{
//					if (debugPrint || _logger.isDebugEnabled())
//						System.out.println("##### sendAlarmRequest("+cm.getName()+"): pctAct='"+pctAct+"', numFree='"+numFree+"'.");
//
//					int threshold = Configuration.getCombinedConfiguration().getIntProperty(PROPKEY_alarm_ProcedureCacheUsagePct, DEFAULT_alarm_ProcedureCacheUsagePct);
//					if (pctAct.intValue() > threshold)
//					{
//						AlarmHandler.getInstance().addAlarm( 
//							new AlarmEventProcedureCacheLowOnMemory(cm, numFree, pctAct, threshold) );
//					}
//				}
//			}
//		}
		
		for (int r=0; r<cm.getAbsRowCount(); r++)
		{
			boolean didAlarm = false;

			String cfgName   = cm.getAbsString(r,        "Name");
			Double pctAct    = cm.getAbsValueAsDouble(r, "Pct_act");
			Double numFree   = cm.getAbsValueAsDouble(r, "Num_free");
			Double numActive = cm.getAbsValueAsDouble(r, "Num_active");

			if (pctAct == null || numFree == null)
				continue;

			if ("procedure cache size"           .equals(cfgName) && isSystemAlarmsForColumnEnabledAndInTimeRange("ProcedureCacheUsage"))
			{
				int threshold = Configuration.getCombinedConfiguration().getIntProperty(PROPKEY_alarm_ProcedureCacheUsagePct, DEFAULT_alarm_ProcedureCacheUsagePct);
				if (pctAct.intValue() > threshold)
				{
					Double numFreeMb = numFree / 512.0;

					AlarmHandler.getInstance().addAlarm(new AlarmEventProcedureCacheLowOnMemory(cm, numFreeMb, pctAct, threshold) );
					didAlarm = true;
				}
			}
			else if ("number of open objects"    .equals(cfgName) && isSystemAlarmsForColumnEnabledAndInTimeRange("NumberOfOpenObjectsPct"))
			{
				int threshold = Configuration.getCombinedConfiguration().getIntProperty(PROPKEY_alarm_NumberOfOpenObjectsPct, DEFAULT_alarm_NumberOfOpenObjectsPct);
				if (pctAct.intValue() > threshold)
				{
					AlarmHandler.getInstance().addAlarm(new AlarmEventConfigResourceIsLow(cm, cfgName, numFree, numActive, pctAct, threshold) );
					didAlarm = true;
				}
			}
			else if ("number of open partitions" .equals(cfgName) && isSystemAlarmsForColumnEnabledAndInTimeRange("NumberOfOpenPartitionsPct"))
			{
				int threshold = Configuration.getCombinedConfiguration().getIntProperty(PROPKEY_alarm_NumberOfOpenPartitionsPct, DEFAULT_alarm_NumberOfOpenPartitionsPct);
				if (pctAct.intValue() > threshold)
				{
					AlarmHandler.getInstance().addAlarm(new AlarmEventConfigResourceIsLow(cm, cfgName, numFree, numActive, pctAct, threshold) );
					didAlarm = true;
				}
			}
			else if ("number of open indexes"    .equals(cfgName) && isSystemAlarmsForColumnEnabledAndInTimeRange("NumberOfOpenIndexesPct"))
			{
				int threshold = Configuration.getCombinedConfiguration().getIntProperty(PROPKEY_alarm_NumberOfOpenIndexesPct, DEFAULT_alarm_NumberOfOpenIndexesPct);
				if (pctAct.intValue() > threshold)
				{
					AlarmHandler.getInstance().addAlarm(new AlarmEventConfigResourceIsLow(cm, cfgName, numFree, numActive, pctAct, threshold) );
					didAlarm = true;
				}
			}
			else if ("number of open databases"  .equals(cfgName) && isSystemAlarmsForColumnEnabledAndInTimeRange("NumberOfOpenDatabasesPct"))
			{
				int threshold = Configuration.getCombinedConfiguration().getIntProperty(PROPKEY_alarm_NumberOfOpenDatabasesPct, DEFAULT_alarm_NumberOfOpenDatabasesPct);
				if (pctAct.intValue() > threshold)
				{
					AlarmHandler.getInstance().addAlarm(new AlarmEventConfigResourceIsLow(cm, cfgName, numFree, numActive, pctAct, threshold) );
					didAlarm = true;
				}
			}
			else if ("number of locks"           .equals(cfgName) && isSystemAlarmsForColumnEnabledAndInTimeRange("NumberOfLocksPct"))
			{
				// AlarmEventConfigResourceIsUsedUp: is called from CounterModel.java 
				int threshold = Configuration.getCombinedConfiguration().getIntProperty(PROPKEY_alarm_NumberOfLocksPct, DEFAULT_alarm_NumberOfLocksPct);
				if (pctAct.intValue() > threshold)
				{
					AlarmHandler.getInstance().addAlarm(new AlarmEventConfigResourceIsLow(cm, cfgName, numFree, numActive, pctAct, threshold) );
					didAlarm = true;
				}
			}
			else if ("number of user connections".equals(cfgName) && isSystemAlarmsForColumnEnabledAndInTimeRange("NumberOfUserConnectionsPct"))
			{
				int threshold = Configuration.getCombinedConfiguration().getIntProperty(PROPKEY_alarm_NumberOfUserConnectionsPct, DEFAULT_alarm_NumberOfUserConnectionsPct);
				if (pctAct.intValue() > threshold)
				{
					AlarmHandler.getInstance().addAlarm(new AlarmEventConfigResourceIsLow(cm, cfgName, numFree, numActive, pctAct, threshold) );
					didAlarm = true;
				}
			}
			
			// OUT OF
			int outOfThreshold = 2;
			if ("number of open databases".equals(cfgName))
				outOfThreshold = 0;
			
			if (numFree <= outOfThreshold) // 0 is REALLY OUT OF it (but normally: it do not stay at 0 for a very long time... so lets use some other low value...)
			{
				if ("procedure cache size".equals(cfgName))
				{
					// EMULATE: Error=701, Severity=17, Text=There is not enough procedure cache to run this procedure, trigger, or SQL batch. Retry later, or ask your SA to reconfigure ASE with more procedure cache.
					AlarmHandler.getInstance().addAlarm(new AlarmEventConfigResourceIsUsedUp(cm, cfgName, 701, "Configuration '"+cfgName+"' has ZERO free slots (numFree="+numFree+", threshold="+outOfThreshold+"). There is not enough procedure cache to run this procedure, trigger, or SQL batch. Retry later, or ask your SA to reconfigure ASE with more procedure cache.") );
					didAlarm = true;
				}
				else if ("number of open objects".equals(cfgName))
				{
					AlarmHandler.getInstance().addAlarm(new AlarmEventConfigResourceIsUsedUp(cm, cfgName, -1, "Configuration '"+cfgName+"' has ZERO free slots (numFree="+numFree+", threshold="+outOfThreshold+"). The server will re-use older entries, which will degrade performance. Please add more '"+cfgName+"'.") );
					didAlarm = true;
				}
				else if ("number of open partitions".equals(cfgName))
				{
					AlarmHandler.getInstance().addAlarm(new AlarmEventConfigResourceIsUsedUp(cm, cfgName, -1, "Configuration '"+cfgName+"' has ZERO free slots (numFree="+numFree+", threshold="+outOfThreshold+"). The server will re-use older entries, which will degrade performance. Please add more '"+cfgName+"'.") );
					didAlarm = true;
				}
				else if ("number of open indexes".equals(cfgName))
				{
					AlarmHandler.getInstance().addAlarm(new AlarmEventConfigResourceIsUsedUp(cm, cfgName, -1, "Configuration '"+cfgName+"' has ZERO free slots (numFree="+numFree+", threshold="+outOfThreshold+"). The server will re-use older entries, which will degrade performance. Please add more '"+cfgName+"'.") );
					didAlarm = true;
				}
				else if ("number of open databases".equals(cfgName))
				{
					AlarmHandler.getInstance().addAlarm(new AlarmEventConfigResourceIsUsedUp(cm, cfgName, -1, "Configuration '"+cfgName+"' has ZERO free slots (numFree="+numFree+", threshold="+outOfThreshold+"). The server will re-use older entries, which will degrade performance. Please add more '"+cfgName+"'.") );
					didAlarm = true;
				}
				else if ("number of locks".equals(cfgName))
				{
					// EMULATE: Error=1204, Severity=17, Text=ASE has run out of LOCKS. Re-run your command when there are fewer active users, or contact a user with System Administrator (SA) role to reconfigure ASE with more LOCKS.
					AlarmHandler.getInstance().addAlarm(new AlarmEventConfigResourceIsUsedUp(cm, cfgName, 1204, "Configuration '"+cfgName+"' has ZERO free slots (numFree="+numFree+", threshold="+outOfThreshold+"). ASE has run out of LOCKS. Re-run your command when there are fewer active users, or contact a user with System Administrator (SA) role to reconfigure ASE with more LOCKS.") );
					didAlarm = true;
				}
				else if ("number of user connections".equals(cfgName))
				{
					// EMULATE: Error=1601, Severity=21, Text=There are not enough 'user connections' available to start a new process. Retry when there are fewer active users, or ask your System Administrator to reconfigure ASE with more user connections.
					AlarmHandler.getInstance().addAlarm(new AlarmEventConfigResourceIsUsedUp(cm, cfgName, 1601, "Configuration '"+cfgName+"' has ZERO free slots (numFree="+numFree+", threshold="+outOfThreshold+"). There are not enough 'user connections' available to start a new process. Retry when there are fewer active users, or ask your System Administrator to reconfigure ASE with more user connections.") );
					didAlarm = true;
				}
			}
			
			if ( didAlarm && (debugPrint || _logger.isDebugEnabled()) )
				System.out.println("##### sendAlarmRequest("+cm.getName()+"): name='"+cfgName+"', pctAct='"+pctAct+"', numFree='"+numFree+"'.");
		}

	} // end: method

	public static final String  PROPKEY_alarm_ProcedureCacheUsagePct     = CM_NAME + ".alarm.system.if.ProcedureCacheUsagePct.gt";
	public static final int     DEFAULT_alarm_ProcedureCacheUsagePct     = 80;
	
	public static final String  PROPKEY_alarm_NumberOfOpenObjectsPct     = CM_NAME + ".alarm.system.if.NumberOfOpenObjectsPct.gt";
	public static final int     DEFAULT_alarm_NumberOfOpenObjectsPct     = 90;
	
	public static final String  PROPKEY_alarm_NumberOfOpenPartitionsPct  = CM_NAME + ".alarm.system.if.NumberOfOpenPartitionsPct.gt";
	public static final int     DEFAULT_alarm_NumberOfOpenPartitionsPct  = 90;
	
	public static final String  PROPKEY_alarm_NumberOfOpenIndexesPct     = CM_NAME + ".alarm.system.if.NumberOfOpenIndexesPct.gt";
	public static final int     DEFAULT_alarm_NumberOfOpenIndexesPct     = 90;
	
	public static final String  PROPKEY_alarm_NumberOfOpenDatabasesPct   = CM_NAME + ".alarm.system.if.NumberOfOpenDatabasesPct.gt";
	public static final int     DEFAULT_alarm_NumberOfOpenDatabasesPct   = 95;
//	public static final int     DEFAULT_alarm_NumberOfOpenDatabasesPct   = 90;
	
	public static final String  PROPKEY_alarm_NumberOfLocksPct           = CM_NAME + ".alarm.system.if.NumberOfLocksPct.gt";
	public static final int     DEFAULT_alarm_NumberOfLocksPct           = 90;
	
	public static final String  PROPKEY_alarm_NumberOfUserConnectionsPct = CM_NAME + ".alarm.system.if.NumberOfUserConnectionsPct.gt";
	public static final int     DEFAULT_alarm_NumberOfUserConnectionsPct = 90;
	
	@Override
	public List<CmSettingsHelper> getLocalAlarmSettings()
	{
		Configuration conf = Configuration.getCombinedConfiguration();
		List<CmSettingsHelper> list = new ArrayList<>();
		
		list.add(new CmSettingsHelper("ProcedureCacheUsagePct",     PROPKEY_alarm_ProcedureCacheUsagePct    , Integer.class, conf.getIntProperty(PROPKEY_alarm_ProcedureCacheUsagePct    , DEFAULT_alarm_ProcedureCacheUsagePct    ), DEFAULT_alarm_ProcedureCacheUsagePct    , "If '"+"ProcedureCacheUsagePct"    +"' is greater than ## Percent then send 'AlarmEventProcedureCacheLowOnMemory'." ));
		list.add(new CmSettingsHelper("NumberOfOpenObjectsPct",     PROPKEY_alarm_NumberOfOpenObjectsPct    , Integer.class, conf.getIntProperty(PROPKEY_alarm_NumberOfOpenObjectsPct    , DEFAULT_alarm_NumberOfOpenObjectsPct    ), DEFAULT_alarm_NumberOfOpenObjectsPct    , "If '"+"NumberOfOpenObjectsPct"    +"' is greater than ## Percent then send 'AlarmEvent...'." ));
		list.add(new CmSettingsHelper("NumberOfOpenPartitionsPct",  PROPKEY_alarm_NumberOfOpenPartitionsPct , Integer.class, conf.getIntProperty(PROPKEY_alarm_NumberOfOpenPartitionsPct , DEFAULT_alarm_NumberOfOpenPartitionsPct ), DEFAULT_alarm_NumberOfOpenPartitionsPct , "If '"+"NumberOfOpenPartitionsPct" +"' is greater than ## Percent then send 'AlarmEvent...'." ));
		list.add(new CmSettingsHelper("NumberOfOpenIndexesPct",     PROPKEY_alarm_NumberOfOpenIndexesPct    , Integer.class, conf.getIntProperty(PROPKEY_alarm_NumberOfOpenIndexesPct    , DEFAULT_alarm_NumberOfOpenIndexesPct    ), DEFAULT_alarm_NumberOfOpenIndexesPct    , "If '"+"NumberOfOpenIndexesPct"    +"' is greater than ## Percent then send 'AlarmEvent...'." ));
		list.add(new CmSettingsHelper("NumberOfOpenDatabasesPct",   PROPKEY_alarm_NumberOfOpenDatabasesPct  , Integer.class, conf.getIntProperty(PROPKEY_alarm_NumberOfOpenDatabasesPct  , DEFAULT_alarm_NumberOfOpenDatabasesPct  ), DEFAULT_alarm_NumberOfOpenDatabasesPct  , "If '"+"NumberOfOpenDatabasesPct"  +"' is greater than ## Percent then send 'AlarmEvent...'." ));
		list.add(new CmSettingsHelper("NumberOfLocksPct",           PROPKEY_alarm_NumberOfLocksPct          , Integer.class, conf.getIntProperty(PROPKEY_alarm_NumberOfLocksPct          , DEFAULT_alarm_NumberOfLocksPct          ), DEFAULT_alarm_NumberOfLocksPct          , "If '"+"NumberOfLocksPct"          +"' is greater than ## Percent then send 'AlarmEvent...'." ));
		list.add(new CmSettingsHelper("NumberOfUserConnectionsPct", PROPKEY_alarm_NumberOfUserConnectionsPct, Integer.class, conf.getIntProperty(PROPKEY_alarm_NumberOfUserConnectionsPct, DEFAULT_alarm_NumberOfUserConnectionsPct), DEFAULT_alarm_NumberOfUserConnectionsPct, "If '"+"NumberOfUserConnectionsPct"+"' is greater than ## Percent then send 'AlarmEvent...'." ));

		return list;
	}

}
