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
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Types;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.log4j.Logger;

import com.asetune.gui.ResultSetTableModel;
import com.asetune.gui.ResultSetTableModel.TableStringRenderer;
import com.asetune.pcs.report.DailySummaryReportAbstract;
import com.asetune.pcs.report.content.ase.SparklineHelper.DataSource;
import com.asetune.pcs.report.content.ase.SparklineHelper.SparkLineParams;
import com.asetune.sql.conn.DbxConnection;
import com.asetune.utils.Configuration;
import com.asetune.utils.StringUtil;

public class AseTopCmStmntCacheDetails extends AseAbstract
{
	private static Logger _logger = Logger.getLogger(AseTopCmStmntCacheDetails.class);

	private ResultSetTableModel _shortRstm;
	private ResultSetTableModel _ssqlRstm;
//	private Exception           _problem = null;
	private List<String>        _miniChartJsList = new ArrayList<>();

	private Map<Map<String, Object>, SqlCapExecutedSqlEntries> _keyToExecutedSql;
	private Map<String, String> _planMap;

	
	public AseTopCmStmntCacheDetails(DailySummaryReportAbstract reportingInstance)
	{
		super(reportingInstance);
	}

	@Override
	public boolean hasShortMessageText()
	{
		return false;
	}

	@Override
	public void writeShortMessageText(Writer w)
	throws IOException
	{
	}

	@Override
	public void writeMessageText(Writer sb)
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

//			sb.append("Row Count: " + _shortRstm.getRowCount() + "<br>\n");
			sb.append("Row Count: " + _shortRstm.getRowCount() + "&emsp;&emsp; To change number of <i>top</i> records, set property <code>" + getTopRowsPropertyName() + "=##</code><br>\n");
//			sb.append(toHtmlTable(_shortRstm));

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

			if (_ssqlRstm != null)
			{
				sb.append("Statement Cache Entries Count: " + _ssqlRstm.getRowCount() + "<br>\n");
				sb.append(toHtmlTable(_ssqlRstm));
			}
			
			sb.append("<script type='text/javascript'> \n");
			sb.append("    function showplanForId(id) \n");
			sb.append("    { \n");
			sb.append("        var showplanText = document.getElementById('plan_'+id).innerHTML \n");
//			sb.append("        QP.showPlan(document.getElementById('showplan-container'), showplanText); \n");
//			sb.append("        document.getElementById('showplan-head').innerHTML = 'Below is Execution plan for <code>plan_handle: ' + id + \"</code> <br>Note: You can also view your plan at <a href='http://www.supratimas.com' target='_blank'>http://www.supratimas.com</a>, or any other <i>plan-view</i> application by pasting (Ctrl-V) the clipboard content. <br>SentryOne Plan Explorer can be downloaded here: <a href='https://www.sentryone.com/plan-explorer' target='_blank'>https://www.sentryone.com/plan-explorer</a>\"; \n");
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
//			sb.append("                    <button type='button' class='close' data-dismiss='modal' aria-hidden='true'>&times;</button> \n");
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
		
