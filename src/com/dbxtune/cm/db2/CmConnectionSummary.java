/*******************************************************************************
 * Copyright (C) 2010-2027 Goran Schwarz
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

import java.lang.invoke.MethodHandles;
import java.util.LinkedList;
import java.util.List;

import javax.naming.NameNotFoundException;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

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
 * Where each connection spends its time, as ratios (the same as Db2's own view SYSIBMADM.MON_CONNECTION_SUMMARY).
 * <p>
 * It is computed from MON_GET_CONNECTION (with the formulas of the view), so the login only needs EXECUTE on that function
 * (the SYSIBMADM view needs its own SELECT grant).
 *
 * @author Goran Schwarz (goran_schwarz@hotmail.com)
 */
public class CmConnectionSummary
extends CountersModel
{
	private static final Logger _logger = LogManager.getLogger(MethodHandles.lookup().lookupClass());
	private static final long    serialVersionUID = 1L;

	public static final String   CM_NAME          = CmConnectionSummary.class.getSimpleName();
	public static final String   SHORT_NAME       = "Connection Summary";
	public static final String   HTML_DESC        =
		"<html>" +
		"<h4>Connection Summary</h4>" +
		"Where each connection spends its time, as percent and averages <b>since it connected</b>: " +
		"waiting (I/O, locks, network) or working (sections, sorts, compiles, commits, utilities), CPU per request, buffer pool hit ratio, rows read per row returned.<br>" +
		"<br>" +
		"It is the same as Db2's view SYSIBMADM.MON_CONNECTION_SUMMARY (computed from MON_GET_CONNECTION).<br>" +
		"The counters (commits, rollbacks, activities, requests) are also shown as Diff and Rate.<br>" +
		"</html>";

	public static final String   GROUP_NAME       = MainFrame.TCP_GROUP_SERVER;
	public static final String   GUI_ICON_FILE    = "images/" + CM_NAME + ".png";

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
			guiController.splashWindowProgress("Loading: Counter Model '" + CM_NAME + "'");

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

		setCounterController(counterController);
		setGuiController(guiController);

		CounterSetTemplates.register(this);
	}


	//------------------------------------------------------------
	// Implementation
	//------------------------------------------------------------
	@Override
	public String[] getDependsOnConfigForVersion(DbxConnection conn, DbmsVersionInfo versionInfo)
	{
		return NEED_CONFIG;
	}

	@Override
	public boolean checkDependsOnOther(DbxConnection conn)
	{
		return Db2CmUtils.checkFunctionsAreExecutable(this, conn, "MON_GET_CONNECTION(NULL, -2)");
	}

	@Override
	public void addMonTableDictForVersion(DbxConnection conn, DbmsVersionInfo versionInfo)
	{
		try
		{
			MonTablesDictionary mtd = MonTablesDictionaryManager.getInstance();
			mtd.addTable("MON_CONNECTION_SUMMARY",  "Where each connection spends its time, as ratios since it connected (the formulas of the view SYSIBMADM.MON_CONNECTION_SUMMARY, computed from MON_GET_CONNECTION).");

			mtd.addColumn("MON_CONNECTION_SUMMARY", "APPLICATION_HANDLE"                    , "<html>Application handle (the connection).</html>");
			mtd.addColumn("MON_CONNECTION_SUMMARY", "APPLICATION_NAME"                      , "<html>Application name.</html>");
			mtd.addColumn("MON_CONNECTION_SUMMARY", "APPLICATION_ID"                        , "<html>Application ID.</html>");
			mtd.addColumn("MON_CONNECTION_SUMMARY", "SESSION_AUTH_ID"                       , "<html>Session authorization ID (the user).</html>");
			mtd.addColumn("MON_CONNECTION_SUMMARY", "CLIENT_HOSTNAME"                       , "<html>The host the client connects from.</html>");
			mtd.addColumn("MON_CONNECTION_SUMMARY", "TOTAL_APP_COMMITS"                     , "<html>Commits issued by the application.</html>");
			mtd.addColumn("MON_CONNECTION_SUMMARY", "TOTAL_APP_ROLLBACKS"                   , "<html>Rollbacks issued by the application.</html>");
			mtd.addColumn("MON_CONNECTION_SUMMARY", "ACT_COMPLETED_TOTAL"                   , "<html>Activities (statements) that completed successfully.</html>");
			mtd.addColumn("MON_CONNECTION_SUMMARY", "APP_RQSTS_COMPLETED_TOTAL"             , "<html>Requests from the application that completed (one request can run many statements).</html>");
			mtd.addColumn("MON_CONNECTION_SUMMARY", "AVG_RQST_CPU_TIME"                     , "<html>Average CPU time per completed request, in microseconds.<br><b>Formula</b>: TOTAL_CPU_TIME / APP_RQSTS_COMPLETED_TOTAL</html>");
			mtd.addColumn("MON_CONNECTION_SUMMARY", "ROUTINE_TIME_RQST_PERCENT"             , "<html>Percent of the request time that was spent in routines (procedures, functions).<br><b>Formula</b>: TOTAL_ROUTINE_TIME / TOTAL_RQST_TIME</html>");
			mtd.addColumn("MON_CONNECTION_SUMMARY", "RQST_WAIT_TIME_PERCENT"                , "<html>Percent of the request time that was spent waiting in Db2.<br><b>Formula</b>: TOTAL_WAIT_TIME / TOTAL_RQST_TIME</html>");
			mtd.addColumn("MON_CONNECTION_SUMMARY", "ACT_WAIT_TIME_PERCENT"                 , "<html>Percent of the activity (statement) time that was spent waiting.<br><b>Formula</b>: TOTAL_ACT_WAIT_TIME / TOTAL_ACT_TIME</html>");
			mtd.addColumn("MON_CONNECTION_SUMMARY", "IO_WAIT_TIME_PERCENT"                  , "<html>Percent of the wait time that was I/O (buffer pool reads/writes and direct I/O).<br><b>Formula</b>: (POOL_READ_TIME + POOL_WRITE_TIME + DIRECT_READ_TIME + DIRECT_WRITE_TIME) / TOTAL_WAIT_TIME</html>");
			mtd.addColumn("MON_CONNECTION_SUMMARY", "LOCK_WAIT_TIME_PERCENT"                , "<html>Percent of the wait time that was waiting for locks.<br><b>Formula</b>: LOCK_WAIT_TIME / TOTAL_WAIT_TIME</html>");
			mtd.addColumn("MON_CONNECTION_SUMMARY", "AGENT_WAIT_TIME_PERCENT"               , "<html>Percent of the wait time that was waiting for an agent (connection concentrator).<br><b>Formula</b>: AGENT_WAIT_TIME / TOTAL_WAIT_TIME</html>");
			mtd.addColumn("MON_CONNECTION_SUMMARY", "NETWORK_WAIT_TIME_PERCENT"             , "<html>Percent of the wait time that was network (TCP/IP and IPC send/receive).<br><b>Formula</b>: (TCPIP_SEND_WAIT_TIME + TCPIP_RECV_WAIT_TIME + IPC_SEND_WAIT_TIME + IPC_RECV_WAIT_TIME) / TOTAL_WAIT_TIME</html>");
			mtd.addColumn("MON_CONNECTION_SUMMARY", "SECTION_PROC_TIME_PERCENT"             , "<html>Percent of the processing time (request time - wait time) that was spent executing sections (statements), including sorts.<br><b>Formula</b>: TOTAL_SECTION_PROC_TIME / (TOTAL_RQST_TIME - TOTAL_WAIT_TIME)</html>");
			mtd.addColumn("MON_CONNECTION_SUMMARY", "SECTION_SORT_PROC_TIME_PERCENT"        , "<html>Percent of the processing time that was spent sorting.<br><b>Formula</b>: TOTAL_SECTION_SORT_PROC_TIME / (TOTAL_RQST_TIME - TOTAL_WAIT_TIME)</html>");
			mtd.addColumn("MON_CONNECTION_SUMMARY", "COMPILE_PROC_TIME_PERCENT"             , "<html>Percent of the processing time that was spent compiling SQL (explicit and implicit). High: statements are not reused (literals instead of parameter markers?).<br><b>Formula</b>: (TOTAL_COMPILE_PROC_TIME + TOTAL_IMPLICIT_COMPILE_PROC_TIME) / (TOTAL_RQST_TIME - TOTAL_WAIT_TIME)</html>");
			mtd.addColumn("MON_CONNECTION_SUMMARY", "TRANSACT_END_PROC_TIME_PERCENT"        , "<html>Percent of the processing time that was spent in commits and rollbacks.<br><b>Formula</b>: (TOTAL_COMMIT_PROC_TIME + TOTAL_ROLLBACK_PROC_TIME) / (TOTAL_RQST_TIME - TOTAL_WAIT_TIME)</html>");
			mtd.addColumn("MON_CONNECTION_SUMMARY", "UTILS_PROC_TIME_PERCENT"               , "<html>Percent of the processing time that was spent in utilities (RUNSTATS, REORG, LOAD).<br><b>Formula</b>: (TOTAL_RUNSTATS_PROC_TIME + TOTAL_REORG_PROC_TIME + TOTAL_LOAD_PROC_TIME) / (TOTAL_RQST_TIME - TOTAL_WAIT_TIME)</html>");
			mtd.addColumn("MON_CONNECTION_SUMMARY", "AVG_LOCK_WAITS_PER_ACT"                , "<html>Lock waits per activity (statement).<br><b>Formula</b>: LOCK_WAITS / (ACT_COMPLETED_TOTAL + ACT_ABORTED_TOTAL)</html>");
			mtd.addColumn("MON_CONNECTION_SUMMARY", "AVG_LOCK_TIMEOUTS_PER_ACT"             , "<html>Lock timeouts per activity.<br><b>Formula</b>: LOCK_TIMEOUTS / (ACT_COMPLETED_TOTAL + ACT_ABORTED_TOTAL)</html>");
			mtd.addColumn("MON_CONNECTION_SUMMARY", "AVG_DEADLOCKS_PER_ACT"                 , "<html>Deadlocks per activity.<br><b>Formula</b>: DEADLOCKS / (ACT_COMPLETED_TOTAL + ACT_ABORTED_TOTAL)</html>");
			mtd.addColumn("MON_CONNECTION_SUMMARY", "AVG_LOCK_ESCALS_PER_ACT"               , "<html>Lock escalations per activity.<br><b>Formula</b>: LOCK_ESCALS / (ACT_COMPLETED_TOTAL + ACT_ABORTED_TOTAL)</html>");
			mtd.addColumn("MON_CONNECTION_SUMMARY", "ROWS_READ_PER_ROWS_RETURNED"           , "<html>Rows read for each row returned. A high value means table scans, or indexes that do not match the queries.<br><b>Formula</b>: ROWS_READ / ROWS_RETURNED</html>");
			mtd.addColumn("MON_CONNECTION_SUMMARY", "TOTAL_BP_HIT_RATIO_PERCENT"            , "<html>Buffer pool hit ratio: logical reads that did not need a disk read (data, index, xda and column organized, also temporary).<br><b>Formula</b>: (POOL_*_LBP_PAGES_FOUND) / (POOL_*_L_READS)</html>");
		}
		catch (NameNotFoundException e)
		{
			_logger.warn("Problems in cm='" + CM_NAME + "', adding addMonTableDictForVersion. Caught: " + e);
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
		// The formulas of SYSIBMADM.MON_CONNECTION_SUMMARY (Db2 11.5.8), without the pureScale-only columns (GBP, caching tier, CF, reclaim)
		String sql = ""
			+ "with M as ( \n"
			+ "    select /* ${cmCollectorName} */ \n"
			+ "         APPLICATION_HANDLE \n"
			+ "        ,max(APPLICATION_NAME) as APPLICATION_NAME, max(APPLICATION_ID) as APPLICATION_ID, max(SESSION_AUTH_ID) as SESSION_AUTH_ID, max(CLIENT_HOSTNAME) as CLIENT_HOSTNAME \n"
			+ "        ,sum(TOTAL_APP_COMMITS) as TOTAL_APP_COMMITS, sum(TOTAL_APP_ROLLBACKS) as TOTAL_APP_ROLLBACKS \n"
			+ "        ,sum(ACT_COMPLETED_TOTAL) as ACT_COMPLETED_TOTAL, sum(ACT_COMPLETED_TOTAL + ACT_ABORTED_TOTAL) as ACT_TOTAL \n"
			+ "        ,sum(APP_RQSTS_COMPLETED_TOTAL) as APP_RQSTS_COMPLETED_TOTAL, sum(TOTAL_CPU_TIME) as TOTAL_CPU_TIME \n"
			+ "        ,sum(ROWS_READ) as ROWS_READ, sum(ROWS_RETURNED) as ROWS_RETURNED \n"
			+ "        ,sum(TOTAL_WAIT_TIME) as TOTAL_WAIT_TIME, sum(TOTAL_RQST_TIME) as TOTAL_RQST_TIME \n"
			+ "        ,sum(TOTAL_ACT_WAIT_TIME) as TOTAL_ACT_WAIT_TIME, sum(TOTAL_ACT_TIME) as TOTAL_ACT_TIME \n"
			+ "        ,sum(POOL_READ_TIME + POOL_WRITE_TIME + DIRECT_READ_TIME + DIRECT_WRITE_TIME) as IO_WAIT_TIME \n"
			+ "        ,sum(LOCK_WAIT_TIME) as LOCK_WAIT_TIME, sum(AGENT_WAIT_TIME) as AGENT_WAIT_TIME \n"
			+ "        ,sum(LOCK_WAITS) as LOCK_WAITS, sum(LOCK_TIMEOUTS) as LOCK_TIMEOUTS, sum(DEADLOCKS) as DEADLOCKS, sum(LOCK_ESCALS) as LOCK_ESCALS \n"
			+ "        ,sum(TCPIP_SEND_WAIT_TIME + TCPIP_RECV_WAIT_TIME + IPC_SEND_WAIT_TIME + IPC_RECV_WAIT_TIME) as NETWORK_WAIT_TIME \n"
			+ "        ,sum(TOTAL_RQST_TIME - TOTAL_WAIT_TIME) as PROC_TIME \n"
			+ "        ,sum(TOTAL_SECTION_PROC_TIME) as SECTION_PROC_TIME, sum(TOTAL_SECTION_SORT_PROC_TIME) as SECTION_SORT_PROC_TIME \n"
			+ "        ,sum(TOTAL_COMPILE_PROC_TIME + TOTAL_IMPLICIT_COMPILE_PROC_TIME) as COMPILE_PROC_TIME \n"
			+ "        ,sum(TOTAL_COMMIT_PROC_TIME + TOTAL_ROLLBACK_PROC_TIME) as TRANSACT_END_PROC_TIME \n"
			+ "        ,sum(TOTAL_RUNSTATS_PROC_TIME + TOTAL_REORG_PROC_TIME + TOTAL_LOAD_PROC_TIME) as UTILS_PROC_TIME \n"
			+ "        ,sum(POOL_DATA_L_READS + POOL_TEMP_DATA_L_READS + POOL_INDEX_L_READS + POOL_TEMP_INDEX_L_READS + POOL_XDA_L_READS + POOL_TEMP_XDA_L_READS + POOL_COL_L_READS + POOL_TEMP_COL_L_READS) as L_READS \n"
			+ "        ,sum(POOL_DATA_LBP_PAGES_FOUND + POOL_INDEX_LBP_PAGES_FOUND + POOL_XDA_LBP_PAGES_FOUND + POOL_COL_LBP_PAGES_FOUND) as BP_PAGES_FOUND \n"
			+ "        ,sum(TOTAL_ROUTINE_TIME) as TOTAL_ROUTINE_TIME \n"
			+ "    from table(MON_GET_CONNECTION(NULL, -2)) x \n"
			+ "    group by APPLICATION_HANDLE \n"
			+ ") \n"
			+ "select \n"
			+ "     APPLICATION_HANDLE, APPLICATION_NAME, APPLICATION_ID, SESSION_AUTH_ID, CLIENT_HOSTNAME \n"
			+ "    ,TOTAL_APP_COMMITS, TOTAL_APP_ROLLBACKS, ACT_COMPLETED_TOTAL, APP_RQSTS_COMPLETED_TOTAL \n"
			+ "    ,CASE WHEN APP_RQSTS_COMPLETED_TOTAL > 0 THEN TOTAL_CPU_TIME / APP_RQSTS_COMPLETED_TOTAL END                       as AVG_RQST_CPU_TIME \n"
			+ "    ,CASE WHEN TOTAL_RQST_TIME > 0 THEN DEC(FLOAT(TOTAL_ROUTINE_TIME)     / FLOAT(TOTAL_RQST_TIME) * 100, 5, 2) END       as ROUTINE_TIME_RQST_PERCENT \n"
			+ "    ,CASE WHEN TOTAL_RQST_TIME > 0 THEN DEC(FLOAT(TOTAL_WAIT_TIME)        / FLOAT(TOTAL_RQST_TIME) * 100, 5, 2) END       as RQST_WAIT_TIME_PERCENT \n"
			+ "    ,CASE WHEN TOTAL_ACT_TIME  > 0 THEN DEC(FLOAT(TOTAL_ACT_WAIT_TIME)    / FLOAT(TOTAL_ACT_TIME)  * 100, 5, 2) END       as ACT_WAIT_TIME_PERCENT \n"
			+ "    ,CASE WHEN TOTAL_WAIT_TIME > 0 THEN DEC(FLOAT(IO_WAIT_TIME)           / FLOAT(TOTAL_WAIT_TIME) * 100, 5, 2) END       as IO_WAIT_TIME_PERCENT \n"
			+ "    ,CASE WHEN TOTAL_WAIT_TIME > 0 THEN DEC(FLOAT(LOCK_WAIT_TIME)         / FLOAT(TOTAL_WAIT_TIME) * 100, 5, 2) END       as LOCK_WAIT_TIME_PERCENT \n"
			+ "    ,CASE WHEN TOTAL_WAIT_TIME > 0 THEN DEC(FLOAT(AGENT_WAIT_TIME)        / FLOAT(TOTAL_WAIT_TIME) * 100, 5, 2) END       as AGENT_WAIT_TIME_PERCENT \n"
			+ "    ,CASE WHEN TOTAL_WAIT_TIME > 0 THEN DEC(FLOAT(NETWORK_WAIT_TIME)      / FLOAT(TOTAL_WAIT_TIME) * 100, 5, 2) END       as NETWORK_WAIT_TIME_PERCENT \n"
			+ "    ,CASE WHEN PROC_TIME       > 0 THEN DEC(FLOAT(SECTION_PROC_TIME)      / FLOAT(PROC_TIME)       * 100, 5, 2) END       as SECTION_PROC_TIME_PERCENT \n"
			+ "    ,CASE WHEN PROC_TIME       > 0 THEN DEC(FLOAT(SECTION_SORT_PROC_TIME) / FLOAT(PROC_TIME)       * 100, 5, 2) END       as SECTION_SORT_PROC_TIME_PERCENT \n"
			+ "    ,CASE WHEN PROC_TIME       > 0 THEN DEC(FLOAT(COMPILE_PROC_TIME)      / FLOAT(PROC_TIME)       * 100, 5, 2) END       as COMPILE_PROC_TIME_PERCENT \n"
			+ "    ,CASE WHEN PROC_TIME       > 0 THEN DEC(FLOAT(TRANSACT_END_PROC_TIME) / FLOAT(PROC_TIME)       * 100, 5, 2) END       as TRANSACT_END_PROC_TIME_PERCENT \n"
			+ "    ,CASE WHEN PROC_TIME       > 0 THEN DEC(FLOAT(UTILS_PROC_TIME)        / FLOAT(PROC_TIME)       * 100, 5, 2) END       as UTILS_PROC_TIME_PERCENT \n"
			+ "    ,CASE WHEN ACT_TOTAL       > 0 THEN LOCK_WAITS    / ACT_TOTAL END                                                       as AVG_LOCK_WAITS_PER_ACT \n"
			+ "    ,CASE WHEN ACT_TOTAL       > 0 THEN LOCK_TIMEOUTS / ACT_TOTAL END                                                       as AVG_LOCK_TIMEOUTS_PER_ACT \n"
			+ "    ,CASE WHEN ACT_TOTAL       > 0 THEN DEADLOCKS     / ACT_TOTAL END                                                       as AVG_DEADLOCKS_PER_ACT \n"
			+ "    ,CASE WHEN ACT_TOTAL       > 0 THEN LOCK_ESCALS   / ACT_TOTAL END                                                       as AVG_LOCK_ESCALS_PER_ACT \n"
			+ "    ,CASE WHEN ROWS_RETURNED   > 0 THEN ROWS_READ     / ROWS_RETURNED END                                                   as ROWS_READ_PER_ROWS_RETURNED \n"
			+ "    ,CASE WHEN L_READS         > 0 THEN DEC(FLOAT(BP_PAGES_FOUND)         / FLOAT(L_READS)         * 100, 5, 2) END       as TOTAL_BP_HIT_RATIO_PERCENT \n"
			+ "from M \n"
			+ "order by APPLICATION_HANDLE \n"
			+ "";

		return sql;
	}
}
