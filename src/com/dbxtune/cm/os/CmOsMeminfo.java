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
import java.math.BigDecimal;
import java.math.RoundingMode;
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
import com.dbxtune.cm.os.gui.CmOsMeminfoPanel;
import com.dbxtune.graph.TrendGraphDataPoint;
import com.dbxtune.graph.TrendGraphDataPoint.LabelType;
import com.dbxtune.gui.MainFrame;
import com.dbxtune.gui.TabularCntrPanel;
import com.dbxtune.hostmon.HostMonitor.OsVendor;
import com.dbxtune.hostmon.OsTable;
import com.dbxtune.utils.Configuration;
import com.dbxtune.utils.MovingAverageChart;
import com.dbxtune.utils.MovingAverageCounterManager;

public class CmOsMeminfo
extends CounterModelHostMonitor
{
	private static final Logger _logger = LogManager.getLogger(MethodHandles.lookup().lookupClass());
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

	
// TODO: Look at the following places to get ideas for Memory Usage [Linux/Windows]
//			Linux   -- https://github.com/postgrespro/mamonsu/blob/master/mamonsu/plugins/system/linux/memory.py	
//			Windows -- https://github.com/postgrespro/mamonsu/blob/master/mamonsu/plugins/system/windows/memory.py	

 /*  Linux   -- https://github.com/postgrespro/mamonsu/blob/master/mamonsu/plugins/system/linux/memory.py
        ("active"      , "Active"      , "Active - Memory Recently Used"                 , "BAEB6B", 1),
        ("available"   , "MemAvailable", "Available - Free Memory"                       , "00CC00", 1),
        ("buffers"     , "Buffers"     , "Buffers - Block Device Cache and Dirty"        , "00B0B8", 1),
        ("cached"      , "Cached"      , "Cached - Parked File Data (file content) Cache", "52768F", 1),
        ("committed"   , "Committed_AS", "Committed AS - Total Committed Memory"         , "9C8A4E", 1),
        ("inactive"    , "Inactive"    , "Inactive - Memory Not Currently Used"          , "A39B98", 1),
        ("mapped"      , "Mapped"      , "Mapped - All mmap()ed Pages"                   , "9F1E28", 1),
        ("page_tables" , "PageTables"  , "PageTables - Map bt Virtual and Physical"      , "793F5D", 1),
        ("slab"        , "Slab"        , "Slab - Kernel Used Memory (inode cache)"       , "F6CB93", 1),
        ("swap"        , None          , "Swap - Swap Space Used"                        , "006AAE", 1),
        ("swap_cache"  , "SwapCached"  , "SwapCached - Fetched unmod Yet Swap Pages"     , "87C2B9", 1),
        ("total"       , "MemTotal"    , "Total - All Memory"                            , "FF5656", 4),
        ("unused"      , "MemFree"     , "Unused - Wasted Memory"                        , "3B415A", 1),
        ("used"        , None          , "Used - User-Space Applications"                , "001219", 1),
        ("vmalloc_used", "VmallocUsed" , "VMallocUsed - vmaloc() Allocated by Kernel"    , "CF6518", 1)

        for item in self.Items:
            zbx_key, meminfo_key = item[0], item[1]
            if meminfo_key is not None:
                result[zbx_key] = meminfo.get(meminfo_key) or 0
        used = meminfo["MemTotal"] - result["unused"] - result["buffers"] - result["cached"] - result["slab"] - result["page_tables"] - result["swap_cache"]
        result["used"] = used if used > 0 else 0
        result["swap"] = (meminfo.get("SwapTotal") or 0) - (meminfo.get("SwapFree") or 0)
 */

 /*  Windows   -- https://github.com/postgrespro/mamonsu/blob/master/mamonsu/plugins/system/windows/memory.py
        (r"\Memory\Cache Bytes",                 "[cache]",     "Memory Cached",    Plugin.UNITS.bytes, ("9C8A4E", 0)),
        (r"\Memory\Available Bytes",             "[available]", "Memory Available", Plugin.UNITS.bytes, ("00CC00", 0)),
        (r"\Memory\Free & Zero Page List Bytes", "[free]",      "Memory Free",      Plugin.UNITS.bytes, ("3B415A", 0))
 */
	
	
	
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
			TrendGraphDataPoint.createGraphProps(TrendGraphDataPoint.Y_AXIS_SCALE_LABELS_MB, CentralPersistReader.SampleType.MAX_OVER_SAMPLES),
			new String[] { "MemUsed in MB" }, 
			LabelType.Static,
			TrendGraphDataPoint.Category.MEMORY,
			false, // is Percent Graph
			false, // visible at start
			0,     // graph is valid from Server Version. 0 = All Versions; >0 = Valid from this version and above 
			-1);   // minimum height

		// GRAPH
		addTrendGraph(GRAPH_NAME_MEM_AVAILABLE,
			"meminfo: Available Memory", 	                                // Menu CheckBox text
			"meminfo: Available Memory ("+GROUP_NAME+"->"+SHORT_NAME+")",    // Label 
			TrendGraphDataPoint.createGraphProps(TrendGraphDataPoint.Y_AXIS_SCALE_LABELS_MB, CentralPersistReader.SampleType.MIN_OVER_SAMPLES),
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
			TrendGraphDataPoint.createGraphProps(TrendGraphDataPoint.Y_AXIS_SCALE_LABELS_PERSEC, CentralPersistReader.SampleType.MAX_OVER_SAMPLES),
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
			TrendGraphDataPoint.createGraphProps(TrendGraphDataPoint.Y_AXIS_SCALE_LABELS_MB, CentralPersistReader.SampleType.MAX_OVER_SAMPLES),
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
				thisOsSampleTable.setValueAt(Long.valueOf(0),       r, pos_usedDiff);
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
					BigDecimal rateVal = new BigDecimal( (diffVal*1.0) / (getSampleInterval()/1000.0) ).setScale(1, RoundingMode.HALF_EVEN);
					
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
				htmlChartImage += CmOsPs.getCmOsPs_getGraphDataHistoryAsHtmlImage(getCounterController(), CmOsPs.GRAPH_NAME_WIN_PS);
				htmlChartImage += CmOsPs.getCmOsPs_asHtmlTable(getCounterController(), 15);
				
				AlarmEventOsSwapping alarm = new AlarmEventOsSwapping(cm, threshold, maxCap, hostname, "over " + MOVING_AVG_TIME_IN_MINUTES + " minute moving average", 
						swapIn_xmAvg,  swapIn_peakTs,  swapIn_peakNumber,
						swapOut_xmAvg, swapOut_peakTs, swapOut_peakNumber);
				
				alarm.setExtendedDescription(null, htmlChartImage);

				alarmHandler.addAlarm( alarm );
			}
		}

		//-------------------------------------------------------
		// SwapThrashing -- Windows Only (both Page In AND Page Out at the same time)
		//-------------------------------------------------------
		if (isConnectedToVendor(OsVendor.Windows) && isSystemAlarmsForColumnEnabledAndInTimeRange("SwapThrashing"))
		{
			int    threshold        = Configuration.getCombinedConfiguration().getIntProperty(PROPKEY_alarm_swap_thrashing, DEFAULT_alarm_swap_thrashing);
			double maxCapMultiplier = Configuration.getCombinedConfiguration().getDoubleProperty(PROPKEY_alarm_swap_thrashing_maxCap_multiplier, DEFAULT_alarm_swap_thrashing_maxCap_multiplier);
			double maxCap = threshold * maxCapMultiplier; // default: 150 * 2.0

			// Get data
			Double swapIn_tmp  = this.getAbsValueAsDouble(0, "Pages Input/sec");
			Double swapOut_tmp = this.getAbsValueAsDouble(0, "Pages Output/sec");

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
				htmlChartImage += CmOsPs.getCmOsPs_getGraphDataHistoryAsHtmlImage(getCounterController(), CmOsPs.GRAPH_NAME_WIN_PS);
				htmlChartImage += CmOsPs.getCmOsPs_asHtmlTable(getCounterController(), 15);
				
				AlarmEventOsSwapThrashing alarm = new AlarmEventOsSwapThrashing(cm, threshold, maxCap, hostname, "over " + MOVING_AVG_TIME_IN_MINUTES + " minute moving average", 
						swapIn_xmAvg,  swapIn_peakTs,  swapIn_peakNumber,
						swapOut_xmAvg, swapOut_peakTs, swapOut_peakNumber);
				
				alarm.setExtendedDescription(null, htmlChartImage);

				alarmHandler.addAlarm( alarm );
			}
		}
	}

	public static final String  PROPKEY_alarm_swap                             = CM_NAME + ".alarm.system.if.swap.gt"; // Pages in OR out
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

		list.add(new CmSettingsHelper("Swapping"     , isAlarmSwitch  , PROPKEY_alarm_swap                             , Integer.class, conf.getIntProperty   (PROPKEY_alarm_swap                             , DEFAULT_alarm_swap                            ), DEFAULT_alarm_swap                            , "If 'Pages Input/sec' or 'Pages Output/sec' is greater than ## (" + MOVING_AVG_TIME_IN_MINUTES + " minute average), then send 'AlarmEventOsSwapping'. NOTE: This Alarm is only on Windows. (for Unix/Linux see 'CmOsVmstat')" ));
		list.add(new CmSettingsHelper("Swapping MaxCapMultiplier"     , PROPKEY_alarm_swap_maxCap_multiplier           , Double .class, conf.getDoubleProperty(PROPKEY_alarm_swap_maxCap_multiplier           , DEFAULT_alarm_swap_maxCap_multiplier          ), DEFAULT_alarm_swap_maxCap_multiplier          , "Parameter to 'Swapping', which sets a top limit (max cap), values above this does only count as the 'maxCap' value. so if the 'theshold' is set to 1000 and 'MaxCap Multiplier' is '1.5' The MaxCap will be 1500..." ));

		list.add(new CmSettingsHelper("SwapThrashing", isAlarmSwitch  , PROPKEY_alarm_swap_thrashing                   , Integer.class, conf.getIntProperty   (PROPKEY_alarm_swap_thrashing                   , DEFAULT_alarm_swap_thrashing)                  , DEFAULT_alarm_swap_thrashing                  , "If 'Pages Input/sec' AND 'Pages Output/sec' is greater than ## (" + MOVING_AVG_TIME_IN_MINUTES + " minute average), then send 'AlarmEventOsSwapThrashing'. NOTE: This Alarm is only on Windows. (for Unix/Linux see 'CmOsVmstat')" ));
		list.add(new CmSettingsHelper("SwapThrashing MaxCapMultiplier", PROPKEY_alarm_swap_thrashing_maxCap_multiplier , Double .class, conf.getDoubleProperty(PROPKEY_alarm_swap_thrashing_maxCap_multiplier , DEFAULT_alarm_swap_thrashing_maxCap_multiplier), DEFAULT_alarm_swap_thrashing_maxCap_multiplier, "Parameter to 'SwapThrashing', which sets a top limit (max cap), values above this does only count as the 'maxCap' value. so if the 'theshold' is set to 150 and 'MaxCap Multiplier' is '2.0' The MaxCap will be 300..." ));

		return list;
	}
}
