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

import com.dbxtune.cm.rs.CmAdminWhoDsi;
import com.dbxtune.cm.rs.CmAdminWhoSqm;
import com.dbxtune.cm.rs.CmAdminWhoSqt;
import com.dbxtune.pcs.report.DailySummaryReportAbstract;
import com.dbxtune.pcs.report.content.IReportChart;
import com.dbxtune.pcs.report.content.ase.AseAbstract;
import com.dbxtune.sql.conn.DbxConnection;
import com.dbxtune.utils.Configuration;

public class RsSqmSqtDsiInfo extends AseAbstract
{
//	private static final Logger _logger = LogManager.getLogger(MethodHandles.lookup().lookupClass());

	public RsSqmSqtDsiInfo(DailySummaryReportAbstract reportingInstance)
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
		sb.append(getDbxCentralLinkWithDescForGraphs(false, "Below are Information about SQM, SQT and DSI Modules.",
				"CmAdminWhoSqm_SqmWrites",
				"CmAdminWhoSqt_SqtClosed",
				"CmAdminWhoSqt_SqtFirstTranCmds",
				"CmAdminWhoDsi_DsiXactSucceeded",
				"CmAdminWhoDsi_DsiCmdRead"
				));

		_CmAdminWhoSqm_SqmWrites       .writeHtmlContent(sb, null, null);

		_CmAdminWhoSqt_SqtClosed       .writeHtmlContent(sb, null, null);
		_CmAdminWhoSqt_SqtFirstTranCmds.writeHtmlContent(sb, null, null);

		_CmAdminWhoDsi_DsiXactSucceeded.writeHtmlContent(sb, null, null);
		_CmAdminWhoDsi_DsiCmdRead      .writeHtmlContent(sb, null, null);
	}

	@Override
	public String getSubject()
	{
		return "SQM/SQT/DSI Information (origin: CmAdminWhoSqm, CmAdminWhoSqt)";
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

		_CmAdminWhoSqm_SqmWrites        = createTsLineChart(conn, schema, CmAdminWhoSqm.CM_NAME, CmAdminWhoSqm.GRAPH_NAME_WRITES         , -1, true, null, "SQM: Number of messages written into the queue (col 'Writes', per second)");

		_CmAdminWhoSqt_SqtClosed        = createTsLineChart(conn, schema, CmAdminWhoSqt.CM_NAME, CmAdminWhoSqt.GRAPH_NAME_CLOSED         , -1, true, null, "SQT: Number of committed transactions in the SQT cache (col 'Closed', absolute)");
		_CmAdminWhoSqt_SqtFirstTranCmds = createTsLineChart(conn, schema, CmAdminWhoSqt.CM_NAME, CmAdminWhoSqt.GRAPH_NAME_FIRST_TRAN_CMDS, -1, true, null, "SQT: Number of Commands in First Tran (col 'First Trans', 'st:C,cmds:###')");

		_CmAdminWhoDsi_DsiXactSucceeded = createTsLineChart(conn, schema, CmAdminWhoDsi.CM_NAME, CmAdminWhoDsi.GRAPH_NAME_XACT_SUCCEEDED , -1, true, null, "DSI: Number of Transactions Succeeded (col 'Xacts_succeeded', per second)");
		_CmAdminWhoDsi_DsiCmdRead       = createTsLineChart(conn, schema, CmAdminWhoDsi.CM_NAME, CmAdminWhoDsi.GRAPH_NAME_CMD_READ       , -1, true, null, "DSI: Number of Commands Read (col 'Cmds_read', per second)");
	}

	private IReportChart _CmAdminWhoSqm_SqmWrites;

	private IReportChart _CmAdminWhoSqt_SqtClosed;
	private IReportChart _CmAdminWhoSqt_SqtFirstTranCmds;

	private IReportChart _CmAdminWhoDsi_DsiXactSucceeded;
	private IReportChart _CmAdminWhoDsi_DsiCmdRead;
}
