/*******************************************************************************
 * Copyright (C) 2010-2027 Goran Schwarz
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

import java.util.Locale;

import com.dbxtune.cloud.CloudMetricsSnapshot;
import com.dbxtune.utils.Configuration;

/**
 * Cloud implementation of {@link MonitorIo} (populates <code>CmOsIostat</code> from {@link CloudMetricsSnapshot}).
 */
public class MonitorIoCloud
extends MonitorIo
{
	public MonitorIoCloud()
	{
		this(-1, null);
	}

	public MonitorIoCloud(int utilVersion, String utilExtraInfo)
	{
		super(utilVersion, utilExtraInfo);
	}

	@Override
	public String getModuleName()
	{
		return "MonitorIoCloud";
	}

	@Override
	public String getCommand()
	{
		return "Cloud API: database/disk/read_ops_count, write_ops_count, read_bytes_count, write_bytes_count";
	}

	@Override
	public HostMonitorMetaData createMetaData(int utilVersion, Configuration utilExtraInfo)
	{
		HostMonitorMetaData md = new HostMonitorMetaData();
		md.setTableName(getModuleName());

		md.addStrColumn ("device",            1,  1, false, 30,    "Disk device name (cloudsql-disk)");
		md.addIntColumn ("samples",           2,  0, true,         "Number of 'sub' sample entries for this device");
		md.addDecColumn ("rrqmPerSec",        3,  2, true, 10, 2,  "Read requests merged per second (0.0 on Cloud)");
		md.addDecColumn ("wrqmPerSec",        4,  3, true, 10, 2,  "Write requests merged per second (0.0 on Cloud)");
		md.addDecColumn ("readsPerSec",       5,  4, true, 10, 2,  "Read IOPS (operations per second)");
		md.addDecColumn ("writesPerSec",      6,  5, true, 10, 2,  "Write IOPS (operations per second)");
		md.addDecColumn ("kbReadPerSec",      7,  6, true, 10, 2,  "Kilobytes read from disk per second");
		md.addDecColumn ("kbWritePerSec",     8,  7, true, 10, 2,  "Kilobytes written to disk per second");
		md.addStatColumn("avgReadKbPerIo",    9,  0, true, 10, 1,  "Average Read KB Per IO (kbReadPerSec / readsPerSec)");
		md.addStatColumn("avgWriteKbPerIo",  10,  0, true, 10, 1,  "Average Write KB Per IO (kbWritePerSec / writesPerSec)");
		md.addDecColumn ("avgrq_sz",         11,  8, true, 10, 2,  "Average size (in 512-byte sectors) of I/O requests");
		md.addDecColumn ("avgqu_sz",         12,  9, true, 10, 2,  "Average queue length (not exposed by Cloud SQL)");
		md.addDecColumn ("await",            13, 10, true, 10, 2,  "Average wait time in ms (not exposed by Cloud SQL)");
		md.addDecColumn ("r_await",          14, 11, true, 10, 2,  "Average read wait time in ms (not exposed by Cloud SQL)");
		md.addDecColumn ("w_await",          15, 12, true, 10, 2,  "Average write wait time in ms (not exposed by Cloud SQL)");
		md.addDecColumn ("svctm",            16, 13, true, 10, 2,  "Average service time in ms (not exposed by Cloud SQL)");
		md.addDecColumn ("utilPct",          17, 14, true,  5, 2,  "Disk Quota Utilization % (max of Read/Write IOPS and Throughput vs Cloud Quota)");
		md.addStrColumn ("deviceDescription",18,  0, true, 255,    "Cloud Disk Quota details (IOPS and Throughput limits)");

		md.setStatusCol("samples", HostMonitorMetaData.STATUS_COL_SUB_SAMPLE);
		md.setPercentCol("utilPct");
		md.setPkCol("device");
		md.setOsCommandStreaming(false);

		return md;
	}

	@Override
	public OsTable executeAndParse()
	{
		try
		{
			HostMonitorConnection conn = getConnection();
			if (!(conn instanceof HostMonitorConnectionCloud))
				return null;

			HostMonitorConnectionCloud cloudConn = (HostMonitorConnectionCloud) conn;
			CloudMetricsSnapshot snap = cloudConn.getMetricsSnapshot();
			if (snap == null)
				return null;

			HostMonitorMetaData md = getMetaData();
			OsTable table = new OsTable(md);

			double rOps  = snap.getDiskReadOpsPerSec();
			double wOps  = snap.getDiskWriteOpsPerSec();
			double rKb   = snap.getDiskReadBytesPerSec()  / 1024.0;
			double wKb   = snap.getDiskWriteBytesPerSec() / 1024.0;

			double totOps = rOps + wOps;
			double totKb  = rKb  + wKb;
			// avgrq_sz is in 512-byte sectors (1 KB = 2 sectors)
			double avgrqSz = totOps > 0.0 ? (totKb * 2.0) / totOps : 0.0;

			// Calculate utilPct as highest utilization against provisioned Cloud SQL IOPS/Throughput quotas
			double quotaUtilPct = 0.0;
			if (snap.getDiskReadOpsQuota() > 0)
				quotaUtilPct = Math.max(quotaUtilPct, (rOps / snap.getDiskReadOpsQuota()) * 100.0);
			if (snap.getDiskWriteOpsQuota() > 0)
				quotaUtilPct = Math.max(quotaUtilPct, (wOps / snap.getDiskWriteOpsQuota()) * 100.0);
			if (snap.getDiskReadBytesQuota() > 0)
				quotaUtilPct = Math.max(quotaUtilPct, (snap.getDiskReadBytesPerSec() / snap.getDiskReadBytesQuota()) * 100.0);
			if (snap.getDiskWriteBytesQuota() > 0)
				quotaUtilPct = Math.max(quotaUtilPct, (snap.getDiskWriteBytesPerSec() / snap.getDiskWriteBytesQuota()) * 100.0);
			quotaUtilPct = Math.min(100.0, quotaUtilPct);

			String[] vals = new String[] {
				"cloudsql-disk",
				"0.00",
				"0.00",
				String.format(Locale.US, "%.2f", rOps),
				String.format(Locale.US, "%.2f", wOps),
				String.format(Locale.US, "%.2f", rKb),
				String.format(Locale.US, "%.2f", wKb),
				String.format(Locale.US, "%.2f", avgrqSz),
				"0.00",
				"0.00",
				"0.00",
				"0.00",
				"0.00",
				String.format(Locale.US, "%.2f", quotaUtilPct)
			};

			OsTableRow row = new OsTableRow(md, vals);
			row.setValue(2, Integer.valueOf(1)); // samples = 1

			StringBuilder desc = new StringBuilder("Cloud SQL Disk");
			if (snap.getDiskReadOpsQuota() > 0 || snap.getDiskWriteOpsQuota() > 0)
			{
				desc.append(" [ReadQuota=").append(snap.getDiskReadOpsQuota()).append(" IOPS");
				if (snap.getDiskReadBytesQuota() > 0)
					desc.append(" / ").append(String.format(Locale.US, "%.0f MB/s", snap.getDiskReadBytesQuota() / (1024.0 * 1024.0)));
				desc.append(", WriteQuota=").append(snap.getDiskWriteOpsQuota()).append(" IOPS");
				if (snap.getDiskWriteBytesQuota() > 0)
					desc.append(" / ").append(String.format(Locale.US, "%.0f MB/s", snap.getDiskWriteBytesQuota() / (1024.0 * 1024.0)));
				desc.append("]");
			}
			row.setValue(18, desc.toString());

			table.addRow(row);
			return table;
		}
		catch (Exception ex)
		{
			addException(ex);
			return null;
		}
	}
}
