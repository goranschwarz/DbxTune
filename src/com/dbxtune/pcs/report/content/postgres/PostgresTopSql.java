/*******************************************************************************
 * Copyright (C) 2010-2025 Goran Schwarz
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
package com.dbxtune.pcs.report.content.postgres;

import java.io.IOException;
import java.io.Writer;
import java.sql.SQLException;
import java.sql.Types;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.List;

import org.apache.commons.text.StringEscapeUtils;
import org.apache.log4j.Logger;
import org.h2.tools.SimpleResultSet;

import com.dbxtune.gui.ResultSetTableModel;
import com.dbxtune.gui.ResultSetTableModel.TableStringRenderer;
import com.dbxtune.pcs.DictCompression;
import com.dbxtune.pcs.report.DailySummaryReportAbstract;
import com.dbxtune.pcs.report.content.SparklineHelper;
import com.dbxtune.pcs.report.content.SparklineHelper.AggType;
import com.dbxtune.pcs.report.content.SparklineHelper.DataSource;
import com.dbxtune.pcs.report.content.SparklineHelper.SparkLineParams;
import com.dbxtune.sql.conn.DbxConnection;
import com.dbxtune.utils.Configuration;
import com.dbxtune.utils.DbUtils;
import com.dbxtune.utils.HtmlTableProducer;
import com.dbxtune.utils.StringUtil;
import com.dbxtune.utils.HtmlTableProducer.ColumnCopyDef;
import com.dbxtune.utils.HtmlTableProducer.ColumnCopyRender;
import com.dbxtune.utils.HtmlTableProducer.ColumnCopyRow;
import com.dbxtune.utils.HtmlTableProducer.ColumnStatic;
import com.dbxtune.utils.HtmlTableProducer.EmptyColumn;

public class PostgresTopSql
extends PostgresAbstract
{
	private static Logger _logger = Logger.getLogger(PostgresTopSql.class);

	private ResultSetTableModel _shortRstm;
	private ResultSetTableModel _sqTextRstm;
	private ResultSetTableModel _perfConfigRstm;
//	private Exception           _problem = null;
	private List<String>        _miniChartJsList = new ArrayList<>();

	public static final String PROPKEY_skip_dbname_csv   = "PostgresTopSql.skip.dbname.csv";
	public static final String DEFAULT_skip_dbname_csv   = null;

	public static final String PROPKEY_skip_username_csv = "PostgresTopSql.skip.username.csv";
	public static final String DEFAULT_skip_username_csv = null;

	public PostgresTopSql(DailySummaryReportAbstract reportingInstance)
	{
		super(reportingInstance);
	}

	@Override
	public boolean hasMinimalMessageText()
	{
		return false;
	}

	@Override
	public boolean hasShortMessageText()
	{
		return true;
	}

//	@Override
//	public void writeShortMessageText(Writer w)
//	throws IOException
//	{
//		writeMessageText(w);
//	}

	@Override
	public void writeMessageText(Writer sb, MessageType messageType)
	throws IOException
	{
		if (_shortRstm.getRowCount() == 0)
		{
			sb.append("No rows found <br>\n");
		}
		else
		{
			// Full information, including (top summary table)
			if (isFullMessageType())
			{
				// Get a description of this section, and column names
				sb.append(getSectionDescriptionHtml(_shortRstm, true));

				sb.append("Optional settings:<br>\n");
				sb.append(" &emsp; &bull; Skip any database(s) in the below table, set property <code>" + PROPKEY_skip_dbname_csv + "=db1,db2,db3</code> <br>\n");
				sb.append(" &emsp; &bull; Skip any username(s) in the below table, set property <code>" + PROPKEY_skip_username_csv + "=user1,user2</code> <br>\n");
				sb.append("<br>\n");

//				sb.append("Row Count: " + _shortRstm.getRowCount() + "<br>\n");
				sb.append("Row Count: " + _shortRstm.getRowCount() + "&emsp;&emsp; To change number of <i>top</i> records, set property <code>" + getTopRowsPropertyName() + "=##</code><br>\n");
				TableStringRenderer tableRender = new ReportEntryTableStringRenderer()
				{
					@Override
					public String cellValue(ResultSetTableModel rstm, int row, int col, String colName, Object objVal, String strVal)
					{
						if ("query".equals(colName))
						{
							// Get Actual Executed SQL Text for current row
							String queryid = rstm.getValueAsString(row, "queryid");
							
							// Put the "Actual Executed SQL Text" as a "tooltip"
							return "<div title='Click for Detailes' "
									+ "data-toggle='modal' "
									+ "data-target='#dbx-view-sqltext-dialog' "
									+ "data-objectname='" + queryid + "' "
									+ "data-tooltip=\""   + getTooltipForSqlText(rstm, row) + "\" "
									+ ">&#x1F4AC;</div>"; // symbol popup with "..."
						}

						return strVal;
					}
				};
				sb.append(_shortRstm.toHtmlTableString("sortable", true, true, null, tableRender));
			}

			// The "sub table" with pivot info on most mini-charts/sparkline
			if (_sqTextRstm != null)
			{
//				sb.append("<br>\n");
//				sb.append("SQL Text by queryid, Row Count: " + _sqTextRstm.getRowCount() + " (This is the same SQL Text as the in the above table, but without all counter details).<br>\n");
//				sb.append(toHtmlTable(_sqTextRstm));

				sb.append("<br>\n");
				sb.append("<details open> \n");
				sb.append("<summary>Details for above Statements, including SQL Text (click to collapse) </summary> \n");
				
				sb.append("<br>\n");
				sb.append("SQL Text by queryid, Row Count: " + _sqTextRstm.getRowCount() + " (This is the same SQL Text as the in the above table, but without all counter details).<br>\n");
				sb.append(toHtmlTable(_sqTextRstm));
//				sb.append(_sqTextRstm.toHtmlTableString("sortable sparklines-table", false, false, null, new ReportEntryTableStringRenderer()));

				sb.append("\n");
				sb.append("</details> \n");
			}

			
			// Write server level configuration relevant for this section
			sb.append("<br>\n");
			sb.append("<b>Below is serverwide 'Query Tuning / Planner Cost Constants' Parameters.</b><br>\n");
			sb.append("<i>One parameter to look at is <code>random_page_cost</code>... If you have fast disk (SSD), it's probably a good idea to set this to <code>1.1</code>, which makes the optimizer more likely to choose indexes over table scans.</i><br>\n");
			sb.append("Please search the Postgres Documentation and Internet for <code>random_page_cost</code> to make your own decisions!<br>\n");
			sb.append("One good place is <a href='https://www.interdb.jp/pg' target='_blank'>https://www.interdb.jp/pg -- The Internals of PostgreSQL</a><br>\n");

			sb.append(toHtmlTable(_perfConfigRstm));
		}
		
		// Write JavaScript code for CPU SparkLine
		if (isFullMessageType())
		{
			for (String str : _miniChartJsList)
				sb.append(str);
		}
	}

	/** Format Numbers with comma separators for readability), everything else "as is" */
	private String format(Object obj)
	{
		if (obj == null)
			return null;

		if (obj instanceof Number)
		{
			NumberFormat nf = NumberFormat.getInstance();
			return nf.format( (Number) obj );
		}
		else
		{
			return obj.toString();
		}
	}

	/** double quotes (") must be avoided or escaped */
	private String getTooltipForSqlText(ResultSetTableModel rstm, int row)
	{
		StringBuilder sb = new StringBuilder();

		sb.append("-- Some columns extracted from current row.\n");
		sb.append("-----------------------------------------------------------------------------------------------\n");
		sb.append("-- datname:                      ").append( rstm.getValueAsString(row, "datname"                  ) ).append("\n");
		sb.append("-- usename:                      ").append( rstm.getValueAsString(row, "usename"                  ) ).append("\n");
		sb.append("-- queryid:                      ").append( rstm.getValueAsString(row, "queryid"                  ) ).append("\n");
		sb.append("-- SessionSampleTime__min:       ").append( rstm.getValueAsString(row, "SessionSampleTime__min"    ) ).append("\n");
		sb.append("-- SessionSampleTime__max:       ").append( rstm.getValueAsString(row, "SessionSampleTime__max"    ) ).append("\n");
		sb.append("-- Duration:                     ").append( rstm.getValueAsString(row, "Duration"                 ) ).append("\n");
		
		sb.append("-- calls__sum:                   ").append( format(rstm.getValueAsBigDecimal(row, "calls__sum"                  ))).append("\n");
//		sb.append("-- avg_time_per_call__avg:       ").append( format(rstm.getValueAsBigDecimal(row, "avg_time_per_call__avg"      ))).append("\n");
		sb.append("-- total_time__per_call:         ").append( format(rstm.getValueAsBigDecimal(row, "total_time__per_call"        ))).append("\n");
		sb.append("-- total_time__sum:              ").append( format(rstm.getValueAsBigDecimal(row, "total_time__sum"             ))).append("\n");
//		sb.append("-- avg_rows_per_call__avg:       ").append( format(rstm.getValueAsBigDecimal(row, "avg_rows_per_call__avg"      ))).append("\n");
		sb.append("-- rows__per_call:               ").append( format(rstm.getValueAsBigDecimal(row, "rows__per_call"              ))).append("\n");
		sb.append("-- rows__sum:                    ").append( format(rstm.getValueAsBigDecimal(row, "rows__sum"                   ))).append("\n");
		sb.append("-- cache_hit_pct__avg:           ").append( format(rstm.getValueAsBigDecimal(row, "cache_hit_pct__avg"          ))).append("\n");
//		sb.append("-- shared_blks_hit_per_row__avg: ").append( format(rstm.getValueAsBigDecimal(row, "shared_blks_hit_per_row__avg"))).append("\n");
		sb.append("-- shared_blks_hit__sum:         ").append( format(rstm.getValueAsBigDecimal(row, "shared_blks_hit__sum"        ))).append("\n");
		sb.append("-- shared_blks_read__sum:        ").append( format(rstm.getValueAsBigDecimal(row, "shared_blks_read__sum"       ))).append("\n");
		sb.append("-- shared_blks_dirtied__sum:     ").append( format(rstm.getValueAsBigDecimal(row, "shared_blks_dirtied__sum"    ))).append("\n");
		sb.append("-- shared_blks_written__sum:     ").append( format(rstm.getValueAsBigDecimal(row, "shared_blks_written__sum"    ))).append("\n");
		sb.append("-- local_blks_hit__sum:          ").append( format(rstm.getValueAsBigDecimal(row, "local_blks_hit__sum"         ))).append("\n");
		sb.append("-- local_blks_read__sum:         ").append( format(rstm.getValueAsBigDecimal(row, "local_blks_read__sum"        ))).append("\n");
		sb.append("-- local_blks_dirtied__sum:      ").append( format(rstm.getValueAsBigDecimal(row, "local_blks_dirtied__sum"     ))).append("\n");
		sb.append("-- local_blks_written__sum:      ").append( format(rstm.getValueAsBigDecimal(row, "local_blks_written__sum"     ))).append("\n");
		sb.append("-- temp_blks_read__sum:          ").append( format(rstm.getValueAsBigDecimal(row, "temp_blks_read__sum"         ))).append("\n");
		sb.append("-- temp_blks_written__sum:       ").append( format(rstm.getValueAsBigDecimal(row, "temp_blks_written__sum"      ))).append("\n");
		sb.append("-- blks_read_time__sum:          ").append( format(rstm.getValueAsBigDecimal(row, "blks_read_time__sum"         ))).append("\n");
		sb.append("-- blks_write_time__sum:         ").append( format(rstm.getValueAsBigDecimal(row, "blks_write_time__sum"        ))).append("\n");
		sb.append("-----------------------------------------------------------------------------------------------\n");
		sb.append(StringEscapeUtils.escapeHtml4(rstm.getValueAsString(row, "query")));

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
				"Top Slow SQL Statements are presented here (ordered by: total_time__sum) <br>" +
				"<br>" +
				"Postgres Source table is 'pg_stat_statements'. <br>" +
				"PCS Source table is 'CmPgStatements_diff'. (PCS = Persistent Counter Store) <br>" +
				"The report <i>summarizes</i> (min/max/count/sum/avg) all entries/samples from the <i>CmPgStatements_diff</i> table grouped by 'datname, usename, queryid'. <br>" +
				"Typically the column name <i>postfix</i> will tell you what aggregate function was used. <br>" +
				"SQL Text will also be displayed in a separate table below the <i>summary</i> table.<br>" +
				"");

		// Columns description
		rstm.setColumnDescription("datname"                     , "Database name this Statement was executed in.");
		rstm.setColumnDescription("usename"                     , "Username that executed this Statement.");
		rstm.setColumnDescription("queryid"                     , "Internal hash code, computed from the statement's parse tree");
		rstm.setColumnDescription("samples_count"               , "Number of entries for this 'datname, usename, queryid' in the report period");
		rstm.setColumnDescription("SessionSampleTime__min"      , "First entry was sampled for this entry");
		rstm.setColumnDescription("SessionSampleTime__max"      , "Last entry was sampled for this entry");
		rstm.setColumnDescription("Duration"                    , "Start/end time presented as HH:MM:SS, so we can see if this entry is just for a short time or if it spans over a long period of time.");
		rstm.setColumnDescription("CmSampleMs__sum"             , "Number of milliseconds this object has been available for sampling");

		rstm.setColumnDescription("calls__sum"                  , "Number of times executed");
		rstm.setColumnDescription("avg_time_per_call__avg"      , "Average Execution Time per call              (Algorithm: total_time / calls)");
		rstm.setColumnDescription("total_time__sum"             , "Total time spent in the statement, in milliseconds");
		rstm.setColumnDescription("avg_rows_per_call__avg"      , "Average 'number of rows retrived' per call   (Algorithm: rows / calls)");
		rstm.setColumnDescription("rows_sum"                    , "Total number of rows retrieved or affected by the statement");

		rstm.setColumnDescription("cache_hit_pct__avg"          , "Average blockes found in cache by the statement (Algorithm: 100.0 * shared_blks_hit / (shared_blks_hit + shared_blks_read)");
