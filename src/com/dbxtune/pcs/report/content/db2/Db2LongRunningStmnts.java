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
 * Long running statements, the connections that blocked others, and open units of work that waited for the client (from CmActiveStatements). Built on MySqlLongRunningStmnts.
 * <p>
 * CmActiveStatements is a snapshot every sample: a statement or a block is seen in the samples while it lasts, so the times here are what the samples saw.
 */
public class Db2LongRunningStmnts
extends Db2Abstract
{
	private static final Logger _logger = LogManager.getLogger(MethodHandles.lookup().lookupClass());

	private ResultSetTableModel _stmntRstm;
	private ResultSetTableModel _blockerRstm;
	private ResultSetTableModel _idleTxnRstm;

	public Db2LongRunningStmnts(DailySummaryReportAbstract reportingInstance)
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
		w.append(_blockerRstm.getRowCount() == 0 ? "No connection blocked other connections longer than " + getMinBlockingInSec() + " seconds.<br>\n" : toHtmlTable(_blockerRstm));

		w.append("<br>\n");
		w.append(getSectionDescriptionHtml(_stmntRstm, true));
		w.append("Row Count: " + _stmntRstm.getRowCount() + "&emsp;&emsp; To change number of <i>top</i> records, set property <code>" + getTopRowsPropertyName() + "=##</code><br>\n");
		w.append(_stmntRstm.getRowCount() == 0 ? "No statement ran longer than " + getMinExecTimeInSec() + " seconds.<br>\n" : toHtmlTable(_stmntRstm));

		w.append("<br>\n");
		w.append(getSectionDescriptionHtml(_idleTxnRstm, true));
		w.append("Row Count: " + _idleTxnRstm.getRowCount() + "<br>\n");
		w.append(_idleTxnRstm.getRowCount() == 0 ? "No unit of work waited for its client longer than " + getMinIdleTxnInSec() + " seconds.<br>\n" : toHtmlTable(_idleTxnRstm));
	}

	@Override
	public String getSubject()
	{
		return "Long Running Statements, Blocking and Idle Open Transactions (origin: CmActiveStatements / MON_GET_ACTIVITY, MON_GET_UNIT_OF_WORK, MON_GET_APPL_LOCKWAIT)";
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

	public static final String PROPKEY_minExecTimeInSec = "DailySummaryReport.Db2LongRunningStmnts.minExecTimeInSec";
	public static final int    DEFAULT_minExecTimeInSec = 10;

	public static final String PROPKEY_minIdleTxnInSec  = "DailySummaryReport.Db2LongRunningStmnts.minIdleTxnInSec";
	public static final int    DEFAULT_minIdleTxnInSec  = 60;

	public static final String PROPKEY_minBlockingInSec = "DailySummaryReport.Db2LongRunningStmnts.minBlockingInSec";
	public static final int    DEFAULT_minBlockingInSec = 10;

	private int getMinExecTimeInSec() { return Configuration.getCombinedConfiguration().getIntProperty(PROPKEY_minExecTimeInSec, DEFAULT_minExecTimeInSec); }
	private int getMinIdleTxnInSec()  { return Configuration.getCombinedConfiguration().getIntProperty(PROPKEY_minIdleTxnInSec , DEFAULT_minIdleTxnInSec ); }
	private int getMinBlockingInSec() { return Configuration.getCombinedConfiguration().getIntProperty(PROPKEY_minBlockingInSec, DEFAULT_minBlockingInSec); }

	@Override
	public void create(DbxConnection conn, String srvName, Configuration pcsSavedConf, Configuration localConf)
	{
		int topRows   = getTopRows();
		int pollTime  = getReportingInstance().getRecordingSampleTime();

		// The SQL Text is normally stored "Dictionary Compressed": the column has a hash id, and the text is in a table of its own
		boolean hasDictCompCols = false;
		try {
			hasDictCompCols = DictCompression.hasCompressedColumnNames(conn, null, "CmActiveStatements_abs");
		} catch (SQLException ex) {
			_logger.error("Problems checking for Dictionary Compressed Columns in table 'CmActiveStatements_abs'.", ex);
		}
		String col_SqlText = hasDictCompCols ? "SQL_TEXT$dcc$" : "SQL_TEXT";

		//--------------------------------------------------------------------------------
		// The connections that blocked others, and were not blocked themselves (the root cause)
		// One row per blocking "episode": the same connection and unit of work
		//--------------------------------------------------------------------------------
		String sql = ""
			    + "select top " + topRows + " \n"
			    + "     cast([APPLICATION_HANDLE] as varchar(20))                as [APPLICATION_HANDLE] \n"
			    + "    ,max([SESSION_AUTH_ID])                                  as [SESSION_AUTH_ID] \n"
			    + "    ,max([APPLICATION_NAME])                                 as [APPLICATION_NAME] \n"
			    + "    ,max([CLIENT_HOSTNAME])                                  as [CLIENT_HOSTNAME] \n"
			    + "    ,max([BLOCKING_OTHERS_MAX_SEC])                          as [BLOCKING_OTHERS_MAX_SEC] \n"
			    + "    ,max([BLOCKING_OTHER_APPL_HANDLES])                      as [BLOCKING_OTHER_APPL_HANDLES] \n"
			    + "    ,max([UOW_STATE])                                        as [UOW_STATE] \n"
			    + "    ,max([UOW_AGE_SEC])                                      as [UOW_AGE_SEC] \n"
			    + "    ,max([NUM_LOCKS_HELD])                                   as [NUM_LOCKS_HELD] \n"
			    + "    ,max([UOW_ROWS_MODIFIED])                                as [UOW_ROWS_MODIFIED] \n"
			    + "    ,min([SessionSampleTime])                                as [FirstSeen] \n"
			    + "    ,max([SessionSampleTime])                                as [LastSeen] \n"
			    + "    ,max([" + col_SqlText + "])                              as [SQL_TEXT] \n"
			    + "from [CmActiveStatements_abs] \n"
			    + "where [BLOCKING_OTHER_APPL_HANDLES] <> '' \n"
			    + "  and [BLOCKED_BY_APPL_HANDLE] = 0 \n"
			    + "  and [BLOCKING_OTHERS_MAX_SEC] >= " + getMinBlockingInSec() + " \n"
			    + getReportPeriodSqlWhere()
			    + "group by [APPLICATION_HANDLE], [UOW_ID] \n"
			    + "order by max([BLOCKING_OTHERS_MAX_SEC]) desc \n"
			    + "";

		_blockerRstm = executeQuery(conn, sql, true, "Db2Blockers", false);
		if (hasDictCompCols)
			updateDictionaryCompressedColumn(_blockerRstm, conn, null, "CmActiveStatements", "SQL_TEXT", "SQL_TEXT");

		_blockerRstm.setHighlightSortColumns("BLOCKING_OTHERS_MAX_SEC");
		_blockerRstm.setDescription(
				"<b>Blocking</b>: the connections that blocked other connections for at least " + getMinBlockingInSec() + " seconds, and were not blocked themselves: the root cause (property <code>" + PROPKEY_minBlockingInSec + "</code>).<br>" +
				"One row per blocking episode (the same connection and unit of work). <code>UOW_STATE</code> = UOWWAIT means that the connection did nothing: it held its locks while the application had control (a transaction that was not committed).<br>" +
				"<b>Note</b>: CmActiveStatements is a snapshot every " + pollTime + " seconds, so a short block can be missed, and the times are what the samples saw.<br>");
		_blockerRstm.setColumnDescription("APPLICATION_HANDLE"         , "The connection that blocked the others (what FORCE APPLICATION takes)");
		_blockerRstm.setColumnDescription("SESSION_AUTH_ID"            , "The user of the connection");
		_blockerRstm.setColumnDescription("APPLICATION_NAME"           , "The program of the connection");
		_blockerRstm.setColumnDescription("CLIENT_HOSTNAME"            , "The client host of the connection");
		_blockerRstm.setColumnDescription("BLOCKING_OTHERS_MAX_SEC"    , "The longest time one of the blocked connections waited, in seconds");
		_blockerRstm.setColumnDescription("BLOCKING_OTHER_APPL_HANDLES", "The connections that waited for it (at one of the samples)");
		_blockerRstm.setColumnDescription("UOW_STATE"                  , "What it did: UOWEXEC (it ran something), UOWWAIT (= nothing, it waited for the application)");
		_blockerRstm.setColumnDescription("UOW_AGE_SEC"                , "How old its unit of work (transaction) was, in seconds");
		_blockerRstm.setColumnDescription("NUM_LOCKS_HELD"             , "Locks its unit of work held");
		_blockerRstm.setColumnDescription("UOW_ROWS_MODIFIED"          , "Rows its unit of work had changed");
		_blockerRstm.setColumnDescription("FirstSeen"                  , "The first sample where it blocked others");
		_blockerRstm.setColumnDescription("LastSeen"                   , "The last sample where it blocked others");
		_blockerRstm.setColumnDescription("SQL_TEXT"                   , "The statement it ran (or ran last, when it did nothing)");

		//--------------------------------------------------------------------------------
		// The longest statements: one row per statement execution (the same connection, unit of work, activity and start time)
		//--------------------------------------------------------------------------------
		sql = ""
			    + "select top " + topRows + " \n"
			    + "     cast([APPLICATION_HANDLE] as varchar(20))                as [APPLICATION_HANDLE] \n"
			    + "    ,max([SESSION_AUTH_ID])                                  as [SESSION_AUTH_ID] \n"
			    + "    ,max([APPLICATION_NAME])                                 as [APPLICATION_NAME] \n"
			    + "    ,max([CLIENT_HOSTNAME])                                  as [CLIENT_HOSTNAME] \n"
			    + "    ,max([ACTIVITY_TYPE])                                    as [ACTIVITY_TYPE] \n"
			    + "    ,max([EXEC_TIME_SEC])                                    as [EXEC_TIME_SEC] \n"
			    + "    ,max([BLOCKED_SEC])                                      as [BLOCKED_SEC] \n"
			    + "    ,max([ACT_ROWS_READ])                                    as [ACT_ROWS_READ] \n"
			    + "    ,max([ACT_ROWS_RETURNED])                                as [ACT_ROWS_RETURNED] \n"
			    + "    ,max([ACT_CPU_TIME_MS])                                  as [ACT_CPU_TIME_MS] \n"
			    + "    ,max(cast([HAS_EXEC_PLAN] as int))                       as [HAS_EXEC_PLAN] \n"
			    + "    ,min([SessionSampleTime])                                as [FirstSeen] \n"
			    + "    ,max([SessionSampleTime])                                as [LastSeen] \n"
			    + "    ,max([EXECUTABLE_ID])                                    as [EXECUTABLE_ID] \n"
			    + "    ,max([" + col_SqlText + "])                              as [SQL_TEXT] \n"
			    + "from [CmActiveStatements_abs] \n"
			    + "where [ACTIVITY_STATE] = 'EXECUTING' \n"
			    + "  and [EXEC_TIME_SEC] >= " + getMinExecTimeInSec() + " \n"
			    + getReportPeriodSqlWhere()
			    + "group by [APPLICATION_HANDLE], [UOW_ID], [ACTIVITY_ID], [ACT_START_TIME] \n"
			    + "order by max([EXEC_TIME_SEC]) desc \n"
			    + "";

		_stmntRstm = executeQuery(conn, sql, true, "Db2LongRunningStmnts", false);
		if (hasDictCompCols)
			updateDictionaryCompressedColumn(_stmntRstm, conn, null, "CmActiveStatements", "SQL_TEXT", "SQL_TEXT");

		_stmntRstm.setHighlightSortColumns("EXEC_TIME_SEC");
		_stmntRstm.setDescription(
				"<b>Long running statements</b>: the statements that ran for at least " + getMinExecTimeInSec() + " seconds (property <code>" + PROPKEY_minExecTimeInSec + "</code>), the longest first.<br>" +
				"<code>BLOCKED_SEC</code> is how long it waited for a lock held by another connection (see Blocking above). HAS_EXEC_PLAN 1 = the plan was captured (in the 'Top SQL' section, by the same EXECUTABLE_ID).<br>" +
				"<b>Note</b>: The time is what the last sample saw: a statement ran up to " + pollTime + " seconds longer than this.<br>");
		_stmntRstm.setColumnDescription("APPLICATION_HANDLE", "The connection");
		_stmntRstm.setColumnDescription("SESSION_AUTH_ID"   , "The user of the connection");
		_stmntRstm.setColumnDescription("APPLICATION_NAME"  , "The program of the connection");
		_stmntRstm.setColumnDescription("CLIENT_HOSTNAME"   , "The client host of the connection");
		_stmntRstm.setColumnDescription("ACTIVITY_TYPE"     , "READ_DML, WRITE_DML, DDL, CALL, LOAD, OTHER");
		_stmntRstm.setColumnDescription("EXEC_TIME_SEC"     , "How long the statement had run at the last sample where it was seen, in seconds");
		_stmntRstm.setColumnDescription("BLOCKED_SEC"       , "How long it waited for a lock held by another connection, in seconds (the longest wait that was seen)");
		_stmntRstm.setColumnDescription("ACT_ROWS_READ"     , "Rows the statement had read at the last sample");
		_stmntRstm.setColumnDescription("ACT_ROWS_RETURNED" , "Rows the statement had returned at the last sample");
		_stmntRstm.setColumnDescription("ACT_CPU_TIME_MS"   , "CPU the statement had used at the last sample, in milliseconds");
		_stmntRstm.setColumnDescription("HAS_EXEC_PLAN"     , "1 = the execution plan was captured while it ran");
		_stmntRstm.setColumnDescription("FirstSeen"         , "The first sample where the statement was running");
		_stmntRstm.setColumnDescription("LastSeen"          , "The last sample where the statement was running");
		_stmntRstm.setColumnDescription("EXECUTABLE_ID"     , "The id of the statement in the package cache: the same as in the Top SQL section, if it's there");
		_stmntRstm.setColumnDescription("SQL_TEXT"          , "The statement");

		//--------------------------------------------------------------------------------
		// Open units of work that waited for the client (UOWWAIT) with locks: the application did not commit, and the locks are held
		//--------------------------------------------------------------------------------
		sql = ""
			    + "select top " + topRows + " \n"
			    + "     cast([APPLICATION_HANDLE] as varchar(20))                as [APPLICATION_HANDLE] \n"
			    + "    ,max([SESSION_AUTH_ID])                                  as [SESSION_AUTH_ID] \n"
			    + "    ,max([APPLICATION_NAME])                                 as [APPLICATION_NAME] \n"
			    + "    ,max([CLIENT_HOSTNAME])                                  as [CLIENT_HOSTNAME] \n"
			    + "    ,max([UOW_AGE_SEC])                                      as [UOW_AGE_SEC] \n"
			    + "    ,max([CLIENT_IDLE_SEC])                                  as [CLIENT_IDLE_SEC] \n"
			    + "    ,max([NUM_LOCKS_HELD])                                   as [NUM_LOCKS_HELD] \n"
			    + "    ,max([UOW_ROWS_MODIFIED])                                as [UOW_ROWS_MODIFIED] \n"
			    + "    ,max([UOW_LOG_USED_MB])                                  as [UOW_LOG_USED_MB] \n"
			    + "    ,max([BLOCKING_OTHER_APPL_HANDLES])                      as [BLOCKING_OTHER_APPL_HANDLES] \n"
			    + "    ,min([SessionSampleTime])                                as [FirstSeen] \n"
			    + "    ,max([SessionSampleTime])                                as [LastSeen] \n"
			    + "    ,max([" + col_SqlText + "])                              as [SQL_TEXT] \n"
			    + "from [CmActiveStatements_abs] \n"
			    + "where [UOW_STATE] = 'UOWWAIT' \n"
			    + "  and [NUM_LOCKS_HELD] > 0 \n"
			    + "  and [CLIENT_IDLE_SEC] >= " + getMinIdleTxnInSec() + " \n"
			    + getReportPeriodSqlWhere()
			    + "group by [APPLICATION_HANDLE], [UOW_ID] \n"
			    + "order by max([CLIENT_IDLE_SEC]) desc \n"
			    + "";

		_idleTxnRstm = executeQuery(conn, sql, true, "Db2IdleOpenTxn", false);
		if (hasDictCompCols)
			updateDictionaryCompressedColumn(_idleTxnRstm, conn, null, "CmActiveStatements", "SQL_TEXT", "SQL_TEXT");

		_idleTxnRstm.setHighlightSortColumns("CLIENT_IDLE_SEC");
		_idleTxnRstm.setDescription(
				"<b>Idle open transactions</b>: a unit of work with locks, where the connection had waited for the client for at least " + getMinIdleTxnInSec() + " seconds (property <code>" + PROPKEY_minIdleTxnInSec + "</code>).<br>" +
				"The application started a transaction and did not commit it: its locks are held (others can wait for them), and its log can not be reused (a long one can fill the transaction log).<br>" +
				"Typical causes: autocommit off without a commit, or a commit that is only done after a slow call to something else.<br>");
		_idleTxnRstm.setColumnDescription("APPLICATION_HANDLE"         , "The connection");
		_idleTxnRstm.setColumnDescription("SESSION_AUTH_ID"            , "The user of the connection");
		_idleTxnRstm.setColumnDescription("APPLICATION_NAME"           , "The program of the connection");
		_idleTxnRstm.setColumnDescription("CLIENT_HOSTNAME"            , "The client host of the connection");
		_idleTxnRstm.setColumnDescription("UOW_AGE_SEC"                , "How old the unit of work was at the last sample, in seconds");
		_idleTxnRstm.setColumnDescription("CLIENT_IDLE_SEC"            , "How long the connection had waited for the client at the last sample, in seconds");
		_idleTxnRstm.setColumnDescription("NUM_LOCKS_HELD"             , "Locks the unit of work held");
		_idleTxnRstm.setColumnDescription("UOW_ROWS_MODIFIED"          , "Rows the unit of work had changed (not committed)");
		_idleTxnRstm.setColumnDescription("UOW_LOG_USED_MB"            , "Transaction log used by the unit of work, in MB");
		_idleTxnRstm.setColumnDescription("BLOCKING_OTHER_APPL_HANDLES", "Connections that waited for it (at one of the samples)");
		_idleTxnRstm.setColumnDescription("FirstSeen"                  , "The first sample where it was seen idle with the unit of work open");
		_idleTxnRstm.setColumnDescription("LastSeen"                   , "The last sample where it was seen idle with the unit of work open");
		_idleTxnRstm.setColumnDescription("SQL_TEXT"                   , "The last statement the connection ran");
	}
}
