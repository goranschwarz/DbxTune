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
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Types;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.apache.log4j.Logger;
import org.h2.tools.SimpleResultSet;

import com.asetune.cache.XmlPlanAseUtils;
import com.asetune.gui.ResultSetTableModel;
import com.asetune.gui.ResultSetTableModel.TableStringRenderer;
import com.asetune.pcs.report.DailySummaryReportAbstract;
import com.asetune.pcs.report.content.SparklineHelper;
import com.asetune.pcs.report.content.SparklineHelper.AggType;
import com.asetune.pcs.report.content.SparklineHelper.DataSource;
import com.asetune.pcs.report.content.SparklineHelper.SparkLineParams;
import com.asetune.sql.SqlParserUtils;
import com.asetune.sql.conn.DbxConnection;
import com.asetune.utils.Configuration;
import com.asetune.utils.HtmlTableProducer;
import com.asetune.utils.HtmlTableProducer.ColumnCopyDef;
import com.asetune.utils.HtmlTableProducer.ColumnCopyRender;
import com.asetune.utils.HtmlTableProducer.ColumnCopyRow;
import com.asetune.utils.HtmlTableProducer.ColumnStatic;
import com.asetune.utils.StringUtil;

import net.sf.jsqlparser.parser.ParseException;

public class AseTopCmStmntCacheDetails extends AseAbstract
{
	private static Logger _logger = Logger.getLogger(AseTopCmStmntCacheDetails.class);

	private ResultSetTableModel _shortRstm;
	private ResultSetTableModel _ssqlRstm;
//	private Exception           _problem = null;
	private List<String>        _miniChartJsList = new ArrayList<>();

	private Map<Map<String, Object>, SqlCapExecutedSqlEntries> _keyToExecutedSql;
	private Map<String, String> _planMap = new HashMap<>();

	private ReportType _reportType = ReportType.CPU_TIME;
	
	public enum ReportType
	{
		CPU_TIME, 
		WAIT_TIME
	};
	
	public AseTopCmStmntCacheDetails(DailySummaryReportAbstract reportingInstance, ReportType reportType)
	{
		super(reportingInstance);
		_reportType = reportType;
	}

	@Override
	public boolean hasShortMessageText()
	{
		if (ReportType.CPU_TIME.equals(_reportType))
			return true;

		return false;
	}

//	@Override
//	public void writeShortMessageText(Writer w)
//	throws IOException
//	{
//		writeMessageText(w);
//	}

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
			// Full information, including (top summary table)
			if (isFullMessageType())
			{
				// Get a description of this section, and column names
				sb.append(getSectionDescriptionHtml(_shortRstm, true));

//				sb.append("Row Count: " + _shortRstm.getRowCount() + "<br>\n");
				sb.append("Row Count: " + _shortRstm.getRowCount() + "&emsp;&emsp; To change number of <i>top</i> records, set property <code>" + getTopRowsPropertyName() + "=##</code><br>\n");
//				sb.append(toHtmlTable(_shortRstm));

				// Create a default renderer
				TableStringRenderer tableRender = new ReportEntryTableStringRenderer()
				{
					@Override
					public String cellValue(ResultSetTableModel rstm, int row, int col, String colName, Object objVal, String strVal)
					{
						if ("txt".equals(colName))
						{
							// Get Actual Executed SQL Text for current 'Hashkey'
							int Hashkey = rstm.getValueAsInteger(row, "Hashkey"); // Note: 'HashKey' (capital K) in [MonSqlCapStatements] but 'Hashkey' in RSTM
							
							Map<String, Object> whereColValMap = new LinkedHashMap<>();
							whereColValMap.put("HashKey"  , Hashkey); // Note: 'HashKey' (capital K) in [MonSqlCapStatements] but 'Hashkey' in RSTM

							String executedSqlText = getSqlCapExecutedSqlTextAsString(_keyToExecutedSql, whereColValMap);

							// Put the "Actual Executed SQL Text" as a "tooltip"
							return "<div title='Click for Detailes' "
									+ "data-toggle='modal' "
									+ "data-target='#dbx-view-sqltext-dialog' "
									+ "data-objectname='" + Hashkey + "' "
									+ "data-tooltip=\""   + executedSqlText     + "\" "
									+ ">&#x1F4AC;</div>"; // symbol popup with "..."
						}

						return strVal;
					}
				};

				sb.append(_shortRstm.toHtmlTableString("sortable", true, true, null, tableRender));
			}
			
			// The "sub table" with pivot info on most mini-charts/sparkline
			if (_ssqlRstm != null)
			{
//				sb.append("Statement Cache Entries Count: " + _ssqlRstm.getRowCount() + "<br>\n");
//				sb.append(toHtmlTable(_ssqlRstm));
				
				sb.append("<br>\n");
				sb.append("<details open> \n");
				sb.append("<summary>Details for above Statements, including SQL Text (click to collapse) </summary> \n");
				
				sb.append("<br>\n");
				sb.append("SQL Text by 'query_hash', Row Count: " + _ssqlRstm.getRowCount() + "\n");
				sb.append(toHtmlTable(_ssqlRstm));

				sb.append("\n");
				sb.append("</details> \n");
			}

