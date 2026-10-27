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

import com.dbxtune.gui.ResultSetTableModel;
import com.dbxtune.pcs.report.DailySummaryReportAbstract;
import com.dbxtune.sql.conn.DbxConnection;
import com.dbxtune.utils.Configuration;

/**
 * The critical, error and warning messages of the notification log (from CmErrorLog). Built on MySqlErrorLog.
 */
public class Db2ErrorLog
extends Db2Abstract
{
	private ResultSetTableModel _countRstm; // null = the notification log was not recorded
	private ResultSetTableModel _rowsRstm;

	public Db2ErrorLog(DailySummaryReportAbstract reportingInstance)
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
			w.append("<b>The notification log was not recorded</b> (the table <code>CmErrorLog_abs</code> does not exist).<br>\n");
			w.append("Either the Counter Model <code>CmErrorLog</code> is disabled, or the login can't use PD_GET_LOG_MSGS (see DbmsSetup/db2.txt).<br>\n");
			return;
		}

		if (_countRstm.getRowCount() == 0)
		{
			w.append("No critical, error or warning messages was found in the notification log.<br>\n");
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
		return "Notification Log: critical, error and warning messages (origin: CmErrorLog / PD_GET_LOG_MSGS)";
	}

	@Override
	public boolean hasIssueToReport()
	{
		return false; // even if we found entries, do NOT indicate this as a Problem or Issue (the alarms for the log does that)
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
		// No mandatory table: a login without access is a text, not a red ERROR
		if ( ! doTableExist(conn, getReportingInstance().getDbmsSchemaName(), "CmErrorLog_abs") )
			return;

		// MSGSEVERITY: C = critical, E = error, W = warning, I = information
		String what = "[MSGSEVERITY] in ('C', 'E', 'W') \n";

		// How many of each message
		String sql = ""
			    + "select \n"
			    + "     [MSGSEVERITY] \n"
			    + "    ,[MSG_ID] \n"
			    + "    ,max([DBNAME])        as [DBNAME] \n"
			    + "    ,count(*)             as [MessageCount] \n"
			    + "    ,min([TIMESTAMP])     as [FirstEntry] \n"
			    + "    ,max([TIMESTAMP])     as [LastEntry] \n"
			    + "    ,max([MESSAGE_TEXT])  as [ExampleText] \n"
			    + "from [CmErrorLog_abs] \n"
			    + "where " + what
			    + getReportPeriodSqlWhere()
			    + "group by [MSGSEVERITY], [MSG_ID] \n"
			    + "order by case when [MSGSEVERITY] = 'C' then 1 when [MSGSEVERITY] = 'E' then 2 else 3 end, count(*) desc, [MSG_ID] \n"
			    + "";

		_countRstm = executeQuery(conn, sql, true, "Db2ErrorLogCount");

		_countRstm.setHighlightSortColumns("MessageCount");

		_countRstm.setDescription(
				"How many of each critical, error and warning message that was written to the notification log (from the table <code>CmErrorLog_abs</code>). Critical first, then errors, then warnings.<br>" +
				"<b>Note</b>: When the collector starts it reads the last hours of the log (property <code>CmErrorLog.sample.firstSample.hours</code>), so the first rows can be older than the recording.<br>" +
				"<b>Note</b>: The notification log is for the whole instance: a message without a DBNAME is about the instance, or another database.<br>" +
				"");

		_countRstm.setColumnDescription("MSGSEVERITY" , "C = critical, E = error, W = warning");
		_countRstm.setColumnDescription("MSG_ID"      , "The message id (ADM6044E, ADM1823E...). Look it up with: db2 ? <id>");
		_countRstm.setColumnDescription("DBNAME"      , "The database the message is about (empty = the instance)");
		_countRstm.setColumnDescription("MessageCount", "Number of messages with this id");
		_countRstm.setColumnDescription("FirstEntry"  , "Time of the first message (the server's time)");
		_countRstm.setColumnDescription("LastEntry"   , "Time of the last message (the server's time)");
		_countRstm.setColumnDescription("ExampleText" , "The text of one of the messages");

		// The rows, newest first
		sql = ""
			    + "select top " + getTopRows() + " \n"
			    + "     [TIMESTAMP] \n"
			    + "    ,[MSGSEVERITY] \n"
			    + "    ,[MSG_ID] \n"
			    + "    ,[DBNAME] \n"
			    + "    ,[APPL_ID] \n"
			    + "    ,[MESSAGE_TEXT] \n"
			    + "from [CmErrorLog_abs] \n"
			    + "where " + what
			    + getReportPeriodSqlWhere()
			    + "order by [TIMESTAMP] desc \n"
			    + "";

		_rowsRstm = executeQuery(conn, sql, true, "Db2ErrorLogRows");

		_rowsRstm.setDescription("The newest critical, error and warning messages in the notification log.<br>");

		_rowsRstm.setColumnDescription("TIMESTAMP"   , "When the message was written (the server's time)");
		_rowsRstm.setColumnDescription("MSGSEVERITY" , "C = critical, E = error, W = warning");
		_rowsRstm.setColumnDescription("MSG_ID"      , "The message id");
		_rowsRstm.setColumnDescription("DBNAME"      , "The database (empty = the instance)");
		_rowsRstm.setColumnDescription("APPL_ID"     , "The application (connection) it is about, if any");
		_rowsRstm.setColumnDescription("MESSAGE_TEXT", "The text of the message");
	}
}
