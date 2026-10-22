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
package com.dbxtune.cm.oracle;

import java.lang.invoke.MethodHandles;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

import javax.naming.NameNotFoundException;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.dbxtune.ICounterController;
import com.dbxtune.IGuiController;
import com.dbxtune.alarm.AlarmHandler;
import com.dbxtune.alarm.events.AlarmDescriptionProviderBase;
import com.dbxtune.alarm.events.AlarmEvent;
import com.dbxtune.alarm.events.oracle.AlarmEventOraJobFailed;
import com.dbxtune.cm.CmSettingsHelper;
import com.dbxtune.cm.CmSettingsHelper.RegExpInputValidator;
import com.dbxtune.cm.CounterSetTemplates;
import com.dbxtune.cm.CounterSetTemplates.Type;
import com.dbxtune.cm.CountersModel;
import com.dbxtune.config.dict.MonTablesDictionary;
import com.dbxtune.config.dict.MonTablesDictionaryManager;
import com.dbxtune.gui.MainFrame;
import com.dbxtune.sql.conn.DbxConnection;
import com.dbxtune.sql.conn.info.DbmsVersionInfo;
import com.dbxtune.sql.conn.info.DbmsVersionInfoOracle;
import com.dbxtune.utils.Configuration;
import com.dbxtune.utils.StringUtil;
import com.dbxtune.utils.Ver;

/**
 * @author Goran Schwarz (goran_schwarz@hotmail.com)
 */
