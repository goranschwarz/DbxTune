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

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.sql.Types;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

import org.apache.log4j.Logger;
import org.apache.log4j.PropertyConfigurator;

import com.dbxtune.gui.ResultSetTableModel;
import com.dbxtune.sql.ResultSetMetaDataCached;
import com.dbxtune.sql.conn.ConnectionProp;
import com.dbxtune.sql.conn.DbxConnection;
import com.dbxtune.sql.conn.info.DbmsVersionInfo;
import com.dbxtune.sql.ddl.IDbmsDdlResolver;
import com.dbxtune.utils.Configuration;
import com.dbxtune.utils.DbUtils;
import com.dbxtune.utils.StringUtil;
import com.dbxtune.utils.TimeUtils;
import com.dbxtune.utils.Ver;

/**
 * Most code for this was reused from SqlServerQueryStoreExtractor...
 */
public class SqlServerJobSchedulerExtractor
{
	private static Logger _logger = Logger.getLogger(SqlServerJobSchedulerExtractor.class);

	public static final String PROPKEY_overview_calcHistoricalDays = "SqlServerJobSchedulerExtractor.overview.calc.historical.days";
	public static final int    DEFAULT_overview_calcHistoricalDays = 30;
	
	public static final String PROPKEY_outliers_calcHistoricalDays = "SqlServerJobSchedulerExtractor.outliers.calc.historical.days";
	public static final int    DEFAULT_outliers_calcHistoricalDays = 30;
	
	public static final String PROPKEY_search_errorMessage = "SqlServerJobSchedulerExtractor.search.errorMessage";
	public static final String DEFAULT_search_errorMessage = "ERROR-MSG: ";
	
	public static final String PCS_SCHEMA_NAME = "job_scheduler";
	
	private String        _monDbName     = "msdb";
	private String        _pcsSchemaName = PCS_SCHEMA_NAME;

	private DbxConnection _monConn;
	private DbxConnection _pcsConn;

	/** period: -1 = Everything. above 0 = Number of days to extract */
	private int           _period = -1;
	private Timestamp     _periodStartTimeLocal;
	private Timestamp     _periodStopTimeLocal;
	private Timestamp     _periodStartTimeUtc;
	private Timestamp     _periodStopTimeUtc;

//	private Timestamp     _recordingStartTime;

	private int _totalRowsTransferred  = 0;
	private int _totalTableTransferred = 0;

	public enum SqlAgentInfo
	{
		 sysjobs              // Most columns from: msdb.dbo.sysjobs  
		,sysjobsteps          // Most columns from: msdb.dbo.sysjobsteps + some extra aggregated column like 'disabled_by_step, next_step'
		,sysjobhistory        // Most columns from: msdb.dbo.sysjobhistory + some extra aggregated column like 'run_status_desc, run_ts, run_duration_sec'

		,job_history_overview // How many executions Jobs and Steps that had been executed by the scheduler.
		,job_history_outliers // Get info about jobs that is running above the "normal" average...
		,job_history_errors   // Get info about jobs that is has errors 
	};

	
	public SqlServerJobSchedulerExtractor(int period, DbxConnection monConn, DbxConnection pcsConn)
	{
//		_monDbName     = dbname;
//		_pcsSchemaName = "qs:" + _monDbName;

		_monConn    = monConn;
		_pcsConn    = pcsConn;
		
		_period     = period;

		if (_period == 0)
			_period = -1;
	}

