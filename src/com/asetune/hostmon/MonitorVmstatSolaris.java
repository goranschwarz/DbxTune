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

public class MonitorVmstatSolaris
extends MonitorVmstat
{
	public MonitorVmstatSolaris()
	{
		this(-1, null);
	}
	public MonitorVmstatSolaris(int utilVersion, String utilExtraInfo)
	{
		super(utilVersion, utilExtraInfo);
	}

	@Override
	public String getModuleName()
	{
		return "MonitorVmstatSolaris";
	}

	@Override
	public String getCommand()
	{
		String cmd = super.getCommand();
		return cmd != null ? cmd : "vmstat "+getSleepTime();
	}

	@Override
	public HostMonitorMetaData createMetaData(int utilVersion, Configuration utilExtraInfo)
	{
		HostMonitorMetaData md = new HostMonitorMetaData();
		md.setTableName(getModuleName());

		md.addDatetimeColumn("sampleTime", 1,  0, true, "Approximately when this record was samples");

		md.addIntColumn("kthr_r",      2,  1, true, "the number of kernel threads in run queue");
		md.addIntColumn("kthr_b",      3,  2, true, "the number of blocked kernel threads that are waiting for resources I/O, paging, and so forth");
		md.addIntColumn("kthr_w",      4,  3, true, "the number of swapped out lightweight processes (LWPs) that are waiting for processing resources to finish.");

		md.addIntColumn("memory_swap", 5,  4, true, "available swap space (Kbytes)");
		md.addIntColumn("memory_free", 6,  5, true, "size of the free list (Kbytes)");

		md.addIntColumn("page_re",     7,  6, true, "page reclaims - but see the -S option for how this field is modified.");
		md.addIntColumn("page_mf",     8,  7, true, "minor faults - but see the -S option for how this field is modified.");
		md.addIntColumn("page_pi",     9,  8, true, "kilobytes paged in");
		md.addIntColumn("page_po",    10,  9, true, "kilobytes paged out");
		md.addIntColumn("page_fr",    11, 10, true, "kilobytes freed");
		md.addIntColumn("page_de",    12, 11, true, "anticipated short-term memory shortfall (Kbytes)");
		md.addIntColumn("page_sr",    13, 12, true, "pages scanned by clock algorithm");

		md.addIntColumn("disk_1",     14, 13, true, "");
		md.addIntColumn("disk_2",     15, 14, true, "");
		md.addIntColumn("disk_3",     16, 15, true, "");
		md.addIntColumn("disk_4",     17, 16, true, "");

		md.addIntColumn("faults_in",  18, 17, true, "interrupts");
		md.addIntColumn("faults_sy",  19, 18, true, "system calls");
		md.addIntColumn("faults_cs",  20, 19, true, "CPU context switches");

		md.addIntColumn("cpu_us",     21, 20, true, "user time");
		md.addIntColumn("cpu_sy" ,    22, 21, true, "system time");
		md.addIntColumn("cpu_id",     23, 22, true, "idle time");

		// Set Percent columns
		md.setPercentCol("cpu_us");
		md.setPercentCol("cpu_sy");
		md.setPercentCol("cpu_id");
		

		// Set column "sampleTime", to a special status, which will contain the  
		// underlying sample TIME the record was sampled
		md.setStatusCol("sampleTime", HostMonitorMetaData.STATUS_COL_SAMPLE_TIME);

		// What regexp to use to split the input row into individual fields
		md.setParseRegexp(HostMonitorMetaData.REGEXP_IS_SPACE);

		// Skip the header line
		md.setSkipRows("memory_swap", "swap");

		// Get SKIP and ALLOW from the Configuration
		md.setSkipAndAllowRows(null, Configuration.getCombinedConfiguration());
		md.setSkipAndAllowRows("hostmon.MonitorVmstat.", Configuration.getCombinedConfiguration());

		return md;
	}
}
