package asemon.hostmon;

import asemon.utils.Configuration;


public class MonitorMpstatSolaris
extends MonitorVmstat
{
	@Override
	public String getModuleName()
	{
		return "MonitorMpstatSolaris";
	}

	@Override
	public String getCommand()
	{
		String cmd = super.getCommand();
		return cmd != null ? cmd : "mpstat "+getSleepTime();
	}

	@Override
	public HostMonitorMetaData createMetaData()
	{
		HostMonitorMetaData md = new HostMonitorMetaData();
		md.setTableName(getModuleName());

		md.addStrColumn( "CPU",      1,  1, false,   5,  "Without the -a option, mpstat reports CPU statistics for a processor ID. With the -a option, mpstat reports SET statistics for a processor set ID.");
		md.addIntColumn( "samples",  2,  0, true,        "Number of 'sub' sample entries of iostat this value is based on");

		md.addStatColumn("minf",     3,  2, true, 7,  1, "minor faults");
		md.addStatColumn("mjf",      4,  3, true, 7,  1, "major faults");
		md.addStatColumn("xcal",     5,  4, true, 7,  1, "inter-processor cross-calls");
		md.addStatColumn("intr",     6,  5, true, 7,  1, "interrupts");
		md.addStatColumn("ithr",     7,  6, true, 7,  1, "interrupts as threads (not counting clock interrupt)");
		md.addStatColumn("csw",      8,  7, true, 7,  1, "context switches");
		md.addStatColumn("icsw",     9,  8, true, 7,  1, "involuntary context switches");
		md.addStatColumn("migr",    10,  9, true, 7,  1, "thread migrations (to another processor)");
		md.addStatColumn("smtx",    11, 10, true, 7,  1, "spins on mutexes (lock not acquired on first try)");
		md.addStatColumn("srw",     12, 11, true, 7,  1, "spins on readers/writer locks (lock not acquired on first try)");
		md.addStatColumn("syscl",   13, 12, true, 7,  1, "system calls");
		md.addStatColumn("usr",     14, 13, true, 7,  1, "percent user time");
		md.addStatColumn("sys",     15, 14, true, 7,  1, "percent system time");
		md.addStatColumn("wt",      16, 15, true, 7,  1, "the I/O wait time is no longer calculated as a percentage of CPU time, and this statistic will always return zero.");
		md.addStatColumn("idl",     17, 16, true, 7,  1, "percent idle time");
//		md.addStatColumn("sze",     18, 17, true, 7,  1, "number of processors in the requested processor set");

		// Set Percent columns
		md.setPercentCol("usr");
		md.setPercentCol("sys");
		md.setPercentCol("wt");
		md.setPercentCol("idl");

		// Use "CPU" as the Primary Key, which is used to du summary/average calculations
		md.setPkCol("CPU");

		// Set column "samples", to a special status, which will contain number of 
		// underlying samples the summary/average caclulation was based on
		md.setStatusCol("samples",    HostMonitorMetaData.STATUS_COL_SUB_SAMPLE);

		// What regexp to use to split the input row into individual fields
		md.setParseRegexp(HostMonitorMetaData.REGEXP_IS_SPACE);

		// Skip the header line
		md.setSkipRows("CPU", "CPU");

		// Get SKIP and ALLOW from the Configuration
		md.setSkipAndAllowRows(null, Configuration.getCombinedConfiguration());
		md.setSkipAndAllowRows("hostmon.MonitorMpstat.", Configuration.getCombinedConfiguration());

		return md;
	}
}
