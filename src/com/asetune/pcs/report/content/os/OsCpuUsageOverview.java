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
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 * 
 * DbxTune is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License
 * along with DbxTune.  If not, see <http://www.gnu.org/licenses/>.
 ******************************************************************************/
package com.asetune.pcs.report.content.os;

import java.io.IOException;
import java.io.Writer;

import com.asetune.pcs.report.DailySummaryReportAbstract;
import com.asetune.pcs.report.content.IReportChart;
import com.asetune.sql.conn.DbxConnection;
import com.asetune.utils.Configuration;
import com.asetune.utils.StringUtil;

public class OsCpuUsageOverview extends OsAbstract
{
//	private static Logger _logger = Logger.getLogger(OsCpuUsageOverview.class);

	public OsCpuUsageOverview(DailySummaryReportAbstract reportingInstance)
	{
		super(reportingInstance);
	}

	@Override
	public boolean isEnabled()
	{
		// If super is DISABLED, no need to continue
		boolean isEnabled = super.isEnabled();
		if ( ! isEnabled )
			return isEnabled;

//		// NOT For Windows
//		String dbmsVerStr = getReportingInstance().getDbmsVersionStr();
//		if (StringUtil.hasValue(dbmsVerStr))
//		{
//			if (dbmsVerStr.indexOf("Windows") != -1)
//			{
//				setDisabledReason("This DBMS is running on Windows, wich is not supported by this report.");
//				return false;
//			}
//		}
		return true;
	}
	
	@Override
	public boolean hasMinimalMessageText()
	{
		return true;
	}

	@Override
	public boolean hasShortMessageText()
	{
		return true;
	}

//	@Override
//	public void writeShortMessageText(Writer w)
//	throws IOException
//	{
//		writeMessageText(w);
//	}

	@Override
	public void writeMessageText(Writer sb, MessageType messageType)
	throws IOException
	{
		sb.append(getDbxCentralLinkWithDescForGraphs(false, "Below are CPU Graphs/Charts with various information that can help you decide how the DBMS is handling the load on the Operating System Level.",
				"CmOsMpstat_MpSum",
				"CmOsMpstat_MpCpu",
				"CmOsUptime_AdjLoadAverage",
				_isWindows ? "CmOsMeminfo_WinPaging" : "CmOsVmstat_SwapInOut"
				));

		_CmOsMpstat_MpSum         .writeHtmlContent(sb, null, null);
		_CmOsMpstat_MpCpu         .writeHtmlContent(sb, null, null);
		_CmOsUptime_AdjLoadAverage.writeHtmlContent(sb, null, null);
		
		if (_CmOsVmstat_SwapInOut  != null) _CmOsVmstat_SwapInOut .writeHtmlContent(sb, null, null);
		if (_CmOsMeminfo_WinPaging != null)	_CmOsMeminfo_WinPaging.writeHtmlContent(sb, null, null);

		_CmOsMeminfo_MemAvailable .writeHtmlContent(sb, null, null);
	}

	@Override
	public String getSubject()
	{
		return "OS CPU Usage graph of the full day (origin: CmOsMpstat_abs,CmOsUptime_abs / os-cmd:mpstat,uptime)";
	}

	@Override
	public boolean hasIssueToReport()
	{
		return false; // even if we found entries, do NOT indicate this as a Problem or Issue
	}


	@Override
	public void create(DbxConnection conn, String srvName, Configuration pcsSavedConf, Configuration localConf)
	{
		// For Linux/Unix do NOT show chart-line for "idlePct"
		String idlePct = "idlePct";
		_isWindows = isWindows();

//		// If the SQL Server is hosted on Windows... remove chart-line: "% Idle Time" 
//		String dbmsVerStr = getReportingInstance().getDbmsVersionStr();
//		if (StringUtil.hasValue(dbmsVerStr))
//		{
//			if (dbmsVerStr.contains("Windows"))
//			{
//				_isWindows = true;
//				idlePct = "% Idle Time";
//			}
//		}
		if (_isWindows)
		{
			idlePct = "% Idle Time";
		}

		String schema = getReportingInstance().getDbmsSchemaName();
		
		int maxValue = 100;
		_CmOsMpstat_MpSum          = createTsLineChart(conn, schema, "CmOsMpstat", "MpSum",          maxValue, false, idlePct, "mpstat: CPU usage Summary (Host Monitor->OS CPU(mpstat))");
		_CmOsMpstat_MpCpu          = createTsLineChart(conn, schema, "CmOsMpstat", "MpCpu",          maxValue, false, null,    "mpstat: CPU usage per core (usr+sys+iowait) (Host Monitor->OS CPU(mpstat))");
		_CmOsUptime_AdjLoadAverage = createTsLineChart(conn, schema, "CmOsUptime", "AdjLoadAverage", -1,       false, null,    "uptime: Adjusted Load Average (Host Monitor->OS Load Average(uptime))");

		if ( ! _isWindows)
			_CmOsVmstat_SwapInOut  = createTsLineChart(conn, schema, "CmOsVmstat", "SwapInOut",      -1,       false, null,    "vmstat: Swap In/Out per sec (Host Monitor->OS CPU(vmstat))");
		else
			_CmOsMeminfo_WinPaging = createTsLineChart(conn, schema, "CmOsMeminfo", "WinPaging",     -1,       false, null,    "meminfo: Windows Paging or Swap Usage (Host Monitor->OS Memory Info)");
			
		_CmOsMeminfo_MemAvailable  = createTsLineChart(conn, schema, "CmOsMeminfo", "MemAvailable",  -1,       false, null,    "meminfo: Available Memory (Host Monitor->OS Memory Info)");
	}

	private boolean _isWindows = false;

	private IReportChart _CmOsMpstat_MpSum;
	private IReportChart _CmOsMpstat_MpCpu;
	private IReportChart _CmOsUptime_AdjLoadAverage;
	private IReportChart _CmOsVmstat_SwapInOut;
	private IReportChart _CmOsMeminfo_WinPaging;
	private IReportChart _CmOsMeminfo_MemAvailable;
}
