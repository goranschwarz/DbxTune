/*******************************************************************************
 * Copyright (C) 2010-2025 Goran Schwarz
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
package com.dbxtune.cm.sqlserver;

import java.awt.event.MouseEvent;
import java.lang.invoke.MethodHandles;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.naming.NameNotFoundException;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.dbxtune.ICounterController;
import com.dbxtune.IGuiController;
import com.dbxtune.alarm.AlarmHandler;
import com.dbxtune.alarm.events.AlarmEvent;
import com.dbxtune.alarm.events.sqlserver.AlarmEventJobSchedulerLastRunFailed;
import com.dbxtune.alarm.events.sqlserver.AlarmEventJobSchedulerLongRunning;
import com.dbxtune.alarm.events.sqlserver.AlarmEventJobSchedulerMissed;
import com.dbxtune.cm.CmHighlighterDescriptor;
import com.dbxtune.cm.CmSettingsHelper;
import com.dbxtune.cm.CmSettingsHelper.RegExpInputValidator;
import com.dbxtune.cm.CounterSample;
import com.dbxtune.cm.CounterSetTemplates;
import com.dbxtune.cm.CounterSetTemplates.Type;
import com.dbxtune.cm.CountersModel;
import com.dbxtune.cm.sqlserver.gui.CmJobSchedulerPanel;
import com.dbxtune.config.dict.MonTablesDictionary;
import com.dbxtune.config.dict.MonTablesDictionaryManager;
import com.dbxtune.gui.MainFrame;
import com.dbxtune.gui.ResultSetTableModel;
import com.dbxtune.gui.ResultSetTableModel.TableStringRenderer;
import com.dbxtune.gui.TabularCntrPanel;
import com.dbxtune.sql.conn.DbxConnection;
import com.dbxtune.sql.conn.info.DbmsVersionInfo;
import com.dbxtune.utils.Configuration;
import com.dbxtune.utils.SqlServerUtils;
import com.dbxtune.utils.StringUtil;
import com.dbxtune.utils.TimeUtils;

/**
 * @author Goran Schwarz (goran_schwarz@hotmail.com)
 */