			if (isFullMessageType())
			{
				sb.append("<script type='text/javascript'> \n");
				sb.append("    function showplanForId(id) \n");
				sb.append("    { \n");
				sb.append("        var showplanText = document.getElementById('plan_'+id).innerHTML \n");
//				sb.append("        QP.showPlan(document.getElementById('showplan-container'), showplanText); \n");
//				sb.append("        document.getElementById('showplan-head').innerHTML = 'Below is Execution plan for <code>plan_handle: ' + id + \"</code> <br>Note: You can also view your plan at <a href='http://www.supratimas.com' target='_blank'>http://www.supratimas.com</a>, or any other <i>plan-view</i> application by pasting (Ctrl-V) the clipboard content. <br>SentryOne Plan Explorer can be downloaded here: <a href='https://www.sentryone.com/plan-explorer' target='_blank'>https://www.sentryone.com/plan-explorer</a>\"; \n");
				sb.append("        copyStringToClipboard(showplanText); \n");
				sb.append("    } \n");
				sb.append("\n");
				sb.append("    function copyStringToClipboard (string)                                   \n");
				sb.append("    {                                                                         \n");
				sb.append("        function handler (event)                                              \n");
				sb.append("        {                                                                     \n");
				sb.append("            event.clipboardData.setData('text/plain', string);                \n");
				sb.append("            event.preventDefault();                                           \n");
				sb.append("            document.removeEventListener('copy', handler, true);              \n");
				sb.append("        }                                                                     \n");
				sb.append("                                                                              \n");
				sb.append("        document.addEventListener('copy', handler, true);                     \n");
				sb.append("        document.execCommand('copy');                                         \n");
				sb.append("                                                                              \n");
				sb.append("        // Open a popup... and close it 3 seconds later...                    \n");
				sb.append("        $('#copyPastePopup').modal('show');                                   \n");
				sb.append("            setTimeout(function() {                                           \n");
				sb.append("            $('#copyPastePopup').modal('hide');                               \n");
				sb.append("        }, 3000);		                                                     \n");
				sb.append("    }                                                                         \n");
				sb.append("</script> \n");


				// HTML Code for the bootstrap popup...
				sb.append("    <div class='modal fade' id='copyPastePopup'>                              \n");
				sb.append("        <div class='modal-dialog'>                                            \n");
				sb.append("            <div class='modal-content'>                                       \n");
				sb.append("                <div class='modal-header'>                                    \n");
//				sb.append("                    <button type='button' class='close' data-dismiss='modal' aria-hidden='true'>&times;</button> \n");
				sb.append("                    <h4 class='modal-title'>Auto Close in 3 seconds</h4>      \n");
				sb.append("                </div>                                                        \n");
				sb.append("                <div class='modal-body'>                                      \n");
				sb.append("                    <p>The XML Plan was copied to Clipboard</p>               \n");
				sb.append("                    <p>To see the GUI Plan, for example: Past it into SQL Window (sqlw)<br> \n");
				sb.append("                       SQL Window is included in the DbxTune package.         \n");
				sb.append("                    </p>                                                      \n");
				sb.append("                </div>                                                        \n");
				sb.append("            </div>                                                            \n");
				sb.append("        </div>                                                                \n");
				sb.append("    </div>                                                                    \n");

				for (String planHandle : _planMap.keySet())
				{
					String xmlPlan = _planMap.get(planHandle);

					// replace '*' with '_'
					planHandle = planHandle.replace('*', '_');

					sb.append("\n<script id='plan_").append(planHandle).append("' type='text/xmldata'>\n");
					sb.append(xmlPlan);
					sb.append("\n</script>\n");
				}
			}
		}

		// Write JavaScript code for CPU SparkLine
		if (isFullMessageType())
		{
			for (String str : _miniChartJsList)
				sb.append(str);
		}
	}

	@Override
	public String getSubject()
	{
		if (ReportType.WAIT_TIME.equals(_reportType))
			return "Top Statement Cache Entries by WAIT Time (order by: TotalSortTimeDiff_sum, origin: CmStmntCacheDetails / monCachedStatement)";

		return "Top Statement Cache Entries by CPU Time (order by: TotalCpuTimeDiff_sum, origin: CmStmntCacheDetails / monCachedStatement)";
	}

	@Override
	public boolean hasIssueToReport()
	{
		return false; // even if we found entries, do NOT indicate this as a Problem or Issue
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
				"Both slow and fast SQL Statements (from the Statement Cache) are presented here (ordered by: " + _reportType + ") <br>" +
				"<br>" +
				"This so you can see if there are problems with <i>Statement Cache Entries</i> that falls <i>just below</i> the threshold for 'Slow SQL Statements' <br>" +
				"<br>" +
				"ASE Source table is 'master.dbo.monCachedStatement' where StatementCache and DynamicSql are displayed. <br>" +
				"PCS Source table is 'CmStmntCacheDetails_diff'. (PCS = Persistent Counter Store) <br>" +
				"<br>" +
				"The report <i>summarizes</i> (min/max/count/sum/avg) all entries/samples from the <i>source_DIFF</i> table. <br>" +
				"Typically the column name <i>postfix</i> will tell you what aggregate function was used. <br>" +
				"");

		// Columns description
		rstm.setColumnDescription("DBName"                   , "Database name");
		rstm.setColumnDescription("ObjectName"               , "StatementCache Name... '*ss' for StatementCache entries and '*sq' for DynamicPreparedSql");
		rstm.setColumnDescription("samples_count"            , "Number of entries for this 'ObjectName' in the report period");
		rstm.setColumnDescription("SessionSampleTime_min"    , "First entry was sampled at this time");
		rstm.setColumnDescription("SessionSampleTime_max"    , "Last entry was sampled at this time");
		rstm.setColumnDescription("newDiffRow_sum"           , "Number of Diff Records that was seen for the first time.");
		rstm.setColumnDescription("CmSampleMs_sum"           , "Number of milliseconds this object has been available for sampling");

		rstm.setColumnDescription("UseCountDiff_sum"         , "How many times this object was Uased");

		rstm.setColumnDescription("TotalElapsedTimeDiff_sum" , "How many milliseconds did we spend in execution during the report period");
		rstm.setColumnDescription("TotalEstWaitTimeDiff_sum" , "How many milliseconds did we WAIT for execution during the report period (the reason for wait is harder to figure out on a Statement level)");
		rstm.setColumnDescription("TotalCpuTimeDiff_sum"     , "How much CPUTime did we use during the report period");
		rstm.setColumnDescription("TotalLioDiff_sum"         , "How many Logical I/O did we do during the report period");
		rstm.setColumnDescription("TotalPioDiff_sum"         , "How many Physical I/O did we do during the report period");
		
		rstm.setColumnDescription("AvgElapsedTime"           , "Average ElapsedTime per execution  (PostResolvedAs: TotalElapsedTimeDiff_sum / UseCountDiff_sum)");
		rstm.setColumnDescription("AvgCpuTime"               , "Average CpuTime per execution      (PostResolvedAs: TotalCpuTimeDiff_sum     / UseCountDiff_sum)");
		rstm.setColumnDescription("AvgLIO"                   , "Average Logical I/O per execution  (PostResolvedAs: TotalLioDiff_sum         / UseCountDiff_sum)");
		rstm.setColumnDescription("AvgPIO"                   , "Average Physical I/O per execution (PostResolvedAs: TotalPioDiff_sum         / UseCountDiff_sum)");
		                                                     
		rstm.setColumnDescription("LockWaitsDiff_sum"        , "How many times did this object Wait for a LOCK during the reporting period.");
		rstm.setColumnDescription("LockWaitTimeDiff_sum"     , "How many milliseconds did this object Wait for a LOCK during the reporting period.");
		
		rstm.setColumnDescription("MaxSortTime_max"          , "Max time we spend on <i>sorting</i> for this object during the reporting period.");
		rstm.setColumnDescription("SortSpilledCount_sum"     , "How many timed did a <i>sort</i> operation spill to tempdb (not done <i>in memory</i>)");
		rstm.setColumnDescription("SortCountDiff_sum"        , "How many times did this object perform sort operation during the reporting period.");
		rstm.setColumnDescription("TotalSortTimeDiff_sum"    , "Total time used for Sorting for this object during the reporting period. (if much, do we have an index to support the sort).");
	}

	@Override
	public String[] getMandatoryTables()
	{
		return new String[] {"CmStmntCacheDetails_diff"};
	}

	@Override
	public void create(DbxConnection conn, String srvName, Configuration pcsSavedConf, Configuration localConf)
	{
		int topRows = getTopRows();
//		int topRows = localConf.getIntProperty(this.getClass().getSimpleName()+".top", 20);

		// in some versions (ASE 16.0 SP2 I have observed this in) seems to (in some cases) keep the old counter values even if we compile a new PlanID
		// So DO NOT TRUST NEWLY created PlanID's 
		// Although this can create statistical problems:
		//   - if a procedure is *constantly* recompiled (due to "whatever" reason), those procedures will be discarded from below report
		boolean skipNewDiffRateRows    = localConf.getBooleanProperty(this.getClass().getSimpleName()+".skipNewDiffRateRows", false);
		boolean hasSkipNewDiffRateRows = localConf.hasProperty(this.getClass().getSimpleName()+".skipNewDiffRateRows");

		// try to figure out if we have *new* diff values that exceeds (using column 'ExecutionCount')
		if ( ! hasSkipNewDiffRateRows )
		{
			int useCountThreshold = 10000;
			String sql = ""
				    + "select count(*) \n"
				    + "from [CmStmntCacheDetails_diff] \n"
				    + "where [CmNewDiffRateRow] = 1 \n"
				    + "  and [UseCount] > " + useCountThreshold + " \n"
					+ getReportPeriodSqlWhere()
				    + "";
			sql = conn.quotifySqlString(sql);
			
			int foundCount = 0;
			try ( Statement stmnt = conn.createStatement(); ResultSet rs = stmnt.executeQuery(sql) )
			{
				while (rs.next())
					foundCount = rs.getInt(1);
			}
			catch(SQLException ex)
			{
				_logger.warn("Problems trying to identify if we have any newDiffRate records that seems 'strange'... executed SQL='" + sql + "'.");
			}
			
			if (foundCount > 0)
			{
				skipNewDiffRateRows = true;
				_logger.warn("Found " + foundCount + " records in 'CmStmntCacheDetails_diff' which had 'CmNewDiffRateRow=1' and 'UseCount>" + useCountThreshold + "'. This means I will EXCLUDE records with 'CmNewDiffRateRow=1'.");
				
				addWarningMessage("Found " + foundCount + " records in 'CmStmntCacheDetails_diff' which had 'CmNewDiffRateRow=1' and 'UseCount>" + useCountThreshold + "'. This means I will EXCLUDE records with 'CmNewDiffRateRow=1'.");
			}
		}
		if (skipNewDiffRateRows)
			addWarningMessage("Records with the flag 'CmNewDiffRateRow=1' will NOT be part of the Report. This means that the first time a StatementCache is executed (or recompiled and executed), the first execution counter will NOT be part of the statistics.");
		

		
		String dummySql = "select * from [CmStmntCacheDetails_diff] where 1 = 2"; // just to get Column names
		ResultSetTableModel dummyRstm = executeQuery(conn, dummySql, true, "metadata");

		
		// Get ASE Page Size
		int asePageSize = -1;
		try	{ asePageSize = getAsePageSizeFromMonDdlStorage(conn); }
		catch (SQLException ex) { }
		int asePageSizeDivider = 1024 * 1024 / asePageSize; // 2k->512, 4k->256, 8k=128, 16k=64

		
		// Create Column selects, but only if the column exists in the PCS Table
//		String col_UseCount__max              = !dummyRstm.hasColumnNoCase("UseCount"              ) ? "" : " ,max([UseCount])               as [UseCount__max] \n"; 
		String col_UseCount__sum              = !dummyRstm.hasColumnNoCase("UseCountDiff"          ) ? "" : " ,sum([UseCountDiff])           as [UseCount__sum] \n"; 

		String col_TotalElapsedTime__sum      = !dummyRstm.hasColumnNoCase("TotalElapsedTimeDiff"  ) ? "" : " ,sum([TotalElapsedTimeDiff])   as [TotalElapsedTime__sum] \n"; 
		String col_TotalCpuTime__sum          = !dummyRstm.hasColumnNoCase("TotalCpuTimeDiff"      ) ? "" : " ,sum([TotalCpuTimeDiff])       as [TotalCpuTime__sum] \n"; 
		String col_TotalEstWaitTime__sum      = !dummyRstm.hasColumnNoCase("TotalElapsedTimeDiff"  ) ? "" : " ,sum([TotalElapsedTimeDiff]) - sum([TotalCpuTimeDiff]) as [TotalEstWaitTime__sum] \n"; 
		String col_TotalLio__sum              = !dummyRstm.hasColumnNoCase("TotalLioDiff"          ) ? "" : " ,sum([TotalLioDiff])           as [TotalLio__sum] \n"; 
		String col_TotalLioMb__sum            = !dummyRstm.hasColumnNoCase("TotalLioDiff"          ) ? "" : " ,cast (sum([TotalLioDiff]) / "+asePageSizeDivider+" as bigint) as [TotalLioMb__sum] \n"; // bigint / val in h2 version 1.4.200, seems to return DECIMAL instead of bigint
		String col_TotalPio__sum              = !dummyRstm.hasColumnNoCase("TotalPioDiff"          ) ? "" : " ,sum([TotalPioDiff])           as [TotalPio__sum] \n"; 
		String col_LockWaits__sum             = !dummyRstm.hasColumnNoCase("LockWaitsDiff"         ) ? "" : " ,sum([LockWaitsDiff])          as [LockWaits__sum] \n"; 
		String col_LockWaitTime__sum          = !dummyRstm.hasColumnNoCase("LockWaitTimeDiff"      ) ? "" : " ,sum([LockWaitTimeDiff])       as [LockWaitTime__sum] \n"; 
		String col_SortCount__sum             = !dummyRstm.hasColumnNoCase("SortCountDiff"         ) ? "" : " ,sum([SortCountDiff])          as [SortCount__sum] \n"; 
		String col_SortSpilledCount__sum      = !dummyRstm.hasColumnNoCase("SortSpilledCount"      ) ? "" : " ,sum([SortSpilledCount])       as [SortSpilledCount__sum] \n"; 
		String col_TotalSortTime__sum         = !dummyRstm.hasColumnNoCase("TotalSortTimeDiff"     ) ? "" : " ,sum([TotalSortTimeDiff])      as [TotalSortTime__sum] \n"; 

		String col_AvgElapsedTime             = !dummyRstm.hasColumnNoCase("TotalElapsedTimeDiff"  ) ? "" : " ,sum([TotalElapsedTimeDiff])*1.0 / nullif(sum([UseCountDiff]), 0)  as [AvgElapsedTime] \n";
		String col_AvgCpuTime                 = !dummyRstm.hasColumnNoCase("TotalCpuTimeDiff"      ) ? "" : " ,sum([TotalCpuTimeDiff])    *1.0 / nullif(sum([UseCountDiff]), 0)  as [AvgCpuTime] \n";
		String col_AvgEstWaitTime             = !dummyRstm.hasColumnNoCase("TotalEstWaitTimeDiff"  ) ? "" : " ,sum([TotalEstWaitTimeDiff])*1.0 / nullif(sum([UseCountDiff]), 0)  as [AvgEstWaitTime] \n";
		String col_AvgLIO                     = !dummyRstm.hasColumnNoCase("TotalLioDiff"          ) ? "" : " ,sum([TotalLioDiff])        *1.0 / nullif(sum([UseCountDiff]), 0)  as [AvgLIO] \n";
		String col_AvgLioMb                   = !dummyRstm.hasColumnNoCase("TotalLioDiff"          ) ? "" : " ,sum([TotalLioDiff]) * 1.0 / "+asePageSizeDivider+" / nullif(sum([UseCountDiff]), 0)  as [AvgLioMb] \n";
		String col_AvgPIO                     = !dummyRstm.hasColumnNoCase("TotalPioDiff"          ) ? "" : " ,sum([TotalPioDiff])        *1.0 / nullif(sum([UseCountDiff]), 0)  as [AvgPIO] \n";
		String col_AvgLockWaits               = !dummyRstm.hasColumnNoCase("LockWaitsDiff"         ) ? "" : " ,sum([LockWaitsDiff])       *1.0 / nullif(sum([UseCountDiff]), 0)  as [AvgLockWaits] \n";
		String col_AvgLockWaitTime            = !dummyRstm.hasColumnNoCase("LockWaitTimeDiff"      ) ? "" : " ,sum([LockWaitTimeDiff])    *1.0 / nullif(sum([UseCountDiff]), 0)  as [AvgLockWaitTime] \n";
		String col_AvgSortCount               = !dummyRstm.hasColumnNoCase("SortCountDiff"         ) ? "" : " ,sum([SortCountDiff])       *1.0 / nullif(sum([UseCountDiff]), 0)  as [AvgSortCount] \n";
		String col_AvgSortSpilledCount        = !dummyRstm.hasColumnNoCase("SortSpilledCount"      ) ? "" : " ,sum([SortSpilledCount])    *1.0 / nullif(sum([UseCountDiff]), 0)  as [AvgSortSpilledCount] \n";
		String col_AvgSortTime                = !dummyRstm.hasColumnNoCase("TotalSortTimeDiff"     ) ? "" : " ,sum([TotalSortTimeDiff])   *1.0 / nullif(sum([UseCountDiff]), 0)  as [AvgSortTime] \n";
		
		String col_MaxSortTime__max           = !dummyRstm.hasColumnNoCase("MaxSortTime"           ) ? "" : " ,max([MaxSortTime])            as [MaxSortTime_max] \n"; 

		String col_MaxScanRows                = !dummyRstm.hasColumnNoCase("MaxScanRows"           ) ? "" : " ,max([MaxScanRows])            as [MaxScanRows] \n";
		String col_AvgScanRows                = !dummyRstm.hasColumnNoCase("AvgScanRows"           ) ? "" : " ,avg([AvgScanRows])            as [AvgScanRows] \n";
		String col_MaxQualifyingReadRows      = !dummyRstm.hasColumnNoCase("MaxQualifyingReadRows" ) ? "" : " ,max([MaxQualifyingReadRows])  as [MaxQualifyingReadRows] \n";
		String col_AvgQualifyingReadRows      = !dummyRstm.hasColumnNoCase("AvgQualifyingReadRows" ) ? "" : " ,avg([AvgQualifyingReadRows])  as [AvgQualifyingReadRows] \n";
		String col_MaxQualifyingWriteRows     = !dummyRstm.hasColumnNoCase("MaxQualifyingWriteRows") ? "" : " ,max([MaxQualifyingWriteRows]) as [MaxQualifyingWriteRows] \n";
		String col_AvgQualifyingWriteRows     = !dummyRstm.hasColumnNoCase("AvgQualifyingWriteRows") ? "" : " ,avg([AvgQualifyingWriteRows]) as [AvgQualifyingWriteRows] \n";
		
		String col_HasAutoParams              = !dummyRstm.hasColumnNoCase("HasAutoParams"             ) ? "" : " ,avg(cast([HasAutoParams] as int)) as [HasAutoParams] \n";
		String col_ParallelDegree             = !dummyRstm.hasColumnNoCase("ParallelDegree"            ) ? "" : " ,avg([ParallelDegree])             as [ParallelDegree] \n";
		String col_ParallelDegreeReduced      = !dummyRstm.hasColumnNoCase("ParallelDegreeReduced"     ) ? "" : " ,sum([ParallelDegreeReduced])      as [ParallelDegreeReduced] \n";
		String col_ParallelPlanRanSerial      = !dummyRstm.hasColumnNoCase("ParallelPlanRanSerial"     ) ? "" : " ,sum([ParallelPlanRanSerial])      as [ParallelPlanRanSerial] \n";
		String col_WorkerThreadDeficit        = !dummyRstm.hasColumnNoCase("WorkerThreadDeficit"       ) ? "" : " ,sum([WorkerThreadDeficit])        as [WorkerThreadDeficit] \n";
//		String col_TransactionIsolationLevel  = !dummyRstm.hasColumnNoCase("TransactionIsolationLevel" ) ? "" : " ,max([TransactionIsolationLevel])  as [TransactionIsolationLevel] \n";
//		String col_TransactionMode            = !dummyRstm.hasColumnNoCase("TransactionMode"           ) ? "" : " ,max([TransactionMode])            as [TransactionMode] \n";
		String col_StatementSize              = !dummyRstm.hasColumnNoCase("StatementSize"             ) ? "" : " ,avg([StatementSize]/1024)         as [StatementSize] \n";
		String col_MaxUsageCount              = !dummyRstm.hasColumnNoCase("MaxUsageCount"             ) ? "" : " ,max([MaxUsageCount])              as [MaxUsageCount] \n";
		String col_NumRecompilesSchemaChanges = !dummyRstm.hasColumnNoCase("NumRecompilesSchemaChanges") ? "" : " ,sum([NumRecompilesSchemaChanges]) as [NumRecompilesSchemaChanges] \n"; // diff calculated
		String col_NumRecompilesPlanFlushes   = !dummyRstm.hasColumnNoCase("NumRecompilesPlanFlushes"  ) ? "" : " ,sum([NumRecompilesPlanFlushes])   as [NumRecompilesPlanFlushes] \n";   // diff calculated
		
		// Sparklines/charts
		String col_UseCountDiff__chart        = !dummyRstm.hasColumnNoCase("UseCountDiff"        ) ? "" : " ,cast('' as varchar(512)) as [UseCountDiff__chart] \n"; 
		String col_ElapsedTimeDiff__chart     = !dummyRstm.hasColumnNoCase("TotalElapsedTimeDiff") ? "" : " ,cast('' as varchar(512)) as [ElapsedTimeDiff__chart] \n"; 
		String col_CpuTimeDiff__chart         = !dummyRstm.hasColumnNoCase("TotalCpuTimeDiff"    ) ? "" : " ,cast('' as varchar(512)) as [CpuTimeDiff__chart] \n"; 
		String col_EstWaitTimeDiff__chart     = !dummyRstm.hasColumnNoCase("TotalEstWaitTimeDiff") ? "" : " ,cast('' as varchar(512)) as [EstWaitTimeDiff__chart] \n"; 
		String col_LioDiff__chart             = !dummyRstm.hasColumnNoCase("TotalLioDiff"        ) ? "" : " ,cast('' as varchar(512)) as [LioDiff__chart] \n"; 
		String col_LioDiffMb__chart           = !dummyRstm.hasColumnNoCase("TotalLioDiff"        ) ? "" : " ,cast('' as varchar(512)) as [LioDiffMb__chart] \n";
		String col_PioDiff__chart             = !dummyRstm.hasColumnNoCase("TotalPioDiff"        ) ? "" : " ,cast('' as varchar(512)) as [PioDiff__chart] \n"; 
		String col_LockWaitsDiff__chart       = !dummyRstm.hasColumnNoCase("LockWaitsDiff"       ) ? "" : " ,cast('' as varchar(512)) as [LockWaitsDiff__chart] \n"; 
		String col_LockWaitTimeDiff__chart    = !dummyRstm.hasColumnNoCase("LockWaitTimeDiff"    ) ? "" : " ,cast('' as varchar(512)) as [LockWaitTimeDiff__chart] \n"; 
		String col_SortCount__chart           = !dummyRstm.hasColumnNoCase("SortCountDiff"       ) ? "" : " ,cast('' as varchar(512)) as [SortCount__chart] \n";
		String col_SortSpilledCount__chart    = !dummyRstm.hasColumnNoCase("SortSpilledCount"    ) ? "" : " ,cast('' as varchar(512)) as [SortSpilledCount__chart] \n";
		String col_SortTimeDiff__chart        = !dummyRstm.hasColumnNoCase("TotalSortTimeDiff"   ) ? "" : " ,cast('' as varchar(512)) as [SortTimeDiff__chart] \n";

		// Reset some columns
		if (asePageSize == -1)
		{
			col_LioDiffMb__chart = "";
			col_TotalLioMb__sum  = "";
			col_AvgLioMb         = "";
		}		

		// Order By
//		String extraWhereClause = "where [UseCount] > 100 or [AvgLIO] > 100000\n"; // this is only used if we havn't got "TotalLioDiff" or "TotalCpuTimeDiff"
		String extraWhereClause = "";
		String orderByCol = "[samples_count]";
		if (dummyRstm.hasColumnNoCase("UseCountDiff"))     { orderByCol = "[UseCount__sum]";     extraWhereClause = "  and [UseCountDiff] > 0 \n"; }
//		if (dummyRstm.hasColumnNoCase("AvgCpuTime"))       { orderByCol = "[AvgCpuTime_est_max]";   }
		if (dummyRstm.hasColumnNoCase("TotalLioDiff"))     { orderByCol = "[TotalLio__sum]";     }
		if (dummyRstm.hasColumnNoCase("TotalCpuTimeDiff")) { orderByCol = "[TotalCpuTime__sum]"; }
		if (ReportType.WAIT_TIME.equals(_reportType))      { orderByCol = "[TotalEstWaitTime__sum]"; }
		
		String whereFilter_skipNewDiffRateRows = !skipNewDiffRateRows ? "" : "  and [CmNewDiffRateRow] = 0 -- only records that has been diff calculations (not first time seen, some ASE Versions has a bug that do not clear counters on reuse) \n";

		// Build: SQL Statement
		String sql = getCmDiffColumnsAsSqlComment("CmStmntCacheDetails")
				+ "select top " + topRows + " \n"
				+ "  [DBName] \n"
				+ " ,[Hashkey] \n"
				+ " ,cast('' as varchar(10))     as [txt] \n"
//				+ " ,[ObjectName] \n"
				+ " ,min([ObjectName])           as [ObjectName] \n"	
				+ " \n"

				+ col_UseCountDiff__chart
//				+ col_UseCount_max
				+ col_UseCount__sum
				+ " \n"

				+ col_ElapsedTimeDiff__chart
				+ col_TotalElapsedTime__sum
				+ col_AvgElapsedTime
				+ " \n"

				+ col_CpuTimeDiff__chart
				+ col_TotalCpuTime__sum
				+ col_AvgCpuTime
				+ " \n"

				+ col_EstWaitTimeDiff__chart
				+ col_TotalEstWaitTime__sum
				+ col_AvgEstWaitTime
				+ " \n"

				+ col_LioDiff__chart
				+ col_TotalLio__sum
				+ col_AvgLIO
				+ " \n"
				
				+ col_PioDiff__chart
				+ col_TotalPio__sum
				+ col_AvgPIO
				+ " \n"

//				+ col_AvgCpuTime_est_max
				+ " \n"
				
				+ col_LockWaitsDiff__chart
				+ col_LockWaits__sum
				+ col_AvgLockWaits
				+ " \n"

				+ col_LockWaitTimeDiff__chart
				+ col_LockWaitTime__sum
				+ col_AvgLockWaitTime
				+ " \n"
				
				+ col_SortCount__chart
				+ col_SortCount__sum
				+ col_AvgSortCount
				+ " \n"

				+ col_SortSpilledCount__chart
				+ col_SortSpilledCount__sum
				+ col_AvgSortSpilledCount
				+ " \n"

				+ col_SortTimeDiff__chart
				+ col_TotalSortTime__sum
				+ col_AvgSortTime
				+ col_MaxSortTime__max
				+ " \n"

				+ col_AvgScanRows                       // How many rows: Rows read during scan
				+ col_AvgQualifyingReadRows             // How many rows: Rows sent back to client ???
				+ col_AvgQualifyingWriteRows            // How many rows: Rows affected by ins/upd/del ???
				+ " \n"

				+ col_MaxScanRows                       // How many rows: Rows read during scan
				+ col_MaxQualifyingReadRows             // How many rows: Rows sent back to client ???
				+ col_MaxQualifyingWriteRows            // How many rows: Rows affected by ins/upd/del ???
				+ " \n"
				
				+ col_HasAutoParams
				+ col_ParallelDegree
				+ col_ParallelDegreeReduced
				+ col_ParallelPlanRanSerial
				+ col_WorkerThreadDeficit
				+ col_StatementSize
				+ col_MaxUsageCount
				+ col_NumRecompilesSchemaChanges
				+ col_NumRecompilesPlanFlushes
				
				+ col_LioDiffMb__chart
				+ col_TotalLioMb__sum
				+ col_AvgLioMb

				+ " ,count(*)                    as [samples_count] \n"
				+ " ,min([SessionSampleTime])    as [SessionSampleTime_min] \n"
				+ " ,max([SessionSampleTime])    as [SessionSampleTime_max] \n"
				+ " ,cast('' as varchar(30))     as [Duration] \n"
				+ " ,sum([CmNewDiffRateRow])     as [newDiffRow_sum] \n"
				+ " ,sum([CmSampleMs])           as [CmSampleMs_sum] \n"

				+ "from [CmStmntCacheDetails_diff] \n"
				+ "where 1 = 1 \n"
				+ getReportPeriodSqlWhere()
				+ extraWhereClause
				+ whereFilter_skipNewDiffRateRows
				+ "group by [DBName], [Hashkey] \n"
//also in all/some reports, do not trust count(*), it has to be where changes has happened (I think I did something like that in SQL Server SqlServerTopCmExecQueryStats)
				+ " \n"
				+ "order by " + orderByCol + " desc \n"
				+ "";

		_shortRstm = executeQuery(conn, sql, false, "TopStatementCacheCalls");
		if (_shortRstm == null)
		{
			_shortRstm = ResultSetTableModel.createEmpty("TopStatementCacheCalls");
			return;
		}
		else
		{
			// Highlight sort column
			String orderByCol_noBrackets = orderByCol.replace("[", "").replace("]", "");
			_shortRstm.setHighlightSortColumns(orderByCol_noBrackets);

			// Describe the table
			setSectionDescription(_shortRstm);

			setDurationColumn(_shortRstm, "SessionSampleTime_min", "SessionSampleTime_max", "Duration");


			//----------------------------------------
			// Spark lines -- mini charts
			//----------------------------------------

			String whereKeyColumn = "DBName, Hashkey";

			if (StringUtil.hasValue(col_UseCountDiff__chart))
			{
				_miniChartJsList.add(SparklineHelper.createSparkline(conn, this, _shortRstm, 
						SparkLineParams.create       (DataSource.CounterModel)
						.setHtmlChartColumnName      ("UseCountDiff__chart")
						.setHtmlWhereKeyColumnName   (whereKeyColumn)
						.setDbmsTableName            ("CmStmntCacheDetails_diff")
						.setDbmsSampleTimeColumnName ("SessionSampleTime")
						.setDbmsDataValueColumnName  ("UseCountDiff")
						.setDbmsWhereKeyColumnName   (whereKeyColumn)
						.setDbmsExtraWhereClause     (whereFilter_skipNewDiffRateRows)
						.validate()));
			}

			if (StringUtil.hasValue(col_ElapsedTimeDiff__chart))
			{
				_miniChartJsList.add(SparklineHelper.createSparkline(conn, this, _shortRstm, 
						SparkLineParams.create       (DataSource.CounterModel)
						.setHtmlChartColumnName      ("ElapsedTimeDiff__chart")
						.setHtmlWhereKeyColumnName   (whereKeyColumn)
						.setDbmsTableName            ("CmStmntCacheDetails_diff")
						.setDbmsSampleTimeColumnName ("SessionSampleTime")
//						.setDbmsDataValueColumnName  ("TotalElapsedTimeDiff")
						.setDbmsDataValueColumnName  ("sum(1.0*[TotalElapsedTimeDiff]) / nullif(sum([UseCountDiff]), 0)").setGroupDataAggregationType(AggType.USER_PROVIDED).setDecimalScale(1)
						.setDbmsWhereKeyColumnName   (whereKeyColumn)
						.setDbmsExtraWhereClause     (whereFilter_skipNewDiffRateRows)
						.validate()));
			}

			if (StringUtil.hasValue(col_CpuTimeDiff__chart))
			{
				_miniChartJsList.add(SparklineHelper.createSparkline(conn, this, _shortRstm, 
						SparkLineParams.create       (DataSource.CounterModel)
						.setHtmlChartColumnName      ("CpuTimeDiff__chart")
						.setHtmlWhereKeyColumnName   (whereKeyColumn)
						.setDbmsTableName            ("CmStmntCacheDetails_diff")
						.setDbmsSampleTimeColumnName ("SessionSampleTime")
//						.setDbmsDataValueColumnName  ("TotalCpuTimeDiff")
						.setDbmsDataValueColumnName  ("sum(1.0*[TotalCpuTimeDiff]) / nullif(sum([UseCountDiff]), 0)").setGroupDataAggregationType(AggType.USER_PROVIDED).setDecimalScale(1)
						.setDbmsWhereKeyColumnName   (whereKeyColumn)
						.setDbmsExtraWhereClause     (whereFilter_skipNewDiffRateRows)
						.validate()));
			}

			if (StringUtil.hasValue(col_EstWaitTimeDiff__chart))
			{
				_miniChartJsList.add(SparklineHelper.createSparkline(conn, this, _shortRstm, 
						SparkLineParams.create       (DataSource.CounterModel)
						.setHtmlChartColumnName      ("EstWaitTimeDiff__chart")
						.setHtmlWhereKeyColumnName   (whereKeyColumn)
						.setDbmsTableName            ("CmStmntCacheDetails_diff")
						.setDbmsSampleTimeColumnName ("SessionSampleTime")
//						.setDbmsDataValueColumnName  ("TotalEstWaitTimeDiff")
						.setDbmsDataValueColumnName  ("sum(1.0*[TotalEstWaitTimeDiff]) / nullif(sum([UseCountDiff]), 0)").setGroupDataAggregationType(AggType.USER_PROVIDED).setDecimalScale(1)
						.setDbmsWhereKeyColumnName   (whereKeyColumn)
						.setDbmsExtraWhereClause     (whereFilter_skipNewDiffRateRows)
						.validate()));
			}

			if (StringUtil.hasValue(col_LioDiff__chart))
			{
				_miniChartJsList.add(SparklineHelper.createSparkline(conn, this, _shortRstm, 
						SparkLineParams.create       (DataSource.CounterModel)
						.setHtmlChartColumnName      ("LioDiff__chart")
						.setHtmlWhereKeyColumnName   (whereKeyColumn)
						.setDbmsTableName            ("CmStmntCacheDetails_diff")
						.setDbmsSampleTimeColumnName ("SessionSampleTime")
//						.setDbmsDataValueColumnName  ("TotalLioDiff")
						.setDbmsDataValueColumnName  ("sum(1.0*[TotalLioDiff]) / nullif(sum([UseCountDiff]), 0)").setGroupDataAggregationType(AggType.USER_PROVIDED).setDecimalScale(1)
						.setDbmsWhereKeyColumnName   (whereKeyColumn)
						.setDbmsExtraWhereClause     (whereFilter_skipNewDiffRateRows)
						.validate()));
			}

			if (StringUtil.hasValue(col_LioDiffMb__chart))
			{
				_miniChartJsList.add(SparklineHelper.createSparkline(conn, this, _shortRstm, 
						SparkLineParams.create       (DataSource.CounterModel)
						.setHtmlChartColumnName      ("LioDiffMb__chart")
						.setHtmlWhereKeyColumnName   (whereKeyColumn)
						.setDbmsTableName            ("CmStmntCacheDetails_diff")
						.setDbmsSampleTimeColumnName ("SessionSampleTime")
//						.setDbmsDataValueColumnName  ("TotalLioDiff")
						.setDbmsDataValueColumnName  ("sum([TotalLioDiff]) * 1.0 / " + asePageSizeDivider + "  / nullif(sum([UseCountDiff]), 0)").setGroupDataAggregationType(AggType.USER_PROVIDED).setDecimalScale(1)
						.setDbmsWhereKeyColumnName   (whereKeyColumn)
						.setDbmsExtraWhereClause     (whereFilter_skipNewDiffRateRows)
						.validate()));
			}

			if (StringUtil.hasValue(col_PioDiff__chart))
			{
				_miniChartJsList.add(SparklineHelper.createSparkline(conn, this, _shortRstm, 
						SparkLineParams.create       (DataSource.CounterModel)
						.setHtmlChartColumnName      ("PioDiff__chart")
						.setHtmlWhereKeyColumnName   (whereKeyColumn)
						.setDbmsTableName            ("CmStmntCacheDetails_diff")
						.setDbmsSampleTimeColumnName ("SessionSampleTime")
//						.setDbmsDataValueColumnName  ("TotalPioDiff")
						.setDbmsDataValueColumnName  ("sum(1.0*[TotalPioDiff]) / nullif(sum([UseCountDiff]), 0)").setGroupDataAggregationType(AggType.USER_PROVIDED).setDecimalScale(1)
						.setDbmsWhereKeyColumnName   (whereKeyColumn)
						.setDbmsExtraWhereClause     (whereFilter_skipNewDiffRateRows)
						.validate()));
			}

			if (StringUtil.hasValue(col_LockWaitsDiff__chart))
			{
				_miniChartJsList.add(SparklineHelper.createSparkline(conn, this, _shortRstm, 
						SparkLineParams.create       (DataSource.CounterModel)
						.setHtmlChartColumnName      ("LockWaitsDiff__chart")
						.setHtmlWhereKeyColumnName   (whereKeyColumn)
						.setDbmsTableName            ("CmStmntCacheDetails_diff")
						.setDbmsSampleTimeColumnName ("SessionSampleTime")
//						.setDbmsDataValueColumnName  ("LockWaitsDiff")
						.setDbmsDataValueColumnName  ("sum(1.0*[LockWaitsDiff]) / nullif(sum([UseCountDiff]), 0)").setGroupDataAggregationType(AggType.USER_PROVIDED).setDecimalScale(1)
						.setDbmsWhereKeyColumnName   (whereKeyColumn)
						.setDbmsExtraWhereClause     (whereFilter_skipNewDiffRateRows)
						.validate()));
			}

			if (StringUtil.hasValue(col_LockWaitTimeDiff__chart))
			{
				_miniChartJsList.add(SparklineHelper.createSparkline(conn, this, _shortRstm, 
						SparkLineParams.create       (DataSource.CounterModel)
						.setHtmlChartColumnName      ("LockWaitTimeDiff__chart")
						.setHtmlWhereKeyColumnName   (whereKeyColumn)
						.setDbmsTableName            ("CmStmntCacheDetails_diff")
						.setDbmsSampleTimeColumnName ("SessionSampleTime")
//						.setDbmsDataValueColumnName  ("LockWaitTimeDiff")
						.setDbmsDataValueColumnName  ("sum(1.0*[LockWaitTimeDiff]) / nullif(sum([UseCountDiff]), 0)").setGroupDataAggregationType(AggType.USER_PROVIDED).setDecimalScale(1)
						.setDbmsWhereKeyColumnName   (whereKeyColumn)
						.setDbmsExtraWhereClause     (whereFilter_skipNewDiffRateRows)
						.validate()));
			}

			if (StringUtil.hasValue(col_SortCount__chart))
			{
				_miniChartJsList.add(SparklineHelper.createSparkline(conn, this, _shortRstm, 
						SparkLineParams.create       (DataSource.CounterModel)
						.setHtmlChartColumnName      ("SortCount__chart")
						.setHtmlWhereKeyColumnName   (whereKeyColumn)
						.setDbmsTableName            ("CmStmntCacheDetails_diff")
						.setDbmsSampleTimeColumnName ("SessionSampleTime")
//						.setDbmsDataValueColumnName  ("SortCountDiff")
						.setDbmsDataValueColumnName  ("sum(1.0*[SortCountDiff]) / nullif(sum([UseCountDiff]), 0)").setGroupDataAggregationType(AggType.USER_PROVIDED).setDecimalScale(1)
						.setDbmsWhereKeyColumnName   (whereKeyColumn)
						.setDbmsExtraWhereClause     (whereFilter_skipNewDiffRateRows)
						.validate()));
			}

			if (StringUtil.hasValue(col_SortSpilledCount__chart))
			{
				_miniChartJsList.add(SparklineHelper.createSparkline(conn, this, _shortRstm, 
						SparkLineParams.create       (DataSource.CounterModel)
						.setHtmlChartColumnName      ("SortSpilledCount__chart")
						.setHtmlWhereKeyColumnName   (whereKeyColumn)
						.setDbmsTableName            ("CmStmntCacheDetails_diff")
						.setDbmsSampleTimeColumnName ("SessionSampleTime")
//						.setDbmsDataValueColumnName  ("SortSpilledCount")
						.setDbmsDataValueColumnName  ("sum(1.0*[SortSpilledCount]) / nullif(sum([UseCountDiff]), 0)").setGroupDataAggregationType(AggType.USER_PROVIDED).setDecimalScale(1)
						.setDbmsWhereKeyColumnName   (whereKeyColumn)
						.setDbmsExtraWhereClause     (whereFilter_skipNewDiffRateRows)
						.validate()));
			}

			if (StringUtil.hasValue(col_SortTimeDiff__chart))
			{
				_miniChartJsList.add(SparklineHelper.createSparkline(conn, this, _shortRstm, 
						SparkLineParams.create       (DataSource.CounterModel)
						.setHtmlChartColumnName      ("SortTimeDiff__chart")
						.setHtmlWhereKeyColumnName   (whereKeyColumn)
						.setDbmsTableName            ("CmStmntCacheDetails_diff")
						.setDbmsSampleTimeColumnName ("SessionSampleTime")
//						.setDbmsDataValueColumnName  ("TotalSortTimeDiff")
						.setDbmsDataValueColumnName  ("sum(1.0*[TotalSortTimeDiff]) / nullif(sum([UseCountDiff]), 0)").setGroupDataAggregationType(AggType.USER_PROVIDED).setDecimalScale(1)
						.setDbmsWhereKeyColumnName   (whereKeyColumn)
						.setDbmsExtraWhereClause     (whereFilter_skipNewDiffRateRows)
						.validate()));
			}

			
			//--------------------------------------------------------------------------------------
			// get Executed SQL Statements from the SQL Capture ...
			//--------------------------------------------------------------------------------------
			if (_shortRstm.getRowCount() > 0)
			{
				for (int r=0; r<_shortRstm.getRowCount(); r++)
				{
					// Get Actual Executed SQL Text for current 'Hashkey'
					Map<String, Object> whereColValMap = new LinkedHashMap<>();
					whereColValMap.put("HashKey", _shortRstm.getValueAsInteger(r, "Hashkey")); // Note: 'HashKey' (capital K) in [MonSqlCapStatements] but 'Hashkey' in RSTM

					_keyToExecutedSql = getSqlCapExecutedSqlText(_keyToExecutedSql, conn, true, whereColValMap);
				}
			}


			//----------------------------------------------------
			// Create a SQL-Details ResultSet based on values in _shortRstm
			//----------------------------------------------------
			if ( ReportType.CPU_TIME.equals(_reportType) )
			{
				SimpleResultSet srs = new SimpleResultSet();

				srs.addColumn("DBName"     , Types.VARCHAR,       60, 0);
				srs.addColumn("ObjectName" , Types.VARCHAR,       60, 0);
				srs.addColumn("XmlPlan"    , Types.VARCHAR, 1024*128, 0);
				srs.addColumn("Sparklines" , Types.VARCHAR,      512, 0); 
				srs.addColumn("SQLText"    , Types.VARCHAR, 1024*128, 0); // this is 'text' in the origin table

				// Position in the "source" _shortRstm table (values we will fetch)
				int pos_dbname     = _shortRstm.findColumn("DBName");
				int pos_hashkey    = _shortRstm.findColumn("Hashkey");
				int pos_objectName = _shortRstm.findColumn("ObjectName");


				ColumnCopyRender msToHMS    = HtmlTableProducer.MS_TO_HMS;
				ColumnCopyRender oneDecimal = HtmlTableProducer.ONE_DECIMAL;
				
				HtmlTableProducer htp = new HtmlTableProducer(_shortRstm, "dsr-sub-table-chart");
				htp.setTableHeaders("Charts at 10 minute interval", "Total;style='text-align:right!important'", "Avg per exec;style='text-align:right!important'", "");
				if (StringUtil.hasValue(col_UseCountDiff__chart    )) htp.add("exec-cnt"   , new ColumnCopyRow().add( new ColumnCopyDef("UseCountDiff__chart"    ) ).add(new ColumnCopyDef("UseCount__sum").setColBold())     .addEmptyCol()                                                         .addEmptyCol() );
				if (StringUtil.hasValue(col_ElapsedTimeDiff__chart )) htp.add("exec-time"  , new ColumnCopyRow().add( new ColumnCopyDef("ElapsedTimeDiff__chart" ) ).add(new ColumnCopyDef("TotalElapsedTime__sum", msToHMS) ).add(new ColumnCopyDef("AvgElapsedTime"     , oneDecimal).setColBold()).add(new ColumnStatic("ms")) );
				if (StringUtil.hasValue(col_CpuTimeDiff__chart     )) htp.add("cpu-time"   , new ColumnCopyRow().add( new ColumnCopyDef("CpuTimeDiff__chart"     ) ).add(new ColumnCopyDef("TotalCpuTime__sum"    , msToHMS) ).add(new ColumnCopyDef("AvgCpuTime"         , oneDecimal).setColBold()).add(new ColumnStatic("ms")) );
				if (StringUtil.hasValue(col_EstWaitTimeDiff__chart )) htp.add("wait-time"  , new ColumnCopyRow().add( new ColumnCopyDef("EstWaitTimeDiff__chart" ) ).add(new ColumnCopyDef("TotalEstWaitTime__sum", msToHMS) ).add(new ColumnCopyDef("AvgEstWaitTime"     , oneDecimal).setColBold()).add(new ColumnStatic("ms")) );
				if (StringUtil.hasValue(col_LioDiff__chart         )) htp.add("l-read"     , new ColumnCopyRow().add( new ColumnCopyDef("LioDiff__chart"         ) ).add(new ColumnCopyDef("TotalLio__sum"                 ) ).add(new ColumnCopyDef("AvgLIO"             , oneDecimal).setColBold()).add(new ColumnStatic("pgs")) );
				if (StringUtil.hasValue(col_LioDiffMb__chart       )) htp.add("l-read-mb"  , new ColumnCopyRow().add( new ColumnCopyDef("LioDiffMb__chart"       ) ).add(new ColumnCopyDef("TotalLioMb__sum"               ) ).add(new ColumnCopyDef("AvgLioMb"           , oneDecimal).setColBold()).add(new ColumnStatic("mb" )) );
				if (StringUtil.hasValue(col_PioDiff__chart         )) htp.add("p-read"     , new ColumnCopyRow().add( new ColumnCopyDef("PioDiff__chart"         ) ).add(new ColumnCopyDef("TotalPio__sum"                 ) ).add(new ColumnCopyDef("AvgPIO"             , oneDecimal).setColBold()).add(new ColumnStatic("pgs")) );
				if (StringUtil.hasValue(col_LockWaitsDiff__chart   )) htp.add("lock-waits" , new ColumnCopyRow().add( new ColumnCopyDef("LockWaitsDiff__chart"   ) ).add(new ColumnCopyDef("LockWaits__sum"                ) ).add(new ColumnCopyDef("AvgLockWaits"       , oneDecimal).setColBold()).add(new ColumnStatic("#")) );
				if (StringUtil.hasValue(col_LockWaitTimeDiff__chart)) htp.add("lock-wtime" , new ColumnCopyRow().add( new ColumnCopyDef("LockWaitTimeDiff__chart") ).add(new ColumnCopyDef("LockWaitTime__sum"    , msToHMS) ).add(new ColumnCopyDef("AvgLockWaitTime"    , oneDecimal).setColBold()).add(new ColumnStatic("ms")) );
				if (StringUtil.hasValue(col_SortCount__chart       )) htp.add("sort-cnt"   , new ColumnCopyRow().add( new ColumnCopyDef("SortCount__chart"       ) ).add(new ColumnCopyDef("SortCount__sum"                ) ).add(new ColumnCopyDef("AvgSortCount"       , oneDecimal).setColBold()).add(new ColumnStatic("#")) );
				if (StringUtil.hasValue(col_SortSpilledCount__chart)) htp.add("sort-spills", new ColumnCopyRow().add( new ColumnCopyDef("SortSpilledCount__chart") ).add(new ColumnCopyDef("SortSpilledCount__sum"         ) ).add(new ColumnCopyDef("AvgSortSpilledCount", oneDecimal).setColBold()).add(new ColumnStatic("#")) );
				if (StringUtil.hasValue(col_SortTimeDiff__chart    )) htp.add("sort-time"  , new ColumnCopyRow().add( new ColumnCopyDef("SortTimeDiff__chart"    ) ).add(new ColumnCopyDef("TotalSortTime__sum"   , msToHMS) ).add(new ColumnCopyDef("AvgSortTime"        , oneDecimal).setColBold()).add(new ColumnStatic("ms")) );
				htp.validate();

				// Filter out some rows...
				htp.setRowFilter(new HtmlTableProducer.RowFilter()
				{
					@Override
					public boolean include(ResultSetTableModel rstm, int rstmRow, String rowKey)
					{
						if ("lock-waits".equals(rowKey) || "lock-wtime".equals(rowKey))
						{
							return rstm.hasColumn("LockWaits__sum") && rstm.getValueAsInteger(rstmRow, "LockWaits__sum") > 1;
						}
						if ("sort-cnt".equals(rowKey) || "sort-spills".equals(rowKey) || "sort-time".equals(rowKey))
						{
							return rstm.hasColumn("SortCount__sum") && rstm.getValueAsInteger(rstmRow, "SortCount__sum") > 1;
						}
						return true;
					}
				});


				// add rows to Simple ResultSet
				if (pos_dbname >= 0 && pos_objectName >= 0 && pos_hashkey >= 0)
				{
					for (int r=0; r<_shortRstm.getRowCount(); r++)
					{
						String dbname     = _shortRstm.getValueAsString(r, pos_dbname);
						String hashKey    = _shortRstm.getValueAsString(r, pos_hashkey);
						String objectName = _shortRstm.getValueAsString(r, pos_objectName);

						String xmlPlan            = null;
						String xmlPlanParamsTable = "";
						String query              = "--not-found--";;
						String xmlPlanCellContent = "--not-found--";

						// Get XML plan from DDL Storage
						try {
							xmlPlan = getXmlShowplanFromMonDdlStorage(conn, objectName);
						} catch(SQLException ex) { 
							setProblemException(ex); 
						}

						// Extract SQL from XML, also add XML to: _planMap<objectName, xmlPlan>
						if (StringUtil.hasValue(xmlPlan))
						{
							query = XmlPlanAseUtils.getSqlStatement(xmlPlan);
//							query = StringEscapeUtils.escapeHtml4(query);

							xmlPlanParamsTable = XmlPlanAseUtils.getCompileAndExecParamsAsHtml(xmlPlan);

							// Create "Copy XML" html link
							String planHandle = objectName.replace('*', '_');
							xmlPlanCellContent = "<a href='#showplan-list' title='Copy plan to clipboard... then you can copy it into SqlW to view the GUI Plan!' onclick='showplanForId(\"" + planHandle + "\"); return true;'>Copy XML</a>";

							// Add the Plan the a Map so we later can write the information to the output/HTML file
							if (objectName != null && (objectName.trim().startsWith("*ss") || objectName.trim().startsWith("*sq")) )
								_planMap.put(objectName, xmlPlan);
						}
						
						// Parse the 'sqlText' and extract Table Names..
						// - then get table and index information 
						String tableInfo = "";
						boolean parseSqlText = true;
						List<String> tableList = Collections.emptyList();
						if (StringUtil.hasValue(query) && parseSqlText)
						{
							// Parse the SQL Text to get all tables that are used in the Statement
							String problemDesc = "";
							try { tableList = SqlParserUtils.getTables(query, true); }
							catch (ParseException pex) { problemDesc = pex + ""; }

							// Get information about ALL tables in list 'tableList' from the DDL Storage
							List<AseTableInfo> tableInfoList = getTableInformationFromMonDdlStorage(conn, tableList);
							if (tableInfoList.isEmpty() && StringUtil.isNullOrBlank(problemDesc))
								problemDesc = "-- No tables was found in the DDL Storage for tables: " + tableList;

							// And make it into a HTML table with various information about the table and indexes 
							tableInfo = problemDesc + getTableInfoAsHtmlTable(tableInfoList, tableList, true, "dsr-sub-table-tableinfo");
						}

						// Grab all SparkLines we defined in 'subTableRowSpec'
						String sparklines = htp.getHtmlTextForRow(r);

						String objName_hashKey = objectName + "<br>\n<br>\n<b>Hashkey=</b>" + hashKey;

						// SQL Text (and Compile Execution parameters)
						String sqlTextValue = "<xmp>" + query + "</xmp>";

						// ADD Table and Index information (SHOW details, by default)
						if (StringUtil.hasValue(tableInfo))
						{
							// Surround with collapse div
							sqlTextValue += ""
									//+ "<!--[if !mso]><!--> \n" // BEGIN: IGNORE THIS SECTION FOR OUTLOOK

									+ "\n<br>\n<br>\n"
									+ "<details open> \n"
									+ "<summary>Show/Hide Table information for: " + tableList + "</summary> \n"
									+ tableInfo
									+ "</details> \n"

									//+ "<!--<![endif]-->    \n" // END: IGNORE THIS SECTION FOR OUTLOOK
									+ "";
						}

						// ADD Compile and Execution plan (HIDE details, by default)
						if (StringUtil.hasValue(xmlPlanParamsTable))
						{
							sqlTextValue += "" 
									+ "<!--[if !mso]><!--> \n" // BEGIN: IGNORE THIS SECTION FOR OUTLOOK

									+ ( StringUtil.hasValue(tableInfo) ? "" : "\n<br>\n<br>\n") // add space if we do NOT have 'tableInfo'
									+ "<details> \n"
									+ "<summary>Show/Hide Compile and Execution Parameters</summary> \n"
									+ xmlPlanParamsTable
									+ "</details> \n"

									+ "<!--<![endif]-->    \n" // END: IGNORE THIS SECTION FOR OUTLOOK
									+ "";
						}
						
						//-------------------------------------
						// add record to SimpleResultSet
						srs.addRow(dbname, objName_hashKey, xmlPlanCellContent, sparklines, sqlTextValue);
					}
				}

				// GET SQLTEXT (only)
				try
				{
					// Note the 'srs' is populated when reading above ResultSet from query
					_ssqlRstm = createResultSetTableModel(srs, "Top SQL TEXT", null);
					srs.close();
				}
				catch (SQLException ex)
				{
					setProblemException(ex);
		
					_ssqlRstm = ResultSetTableModel.createEmpty("Top SQL TEXT");
					_logger.warn("Problems getting Top SQL TEXT: " + ex);
				}
			} // end: ReportType.CPU_TIME

		} // end: has: _shortRstm

	} // end: method

} // end: class

