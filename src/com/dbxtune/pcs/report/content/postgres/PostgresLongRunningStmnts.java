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
package com.dbxtune.pcs.report.content.postgres;

import java.io.IOException;
import java.io.Writer;

import com.dbxtune.cm.postgres.CmSummary;
import com.dbxtune.gui.ResultSetTableModel;
import com.dbxtune.gui.ResultSetTableModel.TableStringRenderer;
//import com.dbxtune.gui.ResultSetTableModel.TableStringRenderer; // This needs to be here, but in case of "Organize Imports" (Ctl-O) it may disappear, leading to error !!!
import com.dbxtune.pcs.report.DailySummaryReportAbstract;
import com.dbxtune.pcs.report.content.IReportChart;
import com.dbxtune.sql.conn.DbxConnection;
import com.dbxtune.utils.Configuration;
import com.dbxtune.utils.SqlUtils;
import com.dbxtune.utils.SqlUtils.SqlDialict;
import com.dbxtune.utils.StringUtil;

public class PostgresLongRunningStmnts
extends PostgresAbstract
{
//	private static final Logger _logger = LogManager.getLogger(MethodHandles.lookup().lookupClass());

	private ResultSetTableModel _shortRstm;
	private ResultSetTableModel _msoRstm; // Special table for MS Outlook

	public PostgresLongRunningStmnts(DailySummaryReportAbstract reportingInstance)
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


		// Create a special renderer to control the HTML table content
		class TmpRenderer 
		implements ResultSetTableModel.TableStringRenderer
		{
			@Override
			public String cellValue(ResultSetTableModel rstm, int row, int col, String colName, Object objVal, String strVal)
			{
				// Format the SQL Statement
				if ("last_known_sql_statement".equals(colName))
				{
					if (StringUtil.hasValue(strVal) && ! ResultSetTableModel.DEFAULT_NULL_REPLACE.equals(strVal))
					{
						strVal = "<details class='dsr-pg-lrs-details'>" // removed 'open'
							+ "  <summary>SQL Statement</summary>"
							+ "  <i>-- <font size='-2'>Note: below SQL Statement is formatted (not the original) </i></font><br>"
							+ "  <pre>" + SqlUtils.format(strVal, SqlDialict.Postgres) + "</pre>"
							+ "</details>";
					}
				}

				// Format the SQL Statement
				if ("pid_lock_info".equals(colName))
				{
					if (StringUtil.hasValue(strVal) && ! ResultSetTableModel.DEFAULT_NULL_REPLACE.equals(strVal))
					{
						strVal = "<details class='dsr-pg-lrs-details'>" // removed 'open'
							+ "  <summary>Lock Info</summary>"
							+    strVal
							+ "</details>";
					}
				}

				// Format the SQL Statement
				if ("blocked_pids_info".equals(colName))
				{
					if (StringUtil.hasValue(strVal) && ! ResultSetTableModel.DEFAULT_NULL_REPLACE.equals(strVal))
					{
						strVal = "<details class='dsr-pg-lrs-details' open>" // keep 'open' since we normally do NOT have blocking locks
							+ "  <summary>Blocked PIDs Info</summary>"
							+    strVal
							+ "</details>";
					}
				}
				
				return TableStringRenderer.super.cellValue(rstm, row, col, colName, objVal, strVal);
			}
		}

		String detailsForAboveStatements = ""
				+ "<details> \n"
				+ "<summary>Details for above Statements</summary> \n"
				+ "<br> \n"

				// Get a description of this section, and column names
				+ getSectionDescriptionHtml(_shortRstm, true)
				
				// As a html table
				+ "Row Count: " + _shortRstm.getRowCount() + " &emsp; &emsp; \n"
				+ "<a href='javascript:void(0)' onClick=\"openCloseDsrPgLrsDetails('open')\">Open</a>  \n"
				+ "or <a href='javascript:void(0)' onClick=\"openCloseDsrPgLrsDetails('close')\">Close</a> \n"
				+ " All <i>closable</i> items in the below table.<br> \n"

//				+ toHtmlTable(_shortRstm)
				+ _shortRstm.toHtmlTableString("sortable", true, true, null, new TmpRenderer())
				
				+ "<a href='javascript:void(0)' onClick=\"openCloseDsrPgLrsDetails('open')\">Open</a>  \n"
				+ "or <a href='javascript:void(0)' onClick=\"openCloseDsrPgLrsDetails('close')\">Close</a> \n"
				+ " All <i>closable</i> items in the above table.<br> \n"
				
				+ "\n"
				+ "</details> \n"
				+ "\n"

				+ "\n"
				+ "<script> \n"
				+ "function openCloseDsrPgLrsDetails(openOrClose)                        \n"
				+ "{                                                                     \n"
				+ "    const details = document.querySelectorAll('.dsr-pg-lrs-details'); \n"
				+ "                                                                      \n"
				+ "    details.forEach( (detail) => {                                    \n"
				+ "        if ('open'  === openOrClose) detail.setAttribute('open', true); \n"
				+ "        if ('close' === openOrClose) detail.removeAttribute('open');    \n"
				+ "    });                                                               \n"
				+ "}                                                                     \n"
				+ "</script> \n"
				+ "\n"
				+ "";

		String detailsForAboveStatementsMso = ""
				+ ""
				+ "Details for above Statements  <br>"
				+ "<i>In Outlook, a shorter table is presented. To view the full content with SQL and Lock/Blocking Info... open the attached HTML Report.</i> <br>\n"
				+ "<br> \n"
				+ ""
				// As a html table
				+ "Row Count: " + _msoRstm.getRowCount() + " \n"
				+ ""
//				+ toHtmlTable(_shortRstm)
				+ _msoRstm.toHtmlTableString("sortable", true, true, null, null)
				+ "";

		// If MS Outlook, hide the content, since it can't handle it in a good manner
		// TODO: Possibly copy '_shortRstm' into it's own table just for MS Outlook, which holds even less information!
		sb.append(msOutlookAlternateText(detailsForAboveStatements, "Details for above Statements", detailsForAboveStatementsMso));
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

//		String sql = ""
//			    + "select top " + topRows + " \n"
//			    + "     * \n"
//			    + "from [CmActiveStatements_abs] diff\n"
//			    + "order by [xact_start_sec] desc \n" 
//			    + "";

		String sql = ""
			    + "WITH sqlMaxTime AS ( \n"
			    + "    SELECT [last_known_sql_statement], MAX([xact_start_sec]) AS [max_xact_start_sec] \n"
			    + "    FROM [CmActiveStatements_abs] \n"
			    + "    GROUP BY [last_known_sql_statement] \n"
			    + "    ORDER BY [max_xact_start_sec] DESC \n"
			    + ") \n"
			    + "SELECT top " + topRows + " \n"
			    + "    cm.* \n"
			    + "FROM sqlMaxTime \n"
			    + "JOIN [CmActiveStatements_abs] cm ON sqlMaxTime.[last_known_sql_statement] = cm.[last_known_sql_statement]  AND sqlMaxTime.[max_xact_start_sec] = cm.[xact_start_sec] \n"
			    + "ORDER BY [xact_start_sec] DESC \n"
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
//			_shortRstm.removeColumn("datname");								// keep in table
			_shortRstm.removeColumn("leader_pid");
//			_shortRstm.removeColumn("pid");  								// keep in table
//			_shortRstm.removeColumn("state");								// keep in table
//			_shortRstm.removeColumn("wait_event_type");						// keep in table
//			_shortRstm.removeColumn("wait_event");							// keep in table
//			_shortRstm.removeColumn("im_blocked_by_pids");					// keep in table
//			_shortRstm.removeColumn("im_blocking_other_pids");				// keep in table
//			_shortRstm.removeColumn("im_blocking_others_max_time_in_sec");	// keep in table
			_shortRstm.removeColumn("usesysid");
//			_shortRstm.removeColumn("usename");								// keep in table
//			_shortRstm.removeColumn("application_name");					// keep in table
			_shortRstm.removeColumn("has_sql_text");
			_shortRstm.removeColumn("has_pid_lock_info");
//			_shortRstm.removeColumn("pid_lock_count");						// keep in table
			_shortRstm.removeColumn("has_blocked_pids_info");
//			_shortRstm.removeColumn("xact_start_sec");						// keep in table
//			_shortRstm.removeColumn("stmnt_start_sec");						// keep in table
//			_shortRstm.removeColumn("stmnt_last_exec_sec");					// keep in table
//			_shortRstm.removeColumn("in_current_state_sec");				// keep in table
			_shortRstm.removeColumn("xact_start_age");
			_shortRstm.removeColumn("stmnt_start_age");
			_shortRstm.removeColumn("stmnt_last_exec_age");
			_shortRstm.removeColumn("in_current_state_age");
			_shortRstm.removeColumn("backend_start");
			_shortRstm.removeColumn("xact_start");
			_shortRstm.removeColumn("query_start");
			_shortRstm.removeColumn("state_change");
			_shortRstm.removeColumn("backend_xid");
			_shortRstm.removeColumn("backend_xmin");
//			_shortRstm.removeColumn("backend_type");						// keep in table
//			_shortRstm.removeColumn("client_addr");   						// keep in table
			_shortRstm.removeColumn("client_hostname");
			_shortRstm.removeColumn("client_port");
			_shortRstm.removeColumn("query_id");
//			_shortRstm.removeColumn("last_known_sql_statement");			// keep in table
//			_shortRstm.removeColumn("pid_lock_info");						// keep in table
//			_shortRstm.removeColumn("blocked_pids_info");					// keep in table
			_shortRstm.removeColumn("execTimeInMs");
			_shortRstm.removeColumn("xactTimeInMs");
			
			// Highlight sort column
			_shortRstm.setHighlightSortColumns("xact_start_sec");

			// Describe the table
			setSectionDescription(_shortRstm);
			
			//------------------------------------------------------------------------
			// Copy the "normal" table to a specialized table for MS Outlook
			// Then remove even more columns to get a better overview from in a MS Outlook mail
			_msoRstm = _shortRstm.copy();

			_msoRstm.removeColumn("has_sql_text");
			_msoRstm.removeColumn("has_pid_lock_info");
			_msoRstm.removeColumn("has_blocked_pids_info");
			
			_msoRstm.removeColumn("last_known_sql_statement");
			_msoRstm.removeColumn("pid_lock_info");
			_msoRstm.removeColumn("blocked_pids_info");
			
			_msoRstm.setHighlightSortColumns("xact_start_sec");

			setSectionDescription(_msoRstm);
		}

		String schema = getReportingInstance().getDbmsSchemaName();

		// Create charts
		_CmSummary_oldestComboInSec = createTsLineChart(conn, schema, CmSummary.CM_NAME, CmSummary.GRAPH_NAME_OLDEST_COMBO_IN_SEC,    -1, false, null, "Oldest Xact/Stmnt/State in Seconds");
		_CmSummary_oldestXactInSec  = createTsLineChart(conn, schema, CmSummary.CM_NAME, CmSummary.GRAPH_NAME_OLDEST_XACT_IN_SEC ,    -1, false, null, "Oldest Open Transaction in Seconds");
		_CmSummary_oldestStmntInSec = createTsLineChart(conn, schema, CmSummary.CM_NAME, CmSummary.GRAPH_NAME_OLDEST_STMNT_IN_SEC,    -1, false, null, "Oldest Statement in Seconds");
		_CmSummary_oldestStateInSec = createTsLineChart(conn, schema, CmSummary.CM_NAME, CmSummary.GRAPH_NAME_OLDEST_STATE_IN_SEC,    -1, false, null, "Oldest State in Seconds");

		_CmSummary_blkLockCount     = createTsLineChart(conn, schema, CmSummary.CM_NAME, CmSummary.GRAPH_NAME_BLOCKING_LOCK_COUNT,    -1, false, null, "Blocking Locks Count");
		_CmSummary_blkMaxWaitTime   = createTsLineChart(conn, schema, CmSummary.CM_NAME, CmSummary.GRAPH_NAME_BLOCKING_MAX_WAIT_TIME, -1, false, null, "Max Wait Time for Blocking Locks in Seconds");
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
