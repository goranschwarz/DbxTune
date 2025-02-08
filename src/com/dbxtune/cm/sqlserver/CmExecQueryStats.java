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
 * the Free Software Foundation, version 3 of the License.
 * 
 * DbxTune is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License
 * along with DbxTune.  If not, see <http://www.gnu.org/licenses/>.
 ******************************************************************************/
package com.dbxtune.cm.sqlserver;

import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import com.dbxtune.ICounterController;
import com.dbxtune.IGuiController;
import com.dbxtune.cm.CmSettingsHelper;
import com.dbxtune.cm.CounterSetTemplates;
import com.dbxtune.cm.CounterSetTemplates.Type;
import com.dbxtune.cm.CountersModel;
import com.dbxtune.cm.sqlserver.gui.CmExecQueryStatsPanel;
import com.dbxtune.gui.MainFrame;
import com.dbxtune.gui.TabularCntrPanel;
import com.dbxtune.gui.swing.ColumnHeaderPropsEntry;
import com.dbxtune.pcs.PcsColumnOptions;
import com.dbxtune.pcs.PcsColumnOptions.ColumnType;
import com.dbxtune.sql.conn.DbxConnection;
import com.dbxtune.sql.conn.info.DbmsVersionInfo;
import com.dbxtune.sql.conn.info.DbmsVersionInfoSqlServer;
import com.dbxtune.utils.Configuration;
import com.dbxtune.utils.StringUtil;

/**
 * @author Goran Schwarz (goran_schwarz@hotmail.com)
 */
