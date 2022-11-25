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
package com.asetune.cm.ase;

import java.awt.event.MouseEvent;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import javax.naming.NameNotFoundException;

import org.apache.commons.lang3.math.NumberUtils;
import org.apache.log4j.Logger;

import com.asetune.ICounterController;
import com.asetune.IGuiController;
import com.asetune.alarm.AlarmHandler;
import com.asetune.alarm.events.AlarmEvent;
import com.asetune.alarm.events.AlarmEventClientErrorMsg;
import com.asetune.alarm.events.AlarmEventClientErrorMsgRate;
import com.asetune.alarm.events.AlarmEventSqlCaptureOldData;
import com.asetune.cm.CmSettingsHelper;
import com.asetune.cm.CmSettingsHelper.MapNumberValidator;
import com.asetune.cm.CounterSample;
import com.asetune.cm.CounterSetTemplates;
import com.asetune.cm.CounterSetTemplates.Type;
import com.asetune.cm.CountersModel;
import com.asetune.cm.NoValidRowsInSample;
import com.asetune.config.dict.AseErrorMessageDictionary;
import com.asetune.config.dict.MonTablesDictionary;
import com.asetune.config.dict.MonTablesDictionaryManager;
import com.asetune.graph.TrendGraphDataPoint;
import com.asetune.graph.TrendGraphDataPoint.LabelType;
import com.asetune.gui.MainFrame;
import com.asetune.gui.TrendGraph;
import com.asetune.pcs.PersistentCounterHandler;
import com.asetune.pcs.sqlcapture.ISqlCaptureBroker;
import com.asetune.pcs.sqlcapture.SqlCaptureBrokerAse;
import com.asetune.pcs.sqlcapture.SqlCaptureStatementStatisticsSample;
import com.asetune.sql.conn.DbxConnection;
import com.asetune.sql.conn.info.DbmsVersionInfo;
import com.asetune.utils.Configuration;
import com.asetune.utils.StringUtil;
import com.google.common.reflect.TypeToken;
import com.google.gson.Gson;

/**
 * @author Goran Schwarz (goran_schwarz@hotmail.com)
 */
public class CmSqlStatement
extends CountersModel
{
	private static Logger        _logger          = Logger.getLogger(CmSqlStatement.class);
	private static final long    serialVersionUID = 1L;

	public static final String   CM_NAME          = CmSqlStatement.class.getSimpleName();
//	public static final String   SHORT_NAME       = "SQL Batch";
	public static final String   SHORT_NAME       = "SQL Statements";
	public static final String   HTML_DESC        = 
		"<html>" +
		"SQL Statement/Batch Activity<br>" +
		"In what <i>time span</i> does most SQL Statements/Batches execute within, and how much resources do they consume.<br>" +
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

	public static final String[] MON_TABLES       = new String[] {"CmSqlStatement"};
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

		return new CmSqlStatement(counterController, guiController);
	}

	public CmSqlStatement(ICounterController counterController, IGuiController guiController)
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
	public static final String GRAPH_NAME_SQL_STATEMENT_SEC               = "SqlStmnt";
	public static final String GRAPH_NAME_SQL_STATEMENT_TIME_SPAN_ALL     = "SqlStmntTSpanAll";
	public static final String GRAPH_NAME_SQL_STATEMENT_TIME_SPAN_NOOP    = "SqlStmntTSpanNoOp";
	public static final String GRAPH_NAME_SQL_STATEMENT_TIME_SPAN_0       = "SqlStmntTSpan0";
	public static final String GRAPH_NAME_SQL_STATEMENT_TIME_SPAN_1       = "SqlStmntTSpan1";
	public static final String GRAPH_NAME_SQL_STATEMENT_TIME_SPAN_2       = "SqlStmntTSpan2";
	public static final String GRAPH_NAME_SQL_STATEMENT_TIME_SPAN_3       = "SqlStmntTSpan3";
	public static final String GRAPH_NAME_SQL_STATEMENT_TIME_SPAN_4       = "SqlStmntTSpan4";

//TODO: implement the below graphs (the most important one is probably: LOGICAL_READ, so we can see what response time "span" does the most logical IO... so we can have a start "where" too look)
	public static final String GRAPH_NAME_SQL_STATEMENT_SUM_EXEC_TIME     = "SqlStmntSumExecMs";
	public static final String GRAPH_NAME_SQL_STATEMENT_SUM_LOGICAL_READ  = "SqlStmntSumLRead";
	public static final String GRAPH_NAME_SQL_STATEMENT_SUM_PHYSICAL_READ = "SqlStmntSumPRead";
	public static final String GRAPH_NAME_SQL_STATEMENT_SUM_CPU_TIME      = "SqlStmntSumCpuTime";
	public static final String GRAPH_NAME_SQL_STATEMENT_SUM_WAIT_TIME     = "SqlStmntSumWaitTime";
	public static final String GRAPH_NAME_SQL_STATEMENT_SUM_ROWS_AFFECTED = "SqlStmntSumRowsAfct";

	public static final String GRAPH_NAME_SQL_STATEMENT_ERROR_COUNT       = "SqlStmntSumErrorCnt";
	
