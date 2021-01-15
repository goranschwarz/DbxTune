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
package com.asetune.cm.os;

import org.apache.commons.lang3.ArrayUtils;

import com.asetune.ICounterController;
import com.asetune.IGuiController;
import com.asetune.cm.CounterModelHostMonitor;
import com.asetune.cm.CounterSetTemplates;
import com.asetune.cm.CounterSetTemplates.Type;
import com.asetune.cm.CountersModel;
import com.asetune.cm.os.gui.CmOsMpstatPanel;
import com.asetune.graph.TrendGraphDataPoint;
import com.asetune.graph.TrendGraphDataPoint.LabelType;
import com.asetune.gui.MainFrame;
import com.asetune.gui.TabularCntrPanel;
import com.asetune.gui.TrendGraph;
import com.asetune.hostmon.HostMonitor.OsVendor;
import com.asetune.utils.StringUtil;

public class CmOsMpstat
extends CounterModelHostMonitor
{
//	private static Logger        _logger          = Logger.getLogger(CmOsMpstat.class);
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
			TrendGraphDataPoint.Y_AXIS_SCALE_LABELS_PERCENT,
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
			TrendGraphDataPoint.Y_AXIS_SCALE_LABELS_PERCENT,
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
						dArray[0] = this.getAbsValueAsDouble(i, "% Processor Time");
						dArray[1] = this.getAbsValueAsDouble(i, "% User Time");
						dArray[2] = this.getAbsValueAsDouble(i, "% Privileged Time");
						dArray[3] = this.getAbsValueAsDouble(i, "% Idle Time");
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

				int idlePct_pos                      = this.findColumn("idlePct"); // Linix
				if (idlePct_pos < 0)     idlePct_pos = this.findColumn("idl");     // Solaris
				if (idlePct_pos < 0)     idlePct_pos = this.findColumn("id");      // AIX

				int iowaitPct_pos                    = this.findColumn("iowaitPct"); // Linix
				if (iowaitPct_pos < 0) iowaitPct_pos = this.findColumn("wt");        // Solaris
				if (iowaitPct_pos < 0) iowaitPct_pos = this.findColumn("wt");        // AIX

				if (cpu_pos < 0 || usrPct_pos < 0 || sysPct_pos < 0 || idlePct_pos < 0 || iowaitPct_pos < 0)
				{
					String msg = "";
					if (cpu_pos        < 0) msg += "'CPU', ";
					if (usrPct_pos     < 0) msg += "'usrPct|usr|us', ";
					if (sysPct_pos     < 0) msg += "'sysPct|sys|sy', ";
					if (idlePct_pos    < 0) msg += "'idlePct|idl|id', ";
					if (iowaitPct_pos  < 0) msg += "'iowaitPct|wt', ";

					if (tg != null)
						tg.setWarningLabel("Column(s) "+StringUtil.removeLastComma(msg)+" can't be found. This graph is only supported on Linux/Solaris/AIX systems");
				}
				else
				{
					boolean gotAllEntry = false;

//					Double[] dArray = new Double[this.getRowCount()];
					Double[] aArray = new Double[4]; // "all" array (for values that do have the 'all' record)
					Double[] sArray = new Double[4]; // "sum" array (for values that do NOT have the 'all' record)
					String[] lArray = new String[4];

					// Initialize the "sum" array
					for (int i=0; i<sArray.length; i++)
						sArray[i] = 0.0;

					lArray[0] = "usrPct";
					lArray[1] = "sysPct";
					lArray[2] = "idlePct";
					lArray[3] = "iowaitPct";

					for (int i=0; i<this.getRowCount(); i++)
					{
						String cpu = this.getAbsString(i, cpu_pos);

						if ("all".equalsIgnoreCase(cpu))
						{
							gotAllEntry = true;
							
							Double usrPct    = this.getAbsValueAsDouble(i, usrPct_pos);
							Double sysPct    = this.getAbsValueAsDouble(i, sysPct_pos);
							Double idlePct   = this.getAbsValueAsDouble(i, idlePct_pos);
							Double iowaitPct = this.getAbsValueAsDouble(i, iowaitPct_pos);

							aArray[0] = usrPct;
							aArray[1] = sysPct;
							aArray[2] = idlePct;
							aArray[3] = iowaitPct;
						}
						else
						{
							Double usrPct    = this.getAbsValueAsDouble(i, usrPct_pos);
							Double sysPct    = this.getAbsValueAsDouble(i, sysPct_pos);
							Double idlePct   = this.getAbsValueAsDouble(i, idlePct_pos);
							Double iowaitPct = this.getAbsValueAsDouble(i, iowaitPct_pos);

							sArray[0] += usrPct;
							sArray[1] += sysPct;
							sArray[2] += idlePct;
							sArray[3] += iowaitPct;
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
				
				int usrPct_pos                       = this.findColumn("usrPct"); // Linix
				if (usrPct_pos < 0)       usrPct_pos = this.findColumn("usr");    // Solaris
				if (usrPct_pos < 0)       usrPct_pos = this.findColumn("us");     // AIX

				int sysPct_pos                       = this.findColumn("sysPct"); // Linix
				if (sysPct_pos < 0)       sysPct_pos = this.findColumn("sys");    // Solaris
				if (sysPct_pos < 0)       sysPct_pos = this.findColumn("sy");     // AIX

				int idlePct_pos                      = this.findColumn("idlePct"); // Linix
				if (idlePct_pos < 0)     idlePct_pos = this.findColumn("idl");     // Solaris
				if (idlePct_pos < 0)     idlePct_pos = this.findColumn("id");      // AIX

				int iowaitPct_pos                    = this.findColumn("iowaitPct"); // Linix
				if (iowaitPct_pos < 0) iowaitPct_pos = this.findColumn("wt");        // Solaris
				if (iowaitPct_pos < 0) iowaitPct_pos = this.findColumn("wt");        // AIX

				if (cpu_pos < 0 || usrPct_pos < 0 || sysPct_pos < 0 || idlePct_pos < 0 || iowaitPct_pos < 0)
				{
					String msg = "";
					if (cpu_pos        < 0) msg += "'CPU', ";
					if (usrPct_pos     < 0) msg += "'usrPct|usr|us', ";
					if (sysPct_pos     < 0) msg += "'sysPct|sys|sy', ";
					if (idlePct_pos    < 0) msg += "'idlePct|idl|id', ";
					if (iowaitPct_pos  < 0) msg += "'iowaitPct|wt', ";

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
//						Double idlePct   = this.getAbsValueAsDouble(i, idlePct_pos);
						Double iowaitPct = this.getAbsValueAsDouble(i, iowaitPct_pos);

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

}
