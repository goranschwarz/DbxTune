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

import java.util.LinkedList;
import java.util.List;

import com.asetune.ICounterController;
import com.asetune.IGuiController;
import com.asetune.cm.CounterSetTemplates;
import com.asetune.cm.CounterSetTemplates.Type;
import com.asetune.cm.CountersModel;
import com.asetune.gui.MainFrame;
import com.asetune.sql.conn.DbxConnection;
import com.asetune.sql.conn.info.DbmsVersionInfo;

/**
 * @author Goran Schwarz (goran_schwarz@hotmail.com)
 */
public class CmCsTables
extends CountersModel
{
//	private static Logger        _logger          = Logger.getLogger(CmServiceMemory.class);
	private static final long    serialVersionUID = 1L;

	public static final String   CM_NAME          = CmCsTables.class.getSimpleName();
	public static final String   SHORT_NAME       = "Column Tables";
	public static final String   HTML_DESC        = 
		"<html>" +
		"<p>FIXME</p>" +
		"</html>";

	public static final String   GROUP_NAME       = MainFrame.TCP_GROUP_OBJECT_ACCESS;
	public static final String   GUI_ICON_FILE    = "images/"+CM_NAME+".png";

	public static final long     NEED_SRV_VERSION = 0;
	public static final long     NEED_CE_VERSION  = 0;

	public static final String[] MON_TABLES       = new String[] {"M_CS_TABLES"};
	public static final String[] NEED_ROLES       = new String[] {};
	public static final String[] NEED_CONFIG      = new String[] {};

	public static final String[] PCT_COLUMNS      = new String[] {};
	public static final String[] DIFF_COLUMNS     = new String[] {
		"MEMORY_SIZE_IN_TOTAL",               // java.sql.Types.BIGINT    BIGINT           
		"MEMORY_SIZE_IN_MAIN",                // java.sql.Types.BIGINT    BIGINT           
		"MEMORY_SIZE_IN_DELTA",               // java.sql.Types.BIGINT    BIGINT           
		"MEMORY_SIZE_IN_HISTORY_MAIN",        // java.sql.Types.BIGINT    BIGINT           
		"MEMORY_SIZE_IN_HISTORY_DELTA",       // java.sql.Types.BIGINT    BIGINT           
		"ESTIMATED_MAX_MEMORY_SIZE_IN_TOTAL", // java.sql.Types.BIGINT    BIGINT           
		"LAST_ESTIMATED_MEMORY_SIZE",         // java.sql.Types.BIGINT    BIGINT           
		"RECORD_COUNT",                       // java.sql.Types.BIGINT    BIGINT           
		"RAW_RECORD_COUNT_IN_MAIN",           // java.sql.Types.BIGINT    BIGINT           
		"RAW_RECORD_COUNT_IN_DELTA",          // java.sql.Types.BIGINT    BIGINT           
		"RAW_RECORD_COUNT_IN_HISTORY_MAIN",   // java.sql.Types.BIGINT    BIGINT           
		"RAW_RECORD_COUNT_IN_HISTORY_DELTA",  // java.sql.Types.BIGINT    BIGINT           
		"LAST_COMPRESSED_RECORD_COUNT",       // java.sql.Types.BIGINT    BIGINT           
//		"MAX_UDIV",                           // java.sql.Types.BIGINT    BIGINT           
//		"MAX_MERGE_CID",                      // java.sql.Types.BIGINT    BIGINT           
//		"MAX_ROWID",                          // java.sql.Types.BIGINT    BIGINT           
		"READ_COUNT",                         // java.sql.Types.BIGINT    BIGINT           
		"WRITE_COUNT",                        // java.sql.Types.BIGINT    BIGINT           
		"MERGE_COUNT",                        // java.sql.Types.BIGINT    BIGINT           
//		"UNUSED_RETENTION_PERIOD",            // java.sql.Types.INTEGER   INTEGER          
		"-last-dummy-col-"
		};

//	1> select * from SYS.M_CS_TABLES
//	RS> Col# Label                              JDBC Type Name           Guessed DBMS type
//	RS> ---- ---------------------------------- ------------------------ -----------------
//	RS> 1    HOST                               java.sql.Types.VARCHAR   VARCHAR(64)      
//	RS> 2    PORT                               java.sql.Types.INTEGER   INTEGER          
//	RS> 3    SCHEMA_NAME                        java.sql.Types.NVARCHAR  NVARCHAR(256)    
//	RS> 4    TABLE_NAME                         java.sql.Types.NVARCHAR  NVARCHAR(256)    
//	RS> 5    PART_ID                            java.sql.Types.INTEGER   INTEGER          
//	RS> 6    MEMORY_SIZE_IN_TOTAL               java.sql.Types.BIGINT    BIGINT           
//	RS> 7    MEMORY_SIZE_IN_MAIN                java.sql.Types.BIGINT    BIGINT           
//	RS> 8    MEMORY_SIZE_IN_DELTA               java.sql.Types.BIGINT    BIGINT           
//	RS> 9    MEMORY_SIZE_IN_HISTORY_MAIN        java.sql.Types.BIGINT    BIGINT           
//	RS> 10   MEMORY_SIZE_IN_HISTORY_DELTA       java.sql.Types.BIGINT    BIGINT           
//	RS> 11   ESTIMATED_MAX_MEMORY_SIZE_IN_TOTAL java.sql.Types.BIGINT    BIGINT           
//	RS> 12   LAST_ESTIMATED_MEMORY_SIZE         java.sql.Types.BIGINT    BIGINT           
//	RS> 13   LAST_ESTIMATED_MEMORY_SIZE_TIME    java.sql.Types.TIMESTAMP TIMESTAMP        
//	RS> 14   RECORD_COUNT                       java.sql.Types.BIGINT    BIGINT           
//	RS> 15   RAW_RECORD_COUNT_IN_MAIN           java.sql.Types.BIGINT    BIGINT           
//	RS> 16   RAW_RECORD_COUNT_IN_DELTA          java.sql.Types.BIGINT    BIGINT           
//	RS> 17   RAW_RECORD_COUNT_IN_HISTORY_MAIN   java.sql.Types.BIGINT    BIGINT           
//	RS> 18   RAW_RECORD_COUNT_IN_HISTORY_DELTA  java.sql.Types.BIGINT    BIGINT           
//	RS> 19   LAST_COMPRESSED_RECORD_COUNT       java.sql.Types.BIGINT    BIGINT           
//	RS> 20   MAX_UDIV                           java.sql.Types.BIGINT    BIGINT           
//	RS> 21   MAX_MERGE_CID                      java.sql.Types.BIGINT    BIGINT           
//	RS> 22   MAX_ROWID                          java.sql.Types.BIGINT    BIGINT           
//	RS> 23   IS_DELTA2_ACTIVE                   java.sql.Types.VARCHAR   VARCHAR(5)       
//	RS> 24   IS_DELTA_LOADED                    java.sql.Types.VARCHAR   VARCHAR(5)       
//	RS> 25   IS_LOG_DELTA                       java.sql.Types.VARCHAR   VARCHAR(5)       
//	RS> 26   PERSISTENT_MERGE                   java.sql.Types.VARCHAR   VARCHAR(5)       
//	RS> 27   CREATE_TIME                        java.sql.Types.TIMESTAMP TIMESTAMP        
//	RS> 28   MODIFY_TIME                        java.sql.Types.TIMESTAMP TIMESTAMP        
//	RS> 29   LAST_MERGE_TIME                    java.sql.Types.TIMESTAMP TIMESTAMP        
//	RS> 30   LAST_REPLAY_LOG_TIME               java.sql.Types.TIMESTAMP TIMESTAMP        
//	RS> 31   LAST_TRUNCATION_TIME               java.sql.Types.TIMESTAMP TIMESTAMP        
//	RS> 32   LOADED                             java.sql.Types.VARCHAR   VARCHAR(10)      
//	RS> 33   READ_COUNT                         java.sql.Types.BIGINT    BIGINT           
//	RS> 34   WRITE_COUNT                        java.sql.Types.BIGINT    BIGINT           
//	RS> 35   MERGE_COUNT                        java.sql.Types.BIGINT    BIGINT           
//	RS> 36   IS_REPLICA                         java.sql.Types.VARCHAR   VARCHAR(5)       
//	RS> 37   UNUSED_RETENTION_PERIOD            java.sql.Types.INTEGER   INTEGER          
//	+------------+-----+---------------+-----------------------------------------------------------------------------+-------+--------------------+-------------------+--------------------+---------------------------+----------------------------+----------------------------------+--------------------------+-------------------------------+------------+------------------------+-------------------------+--------------------------------+---------------------------------+----------------------------+--------+-------------+---------+----------------+---------------+------------+----------------+-----------------------+-----------------------+-----------------------+-----------------------+-----------------------+---------+----------+-----------+-----------+----------+-----------------------+
//	|HOST        |PORT |SCHEMA_NAME    |TABLE_NAME                                                                   |PART_ID|MEMORY_SIZE_IN_TOTAL|MEMORY_SIZE_IN_MAIN|MEMORY_SIZE_IN_DELTA|MEMORY_SIZE_IN_HISTORY_MAIN|MEMORY_SIZE_IN_HISTORY_DELTA|ESTIMATED_MAX_MEMORY_SIZE_IN_TOTAL|LAST_ESTIMATED_MEMORY_SIZE|LAST_ESTIMATED_MEMORY_SIZE_TIME|RECORD_COUNT|RAW_RECORD_COUNT_IN_MAIN|RAW_RECORD_COUNT_IN_DELTA|RAW_RECORD_COUNT_IN_HISTORY_MAIN|RAW_RECORD_COUNT_IN_HISTORY_DELTA|LAST_COMPRESSED_RECORD_COUNT|MAX_UDIV|MAX_MERGE_CID|MAX_ROWID|IS_DELTA2_ACTIVE|IS_DELTA_LOADED|IS_LOG_DELTA|PERSISTENT_MERGE|CREATE_TIME            |MODIFY_TIME            |LAST_MERGE_TIME        |LAST_REPLAY_LOG_TIME   |LAST_TRUNCATION_TIME   |LOADED   |READ_COUNT|WRITE_COUNT|MERGE_COUNT|IS_REPLICA|UNUSED_RETENTION_PERIOD|
//	+------------+-----+---------------+-----------------------------------------------------------------------------+-------+--------------------+-------------------+--------------------+---------------------------+----------------------------+----------------------------------+--------------------------+-------------------------------+------------+------------------------+-------------------------+--------------------------------+---------------------------------+----------------------------+--------+-------------+---------+----------------+---------------+------------+----------------+-----------------------+-----------------------+-----------------------+-----------------------+-----------------------+---------+----------+-----------+-----------+----------+-----------------------+
//	|mo-b402c54f9|30003|BILLING        |ACCOUNTMANAGERS                                                              |0      |0                   |0                  |0                   |0                          |0                           |118408                            |151800                    |2015-03-19 20:10:17.69         |0           |0                       |0                        |0                               |0                                |0                           |0       |0            |0        |FALSE           |FALSE          |TRUE        |TRUE            |2015-03-19 20:10:17.69 |(NULL)                 |(NULL)                 |2015-03-19 20:10:17.695|(NULL)                 |NO       |8583      |0          |0          |FALSE     |0                      |
//	|mo-b402c54f9|30003|_SYS_REPO      |ACTIVATION_HELPER2                                                           |0      |357308              |9216               |348092              |0                          |0                           |366092                            |138000                    |2015-04-29 15:32:03.052        |1           |0                       |1                        |0                               |0                                |0                           |0       |0            |1        |FALSE           |TRUE           |TRUE        |TRUE            |2015-03-16 12:20:14.83 |2015-04-29 15:32:03.052|2015-04-29 15:32:03.052|2015-04-29 15:32:03.112|2015-04-29 15:32:03.052|PARTIALLY|8833      |8          |0          |FALSE     |0                      |
//	|mo-b402c54f9|30003|_SYS_REPO      |ACTIVE_CONTENT_TEXT                                                          |0      |623227              |546051             |77176               |0                          |0                           |636988                            |636988                    |2015-04-16 10:54:34.099        |5394        |5394                    |0                        |0                               |0                                |0                           |5394    |0            |5439     |FALSE           |TRUE           |TRUE        |TRUE            |2015-03-16 12:20:14.964|(NULL)                 |2015-04-16 10:54:34.099|2015-04-24 13:51:20.349|(NULL)                 |PARTIALLY|8737      |0          |0          |FALSE     |0                      |
//	+------------+-----+---------------+-----------------------------------------------------------------------------+-------+--------------------+-------------------+--------------------+---------------------------+----------------------------+----------------------------------+--------------------------+-------------------------------+------------+------------------------+-------------------------+--------------------------------+---------------------------------+----------------------------+--------+-------------+---------+----------------+---------------+------------+----------------+-----------------------+-----------------------+-----------------------+-----------------------+-----------------------+---------+----------+-----------+-----------+----------+-----------------------+

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

		return new CmCsTables(counterController, guiController);
	}

	public CmCsTables(ICounterController counterController, IGuiController guiController)
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

		pkCols.add("HOST");
		pkCols.add("PORT");
		pkCols.add("SCHEMA_NAME");
		pkCols.add("TABLE_NAME");
		pkCols.add("PART_ID");

		return pkCols;
	}

	@Override
	public String getSqlForVersion(DbxConnection conn, DbmsVersionInfo versionInfo)
	{
		String sql = "select * from M_CS_TABLES";

		return sql;
	}
}
