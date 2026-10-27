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

import com.dbxtune.gui.ResultSetTableModel;
import com.dbxtune.pcs.report.DailySummaryReportAbstract;
import com.dbxtune.sql.conn.DbxConnection;
import com.dbxtune.utils.Configuration;

/**
 * The errors, warnings and deadlocks in the error log (from CmErrorLog). Built on OracleAlertLogErrors.
 */
public class MySqlErrorLog
extends MySqlAbstract
{
	private ResultSetTableModel _countRstm; // null = the error log was not recorded
	private ResultSetTableModel _rowsRstm;

	public MySqlErrorLog(DailySummaryReportAbstract reportingInstance)
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
		if (_countRstm == null)
		{
			w.append("<b>The error log was not recorded</b> (the table <code>CmErrorLog_abs</code> does not exist).<br>\n");
			w.append("Either the Counter Model <code>CmErrorLog</code> is disabled, the server is older than MySQL 8.0.22 (performance_schema.error_log), or the login can't read it: <code>GRANT SELECT ON performance_schema.* TO &lt;user&gt;</code><br>\n");
			return;
		}

		if (_countRstm.getRowCount() == 0)
		{
			w.append("No errors, warnings or deadlocks was found in the error log.<br>\n");
			return;
		}

		// Count per message
		w.append(getSectionDescriptionHtml(_countRstm, true));
		w.append("Row Count: " + _countRstm.getRowCount() + "<br>\n");
		w.append(toHtmlTable(_countRstm));

		// The rows (not in the short mail message)
		if (isFullMessageType())
		{
			w.append("<br>\n");
			w.append(getSectionDescriptionHtml(_rowsRstm, true));
			w.append("Row Count: " + _rowsRstm.getRowCount() + "&emsp;&emsp; To change number of <i>top</i> records, set property <code>" + getTopRowsPropertyName() + "=##</code><br>\n");
			w.append(toHtmlTable(_rowsRstm));
		}
	}

	@Override
	public String getSubject()
	{
		return "Error Log: errors, warnings and deadlocks (origin: CmErrorLog / performance_schema.error_log)";
	}

	@Override
	public boolean hasIssueToReport()
	{
		return false; // even if we found entries, do NOT indicate this as a Problem or Issue (the alarms for the error log does that)
	}

	/** The newest ## rows (the count table always has all of them) */
	@Override
	public int getTopRowsDefault()
	{
		return 100;
	}

	@Override
	public void create(DbxConnection conn, String srvName, Configuration pcsSavedConf, Configuration localConf)
	{
		// No mandatory table: an older version (before 8.0.22) or a login without access is a text, not a red ERROR
		if ( ! doTableExist(conn, getReportingInstance().getDbmsSchemaName(), "CmErrorLog_abs") )
			return;

		// Errors, warnings and the deadlocks (written as a note, MY-012468: the details are in the notes after it, MY-012469)
		String what = "([Prio] in ('Error', 'Warning') or [ErrorCode] = 'MY-012468') \n";

		// How many of each message
		String sql = ""
			    + "select \n"
			    + "     [Prio] \n"
			    + "    ,[ErrorCode] \n"
			    + "    ,max([Subsystem])   as [Subsystem] \n"
			    + "    ,count(*)           as [MessageCount] \n"
			    + "    ,min([LogTime])     as [FirstEntry] \n"
			    + "    ,max([LogTime])     as [LastEntry] \n"
			    + "    ,max([Message])     as [ExampleText] \n"
			    + "from [CmErrorLog_abs] \n"
			    + "where " + what
			    + getReportPeriodSqlWhere()
			    + "group by [Prio], [ErrorCode] \n"
			    + "order by case when [Prio] = 'Error' then 1 when [ErrorCode] = 'MY-012468' then 2 else 3 end, count(*) desc, [ErrorCode] \n"
			    + "";

		_countRstm = executeQuery(conn, sql, true, "MySqlErrorLogCount");

		_countRstm.setHighlightSortColumns("MessageCount");

		_countRstm.setDescription(
				"How many of each error, warning and deadlock that was written to the error log (from the table <code>CmErrorLog_abs</code>). Errors first, then deadlocks, then warnings.<br>" +
				"<b>Note</b>: When the collector starts it reads the last 24 hours of the error log, so the first rows can be older than the recording.<br>" +
				"<b>Note</b>: Deadlocks are only written if <code>innodb_print_all_deadlocks = ON</code> and <code>log_error_verbosity = 3</code>. The number of deadlocks is also in the Summary (InnoDB metrics).<br>" +
				"");

		_countRstm.setColumnDescription("Prio"        , "Error, Warning, or Note (only the deadlocks are notes here)");
		_countRstm.setColumnDescription("ErrorCode"   , "The message number. MY-012468 is a deadlock. Look it up in the MySQL 'Error Message Reference', or with: perror <number>");
		_countRstm.setColumnDescription("Subsystem"   , "Server, InnoDB, Repl (replication)...");
		_countRstm.setColumnDescription("MessageCount", "Number of rows in the error log with this message number");
		_countRstm.setColumnDescription("FirstEntry"  , "Time of the first row (the server's time)");
		_countRstm.setColumnDescription("LastEntry"   , "Time of the last row (the server's time)");
		_countRstm.setColumnDescription("ExampleText" , "The text of one of the rows");

		// The rows, newest first (the deadlock details too: they make the deadlock readable)
		sql = ""
			    + "select top " + getTopRows() + " \n"
			    + "     [LogTime] \n"
			    + "    ,[Prio] \n"
			    + "    ,[ErrorCode] \n"
			    + "    ,[Subsystem] \n"
			    + "    ,[ThreadId] \n"
			    + "    ,[Message] \n"
			    + "from [CmErrorLog_abs] \n"
			    + "where (" + what.trim() + " or [ErrorCode] = 'MY-012469') \n"
			    + getReportPeriodSqlWhere()
			    + "order by [LogTime] desc \n"
			    + "";

		_rowsRstm = executeQuery(conn, sql, true, "MySqlErrorLogRows");

		_rowsRstm.setDescription("The newest errors, warnings and deadlocks in the error log (a deadlock is several rows: MY-012468 and the details after it, MY-012469).<br>");

		_rowsRstm.setColumnDescription("LogTime"  , "When the row was written to the error log (the server's time)");
		_rowsRstm.setColumnDescription("Prio"     , "Error, Warning or Note");
		_rowsRstm.setColumnDescription("ErrorCode", "The message number");
		_rowsRstm.setColumnDescription("Subsystem", "Server, InnoDB, Repl (replication)...");
		_rowsRstm.setColumnDescription("ThreadId" , "The connection that wrote it. 0 = the server itself.");
		_rowsRstm.setColumnDescription("Message"  , "The text in the error log");
	}
}
