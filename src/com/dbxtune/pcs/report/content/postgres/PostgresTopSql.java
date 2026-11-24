/*******************************************************************************
 * Copyright (C) 2010-2027 Goran Schwarz
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
import java.lang.invoke.MethodHandles;
import java.sql.SQLException;
import java.sql.Types;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import org.apache.commons.text.StringEscapeUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.h2.tools.SimpleResultSet;

import com.dbxtune.central.llm.LlmSqlContextBuilder;
import com.dbxtune.gui.ResultSetTableModel;
import com.dbxtune.gui.ResultSetTableModel.TableStringRenderer;
import com.dbxtune.pcs.DictCompression;
import com.dbxtune.pcs.report.DailySummaryReportAbstract;
import com.dbxtune.pcs.report.content.SparklineHelper;
import com.dbxtune.pcs.report.content.TopSqlOverview;
import com.dbxtune.pcs.report.content.SparklineHelper.AggType;
import com.dbxtune.pcs.report.content.SparklineHelper.DataSource;
import com.dbxtune.pcs.report.content.SparklineHelper.SparkLineParams;
import com.dbxtune.sql.conn.DbxConnection;
import com.dbxtune.utils.Configuration;
import com.dbxtune.utils.DbUtils;
import com.dbxtune.utils.HtmlTableProducer;
import com.dbxtune.utils.HtmlTableProducer.ColumnCopyDef;
import com.dbxtune.utils.HtmlTableProducer.ColumnCopyRender;
import com.dbxtune.utils.HtmlTableProducer.ColumnCopyRow;
import com.dbxtune.utils.HtmlTableProducer.ColumnStatic;
import com.dbxtune.utils.HtmlTableProducer.EmptyColumn;
import com.dbxtune.utils.StringUtil;

public class PostgresTopSql
extends PostgresAbstract
{
	private static final Logger _logger = LogManager.getLogger(MethodHandles.lookup().lookupClass());

	private ResultSetTableModel _shortRstm;
	private ResultSetTableModel _sqTextRstm;
	private ResultSetTableModel _perfConfigRstm;
//	private Exception           _problem = null;
	private List<String>        _miniChartJsList = new ArrayList<>();

	public static final String PROPKEY_skip_dbname_csv   = "PostgresTopSql.skip.dbname.csv";
	public static final String DEFAULT_skip_dbname_csv   = null;

	public static final String PROPKEY_skip_username_csv = "PostgresTopSql.skip.username.csv";
	public static final String DEFAULT_skip_username_csv = null;

	/** One section per measure (as SQL Server and ASE): each with its own top statements */
	public enum ReportType
	{
		OVERVIEW  ("largest_pct"),
		TOTAL_TIME("total_time__sum"),
		DISK_READS("shared_blks_read__sum"),
		TEMP      ("temp_blks_written__sum"),
		WAL       ("wal_mb__sum"),
		CALLS     ("calls__sum"),
		PLAN_TIME ("plan_time__sum");

		/** The column the section is ordered by (highlighted in the table) */
		final String orderByCol;
		ReportType(String orderByCol) { this.orderByCol = orderByCol; }

		/** The section's subject: also its anchor in the report (the Overview links to the sections) */
		String subject() { return "Top SQL Statements - " + name() + " (order by: " + orderByCol + ", origin: pg_stat_statements)"; }
	};

	private ReportType  _reportType;
	private TopSqlOverview _overview;  // OVERVIEW: the shares of all the statements (see TopSqlOverview)
	private String      _orderByCol;    // the column the section is ordered by (highlighted in the table)
	private String     _measureText;   // what the section is about, in words
	private String     _noRowsText;    // when no statement has anything of it

	public PostgresTopSql(DailySummaryReportAbstract reportingInstance, ReportType reportType)
	{
		super(reportingInstance);
		_reportType = reportType;
		_orderByCol = reportType.orderByCol;

		if      (ReportType.OVERVIEW  .equals(reportType)) { _measureText = "their largest share of all the statements' time, disk reads, temp, WAL, calls or planning time"; _noRowsText = "No statements were found."; }
		else if (ReportType.TOTAL_TIME.equals(reportType)) { _measureText = "total execution time";                                           _noRowsText = "No statements were found."; }
		else if (ReportType.DISK_READS.equals(reportType)) { _measureText = "blocks read outside shared_buffers (shared_blks_read)";        _noRowsText = "No statement read any block outside shared_buffers in the period."; }
		else if (ReportType.TEMP      .equals(reportType)) { _measureText = "temp blocks written (sorts and hashes that did not fit in work_mem)"; _noRowsText = "No statement wrote any temp blocks in the period (all sorts and hashes fit in work_mem)."; }
		else if (ReportType.WAL       .equals(reportType)) { _measureText = "WAL written";                                                    _noRowsText = "No statement wrote any WAL in the period (or Postgres is older than 13: pg_stat_statements has no WAL counters)."; }
		else if (ReportType.CALLS     .equals(reportType)) { _measureText = "number of calls";                                                _noRowsText = "No statements were found."; }
		else if (ReportType.PLAN_TIME .equals(reportType)) { _measureText = "planning time";                                                  _noRowsText = "No planning time was recorded in the period (Postgres 13 and above, with <code>pg_stat_statements.track_planning = on</code>)."; }
		else throw new IllegalArgumentException("Unhandled reportType='" + reportType + "'.");
	}

	@Override
	public boolean hasMinimalMessageText()
	{
		return false;
	}

	@Override
	public boolean hasShortMessageText()
	{
		// The mail has the OVERVIEW and the TOTAL_TIME section (as SQL Server: CPU_TIME), the other sections are in the full report
		return ReportType.OVERVIEW.equals(_reportType) || ReportType.TOTAL_TIME.equals(_reportType);
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
			sb.append(_noRowsText + "<br>\n");
		}
		else if (ReportType.OVERVIEW.equals(_reportType))
		{
			// The overview: one small table, the details are in the sections it links to
			_overview.writeHtml(sb, this, getTopRows());
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
				sb.append("Row Count: " + _shortRstm.getRowCount() + " (the top " + getTopRows() + " on " + _measureText + ")"
						+ "&emsp;&emsp; To change number of <i>top</i> records, set property <code>" + getTopRowsPropertyName() + "=##</code><br>\n");
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
									+ "data-bs-toggle='modal' "
									+ "data-bs-target='#dbx-view-sqltext-dialog' "
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

				// Open in the first section (TOTAL_TIME), closed in the others (the page gets very long otherwise)
				sb.append("<br>\n");
				sb.append("<details" + (ReportType.TOTAL_TIME.equals(_reportType) ? " open" : "") + "> \n");
				sb.append("<summary>Details for above Statements, including SQL Text (click to open/close) </summary> \n");
				
				sb.append("<br>\n");
				sb.append("SQL Text by queryid, Row Count: " + _sqTextRstm.getRowCount() + " (This is the same SQL Text as the in the above table, but without all counter details).<br>\n");
				sb.append(toHtmlTable(_sqTextRstm));
//				sb.append(_sqTextRstm.toHtmlTableString("sortable sparklines-table", false, false, null, new ReportEntryTableStringRenderer()));

				sb.append("\n");
				sb.append("</details> \n");
			}

			
			// Write server level configuration relevant for this section (once: in the TOTAL_TIME section)
			if (ReportType.TOTAL_TIME.equals(_reportType) && _perfConfigRstm != null)
			{
				sb.append("<br>\n");
				sb.append("<b>Below is serverwide 'Query Tuning / Planner Cost Constants' Parameters.</b><br>\n");
				sb.append("<i>One parameter to look at is <code>random_page_cost</code>... If you have fast disk (SSD), it's probably a good idea to set this to <code>1.1</code>, which makes the optimizer more likely to choose indexes over table scans.</i><br>\n");
				sb.append("Please search the Postgres Documentation and Internet for <code>random_page_cost</code> to make your own decisions!<br>\n");
				sb.append("One good place is <a href='https://www.interdb.jp/pg' target='_blank'>https://www.interdb.jp/pg -- The Internals of PostgreSQL</a><br>\n");

				sb.append(toHtmlTable(_perfConfigRstm));
			}
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
		return _reportType.subject();
	}

	@Override
	public boolean hasIssueToReport()
	{
		return false; // even if we found entries, do NOT indicate this as a Problem or Issue
	}

	@Override
	public String getLlmDigest()
	{
		// The statement is named by queryid (+ datname, usename); NOT the 'query' column
		return llmDigestRows(_shortRstm, 10, "queryid", "datname", "usename", "calls__sum", "total_time__sum", "total_time__per_call",
				"rows__sum", "cache_hit_pct__avg", "logical_reads__per_call", "shared_blks_read__sum", "temp_blks_written__sum",
				"wal_mb__sum", "plan_time__sum", "blk_read_time__per_call",
				"largest_pct", "pct_time", "pct_disk_reads", "pct_temp", "pct_wal", "pct_calls", "pct_plan_time"); // the OVERVIEW section
	}

	/**
	 * Set descriptions for the table, and the columns
	 */
	private boolean _noPlanCounts  = false; // pg_stat_statements.track_planning is off: no 'plans' counts
	private boolean _trackPlanning = false; // pg_stat_statements.track_planning = on: the 'plans' counts can be used
	private String  _planCacheMode = null;  // the setting plan_cache_mode (null: not known)

	private void setSectionDescription(ResultSetTableModel rstm)
	{
		if (rstm == null)
			return;
		
		// Section description
		rstm.setDescription(
				"The top " + getTopRows() + " SQL Statements on <b>" + _measureText + "</b> (ordered by: " + _orderByCol + ") <br>" +
				"There is one section like this for each of: total time, disk reads, temp written, WAL (Postgres 13 and above), calls " +
				"and planning time (Postgres 13 and above, when <code>pg_stat_statements.track_planning = on</code>). A statement can be in several of them.<br>" +
				"<br>" +
				"Postgres Source table is 'pg_stat_statements'. <br>" +
				"PCS Source table is 'CmPgStatements_diff'. (PCS = Persistent Counter Store) <br>" +
				"The report <i>summarizes</i> (min/max/count/sum/avg) all entries/samples from the <i>CmPgStatements_diff</i> table grouped by 'datname, usename, queryid'. <br>" +
				"Typically the column name <i>postfix</i> will tell you what aggregate function was used. <br>" +
				"SQL Text will also be displayed in a separate table below the <i>summary</i> table.<br>" +
				(_noPlanCounts ? "<br><b>Note</b>: 'Planned' (how often the plan is reused, in the Info column) is not shown: there are no planning counts, <code>pg_stat_statements.track_planning</code> is off (the default). "
						+ "Turn it on to see if prepared statements reuse their plans (it has a small cost on very busy statements).<br>" : "") +
				(_planCacheMode != null && ! "auto".equalsIgnoreCase(_planCacheMode.trim()) ? "<br><b>Note</b>: <code>plan_cache_mode = " + _planCacheMode + "</code> (default: auto): "
						+ ("force_custom_plan".equalsIgnoreCase(_planCacheMode.trim()) ? "prepared statements are planned for each call's values (no generic plan)." : "prepared statements always use a generic plan (one plan for all values).") + "<br>" : "") +
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

		rstm.setColumnDescription("wal_mb__sum"                 , "WAL written by the statement, in MB (Postgres 13 and above)");
		rstm.setColumnDescription("plan_time__sum"              , "Time spent planning the statement, in milliseconds (0 when pg_stat_statements.track_planning is off)");
		rstm.setColumnDescription("plans__sum"                  , "Number of times the statement was planned (0 when pg_stat_statements.track_planning is off). Compared with calls: is the plan reused?");
		rstm.setColumnDescription("generic_plan_calls__sum"     , "Number of calls that used a generic plan (the same plan for all parameter values)");
		rstm.setColumnDescription("custom_plan_calls__sum"      , "Number of calls that used a custom plan (planned for the parameter values of that call)");
	}

	@Override
	public List<ReportingIndexEntry> getReportingIndexes()
	{
		List<ReportingIndexEntry> list = new ArrayList<>();

		// The collector stores EVERY statement in EVERY sample (most rows have calls = 0): the sections only read the rows with calls > 0.
		// Without this index each section reads the whole table (prod recording 2026-11-21: 2.47M rows, 11% with calls: 42 s -> 3.7 s per section)
		list.add(new ReportingIndexEntry("CmPgStatements_diff", "calls"));

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

		// Postgres 17 renamed 'blk_read_time' -> 'shared_blk_read_time' and 'blk_write_time' -> 'shared_blk_write_time'
		// CmPgStatements saves the columns with the names from pg_stat_statements, so use the one the table has
		String col_blk_read_time  = "blk_read_time";
		String col_blk_write_time = "blk_write_time";
		ResultSetTableModel dummyRstm = executeQuery(conn, "select * from [CmPgStatements_diff] where 1 = 2", false, "metadata");
		if (dummyRstm != null && ! dummyRstm.hasColumn("blk_read_time") && dummyRstm.hasColumn("shared_blk_read_time"))
		{
			col_blk_read_time  = "shared_blk_read_time";
			col_blk_write_time = "shared_blk_write_time";
		}


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

		// This section's top ## on its measure: time, physical reads, temp written, WAL (13+), calls, planning time (13+, pg_stat_statements.track_planning)
		Set<String> stmtCols = getRecordedColumns(conn, "CmPgStatements_diff");
		boolean hasWal  = stmtCols.contains("wal_bytes");
		boolean hasPlan = stmtCols.contains("total_plan_time");
		boolean hasPlans      = stmtCols.contains("plans");                                                           // 13+ (counted when pg_stat_statements.track_planning = on)
		boolean hasGenericCnt = stmtCols.contains("generic_plan_calls") && stmtCols.contains("custom_plan_calls");    // newer pg_stat_statements

		//------------------------------------------------------------------------
		// OVERVIEW: each statement's share of all the statements' time, disk reads, temp, WAL, calls and planning time (see TopSqlOverview)
		//------------------------------------------------------------------------
		if (ReportType.OVERVIEW.equals(_reportType))
		{
			List<TopSqlOverview.Measure> measures = List.of(
				new TopSqlOverview.Measure("time"      , "sum([total_time])"                        , ReportType.TOTAL_TIME.subject(), "TOTAL_TIME", false, "Share of all the statements' total time, in percent"),
				new TopSqlOverview.Measure("disk_reads", "sum([shared_blks_read])"                  , ReportType.DISK_READS.subject(), "DISK_READS", true , "Share of all the statements' disk reads (shared_blks_read: blocks read outside shared_buffers), in percent"),
				new TopSqlOverview.Measure("temp"      , "sum([temp_blks_written])"                 , ReportType.TEMP      .subject(), "TEMP"      , true , "Share of all the statements' temp blocks written, in percent"),
				new TopSqlOverview.Measure("wal"       , hasWal  ? "sum([wal_bytes])"       : null  , ReportType.WAL       .subject(), "WAL"       , true , "Share of all the statements' WAL written, in percent (Postgres 13 and above)"),
				new TopSqlOverview.Measure("calls"     , "sum([calls])"                             , ReportType.CALLS     .subject(), "CALLS"     , false, "Share of all the statements' calls, in percent"),
				new TopSqlOverview.Measure("plan_time" , hasPlan ? "sum([total_plan_time])" : null  , ReportType.PLAN_TIME .subject(), "PLAN_TIME" , true , "Share of all the statements' planning time, in percent (Postgres 13 and above, pg_stat_statements.track_planning = on)"));

			_overview = TopSqlOverview.create(this, conn, "[CmPgStatements_diff]",
					"where [calls] > 0 \n" + skip_dbname + skip_username + getReportPeriodSqlWhere(),
					"[datname], [usename], [queryid]", "queryid", "min([" + col_query + "]) as [query]", "query", measures, topRows);
			_shortRstm = _overview.getRstm();

			// The query text is "Dictionary Compressed": get the real text
			if (hasDictCompCols)
			{
				int pos_query = _shortRstm.findColumn("query");
				for (int r = 0; r < _shortRstm.getRowCount(); r++)
				{
					try { _shortRstm.setValueAtWithOverride(DictCompression.getValueForHashId(conn, null, "CmPgStatements", "query", _shortRstm.getValueAsString(r, pos_query)), r, pos_query); }
					catch (SQLException ex) { _logger.warn("Problems getting the Dictionary Compressed 'query' for the Top SQL Overview.", ex); }
				}
			}

			_overview.setDescription("time, disk reads (shared_blks_read), temp written, WAL, calls and planning time",
					"the statements pg_stat_statements kept (setting <code>pg_stat_statements.max</code>), in the databases and users that are not skipped");
			return;
		}

		// No WAL / planning counters recorded (Postgres before 13): nothing to show in that section
		if ( (ReportType.WAL.equals(_reportType) && !hasWal) || (ReportType.PLAN_TIME.equals(_reportType) && !hasPlan) )
		{
			_shortRstm = ResultSetTableModel.createEmpty("Top SQL");
			return;
		}

		// Only statements that have some of the measure (no zero rows in "disk reads", "temp" ...)
		String having = "";
		if      (ReportType.DISK_READS.equals(_reportType)) having = "having sum([shared_blks_read])  > 0 \n";
		else if (ReportType.TEMP      .equals(_reportType)) having = "having sum([temp_blks_written]) > 0 \n";
		else if (ReportType.WAL       .equals(_reportType)) having = "having sum([wal_bytes])         > 0 \n";
		else if (ReportType.PLAN_TIME .equals(_reportType)) having = "having sum([total_plan_time])   > 0 \n";

		// GET ALL INFO
		String sql = getCmDiffColumnsAsSqlComment("CmPgStatements")
			    + "select top " + topRows + " \n"
			    + "     [datname] \n"
			    + "    ,[usename] \n"
			    + "    ,[queryid] \n"
			    + "    ,min([" + col_query + "])                                                             as [query] \n"
			    + " \n"
			    + (hasWal  ? "    ,CAST( sum([wal_bytes]) / 1048576.0 AS DECIMAL(19,1) )                                as [wal_mb__sum] \n" : "")
			    + (hasPlan ? "    ,CAST( sum([total_plan_time]) AS DECIMAL(20,0) )                                      as [plan_time__sum] \n" : "")
			    + (hasPlans ? "    ,sum([plans])                                                                         as [plans__sum] \n" : "")
			    + (hasGenericCnt ? "    ,sum([generic_plan_calls])                                                            as [generic_plan_calls__sum] \n"
			                     + "    ,sum([custom_plan_calls])                                                             as [custom_plan_calls__sum] \n" : "")
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
			    
			    + "    ,sum([" + col_blk_read_time + "])                                                                 as [blks_read_time__chart] \n"
			    + "    ,sum([" + col_blk_read_time + "])                                                                 as [blks_read_time__sum] \n"
			    + "    ,CAST( sum([" + col_blk_read_time + "])       * 1.0 / nullif(sum([calls]), 0) AS DECIMAL(19,1) )  as [blk_read_time__per_call] \n"
			    
			    + "    ,sum([" + col_blk_write_time + "])                                                                as [blks_write_time__chart] \n"
			    + "    ,sum([" + col_blk_write_time + "])                                                                as [blks_write_time__sum] \n"
			    + "    ,CAST( sum([" + col_blk_write_time + "])      * 1.0 / nullif(sum([calls]), 0) AS DECIMAL(19,1) )  as [blk_write_time__per_call] \n"
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
			    + having
			    + "order by [" + _orderByCol + "] desc, [datname], [usename], [queryid] \n"   // tie-break on the key: the OVERVIEW does the same
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
			_shortRstm.setHighlightSortColumns(_orderByCol);

			// Planning counts at all in the period? No: pg_stat_statements.track_planning is off (the default).
			// (Decided for ALL statements: a statement that was prepared before the period and reused all day has 0 plans, that is the best case, not "unknown")
			// The settings are stored at each collector start (MonSessionDbmsConfig): the last one. Not stored: is any statement planned in the period?
			_trackPlanning = false;
			_planCacheMode = null;
			if (hasPlans)
			{
				ResultSetTableModel cfg = executeQuery(conn, "select [ParameterName], [CurrentValue] from [MonSessionDbmsConfig] \n"
						+ "where [SessionStartTime] = (select max([SessionStartTime]) from [MonSessionDbmsConfig]) \n"
						+ "  and [ParameterName] in ('pg_stat_statements.track_planning', 'plan_cache_mode') \n", true, "TopSqlPlanConfig");
				String trackPlanning = null;
				for (int r = 0; cfg != null && r < cfg.getRowCount(); r++)
				{
					if ("plan_cache_mode".equals(cfg.getValueAsString(r, 0))) _planCacheMode = cfg.getValueAsString(r, 1);
					else                                                      trackPlanning  = cfg.getValueAsString(r, 1);
				}
				if (trackPlanning != null)
					_trackPlanning = "on".equalsIgnoreCase(trackPlanning.trim());
				else
				{
					ResultSetTableModel anyPlans = executeQuery(conn, "select top 1 [plans] from [CmPgStatements_diff] where [plans] > 0 \n" + getReportPeriodSqlWhere(), true, "TopSqlAnyPlans");
					_trackPlanning = anyPlans != null && anyPlans.getRowCount() > 0;
				}
			}
			_noPlanCounts = hasPlans && !_trackPlanning;

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
					.setDbmsDataValueColumnName  ("sum(1.0*[" + col_blk_read_time + "]) / nullif(sum([calls]), 0)").setGroupDataAggregationType(AggType.USER_PROVIDED)
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
					.setDbmsDataValueColumnName  ("sum(1.0*[" + col_blk_write_time + "]) / nullif(sum([calls]), 0)").setGroupDataAggregationType(AggType.USER_PROVIDED)
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

			srs.addColumn("Info",       Types.VARCHAR,  1024*64, 0); // one narrow column: database, user, queryid, LLM advice (as the ASE Info column)
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

					// "Get LLM Optimization Advice" link - plain hyperlink (not inline JS) since this report can be e-mailed
					String llmDdlContext = LlmSqlContextBuilder.buildDdlContext(conn, datname, sqlText, DbUtils.DB_PROD_NAME_POSTGRES);
					String llmAdviceLink = LlmSqlContextBuilder.buildAdviceLinkHtml(getReportingInstance().getDbxCentralPublicBaseUrl(), sqlText, llmDdlContext, DbUtils.DB_PROD_NAME_POSTGRES, getReportingInstance().getDbmsVersionStr(), getReportingInstance().getServerName());

					// SQL Text
					sqlText = "<xmp>" + sqlText + "</xmp>" + tableInfo;
//					sqlText = "<script type='text/plain' readonly>" + sqlText + "</script>" + tableInfo;
//					sqlText = "<div class='sqltext'>" + StringEscapeUtils.escapeHtml4(sqlText) + "</div>" + tableInfo;

					// Plan reuse: how often the statement was planned per call (pg_stat_statements.track_planning = on), and generic/custom plans
					// (the plan columns are only there on newer Postgres versions: 0 when not recorded)
					long   calls   = _shortRstm.getValueAsLong(r, "calls__sum", false, 0L);
					long   plans   = !_shortRstm.hasColumnNoCase("plans__sum")              ? 0  : _shortRstm.getValueAsLong  (r, "plans__sum"             , false, 0L);
					long   generic = !_shortRstm.hasColumnNoCase("generic_plan_calls__sum") ? 0  : _shortRstm.getValueAsLong  (r, "generic_plan_calls__sum", false, 0L);
					long   custom  = !_shortRstm.hasColumnNoCase("custom_plan_calls__sum")  ? 0  : _shortRstm.getValueAsLong  (r, "custom_plan_calls__sum" , false, 0L);
					double planMs  = !_shortRstm.hasColumnNoCase("plan_time__sum")          ? 0d : _shortRstm.getValueAsDouble(r, "plan_time__sum"         , false, 0d);
					double totMs   = _shortRstm.getValueAsDouble(r, "total_time__sum", false, 0d);
					String planned = null;
					if (_trackPlanning && calls > 0 && plans <= calls * 1.1) // more plans than calls: an older recording, where 'plans' was stored as the total since the statistics reset
					{
						double planPct  = 100.0 * plans / calls;
						int    planShare = totMs > 0 ? (int) Math.round(100.0 * planMs / totMs) : 0;
						planned = (planPct >= 90 ? "every call" : planPct >= 1 ? Math.round(planPct) + "% of calls" : plans > 0 ? "under 1% of calls" : "never")
								+ (planPct < 10 ? " (reused)" : "")
								+ (planShare >= 1 ? ", planning " + planShare + "% of its time" : "");
						if (planPct >= 90 && planShare >= 20) // planned every time, and the planning is a big part: a prepared statement would save it
							planned = "<span style='color:#c2410c;'>" + planned + "</span>";
					}
					String genericPlans = generic + custom <= 0 ? null : Math.round(100.0 * generic / (generic + custom)) + "% of calls";

					// The Info column (the queryid without thousands separators: to copy it, or search for it)
					String info = infoCell(
							  infoValue("Database", null, datname)
							, infoValue("User",     null, usename)
							, infoValue("queryid",  "Hash of the statement's parse tree (pg_stat_statements.queryid): the same statement with other literals gets the same queryid", queryid)
							, infoValue("Planned",  "How often the statement was planned, per call (pg_stat_statements plans / calls). "
									+ "'every call': it is not prepared, or it gets a custom plan each time; if the planning is a big part of its time (orange), a prepared statement (server side) would save it. "
									+ "A low percent: the plan is reused (a prepared statement). The JDBC driver prepares on the server after prepareThreshold (5) executions. "
									+ "A connection pooler in transaction mode (pgBouncer) can stop that, unless max_prepared_statements is set.", planned)
							, infoValue("Generic plan", "Calls that used a generic plan: one plan for all parameter values (the other calls were planned for their values, a custom plan). "
									+ "Postgres tries 5 custom plans first, then uses the generic plan if it does not look more expensive. "
									+ "If a statement is fast for most values but very slow for some (max time far above the average), the generic plan may be bad for those values: "
									+ "setting plan_cache_mode = force_custom_plan (for the user or the session) plans each call for its values.", genericPlans)
							, llmAdviceLink);

					// add record to SimpleResultSet
					srs.addRow(info, sparklines, sqlText);
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