	public String getSqlFor(SqlAgentInfo info)
	{

		// Last X days
		switch (info)
		{
    		case sysjobs:
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
    		case sysjobsteps:
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
    		case sysjobhistory:
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
    		case job_history_overview:
			{
				int overview_calcHistoricalDays = Configuration.getCombinedConfiguration().getIntProperty(PROPKEY_overview_calcHistoricalDays, DEFAULT_overview_calcHistoricalDays);

				String allExecTimes = "        ,allExecTimes  = 'Only available in SQL Server 2017 and above' \n";
				DbmsVersionInfo verInfo = _monConn.getDbmsVersionInfo();
				if (verInfo != null && verInfo.getLongVersion() >= Ver.ver(2017))
				{
					allExecTimes = ""
						    + "        ,allExecTimes  = STRING_AGG( \n"
						    + "                             CAST( \n"
						    + "                                 'ts=' + convert(varchar(30), date_executed, 120) \n"
						    + "                               + ', wd=' + cast(datename(weekday, date_executed) as char(9)) \n"
						    + "                               + ', HMS=' + convert(varchar(10), dateadd(second, secs_duration, '2000-01-01'), 8) \n"
						    + "                               + ', sec=' + cast(secs_duration as varchar(20)) \n"
						    + "                               + ', status=' + run_status_desc \n"
						    + "                               + ';' \n"
						    + "                               as varchar(max)) \n"
						    + "                           ,char(10) \n"
						    + "                         ) WITHIN GROUP (ORDER BY date_executed) \n"
							;
				}
				
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
					    + "  from msdb.dbo.sysjobhistory \n"
					    + "  where step_id=0     /* Job Outcome */ \n"
					    + "--    and run_status=1  /* Succeeded */ \n"
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
					    + "    GROUP BY job_id \n"
					    + ") \n"
					    + "SELECT \n"
					    + "     JobName           = j.name \n"
//					    + "--  ,execTime          = today.date_executed \n"
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
//					    + "--    ,histDays          = @historyDayCount \n"
					    + "    ,histExecCount     = hist.recordCount \n"
					    + "    ,histDaysCount     = hist.daysCount \n"
					    + "    ,histFirstExecTime = hist.firstExecTime \n"
					    + "    ,histAvgTimeInSec  = hist.avgDuration \n"
					    + "    ,histAvgTime_HMS   = convert(varchar(10), dateadd(second, hist.avgDuration, '2000-01-01'), 8) \n"
					    + "    ,histMinTime_HMS   = convert(varchar(10), dateadd(second, hist.minDuration, '2000-01-01'), 8) \n"
					    + "    ,histMaxTime_HMS   = convert(varchar(10), dateadd(second, hist.maxDuration, '2000-01-01'), 8) \n"
					    + "    ,histStdevp        = CAST(hist.stdevp as numeric(10,1)) \n"
					    + "    ,histAllExecTimes  = hist.allExecTimes \n"
					    + " \n"
					    + "    ,job_id            = cast(j.job_id as varchar(40)) \n"
//					    + "    ,job_id            = j.job_id \n"  // This does NOT work for the moment... FIXME: The JDBC type mapping
					    + " \n"
					    + "FROM lastXHours       today \n"
					    + "JOIN lastXDays        hist ON today.job_id = hist.job_id \n"
					    + "JOIN msdb.dbo.sysjobs j    ON today.job_id = j.job_id \n"
					    + "ORDER BY today.avgDuration DESC \n"
					    + "";
				
				return sql;
			}
			case job_history_outliers:
			{
				int outliers_calcHistoricalDays = Configuration.getCombinedConfiguration().getIntProperty(PROPKEY_outliers_calcHistoricalDays, DEFAULT_outliers_calcHistoricalDays);

				String allExecTimes = "        ,allExecTimes  = 'Only available in SQL Server 2017 and above' \n";
				DbmsVersionInfo verInfo = _monConn.getDbmsVersionInfo();
				if (verInfo != null && verInfo.getLongVersion() >= Ver.ver(2017))
				{
					allExecTimes = ""
						    + "        ,allExecTimes  = STRING_AGG( \n"
						    + "                             CAST( \n"
						    + "                                 'ts=' + convert(varchar(30), date_executed, 120) \n"
						    + "                               + ', wd=' + cast(datename(weekday, date_executed) as char(9)) \n"
						    + "                               + ', HMS=' + convert(varchar(10), dateadd(second, secs_duration, '2000-01-01'), 8) \n"
						    + "                               + ', sec=' + cast(secs_duration as varchar(20)) \n"
						    + "                               + ', status=' + run_status_desc \n"
						    + "                               + ';' \n"
						    + "                               as varchar(max)) \n"
						    + "                           ,char(10) \n"
						    + "                         ) WITHIN GROUP (ORDER BY date_executed) \n"
							;
				}
				
				// This was mostly from https://bradsruminations.blogspot.com/2011/04/sysjobhistory-job_history_outliers.html
				// with some modifications
				String sql = ""
					    + "DECLARE @historyBeginDate   datetime = NULL \n"
					    + "DECLARE @historyEndDate     datetime = NULL \n"
					    + "DECLARE @analysisBeginDate  datetime = '" + _periodStartTimeLocal + "' \n"
//					    + "DECLARE @analysisBeginDate  datetime = NULL \n"
					    + "DECLARE @analysisEndDate    datetime = NULL \n"
					    + "DECLARE @minHistExecutions  int      = 10 \n"
					    + "DECLARE @minAvgSecsDuration int      = 30 \n"
					    + "DECLARE @historyDayCount    int      = "  + outliers_calcHistoricalDays + " \n"
					    + " \n"
//					    + "IF @historyBeginDate  IS NULL SET @historyBeginDate  = '1900-01-01' ; \n"
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
//					    + "--    AND run_status = 1  --Succeeded \n"
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
//					    + "    WHERE date_executed >= dateadd(day,     datediff(day, '1900-01-01', @historyBeginDate), '1900-01-01') \n"
//					    + "      AND date_executed <  dateadd(day, 1 + datediff(day, '1900-01-01', @historyEndDate  ), '1900-01-01') \n"
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
					    + "    ,histAllExecTimes = jhs.allExecTimes \n"
					    + "    ,job_id           = cast(jd.job_id         as varchar(40)) \n"
					    + "FROM JobHistData jd \n"
					    + "JOIN JobHistStats jhs       ON jd.job_id = jhs.job_id AND jd.step_id = jhs.step_id \n"
					    + "JOIN msdb.dbo.sysjobs j     ON jd.job_id = j.job_id \n"
					    + "JOIN msdb.dbo.sysjobsteps s ON jd.job_id = s.job_id   AND jd.step_id = s.step_id \n"
//					    + "WHERE date_executed >= dateadd(day,     datediff(day, '1900-01-01', @analysisBeginDate), '1900-01-01') \n"
//					    + "  AND date_executed <  dateadd(day, 1 + datediff(day, '1900-01-01', @analysisEndDate  ), '1900-01-01') \n"
						+ "WHERE date_executed >= @analysisBeginDate \n"
						+ "  AND date_executed <  @analysisEndDate   \n"
					    + "  AND secs_duration >  AvgPlus2StDev \n"
					    + "ORDER BY jd.date_executed \n"
					    + "";
				
				return sql;
			}

			case job_history_errors:
			{
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
					    + "                WHEN message like '%![SQLSTATE %' ESCAPE '!' THEN 'WARNING' \n"
					    + sql_caseWhenMessageLike
					    + "                WHEN run_status = 0 THEN 'FAILED' \n"
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
					    + "SELECT \n"
					    + "     execTime = jd.date_executed \n"
					    + "    ,JobName = j.name \n"
					    + "    ,jd.step_name \n"
					    + "    ,jd.step_id \n"
					    + "    ,s.subsystem \n"
					    + "    ,jd.run_status_desc \n"
					    + "    ,jd.retries_attempted \n"
					    + "    ,jd.sql_message_id \n"
					    + "    ,jd.sql_severity \n"
					    + "    ,jd.message \n"
						+ "    ,job_id = cast(jd.job_id as varchar(40)) \n"
					    + "FROM JobHistData jd \n"
					    + "JOIN msdb.dbo.sysjobs j     ON jd.job_id = j.job_id \n"
					    + "JOIN msdb.dbo.sysjobsteps s ON jd.job_id = s.job_id AND jd.step_id = s.step_id \n"
					    + "WHERE jd.date_executed >= @periodStartTime \n"
					    + "ORDER BY jd.date_executed, jd.step_id \n"
					    + "";

				return sql;
			}
		
			default:
				throw new RuntimeException("getSqlFor(): Unhandled Agent Info Name: " + info);
		}
	}

