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
package com.dbxtune.pcs.report.content.os;

import java.io.IOException;
import java.io.Writer;

import com.dbxtune.pcs.report.DailySummaryReportAbstract;
import com.dbxtune.pcs.report.content.IReportChart;
import com.dbxtune.sql.conn.DbxConnection;
import com.dbxtune.utils.Configuration;

public class OsSpaceUsageOverview extends OsAbstract
{
//	private static final Logger _logger = LogManager.getLogger(MethodHandles.lookup().lookupClass());

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
//		String dbmsVerStr = getReportingInstance().getDbmsVersionStr();
//		if (StringUtil.hasValue(dbmsVerStr))
//		{
//			if (dbmsVerStr.indexOf("Windows") >= 0)
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
		return false;
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
		sb.append(getDbxCentralLinkWithDescForGraphs(false, "Below are Disk Space Usage on the Operating System Level.",
				"CmOsDiskSpace_FsUsedPct",
				"CmOsDiskSpace_FsAvailableMb",
				"CmOsDiskSpace_FsUsedMb"
				));

		_CmOsDiskSpace_FsUsedPct    .writeHtmlContent(sb, null, null);
		_CmOsDiskSpace_FsAvailableMb.writeHtmlContent(sb, null, null);
		_CmOsDiskSpace_FsUsedMb     .writeHtmlContent(sb, null, null);
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
	public void create(DbxConnection conn, String srvName, Configuration pcsSavedConf, Configuration localConf)
	{
		String schema = getReportingInstance().getDbmsSchemaName();

		int maxValue = 100;
		_CmOsDiskSpace_FsUsedPct     = createTsLineChart(conn, schema, "CmOsDiskSpace", "FsUsedPct",     maxValue, false, null, "df: Space Used in Percent, at MountPoint (Host Monitor->OS Disk Space Usage(df))");
		_CmOsDiskSpace_FsAvailableMb = createTsLineChart(conn, schema, "CmOsDiskSpace", "FsAvailableMb", -1,       false, null, "df: Space Available in MB, at MountPoint (Host Monitor->OS Disk Space Usage(df))");
		_CmOsDiskSpace_FsUsedMb      = createTsLineChart(conn, schema, "CmOsDiskSpace", "FsUsedMb",      -1,       false, null, "df: Space Used in MB, at MountPoint (Host Monitor->OS Disk Space Usage(df))");
	}

	private IReportChart _CmOsDiskSpace_FsUsedPct;
	private IReportChart _CmOsDiskSpace_FsAvailableMb;
	private IReportChart _CmOsDiskSpace_FsUsedMb;
}
