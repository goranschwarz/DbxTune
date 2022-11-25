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

import java.math.BigDecimal;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;

import org.apache.log4j.Logger;

import com.asetune.ICounterController;
import com.asetune.IGuiController;
import com.asetune.alarm.AlarmHandler;
import com.asetune.alarm.events.AlarmEventOsSwapping;
import com.asetune.cm.CmSettingsHelper;
import com.asetune.cm.CounterModelHostMonitor;
import com.asetune.cm.CounterSetTemplates;
import com.asetune.cm.CounterSetTemplates.Type;
import com.asetune.cm.CountersModel;
import com.asetune.cm.os.gui.CmOsMeminfoPanel;
import com.asetune.graph.TrendGraphDataPoint;
import com.asetune.graph.TrendGraphDataPoint.LabelType;
import com.asetune.gui.MainFrame;
import com.asetune.gui.TabularCntrPanel;
import com.asetune.hostmon.HostMonitor.OsVendor;
import com.asetune.hostmon.OsTable;
import com.asetune.utils.Configuration;
import com.asetune.utils.MovingAverageChart;
import com.asetune.utils.MovingAverageCounterManager;

public class CmOsMeminfo
extends CounterModelHostMonitor
{
	private static Logger        _logger          = Logger.getLogger(CmOsMeminfo.class);
	private static final long    serialVersionUID = 1L;

	public static final int      CM_TYPE          = CounterModelHostMonitor.HOSTMON_MEMINFO;
	public static final String   CM_NAME          = CmOsMeminfo.class.getSimpleName();
	public static final String   SHORT_NAME       = "OS Memory Info";
	public static final String   HTML_DESC        = 
		"<html>" +
		"Executes: 'cat /proc/meminfo' on the Operating System (Linux only)" +
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

		return new CmOsMeminfo(counterController, guiController);
	}

	public CmOsMeminfo(ICounterController counterController, IGuiController guiController)
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
		return new CmOsMeminfoPanel(this);
	}


	//------------------------------------------------------------
	// Implementation
	//------------------------------------------------------------
	public static final String GRAPH_NAME_MEM_USED      = "MemUsed";
