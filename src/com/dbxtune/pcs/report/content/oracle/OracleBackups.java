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

public class OracleBackups
extends OracleAbstract
{
	private ResultSetTableModel _shortRstm; // null = no RMAN jobs was recorded

	public OracleBackups(DailySummaryReportAbstract reportingInstance)
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
		if (_shortRstm == null || _shortRstm.getRowCount() == 0)
		{
			w.append("<b>No RMAN backup jobs was recorded.</b><br>\n");
			w.append("Either no RMAN backup has been taken the last days, or the backups are not taken with RMAN, or the collector is connected to a PDB (the RMAN jobs are only visible in CDB$ROOT).<br>\n");
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
		return "RMAN Backups (origin: CmRmanBackup / V$RMAN_BACKUP_JOB_DETAILS)";
	}

	@Override
	public boolean hasIssueToReport()
	{
		return false; // even if we found entries, do NOT indicate this as a Problem or Issue (the alarms for backups does that)
	}

	@Override
	public void create(DbxConnection conn, String srvName, Configuration pcsSavedConf, Configuration localConf)
	{
		// No mandatory table: "no RMAN jobs" is a normal state (and something the reader should be told), not a red ERROR
		if ( ! doTableExist(conn, getReportingInstance().getDbmsSchemaName(), "CmRmanBackup_abs") )
			return;

		// The RMAN jobs at the last sample (the CM keeps the jobs of the last days)
		String sql = ""
			    + "select \n"
			    + "     [INPUT_TYPE] \n"
			    + "    ,[STATUS] \n"
			    + "    ,[OUTCOME] \n"
			    + "    ,[IS_LAST_OF_TYPE] \n"
			    + "    ,[START_TIME] \n"
			    + "    ,[END_TIME] \n"
			    + "    ,[ELAPSED_SECONDS] \n"
			    + "    ,[AGE_HOURS] \n"
			    + "    ,[INPUT_MB] \n"
			    + "    ,[OUTPUT_MB] \n"
			    + "    ,[COMPRESSION_RATIO] \n"
			    + "    ,[OUTPUT_MB_PER_SEC] \n"
			    + "    ,[OUTPUT_DEVICE_TYPE] \n"
			    + "    ,[SESSION_KEY] \n"
			    + "from [CmRmanBackup_abs] \n"
			    + "where [SessionSampleTime] = (select max([SessionSampleTime]) from [CmRmanBackup_abs] where 1 = 1 " + getReportPeriodSqlWhere().trim() + ") \n"
			    + "order by [START_TIME] desc \n"
			    + "";

		_shortRstm = executeQuery(conn, sql, true, "OracleBackups");

		_shortRstm.setHighlightSortColumns("START_TIME");

		_shortRstm.setDescription(
				"The RMAN jobs at the last collector sample (from the table <code>CmRmanBackup_abs</code>), the newest first.<br>" +
				"Look for: <code>OUTCOME</code> = FAILED, and how old the newest database backup is (<code>INPUT_TYPE</code> 'DB FULL' or 'DB INCR' with <code>IS_LAST_OF_TYPE</code> = YES).<br>" +
				"");

		_shortRstm.setColumnDescription("INPUT_TYPE"        , "What was backed up: DB FULL, DB INCR, ARCHIVELOG, CONTROLFILE, SPFILE, DATAFILE FULL...");
		_shortRstm.setColumnDescription("STATUS"            , "Status from RMAN: COMPLETED, COMPLETED WITH WARNINGS, COMPLETED WITH ERRORS, FAILED, RUNNING...");
		_shortRstm.setColumnDescription("OUTCOME"           , "A simpler version of STATUS: OK, FAILED or RUNNING");
		_shortRstm.setColumnDescription("IS_LAST_OF_TYPE"   , "YES = this is the newest finished job of this INPUT_TYPE");
		_shortRstm.setColumnDescription("START_TIME"        , "When the job started (the database servers time)");
		_shortRstm.setColumnDescription("END_TIME"          , "When the job ended (the database servers time)");
		_shortRstm.setColumnDescription("ELAPSED_SECONDS"   , "How long the job took, in seconds");
		_shortRstm.setColumnDescription("AGE_HOURS"         , "Hours since the job ended, counted at the last collector sample");
		_shortRstm.setColumnDescription("INPUT_MB"          , "How much that was read, in MB");
		_shortRstm.setColumnDescription("OUTPUT_MB"         , "How much that was written (the size of the backup), in MB");
		_shortRstm.setColumnDescription("COMPRESSION_RATIO" , "INPUT_MB / OUTPUT_MB");
		_shortRstm.setColumnDescription("OUTPUT_MB_PER_SEC" , "Write speed, in MB per second");
		_shortRstm.setColumnDescription("OUTPUT_DEVICE_TYPE", "DISK or SBT_TAPE (a media manager)");
		_shortRstm.setColumnDescription("SESSION_KEY"       , "The id of the RMAN session (V$RMAN_BACKUP_JOB_DETAILS.SESSION_KEY)");
	}
}
