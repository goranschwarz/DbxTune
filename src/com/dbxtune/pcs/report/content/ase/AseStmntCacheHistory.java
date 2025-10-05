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
package com.dbxtune.pcs.report.content.ase;

import java.io.IOException;
import java.io.Writer;
import java.lang.invoke.MethodHandles;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.text.NumberFormat;
import java.util.LinkedHashMap;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.dbxtune.gui.ResultSetTableModel;
import com.dbxtune.pcs.report.DailySummaryReportAbstract;
import com.dbxtune.sql.conn.DbxConnection;
import com.dbxtune.utils.Configuration;
import com.dbxtune.utils.HtmlTableProducer;

public class AseStmntCacheHistory extends AseAbstract
{
	private static final Logger _logger = LogManager.getLogger(MethodHandles.lookup().lookupClass());

	public static final String PROPKEY_top = "DailySummaryReport.AseStmntCacheHistory.top.rows";
	public static final int    DEFAULT_top = 50;

	private ResultSetTableModel _shortRstm;
	private ResultSetTableModel _stmntCacheSummary;
	private int _stmntCacheDayCount = -1;

	public AseStmntCacheHistory(DailySummaryReportAbstract reportingInstance)
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
//		return true;
		return false;
	}

	@Override
	public void writeMessageText(Writer sb, MessageType messageType)
	throws IOException
	{
		if (_shortRstm.getRowCount() == 0)
		{
			sb.append("No rows found <br>\n");
		}
		else
		{
			// Get a description of this section, and column names
			sb.append(getSectionDescriptionHtml(_shortRstm, true));

//			sb.append("<b>Total number of days in the Statement Cache:</b> " + _stmntCacheDayCount + " <br>\n");
//			sb.append("<br>\n");
			
			if (_stmntCacheSummary != null && !_stmntCacheSummary.isEmpty())
			{
				LinkedHashMap<String, String> summaryTable = new LinkedHashMap<>();
				NumberFormat nf = NumberFormat.getInstance();
				
				summaryTable.put("Total Days in Stmnt Cache", nf.format(_stmntCacheDayCount));

				summaryTable.put("stmnt_count"              , nf.format(_stmntCacheSummary.getValueAsLong(0, "stmnt_count", true, -1L)));
				summaryTable.put("exec_count"               , nf.format(_stmntCacheSummary.getValueAsLong(0, "exec_count", true, -1L)));
				summaryTable.put("PlanSizeKB"               , nf.format(_stmntCacheSummary.getValueAsLong(0, "PlanSizeKB", true, -1L)));

				summaryTable.put("ObjType_Dynamic_sq"       , nf.format(_stmntCacheSummary.getValueAsDouble(0, "ObjType_Dynamic_sq"                 , true, -1d)));
				summaryTable.put("ObjType_Language_ss"      , nf.format(_stmntCacheSummary.getValueAsDouble(0, "ObjType_Language_ss"             , true, -1d)));
				
				summaryTable.put("total_logical_reads"      , nf.format(_stmntCacheSummary.getValueAsLong(0, "total_logical_reads" , true, -1L)));
				summaryTable.put("total_physical_reads"     , nf.format(_stmntCacheSummary.getValueAsLong(0, "total_physical_reads", true, -1L)));
				summaryTable.put("total_sort_count"         , nf.format(_stmntCacheSummary.getValueAsLong(0, "total_sort_count", true, -1L)));
				summaryTable.put("total_sort_time"          , nf.format(_stmntCacheSummary.getValueAsLong(0, "total_sort_time", true, -1L)));
				summaryTable.put("avg_qual_read_rows"       , nf.format(_stmntCacheSummary.getValueAsLong(0, "avg_qual_read_rows")));
				summaryTable.put("avg_qual_write_rows"      , nf.format(_stmntCacheSummary.getValueAsLong(0, "avg_qual_write_rows")));

				sb.append("<b>Summary for the full available history (for what's in the Statement Cache).</b> <br>\n");
				sb.append(HtmlTableProducer.createHtmlTable(summaryTable, "", true));
				sb.append("<br>\n");
			}

			// Remove all rows except the first # rows 
			int topRows = Configuration.getCombinedConfiguration().getIntProperty(PROPKEY_top, DEFAULT_top);
			_shortRstm.setRowCount(topRows);
			
			sb.append("Showing only " + _shortRstm.getRowCount() + " first rows. To change this number, set configuration <code>" + PROPKEY_top + " = ###</code><br>\n");
			sb.append(toHtmlTable(_shortRstm));
		}
	}
	
	@Override
	public String getSubject()
	{
		return "Statement Cache History";
	}

	@Override
	public boolean hasIssueToReport()
	{
		return false; // even if we found entries, do NOT indicate this as a Problem or Issue
	}


	@Override
	public String[] getMandatoryTables()
	{
		return new String[] { "CmStmntCacheHistory_abs" };
	}

	@Override
	public void create(DbxConnection conn, String srvName, Configuration pcsSavedConf, Configuration localConf)
	{
		// Get 'statement cache size' from saved configuration 
		String sql = "" 
			+ "select  \n"
			+ "       [creation_date] \n"
			+ "      ,[creation_hour] \n"
			+ "      ,max([stmnt_count])                         as [stmnt_count] \n"
			
//			+ "      ,'------>>>>>------'                       as [details-to-right] \n"
			+ "      ,'&emsp;&emsp;>>>>>&emsp;&emsp;'           as [details-to-right] \n"

			+ "      ,sum([exec_count_diff])                    as [exec_count] \n"
			+ "      ,max([PlanSizeKB])                         as [PlanSizeKB] \n"
			+ "      ,max([ObjType_Dynamic_sq])                 as [ObjType_Dynamic_sq] \n"
			+ "      ,max([ObjType_Language_ss])                as [ObjType_Language_ss] \n"
			
			+ "      ,sum([total_elapsed_time_ms])              as [total_elapsed_time_ms] \n"
			+ "      ,sum([total_cpu_time_ms])                  as [total_elapsed_time_ms] \n"
			+ "      ,sum([total_est_wait_time_ms])             as [total_elapsed_time_ms] \n"
			+ "      ,sum([total_logical_reads])                as [total_logical_reads] \n"
			+ "      ,sum([total_physical_reads])               as [total_physical_reads] \n"
			+ "      ,sum([total_sort_count])                   as [total_sort_count] \n"
			+ "      ,sum([total_sort_time])                    as [total_sort_time] \n"
			+ "      ,sum([avg_qual_read_rows])                 as [avg_qual_read_rows] \n"
			+ "      ,sum([avg_qual_write_rows])                as [avg_qual_write_rows] \n"
			+ "from [CmStmntCacheHistory_diff] \n" 
			+ "where [creation_date] < '9999-12-31' \n" // do not include the SUMMARY record
			+ "group by [creation_date], [creation_hour] \n"
			+ "order by [creation_date] desc, [creation_hour] desc";

		_shortRstm = executeQuery(conn, sql, false, "StmntCacheHistory");

		if (_shortRstm == null)
		{
			_shortRstm = ResultSetTableModel.createEmpty("StmntCacheHistory");
			return;
		}
		else
		{
			// Get TOTAL days
			sql = conn.quotifySqlString("select count(distinct [creation_date]) as [days_count] from [CmStmntCacheHistory_abs] where [creation_date] < '9999-12-31'");
			try (Statement stmnt = conn.createStatement(); ResultSet rs = stmnt.executeQuery(sql))
			{
				while(rs.next())
					_stmntCacheDayCount = rs.getInt(1);
			}
			catch(SQLException ex)
			{
				_logger.warn("Problems when getting Number of DYA in the Plan Cache using SQL=|" + sql + "|.", ex);
			}

			// Get TOTAL SUMMARY of things (total sum is represented as day '9999-12-31')
			sql = "select * from [CmStmntCacheHistory_abs] where [creation_date] = '9999-12-31'";
			_stmntCacheSummary = ResultSetTableModel.executeQuery(conn, sql, false, "");
			
			// describe things
			setSectionDescription(_shortRstm);
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
				"How much Statement History does ASE have available? <br>" +
				"Looking at the top ## statements in the cache doesn't do me much good if: \n" +
				"<ul> \n" +
				"    <li>Someone restarted the server recently</li> \n" +
				"    <li>Someone ran DBCC purgesqlcache</li> \n" +
				"    <li>Somebody's addicted to rebuilding indexes and updating stats (which invalidates statements for all affected objects)</li> \n" +
//				"    <li>The server's under extreme memory pressure</li> \n" +
				"    <li>The developers <i>aren't parameterizing their queries</i>... bloating/trashing the cache.</li> \n" +
//				"    <li>The app has an <i>old version of NHibernate with the parameterization bug</i></li> \n" +
//				"    <li>The .NET app calls <i>Parameters.Add without setting the parameter size</i></li> \n" +
//				"    <li>The Java app gets <code>CONVERT_IMPLICIT</code> when call <i><code>stmnt.setString()</code> on columns that has VARCHAR... Workaround: set connection parameter <code>sendStringParametersAsUnicode=false</code></i></li> \n" +
				"</ul> \n" +
				"<br>" +
				"The <code>stmnt_count</code> should not vary to much, and we should have a resonable number of days in the history. <br>" +
				"<br>" +
				"SQL Source table is 'monStatementCache'. <br>" +
				"PCS Source table is 'CmStmntCacheHistory_diff'. (PCS = Persistent Counter Store) <br>" +
				"");

		// Columns description
		rstm.setColumnDescription("creation_date"       , "Date that 'stmnt_count' is for.");
		rstm.setColumnDescription("creation_hour"       , "If it's 0 or above it means hour of the day, -1 means: full day.");
		rstm.setColumnDescription("stmnt_count"         , "How many statements was created during this time frame (creation_date & creation_hour)");
		rstm.setColumnDescription("exec_count"          , "How many Executions has been done using statements created in this time frame (creation_date & creation_hour)");
	}

}
