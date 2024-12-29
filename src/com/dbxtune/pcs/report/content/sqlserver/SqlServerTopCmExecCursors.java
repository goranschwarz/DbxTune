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
package com.dbxtune.pcs.report.content.sqlserver;

import java.io.IOException;
import java.io.Writer;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

import org.apache.log4j.Logger;

import com.dbxtune.gui.ResultSetTableModel;
import com.dbxtune.pcs.DictCompression;
import com.dbxtune.pcs.report.DailySummaryReportAbstract;
import com.dbxtune.pcs.report.content.SparklineHelper;
import com.dbxtune.pcs.report.content.SparklineHelper.AggType;
import com.dbxtune.pcs.report.content.SparklineHelper.DataSource;
import com.dbxtune.pcs.report.content.SparklineHelper.SparkLineParams;
import com.dbxtune.sql.conn.DbxConnection;
import com.dbxtune.utils.Configuration;

public class SqlServerTopCmExecCursors
extends SqlServerAbstract
{
	private static Logger _logger = Logger.getLogger(SqlServerTopCmExecCursors.class);

	private ResultSetTableModel _shortRstm;
	private ExecutionPlanCollection _planCollection;
	private List<String>        _miniChartJsList = new ArrayList<>();

	public SqlServerTopCmExecCursors(DailySummaryReportAbstract reportingInstance)
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
		return false;
	}

