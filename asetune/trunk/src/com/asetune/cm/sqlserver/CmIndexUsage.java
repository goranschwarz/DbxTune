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
public class CmIndexUsage
extends CountersModel
{
//	private static Logger        _logger          = Logger.getLogger(CmServiceMemory.class);
	private static final long    serialVersionUID = 1L;

	public static final String   CM_NAME          = CmIndexUsage.class.getSimpleName();
	public static final String   SHORT_NAME       = "Index Usage";
	public static final String   HTML_DESC        = 
		"<html>" +
		"<p>FIXME</p>" +
		"</html>";

	public static final String   GROUP_NAME       = MainFrame.TCP_GROUP_OBJECT_ACCESS;
	public static final String   GUI_ICON_FILE    = "images/"+CM_NAME+".png";

	public static final int      NEED_SRV_VERSION = 0;
	public static final int      NEED_CE_VERSION  = 0;

	public static final String[] MON_TABLES       = new String[] {"dm_db_index_usage_stats"};
	public static final String[] NEED_ROLES       = new String[] {};//{"VIEW SERVER STATE"};
	public static final String[] NEED_CONFIG      = new String[] {};

	public static final String[] PCT_COLUMNS      = new String[] {};
	public static final String[] DIFF_COLUMNS     = new String[] {
//		"database_id",
//		"object_id",
//		"index_id",
		"user_seeks",
		"user_scans",
		"user_lookups",
		"user_updates",
//		"last_user_seek",
//		"last_user_scan",
//		"last_user_lookup",
//		"last_user_update",
		"system_seeks",
		"system_scans",
		"system_lookups",
		"system_updates",
//		"last_system_seek",
//		"last_system_scan",
//		"last_system_lookup",
//		"last_system_update",
		"_last_column_name_only_used_as_a_place_holder_here"
		};

// Microsoft SQL Server 2008 R2 (SP2) - 10.50.4000.0 (X64)  	Jun 28 2012 08:36:30  	Copyright (c) Microsoft Corporation 	Express Edition with Advanced Services (64-bit) on Windows NT 6.1 <X64> (Build 7601: Service Pack 1)

//	RS> Col# Label              JDBC Type Name           Guessed DBMS type
//	RS> ---- ------------------ ------------------------ -----------------
//	RS> 1    dbname             java.sql.Types.NVARCHAR  nvarchar(128)    
//	RS> 2    objectName         java.sql.Types.NVARCHAR  nvarchar(128)    
//	RS> 3    database_id        java.sql.Types.SMALLINT  smallint         
//	RS> 4    object_id          java.sql.Types.INTEGER   int              
//	RS> 5    index_id           java.sql.Types.INTEGER   int              
//	RS> 6    user_seeks         java.sql.Types.BIGINT    bigint           
//	RS> 7    user_scans         java.sql.Types.BIGINT    bigint           
//	RS> 8    user_lookups       java.sql.Types.BIGINT    bigint           
//	RS> 9    user_updates       java.sql.Types.BIGINT    bigint           
//	RS> 10   last_user_seek     java.sql.Types.TIMESTAMP datetime         
//	RS> 11   last_user_scan     java.sql.Types.TIMESTAMP datetime         
//	RS> 12   last_user_lookup   java.sql.Types.TIMESTAMP datetime         
//	RS> 13   last_user_update   java.sql.Types.TIMESTAMP datetime         
//	RS> 14   system_seeks       java.sql.Types.BIGINT    bigint           
//	RS> 15   system_scans       java.sql.Types.BIGINT    bigint           
//	RS> 16   system_lookups     java.sql.Types.BIGINT    bigint           
//	RS> 17   system_updates     java.sql.Types.BIGINT    bigint           
//	RS> 18   last_system_seek   java.sql.Types.TIMESTAMP datetime         
//	RS> 19   last_system_scan   java.sql.Types.TIMESTAMP datetime         
//	RS> 20   last_system_lookup java.sql.Types.TIMESTAMP datetime         
//	RS> 21   last_system_update java.sql.Types.TIMESTAMP datetime         
	
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

		return new CmIndexUsage(counterController, guiController);
	}

	public CmIndexUsage(ICounterController counterController, IGuiController guiController)
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
		pkCols.add("index_id");

		return pkCols;
	}

	@Override
	public String getSqlForVersion(Connection conn, int aseVersion, boolean isClusterEnabled)
	{
		String sql = "select dbname=db_name(database_id), objectName=object_name(object_id, database_id), * from sys.dm_db_index_usage_stats";

		return sql;
	}
}
