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

import com.asetune.utils.Configuration;

public class MonitorNwInfoLinux
extends MonitorNwInfo
{
//	private static Logger _logger = Logger.getLogger(MonitorNwInfoLinux.class);

	/*
	 * -----------------------------------------------------------
	 * Raw output
	 * -----------------------------------------------------------
     *  gorans@gorans-ub2:~$ cat /proc/net/dev
     *  Inter-|   Receive                                                |  Transmit
     *   face |bytes    packets errs drop fifo frame compressed multicast|bytes    packets errs drop fifo colls carrier compressed
     *      lo: 4287452981 9910912    0    0    0     0          0         0 4287452981 9910912    0    0    0     0       0          0
     *   wlan0:       0       0    0    0    0     0          0         0        0       0    0    0    0     0       0          0
     *    eth0: 54703182673 100413817    0    0    0     0          0   3711027 161310358379 151423257    0    0    0     0       0          0
     *  gorans@gorans-ub2:~$
     *
     *
	 * -----------------------------------------------------------
	 * With some formating
	 * -----------------------------------------------------------
     *  gorans@gorans-ub2:~$ cat /proc/net/dev | sed '1d' | sed 's/|/ /g' | column -t
     *  face    bytes        packets    errs  drop  fifo  frame  compressed  multicast  bytes         packets    errs  drop  fifo  colls  carrier  compressed
     *  lo:     4287446092   9910884    0     0     0     0      0           0          4287446092    9910884    0     0     0     0      0        0
     *  wlan0:  0            0          0     0     0     0      0           0          0             0          0     0     0     0      0        0
     *  eth0:   54703173945  100413725  0     0     0     0      0           3711026    161310351530  151423191  0     0     0     0      0        0
     *  gorans@gorans-ub2:~$
	 */
	
	public MonitorNwInfoLinux()
	{
		this(-1, null);
	}
	public MonitorNwInfoLinux(int utilVersion, String utilExtraInfo)
	{
		super(utilVersion, utilExtraInfo);
	}

	@Override
	public String getModuleName()
	{
		return "MonitorNwInfoLinux";
	}

	@Override
	public String getCommand()
	{
		String cmd = super.getCommand();
		return cmd != null ? cmd : "cat /proc/net/dev | sed '1,2d'";
	}

	@Override
	public HostMonitorMetaData createMetaData(int utilVersion, Configuration utilExtraInfo)
	{
		HostMonitorMetaData md = new HostMonitorMetaData();
		md.setTableName(getModuleName());

		// description grabbed from: http://www.onlamp.com/pub/a/linux/2000/11/16/LinuxAdmin.html
		
		md.addStrColumn ("interface",            1,  1, false, 20,   "The Network interface name");
		md.addLongColumn("r_bytes",              2,  2, false,       "The total number of bytes of data received by the interface.");
		md.addDecColumn ("r_KB",                 3,  0, false, 15,1, "The total number of KB of data received by the interface.");
		md.addDecColumn ("r_MB",                 4,  0, false, 15,1, "The total number of MB of data received by the interface.");
		md.addDecColumn ("r_Mbit",               5,  0, false, 15,1, "The total number of Mbit of data received by the interface. Formula: MB*8");
		md.addLongColumn("r_packets",            6,  3, false,       "The total number of packets of data received by the interface.");
		md.addIntColumn ("r_bPerPacket",         7,  0, false,       "Bytes per packet, Formula: r_bytes/r_packets.");
		md.addLongColumn("r_errs",               8,  4, false,       "The total number of receive errors detected by the device driver.");
		md.addLongColumn("r_drop",               9,  5, false,       "The total number of packets dropped by the device driver.");
		md.addLongColumn("r_fifo",              10,  6, false,       "The number of FIFO buffer ERRORS.");
		md.addLongColumn("r_frame",             11,  7, false,       "The number of packet framing ERRORS.");
		md.addLongColumn("r_compressed",        12,  8, false,       "The number of compressed packets received by the device driver. (This appears to be unused in the 2.2.15 kernel.)");
		md.addLongColumn("r_multicast",         13,  9, false,       "The number of multicast frames transmitted or received by the device driver.");

		md.addLongColumn("t_bytes",             14, 10, false,       "The total number of bytes of data transmitted by the interface.");
		md.addDecColumn ("t_KB",                15,  0, false, 15,1, "The total number of KB of data transmitted by the interface.");
		md.addDecColumn ("t_MB",                16,  0, false, 15,1, "The total number of MB of data transmitted by the interface.");
		md.addDecColumn ("t_Mbit",              17,  0, false, 15,1, "The total number of Mbit of data transmitted by the interface. Formula: MB*8");
		md.addLongColumn("t_packets",           18, 11, false,       "The total number of packets of data transmitted by the interface.");
		md.addIntColumn ("t_bPerPacket",        19,  0, false,       "Bytes per packet, Formula: t_bytes/t_packets.");
		md.addLongColumn("t_errs",              20, 12, false,       "The total number of transmit errors detected by the device driver.");
		md.addLongColumn("t_drop",              21, 13, false,       "The total number of packets dropped by the device driver.");
		md.addLongColumn("t_fifo",              22, 14, false,       "The number of FIFO buffer errors.");
		md.addLongColumn("t_colls",             23, 15, false,       "The number of collisions detected on the interface.");
		md.addLongColumn("t_carrier",           24, 16, false,       "The number of carrier losses detected by the device driver.");
		md.addLongColumn("t_compressed",        25, 17, false,       "The number of compressed packets transmitted by the device driver. (This appears to be unused in the 2.2.15 kernel.)");
		
		// What regexp to use to split the input row into individual fields
		md.setParseRegexp(HostMonitorMetaData.REGEXP_IS_SPACE);

		md.setOsCommandStreaming(false);
		
		md.setPkCol("interface");
		
		md.setDiffCol( "r_bytes", "r_KB", "r_MB", "r_Mbit", "r_packets",
		               "t_bytes", "t_KB", "t_MB", "t_Mbit", "t_packets");
		
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
		String[] sa = super.parseRow(md, row, preParsed, type);
		if (sa == null)
			return null;
		
		// Strip off trailing ":" from 'interface' column 1 (array pos 0)
		if ( sa[0] != null && sa[0].endsWith(":") )
			sa[0] = sa[0].substring(0, sa[0].length()-1);

		return sa;
	}
	
//	@Override
//	public String[] parseRow(HostMonitorMetaData md, String row, String[] preParsed, int type)
//	{
//		if (type == SshConnection.STDERR_DATA)
//			return null;
//
////System.out.println("########## parseRow(): row="+row+", preParsed.length="+preParsed.length+", preParsed="+StringUtil.toCommaStrQuoted('|', preParsed));
//		// Check preParsed for correct length: if not correct: print out some info for debugging.
//		if (preParsed.length != md.getParseColumnCount())
//		{
//			// Some row only have 2 columns (no "unit" kB at the end)
//			if (preParsed.length == 2)
//			{
//				String[] tmp = new String[3];
//				tmp[0]  = preParsed[0];
//				tmp[1]  = preParsed[1];
//				tmp[2]  = "";
//				
//				preParsed = tmp;
//			}
//		}
//
//		// Note: "usedDiff" will be filled in by CmOsMeminfo.localCalculation(OsTable osSampleTable)
//
//		// Let "super" do it's intended work
//		String[] data = super.parseRow(md, row, preParsed, type);
//		if (data == null)
//			return null;
//
//		// Strip off *trailing* ':' characters in ALL fields
//		for (int i=0; i<data.length; i++)
//			if (data[i].endsWith(":"))
//				data[i] = data[i].substring(0, data[i].length()-1);
//
//		return data;
//	}
}
