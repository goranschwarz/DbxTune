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
package com.asetune.cm.sqlserver;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import javax.naming.NameNotFoundException;

import org.apache.log4j.Logger;

import com.asetune.ICounterController;
import com.asetune.IGuiController;
import com.asetune.alarm.AlarmHandler;
import com.asetune.alarm.events.AlarmEvent;
import com.asetune.alarm.events.sqlserver.AlarmEventMemoryGrantWait;
import com.asetune.cm.CmSettingsHelper;
import com.asetune.cm.CounterSample;
import com.asetune.cm.CounterSetTemplates;
import com.asetune.cm.CounterSetTemplates.Type;
import com.asetune.cm.CountersModel;
import com.asetune.config.dict.MonTablesDictionary;
import com.asetune.config.dict.MonTablesDictionaryManager;
import com.asetune.graph.TrendGraphDataPoint;
import com.asetune.graph.TrendGraphDataPoint.LabelType;
import com.asetune.gui.MainFrame;
import com.asetune.sql.conn.DbxConnection;
import com.asetune.sql.conn.info.DbmsVersionInfo;
import com.asetune.sql.conn.info.DbmsVersionInfoSqlServer;
import com.asetune.utils.Configuration;
import com.asetune.utils.MathUtils;

/**
 * @author Goran Schwarz (goran_schwarz@hotmail.com)
 */