public class CmJobScheduler
extends CountersModel
{
	private static final Logger _logger = LogManager.getLogger(MethodHandles.lookup().lookupClass());
	private static final long    serialVersionUID = 1L;

	public static final String   CM_NAME          = CmJobScheduler.class.getSimpleName();
	public static final String   SHORT_NAME       = "Job Scheduler";
	public static final String   HTML_DESC        = 
		"<html>" +
			"<p>What SQL Agent Job Scheduler are running now and what has been running.</p>" +
			"<br>" +
			"Table Background colors:" +
			"<ul>" +
//			"    <li>YELLOW      - xxxx</li>" +
			"    <li>GREEN       - RUNNING_OK      - Jobs is Currently running</li>" +
			"    <li>ORANGE      - RUNNING_LONG    - Job is Running longer than it usually does.</li>" +
			"    <li>PINK        - MISSED          - Job was scheduled to run, but it hasn't.</li>" +
			"    <li>RED         - FAILED_LAST_RUN - Jobs Failed on last execution.</li>" +
			"</ul>" +
		"</html>";

	public static final String   GROUP_NAME       = MainFrame.TCP_GROUP_SERVER;
	public static final String   GUI_ICON_FILE    = "images/"+CM_NAME+".png";

	public static final long     NEED_SRV_VERSION = 0;
	public static final long     NEED_CE_VERSION  = 0;

	public static final String[] MON_TABLES       = new String[] { CM_NAME };
	public static final String[] NEED_ROLES       = new String[] {};
	public static final String[] NEED_CONFIG      = new String[] {};

	public static final String[] PCT_COLUMNS      = new String[] {};
	public static final String[] DIFF_COLUMNS     = new String[] {};

	public static final boolean  NEGATIVE_DIFF_COUNTERS_TO_ZERO = false;
	public static final boolean  IS_SYSTEM_CM                   = true;
	public static final int      DEFAULT_POSTPONE_TIME          = 300;
	public static final int      DEFAULT_QUERY_TIMEOUT          = CountersModel.DEFAULT_sqlQueryTimeout;;

	@Override public int     getDefaultPostponeTime()                 { return DEFAULT_POSTPONE_TIME; }
	@Override public int     getDefaultQueryTimeout()                 { return DEFAULT_QUERY_TIMEOUT; }
	@Override public boolean getDefaultIsNegativeDiffCountersToZero() { return NEGATIVE_DIFF_COUNTERS_TO_ZERO; }
	@Override public Type    getTemplateLevel()                       { return Type.ALL; }

	/**
	 * FACTORY  method to create the object
	 */
	public static CountersModel create(ICounterController counterController, IGuiController guiController)
	{
		if (guiController != null && guiController.hasGUI())
			guiController.splashWindowProgress("Loading: Counter Model '"+CM_NAME+"'");

		return new CmJobScheduler(counterController, guiController);
	}

	public CmJobScheduler(ICounterController counterController, IGuiController guiController)
	{
		super(counterController, guiController,
				CM_NAME, GROUP_NAME, /*sql*/null, /*pkList*/null, 
				DIFF_COLUMNS, PCT_COLUMNS, MON_TABLES, 
				NEED_ROLES, NEED_CONFIG, NEED_SRV_VERSION, NEED_CE_VERSION, 
				NEGATIVE_DIFF_COUNTERS_TO_ZERO, IS_SYSTEM_CM, DEFAULT_POSTPONE_TIME);

		setDisplayName(SHORT_NAME);
		setDescription(HTML_DESC);

		setIconFile(GUI_ICON_FILE);

		setShowClearTime(false);

		setCounterController(counterController);
		setGuiController(guiController);
		
		addTrendGraphs();
		
		CounterSetTemplates.register(this);
	}


	//------------------------------------------------------------
	// Implementation
	//------------------------------------------------------------
	private static final String  PROP_PREFIX = CM_NAME;

	public static final String  PROPKEY_when_status_FAILED_LAST_RUN_get_errors           = PROP_PREFIX + ".when.status.FAILED_LAST_RUN.get.errors";
	public static final boolean DEFAULT_when_status_FAILED_LAST_RUN_get_errors           = true;

	public static final String  PROPKEY_when_status_FAILED_LAST_RUN_forceNormalHtmlTable = PROP_PREFIX + ".when.status.FAILED_LAST_RUN.force.normal.html.table";
	public static final boolean DEFAULT_when_status_FAILED_LAST_RUN_forceNormalHtmlTable = false;

	
	private void addTrendGraphs()
	{
	}

	@Override
	protected TabularCntrPanel createGui()
	{
		return new CmJobSchedulerPanel(this);
	}

	@Override
	public String[] getDependsOnConfigForVersion(DbxConnection conn, DbmsVersionInfo versionInfo)
	{
		return NEED_CONFIG;
	}
	
	@Override
	public void addMonTableDictForVersion(DbxConnection conn, DbmsVersionInfo versionInfo)
	{
		try 
		{
			MonTablesDictionary mtd = MonTablesDictionaryManager.getInstance();
			mtd.addTable(CM_NAME, HTML_DESC);

			mtd.addColumn(CM_NAME, "job_name",                "<html>Name of the SQL Server Agent job.</html>");
			mtd.addColumn(CM_NAME, "schedule_name",           "<html>Name of the SQL Server Agent schedule attached to the job.</html>");
			mtd.addColumn(CM_NAME, "status",                  "<html>Monitoring status derived from runtime, scheduling, and last execution result.<br><br>"
			                                                      + "Possible values:<br>"
			                                                      + "- OK = no issues detected<br>"
			                                                      + "- RUNNING_OK = currently executing within expected runtime<br>"
			                                                      + "- RUNNING_LONG = currently executing longer than expected<br>"
			                                                      + "- MISSED = scheduled execution appears overdue and was not executed<br>"
			                                                      + "- FAILED_LAST_RUN = most recent execution failed"
			                                                      + "</html>");
			mtd.addColumn(CM_NAME, "is_running",              "<html>1 if the job is currently executing, otherwise 0.</html>");
			mtd.addColumn(CM_NAME, "is_overdue",              "<html>1 if next scheduled execution time is in the past, otherwise 0.<br>Note: overdue does not automatically mean failure or missed execution.</html>");
			mtd.addColumn(CM_NAME, "current_step_name",       "<html>Current StepID - StepName.<br>NULL if the job is not currently running.</html>");
			mtd.addColumn(CM_NAME, "start_execution_date",    "<html>Timestamp when current execution started.<br>NULL if the job is not currently running.</html>");
			mtd.addColumn(CM_NAME, "current_runtime_seconds", "<html>Elapsed runtime in seconds for the current execution.<br>NULL if the job is not currently running.</html>");
			mtd.addColumn(CM_NAME, "current_runtime_hms",     "<html>Same as 'current_runtime_seconds' but in readable format of: #d #h #m #s </html>");
			mtd.addColumn(CM_NAME, "avg_runtime_seconds",     "<html>Average runtime in seconds across historical successful executions.</html>");
			mtd.addColumn(CM_NAME, "avg_runtime_hms",         "<html>Same as 'avg_runtime_seconds' but in readable format of: #d #h #m #s </html>");
			mtd.addColumn(CM_NAME, "max_runtime_seconds",     "<html>Longest observed runtime in seconds across historical successful executions.</html>");
			mtd.addColumn(CM_NAME, "max_runtime_hms",        "<html>Same as 'max_runtime_seconds' but in readable format of: #d #h #m #s </html>");
			mtd.addColumn(CM_NAME, "execution_count",         "<html>Number of historical successful executions included in runtime statistics.</html>");
			mtd.addColumn(CM_NAME, "last_runtime_seconds",    "<html>Runtime in seconds of the most recent execution, regardless of success or failure.</html>");
			mtd.addColumn(CM_NAME, "last_runtime_hms",        "<html>Same as 'last_runtime_seconds' but in readable format of: #d #h #m #s </html>");
			mtd.addColumn(CM_NAME, "last_run_datetime",       "<html>Timestamp of the most recent execution.</html>");
			mtd.addColumn(CM_NAME, "last_run_age_seconds",    "<html>Seconds elapsed since the most recent execution.</html>");
			mtd.addColumn(CM_NAME, "last_run_age_hms",        "<html>Same as 'last_run_age_seconds' but in readable format of: #d #h #m #s </html>");
			mtd.addColumn(CM_NAME, "next_run_datetime",       "<html>Next scheduled execution timestamp according to SQL Server Agent.</html>");
			mtd.addColumn(CM_NAME, "schedule_offset_seconds", "<html>Difference in seconds between next scheduled execution time and current time.<br><br>"
			                                                      + "Positive = next execution is scheduled in the future.<br>"
			                                                      + "Negative = scheduled time has already passed."
			                                                      + "</html>");
			mtd.addColumn(CM_NAME, "schedule_offset_hms",     "<html>Same as 'schedule_offset_seconds' but in readable format of: #d #h #m #s </html>");
		}
		catch (NameNotFoundException e) 
		{
			_logger.warn("Problems in cm='" + CM_NAME + "', adding addMonTableDictForVersion. Caught: " + e); 
		}
	}

	@Override
	public List<String> getPkForVersion(DbxConnection conn, DbmsVersionInfo versionInfo)
	{
//		List <String> pkCols = new LinkedList<String>();
//
//		pkCols.add("job_id");
//
//		return pkCols;
		return null;
	}

	@Override
	public String getSqlForVersion(DbxConnection conn, DbmsVersionInfo versionInfo)
	{
		String sql = ""
			+ "IF OBJECT_ID('tempdb..#HistoryBase') IS NOT NULL \n"
			+ "    DROP TABLE #HistoryBase; \n"
			+ " \n"
			+ "SELECT \n"
			+ "    h.job_id, \n"
			+ "    h.run_status, \n"
			+ " \n"
			+ "    DATETIMEFROMPARTS( \n"
			+ "        h.run_date / 10000, \n"
			+ "        (h.run_date % 10000) / 100, \n"
			+ "        h.run_date % 100, \n"
			+ "        h.run_time / 10000, \n"
			+ "        (h.run_time % 10000) / 100, \n"
			+ "        h.run_time % 100, \n"
			+ "        0 \n"
			+ "    ) AS run_datetime, \n"
			+ " \n"
			+ "    ( \n"
			+ "        (h.run_duration / 10000) * 3600 + \n"
			+ "        ((h.run_duration % 10000) / 100) * 60 + \n"
			+ "        (h.run_duration % 100) \n"
			+ "    ) AS runtime_seconds, \n"
			+ " \n"
			+ "    ROW_NUMBER() OVER ( \n"
			+ "        PARTITION BY h.job_id \n"
			+ "        ORDER BY h.run_date DESC, h.run_time DESC \n"
			+ "    ) AS rn \n"
			+ "INTO #HistoryBase \n"
			+ "FROM msdb.dbo.sysjobhistory h \n"
			+ "WHERE h.step_id = 0; \n"
			+ " \n"
			+ "CREATE CLUSTERED INDEX IX_HistoryBase \n"
			+ "ON #HistoryBase (job_id, rn); \n"
			+ " \n"

			+ "WITH RunningJobs AS ( \n"
			+ "    SELECT \n"
			+ "        ja.job_id, \n"
			+ "        j.name AS job_name, \n"
			+ "        ja.start_execution_date, \n"
			+ "        DATEDIFF(SECOND, ja.start_execution_date, GETDATE()) AS current_runtime_seconds, \n"
			+ "        1 AS is_running, \n"
			+ "        CAST(ISNULL(ja.last_executed_step_id, 0) + 1 AS varchar(10)) \n"
			+ "          + ' - ' \n"
			+ "          + ( \n"
			+ "              SELECT js.step_name \n"
			+ "              FROM msdb.dbo.sysjobsteps js \n"
			+ "              WHERE ja.job_id = js.job_id \n"
			+ "                AND js.step_id = ISNULL(ja.last_executed_step_id, 0) + 1 \n"
			+ "        ) AS current_step_name \n"
			+ "    FROM msdb.dbo.sysjobactivity ja \n"
			+ "    INNER JOIN msdb.dbo.sysjobs j \n"
			+ "        ON ja.job_id = j.job_id \n"
			+ "    WHERE ja.session_id = (SELECT MAX(session_id) FROM msdb.dbo.sysjobactivity) \n"
			+ "      AND ja.start_execution_date IS NOT NULL \n"
			+ "      AND ja.stop_execution_date IS NULL \n"
			+ "), \n"
			+ " \n"

			+ "JobRuntimeHistory AS ( \n"
			+ "    SELECT \n"
			+ "        job_id, \n"
			+ " \n"
			+ "        CAST( \n"
			+ "            AVG(CASE WHEN run_status = 1 THEN runtime_seconds END) \n"
			+ "            AS INT \n"
			+ "        ) AS avg_runtime_seconds, \n"
			+ " \n"
			+ "        MAX(CASE WHEN run_status = 1 THEN runtime_seconds END) \n"
			+ "            AS max_runtime_seconds, \n"
			+ " \n"
			+ "        COUNT(CASE WHEN run_status = 1 THEN 1 END) \n"
			+ "            AS execution_count \n"
			+ "    FROM #HistoryBase \n"
			+ "    GROUP BY job_id \n"
			+ "), \n"
			+ " \n"

			+ "LastRuns AS ( \n"
			+ "    SELECT \n"
			+ "        job_id, \n"
			+ "        run_datetime AS last_run_datetime, \n"
			+ "        runtime_seconds AS last_runtime_seconds, \n"
			+ "        CASE WHEN run_status = 0 THEN 1 ELSE 0 END AS had_failure \n"
			+ "    FROM #HistoryBase \n"
			+ "    WHERE rn = 1 \n"
			+ "), \n"
			+ " \n"

			+ "SchedulesRaw AS ( \n"
			+ "    SELECT \n"
			+ "        j.job_id, \n"
			+ "        j.name AS job_name, \n"
			+ "        s.name AS schedule_name, \n"
			+ " \n"
			+ "        DATETIMEFROMPARTS( \n"
			+ "            js.next_run_date / 10000, \n"
			+ "            (js.next_run_date % 10000) / 100, \n"
			+ "            js.next_run_date % 100, \n"
			+ "            js.next_run_time / 10000, \n"
			+ "            (js.next_run_time % 10000) / 100, \n"
			+ "            js.next_run_time % 100, \n"
			+ "            0 \n"
			+ "        ) AS next_run_datetime \n"
			+ "    FROM msdb.dbo.sysjobs j \n"
			+ "    INNER JOIN msdb.dbo.sysjobschedules js \n"
			+ "        ON j.job_id = js.job_id \n"
			+ "    INNER JOIN msdb.dbo.sysschedules s \n"
			+ "        ON js.schedule_id = s.schedule_id \n"
			+ "    WHERE j.enabled = 1 \n"
			+ "      AND s.enabled = 1 \n"
			+ "      AND js.next_run_date > 0 \n"
			+ "), \n"
			+ " \n"

			+ "Schedules AS ( \n"
			+ "    SELECT \n"
			+ "        job_id, \n"
			+ "        job_name, \n"
			+ "        schedule_name, \n"
			+ "        next_run_datetime \n"
			+ "    FROM ( \n"
			+ "        SELECT \n"
			+ "            sr.*, \n"
			+ "            lr.last_run_datetime, \n"
			+ "            r.is_running, \n"
			+ " \n"
			+ "            ROW_NUMBER() OVER ( \n"
			+ "                PARTITION BY sr.job_id \n"
			+ "                ORDER BY \n"
			+ "                    CASE \n"
			+ "                        WHEN ISNULL(r.is_running, 0) = 1 THEN \n"
			+ "                            ABS(DATEDIFF( \n"
			+ "                                SECOND, \n"
			+ "                                lr.last_run_datetime, \n"
			+ "                                sr.next_run_datetime \n"
			+ "                            )) \n"
			+ " \n"
			+ "                        ELSE \n"
			+ "                            CASE \n"
			+ "                                WHEN sr.next_run_datetime >= GETDATE() THEN 0 \n"
			+ "                                ELSE 1 \n"
			+ "                            END \n"
			+ "                    END, \n"
			+ " \n"
			+ "                    CASE \n"
			+ "                        WHEN ISNULL(r.is_running, 0) = 1 \n"
			+ "                        THEN sr.next_run_datetime \n"
			+ "                    END ASC, \n"
			+ " \n"
			+ "                    CASE \n"
			+ "                        WHEN ISNULL(r.is_running, 0) = 0 \n"
			+ "                        THEN sr.next_run_datetime \n"
			+ "                    END ASC \n"
			+ "            ) AS rn \n"
			+ " \n"
			+ "        FROM SchedulesRaw sr \n"
			+ "        LEFT JOIN LastRuns lr \n"
			+ "            ON sr.job_id = lr.job_id \n"
			+ "        LEFT JOIN RunningJobs r \n"
			+ "            ON sr.job_id = r.job_id \n"
			+ "    ) x \n"
			+ "    WHERE rn = 1 \n"
			+ "), \n"
			+ " \n"

			+ "Combined AS ( \n"
			+ "    SELECT \n"
			+ "        s.job_id, \n"
			+ "        s.job_name, \n"
			+ "        s.schedule_name, \n"
			+ "        s.next_run_datetime, \n"
			+ " \n"
			+ "        lr.last_run_datetime, \n"
			+ "        lr.last_runtime_seconds, \n"
			+ "        lr.had_failure, \n"
			+ " \n"
			+ "        r.start_execution_date, \n"
			+ "        r.current_runtime_seconds, \n"
			+ "        r.current_step_name, \n"
			+ "        ISNULL(r.is_running, 0) AS is_running, \n"
			+ " \n"
			+ "        rh.avg_runtime_seconds, \n"
			+ "        rh.max_runtime_seconds, \n"
			+ "        rh.execution_count \n"
			+ " \n"
			+ "    FROM Schedules s \n"
			+ "    LEFT JOIN RunningJobs r \n"
			+ "        ON s.job_id = r.job_id \n"
			+ "    LEFT JOIN JobRuntimeHistory rh \n"
			+ "        ON s.job_id = rh.job_id \n"
			+ "    LEFT JOIN LastRuns lr \n"
			+ "        ON s.job_id = lr.job_id \n"
			+ "), \n"
			+ " \n"

			+ "FinalStatus AS ( \n"
			+ "    SELECT \n"
			+ "        *, \n"
			+ " \n"
			+ "        DATEDIFF(SECOND, last_run_datetime, GETDATE()) AS last_run_age_seconds, \n"
			+ "        DATEDIFF(SECOND, GETDATE(), next_run_datetime) AS schedule_offset_seconds, \n"
			+ " \n"
			+ "        CASE \n"
			+ "            WHEN DATEDIFF(SECOND, GETDATE(), next_run_datetime) < 0 THEN 1 \n"
			+ "            ELSE 0 \n"
			+ "        END AS is_overdue, \n"
			+ " \n"
			+ "        CASE \n"
			+ "            WHEN is_running = 1 \n"
			+ "                 AND current_runtime_seconds > \n"
			+ "                    COALESCE( \n"
			+ "                        CAST(max_runtime_seconds * 1.2 AS INT), \n"
			+ "                        avg_runtime_seconds * 2, \n"
			+ "                        7200 \n"
			+ "                    ) \n"
			+ "            THEN 'RUNNING_LONG' \n"
			+ " \n"
			+ "            WHEN is_running = 0 \n"
			+ "                 AND next_run_datetime < DATEADD(MINUTE, -15, GETDATE()) \n"
			+ "                 AND ( \n"
			+ "                     last_run_datetime IS NULL \n"
			+ "                     OR last_run_datetime < next_run_datetime \n"
			+ "                 ) \n"
			+ "            THEN 'MISSED' \n"
			+ " \n"
			+ "            WHEN had_failure = 1 \n"
			+ "            THEN 'FAILED_LAST_RUN' \n"
			+ " \n"
			+ "            WHEN is_running = 1 \n"
			+ "            THEN 'RUNNING_OK' \n"
			+ " \n"
			+ "            ELSE 'OK' \n"
			+ "        END AS status \n"
			+ " \n"
			+ "    FROM Combined \n"
			+ ") \n"
			+ " \n"

			+ "SELECT \n"
			+ "     job_name \n"
			+ "    ,schedule_name \n"
			+ "    ,status \n"
			+ " \n"
			+ "    ,is_running \n"
			+ "    ,is_overdue \n"
			+ " \n"
			+ "    ,current_step_name \n"
			+ "    ,start_execution_date \n"
			+ "    ,current_runtime_seconds \n"
			+ "    ,CAST('' as varchar(20)) AS current_runtime_hms \n"
			+ " \n"
			+ "    ,last_runtime_seconds \n"
			+ "    ,CAST('' as varchar(20)) AS last_runtime_hms \n"
			+ "    ,avg_runtime_seconds \n"
			+ "    ,CAST('' as varchar(20)) AS avg_runtime_hms \n"
			+ "    ,max_runtime_seconds \n"
			+ "    ,CAST('' as varchar(20)) AS max_runtime_hms \n"
			+ "    ,execution_count \n"
			+ " \n"
			+ "    ,last_run_datetime \n"
			+ "    ,last_run_age_seconds \n"
			+ "    ,CAST('' as varchar(20)) AS last_run_age_hms \n"
			+ " \n"
			+ "    ,next_run_datetime \n"
			+ "    ,schedule_offset_seconds \n"
			+ "    ,CAST('' as varchar(20)) AS schedule_offset_hms \n"
			+ " \n"
			+ "    ,CAST(job_id as varchar(40))  AS job_id \n"     // If we need a PK this should be it
			+ "    ,CAST(NULL   as varchar(max)) AS extra_info \n" // Updated in: localCalculation(CounterSample newSample) 
			+ " \n"
			+ "FROM FinalStatus \n"
			+ "ORDER BY schedule_offset_seconds \n"
			+ "";

		return sql;
	}


	/** 
	 * Remember _lastErrors from last sample.
	 * Note: in localCalculation(CounterSample newSample) we will create a new Map on every sample
	 * Key: job_is:last_run_datetime
	 * Val: the last errors from: msdb.dbo.sysjobhistory
	 */
	private Map<String, String> _lastErrorsMap = new HashMap<>();
	
	@Override
	public void localCalculation(CounterSample newSample)
	{
		// Fill in h:m:s columns
		int pos_schedule_offset_seconds = newSample.findColumn("schedule_offset_seconds");
		int pos_last_run_age_seconds    = newSample.findColumn("last_run_age_seconds");

		int pos_schedule_offset_hms     = newSample.findColumn("schedule_offset_hms");
		int pos_last_run_age_hms        = newSample.findColumn("last_run_age_hms");

		int pos_current_runtime_seconds = newSample.findColumn("current_runtime_seconds");
		int pos_avg_runtime_seconds     = newSample.findColumn("avg_runtime_seconds");
		int pos_max_runtime_seconds     = newSample.findColumn("max_runtime_seconds");
		int pos_last_runtime_seconds    = newSample.findColumn("last_runtime_seconds");
		
		int pos_current_runtime_hms     = newSample.findColumn("current_runtime_hms");
		int pos_avg_runtime_hms         = newSample.findColumn("avg_runtime_hms");
		int pos_max_runtime_hms         = newSample.findColumn("max_runtime_hms");
		int pos_last_runtime_hms        = newSample.findColumn("last_runtime_hms");

		int pos_job_id                  = newSample.findColumn("job_id");
		int pos_last_run_datetime       = newSample.findColumn("last_run_datetime");
		int pos_status                  = newSample.findColumn("status");
		int pos_extra_info              = newSample.findColumn("extra_info");

		// Create a new Map which will hold LastErrors
		Map<String, String> newLastErrorsMap = new HashMap<>();
		
		// Loop on all diffData rows
		for (int rowId=0; rowId < newSample.getRowCount(); rowId++) 
		{
			// "current_runtime_seconds" -- Try to set 0 -> NULL
			if (pos_current_runtime_seconds != -1) 
			{
				Integer val = newSample.getValueAsInteger(rowId, pos_current_runtime_seconds, null);
				if (val != null && val == 0)
				{
					newSample.setValueAt(null, rowId, pos_current_runtime_seconds); 
				}
			}
			
			// Set HMS columns
			if (pos_schedule_offset_hms != -1) { newSample.setValueAt(toHms(newSample.getValueAsInteger(rowId, pos_schedule_offset_seconds, null)), rowId, pos_schedule_offset_hms); }
			if (pos_last_run_age_hms    != -1) { newSample.setValueAt(toHms(newSample.getValueAsInteger(rowId, pos_last_run_age_seconds   , null)), rowId, pos_last_run_age_hms   ); }

			if (pos_current_runtime_hms != -1) { newSample.setValueAt(toHms(newSample.getValueAsInteger(rowId, pos_current_runtime_seconds, null), true), rowId, pos_current_runtime_hms); }
			if (pos_avg_runtime_hms     != -1) { newSample.setValueAt(toHms(newSample.getValueAsInteger(rowId, pos_avg_runtime_seconds    , null)), rowId, pos_avg_runtime_hms    ); }
			if (pos_max_runtime_hms     != -1) { newSample.setValueAt(toHms(newSample.getValueAsInteger(rowId, pos_max_runtime_seconds    , null)), rowId, pos_max_runtime_hms    ); }
			if (pos_last_runtime_hms    != -1) { newSample.setValueAt(toHms(newSample.getValueAsInteger(rowId, pos_last_runtime_seconds   , null)), rowId, pos_last_runtime_hms   ); }

			// "status" -- Get some extra information
			if (pos_status != -1 && pos_extra_info != -1 && pos_job_id != -1 && pos_last_run_datetime != -1)
			{
				String status            = newSample.getValueAsString(rowId, pos_status);

				if ("FAILED_LAST_RUN".equals(status))
				{
					if (Configuration.getCombinedConfiguration().getBooleanProperty(PROPKEY_when_status_FAILED_LAST_RUN_get_errors, DEFAULT_when_status_FAILED_LAST_RUN_get_errors))
					{
						String job_id            = newSample.getValueAsString(rowId, pos_job_id);
						String last_run_datetime = newSample.getValueAsString(rowId, pos_last_run_datetime);
						
						String lastErrorKey = job_id + ":" + last_run_datetime;

						String lastErrorVal = _lastErrorsMap.get(lastErrorKey);

						// Get ERRORS from the DBMS, since there was NOT in the cache
						if (lastErrorVal == null)
						{
							lastErrorVal = getLastErrors(job_id);
						}
						
						// Now add to the NEW cache. (which is "swapped: _lastErrorsMap = newLastErrorsMap" at the end of the method)
						newLastErrorsMap.put(lastErrorKey, lastErrorVal);
						
						// and the old or new LastError in column: "extra_info"
						newSample.setValueAt(lastErrorVal, rowId, pos_extra_info);
					}
					else
					{
						newSample.setValueAt("getLastErrors(jobId) is disable. Enable with property: " + PROPKEY_when_status_FAILED_LAST_RUN_get_errors + "=true", rowId, pos_extra_info);
					}
				}
				else if ("MISSED".equals(status))
				{
					// NO Action
				}
				else if ("RUNNING_LONG".equals(status))
				{
					// NO Action
				}
			}
		}

		// Now switch to the new Map
		_lastErrorsMap = newLastErrorsMap;
	}
	
	private String toHms(Integer sec)
	{
		return toHms(sec, false);
	}
	private String toHms(Integer sec, boolean zeroAsNull)
	{
//		if (sec == null) return "";
		if (sec == null) return null;
		if (sec == 0   ) return zeroAsNull ? null : "0s";

		boolean negative = sec < 0;
		int abs = Math.abs(sec);

		int days = abs / 86400;
		abs %= 86400;

		int hours = abs / 3600;
		abs %= 3600;

		int minutes = abs / 60;
		int seconds = abs % 60;

		StringBuilder sb = new StringBuilder();

		if ( negative )
		{
			sb.append("-");
		}

		if ( days > 0 )
		{
			sb.append(days).append("d ");
		}

		if ( hours > 0 )
		{
			sb.append(hours).append("h ");
		}

		if ( minutes > 0 )
		{
			sb.append(minutes).append("m ");
		}

		if ( seconds > 0 || sb.length() == (negative ? 1 : 0) )
		{
			sb.append(seconds).append("s");
		}

		return sb.toString().trim();
	}

	@Override
	public String getToolTipTextOnTableCell(MouseEvent e, String colName, Object cellValue, int modelRow, int modelCol)
	{
		if ("extra_info".equals(colName))
		{
//			return "<html>" + cellValue + "</html>";
			return cellValue + "";
		}

		return super.getToolTipTextOnTableCell(e, colName, cellValue, modelRow, modelCol);
	}

	public String getLastErrors(String jobId)
	{
		if (StringUtil.isNullOrBlank(jobId))
		{
			return "getLastErrors(jobId=|" + jobId + "|): ERROR: Empty jobId, exiting early...";
		}

		String sql = ""
			    + "WITH FailedRun AS ( \n"
			    + "    SELECT TOP 1 \n"
			    + "        h.instance_id AS end_instance_id \n"
			    + "    FROM msdb.dbo.sysjobhistory h \n"
			    + "    WHERE h.job_id = ? \n"
			    + "      AND h.step_id = 0 \n"
			    + "      AND h.run_status = 0 \n"
			    + "    ORDER BY h.instance_id DESC \n"
			    + "), \n"
			    + "PreviousRun AS ( \n"
			    + "    SELECT \n"
			    + "        MAX(h.instance_id) AS previous_end_instance_id \n"
			    + "    FROM msdb.dbo.sysjobhistory h \n"
			    + "    CROSS JOIN FailedRun fr \n"
			    + "    WHERE h.job_id = ? \n"
			    + "      AND h.step_id = 0 \n"
			    + "      AND h.instance_id < fr.end_instance_id \n"
			    + ") \n"
			    + " \n"
			    + "SELECT \n"
//			    + "    h.instance_id, \n"
//			    + "    h.job_id, \n"
			    + "    h.step_id, \n"
			    + " \n"
			    + "    CASE \n"
			    + "        WHEN h.step_id = 0 THEN '(JOB SUMMARY)' \n"
			    + "        ELSE js.step_name \n"
			    + "    END AS step_name, \n"
			    + " \n"
			    + "    js.subsystem, \n"
			    + "    js.database_name, \n"
			    + " \n"
//			    + "    h.run_status, \n"
			    + "    CASE \n"
			    + "        WHEN h.run_status = 0 THEN 'FAILED' \n"
			    + "        WHEN h.run_status = 1 THEN 'OK' \n"
			    + "        WHEN h.run_status = 2 THEN 'RETRY' \n"
			    + "        WHEN h.run_status = 3 THEN 'CANCELED' \n"
			    + "        WHEN h.run_status = 4 THEN 'IN PROGRESS' \n"
			    + "                              ELSE '-unknown-' \n"
			    + "    END AS run_status, \n"
			    + "    h.sql_severity, \n"
			    + "    h.sql_message_id, \n"
			    + "    h.message, \n"
			    + " \n"
			    + "    js.command, \n"
			    + " \n"
			    + "    DATETIMEFROMPARTS( \n"
			    + "        h.run_date / 10000, \n"
			    + "        (h.run_date % 10000) / 100, \n"
			    + "        h.run_date % 100, \n"
			    + "        h.run_time / 10000, \n"
			    + "        (h.run_time % 10000) / 100, \n"
			    + "        h.run_time % 100, \n"
			    + "        0 \n"
			    + "    ) AS run_datetime, \n"
			    + " \n"
			    + "    h.run_duration, \n"
			    + "    h.retries_attempted, \n"
			    + "    h.server \n"
			    + " \n"
			    + "FROM msdb.dbo.sysjobhistory h \n"
			    + "LEFT JOIN msdb.dbo.sysjobsteps js \n"
			    + "    ON h.job_id = js.job_id \n"
			    + "   AND h.step_id = js.step_id \n"
			    + "CROSS JOIN FailedRun fr \n"
			    + "CROSS JOIN PreviousRun pr \n"
			    + " \n"
			    + "WHERE h.job_id = ? \n"
			    + "  AND h.instance_id > ISNULL(pr.previous_end_instance_id, 0) \n"
			    + "  AND h.instance_id <= fr.end_instance_id \n"
			    + " \n"
			    + "ORDER BY h.step_id ASC \n"
			    + "";

		DbxConnection conn = getCounterController().getMonConnection();
		try (PreparedStatement pstmnt = conn.prepareStatement(sql))
		{
			pstmnt.setString(1, jobId);
			pstmnt.setString(2, jobId);
			pstmnt.setString(3, jobId);
			
			try (ResultSet rs = pstmnt.executeQuery())
			{
				ResultSetTableModel rstm = new ResultSetTableModel(rs, "getLastErrors");
//				return rstm.toHtmlTableString("getLastErrors");
				
				TableStringRenderer tsr = new TableStringRenderer()
				{
					@Override
					public String cellValue(ResultSetTableModel rstm, int row, int col, String colName, Object objVal, String strVal)
					{
//						if ("run_status".equals(colName) && "FAILED"  .equals(strVal)) return "<b><font color='red'>"    + strVal + "</font></b>";
//						if ("run_status".equals(colName) && "OK"      .equals(strVal)) return "<b><font color='green'>"  + strVal + "</font></b>";
//						if ("run_status".equals(colName) && "RETRY"   .equals(strVal)) return    "<font color='orange'>" + strVal + "</font>";
//						if ("run_status".equals(colName) && "CANCELED".equals(strVal)) return    "<font color='red'>"    + strVal + "</font>";
						if ("run_status".equals(colName) && "FAILED"  .equals(strVal)) return "<span style='background-color: red;'   >" + strVal + "</span>";
						if ("run_status".equals(colName) && "OK"      .equals(strVal)) return "<span style='background-color: green;' >" + strVal + "</span>";
						if ("run_status".equals(colName) && "RETRY"   .equals(strVal)) return "<span style='background-color: orange;'>" + strVal + "</span>";
						if ("run_status".equals(colName) && "CANCELED".equals(strVal)) return "<span style='background-color: red;'   >" + strVal + "</span>";

						if ("command".equals(colName)) 
						{
							// replace any potential \r (on Windows Systems)
							return "<pre><code>" + strVal.replace("\r", "") + "</code></pre>";
						}

						if ("message".equals(colName)) 
						{
							String subsystem = rstm.getValueAsString(row, "subsystem", true, "-unknonwn-");
							String message   = SqlServerUtils.jobMessageFormatter(strVal, subsystem);
							return "<pre><code>" + message + "</code></pre>";
						}
						
						// Format numbers to a more readable format
						// Possibly use: some Locale.???
						if (objVal instanceof Number)
						{
							NumberFormat nf = NumberFormat.getInstance();
						//	NumberFormat nf = NumberFormat.getInstance(Locale.US);
							return nf.format(objVal);
						}

						return strVal;
					}
				};

				boolean hasGui = false;
				if (getGuiController() != null && getGuiController().hasGUI())
					hasGui = true;

				boolean forceNormalHtmlTable = Configuration.getCombinedConfiguration().getBooleanProperty(PROPKEY_when_status_FAILED_LAST_RUN_forceNormalHtmlTable, DEFAULT_when_status_FAILED_LAST_RUN_forceNormalHtmlTable);
				
				// For GUI produce a html-table. A bit easier to read if we are having a normal screen
				//   +------+------+------+
				//   | col1 | col2 | col3 |
				//   +------+------+------+
				//   | r1-v | r1-v | r1-v |
				//   | r2-v | r2-v | r2-v |
				//   +------+------+------+
				if (hasGui || forceNormalHtmlTable)
					return "<html>" + rstm.toHtmlTableString("getLastErrors", tsr) + "</html>";

				// For NO-GUI produce one table for each row (good for Alarms "mails" on small devices like iPhone)
				//   Row 1 (2)
				//   +--------+------------+
				//   | Column | Value      |
				//   +--------+------------+
				//   | col1   | r1-c1-val  |
				//   | col2   | r1-c2-val  |
				//   | col3   | r1-c2-val  |
				//   +--------+------------+
				//   Row 2 (2)
				//   +--------+------------+
				//   | Column | Value      |
				//   +--------+------------+
				//   | col1   | r2-c1-val  |
				//   | col2   | r2-c2-val  |
				//   | col3   | r2-c2-val  |
				//   +--------+------------+
				return "<html>" + rstm.toHtmlTablesVerticalString("getLastErrors", tsr) + "</html>";
			}
		}
		catch (SQLException ex)
		{
			return "getLastErrors(jobId=|" + jobId + "|): Caught: " + ex.toString();
		}
	}

//	public static String escapeHtml(Object o)
//	{
//		if (o == null)
//			return null;
//
//		String str = o.toString();
//		str = str.replace("&", "&amp;");
//		str = str.replace("<", "&lt;");
//		str = str.replace(">", "&gt;");
//
//		return str;
//	}

	//--------------------------------------------------------------------------------------
	//--------------------------------------------------------------------------------------
	//-- Alarm Handling
	//--------------------------------------------------------------------------------------
	//--------------------------------------------------------------------------------------
	@Override
	public void sendAlarmRequest()
	{
//		if ( ! hasDiffData() )
//			return;
		
		if ( ! AlarmHandler.hasInstance() )
			return;

		AlarmHandler alarmHandler = AlarmHandler.getInstance();
		
		CountersModel cm = this;
		String dbmsSrvName = cm.getServerName();

//		boolean debugPrint = Configuration.getCombinedConfiguration().getBooleanProperty("sendAlarmRequest.debug", _logger.isDebugEnabled());

		for (int r=0; r<cm.getDiffRowCount(); r++)
		{
			String job_name = cm.getAbsString(r, "job_name");

			//-------------------------------------------------------
			// RUNNING_LONG
			//-------------------------------------------------------
			if (isSystemAlarmsForColumnEnabledAndInTimeRange("RUNNING_LONG"))
			{
				String status = cm.getAbsString(r, "status");

				if ("RUNNING_LONG".equals(status))
				{
					// Get config 'skip/allow'
					String keepSrvRegExp = Configuration.getCombinedConfiguration().getProperty(PROPKEY_alarm_RUNNING_LONG_ForSrv     , DEFAULT_alarm_RUNNING_LONG_ForSrv);
					String skipSrvRegExp = Configuration.getCombinedConfiguration().getProperty(PROPKEY_alarm_RUNNING_LONG_SkipSrv    , DEFAULT_alarm_RUNNING_LONG_SkipSrv);
					String keepJobRegExp = Configuration.getCombinedConfiguration().getProperty(PROPKEY_alarm_RUNNING_LONG_ForJobName , DEFAULT_alarm_RUNNING_LONG_ForJobName);
					String skipJobRegExp = Configuration.getCombinedConfiguration().getProperty(PROPKEY_alarm_RUNNING_LONG_SkipJobName, DEFAULT_alarm_RUNNING_LONG_SkipJobName);

					// note: this must be set to true at start, otherwise all below rules will be disabled (it "stops" processing at first doAlarm==false)
					boolean doAlarm = true;

					// The below could have been done with nested if(keep-db), if(keep-srv), if(!skipDb), if(!skipSrv) doAlarm=true; 
					// Below is more readable, from a variable context point-of-view, but HARDER to understand
					doAlarm = (doAlarm && (StringUtil.isNullOrBlank(keepSrvRegExp) ||   dbmsSrvName.matches(keepSrvRegExp))); //    matches the KEEP Srv regexp
					doAlarm = (doAlarm && (StringUtil.isNullOrBlank(keepJobRegExp) ||   job_name   .matches(keepJobRegExp))); //    matches the KEEP Job regexp

					doAlarm = (doAlarm && (StringUtil.isNullOrBlank(skipSrvRegExp) || ! dbmsSrvName.matches(skipSrvRegExp))); // NO match in the SKIP Srv regexp
					doAlarm = (doAlarm && (StringUtil.isNullOrBlank(skipJobRegExp) || ! job_name   .matches(skipJobRegExp))); // NO match in the SKIP Job regexp
					
					if (doAlarm)
					{
						String current_step_name    = cm.getAbsString                  (r, "current_step_name");
						String start_execution_date = toYmdHm(cm.getAbsValueAsTimestamp(r, "start_execution_date"));
						String current_runtime_hms  = cm.getAbsString                  (r, "current_runtime_hms");
						
						String extendedDescText = cm.toTextTableString(DATA_ABS, r);
						String extendedDescHtml = cm.toHtmlTableString(DATA_ABS, r, true, false, false);

						// Create the alarm
						AlarmEvent ae = new AlarmEventJobSchedulerLongRunning(cm, job_name, current_step_name, start_execution_date, current_runtime_hms);
						ae.setExtendedDescription(extendedDescText, extendedDescHtml);

						// Set info how we could enable/disable this alarm
//						ae.setDisableAlarmDescription(getDisableAlarmMessage(status, job_name, dbmsSrvName));
						
						// Information about how to disable this alarm
						ae.createAlarmOptionsMessage(this, "RUNNING_LONG");

						alarmHandler.addAlarm( ae );
					}
				}
			}

			//-------------------------------------------------------
			// MISSED
			//-------------------------------------------------------
			if (isSystemAlarmsForColumnEnabledAndInTimeRange("MISSED"))
			{
				String status = cm.getAbsString(r, "status");

				if ("MISSED".equals(status))
				{
					// Get config 'skip/allow'
					String keepSrvRegExp = Configuration.getCombinedConfiguration().getProperty(PROPKEY_alarm_MISSED_ForSrv     , DEFAULT_alarm_MISSED_ForSrv);
					String skipSrvRegExp = Configuration.getCombinedConfiguration().getProperty(PROPKEY_alarm_MISSED_SkipSrv    , DEFAULT_alarm_MISSED_SkipSrv);
					String keepJobRegExp = Configuration.getCombinedConfiguration().getProperty(PROPKEY_alarm_MISSED_ForJobName , DEFAULT_alarm_MISSED_ForJobName);
					String skipJobRegExp = Configuration.getCombinedConfiguration().getProperty(PROPKEY_alarm_MISSED_SkipJobName, DEFAULT_alarm_MISSED_SkipJobName);

					// note: this must be set to true at start, otherwise all below rules will be disabled (it "stops" processing at first doAlarm==false)
					boolean doAlarm = true;

					// The below could have been done with nested if(keep-db), if(keep-srv), if(!skipDb), if(!skipSrv) doAlarm=true; 
					// Below is more readable, from a variable context point-of-view, but HARDER to understand
					doAlarm = (doAlarm && (StringUtil.isNullOrBlank(keepSrvRegExp) ||   dbmsSrvName.matches(keepSrvRegExp))); //    matches the KEEP Srv regexp
					doAlarm = (doAlarm && (StringUtil.isNullOrBlank(keepJobRegExp) ||   job_name   .matches(keepJobRegExp))); //    matches the KEEP Job regexp

					doAlarm = (doAlarm && (StringUtil.isNullOrBlank(skipSrvRegExp) || ! dbmsSrvName.matches(skipSrvRegExp))); // NO match in the SKIP Srv regexp
					doAlarm = (doAlarm && (StringUtil.isNullOrBlank(skipJobRegExp) || ! job_name   .matches(skipJobRegExp))); // NO match in the SKIP Job regexp
					
					if (doAlarm)
					{
						String next_run_datetime = toYmdHm(cm.getAbsValueAsTimestamp(r, "next_run_datetime"));

						String extendedDescText = cm.toTextTableString(DATA_ABS, r);
						String extendedDescHtml = cm.toHtmlTableString(DATA_ABS, r, true, false, false);

						// Create the alarm
						AlarmEvent ae = new AlarmEventJobSchedulerMissed(cm, job_name, next_run_datetime);
						ae.setExtendedDescription(extendedDescText, extendedDescHtml);

						// Information about how to disable this alarm
						ae.createAlarmOptionsMessage(this, "MISSED");

						alarmHandler.addAlarm( ae );
					}
				}
			}

			//-------------------------------------------------------
			// FAILED_LAST_RUN
			//-------------------------------------------------------
			if (isSystemAlarmsForColumnEnabledAndInTimeRange("FAILED_LAST_RUN"))
			{
				String status = cm.getAbsString(r, "status");

				if ("FAILED_LAST_RUN".equals(status))
				{
					// Get config 'skip/allow'
					String keepSrvRegExp = Configuration.getCombinedConfiguration().getProperty(PROPKEY_alarm_FAILED_LAST_RUN_ForSrv     , DEFAULT_alarm_FAILED_LAST_RUN_ForSrv);
					String skipSrvRegExp = Configuration.getCombinedConfiguration().getProperty(PROPKEY_alarm_FAILED_LAST_RUN_SkipSrv    , DEFAULT_alarm_FAILED_LAST_RUN_SkipSrv);
					String keepJobRegExp = Configuration.getCombinedConfiguration().getProperty(PROPKEY_alarm_FAILED_LAST_RUN_ForJobName , DEFAULT_alarm_FAILED_LAST_RUN_ForJobName);
					String skipJobRegExp = Configuration.getCombinedConfiguration().getProperty(PROPKEY_alarm_FAILED_LAST_RUN_SkipJobName, DEFAULT_alarm_FAILED_LAST_RUN_SkipJobName);

					// note: this must be set to true at start, otherwise all below rules will be disabled (it "stops" processing at first doAlarm==false)
					boolean doAlarm = true;

					// The below could have been done with nested if(keep-db), if(keep-srv), if(!skipDb), if(!skipSrv) doAlarm=true; 
					// Below is more readable, from a variable context point-of-view, but HARDER to understand
					doAlarm = (doAlarm && (StringUtil.isNullOrBlank(keepSrvRegExp) ||   dbmsSrvName.matches(keepSrvRegExp))); //    matches the KEEP Srv regexp
					doAlarm = (doAlarm && (StringUtil.isNullOrBlank(keepJobRegExp) ||   job_name   .matches(keepJobRegExp))); //    matches the KEEP Job regexp

					doAlarm = (doAlarm && (StringUtil.isNullOrBlank(skipSrvRegExp) || ! dbmsSrvName.matches(skipSrvRegExp))); // NO match in the SKIP Srv regexp
					doAlarm = (doAlarm && (StringUtil.isNullOrBlank(skipJobRegExp) || ! job_name   .matches(skipJobRegExp))); // NO match in the SKIP Job regexp
					
					if (doAlarm)
					{
						String last_run_datetime = toYmdHm(cm.getAbsValueAsTimestamp(r, "last_run_datetime"));

						String extendedDescText = cm.toTextTableString(DATA_ABS, r);
						String extendedDescHtml = cm.toHtmlTableString(DATA_ABS, r, true, false, false);

						// Create the alarm
						AlarmEvent ae = new AlarmEventJobSchedulerLastRunFailed(cm, job_name, last_run_datetime);
						ae.setExtendedDescription(extendedDescText, extendedDescHtml);
						
						// Information about how to disable this alarm
						ae.createAlarmOptionsMessage(this, "FAILED_LAST_RUN");

						alarmHandler.addAlarm( ae );
					}
				}
			}
		}
	}
	
	private static String toYmdHm(Timestamp ts)
	{
		if (ts == null)
			return "NULL";
		
		return TimeUtils.toStringYmdHm(ts);
	}

	public static final String  PROPKEY_alarm_RUNNING_LONG                = CM_NAME + ".alarm.system.if.status.eq.RUNNING_LONG";
	public static final boolean DEFAULT_alarm_RUNNING_LONG                = true;
	public static final String  PROPKEY_alarm_RUNNING_LONG_ForSrv         = CM_NAME + ".alarm.system.if.status.eq.RUNNING_LONG.for.srv";
	public static final String  DEFAULT_alarm_RUNNING_LONG_ForSrv         = "";
	public static final String  PROPKEY_alarm_RUNNING_LONG_SkipSrv        = CM_NAME + ".alarm.system.if.status.eq.RUNNING_LONG.skip.srv";
	public static final String  DEFAULT_alarm_RUNNING_LONG_SkipSrv        = "";
	public static final String  PROPKEY_alarm_RUNNING_LONG_ForJobName     = CM_NAME + ".alarm.system.if.status.eq.RUNNING_LONG.for.jobName";
	public static final String  DEFAULT_alarm_RUNNING_LONG_ForJobName     = "";
	public static final String  PROPKEY_alarm_RUNNING_LONG_SkipJobName    = CM_NAME + ".alarm.system.if.status.eq.RUNNING_LONG.skip.jobName";
	public static final String  DEFAULT_alarm_RUNNING_LONG_SkipJobName    = "";
	
	public static final String  PROPKEY_alarm_MISSED                      = CM_NAME + ".alarm.system.if.status.eq.MISSED";
	public static final boolean DEFAULT_alarm_MISSED                      = true;
	public static final String  PROPKEY_alarm_MISSED_ForSrv               = CM_NAME + ".alarm.system.if.status.eq.MISSED.for.srv";
	public static final String  DEFAULT_alarm_MISSED_ForSrv               = "";
	public static final String  PROPKEY_alarm_MISSED_SkipSrv              = CM_NAME + ".alarm.system.if.status.eq.MISSED.skip.srv";
	public static final String  DEFAULT_alarm_MISSED_SkipSrv              = "";
	public static final String  PROPKEY_alarm_MISSED_ForJobName           = CM_NAME + ".alarm.system.if.status.eq.MISSED.for.jobName";
	public static final String  DEFAULT_alarm_MISSED_ForJobName           = "";
	public static final String  PROPKEY_alarm_MISSED_SkipJobName          = CM_NAME + ".alarm.system.if.status.eq.MISSED.skip.jobName";
	public static final String  DEFAULT_alarm_MISSED_SkipJobName          = "";
	
	public static final String  PROPKEY_alarm_FAILED_LAST_RUN             = CM_NAME + ".alarm.system.if.status.eq.FAILED_LAST_RUN";
	public static final boolean DEFAULT_alarm_FAILED_LAST_RUN             = true;
	public static final String  PROPKEY_alarm_FAILED_LAST_RUN_ForSrv      = CM_NAME + ".alarm.system.if.status.eq.FAILED_LAST_RUN.for.srv";
	public static final String  DEFAULT_alarm_FAILED_LAST_RUN_ForSrv      = "";
	public static final String  PROPKEY_alarm_FAILED_LAST_RUN_SkipSrv     = CM_NAME + ".alarm.system.if.status.eq.FAILED_LAST_RUN.skip.srv";
	public static final String  DEFAULT_alarm_FAILED_LAST_RUN_SkipSrv     = "";
	public static final String  PROPKEY_alarm_FAILED_LAST_RUN_ForJobName  = CM_NAME + ".alarm.system.if.status.eq.FAILED_LAST_RUN.for.jobName";
	public static final String  DEFAULT_alarm_FAILED_LAST_RUN_ForJobName  = "";
	public static final String  PROPKEY_alarm_FAILED_LAST_RUN_SkipJobName = CM_NAME + ".alarm.system.if.status.eq.FAILED_LAST_RUN.skip.jobName";
	public static final String  DEFAULT_alarm_FAILED_LAST_RUN_SkipJobName = "";
	
	@Override
	public List<CmSettingsHelper> getLocalAlarmSettings()
	{
		Configuration conf = Configuration.getCombinedConfiguration();
		List<CmSettingsHelper> list = new ArrayList<>();

		CmSettingsHelper.Type isAlarmSwitch = CmSettingsHelper.Type.IS_ALARM_SWITCH;
		
		list.add(new CmSettingsHelper("RUNNING_LONG",    isAlarmSwitch, PROPKEY_alarm_RUNNING_LONG               , Boolean.class, conf.getBooleanProperty(PROPKEY_alarm_RUNNING_LONG                  , DEFAULT_alarm_RUNNING_LONG                  ), DEFAULT_alarm_RUNNING_LONG                  , "If 'status' is 'RUNNING_LONG' send Alarm." ));
		list.add(new CmSettingsHelper("RUNNING_LONG ForSrv",            PROPKEY_alarm_RUNNING_LONG_ForSrv        , String .class, conf.getProperty       (PROPKEY_alarm_RUNNING_LONG_ForSrv           , DEFAULT_alarm_RUNNING_LONG_ForSrv           ), DEFAULT_alarm_RUNNING_LONG_ForSrv           , "If 'RUNNING_LONG'; Only for the servers listed (regexp is used, blank=for-all-srv). After this rule the 'skip' rule is evaluated.",      new RegExpInputValidator()));
		list.add(new CmSettingsHelper("RUNNING_LONG SkipSrv",           PROPKEY_alarm_RUNNING_LONG_SkipSrv       , String .class, conf.getProperty       (PROPKEY_alarm_RUNNING_LONG_SkipSrv          , DEFAULT_alarm_RUNNING_LONG_SkipSrv          ), DEFAULT_alarm_RUNNING_LONG_SkipSrv          , "If 'RUNNING_LONG'; Discard servers listed (regexp is used). Before this rule the 'for/keep' rule is evaluated",                          new RegExpInputValidator()));
		list.add(new CmSettingsHelper("RUNNING_LONG ForJobName",        PROPKEY_alarm_RUNNING_LONG_ForJobName    , String .class, conf.getProperty       (PROPKEY_alarm_RUNNING_LONG_ForJobName       , DEFAULT_alarm_RUNNING_LONG_ForJobName       ), DEFAULT_alarm_RUNNING_LONG_ForJobName       , "If 'RUNNING_LONG'; Only for the JobName listed (regexp is used, blank=for-all-jobs). After this rule the 'skip' rule is evaluated.",     new RegExpInputValidator()));
		list.add(new CmSettingsHelper("RUNNING_LONG SkipJobName",       PROPKEY_alarm_RUNNING_LONG_SkipJobName   , String .class, conf.getProperty       (PROPKEY_alarm_RUNNING_LONG_SkipJobName      , DEFAULT_alarm_RUNNING_LONG_SkipJobName      ), DEFAULT_alarm_RUNNING_LONG_SkipJobName      , "If 'RUNNING_LONG'; Discard JobName listed (regexp is used). Before this rule the 'for/keep' rule is evaluated",                          new RegExpInputValidator()));

		list.add(new CmSettingsHelper("MISSED",          isAlarmSwitch, PROPKEY_alarm_MISSED                     , Boolean.class, conf.getBooleanProperty(PROPKEY_alarm_MISSED                        , DEFAULT_alarm_MISSED                        ), DEFAULT_alarm_MISSED                        , "If 'status' is 'MISSED' send Alarm." ));
		list.add(new CmSettingsHelper("MISSED ForSrv",                  PROPKEY_alarm_MISSED_ForSrv              , String .class, conf.getProperty       (PROPKEY_alarm_MISSED_ForSrv                 , DEFAULT_alarm_MISSED_ForSrv                 ), DEFAULT_alarm_MISSED_ForSrv                 , "If 'MISSED'; Only for the servers listed (regexp is used, blank=for-all-srv). After this rule the 'skip' rule is evaluated.",            new RegExpInputValidator()));
		list.add(new CmSettingsHelper("MISSED SkipSrv",                 PROPKEY_alarm_MISSED_SkipSrv             , String .class, conf.getProperty       (PROPKEY_alarm_MISSED_SkipSrv                , DEFAULT_alarm_MISSED_SkipSrv                ), DEFAULT_alarm_MISSED_SkipSrv                , "If 'MISSED'; Discard servers listed (regexp is used). Before this rule the 'for/keep' rule is evaluated",                                new RegExpInputValidator()));
		list.add(new CmSettingsHelper("MISSED ForJobName",              PROPKEY_alarm_MISSED_ForJobName          , String .class, conf.getProperty       (PROPKEY_alarm_MISSED_ForJobName             , DEFAULT_alarm_MISSED_ForJobName             ), DEFAULT_alarm_MISSED_ForJobName             , "If 'MISSED'; Only for the JobName listed (regexp is used, blank=for-all-jobs). After this rule the 'skip' rule is evaluated.",           new RegExpInputValidator()));
		list.add(new CmSettingsHelper("MISSED SkipJobName",             PROPKEY_alarm_MISSED_SkipJobName         , String .class, conf.getProperty       (PROPKEY_alarm_MISSED_SkipJobName            , DEFAULT_alarm_MISSED_SkipJobName            ), DEFAULT_alarm_MISSED_SkipJobName            , "If 'MISSED'; Discard JobName listed (regexp is used). Before this rule the 'for/keep' rule is evaluated",                                new RegExpInputValidator()));

		list.add(new CmSettingsHelper("FAILED_LAST_RUN", isAlarmSwitch, PROPKEY_alarm_FAILED_LAST_RUN            , Boolean.class, conf.getBooleanProperty(PROPKEY_alarm_FAILED_LAST_RUN               , DEFAULT_alarm_FAILED_LAST_RUN               ), DEFAULT_alarm_FAILED_LAST_RUN               , "If 'status' is 'FAILED_LAST_RUN' send Alarm." ));
		list.add(new CmSettingsHelper("FAILED_LAST_RUN ForSrv",         PROPKEY_alarm_FAILED_LAST_RUN_ForSrv     , String .class, conf.getProperty       (PROPKEY_alarm_FAILED_LAST_RUN_ForSrv        , DEFAULT_alarm_FAILED_LAST_RUN_ForSrv        ), DEFAULT_alarm_FAILED_LAST_RUN_ForSrv        , "If 'FAILED_LAST_RUN'; Only for the servers listed (regexp is used, blank=for-all-srv). After this rule the 'skip' rule is evaluated.",   new RegExpInputValidator()));
		list.add(new CmSettingsHelper("FAILED_LAST_RUN SkipSrv",        PROPKEY_alarm_FAILED_LAST_RUN_SkipSrv    , String .class, conf.getProperty       (PROPKEY_alarm_FAILED_LAST_RUN_SkipSrv       , DEFAULT_alarm_FAILED_LAST_RUN_SkipSrv       ), DEFAULT_alarm_FAILED_LAST_RUN_SkipSrv       , "If 'FAILED_LAST_RUN'; Discard servers listed (regexp is used). Before this rule the 'for/keep' rule is evaluated",                       new RegExpInputValidator()));
		list.add(new CmSettingsHelper("FAILED_LAST_RUN ForJobName",     PROPKEY_alarm_FAILED_LAST_RUN_ForJobName , String .class, conf.getProperty       (PROPKEY_alarm_FAILED_LAST_RUN_ForJobName    , DEFAULT_alarm_FAILED_LAST_RUN_ForJobName    ), DEFAULT_alarm_FAILED_LAST_RUN_ForJobName    , "If 'FAILED_LAST_RUN'; Only for the JobName listed (regexp is used, blank=for-all-jobs). After this rule the 'skip' rule is evaluated.",  new RegExpInputValidator()));
		list.add(new CmSettingsHelper("FAILED_LAST_RUN SkipJobName",    PROPKEY_alarm_FAILED_LAST_RUN_SkipJobName, String .class, conf.getProperty       (PROPKEY_alarm_FAILED_LAST_RUN_SkipJobName   , DEFAULT_alarm_FAILED_LAST_RUN_SkipJobName   ), DEFAULT_alarm_FAILED_LAST_RUN_SkipJobName   , "If 'FAILED_LAST_RUN'; Discard JobName listed (regexp is used). Before this rule the 'for/keep' rule is evaluated",                       new RegExpInputValidator()));

		return list;
	}

	
	//--------------------------------------------------------------------------------------
	@Override
	public List<CmHighlighterDescriptor> createHighlighterDescriptors()
	{
		List<CmHighlighterDescriptor> list = new ArrayList<>();

		// GREEN
		list.add(new CmHighlighterDescriptor()
			.name("RUNNING_OK")
			.strEquals("status", "RUNNING_OK")
			.bgColor("#90EE90"));

		// ORANGE
		list.add(new CmHighlighterDescriptor()
			.name("RUNNING_LONG")
			.strEquals("status", "RUNNING_LONG")
			.bgColor("#FFD480"));

		// PINK
		list.add(new CmHighlighterDescriptor()
			.name("MISSED")
			.strEquals("status", "MISSED")
			.bgColor("#FFB6C1"));

		// RED
		list.add(new CmHighlighterDescriptor()
			.name("FAILED_LAST_RUN")
			.strEquals("status", "FAILED_LAST_RUN")
			.bgColor("#FF9999"));

		return list;
	}
	
}
