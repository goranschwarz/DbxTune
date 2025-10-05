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
package com.dbxtune.cm.ase;

import java.awt.event.MouseEvent;
import java.lang.invoke.MethodHandles;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import javax.naming.NameNotFoundException;

import org.apache.commons.lang3.ArrayUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.dbxtune.ICounterController;
import com.dbxtune.IGuiController;
import com.dbxtune.central.pcs.CentralPersistReader;
import com.dbxtune.cm.CounterSample;
import com.dbxtune.cm.CounterSetTemplates;
import com.dbxtune.cm.CounterSetTemplates.Type;
import com.dbxtune.cm.CountersModel.AggregationType;
import com.dbxtune.cm.CountersModel;
import com.dbxtune.cm.NoValidRowsInSample;
import com.dbxtune.config.dict.AseErrorMessageDictionary;
import com.dbxtune.config.dict.MonTablesDictionary;
import com.dbxtune.config.dict.MonTablesDictionaryManager;
import com.dbxtune.graph.TrendGraphDataPoint;
import com.dbxtune.graph.TrendGraphDataPoint.LabelType;
import com.dbxtune.gui.MainFrame;
import com.dbxtune.pcs.PersistentCounterHandler;
import com.dbxtune.pcs.sqlcapture.ISqlCaptureBroker;
import com.dbxtune.pcs.sqlcapture.SqlCaptureBrokerAse;
import com.dbxtune.pcs.sqlcapture.SqlCaptureStatementStatisticsSample;
import com.dbxtune.sql.conn.DbxConnection;
import com.dbxtune.sql.conn.info.DbmsVersionInfo;
import com.dbxtune.utils.Configuration;
import com.dbxtune.utils.StringUtil;
import com.google.common.reflect.TypeToken;
import com.google.gson.Gson;

/**
 * @author Goran Schwarz (goran_schwarz@hotmail.com)
 */
