package com.asetune.hostmon;

import com.asetune.utils.Configuration;

public class MonitorIoVeritas
extends MonitorIo
{
	@Override
	public String getModuleName()
	{
		return "MonitorIoVeritas";
	}

	@Override
	public String getCommand()
	{
		/* 
		 * from: http://sfdoccentral.symantec.com/sf/5.0MP3/linux/manpages/vxvm/man1m/vxstat.html
		 * 
		 * -i interval [-S]
    	 * Prints the change in volume statistics that occurs after every interval seconds. 
    	 * The first interval is assumed to encompass the entire previous history of objects. 
    	 * Subsequent displays will show statistics with a zero value if there has been no 
    	 * change since the previous interval.
    	 * 
    	 * Specify the -S option to display statistics for each interval grouped per second. 
    	 * Otherwise, the command displays the accumulated statistics for the entire time interval. 
    	 * With the -S option, the command generates scaled statistics outputs. 
    	 * The unit of the scaled output depends on how large the statistics value being printed is. 
		 */
		String cmd = super.getCommand();
		return cmd != null ? cmd : "vxstat -o alldgs -i "+getSleepTime()+" -S";    // at disk group level... should I add -S at the end to get 
//		return cmd != null ? cmd : "vxstat -o alldgs -d -i "+getSleepTime()+" -S"; // for the individual disks
	}

	@Override
	public HostMonitorMetaData createMetaData()
	{
		HostMonitorMetaData md = new HostMonitorMetaData();
		md.setTableName(getModuleName());

		//-------------------------------
		// The below is NOT TESTED
		//-------------------------------
        md.addStrColumn( "type",             1,  1, false,   30, "Type of Veritas device (vol/disk)");
        md.addStrColumn( "name",             2,  2, false,   30, "Name of the disk");
		md.addIntColumn( "samples",          3,  0, true,        "Number of 'sub' sample entries of vxstat this value is based on");

		md.addStatColumn("OperationsRead",   4,  3, true, 10, 1, "The number of read operations, per seconds");
		md.addStatColumn("OperationsWrite",  5,  4, true, 10, 1, "The number of write operations, per seconds");
		md.addStatColumn("BlocksRead",       6,  5, true, 10, 1, "The number of blocks read, per seconds");
		md.addStatColumn("BlocksWrite",      7,  6, true, 10, 1, "The number of blocks written, per second");
		md.addStatColumn("AvgTimeMsRead",    8,  7, true, 10, 1, "The average time in milliseconds spent on read operations in the interval");
		md.addStatColumn("AvgTimeMsWrite",   9,  8, true, 10, 1, "The average time in milliseconds spent on write operations in the interval");

		// Use "name" as the Primary Key, which is used to do summary/average calculations
		md.setPkCol("name");

		// Set column "samples", to a special status, which will contain number of 
		// underlying samples the summary/average calculation was based on
		md.setStatusCol("samples",    HostMonitorMetaData.STATUS_COL_SUB_SAMPLE);

		// What regexp to use to split the input row into individual fields
		md.setParseRegexp(HostMonitorMetaData.REGEXP_IS_SPACE);

		// Skip the header line
		md.setSkipRows("name", "NAME");

		// Get SKIP and ALLOW from the Configuration
		md.setSkipAndAllowRows(null, Configuration.getCombinedConfiguration());
		md.setSkipAndAllowRows("hostmon.MonitorIo.", Configuration.getCombinedConfiguration());

		return md;
	}
}

