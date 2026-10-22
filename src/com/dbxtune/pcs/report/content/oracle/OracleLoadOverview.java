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
package com.dbxtune.pcs.report.content.oracle;

import java.io.IOException;
import java.io.Writer;

import com.dbxtune.cm.oracle.CmSessions;
import com.dbxtune.cm.oracle.CmSummary;
import com.dbxtune.cm.oracle.CmSysMetric;
import com.dbxtune.pcs.report.DailySummaryReportAbstract;
import com.dbxtune.pcs.report.content.IReportChart;
import com.dbxtune.sql.conn.DbxConnection;
import com.dbxtune.utils.Configuration;

public class OracleLoadOverview
extends OracleAbstract
{
	public OracleLoadOverview(DailySummaryReportAbstract reportingInstance)
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
		w.append(getDbxCentralLinkWithDescForGraphs(false, "Below are Graphs/Charts that shows how much work the database did during the day, and if the sessions were working or waiting.",
				"CmSummary_AasGraph",
				"CmSessions_ActiveByWaitClass",
				"CmSummary_HostCpuGraph",
				"CmSummary_CallsGraph",
				"CmSummary_TransGraph",
				"CmSummary_ReadsGraph",
				"CmSummary_RedoGraph",
				"CmSysMetric_MetricResponseMs",
				"CmSummary_BlockingLocksGraph",
				"CmSummary_OldestTranInSecGraph",
				"CmSummary_ConnectionsGraph",
				"CmSummary_ParseGraph"
				));

		_CmSummary_AasGraph             .writeHtmlContent(w, null, "<b>Note</b>: 'Average Active Sessions' is the load of the database: 1 means that on average one session was working (on CPU or waiting for something). Compare it with the number of CPU cores on the host.");
		_CmSessions_ActiveByWaitClass   .writeHtmlContent(w, null, null);
		_CmSummary_HostCpuGraph         .writeHtmlContent(w, null, null);

		// The minimal message (a mail that otherwise gets too big) stops here
		if (isMinimalMessageType())
			return;

		_CmSummary_CallsGraph           .writeHtmlContent(w, null, null);
		_CmSummary_TransGraph           .writeHtmlContent(w, null, null);
		_CmSummary_ReadsGraph           .writeHtmlContent(w, null, null);
		_CmSummary_RedoGraph            .writeHtmlContent(w, null, null);
		if (_CmSysMetric_MetricResponseMs != null)
			_CmSysMetric_MetricResponseMs.writeHtmlContent(w, null, null);
		_CmSummary_BlockingLocksGraph   .writeHtmlContent(w, null, null, "No Blocking Locks was detected");
		_CmSummary_OldestTranInSecGraph .writeHtmlContent(w, null, null);

		if (isFullMessageType())
		{
			_CmSummary_ConnectionsGraph .writeHtmlContent(w, null, null);
			_CmSummary_ParseGraph       .writeHtmlContent(w, null, null);
		}
	}

	@Override
	public String getSubject()
	{
		return "Load Overview of the full day (origin: CmSummary, CmSessions, CmSysMetric / V$SYSSTAT, V$SESSION, V$SYSMETRIC)";
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

		_CmSummary_AasGraph              = createTsLineChart(conn, schema, CmSummary  .CM_NAME, CmSummary  .GRAPH_NAME_AAS,                  -1,  false, null, "Average Active Sessions: how many sessions are working (on CPU or waiting) (Summary)");
		_CmSessions_ActiveByWaitClass    = createTsLineChart(conn, schema, CmSessions .CM_NAME, CmSessions .GRAPH_NAME_ACTIVE_BY_WAIT_CLASS, -1,  true,  null, "Active User Sessions at Sample Time: on CPU, or Waiting per Wait Class (" + CmSessions.SHORT_NAME + ")");
		_CmSummary_HostCpuGraph          = createTsLineChart(conn, schema, CmSummary  .CM_NAME, CmSummary  .GRAPH_NAME_HOST_CPU,             100, false, null, "Host CPU Usage in Percent, for ALL processes on the host (Summary)");
		_CmSummary_CallsGraph            = createTsLineChart(conn, schema, CmSummary  .CM_NAME, CmSummary  .GRAPH_NAME_CALLS,                -1,  false, null, "User Calls and SQL Executions per Second (Summary)");
		_CmSummary_TransGraph            = createTsLineChart(conn, schema, CmSummary  .CM_NAME, CmSummary  .GRAPH_NAME_TRANSACTION,          -1,  false, null, "Transaction per Second (Summary)");
		_CmSummary_ReadsGraph            = createTsLineChart(conn, schema, CmSummary  .CM_NAME, CmSummary  .GRAPH_NAME_READS,                -1,  false, null, "Logical and Physical IO, in Blocks per Second (Summary)");
		_CmSummary_RedoGraph             = createTsLineChart(conn, schema, CmSummary  .CM_NAME, CmSummary  .GRAPH_NAME_REDO,                 -1,  false, null, "Redo Generated in KB per Second (Summary)");
		_CmSummary_BlockingLocksGraph    = createTsLineChart(conn, schema, CmSummary  .CM_NAME, CmSummary  .GRAPH_NAME_BLOCKING_LOCKS,       -1,  false, null, "Number of Concurrently Blocking Locks (Summary)");
		_CmSummary_OldestTranInSecGraph  = createTsLineChart(conn, schema, CmSummary  .CM_NAME, CmSummary  .GRAPH_NAME_OLDEST_TRAN_IN_SEC,   -1,  false, null, "Oldest Open Transaction, in Seconds (Summary)");
		_CmSummary_ConnectionsGraph      = createTsLineChart(conn, schema, CmSummary  .CM_NAME, CmSummary  .GRAPH_NAME_CONNECTION,           -1,  false, null, "Connections/Users connected to Oracle (Summary)");
		_CmSummary_ParseGraph            = createTsLineChart(conn, schema, CmSummary  .CM_NAME, CmSummary  .GRAPH_NAME_PARSE,                -1,  false, null, "SQL Parses per Second (Summary)");

		// Oracle's own metrics: V$SYSMETRIC is empty when the collector is connected to a PDB, and then this graph was never recorded
		if (doTableExist(conn, schema, CmSysMetric.CM_NAME + "_" + CmSysMetric.GRAPH_NAME_RESPONSE_MS))
			_CmSysMetric_MetricResponseMs = createTsLineChart(conn, schema, CmSysMetric.CM_NAME, CmSysMetric.GRAPH_NAME_RESPONSE_MS,         -1,  false, null, "SQL Response Time per Call, and Single Block Read Time, in Milliseconds (" + CmSysMetric.SHORT_NAME + ")");
	}

	private IReportChart _CmSummary_AasGraph;
	private IReportChart _CmSessions_ActiveByWaitClass;
	private IReportChart _CmSummary_HostCpuGraph;
	private IReportChart _CmSummary_CallsGraph;
	private IReportChart _CmSummary_TransGraph;
	private IReportChart _CmSummary_ReadsGraph;
	private IReportChart _CmSummary_RedoGraph;
	private IReportChart _CmSysMetric_MetricResponseMs; // null = not recorded
	private IReportChart _CmSummary_BlockingLocksGraph;
	private IReportChart _CmSummary_OldestTranInSecGraph;
	private IReportChart _CmSummary_ConnectionsGraph;
	private IReportChart _CmSummary_ParseGraph;
}
