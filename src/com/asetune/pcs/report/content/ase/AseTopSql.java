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
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;

import org.apache.log4j.Logger;

import com.asetune.gui.ModelMissmatchException;
import com.asetune.gui.ResultSetTableModel;
import com.asetune.pcs.report.DailySummaryReportAbstract;
import com.asetune.sql.conn.DbxConnection;
import com.asetune.utils.Configuration;
import com.asetune.utils.TimeUtils;

public class AseTopSql extends AseAbstract
{
	private static Logger _logger = Logger.getLogger(AseTopSql.class);

	private ResultSetTableModel _shortRstm;
	private ResultSetTableModel _longRstm;
//	private Exception           _problem = null;

	public AseTopSql(DailySummaryReportAbstract reportingInstance)
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
			sb.append("Row Count: ").append(_shortRstm.getRowCount()).append("\n");
			sb.append(_shortRstm.toAsciiTableString());
			sb.append(_longRstm.toAsciiTablesVerticalString());
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

			sb.append("<br>\n");
			sb.append("SQL Text by JavaSqlHashCode: ").append(_shortRstm.getRowCount()).append("<br>\n");
//			sb.append(_longRstm.toHtmlTablesVerticalString("sortable"));
			sb.append(_longRstm.toHtmlTableString("sortable"));
		}

		if (hasProblem())
			sb.append("<pre>").append(getProblem()).append("</pre> \n");

		sb.append("\n<br>");

		return sb.toString();
	}

	@Override
	public String getSubject()
	{
		return "Top Slow SQL Statements (order by: sumCpuTime, origin: monSysStatement) [with gt: execTime="+_statement_gt_execTime+", logicalReads="+_statement_gt_logicalReads+", physicalReads="+_statement_gt_physicalReads+"]";
	}

	@Override
	public boolean hasIssueToReport()
	{
		return false; // even if we found entries, do NOT indicate this as a Problem or Issue
	}

	@Override
	public void create(DbxConnection conn, String srvName, Configuration conf)
	{
		// Set: _statement_gt_* variables
		getSlowQueryThresholds(conn);
		
		int topRows = conf.getIntProperty(this.getClass().getSimpleName()+".top", 20);

		String sql = ""
			    + "select top " + topRows + " \n"
			    + "    [JavaSqlHashCode] \n"
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
			    + "where [ProcName] is NULL \n"
			    + "group by [JavaSqlHashCode] \n"
//			    + "order by [records] desc \n"
//			    + "order by [sumLogicalReads] desc \n"
			    + "order by [sumCpuTime] desc \n"
			    + "";

		_shortRstm = executeQuery(conn, sql, false, "Top SQL");
		if (_shortRstm == null)
		{
			_shortRstm = ResultSetTableModel.createEmpty("Top SQL");
			return;
		}
		else
		{
			// Do some calculations (which was hard to do in a PORTABLE SQL Way)
			int pos_sumLogicalReads             = _shortRstm.findColumn("sumLogicalReads");
			int pos_sumRowsAffected             = _shortRstm.findColumn("sumRowsAffected");
			int pos_LogicalReadsPerRowsAffected = _shortRstm.findColumn("LogicalReadsPerRowsAffected");

			int pos_FirstEntry = _shortRstm.findColumn("StartTime_min");
			int pos_LastEntry  = _shortRstm.findColumn("EndTime_max");
			int pos_Duration   = _shortRstm.findColumn("Duration");
			
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

					//------------------------------------------------
					// set "Duration"
					Timestamp FirstEntry = _shortRstm.getValueAsTimestamp(r, pos_FirstEntry);
					Timestamp LastEntry  = _shortRstm.getValueAsTimestamp(r, pos_LastEntry);

					if (FirstEntry != null && LastEntry != null)
					{
						long durationInMs = LastEntry.getTime() - FirstEntry.getTime();
						String durationStr = TimeUtils.msToTimeStr("%HH:%MM:%SS", durationInMs);
						_shortRstm.setValueAtWithOverride(durationStr, r, pos_Duration);
					}
				}
			}

			// Get SQL Text
			if (_shortRstm.getRowCount() > 0)
			{
				// For each record... try to get the SQL Text based on the JavaSqlHashCode
				int pos_JavaSqlHashCode = _shortRstm.findColumn("JavaSqlHashCode");
				if (pos_JavaSqlHashCode != -1)
				{
					for (int r=0; r<_shortRstm.getRowCount(); r++)
					{
						int JavaSqlHashCode = _shortRstm.getValueAsInteger(r, pos_JavaSqlHashCode);
						
						if (JavaSqlHashCode != -1)
						{
							sql = ""
								    + "select distinct [JavaSqlHashCode], [ServerLogin], '<pre>'||[SQLText]||'</pre>' as [SQLText] \n"
								    + "from [MonSqlCapSqlText] \n"
								    + "where [JavaSqlHashCode] = " + JavaSqlHashCode + " \n"
								    + "";
							
							sql = conn.quotifySqlString(sql);
							try ( Statement stmnt = conn.createStatement() )
							{
								// Unlimited execution time
								stmnt.setQueryTimeout(0);
								try ( ResultSet rs = stmnt.executeQuery(sql) )
								{
//									ResultSetTableModel rstm = new ResultSetTableModel(rs, "SqlDetails");
									ResultSetTableModel rstm = createResultSetTableModel(rs, "SqlDetails", sql);
									
									if (_longRstm == null)
										_longRstm = rstm;
									else
										_longRstm.add(rstm);
	
									if (_logger.isDebugEnabled())
										_logger.debug("SqlDetails.getRowCount()="+ rstm.getRowCount());
								}
							}
							catch(SQLException ex)
							{
								setProblem(ex);
	
								_logger.warn("Problems getting SQL by JavaSqlHashCode = "+JavaSqlHashCode+": " + ex);
							} 
							catch(ModelMissmatchException ex)
							{
								setProblem(ex);
	
								_logger.warn("Problems (merging into previous ResultSetTableModel) when getting SQL by JavaSqlHashCode = "+JavaSqlHashCode+": " + ex);
							} 
						}
					}
				}
			}
		}

