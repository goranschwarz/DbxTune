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
package com.dbxtune.cm.oracle;

import java.util.LinkedList;
import java.util.List;

import com.dbxtune.ICounterController;
import com.dbxtune.IGuiController;
import com.dbxtune.cm.CounterSetTemplates;
import com.dbxtune.cm.CounterSetTemplates.Type;
import com.dbxtune.cm.CountersModel;
import com.dbxtune.gui.MainFrame;
import com.dbxtune.sql.conn.DbxConnection;
import com.dbxtune.sql.conn.info.DbmsVersionInfo;

/**
 * @author Goran Schwarz (goran_schwarz@hotmail.com)
 */
public class CmExecutionTime
extends CountersModel
{
	private static final long    serialVersionUID = 1L;

	public static final String   CM_NAME          = CmExecutionTime.class.getSimpleName();
	public static final String   SHORT_NAME       = "Execution Time";
	public static final String   HTML_DESC        = 
		"<html>" +
		"Execution times for individual Oracle Modules<br>" +
		"Where do we spend the CPU Cycles?<br>" +
//		"<ul>" +
//		"   <li><b>Compilation</b> - Maybe it's time to consider Statement Cache (if not already done), or increase the size of the statement cache</li>" +
//		"   <li><b>Sorting    </b> - Find SQL Statement that does a lot of sorting and try to do that on the client side, if possible, or add index to support that order.</li>" +
//		"   <li><b>Execution  </b> - Hopefully this is where most CPU Cycles is spent.</li>" +
//		"</ul>" +
		"</html>";

	public static final String   GROUP_NAME       = MainFrame.TCP_GROUP_SERVER;
	public static final String   GUI_ICON_FILE    = "images/"+CM_NAME+".png";

	public static final long     NEED_SRV_VERSION = 0;
	public static final long     NEED_CE_VERSION  = 0;

	public static final String[] MON_TABLES       = new String[] {"V$SYS_TIME_MODEL"};
	public static final String[] NEED_ROLES       = new String[] {};
	public static final String[] NEED_CONFIG      = new String[] {};

	public static final String[] PCT_COLUMNS      = new String[] {};
//	public static final String[] PCT_COLUMNS      = new String[] {"CpuUsagePct"};
//	public static final String[] DIFF_COLUMNS     = new String[] {"ExecutionTimeDiff", "ExecutionCntDiff"};
	public static final String[] DIFF_COLUMNS     = new String[] {"VALUE"};

	public static final boolean  NEGATIVE_DIFF_COUNTERS_TO_ZERO = true;
	public static final boolean  IS_SYSTEM_CM                   = true;
	public static final int      DEFAULT_POSTPONE_TIME          = 0;
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

		return new CmExecutionTime(counterController, guiController);
	}

	public CmExecutionTime(ICounterController counterController, IGuiController guiController)
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
	private void addTrendGraphs()
	{
	}
	
