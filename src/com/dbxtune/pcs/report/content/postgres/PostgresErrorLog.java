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

import java.awt.Color;
import java.io.IOException;
import java.io.Writer;
import java.lang.invoke.MethodHandles;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.dbxtune.cm.postgres.CmErrorLog;
import com.dbxtune.gui.ResultSetTableModel;
import com.dbxtune.pcs.report.DailySummaryReportAbstract;
import com.dbxtune.pcs.report.content.IReportChart;
import com.dbxtune.pcs.report.content.ReportChartAbstract;
import com.dbxtune.pcs.report.content.ReportChartTimeSeriesStackedBar;
import com.dbxtune.sql.conn.DbxConnection;
import com.dbxtune.utils.Configuration;

/**
 * Errors, warnings, and operational analytics (pgbadger-style insights: checkpoints, autovacuum/autoanalyze,
 * temporary files, lock waits, and slow statements / auto_explain) in the Postgres server log (from CmErrorLog).
 */
public class PostgresErrorLog
extends PostgresAbstract
{
	private static final Logger _logger = LogManager.getLogger(MethodHandles.lookup().lookupClass());

	private ResultSetTableModel _countRstm; // null = not recorded
	private ResultSetTableModel _rowsRstm;

	// Operational LOG analytics (pgbadger-style insights)
	private ResultSetTableModel _ckptRstm;
	private ResultSetTableModel _autovacRstm;
	private ResultSetTableModel _tempFilesRstm;
	private ResultSetTableModel _lockWaitsRstm;
	private ResultSetTableModel _lockWaitRowsRstm;
	private ResultSetTableModel _slowQueriesRstm;
	private List<String>        _verdicts = new ArrayList<>();
	private String              _logConfigNote;

	private IReportChart        _CmErrorLog_SeverityCount;
	private IReportChart        _CmErrorLog_SeverityPerSec;
	private IReportChart        _CmErrorLog_OperationalEvents;
	private IReportChart        _CmErrorLog_OperationalEventsPerSec;

	/** The severities that are reported in the error/warning count table */
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
			w.append("The Counter Model <code>CmErrorLog</code> reads the log either:<br>\n");
			w.append("<ul>\n");
			w.append("  <li><b>Through SQL (on-prem / VM)</b>: needs CSV or JSON logging (<code>ALTER SYSTEM SET logging_collector = on;</code> and " +
					"<code>ALTER SYSTEM SET log_destination = 'stderr,csvlog';</code> or <code>'stderr,jsonlog'</code> on Postgres 15+), " +
					"and a login with <code>GRANT pg_read_server_files TO &lt;user&gt;;</code> (or superuser).</li>\n");
			w.append("  <li><b>Through Cloud Logging (GCP Cloud SQL)</b>: configure Host Monitor with <code>gcp://&lt;project&gt;:&lt;region&gt;:&lt;instance&gt;</code> " +
					"and grant <code>roles/logging.viewer</code> to the service account.</li>\n");
			w.append("</ul>\n");
			return;
		}

		//---------------------------------------------------------------------
		// 1. Errors & Warnings (PANIC, FATAL, ERROR, WARNING)
		//---------------------------------------------------------------------
		if (_countRstm.getRowCount() == 0)
		{
			w.append("No errors or warnings (PANIC, FATAL, ERROR, WARNING) were written to the server log during the period.<br>\n");
		}
		else
		{
			w.append(getSectionDescriptionHtml(_countRstm, true));
			w.append("Row Count: " + _countRstm.getRowCount() + "<br>\n");
			w.append(toHtmlTable(_countRstm));

			// The rows (not in the short mail message)
			if (isFullMessageType() && _rowsRstm != null && _rowsRstm.getRowCount() > 0)
			{
				w.append("<br>\n");
				w.append(getSectionDescriptionHtml(_rowsRstm, true));
				w.append("Row Count: " + _rowsRstm.getRowCount() + "&emsp;&emsp; To change number of <i>top</i> records, set property <code>" + getTopRowsPropertyName() + "=##</code><br>\n");
				w.append(toHtmlTable(_rowsRstm));
			}
		}

		// Live Trend Charts from CmErrorLog (if recorded)
		if (_CmErrorLog_SeverityCount != null || _CmErrorLog_SeverityPerSec != null || _CmErrorLog_OperationalEvents != null || _CmErrorLog_OperationalEventsPerSec != null)
		{
			w.append("<br>\n");
			w.append(getDbxCentralLinkWithDescForGraphs(false, "Below are the server log message counts and rates (per second) as well as operational events during the day.",
					CmErrorLog.CM_NAME + "_" + CmErrorLog.GRAPH_NAME_SEVERITY_COUNT,
					CmErrorLog.CM_NAME + "_" + CmErrorLog.GRAPH_NAME_SEVERITY_PER_SEC,
					CmErrorLog.CM_NAME + "_" + CmErrorLog.GRAPH_NAME_OPERATIONAL_EVENTS,
					CmErrorLog.CM_NAME + "_" + CmErrorLog.GRAPH_NAME_OPERATIONAL_PER_SEC
					));
			if (_CmErrorLog_SeverityCount != null)
				_CmErrorLog_SeverityCount.writeHtmlContent(w, null, null);
			if (_CmErrorLog_SeverityPerSec != null)
				_CmErrorLog_SeverityPerSec.writeHtmlContent(w, null, null);
			if (_CmErrorLog_OperationalEvents != null)
				_CmErrorLog_OperationalEvents.writeHtmlContent(w, null, null);
			if (_CmErrorLog_OperationalEventsPerSec != null)
				_CmErrorLog_OperationalEventsPerSec.writeHtmlContent(w, null, null);
		}

		//---------------------------------------------------------------------
		// 2. Operational Log Analytics (LOG severity: pgbadger-style insights)
		//---------------------------------------------------------------------
		for (String v : _verdicts)
			w.append("<p><b>Verdict</b>: " + v + "</p>\n");

		if (_logConfigNote != null)
			w.append("<p>" + _logConfigNote + "</p>\n");

		// 2a. Checkpoints
		if (_ckptRstm != null && _ckptRstm.getRowCount() > 0)
		{
			w.append("<br>\n");
			w.append("<h4>Logged Checkpoints (origin: CmErrorLog / log_checkpoints)</h4>\n");
			w.append(getSectionDescriptionHtml(_ckptRstm, true));
			w.append("Row Count: " + _ckptRstm.getRowCount() + "<br>\n");
			w.append(toHtmlTable(_ckptRstm));
		}

		// 2b. Autovacuum & Autoanalyze
		if (_autovacRstm != null && _autovacRstm.getRowCount() > 0)
		{
			w.append("<br>\n");
			w.append("<h4>Logged Autovacuum &amp; Autoanalyze Activity (origin: CmErrorLog / log_autovacuum_min_duration)</h4>\n");
			w.append(getSectionDescriptionHtml(_autovacRstm, true));
			w.append("Row Count: " + _autovacRstm.getRowCount() + "<br>\n");
			w.append(toHtmlTable(_autovacRstm));
		}

		// 2c. Temporary Files
		if (_tempFilesRstm != null && _tempFilesRstm.getRowCount() > 0)
		{
			w.append("<br>\n");
			w.append("<h4>Logged Temporary Files (origin: CmErrorLog / log_temp_files)</h4>\n");
			w.append(getSectionDescriptionHtml(_tempFilesRstm, true));
			w.append("Row Count: " + _tempFilesRstm.getRowCount() + "<br>\n");
			w.append(toHtmlTable(_tempFilesRstm));
		}

		// 2d. Lock Waits
		if (_lockWaitsRstm != null && _lockWaitsRstm.getRowCount() > 0)
		{
			w.append("<br>\n");
			w.append("<h4>Logged Lock Waits (origin: CmErrorLog / log_lock_waits)</h4>\n");
			w.append(getSectionDescriptionHtml(_lockWaitsRstm, true));
			w.append("Row Count: " + _lockWaitsRstm.getRowCount() + "<br>\n");
			w.append(toHtmlTable(_lockWaitsRstm));

			if (isFullMessageType() && _lockWaitRowsRstm != null && _lockWaitRowsRstm.getRowCount() > 0)
			{
				w.append("<br>\n");
				w.append(getSectionDescriptionHtml(_lockWaitRowsRstm, true));
				w.append("Row Count: " + _lockWaitRowsRstm.getRowCount() + "<br>\n");
				w.append(toHtmlTable(_lockWaitRowsRstm));
			}
		}

		// 2e. Slow Statements / auto_explain Plans
		if (_slowQueriesRstm != null && _slowQueriesRstm.getRowCount() > 0)
		{
			w.append("<br>\n");
			w.append("<h4>Logged Slow Statements &amp; auto_explain Plans (origin: CmErrorLog / log_min_duration_statement)</h4>\n");
			w.append(getSectionDescriptionHtml(_slowQueriesRstm, true));
			w.append("Row Count: " + _slowQueriesRstm.getRowCount() + "<br>\n");
			w.append(toHtmlTable(_slowQueriesRstm));
		}
	}

	@Override
	public String getSubject()
	{
		return "Error Log: errors, warnings and operational analytics (origin: CmErrorLog / the server log)";
	}

	@Override
	public boolean hasIssueToReport()
	{
		return false; // even if we found entries, do NOT indicate this as a Problem or Issue (the alarms for the error log does that)
	}

	@Override
	public String getLlmDigest()
	{
		// The SQLSTATE and the counts only: NOT 'example_message' (it can hold data values and SQL text)
		return llmDigestRows(_countRstm, 20, "error_severity", "sql_state_code", "sql_state_code_desc", "message_count", "databases", "first_entry", "last_entry");
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

		//---------------------------------------------------------------------
		// 1. Errors & Warnings: severity + SQLSTATE
		//---------------------------------------------------------------------
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

		//---------------------------------------------------------------------
		// 2. Operational LOG Analytics (pgbadger-style insights)
		//---------------------------------------------------------------------
		try
		{
			PostgresErrorLogBgParser bgParser = new PostgresErrorLogBgParser();
			bgParser.parseFromPcs(conn, getReportPeriodSqlWhere());

			int topAnalyticsRows = Math.min(getTopRows(), 50);
			_ckptRstm         = bgParser.buildCheckpointsRstm();
			_autovacRstm      = bgParser.buildAutovacuumRstm(topAnalyticsRows);
			_tempFilesRstm    = bgParser.buildTempFilesRstm(topAnalyticsRows);
			_lockWaitsRstm    = bgParser.buildLockWaitsSummaryRstm();
			_lockWaitRowsRstm = bgParser.buildLockWaitDetailsRstm(25);
			_slowQueriesRstm  = bgParser.buildSlowQueriesRstm(topAnalyticsRows);

			buildVerdictsAndConfigNotes(conn);
		}
		catch (Exception ex)
		{
			_logger.warn("Problems parsing operational LOG entries from CmErrorLog_abs.", ex);
		}

		//---------------------------------------------------------------------
		// 3. Trend Graphs from CmErrorLog (if recorded)
		//---------------------------------------------------------------------
		String schema = getReportingInstance().getDbmsSchemaName();
		// The counts as stacked bars per 10 minutes (as lines they are a "comb" of small values, where a rare FATAL/ERROR is hidden under the LOG messages)
		// The severities have fixed colors: PANIC purple, FATAL red, ERROR light orange, WARNING yellow, LOG light gray
		// The severity bars are without LOG (LOG has far more messages, the errors would be thin slices at the bottom); LOG is in the Per Second chart
		int barMinutes = 10;
		// The severity bars count the rows in CmErrorLog_abs (by log_time), the same rows as the table above: they always add up the same
		// (the graph counter CmErrorLog_SeverityCount skips the first sample after a collector start, and is by sample time)
		_CmErrorLog_SeverityCount = ! doTableExist(conn, schema, "CmErrorLog_abs") ? null
				: new ReportChartTimeSeriesStackedBar(this, conn, schema, CmErrorLog.CM_NAME, CmErrorLog.GRAPH_NAME_SEVERITY_COUNT, "CmErrorLog_abs", "log_time", "error_severity", "PANIC, FATAL, ERROR, WARNING", barMinutes,
						"ErrorLog Messages by Severity (without LOG), Count per " + barMinutes + " minutes (" + CmErrorLog.SHORT_NAME + ")").setSeriesColors(ReportChartAbstract.SEVERITY_COLORS);
		_CmErrorLog_SeverityPerSec          = chart   (conn, schema, CmErrorLog.CM_NAME, CmErrorLog.GRAPH_NAME_SEVERITY_PER_SEC,                       "ErrorLog Messages by Severity, Per Second (" + CmErrorLog.SHORT_NAME + ")", ReportChartAbstract.SEVERITY_COLORS);
		_CmErrorLog_OperationalEvents       = barChart(conn, schema, CmErrorLog.CM_NAME, CmErrorLog.GRAPH_NAME_OPERATIONAL_EVENTS, barMinutes, null,   "ErrorLog Operational Events, Count per " + barMinutes + " minutes (" + CmErrorLog.SHORT_NAME + ")", null);
		_CmErrorLog_OperationalEventsPerSec = chart   (conn, schema, CmErrorLog.CM_NAME, CmErrorLog.GRAPH_NAME_OPERATIONAL_PER_SEC,            "ErrorLog Operational Events, Per Second (" + CmErrorLog.SHORT_NAME + ")", null);
	}

	/** A line chart, or null when the graph was not recorded */
	private IReportChart chart(DbxConnection conn, String schema, String cmName, String graphName, String title, Map<String, Color> seriesColors)
	{
		if ( ! doTableExist(conn, schema, cmName + "_" + graphName) )
			return null;
		IReportChart chart = createTsLineChart(conn, schema, cmName, graphName, -1, false, null, title);
		if (seriesColors != null && chart instanceof ReportChartAbstract)
			((ReportChartAbstract) chart).setSeriesColors(seriesColors);
		return chart;
	}

	/** A stacked bar chart (each series summed per N minutes, skipSeries left out), or null when the graph was not recorded */
	private IReportChart barChart(DbxConnection conn, String schema, String cmName, String graphName, int minutes, String skipSeries, String title, Map<String, Color> seriesColors)
	{
		if ( ! doTableExist(conn, schema, cmName + "_" + graphName) )
			return null;
		return createTsStackedBarChartFromGraph(conn, schema, cmName, graphName, minutes, skipSeries, title).setSeriesColors(seriesColors);
	}

	private void buildVerdictsAndConfigNotes(DbxConnection conn)
	{
		// Check checkpoint fsync latency spikes
		if (_ckptRstm != null)
		{
			double maxLongestSync = 0;
			for (int r = 0; r < _ckptRstm.getRowCount(); r++)
			{
				Double d = _ckptRstm.getValueAsDouble(r, "longest_sync_sec__max");
				if (d != null)
					maxLongestSync = Math.max(maxLongestSync, d);
			}
			if (maxLongestSync >= 1.0)
			{
				_verdicts.add("A checkpoint file <code>fsync</code> call took up to <b>" + String.format("%.3f", maxLongestSync) + " s</b>. "
						+ "Slow <code>fsync</code> stalls checkpoint completion and can cause commit latency spikes on the underlying storage.");
			}
		}

		// Check autovacuum wraparound or unremovable dead tuples
		if (_autovacRstm != null)
		{
			int    wrapSum          = 0;
			long   maxDeadUnrem     = 0;
			String maxDeadUnremTab  = null;
			for (int r = 0; r < _autovacRstm.getRowCount(); r++)
			{
				wrapSum += _autovacRstm.getValueAsInteger(r, "wraparound_count", true, 0);
				long deadUnrem = _autovacRstm.getValueAsLong(r, "dead_not_removable__max", true, 0L);
				if (deadUnrem > maxDeadUnrem)
				{
					maxDeadUnrem    = deadUnrem;
					maxDeadUnremTab = _autovacRstm.getValueAsString(r, "table_name");
				}
			}
			if (wrapSum > 0)
			{
				_verdicts.add("<b>" + wrapSum + " anti-wraparound autovacuum(s)</b> ran during the period. "
						+ "Anti-wraparound vacuums cannot be canceled by lock conflicts; see 'Vacuum Health' for transaction ID ages.");
			}
			if (maxDeadUnrem >= 100_000 && maxDeadUnremTab != null)
			{
				_verdicts.add("Autovacuum on <code>" + maxDeadUnremTab + "</code> found <b>" + String.format("%,d", maxDeadUnrem)
						+ " dead tuples that could not yet be removed</b> because an older transaction snapshot (oldest xmin) was still open.");
			}
		}

		// Check logging configuration settings if MonSessionDbmsConfig was recorded
		List<String> offTips = new ArrayList<>();
		String logCkpt   = getServerSetting(conn, "log_checkpoints");
		String logLock   = getServerSetting(conn, "log_lock_waits");
		String logTemp   = getServerSetting(conn, "log_temp_files");
		String logVacMin = getServerSetting(conn, "log_autovacuum_min_duration");

		if ("off".equalsIgnoreCase(logCkpt))
			offTips.add("<code>log_checkpoints = on</code> (logs checkpoint buffer writes and sync latency)");
		if ("off".equalsIgnoreCase(logLock))
			offTips.add("<code>log_lock_waits = on</code> (logs lock waits exceeding <code>deadlock_timeout</code>)");
		if ("-1".equals(logTemp))
			offTips.add("<code>log_temp_files = 0</code> (logs queries that spill temporary files to disk)");
		if ("-1".equals(logVacMin))
			offTips.add("<code>log_autovacuum_min_duration = '1s'</code> (or <code>0</code>, logs autovacuum and autoanalyze runs)");

		if (!offTips.isEmpty())
		{
			_logConfigNote = "<b>Tip (Server Log Observability)</b>: To capture more operational insights in this section, consider enabling: "
					+ String.join(", ", offTips) + ".";
		}
	}

	// Package-private getters for unit testing
	ResultSetTableModel getCountRstm()        { return _countRstm; }
	ResultSetTableModel getCheckpointsRstm()  { return _ckptRstm; }
	ResultSetTableModel getAutovacuumRstm()   { return _autovacRstm; }
	ResultSetTableModel getTempFilesRstm()    { return _tempFilesRstm; }
	ResultSetTableModel getLockWaitsRstm()    { return _lockWaitsRstm; }
	ResultSetTableModel getLockWaitRowsRstm() { return _lockWaitRowsRstm; }
	ResultSetTableModel getSlowQueriesRstm()  { return _slowQueriesRstm; }
	List<String>        getVerdicts()         { return _verdicts; }
}
