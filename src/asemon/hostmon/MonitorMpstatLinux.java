package asemon.hostmon;

import asemon.utils.Configuration;


public class MonitorMpstatLinux
extends MonitorVmstat
{
	@Override
	public String getModuleName()
	{
		return "MonitorMpstatLinux";
	}

	@Override
	public String getCommand()
	{
		String cmd = super.getCommand();
		return cmd != null ? cmd : "mpstat -P ALL "+getSleepTime();
	}

	@Override
	public HostMonitorMetaData createMetaData()
	{
		HostMonitorMetaData md = new HostMonitorMetaData();
		md.setTableName(getModuleName());

		md.addStrColumn( "CPU",            1,  3, false,   5, "Processor number. The keyword all indicates that statistics are calculated as averages among all processors.");
		md.addIntColumn( "samples",        2,  0, true,        "Number of 'sub' sample entries of iostat this value is based on");

		md.addStatColumn("usrPct",         3,  4, true, 5,  1, "Show the percentage of CPU utilization that occurred while executing at the user level (application).");
		md.addStatColumn("nicePct",        4,  5, true, 5,  1, "Show the percentage of CPU utilization that occurred while executing at the user level with nice priority.");
		md.addStatColumn("sysPct",         5,  6, true, 5,  1, "Show the percentage of CPU utilization that occurred while executing at the system level (kernel). Note that this does not include time spent servicing hardware and software interrupts.");
		md.addStatColumn("iowaitPct",      6,  7, true, 5,  1, "Show the percentage of time that the CPU or CPUs were idle during which the system had an outstanding disk I/O request.");
		md.addStatColumn("irqPct",         7,  8, true, 5,  1, "Show the percentage of time spent by the CPU or CPUs to service hardware interrupts.");
		md.addStatColumn("softPct",        8,  9, true, 5,  1, "Show the percentage of time spent by the CPU or CPUs to service software interrupts.");
		md.addStatColumn("stealPct",       9, 10, true, 5,  1, "Show the percentage of time spent in involuntary wait by the virtual CPU or CPUs while the hypervisor was servicing another virtual processor.");
		md.addStatColumn("guestPct",      10, 11, true, 5,  1, "Show the percentage of time spent by the CPU or CPUs to run a virtual processor.");
		md.addStatColumn("idlePct",       11, 12, true, 5,  1, "Show the percentage of time that the CPU or CPUs were idle and the system did not have an outstanding disk I/O request.");

		// Set Percent columns
		md.setPercentCol("usrPct");
		md.setPercentCol("nicePct");
		md.setPercentCol("sysPct");
		md.setPercentCol("iowaitPct");
		md.setPercentCol("irqPct");
		md.setPercentCol("softPct");
		md.setPercentCol("stealPct");
		md.setPercentCol("guestPct");
		md.setPercentCol("idlePct");

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
