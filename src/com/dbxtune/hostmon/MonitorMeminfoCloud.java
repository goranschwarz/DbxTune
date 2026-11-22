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
 * Cloud implementation of {@link MonitorMeminfo} (populates <code>CmOsMeminfo</code> from {@link CloudMetricsSnapshot}).
 */
public class MonitorMeminfoCloud
extends MonitorMeminfoLinux
{
	public MonitorMeminfoCloud()
	{
		this(-1, null);
	}

	public MonitorMeminfoCloud(int utilVersion, String utilExtraInfo)
	{
		super(utilVersion, utilExtraInfo);
	}

	@Override
	public String getModuleName()
	{
		return "MonitorMeminfoCloud";
	}

	@Override
	public String getCommand()
	{
		return "Cloud API: database/memory/quota, total_usage, components, swap_utilization";
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

			long memTotalKb     = snap.getMemoryQuotaBytes() / 1024L;
			long memCachedKb    = snap.getMemoryCacheBytes() / 1024L;
			long memActiveKb    = snap.getMemoryUsageBytes() / 1024L;
			long memFreeKb      = snap.getMemoryFreeBytes()  / 1024L;
			long memAvailableKb = memFreeKb + memCachedKb;
			long swapUsedKb     = snap.getSwapUsedBytes()    / 1024L;
			long swapFreeKb     = snap.getSwapFreeBytes()    / 1024L;

			addMemRow(table, md, "MemTotal",     memTotalKb);
			addMemRow(table, md, "MemFree",      memFreeKb);
			addMemRow(table, md, "MemAvailable", memAvailableKb);
			addMemRow(table, md, "Cached",       memCachedKb);
			addMemRow(table, md, "Buffers",      0L);
			addMemRow(table, md, "Active",       memActiveKb);
			addMemRow(table, md, "SwapTotal",    swapUsedKb + swapFreeKb);
			addMemRow(table, md, "SwapFree",     swapFreeKb);

			return table;
		}
		catch (Exception ex)
		{
			addException(ex);
			return null;
		}
	}

	private static void addMemRow(OsTable table, HostMonitorMetaData md, String memoryType, long kbVal)
	throws OsRecordParseException
	{
		String[] vals = new String[] { memoryType, Long.toString(kbVal), "kB" };
		table.addRow(new OsTableRow(md, vals));
	}
}
