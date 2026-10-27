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
package com.dbxtune.pcs.report.content.mysql;

import java.io.IOException;
import java.io.Writer;

import com.dbxtune.cm.mysql.CmMemory;
import com.dbxtune.pcs.report.DailySummaryReportAbstract;
import com.dbxtune.pcs.report.content.IReportChart;
import com.dbxtune.sql.conn.DbxConnection;
import com.dbxtune.utils.Configuration;

/**
 * What the server used its memory for during the day (from CmMemory).
 */
public class MySqlMemory
extends MySqlAbstract
{
	public MySqlMemory(DailySummaryReportAbstract reportingInstance)
	{
		super(reportingInstance);
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

	@Override
	public void writeMessageText(Writer w, MessageType messageType)
	throws IOException
	{
		w.append(getDbxCentralLinkWithDescForGraphs(false, "Below is what the server used its memory for during the day.",
				"CmMemory_MemoryAreaMb",
				"CmMemory_MemoryTopEventsMb"
				));

		_CmMemory_AreaMb.writeHtmlContent(w, null, "<b>Note</b>: Only memory that performance_schema counts (the memory instruments): it is less than what the operating system says the process uses.");

		if (isFullMessageType())
			_CmMemory_TopEventsMb.writeHtmlContent(w, null, null);
	}

	@Override
	public String getSubject()
	{
		return "Memory Usage of the full day (origin: CmMemory / performance_schema.memory_summary_global_by_event_name)";
	}

	@Override
	public boolean hasIssueToReport()
	{
		return false; // even if we found entries, do NOT indicate this as a Problem or Issue
	}

	@Override
	public String[] getMandatoryTables()
	{
		return new String[] { CmMemory.CM_NAME + "_" + CmMemory.GRAPH_NAME_AREA_MB };
	}

	@Override
	public void create(DbxConnection conn, String srvName, Configuration pcsSavedConf, Configuration localConf)
	{
		String schema = getReportingInstance().getDbmsSchemaName();

		_CmMemory_AreaMb      = createTsLineChart(conn, schema, CmMemory.CM_NAME, CmMemory.GRAPH_NAME_AREA_MB,       -1, true, null, "Memory Used per Area (innodb, sql, performance_schema...), in MB (" + CmMemory.SHORT_NAME + ")");
		_CmMemory_TopEventsMb = createTsLineChart(conn, schema, CmMemory.CM_NAME, CmMemory.GRAPH_NAME_TOP_EVENTS_MB, -1, true, null, "Memory Used by the Top 15 Memory Instruments, in MB (" + CmMemory.SHORT_NAME + ")");
	}

	private IReportChart _CmMemory_AreaMb;
	private IReportChart _CmMemory_TopEventsMb;
}
