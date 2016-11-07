package com.asetune.cm.ase;

import java.math.BigDecimal;
import java.sql.Connection;
import java.util.LinkedList;
import java.util.List;

import javax.naming.NameNotFoundException;

import com.asetune.ICounterController;
import com.asetune.IGuiController;
import com.asetune.cm.CounterSample;
import com.asetune.cm.CounterSetTemplates;
import com.asetune.cm.CounterSetTemplates.Type;
import com.asetune.cm.CountersModel;
import com.asetune.cm.ase.gui.CmExecutionTimePanel;
import com.asetune.config.dict.MonTablesDictionary;
import com.asetune.config.dict.MonTablesDictionaryManager;
import com.asetune.graph.TrendGraphDataPoint;
import com.asetune.gui.MainFrame;
import com.asetune.gui.TabularCntrPanel;
import com.asetune.gui.TrendGraph;
import com.asetune.utils.Ver;

/**
 * @author Goran Schwarz (goran_schwarz@hotmail.com)
 */
public class CmExecutionTime
extends CountersModel
{
//	private static Logger        _logger          = Logger.getLogger(ACopyMe.class);
	private static final long    serialVersionUID = 1L;

	public static final String   CM_NAME          = CmExecutionTime.class.getSimpleName();
	public static final String   SHORT_NAME       = "Execution Time";
	public static final String   HTML_DESC        = 
		"<html>" +
		"Execution times for individual ASE Modules<br>" +
		"Where do we spend the CPU Cycles?<br>" +
		"<ul>" +
		"   <li>Compilation - Maybe it's time to consider Statement Cache (if not already done)</li>" +
		"   <li>Sorting - Find SQL Statement that does a lot of sorting and try to do that on the client side, if possible</li>" +
		"   <li>Execution - Hopefully this is where most CPU Cycles is spent.</li>" +
		"</ul>" +
		"</html>";

	public static final String   GROUP_NAME       = MainFrame.TCP_GROUP_SERVER;
	public static final String   GUI_ICON_FILE    = "images/"+CM_NAME+".png";

//	public static final int      NEED_SRV_VERSION = 1570100; // 15.7 SP100
	public static final int      NEED_SRV_VERSION = Ver.ver(15,7,0,100); // 15.7 SP100
	public static final int      NEED_CE_VERSION  = 0;

	public static final String[] MON_TABLES       = new String[] {"monSysExecutionTime"};
	public static final String[] NEED_ROLES       = new String[] {"mon_role"};
	public static final String[] NEED_CONFIG      = new String[] {"enable monitoring=1", "execution time monitoring=1"};

	public static final String[] PCT_COLUMNS      = new String[] {};
//	public static final String[] DIFF_COLUMNS     = new String[] {"ExecutionTimeDiff", "ExecutionCntDiff"};
	public static final String[] DIFF_COLUMNS     = new String[] {"ExecutionTime", "ExecutionCnt"};

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
	
	public static final String GRAPH_NAME_EXECUTION_COUNT          = "CountGraph"; //String x=GetCounters.XXX;
	public static final String GRAPH_NAME_EXECUTION_TIME           = "TimeGraph"; //String x=GetCounters.XXX;
	public static final String GRAPH_NAME_EXECUTION_TIME_PER_COUNT = "TimePerCountGraph"; //String x=GetCounters.XXX;

	private void addTrendGraphs()
	{
		String[] labels = new String[] { "runtime-replaced" };
		
		addTrendGraphData(GRAPH_NAME_EXECUTION_COUNT,          new TrendGraphDataPoint(GRAPH_NAME_EXECUTION_COUNT,          labels));
		addTrendGraphData(GRAPH_NAME_EXECUTION_TIME,           new TrendGraphDataPoint(GRAPH_NAME_EXECUTION_TIME,           labels));
		addTrendGraphData(GRAPH_NAME_EXECUTION_TIME_PER_COUNT, new TrendGraphDataPoint(GRAPH_NAME_EXECUTION_TIME_PER_COUNT, labels));

		// if GUI
		if (getGuiController() != null && getGuiController().hasGUI())
		{
			// GRAPH
			TrendGraph tg = null;
			tg = new TrendGraph(GRAPH_NAME_EXECUTION_COUNT,
				"ASE SubSystem Execution Count", 	                                 // Menu CheckBox text
				"ASE SubSystem Operations - Execution Count (15.7 SP100 or above)", // Label 
				labels, 
				false, // is Percent Graph
				this, 
				false, // visible at start
//				1570100,     // graph is valid from Server Version. 0 = All Versions; >0 = Valid from this version and above 
				Ver.ver(15,7,0,100),     // graph is valid from Server Version. 0 = All Versions; >0 = Valid from this version and above 
				-1);  // minimum height
			addTrendGraph(tg.getName(), tg, true);

			// GRAPH
			tg = new TrendGraph(GRAPH_NAME_EXECUTION_TIME,
				"ASE SubSystem Execution MicroSeconds", 	                                 // Menu CheckBox text
				"ASE SubSystem Operations - Execution Time, in Micro Seconds (15.7 SP100 or above)", // Label 
				labels, 
				false, // is Percent Graph
				this, 
				false, // visible at start
//				1570100,     // graph is valid from Server Version. 0 = All Versions; >0 = Valid from this version and above 
				Ver.ver(15,7,0,100),     // graph is valid from Server Version. 0 = All Versions; >0 = Valid from this version and above 
				-1);  // minimum height
			addTrendGraph(tg.getName(), tg, true);

			// GRAPH
			tg = new TrendGraph(GRAPH_NAME_EXECUTION_TIME_PER_COUNT,
				"ASE SubSystem Execution MicroSeconds per Count", 	                                 // Menu CheckBox text
				"ASE SubSystem Operations - Execution Time, in Micro Seconds per Count (15.7 SP100 or above)", // Label 
				labels, 
				false, // is Percent Graph
				this, 
				false, // visible at start
//				1570100,     // graph is valid from Server Version. 0 = All Versions; >0 = Valid from this version and above 
				Ver.ver(15,7,0,100),     // graph is valid from Server Version. 0 = All Versions; >0 = Valid from this version and above 
				-1);  // minimum height
			addTrendGraph(tg.getName(), tg, true);
		}
	}

	@Override
	protected TabularCntrPanel createGui()
	{
		return new CmExecutionTimePanel(this);
	}

	@Override
	public String[] getDependsOnConfigForVersion(Connection conn, int srvVersion, boolean isClusterEnabled)
	{
		return NEED_CONFIG;
	}

	@Override
	public void addMonTableDictForVersion(Connection conn, int aseVersion, boolean isClusterEnabled)
	{
		try 
		{
			MonTablesDictionary mtd = MonTablesDictionaryManager.getInstance();
			mtd.addColumn("monSysExecutionTime",  "ExecutionTimePerCnt",     "<html>" +
			                                                                     "ExecutionTime in Micro Seconds for each individual ExecutionCount<br>" +
			                                                                     "<b>Formula</b>: ExecutionTime / ExecutionCnt<br>" +
			                                                                 "</html>");
		}
		catch (NameNotFoundException e) {/*ignore*/}
	}

	@Override
	public List<String> getPkForVersion(Connection conn, int srvVersion, boolean isClusterEnabled)
	{
		List <String> pkCols = new LinkedList<String>();

		pkCols.add("InstanceID");
		pkCols.add("OperationID");

		return pkCols;
	}

	@Override
	public String getSqlForVersion(Connection conn, int aseVersion, boolean isClusterEnabled)
	{
		String sql = 
			"select InstanceID, OperationID, OperationName, ExecutionCnt, ExecutionTime, \n" +
			"       ExecutionTimePerCnt = CASE WHEN ExecutionCnt > 0 \n" +
			"                                  THEN convert(numeric(10,1), (ExecutionTime + 0.0) / (ExecutionCnt + 0.0) ) \n" +
			"                                  ELSE convert(numeric(10,1), null) \n" +
			"                             END \n" +
//			"       ExecutionCntDiff = ExecutionCnt, ExecutionTimeDiff = ExecutionTime \n" +
//			"       description  = CASE \n" +
//			"                           WHEN OperationID = 0 THEN convert(varchar(255), '') \n" +
//			"                           WHEN OperationID = 1 THEN convert(varchar(255), '') \n" +
//			"                           WHEN OperationID = 2 THEN convert(varchar(255), '') \n" +
//			"                           WHEN OperationID = 3 THEN convert(varchar(255), '') \n" +
//			"                           WHEN OperationID = 4 THEN convert(varchar(255), '') \n" +
//			"                           WHEN OperationID = 5 THEN convert(varchar(255), '') \n" +
//			"                           ELSE                      convert(varchar(255), '') \n" +
//			"                      END \n" +
			"from master..monSysExecutionTime";
		return sql;
	}

	@Override
	public void updateGraphData(TrendGraphDataPoint tgdp)
	{
		if (GRAPH_NAME_EXECUTION_COUNT.equals(tgdp.getName()))
		{
			// Write 1 "line" for every database
			Double[] dArray = new Double[this.size()];
			String[] lArray = new String[dArray.length];
			for (int i = 0; i < dArray.length; i++)
			{
				lArray[i] = this.getRateString       (i, "OperationName");
				dArray[i] = this.getRateValueAsDouble(i, "ExecutionCnt");
			}

			// Set the values
			tgdp.setDate(this.getTimestamp());
			tgdp.setLabel(lArray);
			tgdp.setData(dArray);
		}

		if (GRAPH_NAME_EXECUTION_TIME.equals(tgdp.getName()))
		{
			// Write 1 "line" for every database
			Double[] dArray = new Double[this.size()];
			String[] lArray = new String[dArray.length];
			for (int i = 0; i < dArray.length; i++)
			{
				lArray[i] = this.getRateString       (i, "OperationName");
				dArray[i] = this.getRateValueAsDouble(i, "ExecutionTime");
			}

			// Set the values
			tgdp.setDate(this.getTimestamp());
			tgdp.setLabel(lArray);
			tgdp.setData(dArray);
		}

		if (GRAPH_NAME_EXECUTION_TIME_PER_COUNT.equals(tgdp.getName()))
		{
			// Write 1 "line" for every database
			Double[] dArray = new Double[this.size()];
			String[] lArray = new String[dArray.length];
			for (int i = 0; i < dArray.length; i++)
			{
				lArray[i] = this.getRateString       (i, "OperationName");
				dArray[i] = this.getRateValueAsDouble(i, "ExecutionTimePerCnt");
			}

			// Set the values
			tgdp.setDate(this.getTimestamp());
			tgdp.setLabel(lArray);
			tgdp.setData(dArray);
		}
	}

	@Override
	public void localCalculation(CounterSample prevSample, CounterSample newSample, CounterSample diffData)
	{
		int  ExecutionTimePerCnt_pos = -1;

		long ExecutionCnt,          ExecutionTime;
		int  ExecutionCnt_pos = -1, ExecutionTime_pos = -1;

		// Find column Id's
		List<String> colNames = diffData.getColNames();
		if (colNames == null)
			return;
		for (int colId = 0; colId < colNames.size(); colId++)
		{
			String colName = (String) colNames.get(colId);
			if      (colName.equals("ExecutionCnt"))        ExecutionCnt_pos        = colId;
			else if (colName.equals("ExecutionTime"))       ExecutionTime_pos       = colId;
			else if (colName.equals("ExecutionTimePerCnt")) ExecutionTimePerCnt_pos = colId;
		}

		// Loop on all diffData rows
		for (int rowId = 0; rowId < diffData.getRowCount(); rowId++)
		{
			ExecutionCnt  = ((Number) diffData.getValueAt(rowId, ExecutionCnt_pos)) .longValue();
			ExecutionTime = ((Number) diffData.getValueAt(rowId, ExecutionTime_pos)).longValue();

			if (ExecutionCnt > 0)
			{
				double calc = (ExecutionTime + 0.0) / (ExecutionCnt + 0.0);

				BigDecimal newVal = new BigDecimal(calc).setScale(1, BigDecimal.ROUND_HALF_EVEN);
				diffData.setValueAt(newVal, rowId, ExecutionTimePerCnt_pos);
			}
			else
				diffData.setValueAt(new BigDecimal(0), rowId, ExecutionTimePerCnt_pos);
		}
	}
}
