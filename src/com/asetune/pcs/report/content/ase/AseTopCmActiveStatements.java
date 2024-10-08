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
package com.asetune.pcs.report.content.ase;

import java.io.IOException;
import java.io.Writer;
import java.sql.SQLException;
import java.sql.Types;
import java.util.ArrayList;
import java.util.List;

import org.apache.log4j.Logger;
import org.h2.tools.SimpleResultSet;

import com.asetune.gui.ResultSetTableModel;
import com.asetune.gui.ResultSetTableModel.TableStringRenderer;
import com.asetune.pcs.DictCompression;
import com.asetune.pcs.report.DailySummaryReportAbstract;
import com.asetune.sql.conn.DbxConnection;
import com.asetune.utils.Configuration;
import com.asetune.utils.StringUtil;

public class AseTopCmActiveStatements extends AseAbstract
{
	private static Logger _logger = Logger.getLogger(AseTopCmActiveStatements.class);

	private ResultSetTableModel _shortRstm;
	private ResultSetTableModel _sqTextRstm;
	private ResultSetTableModel _showplanRstm;

	private List<String>        _messages = new ArrayList<>();

	public AseTopCmActiveStatements(DailySummaryReportAbstract reportingInstance)
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
		if (_messages.size() > 0)
		{
			sb.append("<b>Messages:</b> \n");
			sb.append("<ul> \n");
			for (String msg : _messages)
				sb.append("  <li>").append(msg).append("</li> \n");
			sb.append("</ul> \n");
		}

