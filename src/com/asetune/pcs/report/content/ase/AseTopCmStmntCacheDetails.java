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
package com.asetune.pcs.report.content.ase;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.sql.SQLException;
import java.util.Set;

import com.asetune.gui.ResultSetTableModel;
import com.asetune.pcs.report.DailySummaryReportAbstract;
import com.asetune.sql.conn.DbxConnection;
import com.asetune.utils.Configuration;

public class AseTopCmStmntCacheDetails extends AseAbstract
{
//	private static Logger _logger = Logger.getLogger(AseTopCmStmntCacheDetails.class);

	private ResultSetTableModel _shortRstm;
	private ResultSetTableModel _ssqlRstm;
//	private Exception           _problem = null;

	public AseTopCmStmntCacheDetails(DailySummaryReportAbstract reportingInstance)
	{
		super(reportingInstance);
	}

	@Override
	public String getMsgAsText()
	{
		StringBuilder sb = new StringBuilder();

		if (_shortRstm.getRowCount() == 0)
		{
			sb.append("No rows found \n");
		}
		else
		{
			sb.append(getSubject() + " Count: ").append(_shortRstm.getRowCount()).append("\n");
			sb.append(_shortRstm.toAsciiTableString());

			if (_ssqlRstm != null)
			{
				sb.append("Statement Cache Entries Count: ").append(_ssqlRstm.getRowCount()).append("\n");
				sb.append(_ssqlRstm.toAsciiTablesVerticalString());
			}
		}

		if (hasProblem())
			sb.append(getProblem());
		
		return sb.toString();
	}

	@Override
	public String getMsgAsHtml()
	{
		StringBuilder sb = new StringBuilder();

		if (_shortRstm.getRowCount() == 0)
		{
			sb.append("No rows found <br>\n");
		}
		else
		{
			// Get a description of this section, and column names
			sb.append(getSectionDescriptionHtml(_shortRstm, true));

			sb.append("Row Count: ").append(_shortRstm.getRowCount()).append("<br>\n");
			sb.append(_shortRstm.toHtmlTableString("sortable"));

			if (_ssqlRstm != null)
			{
				sb.append("Statement Cache Entries Count: ").append(_ssqlRstm.getRowCount()).append("<br>\n");
//				sb.append(_ssqlRstm.toHtmlTablesVerticalString("sortable"));
				sb.append(_ssqlRstm.toHtmlTableString("sortable"));
			}
		}

		if (hasProblem())
			sb.append("<pre>").append(getProblem()).append("</pre> \n");

		sb.append("\n<br>");

		return sb.toString();
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
	public void create(DbxConnection conn, String srvName, Configuration conf)
	{
		int topRows = conf.getIntProperty(this.getClass().getSimpleName()+".top", 20);

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

//		String whereClause = "where [UseCount] > 100 or [AvgLIO] > 100000\n"; // this is only used if we havn't got "TotalLioDiff" or "TotalCpuTimeDiff"
		String whereClause = "";
		String orderByCol = "[samples_count]";
		if (dummyRstm.hasColumnNoCase("UseCountDiff"))     { orderByCol = "[UseCountDiff_sum]";     }
		if (dummyRstm.hasColumnNoCase("AvgCpuTime"))       { orderByCol = "[AvgCpuTime_est_max]";   }
		if (dummyRstm.hasColumnNoCase("TotalLioDiff"))     { orderByCol = "[TotalLioDiff_sum]";     whereClause = ""; }
		if (dummyRstm.hasColumnNoCase("TotalCpuTimeDiff")) { orderByCol = "[TotalCpuTimeDiff_sum]"; whereClause = ""; }

		String sql = getCmDiffColumnsAsSqlComment("CmStmntCacheDetails")
			    + "select top " + topRows + " \n"
			    + "  [DBName] \n"
			    + " ,[ObjectName] \n"
			    + " ,count(*)                    as [samples_count] \n"
			    + " ,min([SessionSampleTime])    as [SessionSampleTime_min] \n"
			    + " ,max([SessionSampleTime])    as [SessionSampleTime_max] \n"
			    + " ,cast('' as varchar(30))     as [Duration] \n"
			    + " ,sum([CmSampleMs])           as [CmSampleMs_sum] \n"
			    + " \n"
			    + col_UseCount_max
			    + col_UseCountDiff_sum
			    + " \n"
			    + col_TotalElapsedTimeDiff_sum
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
			    + whereClause
			    + "group by [DBName], [ObjectName] \n"
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

			
			Set<String> stmntCacheObjects = getStatementCacheObjects(_shortRstm, "ObjectName");
			if (stmntCacheObjects != null && ! stmntCacheObjects.isEmpty() )
			{
				try 
				{
					_ssqlRstm = getSqlStatementsFromMonDdlStorage(conn, stmntCacheObjects);
				}
				catch (SQLException ex)
				{
					setProblem(ex);
				}
			}
		}
	}
}
