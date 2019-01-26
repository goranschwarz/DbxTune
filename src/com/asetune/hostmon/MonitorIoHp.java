/*******************************************************************************
 * Copyright (C) 2010-2019 Goran Schwarz
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

import com.asetune.utils.Configuration;

public class MonitorIoHp 
extends MonitorIo
{
	public MonitorIoHp()
	{
		this(-1, null);
	}
	public MonitorIoHp(int utilVersion, String utilExtraInfo)
	{
		super(utilVersion, utilExtraInfo);
	}

	@Override
	public String getModuleName()
	{
		return "MonitorIoHp";
	}

	@Override
	public String getCommand()
	{
		String cmd = super.getCommand();
		return cmd != null ? cmd : "iostat "+getSleepTime();
	}

	@Override
	public HostMonitorMetaData createMetaData(int utilVersion, Configuration utilExtraInfo)
	{
		HostMonitorMetaData md = new HostMonitorMetaData();
		md.setTableName(getModuleName());

		md.addStrColumn( "device",  1,  1, false,   30, "Device name");
		md.addIntColumn( "samples", 2,  0, true,        "Number of 'sub' sample entries of iostat this value is based on");

		md.addStatColumn("bps",     3,  2, true, 10, 1, "Kilobytes transferred per second");
		md.addStatColumn("sps",     4,  3, true, 10, 1, "Number of seeks per second");
		md.addStatColumn("msps",    5,  4, true, 10, 1, "Milliseconds per average seek");

		md.addStrColumn ("deviceDescription", 6, 0, true, 255, "Mapping of the column 'device' to your own description.");

		// Use "device" as the Primary Key, which is used to du summary/average calculations
		md.setPkCol("device");

		// Set column "samples", to a special status, which will contain number of 
		// underlying samples the summary/average caclulation was based on
		md.setStatusCol("samples",    HostMonitorMetaData.STATUS_COL_SUB_SAMPLE);

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
//
// Maybe use sar on HP instaed: sar -d | cut -c 9-255
// the cut to get rid of the timestamp, which is NOT printed on *every* disk row
//
//
//Hewlett-Packard Company            - 2 -   HP-UX 11i Version 2: August 2003
//
//sar(1M)                                                             sar(1M)
//
//
//          -d    Report activity for each block device, e.g., disk or tape
//                drive.  One line is printed for each device that had
//                activity during the last interval.  If no devices were
//                active, a blank line is printed.  Each line contains the
//                following data:
//
//                    device         Logical name of the device and its
//                                   corresponding instance.  Devices are
//                                   categorized into the following device
//                                   types:
//
//                                        disk3 - SCSI and NIO FL disks
//                                        sdisk - SCSI disks;
//
//                    %busy          Portion of time device was busy
//                                   servicing a request;
//
//                    avque          Average number of requests outstanding
//                                   for the device;
//
//                    r+w/s          Number of data transfers per second
//                                   (read and writes) from and to the
//                                   device;
//
//                    blks/s         Number of bytes transferred (in 512-byte
//                                   units) from and to the device;
//
//                    avwait         Average time (in milliseconds) that
//                                   transfer requests waited idly on queue
//                                   for the device;
//
//                    avserv         Average time (in milliseconds) to
//                                   service each transfer request (includes
//                                   seek, rotational latency, and data
//                                   transfer times) for the device.
//
//
//
//$ sar -d 2 99999999999
//
//HP-UX hpitan20 B.11.23 U ia64    04/01/11
//
//02:39:32   device   %busy   avque   r+w/s  blks/s  avwait  avserv
//02:39:34   c0t2d0    0.50    0.50       1      16    0.00   13.79
//02:39:36   c0t2d0    3.00    0.50       6      50    0.00    5.25
//02:39:38   c0t2d0   14.00    0.50       3      48    0.00   51.43
//02:39:40   c0t2d0    1.49    0.50       3      56    0.00    7.50
//02:39:42
//02:39:44   c0t2d0    1.00    0.50       2      17    0.00    6.65
//02:39:46   c0t2d0    0.50    0.50       1      24    0.00    9.79
//02:39:48   c0t2d0    0.50    0.50       1      16    0.00    3.54
//02:39:50   c0t2d0   28.00    0.50       4      64    0.00  395.25
//          c0t0d0    0.50    0.50       0       8    0.00    7.89
//02:39:52   c0t2d0    1.00    0.50       4      56    0.00   10.68
//02:39:54   c0t2d0    0.50    0.50       1      16    0.00    4.62
//02:39:56   c0t2d0    0.50    0.50       1       1    0.01    6.10
//02:39:58   c0t2d0    2.50    0.50       4      65    0.00    5.15
//          c0t0d0    0.50    0.50       0       8    0.00    6.79
//02:40:00   c0t2d0    1.00    0.50       2      24    0.00    7.02
//02:40:02   c0t2d0    0.50    0.50       0       8    0.01    8.80
//$