//	public static final String GRAPH_NAME_XXX = "xxxGraph"; //String x=GetCounters.XXX;
//
//	private void addTrendGraphs()
//	{
//		addTrendGraph(GRAPH_NAME_XXX,
//			"MenuTextXXX", 	                                 // Menu CheckBox text
//			"GrapgLabelXXX", // Label 
//			new String[] { "XXX", "YYY", "ZZZ" }, 
//			LabelType.Static,
//			TrendGraphDataPoint.Category.OTHER,
//			false, // is Percent Graph
//			false, // visible at start
//			0,     // graph is valid from Server Version. 0 = All Versions; >0 = Valid from this version and above 
//			-1);
//	}
	private void addTrendGraphs()
	{
		//-----
		addTrendGraph(GRAPH_NAME_SQL_STATEMENT_SEC,
			"SQL Statements Executed per Sec", // Menu CheckBox text
			"SQL Statements Executed per Sec ("+GROUP_NAME+"->"+SHORT_NAME+")", // Label 
			TrendGraphDataPoint.Y_AXIS_SCALE_LABELS_PERSEC,
			new String[] { "Total Statements Executed/sec", "SQL Batches Executed/sec", "SQL in Statement Cache Executed/sec", "Dynamic SQL/sec", "Statements in Procedures Executed/sec" }, 
			LabelType.Static,
			TrendGraphDataPoint.Category.OPERATIONS,
			false, // is Percent Graph
			true,  // visible at start
			0,     // graph is valid from Server Version. 0 = All Versions; >0 = Valid from this version and above 
			-1);   // minimum height

		//-----
		addTrendGraph(GRAPH_NAME_SQL_STATEMENT_TIME_SPAN_ALL,
			"SQL Statements (all) In Time Span Received per Sec", // Menu CheckBox text
			"SQL Statements (all) In Time Span Received per Sec ("+GROUP_NAME+"->"+SHORT_NAME+")", // Label 
			TrendGraphDataPoint.Y_AXIS_SCALE_LABELS_PERSEC,
			new String[] { "0ms & NoLReads", "<1ms", "1-2ms", "2-5ms", "5-10ms", "10-20ms", "20-50ms", "50-100ms", "100-200ms", "200-500ms", "500ms-1s", "1-2s", "2-5s", "5-10s", "10-20s", "20-50s", "50-100s", ">100s" }, 
			LabelType.Static,
			TrendGraphDataPoint.Category.OPERATIONS,
			false, // is Percent Graph
			false, // visible at start
			0,     // graph is valid from Server Version. 0 = All Versions; >0 = Valid from this version and above 
			-1);   // minimum height

		//-----
		addTrendGraph(GRAPH_NAME_SQL_STATEMENT_TIME_SPAN_NOOP,
			"SQL Statements (0 ms and 0 LogicalReads) In Time Span Received per Sec", // Menu CheckBox text
			"SQL Statements (0 ms and 0 LogicalReads) In Time Span Received per Sec ("+GROUP_NAME+"->"+SHORT_NAME+")", // Label 
			TrendGraphDataPoint.Y_AXIS_SCALE_LABELS_PERSEC,
			new String[] { "0 ms ExecTime AND 0 LogicalReads" }, 
			LabelType.Static,
			TrendGraphDataPoint.Category.OPERATIONS,
			false, // is Percent Graph
			false, // visible at start
			0,     // graph is valid from Server Version. 0 = All Versions; >0 = Valid from this version and above 
			-1);   // minimum height

		//-----
		addTrendGraph(GRAPH_NAME_SQL_STATEMENT_TIME_SPAN_0,
			"SQL Statements (0-10ms) In Time Span Received per Sec", // Menu CheckBox text
			"SQL Statements (0-10ms) In Time Span Received per Sec ("+GROUP_NAME+"->"+SHORT_NAME+")", // Label 
			TrendGraphDataPoint.Y_AXIS_SCALE_LABELS_PERSEC,
			new String[] { "<1ms", "1-2ms", "2-5ms", "5-10ms" }, 
			LabelType.Static,
			TrendGraphDataPoint.Category.OPERATIONS,
			false, // is Percent Graph
			false, // visible at start
			0,     // graph is valid from Server Version. 0 = All Versions; >0 = Valid from this version and above 
			-1);   // minimum height

		//-----
		addTrendGraph(GRAPH_NAME_SQL_STATEMENT_TIME_SPAN_1,
			"SQL Statements (10ms-100ms) In Time Span Received per Sec", // Menu CheckBox text
			"SQL Statements (10ms-100ms) In Time Span Received per Sec ("+GROUP_NAME+"->"+SHORT_NAME+")", // Label 
			TrendGraphDataPoint.Y_AXIS_SCALE_LABELS_PERSEC,
			new String[] { "10-20ms", "20-50ms", "50-100ms" }, 
			LabelType.Static,
			TrendGraphDataPoint.Category.OPERATIONS,
			false, // is Percent Graph
			false, // visible at start
			0,     // graph is valid from Server Version. 0 = All Versions; >0 = Valid from this version and above 
			-1);   // minimum height

		//-----
		addTrendGraph(GRAPH_NAME_SQL_STATEMENT_TIME_SPAN_2,
			"SQL Statements (100ms-1s) In Time Span Received per Sec", // Menu CheckBox text
			"SQL Statements (100ms-1s) In Time Span Received per Sec ("+GROUP_NAME+"->"+SHORT_NAME+")", // Label 
			TrendGraphDataPoint.Y_AXIS_SCALE_LABELS_PERSEC,
			new String[] { "100-200ms", "200-500ms", "500ms-1s" }, 
			LabelType.Static,
			TrendGraphDataPoint.Category.OPERATIONS,
			false, // is Percent Graph
			false, // visible at start
			0,     // graph is valid from Server Version. 0 = All Versions; >0 = Valid from this version and above 
			-1);   // minimum height

		//-----
		addTrendGraph(GRAPH_NAME_SQL_STATEMENT_TIME_SPAN_3,
			"SQL Statements (1s-10s) In Time Span Received per Sec", // Menu CheckBox text
			"SQL Statements (1s-10s) In Time Span Received per Sec ("+GROUP_NAME+"->"+SHORT_NAME+")", // Label 
			TrendGraphDataPoint.Y_AXIS_SCALE_LABELS_PERSEC,
			new String[] { "1-2s", "2-5s", "5-10s" }, 
			LabelType.Static,
			TrendGraphDataPoint.Category.OPERATIONS,
			false, // is Percent Graph
			false, // visible at start
			0,     // graph is valid from Server Version. 0 = All Versions; >0 = Valid from this version and above 
			-1);   // minimum height

		//-----
		addTrendGraph(GRAPH_NAME_SQL_STATEMENT_TIME_SPAN_4,
			"SQL Statements (>10s) In Time Span Received per SAMPLE", // Menu CheckBox text
			"SQL Statements (>10s) In Time Span Received per SAMPLE ("+GROUP_NAME+"->"+SHORT_NAME+")", // Label 
			TrendGraphDataPoint.Y_AXIS_SCALE_LABELS_PERSEC,
			new String[] { "10-20s", "20-50s", "50-100s", ">100s" }, 
			LabelType.Static,
			TrendGraphDataPoint.Category.OPERATIONS,
			false, // is Percent Graph
			false, // visible at start
			0,     // graph is valid from Server Version. 0 = All Versions; >0 = Valid from this version and above 
			-1);   // minimum height


		
		//--------------------------------------------------------------------------------------
		//----- Sum XXXXX Over SQL Response Time
		//--------------------------------------------------------------------------------------

		//-----
		addTrendGraph(GRAPH_NAME_SQL_STATEMENT_SUM_EXEC_TIME,
			"Sum Exec Time per sec Over SQL Response Time", // Menu CheckBox text
			"Sum Exec Time per sec Over SQL Response Time ("+GROUP_NAME+"->"+SHORT_NAME+")", // Label 
			TrendGraphDataPoint.Y_AXIS_SCALE_LABELS_PERSEC,
			new String[] { "0ms & NoLReads", "<1ms", "1-2ms", "2-5ms", "5-10ms", "10-20ms", "20-50ms", "50-100ms", "100-200ms", "200-500ms", "500ms-1s", "1-2s", "2-5s", "5-10s", "10-20s", "20-50s", "50-100s", ">100s" }, 
			LabelType.Static,
			TrendGraphDataPoint.Category.OPERATIONS,
			false, // is Percent Graph
			false, // visible at start
			0,     // graph is valid from Server Version. 0 = All Versions; >0 = Valid from this version and above 
			-1);   // minimum height

		//-----
		addTrendGraph(GRAPH_NAME_SQL_STATEMENT_SUM_LOGICAL_READ,
			"Sum Logical Reads per sec Over SQL Response Time", // Menu CheckBox text
			"Sum Logical Reads per sec Over SQL Response Time ("+GROUP_NAME+"->"+SHORT_NAME+")", // Label 
			TrendGraphDataPoint.Y_AXIS_SCALE_LABELS_PERSEC,
			new String[] { "0ms & NoLReads", "<1ms", "1-2ms", "2-5ms", "5-10ms", "10-20ms", "20-50ms", "50-100ms", "100-200ms", "200-500ms", "500ms-1s", "1-2s", "2-5s", "5-10s", "10-20s", "20-50s", "50-100s", ">100s" }, 
			LabelType.Static,
			TrendGraphDataPoint.Category.OPERATIONS,
			false, // is Percent Graph
			false, // visible at start
			0,     // graph is valid from Server Version. 0 = All Versions; >0 = Valid from this version and above 
			-1);   // minimum height

		//-----
		addTrendGraph(GRAPH_NAME_SQL_STATEMENT_SUM_PHYSICAL_READ,
			"Sum Physical Reads per sec Over SQL Response Time", // Menu CheckBox text
			"Sum Physical Reads per sec Over SQL Response Time ("+GROUP_NAME+"->"+SHORT_NAME+")", // Label 
			TrendGraphDataPoint.Y_AXIS_SCALE_LABELS_PERSEC,
			new String[] { "0ms & NoLReads", "<1ms", "1-2ms", "2-5ms", "5-10ms", "10-20ms", "20-50ms", "50-100ms", "100-200ms", "200-500ms", "500ms-1s", "1-2s", "2-5s", "5-10s", "10-20s", "20-50s", "50-100s", ">100s" }, 
			LabelType.Static,
			TrendGraphDataPoint.Category.OPERATIONS,
			false, // is Percent Graph
			false, // visible at start
			0,     // graph is valid from Server Version. 0 = All Versions; >0 = Valid from this version and above 
			-1);   // minimum height

		//-----
		addTrendGraph(GRAPH_NAME_SQL_STATEMENT_SUM_CPU_TIME,
			"Sum CPU Time per sec Over SQL Response Time", // Menu CheckBox text
			"Sum CPU Time per sec Over SQL Response Time ("+GROUP_NAME+"->"+SHORT_NAME+")", // Label 
			TrendGraphDataPoint.Y_AXIS_SCALE_LABELS_PERSEC,
			new String[] { "0ms & NoLReads", "<1ms", "1-2ms", "2-5ms", "5-10ms", "10-20ms", "20-50ms", "50-100ms", "100-200ms", "200-500ms", "500ms-1s", "1-2s", "2-5s", "5-10s", "10-20s", "20-50s", "50-100s", ">100s" }, 
			LabelType.Static,
			TrendGraphDataPoint.Category.OPERATIONS,
			false, // is Percent Graph
			false, // visible at start
			0,     // graph is valid from Server Version. 0 = All Versions; >0 = Valid from this version and above 
			-1);   // minimum height

		//-----
		addTrendGraph(GRAPH_NAME_SQL_STATEMENT_SUM_WAIT_TIME,
			"Sum Wait Time per sec Over SQL Response Time", // Menu CheckBox text
			"Sum Wait Time per sec Over SQL Response Time ("+GROUP_NAME+"->"+SHORT_NAME+")", // Label 
			TrendGraphDataPoint.Y_AXIS_SCALE_LABELS_PERSEC,
			new String[] { "0ms & NoLReads", "<1ms", "1-2ms", "2-5ms", "5-10ms", "10-20ms", "20-50ms", "50-100ms", "100-200ms", "200-500ms", "500ms-1s", "1-2s", "2-5s", "5-10s", "10-20s", "20-50s", "50-100s", ">100s" }, 
			LabelType.Static,
			TrendGraphDataPoint.Category.OPERATIONS,
			false, // is Percent Graph
			false, // visible at start
			0,     // graph is valid from Server Version. 0 = All Versions; >0 = Valid from this version and above 
			-1);   // minimum height

		//-----
		addTrendGraph(GRAPH_NAME_SQL_STATEMENT_SUM_ROWS_AFFECTED,
			"Sum Rows Affected per sec Over SQL Response Time", // Menu CheckBox text
			"Sum Rows Affected per sec Over SQL Response Time ("+GROUP_NAME+"->"+SHORT_NAME+")", // Label 
			TrendGraphDataPoint.Y_AXIS_SCALE_LABELS_PERSEC,
			new String[] { "0ms & NoLReads", "<1ms", "1-2ms", "2-5ms", "5-10ms", "10-20ms", "20-50ms", "50-100ms", "100-200ms", "200-500ms", "500ms-1s", "1-2s", "2-5s", "5-10s", "10-20s", "20-50s", "50-100s", ">100s" }, 
			LabelType.Static,
			TrendGraphDataPoint.Category.OPERATIONS,
			false, // is Percent Graph
			false, // visible at start
			0,     // graph is valid from Server Version. 0 = All Versions; >0 = Valid from this version and above 
			-1);   // minimum height

		//-----
		addTrendGraph(GRAPH_NAME_SQL_STATEMENT_ERROR_COUNT,
			"Sum SQL Statements Error Count Per Sec", // Menu CheckBox text
			"Sum SQL Statements Error Count Per Sec ("+GROUP_NAME+"->"+SHORT_NAME+")", // Label 
			TrendGraphDataPoint.Y_AXIS_SCALE_LABELS_PERSEC,
			new String[] { "errorCount" }, 
			LabelType.Static,
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
		if (GRAPH_NAME_SQL_STATEMENT_SEC.equals(tgdp.getName()))
		{
			// TODO: rewrite to use: this.getAbsValueAsDouble(pk, "calculated_value");
			Double[] arr = new Double[5];

			arr[0] = this.getRateValueSum("totalCount");
			arr[1] = this.getRateValueSum("sqlBatchCount");
			arr[2] = this.getRateValueSum("inStmntCacheCount");
			arr[3] = this.getRateValueSum("dynamicStmntCount");
			arr[4] = this.getRateValueSum("inProcedureCount");

			// Set the values
			tgdp.setDataPoint(this.getTimestamp(), arr);
		}

		// -----------------------------------------------------------------------------------------
		if (GRAPH_NAME_SQL_STATEMENT_TIME_SPAN_ALL.equals(tgdp.getName()))
		{
			allResponseTimesForColumnName(tgdp, "totalCount");

//			// TODO: rewrite to use: this.getAbsValueAsDouble(pk, "calculated_value");
//			Double[] arr = new Double[18];
//
//			// Note the prefix: 'SQLServer' or 'MSSQL$@@servicename' is removed in SQL query
//			String pk0  = createPkStr(SqlCaptureStatementStatisticsSample.EXEC_SPAN_0ms_0lr_0pr   );
//			String pk1  = createPkStr(SqlCaptureStatementStatisticsSample.EXEC_SPAN_0_to_1_ms     );
//			String pk2  = createPkStr(SqlCaptureStatementStatisticsSample.EXEC_SPAN_1_to_2_ms     );
//			String pk3  = createPkStr(SqlCaptureStatementStatisticsSample.EXEC_SPAN_2_to_5_ms     );
//			String pk4  = createPkStr(SqlCaptureStatementStatisticsSample.EXEC_SPAN_5_to_10_ms    );
//			String pk5  = createPkStr(SqlCaptureStatementStatisticsSample.EXEC_SPAN_10_to_20_ms   );
//			String pk6  = createPkStr(SqlCaptureStatementStatisticsSample.EXEC_SPAN_20_to_50_ms   );
//			String pk7  = createPkStr(SqlCaptureStatementStatisticsSample.EXEC_SPAN_50_to_100_ms  );
//			String pk8  = createPkStr(SqlCaptureStatementStatisticsSample.EXEC_SPAN_100_to_200_ms );
//			String pk9  = createPkStr(SqlCaptureStatementStatisticsSample.EXEC_SPAN_200_to_500_ms );
//			String pk10 = createPkStr(SqlCaptureStatementStatisticsSample.EXEC_SPAN_500_to_1000_ms);
//			String pk11 = createPkStr(SqlCaptureStatementStatisticsSample.EXEC_SPAN_1_to_2_sec    );
//			String pk12 = createPkStr(SqlCaptureStatementStatisticsSample.EXEC_SPAN_2_to_5_sec    );
//			String pk13 = createPkStr(SqlCaptureStatementStatisticsSample.EXEC_SPAN_5_to_10_sec   );
//			String pk14 = createPkStr(SqlCaptureStatementStatisticsSample.EXEC_SPAN_10_to_20_sec  );
//			String pk15 = createPkStr(SqlCaptureStatementStatisticsSample.EXEC_SPAN_20_to_50_sec  );
//			String pk16 = createPkStr(SqlCaptureStatementStatisticsSample.EXEC_SPAN_50_to_100_sec );
//			String pk17 = createPkStr(SqlCaptureStatementStatisticsSample.EXEC_SPAN_ABOVE_100_sec );
//
//			Double val0  = this.getRateValueAsDouble(pk0 , "totalCount");
//			Double val1  = this.getRateValueAsDouble(pk1 , "totalCount");
//			Double val2  = this.getRateValueAsDouble(pk2 , "totalCount");
//			Double val3  = this.getRateValueAsDouble(pk3 , "totalCount");
//			Double val4  = this.getRateValueAsDouble(pk4 , "totalCount");
//			Double val5  = this.getRateValueAsDouble(pk5 , "totalCount");
//			Double val6  = this.getRateValueAsDouble(pk6 , "totalCount");
//			Double val7  = this.getRateValueAsDouble(pk7 , "totalCount");
//			Double val8  = this.getRateValueAsDouble(pk8 , "totalCount");
//			Double val9  = this.getRateValueAsDouble(pk9 , "totalCount");
//			Double val10 = this.getRateValueAsDouble(pk10, "totalCount");
//			Double val11 = this.getRateValueAsDouble(pk11, "totalCount");
//			Double val12 = this.getRateValueAsDouble(pk12, "totalCount");
//			Double val13 = this.getRateValueAsDouble(pk13, "totalCount");
//			Double val14 = this.getRateValueAsDouble(pk14, "totalCount");
//			Double val15 = this.getRateValueAsDouble(pk15, "totalCount");
//			Double val16 = this.getRateValueAsDouble(pk16, "totalCount");
//			Double val17 = this.getRateValueAsDouble(pk17, "totalCount");
//			
//			if (val0 != null && val1 != null && val2 != null && val3 != null && val4 != null && val5 != null && val6 != null && val7 != null && val8 != null && val9 != null && val10 != null && val11 != null && val12 != null && val13 != null && val14 != null && val15 != null && val16 != null && val17 != null)
//			{
//				// Calculate
//				arr[0 ] = val0 ;
//				arr[1 ] = val1 ;
//				arr[2 ] = val2 ;
//				arr[3 ] = val3 ;
//				arr[4 ] = val4 ;
//				arr[5 ] = val5 ;
//				arr[6 ] = val6 ;
//				arr[7 ] = val7 ;
//				arr[8 ] = val8 ;
//				arr[9 ] = val9 ;
//				arr[10] = val10;
//				arr[11] = val11;
//				arr[12] = val12;
//				arr[13] = val13;
//				arr[14] = val14;
//				arr[15] = val15;
//				arr[16] = val16;
//				arr[17] = val17;
//
//				// Set the values
//				tgdp.setDataPoint(this.getTimestamp(), arr);
//			}
//			else
//			{
//				TrendGraph tg = getTrendGraph(tgdp.getName());
//				if (tg != null)
//					tg.setWarningLabel("Failed to get value for 'some' pk-row: '"+pk1+"'='"+val1+"'.");
//			}
		}

		// -----------------------------------------------------------------------------------------
		if (GRAPH_NAME_SQL_STATEMENT_TIME_SPAN_NOOP.equals(tgdp.getName()))
		{
			Double[] arr = new Double[1];

			String pk1  = createPkStr(SqlCaptureStatementStatisticsSample.EXEC_SPAN_0_to_1_ms );

			Double val1  = this.getRateValueAsDouble(pk1 , "totalCount");
			
			if (val1 != null)
			{
				// Calculate
				arr[0 ] = val1 ;

				// Set the values
				tgdp.setDataPoint(this.getTimestamp(), arr);
			}
			else
			{
				TrendGraph tg = getTrendGraph(tgdp.getName());
				if (tg != null)
					tg.setWarningLabel("Failed to get value for 'some' pk-row: '"+pk1+"'='"+val1+"'.");
			}
		}

		// -----------------------------------------------------------------------------------------
		if (GRAPH_NAME_SQL_STATEMENT_TIME_SPAN_0.equals(tgdp.getName()))
		{
			Double[] arr = new Double[4];

			String pk1  = createPkStr(SqlCaptureStatementStatisticsSample.EXEC_SPAN_0_to_1_ms     );
			String pk2  = createPkStr(SqlCaptureStatementStatisticsSample.EXEC_SPAN_1_to_2_ms     );
			String pk3  = createPkStr(SqlCaptureStatementStatisticsSample.EXEC_SPAN_2_to_5_ms     );
			String pk4  = createPkStr(SqlCaptureStatementStatisticsSample.EXEC_SPAN_5_to_10_ms    );

			Double val1  = this.getRateValueAsDouble(pk1 , "totalCount");
			Double val2  = this.getRateValueAsDouble(pk2 , "totalCount");
			Double val3  = this.getRateValueAsDouble(pk3 , "totalCount");
			Double val4  = this.getRateValueAsDouble(pk4 , "totalCount");
			
			if (val1 != null && val2 != null && val3 != null && val4 != null)
			{
				// Calculate
				arr[0 ] = val1 ;
				arr[1 ] = val2 ;
				arr[2 ] = val3 ;
				arr[3 ] = val4 ;

				// Set the values
				tgdp.setDataPoint(this.getTimestamp(), arr);
			}
			else
			{
				TrendGraph tg = getTrendGraph(tgdp.getName());
				if (tg != null)
					tg.setWarningLabel("Failed to get value for 'some' pk-row: '"+pk1+"'='"+val1+"'.");
			}
		}

		// -----------------------------------------------------------------------------------------
		if (GRAPH_NAME_SQL_STATEMENT_TIME_SPAN_1.equals(tgdp.getName()))
		{
			Double[] arr = new Double[3];

			String pk5  = createPkStr(SqlCaptureStatementStatisticsSample.EXEC_SPAN_10_to_20_ms);
			String pk6  = createPkStr(SqlCaptureStatementStatisticsSample.EXEC_SPAN_20_to_50_ms);
			String pk7  = createPkStr(SqlCaptureStatementStatisticsSample.EXEC_SPAN_50_to_100_ms);

			Double val5  = this.getRateValueAsDouble(pk5 , "totalCount");
			Double val6  = this.getRateValueAsDouble(pk6 , "totalCount");
			Double val7  = this.getRateValueAsDouble(pk7 , "totalCount");
			
			if (val5 != null && val6 != null && val7 != null)
			{
				// Calculate
				arr[0 ] = val5 ;
				arr[1 ] = val6 ;
				arr[2 ] = val7 ;

				// Set the values
				tgdp.setDataPoint(this.getTimestamp(), arr);
			}
			else
			{
				TrendGraph tg = getTrendGraph(tgdp.getName());
				if (tg != null)
					tg.setWarningLabel("Failed to get value for 'some' pk-row: '"+pk5+"'='"+val5+"'.");
			}
		}

		// -----------------------------------------------------------------------------------------
		if (GRAPH_NAME_SQL_STATEMENT_TIME_SPAN_2.equals(tgdp.getName()))
		{
			Double[] arr = new Double[3];

			String pk8  = createPkStr(SqlCaptureStatementStatisticsSample.EXEC_SPAN_100_to_200_ms);
			String pk9  = createPkStr(SqlCaptureStatementStatisticsSample.EXEC_SPAN_200_to_500_ms);
			String pk10 = createPkStr(SqlCaptureStatementStatisticsSample.EXEC_SPAN_500_to_1000_ms);

			Double val8  = this.getRateValueAsDouble(pk8 , "totalCount");
			Double val9  = this.getRateValueAsDouble(pk9 , "totalCount");
			Double val10 = this.getRateValueAsDouble(pk10, "totalCount");
			
			if (val8 != null && val9 != null && val10 != null)
			{
				// Calculate
				arr[0 ] = val8 ;
				arr[1 ] = val9 ;
				arr[2 ] = val10;

				// Set the values
				tgdp.setDataPoint(this.getTimestamp(), arr);
			}
			else
			{
				TrendGraph tg = getTrendGraph(tgdp.getName());
				if (tg != null)
					tg.setWarningLabel("Failed to get value for 'some' pk-row: '"+pk8+"'='"+val8+"'.");
			}
		}

		// -----------------------------------------------------------------------------------------
		if (GRAPH_NAME_SQL_STATEMENT_TIME_SPAN_3.equals(tgdp.getName()))
		{
			Double[] arr = new Double[3];

			String pk11 = createPkStr(SqlCaptureStatementStatisticsSample.EXEC_SPAN_1_to_2_sec);
			String pk12 = createPkStr(SqlCaptureStatementStatisticsSample.EXEC_SPAN_2_to_5_sec);
			String pk13 = createPkStr(SqlCaptureStatementStatisticsSample.EXEC_SPAN_5_to_10_sec);

			Double val11 = this.getRateValueAsDouble(pk11, "totalCount");
			Double val12 = this.getRateValueAsDouble(pk12, "totalCount");
			Double val13 = this.getRateValueAsDouble(pk13, "totalCount");
			
			if (val11 != null && val12 != null && val13 != null)
			{
				// Calculate
				arr[0 ] = val11;
				arr[1 ] = val12;
				arr[2 ] = val13;

				// Set the values
				tgdp.setDataPoint(this.getTimestamp(), arr);
			}
			else
			{
				TrendGraph tg = getTrendGraph(tgdp.getName());
				if (tg != null)
					tg.setWarningLabel("Failed to get value for 'some' pk-row: '"+pk11+"'='"+val11+"'.");
			}
		}

		// -----------------------------------------------------------------------------------------
		if (GRAPH_NAME_SQL_STATEMENT_TIME_SPAN_4.equals(tgdp.getName()))
		{
			// TODO: rewrite to use: this.getAbsValueAsDouble(pk, "calculated_value");
			Double[] arr = new Double[4];

			String pk14 = createPkStr(SqlCaptureStatementStatisticsSample.EXEC_SPAN_10_to_20_sec);
			String pk15 = createPkStr(SqlCaptureStatementStatisticsSample.EXEC_SPAN_20_to_50_sec);
			String pk16 = createPkStr(SqlCaptureStatementStatisticsSample.EXEC_SPAN_50_to_100_sec);
			String pk17 = createPkStr(SqlCaptureStatementStatisticsSample.EXEC_SPAN_ABOVE_100_sec);

			Double val14 = this.getDiffValueAsDouble(pk14, "totalCount");
			Double val15 = this.getDiffValueAsDouble(pk15, "totalCount");
			Double val16 = this.getDiffValueAsDouble(pk16, "totalCount");
			Double val17 = this.getDiffValueAsDouble(pk17, "totalCount");
			
			if (val14 != null && val15 != null && val16 != null && val17 != null)
			{
				// Calculate
				arr[0] = val14;
				arr[1] = val15;
				arr[2] = val16;
				arr[3] = val17;

				// Set the values
				tgdp.setDataPoint(this.getTimestamp(), arr);
			}
			else
			{
				TrendGraph tg = getTrendGraph(tgdp.getName());
				if (tg != null)
					tg.setWarningLabel("Failed to get value for 'some' pk-row: '"+pk14+"'='"+val14+"'.");
			}
		}
//		public static final String GRAPH_NAME_SQL_STATEMENT_SUM_EXEC_TIME     = "SqlStmntSumExecMs";
//		public static final String GRAPH_NAME_SQL_STATEMENT_SUM_LOGICAL_READ  = "SqlStmntSumLRead";
//		public static final String GRAPH_NAME_SQL_STATEMENT_SUM_PHYSICAL_READ = "SqlStmntSumPRead";
//		public static final String GRAPH_NAME_SQL_STATEMENT_SUM_CPU_TIME      = "SqlStmntSumCpuTime";
//		public static final String GRAPH_NAME_SQL_STATEMENT_SUM_WAIT_TIME     = "SqlStmntSumWaitTime";
//		public static final String GRAPH_NAME_SQL_STATEMENT_SUM_ROWS_AFFECTED = "SqlStmntSumRowsAfct";

		// -----------------------------------------------------------------------------------------
		if (GRAPH_NAME_SQL_STATEMENT_SUM_EXEC_TIME.equals(tgdp.getName()))
		{
			allResponseTimesForColumnName(tgdp, "sumExecTimeMs");
		}

		// -----------------------------------------------------------------------------------------
		if (GRAPH_NAME_SQL_STATEMENT_SUM_LOGICAL_READ.equals(tgdp.getName()))
		{
			allResponseTimesForColumnName(tgdp, "sumLogicalReads");
		}

		// -----------------------------------------------------------------------------------------
		if (GRAPH_NAME_SQL_STATEMENT_SUM_PHYSICAL_READ.equals(tgdp.getName()))
		{
			allResponseTimesForColumnName(tgdp, "sumPhysicalReads");
		}

		// -----------------------------------------------------------------------------------------
		if (GRAPH_NAME_SQL_STATEMENT_SUM_CPU_TIME.equals(tgdp.getName()))
		{
			allResponseTimesForColumnName(tgdp, "sumCpuTime");
		}

		// -----------------------------------------------------------------------------------------
		if (GRAPH_NAME_SQL_STATEMENT_SUM_WAIT_TIME.equals(tgdp.getName()))
		{
			allResponseTimesForColumnName(tgdp, "sumWaitTime");
		}

		// -----------------------------------------------------------------------------------------
		if (GRAPH_NAME_SQL_STATEMENT_SUM_ROWS_AFFECTED.equals(tgdp.getName()))
		{
			allResponseTimesForColumnName(tgdp, "sumRowsAffected");
		}
		
		// -----------------------------------------------------------------------------------------
		if (GRAPH_NAME_SQL_STATEMENT_ERROR_COUNT.equals(tgdp.getName()))
		{
			Double[] arr = new Double[1];

			arr[0] = this.getRateValueSum("errorCount");

			// Set the values
			tgdp.setDataPoint(this.getTimestamp(), arr);
		}
	}
	
	private void allResponseTimesForColumnName(TrendGraphDataPoint tgdp, String colName)
	{
		Double[] arr = new Double[18];

		String pk0  = createPkStr(SqlCaptureStatementStatisticsSample.EXEC_SPAN_0ms_0lr_0pr   );
		String pk1  = createPkStr(SqlCaptureStatementStatisticsSample.EXEC_SPAN_0_to_1_ms     );
		String pk2  = createPkStr(SqlCaptureStatementStatisticsSample.EXEC_SPAN_1_to_2_ms     );
		String pk3  = createPkStr(SqlCaptureStatementStatisticsSample.EXEC_SPAN_2_to_5_ms     );
		String pk4  = createPkStr(SqlCaptureStatementStatisticsSample.EXEC_SPAN_5_to_10_ms    );
		String pk5  = createPkStr(SqlCaptureStatementStatisticsSample.EXEC_SPAN_10_to_20_ms   );
		String pk6  = createPkStr(SqlCaptureStatementStatisticsSample.EXEC_SPAN_20_to_50_ms   );
		String pk7  = createPkStr(SqlCaptureStatementStatisticsSample.EXEC_SPAN_50_to_100_ms  );
		String pk8  = createPkStr(SqlCaptureStatementStatisticsSample.EXEC_SPAN_100_to_200_ms );
		String pk9  = createPkStr(SqlCaptureStatementStatisticsSample.EXEC_SPAN_200_to_500_ms );
		String pk10 = createPkStr(SqlCaptureStatementStatisticsSample.EXEC_SPAN_500_to_1000_ms);
		String pk11 = createPkStr(SqlCaptureStatementStatisticsSample.EXEC_SPAN_1_to_2_sec    );
		String pk12 = createPkStr(SqlCaptureStatementStatisticsSample.EXEC_SPAN_2_to_5_sec    );
		String pk13 = createPkStr(SqlCaptureStatementStatisticsSample.EXEC_SPAN_5_to_10_sec   );
		String pk14 = createPkStr(SqlCaptureStatementStatisticsSample.EXEC_SPAN_10_to_20_sec  );
		String pk15 = createPkStr(SqlCaptureStatementStatisticsSample.EXEC_SPAN_20_to_50_sec  );
		String pk16 = createPkStr(SqlCaptureStatementStatisticsSample.EXEC_SPAN_50_to_100_sec );
		String pk17 = createPkStr(SqlCaptureStatementStatisticsSample.EXEC_SPAN_ABOVE_100_sec );

		Double val0  = this.getRateValueAsDouble(pk0 , colName);
		Double val1  = this.getRateValueAsDouble(pk1 , colName);
		Double val2  = this.getRateValueAsDouble(pk2 , colName);
		Double val3  = this.getRateValueAsDouble(pk3 , colName);
		Double val4  = this.getRateValueAsDouble(pk4 , colName);
		Double val5  = this.getRateValueAsDouble(pk5 , colName);
		Double val6  = this.getRateValueAsDouble(pk6 , colName);
		Double val7  = this.getRateValueAsDouble(pk7 , colName);
		Double val8  = this.getRateValueAsDouble(pk8 , colName);
		Double val9  = this.getRateValueAsDouble(pk9 , colName);
		Double val10 = this.getRateValueAsDouble(pk10, colName);
		Double val11 = this.getRateValueAsDouble(pk11, colName);
		Double val12 = this.getRateValueAsDouble(pk12, colName);
		Double val13 = this.getRateValueAsDouble(pk13, colName);
		Double val14 = this.getRateValueAsDouble(pk14, colName);
		Double val15 = this.getRateValueAsDouble(pk15, colName);
		Double val16 = this.getRateValueAsDouble(pk16, colName);
		Double val17 = this.getRateValueAsDouble(pk17, colName);
		
		if (val0 != null && val1 != null && val2 != null && val3 != null && val4 != null && val5 != null && val6 != null && val7 != null && val8 != null && val9 != null && val10 != null && val11 != null && val12 != null && val13 != null && val14 != null && val15 != null && val16 != null && val17 != null)
		{
			// Calculate
			arr[0 ] = val0 ;
			arr[1 ] = val1 ;
			arr[2 ] = val2 ;
			arr[3 ] = val3 ;
			arr[4 ] = val4 ;
			arr[5 ] = val5 ;
			arr[6 ] = val6 ;
			arr[7 ] = val7 ;
			arr[8 ] = val8 ;
			arr[9 ] = val9 ;
			arr[10] = val10;
			arr[11] = val11;
			arr[12] = val12;
			arr[13] = val13;
			arr[14] = val14;
			arr[15] = val15;
			arr[16] = val16;
			arr[17] = val17;

			// Set the values
			tgdp.setDataPoint(this.getTimestamp(), arr);
		}
		else
		{
			TrendGraph tg = getTrendGraph(tgdp.getName());
			if (tg != null)
				tg.setWarningLabel("Failed to get value for 'some' pk-row:"
					+ " '" + pk0  + "'='" + val0  + "'"
					+ ",'" + pk1  + "'='" + val1  + "'"
					+ ",'" + pk2  + "'='" + val2  + "'"
					+ ",'" + pk3  + "'='" + val3  + "'"
					+ ",'" + pk4  + "'='" + val4  + "'"
					+ ",'" + pk5  + "'='" + val5  + "'"
					+ ",'" + pk6  + "'='" + val6  + "'"
					+ ",'" + pk7  + "'='" + val7  + "'"
					+ ",'" + pk8  + "'='" + val8  + "'"
					+ ",'" + pk9  + "'='" + val9  + "'"
					+ ",'" + pk10 + "'='" + val10 + "'"
					+ ",'" + pk11 + "'='" + val11 + "'"
					+ ",'" + pk12 + "'='" + val12 + "'"
					+ ",'" + pk13 + "'='" + val13 + "'"
					+ ",'" + pk14 + "'='" + val14 + "'"
					+ ",'" + pk15 + "'='" + val15 + "'"
					+ ",'" + pk16 + "'='" + val16 + "'"
					+ ",'" + pk17 + "'='" + val17 + "'"
					+ ".");
		}
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
			String tabName = "CmSqlStatement";

			MonTablesDictionary mtd = MonTablesDictionaryManager.getInstance();
			mtd.addTable(tabName,  "xxx.");

			mtd.addColumn(tabName, "name",                 "<html>Execution time span for this statistics</html>");

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

//			mtd.addColumn(tabName, "errorMsgCountMap",      "<html>A JSON String, which contains: {\"MsgNumber\"=count, \"MsgNumber\"=count}.</html>");
			mtd.addColumn(tabName, "errorMsgCountMap",      "<html>A JSON String, which contains: {\"MsgNumber\"={\"dbname\"=count}, \"MsgNumber\"={\"dbname\"=count}}.</html>");
		}
		catch (NameNotFoundException e) {/*ignore*/}
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

		pkCols.add("name");

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
			((CmSqlStatement)cm)._sqlCaptureLastUpdateTime = sqlCapStat.getLastUpdateTime();
			
			// create a "bucket" where all the rows will end up in ( add will be done in method: readResultset() )
			_rows = new ArrayList<List<Object>>();

			ResultSet rs = sqlCapStat.toResultSet();

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
//		tmp = new AggregationType("name",                   Types.VARCHAR, 30, 0);
        
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
		        
//		tmp = new AggregationType("errorMsgCountMap",       Types.VARCHAR, 1024, 0);

//		tmp = new AggregationType("statId",                 Types.INTEGER,  0, 0);

		return aggColumns;
	}

	@Override
	public boolean isAggregateRowAppendEnabled()
	{
		return true;
	}

	@Override
	public Object calculateAggregateRow_nonAggregatedColumnDataProvider(CounterSample newSample, String colName, int c, int jdbcType, Object addValue)
	{
		if ("statId".equalsIgnoreCase(colName))
			return new Integer(SqlCaptureStatementStatisticsSample.SUMMARY_STAT_ID);
		
		return null;
	}

