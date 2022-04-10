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
//XXX* to config dlg * select * from monThreadPool		--Provides information on the thread pools within the system	
//* to server     * select * from monThread			--Provides information on ASE threads	
//* to disk       * select * from monIOController		--Provides information on I/O controllers	
//* ?  server     * select * from monWorkQueue		--Provides information on work queues	
//* not ?? proces * select * from monTask			--Provides information on tasks subsystem	
//* not           * select * from monServiceTask		--Provides information on service task bindings	
package com.asetune.cm.ase;

import java.math.BigDecimal;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;

import javax.swing.JCheckBoxMenuItem;

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
import com.asetune.gui.TrendGraph;
import com.asetune.sql.conn.DbxConnection;
import com.asetune.utils.Ver;

/**
 * @author Goran Schwarz (goran_schwarz@hotmail.com)
 */
public class CmThreads
extends CountersModel
{
	private static Logger        _logger          = Logger.getLogger(CmThreads.class);
	private static final long    serialVersionUID = 1L;

	public static final String   CM_NAME          = CmThreads.class.getSimpleName();
	public static final String   SHORT_NAME       = "Threads";
	public static final String   HTML_DESC        = 
		"<html>" +
		"Provides information on ASE threads" +
		"</html>";

	public static final String   GROUP_NAME       = MainFrame.TCP_GROUP_SERVER;
	public static final String   GUI_ICON_FILE    = "images/"+CM_NAME+".png";

//	public static final long     NEED_SRV_VERSION = 15700;
//	public static final long     NEED_SRV_VERSION = 1570000;
	public static final long     NEED_SRV_VERSION = Ver.ver(15,7);
	public static final long     NEED_CE_VERSION  = 0;

	public static final String[] MON_TABLES       = new String[] {"monThread"};
	public static final String[] NEED_ROLES       = new String[] {"mon_role"};
	public static final String[] NEED_CONFIG      = new String[] {};

	public static final String[] PCT_COLUMNS      = new String[] {"IdleTicksPct", "SleepTicksPct", "BusyTicksPct"};
	public static final String[] DIFF_COLUMNS     = new String[] {
		"TaskRuns", "TotalTicks", "IdleTicks", "SleepTicks", "BusyTicks", 
		"UserTime", "SystemTime", 
		"MinorFaults", "MajorFaults", 
		"VoluntaryCtxtSwitches", "NonVoluntaryCtxtSwitches"};

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

		return new CmThreads(counterController, guiController);
	}

	/**
	 * Constructor
	 */
	public CmThreads(ICounterController counterController, IGuiController guiController)
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

	public static final String   GRAPH_NAME_BUSY_AVG    = "busyAvg";
	public static final String   GRAPH_NAME_BUSY_THREAD = "busyThread";
	public static final String   GRAPH_NAME_TASKS       = "tasks";

	private void addTrendGraphs()
	{
//////		String[] sumLabels    = new String[] { "BusyTicksPct", "SleepTicksPct" };
////		String[] sumLabels    = new String[] { "-runtime-replaced-" };
////		String[] threadLabels = new String[] { "-runtime-replaced-" };
//		String[] sumLabels    = TrendGraphDataPoint.RUNTIME_REPLACED_LABELS;
//		String[] threadLabels = TrendGraphDataPoint.RUNTIME_REPLACED_LABELS;
//		String[] tasksLabels  = TrendGraphDataPoint.RUNTIME_REPLACED_LABELS;
//
//		addTrendGraphData(GRAPH_NAME_BUSY_AVG,    new TrendGraphDataPoint(GRAPH_NAME_BUSY_AVG,    sumLabels,    LabelType.Dynamic));
//		addTrendGraphData(GRAPH_NAME_BUSY_THREAD, new TrendGraphDataPoint(GRAPH_NAME_BUSY_THREAD, threadLabels, LabelType.Dynamic));
//		addTrendGraphData(GRAPH_NAME_TASKS,       new TrendGraphDataPoint(GRAPH_NAME_TASKS,       tasksLabels,  LabelType.Dynamic));

		addTrendGraph(GRAPH_NAME_BUSY_AVG,
			"CPU Thread BusyTicksPct Average per Pool Type",                  // Menu CheckBox text
			"CPU Thread BusyTicksPct Average per Pool Type ("+GROUP_NAME+"->"+SHORT_NAME+")", // Label 
			TrendGraphDataPoint.Y_AXIS_SCALE_LABELS_PERCENT,
			null,
			LabelType.Dynamic,
			TrendGraphDataPoint.Category.CPU,
			true,  // is Percent Graph
			true,  // visible at start
			Ver.ver(15,7), // graph is valid from Server Version. 0 = All Versions; >0 = Valid from this version and above 
			-1);   // minimum height

		addTrendGraph(GRAPH_NAME_BUSY_THREAD,
			"CPU Thread BusyTicksPct Usage per Thread",                  // Menu CheckBox text
			"CPU Thread BusyTicksPct Usage per Thread ("+GROUP_NAME+"->"+SHORT_NAME+")", // Label 
			TrendGraphDataPoint.Y_AXIS_SCALE_LABELS_PERCENT,
			null,
			LabelType.Dynamic,
			TrendGraphDataPoint.Category.CPU,
			true,  // is Percent Graph
			true,  // visible at start
			Ver.ver(15,7), // graph is valid from Server Version. 0 = All Versions; >0 = Valid from this version and above 
			-1);   // minimum height

		addTrendGraph(GRAPH_NAME_TASKS,
			"Tasks Executed per Thread",                  // Menu CheckBox text
			"Tasks Executed per Thread ("+GROUP_NAME+"->"+SHORT_NAME+")", // Label 
			TrendGraphDataPoint.Y_AXIS_SCALE_LABELS_NORMAL,
			null,
			LabelType.Dynamic,
			TrendGraphDataPoint.Category.CPU,
			false, // is Percent Graph
			false, // visible at start
			Ver.ver(15,7), // graph is valid from Server Version. 0 = All Versions; >0 = Valid from this version and above 
			-1);   // minimum height

//		// if GUI
//		if (getGuiController() != null && getGuiController().hasGUI())
//		{
//			// GRAPH
//			TrendGraph tg = null;
//			tg = new TrendGraph(GRAPH_NAME_BUSY_AVG,
//					"CPU Thread BusyTicksPct Average per Pool Type",                  // Menu CheckBox text
//					"CPU Thread BusyTicksPct Average per Pool Type ("+GROUP_NAME+"->"+SHORT_NAME+")", // Label 
//					sumLabels, 
//					true,  // is Percent Graph
//					this, 
//					true,  // visible at start
//					15700, // graph is valid from Server Version. 0 = All Versions; >0 = Valid from this version and above 
//					-1);   // minimum height
//			addTrendGraph(tg.getName(), tg, true);
//
//			tg = new TrendGraph(GRAPH_NAME_BUSY_THREAD,
//					"CPU Thread BusyTicksPct Usage per Thread",                  // Menu CheckBox text
//					"CPU Thread BusyTicksPct Usage per Thread ("+GROUP_NAME+"->"+SHORT_NAME+")", // Label 
//					threadLabels, 
//					true,  // is Percent Graph
//					this, 
//					true,  // visible at start
//					15700, // graph is valid from Server Version. 0 = All Versions; >0 = Valid from this version and above 
//					-1);   // minimum height
//			addTrendGraph(tg.getName(), tg, true);
//
//			tg = new TrendGraph(GRAPH_NAME_TASKS,
//					"Tasks Executed per Thread",                  // Menu CheckBox text
//					"Tasks Executed per Thread ("+GROUP_NAME+"->"+SHORT_NAME+")", // Label 
//					tasksLabels, 
//					false, // is Percent Graph
//					this, 
//					false, // visible at start
//					15700, // graph is valid from Server Version. 0 = All Versions; >0 = Valid from this version and above 
//					-1);   // minimum height
//			addTrendGraph(tg.getName(), tg, true);
//		}
	}
	
	@Override
	public String[] getDependsOnConfigForVersion(DbxConnection conn, long srvVersion, boolean isClusterEnabled)
	{
		return NEED_CONFIG;
	}

	@Override
	public List<String> getPkForVersion(DbxConnection conn, long srvVersion, boolean isClusterEnabled)
	{
		List <String> pkCols = new LinkedList<String>();

		pkCols.add("InstanceID");
		pkCols.add("ThreadID");

		return pkCols;
	}

	@Override
	public String getSqlForVersion(DbxConnection conn, long srvVersion, boolean isClusterEnabled)
	{
		addDropTempTable("#tmpCmThreads");

		String preDropTmpTables =
			"\n " +
			"/*------ drop tempdb objects if we failed doing that in previous execution -------*/ \n" +
			"if ((select object_id('#tmpCmThreads')) IS NOT NULL) drop table #tmpCmThreads \n" +
			"go \n" +
			"\n";

//		String sql = "select * from master..monThread";
//		String sql = 
//			"select \n" +
//			"     InstanceID, ThreadPoolID, ThreadPoolName, State, ThreadID, \n" +
//			"     TaskRuns, TotalTicks, IdleTicks, SleepTicks, BusyTicks, \n" +
//			"     IdleTicksPct  = convert(numeric(10,1), ((IdleTicks +0.0)/(TotalTicks+0.0)) * 100.0), \n" +
//			"     SleepTicksPct = convert(numeric(10,1), ((SleepTicks+0.0)/(TotalTicks+0.0)) * 100.0), \n" +
//			"     BusyTicksPct  = convert(numeric(10,1), ((BusyTicks +0.0)/(TotalTicks+0.0)) * 100.0), \n" +
//			"     UserTime, SystemTime, \n" +
//			"     KTID, OSThreadID, AltOSThreadID, ThreadAffinity, \n" +
//			"     MinorFaults, MajorFaults, \n" +
//			"     VoluntaryCtxtSwitches, NonVoluntaryCtxtSwitches \n" +
//			"from master..monThread \n" +
//			"";
		String sql = preDropTmpTables +
			"select \n" +
			"     th.InstanceID, th.ThreadPoolID, th.ThreadPoolName, \n" +
			"     ThreadType = CASE WHEN ta.Name like 'KPID %' THEN convert(varchar(30),'Engine') \n" +
			"                       WHEN ta.Name IS NULL       THEN convert(varchar(30),'Engine') \n" +
			"                       ELSE ta.Name \n" +
			"                  END, \n" +
			"     th.State, th.ThreadID, \n" +
			"     th.TaskRuns, th.TotalTicks, th.IdleTicks, th.SleepTicks, th.BusyTicks, \n" +
			"     IdleTicksPct  = convert(numeric(10,1), ((th.IdleTicks +0.0)/(th.TotalTicks+0.0)) * 100.0), \n" +
			"     SleepTicksPct = convert(numeric(10,1), ((th.SleepTicks+0.0)/(th.TotalTicks+0.0)) * 100.0), \n" +
			"     BusyTicksPct  = convert(numeric(10,1), ((th.BusyTicks +0.0)/(th.TotalTicks+0.0)) * 100.0), \n" +
			"     th.UserTime, th.SystemTime, \n" +
			"     th.KTID, th.OSThreadID, th.AltOSThreadID, th.ThreadAffinity, \n" +
			"     th.MinorFaults, th.MajorFaults, \n" +
			"     th.VoluntaryCtxtSwitches, th.NonVoluntaryCtxtSwitches, \n" +
			"     ThreadPoolNameType = th.ThreadPoolName + ':' \n" +
			"                        + CASE WHEN ta.Name like 'KPID %' THEN convert(varchar(30),'Engine') \n" +
			"                               WHEN ta.Name IS NULL       THEN convert(varchar(30),'Engine') \n" +
			"                               ELSE ta.Name \n" +
			"                          END, \n" +
			"     ThreadPoolNameTypeNum = convert(int, 0) \n" +
			"into #tmpCmThreads \n" +
			"from master..monThread th, master..monTask ta \n" +
			"where th.InstanceID *= ta.InstanceID \n" +
			"  and th.ThreadID   *= ta.ThreadID \n" +
			"  and th.KTID       *= ta.KTID \n" +
			"order by th.ThreadPoolID, th.ThreadID \n" +
			"     \n" +
			"update #tmpCmThreads \n" +
			"set ThreadPoolNameTypeNum = isnull((select #tmpCmThreads.ThreadID - min(TMP.ThreadID) + 1 \n" +
			"                                      from #tmpCmThreads TMP \n" +
			"                                     where #tmpCmThreads.ThreadPoolNameType  = TMP.ThreadPoolNameType  \n" +
			"                                   ), #tmpCmThreads.ThreadID)  \n" +
			"    \n" +
			"select * from #tmpCmThreads \n" +
			"drop table #tmpCmThreads \n" +
			"";
			
		return sql;
	}

	@Override
	public void updateGraphData(TrendGraphDataPoint tgdp)
	{
//		if (getServerVersion() < 15700)
//		if (getServerVersion() < 1570000)
		if (getServerVersion() < Ver.ver(15,7))
		{
			// disable the graph checkbox...
			TrendGraph tg = getTrendGraph(tgdp.getName());
			if (tg != null)
			{
				JCheckBoxMenuItem menuItem = tg.getViewMenuItem();
				if (menuItem.isSelected())
					menuItem.doClick();
			}
		}

		if (GRAPH_NAME_BUSY_AVG.equals(tgdp.getName()))
		{
			// Get distinct pool name types
//			ArrayList<String> poolNameTypes = new ArrayList<String>();
//			int pos_poolName = findColumn("ThreadPoolNameType");
//			for (int r=0; r< this.size(); r++)
//			{
//				String poolNameType = (String) getValueAt(r, pos_poolName);
//				if ( ! poolNameTypes.contains(poolNameType))
//					poolNameTypes.add(poolNameType);
//			}
//			
//			// Write 1 "line (busy)" for every distinct poolName
//			Double[] dArray = new Double[poolNameTypes.size()];
//			String[] lArray = new String[dArray.length];
//			for (int i=0; i< poolNameTypes.size(); i++)
//			{
//				// get Average BusyTicksPct per poolName
//				String poolName = poolNameTypes.get(i);
//				int[] pkRows = this.getAbsRowIdsWhere("ThreadPoolNameType", poolName);
//				Double BusyTicksPct  = this.getDiffValueAvg(pkRows, "BusyTicksPct");
//				//Double SleepTicksPct = this.getDiffValueAvg(pkRows, "SleepTicksPct");
//
//				lArray[i] = poolName;
//				dArray[i] = BusyTicksPct;
//			}
//			// Set the values
//			tgdp.setDate(this.getTimestamp());
//			tgdp.setLabel(lArray);
//			tgdp.setData(dArray);

			LinkedHashMap<String, Integer> poolNameTypes = new LinkedHashMap<String, Integer>();
			int pos_poolName = findColumn("ThreadPoolNameType");
			for (int r=0; r< this.size(); r++)
			{
				String poolNameType = (String) getValueAt(r, pos_poolName);
				Integer count = poolNameTypes.get(poolNameType);
				if (count == null)
					poolNameTypes.put(poolNameType, 1);
				else
					poolNameTypes.put(poolNameType, count.intValue() + 1);
			}

			// Write 1 "line (busy)" for every distinct poolName
			Double[] dArray = new Double[poolNameTypes.size()];
			String[] lArray = new String[dArray.length];
			int i = 0;
			for (String poolName : poolNameTypes.keySet())
			{
				Integer count = poolNameTypes.get(poolName);

				int[] pkRows = this.getAbsRowIdsWhere("ThreadPoolNameType", poolName);
				Double BusyTicksPct  = this.getDiffValueAvg(pkRows, "BusyTicksPct");
				//Double SleepTicksPct = this.getDiffValueAvg(pkRows, "SleepTicksPct");

				lArray[i] = poolName + "(" + count + ")";
				dArray[i] = BusyTicksPct;
				i++;
			}
			// Set the values
			tgdp.setDataPoint(this.getTimestamp(), lArray, dArray);
		}

		if (GRAPH_NAME_BUSY_THREAD.equals(tgdp.getName()))
		{
			boolean useShortNames = this.size() > 16;
				
			// Write 1 "line" for every row
			Double[] dArray = new Double[this.size()];
			String[] lArray = new String[dArray.length];
			for (int i = 0; i < dArray.length; i++)
			{
				String ThreadPoolNameType    = this.getRateString  (i, "ThreadPoolNameType");
//				String ThreadID              = this.getRateString  (i, "ThreadID");
				String ThreadPoolNameTypeNum = this.getRateString  (i, "ThreadPoolNameTypeNum");
				String InstanceID            = this.getRateString  (i, "InstanceID");

				String labelName;
				if      (ThreadPoolNameType.startsWith("syb_default_pool:"))               labelName = useShortNames ? "E" : "Engine";
				else if (ThreadPoolNameType.startsWith("syb_system_pool:DiskController" )) labelName = "DiskCtrl";
				else if (ThreadPoolNameType.startsWith("syb_system_pool:NetController" ))  labelName = "NetCtrl";
				else if (ThreadPoolNameType.startsWith("syb_system_pool:sybperf helper" )) labelName = "WinPerfmonHelper";
				else if (ThreadPoolNameType.startsWith("syb_blocking_pool:"))              labelName = "BlockingPool";
				else labelName = ThreadPoolNameType;
				
				if (isClusterEnabled())
					labelName = InstanceID + ":" + labelName + "-" + ThreadPoolNameTypeNum;
				else
					labelName =                    labelName + "-" + ThreadPoolNameTypeNum;

				lArray[i] = labelName;
				dArray[i] = this.getRateValueAsDouble(i, "BusyTicksPct");
			}

			// Set the values
			tgdp.setDataPoint(this.getTimestamp(), lArray, dArray);
		}

		if (GRAPH_NAME_TASKS.equals(tgdp.getName()))
		{
			boolean useShortNames = this.size() > 32;
			
			if ( findColumn("TaskRuns") == -1 )
				return;

			// Get engine count
			int engineCount = 0;
			for (int i = 0; i < this.size(); i++)
			{
				String ThreadType = this.getRateString(i, "ThreadType");

				if ( ! "Engine".equals(ThreadType) )
					continue;
				
				engineCount++;
			}
			
			// Write 1 "line" for every row, and a SUMMARY Line
			Double[] dArray = new Double[engineCount + 1]; // + 1 for summary line
			String[] lArray = new String[dArray.length];
			for (int i = 1; i < dArray.length; i++) // leave pos 0 for summary
			{
				String ThreadType            = this.getRateString  (i-1, "ThreadType");            // -1 = adjust for the rownum
				String ThreadPoolNameTypeNum = this.getRateString  (i-1, "ThreadPoolNameTypeNum"); // -1 = adjust for the rownum
				String InstanceID            = this.getRateString  (i-1, "InstanceID");            // -1 = adjust for the rownum

				if ( ! "Engine".equals(ThreadType) )
					continue;
				
				String labelName = useShortNames ? "E" : "Engine";;
				
				if (isClusterEnabled())
					labelName = InstanceID + ":" + labelName + "-" + ThreadPoolNameTypeNum;
				else
					labelName =                    labelName + "-" + ThreadPoolNameTypeNum;

				lArray[i] = labelName;
				dArray[i] = this.getRateValueAsDouble(i, "TaskRuns");
			}

			// Calculate the summary based on dArray[1..num]
			double sumAllEngines = 0;
			for (int i = 1; i < dArray.length; i++) // leave pos 0 for summary
				sumAllEngines += dArray[i];
			
			lArray[0] = "All-Engines";
			dArray[0] = sumAllEngines;

			// Set the values
			tgdp.setDataPoint(this.getTimestamp(), lArray, dArray);
		}
	}

	/** 
	 * Compute the avgServ column, which is IOTime/(Reads+Writes)
	 */
	@Override
	public void localCalculation(CounterSample prevSample, CounterSample newSample, CounterSample diffData)
	{
		int     TotalTicks,        IdleTicks,           SleepTicks,           BusyTicks;
		int pos_TotalTicks=-1, pos_IdleTicks=-1,    pos_SleepTicks=-1,    pos_BusyTicks=-1;
		int                    pos_IdleTicksPct=-1, pos_SleepTicksPct=-1, pos_BusyTicksPct=-1;
		
		// Find column Id's
		List<String> colNames = diffData.getColNames();
		if (colNames == null)
			return;
		for (int colId = 0; colId < colNames.size(); colId++)
		{
			String colName = (String) colNames.get(colId);
			if      (colName.equals("TotalTicks"))    pos_TotalTicks    = colId;
			else if (colName.equals("IdleTicks"))     pos_IdleTicks     = colId;
			else if (colName.equals("IdleTicksPct"))  pos_IdleTicksPct  = colId;
			else if (colName.equals("SleepTicks"))    pos_SleepTicks    = colId;
			else if (colName.equals("SleepTicksPct")) pos_SleepTicksPct = colId;
			else if (colName.equals("BusyTicks"))     pos_BusyTicks     = colId;
			else if (colName.equals("BusyTicksPct"))  pos_BusyTicksPct  = colId;
		}

		// Loop on all diffData rows
		for (int rowId = 0; rowId < diffData.getRowCount(); rowId++)
		{
			TotalTicks = ((Number) diffData.getValueAt(rowId, pos_TotalTicks)).intValue();
			IdleTicks  = ((Number) diffData.getValueAt(rowId, pos_IdleTicks)) .intValue();
			SleepTicks = ((Number) diffData.getValueAt(rowId, pos_SleepTicks)).intValue();
			BusyTicks  = ((Number) diffData.getValueAt(rowId, pos_BusyTicks)) .intValue();


			//--------------------
			//---- IdleTicksPct
			if (TotalTicks > 0)
			{
				double calc = (IdleTicks + 0.0) / (TotalTicks + 0.0) * 100.0;

				BigDecimal newVal = new BigDecimal(calc).setScale(1, BigDecimal.ROUND_HALF_EVEN);
				diffData.setValueAt(newVal, rowId, pos_IdleTicksPct);
			}
			else
				diffData.setValueAt(new BigDecimal(0), rowId, pos_IdleTicksPct);

			//--------------------
			//---- SleepTicksPct
			if (TotalTicks > 0)
			{
				double calc = (SleepTicks + 0.0) / (TotalTicks + 0.0) * 100.0;

				BigDecimal newVal = new BigDecimal(calc).setScale(1, BigDecimal.ROUND_HALF_EVEN);
				diffData.setValueAt(newVal, rowId, pos_SleepTicksPct);
			}
			else
				diffData.setValueAt(new BigDecimal(0), rowId, pos_SleepTicksPct);

			//--------------------
			//---- BusyTicksPct
			if (TotalTicks > 0)
			{
				double calc = (BusyTicks + 0.0) / (TotalTicks + 0.0) * 100.0;

				BigDecimal newVal = new BigDecimal(calc).setScale(1, BigDecimal.ROUND_HALF_EVEN);
				diffData.setValueAt(newVal, rowId, pos_BusyTicksPct);
			}
			else
				diffData.setValueAt(new BigDecimal(0), rowId, pos_BusyTicksPct);
		}
	}
}
