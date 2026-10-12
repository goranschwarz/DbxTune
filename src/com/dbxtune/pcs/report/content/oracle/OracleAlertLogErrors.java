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

import com.dbxtune.gui.ResultSetTableModel;
import com.dbxtune.pcs.report.DailySummaryReportAbstract;
import com.dbxtune.sql.conn.DbxConnection;
import com.dbxtune.utils.Configuration;

public class OracleAlertLogErrors
extends OracleAbstract
{
	private ResultSetTableModel _countRstm; // null = the alert log was not recorded
	private ResultSetTableModel _rowsRstm;

	public OracleAlertLogErrors(DailySummaryReportAbstract reportingInstance)
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
			w.append("<b>The alert log was not recorded</b> (the table <code>CmErrorLog_abs</code> does not exist).<br>\n");
			w.append("Either the Counter Model <code>CmErrorLog</code> is disabled, or the monitoring login can't read the alert log. That needs (as SYS): <code>GRANT SELECT ON SYS.V_$DIAG_ALERT_EXT TO &lt;user&gt;</code><br>\n");
			return;
		}

		if (_countRstm.getRowCount() == 0)
		{
			w.append("No rows with an ORA- error was found in the alert log.<br>\n");
			return;
		}

		// Count per error
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
		return "Alert Log: rows with an ORA- error (origin: CmErrorLog / V$DIAG_ALERT_EXT)";
	}

	@Override
	public boolean hasIssueToReport()
	{
		return false; // even if we found entries, do NOT indicate this as a Problem or Issue (the alarms for the alert log does that)
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
		// No mandatory table: a login without the grant on the alert log is common, and that should be a text and not a red ERROR
		if ( ! doTableExist(conn, getReportingInstance().getDbmsSchemaName(), "CmErrorLog_abs") )
			return;

		// How many of each error
		String sql = ""
			    + "select \n"
			    + "     [ORA_ERROR] \n"
			    + "    ,count(*)           as [ErrorCount] \n"
			    + "    ,min([LOG_TIME])    as [FirstEntry] \n"
			    + "    ,max([LOG_TIME])    as [LastEntry] \n"
			    + "    ,max([MESSAGE_TEXT]) as [ExampleText] \n"
			    + "from [CmErrorLog_abs] \n"
			    + "where [ORA_ERROR] is not null \n"
			    + getReportPeriodSqlWhere()
			    + "group by [ORA_ERROR] \n"
			    + "order by count(*) desc, [ORA_ERROR] \n"
			    + "";

		_countRstm = executeQuery(conn, sql, true, "OracleAlertLogErrorCount");

		_countRstm.setHighlightSortColumns("ErrorCount");

		_countRstm.setDescription(
				"How many rows of each ORA- error that was written to the alert log (from the table <code>CmErrorLog_abs</code>).<br>" +
				"<b>Note</b>: When the collector starts it reads the last 24 hours of the alert log, so the first rows can be older than the recording.<br>" +
				"");

		_countRstm.setColumnDescription("ORA_ERROR"  , "The first ORA- error number in the message text");
		_countRstm.setColumnDescription("ErrorCount" , "Number of rows in the alert log with this error");
		_countRstm.setColumnDescription("FirstEntry" , "Time of the first row with this error (the database servers time)");
		_countRstm.setColumnDescription("LastEntry"  , "Time of the last row with this error (the database servers time)");
		_countRstm.setColumnDescription("ExampleText", "The message text of one of the rows");

		// The rows, newest first
		sql = ""
			    + "select top " + getTopRows() + " \n"
			    + "     [LOG_TIME] \n"
			    + "    ,[ORA_ERROR] \n"
			    + "    ,[MESSAGE_TYPE] \n"
			    + "    ,[CON_NAME] \n"
			    + "    ,[PROBLEM_KEY] \n"
			    + "    ,[MODULE_ID] \n"
			    + "    ,[MESSAGE_TEXT] \n"
			    + "from [CmErrorLog_abs] \n"
			    + "where [ORA_ERROR] is not null \n"
			    + getReportPeriodSqlWhere()
			    + "order by [LOG_TIME] desc \n"
			    + "";

		_rowsRstm = executeQuery(conn, sql, true, "OracleAlertLogErrorRows");

		_rowsRstm.setDescription("The newest rows with an ORA- error in the alert log.<br>");

		_rowsRstm.setColumnDescription("LOG_TIME"    , "When the row was written to the alert log (the database servers time)");
		_rowsRstm.setColumnDescription("ORA_ERROR"   , "The first ORA- error number in the message text");
		_rowsRstm.setColumnDescription("MESSAGE_TYPE", "UNKNOWN, INCIDENT_ERROR, ERROR, WARNING, NOTIFICATION or TRACE. Most rows are UNKNOWN, also real errors.");
		_rowsRstm.setColumnDescription("CON_NAME"    , "Container (PDB) name, when the database is a Container Database");
		_rowsRstm.setColumnDescription("PROBLEM_KEY" , "For an incident (ORA-00600, ORA-07445...): the key that identifies the problem");
		_rowsRstm.setColumnDescription("MODULE_ID"   , "The module of the session that caused the row");
		_rowsRstm.setColumnDescription("MESSAGE_TEXT", "The text in the alert log");
	}
}