//	/**
//	 * Do DIFF calculation on the JSON value at column 'errorMsgCountMap', which is a HashMap of &lt;MsgNumber, Counter&gt;
//	 */
//	@Override
//	public void localCalculation(CounterSample prevSample, CounterSample newSample, CounterSample diffData)
//	{
//		int errorMsgCountMap_pos = newSample.findColumn("errorMsgCountMap");
//		if (errorMsgCountMap_pos == -1)
//			return;
//		
//		@SuppressWarnings("serial")
//		java.lang.reflect.Type mapIntLongType = new TypeToken<Map<Integer, Long>>(){}.getType();
//		Gson gson = new Gson();
//
//		for (int r=0; r<newSample.getRowCount(); r++)
//		{
//			String pk = newSample.getPkValue(r);
//			
//			String prevSampleErrorMsgCountMapStr = prevSample.getValueAsString(pk, "errorMsgCountMap");
//			String newSampleErrorMsgCountMapStr  = newSample .getValueAsString(pk, "errorMsgCountMap");
//			
//			if (StringUtil.isNullOrBlank(prevSampleErrorMsgCountMapStr) || StringUtil.isNullOrBlank(newSampleErrorMsgCountMapStr))
//				continue;
//
//			Map<Integer, Long> prevSampleErrorMsgCountMap = gson.fromJson(prevSampleErrorMsgCountMapStr, mapIntLongType);
//			Map<Integer, Long> newSampleErrorMsgCountMap  = gson.fromJson(newSampleErrorMsgCountMapStr , mapIntLongType);
//
//			Map<Integer, Long> diffErrorMsgCountMap = new HashMap<>();
//			
//			for (Entry<Integer, Long> ne : newSampleErrorMsgCountMap.entrySet())
//			{
//				Integer key    = ne.getKey();
//				Long newCount  = ne.getValue();
//				Long prevCount = prevSampleErrorMsgCountMap.get(key);
//				if (prevCount == null)
//					prevCount = 0L;
//				
//				Long newDiffCount = newCount - prevCount;
////System.out.println("   >>> "+getName()+".localCalculation(prevSample,newSample,diffData): pk='"+pk+"', key="+key+", newDiffCount="+newDiffCount+", newCount="+newCount+", prevCount="+prevCount);
//				if (newDiffCount > 0)
//					diffErrorMsgCountMap.put(key, newDiffCount);
//			}
//
//			String json = null;
//			if ( ! diffErrorMsgCountMap.isEmpty() )
//				json = gson.toJson(diffErrorMsgCountMap);
//
//			// Set Value
//			int diffRowId = diffData.getRowNumberForPkValue(pk);
//			diffData.setValueAt(json, diffRowId, errorMsgCountMap_pos);
////System.out.println("     + "+getName()+".localCalculation(prevSample,newSample,diffData): pk='"+pk+"', diffRowId="+diffRowId+", errorMsgCountMap_pos="+errorMsgCountMap_pos+", json="+json);
//		}
//	}
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
	
