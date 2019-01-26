package com.asetune.hostmon;

import org.apache.log4j.Logger;

import com.asetune.utils.Configuration;
import com.asetune.utils.VersionShort;

public class MonitorVmstatLinux
extends MonitorVmstat
{
	private static Logger _logger = Logger.getLogger(MonitorVmstatLinux.class);

	public MonitorVmstatLinux()
	{
		this(-1, null);
	}
	public MonitorVmstatLinux(int utilVersion, String utilExtraInfo)
	{
		super(utilVersion, utilExtraInfo);
	}

	@Override
	public String getModuleName()
	{
		return "MonitorVmstatLinux";
	}

	@Override
	public String getCommand()
	{
		String cmd = super.getCommand();
		return cmd != null ? cmd : "vmstat "+getSleepTime();
	}

	@Override
	public HostMonitorMetaData createMetaData(int utilVersion, Configuration utilExtraInfo)
	{
		HostMonitorMetaData md = new HostMonitorMetaData();
		md.setTableName(getModuleName());

		_logger.info("When creating meta data for Linux 'vmstat', initializing it using utility version "+VersionShort.toStr(utilVersion)+" (intVer="+utilVersion+").");

		//--------------------------------------------------------
		// the below extra descriptions was grabbed from - http://www.lazysystemadmin.com/2011/04/understanding-vmstat-output-explained.html
		//--------------------------------------------------------

		String noteOnBlocks = "<br><b>Note:</b> the memory, swap, and I/O statistics are in blocks, not in bytes. In Linux, blocks are usually 4,096 bytes (4 KB). <br>"
		                    + "You can get/check memory pagesize with 'getconf PAGE_SIZE', although the FileSystem block size is specified in each FS, but it's usually also 4K.<br>";

		String sampleTime_tooltip = "<html>Approximately when this record was samples</html>";

		String procs_r_tooltip = "<html>The number of processes waiting for run time.<br>       How many processes are waiting for CPU time.</html>";
		String procs_b_tooltip = "<html>The number of processes in uninterruptible sleep.<br>   Wait Queue - Process which are waiting for I/O (disk, network, user input,etc..)</html>";
		
		String memory_swpd_tooltip = "<html>the amount of virtual memory used.<br>      Shows how many blocks are swapped out to disk (paged). Total Virtual memory usage. <br> <b>Note:</b> you can see the swap area configured in server using 'cat /proc/swaps'<br>"+noteOnBlocks+"</html>";
		String memory_free_tooltip = "<html>the amount of idle memory.<br>              Idle Memory.<br>"+noteOnBlocks+"</html>";
		String memory_buff_tooltip = "<html>the amount of memory used as buffers.<br>   Memory used as buffers, like before/after I/O operations.<br>"+noteOnBlocks+"</html>";
		String memory_cache_tooltip = "<html>the amount of memory used as cache.<br>    Memory used as cache by the Operating System.<br>"+noteOnBlocks+"</html>";
		
		String swap_si_tooltip = "<html>Amount of memory swapped in from disk (/s).<br>   How many blocks per second the operating system is swapping in. <br>i.e Memory swapped in from the disk (Read from swap area to Memory)<br>           Note: In Ideal condition, We like to see si and so at 0 most of the time, and we definitely don’t like to see more than 10 blocks per second.<br>"+noteOnBlocks+"</html>";
		String swap_so_tooltip = "<html>Amount of memory swapped to disk (/s).<br>        How many blocks per second the operating system is swaped Out.  <br>i.e Memory swapped to the disk (Written to swap area and cleared from Memory)<br> Note: In Ideal condition, We like to see si and so at 0 most of the time, and we definitely don’t like to see more than 10 blocks per second.<br>"+noteOnBlocks+"</html>";
		
		String io_bi_tooltip = "<html>Blocks received from a block device (blocks/s).<br>   Blocks received from block device - Read (like a hard disk).<br>"+noteOnBlocks+"</html>";
		String io_bo_tooltip = "<html>Blocks sent to a block device (blocks/s).<br>         Blocks sent to a block device - Write.<br>"+noteOnBlocks+"</html>";
		
		String system_in_tooltip = "<html>The number of interrupts per second, including the clock.<br>   </html>";
		String system_cs_tooltip = "<html>The number of context switches per second.<br>                  </html>";
		
		String cpu_us_tooltip = "<html>Time spent running non-kernel code. (user time, including nice time)<br>        </html>";
		String cpu_sy_tooltip = "<html>Time spent running kernel code. (system time - network, IO interrupts, etc)<br> </html>";
		String cpu_id_tooltip = "<html>Time spent idle. Prior to Linux 2.5.41, this includes IO-wait time.<br>         </html>";
		String cpu_wa_tooltip = "<html>Time spent waiting for IO. Prior to Linux 2.5.41, included in idle.<br>         </html>";
		String cpu_st_tooltip = "<html>Time stolen from a virtual machine. Prior to Linux 2.6.11, unknown.<br>         </html>";
		

//		if ( utilVersion >= VersionShort.toInt(99,99,99) || utilVersion == -1) // -1 is not defined or "offline" mode... so choose the type with most columns (in the future might save the utilVersion in the offline database)
//		{
//			// Latest version should be in here...
//		}
//		else
//		{
			md.addDatetimeColumn("sampleTime", 1,  0, true, sampleTime_tooltip);

			md.addIntColumn("procs_r",      2,  1, true, procs_r_tooltip);
			md.addIntColumn("procs_b",      3,  2, true, procs_b_tooltip);

			md.addIntColumn("memory_swpd",  4,  3, true, memory_swpd_tooltip);
			md.addIntColumn("memory_free",  5,  4, true, memory_free_tooltip);
			md.addIntColumn("memory_buff",  6,  5, true, memory_buff_tooltip);
			md.addIntColumn("memory_cache", 7,  6, true, memory_cache_tooltip);

			md.addIntColumn("swap_si",      8,  7, true, swap_si_tooltip);
			md.addIntColumn("swap_so",      9,  8, true, swap_so_tooltip);

			md.addIntColumn("io_bi",       10,  9, true, io_bi_tooltip);
			md.addIntColumn("io_bo",       11, 10, true, io_bo_tooltip);

			md.addIntColumn("system_in",   12, 11, true, system_in_tooltip);
			md.addIntColumn("system_cs",   13, 12, true, system_cs_tooltip);

			md.addIntColumn("cpu_us",      14, 13, true, cpu_us_tooltip);
			md.addIntColumn("cpu_sy",      15, 14, true, cpu_sy_tooltip);
			md.addIntColumn("cpu_id",      16, 15, true, cpu_id_tooltip);
			md.addIntColumn("cpu_wa",      17, 16, true, cpu_wa_tooltip);
			md.addIntColumn("cpu_st",      18, 17, true, cpu_st_tooltip);
//		}

		// Set Percent columns
		md.setPercentCol("cpu_us");
		md.setPercentCol("cpu_sy");
		md.setPercentCol("cpu_id");
		md.setPercentCol("cpu_wa");
		md.setPercentCol("cpu_st");
		

		// Set column "sampleTime", to a special status, which will contain the  
		// underlying sample TIME the record was sampled
		md.setStatusCol("sampleTime", HostMonitorMetaData.STATUS_COL_SAMPLE_TIME);

		// What regexp to use to split the input row into individual fields
		md.setParseRegexp(HostMonitorMetaData.REGEXP_IS_SPACE);

		// Skip the header line
		md.setSkipRows("memory_swpd", "swpd");

		// Get SKIP and ALLOW from the Configuration
		md.setSkipAndAllowRows(null, Configuration.getCombinedConfiguration());
		md.setSkipAndAllowRows("hostmon.MonitorVmstat.", Configuration.getCombinedConfiguration());

		return md;
	}

	@Override
	public String[] parseRow(HostMonitorMetaData md, String row, String[] preParsed, int type)
	{
		// The column "cpu_st", seems to be missing from some Linux versions...
		// Then ADD this columns in, with a 0/zero value
		if (preParsed.length + 1 == md.getParseColumnCount())
		{
			// Copy "preParsed" array into new "tmp" array
			// Then add last entry with "0"
			String[] tmp = new String[preParsed.length + 1];
			System.arraycopy(preParsed, 0, tmp, 0, preParsed.length);
			tmp[tmp.length - 1] = "0";
			
			preParsed = tmp;
		}

		// Now call the ordinary parseRow()
		return super.parseRow(md, row, preParsed, type);
	}
	
}
