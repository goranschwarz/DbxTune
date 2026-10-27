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

import com.dbxtune.cm.mysql.CmReplication;
import com.dbxtune.gui.ResultSetTableModel;
import com.dbxtune.pcs.report.DailySummaryReportAbstract;
import com.dbxtune.pcs.report.content.IReportChart;
import com.dbxtune.sql.conn.DbxConnection;
import com.dbxtune.utils.Configuration;

/**
 * Replication: how far behind this replica was, and if the replication stopped (from CmReplication). Built on OracleDataGuard.
 */
public class MySqlReplication
extends MySqlAbstract
{
	private ResultSetTableModel _shortRstm; // null = replication was not recorded

	public MySqlReplication(DailySummaryReportAbstract reportingInstance)
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
		if (_shortRstm == null || _shortRstm.getRowCount() == 0)
		{
			w.append("<b>Replication was not recorded.</b><br>\n");
			w.append("This server is not a replica (it has no replication channel). If it is a source, the replicas that were connected are in the DBMS Configuration, tab 'Replication'.<br>\n");
			return;
		}

		// Get a description of this section, and column names
		w.append(getSectionDescriptionHtml(_shortRstm, true));

		w.append("Row Count: " + _shortRstm.getRowCount() + "<br>\n");
		w.append(toHtmlTable(_shortRstm));

		if (_CmReplication_LagSec != null)
		{
			w.append(getDbxCentralLinkWithDescForGraphs(false, "Below is how far behind the replica was during the day.",
					"CmReplication_ReplLagSec"
					));

			_CmReplication_LagSec.writeHtmlContent(w, null, null);
		}
	}

	@Override
	public String getSubject()
	{
		return "Replication: Replica Lag and Stops (origin: CmReplication / performance_schema.replication_*)";
	}

	@Override
	public boolean hasIssueToReport()
	{
		return false; // even if we found entries, do NOT indicate this as a Problem or Issue (the alarms does that)
	}

	@Override
	public void create(DbxConnection conn, String srvName, Configuration pcsSavedConf, Configuration localConf)
	{
		String schema = getReportingInstance().getDbmsSchemaName();

		// No mandatory table: a server that is not a replica is the normal case (and something the reader should be told), not a red ERROR
		if ( ! doTableExist(conn, schema, "CmReplication_abs") )
			return;

		// The channels at the LAST sample, with the worst values of the period
		String sql = ""
			    + "select \n"
			    + "     l.[ChannelName] \n"
			    + "    ,l.[SourceHost] \n"
			    + "    ,l.[SourcePort] \n"
			    + "    ,l.[IoThread] \n"
			    + "    ,l.[SqlThread] \n"
			    + "    ,l.[ApplyLagSec] \n"
			    + "    ,m.[ApplyLagSec__max] \n"
			    + "    ,m.[samples_io_not_on] \n"
			    + "    ,m.[samples_sql_not_on] \n"
			    + "    ,m.[samples_count] \n"
			    + "    ,l.[DesiredDelaySec] \n"
			    + "    ,l.[Workers] \n"
			    + "    ,l.[TransactionRetries] \n"
			    + "    ,l.[IoErrorNumber] \n"
			    + "    ,l.[IoErrorMessage] \n"
			    + "    ,l.[IoErrorTime] \n"
			    + "    ,l.[SqlErrorNumber] \n"
			    + "    ,l.[SqlErrorMessage] \n"
			    + "    ,l.[SqlErrorTime] \n"
			    + "    ,l.[AutoPosition] \n"
			    + "from [CmReplication_abs] l \n"
			    + "join (select \n"
			    + "           [ChannelName] \n"
			    + "          ,max([ApplyLagSec])                                       as [ApplyLagSec__max] \n"
			    + "          ,sum(case when [IoThread]  <> 'ON' then 1 else 0 end)     as [samples_io_not_on] \n"
			    + "          ,sum(case when [SqlThread] <> 'ON' then 1 else 0 end)     as [samples_sql_not_on] \n"
			    + "          ,count(*)                                                 as [samples_count] \n"
			    + "      from [CmReplication_abs] \n"
			    + "      where 1 = 1 " + getReportPeriodSqlWhere().trim() + " \n"
			    + "      group by [ChannelName] \n"
			    + "     ) m on m.[ChannelName] = l.[ChannelName] \n"
			    + "where l.[SessionSampleTime] = (select max([SessionSampleTime]) from [CmReplication_abs] where 1 = 1 " + getReportPeriodSqlWhere().trim() + ") \n"
			    + "order by l.[ChannelName] \n"
			    + "";

		_shortRstm = executeQuery(conn, sql, true, "MySqlReplication");

		_shortRstm.setDescription(
				"The replication channels of this replica at the last collector sample, and the worst values of the period (from the table <code>CmReplication_abs</code>).<br>" +
				"<code>IoThread</code> receives the transactions from the source, <code>SqlThread</code> applies them. Both should be ON.<br>" +
				"The last error (the IO... and Sql... columns) stays after it is fixed: compare its time with the period.<br>" +
				"");

		_shortRstm.setColumnDescription("ChannelName"       , "The replication channel ('(default)' when there is only one)");
		_shortRstm.setColumnDescription("SourceHost"        , "The source server the replica reads from");
		_shortRstm.setColumnDescription("SourcePort"        , "The port of the source server");
		_shortRstm.setColumnDescription("IoThread"          , "The receiver (IO thread) at the last sample: ON, OFF or CONNECTING");
		_shortRstm.setColumnDescription("SqlThread"         , "The applier (SQL thread) at the last sample: ON or OFF");
		_shortRstm.setColumnDescription("ApplyLagSec"       , "How far behind the replica was at the last sample, in seconds");
		_shortRstm.setColumnDescription("ApplyLagSec__max"  , "The highest lag during the period, in seconds");
		_shortRstm.setColumnDescription("samples_io_not_on" , "In how many collector samples the receiver (IO thread) was not ON");
		_shortRstm.setColumnDescription("samples_sql_not_on", "In how many collector samples the applier (SQL thread) was not ON");
		_shortRstm.setColumnDescription("samples_count"     , "Number of collector samples in the period");
		_shortRstm.setColumnDescription("DesiredDelaySec"   , "A delay on purpose (SOURCE_DELAY): the replica is kept this many seconds behind");
		_shortRstm.setColumnDescription("Workers"           , "Number of applier threads (replica_parallel_workers)");
		_shortRstm.setColumnDescription("TransactionRetries", "Transactions the applier had to retry (since the replication started)");
		_shortRstm.setColumnDescription("IoErrorNumber"     , "The last error of the receiver (0 = none)");
		_shortRstm.setColumnDescription("IoErrorMessage"    , "The text of the last error of the receiver");
		_shortRstm.setColumnDescription("IoErrorTime"       , "When the last error of the receiver happened");
		_shortRstm.setColumnDescription("SqlErrorNumber"    , "The last error of the applier (0 = none)");
		_shortRstm.setColumnDescription("SqlErrorMessage"   , "The text of the last error of the applier");
		_shortRstm.setColumnDescription("SqlErrorTime"      , "When the last error of the applier happened");
		_shortRstm.setColumnDescription("AutoPosition"      , "1 = GTID based (the replica knows by itself where to continue)");

		// The graph: only if it was recorded
		if (doTableExist(conn, schema, CmReplication.CM_NAME + "_" + CmReplication.GRAPH_NAME_APPLY_LAG))
			_CmReplication_LagSec = createTsLineChart(conn, schema, CmReplication.CM_NAME, CmReplication.GRAPH_NAME_APPLY_LAG, -1, true, null, "Replica Apply Lag in Seconds, per Channel (" + CmReplication.SHORT_NAME + ")");
	}

	private IReportChart _CmReplication_LagSec; // null = not recorded
}
