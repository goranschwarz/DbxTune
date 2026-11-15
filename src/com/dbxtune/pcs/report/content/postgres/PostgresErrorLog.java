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
package com.dbxtune.pcs.report.content.postgres;

import java.io.IOException;
import java.io.Writer;

import com.dbxtune.gui.ResultSetTableModel;
import com.dbxtune.pcs.report.DailySummaryReportAbstract;
import com.dbxtune.sql.conn.DbxConnection;
import com.dbxtune.utils.Configuration;

/**
 * Errors and warnings in the Postgres server log: how many of each (severity + SQLSTATE), and the newest rows (from CmErrorLog). Built on MySqlErrorLog.
 */
public class PostgresErrorLog
extends PostgresAbstract
{
	private ResultSetTableModel _countRstm; // null = not recorded
	private ResultSetTableModel _rowsRstm;

	/** The severities that are reported (LOG, INFO, NOTICE... are not) */
	private static final String SEVERITIES = "('PANIC', 'FATAL', 'ERROR', 'WARNING')";

	public PostgresErrorLog(DailySummaryReportAbstract reportingInstance)
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
			w.append("<b>The server log was not recorded</b> (the table <code>CmErrorLog_abs</code> does not exist).<br>\n");
			w.append("The Counter Model <code>CmErrorLog</code> reads the log through SQL, so it needs CSV or JSON logging: " +
					"<code>ALTER SYSTEM SET logging_collector = on;</code> (a restart) and <code>ALTER SYSTEM SET log_destination = 'stderr,csvlog';</code> (or 'stderr,jsonlog' on Postgres 15 and above), " +
					"and a login that may read the server files: <code>GRANT pg_read_server_files TO &lt;user&gt;;</code> (or a superuser). " +
					"A managed service (RDS, Azure, Cloud SQL...) usually has the log in its own console instead.<br>\n");
			return;
		}

		if (_countRstm.getRowCount() == 0)
		{
			w.append("No errors or warnings (PANIC, FATAL, ERROR, WARNING) were written to the server log during the period.<br>\n");
			return;
		}

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
		return "Error Log: errors and warnings (origin: CmErrorLog / the server log)";
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
		// No mandatory table: a server without CSV/JSON logging (or a login without access) is a text, not a red ERROR
		if ( ! doTableExist(conn, getReportingInstance().getDbmsSchemaName(), "CmErrorLog_abs") )
			return;

		// How many of each: severity + SQLSTATE (the messages have values in them, so they are not grouped on)
		String sql = ""
			    + "select \n"
			    + "     [error_severity] \n"
			    + "    ,[sql_state_code] \n"
			    + "    ,max([sql_state_code_desc])                 as [sql_state_code_desc] \n"
			    + "    ,count(*)                                   as [message_count] \n"
			    + "    ,count(distinct [database_name])            as [databases] \n"
			    + "    ,count(distinct [user_name])                as [users] \n"
			    + "    ,min([log_time])                            as [first_entry] \n"
			    + "    ,max([log_time])                            as [last_entry] \n"
			    + "    ,max(cast([message] as varchar(1000)))      as [example_message] \n"
			    + "from [CmErrorLog_abs] \n"
			    + "where [error_severity] in " + SEVERITIES + " \n"
			    + getReportPeriodSqlWhere()
			    + "group by [error_severity], [sql_state_code] \n"
			    + "order by case [error_severity] when 'PANIC' then 1 when 'FATAL' then 2 when 'ERROR' then 3 else 4 end, count(*) desc, [sql_state_code] \n"
			    + "";

		_countRstm = executeQuery(conn, sql, true, "PostgresErrorLogCount");
		_countRstm.setHighlightSortColumns("message_count");

		_countRstm.setDescription(
				"How many errors and warnings of each kind were written to the server log (from the table <code>CmErrorLog_abs</code>), grouped on severity and SQLSTATE. PANIC and FATAL first.<br>" +
				"<b>Note</b>: The collector reads the log while it runs, so rows written while it was not running are not here.<br>" +
				"Common ones: <code>28P01</code>/<code>28000</code> failed logins (wrong password, no pg_hba.conf entry), <code>23505</code> unique violation, <code>40P01</code> deadlock, " +
				"<code>57014</code> query canceled (statement_timeout), <code>53xxx</code> out of resources (disk full, too many connections), <code>XX000</code> internal error.<br>" +
				"");
		_countRstm.setColumnDescription("error_severity"     , "PANIC (the server restarts), FATAL (the session ends), ERROR (the statement fails) or WARNING");
		_countRstm.setColumnDescription("sql_state_code"     , "The SQLSTATE error code");
		_countRstm.setColumnDescription("sql_state_code_desc", "What the SQLSTATE means");
		_countRstm.setColumnDescription("message_count"      , "Number of rows in the log");
		_countRstm.setColumnDescription("databases"          , "In how many databases");
		_countRstm.setColumnDescription("users"              , "By how many users");
		_countRstm.setColumnDescription("first_entry"        , "The first row (the server's time)");
		_countRstm.setColumnDescription("last_entry"         , "The last row (the server's time)");
		_countRstm.setColumnDescription("example_message"    , "The text of one of the rows");

		// The rows, newest first
		sql = ""
			    + "select top " + getTopRows() + " \n"
			    + "     [log_time] \n"
			    + "    ,[error_severity] \n"
			    + "    ,[sql_state_code] \n"
			    + "    ,[user_name] \n"
			    + "    ,[database_name] \n"
			    + "    ,[application_name] \n"
			    + "    ,[connection_from] \n"
			    + "    ,cast([message] as varchar(2000))                as [message] \n"
			    + "    ,cast([detail]  as varchar(1000))                as [detail] \n"
			    + "    ,cast([hint]    as varchar(1000))                as [hint] \n"
			    + "    ,cast([query]   as varchar(1000))                as [query] \n"
			    + "from [CmErrorLog_abs] \n"
			    + "where [error_severity] in " + SEVERITIES + " \n"
			    + getReportPeriodSqlWhere()
			    + "order by [log_time] desc \n"
			    + "";

		_rowsRstm = executeQuery(conn, sql, true, "PostgresErrorLogRows");
		_rowsRstm.setDescription("The newest errors and warnings in the server log.<br>");
		_rowsRstm.setColumnDescription("log_time"        , "When the row was written (the server's time)");
		_rowsRstm.setColumnDescription("error_severity"  , "PANIC, FATAL, ERROR or WARNING");
		_rowsRstm.setColumnDescription("sql_state_code"  , "The SQLSTATE error code");
		_rowsRstm.setColumnDescription("user_name"       , "The user");
		_rowsRstm.setColumnDescription("database_name"   , "The database");
		_rowsRstm.setColumnDescription("application_name", "The client's application_name");
		_rowsRstm.setColumnDescription("connection_from" , "The client's host:port");
		_rowsRstm.setColumnDescription("message"         , "The message");
		_rowsRstm.setColumnDescription("detail"          , "More detail (for example the duplicate key value)");
		_rowsRstm.setColumnDescription("hint"            , "A hint from Postgres");
		_rowsRstm.setColumnDescription("query"           , "The statement that failed (when it is logged)");
	}
}
