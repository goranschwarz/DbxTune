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
		this(-1);
	}
	public MonitorVmstatLinux(int utilVersion)
	{
		super(utilVersion);
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
	public HostMonitorMetaData createMetaData(int utilVersion)
	{
		HostMonitorMetaData md = new HostMonitorMetaData();
		md.setTableName(getModuleName());

		_logger.info("When creating meta data for Linux 'vmstat', initializing it using utility version "+VersionShort.toStr(utilVersion));
		
//		if ( utilVersion >= VersionShort.toInt(99,99,99) || utilVersion == -1) // -1 is not defined or "offline" mode... so choose the type with most columns (in the future might save the utilVersion in the offline database)
//		{
//			// Latest version should be in here...
//		}
//		else
//		{
			md.addDatetimeColumn("sampleTime", 1,  0, true, "Approximately when this record was samples");

			md.addIntColumn("procs_r",      2,  1, true, "The number of processes waiting for run time.");
			md.addIntColumn("procs_b",      3,  2, true, "The number of processes in uninterruptible sleep.");

			md.addIntColumn("memory_swpd",  4,  3, true, "the amount of virtual memory used.");
			md.addIntColumn("memory_free",  5,  4, true, "the amount of idle memory.");
			md.addIntColumn("memory_buff",  6,  5, true, "the amount of memory used as buffers.");
			md.addIntColumn("memory_cache", 7,  6, true, "the amount of memory used as cache.");

			md.addIntColumn("swap_si",      8,  7, true, "Amount of memory swapped in from disk (/s).");
			md.addIntColumn("swap_so",      9,  8, true, "Amount of memory swapped to disk (/s).");

			md.addIntColumn("io_bi",       10,  9, true, "Blocks received from a block device (blocks/s).");
			md.addIntColumn("io_bo",       11, 10, true, "Blocks sent to a block device (blocks/s).");

			md.addIntColumn("system_in",   12, 11, true, "The number of interrupts per second, including the clock.");
			md.addIntColumn("system_cs",   13, 12, true, "The number of context switches per second.");

			md.addIntColumn("cpu_us",      14, 13, true, "Time spent running non-kernel code. (user time, including nice time)");
			md.addIntColumn("cpu_sy",      15, 14, true, "Time spent running kernel code. (system time)");
			md.addIntColumn("cpu_id",      16, 15, true, "Time spent idle. Prior to Linux 2.5.41, this includes IO-wait time.");
			md.addIntColumn("cpu_wa",      17, 16, true, "Time spent waiting for IO. Prior to Linux 2.5.41, included in idle.");
			md.addIntColumn("cpu_st",      18, 17, true, "Time stolen from a virtual machine. Prior to Linux 2.6.11, unknown.");
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
