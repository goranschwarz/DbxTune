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

import com.asetune.gui.ResultSetTableModel;
import com.asetune.pcs.report.DailySummaryReportAbstract;
import com.asetune.pcs.report.content.ase.AseAbstract;
import com.asetune.sql.conn.DbxConnection;
import com.asetune.utils.Configuration;

public class PostgresTopTableAccess extends AseAbstract
{
//	private static Logger _logger = Logger.getLogger(PostgresTopTableAccess.class);

	private ResultSetTableModel _rstm_IoCache;
	private ResultSetTableModel _rstm_access;

	public PostgresTopTableAccess(DailySummaryReportAbstract reportingInstance)
	{
		super(reportingInstance);
	}

	@Override
	public String getMessageText()
	{
		StringBuilder sb = new StringBuilder();

		//------------------------------------------
		// IO and Cache
		if (_rstm_IoCache.getRowCount() == 0)
		{
			sb.append("No rows found for 'Table/Index IO and Cache Information' <br>\n");
		}
		else
		{
			// Get a description of this section, and column names
			sb.append(getSectionDescriptionHtml(_rstm_IoCache, true));

			sb.append("Row Count: ").append(_rstm_IoCache.getRowCount()).append("<br>\n");
			sb.append(_rstm_IoCache.toHtmlTableString("sortable"));
		}

		//------------------------------------------
		// Access
		if (_rstm_access.getRowCount() == 0)
		{
			sb.append("No rows found for 'Table/Index Access Activity' <br>\n");
		}
		else
		{
			// Get a description of this section, and column names
			sb.append(getSectionDescriptionHtml(_rstm_access, true));

			sb.append("Row Count: ").append(_rstm_access.getRowCount()).append("<br>\n");
			sb.append(_rstm_access.toHtmlTableString("sortable"));
		}

		return sb.toString();
	}

	@Override
	public String getSubject()
	{
		return "Top TABLE/INDEX Activity (origin: CmPgTables & CmPgTablesIo / pg_stat_all_tables & pg_statio_all_tables)";
	}

	@Override
	public boolean hasIssueToReport()
	{
		return false; // even if we found entries, do NOT indicate this as a Problem or Issue
	}


	@Override
	public void create(DbxConnection conn, String srvName, Configuration pcsSavedConf, Configuration localConf)
	{
		_rstm_IoCache = create_CmPgTablesIo_diff(conn, srvName, pcsSavedConf, localConf);
		_rstm_access  = create_CmPgTables_diff  (conn, srvName, pcsSavedConf, localConf);
	}