//	@Override
//	public void writeShortMessageText(Writer w)
//	throws IOException
//	{
//	}

	@Override
	public void writeMessageText(Writer sb, MessageType messageType)
	throws IOException
	{
		// Get a description of this section, and column names
		sb.append(getSectionDescriptionHtml(_shortRstm, true));

//		sb.append("Row Count: " + _shortRstm.getRowCount() + "<br>\n");
		sb.append("Row Count: " + _shortRstm.getRowCount() + "&emsp;&emsp; To change number of <i>top</i> records, set property <code>" + getTopRowsPropertyName() + "=##</code><br>\n");
		sb.append(toHtmlTable(_shortRstm));

		// Write HTML/JavaScript Code for the Execution Plan...
		if (isFullMessageType())
		{
			if (_planCollection != null)
				_planCollection.writeMessageText(sb);
		}
	
		// Write JavaScript code for CPU SparkLine
		if (isFullMessageType())
		{
			for (String str : _miniChartJsList)
				sb.append(str);
		}
	}

	@Override
	public String getSubject()
	{
		return "Top Cursor Calls (order by: worker_time, origin: CmExecCursors/dm_exec_cursors_stats)";
	}

	@Override
	public boolean hasIssueToReport()
	{
		return false; // even if we found entries, do NOT indicate this as a Problem or Issue
	}


	@Override
	public String[] getMandatoryTables()
	{
		return new String[] { "CmExecCursors_abs" };
	}

	@Override
	public void create(DbxConnection conn, String srvName, Configuration pcsSavedConf, Configuration localConf)
	{
		 // just to get Column names
		String dummySql = "select * from [CmExecCursors_abs] where 1 = 2";
		ResultSetTableModel dummyRstm = executeQuery(conn, dummySql, false, "metadata");
		if (dummyRstm == null)
		{
			String msg = "Table 'CmExecCursors_abs' did not exist. So Performance Counters for this hasn't been sampled during this period.";

			//addMessage(msg);
			setProblemException(new Exception(msg));

			_shortRstm = ResultSetTableModel.createEmpty("CmExecCursors");
			return;
		}

//		String col_total_worker_time__sum               = !dummyRstm.hasColumnNoCase("total_worker_time"              ) ? "" : "    ,sum([total_worker_time])               as [total_worker_time__sum]                 \n"; 


		int topRows = getTopRows();

		String orderByCol = "[worker_time_ms__sum]";

		// Check if table "CmPgStatements_abs" has Dictionary Compressed Columns (any columns ends with "$dcc$")
		boolean hasDictCompCols = false;
		try {
			hasDictCompCols = DictCompression.hasCompressedColumnNames(conn, null, "CmExecCursors_abs");
		} catch (SQLException ex) {
			_logger.error("Problems checking for Dictionary Compressed Columns in table 'CmExecCursors_abs'.", ex);
		}
		
		String col_SqlText = "SqlText";
		if (hasDictCompCols)
			col_SqlText = "SqlText$dcc$";


		String sql = getCmDiffColumnsAsSqlComment("CmExecCursors")
			    + "select top " + topRows + " \n"
			    + "     max([name])                            as [name] \n"
			    + "    ,max([properties])                      as [properties] \n"
			    + "    ,cast('' as varchar(512))               as [samples__chart] \n"
			    + "    ,count(*)                               as [samples__count] \n"
			    + "    \n"
			    + "    ,cast('' as varchar(512))               as [worker_time__chart] \n"
			    + "    ,sum([worker_time]/1000.0)              as [worker_time_ms__sum] \n"    // note: this should actually be MAX on the LAST cursor_id... but lets do that later

//			    + "    ,sum([fetch_buffer_start])              as [fetch_buffer_start__sum] \n"

				+ "    ,cast('' as varchar(512))               as [reads__chart] \n"
			    + "    ,sum([reads])                           as [reads__sum] \n"          // note: this should actually be MAX on the LAST cursor_id... but lets do that later

			    + "    ,cast('' as varchar(512))               as [writes__chart] \n"
			    + "    ,sum([writes])                          as [writes__sum] \n"         // note: this should actually be MAX on the LAST cursor_id... but lets do that later

			    + "    ,sum([dormant_duration])                as [dormant_duration__sum] \n" // note: this should actually be MAX on the LAST cursor_id... but lets do that later
			    
			    + "    ,[sql_handle] \n"
			    + "    ,max([exec_sql_text])                   as [exec_sql_text] \n"

			    + "    ,min([SessionSampleTime])               as [SessionSampleTime__min] \n"
			    + "    ,max([SessionSampleTime])               as [SessionSampleTime__max] \n"
			    + "    ,cast('' as varchar(30))                as [Duration] \n"
			    + "    \n"
				+ "from [CmExecCursors_abs] \n"
				+ getReportPeriodSqlWhere()
				+ "group by [sql_handle] \n"
				+ "order by " + orderByCol + " desc \n"
			    + "";
		
		
		_shortRstm = executeQuery(conn, sql, false, "CmExecCursors");
		if (_shortRstm == null)
		{
			_shortRstm = ResultSetTableModel.createEmpty("CmExecCursors");
			return;
		}
		else
		{
			// Highlight sort column
			String orderByCol_noBrackets = orderByCol.replace("[", "").replace("]", "");
			_shortRstm.setHighlightSortColumns(orderByCol_noBrackets);

			// Describe the table
			setSectionDescription(_shortRstm);

			setDurationColumn(_shortRstm, "SessionSampleTime__min", "SessionSampleTime__max", "Duration");

			// get Dictionary Compressed values for column: SqlText
			if (hasDictCompCols)
			{
				updateDictionaryCompressedColumn(_shortRstm, conn, null, "CmExecQueryStats", "SqlText", null);
			}

//			// - Get all "plann_handle" in table '_shortRstm'
//			// - Get the Execution Plan all the "plann_handle"s
//			// - In the table substitute the "plann_handle"s with a link that will display the XML Plan on the HTML Page
//			_planCollection = new ExecutionPlanCollection(this, _shortRstm, this.getClass().getSimpleName());
//			_planCollection.getPlans(conn, null, "plan_handle");
//			_planCollection.substituteWithLinks(null, "plan_handle", "ExecPlan", "view plan", "--not-found--"); // this needs to be first, no modification of plan_handle cell content
//			_planCollection.substituteWithLinks(null, "plan_handle");                                         // this needs to be "last", since it changes of 'plan_handle' cell content
			

			//-----------------------------------------------
			// Mini Chart on "..."
			//-----------------------------------------------
			String whereKeyColumn = "sql_handle"; 

			_miniChartJsList.add(SparklineHelper.createSparkline(conn, this, _shortRstm, 
					SparkLineParams.create       (DataSource.CounterModel)
					.setHtmlChartColumnName      ("samples__chart")
					.setHtmlWhereKeyColumnName   (whereKeyColumn)
					.setDbmsTableName            ("CmExecCursors_abs")
					.setDbmsSampleTimeColumnName ("SessionSampleTime")
					.setDbmsDataValueColumnName  ("1").setDbmsDataValueColumnNameIsExpression(true)
					.setDbmsWhereKeyColumnName   (whereKeyColumn)
					.validate()));

			_miniChartJsList.add(SparklineHelper.createSparkline(conn, this, _shortRstm, 
					SparkLineParams.create       (DataSource.CounterModel)
					.setHtmlChartColumnName      ("worker_time__chart")
					.setHtmlWhereKeyColumnName   (whereKeyColumn)
					.setDbmsTableName            ("CmExecCursors_abs")
					.setDbmsSampleTimeColumnName ("SessionSampleTime")
//					.setDbmsDataValueColumnName  ("worker_time")
//					.setDbmsDataValueColumnName  ("sum([worker_time]/1000.0) / nullif(sum([execution_count]), 0)").setGroupDataAggregationType(AggType.USER_PROVIDED)
					.setDbmsDataValueColumnName  ("sum([worker_time]/1000.0)").setGroupDataAggregationType(AggType.USER_PROVIDED)
//					.setSparklineTooltipPostfix  ("Average 'worker_time' in in milliseconds for below period")
//					.setSparklineTooltipPostfix  ("Total 'worker_time' in in milliseconds for below period")
					.setDbmsWhereKeyColumnName   (whereKeyColumn)
					.validate()));

			_miniChartJsList.add(SparklineHelper.createSparkline(conn, this, _shortRstm, 
					SparkLineParams.create       (DataSource.CounterModel)
					.setHtmlChartColumnName      ("reads__chart")
					.setHtmlWhereKeyColumnName   (whereKeyColumn)
					.setDbmsTableName            ("CmExecCursors_abs")
					.setDbmsSampleTimeColumnName ("SessionSampleTime")
					.setDbmsDataValueColumnName  ("reads")
					.setDbmsWhereKeyColumnName   (whereKeyColumn)
					.validate()));

			_miniChartJsList.add(SparklineHelper.createSparkline(conn, this, _shortRstm, 
					SparkLineParams.create       (DataSource.CounterModel)
					.setHtmlChartColumnName      ("writes__chart")
					.setHtmlWhereKeyColumnName   (whereKeyColumn)
					.setDbmsTableName            ("CmExecCursors_abs")
					.setDbmsSampleTimeColumnName ("SessionSampleTime")
					.setDbmsDataValueColumnName  ("writes")
					.setDbmsWhereKeyColumnName   (whereKeyColumn)
					.validate()));
		}
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
				"Top Cursors (and it's SQL Text)...  (ordered by: worker_time__sum) <br>" +
				"Note: The information in 'dm_exec_cursors_stats' is only available when the cursor is <i>active</i>.<br>" +
				"So we will/might miss cursors, if they wasn't <b>active</b> while we <i>sampled</i> values! <br>" +
				"<br>" +
				"Note: worker_time__sum, reads__sum, writes__sum, dormant_duration__sum: is a SUM() but should really be a MAX() of each individual cursor_id... so values will be higher than they really are... fixing this in a <i>later</i> relase.<br>" +
				"<br>" +
				"SqlServer Source table is 'dm_exec_cursors_stats'. <br>" +
				"PCS Source table is 'CmExecCursors_abs'. (PCS = Persistent Counter Store) <br>" +
				"");
	}
}

