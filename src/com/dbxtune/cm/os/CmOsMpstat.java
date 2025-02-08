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

import java.lang.invoke.MethodHandles;
import java.util.ArrayList;
import java.util.List;

import org.apache.commons.lang3.ArrayUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.dbxtune.CounterController;
import com.dbxtune.ICounterController;
import com.dbxtune.IGuiController;
import com.dbxtune.alarm.AlarmHandler;
import com.dbxtune.alarm.events.AlarmEvent;
import com.dbxtune.alarm.events.AlarmEventHighCpuUtilization;
import com.dbxtune.central.pcs.CentralPersistReader;
import com.dbxtune.cm.CmSettingsHelper;
import com.dbxtune.cm.CounterModelHostMonitor;
import com.dbxtune.cm.CounterSetTemplates;
import com.dbxtune.cm.CounterSetTemplates.Type;
import com.dbxtune.cm.CountersModel;
import com.dbxtune.cm.os.gui.CmOsMpstatPanel;
import com.dbxtune.graph.TrendGraphDataPoint;
import com.dbxtune.graph.TrendGraphDataPoint.LabelType;
import com.dbxtune.gui.MainFrame;
import com.dbxtune.gui.TabularCntrPanel;
import com.dbxtune.gui.TrendGraph;
import com.dbxtune.hostmon.HostMonitor.OsVendor;
import com.dbxtune.utils.Configuration;
import com.dbxtune.utils.StringUtil;

