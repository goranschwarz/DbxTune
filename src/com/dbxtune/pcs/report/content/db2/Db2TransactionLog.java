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

import com.dbxtune.cm.db2.CmTransactionLog;
import com.dbxtune.gui.ResultSetTableModel;
import com.dbxtune.pcs.report.DailySummaryReportAbstract;
import com.dbxtune.pcs.report.content.IReportChart;
import com.dbxtune.sql.conn.DbxConnection;
import com.dbxtune.utils.Configuration;

/**
 * How full the transaction log was during the day, which transactions kept it from being reused, and if the archiving worked (from CmTransactionLog).
 */
public class Db2TransactionLog
extends Db2Abstract
{
	private ResultSetTableModel _lastRstm;
	private ResultSetTableModel _holdersRstm;

	public Db2TransactionLog(DailySummaryReportAbstract reportingInstance)
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
		w.append(getSectionDescriptionHtml(_lastRstm, true));
		w.append(toHtmlTable(_lastRstm));
		w.append("<br>\n");

		w.append(getDbxCentralLinkWithDescForGraphs(false, "Below is how full the transaction log was during the day, and how much was written to it.",
				"CmTransactionLog_LogUsedPct",
				"CmTransactionLog_LogWriteMb",
				"CmTransactionLog_LogWriteTime"
				));

		_CmTransactionLog_LogUsedPct.writeHtmlContent(w, null, "<b>Note</b>: At 100% all changes fail with SQL0964C (the transaction log is full).");

		// Who kept the log from being reused
		w.append("<br>\n");
		w.append(getSectionDescriptionHtml(_holdersRstm, true));
		if (_holdersRstm.getRowCount() == 0)
		{
			w.append("No transaction was open for more than " + getMinAgeSec() + " seconds.<br>\n");
		}
		else
		{
			w.append("Row Count: " + _holdersRstm.getRowCount() + "&emsp;&emsp; To change number of <i>top</i> records, set property <code>" + getTopRowsPropertyName() + "=##</code><br>\n");
			w.append(toHtmlTable(_holdersRstm));
		}

		if (isFullMessageType())
		{
			_CmTransactionLog_LogWriteMb  .writeHtmlContent(w, null, null);
			_CmTransactionLog_LogWriteTime.writeHtmlContent(w, null, "<b>Note</b>: A commit waits for its log write: a slow log disk makes every commit slow.");
		}
	}

	@Override
	public String getSubject()
	{
		return "Transaction Log: Usage, Long Transactions and Archiving (origin: CmTransactionLog / MON_GET_TRANSACTION_LOG)";
	}

	@Override
	public boolean hasIssueToReport()
	{
		return false; // even if we found entries, do NOT indicate this as a Problem or Issue (the alarms does that)
	}

	@Override
	public String[] getMandatoryTables()
	{
		return new String[] { "CmTransactionLog_abs" };
	}

	@Override
	public int getTopRowsDefault()
	{
		return 20;
	}

	/** Transactions younger than this are not listed */
	private int getMinAgeSec()
	{
		return Configuration.getCombinedConfiguration().getIntProperty(getClass().getSimpleName() + ".minAgeSec", 300);
	}

	@Override
	public void create(DbxConnection conn, String srvName, Configuration pcsSavedConf, Configuration localConf)
	{
		String schema = getReportingInstance().getDbmsSchemaName();
		String where  = "where 1 = 1 " + getReportPeriodSqlWhere().trim();

		// The log at the LAST sample, with the worst values of the period
		String sql = ""
			    + "select \n"
			    + "     l.[MEMBER] \n"
			    + "    ,l.[LOG_USED_PCT] \n"
			    + "    ,m.[LOG_USED_PCT__max] \n"
			    + "    ,(select min([SessionSampleTime]) from [CmTransactionLog_abs] x " + where + " and x.[MEMBER] = l.[MEMBER] and x.[LOG_USED_PCT] = m.[LOG_USED_PCT__max]) as [LOG_USED_PCT__max_time] \n"
			    + "    ,l.[LOG_TOTAL_MB] \n"
			    + "    ,l.[LOG_USED_TOP_MB] \n"
			    + "    ,m.[SEC_LOGS_ALLOCATED__max] \n"
			    + "    ,m.[OLDEST_XACT_AGE_SEC__max] \n"
			    + "    ,l.[LOGARCHMETH1] \n"
			    + "    ,l.[ARCHIVE_METHOD1_STATUS] \n"
			    + "    ,m.[samples_archive_failing] \n"
			    + "    ,m.[samples_count] \n"
			    + "    ,l.[METHOD1_FIRST_FAILURE] \n"
			    + "    ,l.[CURRENT_ACTIVE_LOG] \n"
			    + "    ,l.[CURRENT_ARCHIVE_LOG] \n"
			    + "    ,l.[NUM_INDOUBT_TRANS] \n"
			    + "from [CmTransactionLog_abs] l \n"
			    + "join (select \n"
			    + "           [MEMBER] \n"
			    + "          ,max([LOG_USED_PCT])                                       as [LOG_USED_PCT__max] \n"
			    + "          ,max([SEC_LOGS_ALLOCATED])                                 as [SEC_LOGS_ALLOCATED__max] \n"
			    + "          ,max([OLDEST_XACT_AGE_SEC])                                as [OLDEST_XACT_AGE_SEC__max] \n"
			    + "          ,sum(case when [LOGARCHMETH1] <> 'OFF' and [ARCHIVE_METHOD1_STATUS] = 0 then 1 else 0 end) as [samples_archive_failing] \n"
			    + "          ,count(*)                                                  as [samples_count] \n"
			    + "      from [CmTransactionLog_abs] \n"
			    + "      " + where + " \n"
			    + "      group by [MEMBER] \n"
			    + "     ) m on m.[MEMBER] = l.[MEMBER] \n"
			    + "where l.[SessionSampleTime] = (select max([SessionSampleTime]) from [CmTransactionLog_abs] " + where + ") \n"
			    + "order by l.[MEMBER] \n"
			    + "";

		_lastRstm = executeQuery(conn, sql, true, "Db2TransactionLog");

		_lastRstm.setDescription(
				"The transaction log at the last collector sample, and the worst values of the period (from the table <code>CmTransactionLog_abs</code>).<br>" +
				"Look for: a high <code>LOG_USED_PCT__max</code> (at 100% all changes fail), and <code>samples_archive_failing</code> &gt; 0 (the archived logs are needed to restore the database).<br>" +
				"");

		_lastRstm.setColumnDescription("MEMBER"                  , "The Db2 member (0 when not pureScale)");
		_lastRstm.setColumnDescription("LOG_USED_PCT"            , "Log used in percent, at the last sample");
		_lastRstm.setColumnDescription("LOG_USED_PCT__max"       , "The fullest the log was during the period, in percent");
		_lastRstm.setColumnDescription("LOG_USED_PCT__max_time"  , "When the log was the fullest");
		_lastRstm.setColumnDescription("LOG_TOTAL_MB"            , "The size of the log (primary + secondary that can be allocated), in MB");
		_lastRstm.setColumnDescription("LOG_USED_TOP_MB"         , "The most log space used at the same time since the database was activated, in MB");
		_lastRstm.setColumnDescription("SEC_LOGS_ALLOCATED__max" , "The most secondary log files that were allocated during the period (more than 0 = the primary logs were not enough)");
		_lastRstm.setColumnDescription("OLDEST_XACT_AGE_SEC__max", "The oldest open transaction during the period, in seconds");
		_lastRstm.setColumnDescription("LOGARCHMETH1"            , "Where log files are archived. OFF = circular logging (no online backup, no roll forward)");
		_lastRstm.setColumnDescription("ARCHIVE_METHOD1_STATUS"  , "The last archive attempt: 1 = it worked, 0 = it failed");
		_lastRstm.setColumnDescription("samples_archive_failing" , "In how many collector samples the archiving failed");
		_lastRstm.setColumnDescription("samples_count"           , "Number of collector samples in the period");
		_lastRstm.setColumnDescription("METHOD1_FIRST_FAILURE"   , "The first log file number that could not be archived");
		_lastRstm.setColumnDescription("CURRENT_ACTIVE_LOG"      , "The log file number written right now");
		_lastRstm.setColumnDescription("CURRENT_ARCHIVE_LOG"     , "The log file number being archived right now");
		_lastRstm.setColumnDescription("NUM_INDOUBT_TRANS"       , "Indoubt transactions (two-phase commit) that keep the log until they are resolved");

		// The transactions that were open a long time (they keep all log written after them from being reused)
		sql = ""
			    + "select top " + getTopRows() + " \n"
			    + "     [OLDEST_XACT_APPL_HANDLE] \n"
			    + "    ,max([OLDEST_XACT_AUTH_ID])                as [OLDEST_XACT_AUTH_ID] \n"
			    + "    ,max([OLDEST_XACT_APPL_NAME])              as [OLDEST_XACT_APPL_NAME] \n"
			    + "    ,max([OLDEST_XACT_CLIENT_HOST])            as [OLDEST_XACT_CLIENT_HOST] \n"
			    + "    ,[OLDEST_XACT_START_TIME] \n"
			    + "    ,max([SessionSampleTime])                  as [LAST_SEEN] \n"
			    + "    ,max([OLDEST_XACT_AGE_SEC])                as [OLDEST_XACT_AGE_SEC__max] \n"
			    + "    ,max([LOG_USED_PCT])                       as [LOG_USED_PCT__max] \n"
			    + "    ,max([OLDEST_XACT_LOG_USED_MB])            as [OLDEST_XACT_LOG_USED_MB__max] \n"
			    + "    ,sum(case when [OLDEST_XACT_STATE] = 'UOWWAIT' then 1 else 0 end) as [samples_idle] \n"
			    + "    ,count(*)                                  as [samples_count] \n"
			    + "from [CmTransactionLog_abs] \n"
			    + where + " \n"
			    + "  and [OLDEST_XACT_AGE_SEC] >= " + getMinAgeSec() + " \n"
			    + "group by [OLDEST_XACT_APPL_HANDLE], [OLDEST_XACT_START_TIME] \n"
			    + "order by max([LOG_USED_PCT]) desc, max([OLDEST_XACT_AGE_SEC]) desc \n"
			    + "";

		_holdersRstm = executeQuery(conn, sql, true, "Db2TransactionLogHolders");

		_holdersRstm.setHighlightSortColumns("LOG_USED_PCT__max");

		_holdersRstm.setDescription(
				"The oldest open transaction, when it had been open for more than " + getMinAgeSec() + " seconds (property <code>" + getClass().getSimpleName() + ".minAgeSec</code>). The fullest log first.<br>" +
				"Db2 can't reuse any log written after the start of the oldest open transaction, even if that transaction itself wrote little. " +
				"A transaction that waits for the client (<code>samples_idle</code>) is often a forgotten commit.<br>" +
				"");

		_holdersRstm.setColumnDescription("OLDEST_XACT_APPL_HANDLE"     , "The application handle of the connection with the oldest transaction");
		_holdersRstm.setColumnDescription("OLDEST_XACT_AUTH_ID"         , "The login");
		_holdersRstm.setColumnDescription("OLDEST_XACT_APPL_NAME"       , "The application name");
		_holdersRstm.setColumnDescription("OLDEST_XACT_CLIENT_HOST"     , "The client host");
		_holdersRstm.setColumnDescription("OLDEST_XACT_START_TIME"      , "When the transaction started");
		_holdersRstm.setColumnDescription("LAST_SEEN"                   , "The last collector sample where it was the oldest open transaction");
		_holdersRstm.setColumnDescription("OLDEST_XACT_AGE_SEC__max"    , "How long it was open, in seconds");
		_holdersRstm.setColumnDescription("LOG_USED_PCT__max"           , "The fullest the log was while it was the oldest open transaction, in percent");
		_holdersRstm.setColumnDescription("OLDEST_XACT_LOG_USED_MB__max", "Log written by the transaction itself, in MB");
		_holdersRstm.setColumnDescription("samples_idle"                , "In how many samples the connection waited for the client (UOWWAIT): the transaction was open, but nothing ran");
		_holdersRstm.setColumnDescription("samples_count"               , "In how many samples it was the oldest open transaction (older than the limit)");

		_CmTransactionLog_LogUsedPct   = createTsLineChart(conn, schema, CmTransactionLog.CM_NAME, CmTransactionLog.GRAPH_NAME_LOG_USED_PCT,   100, false, null, "Transaction Log Used in Percent (" + CmTransactionLog.SHORT_NAME + ")");
		_CmTransactionLog_LogWriteMb   = createTsLineChart(conn, schema, CmTransactionLog.CM_NAME, CmTransactionLog.GRAPH_NAME_LOG_WRITE_MB,   -1,  false, null, "Transaction Log Written and Read, MB per Second (" + CmTransactionLog.SHORT_NAME + ")");
		_CmTransactionLog_LogWriteTime = createTsLineChart(conn, schema, CmTransactionLog.CM_NAME, CmTransactionLog.GRAPH_NAME_LOG_WRITE_TIME, -1,  false, null, "Transaction Log Write Time, Average ms per Write (" + CmTransactionLog.SHORT_NAME + ")");
	}

	private IReportChart _CmTransactionLog_LogUsedPct;
	private IReportChart _CmTransactionLog_LogWriteMb;
	private IReportChart _CmTransactionLog_LogWriteTime;
}
