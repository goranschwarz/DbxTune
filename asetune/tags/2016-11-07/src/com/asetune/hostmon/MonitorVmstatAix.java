package com.asetune.hostmon;

import com.asetune.utils.Configuration;

public class MonitorVmstatAix
extends MonitorVmstat
{
	public MonitorVmstatAix()
	{
		this(-1);
	}
	public MonitorVmstatAix(int utilVersion)
	{
		super(utilVersion);
	}

	@Override
	public String getModuleName()
	{
		return "MonitorVmstatAix";
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

		md.addIntColumn("kthr_r",          2,  1, true, "Number of kernel threads placed in run queue.");
		md.addIntColumn("kthr_b",          3,  2, true, "Number of kernel threads placed in wait queue (awaiting resource, awaiting input/output).");

		md.addIntColumn("memory_avm",      4,  3, true, "Active virtual pages.");
		md.addIntColumn("memory_free",     5,  4, true, "Size of the free list. Note: A large portion of real memory is utilized as a cache for file system data. It is not unusual for the size of the free list to remain small.");

		md.addIntColumn("page_re",         6,  5, true, "Pager input/output list.");
		md.addIntColumn("page_pi",         7,  6, true, "Pages paged in from paging space.");
		md.addIntColumn("page_po",         8,  7, true, "Pages paged out to paging space.");
		md.addIntColumn("page_fr",         9,  8, true, "Pages freed (page replacement).");
		md.addIntColumn("page_sr",        10,  9, true, "Pages scanned by page-replacement algorithm.");
		md.addIntColumn("page_cy",        11, 10, true, "Clock cycles by page-replacement algorithm.");

		md.addIntColumn("faults_in",      12, 11, true, "Device interrupts.");
		md.addIntColumn("faults_sy",      13, 12, true, "System calls.");
		md.addIntColumn("faults_cs",      14, 13, true, "Kernel thread context switches.");

		md.addIntColumn("cpu_us",         15, 14, true, "User time.");
		md.addIntColumn("cpu_sy",         16, 15, true, "System time.");
		md.addIntColumn("cpu_id",         17, 16, true, "CPU idle time.");
		md.addIntColumn("cpu_wa",         18, 17, true, "CPU idle time during which the system had outstanding disk/NFS I/O request(s).");

		// Set Percent columns
		md.setPercentCol("cpu_us");
		md.setPercentCol("cpu_sy");
		md.setPercentCol("cpu_id");
		md.setPercentCol("cpu_wa");
		
		// Set column "sampleTime", to a special status, which will contain the  
		// underlying sample TIME the record was sampled
		md.setStatusCol("sampleTime", HostMonitorMetaData.STATUS_COL_SAMPLE_TIME);

		// What regexp to use to split the input row into individual fields
		md.setParseRegexp(HostMonitorMetaData.REGEXP_IS_SPACE);

		// Skip the header line
		md.setSkipRows("memory_avm", "avm");

		// Get SKIP and ALLOW from the Configuration
		md.setSkipAndAllowRows(null, Configuration.getCombinedConfiguration());
		md.setSkipAndAllowRows("hostmon.MonitorVmstat.", Configuration.getCombinedConfiguration());

		return md;
	}
}