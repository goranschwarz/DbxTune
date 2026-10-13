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
package com.dbxtune.pcs.report.content.oracle;

import java.io.IOException;
import java.io.Writer;
import java.sql.Timestamp;
import java.text.SimpleDateFormat;

import com.dbxtune.gui.ResultSetTableModel;
import com.dbxtune.pcs.OracleJobSchedulerExtractor;
import com.dbxtune.pcs.report.DailySummaryReportAbstract;
import com.dbxtune.sql.conn.DbxConnection;
import com.dbxtune.utils.Configuration;

/**
 * The jobs of the Oracle Scheduler during the report period: what ran (and for how long), what failed, what took longer than
 * it normally does, which jobs are in a state where they do not run, and what was still running.
 * <p>
 * It is made of the log of the Scheduler, which OracleJobSchedulerExtractor copies into the recording (when the recording database
 * is rolled over at midnight, or when the "Scheduler Report for Today" is asked for). The days BEFORE the report period are what
 * "normal" is: the columns that start with <code>hist_</code>.
 * <p>
 * A recording without that copy (an older one, or the copy failed) gets the section it had before: OracleJobFailures.
 */
public class OracleJobScheduler
extends OracleAbstract
{
	private static final String SCHEMA = OracleJobSchedulerExtractor.PCS_SCHEMA_NAME;

	// A run is "slower than normal" when it took at least this many seconds, and at least this many times its normal time (of at least this many earlier runs)
	private static final int SLOW_MIN_SECONDS   = 60;
	private static final int SLOW_TIMES_NORMAL  = 2;
	private static final int SLOW_MIN_HIST_RUNS = 3;

	private ResultSetTableModel _overviewRstm;
	private ResultSetTableModel _failedRstm;
	private ResultSetTableModel _slowRstm;
	private ResultSetTableModel _stateRstm;
	private ResultSetTableModel _runningRstm;

	private String _extractInfo = "";
	private String _timelineUrl = "";

	/** When the log of the Scheduler is not in the recording: the section that is made of what the collector sampled */
	private OracleJobFailures _fallback;

	public OracleJobScheduler(DailySummaryReportAbstract reportingInstance)
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
	public String getSubject()
	{
		return "Job Scheduler: what ran, what failed, what took longer than normal (origin: DBA_SCHEDULER_JOB_RUN_DETAILS, DBA_SCHEDULER_JOBS)";
	}

	@Override
	public boolean hasIssueToReport()
	{
		return false; // even if we found entries, do NOT indicate this as a Problem or Issue (the alarms for the jobs does that)
	}

	@Override
	public void writeMessageText(Writer w, MessageType messageType)
	throws IOException
	{
		if (_fallback != null)
		{
			w.append("<i>The log of the Oracle Scheduler was not copied into this recording, so this is only what the collector saw when it sampled (every 5 minutes).</i><br>\n");
			_fallback.writeMessageText(w, messageType);
			return;
		}

		if (_overviewRstm == null)
		{
			w.append("<b>The jobs was not recorded.</b><br>\n");
			return;
		}

		w.append(_extractInfo);
		w.append("<a href='" + _timelineUrl + "' target='_blank'>Open the <i>Timeline</i> view</a> of the period (it is read from the database when you open it, so it works as long as Oracle keeps the log: 30 days by default).<br>\n");
		w.append("<br>\n");

		// What failed
		w.append("<b>Runs that did not succeed</b><br>\n");
		if (_failedRstm.getRowCount() == 0)
		{
			w.append("No job run failed (or was stopped) during the period.<br><br>\n");
		}
		else
		{
			w.append(getSectionDescriptionHtml(_failedRstm, true));
			w.append("Row Count: " + _failedRstm.getRowCount() + "<br>\n");
			w.append(toHtmlTable(_failedRstm));
		}

		// What took longer than normal
		w.append("<b>Runs that took longer than normal</b><br>\n");
		if (_slowRstm.getRowCount() == 0)
		{
			w.append("No run took more than " + SLOW_TIMES_NORMAL + " times as long as the job normally does (runs shorter than " + SLOW_MIN_SECONDS + " seconds are not looked at).<br><br>\n");
		}
		else
		{
			w.append(getSectionDescriptionHtml(_slowRstm, true));
			w.append("Row Count: " + _slowRstm.getRowCount() + "<br>\n");
			w.append(toHtmlTable(_slowRstm));
		}

		// Still running when the log was read
		if (_runningRstm.getRowCount() > 0)
		{
			w.append("<b>Jobs that were running when the log was read</b><br>\n");
			w.append(getSectionDescriptionHtml(_runningRstm, true));
			w.append(toHtmlTable(_runningRstm));
		}

		// Jobs that do not run
		if (_stateRstm.getRowCount() > 0)
		{
			w.append("<b>Jobs in a state where they do not run</b><br>\n");
			w.append(getSectionDescriptionHtml(_stateRstm, true));
			w.append(toHtmlTable(_stateRstm));
		}

		// Every job that ran. In the short message (the mail) only how many: the table can be long
		w.append("<b>All jobs that ran during the period</b><br>\n");
		if (_overviewRstm.getRowCount() == 0)
		{
			w.append("No job ran during the period.<br>\n");
		}
		else if (MessageType.FULL_MESSAGE.equals(messageType))
		{
			w.append(getSectionDescriptionHtml(_overviewRstm, true));
			w.append("Row Count: " + _overviewRstm.getRowCount() + "<br>\n");
			w.append(toHtmlTable(_overviewRstm));
		}
		else
		{
			w.append(_overviewRstm.getRowCount() + " jobs ran during the period. The table with all of them is in the full report.<br>\n");
		}
	}

	/** Seconds as HH:MM:SS, in the SQL of the recording database (H2) */
	private static String hms(String secExpr)
	{
		String s = "cast(" + secExpr + " as bigint)";
		return "cast(lpad(cast(" + s + " / 3600 as varchar(10)), 2, '0') || ':' || lpad(cast(mod(" + s + " / 60, 60) as varchar(2)), 2, '0') || ':' || lpad(cast(mod(" + s + ", 60) as varchar(2)), 2, '0') as varchar(20))";
	}

	@Override
	public void create(DbxConnection conn, String srvName, Configuration pcsSavedConf, Configuration localConf)
	{
		// The log of the Scheduler is not in the recording: show what the collector sampled instead
		if ( ! doTableExist(conn, SCHEMA, OracleJobSchedulerExtractor.TABLE_job_runs) || ! doTableExist(conn, SCHEMA, OracleJobSchedulerExtractor.TABLE_jobs) )
		{
			_fallback = new OracleJobFailures(getReportingInstance());
			_fallback.create(conn, srvName, pcsSavedConf, localConf);
			return;
		}

		String runs    = "[" + SCHEMA + "].[" + OracleJobSchedulerExtractor.TABLE_job_runs     + "]";
		String jobs    = "[" + SCHEMA + "].[" + OracleJobSchedulerExtractor.TABLE_jobs         + "]";
		String running = "[" + SCHEMA + "].[" + OracleJobSchedulerExtractor.TABLE_running_jobs + "]";
		String info    = "[" + SCHEMA + "].[" + OracleJobSchedulerExtractor.TABLE_extract_info + "]";

		//---------------------------------------------------------------
		// The period. A report of "the recording" (no period was asked for) starts at midnight of the day the recording
		// started: a job that ran at 02:00 is a job of that day, also if the collector was started at 08:00
		//---------------------------------------------------------------
		Timestamp beginTs = getReportingInstance().getReportPeriodOrRecordingBeginTime();
		Timestamp endTs   = getReportingInstance().getReportPeriodOrRecordingEndTime();
		if (beginTs == null) beginTs = new Timestamp(System.currentTimeMillis());
		if (endTs   == null) endTs   = new Timestamp(System.currentTimeMillis());

		SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
		String begin = hasReportPeriod() ? sdf.format(beginTs) : new SimpleDateFormat("yyyy-MM-dd").format(beginTs) + " 00:00:00";
		String end   = sdf.format(endTs);

		String inPeriod     =   "[START_TIME] >= '" + begin + "' and   [START_TIME] <= '" + end + "'";
		String rInPeriod    = "r.[START_TIME] >= '" + begin + "' and r.[START_TIME] <= '" + end + "'";
		String beforePeriod = "[START_TIME] <  '" + begin + "'";

		// The timeline page of DbxCentral reads the log from the database (through the collector), for the same period
		_timelineUrl = "/api/cc/reports?srvName=" + srvName + "&reportName=oracle-job-scheduler-timeline&startTime=" + begin.substring(0, 16).replace(" ", "%20") + "&endTime=" + end.substring(0, 16).replace(" ", "%20");

		//---------------------------------------------------------------
		// When the log was read, and how much of it
		//---------------------------------------------------------------
		ResultSetTableModel infoRstm = executeQuery(conn, "select [EXTRACT_TIME], [DAYS_COPIED] from " + info, true, "OracleJobScheduler_info");
		if (infoRstm.getRowCount() > 0)
		{
			_extractInfo = "The log of the Oracle Scheduler was read at <b>" + infoRstm.getValueAsString(0, 0) + "</b>, and the last <b>" + infoRstm.getValueAsString(0, 1) + "</b> days of it. "
					+ "The period is from <b>" + begin + "</b> to <b>" + end + "</b>; the days before it are what <i>normal</i> is (the columns that start with <code>hist_</code>).<br>\n"
					+ "The times are the clock of the collector (the same as the database server has, when they are in the same time zone). Oracle saves how long a run took in whole seconds.<br>\n";
		}

		// What a job normally does: its runs that succeeded BEFORE the period
		String hist = ""
			+ "    select [CON_ID], [OWNER], [JOB_NAME] \n"
			+ "          ,count(*)            as [hist_runs] \n"
			+ "          ,avg([DURATION_SEC]) as [hist_avg_sec] \n"
			+ "          ,max([DURATION_SEC]) as [hist_max_sec] \n"
			+ "    from " + runs + " \n"
			+ "    where [JOB_SUBNAME] is null and [STATUS] = 'SUCCEEDED' and " + beforePeriod + " \n"
			+ "    group by [CON_ID], [OWNER], [JOB_NAME] \n";

		//---------------------------------------------------------------
		// Runs that did not succeed
		//---------------------------------------------------------------
		String sql = ""
			+ "select \n"
			+ "     r.[START_TIME] \n"
			+ "    ,r.[CON_NAME] \n"
			+ "    ,r.[OWNER] \n"
			+ "    ,r.[JOB_NAME] \n"
			+ "    ,r.[JOB_SUBNAME] \n"
			+ "    ,r.[STATUS] \n"
			+ "    ,cast(r.[ERROR_NUM] as varchar(10))    as [ERROR_NUM] \n"   // as a string: it is an error number, not something to write as 20 001
			+ "    ," + hms("r.[DURATION_SEC]") + "       as [duration] \n"
			+ "    ,(select count(*) from " + runs + " f where f.[CON_ID] = r.[CON_ID] and f.[OWNER] = r.[OWNER] and f.[JOB_NAME] = r.[JOB_NAME] and f.[STATUS] <> 'SUCCEEDED' and f." + beforePeriod + ") as [hist_failed] \n"
			+ "    ,(select count(*) from " + runs + " f where f.[CON_ID] = r.[CON_ID] and f.[OWNER] = r.[OWNER] and f.[JOB_NAME] = r.[JOB_NAME] and f.[STATUS] =  'SUCCEEDED' and f." + beforePeriod + ") as [hist_succeeded] \n"
			+ "    ,coalesce(r.[ERRORS], r.[RUN_INFO])    as [error_text] \n"
			+ "from " + runs + " r \n"
			+ "where r.[STATUS] <> 'SUCCEEDED' and " + rInPeriod + " \n"
			+ "order by r.[START_TIME] desc \n";

		_failedRstm = executeQuery(conn, sql, true, "OracleJobScheduler_failed", false); // false = do not truncate (the error text)
		_failedRstm.setHighlightSortColumns("START_TIME");
		_failedRstm.setDescription("Every run during the period that ended in another way than SUCCEEDED, the newest first.<br>");
		_failedRstm.setColumnDescription("START_TIME"    , "When the run started");
		_failedRstm.setColumnDescription("CON_NAME"      , "Container (PDB) name. Empty when the database is not a Container Database, or when the collector is connected to a PDB.");
		_failedRstm.setColumnDescription("OWNER"         , "The schema that owns the job");
		_failedRstm.setColumnDescription("JOB_NAME"      , "Name of the job. An automatic maintenance task has the name of the task (without the number Oracle adds for each run)");
		_failedRstm.setColumnDescription("JOB_SUBNAME"   , "For a job that is a chain: the step that failed");
		_failedRstm.setColumnDescription("STATUS"        , "How the run ended: FAILED, or STOPPED (someone stopped it, or it ran into the end of its window or its maximum time)");
		_failedRstm.setColumnDescription("ERROR_NUM"     , "The ORA- error number of the run");
		_failedRstm.setColumnDescription("duration"      , "How long the run took (HH:MM:SS)");
		_failedRstm.setColumnDescription("hist_failed"   , "How many runs of the job that did not succeed in the days before the period: 0 = this is new");
		_failedRstm.setColumnDescription("hist_succeeded", "How many runs of the job that succeeded in the days before the period");
		_failedRstm.setColumnDescription("error_text"    , "What the Scheduler wrote about the run: the error message");

		//---------------------------------------------------------------
		// Runs that took longer than normal
		//---------------------------------------------------------------
		sql = ""
			+ "select \n"
			+ "     r.[START_TIME] \n"
			+ "    ,r.[CON_NAME] \n"
			+ "    ,r.[OWNER] \n"
			+ "    ,r.[JOB_NAME] \n"
			+ "    ,r.[STATUS] \n"
			+ "    ," + hms("r.[DURATION_SEC]") + "                                  as [duration] \n"
			+ "    ," + hms("h.[hist_avg_sec]") + "                                  as [hist_avg_duration] \n"
			+ "    ,cast(r.[DURATION_SEC] * 1.0 / h.[hist_avg_sec] as decimal(12,1)) as [times_normal] \n"
			+ "    ," + hms("h.[hist_max_sec]") + "                                  as [hist_max_duration] \n"
			+ "    ,h.[hist_runs] \n"
			+ "from " + runs + " r \n"
			+ "join ( \n" + hist + ") h on h.[CON_ID] = r.[CON_ID] and h.[OWNER] = r.[OWNER] and h.[JOB_NAME] = r.[JOB_NAME] \n"
			+ "where r.[JOB_SUBNAME] is null and " + rInPeriod + " \n"
			+ "  and r.[DURATION_SEC] >= " + SLOW_MIN_SECONDS + " \n"
			+ "  and h.[hist_runs]    >= " + SLOW_MIN_HIST_RUNS + " \n"
			+ "  and r.[DURATION_SEC] >= " + SLOW_TIMES_NORMAL + " * h.[hist_avg_sec] \n"
			+ "order by r.[DURATION_SEC] * 1.0 / (h.[hist_avg_sec] + 1) desc \n";

		_slowRstm = executeQuery(conn, sql, true, "OracleJobScheduler_slow");
		_slowRstm.setHighlightSortColumns("times_normal");
		_slowRstm.setDescription(
				"Runs during the period that took at least " + SLOW_TIMES_NORMAL + " times as long as the job normally does, the worst first.<br>" +
				"<i>Normal</i> is the average of the runs that succeeded in the days before the period (the job must have at least " + SLOW_MIN_HIST_RUNS + " of them). Runs shorter than " + SLOW_MIN_SECONDS + " seconds are not looked at.<br>");
		_slowRstm.setColumnDescription("START_TIME"       , "When the run started");
		_slowRstm.setColumnDescription("CON_NAME"         , "Container (PDB) name");
		_slowRstm.setColumnDescription("OWNER"            , "The schema that owns the job");
		_slowRstm.setColumnDescription("JOB_NAME"         , "Name of the job");
		_slowRstm.setColumnDescription("STATUS"           , "How the run ended");
		_slowRstm.setColumnDescription("duration"         , "How long this run took (HH:MM:SS)");
		_slowRstm.setColumnDescription("hist_avg_duration", "How long the job normally takes: the average of the runs that succeeded in the days before the period");
		_slowRstm.setColumnDescription("times_normal"     , "duration / hist_avg_duration");
		_slowRstm.setColumnDescription("hist_max_duration", "The longest run that succeeded in the days before the period");
		_slowRstm.setColumnDescription("hist_runs"        , "How many runs that succeeded in the days before the period");

		//---------------------------------------------------------------
		// Jobs that were running when the log was read
		//---------------------------------------------------------------
		sql = ""
			+ "select \n"
			+ "     r.[START_TIME] \n"
			+ "    ,r.[CON_NAME] \n"
			+ "    ,r.[OWNER] \n"
			+ "    ,r.[JOB_NAME] \n"
			+ "    ,r.[JOB_SUBNAME] \n"
			+ "    ," + hms("r.[ELAPSED_SEC]") + "  as [elapsed] \n"
			+ "    ," + hms("h.[hist_avg_sec]") + " as [hist_avg_duration] \n"
			+ "    ," + hms("h.[hist_max_sec]") + " as [hist_max_duration] \n"
			+ "    ,r.[CPU_SEC] \n"
			+ "    ,r.[SESSION_ID] \n"
			+ "    ,r.[INSTANCE_ID] \n"
			+ "from " + running + " r \n"
			+ "left join ( \n" + hist + ") h on h.[CON_ID] = r.[CON_ID] and h.[OWNER] = r.[OWNER] and h.[JOB_NAME] = r.[JOB_NAME] \n"
			+ "order by r.[ELAPSED_SEC] desc \n";

		_runningRstm = executeQuery(conn, sql, true, "OracleJobScheduler_running");
		_runningRstm.setDescription("The jobs that were running at the moment the log was read (a run is only in the log when it has ended). Compare <code>elapsed</code> with how long the job normally takes.<br>");
		_runningRstm.setColumnDescription("START_TIME"       , "When the run started");
		_runningRstm.setColumnDescription("CON_NAME"         , "Container (PDB) name");
		_runningRstm.setColumnDescription("OWNER"            , "The schema that owns the job");
		_runningRstm.setColumnDescription("JOB_NAME"         , "Name of the job");
		_runningRstm.setColumnDescription("JOB_SUBNAME"      , "For a job that is a chain: the step that was running");
		_runningRstm.setColumnDescription("elapsed"          , "How long it had been running (HH:MM:SS)");
		_runningRstm.setColumnDescription("hist_avg_duration", "How long the job normally takes: the average of the runs that succeeded in the days before the period");
		_runningRstm.setColumnDescription("hist_max_duration", "The longest run that succeeded in the days before the period");
		_runningRstm.setColumnDescription("CPU_SEC"          , "CPU seconds the run had used");
		_runningRstm.setColumnDescription("SESSION_ID"       , "The SID of the session that runs the job");
		_runningRstm.setColumnDescription("INSTANCE_ID"      , "The instance that runs the job (RAC)");

		//---------------------------------------------------------------
		// Jobs in a state where they do not run. A DISABLED job only if it ran in the days we have: Oracle ships jobs that are disabled.
		// And not the jobs of SYS: Oracle 23 has its own tasks as jobs that are DISABLED, and runs them all the same (ORA$_ATSK_...)
		//---------------------------------------------------------------
		sql = ""
			+ "select \n"
			+ "     j.[CON_NAME] \n"
			+ "    ,j.[OWNER] \n"
			+ "    ,j.[JOB_NAME] \n"
			+ "    ,j.[ENABLED] \n"
			+ "    ,j.[STATE] \n"
			+ "    ,j.[LAST_START_TIME] \n"
			+ "    ,j.[FAILURE_COUNT] \n"
			+ "    ,j.[RUN_COUNT] \n"
			+ "    ,j.[SCHEDULE] \n"
			+ "    ,j.[JOB_ACTION] \n"
			+ "from " + jobs + " j \n"
			+ "where j.[STATE] in ('BROKEN', 'FAILED', 'CHAIN_STALLED', 'RETRY SCHEDULED') \n"
			+ "   or (j.[STATE] = 'DISABLED' and j.[OWNER] <> 'SYS' and exists (select 1 from " + runs + " r where r.[CON_ID] = j.[CON_ID] and r.[OWNER] = j.[OWNER] and r.[JOB_NAME] = j.[JOB_NAME])) \n"
			+ "order by j.[STATE], j.[OWNER], j.[JOB_NAME] \n";

		_stateRstm = executeQuery(conn, sql, true, "OracleJobScheduler_state");
		_stateRstm.setDescription(
				"<b>BROKEN</b> = it failed too many times in a row (16 by default): it will not run again until someone enables it. <b>FAILED</b> = a job that should run once, and failed. " +
				"<b>CHAIN_STALLED</b> = a chain that can not go on. <b>RETRY SCHEDULED</b> = it failed, and will be tried again. " +
				"<b>DISABLED</b> is only listed for a job that has run in the days of the log (so someone, or something, disabled it lately), and not for the jobs of SYS.<br>");
		_stateRstm.setColumnDescription("CON_NAME"       , "Container (PDB) name");
		_stateRstm.setColumnDescription("OWNER"          , "The schema that owns the job");
		_stateRstm.setColumnDescription("JOB_NAME"       , "Name of the job");
		_stateRstm.setColumnDescription("ENABLED"        , "TRUE or FALSE");
		_stateRstm.setColumnDescription("STATE"          , "State of the job when the log was read");
		_stateRstm.setColumnDescription("LAST_START_TIME", "When the job was last started");
		_stateRstm.setColumnDescription("FAILURE_COUNT"  , "Number of times the job has failed, in all");
		_stateRstm.setColumnDescription("RUN_COUNT"      , "Number of times the job has run, in all");
		_stateRstm.setColumnDescription("SCHEDULE"       , "When the job runs: the repeat interval, or the name of a schedule or a window");
		_stateRstm.setColumnDescription("JOB_ACTION"     , "What the job does: the name of the program, or the start of the PL/SQL that it runs");

		//---------------------------------------------------------------
		// All jobs that ran during the period
		//---------------------------------------------------------------
		sql = ""
			+ "select \n"
			+ "     p.[CON_NAME] \n"
			+ "    ,p.[JOB_KIND] \n"
			+ "    ,p.[OWNER] \n"
			+ "    ,p.[JOB_NAME] \n"
			+ "    ,p.[runs] \n"
			+ "    ,p.[succeeded] \n"
			+ "    ,p.[not_succeeded] \n"
			+ "    ,p.[first_start] \n"
			+ "    ,p.[last_start] \n"
			+ "    ," + hms("p.[total_sec]") + "    as [total_duration] \n"
			+ "    ," + hms("p.[avg_sec]") + "      as [avg_duration] \n"
			+ "    ," + hms("p.[max_sec]") + "      as [max_duration] \n"
			+ "    ," + hms("h.[hist_avg_sec]") + " as [hist_avg_duration] \n"
			+ "    ," + hms("h.[hist_max_sec]") + " as [hist_max_duration] \n"
			+ "    ,h.[hist_runs] \n"
			+ "    ,j.[STATE] \n"
			+ "    ,j.[NEXT_RUN_TIME] \n"
			+ "    ,j.[SCHEDULE] \n"
			+ "    ,j.[JOB_ACTION] \n"
			+ "from ( \n"
			+ "    select [CON_ID], max([CON_NAME]) as [CON_NAME], [JOB_KIND], [OWNER], [JOB_NAME] \n"
			+ "          ,count(*)                                                 as [runs] \n"
			+ "          ,sum(case when [STATUS] =  'SUCCEEDED' then 1 else 0 end) as [succeeded] \n"
			+ "          ,sum(case when [STATUS] <> 'SUCCEEDED' then 1 else 0 end) as [not_succeeded] \n"
			+ "          ,min([START_TIME])   as [first_start] \n"
			+ "          ,max([START_TIME])   as [last_start] \n"
			+ "          ,sum([DURATION_SEC]) as [total_sec] \n"
			+ "          ,avg([DURATION_SEC]) as [avg_sec] \n"
			+ "          ,max([DURATION_SEC]) as [max_sec] \n"
			+ "    from " + runs + " \n"
			+ "    where [JOB_SUBNAME] is null and " + inPeriod + " \n"
			+ "    group by [CON_ID], [JOB_KIND], [OWNER], [JOB_NAME] \n"
			+ ") p \n"
			+ "left join ( \n" + hist + ") h on h.[CON_ID] = p.[CON_ID] and h.[OWNER] = p.[OWNER] and h.[JOB_NAME] = p.[JOB_NAME] \n"
			+ "left join " + jobs + " j on j.[CON_ID] = p.[CON_ID] and j.[OWNER] = p.[OWNER] and j.[JOB_NAME] = p.[JOB_NAME] \n"
			+ "order by p.[total_sec] desc, p.[runs] desc, p.[OWNER], p.[JOB_NAME] \n";

		_overviewRstm = executeQuery(conn, sql, true, "OracleJobScheduler_overview");
		_overviewRstm.setHighlightSortColumns("total_duration");
		_overviewRstm.setDescription("One row for each job that ran during the period, the one that ran for the longest time (all its runs added up) first.<br>");
		_overviewRstm.setColumnDescription("CON_NAME"         , "Container (PDB) name. Empty when the database is not a Container Database, or when the collector is connected to a PDB.");
		_overviewRstm.setColumnDescription("JOB_KIND"         , "SCHEDULER = a job in DBA_SCHEDULER_JOBS, AUTOTASK = an automatic maintenance task (statistics, space advisor...)");
		_overviewRstm.setColumnDescription("OWNER"            , "The schema that owns the job");
		_overviewRstm.setColumnDescription("JOB_NAME"         , "Name of the job");
		_overviewRstm.setColumnDescription("runs"             , "How many times the job ran during the period");
		_overviewRstm.setColumnDescription("succeeded"        , "How many of the runs that succeeded");
		_overviewRstm.setColumnDescription("not_succeeded"    , "How many of the runs that failed or was stopped");
		_overviewRstm.setColumnDescription("first_start"      , "When the first run of the period started");
		_overviewRstm.setColumnDescription("last_start"       , "When the last run of the period started");
		_overviewRstm.setColumnDescription("total_duration"   , "All the runs of the period added up (HH:MM:SS)");
		_overviewRstm.setColumnDescription("avg_duration"     , "Average time of a run during the period");
		_overviewRstm.setColumnDescription("max_duration"     , "The longest run during the period");
		_overviewRstm.setColumnDescription("hist_avg_duration", "How long the job normally takes: the average of the runs that succeeded in the days before the period");
		_overviewRstm.setColumnDescription("hist_max_duration", "The longest run that succeeded in the days before the period");
		_overviewRstm.setColumnDescription("hist_runs"        , "How many runs that succeeded in the days before the period");
		_overviewRstm.setColumnDescription("STATE"            , "State of the job when the log was read: SCHEDULED, RUNNING, DISABLED, BROKEN... Empty for an automatic task, and for a job that does not exist any more");
		_overviewRstm.setColumnDescription("NEXT_RUN_TIME"    , "When the job will run next");
		_overviewRstm.setColumnDescription("SCHEDULE"         , "When the job runs: the repeat interval, or the name of a schedule or a window");
		_overviewRstm.setColumnDescription("JOB_ACTION"       , "What the job does: the name of the program, or the start of the PL/SQL that it runs");
	}
}
