/*******************************************************************************
 * Copyright (C) 2010-2027 Goran Schwarz
 *
 * This file is part of DbxTune
 * DbxTune is a family of sub-products *Alarm-Coverage*, *## Batch-Coverage*
 *                                      *Error-Tracking* and *Report-Handling*
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
package com.dbxtune.mgt.controllers;

import java.io.PrintWriter;
import java.lang.invoke.MethodHandles;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.servlet.http.HttpServletResponse;

import org.apache.commons.text.StringEscapeUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.dbxtune.CounterController;
import com.dbxtune.Version;
import com.dbxtune.central.controllers.DbxCentralPageTemplate;
import com.dbxtune.central.controllers.DbxTimelineRows;
import com.dbxtune.central.controllers.HtmlStatic.PageSection;
import com.dbxtune.pcs.OracleJobSchedulerExtractor;
import com.dbxtune.sql.conn.DbxConnection;
import com.dbxtune.utils.Configuration;
import com.dbxtune.utils.StringUtil;
import com.dbxtune.utils.TimeUtils;

/**
 * "Scheduler Timeline": the runs of the Oracle Scheduler jobs as bars on a time line, one row for each job.
 * <p>
 * The log of the Scheduler is read from the database when the page is asked for (with a connection of its own), and drawn in the
 * browser by /scripts/dbxtune/js/dbxTimeline.js. It is the Oracle version of SqlServerJobSchedulerTimeline, and much smaller: a run
 * in Oracle has no steps (the steps of a CHAIN are nested rows under their job).
 * <p>
 * The colors: a run that failed is red, one that was stopped orange, one that took at least 2 times as long as the job normally does
 * (the average of its runs in the last 30 days) is pink, a job that is running now is green.
 */
