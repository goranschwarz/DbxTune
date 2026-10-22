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
package com.dbxtune.pcs;

import java.lang.invoke.MethodHandles;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.dbxtune.sql.conn.DbxConnection;
import com.dbxtune.sql.conn.info.DbmsVersionInfo;
import com.dbxtune.sql.conn.info.DbmsVersionInfoOracle;
import com.dbxtune.utils.Ver;

/**
 * Copies the log of the Oracle Scheduler (the runs of the jobs) into the PCS, so the Daily Summary Report (OracleJobScheduler)
 * and the "Scheduler Report for Today" can be made from the recording.
 * <p>
 * Tables, in the PCS schema 'job_scheduler':
 * <ul>
 *   <li><b>job_runs</b>     - one row for each run in DBA_SCHEDULER_JOB_RUN_DETAILS, the last N days (the days before the report day are
 *                             what "normal" is compared with)</li>
 *   <li><b>jobs</b>         - the jobs in DBA_SCHEDULER_JOBS: enabled, state, schedule, what they do</li>
 *   <li><b>running_jobs</b> - the jobs that were running when this was read (DBA_SCHEDULER_RUNNING_JOBS)</li>
 *   <li><b>extract_info</b> - one row: when this was read, and how many days</li>
 * </ul>
 * None of the views needs an extra license from Oracle. In the root of a Container Database the CDB_ views are read (all containers).
 * <p>
 * <b>The times</b>: the Scheduler has its times as TIMESTAMP WITH TIME ZONE (and every job can have its own time zone). Here they are made
 * into the clock of the COLLECTOR: "how long ago" is calculated in UTC, and that is taken from the time of the collector. So they can be
 * compared with the sample times of the recording. (It is the same clock as the database server has, when both are in the same time zone.)
 * <p>
 * The SQL is also used by the page that draws the runs as a timeline (OracleJobSchedulerTimeline), which reads the database directly.
 */
