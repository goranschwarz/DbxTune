/*******************************************************************************
 * Copyright (C) 2010-2019 Goran Schwarz
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
package com.dbxtune.pcs;

import java.lang.invoke.MethodHandles;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.dbxtune.gui.ResultSetTableModel;
import com.dbxtune.sql.conn.ConnectionProp;
import com.dbxtune.sql.conn.DbxConnection;
import com.dbxtune.utils.Configuration;
import com.dbxtune.utils.DbUtils;
import com.dbxtune.utils.StringUtil;

/**
 * Most code for this was reused from SqlServerQueryStoreExtractor...
 */
public class SqlServerJobSchedulerExtractor
extends DbmsExtractorAbstract
{
	private static final Logger _logger = LogManager.getLogger(MethodHandles.lookup().lookupClass());

	public static final String PROPKEY_overview_calcHistoricalDays = "SqlServerJobSchedulerExtractor.overview.calc.historical.days";
	public static final int    DEFAULT_overview_calcHistoricalDays = 30;
	
	public static final String PROPKEY_outliers_calcHistoricalDays = "SqlServerJobSchedulerExtractor.outliers.calc.historical.days";
	public static final int    DEFAULT_outliers_calcHistoricalDays = 30;
	
	public static final String PROPKEY_search_errorMessage = "SqlServerJobSchedulerExtractor.search.errorMessage";
	public static final String DEFAULT_search_errorMessage = "ERROR-MSG: ";
	
	public static final String EXTRACTOR_NAME  = "SQL Server Job Scheduler Extractor";
	public static final String MON_DB_NAME     = "msdb";
	public static final String PCS_SCHEMA_NAME = "job_scheduler";
	
	/** period: -1 = Everything. above 0 = Number of days to extract */
	private int           _period         = -1;
	private String        _periodStrShort = "";
	private String        _periodStrLong  = "";

	private Timestamp     _periodStartTimeLocal;
	private Timestamp     _periodStopTimeLocal;
	private Timestamp     _periodStartTimeUtc;
	private Timestamp     _periodStopTimeUtc;

	public enum SqlAgentInfo
	{
		 sysjobs              // Most columns from: msdb.dbo.sysjobs  
		,sysjobsteps          // Most columns from: msdb.dbo.sysjobsteps + some extra aggregated column like 'disabled_by_step, next_step'
		,sysjobhistory        // Most columns from: msdb.dbo.sysjobhistory + some extra aggregated column like 'run_status_desc, run_ts, run_duration_sec'

		,job_history_overview_all // All 
		,job_history_overview     // How many executions Jobs and Steps that had been executed by the scheduler.
		,job_history_outliers     // Get info about jobs that is running above the "normal" average...
		,job_history_errors       // Get info about jobs that is has errors 
	};

	
	public SqlServerJobSchedulerExtractor(int period, DbxConnection monConn, DbxConnection pcsConn)
	{
		super(EXTRACTOR_NAME, monConn, MON_DB_NAME, pcsConn, PCS_SCHEMA_NAME);

		_period     = period;

		if (_period == 0)
			_period = -1;

		_periodStrShort = "everything";
		if (_period == 1)
			_periodStrShort = "this day";
		else if (_period > 1)
			_periodStrShort = "last " + _period + " days";
	}
	
	@Override
	protected void onTransferStart()
	throws SQLException
	{
		// Get recording start time
		setPeriodStartAndEndTime();

		// getPeriodInfo() will be used to pick up the below when calling: onTransferStart()
		_periodStrLong = "StartTimeUTC='" + _periodStartTimeUtc + "', EndTimeUTC='" + _periodStopTimeUtc + "', StartTimeLocal='" + _periodStartTimeLocal + "', EndTimeLocal='" + _periodStopTimeLocal + "'.";

		// And do "normal thing"
		super.onTransferStart();
	}

	@Override
	protected String getPeriodInfoShort()
	{
		return _periodStrShort;
	}

	@Override
	protected String getPeriodInfoLong()
	{
		return _periodStrLong;
	}
		
	@Override
	protected List<ExtractorEntry> createExtractors() 
	{
		List<ExtractorEntry> list = new ArrayList<>();

		list.add( new sysjobs()       );
		list.add( new sysjobsteps()   );
		list.add( new sysjobhistory() );

		list.add( new job_history_overview_all() );
		list.add( new job_history_overview()     );
		list.add( new job_history_outliers()     );
		list.add( new job_history_errors()       );
		
		return list;
	}	
	
	//--------------------------------------------------------------------
	//-- sysjobs
	//--------------------------------------------------------------------
	public class sysjobs implements ExtractorEntry
	{
		@Override public String           getTableName()        { return this.getClass().getSimpleName(); }
//		@Override public String           getTableName()        { return "sysjobs"; }
		@Override public ExtractionMethod getExtractionMethod() { return ExtractionMethod.STREAMING; }
		@Override public List<String>     getCreateIndex()      { return Collections.emptyList(); }
		
		@Override
		public String getSql()
		{
			String sql = ""
				    + "SELECT \n"
				    + "     job_id = cast(j.job_id as varchar(40)) \n"
				    + "    ,j.name \n"
				    + "    ,j.enabled \n"
				    + "    ,j.description \n"
				    + "    ,j.start_step_id \n"
				    + "    ,j.category_id \n"
				    + "    ,owner = SUSER_SNAME(j.owner_sid) \n"
				    + "    ,j.date_created \n"
				    + "    ,j.date_modified \n"
				    + "    ,j.version_number \n"
				    + "FROM msdb.dbo.sysjobs j \n"
				    + "";
			
			return sql;
		}
	}

	//--------------------------------------------------------------------
	//-- sysjobsteps
	//--------------------------------------------------------------------
	public class sysjobsteps implements ExtractorEntry
	{
		@Override public String           getTableName()        { return this.getClass().getSimpleName(); }
//		@Override public String           getTableName()        { return "sysjobsteps"; }
		@Override public ExtractionMethod getExtractionMethod() { return ExtractionMethod.STREAMING; }
		@Override public List<String>     getCreateIndex()      { return Collections.emptyList(); }
		
		@Override
		public String getSql()
		{
			String sql = ""
				    + "SELECT \n"
				    + "     job_id = cast(js.job_id as varchar(40)) \n"
				    + "    ,js.step_id \n"
				    + "    ,sdi.disabled_by_step \n"
				    + "    ,sdi.next_step \n"
				    + "    ,js.step_name \n"
				    + "    ,js.subsystem \n"
				    + "    ,js.command \n"
				    + "    ,js.cmdexec_success_code \n"
				    + "    ,js.on_success_action \n"
				    + "    ,js.on_success_step_id \n"
				    + "    ,js.on_fail_action \n"
				    + "    ,js.on_fail_step_id \n"
				    + "    ,js.server \n"
				    + "    ,js.database_name \n"
				    + "    ,js.database_user_name \n"
				    + "    ,js.retry_attempts \n"
				    + "    ,js.retry_interval \n"
				    + "    ,js.os_run_priority \n"
				    + "    ,js.output_file_name \n"
				    + "    ,js.last_run_outcome \n"
				    + "    ,js.last_run_duration \n"
				    + "    ,js.last_run_retries \n"
				    + "    ,js.last_run_date \n"
				    + "    ,js.last_run_time \n"
				    + "    ,js.proxy_id \n"
				    + "    ,step_uid = cast(js.step_uid as varchar(40)) \n"
				    + "FROM msdb.dbo.sysjobsteps js \n"
				    + "CROSS APPLY ( \n"
				    + "    SELECT \n"
				    + "         next_step        = max(js2.on_success_step_id) \n"
				    + "        ,disabled_by_step = min(js2.step_id) \n"
				    + "    FROM msdb.dbo.sysjobsteps js2 \n"
				    + "    WHERE 1 = 1 \n"
				    + "      AND js2.job_id              = js.job_id \n"
				    + "      AND js2.step_id             < js.step_id \n"
				    + "      AND js2.on_success_step_id  > js.step_id \n"
				    + "      AND js2.on_success_step_id <> 0 \n"
				    + ") AS sdi /* Step Disable Info */ \n"
				    + "";
			
			return sql;
		}
	}

	//--------------------------------------------------------------------
	//-- sysjobhistory
	//--------------------------------------------------------------------
	public class sysjobhistory implements ExtractorEntry
	{
		@Override public String           getTableName()        { return this.getClass().getSimpleName(); }
//		@Override public String           getTableName()        { return "sysjobhistory"; }
		@Override public ExtractionMethod getExtractionMethod() { return ExtractionMethod.STREAMING; }
		@Override public List<String>     getCreateIndex()      { return Collections.emptyList(); }
		
		@Override
		public String getSql()
		{
			String sql = ""
				    + "SELECT \n"
				    + "     jh.instance_id \n"
				    + "    ,job_id = cast(jh.job_id as varchar(40)) \n"
				    + "    ,jh.step_id \n"
				    + "    ,jh.step_name \n"
				    + "    ,jh.sql_message_id \n"
				    + "    ,jh.sql_severity \n"
				    + "    ,jh.message \n"
				    + "    ,jh.run_status \n"                // decode this later on
				    + "    ,jh.run_date \n"                  // Note: integer in format yyyyMMdd  -- this is decoded later in column 'run_ts'           as a "datetime"  
				    + "    ,jh.run_time \n"                  // Note: integer in format HHmmss    -- this is decoded later in column 'run_ts'           as a "datetime"
				    + "    ,jh.run_duration \n"              // Note: integer in format HHmmss    -- this is decoded later in column 'run_duration_sec' as a "int" with number of *seconds* 
				    + "    ,jh.operator_id_emailed \n"
				    + "    ,jh.operator_id_netsent \n"
				    + "    ,jh.operator_id_paged \n"
				    + "    ,jh.retries_attempted \n"
				    + "    ,jh.server \n"

				            // Decode some of the above columns to make it easer to read them...
				    + "    ,run_status_desc = \n"
				    + "        CASE \n"
				    + "            WHEN jh.run_status = 0 THEN 'FAILED' \n"
				    + "            WHEN jh.run_status = 1 THEN 'SUCCESS' \n"
				    + "            WHEN jh.run_status = 2 THEN 'RETRY' \n"
				    + "            WHEN jh.run_status = 3 THEN 'CANCELED' \n"
				    + "            WHEN jh.run_status = 4 THEN 'IN PROGRESS' \n"
				    + "            ELSE '-UNKNOWN-' + cast(jh.run_status as varchar(10)) + '-' \n"
				    + "        END \n"
				    + "    ,run_ts = convert(datetime, convert(varchar(8), jh.run_date)) \n"
				    + "                              + ' ' \n"
				    + "                              + stuff(stuff(right(1000000 + jh.run_time \n"
				    + "                                                 ,6) \n"
				    + "                                            ,3,0,':') \n"
				    + "                                      ,6,0,':') \n"
				    + "    ,run_duration_sec = jh.run_duration / 10000 * 3600 \n"
				    + "                      + jh.run_duration % 10000 / 100 * 60 \n"
				    + "                      + jh.run_duration % 100 \n"
				    
				    + "FROM msdb.dbo.sysjobhistory jh \n"
				    + "";
			
			return sql;
		}
	}




	//--------------------------------------------------------------------
	//-- job_history_overview_all
	//--------------------------------------------------------------------
	public class job_history_overview_all implements ExtractorEntry
	{
		@Override public String           getTableName()        { return this.getClass().getSimpleName(); }
//		@Override public String           getTableName()        { return "job_history_overview_all"; }
		@Override public ExtractionMethod getExtractionMethod() { return ExtractionMethod.RSTM; }
		@Override public List<String>     getCreateIndex()      { return Collections.emptyList(); }
		
		@Override
		public String getSql()
		{
			String allExecTimes = "";
//			String allExecTimes = "        ,allExecTimes  = 'Only available in SQL Server 2017 or later' \n";
//			DbmsVersionInfo verInfo = _monConn.getDbmsVersionInfo();
//			if (verInfo != null && verInfo.getLongVersion() >= Ver.ver(2017))
//			{
//				allExecTimes = ""
//    				    + "        ,allExecTimes  = STRING_AGG( \n"
//    				    + "                             CAST( \n"
//    				    + "                                 'ts=' + convert(varchar(30), start_execution_date, 120) \n"
//    				    + "                               + ', wd=' + cast(datename(weekday, start_execution_date) as char(9)) \n"
//    				    + "                               + ', HMS=' + convert(varchar(10), dateadd(second, datediff(second, start_execution_date, stop_execution_date), '2000-01-01'), 8) \n"
//    				    + "                               + ', sec=' + cast(datediff(second, start_execution_date, stop_execution_date) as varchar(20)) \n"
//    				    + "                               + ', status=' + 'UNKNOWN' \n"
//    				    + "                               + ';' \n"
//    				    + "                               as varchar(max)) \n"
//    				    + "                           ,char(10) \n"
//    				    + "                         ) WITHIN GROUP (ORDER BY start_execution_date) \n"
//						;
//			}
			
//			String sql = ""
//				    + "WITH jobactivity \n"
//				    + "as ( \n"
//				    + "    select \n"
//				    + "         job_id \n"
//				    + "        ,avg_exec_seconds = avg(datediff(second, start_execution_date, stop_execution_date)) \n"
//				    + "        ,min_exec_seconds = min(datediff(second, start_execution_date, stop_execution_date)) \n"
//				    + "        ,max_exec_seconds = max(datediff(second, start_execution_date, stop_execution_date)) \n"
//				    + "        ,first_started = min(start_execution_date) \n"
//				    + "        ,last_started  = max(start_execution_date) \n"
//				    + "        ,next_scheduled_run_date  = max(next_scheduled_run_date) \n"
//				    + "        ,exec_count = count(*) \n"
//				    + allExecTimes
//				    + "    from msdb.dbo.sysjobactivity \n"
//				    + "    group by job_id \n"
//				    + ") \n"
//				    + "select \n"
//				    + "     job_name = j.name \n"
//				    + "    ,j.enabled \n"
//				    + " \n"
//				    + "    ,a.exec_count \n"
//				    + "    ,step_count = (select count(*) from msdb.dbo.sysjobsteps s where s.job_id = a.job_id) \n"
//				    + "    ,a.last_started \n"
//				    + "    ,days_since_last_exec = datediff(day, a.last_started, getdate()) \n"
//				    + "    ,avg_exec_hms = convert(varchar(10), dateadd(second, a.avg_exec_seconds, '2000-01-01'), 8) \n"
//				    + "    ,max_exec_hms = convert(varchar(10), dateadd(second, a.max_exec_seconds, '2000-01-01'), 8) \n"
//				    + "    ,min_exec_hms = convert(varchar(10), dateadd(second, a.min_exec_seconds, '2000-01-01'), 8) \n"
//				    + "    ,a.avg_exec_seconds \n"
//				    + "    ,a.max_exec_seconds \n"
//				    + "    ,a.min_exec_seconds \n"
//				    + "    ,a.next_scheduled_run_date \n"
//				    + "    ,next_scheduled_in_days = CASE WHEN j.enabled = 0 THEN NULL ELSE datediff(day, getdate(), a.next_scheduled_run_date) END \n"
//				    + "    ,a.first_started \n"
////				    + "    ,a.allExecTimes \n"
//				    + " \n"
//				    + "    ,j.date_created \n"
//				    + "    ,j.date_modified \n"
//				    + "    ,last_modified_in_days = datediff(day, j.date_modified, getdate()) \n"
//				    + "    ,j.version_number \n"
//				    + "    ,j.description \n"
//				    + "    ,job_id = cast(a.job_id as varchar(40)) \n"
//				    + "from jobactivity a \n"
//				    + "inner join msdb.dbo.sysjobs j on a.job_id = j.job_id \n"
//				    + "order by 2 desc, 1 \n"
//				    + "";

			String sql = ""
				    + "WITH history AS \n"
				    + "( \n"
				    + "    SELECT \n"
				    + "         job_id \n"
				    + "        ,jh.run_status \n"
				    + "        ,run_ts = convert(datetime, convert(varchar(8), jh.run_date)) \n"
				    + "                                  + ' ' \n"
				    + "                                  + stuff(stuff(right(1000000 + jh.run_time \n"
				    + "                                                     ,6) \n"
				    + "                                                ,3,0,':') \n"
				    + "                                          ,6,0,':') \n"
				    + "        ,run_duration_sec = jh.run_duration / 10000 * 3600 \n"
				    + "                          + jh.run_duration % 10000 / 100 * 60 \n"
				    + "                          + jh.run_duration % 100 \n"
				    + "    FROM msdb.dbo.sysjobhistory jh \n"
				    + "    WHERE jh.step_id = 0 \n"
				    + ") \n"
				    + ",job_info AS \n"
				    + "( \n"
				    + "    SELECT \n"
				    + "        job_id \n"
				    + "        ,exec_count        = COUNT(*) \n"
				    + "        ,sum_exec_seconds  = SUM(run_duration_sec) \n"
				    + "        ,avg_exec_seconds  = AVG(run_duration_sec) \n"
				    + "        ,min_exec_seconds  = MIN(run_duration_sec) \n"
				    + "        ,max_exec_seconds  = MAX(run_duration_sec) \n"
				    + "        ,first_started     = MIN(run_ts) \n"
				    + "        ,last_started      = MAX(run_ts) \n"
				    + "        ,run_fail_count    = SUM(CASE WHEN h.run_status = 0 THEN 1 ELSE 0 END) \n"
				    + "        ,run_success_count = SUM(CASE WHEN h.run_status = 1 THEN 1 ELSE 0 END) \n"
				    + "        ,run_retry_count   = SUM(CASE WHEN h.run_status = 2 THEN 1 ELSE 0 END) \n"
				    + "        ,run_cancel_count  = SUM(CASE WHEN h.run_status = 3 THEN 1 ELSE 0 END) \n"
				    + "    FROM history h \n"
				    + "    GROUP BY job_id \n"
				    + ") \n"
				    + ",next_job_info AS \n"
				    + "( \n"
					+ "    /*** Only get the FIRST/next to to be sceduled... NOTE: The job can be assigned to MANY schedulers ***/ \n"
		    	    + "    SELECT * \n"
		    	    + "    FROM ( \n"
		    	    + "        SELECT \n"
		    	    + "             sh.job_id \n"
		    	    + "            ,sh.schedule_id \n"
//		    	    + "            ,next_scheduled_ts = convert(datetime, convert(varchar(8), sh.next_run_date)) + ' ' + stuff(stuff(right(1000000 + sh.next_run_time,6),3,0,':'),6,0,':') \n"
		    	    + "            ,next_scheduled_ts = CASE WHEN sh.next_run_date <= 0 THEN NULL ELSE convert(datetime, convert(varchar(8), sh.next_run_date)) + ' ' + stuff(stuff(right(1000000 + sh.next_run_time,6),3,0,':'),6,0,':') END \n" // USE This line if we want 'next_scheduled_ts' to be NULL when 'sh.next_run_date' is 0 (not initialized)... Then also remove the 'WHERE sh.next_run_date > 0' at the end. 
		    	    + "            ,scheduler_name    = (SELECT s.name FROM msdb.dbo.sysschedules s WHERE s.schedule_id = sh.schedule_id) \n"
		    	    + "            ,scheduler_count   = COUNT(*)     OVER (PARTITION BY job_id) \n"
		    	    + "            ,row_num           = ROW_NUMBER() OVER (PARTITION BY job_id ORDER BY next_run_date, next_run_time) \n"
		    	    + "        FROM msdb.dbo.sysjobschedules sh \n"
//		    	    + "        WHERE sh.next_run_date > 0 \n" // next_run_date might be 0 -- The Sysjobschedules will always show 0 until it gets refreshed  
		    	    + "    ) js \n"
		    	    + "    WHERE js.row_num = 1 \n"
				    + ") \n"
				    + "SELECT \n"
				    + "     job_name = jobs.name \n"
				    + "    ,jobs.enabled \n"
				    + "    ,ji.exec_count \n"
//				    + "    ,details = '-filled-in-by-dsr-' \n"
//				    + "    ,execGraph = '-filled-in-by-dsr-' \n"
				    + "    ,step_count = (SELECT count(*) FROM msdb.dbo.sysjobsteps s WHERE s.job_id = jobs.job_id) \n"
//				    + "    ,stepCmds = '-filled-in-by-dsr-' \n"
				    + "    ,ji.last_started \n"
				    + "    ,days_since_last_start = datediff(day, ji.last_started, getdate()) \n"
//				    + "    ,sum_exec_hms = convert(varchar(10), dateadd(second, ji.sum_exec_seconds, '2000-01-01'), 8) \n" // NOTE: This will overflow and not show #DAYS ... so calculate this at client instead
//				    + "    ,avg_exec_hms = convert(varchar(10), dateadd(second, ji.avg_exec_seconds, '2000-01-01'), 8) \n" // NOTE: This will overflow and not show #DAYS ... so calculate this at client instead
//				    + "    ,max_exec_hms = convert(varchar(10), dateadd(second, ji.max_exec_seconds, '2000-01-01'), 8) \n" // NOTE: This will overflow and not show #DAYS ... so calculate this at client instead
//				    + "    ,min_exec_hms = convert(varchar(10), dateadd(second, ji.min_exec_seconds, '2000-01-01'), 8) \n" // NOTE: This will overflow and not show #DAYS ... so calculate this at client instead
				    + "    ,ji.sum_exec_seconds \n"
				    + "    ,ji.avg_exec_seconds \n"
				    + "    ,ji.max_exec_seconds \n"
				    + "    ,ji.min_exec_seconds \n"
				    + " \n"
				    + "    ,next_scheduled_ts      = CASE WHEN jobs.enabled = 1 THEN nj.next_scheduled_ts                           ELSE NULL END \n"
				    + "    ,next_scheduled_in_days = CASE WHEN jobs.enabled = 1 THEN datediff(day, getdate(), nj.next_scheduled_ts) ELSE NULL END \n"
				    + "    ,nj.scheduler_name \n"
				    + "    ,nj.scheduler_count \n"
				    + " \n"
//				    + "    ,allExecTimes      = '-filled-in-by-dsr-' \n"
//				    + "    ,allExecTimesChart = '-filled-in-by-dsr-' \n"
				    + " \n"
				    + "    ,ji.first_started \n"
				    + "    ,jobs.date_created \n"
				    + "    ,jobs.date_modified \n"
				    + "    ,last_modified_in_days = datediff(day, jobs.date_modified, getdate()) \n"
				    + "    ,jobs.version_number \n"
				    + " \n"
				    + "    ,notify_eventlog_on = CASE jobs.notify_level_eventlog WHEN 0 THEN '-off-' WHEN 1 THEN 'SUCCESS' WHEN 2 THEN 'FAILURE' WHEN 3 THEN 'COMPLETION' ELSE '-unknown-' END \n"
				    + "    ,notify_mail_on     = CASE jobs.notify_level_email    WHEN 0 THEN '-off-' WHEN 1 THEN 'SUCCESS' WHEN 2 THEN 'FAILURE' WHEN 3 THEN 'COMPLETION' ELSE '-unknown-' END \n"
				    + "    ,notify_mail        = (SELECT o.name + ': ' + o.email_address   FROM msdb.dbo.sysoperators o WHERE o.id = notify_email_operator_id) \n"
				    + "    ,notify_netsend_on  = CASE jobs.notify_level_netsend  WHEN 0 THEN '-off-' WHEN 1 THEN 'SUCCESS' WHEN 2 THEN 'FAILURE' WHEN 3 THEN 'COMPLETION' ELSE '-unknown-' END \n"
				    + "    ,notify_netsend     = (SELECT o.name + ': ' + o.netsend_address FROM msdb.dbo.sysoperators o WHERE o.id = notify_email_operator_id) \n"
				    + "    ,notify_page_on     = CASE jobs.notify_level_page     WHEN 0 THEN '-off-' WHEN 1 THEN 'SUCCESS' WHEN 2 THEN 'FAILURE' WHEN 3 THEN 'COMPLETION' ELSE '-unknown-' END \n"
				    + "    ,notify_page        = (SELECT o.name + ': ' + o.pager_address   FROM msdb.dbo.sysoperators o WHERE o.id = notify_email_operator_id) \n"
				    + " \n"
				    + "    ,job_description    = jobs.description \n"
				    + "    ,job_category       = (SELECT c.name FROM msdb.dbo.syscategories c WHERE c.category_id = jobs.category_id) \n"
				    + "    ,job_id             = CAST(jobs.job_id as varchar(40)) \n"
				    + " \n"
				    + "FROM msdb.dbo.sysjobs jobs \n"
				    + "LEFT OUTER JOIN job_info      ji ON ji.job_id = jobs.job_id \n"
				    + "LEFT OUTER JOIN next_job_info nj ON nj.job_id = jobs.job_id \n"
				    + "ORDER BY jobs.enabled DESC, job_name \n"
				    + "-------------------------------------------- \n"
				    + "";
			
			return sql;
		}
	}

	//--------------------------------------------------------------------
	//-- job_history_overview
	//--------------------------------------------------------------------
	public class job_history_overview implements ExtractorEntry
	{
		@Override public String           getTableName()        { return this.getClass().getSimpleName(); }
//		@Override public String           getTableName()        { return "job_history_overview"; }
		@Override public ExtractionMethod getExtractionMethod() { return ExtractionMethod.RSTM; }
		@Override public List<String>     getCreateIndex()      { return Collections.emptyList(); }
		
		@Override
		public String getSql()
		{
			int overview_calcHistoricalDays = Configuration.getCombinedConfiguration().getIntProperty(PROPKEY_overview_calcHistoricalDays, DEFAULT_overview_calcHistoricalDays);

			String allExecTimes = "";
//			String allExecTimes = "        ,allExecTimes  = 'Only available in SQL Server 2017 or later' \n";
//			DbmsVersionInfo verInfo = _monConn.getDbmsVersionInfo();
//			if (verInfo != null && verInfo.getLongVersion() >= Ver.ver(2017))
//			{
//				allExecTimes = ""
//					    + "        ,allExecTimes  = STRING_AGG( \n"
//					    + "                             CAST( \n"
//					    + "                                 'ts=' + convert(varchar(30), date_executed, 120) \n"
//					    + "                               + ', wd=' + cast(datename(weekday, date_executed) as char(9)) \n"
//					    + "                               + ', HMS=' + convert(varchar(10), dateadd(second, secs_duration, '2000-01-01'), 8) \n"
//					    + "                               + ', sec=' + cast(secs_duration as varchar(20)) \n"
//					    + "                               + ', status=' + run_status_desc \n"
//					    + "                               + ';' \n"
//					    + "                               as varchar(max)) \n"
//					    + "                           ,char(10) \n"
//					    + "                         ) WITHIN GROUP (ORDER BY date_executed) \n"
//						;
//			}
			
			String sql = ""
				    + "DECLARE @todayBeginTs      datetime = '" + _periodStartTimeLocal      + "' \n"
				    + "DECLARE @historyDayCount   int      = "  + overview_calcHistoricalDays + " \n"
				    + "DECLARE @todayEndTs        datetime = getdate() \n"
				    + "DECLARE @lastXDaysBeginTs  datetime = dateadd(day, -@historyDayCount, @todayBeginTs) \n"
				    + "DECLARE @lastXDaysEndTs    datetime = getdate() \n"
				    + " \n"
				    + ";WITH JobHistData AS \n"
				    + "( \n"
				    + "    SELECT \n"
				    + "         job_id \n"
				    + "        ,run_status_desc = \n"
				    + "            CASE \n"
				    + "                WHEN run_status = 0 THEN 'FAILED' \n"
				    + "                WHEN run_status = 1 THEN 'SUCCESS' \n"
				    + "                WHEN run_status = 2 THEN 'RETRY' \n"
				    + "                WHEN run_status = 3 THEN 'CANCELED' \n"
				    + "                WHEN run_status = 4 THEN 'IN PROGRESS' \n"
				    + "                ELSE '-UNKNOWN-' + cast(run_status as varchar(10)) + '-' \n"
				    + "            END \n"
				    + "        ,date_executed=convert(datetime,convert(varchar(8),run_date)) \n"
				    + "                                       +' ' \n"
				    + "                                       +stuff(stuff(right(1000000+run_time \n"
				    + "                                                         ,6) \n"
				    + "                                                    ,3,0,':') \n"
				    + "                                              ,6,0,':') \n"
				    + "        ,secs_duration=run_duration/10000*3600 \n"
				    + "                      +run_duration%10000/100*60 \n"
				    + "                      +run_duration%100 \n"
				    + "    FROM msdb.dbo.sysjobhistory \n"
				    + "    WHERE step_id=0     /* Job Outcome */ \n"
//				    + "      AND run_status=1  /* Succeeded */ \n"
				    + ") \n"
				    + ",lastXHours as \n"
				    + "( \n"
				    + "    SELECT \n"
				    + "         job_id \n"
				    + "        ,runStatusDesc = max(run_status_desc) \n"  //TODO: Possibly show: SUCCESS/FAILED or even better: SUCCESS(count), FAILED(count), ...  possibly with STRING_AGG(distinct run_status_desc)
				    + "        ,sumDuration   = sum(secs_duration) \n"
				    + "        ,avgDuration   = avg(secs_duration) \n"
				    + "        ,avgPlus2StDev = avg(secs_duration) + (2 * stdevp(secs_duration)) \n"
				    + "        ,minDuration   = min(secs_duration) \n"
				    + "        ,maxDuration   = max(secs_duration) \n"
				    + "        ,recordCount   = count(*) \n"
				    + "        ,stdevp        = stdevp(secs_duration) \n"
				    + "        ,firstExecTime = min(date_executed) \n"
				    + "        ,lastExecTime  = max(date_executed) \n"
				    + "    FROM JobHistData \n"
				    + "    WHERE date_executed >= @todayBeginTs \n"
				    + "      AND date_executed <  @todayEndTs \n"
//				    + "      AND run_status_desc = 'SUCCESS' \n"   // POSSIBLY if not using: AND run_status=1  /* Succeeded */
				    + "    GROUP BY job_id \n"
				    + ") \n"
				    + ",lastXDays as \n"
				    + "( \n"
				    + "    SELECT \n"
				    + "         job_id \n"
				    + "        ,avgDuration   = avg(secs_duration) \n"
				    + "        ,avgPlus2StDev = avg(secs_duration) + (2 * stdevp(secs_duration)) \n"
				    + "        ,minDuration   = min(secs_duration) \n"
				    + "        ,maxDuration   = max(secs_duration) \n"
				    + "        ,recordCount   = count(*) \n"
				    + "        ,daysCount     = count(distinct cast(date_executed as date)) \n"
				    + "        ,stdevp        = stdevp(secs_duration) \n"
				    + "        ,firstExecTime = min(date_executed) \n"
				    + "        ,lastExecTime  = max(date_executed) \n"
				    + allExecTimes // Note: Only available in 2017 and above... when STRING_AGG was introduced
				    + "    FROM JobHistData \n"
				    + "    WHERE date_executed >= @lastXDaysBeginTs \n"
				    + "      AND date_executed <  @lastXDaysEndTs \n"
//				    + "      AND run_status_desc = 'SUCCESS' \n"   // POSSIBLY if not using: AND run_status=1  /* Succeeded */
				    + "    GROUP BY job_id \n"
				    + ") \n"
				    + "SELECT \n"
				    + "     JobName           = j.name \n"
//				    + "--  ,execTime          = today.date_executed \n"
				    + "    ,stepCount         = (SELECT count(*) FROM msdb.dbo.sysjobsteps js WHERE js.job_id = today.job_id) \n"
				    + "    ,stepCmds          = 'FILLED_IN_AT_RUNTIME' \n"
				    + "    ,runStatusDesc     = today.runStatusDesc \n"
				    + "    ,execCount         = today.recordCount \n"
				    + "    ,sumTimeInSec      = today.sumDuration \n"
				    + "    ,sumTime_HMS       = convert(varchar(10), dateadd(second, today.sumDuration, '2000-01-01'), 8) \n"
				    + "    ,avgTimeInSec      = today.avgDuration \n"
				    + "    ,avgTime_HMS       = convert(varchar(10), dateadd(second, today.avgDuration, '2000-01-01'), 8) \n"
				    + "    ,minTime_HMS       = convert(varchar(10), dateadd(second, today.minDuration, '2000-01-01'), 8) \n"
				    + "    ,maxTime_HMS       = convert(varchar(10), dateadd(second, today.maxDuration, '2000-01-01'), 8) \n"
				    + "    ,stdevp            = CAST(today.stdevp as numeric(10,1)) \n"
				    + "    ,firstExecTime     = today.firstExecTime \n"
				    + "    ,lastExecTime      = today.lastExecTime \n"
				    + " \n"
//				    + "--    ,histDays          = @historyDayCount \n"
				    + "    ,histExecCount     = hist.recordCount \n"
				    + "    ,histDaysCount     = hist.daysCount \n"
				    + "    ,histFirstExecTime = hist.firstExecTime \n"
				    + "    ,histAvgTimeInSec  = hist.avgDuration \n"
				    + "    ,histAvgTime_HMS   = convert(varchar(10), dateadd(second, hist.avgDuration, '2000-01-01'), 8) \n"
				    + "    ,histMinTime_HMS   = convert(varchar(10), dateadd(second, hist.minDuration, '2000-01-01'), 8) \n"
				    + "    ,histMaxTime_HMS   = convert(varchar(10), dateadd(second, hist.maxDuration, '2000-01-01'), 8) \n"
				    + "    ,histStdevp        = CAST(hist.stdevp as numeric(10,1)) \n"
//				    + "    ,histAllExecTimes  = hist.allExecTimes \n"
				    + " \n"
				    + "    ,job_id            = cast(j.job_id as varchar(40)) \n"
//				    + "    ,job_id            = j.job_id \n"  // This does NOT work for the moment... FIXME: The JDBC type mapping
				    + " \n"
				    + "FROM lastXHours       today \n"
				    + "JOIN lastXDays        hist ON today.job_id = hist.job_id \n"
				    + "JOIN msdb.dbo.sysjobs j    ON today.job_id = j.job_id \n"
				    + "ORDER BY today.avgDuration DESC \n"
				    + "";
			
			return sql;
		}
	}

	//--------------------------------------------------------------------
	//-- job_history_outliers
	//--------------------------------------------------------------------
	public class job_history_outliers implements ExtractorEntry
	{
		@Override public String           getTableName()        { return this.getClass().getSimpleName(); }
//		@Override public String           getTableName()        { return "job_history_outliers"; }
		@Override public ExtractionMethod getExtractionMethod() { return ExtractionMethod.RSTM; }
		@Override public List<String>     getCreateIndex()      { return Collections.emptyList(); }
		
		@Override
		public String getSql()
		{
			int outliers_calcHistoricalDays = Configuration.getCombinedConfiguration().getIntProperty(PROPKEY_outliers_calcHistoricalDays, DEFAULT_outliers_calcHistoricalDays);

			String allExecTimes = "";
//			String allExecTimes = "        ,allExecTimes  = 'Only available in SQL Server 2017 or later' \n";
//			DbmsVersionInfo verInfo = _monConn.getDbmsVersionInfo();
//			if (verInfo != null && verInfo.getLongVersion() >= Ver.ver(2017))
//			{
//				allExecTimes = ""
//					    + "        ,allExecTimes  = STRING_AGG( \n"
//					    + "                             CAST( \n"
//					    + "                                 'ts=' + convert(varchar(30), date_executed, 120) \n"
//					    + "                               + ', wd=' + cast(datename(weekday, date_executed) as char(9)) \n"
//					    + "                               + ', HMS=' + convert(varchar(10), dateadd(second, secs_duration, '2000-01-01'), 8) \n"
//					    + "                               + ', sec=' + cast(secs_duration as varchar(20)) \n"
//					    + "                               + ', status=' + run_status_desc \n"
//					    + "                               + ';' \n"
//					    + "                               as varchar(max)) \n"
//					    + "                           ,char(10) \n"
//					    + "                         ) WITHIN GROUP (ORDER BY date_executed) \n"
//						;
//			}
			
			// This was mostly from https://bradsruminations.blogspot.com/2011/04/sysjobhistory-job_history_outliers.html
			// with some modifications
			String sql = ""
				    + "DECLARE @historyBeginDate   datetime = NULL \n"
				    + "DECLARE @historyEndDate     datetime = NULL \n"
				    + "DECLARE @analysisBeginDate  datetime = '" + _periodStartTimeLocal + "' \n"
//				    + "DECLARE @analysisBeginDate  datetime = NULL \n"
				    + "DECLARE @analysisEndDate    datetime = NULL \n"
				    + "DECLARE @minHistExecutions  int      = 10 \n"
				    + "DECLARE @minAvgSecsDuration int      = 30 \n"
				    + "DECLARE @historyDayCount    int      = "  + outliers_calcHistoricalDays + " \n"
				    + " \n"
//				    + "IF @historyBeginDate  IS NULL SET @historyBeginDate  = '1900-01-01' ; \n"
				    + "IF @historyBeginDate  IS NULL SET @historyBeginDate  = cast(dateadd(day, -@historyDayCount, getdate()) as date); -- XX Days ago \n"
				    + "IF @historyEndDate    IS NULL SET @historyEndDate    = getdate() ; \n"
				    + "IF @analysisBeginDate IS NULL SET @analysisBeginDate = cast(getdate() as date); -- TODAY \n"
				    + "IF @analysisEndDate   IS NULL SET @analysisEndDate   = getdate() ; \n"
				    + " \n"
				    + ";WITH JobHistData AS \n"
				    + "( \n"
				    + "    SELECT \n"
				    + "         job_id \n"
				    + "        ,step_id \n"
				    + "        ,run_status_desc = \n"
				    + "            CASE \n"
				    + "                WHEN run_status = 0 THEN 'FAILED' \n"
				    + "                WHEN run_status = 1 THEN 'SUCCESS' \n"
				    + "                WHEN run_status = 2 THEN 'RETRY' \n"
				    + "                WHEN run_status = 3 THEN 'CANCELED' \n"
				    + "                WHEN run_status = 4 THEN 'IN PROGRESS' \n"
				    + "                ELSE '-UNKNOWN-' + cast(run_status as varchar(10)) + '-' \n"
				    + "            END \n"
				    + "        ,date_executed = convert(datetime, convert(varchar(8), run_date)) \n"
				    + "                                         + ' ' \n"
				    + "                                         + stuff(stuff(right(1000000 + run_time \n"
				    + "                                                            ,6) \n"
				    + "                                                       ,3,0,':') \n"
				    + "                                                 ,6,0,':') \n"
				    + "        ,secs_duration = run_duration / 10000 * 3600 \n"
				    + "                       + run_duration % 10000 / 100 * 60 \n"
				    + "                       + run_duration % 100 \n"
				    + "    FROM msdb.dbo.sysjobhistory \n"
				    + "    WHERE step_id <> 0 \n"
//				    + "--    AND run_status = 1  --Succeeded \n"
				    + ") \n"
				    + ",JobHistStats AS \n"
				    + "( \n"
				    + "    SELECT \n"
				    + "         job_id \n"
				    + "        ,step_id \n"
				    + "        ,avgDuration   = avg(secs_duration) \n"
				    + "        ,avgPlus2StDev = avg(secs_duration) + (2 * stdevp(secs_duration)) \n"
				    + "        ,minDuration   = min(secs_duration) \n"
				    + "        ,maxDuration   = max(secs_duration) \n"
				    + "        ,recordCount   = count(*) \n"
				    + "        ,daysCount     = count(distinct cast(date_executed as date))"
				    + "        ,stdevp        = stdevp(secs_duration) \n"
				    + allExecTimes // Note: Only available in 2017 and above... when STRING_AGG was introduced
				    + "    FROM JobHistData \n"
//				    + "    WHERE date_executed >= dateadd(day,     datediff(day, '1900-01-01', @historyBeginDate), '1900-01-01') \n"
//				    + "      AND date_executed <  dateadd(day, 1 + datediff(day, '1900-01-01', @historyEndDate  ), '1900-01-01') \n"
					+ "    WHERE date_executed >= @historyBeginDate \n"
					+ "      AND date_executed <  @historyEndDate   \n"
				    + "    GROUP BY job_id \n"
				    + "            ,step_id \n"
				    + "    HAVING count(*) >= @minHistExecutions \n"
				    + "       AND avg(secs_duration * 1.0) >= @minAvgSecsDuration \n"
				    + ") \n"
				    + "SELECT \n"
				    + "     execTime         = jd.date_executed \n"
				    + "    ,JobName          = j.name \n"
				    + "    ,jd.step_id \n"
				    + "    ,jd.run_status_desc \n"
				    + "    ,s.step_name \n"
				    + "    ,deviationInPct   = cast((jd.secs_duration - jhs.avgDuration) / (jhs.avgDuration*1.0) * 100.0 as int) \n"
				    + "    ,deviationInSec   = cast(jd.secs_duration - jhs.avgDuration as int) \n"
				    + "    ,timeInSec        = cast(jd.secs_duration as int) \n"
				    + "    ,time_HMS         = convert(varchar(10), dateadd(second, jd.secs_duration, '2000-01-01'), 8) \n"
				    + "    ,histAvgTime_HMS  = convert(varchar(10), dateadd(second, jhs.avgDuration,  '2000-01-01'), 8) \n"
				    + "    ,histMinTime_HMS  = convert(varchar(10), dateadd(second, jhs.minDuration,  '2000-01-01'), 8) \n"
				    + "    ,histMaxTime_HMS  = convert(varchar(10), dateadd(second, jhs.maxDuration,  '2000-01-01'), 8) \n"
				    + "    ,histAvgTimeInSec = cast(jhs.avgDuration   as int) \n"
				    + "    ,histMinTimeInSec = cast(jhs.minDuration   as int) \n"
				    + "    ,histMaxTimeInSec = cast(jhs.maxDuration   as int) \n"
				    + "    ,histCount        = jhs.recordCount \n"
				    + "    ,histDaysCount    = jhs.daysCount \n"
				    + "    ,histStdevp       = cast(jhs.stdevp        as numeric(10,1)) \n"
				    + "    ,thresholdInSec   = cast(jhs.avgPlus2StDev as int) \n"
//				    + "    ,histAllExecTimes = jhs.allExecTimes \n"
				    + "    ,job_id           = cast(jd.job_id         as varchar(40)) \n"
				    + "FROM JobHistData jd \n"
				    + "JOIN JobHistStats jhs       ON jd.job_id = jhs.job_id AND jd.step_id = jhs.step_id \n"
				    + "JOIN msdb.dbo.sysjobs j     ON jd.job_id = j.job_id \n"
				    + "JOIN msdb.dbo.sysjobsteps s ON jd.job_id = s.job_id   AND jd.step_id = s.step_id \n"
//				    + "WHERE date_executed >= dateadd(day,     datediff(day, '1900-01-01', @analysisBeginDate), '1900-01-01') \n"
//				    + "  AND date_executed <  dateadd(day, 1 + datediff(day, '1900-01-01', @analysisEndDate  ), '1900-01-01') \n"
					+ "WHERE date_executed >= @analysisBeginDate \n"
					+ "  AND date_executed <  @analysisEndDate   \n"
				    + "  AND secs_duration >  AvgPlus2StDev \n"
				    + "ORDER BY jd.date_executed \n"
				    + "";
			
			return sql;
		}
	}

	//--------------------------------------------------------------------
	//-- job_history_errors
	//--------------------------------------------------------------------
	public class job_history_errors implements ExtractorEntry
	{
		@Override public String           getTableName()        { return this.getClass().getSimpleName(); }
//		@Override public String           getTableName()        { return "job_history_errors"; }
		@Override public ExtractionMethod getExtractionMethod() { return ExtractionMethod.RSTM; }
		@Override public List<String>     getCreateIndex()      { return Collections.emptyList(); }
		
		@Override
		public String getSql()
		{
			String allExecTimes = "";
//			String allExecTimes = "        ,allExecTimes  = 'Only available in SQL Server 2017 or later' \n";
//			DbmsVersionInfo verInfo = _monConn.getDbmsVersionInfo();
//			if (verInfo != null && verInfo.getLongVersion() >= Ver.ver(2017))
//			{
//				allExecTimes = ""
//					    + "        ,allExecTimes  = STRING_AGG( \n"
//					    + "                             CAST( \n"
//					    + "                                 'ts=' + convert(varchar(30), date_executed, 120) \n"
//					    + "                               + ', wd=' + cast(datename(weekday, date_executed) as char(9)) \n"
//					    + "                               + ', HMS=' + convert(varchar(10), dateadd(second, secs_duration, '2000-01-01'), 8) \n"
//					    + "                               + ', sec=' + cast(secs_duration as varchar(20)) \n"
//					    + "                               + ', status=' + run_status_desc \n"
//					    + "                               + ';' \n"
//					    + "                               as varchar(max)) \n"
//					    + "                           ,char(10) \n"
//					    + "                         ) WITHIN GROUP (ORDER BY date_executed) \n"
//						;
//			}
			
			String searchErrorMessage      = Configuration.getCombinedConfiguration().getProperty(PROPKEY_search_errorMessage, DEFAULT_search_errorMessage);
			String sql_caseWhenMessageLike = "";
			String sql_whereMessageLike    = "";
			if (StringUtil.hasValue(searchErrorMessage)) 
			{
				sql_caseWhenMessageLike = "                WHEN message like '%" + searchErrorMessage + "%' THEN 'WARNING' \n";
				sql_whereMessageLike    = "       OR message like '%" + searchErrorMessage + "%' \n";
			}

			String sql = ""
				    + "DECLARE @periodStartTime datetime = '" + _periodStartTimeLocal + "' \n"
				    + " \n"
				    + ";WITH JobHistData AS \n"
				    + "( \n"
				    + "    SELECT \n"
				    + "         date_executed = convert(datetime, convert(varchar(8), run_date)) \n"
				    + "                                         + ' ' \n"
				    + "                                         + stuff(stuff(right(1000000 + run_time \n"
				    + "                                                            ,6) \n"
				    + "                                                       ,3,0,':') \n"
				    + "                                                 ,6,0,':') \n"
				    + "        ,secs_duration = run_duration / 10000 * 3600 \n"
				    + "                       + run_duration % 10000 / 100 * 60 \n"
				    + "                       + run_duration % 100 \n"
				    + "        ,job_id \n"
				    + "        ,run_status_desc = \n"
				    + "            CASE \n"
				    + "                WHEN run_status = 0 THEN 'FAILED' \n"
				    + "                WHEN message like '%![SQLSTATE %' ESCAPE '!' THEN 'WARNING' \n"
				    + sql_caseWhenMessageLike
				    + "                WHEN run_status = 1 THEN 'SUCCESS' \n"
				    + "                WHEN run_status = 2 THEN 'RETRY' \n"
				    + "                WHEN run_status = 3 THEN 'CANCELED' \n"
				    + "                WHEN run_status = 4 THEN 'IN PROGRESS' \n"
				    + "                ELSE '-UNKNOWN-' + cast(run_status as varchar(10)) + '-' \n"
				    + "            END \n"
				    + "        ,retries_attempted \n"
				    + "        ,step_id \n"
				    + "        ,step_name \n"
				    + "        ,sql_message_id \n"
				    + "        ,sql_severity \n"
				    + "        ,message \n"
				    + "    FROM msdb.dbo.sysjobhistory \n"
				    + "    WHERE step_id <> 0 \n"
				    + "      AND run_status = 0  /* FAILED */ \n"
				    + "       OR sql_message_id <> 0 \n"
				    + "       OR message like '%![SQLSTATE %' ESCAPE '!' /* Escape the '[' character */ \n"
				    + sql_whereMessageLike
				    + ") \n"
//				    + ",JobHistStats AS \n"
//				    + "( \n"
//				    + "    SELECT \n"
//				    + "         job_id \n"
//				    + "        ,step_id \n"
//				    + allExecTimes // Note: Only available in 2017 and above... when STRING_AGG was introduced
//				    + "    FROM JobHistData \n"
//				    + "    GROUP BY job_id \n"
//				    + "            ,step_id \n"
//				    + ") \n"
				    + "SELECT \n"
				    + "     execTime = jd.date_executed \n"
				    + "    ,JobName = j.name \n"
				    + "    ,jd.step_name \n"
				    + "    ,jd.step_id \n"
				    + allExecTimes
				    + "    ,s.subsystem \n"
				    + "    ,jd.run_status_desc \n"
				    + "    ,jd.retries_attempted \n"
				    + "    ,jd.sql_message_id \n"
				    + "    ,jd.sql_severity \n"
				    + "    ,jd.message \n"
					+ "    ,job_id = cast(jd.job_id as varchar(40)) \n"
				    + "FROM JobHistData jd \n"
//				    + "JOIN JobHistStats jhs       ON jd.job_id = jhs.job_id AND jd.step_id = jhs.step_id \n"
				    + "JOIN msdb.dbo.sysjobs j     ON jd.job_id = j.job_id \n"
				    + "JOIN msdb.dbo.sysjobsteps s ON jd.job_id = s.job_id AND jd.step_id = s.step_id \n"
				    + "WHERE jd.date_executed >= @periodStartTime \n"
				    + "ORDER BY jd.date_executed, jd.step_id \n"
				    + "";

			return sql;
		}
	}


	private void setPeriodStartAndEndTime()
	throws SQLException
	{
		Timestamp ts = null;

		String schemaName = null;

		// Start/end time for the recording
		String sql = "select min([SessionSampleTime]) \n" + // or if we want both start/end: select min([SessionSampleTime]), max([SessionSampleTime])
		      "from " + PersistWriterBase.getTableName(getMonConn(), schemaName, PersistWriterBase.SESSION_SAMPLES, null, true) + " \n";

		sql = getPcsConn().quotifySqlString(sql);
		try (Statement stmnt = getPcsConn().createStatement(); ResultSet rs = stmnt.executeQuery(sql))
		{
			while(rs.next())
				ts = rs.getTimestamp(1);
		}

		if (ts != null)
		{
//			_recordingStartTime = ts;
			
			LocalDateTime recStart = ts.toLocalDateTime();

			LocalDateTime periodStartTime = recStart.truncatedTo( ChronoUnit.DAYS );   // set to: YYYY-MM-DD 00:00:00.000000000
			LocalDateTime periodStopTime  = periodStartTime.plusDays(1).minusNanos(1); // set to: YYYY-MM-DD 23:59:59.999999999

			if (_period > 1)
				periodStartTime = periodStartTime.minusDays( _period - 1 );
			
//			_recordingStartTime   = ts;
			_periodStartTimeLocal = Timestamp.valueOf(periodStartTime);
			_periodStopTimeLocal  = Timestamp.valueOf(periodStopTime);
			_periodStartTimeUtc   = Timestamp.valueOf(convertToUtc(periodStartTime));
			_periodStopTimeUtc    = Timestamp.valueOf(convertToUtc(periodStopTime));
			
//System.out.println("------------- setPeriodStartAndEndTime(): _recordingStartTime='" + _recordingStartTime + "', _periodStartTime='" + _periodStartTime + "', _periodStopTime='" + _periodStopTime + "'.");
		}
	}

	/**
	 * Simple test during development
	 * @param args
	 */
	public static void main(String[] args)
//	public static void TEST_main(String[] args)
	{
		// Set Log4j Log Level
//		Configurator.setRootLevel(Level.TRACE);


		ConnectionProp pcsCp = new ConnectionProp();
//		pcsCp.setUrl("jdbc:h2:file:C:/Users/goran/.dbxtune/data/JOB_SCHED_EXTRACT"); // ;IFEXISTS=TRUE
		pcsCp.setUrl("jdbc:h2:file:C:/Users/goran/.dbxtune/data/MM-OP-DW_2024-11-17;IFEXISTS=TRUE");
		pcsCp.setUsername("sa");
		pcsCp.setPassword("");

		ConnectionProp monCp = new ConnectionProp();
		monCp.setUrl("jdbc:sqlserver://gorans.org;trustServerCertificate=true");
		monCp.setUsername("sa");
		monCp.setPassword("**-not-on-github-**");
//		monCp.setUsername("dbxtune");
//		monCp.setPassword("xxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxx");

		try
		{
			DbxConnection pcsConn = DbxConnection.connect(null, pcsCp);
			TEST_createSessionSamplesIfNotExists(pcsConn);

			DbxConnection monConn = DbxConnection.connect(null, monCp);
			
			String srvName = "DUMMY";
			int daysToCopy = 1;
			
			_logger.info("On PCS Database Rollover: Extracting 'Job Scheduler' information On server '" + srvName+ "'.");
			try
			{
				SqlServerJobSchedulerExtractor extractor = new SqlServerJobSchedulerExtractor(daysToCopy, monConn, pcsConn);
				extractor.transfer();

				for (ExtractorEntry entry : extractor.getExtractors())
				{
					String sql = pcsConn.quotifySqlString("select * from [" + PCS_SCHEMA_NAME + "].[" + entry.getTableName() + "]");
					ResultSetTableModel rstm = ResultSetTableModel.executeQuery(pcsConn, sql, entry.getTableName());

					System.out.println(rstm.toAsciiTableString());
				}
			}
			catch (Exception ex)
			{
				_logger.error("On PCS Database Rollover: Problems extracting 'Job Scheduler' information from server '" + srvName + "'.", ex);
			}
		}
		catch (Exception e)
		{
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	
	private static void TEST_createSessionSamplesIfNotExists(DbxConnection conn)
	throws SQLException
	{
		String schemaName = null;
//		String schemaName = PCS_SCHEMA_NAME;
		String tabName = PersistWriterBase.getTableName(conn, schemaName, PersistWriterBase.SESSION_SAMPLES, null, false);

		if ( ! DbUtils.checkIfTableExistsNoThrow(conn, null, schemaName, tabName) )
		{
			List<String> ddlList = PersistWriterBase.getTableDdlString(conn, schemaName, PersistWriterBase.SESSION_SAMPLES, null);
			String ddl = ddlList.get(0);
			
		//	System.out.println("-------- Creating table: \n" + ddl);
			DbUtils.exec(conn, ddl);

			String sql = conn.quotifySqlString(
					  "insert into [" + tabName + "]([SessionStartTime], [SessionSampleTime]) \n"
					+ "select CURRENT_DATE(), CURRENT_TIMESTAMP");
			DbUtils.exec(conn, sql);
		}
		String sql = conn.quotifySqlString("select * from [" + tabName + "]"); 
		System.out.println(ResultSetTableModel.executeQuery(conn, sql, tabName).toAsciiTableString());
	}
}

