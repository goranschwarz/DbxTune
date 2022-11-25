/*******************************************************************************
 * Copyright (C) 2010-2020 Goran Schwarz
 * 
 * This file is part of DbxTune
 * DbxTune is a family of sub-products *Tune, hence the Dbx
 * Here are some of the tools: AseTune, IqTune, RsTune, RaxTune, HanaTune, 
 *          SqlServerTune, PostgresTune, MySqlTune, MariaDbTune, Db2Tune, ...
 * 
 * DbxTune is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, version 3 of the License.
 * 
 * DbxTune is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License
 * along with DbxTune.  If not, see <http://www.gnu.org/licenses/>.
 ******************************************************************************/
package com.asetune.hostmon;

import org.apache.log4j.Logger;

import com.asetune.cm.os.CmOsIostat;
import com.asetune.utils.Configuration;
import com.asetune.utils.VersionShort;

public class MonitorIoLinux 
extends MonitorIo
{
	private static Logger _logger = Logger.getLogger(MonitorIoLinux.class);

	public static String getUtilExtraInfoCommand()
	{
		// Get number of columns from iostat (version is sometimes "off")
		return "ioStatColCount=$(iostat -xdk | egrep '^[Dd]evice' | wc -w); echo \"ioStatColCount=${ioStatColCount}\"";
	}
	
	
	public MonitorIoLinux()
	{
		this(-1, null);
	}
	public MonitorIoLinux(int utilVersion, String utilExtraInfo)
	{
		super(utilVersion, utilExtraInfo);
	}

	@Override
	public String getModuleName()
	{
		return "MonitorIoLinux";
	}

	@Override
	public String getCommand()
	{
		// iotat switch: -N Display the registered device mapper names for any device mapper devices. Useful for viewing LVM2 statistics.
		boolean opt_N = Configuration.getCombinedConfiguration().getBooleanProperty(CmOsIostat.PROPKEY_linux_opt_N, CmOsIostat.DEFAULT_linux_opt_N);
		String extraOptions = "";
		
		if (opt_N)
			extraOptions = "-N "; // Note the extra space at the end 

		String cmd = super.getCommand();
		return cmd != null ? cmd : "iostat -xdk " + extraOptions + getSleepTime();
//		return cmd != null ? cmd : "iostat -xdzk "+getSleepTime(); // z doesnt seems to be standard
	}

	@Override
	public HostMonitorMetaData createMetaData(int utilVersion, Configuration utilExtraInfo)
	{
		HostMonitorMetaData md = new HostMonitorMetaData();
		md.setTableName(getModuleName());

		int extraInfo_ioStatColCount = utilExtraInfo.getIntProperty("ioStatColCount", -1);

		int ioStatColCount = -1;
		if (extraInfo_ioStatColCount > 0)
		{
			ioStatColCount = extraInfo_ioStatColCount;
		}
		else
		{
			_logger.warn("Number of fields for iostat could not be resoved. ioStatColCount=" + ioStatColCount + ". So we need to resolv to parsing the version number. utilVersionAsStr=" + VersionShort.toStr(utilVersion)+", utilVersionAsInt=" + utilVersion);

			if ( utilVersion >= VersionShort.toInt(12,2,0))
			{
				ioStatColCount = 21;
			}
			else if ( utilVersion >= VersionShort.toInt(11,5,7))
			{
				ioStatColCount = 16;
			}
			else if ( utilVersion >= VersionShort.toInt(9,1,2) || utilVersion == -1) // -1 is not defined or "offline" mode... so choose the type with most columns (in the future might save the utilVersion in the offline database)
			{
				ioStatColCount = 14;
			}
		}

		// Device:         rrqm/s   wrqm/s     r/s     w/s   rsec/s   wsec/s avgrq-sz avgqu-sz   await  svctm  %util
		// sda               0.02     1.49    0.16    0.95     3.27    19.52    20.57     0.04   36.29   2.10   0.23

		//-------------------------------------------------------------------------------------------------------
		// Version 10.2 example, has 14 columns
		//-------------------------------------------------------------------------------------------------------
		// gorans@gorans-ub:~$ iostat -V
		// sysstat version 10.2.0
		// (C) Sebastien Godard (sysstat <at> orange.fr)
		//
		// Device:         rrqm/s   wrqm/s     r/s     w/s    rkB/s    wkB/s avgrq-sz avgqu-sz   await r_await w_await  svctm  %util
		// sda               0.40     7.40    0.74    1.50    20.08    46.56    59.67     0.12   52.65   19.91   68.77   7.93   1.77
		//                                                                                             ^^^^^^^ ^^^^^^^
		//                                                                                             new columns
		// at github oldest version (9.5.1): https://github.com/sysstat/sysstat/commits/master/iostat.c
		// I can still see r_await and w_await, so let start use those columns from 9.5.1
		// 
		// and: http://sebastien.godard.pagesperso-orange.fr/
		// Sysstat 9.1.2 released (development version).
		// New fields have been added to iostat's extended statistics, giving the average time for read and write requests to be served (thanks go to J�r�me Marchand from Redhat for his help with that). 
		// Note that those fields should not be mistaken for read and write service times as they also include the time spent by the requests in queue... 
		// Also with this version, tickless CPUs will no longer be displayed as offline processors, but as 100% idle ones.


		//-------------------------------------------------------------------------------------------------------
		// Version 11.7 example, has 16 columns
		//-------------------------------------------------------------------------------------------------------
		// [gorans@primary1 ~]$ iostat -V
		// sysstat version 11.7.3
		// (C) Sebastien Godard (sysstat <at> orange.fr)
		//-------------------------------------------------------------------------------------------------------
		// [gorans@primary1 ~]$ iostat -xdk
		// Linux 4.18.0-408.el8.x86_64 (primary1)  11/08/2022      _x86_64_        (1 CPU)
        // 
		// Device            r/s     w/s     rkB/s     wkB/s   rrqm/s   wrqm/s  %rrqm  %wrqm r_await w_await aqu-sz rareq-sz wareq-sz  svctm  %util
		// sda              0.02    0.32      0.46      9.04     0.00     0.08   0.27  20.37    0.36    1.20   0.00    27.76    28.62   0.87   0.03
		// scd0             0.00    0.00      0.00      0.00     0.00     0.00   0.00   0.00    0.00    0.00   0.00     0.12     0.00   0.46   0.00
		// dm-0             0.02    0.39      0.44      9.02     0.00     0.00   0.00   0.00    0.35    0.92   0.00    28.23    22.87   0.70   0.03
		// dm-1             0.00    0.00      0.00      0.01     0.00     0.00   0.00   0.00    0.14    1.17   0.00    18.98     4.00   0.27   0.00
		//-------------------------------------------------------------------------------------------------------
		
		
		// -----------------------------------------------------------
		// Below is from: http://sebastien.godard.pagesperso-orange.fr/man_iostat.html
		// at: 2021-10-07, when latest version was: 12.5.4
		// -----------------------------------------------------------
		// Device Utilization Report
		// The second report generated by the iostat command is the Device Utilization Report. 
		// The device report provides statistics on a per physical device or partition basis. 
		// Block devices and partitions for which statistics are to be displayed may be entered on the command line. 
		// If no device nor partition is entered, then statistics are displayed for every device used by the system, and providing that the kernel maintains statistics for it. 
		// If the ALL keyword is given on the command line, then statistics are displayed for every device defined by the system, including those that have never been used. 
		// Transfer rates are shown in 1K blocks by default, unless the environment variable POSIXLY_CORRECT is set, in which case 512-byte blocks are used. 
		// The report may show the following fields, depending on the flags used (e.g. -x, -s and -k or -m):
        // 
		// Column Name                          Description
		// ---------------------------------    ----------------------------------------------------------------------------------------------------
		// Device                            -- This column gives the device (or partition) name as listed in the /dev directory. 
		// tps                               -- Indicate the number of transfers per second that were issued to the device. A transfer is an I/O request to the device. Multiple logical requests can be combined into a single I/O request to the device. A transfer is of indeterminate size. 
		// Blk_read/s (kB_read/s, MB_read/s) -- Indicate the amount of data read from the device expressed in a number of blocks (kilobytes, megabytes) per second. Blocks are equivalent to sectors and therefore have a size of 512 bytes. 
		// Blk_wrtn/s (kB_wrtn/s, MB_wrtn/s) -- Indicate the amount of data written to the device expressed in a number of blocks (kilobytes, megabytes) per second.
		// Blk_dscd/s (kB_dscd/s, MB_dscd/s) -- Indicate the amount of data discarded for the device expressed in a number of blocks (kilobytes, megabytes) per second.
		// Blk_w+d/s (kB_w+d/s, MB_w+d/s)    -- Indicate the amount of data written to or discarded for the device expressed in a number of blocks (kilobytes, megabytes) per second.
		// Blk_read (kB_read, MB_read)       -- The total number of blocks (kilobytes, megabytes) read. 
		// Blk_wrtn (kB_wrtn, MB_wrtn)       -- The total number of blocks (kilobytes, megabytes) written.
		// Blk_dscd (kB_dscd, MB_dscd)       -- The total number of blocks (kilobytes, megabytes) discarded.
		// Blk_w+d (kB_w+d, MB_w+d)          -- The total number of blocks (kilobytes, megabytes) written or discarded.
		// r/s                               -- The number (after merges) of read requests completed per second for the device. 
		// w/s                               -- The number (after merges) of write requests completed per second for the device.
		// d/s                               -- The number (after merges) of discard requests completed per second for the device.
		// f/s                               -- The number (after merges) of flush requests completed per second for the device. This counts flush requests executed by disks. Flush requests are not tracked for partitions. Before being merged, flush operations are counted as writes.
		// sec/s (kB/s, MB/s)                -- The number of sectors (kilobytes, megabytes) read from, written to or discarded for the device per second.
		// rsec/s (rkB/s, rMB/s)             -- The number of sectors (kilobytes, megabytes) read from the device per second. 
		// wsec/s (wkB/s, wMB/s)             -- The number of sectors (kilobytes, megabytes) written to the device per second.
		// dsec/s (dkB/s, dMB/s)             -- The number of sectors (kilobytes, megabytes) discarded for the device per second.
		// rqm/s                             -- The number of I/O requests merged per second that were queued to the device.
		// rrqm/s                            -- The number of read requests merged per second that were queued to the device. 
		// wrqm/s                            -- The number of write requests merged per second that were queued to the device.
		// drqm/s                            -- The number of discard requests merged per second that were queued to the device.
		// %rrqm                             -- The percentage of read requests merged together before being sent to the device.
		// %wrqm                             -- The percentage of write requests merged together before being sent to the device.
		// %drqm                             -- The percentage of discard requests merged together before being sent to the device.
		// areq-sz                           -- The average size (in kilobytes) of the requests that were issued to the device.
		//                                      Note: In previous versions, this field was known as avgrq-sz and was expressed in sectors.
		// rareq-sz                          -- The average size (in kilobytes) of the read requests that were issued to the device.
		// wareq-sz                          -- The average size (in kilobytes) of the write requests that were issued to the device.
		// dareq-sz                          -- The average size (in kilobytes) of the discard requests that were issued to the device.
		// await                             -- The average time (in milliseconds) for I/O requests issued to the device to be served. This includes the time spent by the requests in queue and the time spent servicing them.
		// r_await                           -- The average time (in milliseconds) for read requests issued to the device to be served. This includes the time spent by the requests in queue and the time spent servicing them.
		// w_await                           -- The average time (in milliseconds) for write requests issued to the device to be served. This includes the time spent by the requests in queue and the time spent servicing them.
		// d_await                           -- The average time (in milliseconds) for discard requests issued to the device to be served. This includes the time spent by the requests in queue and the time spent servicing them.
		// f_await                           -- The average time (in milliseconds) for flush requests issued to the device to be served. The block layer combines flush requests and executes at most one at a time. Thus flush operations could be twice as long: Wait for current flush request, then execute it, then wait for the next one.
		// aqu-sz                            -- The average queue length of the requests that were issued to the device.
		//                                      Note: In previous versions, this field was known as avgqu-sz.
		// %util                             -- Percentage of elapsed time during which I/O requests were issued to the device (bandwidth utilization for the device). Device saturation occurs when this value is close to 100% for devices serving requests serially. But for devices serving requests in parallel, such as RAID arrays and modern SSDs, this number does not reflect their performance limits.


		//------------------------------------------
		// Version 12.2 has 21 columns
		//------------------------------------------
		// [00:03:53][gorans@gorans-ub3:~$]$ iostat -V
		// sysstat version 12.2.0 
		// (C) Sebastien Godard (sysstat <at> orange.fr)
		// [00:03:54][gorans@gorans-ub3:~$]$ iostat -xdk 5
		// Linux 5.4.0-80-generic (gorans-ub3)     10/07/2021      _x86_64_        (4 CPU)
        // 
		// Device            r/s     rkB/s   rrqm/s  %rrqm r_await rareq-sz     w/s     wkB/s   wrqm/s  %wrqm w_await wareq-sz     d/s     dkB/s   drqm/s  %drqm d_await dareq-sz  aqu-sz  %util
		// dm-0             4.95     57.45     0.00   0.00    0.20    11.61   14.97     73.44     0.00   0.00    0.25     4.90    0.00      0.00     0.00   0.00    0.00     0.00    0.00   0.59
		// loop0            0.00      0.00     0.00   0.00    0.19    12.28    0.00      0.00     0.00   0.00    0.00     0.00    0.00      0.00     0.00   0.00    0.00     0.00    0.00   0.00
		// loop1            0.00      0.01     0.00   0.00    0.10     1.59    0.00      0.00     0.00   0.00    0.00     0.00    0.00      0.00     0.00   0.00    0.00     0.00    0.00   0.00
		// loop10           0.04      0.04     0.00   0.00    0.11     1.03    0.00      0.00     0.00   0.00    0.00     0.00    0.00      0.00     0.00   0.00    0.00     0.00    0.00   0.00
		// loop11           0.00      0.00     0.00   0.00    0.23     2.99    0.00      0.00     0.00   0.00    0.00     0.00    0.00      0.00     0.00   0.00    0.00     0.00    0.00   0.00
		// loop12           0.16      0.16     0.00   0.00    0.14     1.01    0.00      0.00     0.00   0.00    0.00     0.00    0.00      0.00     0.00   0.00    0.00     0.00    0.00   0.00
		// loop13           0.00      0.00     0.00   0.00    0.00     1.33    0.00      0.00     0.00   0.00    0.00     0.00    0.00      0.00     0.00   0.00    0.00     0.00    0.00   0.00
		// loop2            0.00      0.00     0.00   0.00    0.14     9.83    0.00      0.00     0.00   0.00    0.00     0.00    0.00      0.00     0.00   0.00    0.00     0.00    0.00   0.00
		// loop3            0.00      0.00     0.00   0.00    0.14     9.92    0.00      0.00     0.00   0.00    0.00     0.00    0.00      0.00     0.00   0.00    0.00     0.00    0.00   0.00
		// loop4            0.07      0.07     0.00   0.00    0.08     1.01    0.00      0.00     0.00   0.00    0.00     0.00    0.00      0.00     0.00   0.00    0.00     0.00    0.00   0.00
		// loop5            0.08      0.09     0.00   0.00    0.09     1.02    0.00      0.00     0.00   0.00    0.00     0.00    0.00      0.00     0.00   0.00    0.00     0.00    0.00   0.00
		// loop6            0.10      0.10     0.00   0.00    0.12     1.02    0.00      0.00     0.00   0.00    0.00     0.00    0.00      0.00     0.00   0.00    0.00     0.00    0.00   0.00
		// loop7            0.08      0.08     0.00   0.00    0.10     1.01    0.00      0.00     0.00   0.00    0.00     0.00    0.00      0.00     0.00   0.00    0.00     0.00    0.00   0.00
		// loop8            0.00      0.00     0.00   0.00    0.23    17.20    0.00      0.00     0.00   0.00    0.00     0.00    0.00      0.00     0.00   0.00    0.00     0.00    0.00   0.00
		// loop9            0.00      0.00     0.00   0.00    0.18    16.28    0.00      0.00     0.00   0.00    0.00     0.00    0.00      0.00     0.00   0.00    0.00     0.00    0.00   0.00
		// sda              4.77     57.62     0.18   3.70    0.19    12.07   10.27     73.63     4.71  31.43    0.30     7.17    0.00      0.00     0.00   0.00    0.00     0.00    0.00   0.59
		// sdb             21.10    689.80     0.00   0.02    0.17    32.69    7.94   1038.71     1.53  16.19    0.53   130.84    0.00      0.00     0.00   0.00    0.00     0.00    0.00   1.26
		// scd0             0.00      0.03     0.00   0.00    0.13    38.34    0.00      0.00     0.00   0.00    0.00     0.00    0.00      0.00     0.00   0.00    0.00     0.00    0.00   0.00
		// ------------- ------- ---------  ------- ------ ------- -------- ------- --------- -------- ------ ------- -------- ------- --------- -------- ------ ------- -------- ------- ------
		// 1 (field#)          2         3        4      5       6        7       8         9       10     11      12       13      14        15       16     17      18       19      20     21
		// ------------- ------- ---------  ------- ------ ------- -------- ------- --------- -------- ------ ------- -------- ------- --------- -------- ------ ------- -------- ------- ------
		
		
//System.out.println("MonitorIoLinux.createMetaData(utilVersion="+utilVersion+")");
		_logger.info("When creating meta data for Linux 'iostat', initializing it using utility version "+VersionShort.toStr(utilVersion)+" (intVer="+utilVersion+"), extraInfo_ioStatColCount="+extraInfo_ioStatColCount+", ioStatColCount="+ioStatColCount+".");
		_logger.debug("MonitorIoLinux.createMetaData(utilVersion="+utilVersion+")");

		boolean alwaysFalse = false;
		if (alwaysFalse)
		{
			// Dummy code, which we never enter... so we can comment out first section easy (probably just when adding new io stat layout)
		}
		else if ( ioStatColCount >= 21) // -1 is not defined or "offline" mode... so choose the type with most columns (in the future might save the utilVersion in the offline database)
		{
			_logger.info("Initializing MetaData with 21 (or above) column. utilVersion='"+VersionShort.toStr(utilVersion)+"', intVer="+utilVersion+", VersionShort.toInt(12,2,0)="+VersionShort.toInt(12,2,0));

			md.addStrColumn( "device",            1,  1, false,   30, "iostat[Device]: This column gives the device (or partition) name as listed in the /dev directory.");
			md.addIntColumn( "samples",           2,  0, true,        "Number of 'sub' sample entries of iostat this value is based on");

			md.addDecColumn ("totalIoPerSec",     3,  0, true, 10, 2, "Total number of IO's Formula: readsPerSec + writesPerSec");
			md.addDecColumn ("readPct",           4,  0, true, 10, 1, "Percent of Reads    from total IO's. Formula: readsPerSec / totalIo * 100");
			md.addDecColumn ("writePct",          5,  0, true, 10, 1, "Percent of Writes   from total IO's. Formula: writesPerSec / totalIo * 100");

			md.addStatColumn("readsPerSec",       6,  2, true, 10, 2, "iostat[r/s]: The number (after merges) of read requests completed per second for the device.");
			md.addStatColumn("writesPerSec",      7,  8, true, 10, 2, "iostat[w/s]: The number (after merges) of write requests completed per second for the device.");
			md.addStatColumn("discardPerSec",     8, 14, true, 10, 2, "iostat[d/s]: The number (after merges) of discard requests completed per second for the device.");

			md.addStatColumn("kbReadPerSec",      9,  3, true, 10, 2, "iostat[rkB/s]: The number of kilobytes read from the device per second.");
			md.addStatColumn("kbWritePerSec",    10,  9, true, 10, 2, "iostat[wkB/s]: The number of kilobytes written to the device per second.");
			md.addStatColumn("kbDiscardPerSec",  11, 15, true, 10, 2, "iostat[dkB/s]: The number of kilobytes discarded for the device per second.");

			md.addStatColumn("rrqmPerSec",       12,  4, true, 10, 2, "iostat[rrqm/s]: The number of read requests merged per second that were queued to the device. ");
			md.addStatColumn("wrqmPerSec",       13, 10, true, 10, 2, "iostat[wrqm/s]: The number of write requests merged per second that were queued to the device.");
			md.addStatColumn("drqmPerSec",       14, 16, true, 10, 2, "iostat[drqm/s]: The number of discard requests merged per second that were queued to the device.");

			md.addStatColumn("rrqmPct",          15,  5, true, 10, 2, "iostat[%rrqm]: The percentage of read requests merged together before being sent to the device.");
			md.addStatColumn("wrqmPct",          16, 11, true, 10, 2, "iostat[%wrqm]: The percentage of write requests merged together before being sent to the device.");
			md.addStatColumn("drqmPct",          17, 17, true, 10, 2, "iostat[%drqm]: The percentage of discard requests merged together before being sent to the device.");

			md.addDecColumn ("avgReadKbPerIo",   18,  0, true, 10, 1, "Average read size in KB per IO. Formula: kbReadPerSec/readsPerSec");
			md.addDecColumn ("avgWriteKbPerIo",  19,  0, true, 10, 1, "Average write size in KB per IO. Formula: kbWritePerSec/writePerSec");

			md.addStatColumn("readAvgSizeKb",    20,  7, true, 10, 2, "iostat[rareq-sz]: The average size (in kilobytes) of the read requests that were issued to the device.");
			md.addStatColumn("writeAvgSizeKb",   21, 13, true, 10, 2, "iostat[wareq-sz]: The average size (in kilobytes) of the write requests that were issued to the device.");
			md.addStatColumn("discardAvgSizeKb", 22, 19, true, 10, 2, "iostat[dareq-sz]: The average size (in kilobytes) of the discard requests that were issued to the device.");

			md.addStatColumn("r_await",          23,  6, true, 10, 2, "iostat[r_await]: The average time (in milliseconds) for read requests issued to the device to be served. This includes the time spent by the requests in queue and the time spent servicing them.");
			md.addStatColumn("w_await",          24, 12, true, 10, 2, "iostat[w_await]: The average time (in milliseconds) for write requests issued to the device to be served. This includes the time spent by the requests in queue and the time spent servicing them.");
			md.addStatColumn("d_await",          25, 18, true, 10, 2, "iostat[d_await]: The average time (in milliseconds) for discard requests issued to the device to be served. This includes the time spent by the requests in queue and the time spent servicing them.");

		//	md.addStatColumn("r_avgrq-sz",       ##, ##, true, 10, 1, "--->> removed in this version");
		//	md.addStatColumn("w_avgrq-sz",       ##, ##, true, 10, 1, "--->> removed in this version");
			md.addStatColumn("avgqu-sz",         26, 20, true, 10, 2, "iostat[aqu-sz]: The average queue length of the requests that were issued to the device. Note: In previous versions, this field was known as avgqu-sz.");

		//	md.addStatColumn("svctm",            ##, ##, true, 10, 2, "--->> removed in this version");
			md.addStatColumn("utilPct",          27, 21, true, 5,  2, "iostat[%util]: Percentage of elapsed time during which I/O requests were issued to the device (bandwidth utilization for the device). Device saturation occurs when this value is close to 100% for devices serving requests serially. But for devices serving requests in parallel, such as RAID arrays and modern SSDs, this number does not reflect their performance limits.");

			md.addStrColumn("deviceDescription", 28,  0, true, 255,   "Mapping of the column 'device' to your own description.");
		}
		else if ( ioStatColCount >= 16) // -1 is not defined or "offline" mode... so choose the type with most columns (in the future might save the utilVersion in the offline database)
		{
			_logger.info("Initializing MetaData with 16 (or above) column. utilVersion='"+VersionShort.toStr(utilVersion)+"', intVer="+utilVersion+", VersionShort.toInt(11,5,7)="+VersionShort.toInt(11,5,7));

			md.addStrColumn( "device",            1,  1, false,   30, "Disk device name (origin iostat colname='Device')");
			md.addIntColumn( "samples",           2,  0, true,        "Number of 'sub' sample entries of iostat this value is based on");

			md.addDecColumn ("totalIoPerSec",     3,  0, true, 10, 2, "Total number of IO's Formula: readsPerSec + writesPerSec");
			md.addDecColumn ("readPct",           4,  0, true, 10, 1, "Percent of Reads    from total IO's. Formula: readsPerSec / totalIo * 100");
			md.addDecColumn ("writePct",          5,  0, true, 10, 1, "Percent of Writes   from total IO's. Formula: writesPerSec / totalIo * 100");

			md.addStatColumn("readsPerSec",       6,  2, true, 10, 1, "The number (after merges) of read requests completed per second for the device. (origin iostat colname='r/s')");
			md.addStatColumn("writesPerSec",      7,  3, true, 10, 1, "The number (after merges) of write requests completed per second for the device. (origin iostat colname='w/s')");
			md.addStatColumn("kbReadPerSec",      8,  4, true, 10, 1, "The number of kilobytes read from the device per second. (origin iostat colname='rkB/s')");
			md.addStatColumn("kbWritePerSec",     9,  5, true, 10, 1, "The number of kilobytes written to the device per second. (origin iostat colname='wkB/s')");

			md.addStatColumn("rrqmPerSec",       10,  6, true, 10, 1, "The number of read requests merged per second that were queued to the device (origin iostat colname='rrqm/s')");
			md.addStatColumn("wrqmPerSec",       11,  7, true, 10, 1, "The number of write requests merged per second that were queued to the device. (origin iostat colname='wrqm/s')");
			md.addStatColumn("rrqmPct",          12,  8, true, 10, 1, "The percentage of read requests merged together before being sent to the device (origin iostat colname='%rrqm')");
			md.addStatColumn("wrqmPct",          13,  9, true, 10, 1, "The percentage of write requests merged together before being sent to the device. (origin iostat colname='%wrqm')");

			md.addDecColumn ("avgReadKbPerIo",   14,  0, true, 10, 1, "Average read size in KB per IO. Formula: kbReadPerSec/readsPerSec");
			md.addDecColumn ("avgWriteKbPerIo",  15,  0, true, 10, 1, "Average write size in KB per IO. Formula: kbWritePerSec/writePerSec");

			//md.addStatColumn("await",            13, 10, true, 10, 1, "The average time (in milliseconds) for I/O requests issued to the device to be served. This includes the time spent by the requests in queue and the time spent servicing them.");
			md.addStatColumn("r_await",          16, 10, true, 10, 2, "The average time (in milliseconds) for read requests issued to the device to be served. This includes the time spent by the requests in queue and the time spent servicing them. (origin iostat colname='r_await')");
			md.addStatColumn("w_await",          17, 11, true, 10, 2, "The average time (in milliseconds) for write requests issued to the device to be served. This includes the time spent by the requests in queue and the time spent servicing them. (origin iostat colname='w_await')");

			md.addStatColumn("r_avgrq-sz",       18, 12, true, 10, 1, "The average size (in kilobytes) of the read requests that were issued to the device. (origin iostat colname='rareq-sz')");
			md.addStatColumn("w_avgrq-sz",       19, 13, true, 10, 1, "The average size (in kilobytes) of the write requests that were issued to the device. (origin iostat colname='wareq-sz')");
			md.addStatColumn("avgqu-sz",         20, 14, true, 10, 1, "The average queue length of the requests that were issued to the device. (origin iostat colname='aqu-sz')");

			md.addStatColumn("svctm",            21, 15, true, 10, 2, "The average service time (in milliseconds) for I/O requests that were issued to the device. Warning! Do not trust this field any more.  This field will be removed in a future sysstat version. (origin iostat colname='svctm')");
			md.addStatColumn("utilPct",          22, 16, true, 5,  2, "Percentage of elapsed time during which I/O requests were issued to the device (bandwidth utilization for the device). Device saturation occurs when this value is close to 100% for devices serving requests serially. But for devices serving requests in parallel, such as RAID arrays and modern SSDs, this number does not reflect their performance limits. (origin iostat colname='%util')");

			md.addStrColumn("deviceDescription", 23,  0, true, 255,   "Mapping of the column 'device' to your own description.");
		}
		else if ( ioStatColCount >= 14 || utilVersion == -1) // -1 is not defined or "offline" mode... so choose the type with most columns (in the future might save the utilVersion in the offline database)
		{
			_logger.info("Initializing MetaData with 14 column. utilVersion='"+VersionShort.toStr(utilVersion)+"', intVer="+utilVersion+", VersionShort.toInt(9,1,2)="+VersionShort.toInt(9,1,2));
			
			md.addStrColumn( "device",            1,  1, false,   30, "Disk device name");
			md.addIntColumn( "samples",           2,  0, true,        "Number of 'sub' sample entries of iostat this value is based on");
                                                 
			md.addDecColumn ("totalIoPerSec",     3,  0, true, 10, 2, "Total number of IO's Formula: readsPerSec + writesPerSec");
			md.addDecColumn ("readPct",           4,  0, true, 10, 1, "Percent of Reads    from total IO's. Formula: readsPerSec / totalIo * 100");
			md.addDecColumn ("writePct",          5,  0, true, 10, 1, "Percent of Writes   from total IO's. Formula: writesPerSec / totalIo * 100");

			md.addStatColumn("rrqmPerSec",        6,  2, true, 10, 1, "The number of read requests merged per second that were queued to the device");
			md.addStatColumn("wrqmPerSec",        7,  3, true, 10, 1, "The number of write requests merged per second that were queued to the device.");
			md.addStatColumn("readsPerSec",       8,  4, true, 10, 1, "The number of read requests that were issued to the device per second.");
			md.addStatColumn("writesPerSec",      9,  5, true, 10, 1, "The number of write requests that were issued to the device per second.");
			md.addStatColumn("kbReadPerSec",     10,  6, true, 10, 1, "The number of kilobytes read from the device per second.");
			md.addStatColumn("kbWritePerSec",    11,  7, true, 10, 1, "The number of kilobytes writ to the device per second.");
			md.addDecColumn ("avgReadKbPerIo",   12,  0, true, 10, 1, "Avergare read size in KB per IO. Formula: kbReadPerSec/readsPerSec");
			md.addDecColumn ("avgWriteKbPerIo",  13,  0, true, 10, 1, "Avergare write size in KB per IO. Formula: kbWritePerSec/writePerSec");

			md.addStatColumn("avgrq-sz",         14,  8, true, 10, 1, "The average size (in  sectors) of the requests that were issued to the device.");
			md.addStatColumn("avgqu-sz",         15,  9, true, 10, 1, "The average queue length of the requests that were issued to the device.");
                                                
			md.addStatColumn("await",            16, 10, true, 10, 2, "The average time (in milliseconds) for I/O requests issued to the device to be served. This includes the time spent by the requests in queue and the time spent servicing them.");
			md.addStatColumn("r_await",          17, 11, true, 10, 2, "The average time (in milliseconds) for read requests issued to the device to be served. This includes the time spent by the requests in queue and the time spent servicing them.");
			md.addStatColumn("w_await",          18, 12, true, 10, 2, "The average time (in milliseconds) for write requests issued to the device to be served. This includes the time spent by the requests in queue and the time spent servicing them.");
			md.addStatColumn("svctm",            19, 13, true, 10, 2, "The average service time (in milliseconds) for I/O requests that were issued to the device.");
			md.addStatColumn("utilPct",          20, 14, true, 5,  2, "Percentage of CPU time during which I/O requests were issued to the device (bandwidth utilization for the device). Device saturation occurs when this value is close to 100%.");

			md.addStrColumn("deviceDescription", 21,  0, true, 255,   "Mapping of the column 'device' to your own description.");
		}
		else
		{
			_logger.info("Initializing MetaData with 12 column. utilVersion='"+VersionShort.toStr(utilVersion)+"', intVer="+utilVersion);

			md.addStrColumn( "device",            1,  1, false,   30, "Disk device name");
			md.addIntColumn( "samples",           2,  0, true,        "Number of 'sub' sample entries of iostat this value is based on");
                                                  
			md.addDecColumn ("totalIoPerSec",     3,  0, true, 10, 2, "Total number of IO's Formula: readsPerSec + writesPerSec");
			md.addDecColumn ("readPct",           4,  0, true, 10, 1, "Percent of Reads    from total IO's. Formula: readsPerSec / totalIo * 100");
			md.addDecColumn ("writePct",          5,  0, true, 10, 1, "Percent of Writes   from total IO's. Formula: writesPerSec / totalIo * 100");

			md.addStatColumn("rrqmPerSec",        6,  2, true, 10, 1, "The number of read requests merged per second that were queued to the device");
			md.addStatColumn("wrqmPerSec",        7,  3, true, 10, 1, "The number of write requests merged per second that were queued to the device.");
			md.addStatColumn("readsPerSec",       8,  4, true, 10, 1, "The number of read requests that were issued to the device per second.");
			md.addStatColumn("writesPerSec",      9,  5, true, 10, 1, "The number of write requests that were issued to the device per second.");
			md.addStatColumn("kbReadPerSec",     10,  6, true, 10, 1, "The number of kilobytes read from the device per second.");
			md.addStatColumn("kbWritePerSec",    11,  7, true, 10, 1, "The number of kilobytes writ to the device per second.");
			md.addDecColumn ("avgReadKbPerIo",   12,  0, true, 10, 1, "Avergare read size in KB per IO. Formula: kbReadPerSec/readsPerSec");
			md.addDecColumn ("avgWriteKbPerIo",  13,  0, true, 10, 1, "Avergare write size in KB per IO. Formula: kbWritePerSec/writePerSec");

			md.addStatColumn("avgrq-sz",         14,  8, true, 10, 1, "The average size (in  sectors) of the requests that were issued to the device.");
			md.addStatColumn("avgqu-sz",         15,  9, true, 10, 1, "The average queue length of the requests that were issued to the device.");
                                                 
			md.addStatColumn("await",            16, 10, true, 10, 2, "The average time (in milliseconds) for I/O requests issued to the device to be served. This includes the time spent by the requests in queue and the time spent servicing them.");
			md.addStatColumn("svctm",            17, 11, true, 10, 2, "The average service time (in milliseconds) for I/O requests that were issued to the device.");
			md.addStatColumn("utilPct",          18, 12, true, 5,  2, "Percentage of CPU time during which I/O requests were issued to the device (bandwidth utilization for the device). Device saturation occurs when this value is close to 100%.");

			md.addStrColumn("deviceDescription", 19, 0, true, 255, "Mapping of the column 'device' to your own description.");
		}

		// Use "device" as the Primary Key, which is used to du summary/average calculations
		md.setPkCol("device");
		
		// Set column "samples", to a special status, which will contain number of 
		// underlying samples the summary/average caclulation was based on
		md.setStatusCol("samples",    HostMonitorMetaData.STATUS_COL_SUB_SAMPLE);

		// Set Percent columns
		if (md.hasColumn("readPct" )) md.setPercentCol("readPct");
		if (md.hasColumn("writePct")) md.setPercentCol("writePct");
		if (md.hasColumn("rrqmPct" )) md.setPercentCol("rrqmPct");
		if (md.hasColumn("wrqmPct" )) md.setPercentCol("wrqmPct");
		if (md.hasColumn("drqmPct" )) md.setPercentCol("drqmPct");
		if (md.hasColumn("utilPct" )) md.setPercentCol("utilPct");

		// What regexp to use to split the input row into individual fields
		md.setParseRegexp(HostMonitorMetaData.REGEXP_IS_SPACE);

		// Skip the header line
//		md.setSkipRows("device", "Device:");
		md.setSkipRows("device", "^[Dd]evice.*"); // later versions (11,5,7 ???) of iostat do NOT have the ':' in the string...

		// Skip "loop" devices
		if (Configuration.getCombinedConfiguration().getBooleanProperty("hostmon.MonitorIo.skipLoopDevices", true))
			md.setSkipRows("device", "^loop[0-9]+");

		// Get SKIP and ALLOW from the Configuration
		md.setSkipAndAllowRows(null, Configuration.getCombinedConfiguration());
		md.setSkipAndAllowRows("hostmon.MonitorIo.", Configuration.getCombinedConfiguration());

		// set positions of some columns, this will be used in parseRow()
		_pos_readsPerSec  = md.getParseColumnArrayPos("readsPerSec");
		_pos_writesPerSec = md.getParseColumnArrayPos("writesPerSec");

		return md;
	}

	private int _pos_readsPerSec;  // Skip rows with 0.0
	private int _pos_writesPerSec; // Skip rows with 0.0

	@Override
	public String[] parseRow(HostMonitorMetaData md, String row, String[] preParsed, int type)
	{
		if ( preParsed.length == md.getParseColumnCount() )
		{
			// Skip header 
			if (skipRow(md, row, preParsed, type))
				return null;

			// now check the ALLOWED rows
			// If NO allow entries are installed, then true will be returned.
			if ( ! allowRow(md, row, preParsed, type))
				return null;

			// If first time sample, get ALL devices (simulate -z switch)
			if (isFirstTimeSample())
				return preParsed;

			// Skip readsPerSec && writesPerSec, which is ZERO
			// emulate the -z switch, which isn't around on all Linux platforms
			if ( "0.00".equals(preParsed[_pos_readsPerSec]) && "0.00".equals(preParsed[_pos_writesPerSec]) )
				return null;

			// Allow the row
			return preParsed;
		}
		return null;
	}
}



