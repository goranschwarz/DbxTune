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
import java.sql.Types;

import org.apache.log4j.Logger;
import org.h2.tools.SimpleResultSet;

import com.asetune.gui.ResultSetTableModel;
import com.asetune.pcs.DictCompression;
import com.asetune.pcs.report.DailySummaryReportAbstract;
import com.asetune.pcs.report.content.ReportEntryAbstract;
import com.asetune.sql.conn.DbxConnection;
import com.asetune.utils.Configuration;

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

			if (_sqTextRstm != null)
			{
				sb.append("<br>\n");
				sb.append("SQL Text by queryid, Row Count: ").append(_sqTextRstm.getRowCount()).append(" (This is the same SQL Text as the in the above table, but without all counter details).<br>\n");
				sb.append(_sqTextRstm.toHtmlTableString("sortable"));
			}
		}

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

	/**
	 * Set descriptions for the table, and the columns
	 */
	private void setSectionDescription(ResultSetTableModel rstm)
	{
		if (rstm == null)
			return;
		
		// Section description
		rstm.setDescription(
				"Top Slow SQL Statements are presented here (ordered by: total_time_sum) <br>" +
				"<br>" +
				"Postgres Source table is 'pg_stat_statements'. <br>" +
				"PCS Source table is 'CmPgStatements_diff'. (PCS = Persistent Counter Store) <br>" +
				"The report <i>summarizes</i> (min/max/count/sum/avg) all entries/samples from the <i>CmPgStatements_diff</i> table grouped by 'datname, usename, queryid'. <br>" +
				"Typically the column name <i>postfix</i> will tell you what aggregate function was used. <br>" +
				"SQL Text will also be displayed in a separate table below the <i>summary</i> table.<br>" +
				"");

		// Columns description
		rstm.setColumnDescription("datname"                    , "Database name this Statement was executed in.");
		rstm.setColumnDescription("usename"                    , "Username that executed this Statement.");
		rstm.setColumnDescription("queryid"                    , "Internal hash code, computed from the statement's parse tree");
		rstm.setColumnDescription("samples_count"              , "Number of entries for this 'datname, usename, queryid' in the report period");
		rstm.setColumnDescription("SessionSampleTime_min"      , "First entry was sampled for this entry");
		rstm.setColumnDescription("SessionSampleTime_max"      , "Last entry was sampled for this entry");
		rstm.setColumnDescription("Duration"                   , "Start/end time presented as HH:MM:SS, so we can see if this entry is just for a short time or if it spans over a long period of time.");
		rstm.setColumnDescription("CmSampleMs_sum"             , "Number of milliseconds this object has been available for sampling");

		rstm.setColumnDescription("calls_sum"                  , "Number of times executed");
		rstm.setColumnDescription("avg_time_per_call_avg"      , "Average Execution Time per call              (Algorithm: total_time / calls)");
		rstm.setColumnDescription("total_time_sum"             , "Total time spent in the statement, in milliseconds");
		rstm.setColumnDescription("avg_rows_per_call_avg"      , "Average 'number of rows retrived' per call   (Algorithm: rows / calls)");
		rstm.setColumnDescription("rows_sum"                   , "Total number of rows retrieved or affected by the statement");

		rstm.setColumnDescription("shared_blks_hit_per_row_avg", "Average 'number of cache reads' per call     (Algorithm: shared_blks_hit / calls)");
		rstm.setColumnDescription("shared_blks_hit_sum"        , "Total number of shared block cache hits by the statement");
		rstm.setColumnDescription("shared_blks_read_sum"       , "Total number of shared blocks read by the statement");
		rstm.setColumnDescription("shared_blks_dirtied_sum"    , "Total number of shared blocks dirtied by the statement");
		rstm.setColumnDescription("shared_blks_written_sum"    , "Total number of shared blocks written by the statement");
		
		rstm.setColumnDescription("local_blks_hit_sum"         , "Total number of local block cache hits by the statement");
		rstm.setColumnDescription("local_blks_read_sum"        , "Total number of local blocks read by the statement");
		rstm.setColumnDescription("local_blks_dirtied_sum"     , "Total number of local blocks dirtied by the statement");
		rstm.setColumnDescription("local_blks_written_sum"     , "Total number of local blocks written by the statement");

		rstm.setColumnDescription("temp_blks_read_sum"         , "Total number of temp blocks read by the statement");
		rstm.setColumnDescription("temp_blks_written_sum"      , "Total number of temp blocks written by the statement");

		rstm.setColumnDescription("blks_read_time_sum"         , "Total time the statement spent reading blocks, in milliseconds (if track_io_timing is enabled, otherwise zero)");
		rstm.setColumnDescription("blks_write_time_sum"        , "Total time the statement spent writing blocks, in milliseconds (if track_io_timing is enabled, otherwise zero)");

		rstm.setColumnDescription("query"                      , "Text of a representative statement");
	}

	@Override
	public void create(DbxConnection conn, String srvName, Configuration pcsSavedConf, Configuration localConf)
	{
		int topRows = localConf.getIntProperty(this.getClass().getSimpleName()+".top", 20);

		// used to lookup queryid->sqlText (as a second step)
//		List<Integer> queryIdList = new ArrayList<>();
		
		// Check if table "CmPgStatements_diff" has Dictionary Compressed Columns (any columns ends with "$dcc$")
		boolean hasDictCompCols = false;
		try {
			hasDictCompCols = DictCompression.hasCompressedColumnNames(conn, null, "CmPgStatements_diff");
		} catch (SQLException ex) {
			_logger.error("Problems checking for Dictionary Compressed Columns in table 'CmPgStatements_diff'.", ex);
		}
		
		String col_query = "query";
		if (hasDictCompCols)
			col_query = "query$dcc$";

		// GET ALL INFO
		String sql = getCmDiffColumnsAsSqlComment("CmPgStatements")
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
			    + "	,min([" + col_query + "])                                      as [query] \n"
			    + "from [CmPgStatements_diff] x \n"
//			    + "where [usename] != 'postgres' \n"
			    + "where [calls] > 0 \n"                          // do only get records that has been executed (even if presented in the "Statement cache")
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
			// Describe the table
			setSectionDescription(_shortRstm);

			// set duration
			setDurationColumn(_shortRstm, "SessionSampleTime_min", "SessionSampleTime_max", "Duration");
						
			
			// Create a SQL-Details ResultSet based on values in _shortRstm
			SimpleResultSet srs = new SimpleResultSet();

			srs.addColumn("datname",      Types.VARCHAR,       60, 0);
			srs.addColumn("usename",      Types.VARCHAR,       60, 0);
			srs.addColumn("queryid",      Types.BIGINT,         0, 0);
			srs.addColumn("query",        Types.VARCHAR, 1024*128, 0); // this is 'text' in the origin table


			int pos_datname    = _shortRstm.findColumn("datname");
			int pos_usename    = _shortRstm.findColumn("usename");
			int pos_queryid    = _shortRstm.findColumn("queryid");
			int pos_query      = _shortRstm.findColumn("query");

			if (pos_datname >= 0 && pos_usename >= 0 && pos_queryid >= 0 && pos_query >= 0)
			{
				for (int r=0; r<_shortRstm.getRowCount(); r++)
				{
					String  datname  = _shortRstm.getValueAsString (r, pos_datname);
					String  usename  = _shortRstm.getValueAsString (r, pos_usename);
					Long    queryid  = _shortRstm.getValueAsLong   (r, pos_queryid);
					String  query    = _shortRstm.getValueAsString (r, pos_query);

					// When using Dictionary Compression the "query" is just a hashId
					// So get the real Query TEXT from the key/value lookup table
					if (hasDictCompCols)
					{
						String hashId = _shortRstm.getValueAsString (r, pos_query);
						try
						{
							query = DictCompression.getValueForHashId(conn, null, "CmPgStatements", "query", hashId);
						} 
						catch (SQLException ex) 
						{
							query = "Problems getting Dictionary Compressed column for tabName='CmPgStatements', colName='query', hashId='" + hashId + "'.";
						}

						// set QUERY text in the original ResultSet
						_shortRstm.setValueAtWithOverride(query, r, pos_query);
					}

					// add record to SimpleResultSet
					srs.addRow(datname, usename, queryid, "<xmp>" + query + "</xmp>");
				}
			}

			// GET SQLTEXT (only)
			try
			{
				// Note the 'srs' is populated when reading above ResultSet from query
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
	}
}
