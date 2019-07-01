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
	
	/**
	 * Set descriptions for the table, and the columns
	 */
	private void setSectionDescription(ResultSetTableModel rstm)
	{
		if (rstm == null)
			return;
		
		// Section description
		rstm.setDescription(
				"Both slow and fast SQL Statements are presented here (ordered by: CPUTime_sum) <br>" +
				"<br>" +
				"This so you can see if there are problems with <i>procedures</i> that falls <i>just below</i> the threshold for 'Slow SQL Statements' <br>" +
				"<br>" +
				"ASE Source table is 'master.dbo.monCachedProcedures' where both StoredProcedures, StatementCache and DynamicSql are displayed. <br>" +
				"PCS Source table is 'CmCachedProcs_diff'. (PCS = Persistent Counter Store) <br>" +
				"<br>" +
				"The report <i>summarizes</i> (min/max/count/sum/avg) all entries/samples from the <i>source_DIFF</i> table. <br>" +
				"Typically the column name <i>postfix</i> will tell you what aggregate function was used. <br>" +
				"");

		// Columns description
		rstm.setColumnDescription("DBName"                , "Database name");
		rstm.setColumnDescription("ObjectName"            , "Procedure Name or '*ss' for StatementCache entries and '*sq' for DynamicPreparedSql");
		rstm.setColumnDescription("PlanID_count"          , "Number of entries this plan has in the period");
		rstm.setColumnDescription("SessionSampleTime_min" , "First entry was sampled at this time");
		rstm.setColumnDescription("SessionSampleTime_max" , "Last entry was sampled at this time");
		rstm.setColumnDescription("CmSampleMs_sum"        , "Number of milliseconds this object has been available for sampling");
		rstm.setColumnDescription("CompileDate_min"       , "First time this object was compiled");
		rstm.setColumnDescription("CompileDate_max"       , "Last time this object was compiled");
		
		rstm.setColumnDescription("MemUsageKB_max"        , "Max Memory used by this object during the report period");
		rstm.setColumnDescription("RequestCntDiff_sum"    , "How many times this object was Requested");
		rstm.setColumnDescription("TempdbRemapCnt_sum"    , "How many times this object was was recompiled due to 'tempdb changes'.");
		rstm.setColumnDescription("ExecutionCount_sum"    , "How many times this object was Executed");
		                                                  
		rstm.setColumnDescription("CPUTime_sum"           , "How much CPUTime did we use during the report period");
		rstm.setColumnDescription("ExecutionTime_sum"     , "How many milliseconds did we spend in execution during the report period");
		rstm.setColumnDescription("PhysicalReads_sum"     , "How many Physical Reads did we do during the report period");
		rstm.setColumnDescription("LogicalReads_sum"      , "How many Logical Reads did we do during the report period");
		rstm.setColumnDescription("PhysicalWrites_sum"    , "How many Physical Writes did we do during the report period");
		rstm.setColumnDescription("PagesWritten_sum"      , "How many PagesWritten (8 pages = 1 physical IO if we did 'extent io' writes) did we do during the report period");
		                                                  
		rstm.setColumnDescription("AvgCPUTime_max"        , "Average CPUTime per execution");
		rstm.setColumnDescription("AvgExecutionTime_max"  , "Average ExecutionTime for this object");
		rstm.setColumnDescription("AvgPhysicalReads_max"  , "Average PhysicalReads per execution");
		rstm.setColumnDescription("AvgLogicalReads_max"   , "Average gLogicalReads per execution");
		rstm.setColumnDescription("AvgPhysicalWrites_max" , "Average PhysicalWrites per execution");
		rstm.setColumnDescription("AvgPagesWritten_max"   , "Average PagesWritten per execution");
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
			// Describe the table
			setSectionDescription(_shortRstm);


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