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

import javax.naming.NameNotFoundException;

import org.apache.log4j.Logger;

import com.asetune.ICounterController;
import com.asetune.IGuiController;
import com.asetune.alarm.AlarmHandler;
import com.asetune.alarm.events.AlarmEventConfigResourceIsLow;
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
import com.asetune.utils.Configuration;
import com.asetune.utils.Ver;

/**
 * @author Goran Schwarz (goran_schwarz@hotmail.com)
 */
public class CmStatementCache
extends CountersModel
{
	private static Logger        _logger          = Logger.getLogger(CmStatementCache.class);
	private static final long    serialVersionUID = 1L;

	public static final String   CM_NAME          = CmStatementCache.class.getSimpleName();
	public static final String   SHORT_NAME       = "Statement Cache";
	public static final String   HTML_DESC        = 
		"<html>" +
		"Get overall statistics on the whole statement cache." +
		"</html>";

	public static final String   GROUP_NAME       = MainFrame.TCP_GROUP_CACHE;
	public static final String   GUI_ICON_FILE    = "images/"+CM_NAME+".png";

//	public static final long     NEED_SRV_VERSION = 15020;
//	public static final long     NEED_SRV_VERSION = 1502000;
	public static final long     NEED_SRV_VERSION = Ver.ver(15,0,2);
	public static final long     NEED_CE_VERSION  = 0;

	public static final String[] MON_TABLES       = new String[] {"monStatementCache"};
	public static final String[] NEED_ROLES       = new String[] {"mon_role"};
	public static final String[] NEED_CONFIG      = new String[] {"enable monitoring=1", "enable stmt cache monitoring=1", "statement cache size"}; // NO default for 'statement cache size' configuration

	public static final String[] PCT_COLUMNS      = new String[] {"CacheHitPct", "OveralAvgReusePct"};
	public static final String[] DIFF_COLUMNS     = new String[] {
		"NumStatementsDiff", "NumSearches", "HitCount", "MissCount", "NumInserts", "NumRemovals", 
		"NumRecompilesSchemaChanges", "NumRecompilesPlanFlushes"};

	public static final boolean  NEGATIVE_DIFF_COUNTERS_TO_ZERO = true;
	public static final boolean  IS_SYSTEM_CM                   = true;
	public static final int      DEFAULT_POSTPONE_TIME          = 0;
	public static final int      DEFAULT_QUERY_TIMEOUT          = CountersModel.DEFAULT_sqlQueryTimeout;

	@Override public int     getDefaultPostponeTime()                 { return DEFAULT_POSTPONE_TIME; }
	@Override public int     getDefaultQueryTimeout()                 { return DEFAULT_QUERY_TIMEOUT; }
	@Override public boolean getDefaultIsNegativeDiffCountersToZero() { return NEGATIVE_DIFF_COUNTERS_TO_ZERO; }
	@Override public Type    getTemplateLevel()                       { return Type.LARGE; }

	/**
	 * FACTORY  method to create the object
	 */
	public static CountersModel create(ICounterController counterController, IGuiController guiController)
	{
		if (guiController != null && guiController.hasGUI())
			guiController.splashWindowProgress("Loading: Counter Model '"+CM_NAME+"'");

		return new CmStatementCache(counterController, guiController);
	}

	public CmStatementCache(ICounterController counterController, IGuiController guiController)
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

	public static final String GRAPH_NAME_REQUEST_PER_SEC = "RequestPerSecGraph"; //String x=GetCounters.CM_GRAPH_NAME__STATEMENT_CACHE__REQUEST_PER_SEC;
	public static final String GRAPH_NAME_HIT_RATE_PCT    = "HitRatePctGraph";    //String x=GetCounters.CM_GRAPH_NAME__STATEMENT_CACHE__REQUEST_PER_SEC;

	private void addTrendGraphs()
	{
//		String[] labelsPerSec  = new String[] { "NumSearches", "HitCount", "NumInserts", "NumRemovals" };
//		String[] labelsHitRate = new String[] { "Hit rate" };
//		
//		addTrendGraphData(GRAPH_NAME_REQUEST_PER_SEC, new TrendGraphDataPoint(GRAPH_NAME_REQUEST_PER_SEC, labelsPerSec,  LabelType.Static));
//		addTrendGraphData(GRAPH_NAME_HIT_RATE_PCT,    new TrendGraphDataPoint(GRAPH_NAME_HIT_RATE_PCT,    labelsHitRate, LabelType.Static));

		addTrendGraph(GRAPH_NAME_REQUEST_PER_SEC,
			"Statement Cache Requests", 	                           // Menu CheckBox text
			"Number of Requests from the Statement Cache, per Second ("+GROUP_NAME+"->"+SHORT_NAME+")", // Label 
			TrendGraphDataPoint.Y_AXIS_SCALE_LABELS_PERSEC,
			new String[] { "NumSearches", "HitCount", "NumInserts", "NumRemovals" }, 
			LabelType.Static,
			TrendGraphDataPoint.Category.CACHE,
			false, // is Percent Graph
			false, // visible at start
			0,     // graph is valid from Server Version. 0 = All Versions; >0 = Valid from this version and above 
			-1);   // minimum height

		// GRAPH
		addTrendGraph(GRAPH_NAME_HIT_RATE_PCT,
			"Statement Cache Hit Rate", 	                           // Menu CheckBox text
			"Statement Cache Hit Rate, in Percent ("+GROUP_NAME+"->"+SHORT_NAME+")",                    // Label 
			TrendGraphDataPoint.Y_AXIS_SCALE_LABELS_PERCENT,
			new String[] { "Hit rate" }, 
			LabelType.Static,
			TrendGraphDataPoint.Category.CACHE,
			true, // is Percent Graph
			false, // visible at start
			0,     // graph is valid from Server Version. 0 = All Versions; >0 = Valid from this version and above 
			-1);   // minimum height

//		// if GUI
//		if (getGuiController() != null && getGuiController().hasGUI())
//		{
//			// GRAPH
//			TrendGraph tg = null;
//			tg = new TrendGraph(GRAPH_NAME_REQUEST_PER_SEC,
//				"Statement Cache Requests", 	                           // Menu CheckBox text
//				"Number of Requests from the Statement Cache, per Second ("+GROUP_NAME+"->"+SHORT_NAME+")", // Label 
//				labelsPerSec, 
//				false, // is Percent Graph
//				this, 
//				false, // visible at start
//				0,     // graph is valid from Server Version. 0 = All Versions; >0 = Valid from this version and above 
//				-1);   // minimum height
//			addTrendGraph(tg.getName(), tg, true);
//
//			// GRAPH
//			tg = new TrendGraph(GRAPH_NAME_HIT_RATE_PCT,
//				"Statement Cache Hit Rate", 	                           // Menu CheckBox text
//				"Statement Cache Hit Rate, in Percent ("+GROUP_NAME+"->"+SHORT_NAME+")",                    // Label 
//				labelsHitRate, 
//				true, // is Percent Graph
//				this, 
//				false, // visible at start
//				0,     // graph is valid from Server Version. 0 = All Versions; >0 = Valid from this version and above 
//				-1);   // minimum height
//			addTrendGraph(tg.getName(), tg, true);
//		}
	}

//	@Override
//	protected TabularCntrPanel createGui()
//	{
//		return new CmStatementCachePanel(this);
//	}

	@Override
	public String[] getDependsOnConfigForVersion(Connection conn, long srvVersion, boolean isClusterEnabled)
	{
		return NEED_CONFIG;
	}

	@Override
	public void addMonTableDictForVersion(Connection conn, long srvVersion, boolean isClusterEnabled)
	{
		try 
		{
			MonTablesDictionary mtd = MonTablesDictionaryManager.getInstance();
			mtd.addColumn("monStatementCache",  "UnusedSizeKB", "<html>" +
					"Number of KB that is free for usage by any statement.<br>" +
					"<b>Formula</b>: abs.TotalSizeKB - abs.UsedSizeKB<br></html>");
			mtd.addColumn("monStatementCache",  "AvgStmntSizeInKB", "<html>" +
					"Average KB that each compiled SQL Statement are using.<br>" +
					"<b>Formula</b>: abs.UsedSizeKB / abs.NumStatements<br></html>");
			mtd.addColumn("monStatementCache",  "NumStatementsDiff", "<html>" +
					"Simply the difference count from previous sample of 'NumStatements'.<br>" +
					"<b>Formula</b>: this.NumStatements - previous.NumStatements<br></html>");
			mtd.addColumn("monStatementCache",  "CacheHitPct", "<html>" +
					"Percent of Statements that already was in the Statement Cache<br>" +
					"<b>Formula</b>: diff.HitCount / diff.NumSearches * 100 <br></html>");
			mtd.addColumn("monStatementCache",  "MissCount", "<html>" +
					"Statements that was not fount in the Statement Cache<br>" +
					"<b>Formula</b>: NumSearches - HitCount <br></html>");
//			mtd.addColumn("monStatementCache",  "OveralAvgReusePct", "<html>" +
//					"A good indication of overall average reuse for each statement. A 10:1 ratio obviously is much better than 2:1<br>" +
//					"<b>Formula</b>: diff.HitCount / diff.NumStatements * 100 <br>" +
//					"<b>Note</b>: The sampling interval plays a huge role in this metric – during a 1 second sample, not that many statements <br>" +
//					"             may be executed as compared to a 10 minute sample – and could distort the ratio to be viewed as excessively low.<br></html>");
		}
		catch (NameNotFoundException e) {/*ignore*/}
	}

	@Override
	public List<String> getPkForVersion(Connection conn, long srvVersion, boolean isClusterEnabled)
	{
		List <String> pkCols = new LinkedList<String>();

		if (isClusterEnabled)
			pkCols.add("InstanceID");

		pkCols.add("TotalSizeKB");

		return pkCols;
	}

	@Override
	public String getSqlForVersion(Connection conn, long srvVersion, boolean isClusterEnabled)
	{
		// ------------------------------------------------------------------------------------------------
		// ASE 16.0 SP3 PL0
		// ------------------------------------------------------------------------------------------------
		String ProcedureCacheSizeKB = ""; // Capacity of the Procedure Cache (KB)
		String AQTCacheUsedSizeKB   = ""; // Amount of cache in use for Auto Query Tuning (AQT) objects (KB)
		String NumAQTs              = ""; // Number of AQT objects in the cache
		String nl_160_sp3           = "";

		if ( srvVersion >= Ver.ver(16,0,0, 3,0) )
		{
			ProcedureCacheSizeKB = "  , ProcedureCacheSizeKB";
			AQTCacheUsedSizeKB   = "  , AQTCacheUsedSizeKB";
			NumAQTs              = "  , NumAQTs";
			nl_160_sp3           = "\n";
		}

		// Can we reset counters/something if 'NumSearches' or 'HitCount' is below 0 (counter has wrapped)
		String sql = 
			"SELECT \n" +
			(isClusterEnabled ? "  InstanceID, \n" : "") +
			"  TotalSizeKB, \n" +
			"  UsedSizeKB, \n" +
			"  UnusedSizeKB      = TotalSizeKB - UsedSizeKB, \n" +
			"  AvgStmntSizeInKB  = CASE WHEN NumStatements>0 THEN UsedSizeKB / NumStatements ELSE 0 END, \n" +
			"  NumStatements, \n" +
			"  NumStatementsDiff = NumStatements, \n" +
			"  CacheHitPct       = CASE WHEN NumSearches > 0 \n" +
			"                           THEN convert(numeric(10,1), ((HitCount+0.0)/(NumSearches+0.0))  *100.0 ) \n" +
			"                           ELSE convert(numeric(10,1), -1) \n" +
			"                      END, \n" +
//			"  OveralAvgReusePct = CASE WHEN NumStatements>0 THEN convert(numeric(10,1), ((HitCount+0.0)/(NumStatements+0.0))*100.0 ) ELSE convert(numeric(10,1), 0) END, \n" +
			"  NumSearches, \n" +
			"  HitCount, \n" +
//			"  MissCount = NumSearches - HitCount, \n" +
			"  MissCount         = CASE WHEN NumSearches > 0 AND HitCount > 0 -- Protect for 'counter spillover' to negative number or 'arithmic overflow' \n" +
			"                           THEN NumSearches - HitCount \n" +
			"                           ELSE -1 \n" +
			"                      END, \n" +
			"  NumInserts, \n" +
			"  NumRemovals, \n" +
			"  NumRecompilesSchemaChanges, \n" +
			"  NumRecompilesPlanFlushes \n" +
			ProcedureCacheSizeKB  + nl_160_sp3 +
			AQTCacheUsedSizeKB    + nl_160_sp3 +
			NumAQTs               + nl_160_sp3 +
			"FROM master..monStatementCache \n";

		return sql;
	}

	/** 
	 * Compute the CacheHitPct for DIFF values
	 */
	@Override
	public void localCalculation(CounterSample prevSample, CounterSample newSample, CounterSample diffData)
	{
		int CacheHitPctId = -1, OveralAvgReusePctId = -1;

		int HitCount,        NumSearches;//,        NumStatementsDiff;
		int HitCountId = -1, NumSearchesId = -1;//, NumStatementsDiffId = -1;

		// Find column Id's
		List<String> colNames = diffData.getColNames();
		if (colNames == null)
			return;

		for (int colId=0; colId < colNames.size(); colId++) 
		{
			String colName = colNames.get(colId);
			if      (colName.equals("HitCount"))          HitCountId          = colId;
			else if (colName.equals("NumSearches"))       NumSearchesId       = colId;
//			else if (colName.equals("NumStatementsDiff")) NumStatementsDiffId = colId;
			else if (colName.equals("CacheHitPct"))       CacheHitPctId       = colId;
			else if (colName.equals("OveralAvgReusePct")) OveralAvgReusePctId = colId;

			// Noo need to continue, we got all our columns
			if (    HitCountId          >= 0 
			     && NumSearchesId       >= 0 
//			     && NumStatementsDiffId >= 0  
			     && CacheHitPctId       >= 0  
			     && OveralAvgReusePctId >= 0  
			   )
				break;
		}

		// Loop on all diffData rows
		for (int rowId = 0; rowId < diffData.getRowCount(); rowId++)
		{
			HitCount          = ((Number)diffData.getValueAt(rowId, HitCountId         )).intValue();
			NumSearches       = ((Number)diffData.getValueAt(rowId, NumSearchesId      )).intValue();
//			NumStatementsDiff = ((Number)diffData.getValueAt(rowId, NumStatementsDiffId)).intValue();

			//---- CacheHitPct
			int colPos = CacheHitPctId;
			if (NumSearches > 0)
			{
				double calc = ((HitCount+0.0) / (NumSearches+0.0)) * 100.0;

				BigDecimal newVal = new BigDecimal(calc).setScale(1, BigDecimal.ROUND_HALF_EVEN);
				diffData.setValueAt(newVal, rowId, colPos);
			}
			else
				diffData.setValueAt(new BigDecimal(0).setScale(1, BigDecimal.ROUND_HALF_EVEN), rowId, colPos);

//			//---- OveralAvgReusePct
//			colPos = OveralAvgReusePctId;
//			if (NumStatementsDiff > 0)
//			{
//				double calc = ((HitCount+0.0) / (NumStatementsDiff+0.0)) * 100.0;
//
//				BigDecimal newVal = new BigDecimal(calc).setScale(1, BigDecimal.ROUND_HALF_EVEN);
//				diffData.setValueAt(newVal, rowId, colPos);
//			}
//			else
//				diffData.setValueAt(new BigDecimal(0).setScale(1, BigDecimal.ROUND_HALF_EVEN), rowId, colPos);
		}
	}

	@Override
	public void updateGraphData(TrendGraphDataPoint tgdp)
	{
		if (GRAPH_NAME_REQUEST_PER_SEC.equals(tgdp.getName()))
		{
			Double[] arr = new Double[4];

			arr[0] = this.getRateValueSum("NumSearches");
			arr[1] = this.getRateValueSum("HitCount");
			arr[2] = this.getRateValueSum("NumInserts");
			arr[3] = this.getRateValueSum("NumRemovals");
			
			if (_logger.isDebugEnabled())
				_logger.debug("updateGraphData(StatementCache:RequestPerSecGraph): NumSearches='"+arr[0]+"', HitCount='"+arr[1]+"', NumInserts='"+arr[2]+"', NumRemovals='"+arr[3]+"'.");

			// Set the values
			tgdp.setDataPoint(this.getTimestamp(), arr);
		}

		if (GRAPH_NAME_HIT_RATE_PCT.equals(tgdp.getName()))
		{
			Double[] arr = new Double[1];

			arr[0] = this.getRateValueSum("CacheHitPct");
			
			if (_logger.isDebugEnabled())
				_logger.debug("updateGraphData(StatementCache:HitRatePctGraph): CacheHitPct='"+arr[0]+"'.");

			// Set the values
			tgdp.setDataPoint(this.getTimestamp(), arr);
		}
	}
	
	
	//--------------------------------------------------------------------
	// Alarm Handling
	//--------------------------------------------------------------------
	@Override
	public void sendAlarmRequest()
	{
		if ( ! hasDiffData() )
			return;

		if ( ! AlarmHandler.hasInstance() )
			return;

		CountersModel cm = this;

		boolean debugPrint = System.getProperty("sendAlarmRequest.debug", "false").equalsIgnoreCase("true");

		//-------------------------------------------------------
		// CacheHitPct
		//-------------------------------------------------------
		if (isSystemAlarmsForColumnEnabledAndInTimeRange("CacheHitPct"))
		{
			Double CacheHitPct = cm.getAbsValueAsDouble (0, "CacheHitPct");
			if (CacheHitPct != null)
			{
				if (debugPrint || _logger.isDebugEnabled())
					System.out.println("##### sendAlarmRequest("+cm.getName()+"): CacheHitPct='"+CacheHitPct+"'.");

				int threshold = Configuration.getCombinedConfiguration().getIntProperty(PROPKEY_alarm_CacheHitPct, DEFAULT_alarm_CacheHitPct);
				if (CacheHitPct.intValue() < threshold)
				{
					Double TotalSizeKB  = cm.getAbsValueAsDouble (0, "TotalSizeKB");
					Double UnusedSizeKB = cm.getAbsValueAsDouble (0, "UnusedSizeKB");
					
					Double TotalSizeMB  = TotalSizeKB / 1024.0;

					// Note: AlarmEventCacheHitRate do not yet exist (not sure if I want to create a specific AlarmEvent for this... for now: reuse AlarmEventConfigResourceIsLow)
//					String warnMsg = "Statement Cache Hit Rate is below ## Percent (alarmValue="+CacheHitPct+"). The Statement Cache might be configured to low";
//					String fixMsg  = "sp_configure 'statement cache size', 0, '###M'... or free unused memory with 'dbcc traceon(3604) dbcc proc_cache(free_unused)'.";
//					String msg     = warnMsg + fixMsg;
//
//					AlarmHandler.getInstance().addAlarm(
//							new AlarmEventCacheHitRate(cm, "Statement Cache", msg, threshold, CacheHitPct) );

					String warnMsg = "The 'statement cache size' might be configured to low, Statement Cache Hit Rate is below " + threshold + " Percent (CacheHitPct alarmValue=" + CacheHitPct + ", TotalSizeMB=" + TotalSizeMB + ", UnusedSizeKB=" + UnusedSizeKB + "). ";
					String fixMsg  = "Fix this using: sp_configure 'statement cache size', 0, '###M'... or free unused memory with 'dbcc traceon(3604) dbcc proc_cache(free_unused)'.";
					String msg     = warnMsg + fixMsg;

					AlarmHandler.getInstance().addAlarm(
						new AlarmEventConfigResourceIsLow(cm, "statement cache size", TotalSizeMB, msg, threshold) );
				}
			}
		}
	}

	public static final String  PROPKEY_alarm_CacheHitPct = CM_NAME + ".alarm.system.if.CacheHitPct.lt";
	public static final int     DEFAULT_alarm_CacheHitPct = 25;
	
	@Override
	public List<CmSettingsHelper> getLocalAlarmSettings()
	{
		Configuration conf = Configuration.getCombinedConfiguration();
		List<CmSettingsHelper> list = new ArrayList<>();
		
		CmSettingsHelper.Type isAlarmSwitch = CmSettingsHelper.Type.IS_ALARM_SWITCH;
		
//		list.add(new CmSettingsHelper("CacheHitPct", isAlarmSwitch, PROPKEY_alarm_CacheHitPct, Integer.class, conf.getIntProperty(PROPKEY_alarm_CacheHitPct, DEFAULT_alarm_CacheHitPct), DEFAULT_alarm_CacheHitPct, "If 'CacheHitPct' is less than ## then send 'AlarmEventCacheHitRate'." ));
		list.add(new CmSettingsHelper("CacheHitPct", isAlarmSwitch, PROPKEY_alarm_CacheHitPct, Integer.class, conf.getIntProperty(PROPKEY_alarm_CacheHitPct, DEFAULT_alarm_CacheHitPct), DEFAULT_alarm_CacheHitPct, "If 'CacheHitPct' is less than ## then send 'AlarmEventConfigResourceIsLow'." ));

		return list;
	}
}