public class CmSqlStatementPerDb
extends CountersModel
{
	private static final Logger _logger = LogManager.getLogger(MethodHandles.lookup().lookupClass());
	private static final long    serialVersionUID = 1L;

	public static final String   CM_NAME          = CmSqlStatementPerDb.class.getSimpleName();
//	public static final String   SHORT_NAME       = "SQL Batch";
	public static final String   SHORT_NAME       = "SQL Statements/DB";
	public static final String   HTML_DESC        = 
		"<html>" +
		"SQL Statement/Batch Activity per database name<br>" +
		"In what <i>dbname</i> does most SQL Statements/Batches execute within, and how much resources do they consume.<br>" +
		"<br>" +
		"Gets records from monSysStatement (from the AseTune - SQL Capture Broker subsystem)<br>" + 
		"Note: If you have a Stored Procedure based system, you will have a lot of extra 'cnt' incrementations,<br>" +
		"this since one row in monSysStatement is produced for every statement in a stored proc.<br>" +
		"<br>" +
		"Reqirements for this to work" +
		"<ul>" +
		"  <li>You need to record the session</li>" +
		"  <li>Option 'Do SQL Capture and Store' for 'Statement Info' needs to be enabled.<br>Which is properties '"+PersistentCounterHandler.PROPKEY_sqlCap_doSqlCaptureAndStore+"' and '"+PersistentCounterHandler.PROPKEY_sqlCap_doStatementInfo+"'.</li>" +
		"  <li>ASE Configuration 'statement pipe active' and 'statement pipe max messages', needs to be enabled.</li>" +
		"</ul>" +
		"Note: If ASE config 'statement pipe max messages' is set to low, we might <i>miss</i> entries in the queue/event-pipe<br>" +
		"<br>" +
		"</html>";

	public static final String   GROUP_NAME       = MainFrame.TCP_GROUP_OBJECT_ACCESS;
	public static final String   GUI_ICON_FILE    = "images/"+CM_NAME+".png";

	public static final long     NEED_SRV_VERSION = 0;
	public static final long     NEED_CE_VERSION  = 0;

	public static final String[] MON_TABLES       = new String[] {"CmSqlStatementDb"};
	public static final String[] NEED_ROLES       = new String[] {"mon_role"};
	public static final String[] NEED_CONFIG      = new String[] {"statement pipe active"};

	public static final String[] PCT_COLUMNS      = new String[] {};
	public static final String[] DIFF_COLUMNS     = new String[] {
			 "totalCount"
			,"sqlBatchCount"
			,"errorCount"
			,"inStmntCacheCount"
			,"dynamicStmntCount"
			,"inProcedureCount"
			,"inProcNameNullCount"
			,"sumExecTimeMs"
			,"sumLogicalReads"
			,"sumPhysicalReads"
			,"sumCpuTime"
			,"sumWaitTime"
			,"sumRowsAffected"
			,"cntQueryOptimizationTimeGtZero"
			,"sumQueryOptimizationTime"
			//------------------------------------------
			,"compileCount"
			,"compileCount_withTime_gt10_lt100"
			,"compileCount_withTime_gt100_lt1000"
			,"compileCount_withTime_gt1000"
			,"sumCompileTime"

			,"compileCount_pureLang"
			,"compileCount_pureLang_withTime_gt10_lt100"
			,"compileCount_pureLang_withTime_gt100_lt1000"
			,"compileCount_pureLang_withTime_gt1000"
			,"sumCompileTime_pureLang"

			,"compileCount_langInStmntCache"
			,"compileCount_langInStmntCache_withTime_gt10_lt100"
			,"compileCount_langInStmntCache_withTime_gt100_lt1000"
			,"compileCount_langInStmntCache_withTime_gt1000"
			,"sumCompileTime_langInStmntCache"

			,"compileCount_dynamicInStmntCache"
			,"compileCount_dynamicInStmntCache_withTime_gt10_lt100"
			,"compileCount_dynamicInStmntCache_withTime_gt100_lt1000"
			,"compileCount_dynamicInStmntCache_withTime_gt1000"
			,"sumCompileTime_dynamicInStmntCache"
			//------------------------------------------
			};

	public static final boolean  NEGATIVE_DIFF_COUNTERS_TO_ZERO = true;
	public static final boolean  IS_SYSTEM_CM                   = true;
	public static final int      DEFAULT_POSTPONE_TIME          = 0;
	public static final int      DEFAULT_QUERY_TIMEOUT          = CountersModel.DEFAULT_sqlQueryTimeout;

	@Override public int     getDefaultPostponeTime()                 { return DEFAULT_POSTPONE_TIME; }
	@Override public int     getDefaultQueryTimeout()                 { return DEFAULT_QUERY_TIMEOUT; }
	@Override public boolean getDefaultIsNegativeDiffCountersToZero() { return NEGATIVE_DIFF_COUNTERS_TO_ZERO; }
	@Override public Type    getTemplateLevel()                       { return Type.OFF; }

	/**
	 * FACTORY  method to create the object
	 */
	public static CountersModel create(ICounterController counterController, IGuiController guiController)
	{
		if (guiController != null && guiController.hasGUI())
			guiController.splashWindowProgress("Loading: Counter Model '"+CM_NAME+"'");

		return new CmSqlStatementPerDb(counterController, guiController);
	}

	public CmSqlStatementPerDb(ICounterController counterController, IGuiController guiController)
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
		
//		addDependsOnCm(CmXxx.CM_NAME); // CMspinlockSum must have been executed before this cm

		addTrendGraphs();
		
		CounterSetTemplates.register(this);
	}

	//////////////////////////////////////////////////////////////////////////////////////////////////////////
	// NOTE: This is mostly the same as "CmSqlStatementPerDb" -- So changes might need to be synchronized
	//////////////////////////////////////////////////////////////////////////////////////////////////////////


	//------------------------------------------------------------
	// Implementation
	//------------------------------------------------------------
	public static final String GRAPH_NAME_SQL_STATEMENT_DB_TOT_EXEC_SEC          = "SsDbTotExecCnt";     // Total Statements Executed/sec
	public static final String GRAPH_NAME_SQL_STATEMENT_DB_TOT_BATCHES_SEC       = "SsDbTotBatch";       // SQL Batches Executed/sec
	public static final String GRAPH_NAME_SQL_STATEMENT_DB_TOT_STMNT_CACHE_SEC   = "SsDbTotStmntCache";  // SQL in Statement Cache Executed/sec
	public static final String GRAPH_NAME_SQL_STATEMENT_DB_TOT_DYNAMIC_SQL_SEC   = "SsDbTotDynSql";      // Dynamic SQL/sec
	public static final String GRAPH_NAME_SQL_STATEMENT_DB_TOT_STMNT_IN_PROC_SEC = "SsDbTotStmntInProc"; // Statements in Procedures Executed/sec

	public static final String GRAPH_NAME_SQL_STATEMENT_DB_SUM_EXEC_TIME         = "SsDbSumExecTime";
	public static final String GRAPH_NAME_SQL_STATEMENT_DB_SUM_LOGICAL_READ      = "SsDbSumLRead";
	public static final String GRAPH_NAME_SQL_STATEMENT_DB_SUM_PHYSICAL_READ     = "SsDbSumPRead";
	public static final String GRAPH_NAME_SQL_STATEMENT_DB_SUM_CPU_TIME          = "SsDbSumCpuTime";
	public static final String GRAPH_NAME_SQL_STATEMENT_DB_SUM_WAIT_TIME         = "SsDbSumWaitTime";
	public static final String GRAPH_NAME_SQL_STATEMENT_DB_SUM_ROWS_AFFECTED     = "SsDbSumRowsAfct";

	public static final String GRAPH_NAME_SQL_STATEMENT_DB_SUM_COMPILE_CNT       = "SsDbSumCmplCnt";
	public static final String GRAPH_NAME_SQL_STATEMENT_DB_SUM_COMPILE_TIME      = "SsDbSumCmplTime";
	public static final String GRAPH_NAME_SQL_STATEMENT_SUM_COMPILE_TYPE_CNT     = "SsSumCmplTypeCnt";

	public static final String GRAPH_NAME_SQL_STATEMENT_DB_ERROR_COUNT           = "SsDbSumErrorCnt";
	
	private void addTrendGraphs()
	{
		//---------------------------------------------------------------------------

		addTrendGraph(GRAPH_NAME_SQL_STATEMENT_DB_TOT_EXEC_SEC,
				"SQL Statements Per DB - Total Statements Executed/sec", // Menu CheckBox text
				"SQL Statements Per DB - Total Statements Executed/sec ("+GROUP_NAME+"->"+SHORT_NAME+")", // Label 
				TrendGraphDataPoint.createGraphProps(TrendGraphDataPoint.Y_AXIS_SCALE_LABELS_PERSEC, CentralPersistReader.SampleType.MAX_OVER_SAMPLES),
				null, 
				LabelType.Dynamic,
				TrendGraphDataPoint.Category.OPERATIONS,
				false, // is Percent Graph
				true,  // visible at start
				0,     // graph is valid from Server Version. 0 = All Versions; >0 = Valid from this version and above 	
				-1);   // minimum height

		addTrendGraph(GRAPH_NAME_SQL_STATEMENT_DB_TOT_BATCHES_SEC,
				"SQL Statements Per DB - SQL Batches Executed/sec", // Menu CheckBox text
				"SQL Statements Per DB - SQL Batches Executed/secc ("+GROUP_NAME+"->"+SHORT_NAME+")", // Label 
				TrendGraphDataPoint.createGraphProps(TrendGraphDataPoint.Y_AXIS_SCALE_LABELS_PERSEC, CentralPersistReader.SampleType.MAX_OVER_SAMPLES),
				null, 
				LabelType.Dynamic,
				TrendGraphDataPoint.Category.OPERATIONS,
				false, // is Percent Graph
				false, // visible at start
				0,     // graph is valid from Server Version. 0 = All Versions; >0 = Valid from this version and above 	
				-1);   // minimum height

		addTrendGraph(GRAPH_NAME_SQL_STATEMENT_DB_TOT_STMNT_CACHE_SEC,
				"SQL Statements Per DB - SQL in Statement Cache Executed/sec", // Menu CheckBox text
				"SQL Statements Per DB - SQL in Statement Cache Executed/sec ("+GROUP_NAME+"->"+SHORT_NAME+")", // Label 
				TrendGraphDataPoint.createGraphProps(TrendGraphDataPoint.Y_AXIS_SCALE_LABELS_PERSEC, CentralPersistReader.SampleType.MAX_OVER_SAMPLES),
				null, 
				LabelType.Dynamic,
				TrendGraphDataPoint.Category.OPERATIONS,
				false, // is Percent Graph
				false, // visible at start
				0,     // graph is valid from Server Version. 0 = All Versions; >0 = Valid from this version and above 	
				-1);   // minimum height

		addTrendGraph(GRAPH_NAME_SQL_STATEMENT_DB_TOT_DYNAMIC_SQL_SEC,
				"SQL Statements Per DB - Dynamic SQL/sec", // Menu CheckBox text
				"SQL Statements Per DB - Dynamic SQL/sec ("+GROUP_NAME+"->"+SHORT_NAME+")", // Label 
				TrendGraphDataPoint.createGraphProps(TrendGraphDataPoint.Y_AXIS_SCALE_LABELS_PERSEC, CentralPersistReader.SampleType.MAX_OVER_SAMPLES),
				null, 
				LabelType.Dynamic,
				TrendGraphDataPoint.Category.OPERATIONS,
				false, // is Percent Graph
				false, // visible at start
				0,     // graph is valid from Server Version. 0 = All Versions; >0 = Valid from this version and above 	
				-1);   // minimum height

		addTrendGraph(GRAPH_NAME_SQL_STATEMENT_DB_TOT_STMNT_IN_PROC_SEC,
				"SQL Statements Per DB - Statements in Procedures Executed/sec", // Menu CheckBox text
				"SQL Statements Per DB - Statements in Procedures Executed/sec ("+GROUP_NAME+"->"+SHORT_NAME+")", // Label 
				TrendGraphDataPoint.createGraphProps(TrendGraphDataPoint.Y_AXIS_SCALE_LABELS_PERSEC, CentralPersistReader.SampleType.MAX_OVER_SAMPLES),
				null, 
				LabelType.Dynamic,
				TrendGraphDataPoint.Category.OPERATIONS,
				false, // is Percent Graph
				false, // visible at start
				0,     // graph is valid from Server Version. 0 = All Versions; >0 = Valid from this version and above 	
				-1);   // minimum height

		//---------------------------------------------------------------------------

		addTrendGraph(GRAPH_NAME_SQL_STATEMENT_DB_SUM_EXEC_TIME,
				"SQL Statements Per DB - Sum Execution Time in ms", // Menu CheckBox text
				"SQL Statements Per DB - Sum Execution Time in ms ("+GROUP_NAME+"->"+SHORT_NAME+")", // Label 
				TrendGraphDataPoint.createGraphProps(TrendGraphDataPoint.Y_AXIS_SCALE_LABELS_MILLISEC, CentralPersistReader.SampleType.MAX_OVER_SAMPLES),
				null, 
				LabelType.Dynamic,
				TrendGraphDataPoint.Category.OPERATIONS,
				false, // is Percent Graph
				false, // visible at start
				0,     // graph is valid from Server Version. 0 = All Versions; >0 = Valid from this version and above 	
				-1);   // minimum height

		addTrendGraph(GRAPH_NAME_SQL_STATEMENT_DB_SUM_LOGICAL_READ,
				"SQL Statements Per DB - Sum Logical Reads", // Menu CheckBox text
				"SQL Statements Per DB - Sum Logical Reads ("+GROUP_NAME+"->"+SHORT_NAME+")", // Label 
				TrendGraphDataPoint.createGraphProps(TrendGraphDataPoint.Y_AXIS_SCALE_LABELS_NORMAL, CentralPersistReader.SampleType.MAX_OVER_SAMPLES),
				null, 
				LabelType.Dynamic,
				TrendGraphDataPoint.Category.OPERATIONS,
				false, // is Percent Graph
				false, // visible at start
				0,     // graph is valid from Server Version. 0 = All Versions; >0 = Valid from this version and above 	
				-1);   // minimum height

		addTrendGraph(GRAPH_NAME_SQL_STATEMENT_DB_SUM_PHYSICAL_READ,
				"SQL Statements Per DB - Sum Physical Reads", // Menu CheckBox text
				"SQL Statements Per DB - Sum Physical Reads ("+GROUP_NAME+"->"+SHORT_NAME+")", // Label 
				TrendGraphDataPoint.createGraphProps(TrendGraphDataPoint.Y_AXIS_SCALE_LABELS_NORMAL, CentralPersistReader.SampleType.MAX_OVER_SAMPLES),
				null, 
				LabelType.Dynamic,
				TrendGraphDataPoint.Category.OPERATIONS,
				false, // is Percent Graph
				false, // visible at start
				0,     // graph is valid from Server Version. 0 = All Versions; >0 = Valid from this version and above 	
				-1);   // minimum height

		addTrendGraph(GRAPH_NAME_SQL_STATEMENT_DB_SUM_CPU_TIME,
				"SQL Statements Per DB - Sum CPU Time in ms", // Menu CheckBox text
				"SQL Statements Per DB - Sum CPU Time in ms ("+GROUP_NAME+"->"+SHORT_NAME+")", // Label 
				TrendGraphDataPoint.createGraphProps(TrendGraphDataPoint.Y_AXIS_SCALE_LABELS_MILLISEC, CentralPersistReader.SampleType.MAX_OVER_SAMPLES),
				null, 
				LabelType.Dynamic,
				TrendGraphDataPoint.Category.OPERATIONS,
				false, // is Percent Graph
				false, // visible at start
				0,     // graph is valid from Server Version. 0 = All Versions; >0 = Valid from this version and above 	
				-1);   // minimum height

		addTrendGraph(GRAPH_NAME_SQL_STATEMENT_DB_SUM_WAIT_TIME,
				"SQL Statements Per DB - Sum Wait Time in ms", // Menu CheckBox text
				"SQL Statements Per DB - Sum Wait Time in ms ("+GROUP_NAME+"->"+SHORT_NAME+")", // Label 
				TrendGraphDataPoint.createGraphProps(TrendGraphDataPoint.Y_AXIS_SCALE_LABELS_MILLISEC, CentralPersistReader.SampleType.MAX_OVER_SAMPLES),
				null, 
				LabelType.Dynamic,
				TrendGraphDataPoint.Category.OPERATIONS,
				false, // is Percent Graph
				false, // visible at start
				0,     // graph is valid from Server Version. 0 = All Versions; >0 = Valid from this version and above 	
				-1);   // minimum height

		addTrendGraph(GRAPH_NAME_SQL_STATEMENT_DB_SUM_ROWS_AFFECTED,
				"SQL Statements Per DB - Sum Number of Rows Affected", // Menu CheckBox text
				"SQL Statements Per DB - Sum Number of Rows Affected ("+GROUP_NAME+"->"+SHORT_NAME+")", // Label 
				TrendGraphDataPoint.createGraphProps(TrendGraphDataPoint.Y_AXIS_SCALE_LABELS_NORMAL, CentralPersistReader.SampleType.MAX_OVER_SAMPLES),
				null, 
				LabelType.Dynamic,
				TrendGraphDataPoint.Category.OPERATIONS,
				false, // is Percent Graph
				false, // visible at start
				0,     // graph is valid from Server Version. 0 = All Versions; >0 = Valid from this version and above 	
				-1);   // minimum height

		//-----
		addTrendGraph(GRAPH_NAME_SQL_STATEMENT_DB_SUM_COMPILE_CNT,
			"SQL Statements Per DB - Sum Query Compile/Optimization Count per Sec", // Menu CheckBox text
			"SQL Statements Per DB - Sum Query Compile/Optimization Count per Sec ("+GROUP_NAME+"->"+SHORT_NAME+")", // Label 
			TrendGraphDataPoint.createGraphProps(TrendGraphDataPoint.Y_AXIS_SCALE_LABELS_PERSEC, CentralPersistReader.SampleType.MAX_OVER_SAMPLES),
//			new String[] { "0ms & NoLReads", "<1ms", "1-2ms", "2-5ms", "5-10ms", "10-20ms", "20-50ms", "50-100ms", "100-200ms", "200-500ms", "500ms-1s", "1-2s", "2-5s", "5-10s", "10-20s", "20-50s", "50-100s", ">100s" }, 
//			LabelType.Static,
			null, 
			LabelType.Dynamic,
			TrendGraphDataPoint.Category.OPERATIONS,
			false, // is Percent Graph
			false, // visible at start
			0,     // graph is valid from Server Version. 0 = All Versions; >0 = Valid from this version and above 
			-1);   // minimum height

		//-----
		addTrendGraph(GRAPH_NAME_SQL_STATEMENT_DB_SUM_COMPILE_TIME,
			"SQL Statements Per DB - Sum Query Compile/Optimization Time", // Menu CheckBox text
			"SQL Statements Per DB - Sum Query Compile/Optimization Time ("+GROUP_NAME+"->"+SHORT_NAME+")", // Label 
			TrendGraphDataPoint.createGraphProps(TrendGraphDataPoint.Y_AXIS_SCALE_LABELS_MILLISEC, CentralPersistReader.SampleType.MAX_OVER_SAMPLES),
//			new String[] { "0ms & NoLReads", "<1ms", "1-2ms", "2-5ms", "5-10ms", "10-20ms", "20-50ms", "50-100ms", "100-200ms", "200-500ms", "500ms-1s", "1-2s", "2-5s", "5-10s", "10-20s", "20-50s", "50-100s", ">100s" }, 
//			LabelType.Static,
			null, 
			LabelType.Dynamic,
			TrendGraphDataPoint.Category.OPERATIONS,
			false, // is Percent Graph
			false, // visible at start
			0,     // graph is valid from Server Version. 0 = All Versions; >0 = Valid from this version and above 
			-1);   // minimum height

		//-----
		addTrendGraph(GRAPH_NAME_SQL_STATEMENT_SUM_COMPILE_TYPE_CNT,
			"SQL Statements - Sum Query Compile/Optimization Type Count per Sec", // Menu CheckBox text
			"SQL Statements - Sum Query Compile/Optimization Type Count per Sec ("+GROUP_NAME+"->"+SHORT_NAME+")", // Label 
			TrendGraphDataPoint.createGraphProps(TrendGraphDataPoint.Y_AXIS_SCALE_LABELS_PERSEC, CentralPersistReader.SampleType.MAX_OVER_SAMPLES),
			new String[] { "Pure Language", "Language In Statement Cache", "Dynamic (Prepared) Statements In Statement Cache3" }, 
			LabelType.Static,
			TrendGraphDataPoint.Category.OPERATIONS,
			false, // is Percent Graph
			false, // visible at start
			0,     // graph is valid from Server Version. 0 = All Versions; >0 = Valid from this version and above 
			-1);   // minimum height

		//---------------------------------------------------------------------------

		addTrendGraph(GRAPH_NAME_SQL_STATEMENT_DB_ERROR_COUNT,
				"SQL Statements Per DB - Errors/sec", // Menu CheckBox text
				"SQL Statements Per DB - Errors/sec ("+GROUP_NAME+"->"+SHORT_NAME+")", // Label 
				TrendGraphDataPoint.createGraphProps(TrendGraphDataPoint.Y_AXIS_SCALE_LABELS_PERSEC, CentralPersistReader.SampleType.MAX_OVER_SAMPLES),
				null, 
				LabelType.Dynamic,
				TrendGraphDataPoint.Category.OPERATIONS,
				false, // is Percent Graph
				false, // visible at start
				0,     // graph is valid from Server Version. 0 = All Versions; >0 = Valid from this version and above 	
				-1);   // minimum height

	}

	@Override
	public void updateGraphData(TrendGraphDataPoint tgdp)
	{
		// -----------------------------------------------------------------------------------------
		if (GRAPH_NAME_SQL_STATEMENT_DB_TOT_EXEC_SEC         .equals(tgdp.getName())) private_updateGraphData(tgdp, "totalCount");
		if (GRAPH_NAME_SQL_STATEMENT_DB_TOT_BATCHES_SEC      .equals(tgdp.getName())) private_updateGraphData(tgdp, "sqlBatchCount");
		if (GRAPH_NAME_SQL_STATEMENT_DB_TOT_STMNT_CACHE_SEC  .equals(tgdp.getName())) private_updateGraphData(tgdp, "inStmntCacheCount");
		if (GRAPH_NAME_SQL_STATEMENT_DB_TOT_DYNAMIC_SQL_SEC  .equals(tgdp.getName())) private_updateGraphData(tgdp, "dynamicStmntCount");
		if (GRAPH_NAME_SQL_STATEMENT_DB_TOT_STMNT_IN_PROC_SEC.equals(tgdp.getName())) private_updateGraphData(tgdp, "inProcedureCount");
		
		if (GRAPH_NAME_SQL_STATEMENT_DB_SUM_EXEC_TIME        .equals(tgdp.getName())) private_updateGraphData(tgdp, "sumExecTimeMs");
		if (GRAPH_NAME_SQL_STATEMENT_DB_SUM_LOGICAL_READ     .equals(tgdp.getName())) private_updateGraphData(tgdp, "sumLogicalReads");
		if (GRAPH_NAME_SQL_STATEMENT_DB_SUM_PHYSICAL_READ    .equals(tgdp.getName())) private_updateGraphData(tgdp, "sumPhysicalReads");
		if (GRAPH_NAME_SQL_STATEMENT_DB_SUM_CPU_TIME         .equals(tgdp.getName())) private_updateGraphData(tgdp, "sumCpuTime");
		if (GRAPH_NAME_SQL_STATEMENT_DB_SUM_WAIT_TIME        .equals(tgdp.getName())) private_updateGraphData(tgdp, "sumWaitTime");
		if (GRAPH_NAME_SQL_STATEMENT_DB_SUM_ROWS_AFFECTED    .equals(tgdp.getName())) private_updateGraphData(tgdp, "sumRowsAffected");

		if (GRAPH_NAME_SQL_STATEMENT_DB_SUM_COMPILE_CNT      .equals(tgdp.getName())) private_updateGraphData(tgdp, "cntQueryOptimizationTimeGtZero");
		if (GRAPH_NAME_SQL_STATEMENT_DB_SUM_COMPILE_TIME     .equals(tgdp.getName())) private_updateGraphData(tgdp, "sumQueryOptimizationTime");
		
		if (GRAPH_NAME_SQL_STATEMENT_DB_ERROR_COUNT          .equals(tgdp.getName())) private_updateGraphData(tgdp, "errorCount");

		if (GRAPH_NAME_SQL_STATEMENT_SUM_COMPILE_TYPE_CNT.equals(tgdp.getName()))
		{
			// get Aggre
			if (hasAggregatedRowId())
			{
				Double[] arr = new Double[3];

				int aggRowId = getAggregatedRowId();

				arr[0] = this.getRateValueAsDouble(aggRowId, "compileCount_pureLang");
				arr[1] = this.getRateValueAsDouble(aggRowId, "compileCount_langInStmntCache");
				arr[2] = this.getRateValueAsDouble(aggRowId, "compileCount_dynamicInStmntCache");

				// Set the values
				tgdp.setDataPoint(this.getTimestamp(), arr);
			}
		}
	}

	private void private_updateGraphData(TrendGraphDataPoint tgdp, String serieName)
	{
		Double[] dArray = new Double[this.size()];
		String[] lArray = new String[dArray.length];
		for (int i = 0; i < dArray.length; i++)
		{
			lArray[i] = this.getRateString       (i, "dbname");
			dArray[i] = this.getRateValueAsDouble(i, serieName);
		}

		// If '_Total' is the LAST entry (which is the normal) -->> Move it to First Entry
		if (lArray[lArray.length-1].equals("_Total"))
		{
			ArrayUtils.shift(lArray, 1);
			ArrayUtils.shift(dArray, 1);
		}

		// Set the values
		tgdp.setDataPoint(this.getTimestamp(), lArray, dArray);
	}

