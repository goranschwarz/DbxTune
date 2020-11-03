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
import java.io.StringWriter;
import java.io.Writer;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Set;

import org.apache.log4j.Logger;

import com.asetune.gui.ResultSetTableModel;
import com.asetune.pcs.report.DailySummaryReportAbstract;
import com.asetune.sql.conn.DbxConnection;
import com.asetune.utils.Configuration;

public class AseTopCmCachedProcs extends AseAbstract
{
	private static Logger _logger = Logger.getLogger(AseTopCmCachedProcs.class);

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

			sb.append("Row Count: " + _shortRstm.getRowCount() + "<br>\n");
			sb.append(toHtmlTable(_shortRstm));


			if (_ssqlRstm != null)
			{
				sb.append("Statement Cache Entries Count: " + _ssqlRstm.getRowCount() + "<br>\n");
				sb.append(toHtmlTable(_ssqlRstm));
			}
		}
	}

//	@Override
//	public String getMessageText()
//	{
//		StringBuilder sb = new StringBuilder();
//
//		if (_shortRstm.getRowCount() == 0)
//		{
//			sb.append("No rows found <br>\n");
//		}
//		else
//		{
//			// Get a description of this section, and column names
//			sb.append(getSectionDescriptionHtml(_shortRstm, true));
//
//			sb.append("Row Count: ").append(_shortRstm.getRowCount()).append("<br>\n");
////			sb.append(_shortRstm.toHtmlTableString("sortable"));
////			sb.append(toHtmlTable(_shortRstm));
//			sb.append(toHtmlTable(_shortRstm));
//
//
//			if (_ssqlRstm != null)
//			{
//				sb.append("Statement Cache Entries Count: ").append(_ssqlRstm.getRowCount()).append("<br>\n");
////				sb.append(_ssqlRstm.toHtmlTableString("sortable"));
////				sb.append(toHtmlTable(_ssqlRstm));
//				sb.append(toHtmlTable(_ssqlRstm));
//			}
//		}
//
//		return sb.toString();
//	}

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
	public String[] getMandatoryTables()
	{
		return new String[] { "CmCachedProcs_diff" };
	}

	@Override
	public void create(DbxConnection conn, String srvName, Configuration pcsSavedConf, Configuration localConf)
	{
		createTopCmCachedProcs(conn, srvName, pcsSavedConf, localConf);
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
		rstm.setColumnDescription("Remark"                , "Used by the 'validate' method to check if values looks ok. (for example if 'CPUTime_sum' is above 24H, you will get a WARNING) ");
		rstm.setColumnDescription("PlanID_count"          , "Number of entries this plan has in the period");
		rstm.setColumnDescription("SessionSampleTime_min" , "First entry was sampled at this time");
		rstm.setColumnDescription("SessionSampleTime_max" , "Last entry was sampled at this time");
		rstm.setColumnDescription("newDiffRow_sum"        , "Number of Diff Records that was seen for the first time.");
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

	private void createTopCmCachedProcs(DbxConnection conn, String srvName, Configuration pcsSavedConf, Configuration localConf)
	{
		int topRows = localConf.getIntProperty(this.getClass().getSimpleName()+".top", 20);

		// in some versions (ASE 16.0 SP2 I have observed this in) seems to (in some cases) keep the old counter values even if we compile a new PlanID
		// So DO NOT TRUST NEWLY created PlanID's 
		// Although this can create statistical problems:
		//   - if a procedure is *constantly* recompiled (due to "whatever" reason), those procedures will be discarded from below report
		boolean skipNewDiffRateRows    = localConf.getBooleanProperty(this.getClass().getSimpleName()+".skipNewDiffRateRows", false);
		boolean hasSkipNewDiffRateRows = localConf.hasProperty(       this.getClass().getSimpleName()+".skipNewDiffRateRows");

		// try to figure out if we have *new* diff values that exceeds (using column 'ExecutionCount')
		if ( ! hasSkipNewDiffRateRows )
		{
			int executionCountThreshold = 10000;
			String sql = ""
				    + "select count(*) \n"
				    + "from [CmCachedProcs_diff] \n"
				    + "where [CmNewDiffRateRow] = 1 \n"
				    + "  and [ExecutionCount] > " + executionCountThreshold + " \n"
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
				_logger.warn("Found " + foundCount + " records in 'CmCachedProcs_diff' which had 'CmNewDiffRateRow=1' and 'ExecutionCount>" + executionCountThreshold + "'. This means I will EXCLUDE records with 'CmNewDiffRateRow=1'.");
				
				addWarningMessage("Found " + foundCount + " records in 'CmCachedProcs_diff' which had 'CmNewDiffRateRow=1' and 'ExecutionCount>" + executionCountThreshold + "'. This means I will EXCLUDE records with 'CmNewDiffRateRow=1'.");
			}
		}
		if (skipNewDiffRateRows)
			addWarningMessage("Records with the flag 'CmNewDiffRateRow=1' will NOT be part of the Report. This means that the first time a procedure is executed (or recompiled and executed), the first execution counter will NOT be part of the statistics.");
		
		
//		Set<String> existingColNames = DbUtils.getColumnNamesNoThrow(conn, null, "CmCachedProcs_diff");
//		
//		String MemUsageKB_max        = !existingColNames.contains("MemUsageKB")        ? "" : "    ,max([MemUsageKB])        as [MemUsageKB_max]        \n";
//		String RequestCntDiff_sum    = !existingColNames.contains("RequestCntDiff")    ? "" : "    ,sum([RequestCntDiff])    as RequestCntDiff_sum]     \n";
//		String TempdbRemapCnt_sum    = !existingColNames.contains("TempdbRemapCnt")    ? "" : "    ,sum([TempdbRemapCnt])    as [TempdbRemapCnt_sum]    \n";
//		String ExecutionCount_sum    = !existingColNames.contains("ExecutionCount")    ? "" : "    ,sum([ExecutionCount])    as [ExecutionCount_sum]    \n";
//
//		String CPUTime_sum           = !existingColNames.contains("CPUTime")           ? "" : "    ,sum([CPUTime])           as [CPUTime_sum]           \n";           
//		String AvgCPUTime_max        = !existingColNames.contains("AvgCPUTime")        ? "" : "    ,max([AvgCPUTime])        as [AvgCPUTime_max]        \n";        
//		String ExecutionTime_sum     = !existingColNames.contains("ExecutionTime")     ? "" : "    ,sum([ExecutionTime])     as [ExecutionTime_sum]     \n";     
//		String AvgExecutionTime_max  = !existingColNames.contains("AvgExecutionTime")  ? "" : "    ,max([AvgExecutionTime])  as [AvgExecutionTime_max]  \n";  
//		String PhysicalReads_sum     = !existingColNames.contains("PhysicalReads")     ? "" : "    ,sum([PhysicalReads])     as [PhysicalReads_sum]     \n";     
//		String AvgPhysicalReads_max  = !existingColNames.contains("AvgPhysicalReads")  ? "" : "    ,max([AvgPhysicalReads])  as [AvgPhysicalReads_max]  \n";  
//		String LogicalReads_sum      = !existingColNames.contains("LogicalReads")      ? "" : "    ,sum([LogicalReads])      as [LogicalReads_sum]      \n"; 
//		String AvgLogicalReads_max   = !existingColNames.contains("AvgLogicalReads")   ? "" : "    ,max([AvgLogicalReads])   as [AvgLogicalReads_max]   \n";   
//		String PhysicalWrites_sum    = !existingColNames.contains("PhysicalWrites")    ? "" : "    ,sum([PhysicalWrites])    as [PhysicalWrites_sum]    \n";    
//		String AvgPhysicalWrites_max = !existingColNames.contains("AvgPhysicalWrites") ? "" : "    ,max([AvgPhysicalWrites]) as [AvgPhysicalWrites_max] \n"; 
//		String PagesWritten_sum      = !existingColNames.contains("PagesWritten")      ? "" : "    ,sum([PagesWritten])      as [PagesWritten_sum]      \n";      
//		String AvgPagesWritten_max   = !existingColNames.contains("AvgPagesWritten")   ? "" : "    ,max([AvgPagesWritten])   as [AvgPagesWritten_max]   \n";   
		
		String dummySql = "select * from [CmCachedProcs_diff] where 1 = 2"; // just to get Column names
		ResultSetTableModel dummyRstm = executeQuery(conn, dummySql, true, "metadata");
		
		// Create Column selects, but only if the column exists in the PCS Table
		String ExecutionCount_sum    = !dummyRstm.hasColumnNoCase("ExecutionCount"   ) ? "" : "    ,sum([ExecutionCount])    as [ExecutionCount_sum] \n"   ;

		String CPUTime_sum           = !dummyRstm.hasColumnNoCase("CPUTime"          ) ? "" : "    ,sum([CPUTime])           as [CPUTime_sum] \n"          ; 
		String AvgCPUTime_max        = !dummyRstm.hasColumnNoCase("AvgCPUTime"       ) ? "" : "    ,max([AvgCPUTime])        as [AvgCPUTime_max] \n"       ; 
		String ExecutionTime_sum     = !dummyRstm.hasColumnNoCase("ExecutionTime"    ) ? "" : "    ,sum([ExecutionTime])     as [ExecutionTime_sum] \n"    ; 
		String AvgExecutionTime_max  = !dummyRstm.hasColumnNoCase("AvgExecutionTime" ) ? "" : "    ,max([AvgExecutionTime])  as [AvgExecutionTime_max] \n" ; 
		String PhysicalReads_sum     = !dummyRstm.hasColumnNoCase("PhysicalReads"    ) ? "" : "    ,sum([PhysicalReads])     as [PhysicalReads_sum] \n"    ; 
		String AvgPhysicalReads_max  = !dummyRstm.hasColumnNoCase("AvgPhysicalReads" ) ? "" : "    ,max([AvgPhysicalReads])  as [AvgPhysicalReads_max] \n" ; 
		String LogicalReads_sum      = !dummyRstm.hasColumnNoCase("LogicalReads"     ) ? "" : "    ,sum([LogicalReads])      as [LogicalReads_sum] \n"     ; 
		String AvgLogicalReads_max   = !dummyRstm.hasColumnNoCase("AvgLogicalReads"  ) ? "" : "    ,max([AvgLogicalReads])   as [AvgLogicalReads_max] \n"  ; 
		String PhysicalWrites_sum    = !dummyRstm.hasColumnNoCase("PhysicalWrites"   ) ? "" : "    ,sum([PhysicalWrites])    as [PhysicalWrites_sum] \n"   ; 
		String AvgPhysicalWrites_max = !dummyRstm.hasColumnNoCase("AvgPhysicalWrites") ? "" : "    ,max([AvgPhysicalWrites]) as [AvgPhysicalWrites_max] \n"; 
		String PagesWritten_sum      = !dummyRstm.hasColumnNoCase("PagesWritten"     ) ? "" : "    ,sum([PagesWritten])      as [PagesWritten_sum] \n"     ; 
		String AvgPagesWritten_max   = !dummyRstm.hasColumnNoCase("AvgPagesWritten"  ) ? "" : "    ,max([AvgPagesWritten])   as [AvgPagesWritten_max] \n"  ; 

		String whereFilter           = !dummyRstm.hasColumnNoCase("CPUTime"          ) ? "1 = 1 \n" : "([CPUTime] > 0.0 OR [ExecutionTime] > 0.0 OR [LogicalReads] > 0.0) \n";
		String orderBy               = !dummyRstm.hasColumnNoCase("CPUTime"          ) ? "order by [RequestCntDiff_sum] desc \n" : "order by [CPUTime_sum] desc \n"; 

		String whereFilter_skipNewDiffRateRows = !skipNewDiffRateRows ? "" : "  and [CmNewDiffRateRow] = 0 -- only records that has been diff calculations (not first time seen, some ASE Versions has a bug that do not clear counters on reuse) \n";

		String ObjectName = "    ,[ObjectName] \n";
		String groupBy    = "group by [DBName], [ObjectName] \n";
		if (dummyRstm.hasColumnNoCase("Hashkey2"))
		{
			ObjectName = "    ,max([ObjectName])        as [ObjectName] \n";
			groupBy    = "group by [DBName], [Hashkey2] \n"; 
		}
		
		String sql = getCmDiffColumnsAsSqlComment("CmCachedProcs")
			    + "select top " + topRows + " \n"
			    + "     [DBName] \n"
			    + ObjectName
			    + "    ,cast('' as varchar(255)) as [Remark] \n"
			    + "    ,count(distinct [PlanID]) as [PlanID_count] \n"
			    + "    ,count(*)                 as [samples_count] \n"
			    + "    ,min([SessionSampleTime]) as [SessionSampleTime_min] \n"
			    + "    ,max([SessionSampleTime]) as [SessionSampleTime_max] \n"
			    + "    ,cast('' as varchar(30))  as [Duration] \n"
			    + "    ,sum([CmNewDiffRateRow])  as [newDiffRow_sum] \n"
			    + "    ,sum([CmSampleMs])        as [CmSampleMs_sum] \n"
			    + "    ,min([CompileDate])       as [CompileDate_min] \n"
			    + "    ,max([CompileDate])       as [CompileDate_max] \n"
			    + "    \n"
			    + "    ,max([MemUsageKB])        as [MemUsageKB_max] \n"
			    + "    ,sum([RequestCntDiff])    as [RequestCntDiff_sum] \n"
			    + "    ,sum([TempdbRemapCnt])    as [TempdbRemapCnt_sum] \n"
			    + ExecutionCount_sum
			    + "    \n"
			    + CPUTime_sum
			    + AvgCPUTime_max
			    + ExecutionTime_sum
			    + AvgExecutionTime_max
			    + PhysicalReads_sum
			    + AvgPhysicalReads_max
			    + LogicalReads_sum
			    + AvgLogicalReads_max
			    + PhysicalWrites_sum
			    + AvgPhysicalWrites_max
			    + PagesWritten_sum
			    + AvgPagesWritten_max
			    + " \n"
			    + "from [CmCachedProcs_diff] \n"
			    + "where " + whereFilter + " \n"
			    + whereFilter_skipNewDiffRateRows
			    + "  and [ObjectName] NOT like '*ss%' -- If we do NOT want statement cache entries.  *ss = Statement Cache, *sq = Dynamic SQL \n"
				+ getReportPeriodSqlWhere()
			    + groupBy
			    + " \n"
			    + orderBy
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

			// Calculate Duration
			setDurationColumn(_shortRstm, "SessionSampleTime_min", "SessionSampleTime_max", "Duration");

			// Check if rows looks strange (or validate) rows
			// For example if "CPUTime_sum" is above 24 hours (or the sample period), should we mark this entry as "suspect"...
			validateEntries();
			
			// Get SQL-Text for StatementCache entries
			Set<String> stmntCacheObjects = getStatementCacheObjects(_shortRstm, "ObjectName");
			if (stmntCacheObjects != null && ! stmntCacheObjects.isEmpty() )
			{
				try 
				{
					_ssqlRstm = getSqlStatementsFromMonDdlStorage(conn, stmntCacheObjects);
				}
				catch (SQLException ex)
				{
					setProblemException(ex);
				}
			}
		}
	}

	/**
	 * Validate entries... 
	 * for example if some values looks TO BIG (for example "CPUTime_sum" is above 24 hours (or the sample period), then set a "Remark"
	 *             NOTE: If we have *many* engines, then 24H could be OK... or it might be a ASE-MDA counter issue (or some other strange issue)
	 */
	private void validateEntries()
	{
		if (_shortRstm == null)
			return;

		ResultSetTableModel rstm = _shortRstm;

		// Threshold value to be crossed: then make: WARNING
		Long th_CPUTime_sum = 3_600 * 1000 * 24L;  // 24 hours in milliseconds
		
		int pos_CPUTime_sum = rstm.findColumn("CPUTime_sum");
		int pos_remark      = rstm.findColumn("Remark");

		if (pos_CPUTime_sum >= 0 && pos_remark >= 0)
		{
			for (int r=0; r<rstm.getRowCount(); r++)
			{
				Long CPUTime_sum = rstm.getValueAsLong(r, pos_CPUTime_sum);

				if (CPUTime_sum != null)
				{
					if (CPUTime_sum > th_CPUTime_sum)
					{
//						rstm.setValueAtWithOverride("WARNING: CPUTime_sum > 24h", r, pos_remark);
						rstm.setValueAtWithOverride("<font color='red'>WARNING: CPUTime_sum > 24h</font>", r, pos_remark);
					}
				}
			}
		}
	}
}