	private ResultSetTableModel create_CmPgTablesIo_diff(DbxConnection conn, String srvName, Configuration pcsSavedConf, Configuration localConf)
	{
		int topRows = localConf.getIntProperty(this.getClass().getSimpleName()+".top", 20);

		 // just to get Column names
//		String dummySql = "select * from [CmPgTablesIo_diff] where 1 = 2";
//		ResultSetTableModel dummyRstm = executeQuery(conn, dummySql, true, "metadata");

		String sql = getCmDiffColumnsAsSqlComment("CmPgTablesIo")
			    + "select top " + topRows + " \n"
			    + "     min([CmSampleTime])           as [CmSampleTime_MIN] \n"
			    + "    ,max([CmSampleTime])           as [CmSampleTime_MAX] \n"
			    + "    ,cast('' as varchar(30))       as [Duration] \n"
			    + " \n"
			    + "    ,[dbname] \n"
			    + "    ,[schemaname] \n"
			    + "    ,[relname] \n"
			    + "    ,max([relid])                  as [relid] \n"
			    + " \n"
			    + "    ,(sum([heap_blks_read]) + sum([idx_blks_read]) + sum([toast_blks_read]) + sum([tidx_blks_read]) ) as [ALL_blks_read_SUM] \n"
			    + "    ,(sum([heap_blks_hit])  + sum([idx_blks_hit])  + sum([toast_blks_hit])  + sum([tidx_blks_hit])  ) as [ALL_blks_hit_SUM] \n"
			    + " \n"
			    + "    ,sum([heap_blks_read])         as [heap_blks_read_SUM] \n"
			    + "    ,sum([heap_blks_hit])          as [heap_blks_hit_SUM] \n"
			    + "    ,sum([idx_blks_read])          as [idx_blks_read_SUM] \n"
			    + "    ,sum([idx_blks_hit])           as [idx_blks_hit_SUM] \n"
			    + "    ,sum([toast_blks_read])        as [toast_blks_read_SUM] \n"
			    + "    ,sum([toast_blks_hit])         as [toast_blks_hit_SUM] \n"
			    + "    ,sum([tidx_blks_read])         as [tidx_blks_read_SUM] \n"
			    + "    ,sum([tidx_blks_hit])          as [tidx_blks_hit_SUM] \n"
			    + "from [CmPgTablesIo_diff] \n"
			    + "group by [dbname], [schemaname], [relname] \n"
			    + "order by sum([heap_blks_hit]) + sum([idx_blks_hit]) + sum([toast_blks_hit]) + sum([tidx_blks_hit]) desc \n"
			    + "";

		ResultSetTableModel rstm = executeQuery(conn, sql, false, "TopTableAccessIO");
		if (rstm == null)
		{
			rstm = ResultSetTableModel.createEmpty("TopTableAccessIO");
		}
		else
		{
			// Describe the table
			rstm.setDescription("<h4>Table/Index IO and Cache Activity (ordered by: heap_blks_hit + idx_blks_hit + toast_blks_hit + tidx_blks_hit)</h4>");

			// Columns description
			rstm.setColumnDescription("CmSampleTime_MIN"          , "First entry was sampled.");
			rstm.setColumnDescription("CmSampleTime_MAX"          , "Last entry was sampled.");
			rstm.setColumnDescription("Duration"                  , "Difference between first/last sample");

			rstm.setColumnDescription("dbname"                    , "Database name");
			rstm.setColumnDescription("schemaname"                , "Name of the schema that this table is in");
			rstm.setColumnDescription("relname"                   , "Name of this table");
			rstm.setColumnDescription("relid"                     , "OID of a table (can be used to reference the table in the filesystem)");

			rstm.setColumnDescription("ALL_blks_read_SUM"         , "Number of disk blocks read from this table (heap_blks_read + idx_blks_read + toast_blks_read + tidx_blks_read");
			rstm.setColumnDescription("ALL_blks_hit_SUM"          , "Number of buffer hits in this table (heap_blks_hit + idx_blks_hit + toast_blks_hit + tidx_blks_hit)");

			rstm.setColumnDescription("heap_blks_read_SUM"        , "Number of disk blocks read from this table");
			rstm.setColumnDescription("heap_blks_hit_SUM"         , "Number of buffer hits in this table");
			rstm.setColumnDescription("idx_blks_read_SUM"         , "Number of disk blocks read from all indexes on this table");
			rstm.setColumnDescription("idx_blks_hit_SUM"          , "Number of buffer hits in all indexes on this table");
			rstm.setColumnDescription("toast_blks_read_SUM"       , "Number of disk blocks read from this table's TOAST table (if any)");
			rstm.setColumnDescription("toast_blks_hit_SUM"        , "Number of buffer hits in this table's TOAST table (if any)");
			rstm.setColumnDescription("tidx_blks_read_SUM"        , "Number of disk blocks read from this table's TOAST table indexes (if any)");
			rstm.setColumnDescription("tidx_blks_hit_SUM"         , "Number of buffer hits in this table's TOAST table indexes (if any)");

			// Calculate Duration
			setDurationColumn(rstm, "CmSampleTime_MIN", "CmSampleTime_MAX", "Duration");
		}
		return rstm;
	}

