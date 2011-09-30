package com.asetune.hostmon;

import com.asetune.utils.Configuration;

public class MonitorIoSolaris
extends MonitorIo
{
	@Override
	public String getModuleName()
	{
		return "MonitorIoSolaris";
	}

	@Override
	public String getCommand()
	{
		String cmd = super.getCommand();
		return cmd != null ? cmd : "iostat -xdz "+getSleepTime();
	}

	@Override
	public HostMonitorMetaData createMetaData()
	{
		HostMonitorMetaData md = new HostMonitorMetaData();
		md.setTableName(getModuleName());

		md.addStrColumn( "device",         1,  1, false,   30, "Disk device name");
		md.addIntColumn( "samples",        2,  0, true,        "Number of 'sub' sample entries of iostat this value is based on");

		md.addStatColumn("readsPerSec",    3,  2, true, 5,  1, "Reads per second");
		md.addStatColumn("writesPerSec",   4,  3, true, 5,  1, "Writes per second");
		md.addStatColumn("kbReadPerSec",   5,  4, true, 10, 1, "KB read per second");
		md.addStatColumn("kbWritePerSec",  6,  5, true, 10, 1, "KB written per second");
		md.addStatColumn("wait",           7,  6, true, 5,  1, "Average number of transactions waiting for service (Q length)");
		md.addStatColumn("actv",           8,  7, true, 5,  1, "Average number of transactions actively being serviced (removed from the queue but not yet completed)");
		md.addStatColumn("svc_t",          9,  8, true, 5,  1, "Service time (ms). Includes everything: wait time, active queue time, seek rotation, transfer time");
		md.addStatColumn("waitPct",       10,  9, true, 4,  1, "Percent of time there are transactions waiting for service (queue non-empty)");
		md.addStatColumn("busyPct",       11, 10, true, 4,  1, "Percent of time the disk is busy (transactions in progress)");

		// Use "device" as the Primary Key, which is used to du summary/average calculations
		md.setPkCol("device");

		// Set column "samples", to a special status, which will contain number of 
		// underlying samples the summary/average caclulation was based on
		md.setStatusCol("samples",    HostMonitorMetaData.STATUS_COL_SUB_SAMPLE);

		// Set Percent columns
		md.setPercentCol("waitPct");
		md.setPercentCol("busyPct");

		// What regexp to use to split the input row into individual fields
		md.setParseRegexp(HostMonitorMetaData.REGEXP_IS_SPACE);

		// Skip the header line
		md.setSkipRows("device", "device");

		// Get SKIP and ALLOW from the Configuration
		md.setSkipAndAllowRows(null, Configuration.getCombinedConfiguration());
		md.setSkipAndAllowRows("hostmon.MonitorIo.", Configuration.getCombinedConfiguration());

		return md;
	}
}
