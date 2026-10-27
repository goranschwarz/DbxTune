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
package com.dbxtune.pcs.report.content.db2;

import java.io.IOException;
import java.io.Writer;

import com.dbxtune.cm.db2.CmSummary;
import com.dbxtune.cm.db2.CmSystemResourses;
import com.dbxtune.pcs.report.DailySummaryReportAbstract;
import com.dbxtune.pcs.report.content.IReportChart;
import com.dbxtune.sql.conn.DbxConnection;
import com.dbxtune.utils.Configuration;

/**
 * How much work the database did during the day, and if the connections had to wait (the CmSummary graphs). Built on MySqlLoadOverview / OracleLoadOverview.
 */
public class Db2LoadOverview
extends Db2Abstract
{
	public Db2LoadOverview(DailySummaryReportAbstract reportingInstance)
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
		w.append(getDbxCentralLinkWithDescForGraphs(false, "Below are Graphs/Charts that shows how much work the database did during the day, and if the connections had to wait for locks.",
				"CmSummary_StatementsGraph",
				"CmSummary_ConnectionsGraph",
				"CmSummary_BlockingLocksGraph",
				"CmSummary_CpuGraph",
				"CmSummary_WaitPctGraph",
				"CmSummary_TransGraph",
				"CmSummary_RowsGraph",
				"CmSummary_ReadsGraph",
				"CmSummary_BpHitPctGraph",
				"CmSummary_LocksGraph",
				"CmSummary_SortsGraph",
				"CmSummary_OldestTranInSecGraph",
				"CmSummary_LogUsedPctGraph",
				"CmSystemResourses_CpuPct"
				));

		_CmSummary_StatementsGraph    .writeHtmlContent(w, null, null);
		_CmSummary_ConnectionsGraph   .writeHtmlContent(w, null, null);
		_CmSummary_BlockingLocksGraph .writeHtmlContent(w, null, null, "No connection waited for a lock");

		// The minimal message (a mail that otherwise gets too big) stops here
		if (isMinimalMessageType())
			return;

		_CmSummary_CpuGraph           .writeHtmlContent(w, null, null);
		_CmSummary_WaitPctGraph       .writeHtmlContent(w, null, "<b>Note</b>: How much of the time the database worked on requests was spent waiting (for locks, disk, the log, the network...). The section 'Time Spent' says for what.");
		_CmSummary_TransGraph         .writeHtmlContent(w, null, null);
		_CmSummary_RowsGraph          .writeHtmlContent(w, null, "<b>Note</b>: Many more rows read than returned can mean scans where an index would do (see 'Top SQL', ROWS_READ_PER_EXEC).");
		_CmSummary_ReadsGraph         .writeHtmlContent(w, null, null);
		_CmSummary_BpHitPctGraph      .writeHtmlContent(w, null, "<b>Note</b>: Below about 95-99% means that many reads had to go to disk: a buffer pool may be too small (see the 'BufferPool' tab).");
		_CmSummary_LocksGraph         .writeHtmlContent(w, null, null);
		_CmSummary_OldestTranInSecGraph.writeHtmlContent(w, null, null);
		_CmSummary_LogUsedPctGraph    .writeHtmlContent(w, null, "<b>Note</b>: At 100% all changes fail with SQL0964C (the transaction log is full). See the section 'Transaction Log'.");

		if (isFullMessageType())
		{
			_CmSummary_SortsGraph        .writeHtmlContent(w, null, "<b>Note</b>: Sort overflows are sorts that did not fit in SORTHEAP (they were spilled to the temporary table space).");
			_CmSystemResourses_CpuPct    .writeHtmlContent(w, null, "<b>Note</b>: The CPU of the whole host (ENV_GET_SYSTEM_RESOURCES), not only Db2.");
		}
	}

	@Override
	public String getSubject()
	{
		return "Load Overview of the full day (origin: CmSummary / MON_GET_DATABASE, MON_GET_TRANSACTION_LOG, MON_GET_APPL_LOCKWAIT)";
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

		_CmSummary_StatementsGraph      = createTsLineChart(conn, schema, CmSummary.CM_NAME, CmSummary.GRAPH_NAME_STATEMENTS,         -1,  false, null, "Statements per Second (Summary)");
		_CmSummary_ConnectionsGraph     = createTsLineChart(conn, schema, CmSummary.CM_NAME, CmSummary.GRAPH_NAME_CONNECTION,         -1,  false, null, "Connections, and Connections Working in Db2 (Summary)");
		_CmSummary_BlockingLocksGraph   = createTsLineChart(conn, schema, CmSummary.CM_NAME, CmSummary.GRAPH_NAME_BLOCKING_LOCKS,     -1,  false, null, "Number of Connections that Waits for a Lock (Summary)");
		_CmSummary_CpuGraph             = createTsLineChart(conn, schema, CmSummary.CM_NAME, CmSummary.GRAPH_NAME_CPU,                -1,  false, null, "Db2 CPU Time, ms per Second (Summary)");
		_CmSummary_WaitPctGraph         = createTsLineChart(conn, schema, CmSummary.CM_NAME, CmSummary.GRAPH_NAME_WAIT_PCT,           100, false, null, "Wait Time in Percent of the Request Time (Summary)");
		_CmSummary_TransGraph           = createTsLineChart(conn, schema, CmSummary.CM_NAME, CmSummary.GRAPH_NAME_TRANSACTION,        -1,  false, null, "Commits and Rollbacks per Second (Summary)");
		_CmSummary_RowsGraph            = createTsLineChart(conn, schema, CmSummary.CM_NAME, CmSummary.GRAPH_NAME_ROWS,               -1,  false, null, "Rows Read, Returned and Modified per Second (Summary)");
		_CmSummary_ReadsGraph           = createTsLineChart(conn, schema, CmSummary.CM_NAME, CmSummary.GRAPH_NAME_READS,              -1,  false, null, "Buffer Pool Logical and Physical Reads per Second (Summary)");
		_CmSummary_BpHitPctGraph        = createTsLineChart(conn, schema, CmSummary.CM_NAME, CmSummary.GRAPH_NAME_BP_HIT_PCT,         100, false, null, "Buffer Pool Hit Ratio in Percent (Summary)");
		_CmSummary_LocksGraph           = createTsLineChart(conn, schema, CmSummary.CM_NAME, CmSummary.GRAPH_NAME_LOCKS,              -1,  false, null, "Lock Waits, Timeouts, Deadlocks and Escalations per Second (Summary)");
		_CmSummary_SortsGraph           = createTsLineChart(conn, schema, CmSummary.CM_NAME, CmSummary.GRAPH_NAME_SORTS,              -1,  false, null, "Sorts and Sort Overflows per Second (Summary)");
		_CmSummary_OldestTranInSecGraph = createTsLineChart(conn, schema, CmSummary.CM_NAME, CmSummary.GRAPH_NAME_OLDEST_TRAN_IN_SEC, -1,  false, null, "Oldest Open Transaction, in Seconds (Summary)");
		_CmSummary_LogUsedPctGraph      = createTsLineChart(conn, schema, CmSummary.CM_NAME, CmSummary.GRAPH_NAME_LOG_USED_PCT,       100, false, null, "Transaction Log Used in Percent (Summary)");
		_CmSystemResourses_CpuPct       = createTsLineChart(conn, schema, CmSystemResourses.CM_NAME, CmSystemResourses.GRAPH_NAME_CPU, 100, false, null, "Host CPU Usage in Percent (System Resources)");
	}

	private IReportChart _CmSummary_StatementsGraph;
	private IReportChart _CmSummary_ConnectionsGraph;
	private IReportChart _CmSummary_BlockingLocksGraph;
	private IReportChart _CmSummary_CpuGraph;
	private IReportChart _CmSummary_WaitPctGraph;
	private IReportChart _CmSummary_TransGraph;
	private IReportChart _CmSummary_RowsGraph;
	private IReportChart _CmSummary_ReadsGraph;
	private IReportChart _CmSummary_BpHitPctGraph;
	private IReportChart _CmSummary_LocksGraph;
	private IReportChart _CmSummary_SortsGraph;
	private IReportChart _CmSummary_OldestTranInSecGraph;
	private IReportChart _CmSummary_LogUsedPctGraph;
	private IReportChart _CmSystemResourses_CpuPct;
}
