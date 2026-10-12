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
package com.dbxtune.pcs.report.content.oracle;

import java.io.IOException;
import java.io.Writer;

import com.dbxtune.gui.ResultSetTableModel;
import com.dbxtune.pcs.report.DailySummaryReportAbstract;
import com.dbxtune.sql.conn.DbxConnection;
import com.dbxtune.utils.Configuration;

public class OracleJobFailures
extends OracleAbstract
{
	private ResultSetTableModel _shortRstm; // null = the jobs was not recorded

	public OracleJobFailures(DailySummaryReportAbstract reportingInstance)
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
		if (_shortRstm == null)
		{
			w.append("<b>The jobs was not recorded.</b><br>\n");
			return;
		}

		if (_shortRstm.getRowCount() == 0)
		{
			w.append("<b>No job failed during the period, and no job is broken.</b><br>\n");
			return;
		}

		// Get a description of this section, and column names
		w.append(getSectionDescriptionHtml(_shortRstm, true));

		w.append("Row Count: " + _shortRstm.getRowCount() + "<br>\n");
		w.append(toHtmlTable(_shortRstm));
	}

	@Override
	public String getSubject()
	{
		return "Jobs that Failed (origin: CmSchedulerJobs / DBA_SCHEDULER_JOBS, DBA_SCHEDULER_JOB_RUN_DETAILS)";
	}

	@Override
	public boolean hasIssueToReport()
	{
		return false; // even if we found entries, do NOT indicate this as a Problem or Issue (the alarms for the jobs does that)
	}

	@Override
	public void create(DbxConnection conn, String srvName, Configuration pcsSavedConf, Configuration localConf)
	{
		// No mandatory table: "the jobs was not recorded" is something the reader should be told, not a red ERROR
		if ( ! doTableExist(conn, getReportingInstance().getDbmsSchemaName(), "CmSchedulerJobs_abs") )
			return;

		// The collector sees the LAST run of every job, at every sample (every 5 minutes)
		// So: one row for every failed run that was the "last run" at some sample, and one row for a job that is in a state where it does not run
		// A job that fails more often than the collector samples is seen as FAILURE_COUNT going up
		String sql = ""
			    + "select \n"
			    + "     max([CON_NAME])                as [CON_NAME] \n"
			    + "    ,[JOB_KIND] \n"
			    + "    ,[OWNER] \n"
			    + "    ,[JOB_NAME] \n"
			    + "    ,max([STATE])                   as [STATE] \n"
			    + "    ,[LAST_RUN_STATUS] \n"
			    + "    ,[LAST_RUN_TIME] \n"
			    + "    ,max([LAST_RUN_SECONDS])        as [LAST_RUN_SECONDS] \n"
			    + "    ,cast(max([LAST_RUN_ERROR]) as varchar(10)) as [LAST_RUN_ERROR] \n"   // as a string: it is an error number, not something to write as 20 001
			    + "    ,max([FAILURE_COUNT])           as [FAILURE_COUNT] \n"
			    + "    ,max([FAILURE_COUNT]) - min([FAILURE_COUNT]) as [failures_in_period] \n"
			    + "    ,max([SCHEDULE])                as [SCHEDULE] \n"
			    + "    ,max([JOB_ACTION])              as [JOB_ACTION] \n"
			    + "    ,max([LAST_RUN_INFO])           as [LAST_RUN_INFO] \n"
			    + "from [CmSchedulerJobs_abs] \n"
			    + "where ([LAST_RUN_STATUS] = 'FAILED' or [STATE] in ('BROKEN', 'FAILED', 'CHAIN_STALLED')) \n"
			    + getReportPeriodSqlWhere()
			    + "group by [CON_ID], [JOB_KIND], [OWNER], [JOB_NAME], [LAST_RUN_STATUS], [LAST_RUN_TIME] \n"
			    + "order by [LAST_RUN_TIME] desc, [OWNER], [JOB_NAME] \n"
			    + "";

		_shortRstm = executeQuery(conn, sql, true, "OracleJobFailures", false); // false = do not truncate (the error text)

		_shortRstm.setHighlightSortColumns("LAST_RUN_TIME");

		_shortRstm.setDescription(
				"Job runs that failed, and jobs that are in a state where they do not run (from the table <code>CmSchedulerJobs_abs</code>), the newest first.<br>" +
				"The collector looks at the <b>last</b> run of every job every 5 minutes: a failed run is listed if it was the last run at one of those times. " +
				"For a job that runs more often than that, look at <code>failures_in_period</code>.<br>" +
				"A row can be a run from before the period: it is listed as long as it is the last run of the job. <code>STATE</code> = BROKEN means that the job will not run again until someone enables it.<br>" +
				"");

		_shortRstm.setColumnDescription("CON_NAME"          , "Container (PDB) name. Empty when the database is not a Container Database, or when the collector is connected to a PDB.");
		_shortRstm.setColumnDescription("JOB_KIND"          , "SCHEDULER = a job in DBA_SCHEDULER_JOBS, AUTOTASK = an automatic maintenance task, DBMS_JOB = an old style job in DBA_JOBS");
		_shortRstm.setColumnDescription("OWNER"             , "The schema that owns the job");
		_shortRstm.setColumnDescription("JOB_NAME"          , "Name of the job. For a DBMS_JOB: the job number.");
		_shortRstm.setColumnDescription("STATE"             , "State of the job at the last sample where this run was seen: SCHEDULED, RUNNING, DISABLED, BROKEN, FAILED...");
		_shortRstm.setColumnDescription("LAST_RUN_STATUS"   , "How the run ended: FAILED (or something else, for a job that is listed because of it's STATE)");
		_shortRstm.setColumnDescription("LAST_RUN_TIME"     , "When the run started (the clock of the database server). Empty for a DBMS_JOB that failed (Oracle does not save when).");
		_shortRstm.setColumnDescription("LAST_RUN_SECONDS"  , "How long the run took, in seconds");
		_shortRstm.setColumnDescription("LAST_RUN_ERROR"    , "The ORA- error number of the run");
		_shortRstm.setColumnDescription("FAILURE_COUNT"     , "Number of times the job has failed, in all");
		_shortRstm.setColumnDescription("failures_in_period", "How much FAILURE_COUNT went up while this run was the last run of the job");
		_shortRstm.setColumnDescription("SCHEDULE"          , "When the job runs: the repeat interval, or the name of a schedule or a window");
		_shortRstm.setColumnDescription("JOB_ACTION"        , "What the job does: the name of the program, or the start of the PL/SQL that it runs");
		_shortRstm.setColumnDescription("LAST_RUN_INFO"     , "What the Scheduler wrote about the run: the error message");
	}
}
