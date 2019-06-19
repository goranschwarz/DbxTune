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

import java.sql.SQLException;
import java.util.Set;

import com.asetune.gui.ResultSetTableModel;
import com.asetune.pcs.report.DailySummaryReportAbstract;
import com.asetune.sql.conn.DbxConnection;
import com.asetune.utils.Configuration;

public class AseTopCmCachedProcs extends AseAbstract
{
//	private static Logger _logger = Logger.getLogger(AseTopCmCachedProcs.class);

	private ResultSetTableModel _shortRstm;
	private ResultSetTableModel _ssqlRstm;
//	private ResultSetTableModel _cmCachedProcsMin;
//	private ResultSetTableModel _cmCachedProcsMax;
//	private Exception           _problem = null;

	public AseTopCmCachedProcs(DailySummaryReportAbstract reportingInstance)
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
		return "Top Procedure Calls (order by: CPUTime_sum, origin: CmCachedProcs / monCachedProcedures)";
	}

	@Override
	public boolean hasIssueToReport()
	{
		return false; // even if we found entries, do NOT indicate this as a Problem or Issue
	}


	@Override
	public void create(DbxConnection conn, String srvName, Configuration conf)
	{
		createTopCmCachedProcs(conn, srvName, conf);
	}
	
	private void createTopCmCachedProcs(DbxConnection conn, String srvName, Configuration conf)
	{
		int topRows = conf.getIntProperty(this.getClass().getSimpleName()+".top", 20);

		String sql = ""
			    + "select top " + topRows + " \n"
			    + "	 [DBName] \n"
			    + "	,[ObjectName] \n"
			    + "	,count(distinct [PlanID]) as [PlanID_count] \n"
			    + "	,count(*)                 as [samples_count] \n"
			    + "	,min([SessionSampleTime]) as [SessionSampleTime_min] \n"
			    + "	,max([SessionSampleTime]) as [SessionSampleTime_max] \n"
			    + "	,sum([CmSampleMs])        as [CmSampleMs_sum] \n"
			    + "	,min([CompileDate])       as [CompileDate_min] \n"
			    + "	,max([CompileDate])       as [CompileDate_max] \n"
			    + " \n"
			    + "	,max([MemUsageKB])        as [MemUsageKB_max] \n"
			    + "	,sum([RequestCntDiff])    as [RequestCntDiff_sum] \n"
			    + "	,sum([TempdbRemapCnt])    as [TempdbRemapCnt_sum] \n"
			    + "	,sum([ExecutionCount])    as [ExecutionCount_sum] \n"
			    + " \n"
			    + "	,sum([CPUTime])           as [CPUTime_sum] \n"
			    + "	,sum([ExecutionTime])     as [ExecutionTime_sum] \n"
			    + "	,sum([PhysicalReads])     as [PhysicalReads_sum] \n"
			    + "	,sum([LogicalReads])      as [LogicalReads_sum] \n"
			    + "	,sum([PhysicalWrites])    as [PhysicalWrites_sum] \n"
			    + "	,sum([PagesWritten])      as [PagesWritten_sum] \n"
			    + " \n"
			    + "	,max([AvgCPUTime])        as [AvgCPUTime_max] \n"
			    + "	,max([AvgExecutionTime])  as [AvgExecutionTime_max] \n"
			    + "	,max([AvgPhysicalReads])  as [AvgPhysicalReads_max] \n"
			    + "	,max([AvgLogicalReads])   as [AvgLogicalReads_max] \n"
			    + "	,max([AvgPhysicalWrites]) as [AvgPhysicalWrites_max] \n"
			    + "	,max([AvgPagesWritten])   as [AvgPagesWritten_max] \n"
			    + " \n"
			    + "from [CmCachedProcs_diff] \n"
			    + "group by [DBName], [ObjectName] \n"
			    + " \n"
			    + "order by [CPUTime_sum] desc \n"
			    + "";

		_shortRstm = executeQuery(conn, sql, false, "TopCachedProcedureCalls");
		if (_shortRstm == null)
		{
			_shortRstm = ResultSetTableModel.createEmpty("TopCachedProcedureCalls");
			return;
		}
		else
		{
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
////				_shortRstm = new ResultSetTableModel(rs, "TopCachedProcedureCalls");
//				_shortRstm = createResultSetTableModel(rs, "TopCachedProcedureCalls");
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
//			_shortRstm = ResultSetTableModel.createEmpty("TopCachedProcedureCalls");
//			_logger.warn("Problems getting Top Cached Procedure Calls: " + ex);
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


	

//	private void createTopCmCachedProcs(DbxConnection conn)
//	{
//		String sql;
//		String tabName = "CmCachedProcs_abs";
//		// FIXME: also get the CmCachedProcs point of view
//		//        possibly: get FIRST and LAST entry and DIFF them...
//		
//		/*
//		select * from [CmCachedProcs_abs] where [CmSampleTime] = (select min([CmSampleTime]) from [CmCachedProcs_abs]) order by [LogicalReads] desc;
//		select * from [CmCachedProcs_abs] where [CmSampleTime] = (select max([CmSampleTime]) from [CmCachedProcs_abs]) order by [LogicalReads] desc;
//		 */
//		
//		CountersModel cm = CounterController.getInstance().getCmByName("CmCachedProcs");
//		if (cm == null)
//		{
////			//ICounterController cc = new CounterControllerTmp();
////			
////			cm = new CmCachedProcs(null, null);
////			CounterModelForLoad
//		}
//		List<String> diffColumns = Arrays.asList(CmCachedProcs.DIFF_COLUMNS);
//		List<String> pkCols      = cm.getPkForVersion(conn, 0, false);
//		
//		String sqldummy = "select * from [" + tabName + "] where 1=2";
//		sql = sqldummy;
//		List<String> colNames = new ArrayList<>();
//		try ( Statement stmnt = conn.createStatement(); ResultSet rs = stmnt.executeQuery(sql); )
//		{
//			ResultSetTableModel rstm = new ResultSetTableModel(rs, "getColNames");
//
//			colNames = new ArrayList<>(rstm.getColumnNames());
//			colNames.remove("SessionStartTime");
//			colNames.remove("SessionSampleTime");
//			colNames.remove("CmSampleTime");
//			colNames.remove("CmSampleMs");
//			colNames.remove("CmNewDiffRateRow");
//		}
//		catch(SQLException ex)
//		{
//			_problem = ex;
//		}
//
//		String sqlMin = "select " + StringUtil.toCommaStrQuoted("[", "]", colNames) + " from [" + tabName + "] where [SessionSampleTime] = (select min([SessionSampleTime]) from [" + tabName + "])"; 
//		String sqlMax = "select " + StringUtil.toCommaStrQuoted("[", "]", colNames) + " from [" + tabName + "] where [SessionSampleTime] = (select max([SessionSampleTime]) from [" + tabName + "])"; 
//		
////		new CounterSample("name", true, diffColumns, null);
//		
//		// get MIN
//		sql = conn.quotifySqlString(sqlMin);
//		try ( Statement stmnt = conn.createStatement(); ResultSet rs = stmnt.executeQuery(sql); )
//		{
//			_cmCachedProcsMin = new ResultSetTableModel(rs, "CmCachedProcs_abs_MIN");
//
//			if (_logger.isDebugEnabled())
//				_logger.debug("_cmCachedProcsMin.getRowCount()="+ _cmCachedProcsMin.getRowCount());
//		}
//		catch(SQLException ex)
//		{
//			_problem = ex;
//
//			_cmCachedProcsMin = ResultSetTableModel.createEmpty("CmCachedProcs_abs_MIN");
//			_logger.warn("Problems getting CmCachedProcs_abs_MIN: " + ex);
//		}
//
//		// get MAX
//		sql = conn.quotifySqlString(sqlMax);
//		try ( Statement stmnt = conn.createStatement(); ResultSet rs = stmnt.executeQuery(sql); )
//		{
//			_cmCachedProcsMax = new ResultSetTableModel(rs, "CmCachedProcs_abs_MAX");
//
//			if (_logger.isDebugEnabled())
//				_logger.debug("_cmCachedProcsMax.getRowCount()="+ _cmCachedProcsMax.getRowCount());
//		}
//		catch(SQLException ex)
//		{
//			_problem = ex;
//
//			_cmCachedProcsMax = ResultSetTableModel.createEmpty("CmCachedProcs_abs_MAX");
//			_logger.warn("Problems getting CmCachedProcs_abs_MAX: " + ex);
//		}
//
//		// Calculate the difference...
//		if (_cmCachedProcsMin != null && _cmCachedProcsMax != null)
//		{
//			//FIXME
//		}
//	}


}
