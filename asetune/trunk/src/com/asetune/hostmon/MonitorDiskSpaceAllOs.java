package com.asetune.hostmon;

import com.asetune.ssh.SshConnection;

public class MonitorDiskSpaceAllOs
extends HostMonitor
{
//	private static Logger _logger = Logger.getLogger(MonitorDiskSpaceAllOs.class);

	public MonitorDiskSpaceAllOs()
	{
		this(-1);
	}
	public MonitorDiskSpaceAllOs(int utilVersion)
	{
		super(utilVersion);
	}

	@Override
	public String getModuleName()
	{
		return "MonitorDiskSpaceAllOs";
	}

	@Override
	public String getCommand()
	{
		String cmd = super.getCommand();
		return cmd != null ? cmd : "df -k | sed '1d'";
	}

//	gorans@gorans-ub2:/etc/postgresql/9.5/main$ df -k
//	Filesystem     1K-blocks     Used Available Use% Mounted on
//	udev             8137404        0   8137404   0% /dev
//	tmpfs            1631564   157804   1473760  10% /run
//	/dev/sda2       98334332 91646324   1669796  99% /
//	tmpfs            8157816      180   8157636   1% /dev/shm
//	tmpfs               5120        4      5116   1% /run/lock
//	tmpfs            8157816        0   8157816   0% /sys/fs/cgroup
//	/dev/sda1         523248     3480    519768   1% /boot/efi
//	cgmfs                100        0       100   0% /run/cgmanager/fs
//	tmpfs            1631564       72   1631492   1% /run/user/1000
	
	@Override
	public HostMonitorMetaData createMetaData(int utilVersion)
	{
		HostMonitorMetaData md = new HostMonitorMetaData();
		md.setTableName(getModuleName());

		md.addStrColumn ("Filesystem",       1, 1, false, 100,   "Type of filesystem");
		md.addLongColumn("Size-KB",          2, 2, false,        "Total size of usable space (KB) in the file system");
		md.addLongColumn("Used-KB",          3, 3, false,        "Amount of space used in KB");
		md.addLongColumn("Available-KB",     4, 4, false,        "Amount of space available for use, in KB");
		md.addIntColumn ("Size-MB",          5, 0, false,        "Total size of usable space (MB) in the file system");
		md.addIntColumn ("Used-MB",          6, 0, false,        "Amount of space used in MB");
		md.addIntColumn ("Available-MB",     7, 0, false,        "Amount of space available for use, in MB");
		md.addDecColumn ("UsedPct",          8, 5, true,  5, 2,  "<html>Amount of space used, as a percentage of the total capacity<br><br><b>Formula:</b> 100.0 - (availableKB / sizeKB * 100.0)</html>");
		md.addStrColumn ("MountedOn",        9, 6, true,  400,   "Mount point");
                                                 
		// What regexp to use to split the input row into individual fields
		md.setParseRegexp(HostMonitorMetaData.REGEXP_IS_SPACE);

		md.setOsCommandStreaming(false);
		
		md.setPercentCol("UsedPct");

		md.setPkCol("MountedOn");
		
		// Skip the header line
//		md.setSkipRows("Filesystem", "Filesystem");

//		// Get SKIP and ALLOW from the Configuration
//		md.setSkipAndAllowRows(null, Configuration.getCombinedConfiguration());
//		md.setSkipAndAllowRows("hostmon.MonitorVmstat.", Configuration.getCombinedConfiguration());

		return md;
	}

	@Override
	public String[] parseRow(HostMonitorMetaData md, String row, String[] preParsed, int type)
	{
		if (type == SshConnection.STDERR_DATA)
			return null;

		// Let "super" do it's intended work
		String[] data = super.parseRow(md, row, preParsed, type);
		if (data == null)
			return null;

		// Strip off *trailing* '%' characters in 'UsedPct' fields
		int usedPct_pos = 4;
		if (usedPct_pos < data.length)
		{
			if (data[usedPct_pos].endsWith("%"))
				data[usedPct_pos] = data[usedPct_pos].substring(0, data[usedPct_pos].length()-1);
		}

		return data;
	}
	
	
}
