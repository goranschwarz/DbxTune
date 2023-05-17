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
package com.asetune.pcs.report.content.postgres;

import java.io.IOException;
import java.io.Writer;
import java.util.ArrayList;
import java.util.List;

import com.asetune.gui.ResultSetTableModel;
import com.asetune.pcs.report.DailySummaryReportAbstract;
import com.asetune.pcs.report.content.IReportChart;
import com.asetune.pcs.report.content.SparklineHelper;
import com.asetune.pcs.report.content.SparklineHelper.DataSource;
import com.asetune.pcs.report.content.SparklineHelper.SparkLineParams;
import com.asetune.sql.conn.DbxConnection;
import com.asetune.utils.Configuration;

public class PostgresTopDeadRows
extends PostgresAbstract
{
//	private static Logger _logger = Logger.getLogger(PostgresDbSize.class);

	private ResultSetTableModel _shortRstm;
	private ResultSetTableModel _autovacuumConfigRstm;
	private List<String>        _miniChartJsList = new ArrayList<>();

	public PostgresTopDeadRows(DailySummaryReportAbstract reportingInstance)
	{
		super(reportingInstance);
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
		sb.append(toHtmlTable(_shortRstm));
		
		sb.append(getDbxCentralLinkWithDescForGraphs(false, "Below are 'Dead Rows' of each Database during the day.",
				"CmPgTables_DeadRows"
				));

		_CmPgTables_DeadRows.writeHtmlContent(sb, null, null);

		// Write server level auto vaccum config
		sb.append("<br>\n");
		sb.append("<b>Below is serverwide Auto Vaccuum Parameters.</b><br>\n");
		sb.append("<i>The normal default is to do autovacuum when there is 20% 'dead_rows'... Formula: <code>n_dead_tup > (n_live_tup * autovacuum_vacuum_scale_factor + autovacuum_vacuum_threshold)</code> </i><br>\n");
		sb.append("Autovacuum Tuning Basics <a href='https://www.2ndquadrant.com/en/blog/autovacuum-tuning-basics/' target='_blank'>https://www.2ndquadrant.com/en/blog/autovacuum-tuning-basics/</a><br>\n");
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
	public boolean hasIssueToReport()
	{
		return false; // even if we found entries, do NOT indicate this as a Problem or Issue
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
			    + "    ,sum([n_dead_tup]) AS [n_dead_tup__SUM_TODAY] \n"
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
			    + "    ,sum([n_live_tup]) AS [n_live_tup__SUM_TODAY] \n"
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
					.setDbmsTableName            ("CmPgTables_diff")
					.setDbmsSampleTimeColumnName ("SessionSampleTime")
					.setDbmsDataValueColumnName  ("n_dead_tup")
					.setDbmsWhereKeyColumnName   (whereKeyColumn)
					.setSparklineTooltipPostfix  ("Number of 'n_dead_tup' in below period")
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
					.setDbmsTableName            ("CmPgTables_diff")
					.setDbmsSampleTimeColumnName ("SessionSampleTime")
					.setDbmsDataValueColumnName  ("n_live_tup")
					.setDbmsWhereKeyColumnName   (whereKeyColumn)
					.setSparklineTooltipPostfix  ("Number of 'n_live_tup' in below period")
					.validate()));
		}
		
		String schema = getReportingInstance().getDbmsSchemaName();

		_CmPgTables_DeadRows = createTsLineChart(conn, schema, "CmPgTables", "DeadRows", -1, true, null, "Number of Dead Rows per Database(n_dead_tup) per Database");

	
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
				+ "  and [Category] = 'Autovacuum' \n"
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
				"");
	}
}
