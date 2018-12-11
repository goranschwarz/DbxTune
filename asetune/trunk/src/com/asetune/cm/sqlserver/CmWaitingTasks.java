package com.asetune.cm.sqlserver;

import java.sql.Connection;
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
public class CmWaitingTasks
extends CountersModel
{
//	private static Logger        _logger          = Logger.getLogger(CmWaitingTasks.class);
	private static final long    serialVersionUID = 1L;

	public static final String   CM_NAME          = CmWaitingTasks.class.getSimpleName();
	public static final String   SHORT_NAME       = "Waiting Tasks";
	public static final String   HTML_DESC        = 
		"<html>" +
		"<p>FIXME</p>" +
		"</html>";

	public static final String   GROUP_NAME       = MainFrame.TCP_GROUP_SERVER;
	public static final String   GUI_ICON_FILE    = "images/"+CM_NAME+".png";

	public static final long     NEED_SRV_VERSION = 0;
	public static final long     NEED_CE_VERSION  = 0;

	public static final String[] MON_TABLES       = new String[] {"dm_os_waiting_tasks"};
	public static final String[] NEED_ROLES       = new String[] {};//{"VIEW SERVER STATE"};
	public static final String[] NEED_CONFIG      = new String[] {};

	public static final String[] PCT_COLUMNS      = new String[] {};
	public static final String[] DIFF_COLUMNS     = new String[] {};
//	public static final String[] DIFF_COLUMNS     = new String[] {
//		"_last_column_name_only_used_as_a_place_holder_here"
//		};

// Microsoft SQL Server 2008 R2 (SP2) - 10.50.4000.0 (X64)  	Jun 28 2012 08:36:30  	Copyright (c) Microsoft Corporation 	Express Edition with Advanced Services (64-bit) on Windows NT 6.1 <X64> (Build 7601: Service Pack 1)

//	RS> Col# Label                    JDBC Type Name           Guessed DBMS type Source Table
//	RS> ---- ------------------------ ------------------------ ----------------- ------------
//	RS> 1    waiting_task_address     java.sql.Types.VARBINARY varbinary(16)     -none-      
//	RS> 2    session_id               java.sql.Types.SMALLINT  smallint          -none-      
//	RS> 3    exec_context_id          java.sql.Types.INTEGER   int               -none-      
//	RS> 4    wait_duration_ms         java.sql.Types.BIGINT    bigint            -none-      
//	RS> 5    wait_type                java.sql.Types.NVARCHAR  nvarchar(60)      -none-      
//	RS> 6    resource_address         java.sql.Types.VARBINARY varbinary(16)     -none-      
//	RS> 7    blocking_task_address    java.sql.Types.VARBINARY varbinary(16)     -none-      
//	RS> 8    blocking_session_id      java.sql.Types.SMALLINT  smallint          -none-      
//	RS> 9    blocking_exec_context_id java.sql.Types.INTEGER   int               -none-      
//	RS> 10   resource_description     java.sql.Types.NVARCHAR  nvarchar(2048)    -none-      

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

		return new CmWaitingTasks(counterController, guiController);
	}

	public CmWaitingTasks(ICounterController counterController, IGuiController guiController)
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
	public String[] getDependsOnConfigForVersion(Connection conn, long srvVersion, boolean isClusterEnabled)
	{
		return NEED_CONFIG;
	}

	@Override
	public List<String> getPkForVersion(Connection conn, long srvVersion, boolean isClusterEnabled)
	{
//		List <String> pkCols = new LinkedList<String>();
//
//		pkCols.add("session_id");
//
//		return pkCols;

		// no need to have PK, since we are NOT using "diff" counters
		return null;
	}

	@Override
	public String getSqlForVersion(Connection conn, long srvVersion, boolean isClusterEnabled)
	{
		String sql = 
			"select * \n" +
			"from sys.dm_os_waiting_tasks \n" +
			"where session_id is not null \n" +
			"";

		return sql;
	}
}