/* -------------------------------------------------------------------------------------------------------
 * Below is data for iostat version [11.5.7 ???] or higher
 * -------------------------------------------------------------------------------------------------------

[root@dbtest01 ase]# iostat -V
sysstat version 10.1.5
(C) Sebastien Godard (sysstat <at> orange.fr)
[root@dbtest01 ase]#

[root@dbtest01 ase]# iostat -xdctk 5
Linux 3.10.0-229.14.1.el7.x86_64 (dbtest01)     04/17/2020      _x86_64_        (4 CPU)

04/17/2020 12:34:44 AM
avg-cpu:  %user   %nice %system %iowait  %steal   %idle
           3.78    0.00    0.20    1.66    0.00   94.36

Device:         rrqm/s   wrqm/s     r/s     w/s    rkB/s    wkB/s avgrq-sz avgqu-sz   await r_await w_await  svctm  %util
sda               0.90     0.23   32.43   35.65  1423.30  4285.44   167.71     0.01    0.15    1.04    1.67   0.66   4.50
dm-0              0.00     0.00    0.21    0.29    15.46     2.03    70.30     0.01   17.71   21.87   14.64   5.96   0.30
dm-1              0.00     0.00    0.00    0.00     0.00     0.00     8.00     0.00   75.10   22.22   77.18   6.05   0.00
dm-2              0.00     0.00    0.00    0.00     2.47     0.00   978.61     0.00   40.86   41.62   19.05  10.68   0.01
dm-3              0.00     0.00   11.77    2.02  1066.05  1014.81   301.88     0.05    3.48    6.56   26.49   1.75   2.41
dm-4              0.00     0.00   21.34   33.58   339.04  3268.60   131.38     0.06    1.12    2.18    0.45   0.66   3.61



##
## Note: columns are re-arrenged, re-named, and some new
##
[root@DBTEST65 ~]# iostat -V
sysstat version 11.7.3
(C) Sebastien Godard (sysstat <at> orange.fr)

			http://sebastien.godard.pagesperso-orange.fr/
			Sysstat 11.5.7 released (development version).
			Sysstat 11.4.5 released (stable version).
			Sysstat 11.2.11 released (stable version).
			Improvements added here mainly concern the iostat command: New metrics added to the extended statistics report, a new switch (-s) to display a short (narrow) version of the report that will fit in 80 characters wide screens, the JSON output that now depends on options used... Also to be noted: Several metrics (from iostat and sar) are now expressed in kilobytes instead of sectors.
			Last but not least, this development version includes a set of bugfixes that you will also find in the 11.4.x and 11.2.x series.

[root@DBTEST65 ~]# iostat -xdctk 5
Linux 4.18.0-147.el8.x86_64 (DBTEST65)  04/17/2020      _x86_64_        (2 CPU)

04/17/2020 12:36:13 AM
avg-cpu:  %user   %nice %system %iowait  %steal   %idle
          13.52    0.01    3.02    7.30    0.00   76.14

Device            r/s     w/s     rkB/s     wkB/s   rrqm/s   wrqm/s  %rrqm  %wrqm r_await w_await aqu-sz rareq-sz wareq-sz  svctm  %util
sda              1.39    1.54    139.72    360.83     0.02     0.33   1.19  17.68    0.81   17.21   0.03   100.39   234.75   0.65   0.19
sdb             31.93  579.89   9777.80  57560.77     0.74    14.30   2.28   2.41    2.48    0.85   0.40   306.27    99.26   0.36  21.75
sdc              0.01    0.00      0.19      0.00     0.00     0.00   0.00   0.00    0.26    0.00   0.00    36.53     0.00   0.31   0.00
sdd              0.01    0.00      0.19      0.00     0.00     0.00   0.00   0.00    0.26    0.00   0.00    36.53     0.00   0.28   0.00
scd0             0.04    0.00      0.12      0.00     0.00     0.00   0.00   0.00    0.47    0.00   0.00     3.34     0.00   0.36   0.00
dm-0             1.26    1.30    138.82    358.12     0.00     0.00   0.00   0.00    0.84   23.74   0.03   110.22   276.19   0.60   0.15
dm-1             0.11    0.57      0.51      2.65     0.00     0.00   0.00   0.00    0.19    0.57   0.00     4.49     4.65   0.59   0.04

04/17/2020 12:36:18 AM
avg-cpu:  %user   %nice %system %iowait  %steal   %idle
           0.20    0.00    0.30    0.00    0.00   99.50

Device            r/s     w/s     rkB/s     wkB/s   rrqm/s   wrqm/s  %rrqm  %wrqm r_await w_await aqu-sz rareq-sz wareq-sz  svctm  %util
sda              0.00    0.00      0.00      0.00     0.00     0.00   0.00   0.00    0.00    0.00   0.00     0.00     0.00   0.00   0.00
sdb              0.00    0.60      0.00      3.20     0.00     0.20   0.00  25.00    0.00    0.33   0.00     0.00     5.33   1.00   0.06
sdc              0.00    0.00      0.00      0.00     0.00     0.00   0.00   0.00    0.00    0.00   0.00     0.00     0.00   0.00   0.00
sdd              0.00    0.00      0.00      0.00     0.00     0.00   0.00   0.00    0.00    0.00   0.00     0.00     0.00   0.00   0.00
scd0             0.00    0.00      0.00      0.00     0.00     0.00   0.00   0.00    0.00    0.00   0.00     0.00     0.00   0.00   0.00
dm-0             0.00    0.00      0.00      0.00     0.00     0.00   0.00   0.00    0.00    0.00   0.00     0.00     0.00   0.00   0.00
dm-1             0.00    0.00      0.00      0.00     0.00     0.00   0.00   0.00    0.00    0.00   0.00     0.00     0.00   0.00   0.00



--------------------------------------------------
---- Below is a possible mapping...
---- Note: Think about WHAT COLUMN names are used externally (for examples in reports)
----       possibly add a method to get the *real* name of the column to use... 
--------------------------------------------------
			md.addStrColumn( "device",            1,  1, false,   30, "Disk device name");
			md.addIntColumn( "samples",           2,  0, true,        "Number of 'sub' sample entries of iostat this value is based on");
                                                 
			md.addStatColumn("readsPerSec",       3,  2, true, 10, 1, "The number (after merges) of read requests completed per second for the device.");
			md.addStatColumn("writesPerSec",      4,  3, true, 10, 1, "The number (after merges) of write requests completed per second for the device.");
			md.addStatColumn("kbReadPerSec",      5,  4, true, 10, 1, "The number of kilobytes read from the device per second.");
			md.addStatColumn("kbWritePerSec",     6,  5, true, 10, 1, "The number of kilobytes written to the device per second.");

			md.addStatColumn("rrqmPerSec",        7,  6, true, 10, 1, "The number of read requests merged per second that were queued to the device");
			md.addStatColumn("wrqmPerSec",        8,  7, true, 10, 1, "The number of write requests merged per second that were queued to the device.");
			md.addStatColumn("rrqmPct",           9,  8, true, 10, 1, "The percentage of read requests merged together before being sent to the device");
			md.addStatColumn("wrqmPct",          10,  9, true, 10, 1, "The percentage of write requests merged together before being sent to the device.");

			md.addDecColumn ("avgReadKbPerIo",   11,  0, true, 10, 1, "Average read size in KB per IO. Formula: kbReadPerSec/readsPerSec");
			md.addDecColumn ("avgWriteKbPerIo",  12,  0, true, 10, 1, "Average write size in KB per IO. Formula: kbWritePerSec/writePerSec");

			//md.addStatColumn("await",            13, 10, true, 10, 1, "The average time (in milliseconds) for I/O requests issued to the device to be served. This includes the time spent by the requests in queue and the time spent servicing them.");
			md.addStatColumn("r_await",          13, 10, true, 10, 1, "The average time (in milliseconds) for read requests issued to the device to be served. This includes the time spent by the requests in queue and the time spent servicing them.");
			md.addStatColumn("w_await",          14, 11, true, 10, 1, "The average time (in milliseconds) for write requests issued to the device to be served. This includes the time spent by the requests in queue and the time spent servicing them.");

			md.addStatColumn("r_avgrq-sz",       15, 12, true, 10, 1, "The average size (in kilobytes) of the read requests that were issued to the device.");
			md.addStatColumn("w_avgrq-sz",       16, 13, true, 10, 1, "The average size (in kilobytes) of the write requests that were issued to the device.");
			md.addStatColumn("avgqu-sz",         17, 14, true, 10, 1, "The average queue length of the requests that were issued to the device.");
                                                
			md.addStatColumn("svctm",            18, 15, true, 10, 1, "The average service time (in milliseconds) for I/O requests that were issued to the device. Warning! Do not trust this field any more.  This field will be removed in a future sysstat version.");
			md.addStatColumn("utilPct",          19, 16, true, 5,  1, "Percentage of elapsed time during which I/O requests were issued to the device (bandwidth utilization for the device). Device saturation occurs when this value is close to 100% for devices serving requests serially. But for devices serving requests in parallel, such as RAID arrays and modern SSDs, this number does not reflect their performance limits.");

			md.addStrColumn("deviceDescription", 20,  0, true, 255,   "Mapping of the column 'device' to your own description.");

---------------------
			md.addStrColumn( "device",            1,  1, false,   30, "Disk device name");
			md.addIntColumn( "samples",           2,  0, true,        "Number of 'sub' sample entries of iostat this value is based on");
                                                 
			md.addStatColumn("rrqmPerSec",        3,  2, true, 10, 1, "The number of read requests merged per second that were queued to the device");
			md.addStatColumn("wrqmPerSec",        4,  3, true, 10, 1, "The number of write requests merged per second that were queued to the device.");
			md.addStatColumn("readsPerSec",       5,  4, true, 10, 1, "The number of read requests that were issued to the device per second.");
			md.addStatColumn("writesPerSec",      6,  5, true, 10, 1, "The number of write requests that were issued to the device per second.");
			md.addStatColumn("kbReadPerSec",      7,  6, true, 10, 1, "The number of kilobytes read from the device per second.");
			md.addStatColumn("kbWritePerSec",     8,  7, true, 10, 1, "The number of kilobytes writ to the device per second.");
			md.addDecColumn ("avgReadKbPerIo",    9,  0, true, 10, 1, "Avergare read size in KB per IO. Formula: kbReadPerSec/readsPerSec");
			md.addDecColumn ("avgWriteKbPerIo",  10,  0, true, 10, 1, "Avergare write size in KB per IO. Formula: kbWritePerSec/writePerSec");

			md.addStatColumn("avgrq-sz",         11,  8, true, 10, 1, "The average size (in  sectors) of the requests that were issued to the device.");
			md.addStatColumn("avgqu-sz",         12,  9, true, 10, 1, "The average queue length of the requests that were issued to the device.");
                                                
			md.addStatColumn("await",            13, 10, true, 10, 1, "The average time (in milliseconds) for I/O requests issued to the device to be served. This includes the time spent by the requests in queue and the time spent servicing them.");
			md.addStatColumn("r_await",          14, 11, true, 10, 1, "The average time (in milliseconds) for read requests issued to the device to be served. This includes the time spent by the requests in queue and the time spent servicing them.");
			md.addStatColumn("w_await",          15, 12, true, 10, 1, "The average time (in milliseconds) for write requests issued to the device to be served. This includes the time spent by the requests in queue and the time spent servicing them.");
			md.addStatColumn("svctm",            16, 13, true, 10, 1, "The average service time (in milliseconds) for I/O requests that were issued to the device.");
			md.addStatColumn("utilPct",          17, 14, true, 5,  1, "Percentage of CPU time during which I/O requests were issued to the device (bandwidth utilization for the device). Device saturation occurs when this value is close to 100%.");

			md.addStrColumn("deviceDescription", 18,  0, true, 255,   "Mapping of the column 'device' to your own description.");



IOSTAT(1)                                                                            Linux User's Manual                                                                            IOSTAT(1)

NAME
       iostat - Report Central Processing Unit (CPU) statistics and input/output statistics for devices and partitions.

SYNOPSIS
       iostat  [ -c ] [ -d ] [ -h ] [ -k | -m ] [ -N ] [ -s ] [ -t ] [ -V ] [ -x ] [ -y ] [ -z ] [ -j { ID | LABEL | PATH | UUID | ... } ] [ -o JSON ] [ [ -H ] -g group_name ] [ --human ] [
       -p [ device [,...] | ALL ] ] [ device [...] | ALL ] [ interval [ count ] ]

DESCRIPTION
       The iostat command is used for monitoring system input/output device loading by observing the time the devices are active in relation to their average transfer rates. The iostat com-
       mand generates reports that can be used to change system configuration to better balance the input/output load between physical disks.

       The first report generated by the iostat command provides statistics concerning the time since the system was booted, unless the -y option is used (in this case, this first report is
       omitted).  Each subsequent report covers the time since the previous report. All statistics are reported each time the iostat command is run. The report consists of a CPU header  row
       followed  by a row of CPU statistics. On multiprocessor systems, CPU statistics are calculated system-wide as averages among all processors. A device header row is displayed followed
       by a line of statistics for each device that is configured.

       The interval parameter specifies the amount of time in seconds between each report. The count parameter can be specified in conjunction with the  interval  parameter.  If  the  count
       parameter  is  specified, the value of count determines the number of reports generated at interval seconds apart. If the interval parameter is specified without the count parameter,
       the iostat command generates reports continuously.

REPORTS
       The iostat command generates two types of reports, the CPU Utilization report and the Device Utilization report.

       CPU Utilization Report
              The first report generated by the iostat command is the CPU Utilization Report. For multiprocessor systems, the CPU values are  global  averages  among  all  processors.   The
              report has the following format:

              %user
                     Show the percentage of CPU utilization that occurred while executing at the user level (application).

              %nice
                     Show the percentage of CPU utilization that occurred while executing at the user level with nice priority.

              %system
                     Show the percentage of CPU utilization that occurred while executing at the system level (kernel).

              %iowait
                     Show the percentage of time that the CPU or CPUs were idle during which the system had an outstanding disk I/O request.

              %steal
                     Show the percentage of time spent in involuntary wait by the virtual CPU or CPUs while the hypervisor was servicing another virtual processor.

              %idle
                     Show the percentage of time that the CPU or CPUs were idle and the system did not have an outstanding disk I/O request.

       Device Utilization Report
              The  second  report  generated by the iostat command is the Device Utilization Report. The device report provides statistics on a per physical device or partition basis. Block
              devices and partitions for which statistics are to be displayed may be entered on the command line.  If no device nor partition is entered, then statistics are  displayed  for
              every device used by the system, and providing that the kernel maintains statistics for it.  If the ALL keyword is given on the command line, then statistics are displayed for
              every device defined by the system, including those that have never been used.  Transfer rates are shown in 1K blocks by default, unless the environment variable  POSIXLY_COR-
              RECT is set, in which case 512-byte blocks are used.  The report may show the following fields, depending on the flags used:

              Device:
                     This column gives the device (or partition) name as listed in the /dev directory.

              tps
                     Indicate the number of transfers per second that were issued to the device. A transfer is an I/O request to the device. Multiple logical requests can be combined into a
                     single I/O request to the device. A transfer is of indeterminate size.

              Blk_read/s (kB_read/s, MB_read/s)
                     Indicate the amount of data read from the device expressed in a number of blocks (kilobytes, megabytes) per second. Blocks are equivalent to sectors and therefore  have
                     a size of 512 bytes.

              Blk_wrtn/s (kB_wrtn/s, MB_wrtn/s)
                     Indicate the amount of data written to the device expressed in a number of blocks (kilobytes, megabytes) per second.

              Blk_read (kB_read, MB_read)
                     The total number of blocks (kilobytes, megabytes) read.

              Blk_wrtn (kB_wrtn, MB_wrtn)
                     The total number of blocks (kilobytes, megabytes) written.

              r/s
                     The number (after merges) of read requests completed per second for the device.

              w/s
                     The number (after merges) of write requests completed per second for the device.

              sec/s (kB/s, MB/s)
                     The number of sectors (kilobytes, megabytes) read from or written to the device per second.

              rsec/s (rkB/s, rMB/s)
                     The number of sectors (kilobytes, megabytes) read from the device per second.

              wsec/s (wkB/s, wMB/s)
                     The number of sectors (kilobytes, megabytes) written to the device per second.

              rqm/s
                     The number of I/O requests merged per second that were queued to the device.

              rrqm/s
                     The number of read requests merged per second that were queued to the device.

              wrqm/s
                     The number of write requests merged per second that were queued to the device.

              %rrqm
                     The percentage of read requests merged together before being sent to the device.

              %wrqm
                     The percentage of write requests merged together before being sent to the device.

              areq-sz
                     The average size (in kilobytes) of the I/O requests that were issued to the device.
                     Note: In previous versions, this field was known as avgrq-sz and was expressed in sectors.

              rareq-sz
                     The average size (in kilobytes) of the read requests that were issued to the device.

              wareq-sz
                     The average size (in kilobytes) of the write requests that were issued to the device.

              await
                     The  average time (in milliseconds) for I/O requests issued to the device to be served. This includes the time spent by the requests in queue and the time spent servicing them.

              r_await
                     The average time (in milliseconds) for read requests issued to the device to be served. This includes the time spent by the requests in queue and the time spent servicing them.

              w_await
                     The  average  time (in milliseconds) for write requests issued to the device to be served. This includes the time spent by the requests in queue and the time spent servicing them.

              aqu-sz
                     The average queue length of the requests that were issued to the device.
                     Note: In previous versions, this field was known as avgqu-sz.

              svctm
                     The average service time (in milliseconds) for I/O requests that were issued to the device. Warning! Do not trust this field any more.  This field will be removed in  a
                     future sysstat version.

              %util
                     Percentage of elapsed time during which I/O requests were issued to the device (bandwidth utilization for the device). Device saturation occurs when this value is close
                     to 100% for devices serving requests serially.  But for devices serving requests in parallel, such as RAID arrays and modern SSDs, this number does  not  reflect  their
                     performance limits.


OPTIONS
       -c     Display the CPU utilization report.

       -d     Display the device utilization report.

       -g group_name { device [...] | ALL }
              Display  statistics  for  a group of devices.  The iostat command reports statistics for each individual device in the list then a line of global statistics for the group dis-
              played as group_name and made up of all the devices in the list. The ALL keyword means that all the block devices defined by the system shall be included in the group.

       -H     This option must be used with option -g and indicates that only global statistics for the group are to be displayed, and not statistics for individual devices in the group.

       -h     Make the Device Utilization Report easier to read by a human.  --human is enabled implicitly with this option.

       --human
              Print sizes in human readable format (e.g. 1.0k, 1.2M, etc.)  The units displayed with this option supersede any other default units (e.g.  kilobytes,  sectors...)  associated
              with the metrics.

       -j { ID | LABEL | PATH | UUID | ... } [ device [...] | ALL ]
              Display  persistent  device  names.  Options  ID,  LABEL, etc. specify the type of the persistent name. These options are not limited, only prerequisite is that directory with
              required persistent names is present in /dev/disk.  Optionally, multiple devices can be specified in the chosen persistent name type.  Because persistent device names are usu-
              ally long, option

       -k     Display statistics in kilobytes per second.

       -m     Display statistics in megabytes per second.

       -N     Display the registered device mapper names for any device mapper devices.  Useful for viewing LVM2 statistics.

       -o JSON
              Display the statistics in JSON (Javascript Object Notation) format.  JSON output field order is undefined, and new fields may be added in the future.

       -p [ { device [,...] | ALL } ]
              The -p option displays statistics for block devices and all their partitions that are used by the system.  If a device name is entered on the command line, then statistics for
              it and all its partitions are displayed. Last, the ALL keyword indicates that statistics have to be displayed for all the block devices and partitions defined by  the  system,
              including  those  that  have  never been used. If option -j is defined before this option, devices entered on the command line can be specified with the chosen persistent name
              type.

       -s     Display a short (narrow) version of the report that should fit in 80 characters wide screens.

       -t     Print the time for each report displayed. The timestamp format may depend on the value of the S_TIME_FORMAT environment variable (see below).

       -V     Print version number then exit.

       -x     Display extended statistics.

       -y     Omit first report with statistics since system boot, if displaying multiple records at given interval.

       -z     Tell iostat to omit output for any devices for which there was no activity during the sample period.
ENVIRONMENT
       The iostat command takes into account the following environment variables:

       POSIXLY_CORRECT
              When this variable is set, transfer rates are shown in 512-byte blocks instead of the default 1K blocks.

       S_COLORS
              When this variable is set, display statistics in color on the terminal.  Possible values for this variable are never, always or auto (the latter is the default).

              Please note that the color (being red, yellow, or some other color) used to display a value is not indicative of any kind of issue simply because of the color. It  only  indi-
              cates different ranges of values.

       S_COLORS_SGR
              Specify  the  colors  and  other  attributes  used  to  display  statistics  on  the  terminal.   Its  value  is  a  colon-separated  list  of  capabilities  that  defaults to
              H=31;1:I=32;22:M=35;1:N=34;1:Z=34;22.  Supported capabilities are:

              H=     SGR (Select Graphic Rendition) substring for percentage values greater than or equal to 75%.

              I=     SGR substring for device names.

              M=     SGR substring for percentage values in the range from 50% to 75%.

              N=     SGR substring for non-zero statistics values.

              Z=     SGR substring for zero values.

       S_TIME_FORMAT
              If this variable exists and its value is ISO then the current locale will be ignored when printing the date in the report header. The iostat command will use the ISO 8601 for-
              mat (YYYY-MM-DD) instead.  The timestamp displayed with option -t will also be compliant with ISO 8601 format.

EXAMPLES
       iostat
              Display a single history since boot report for all CPU and Devices.

       iostat -d 2
              Display a continuous device report at two second intervals.

       iostat -d 2 6
              Display six reports at two second intervals for all devices.

       iostat -x sda sdb 2 6
              Display six reports of extended statistics at two second intervals for devices sda and sdb.

       iostat -p sda 2 6
              Display six reports at two second intervals for device sda and all its partitions (sda1, etc.)

BUGS
       /proc filesystem must be mounted for iostat to work.

       Kernels older than 2.6.x are no longer supported.

       The  average service time (svctm field) value is meaningless, as I/O statistics are now calculated at block level, and we don't know when the disk driver starts to process a request.
       For this reason, this field will be removed in a future sysstat version.

FILES
       /proc/stat contains system statistics.

       /proc/uptime contains system uptime.

       /proc/diskstats contains disks statistics.

       /sys contains statistics for block devices.

       /proc/self/mountstats contains statistics for network filesystems.

       /dev/disk contains persistent device names.

AUTHOR
       Sebastien Godard (sysstat <at> orange.fr)

SEE ALSO
       sar(1), pidstat(1), mpstat(1), vmstat(8), tapestat(1), nfsiostat(1), cifsiostat(1)

       http://pagesperso-orange.fr/sebastien.godard/

Linux                                                                                    JANUARY 2018                                                                               IOSTAT(1)




2020-04-17 15:11:43,046 - INFO  - AWT-EventQueue-0               - Log4jViewer                    - Setting new Level for 'com.asetune.hostmon.HostMonitor' from 'INFO', to 'DEBUG'.
2020-04-17 15:11:44,733 - DEBUG - MonitorIoLinux                 - HostMonitor                    - -DISCARD-: This row has '16' entries. MetaDataExpected parseCount='14'. Row 'Device            r/s     w/s     rkB/s     wkB/s   rrqm/s   wrqm/s  %rrqm  %wrqm r_await w_await aqu-sz rareq-sz wareq-sz  svctm  %util'. The Input/PreParsed String Array, size(16)=[Device, r/s, w/s, rkB/s, wkB/s, rrqm/s, wrqm/s, %rrqm, %wrqm, r_await, w_await, aqu-sz, rareq-sz, wareq-sz, svctm, %util].
2020-04-17 15:11:44,733 - DEBUG - MonitorIoLinux                 - HostMonitor                    - -DISCARD-: This row has '16' entries. MetaDataExpected parseCount='14'. Row 'sda              0.00    0.00      0.00      0.00     0.00     0.00   0.00   0.00    0.00    0.00   0.00     0.00     0.00   0.00   0.00'. The Input/PreParsed String Array, size(16)=[sda, 0.00, 0.00, 0.00, 0.00, 0.00, 0.00, 0.00, 0.00, 0.00, 0.00, 0.00, 0.00, 0.00, 0.00, 0.00].
2020-04-17 15:11:44,749 - DEBUG - MonitorIoLinux                 - HostMonitor                    - -DISCARD-: This row has '16' entries. MetaDataExpected parseCount='14'. Row 'sdb           1909.00    0.00  55334.00      0.00     0.00     0.00   0.00   0.00    0.64    0.00   0.61    28.99     0.00   0.31  59.85'. The Input/PreParsed String Array, size(16)=[sdb, 1909.00, 0.00, 55334.00, 0.00, 0.00, 0.00, 0.00, 0.00, 0.64, 0.00, 0.61, 28.99, 0.00, 0.31, 59.85].
2020-04-17 15:11:44,749 - DEBUG - MonitorIoLinux                 - HostMonitor                    - -DISCARD-: This row has '16' entries. MetaDataExpected parseCount='14'. Row 'sdc              0.00    0.00      0.00      0.00     0.00     0.00   0.00   0.00    0.00    0.00   0.00     0.00     0.00   0.00   0.00'. The Input/PreParsed String Array, size(16)=[sdc, 0.00, 0.00, 0.00, 0.00, 0.00, 0.00, 0.00, 0.00, 0.00, 0.00, 0.00, 0.00, 0.00, 0.00, 0.00].
2020-04-17 15:11:44,749 - DEBUG - MonitorIoLinux                 - HostMonitor                    - -DISCARD-: This row has '16' entries. MetaDataExpected parseCount='14'. Row 'sdd              0.00    0.00      0.00      0.00     0.00     0.00   0.00   0.00    0.00    0.00   0.00     0.00     0.00   0.00   0.00'. The Input/PreParsed String Array, size(16)=[sdd, 0.00, 0.00, 0.00, 0.00, 0.00, 0.00, 0.00, 0.00, 0.00, 0.00, 0.00, 0.00, 0.00, 0.00, 0.00].
2020-04-17 15:11:44,749 - DEBUG - MonitorIoLinux                 - HostMonitor                    - -DISCARD-: This row has '16' entries. MetaDataExpected parseCount='14'. Row 'scd0             0.00    0.00      0.00      0.00     0.00     0.00   0.00   0.00    0.00    0.00   0.00     0.00     0.00   0.00   0.00'. The Input/PreParsed String Array, size(16)=[scd0, 0.00, 0.00, 0.00, 0.00, 0.00, 0.00, 0.00, 0.00, 0.00, 0.00, 0.00, 0.00, 0.00, 0.00, 0.00].
2020-04-17 15:11:44,764 - DEBUG - MonitorIoLinux                 - HostMonitor                    - -DISCARD-: This row has '16' entries. MetaDataExpected parseCount='14'. Row 'dm-0             0.00    0.00      0.00      0.00     0.00     0.00   0.00   0.00    0.00    0.00   0.00     0.00     0.00   0.00   0.00'. The Input/PreParsed String Array, size(16)=[dm-0, 0.00, 0.00, 0.00, 0.00, 0.00, 0.00, 0.00, 0.00, 0.00, 0.00, 0.00, 0.00, 0.00, 0.00, 0.00].
2020-04-17 15:11:44,764 - DEBUG - MonitorIoLinux                 - HostMonitor                    - -DISCARD-: This row has '16' entries. MetaDataExpected parseCount='14'. Row 'dm-1             0.00    0.00      0.00      0.00     0.00     0.00   0.00   0.00    0.00    0.00   0.00     0.00     0.00   0.00   0.00'. The Input/PreParsed String Array, size(16)=[dm-1, 0.00, 0.00, 0.00, 0.00, 0.00, 0.00, 0.00, 0.00, 0.00, 0.00, 0.00, 0.00, 0.00, 0.00, 0.00].
2020-04-17 15:11:44,764 - DEBUG - MonitorIoLinux                 - HostMonitor                    - -DISCARD-: This row has '1' entries. MetaDataExpected parseCount='14'. Row ''. The Input/PreParsed String Array, size(1)=[].
2020-04-17 15:11:46,718 - DEBUG - MonitorIoLinux                 - HostMonitor                    - -DISCARD-: This row has '16' entries. MetaDataExpected parseCount='14'. Row 'Device            r/s     w/s     rkB/s     wkB/s   rrqm/s   wrqm/s  %rrqm  %wrqm r_await w_await aqu-sz rareq-sz wareq-sz  svctm  %util'. The Input/PreParsed String Array, size(16)=[Device, r/s, w/s, rkB/s, wkB/s, rrqm/s, wrqm/s, %rrqm, %wrqm, r_await, w_await, aqu-sz, rareq-sz, wareq-sz, svctm, %util].
2020-04-17 15:11:46,718 - DEBUG - MonitorIoLinux                 - HostMonitor                    - -DISCARD-: This row has '16' entries. MetaDataExpected parseCount='14'. Row 'sda              0.00    0.00      0.00      0.00     0.00     0.00   0.00   0.00    0.00    0.00   0.00     0.00     0.00   0.00   0.00'. The Input/PreParsed String Array, size(16)=[sda, 0.00, 0.00, 0.00, 0.00, 0.00, 0.00, 0.00, 0.00, 0.00, 0.00, 0.00, 0.00, 0.00, 0.00, 0.00].
2020-04-17 15:11:46,749 - DEBUG - MonitorIoLinux                 - HostMonitor                    - -DISCARD-: This row has '16' entries. MetaDataExpected parseCount='14'. Row 'sdb           1585.00    0.00  43118.00      0.00     0.00     0.00   0.00   0.00    0.72    0.00   0.61    27.20     0.00   0.33  52.25'. The Input/PreParsed String Array, size(16)=[sdb, 1585.00, 0.00, 43118.00, 0.00, 0.00, 0.00, 0.00, 0.00, 0.72, 0.00, 0.61, 27.20, 0.00, 0.33, 52.25].
2020-04-17 15:11:46,749 - DEBUG - MonitorIoLinux                 - HostMonitor                    - -DISCARD-: This row has '16' entries. MetaDataExpected parseCount='14'. Row 'sdc              0.00    0.00      0.00      0.00     0.00     0.00   0.00   0.00    0.00    0.00   0.00     0.00     0.00   0.00   0.00'. The Input/PreParsed String Array, size(16)=[sdc, 0.00, 0.00, 0.00, 0.00, 0.00, 0.00, 0.00, 0.00, 0.00, 0.00, 0.00, 0.00, 0.00, 0.00, 0.00].
2020-04-17 15:11:46,749 - DEBUG - MonitorIoLinux                 - HostMonitor                    - -DISCARD-: This row has '16' entries. MetaDataExpected parseCount='14'. Row 'sdd              0.00    0.00      0.00      0.00     0.00     0.00   0.00   0.00    0.00    0.00   0.00     0.00     0.00   0.00   0.00'. The Input/PreParsed String Array, size(16)=[sdd, 0.00, 0.00, 0.00, 0.00, 0.00, 0.00, 0.00, 0.00, 0.00, 0.00, 0.00, 0.00, 0.00, 0.00, 0.00].
2020-04-17 15:11:46,749 - DEBUG - MonitorIoLinux                 - HostMonitor                    - -DISCARD-: This row has '16' entries. MetaDataExpected parseCount='14'. Row 'scd0             0.00    0.00      0.00      0.00     0.00     0.00   0.00   0.00    0.00    0.00   0.00     0.00     0.00   0.00   0.00'. The Input/PreParsed String Array, size(16)=[scd0, 0.00, 0.00, 0.00, 0.00, 0.00, 0.00, 0.00, 0.00, 0.00, 0.00, 0.00, 0.00, 0.00, 0.00, 0.00].
2020-04-17 15:11:46,749 - DEBUG - MonitorIoLinux                 - HostMonitor                    - -DISCARD-: This row has '16' entries. MetaDataExpected parseCount='14'. Row 'dm-0             0.00    0.00      0.00      0.00     0.00     0.00   0.00   0.00    0.00    0.00   0.00     0.00     0.00   0.00   0.00'. The Input/PreParsed String Array, size(16)=[dm-0, 0.00, 0.00, 0.00, 0.00, 0.00, 0.00, 0.00, 0.00, 0.00, 0.00, 0.00, 0.00, 0.00, 0.00, 0.00].
2020-04-17 15:11:46,749 - DEBUG - MonitorIoLinux                 - HostMonitor                    - -DISCARD-: This row has '16' entries. MetaDataExpected parseCount='14'. Row 'dm-1             0.00    0.00      0.00      0.00     0.00     0.00   0.00   0.00    0.00    0.00   0.00     0.00     0.00   0.00   0.00'. The Input/PreParsed String Array, size(16)=[dm-1, 0.00, 0.00, 0.00, 0.00, 0.00, 0.00, 0.00, 0.00, 0.00, 0.00, 0.00, 0.00, 0.00, 0.00, 0.00].
2020-04-17 15:11:46,764 - DEBUG - MonitorIoLinux                 - HostMonitor                    - -DISCARD-: This row has '1' entries. MetaDataExpected parseCount='14'. Row ''. The Input/PreParsed String Array, size(1)=[].

*/