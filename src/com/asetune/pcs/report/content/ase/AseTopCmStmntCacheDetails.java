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

	@Override
	public void create(DbxConnection conn, String srvName, Configuration conf)
	{
		int topRows = conf.getIntProperty(this.getClass().getSimpleName()+".top", 20);

		String sql = ""
			    + "select top " + topRows + " \n"
			    + "	 [DBName] \n"
			    + "	,[ObjectName] \n"
			    + "	,count(*)                    as [samples_count] \n"
			    + "	,min([SessionSampleTime])    as [SessionSampleTime_min] \n"
			    + "	,max([SessionSampleTime])    as [SessionSampleTime_max] \n"
			    + "	,sum([CmSampleMs])           as [CmSampleMs_sum] \n"
			    + " \n"
			    + "	,sum([UseCountDiff])         as [UseCountDiff_sum] \n"
			    + " \n"
			    + "	,sum([TotalElapsedTimeDiff]) as [TotalElapsedTimeDiff_sum] \n"
			    + "	,sum([TotalCpuTimeDiff])     as [TotalCpuTimeDiff_sum] \n"
			    + "	,sum([TotalLioDiff])         as [TotalLioDiff_sum] \n"
			    + "	,sum([TotalPioDiff])         as [TotalPioDiff_sum] \n"
			    + " \n"
			    + "	,-1.0                         as [AvgElapsedTime] \n"
			    + "	,-1.0                         as [AvgCpuTime] \n"
			    + "	,-1.0                         as [AvgLIO] \n"
			    + "	,-1.0                         as [AvgPIO] \n"
			    + " \n"
			    + "	,sum([LockWaitsDiff])        as [LockWaitsDiff_sum] \n"
			    + "	,sum([LockWaitTimeDiff])     as [LockWaitTimeDiff_sum] \n"
			    + " \n"
			    + "	,max([MaxSortTime])          as [MaxSortTime_max] \n"
			    + "	,sum([SortSpilledCount])     as [SortSpilledCount_sum] \n"
			    + "	,sum([SortCountDiff])        as [SortCountDiff_sum] \n"
			    + "	,sum([TotalSortTimeDiff])    as [TotalSortTimeDiff_sum] \n"
			    + " \n"
			    + "from [CmStmntCacheDetails_diff] \n"
			    + "group by [DBName], [ObjectName] \n"
			    + " \n"
			    + "order by [TotalCpuTimeDiff_sum] desc \n"
			    + "";

		_shortRstm = executeQuery(conn, sql, false, "TopStatementCacheCalls");
		if (_shortRstm == null)
		{
			_shortRstm = ResultSetTableModel.createEmpty("TopStatementCacheCalls");
			return;
		}
		else
		{
			
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
		
//		Set<String> stmntCacheObjects = null;
//		
//		sql = conn.quotifySqlString(sql);
//		try ( Statement stmnt = conn.createStatement() )
//		{
//			// Unlimited execution time
//			stmnt.setQueryTimeout(0);
//			try ( ResultSet rs = stmnt.executeQuery(sql) )
//			{
////				_shortRstm = new ResultSetTableModel(rs, "TopStatementCacheCalls");
//				_shortRstm = createResultSetTableModel(rs, "TopStatementCacheCalls");
//
//				// Do some calculations (which was hard to do in a PORTABLE SQL Way)
//				int pos_UseCountDiff_sum         = _shortRstm.findColumn("UseCountDiff_sum");
//				
//				int pos_TotalElapsedTimeDiff_sum = _shortRstm.findColumn("TotalElapsedTimeDiff_sum");
//				int pos_TotalCpuTimeDiff_sum     = _shortRstm.findColumn("TotalCpuTimeDiff_sum");
//				int pos_TotalLioDiff_sum         = _shortRstm.findColumn("TotalLioDiff_sum");
//				int pos_TotalPioDiff_sum         = _shortRstm.findColumn("TotalPioDiff_sum");
//
//				int pos_AvgElapsedTime           = _shortRstm.findColumn("AvgElapsedTime");
//				int pos_AvgCpuTime               = _shortRstm.findColumn("AvgCpuTime");
//				int pos_AvgLIO                   = _shortRstm.findColumn("AvgLIO");
//				int pos_AvgPIO                   = _shortRstm.findColumn("AvgPIO");
//
//				if (    pos_UseCountDiff_sum >= 0 
//				     && pos_TotalElapsedTimeDiff_sum >= 0 && pos_TotalCpuTimeDiff_sum >= 0 && pos_TotalLioDiff_sum >= 0 && pos_TotalPioDiff_sum >= 0
//				     && pos_AvgElapsedTime           >= 0 && pos_AvgCpuTime           >= 0 && pos_AvgLIO           >= 0 && pos_AvgPIO           >= 0
//				   )
//				{
//					for (int r=0; r<_shortRstm.getRowCount(); r++)
//					{
//						long UseCountDiff_sum = _shortRstm.getValueAsLong(r, pos_UseCountDiff_sum);
//						
//						long TotalElapsedTimeDiff_sum = _shortRstm.getValueAsLong(r, pos_TotalElapsedTimeDiff_sum);
//						long TotalCpuTimeDiff_sum     = _shortRstm.getValueAsLong(r, pos_TotalCpuTimeDiff_sum);
//						long TotalLioDiff_sum         = _shortRstm.getValueAsLong(r, pos_TotalLioDiff_sum);
//						long TotalPioDiff_sum         = _shortRstm.getValueAsLong(r, pos_TotalPioDiff_sum);
//
//						BigDecimal calc1;
//						BigDecimal calc2;
//						BigDecimal calc3;
//						BigDecimal calc4;
//
//						if (UseCountDiff_sum > 0)
//						{
//							calc1 = new BigDecimal( (TotalElapsedTimeDiff_sum*1.0) / (UseCountDiff_sum*1.0) ).setScale(2, RoundingMode.HALF_EVEN);
//							calc2 = new BigDecimal( (TotalCpuTimeDiff_sum    *1.0) / (UseCountDiff_sum*1.0) ).setScale(2, RoundingMode.HALF_EVEN);
//							calc3 = new BigDecimal( (TotalLioDiff_sum        *1.0) / (UseCountDiff_sum*1.0) ).setScale(2, RoundingMode.HALF_EVEN);
//							calc4 = new BigDecimal( (TotalPioDiff_sum        *1.0) / (UseCountDiff_sum*1.0) ).setScale(2, RoundingMode.HALF_EVEN);
//						}
//						else
//						{
//							calc1 = new BigDecimal(-1);
//							calc2 = new BigDecimal(-1);
//							calc3 = new BigDecimal(-1);
//							calc4 = new BigDecimal(-1);
//						}
//						
//						_shortRstm.setValueAtWithOverride(calc1, r, pos_AvgElapsedTime);
//						_shortRstm.setValueAtWithOverride(calc2, r, pos_AvgCpuTime);
//						_shortRstm.setValueAtWithOverride(calc3, r, pos_AvgLIO);
//						_shortRstm.setValueAtWithOverride(calc4, r, pos_AvgPIO);
//					}
//				}
//
//				
//				stmntCacheObjects = getStatementCacheObjects(_shortRstm, "ObjectName");
//				
//				if (_logger.isDebugEnabled())
//					_logger.debug("_shortRstm.getRowCount()="+ _shortRstm.getRowCount());
//			}
//		}
//		catch(SQLException ex)
//		{
//			_problem = ex;
//
//			_shortRstm = ResultSetTableModel.createEmpty("TopStatementCacheCalls");
//			_logger.warn("Problems getting TopStatementCacheCalls: " + ex);
//		}
//		
//		
//		if (stmntCacheObjects != null && ! stmntCacheObjects.isEmpty() )
//		{
//			try 
//			{
//				_ssqlRstm = getSqlStatementsFromMonDdlStorage(conn, stmntCacheObjects);
//			}
//			catch (SQLException ex)
//			{
//				_problem = ex;
//			}
//		}
	}

}
