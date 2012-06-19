package com.asetune.hostmon;


public class MonitorUpTimeAllOs
extends MonitorUpTime
{
	/*
	------------------------------------------
	SunOS sunspot 5.10 Generic_120011-14 sun4u sparc SUNW,Sun-Fire-V890
	sunspot% uptime
	------------------------------------------
  	8:46am  up 19 day(s), 21:36,  27 users,  load average: 0.49, 0.54, 0.56

	------------------------------------------
	AIX bluesky3 3 5 00C77C6F4C00
	% uptime
	------------------------------------------
  	08:38AM   up 41 days,  15:38,  13 users,  load average: 1.83, 1.66, 1.60

	------------------------------------------
	HP-UX potamus B.11.23 U 9000/800 1488144216 unlimited-user license
	[1] % uptime
	------------------------------------------
  	8:35am  up 404 days, 16:56,  7 users,  load average: 0.40, 0.37, 0.36

	------------------------------------------
	Linux sweiq-linux 2.6.18-274.3.1.el5 #1 SMP Tue Sep 6 20:13:52 EDT 2011 x86_64 x86_64 x86_64 GNU/Linux
	[ajackson@sweiq-linux ~]$ uptime
	------------------------------------------
 	17:35:12 up 22 days,  6:23,  1 user,  load average: 0.00, 0.01, 0.00

	------------------------------------------
	Linux gorans-ub 2.6.35-30-generic #54-Ubuntu SMP Tue Jun 7 18:40:23 UTC 2011 i686 GNU/Linux
	gorans@gorans-ub:~$ uptime
	------------------------------------------
 	19:48:46 up 218 days, 7 min,  7 users,  load average: 23.16, 23.05, 23.06

	Solaris: 8:46am    up 19  day(s), 21:36,  27 users,  load average: 0.49, 0.54, 0.56
	AIX:     08:38AM   up 41  days,   15:38,  13 users,  load average: 1.83, 1.66, 1.60
	HP:      8:35am    up 404 days,   16:56,  7  users,  load average: 0.40, 0.37, 0.36
	LINUX 1: 17:35:12  up 22  days,    6:23,  1  user,   load average: 0.00, 0.01, 0.00
 	LINUX 2: 19:48:46  up 218 days,   7 min,  7 users,   load average: 23.16, 23.05, 23.06   <<<<note does not work due to '7 min', not one word
	*/
	
	@Override
	public String getModuleName()
	{
		return "MonitorUpTimeAllOs";
	}

	@Override
	public String getCommand()
	{
		String cmd = super.getCommand();
		return cmd != null ? cmd : "uptime";
	}

	@Override
	public HostMonitorMetaData createMetaData()
	{
		HostMonitorMetaData md = new HostMonitorMetaData();
		md.setTableName(getModuleName());

		md.addStrColumn("sampleTime",         1, 1, true, 10,    "Approximately when this record was samples");

		md.addIntColumn("daysUp",             2, 3, true,        "OS has been up for this amount of days + the hours column");
		md.addStrColumn("hoursUp",            3, 5, true, 10,    "OS has been up for (the days olumn) + this time description");
		md.addIntColumn("users",              4, 6, true,        "Number of users logged on to the OS right now");

		md.addStatColumn("loadAverage_1Min",  5, 10, true, 8, 2, "Load Average for last minute");
		md.addStatColumn("loadAverage_5Min",  6, 11, true, 8, 2, "Load Average for last 5 minutes");
		md.addStatColumn("loadAverage_15Min", 7, 12, true, 8, 2, "Load Average for last 15 minutes");

//		// Set column "sampleTime", to a special status, which will contain the  
//		// underlying sample TIME the record was sampled
//		md.setStatusCol("sampleTime", HostMonitorMetaData.STATUS_COL_SAMPLE_TIME);

		// What regexp to use to split the input row into individual fields
		md.setParseRegexp(HostMonitorMetaData.REGEXP_IS_SPACE);

		md.setOsCommandStreaming(false);
		
//		// Skip the header line
//		md.setSkipRows("memory_swap", "swap");

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

		String[] data = super.parseRow(md, row, preParsed, type);
		if (data == null)
			return null;

		// Strip off ',' characters
		for (int i=0; i<data.length; i++)
			data[i] = data[i].replace(",", "");

		return data;
	}

}
