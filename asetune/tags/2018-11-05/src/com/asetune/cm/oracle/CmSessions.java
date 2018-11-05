package com.asetune.cm.oracle;

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
public class CmSessions
extends CountersModel
{
//	private static Logger        _logger          = Logger.getLogger(CmServiceMemory.class);
	private static final long    serialVersionUID = 1L;

	public static final String   CM_NAME          = CmSessions.class.getSimpleName();
	public static final String   SHORT_NAME       = "Sessions";
	public static final String   HTML_DESC        = 
		"<html>" +
		"FIXME" +
		"</html>";

	public static final String   GROUP_NAME       = MainFrame.TCP_GROUP_SERVER;
	public static final String   GUI_ICON_FILE    = "images/"+CM_NAME+".png";

	public static final int      NEED_SRV_VERSION = 0;
	public static final int      NEED_CE_VERSION  = 0;

	public static final String[] MON_TABLES       = new String[] {};
	public static final String[] NEED_ROLES       = new String[] {};
	public static final String[] NEED_CONFIG      = new String[] {};

	public static final String[] PCT_COLUMNS      = new String[] {};
	public static final String[] DIFF_COLUMNS     = new String[] {};

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

		return new CmSessions(counterController, guiController);
	}

	public CmSessions(ICounterController counterController, IGuiController guiController)
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
	public String[] getDependsOnConfigForVersion(Connection conn, int srvVersion, boolean isClusterEnabled)
	{
		return NEED_CONFIG;
	}

	@Override
	public List<String> getPkForVersion(Connection conn, int srvVersion, boolean isClusterEnabled)
	{
		return null;
//		List <String> pkCols = new LinkedList<String>();
//
//		pkCols.add("SID");
//
//		return pkCols;
	}

	@Override
	public String getSqlForVersion(Connection conn, int aseVersion, boolean isClusterEnabled)
	{
		String sql = "select * from V$SESSION";

		return sql;
	}
}

