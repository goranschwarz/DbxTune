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
package com.asetune.cm.hana;

import java.math.BigDecimal;
import java.util.LinkedList;
import java.util.List;

import org.apache.log4j.Logger;

import com.asetune.ICounterController;
import com.asetune.IGuiController;
import com.asetune.cm.CounterSample;
import com.asetune.cm.CounterSetTemplates;
import com.asetune.cm.CounterSetTemplates.Type;
import com.asetune.cm.CountersModel;
import com.asetune.graph.TrendGraphDataPoint;
import com.asetune.graph.TrendGraphDataPoint.LabelType;
import com.asetune.gui.MainFrame;
import com.asetune.sql.conn.DbxConnection;
import com.asetune.sql.conn.info.DbmsVersionInfo;

/**
 * @author Goran Schwarz (goran_schwarz@hotmail.com)
 */
public class CmPlanCacheOverview
extends CountersModel
{
	private static Logger        _logger          = Logger.getLogger(CmPlanCacheOverview.class);
	private static final long    serialVersionUID = 1L;

	public static final String   CM_NAME          = CmPlanCacheOverview.class.getSimpleName();
	public static final String   SHORT_NAME       = "Statement Cache";
	public static final String   HTML_DESC        = 
		"<html>" +
		"Get overall statistics on the whole statement cache." +
		"</html>";

	public static final String   GROUP_NAME       = MainFrame.TCP_GROUP_CACHE;
	public static final String   GUI_ICON_FILE    = "images/"+CM_NAME+".png";

	public static final long     NEED_SRV_VERSION = 0;
	public static final long     NEED_CE_VERSION  = 0;

	public static final String[] MON_TABLES       = new String[] {"M_SQL_PLAN_CACHE_OVERVIEW"};
	public static final String[] NEED_ROLES       = new String[] {};
	public static final String[] NEED_CONFIG      = new String[] {};

	public static final String[] PCT_COLUMNS      = new String[] {"CACHE_HIT_PCT"};
	public static final String[] DIFF_COLUMNS     = new String[] {
//		"PLAN_CACHE_CAPACITY",              // java.sql.Types.BIGINT    BIGINT           
//		"CACHED_PLAN_SIZE",                 // java.sql.Types.BIGINT    BIGINT           
//		"PLAN_CACHE_HIT_RATIO",             // java.sql.Types.REAL      REAL             
		"PLAN_CACHE_LOOKUP_COUNT",          // java.sql.Types.BIGINT    BIGINT           
		"PLAN_CACHE_HIT_COUNT",             // java.sql.Types.BIGINT    BIGINT           
//		"EVICTED_PLAN_AVG_CACHE_TIME",      // java.sql.Types.BIGINT    BIGINT           
		"EVICTED_PLAN_COUNT",               // java.sql.Types.BIGINT    BIGINT           
		"EVICTED_PLAN_PREPARATION_COUNT",   // java.sql.Types.BIGINT    BIGINT           
		"EVICTED_PLAN_EXECUTION_COUNT",     // java.sql.Types.BIGINT    BIGINT           
		"EVICTED_PLAN_PREPARATION_TIME",    // java.sql.Types.BIGINT    BIGINT           
		"EVICTED_PLAN_CURSOR_DURATION",     // java.sql.Types.BIGINT    BIGINT           
		"EVICTED_PLAN_TOTAL_EXECUTION_TIME",// java.sql.Types.BIGINT    BIGINT           
		"EVICTED_PLAN_SIZE",                // java.sql.Types.BIGINT    BIGINT           
		"CACHED_PLAN_COUNT",                // java.sql.Types.BIGINT    BIGINT           
		"CACHED_PLAN_PREPARATION_COUNT",    // java.sql.Types.BIGINT    BIGINT           
		"CACHED_PLAN_EXECUTION_COUNT",      // java.sql.Types.BIGINT    BIGINT           
		"CACHED_PLAN_PREPARATION_TIME",     // java.sql.Types.BIGINT    BIGINT           
		"CACHED_PLAN_CURSOR_DURATION",      // java.sql.Types.BIGINT    BIGINT           
		"CACHED_PLAN_TOTAL_EXECUTION_TIME", // java.sql.Types.BIGINT    BIGINT           
//		"CLEAR_TIMESTAMP",                  // java.sql.Types.TIMESTAMP TIMESTAMP        
		"-last-dummy-col-"
	};
//	RS> Col# Label                             JDBC Type Name           Guessed DBMS type
//	RS> ---- --------------------------------- ------------------------ -----------------
//	RS> 1    HOST                              java.sql.Types.VARCHAR   VARCHAR(64)      
//	RS> 2    PORT                              java.sql.Types.INTEGER   INTEGER          
//	RS> 3    PLAN_CACHE_ENABLED                java.sql.Types.INTEGER   INTEGER          
//	RS> 4    STATISTICS_COLLECTION_ENABLED     java.sql.Types.VARCHAR   VARCHAR(5)       
//	RS> 5    PLAN_CACHE_CAPACITY               java.sql.Types.BIGINT    BIGINT           
//	RS> 6    CACHED_PLAN_SIZE                  java.sql.Types.BIGINT    BIGINT           
//	RS> 7    PLAN_CACHE_HIT_RATIO              java.sql.Types.REAL      REAL             
//	RS> 8    PLAN_CACHE_LOOKUP_COUNT           java.sql.Types.BIGINT    BIGINT           
//	RS> 9    PLAN_CACHE_HIT_COUNT              java.sql.Types.BIGINT    BIGINT           
//	RS> 10   EVICTED_PLAN_AVG_CACHE_TIME       java.sql.Types.BIGINT    BIGINT           
//	RS> 11   EVICTED_PLAN_COUNT                java.sql.Types.BIGINT    BIGINT           
//	RS> 12   EVICTED_PLAN_PREPARATION_COUNT    java.sql.Types.BIGINT    BIGINT           
//	RS> 13   EVICTED_PLAN_EXECUTION_COUNT      java.sql.Types.BIGINT    BIGINT           
//	RS> 14   EVICTED_PLAN_PREPARATION_TIME     java.sql.Types.BIGINT    BIGINT           
//	RS> 15   EVICTED_PLAN_CURSOR_DURATION      java.sql.Types.BIGINT    BIGINT           
//	RS> 16   EVICTED_PLAN_TOTAL_EXECUTION_TIME java.sql.Types.BIGINT    BIGINT           
//	RS> 17   EVICTED_PLAN_SIZE                 java.sql.Types.BIGINT    BIGINT           
//	RS> 18   CACHED_PLAN_COUNT                 java.sql.Types.BIGINT    BIGINT           
//	RS> 19   CACHED_PLAN_PREPARATION_COUNT     java.sql.Types.BIGINT    BIGINT           
//	RS> 20   CACHED_PLAN_EXECUTION_COUNT       java.sql.Types.BIGINT    BIGINT           
//	RS> 21   CACHED_PLAN_PREPARATION_TIME      java.sql.Types.BIGINT    BIGINT           
//	RS> 22   CACHED_PLAN_CURSOR_DURATION       java.sql.Types.BIGINT    BIGINT           
//	RS> 23   CACHED_PLAN_TOTAL_EXECUTION_TIME  java.sql.Types.BIGINT    BIGINT           
//	RS> 24   CLEAR_TIMESTAMP                   java.sql.Types.TIMESTAMP TIMESTAMP        

	public static final boolean  NEGATIVE_DIFF_COUNTERS_TO_ZERO = false;
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

		return new CmPlanCacheOverview(counterController, guiController);
	}

	public CmPlanCacheOverview(ICounterController counterController, IGuiController guiController)
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

	public static final String GRAPH_NAME_REQUEST_PER_SEC = "RequestPerSecGraph"; //String x=GetCounters.CM_GRAPH_NAME__STATEMENT_CACHE__REQUEST_PER_SEC;
	public static final String GRAPH_NAME_HIT_RATE_PCT    = "HitRatePctGraph";    //String x=GetCounters.CM_GRAPH_NAME__STATEMENT_CACHE__REQUEST_PER_SEC;

	private void addTrendGraphs()
	{
//		String[] labelsPerSec  = new String[] { "NumSearches", "HitCount", "NumInserts", "NumRemovals" };
//		String[] labelsHitRate = new String[] { "Hit rate percent" };
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
			true,  // visible at start
			0,     // graph is valid from Server Version. 0 = All Versions; >0 = Valid from this version and above 
			-1);   // minimum height

		// GRAPH
		addTrendGraph(GRAPH_NAME_HIT_RATE_PCT,
			"Statement Cache Hit Rate", 	                           // Menu CheckBox text
			"Statement Cache Hit Rate, in Percent ("+GROUP_NAME+"->"+SHORT_NAME+")",                    // Label 
			TrendGraphDataPoint.Y_AXIS_SCALE_LABELS_PERCENT,
			new String[] { "Hit rate percent" }, 
			LabelType.Static,
			TrendGraphDataPoint.Category.CACHE,
			true, // is Percent Graph
			true,  // visible at start
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
//				true,  // visible at start
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
//				true,  // visible at start
//				0,     // graph is valid from Server Version. 0 = All Versions; >0 = Valid from this version and above 
//				-1);   // minimum height
//			addTrendGraph(tg.getName(), tg, true);
//		}
	}

