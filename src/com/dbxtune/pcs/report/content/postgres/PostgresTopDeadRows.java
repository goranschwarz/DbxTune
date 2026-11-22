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
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import com.dbxtune.cm.postgres.CmPgTables;
import com.dbxtune.gui.ResultSetTableModel;
import com.dbxtune.pcs.report.DailySummaryReportAbstract;
import com.dbxtune.pcs.report.content.IReportChart;
import com.dbxtune.pcs.report.content.IndexAnalysisHtml;
import com.dbxtune.pcs.report.content.SparklineHelper;
import com.dbxtune.pcs.report.content.SparklineHelper.DataSource;
import com.dbxtune.pcs.report.content.SparklineHelper.SparkLineParams;
import com.dbxtune.sql.conn.DbxConnection;
import com.dbxtune.utils.Configuration;

public class PostgresTopDeadRows
extends PostgresAbstract
{
//	private static final Logger _logger = LogManager.getLogger(MethodHandles.lookup().lookupClass());

	private ResultSetTableModel _shortRstm;
	private ResultSetTableModel _autovacuumConfigRstm;
	private List<String>        _miniChartJsList = new ArrayList<>();

	public PostgresTopDeadRows(DailySummaryReportAbstract reportingInstance)
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
		// Get a description of this section, and column names
		sb.append(getSectionDescriptionHtml(_shortRstm, true));

		// Last sample Database Size info
		sb.append("Row Count: " + _shortRstm.getRowCount() + "<br>\n");

		// autovacuum_verdict: a link to the table's advice card below (the full report also opens the box)
		sb.append(_shortRstm.toHtmlTableString("sortable", true, true, null, new ReportEntryTableStringRenderer()
		{
			@Override
			public String cellValue(ResultSetTableModel rstm, int row, int col, String colName, Object objVal, String strVal)
			{
				if ("autovacuum_verdict".equals(colName))
				{
					String key = tableKey(rstm, row);
					if (_autovacuumBoxes.containsKey(key))
					{
						String id = autovacuumCardId(key);
						String onclick = isFullMessageType() ? " onclick=\"var d=document.querySelector('#" + id + " details.dbx-autovacuum'); if (d) d.open = true;\"" : "";
						return "<a href='#" + id + "'" + onclick + ">" + IndexAnalysisHtml.esc(strVal) + "</a>";
					}
				}
				return super.cellValue(rstm, row, col, colName, objVal, strVal);
			}
		}));

		// The full advice (with the ALTER TABLE ... SET and its rollback) for the tables whose autovacuum_verdict is not OK
		// (the same box as in Top SQL > Table Information, where only the tables used by a top statement get it)
		if ( ! _autovacuumBoxes.isEmpty() )
		{
			// NOTE: The boxes are left out in Outlook (collapsible <details>), so are the texts that belong to them. One guard per piece: the box has its own (they can't be nested)
			String ifNotMso = "<!--[if !mso]><!--> \n";
			String endIf    = "<!--<![endif]-->    \n";
			sb.append(ifNotMso);
			sb.append("<br>\n");
			sb.append("<b>Autovacuum advice for the " + _autovacuumBoxes.size() + " table(s) above that are not vacuumed (or analyzed) often enough.</b> ");
			sb.append("Open a box for the numbers of the day, the settings, why, and the <i>Change and Rollback</i> script. Nothing has been changed.<br>\n");
			sb.append(endIf);
			// One card per table, so it is clear which text belongs to which table (inline styles: the mail has no Bootstrap)
			for (Map.Entry<String, String> e : _autovacuumBoxes.entrySet())
			{
				sb.append(ifNotMso)
				  .append("<div id='").append(autovacuumCardId(e.getKey())).append("' style='border:1px solid #d6dbe1; border-left:4px solid #e0a800; border-radius:4px; padding:6px 12px; margin:10px 0; max-width:1220px;'>\n")
				  .append("<b>").append(IndexAnalysisHtml.esc(e.getKey())).append("</b>\n")
				  .append(endIf);
				sb.append(e.getValue());
				sb.append(ifNotMso).append("</div>\n").append(endIf);
			}
			sb.append("<br>\n");
		}

		sb.append(getDbxCentralLinkWithDescForGraphs(false, "Below are 'Dead Rows' of each Database during the day.",
				"CmPgTables_DeadRows"
				));

		_CmPgTables_DeadRows.writeHtmlContent(sb, null, null);

		// Write server level auto vaccum config
		sb.append("<br>\n");
		sb.append("<b>Below is serverwide Auto Vaccuum Parameters.</b><br>\n");
		sb.append("<i>The normal default is to do autovacuum when there is 20% 'dead_rows'... Formula: <code>n_dead_tup > (n_live_tup * autovacuum_vacuum_scale_factor + autovacuum_vacuum_threshold)</code> </i><br>\n");
		sb.append("Autovacuum Tuning Basics <a href='https://www.enterprisedb.com/blog/autovacuum-tuning-basics' target='_blank'>https://www.enterprisedb.com/blog/autovacuum-tuning-basics</a><br>\n");
		sb.append(toHtmlTable(_autovacuumConfigRstm));

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
		return "Dead Rows/Tuples (origin: CmPgTables / pg_stat_user_tables)";
	}

	@Override
	public IssueLevel getIssueLevel()
	{
		// Dead rows in themselves are not a problem. A table that is not vacuumed (or analyzed) often enough is (column 'autovacuum_verdict')
		// It's a tuning finding, not an incident: FYI
		return _tablesWithAutovacuumFindings > 0 ? IssueLevel.FYI : IssueLevel.NONE;
	}

	@Override
	public String getIssueReason()
	{
		return _tablesWithAutovacuumFindings + " table(s) with autovacuum findings";
	}
	private int _tablesWithAutovacuumFindings = 0;

	/** The full Autovacuum box (numbers, settings, findings, Change and Rollback) of each table with a finding: "db.schema.table" -> html */
	private Map<String, String> _autovacuumBoxes = new java.util.LinkedHashMap<>();

	/** "dbname.schemaname.relname" */
	private static String tableKey(ResultSetTableModel rstm, int row)
	{
		return rstm.getValueAsString(row, "dbname") + "." + rstm.getValueAsString(row, "schemaname") + "." + rstm.getValueAsString(row, "relname");
	}
	/** The HTML id of a table's advice card */
	private static String autovacuumCardId(String tableKey)
	{
		return "autovacuum_" + tableKey.replaceAll("[^A-Za-z0-9_]", "_");
	}


	@Override
	public String[] getMandatoryTables()
	{
		return new String[] { "CmPgTables_abs" };
	}

	@Override
	public void create(DbxConnection conn, String srvName, Configuration pcsSavedConf, Configuration localConf)
	{
		int topRows = getTopRows();

//		String sql = ""
//			    + "select top " + topRows + " \n"
//			    + "     [dbname] \n"
//			    + "    ,[schemaname] \n"
//			    + "    ,[relname] \n"
//			    + "    ,[n_dead_tup] \n"
//			    + "    ,cast('' as varchar(512))      as [n_dead_tup__chart] \n"
//			    + "    ,[n_live_tup] \n"
//			    + "    ,cast('' as varchar(512))      as [n_live_tup__chart] \n"
//			    + "    ,[n_tup_ins] \n"
//			    + "    ,cast('' as varchar(512))      as [n_tup_ins__chart] \n"
//			    + "    ,[n_tup_upd] \n"
//			    + "    ,cast('' as varchar(512))      as [n_tup_upd__chart] \n"
//			    + "    ,[n_tup_hot_upd] \n"
//			    + "    ,cast('' as varchar(512))      as [n_tup_hot_upd__chart] \n"
//			    + "    ,[n_tup_del] \n"
//			    + "    ,cast('' as varchar(512))      as [n_tup_del__chart] \n"
//			    + "from [CmPgTables_abs] \n"
//			    + "where [SessionSampleTime] = (select max([SessionSampleTime]) from [CmPgTables_abs]) \n"
//			    + "order by [n_dead_tup] desc \n"
//			    + "";
		 // just to get Column names
		String dummySql = "select * from [CmPgTables_diff] where 1 = 2";
		ResultSetTableModel dummyRstm = executeQuery(conn, dummySql, true, "metadata");

		// n_mod_since_analyze was introduced in Postgres Version: 9.4
		boolean has_n_mod_since_analyze = dummyRstm.hasColumnNoCase("n_mod_since_analyze");

		
		String sql = ""
			    + "select top " + topRows + " \n"
			    + "     [dbname] \n"
			    + "    ,[schemaname] \n"
			    + "    ,[relname] \n"
			    + "    ,cast('' as varchar(1024)) as [autovacuum_verdict] \n" // filled below (PostgresAutovacuumAdvice)

			    // total_kb
			    + "    ,(select max([total_kb]) \n"
			    + "          from [CmPgTables_abs] abs \n"
			    + "          where abs.[SessionSampleTime] = (select max([SessionSampleTime]) from [CmPgTables_abs]) \n"
			    + "            and abs.[dbname]     = diff.[dbname] \n"
			    + "            and abs.[schemaname] = diff.[schemaname] \n"
			    + "            and abs.[relname]    = diff.[relname] \n"
			    + "          ) AS [total_kb] \n"

			    // data_kb
			    + "    ,(select max([data_kb]) \n"
			    + "          from [CmPgTables_abs] abs \n"
			    + "          where abs.[SessionSampleTime] = (select max([SessionSampleTime]) from [CmPgTables_abs]) \n"
			    + "            and abs.[dbname]     = diff.[dbname] \n"
			    + "            and abs.[schemaname] = diff.[schemaname] \n"
			    + "            and abs.[relname]    = diff.[relname] \n"
			    + "          ) AS [data_kb] \n"

			    // index_kb
			    + "    ,(select max([index_kb]) \n"
			    + "          from [CmPgTables_abs] abs \n"
			    + "          where abs.[SessionSampleTime] = (select max([SessionSampleTime]) from [CmPgTables_abs]) \n"
			    + "            and abs.[dbname]     = diff.[dbname] \n"
			    + "            and abs.[schemaname] = diff.[schemaname] \n"
			    + "            and abs.[relname]    = diff.[relname] \n"
			    + "          ) AS [index_kb] \n"

			    // pct_dead  ---->>>  100.0 * n_dead_tup__LAST_ABS / nullif(n_live_tup__LAST_ABS, 0)
			    + "    ,CAST( 100.0 * \n"
			    + "           (select max([n_dead_tup]) \n"
			    + "                from [CmPgTables_abs] abs \n"
			    + "                where abs.[SessionSampleTime] = (select max([SessionSampleTime]) from [CmPgTables_abs]) \n"
			    + "                  and abs.[dbname]     = diff.[dbname] \n"
			    + "                  and abs.[schemaname] = diff.[schemaname] \n"
			    + "                  and abs.[relname]    = diff.[relname] \n"
			    + "           )  \n"
			    + "           /  nullif( \n"
			    + "           (select max([n_live_tup]) \n"
			    + "                from [CmPgTables_abs] abs \n"
			    + "                where abs.[SessionSampleTime] = (select max([SessionSampleTime]) from [CmPgTables_abs]) \n"
			    + "                  and abs.[dbname]     = diff.[dbname] \n"
			    + "                  and abs.[schemaname] = diff.[schemaname] \n"
			    + "                  and abs.[relname]    = diff.[relname] \n"
			    + "           ), 0)  \n"
			    + "         as DECIMAL(9,1) )  \n"
			    + "     AS [pct_dead] \n"

			    // n_live_tup__LAST_ABS
			    + "    ,(select max([n_live_tup]) \n"
			    + "          from [CmPgTables_abs] abs \n"
			    + "          where abs.[SessionSampleTime] = (select max([SessionSampleTime]) from [CmPgTables_abs]) \n"
			    + "            and abs.[dbname]     = diff.[dbname] \n"
			    + "            and abs.[schemaname] = diff.[schemaname] \n"
			    + "            and abs.[relname]    = diff.[relname] \n"
			    + "          ) AS [n_live_tup__LAST_ABS] \n"

			    // n_dead_tup__LAST_ABS
			    + "    ,(select max([n_dead_tup]) \n"
			    + "          from [CmPgTables_abs] abs \n"
			    + "          where abs.[SessionSampleTime] = (select max([SessionSampleTime]) from [CmPgTables_abs]) \n"
			    + "            and abs.[dbname]     = diff.[dbname] \n"
			    + "            and abs.[schemaname] = diff.[schemaname] \n"
			    + "            and abs.[relname]    = diff.[relname] \n"
			    + "          ) AS [n_dead_tup__LAST_ABS] \n"
			    // n_dead_tup / n_live_tup are gauges: the highest value of the day (from _abs; a sum of the diff values meant nothing)
			    + "    ,(select max([n_dead_tup]) \n"
			    + "          from [CmPgTables_abs] abs \n"
			    + "          where abs.[dbname]     = diff.[dbname] \n"
			    + "            and abs.[schemaname] = diff.[schemaname] \n"
			    + "            and abs.[relname]    = diff.[relname] \n"
			    + "          ) AS [n_dead_tup__MAX_TODAY] \n"
			    + "    ,cast('' as varchar(512))      as [n_dead_tup__chart] \n"
			    
			    + "    ,cast('>>>>>>>>>' as varchar(30)) as [extra_info] \n"

			    + "    ,sum([n_tup_del]) AS [n_tup_del__SUM] \n"
			    + "    ,cast('' as varchar(512))      as [n_tup_del__chart] \n"
			    
			    + "    ,sum([n_tup_ins]) AS [n_tup_ins__SUM] \n"
			    + "    ,cast('' as varchar(512))      as [n_tup_ins__chart] \n"
			    
			    + "    ,sum([n_tup_upd]) AS [n_tup_upd__SUM] \n"
			    + "    ,cast('' as varchar(512))      as [n_tup_upd__chart] \n"
			    
			    + "    ,sum([n_tup_hot_upd]) AS [n_tup_hot_upd__SUM] \n"
			    + "    ,cast('' as varchar(512))      as [n_tup_hot_upd__chart] \n"
			    
//			    + "    ,(select max([n_live_tup]) \n"
//			    + "          from [CmPgTables_abs] abs \n"
//			    + "          where abs.[SessionSampleTime] = (select max([SessionSampleTime]) from [CmPgTables_abs]) \n"
//			    + "            and abs.[dbname]     = diff.[dbname] \n"
//			    + "            and abs.[schemaname] = diff.[schemaname] \n"
//			    + "            and abs.[relname]    = diff.[relname] \n"
//			    + "          ) AS [n_live_tup__LAST_ABS] \n"
			    + "    ,(select max([n_live_tup]) \n"
			    + "          from [CmPgTables_abs] abs \n"
			    + "          where abs.[dbname]     = diff.[dbname] \n"
			    + "            and abs.[schemaname] = diff.[schemaname] \n"
			    + "            and abs.[relname]    = diff.[relname] \n"
			    + "          ) AS [n_live_tup__MAX_TODAY] \n"
			    + "    ,cast('' as varchar(512))      as [n_live_tup__chart] \n"

			    + "    ,max([last_autovacuum])        as [last_autovacuum__MAX] \n"
			    + "    ,sum([autovacuum_count])       as [autovacuum_count__SUM] \n"

			    + "    ,max([last_autoanalyze])       as [last_autoanalyze__MAX] \n"
			    + "    ,sum([autoanalyze_count])      as [autoanalyze_count__SUM] \n"

			    // n_mod_since_analyze__LAST_ABS
			    // NOTE: only if we got column 'n_mod_since_analyze'
			    + ( ! has_n_mod_since_analyze ? "" : ""
			    	+ "    ,(select max([n_mod_since_analyze]) \n"
				    + "          from [CmPgTables_abs] abs \n"
				    + "          where abs.[SessionSampleTime] = (select max([SessionSampleTime]) from [CmPgTables_abs]) \n"
				    + "            and abs.[dbname]     = diff.[dbname] \n"
				    + "            and abs.[schemaname] = diff.[schemaname] \n"
				    + "            and abs.[relname]    = diff.[relname] \n"
				    + "          ) AS [n_mod_since_analyze__LAST_ABS] \n"
			    )
			    
			    // pct_analyze  ---->>>  100.0 * n_mod_since_analyze / nullif(n_live_tup__LAST_ABS, 0)
			    // NOTE: only if we got column 'n_mod_since_analyze'
			    + ( ! has_n_mod_since_analyze ? "" : "" 
    			    + "    ,CAST( 100.0 * \n"
    			    + "           (select max([n_mod_since_analyze]) \n"
    			    + "                from [CmPgTables_abs] abs \n"
    			    + "                where abs.[SessionSampleTime] = (select max([SessionSampleTime]) from [CmPgTables_abs]) \n"
    			    + "                  and abs.[dbname]     = diff.[dbname] \n"
    			    + "                  and abs.[schemaname] = diff.[schemaname] \n"
    			    + "                  and abs.[relname]    = diff.[relname] \n"
    			    + "           )  \n"
    			    + "           /  nullif( \n"
    			    + "           (select max([n_live_tup]) \n"
    			    + "                from [CmPgTables_abs] abs \n"
    			    + "                where abs.[SessionSampleTime] = (select max([SessionSampleTime]) from [CmPgTables_abs]) \n"
    			    + "                  and abs.[dbname]     = diff.[dbname] \n"
    			    + "                  and abs.[schemaname] = diff.[schemaname] \n"
    			    + "                  and abs.[relname]    = diff.[relname] \n"
    			    + "           ), 0)  \n"
    			    + "         as DECIMAL(9,1) )  \n"
    			    + "     AS [pct_analyze] \n"
			    )

//			    + "    ,max([last_vacuum])            as [last_vacuum__MAX] \n"
//			    + "    ,sum([vacuum_count])           as [vacuum_count__SUM] \n"

//			    + "    ,max([last_analyze])           as [last_analyze__MAX] \n"
//			    + "    ,sum([analyze_count])          as [analyze_count__SUM] \n"
			    
			    + "from [CmPgTables_diff] diff\n"
			    + "group by [dbname], [schemaname], [relname] \n"
//			    + "order by sum([n_dead_tup]) desc \n"
			    + "order by [n_dead_tup__LAST_ABS] desc \n" 
			    + "";

		_shortRstm = executeQuery(conn, sql, true, "CmPgTables_abs");

		if (_shortRstm != null)
		{
			// Is each table vacuumed (and analyzed) often enough? The same analysis as the 'Autovacuum' box in Table Information
			Map<String, String> serverSettings = PostgresAutovacuumAdvice.readServerSettings(conn);
			for (int r = 0; r < _shortRstm.getRowCount(); r++)
			{
				PostgresAutovacuumAdvice advice = PostgresAutovacuumAdvice.create(conn, serverSettings,
						_shortRstm.getValueAsString(r, "dbname"), _shortRstm.getValueAsString(r, "schemaname"), _shortRstm.getValueAsString(r, "relname"));
				_shortRstm.setValueAtWithOverride(advice.getShortVerdict(), r, "autovacuum_verdict");
				if (advice.hasFindings())
				{
					_tablesWithAutovacuumFindings++;
					_autovacuumBoxes.put(tableKey(_shortRstm, r), advice.toHtml(false));
				}
			}

			// Highlight sort column
			_shortRstm.setHighlightSortColumns("n_dead_tup__LAST_ABS");

			// Describe the table
			setSectionDescription(_shortRstm);
			
			// Mini Chart on "..."
			String whereKeyColumn = "dbname, schemaname, relname"; 

			_miniChartJsList.add(SparklineHelper.createSparkline(conn, this, _shortRstm, 
					SparkLineParams.create       (DataSource.CounterModel)
					.setHtmlChartColumnName      ("n_dead_tup__chart")
					.setHtmlWhereKeyColumnName   (whereKeyColumn)
					.setDbmsTableName            ("CmPgTables_abs" )                // a gauge: the value of each sample, the highest in each period
					.setGroupDataAggregationType (SparklineHelper.AggType.MAX)
					.setDbmsSampleTimeColumnName ("SessionSampleTime")
					.setDbmsDataValueColumnName  ("n_dead_tup")
					.setDbmsWhereKeyColumnName   (whereKeyColumn)
					.setSparklineTooltipPostfix  ("Highest 'n_dead_tup' in below period")
					.validate()));

			_miniChartJsList.add(SparklineHelper.createSparkline(conn, this, _shortRstm, 
					SparkLineParams.create       (DataSource.CounterModel)
					.setHtmlChartColumnName      ("n_tup_del__chart")
					.setHtmlWhereKeyColumnName   (whereKeyColumn)
					.setDbmsTableName            ("CmPgTables_diff")
					.setDbmsSampleTimeColumnName ("SessionSampleTime")
					.setDbmsDataValueColumnName  ("n_tup_del")
					.setDbmsWhereKeyColumnName   (whereKeyColumn)
					.setSparklineTooltipPostfix  ("Number of 'n_tup_del' in below period")
					.validate()));

			_miniChartJsList.add(SparklineHelper.createSparkline(conn, this, _shortRstm, 
					SparkLineParams.create       (DataSource.CounterModel)
					.setHtmlChartColumnName      ("n_tup_ins__chart")
					.setHtmlWhereKeyColumnName   (whereKeyColumn)
					.setDbmsTableName            ("CmPgTables_diff")
					.setDbmsSampleTimeColumnName ("SessionSampleTime")
					.setDbmsDataValueColumnName  ("n_tup_ins")
					.setDbmsWhereKeyColumnName   (whereKeyColumn)
					.setSparklineTooltipPostfix  ("Number of 'n_tup_ins' in below period")
					.validate()));

			_miniChartJsList.add(SparklineHelper.createSparkline(conn, this, _shortRstm, 
					SparkLineParams.create       (DataSource.CounterModel)
					.setHtmlChartColumnName      ("n_tup_upd__chart")
					.setHtmlWhereKeyColumnName   (whereKeyColumn)
					.setDbmsTableName            ("CmPgTables_diff")
					.setDbmsSampleTimeColumnName ("SessionSampleTime")
					.setDbmsDataValueColumnName  ("n_tup_upd")
					.setDbmsWhereKeyColumnName   (whereKeyColumn)
					.setSparklineTooltipPostfix  ("Number of 'n_tup_upd' in below period")
					.validate()));

			_miniChartJsList.add(SparklineHelper.createSparkline(conn, this, _shortRstm, 
					SparkLineParams.create       (DataSource.CounterModel)
					.setHtmlChartColumnName      ("n_tup_hot_upd__chart")
					.setHtmlWhereKeyColumnName   (whereKeyColumn)
					.setDbmsTableName            ("CmPgTables_diff")
					.setDbmsSampleTimeColumnName ("SessionSampleTime")
					.setDbmsDataValueColumnName  ("n_tup_hot_upd")
					.setDbmsWhereKeyColumnName   (whereKeyColumn)
					.setSparklineTooltipPostfix  ("Number of 'n_tup_hot_upd' in below period")
					.validate()));

			_miniChartJsList.add(SparklineHelper.createSparkline(conn, this, _shortRstm, 
					SparkLineParams.create       (DataSource.CounterModel)
					.setHtmlChartColumnName      ("n_live_tup__chart")
					.setHtmlWhereKeyColumnName   (whereKeyColumn)
					.setDbmsTableName            ("CmPgTables_abs" )                // a gauge: the value of each sample, the highest in each period
					.setGroupDataAggregationType (SparklineHelper.AggType.MAX)
					.setDbmsSampleTimeColumnName ("SessionSampleTime")
					.setDbmsDataValueColumnName  ("n_live_tup")
					.setDbmsWhereKeyColumnName   (whereKeyColumn)
					.setSparklineTooltipPostfix  ("Highest 'n_live_tup' in below period")
					.validate()));
		}
		
		String schema = getReportingInstance().getDbmsSchemaName();

		_CmPgTables_DeadRows = createTsLineChart(conn, schema, CmPgTables.CM_NAME, CmPgTables.GRAPH_NAME_DEAD_ROWS, -1, true, null, "Number of Dead Rows per Database(n_dead_tup) per Database");

	
		// Get 'Autovacuum' server config values
		String getAutovacuumConfigSql = ""
				+ "select \n"
//				+ "     [Category] \n"
				+ "     [ParameterName] \n"
				+ "    ,[NonDefault] \n"
				+ "    ,[CurrentValue] \n"
				+ "    ,[Description] \n"
				+ "    ,[ExtraDescription] \n"
				+ "from [MonSessionDbmsConfig] \n"
				+ "where [SessionStartTime] = (select max([SessionStartTime]) from [MonSessionDbmsConfig]) \n"
				+ "  and [ParameterName] like 'autovacuum%' \n" // Was: [Category] = 'Autovacuum'. Postgres 19 renamed the category to 'Vacuuming / Automatic Vacuuming' (the same settings)
//				+ "order by [Category], [ParameterName] \n"
				+ "order by [ParameterName] \n"
				+ "";
			
		_autovacuumConfigRstm = executeQuery(conn, getAutovacuumConfigSql, true, "AutovacuumConfig");
	}

	private IReportChart _CmPgTables_DeadRows;

	/**
	 * Set descriptions for the table, and the columns
	 */
	private void setSectionDescription(ResultSetTableModel rstm)
	{
		if (rstm == null)
			return;
		
		// Section description
		rstm.setDescription(
				"Information from last collector sample from the table <code>CmPgTables_abs</code><br>" +
				"The column <code>autovacuum_verdict</code> tells if the table is vacuumed (and analyzed) often enough over the whole day, from its own and the server's autovacuum settings. " +
				"The details, and the suggested <code>ALTER TABLE ... SET (...)</code> with its rollback, are in the 'Autovacuum' part of the Table Information (for the tables in the Top SQL statements).<br>" +
				"");
	}
}
