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
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.dbxtune.ICounterController;
import com.dbxtune.IGuiController;
import com.dbxtune.alarm.AlarmHandler;
import com.dbxtune.alarm.events.AlarmEventOsSwapThrashing;
import com.dbxtune.alarm.events.AlarmEventOsSwapping;
import com.dbxtune.central.pcs.CentralPersistReader;
import com.dbxtune.cm.CmSettingsHelper;
import com.dbxtune.cm.CounterModelHostMonitor;
import com.dbxtune.cm.CounterSetTemplates;
import com.dbxtune.cm.CounterSetTemplates.Type;
import com.dbxtune.cm.CountersModel;
import com.dbxtune.cm.os.gui.CmOsVmstatPanel;
import com.dbxtune.graph.TrendGraphDataPoint;
import com.dbxtune.graph.TrendGraphDataPoint.LabelType;
import com.dbxtune.gui.MainFrame;
import com.dbxtune.gui.TabularCntrPanel;
import com.dbxtune.gui.TrendGraph;
import com.dbxtune.hostmon.HostMonitor.OsVendor;
import com.dbxtune.utils.Configuration;
import com.dbxtune.utils.MovingAverageChart;
import com.dbxtune.utils.MovingAverageCounterManager;

public class CmOsVmstat
extends CounterModelHostMonitor
{
	private static final Logger _logger = LogManager.getLogger(MethodHandles.lookup().lookupClass());
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

	@Override
	protected TabularCntrPanel createGui()
	{
		return new CmOsVmstatPanel(this);
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
		// Windows do NOT (for the moment) have any graphs for Windows
		if (isConnectedToVendor(OsVendor.Windows))
		{
			return;
		}

		// GRAPH
		addTrendGraph(GRAPH_NAME_PROCS_USAGE,
			"vmstat: Processes Usage",                                // Menu CheckBox text
			"vmstat: Processes Usage ("+GROUP_NAME+"->"+SHORT_NAME+")",    // Label 
			TrendGraphDataPoint.createGraphProps(TrendGraphDataPoint.Y_AXIS_SCALE_LABELS_NORMAL, CentralPersistReader.SampleType.MAX_OVER_SAMPLES),
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
			TrendGraphDataPoint.createGraphProps(TrendGraphDataPoint.Y_AXIS_SCALE_LABELS_NORMAL, CentralPersistReader.SampleType.MAX_OVER_SAMPLES),
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
			TrendGraphDataPoint.createGraphProps(TrendGraphDataPoint.Y_AXIS_SCALE_LABELS_NORMAL, CentralPersistReader.SampleType.MAX_OVER_SAMPLES),
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
			TrendGraphDataPoint.createGraphProps(TrendGraphDataPoint.Y_AXIS_SCALE_LABELS_PERSEC, CentralPersistReader.SampleType.MAX_OVER_SAMPLES),
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
			TrendGraphDataPoint.createGraphProps(TrendGraphDataPoint.Y_AXIS_SCALE_LABELS_PERSEC, CentralPersistReader.SampleType.MAX_OVER_SAMPLES),
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
			TrendGraphDataPoint.createGraphProps(TrendGraphDataPoint.Y_AXIS_SCALE_LABELS_PERCENT, CentralPersistReader.SampleType.MAX_OVER_SAMPLES),
			null, 
			LabelType.Dynamic,
			TrendGraphDataPoint.Category.CPU,
			true, // is Percent Graph
			false, // visible at start
			0,     // graph is valid from Server Version. 0 = All Versions; >0 = Valid from this version and above 
			-1);  // minimum height
	}

	
	@Override
	public void updateGraphData(TrendGraphDataPoint tgdp)
	{
		// Windows do NOT (for the moment) have any graphs for Windows
		if (isConnectedToVendor(OsVendor.Windows))
		{
			return;
		}

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

	
	/** How many minutes the Moving Average should be calculated on*/
	private static final int MOVING_AVG_TIME_IN_MINUTES = 10;

	@Override
	public void reset()
	{
		// Reset X minute average counters
		MovingAverageCounterManager.getInstance(this.getName(), "swapIn",  MOVING_AVG_TIME_IN_MINUTES).reset();
		MovingAverageCounterManager.getInstance(this.getName(), "swapOut", MOVING_AVG_TIME_IN_MINUTES).reset();
		
		super.reset();
	}

	@Override
	public void sendAlarmRequest()
	{
		// Windows isn't supported for VMSTAT
		if (isConnectedToVendor(OsVendor.Windows))
			return;

		if ( ! AlarmHandler.hasInstance() )
			return;

		AlarmHandler alarmHandler = AlarmHandler.getInstance();
		
		CountersModel cm = this;
		String groupName = this.getName();

		if ( ! cm.hasAbsData() )
			return;
		if ( ! cm.getCounterController().isHostMonConnected() )
			return;

		String hostname = cm.getCounterController().getHostMonConnection().getHostname();

		boolean debugPrint = Configuration.getCombinedConfiguration().getBooleanProperty("sendAlarmRequest.debug", _logger.isDebugEnabled());
		
		//-------------------------------------------------------
		if (isSystemAlarmsForColumnEnabledAndInTimeRange("Swapping"))
		{
			// Get column name based on what OS we are connected to
			String swap_si = null;
			String swap_so = null;
			if (     isConnectedToVendor(OsVendor.Linux))   { swap_si = "swap_si"; swap_so = "swap_so"; }
			else if (isConnectedToVendor(OsVendor.Solaris)) { swap_si = "page_pi"; swap_so = "page_po";  }
			else if (isConnectedToVendor(OsVendor.Aix))     { swap_si = "page_pi"; swap_so = "page_po";  }
			else if (isConnectedToVendor(OsVendor.Hp))      { swap_si = "page_pi"; swap_so = "page_po"; }

			int    threshold        = Configuration.getCombinedConfiguration().getIntProperty(PROPKEY_alarm_swap, DEFAULT_alarm_swap);
			double maxCapMultiplier = Configuration.getCombinedConfiguration().getDoubleProperty(PROPKEY_alarm_swap_maxCap_multiplier, DEFAULT_alarm_swap_maxCap_multiplier);
			double maxCap = threshold * maxCapMultiplier; // default: 1000 * 1.5

			// Get data
			Double swapIn_tmp  = this.getAbsValueAvg(swap_si);
			Double swapOut_tmp = this.getAbsValueAvg(swap_so);

			int swapIn  = (swapIn_tmp  == null) ? 0 : swapIn_tmp .intValue();
			int swapOut = (swapOut_tmp == null) ? 0 : swapOut_tmp.intValue();

			int swapIn_xmAvg  = (int) MovingAverageCounterManager.getInstance(groupName, "swapIn",  MOVING_AVG_TIME_IN_MINUTES).add(swapIn) .getAvg(0, true, maxCap);
			int swapOut_xmAvg = (int) MovingAverageCounterManager.getInstance(groupName, "swapOut", MOVING_AVG_TIME_IN_MINUTES).add(swapOut).getAvg(0, true, maxCap);

			// Add to 60m since this is what we use as a Graph in the Alarm (but we don't care/use the result calculation)
			MovingAverageCounterManager.getInstance(groupName, "swapIn",  60).add(swapIn) .getAvg(0, true, maxCap);
			MovingAverageCounterManager.getInstance(groupName, "swapOut", 60).add(swapOut).getAvg(0, true, maxCap);
			
			if (debugPrint || _logger.isDebugEnabled())
				System.out.println("##### sendAlarmRequest("+cm.getName()+"): swapping: in=" + swapIn + ", out=" + swapOut + ". swapIn_xmAvg=" + swapIn_xmAvg + ", swapOut_xmAvg=" + swapOut_xmAvg);

			if (swapIn_xmAvg > threshold || swapOut_xmAvg > threshold)
			{
				Timestamp swapIn_peakTs      = MovingAverageCounterManager.getInstance(groupName, "swapIn",  MOVING_AVG_TIME_IN_MINUTES).getPeakTimestamp();
				double    swapIn_peakNumber  = MovingAverageCounterManager.getInstance(groupName, "swapIn",  MOVING_AVG_TIME_IN_MINUTES).getPeakNumber();
				Timestamp swapOut_peakTs     = MovingAverageCounterManager.getInstance(groupName, "swapOut", MOVING_AVG_TIME_IN_MINUTES).getPeakTimestamp();
				double    swapOut_peakNumber = MovingAverageCounterManager.getInstance(groupName, "swapOut", MOVING_AVG_TIME_IN_MINUTES).getPeakNumber();

				// Create a small chart, that can be used in emails etc.
				String htmlChartImage = MovingAverageChart.getChartAsHtmlImage("OS Swapping (1 hour)", 
						MovingAverageCounterManager.getInstance(groupName, "swapIn",  60),  // Note make the chart on 60 minutes to see more info
						MovingAverageCounterManager.getInstance(groupName, "swapOut", 60)); // Note make the chart on 60 minutes to see more info

				// Get CPU Summary Usage chart
				htmlChartImage += CmOsMpstat.getGraphDataHistoryAsHtmlImage(CmOsMpstat.GRAPH_NAME_MpSum, getCounterController());
				
				// Possibly getting info from CmOsPs... This will help us to determine if OTHER processes than the DBMS is loading the server
				htmlChartImage += CmOsPs.getCmOsPs_asHtmlTable(getCounterController(), 15);
				
				AlarmEventOsSwapping alarm = new AlarmEventOsSwapping(cm, threshold, maxCap, hostname, "over " + MOVING_AVG_TIME_IN_MINUTES + " minute moving average", 
						swapIn_xmAvg,  swapIn_peakTs,  swapIn_peakNumber,
						swapOut_xmAvg, swapOut_peakTs, swapOut_peakNumber);

				alarm.setExtendedDescription(null, htmlChartImage);

				alarmHandler.addAlarm( alarm );
			}
		}

		//-------------------------------------------------------
		if (isSystemAlarmsForColumnEnabledAndInTimeRange("SwapThrashing"))
		{
			// Get column name based on what OS we are connected to
			String swap_si = null;
			String swap_so = null;
			if (     isConnectedToVendor(OsVendor.Linux))   { swap_si = "swap_si"; swap_so = "swap_so"; }
			else if (isConnectedToVendor(OsVendor.Solaris)) { swap_si = "page_pi"; swap_so = "page_po";  }
			else if (isConnectedToVendor(OsVendor.Aix))     { swap_si = "page_pi"; swap_so = "page_po";  }
			else if (isConnectedToVendor(OsVendor.Hp))      { swap_si = "page_pi"; swap_so = "page_po"; }

			int    threshold        = Configuration.getCombinedConfiguration().getIntProperty(PROPKEY_alarm_swap_thrashing, DEFAULT_alarm_swap_thrashing);
			double maxCapMultiplier = Configuration.getCombinedConfiguration().getDoubleProperty(PROPKEY_alarm_swap_thrashing_maxCap_multiplier, DEFAULT_alarm_swap_thrashing_maxCap_multiplier);
			double maxCap = threshold * maxCapMultiplier; // default: 150 * 2.0

			// Get data
			Double swapIn_tmp  = this.getAbsValueAvg(swap_si);
			Double swapOut_tmp = this.getAbsValueAvg(swap_so);

			int swapIn  = (swapIn_tmp  == null) ? 0 : swapIn_tmp .intValue();
			int swapOut = (swapOut_tmp == null) ? 0 : swapOut_tmp.intValue();

			int swapIn_xmAvg  = (int) MovingAverageCounterManager.getInstance(groupName, "swapIn",  MOVING_AVG_TIME_IN_MINUTES).add(swapIn) .getAvg(0, true, maxCap);
			int swapOut_xmAvg = (int) MovingAverageCounterManager.getInstance(groupName, "swapOut", MOVING_AVG_TIME_IN_MINUTES).add(swapOut).getAvg(0, true, maxCap);

			// Add to 60m since this is what we use as a Graph in the Alarm (but we don't care/use the result calculation)
			MovingAverageCounterManager.getInstance(groupName, "swapIn",  60).add(swapIn) .getAvg(0, true, maxCap);
			MovingAverageCounterManager.getInstance(groupName, "swapOut", 60).add(swapOut).getAvg(0, true, maxCap);
			
			if (debugPrint || _logger.isDebugEnabled())
				System.out.println("##### sendAlarmRequest("+cm.getName()+"): SwapThrashing: in=" + swapIn + ", out=" + swapOut + ". swapIn_xmAvg=" + swapIn_xmAvg + ", swapOut_xmAvg=" + swapOut_xmAvg);

			// BOTH swap 'in' AND 'out' 
			if (swapIn_xmAvg > threshold && swapOut_xmAvg > threshold)
			{
				Timestamp swapIn_peakTs      = MovingAverageCounterManager.getInstance(groupName, "swapIn",  MOVING_AVG_TIME_IN_MINUTES).getPeakTimestamp();
				double    swapIn_peakNumber  = MovingAverageCounterManager.getInstance(groupName, "swapIn",  MOVING_AVG_TIME_IN_MINUTES).getPeakNumber();
				Timestamp swapOut_peakTs     = MovingAverageCounterManager.getInstance(groupName, "swapOut", MOVING_AVG_TIME_IN_MINUTES).getPeakTimestamp();
				double    swapOut_peakNumber = MovingAverageCounterManager.getInstance(groupName, "swapOut", MOVING_AVG_TIME_IN_MINUTES).getPeakNumber();

				// Create a small chart, that can be used in emails etc.
				String htmlChartImage = MovingAverageChart.getChartAsHtmlImage("OS Swapping (1 hour)", 
						MovingAverageCounterManager.getInstance(groupName, "swapIn",  60),  // Note make the chart on 60 minutes to see more info
						MovingAverageCounterManager.getInstance(groupName, "swapOut", 60)); // Note make the chart on 60 minutes to see more info

				// Get CPU Summary Usage chart
				htmlChartImage += CmOsMpstat.getGraphDataHistoryAsHtmlImage(CmOsMpstat.GRAPH_NAME_MpSum, getCounterController());
				
				// Possibly getting info from CmOsPs... This will help us to determine if OTHER processes than the DBMS is loading the server
				htmlChartImage += CmOsPs.getCmOsPs_asHtmlTable(getCounterController(), 15);
				
				AlarmEventOsSwapThrashing alarm = new AlarmEventOsSwapThrashing(cm, threshold, maxCap, hostname, "over " + MOVING_AVG_TIME_IN_MINUTES + " minute moving average", 
						swapIn_xmAvg,  swapIn_peakTs,  swapIn_peakNumber,
						swapOut_xmAvg, swapOut_peakTs, swapOut_peakNumber);

				alarm.setExtendedDescription(null, htmlChartImage);

				alarmHandler.addAlarm( alarm );
			}
		}
	}

	public static final String  PROPKEY_alarm_swap                             = CM_NAME + ".alarm.system.if.swap.gt";  // Pages in OR out
	public static final int     DEFAULT_alarm_swap                             = 1000;
                                                                               
	public static final String  PROPKEY_alarm_swap_maxCap_multiplier           = CM_NAME + ".alarm.system.swap.maxCap.multiplier";
	public static final double  DEFAULT_alarm_swap_maxCap_multiplier           = 1.5d;

	public static final String  PROPKEY_alarm_swap_thrashing                   = CM_NAME + ".alarm.system.if.swap.thrashing.gt"; // Pages in AND out
	public static final int     DEFAULT_alarm_swap_thrashing                   = 150;
                                                                               
	public static final String  PROPKEY_alarm_swap_thrashing_maxCap_multiplier = CM_NAME + ".alarm.system.swap.thrashing.maxCap.multiplier";
	public static final double  DEFAULT_alarm_swap_thrashing_maxCap_multiplier = 2.0d;

	// The names are used "elsewhere", this makes it less buggy if we change the name
	public static final String  ALARM_NAME_Swapping                            = "Swapping";
	public static final String  ALARM_NAME_SwapThrashing                       = "SwapThrashing";

	@Override
	public List<CmSettingsHelper> getLocalAlarmSettings()
	{
		Configuration conf = Configuration.getCombinedConfiguration();
		List<CmSettingsHelper> list = new ArrayList<>();

		CmSettingsHelper.Type isAlarmSwitch = CmSettingsHelper.Type.IS_ALARM_SWITCH;

		list.add(new CmSettingsHelper("Swapping"     , isAlarmSwitch  , PROPKEY_alarm_swap                             , Integer.class, conf.getIntProperty   (PROPKEY_alarm_swap                             , DEFAULT_alarm_swap                            ), DEFAULT_alarm_swap                            , "If 'swap-in' or 'swap-out' is greater than ## (" + MOVING_AVG_TIME_IN_MINUTES + " minute average), then send 'AlarmEventOsSwapping'. NOTE: This Alarm is only on Linux/Unix. (for Windows see 'CmOsMeminfo')" ));
		list.add(new CmSettingsHelper("Swapping MaxCapMultiplier"     , PROPKEY_alarm_swap_maxCap_multiplier           , Double .class, conf.getDoubleProperty(PROPKEY_alarm_swap_maxCap_multiplier           , DEFAULT_alarm_swap_maxCap_multiplier          ), DEFAULT_alarm_swap_maxCap_multiplier          , "Parameter to 'Swapping', which sets a top limit (max cap), values above this does only count as the 'maxCap' value. so if the 'theshold' is set to 1000 and 'MaxCap Multiplier' is '1.5' The MaxCap will be 1500..." ));

		list.add(new CmSettingsHelper("SwapThrashing", isAlarmSwitch  , PROPKEY_alarm_swap_thrashing                   , Integer.class, conf.getIntProperty   (PROPKEY_alarm_swap_thrashing                   , DEFAULT_alarm_swap_thrashing)                  , DEFAULT_alarm_swap_thrashing                  , "If 'swap-in' AND 'swap-out' is greater than ## (" + MOVING_AVG_TIME_IN_MINUTES + " minute average), then send 'AlarmEventOsSwapThrashing'. NOTE: This Alarm is only on Linux/Unix. (for Windows see 'CmOsMeminfo')" ));
		list.add(new CmSettingsHelper("SwapThrashing MaxCapMultiplier", PROPKEY_alarm_swap_thrashing_maxCap_multiplier , Double .class, conf.getDoubleProperty(PROPKEY_alarm_swap_thrashing_maxCap_multiplier , DEFAULT_alarm_swap_thrashing_maxCap_multiplier), DEFAULT_alarm_swap_thrashing_maxCap_multiplier, "Parameter to 'SwapThrashing', which sets a top limit (max cap), values above this does only count as the 'maxCap' value. so if the 'theshold' is set to 150 and 'MaxCap Multiplier' is '2.0' The MaxCap will be 300..." ));

		return list;
	}
}
