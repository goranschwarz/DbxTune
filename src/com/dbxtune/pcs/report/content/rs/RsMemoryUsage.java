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
package com.dbxtune.pcs.report.content.rs;

import java.io.IOException;
import java.io.Writer;

import com.dbxtune.cm.rs.CmRsMemory;
import com.dbxtune.pcs.report.DailySummaryReportAbstract;
import com.dbxtune.pcs.report.content.IReportChart;
import com.dbxtune.pcs.report.content.ase.AseAbstract;
import com.dbxtune.sql.conn.DbxConnection;
import com.dbxtune.utils.Configuration;

public class RsMemoryUsage extends AseAbstract
{
//	private static final Logger _logger = LogManager.getLogger(MethodHandles.lookup().lookupClass());

	public RsMemoryUsage(DailySummaryReportAbstract reportingInstance)
	{
		super(reportingInstance);
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

	@Override
	public void writeMessageText(Writer sb, MessageType messageType)
	throws IOException
	{
		sb.append(getDbxCentralLinkWithDescForGraphs(false, "Below are Memory Usage for Replication Server.",
				"CmRsMemory_MemoryPct",
				"CmRsMemory_ModuleUsage"
				));

		_CmRsMemory_MemoryPct  .writeHtmlContent(sb, null, null);
		_CmRsMemory_ModuleUsage.writeHtmlContent(sb, null, null);
	}

	@Override
	public String getSubject()
	{
		return "Memory Usage (origin: " + CmRsMemory.CM_NAME + ")";
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
		
		_CmRsMemory_MemoryPct   = createTsLineChart(conn, schema, CmRsMemory.CM_NAME, CmRsMemory.GRAPH_NAME_MEMORY_PCT  , 100, true, null, "Stat: RS Total Memory Usage in Percent");
		_CmRsMemory_ModuleUsage = createTsLineChart(conn, schema, CmRsMemory.CM_NAME, CmRsMemory.GRAPH_NAME_MODULE_USAGE,  -1, true, null, "Stat: Memory Usage in MB per Module");
	}

	private IReportChart _CmRsMemory_MemoryPct;
	private IReportChart _CmRsMemory_ModuleUsage;
}