//	@Override
//	protected TabularCntrPanel createGui()
//	{
//		return new ACopyMePanel(this);
//	}

	@Override
	public void addMonTableDictForVersion(DbxConnection conn, DbmsVersionInfo versionInfo)
	{
		try 
		{
			String tabName = "CmSqlStatementDb";

			MonTablesDictionary mtd = MonTablesDictionaryManager.getInstance();
			mtd.addTable(tabName,  "xxx.");

			mtd.addColumn(tabName, "dbname",               "<html>Execution time for this database</html>");

			mtd.addColumn(tabName, "totalCount",           "<html>Summary value for 'inSqlBatchCount', 'inStmntCacheCount', 'dynamicStmntCount' and 'inProcedureCount'.</html>");
			mtd.addColumn(tabName, "totalCountAbs",        "<html>Summary value for 'inSqlBatchCount', 'inStmntCacheCount', 'dynamicStmntCount' and 'inProcedureCount'.</html>");

			mtd.addColumn(tabName, "sqlBatchCount",        "<html>Estimated SQL Batch requests. <br><b>Algorithm:</b> if columns 'ProcName' is NULL from monSysStatement <br><b>Note:</b> If the SqlBatch contains several statements, the counter will be incremented for every statement within the SQL Batch.</html>");
			mtd.addColumn(tabName, "sqlBatchCountAbs",     "<html>Estimated SQL Batch requests. <br><b>Algorithm:</b> if columns 'ProcName' is NULL from monSysStatement <br><b>Note:</b> If the SqlBatch contains several statements, the counter will be incremented for every statement within the SQL Batch.</html>");

			mtd.addColumn(tabName, "errorCount",           "<html>Summary value for number of records that had a 'ErrorStatus' greater than 0.</html>");
			mtd.addColumn(tabName, "errorCountAbs",        "<html>Summary value for number of records that had a 'ErrorStatus' greater than 0.</html>");

			mtd.addColumn(tabName, "inStmntCacheCount",    "<html>Estimated SQL Statements Batch requests executed as a Statement Cache (compiled). <br><b>Algorithm:</b> if columns 'ProcName' starts with '*ss' from monSysStatement</html>");
			mtd.addColumn(tabName, "inStmntCacheCountAbs", "<html>Estimated SQL Statements Batch requests executed as a Statement Cache (compiled). <br><b>Algorithm:</b> if columns 'ProcName' starts with '*ss' from monSysStatement</html>");

			mtd.addColumn(tabName, "dynamicStmntCount",    "<html>Estimated Dynamic SQL Statements (ct_dynamic or PreparedStatement using a LWP). <br><b>Algorithm:</b> if columns 'ProcName' starts with '*sq' from monSysStatement</html>");
			mtd.addColumn(tabName, "dynamicStmntCountAbs", "<html>Estimated Dynamic SQL Statements (ct_dynamic or PreparedStatement using a LWP). <br><b>Algorithm:</b> if columns 'ProcName' starts with '*sq' from monSysStatement</html>");

			mtd.addColumn(tabName, "inProcedureCount",     "<html>Estimated SQL Statements requests executed from withing a Stored Proc. <br><b>Algorithm:</b> if columns 'ProcName' does NOT start with '*sq' or '*ss' from monSysStatement</html>");
			mtd.addColumn(tabName, "inProcedureCountAbs",  "<html>Estimated SQL Statements requests executed from withing a Stored Proc. <br><b>Algorithm:</b> if columns 'ProcName' does NOT start with '*sq' or '*ss' from monSysStatement</html>");
			
			mtd.addColumn(tabName, "inProcNameNullCount",     "<html>Estimated SQL Statements requests executed where ProcedureId was not 0, but the ProcedureName could NOT be found.. <br><b>Algorithm:</b> if columns 'ProcedureId' != 0 and 'ProcName' is NULL from monSysStatement</html>");
			mtd.addColumn(tabName, "inProcNameNullCountAbs",  "<html>Estimated SQL Statements requests executed where ProcedureId was not 0, but the ProcedureName could NOT be found.. <br><b>Algorithm:</b> if columns 'ProcedureId' != 0 and 'ProcName' is NULL from monSysStatement</html>");
			
			mtd.addColumn(tabName, "sumExecTimeMs",        "<html>Summary of all Executions for this time span.<br>Also 'diff' and 'rate' calculated to get a sence for the changes<br>But for this metrics it's probably more interesting to look at the ABS column (to the right)</html>");
			mtd.addColumn(tabName, "sumExecTimeMsAbs",     "<html>Summary of all Executions for this time span.</html>");
			mtd.addColumn(tabName, "avgExecTimeMs",        "<html>Average execution time for this time span.<br><b>Algorithm:</b> abs.sumExecTimeMs / abs.totalCount</html>");
			mtd.addColumn(tabName, "maxExecTimeMs",        "<html>Maximum execution time for this time span.</html>");
			                                                        
			mtd.addColumn(tabName, "sumLogicalReads",      "<html>Summary of all LogicalReads for this time span.<br>Also 'diff' and 'rate' calculated to get a sence for the changes</html>");
			mtd.addColumn(tabName, "sumLogicalReadsAbs",   "<html>Summary of all LogicalReads for this time span.</html>");
			mtd.addColumn(tabName, "avgLogicalReads",      "<html>Average LogicalReads for this time span.<br><b>Algorithm:</b> abs.sumLogicalReads / abs.totalCount</html>");
			mtd.addColumn(tabName, "maxLogicalReads",      "<html>Maximum LogicalReads for this time span.</html>");
                                                                    
			mtd.addColumn(tabName, "sumPhysicalReads",     "<html>Summary of all PhysicalReads for this time span.<br>Also 'diff' and 'rate' calculated to get a sence for the changes</html>");
			mtd.addColumn(tabName, "sumPhysicalReadsAbs",  "<html>Summary of all PhysicalReads for this time span.</html>");
			mtd.addColumn(tabName, "avgPhysicalReads",     "<html>Average PhysicalReads for this time span.<br><b>Algorithm:</b> abs.sumPhysicalReads / abs.totalCount</html>");
			mtd.addColumn(tabName, "maxPhysicalReads",     "<html>Maximum PhysicalReads for this time span.</html>");
                                                                    
			mtd.addColumn(tabName, "sumCpuTime",           "<html>Summary of all CpuTime for this time span.<br>Also 'diff' and 'rate' calculated to get a sence for the changes<br>But for this metrics it's probably more interesting to look at the ABS column (to the right)</html>");
			mtd.addColumn(tabName, "sumCpuTimeAbs",        "<html>Summary of all CpuTime for this time span.</html>");
			mtd.addColumn(tabName, "avgCpuTime",           "<html>Average CpuTime for this time span.<br><b>Algorithm:</b> abs.sumCpuTime / abs.totalCount</html>");
			mtd.addColumn(tabName, "maxCpuTime",           "<html>Maximum CpuTime for this time span.</html>");
                                                                    
			mtd.addColumn(tabName, "sumWaitTime",          "<html>Summary of all WaitTime for this time span.<br>Also 'diff' and 'rate' calculated to get a sence for the changes<br>But for this metrics it's probably more interesting to look at the ABS column (to the right)</html>");
			mtd.addColumn(tabName, "sumWaitTimeAbs",       "<html>Summary of all WaitTime for this time span.</html>");
			mtd.addColumn(tabName, "avgWaitTime",          "<html>Average WaitTime for this time span.<br><b>Algorithm:</b> abs.sumWaitTime / abs.totalCount</html>");
			mtd.addColumn(tabName, "maxWaitTime",          "<html>Maximum WaitTime for this time span.</html>");
                                                                    
			mtd.addColumn(tabName, "sumRowsAffected",      "<html>Summary of all RowsAffected for this time span.<br>Also 'diff' and 'rate' calculated to get a sence for the changes</html>");
			mtd.addColumn(tabName, "sumRowsAffectedAbs",   "<html>Summary of all RowsAffected for this time span.</html>");
			mtd.addColumn(tabName, "avgRowsAffected",      "<html>Average RowsAffected for this time span.<br><b>Algorithm:</b> abs.sumRowsAffected / abs.totalCount</html>");
			mtd.addColumn(tabName, "maxRowsAffected",      "<html>Maximum RowsAffected for this time span.</html>");

			mtd.addColumn(tabName, "cntQueryOptimizationTimeGtZero",    "<html>Summary of all QueryOptimizationTime with values above 0 for this time span. <br>Also 'diff' and 'rate' calculated to get a sence for the changes</html>");
			mtd.addColumn(tabName, "cntQueryOptimizationTimeGtZeroAbs", "<html>Summary of all QueryOptimizationTime with values above 0 for this time span.</html>");
			mtd.addColumn(tabName, "sumQueryOptimizationTime",          "<html>Summary of all QueryOptimizationTime for this time span. <br>Also 'diff' and 'rate' calculated to get a sence for the changes</html>");
			mtd.addColumn(tabName, "sumQueryOptimizationTimeAbs",       "<html>Summary of all QueryOptimizationTime for this time span.</html>");
			mtd.addColumn(tabName, "avgQueryOptimizationTime",          "<html>Average QueryOptimizationTime for this time span.<br> <b>Algorithm:</b> abs.sumQueryOptimizationTime / abs.cntQueryOptimizationTimeGtZero</html>");
			mtd.addColumn(tabName, "maxQueryOptimizationTime",          "<html>Maximum QueryOptimizationTime for this time span.</html>");
			
			mtd.addColumn(tabName, "compileCount",                                           "<html>Fixme</html>");
			mtd.addColumn(tabName, "compileCountAbs",                                        "<html>Fixme</html>");
			mtd.addColumn(tabName, "compileCount_withTime_gt10_lt100",                       "<html>Fixme</html>");
			mtd.addColumn(tabName, "compileCount_withTime_gt100_lt1000",                     "<html>Fixme</html>");
			mtd.addColumn(tabName, "compileCount_withTime_gt1000",                           "<html>Fixme</html>");

			mtd.addColumn(tabName, "sumCompileTime",                                         "<html>Fixme</html>");
			mtd.addColumn(tabName, "sumCompileTimeAbs",                                      "<html>Fixme</html>");
			mtd.addColumn(tabName, "avgCompileTime",                                         "<html>Fixme</html>");
			mtd.addColumn(tabName, "maxCompileTime",                                         "<html>Fixme</html>");

			mtd.addColumn(tabName, "compileCount_pureLang",                                  "<html>Fixme</html>");
			mtd.addColumn(tabName, "compileCount_pureLangAbs",                               "<html>Fixme</html>");
			mtd.addColumn(tabName, "compileCount_pureLang_withTime_gt10_lt100",              "<html>Fixme</html>");
			mtd.addColumn(tabName, "compileCount_pureLang_withTime_gt100_lt1000",            "<html>Fixme</html>");
			mtd.addColumn(tabName, "compileCount_pureLang_withTime_gt1000",                  "<html>Fixme</html>");

			mtd.addColumn(tabName, "sumCompileTime_pureLang",                                "<html>Fixme</html>");
			mtd.addColumn(tabName, "sumCompileTime_pureLangAbs",                             "<html>Fixme</html>");
			mtd.addColumn(tabName, "avgCompileTime_pureLang",                                "<html>Fixme</html>");
			mtd.addColumn(tabName, "maxCompileTime_pureLang",                                "<html>Fixme</html>");

			mtd.addColumn(tabName, "compileCount_langInStmntCache",                          "<html>Fixme</html>");
			mtd.addColumn(tabName, "compileCount_langInStmntCacheAbs",                       "<html>Fixme</html>");
			mtd.addColumn(tabName, "compileCount_langInStmntCache_withTime_gt10_lt100",      "<html>Fixme</html>");
			mtd.addColumn(tabName, "compileCount_langInStmntCache_withTime_gt100_lt1000",    "<html>Fixme</html>");
			mtd.addColumn(tabName, "compileCount_langInStmntCache_withTime_gt1000",          "<html>Fixme</html>");

			mtd.addColumn(tabName, "sumCompileTime_langInStmntCache",                        "<html>Fixme</html>");
			mtd.addColumn(tabName, "sumCompileTime_langInStmntCacheAbs",                     "<html>Fixme</html>");
			mtd.addColumn(tabName, "avgCompileTime_langInStmntCache",                        "<html>Fixme</html>");
			mtd.addColumn(tabName, "maxCompileTime_langInStmntCache",                        "<html>Fixme</html>");

			mtd.addColumn(tabName, "compileCount_dynamicInStmntCache",                       "<html>Fixme</html>");
			mtd.addColumn(tabName, "compileCount_dynamicInStmntCacheAbs",                    "<html>Fixme</html>");
			mtd.addColumn(tabName, "compileCount_dynamicInStmntCache_withTime_gt10_lt100",   "<html>Fixme</html>");
			mtd.addColumn(tabName, "compileCount_dynamicInStmntCache_withTime_gt100_lt1000", "<html>Fixme</html>");
			mtd.addColumn(tabName, "compileCount_dynamicInStmntCache_withTime_gt1000",       "<html>Fixme</html>");

			mtd.addColumn(tabName, "sumCompileTime_dynamicInStmntCache",                     "<html>Fixme</html>");
			mtd.addColumn(tabName, "sumCompileTime_dynamicInStmntCacheAbs",                  "<html>Fixme</html>");
			mtd.addColumn(tabName, "avgCompileTime_dynamicInStmntCache",                     "<html>Fixme</html>");
			mtd.addColumn(tabName, "maxCompileTime_dynamicInStmntCache",                     "<html>Fixme</html>");
			
//			mtd.addColumn(tabName, "errorMsgCountMap",      "<html>A JSON String, which contains: {\"MsgNumber\"=count, \"MsgNumber\"=count}.</html>");
			mtd.addColumn(tabName, "errorMsgCountMap",      "<html>A JSON String, which contains: {\"MsgNumber\"={\"dbname\"=count}, \"MsgNumber\"={\"dbname\"=count}}.</html>");
		}
		catch (NameNotFoundException e) 
		{
			_logger.warn("Problems in cm='" + CM_NAME + "', adding addMonTableDictForVersion. Caught: " + e); 
		//	System.out.println("Problems in cm='" + CM_NAME + "', adding addMonTableDictForVersion. Caught: " + e); 
		}
	}

	@Override
	public String[] getDependsOnConfigForVersion(DbxConnection conn, DbmsVersionInfo versionInfo)
	{
		return NEED_CONFIG;
	}

	@Override
	public boolean checkDependsOnConfig(DbxConnection conn)
	{
		boolean ok =  super.checkDependsOnConfig(conn);
		if (ok)
		{
			if ( ! PersistentCounterHandler.hasInstance() )
			{
				_logger.warn("When trying to initialize Counters Model '"+getName()+"', named '"+getDisplayName()+"'. No recording is active, which this CM depends on.");
				setActive(false, "No recording is active, which this CM depends on.");
				return false;
			}
			
			Configuration conf = PersistentCounterHandler.getInstance().getConfig();

			boolean sqlCap_doSqlCaptureAndStore = conf.getBooleanProperty(PersistentCounterHandler.PROPKEY_sqlCap_doSqlCaptureAndStore, PersistentCounterHandler.DEFAULT_sqlCap_doSqlCaptureAndStore);
			boolean sqlCap_doStatementInfo      = conf.getBooleanProperty(PersistentCounterHandler.PROPKEY_sqlCap_doStatementInfo,      PersistentCounterHandler.DEFAULT_sqlCap_doStatementInfo);

			if ( ! sqlCap_doSqlCaptureAndStore )
			{
				_logger.warn("When trying to initialize Counters Model '"+getName()+"', named '"+getDisplayName()+"'. Configuration '"+PersistentCounterHandler.PROPKEY_sqlCap_doSqlCaptureAndStore+"' is NOT enabled");
				setActive(false, "Configuration '"+PersistentCounterHandler.PROPKEY_sqlCap_doSqlCaptureAndStore+"' is NOT enabled, which this CM depends on.");
				return false;
			}

			if ( ! sqlCap_doStatementInfo )
			{
				_logger.warn("When trying to initialize Counters Model '"+getName()+"', named '"+getDisplayName()+"'. Configuration '"+PersistentCounterHandler.PROPKEY_sqlCap_doStatementInfo+"' is NOT enabled");
				setActive(false, "Configuration '"+PersistentCounterHandler.PROPKEY_sqlCap_doStatementInfo+"' is NOT enabled, which this CM depends on.");
				return false;
			}
		}
		return ok;
	}
	
	@Override
	public String getSqlForVersion(DbxConnection conn, DbmsVersionInfo versionInfo)
	{
		return "-- grabed from AseTune internally: PersistentCounterHandler.getInstance().getSqlCaptureBroker().getStatementStats(false)";
	}

	@Override
	public List<String> getPkForVersion(DbxConnection conn, DbmsVersionInfo versionInfo)
	{
		List <String> pkCols = new LinkedList<String>();

		pkCols.add("dbname");

		return pkCols;
	}

	@Override
	public void close()
	{
		super.close();

		// reset/close the SqlCapture Statistics
		// OR: maybe this should be done when the PersistentCounterHandler is stopping/closing
		if ( PersistentCounterHandler.hasInstance() )
		{
			ISqlCaptureBroker sqlCapBroker = PersistentCounterHandler.getInstance().getSqlCaptureBroker();
			if (sqlCapBroker != null && sqlCapBroker instanceof SqlCaptureBrokerAse)
			{
				SqlCaptureBrokerAse aseSqlCapBroker = (SqlCaptureBrokerAse)sqlCapBroker;
				aseSqlCapBroker.closeStatementStats();
			}
		}
	}

	
	@Override
	public CounterSample createCounterSample(String name, boolean negativeDiffCountersToZero, String[] diffColumns, CounterSample prevSample)
	{
		return new CounterSamplePrivate(name, negativeDiffCountersToZero, diffColumns, prevSample);
	}
	
	private static class CounterSamplePrivate
	extends CounterSample
	{
		private static final long serialVersionUID = 1L;

		public CounterSamplePrivate(String name, boolean negativeDiffCountersToZero, String[] diffColumns, CounterSample prevSample)
		{
			super(name, negativeDiffCountersToZero, diffColumns, prevSample);
		}
		
		@Override
		public boolean getSample(CountersModel cm, DbxConnection conn, String sql, List<String> pkList) throws SQLException, NoValidRowsInSample
		{
			if ( ! PersistentCounterHandler.hasInstance() )
				throw new SQLException("unable to retrive 'Statement Statistics Object'. No PersistentCounterHandler is available.");

			SqlCaptureStatementStatisticsSample sqlCapStat = null;
			
			PersistentCounterHandler pch = PersistentCounterHandler.getInstance();
			ISqlCaptureBroker sqlCapBroker = pch.getSqlCaptureBroker();
			if (sqlCapBroker != null && sqlCapBroker instanceof SqlCaptureBrokerAse)
			{
				SqlCaptureBrokerAse aseSqlCapBroker = (SqlCaptureBrokerAse)sqlCapBroker;
				sqlCapStat = aseSqlCapBroker.getStatementStats(false);
			}
			if (sqlCapStat == null)
				throw new SQLException("unable to retrive 'Statement Statistics Object'.");

			// update/set the current refresh time and interval
			updateSampleTime(conn, cm);

			// Use this to detect if the "SQL Capture Thread" has died...
//			((CmSqlStatementPerDb)cm)._sqlCaptureLastUpdateTime = sqlCapStat.getLastUpdateTime();
			
			// create a "bucket" where all the rows will end up in ( add will be done in method: readResultset() )
			_rows = new ArrayList<List<Object>>();

//			ResultSet rs = sqlCapStat.toResultSet();
			ResultSet rs = sqlCapStat.toResultSetDbName();

			int rsNum = 0;
			
			ResultSetMetaData originRsmd = rs.getMetaData();
			if ( ! cm.hasResultSetMetaData() )
				cm.setResultSetMetaData( cm.createResultSetMetaData(originRsmd) );

			// The above "remapps" some things...
			//  - Like in Oracle 'NUMBER(0,-127)' is mapped to INTEGER
			// So we should use this when calling readResultset()...
			ResultSetMetaData translatedRsmd = cm.getResultSetMetaData();


			if (readResultset(cm, rs, translatedRsmd, originRsmd, pkList, rsNum))
				rs.close();

			return true;
		}
	};
	
	@Override
	public Map<String, AggregationType> createAggregateColumns()
	{
		HashMap<String, AggregationType> aggColumns = new HashMap<>(getColumnCount());

		AggregationType tmp;
		
		// Create the columns ::::::::::::::::::::::::::::::::::::::::::::::::::::::::: And ADD it to the return Map 
		// The list is grabbed from: SqlCaptureStatementStatisticsSample.toResultSet()
//		tmp = new AggregationType("dbname",                 Types.VARCHAR, 30, 0);
        
		tmp = new AggregationType("totalCount",             AggregationType.Agg.SUM);   aggColumns.put(tmp.getColumnName(), tmp);
		tmp = new AggregationType("sqlBatchCount",          AggregationType.Agg.SUM);   aggColumns.put(tmp.getColumnName(), tmp);
		tmp = new AggregationType("errorCount",             AggregationType.Agg.SUM);   aggColumns.put(tmp.getColumnName(), tmp);
		tmp = new AggregationType("inStmntCacheCount",      AggregationType.Agg.SUM);   aggColumns.put(tmp.getColumnName(), tmp);
		tmp = new AggregationType("dynamicStmntCount",      AggregationType.Agg.SUM);   aggColumns.put(tmp.getColumnName(), tmp);
		tmp = new AggregationType("inProcedureCount",       AggregationType.Agg.SUM);   aggColumns.put(tmp.getColumnName(), tmp);
		tmp = new AggregationType("inProcNameNullCount",    AggregationType.Agg.SUM);   aggColumns.put(tmp.getColumnName(), tmp);

		tmp = new AggregationType("totalCountAbs",          AggregationType.Agg.SUM);   aggColumns.put(tmp.getColumnName(), tmp);
		tmp = new AggregationType("sqlBatchCountAbs",       AggregationType.Agg.SUM);   aggColumns.put(tmp.getColumnName(), tmp);
		tmp = new AggregationType("inStmntCacheCountAbs",   AggregationType.Agg.SUM);   aggColumns.put(tmp.getColumnName(), tmp);
		tmp = new AggregationType("dynamicStmntCountAbs",   AggregationType.Agg.SUM);   aggColumns.put(tmp.getColumnName(), tmp);
		tmp = new AggregationType("inProcedureCountAbs",    AggregationType.Agg.SUM);   aggColumns.put(tmp.getColumnName(), tmp);
		tmp = new AggregationType("inProcNameNullCountAbs", AggregationType.Agg.SUM);   aggColumns.put(tmp.getColumnName(), tmp);

		tmp = new AggregationType("sumExecTimeMs",          AggregationType.Agg.SUM);   aggColumns.put(tmp.getColumnName(), tmp);
		tmp = new AggregationType("sumExecTimeMsAbs",       AggregationType.Agg.SUM);   aggColumns.put(tmp.getColumnName(), tmp);
		tmp = new AggregationType("avgExecTimeMs",          AggregationType.Agg.SUM);   aggColumns.put(tmp.getColumnName(), tmp);
		tmp = new AggregationType("maxExecTimeMs",          AggregationType.Agg.MAX);   aggColumns.put(tmp.getColumnName(), tmp);

		tmp = new AggregationType("sumLogicalReads",        AggregationType.Agg.SUM);   aggColumns.put(tmp.getColumnName(), tmp);
		tmp = new AggregationType("sumLogicalReadsAbs",     AggregationType.Agg.SUM);   aggColumns.put(tmp.getColumnName(), tmp);
		tmp = new AggregationType("avgLogicalReads",        AggregationType.Agg.SUM);   aggColumns.put(tmp.getColumnName(), tmp);
		tmp = new AggregationType("maxLogicalReads",        AggregationType.Agg.MAX);   aggColumns.put(tmp.getColumnName(), tmp);

		tmp = new AggregationType("sumPhysicalReads",       AggregationType.Agg.SUM);   aggColumns.put(tmp.getColumnName(), tmp);
		tmp = new AggregationType("sumPhysicalReadsAbs",    AggregationType.Agg.SUM);   aggColumns.put(tmp.getColumnName(), tmp);
		tmp = new AggregationType("avgPhysicalReads",       AggregationType.Agg.SUM);   aggColumns.put(tmp.getColumnName(), tmp);
		tmp = new AggregationType("maxPhysicalReads",       AggregationType.Agg.MAX);   aggColumns.put(tmp.getColumnName(), tmp);

		tmp = new AggregationType("sumCpuTime",             AggregationType.Agg.SUM);   aggColumns.put(tmp.getColumnName(), tmp);
		tmp = new AggregationType("sumCpuTimeAbs",          AggregationType.Agg.SUM);   aggColumns.put(tmp.getColumnName(), tmp);
		tmp = new AggregationType("avgCpuTime",             AggregationType.Agg.SUM);   aggColumns.put(tmp.getColumnName(), tmp);
		tmp = new AggregationType("maxCpuTime",             AggregationType.Agg.MAX);   aggColumns.put(tmp.getColumnName(), tmp);

		tmp = new AggregationType("sumWaitTime",            AggregationType.Agg.SUM);   aggColumns.put(tmp.getColumnName(), tmp);
		tmp = new AggregationType("sumWaitTimeAbs",         AggregationType.Agg.SUM);   aggColumns.put(tmp.getColumnName(), tmp);
		tmp = new AggregationType("avgWaitTime",            AggregationType.Agg.SUM);   aggColumns.put(tmp.getColumnName(), tmp);
		tmp = new AggregationType("maxWaitTime",            AggregationType.Agg.MAX);   aggColumns.put(tmp.getColumnName(), tmp);

		tmp = new AggregationType("sumRowsAffected",        AggregationType.Agg.SUM);   aggColumns.put(tmp.getColumnName(), tmp);
		tmp = new AggregationType("sumRowsAffectedAbs",     AggregationType.Agg.SUM);   aggColumns.put(tmp.getColumnName(), tmp);
		tmp = new AggregationType("avgRowsAffected",        AggregationType.Agg.SUM);   aggColumns.put(tmp.getColumnName(), tmp);
		tmp = new AggregationType("maxRowsAffected",        AggregationType.Agg.MAX);   aggColumns.put(tmp.getColumnName(), tmp);

		tmp = new AggregationType("cntQueryOptimizationTimeGtZero",     AggregationType.Agg.SUM);   aggColumns.put(tmp.getColumnName(), tmp);
		tmp = new AggregationType("cntQueryOptimizationTimeGtZeroAbs",  AggregationType.Agg.SUM);   aggColumns.put(tmp.getColumnName(), tmp);
		tmp = new AggregationType("sumQueryOptimizationTime",           AggregationType.Agg.SUM);   aggColumns.put(tmp.getColumnName(), tmp);
		tmp = new AggregationType("sumQueryOptimizationTimeAbs",        AggregationType.Agg.SUM);   aggColumns.put(tmp.getColumnName(), tmp);
		tmp = new AggregationType("avgQueryOptimizationTime",           AggregationType.Agg.SUM);   aggColumns.put(tmp.getColumnName(), tmp);
		tmp = new AggregationType("maxQueryOptimizationTime",           AggregationType.Agg.MAX);   aggColumns.put(tmp.getColumnName(), tmp);

//------------------------------------------
//TODO; // Since the name is changed "xxxQueryOptimizationTimeXxx" -->> "???compileCount|Time" we need to look at the graphs (change column names) 
		// Count
		tmp = new AggregationType("compileCount",                                            AggregationType.Agg.SUM);   aggColumns.put(tmp.getColumnName(), tmp);
		tmp = new AggregationType("compileCountAbs",                                         AggregationType.Agg.SUM);   aggColumns.put(tmp.getColumnName(), tmp);
		tmp = new AggregationType("compileCount_withTime_gt10_lt100",                        AggregationType.Agg.SUM);   aggColumns.put(tmp.getColumnName(), tmp);
		tmp = new AggregationType("compileCount_withTime_gt100_lt1000",                      AggregationType.Agg.SUM);   aggColumns.put(tmp.getColumnName(), tmp);
		tmp = new AggregationType("compileCount_withTime_gt1000",                            AggregationType.Agg.SUM);   aggColumns.put(tmp.getColumnName(), tmp);
		// Time                                                                              
		tmp = new AggregationType("sumCompileTime",                                          AggregationType.Agg.SUM);   aggColumns.put(tmp.getColumnName(), tmp);
		tmp = new AggregationType("sumCompileTimeAbs",                                       AggregationType.Agg.SUM);   aggColumns.put(tmp.getColumnName(), tmp);
		tmp = new AggregationType("avgCompileTime",                                          AggregationType.Agg.SUM);   aggColumns.put(tmp.getColumnName(), tmp);
		tmp = new AggregationType("maxCompileTime",                                          AggregationType.Agg.MAX);   aggColumns.put(tmp.getColumnName(), tmp);

		// PURE LANGUAGE -- Count 
		tmp = new AggregationType("compileCount_pureLang",                                   AggregationType.Agg.SUM);   aggColumns.put(tmp.getColumnName(), tmp);
		tmp = new AggregationType("compileCount_pureLangAbs",                                AggregationType.Agg.SUM);   aggColumns.put(tmp.getColumnName(), tmp);
		tmp = new AggregationType("compileCount_pureLang_withTime_gt10_lt100",               AggregationType.Agg.SUM);   aggColumns.put(tmp.getColumnName(), tmp);
		tmp = new AggregationType("compileCount_pureLang_withTime_gt100_lt1000",             AggregationType.Agg.SUM);   aggColumns.put(tmp.getColumnName(), tmp);
		tmp = new AggregationType("compileCount_pureLang_withTime_gt1000",                   AggregationType.Agg.SUM);   aggColumns.put(tmp.getColumnName(), tmp);
		// PURE LANGUAGE -- Time                                                                              
		tmp = new AggregationType("sumCompileTime_pureLang",                                 AggregationType.Agg.SUM);   aggColumns.put(tmp.getColumnName(), tmp);
		tmp = new AggregationType("sumCompileTime_pureLangAbs",                              AggregationType.Agg.SUM);   aggColumns.put(tmp.getColumnName(), tmp);
		tmp = new AggregationType("avgCompileTime_pureLang",                                 AggregationType.Agg.SUM);   aggColumns.put(tmp.getColumnName(), tmp);
		tmp = new AggregationType("maxCompileTime_pureLang",                                 AggregationType.Agg.MAX);   aggColumns.put(tmp.getColumnName(), tmp);

		// Language in Statement Cache -- Count
		tmp = new AggregationType("compileCount_langInStmntCache",                           AggregationType.Agg.SUM);   aggColumns.put(tmp.getColumnName(), tmp);
		tmp = new AggregationType("compileCount_langInStmntCacheAbs",                        AggregationType.Agg.SUM);   aggColumns.put(tmp.getColumnName(), tmp);
		tmp = new AggregationType("compileCount_langInStmntCache_withTime_gt10_lt100",       AggregationType.Agg.SUM);   aggColumns.put(tmp.getColumnName(), tmp);
		tmp = new AggregationType("compileCount_langInStmntCache_withTime_gt100_lt1000",     AggregationType.Agg.SUM);   aggColumns.put(tmp.getColumnName(), tmp);
		tmp = new AggregationType("compileCount_langInStmntCache_withTime_gt1000",           AggregationType.Agg.SUM);   aggColumns.put(tmp.getColumnName(), tmp);
		// Language in Statement Cache -- Time
		tmp = new AggregationType("sumCompileTime_langInStmntCache",                         AggregationType.Agg.SUM);   aggColumns.put(tmp.getColumnName(), tmp);
		tmp = new AggregationType("sumCompileTime_langInStmntCacheAbs",                      AggregationType.Agg.SUM);   aggColumns.put(tmp.getColumnName(), tmp);
		tmp = new AggregationType("avgCompileTime_langInStmntCache",                         AggregationType.Agg.SUM);   aggColumns.put(tmp.getColumnName(), tmp);
		tmp = new AggregationType("maxCompileTime_langInStmntCache",                         AggregationType.Agg.MAX);   aggColumns.put(tmp.getColumnName(), tmp);

		// Dynamic Request in Statement Cache -- Count
		tmp = new AggregationType("compileCount_dynamicInStmntCache",                        AggregationType.Agg.SUM);   aggColumns.put(tmp.getColumnName(), tmp);
		tmp = new AggregationType("compileCount_dynamicInStmntCacheAbs",                     AggregationType.Agg.SUM);   aggColumns.put(tmp.getColumnName(), tmp);
		tmp = new AggregationType("compileCount_dynamicInStmntCache_withTime_gt10_lt100",    AggregationType.Agg.SUM);   aggColumns.put(tmp.getColumnName(), tmp);
		tmp = new AggregationType("compileCount_dynamicInStmntCache_withTime_gt100_lt1000",  AggregationType.Agg.SUM);   aggColumns.put(tmp.getColumnName(), tmp);
		tmp = new AggregationType("compileCount_dynamicInStmntCache_withTime_gt1000",        AggregationType.Agg.SUM);   aggColumns.put(tmp.getColumnName(), tmp);
		// Dynamic Request in Statement Cache -- Time
		tmp = new AggregationType("sumCompileTime_dynamicInStmntCache",                      AggregationType.Agg.SUM);   aggColumns.put(tmp.getColumnName(), tmp);
		tmp = new AggregationType("sumCompileTime_dynamicInStmntCacheAbs",                   AggregationType.Agg.SUM);   aggColumns.put(tmp.getColumnName(), tmp);
		tmp = new AggregationType("avgCompileTime_dynamicInStmntCache",                      AggregationType.Agg.SUM);   aggColumns.put(tmp.getColumnName(), tmp);
		tmp = new AggregationType("maxCompileTime_dynamicInStmntCache",                      AggregationType.Agg.MAX);   aggColumns.put(tmp.getColumnName(), tmp);
		
//------------------------------------------
		
//		tmp = new AggregationType("errorMsgCountMap",       Types.VARCHAR, 1024, 0);

//		tmp = new AggregationType("dbid",                 Types.INTEGER,  0, 0);

		return aggColumns;
	}

	@Override
	public Object calculateAggregateRow_nonAggregatedColumnDataProvider(CounterSample newSample, String colName, int c, int jdbcType, Object addValue)
	{
		if ("dbid".equalsIgnoreCase(colName))
			return Integer.valueOf( 0 );

//		if ("dbid".equalsIgnoreCase(colName))
//			return Integer.valueOf( Short.MAX_VALUE );  // If we wanted sorted "at the end"
		
		return null;
	}

	/**
	 * Do DIFF calculation on the JSON value at column 'errorMsgCountMap', which is a HashMap of &lt;MsgNumber, Counter&gt;
	 */
	@Override
	public void localCalculation(CounterSample prevSample, CounterSample newSample, CounterSample diffData)
	{
		int errorMsgCountMap_pos = newSample.findColumn("errorMsgCountMap");
		if (errorMsgCountMap_pos == -1)
			return;
		
		@SuppressWarnings("serial")
		java.lang.reflect.Type mapType = new TypeToken<Map<Integer, Map<String,Long>>>(){}.getType();
		Gson gson = new Gson();

		for (int r=0; r<newSample.getRowCount(); r++)
		{
			String pk = newSample.getPkValue(r);
			
			String prevSampleErrorMsgCountMapStr = prevSample.getValueAsString(pk, "errorMsgCountMap");
			String newSampleErrorMsgCountMapStr  = newSample .getValueAsString(pk, "errorMsgCountMap");
			
			if (StringUtil.isNullOrBlank(prevSampleErrorMsgCountMapStr) || StringUtil.isNullOrBlank(newSampleErrorMsgCountMapStr))
				continue;

			Map<Integer, Map<String,Long>> prevSampleErrorMsgCountMap = gson.fromJson(prevSampleErrorMsgCountMapStr, mapType);
			Map<Integer, Map<String,Long>> newSampleErrorMsgCountMap  = gson.fromJson(newSampleErrorMsgCountMapStr , mapType);

			if (prevSampleErrorMsgCountMap == null || newSampleErrorMsgCountMap == null)
				continue;

			Map<Integer, Map<String,Long>> diffErrorMsgCountMap = new HashMap<>();
			
			for (Entry<Integer, Map<String,Long>> ne : newSampleErrorMsgCountMap.entrySet())
			{
				Integer           errorNum  = ne.getKey();
				Map<String, Long> dbCntrMap = ne.getValue();
				
				for (Entry<String, Long> ndbe : dbCntrMap.entrySet())
				{
					String  dbname   = ndbe.getKey();
					Long    newCount = ndbe.getValue();
					
					Long prevCount = prevSampleErrorMsgCountMap.get(errorNum) == null ? 0L : prevSampleErrorMsgCountMap.get(errorNum).get(dbname);

					if (newCount  == null) newCount  = 0L;
					if (prevCount == null) prevCount = 0L;

					Long newDiffCount = newCount - prevCount;
//System.out.println("   >>> "+getName()+".localCalculation(prevSample,newSample,diffData): pk='"+pk+"', errorNum="+errorNum+", dbname='"+dbname+"', newDiffCount="+newDiffCount+", newCount="+newCount+", prevCount="+prevCount);
					if (newDiffCount > 0)
					{
						Map<String, Long> newDbCntrMap = new HashMap<>();
						newDbCntrMap.put(dbname, newDiffCount);
						
						diffErrorMsgCountMap.put(errorNum, newDbCntrMap);
					}
				}
			}

			String json = null;
			if ( ! diffErrorMsgCountMap.isEmpty() )
				json = gson.toJson(diffErrorMsgCountMap);

			// Set Value
			int diffRowId = diffData.getRowNumberForPkValue(pk);
			diffData.setValueAt(json, diffRowId, errorMsgCountMap_pos);
//System.out.println("     + "+getName()+".localCalculation(prevSample,newSample,diffData): pk='"+pk+"', diffRowId="+diffRowId+", errorMsgCountMap_pos="+errorMsgCountMap_pos+", json="+json);
		}
	}
	
	/**
	 * Do RATE on the JSON value at column 'errorMsgCountMap', which is a HashMap of &lt;MsgNumber, Counter&gt;
	 */
	@Override
	public void localCalculationRatePerSec(CounterSample rateData, CounterSample diffData)
	{
		int errorMsgCountMap_pos = rateData.findColumn("errorMsgCountMap");
		if (errorMsgCountMap_pos == -1)
			return;

		@SuppressWarnings("serial")
		java.lang.reflect.Type mapType = new TypeToken<Map<Integer, Map<String,Long>>>(){}.getType();
		Gson gson = new Gson();

		for (int r=0; r<rateData.getRowCount(); r++)
		{
			String jsonSrc = rateData.getValueAsString(r, errorMsgCountMap_pos);
			if (StringUtil.isNullOrBlank(jsonSrc))
				continue;

			Map<Integer, Map<String,Long>> errorMsgCountMap = gson.fromJson(jsonSrc, mapType);
			if (errorMsgCountMap == null)
				continue;

			Map<Integer, Map<String,Double>> newRateMap = new HashMap<>();

			for (Entry<Integer, Map<String,Long>> e : errorMsgCountMap.entrySet())
			{
				Integer           errorNum  = e.getKey();
				Map<String, Long> dbCntrMap = e.getValue();
				
				for (Entry<String, Long> dbe : dbCntrMap.entrySet())
				{
					String  dbname    = dbe.getKey();
					Long    diffCount = dbe.getValue();

					Double newRateVal = round3( diffCount * 1000.0 / rateData.getSampleInterval() );

//System.out.println("   >>> "+getName()+".localCalculationRatePerSec(rateData,diffData):   pk='"+rateData.getPkValue(r)+"', errorNum="+errorNum+", dbname='"+dbname+"', newRateVal="+newRateVal+", diffCount="+diffCount+", rateSampleInterval="+rateData.getSampleInterval());

					if (newRateVal > 0.0)
					{
						Map<String, Double> newDbRateMap = new HashMap<>();
						newDbRateMap.put(dbname, newRateVal);
	
						newRateMap.put(errorNum, newDbRateMap);
					}
				}
			}

			String jsonDest = null;
			if ( ! newRateMap.isEmpty() )
				jsonDest = gson.toJson(newRateMap);
			
			// Set Value
			rateData.setValueAt(jsonDest, r, errorMsgCountMap_pos);
//System.out.println("     + "+getName()+".localCalculationRatePerSec(rateData,diffData):   r="+r+", errorMsgCountMap_pos="+errorMsgCountMap_pos+", jsonDest="+jsonDest);
		}
	}

	@Override
	public String getToolTipTextOnTableCell(MouseEvent e, String colName, Object cellValue, int modelRow, int modelCol)
	{
		// Get tip on errorMsgCountMap
		if ("errorMsgCountMap".equals(colName) && cellValue != null)
		{
			@SuppressWarnings("serial")
//			java.lang.reflect.Type mapIntStringType = new TypeToken<Map<Integer, String>>(){}.getType();
			java.lang.reflect.Type mapType = new TypeToken<Map<Integer, Map<String,String>>>(){}.getType();
			Gson gson = new Gson();

			Object objVal = getValueAt(modelRow, modelCol);
			if (objVal instanceof String)
			{
				String jsonSrc = (String) objVal;
				if (StringUtil.hasValue(jsonSrc))
				{
					String htmlTxt = "";
					Map<Integer, Map<String,String>> errorMsgCountMap = gson.fromJson(jsonSrc, mapType);
					if (errorMsgCountMap == null)
						return null;

					htmlTxt += "<html> \n";
					htmlTxt += "<p>The below table is the parsed JSON value in the cell<br> \n";
					htmlTxt += "   The Error Number is also <i>enriched</i> with a static description found in <code>master.dbo.sysmessages</code> \n";
					htmlTxt += "</p> \n";
					htmlTxt += "<br> \n";
					htmlTxt += "<table border=1> \n";
					htmlTxt += "<tr> <th>Error Number</th> <th>DBName</th> <th>Error Count</th> <th>Description</th> </tr> \n";
					for (Entry<Integer, Map<String,String>> entry : errorMsgCountMap.entrySet())
					{
						Integer             errorNum  = entry.getKey();
						Map<String, String> dbCntrMap = entry.getValue();
						
						for (Entry<String, String> dbe : dbCntrMap.entrySet())
						{
							String  dbname  = dbe.getKey();
							String  counter = dbe.getValue();

							htmlTxt += ""
								+ "<tr> "
								+    "<td>" + errorNum + "</td> "
								+    "<td>" + dbname   + "</td> "
								+    "<td>" + counter  + "</td> "
								+    "<td>" + AseErrorMessageDictionary.getInstance().getDescription(errorNum) + "</td> "
								+ "</tr> \n";
						}
					}
					htmlTxt += "</table> \n";
					htmlTxt += "</html> \n";
					
					return htmlTxt;
				}
			}
		}

		return super.getToolTipTextOnTableCell(e, colName, cellValue, modelRow, modelCol);
	}