//	public static final String GRAPH_NAME_MEM_FREE      = "MemFree";
	public static final String GRAPH_NAME_MEM_AVAILABLE = "MemAvailable";

	public static final String GRAPH_NAME_WIN_PAGING      = "WinPaging";
	public static final String GRAPH_NAME_WIN_PAGING_FILE = "WinPagingFile";

	private void addTrendGraphs()
	{
		// GRAPH
		addTrendGraph(GRAPH_NAME_MEM_USED,
			"meminfo: Used Memory", 	                                // Menu CheckBox text
			"meminfo: Used Memory ("+GROUP_NAME+"->"+SHORT_NAME+")",    // Label 
			TrendGraphDataPoint.Y_AXIS_SCALE_LABELS_MB,
			new String[] { "MemUsed in MB" }, 
			LabelType.Static,
			TrendGraphDataPoint.Category.MEMORY,
			false, // is Percent Graph
			false, // visible at start
			0,     // graph is valid from Server Version. 0 = All Versions; >0 = Valid from this version and above 
			-1);   // minimum height

//		// GRAPH
//		addTrendGraph(GRAPH_NAME_MEM_FREE,
//			"meminfo: Free Memory", 	                                // Menu CheckBox text
//			"meminfo: Free Memory ("+GROUP_NAME+"->"+SHORT_NAME+")",    // Label 
//			new String[] { "MemFree in MB" }, 
//			LabelType.Static,
//			false, // is Percent Graph
//			false, // visible at start
//			0,     // graph is valid from Server Version. 0 = All Versions; >0 = Valid from this version and above 
//			-1);   // minimum height

		// GRAPH
		addTrendGraph(GRAPH_NAME_MEM_AVAILABLE,
			"meminfo: Available Memory", 	                                // Menu CheckBox text
			"meminfo: Available Memory ("+GROUP_NAME+"->"+SHORT_NAME+")",    // Label 
			TrendGraphDataPoint.Y_AXIS_SCALE_LABELS_MB,
			new String[] { "MemAvailable in MB" }, 
			LabelType.Static,
			TrendGraphDataPoint.Category.MEMORY,
			false, // is Percent Graph
			false, // visible at start
			0,     // graph is valid from Server Version. 0 = All Versions; >0 = Valid from this version and above 
			-1);   // minimum height

		// GRAPH
		addTrendGraph(GRAPH_NAME_WIN_PAGING,
			"meminfo: Windows Paging or Swap Usage", 	                                // Menu CheckBox text
			"meminfo: Windows Paging or Swap Usage ("+GROUP_NAME+"->"+SHORT_NAME+")",    // Label 
			TrendGraphDataPoint.Y_AXIS_SCALE_LABELS_PERSEC,
			new String[] { "Pages/sec", "Pages Input/sec", "Pages Output/sec"}, 
			LabelType.Static,
			TrendGraphDataPoint.Category.MEMORY,
			false, // is Percent Graph
			false, // visible at start
			0,     // graph is valid from Server Version. 0 = All Versions; >0 = Valid from this version and above 
			-1);   // minimum height

		// GRAPH
		addTrendGraph(GRAPH_NAME_WIN_PAGING_FILE,
			"meminfo: Windows Paging File Usage", 	                                // Menu CheckBox text
			"meminfo: Windows Paging File Usage ("+GROUP_NAME+"->"+SHORT_NAME+")",    // Label 
			TrendGraphDataPoint.Y_AXIS_SCALE_LABELS_MB,
			new String[] { "Paging File(_Total) - % Usage", "Paging File(_Total) - % Usage Peak"}, 
			LabelType.Static,
			TrendGraphDataPoint.Category.MEMORY,
			true,  // is Percent Graph
			false, // visible at start
			0,     // graph is valid from Server Version. 0 = All Versions; >0 = Valid from this version and above 
			-1);   // minimum height
	}
	
	@Override
	public void updateGraphData(TrendGraphDataPoint tgdp)
	{
		if (GRAPH_NAME_MEM_USED.equals(tgdp.getName()))
		{
			if (isConnectedToVendor(OsVendor.Windows))
			{
				// FIXME: I don't know how to get "Bytes of memory in use"
			}
			else
			{
				Double[] arr = new Double[1];

				// NOTE: only ABS values are present in CounterModelHostMonitor
				Double memTotal = this.getAbsValueAsDouble("MemTotal", "used");
				Double memFree  = this.getAbsValueAsDouble("MemFree",  "used");

				// If no data: -1, otherwise: (memTotal - memFree) / 1024
				arr[0] = (memTotal != null && memFree != null) ? (memTotal - memFree) / 1024 : -1;

				tgdp.setDataPoint(this.getTimestamp(), arr);
			}
		}

//		if (GRAPH_NAME_MEM_FREE.equals(tgdp.getName()))
//		{
//			Double[] arr = new Double[1];
//
//			// NOTE: only ABS values are present in CounterModelHostMonitor
//			Double val = this.getAbsValueAsDouble("MemFree", "used");
//			arr[0] = (val == null) ? 0 : val / 1024;
//
//			tgdp.setDataPoint(this.getTimestamp(), arr);
//		}

		if (GRAPH_NAME_MEM_AVAILABLE.equals(tgdp.getName()))
		{
			if (isConnectedToVendor(OsVendor.Windows))
			{
				Double[] arr = new Double[1];

				// NOTE: only ABS values are present in CounterModelHostMonitor
				Double val = this.getAbsValueAsDouble(0, "Available MBytes");
				arr[0] = (val == null) ? 0 : val;

				tgdp.setDataPoint(this.getTimestamp(), arr);
			}
			else
			{
				Double[] arr = new Double[1];

				// NOTE: only ABS values are present in CounterModelHostMonitor
				Double val = this.getAbsValueAsDouble("MemAvailable", "used");
				arr[0] = (val == null) ? 0 : val / 1024;

				tgdp.setDataPoint(this.getTimestamp(), arr);
			}
		}

		if (GRAPH_NAME_WIN_PAGING.equals(tgdp.getName()))
		{
			if (isConnectedToVendor(OsVendor.Windows))
			{
				Double[] arr = new Double[3];

				arr[0] = this.getAbsValueAsDouble(0, "Pages/sec");
				arr[1] = this.getAbsValueAsDouble(0, "Pages Input/sec");
				arr[2] = this.getAbsValueAsDouble(0, "Pages Output/sec");

				tgdp.setDataPoint(this.getTimestamp(), arr);
			}
			else
			{
				// none
			}
		}

		if (GRAPH_NAME_WIN_PAGING_FILE.equals(tgdp.getName()))
		{
			if (isConnectedToVendor(OsVendor.Windows))
			{
				Double[] arr = new Double[2];

				arr[0] = this.getAbsValueAsDouble(0, "Paging File(_Total) - % Usage");
				arr[1] = this.getAbsValueAsDouble(0, "Paging File(_Total) - % Usage Peak");

				tgdp.setDataPoint(this.getTimestamp(), arr);
			}
			else
			{
				// none
			}
		}
	}

	@Override
	public void localCalculation(OsTable prevOsSampleTable, OsTable thisOsSampleTable)
	{
//		if (prevOsSampleTable == null)
//			return;

		// Windows do NOT (for the moment) have any localCalculations
		if (isConnectedToVendor(OsVendor.Windows))
		{
			return;
		}


		// Check rowcount
		if (thisOsSampleTable.getRowCount() == 0)
		{
			_logger.warn(getName() + ".localCalculation(OsTable) expected number of rows > 0, the table contains "+thisOsSampleTable.getRowCount()+" rows.");
			return;
		}
		
		// Check/get column position
		int pos_memoryType  = thisOsSampleTable.findColumn("memoryType");
		int pos_used        = thisOsSampleTable.findColumn("used");
		int pos_usedDiff    = thisOsSampleTable.findColumn("usedDiff");
		int pos_usedRate    = thisOsSampleTable.findColumn("usedRate");
		int pos_description = thisOsSampleTable.findColumn("description");

		if (    pos_memoryType  < 0 
		     || pos_used        < 0
		     || pos_usedDiff    < 0
		     || pos_usedRate    < 0
		     || pos_description < 0
		   )
		{
			_logger.warn(getName() + ".localCalculation(OsTable) could not find all desired columns"
				+ ". pos_memoryType="  + pos_memoryType
				+ ", pos_used="        + pos_used
				+ ", pos_usedDiff="    + pos_usedDiff
				+ ", pos_usedRate="    + pos_usedRate
				+ ", pos_description=" + pos_description
				);
			return;
		}

		// get value for "used" and put it in "usedDiff"
		for (int r=0; r<thisOsSampleTable.getRowCount(); r++)
		{
			if (prevOsSampleTable == null)
			{
				// Set to 0, instead of null on "first sample"
				thisOsSampleTable.setValueAt(new Long(0),       r, pos_usedDiff);
				thisOsSampleTable.setValueAt(new BigDecimal(0), r, pos_usedRate);
			}
			else
			{
				String thisPk = (String) thisOsSampleTable.getValueAt(r, pos_memoryType);
				try
				{
					// if prev sample is smaller than current row... get next row...
					if (prevOsSampleTable.getRowCount() < r)
						continue;
						
					// sometimes we get 'IndexOutOfBoundsException: Index: 46, Size: 46', on the below 'prevPk' get, which is *strange*... That's why the try/catch(RuntimeException) and logging some extra information.
					// the above '(prevOsSampleTable.getRowCount() < r)' should check for issues, so I don't know exactly what's wrong here...
					String prevPk   = (String) prevOsSampleTable.getValueAt(r, pos_memoryType);  
					Long   thisUsed = (Long)   thisOsSampleTable.getValueAt(r, pos_used);
					Long   prevUsed = (Long)   prevOsSampleTable.getRowByPk(prevPk).getValue(pos_used+1); // The model starts at pos 1
					String description = getFieldDescription(prevPk);
					
//					OsTableRow ostr = prevOsSampleTable.getRowByPk(pk);
//					System.out.println("ostr.pk='"+ostr.getPk()+"', val="+ostr.getValue(pos_used+1)); // The model starts at pos 1
//					Long   prevUsed = (Long)   ostr.getValue(pos_used+1);

					Long       diffVal = thisUsed - prevUsed;
					BigDecimal rateVal = new BigDecimal( (diffVal*1.0) / (getSampleInterval()/1000.0) ).setScale(1, BigDecimal.ROUND_HALF_EVEN);
					
//					System.out.println("############: getSampleInterval()="+getSampleInterval()+", pk="+StringUtil.left(pk,20)+", thisUsed="+thisUsed+", prevUsed="+prevUsed+", diffVal="+diffVal+", rateVal="+rateVal);

					thisOsSampleTable.setValueAt(diffVal,     r, pos_usedDiff);
					thisOsSampleTable.setValueAt(rateVal,     r, pos_usedRate);
					thisOsSampleTable.setValueAt(description, r, pos_description);
				}
				catch (RuntimeException ex)
				{
					_logger.warn("Problems doing local diff calculation for '" + getName() + "'. r=" + r + ", thisPk='" + thisPk + "', thisOsSampleTable.getRowCount()=" + thisOsSampleTable.getRowCount() + ", prevOsSampleTable.getRowCount()=" + prevOsSampleTable.getRowCount() + ". Skipping and continuing with next row.", ex);
				}
			}
		}
	}

	@Override
	public void localCalculation(OsTable osSampleTable)
	{
	}
	
	public static String getFieldDescription(String memoryType)
	{
		// below is from: https://access.redhat.com/solutions/406773

		//------------------------------------------
		// High Level statistics: 
		//------------------------------------------
		// RHEL 5, RHEL 6 and RHEL 7		
		if ("MemTotal"         .equalsIgnoreCase(memoryType)) return "Total usable memory";
		if ("MemFree"          .equalsIgnoreCase(memoryType)) return "The amount of physical memory not used by the system";
		if ("Buffers"          .equalsIgnoreCase(memoryType)) return "Memory in buffer cache, so relatively temporary storage for raw disk blocks. This shouldn't get very large.";
		if ("Cached"           .equalsIgnoreCase(memoryType)) return "Memory in the pagecache (Diskcache and Shared Memory)";
		if ("SwapCached"       .equalsIgnoreCase(memoryType)) return "Memory that is present within main memory, but also in the swapfile. (If memory is needed this area does not need to be swapped out AGAIN because it is already in the swapfile. This saves I/O and increases performance if machine runs short on memory.)";
		// RHEL 7 only
		if ("MemAvailable"     .equalsIgnoreCase(memoryType)) return "An estimate of how much memory is available for starting new applications, without swapping.";

		//------------------------------------------
		// Detailed Level statistics
		//------------------------------------------
		// RHEL 5, RHEL 6 and RHEL 7
		if ("Active"           .equalsIgnoreCase(memoryType)) return "Memory that has been used more recently and usually not swapped out or reclaimed";
		if ("Inactive"         .equalsIgnoreCase(memoryType)) return "Memory that has not been used recently and can be swapped out or reclaimed";

		// RHEL 6 and RHEL 7 only
		if ("Active(anon)"     .equalsIgnoreCase(memoryType)) return "Anonymous memory that has been used more recently and usually not swapped out";
		if ("Inactive(anon)"   .equalsIgnoreCase(memoryType)) return "Anonymous memory that has not been used recently and can be swapped out";
		if ("Active(file)"     .equalsIgnoreCase(memoryType)) return "Pagecache memory that has been used more recently and usually not reclaimed until needed";
		if ("Inactive(file)"   .equalsIgnoreCase(memoryType)) return "Pagecache memory that can be reclaimed without huge performance impact";
		if ("Unevictable"      .equalsIgnoreCase(memoryType)) return "Unevictable pages can't be swapped out for a variety of reasons";
		if ("Mlocked"          .equalsIgnoreCase(memoryType)) return "Pages locked to memory using the mlock() system call. Mlocked pages are also Unevictable.";

		//------------------------------------------
		// Memory statistics
		//------------------------------------------
		// RHEL 5, RHEL 6 and RHEL 7
		if ("SwapTotal"        .equalsIgnoreCase(memoryType)) return "Total swap space available";
		if ("SwapFree"         .equalsIgnoreCase(memoryType)) return "The remaining swap space available";
		if ("Dirty"            .equalsIgnoreCase(memoryType)) return "Memory waiting to be written back to disk";
		if ("Writeback"        .equalsIgnoreCase(memoryType)) return "Memory which is actively being written back to disk";
		if ("AnonPages"        .equalsIgnoreCase(memoryType)) return "Non-file backed pages mapped into userspace page tables";
		if ("Mapped"           .equalsIgnoreCase(memoryType)) return "Files which have been mmaped, such as libraries";
		if ("Slab"             .equalsIgnoreCase(memoryType)) return "In-kernel data structures cache";
		if ("PageTables"       .equalsIgnoreCase(memoryType)) return "Amount of memory dedicated to the lowest level of page tables. This can increase to a high value if a lot of processes are attached to the same shared memory segment.";
		if ("NFS_Unstable"     .equalsIgnoreCase(memoryType)) return "NFS pages sent to the server, but not yet commited to the storage";
		if ("Bounce"           .equalsIgnoreCase(memoryType)) return "Memory used for block device bounce buffers";
		if ("CommitLimit"      .equalsIgnoreCase(memoryType)) return "Based on the overcommit ratio (vm.overcommit_ratio), this is the total amount of memory currently available to be allocated on the system. This limit is only adhered to if strict overcommit accounting is enabled (mode 2 in vm.overcommit_memory).";
		if ("Committed_AS"     .equalsIgnoreCase(memoryType)) return "The amount of memory presently allocated on the system. The committed memory is a sum of all of the memory which has been allocated by processes, even if it has not been 'used' by them as of yet.";
		if ("VmallocTotal"     .equalsIgnoreCase(memoryType)) return "total size of vmalloc memory area";
		if ("VmallocUsed"      .equalsIgnoreCase(memoryType)) return "amount of vmalloc area which is used";
		if ("VmallocChunk"     .equalsIgnoreCase(memoryType)) return "largest contiguous block of vmalloc area which is free";
		if ("HugePages_Total"  .equalsIgnoreCase(memoryType)) return "Number of hugepages being allocated by the kernel (Defined with vm.nr_hugepages)";
		if ("HugePages_Free"   .equalsIgnoreCase(memoryType)) return "The number of hugepages not being allocated by a process";
		if ("HugePages_Rsvd"   .equalsIgnoreCase(memoryType)) return "The number of hugepages for which a commitment to allocate from the pool has been made, but no allocation has yet been made.";
		if ("Hugepagesize"     .equalsIgnoreCase(memoryType)) return "The size of a hugepage (usually 2MB on an Intel based system)";

		// RHEL 6 and RHEL 7 only
		if ("Shmem"            .equalsIgnoreCase(memoryType)) return "Total used shared memory (shared between several processes, thus including RAM disks, SYS-V-IPC and BSD like SHMEM)";
		if ("SReclaimable"     .equalsIgnoreCase(memoryType)) return "The part of the Slab that might be reclaimed (such as caches)";
		if ("SUnreclaim"       .equalsIgnoreCase(memoryType)) return "The part of the Slab that can't be reclaimed under memory pressure";
		if ("KernelStack"      .equalsIgnoreCase(memoryType)) return "The memory the kernel stack uses. This is not reclaimable.";
		if ("WritebackTmp"     .equalsIgnoreCase(memoryType)) return "Memory used by FUSE for temporary writeback buffers";
		if ("HardwareCorrupted".equalsIgnoreCase(memoryType)) return "The amount of RAM the kernel identified as corrupted / not working";
		if ("AnonHugePages"    .equalsIgnoreCase(memoryType)) return "Non-file backed huge pages mapped into userspace page tables";
		if ("HugePages_Surp"   .equalsIgnoreCase(memoryType)) return "The number of hugepages in the pool above the value in vm.nr_hugepages. The maximum number of surplus hugepages is controlled by vm.nr_overcommit_hugepages.";
		if ("DirectMap4k"      .equalsIgnoreCase(memoryType)) return "The amount of memory being mapped to standard 4k pages";
		if ("DirectMap2M"      .equalsIgnoreCase(memoryType)) return "The amount of memory being mapped to hugepages (usually 2MB in size)";

		return "";
	}

	@Override
	public void reset()
	{
		// Reset 5 minute average counters
		MovingAverageCounterManager.getInstance(this.getName(), "swapIn",  5).reset();
		MovingAverageCounterManager.getInstance(this.getName(), "swapOut", 5).reset();
		
		super.reset();
	}

	@Override
	public void sendAlarmRequest()
	{
		if ( ! AlarmHandler.hasInstance() )
			return;

		AlarmHandler alarmHandler = AlarmHandler.getInstance();
		
		CountersModel cm = this;
		String groupName = this.getName();

		if ( ! cm.hasAbsData() )
			return;
		if ( ! cm.getCounterController().isHostMonConnected() )
			return;

		String hostname = cm.getCounterController().getHostMonConnection().getHost();

		boolean debugPrint = Configuration.getCombinedConfiguration().getBooleanProperty("sendAlarmRequest.debug", _logger.isDebugEnabled());
		
		//-------------------------------------------------------
		// swapping -- Windows Only
		//-------------------------------------------------------
		if (isConnectedToVendor(OsVendor.Windows) && isSystemAlarmsForColumnEnabledAndInTimeRange("swapping"))
		{
			int    threshold        = Configuration.getCombinedConfiguration().getIntProperty(PROPKEY_alarm_swap, DEFAULT_alarm_swap);
			double maxCapMultiplier = Configuration.getCombinedConfiguration().getDoubleProperty(PROPKEY_alarm_swap_maxCap_multiplier, DEFAULT_alarm_swap_maxCap_multiplier);
			double maxCap = threshold * maxCapMultiplier; // default: 500 * 1.5

			// Get data
			Double swapIn_tmp  = this.getAbsValueAsDouble(0, "Pages Input/sec");
			Double swapOut_tmp = this.getAbsValueAsDouble(0, "Pages Output/sec");

			int swapIn  = (swapIn_tmp  == null) ? 0 : swapIn_tmp .intValue();
			int swapOut = (swapOut_tmp == null) ? 0 : swapOut_tmp.intValue();

			int swapIn_5mAvg  = (int) MovingAverageCounterManager.getInstance(groupName, "swapIn",  5).add(swapIn) .getAvg(0, true, maxCap);
			int swapOut_5mAvg = (int) MovingAverageCounterManager.getInstance(groupName, "swapOut", 5).add(swapOut).getAvg(0, true, maxCap);

//			int swapIn_15mAvg  = (int) MovingAverageCounterManager.getInstance(groupName, "swapIn",  15).add(swapIn) .getAvg(0, true, maxCap);
//			int swapOut_15mAvg = (int) MovingAverageCounterManager.getInstance(groupName, "swapOut", 15).add(swapOut).getAvg(0, true, maxCap);
//			int swapIn_60mAvg  = (int) MovingAverageCounterManager.getInstance(groupName, "swapIn",  60).add(swapIn) .getAvg(0, true, maxCap);
//			int swapOut_60mAvg = (int) MovingAverageCounterManager.getInstance(groupName, "swapOut", 60).add(swapOut).getAvg(0, true, maxCap);
			
			if (debugPrint || _logger.isDebugEnabled())
				System.out.println("##### sendAlarmRequest("+cm.getName()+"): swapping: in=" + swapIn + ", out=" + swapOut + ". swapIn_5mAvg=" + swapIn_5mAvg + ", swapOut_5mAvg=" + swapOut_5mAvg);

//			int threshold = Configuration.getCombinedConfiguration().getIntProperty(PROPKEY_alarm_swap, DEFAULT_alarm_swap);
//			if (swapIn > threshold || swapOut > threshold)
//			{
//				AlarmEventOsSwapping alarm = new AlarmEventOsSwapping(cm, threshold, hostname, swapIn, swapOut);
//				alarmHandler.addAlarm( alarm );
//			}

			if (swapIn_5mAvg > threshold || swapOut_5mAvg > threshold)
			{
				Timestamp swapIn_peakTs      = MovingAverageCounterManager.getInstance(groupName, "swapIn",  5).getPeakTimestamp();
				double    swapIn_peakNumber  = MovingAverageCounterManager.getInstance(groupName, "swapIn",  5).getPeakNumber();
				Timestamp swapOut_peakTs     = MovingAverageCounterManager.getInstance(groupName, "swapOut", 5).getPeakTimestamp();
				double    swapOut_peakNumber = MovingAverageCounterManager.getInstance(groupName, "swapOut", 5).getPeakNumber();

				// Create a small chart, that can be used in emails etc.
//				String htmlChartImage = MovingAverageChart.getChartAsHtmlImage("OS Swapping (15 minutes)", 
//						MovingAverageCounterManager.getInstance("swapIn",  15),  // Note make the chart on 15 minutes to see more info
//						MovingAverageCounterManager.getInstance("swapOut", 15)); // Note make the chart on 15 minutes to see more info
				String htmlChartImage = MovingAverageChart.getChartAsHtmlImage("OS Swapping (1 hour)", 
						MovingAverageCounterManager.getInstance(groupName, "swapIn",  60),  // Note make the chart on 15 minutes to see more info
						MovingAverageCounterManager.getInstance(groupName, "swapOut", 60)); // Note make the chart on 15 minutes to see more info

				AlarmEventOsSwapping alarm = new AlarmEventOsSwapping(cm, threshold, maxCap, hostname, "over 5 minute moving average", 
						swapIn_5mAvg,  swapIn_peakTs,  swapIn_peakNumber,
						swapOut_5mAvg, swapOut_peakTs, swapOut_peakNumber);
				
				alarm.setExtendedDescription(null, htmlChartImage);

				alarmHandler.addAlarm( alarm );
			}
		}
	}

	public static final String  PROPKEY_alarm_swap = CM_NAME + ".alarm.system.if.swap.gt";
	public static final int     DEFAULT_alarm_swap = 1000;

	public static final String  PROPKEY_alarm_swap_maxCap_multiplier = CM_NAME + ".alarm.system.swap.maxCap.multiplier";
	public static final double  DEFAULT_alarm_swap_maxCap_multiplier = 1.5d;

	@Override
	public List<CmSettingsHelper> getLocalAlarmSettings()
	{
		Configuration conf = Configuration.getCombinedConfiguration();
		List<CmSettingsHelper> list = new ArrayList<>();

		list.add(new CmSettingsHelper("Swapping", PROPKEY_alarm_swap , Integer.class, conf.getIntProperty(PROPKEY_alarm_swap , DEFAULT_alarm_swap), DEFAULT_alarm_swap, "If 'Pages Input/sec' or 'Pages Output/sec' is greater than ## (5 minute average), then send 'AlarmEventOsSwapping'. NOTE: This Alarm is only on Windows. (for Unix/Linux see 'CmOsVmstat')" ));

		return list;
	}

//-----------------------------------------------------------------------------
//-- MAYBE: Do alarm when MemAvailable is getting to low... (need algo for this)
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
//		boolean debugPrint = Configuration.getCombinedConfiguration().getBooleanProperty("sendAlarmRequest.debug", _logger.isDebugEnabled());
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
