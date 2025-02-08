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
import java.util.LinkedList;
import java.util.List;

import com.dbxtune.ICounterController;
import com.dbxtune.IGuiController;
import com.dbxtune.cm.CmSettingsHelper;
import com.dbxtune.cm.CounterSample;
import com.dbxtune.cm.CounterSampleCatalogIteratorSqlServer;
import com.dbxtune.cm.CounterSetTemplates;
import com.dbxtune.cm.CounterSetTemplates.Type;
import com.dbxtune.cm.CountersModel;
import com.dbxtune.cm.sqlserver.gui.CmExecFunctionStatsPanel;
import com.dbxtune.gui.MainFrame;
import com.dbxtune.gui.TabularCntrPanel;
import com.dbxtune.sql.conn.DbxConnection;
import com.dbxtune.sql.conn.info.DbmsVersionInfo;
import com.dbxtune.sql.conn.info.DbmsVersionInfoSqlServer;
import com.dbxtune.utils.Configuration;
import com.dbxtune.utils.StringUtil;

/**
 * @author Goran Schwarz (goran_schwarz@hotmail.com)
 */
public class CmExecFunctionStats
extends CountersModel
{
	private static final long    serialVersionUID = 1L;

	public static final String   CM_NAME          = CmExecFunctionStats.class.getSimpleName();
	public static final String   SHORT_NAME       = "Functions";
	public static final String   HTML_DESC        = 
		"<html>"
		+ "<p>"
		+ "Shows aggregate performance statistics for user defined functions. <br>"
		+ "The view returns one row for each cached function plan, and the lifetime of the row is as long as the function remains cached. <br>"
		+ "When a function is removed from the cache, the corresponding row is eliminated from this view. <br>"
		+ "At that time, a Performance Statistics SQL trace event is raised similar to <strong>sys.dm_exec_query_stats</strong>.<br>"
		+ "</p>"
		+ "</html>";

	public static final String   GROUP_NAME       = MainFrame.TCP_GROUP_OBJECT_ACCESS;
	public static final String   GUI_ICON_FILE    = "images/"+CM_NAME+".png";

	public static final long     NEED_SRV_VERSION = 0;
	public static final long     NEED_CE_VERSION  = 0;

	public static final String[] MON_TABLES       = new String[] {"dm_exec_function_stats"};
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
		"total_elapsed_time",
//		"last_elapsed_time",
//		"min_elapsed_time",
//		"max_elapsed_time",
		"total_spills",
		"total_num_physical_reads",
		"total_page_server_reads",
		"total_num_page_server_reads",
		"_last_column_name_only_used_as_a_place_holder_here_"
		};
//		RS> Col# Label                JDBC Type Name           Guessed DBMS type Source Table
//		RS> ---- -------------------- ------------------------ ----------------- ------------
//		RS> 1    database_id          java.sql.Types.INTEGER   int               -none-      
//		RS> 2    object_id            java.sql.Types.INTEGER   int               -none-      
//		RS> 3    type                 java.sql.Types.CHAR      char(2)           -none-      
//		RS> 4    type_desc            java.sql.Types.NVARCHAR  nvarchar(60)      -none-      
//		RS> 5    sql_handle           java.sql.Types.VARBINARY varbinary(128)    -none-      
//		RS> 6    plan_handle          java.sql.Types.VARBINARY varbinary(128)    -none-      
//		RS> 7    cached_time          java.sql.Types.TIMESTAMP datetime          -none-      
//		RS> 8    last_execution_time  java.sql.Types.TIMESTAMP datetime          -none-      
//		RS> 9    execution_count      java.sql.Types.BIGINT    bigint            -none-      
//		RS> 10   total_worker_time    java.sql.Types.BIGINT    bigint            -none-      
//		RS> 11   last_worker_time     java.sql.Types.BIGINT    bigint            -none-      
//		RS> 12   min_worker_time      java.sql.Types.BIGINT    bigint            -none-      
//		RS> 13   max_worker_time      java.sql.Types.BIGINT    bigint            -none-      
//		RS> 14   total_physical_reads java.sql.Types.BIGINT    bigint            -none-      
//		RS> 15   last_physical_reads  java.sql.Types.BIGINT    bigint            -none-      
//		RS> 16   min_physical_reads   java.sql.Types.BIGINT    bigint            -none-      
//		RS> 17   max_physical_reads   java.sql.Types.BIGINT    bigint            -none-      
//		RS> 18   total_logical_writes java.sql.Types.BIGINT    bigint            -none-      
//		RS> 19   last_logical_writes  java.sql.Types.BIGINT    bigint            -none-      
//		RS> 20   min_logical_writes   java.sql.Types.BIGINT    bigint            -none-      
//		RS> 21   max_logical_writes   java.sql.Types.BIGINT    bigint            -none-      
//		RS> 22   total_logical_reads  java.sql.Types.BIGINT    bigint            -none-      
//		RS> 23   last_logical_reads   java.sql.Types.BIGINT    bigint            -none-      
//		RS> 24   min_logical_reads    java.sql.Types.BIGINT    bigint            -none-      
//		RS> 25   max_logical_reads    java.sql.Types.BIGINT    bigint            -none-      
//		RS> 26   total_elapsed_time   java.sql.Types.BIGINT    bigint            -none-      
//		RS> 27   last_elapsed_time    java.sql.Types.BIGINT    bigint            -none-      
//		RS> 28   min_elapsed_time     java.sql.Types.BIGINT    bigint            -none-      
//		RS> 29   max_elapsed_time     java.sql.Types.BIGINT    bigint            -none-     

	public static final boolean  NEGATIVE_DIFF_COUNTERS_TO_ZERO = false;
	public static final boolean  IS_SYSTEM_CM                   = true;
	public static final int      DEFAULT_POSTPONE_TIME          = 300; // every 5 minute
