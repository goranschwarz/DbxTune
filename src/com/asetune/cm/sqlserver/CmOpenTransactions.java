/*******************************************************************************
 * Copyright (C) 2010-2019 Goran Schwarz
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
import com.asetune.cm.sqlserver.gui.CmOpenTransactionsPanel;
import com.asetune.gui.MainFrame;
import com.asetune.gui.TabularCntrPanel;

/**
 * @author Goran Schwarz (goran_schwarz@hotmail.com)
 */
public class CmOpenTransactions
extends CountersModel
{
//	private static Logger        _logger          = Logger.getLogger(CmServiceMemory.class);
	private static final long    serialVersionUID = 1L;

	public static final String   CM_NAME          = CmOpenTransactions.class.getSimpleName();
	public static final String   SHORT_NAME       = "Open Transactions";
	public static final String   HTML_DESC        = 
		"<html>" +
		"<p>Who has open transactions on the server</p>" +
		"<br><br>" +
		"Table Background colors:" +
		"<ul>" +
		"    <li>PINK   - SPID is Blocked by some other SPID that holds a Lock on a database object Table, Page or Row. This is the Lock Victim.</li>" +
		"    <li>RED    - SPID is Blocking other SPID's from running, this SPID is Responsible or the Root Cause of a Blocking Lock.</li>" +
		"</ul>" +
		"</html>";

//	public static final String   GROUP_NAME       = MainFrame.TCP_GROUP_SERVER;
	public static final String   GROUP_NAME       = MainFrame.TCP_GROUP_OBJECT_ACCESS;
	public static final String   GUI_ICON_FILE    = "images/"+CM_NAME+".png";

	public static final long     NEED_SRV_VERSION = 0;
	public static final long     NEED_CE_VERSION  = 0;

	public static final String[] MON_TABLES       = new String[] {"CmOpenTransactions"};
	public static final String[] NEED_ROLES       = new String[] {};//{"VIEW SERVER STATE"};
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

		return new CmOpenTransactions(counterController, guiController);
	}

	public CmOpenTransactions(ICounterController counterController, IGuiController guiController)
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

	@Override
	protected TabularCntrPanel createGui()
	{
		return new CmOpenTransactionsPanel(this);
	}

	@Override
	public String[] getDependsOnConfigForVersion(Connection conn, long srvVersion, boolean isAzure)
	{
		return NEED_CONFIG;
	}

	@Override
	public List<String> getPkForVersion(Connection conn, long srvVersion, boolean isAzure)
	{
		List <String> pkCols = new LinkedList<String>();

//		pkCols.add("session_id");

		return pkCols;
	}

	@Override
	public String getSqlForVersion(Connection conn, long srvVersion, boolean isAzure)
	{
		String dm_tran_database_transactions = "sys.dm_tran_database_transactions";
		String dm_tran_session_transactions  = "sys.dm_tran_session_transactions";
		String dm_exec_sessions              = "sys.dm_exec_sessions";
		String dm_exec_connections           = "sys.dm_exec_connections";
		String dm_exec_requests              = "sys.dm_exec_requests";
		String dm_exec_sql_text              = "sys.dm_exec_sql_text";
		String dm_exec_query_plan            = "sys.dm_exec_query_plan";
		
		if (isAzure)
		{
			dm_tran_database_transactions = "sys.dm_pdw_nodes_tran_database_transactions";
			dm_tran_session_transactions  = "sys.dm_pdw_nodes_tran_session_transactions";
			dm_exec_sessions              = "sys.dm_pdw_nodes_exec_sessions";
			dm_exec_connections           = "sys.dm_pdw_exec_connections";
			dm_exec_requests              = "sys.dm_exec_requests";                          // Same name in Azure ???
			dm_exec_sql_text              = "sys.dm_exec_sql_text";                          // Same name in Azure ???
			dm_exec_query_plan            = "sys.dm_exec_query_plan";                        // Same name in Azure ???
		}

		
		String sql = ""
				+ "-------------------------------------------------------------------------------------------------------\n"
				+ "-- Origin SQL from: https://www.sqlskills.com/blogs/paul/script-open-transactions-with-text-and-plans/ \n"
				+ "-- Just reformated the code and added a bunch of columns \n"
				+ "-------------------------------------------------------------------------------------------------------\n"
				+ "SELECT \n"
				+ "    [s_tst].[session_id], \n"
				+ "    DB_NAME (s_tdt.database_id)                       AS [Database], \n"
				+ "    [s_es].[status]                                   AS [Status], \n"
				+ "    [s_es].[login_name]                               AS [LoginName], \n"
				+ "    [s_es].[login_time]                               AS [LoginTime], \n"
				+ "    [s_es].[host_name]                                AS [HostName], \n"
				+ "    [s_es].[host_process_id]                          AS [HostPID], \n"
				+ "    [s_es].[program_name]                             AS [ProgramName], \n"
				+ "    [s_es].[last_request_start_time]                  AS [LastRequestStartTime], \n"
				+ "    [s_tdt].[database_transaction_begin_time]         AS [TranBeginTime], \n"
				+ "    [s_tdt].[database_transaction_log_record_count]   AS [TranLogRecordCount], \n"
				+ "    [s_tdt].[database_transaction_log_bytes_used]     AS [LogBytesUsed], \n"
				+ "    [s_tdt].[database_transaction_log_bytes_reserved] AS [LogBytesReserved], \n"
				+ "    [s_er].[status]                                   AS [ExecStatus], \n"
				+ "    [s_er].[command]                                  AS [ExecCommand], \n"
				+ "    [s_er].[blocking_session_id]                      AS [BlockedBySpid], \n"
				+ "    (SELECT count(*) FROM " + dm_exec_requests + " x where x.blocking_session_id = s_tst.session_id) AS [BlockingOtherSpidCount], \n"
				+ "    [s_er].[wait_type]                                AS [WaitType], \n"
				+ "    [s_er].[wait_time]                                AS [WaitTimeInMs], \n"
				+ "    [s_er].[last_wait_type]                           AS [LastWaitType], \n"
				+ "    [s_est].text                                      AS [LastSqlText], \n"
				+ "    [s_eqp].[query_plan]                              AS [LastPlan] \n"
				+ "FROM            " + dm_tran_database_transactions + " [s_tdt] \n"
				+ "JOIN            " + dm_tran_session_transactions + "  [s_tst]  ON [s_tst].[transaction_id] = [s_tdt].[transaction_id] \n"
				+ "JOIN            " + dm_exec_sessions + "              [s_es]   ON [s_es] .[session_id]     = [s_tst].[session_id] \n"
				+ "JOIN            " + dm_exec_connections + "           [s_ec]   ON [s_ec] .[session_id]     = [s_tst].[session_id] \n"
				+ "LEFT OUTER JOIN " + dm_exec_requests + "              [s_er]   ON [s_er] .[session_id]     = [s_tst].[session_id] \n"
				+ "CROSS APPLY     " + dm_exec_sql_text + " ([s_ec].[most_recent_sql_handle]) AS [s_est] \n"
				+ "OUTER APPLY     " + dm_exec_query_plan + " ([s_er].[plan_handle])          AS [s_eqp] \n"
				+ "WHERE [s_tdt].[database_transaction_begin_time] IS NOT NULL -- Time at which the database became involved in the transaction. \n"
				+ "ORDER BY [TranBeginTime] ASC                                -- Specifically, it is the time of the first log record in the database for the transaction. \n"
				+ "";

		return sql;
	}
}
