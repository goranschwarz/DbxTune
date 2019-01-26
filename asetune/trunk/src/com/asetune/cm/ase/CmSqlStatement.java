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

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

import javax.naming.NameNotFoundException;

import org.apache.log4j.Logger;

import com.asetune.ICounterController;
import com.asetune.IGuiController;
import com.asetune.cm.CounterSample;
import com.asetune.cm.CounterSetTemplates;
import com.asetune.cm.CounterSetTemplates.Type;
import com.asetune.cm.CountersModel;
import com.asetune.cm.NoValidRowsInSample;
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
import com.asetune.utils.Configuration;

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
			,"inStmntCacheCount"
			,"dynamicStmntCount"
			,"inProcedureCount"
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


	//------------------------------------------------------------
	// Implementation
	//------------------------------------------------------------
	public static final String GRAPH_NAME_SQL_STATEMENT_SEC            = "SqlStmnt";
	public static final String GRAPH_NAME_SQL_STATEMENT_TIME_SPAN_ALL  = "SqlStmntTSpanAll";
	public static final String GRAPH_NAME_SQL_STATEMENT_TIME_SPAN_NOOP = "SqlStmntTSpanNoOp";
	public static final String GRAPH_NAME_SQL_STATEMENT_TIME_SPAN_0    = "SqlStmntTSpan0";
	public static final String GRAPH_NAME_SQL_STATEMENT_TIME_SPAN_1    = "SqlStmntTSpan1";
	public static final String GRAPH_NAME_SQL_STATEMENT_TIME_SPAN_2    = "SqlStmntTSpan2";
	public static final String GRAPH_NAME_SQL_STATEMENT_TIME_SPAN_3    = "SqlStmntTSpan3";
	public static final String GRAPH_NAME_SQL_STATEMENT_TIME_SPAN_4    = "SqlStmntTSpan4";
	
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
			new String[] { "10-20s", "20-50s", "50-100s", ">100s" }, 
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
			// TODO: rewrite to use: this.getAbsValueAsDouble(pk, "calculated_value");
			Double[] arr = new Double[18];

			// Note the prefix: 'SQLServer' or 'MSSQL$@@servicename' is removed in SQL query
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

			Double val0  = this.getRateValueAsDouble(pk0 , "totalCount");
			Double val1  = this.getRateValueAsDouble(pk1 , "totalCount");
			Double val2  = this.getRateValueAsDouble(pk2 , "totalCount");
			Double val3  = this.getRateValueAsDouble(pk3 , "totalCount");
			Double val4  = this.getRateValueAsDouble(pk4 , "totalCount");
			Double val5  = this.getRateValueAsDouble(pk5 , "totalCount");
			Double val6  = this.getRateValueAsDouble(pk6 , "totalCount");
			Double val7  = this.getRateValueAsDouble(pk7 , "totalCount");
			Double val8  = this.getRateValueAsDouble(pk8 , "totalCount");
			Double val9  = this.getRateValueAsDouble(pk9 , "totalCount");
			Double val10 = this.getRateValueAsDouble(pk10, "totalCount");
			Double val11 = this.getRateValueAsDouble(pk11, "totalCount");
			Double val12 = this.getRateValueAsDouble(pk12, "totalCount");
			Double val13 = this.getRateValueAsDouble(pk13, "totalCount");
			Double val14 = this.getRateValueAsDouble(pk14, "totalCount");
			Double val15 = this.getRateValueAsDouble(pk15, "totalCount");
			Double val16 = this.getRateValueAsDouble(pk16, "totalCount");
			Double val17 = this.getRateValueAsDouble(pk17, "totalCount");
			
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
					tg.setWarningLabel("Failed to get value for 'some' pk-row: '"+pk1+"'='"+val1+"'.");
			}
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
	}
	
