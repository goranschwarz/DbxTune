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

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

import com.dbxtune.cloud.CloudMetricsSnapshot;
import com.dbxtune.utils.Configuration;

/**
 * Cloud implementation of {@link MonitorUpTime} (populates <code>CmOsUptime</code> from {@link CloudMetricsSnapshot}).
 */
public class MonitorUpTimeCloud
extends MonitorUpTime
{
	public MonitorUpTimeCloud()
	{
		this(-1, null);
	}

	public MonitorUpTimeCloud(int utilVersion, String utilExtraInfo)
	{
		super(utilVersion, utilExtraInfo);
	}

	@Override
	public String getModuleName()
	{
		return "MonitorUpTimeCloud";
	}

	@Override
	public String getCommand()
	{
		return "Uptime: DBMS start time (CmSummary); Cloud API: database/cpu/usage_time, database/network/connections";
	}

	@Override
	public HostMonitorMetaData createMetaData(int utilVersion, Configuration utilExtraInfo)
	{
		HostMonitorMetaData md = new HostMonitorMetaData();
		md.setTableName(getModuleName());

		md.addStrColumn ("sampleTime",           1, 1, true, 10,    "Approximately when this record was sampled");
		md.addIntColumn ("daysUp",               2, 2, true,        "Instance has been up for this amount of days + the hoursUp column");
		md.addStrColumn ("hoursUp",              3, 3, true, 10,    "Instance has been up for (daysUp) + this HH:MM description");
		md.addIntColumn ("users",                4, 4, true,        "Active network connections to the Cloud SQL instance");

		md.addStatColumn("loadAverage_1Min",     5, 5, true, 8, 2,  "Effective active CPU core usage (1 min)");
		md.addStatColumn("loadAverage_5Min",     6, 6, true, 8, 2,  "Effective active CPU core usage (5 min)");
		md.addStatColumn("loadAverage_15Min",    7, 7, true, 8, 2,  "Effective active CPU core usage (15 min)");

		md.addIntColumn ("nproc",                8, 0, true,        "Reserved vCPUs on the Cloud SQL instance");
		md.addStatColumn("adjLoadAverage_1Min",  9, 0, true, 8, 2,  "Adjusted load average (1 min) divided by reserved vCPUs");
		md.addStatColumn("adjLoadAverage_5Min", 10, 0, true, 8, 2,  "Adjusted load average (5 min) divided by reserved vCPUs");
		md.addStatColumn("adjLoadAverage_15Min",11, 0, true, 8, 2,  "Adjusted load average (15 min) divided by reserved vCPUs");

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

			String timeStr = new SimpleDateFormat("HH:mm:ss").format(snap.getMetricTimestamp() != null ? snap.getMetricTimestamp() : new Date());
			long uptimeSec = snap.getUptimeSeconds();
			int  daysUp    = (int) (uptimeSec / 86400L);
			long remSec    = uptimeSec % 86400L;
			int  hours     = (int) (remSec / 3600L);
			int  mins      = (int) ((remSec % 3600L) / 60L);
			String hoursUp = String.format(Locale.US, "%02d:%02d", hours, mins);

			int cores = snap.getCpuReservedCores();
			double activeCores = snap.getCpuUsageTimeSecPerS();
			if (activeCores <= 0.0 && cores > 0 && snap.getCpuUtilizationPct() > 0.0)
			{
				activeCores = (snap.getCpuUtilizationPct() / 100.0) * cores;
			}
			String loadStr = String.format(Locale.US, "%.2f", activeCores);

			String[] vals = new String[] {
				timeStr,
				Integer.toString(daysUp),
				hoursUp,
				Integer.toString(snap.getActiveConnections()),
				loadStr,
				loadStr,
				loadStr
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