	public void transfer()
	throws SQLException
	{
		String periodStr = "everything";
		if (_period == 1)
			periodStr = "this day";
		else if (_period > 1)
			periodStr = "last " + _period + " days";

		// Get recording start time
		setPeriodStartAndEndTime();
		
		_logger.info("[" + _monDbName + "] Start: Transfer (" + periodStr + ") of Job Scheduler, in database '" + _monDbName + "'. StartTimeUTC='" + _periodStartTimeUtc + "', EndTimeUTC='" + _periodStopTimeUtc + "', StartTimeLocal='" + _periodStartTimeLocal + "', EndTimeLocal='" + _periodStopTimeLocal + "'.");

		long startTime = System.currentTimeMillis();

		// Create a SCHEMA on the PCS
		_pcsConn.createSchemaIfNotExists(_pcsSchemaName);

		try
		{
			// Create any temporary tables
			createTempTables();

			// Transfer tables (using streaming)
			transferTable(true, SqlAgentInfo.sysjobs);
			transferTable(true, SqlAgentInfo.sysjobsteps);
			transferTable(true, SqlAgentInfo.sysjobhistory);

			// Transfer tables (using intermediate in-memory storage)
			transferTable(false, SqlAgentInfo.job_history_overview);
			transferTable(false, SqlAgentInfo.job_history_outliers);
			transferTable(false, SqlAgentInfo.job_history_errors);
		}
		finally
		{
			// Drop any temporary tables
			dropTempTables();
		}

		_logger.info("[" + _monDbName + "] Totally " + _totalRowsTransferred + " rows in " + _totalTableTransferred + " tables was transferred for Job Scheduler, in database '" + _monDbName + "' to the PCS Schema '" + _pcsSchemaName + "'. This took " + TimeUtils.msDiffNowToTimeStr(startTime) + " (HH:MM:SS.ms)");
	}

	
	private void createTempTables()
	{
		// Not implemented, temp tables are not used
	}

