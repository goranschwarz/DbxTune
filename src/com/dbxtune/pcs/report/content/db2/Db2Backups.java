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
package com.dbxtune.pcs.report.content.db2;

import java.io.IOException;
import java.io.Writer;

import com.dbxtune.gui.ResultSetTableModel;
import com.dbxtune.pcs.report.DailySummaryReportAbstract;
import com.dbxtune.sql.conn.DbxConnection;
import com.dbxtune.utils.Configuration;

/**
 * The backups and the log archiving of the database at the last sample (from CmBackups). Built on OracleBackups.
 */
public class Db2Backups
extends Db2Abstract
{
	private ResultSetTableModel _shortRstm; // null = not recorded

	public Db2Backups(DailySummaryReportAbstract reportingInstance)
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
			w.append("<b>The backups were not recorded</b> (the table <code>CmBackups_abs</code> does not exist: the Counter Model <code>CmBackups</code> is disabled).<br>\n");
			return;
		}

		if (_shortRstm.getRowCount() == 0)
		{
			w.append("<b>No backups or log archives at all</b> in the recovery history of the database (SYSIBMADM.DB_HISTORY). The database can not be restored.<br>\n");
			return;
		}

		w.append(getSectionDescriptionHtml(_shortRstm, true));
		w.append("Row Count: " + _shortRstm.getRowCount() + "<br>\n");
		w.append(toHtmlTable(_shortRstm));
	}

	@Override
	public String getSubject()
	{
		return "Backups and Log Archiving (origin: CmBackups / SYSIBMADM.DB_HISTORY)";
	}

	@Override
	public boolean hasIssueToReport()
	{
		return false; // even if we found entries, do NOT indicate this as a Problem or Issue (the alarms does that)
	}

	@Override
	public void create(DbxConnection conn, String srvName, Configuration pcsSavedConf, Configuration localConf)
	{
		// No mandatory table: a disabled CM is a text, not a red ERROR
		if ( ! doTableExist(conn, getReportingInstance().getDbmsSchemaName(), "CmBackups_abs") )
			return;

		// The kinds of backup at the last collector sample (the CM reads the history every 10 minutes)
		String sql = ""
			    + "select \n"
			    + "     [BACKUP_KIND] \n"
			    + "    ,[LAST_STATUS] \n"
			    + "    ,[LAST_SQLCODE] \n"
			    + "    ,[LAST_START_TIME] \n"
			    + "    ,[LAST_END_TIME] \n"
			    + "    ,[LAST_DURATION_SEC] \n"
			    + "    ,[LAST_GOOD_END_TIME] \n"
			    + "    ,[LAST_GOOD_AGE_HOURS] \n"
			    + "    ,[COUNT_24H] \n"
			    + "    ,[FAILED_24H] \n"
			    + "    ,[COUNT_7D] \n"
			    + "    ,[FAILED_7D] \n"
			    + "    ,[LAST_DEVICETYPE] \n"
			    + "    ,[LAST_LOCATION] \n"
			    + "from [CmBackups_abs] \n"
			    + "where [SessionSampleTime] = (select max([SessionSampleTime]) from [CmBackups_abs] where 1 = 1 " + getReportPeriodSqlWhere().trim() + ") \n"
			    + "order by [OPERATION], [LAST_START_TIME] desc \n"
			    + "";

		_shortRstm = executeQuery(conn, sql, true, "Db2Backups");

		_shortRstm.setHighlightSortColumns("LAST_START_TIME");

		_shortRstm.setDescription(
				"The backups and the log archiving at the last collector sample (from the table <code>CmBackups_abs</code>): one row per kind of backup, and per log archive method.<br>" +
				"Look for: <code>LAST_STATUS</code> = FAILED, and how old the newest good database backup is (<code>LAST_GOOD_AGE_HOURS</code>).<br>" +
				"");

		_shortRstm.setColumnDescription("BACKUP_KIND"        , "Online or offline; full, incremental or delta; database or table space. Or the log archive method (LOGARCHMETH1/2)");
		_shortRstm.setColumnDescription("LAST_STATUS"        , "The last one: OK, FAILED or RUNNING");
		_shortRstm.setColumnDescription("LAST_SQLCODE"       , "The SQLCODE of the last one (0 = OK). Look it up with: db2 ? SQL<number>");
		_shortRstm.setColumnDescription("LAST_START_TIME"    , "When the last one started (the server's time)");
		_shortRstm.setColumnDescription("LAST_END_TIME"      , "When the last one ended");
		_shortRstm.setColumnDescription("LAST_DURATION_SEC"  , "How long the last one took, in seconds");
		_shortRstm.setColumnDescription("LAST_GOOD_END_TIME" , "When the last one that went OK ended");
		_shortRstm.setColumnDescription("LAST_GOOD_AGE_HOURS", "Hours since the last good one, at the last collector sample");
		_shortRstm.setColumnDescription("COUNT_24H"          , "How many in the last 24 hours");
		_shortRstm.setColumnDescription("FAILED_24H"         , "How many failed in the last 24 hours");
		_shortRstm.setColumnDescription("COUNT_7D"           , "How many in the last 7 days");
		_shortRstm.setColumnDescription("FAILED_7D"          , "How many failed in the last 7 days");
		_shortRstm.setColumnDescription("LAST_DEVICETYPE"    , "D = disk, A = TSM, O = other vendor, N = null device");
		_shortRstm.setColumnDescription("LAST_LOCATION"      , "Where the last one was written");
	}
}