public class CmMemoryGrantsSum
extends CountersModel
{
	private static Logger        _logger          = Logger.getLogger(CmMemoryGrantsSum.class);
	private static final long    serialVersionUID = 1L;

	public static final String   CM_NAME          = CmMemoryGrantsSum.class.getSimpleName();
	public static final String   SHORT_NAME       = "Memory Grants Sum";
	public static final String   HTML_DESC        = 
		"<html>" +
			"<p>A Memory Grant Summary (from: dm_exec_query_resource_semaphores)</p>" +
		"</html>";

	public static final String   GROUP_NAME       = MainFrame.TCP_GROUP_SERVER;
	public static final String   GUI_ICON_FILE    = "images/"+CM_NAME+".png";

	public static final long     NEED_SRV_VERSION = 0;
	public static final long     NEED_CE_VERSION  = 0;

	public static final String[] MON_TABLES       = new String[] {CmMemoryGrantsSum.class.getSimpleName(), "dm_exec_query_resource_semaphores"};
	public static final String[] NEED_ROLES       = new String[] {"VIEW SERVER STATE"};
	public static final String[] NEED_CONFIG      = new String[] {};

	public static final String[] PCT_COLUMNS      = new String[] {"granted_memory_pct", "used_memory_pct"};
	public static final String[] DIFF_COLUMNS     = new String[] {};
//	public static final String[] DIFF_COLUMNS     = new String[] {
//			 "available_memory_mb"
//			,"granted_memory_mb"
//		    ,"used_memory_mb"
//			
//	};

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

		return new CmMemoryGrantsSum(counterController, guiController);
	}

	public CmMemoryGrantsSum(ICounterController counterController, IGuiController guiController)
	{
		super(counterController,
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


	//------------------------------------------------------------
	// Implementation
	//------------------------------------------------------------
	
//	@Override
//	public void updateGraphData(TrendGraphDataPoint tgdp)
//	{
//	}



//	@Override
//	protected TabularCntrPanel createGui()
//	{
//		return new CmMemoryGrantsSumPanel(this);
//	}

	@Override
	public void addMonTableDictForVersion(DbxConnection conn, DbmsVersionInfo versionInfo)
	{
		try 
		{
			String thisClassName = this.getClass().getSimpleName();

			MonTablesDictionary mtd = MonTablesDictionaryManager.getInstance();
			mtd.addTable(thisClassName, "Summary of Memory Grants");

			mtd.addColumn(thisClassName, "granted_memory_pct",  "<html>How much of the Total Available Memory is Allocated to Memory Grants in Percent<br>"
			                                                  + "<b>Algorithm:</b> granted_memory_kb / total_memory_kb * 100.0</html>");

			mtd.addColumn(thisClassName, "used_memory_pct"   ,  "<html>How much of the granted memory do the workers use<br>"
			                                                  + "<b>Algorithm:</b> granted_memory_kb / total_memory_kb * 100.0</html>");
		}
		catch (NameNotFoundException e) {/*ignore*/}
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

		pkCols.add("pool_id");
		pkCols.add("resource_semaphore_id");

		return pkCols;
	}

	@Override
	public String getSqlForVersion(DbxConnection conn, DbmsVersionInfo versionInfo)
	{
		String dm_exec_query_resource_semaphores = "dm_exec_query_resource_semaphores ";
		
		if ( ((DbmsVersionInfoSqlServer)versionInfo).isAzureSynapseAnalytics())
			dm_exec_query_resource_semaphores = "dm_pdw_nodes_exec_query_resource_semaphores";

		String sql = ""
			    + "SELECT \n"
			    + "     pool_id \n"
			    + "    ,resource_semaphore_id \n"
//			    + "    ,CAST(target_memory_kb       / 1024.0 as numeric(12,1))  AS target_memory_mb \n"
//			    + "    ,CAST(max_target_memory_kb   / 1024.0 as numeric(12,1))  AS max_target_memory_mb \n"
//			    + "    ,CAST(total_memory_kb        / 1024.0 as numeric(12,1))  AS total_memory_mb \n"
//			    + "    ,CAST(available_memory_kb    / 1024.0 as numeric(12,1))  AS available_memory_mb \n"
//			    + "    ,CAST(granted_memory_kb      / 1024.0 as numeric(12,1))  AS granted_memory_mb \n"
				+ "    ,target_memory_kb \n"
				+ "    ,max_target_memory_kb \n"
				+ "    ,total_memory_kb \n"
				+ "    ,available_memory_kb \n"
				+ "    ,granted_memory_kb \n"
			    + "    ,CAST((granted_memory_kb *1.0 / nullif(total_memory_kb,0)   *1.0) * 100.0 as numeric( 9,1))  AS granted_memory_pct \n"
//			    + "    ,CAST(used_memory_kb         / 1024.0 as numeric(12,1))  AS used_memory_mb \n"
			    + "    ,used_memory_kb \n"
			    + "    ,CAST((used_memory_kb    *1.0 / nullif(granted_memory_kb,0) *1.0) * 100.0 as numeric( 9,1))  AS used_memory_pct \n"
			    + "    ,grantee_count \n"
			    + "    ,waiter_count \n"
			    + "    ,timeout_error_count \n"
			    + "    ,forced_grant_count \n"
			    + "FROM sys." + dm_exec_query_resource_semaphores + " \n"
			    + "";
//		+ "    ,cast(child_tables_done   * 1.0 / nullif(child_tables_total ,0) * 100.0 as numeric(9,1))  AS child_tables_pct \n"

		return sql;
	}


	//--------------------------------------------------------------------------------------
	//--------------------------------------------------------------------------------------
	//-- Aggregation
	//--------------------------------------------------------------------------------------
	//--------------------------------------------------------------------------------------
	@Override
	public Map<String, AggregationType> createAggregateColumns()
	{
//		DbmsVersionInfo versionInfo = getDbmsVersionInfo();

		// Create a specific callback to do User Defined Average Aggregation on 'granted_memory_pct' and 'used_memory_pct'
		AggregationAverageCallback aggCb = new AggregationAverageCallback()
		{
			@Override
			public Object doAvgCalc(CountersModel countersModel, CounterSample cs, int counterType, int aggRowId, String colName, int jdbcType)
			{
				//-----------------------------------------------------------------------------------------------
				// Calculate 'granted_memory_pct' based on the values from the the values in the Aggregated Columns
				//-----------------------------------------------------------------------------------------------
				if ("granted_memory_pct".equals(colName))
				{
					BigDecimal retVal = null;
					double granted_memory = cs.getValueAsDouble(aggRowId, "granted_memory_kb", true, 0d);
					double total_memory   = cs.getValueAsDouble(aggRowId, "total_memory_kb"  , true, 0d);
					
					// Calculate
					if (total_memory > 0)
						retVal = MathUtils.roundToBigDecimal( granted_memory / total_memory * 100.0, 1);

					return retVal;
				}
		
				//-----------------------------------------------------------------------------------------------
				// Calculate 'used_memory_pct' based on the values from the the values in the Aggregated Columns
				//-----------------------------------------------------------------------------------------------
				if ("used_memory_pct".equals(colName))
				{
					BigDecimal retVal = null;
					double used_memory    = cs.getValueAsDouble(aggRowId, "used_memory_kb", true, 0d);
					double granted_memory = cs.getValueAsDouble(aggRowId, "granted_memory_kb"  , true, 0d);
					
					// Calculate
					if (granted_memory > 0)
						retVal = MathUtils.roundToBigDecimal( used_memory / granted_memory * 100.0, 1);

					return retVal;
				}
				
				throw new RuntimeException("in doAvgCalc() for CM='" + getName() + "' unhandled column name '" + colName + "'.");
			}
		};
		
		HashMap<String, AggregationType> aggColumns = new HashMap<>(getColumnCount());

		AggregationType tmp;
		
		// Create the columns :::::::::::::::::::::::::::::::::::::::::::::::::::::: And ADD it to the return Map 
//		tmp = new AggregationType("target_memory_mb"     , AggregationType.Agg.SUM);          aggColumns.put(tmp.getColumnName(), tmp);
//		tmp = new AggregationType("max_target_memory_mb" , AggregationType.Agg.SUM);          aggColumns.put(tmp.getColumnName(), tmp);
//		tmp = new AggregationType("total_memory_mb"      , AggregationType.Agg.SUM);          aggColumns.put(tmp.getColumnName(), tmp);
//		tmp = new AggregationType("available_memory_mb"  , AggregationType.Agg.SUM);          aggColumns.put(tmp.getColumnName(), tmp);
//		tmp = new AggregationType("granted_memory_mb"    , AggregationType.Agg.SUM);          aggColumns.put(tmp.getColumnName(), tmp);
		tmp = new AggregationType("target_memory_kb"     , AggregationType.Agg.SUM);          aggColumns.put(tmp.getColumnName(), tmp);
		tmp = new AggregationType("max_target_memory_kb" , AggregationType.Agg.SUM);          aggColumns.put(tmp.getColumnName(), tmp);
		tmp = new AggregationType("total_memory_kb"      , AggregationType.Agg.SUM);          aggColumns.put(tmp.getColumnName(), tmp);
		tmp = new AggregationType("available_memory_kb"  , AggregationType.Agg.SUM);          aggColumns.put(tmp.getColumnName(), tmp);
		tmp = new AggregationType("granted_memory_kb"    , AggregationType.Agg.SUM);          aggColumns.put(tmp.getColumnName(), tmp);
		tmp = new AggregationType("granted_memory_pct"   , AggregationType.Agg.AVG, aggCb);   aggColumns.put(tmp.getColumnName(), tmp);
//		tmp = new AggregationType("used_memory_mb"       , AggregationType.Agg.SUM);          aggColumns.put(tmp.getColumnName(), tmp);
		tmp = new AggregationType("used_memory_kb"       , AggregationType.Agg.SUM);          aggColumns.put(tmp.getColumnName(), tmp);
		tmp = new AggregationType("used_memory_pct"      , AggregationType.Agg.AVG, aggCb);   aggColumns.put(tmp.getColumnName(), tmp);

		tmp = new AggregationType("grantee_count"        , AggregationType.Agg.SUM);          aggColumns.put(tmp.getColumnName(), tmp);
		tmp = new AggregationType("waiter_count"         , AggregationType.Agg.SUM);          aggColumns.put(tmp.getColumnName(), tmp);
		tmp = new AggregationType("timeout_error_count"  , AggregationType.Agg.SUM);          aggColumns.put(tmp.getColumnName(), tmp);
		tmp = new AggregationType("forced_grant_count"   , AggregationType.Agg.SUM);          aggColumns.put(tmp.getColumnName(), tmp);

		return aggColumns;
	}

	@Override
	public boolean isAggregateRowAppendEnabled()
	{
		return true;
	}

	@Override
	public Object calculateAggregateRow_getAggregatePkColumnDataProvider(CounterSample newSample, String colName, int c, int jdbcType, Object addValue)
	{
		if ("pool_id"              .equalsIgnoreCase(colName)) return new Integer(-1);
		if ("resource_semaphore_id".equalsIgnoreCase(colName)) return new Short( (short) -1 );
		
		return addValue;
	}

//	@Override
//	public Object calculateAggregateRow_nonAggregatedColumnDataProvider(CounterSample newSample, String colName, int c, int jdbcType, Object addValue)
//	{
//		if ("dbname"       .equalsIgnoreCase(colName)) return "_Total";
//		
//		return null;
//	}

	
	//--------------------------------------------------------------------------------------
	//--------------------------------------------------------------------------------------
	//-- Charts
	//--------------------------------------------------------------------------------------
	//--------------------------------------------------------------------------------------
	public static final String GRAPH_NAME_GRANTEE_WAITER_COUNT = "GrantWaitCnt";
	public static final String GRAPH_NAME_GRANTED_MEMORY_SUM   = "GrantedMemSum";
	public static final String GRAPH_NAME_GRANTED_MEMORY_PCT   = "GrantedMemPct";

	private void addTrendGraphs()
	{
		addTrendGraph(GRAPH_NAME_GRANTEE_WAITER_COUNT,
				"Memory Grants and Wait Count",        // Menu CheckBox text
				"Memory Grants and Wait Count ("+GROUP_NAME+"->"+SHORT_NAME+")", // Label 
				TrendGraphDataPoint.Y_AXIS_SCALE_LABELS_NORMAL,
				new String[] {"grantee_count", "waiter_count"}, 
				LabelType.Static,
				TrendGraphDataPoint.Category.OPERATIONS,
				false, // is Percent Graph
				true, // visible at start
				0,     // graph is valid from Server Version. 0 = All Versions; >0 = Valid from this version and above 
				-1);   // minimum height

		addTrendGraph(GRAPH_NAME_GRANTED_MEMORY_SUM,
				"Memory Grant Summary in MB",        // Menu CheckBox text
				"Memory Grant Summary in MB ("+GROUP_NAME+"->"+SHORT_NAME+")", // Label 
				TrendGraphDataPoint.Y_AXIS_SCALE_LABELS_KB,
				new String[] {"granted_memory_mb", "used_memory_mb"}, 
				LabelType.Static,
				TrendGraphDataPoint.Category.MEMORY,
				false, // is Percent Graph
				true, // visible at start
				0,     // graph is valid from Server Version. 0 = All Versions; >0 = Valid from this version and above 
				-1);   // minimum height

		addTrendGraph(GRAPH_NAME_GRANTED_MEMORY_PCT,
				"Memory Granted in Percent of Total Available Memory",        // Menu CheckBox text
				"Memory Granted in Percent of Total Available Memory ("+GROUP_NAME+"->"+SHORT_NAME+")", // Label 
				TrendGraphDataPoint.Y_AXIS_SCALE_LABELS_PERCENT,
				new String[] {"granted_memory_pct"}, 
				LabelType.Static,
				TrendGraphDataPoint.Category.MEMORY,
				true,  // is Percent Graph
				false, // visible at start
				0,     // graph is valid from Server Version. 0 = All Versions; >0 = Valid from this version and above 
				-1);   // minimum height
	}

	@Override
	public void updateGraphData(TrendGraphDataPoint tgdp)
	{
		if (GRAPH_NAME_GRANTEE_WAITER_COUNT.equals(tgdp.getName()))
		{
			Double[] arr = new Double[2];

			int aggRowId = getAggregatedRowId();
			
			arr[0] = this.getAbsValueAsDouble(aggRowId, "grantee_count");
			arr[0] = this.getAbsValueAsDouble(aggRowId, "waiter_count");

			// Set the values
			tgdp.setDataPoint(this.getTimestamp(), arr);
		}

		if (GRAPH_NAME_GRANTED_MEMORY_SUM.equals(tgdp.getName()))
		{
			Double[] arr = new Double[2];

			int aggRowId = getAggregatedRowId();
			
			arr[0] = this.getAbsValueAsDouble(aggRowId, "granted_memory_kb") / 1024.0;
			arr[1] = this.getAbsValueAsDouble(aggRowId, "used_memory_kb")    / 1024.0;
			

			// Set the values
			tgdp.setDataPoint(this.getTimestamp(), arr);
		}

		if (GRAPH_NAME_GRANTED_MEMORY_PCT.equals(tgdp.getName()))
		{
			Double[] arr = new Double[1];

			int aggRowId = getAggregatedRowId();
			
			arr[0] = this.getAbsValueAsDouble(aggRowId, "granted_memory_pct");

			// Set the values
			tgdp.setDataPoint(this.getTimestamp(), arr);
		}
	}

	

	//--------------------------------------------------------------------------------------
	//--------------------------------------------------------------------------------------
	//-- Alarm Handling
	//--------------------------------------------------------------------------------------
	//--------------------------------------------------------------------------------------
	@Override
	public void sendAlarmRequest()
	{
		if ( ! hasDiffData() )
			return;
		
		if ( ! AlarmHandler.hasInstance() )
			return;

		AlarmHandler alarmHandler = AlarmHandler.getInstance();
		
		CountersModel cm = this;

		boolean debugPrint = Configuration.getCombinedConfiguration().getBooleanProperty("sendAlarmRequest.debug", _logger.isDebugEnabled());

		
		int aggRowId = getAggregatedRowId();

		//-------------------------------------------------------
		// SumWaiterCount 
		//-------------------------------------------------------
		if (isSystemAlarmsForColumnEnabledAndInTimeRange("SumWaiterCount"))
		{
			int waiter_count = cm.getAbsValueAsInteger(aggRowId, "waiter_count", -1);
			int threshold = Configuration.getCombinedConfiguration().getIntProperty(PROPKEY_alarm_SumWaiterCount, DEFAULT_alarm_SumWaiterCount);

			if (debugPrint || _logger.isDebugEnabled())
				System.out.println("##### sendAlarmRequest(" + cm.getName() + "): threshold=" + threshold + ", waiter_count='" + waiter_count);

			if (waiter_count > threshold)
			{
				String extendedDescText = cm.toTextTableString(DATA_RATE, aggRowId);
				String extendedDescHtml = cm.toHtmlTableString(DATA_RATE, aggRowId, true, false, false);

				AlarmEvent ae = new AlarmEventMemoryGrantWait(cm, threshold, waiter_count);

				ae.setExtendedDescription(extendedDescText, extendedDescHtml);
				
				alarmHandler.addAlarm( ae );
			}
		}
	}

	public static final String  PROPKEY_alarm_SumWaiterCount  = CM_NAME + ".alarm.system.if.sum.waiter_count.gt";
	public static final int     DEFAULT_alarm_SumWaiterCount  = 0;
	
	@Override
	public List<CmSettingsHelper> getLocalAlarmSettings()
	{
		Configuration conf = Configuration.getCombinedConfiguration();
		List<CmSettingsHelper> list = new ArrayList<>();

		CmSettingsHelper.Type isAlarmSwitch = CmSettingsHelper.Type.IS_ALARM_SWITCH;
		
		list.add(new CmSettingsHelper("SumWaiterCount", isAlarmSwitch, PROPKEY_alarm_SumWaiterCount, Integer.class, conf.getIntProperty(PROPKEY_alarm_SumWaiterCount, DEFAULT_alarm_SumWaiterCount), DEFAULT_alarm_SumWaiterCount, "If 'waiter_count' in the SUM row is greater than ## then send 'AlarmEventMemoryGrantWait'." ));

		return list;
	}
}
