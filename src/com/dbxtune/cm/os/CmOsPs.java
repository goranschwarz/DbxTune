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
package com.dbxtune.cm.os;

import java.awt.event.MouseEvent;
import java.lang.invoke.MethodHandles;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map.Entry;

import javax.swing.table.TableModel;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.dbxtune.CounterController;
import com.dbxtune.ICounterController;
import com.dbxtune.IGuiController;
import com.dbxtune.central.pcs.CentralPersistReader;
import com.dbxtune.cm.CmSettingsHelper;
import com.dbxtune.cm.CounterModelHostMonitor;
import com.dbxtune.cm.CounterSetTemplates;
import com.dbxtune.cm.CounterSetTemplates.Type;
import com.dbxtune.cm.CountersModel;
import com.dbxtune.cm.os.gui.CmOsPsPanel;
import com.dbxtune.graph.TrendGraphDataPoint;
import com.dbxtune.graph.TrendGraphDataPoint.LabelType;
import com.dbxtune.gui.MainFrame;
import com.dbxtune.gui.TabularCntrPanel;
import com.dbxtune.gui.swing.TableModelSortable;
import com.dbxtune.hostmon.HostMonitor.OsVendor;
import com.dbxtune.hostmon.OsTable;
import com.dbxtune.utils.Configuration;
import com.dbxtune.utils.StringUtil;
import com.dbxtune.utils.SwingUtils;
import com.dbxtune.utils.TimeUtils;