//-----------------------------------
// select * from v$session
//-----------------------------------
//    RS> Col# Label                         JDBC Type Name           Guessed DBMS type
//    RS> ---- ----------------------------- ------------------------ -----------------
//    RS> 1    SADDR                         java.sql.Types.VARBINARY RAW(8)           
//    RS> 2    SID                           java.sql.Types.NUMERIC   NUMBER(0,-127)   
//    RS> 3    SERIAL#                       java.sql.Types.NUMERIC   NUMBER(0,-127)   
//    RS> 4    AUDSID                        java.sql.Types.NUMERIC   NUMBER(0,-127)   
//    RS> 5    PADDR                         java.sql.Types.VARBINARY RAW(8)           
//    RS> 6    USER#                         java.sql.Types.NUMERIC   NUMBER(0,-127)   
//    RS> 7    USERNAME                      java.sql.Types.VARCHAR   VARCHAR2(30)     
//    RS> 8    COMMAND                       java.sql.Types.NUMERIC   NUMBER(0,-127)   
//    RS> 9    OWNERID                       java.sql.Types.NUMERIC   NUMBER(0,-127)   
//    RS> 10   TADDR                         java.sql.Types.VARCHAR   VARCHAR2(16)     
//    RS> 11   LOCKWAIT                      java.sql.Types.VARCHAR   VARCHAR2(16)     
//    RS> 12   STATUS                        java.sql.Types.VARCHAR   VARCHAR2(8)      
//    RS> 13   SERVER                        java.sql.Types.VARCHAR   VARCHAR2(9)      
//    RS> 14   SCHEMA#                       java.sql.Types.NUMERIC   NUMBER(0,-127)   
//    RS> 15   SCHEMANAME                    java.sql.Types.VARCHAR   VARCHAR2(30)     
//    RS> 16   OSUSER                        java.sql.Types.VARCHAR   VARCHAR2(30)     
//    RS> 17   PROCESS                       java.sql.Types.VARCHAR   VARCHAR2(24)     
//    RS> 18   MACHINE                       java.sql.Types.VARCHAR   VARCHAR2(64)     
//    RS> 19   PORT                          java.sql.Types.NUMERIC   NUMBER(0,-127)   
//    RS> 20   TERMINAL                      java.sql.Types.VARCHAR   VARCHAR2(30)     
//    RS> 21   PROGRAM                       java.sql.Types.VARCHAR   VARCHAR2(48)     
//    RS> 22   TYPE                          java.sql.Types.VARCHAR   VARCHAR2(10)     
//    RS> 23   SQL_ADDRESS                   java.sql.Types.VARBINARY RAW(8)           
//    RS> 24   SQL_HASH_VALUE                java.sql.Types.NUMERIC   NUMBER(0,-127)   
//    RS> 25   SQL_ID                        java.sql.Types.VARCHAR   VARCHAR2(13)     
//    RS> 26   SQL_CHILD_NUMBER              java.sql.Types.NUMERIC   NUMBER(0,0)      
//    RS> 27   SQL_EXEC_START                java.sql.Types.TIMESTAMP DATE             
//    RS> 28   SQL_EXEC_ID                   java.sql.Types.NUMERIC   NUMBER(0,0)      
//    RS> 29   PREV_SQL_ADDR                 java.sql.Types.VARBINARY RAW(8)           
//    RS> 30   PREV_HASH_VALUE               java.sql.Types.NUMERIC   NUMBER(0,-127)   
//    RS> 31   PREV_SQL_ID                   java.sql.Types.VARCHAR   VARCHAR2(13)     
//    RS> 32   PREV_CHILD_NUMBER             java.sql.Types.NUMERIC   NUMBER(0,0)      
//    RS> 33   PREV_EXEC_START               java.sql.Types.TIMESTAMP DATE             
//    RS> 34   PREV_EXEC_ID                  java.sql.Types.NUMERIC   NUMBER(0,0)      
//    RS> 35   PLSQL_ENTRY_OBJECT_ID         java.sql.Types.NUMERIC   NUMBER(0,0)      
//    RS> 36   PLSQL_ENTRY_SUBPROGRAM_ID     java.sql.Types.NUMERIC   NUMBER(0,0)      
//    RS> 37   PLSQL_OBJECT_ID               java.sql.Types.NUMERIC   NUMBER(0,0)      
//    RS> 38   PLSQL_SUBPROGRAM_ID           java.sql.Types.NUMERIC   NUMBER(0,0)      
//    RS> 39   MODULE                        java.sql.Types.VARCHAR   VARCHAR2(48)     
//    RS> 40   MODULE_HASH                   java.sql.Types.NUMERIC   NUMBER(0,-127)   
//    RS> 41   ACTION                        java.sql.Types.VARCHAR   VARCHAR2(32)     
//    RS> 42   ACTION_HASH                   java.sql.Types.NUMERIC   NUMBER(0,-127)   
//    RS> 43   CLIENT_INFO                   java.sql.Types.VARCHAR   VARCHAR2(64)     
//    RS> 44   FIXED_TABLE_SEQUENCE          java.sql.Types.NUMERIC   NUMBER(0,-127)   
//    RS> 45   ROW_WAIT_OBJ#                 java.sql.Types.NUMERIC   NUMBER(0,-127)   
//    RS> 46   ROW_WAIT_FILE#                java.sql.Types.NUMERIC   NUMBER(0,-127)   
//    RS> 47   ROW_WAIT_BLOCK#               java.sql.Types.NUMERIC   NUMBER(0,-127)   
//    RS> 48   ROW_WAIT_ROW#                 java.sql.Types.NUMERIC   NUMBER(0,-127)   
//    RS> 49   TOP_LEVEL_CALL#               java.sql.Types.NUMERIC   NUMBER(0,-127)   
//    RS> 50   LOGON_TIME                    java.sql.Types.TIMESTAMP DATE             
//    RS> 51   LAST_CALL_ET                  java.sql.Types.NUMERIC   NUMBER(0,-127)   
//    RS> 52   PDML_ENABLED                  java.sql.Types.VARCHAR   VARCHAR2(3)      
//    RS> 53   FAILOVER_TYPE                 java.sql.Types.VARCHAR   VARCHAR2(13)     
//    RS> 54   FAILOVER_METHOD               java.sql.Types.VARCHAR   VARCHAR2(10)     
//    RS> 55   FAILED_OVER                   java.sql.Types.VARCHAR   VARCHAR2(3)      
//    RS> 56   RESOURCE_CONSUMER_GROUP       java.sql.Types.VARCHAR   VARCHAR2(32)     
//    RS> 57   PDML_STATUS                   java.sql.Types.VARCHAR   VARCHAR2(8)      
//    RS> 58   PDDL_STATUS                   java.sql.Types.VARCHAR   VARCHAR2(8)      
//    RS> 59   PQ_STATUS                     java.sql.Types.VARCHAR   VARCHAR2(8)      
//    RS> 60   CURRENT_QUEUE_DURATION        java.sql.Types.NUMERIC   NUMBER(0,-127)   
//    RS> 61   CLIENT_IDENTIFIER             java.sql.Types.VARCHAR   VARCHAR2(64)     
//    RS> 62   BLOCKING_SESSION_STATUS       java.sql.Types.VARCHAR   VARCHAR2(11)     
//    RS> 63   BLOCKING_INSTANCE             java.sql.Types.NUMERIC   NUMBER(0,0)      
//    RS> 64   BLOCKING_SESSION              java.sql.Types.NUMERIC   NUMBER(0,0)      
//    RS> 65   FINAL_BLOCKING_SESSION_STATUS java.sql.Types.VARCHAR   VARCHAR2(11)     
//    RS> 66   FINAL_BLOCKING_INSTANCE       java.sql.Types.NUMERIC   NUMBER(0,0)      
//    RS> 67   FINAL_BLOCKING_SESSION        java.sql.Types.NUMERIC   NUMBER(0,0)      
//    RS> 68   SEQ#                          java.sql.Types.NUMERIC   NUMBER(0,-127)   
//    RS> 69   EVENT#                        java.sql.Types.NUMERIC   NUMBER(0,-127)   
//    RS> 70   EVENT                         java.sql.Types.VARCHAR   VARCHAR2(64)     
//    RS> 71   P1TEXT                        java.sql.Types.VARCHAR   VARCHAR2(64)     
//    RS> 72   P1                            java.sql.Types.NUMERIC   NUMBER(0,-127)   
//    RS> 73   P1RAW                         java.sql.Types.VARBINARY RAW(8)           
//    RS> 74   P2TEXT                        java.sql.Types.VARCHAR   VARCHAR2(64)     
//    RS> 75   P2                            java.sql.Types.NUMERIC   NUMBER(0,-127)   
//    RS> 76   P2RAW                         java.sql.Types.VARBINARY RAW(8)           
//    RS> 77   P3TEXT                        java.sql.Types.VARCHAR   VARCHAR2(64)     
//    RS> 78   P3                            java.sql.Types.NUMERIC   NUMBER(0,-127)   
//    RS> 79   P3RAW                         java.sql.Types.VARBINARY RAW(8)           
//    RS> 80   WAIT_CLASS_ID                 java.sql.Types.NUMERIC   NUMBER(0,-127)   
//    RS> 81   WAIT_CLASS#                   java.sql.Types.NUMERIC   NUMBER(0,-127)   
//    RS> 82   WAIT_CLASS                    java.sql.Types.VARCHAR   VARCHAR2(64)     
//    RS> 83   WAIT_TIME                     java.sql.Types.NUMERIC   NUMBER(0,0)      
//    RS> 84   SECONDS_IN_WAIT               java.sql.Types.NUMERIC   NUMBER(0,0)      
//    RS> 85   STATE                         java.sql.Types.VARCHAR   VARCHAR2(19)     
//    RS> 86   WAIT_TIME_MICRO               java.sql.Types.NUMERIC   NUMBER(0,-127)   
//    RS> 87   TIME_REMAINING_MICRO          java.sql.Types.NUMERIC   NUMBER(0,0)      
//    RS> 88   TIME_SINCE_LAST_WAIT_MICRO    java.sql.Types.NUMERIC   NUMBER(0,-127)   
//    RS> 89   SERVICE_NAME                  java.sql.Types.VARCHAR   VARCHAR2(64)     
//    RS> 90   SQL_TRACE                     java.sql.Types.VARCHAR   VARCHAR2(8)      
//    RS> 91   SQL_TRACE_WAITS               java.sql.Types.VARCHAR   VARCHAR2(5)      
//    RS> 92   SQL_TRACE_BINDS               java.sql.Types.VARCHAR   VARCHAR2(5)      
//    RS> 93   SQL_TRACE_PLAN_STATS          java.sql.Types.VARCHAR   VARCHAR2(10)     
//    RS> 94   SESSION_EDITION_ID            java.sql.Types.NUMERIC   NUMBER(0,-127)   
//    RS> 95   CREATOR_ADDR                  java.sql.Types.VARBINARY RAW(8)           
//    RS> 96   CREATOR_SERIAL#               java.sql.Types.NUMERIC   NUMBER(0,-127)   
//    RS> 97   ECID                          java.sql.Types.VARCHAR   VARCHAR2(64)     