/*

qqfktdb@stfkt01:/opt/VRTS/bin$ ./vxstat -?
VxVM vxstat INFO V-5-1-0 Usage: ./vxstat [-g diskgroup | -o alldgs] [-i interval [-S] [-c count ]] [-f {abvcfoprsFMWROSCV}] [-CpsvduPT] [-r] [ obj ... ] [-n node [,node ... ]]
Formats: s - read/write, a - atomic copies, v - verify reads
         c - corrected reads, f - failed I/Os, b - read-writeback
         o - reads for snapshots, p - push writes, r - resync writes
          e - extended copies,
         F - full-stripe write, M - read-modify-write
         W - reconstruct writes, R - reconstruct reads
         O - zero ioctls, S - parity resync ioctls
         C - subdisk reconstruct ioctls
         V - raid5 verify ioctls
           T - print i/o statistics in microseconds
         u - print scaled output
         S - print per-second reports

---- from manpage on the internet ....
To display statistics for all subdisks associated with all volumes, use the command: 
	vxstat -s 

To display statistics for the plexes and subdisks of a volume named blop, use the following: 
	vxstat -ps blop 

To reset all statistics for a disk group named foodg, type the following command: 
	vxstat -g foodg -r 

To display 5 sets of disk statistics at 10 second intervals, use the following: 
	vxstat -i 10 -c 5 -d 

RESET VX STAT
/opt/VRTS/bin/vxstat -g tfkitmigrdg -r
/opt/VRTS/bin/vxstat -g tfkitcompdg -r



COLLECT in 10 seconds intervall:
/opt/VRTS/bin/vxstat -o alldgs -i 10     # at disk group level
/opt/VRTS/bin/vxstat -o alldgs -i 10 -d  # for the individual disks



########################
# TEST RUN
########################

qqfktdb@stfkt01:/opt/VRTS/bin$ /opt/VRTS/bin/vxstat -o alldgs -i 10
                      OPERATIONS          BLOCKS           AVG TIME(ms)
TYP NAME              READ     WRITE      READ     WRITE   READ  WRITE

Wed Sep 29 15:29:34 2010
DG tfkitmigrdg
vol migrfs01      57405968 249832776 2481241207 798764918   0.00   0.02
vol migrfs02        179351     46205  57072138   2740987   1.47   1.39

DG tfkitcompdg
vol compfs01       9043971 152663063 2237217089 1203958973   0.07   0.00
vol compfs02        120071     40340  11817422     81005   1.68   1.11


Wed Sep 29 15:29:44 2010
DG tfkitmigrdg
vol migrfs01             0         6         0        81   0.00   0.62
vol migrfs02             0         0         0         0   0.00   0.00

DG tfkitcompdg
vol compfs01             0         0         0         0   0.00   0.00
vol compfs02             0         0         0         0   0.00   0.00




------------------------------------------------------------------------------------
------------------------------------------------------------------------------------
http://uw714doc.sco.com/en/ODM_VMadmin/sag-5.html

Using I/O Statistics
Examination of the I/O statistics may suggest reconfiguration. There are two primary statistics to look at: volume I/O activity and disk I/O activity.

Before obtaining statistics, consider clearing (resetting) all existing statistics. Use the command vxstat -r to clear all statistics. Clearing statistics eliminates any differences between volumes or disks that might appear due to volumes being created, and also removes statistics from booting (which are not normally of interest).

After clearing the statistics, let the system run for a while and then display the accumulated statistics. Try to let it run during typical system activity. In order to measure the effect of a particular application or workload, it should be run specifically. When monitoring a system that is used for multiple purposes, try not to exercise any one application more than it would be exercised normally. When monitoring a time-sharing system with many users, try to let statistics accumulate during normal use for several hours during the day.

To display volume statistics, use the command vxstat with no arguments. This might display a list such as:

                OPERATIONS         BLOCKS         AVG TIME(ms) 
TYP  NAME      READ  WRITE      READ    WRITE     READ   WRITE 
vol  archive    865    807      5722     3809     32.5    24.0 
vol  home      2980   5287      6504    10550     37.7   221.1 
vol  local    49477  49230    507892   204975     28.5    33.5 
vol  rootvol 102906 342664   1085520  1962946     28.1    25.6 
vol  src      79174  23603    425472   139302     22.4    30.9 
vol  swapvol  22751  32364    182001   258905     25.3   323.2

This output helps to identify volumes with an unusually large number of operations or excessive read or write times.

To display disk statistics, use the command vxstat -d. This might display a list such as:

                 OPERATIONS        BLOCKS        AVG TIME(ms) 
TYP  NAME      READ   WRITE      READ   WRITE    READ   WRITE 
dm  disk01    40473  174045    455898  951379    29.5    35.4 
dm  disk02    32668   16873    470337  351351    35.2   102.9 
dm  disk03    55249   60043    780779  731979    35.3    61.2 
dm  disk04    11909   13745    114508  128605    25.0   30.7 

*/