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

import com.dbxtune.cloud.CloudMetricsSnapshot;
import com.dbxtune.utils.Configuration;

/**
 * Cloud implementation of {@link MonitorNwInfo} (populates <code>CmOsNwInfo</code> from {@link CloudMetricsSnapshot}).
 */
public class MonitorNwInfoCloud
extends MonitorNwInfoLinux
{
	public MonitorNwInfoCloud()
	{
		this(-1, null);
	}

	public MonitorNwInfoCloud(int utilVersion, String utilExtraInfo)
	{
		super(utilVersion, utilExtraInfo);
	}

	@Override
	public String getModuleName()
	{
		return "MonitorNwInfoCloud";
	}

	@Override
	public String getCommand()
	{
		return "Cloud API: database/network/received_bytes_count, sent_bytes_count, connections";
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

			long rBytes = snap.getNetRxBytesTotal();
			long tBytes = snap.getNetTxBytesTotal();
			// Estimate packet count assuming ~1024B average payload if packet counter is not exposed by Cloud SQL
			long rPkts  = Math.max(0L, rBytes / 1024L);
			long tPkts  = Math.max(0L, tBytes / 1024L);

			String[] vals = new String[] {
				"cloudsql-nic0",
				Long.toString(rBytes),
				Long.toString(rPkts),
				"0", "0", "0", "0", "0", "0",
				Long.toString(tBytes),
				Long.toString(tPkts),
				"0", "0", "0", "0", "0", "0"
			};

			OsTableRow row = new OsTableRow(md, vals);
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