	private void dropTempTables()
	{
		// Not implemented, temp tables are not used
	}


	private void setPeriodStartAndEndTime()
	throws SQLException
	{
		Timestamp ts = null;

		String schemaName = null;

		// Start/end time for the recording
		String sql = "select min([SessionSampleTime]) \n" + // or if we want both start/end: select min([SessionSampleTime]), max([SessionSampleTime])
		      "from " + PersistWriterBase.getTableName(_monConn, schemaName, PersistWriterBase.SESSION_SAMPLES, null, true) + " \n";

		sql = _pcsConn.quotifySqlString(sql);
		try (Statement stmnt = _pcsConn.createStatement(); ResultSet rs = stmnt.executeQuery(sql))
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

	public static LocalDateTime convertToUtc(LocalDateTime time) 
	{
	    return time.atZone(ZoneId.systemDefault()).withZoneSameInstant(ZoneOffset.UTC).toLocalDateTime();
	}

	/**
	 * Transfer "table" or "resultset" 
	 * 
	 * @param tabName
	 * @return
	 * @throws SQLException
	 */
	public int transferTable(boolean useStreaming, SqlAgentInfo tabName)
	throws SQLException
	{
		if (useStreaming)
		{
			return transferTableStreaming(tabName);
		}
		else
		{
			return transferTableRstm(tabName);
		}
	}

	/**
	 * Transfer "table" or "resultset" using a intermediate storage (in memory) to hold data before inserting them
	 * 
	 * @param tabName
	 * @return
	 * @throws SQLException
	 */
	public int transferTableRstm(SqlAgentInfo tabName)
	throws SQLException
	{
		// Possibly: Check for SQL Server Version and do different things

//		String tabName = info.toString();
		
		long startTime = System.currentTimeMillis();
		String sql = "";

		// Drop table if it already exists?
		boolean pcsTabExists = DbUtils.checkIfTableExistsNoThrow(_pcsConn, null, _pcsSchemaName, tabName.toString());
		if (pcsTabExists)
		{
			// Should we drop it?
			// Should we truncate it?
			
			// Lets drop the table
			sql = _pcsConn.quotifySqlString("drop table [" + _pcsSchemaName + "].[" + tabName + "]");
			
			try (Statement stmnt = _pcsConn.createStatement())
			{
				stmnt.executeUpdate(sql);
			}
		}

		// Execute SQL and store the result in 'rstm'
		// I know this isn't as "straight forward" as the code in "QueryStoreExtractor"... but it should NOT be "that" many rows...
		ResultSetMetaDataCached sourceRsmdC;
		ResultSetTableModel rstm;
		try
		{
			sql = getSqlFor(tabName);
//System.out.println("TAB-NAME='" + tabName + "'.");
//System.out.println("SQL=|\n" + sql + "|.");
			rstm = ResultSetTableModel.executeQuery(_monConn, sql, tabName.toString());

			sourceRsmdC = rstm.getResultSetMetaDataCached();
			sourceRsmdC = ResultSetMetaDataCached.createNormalizedRsmd(sourceRsmdC);
		}
		catch (SQLException ex)
		{
			// Log something about the SQL caused the error.
			_logger.error("[" + _monDbName + "] Problems with SQL=|" + sql + "|. MsgNum=" + ex.getErrorCode() + ", SqlState='" + ex.getSQLState() + "', MsgText='" + ex.getMessage() + "'.");
			
			// Get out of here
			throw ex;
		}
				
		String crTabSql = null;
		IDbmsDdlResolver dbmsDdlResolver = _pcsConn.getDbmsDdlResolver();
		ResultSetMetaDataCached targetRsmdC = dbmsDdlResolver.transformToTargetDbms(sourceRsmdC); // note the 'sourceRsmdC' is already normalized (done at the "top")

		crTabSql = dbmsDdlResolver.ddlTextTable(targetRsmdC, _pcsSchemaName, tabName.toString());
		crTabSql = crTabSql.trim();
		
		// Translate '[' and ']' chars into DBMS Vendor specific Quoted Chars
		crTabSql = _pcsConn.quotifySqlString(crTabSql);
		
		try (Statement stmnt = _pcsConn.createStatement())
		{
			stmnt.executeUpdate(crTabSql);
			
//			_logger.info("[" + _monDbName + "] CREATED Destination[" + _pcsConn + "], schema '" + _schemaName + "', table '" + tabName + "'.");
			_logger.debug("[" + _monDbName + "] " + crTabSql);
//System.out.println("----------------------- Create Table: \n" + crTabSql);
		}
		catch (SQLException ex)
		{
			// Log something about the SQL caused the error.
			_logger.error("[" + _monDbName + "] Problems with Creating table '" + tabName + "', using DDL=|" + crTabSql + "|. MsgNum=" + ex.getErrorCode() + ", SqlState='" + ex.getSQLState() + "', MsgText='" + ex.getMessage() + "'.");
			
			// Get out of here
			throw ex;
		}

		// Build Insert statement
		String columnStr = " (" + StringUtil.toCommaStrQuoted('[', ']', targetRsmdC.getColumnNames()) + ")";
		
		// Build: values(?, ?, ?, ?...)
		String valuesStr = " values(" + StringUtil.removeLastComma(StringUtil.replicate("?, ", targetRsmdC.getColumnNames().size())) + ")";
		
		// Build insert SQL
		String insertSql = "insert into [" + _pcsSchemaName + "].[" + tabName + "]" + columnStr + valuesStr;

		// Translate '[' and ']' chars into DBMS Vendor specific Quoted Chars
		insertSql = _pcsConn.quotifySqlString(insertSql);

		// Create the Prepared Statement
		PreparedStatement pstmt = _pcsConn.prepareStatement(insertSql);

		// Build the SQL Statement that will *fetch* data
		sql = _monConn.quotifySqlString(getSqlFor(tabName));
		
		if (_logger.isDebugEnabled())
			_logger.debug("[" + _monDbName + "] Issuing SQL for table '" + tabName + "'. \n" + sql);
//System.out.println("Issuing SQL for table '" + tabName + "'. \n" + sql);


		// Loop the saved values in 'rstm'
		int totalRowCount = 0;
		for (int r=0; r<rstm.getRowCount(); r++)
		{
			totalRowCount++;

			// Loop source columns
			for (int c=0; c<rstm.getColumnCount(); c++)
			{
				int sqlPos = c + 1;
				int sourceColJdbcDataType = sourceRsmdC.getColumnType(sqlPos);
				int targetColJdbcDataType = targetRsmdC.getColumnType(sqlPos); // FIXME: for UUID the return the "wrong" (not properly "mapped") type, it returns: ExtendedTypes.DBXTUNE_TYPE_UUID instead of ????  (workaround was to cast the 'job_id' to varchar(...) instead of: MSSQL -> uniqueidentifier
//TODO: createNormalizedRsmd also needs to set "target JDBC Type"... For example SQLServer->H2 we should do: ExtendedTypes.DBXTUNE_TYPE_UUID --> java.sql.Types.VARCHAR ??? (or does H2 have a "special" java.sql.Types.XXXX)
//      --------------------------------
//      https://www.h2database.com/html/datatypes.html?highlight=limited&search=Limit#uuid_type
//      --------------------------------
//      RFC 9562-compliant universally unique identifier. This is a 128 bit value. To store values, use PreparedStatement.setBytes, setString, or setObject(uuid) (where uuid is a java.util.UUID). ResultSet.getObject will return a java.util.UUID.
//      Please note that using an index on randomly generated data will result on poor performance once there are millions of rows in a table. The reason is that the cache behavior is very bad with randomly distributed data. This is a problem for any database system. To avoid this problem use UUID version 7 values.
//      For details, see the documentation of java.util.UUID
				
				Object nullReplacement = null;
				Object obj = rstm.getValueAt(r, c, nullReplacement);
//				String colName = rstm.getColumnName(c);

				try
				{
					// if source type is "TIMESTAMP WITH TIME ZONE"
					// Then we might have to get it as a string
					if (obj != null && sourceColJdbcDataType == Types.TIMESTAMP_WITH_TIMEZONE)
					{
//						if (_logger.isDebugEnabled() && QsTables.query_store_runtime_stats_interval.equals(tabName))
//							_logger.debug("---- query_store_runtime_stats_interval[colPos=" + sqlPos + ", colName='" + sourceRsmdC.getColumnLabel(sqlPos) + "'] val is TIMESTAMP_WITH_TIMEZONE=|" + obj.toString() + "|, objJavaClass='" + obj.getClass().getName() + "'.");
//
//						obj = obj.toString();
					}
					
					// Ugly: But translate/inject some "newlines" in the message on specific wordings...
					// Lets do this some where else, like the client that will finally read the data
//					if ("message".equalsIgnoreCase(colName) && obj != null)
//					{
//						obj = formatMessageString( obj.toString(), "<BR>" );
////						obj = formatMessageString( obj.toString(), "\n" );
//					}

					// SET the data or NULL value
					if (obj != null)
						pstmt.setObject(sqlPos, obj, targetColJdbcDataType);
					else
						pstmt.setNull(sqlPos, targetColJdbcDataType);
					
				}
				catch (SQLException ex)
				{
					String sourceColName = sourceRsmdC.getColumnLabel(sqlPos);
					String destColName   = targetRsmdC.getColumnLabel(sqlPos);

					String msg = "[" + _monDbName + "] ROW: " + totalRowCount + " - Problems setting column c=" + sqlPos + ", sourceName='" + sourceColName + "', destName='" + destColName + "'. Caught: Err=" + ex.getErrorCode() + ", State='" + ex.getSQLState() + "', msg='" + ex.getMessage().trim() + "'.";
					_logger.error(msg);

					// NOTE: Here we THROW (out of method), should we do something "better"
					throw ex;
				}
			} // end: loop source columns
			
			// Add ROW to batch
			pstmt.addBatch();

		} // end: loop rows from source

		// Send the whole batch to the TARGET
//		int[] batchInsCount = pstmt.executeBatch();
		pstmt.executeBatch();
		pstmt.close();
//System.out.println("batchInsCount.length=" + batchInsCount.length + ": " + StringUtil.toCommaStr(batchInsCount));

		_logger.info("[" + _monDbName + "] --> Transferred " + totalRowCount + " rows from '" + _monDbName + "' into: schema '" + _pcsSchemaName + "', table '" + tabName + "'. This took " + TimeUtils.msDiffNowToTimeStr(startTime) + " (HH:MM:SS.ms)");

		// Possibly create any indexes
		createIndexForTable(tabName);

		// Increment some statistics
		_totalRowsTransferred += totalRowCount;
		_totalTableTransferred++;

		return totalRowCount;
	} // end: method


	/**
	 * Streaming version of the "transferTable" no intermediate storage is needed
	 * @param tabName
	 * @return
	 * @throws SQLException
	 */
	public int transferTableStreaming(SqlAgentInfo tabName)
	throws SQLException
	{
		long startTime = System.currentTimeMillis();
		String sql = "";

		// Drop table if it already exists?
		boolean pcsTabExists = DbUtils.checkIfTableExistsNoThrow(_pcsConn, null, _pcsSchemaName, tabName.toString());
		if (pcsTabExists)
		{
			// Should we drop it?
			// Should we truncate it?
			
			// Lets drop the table
			sql = _pcsConn.quotifySqlString("drop table [" + _pcsSchemaName + "].[" + tabName + "]");
			
			try (Statement stmnt = _pcsConn.createStatement())
			{
				stmnt.executeUpdate(sql);
			}
		}

		// Execute SQL (with and extra WHERE 1=2) only to get MetaData for the SOURCE ResultSet, which we are about to transfer
		// This so we can make a CRERATE TABLE DDL String
		ResultSetMetaDataCached sourceRsmdC;
		sql = getSqlFor(tabName) + "\nWHERE 1=2"; // execute ('and 1=2' means more or less "no-exec") get only the ResultSet (this is needed if we append any columns like: schema_name & object_name...
		try (Statement stmnt = _monConn.createStatement(); ResultSet rs = stmnt.executeQuery(sql))
		{
			sourceRsmdC = ResultSetMetaDataCached.createNormalizedRsmd(rs);
		}
		catch (SQLException ex)
		{
			// Log something about the SQL caused the error.
			_logger.error("[" + _monDbName + "] Problems with SQL=|" + sql + "|. MsgNum=" + ex.getErrorCode() + ", SqlState='" + ex.getSQLState() + "', MsgText='" + ex.getMessage() + "'.");
			
			// Get out of here
			throw ex;
		}
		

		String crTabSql = null;
		IDbmsDdlResolver dbmsDdlResolver = _pcsConn.getDbmsDdlResolver();
		ResultSetMetaDataCached targetRsmdC = dbmsDdlResolver.transformToTargetDbms(sourceRsmdC); // note the 'sourceRsmdC' is already normalized (done at the "top")

		crTabSql = dbmsDdlResolver.ddlTextTable(targetRsmdC, _pcsSchemaName, tabName.toString());
		crTabSql = crTabSql.trim();
		
		// Translate '[' and ']' chars into DBMS Vendor specific Quoted Chars
		crTabSql = _pcsConn.quotifySqlString(crTabSql);
		
		try (Statement stmnt = _pcsConn.createStatement())
		{
			stmnt.executeUpdate(crTabSql);
			
//			_logger.info("[" + _monDbName + "] CREATED Destination[" + _pcsConn + "], schema '" + _schemaName + "', table '" + tabName + "'.");
			_logger.debug("[" + _monDbName + "] " + crTabSql);
//System.out.println("----------------------- Create Table: \n" + crTabSql);
		}
		catch (SQLException ex)
		{
			// Log something about the SQL caused the error.
			_logger.error("[" + _monDbName + "] Problems with Creating table '" + tabName + "', using DDL=|" + crTabSql + "|. MsgNum=" + ex.getErrorCode() + ", SqlState='" + ex.getSQLState() + "', MsgText='" + ex.getMessage() + "'.");

			// Get out of here
			throw ex;
		}

		// Build Insert statement
		String columnStr = " (" + StringUtil.toCommaStrQuoted('[', ']', targetRsmdC.getColumnNames()) + ")";
		
		// Build: values(?, ?, ?, ?...)
		String valuesStr = " values(" + StringUtil.removeLastComma(StringUtil.replicate("?, ", targetRsmdC.getColumnNames().size())) + ")";
		
		// Build insert SQL
		String insertSql = "insert into [" + _pcsSchemaName + "].[" + tabName + "]" + columnStr + valuesStr;

		// Translate '[' and ']' chars into DBMS Vendor specific Quoted Chars
		insertSql = _pcsConn.quotifySqlString(insertSql);

		// Create the Prepared Statement
		PreparedStatement pstmt = _pcsConn.prepareStatement(insertSql);

		// Build the SQL Statement that will *fetch* data
		sql = _monConn.quotifySqlString(getSqlFor(tabName));
		
		if (_logger.isDebugEnabled())
			_logger.debug("[" + _monDbName + "] Issuing SQL for table '" + tabName + "'. \n" + sql);
//System.out.println("Issuing SQL for table '" + tabName + "'. \n" + sql);

		// Execute SQL and Loop the SOURCE ResultSet and: setObject(), addBatch(), executeBatch()
		int totalRowCount = 0;
		try (Statement stmnt = _monConn.createStatement(); ResultSet sourceRs = stmnt.executeQuery(sql))
		{
			// Loop source rows
			while (sourceRs.next())
			{
				totalRowCount++;

				// Loop source columns
				for (int sqlPos=1; sqlPos<sourceRsmdC.getColumnCount()+1; sqlPos++)
				{
					int sourceColJdbcDataType = sourceRsmdC.getColumnType(sqlPos);
					int targetColJdbcDataType = targetRsmdC.getColumnType(sqlPos);

					try
					{
						// GET data
						Object obj = sourceRs.getObject(sqlPos);

						// if source type is "TIMESTAMP WITH TIME ZONE"
						// Then we might have to get it as a string
						if (obj != null && sourceColJdbcDataType == Types.TIMESTAMP_WITH_TIMEZONE)
						{
							obj = obj.toString();
						}

						// SET the data or NULL value
						if (obj != null)
							pstmt.setObject(sqlPos, obj, targetColJdbcDataType);
						else
							pstmt.setNull(sqlPos, targetColJdbcDataType);
					}
					catch (SQLException ex)
					{
						String sourceColName = sourceRsmdC.getColumnLabel(sqlPos);
						String destColName   = targetRsmdC.getColumnLabel(sqlPos);

						String msg = "[" + _monDbName + "] ROW: " + totalRowCount + " - Problems setting column c=" + sqlPos + ", sourceName='" + sourceColName + "', destName='" + destColName + "'. Caught: Err=" + ex.getErrorCode() + ", State='" + ex.getSQLState() + "', msg='" + ex.getMessage().trim() + "'.";
						_logger.error(msg);

						// NOTE: Here we THROW (out of method), should we do something "better"
						throw ex;
					}
				} // end: loop source columns

				// Add ROW to batch
				pstmt.addBatch();

			} // end: loop rows from source

		} // end: select from source
		catch (SQLException ex)
		{
			// Log something about the SQL caused the error.
			_logger.error("[" + _monDbName + "] Problems with SQL=|" + sql + "|. MsgNum=" + ex.getErrorCode() + ", SqlState='" + ex.getSQLState() + "', MsgText='" + ex.getMessage() + "'.");
			
			// Get out of here
			throw ex;
		}

		// Send the whole batch to the TARGET
//		int[] batchInsCount = pstmt.executeBatch();
		pstmt.executeBatch();
		pstmt.close();
//System.out.println("batchInsCount.length=" + batchInsCount.length + ": " + StringUtil.toCommaStr(batchInsCount));

		_logger.info("[" + _monDbName + "] --> Transferred " + totalRowCount + " rows from '" + _monDbName + "' into: schema '" + _pcsSchemaName + "', table '" + tabName + "'. This took " + TimeUtils.msDiffNowToTimeStr(startTime) + " (HH:MM:SS.ms)");

		// Possibly create any indexes
		createIndexForTable(tabName);

		// Increment some statistics
		_totalRowsTransferred += totalRowCount;
		_totalTableTransferred++;

		return totalRowCount;
	} // end: method

	
	/**
	 * Create indexes
	 * @param info
	 */
	private void createIndexForTable(SqlAgentInfo info)
	{
		List<String> ddlList = new ArrayList<>();

		switch (info)
		{
		case sysjobs:
			break;

		case sysjobsteps:
			break;
			
		case sysjobhistory:
			break;
			
		case job_history_overview:
			break;

		case job_history_outliers:
			break;

		case job_history_errors:
			break;

		default:
			throw new RuntimeException("createIndexForTable(): Unhandled 'Job Scheduler' Table Name: " + info);
		}

		for (String ddl : ddlList)
		{
			ddl = _pcsConn.quotifySqlString(ddl);
			
			try (Statement stmnt = _pcsConn.createStatement())
			{
				_logger.info("[" + _monDbName + "]     Created index in schema '" + _pcsSchemaName + "' on table '" + info + "'. DDL: " + ddl);
				stmnt.executeUpdate(ddl);
			}
			catch (SQLException ex)
			{
				_logger.error("[" + _monDbName + "] Problems creating index on table '" + info + "' using DDL '" + ddl + "'. Caught: " + ex);
			}
		}
	}
	
	
	/**
	 * Simple test during development
	 * @param args
	 */
	public static void main(String[] args)
//	public static void TEST_main(String[] args)
	{
		Properties log4jProps = new Properties();
		log4jProps.setProperty("log4j.rootLogger", "INFO, A1");
//		log4jProps.setProperty("log4j.rootLogger", "DEBUG, A1");
		log4jProps.setProperty("log4j.appender.A1", "org.apache.log4j.ConsoleAppender");
		log4jProps.setProperty("log4j.appender.A1.layout", "org.apache.log4j.PatternLayout");
		log4jProps.setProperty("log4j.appender.A1.layout.ConversionPattern", "%d - %-5p - %-30c{1} - %m%n");
		PropertyConfigurator.configure(log4jProps);


		ConnectionProp pcsCp = new ConnectionProp();
//		pcsCp.setUrl("jdbc:h2:file:C:/Users/goran/.dbxtune/data/JOB_SCHED_EXTRACT"); // ;IFEXISTS=TRUE
		pcsCp.setUrl("jdbc:h2:file:C:/Users/goran/.dbxtune/data/MM-OP-DW_2024-11-17;IFEXISTS=TRUE");
		pcsCp.setUsername("sa");
		pcsCp.setPassword("");

		ConnectionProp monCp = new ConnectionProp();
		monCp.setUrl("jdbc:sqlserver://mm-op-dw.maxm.se;trustServerCertificate=true");
//		monCp.setUrl("jdbc:sqlserver://mm-op-dwutv.maxm.se;trustServerCertificate=true");
		monCp.setUsername("sa");
		monCp.setPassword("K2f88k3hV682uRSXrEKpDQu");
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
				SqlServerJobSchedulerExtractor jse = new SqlServerJobSchedulerExtractor(daysToCopy, monConn, pcsConn);
				jse.transfer();

				String sql;
				sql = pcsConn.quotifySqlString("select * from [" + PCS_SCHEMA_NAME + "].[" + SqlAgentInfo.sysjobs + "]"); 
				System.out.println(ResultSetTableModel.executeQuery(pcsConn, sql, SqlAgentInfo.sysjobs.toString()).toAsciiTableString());

				sql = pcsConn.quotifySqlString("select * from [" + PCS_SCHEMA_NAME + "].[" + SqlAgentInfo.sysjobsteps + "]"); 
				System.out.println(ResultSetTableModel.executeQuery(pcsConn, sql, SqlAgentInfo.sysjobsteps.toString()).toAsciiTableString());

				sql = pcsConn.quotifySqlString("select * from [" + PCS_SCHEMA_NAME + "].[" + SqlAgentInfo.sysjobhistory + "]"); 
				System.out.println(ResultSetTableModel.executeQuery(pcsConn, sql, SqlAgentInfo.sysjobhistory.toString()).toAsciiTableString());

				sql = pcsConn.quotifySqlString("select * from [" + PCS_SCHEMA_NAME + "].[" + SqlAgentInfo.job_history_overview + "]"); 
				System.out.println(ResultSetTableModel.executeQuery(pcsConn, sql, SqlAgentInfo.job_history_overview.toString()).toAsciiTableString());

				sql = pcsConn.quotifySqlString("select * from [" + PCS_SCHEMA_NAME + "].[" + SqlAgentInfo.job_history_outliers + "]"); 
				System.out.println(ResultSetTableModel.executeQuery(pcsConn, sql, SqlAgentInfo.job_history_outliers.toString()).toAsciiTableString());

				sql = pcsConn.quotifySqlString("select * from [" + PCS_SCHEMA_NAME + "].[" + SqlAgentInfo.job_history_errors + "]"); 
				System.out.println(ResultSetTableModel.executeQuery(pcsConn, sql, SqlAgentInfo.job_history_errors.toString()).toAsciiTableString());
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