public class CmExecQueryStats
extends CountersModel
{
	private static final long    serialVersionUID = 1L;

	public static final String   CM_NAME          = CmExecQueryStats.class.getSimpleName();
	public static final String   SHORT_NAME       = "Query Stat";
	public static final String   HTML_DESC        = 
		"<html>" +
		"<p>FIXME</p>" +
		"</html>";

	public static final String   GROUP_NAME       = MainFrame.TCP_GROUP_OBJECT_ACCESS;
	public static final String   GUI_ICON_FILE    = "images/"+CM_NAME+".png";

	public static final long     NEED_SRV_VERSION = 0;
	public static final long     NEED_CE_VERSION  = 0;

	public static final String[] MON_TABLES       = new String[] {"dm_exec_query_stats"};
	public static final String[] NEED_ROLES       = new String[] {"VIEW SERVER STATE"};
	public static final String[] NEED_CONFIG      = new String[] {};

	public static final String[] PCT_COLUMNS      = new String[] {};
	public static final String[] DIFF_COLUMNS     = new String[] {
		"execution_count",
		"total_worker_time",
//		"last_worker_time",
//		"min_worker_time",
//		"max_worker_time",
		"total_physical_reads",
//		"last_physical_reads",
//		"min_physical_reads",
//		"max_physical_reads",
		"total_logical_writes",
//		"last_logical_writes",
//		"min_logical_writes",
//		"max_logical_writes",
		"total_logical_reads",
//		"last_logical_reads",
//		"min_logical_reads",
//		"max_logical_reads",
		"total_clr_time",
//		"last_clr_time",
//		"min_clr_time",
//		"max_clr_time",
		"total_elapsed_time",
//		"last_elapsed_time",
//		"min_elapsed_time",
//		"max_elapsed_time",
//		"query_hash",
//		"query_plan_hash",
		"total_rows",
//		"last_rows",
//		"min_rows",
//		"max_rows",
		"total_dop",
		"total_grant_kb",
		"total_used_grant_kb",
		"total_ideal_grant_kb",
		"total_reserved_threads",
		"total_used_threads",
		"total_columnstore_segment_reads",
		"total_columnstore_segment_skips",
		"total_spills",
		"total_num_physical_reads",
		"total_page_server_reads",
		"total_num_page_server_reads",
		"_last_column_name_only_used_as_a_place_holder_here_"
		};

// Microsoft SQL Server 2008 R2 (SP2) - 10.50.4000.0 (X64)  	Jun 28 2012 08:36:30  	Copyright (c) Microsoft Corporation 	Express Edition with Advanced Services (64-bit) on Windows NT 6.1 <X64> (Build 7601: Service Pack 1)

//	RS> Col# Label                  JDBC Type Name           Guessed DBMS type
//	RS> ---- ---------------------- ------------------------ -----------------
//	RS> 1    sql_handle             java.sql.Types.VARBINARY varbinary(128)   
//	RS> 2    statement_start_offset java.sql.Types.INTEGER   int              
//	RS> 3    statement_end_offset   java.sql.Types.INTEGER   int              
//	RS> 4    plan_generation_num    java.sql.Types.BIGINT    bigint           
//	RS> 5    plan_handle            java.sql.Types.VARBINARY varbinary(128)   
//	RS> 6    creation_time          java.sql.Types.TIMESTAMP datetime         
//	RS> 7    last_execution_time    java.sql.Types.TIMESTAMP datetime         
//	RS> 8    execution_count        java.sql.Types.BIGINT    bigint           
//	RS> 9    total_worker_time      java.sql.Types.BIGINT    bigint           
//	RS> 10   last_worker_time       java.sql.Types.BIGINT    bigint           
//	RS> 11   min_worker_time        java.sql.Types.BIGINT    bigint           
//	RS> 12   max_worker_time        java.sql.Types.BIGINT    bigint           
//	RS> 13   total_physical_reads   java.sql.Types.BIGINT    bigint           
//	RS> 14   last_physical_reads    java.sql.Types.BIGINT    bigint           
//	RS> 15   min_physical_reads     java.sql.Types.BIGINT    bigint           
//	RS> 16   max_physical_reads     java.sql.Types.BIGINT    bigint           
//	RS> 17   total_logical_writes   java.sql.Types.BIGINT    bigint           
//	RS> 18   last_logical_writes    java.sql.Types.BIGINT    bigint           
//	RS> 19   min_logical_writes     java.sql.Types.BIGINT    bigint           
//	RS> 20   max_logical_writes     java.sql.Types.BIGINT    bigint           
//	RS> 21   total_logical_reads    java.sql.Types.BIGINT    bigint           
//	RS> 22   last_logical_reads     java.sql.Types.BIGINT    bigint           
//	RS> 23   min_logical_reads      java.sql.Types.BIGINT    bigint           
//	RS> 24   max_logical_reads      java.sql.Types.BIGINT    bigint           
//	RS> 25   total_clr_time         java.sql.Types.BIGINT    bigint           
//	RS> 26   last_clr_time          java.sql.Types.BIGINT    bigint           
//	RS> 27   min_clr_time           java.sql.Types.BIGINT    bigint           
//	RS> 28   max_clr_time           java.sql.Types.BIGINT    bigint           
//	RS> 29   total_elapsed_time     java.sql.Types.BIGINT    bigint           
//	RS> 30   last_elapsed_time      java.sql.Types.BIGINT    bigint           
//	RS> 31   min_elapsed_time       java.sql.Types.BIGINT    bigint           
//	RS> 32   max_elapsed_time       java.sql.Types.BIGINT    bigint           
//	RS> 33   query_hash             java.sql.Types.BINARY    binary(16)       
//	RS> 34   query_plan_hash        java.sql.Types.BINARY    binary(16)       
//	RS> 35   total_rows             java.sql.Types.BIGINT    bigint           
//	RS> 36   last_rows              java.sql.Types.BIGINT    bigint           
//	RS> 37   min_rows               java.sql.Types.BIGINT    bigint           
//	RS> 38   max_rows               java.sql.Types.BIGINT    bigint           	

	public static final boolean  NEGATIVE_DIFF_COUNTERS_TO_ZERO = false;
	public static final boolean  IS_SYSTEM_CM                   = true;
	public static final int      DEFAULT_POSTPONE_TIME          = 600;
	public static final int      DEFAULT_QUERY_TIMEOUT          = CountersModel.DEFAULT_sqlQueryTimeout;;

//	@Override public int     getDefaultPostponeTime()                 { return DEFAULT_POSTPONE_TIME; }
	@Override public int     getDefaultPostponeTime()                 { return DEFAULT_POSTPONE_TIME; } // every 10 minute
	@Override public int     getDefaultQueryTimeout()                 { return DEFAULT_QUERY_TIMEOUT; }
	@Override public boolean getDefaultIsNegativeDiffCountersToZero() { return NEGATIVE_DIFF_COUNTERS_TO_ZERO; }
	@Override public Type    getTemplateLevel()                       { return Type.ALL; }

	/**
	 * FACTORY  method to create the object
	 */
	public static CountersModel create(ICounterController counterController, IGuiController guiController)
	{
		if (guiController != null && guiController.hasGUI())
			guiController.splashWindowProgress("Loading: Counter Model '"+CM_NAME+"'");

		return new CmExecQueryStats(counterController, guiController);
	}

	public CmExecQueryStats(ICounterController counterController, IGuiController guiController)
	{
		super(counterController, guiController,
				CM_NAME, GROUP_NAME, /*sql*/null, /*pkList*/null, 
				DIFF_COLUMNS, PCT_COLUMNS, MON_TABLES, 
				NEED_ROLES, NEED_CONFIG, NEED_SRV_VERSION, NEED_CE_VERSION, 
				NEGATIVE_DIFF_COUNTERS_TO_ZERO, IS_SYSTEM_CM, DEFAULT_POSTPONE_TIME);

		setDisplayName(SHORT_NAME);
		setDescription(HTML_DESC);

		setIconFile(GUI_ICON_FILE);

		setShowClearTime(false);

		setCounterController(counterController);
		setGuiController(guiController);
		
		addTrendGraphs();
		
		CounterSetTemplates.register(this);
	}


	//------------------------------------------------------------
	// Implementation
	//------------------------------------------------------------
	private static final String  PROP_PREFIX                      = CM_NAME;

	public static final String  PROPKEY_sample_extraWhereClause   = PROP_PREFIX + ".sample.extraWhereClause";
	public static final String  DEFAULT_sample_extraWhereClause   = "qs.last_logical_reads > 100";

	public static final String  PROPKEY_sample_afterPrevSample    = PROP_PREFIX + ".sample.afterPrevSample";
	public static final boolean DEFAULT_sample_afterPrevSample    = false;

	public static final String  PROPKEY_sample_lastXminutes       = PROP_PREFIX + ".sample.lastXminutes";
	public static final boolean DEFAULT_sample_lastXminutes       = true;

	public static final String  PROPKEY_sample_lastXminutesTime   = PROP_PREFIX + ".sample.lastXminutes.time";
//	public static final int     DEFAULT_sample_lastXminutesTime   = 30;
	public static final int     DEFAULT_sample_lastXminutesTime   = 6 * 60; // 6 hours


	@Override
	protected void registerDefaultValues()
	{
		super.registerDefaultValues();

		Configuration.registerDefaultValue(PROPKEY_sample_extraWhereClause, DEFAULT_sample_extraWhereClause);
		Configuration.registerDefaultValue(PROPKEY_sample_afterPrevSample,  DEFAULT_sample_afterPrevSample);
		Configuration.registerDefaultValue(PROPKEY_sample_lastXminutes,     DEFAULT_sample_lastXminutes);
		Configuration.registerDefaultValue(PROPKEY_sample_lastXminutesTime, DEFAULT_sample_lastXminutesTime);
	}


	/** Used by the: Create 'Offline Session' Wizard */
	@Override
	public List<CmSettingsHelper> getLocalSettings()
	{
		Configuration conf = Configuration.getCombinedConfiguration();
		List<CmSettingsHelper> list = new ArrayList<>();
		
		list.add(new CmSettingsHelper("Extra Where Clause",                           PROPKEY_sample_extraWhereClause , String .class, conf.getProperty       (PROPKEY_sample_extraWhereClause , DEFAULT_sample_extraWhereClause ), DEFAULT_sample_extraWhereClause, CmExecQueryStatsPanel.TOOLTIP_sample_extraWhereClause ));
		list.add(new CmSettingsHelper("Show only SQL exected since last sample time", PROPKEY_sample_afterPrevSample  , Boolean.class, conf.getBooleanProperty(PROPKEY_sample_afterPrevSample  , DEFAULT_sample_afterPrevSample  ), DEFAULT_sample_afterPrevSample , CmExecQueryStatsPanel.TOOLTIP_sample_afterPrevSample  ));
		list.add(new CmSettingsHelper("Show only SQL exected last 10 minutes",        PROPKEY_sample_lastXminutes     , Boolean.class, conf.getBooleanProperty(PROPKEY_sample_lastXminutes     , DEFAULT_sample_lastXminutes     ), DEFAULT_sample_lastXminutes    , CmExecQueryStatsPanel.TOOLTIP_sample_lastXminutes     ));
		list.add(new CmSettingsHelper("Show only SQL exected last ## minutes",        PROPKEY_sample_lastXminutesTime , Integer.class, conf.getIntProperty    (PROPKEY_sample_lastXminutesTime , DEFAULT_sample_lastXminutesTime ), DEFAULT_sample_lastXminutesTime, CmExecQueryStatsPanel.TOOLTIP_sample_lastXminutesTime ));

		return list;
	}


	private void addTrendGraphs()
	{
	}

	@Override
	protected TabularCntrPanel createGui()
	{
		return new CmExecQueryStatsPanel(this);
	}


	@Override
	public String[] getDependsOnConfigForVersion(DbxConnection conn, DbmsVersionInfo versionInfo)
	{
		return NEED_CONFIG;
	}

	@Override
	public List<String> getPkForVersion(DbxConnection conn, DbmsVersionInfo versionInfo)
	{
		List <String> pkCols = new LinkedList<String>();

		pkCols.add("sql_handle");
		pkCols.add("statement_start_offset");
		pkCols.add("statement_end_offset");
		pkCols.add("plan_generation_num");
		pkCols.add("plan_handle");

		return pkCols;
	}

	@Override
	public Map<String, PcsColumnOptions> getPcsColumnOptions()
	{
		Map<String, PcsColumnOptions> map = super.getPcsColumnOptions();

		// No settings in the super, create one, and set it at super
		if (map == null)
		{
			map = new HashMap<>();
			map.put("SqlText", new PcsColumnOptions(ColumnType.DICTIONARY_COMPRESSION));

			// Set the map in the super
			setPcsColumnOptions(map);
		}

		return map;
	}

//	@Override
//	public Map<String, AggregationType> createAggregateColumns()
//	{
//		HashMap<String, AggregationType> aggColumns = new HashMap<>(getColumnCount());
//
//		AggregationType tmp;
//
//		//Should we do this;
//		//I think there will be a POTENTIAL data overflow... 
//		//This has NOT YET been tested;
//		
//		// Create the columns :::::::::::::::::::::::::::::::::::::::::::::::::::::: And ADD it to the return Map 
//		tmp = new AggregationType("execution_count"                , AggregationType.Agg.SUM);   aggColumns.put(tmp.getColumnName(), tmp);
//		tmp = new AggregationType("total_worker_time"              , AggregationType.Agg.SUM);   aggColumns.put(tmp.getColumnName(), tmp);
//		tmp = new AggregationType("total_physical_reads"           , AggregationType.Agg.SUM);   aggColumns.put(tmp.getColumnName(), tmp);
//		tmp = new AggregationType("total_logical_writes"           , AggregationType.Agg.SUM);   aggColumns.put(tmp.getColumnName(), tmp);
//		tmp = new AggregationType("total_logical_reads"            , AggregationType.Agg.SUM);   aggColumns.put(tmp.getColumnName(), tmp);
//		tmp = new AggregationType("total_clr_time"                 , AggregationType.Agg.SUM);   aggColumns.put(tmp.getColumnName(), tmp);
//		tmp = new AggregationType("total_elapsed_time"             , AggregationType.Agg.SUM);   aggColumns.put(tmp.getColumnName(), tmp);
//		tmp = new AggregationType("total_rows"                     , AggregationType.Agg.SUM);   aggColumns.put(tmp.getColumnName(), tmp);
//		tmp = new AggregationType("total_dop"                      , AggregationType.Agg.SUM);   aggColumns.put(tmp.getColumnName(), tmp);
//		tmp = new AggregationType("total_grant_kb"                 , AggregationType.Agg.SUM);   aggColumns.put(tmp.getColumnName(), tmp);
//		tmp = new AggregationType("total_used_grant_kb"            , AggregationType.Agg.SUM);   aggColumns.put(tmp.getColumnName(), tmp);
//		tmp = new AggregationType("total_ideal_grant_kb"           , AggregationType.Agg.SUM);   aggColumns.put(tmp.getColumnName(), tmp);
//		tmp = new AggregationType("total_reserved_threads"         , AggregationType.Agg.SUM);   aggColumns.put(tmp.getColumnName(), tmp);
//		tmp = new AggregationType("total_used_threads"             , AggregationType.Agg.SUM);   aggColumns.put(tmp.getColumnName(), tmp);
//		tmp = new AggregationType("total_columnstore_segment_reads", AggregationType.Agg.SUM);   aggColumns.put(tmp.getColumnName(), tmp);
//		tmp = new AggregationType("total_columnstore_segment_skips", AggregationType.Agg.SUM);   aggColumns.put(tmp.getColumnName(), tmp);
//		tmp = new AggregationType("total_spills"                   , AggregationType.Agg.SUM);   aggColumns.put(tmp.getColumnName(), tmp);
//		tmp = new AggregationType("total_page_server_reads"        , AggregationType.Agg.SUM);   aggColumns.put(tmp.getColumnName(), tmp);
//
//		return aggColumns;
//	}

	@Override
	public String getSqlForVersion(DbxConnection conn, DbmsVersionInfo versionInfo)
	{
		DbmsVersionInfoSqlServer ssVersionInfo = (DbmsVersionInfoSqlServer) versionInfo;

		int c = 0;
		addPreferredColumnOrder(new ColumnHeaderPropsEntry("plan_handle",            c++));
		addPreferredColumnOrder(new ColumnHeaderPropsEntry("SqlText",                c++));
		addPreferredColumnOrder(new ColumnHeaderPropsEntry("sql_handle",             ColumnHeaderPropsEntry.AS_LAST_VIEW_COLUMN));
		addPreferredColumnOrder(new ColumnHeaderPropsEntry("statement_start_offset", ColumnHeaderPropsEntry.AS_LAST_VIEW_COLUMN));
		addPreferredColumnOrder(new ColumnHeaderPropsEntry("statement_end_offset",   ColumnHeaderPropsEntry.AS_LAST_VIEW_COLUMN));
		addPreferredColumnOrder(new ColumnHeaderPropsEntry("plan_generation_num",    ColumnHeaderPropsEntry.AS_LAST_VIEW_COLUMN));

		String dm_exec_query_stats = "dm_exec_query_stats";
		
		if (ssVersionInfo.isAzureSynapseAnalytics())
			dm_exec_query_stats = "dm_pdw_nodes_exec_query_stats";


		Configuration conf = Configuration.getCombinedConfiguration();
		String  sample_extraWhereClause = conf.getProperty(       PROPKEY_sample_extraWhereClause, DEFAULT_sample_extraWhereClause);
		boolean sample_lastXminutes     = conf.getBooleanProperty(PROPKEY_sample_lastXminutes,     DEFAULT_sample_lastXminutes);
		int     sample_lastXminutesTime = conf.getIntProperty(    PROPKEY_sample_lastXminutesTime, DEFAULT_sample_lastXminutesTime);

		// Do we have extra where clauses
		String sql_sample_extraWhereClause = "  /* Extra where clauses will go here. (it will look like: AND the_extra_where_clause) */ \n";
		if ( ! StringUtil.isNullOrBlank(sample_extraWhereClause) )
			sql_sample_extraWhereClause = "  AND " + sample_extraWhereClause + "\n";

		String sql_sample_lastXminutes = "";
		if (sample_lastXminutes)
		{
			// The below Wont work correctly (Check the below Note)
			//sql_sample_lastXminutes = "  AND last_execution_time > dateadd(mi, -"+sample_lastXminutesTime+", getdate())\n";
			//
			// Note:
			//  - statements are added to 'dm_exec_query_stats' when they are FINNISHED (at end of execution)
			//  - The 'last_execution_time' is when the statement was last STARTED
			// Which means that if we have 'lastXMinutes' to 10 minutes (as an example) and the execution takes 11 minutes it wont be captured :(
			// So we need to **add** 'last_elapsed_time' to the 'last_execution_time' to get a Statement END/COMPLETION time!
			// hence: dateadd(ms, (last_elapsed_time/1000), last_execution_time) > #numberOfMinutesToSave#
			//                     ^^^^^^^^^^^^^^^^^ ^^^^
			//                     in-microseconds   to-milliseconds
			sql_sample_lastXminutes = "  AND dateadd(ms, (last_elapsed_time/1000), last_execution_time) > dateadd(mi, -" + sample_lastXminutesTime + ", getdate())\n";
		}

		String sql = 
				"SELECT /* ${cmCollectorName} */ \n" +
				"     SUBSTRING(txt.text, (qs.statement_start_offset/2)+1, ((CASE WHEN qs.statement_end_offset = -1 THEN DATALENGTH(txt.text) ELSE qs.statement_end_offset END - qs.statement_start_offset)/2) + 1) AS [SqlText] \n" +
//				"    ,db_name(txt.dbid) AS dbname \n" + // NOTE: dm_exec_sql_text.dbid is NULL in many cases, so using dm_exec_plan_attributes.dbid seems a lot better
				"    ,(select isnull(db_name(CONVERT(int, value)),CONVERT(nvarchar(10), value)) from sys.dm_exec_plan_attributes(qs.plan_handle) where attribute = N'dbid') AS dbname \n" +
				"    ,txt.objectid \n" +
				"    ,CASE WHEN txt.objectid IS NOT NULL and txt.dbid IS NOT NULL THEN object_name(txt.objectid, txt.dbid) ELSE NULL END AS object_name \n" +
				"    ,CAST(ecp.size_in_bytes / 1024.0 AS numeric(12,1)) AS [PlanSizeKB] \n" +
				"    ,ecp.cacheobjtype \n" +
				"    ,ecp.objtype \n" +
				"    ,qs.* \n" +
				"FROM sys." + dm_exec_query_stats + " qs \n" +
				"LEFT OUTER JOIN sys.dm_exec_cached_plans ecp ON qs.plan_handle = ecp.plan_handle \n" +
				"OUTER APPLY sys.dm_exec_sql_text(qs.sql_handle) txt \n" +
				"WHERE 1 = 1 /* to make extra where clauses easier */ \n" +
				sql_sample_extraWhereClause +
				sql_sample_lastXminutes;

//		String sql = 
//				"SELECT /* ${cmCollectorName} */ \n" +
//				"     SUBSTRING(txt.text, (qs.statement_start_offset/2)+1, ((CASE WHEN qs.statement_end_offset = -1 THEN DATALENGTH(txt.text) ELSE qs.statement_end_offset END - qs.statement_start_offset)/2) + 1) AS [SqlText] \n" +
////				"    ,db_name(txt.dbid) AS dbname \n" + // NOTE: dm_exec_sql_text.dbid is NULL in many cases, so using dm_exec_plan_attributes.dbid seems a lot better
//				"    ,(select isnull(db_name(CONVERT(int, value)),CONVERT(nvarchar(10), value)) from sys.dm_exec_plan_attributes(qs.plan_handle) where attribute = N'dbid') AS dbname \n" +
//				"    ,txt.objectid \n" +
//				"    ,CASE WHEN txt.objectid IS NOT NULL and txt.dbid IS NOT NULL THEN object_name(txt.objectid, txt.dbid) ELSE NULL END AS object_name \n" +
//				"    ,qs.* \n" +
//				"FROM sys." + dm_exec_query_stats + " qs \n" +
//				"OUTER APPLY sys.dm_exec_sql_text(qs.sql_handle) txt \n" +
//				"WHERE 1 = 1 -- to make extra where clauses easier \n" +
//				sql_sample_extraWhereClause +
//				sql_sample_lastXminutes;
		
		// Possible SQL if we want to use CROSS APPLY to get the DBID in dm_exec_plan_attributes
        //    SELECT
        //         SUBSTRING(txt.text, (qs.statement_start_offset/2)+1, ((CASE WHEN qs.statement_end_offset = -1 THEN DATALENGTH(txt.text) ELSE qs.statement_end_offset END - qs.statement_start_offset)/2) + 1) AS [SqlText]
        //        ,db_name(pa.dbid) AS dbname
        //        ,txt.objectid
        //        ,CASE WHEN txt.objectid IS NOT NULL and pa.dbid IS NOT NULL THEN object_name(txt.objectid, pa.dbid) ELSE NULL END AS object_name
        //        ,qs.*
        //    FROM sys.dm_exec_query_stats qs
        //    CROSS APPLY sys.dm_exec_sql_text(qs.sql_handle) txt
        //    CROSS APPLY (select CONVERT(int, value) as dbid from sys.dm_exec_plan_attributes(qs.plan_handle) where attribute = N'dbid') pa
        //    WHERE 1 = 1 -- to make extra where clauses easier
        //      AND qs.last_logical_reads > 100


		return sql;
	}

	@Override
	public String getSql()
	{
		Configuration conf = Configuration.getCombinedConfiguration();
		boolean sample_afterPrevSample = conf.getBooleanProperty(PROPKEY_sample_afterPrevSample, DEFAULT_sample_afterPrevSample);

		if (sample_afterPrevSample)
		{
    		Timestamp prevSample = getPreviousSampleTime();
    		if (prevSample == null)
    		{
    			setSqlWhere("AND 1=0"); // do not get any rows for the first sample...
    		}
    		else
    		{
//    			setSqlWhere("AND last_execution_time > '"+prevSample+"' "); 
    			setSqlWhere("AND dateadd(ms, (last_elapsed_time/1000), last_execution_time) > '"+prevSample+"' "); 
    		}
		}
		else
			setSqlWhere("");

		// Now get the SQL from super method...
		return super.getSql();
	}

	
	/** 
	 * Get number of rows to save/request ddl information for 
	 * if 0 is return no lookup will be done.
	 */
	@Override
	public int getMaxNumOfDdlsToPersist()
	{
		return 10;
	}

	/** 
	 * Get Column names to where DBName and ObjectName is called, this must always return at least a array with 2 strings. 
	 */
	@Override
	public String[] getDdlDetailsColNames()
	{
		String[] sa = {"dbname", "plan_handle"};
		return sa;
	}
	/**
	 * Sort descending on this column(s) to get values from the diff/rate structure<br>
	 * One sort for every value will be done, meaning we can do "top" for more than 1 column<br>
	 * So if we want to do top 10 LogicalReads AND top 10 LockContention
	 * If this one returns null, this will not be done
	 * @return
	 */
	@Override
	public String[] getDdlDetailsSortOnColName()
	{
//fixme: maybe add an object for every column here, like new ObjectLookupSortPredicate("execution_count", GT, ABS|DIFF|RATE, 0);
//This so we dont extract so many "extra objects"...

		String[] sa = {"execution_count", "total_worker_time", "total_logical_reads", "total_elapsed_time"};
		return sa;
	}
}