//	@Override
//	protected TabularCntrPanel createGui()
//	{
//		return new CmPlanCacheOverviewPanel(this);
//	}

	@Override
	public String[] getDependsOnConfigForVersion(DbxConnection conn, DbmsVersionInfo versionInfo)
	{
		return NEED_CONFIG;
	}

	@Override
	public void addMonTableDictForVersion(DbxConnection conn, DbmsVersionInfo versionInfo)
	{
//		try 
//		{
//			MonTablesDictionary mtd = MonTablesDictionary.getInstance();
//			mtd.addColumn("monStatementCache",  "UnusedSizeKB", "<html>" +
//					"Number of KB that is free for usage by any statement.<br>" +
//					"<b>Formula</b>: abs.TotalSizeKB - abs.UsedSizeKB<br></html>");
//			mtd.addColumn("monStatementCache",  "AvgStmntSizeInKB", "<html>" +
//					"Average KB that each compiled SQL Statement are using.<br>" +
//					"<b>Formula</b>: abs.UsedSizeKB / abs.NumStatements<br></html>");
//			mtd.addColumn("monStatementCache",  "NumStatementsDiff", "<html>" +
//					"Simply the difference count from previous sample of 'NumStatements'.<br>" +
//					"<b>Formula</b>: this.NumStatements - previous.NumStatements<br></html>");
//			mtd.addColumn("monStatementCache",  "CacheHitPct", "<html>" +
//					"Percent of Statements that already was in the Statement Cache<br>" +
//					"<b>Formula</b>: diff.HitCount / diff.NumSearches * 100 <br></html>");
//			mtd.addColumn("monStatementCache",  "MissCount", "<html>" +
//					"Statements that was not fount in the Statement Cache<br>" +
//					"<b>Formula</b>: NumSearches - HitCount <br></html>");
////			mtd.addColumn("monStatementCache",  "OveralAvgReusePct", "<html>" +
////					"A good indication of overall average reuse for each statement. A 10:1 ratio obviously is much better than 2:1<br>" +
////					"<b>Formula</b>: diff.HitCount / diff.NumStatements * 100 <br>" +
////					"<b>Note</b>: The sampling interval plays a huge role in this metric – during a 1 second sample, not that many statements <br>" +
////					"             may be executed as compared to a 10 minute sample – and could distort the ratio to be viewed as excessively low.<br></html>");
//		}
//		catch (NameNotFoundException e) {/*ignore*/}
	}

	@Override
	public List<String> getPkForVersion(DbxConnection conn, DbmsVersionInfo versionInfo)
	{
		List <String> pkCols = new LinkedList<String>();

		pkCols.add("HOST");
		pkCols.add("PORT");

		return pkCols;
	}

	@Override
	public String getSqlForVersion(DbxConnection conn, DbmsVersionInfo versionInfo)
	{
		String sql = 
			"select \n" +
			"    HOST, \n" + 
			"    PORT, \n" + 
			"    CASE WHEN PLAN_CACHE_LOOKUP_COUNT > 0 \n" +
			"        THEN TO_DECIMAL(PLAN_CACHE_HIT_COUNT / PLAN_CACHE_LOOKUP_COUNT * 100.0, 10, 1) \n" +
			"        ELSE TO_DECIMAL(0.0, 10, 1) \n" +
			"    END as CACHE_HIT_PCT, \n" +
			"    PLAN_CACHE_ENABLED, \n" + 
			"    STATISTICS_COLLECTION_ENABLED, \n" + 
			"    PLAN_CACHE_CAPACITY, \n" + 
			"    CACHED_PLAN_SIZE, \n" + 
			"    PLAN_CACHE_HIT_RATIO, \n" + 
			"    PLAN_CACHE_LOOKUP_COUNT, \n" + 
			"    PLAN_CACHE_HIT_COUNT, \n" + 
			"    EVICTED_PLAN_AVG_CACHE_TIME, \n" + 
			"    EVICTED_PLAN_COUNT, \n" + 
			"    EVICTED_PLAN_PREPARATION_COUNT, \n" + 
			"    EVICTED_PLAN_EXECUTION_COUNT, \n" + 
			"    EVICTED_PLAN_PREPARATION_TIME, \n" + 
			"    EVICTED_PLAN_CURSOR_DURATION, \n" + 
			"    EVICTED_PLAN_TOTAL_EXECUTION_TIME, \n" + 
			"    EVICTED_PLAN_SIZE, \n" + 
			"    CACHED_PLAN_COUNT, \n" + 
			"    CACHED_PLAN_PREPARATION_COUNT, \n" + 
			"    CACHED_PLAN_EXECUTION_COUNT, \n" + 
			"    CACHED_PLAN_PREPARATION_TIME, \n" + 
			"    CACHED_PLAN_CURSOR_DURATION, \n" + 
			"    CACHED_PLAN_TOTAL_EXECUTION_TIME, \n" + 
			"    CLEAR_TIMESTAMP \n" + 
			"FROM M_SQL_PLAN_CACHE_OVERVIEW \n";

		return sql;
	}