	private ResultSetTableModel create_CmPgTables_diff(DbxConnection conn, String srvName, Configuration pcsSavedConf, Configuration localConf)
	{
		int topRows = localConf.getIntProperty(this.getClass().getSimpleName()+".top", 20);

		 // just to get Column names
//		String dummySql = "select * from [CmPgTables_diff] where 1 = 2";
//		ResultSetTableModel dummyRstm = executeQuery(conn, dummySql, true, "metadata");

		String sql = getCmDiffColumnsAsSqlComment("CmPgTables")
			    + "select top " + topRows + " \n"
			    + "     min([CmSampleTime])                                        as [CmSampleTime_MIN] \n"
			    + "    ,max([CmSampleTime])                                        as [CmSampleTime_MAX] \n"
			    + "    ,cast('' as varchar(30))                                    as [Duration] \n"
			    + " \n"
			    + "    ,[dbname] \n"
			    + "    ,[schemaname] \n"
			    + "    ,[relname] \n"
			    + "    ,max([relid])                                               as [relid] \n"
			    + " \n"
			    + "    ,max([total_kb])                                            as [total_kb_MAX] \n"
			    + "    ,max([data_kb])                                             as [data_kb_MAX] \n"
			    + "    ,max([index_kb])                                            as [index_kb_MAX] \n"
			    + " \n"
			    + "    ,CAST( avg([table_scan_pct])         as decimal(5,1) )      as [table_scan_pct_AVG] \n"
			    + "    ,CAST( avg([index_usage_pct])        as decimal(5,1) )      as [index_usage_pct_AVG] \n"
			    + "    ,sum([seq_scan])                                            as [seq_scan_SUM] \n"
			    + "    ,sum([seq_tup_read])                                        as [seq_tup_read_SUM] \n"
			    + "    ,CAST( avg([seq_tup_read_per_scan])  as bigint )            as [seq_tup_read_per_scan_AVG] \n"
			    + "    ,sum([idx_scan])                                            as [idx_scan_SUM] \n"
			    + "    ,sum([idx_tup_fetch])                                       as [idx_tup_fetch_SUM] \n"
			    + "    ,CAST( avg([idx_tup_fetch_per_scan]) as bigint )            as [idx_tup_fetch_per_scan_AVG] \n"
			    + "    ,sum([n_tup_ins])                                           as [n_tup_ins_SUM] \n"
			    + "    ,sum([n_tup_upd])                                           as [n_tup_upd_SUM] \n"
			    + "    ,sum([n_tup_del])                                           as [n_tup_del_SUM] \n"
			    + "    ,sum([n_tup_hot_upd])                                       as [n_tup_hot_upd_SUM] \n"
			    + "    ,sum([n_live_tup])                                          as [n_live_tup_SUM] \n"
			    + "    ,sum([n_dead_tup])                                          as [n_dead_tup_SUM] \n"
			    + "    ,sum([n_mod_since_analyze])                                 as [n_mod_since_analyze_SUM] \n"
			    + "    ,max([last_vacuum])                                         as [last_vacuum_MAX] \n"
			    + "    ,max([last_autovacuum])                                     as [last_autovacuum_MAX] \n"
			    + "    ,max([last_analyze])                                        as [last_analyze_MAX] \n"
			    + "    ,max([last_autoanalyze])                                    as [last_autoanalyze_MAX] \n"
			    + "    ,sum([vacuum_count])                                        as [vacuum_count_SUM] \n"
			    + "    ,sum([autovacuum_count])                                    as [autovacuum_count_SUM] \n"
			    + "    ,sum([analyze_count])                                       as [analyze_count_SUM] \n"
			    + "    ,sum([autoanalyze_count])                                   as [autoanalyze_count_SUM] \n"
			    + "from [CmPgTables_diff] \n"
			    + "group by [dbname], [schemaname], [relname] \n"
			    + "order by sum([seq_tup_read]) + sum([idx_tup_fetch]) desc \n"
			    + "";

		ResultSetTableModel rstm = executeQuery(conn, sql, false, "TopTableAccess");
		if (rstm == null)
		{
			rstm = ResultSetTableModel.createEmpty("TopTableAccess");
		}
		else
		{
			// Describe the table
			rstm.setDescription("<h4>Table/Index Access Activity (ordered by: seq_tup_read + idx_tup_fetch)</h4>"
					+ "Autovacuum Tuning Basics (for TOAST cleanup/reduction)<br>"
					+ "https://www.2ndquadrant.com/en/blog/autovacuum-tuning-basics/"
					+ "<br>");

			// Columns description
			rstm.setColumnDescription("CmSampleTime_MIN"          , "First entry was sampled.");
			rstm.setColumnDescription("CmSampleTime_MAX"          , "Last entry was sampled.");
			rstm.setColumnDescription("Duration"                  , "Difference between first/last sample");

			rstm.setColumnDescription("dbname"                    , "Database name");
			rstm.setColumnDescription("schemaname"                , "Name of the schema that this table is in");
			rstm.setColumnDescription("relname"                   , "Name of this table");
			rstm.setColumnDescription("relid"                     , "OID of a table (can be used to reference the table in the filesystem)");

			rstm.setColumnDescription("table_scan_pct_AVG"        , "<b>Calculated:</b> Percent of accesses was TABLE SCANS.         <b>Algorithm:</b> <code>100 * seq_scan / (seq_scan + idx_scan)</code>");
			rstm.setColumnDescription("index_usage_pct_AVG"       , "<b>Calculated:</b> Percent of accesses was INDEX SCANS/Lookups. <b>Algorithm:</b> <code>100 * idx_scan / (seq_scan + idx_scan)</code>");
			rstm.setColumnDescription("total_kb_MAX"              , "Total Table size in KB");
			rstm.setColumnDescription("data_kb_MAX"               , "Total DATA size in KB");
			rstm.setColumnDescription("index_kb_MAX"              , "Total INDEX size in KB");
			rstm.setColumnDescription("seq_tup_read_per_scan_AVG" , "<b>Calculated:</b> How many rows was <i>read</i>    per TABLE SCAN.         <b>Algorithm:</b> <code>seq_tup_read / seq_scan</code>");
			rstm.setColumnDescription("idx_tup_fetch_per_scan_AVG", "<b>Calculated:</b> How many rows was <i>fetched</i> per INDEX SCAN/Lookups. <b>Algorithm:</b> <code>idx_tup_fetch / idx_scan</code>");
			rstm.setColumnDescription("seq_scan_SUM"              , "Number of sequential scans initiated on this table");
			rstm.setColumnDescription("seq_tup_read_SUM"          , "Number of live rows fetched by sequential scans");
			rstm.setColumnDescription("idx_scan_SUM"              , "Number of index scans initiated on this table");
			rstm.setColumnDescription("idx_tup_fetch_SUM"         , "Number of live rows fetched by index scans");
			rstm.setColumnDescription("n_tup_ins_SUM"             , "Number of rows inserted");
			rstm.setColumnDescription("n_tup_upd_SUM"             , "Number of rows updated");
			rstm.setColumnDescription("n_tup_del_SUM"             , "Number of rows deleted");
			rstm.setColumnDescription("n_tup_hot_upd_SUM"         , "Number of rows HOT updated (i.e., with no separate index update required)");
			rstm.setColumnDescription("n_live_tup_SUM"            , "Estimated number of live rows");
			rstm.setColumnDescription("n_dead_tup_SUM"            , "Estimated number of dead rows");
			rstm.setColumnDescription("n_mod_since_analyze_SUM"   , "Estimated number of rows modified since this table was last analyzed");
			rstm.setColumnDescription("last_vacuum_MAX"           , "Last time at which this table was manually vacuumed (not counting VACUUM FULL)");
			rstm.setColumnDescription("last_autovacuum_MAX"       , "Last time at which this table was vacuumed by the autovacuum daemon");
			rstm.setColumnDescription("last_analyze_MAX"          , "Last time at which this table was manually analyzed");
			rstm.setColumnDescription("last_autoanalyze_MAX"      , "Last time at which this table was analyzed by the autovacuum daemon");
			rstm.setColumnDescription("vacuum_count_SUM"          , "Number of times this table has been manually vacuumed (not counting VACUUM FULL)");
			rstm.setColumnDescription("autovacuum_count_SUM"      , "Number of times this table has been vacuumed by the autovacuum daemon");
			rstm.setColumnDescription("analyze_count_SUM"         , "Number of times this table has been manually analyzed");
			rstm.setColumnDescription("autoanalyze_count_SUM"     , "Number of times this table has been analyzed by the autovacuum daemon");

			// Calculate Duration
			setDurationColumn(rstm, "CmSampleTime_MIN", "CmSampleTime_MAX", "Duration");
		}
		return rstm;
	}
}
