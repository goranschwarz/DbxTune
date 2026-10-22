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

import com.dbxtune.cm.mysql.CmSummary;
import com.dbxtune.pcs.report.DailySummaryReportAbstract;
import com.dbxtune.pcs.report.content.IReportChart;
import com.dbxtune.sql.conn.DbxConnection;
import com.dbxtune.utils.Configuration;

public class MySqlLoadOverview
extends MySqlAbstract
{
	public MySqlLoadOverview(DailySummaryReportAbstract reportingInstance)
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
	public void writeMessageText(Writer w, MessageType messageType)
	throws IOException
	{
		w.append(getDbxCentralLinkWithDescForGraphs(false, "Below are Graphs/Charts that shows how much work the server did during the day, and if the connections had to wait for locks.",
				"CmSummary_StatementsGraph",
				"CmSummary_ConnectionsGraph",
				"CmSummary_BlockingLocksGraph",
				"CmSummary_TransGraph",
				"CmSummary_InnoDbRowsGraph",
				"CmSummary_ReadsGraph",
				"CmSummary_BpHitPctGraph",
				"CmSummary_RedoGraph",
				"CmSummary_SqlProblemsGraph",
				"CmSummary_OldestTranInSecGraph",
				"CmSummary_HistoryListGraph",
				"CmSummary_ConnProblemsGraph"
				));

		_CmSummary_StatementsGraph    .writeHtmlContent(w, null, null);
		_CmSummary_ConnectionsGraph   .writeHtmlContent(w, null, "<b>Note</b>: 'Threads running' is the load of the server: the connections that are executing something right now. Compare it with the number of CPU cores on the host. "
				+ "It also counts the server's own 'Daemon' threads (the event scheduler...), so it can be higher than 'Threads connected'.");
		_CmSummary_BlockingLocksGraph .writeHtmlContent(w, null, null, "No connection waited for a lock");

		// The minimal message (a mail that otherwise gets too big) stops here
		if (isMinimalMessageType())
			return;

		_CmSummary_TransGraph         .writeHtmlContent(w, null, null);
		_CmSummary_InnoDbRowsGraph    .writeHtmlContent(w, null, null);
		_CmSummary_ReadsGraph         .writeHtmlContent(w, null, null);
		_CmSummary_BpHitPctGraph      .writeHtmlContent(w, null, "<b>Note</b>: Below about 99% means that many reads had to go to disk: the buffer pool (innodb_buffer_pool_size) may be too small for the data that is used.");
		_CmSummary_RedoGraph          .writeHtmlContent(w, null, null);
		_CmSummary_SqlProblemsGraph   .writeHtmlContent(w, null, null);
		_CmSummary_OldestTranInSecGraph.writeHtmlContent(w, null, null);

		if (isFullMessageType())
		{
			_CmSummary_HistoryListGraph .writeHtmlContent(w, null, "<b>Note</b>: A History List Length that keeps growing means that purge can't remove old row versions, normally because a transaction has been open for a long time.");
			_CmSummary_ConnProblemsGraph.writeHtmlContent(w, null, null);
		}
	}

	@Override
	public String getSubject()
	{
		return "Load Overview of the full day (origin: CmSummary / performance_schema.global_status, INNODB_METRICS, INNODB_TRX)";
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

		_CmSummary_StatementsGraph      = createTsLineChart(conn, schema, CmSummary.CM_NAME, CmSummary.GRAPH_NAME_STATEMENTS,         -1,  false, null, "Statements per Second: all, and SELECT/INSERT/UPDATE/DELETE (Summary)");
		_CmSummary_ConnectionsGraph     = createTsLineChart(conn, schema, CmSummary.CM_NAME, CmSummary.GRAPH_NAME_CONNECTION,         -1,  false, null, "Connections and Running Threads (Summary)");
		_CmSummary_BlockingLocksGraph   = createTsLineChart(conn, schema, CmSummary.CM_NAME, CmSummary.GRAPH_NAME_BLOCKING_LOCKS,     -1,  false, null, "Number of Sessions that Waits for a Lock: row locks and metadata locks (Summary)");
		_CmSummary_TransGraph           = createTsLineChart(conn, schema, CmSummary.CM_NAME, CmSummary.GRAPH_NAME_TRANSACTION,        -1,  false, null, "Commits and Rollbacks per Second, also autocommit (Summary)");
		_CmSummary_InnoDbRowsGraph      = createTsLineChart(conn, schema, CmSummary.CM_NAME, CmSummary.GRAPH_NAME_INNODB_ROWS,        -1,  false, null, "InnoDB Rows Read/Inserted/Updated/Deleted per Second (Summary)");
		_CmSummary_ReadsGraph           = createTsLineChart(conn, schema, CmSummary.CM_NAME, CmSummary.GRAPH_NAME_READS,              -1,  false, null, "Buffer Pool: Logical Reads, Physical Reads (from disk) and Pages Flushed (to disk) per Second (Summary)");
		_CmSummary_BpHitPctGraph        = createTsLineChart(conn, schema, CmSummary.CM_NAME, CmSummary.GRAPH_NAME_BP_HIT_PCT,         100, false, null, "Buffer Pool Hit Ratio in Percent, logical reads that did not need a disk read (Summary)");
		_CmSummary_RedoGraph            = createTsLineChart(conn, schema, CmSummary.CM_NAME, CmSummary.GRAPH_NAME_REDO,               -1,  false, null, "Redo Log Written in KB per Second (Summary)");
		_CmSummary_SqlProblemsGraph     = createTsLineChart(conn, schema, CmSummary.CM_NAME, CmSummary.GRAPH_NAME_SQL_PROBLEMS,       -1,  false, null, "Expensive SQL Signs per Second: slow queries, temp tables on disk, joins without index, sort merge passes (Summary)");
		_CmSummary_OldestTranInSecGraph = createTsLineChart(conn, schema, CmSummary.CM_NAME, CmSummary.GRAPH_NAME_OLDEST_TRAN_IN_SEC, -1,  false, null, "Oldest Open Transaction, in Seconds (Summary)");
		_CmSummary_HistoryListGraph     = createTsLineChart(conn, schema, CmSummary.CM_NAME, CmSummary.GRAPH_NAME_HISTORY_LIST,       -1,  false, null, "InnoDB History List Length: old row versions that purge has not removed yet (Summary)");
		_CmSummary_ConnProblemsGraph    = createTsLineChart(conn, schema, CmSummary.CM_NAME, CmSummary.GRAPH_NAME_CONN_PROBLEMS,      -1,  false, null, "Failed and Aborted Connections per Second (Summary)");
	}

	private IReportChart _CmSummary_StatementsGraph;
	private IReportChart _CmSummary_ConnectionsGraph;
	private IReportChart _CmSummary_BlockingLocksGraph;
	private IReportChart _CmSummary_TransGraph;
	private IReportChart _CmSummary_InnoDbRowsGraph;
	private IReportChart _CmSummary_ReadsGraph;
	private IReportChart _CmSummary_BpHitPctGraph;
	private IReportChart _CmSummary_RedoGraph;
	private IReportChart _CmSummary_SqlProblemsGraph;
	private IReportChart _CmSummary_OldestTranInSecGraph;
	private IReportChart _CmSummary_HistoryListGraph;
	private IReportChart _CmSummary_ConnProblemsGraph;
}
