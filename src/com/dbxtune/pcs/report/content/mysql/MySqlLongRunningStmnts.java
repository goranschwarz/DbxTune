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
import java.lang.invoke.MethodHandles;
import java.sql.SQLException;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.dbxtune.gui.ResultSetTableModel;
import com.dbxtune.pcs.DictCompression;
import com.dbxtune.pcs.report.DailySummaryReportAbstract;
import com.dbxtune.sql.conn.DbxConnection;
import com.dbxtune.utils.Configuration;

/**
 * Long running statements, the connections that blocked others, and long open transactions that did nothing (from CmActiveStatements).
 * <p>
 * CmActiveStatements is a snapshot every sample: a statement or a block is seen in the samples while it lasts, so the times here are what the samples saw.
 */
public class MySqlLongRunningStmnts
extends MySqlAbstract
{
	private static final Logger _logger = LogManager.getLogger(MethodHandles.lookup().lookupClass());

	private ResultSetTableModel _stmntRstm;
	private ResultSetTableModel _blockerRstm;
	private ResultSetTableModel _idleTxnRstm;

	public MySqlLongRunningStmnts(DailySummaryReportAbstract reportingInstance)
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
		w.append(getSectionDescriptionHtml(_blockerRstm, true));
		w.append("Row Count: " + _blockerRstm.getRowCount() + "<br>\n");
		w.append(_blockerRstm.getRowCount() == 0 ? "No connection blocked other connections longer than the alarm limit.<br>\n" : toHtmlTable(_blockerRstm));

		w.append("<br>\n");
		w.append(getSectionDescriptionHtml(_stmntRstm, true));
		w.append("Row Count: " + _stmntRstm.getRowCount() + "&emsp;&emsp; To change number of <i>top</i> records, set property <code>" + getTopRowsPropertyName() + "=##</code><br>\n");
		w.append(_stmntRstm.getRowCount() == 0 ? "No statement ran longer than " + getMinExecTimeInSec() + " seconds.<br>\n" : toHtmlTable(_stmntRstm));

		w.append("<br>\n");
		w.append(getSectionDescriptionHtml(_idleTxnRstm, true));
		w.append("Row Count: " + _idleTxnRstm.getRowCount() + "<br>\n");
		w.append(_idleTxnRstm.getRowCount() == 0 ? "No transaction was open and idle longer than " + getMinIdleTxnInSec() + " seconds.<br>\n" : toHtmlTable(_idleTxnRstm));
	}

	@Override
	public String getSubject()
	{
		return "Long Running Statements, Blocking and Idle Open Transactions (origin: CmActiveStatements / performance_schema.threads, events_statements_current, INNODB_TRX)";
	}

	@Override
	public boolean hasIssueToReport()
	{
		return false; // even if we found entries, do NOT indicate this as a Problem or Issue (the alarms does that)
	}

	@Override
	public String[] getMandatoryTables()
	{
		return new String[] { "CmActiveStatements_abs" };
	}

	public static final String PROPKEY_minExecTimeInSec = "DailySummaryReport.MySqlLongRunningStmnts.minExecTimeInSec";
	public static final int    DEFAULT_minExecTimeInSec = 10;

	public static final String PROPKEY_minIdleTxnInSec  = "DailySummaryReport.MySqlLongRunningStmnts.minIdleTxnInSec";
	public static final int    DEFAULT_minIdleTxnInSec  = 60;

	public static final String PROPKEY_minBlockingInSec = "DailySummaryReport.MySqlLongRunningStmnts.minBlockingInSec";
	public static final int    DEFAULT_minBlockingInSec = 10;

	private int getMinExecTimeInSec() { return Configuration.getCombinedConfiguration().getIntProperty(PROPKEY_minExecTimeInSec, DEFAULT_minExecTimeInSec); }
	private int getMinIdleTxnInSec()  { return Configuration.getCombinedConfiguration().getIntProperty(PROPKEY_minIdleTxnInSec , DEFAULT_minIdleTxnInSec ); }
	private int getMinBlockingInSec() { return Configuration.getCombinedConfiguration().getIntProperty(PROPKEY_minBlockingInSec, DEFAULT_minBlockingInSec); }

	@Override
	public void create(DbxConnection conn, String srvName, Configuration pcsSavedConf, Configuration localConf)
	{
		int topRows   = getTopRows();
		int pollTime  = getReportingInstance().getRecordingSampleTime();

		// The SQL Text is normally stored "Dictionary Compressed": the column has a hash id, and the text is in a table of it's own
		boolean hasDictCompCols = false;
		try {
			hasDictCompCols = DictCompression.hasCompressedColumnNames(conn, null, "CmActiveStatements_abs");
		} catch (SQLException ex) {
			_logger.error("Problems checking for Dictionary Compressed Columns in table 'CmActiveStatements_abs'.", ex);
		}
		String col_SqlText = hasDictCompCols ? "SqlText$dcc$" : "SqlText";

		//--------------------------------------------------------------------------------
		// The connections that blocked others, and were not blocked themselves (the root cause)
		// One row per blocking "episode": the same connection, the same transaction (or the same statement when there is no transaction)
		//--------------------------------------------------------------------------------
		String sql = ""
			    + "select top " + topRows + " \n"
			    + "     cast([Pid] as varchar(20))                               as [Pid] \n"
			    + "    ,max([UserName])                                         as [UserName] \n"
			    + "    ,max([Host])                                             as [Host] \n"
			    + "    ,max([ProgramName])                                      as [ProgramName] \n"
			    + "    ,max([BlockingOthersMaxSec])                             as [BlockingOthersMaxSec] \n"
			    + "    ,max([BlockingOtherPids])                                as [BlockingOtherPids] \n"
			    + "    ,max([Command])                                          as [Command] \n"
			    + "    ,max([HasOpenTxn])                                       as [HasOpenTxn] \n"
			    + "    ,max([TxnAgeInSec])                                      as [TxnAgeInSec] \n"
			    + "    ,max([TxnRowsLocked])                                    as [TxnRowsLocked] \n"
			    + "    ,min([SessionSampleTime])                                as [FirstSeen] \n"
			    + "    ,max([SessionSampleTime])                                as [LastSeen] \n"
			    + "    ,max([" + col_SqlText + "])                              as [SqlText] \n"
			    + "from [CmActiveStatements_abs] \n"
			    + "where [BlockingOtherPids] <> '' \n"
			    + "  and [BlockedByPid] = 0 \n"
			    + "  and [BlockingOthersMaxSec] >= " + getMinBlockingInSec() + " \n"
			    + getReportPeriodSqlWhere()
			    + "group by [Pid], coalesce(cast([TxnStartTime] as varchar(30)), cast([StmntEventId] as varchar(30))) \n"
			    + "order by max([BlockingOthersMaxSec]) desc \n"
			    + "";

		_blockerRstm = executeQuery(conn, sql, true, "MySqlBlockers", false);
		if (hasDictCompCols)
			updateDictionaryCompressedColumn(_blockerRstm, conn, null, "CmActiveStatements", "SqlText", "SqlText");

		_blockerRstm.setHighlightSortColumns("BlockingOthersMaxSec");
		_blockerRstm.setDescription(
				"<b>Blocking</b>: the connections that blocked other connections (row locks or metadata locks) for at least " + getMinBlockingInSec() + " seconds, and were not blocked themselves: the root cause (property <code>" + PROPKEY_minBlockingInSec + "</code>).<br>" +
				"One row per blocking episode (the same connection and transaction). <code>Command</code> = Sleep means that the connection did nothing: it held its locks while the application had control (a transaction that was not committed).<br>" +
				"<b>Note</b>: CmActiveStatements is a snapshot every " + pollTime + " seconds, so a short block can be missed, and the times are what the samples saw.<br>");
		_blockerRstm.setColumnDescription("Pid"                 , "The connection (processlist id) that blocked the others");
		_blockerRstm.setColumnDescription("UserName"            , "The user of the connection");
		_blockerRstm.setColumnDescription("Host"                , "The client host of the connection");
		_blockerRstm.setColumnDescription("ProgramName"         , "The program, if the client tells it (connection attribute 'program_name')");
		_blockerRstm.setColumnDescription("BlockingOthersMaxSec", "The longest time one of the blocked connections waited, in seconds");
		_blockerRstm.setColumnDescription("BlockingOtherPids"   , "The connections that waited for it (at one of the samples)");
		_blockerRstm.setColumnDescription("Command"             , "What it did: Query, Sleep (= nothing, it waited for the application)...");
		_blockerRstm.setColumnDescription("HasOpenTxn"          , "Y = it had an open transaction");
		_blockerRstm.setColumnDescription("TxnAgeInSec"         , "How old its transaction was, in seconds");
		_blockerRstm.setColumnDescription("TxnRowsLocked"       , "Rows its transaction had locked");
		_blockerRstm.setColumnDescription("FirstSeen"           , "The first sample where it blocked others");
		_blockerRstm.setColumnDescription("LastSeen"            , "The last sample where it blocked others");
		_blockerRstm.setColumnDescription("SqlText"             , "The statement it ran (or ran last, when Command is Sleep)");

		//--------------------------------------------------------------------------------
		// The longest statements: one row per statement execution (the same connection and statement event)
		//--------------------------------------------------------------------------------
		sql = ""
			    + "select top " + topRows + " \n"
			    + "     cast([Pid] as varchar(20))                               as [Pid] \n"
			    + "    ,max([UserName])                                         as [UserName] \n"
			    + "    ,max([Host])                                             as [Host] \n"
			    + "    ,max([ProgramName])                                      as [ProgramName] \n"
			    + "    ,max([CurrentSchema])                                    as [CurrentSchema] \n"
			    + "    ,max([ExecTimeInSec])                                    as [ExecTimeInSec] \n"
			    + "    ,max([BlockedSeconds])                                   as [BlockedSeconds] \n"
			    + "    ,max([RowsExamined])                                     as [RowsExamined] \n"
			    + "    ,max([RowsSent])                                         as [RowsSent] \n"
			    + "    ,min([SessionSampleTime])                                as [FirstSeen] \n"
			    + "    ,max([SessionSampleTime])                                as [LastSeen] \n"
			    + "    ,max([Digest])                                           as [Digest] \n"
			    + "    ,max([" + col_SqlText + "])                              as [SqlText] \n"
			    + "from [CmActiveStatements_abs] \n"
			    + "where [Command] <> 'Sleep' \n"
			    + "  and [ExecTimeInSec] >= " + getMinExecTimeInSec() + " \n"
			    + getReportPeriodSqlWhere()
			    + "group by [Pid], [StmntEventId] \n"
			    + "order by max([ExecTimeInSec]) desc \n"
			    + "";

		_stmntRstm = executeQuery(conn, sql, true, "MySqlLongRunningStmnts", false);
		if (hasDictCompCols)
			updateDictionaryCompressedColumn(_stmntRstm, conn, null, "CmActiveStatements", "SqlText", "SqlText");

		_stmntRstm.setHighlightSortColumns("ExecTimeInSec");
		_stmntRstm.setDescription(
				"<b>Long running statements</b>: the statements that ran for at least " + getMinExecTimeInSec() + " seconds (property <code>" + PROPKEY_minExecTimeInSec + "</code>), the longest first.<br>" +
				"<code>BlockedSeconds</code> is how long it waited for a lock held by another connection (see Blocking above).<br>" +
				"<b>Note</b>: The time is what the last sample saw: a statement ran up to " + pollTime + " seconds longer than this.<br>");
		_stmntRstm.setColumnDescription("Pid"           , "The connection (processlist id)");
		_stmntRstm.setColumnDescription("UserName"      , "The user of the connection");
		_stmntRstm.setColumnDescription("Host"          , "The client host of the connection");
		_stmntRstm.setColumnDescription("ProgramName"   , "The program, if the client tells it (connection attribute 'program_name')");
		_stmntRstm.setColumnDescription("CurrentSchema" , "The default schema of the connection (USE ...)");
		_stmntRstm.setColumnDescription("ExecTimeInSec" , "How long the statement had run at the last sample where it was seen, in seconds");
		_stmntRstm.setColumnDescription("BlockedSeconds", "How long it waited for a lock held by another connection, in seconds (the longest wait that was seen)");
		_stmntRstm.setColumnDescription("RowsExamined"  , "Rows the statement had read at the last sample");
		_stmntRstm.setColumnDescription("RowsSent"      , "Rows the statement had returned at the last sample");
		_stmntRstm.setColumnDescription("FirstSeen"     , "The first sample where the statement was running");
		_stmntRstm.setColumnDescription("LastSeen"      , "The last sample where the statement was running");
		_stmntRstm.setColumnDescription("Digest"        , "The digest of the statement: the same as in the Top SQL section, if it's there");
		_stmntRstm.setColumnDescription("SqlText"       , "The statement");

		//--------------------------------------------------------------------------------
		// Open transactions that did nothing (Command = Sleep): the application did not commit, and the locks are held
		//--------------------------------------------------------------------------------
		sql = ""
			    + "select top " + topRows + " \n"
			    + "     cast([Pid] as varchar(20))                               as [Pid] \n"
			    + "    ,max([UserName])                                         as [UserName] \n"
			    + "    ,max([Host])                                             as [Host] \n"
			    + "    ,max([ProgramName])                                      as [ProgramName] \n"
			    + "    ,max([TxnAgeInSec])                                      as [TxnAgeInSec] \n"
			    + "    ,max([TimeInStateSec])                                   as [IdleSec] \n"
			    + "    ,max([TxnRowsLocked])                                    as [TxnRowsLocked] \n"
			    + "    ,max([TxnRowsModified])                                  as [TxnRowsModified] \n"
			    + "    ,max([BlockingOtherPids])                                as [BlockingOtherPids] \n"
			    + "    ,min([SessionSampleTime])                                as [FirstSeen] \n"
			    + "    ,max([SessionSampleTime])                                as [LastSeen] \n"
			    + "    ,max([" + col_SqlText + "])                              as [SqlText] \n"
			    + "from [CmActiveStatements_abs] \n"
			    + "where [HasOpenTxn] = 'Y' \n"
			    + "  and [Command] = 'Sleep' \n"
			    + "  and [TxnAgeInSec] >= " + getMinIdleTxnInSec() + " \n"
			    + getReportPeriodSqlWhere()
			    + "group by [Pid], [TxnStartTime] \n"
			    + "order by max([TxnAgeInSec]) desc \n"
			    + "";

		_idleTxnRstm = executeQuery(conn, sql, true, "MySqlIdleOpenTxn", false);
		if (hasDictCompCols)
			updateDictionaryCompressedColumn(_idleTxnRstm, conn, null, "CmActiveStatements", "SqlText", "SqlText");

		_idleTxnRstm.setHighlightSortColumns("TxnAgeInSec");
		_idleTxnRstm.setDescription(
				"<b>Idle open transactions</b>: a transaction that had been open for at least " + getMinIdleTxnInSec() + " seconds, while the connection did nothing (property <code>" + PROPKEY_minIdleTxnInSec + "</code>).<br>" +
				"The application started a transaction and did not commit it: its locks are held, and InnoDB can't purge old row versions (the History List Length grows).<br>" +
				"Typical causes: autocommit=0 without a commit, or a commit that is only done after a slow call to something else.<br>");
		_idleTxnRstm.setColumnDescription("Pid"            , "The connection (processlist id)");
		_idleTxnRstm.setColumnDescription("UserName"       , "The user of the connection");
		_idleTxnRstm.setColumnDescription("Host"           , "The client host of the connection");
		_idleTxnRstm.setColumnDescription("ProgramName"    , "The program, if the client tells it (connection attribute 'program_name')");
		_idleTxnRstm.setColumnDescription("TxnAgeInSec"    , "How old the transaction was at the last sample, in seconds");
		_idleTxnRstm.setColumnDescription("IdleSec"        , "How long the connection had done nothing at the last sample, in seconds");
		_idleTxnRstm.setColumnDescription("TxnRowsLocked"  , "Rows the transaction had locked");
		_idleTxnRstm.setColumnDescription("TxnRowsModified", "Rows the transaction had changed (not committed)");
		_idleTxnRstm.setColumnDescription("BlockingOtherPids", "Connections that waited for it (at one of the samples)");
		_idleTxnRstm.setColumnDescription("FirstSeen"      , "The first sample where it was seen idle with the transaction open");
		_idleTxnRstm.setColumnDescription("LastSeen"       , "The last sample where it was seen idle with the transaction open");
		_idleTxnRstm.setColumnDescription("SqlText"        , "The last statement the connection ran");
	}
}
