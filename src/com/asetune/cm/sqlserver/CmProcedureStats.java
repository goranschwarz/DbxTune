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

import java.sql.Connection;
import java.util.LinkedList;
import java.util.List;

import com.asetune.ICounterController;
import com.asetune.IGuiController;
import com.asetune.cm.CounterSetTemplates;
import com.asetune.cm.CounterSetTemplates.Type;
import com.asetune.cm.CountersModel;
import com.asetune.gui.MainFrame;

/**
 * @author Goran Schwarz (goran_schwarz@hotmail.com)
 */
public class CmProcedureStats
extends CountersModel
{
//	private static Logger        _logger          = Logger.getLogger(CmServiceMemory.class);
	private static final long    serialVersionUID = 1L;

	public static final String   CM_NAME          = CmProcedureStats.class.getSimpleName();
	public static final String   SHORT_NAME       = "Procedure Stats";
	public static final String   HTML_DESC        = 
		"<html>" +
		"<p>FIXME</p>" +
		"</html>";

	public static final String   GROUP_NAME       = MainFrame.TCP_GROUP_CACHE;
	public static final String   GUI_ICON_FILE    = "images/"+CM_NAME+".png";

	public static final long     NEED_SRV_VERSION = 0;
	public static final long     NEED_CE_VERSION  = 0;

	public static final String[] MON_TABLES       = new String[] {"dm_exec_procedure_stats"};
	public static final String[] NEED_ROLES       = new String[] {};
	public static final String[] NEED_CONFIG      = new String[] {};

	public static final String[] PCT_COLUMNS      = new String[] {};
	public static final String[] DIFF_COLUMNS     = new String[] {
		// Label                  JDBC Type Name           Guessed DBMS type
		// --------------------   ------------------------ -----------------
//		"database_id",          //java.sql.Types.INTEGER   int              
//		"object_id",            //java.sql.Types.INTEGER   int              
//		"type",                 //java.sql.Types.CHAR      char(2)          
//		"type_desc",            //java.sql.Types.NVARCHAR  nvarchar(60)     
//		"sql_handle",           //java.sql.Types.VARBINARY varbinary(128)   
//		"plan_handle",          //java.sql.Types.VARBINARY varbinary(128)   
//		"cached_time",          //java.sql.Types.TIMESTAMP datetime         
//		"last_execution_time",  //java.sql.Types.TIMESTAMP datetime         
		"execution_count",      //java.sql.Types.BIGINT    bigint           
		"total_worker_time",    //java.sql.Types.BIGINT    bigint           
//		"last_worker_time",     //java.sql.Types.BIGINT    bigint           
//		"min_worker_time",      //java.sql.Types.BIGINT    bigint           
//		"max_worker_time",      //java.sql.Types.BIGINT    bigint           
		"total_physical_reads", //java.sql.Types.BIGINT    bigint           
//		"last_physical_reads",  //java.sql.Types.BIGINT    bigint           
//		"min_physical_reads",   //java.sql.Types.BIGINT    bigint           
//		"max_physical_reads",   //java.sql.Types.BIGINT    bigint           
		"total_logical_writes", //java.sql.Types.BIGINT    bigint           
//		"last_logical_writes",  //java.sql.Types.BIGINT    bigint           
//		"min_logical_writes",   //java.sql.Types.BIGINT    bigint           
//		"max_logical_writes",   //java.sql.Types.BIGINT    bigint           
		"total_logical_reads",  //java.sql.Types.BIGINT    bigint           
//		"last_logical_reads",   //java.sql.Types.BIGINT    bigint           
//		"min_logical_reads",    //java.sql.Types.BIGINT    bigint           
//		"max_logical_reads",    //java.sql.Types.BIGINT    bigint           
		"total_elapsed_time",   //java.sql.Types.BIGINT    bigint           
//		"last_elapsed_time",    //java.sql.Types.BIGINT    bigint           
//		"min_elapsed_time",     //java.sql.Types.BIGINT    bigint           
//		"max_elapsed_time",     //java.sql.Types.BIGINT    bigint           
		"-dummy-last-col-"
		};

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

		return new CmProcedureStats(counterController, guiController);
	}

	public CmProcedureStats(ICounterController counterController, IGuiController guiController)
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
	public String[] getDependsOnConfigForVersion(Connection conn, long srvVersion, boolean isAzure)
	{
		return NEED_CONFIG;
	}

	@Override
	public List<String> getPkForVersion(Connection conn, long srvVersion, boolean isAzure)
	{
		List <String> pkCols = new LinkedList<String>();

		pkCols.add("database_id");
		pkCols.add("object_id");
		pkCols.add("plan_handle");

		return pkCols;
	}

	@Override
	public String getSqlForVersion(Connection conn, long srvVersion, boolean isAzure)
	{
		String dm_exec_procedure_stats = "dm_exec_procedure_stats";
		
		if (isAzure)
			dm_exec_procedure_stats = "dm_pdw_nodes_exec_procedure_stats";

		String sql = ""
		    + "-- Note: Below SQL Statement is executed in every database that is 'online', more or less like: sp_msforeachdb \n"
		    + "-- Note: object_schema_name() and object_name() can NOT be used for 'dirty-reads', they may block... hence the 'ugly' fullname sub-selects in the select column list \n"
		    + "select \n"
		    + "    DbName     = db_name(database_id), \n"
		    + "    SchemaName = (select sys.schemas.name from sys.objects inner join sys.schemas ON sys.schemas.schema_id = sys.objects.schema_id where sys.objects.object_id = BASE.object_id), \n"
		    + "    ObjectName = (select sys.objects.name from sys.objects where sys.objects.object_id = BASE.object_id), \n"
			+ "    type, \n"
			+ "    type_desc, \n"
			+ "    cached_time, \n"
//			+ "    cached_time_ss         = datediff(ss, cached_time, getdate()), \n"
			+ "    cached_time_ss         = CASE WHEN datediff(day, cached_time, getdate()) >= 24 THEN -1 ELSE  datediff(ss, cached_time, getdate()) END, \n"
			+ "    last_execution_time, \n"
//			+ "    last_execution_time_ss = datediff(ss, last_execution_time, getdate()), \n"
			+ "    last_execution_time_ss = CASE WHEN datediff(day, last_execution_time, getdate()) >= 24 THEN -1 ELSE  datediff(ss, last_execution_time, getdate()) END, \n"
			+ "    execution_count, \n"
			+ "    total_worker_time, \n"
			+ "    last_worker_time, \n"
			+ "    min_worker_time, \n"
			+ "    max_worker_time, \n"
			+ "    total_physical_reads, \n"
			+ "    last_physical_reads, \n"
			+ "    min_physical_reads, \n"
			+ "    max_physical_reads, \n"
			+ "    total_logical_writes, \n"
			+ "    last_logical_writes, \n"
			+ "    min_logical_writes, \n"
			+ "    max_logical_writes, \n"
			+ "    total_logical_reads, \n"
			+ "    last_logical_reads, \n"
			+ "    min_logical_reads, \n"
			+ "    max_logical_reads, \n"
			+ "    total_elapsed_time, \n"
			+ "    last_elapsed_time, \n"
			+ "    min_elapsed_time, \n"
			+ "    max_elapsed_time, \n"
			+ "    sql_handle, \n"
			+ "    plan_handle, \n"
			+ "    database_id, \n"
			+ "    object_id \n"
			+ "from sys." + dm_exec_procedure_stats + " BASE \n"
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
