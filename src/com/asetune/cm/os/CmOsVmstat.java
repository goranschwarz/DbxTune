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
package com.asetune.cm.os;

import com.asetune.ICounterController;
import com.asetune.IGuiController;
import com.asetune.cm.CounterModelHostMonitor;
import com.asetune.cm.CounterSetTemplates;
import com.asetune.cm.CounterSetTemplates.Type;
import com.asetune.cm.CountersModel;
import com.asetune.cm.os.gui.CmOsVmstatPanel;
import com.asetune.graph.TrendGraphDataPoint;
import com.asetune.graph.TrendGraphDataPoint.LabelType;
import com.asetune.gui.MainFrame;
import com.asetune.gui.TabularCntrPanel;
import com.asetune.gui.TrendGraph;
import com.asetune.hostmon.HostMonitor.OsVendor;

public class CmOsVmstat
extends CounterModelHostMonitor
{
//	private static Logger        _logger          = Logger.getLogger(CmOsVmstat.class);
	private static final long    serialVersionUID = 1L;

	public static final int      CM_TYPE          = CounterModelHostMonitor.HOSTMON_VMSTAT;
	public static final String   CM_NAME          = CmOsVmstat.class.getSimpleName();
	public static final String   SHORT_NAME       = "OS CPU(vmstat)";
	public static final String   HTML_DESC        = 
		"<html>" +
		"Executes: 'vmstat' on the Operating System" +
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

		return new CmOsVmstat(counterController, guiController);
	}

	public CmOsVmstat(ICounterController counterController, IGuiController guiController)
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


	//------------------------------------------------------------
	// Implementation
	//------------------------------------------------------------
	public static final String GRAPH_NAME_PROCS_USAGE   = "ProcsUsage";
	public static final String GRAPH_NAME_SWAP_USAGE    = "SwapUsage";
	public static final String GRAPH_NAME_MEM_USAGE     = "MemUsage";
	public static final String GRAPH_NAME_SWAP_IN_OUT   = "SwapInOut";
	public static final String GRAPH_NAME_IO_READ_WRITE = "IoReadWrite";
	public static final String GRAPH_NAME_CPU_USAGE     = "CpuUsage";

	private void addTrendGraphs()
	{
//////		String[] labelsCpu  = new String[] { "cpu_sy+cpu_us", "cpu_sy", "cpu_us", "cpu_id" };
////		String[] labelsCpu  = new String[] { "-runtime-replaced-" };
//		String[] labels = TrendGraphDataPoint.RUNTIME_REPLACED_LABELS;
//		
//		addTrendGraphData(GRAPH_NAME_PROCS_USAGE,   new TrendGraphDataPoint(GRAPH_NAME_PROCS_USAGE,   labels, LabelType.Dynamic));
//		addTrendGraphData(GRAPH_NAME_SWAP_USAGE,    new TrendGraphDataPoint(GRAPH_NAME_SWAP_USAGE,    labels, LabelType.Dynamic));
//		addTrendGraphData(GRAPH_NAME_MEM_USAGE,     new TrendGraphDataPoint(GRAPH_NAME_MEM_USAGE,     labels, LabelType.Dynamic));
//		addTrendGraphData(GRAPH_NAME_SWAP_IN_OUT,   new TrendGraphDataPoint(GRAPH_NAME_SWAP_IN_OUT,   labels, LabelType.Dynamic));
//		addTrendGraphData(GRAPH_NAME_IO_READ_WRITE, new TrendGraphDataPoint(GRAPH_NAME_IO_READ_WRITE, labels, LabelType.Dynamic));
//		addTrendGraphData(GRAPH_NAME_CPU_USAGE,     new TrendGraphDataPoint(GRAPH_NAME_CPU_USAGE,     labels, LabelType.Dynamic));

		// GRAPH
		addTrendGraph(GRAPH_NAME_PROCS_USAGE,
			"vmstat: Processes Usage",                                // Menu CheckBox text
			"vmstat: Processes Usage ("+GROUP_NAME+"->"+SHORT_NAME+")",    // Label 
			TrendGraphDataPoint.Y_AXIS_SCALE_LABELS_NORMAL,
			null, 
			LabelType.Dynamic,
			TrendGraphDataPoint.Category.CPU,
			false, // is Percent Graph
			false, // visible at start
			0,     // graph is valid from Server Version. 0 = All Versions; >0 = Valid from this version and above 
			-1);  // minimum height

		// GRAPH
		addTrendGraph(GRAPH_NAME_SWAP_USAGE,
			"vmstat: Swap Usage",                                // Menu CheckBox text
			"vmstat: Swap Usage ("+GROUP_NAME+"->"+SHORT_NAME+")",    // Label 
			TrendGraphDataPoint.Y_AXIS_SCALE_LABELS_NORMAL,
			null, 
			LabelType.Dynamic,
			TrendGraphDataPoint.Category.MEMORY,
			false, // is Percent Graph
			false, // visible at start
			0,     // graph is valid from Server Version. 0 = All Versions; >0 = Valid from this version and above 
			-1);  // minimum height

		// GRAPH
		addTrendGraph(GRAPH_NAME_MEM_USAGE,
			"vmstat: Memory Usage",                                // Menu CheckBox text
			"vmstat: Memory Usage ("+GROUP_NAME+"->"+SHORT_NAME+")",    // Label 
			TrendGraphDataPoint.Y_AXIS_SCALE_LABELS_NORMAL,
			null, 
			LabelType.Dynamic,
			TrendGraphDataPoint.Category.MEMORY,
			false, // is Percent Graph
			false, // visible at start
			0,     // graph is valid from Server Version. 0 = All Versions; >0 = Valid from this version and above 
			-1);  // minimum height

		addTrendGraph(GRAPH_NAME_SWAP_IN_OUT,
			"vmstat: Swap In/Out per sec",                                // Menu CheckBox text
			"vmstat: Swap In/Out per sec ("+GROUP_NAME+"->"+SHORT_NAME+")",    // Label 
			TrendGraphDataPoint.Y_AXIS_SCALE_LABELS_PERSEC,
			null, 
			LabelType.Dynamic,
			TrendGraphDataPoint.Category.MEMORY,
			false, // is Percent Graph
			false, // visible at start
			0,     // graph is valid from Server Version. 0 = All Versions; >0 = Valid from this version and above 
			-1);  // minimum height

		addTrendGraph(GRAPH_NAME_IO_READ_WRITE,
			"vmstat: IO Read/Write or blk-in/out per sec",                                // Menu CheckBox text
			"vmstat: IO Read/Write or blk-in/out per sec ("+GROUP_NAME+"->"+SHORT_NAME+")",    // Label 
			TrendGraphDataPoint.Y_AXIS_SCALE_LABELS_PERSEC,
			null, 
			LabelType.Dynamic,
			TrendGraphDataPoint.Category.DISK,
			false, // is Percent Graph
			false, // visible at start
			0,     // graph is valid from Server Version. 0 = All Versions; >0 = Valid from this version and above 
			-1);  // minimum height

		// GRAPH
		addTrendGraph(GRAPH_NAME_CPU_USAGE,
			"vmstat: CPU Usage",                                // Menu CheckBox text
			"vmstat: CPU Usage ("+GROUP_NAME+"->"+SHORT_NAME+")",    // Label 
			TrendGraphDataPoint.Y_AXIS_SCALE_LABELS_PERCENT,
			null, 
			LabelType.Dynamic,
			TrendGraphDataPoint.Category.CPU,
			true, // is Percent Graph
			false, // visible at start
			0,     // graph is valid from Server Version. 0 = All Versions; >0 = Valid from this version and above 
			-1);  // minimum height
		
//		// if GUI
//		if (getGuiController() != null && getGuiController().hasGUI())
//		{
//			TrendGraph tg = null;
//
//			// GRAPH
//			tg = new TrendGraph(GRAPH_NAME_PROCS_USAGE,
//				"vmstat: Processes Usage",                                // Menu CheckBox text
//				"vmstat: Processes Usage ("+GROUP_NAME+"->"+SHORT_NAME+")",    // Label 
//				labels, 
//				false, // is Percent Graph
//				this, 
//				false, // visible at start
//				0,     // graph is valid from Server Version. 0 = All Versions; >0 = Valid from this version and above 
//				-1);  // minimum height
//			addTrendGraph(tg.getName(), tg, true);
//
//			// GRAPH
//			tg = new TrendGraph(GRAPH_NAME_SWAP_USAGE,
//				"vmstat: Swap Usage",                                // Menu CheckBox text
//				"vmstat: Swap Usage ("+GROUP_NAME+"->"+SHORT_NAME+")",    // Label 
//				labels, 
//				false, // is Percent Graph
//				this, 
//				false, // visible at start
//				0,     // graph is valid from Server Version. 0 = All Versions; >0 = Valid from this version and above 
//				-1);  // minimum height
//			addTrendGraph(tg.getName(), tg, true);
//
//			// GRAPH
//			tg = new TrendGraph(GRAPH_NAME_MEM_USAGE,
//				"vmstat: Memory Usage",                                // Menu CheckBox text
//				"vmstat: Memory Usage ("+GROUP_NAME+"->"+SHORT_NAME+")",    // Label 
//				labels, 
//				false, // is Percent Graph
//				this, 
//				false, // visible at start
//				0,     // graph is valid from Server Version. 0 = All Versions; >0 = Valid from this version and above 
//				-1);  // minimum height
//			addTrendGraph(tg.getName(), tg, true);
//
//			tg = new TrendGraph(GRAPH_NAME_SWAP_IN_OUT,
//				"vmstat: Swap In/Out per sec",                                // Menu CheckBox text
//				"vmstat: Swap In/Out per sec ("+GROUP_NAME+"->"+SHORT_NAME+")",    // Label 
//				labels, 
//				false, // is Percent Graph
//				this, 
//				false, // visible at start
//				0,     // graph is valid from Server Version. 0 = All Versions; >0 = Valid from this version and above 
//				-1);  // minimum height
//			addTrendGraph(tg.getName(), tg, true);
//
//			tg = new TrendGraph(GRAPH_NAME_IO_READ_WRITE,
//				"vmstat: IO Read/Write or blk-in/out per sec",                                // Menu CheckBox text
//				"vmstat: IO Read/Write or blk-in/out per sec ("+GROUP_NAME+"->"+SHORT_NAME+")",    // Label 
//				labels, 
//				false, // is Percent Graph
//				this, 
//				false, // visible at start
//				0,     // graph is valid from Server Version. 0 = All Versions; >0 = Valid from this version and above 
//				-1);  // minimum height
//			addTrendGraph(tg.getName(), tg, true);
//
//			// GRAPH
//			tg = new TrendGraph(GRAPH_NAME_CPU_USAGE,
//				"vmstat: CPU Usage",                                // Menu CheckBox text
//				"vmstat: CPU Usage ("+GROUP_NAME+"->"+SHORT_NAME+")",    // Label 
//				labels, 
//				true, // is Percent Graph
//				this, 
//				false, // visible at start
//				0,     // graph is valid from Server Version. 0 = All Versions; >0 = Valid from this version and above 
//				-1);  // minimum height
//			addTrendGraph(tg.getName(), tg, true);
//		}
	}

	
	@Override
	protected TabularCntrPanel createGui()
	{
		return new CmOsVmstatPanel(this);
	}

	@Override
	public void updateGraphData(TrendGraphDataPoint tgdp)
	{
		if (GRAPH_NAME_PROCS_USAGE.equals(tgdp.getName()))
		{
			String procs_r = null;
			String procs_b = null;
			if (     isConnectedToVendor(OsVendor.Linux))   { procs_r = "procs_r"; procs_b = "procs_b"; }
			else if (isConnectedToVendor(OsVendor.Solaris)) { procs_r = "kthr_r";  procs_b = "kthr_b";  }
			else if (isConnectedToVendor(OsVendor.Aix))     { procs_r = "kthr_r";  procs_b = "kthr_b";  }
			else if (isConnectedToVendor(OsVendor.Hp))      { procs_r = "procs_r"; procs_b = "procs_b"; }

			Double[] arr = new Double[2];
			String[] label = new String[] {
					"RunQueue - # Processes Waiting for CPU time ["+procs_r+"]", 
					"WaitQueue - # Process Waiting for I/O (disk, network, user input,etc..) ["+procs_b+"]"};
			
			arr[0] = this.getAbsValueAvg(procs_r);
			arr[1] = this.getAbsValueAvg(procs_b);

			tgdp.setDataPoint(this.getTimestamp(), label, arr);
		}
		
		if (GRAPH_NAME_SWAP_USAGE.equals(tgdp.getName()))
		{
			String memory_swpd = null;
			if      (isConnectedToVendor(OsVendor.Linux))   { memory_swpd = "memory_swpd"; }
			else if (isConnectedToVendor(OsVendor.Solaris)) { memory_swpd = "memory_swap"; } // INVESTIGATE HERE
			else if (isConnectedToVendor(OsVendor.Aix))     { memory_swpd = "memory_avm";  } // INVESTIGATE HERE
			else if (isConnectedToVendor(OsVendor.Hp))      { memory_swpd = "memory_avm";  } // INVESTIGATE HERE

			Double[] arr = new Double[1];
			String[] label = new String[] {"How many Blocks are Swapped out to disk (paged). Total Virtual memory usage. ["+memory_swpd+"]"};
			
			arr[0] = this.getAbsValueAvg(memory_swpd);

			tgdp.setDataPoint(this.getTimestamp(), label, arr);
		}
	
		if (GRAPH_NAME_MEM_USAGE.equals(tgdp.getName()))
		{
			String memory_free = null;
			String memory_buff = null;
			String memory_cache = null;
			if (     isConnectedToVendor(OsVendor.Linux))   { memory_free = "memory_free"; memory_buff = "memory_buff"; memory_cache = "memory_cache"; }
//			else if (isConnectedToVendor(OsVendor.Solaris)) { memory_free = "memory_free"; memory_buff = "?????";       memory_cache = "????"; } // INVESTIGATE HERE
//			else if (isConnectedToVendor(OsVendor.Aix))     { memory_free = "memory_free"; memory_buff = "?????";       memory_cache = "????"; } // INVESTIGATE HERE
//			else if (isConnectedToVendor(OsVendor.Hp))      { memory_free = "memory_free"; memory_buff = "?????";       memory_cache = "????"; } // INVESTIGATE HERE
			else
			{
				TrendGraph tg = getTrendGraph(tgdp.getName());
				if (tg != null)
    				tg.setWarningLabel("This graph is not available on '"+getConnectedToVendor()+"'.");
				return;
			}

			Double[] arr = new Double[3];
			String[] label = new String[] {
					"Idle/Free Memory ["+memory_free+"]", 
					"Memory used as Buffers ["+memory_buff+"]", 
					"Memory used as cache by the Operating System ["+memory_cache+"]"};
			
			arr[0] = this.getAbsValueAvg(memory_free);
			arr[1] = this.getAbsValueAvg(memory_buff);
			arr[2] = this.getAbsValueAvg(memory_cache);

			tgdp.setDataPoint(this.getTimestamp(), label, arr);
		}
	
		if (GRAPH_NAME_SWAP_IN_OUT.equals(tgdp.getName()))
		{
			String swap_si = null;
			String swap_so = null;
			if (     isConnectedToVendor(OsVendor.Linux))   { swap_si = "swap_si"; swap_so = "swap_so"; }
			else if (isConnectedToVendor(OsVendor.Solaris)) { swap_si = "page_pi"; swap_so = "page_po";  }
			else if (isConnectedToVendor(OsVendor.Aix))     { swap_si = "page_pi"; swap_so = "page_po";  }
			else if (isConnectedToVendor(OsVendor.Hp))      { swap_si = "page_pi"; swap_so = "page_po"; }

			Double[] arr = new Double[2];
			String[] label = new String[] {
					"IN - Read from Swap to Memory ["+swap_si+"]", 
					"OUT - Written to Swap and Removed from Memory ["+swap_so+"]"};
			
			arr[0] = this.getAbsValueAvg(swap_si);
			arr[1] = this.getAbsValueAvg(swap_so);

			tgdp.setDataPoint(this.getTimestamp(), label, arr);
		}
	
		if (GRAPH_NAME_IO_READ_WRITE.equals(tgdp.getName()))
		{
			String io_bi = null;
			String io_bo = null;
			if (     isConnectedToVendor(OsVendor.Linux))   { io_bi = "io_bi"; io_bo = "io_bo"; }
//			else if (isConnectedToVendor(OsVendor.Solaris)) { io_bi = "?????"; io_bo = "?????"; }  // INVESTIGATE HERE
//			else if (isConnectedToVendor(OsVendor.Aix))     { io_bi = "?????"; io_bo = "?????"; }  // INVESTIGATE HERE
//			else if (isConnectedToVendor(OsVendor.Hp))      { io_bi = "?????"; io_bo = "?????"; }  // INVESTIGATE HERE
			else
			{
				TrendGraph tg = getTrendGraph(tgdp.getName());
				if (tg != null)
    				tg.setWarningLabel("This graph is not available on '"+getConnectedToVendor()+"'.");
				return;
			}

			Double[] arr = new Double[2];
			String[] label = new String[] {
					"Reads/s - Blocks received from block device ["+io_bi+"]", 
					"Writes/s - Blocks sent to a block device ["+io_bo+"]"};
			
			arr[0] = this.getAbsValueAvg(io_bi);
			arr[1] = this.getAbsValueAvg(io_bo);

			tgdp.setDataPoint(this.getTimestamp(), label, arr);
		}
//Also look at: SPID-Wait... "empty" labels, should expand to all-zero if we have previous labels attached...
	
		if (GRAPH_NAME_CPU_USAGE.equals(tgdp.getName()))
		{
			boolean hasCpuWa = this.findColumn("cpu_wa") > 0;
			Double[] arr = new Double[4];
			String[] label = new String[] {
					"cpu_sy+cpu_us", 
					"cpu_sy", 
					"cpu_us", 
					"cpu_id", 
					"cpu_wa"};

			// If we have WA = IO Wait, lets include that one
			if (hasCpuWa)
			{
				arr = new Double[5];
				label = new String[] {
						"Total (cpu_sy + cpu_us + cpu_wa)", 
						"cpu_sy", 
						"cpu_us", 
						"cpu_id", 
						"cpu_wa"};
			}
			else
			{
				arr = new Double[4];
				label = new String[] {
						"Total (cpu_sy + cpu_us)", 
						"cpu_sy", 
						"cpu_us", 
						"cpu_id"};
			}
				

			// NOTE: only ABS values are present in CounterModelHostMonitor
//			arr[0] = this.getAbsValueAvg("cpu_us"); // NOTE: arr[0] is set near the end
			arr[1] = this.getAbsValueAvg("cpu_sy");
			arr[2] = this.getAbsValueAvg("cpu_us");
			arr[3] = this.getAbsValueAvg("cpu_id");
			if (hasCpuWa)
				arr[4] = this.getAbsValueAvg("cpu_wa");

//System.out.println(GRAPH_NAME_CPU_USAGE + ": arr[1] 'sy' = " + arr[1]);
//System.out.println(GRAPH_NAME_CPU_USAGE + ": arr[2] 'us' = " + arr[2]);
//System.out.println(GRAPH_NAME_CPU_USAGE + ": arr[3] 'id' = " + arr[3]);
//if (hasCpuWa)
//	System.out.println(GRAPH_NAME_CPU_USAGE + ": arr[4] 'wa' = " + arr[4]);

			// Calculate "cpu_sy + cpu_us [+ cpu_wa]"
			if (arr[1] != null && arr[2] != null)
				arr[0] = arr[1] + arr[2];
			if (hasCpuWa && arr[1] != null && arr[2] != null && arr[4] != null)
				arr[0] = arr[1] + arr[2] + arr[4];
			
			// Fix if combined is over 100
			if (arr[0] != null && arr[0] > 100.0)
				arr[0] = 100.0;

//			if (_logger.isDebugEnabled())
//				_logger.debug("updateGraphData(OsVmstat): us+sy='"+arr[0]+"', cpu_us='"+arr[1]+"', cpu_sy='"+arr[2]+"', cpu_id='"+arr[3]+"'.");

			// Set the values
			tgdp.setDataPoint(this.getTimestamp(), label, arr);
//			tgdp.setDate(this.getTimestamp());
//			tgdp.setLabel(label);
//			tgdp.setData(arr);
		}
	}