		if (_shortRstm.getRowCount() == 0)
		{
			sb.append("No rows found <br>\n");
		}
		else
		{
			// Get a description of this section, and column names
			sb.append(getSectionDescriptionHtml(_shortRstm, true));

			// Create a default renderer
			TableStringRenderer tableRender = new ReportEntryTableStringRenderer()
			{
				@Override
				public String cellValue(ResultSetTableModel rstm, int row, int col, String colName, Object objVal, String strVal)
				{
					if ("MonSqlText_max".equals(colName))
					{
						strVal = strVal.replace("<html><pre>",   "");
						strVal = strVal.replace("</pre></html>", "");

						return "<xmp>" + strVal + "</xmp>";
					}
					if ("ShowPlanText_max".equals(colName))
					{
						strVal = strVal.replace("<html><pre>",   "");
						strVal = strVal.replace("</pre></html>", "");

						return "<xmp>" + strVal + "</xmp>";
					}
					return strVal;
				}
			};
			sb.append("Row Count: " + _shortRstm.getRowCount() + "&emsp;&emsp; To change number of <i>top</i> records, set property <code>" + getTopRowsPropertyName() + "=##</code><br>\n");
			sb.append(_shortRstm.toHtmlTableString("sortable", true, true, null, tableRender));


			if (_sqTextRstm != null)
			{
				String  divId       = "ActiveSqlText";
				boolean showAtStart = false;
				String  htmlContent = _sqTextRstm.toHtmlTableString("sortable");
				
				String showHideDiv = createShowHideDiv(divId, showAtStart, "SQL Text by 'dbname, ProcNameOrSqlText, linenum', Row Count: " + _sqTextRstm.getRowCount() + " (This is the same SQL Text as the in the above table, but without all counter details)", htmlContent);

				// Compose special condition for Microsoft Outlook
				sb.append(msOutlookAlternateText(showHideDiv, "ActiveSqlText", null));
			}

			if (_showplanRstm != null)
			{
				String  divId       = "ActiveSqlShowplan";
				boolean showAtStart = false;
				String  htmlContent = _showplanRstm.toHtmlTableString("sortable");
				
				String showHideDiv = createShowHideDiv(divId, showAtStart, "Showplan (sp_showplan) by 'dbname, ProcNameOrSqlText, linenum', Row Count: " + _showplanRstm.getRowCount() + " (This is the same SHOWPLAN as the in the above table, but without all counter details)", htmlContent);

				// Compose special condition for Microsoft Outlook
				sb.append(msOutlookAlternateText(showHideDiv, "ActiveSqlShowplan", null));
			}
		}
	}

	@Override
	public String getSubject()
	{
		return "Top [sampled] ACTIVE SQL Statements (order by: CpuTime_max, origin: CmActiveStatements_diff / monProcessStatement, monProcess)";
	}

	@Override
	public boolean hasIssueToReport()
	{
		return false; // even if we found entries, do NOT indicate this as a Problem or Issue
	}


	@Override
	public String[] getMandatoryTables()
	{
		return new String[] { "CmActiveStatements_diff" };
	}

	@Override
	public void create(DbxConnection conn, String srvName, Configuration pcsSavedConf, Configuration localConf)
	{
//		int topRows = localConf.getIntProperty(this.getClass().getSimpleName()+".top", 20);
		int topRows = getTopRows();
		int havingAbove = 1000;

		String skipDumpDbAndTran    = "  and [Command] not like 'DUMP %' \n";
		String skipUpdateStatistics = "  and [Command] not like 'UPDATE STATISTICS%' \n";

		_messages.add("Skipping Command's with 'DUMP %' ");
		_messages.add("Skipping Command's with 'UPDATE STATISTICS%' ");
		
		boolean hasDccCols      = false; 
		String col_MonSqlText   = "MonSqlText";
		String col_ShowPlanText = "ShowPlanText";
		try
		{
    		if (DictCompression.hasCompressedColumnNames(conn, null, "CmActiveStatements_diff"))
    		{
    			hasDccCols       = true; 
    			col_MonSqlText   = "MonSqlText$dcc$";
    			col_ShowPlanText = "ShowPlanText$dcc$";
    		}
		}
		catch(SQLException ex)
		{
			_logger.warn("Problems checking for Dictionary Compressed Columns in table 'CmActiveStatements_diff'.");
		}

		 // just to get Column names
		String dummySql = "select * from [CmActiveStatements_diff] where 1 = 2";
		ResultSetTableModel dummyRstm = executeQuery(conn, dummySql, true, "metadata");

		// Create Column selects, but only if the column exists in the PCS Table
		String HostName_max           = !dummyRstm.hasColumnNoCase("HostName"          ) ? "" : "    ,max([HostName])                                                   as [HostName_max] \n";

		
		String sql = getCmDiffColumnsAsSqlComment("CmActiveStatements")
			    + "select top " + topRows + " \n"
			    + "     [dbname]                                                          as [dbname] \n"
			    + "    , CASE WHEN [procname] != '' THEN [procname] ELSE [" + col_MonSqlText + "] END as [ProcNameOrSqlText] \n"
			    + "    ,[linenum]                                                         as [linenum] \n"
			    + "    ,count(*)                                                          as [samples_count] \n"
			    + "    ,sum(CASE WHEN [multiSampled] = 'YES' THEN 1 ELSE 0 END)           as [multiSampled_count] \n"
			    
			    + "    ,max([Command])                                                    as [Command_max] \n"
			    + "    ,max([Application])                                                as [Application_max] \n"
			    + HostName_max
			    
			    + "    ,max([ExecTimeInMs])                                               as [ExecTimeInMs_max] \n"
			    + "    ,max([UsefullExecTime])                                            as [UsefullExecTime_max] \n"
			    + "    ,max([CpuTime])                                                    as [CpuTime_max] \n"
			    + "    ,max([WaitTime])                                                   as [WaitTime_max] \n"
			    + "    ,sum([PhysicalReads])                                              as [PhysicalReads_sum] \n"
			    + "    ,sum([LogicalReads])                                               as [LogicalReads_sum] \n"
			    + "    ,sum([RowsAffectedDiff])                                           as [RowsAffectedDiff_sum] \n"
			    + "    ,sum([MemUsageKB])                                                 as [MemUsageKB_sum] \n"
			    + "    ,sum([pssinfo_tempdb_pages])                                       as [pssinfo_tempdb_pages_sum] \n"
			    + "    ,sum([PagesModified])                                              as [PagesModified_sum] \n"
			    + "    ,sum([PacketsSent])                                                as [PacketsSent_sum] \n"
			    + "    ,sum([PacketsReceived])                                            as [PacketsReceived_sum] \n"
			    + "    ,max([NetworkPacketSize])                                          as [NetworkPacketSize_max] \n"
			    + "    ,sum(CASE WHEN [BlockingOtherSpids] != '' THEN 1 ELSE 0 END)       as [BlockingOtherSpids_count] \n"
			    + "    ,max([SecondsWaiting])                                             as [SecondsWaiting_max] \n"
//			    + "    ,max([MonSqlText])                                                 as [MonSqlText_max] \n"
//			    + "    ,max([ShowPlanText])                                               as [ShowPlanText_max] \n"
			    + "    ,max([" + col_MonSqlText + "])                                         as [MonSqlText_max] \n"
			    + "    ,max([" + col_ShowPlanText + "])                                       as [ShowPlanText_max] \n"
			    
			    + "    ,min([CmSampleTime])                                               as [CmSampleTime_min] \n"
			    + "    ,max([CmSampleTime])                                               as [CmSampleTime_max] \n"
			    + "    ,cast('' as varchar(30))                                           as [Duration] \n"
			    
			    + "from [CmActiveStatements_diff] x \n"
			    + "where 1=1 \n"
			    + skipDumpDbAndTran
			    + skipUpdateStatistics
				+ getReportPeriodSqlWhere()
			    + "  and [monSource] = 'ACTIVE' \n"
			    + "group by [dbname], CASE WHEN [procname] != '' THEN [procname] ELSE [" + col_MonSqlText + "] END, [linenum] \n"
			    + "having [CpuTime_max] > " + havingAbove + "\n"
			    + "order by [CpuTime_max] desc \n"
			    + "";

		_shortRstm = executeQuery(conn, sql, false, "TopActiveStatements");
		if (_shortRstm == null)
		{
			_shortRstm = ResultSetTableModel.createEmpty("TopActiveStatements");
			return;
		}
		else
		{
			// Highlight sort column
			_shortRstm.setHighlightSortColumns("CpuTime_max");

			// Describe the table
			setSectionDescription(_shortRstm);

			// Calculate Duration
			setDurationColumn(_shortRstm, "CmSampleTime_min", "CmSampleTime_max", "Duration");
			
			// get Dictionary Compressed values for column: MonSqlText, ShowPlanText  -- store them in MonSqlText_max, ShowPlanText_max
			if (hasDccCols)
			{
				updateDictionaryCompressedColumn(_shortRstm, conn, null, "CmActiveStatements", "MonSqlText",   "MonSqlText_max");
				updateDictionaryCompressedColumn(_shortRstm, conn, null, "CmActiveStatements", "ShowPlanText", "ShowPlanText_max");
			}
			
			SimpleResultSet srs_sqlText = new SimpleResultSet();
			srs_sqlText.addColumn("dbname",            Types.VARCHAR,       30, 0);
			srs_sqlText.addColumn("ProcNameOrSqlText", Types.VARCHAR,      512, 0);
			srs_sqlText.addColumn("linenum",           Types.INTEGER,        0, 0);
			srs_sqlText.addColumn("MonSqlText",        Types.VARCHAR, 1024*128, 0); // this is 'text' in the origin table
			
			SimpleResultSet srs_showplan = new SimpleResultSet();
			srs_showplan.addColumn("dbname",            Types.VARCHAR,       30, 0);
			srs_showplan.addColumn("ProcNameOrSqlText", Types.VARCHAR,      512, 0);
			srs_showplan.addColumn("linenum",           Types.INTEGER,        0, 0);
			srs_showplan.addColumn("ShowPlanText",      Types.VARCHAR, 1024*128, 0); // this is 'text' in the origin table

		
			int pos_dbname            = _shortRstm.findColumnNoCase("dbname");
			int pos_ProcNameOrSqlText = _shortRstm.findColumnNoCase("ProcNameOrSqlText");
			int pos_linenum           = _shortRstm.findColumnNoCase("linenum");
			int pos_MonSqlText        = _shortRstm.findColumnNoCase("MonSqlText_max");
			int pos_ShowPlanText      = _shortRstm.findColumnNoCase("ShowPlanText_max");

			if (pos_dbname >= 0 && pos_ProcNameOrSqlText >= 0 && pos_linenum >= 0 && pos_MonSqlText >= 0 && pos_ShowPlanText >= 0)
			{
				for (int r=0; r<_shortRstm.getRowCount(); r++)
				{
					String  dbname            = _shortRstm.getValueAsString (r, pos_dbname);
					String  ProcNameOrSqlText = _shortRstm.getValueAsString (r, pos_ProcNameOrSqlText);
					Integer linenum           = _shortRstm.getValueAsInteger(r, pos_linenum);
					String  MonSqlText        = _shortRstm.getValueAsString (r, pos_MonSqlText);
					String  ShowPlanText      = _shortRstm.getValueAsString (r, pos_ShowPlanText);

					if (StringUtil.countLines(MonSqlText) > 5)
						_shortRstm.setValueAtWithOverride("above 5 rows... see table below", r, pos_MonSqlText);
					
					if (StringUtil.countLines(ShowPlanText) > 5)
						_shortRstm.setValueAtWithOverride("above 5 rows... see table below", r, pos_ShowPlanText);
					
					// Remove starting/ending '<html><pre>'
					if (MonSqlText != null && MonSqlText.startsWith("<html><pre>") && MonSqlText.endsWith("</pre></html>"))
					{
						MonSqlText = MonSqlText.substring("<html><pre>".length());
						MonSqlText = MonSqlText.substring(0, MonSqlText.length() - "</pre></html>".length());
					}
					
					// Remove starting/ending '<html><pre>'
					if (ShowPlanText != null && ShowPlanText.startsWith("<html>Showplan:<pre>") && ShowPlanText.endsWith("</pre></html>"))
					{
						ShowPlanText = ShowPlanText.substring("<html>Showplan:<pre>".length());
						ShowPlanText = ShowPlanText.substring(0, ShowPlanText.length() - "</pre></html>".length());
					}

					srs_sqlText .addRow(dbname, ProcNameOrSqlText, linenum, "<xmp>" + MonSqlText + "</xmp>");
					srs_showplan.addRow(dbname, ProcNameOrSqlText, linenum, "<xmp>" + ShowPlanText + "</xmp>");
				}
			}
			
			
			// GET SQLTEXT (only)
			try
			{
				// Note the 'srs' is populated when reading above ResultSet from query
				_sqTextRstm = createResultSetTableModel(srs_sqlText, "Top SQL TEXT", null);
				srs_sqlText.close();
			}
			catch (SQLException ex)
			{
				setProblemException(ex);
	
				_sqTextRstm = ResultSetTableModel.createEmpty("Top SQL TEXT");
				_logger.warn("Problems getting Top SQL TEXT: " + ex);
			}
			
			// GET SHOWPLAN (only)
			try
			{
				// Note the 'srs' is populated when reading above ResultSet from query
				_showplanRstm = createResultSetTableModel(srs_showplan, "Top SHOWPLAN TEXT", null);
				srs_showplan.close();
			}
			catch (SQLException ex)
			{
				setProblemException(ex);
	
				_showplanRstm = ResultSetTableModel.createEmpty("Top SHOWPLAN TEXT");
				_logger.warn("Problems getting Top SHOWPLAN TEXT: " + ex);
			}
		}
	}
	
	/**
	 * Set descriptions for the table, and the columns
	 */
	private void setSectionDescription(ResultSetTableModel rstm)
	{
		if (rstm == null)
			return;
		
		// Section description
		rstm.setDescription(
				"SQL Statements that was active/running while AseTune sampled data. <br>" +
				"Ordered by <code>CpuTime_max</code> This includes LogicalReads, Sorting etc...<br>" +
				"<br>" +
				"Since we <i>snapshot</i> or sample this every X second, this might not be a good representation! <br>" +
				"But if the other monTables do not catch or see the Executed SQL Statement, this <b>would</b> be a good/alternate place to see what's happening. <br>" +
				"But keep in mind it's only a <i>snapshot</i> data ever X second/minute...<br>" +
				"<br>" +
				"ASE Source table is 'master.dbo.monProcessStatement' and 'master.dbo.monProcess'. <br>" +
				"PCS Source table is 'CmActiveStatements_diff'. (PCS = Persistent Counter Store) <br>" +
				"<br>" +
				"The report <i>summarizes</i> (min/max/count/sum/avg) all entries/samples from the <i>source_DIFF</i> table. <br>" +
				"Typically the column name <i>postfix</i> will tell you what aggregate function was used. <br>" +
				"");

		// Columns description
		rstm.setColumnDescription("CmSampleTime_min"          , "First entry was sampled.");
		rstm.setColumnDescription("CmSampleTime_max"          , "Last entry was sampled.");
		rstm.setColumnDescription("Duration"                  , "Difference between first/last sample");
		rstm.setColumnDescription("dbname"                    , "Database name");
		rstm.setColumnDescription("ProcNameOrSqlText"         , "Procedure Name or '*ss' for StatementCache entries and '*sq' for DynamicPreparedSql otherwise the SqlText");
		rstm.setColumnDescription("linenum"                   , "If it's a Procedure Then it's procs line number");
		rstm.setColumnDescription("samples_count"             , "Number of entries this Statement is sampled in the period");
		rstm.setColumnDescription("multiSampled_count"        , "How many entries this Statment was sampled with the column 'multiSampled' set to 'YES'");
		
		rstm.setColumnDescription("Command_max"               , "Command that was issues (note this is MAX... so it might be more states...)");
		rstm.setColumnDescription("Application_max"           , "Application Name (note this is MAX... so it might be more names...)");
		rstm.setColumnDescription("HostName_max"              , "HsotName (note this is MAX... so it might be more names...)");

		rstm.setColumnDescription("ExecTimeInMs_max"          , "Number of milliseconds we have spend in execution mode for this SQL Statement");
		rstm.setColumnDescription("UsefullExecTime_max"       , "Like 'ExecTimeInMs_max', but typically subtracts 'WaitTime_max'.");
		rstm.setColumnDescription("CpuTime_max"               , "CpuTime for each sampled entry as a summary for the duration");
		rstm.setColumnDescription("WaitTime_max"              , "WaitTime for each sampled entry as a summary for the duration");
		rstm.setColumnDescription("PhysicalReads_sum"         , "PhysicalReads for each sampled entry as a summary for the duration");
		rstm.setColumnDescription("LogicalReads_sum"          , "LogicalReads for each sampled entry as a summary for the duration");
		rstm.setColumnDescription("MemUsageKB_sum"            , "MemUsageKB for each sampled entry as a summary for the duration");
		rstm.setColumnDescription("pssinfo_tempdb_pages_sum"  , "pssinfo_tempdb_pages for each sampled entry as a summary for the duration");
		rstm.setColumnDescription("PagesModified_sum"         , "PagesModified for each sampled entry as a summary for the duration");
		rstm.setColumnDescription("PacketsSent_sum"           , "PacketsSent for each sampled entry as a summary for the duration");
		rstm.setColumnDescription("PacketsReceived_sum"       , "PacketsReceived for each sampled entry as a summary for the duration");
		rstm.setColumnDescription("NetworkPacketSize_max"     , "NetworkPacketSize used by this connection");
		rstm.setColumnDescription("BlockingOtherSpids_count"  , "Number of times in the duration this statement has <b>blocked others</b> from working.");
		rstm.setColumnDescription("SecondsWaiting_max"        , "Number of seconds we have been waiting for other's to do there work.");
		rstm.setColumnDescription("MonSqlText_max"            , "The SQL Text (if available)");
		rstm.setColumnDescription("ShowPlanText_max"          , "sp_showplan output (if available)");
	}
}
