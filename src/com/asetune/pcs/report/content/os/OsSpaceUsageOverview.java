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
import java.io.StringWriter;
import java.io.Writer;

import com.asetune.pcs.report.DailySummaryReportAbstract;
import com.asetune.pcs.report.content.IReportChart;
import com.asetune.pcs.report.content.ase.AseAbstract;
import com.asetune.sql.conn.DbxConnection;
import com.asetune.utils.Configuration;
import com.asetune.utils.StringUtil;

public class OsSpaceUsageOverview extends AseAbstract
{
//	private static Logger _logger = Logger.getLogger(OsCpuUsageOverview.class);

	public OsSpaceUsageOverview(DailySummaryReportAbstract reportingInstance)
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

		// NOT For Windows
		String dbmsVerStr = getReportingInstance().getDbmsVersionStr();
		if (StringUtil.hasValue(dbmsVerStr))
		{
			if (dbmsVerStr.indexOf("Windows") >= 0)
			{
				setDisabledReason("This DBMS is running on Windows, wich is not supported by this report.");
				return false;
			}
		}
		return true;
	}
	
	@Override
	public void writeMessageText(Writer sb)
	throws IOException
	{
		sb.append(getDbxCentralLinkWithDescForGraphs(false, "Below are Disk Space Usage on the Operating System Level.",
				"CmOsDiskSpace_FsUsedPct",
				"CmOsDiskSpace_FsAvailableMb",
				"CmOsDiskSpace_FsUsedMb"
				));

		_CmOsDiskSpace_FsUsedPct    .writeHtmlContent(sb, null, null);
		_CmOsDiskSpace_FsAvailableMb.writeHtmlContent(sb, null, null);
		_CmOsDiskSpace_FsUsedMb     .writeHtmlContent(sb, null, null);
	}

//	@Override
//	public String getMessageText()
//	{
//		StringBuilder sb = new StringBuilder();
//
//		sb.append(getDbxCentralLinkWithDescForGraphs(false, "Below are Disk Space Usage on the Operating System Level.",
//				"CmOsDiskSpace_FsUsedPct",
//				"CmOsDiskSpace_FsAvailableMb",
//				"CmOsDiskSpace_FsUsedMb"
//				));
//
//		sb.append(_CmOsDiskSpace_FsUsedPct    .getHtmlContent(null, null));
//		sb.append(_CmOsDiskSpace_FsAvailableMb.getHtmlContent(null, null));
//		sb.append(_CmOsDiskSpace_FsUsedMb     .getHtmlContent(null, null));
//		
//		return sb.toString();
//	}

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
	public void create(DbxConnection conn, String srvName, Configuration pcsSavedConf, Configuration localConf)
	{
		int maxValue = 100;
		_CmOsDiskSpace_FsUsedPct     = createTsLineChart(conn, "CmOsDiskSpace", "FsUsedPct",     maxValue, null, "df: Space Used in Percent, at MountPoint (Host Monitor->OS Disk Space Usage(df))");
		_CmOsDiskSpace_FsAvailableMb = createTsLineChart(conn, "CmOsDiskSpace", "FsAvailableMb", -1,       null, "df: Space Available in MB, at MountPoint (Host Monitor->OS Disk Space Usage(df))");
		_CmOsDiskSpace_FsUsedMb      = createTsLineChart(conn, "CmOsDiskSpace", "FsUsedMb",      -1,       null, "df: Space Used in MB, at MountPoint (Host Monitor->OS Disk Space Usage(df))");
	}

	private IReportChart _CmOsDiskSpace_FsUsedPct;
	private IReportChart _CmOsDiskSpace_FsAvailableMb;
	private IReportChart _CmOsDiskSpace_FsUsedMb;
}
