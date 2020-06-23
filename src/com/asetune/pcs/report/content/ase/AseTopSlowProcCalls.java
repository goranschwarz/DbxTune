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

public class AseTopSlowProcCalls extends AseAbstract
{
//	private static Logger _logger = Logger.getLogger(AseTopSlowProcCalls.class);

	private ResultSetTableModel _shortRstm;
	private ResultSetTableModel _ssqlRstm;
//	private Exception           _problem = null;

	public AseTopSlowProcCalls(DailySummaryReportAbstract reportingInstance)
	{
		super(reportingInstance);
	}

	@Override
	public String getMessageText()
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

		return sb.toString();
	}

	@Override
	public String getSubject()
	{
		return "Top [SQL Captured] SLOW Procedure Calls (order by: sumCpuTime,  origin: monSysStatement) [with gt: execTime="+_statement_gt_execTime+", logicalReads="+_statement_gt_logicalReads+", physicalReads="+_statement_gt_physicalReads+"]";
	}

	@Override
	public boolean hasIssueToReport()
	{
		return false; // even if we found entries, do NOT indicate this as a Problem or Issue
	}


	@Override
	public void create(DbxConnection conn, String srvName, Configuration pcsSavedConf, Configuration localConf)
	{
		// Set: _statement_gt_* variables
		getSlowQueryThresholds(conn);
		
		createTopSlowSqlProcedureCalls(conn, srvName, pcsSavedConf, localConf);
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
				"Top [SQL Captured] Slow Procedure/LineNumber are presented here (this means at a 'LineNumber' level for Stored Procedures) (ordered by: sumCpuTime) <br>" +
				"<br>" +
				"Thresholds: with GreaterThan: execTime="+_statement_gt_execTime+", logicalReads="+_statement_gt_logicalReads+", physicalReads="+_statement_gt_physicalReads+"<br>" +
				"Thresholds: having sumCpuTime &gt;= 1000<br>" +
				"<br>" +
				"ASE Source table is 'master.dbo.monSysStatement', which is a <i>ring buffer</i>, if the buffer is small, then we will be missing entries. <br>" +
				"PCS Source table is 'MonSqlCapStatements'. (PCS = Persistent Counter Store) <br>" +
				"<br>" +
				"The report <i>summarizes</i> (min/max/count/sum/avg) all entries/samples from the <i>MonSqlCapStatements</i> table grouped by 'ProcName, LineNumber'. <br>" +
				"Typically the column name <i>postfix</i> will tell you what aggregate function was used. <br>" +
				"SQL Text will be displayed in a separate table below the <i>summary</i> table.<br>" +
				"");

		// Columns description
		rstm.setColumnDescription("ProcName"                   , "Stored Procedure Name");
		rstm.setColumnDescription("LineNumber"                 , "LineNumber within the Stored Procedure");
		rstm.setColumnDescription("records"                    , "Number of entries for this 'JavaSqlHashCode' in the report period");
                                                              
		rstm.setColumnDescription("StartTime_min"              , "First entry was sampled for this JavaSqlHashCode");
		rstm.setColumnDescription("EndTime_max"                , "Last entry was sampled for this JavaSqlHashCode");
		rstm.setColumnDescription("Duration"                   , "Start/end time presented as HH:MM:SS, so we can see if this JavaSqlHashCode is just for a short time or if it spans over a long period of time.");
                                                              
		rstm.setColumnDescription("avgElapsed_ms"              , "Average Time it took to execute this Statement during the report period (sumElapsed_ms/records)   ");
		rstm.setColumnDescription("avgCpuTime"                 , "Average CpuTime this Statement used            during the report period (sumCpuTime/records)      ");
		rstm.setColumnDescription("avgWaitTime"                , "Average avgWaitTime this Statement waited      during the report period (sumWaitTime/records)     ");
		rstm.setColumnDescription("avgMemUsageKB"              , "Average MemUsageKB this Statement used         during the report period (sumMemUsageKB/records)   ");
		rstm.setColumnDescription("avgPhysicalReads"           , "Average PhysicalReads this Statement used      during the report period (sumPhysicalReads/records)");
		rstm.setColumnDescription("avgLogicalReads"            , "Average LogicalReads this Statement used       during the report period (sumLogicalReads/records) ");
		rstm.setColumnDescription("avgRowsAffected"            , "Average RowsAffected this Statement did        during the report period (sumRowsAffected/records) ");
                                                              
		rstm.setColumnDescription("sumElapsed_ms"              , "How many milliseconds did we spend in execution during the report period");
		rstm.setColumnDescription("sumCpuTime"                 , "How much CPUTime did we use during the report period");
		rstm.setColumnDescription("sumWaitTime"                , "How much WaitTime did we use during the report period");
		rstm.setColumnDescription("sumMemUsageKB"              , "How much MemUsageKB did we use during the report period");
		rstm.setColumnDescription("sumPhysicalReads"           , "How much PhysicalReads did we use during the report period");
		rstm.setColumnDescription("sumLogicalReads"            , "How much LogicalReads did we use during the report period");
		rstm.setColumnDescription("sumRowsAffected"            , "How many RowsAffected did did this Statement do during the report period");

		rstm.setColumnDescription("LogicalReadsPerRowsAffected", "How Many LogicalReads per RowsAffected did this Statement do during the report period (Algorithm: sumLogicalReads/sumRowsAffected)");
	}

	private void createTopSlowSqlProcedureCalls(DbxConnection conn, String srvName, Configuration pcsSavedConf, Configuration localConf)
	{
		int topRows          = localConf.getIntProperty(this.getClass().getSimpleName()+".top", 20);
		int havingSumCpuTime = 1000; // 1 second
		
			String sql = "-- source table: MonSqlCapStatements \n"
			    + "select top " + topRows + " \n"
			    + "    [ProcName] \n"
			    + "   ,[LineNumber] \n"
			    + "   ,count(*)                     as [records] \n"
			    + " \n"
			    + "	  ,min([StartTime])             as [StartTime_min] \n"
			    + "	  ,max([EndTime])               as [EndTime_max] \n"
			    + "	  ,cast('' as varchar(30))      as [Duration] \n"
			    + " \n"
			    + "   ,avg([Elapsed_ms])            as [avgElapsed_ms] \n"
			    + "   ,avg([CpuTime])               as [avgCpuTime] \n"
			    + "   ,avg([WaitTime])              as [avgWaitTime] \n"
			    + "   ,avg([MemUsageKB])            as [avgMemUsageKB] \n"
			    + "   ,avg([PhysicalReads])         as [avgPhysicalReads] \n"
			    + "   ,avg([LogicalReads])          as [avgLogicalReads] \n"
			    + "   ,avg([RowsAffected])          as [avgRowsAffected] \n"
//			    + "   ,avg([QueryOptimizationTime]) as [avgQueryOptimizationTime] \n"
//			    + "   ,avg([PagesModified])         as [avgPagesModified] \n"
//			    + "   ,avg([PacketsSent])           as [avgPacketsSent] \n"
//			    + "   ,avg([PacketsReceived])       as [avgPacketsReceived] \n"
			    + " \n"
			    + "   ,sum([Elapsed_ms])            as [sumElapsed_ms] \n"
			    + "   ,sum([CpuTime])               as [sumCpuTime] \n"
			    + "   ,sum([WaitTime])              as [sumWaitTime] \n"
			    + "   ,sum([MemUsageKB])            as [sumMemUsageKB] \n"
			    + "   ,sum([PhysicalReads])         as [sumPhysicalReads] \n"
			    + "   ,sum([LogicalReads])          as [sumLogicalReads] \n"
			    + "   ,sum([RowsAffected])          as [sumRowsAffected] \n"
//			    + "   ,sum([QueryOptimizationTime]) as [sumQueryOptimizationTime] \n"
//			    + "   ,sum([PagesModified])         as [sumPagesModified] \n"
//			    + "   ,sum([PacketsSent])           as [sumPacketsSent] \n"
//			    + "   ,sum([PacketsReceived])       as [sumPacketsReceived] \n"

//				+ "   ,(sum([LogicalReads])*1.0) / (coalesce(sum([RowsAffected]),1)*1.0) as [LogicalReadsPerRowsAffected] \n"
				+ "   ,-9999999.0 as [LogicalReadsPerRowsAffected] \n"
				
			    + "from [MonSqlCapStatements] \n"
			    + "where [ProcName] is NOT NULL \n"
			    + "group by [ProcName], [LineNumber] \n"
			    + "having [sumCpuTime] >= " + havingSumCpuTime + " \n"
//			    + "order by [records] desc \n"
//			    + "order by [sumLogicalReads] desc \n"
			    + "order by [sumCpuTime] desc \n"
			    + "";

		_shortRstm = executeQuery(conn, sql, false, "TopSqlProcedureCalls");
		if (_shortRstm == null)
		{
			_shortRstm = ResultSetTableModel.createEmpty("TopSqlProcedureCalls");
			return;
		}
		else
		{
			// Describe the table
			setSectionDescription(_shortRstm);

			// set duration
			setDurationColumn(_shortRstm, "StartTime_min", "EndTime_max", "Duration");
			
			// Do some calculations (which was hard to do in a PORTABLE SQL Way)
			int pos_sumLogicalReads             = _shortRstm.findColumn("sumLogicalReads");
			int pos_sumRowsAffected             = _shortRstm.findColumn("sumRowsAffected");
			int pos_LogicalReadsPerRowsAffected = _shortRstm.findColumn("LogicalReadsPerRowsAffected");

			if (pos_sumLogicalReads >= 0 && pos_sumRowsAffected >= 0 && pos_LogicalReadsPerRowsAffected >= 0)
			{
				for (int r=0; r<_shortRstm.getRowCount(); r++)
				{
					//------------------------------------------------
					// set "LogicalReadsPerRowsAffected"
					long sumLogicalReads = _shortRstm.getValueAsLong(r, pos_sumLogicalReads);
					long sumRowsAffected = _shortRstm.getValueAsLong(r, pos_sumRowsAffected);

					BigDecimal calc = new BigDecimal(-1);
					if (sumRowsAffected > 0)
						calc = new BigDecimal( (sumLogicalReads*1.0) / (sumRowsAffected*1.0) ).setScale(2, RoundingMode.HALF_EVEN);
					
					_shortRstm.setValueAtWithOverride(calc, r, pos_LogicalReadsPerRowsAffected);
				}
			}
			
			//--------------------------------------------------------------------------------------
			// For StatementCache entries... get the SQL Text (or actually the XML Plan and try to get SQL Text from that)
			//--------------------------------------------------------------------------------------
			if (_shortRstm.getRowCount() > 0)
			{
				Set<String> stmntCacheObjects = getStatementCacheObjects(_shortRstm, "ProcName");

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
}