public class CmSchedulerJobs
extends CountersModel
{
	private static final Logger _logger = LogManager.getLogger(MethodHandles.lookup().lookupClass());
	private static final long    serialVersionUID = 1L;

	public static final String   CM_NAME          = CmSchedulerJobs.class.getSimpleName();
	public static final String   SHORT_NAME       = "Jobs";
	public static final String   HTML_DESC        =
		"<html>" +
		"The jobs that the database runs by itself, and how the last run of each one went. A job that fails every night does not tell anyone: this tab (and it's alarm) does.<br>" +
		"<br>" +
		"<code>JOB_KIND</code> is:" +
		"<ul>" +
		"  <li><b>SCHEDULER</b> - a job in <code>DBA_SCHEDULER_JOBS</code> (made with <code>DBMS_SCHEDULER</code>).</li>" +
		"  <li><b>AUTOTASK</b> - one of the automatic maintenance tasks, that runs in the maintenance windows: optimizer statistics, space advisor, SQL tuning advisor.</li>" +
		"  <li><b>DBMS_JOB</b> - an old style job in <code>DBA_JOBS</code>. From Oracle 19 those are Scheduler jobs with the name <code>DBMS_JOB$_nnn</code>, and are shown as SCHEDULER.</li>" +
		"</ul>" +
		"<code>LAST_RUN_STATUS</code> comes from the log of the Scheduler (<code>DBA_SCHEDULER_JOB_RUN_DETAILS</code>, the last 31 days). It is empty for a job that does not log it's runs.<br>" +
		"A job with <code>STATE</code> = BROKEN will not run again until someone enables it.<br>" +
		"<br>" +
		"<b>Note</b>: The times are the clock of the database server.<br>" +
		"<b>Note</b>: In a Container Database, connect to CDB$ROOT to see all PDBs.<br>" +
		"</html>";

	public static final String   GROUP_NAME       = MainFrame.TCP_GROUP_SERVER;
	public static final String   GUI_ICON_FILE    = "images/" + CM_NAME + ".png";

	public static final long     NEED_SRV_VERSION = 0;
	public static final long     NEED_CE_VERSION  = 0;

	public static final String[] MON_TABLES       = new String[] {CM_NAME, "DBA_SCHEDULER_JOBS", "DBA_SCHEDULER_JOB_RUN_DETAILS", "DBA_JOBS"};
	public static final String[] NEED_ROLES       = new String[] {};
	public static final String[] NEED_CONFIG      = new String[] {};

	public static final String[] PCT_COLUMNS      = new String[] {};
	public static final String[] DIFF_COLUMNS     = new String[] {"RUN_COUNT", "FAILURE_COUNT"};

	public static final boolean  NEGATIVE_DIFF_COUNTERS_TO_ZERO = true;
	public static final boolean  IS_SYSTEM_CM                   = true;
	public static final int      DEFAULT_POSTPONE_TIME          = 300;
	public static final int      DEFAULT_QUERY_TIMEOUT          = 30;

	@Override public int     getDefaultPostponeTime()                 { return DEFAULT_POSTPONE_TIME; }
	@Override public int     getDefaultQueryTimeout()                 { return DEFAULT_QUERY_TIMEOUT; }
	@Override public boolean getDefaultIsNegativeDiffCountersToZero() { return NEGATIVE_DIFF_COUNTERS_TO_ZERO; }
	@Override public Type    getTemplateLevel()                       { return Type.SMALL; }

	/**
	 * FACTORY  method to create the object
	 */
	public static CountersModel create(ICounterController counterController, IGuiController guiController)
	{
		if (guiController != null && guiController.hasGUI())
			guiController.splashWindowProgress("Loading: Counter Model '" + CM_NAME + "'");

		return new CmSchedulerJobs(counterController, guiController);
	}

	public CmSchedulerJobs(ICounterController counterController, IGuiController guiController)
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

		CounterSetTemplates.register(this);
	}


	//------------------------------------------------------------
	// Implementation
	//------------------------------------------------------------
	@Override
	public List<String> getPkForVersion(DbxConnection conn, DbmsVersionInfo versionInfo)
	{
		List <String> pkCols = new LinkedList<String>();

		// CON_ID is always 0 in a non-CDB (and in 11.2), in a CDB the same job exists in many containers
		pkCols.add("CON_ID");
		pkCols.add("JOB_KIND");
		pkCols.add("OWNER");
		pkCols.add("JOB_NAME");

		return pkCols;
	}

	/** If we can't read the views: disable the CM, and tell what to do about it */
	@Override
	public boolean checkDependsOnOther(DbxConnection conn)
	{
		return OracleCmUtils.checkViewsAreReadable(this, conn, "DBA_SCHEDULER_JOBS", "DBA_SCHEDULER_JOB_RUN_DETAILS", "DBA_JOBS");
	}

	@Override
	public String getSqlForVersion(DbxConnection conn, DbmsVersionInfo versionInfo)
	{
		DbmsVersionInfoOracle oraVersionInfo = (DbmsVersionInfoOracle) versionInfo;

		// From Oracle 19 a DBMS_JOB is also a Scheduler job (DBMS_JOB$_nnn), so there DBA_JOBS would only give every such job a second row
		boolean withDbaJobs = versionInfo.getLongVersion() < Ver.ver(19,0);

		// 11.2 and above: a non-CDB, or when we are connected to a PDB
		String sql = getSql(false, withDbaJobs);

		// 12.1 and above, connected to CDB$ROOT: the jobs in EVERY container
		if (versionInfo.getLongVersion() >= Ver.ver(12,1) && oraVersionInfo.isCdbRoot())
		{
			String sqlCdb = getSql(true, withDbaJobs);

			// The SQL for 12.1 and above can NOT be tested on all versions: so "dry-run" it, and if it fails use the 11.2 SQL (which works on all versions)
			// conn is null when we just want to "view" the SQL (the CM Properties dialog)
			if (conn == null)
			{
				sql = sqlCdb;
			}
			else
			{
				try (Statement stmnt = conn.createStatement(); ResultSet rs = stmnt.executeQuery("select * from (" + sqlCdb + ") where 1=2"))
				{
					sql = sqlCdb;
				}
				catch (SQLException ex)
				{
					_logger.warn(CM_NAME + ": The SQL for a CDB (version " + Ver.versionNumToStr(versionInfo.getLongVersion()) + ") failed, only the jobs in CDB$ROOT will be shown. Caught: " + ex.getMessage().trim());
				}
			}
		}

		return sql;
	}

	/**
	 * @param cdb           false = the DBA_ views (one container), true = the CDB_ views (all containers, they have the column CON_ID)
	 * @param withDbaJobs   true = also the old style jobs in DBA_JOBS
	 */
	private static String getSql(boolean cdb, boolean withDbaJobs)
	{
		String views = cdb ? "CDB" : "DBA";

		// The Scheduler has it's times as TIMESTAMP WITH TIME ZONE, and every job can have it's own time zone
		// So: how long ago it was is calculated in UTC, and then that is taken from SYSDATE. That gives the clock of the database server
		String utcNow         = "CAST(SYS_EXTRACT_UTC(SYSTIMESTAMP) as DATE)";
		String jLastStart     = "NVL(r.ACTUAL_START_DATE, j.LAST_START_DATE)";
		String jLastStartDays = "(" + utcNow + " - CAST(SYS_EXTRACT_UTC(" + jLastStart          + ") as DATE))";
		String jNextRunDays   = "(" + utcNow + " - CAST(SYS_EXTRACT_UTC(j.NEXT_RUN_DATE"        + ") as DATE))";
		String rLastStartDays = "(" + utcNow + " - CAST(SYS_EXTRACT_UTC(r.ACTUAL_START_DATE"    + ") as DATE))";

		// An INTERVAL as seconds
		String jDuration      = "NVL(r.RUN_DURATION, j.LAST_RUN_DURATION)";
		String jDurationSec   = "extract(day from " + jDuration      + ") * 86400 + extract(hour from " + jDuration      + ") * 3600 + extract(minute from " + jDuration      + ") * 60 + extract(second from " + jDuration      + ")";
		String rDurationSec   = "extract(day from " + "r.RUN_DURATION" + ") * 86400 + extract(hour from " + "r.RUN_DURATION" + ") * 3600 + extract(minute from " + "r.RUN_DURATION" + ") * 60 + extract(second from " + "r.RUN_DURATION" + ")";

		// An automatic task runs as a job with a new name every time (ORA$AT_OS_OPT_SY_1234): without the number it is one "job"
		String rJobName       = "case when substr(d.JOB_NAME, 1, 7) = 'ORA$AT_' then regexp_replace(d.JOB_NAME, '_[0-9]+$', '') else d.JOB_NAME end";

		int runDetailsDays = Configuration.getCombinedConfiguration().getIntProperty(PROPKEY_sample_runDetailsDays, DEFAULT_sample_runDetailsDays);

		// r: the LAST run of every job, from the log of the Scheduler. It is read ONCE (materialize) and then joined
		String sql = ""
			+ "with r as (select /*+ materialize */ " + (cdb ? "CON_ID, " : "") + "OWNER, JOB_NAME, IS_AUTOTASK, STATUS, ERROR#, ACTUAL_START_DATE, RUN_DURATION, ADDITIONAL_INFO \n"
			+ "           from (select " + (cdb ? "d.CON_ID, " : "") + "d.OWNER \n"
			+ "                       ," + rJobName + " as JOB_NAME \n"
			+ "                       ,case when substr(d.JOB_NAME, 1, 7) = 'ORA$AT_' then 1 else 0 end as IS_AUTOTASK \n"
			+ "                       ,d.STATUS, d.ERROR#, d.ACTUAL_START_DATE, d.RUN_DURATION, d.ADDITIONAL_INFO \n"
			+ "                       ,row_number() over (partition by " + (cdb ? "d.CON_ID, " : "") + "d.OWNER, " + rJobName + " order by d.LOG_ID desc) as RN \n"
			+ "                 from " + views + "_SCHEDULER_JOB_RUN_DETAILS d \n"
			+ "                 where d.LOG_DATE > SYSTIMESTAMP - NUMTODSINTERVAL(" + runDetailsDays + ", 'DAY')) \n"
			+ "           where RN = 1) \n"
			+ "select \n"
			+ "     " + (cdb ? "x.CON_ID" : "CAST(0    as NUMBER(10))")    + " as CON_ID \n"
			+ "    ," + (cdb ? "c.NAME"   : "CAST(NULL as VARCHAR2(128))") + " as CON_NAME \n"
			+ "    ,x.JOB_KIND \n"
			+ "    ,x.OWNER \n"
			+ "    ,x.JOB_NAME \n"
			+ "    ,x.ENABLED \n"
			+ "    ,x.STATE \n"
			+ "    ,x.LAST_RUN_STATUS \n"
			+ "    ,x.LAST_RUN_TIME \n"
			+ "    ,x.LAST_RUN_AGE_HOURS \n"
			+ "    ,x.LAST_RUN_SECONDS \n"
			+ "    ,x.LAST_RUN_ERROR \n"
			+ "    ,x.RUN_COUNT \n"
			+ "    ,x.FAILURE_COUNT \n"
			+ "    ,x.NEXT_RUN_TIME \n"
			+ "    ,x.SCHEDULE \n"
			+ "    ,x.JOB_ACTION \n"
			+ "    ,x.LAST_RUN_INFO \n"
			+ "from ( \n"

			// The jobs of the Scheduler
			+ "    select \n"
			+ "         " + (cdb ? "j.CON_ID, \n         " : "") + "CAST('SCHEDULER' as VARCHAR2(10))                             as JOB_KIND \n"
			+ "        ,j.OWNER \n"
			+ "        ,CAST(j.JOB_NAME as VARCHAR2(128))                             as JOB_NAME \n"
			+ "        ,CAST(j.ENABLED  as VARCHAR2(5))                               as ENABLED \n"
			+ "        ,CAST(j.STATE    as VARCHAR2(20))                              as STATE \n"
			+ "        ,CAST(r.STATUS   as VARCHAR2(30))                              as LAST_RUN_STATUS \n"
			+ "        ,SYSDATE - " + jLastStartDays + " as LAST_RUN_TIME \n"
			+ "        ,CAST(" + jLastStartDays + " * 24 as NUMBER(10,1)) as LAST_RUN_AGE_HOURS \n"
			+ "        ,CAST(" + jDurationSec + " as NUMBER(10)) as LAST_RUN_SECONDS \n"
			+ "        ,CAST(r.ERROR# as NUMBER(10))                                  as LAST_RUN_ERROR \n"
			+ "        ,CAST(NVL(j.RUN_COUNT,     0) as NUMBER(18))                   as RUN_COUNT \n"
			+ "        ,CAST(NVL(j.FAILURE_COUNT, 0) as NUMBER(18))                   as FAILURE_COUNT \n"
			+ "        ,SYSDATE - " + jNextRunDays + " as NEXT_RUN_TIME \n"
			+ "        ,CAST(substr(NVL(j.REPEAT_INTERVAL, j.SCHEDULE_NAME), 1, 400) as VARCHAR2(400)) as SCHEDULE \n"
			+ "        ,CAST(substr(NVL(j.PROGRAM_NAME,    j.JOB_ACTION),    1, 400) as VARCHAR2(400)) as JOB_ACTION \n"
			+ "        ,CAST(substr(r.ADDITIONAL_INFO, 1, 1000) as VARCHAR2(1000))    as LAST_RUN_INFO \n"
			+ "    from " + views + "_SCHEDULER_JOBS j \n"
			+ "    left join r on r.OWNER = j.OWNER and r.JOB_NAME = j.JOB_NAME and r.IS_AUTOTASK = 0" + (cdb ? " and r.CON_ID = j.CON_ID" : "") + " \n"
			+ "    where j.JOB_SUBNAME is null \n"

			// The automatic maintenance tasks: they are only jobs while they run, so they are taken from the log
			+ "    union all \n"
			+ "    select \n"
			+ "         " + (cdb ? "r.CON_ID, \n         " : "") + "CAST('AUTOTASK' as VARCHAR2(10)) \n"
			+ "        ,r.OWNER \n"
			+ "        ,CAST(decode(r.JOB_NAME, 'ORA$AT_OS_OPT_SY', 'auto optimizer stats collection', 'ORA$AT_SA_SPC_SY', 'auto space advisor', 'ORA$AT_SQ_SQL_SW', 'sql tuning advisor', r.JOB_NAME) as VARCHAR2(128)) \n"
			+ "        ,CAST(NULL as VARCHAR2(5)) \n"
			+ "        ,CAST(NULL as VARCHAR2(20)) \n"
			+ "        ,CAST(r.STATUS as VARCHAR2(30)) \n"
			+ "        ,SYSDATE - " + rLastStartDays + " \n"
			+ "        ,CAST(" + rLastStartDays + " * 24 as NUMBER(10,1)) \n"
			+ "        ,CAST(" + rDurationSec + " as NUMBER(10)) \n"
			+ "        ,CAST(r.ERROR# as NUMBER(10)) \n"
			+ "        ,CAST(0 as NUMBER(18)) \n"
			+ "        ,CAST(0 as NUMBER(18)) \n"
			+ "        ,CAST(NULL as DATE) \n"
			+ "        ,CAST('in the maintenance windows' as VARCHAR2(400)) \n"
			+ "        ,CAST(r.JOB_NAME || '_nnn' as VARCHAR2(400)) \n"
			+ "        ,CAST(substr(r.ADDITIONAL_INFO, 1, 1000) as VARCHAR2(1000)) \n"
			+ "    from r \n"
			+ "    where r.IS_AUTOTASK = 1 \n"
			+ "";

		// The old style jobs. DBA_JOBS has no log: FAILURES is the number of failed runs since the last one that worked, and LAST_DATE is the last run that worked
		if (withDbaJobs)
		{
			sql += ""
				+ "    union all \n"
				+ "    select \n"
				+ "         " + (cdb ? "b.CON_ID, \n         " : "") + "CAST('DBMS_JOB' as VARCHAR2(10)) \n"
				+ "        ,b.SCHEMA_USER \n"
				+ "        ,CAST(to_char(b.JOB) as VARCHAR2(128)) \n"
				+ "        ,CAST(decode(b.BROKEN, 'Y', 'FALSE', 'TRUE') as VARCHAR2(5)) \n"
				+ "        ,CAST(case when b.BROKEN = 'Y' then 'BROKEN' when b.THIS_DATE is not null then 'RUNNING' else 'SCHEDULED' end as VARCHAR2(20)) \n"
				+ "        ,CAST(case when b.FAILURES > 0 then 'FAILED' when b.LAST_DATE is not null then 'SUCCEEDED' end as VARCHAR2(30)) \n"
				+ "        ,case when NVL(b.FAILURES, 0) = 0 then b.LAST_DATE end \n"
				+ "        ,CAST(case when NVL(b.FAILURES, 0) = 0 then (SYSDATE - b.LAST_DATE) * 24 end as NUMBER(10,1)) \n"
				+ "        ,CAST(NULL as NUMBER(10)) \n"
				+ "        ,CAST(NULL as NUMBER(10)) \n"
				+ "        ,CAST(0 as NUMBER(18)) \n"
				+ "        ,CAST(NVL(b.FAILURES, 0) as NUMBER(18)) \n"
				+ "        ,b.NEXT_DATE \n"
				+ "        ,CAST(substr(b.INTERVAL, 1, 400) as VARCHAR2(400)) \n"
				+ "        ,CAST(substr(b.WHAT,     1, 400) as VARCHAR2(400)) \n"
				+ "        ,CAST(NULL as VARCHAR2(1000)) \n"
				+ "    from " + views + "_JOBS b \n"
				+ "";
		}

		sql += ""
			+ ") x \n"
			+ (cdb ? "left join V$CONTAINERS c on c.CON_ID = x.CON_ID \n" : "")
			+ "order by " + (cdb ? "x.CON_ID, " : "") + "x.JOB_KIND, x.OWNER, x.JOB_NAME \n"
			+ "";

		return sql;
	}

	@Override
	public void addMonTableDictForVersion(DbxConnection conn, DbmsVersionInfo versionInfo)
	{
		try
		{
			MonTablesDictionary mtd = MonTablesDictionaryManager.getInstance();

			mtd.addTable(CM_NAME, "The jobs that the database runs by itself, and how the last run of each one went.");

			mtd.addColumn(CM_NAME, "CON_ID",             "<html>Container ID. 0 = This is not a Container Database (or the version is below 12.1), or we are connected to a PDB.</html>");
			mtd.addColumn(CM_NAME, "CON_NAME",           "<html>Name of the Container (PDB) the job is in. NULL if CON_ID is 0.</html>");
			mtd.addColumn(CM_NAME, "JOB_KIND",           "<html><b>SCHEDULER</b> = a job in DBA_SCHEDULER_JOBS.<br><b>AUTOTASK</b> = an automatic maintenance task (it runs in the maintenance windows).<br><b>DBMS_JOB</b> = an old style job in DBA_JOBS (below Oracle 19).</html>");
			mtd.addColumn(CM_NAME, "OWNER",              "<html>The schema that owns the job.</html>");
			mtd.addColumn(CM_NAME, "JOB_NAME",           "<html>Name of the job. For a DBMS_JOB: the job number.</html>");
			mtd.addColumn(CM_NAME, "ENABLED",            "<html>TRUE = the job runs when it's time comes. FALSE = it does not (for a DBMS_JOB: it is BROKEN). Empty for an AUTOTASK.</html>");
			mtd.addColumn(CM_NAME, "STATE",              "<html>SCHEDULED, RUNNING, DISABLED, SUCCEEDED / COMPLETED (a job that will not run again), BROKEN (it failed too many times, or someone marked it), FAILED, CHAIN_STALLED...</html>");
			mtd.addColumn(CM_NAME, "LAST_RUN_STATUS",    "<html>How the last run ended: SUCCEEDED, FAILED, STOPPED (someone stopped it, or the maintenance window closed)...<br>From the log of the Scheduler. Empty = the job has not logged a run the last 31 days (it has not run, or it does not log).<br>For a DBMS_JOB: FAILED when FAILURES is above 0.</html>");
			mtd.addColumn(CM_NAME, "LAST_RUN_TIME",      "<html>When the last run started (the clock of the database server).<br>For a DBMS_JOB: the last run that worked, empty when the last run failed (Oracle does not save when that was).</html>");
			mtd.addColumn(CM_NAME, "LAST_RUN_AGE_HOURS", "<html>Number of hours since LAST_RUN_TIME.</html>");
			mtd.addColumn(CM_NAME, "LAST_RUN_SECONDS",   "<html>How long the last run took, in seconds.</html>");
			mtd.addColumn(CM_NAME, "LAST_RUN_ERROR",     "<html>The ORA- error number of the last run. 0 = no error.</html>");
			mtd.addColumn(CM_NAME, "RUN_COUNT",          "<html>Number of times the job has run. DIFF: the runs since the previous sample.</html>");
			mtd.addColumn(CM_NAME, "FAILURE_COUNT",      "<html>Number of times the job has failed. DIFF: the failures since the previous sample.<br>For a DBMS_JOB: the number of failed runs since the last one that worked (at 16 Oracle marks it as BROKEN).</html>");
			mtd.addColumn(CM_NAME, "NEXT_RUN_TIME",      "<html>When the job will run next (the clock of the database server).</html>");
			mtd.addColumn(CM_NAME, "SCHEDULE",           "<html>When the job runs: the repeat interval, or the name of a schedule or a window.</html>");
			mtd.addColumn(CM_NAME, "JOB_ACTION",         "<html>What the job does: the name of the program, or the start of the PL/SQL that it runs.</html>");
			mtd.addColumn(CM_NAME, "LAST_RUN_INFO",      "<html>What the Scheduler wrote about the last run. When it failed: the error message.</html>");
		}
		catch (NameNotFoundException e)
		{
			_logger.warn("Problems in cm='" + CM_NAME + "', adding addMonTableDictForVersion. Caught: " + e);
		}
	}

	@Override
	public void sendAlarmRequest()
	{
		if ( ! hasAbsData() )
			return;

		if ( ! AlarmHandler.hasInstance() )
			return;

		boolean debugPrint = Configuration.getCombinedConfiguration().getBooleanProperty("sendAlarmRequest.debug", _logger.isDebugEnabled());

		CountersModel cm   = this;
		Configuration conf = Configuration.getCombinedConfiguration();

		String skipJobsRegex = conf.getProperty(PROPKEY_alarm_SkipJobsRegex, DEFAULT_alarm_SkipJobsRegex);

		for (int r=0; r<cm.getAbsRowCount(); r++)
		{
			String jobKind       = cm.getAbsString(r, "JOB_KIND");
			String owner         = cm.getAbsString(r, "OWNER");
			String jobName       = cm.getAbsString(r, "JOB_NAME");
			String conName       = cm.getAbsString(r, "CON_NAME");
			String state         = cm.getAbsString(r, "STATE");
			String lastRunStatus = cm.getAbsString(r, "LAST_RUN_STATUS");
			Double ageInHours    = cm.getAbsValueAsDouble(r, "LAST_RUN_AGE_HOURS");

			if (jobKind == null || owner == null || jobName == null)
				continue;

			// owner.jobName   or in a CDB: container:owner.jobName
			String ownerAndJob = owner + "." + jobName;
			String jobLabel    = StringUtil.hasValue(conName) ? conName + ":" + ownerAndJob : ownerAndJob;

			// Jobs we do not care about
			if (StringUtil.hasValue(skipJobsRegex) && (ownerAndJob.matches(skipJobsRegex) || jobLabel.matches(skipJobsRegex)))
				continue;

			//-------------------------------------------------------
			// JobFailed: the LAST run failed, and it is not too long ago
			// (an old style DBMS_JOB has no time for a failed run: it is "failed" until a run works)
			//-------------------------------------------------------
			if (isSystemAlarmsForColumnEnabledAndInTimeRange("JobFailed"))
			{
				int threshold = conf.getIntProperty(PROPKEY_alarm_JobFailedAgeInHours, DEFAULT_alarm_JobFailedAgeInHours);

				if (debugPrint || _logger.isDebugEnabled())
					System.out.println("##### sendAlarmRequest(" + cm.getName() + "): JobFailed: job='" + jobLabel + "', jobKind='" + jobKind + "', threshold=" + threshold + ", LAST_RUN_STATUS='" + lastRunStatus + "', LAST_RUN_AGE_HOURS='" + ageInHours + "'.");

				if ("FAILED".equals(lastRunStatus) && (ageInHours == null || ageInHours < threshold))
				{
					Double errorNumber = cm.getAbsValueAsDouble(r, "LAST_RUN_ERROR");

					AlarmEvent ae = new AlarmEventOraJobFailed(cm, jobKind, jobLabel, cm.getAbsString(r, "LAST_RUN_TIME"), ageInHours, (errorNumber == null ? null : errorNumber.intValue()), cm.getAbsString(r, "LAST_RUN_INFO"), threshold);

					// The below is called from AlarmHandler on: Raise, RE-RAISE & CANCEL
					ae.setAlarmDescriptionProvider(this, new AlarmDescriptionProviderBase(cm.getPk(), cm.getAbsPkValue(r), threshold)
					{
						@Override
						public void setValues(CountersModel cm, AlarmEvent alarmEvent, AlarmPhase phase)
						{
							int    rowId = cm.getAbsRowIdForPkValue(getRowPk());
							String label = "Job '" + jobLabel + "'";

							if (rowId != -1)
							{
								// Always: (RAISE, RE-RAISE, CANCEL) - Set the row as a table
								String extendedDescText = cm.toTextTableString(DATA_ABS, rowId);
								String extendedDescHtml = cm.toHtmlTableString(DATA_ABS, rowId, true, false, false);
								alarmEvent.setExtendedDescription(extendedDescText, extendedDescHtml);

								// Set CANCEL message (values that was found AFTER last raise/re-raise event)
								if (AlarmPhase.CANCEL.equals(phase))
									alarmEvent.setCancelDescription(getAlarmCancelText(label + ": the last run", "'" + cm.getAbsString(rowId, "LAST_RUN_STATUS") + "', it started " + toAlarmCancelValue(cm.getAbsValueAsDouble(rowId, "LAST_RUN_AGE_HOURS")) + " hours ago", " hours"));
							}
							else
							{
								// The row is gone: the job was dropped (or it is an automatic task with no run in the log any more)
								if (AlarmPhase.CANCEL.equals(phase))
									alarmEvent.setCancelDescription(getAlarmCancelTextGone(label));
							}
						}
					});

					// Information about how to disable this alarm
					ae.createAlarmOptionsMessage(this, "JobFailed");

					AlarmHandler.getInstance().addAlarm(ae);
				}
			} // end: JobFailed

			//-------------------------------------------------------
			// JobBroken: the job is in a state where it will not run again by itself
			//-------------------------------------------------------
			if (isSystemAlarmsForColumnEnabledAndInTimeRange("JobBroken"))
			{
				String stateRegex = conf.getProperty(PROPKEY_alarm_JobBrokenStates, DEFAULT_alarm_JobBrokenStates);

				if (debugPrint || _logger.isDebugEnabled())
					System.out.println("##### sendAlarmRequest(" + cm.getName() + "): JobBroken: job='" + jobLabel + "', jobKind='" + jobKind + "', stateRegex='" + stateRegex + "', STATE='" + state + "'.");

				if (state != null && StringUtil.hasValue(stateRegex) && state.matches(stateRegex))
				{
					Double failureCount = cm.getAbsValueAsDouble(r, "FAILURE_COUNT");

					AlarmEvent ae = new AlarmEventOraJobFailed(cm, jobKind, jobLabel, state, (failureCount == null ? 0 : failureCount.longValue()), stateRegex);

					// The below is called from AlarmHandler on: Raise, RE-RAISE & CANCEL
					ae.setAlarmDescriptionProvider(this, new AlarmDescriptionProviderBase(cm.getPk(), cm.getAbsPkValue(r), null)
					{
						@Override
						public void setValues(CountersModel cm, AlarmEvent alarmEvent, AlarmPhase phase)
						{
							int    rowId = cm.getAbsRowIdForPkValue(getRowPk());
							String label = "Job '" + jobLabel + "'";

							if (rowId != -1)
							{
								// Always: (RAISE, RE-RAISE, CANCEL) - Set the row as a table
								String extendedDescText = cm.toTextTableString(DATA_ABS, rowId);
								String extendedDescHtml = cm.toHtmlTableString(DATA_ABS, rowId, true, false, false);
								alarmEvent.setExtendedDescription(extendedDescText, extendedDescHtml);

								// Set CANCEL message (values that was found AFTER last raise/re-raise event)
								if (AlarmPhase.CANCEL.equals(phase))
									alarmEvent.setCancelDescription(getAlarmCancelText(label + ": STATE", "'" + cm.getAbsString(rowId, "STATE") + "'"));
							}
							else
							{
								// The row is gone: the job was dropped
								if (AlarmPhase.CANCEL.equals(phase))
									alarmEvent.setCancelDescription(getAlarmCancelTextGone(label));
							}
						}
					});

					// Information about how to disable this alarm
					ae.createAlarmOptionsMessage(this, "JobBroken");

					AlarmHandler.getInstance().addAlarm(ae);
				}
			} // end: JobBroken

		} // end: loop-rows
	}

	public static final String  PROPKEY_sample_runDetailsDays     = CM_NAME + ".sample.runDetailsDays";
	public static final int     DEFAULT_sample_runDetailsDays     = 31;

	public static final String  PROPKEY_alarm_JobFailedAgeInHours = CM_NAME + ".alarm.system.if.JobFailed.ageInHours.lt";
	public static final int     DEFAULT_alarm_JobFailedAgeInHours = 24;

	public static final String  PROPKEY_alarm_JobBrokenStates     = CM_NAME + ".alarm.system.if.State.in";
	public static final String  DEFAULT_alarm_JobBrokenStates     = "(BROKEN|FAILED|CHAIN_STALLED)";

	// SYS.BSLN_MAINTAIN_STATS_JOB comes with Oracle, and in 11.2 it fails every week on a plain installation (ORA-06550, a known bug in the job itself)
	// The '.' between owner and job is "any character" in a regexp: a backslash is a pain to write in a properties file
	public static final String  PROPKEY_alarm_SkipJobsRegex       = CM_NAME + ".alarm.system.skip.jobs";
	public static final String  DEFAULT_alarm_SkipJobsRegex       = "(SYS.BSLN_MAINTAIN_STATS_JOB)";

	@Override
	protected void registerDefaultValues()
	{
		super.registerDefaultValues();

		Configuration.registerDefaultValue(PROPKEY_sample_runDetailsDays, DEFAULT_sample_runDetailsDays);
	}

	/** Used by the: Create 'Offline Session' Wizard */
	@Override
	public List<CmSettingsHelper> getLocalSettings()
	{
		Configuration conf = Configuration.getCombinedConfiguration();
		List<CmSettingsHelper> list = new ArrayList<>();

		list.add(new CmSettingsHelper("Scheduler log: days back", PROPKEY_sample_runDetailsDays, Integer.class, conf.getIntProperty(PROPKEY_sample_runDetailsDays, DEFAULT_sample_runDetailsDays), DEFAULT_sample_runDetailsDays, "How many days back in the log of the Scheduler (DBA_SCHEDULER_JOB_RUN_DETAILS) to look for the last run of a job. A lower value is faster on a system where the jobs run very often." ));

		return list;
	}

	@Override
	public List<CmSettingsHelper> getLocalAlarmSettings()
	{
		Configuration conf = Configuration.getCombinedConfiguration();
		List<CmSettingsHelper> list = new ArrayList<>();

		CmSettingsHelper.Type isAlarmSwitch = CmSettingsHelper.Type.IS_ALARM_SWITCH;
		CmSettingsHelper.Type isPreCheck    = CmSettingsHelper.Type.IS_PRE_CHECK;

		list.add(new CmSettingsHelper("SkipJobsRegex", isPreCheck   , PROPKEY_alarm_SkipJobsRegex      , String .class, conf.getProperty   (PROPKEY_alarm_SkipJobsRegex      , DEFAULT_alarm_SkipJobsRegex      ), DEFAULT_alarm_SkipJobsRegex      , "Skip jobs where 'OWNER.JOB_NAME' (in a CDB also 'container:OWNER.JOB_NAME') matches this regexp. Default is a job that comes with Oracle, and that fails every week on a plain 11.2 installation.", new RegExpInputValidator()));

		list.add(new CmSettingsHelper("JobFailed"    , isAlarmSwitch, PROPKEY_alarm_JobFailedAgeInHours, Integer.class, conf.getIntProperty(PROPKEY_alarm_JobFailedAgeInHours, DEFAULT_alarm_JobFailedAgeInHours), DEFAULT_alarm_JobFailedAgeInHours, "If 'LAST_RUN_STATUS' is FAILED, and 'LAST_RUN_AGE_HOURS' is less than ## hours, then send 'AlarmEventOraJobFailed'. The alarm goes away when a later run succeeds, or when the failed run is older than this." ));
		list.add(new CmSettingsHelper("JobBroken"    , isAlarmSwitch, PROPKEY_alarm_JobBrokenStates    , String .class, conf.getProperty   (PROPKEY_alarm_JobBrokenStates    , DEFAULT_alarm_JobBrokenStates    ), DEFAULT_alarm_JobBrokenStates    , "If 'STATE' matches this regexp then send 'AlarmEventOraJobFailed': the job will not run again until someone fixes it.", new RegExpInputValidator()));

		return list;
	}
}
