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
package com.asetune.cm.hana;

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
public class CmActiveStatements
extends CountersModel
{
//	private static Logger        _logger          = Logger.getLogger(CmServiceMemory.class);
	private static final long    serialVersionUID = 1L;

	public static final String   CM_NAME          = CmActiveStatements.class.getSimpleName();
	public static final String   SHORT_NAME       = "Active Statements";
	public static final String   HTML_DESC        = 
		"<html>" +
		"<p>FIXME</p>" +
		"</html>";

	public static final String   GROUP_NAME       = MainFrame.TCP_GROUP_OBJECT_ACCESS;
	public static final String   GUI_ICON_FILE    = "images/"+CM_NAME+".png";

	public static final long     NEED_SRV_VERSION = 0;
	public static final long     NEED_CE_VERSION  = 0;

	public static final String[] MON_TABLES       = new String[] {"M_ACTIVE_STATEMENTS"};
	public static final String[] NEED_ROLES       = new String[] {};
	public static final String[] NEED_CONFIG      = new String[] {};

	public static final String[] PCT_COLUMNS      = new String[] {};
	public static final String[] DIFF_COLUMNS     = new String[] {
		"ALLOCATED_MEMORY_SIZE",       // java.sql.Types.BIGINT    BIGINT           
		"USED_MEMORY_SIZE",            // java.sql.Types.BIGINT    BIGINT           
		"RECOMPILE_COUNT",             // java.sql.Types.BIGINT    BIGINT           
		"EXECUTION_COUNT",             // java.sql.Types.BIGINT    BIGINT           
		"TOTAL_EXECUTION_TIME",        // java.sql.Types.BIGINT    BIGINT           
		"TOTAL_CURSOR_DURATION",       // java.sql.Types.BIGINT    BIGINT           
		"TOTAL_EXECUTION_MEMORY_SIZE", // java.sql.Types.BIGINT    BIGINT           
		"TOTAL_LOCKWAIT_COUNT",        // java.sql.Types.BIGINT    BIGINT           
		"TOTAL_LOCKWAIT_TIME",         // java.sql.Types.BIGINT    BIGINT           
		"TOTAL_PREPARATION_TIME",      // java.sql.Types.BIGINT    BIGINT           
		"TOTAL_PREPARATION_COUNT",     // java.sql.Types.BIGINT    BIGINT           
		"-last-dummy-col-"
		};
//	1> select * from M_ACTIVE_STATEMENTS
//	RS> Col# Label                       JDBC Type Name           Guessed DBMS type
//	RS> ---- --------------------------- ------------------------ -----------------
//	RS> 1    HOST                        java.sql.Types.VARCHAR   VARCHAR(64)      
//	RS> 2    PORT                        java.sql.Types.INTEGER   INTEGER          
//	RS> 3    CONNECTION_ID               java.sql.Types.INTEGER   INTEGER          
//	RS> 4    STATEMENT_ID                java.sql.Types.VARCHAR   VARCHAR(256)     
//	RS> 5    START_MVCC_TIMESTAMP        java.sql.Types.BIGINT    BIGINT           
//	RS> 6    COMPILED_TIME               java.sql.Types.TIMESTAMP TIMESTAMP        
//	RS> 7    STATEMENT_STATUS            java.sql.Types.VARCHAR   VARCHAR(128)     
//	RS> 8    STATEMENT_STRING            java.sql.Types.NCLOB     NCLOB            
//	RS> 9    ALLOCATED_MEMORY_SIZE       java.sql.Types.BIGINT    BIGINT           
//	RS> 10   USED_MEMORY_SIZE            java.sql.Types.BIGINT    BIGINT           
//	RS> 11   PLAN_ID                     java.sql.Types.BIGINT    BIGINT           
//	RS> 12   LAST_EXECUTED_TIME          java.sql.Types.TIMESTAMP TIMESTAMP        
//	RS> 13   LAST_ACTION_TIME            java.sql.Types.TIMESTAMP TIMESTAMP        
//	RS> 14   RECOMPILE_COUNT             java.sql.Types.BIGINT    BIGINT           
//	RS> 15   EXECUTION_COUNT             java.sql.Types.BIGINT    BIGINT           
//	RS> 16   AVG_EXECUTION_TIME          java.sql.Types.BIGINT    BIGINT           
//	RS> 17   MAX_EXECUTION_TIME          java.sql.Types.BIGINT    BIGINT           
//	RS> 18   MIN_EXECUTION_TIME          java.sql.Types.BIGINT    BIGINT           
//	RS> 19   TOTAL_EXECUTION_TIME        java.sql.Types.BIGINT    BIGINT           
//	RS> 20   AVG_CURSOR_DURATION         java.sql.Types.BIGINT    BIGINT           
//	RS> 21   MAX_CURSOR_DURATION         java.sql.Types.BIGINT    BIGINT           
//	RS> 22   MIN_CURSOR_DURATION         java.sql.Types.BIGINT    BIGINT           
//	RS> 23   TOTAL_CURSOR_DURATION       java.sql.Types.BIGINT    BIGINT           
//	RS> 24   AVG_EXECUTION_MEMORY_SIZE   java.sql.Types.BIGINT    BIGINT           
//	RS> 25   MAX_EXECUTION_MEMORY_SIZE   java.sql.Types.BIGINT    BIGINT           
//	RS> 26   MIN_EXECUTION_MEMORY_SIZE   java.sql.Types.BIGINT    BIGINT           
//	RS> 27   TOTAL_EXECUTION_MEMORY_SIZE java.sql.Types.BIGINT    BIGINT           
//	RS> 28   AVG_LOCKWAIT_TIME           java.sql.Types.BIGINT    BIGINT           
//	RS> 29   MAX_LOCKWAIT_TIME           java.sql.Types.BIGINT    BIGINT           
//	RS> 30   MIN_LOCKWAIT_TIME           java.sql.Types.BIGINT    BIGINT           
//	RS> 31   TOTAL_LOCKWAIT_COUNT        java.sql.Types.BIGINT    BIGINT           
//	RS> 32   TOTAL_LOCKWAIT_TIME         java.sql.Types.BIGINT    BIGINT           
//	RS> 33   AVG_PREPARATION_TIME        java.sql.Types.BIGINT    BIGINT           
//	RS> 34   MAX_PREPARATION_TIME        java.sql.Types.BIGINT    BIGINT           
//	RS> 35   MIN_PREPARATION_TIME        java.sql.Types.BIGINT    BIGINT           
//	RS> 36   TOTAL_PREPARATION_TIME      java.sql.Types.BIGINT    BIGINT           
//	RS> 37   TOTAL_PREPARATION_COUNT     java.sql.Types.BIGINT    BIGINT           
//	RS> 38   HAS_HOLDABLE_CURSOR         java.sql.Types.VARCHAR   VARCHAR(5)       
//	RS> 39   CURSOR_TYPE                 java.sql.Types.VARCHAR   VARCHAR(18)      
//	RS> 40   PARENT_STATEMENT_ID         java.sql.Types.VARCHAR   VARCHAR(256)     
//	RS> 41   APPLICATION_SOURCE          java.sql.Types.NVARCHAR  NVARCHAR(256)    
//	+------------+-----+-------------+----------------+--------------------+--------------------------+----------------+---------------------------------+---------------------+----------------+--------+--------------------------+--------------------------+---------------+---------------+------------------+------------------+------------------+--------------------+-------------------+-------------------+-------------------+---------------------+-------------------------+-------------------------+-------------------------+---------------------------+-----------------+-----------------+-----------------+--------------------+-------------------+--------------------+--------------------+--------------------+----------------------+-----------------------+-------------------+------------+-------------------+------------------+
//	|HOST        |PORT |CONNECTION_ID|STATEMENT_ID    |START_MVCC_TIMESTAMP|COMPILED_TIME             |STATEMENT_STATUS|STATEMENT_STRING                 |ALLOCATED_MEMORY_SIZE|USED_MEMORY_SIZE|PLAN_ID |LAST_EXECUTED_TIME        |LAST_ACTION_TIME          |RECOMPILE_COUNT|EXECUTION_COUNT|AVG_EXECUTION_TIME|MAX_EXECUTION_TIME|MIN_EXECUTION_TIME|TOTAL_EXECUTION_TIME|AVG_CURSOR_DURATION|MAX_CURSOR_DURATION|MIN_CURSOR_DURATION|TOTAL_CURSOR_DURATION|AVG_EXECUTION_MEMORY_SIZE|MAX_EXECUTION_MEMORY_SIZE|MIN_EXECUTION_MEMORY_SIZE|TOTAL_EXECUTION_MEMORY_SIZE|AVG_LOCKWAIT_TIME|MAX_LOCKWAIT_TIME|MIN_LOCKWAIT_TIME|TOTAL_LOCKWAIT_COUNT|TOTAL_LOCKWAIT_TIME|AVG_PREPARATION_TIME|MAX_PREPARATION_TIME|MIN_PREPARATION_TIME|TOTAL_PREPARATION_TIME|TOTAL_PREPARATION_COUNT|HAS_HOLDABLE_CURSOR|CURSOR_TYPE |PARENT_STATEMENT_ID|APPLICATION_SOURCE|
//	+------------+-----+-------------+----------------+--------------------+--------------------------+----------------+---------------------------------+---------------------+----------------+--------+--------------------------+--------------------------+---------------+---------------+------------------+------------------+------------------+--------------------+-------------------+-------------------+-------------------+---------------------+-------------------------+-------------------------+-------------------------+---------------------------+-----------------+-----------------+-----------------+--------------------+-------------------+--------------------+--------------------+--------------------+----------------------+-----------------------+-------------------+------------+-------------------+------------------+
//	|mo-b402c54f9|30003|301409       |1434566801762546|10833430            |2015-04-29 13:06:42.369603|ACTIVE          |select * from M_ACTIVE_STATEMENTS|42112                |41824           |20450003|2015-04-29 13:06:42.369951|2015-04-29 13:06:42.369951|0              |0              |0                 |0                 |0                 |0                   |0                  |0                  |0                  |0                    |0                        |0                        |0                        |0                          |0                |0                |0                |0                   |0                  |321                 |321                 |321                 |321                   |1                      |TRUE               |FORWARD ONLY|0                  |                  |
//	+------------+-----+-------------+----------------+--------------------+--------------------------+----------------+---------------------------------+---------------------+----------------+--------+--------------------------+--------------------------+---------------+---------------+------------------+------------------+------------------+--------------------+-------------------+-------------------+-------------------+---------------------+-------------------------+-------------------------+-------------------------+---------------------------+-----------------+-----------------+-----------------+--------------------+-------------------+--------------------+--------------------+--------------------+----------------------+-----------------------+-------------------+------------+-------------------+------------------+
//	Rows 1
//	(1 rows affected)

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

		return new CmActiveStatements(counterController, guiController);
	}

	public CmActiveStatements(ICounterController counterController, IGuiController guiController)
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
		List <String> pkCols = new LinkedList<String>();

		pkCols.add("HOST");
		pkCols.add("PORT");
		pkCols.add("CONNECTION_ID");
		pkCols.add("STATEMENT_ID");

		return pkCols;
	}

	@Override
	public String getSqlForVersion(Connection conn, long srvVersion, boolean isClusterEnabled)
	{
		String sql = "select * from M_ACTIVE_STATEMENTS where CONNECTION_ID != CURRENT_CONNECTION";

		return sql;
	}
}
