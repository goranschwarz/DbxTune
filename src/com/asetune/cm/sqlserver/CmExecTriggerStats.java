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
package com.asetune.cm.sqlserver;

import java.util.LinkedList;
import java.util.List;

import com.asetune.ICounterController;
import com.asetune.IGuiController;
import com.asetune.cm.CounterSample;
import com.asetune.cm.CounterSampleCatalogIteratorSqlServer;
import com.asetune.cm.CounterSetTemplates;
import com.asetune.cm.CounterSetTemplates.Type;
import com.asetune.cm.CountersModel;
import com.asetune.gui.MainFrame;
import com.asetune.sql.conn.DbxConnection;
import com.asetune.sql.conn.info.DbmsVersionInfo;
import com.asetune.sql.conn.info.DbmsVersionInfoSqlServer;

/**
 * @author Goran Schwarz (goran_schwarz@hotmail.com)
 */
public class CmExecTriggerStats
extends CountersModel
{
//	private static Logger        _logger          = Logger.getLogger(CmExecTriggerStats.class);
	private static final long    serialVersionUID = 1L;

	public static final String   CM_NAME          = CmExecTriggerStats.class.getSimpleName();
	public static final String   SHORT_NAME       = "Triggers";
	public static final String   HTML_DESC        = 
		"<html>"
		+ "<p>"
		+ "Shows aggregate performance statistics for cached triggers. <br>"
		+ "The view contains one row per trigger, and the lifetime of the row is as long as the trigger remains cached. <br>"
		+ "When a trigger is removed from the cache, the corresponding row is eliminated from this view. At that time, a Performance Statistics SQL trace event is raised similar to <strong>sys.dm_exec_query_stats</strong>."
		+ "</p>"
		+ "</html>";

	public static final String   GROUP_NAME       = MainFrame.TCP_GROUP_OBJECT_ACCESS;
	public static final String   GUI_ICON_FILE    = "images/"+CM_NAME+".png";

	public static final long     NEED_SRV_VERSION = 0;
	public static final long     NEED_CE_VERSION  = 0;

	public static final String[] MON_TABLES       = new String[] {"dm_exec_trigger_stats"};
	public static final String[] NEED_ROLES       = new String[] {};//{"VIEW SERVER STATE"};
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

//	RS> Col# Label                JDBC Type Name           Guessed DBMS type Source Table
//	RS> ---- -------------------- ------------------------ ----------------- ------------
//	RS> 1    DbName               java.sql.Types.NVARCHAR  nvarchar(128)     -none-      
//	RS> 2    ObjectName           java.sql.Types.NVARCHAR  nvarchar(128)     -none-      
//	RS> 3    database_id          java.sql.Types.INTEGER   int               -none-      
//	RS> 4    object_id            java.sql.Types.INTEGER   int               -none-      
//	RS> 5    type                 java.sql.Types.CHAR      char(2)           -none-      
//	RS> 6    type_desc            java.sql.Types.NVARCHAR  nvarchar(60)      -none-      
//	RS> 7    sql_handle           java.sql.Types.VARBINARY varbinary(128)    -none-      
//	RS> 8    plan_handle          java.sql.Types.VARBINARY varbinary(128)    -none-      
//	RS> 9    cached_time          java.sql.Types.TIMESTAMP datetime          -none-      
//	RS> 10   last_execution_time  java.sql.Types.TIMESTAMP datetime          -none-      
//	RS> 11   execution_count      java.sql.Types.BIGINT    bigint            -none-      
//	RS> 12   total_worker_time    java.sql.Types.BIGINT    bigint            -none-      
//	RS> 13   last_worker_time     java.sql.Types.BIGINT    bigint            -none-      
//	RS> 14   min_worker_time      java.sql.Types.BIGINT    bigint            -none-      
//	RS> 15   max_worker_time      java.sql.Types.BIGINT    bigint            -none-      
//	RS> 16   total_physical_reads java.sql.Types.BIGINT    bigint            -none-      
//	RS> 17   last_physical_reads  java.sql.Types.BIGINT    bigint            -none-      
//	RS> 18   min_physical_reads   java.sql.Types.BIGINT    bigint            -none-      
//	RS> 19   max_physical_reads   java.sql.Types.BIGINT    bigint            -none-      
//	RS> 20   total_logical_writes java.sql.Types.BIGINT    bigint            -none-      
//	RS> 21   last_logical_writes  java.sql.Types.BIGINT    bigint            -none-      
//	RS> 22   min_logical_writes   java.sql.Types.BIGINT    bigint            -none-      
//	RS> 23   max_logical_writes   java.sql.Types.BIGINT    bigint            -none-      
//	RS> 24   total_logical_reads  java.sql.Types.BIGINT    bigint            -none-      
//	RS> 25   last_logical_reads   java.sql.Types.BIGINT    bigint            -none-      
//	RS> 26   min_logical_reads    java.sql.Types.BIGINT    bigint            -none-      
//	RS> 27   max_logical_reads    java.sql.Types.BIGINT    bigint            -none-      
//	RS> 28   total_elapsed_time   java.sql.Types.BIGINT    bigint            -none-      
//	RS> 29   last_elapsed_time    java.sql.Types.BIGINT    bigint            -none-      
//	RS> 30   min_elapsed_time     java.sql.Types.BIGINT    bigint            -none-      
//	RS> 31   max_elapsed_time     java.sql.Types.BIGINT    bigint            -none-      

	public static final boolean  NEGATIVE_DIFF_COUNTERS_TO_ZERO = false;
	public static final boolean  IS_SYSTEM_CM                   = true;
	public static final int      DEFAULT_POSTPONE_TIME          = 0;
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

		return new CmExecTriggerStats(counterController, guiController);
	}

	public CmExecTriggerStats(ICounterController counterController, IGuiController guiController)
	{
		super(counterController,
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
	
	private void addTrendGraphs()
	{
	}

//	@Override
//	protected TabularCntrPanel createGui()
//	{
//		return new CmRaSysmonPanel(this);
//	}

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
		// Using DEFAULT_SKIP_DB_LIST: 'master', 'model', 'tempdb', 'msdb', 'SSISDB', 'ReportServer', 'ReportServerTempDB'
		return new CounterSampleCatalogIteratorSqlServer(name, negativeDiffCountersToZero, diffColumns, prevSample);
	}

	@Override
	public String getSqlForVersion(DbxConnection conn, DbmsVersionInfo versionInfo)
	{
		DbmsVersionInfoSqlServer ssVersionInfo = (DbmsVersionInfoSqlServer) versionInfo;

		String dm_exec_trigger_stats = "dm_exec_trigger_stats";
		
		if (ssVersionInfo.isAzureSynapseAnalytics())
			dm_exec_trigger_stats = "dm_exec_trigger_stats";   // IS THIS THE SAME NAME IN AZURE ?????


//		String sql = 
//			"select \n"
//			+ "    DbName     = db_name(database_id), \n"
//			+ "    ObjectName = object_name(object_id, database_id), \n"
//			+ "    * \n"
//			+ "from sys." + dm_exec_trigger_stats;

		String sql = ""
			    + "-- Note: Below SQL Statement is executed in every database that is 'online', more or less like: sp_msforeachdb \n"
			    + "-- Note: object_schema_name() and object_name() can NOT be used for 'dirty-reads', they may block... hence the 'ugly' fullname sub-selects in the select column list \n"
			    + "select /* ${cmCollectorName} */ \n"
			    + "    DbName     = db_name(database_id), \n"
			    + "    SchemaName = (select sys.schemas.name from sys.objects WITH (READUNCOMMITTED) inner join sys.schemas WITH (READUNCOMMITTED) ON sys.schemas.schema_id = sys.objects.schema_id where sys.objects.object_id = BASE.object_id), \n"
			    + "    TableName  = (select sys.objects.name from sys.objects WITH (READUNCOMMITTED) where sys.objects.object_id = BASE.object_id), \n"
			    + "    * \n"
			    + "from sys." + dm_exec_trigger_stats + " BASE \n"
			    + "where BASE.database_id = db_id() \n"
			    + "";

		return sql;
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
		String[] sa = {"DbName", "ObjectName"};
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