//	/**
//	 * Do RATE on the JSON value at column 'errorMsgCountMap', which is a HashMap of &lt;MsgNumber, Counter&gt;
//	 */
//	@Override
//	public void localCalculationRatePerSec(CounterSample rateData, CounterSample diffData)
//	{
//		int errorMsgCountMap_pos = rateData.findColumn("errorMsgCountMap");
//		if (errorMsgCountMap_pos == -1)
//			return;
//
//		@SuppressWarnings("serial")
//		java.lang.reflect.Type mapIntLongType = new TypeToken<Map<Integer, Long>>(){}.getType();
//		Gson gson = new Gson();
//
//		for (int r=0; r<rateData.getRowCount(); r++)
//		{
//			String jsonSrc = rateData.getValueAsString(r, errorMsgCountMap_pos);
//			if (StringUtil.isNullOrBlank(jsonSrc))
//				continue;
//
//			Map<Integer, Long> errorMsgCountMap = gson.fromJson(jsonSrc, mapIntLongType);
//
//			Map<Integer, Double> newRateMap = new HashMap<>();
//
//			for (Entry<Integer, Long> e : errorMsgCountMap.entrySet())
//			{
//				Double newRateVal = round3( e.getValue() * 1000.0 / rateData.getSampleInterval() );
////System.out.println("   >>> "+getName()+".localCalculationRatePerSec(rateData,diffData):   pk='"+rateData.getPkValue(r)+"', key="+e.getKey()+", newRateVal="+newRateVal+", diffVal="+e.getValue()+", rateSampleInterval="+rateData.getSampleInterval());
//
//				if (newRateVal > 0.0)
//					newRateMap.put(e.getKey(), newRateVal);
//			}
//
//			String jsonDest = null;
//			if ( ! newRateMap.isEmpty() )
//				jsonDest = gson.toJson(newRateMap);
//			
//			// Set Value
//			rateData.setValueAt(jsonDest, r, errorMsgCountMap_pos);
////System.out.println("     + "+getName()+".localCalculationRatePerSec(rateData,diffData):   r="+r+", errorMsgCountMap_pos="+errorMsgCountMap_pos+", jsonDest="+jsonDest);
//		}
//	}
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

