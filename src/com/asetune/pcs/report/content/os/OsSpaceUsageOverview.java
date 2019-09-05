/*******************************************************************************
 * Copyright (C) 2010-2019 Goran Schwarz
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

import com.asetune.pcs.report.DailySummaryReportAbstract;
import com.asetune.pcs.report.content.ReportChartObject;
import com.asetune.pcs.report.content.ase.AseAbstract;
import com.asetune.sql.conn.DbxConnection;
import com.asetune.utils.Configuration;

public class OsSpaceUsageOverview extends AseAbstract
{
//	private static Logger _logger = Logger.getLogger(OsCpuUsageOverview.class);

	public OsSpaceUsageOverview(DailySummaryReportAbstract reportingInstance)
	{
		super(reportingInstance);
	}

	@Override
	public String getMsgAsText()
	{
		return "Text Report is not implemented";
	}

	@Override
	public String getMsgAsHtml()
	{
		StringBuilder sb = new StringBuilder();

		sb.append(getDbxCentralLinkWithDescForGraphs(false, "Below are Disk Space Usage on the Operating System Level.",
				"CmOsDiskSpace_FsAvailableMb",
				"CmOsDiskSpace_FsUsedMb",
				"CmOsDiskSpace_FsUsedPct"
				));

		sb.append(_CmOsDiskSpace_FsAvailableMb.getHtmlContent(null, null));
		sb.append(_CmOsDiskSpace_FsUsedMb     .getHtmlContent(null, null));
		sb.append(_CmOsDiskSpace_FsUsedPct    .getHtmlContent(null, null));
		
		if (hasProblem())
			sb.append("<pre>").append(getProblem()).append("</pre> \n");

		sb.append("\n<br>");

		return sb.toString();
	}

	@Override
	public String getSubject()
	{
		return "OS Disk Space Usage (origin: CmOsDiskSpace_abs / os-cmd:df)";
	}

	@Override
	public boolean hasIssueToReport()
	{
		return false; // even if we found entries, do NOT indicate this as a Problem or Issue
	}


	@Override
	public void create(DbxConnection conn, String srvName, Configuration conf)
	{
		int maxValue = 100;
		_CmOsDiskSpace_FsAvailableMb = createChart(conn, "CmOsDiskSpace", "FsAvailableMb", -1,       null, "df: Space Available in MB, at MountPoint (Host Monitor->OS Disk Space Usage(df))");
		_CmOsDiskSpace_FsUsedMb      = createChart(conn, "CmOsDiskSpace", "FsUsedMb",      -1,       null, "df: Space Used in MB, at MountPoint (Host Monitor->OS Disk Space Usage(df))");
		_CmOsDiskSpace_FsUsedPct     = createChart(conn, "CmOsDiskSpace", "FsUsedPct",     maxValue, null, "df: Space Used in Percent, at MountPoint (Host Monitor->OS Disk Space Usage(df))");
	}

	private ReportChartObject _CmOsDiskSpace_FsAvailableMb;
	private ReportChartObject _CmOsDiskSpace_FsUsedMb;
	private ReportChartObject _CmOsDiskSpace_FsUsedPct;
}