public class CmOsPs
extends CounterModelHostMonitor
{
	private static final Logger _logger = LogManager.getLogger(MethodHandles.lookup().lookupClass());
	private static final long    serialVersionUID = 1L;

	public static final int      CM_TYPE          = CounterModelHostMonitor.HOSTMON_PS;
	public static final String   CM_NAME          = CmOsPs.class.getSimpleName();
	public static final String   SHORT_NAME       = "OS Top Process(ps)";
	public static final String   HTML_DESC        = 
		"<html>" +
		"Executes: 'ps ... | top ##' on the Operating System" +
		"</html>";

	public static final String   GROUP_NAME       = MainFrame.TCP_GROUP_HOST_MONITOR;
	public static final String   GUI_ICON_FILE    = "images/"+CM_NAME+".png";

	public static final boolean  NEGATIVE_DIFF_COUNTERS_TO_ZERO = false;
	public static final boolean  IS_SYSTEM_CM                   = true;
	public static final int      DEFAULT_POSTPONE_TIME          = 0;

	@Override public int     getDefaultPostponeTime()                 { return DEFAULT_POSTPONE_TIME; }
//	@Override public int     getDefaultQueryTimeout()                 { return DEFAULT_QUERY_TIMEOUT; }
	@Override public boolean getDefaultIsNegativeDiffCountersToZero() { return NEGATIVE_DIFF_COUNTERS_TO_ZERO; }
	@Override public Type    getTemplateLevel()                       { return Type.LARGE; }

	/**
	 * FACTORY  method to create the object
	 */
	public static CountersModel create(ICounterController counterController, IGuiController guiController)
	{
		if (guiController != null && guiController.hasGUI())
			guiController.splashWindowProgress("Loading: Counter Model '"+CM_NAME+"'");

		return new CmOsPs(counterController, guiController);
	}

	public CmOsPs(ICounterController counterController, IGuiController guiController)
	{
		super(CM_NAME, GROUP_NAME, CM_TYPE, null, NEGATIVE_DIFF_COUNTERS_TO_ZERO, IS_SYSTEM_CM, DEFAULT_POSTPONE_TIME);

		setDisplayName(SHORT_NAME);
		setDescription(HTML_DESC);

		setIconFile(GUI_ICON_FILE);

		setCounterController(counterController);
		setGuiController(guiController);
		
		// Normally for HostMonitor is ABS
//		setDataSource(DATA_RATE, false);
		setDataSource(DATA_ABS, false);
		
		addTrendGraphs();
		
		CounterSetTemplates.register(this);
	}

	public static final String  PROPKEY_top = "MonitorPs.top";
	public static final int     DEFAULT_top = -1; // ALL
	
	/** Used by the: Create 'Offline Session' Wizard */
	@Override
	public List<CmSettingsHelper> getLocalSettings()
	{
		Configuration conf = Configuration.getCombinedConfiguration();
		List<CmSettingsHelper> list = new ArrayList<>();
		
		list.add(new CmSettingsHelper("Top Rows", PROPKEY_top, Integer.class, conf.getIntProperty(PROPKEY_top, DEFAULT_top), DEFAULT_top, "Number of top rows."));

		return list;
	}


	@Override
	protected TabularCntrPanel createGui()
	{
		return new CmOsPsPanel(this);
	}

	//------------------------------------------------------------
	// Implementation
	//------------------------------------------------------------
	public static final String GRAPH_NAME_WIN_PS      = "WinPs";

	private void addTrendGraphs()
	{
		// GRAPH
		addTrendGraph(GRAPH_NAME_WIN_PS,
			"ps: Windows Process CPU Usage in Seconds", 	                                // Menu CheckBox text
			"ps: Windows Process CPU Usage in Seconds ("+GROUP_NAME+"->"+SHORT_NAME+")",    // Label 
			TrendGraphDataPoint.createGraphProps(TrendGraphDataPoint.Y_AXIS_SCALE_LABELS_SECONDS, CentralPersistReader.SampleType.MAX_OVER_SAMPLES),
			null, 
			LabelType.Dynamic,
			TrendGraphDataPoint.Category.CPU,
			false, // is Percent Graph
			false, // visible at start
			0,     // graph is valid from Server Version. 0 = All Versions; >0 = Valid from this version and above 
			-1);   // minimum height

	}

////--OK--//TODO; // DSR -- JobScheduler: The Tooltip in the "DbxTune" also had some issues... 
//TODO; // Convert the WindowsPs to a CSV Reader and use .... Get-Process | Where-Object { $_.CPU -ne $null } | Sort-Object CPU -Descending | Select-Object Id, SessionId, StartTime, ProcessName, CPU, HandleCount, Threads, BasePriority, PriorityClass, ProcessorAffinity, MaxWorkingSet, MinWorkingSet, NonpagedSystemMemorySize64, PagedMemorySize64, PagedSystemMemorySize64, PeakPagedMemorySize64, PeakVirtualMemorySize64, PeakWorkingSet64, PrivateMemorySize64, VirtualMemorySize64, WorkingSet64, PrivilegedProcessorTime, TotalProcessorTime, UserProcessorTime, Company, FileVersion, Path, Product, ProductVersion | select -first 10 | ConvertTo-Csv -NoTypeInformation
	
	
	@Override
	public void updateGraphData(TrendGraphDataPoint tgdp)
	{
		if (GRAPH_NAME_WIN_PS.equals(tgdp.getName()))
		{
			if (isConnectedToVendor(OsVendor.Windows))
			{
				//          procName->cpuSec
				LinkedHashMap<String, Double> procToCounterMap = new LinkedHashMap<>();

				// Summarize: CpuSeconds for each ProcessName... 
				// DO NOT add PID/id (due to the Graph in DbxCentral will then contain to many columns due to that PID will change all the time...)
				double sumCpuSec = 0.0;
				for (int r = 0; r < this.getRateRowCount(); r++)
				{
					// Do NOT include "first time" the PK is sampled (this could also happen if PID is not-included in Get-Process and the on next sample it is)
					// Also in any DSR Reports we need to filter out those records... (_sql_and_skipNewOrDiffRateRows)
					if (this.isNewDeltaOrRateRow(r))
						continue;

					String procName = this.getRateString        (r, "ProcessName");
					Double cpuSec   = this.getRateValueAsDouble (r, "CPU(s)", true, 0d);

					if (cpuSec > 0.0)
					{
						// Get current value, if not found add it as a 0
						Double currentCounter = procToCounterMap.get(procName);
						if (currentCounter == null)
						{
							currentCounter = new Double(0.0);
							procToCounterMap.put(procName, currentCounter);
						}
						// Increment/Set value
						procToCounterMap.put(procName, currentCounter + cpuSec);
						
						sumCpuSec += cpuSec;
					}
				}

				if ( ! procToCounterMap.isEmpty() )
				{
					// Write 1 "line" for every ProcName
					Double[] dArray = new Double[ 1 + procToCounterMap.size() ];
					String[] lArray = new String[ 1 + procToCounterMap.size() ];

					lArray[0] = "_Total";
					dArray[0] = sumCpuSec;

					int i = 0;
					for (Entry<String, Double> entry : procToCounterMap.entrySet())
					{
						lArray[i+1] = entry.getKey();
						dArray[i+1] = entry.getValue();
						
						i++;
					}

					// Set the values
					tgdp.setDataPoint(this.getTimestamp(), lArray, dArray);
				}
			}

//			if (isConnectedToVendor(OsVendor.Windows))
//			{
//				List<String> labelList = new ArrayList<>();
//				List<Double> dataList  = new ArrayList<>();
//
//				double sumCpuSec = 0.0;
//				for (int r = 0; r < this.getRateRowCount(); r++)
//				{
//					String procName = this.getRateString        (r, "ProcessName");
//					int    procId   = this.getRateValueAsInteger(r, "Id"    , true, -1);
//					Double cpuSec   = this.getRateValueAsDouble (r, "CPU(s)", true, 0d);
//
//					if (cpuSec > 0.0)
//					{
//						labelList.add(procName + "[" + procId + "]");
//						dataList.add(cpuSec);
//						
//						sumCpuSec += cpuSec;
//					}
//				}
//
//				if ( ! labelList.isEmpty() )
//				{
//					// Write 1 "line" for every device
//					Double[] dArray = new Double[ 1 + labelList.size() ];
//					String[] lArray = new String[ 1 + labelList.size() ];
//
//					lArray[0] = "_Total";
//					dArray[0] = sumCpuSec;
//					
//					for (int i = 0; i < labelList.size(); i++)
//					{
//						lArray[i+1] = labelList.get(i);
//						dArray[i+1] = dataList .get(i);
//					}
//
//					// Set the values
//					tgdp.setDataPoint(this.getTimestamp(), lArray, dArray);
//				}
//			}
		} // end: graph

	} // end: method
	
	// TODO: Should we setup some "Longer inMem Storage" so we can see how much CPU time that has been spent in the last 10 minutes... (and possibly a SparkLine to indicate change)
	//       This would be a MovingAverage or "slidingWindow" for each PID  


	@Override
	public void localCalculation(OsTable thisSample)
	{
		// for WINDOWS
		if (isConnectedToVendor(OsVendor.Windows))
		{
			int pos_cpuSec      = thisSample.findColumn("CPU(s)");
			int pos_cpuTimeHms  = thisSample.findColumn("CpuTimeHms");
//			int pos_description = thisSample.findColumn("Description");
//			int pos_processName = thisSample.findColumn("ProcessName");

			// set 'CpuTimeHms'
			if (pos_cpuSec != -1 && pos_cpuTimeHms != -1)
			{
				for (int r = 0; r < thisSample.getRowCount(); r++)
				{
					double cpuSec = thisSample.getValueAsDouble(r, pos_cpuSec, 0d);

					// Calculate: [#d] HH:MM:SS.ms
					String cpuTimeHms = TimeUtils.msToTimeStrDHMSms( (long) (cpuSec * 1000) );

					thisSample.setValueAt(cpuTimeHms, r, pos_cpuTimeHms);
				}
			}

//			// set 'CpuTimeHms'
//			if (pos_description != -1 && pos_processName != -1)
//			{
//				for (int r = 0; r < thisSample.getRowCount(); r++)
//				{
//					String processName = thisSample.getValueAsString(r, pos_processName);
//					String description = getWindowsProcessDescription(processName);
//
//					thisSample.setValueAt(description, r, pos_description);
//				}
//			}
		}
		// LINUX
		else if (isConnectedToVendor(OsVendor.Linux))
		{
			// For the moment: do nothing
		}
	}

	/**
	 * Some kind of translation for server processes that can be view in a tool tip or similar
	 * @param processName
	 * @return
	 */
	public static String getWindowsProcessDescription(String processName)
	{
		if (processName == null)
			return "";

		// Remove ".exe"
		if (processName.endsWith(".exe"))
			processName = processName.substring(0, processName.length() - ".exe".length());

		if ("sqlservr"         .equals(processName)) return "SQL Server Process";
		if ("msmdsrv"          .equals(processName)) return "SQL Server Analysis Services Server";
		if ("MsMpEng"          .equals(processName)) return "Windows Defender -- Virus Scan Program (Make sure your DATA/LOG files are not scanned)";
		if ("coreServiceShell" .equals(processName)) return "Trend Micro Internet Security";
		if ("dsa"              .equals(processName)) return "directory system agent (DSA) is a collection of services and processes that run on each domain controller and provides access to the data store";
		if ("MsSense"          .equals(processName)) return "Windows Defender Advanced Threat Protection Service Executable";
		if ("typeperf"         .equals(processName)) return "Windows CmdLine tool to get Windows Counters, Used by DbxTune to collect OS Metrics.";
		if ("System"           .equals(processName)) return "Windows System.";

		return "";
	}

	@Override
	public String getToolTipTextOnTableCell(MouseEvent e, String colName, Object cellValue, int modelRow, int modelCol)
	{
		if ("ProcessName".equals(colName))
		{
			return getWindowsProcessDescription( (String)cellValue );
		}

		return super.getToolTipTextOnTableCell(e, colName, cellValue, modelRow, modelCol);
	}


//	@Override
//	public void localCalculation(OsTable newSample)
//	{
////		System.out.println("localCalculation(OsTable thisSample): newSample.getColumnCount()="+newSample.getColumnCount()+", "+newSample.getColNames());
//
//		int sizeKB_pos      = newSample.findColumn("Size-KB");
//		int usedKB_pos      = newSample.findColumn("Used-KB");
//		int availableKB_pos = newSample.findColumn("Available-KB");
//
//		int sizeMB_pos      = newSample.findColumn("Size-MB");
//		int usedMB_pos      = newSample.findColumn("Used-MB");
//		int availableMB_pos = newSample.findColumn("Available-MB");
//
//		int usedPct_pos     = newSample.findColumn("UsedPct");
//
//		if (sizeKB_pos == -1 || usedKB_pos == -1 || availableKB_pos == -1 || sizeMB_pos == -1 || usedMB_pos == -1 || availableMB_pos == -1 || usedPct_pos == -1)
//		{
//			_logger.warn("Column position not available. sizeKB_pos="+sizeKB_pos+", usedKB_pos="+usedKB_pos+", availableKB_pos="+availableKB_pos+", sizeMB_pos="+sizeMB_pos+", usedMB_pos="+usedMB_pos+", availableMB_pos="+availableMB_pos+", usedPct_pos="+usedPct_pos+".");
//			return;
//		}
//		
//		for (int r=0; r<newSample.getRowCount(); r++)
//		{
//			Number sizeKB_num      = (Number) newSample.getValueAt(r, sizeKB_pos);
//			Number usedKB_num      = (Number) newSample.getValueAt(r, usedKB_pos);
//			Number availableKB_num = (Number) newSample.getValueAt(r, availableKB_pos);
//
//			if (sizeKB_num      != null) newSample.setValueAt(Integer.valueOf(sizeKB_num     .intValue()/1024), r, sizeMB_pos);
//			if (usedKB_num      != null) newSample.setValueAt(Integer.valueOf(usedKB_num     .intValue()/1024), r, usedMB_pos);
//			if (availableKB_num != null) newSample.setValueAt(Integer.valueOf(availableKB_num.intValue()/1024), r, availableMB_pos);
//
//			// Calculate the Pct value with a higher (scale=1) resolution than df
//			if (sizeKB_num != null && usedKB_num != null && availableKB_num != null)
//			{
//				if (sizeKB_num.intValue() > 0)
//				{
////					double pct = usedKB_num.doubleValue() / sizeKB_num.doubleValue() * 100.0;
//					double pct = 100.0 - (availableKB_num.doubleValue() / sizeKB_num.doubleValue() * 100.0);
//					if (pct <= 0)
//						pct = 0;
//					if (pct > 100)
//						pct = 100;
//
//					BigDecimal bd =  new BigDecimal( pct ).setScale(1, RoundingMode.HALF_EVEN);
//					newSample.setValueAt(bd, r, usedPct_pos);
//				}
//			}
//		}
//	}
	
	
	/**
	 * Get a HTML Table representation of the ABS/RATE values from CmOsPs
	 * 
	 * @param counterController 
	 * @param topRows                 Only the top X numer of rows (-1: if you want all rows) 
	 * 
	 * @return A HTML Table String (if CmOsPs is not found, then "" will be returned)
	 */
	public static String getCmOsPs_asHtmlTable(ICounterController counterController, int topRows)
	{
		String retStr = "";
		String ccWarn = "";
		
		if (counterController == null)
		{
			counterController = CounterController.getInstance();
			ccWarn = "WARNING: using Global Counter Controller [" + (counterController == null ? "-null-" : counterController.getClass().getSimpleName()) + "].<br>";
			_logger.warn("No CounterController was passed to 'getCmOsPs_asHtmlTable', grabbing the global CounterController instance, which might NOT be the correct.");
		}
		
		CountersModel cmOsPs = counterController.getCmByName(CmOsPs.CM_NAME);
		if (cmOsPs != null)
		{
			if (cmOsPs.hasAbsData())
			{
				int coreCount = CmOsMpstat.static_getCoreCount(counterController);

				retStr += "<b>ABS Top Processes: (" + cmOsPs.getAbsRowCount() + " records)</b>, coreCount=" + coreCount + "<br>";
				retStr += SwingUtils.tableToHtmlString(cmOsPs.getCounterDataAbs(), topRows) + "<br>";

				if (cmOsPs.hasRateData())
				{
					TableModel tm = cmOsPs.getCounterDataRate();
					
					// SORT the RATE before printing it
					// "CPU(s)" is on Windows, for *nix we need another column, but for the moment: we do NOT do RATE calculations on *nix systems
					if (cmOsPs.hasColumn("CPU(s)"))
					{
						TableModelSortable sortedRate = new TableModelSortable(cmOsPs.getCounterDataRate());
						sortedRate.sortByColumn("CPU(s)", false); // Sort in DESC order 
						tm = sortedRate;
					}
					
					retStr += "<b>RATE Top Processes: (" + tm.getRowCount() + " records)</b>, coreCount=" + coreCount + ", NOTE: 'CPU(s)' needs to be divided by 'coreCount'.<br>";
					retStr += SwingUtils.tableToHtmlString(tm, topRows) + "<br>";
				}
			}
			else
			{
				if (cmOsPs.isActive())
					retStr += "<b>Top Processes: (CmOsPs does NOT contain any data)</b><br>";
				else
					retStr += "<b>Top Processes: (CmOsPs is NOT enabled)</b><br>";
			}
		}
		
		// Add a newline prior to the output
		if (StringUtil.hasValue(retStr))
			retStr = "<br>" + ccWarn + retStr;
		
		return retStr;
	}

}