//-------------------------------------------------------------------------------
// com.asetune.cm.ase.CmStmntCacheDetails
//-------------------------------------------------------------------------------
// public static final String[] DIFF_COLUMNS     = new String[] {
// 		"UseCountDiff", 
// 		"NumRecompilesPlanFlushes", 
// 		"NumRecompilesSchemaChanges", 
// 		"LockWaitsDiff", 
// 		"LockWaitTimeDiff", 
// 		"SortCountDiff", 
// 		"SortSpilledCount", 
// 		"TotalSortTimeDiff", 
// 		"ParallelDegreeReduced", 
// 		"ParallelPlanRanSerial", 
// 		"WorkerThreadDeficit",
// 		"TotalPioDiff", 
// 		"TotalLioDiff", 
// 		"TotalCpuTimeDiff", 
// 		"TotalElapsedTimeDiff", 
// 		"TotalEstWaitTimeDiff"
// 		};
//-------------------------------------------------------------------------------

//-------------------------------------------------------------------------------
// SAP - Documentation
// https://help.sap.com/viewer/ad4a1ddf1bf34768841bd09d1eddf434/16.0.4.1/en-US/ab9cd3c8bc2b1014b077e45dbad89abe.html?q=monCachedStatement
//-------------------------------------------------------------------------------
//  Names                          Datatypes     Description
//  ------------------------------ ------------- --------------------------------------------------
//  InstanceID                     tinyint       (Cluster environments only) ID of an instance in a shared-disk cluster.
//  SSQLID                         int           Unique identifier for each cached statement. This value is treated as a primary key for monCachedStatement, and is used in functions. show_cached_text uses SSQLID to refer to individual statements in the cache.
//  Hashkey                        int           Hash value of the SQL text of the cached statement. A hash key is generated based on a statement’s text, and can be used as an approximate key for searching other monitoring tables.
//  StmtType                       tinyint       
//  UserID                         int           User ID of the user who initiated the statement that has been cached.
//  SUserID                        int           Server ID of the user who initiated the cached statement.
//  DBID                           smallint      Database ID of the database from which the statement was cached.
//  UseCount                       int           Number of times the statement was accessed after it was cached.
//  StatementSize                  int           Size of the cached statement, in bytes.
//  MinPlanSizeKB                  int           Size of the plan when it is not in use, in kilobytes.
//  MaxPlanSizeKB                  int           Size of the plan when it is in use, in kilobytes.
//  CurrentUsageCount              int           Number of concurrent users of the cached statement. Attribute is counter.
//  MaxUsageCount                  int           Maximum number of times the cached statement’s text was simultaneously accessed. Attribute is counter.
//  NumRecompilesSchemaChanges     int           Number of times the statement was recompiled due to schema changes. Running update statistics on a table may result in changes to the best plan. This change is treated as a minor schema change. Recompiling a statement many times indicates that it is not effective to cache this particular statement, and that you may want to delete the statement from the statement cache to make space for some other, more stable, statement. Attribute is counter.
//  NumRecompilesPlanFlushes       int           Number of times the cached statement was recompiled because a plan was not found in the cache. Attribute is counter.
//  HasAutoParams                  tinyint       “true” if the statement has any parameterized literals, “false” if it does not.
//  ParallelDegree                 tinyint       Degree of parallelism used by the query that is stored for this statement
//  QuotedIdentifier               tinyint       Specifies whether the plan compiled with set quoted_identifier is enabled.
//  TransactionIsolationLevel      tinyint       Transaction isolation level for which the statement was compiled.
//  TransactionMode                tinyint       Specifies whether “chained transaction mode” is enabled for the statement.
//  SAAuthorization                tinyint       Specifies whether the plan was compiled with sa_role authorization.
//  SystemCatalogUpdate            tinyint       Specifies whether allow catalog updates was enabled when the plan was compiled.
//  MetricsCount                   int           Number of times metrics were aggregated for this statement.
//  MinPIO                         int           Maximum physical I/Os that occurred during any execution of this statement.
//  MaxPIO                         int           Maximum physical I/Os that occurred during any execution of this statement.
//  AvgPIO                         int           Average number of physical I/Os that occurred during execution of this statement.
//  MinLIO                         int           Minimum logical I/Os that occurred during any execution of this statement.
//  MaxLIO                         int           Maximum logical I/Os that occurred during any one execution of this statement.
//  AvgLIO                         int           Average number of logical I/Os that occurred during execution of this statement.
//  MinCpuTime                     int           The minimum amount of CPU time, in milliseconds, consumed by any execution of this statement.
//  MaxCpuTime                     int           The maximum amount of CPU time, in milliseconds, consumed by any execution of this statement.
//  AvgCpuTime                     int           The average amount of CPU time, in milliseconds, consumed by this statement.
//  MinElapsedTime                 int           Minimum elapsed execution time for this statement.
//  MaxElapsedTime                 int           Maximum elapsed execution time for this statement.
//  AvgElapsedTime                 int           Average elapsed execution time for this statement.
//  AvgScanRows                    int           Average number of scanned rows read per execution
//  MaxScanRows                    int           Maximum number of scanned rows read per execution
//  AvgQualifyingReadRows          int           Average number of qualifying data rows per read command execution
//  MaxQualifyingReadRows          int           Maximum number of qualifying data rows per query execution
//  AvgQualifyingWriteRows         int           Average number of qualifying data rows per query execution
//  MaxQualifyingWriteRows         int           Maximum number of qualifying data rows per query execution
//  LockWaits                      int           Total number of lock waits
//  LockWaitTime                   int           Total amount of time, in milliseconds, spent waiting for locks
//  SortCount                      int           Total number of sort operations
//  SortSpilledCount               int           Total number of sort operations spilled to disk
//  TotalSortTime                  int           Total amount of time, in milliseconds, spent in sorts
//  MaxSortTime                    int           Maximum amount of time, in milliseconds, spent in a sort
//  DBName                         varchar(30)   Name of database from which the statement was cached. Attribute is null.
//  CachedDate                     datetime      Timestamp of the date and time when the statement was first cached.
//  LastUsedDate                   datetime      Timestamp of the date and time when the cached statement was last used. Use this information with CachedDate to determine how frequently this statement is used, and whether it is helpful to have it cached.
//  LastRecompiledDate             datetime      Date when the statement was last recompiled, because of schema changes or because the statement was not found in the statement cache.
//  OptimizationGoal               varchar(30)   The optimization goal used to optimize this statement.
//  OptimizerLevel                 varchar(30)   The optimizer level used to optimize this statement.
//  ParallelDegreeReduced          int           Indicates if an insufficient number of worker threads were available to execute the query with the full degree of parallelism the query plan calls for, but the query did execute with some parallelism.
//  ParallelPlanRanSerial          int           Indicates if an insufficient number of worker threads were available to execute the query in parallel so the query was executed serially.
//  WorkerThreadDeficit            int           Indicates that the cumulative total number of worker threads were unavailable to execute this query since it was added to the statement cache.
//  TotalLIO                       bigint        Cumulative logical I/O
//  TotalPIO                       bigint        Cumulative physical I/O
//  TotalCpuTime                   bigint        Cumulative elapsed time, in seconds, this statement spent using CPU
//  TotalElapsedTime               bigint        Cumulative amount of time, in seconds spent executing this statement
//-------------------------------------------------------------------------------