//	public static final String GRAPH_NAME_EXECUTION_COUNT          = "CountGraph"; //String x=GetCounters.XXX;
//	public static final String GRAPH_NAME_EXECUTION_TIME           = "TimeGraph"; //String x=GetCounters.XXX;
//	public static final String GRAPH_NAME_EXECUTION_TIME_PER_COUNT = "TimePerCountGraph"; //String x=GetCounters.XXX;
//	public static final String GRAPH_NAME_CPU_USAGE_PCT            = "CpuUsagePct";
//
//	private void addTrendGraphs()
//	{
//////		String[] labels = new String[] { "runtime-replaced" };
////		String[] labels = TrendGraphDataPoint.RUNTIME_REPLACED_LABELS;
////		
////		addTrendGraphData(GRAPH_NAME_EXECUTION_COUNT,          new TrendGraphDataPoint(GRAPH_NAME_EXECUTION_COUNT,          labels, LabelType.Dynamic));
////		addTrendGraphData(GRAPH_NAME_EXECUTION_TIME,           new TrendGraphDataPoint(GRAPH_NAME_EXECUTION_TIME,           labels, LabelType.Dynamic));
////		addTrendGraphData(GRAPH_NAME_EXECUTION_TIME_PER_COUNT, new TrendGraphDataPoint(GRAPH_NAME_EXECUTION_TIME_PER_COUNT, labels, LabelType.Dynamic));
//
//		addTrendGraph(GRAPH_NAME_EXECUTION_COUNT,
//			"ASE SubSystem Execution Count", 	                                 // Menu CheckBox text
//			"ASE SubSystem Operations - Execution Count ("+GROUP_NAME+"->"+SHORT_NAME+")", // Label 
//			TrendGraphDataPoint.Y_AXIS_SCALE_LABELS_NORMAL,
//			null, 
//			LabelType.Dynamic,
//			TrendGraphDataPoint.Category.CPU,
//			false, // is Percent Graph
//			false, // visible at start
//			Ver.ver(15,7,0,100),     // graph is valid from Server Version. 0 = All Versions; >0 = Valid from this version and above 
//			-1);  // minimum height
//
//		// GRAPH
//		addTrendGraph(GRAPH_NAME_EXECUTION_TIME,
//			"ASE SubSystem Execution MicroSeconds", 	                                 // Menu CheckBox text
//			"ASE SubSystem Operations - Execution Time, in Micro Seconds ("+GROUP_NAME+"->"+SHORT_NAME+")", // Label 
//			TrendGraphDataPoint.Y_AXIS_SCALE_LABELS_MICROSEC,
//			null, 
//			LabelType.Dynamic,
//			TrendGraphDataPoint.Category.CPU,
//			false, // is Percent Graph
//			false, // visible at start
//			Ver.ver(15,7,0,100),     // graph is valid from Server Version. 0 = All Versions; >0 = Valid from this version and above 
//			-1);  // minimum height
//
//		// GRAPH
//		addTrendGraph(GRAPH_NAME_EXECUTION_TIME_PER_COUNT,
//			"ASE SubSystem Execution MicroSeconds per Count", 	                                 // Menu CheckBox text
//			"ASE SubSystem Operations - Execution Time, in Micro Seconds per Count ("+GROUP_NAME+"->"+SHORT_NAME+")", // Label 
//			TrendGraphDataPoint.Y_AXIS_SCALE_LABELS_MICROSEC,
//			null, 
//			LabelType.Dynamic,
//			TrendGraphDataPoint.Category.CPU,
//			false, // is Percent Graph
//			false, // visible at start
//			Ver.ver(15,7,0,100),     // graph is valid from Server Version. 0 = All Versions; >0 = Valid from this version and above 
//			-1);  // minimum height
//
//		// GRAPH
//		addTrendGraph(GRAPH_NAME_CPU_USAGE_PCT,
//			"ASE SubSystem Execution CPU Usage Percent", 	                                 // Menu CheckBox text
//			"ASE SubSystem Operations - CPU Usage Percent ("+GROUP_NAME+"->"+SHORT_NAME+")", // Label 
//			TrendGraphDataPoint.Y_AXIS_SCALE_LABELS_PERCENT,
//			null, 
//			LabelType.Dynamic,
//			TrendGraphDataPoint.Category.CPU,
//			true, // is Percent Graph
//			false, // visible at start
//			Ver.ver(15,7,0,100),     // graph is valid from Server Version. 0 = All Versions; >0 = Valid from this version and above 
//			-1);  // minimum height
//	}
//
//	@Override
//	protected TabularCntrPanel createGui()
//	{
//		return new CmExecutionTimePanel(this);
//	}

	@Override
	public String[] getDependsOnConfigForVersion(DbxConnection conn, DbmsVersionInfo versionInfo)
	{
		return NEED_CONFIG;
	}

