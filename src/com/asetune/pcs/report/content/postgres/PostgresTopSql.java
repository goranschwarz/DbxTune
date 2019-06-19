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
package com.asetune.pcs.report.content.postgres;

import java.sql.SQLException;
import java.sql.Timestamp;
import java.sql.Types;

import org.apache.log4j.Logger;
import org.h2.tools.SimpleResultSet;

import com.asetune.gui.ResultSetTableModel;
import com.asetune.pcs.report.DailySummaryReportAbstract;
import com.asetune.pcs.report.content.ReportEntryAbstract;
import com.asetune.sql.conn.DbxConnection;
import com.asetune.utils.Configuration;
import com.asetune.utils.TimeUtils;

public class PostgresTopSql
extends ReportEntryAbstract
{
	private static Logger _logger = Logger.getLogger(PostgresTopSql.class);

	private ResultSetTableModel _shortRstm;
	private ResultSetTableModel _sqTextRstm;
//	private Exception           _problem = null;

	public PostgresTopSql(DailySummaryReportAbstract reportingInstance)
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
			sb.append("SQL Text by queryid, Row Count: ").append(_sqTextRstm.getRowCount()).append(" (This is the same SQL Text as the in the above table, but without all counter details).<br>\n");
			sb.append(_sqTextRstm.toHtmlTableString("sortable"));
		}

		if (hasProblem())
			sb.append("<pre>").append(getProblem()).append("</pre> \n");

		sb.append("\n<br>");

		return sb.toString();
	}

	@Override
	public String getSubject()
	{
		return "Top SQL Statements (order by: total_time, origin: pg_stat_statements)";
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

		// used to lookup queryid->sqlText (as a second step)
//		List<Integer> queryIdList = new ArrayList<>();
		
		// GET ALL INFO
		String sql = ""
			    + "select top " + topRows + " \n"
			    + "	 [datname] \n"
			    + "	,[usename] \n"
			    + "	,[queryid] \n"
			    + "	,count(*)                                               as [samples_count] \n"
			    + "	,min([SessionSampleTime])                               as [SessionSampleTime_min] \n"
			    + "	,max([SessionSampleTime])                               as [SessionSampleTime_max] \n"
			    + "	,cast('' as varchar(30))                                as [Duration] \n"
			    + "	,sum([CmSampleMs])                                      as [CmSampleMs_sum] \n"
			    + " \n"
			    + "	,sum([calls])                                           as [calls_sum] \n"
			    + "	,CAST( avg([avg_time_per_call]) AS DECIMAL(20,1) )      as [avg_time_per_call_avg] \n"			// needs rounding
			    + "	,CAST( sum([total_time])        AS DECIMAL(20,1) )      as [total_time_sum] \n"					// needs rounding
			    + "	,avg([avg_rows_per_call])                               as [avg_rows_per_call_avg] \n"
			    + "	,sum([rows])                                            as [rows_sum] \n"
			    + " \n"
			    + "	,avg([shared_blks_hit_per_row])                         as [shared_blks_hit_per_row_avg] \n"
			    + "	,sum([shared_blks_hit])                                 as [shared_blks_hit_sum] \n"
			    + "	,sum([shared_blks_read])                                as [shared_blks_read_sum] \n"
			    + "	,sum([shared_blks_dirtied])                             as [shared_blks_dirtied_sum] \n"
			    + "	,sum([shared_blks_written])                             as [shared_blks_written_sum] \n"
			    + " \n"                                                     
			    + "	,sum([local_blks_hit])                                  as [local_blks_hit_sum] \n"
			    + "	,sum([local_blks_read])                                 as [local_blks_read_sum] \n"
			    + "	,sum([local_blks_dirtied])                              as [local_blks_dirtied_sum] \n"
			    + "	,sum([local_blks_written])                              as [local_blks_written_sum] \n"
			    + " \n"                                                     
			    + "	,sum([temp_blks_read])                                  as [temp_blks_read_sum] \n"
			    + "	,sum([temp_blks_written])                               as [temp_blks_written_sum] \n"
			    + " \n"                                                     
			    + "	,sum([blk_read_time])                                   as [blks_read_time_sum] \n"
			    + "	,sum([blk_write_time])                                  as [blks_write_time_sum] \n"
			    + " \n"                                                     
			    + "	,min([query])                                           as [query] \n"
			    + "from [CmPgStatements_diff] x \n"
//			    + "where [usename] != 'postgres' \n"
			    + "group by [datname], [usename], [queryid] \n"
			    + "order by [total_time_sum] desc \n"
			    + "";

		_shortRstm = executeQuery(conn, sql, false, "Top SQL");
		if (_shortRstm == null)
		{
			_shortRstm = ResultSetTableModel.createEmpty("Top SQL");
			return;
		}
		else
		{
			SimpleResultSet srs = new SimpleResultSet();

			srs.addColumn("datname",      Types.VARCHAR,       60, 0);
			srs.addColumn("usename",      Types.VARCHAR,       60, 0);
			srs.addColumn("queryid",      Types.BIGINT,         0, 0);
			srs.addColumn("query",        Types.VARCHAR, 1024*128, 0); // this is 'text' in the origin table


			// Calculate: Duration
			int pos_FirstEntry = _shortRstm.findColumn("SessionSampleTime_min");
			int pos_LastEntry  = _shortRstm.findColumn("SessionSampleTime_max");
			int pos_Duration   = _shortRstm.findColumn("Duration");
			
			int pos_datname    = _shortRstm.findColumn("datname");
			int pos_usename    = _shortRstm.findColumn("usename");
			int pos_queryid    = _shortRstm.findColumn("queryid");
			int pos_query      = _shortRstm.findColumn("query");

			if (pos_FirstEntry >= 0 && pos_LastEntry >= 0 && pos_Duration >= 0)
			{
				for (int r=0; r<_shortRstm.getRowCount(); r++)
				{
					Timestamp FirstEntry = _shortRstm.getValueAsTimestamp(r, pos_FirstEntry);
					Timestamp LastEntry  = _shortRstm.getValueAsTimestamp(r, pos_LastEntry);

					if (FirstEntry != null && LastEntry != null)
					{
						long durationInMs = LastEntry.getTime() - FirstEntry.getTime();
						String durationStr = TimeUtils.msToTimeStr("%HH:%MM:%SS", durationInMs);
						_shortRstm.setValueAtWithOverride(durationStr, r, pos_Duration);
					}

					// Get QueryID
//					Integer queryid  = _shortRstm.getValueAsInteger(r, pos_queryid);
//					if (queryid != null)
//						queryIdList.add(queryid);
					String  datname  = _shortRstm.getValueAsString (r, pos_datname);
					String  usename  = _shortRstm.getValueAsString (r, pos_usename);
					Long    queryid  = _shortRstm.getValueAsLong   (r, pos_queryid);
					String  query    = _shortRstm.getValueAsString (r, pos_query);
					
					srs.addRow(datname, usename, queryid, query);
				}
			}

			// GET SQLTEXT (only)
			try
			{
				// Note the 'srs' is populated when reading above ResultSet from query
//				_sqTextRstm = new ResultSetTableModel(srs, "Top SQL TEXT");
				_sqTextRstm = createResultSetTableModel(srs, "Top SQL TEXT", null);
				srs.close();
			}
			catch (SQLException ex)
			{
				setProblem(ex);
	
				_sqTextRstm = ResultSetTableModel.createEmpty("Top SQL TEXT");
				_logger.warn("Problems getting Top SQL TEXT: " + ex);
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
//				// Calculate: Duration
//				int pos_FirstEntry = _shortRstm.findColumn("SessionSampleTime_min");
//				int pos_LastEntry  = _shortRstm.findColumn("SessionSampleTime_max");
//				int pos_Duration   = _shortRstm.findColumn("Duration");
//				
//				int pos_datname    = _shortRstm.findColumn("datname");
//				int pos_usename    = _shortRstm.findColumn("usename");
//				int pos_queryid    = _shortRstm.findColumn("queryid");
//				int pos_query      = _shortRstm.findColumn("query");
//
//				if (pos_FirstEntry >= 0 && pos_LastEntry >= 0 && pos_Duration >= 0)
//				{
//					for (int r=0; r<_shortRstm.getRowCount(); r++)
//					{
//						Timestamp FirstEntry = _shortRstm.getValueAsTimestamp(r, pos_FirstEntry);
//						Timestamp LastEntry  = _shortRstm.getValueAsTimestamp(r, pos_LastEntry);
//
//						if (FirstEntry != null && LastEntry != null)
//						{
//							long durationInMs = LastEntry.getTime() - FirstEntry.getTime();
//							String durationStr = TimeUtils.msToTimeStr("%HH:%MM:%SS", durationInMs);
//							_shortRstm.setValueAtWithOverride(durationStr, r, pos_Duration);
//						}
//
//						// Get QueryID
////						Integer queryid  = _shortRstm.getValueAsInteger(r, pos_queryid);
////						if (queryid != null)
////							queryIdList.add(queryid);
//						String  datname  = _shortRstm.getValueAsString (r, pos_datname);
//						String  usename  = _shortRstm.getValueAsString (r, pos_usename);
//						Integer queryid  = _shortRstm.getValueAsInteger(r, pos_queryid);
//						String  query    = _shortRstm.getValueAsString (r, pos_query);
//						
//						srs.addRow(datname, usename, queryid, query);
//					}
//				}
//
//				if (_logger.isDebugEnabled())
//					_logger.debug("_shortRstm.getRowCount()="+ _shortRstm.getRowCount());
//			}
//		}
//		catch (SQLException ex)
//		{
//			_problem = ex;
//
//			_shortRstm = ResultSetTableModel.createEmpty("Top SQL");
//			_logger.warn("Problems getting Top SQL: " + ex);
//		}

	
//		// GET SQLTEXT (only)
//		try
//		{
//			// Note the 'srs' is populated when reading above ResultSet from query
////			_sqTextRstm = new ResultSetTableModel(srs, "Top SQL TEXT");
//			_sqTextRstm = createResultSetTableModel(srs, "Top SQL TEXT");
//			srs.close();
//		}
//		catch (SQLException ex)
//		{
//			_problem = ex;
//
//			_sqTextRstm = ResultSetTableModel.createEmpty("Top SQL TEXT");
//			_logger.warn("Problems getting Top SQL TEXT: " + ex);
//		}
	
//		// GET SQLTEXT (only)
//		sql = ""
//			    + "select distinct \n"
//			    + "	 [datname] \n"
//			    + "	,[usename] \n"
//			    + "	,[queryid] \n"
//			    + "	,[query] \n"
//			    + "from [CmPgStatements_diff] x \n"
//			    + "where [queryid] in (" +  StringUtil.toCommaStr(queryIdList) + ") \n"
//			    + "";
//
//		sql = conn.quotifySqlString(sql);
//		try ( Statement stmnt = conn.createStatement() )
//		{
//			// Unlimited execution time
//			stmnt.setQueryTimeout(0);
//			try ( ResultSet rs = stmnt.executeQuery(sql) )
//			{
//				_sqTextRstm = new ResultSetTableModel(rs, "Top SQL TEXT");
//				
//				if (_logger.isDebugEnabled())
//					_logger.debug("_sqTextRstm.getRowCount()="+ _sqTextRstm.getRowCount());
//			}
//		}
//		catch(SQLException ex)
//		{
//			_problem = ex;
//
//			_sqTextRstm = ResultSetTableModel.createEmpty("Top SQL TEXT");
//			_logger.warn("Problems getting Top SQL TEXT: " + ex);
//		}
	}
}