//		sql = conn.quotifySqlString(sql);
//		try ( Statement stmnt = conn.createStatement() )
//		{
//			// Unlimited execution time
//			stmnt.setQueryTimeout(0);
//			try ( ResultSet rs = stmnt.executeQuery(sql) )
//			{
////				_shortRstm = new ResultSetTableModel(rs, "Top SQL");
//				_shortRstm = createResultSetTableModel(rs, "Top SQL");
//				
//				// Do some calculations (which was hard to do in a PORTABLE SQL Way)
//				int pos_sumLogicalReads             = _shortRstm.findColumn("sumLogicalReads");
//				int pos_sumRowsAffected             = _shortRstm.findColumn("sumRowsAffected");
//				int pos_LogicalReadsPerRowsAffected = _shortRstm.findColumn("LogicalReadsPerRowsAffected");
//
//				int pos_FirstEntry = _shortRstm.findColumn("StartTime_min");
//				int pos_LastEntry  = _shortRstm.findColumn("EndTime_max");
//				int pos_Duration   = _shortRstm.findColumn("Duration");
//				
//				if (pos_sumLogicalReads >= 0 && pos_sumRowsAffected >= 0 && pos_LogicalReadsPerRowsAffected >= 0)
//				{
//					for (int r=0; r<_shortRstm.getRowCount(); r++)
//					{
//						//------------------------------------------------
//						// set "LogicalReadsPerRowsAffected"
//						long sumLogicalReads = _shortRstm.getValueAsLong(r, pos_sumLogicalReads);
//						long sumRowsAffected = _shortRstm.getValueAsLong(r, pos_sumRowsAffected);
//
//						BigDecimal calc = new BigDecimal(-1);
//						if (sumRowsAffected > 0)
//							calc = new BigDecimal( (sumLogicalReads*1.0) / (sumRowsAffected*1.0) ).setScale(2, RoundingMode.HALF_EVEN);
//						
//						_shortRstm.setValueAtWithOverride(calc, r, pos_LogicalReadsPerRowsAffected);
//
//						//------------------------------------------------
//						// set "Duration"
//						Timestamp FirstEntry = _shortRstm.getValueAsTimestamp(r, pos_FirstEntry);
//						Timestamp LastEntry  = _shortRstm.getValueAsTimestamp(r, pos_LastEntry);
//
//						if (FirstEntry != null && LastEntry != null)
//						{
//							long durationInMs = LastEntry.getTime() - FirstEntry.getTime();
//							String durationStr = TimeUtils.msToTimeStr("%HH:%MM:%SS", durationInMs);
//							_shortRstm.setValueAtWithOverride(durationStr, r, pos_Duration);
//						}
//					}
//				}
//			
//				if (_logger.isDebugEnabled())
//					_logger.debug("_shortRstm.getRowCount()="+ _shortRstm.getRowCount());
//			}
//		}
//		catch(SQLException ex)
//		{
//			_problem = ex;
//
//			_shortRstm = ResultSetTableModel.createEmpty("Top SQL");
//			_logger.warn("Problems getting Top SQL: " + ex);
//		}
//
//		// Get SQL Text
//		if (_shortRstm != null && _shortRstm.getRowCount() > 0)
//		{
//			// For each record... try to get the SQL Text based on the JavaSqlHashCode
//			int pos_JavaSqlHashCode = _shortRstm.findColumn("JavaSqlHashCode");
//			if (pos_JavaSqlHashCode != -1)
//			{
//				for (int r=0; r<_shortRstm.getRowCount(); r++)
//				{
//					int JavaSqlHashCode = _shortRstm.getValueAsInteger(r, pos_JavaSqlHashCode);
//					
//					if (JavaSqlHashCode != -1)
//					{
////						sql = ""
////							    + "select top " + topRows + " \n"
////							    + "    count(*) as [records] \n"
////							    + "   ,[SQLText] \n"
////							    + "from [MonSqlCapSqlText] \n"
////							    + "where 1 = 1 \n"
////							    + "group by [SQLText] \n"
////							    + "order by [records] desc \n"
////							    + "";
//
//						sql = ""
//							    + "select distinct [JavaSqlHashCode], [ServerLogin], '<pre>'||[SQLText]||'</pre>' as [SQLText] \n"
//							    + "from [MonSqlCapSqlText] \n"
//							    + "where [JavaSqlHashCode] = " + JavaSqlHashCode + " \n"
//							    + "";
//						
//						sql = conn.quotifySqlString(sql);
//						try ( Statement stmnt = conn.createStatement() )
//						{
//							// Unlimited execution time
//							stmnt.setQueryTimeout(0);
//							try ( ResultSet rs = stmnt.executeQuery(sql) )
//							{
////								ResultSetTableModel rstm = new ResultSetTableModel(rs, "SqlDetails");
//								ResultSetTableModel rstm = createResultSetTableModel(rs, "SqlDetails");
//								
//								if (_longRstm == null)
//									_longRstm = rstm;
//								else
//									_longRstm.add(rstm);
//
//								if (_logger.isDebugEnabled())
//									_logger.debug("SqlDetails.getRowCount()="+ rstm.getRowCount());
//							}
//						}
//						catch(SQLException ex)
//						{
//							_problem = ex;
//
//							_logger.warn("Problems getting SQL by JavaSqlHashCode = "+JavaSqlHashCode+": " + ex);
//						} 
//						catch(ModelMissmatchException ex)
//						{
//							_problem = ex;
//
//							_logger.warn("Problems (merging into previous ResultSetTableModel) when getting SQL by JavaSqlHashCode = "+JavaSqlHashCode+": " + ex);
//						} 
//					}
//				}
//			}
//			
//		}
	}
}
