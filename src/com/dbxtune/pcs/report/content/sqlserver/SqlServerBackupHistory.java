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
package com.dbxtune.pcs.report.content.sqlserver;

import java.io.IOException;
import java.io.Writer;

import com.dbxtune.CounterControllerSqlServer;
import com.dbxtune.gui.ResultSetTableModel;
import com.dbxtune.gui.ResultSetTableModel.TableStringRenderer;
import com.dbxtune.pcs.SqlServerBackupHistoryExtractor;
import com.dbxtune.pcs.report.DailySummaryReportAbstract;
import com.dbxtune.sql.conn.DbxConnection;
import com.dbxtune.utils.Configuration;
import com.dbxtune.utils.DbUtils;

public class SqlServerBackupHistory
extends SqlServerAbstract
{
//	private static final Logger _logger = LogManager.getLogger(MethodHandles.lookup().lookupClass());

	private ResultSetTableModel _summaryRstm;
	private ResultSetTableModel _detailsRstm;


	public SqlServerBackupHistory(DailySummaryReportAbstract reportingInstance)
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
		return false;
	}

//	@Override
//	public void writeShortMessageText(Writer w)
//	throws IOException
//	{
//	}

	@Override
	public void writeMessageText(Writer sb, MessageType messageType)
	throws IOException
	{
		boolean isEnabled = Configuration.getCombinedConfiguration().getBooleanProperty(CounterControllerSqlServer.PROPKEY_onPcsDatabaseRollover_captureBackupHistory           , CounterControllerSqlServer.DEFAULT_onPcsDatabaseRollover_captureBackupHistory);
		int     days      = Configuration.getCombinedConfiguration().getIntProperty(    CounterControllerSqlServer.PROPKEY_onPcsDatabaseRollover_captureBackupHistory_daysToCopy, CounterControllerSqlServer.DEFAULT_onPcsDatabaseRollover_captureBackupHistory_daysToCopy);

		if (_detailsRstm.isEmpty())
		{
			if ( ! isEnabled )
			{
				sb.append("Backup History is DISABLED. Please change Property <code>" + CounterControllerSqlServer.PROPKEY_onPcsDatabaseRollover_captureBackupHistory + " = true</code>");
				return;
			}

			sb.append("Backup History was empty. This could be to several reasons:");
			sb.append("<ul>");
			sb.append("  <li>No backups has been made for the last " + days + " days. </li>");
			sb.append("  <li>The user extracting data did not have correct authorization.</li>");
			sb.append("  <li>You initiated a manual 'Create Daily Summary Report on Current Recording', from the Web UI, then the data is not extracted.</li>");
			sb.append("</ul>");
			sb.append("You can probably find the reason in the Collectors Errorlog, at 'midnight'...");
			return;
		}

		// Get a description of this section, and column names
//		sb.append(getSectionDescriptionHtml(_shortRstm, true));

		// Last sample Database Size info
//		sb.append("Row Count: " + _shortRstm.getRowCount() + "<br>\n");
//		sb.append("Row Count: " + _shortRstm.getRowCount() + "&emsp;&emsp; To change number of <i>top</i> records, set property <code>" + getTopRowsPropertyName() + "=##</code><br>\n");

		
		sb.append("<p>");
		sb.append("Below is a Summary of all <b>databases</b> backups grouped by date. (" + _summaryRstm.getRowCount() + " days)<br>");
		sb.append("Number of days extracted from the DBMS was " + days + ", and can be changed with property: <code>" + CounterControllerSqlServer.PROPKEY_onPcsDatabaseRollover_captureBackupHistory_daysToCopy + " = ##</code>");
		sb.append("</p>");
		sb.append(toHtmlTable(_summaryRstm));
		sb.append("<br>");
		
		sb.append("<p>");
		sb.append("Below are Backup Detailes for each database (both DB and TRAN backups).<br>");
		sb.append("Number of days extracted from the DBMS was " + days + ", and can be changed with property: <code>" + CounterControllerSqlServer.PROPKEY_onPcsDatabaseRollover_captureBackupHistory_daysToCopy + " = ##</code>");
		sb.append("</p>");
//		sb.append(toHtmlTable(_detailsRstm));
		sb.append(_detailsRstm.toHtmlTableFoldableString("sorttable", _detailesRstmRenderer));
		sb.append("<br>");
	}

	/**
	 * HTML Table Renderer for: _detailsRstm
	 */
	private TableStringRenderer _detailesRstmRenderer = new TableStringRenderer()
	{
		@Override
		public String tagTrAttr(ResultSetTableModel rstm, int row)
		{
//			String status = rstm.getValueAsString(row, "status", true, "");
//			if ( ! status.toUpperCase().contains("SUCCESS") )
//			{
//				return "style='color:red;'";
//			}
			
			String backupType = rstm.getValueAsString(row, "backup_type", true, "");
			if ("DATABASE".equals(backupType))
			{
				// If NEXT row is NOT the same type... Mark the row in some way
				String nextRowBackupType = rstm.getRowCount() > row+1 ? rstm.getValueAsString(row + 1, "backup_type", true, "") : backupType;
				if ( ! backupType.equals(nextRowBackupType) )
				{
					return "style='color:DodgerBlue;'";
				}
			}
			return TableStringRenderer.super.tagTrAttr(rstm, row);
		}
	};

	@Override
	public String getSubject()
	{
		return "Database Backup History (origin: msdb.dbo.backupset)";
	}

	@Override
	public boolean hasIssueToReport()
	{
		return false; // even if we found entries, do NOT indicate this as a Problem or Issue
	}


	@Override
	public String[] getMandatoryTables()
	{
		return null;
//		return new String[] { "FIXME" };
	}

	@Override
	public void create(DbxConnection conn, String srvName, Configuration pcsSavedConf, Configuration localConf)
	{
		String sql;
		String schemaName = SqlServerBackupHistoryExtractor.PCS_SCHEMA_NAME; 
		String tableName;
		String top = "top " + getTopRows();
		top = "";
		
		// Check if the 'dump_history' table exist in the recording
		if ( ! DbUtils.checkIfTableExistsNoThrow(conn, null, schemaName, SqlServerBackupHistoryExtractor.backup_details.class.getSimpleName()) )
		{
			_summaryRstm = ResultSetTableModel.createEmpty("BackupHistorySummary");
			_detailsRstm = ResultSetTableModel.createEmpty("BackupHistoryDetails");
			return;
		}
		
		// Summary -- This can PROBABLY be created based on values in 'backup_details'... See how I do this is AseBackupHistory.create()
		tableName  = SqlServerBackupHistoryExtractor.backup_summary.class.getSimpleName(); 
		sql = "SELECT " + top + " * from [" + schemaName + "].[" + tableName + "]";
		_summaryRstm = executeQuery(conn, sql, true, "BackupHistorySummary");

		
		// Details
		tableName  = SqlServerBackupHistoryExtractor.backup_details.class.getSimpleName(); 
		sql = "SELECT " + top + " * from [" + schemaName + "].[" + tableName + "]";
		_detailsRstm   = executeQuery(conn, sql, true, "BackupHistoryDetails");

//		// Highlight sort column
//		_shortRstm.setHighlightSortColumns("Impact");
//
//		// Remove some columns which we don't really need
//		_shortRstm.removeColumnNoCase("SessionStartTime");
//		_shortRstm.removeColumnNoCase("SessionSampleTime");
////		_shortRstm.removeColumnNoCase("CmSampleTime");
//		_shortRstm.removeColumnNoCase("CmSampleMs");
//		_shortRstm.removeColumnNoCase("CmNewDiffRateRow");  // This was changed into "CmRowState"
//		_shortRstm.removeColumnNoCase("CmRowState");
//		
//		// Describe the table
//		setSectionDescription(_shortRstm);
	}

//	/**
//	 * Set descriptions for the table, and the columns
//	 */
//	private void setSectionDescription(ResultSetTableModel rstm)
//	{
//		if (rstm == null)
//			return;
//		
//		// Section description
//		rstm.setDescription(
//				"Information from last collector sample from the table <code>CmIndexMissing_abs</code><br>" +
//				"");
//	}
}

