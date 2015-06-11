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

	public static final int      NEED_SRV_VERSION = 0;
	public static final int      NEED_CE_VERSION  = 0;

	public static final String[] MON_TABLES       = new String[] {};
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
		super(CM_NAME, GROUP_NAME, /*sql*/null, /*pkList*/null, 
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
	public String[] getDependsOnConfigForVersion(Connection conn, int srvVersion, boolean isClusterEnabled)
	{
		return NEED_CONFIG;
	}

	@Override
	public List<String> getPkForVersion(Connection conn, int srvVersion, boolean isClusterEnabled)
	{
		List <String> pkCols = new LinkedList<String>();

		pkCols.add("database_id");
		pkCols.add("object_id");

		return pkCols;
	}

	@Override
	public String getSqlForVersion(Connection conn, int aseVersion, boolean isClusterEnabled)
	{
		String sql = 
			"select  \n" +
			"  db_name     = convert(varchar(30), isnull(db_name(database_id), database_id) ), \n" +
			"  pbject_name = convert(varchar(30), isnull(object_name(object_id,database_id), object_id) ),  \n" +
			"  type, \n" +
			"  type_desc, \n" +
			"  cached_time, \n" +
			"  cached_time_ss         = datediff(ss, cached_time, getdate()), \n" +
			"  last_execution_time, \n" +
			"  last_execution_time_ss = datediff(ss, last_execution_time, getdate()), \n" +
			"  execution_count, \n" +
			"  total_worker_time, \n" +
			"  last_worker_time, \n" +
			"  min_worker_time, \n" +
			"  max_worker_time, \n" +
			"  total_physical_reads, \n" +
			"  last_physical_reads, \n" +
			"  min_physical_reads, \n" +
			"  max_physical_reads, \n" +
			"  total_logical_writes, \n" +
			"  last_logical_writes, \n" +
			"  min_logical_writes, \n" +
			"  max_logical_writes, \n" +
			"  total_logical_reads, \n" +
			"  last_logical_reads, \n" +
			"  min_logical_reads, \n" +
			"  max_logical_reads, \n" +
			"  total_elapsed_time, \n" +
			"  last_elapsed_time, \n" +
			"  min_elapsed_time, \n" +
			"  max_elapsed_time, \n" +
			"  sql_handle, \n" +
			"  plan_handle, \n" +
			"  database_id, \n" +
			"  object_id \n" +
			"from sys.dm_exec_procedure_stats \n";

		return sql;
	}
}