public class OracleJobSchedulerTimeline
extends DbxCentralPageTemplate
{
	private static final long serialVersionUID = 1L;
	private static final Logger _logger = LogManager.getLogger(MethodHandles.lookup().lookupClass());

	// The colors of the bars (also in the legend below the chart)
	private static final String COLOR_OK       = "lightblue";
	private static final String COLOR_AUTOTASK = "#b8c4d6";
	private static final String COLOR_STEP     = "#6ea8fe";
	private static final String COLOR_FAILED   = "red";
	private static final String COLOR_STOPPED  = "orange";
	private static final String COLOR_SLOW     = "#FFC0CB";
	private static final String COLOR_RUNNING  = "lightgreen";

	// A run is "slower than normal" when it took at least this many seconds, and at least this many times its normal time (of at least this many earlier runs)
	private static final int SLOW_MIN_SECONDS   = 60;
	private static final int SLOW_TIMES_NORMAL  = 2;
	private static final int SLOW_MIN_HIST_RUNS = 3;

	private DbxConnection _conn = null;

	@Override
	public String getHeadTitle()
	{
		return "Oracle Job Scheduler Activity";
	}

	@Override
	public PageSection getPageSection()
	{
		return PageSection.None;
	}

	@Override
	public int getHeadRefreshTime()
	{
		return getUrlParameterInt_defaultFromDesc("refresh");
	}

	@Override
	public void craeteHtmlHeadCssPre(PrintWriter writer)
	{
		writer.println("<style type='text/css'>");
		writer.println("    html, body      { height: 100%; }");
		writer.println("    .grow-to-bottom { height: 85vh; }");
		writer.println("</style>");
	}

	@Override
	protected List<String> getJavaScriptList()
	{
		// Timeline chart: vis-timeline + dbxTimeline.js
		return new ArrayList<>(DbxTimelineRows.JAVASCRIPT_LIST);
	}

	@Override
	protected List<String> getCssList()
	{
		return new ArrayList<>(DbxTimelineRows.CSS_LIST);
	}

	//-----------------------------------------------------------------------
	//-- Parameters
	//-----------------------------------------------------------------------
	@Override
	public Set<UrlParameterDescription> createUrlParameterDescription()
	{
		LinkedHashSet<UrlParameterDescription> set = new LinkedHashSet<>();

		set.add(new UrlParameterDescription(
				"startTime",
				"From when. One of: <code>TODAY</code> (today at 00:00), <code>WEEK</code> (Monday at 00:00), <code>-4h</code> / <code>-2d</code> (that long back from now), or a date with optional time: <code>2026-10-13</code>, <code>2026-10-13 08:00</code><br>"
						+ "<b>Default</b>: <code>TODAY</code>",
				"TODAY",
				String.class));

		set.add(new UrlParameterDescription(
				"endTime",
				"Until when. One of: <code>NOW</code>, <code>8h</code> / <code>1d</code> (that long after the startTime), or a date with optional time<br>"
						+ "<b>Default</b>: now",
				null,
				String.class));

		set.add(new UrlParameterDescription(
				"minDurationInSeconds",
				"Leave out the runs that took less than this many seconds (a run that did not succeed is always shown). Oracle saves how long a run took in whole seconds, so a quick job is 0.<br>"
						+ "<b>Default</b>: <code>0</code> (all runs)",
				0,
				Integer.class));

		set.add(new UrlParameterDescription(
				"showAutotasks",
				"Show the automatic maintenance tasks of Oracle (optimizer statistics, space advisor, SQL tuning advisor), which run in the maintenance windows.<br>"
						+ "<b>Default</b>: <code>true</code>",
				true,
				Boolean.class));

		set.add(new UrlParameterDescription(
				"filter",
				"Text for the filter box above the chart: only the rows where the job name (or the text on a bar) has this text are shown.<br>"
						+ "<b>Default</b>: <i>none</i>",
				null,
				String.class));

		set.add(new UrlParameterDescription(
				"refresh",
				"Load the page again every this many seconds.<br>"
						+ "<b>Default</b>: <code>0</code> (never)",
				0,
				Integer.class));

		return set;
	}

	@Override
	public void checkUrlParameters()
	throws Exception
	{
		String startTimeStr = getUrlParameter_defaultFromDesc("startTime");
		String endTimeStr   = getUrlParameter_defaultFromDesc("endTime");

		Timestamp startTs = TimeUtils.parseStartTime(StringUtil.hasValue(startTimeStr) ? startTimeStr : "TODAY");
		Timestamp endTs   = TimeUtils.parseEndTime(endTimeStr, startTs);
		if (endTs == null)
			endTs = new Timestamp(System.currentTimeMillis() + 60 * 1000); // now, plus one minute

		setUrlParameter("startTime", startTs);
		setUrlParameter("endTime"  , endTs);
	}

	//-----------------------------------------------------------------------
	//-- Open/Close
	//-----------------------------------------------------------------------
	@Override
	protected void open()
	throws SendResponseErrorException
	{
		if ( ! CounterController.hasInstance() )
			throw new SendResponseErrorException(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "No Counter Controller was found.");

		try
		{
			// A connection of our own: the collector goes on sampling with its connection meanwhile
			_conn = CounterController.getInstance().cloneMonConnection(Version.getAppName() + "-JobSchedulerActivity");
		}
		catch (Exception ex)
		{
			String msg = "Problems connecting to the Oracle database. Caught: " + ex;
			_logger.error(msg, ex);
			throw new SendResponseErrorException(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, msg);
		}
	}

	@Override
	protected void close()
	{
		if (_conn != null)
			_conn.closeNoThrow();
	}

	//-----------------------------------------------------------------------
	//-- Page BODY
	//-----------------------------------------------------------------------
	@Override
	public void createHtmlBodyContent(PrintWriter writer)
	{
		Timestamp startTime = getUrlParameterTs("startTime", null);
		Timestamp endTime   = getUrlParameterTs("endTime"  , null);

		// Buttons to the day before, today and the day after. They change 'startTime' and 'endTime' in the URL of the BROWSER
		// (which is the one of DbxCentral, with its own parameters: this page is fetched through it), and keep the rest of it
		SimpleDateFormat ymd = new SimpleDateFormat("yyyy-MM-dd");
		String prevDay = ymd.format(new Timestamp(startTime.getTime() - 24 * 3600 * 1000L));
		String nextDay = ymd.format(new Timestamp(startTime.getTime() + 24 * 3600 * 1000L));

		writer.println("<script>");
		writer.println("    function oraJobTimelineGo(startTime, endTime)");
		writer.println("    {");
		writer.println("        var p = new URLSearchParams(window.location.search);");
		writer.println("        p.set('startTime', startTime);");
		writer.println("        if (endTime) p.set('endTime', endTime); else p.delete('endTime');");
		writer.println("        window.location.search = p.toString();");
		writer.println("    }");
		writer.println("</script>");
		writer.println();
		writer.println("<div class='container-fluid'>");
		writer.println();
		writer.println("<div style='margin: 4px 0;'>");
		writer.println("    <b>" + StringEscapeUtils.escapeHtml4(CounterController.getInstance().getServerName()) + "</b>: jobs of the Oracle Scheduler from <b>" + TimeUtils.toStringYmdHm(startTime.getTime()) + "</b> to <b>" + TimeUtils.toStringYmdHm(endTime.getTime()) + "</b> &nbsp; ");
		writer.println("    <button type='button' class='btn btn-sm btn-outline-secondary' onclick=\"oraJobTimelineGo('" + prevDay + "', '1d')\">&laquo; " + prevDay + "</button>");
		writer.println("    <button type='button' class='btn btn-sm btn-outline-secondary' onclick=\"oraJobTimelineGo('TODAY', null)\">Today</button>");
		writer.println("    <button type='button' class='btn btn-sm btn-outline-secondary' onclick=\"oraJobTimelineGo('" + nextDay + "', '1d')\">" + nextDay + " &raquo;</button>");
		writer.println("</div>");
		writer.println();
		writer.println("<details>");
		writer.println("    <summary>Show Parameters and what the colors mean. Page loaded: " + TimeUtils.toStringYmdHms(System.currentTimeMillis()) + "</summary>");
		writer.println("    One row for each job, one bar for each run. Hover over a bar to see the run: status, how long it took and how long the job normally takes, the error message.<br>");
		writer.println("    The steps of a job that is a CHAIN are rows of their own under the job: click the name of the job to show them.<br>");
		writer.println("    The times are the clock of the collector (the same as the database server has, when they are in the same time zone). Oracle keeps the log for 30 days by default.<br>");
		writer.println("    <br>");
		writer.println("    Below are descriptions of URL Parameters for this page");
		writer.println(getParameterDescriptionHtmlTable());
		writer.println("</details>");
		writer.println();
		writer.println("<div id='dbx-job-scheduler-no-data' style='color: orange; display: none;'>No job ran during the period.</div>");
		writer.println("<div id='dbx-servlet-exception' style='color: red; display: none;'></div>");
		writer.println("<div id='timeline' class='grow-to-bottom'></div>");
		writer.println();
		writer.println("</div>");

		try
		{
			createJsTimeline(writer, startTime, endTime);
		}
		catch (Exception ex)
		{
			_logger.error("Problems creating the Oracle Job Scheduler Timeline.", ex);

			writer.println("<script>");
			writer.println("    DbxTimeline.progressDone();");
			writer.println("    document.getElementById('dbx-servlet-exception').style.display = 'block';");
			writer.println("    document.getElementById('dbx-servlet-exception').innerHTML = " + toJs("Problems reading the log of the Oracle Scheduler:<br><pre>" + StringEscapeUtils.escapeHtml4(ex.toString()) + "</pre>") + ";");
			writer.println("</script>");
		}
	}

	private static String toJs(Object obj)
	throws RuntimeException
	{
		try { return DbxTimelineRows.toScriptJson(obj); }
		catch (Exception ex) { throw new RuntimeException(ex); }
	}

	/** One row of a tooltip (nothing when there is no value) */
	private static String tt(String name, Object value)
	{
		if (value == null || value.toString().isEmpty())
			return "";
		return "<tr><td style='padding: 0 8px 0 4px;' nowrap><b>" + name + "</b></td><td style='padding: 0 4px 0 0;'>" + StringEscapeUtils.escapeHtml4(value.toString()) + "</td></tr>";
	}

	/** Read the log of the Scheduler, and write the JavaScript that draws it */
	private void createJsTimeline(PrintWriter writer, Timestamp startTime, Timestamp endTime)
	throws Exception
	{
		int     minDurationInSeconds = getUrlParameterInt_defaultFromDesc("minDurationInSeconds");
		boolean showAutotasks        = getUrlParameterBoolean_defaultFromDesc("showAutotasks");

		DbxConnection conn = _conn;

		// A long period (months) takes time: say that we are working, and send the start of the page to the browser before we read
		DbxTimelineRows.writeProgressStart(writer, "Reading the log of the Oracle Scheduler from the database...");

		boolean cdb       = OracleJobSchedulerExtractor.canReadCdbViews(conn);
		boolean hasErrors = OracleJobSchedulerExtractor.hasErrorsColumn(conn);

		SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
		String startDate = "TO_DATE('" + sdf.format(startTime) + "', 'YYYY-MM-DD HH24:MI:SS')";
		String endDate   = "TO_DATE('" + sdf.format(endTime)   + "', 'YYYY-MM-DD HH24:MI:SS')";

		// How many days of the log that has to be read to get the period (a run is in the log from when it ended)
		int daysBack = (int) Math.max(1, (System.currentTimeMillis() - startTime.getTime()) / (24 * 3600 * 1000L) + 1);
		int histDays = Math.max(daysBack, Configuration.getCombinedConfiguration().getIntProperty(OracleJobSchedulerExtractor.PROPKEY_daysToCopy, OracleJobSchedulerExtractor.DEFAULT_daysToCopy));

		//-----------------------------------------------------------------
		// What every job normally does: its runs that succeeded BEFORE the period. Key: CON_ID|OWNER|JOB_NAME, value: {runs, avg seconds, max seconds}
		//-----------------------------------------------------------------
		Map<String, long[]> normal = new HashMap<>();
		String sql = ""
			+ "select h.CON_ID, h.OWNER, h.JOB_NAME, count(*), avg(h.DURATION_SEC), max(h.DURATION_SEC) \n"
			+ "from (" + OracleJobSchedulerExtractor.getJobRunsSql(cdb, hasErrors, histDays) + ") h \n"
			+ "where h.JOB_SUBNAME is null and h.STATUS = 'SUCCEEDED' and h.START_TIME < " + startDate + " \n"
			+ "group by h.CON_ID, h.OWNER, h.JOB_NAME";
		try (Statement stmnt = conn.createStatement(); ResultSet rs = stmnt.executeQuery(sql))
		{
			while (rs.next())
				normal.put(rs.getLong(1) + "|" + rs.getString(2) + "|" + rs.getString(3), new long[] { rs.getLong(4), Math.round(rs.getDouble(5)), rs.getLong(6) });
		}

		List<Map<String, Object>> rows = new ArrayList<>();
		Set<String> jobKeys = new LinkedHashSet<>();
		int failedCount = 0;

		//-----------------------------------------------------------------
		// The runs of the period
		//-----------------------------------------------------------------
		sql = ""
			+ "select r.* \n"
			+ "from (" + OracleJobSchedulerExtractor.getJobRunsSql(cdb, hasErrors, daysBack) + ") r \n"
			+ "where r.END_TIME >= " + startDate + " and r.START_TIME <= " + endDate + " \n"
			+ "order by r.START_TIME, r.LOG_ID";
		DbxTimelineRows.writeProgressText(writer, "Reading the runs of the period...");
		try (Statement stmnt = conn.createStatement(); ResultSet rs = stmnt.executeQuery(sql))
		{
			int readCount = 0;
			while (rs.next())
			{
				// How far we are, now and then (the read is fast, so it is only seen when there are many thousands of runs)
				if (++readCount % 5000 == 0)
					DbxTimelineRows.writeProgressText(writer, "Reading the runs of the period: " + readCount + " so far...");

				String    conName  = rs.getString   ("CON_NAME");
				String    jobKind  = rs.getString   ("JOB_KIND");
				String    owner    = rs.getString   ("OWNER");
				String    jobName  = rs.getString   ("JOB_NAME");
				String    subName  = rs.getString   ("JOB_SUBNAME");
				String    status   = rs.getString   ("STATUS");
				Timestamp startTs  = rs.getTimestamp("START_TIME");
				Timestamp endTs    = rs.getTimestamp("END_TIME");
				long      seconds  = rs.getLong     ("DURATION_SEC");
				String    errorNum = rs.getString   ("ERROR_NUM");
				String    errors   = StringUtil.hasValue(rs.getString("ERRORS")) ? rs.getString("ERRORS") : rs.getString("RUN_INFO");

				boolean succeeded = "SUCCEEDED".equals(status);
				boolean isStep    = StringUtil.hasValue(subName);

				if ("AUTOTASK".equals(jobKind) && ! showAutotasks)
					continue;
				if (succeeded && seconds < minDurationInSeconds)
					continue;

				long[]  hist   = normal.get(rs.getLong("CON_ID") + "|" + owner + "|" + jobName);
				boolean isSlow = ! isStep && hist != null && hist[0] >= SLOW_MIN_HIST_RUNS && seconds >= SLOW_MIN_SECONDS && seconds >= SLOW_TIMES_NORMAL * hist[1];

				String color = COLOR_OK;
				if      ( ! succeeded && "STOPPED".equals(status) ) color = COLOR_STOPPED;
				else if ( ! succeeded )                             color = COLOR_FAILED;
				else if (isSlow)                                    color = COLOR_SLOW;
				else if (isStep)                                    color = COLOR_STEP;
				else if ("AUTOTASK".equals(jobKind))                color = COLOR_AUTOTASK;

				String jobKey   = (StringUtil.hasValue(conName) ? conName + "." : "") + owner + "." + jobName;
				String duration = TimeUtils.secToTimeStrHMS(seconds);
				String barText  = duration + (succeeded ? "" : " " + status) + (isSlow ? " (normally " + TimeUtils.secToTimeStrHMS(hist[1]) + ")" : "");

				String tooltip = "<table>"
					+ tt("Job"           , jobKey + (isStep ? "  (step: " + subName + ")" : ""))
					+ tt("Status"        , status + ("0".equals(errorNum) || errorNum == null ? "" : ", ORA-" + errorNum))
					+ tt("Started"       , TimeUtils.toStringYmdHms(startTs))
					+ tt("Ended"         , TimeUtils.toStringYmdHms(endTs))
					+ tt("Duration"      , duration)
					+ tt("Normal"        , hist == null || isStep ? null : TimeUtils.secToTimeStrHMS(hist[1]) + " (the average of " + hist[0] + " runs before the period, the longest: " + TimeUtils.secToTimeStrHMS(hist[2]) + ")")
					+ tt("CPU seconds"   , rs.getString("CPU_SEC"))
					+ tt("Started late"  , rs.getLong("START_DELAY_SEC") >= 60 ? TimeUtils.secToTimeStrHMS(rs.getLong("START_DELAY_SEC")) + " after it was scheduled to start" : null)
					+ tt("Session"       , rs.getString("SESSION_ID"))
					+ tt("Kind"          , "AUTOTASK".equals(jobKind) ? "automatic maintenance task (logged as " + rs.getString("LOG_JOB_NAME") + ")" : null)
					+ tt("Message"       , succeeded ? null : errors)
					+ "</table>";

				// A bar needs some width to be seen (and Oracle only has whole seconds): a run of 0 seconds is drawn as one second
				if (endTs.getTime() - startTs.getTime() < 1000)
					endTs = new Timestamp(startTs.getTime() + 1000);

				Map<String, Object> row = DbxTimelineRows.createRow(isStep ? jobKey + ":" + subName : jobKey, barText, color, tooltip, startTs, endTs);
				row.put("label", isStep ? subName : jobKey);
				if (isStep)
					row.put("parentKey", jobKey);
				if ( ! succeeded )
				{
					row.put("failed", true);
					failedCount++;
				}
				rows.add(row);
				jobKeys.add(jobKey);
			}
		}

		//-----------------------------------------------------------------
		// The jobs that are running now (a run is only in the log when it has ended)
		//-----------------------------------------------------------------
		if (endTime.getTime() >= System.currentTimeMillis() - 60 * 1000)
		{
			sql = "select r.* from (" + OracleJobSchedulerExtractor.getRunningJobsSql(cdb) + ") r order by r.START_TIME";
			try (Statement stmnt = conn.createStatement(); ResultSet rs = stmnt.executeQuery(sql))
			{
				while (rs.next())
				{
					String    conName = rs.getString   ("CON_NAME");
					String    owner   = rs.getString   ("OWNER");
					String    jobName = rs.getString   ("JOB_NAME");
					String    subName = rs.getString   ("JOB_SUBNAME");
					Timestamp startTs = rs.getTimestamp("START_TIME");
					long      seconds = rs.getLong     ("ELAPSED_SEC");
					boolean   isStep  = StringUtil.hasValue(subName);

					long[] hist    = normal.get(rs.getLong("CON_ID") + "|" + owner + "|" + jobName);
					String jobKey  = (StringUtil.hasValue(conName) ? conName + "." : "") + owner + "." + jobName;
					String barText = "RUNNING " + TimeUtils.secToTimeStrHMS(seconds) + (hist == null || isStep ? "" : " (normally " + TimeUtils.secToTimeStrHMS(hist[1]) + ")");

					String tooltip = "<table>"
						+ tt("Job"        , jobKey + (isStep ? "  (step: " + subName + ")" : ""))
						+ tt("Status"     , "RUNNING")
						+ tt("Started"    , TimeUtils.toStringYmdHms(startTs))
						+ tt("Elapsed"    , TimeUtils.secToTimeStrHMS(seconds))
						+ tt("Normal"     , hist == null || isStep ? null : TimeUtils.secToTimeStrHMS(hist[1]) + " (the average of " + hist[0] + " runs before the period, the longest: " + TimeUtils.secToTimeStrHMS(hist[2]) + ")")
						+ tt("CPU seconds", rs.getString("CPU_SEC"))
						+ tt("Session"    , rs.getString("SESSION_ID"))
						+ "</table>";

					Map<String, Object> row = DbxTimelineRows.createRow(isStep ? jobKey + ":" + subName : jobKey, barText, COLOR_RUNNING, tooltip, startTs, new Timestamp(System.currentTimeMillis()));
					row.put("label", isStep ? subName : jobKey);
					if (isStep)
						row.put("parentKey", jobKey);
					rows.add(row);
					jobKeys.add(jobKey);
				}
			}
		}

		writer.println("<script>");
		writer.println("    // One bar for each run, see /scripts/dbxtune/js/dbxTimeline.js");
		writer.println("    const _dbxTimelineRows = " + toJs(rows) + ";");
		writer.println();
		writer.println("    // createLater: it says \"Drawing N bars...\" while it draws (many thousands of bars take some seconds)");
		writer.println("    DbxTimeline.createLater('timeline', _dbxTimelineRows, {");
		writer.println("        start:         " + DbxTimelineRows.toJsTs(startTime) + ",");
		writer.println("        end:           " + DbxTimelineRows.toJsTs(endTime)   + ",");
		writer.println("        startExpanded: false,");
		writer.println("        showKeys:      true,");
		writer.println("        filter:        " + toJs(StringUtil.nullToValue(getUrlParameter("filter"), "")) + ",");
		writer.println("        legend: [");
		writer.println("            { color: '" + COLOR_OK       + "', text: 'Succeeded' },");
		writer.println("            { color: '" + COLOR_AUTOTASK + "', text: 'Automatic maintenance task' },");
		writer.println("            { color: '" + COLOR_STEP     + "', text: 'Step of a chain' },");
		writer.println("            { color: '" + COLOR_FAILED   + "', text: 'Failed' },");
		writer.println("            { color: '" + COLOR_STOPPED  + "', text: 'Stopped' },");
		writer.println("            { color: '" + COLOR_SLOW     + "', text: 'Slower than normal (at least " + SLOW_TIMES_NORMAL + " times)' },");
		writer.println("            { color: '" + COLOR_RUNNING  + "', text: 'Running now' }");
		writer.println("        ],");
		writer.println("        labelHeader:   'Job (" + jobKeys.size() + "), runs: " + rows.size() + (failedCount > 0 ? ", not succeeded: " + failedCount : "") + "',");
		writer.println("        expandText:    'Show the steps of the chain',");
		writer.println("        collapseText:  'Hide the steps of the chain'");
		writer.println("    });");
		if (rows.isEmpty())
			writer.println("    document.getElementById('dbx-job-scheduler-no-data').style.display = 'block';");
		writer.println("</script>");
	}
}