//	@Override
//	public String getToolTipTextOnTableCell(MouseEvent e, String colName, Object cellValue, int modelRow, int modelCol)
//	{
//		// Get tip on errorMsgCountMap
//		if ("errorMsgCountMap".equals(colName) && cellValue != null)
//		{
//			@SuppressWarnings("serial")
//			java.lang.reflect.Type mapIntStringType = new TypeToken<Map<Integer, String>>(){}.getType();
//			Gson gson = new Gson();
//
//			Object objVal = getValueAt(modelRow, modelCol);
//			if (objVal instanceof String)
//			{
//				String jsonSrc = (String) objVal;
//				if (StringUtil.hasValue(jsonSrc))
//				{
//					String htmlTxt = "";
//					Map<Integer, String> errorMsgCountMap = gson.fromJson(jsonSrc, mapIntStringType);
//
//					htmlTxt += "<html>\n";
//					htmlTxt += "<p>The below table is the parsed JSON value in the cell<br>\n";
//					htmlTxt += "   The Error Number is also <i>enriched</i> with a static description found in <code>master.dbo.sysmessages</code>\n";
//					htmlTxt += "</p>\n";
//					htmlTxt += "<br>\n";
//					htmlTxt += "<table border=1>\n";
//					htmlTxt += "<tr> <th>Error Number</th> <th>Error Count</th> <th>Description</th> </tr>\n";
//					for (Entry<Integer, String> entry : errorMsgCountMap.entrySet())
//					{
//						htmlTxt += "<tr> <td>" + entry.getKey() + "</td> <td>" + entry.getValue() + "</td> <td>" + AseErrorMessageDictionary.getInstance().getDescription(entry.getKey()) + "</td> </tr>\n";
//					}
//					htmlTxt += "</table>\n";
//					htmlTxt += "</html>\n";
//					
//					return htmlTxt;
//				}
//			}
//		}
//
//		return super.getToolTipTextOnTableCell(e, colName, cellValue, modelRow, modelCol);
//	}
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

		boolean debugPrint = Configuration.getCombinedConfiguration().getBooleanProperty("sendAlarmRequest.debug", _logger.isDebugEnabled());

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
//					Map<Integer, Double> sumRateMap = new HashMap<>();
//
//					CounterSample rateData = cm.getCounterSampleRate();
//					int errorMsgCountMap_pos = rateData.findColumn("errorMsgCountMap");
//
//					@SuppressWarnings("serial")
//					java.lang.reflect.Type mapIntLongType = new TypeToken<Map<Integer, Double>>(){}.getType();
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
//							Map<Integer, Double> errorMsgCountMap = gson.fromJson(jsonSrc, mapIntLongType);
//
//							for (Entry<Integer, Double> e : errorMsgCountMap.entrySet())
//							{
//								Double sumRateValue = sumRateMap.get(e.getKey());
//								if (sumRateValue == null)
//									sumRateValue = 0.0;
//								
//								sumRateValue += e.getValue();
//
//								sumRateMap.put(e.getKey(), round1(sumRateValue));
//							}
//						}
//					}
//
//					String errorMsgInfoJson = "";
//					String errorMsgInfoTxt  = "";
//					String errorMsgInfoHtml = "";
//
//					// JSON
//					if ( ! sumRateMap.isEmpty() )
//						errorMsgInfoJson = gson.toJson(sumRateMap);
//					
//					// TXT
//					
//					for (Entry<Integer, Double> e : sumRateMap.entrySet())
//						errorMsgInfoTxt += "Msg=" + e.getKey() + ", ErrorsPerSec=" + e.getValue() + ", Description='" + AseErrorMessageDictionary.getInstance().getDescription(e.getKey()) + "\n";
//
//					// HTML
//					errorMsgInfoHtml += "<table border=1>\n";
//					errorMsgInfoHtml += "<tr> <th>Msg</th> <th>ErrorsPerSec</th> <th>Description</th> </tr>\n";
//					for (Entry<Integer, Double> e : sumRateMap.entrySet())
//						errorMsgInfoHtml += "<tr> <td>" + e.getKey() + "</td> <td>" + e.getValue() + "</td> <td>" + AseErrorMessageDictionary.getInstance().getDescription(e.getKey()) + "</td> </tr>\n";
//					errorMsgInfoHtml += "</table>\n";
//					
//					// END: construct a summary Map (for 'error info' to Alarm) of all RATE errors, and set it to a JSON String...
//
////					System.out.println("--------------------------------------------------------------------------------------");
////					System.out.println("XXXXXXXXXXXXXXXXXX: errorMsgInfoJson="+errorMsgInfoJson);
////					System.out.println("XXXXXXXXXXXXXXXXXX: errorMsgInfoTxt ="+errorMsgInfoTxt);
////					System.out.println("XXXXXXXXXXXXXXXXXX: errorMsgInfoHtml="+errorMsgInfoHtml);
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
//			Map<Integer, Long> sumDiffMap = new HashMap<>();
//
//			CounterSample diffData = cm.getCounterSampleDiff();
//			int errorMsgCountMap_pos = diffData.findColumn("errorMsgCountMap");
//
//			if (errorMsgCountMap_pos != -1)
//			{
//				@SuppressWarnings("serial")
//				java.lang.reflect.Type mapIntLongType = new TypeToken<Map<Integer, Long>>(){}.getType();
//				Gson gson = new Gson();
//
//				for (int r=0; r<diffData.getRowCount(); r++)
//				{
//					String jsonSrc = diffData.getValueAsString(r, errorMsgCountMap_pos);
//					if (StringUtil.isNullOrBlank(jsonSrc))
//						continue;
//
//					Map<Integer, Long> errorMsgCountMap = gson.fromJson(jsonSrc, mapIntLongType);
//
//					for (Entry<Integer, Long> e : errorMsgCountMap.entrySet())
//					{
//						Long sumDiffValue = sumDiffMap.get(e.getKey());
//						if (sumDiffValue == null)
//							sumDiffValue = 0L;
//						
//						sumDiffValue += e.getValue();
//
//						sumDiffMap.put(e.getKey(), sumDiffValue);
//					}
//				}
//			}
//			
////			Map<Integer, Integer> _alarmErrorMap = new HashMap<>();
////			_alarmErrorMap.put(1105, 0);
////			_alarmErrorMap.put(1205, 0);
//			if ( ! sumDiffMap.isEmpty() )
//			{
//				// loop ErrorNumbers and check if we got any matching entries
//				for (Entry<Integer, Integer> e : _map_alarm_ErrorNumbers.entrySet())
//				{
//					Integer alarmErrorNum  = e.getKey();
//					Integer alarmThreshold = e.getValue();
//
//					if (sumDiffMap.containsKey(alarmErrorNum))
//					{
//						Long errorCount = sumDiffMap.getOrDefault(alarmErrorNum, 0L);
//						
//						if (errorCount > alarmThreshold)
//						{
//							String errorDesc = AseErrorMessageDictionary.getInstance().getDescription(alarmErrorNum);
//							
//							if (debugPrint || _logger.isDebugEnabled())
//								System.out.println("##### sendAlarmRequest("+cm.getName()+"): ErrorNumber="+alarmErrorNum+", Count="+errorCount+", is above threshold="+alarmThreshold+".)");
//
//							// Create Alarm
//							AlarmEvent alarm = new AlarmEventClientErrorMsg(cm, alarmErrorNum, errorCount, errorDesc, alarmThreshold);
//							
//							// Set the Error Info
//							String errorMsgInfoTxt  = "Msg=" + alarmErrorNum + ", DiffErrorCount=" + errorCount + ", Description='" + errorDesc + "'";
//							String errorMsgInfoHtml = errorMsgInfoTxt;
//
//							alarm.setExtendedDescription(errorMsgInfoTxt, errorMsgInfoHtml);
//							
//							// Add the Alarm
//							AlarmHandler.getInstance().addAlarm(alarm);
//						}
//					}
//				}
//			}
//		} //end: ErrorNumber
		
		//-------------------------------------------------------
		// errorCount
		//-------------------------------------------------------
		if (isSystemAlarmsForColumnEnabledAndInTimeRange("errorCount"))
		{
			// SUM all rows for 'errorCount' column
			Double errorCountPerSec = cm.getRateValueSum("errorCount");

			if (errorCountPerSec != null)
			{
				int threshold = Configuration.getCombinedConfiguration().getIntProperty(PROPKEY_alarm_ErrorCountPerSec, DEFAULT_alarm_ErrorCountPerSec);

				if (debugPrint || _logger.isDebugEnabled())
					System.out.println("##### sendAlarmRequest("+cm.getName()+"): threshold="+threshold+", errorCountPerSec='"+errorCountPerSec+"'.");

				if (errorCountPerSec.intValue() > threshold)
				{
					// BEGIN: construct a summary Map (for 'error info' to Alarm) of all RATE errors, and set it to a JSON String...
//					Map<Integer, Double> sumRateMap = new HashMap<>();
					Map<Integer, Map<String,Double>> sumDbRateMap = new HashMap<>();

					CounterSample rateData = cm.getCounterSampleRate();
					int errorMsgCountMap_pos = rateData.findColumn("errorMsgCountMap");

					@SuppressWarnings("serial")
					java.lang.reflect.Type mapType = new TypeToken<Map<Integer, Map<String,Double>>>(){}.getType();
					Gson gson = new Gson();

					if (errorMsgCountMap_pos != -1)
					{
						for (int r=0; r<rateData.getRowCount(); r++)
						{
							String jsonSrc = rateData.getValueAsString(r, errorMsgCountMap_pos);
							if (StringUtil.isNullOrBlank(jsonSrc))
								continue;

							Map<Integer, Map<String,Double>> errorMsgCountMap = gson.fromJson(jsonSrc, mapType);
							if (errorMsgCountMap == null)
								continue;

							for (Entry<Integer, Map<String, Double>> e : errorMsgCountMap.entrySet())
							{
								Integer errorNum = e.getKey();
								Map<String, Double> dbMap = e.getValue();
								
								for (Entry<String, Double> dbe : dbMap.entrySet())
								{
									String dbname  = dbe.getKey();
									Double rateVal = dbe.getValue();

									// Sum on ErrorMumber per DBName
									Map<String, Double> sumDbMap = sumDbRateMap.get(errorNum);
									if (sumDbMap == null)
									{
										sumDbMap = new HashMap<>();
										sumDbMap.put(dbname, 0d);
									}
									
									Double sumDbRateValue = sumDbMap.get(dbname);
									if (sumDbRateValue == null)
										sumDbRateValue = 0d;

									sumDbRateValue += rateVal;
									dbMap.put(dbname, sumDbRateValue);
									
									sumDbRateMap.put(errorNum, dbMap);
									
								}
							}
						}
					}

					String errorMsgInfoJson = "";
					String errorMsgInfoTxt  = "";
					String errorMsgInfoHtml = "";

					// JSON
					if ( ! sumDbRateMap.isEmpty() )
						errorMsgInfoJson = gson.toJson(sumDbRateMap);
					
					// TXT
					for (Entry<Integer, Map<String, Double>> e : sumDbRateMap.entrySet())
					{
						Integer errorNum = e.getKey();
						Map<String, Double> dbMap = e.getValue();

						for (Entry<String, Double> dbe : dbMap.entrySet())
						{
							String dbname  = dbe.getKey();
							Double rateVal = dbe.getValue();
							
							errorMsgInfoTxt += "Msg=" + errorNum + ", DBName='" + dbname + "', ErrorsPerSec=" + rateVal + ", Description='" + AseErrorMessageDictionary.getInstance().getDescription(errorNum) + "\n";
						}
					}

					// HTML
					errorMsgInfoHtml += "<table border=1>\n";
					errorMsgInfoHtml += "<tr> <th>Msg</th> <th>DBName</th> <th>ErrorsPerSec</th> <th>Description</th> </tr>\n";
					for (Entry<Integer, Map<String, Double>> e : sumDbRateMap.entrySet())
					{
						Integer errorNum = e.getKey();
						Map<String, Double> dbMap = e.getValue();

						for (Entry<String, Double> dbe : dbMap.entrySet())
						{
							String dbname  = dbe.getKey();
							Double rateVal = dbe.getValue();
							
							errorMsgInfoHtml += "<tr> <td>" + errorNum + "</td> <td>" + dbname + "</td> <td>" + rateVal + "</td> <td>" + AseErrorMessageDictionary.getInstance().getDescription(errorNum) + "</td> </tr>\n";
						}
					}
					errorMsgInfoHtml += "</table>\n";
					
					// END: construct a summary Map (for 'error info' to Alarm) of all RATE errors, and set it to a JSON String...

//					System.out.println("--------------------------------------------------------------------------------------");
//					System.out.println("XXXXXXXXXXXXXXXXXX: errorMsgInfoJson="+errorMsgInfoJson);
//					System.out.println("XXXXXXXXXXXXXXXXXX: errorMsgInfoTxt ="+errorMsgInfoTxt);
//					System.out.println("XXXXXXXXXXXXXXXXXX: errorMsgInfoHtml="+errorMsgInfoHtml);

					errorMsgInfoHtml += "<br><br>" + cm.getGraphDataHistoryAsHtmlImage(GRAPH_NAME_SQL_STATEMENT_ERROR_COUNT);
					errorMsgInfoHtml += "<br><br>" + cm.getGraphDataHistoryAsHtmlImage(GRAPH_NAME_SQL_STATEMENT_SEC);
					
					// Create Alarm
					AlarmEvent alarm = new AlarmEventClientErrorMsgRate(cm, round1(errorCountPerSec), errorMsgInfoJson, threshold);
					
					// Set the Error Info
					alarm.setExtendedDescription(errorMsgInfoTxt, errorMsgInfoHtml);
					
					// Add the Alarm
					AlarmHandler.getInstance().addAlarm(alarm);
				}
			}
		} //end: errorCount

		//-------------------------------------------------------
		// ErrorNumber
		//-------------------------------------------------------
		if (isSystemAlarmsForColumnEnabledAndInTimeRange("ErrorNumbers"))
		{
			// BEGIN: construct a summary Map of all DIFF errors, and set it to a JSON String... errorMsgInfo
//			Map<Integer, Long> sumDiffMap = new HashMap<>();
			Map<Integer, Map<String,Long>> sumDbDiffMap = new HashMap<>();

			CounterSample diffData = cm.getCounterSampleDiff();
			int errorMsgCountMap_pos = diffData.findColumn("errorMsgCountMap");

			if (errorMsgCountMap_pos != -1)
			{
				@SuppressWarnings("serial")
				java.lang.reflect.Type mapType = new TypeToken<Map<Integer, Map<String,Long>>>(){}.getType();
				Gson gson = new Gson();

				for (int r=0; r<diffData.getRowCount(); r++)
				{
					String jsonSrc = diffData.getValueAsString(r, errorMsgCountMap_pos);
					if (StringUtil.isNullOrBlank(jsonSrc))
						continue;

					Map<Integer, Map<String,Long>> errorMsgCountMap = gson.fromJson(jsonSrc, mapType);
					if (errorMsgCountMap == null)
						continue;

					for (Entry<Integer, Map<String,Long>> e : errorMsgCountMap.entrySet())
					{
						Integer           errorNum  = e.getKey();
						Map<String, Long> dbCntrMap = e.getValue();
						
						for (Entry<String, Long> dbe : dbCntrMap.entrySet())
						{
							String  dbname    = dbe.getKey();
							Long    diffCount = dbe.getValue();

//							// Sum on ErrorMumber
//							Long sumDiffValue = sumDiffMap.get(errorNum);
//							if (sumDiffValue == null)
//								sumDiffValue = 0L;
//							
//							sumDiffValue += diffCount;
//
//							sumDiffMap.put(errorNum, sumDiffValue);
							

							// Sum on ErrorMumber per DBName
							Map<String, Long> dbMap = sumDbDiffMap.get(errorNum);
							if (dbMap == null)
							{
								dbMap = new HashMap<>();
								dbMap.put(dbname, 0L);
							}
							
							Long sumDbDiffValue = dbMap.get(dbname);
							if (sumDbDiffValue == null)
								sumDbDiffValue = 0L;

							sumDbDiffValue += diffCount;
							dbMap.put(dbname, sumDbDiffValue);
							
							sumDbDiffMap.put(errorNum, dbMap);
						}
					}
				}
			}
			
//			Map<Integer, Integer> _alarmErrorMap = new HashMap<>();
//			_alarmErrorMap.put(1105, 0);
//			_alarmErrorMap.put(1205, 0);
			if ( ! sumDbDiffMap.isEmpty() )
			{
				// loop ErrorNumbers and check if we got any matching entries
				for (Entry<Integer, Integer> e : _map_alarm_ErrorNumbers.entrySet())
				{
					Integer alarmErrorNum  = e.getKey();
					Integer alarmThreshold = e.getValue();

					if (sumDbDiffMap.containsKey(alarmErrorNum))
					{
						Map<String, Long> dbMap = sumDbDiffMap.get(alarmErrorNum);
						if (dbMap != null)
						{
							for (Entry<String, Long> dbe : dbMap.entrySet())
							{
								String dbname     = dbe.getKey();
								Long   errorCount = dbe.getValue();
								
								if (errorCount == null)
									errorCount = 0L;

								if (errorCount > alarmThreshold)
								{
									String errorDesc = AseErrorMessageDictionary.getInstance().getDescription(alarmErrorNum);
									
									if (debugPrint || _logger.isDebugEnabled())
										System.out.println("##### sendAlarmRequest("+cm.getName()+"): ErrorNumber="+alarmErrorNum+", Count="+errorCount+", is above threshold="+alarmThreshold+".)");

									// if 1105 (out-of-space) -- Get TranLog Charts 'LogSize Left in MB' from CmOpenDatabases
									// NOTE: Make the chart for ALL Databases, since it might be 'tempdb' that the 1105 happens in (even if the current working database is a User Database)
									String extendedDescHtml = "";
									if (alarmErrorNum == 1105)
									{
										CountersModel cmOpenDatabases = getCounterController().getCmByName(CmOpenDatabases.CM_NAME);
										extendedDescHtml = "<br><br>" + cmOpenDatabases.getGraphDataHistoryAsHtmlImage(CmOpenDatabases.GRAPH_NAME_LOGSIZE_LEFT_MB);
										extendedDescHtml = "<br><br>" + cmOpenDatabases.getGraphDataHistoryAsHtmlImage(CmOpenDatabases.GRAPH_NAME_LOGSIZE_USED_PCT);
									}

									// Create Alarm
									AlarmEvent alarm = new AlarmEventClientErrorMsg(cm, alarmErrorNum, dbname, errorCount, errorDesc, alarmThreshold);
									
									// Set the Error Info
									String errorMsgInfoTxt  = "Msg=" + alarmErrorNum + ", DBName='" + dbname + "', DiffErrorCount=" + errorCount + ", Description='" + errorDesc + "'";
									String errorMsgInfoHtml = errorMsgInfoTxt + extendedDescHtml;

									alarm.setExtendedDescription(errorMsgInfoTxt, errorMsgInfoHtml);
									
									// Add the Alarm
									AlarmHandler.getInstance().addAlarm(alarm);
								}
							} // end: dbMap loop
						} // end: dbMap != null
					} // end: sumDbDiffMap has alarmErrorNum
				} // end: _map_alarm_ErrorNumbers
			}
		} //end: ErrorNumber


		// Check if the "statistics producer - SQL Capture Thread" is delivering data statistics (is still alive)
		if (isSystemAlarmsForColumnEnabledAndInTimeRange("SqlCaptureAge"))
		{
			if (_sqlCaptureLastUpdateTime > 0)
			{
				int ageInSec = (int) (System.currentTimeMillis() - _sqlCaptureLastUpdateTime) / 1000;
				int thresholdInSec = Configuration.getCombinedConfiguration().getIntProperty(PROPKEY_alarm_SqlCapUpdateAgeSec, DEFAULT_alarm_SqlCapUpdateAgeSec);
				
				if (debugPrint || _logger.isDebugEnabled())
					System.out.println("##### sendAlarmRequest("+cm.getName()+"): SqlCaptureAge - thresholdInSec="+thresholdInSec+", ageInSec="+ageInSec+".");

				// possibly get Connection connect time also ... and check if we "just" connected... to alarm after a connection "down" time...
				if (getCounterController().getMonConnection() != null)
				{
					int lastConnectTimeInSec = (int) getCounterController().getMonConnection().getConnectTime() / 1000;
					
					if (debugPrint || _logger.isDebugEnabled())
						System.out.println("##### sendAlarmRequest("+cm.getName()+"): SqlCaptureAge - thresholdInSec="+thresholdInSec+", ageInSec="+ageInSec+", lastConnectTimeInSec="+lastConnectTimeInSec+".");

					if (lastConnectTimeInSec > 0 && lastConnectTimeInSec < ageInSec)
					{
						ageInSec = lastConnectTimeInSec;
					}
				}


				if (ageInSec > thresholdInSec)
				{
					AlarmEvent alarm = new AlarmEventSqlCaptureOldData(cm, ageInSec, thresholdInSec);
					AlarmHandler.getInstance().addAlarm(alarm);
				}
			}
		} // end: check - SQL Capture Thread
	}

	@Override
	public boolean isGraphDataHistoryEnabled(String name)
	{
		// ENABLED for the following graphs
		if (GRAPH_NAME_SQL_STATEMENT_ERROR_COUNT.equals(name)) return true;

		// default: DISABLED
		return false;
	}
	@Override
	public int getGraphDataHistoryTimeInterval(String name)
	{
		// Keep interval: default is 60 minutes
		return super.getGraphDataHistoryTimeInterval(name);
	}

	
	private Map<Integer, Integer> _map_alarm_ErrorNumbers;  // Note: do NOT initialize this here... since the initAlarms() is done in super, if initialized it will be overwritten here...

	/**
	 * Initialize stuff that has to do with alarms
	 */
	@Override
	public void initAlarms()
	{
		Configuration conf = Configuration.getCombinedConfiguration();
		String cfgVal;

		_map_alarm_ErrorNumbers = new LinkedHashMap<>();
		
		String prefix = "       ";
		
		//--------------------------------------
		// LowDbFreeSpaceInMb
		cfgVal = conf.getProperty(PROPKEY_alarm_ErrorNumbers, DEFAULT_alarm_ErrorNumbers);
		if (StringUtil.hasValue(cfgVal))
		{
			Map<String, String> map = StringUtil.parseCommaStrToMap(cfgVal);
			if (_logger.isDebugEnabled())
				_logger.debug(prefix + "Initializing alarm 'ErrorNumbers'. After parseCommaStrToMap, map looks like: "+map);
			
			for (String key : map.keySet())
			{
				String val = map.get(key);
				
				try
				{
					int error = NumberUtils.createNumber(key).intValue();
					int count = NumberUtils.createNumber(val).intValue();
					_map_alarm_ErrorNumbers.put(error, count);

					_logger.info(prefix + "Initializing alarm. Using 'ErrorNumbers', ErrorMsg='"+key+"', thresholdCount="+count+", ErrorDesciption='"+AseErrorMessageDictionary.getInstance().getDescription(error)+"'.");
				}
				catch (NumberFormatException ex)
				{
					_logger.info(prefix + "Initializing alarm. Skipping 'ErrorNumbers' enty ErrorMsg='"+key+"', val='"+val+"'. The value is not a number.");
				}
			}
		}
	}

	// Updated by: CounterSamplePrivate.getSample(); used to detect if the "SQL Capture Thread" is still alive and delivers statistics
	private long _sqlCaptureLastUpdateTime = -1;

	public static final String  PROPKEY_alarm_ErrorCountPerSec   = CM_NAME + ".alarm.system.if.errorCount.gt";
	public static final int     DEFAULT_alarm_ErrorCountPerSec   = 20;
	
	public static final String  PROPKEY_alarm_ErrorNumbers       = CM_NAME + ".alarm.system.if.errorNumber";
	public static final String  DEFAULT_alarm_ErrorNumbers       = "701=0, 713=0, 971=5, 1105=0, 1204=0, 1205=5";

	public static final String  PROPKEY_alarm_SqlCapUpdateAgeSec = CM_NAME + ".alarm.system.if.SqlCapture.lastUpdate.ageInSec.gt";
	public static final int     DEFAULT_alarm_SqlCapUpdateAgeSec = 600; // 10 minutes