//	//--------------------------------------------------------------------
//	// Alarm Handling
//	//--------------------------------------------------------------------
//	@Override
//	public void sendAlarmRequest()
//	{
//		if ( ! hasDiffData() )
//			return;
//
//		if ( ! AlarmHandler.hasInstance() )
//			return;
//
//		CountersModel cm = this;
//
//		boolean debugPrint = Configuration.getCombinedConfiguration().getBooleanProperty("sendAlarmRequest.debug", _logger.isDebugEnabled());
//
//		//-------------------------------------------------------
//		// errorCount
//		//-------------------------------------------------------
//		if (isSystemAlarmsForColumnEnabledAndInTimeRange("errorCount"))
//		{
//			// SUM all rows for 'errorCount' column
//			Double errorCountPerSec = cm.getRateValueSum("errorCount");
//
//			if (errorCountPerSec != null)
//			{
//				int threshold = Configuration.getCombinedConfiguration().getIntProperty(PROPKEY_alarm_ErrorCountPerSec, DEFAULT_alarm_ErrorCountPerSec);
//
//				if (debugPrint || _logger.isDebugEnabled())
//					System.out.println("##### sendAlarmRequest("+cm.getName()+"): threshold="+threshold+", errorCountPerSec='"+errorCountPerSec+"'.");
//
//				if (errorCountPerSec.intValue() > threshold)
//				{
//					// BEGIN: construct a summary Map (for 'error info' to Alarm) of all RATE errors, and set it to a JSON String...
////					Map<Integer, Double> sumRateMap = new HashMap<>();
//					Map<Integer, Map<String,Double>> sumDbRateMap = new HashMap<>();
//
//					CounterSample rateData = cm.getCounterSampleRate();
//					int errorMsgCountMap_pos = rateData.findColumn("errorMsgCountMap");
//
//					@SuppressWarnings("serial")
//					java.lang.reflect.Type mapType = new TypeToken<Map<Integer, Map<String,Double>>>(){}.getType();
//					Gson gson = new Gson();
//
//					if (errorMsgCountMap_pos != -1)
//					{
//						for (int r=0; r<rateData.getRowCount(); r++)
//						{
//							String jsonSrc = rateData.getValueAsString(r, errorMsgCountMap_pos);
//							if (StringUtil.isNullOrBlank(jsonSrc))
//								continue;
//
//							Map<Integer, Map<String,Double>> errorMsgCountMap = gson.fromJson(jsonSrc, mapType);
//							if (errorMsgCountMap == null)
//								continue;
//
//							for (Entry<Integer, Map<String, Double>> e : errorMsgCountMap.entrySet())
//							{
//								Integer errorNum = e.getKey();
//								Map<String, Double> dbMap = e.getValue();
//								
//								for (Entry<String, Double> dbe : dbMap.entrySet())
//								{
//									String dbname  = dbe.getKey();
//									Double rateVal = dbe.getValue();
//
//									// Sum on ErrorMumber per DBName
//									Map<String, Double> sumDbMap = sumDbRateMap.get(errorNum);
//									if (sumDbMap == null)
//									{
//										sumDbMap = new HashMap<>();
//										sumDbMap.put(dbname, 0d);
//									}
//									
//									Double sumDbRateValue = sumDbMap.get(dbname);
//									if (sumDbRateValue == null)
//										sumDbRateValue = 0d;
//
//									sumDbRateValue += rateVal;
//									dbMap.put(dbname, sumDbRateValue);
//									
//									sumDbRateMap.put(errorNum, dbMap);
//									
//								}
//							}
//						}
//					}
//
//					String errorMsgInfoJson = "";
//					String errorMsgInfoTxt  = "";
//					String errorMsgInfoHtml = "";
//
//					// JSON
//					if ( ! sumDbRateMap.isEmpty() )
//						errorMsgInfoJson = gson.toJson(sumDbRateMap);
//					
//					// TXT
//					for (Entry<Integer, Map<String, Double>> e : sumDbRateMap.entrySet())
//					{
//						Integer errorNum = e.getKey();
//						Map<String, Double> dbMap = e.getValue();
//
//						for (Entry<String, Double> dbe : dbMap.entrySet())
//						{
//							String dbname  = dbe.getKey();
//							Double rateVal = dbe.getValue();
//							
//							errorMsgInfoTxt += "Msg=" + errorNum + ", DBName='" + dbname + "', ErrorsPerSec=" + rateVal + ", Description='" + AseErrorMessageDictionary.getInstance().getDescription(errorNum) + "\n";
//						}
//					}
//
//					// HTML
//					errorMsgInfoHtml += "<table border=1>\n";
//					errorMsgInfoHtml += "<tr> <th>Msg</th> <th>DBName</th> <th>ErrorsPerSec</th> <th>Description</th> </tr>\n";
//					for (Entry<Integer, Map<String, Double>> e : sumDbRateMap.entrySet())
//					{
//						Integer errorNum = e.getKey();
//						Map<String, Double> dbMap = e.getValue();
//
//						for (Entry<String, Double> dbe : dbMap.entrySet())
//						{
//							String dbname  = dbe.getKey();
//							Double rateVal = dbe.getValue();
//							
//							errorMsgInfoHtml += "<tr> <td>" + errorNum + "</td> <td>" + dbname + "</td> <td>" + rateVal + "</td> <td>" + AseErrorMessageDictionary.getInstance().getDescription(errorNum) + "</td> </tr>\n";
//						}
//					}
//					errorMsgInfoHtml += "</table>\n";
//					
//					// END: construct a summary Map (for 'error info' to Alarm) of all RATE errors, and set it to a JSON String...
//
////					System.out.println("--------------------------------------------------------------------------------------");
////					System.out.println("XXXXXXXXXXXXXXXXXX: errorMsgInfoJson="+errorMsgInfoJson);
////					System.out.println("XXXXXXXXXXXXXXXXXX: errorMsgInfoTxt ="+errorMsgInfoTxt);
////					System.out.println("XXXXXXXXXXXXXXXXXX: errorMsgInfoHtml="+errorMsgInfoHtml);
//
//					errorMsgInfoHtml += "<br><br>" + cm.getGraphDataHistoryAsHtmlImage(GRAPH_NAME_SQL_STATEMENT_ERROR_COUNT);
//					errorMsgInfoHtml += "<br><br>" + cm.getGraphDataHistoryAsHtmlImage(GRAPH_NAME_SQL_STATEMENT_SEC);
//					
//					// Create Alarm
//					AlarmEvent alarm = new AlarmEventClientErrorMsgRate(cm, round1(errorCountPerSec), errorMsgInfoJson, threshold);
//					
//					// Set the Error Info
//					alarm.setExtendedDescription(errorMsgInfoTxt, errorMsgInfoHtml);
//					
//					// Add the Alarm
//					AlarmHandler.getInstance().addAlarm(alarm);
//				}
//			}
//		} //end: errorCount
//
//		//-------------------------------------------------------
//		// ErrorNumber
//		//-------------------------------------------------------
//		if (isSystemAlarmsForColumnEnabledAndInTimeRange("ErrorNumbers"))
//		{
//			// BEGIN: construct a summary Map of all DIFF errors, and set it to a JSON String... errorMsgInfo
////			Map<Integer, Long> sumDiffMap = new HashMap<>();
//			Map<Integer, Map<String,Long>> sumDbDiffMap = new HashMap<>();
//
//			CounterSample diffData = cm.getCounterSampleDiff();
//			int errorMsgCountMap_pos = diffData.findColumn("errorMsgCountMap");
//
//			if (errorMsgCountMap_pos != -1)
//			{
//				@SuppressWarnings("serial")
//				java.lang.reflect.Type mapType = new TypeToken<Map<Integer, Map<String,Long>>>(){}.getType();
//				Gson gson = new Gson();
//
//				for (int r=0; r<diffData.getRowCount(); r++)
//				{
//					String jsonSrc = diffData.getValueAsString(r, errorMsgCountMap_pos);
//					if (StringUtil.isNullOrBlank(jsonSrc))
//						continue;
//
//					Map<Integer, Map<String,Long>> errorMsgCountMap = gson.fromJson(jsonSrc, mapType);
//					if (errorMsgCountMap == null)
//						continue;
//
//					for (Entry<Integer, Map<String,Long>> e : errorMsgCountMap.entrySet())
//					{
//						Integer           errorNum  = e.getKey();
//						Map<String, Long> dbCntrMap = e.getValue();
//						
//						for (Entry<String, Long> dbe : dbCntrMap.entrySet())
//						{
//							String  dbname    = dbe.getKey();
//							Long    diffCount = dbe.getValue();
//
////							// Sum on ErrorMumber
////							Long sumDiffValue = sumDiffMap.get(errorNum);
////							if (sumDiffValue == null)
////								sumDiffValue = 0L;
////							
////							sumDiffValue += diffCount;
////
////							sumDiffMap.put(errorNum, sumDiffValue);
//							
//
//							// Sum on ErrorMumber per DBName
//							Map<String, Long> dbMap = sumDbDiffMap.get(errorNum);
//							if (dbMap == null)
//							{
//								dbMap = new HashMap<>();
//								dbMap.put(dbname, 0L);
//							}
//							
//							Long sumDbDiffValue = dbMap.get(dbname);
//							if (sumDbDiffValue == null)
//								sumDbDiffValue = 0L;
//
//							sumDbDiffValue += diffCount;
//							dbMap.put(dbname, sumDbDiffValue);
//							
//							sumDbDiffMap.put(errorNum, dbMap);
//						}
//					}
//				}
//			}
//			
////			Map<Integer, Integer> _alarmErrorMap = new HashMap<>();
////			_alarmErrorMap.put(1105, 0);
////			_alarmErrorMap.put(1205, 0);
//			if ( ! sumDbDiffMap.isEmpty() )
//			{
//				// loop ErrorNumbers and check if we got any matching entries
//				for (Entry<Integer, Integer> e : _map_alarm_ErrorNumbers.entrySet())
//				{
//					Integer alarmErrorNum  = e.getKey();
//					Integer alarmThreshold = e.getValue();
//
//					if (sumDbDiffMap.containsKey(alarmErrorNum))
//					{
//						Map<String, Long> dbMap = sumDbDiffMap.get(alarmErrorNum);
//						if (dbMap != null)
//						{
//							for (Entry<String, Long> dbe : dbMap.entrySet())
//							{
//								String dbname     = dbe.getKey();
//								Long   errorCount = dbe.getValue();
//								
//								if (errorCount == null)
//									errorCount = 0L;
//
//								if (errorCount > alarmThreshold)
//								{
//									String errorDesc = AseErrorMessageDictionary.getInstance().getDescription(alarmErrorNum);
//									
//									if (debugPrint || _logger.isDebugEnabled())
//										System.out.println("##### sendAlarmRequest("+cm.getName()+"): ErrorNumber="+alarmErrorNum+", Count="+errorCount+", is above threshold="+alarmThreshold+".)");
//
//									// if 1105 (out-of-space) -- Get TranLog Charts 'LogSize Left in MB' from CmOpenDatabases
//									// NOTE: Make the chart for ALL Databases, since it might be 'tempdb' that the 1105 happens in (even if the current working database is a User Database)
//									String extendedDescHtml = "";
//									if (alarmErrorNum == 1105)
//									{
//										CountersModel cmOpenDatabases = getCounterController().getCmByName(CmOpenDatabases.CM_NAME);
//										extendedDescHtml = "<br><br>" + cmOpenDatabases.getGraphDataHistoryAsHtmlImage(CmOpenDatabases.GRAPH_NAME_LOGSIZE_LEFT_MB);
//										extendedDescHtml = "<br><br>" + cmOpenDatabases.getGraphDataHistoryAsHtmlImage(CmOpenDatabases.GRAPH_NAME_LOGSIZE_USED_PCT);
//									}
//
//									// Create Alarm
//									AlarmEvent alarm = new AlarmEventClientErrorMsg(cm, alarmErrorNum, dbname, errorCount, errorDesc, alarmThreshold);
//									
//									// Set the Error Info
//									String errorMsgInfoTxt  = "Msg=" + alarmErrorNum + ", DBName='" + dbname + "', DiffErrorCount=" + errorCount + ", Description='" + errorDesc + "'";
//									String errorMsgInfoHtml = errorMsgInfoTxt + extendedDescHtml;
//
//									alarm.setExtendedDescription(errorMsgInfoTxt, errorMsgInfoHtml);
//									
//									// Add the Alarm
//									AlarmHandler.getInstance().addAlarm(alarm);
//								}
//							} // end: dbMap loop
//						} // end: dbMap != null
//					} // end: sumDbDiffMap has alarmErrorNum
//				} // end: _map_alarm_ErrorNumbers
//			}
//		} //end: ErrorNumber
//
//
//		// Check if the "statistics producer - SQL Capture Thread" is delivering data statistics (is still alive)
//		if (isSystemAlarmsForColumnEnabledAndInTimeRange("SqlCaptureAge"))
//		{
//			if (_sqlCaptureLastUpdateTime > 0)
//			{
//				int ageInSec = (int) (System.currentTimeMillis() - _sqlCaptureLastUpdateTime) / 1000;
//				int thresholdInSec = Configuration.getCombinedConfiguration().getIntProperty(PROPKEY_alarm_SqlCapUpdateAgeSec, DEFAULT_alarm_SqlCapUpdateAgeSec);
//				
//				if (debugPrint || _logger.isDebugEnabled())
//					System.out.println("##### sendAlarmRequest("+cm.getName()+"): SqlCaptureAge - thresholdInSec="+thresholdInSec+", ageInSec="+ageInSec+".");
//
//				// possibly get Connection connect time also ... and check if we "just" connected... to alarm after a connection "down" time...
//				if (getCounterController().getMonConnection() != null)
//				{
//					int lastConnectTimeInSec = (int) getCounterController().getMonConnection().getConnectTime() / 1000;
//					
//					if (debugPrint || _logger.isDebugEnabled())
//						System.out.println("##### sendAlarmRequest("+cm.getName()+"): SqlCaptureAge - thresholdInSec="+thresholdInSec+", ageInSec="+ageInSec+", lastConnectTimeInSec="+lastConnectTimeInSec+".");
//
//					if (lastConnectTimeInSec > 0 && lastConnectTimeInSec < ageInSec)
//					{
//						ageInSec = lastConnectTimeInSec;
//					}
//				}
//
//
//				if (ageInSec > thresholdInSec)
//				{
//					AlarmEvent alarm = new AlarmEventSqlCaptureOldData(cm, ageInSec, thresholdInSec);
//					AlarmHandler.getInstance().addAlarm(alarm);
//				}
//			}
//		} // end: check - SQL Capture Thread
//	}
//
//	@Override
//	public boolean isGraphDataHistoryEnabled(String name)
//	{
//		// ENABLED for the following graphs
//		if (GRAPH_NAME_SQL_STATEMENT_ERROR_COUNT.equals(name)) return true;
//
//		// default: DISABLED
//		return false;
//	}
//	@Override
//	public int getGraphDataHistoryTimeInterval(String name)
//	{
//		// Keep interval: default is 60 minutes
//		return super.getGraphDataHistoryTimeInterval(name);
//	}
//
//	
//	private Map<Integer, Integer> _map_alarm_ErrorNumbers;  // Note: do NOT initialize this here... since the initAlarms() is done in super, if initialized it will be overwritten here...
//
//	/**
//	 * Initialize stuff that has to do with alarms
//	 */
//	@Override
//	public void initAlarms()
//	{
//		Configuration conf = Configuration.getCombinedConfiguration();
//		String cfgVal;
//
//		_map_alarm_ErrorNumbers = new LinkedHashMap<>();
//		
//		String prefix = "       ";
//		
//		//--------------------------------------
//		// LowDbFreeSpaceInMb
//		cfgVal = conf.getProperty(PROPKEY_alarm_ErrorNumbers, DEFAULT_alarm_ErrorNumbers);
//		if (StringUtil.hasValue(cfgVal))
//		{
//			Map<String, String> map = StringUtil.parseCommaStrToMap(cfgVal);
//			if (_logger.isDebugEnabled())
//				_logger.debug(prefix + "Initializing alarm 'ErrorNumbers'. After parseCommaStrToMap, map looks like: "+map);
//			
//			for (String key : map.keySet())
//			{
//				String val = map.get(key);
//				
//				try
//				{
//					int error = NumberUtils.createNumber(key).intValue();
//					int count = NumberUtils.createNumber(val).intValue();
//					_map_alarm_ErrorNumbers.put(error, count);
//
//					_logger.info(prefix + "Initializing alarm. Using 'ErrorNumbers', ErrorMsg='"+key+"', thresholdCount="+count+", ErrorDescription='"+AseErrorMessageDictionary.getInstance().getDescription(error)+"'.");
//				}
//				catch (NumberFormatException ex)
//				{
//					_logger.info(prefix + "Initializing alarm. Skipping 'ErrorNumbers' enty ErrorMsg='"+key+"', val='"+val+"'. The value is not a number.");
//				}
//			}
//		}
//	}
//
//	// Updated by: CounterSamplePrivate.getSample(); used to detect if the "SQL Capture Thread" is still alive and delivers statistics
//	private long _sqlCaptureLastUpdateTime = -1;
//
//	public static final String  PROPKEY_alarm_ErrorCountPerSec   = CM_NAME + ".alarm.system.if.errorCount.gt";
//	public static final int     DEFAULT_alarm_ErrorCountPerSec   = 20;
//	
//	public static final String  PROPKEY_alarm_ErrorNumbers       = CM_NAME + ".alarm.system.if.errorNumber";
//	public static final String  DEFAULT_alarm_ErrorNumbers       = "701=0, 713=0, 971=5, 1105=0, 1204=0, 1205=5";
//
//	public static final String  PROPKEY_alarm_SqlCapUpdateAgeSec = CM_NAME + ".alarm.system.if.SqlCapture.lastUpdate.ageInSec.gt";
//	public static final int     DEFAULT_alarm_SqlCapUpdateAgeSec = 600; // 10 minutes
//
////	Error=701   , Severity=17 , Message="There is not enough procedure cache to run this procedure, trigger, or SQL batch. Retry later, or ask your SA to reconfigure ASE with more procedure cache.");
////	Error=713   , Severity=16 , Message="Sort failed because there is insufficient procedure cache for the configured number of sort buffers. Please retry the query after configuring lesser number of sort buffers.");
////	Error=971   , Severity=14 , Message="Database '%.*s' is currently unavailable. It is undergoing recovery of a critical database operation due to failure of a cluster instance. Wait and retry later.");
////	Error=1105  , Severity=17 , Message="Can't allocate space for object '%.*s' in database '%.*s' because '%.*s' segment is full/has no free extents. If you ran out of space in syslogs, dump the transaction log. Otherwise, use ALTER DATABASE to increase the size of the segment.");
////	Error=1204  , Severity=17 , Message="ASE has run out of LOCKS. Re-run your command when there are fewer active users, or contact a user with System Administrator (SA) role to reconfigure ASE with more LOCKS.");
////	Error=1205  , Severity=13 , Message="Your server command (family id #%d, process id #%d) encountered a deadlock situation. Please re-run your command.");
//	
//	@Override
//	public List<CmSettingsHelper> getLocalAlarmSettings()
//	{
//		Configuration conf = Configuration.getCombinedConfiguration();
//		List<CmSettingsHelper> list = new ArrayList<>();
//
//		CmSettingsHelper.Type isAlarmSwitch = CmSettingsHelper.Type.IS_ALARM_SWITCH;
//
//		list.add(new CmSettingsHelper("errorCount"   , isAlarmSwitch, PROPKEY_alarm_ErrorCountPerSec,   Integer.class, conf.getIntProperty(PROPKEY_alarm_ErrorCountPerSec  , DEFAULT_alarm_ErrorCountPerSec  ), DEFAULT_alarm_ErrorCountPerSec  , "If 'errorCount' is greater than ## per second then send 'AlarmEventErrorMsgRate'." ));
//		list.add(new CmSettingsHelper("ErrorNumbers" , isAlarmSwitch, PROPKEY_alarm_ErrorNumbers,       String .class, conf.getProperty   (PROPKEY_alarm_ErrorNumbers      , DEFAULT_alarm_ErrorNumbers      ), DEFAULT_alarm_ErrorNumbers      , "If 'DiffErrorCount' for 'ErrorMsg' is greater than ## then send 'AlarmEventClientErrorMsg'.format: 1105=#, 1205=#, 1234=# (check sysmessages for descriptions)", new MapNumberValidator()));
//		list.add(new CmSettingsHelper("SqlCaptureAge", isAlarmSwitch, PROPKEY_alarm_SqlCapUpdateAgeSec, Integer.class, conf.getIntProperty(PROPKEY_alarm_SqlCapUpdateAgeSec, DEFAULT_alarm_SqlCapUpdateAgeSec), DEFAULT_alarm_SqlCapUpdateAgeSec, "If 'SQL Capture Thread' hasn't been updated it's statistics in ## seconds then send 'AlarmEventSqlCaptureOldData'."));
//
//		return list;
//	}
}