//	@Override
//	public void addMonTableDictForVersion(DbxConnection conn, DbmsVersionInfo versionInfo)
//	{
//		try 
//		{
//			MonTablesDictionary mtd = MonTablesDictionaryManager.getInstance();
//			mtd.addColumn("monSysExecutionTime",  "ExecutionTimePerCnt",     "<html>" +
//			                                                                     "ExecutionTime in Micro Seconds for each individual ExecutionCount<br>" +
//			                                                                     "<b>Formula</b>: ExecutionTime / ExecutionCnt<br>" +
//			                                                                 "</html>");
//			mtd.addColumn("monSysExecutionTime",  "CpuUsagePct",             "<html>" +
//			                                                                     "How much CPU Time (in Percent) does this 'operation category' consume of the total CPU Time/slots available.<br>" +
//			                                                                     "<b>Formula ABS</b>:  ((abs.ExecutionTime/1000000) / (<i>srvRunningForSec*engCnt</i>)) * 100.0<br>" +
//			                                                                     "<b>Formula RATE</b>: used_ExecuationTimeInMicroSeconds_inThisPeriod / total_MicroSecondsPossibleForScheduling_forAllEngines_inThisPeriod. or detailed: val = (rate_ExecutionTime / (1_000_000.0 * rate_EngineCount)) * 100.0<br>" +
//			                                                                     "<b>Formula DIFF</b>: same as RATE<br>" +
//			                                                                 "</html>");
//			mtd.addColumn("monSysExecutionTime",  "EngineCount",             "<html>" +
//			                                                                     "How many engines does the ASE has avilable<br>" +
//			                                                                     "<b>Formula</b>: count(*) from master.dbo.monEngine where Status = 'online' <br>" +
//			                                                                 "</html>");
//		}
//		catch (NameNotFoundException e) {/*ignore*/}
//	}

	@Override
	public List<String> getPkForVersion(DbxConnection conn, DbmsVersionInfo versionInfo)
	{
		List <String> pkCols = new LinkedList<String>();

		pkCols.add("STAT_ID");
//		pkCols.add("STAT_NAME");

		return pkCols;
	}

	@Override
	public String getSqlForVersion(DbxConnection conn, DbmsVersionInfo versionInfo)
	{
		String sql = "select * from V$SYS_TIME_MODEL \n";
		return sql;
	}

