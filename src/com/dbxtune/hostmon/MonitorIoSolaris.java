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

import com.dbxtune.utils.Configuration;

public class MonitorIoSolaris
extends MonitorIo
{
	public MonitorIoSolaris()
	{
		this(-1, null);
	}
	public MonitorIoSolaris(int utilVersion, String utilExtraInfo)
	{
		super(utilVersion, utilExtraInfo);
	}

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
	public HostMonitorMetaData createMetaData(int utilVersion, Configuration utilExtraInfo)
	{
		HostMonitorMetaData md = new HostMonitorMetaData();
		md.setTableName(getModuleName());

		md.addStrColumn( "device",             1,  1, false,   30, "Disk device name");
		md.addIntColumn( "samples",            2,  0, true,        "Number of 'sub' sample entries of iostat this value is based on");

		md.addStatColumn("readsPerSec",        3,  2, true, 10, 1, "Reads per second");
		md.addStatColumn("writesPerSec",       4,  3, true, 10, 1, "Writes per second");
		md.addStatColumn("kbReadPerSec",       5,  4, true, 10, 1, "KB read per second");
		md.addStatColumn("kbWritePerSec",      6,  5, true, 10, 1, "KB written per second");
		md.addDecColumn ("avgReadKbPerIo",     7,  0, true, 10, 1, "Avergare read size in KB per IO. Formula: kbReadPerSec/readsPerSec");
		md.addDecColumn ("avgWriteKbPerIo",    8,  0, true, 10, 1, "Avergare write size in KB per IO. Formula: kbWritePerSec/writePerSec");
		md.addStatColumn("wait",               9,  6, true, 10, 1, "Average number of transactions waiting for service (Q length)");
		md.addStatColumn("actv",              10,  7, true, 10, 1, "Average number of transactions actively being serviced (removed from the queue but not yet completed)");
		md.addStatColumn("svc_t",             11,  8, true, 10, 1, "Service time (ms). Includes everything: wait time, active queue time, seek rotation, transfer time");
		md.addStatColumn("waitPct",           12,  9, true, 5,  1, "Percent of time there are transactions waiting for service (queue non-empty)");
		md.addStatColumn("busyPct",           13, 10, true, 5,  1, "Percent of time the disk is busy (transactions in progress)");

		md.addStrColumn ("deviceDescription", 14,  0, true,   255, "Mapping of the column 'device' to your own description.");

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
