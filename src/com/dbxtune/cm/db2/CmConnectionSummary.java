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
package com.dbxtune.cm.db2;

import java.util.LinkedList;
import java.util.List;

import javax.naming.NameNotFoundException;

import com.dbxtune.ICounterController;
import com.dbxtune.IGuiController;
import com.dbxtune.cm.CounterSetTemplates;
import com.dbxtune.cm.CounterSetTemplates.Type;
import com.dbxtune.cm.CountersModel;
import com.dbxtune.config.dict.MonTablesDictionary;
import com.dbxtune.config.dict.MonTablesDictionaryManager;
import com.dbxtune.gui.MainFrame;
import com.dbxtune.sql.conn.DbxConnection;
import com.dbxtune.sql.conn.info.DbmsVersionInfo;

/**
 * @author Goran Schwarz (goran_schwarz@hotmail.com)
 */

public class CmConnectionSummary
extends CountersModel
{
	private static final long    serialVersionUID = 1L;

	public static final String   CM_NAME          = CmConnectionSummary.class.getSimpleName();
	public static final String   SHORT_NAME       = "Connection Summary";
	public static final String   HTML_DESC        = 
		"<html>" +
		"<h4>Connection Summary</h4>" + 
		"Fixme" +
		"</html>";

	public static final String   GROUP_NAME       = MainFrame.TCP_GROUP_SERVER;
	public static final String   GUI_ICON_FILE    = "images/"+CM_NAME+".png";

	public static final long     NEED_SRV_VERSION = 0;
	public static final long     NEED_CE_VERSION  = 0;

	public static final String[] MON_TABLES       = new String[] {"MON_CONNECTION_SUMMARY"};
	public static final String[] NEED_ROLES       = new String[] {};
	public static final String[] NEED_CONFIG      = new String[] {};

	public static final String[] PCT_COLUMNS      = new String[] {};
	public static final String[] DIFF_COLUMNS     = new String[] {
		"TOTAL_APP_COMMITS",
		"TOTAL_APP_ROLLBACKS",
		"ACT_COMPLETED_TOTAL",
		"APP_RQSTS_COMPLETED_TOTAL"
	};
//	RS> Col# Label                                  JDBC Type Name         Guessed DBMS type Source Table                           
//	RS> ---- -------------------------------------- ---------------------- ----------------- ---------------------------------------
//	RS> 1    APPLICATION_HANDLE                     java.sql.Types.BIGINT  BIGINT            SAMPLE.SYSIBMADM.MON_CONNECTION_SUMMARY
//	RS> 2    APPLICATION_NAME                       java.sql.Types.VARCHAR VARCHAR(128)      SAMPLE.SYSIBMADM.MON_CONNECTION_SUMMARY
//	RS> 3    APPLICATION_ID                         java.sql.Types.VARCHAR VARCHAR(128)      SAMPLE.SYSIBMADM.MON_CONNECTION_SUMMARY
//	RS> 4    SESSION_AUTH_ID                        java.sql.Types.VARCHAR VARCHAR(128)      SAMPLE.SYSIBMADM.MON_CONNECTION_SUMMARY
//	RS> 5    TOTAL_APP_COMMITS                      java.sql.Types.BIGINT  BIGINT            SAMPLE.SYSIBMADM.MON_CONNECTION_SUMMARY
//	RS> 6    TOTAL_APP_ROLLBACKS                    java.sql.Types.BIGINT  BIGINT            SAMPLE.SYSIBMADM.MON_CONNECTION_SUMMARY
//	RS> 7    ACT_COMPLETED_TOTAL                    java.sql.Types.BIGINT  BIGINT            SAMPLE.SYSIBMADM.MON_CONNECTION_SUMMARY
//	RS> 8    APP_RQSTS_COMPLETED_TOTAL              java.sql.Types.BIGINT  BIGINT            SAMPLE.SYSIBMADM.MON_CONNECTION_SUMMARY
//	RS> 9    AVG_RQST_CPU_TIME                      java.sql.Types.BIGINT  BIGINT            SAMPLE.SYSIBMADM.MON_CONNECTION_SUMMARY
//	RS> 10   ROUTINE_TIME_RQST_PERCENT              java.sql.Types.DECIMAL DECIMAL(5,2)      SAMPLE.SYSIBMADM.MON_CONNECTION_SUMMARY
//	RS> 11   RQST_WAIT_TIME_PERCENT                 java.sql.Types.DECIMAL DECIMAL(5,2)      SAMPLE.SYSIBMADM.MON_CONNECTION_SUMMARY
//	RS> 12   ACT_WAIT_TIME_PERCENT                  java.sql.Types.DECIMAL DECIMAL(5,2)      SAMPLE.SYSIBMADM.MON_CONNECTION_SUMMARY
//	RS> 13   IO_WAIT_TIME_PERCENT                   java.sql.Types.DECIMAL DECIMAL(5,2)      SAMPLE.SYSIBMADM.MON_CONNECTION_SUMMARY
//	RS> 14   LOCK_WAIT_TIME_PERCENT                 java.sql.Types.DECIMAL DECIMAL(5,2)      SAMPLE.SYSIBMADM.MON_CONNECTION_SUMMARY
//	RS> 15   AGENT_WAIT_TIME_PERCENT                java.sql.Types.DECIMAL DECIMAL(5,2)      SAMPLE.SYSIBMADM.MON_CONNECTION_SUMMARY
//	RS> 16   NETWORK_WAIT_TIME_PERCENT              java.sql.Types.DECIMAL DECIMAL(5,2)      SAMPLE.SYSIBMADM.MON_CONNECTION_SUMMARY
//	RS> 17   SECTION_PROC_TIME_PERCENT              java.sql.Types.DECIMAL DECIMAL(5,2)      SAMPLE.SYSIBMADM.MON_CONNECTION_SUMMARY
//	RS> 18   SECTION_SORT_PROC_TIME_PERCENT         java.sql.Types.DECIMAL DECIMAL(5,2)      SAMPLE.SYSIBMADM.MON_CONNECTION_SUMMARY
//	RS> 19   COMPILE_PROC_TIME_PERCENT              java.sql.Types.DECIMAL DECIMAL(5,2)      SAMPLE.SYSIBMADM.MON_CONNECTION_SUMMARY
//	RS> 20   TRANSACT_END_PROC_TIME_PERCENT         java.sql.Types.DECIMAL DECIMAL(5,2)      SAMPLE.SYSIBMADM.MON_CONNECTION_SUMMARY
//	RS> 21   UTILS_PROC_TIME_PERCENT                java.sql.Types.DECIMAL DECIMAL(5,2)      SAMPLE.SYSIBMADM.MON_CONNECTION_SUMMARY
//	RS> 22   AVG_LOCK_WAITS_PER_ACT                 java.sql.Types.BIGINT  BIGINT            SAMPLE.SYSIBMADM.MON_CONNECTION_SUMMARY
//	RS> 23   AVG_LOCK_TIMEOUTS_PER_ACT              java.sql.Types.BIGINT  BIGINT            SAMPLE.SYSIBMADM.MON_CONNECTION_SUMMARY
//	RS> 24   AVG_DEADLOCKS_PER_ACT                  java.sql.Types.BIGINT  BIGINT            SAMPLE.SYSIBMADM.MON_CONNECTION_SUMMARY
//	RS> 25   AVG_LOCK_ESCALS_PER_ACT                java.sql.Types.BIGINT  BIGINT            SAMPLE.SYSIBMADM.MON_CONNECTION_SUMMARY
//	RS> 26   ROWS_READ_PER_ROWS_RETURNED            java.sql.Types.BIGINT  BIGINT            SAMPLE.SYSIBMADM.MON_CONNECTION_SUMMARY
//	RS> 27   TOTAL_BP_HIT_RATIO_PERCENT             java.sql.Types.DECIMAL DECIMAL(5,2)      SAMPLE.SYSIBMADM.MON_CONNECTION_SUMMARY
//	RS> 28   TOTAL_GBP_HIT_RATIO_PERCENT            java.sql.Types.DECIMAL DECIMAL(5,2)      SAMPLE.SYSIBMADM.MON_CONNECTION_SUMMARY
//	RS> 29   TOTAL_CACHING_TIER_HIT_RATIO_PERCENT   java.sql.Types.DECIMAL DECIMAL(5,2)      SAMPLE.SYSIBMADM.MON_CONNECTION_SUMMARY
//	RS> 30   CF_WAIT_TIME_PERCENT                   java.sql.Types.DECIMAL DECIMAL(5,2)      SAMPLE.SYSIBMADM.MON_CONNECTION_SUMMARY
//	RS> 31   RECLAIM_WAIT_TIME_PERCENT              java.sql.Types.DECIMAL DECIMAL(5,2)      SAMPLE.SYSIBMADM.MON_CONNECTION_SUMMARY
//	RS> 32   SPACEMAPPAGE_RECLAIM_WAIT_TIME_PERCENT java.sql.Types.DECIMAL DECIMAL(5,2)      SAMPLE.SYSIBMADM.MON_CONNECTION_SUMMARY
	
	public static final boolean  NEGATIVE_DIFF_COUNTERS_TO_ZERO = true;
	public static final boolean  IS_SYSTEM_CM                   = true;
	public static final int      DEFAULT_POSTPONE_TIME          = 0;
	public static final int      DEFAULT_QUERY_TIMEOUT          = 60; //CountersModel.DEFAULT_sqlQueryTimeout;

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

		return new CmConnectionSummary(counterController, guiController);
	}

	public CmConnectionSummary(ICounterController counterController, IGuiController guiController)
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
//		setBackgroundDataPollingEnabled(true, false);
		
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
//		return new CmConnectionSummaryPanel(this);
//	}

	@Override
	public String[] getDependsOnConfigForVersion(DbxConnection conn, DbmsVersionInfo versionInfo)
	{
		return NEED_CONFIG;
	}

	@Override
	public void addMonTableDictForVersion(DbxConnection conn, DbmsVersionInfo versionInfo)
	{
		try 
		{
			MonTablesDictionary mtd = MonTablesDictionaryManager.getInstance();
			mtd.addTable("MON_CONNECTION_SUMMARY",  "fixme");

			mtd.addColumn("MON_CONNECTION_SUMMARY", "APPLICATION_HANDLE"                    , "<html>application_handle - Application handle</html>");
			mtd.addColumn("MON_CONNECTION_SUMMARY", "APPLICATION_NAME"                      , "<html>appl_name - Application name/html>");
			mtd.addColumn("MON_CONNECTION_SUMMARY", "APPLICATION_ID"                        , "<html>appl_id - Application ID</html>");
			mtd.addColumn("MON_CONNECTION_SUMMARY", "SESSION_AUTH_ID"                       , "<html>session_auth_id - Session authorization ID</html>");
			mtd.addColumn("MON_CONNECTION_SUMMARY", "TOTAL_APP_COMMITS"                     , "<html>total_app_commits - Total application commits monitor elements</html>");
			mtd.addColumn("MON_CONNECTION_SUMMARY", "TOTAL_APP_ROLLBACKS"                   , "<html>total_app_rollbacks - Total application rollbacks monitor element</html>");
			mtd.addColumn("MON_CONNECTION_SUMMARY", "ACT_COMPLETED_TOTAL"                   , "<html>act_completed_total - Total completed activities monitor element</html>");
			mtd.addColumn("MON_CONNECTION_SUMMARY", "APP_RQSTS_COMPLETED_TOTAL"             , "<html>Total number of external (application) requests that completed successfully across all members of the database for the specified service subclass</html>");
			mtd.addColumn("MON_CONNECTION_SUMMARY", "AVG_RQST_CPU_TIME"                     , "<html>Average amount of CPU time, in microseconds, used by all external requests that completed successfully. It represents the total of both user and system CPU time. Formula to calculate ratio: TOTAL_CPU_TIME / APP_RQSTS_COMPLETED_TOTAL</html>");
			mtd.addColumn("MON_CONNECTION_SUMMARY", "ROUTINE_TIME_RQST_PERCENT"             , "<html>The percentage of time the database server spent working on requests that was spent executing user routines. Formula to calculate ratio: TOTAL_ROUTINE_TIME / TOTAL_RQST_TIME</html>");
			mtd.addColumn("MON_CONNECTION_SUMMARY", "RQST_WAIT_TIME_PERCENT"                , "<html>The percentage of the time spent working on requests that was spent waiting within the DB2� database server. Formula to calculate ratio: TOTAL_WAIT_TIME / TOTAL_RQST_TIME</html>");
			mtd.addColumn("MON_CONNECTION_SUMMARY", "ACT_WAIT_TIME_PERCENT"                 , "<html>The percentage of the time spent executing activities that was spent waiting within the DB2 database server. Formula to calculate ratio: TOTAL_ACT_WAIT_TIME / TOTAL_ACT_TIME</html>");
			mtd.addColumn("MON_CONNECTION_SUMMARY", "IO_WAIT_TIME_PERCENT"                  , "<html>The percentage of the time spent waiting within the DB2 database server that was due to I/O operations. This includes time spent performing direct reads or direct writes, and time spent reading data and index pages from the table space to the bufferpool or writing them back to disk. Formula to calculate ratio: (POOL_READ_TIME + POOL_WRITE_TIME + DIRECT_READ_TIME + DIRECT_WRITE_TIME) / TOTAL_WAIT_TIME</html>");
			mtd.addColumn("MON_CONNECTION_SUMMARY", "LOCK_WAIT_TIME_PERCENT"                , "<html>The percentage of time spent waiting within the DB2 database server that was spent waiting on locks. Formula to calculate ratio: LOCK_WAIT_TIME / TOTAL_WAIT_TIME</html>");
			mtd.addColumn("MON_CONNECTION_SUMMARY", "AGENT_WAIT_TIME_PERCENT"               , "<html>The percentage of time spent waiting within the DB2 database server that was spent by an application queued to wait for an agent under concentrator configurations. Formula to calculate ratio: AGENT_WAIT_TIME / TOTAL_WAIT_TIME</html>");
			mtd.addColumn("MON_CONNECTION_SUMMARY", "NETWORK_WAIT_TIME_PERCENT"             , "<html>The percentage of time spent waiting within the DB2 database server that was spent on client-server communications. This includes time spent sending and receiving data over TCP/IP or using the IPC protocol. Formula to calculate ratio: (TCPIP_SEND_WAIT_TIME + TCPIP_RECV_WAIT_TIME + IPC_SEND_WAIT_TIME + IPC_RECV_WAIT_TIME) / TOTAL_WAIT_TIME</html>");
			mtd.addColumn("MON_CONNECTION_SUMMARY", "SECTION_PROC_TIME_PERCENT"             , "<html>The percentage of time the database server spent actively working on requests that was spent executing sections. This includes the time spent performing sorts. Formula to calculate ratio: TOTAL_SECTION_PROC_TIME / (TOTAL_RQST_TIME - TOTAL_WAIT_TIME)</html>");
			mtd.addColumn("MON_CONNECTION_SUMMARY", "SECTION_SORT_PROC_TIME_PERCENT"        , "<html>The percentage of time the database server spent actively working on requests that was spent performing sorts while executing sections. Formula to calculate ratio: TOTAL_SECTION_SORT_PROC_TIME / (TOTAL_RQST_TIME - TOTAL_WAIT_TIME)</html>");
			mtd.addColumn("MON_CONNECTION_SUMMARY", "COMPILE_PROC_TIME_PERCENT"             , "<html>The percentage of time the database server spent actively working on requests that was spent compiling an SQL statement. This includes explicit and implicit compile times. Formula to calculate ratio: (TOTAL_COMPILE_PROC_TIME + TOTAL_IMPLICIT_COMPILE_PROC_TIME) / (TOTAL_RQST_TIME - TOTAL_WAIT_TIME)</html>");
			mtd.addColumn("MON_CONNECTION_SUMMARY", "TRANSACT_END_PROC_TIME_PERCENT"        , "<html>The percentage of time the database server spent actively working on requests that was spent performing commit processing or rolling back transactions. Formula to calculate ratio: (TOTAL_COMMIT_PROC_TIME + TOTAL_ROLLBACK_PROC_TIME) / (TOTAL_RQST_TIME - TOTAL_WAIT_TIME)</html>");
			mtd.addColumn("MON_CONNECTION_SUMMARY", "UTILS_PROC_TIME_PERCENT"               , "<html>The percentage of time the database server spent actively working on requests that was spent running utilities. This includes performing runstats, reorganization, and load operations. Formula to calculate ratio: (TOTAL_RUNSTATS_PROC_TIME + TOTAL_REORG_PROC_TIME + TOTAL_LOAD_PROC_TIME) / (TOTAL_RQST_TIME - TOTAL_WAIT_TIME)</html>");
			mtd.addColumn("MON_CONNECTION_SUMMARY", "AVG_LOCK_WAITS_PER_ACT"                , "<html>The average number of times that applications or connections waited for locks per coordinator activities (successful and aborted). Formula to calculate ratio: LOCK_WAITS / (ACT_COMPLETED_TOTAL + ACT_ABORTED_TOTAL)</html>");
			mtd.addColumn("MON_CONNECTION_SUMMARY", "AVG_LOCK_TIMEOUTS_PER_ACT"             , "<html>The average number of times that a request to lock an object timed out per coordinator activities (successful and aborted). Formula to calculate ratio: LOCK_TIMEOUTS / (ACT_COMPLETED_TOTAL + ACT_ABORTED_TOTAL)</html>");
			mtd.addColumn("MON_CONNECTION_SUMMARY", "AVG_DEADLOCKS_PER_ACT"                 , "<html>The average number of deadlocks per coordinator activities (successful and aborted). Formula to calculate ratio: DEADLOCKS / (ACT_COMPLETED_TOTAL + ACT_ABORTED_TOTAL)</html>");
			mtd.addColumn("MON_CONNECTION_SUMMARY", "AVG_LOCK_ESCALS_PER_ACT"               , "<html>The average number of times that locks have been escalated from several row locks to a table lock per coordinator activities (successful and aborted). Formula to calculate ratio: LOCK_ESCALS / (ACT_COMPLETED_TOTAL + ACT_ABORTED_TOTAL)</html>");
			mtd.addColumn("MON_CONNECTION_SUMMARY", "ROWS_READ_PER_ROWS_RETURNED"           , "<html>The average number of rows read from the table per rows returned to the application. Formula to calculate ratio: ROWS_READ / ROWS_RETURNED</html>");
			mtd.addColumn("MON_CONNECTION_SUMMARY", "TOTAL_BP_HIT_RATIO_PERCENT"            , "<html>The percentage of time that the database manager did not need to load a page from disk to service a data or index page request, including requests for XML storage objects (XDAs). Formula to calculate ratio: 1 - ((POOL_DATA_P_READS + POOL_TEMP_DATA_P_READS + POOL_INDEX_P_READS + POOL_TEMP_INDEX_P_READS + POOL_XDA_P_READS + POOL_TEMP_XDA_P_READS) / (POOL_DATA_L_READS + POOL_TEMP_DATA_L_READS + POOL_INDEX_L_READS + POOL_TEMP_INDEX_L_READS + POOL_XDA_L_READS + POOL_TEMP_XDA_L_READS))</html>");
			mtd.addColumn("MON_CONNECTION_SUMMARY", "TOTAL_GBP_HIT_RATIO_PERCENT"           , "<html></html>");
			mtd.addColumn("MON_CONNECTION_SUMMARY", "TOTAL_CACHING_TIER_HIT_RATIO_PERCENT"  , "<html></html>");
			mtd.addColumn("MON_CONNECTION_SUMMARY", "CF_WAIT_TIME_PERCENT"                  , "<html></html>");
			mtd.addColumn("MON_CONNECTION_SUMMARY", "RECLAIM_WAIT_TIME_PERCENT"             , "<html></html>");
			mtd.addColumn("MON_CONNECTION_SUMMARY", "SPACEMAPPAGE_RECLAIM_WAIT_TIME_PERCENT", "<html></html>");
		}
		catch (NameNotFoundException e) 
		{
		//	_logger.warn("Problems in cm='" + CM_NAME + "', adding addMonTableDictForVersion. Caught: " + e); 
			System.out.println("Problems in cm='" + CM_NAME + "', adding addMonTableDictForVersion. Caught: " + e); 
		}
	}

	@Override
	public List<String> getPkForVersion(DbxConnection conn, DbmsVersionInfo versionInfo)
	{
		List <String> pkCols = new LinkedList<String>();

		pkCols.add("APPLICATION_HANDLE");

		return pkCols;
	}

	@Override
	public String getSqlForVersion(DbxConnection conn, DbmsVersionInfo versionInfo)
	{
		String sql = "select * from SYSIBMADM.MON_CONNECTION_SUMMARY";

		return sql;
	}
}
