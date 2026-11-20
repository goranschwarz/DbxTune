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
 * Cloud implementation of {@link MonitorDiskSpace} (populates <code>CmOsDiskSpace</code> from {@link CloudMetricsSnapshot}).
 */
public class MonitorDiskSpaceCloud
extends MonitorDiskSpaceAllOs
{
	public MonitorDiskSpaceCloud()
	{
		this(-1, null);
	}

	public MonitorDiskSpaceCloud(int utilVersion, String utilExtraInfo)
	{
		super(utilVersion, utilExtraInfo);
	}

	@Override
	public String getModuleName()
	{
		return "MonitorDiskSpaceCloud";
	}

	@Override
	public String getCommand()
	{
		return "Cloud API: database/disk/quota, bytes_used, bytes_used_by_data_type";
	}

	@Override
	public HostMonitorMetaData createMetaData(int utilVersion, Configuration utilExtraInfo)
	{
		return super.createMetaData(utilVersion, utilExtraInfo);
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

			long sizeKb  = snap.getDiskQuotaBytes() / 1024L;
			long usedKb  = snap.getDiskBytesUsed()  / 1024L;
			long availKb = Math.max(0L, sizeKb - usedKb);
			double usedPct = sizeKb > 0 ? Math.min(100.0, (usedKb * 100.0) / sizeKb) : 0.0;

			StringBuilder fsLabel = new StringBuilder("cloudsql-disk");
			if (snap.getDiskDataBytes() > 0 || snap.getDiskBinlogBytes() > 0 || snap.getDiskTmpBytes() > 0)
			{
				fsLabel.append(" [data=").append(formatBytes(snap.getDiskDataBytes()));
				fsLabel.append(", wal=").append(formatBytes(snap.getDiskBinlogBytes()));
				if (snap.getDiskTmpBytes() > 0)
					fsLabel.append(", tmp=").append(formatBytes(snap.getDiskTmpBytes()));
				if (snap.getDiskOtherBytes() > 0)
					fsLabel.append(", other=").append(formatBytes(snap.getDiskOtherBytes()));
				fsLabel.append("]");
			}

			String[] vals = new String[] {
				"cloudsql-disk",
				Long.toString(sizeKb),
				Long.toString(usedKb),
				Long.toString(availKb),
				String.format(Locale.US, "%.2f", usedPct),
				"/var/lib/postgresql/data"
			};

			OsTableRow row = new OsTableRow(md, vals);
			row.setValue(1, fsLabel.toString()); // Set Filesystem with full data/wal/tmp breakdown
			table.addRow(row);

			return table;
		}
		catch (Exception ex)
		{
			addException(ex);
			return null;
		}
	}

	private static String formatBytes(long bytes)
	{
		double gb = bytes / (1024.0 * 1024.0 * 1024.0);
		if (gb >= 1.0)
			return String.format(Locale.US, "%.2fGB", gb);
		double mb = bytes / (1024.0 * 1024.0);
		return String.format(Locale.US, "%.1fMB", mb);
	}
}