//	RS> Col# Label                             JDBC Type Name           Guessed DBMS type
//	RS> ---- --------------------------------- ------------------------ -----------------
//	RS> 1    HOST                              java.sql.Types.VARCHAR   VARCHAR(64)      
//	RS> 2    PORT                              java.sql.Types.INTEGER   INTEGER          
//	RS> 3    PLAN_CACHE_ENABLED                java.sql.Types.INTEGER   INTEGER          
//	RS> 4    STATISTICS_COLLECTION_ENABLED     java.sql.Types.VARCHAR   VARCHAR(5)       
//	RS> 5    PLAN_CACHE_CAPACITY               java.sql.Types.BIGINT    BIGINT           
//	RS> 6    CACHED_PLAN_SIZE                  java.sql.Types.BIGINT    BIGINT           
//	RS> 7    PLAN_CACHE_HIT_RATIO              java.sql.Types.REAL      REAL             
//	RS> 8    PLAN_CACHE_LOOKUP_COUNT           java.sql.Types.BIGINT    BIGINT           
//	RS> 9    PLAN_CACHE_HIT_COUNT              java.sql.Types.BIGINT    BIGINT           
//	RS> 10   EVICTED_PLAN_AVG_CACHE_TIME       java.sql.Types.BIGINT    BIGINT           
//	RS> 11   EVICTED_PLAN_COUNT                java.sql.Types.BIGINT    BIGINT           
//	RS> 12   EVICTED_PLAN_PREPARATION_COUNT    java.sql.Types.BIGINT    BIGINT           
//	RS> 13   EVICTED_PLAN_EXECUTION_COUNT      java.sql.Types.BIGINT    BIGINT           
//	RS> 14   EVICTED_PLAN_PREPARATION_TIME     java.sql.Types.BIGINT    BIGINT           
//	RS> 15   EVICTED_PLAN_CURSOR_DURATION      java.sql.Types.BIGINT    BIGINT           
//	RS> 16   EVICTED_PLAN_TOTAL_EXECUTION_TIME java.sql.Types.BIGINT    BIGINT           
//	RS> 17   EVICTED_PLAN_SIZE                 java.sql.Types.BIGINT    BIGINT           
//	RS> 18   CACHED_PLAN_COUNT                 java.sql.Types.BIGINT    BIGINT           
//	RS> 19   CACHED_PLAN_PREPARATION_COUNT     java.sql.Types.BIGINT    BIGINT           
//	RS> 20   CACHED_PLAN_EXECUTION_COUNT       java.sql.Types.BIGINT    BIGINT           
//	RS> 21   CACHED_PLAN_PREPARATION_TIME      java.sql.Types.BIGINT    BIGINT           
//	RS> 22   CACHED_PLAN_CURSOR_DURATION       java.sql.Types.BIGINT    BIGINT           
//	RS> 23   CACHED_PLAN_TOTAL_EXECUTION_TIME  java.sql.Types.BIGINT    BIGINT           
//	RS> 24   CLEAR_TIMESTAMP                   java.sql.Types.TIMESTAMP TIMESTAMP        



	/** 
	 * Compute the CacheHitPct for DIFF values
	 */
	@Override
	public void localCalculation(CounterSample prevSample, CounterSample newSample, CounterSample diffData)
	{
		int CacheHitPctId = -1;

		int HitCount,        NumSearches;//,        NumStatementsDiff;
		int HitCountId = -1, NumSearchesId = -1;//, NumStatementsDiffId = -1;

		// Find column Id's
		List<String> colNames = diffData.getColNames();
		if (colNames == null)
			return;

		for (int colId=0; colId < colNames.size(); colId++) 
		{
			String colName = colNames.get(colId);
			if      (colName.equals("PLAN_CACHE_HIT_COUNT"))    HitCountId          = colId;
			else if (colName.equals("PLAN_CACHE_LOOKUP_COUNT")) NumSearchesId       = colId;
			else if (colName.equals("CACHE_HIT_PCT"))           CacheHitPctId       = colId;

			// No need to continue, we got all our columns
			if (    HitCountId          >= 0 
			     && NumSearchesId       >= 0 
			     && CacheHitPctId       >= 0  
			   )
				break;
		}

		// Loop on all diffData rows
		for (int rowId = 0; rowId < diffData.getRowCount(); rowId++)
		{
			HitCount          = ((Number)diffData.getValueAt(rowId, HitCountId         )).intValue();
			NumSearches       = ((Number)diffData.getValueAt(rowId, NumSearchesId      )).intValue();

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
		}
	}

	@Override
	public void updateGraphData(TrendGraphDataPoint tgdp)
	{
		if (GRAPH_NAME_REQUEST_PER_SEC.equals(tgdp.getName()))
		{
			Double[] arr = new Double[4];

			arr[0] = this.getRateValueSum("PLAN_CACHE_LOOKUP_COUNT");        // NumSearches
			arr[1] = this.getRateValueSum("PLAN_CACHE_HIT_COUNT");           // HitCount
			arr[2] = this.getRateValueSum("CACHED_PLAN_PREPARATION_COUNT");  // NumInserts
			arr[3] = this.getRateValueSum("EVICTED_PLAN_COUNT");             // NumRemovals
			
			if (_logger.isDebugEnabled())
				_logger.debug("updateGraphData("+tgdp.getName()+"): NumSearches='"+arr[0]+"', HitCount='"+arr[1]+"', NumInserts='"+arr[2]+"', NumRemovals='"+arr[3]+"'.");

			// Set the values
			tgdp.setDataPoint(this.getTimestamp(), arr);
//			tgdp.setDate(this.getTimestamp());
//			tgdp.setData(arr);
		}

		if (GRAPH_NAME_HIT_RATE_PCT.equals(tgdp.getName()))
		{
			Double[] arr = new Double[1];

			arr[0] = this.getRateValueAvgGtZero("CACHE_HIT_PCT");
			
			if (_logger.isDebugEnabled())
				_logger.debug("updateGraphData("+tgdp.getName()+"): CacheHitPct='"+arr[0]+"'.");

			// Set the values
			tgdp.setDataPoint(this.getTimestamp(), arr);
//			tgdp.setDate(this.getTimestamp());
//			tgdp.setData(arr);
		}
	}
}

