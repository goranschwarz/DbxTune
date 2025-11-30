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
package com.dbxtune.pcs.report.content.os;

import java.io.IOException;
import java.io.Writer;

import com.dbxtune.cm.os.CmOsNwInfo;
import com.dbxtune.pcs.report.DailySummaryReportAbstract;
import com.dbxtune.pcs.report.content.IReportChart;
import com.dbxtune.sql.conn.DbxConnection;
import com.dbxtune.utils.Configuration;

public class CmOsNwInfoOverview
extends OsAbstract
{
	public CmOsNwInfoOverview(DailySummaryReportAbstract reportingInstance)
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
		return false;
	}

	@Override
	public String getSubject()
	{
		return "OS Network Usage graph of the full day (origin: CmOsNwInfo_rate / os-cmd: linux=/proc/net/dev, Windows=typeperf \\Network Interface(*)\\*)";
	}

	@Override
	public boolean hasIssueToReport()
	{
		return false; // even if we found entries, do NOT indicate this as a Problem or Issue
	}

	@Override
	public void writeMessageText(Writer sb, MessageType messageType)
	throws IOException
	{
		sb.append(getDbxCentralLinkWithDescForGraphs(false, "Below are Network Graphs/Charts.",
				"CmOsNwInfo_AllMbit",
				"CmOsNwInfo_RecvMbit",
				"CmOsNwInfo_TransMbit"
				));

		_CmOsNwInfo_AllMbit  .writeHtmlContent(sb, null, null);
		_CmOsNwInfo_RecvMbit .writeHtmlContent(sb, null, null);
		_CmOsNwInfo_TransMbit.writeHtmlContent(sb, null, null);
	}

	@Override
	public void create(DbxConnection conn, String srvName, Configuration pcsSavedConf, Configuration localConf)
	{
		String schema = getReportingInstance().getDbmsSchemaName();
		
		_CmOsNwInfo_AllMbit   = createTsLineChart(conn, schema, CmOsNwInfo.CM_NAME, CmOsNwInfo.GRAPH_NAME_ALL_BANDWIDTH_MBIT,   -1, false, null, "Network Received/Transmitted all NIC in Mbit per Sec (Host Monitor->Network Stat)");
		_CmOsNwInfo_RecvMbit  = createTsLineChart(conn, schema, CmOsNwInfo.CM_NAME, CmOsNwInfo.GRAPH_NAME_RECV_BANDWIDTH_MBIT,  -1, false, null, "Network Received Mbit per Sec (Host Monitor->Network Stat)");
		_CmOsNwInfo_TransMbit = createTsLineChart(conn, schema, CmOsNwInfo.CM_NAME, CmOsNwInfo.GRAPH_NAME_TRANS_BANDWIDTH_MBIT, -1, false, null, "Network Transmitted in Mbit per Sec (Host Monitor->Network Stat)");
	}

	private IReportChart _CmOsNwInfo_AllMbit;
	private IReportChart _CmOsNwInfo_RecvMbit;
	private IReportChart _CmOsNwInfo_TransMbit;
}