		// Write JavaScript code for CPU SparkLine
		for (String str : _miniChartJsList)
		{
			sb.append(str);
		}
	}

	@Override
	public String getSubject()
	{
		return "Top Statement Cache Entries (order by: TotalCpuTimeDiff_sum, origin: CmStmntCacheDetails / monCachedStatement)";
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
				"Both slow and fast SQL Statements (from the Statement Cache) are presented here (ordered by: TotalCpuTimeDiff_sum) <br>" +
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
		rstm.setColumnDescription("newDiffRow_sum"        , "Number of Diff Records that was seen for the first time.");
		rstm.setColumnDescription("CmSampleMs_sum"           , "Number of milliseconds this object has been available for sampling");

		rstm.setColumnDescription("UseCountDiff_sum"         , "How many times this object was Uased");

		rstm.setColumnDescription("TotalElapsedTimeDiff_sum" , "How many milliseconds did we spend in execution during the report period");
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
//if (srvVersion >= Ver.ver(15,7,0, 130))
//{
//	TotalPIO		     = "TotalPIO,         ";
//	TotalLIO             = "TotalLIO,         ";
//	TotalCpuTime         = "TotalCpuTime,     ";
//	TotalElapsedTime     = "TotalElapsedTime, ";
//
//	TotalPioDiff		 = "TotalPioDiff         = TotalPIO, ";         // DIFF COUNTER
//	TotalLioDiff         = "TotalLioDiff         = TotalLIO, ";         // DIFF COUNTER
//	TotalCpuTimeDiff     = "TotalCpuTimeDiff     = TotalCpuTime, ";     // DIFF COUNTER
//	TotalElapsedTimeDiff = "TotalElapsedTimeDiff = TotalElapsedTime, "; // DIFF COUNTER
//}

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
		
		// Create Column selects, but only if the column exists in the PCS Table
		String col_UseCount_max             = !dummyRstm.hasColumnNoCase("UseCount"            ) ? "" : " ,max([UseCount])                 as [UseCount_max] \n"; 
		String col_UseCountDiff_sum         = !dummyRstm.hasColumnNoCase("UseCountDiff"        ) ? "" : " ,sum([UseCountDiff])             as [UseCountDiff_sum] \n"; 

		String col_TotalElapsedTimeDiff_sum = !dummyRstm.hasColumnNoCase("TotalElapsedTimeDiff"  ) ? "" : " ,sum([TotalElapsedTimeDiff])   as [TotalElapsedTimeDiff_sum] \n"; 
		String col_TotalCpuTimeDiff_sum     = !dummyRstm.hasColumnNoCase("TotalCpuTimeDiff"      ) ? "" : " ,sum([TotalCpuTimeDiff])       as [TotalCpuTimeDiff_sum] \n"; 
		String col_TotalLioDiff_sum         = !dummyRstm.hasColumnNoCase("TotalLioDiff"          ) ? "" : " ,sum([TotalLioDiff])           as [TotalLioDiff_sum] \n"; 
		String col_TotalPioDiff_sum         = !dummyRstm.hasColumnNoCase("TotalPioDiff"          ) ? "" : " ,sum([TotalPioDiff])           as [TotalPioDiff_sum] \n"; 

		String col_AvgCpuTime_est_max       = !dummyRstm.hasColumnNoCase("AvgCpuTime"            ) ? "" : " ,max([AvgCpuTime]*[UseCount])  as [AvgCpuTime_est_max] \n"; 
		
//		String col_AvgElapsedTime           = !dummyRstm.hasColumnNoCase("TotalElapsedTimeDiff"  ) ? "" : " ,-1.0                          as [AvgElapsedTime] \n";
//		String col_AvgCpuTime               = !dummyRstm.hasColumnNoCase("TotalCpuTimeDiff"      ) ? "" : " ,-1.0                          as [AvgCpuTime] \n";
//		String col_AvgLIO                   = !dummyRstm.hasColumnNoCase("TotalLioDiff"          ) ? "" : " ,-1.0                          as [AvgLIO] \n";
//		String col_AvgPIO                   = !dummyRstm.hasColumnNoCase("TotalPioDiff"          ) ? "" : " ,-1.0                          as [AvgPIO] \n";
		String col_AvgElapsedTime           = !dummyRstm.hasColumnNoCase("TotalElapsedTimeDiff"  ) ? " ,max([AvgElapsedTime]) as [AvgElapsedTime_max] \n" : " ,-1.0 as [AvgElapsedTime] \n";
		String col_AvgCpuTime               = !dummyRstm.hasColumnNoCase("TotalCpuTimeDiff"      ) ? " ,max([AvgCpuTime])     as [AvgCpuTime_max]     \n" : " ,-1.0 as [AvgCpuTime] \n";
		String col_AvgLIO                   = !dummyRstm.hasColumnNoCase("TotalLioDiff"          ) ? " ,max([AvgLIO])         as [AvgLIO_max]         \n" : " ,-1.0 as [AvgLIO] \n";
		String col_AvgPIO                   = !dummyRstm.hasColumnNoCase("TotalPioDiff"          ) ? " ,max([AvgPIO])         as [AvgPIO_max]         \n" : " ,-1.0 as [AvgPIO] \n";
		
		String col_AvgScanRows              = !dummyRstm.hasColumnNoCase("AvgScanRows"           ) ? "" : " ,max([AvgScanRows])            as [AvgScanRows_max] \n";
		String col_AvgQualifyingReadRows    = !dummyRstm.hasColumnNoCase("AvgQualifyingReadRows" ) ? "" : " ,max([AvgQualifyingReadRows])  as [AvgQualifyingReadRows_max] \n";
		String col_AvgQualifyingWriteRows   = !dummyRstm.hasColumnNoCase("AvgQualifyingWriteRows") ? "" : " ,max([AvgQualifyingWriteRows]) as [AvgQualifyingWriteRows_max] \n";
		
		String col_LockWaitsDiff_sum        = !dummyRstm.hasColumnNoCase("LockWaitsDiff"         ) ? "" : " ,sum([LockWaitsDiff])          as [LockWaitsDiff_sum] \n"; 
		String col_LockWaitTimeDiff_sum     = !dummyRstm.hasColumnNoCase("LockWaitTimeDiff"      ) ? "" : " ,sum([LockWaitTimeDiff])       as [LockWaitTimeDiff_sum] \n"; 
		
		String col_MaxSortTime_max          = !dummyRstm.hasColumnNoCase("MaxSortTime"           ) ? "" : " ,max([MaxSortTime])            as [MaxSortTime_max] \n"; 
		String col_SortSpilledCount_sum     = !dummyRstm.hasColumnNoCase("SortSpilledCount"      ) ? "" : " ,max([SortSpilledCount])       as [SortSpilledCount_sum] \n"; 
		String col_SortCountDiff_sum        = !dummyRstm.hasColumnNoCase("SortCountDiff"         ) ? "" : " ,max([SortCountDiff])          as [SortCountDiff_sum] \n"; 
		String col_TotalSortTimeDiff_sum    = !dummyRstm.hasColumnNoCase("TotalSortTimeDiff"     ) ? "" : " ,max([TotalSortTimeDiff])      as [TotalSortTimeDiff_sum] \n"; 

//		String extraWhereClause = "where [UseCount] > 100 or [AvgLIO] > 100000\n"; // this is only used if we havn't got "TotalLioDiff" or "TotalCpuTimeDiff"
		String extraWhereClause = "";
		String orderByCol = "[samples_count]";
		if (dummyRstm.hasColumnNoCase("UseCountDiff"))     { orderByCol = "[UseCountDiff_sum]";     extraWhereClause = "  and [UseCountDiff] > 0 \n"; }
		if (dummyRstm.hasColumnNoCase("AvgCpuTime"))       { orderByCol = "[AvgCpuTime_est_max]";   }
		if (dummyRstm.hasColumnNoCase("TotalLioDiff"))     { orderByCol = "[TotalLioDiff_sum]";     }
		if (dummyRstm.hasColumnNoCase("TotalCpuTimeDiff")) { orderByCol = "[TotalCpuTimeDiff_sum]"; }

		String whereFilter_skipNewDiffRateRows = !skipNewDiffRateRows ? "" : "  and [CmNewDiffRateRow] = 0 -- only records that has been diff calculations (not first time seen, some ASE Versions has a bug that do not clear counters on reuse) \n";

		String sql = getCmDiffColumnsAsSqlComment("CmStmntCacheDetails")
			    + "select top " + topRows + " \n"
			    + "  [DBName] \n"
			    + " ,[Hashkey] \n"
				+ " ,cast('' as varchar(10))     as [txt] \n"
//			    + " ,[ObjectName] \n"
			    + " ,min([ObjectName])           as [ObjectName] \n"
			    + " ,count(*)                    as [samples_count] \n"
			    + " ,min([SessionSampleTime])    as [SessionSampleTime_min] \n"
			    + " ,max([SessionSampleTime])    as [SessionSampleTime_max] \n"
			    + " ,cast('' as varchar(30))     as [Duration] \n"
			    + " ,sum([CmNewDiffRateRow])     as [newDiffRow_sum] \n"
			    + " ,sum([CmSampleMs])           as [CmSampleMs_sum] \n"
			    + " \n"
			    + col_UseCount_max
			    + col_UseCountDiff_sum
			    + (StringUtil.hasValue(col_UseCountDiff_sum) ? " ,cast('' as varchar(512)) as [UseCountDiff__chart] \n" : "")
			    + " \n"
			    + col_TotalElapsedTimeDiff_sum
			    + (StringUtil.hasValue(col_TotalCpuTimeDiff_sum) ? " ,cast('' as varchar(512)) as [TotalCpuTimeDiff__chart] \n" : "")
			    + col_TotalCpuTimeDiff_sum
			    + col_TotalLioDiff_sum
			    + col_TotalPioDiff_sum
			    + col_AvgCpuTime_est_max
			    + " \n"
			    + col_AvgElapsedTime
			    + col_AvgCpuTime
			    + col_AvgLIO
			    + col_AvgPIO
			    + " \n"
			    + col_LockWaitsDiff_sum
			    + col_LockWaitTimeDiff_sum
			    + " \n"
			    + col_MaxSortTime_max
			    + col_SortSpilledCount_sum
			    + col_SortCountDiff_sum
			    + col_TotalSortTimeDiff_sum
			    + " \n"
			    + col_AvgScanRows
			    + col_AvgQualifyingReadRows
			    + col_AvgQualifyingWriteRows
			    + " \n"
			    + "from [CmStmntCacheDetails_diff] \n"
			    + "where 1 = 1 \n"
				+ getReportPeriodSqlWhere()
			    + extraWhereClause
			    + whereFilter_skipNewDiffRateRows
//maybe we can use SqlHash here instead... note: we need to change at the top of the select statement as well [ObjectName] ->> [Hashkey]
//			    + "group by [DBName], [ObjectName] \n"
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
			// Describe the table
			setSectionDescription(_shortRstm);

			setDurationColumn(_shortRstm, "SessionSampleTime_min", "SessionSampleTime_max", "Duration");
			
			// Do some calculations (which was hard to do in a PORTABLE SQL Way)
			int pos_UseCountDiff_sum         = _shortRstm.findColumn("UseCountDiff_sum");
			
			int pos_TotalElapsedTimeDiff_sum = _shortRstm.findColumn("TotalElapsedTimeDiff_sum");
			int pos_TotalCpuTimeDiff_sum     = _shortRstm.findColumn("TotalCpuTimeDiff_sum");
			int pos_TotalLioDiff_sum         = _shortRstm.findColumn("TotalLioDiff_sum");
			int pos_TotalPioDiff_sum         = _shortRstm.findColumn("TotalPioDiff_sum");

			int pos_AvgElapsedTime           = _shortRstm.findColumn("AvgElapsedTime");
			int pos_AvgCpuTime               = _shortRstm.findColumn("AvgCpuTime");
			int pos_AvgLIO                   = _shortRstm.findColumn("AvgLIO");
			int pos_AvgPIO                   = _shortRstm.findColumn("AvgPIO");

			if (    pos_UseCountDiff_sum >= 0 
			     && pos_TotalElapsedTimeDiff_sum >= 0 && pos_TotalCpuTimeDiff_sum >= 0 && pos_TotalLioDiff_sum >= 0 && pos_TotalPioDiff_sum >= 0
			     && pos_AvgElapsedTime           >= 0 && pos_AvgCpuTime           >= 0 && pos_AvgLIO           >= 0 && pos_AvgPIO           >= 0
			   )
			{
				for (int r=0; r<_shortRstm.getRowCount(); r++)
				{
					long UseCountDiff_sum = _shortRstm.getValueAsLong(r, pos_UseCountDiff_sum);
					
					long TotalElapsedTimeDiff_sum = _shortRstm.getValueAsLong(r, pos_TotalElapsedTimeDiff_sum);
					long TotalCpuTimeDiff_sum     = _shortRstm.getValueAsLong(r, pos_TotalCpuTimeDiff_sum);
					long TotalLioDiff_sum         = _shortRstm.getValueAsLong(r, pos_TotalLioDiff_sum);
					long TotalPioDiff_sum         = _shortRstm.getValueAsLong(r, pos_TotalPioDiff_sum);

					BigDecimal calc1;
					BigDecimal calc2;
					BigDecimal calc3;
					BigDecimal calc4;

					if (UseCountDiff_sum > 0)
					{
						calc1 = new BigDecimal( (TotalElapsedTimeDiff_sum*1.0) / (UseCountDiff_sum*1.0) ).setScale(2, RoundingMode.HALF_EVEN);
						calc2 = new BigDecimal( (TotalCpuTimeDiff_sum    *1.0) / (UseCountDiff_sum*1.0) ).setScale(2, RoundingMode.HALF_EVEN);
						calc3 = new BigDecimal( (TotalLioDiff_sum        *1.0) / (UseCountDiff_sum*1.0) ).setScale(2, RoundingMode.HALF_EVEN);
						calc4 = new BigDecimal( (TotalPioDiff_sum        *1.0) / (UseCountDiff_sum*1.0) ).setScale(2, RoundingMode.HALF_EVEN);
					}
					else
					{
						calc1 = new BigDecimal(-1);
						calc2 = new BigDecimal(-1);
						calc3 = new BigDecimal(-1);
						calc4 = new BigDecimal(-1);
					}
					
					_shortRstm.setValueAtWithOverride(calc1, r, pos_AvgElapsedTime);
					_shortRstm.setValueAtWithOverride(calc2, r, pos_AvgCpuTime);
					_shortRstm.setValueAtWithOverride(calc3, r, pos_AvgLIO);
					_shortRstm.setValueAtWithOverride(calc4, r, pos_AvgPIO);
				}
			}

			// Mini Chart on "CPU Time"
			if (_shortRstm.hasColumn("TotalCpuTimeDiff_sum") && _shortRstm.hasColumn("TotalCpuTimeDiff__chart"))
			{
				_miniChartJsList.add(SparklineHelper.createSparkline(conn, this, _shortRstm, 
						SparkLineParams.create       (DataSource.CounterModel)
						.setHtmlChartColumnName      ("TotalCpuTimeDiff__chart")
						.setHtmlWhereKeyColumnName   ("DBName, Hashkey")
						.setDbmsTableName            ("CmStmntCacheDetails_diff")
						.setDbmsSampleTimeColumnName ("SessionSampleTime")
						.setDbmsDataValueColumnName  ("TotalElapsedTimeDiff")   
						.setDbmsWhereKeyColumnName   ("DBName, Hashkey")
						.setDbmsExtraWhereClause     (whereFilter_skipNewDiffRateRows)
//						.setSparklineTooltipPostfix  ("SUM 'CPU Time in ms' at below period")
						.validate()));
			}

			// Mini Chart on "UseCountDiff"
			if (_shortRstm.hasColumn("UseCountDiff_sum") && _shortRstm.hasColumn("UseCountDiff__chart"))
			{
				_miniChartJsList.add(SparklineHelper.createSparkline(conn, this, _shortRstm, 
						SparkLineParams.create       (DataSource.CounterModel)
						.setHtmlChartColumnName      ("UseCountDiff__chart")
						.setHtmlWhereKeyColumnName   ("DBName, Hashkey")
						.setDbmsTableName            ("CmStmntCacheDetails_diff")
						.setDbmsSampleTimeColumnName ("SessionSampleTime")
						.setDbmsDataValueColumnName  ("UseCountDiff")   
						.setDbmsWhereKeyColumnName   ("DBName, Hashkey")
						.setDbmsExtraWhereClause     (whereFilter_skipNewDiffRateRows)
//						.setSparklineTooltipPostfix  ("SUM 'Execution Count' in below period")
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


			Set<String> stmntCacheObjects = getStatementCacheObjects(_shortRstm, "ObjectName");
			if (stmntCacheObjects != null && ! stmntCacheObjects.isEmpty() )
			{
				try 
				{
					// Get full XML Query Plan
					_planMap = getXmlShowplanMapFromMonDdlStorage(conn, stmntCacheObjects);

					// Get SQL Text 
					_ssqlRstm = getSqlStatementsFromMonDdlStorage(conn, stmntCacheObjects);

					// Add CpuTime
					_ssqlRstm.addColumn("CpuTime", 1, Types.VARCHAR, "varchar", "varchar(512)", 512, 0, "-", String.class);

					// Add "CopyXmlPlan"
					_ssqlRstm.addColumn("XmlPlan", 1, Types.LONGVARCHAR, "text", "text", 512, 0, "-not-available-", String.class);

					// Mini Chart on "CPU Time"
					// COPY Cell data from the "details" table
					_ssqlRstm.copyCellContentFrom(_shortRstm, "ObjectName", "TotalCpuTimeDiff__chart",   "objectName", "CpuTime");
				}
				catch (SQLException ex)
				{
					setProblemException(ex);
				}

				// Set the "html link0" for "Copy XML Plan"
				if (_planMap != null && !_planMap.isEmpty())
				{
					int pos_objectName  = _ssqlRstm.findColumn("objectName");
					int pos_CopyXmlPlan = _ssqlRstm.findColumn("XmlPlan");

					if (pos_objectName != -1 && pos_CopyXmlPlan != -1)
					{
						for (int r=0; r<_ssqlRstm.getRowCount(); r++)
						{
							String ObjectName = _ssqlRstm.getValueAsString(r, pos_objectName);

							if (ObjectName != null && (ObjectName.trim().startsWith("*ss") || ObjectName.trim().startsWith("*sq")) )
							{
								if (_planMap.containsKey(ObjectName))
								{
									String planHandle = ObjectName.replace('*', '_');
									String newCellContent = "<a href='#showplan-list' title='Copy plan to clipboard... then you can copy it into SqlW to view the GUI Plan!' onclick='showplanForId(\"" + planHandle + "\"); return true;'>Copy XML</a>";

									_ssqlRstm.setValueAtWithOverride(newCellContent, r, pos_CopyXmlPlan);
								}
							}
						}
					}
				} // end: Set the "html link0" for "Copy XML Plan"
			}
		}
	}
}