//	public static final int      DEFAULT_POSTPONE_TIME          = 600; // every 10 minute
	public static final int      DEFAULT_QUERY_TIMEOUT          = CountersModel.DEFAULT_sqlQueryTimeout;;

	@Override public int     getDefaultPostponeTime()                 { return DEFAULT_POSTPONE_TIME; }
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

		return new CmExecFunctionStats(counterController, guiController);
	}

	public CmExecFunctionStats(ICounterController counterController, IGuiController guiController)
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
//	public static final String  DEFAULT_sample_extraWhereClause   = "qs.last_logical_reads > 100";
	public static final String  DEFAULT_sample_extraWhereClause   = "";

	public static final String  PROPKEY_sample_afterPrevSample    = PROP_PREFIX + ".sample.afterPrevSample";
	public static final boolean DEFAULT_sample_afterPrevSample    = false;

	public static final String  PROPKEY_sample_lastXminutes       = PROP_PREFIX + ".sample.lastXminutes";
	public static final boolean DEFAULT_sample_lastXminutes       = true;

	public static final String  PROPKEY_sample_lastXminutesTime   = PROP_PREFIX + ".sample.lastXminutes.time";
//	public static final int     DEFAULT_sample_lastXminutesTime   = 30;
	public static final int     DEFAULT_sample_lastXminutesTime   = 25 * 60; // 25 hours
	
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
		
		list.add(new CmSettingsHelper("Extra Where Clause",                           PROPKEY_sample_extraWhereClause , String .class, conf.getProperty       (PROPKEY_sample_extraWhereClause , DEFAULT_sample_extraWhereClause ), DEFAULT_sample_extraWhereClause, CmExecFunctionStatsPanel.TOOLTIP_sample_extraWhereClause ));
		list.add(new CmSettingsHelper("Show only SQL exected since last sample time", PROPKEY_sample_afterPrevSample  , Boolean.class, conf.getBooleanProperty(PROPKEY_sample_afterPrevSample  , DEFAULT_sample_afterPrevSample  ), DEFAULT_sample_afterPrevSample , CmExecFunctionStatsPanel.TOOLTIP_sample_afterPrevSample  ));
		list.add(new CmSettingsHelper("Show only SQL exected last 10 minutes",        PROPKEY_sample_lastXminutes     , Boolean.class, conf.getBooleanProperty(PROPKEY_sample_lastXminutes     , DEFAULT_sample_lastXminutes     ), DEFAULT_sample_lastXminutes    , CmExecFunctionStatsPanel.TOOLTIP_sample_lastXminutes     ));
		list.add(new CmSettingsHelper("Show only SQL exected last ## minutes",        PROPKEY_sample_lastXminutesTime , Integer.class, conf.getIntProperty    (PROPKEY_sample_lastXminutesTime , DEFAULT_sample_lastXminutesTime ), DEFAULT_sample_lastXminutesTime, CmExecFunctionStatsPanel.TOOLTIP_sample_lastXminutesTime ));

		return list;
	}


	private void addTrendGraphs()
	{
	}

	@Override
	protected TabularCntrPanel createGui()
	{
		return new CmExecFunctionStatsPanel(this);
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

		pkCols.add("database_id");
		pkCols.add("object_id");
		pkCols.add("plan_handle");
		
		return pkCols;
	}

	/**
	 * Create a special CounterSample, that will iterate over all databases that we will interrogate
	 */
	@Override
	public CounterSample createCounterSample(String name, boolean negativeDiffCountersToZero, String[] diffColumns, CounterSample prevSample)
	{
		// Using DEFAULT_SKIP_DB_LIST: 'master', 'model', 'msdb', 'SSISDB', 'ReportServer', 'ReportServerTempDB'
		return new CounterSampleCatalogIteratorSqlServer(name, negativeDiffCountersToZero, diffColumns, prevSample);
	}

	@Override
	public String getSqlForVersion(DbxConnection conn, DbmsVersionInfo versionInfo)
	{
		DbmsVersionInfoSqlServer ssVersionInfo = (DbmsVersionInfoSqlServer) versionInfo;

		String dm_exec_function_stats = "dm_exec_function_stats";
		
		if (ssVersionInfo.isAzureSynapseAnalytics())
			dm_exec_function_stats = "dm_exec_function_stats"; // SAME NAME IN AZURE ????

		
		Configuration conf = Configuration.getCombinedConfiguration();
		String  sample_extraWhereClause = conf.getProperty(       PROPKEY_sample_extraWhereClause, DEFAULT_sample_extraWhereClause);
		boolean sample_lastXminutes     = conf.getBooleanProperty(PROPKEY_sample_lastXminutes,     DEFAULT_sample_lastXminutes);
		int     sample_lastXminutesTime = conf.getIntProperty(    PROPKEY_sample_lastXminutesTime, DEFAULT_sample_lastXminutesTime);

		// Do we have extra where clauses
		String sql_sample_extraWhereClause = "  -- Extra where clauses will go here. (it will look like: AND the_extra_where_clause) \n";
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
			sql_sample_lastXminutes = "  AND dateadd(ms, (last_elapsed_time/1000), last_execution_time) > dateadd(mi, -"+sample_lastXminutesTime+", getdate())\n";
		}

