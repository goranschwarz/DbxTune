/*******************************************************************************
 * Copyright (C) 2010-2025 Goran Schwarz
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
package com.dbxtune.hostmon;

import com.dbxtune.ssh.SshConnection;
import com.dbxtune.utils.Configuration;

public class MonitorMeminfoLinux
extends HostMonitor
{
//	private static final Logger _logger = LogManager.getLogger(MethodHandles.lookup().lookupClass());

	/*
        gorans@gorans-ub2:~$ cat /proc/meminfo
        MemTotal:       16315784 kB
        MemFree:          545140 kB
        MemAvailable:   11732672 kB
        Buffers:          974036 kB
        Cached:         10086180 kB
        SwapCached:          348 kB
        Active:          7932608 kB
        Inactive:        6132448 kB
        Active(anon):    2951504 kB
        Inactive(anon):  1067064 kB
        Active(file):    4981104 kB
        Inactive(file):  5065384 kB
        Unevictable:          32 kB
        Mlocked:              32 kB
        SwapTotal:      16658428 kB
        SwapFree:       16622984 kB
        Dirty:               256 kB
        Writeback:             0 kB
        AnonPages:       3004616 kB
        Mapped:          1059040 kB
        Shmem:           1013728 kB
        Slab:            1547172 kB
        SReclaimable:    1480836 kB
        SUnreclaim:        66336 kB
        KernelStack:       13456 kB
        PageTables:        48536 kB
        NFS_Unstable:          0 kB
        Bounce:                0 kB
        WritebackTmp:          0 kB
        CommitLimit:    24816320 kB
        Committed_AS:    9934416 kB
        VmallocTotal:   34359738367 kB
        VmallocUsed:           0 kB
        VmallocChunk:          0 kB
        HardwareCorrupted:     0 kB
        AnonHugePages:   1779712 kB
        CmaTotal:              0 kB
        CmaFree:               0 kB
        HugePages_Total:       0
        HugePages_Free:        0
        HugePages_Rsvd:        0
        HugePages_Surp:        0
        Hugepagesize:       2048 kB
        DirectMap4k:      249676 kB
        DirectMap2M:    16410624 kB
        DirectMap1G:     1048576 kB
        gorans@gorans-ub2:~$

	*/
	
	public MonitorMeminfoLinux()
	{
		this(-1, null);
	}
	public MonitorMeminfoLinux(int utilVersion, String utilExtraInfo)
	{
		super(utilVersion, utilExtraInfo);
	}

	@Override
	public String getModuleName()
	{
		return "MonitorMeminfoLinux";
	}

	@Override
	public String getCommand()
	{
		String cmd = super.getCommand();
		return cmd != null ? cmd : "cat /proc/meminfo";
	}

	@Override
	public HostMonitorMetaData createMetaData(int utilVersion, Configuration utilExtraInfo)
	{
		HostMonitorMetaData md = new HostMonitorMetaData();
		md.setTableName(getModuleName());

		md.addStrColumn ("memoryType",           1, 1, false, 20,    "What memory are we looking at");
		md.addLongColumn("used",                 2, 2, false,        "How much memory is used by this module");
		md.addDecColumn ("usedDiff",             3, 0, false, 15, 1, "How much memory is used by this module, since last sample");
		md.addDecColumn ("usedRate",             4, 0, false, 15, 1, "How much memory is used by this module, memory per second, since last sample");
		md.addStrColumn ("unit",                 5, 3, true,  10,    "What unit is this in (KB, MB) ");
		md.addStrColumn ("description",          6, 0, true,  400,   "Description of the field");
                                                 
		// What regexp to use to split the input row into individual fields
		md.setParseRegexp(HostMonitorMetaData.REGEXP_IS_SPACE);

		md.setOsCommandStreaming(false);
		
		md.setPkCol("memoryType");
		
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

//System.out.println("########## parseRow(): row="+row+", preParsed.length="+preParsed.length+", preParsed="+StringUtil.toCommaStrQuoted('|', preParsed));
		// Check preParsed for correct length: if not correct: print out some info for debugging.
		if (preParsed.length != md.getParseColumnCount())
		{
			// Some row only have 2 columns (no "unit" kB at the end)
			if (preParsed.length == 2)
			{
				String[] tmp = new String[3];
				tmp[0]  = preParsed[0];
				tmp[1]  = preParsed[1];
				tmp[2]  = "";
				
				preParsed = tmp;
			}
		}

		// Note: "usedDiff" will be filled in by CmOsMeminfo.localCalculation(OsTable osSampleTable)

		// Let "super" do it's intended work
		String[] data = super.parseRow(md, row, preParsed, type);
		if (data == null)
			return null;

		// Strip off *trailing* ':' characters in ALL fields
		for (int i=0; i<data.length; i++)
			if (data[i].endsWith(":"))
				data[i] = data[i].substring(0, data[i].length()-1);

		return data;
	}
}