//	Error=701   , Severity=17 , Message="There is not enough procedure cache to run this procedure, trigger, or SQL batch. Retry later, or ask your SA to reconfigure ASE with more procedure cache.");
//	Error=713   , Severity=16 , Message="Sort failed because there is insufficient procedure cache for the configured number of sort buffers. Please retry the query after configuring lesser number of sort buffers.");
//	Error=971   , Severity=14 , Message="Database '%.*s' is currently unavailable. It is undergoing recovery of a critical database operation due to failure of a cluster instance. Wait and retry later.");
//	Error=1105  , Severity=17 , Message="Can't allocate space for object '%.*s' in database '%.*s' because '%.*s' segment is full/has no free extents. If you ran out of space in syslogs, dump the transaction log. Otherwise, use ALTER DATABASE to increase the size of the segment.");
//	Error=1204  , Severity=17 , Message="ASE has run out of LOCKS. Re-run your command when there are fewer active users, or contact a user with System Administrator (SA) role to reconfigure ASE with more LOCKS.");
//	Error=1205  , Severity=13 , Message="Your server command (family id #%d, process id #%d) encountered a deadlock situation. Please re-run your command.");
	
	@Override
	public List<CmSettingsHelper> getLocalAlarmSettings()
	{
		Configuration conf = Configuration.getCombinedConfiguration();
		List<CmSettingsHelper> list = new ArrayList<>();

		CmSettingsHelper.Type isAlarmSwitch = CmSettingsHelper.Type.IS_ALARM_SWITCH;

		list.add(new CmSettingsHelper("errorCount"   , isAlarmSwitch, PROPKEY_alarm_ErrorCountPerSec,   Integer.class, conf.getIntProperty(PROPKEY_alarm_ErrorCountPerSec  , DEFAULT_alarm_ErrorCountPerSec  ), DEFAULT_alarm_ErrorCountPerSec  , "If 'errorCount' is greater than ## per second then send 'AlarmEventErrorMsgRate'." ));
		list.add(new CmSettingsHelper("ErrorNumbers" , isAlarmSwitch, PROPKEY_alarm_ErrorNumbers,       String .class, conf.getProperty   (PROPKEY_alarm_ErrorNumbers      , DEFAULT_alarm_ErrorNumbers      ), DEFAULT_alarm_ErrorNumbers      , "If 'DiffErrorCount' for 'ErrorMsg' is greater than ## then send 'AlarmEventClientErrorMsg'.format: 1105=#, 1205=#, 1234=# (check sysmessages for descriptions)", new MapNumberValidator()));
		list.add(new CmSettingsHelper("SqlCaptureAge", isAlarmSwitch, PROPKEY_alarm_SqlCapUpdateAgeSec, Integer.class, conf.getIntProperty(PROPKEY_alarm_SqlCapUpdateAgeSec, DEFAULT_alarm_SqlCapUpdateAgeSec), DEFAULT_alarm_SqlCapUpdateAgeSec, "If 'SQL Capture Thread' hasn't been updated it's statistics in ## seconds then send 'AlarmEventSqlCaptureOldData'."));

		return list;
	}
}