//		String sql = 
//			"select \n"
//			+ "    DbName     = db_name(database_id), \n"
//			+ "    ObjectName = object_name(object_id, database_id), \n"
//			+ "    * \n"
//			+ "from sys." + dm_exec_function_stats;

		String sql = ""
			    + "-- Note: Below SQL Statement is executed in every database that is 'online', more or less like: sp_msforeachdb \n"
			    + "-- Note: object_schema_name() and object_name() can NOT be used for 'dirty-reads', they may block... hence the 'ugly' fullname sub-selects in the select column list \n"
			    + "select /* ${cmCollectorName} */ \n"
			    + "    DbName     = db_name(database_id), \n"
			    + "    SchemaName = (select sys.schemas.name from sys.objects WITH (READUNCOMMITTED) inner join sys.schemas WITH (READUNCOMMITTED) ON sys.schemas.schema_id = sys.objects.schema_id where sys.objects.object_id = BASE.object_id), \n"
			    + "    ObjectName = (select sys.objects.name from sys.objects WITH (READUNCOMMITTED) where sys.objects.object_id = BASE.object_id), \n"
			    + "    * \n"
			    + "from sys." + dm_exec_function_stats + " BASE \n"
			    + "where 1 = 1 /* to make extra where clauses easier */ \n"
			    + "  and BASE.database_id = db_id() /* Since we are 'looping' all databases. like sp_msforeachdb */ \n" 
				+ sql_sample_extraWhereClause
				+ sql_sample_lastXminutes
				;

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
		String[] sa = {"DBName", "ObjectName"};
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
		String[] sa = {"execution_count", "total_logical_reads", "total_elapsed_time"};
		return sa;
	}


}