//	@Override
//	public void updateGraphData(TrendGraphDataPoint tgdp)
//	{
//		if (GRAPH_NAME_EXECUTION_COUNT.equals(tgdp.getName()))
//		{
//			// Write 1 "line" for every rowInTable
//			Double[] dArray = new Double[this.size()];
//			String[] lArray = new String[dArray.length];
//			for (int i = 0; i < dArray.length; i++)
//			{
//				lArray[i] = this.getRateString       (i, "OperationName");
//				dArray[i] = this.getRateValueAsDouble(i, "ExecutionCnt");
//			}
//
//			// Set the values
//			tgdp.setDataPoint(this.getTimestamp(), lArray, dArray);
//		}
//
//		if (GRAPH_NAME_EXECUTION_TIME.equals(tgdp.getName()))
//		{
//			// Write 1 "line" for every rowInTable
//			Double[] dArray = new Double[this.size()];
//			String[] lArray = new String[dArray.length];
//			for (int i = 0; i < dArray.length; i++)
//			{
//				lArray[i] = this.getRateString       (i, "OperationName");
//				dArray[i] = this.getRateValueAsDouble(i, "ExecutionTime");
//			}
//
//			// Set the values
//			tgdp.setDataPoint(this.getTimestamp(), lArray, dArray);
//		}
//
//		if (GRAPH_NAME_EXECUTION_TIME_PER_COUNT.equals(tgdp.getName()))
//		{
//			// Write 1 "line" for every rowInTable
//			Double[] dArray = new Double[this.size()];
//			String[] lArray = new String[dArray.length];
//			for (int i = 0; i < dArray.length; i++)
//			{
//				lArray[i] = this.getRateString       (i, "OperationName");
//				dArray[i] = this.getRateValueAsDouble(i, "ExecutionTimePerCnt");
//			}
//
//			// Set the values
//			tgdp.setDataPoint(this.getTimestamp(), lArray, dArray);
//		}
//
//		if (GRAPH_NAME_CPU_USAGE_PCT.equals(tgdp.getName()))
//		{
//			// Summary of ALL CPU lines
//			double sum = 0;
//			
//			// Write 1 "line" for every rowInTable
//			Double[] dArray = new Double[this.size() + 1];
//			String[] lArray = new String[dArray.length];
//
//			for (int ap = 0; ap < dArray.length; ap++)
//			{
//				// ArrayPos 0 holds: SUMMARY which will be added at the end
//				if (ap == 0)
//					continue;
//
//				int row = ap - 1;
//				
//				lArray[ap] = this.getRateString       (row, "OperationName");
//				dArray[ap] = this.getRateValueAsDouble(row, "CpuUsagePct");
//				
//				sum += dArray[ap]; 
//			}
//			
//			// Finally Write the SUMMARY
//			lArray[0] = "SumAllCategories";
//			dArray[0] = sum;
//
//			// Set the values
//			tgdp.setDataPoint(this.getTimestamp(), lArray, dArray);
//		}
//	}
//
//	@Override
//	public void localCalculation(CounterSample prevSample, CounterSample newSample, CounterSample diffData)
//	{
//		int  ExecutionTimePerCnt_pos = -1;
//
//		long ExecutionCnt,          ExecutionTime;
//		int  ExecutionCnt_pos = -1, ExecutionTime_pos = -1;
//
//		// Find column Id's
//		List<String> colNames = diffData.getColNames();
//		if (colNames == null)
//			return;
//		for (int colId = 0; colId < colNames.size(); colId++)
//		{
//			String colName = (String) colNames.get(colId);
//			if      (colName.equals("ExecutionCnt"))        ExecutionCnt_pos        = colId;
//			else if (colName.equals("ExecutionTime"))       ExecutionTime_pos       = colId;
//			else if (colName.equals("ExecutionTimePerCnt")) ExecutionTimePerCnt_pos = colId;
//		}
//
//		// Loop on all diffData rows
//		for (int rowId = 0; rowId < diffData.getRowCount(); rowId++)
//		{
//			ExecutionCnt  = ((Number) diffData.getValueAt(rowId, ExecutionCnt_pos )).longValue();
//			ExecutionTime = ((Number) diffData.getValueAt(rowId, ExecutionTime_pos)).longValue();
//
//			if (ExecutionCnt > 0)
//			{
//				double calc = (ExecutionTime + 0.0) / (ExecutionCnt + 0.0);
//
//				BigDecimal newVal = new BigDecimal(calc).setScale(1, BigDecimal.ROUND_HALF_EVEN);
//				diffData.setValueAt(newVal, rowId, ExecutionTimePerCnt_pos);
//			}
//			else
//				diffData.setValueAt(new BigDecimal(0), rowId, ExecutionTimePerCnt_pos);
//		}
//	}
//	
//	@Override
//	public void localCalculationRatePerSec(CounterSample rateData, CounterSample diffData)
//	{
//		int  ExecutionTime_pos = -1;
//		int  CpuUsagePct_pos   = -1;
//		int  EngineCount_pos   = -1;
//
//		// Find column Id's
//		List<String> colNames = diffData.getColNames();
//		if (colNames == null)
//			return;
//		for (int colId = 0; colId < colNames.size(); colId++)
//		{
//			String colName = (String) colNames.get(colId);
//			if      (colName.equals("ExecutionTime"))       ExecutionTime_pos       = colId;
//			else if (colName.equals("EngineCount"))         EngineCount_pos         = colId;
//			else if (colName.equals("CpuUsagePct"))         CpuUsagePct_pos         = colId;
//		}
//
//		// Loop on all rate/diff Data rows
//		for (int rowId = 0; rowId < diffData.getRowCount(); rowId++)
//		{
//			double rate_ExecutionTime = ((Number) rateData.getValueAt(rowId, ExecutionTime_pos)).doubleValue();
//			int    rate_EngineCount   = ((Number) rateData.getValueAt(rowId, EngineCount_pos  )).intValue();
//
//			// CpuUsagePct
//			if (rate_EngineCount > 0)
//			{
//				// Basic Algorithm: used_ExecuationTimeInMicroSeconds_inThisPeriod / total_MicroSecondsPossibleForScheduling_forAllEngines_inThisPeriod
//
//				// Should we filter out "out-of-bound" values here as well?
//				// that would be: if (rate_ExecutionTime > 1_000_000.0 * rate_EngineCount) ...
//				
//				// ExecutionTime                  / allEngines*1000000 == PossibleExecutionSlotsInMicroSec
//				double calc = (rate_ExecutionTime / (1_000_000.0 * rate_EngineCount)) * 100.0;
//
//				BigDecimal newVal = new BigDecimal(calc).setScale(1, BigDecimal.ROUND_HALF_EVEN);
//				diffData.setValueAt(newVal, rowId, CpuUsagePct_pos);
//				rateData.setValueAt(newVal, rowId, CpuUsagePct_pos);
//			}
//		}
//	}
}