//	@Override
//	protected TabularCntrPanel createGui()
//	{
//		return new ACopyMePanel(this);
//	}

	@Override
	public void addMonTableDictForVersion(Connection conn, long srvVersion, boolean isClusterEnabled)
	{
		try 
		{
			MonTablesDictionary mtd = MonTablesDictionaryManager.getInstance();
			mtd.addTable("CmSqlStatement",  "xxx.");

			mtd.addColumn("CmSqlStatement", "name",                 "<html>Execution time span for this statistics</html>");

			mtd.addColumn("CmSqlStatement", "totalCount",           "<html>Summary value for 'inSqlBatchCount', 'inStmntCacheCount', 'dynamicStmntCount' and 'inProcedureCount'.</html>");
			mtd.addColumn("CmSqlStatement", "totalCountAbs",        "<html>Summary value for 'inSqlBatchCount', 'inStmntCacheCount', 'dynamicStmntCount' and 'inProcedureCount'.</html>");

			mtd.addColumn("CmSqlStatement", "sqlBatchCount",        "<html>Estimated SQL Batch requests. <br><b>Algorithm:</b> if columns 'ProcName' is NULL from monSysStatement <br><b>Note:</b> If the SqlBatch contains several statements, the counter will be incremented for every statement within the SQL Batch.</html>");
			mtd.addColumn("CmSqlStatement", "sqlBatchCountAbs",     "<html>Estimated SQL Batch requests. <br><b>Algorithm:</b> if columns 'ProcName' is NULL from monSysStatement <br><b>Note:</b> If the SqlBatch contains several statements, the counter will be incremented for every statement within the SQL Batch.</html>");

			mtd.addColumn("CmSqlStatement", "inStmntCacheCount",    "<html>Estimated SQL Statements Batch requests executed as a Statement Cache (compiled). <br><b>Algorithm:</b> if columns 'ProcName' starts with '*ss' from monSysStatement</html>");
			mtd.addColumn("CmSqlStatement", "inStmntCacheCountAbs", "<html>Estimated SQL Statements Batch requests executed as a Statement Cache (compiled). <br><b>Algorithm:</b> if columns 'ProcName' starts with '*ss' from monSysStatement</html>");

			mtd.addColumn("CmSqlStatement", "dynamicStmntCount",    "<html>Estimated Dynamic SQL Statements (ct_dynamic or PreparedStatement using a LWP). <br><b>Algorithm:</b> if columns 'ProcName' starts with '*sq' from monSysStatement</html>");
			mtd.addColumn("CmSqlStatement", "dynamicStmntCountAbs", "<html>Estimated Dynamic SQL Statements (ct_dynamic or PreparedStatement using a LWP). <br><b>Algorithm:</b> if columns 'ProcName' starts with '*sq' from monSysStatement</html>");

			mtd.addColumn("CmSqlStatement", "inProcedureCount",     "<html>Estimated SQL Statements requests executed from withing a Stored Proc. <br><b>Algorithm:</b> if columns 'ProcName' does NOT start with '*sq' or '*ss' from monSysStatement</html>");
			mtd.addColumn("CmSqlStatement", "inProcedureCountAbs",  "<html>Estimated SQL Statements requests executed from withing a Stored Proc. <br><b>Algorithm:</b> if columns 'ProcName' does NOT start with '*sq' or '*ss' from monSysStatement</html>");
			
			mtd.addColumn("CmSqlStatement", "sumExecTimeMs",        "<html>Summary of all Executions for this time span.<br>Also 'diff' and 'rate' calculated to get a sence for the changes<br>But for this metrics it's probably more interesting to look at the ABS column (to the right)</html>");
			mtd.addColumn("CmSqlStatement", "sumExecTimeMsAbs",     "<html>Summary of all Executions for this time span.</html>");
			mtd.addColumn("CmSqlStatement", "avgExecTimeMs",        "<html>Average execution time for this time span.<br><b>Algorithm:</b> abs.sumExecTimeMs / abs.totalCount</html>");
			mtd.addColumn("CmSqlStatement", "maxExecTimeMs",        "<html>Maximum execution time for this time span.</html>");
			                                                        
			mtd.addColumn("CmSqlStatement", "sumLogicalReads",      "<html>Summary of all LogicalReads for this time span.<br>Also 'diff' and 'rate' calculated to get a sence for the changes</html>");
			mtd.addColumn("CmSqlStatement", "sumLogicalReadsAbs",   "<html>Summary of all LogicalReads for this time span.</html>");
			mtd.addColumn("CmSqlStatement", "avgLogicalReads",      "<html>Average LogicalReads for this time span.<br><b>Algorithm:</b> abs.sumLogicalReads / abs.totalCount</html>");
			mtd.addColumn("CmSqlStatement", "maxLogicalReads",      "<html>Maximum LogicalReads for this time span.</html>");
                                                                    
			mtd.addColumn("CmSqlStatement", "sumPhysicalReads",     "<html>Summary of all PhysicalReads for this time span.<br>Also 'diff' and 'rate' calculated to get a sence for the changes</html>");
			mtd.addColumn("CmSqlStatement", "sumPhysicalReadsAbs",  "<html>Summary of all PhysicalReads for this time span.</html>");
			mtd.addColumn("CmSqlStatement", "avgPhysicalReads",     "<html>Average PhysicalReads for this time span.<br><b>Algorithm:</b> abs.sumPhysicalReads / abs.totalCount</html>");
			mtd.addColumn("CmSqlStatement", "maxPhysicalReads",     "<html>Maximum PhysicalReads for this time span.</html>");
                                                                    
			mtd.addColumn("CmSqlStatement", "sumCpuTime",           "<html>Summary of all CpuTime for this time span.<br>Also 'diff' and 'rate' calculated to get a sence for the changes<br>But for this metrics it's probably more interesting to look at the ABS column (to the right)</html>");
			mtd.addColumn("CmSqlStatement", "sumCpuTimeAbs",        "<html>Summary of all CpuTime for this time span.</html>");
			mtd.addColumn("CmSqlStatement", "avgCpuTime",           "<html>Average CpuTime for this time span.<br><b>Algorithm:</b> abs.sumCpuTime / abs.totalCount</html>");
			mtd.addColumn("CmSqlStatement", "maxCpuTime",           "<html>Maximum CpuTime for this time span.</html>");
                                                                    
			mtd.addColumn("CmSqlStatement", "sumWaitTime",          "<html>Summary of all WaitTime for this time span.<br>Also 'diff' and 'rate' calculated to get a sence for the changes<br>But for this metrics it's probably more interesting to look at the ABS column (to the right)</html>");
			mtd.addColumn("CmSqlStatement", "sumWaitTimeAbs",       "<html>Summary of all WaitTime for this time span.</html>");
			mtd.addColumn("CmSqlStatement", "avgWaitTime",          "<html>Average WaitTime for this time span.<br><b>Algorithm:</b> abs.sumWaitTime / abs.totalCount</html>");
			mtd.addColumn("CmSqlStatement", "maxWaitTime",          "<html>Maximum WaitTime for this time span.</html>");
                                                                    
			mtd.addColumn("CmSqlStatement", "sumRowsAffected",      "<html>Summary of all RowsAffected for this time span.<br>Also 'diff' and 'rate' calculated to get a sence for the changes</html>");
			mtd.addColumn("CmSqlStatement", "sumRowsAffectedAbs",   "<html>Summary of all RowsAffected for this time span.</html>");
			mtd.addColumn("CmSqlStatement", "avgRowsAffected",      "<html>Average RowsAffected for this time span.<br><b>Algorithm:</b> abs.sumRowsAffected / abs.totalCount</html>");
			mtd.addColumn("CmSqlStatement", "maxRowsAffected",      "<html>Maximum RowsAffected for this time span.</html>");
		}
		catch (NameNotFoundException e) {/*ignore*/}
	}

	@Override
	public String[] getDependsOnConfigForVersion(Connection conn, long srvVersion, boolean isClusterEnabled)
	{
		return NEED_CONFIG;
	}

	@Override
	public boolean checkDependsOnConfig(Connection conn)
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
	public String getSqlForVersion(Connection conn, long srvVersion, boolean isClusterEnabled)
	{
		return "-- grabed from AseTune internally: PersistentCounterHandler.getInstance().getSqlCaptureBroker().getStatementStats(false)";
	}

	@Override
	public List<String> getPkForVersion(Connection conn, long srvVersion, boolean isClusterEnabled)
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
}