public class CmOsMpstat
extends CounterModelHostMonitor
{
	private static final Logger _logger = LogManager.getLogger(MethodHandles.lookup().lookupClass());
	private static final long    serialVersionUID = 1L;

	public static final int      CM_TYPE          = CounterModelHostMonitor.HOSTMON_MPSTAT;
	public static final String   CM_NAME          = CmOsMpstat.class.getSimpleName();
	public static final String   SHORT_NAME       = "OS CPU(mpstat)";
	public static final String   HTML_DESC        = 
		"<html>" +
		"Executes: 'mpstat' on the Operating System" +
		"</html>";

	public static final String   GROUP_NAME       = MainFrame.TCP_GROUP_HOST_MONITOR;
	public static final String   GUI_ICON_FILE    = "images/"+CM_NAME+".png";

	public static final boolean  NEGATIVE_DIFF_COUNTERS_TO_ZERO = true;
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

		return new CmOsMpstat(counterController, guiController);
	}

	public CmOsMpstat(ICounterController counterController, IGuiController guiController)
	{
		super(CM_NAME, GROUP_NAME, CM_TYPE, null, NEGATIVE_DIFF_COUNTERS_TO_ZERO, IS_SYSTEM_CM, DEFAULT_POSTPONE_TIME);

		setDisplayName(SHORT_NAME);
		setDescription(HTML_DESC);

		setIconFile(GUI_ICON_FILE);

		setCounterController(counterController);
		setGuiController(guiController);
		
		setDataSource(DATA_ABS, false);
		
		addTrendGraphs();
		
		CounterSetTemplates.register(this);
	}


	@Override
	protected TabularCntrPanel createGui()
	{
		return new CmOsMpstatPanel(this);
	}

	//------------------------------------------------------------
	// Implementation
	//------------------------------------------------------------
	public static final String GRAPH_NAME_MpSum  = "MpSum";
	public static final String GRAPH_NAME_MpCpu  = "MpCpu";

	private void addTrendGraphs()
	{
		// GRAPH
		addTrendGraph(GRAPH_NAME_MpSum,
			"mpstat: CPU usage Summary",                 // Menu CheckBox text
			"mpstat: CPU usage Summary ("+GROUP_NAME+"->"+SHORT_NAME+")",   // Label 
			TrendGraphDataPoint.createGraphProps(TrendGraphDataPoint.Y_AXIS_SCALE_LABELS_PERCENT, CentralPersistReader.SampleType.MAX_OVER_SAMPLES),
			null, 
			LabelType.Dynamic,
			TrendGraphDataPoint.Category.CPU,
			true,  // is Percent Graph
			false, // visible at start
			0,     // graph is valid from Server Version. 0 = All Versions; >0 = Valid from this version and above 
			-1);   // minimum height

		// GRAPH
		addTrendGraph(GRAPH_NAME_MpCpu,
			"mpstat: CPU usage per core",                 // Menu CheckBox text
			"mpstat: CPU usage per core (usr+sys+iowait) ("+GROUP_NAME+"->"+SHORT_NAME+")",   // Label 
			TrendGraphDataPoint.createGraphProps(TrendGraphDataPoint.Y_AXIS_SCALE_LABELS_PERCENT, CentralPersistReader.SampleType.MAX_OVER_SAMPLES),
			null, 
			LabelType.Dynamic,
			TrendGraphDataPoint.Category.CPU,
			true,  // is Percent Graph
			false, // visible at start
			0,     // graph is valid from Server Version. 0 = All Versions; >0 = Valid from this version and above 
			-1);   // minimum height
	} // end: method
	
	@Override
	public void updateGraphData(TrendGraphDataPoint tgdp)
	{
		TrendGraph tg = getTrendGraph(tgdp.getName());
//		if (tg == null)
//		{
//			_logger.warn("Trend graph with the name '"+tgdp.getName()+"' can't be found in the cm '"+this.getName()+"'.");
//			return;
//		}

		// GRAPH: WAIT TIME
		if (GRAPH_NAME_MpSum.equals(tgdp.getName()))
		{
			if (isConnectedToVendor(OsVendor.Windows))
			{
//				Double[] dArray = new Double[5];
//				String[] lArray = new String[5];
//
//				// Initialize the "sum" array
//				for (int i=0; i<dArray.length; i++)
//					dArray[i] = 0.0;
//
//				lArray[0] = "% Total (Processor + User + Privileged)"; // this is faulty??? ... I think "% Processor Time" is "% User Time + % Privileged Time"
//				lArray[1] = "% Processor Time";
//				lArray[2] = "% User Time";
//				lArray[3] = "% Privileged Time";
//				lArray[4] = "% Idle Time";
//
//				for (int i=0; i<this.getRowCount(); i++)
//				{
//					String cpu = this.getAbsString(i, "Instance");
//
//					if ("_Total".equalsIgnoreCase(cpu))
//					{
//						Double processorTime  = this.getAbsValueAsDouble(i, "% Processor Time");
//						Double userTime       = this.getAbsValueAsDouble(i, "% User Time");
//						Double privilegedTime = this.getAbsValueAsDouble(i, "% Privileged Time");
//						Double idleTime       = this.getAbsValueAsDouble(i, "% Idle Time");
//
//						dArray[0] = processorTime + userTime + privilegedTime;
//						dArray[1] = processorTime;
//						dArray[2] = userTime;
//						dArray[3] = privilegedTime;
//						dArray[4] = idleTime;
//					}
//				}
//
//				tgdp.setDataPoint(this.getTimestamp(), lArray, dArray);

				Double[] dArray = new Double[4];
				String[] lArray = new String[4];

				// Initialize the "sum" array
				for (int i=0; i<dArray.length; i++)
					dArray[i] = 0.0;

				lArray[0] = "% Processor Time";
				lArray[1] = "% User Time";
				lArray[2] = "% Privileged Time";
				lArray[3] = "% Idle Time";

				for (int i=0; i<this.getRowCount(); i++)
				{
					String cpu = this.getAbsString(i, "Instance");

					if ("_Total".equalsIgnoreCase(cpu))
					{
						Double processorTime  = this.getAbsValueAsDouble(i, "% Processor Time");
						Double userTime       = this.getAbsValueAsDouble(i, "% User Time");
						Double privilegedTime = this.getAbsValueAsDouble(i, "% Privileged Time");
						Double idleTime       = this.getAbsValueAsDouble(i, "% Idle Time");

						dArray[0] = processorTime;
						dArray[1] = userTime;
						dArray[2] = privilegedTime;
						dArray[3] = idleTime;
					}
				}

				tgdp.setDataPoint(this.getTimestamp(), lArray, dArray);
			}
			else
			{
				int cpu_pos                          = this.findColumn("CPU"); // Linux, Solaris
				if (cpu_pos < 0)             cpu_pos = this.findColumn("cpu"); // AIX
				
				int usrPct_pos                       = this.findColumn("usrPct"); // Linix
				if (usrPct_pos < 0)       usrPct_pos = this.findColumn("usr");    // Solaris
				if (usrPct_pos < 0)       usrPct_pos = this.findColumn("us");     // AIX

				int sysPct_pos                       = this.findColumn("sysPct"); // Linix
				if (sysPct_pos < 0)       sysPct_pos = this.findColumn("sys");    // Solaris
				if (sysPct_pos < 0)       sysPct_pos = this.findColumn("sy");     // AIX

				int iowaitPct_pos                    = this.findColumn("iowaitPct"); // Linix
				if (iowaitPct_pos < 0) iowaitPct_pos = this.findColumn("wt");        // Solaris
				if (iowaitPct_pos < 0) iowaitPct_pos = this.findColumn("wt");        // AIX

				int idlePct_pos                      = this.findColumn("idlePct"); // Linix
				if (idlePct_pos < 0)     idlePct_pos = this.findColumn("idl");     // Solaris
				if (idlePct_pos < 0)     idlePct_pos = this.findColumn("id");      // AIX

				if (cpu_pos < 0 || usrPct_pos < 0 || sysPct_pos < 0 || iowaitPct_pos < 0 || idlePct_pos < 0)
				{
					String msg = "";
					if (cpu_pos        < 0) msg += "'CPU', ";
					if (usrPct_pos     < 0) msg += "'usrPct|usr|us', ";
					if (sysPct_pos     < 0) msg += "'sysPct|sys|sy', ";
					if (iowaitPct_pos  < 0) msg += "'iowaitPct|wt', ";
					if (idlePct_pos    < 0) msg += "'idlePct|idl|id', ";

					if (tg != null)
						tg.setWarningLabel("Column(s) "+StringUtil.removeLastComma(msg)+" can't be found. This graph is only supported on Linux/Solaris/AIX systems");
				}
				else
				{
					boolean gotAllEntry = false;

//					Double[] dArray = new Double[this.getRowCount()];
					Double[] aArray = new Double[5]; // "all" array (for values that do have the 'all' record)
					Double[] sArray = new Double[5]; // "sum" array (for values that do NOT have the 'all' record)
					String[] lArray = new String[5];

					// Initialize the "sum" array
					for (int i=0; i<sArray.length; i++)
						sArray[i] = 0.0;

					lArray[0] = "TotalPct (usr + sys + iowait)";
					lArray[1] = "usrPct";
					lArray[2] = "sysPct";
					lArray[3] = "iowaitPct";
					lArray[4] = "idlePct";

					for (int i=0; i<this.getRowCount(); i++)
					{
						String cpu = this.getAbsString(i, cpu_pos);

						if ("all".equalsIgnoreCase(cpu))
						{
							gotAllEntry = true;
							
							Double usrPct    = this.getAbsValueAsDouble(i, usrPct_pos);
							Double sysPct    = this.getAbsValueAsDouble(i, sysPct_pos);
							Double iowaitPct = this.getAbsValueAsDouble(i, iowaitPct_pos);
							Double idlePct   = this.getAbsValueAsDouble(i, idlePct_pos);

							aArray[0] = usrPct + sysPct + iowaitPct;
							aArray[1] = usrPct;
							aArray[2] = sysPct;
							aArray[3] = iowaitPct;
							aArray[4] = idlePct;
						}
						else
						{
							Double usrPct    = this.getAbsValueAsDouble(i, usrPct_pos);
							Double sysPct    = this.getAbsValueAsDouble(i, sysPct_pos);
							Double iowaitPct = this.getAbsValueAsDouble(i, iowaitPct_pos);
							Double idlePct   = this.getAbsValueAsDouble(i, idlePct_pos);

							sArray[0] += usrPct + sysPct + iowaitPct;
							sArray[1] += usrPct;
							sArray[2] += sysPct;
							sArray[3] += iowaitPct;
							sArray[4] += idlePct;
						}
					}

					// Set the values
//					tgdp.setDate(this.getTimestamp());
//					tgdp.setLabel(lArray);
//					if (gotAllEntry)
//						// use the "all" entry
//						tgdp.setData(aArray);
//					else
//					{
//						// get average value from "sum" values / rowCount 
//						for (int i=0; i<sArray.length; i++)
//							sArray[i] = sArray[i] / this.getRowCount();
//						tgdp.setData(sArray);
//					}
					if (gotAllEntry)
					{
						// use the "all" entry
						tgdp.setDataPoint(this.getTimestamp(), lArray, aArray);
					}
					else
					{
						// get average value from "sum" values / rowCount 
						for (int i=0; i<sArray.length; i++)
							sArray[i] = sArray[i] / this.getRowCount();

						tgdp.setDataPoint(this.getTimestamp(), lArray, sArray);
					}

					if (tg != null)
						tg.setWarningLabel(null);
						//tg.setWarningLabel("Row 'all' was not found, This graph is only supported on Linux systems (or systems that has the 'all' entry)");
				}
			}
		} // end: graph

		// GRAPH: READ WAIT TIME
		if (GRAPH_NAME_MpCpu.equals(tgdp.getName()))
		{
			if (isConnectedToVendor(OsVendor.Windows))
			{
//				// Write 1 "line" for each CPU entry
//				Double[] dArray = new Double[this.getRowCount()];
//				String[] lArray = new String[dArray.length];
//				for (int i=0; i<dArray.length; i++)
//				{
//					String cpu = this.getAbsString(i, "Instance");
//
//					Double procPct   = this.getAbsValueAsDouble(i, "% Processor Time");
////					Double usrPct    = this.getAbsValueAsDouble(i, "% User Time");
////					Double sysPct    = this.getAbsValueAsDouble(i, "% Privileged Time");
////					Double idlePct   = this.getAbsValueAsDouble(i, "% Idle Time");
////					Double iowaitPct = this.getAbsValueAsDouble(i, iowaitPct_pos); // IO Wait is unknown in Windows
//
//					lArray[i] = "c-" + cpu;
//					dArray[i] = procPct;
//				}
//
//				// Set the values
//				tgdp.setDataPoint(this.getTimestamp(), lArray, dArray);
				
				// Write 1 "line" for each CPU entry
				Double[] dArray = new Double[this.getRowCount()];
				String[] lArray = new String[dArray.length];

				for (int i=0; i<dArray.length; i++)
				{
					String cpu = this.getAbsString(i, "Instance");

					Double procPct   = this.getAbsValueAsDouble(i, "% Processor Time");

					lArray[i] = cpu.equals("_Total") ? cpu : ("c-" + cpu);
					dArray[i] = procPct;
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
			else
			{
				int cpu_pos                          = this.findColumn("CPU");  // Linux, Solaris
				if (cpu_pos < 0)             cpu_pos = this.findColumn("cpu");  // AIX
				
				int usrPct_pos                       = this.findColumn("usrPct"); // Linux
				if (usrPct_pos < 0)       usrPct_pos = this.findColumn("usr");    // Solaris
				if (usrPct_pos < 0)       usrPct_pos = this.findColumn("us");     // AIX

				int sysPct_pos                       = this.findColumn("sysPct"); // Linux
				if (sysPct_pos < 0)       sysPct_pos = this.findColumn("sys");    // Solaris
				if (sysPct_pos < 0)       sysPct_pos = this.findColumn("sy");     // AIX

				int iowaitPct_pos                    = this.findColumn("iowaitPct"); // Linux
				if (iowaitPct_pos < 0) iowaitPct_pos = this.findColumn("wt");        // Solaris
				if (iowaitPct_pos < 0) iowaitPct_pos = this.findColumn("wt");        // AIX

				int idlePct_pos                      = this.findColumn("idlePct"); // Linux
				if (idlePct_pos < 0)     idlePct_pos = this.findColumn("idl");     // Solaris
				if (idlePct_pos < 0)     idlePct_pos = this.findColumn("id");      // AIX

				if (cpu_pos < 0 || usrPct_pos < 0 || sysPct_pos < 0 || iowaitPct_pos < 0 || idlePct_pos < 0)
				{
					String msg = "";
					if (cpu_pos        < 0) msg += "'CPU', ";
					if (usrPct_pos     < 0) msg += "'usrPct|usr|us', ";
					if (sysPct_pos     < 0) msg += "'sysPct|sys|sy', ";
					if (iowaitPct_pos  < 0) msg += "'iowaitPct|wt', ";
					if (idlePct_pos    < 0) msg += "'idlePct|idl|id', ";

					if (tg != null)
						tg.setWarningLabel("Column(s) "+StringUtil.removeLastComma(msg)+" can't be found. This graph is only supported on Linux/Solaris/AIX systems");
				}
				else
				{
					// Write 1 "line" for each CPU entry
					Double[] dArray = new Double[this.getRowCount()];
					String[] lArray = new String[dArray.length];
					for (int i=0; i<dArray.length; i++)
					{
						String cpu = this.getAbsString(i, cpu_pos);

						Double usrPct    = this.getAbsValueAsDouble(i, usrPct_pos);
						Double sysPct    = this.getAbsValueAsDouble(i, sysPct_pos);
						Double iowaitPct = this.getAbsValueAsDouble(i, iowaitPct_pos);
//						Double idlePct   = this.getAbsValueAsDouble(i, idlePct_pos);

						lArray[i] = "c-" + cpu;
						dArray[i] = usrPct + sysPct + iowaitPct;
					}

					// Set the values
					tgdp.setDataPoint(this.getTimestamp(), lArray, dArray);

					if (tg != null)
						tg.setWarningLabel(null);
				}
			}
		} // end: graph

	} // end: method

//	TODO; // Add alarm: cpu usage for all CPU's WARNING: ... at 95%
//	TODO; // Add alarm: cpu usage for all CPU's ERROR:   ... at 99%

	/**
	 * Get how many cores we have
	 * 
	 * @return core count (-1 if not available)
	 */
	public static int static_getCoreCount(ICounterController counterController)
	{
		int coreCount = -1;
		if (counterController == null)
		{
			counterController = CounterController.getInstance();
			//ccWarn = "WARNING: using Global Counter Controller.<br>";
			_logger.warn("No CounterController was passed to 'getCmOsPs_asHtmlTable', grabbing the global CounterController instance, which might NOT be the correct.");
		}
		
		CountersModel cmOsMpstat = counterController.getCmByName(CM_NAME);
		if (cmOsMpstat != null)
		{
			return ((CmOsMpstat)cmOsMpstat).getCoreCount();
		}
		
		return coreCount;
	}

	/**
	 * Get how many cores we have
	 * 
	 * @return core count (-1 if not available)
	 */
	public int getCoreCount()
	{
		int coreCount = -1;
		
//		CountersModel cmOsMpstat = CounterController.getInstance().getCmByName(CM_NAME);
		CountersModel cmOsMpstat = getCounterController().getCmByName(CM_NAME);
		if (cmOsMpstat != null)
		{
			if (cmOsMpstat.hasAbsData())
			{
				coreCount = 0;

				if (isConnectedToVendor(OsVendor.Windows))
				{
					int pos_Instance = this.findColumn("Instance");
					for (int r=0; r<this.getRowCount(); r++)
					{
						String instance = this.getAbsString(r, pos_Instance);
						if ("_Total".equals(instance))
							continue;
						
						coreCount++;
					}
				}
				else
				{
					int cpu_pos                          = this.findColumn("CPU"); // Linux, Solaris
					if (cpu_pos < 0)             cpu_pos = this.findColumn("cpu"); // AIX

					if (cpu_pos != -1)
					{
						for (int i=0; i<this.getRowCount(); i++)
						{
							String cpu = this.getAbsString(i, cpu_pos);

							if ("all".equalsIgnoreCase(cpu))
								continue;
							
							coreCount++;
						}
					}
				}
			}
		}
		
		return coreCount;
	}

	private Double getCpuUsage()
	{
		Double cpuPctUsage = null;

		if (isConnectedToVendor(OsVendor.Windows))
		{
			// Note the prefix: 'SQLServer' or 'MSSQL$@@servicename' is removed in SQL query
//			String pk = createPkStr("_Total", "", "");
//			Double val = this.getRateValueAsDouble(pk, "cntr_value");
//			Double processorTime  = this.getAbsValueAsDouble(pk, "% Processor Time");
//			Double userTime       = this.getAbsValueAsDouble(i, "% User Time");
//			Double privilegedTime = this.getAbsValueAsDouble(i, "% Privileged Time");
//			Double idleTime       = this.getAbsValueAsDouble(i, "% Idle Time");
			
			int pos_Instance = this.findColumn("Instance");
			for (int r=0; r<this.getRowCount(); r++)
			{
				String instance = this.getAbsString(r, pos_Instance);
				if ("_Total".equals(instance))
					cpuPctUsage = this.getAbsValueAsDouble(r, "% Processor Time");
			}
		}
		else
		{
			int cpu_pos                          = this.findColumn("CPU"); // Linux, Solaris
			if (cpu_pos < 0)             cpu_pos = this.findColumn("cpu"); // AIX
			
			int usrPct_pos                       = this.findColumn("usrPct"); // Linix
			if (usrPct_pos < 0)       usrPct_pos = this.findColumn("usr");    // Solaris
			if (usrPct_pos < 0)       usrPct_pos = this.findColumn("us");     // AIX

			int sysPct_pos                       = this.findColumn("sysPct"); // Linix
			if (sysPct_pos < 0)       sysPct_pos = this.findColumn("sys");    // Solaris
			if (sysPct_pos < 0)       sysPct_pos = this.findColumn("sy");     // AIX

			int iowaitPct_pos                    = this.findColumn("iowaitPct"); // Linix
			if (iowaitPct_pos < 0) iowaitPct_pos = this.findColumn("wt");        // Solaris
			if (iowaitPct_pos < 0) iowaitPct_pos = this.findColumn("wt");        // AIX

			int idlePct_pos                      = this.findColumn("idlePct"); // Linix
			if (idlePct_pos < 0)     idlePct_pos = this.findColumn("idl");     // Solaris
			if (idlePct_pos < 0)     idlePct_pos = this.findColumn("id");      // AIX

			if (cpu_pos < 0 || usrPct_pos < 0 || sysPct_pos < 0 || iowaitPct_pos < 0 || idlePct_pos < 0)
			{
				String msg = "";
				if (cpu_pos        < 0) msg += "'CPU', ";
				if (usrPct_pos     < 0) msg += "'usrPct|usr|us', ";
				if (sysPct_pos     < 0) msg += "'sysPct|sys|sy', ";
				if (iowaitPct_pos  < 0) msg += "'iowaitPct|wt', ";
				if (idlePct_pos    < 0) msg += "'idlePct|idl|id', ";
			}
			else
			{
				boolean gotAllEntry = false;

//				Double[] dArray = new Double[this.getRowCount()];
				Double[] aArray = new Double[5]; // "all" array (for values that do have the 'all' record)
				Double[] sArray = new Double[5]; // "sum" array (for values that do NOT have the 'all' record)
				String[] lArray = new String[5];

				// Initialize the "sum" array
				for (int i=0; i<sArray.length; i++)
					sArray[i] = 0.0;

				lArray[0] = "TotalPct (usr + sys + iowait)";
				lArray[1] = "usrPct";
				lArray[2] = "sysPct";
				lArray[3] = "iowaitPct";
				lArray[4] = "idlePct";

				for (int i=0; i<this.getRowCount(); i++)
				{
					String cpu = this.getAbsString(i, cpu_pos);

					if ("all".equalsIgnoreCase(cpu))
					{
						gotAllEntry = true;
						
						Double usrPct    = this.getAbsValueAsDouble(i, usrPct_pos);
						Double sysPct    = this.getAbsValueAsDouble(i, sysPct_pos);
						Double iowaitPct = this.getAbsValueAsDouble(i, iowaitPct_pos);
						Double idlePct   = this.getAbsValueAsDouble(i, idlePct_pos);

						aArray[0] = usrPct + sysPct + iowaitPct;
						aArray[1] = usrPct;
						aArray[2] = sysPct;
						aArray[3] = iowaitPct;
						aArray[4] = idlePct;
					}
					else
					{
						Double usrPct    = this.getAbsValueAsDouble(i, usrPct_pos);
						Double sysPct    = this.getAbsValueAsDouble(i, sysPct_pos);
						Double iowaitPct = this.getAbsValueAsDouble(i, iowaitPct_pos);
						Double idlePct   = this.getAbsValueAsDouble(i, idlePct_pos);

						sArray[0] += usrPct + sysPct + iowaitPct;
						sArray[1] += usrPct;
						sArray[2] += sysPct;
						sArray[3] += iowaitPct;
						sArray[4] += idlePct;
					}
				}

				if (gotAllEntry)
				{
					cpuPctUsage = aArray[0];
					// use the "all" entry
//					tgdp.setDataPoint(this.getTimestamp(), lArray, aArray);
				}
				else
				{
					// get average value from "sum" values / rowCount 
					for (int i=0; i<sArray.length; i++)
						sArray[i] = sArray[i] / (this.getRowCount() * 1.0);

					cpuPctUsage = sArray[0];
//					tgdp.setDataPoint(this.getTimestamp(), lArray, sArray);
				}
			}
		}
		return cpuPctUsage;
	}
	
	//--------------------------------------------------------------
	// Alarm handling
	//--------------------------------------------------------------
	@Override
	public void sendAlarmRequest()
	{
		if ( ! hasAbsData() )
			return;
		
		if ( ! AlarmHandler.hasInstance() )
			return;

		CountersModel cm = this;

		boolean debugPrint = Configuration.getCombinedConfiguration().getBooleanProperty("sendAlarmRequest.debug", _logger.isDebugEnabled());

		//-------------------------------------------------------
		// CpuUsageInfo
		//-------------------------------------------------------
		if (isSystemAlarmsForColumnEnabledAndInTimeRange("CpuUsageInfo"))
		{
			Double cpuPctUsage = getCpuUsage();
//System.out.println("CmOsMpstat: CpuUsageWarning: cpuPctUsage=" + cpuPctUsage);
			
			if (cpuPctUsage != null)
			{
				if (debugPrint || _logger.isDebugEnabled())
					System.out.println("##### sendAlarmRequest(" + cm.getName() + "): cpuPctUsage=" + cpuPctUsage + ".");

				double threshold = Configuration.getCombinedConfiguration().getDoubleProperty(PROPKEY_alarm_cpuUsageInfo, DEFAULT_alarm_cpuUsageInfo);
				if (cpuPctUsage > threshold)
				{
					String extendedDescText = "";
					String extendedDescHtml =               cm.getGraphDataHistoryAsHtmlImage(GRAPH_NAME_MpSum);
					       extendedDescHtml += "<br><br>" + cm.getGraphDataHistoryAsHtmlImage(GRAPH_NAME_MpCpu);

					// Possibly getting info from CmOsPs
					// This will help us to determine if OTHER processes than the DBMS is loading the server
					extendedDescHtml += CmOsPs.getCmOsPs_asHtmlTable(getCounterController(), 15);

					AlarmEvent ae = new AlarmEventHighCpuUtilization(cm, threshold, cpuPctUsage, AlarmEvent.Severity.INFO);
					ae.setExtendedDescription(extendedDescText, extendedDescHtml);
					
					int raiseDelayMinutes = Configuration.getCombinedConfiguration().getIntProperty(PROPKEY_alarm_cpuUsageInfo_delay_minutes, DEFAULT_alarm_cpuUsageInfo_delay_minutes);
					ae.setRaiseDelayInMinutes(raiseDelayMinutes);
					
					AlarmHandler.getInstance().addAlarm(ae);
				}
			}
		}

		//-------------------------------------------------------
		// CpuUsageWarning
		//-------------------------------------------------------
		if (isSystemAlarmsForColumnEnabledAndInTimeRange("CpuUsageWarning"))
		{
			Double cpuPctUsage = getCpuUsage();
//System.out.println("CmOsMpstat: CpuUsageWarning: cpuPctUsage=" + cpuPctUsage);
			
			if (cpuPctUsage != null)
			{
				if (debugPrint || _logger.isDebugEnabled())
					System.out.println("##### sendAlarmRequest(" + cm.getName() + "): cpuPctUsage=" + cpuPctUsage + ".");

				double threshold = Configuration.getCombinedConfiguration().getDoubleProperty(PROPKEY_alarm_cpuUsageWarning, DEFAULT_alarm_cpuUsageWarning);
				if (cpuPctUsage > threshold)
				{
					String extendedDescText = "";
					String extendedDescHtml =               cm.getGraphDataHistoryAsHtmlImage(GRAPH_NAME_MpSum);
					       extendedDescHtml += "<br><br>" + cm.getGraphDataHistoryAsHtmlImage(GRAPH_NAME_MpCpu);

					// Possibly getting info from CmOsPs
					// This will help us to determine if OTHER processes than the DBMS is loading the server
					extendedDescHtml += CmOsPs.getCmOsPs_asHtmlTable(getCounterController(), 15);

					AlarmEvent ae = new AlarmEventHighCpuUtilization(cm, threshold, cpuPctUsage, AlarmEvent.Severity.WARNING);
					ae.setExtendedDescription(extendedDescText, extendedDescHtml);
					
					int raiseDelayMinutes = Configuration.getCombinedConfiguration().getIntProperty(PROPKEY_alarm_cpuUsageWarning_delay_minutes, DEFAULT_alarm_cpuUsageWarning_delay_minutes);
					ae.setRaiseDelayInMinutes(raiseDelayMinutes);
					
					AlarmHandler.getInstance().addAlarm(ae);
				}
			}
		}

		//-------------------------------------------------------
		// CpuUsageError
		//-------------------------------------------------------
		if (isSystemAlarmsForColumnEnabledAndInTimeRange("CpuUsageError"))
		{
			Double cpuPctUsage = getCpuUsage();
//System.out.println("CmOsMpstat: CpuUsageError: cpuPctUsage=" + cpuPctUsage);
			
			if (cpuPctUsage != null)
			{
				if (debugPrint || _logger.isDebugEnabled())
					System.out.println("##### sendAlarmRequest(" + cm.getName() + "): cpuPctUsage=" + cpuPctUsage + ".");

				double threshold = Configuration.getCombinedConfiguration().getDoubleProperty(PROPKEY_alarm_cpuUsageError, DEFAULT_alarm_cpuUsageError);
				if (cpuPctUsage > threshold)
				{
					String extendedDescText = "";
					String extendedDescHtml =               cm.getGraphDataHistoryAsHtmlImage(GRAPH_NAME_MpSum);
					       extendedDescHtml += "<br><br>" + cm.getGraphDataHistoryAsHtmlImage(GRAPH_NAME_MpCpu);

					// Possibly getting info from CmOsPs
					// This will help us to determine if OTHER processes than the DBMS is loading the server
					extendedDescHtml += CmOsPs.getCmOsPs_asHtmlTable(getCounterController(), 15);

					AlarmEvent ae = new AlarmEventHighCpuUtilization(cm, threshold, cpuPctUsage, AlarmEvent.Severity.ERROR);
					ae.setExtendedDescription(extendedDescText, extendedDescHtml);

					int raiseDelayMinutes = Configuration.getCombinedConfiguration().getIntProperty(PROPKEY_alarm_cpuUsageError_delay_minutes, DEFAULT_alarm_cpuUsageError_delay_minutes);
					ae.setRaiseDelayInMinutes(raiseDelayMinutes);
					
					AlarmHandler.getInstance().addAlarm(ae);
				}
			}
		}
	} // end: method

	/**
	 * @return Get a HTML Image of the CPU Usage
	 */
	public static String getGraphDataHistoryAsHtmlImage(String graphName, ICounterController counterController)
	{
		String retStr = "";
		String ccWarn = "";
		
		if (counterController == null)
		{
			counterController = CounterController.getInstance();
			ccWarn = "WARNING: using Global Counter Controller.<br>";
			_logger.warn("No CounterController was passed to 'getCmOsPs_asHtmlTable', grabbing the global CounterController instance, which might NOT be the correct.");
		}
		
//		CountersModel cmOsMpstat = CounterController.getInstance().getCmByName(CM_NAME);
		CountersModel cmOsMpstat = counterController.getCmByName(CM_NAME);
		if (cmOsMpstat != null)
		{
			return cmOsMpstat.getGraphDataHistoryAsHtmlImage(graphName);
		}
		
		return retStr;
	}

	@Override
	public boolean isGraphDataHistoryEnabled(String name)
	{
		// ENABLED for the following graphs
		if (GRAPH_NAME_MpSum.equals(name)) return true;
		if (GRAPH_NAME_MpCpu.equals(name)) return true;

		// default: DISABLED
		return false;
	}
	@Override
	public int getGraphDataHistoryTimeInterval(String name)
	{
		// Keep interval: default is 60 minutes
		return super.getGraphDataHistoryTimeInterval(name);
	}

	
	public static final String  PROPKEY_alarm_cpuUsageInfo                       = CM_NAME + ".alarm.system.if.CpuUsageInfo.gt";
	public static final double  DEFAULT_alarm_cpuUsageInfo                       = 90.0d;
	public static final String  PROPKEY_alarm_cpuUsageInfo_delay_minutes         = CM_NAME + ".alarm.system.if.CpuUsageInfo.raise.delay.minutes";
	public static final int     DEFAULT_alarm_cpuUsageInfo_delay_minutes         = 45; // 45 minutes
	
	public static final String  PROPKEY_alarm_cpuUsageWarning                    = CM_NAME + ".alarm.system.if.CpuUsageWarning.gt";
	public static final double  DEFAULT_alarm_cpuUsageWarning                    = 95.0d;
	public static final String  PROPKEY_alarm_cpuUsageWarning_delay_minutes      = CM_NAME + ".alarm.system.if.CpuUsageWarning.raise.delay.minutes";
	public static final int     DEFAULT_alarm_cpuUsageWarning_delay_minutes      = 15; // 15 minutes
	
	public static final String  PROPKEY_alarm_cpuUsageError                      = CM_NAME + ".alarm.system.if.cpu.usage.error.gt";
	public static final double  DEFAULT_alarm_cpuUsageError                      = 99.0d;
	public static final String  PROPKEY_alarm_cpuUsageError_delay_minutes        = CM_NAME + ".alarm.system.if.CpuUsageWarning.raise.delay.minutes";
	public static final int     DEFAULT_alarm_cpuUsageError_delay_minutes        = 15; // 15 minutes
	
	
	@Override
	public List<CmSettingsHelper> getLocalAlarmSettings()
	{
		Configuration conf = Configuration.getCombinedConfiguration();
		List<CmSettingsHelper> list = new ArrayList<>();

		CmSettingsHelper.Type isAlarmSwitch = CmSettingsHelper.Type.IS_ALARM_SWITCH;

		list.add(new CmSettingsHelper("CpuUsageInfo", isAlarmSwitch,    PROPKEY_alarm_cpuUsageInfo,                  Double.class,  conf.getDoubleProperty(PROPKEY_alarm_cpuUsageInfo                 , DEFAULT_alarm_cpuUsageInfo                 ), DEFAULT_alarm_cpuUsageInfo                 , "If 'CpuUsage' is GREATER than ##.#, send 'AlarmEventHighCpuUtilization(INFO)'."));
		list.add(new CmSettingsHelper("CpuUsageInfo RaiseDelay",        PROPKEY_alarm_cpuUsageInfo_delay_minutes,    Integer.class, conf.getIntProperty(   PROPKEY_alarm_cpuUsageInfo_delay_minutes   , DEFAULT_alarm_cpuUsageInfo_delay_minutes   ), DEFAULT_alarm_cpuUsageInfo_delay_minutes   , "If 'CpuUsageWarning' is true and has been so for 45 minutes then proceed with the Alarm Raise." ));

		list.add(new CmSettingsHelper("CpuUsageWarning", isAlarmSwitch, PROPKEY_alarm_cpuUsageWarning,               Double.class,  conf.getDoubleProperty(PROPKEY_alarm_cpuUsageWarning              , DEFAULT_alarm_cpuUsageWarning              ), DEFAULT_alarm_cpuUsageWarning              , "If 'CpuUsage' is GREATER than ##.#, send 'AlarmEventHighCpuUtilization(WARNING)'."));
		list.add(new CmSettingsHelper("CpuUsageWarning RaiseDelay",     PROPKEY_alarm_cpuUsageWarning_delay_minutes, Integer.class, conf.getIntProperty(   PROPKEY_alarm_cpuUsageWarning_delay_minutes, DEFAULT_alarm_cpuUsageWarning_delay_minutes), DEFAULT_alarm_cpuUsageWarning_delay_minutes, "If 'CpuUsageWarning' is true and has been so for 15 minutes then proceed with the Alarm Raise." ));

		list.add(new CmSettingsHelper("CpuUsageError"  , isAlarmSwitch, PROPKEY_alarm_cpuUsageError,                 Double.class,  conf.getDoubleProperty(PROPKEY_alarm_cpuUsageError                , DEFAULT_alarm_cpuUsageError                ), DEFAULT_alarm_cpuUsageError                , "If 'CpuUsage' is GREATER than ##.# ,send 'AlarmEventHighCpuUtilization(ERROR)'."));
		list.add(new CmSettingsHelper("CpuUsageError RaiseDelay",       PROPKEY_alarm_cpuUsageError_delay_minutes,   Integer.class, conf.getIntProperty(   PROPKEY_alarm_cpuUsageError_delay_minutes  , DEFAULT_alarm_cpuUsageError_delay_minutes  ), DEFAULT_alarm_cpuUsageError_delay_minutes  , "If 'CpuUsageError' is true and has been so for 15 minutes then proceed with the Alarm Raise." ));

		return list;
	}
}
