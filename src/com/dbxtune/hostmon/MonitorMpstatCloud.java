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
 * Cloud implementation of {@link MonitorMpstat} (populates <code>CmOsMpstat</code> from {@link CloudMetricsSnapshot}).
 */
public class MonitorMpstatCloud
extends MonitorMpstat
{
	public MonitorMpstatCloud()
	{
		this(-1, null);
	}

	public MonitorMpstatCloud(int utilVersion, String utilExtraInfo)
	{
		super(utilVersion, utilExtraInfo);
	}

	@Override
	public String getModuleName()
	{
		return "MonitorMpstatCloud";
	}

	@Override
	public String getCommand()
	{
		return "Cloud API: database/cpu/utilization, database/cpu/reserved_cores";
	}

	@Override
	public HostMonitorMetaData createMetaData(int utilVersion, Configuration utilExtraInfo)
	{
		HostMonitorMetaData md = new HostMonitorMetaData();
		md.setTableName(getModuleName());

		md.addDatetimeColumn("sampleTime",    1,  0, true,        "Approximately when this record was sampled");
		md.addStrColumn     ("subSampleTime", 2,  1, true, 20,    "Time of the Cloud metric data point");
		md.addStrColumn     ("CPU",           3,  2, false, 10,   "Processor number (all for Cloud instance aggregate, or vCPU count)");
		md.addIntColumn     ("samples",       4,  0, true,        "Number of 'sub' sample entries for this entry");

		md.addDecColumn     ("usrPct",        5,  3, true,  5, 1, "Cloud instance CPU utilization percentage (0.0 - 100.0).");
		md.addDecColumn     ("nicePct",       6,  4, true,  5, 1, "Not applicable on managed Cloud instances (0.0).");
		md.addDecColumn     ("sysPct",        7,  5, true,  5, 1, "Not split out by Cloud provider (included in usrPct).");
		md.addDecColumn     ("iowaitPct",     8,  6, true,  5, 1, "Not split out by Cloud provider (0.0).");
		md.addDecColumn     ("irqPct",        9,  7, true,  5, 1, "Not applicable on managed Cloud instances (0.0).");
		md.addDecColumn     ("softPct",      10,  8, true,  5, 1, "Not applicable on managed Cloud instances (0.0).");
		md.addDecColumn     ("stealPct",     11,  9, true,  5, 1, "Not applicable on managed Cloud instances (0.0).");
		md.addDecColumn     ("guestPct",     12, 10, true,  5, 1, "Not applicable on managed Cloud instances (0.0).");
		md.addDecColumn     ("idlePct",      13, 11, true,  5, 1, "Percentage of idle CPU capacity (100.0 - usrPct).");

		md.setStatusCol("sampleTime", HostMonitorMetaData.STATUS_COL_SAMPLE_TIME);
		md.setStatusCol("samples",    HostMonitorMetaData.STATUS_COL_SUB_SAMPLE);

		md.setPercentCol("usrPct", "nicePct", "sysPct", "iowaitPct", "irqPct", "softPct", "stealPct", "guestPct", "idlePct");
		md.setPkCol("CPU");
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

			double utilPct = snap.getCpuUtilizationPct();
			double idlePct = Math.max(0.0, 100.0 - utilPct);
			String timeStr = new SimpleDateFormat("HH:mm:ss").format(snap.getMetricTimestamp() != null ? snap.getMetricTimestamp() : new Date());

			String[] vals = new String[] {
				timeStr,
				"all",
				String.format(Locale.US, "%.1f", utilPct),
				"0.0",
				"0.0",
				"0.0",
				"0.0",
				"0.0",
				"0.0",
				"0.0",
				String.format(Locale.US, "%.1f", idlePct)
			};

			OsTableRow row = new OsTableRow(md, vals);
			row.setValue(4, Integer.valueOf(1)); // samples = 1
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