public class OracleJobSchedulerExtractor
extends DbmsExtractorAbstract
{
	private static final Logger _logger = LogManager.getLogger(MethodHandles.lookup().lookupClass());

	public static final String PROPKEY_daysToCopy = "OracleJobSchedulerExtractor.daysToCopy";
	public static final int    DEFAULT_daysToCopy = 30;

	public static final String EXTRACTOR_NAME  = "Oracle Job Scheduler Extractor";
	public static final String PCS_SCHEMA_NAME = "job_scheduler";

	public static final String TABLE_job_runs     = "job_runs";
	public static final String TABLE_jobs         = "jobs";
	public static final String TABLE_running_jobs = "running_jobs";
	public static final String TABLE_extract_info = "extract_info";

	private int     _daysToCopy;
	private boolean _cdb       = false;  // read the CDB_ views (we are in the root of a Container Database, and they can be read)
	private boolean _hasErrors = false;  // DBA_SCHEDULER_JOB_RUN_DETAILS has the column ERRORS (12.1 and above)

	/**
	 * @param daysToCopy   how many days of the Scheduler log to copy (less than 1 = the default: 30)
	 */
	public OracleJobSchedulerExtractor(int daysToCopy, DbxConnection monConn, DbxConnection pcsConn)
	{
		super(EXTRACTOR_NAME, monConn, "", pcsConn, PCS_SCHEMA_NAME);

		_daysToCopy = daysToCopy < 1 ? DEFAULT_daysToCopy : daysToCopy;
	}

	@Override
	protected String getPeriodInfoShort()
	{
		return "last " + _daysToCopy + " days";
	}

	/** Look at what we are connected to, before the SQL is made: a Container Database? which version? */
	@Override
	protected void onTransferStart()
	throws SQLException
	{
		_hasErrors = hasErrorsColumn(getMonConn());
		_cdb       = canReadCdbViews(getMonConn());

		super.onTransferStart();
	}

	@Override
	protected List<ExtractorEntry> createExtractors()
	{
		List<ExtractorEntry> list = new ArrayList<>();

		list.add( new Entry(TABLE_extract_info) { @Override public String getSql() { return getExtractInfoSql(_cdb, _daysToCopy);            } } );
		list.add( new Entry(TABLE_jobs)         { @Override public String getSql() { return getJobsSql(_cdb);                                } } );
		list.add( new Entry(TABLE_running_jobs) { @Override public String getSql() { return getRunningJobsSql(_cdb);                         } } );
		list.add( new Entry(TABLE_job_runs)     { @Override public String getSql() { return getJobRunsSql(_cdb, _hasErrors, _daysToCopy);    } } );

		return list;
	}

	/**
	 * One table to copy.<br>
	 * STREAMING: the rows are not kept in memory (the log can be big: a job that runs every minute makes 43 000 rows in 30 days).<br>
	 * NOTE: for STREAMING the framework adds "WHERE 1=2" at the end of the SQL (to get the columns), so every SQL here is: select x.* from (...) x
	 */
	private abstract class Entry implements ExtractorEntry
	{
		private String _tableName;

		Entry(String tableName)
		{
			_tableName = tableName;
		}

		@Override public String           getTableName()        { return _tableName; }
		@Override public ExtractionMethod getExtractionMethod() { return ExtractionMethod.STREAMING; }
		@Override public List<String>     getCreateIndex()      { return Collections.emptyList(); }
	}

	//--------------------------------------------------------------------
	// What the SQL depends on
	//--------------------------------------------------------------------

	/** @return true if DBA_SCHEDULER_JOB_RUN_DETAILS has the column ERRORS (Oracle 12.1 and above) */
	public static boolean hasErrorsColumn(DbxConnection conn)
	{
		return conn.getDbmsVersionInfo().getLongVersion() >= Ver.ver(12,1);
	}

	/**
	 * @return true if we are in the root of a Container Database and the CDB_ views of the Scheduler can be read (so we see the jobs of all containers).
	 *         They can NOT be tested on all versions: so they are "dry-run", and if that fails it is the DBA_ views (the jobs of CDB$ROOT only)
	 */
	public static boolean canReadCdbViews(DbxConnection conn)
	{
		DbmsVersionInfo versionInfo = conn.getDbmsVersionInfo();
		if ( ! (versionInfo instanceof DbmsVersionInfoOracle) || versionInfo.getLongVersion() < Ver.ver(12,1) || ! ((DbmsVersionInfoOracle) versionInfo).isCdbRoot() )
			return false;

		String sql = "select * from (" + getJobRunsSql(true, hasErrorsColumn(conn), 1) + ") where 1=2";
		try (Statement stmnt = conn.createStatement(); ResultSet rs = stmnt.executeQuery(sql))
		{
			return true;
		}
		catch (SQLException ex)
		{
			_logger.warn("[" + PCS_SCHEMA_NAME + "] The SQL for a CDB failed, only the jobs in CDB$ROOT will be read. Caught: " + ex.getMessage().trim());
			return false;
		}
	}

	//--------------------------------------------------------------------
	// SQL helpers
	//--------------------------------------------------------------------

	/** The time of the collector, as an Oracle DATE */
	private static String collectorNow()
	{
		return "TO_DATE('" + new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date()) + "', 'YYYY-MM-DD HH24:MI:SS')";
	}

	/** A TIMESTAMP WITH TIME ZONE as the clock of the collector: now (of the collector) minus how long ago it was (in UTC) */
	private static String collectorTime(String now, String tstzExpr)
	{
		return "(" + now + " - (CAST(SYS_EXTRACT_UTC(SYSTIMESTAMP) as DATE) - CAST(SYS_EXTRACT_UTC(" + tstzExpr + ") as DATE)))";
	}

	/** An INTERVAL DAY TO SECOND as seconds */
	private static String intervalSec(String intervalExpr)
	{
		return "(extract(day from " + intervalExpr + ") * 86400 + extract(hour from " + intervalExpr + ") * 3600 + extract(minute from " + intervalExpr + ") * 60 + extract(second from " + intervalExpr + "))";
	}

	private static String views         (boolean cdb)               { return cdb ? "CDB" : "DBA"; }
	private static String conId         (boolean cdb, String alias) { return cdb ? "CAST(" + alias + ".CON_ID as NUMBER(10))" : "CAST(0 as NUMBER(10))"; }
	private static String conName       (boolean cdb)               { return cdb ? "c.NAME" : "CAST(NULL as VARCHAR2(128))"; }
	private static String joinContainers(boolean cdb, String alias) { return cdb ? "    left join V$CONTAINERS c on c.CON_ID = " + alias + ".CON_ID \n" : ""; }

	//--------------------------------------------------------------------
	// extract_info
	//--------------------------------------------------------------------
	private static String getExtractInfoSql(boolean cdb, int days)
	{
		return ""
			+ "select x.* from ( \n"
			+ "    select \n"
			+ "         " + collectorNow() + "                                          as EXTRACT_TIME \n"
			+ "        ,SYSDATE                                                         as DB_TIME \n"
			+ "        ,CAST(" + days + " as NUMBER(10))                                as DAYS_COPIED \n"
			+ "        ,CAST('" + (cdb ? "Y" : "N") + "' as VARCHAR2(1))                as ALL_CONTAINERS \n"
			+ "    from DUAL \n"
			+ ") x";
	}

	//--------------------------------------------------------------------
	// jobs
	//--------------------------------------------------------------------
	/**
	 * The jobs in DBA_SCHEDULER_JOBS
	 * @param cdb  true = read the CDB_ view (see {@link #canReadCdbViews(DbxConnection)})
	 */
	public static String getJobsSql(boolean cdb)
	{
		String now = collectorNow();

		return ""
			+ "select x.* from ( \n"
			+ "    select \n"
			+ "         " + conId(cdb, "j") + "                                         as CON_ID \n"
			+ "        ," + conName(cdb) + "                                            as CON_NAME \n"
			+ "        ,CAST(j.OWNER    as VARCHAR2(128))                               as OWNER \n"
			+ "        ,CAST(j.JOB_NAME as VARCHAR2(128))                               as JOB_NAME \n"
			+ "        ,CAST(j.JOB_TYPE as VARCHAR2(30))                                as JOB_TYPE \n"
			+ "        ,CAST(j.ENABLED  as VARCHAR2(5))                                 as ENABLED \n"
			+ "        ,CAST(j.STATE    as VARCHAR2(20))                                as STATE \n"
			+ "        ,CAST(NVL(j.RUN_COUNT,     0) as NUMBER(18))                     as RUN_COUNT \n"
			+ "        ,CAST(NVL(j.FAILURE_COUNT, 0) as NUMBER(18))                     as FAILURE_COUNT \n"
			+ "        ," + collectorTime(now, "j.LAST_START_DATE") + "                 as LAST_START_TIME \n"
			+ "        ,CAST(" + intervalSec("j.LAST_RUN_DURATION") + " as NUMBER(10))  as LAST_RUN_SEC \n"
			+ "        ," + collectorTime(now, "j.NEXT_RUN_DATE") + "                   as NEXT_RUN_TIME \n"
			+ "        ,CAST(substr(NVL(j.REPEAT_INTERVAL, j.SCHEDULE_NAME), 1, 400) as VARCHAR2(400))  as SCHEDULE \n"
			+ "        ,CAST(substr(NVL(j.PROGRAM_NAME,    j.JOB_ACTION),    1, 2000) as VARCHAR2(2000)) as JOB_ACTION \n"
			+ "        ,CAST(j.JOB_CLASS as VARCHAR2(128))                              as JOB_CLASS \n"
			+ "        ,CAST(substr(j.COMMENTS, 1, 400) as VARCHAR2(400))               as COMMENTS \n"
			+ "    from " + views(cdb) + "_SCHEDULER_JOBS j \n"
			+ joinContainers(cdb, "j")
			+ "    where j.JOB_SUBNAME is null \n"
			+ ") x";
	}

	//--------------------------------------------------------------------
	// running_jobs
	//--------------------------------------------------------------------
	/**
	 * The jobs that are running now, from DBA_SCHEDULER_RUNNING_JOBS
	 * @param cdb  true = read the CDB_ view (see {@link #canReadCdbViews(DbxConnection)})
	 */
	public static String getRunningJobsSql(boolean cdb)
	{
		String now = collectorNow();

		// (the same name as in the log, see getJobRunsSql(): an automatic maintenance task without the number of the run)
		String jobName = "case when substr(r.JOB_NAME, 1, 7) = 'ORA$AT_' then regexp_replace(r.JOB_NAME, '_[0-9]+$', '') else r.JOB_NAME end";

		return ""
			+ "select x.* from ( \n"
			+ "    select \n"
			+ "         " + conId(cdb, "r") + "                                         as CON_ID \n"
			+ "        ," + conName(cdb) + "                                            as CON_NAME \n"
			+ "        ,CAST(r.OWNER       as VARCHAR2(128))                            as OWNER \n"
			+ "        ,CAST(" + jobName + " as VARCHAR2(128))                          as JOB_NAME \n"
			+ "        ,CAST(r.JOB_SUBNAME as VARCHAR2(128))                            as JOB_SUBNAME \n"
			+ "        ,CAST(r.SESSION_ID  as NUMBER(10))                               as SESSION_ID \n"
			+ "        ,CAST(r.RUNNING_INSTANCE as NUMBER(10))                          as INSTANCE_ID \n"
			+ "        ," + now + " - " + intervalSec("r.ELAPSED_TIME") + " / 86400     as START_TIME \n"
			+ "        ,CAST(" + intervalSec("r.ELAPSED_TIME") + " as NUMBER(10))       as ELAPSED_SEC \n"
			+ "        ,CAST(" + intervalSec("r.CPU_USED")     + " as NUMBER(12,2))     as CPU_SEC \n"
			+ "    from " + views(cdb) + "_SCHEDULER_RUNNING_JOBS r \n"
			+ joinContainers(cdb, "r")
			+ ") x";
	}

	//--------------------------------------------------------------------
	// job_runs
	//--------------------------------------------------------------------
	/**
	 * The runs in the log of the Scheduler: DBA_SCHEDULER_JOB_RUN_DETAILS
	 *
	 * @param cdb        true = read the CDB_ view (see {@link #canReadCdbViews(DbxConnection)})
	 * @param hasErrors  true = the view has the column ERRORS (see {@link #hasErrorsColumn(DbxConnection)})
	 * @param days       how many days back (when the run ENDED)
	 */
	public static String getJobRunsSql(boolean cdb, boolean hasErrors, int days)
	{
		String now       = collectorNow();
		String startTime = collectorTime(now, "d.ACTUAL_START_DATE");

		// An automatic maintenance task runs as a job with a new name every time (ORA$AT_OS_OPT_SY_1234): without the number it is one "job"
		String isAutotask = "substr(d.JOB_NAME, 1, 7) = 'ORA$AT_'";
		String jobName    = "case when " + isAutotask + " then regexp_replace(d.JOB_NAME, '_[0-9]+$', '') else d.JOB_NAME end";

		return ""
			+ "select x.* from ( \n"
			+ "    select \n"
			+ "         " + conId(cdb, "d") + "                                         as CON_ID \n"
			+ "        ," + conName(cdb) + "                                            as CON_NAME \n"
			+ "        ,CAST(d.LOG_ID as NUMBER(18))                                    as LOG_ID \n"
			+ "        ,CAST(case when " + isAutotask + " then 'AUTOTASK' else 'SCHEDULER' end as VARCHAR2(10)) as JOB_KIND \n"
			+ "        ,CAST(d.OWNER as VARCHAR2(128))                                  as OWNER \n"
			+ "        ,CAST(" + jobName + " as VARCHAR2(128))                          as JOB_NAME \n"
			+ "        ,CAST(d.JOB_SUBNAME as VARCHAR2(128))                            as JOB_SUBNAME \n"
			+ "        ,CAST(d.STATUS as VARCHAR2(30))                                  as STATUS \n"
			+ "        ,CAST(d.ERROR# as NUMBER(10))                                    as ERROR_NUM \n"
			+ "        ," + startTime + "                                               as START_TIME \n"
			+ "        ," + startTime + " + " + intervalSec("d.RUN_DURATION") + " / 86400 as END_TIME \n"
			+ "        ,CAST(" + intervalSec("d.RUN_DURATION") + " as NUMBER(10))       as DURATION_SEC \n"
			+ "        ,CAST(" + intervalSec("d.CPU_USED")     + " as NUMBER(12,2))     as CPU_SEC \n"
			+ "        ,CAST(86400 * (CAST(SYS_EXTRACT_UTC(d.ACTUAL_START_DATE) as DATE) - CAST(SYS_EXTRACT_UTC(d.REQ_START_DATE) as DATE)) as NUMBER(10)) as START_DELAY_SEC \n"
			+ "        ,CAST(d.SESSION_ID  as VARCHAR2(30))                             as SESSION_ID \n"
			+ "        ,CAST(d.INSTANCE_ID as NUMBER(10))                               as INSTANCE_ID \n"
			+ "        ,CAST(d.JOB_NAME as VARCHAR2(128))                               as LOG_JOB_NAME \n"
			+ "        ,CAST(substr(d.ADDITIONAL_INFO, 1, 4000) as VARCHAR2(4000))      as RUN_INFO \n"
			+ "        ," + (hasErrors ? "CAST(substr(d.ERRORS, 1, 4000) as VARCHAR2(4000))" : "CAST(NULL as VARCHAR2(4000))") + " as ERRORS \n"
			+ "        ," + (hasErrors ? "CAST(substr(d.OUTPUT, 1, 4000) as VARCHAR2(4000))" : "CAST(NULL as VARCHAR2(4000))") + " as OUTPUT \n"   // (12.1 and above, as ERRORS) what the job wrote: DBMS_OUTPUT, or stdout of an external job
			+ "    from " + views(cdb) + "_SCHEDULER_JOB_RUN_DETAILS d \n"
			+ joinContainers(cdb, "d")
			+ "    where d.LOG_DATE > SYSTIMESTAMP - NUMTODSINTERVAL(" + days + ", 'DAY') \n"
			+ "      and d.ACTUAL_START_DATE is not null \n"
			+ ") x";
	}
}
