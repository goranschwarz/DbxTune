/*******************************************************************************
 * Copyright (C) 2010-2020 Goran Schwarz
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
package com.asetune.pcs.report.content.postgres;

import java.io.IOException;
import java.io.Writer;

import com.asetune.gui.ResultSetTableModel;
import com.asetune.pcs.report.DailySummaryReportAbstract;
import com.asetune.pcs.report.content.IReportChart;
import com.asetune.sql.conn.DbxConnection;
import com.asetune.utils.Configuration;

public class PostgresLongRunningStmnts
extends PostgresAbstract
{
//	private static Logger _logger = Logger.getLogger(PostgresLongRunningStmnts.class);

	private ResultSetTableModel _shortRstm;

	public PostgresLongRunningStmnts(DailySummaryReportAbstract reportingInstance)
	{
		super(reportingInstance);
	}

	@Override
	public boolean hasShortMessageText()
	{
		return true;
	}

	@Override
	public void writeMessageText(Writer sb, MessageType messageType)
	throws IOException
	{
		// Get (SQL) info from; CmActiveStatements; // Probably ordered by Xact time (or possibly 2 tables, 1 by Xact_isSeconds, and 1 by Statmnt_inSeconds; 
		// Also in the description: remind that CmActiveStatements are only polled every ## second (so age might not be 100 correct)
		// TODO: add/implement; CmBlockingLocks (even if it's only for PG 14 or above)
		//       if (CmSummary.hasBlockingLocks()) then refresh...
		
		// Last sample Database Size info
		sb.append("For how long (max times in seconds) was any Transactions and Statements running in the DBMS.<br>\n");
		
		sb.append(getDbxCentralLinkWithDescForGraphs(false, "Below are Oldest Transactions and Statements during the day.",
				"CmSummary_oldestComboInSec",
				"CmSummary_oldestXactInSec",
				"CmSummary_oldestStmntInSec",
				"CmSummary_oldestStateInSec",
				"CmSummary_blkLockCount",
				"CmSummary_blkMaxWaitTime"
				));

		if (isFullMessageType())
		{
			String postText = "" 
					+ "<b>NOTE:</b> (on above chart) If <code>idle in transaction</code> is long, it indicates that we have a longer transaction than needed.<br>"
					+ "Either by a manual transaction (<code>start transaction... commit/rollback transaction</code>)<br>"
					+ "Or by running in AutoCommit=<b>false</b> and not turning AutoCommitt=<b>true</b> when the SQL Statements are finnished.<br>"
					+ "<b>Note:</b> If AutoCommit=<b>false</b>, it's not enough to just commit/rollback... That just starts a new transaction...";

			_CmSummary_oldestComboInSec.writeHtmlContent(sb, null, null);
			_CmSummary_oldestXactInSec .writeHtmlContent(sb, null, null);
			_CmSummary_oldestStmntInSec.writeHtmlContent(sb, null, null);
			_CmSummary_oldestStateInSec.writeHtmlContent(sb, null, postText);
			
			_CmSummary_blkLockCount    .writeHtmlContent(sb, null, null);
			_CmSummary_blkMaxWaitTime  .writeHtmlContent(sb, null, null);
		}
		else
		{
			String postText = "" 
					+ "<b>NOTE:</b> (on above chart) If <code>idle in transaction</code> is long, it indicates that we have a longer transaction than needed.<br>"
					+ "Either by a manual transaction (<code>start transaction... commit/rollback transaction</code>)<br>"
					+ "Or by running in AutoCommit=<b>false</b> and not turning AutoCommitt=<b>true</b> when the SQL Statements are finnished.<br>"
					+ "<b>Note:</b> If AutoCommit=<b>false</b>, it's not enough to just commit/rollback... That just starts a new transaction...";

			_CmSummary_oldestComboInSec.writeHtmlContent(sb, null, postText);
			_CmSummary_oldestStateInSec.writeHtmlContent(sb, null, null);
			
			_CmSummary_blkLockCount    .writeHtmlContent(sb, null, null);
			_CmSummary_blkMaxWaitTime  .writeHtmlContent(sb, null, null);
		}


		// Get a description of this section, and column names
		sb.append(getSectionDescriptionHtml(_shortRstm, true));

		// Last sample Database Size info
		sb.append("Row Count: " + _shortRstm.getRowCount() + "<br>\n");
		sb.append(toHtmlTable(_shortRstm));
	}

	@Override
	public String getSubject()
	{
		return "Long Runing Statements Information (origin: CmSummary / pg_stat_activity)";
	}

	@Override
	public boolean hasIssueToReport()
	{
		return false; // even if we found entries, do NOT indicate this as a Problem or Issue
	}


	@Override
	public String[] getMandatoryTables()
	{
		return new String[] { "CmSummary_abs" };
	}

	@Override
	public void create(DbxConnection conn, String srvName, Configuration pcsSavedConf, Configuration localConf)
	{
		int topRows = getTopRows();

		String sql = ""
			    + "select top " + topRows + " \n"
			    + "     * \n"
			    + "from [CmActiveStatements_abs] diff\n"
			    + "order by [xact_start_sec] desc \n" 
			    + "";

		_shortRstm = executeQuery(conn, sql, true, "CmActiveStatements_abs");

		if (_shortRstm != null)
		{
			// Enrich any DCC columns (Dictionary Compressed Column) -- in this case 'query$dcc$' hash to SQL Statement
			fixDictionaryCompressedColumns(conn, _shortRstm, "CmActiveStatements");

			// Remove some "basic columns" columns
			_shortRstm.removeColumn("SessionStartTime");
			_shortRstm.removeColumn("SessionSampleTime");
			_shortRstm.removeColumn("CmSampleTime");
			_shortRstm.removeColumn("CmSampleMs");
			_shortRstm.removeColumn("CmRowState");			

			// Remove some "other columns" columns
			_shortRstm.removeColumn("datid");
			_shortRstm.removeColumn("pid");
			_shortRstm.removeColumn("usesysid");
			
			// Highlight sort column
			_shortRstm.setHighlightSortColumns("xact_start_sec");

			// Describe the table
			setSectionDescription(_shortRstm);
		}

		// Create charts
		_CmSummary_oldestComboInSec = createTsLineChart(conn, "CmSummary", "oldestComboInSec", -1, null, "Oldest Xact/Stmnt/State in Seconds");
		_CmSummary_oldestXactInSec  = createTsLineChart(conn, "CmSummary", "oldestXactInSec" , -1, null, "Oldest Open Transaction in Seconds");
		_CmSummary_oldestStmntInSec = createTsLineChart(conn, "CmSummary", "oldestStmntInSec", -1, null, "Oldest Statement in Seconds");
		_CmSummary_oldestStateInSec = createTsLineChart(conn, "CmSummary", "oldestStateInSec", -1, null, "Oldest State in Seconds");

		_CmSummary_blkLockCount     = createTsLineChart(conn, "CmSummary", "blkLockCount"    , -1, null, "Blocking Locks Count");
		_CmSummary_blkMaxWaitTime   = createTsLineChart(conn, "CmSummary", "blkMaxWaitTime"  , -1, null, "Max Wait Time for Blocking Locks in Seconds");
	}

	private IReportChart _CmSummary_oldestComboInSec;
	private IReportChart _CmSummary_oldestXactInSec;
	private IReportChart _CmSummary_oldestStmntInSec;
	private IReportChart _CmSummary_oldestStateInSec;
	
	private IReportChart _CmSummary_blkLockCount;
	private IReportChart _CmSummary_blkMaxWaitTime;
	

	
	/**
	 * Set descriptions for the table, and the columns
	 */
	private void setSectionDescription(ResultSetTableModel rstm)
	{
		if (rstm == null)
			return;
		
		int pollTime = getReportingInstance().getRecordingSampleTime();

		// Section description
		rstm.setDescription(
				"Information about longest Transactions from table <code>CmActiveStatements_abs</code><br>" +
				"<b>Note:</b> Information in CmActiveStatements is only polled every " + pollTime + " seconds...<br>");
	}
}