//-----------------------------------------------------------------------------
//-- MAYBE: Do alarm when the machine has been swaping "to much" for a while
//--        We may need to calculate our own "1m, 5m, 15m" swap average
//-- NOTE:  The below is just "taken" from CmOsUptime, and needs to be altered
//-----------------------------------------------------------------------------
//	@Override
//	public void sendAlarmRequest()
//	{
//		CountersModel cm = this;
//
//		if ( ! cm.hasAbsData() )
//			return;
//		if ( ! cm.getCounterController().isHostMonConnected() )
//			return;
//
//		String hostname = cm.getCounterController().getHostMonConnection().getHost();
//
//		boolean debugPrint = System.getProperty("sendAlarmRequest.debug", "false").equalsIgnoreCase("true");
//		
//		//-------------------------------------------------------
//		// Run Queue Length (adjLoadAverage_1Min), Avg Last Minute 
//		//-------------------------------------------------------
//		Double adjLoadAverage_1Min  = this.getAbsValueAsDouble(0, "adjLoadAverage_1Min");
//		Double adjLoadAverage_5Min  = this.getAbsValueAsDouble(0, "adjLoadAverage_5Min");
//		Double adjLoadAverage_15Min = this.getAbsValueAsDouble(0, "adjLoadAverage_15Min");
//
//		if (adjLoadAverage_1Min != null)
//		{
//			if (debugPrint || _logger.isDebugEnabled())
//				System.out.println("##### sendAlarmRequest("+cm.getName()+"): adjLoadAverage: 1min=" + adjLoadAverage_1Min + ", 5min=" + adjLoadAverage_5Min + ", 15min=" + adjLoadAverage_15Min + ".");
//
//			if (AlarmHandler.hasInstance())
//			{
//				double threshold = Configuration.getCombinedConfiguration().getDoubleProperty(PROPKEY_alarm_adjLoadAverage_1Min, DEFAULT_alarm_adjLoadAverage_1Min);
//				if (adjLoadAverage_1Min > threshold)
//					AlarmHandler.getInstance().addAlarm( new AlarmEventOsLoadAverage(cm, hostname, adjLoadAverage_1Min, adjLoadAverage_5Min, adjLoadAverage_15Min) );
//			}
//		}
//	}
//
//	public static final String  PROPKEY_alarm_adjLoadAverage_1Min = CM_NAME + ".alarm.system.if.adjLoadAverage_1Min.gt";
//	public static final double  DEFAULT_alarm_adjLoadAverage_1Min = 1.0;
//
//	@Override
//	public List<CmSettingsHelper> getLocalAlarmSettings()
//	{
//		Configuration conf = Configuration.getCombinedConfiguration();
//		List<CmSettingsHelper> list = new ArrayList<>();
//		
//		list.add(new CmSettingsHelper("adjLoadAverage_1Min", PROPKEY_alarm_adjLoadAverage_1Min , Double.class, conf.getDoubleProperty(PROPKEY_alarm_adjLoadAverage_1Min , DEFAULT_alarm_adjLoadAverage_1Min), DEFAULT_alarm_adjLoadAverage_1Min, "If 'adjLoadAverage_1Min' is greater than ## then send 'AlarmEventOsLoadAverage'." ));
//
//		return list;
//	}
}
