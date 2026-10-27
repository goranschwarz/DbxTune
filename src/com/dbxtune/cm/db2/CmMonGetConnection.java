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
 * Activity per connection (MON_GET_CONNECTION): who it is, if it has an open transaction, and what it has done since it connected.
 *
 * @author Goran Schwarz (goran_schwarz@hotmail.com)
 */
public class CmMonGetConnection
extends CountersModel
{
	private static final Logger _logger = LogManager.getLogger(MethodHandles.lookup().lookupClass());
	private static final long    serialVersionUID = 1L;

	public static final String   CM_NAME          = CmMonGetConnection.class.getSimpleName();
	public static final String   SHORT_NAME       = "Connection Activity";
	public static final String   HTML_DESC        =
		"<html>" +
		"<h4>Connection Activity</h4>" +
		"Activity per connection: who it is (user, client host, application), if it has an open transaction, its locks, " +
		"and what it has done since it connected (commits, statements, rows, CPU, wait time, buffer pool reads, sorts...).<br>" +
		"<br>" +
		"<b>IN_TRAN</b> = 1: the connection has an open transaction. Together with WORKLOAD_OCCURRENCE_STATE = UOWWAIT (the application is not running anything), it may be a forgotten commit.<br>" +
		"What the connections run <i>right now</i> is in the tab 'Active Statements'.<br>" +
		"</html>";

	public static final String   GROUP_NAME       = MainFrame.TCP_GROUP_SERVER;
	public static final String   GUI_ICON_FILE    = "images/" + CM_NAME + ".png";

	public static final long     NEED_SRV_VERSION = 0;
	public static final long     NEED_CE_VERSION  = 0;

	public static final String[] MON_TABLES       = new String[] {"MON_GET_CONNECTION", CM_NAME};
	public static final String[] NEED_ROLES       = new String[] {};
	public static final String[] NEED_CONFIG      = new String[] {};

	public static final String[] PCT_COLUMNS      = new String[] {};
	public static final String[] DIFF_COLUMNS     = new String[] {
		"TOTAL_APP_COMMITS",
		"TOTAL_APP_ROLLBACKS",
		"ACT_COMPLETED_TOTAL",
		"ACT_ABORTED_TOTAL",
		"APP_RQSTS_COMPLETED_TOTAL",
		"FAILED_SQL_STMTS",
		"ROWS_READ",
		"ROWS_RETURNED",
		"ROWS_MODIFIED",
		"BP_LOGICAL_READS",
		"BP_PHYSICAL_READS",
		"DIRECT_READS",
		"DIRECT_WRITES",
		"TOTAL_CPU_TIME",
		"TOTAL_RQST_TIME",
		"TOTAL_WAIT_TIME",
		"CLIENT_IDLE_WAIT_TIME",
		"LOCK_WAITS",
		"LOCK_WAIT_TIME",
		"LOCK_TIMEOUTS",
		"DEADLOCKS",
		"LOCK_ESCALS",
		"POOL_READ_TIME",
		"POOL_WRITE_TIME",
		"DIRECT_READ_TIME",
		"LOG_DISK_WAIT_TIME",
		"TCPIP_WAIT_TIME",
		"TOTAL_SORTS",
		"SORT_OVERFLOWS",
		"TOTAL_COMPILATIONS",
		"TOTAL_COMPILE_TIME",
		"PKG_CACHE_LOOKUPS",
		"PKG_CACHE_INSERTS",
		"TOTAL_ROUTINE_TIME",
		"TCPIP_SEND_VOLUME",
		"TCPIP_RECV_VOLUME"
	};

	public static final boolean  NEGATIVE_DIFF_COUNTERS_TO_ZERO = true;
	public static final boolean  IS_SYSTEM_CM                   = true;
	public static final int      DEFAULT_POSTPONE_TIME          = 0;
	public static final int      DEFAULT_QUERY_TIMEOUT          = 60; //CountersModel.DEFAULT_sqlQueryTimeout;

	@Override public int     getDefaultPostponeTime()                 { return DEFAULT_POSTPONE_TIME; }
	@Override public int     getDefaultQueryTimeout()                 { return DEFAULT_QUERY_TIMEOUT; }
	@Override public boolean getDefaultIsNegativeDiffCountersToZero() { return NEGATIVE_DIFF_COUNTERS_TO_ZERO; }
	@Override public Type    getTemplateLevel()                       { return Type.MEDIUM; }

	/**
	 * FACTORY  method to create the object
	 */
	public static CountersModel create(ICounterController counterController, IGuiController guiController)
	{
		if (guiController != null && guiController.hasGUI())
			guiController.splashWindowProgress("Loading: Counter Model '" + CM_NAME + "'");

		return new CmMonGetConnection(counterController, guiController);
	}

	public CmMonGetConnection(ICounterController counterController, IGuiController guiController)
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
			mtd.addTable(CM_NAME,  "Activity per connection (MON_GET_CONNECTION).");

			mtd.addColumn(CM_NAME, "APPLICATION_HANDLE",        "<html>Application handle: the connection.</html>");
			mtd.addColumn(CM_NAME, "APPLICATION_ID",            "<html>Application id (it includes the client IP and port).</html>");
			mtd.addColumn(CM_NAME, "MEMBER",                    "<html>The database member (0 if this is not pureScale or DPF).</html>");
			mtd.addColumn(CM_NAME, "APPLICATION_NAME",          "<html>Application name (for Java: db2jcc_application, unless the program sets clientProgramName).</html>");
			mtd.addColumn(CM_NAME, "SESSION_AUTH_ID",           "<html>The user.</html>");
			mtd.addColumn(CM_NAME, "CLIENT_HOSTNAME",           "<html>The client host.</html>");
			mtd.addColumn(CM_NAME, "CLIENT_IPADDR",             "<html>The client IP address.</html>");
			mtd.addColumn(CM_NAME, "CLIENT_PID",                "<html>The client process id.</html>");
			mtd.addColumn(CM_NAME, "CLIENT_PLATFORM",           "<html>The client platform.</html>");
			mtd.addColumn(CM_NAME, "CLIENT_PROTOCOL",           "<html>The client protocol (TCPIP, LOCAL...).</html>");
			mtd.addColumn(CM_NAME, "CLIENT_USERID",             "<html>Client user id set by the application (client information).</html>");
			mtd.addColumn(CM_NAME, "CLIENT_WRKSTNNAME",         "<html>Client workstation set by the application (client information).</html>");
			mtd.addColumn(CM_NAME, "CLIENT_APPLNAME",           "<html>Client application name set by the application (client information).</html>");
			mtd.addColumn(CM_NAME, "CONNECTION_START_TIME",     "<html>When the connection was made.</html>");
			mtd.addColumn(CM_NAME, "IS_SYSTEM_APPL",            "<html>1 = a Db2 system application (not a client).</html>");
			mtd.addColumn(CM_NAME, "WORKLOAD_OCCURRENCE_STATE", "<html>What the connection does: UOWEXEC (running a statement), UOWWAIT (waiting for the client), UOWQUEUED (queued by WLM), DECOUPLED...</html>");
			mtd.addColumn(CM_NAME, "IN_TRAN",                   "<html>1 = the connection has an open transaction (unit of work) right now.</html>");
			mtd.addColumn(CM_NAME, "TRAN_AGE_SEC",              "<html>How long the open transaction has been open, in seconds (NULL if there is none).</html>");
			mtd.addColumn(CM_NAME, "UOW_START_TIME",            "<html>When the current (or the last) unit of work started.</html>");
			mtd.addColumn(CM_NAME, "UOW_STOP_TIME",             "<html>When the last unit of work ended (NULL while it is open).</html>");
			mtd.addColumn(CM_NAME, "SQL_REQS_SINCE_COMMIT",     "<html>SQL requests since the last commit.</html>");
			mtd.addColumn(CM_NAME, "CURRENT_ISOLATION",         "<html>Isolation level: UR, CS, RS, RR.</html>");
			mtd.addColumn(CM_NAME, "LOCK_TIMEOUT_VAL",          "<html>Lock timeout of the connection, in seconds (-1 = wait forever).</html>");
			mtd.addColumn(CM_NAME, "NUM_LOCKS_HELD",            "<html>Locks held right now.</html>");
			mtd.addColumn(CM_NAME, "NUM_LOCKS_WAITING",         "<html>Agents of this connection that wait for a lock right now.</html>");
			mtd.addColumn(CM_NAME, "NUM_ASSOC_AGENTS",          "<html>Agents working for this connection (more than 1 with intra-partition parallelism).</html>");
			mtd.addColumn(CM_NAME, "TOTAL_APP_COMMITS",         "<html>Commits.</html>");
			mtd.addColumn(CM_NAME, "TOTAL_APP_ROLLBACKS",       "<html>Rollbacks.</html>");
			mtd.addColumn(CM_NAME, "ACT_COMPLETED_TOTAL",       "<html>Activities (statements) that completed.</html>");
			mtd.addColumn(CM_NAME, "ACT_ABORTED_TOTAL",         "<html>Activities (statements) that failed or were interrupted.</html>");
			mtd.addColumn(CM_NAME, "APP_RQSTS_COMPLETED_TOTAL", "<html>Requests from the application that completed.</html>");
			mtd.addColumn(CM_NAME, "FAILED_SQL_STMTS",          "<html>Statements that failed (negative SQLCODE).</html>");
			mtd.addColumn(CM_NAME, "ROWS_READ",                 "<html>Rows read.</html>");
			mtd.addColumn(CM_NAME, "ROWS_RETURNED",             "<html>Rows returned to the application.</html>");
			mtd.addColumn(CM_NAME, "ROWS_MODIFIED",             "<html>Rows inserted, updated or deleted.</html>");
			mtd.addColumn(CM_NAME, "BP_LOGICAL_READS",          "<html>Logical reads (from the buffer pools).</html>");
			mtd.addColumn(CM_NAME, "BP_PHYSICAL_READS",         "<html>Physical reads (from disk).</html>");
			mtd.addColumn(CM_NAME, "DIRECT_READS",              "<html>Direct reads (LOB, LONG data).</html>");
			mtd.addColumn(CM_NAME, "DIRECT_WRITES",             "<html>Direct writes (LOB, LONG data).</html>");
			mtd.addColumn(CM_NAME, "TOTAL_CPU_TIME",            "<html>CPU time, in <b>microseconds</b>.</html>");
			mtd.addColumn(CM_NAME, "TOTAL_RQST_TIME",           "<html>Time spent on requests, in milliseconds.</html>");
			mtd.addColumn(CM_NAME, "TOTAL_WAIT_TIME",           "<html>Time spent waiting in Db2, in milliseconds.</html>");
			mtd.addColumn(CM_NAME, "CLIENT_IDLE_WAIT_TIME",     "<html>Time Db2 waited for the client to send the next request, in milliseconds (the application's own time, not Db2's).</html>");
			mtd.addColumn(CM_NAME, "LOCK_WAITS",                "<html>Lock waits (counted when the wait ends).</html>");
			mtd.addColumn(CM_NAME, "LOCK_WAIT_TIME",            "<html>Time spent waiting for locks, in milliseconds.</html>");
			mtd.addColumn(CM_NAME, "LOCK_TIMEOUTS",             "<html>Lock timeouts.</html>");
			mtd.addColumn(CM_NAME, "DEADLOCKS",                 "<html>Deadlocks.</html>");
			mtd.addColumn(CM_NAME, "LOCK_ESCALS",               "<html>Lock escalations.</html>");
			mtd.addColumn(CM_NAME, "POOL_READ_TIME",            "<html>Time spent on physical reads, in milliseconds.</html>");
			mtd.addColumn(CM_NAME, "POOL_WRITE_TIME",           "<html>Time spent on physical writes, in milliseconds.</html>");
			mtd.addColumn(CM_NAME, "DIRECT_READ_TIME",          "<html>Time spent on direct reads, in milliseconds.</html>");
			mtd.addColumn(CM_NAME, "LOG_DISK_WAIT_TIME",        "<html>Time spent waiting for the log to be written (at commit), in milliseconds.</html>");
			mtd.addColumn(CM_NAME, "TCPIP_WAIT_TIME",           "<html>Time spent waiting to send or receive over TCP/IP, in milliseconds.</html>");
			mtd.addColumn(CM_NAME, "TOTAL_SORTS",               "<html>Sorts.</html>");
			mtd.addColumn(CM_NAME, "SORT_OVERFLOWS",            "<html>Sorts that did not fit in the sort heap.</html>");
			mtd.addColumn(CM_NAME, "TOTAL_COMPILATIONS",        "<html>Statements that were compiled (prepared). High compared to ACT_COMPLETED_TOTAL: literals instead of parameter markers?</html>");
			mtd.addColumn(CM_NAME, "TOTAL_COMPILE_TIME",        "<html>Time spent compiling, in milliseconds.</html>");
			mtd.addColumn(CM_NAME, "PKG_CACHE_LOOKUPS",         "<html>Lookups in the package cache.</html>");
			mtd.addColumn(CM_NAME, "PKG_CACHE_INSERTS",         "<html>Statements that were not in the package cache.</html>");
			mtd.addColumn(CM_NAME, "TOTAL_ROUTINE_TIME",        "<html>Time spent in routines (procedures, functions), in milliseconds.</html>");
			mtd.addColumn(CM_NAME, "TCPIP_SEND_VOLUME",         "<html>Bytes sent to the client.</html>");
			mtd.addColumn(CM_NAME, "TCPIP_RECV_VOLUME",         "<html>Bytes received from the client.</html>");
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
		pkCols.add("APPLICATION_ID");
		pkCols.add("MEMBER");

		return pkCols;
	}

	@Override
	public String getSqlForVersion(DbxConnection conn, DbmsVersionInfo versionInfo)
	{
		// NOTE: An OPEN transaction is a unit of work without UOW_STOP_TIME (an idle connection after a commit still shows its last unit of work, with a stop time)
		String sql = ""
			+ "select /* ${cmCollectorName} */ \n"
			+ "     APPLICATION_HANDLE \n"
			+ "    ,APPLICATION_ID \n"
			+ "    ,MEMBER \n"
			+ "    ,APPLICATION_NAME \n"
			+ "    ,RTRIM(SESSION_AUTH_ID) as SESSION_AUTH_ID \n"
			+ "    ,CLIENT_HOSTNAME \n"
			+ "    ,CLIENT_IPADDR \n"
			+ "    ,CLIENT_PID \n"
			+ "    ,CLIENT_PLATFORM \n"
			+ "    ,CLIENT_PROTOCOL \n"
			+ "    ,CLIENT_USERID \n"
			+ "    ,CLIENT_WRKSTNNAME \n"
			+ "    ,CLIENT_APPLNAME \n"
			+ "    ,CONNECTION_START_TIME \n"
			+ "    ,IS_SYSTEM_APPL \n"
			+ "    ,WORKLOAD_OCCURRENCE_STATE \n"
			+ "    ,CASE WHEN UOW_START_TIME is not NULL AND UOW_STOP_TIME is NULL THEN 1 ELSE 0 END as IN_TRAN \n"
			+ "    ,CASE WHEN UOW_START_TIME is not NULL AND UOW_STOP_TIME is NULL THEN SECONDS_BETWEEN(CURRENT TIMESTAMP, UOW_START_TIME) END as TRAN_AGE_SEC \n"
			+ "    ,UOW_START_TIME \n"
			+ "    ,UOW_STOP_TIME \n"
			+ "    ,SQL_REQS_SINCE_COMMIT \n"
			+ "    ,CURRENT_ISOLATION \n"
			+ "    ,LOCK_TIMEOUT_VAL \n"
			+ "    ,NUM_LOCKS_HELD \n"
			+ "    ,NUM_LOCKS_WAITING \n"
			+ "    ,NUM_ASSOC_AGENTS \n"
			+ "    ,TOTAL_APP_COMMITS \n"
			+ "    ,TOTAL_APP_ROLLBACKS \n"
			+ "    ,ACT_COMPLETED_TOTAL \n"
			+ "    ,ACT_ABORTED_TOTAL \n"
			+ "    ,APP_RQSTS_COMPLETED_TOTAL \n"
			+ "    ,FAILED_SQL_STMTS \n"
			+ "    ,ROWS_READ \n"
			+ "    ,ROWS_RETURNED \n"
			+ "    ,ROWS_MODIFIED \n"
			+ "    ,(POOL_DATA_L_READS + POOL_INDEX_L_READS + POOL_XDA_L_READS + POOL_COL_L_READS + POOL_TEMP_DATA_L_READS + POOL_TEMP_INDEX_L_READS + POOL_TEMP_XDA_L_READS + POOL_TEMP_COL_L_READS) as BP_LOGICAL_READS \n"
			+ "    ,(POOL_DATA_P_READS + POOL_INDEX_P_READS + POOL_XDA_P_READS + POOL_COL_P_READS + POOL_TEMP_DATA_P_READS + POOL_TEMP_INDEX_P_READS + POOL_TEMP_XDA_P_READS + POOL_TEMP_COL_P_READS) as BP_PHYSICAL_READS \n"
			+ "    ,DIRECT_READS \n"
			+ "    ,DIRECT_WRITES \n"
			+ "    ,TOTAL_CPU_TIME \n"
			+ "    ,TOTAL_RQST_TIME \n"
			+ "    ,TOTAL_WAIT_TIME \n"
			+ "    ,CLIENT_IDLE_WAIT_TIME \n"
			+ "    ,LOCK_WAITS \n"
			+ "    ,LOCK_WAIT_TIME \n"
			+ "    ,LOCK_TIMEOUTS \n"
			+ "    ,DEADLOCKS \n"
			+ "    ,LOCK_ESCALS \n"
			+ "    ,POOL_READ_TIME \n"
			+ "    ,POOL_WRITE_TIME \n"
			+ "    ,DIRECT_READ_TIME \n"
			+ "    ,LOG_DISK_WAIT_TIME \n"
			+ "    ,(TCPIP_SEND_WAIT_TIME + TCPIP_RECV_WAIT_TIME) as TCPIP_WAIT_TIME \n"
			+ "    ,TOTAL_SORTS \n"
			+ "    ,SORT_OVERFLOWS \n"
			+ "    ,TOTAL_COMPILATIONS \n"
			+ "    ,TOTAL_COMPILE_TIME \n"
			+ "    ,PKG_CACHE_LOOKUPS \n"
			+ "    ,PKG_CACHE_INSERTS \n"
			+ "    ,TOTAL_ROUTINE_TIME \n"
			+ "    ,TCPIP_SEND_VOLUME \n"
			+ "    ,TCPIP_RECV_VOLUME \n"
			+ "from table(MON_GET_CONNECTION(NULL, -2)) c \n"
			+ "order by APPLICATION_HANDLE, MEMBER \n"
			+ "";

		return sql;
	}
}