//		rstm.setColumnDescription("shared_blks_hit_per_row__avg", "Average 'number of cache reads' per call     (Algorithm: shared_blks_hit / calls)");
		rstm.setColumnDescription("shared_blks_hit__sum"        , "Total number of shared block cache hits by the statement");
		rstm.setColumnDescription("shared_blks_read__sum"       , "Total number of shared blocks read by the statement");
		rstm.setColumnDescription("shared_blks_dirtied__sum"    , "Total number of shared blocks dirtied by the statement");
		rstm.setColumnDescription("shared_blks_written__sum"    , "Total number of shared blocks written by the statement");
		
		rstm.setColumnDescription("local_blks_hit__sum"         , "Total number of local block cache hits by the statement");
		rstm.setColumnDescription("local_blks_read__sum"        , "Total number of local blocks read by the statement");
		rstm.setColumnDescription("local_blks_dirtied__sum"     , "Total number of local blocks dirtied by the statement");
		rstm.setColumnDescription("local_blks_written__sum"     , "Total number of local blocks written by the statement");

		rstm.setColumnDescription("temp_blks_read__sum"         , "Total number of temp blocks read by the statement");
		rstm.setColumnDescription("temp_blks_written__sum"      , "Total number of temp blocks written by the statement");

		rstm.setColumnDescription("blks_read_time__sum"         , "Total time the statement spent reading blocks, in milliseconds (if track_io_timing is enabled, otherwise zero)");
		rstm.setColumnDescription("blks_write_time__sum"        , "Total time the statement spent writing blocks, in milliseconds (if track_io_timing is enabled, otherwise zero)");

		rstm.setColumnDescription("query"                       , "Text of a representative statement");
	}

	@Override
	public List<ReportingIndexEntry> getReportingIndexes()
	{
		List<ReportingIndexEntry> list = new ArrayList<>();
		
		list.add(new ReportingIndexEntry("CmPgIndexes_diff"  , "dbname", "schemaname", "relname", "indexrelname"));
		list.add(new ReportingIndexEntry("CmPgIndexes_abs"   , "dbname", "schemaname", "relname", "indexrelname", "SessionSampleTime"));

		list.add(new ReportingIndexEntry("CmPgIndexesIo_diff", "dbname", "schemaname", "relname", "indexrelname"));
		list.add(new ReportingIndexEntry("CmPgIndexesIo_abs" , "dbname", "schemaname", "relname", "indexrelname", "SessionSampleTime"));

		list.add(new ReportingIndexEntry("CmPgTables_diff"   , "dbname", "schemaname", "relname"));
		list.add(new ReportingIndexEntry("CmPgTables_abs"    , "dbname", "schemaname", "relname", "SessionSampleTime"));

		list.add(new ReportingIndexEntry("CmPgTablesIo_diff" , "dbname", "schemaname", "relname"));
		list.add(new ReportingIndexEntry("CmPgTablesIo_abs"  , "dbname", "schemaname", "relname", "SessionSampleTime"));

		return list;
	}

	@Override
	public String[] getMandatoryTables()
	{
		return new String[] { "CmPgStatements_diff" };
	}

	@Override
	public void create(DbxConnection conn, String srvName, Configuration pcsSavedConf, Configuration localConf)
	{
//		int topRows = localConf.getIntProperty(this.getClass().getSimpleName()+".top", 20);
		int topRows = getTopRows();

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


		// Skip DB name(s)
		String skip_dbname   = "";
		List<String> skipDbnameList   = StringUtil.parseCommaStrToList(Configuration.getCombinedConfiguration().getProperty(PROPKEY_skip_dbname_csv, DEFAULT_skip_dbname_csv), true);
		if ( ! skipDbnameList.isEmpty() )
			skip_dbname = "  and [datname] not in(" + StringUtil.toCommaStrQuoted("'", skipDbnameList) + ") \n";

		// Skip user name(s)
		String skip_username = "";
		List<String> skipUsernameList = StringUtil.parseCommaStrToList(Configuration.getCombinedConfiguration().getProperty(PROPKEY_skip_username_csv, DEFAULT_skip_username_csv), true);
		if ( ! skipUsernameList.isEmpty() )
			skip_username = "  and [usename] not in(" + StringUtil.toCommaStrQuoted("'", skipUsernameList) + ") \n";

		// GET ALL INFO
		String sql = getCmDiffColumnsAsSqlComment("CmPgStatements")
			    + "select top " + topRows + " \n"
			    + "     [datname] \n"
			    + "    ,[usename] \n"
			    + "    ,[queryid] \n"
			    + "    ,min([" + col_query + "])                                                             as [query] \n"
			    + " \n"                                                                                      
			    + "    ,cast('' as varchar(512))                                                             as [calls__chart] \n"
			    + "    ,sum([calls])                                                                         as [calls__sum] \n"

			    + "    ,cast('' as varchar(512))                                                             as [total_time__chart] \n"
			    + "    ,CAST( sum([total_time])        AS DECIMAL(20,0) )                                    as [total_time__sum] \n"					// 0 decimals for readability
//			    + "    ,CAST( avg([avg_time_per_call]) AS DECIMAL(19,1) )                                    as [avg_time_per_call__avg] \n"			// 1 decimal (so we can track sub millisecond executions)
			    + "    ,CAST( sum([total_time])          * 1.0 / nullif(sum([calls]), 0) AS DECIMAL(19,1) )  as [total_time__per_call] \n"			// 1 decimal (so we can track sub millisecond executions)

			    + "    ,cast('' as varchar(512))                                                             as [rows__chart] \n"
			    + "    ,sum([rows])                                                                          as [rows__sum] \n"
//			    + "    ,avg([avg_rows_per_call])                                                             as [avg_rows_per_call__avg] \n"
			    + "    ,CAST( sum([rows]) / nullif(sum([calls]), 0) AS DECIMAL(19,1) )                       as [rows__per_call] \n"
			    + " \n"
				+ "    ,cast('' as varchar(512))                                                             as [cache_hit_pct__chart] \n"
			    + "    ,CAST( avg(100.0 * [shared_blks_hit] / nullif([shared_blks_hit] + [shared_blks_read], 0)) AS DECIMAL(5,1) ) as [cache_hit_pct__avg] \n"

				+ "    ,cast('' as varchar(512))                                                             as [logical_reads__chart] \n"
				+ "    ,sum([shared_blks_hit] + [shared_blks_read])                                          as [logical_reads__sum] \n"
				+ "    ,CAST( sum([shared_blks_hit] + [shared_blks_read]) * 1.0 / nullif(sum([calls]), 0) AS DECIMAL(19,1) )      as [logical_reads__per_call] \n"
			    
				+ "    ,cast('' as varchar(512))                                                             as [logical_reads_mb__chart] \n"
				+ "    ,cast(sum([shared_blks_hit] + [shared_blks_read])/128 as bigint)                      as [logical_reads_mb__sum] \n"
				+ "    ,CAST( sum([shared_blks_hit] + [shared_blks_read]) / 128.0 / nullif(sum([calls]), 0) AS DECIMAL(19,1) )      as [logical_reads_mb__per_call] \n"
			    
				+ "    ,cast('' as varchar(512))                                                             as [shared_blks_hit__chart] \n"
			    + "    ,sum([shared_blks_hit])                                                               as [shared_blks_hit__sum] \n"
			    + "    ,CAST( sum([shared_blks_hit])     * 1.0 / nullif(sum([calls]), 0) AS DECIMAL(19,1) )  as [shared_blks_hit__per_call] \n"

			    + "    ,cast('' as varchar(512))                                                             as [shared_blks_read__chart] \n"
			    + "    ,sum([shared_blks_read])                                                              as [shared_blks_read__sum] \n"
			    + "    ,CAST( sum([shared_blks_read])    * 1.0 / nullif(sum([calls]), 0) AS DECIMAL(19,1) )  as [shared_blks_read__per_call] \n"

			    + "    ,cast('' as varchar(512))                                                             as [shared_blks_dirtied__chart] \n"
			    + "    ,sum([shared_blks_dirtied])                                                           as [shared_blks_dirtied__sum] \n"
			    + "    ,CAST( sum([shared_blks_dirtied]) * 1.0 / nullif(sum([calls]), 0) AS DECIMAL(19,1) )  as [shared_blks_dirtied__per_call] \n"

			    + "    ,cast('' as varchar(512))                                                             as [shared_blks_written__chart] \n"
			    + "    ,sum([shared_blks_written])                                                           as [shared_blks_written__sum] \n"
			    + "    ,CAST( sum([shared_blks_written]) * 1.0 / nullif(sum([calls]), 0) AS DECIMAL(19,1) )  as [shared_blks_written__per_call] \n"
			    + " \n"
			    + "    ,sum([local_blks_hit])                                                                as [local_blks_hit__sum] \n"
			    + "    ,sum([local_blks_read])                                                               as [local_blks_read__sum] \n"
			    + "    ,sum([local_blks_dirtied])                                                            as [local_blks_dirtied__sum] \n"
			    + "    ,sum([local_blks_written])                                                            as [local_blks_written__sum] \n"
			    + " \n"
			    + "    ,cast('' as varchar(512))                                                             as [temp_blks_read__chart] \n"
			    + "    ,sum([temp_blks_read])                                                                as [temp_blks_read__sum] \n"
			    + "    ,CAST( sum([temp_blks_read])      * 1.0 / nullif(sum([calls]), 0) AS DECIMAL(19,1) )  as [temp_blks_read__per_call] \n"
			    
			    + "    ,cast('' as varchar(512))                                                             as [temp_blks_written__chart] \n"
			    + "    ,sum([temp_blks_written])                                                             as [temp_blks_written__sum] \n"
			    + "    ,CAST( sum([temp_blks_written])   * 1.0 / nullif(sum([calls]), 0) AS DECIMAL(19,1) )  as [temp_blks_written__per_call] \n"
			    + " \n"
			    + "    ,cast('' as varchar(512))                                                             as [logical_reads_per_row__chart] \n"
			    + "    ,CAST( sum([shared_blks_hit] + [shared_blks_read]) * 1.0 / nullif(sum([rows]), 0) AS DECIMAL(19,1) )      as [logical_reads__per_row] \n"
			    
			    + "    ,sum([blk_read_time])                                                                 as [blks_read_time__chart] \n"
			    + "    ,sum([blk_read_time])                                                                 as [blks_read_time__sum] \n"
			    + "    ,CAST( sum([blk_read_time])       * 1.0 / nullif(sum([calls]), 0) AS DECIMAL(19,1) )  as [blk_read_time__per_call] \n"
			    
			    + "    ,sum([blk_write_time])                                                                as [blks_write_time__chart] \n"
			    + "    ,sum([blk_write_time])                                                                as [blks_write_time__sum] \n"
			    + "    ,CAST( sum([blk_write_time])      * 1.0 / nullif(sum([calls]), 0) AS DECIMAL(19,1) )  as [blk_write_time__per_call] \n"
			    + " \n"
			    + "    ,count(*)                                                                             as [samples_count] \n"
			    + "    ,min([SessionSampleTime])                                                             as [SessionSampleTime__min] \n"
			    + "    ,max([SessionSampleTime])                                                             as [SessionSampleTime__max] \n"
			    + "    ,cast('' as varchar(30))                                                              as [Duration] \n"
			    + "    ,sum([CmSampleMs])                                                                    as [CmSampleMs__sum] \n"
			    + "from [CmPgStatements_diff] x \n"
//			    + "where [usename] != 'postgres' \n"
			    + "where [calls] > 0 \n"                          // do only get records that has been executed (even if presented in the "Statement cache")
			    + skip_dbname
			    + skip_username
				+ getReportPeriodSqlWhere()
			    + "group by [datname], [usename], [queryid] \n"
			    + "order by [total_time__sum] desc \n"
			    + "";

		_shortRstm = executeQuery(conn, sql, false, "Top SQL");
		if (_shortRstm == null)
		{
			_shortRstm = ResultSetTableModel.createEmpty("Top SQL");
			return;
		}
		else
		{
			// Highlight sort column
			_shortRstm.setHighlightSortColumns("total_time__sum");

			// Describe the table
			setSectionDescription(_shortRstm);

			// set duration
			setDurationColumn(_shortRstm, "SessionSampleTime_min", "SessionSampleTime_max", "Duration");
						
			
			//--------------------------------------------------------------------------------
			// Mini Chart on "..."
			//--------------------------------------------------------------------------------
			String whereKeyColumn = "datname, usename, queryid"; 

			_miniChartJsList.add(SparklineHelper.createSparkline(conn, this, _shortRstm, 
					SparkLineParams.create       (DataSource.CounterModel)
					.setHtmlChartColumnName      ("calls__chart")
					.setHtmlWhereKeyColumnName   (whereKeyColumn)
					.setDbmsTableName            ("CmPgStatements_diff")
					.setDbmsSampleTimeColumnName ("SessionSampleTime")
					.setDbmsDataValueColumnName  ("calls")   
					.setDbmsWhereKeyColumnName   (whereKeyColumn)
					.setSparklineTooltipPostfix  ("Number of times executed in below period")
					.validate()));

			_miniChartJsList.add(SparklineHelper.createSparkline(conn, this, _shortRstm, 
					SparkLineParams.create       (DataSource.CounterModel)
					.setHtmlChartColumnName      ("total_time__chart")
					.setHtmlWhereKeyColumnName   (whereKeyColumn)
					.setDbmsTableName            ("CmPgStatements_diff")
					.setDbmsSampleTimeColumnName ("SessionSampleTime")
//					.setDbmsDataValueColumnName  ("total_time")
					.setDbmsDataValueColumnName  ("sum(1.0*[total_time]) / nullif(sum([calls]), 0)").setGroupDataAggregationType(AggType.USER_PROVIDED)
					.setDbmsWhereKeyColumnName   (whereKeyColumn)
					.setDecimalScale(3) // just use whole numbers for this
//					.setDecimalScale(0) // just use whole numbers for this
					.setSparklineTooltipPostfix  ("Total time spent in the statement in below period")
					.validate()));

			_miniChartJsList.add(SparklineHelper.createSparkline(conn, this, _shortRstm, 
					SparkLineParams.create       (DataSource.CounterModel)
					.setHtmlChartColumnName      ("rows__chart")
					.setHtmlWhereKeyColumnName   (whereKeyColumn)
					.setDbmsTableName            ("CmPgStatements_diff")
					.setDbmsSampleTimeColumnName ("SessionSampleTime")
//					.setDbmsDataValueColumnName  ("rows")
					.setDbmsDataValueColumnName  ("sum(1.0*[rows]) / nullif(sum([calls]), 0)").setGroupDataAggregationType(AggType.USER_PROVIDED)
					.setDbmsWhereKeyColumnName   (whereKeyColumn)
					.setDecimalScale(1) // just use whole numbers for this
//					.setDecimalScale(0) // just use whole numbers for this
					.setSparklineTooltipPostfix  ("Total 'rows' in the statement in below period")
					.validate()));

			_miniChartJsList.add(SparklineHelper.createSparkline(conn, this, _shortRstm, 
					SparkLineParams.create       (DataSource.CounterModel)
					.setHtmlChartColumnName      ("cache_hit_pct__chart")
					.setHtmlWhereKeyColumnName   (whereKeyColumn)
					.setDbmsTableName            ("CmPgStatements_diff")
					.setDbmsSampleTimeColumnName ("SessionSampleTime")
					.setDbmsDataValueColumnName  ("100.0 * sum([shared_blks_hit]) / nullif(sum([shared_blks_hit] + [shared_blks_read]), 0)").setGroupDataAggregationType(AggType.USER_PROVIDED)
					.setDbmsWhereKeyColumnName   (whereKeyColumn)
					.setDecimalScale(2) // just use whole numbers for this
					.setSparklineTooltipPostfix  ("Total 'pages read' from CACHE and DISK for the statement in below period")
					.validate()));

			_miniChartJsList.add(SparklineHelper.createSparkline(conn, this, _shortRstm, 
					SparkLineParams.create       (DataSource.CounterModel)
					.setHtmlChartColumnName      ("logical_reads__chart")
					.setHtmlWhereKeyColumnName   (whereKeyColumn)
					.setDbmsTableName            ("CmPgStatements_diff")
					.setDbmsSampleTimeColumnName ("SessionSampleTime")
//					.setDbmsDataValueColumnName  ("sum([shared_blks_hit] + [shared_blks_read])").setGroupDataAggregationType(AggType.USER_PROVIDED)
					.setDbmsDataValueColumnName  ("sum(1.0*[shared_blks_hit] + [shared_blks_read]) / nullif(sum([calls]), 0)").setGroupDataAggregationType(AggType.USER_PROVIDED)
					.setDbmsWhereKeyColumnName   (whereKeyColumn)
//					.setDecimalScale(0) // just use whole numbers for this
					.setDecimalScale(1)
					.setSparklineTooltipPostfix  ("Total 'pages read' from CACHE and DISK for the statement in below period")
					.validate()));

			_miniChartJsList.add(SparklineHelper.createSparkline(conn, this, _shortRstm, 
					SparkLineParams.create       (DataSource.CounterModel)
					.setHtmlChartColumnName      ("logical_reads_mb__chart")
					.setHtmlWhereKeyColumnName   (whereKeyColumn)
					.setDbmsTableName            ("CmPgStatements_diff")
					.setDbmsSampleTimeColumnName ("SessionSampleTime")
//					.setDbmsDataValueColumnName  ("sum([shared_blks_hit] + [shared_blks_read])").setGroupDataAggregationType(AggType.USER_PROVIDED)
					.setDbmsDataValueColumnName  ("sum(1.0*[shared_blks_hit] + [shared_blks_read]) / 128.0 / nullif(sum([calls]), 0)").setGroupDataAggregationType(AggType.USER_PROVIDED)
					.setDbmsWhereKeyColumnName   (whereKeyColumn)
//					.setDecimalScale(0) // just use whole numbers for this
					.setDecimalScale(1)
					.setSparklineTooltipPostfix  ("Total 'pages read' from CACHE and DISK for the statement in below period")
					.validate()));

			_miniChartJsList.add(SparklineHelper.createSparkline(conn, this, _shortRstm, 
					SparkLineParams.create       (DataSource.CounterModel)
					.setHtmlChartColumnName      ("shared_blks_hit__chart")
					.setHtmlWhereKeyColumnName   (whereKeyColumn)
					.setDbmsTableName            ("CmPgStatements_diff")
					.setDbmsSampleTimeColumnName ("SessionSampleTime")
//					.setDbmsDataValueColumnName  ("shared_blks_hit")
					.setDbmsDataValueColumnName  ("sum(1.0*[shared_blks_hit]) / nullif(sum([calls]), 0)").setGroupDataAggregationType(AggType.USER_PROVIDED)
					.setDbmsWhereKeyColumnName   (whereKeyColumn)
//					.setDecimalScale(0) // just use whole numbers for this
					.setDecimalScale(1)
					.setSparklineTooltipPostfix  ("Total 'pages read' from CACHE for the statement in below period")
					.validate()));

			_miniChartJsList.add(SparklineHelper.createSparkline(conn, this, _shortRstm, 
					SparkLineParams.create       (DataSource.CounterModel)
					.setHtmlChartColumnName      ("shared_blks_read__chart")
					.setHtmlWhereKeyColumnName   (whereKeyColumn)
					.setDbmsTableName            ("CmPgStatements_diff")
					.setDbmsSampleTimeColumnName ("SessionSampleTime")
//					.setDbmsDataValueColumnName  ("shared_blks_read")
					.setDbmsDataValueColumnName  ("sum(1.0*[shared_blks_read]) / nullif(sum([calls]), 0)").setGroupDataAggregationType(AggType.USER_PROVIDED)
					.setDbmsWhereKeyColumnName   (whereKeyColumn)
//					.setDecimalScale(0) // just use whole numbers for this
					.setDecimalScale(1)
					.setSparklineTooltipPostfix  ("Total 'pages read' from DISK for the statement in below period")
					.validate()));

			_miniChartJsList.add(SparklineHelper.createSparkline(conn, this, _shortRstm, 
					SparkLineParams.create       (DataSource.CounterModel)
					.setHtmlChartColumnName      ("shared_blks_dirtied__chart")
					.setHtmlWhereKeyColumnName   (whereKeyColumn)
					.setDbmsTableName            ("CmPgStatements_diff")
					.setDbmsSampleTimeColumnName ("SessionSampleTime")
//					.setDbmsDataValueColumnName  ("shared_blks_dirtied")
					.setDbmsDataValueColumnName  ("sum(1.0*[shared_blks_dirtied]) / nullif(sum([calls]), 0)").setGroupDataAggregationType(AggType.USER_PROVIDED)
					.setDbmsWhereKeyColumnName   (whereKeyColumn)
//					.setDecimalScale(0) // just use whole numbers for this
					.setDecimalScale(1)
					.setSparklineTooltipPostfix  ("Total 'pages' changed by the statement in below period")
					.validate()));

			_miniChartJsList.add(SparklineHelper.createSparkline(conn, this, _shortRstm, 
					SparkLineParams.create       (DataSource.CounterModel)
					.setHtmlChartColumnName      ("shared_blks_written__chart")
					.setHtmlWhereKeyColumnName   (whereKeyColumn)
					.setDbmsTableName            ("CmPgStatements_diff")
					.setDbmsSampleTimeColumnName ("SessionSampleTime")
//					.setDbmsDataValueColumnName  ("shared_blks_written")
					.setDbmsDataValueColumnName  ("sum(1.0*[shared_blks_written]) / nullif(sum([calls]), 0)").setGroupDataAggregationType(AggType.USER_PROVIDED)
					.setDbmsWhereKeyColumnName   (whereKeyColumn)
//					.setDecimalScale(0) // just use whole numbers for this
					.setDecimalScale(1)
					.setSparklineTooltipPostfix  ("Total 'pages' synchronously written to disk by the statement in below period")
					.validate()));

			
			
			_miniChartJsList.add(SparklineHelper.createSparkline(conn, this, _shortRstm, 
					SparkLineParams.create       (DataSource.CounterModel)
					.setHtmlChartColumnName      ("temp_blks_read__chart")
					.setHtmlWhereKeyColumnName   (whereKeyColumn)
					.setDbmsTableName            ("CmPgStatements_diff")
					.setDbmsSampleTimeColumnName ("SessionSampleTime")
//					.setDbmsDataValueColumnName  ("temp_blks_read")
					.setDbmsDataValueColumnName  ("sum(1.0*[temp_blks_read]) / nullif(sum([calls]), 0)").setGroupDataAggregationType(AggType.USER_PROVIDED)
					.setDbmsWhereKeyColumnName   (whereKeyColumn)
//					.setDecimalScale(0) // just use whole numbers for this
					.setDecimalScale(1)
					.setSparklineTooltipPostfix  ("Total 'temp_blks_read' by the statement in below period")
					.validate()));

			_miniChartJsList.add(SparklineHelper.createSparkline(conn, this, _shortRstm, 
					SparkLineParams.create       (DataSource.CounterModel)
					.setHtmlChartColumnName      ("temp_blks_written__chart")
					.setHtmlWhereKeyColumnName   (whereKeyColumn)
					.setDbmsTableName            ("CmPgStatements_diff")
					.setDbmsSampleTimeColumnName ("SessionSampleTime")
//					.setDbmsDataValueColumnName  ("temp_blks_written")
					.setDbmsDataValueColumnName  ("sum(1.0*[temp_blks_written]) / nullif(sum([calls]), 0)").setGroupDataAggregationType(AggType.USER_PROVIDED)
					.setDbmsWhereKeyColumnName   (whereKeyColumn)
//					.setDecimalScale(0) // just use whole numbers for this
					.setDecimalScale(1)
					.setSparklineTooltipPostfix  ("Total 'temp_blks_written' by the statement in below period")
					.validate()));

			_miniChartJsList.add(SparklineHelper.createSparkline(conn, this, _shortRstm, 
					SparkLineParams.create       (DataSource.CounterModel)
					.setHtmlChartColumnName      ("blks_read_time__chart")
					.setHtmlWhereKeyColumnName   (whereKeyColumn)
					.setDbmsTableName            ("CmPgStatements_diff")
					.setDbmsSampleTimeColumnName ("SessionSampleTime")
//					.setDbmsDataValueColumnName  ("blk_read_time")
					.setDbmsDataValueColumnName  ("sum(1.0*[blk_read_time]) / nullif(sum([calls]), 0)").setGroupDataAggregationType(AggType.USER_PROVIDED)
					.setDbmsWhereKeyColumnName   (whereKeyColumn)
//					.setDecimalScale(0) // just use whole numbers for this
					.setDecimalScale(1)
					.setSparklineTooltipPostfix  ("Total 'blk_read_time' by the statement in below period")
					.validate()));

			_miniChartJsList.add(SparklineHelper.createSparkline(conn, this, _shortRstm, 
					SparkLineParams.create       (DataSource.CounterModel)
					.setHtmlChartColumnName      ("blks_write_time__chart")
					.setHtmlWhereKeyColumnName   (whereKeyColumn)
					.setDbmsTableName            ("CmPgStatements_diff")
					.setDbmsSampleTimeColumnName ("SessionSampleTime")
//					.setDbmsDataValueColumnName  ("blk_write_time")
					.setDbmsDataValueColumnName  ("sum(1.0*[blk_write_time]) / nullif(sum([calls]), 0)").setGroupDataAggregationType(AggType.USER_PROVIDED)
					.setDbmsWhereKeyColumnName   (whereKeyColumn)
//					.setDecimalScale(0) // just use whole numbers for this
					.setDecimalScale(1)
					.setSparklineTooltipPostfix  ("Total 'blk_write_time' by the statement in below period")
					.validate()));

			_miniChartJsList.add(SparklineHelper.createSparkline(conn, this, _shortRstm, 
					SparkLineParams.create       (DataSource.CounterModel)
					.setHtmlChartColumnName      ("logical_reads_per_row__chart")
					.setHtmlWhereKeyColumnName   (whereKeyColumn)
					.setDbmsTableName            ("CmPgStatements_diff")
					.setDbmsSampleTimeColumnName ("SessionSampleTime")
//					.setDbmsDataValueColumnName  ("blk_write_time")
					.setDbmsDataValueColumnName  ("sum([shared_blks_hit] + [shared_blks_read]) * 1.0 / nullif(sum([rows]), 0)").setGroupDataAggregationType(AggType.USER_PROVIDED)
					.setDbmsWhereKeyColumnName   (whereKeyColumn)
//					.setDecimalScale(0) // just use whole numbers for this
					.setDecimalScale(1)
//					.setSparklineTooltipPostfix  ("Total 'blk_write_time' by the statement in below period")
					.validate()));


			//----------------------------------------------------
			// Get important Optimizer Config parameters
			//----------------------------------------------------
			String getPerfConfigSql = ""
					+ "select \n"
//					+ "     [Category] \n"
					+ "     [ParameterName] \n"
					+ "    ,[NonDefault] \n"
					+ "    ,[CurrentValue] \n"
					+ "    ,[Description] \n"
					+ "    ,[ExtraDescription] \n"
					+ "from [MonSessionDbmsConfig] \n"
					+ "where [SessionStartTime] = (select max([SessionStartTime]) from [MonSessionDbmsConfig]) \n"
					+ "  and [Category] = 'Query Tuning / Planner Cost Constants' \n"
					+ "order by [ParameterName] \n"
					+ "";

			_perfConfigRstm = executeQuery(conn, getPerfConfigSql, true, "PerfConfig");

			
			
			//----------------------------------------------------
			// Create a SQL-Details ResultSet based on values in _shortRstm
			//----------------------------------------------------
			SimpleResultSet srs = new SimpleResultSet();

			srs.addColumn("datname",    Types.VARCHAR,       60, 0);
			srs.addColumn("usename",    Types.VARCHAR,       60, 0);
			srs.addColumn("queryid",    Types.BIGINT,         0, 0);
			srs.addColumn("sparklines", Types.VARCHAR,      512, 0); 
			srs.addColumn("query",      Types.VARCHAR, 1024*128, 0); // this is 'text' in the origin table

			// Position in the "source" _shortRstm table (values we will fetch)
			int pos_datname    = _shortRstm.findColumn("datname");
			int pos_usename    = _shortRstm.findColumn("usename");
			int pos_queryid    = _shortRstm.findColumn("queryid");
			int pos_query      = _shortRstm.findColumn("query");


			ColumnCopyRender msToHMS    = HtmlTableProducer.MS_TO_HMS;
			ColumnCopyRender oneDecimal = HtmlTableProducer.ONE_DECIMAL;
			
			HtmlTableProducer htp = new HtmlTableProducer(_shortRstm, "dsr-sub-table-chart");
			htp.setTableHeaders("Charts at 10 minute interval", "Total;style='text-align:right!important'", "Avg per call;style='text-align:right!important'", "");
			htp.add("call-cnt"     , new ColumnCopyRow().add( new ColumnCopyDef("calls__chart"                 ) ).add(new ColumnCopyDef("calls__sum").setColBold() ).addEmptyCol()                                                                   .addEmptyCol() );
			htp.add("exec-time"    , new ColumnCopyRow().add( new ColumnCopyDef("total_time__chart"            ) ).add(new ColumnCopyDef("total_time__sum", msToHMS)).add(new ColumnCopyDef("total_time__per_call"         , oneDecimal).setColBold()).add(new ColumnStatic("ms")) );
			htp.add("rows"         , new ColumnCopyRow().add( new ColumnCopyDef("rows__chart"                  ) ).add(new ColumnCopyDef("rows__sum"               )).add(new ColumnCopyDef("rows__per_call"               , oneDecimal).setColBold()).add(new ColumnStatic("rows")) );
			htp.add("cache-hit"    , new ColumnCopyRow().add( new ColumnCopyDef("cache_hit_pct__chart"         ) ).add(new EmptyColumn(                            )).add(new ColumnCopyDef("cache_hit_pct__avg"           , oneDecimal).setColBold()).add(new ColumnStatic("%")) );
			htp.add("total-read"   , new ColumnCopyRow().add( new ColumnCopyDef("logical_reads__chart"         ) ).add(new ColumnCopyDef("logical_reads__sum"      )).add(new ColumnCopyDef("logical_reads__per_call"      , oneDecimal).setColBold()).add(new ColumnStatic("pgs")) );
			htp.add("total-read-mb", new ColumnCopyRow().add( new ColumnCopyDef("logical_reads_mb__chart"      ) ).add(new ColumnCopyDef("logical_reads_mb__sum"   )).add(new ColumnCopyDef("logical_reads_mb__per_call"   , oneDecimal).setColBold()).add(new ColumnStatic("mb")) );
			htp.add("tot-read/row" , new ColumnCopyRow().add( new ColumnCopyDef("logical_reads_per_row__chart" ) ).add(new ColumnStatic ("n/a").setColAlign("right")).add(new ColumnCopyDef("logical_reads__per_row"       , oneDecimal).setColBold()).add(new ColumnStatic("pgs")) );
//			htp.add("cache-read"   , new ColumnCopyRow().add( new ColumnCopyDef("shared_blks_hit__chart"       ) ).add(new ColumnCopyDef("shared_blks_hit__sum"    )).add(new ColumnCopyDef("shared_blks_hit__per_call"    , oneDec).setColBold()).add(new ColumnStatic("pgs")) );
			htp.add("phys-read"    , new ColumnCopyRow().add( new ColumnCopyDef("shared_blks_read__chart"      ) ).add(new ColumnCopyDef("shared_blks_read__sum"   )).add(new ColumnCopyDef("shared_blks_read__per_call"   , oneDecimal).setColBold()).add(new ColumnStatic("pgs")) );
			htp.add("dirtied"      , new ColumnCopyRow().add( new ColumnCopyDef("shared_blks_dirtied__chart"   ) ).add(new ColumnCopyDef("shared_blks_dirtied__sum")).add(new ColumnCopyDef("shared_blks_dirtied__per_call", oneDecimal).setColBold()).add(new ColumnStatic("pgs")) );
//			htp.add("written"      , new ColumnCopyRow().add( new ColumnCopyDef("shared_blks_written__chart"   ) ).add(new ColumnCopyDef("shared_blks_written__sum")).add(new ColumnCopyDef("shared_blks_written__per_call", oneDec).setColBold()).add(new ColumnStatic("pgs")) );
			htp.add("tmp-read"     , new ColumnCopyRow().add( new ColumnCopyDef("temp_blks_read__chart"        ) ).add(new ColumnCopyDef("temp_blks_read__sum"     )).add(new ColumnCopyDef("temp_blks_read__per_call"     , oneDecimal).setColBold()).add(new ColumnStatic("pgs")) );
			htp.add("tmp-write"    , new ColumnCopyRow().add( new ColumnCopyDef("temp_blks_written__chart"     ) ).add(new ColumnCopyDef("temp_blks_written__sum"  )).add(new ColumnCopyDef("temp_blks_written__per_call"  , oneDecimal).setColBold()).add(new ColumnStatic("pgs")) );
			htp.validate();

			// loop "data table" and create "sql table" 
			if (pos_datname >= 0 && pos_usename >= 0 && pos_queryid >= 0 && pos_query >= 0)
			{
				for (int r=0; r<_shortRstm.getRowCount(); r++)
				{
					String     datname    = _shortRstm.getValueAsString(r, pos_datname);
					String     usename    = _shortRstm.getValueAsString(r, pos_usename);
					Long       queryid    = _shortRstm.getValueAsLong  (r, pos_queryid);
					String     sqlText    = _shortRstm.getValueAsString(r, pos_query);

					// Grab all SparkLines we defined in 'subTableRowSpec'
					String sparklines = htp.getHtmlTextForRow(r);

					// When using Dictionary Compression the "query" is just a hashId
					// So get the real Query TEXT from the key/value lookup table
					if (hasDictCompCols)
					{
						String hashId = _shortRstm.getValueAsString (r, pos_query);
						try
						{
							sqlText = DictCompression.getValueForHashId(conn, null, "CmPgStatements", "query", hashId);
						} 
						catch (SQLException ex) 
						{
							sqlText = "Problems getting Dictionary Compressed column for tabName='CmPgStatements', colName='query', hashId='" + hashId + "'.";
						}

						// set QUERY text in the original ResultSet
						_shortRstm.setValueAtWithOverride(sqlText, r, pos_query);
					}
					
					// Parse the 'sqlText' and extract Table Names, then get various table and index information
					String tableInfo = getDbmsTableInformationFromSqlText(conn, datname, sqlText, DbUtils.DB_PROD_NAME_POSTGRES);

					// SQL Text
					sqlText = "<xmp>" + sqlText + "</xmp>" + tableInfo;
//					sqlText = "<script type='text/plain' readonly>" + sqlText + "</script>" + tableInfo;
//					sqlText = "<div class='sqltext'>" + StringEscapeUtils.escapeHtml4(sqlText) + "</div>" + tableInfo;

					// add record to SimpleResultSet
					srs.addRow(datname, usename, queryid, sparklines, sqlText);
				}
			}

			// GET SQLTEXT (only)
			try
			{
				// Note the 'srs' is populated when reading above ResultSet from query
				_sqTextRstm = createResultSetTableModel(srs, "Top SQL TEXT", null, false); // DO NOT TRUNCATE COLUMNS
				srs.close();
			}
			catch (SQLException ex)
			{
				setProblemException(ex);
	
				_sqTextRstm = ResultSetTableModel.createEmpty("Top SQL TEXT");
				_logger.warn("Problems getting Top SQL TEXT: " + ex);
			}

		}
	}
}
