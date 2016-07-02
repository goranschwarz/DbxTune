package com.asetune.hostmon;

import com.asetune.utils.Configuration;

public class MonitorVmstatHp
extends MonitorVmstat
{
	public MonitorVmstatHp()
	{
		this(-1);
	}
	public MonitorVmstatHp(int utilVersion)
	{
		super(utilVersion);
	}

	@Override
	public String getModuleName()
	{
		return "MonitorVmstatHp";
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

		md.addDatetimeColumn("sampleTime", 1,  0, true, "Approximately when this record was samples");

		md.addIntColumn("procs_r",      2,  1, true, "In run queue");
		md.addIntColumn("procs_b",      3,  2, true, "Blocked for resources (I/O, paging, etc.)");
		md.addIntColumn("procs_w",      4,  3, true, "Runnable or short sleeper (< 20 secs) but swapped");

		md.addIntColumn("memory_avm",   5,  4, true, "Active virtual pages");
		md.addIntColumn("memory_free",  6,  5, true, "Size of the free list");

		md.addIntColumn("page_re",      7,  6, true, "Page reclaims (without -S)");
		md.addIntColumn("page_at",      8,  7, true, "Address translation faults (without -S)");
//		md.addIntColumn("page_si",      7,  6, true, "Processes swapped in (with -S)");
//		md.addIntColumn("page_so",      8,  7, true, "Processes swapped out (with -S)");
		md.addIntColumn("page_pi",      9,  8, true, "Pages paged in");
		md.addIntColumn("page_po",     10,  9, true, "Pages paged out");
		md.addIntColumn("page_fr",     11, 10, true, "Pages freed per second");
		md.addIntColumn("page_de",     12, 11, true, "Anticipated short term memory shortfall");
		md.addIntColumn("page_sr",     13, 12, true, "Pages scanned by clock algorithm, per second");

		md.addIntColumn("faults_in",   14, 13, true, "Device interrupts per second (nonclock)");
		md.addIntColumn("faults_sy",   15, 14, true, "System calls per second");
		md.addIntColumn("faults_cs",   16, 15, true, "CPU context switch rate (switches/sec)");

		md.addIntColumn("cpu_us",      17, 16, true, "User time for normal and low priority processes");
		md.addIntColumn("cpu_sy",      18, 17, true, "System time");
		md.addIntColumn("cpu_id",      19, 18, true, "CPU idle");

		// Set Percent columns
		md.setPercentCol("cpu_us");
		md.setPercentCol("cpu_sy");
		md.setPercentCol("cpu_id");
		

		// Set column "sampleTime", to a special status, which will contain the  
		// underlying sample TIME the record was sampled
		md.setStatusCol("sampleTime", HostMonitorMetaData.STATUS_COL_SAMPLE_TIME);

		// What regexp to use to split the input row into individual fields
		md.setParseRegexp(HostMonitorMetaData.REGEXP_IS_SPACE);

		// Skip the header line
		md.setSkipRows("memory_free", "free");

		// Get SKIP and ALLOW from the Configuration
		md.setSkipAndAllowRows(null, Configuration.getCombinedConfiguration());
		md.setSkipAndAllowRows("hostmon.MonitorVmstat.", Configuration.getCombinedConfiguration());

		return md;
	}
}
