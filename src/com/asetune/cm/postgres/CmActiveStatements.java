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
package com.asetune.cm.postgres;

import java.awt.event.MouseEvent;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeoutException;

import javax.naming.NameNotFoundException;

import org.apache.log4j.Logger;

import com.asetune.ICounterController;
import com.asetune.IGuiController;
import com.asetune.Version;
import com.asetune.alarm.AlarmHandler;
import com.asetune.alarm.AlarmHelper;
import com.asetune.alarm.events.AlarmEvent;
import com.asetune.alarm.events.AlarmEventBlockingLockAlarm;
import com.asetune.alarm.events.AlarmEventHoldingLocksWhileWaitForClientInput;
import com.asetune.alarm.events.AlarmEventLongRunningStatement;
import com.asetune.alarm.events.AlarmEventLongRunningTransaction;
import com.asetune.cm.CmSettingsHelper;
import com.asetune.cm.CmSettingsHelper.RegExpInputValidator;
import com.asetune.cm.CounterSample;
import com.asetune.cm.CounterSetTemplates;
import com.asetune.cm.CounterSetTemplates.Type;
import com.asetune.cm.CountersModel;
import com.asetune.cm.postgres.gui.CmActiveStatementsPanel;
import com.asetune.config.dict.MonTablesDictionary;
import com.asetune.config.dict.MonTablesDictionaryManager;
import com.asetune.config.dict.PostgresWaitTypeDictionary;
import com.asetune.gui.MainFrame;
import com.asetune.gui.TabularCntrPanel;
import com.asetune.pcs.PcsColumnOptions;
import com.asetune.pcs.PcsColumnOptions.ColumnType;
import com.asetune.sql.conn.DbxConnection;
import com.asetune.sql.conn.info.DbmsVersionInfo;
import com.asetune.utils.Configuration;
import com.asetune.utils.MathUtils;
import com.asetune.utils.PostgresUtils;
import com.asetune.utils.PostgresUtils.PgLockRecord;
import com.asetune.utils.StringUtil;
import com.asetune.utils.Ver;

/**
 * @author Goran Schwarz (goran_schwarz@hotmail.com)
 */
public class CmActiveStatements
extends CountersModel
{
	private static Logger        _logger          = Logger.getLogger(CmActiveStatements.class);
	private static final long    serialVersionUID = 1L;

	public static final String   CM_NAME          = CmActiveStatements.class.getSimpleName();
	public static final String   SHORT_NAME       = "Active Statements";
	public static final String   HTML_DESC        = 
		"<html>" +
		"Statemenets that are currently executing in the ASE." +
		"<br><br>" +
		"Table Background colors:" +
		"<ul>" +
		"    <li>GREEN  - active - The backend is executing a query.</li>" +
//		"    <li>WHITE  - idle - The backend is waiting for a new client command.</li>" +
		"    <li>YELLOW - idle in transaction -  The backend is in a transaction, but is not currently executing a query.</li>" +
//		"    <li>PINK   - idle in transaction (aborted) -  This state is similar to idle in transaction, except one of the statements in the transaction caused an error.</li>" +
//		"    <li>XXX    - fastpath function call -  The backend is executing a fast-path function.</li>" +
//		"    <li>XXX    - disabled -  This state is reported if track_activities is disabled in this backend.</li>" +
		"    <li>PINK   - PID is Blocked by another PID from running, this PID is the Victim of a Blocking Lock, which is showned in RED.</li>" +
		"    <li>RED    - PID is Blocking other PID's from running, this PID is Responslibe or the Root Cause of a Blocking Lock.</li>" +
		"    <li>LIGHT_BLUE - (only on cell 'pid_exlock_count') PID is holding <b>Exclusive</b> locks.</li>" +
		"</ul>" +
		"</html>";

	public static final String   GROUP_NAME       = MainFrame.TCP_GROUP_OBJECT_ACCESS;
	public static final String   GUI_ICON_FILE    = "images/"+CM_NAME+".png";

	public static final long     NEED_SRV_VERSION = 0;
	public static final long     NEED_CE_VERSION  = 0;

	public static final String[] MON_TABLES       = new String[] {CM_NAME, "pg_stat_activity"};
	public static final String[] NEED_ROLES       = new String[] {};
	public static final String[] NEED_CONFIG      = new String[] {};

	public static final String[] PCT_COLUMNS      = new String[] {};
	public static final String[] DIFF_COLUMNS     = new String[] {};

	public static final boolean  NEGATIVE_DIFF_COUNTERS_TO_ZERO = true;
	public static final boolean  IS_SYSTEM_CM                   = true;
	public static final int      DEFAULT_POSTPONE_TIME          = 0;
	public static final int      DEFAULT_QUERY_TIMEOUT          = CountersModel.DEFAULT_sqlQueryTimeout;

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

		setCounterController(counterController);
		setGuiController(guiController);
		
		addTrendGraphs();
		
		CounterSetTemplates.register(this);
	}


	//------------------------------------------------------------
	// Implementation
	//------------------------------------------------------------
	private static final String  PROP_PREFIX                = CM_NAME;

//	public static final String  PROPKEY_exclude_IdleInTransaction_lt_ms = PROP_PREFIX + ".exclude.idle-in-transaction.lt.ms";
//	public static final int     DEFAULT_exclude_IdleInTransaction_lt_ms = 200; 

	public static final String  PROPKEY_sample_pidLocks          = PROP_PREFIX + ".sample.pidLocks";
	public static final boolean DEFAULT_sample_pidLocks          = true;

	
	private void addTrendGraphs()
	{
	}

	/** Used by the: Create 'Offline Session' Wizard */
	@Override
	public List<CmSettingsHelper> getLocalSettings()
	{
		Configuration conf = Configuration.getCombinedConfiguration();
		List<CmSettingsHelper> list = new ArrayList<>();
		
//		list.add(new CmSettingsHelper("Get SPID's holding locks" , PROPKEY_sample_holdingLocks , Boolean.class, conf.getBooleanProperty(PROPKEY_sample_holdingLocks , DEFAULT_sample_holdingLocks ), DEFAULT_sample_holdingLocks , "Include SPID's that holds locks even if that are not active in the server." ));
		list.add(new CmSettingsHelper("Get PID's Locks"           , PROPKEY_sample_pidLocks    , Boolean.class, conf.getBooleanProperty(PROPKEY_sample_pidLocks    , DEFAULT_sample_pidLocks    ), DEFAULT_sample_pidLocks    , "Do 'select <i>someCols</i> from pg_locks where pid = ?' on every row in the table. This will help us to diagnose what the current SQL statement is locking."));

		return list;
	}

	@Override
	public void addMonTableDictForVersion(DbxConnection conn, DbmsVersionInfo versionInfo)
	{
		try 
		{
			MonTablesDictionary mtd = MonTablesDictionaryManager.getInstance();
			mtd.addTable("CmActiveStatements",  "FIXME.");

			mtd.addColumn("CmActiveStatements", "datid"                             ,  "<html>OID of the database this backend is connected to  </html>");
			mtd.addColumn("CmActiveStatements", "datname"                           ,  "<html>Name of the database this backend is connected to  </html>");
			mtd.addColumn("CmActiveStatements", "pid"                               ,  "<html>Process ID of this backend  </html>");
			mtd.addColumn("CmActiveStatements", "leader_pid"                        ,  "<html>Process ID of the parallel group leader, if this process is a parallel query worker. NULL if this process is a parallel group leader or does not participate in parallel query.  </html>");
			mtd.addColumn("CmActiveStatements", "im_blocked_by_pids"                ,  "<html>This pid is <b>blocked</b> by a list of other pid's. This pad will WAIT until the other pid's has released there locks.<br><b>Note:</b> This is only maintained from Version 9.6 and above.  </html>");
			mtd.addColumn("CmActiveStatements", "im_blocked_max_wait_time_in_sec"   ,  "<html>This pid has been <b>blocked</b> for this amount of times (max wait time from pg_locks)</html>");
			mtd.addColumn("CmActiveStatements", "im_blocking_other_pids"            ,  "<html>This pid is <b>BLOCKING</b> other pid's from working. In a blocking situation this pid is the <b>root cause</b>. Check the SQL Statement or the 'lock list', which is also provided here.  </html>");
			mtd.addColumn("CmActiveStatements", "im_blocking_others_max_time_in_sec",  "<html>Number of seconds this pid has been blocking other pid's from working. <br><b>Note:</b> This is only maintained from Version 14 and above.  </html>");
			mtd.addColumn("CmActiveStatements", "usesysid"                          ,  "<html>OID of the user logged into this backend  </html>");
			mtd.addColumn("CmActiveStatements", "usename"                           ,  "<html>Name of the user logged into this backend  </html>");
			mtd.addColumn("CmActiveStatements", "application_name"                  ,  "<html>Name of the application that is connected to this backend  </html>");
			mtd.addColumn("CmActiveStatements", "xact_start_sec"                    ,  "<html>How many seconds has the Transaction been running for.  </html>");
			mtd.addColumn("CmActiveStatements", "stmnt_start_sec"                   ,  "<html>How many seconds has the Statement been running for.  </html>");
			mtd.addColumn("CmActiveStatements", "stmnt_last_exec_sec"               ,  "<html>How many seconds did the last Statement run for.  </html>");
			mtd.addColumn("CmActiveStatements", "in_current_state_sec"              ,  "<html>How many seconds have we been in the current state. For example if we are in 'idle in transaction'... Then commit the transaction. Or turn AutoCommit=true (otherwise a new transaction is started as soon as you do 'anything').  </html>");
			mtd.addColumn("CmActiveStatements", "xact_start_age"                    ,  "<html>What time did the last Transaction start. (HH:MM:SS.ssssss)  </html>");
			mtd.addColumn("CmActiveStatements", "stmnt_start_age"                   ,  "<html>What time did the last Statement start. (HH:MM:SS.ssssss)  </html>");
			mtd.addColumn("CmActiveStatements", "stmnt_last_exec_age"               ,  "<html>When did the last Statement Execute (HH:MM:SS.ssssss)  </html>");
			mtd.addColumn("CmActiveStatements", "in_current_state_age"              ,  "<html>For how long have we been in this state (HH:MM:SS.ssssss)  </html>");
			mtd.addColumn("CmActiveStatements", "has_sql_text"                      ,  "<html>If we have any SQL Statement in column 'last_known_sql_statement'  </html>");
			mtd.addColumn("CmActiveStatements", "has_pid_lock_info"                 ,  "<html>A table of locks that this PID is holding. <br><b>Note:</b> WaitTime in the table is only maintained from Version 14 and above.  </html>");
			mtd.addColumn("CmActiveStatements", "pid_lock_count"                    ,  "<html>How many locks does this PID hold</html>");
			mtd.addColumn("CmActiveStatements", "pid_exlock_count"                  ,  "<html>How many <b>Exclusive</b> locks does this PID hold</html>");
			mtd.addColumn("CmActiveStatements", "pid_advlock_count"                 ,  "<html>How many <b>Advisory Exclusive</b> locks does this PID hold</html>");
			mtd.addColumn("CmActiveStatements", "has_blocked_pids_info"             ,  "<html>Has values in column 'blocked_pids_info'  </html>");
			mtd.addColumn("CmActiveStatements", "backend_start"                     ,  "<html>Time when this process was started. For client backends, this is the time the client connected to the server.  </html>");
			mtd.addColumn("CmActiveStatements", "xact_start"                        ,  "<html>Time when this process' current transaction was started, or null if no transaction is active. If the current query is the first of its transaction, this column is equal to the query_start column.  </html>");
			mtd.addColumn("CmActiveStatements", "query_start"                       ,  "<html>Time when the currently active query was started, or if state is not active, when the last query was started  </html>");
			mtd.addColumn("CmActiveStatements", "state_change"                      ,  "<html>Time when the state was last changed  </html>");
			mtd.addColumn("CmActiveStatements", "wait_event_type"                   ,  "<html>The type of event for which the backend is waiting, if any; otherwise NULL. See Table 28.4.  "
					+ "<ul>"
					+ "  <li><b>Activity  </b> - The server process is idle. This event type indicates a process waiting for activity in its main processing loop. wait_event will identify the specific wait point. <a href='https://www.postgresql.org/docs/current/monitoring-stats.html#WAIT-EVENT-ACTIVITY-TABLE'>See table</a> </li>"
					+ "  <li><b>BufferPin </b> - The server process is waiting for exclusive access to a data buffer. Buffer pin waits can be protracted if another process holds an open cursor that last read data from the buffer in question. <a href='https://www.postgresql.org/docs/current/monitoring-stats.html#WAIT-EVENT-BUFFERPIN-TABLE'>See table</a> </li>"
					+ "  <li><b>Client    </b> - The server process is waiting for activity on a socket connected to a user application. Thus, the server expects something to happen that is independent of its internal processes. wait_event will identify the specific wait point. <a href='https://www.postgresql.org/docs/current/monitoring-stats.html#WAIT-EVENT-CLIENT-TABLE'>See table</a> </li>"
					+ "  <li><b>Extension </b> - The server process is waiting for some condition defined by an extension module. <a href='https://www.postgresql.org/docs/current/monitoring-stats.html#WAIT-EVENT-EXTENSION-TABLE'>See table</a> </li>"
					+ "  <li><b>IO        </b> - The server process is waiting for an I/O operation to complete. wait_event will identify the specific wait point. <a href='https://www.postgresql.org/docs/current/monitoring-stats.html#WAIT-EVENT-IO-TABLE'>See table</a> </li>"
					+ "  <li><b>IPC       </b> - The server process is waiting for some interaction with another server process. wait_event will identify the specific wait point. <a href='https://www.postgresql.org/docs/current/monitoring-stats.html#WAIT-EVENT-IPC-TABLE'>See table</a> </li>"
					+ "  <li><b>Lock      </b> - The server process is waiting for a heavyweight lock. Heavyweight locks, also known as lock manager locks or simply locks, primarily protect SQL-visible objects such as tables. However, they are also used to ensure mutual exclusion for certain internal operations such as relation extension. wait_event will identify the type of lock awaited. <a href='https://www.postgresql.org/docs/current/monitoring-stats.html#WAIT-EVENT-LOCK-TABLE'>See table</a> </li>"
					+ "  <li><b>LWLock    </b> - The server process is waiting for a lightweight lock. Most such locks protect a particular data structure in shared memory. wait_event will contain a name identifying the purpose of the lightweight lock. (Some locks have specific names; others are part of a group of locks each with a similar purpose.). <a href='https://www.postgresql.org/docs/current/monitoring-stats.html#WAIT-EVENT-LWLOCK-TABLE'>See table</a> </li>"
					+ "  <li><b>Timeout   </b> - The server process is waiting for a timeout to expire. wait_event will identify the specific wait point. <a href='https://www.postgresql.org/docs/current/monitoring-stats.html#WAIT-EVENT-TIMEOUT-TABLE'>See table</a> </li>"
					+ "</ul>"
					+ "</html>");
			mtd.addColumn("CmActiveStatements", "wait_event"                        ,  "<html>Wait event name if backend is currently waiting, otherwise NULL. The list of events are to long to display here. see: https://www.postgresql.org/docs/current/monitoring-stats.html#WAIT-EVENT-TABLE  </html>");
			mtd.addColumn("CmActiveStatements", "state"                             ,  "<html>Current overall state of this backend. Possible values are:"
			                                                                                  + "<ul>"
			                                                                                  + "  <li><b>active</b>                        - The backend is executing a query.</li>"
			                                                                                  + "  <li><b>idle</b>                          - The backend is waiting for a new client command.</li>"
			                                                                                  + "  <li><b>idle in transaction</b>           - The backend is in a transaction, but is not currently executing a query.</li>"
			                                                                                  + "  <li><b>idle in transaction (aborted)</b> - This state is similar to idle in transaction, except one of the statements in the transaction caused an error.</li>"
			                                                                                  + "  <li><b>fastpath function call</b>        - The backend is executing a fast-path function.</li>"
			                                                                                  + "  <li><b>disabled</b>                      - This state is reported if track_activities is disabled in this backend.</li>"
			                                                                                  + "</ul>"
			                                                                                  + "</html>");
			mtd.addColumn("CmActiveStatements", "backend_xid"                       ,  "<html>Top-level transaction identifier of this backend, if any  </html>");
			mtd.addColumn("CmActiveStatements", "backend_xmin"                      ,  "<html>The current backend's xmin horizon  </html>");
			mtd.addColumn("CmActiveStatements", "backend_type"                      ,  "<html>Type of current backend. Possible types are autovacuum launcher, autovacuum worker, logical replication launcher, logical replication worker, parallel worker, background writer, client backend, checkpointer, archiver, startup, walreceiver, walsender and walwriter. In addition, background workers registered by extensions may have additional types  </html>");
			mtd.addColumn("CmActiveStatements", "client_addr"                       ,  "<html>IP address of the client connected to this backend. If this field is null, it indicates either that the client is connected via a Unix socket on the server machine or that this is an internal process such as autovacuum  </html>");
			mtd.addColumn("CmActiveStatements", "client_hostname"                   ,  "<html>Host name of the connected client, as reported by a reverse DNS lookup of client_addr. This field will only be non-null for IP connections, and only when log_hostname is enabled  </html>");
			mtd.addColumn("CmActiveStatements", "client_port"                       ,  "<html>TCP port number that the client is using for communication with this backend, or -1 if a Unix socket is used. If this field is null, it indicates that this is an internal server process  </html>");
			mtd.addColumn("CmActiveStatements", "query_id"                          ,  "<html>Identifier of this backend's most recent query. If state is active this field shows the identifier of the currently executing query. In all other states, it shows the identifier of last query that was executed. Query identifiers are not computed by default so this field will be null unless compute_query_id parameter is enabled or a third-party module that computes query identifiers is configured  </html>");
			mtd.addColumn("CmActiveStatements", "last_known_sql_statement"          ,  "<html>Text of this backend's most recent query. If state is active this field shows the currently executing query. In all other states, it shows the last query that was executed. By default the query text is truncated at 1024 bytes; this value can be changed via the parameter track_activity_query_size  </html>");
			mtd.addColumn("CmActiveStatements", "pid_lock_info"                     ,  "<html>A table of locks that this PID is holding. <br><b>Note:</b> WaitTime in the table is only maintained from Version 14 and above.  </html>");
			mtd.addColumn("CmActiveStatements", "blocked_pids_info"                 ,  "<html>If this PID is BLOCKING other pid's, then here is a html-table of info for the Blocked spid's.  </html>");
			mtd.addColumn("CmActiveStatements", "execTimeInMs"                      ,  "<html>How many milli seconds has current Statement been running for. -1 If the Statement has finnished.  </html>");
			mtd.addColumn("CmActiveStatements", "xactTimeInMs"                      ,  "<html>How many milli seconds has the Transaction been running for  </html>");
		}
		catch (NameNotFoundException e) 
		{
			_logger.warn("Problems in cm='" + CM_NAME + "', adding addMonTableDictForVersion. Caught: " + e); 
		//	System.out.println("Problems in cm='" + CM_NAME + "', adding addMonTableDictForVersion. Caught: " + e); 
		}
	}

	
	@Override
	protected TabularCntrPanel createGui()
	{
		return new CmActiveStatementsPanel(this);
	}
	
	@Override
	public Map<String, PcsColumnOptions> getPcsColumnOptions()
	{
		Map<String, PcsColumnOptions> map = super.getPcsColumnOptions();

		// No settings in the super, create one, and set it at super
		if (map == null)
		{
			map = new HashMap<>();
			map.put("query", new PcsColumnOptions(ColumnType.DICTIONARY_COMPRESSION));

			// Set the map in the super
			setPcsColumnOptions(map);
		}

		return map;
	}
	
	@Override
	public List<String> getPkForVersion(DbxConnection conn, DbmsVersionInfo versionInfo)
	{
		List <String> pkCols = new LinkedList<String>();

		pkCols.add("pid");

		return pkCols;
	}

	@Override
	public String getSqlForVersion(DbxConnection conn, DbmsVersionInfo versionInfo)
	{
		// do NOT include records with status='idle in transaction' with values less than
//		int exclude_idleInTransaction_lt_ms = Configuration.getCombinedConfiguration().getIntProperty(PROPKEY_exclude_IdleInTransaction_lt_ms, DEFAULT_exclude_IdleInTransaction_lt_ms);
		
		String waiting = "    ,waiting \n";

		// ----- 9.4
		String backend_xid = "";
		String backend_xmin = "";
		if (versionInfo.getLongVersion() >= Ver.ver(9, 4))
		{
			backend_xid  = "    ,backend_xid \n";
			backend_xmin = "    ,backend_xmin \n";
		}

		// ----- 9.6
		String im_blocked_by_pids                 = "";
		String im_blocking_other_pids             = "";
		String im_blocked_max_wait_time_in_sec    = "";
		String im_blocking_others_max_time_in_sec = "";
		String wait_event_type                    = "";
		String wait_event                         = "";
		if (versionInfo.getLongVersion() >= Ver.ver(9, 6))
		{
			im_blocked_by_pids                 = "    ,CAST(array_to_string(pg_blocking_pids(pid), ', ') as varchar(512)) AS im_blocked_by_pids \n";
			im_blocking_other_pids             = "    ,CAST('' as varchar(512)) AS im_blocking_other_pids \n";
			im_blocked_max_wait_time_in_sec    = "    ,CAST(-1 as integer)      AS im_blocked_max_wait_time_in_sec \n";
			im_blocking_others_max_time_in_sec = "    ,CAST(-1 as integer)      AS im_blocking_others_max_time_in_sec \n";

			waiting         = ""; // Waiting was removed in 9.6 and replaced by wait_event_type and wait_event
			wait_event_type = "    ,CAST(wait_event_type as varchar(128)) AS wait_event_type \n";
			wait_event      = "    ,CAST(wait_event      as varchar(128)) AS wait_event \n";
		}

		// ----- 10
		String backend_type = "";
		if (versionInfo.getLongVersion() >= Ver.ver(10))
		{
			backend_type    = "    ,CAST(backend_type    as varchar(128)) AS backend_type \n";
		}

		// ----- 11: No changes
		// ----- 12: No changes
		// ----- 13
		String leader_pid = "";
		if (versionInfo.getLongVersion() >= Ver.ver(13))
		{
			leader_pid  = "    ,leader_pid \n";
		}

		// ----- 14
		String query_id = "";
		if (versionInfo.getLongVersion() >= Ver.ver(14))
		{
			query_id  = "    ,query_id \n";
		}

		// ----- 15: No changes

		// Construct the SQL Statement
		String sql = ""
				+ "select \n"
				+ "     datid \n"
				+ "    ,datname \n"
				+ leader_pid
				+ "    ,pid \n"
				+ "    ,CAST(state            as varchar(128)) AS state \n"
				+ backend_type
				+ waiting
				+ wait_event_type
				+ wait_event
				
				+ im_blocked_by_pids
				+ im_blocking_other_pids
				+ im_blocked_max_wait_time_in_sec
				+ im_blocking_others_max_time_in_sec
				+ "    ,usesysid \n"
				+ "    ,usename \n"
				+ "    ,CAST(application_name as varchar(128)) AS application_name \n"
			    + " \n"
				+ "    ,CAST(false as boolean)                 AS has_sql_text \n"
				+ "    ,CAST(false as boolean)                 AS has_pid_lock_info \n"
				+ "    ,CAST(-1    as integer)                 AS pid_lock_count \n"
				+ "    ,CAST(-1    as integer)                 AS pid_exlock_count \n"
				+ "    ,CAST(-1    as integer)                 AS pid_advlock_count \n"
				+ "    ,CAST(false as boolean)                 AS has_blocked_pids_info \n"
			    + " \n"
			    + "    ,CAST( COALESCE( EXTRACT('epoch' FROM clock_timestamp()) - EXTRACT('epoch' FROM xact_start ), -1) as numeric(12,1))  AS xact_start_sec \n"
			    + "    ,CAST( COALESCE( EXTRACT('epoch' FROM clock_timestamp()) - EXTRACT('epoch' FROM query_start), -1) as numeric(12,1))  AS stmnt_start_sec \n"
			    + "    ,CAST( CASE WHEN state = 'active' \n"
			    + "                THEN COALESCE( EXTRACT('epoch' FROM clock_timestamp()) - EXTRACT('epoch' FROM query_start ), -1) /* active -- query-elapsed-time */ \n"
			    + "                ELSE COALESCE( EXTRACT('epoch' FROM state_change     ) - EXTRACT('epoch' FROM query_start ), -1) /* else   -- last-exec-time*/ \n"
			    + "     END as numeric(12,1))                                                                                               AS stmnt_last_exec_sec \n"
			    + "    ,CAST( COALESCE( EXTRACT('epoch' FROM clock_timestamp()) - EXTRACT('epoch' FROM state_change), -1) as numeric(12,1)) AS in_current_state_sec \n"
			    + " \n"
				+ "    ,CAST( clock_timestamp() - xact_start   as varchar(30)) AS xact_start_age \n"
				+ "    ,CAST( clock_timestamp() - query_start  as varchar(30)) AS stmnt_start_age \n"
				+ "    ,CAST( CASE WHEN state = 'active' \n"
				+ "                THEN clock_timestamp() - query_start /* active -- query-elapsed-time */ \n"
				+ "                ELSE state_change      - query_start /* else   -- last-exec-time*/ \n"
				+ "           END as varchar(30))                              AS stmnt_last_exec_age \n"
				+ "    ,CAST( clock_timestamp() - state_change as varchar(30)) AS in_current_state_age \n"
			    + " \n"
				+ "    ,backend_start \n"
				+ "    ,xact_start \n"
				+ "    ,query_start \n"
				+ "    ,state_change \n"
				+ backend_xid
				+ backend_xmin
				+ "    ,CAST(client_addr      as varchar(128)) AS client_addr \n"
				+ "    ,CAST(client_hostname  as varchar(128)) AS client_hostname \n"
				+ "    ,client_port \n"
				+ query_id
				+ "    ,query                                  AS last_known_sql_statement \n"

				+ "    ,CAST(null as text)                     AS pid_lock_info \n"
				+ "    ,CAST(null as text)                     AS blocked_pids_info \n"

				+ "    ,CASE WHEN state != 'active' OR state IS NULL THEN -1 \n"
				+ "          ELSE cast(((EXTRACT('epoch' from CLOCK_TIMESTAMP()) - EXTRACT('epoch' from query_start)) * 1000) as int) \n"
				+ "     END as \"execTimeInMs\" \n"

				+ "    ,CASE WHEN xact_start IS NULL THEN -1 \n"
				+ "          ELSE cast(((EXTRACT('epoch' from CLOCK_TIMESTAMP()) - EXTRACT('epoch' from xact_start)) * 1000) as int) \n"
				+ "     END as \"xactTimeInMs\" \n"

				+ "from pg_stat_activity \n"
				+ "where state != 'idle' \n"
				+ "  and pid   != pg_backend_pid() \n"
				+ "  and application_name != '" + Version.getAppName() + "' \n"
				+ "";

		// Note: This FILTER should be MOVED into the HTML page (so users can decide the filter criteria) 
//		if (exclude_idleInTransaction_lt_ms >= 0)
//		{
//			sql += ""
//				+ "  /* for state='idle in transaction', then show 'xact_start' older than " + exclude_idleInTransaction_lt_ms + " ms */ \n"
//				+ "  and ((EXTRACT('epoch' from CLOCK_TIMESTAMP()) - EXTRACT('epoch' from xact_start)) * 1000) > (CASE WHEN state = 'idle in transaction' THEN " + exclude_idleInTransaction_lt_ms + " ELSE -1 END) \n" 
//				+ "";
//		}
			
		return sql;
	}

	// The below is not more needed... since we do proper CAST(...) of data types in the SELECT Statement
//	/**
//	 * Change data types (or length) for some column  
//	 * <p>
//	 * We could have done that by converting columns into varchar datatypes, but since we do: "select * from ..." 
//	 * for forward/backward compatibility, this is done in the code instead...<br>
//	 * When we switch to "column specified" SQL Statement, then we can get rid of this!  
//	 */
//	@Override
//	public ResultSetMetaDataCached createResultSetMetaData(ResultSetMetaData rsmd) throws SQLException
//	{
//		ResultSetMetaDataCached rsmdc = super.createResultSetMetaData(rsmd);
//
//		if (rsmdc == null)
//			return null;
//		
//		// In PG x.y
//		setColumnShorterLength(rsmdc, "application_name" , 60);  // text --> varchar(60)
//		setColumnShorterLength(rsmdc, "client_addr"      , 30);  // text --> varchar(30)
//		setColumnShorterLength(rsmdc, "client_hostname"  , 60);  // text --> varchar(60)
//		setColumnShorterLength(rsmdc, "state"            , 30);  // text --> varchar(30)
////		setColumnShorterLength(rsmdc, "backend_xid"      , 20);  // xid  --- Already set to varchar(30) in com.asetune.sql.ddl.DbmsDdlResolverPostgres
////		setColumnShorterLength(rsmdc, "backend_xmin"     , 20);  // xid  --- Already set to varchar(30) in com.asetune.sql.ddl.DbmsDdlResolverPostgres
//		
//		// In PG 9.6
//		setColumnShorterLength(rsmdc, "wait_event_type"  , 30);  // text --> varchar(30)
//		setColumnShorterLength(rsmdc, "wait_event"       , 50);  // text --> varchar(50)
//
//		// In PG 10
//		setColumnShorterLength(rsmdc, "backend_type"     , 30);  // text --> varchar(30)
//		
//		return rsmdc;
//	}
//
//	private void setColumnShorterLength(ResultSetMetaDataCached rsmdc, String colName, int newLength)
//	{
//		int colPos = rsmdc.findColumn(colName);
//		
//		// return if column wasn't found
//		if (colPos == -1)
//			return;
//		
//		Entry colEntry = rsmdc.getEntry(colPos);
//		if (colEntry.getPrecision() > newLength)
//		{
//			colEntry.setColumnType(Types.VARCHAR);
//			colEntry.setColumnTypeName("varchar");
//			colEntry.setPrecision(newLength);
//		}
//	}
	
	@Override
	public String getToolTipTextOnTableCell(MouseEvent e, String colName, Object cellValue, int modelRow, int modelCol) 
	{
		// query
		if ("has_sql_text".equals(colName))
		{
			// Find 'SpidLocks' column, is so get it and set it as the tool tip
			int pos_sql_text = findColumn("last_known_sql_statement");
			if (pos_sql_text > 0)
			{
				Object cellVal = getValueAt(modelRow, pos_sql_text);
				if (cellVal instanceof String)
				{
					return "<html><pre>" + cellVal + "</pre></html>";
				}
			}
		}
		if ("last_known_sql_statement".equals(colName))
		{
			return cellValue == null ? null : toHtmlString(cellValue.toString());
		}


		if ("wait_event_type".equals(colName))
		{
			return cellValue == null ? null : PostgresWaitTypeDictionary.getWaitEventTypeDescription(cellValue.toString());
		}
		if ("wait_event".equals(colName))
		{
			return cellValue == null ? null : PostgresWaitTypeDictionary.getWaitEventDescription(cellValue.toString());
		}
		
		
		if ("has_pid_lock_info".equals(colName))
		{
			// Find 'SpidLocks' column, is so get it and set it as the tool tip
			int pos_pidLocks = findColumn("pid_lock_info");
			if (pos_pidLocks > 0)
			{
				Object cellVal = getValueAt(modelRow, pos_pidLocks);
				if (cellVal instanceof String)
				{
					return "<html><pre>" + cellVal + "</pre></html>";
				}
			}
		}
		if ("pid_lock_info".equals(colName))
		{
			return cellValue == null ? null : "<html><pre>" + cellValue + "</pre></html>";
		}


		if ("has_blocked_pids_info".equals(colName))
		{
			// Find 'BlockedSpidsInfo' column, is so get it and set it as the tool tip
			int pos_BlockedSpidsInfo = findColumn("blocked_pids_info");
			if (pos_BlockedSpidsInfo > 0)
			{
				Object cellVal = getValueAt(modelRow, pos_BlockedSpidsInfo);
				if (cellVal == null)
					return "<html>No value</html>";
				else
					return "<html><pre>" + cellVal + "</pre></html>";
			}
		}
		if ("blocked_pids_info".equals(colName))
		{
			return cellValue == null ? null : "<html><pre>" + cellValue + "</pre></html>";
		}

		return super.getToolTipTextOnTableCell(e, colName, cellValue, modelRow, modelCol);
	}
	/** add HTML around the string, and translate line breaks into <br> */
	private String toHtmlString(String in)
	{
		String str = StringUtil.makeApproxLineBreak(in, 150, 10, "\n");
		str = str.replaceAll("\\n", "<br>");
		if (in.indexOf("<html>")>=0 || in.indexOf("<HTML>")>=0)
			return str;
		return "<html><pre>" + str + "</pre></html>";
	}
	

//	@Override
//	public Class<?> getColumnClass(int columnIndex)
//	{
//		// use CHECKBOX for some columns of type bit/Boolean
//		String colName = getColumnName(columnIndex);
//
//		if      ("has_pid_lock_info"    .equals(colName)) return Boolean.class;
//		else if ("has_blocked_pids_info".equals(colName)) return Boolean.class;
//		else return super.getColumnClass(columnIndex);
//	}


	
	@Override
	public void localCalculation(CounterSample newSample)
	{
		// Set the CheckBox 'has_sql_text' to true if we got a SQL Statement
		boolean setHasSqlText = true;
		if (setHasSqlText)
		{
			int pos_has_sql_text = newSample.findColumn("has_sql_text");
			int pos_sql_text     = newSample.findColumn("last_known_sql_statement");
			
			if (pos_has_sql_text != -1 && pos_sql_text != -1)
			{
				for (int rowId=0; rowId < newSample.getRowCount(); rowId++) 
				{
					String sql_text = newSample.getValueAsString(rowId, pos_sql_text);
					if (StringUtil.hasValue(sql_text))
					{
						newSample.setValueAt(true, rowId, pos_has_sql_text);
					}
				}
			}
		} // end: setHasSqlText
			
		
		boolean resolvImBlockedByPids = Configuration.getCombinedConfiguration().getBooleanProperty(PROPKEY_sample_pidLocks, DEFAULT_sample_pidLocks);
		if (resolvImBlockedByPids)
		{
			int pos_pid                    = newSample.findColumn("pid");
			int pos_im_blocked_by_pids     = newSample.findColumn("im_blocked_by_pids");
			int pos_im_blocking_other_pids = newSample.findColumn("im_blocking_other_pids");

			
			if (pos_im_blocked_by_pids != -1 && pos_im_blocking_other_pids != -1)
			{
				for (int rowId=0; rowId < newSample.getRowCount(); rowId++) 
				{
					int pid = newSample.getValueAsInteger(rowId, pos_pid);

					// Get LIST of SPID's that I'm blocking
					String blockingList = getBlockingListStr(newSample, pid, pos_im_blocked_by_pids, pos_pid);
					newSample.setValueAt(blockingList, rowId, pos_im_blocking_other_pids);
				}
			}
		} // end: resolvImBlockedByPids


//TODO; Check CmActiveStatements -- Blocking seems strange
//Check; Daily Summary Report for Servername: mtlplictd01 (2022-12-15) -- in mail;
//And; Who doesnt the BlockingAlrm fire (it does from CmSummary);



		// TODO: The below needs to be implemented BETTER... meaning: PostgresUtils.getLockSummaryForPid
		boolean getPidLocks = Configuration.getCombinedConfiguration().getBooleanProperty(PROPKEY_sample_pidLocks, DEFAULT_sample_pidLocks);
		if (getPidLocks)
		{
			String pidLocksStr     = "This was disabled";
			int    pidLockCount    = -1;
			int    pidExLockCount  = -1; // Exclusive Lock Count
			int    pidAdvLockCount = -1; // Advisory  Lock Count

			int pos_pid                                = newSample.findColumn("pid");
			int pos_has_pid_lock_info                  = newSample.findColumn("has_pid_lock_info");
			int pos_pid_lock_info                      = newSample.findColumn("pid_lock_info");
			int pos_pid_lock_count                     = newSample.findColumn("pid_lock_count");
			int pos_pid_exlock_count                   = newSample.findColumn("pid_exlock_count");
			int pos_pid_advlock_count                  = newSample.findColumn("pid_advlock_count");
			int pos_im_blocked_max_wait_time_in_sec    = newSample.findColumn("im_blocked_max_wait_time_in_sec");
			int pos_im_blocking_others_max_time_in_sec = newSample.findColumn("im_blocking_others_max_time_in_sec");
			

			if (pos_has_pid_lock_info == -1 || pos_pid_lock_info == -1 || pos_pid_lock_count == -1 || pos_pid_exlock_count == -1 || pos_pid_advlock_count == -1)
			{
				_logger.warn("Skipping update of 'locking info', cant find desired columns. pos_has_pid_lock_info=" + pos_has_pid_lock_info + ", pos_pid_lock_info=" + pos_pid_lock_info + ", pos_pid_lock_count=" + pos_pid_lock_count + ", pos_pid_exlock_count=" + pos_pid_exlock_count + ", pos_pid_advlock_count=" + pos_pid_advlock_count);
			}
			else
			{
				for (int rowId=0; rowId < newSample.getRowCount(); rowId++) 
				{
					int pid = newSample.getValueAsInteger(rowId, pos_pid);

					List<PgLockRecord> lockList = null;
					try
					{
						lockList    = PostgresUtils.getLockSummaryForPid(getCounterController().getMonConnection(), pid);
						pidLocksStr = PostgresUtils.getLockSummaryForPid(lockList, true, false);
						if (pidLocksStr == null)
							pidLocksStr = "No Locks found";
					}
					catch (TimeoutException ex)
					{
						pidLocksStr = "Timeout - when getting lock information";
					}
					
					BigDecimal blocked_max_wait_time_in_sec    = null;
					BigDecimal blocking_others_max_time_in_sec = null;
					pidLockCount    = 0;
					pidExLockCount  = 0;
					pidAdvLockCount = 0;
					if (lockList != null)
					{
						for (PgLockRecord lockRecord : lockList)
						{
							pidLockCount    += lockRecord._lockCount;
							pidExLockCount  += lockRecord._exLockCount;
							pidAdvLockCount += lockRecord._advisoryLockCount;

							// Get I'm Being Blocked Max Wait Time
							blocked_max_wait_time_in_sec = MathUtils.max(blocked_max_wait_time_in_sec, lockRecord._lockWaitInSec);

							// Get I'm Blocking others Max Wait Time
							blocking_others_max_time_in_sec = MathUtils.max(blocking_others_max_time_in_sec, lockRecord._blockedPidsMaxWaitInSec);
						}
					}

					boolean b;

					// SPID Locks
					b = !"This was disabled".equals(pidLocksStr) && !"No Locks found".equals(pidLocksStr) && !"Timeout - when getting lock information".equals(pidLocksStr);
					newSample.setValueAt(new Boolean(b),  rowId, pos_has_pid_lock_info);
					newSample.setValueAt(pidLocksStr,     rowId, pos_pid_lock_info);
					newSample.setValueAt(pidLockCount,    rowId, pos_pid_lock_count);
					newSample.setValueAt(pidExLockCount,  rowId, pos_pid_exlock_count);
					newSample.setValueAt(pidAdvLockCount, rowId, pos_pid_advlock_count);

					if (pos_im_blocked_max_wait_time_in_sec != -1 && blocked_max_wait_time_in_sec != null)
					{
						newSample.setValueAt(blocked_max_wait_time_in_sec, rowId, pos_im_blocked_max_wait_time_in_sec);
					}

					if (pos_im_blocking_others_max_time_in_sec != -1 && blocking_others_max_time_in_sec != null)
					{
						newSample.setValueAt(blocking_others_max_time_in_sec, rowId, pos_im_blocking_others_max_time_in_sec);
					}
				}
			} // end: 
		} // end: getPidLocks
		
		
		// Fill in the column 'blocked_pids_info' with a HTML Table for each PID that I'm blocking with: 'last_known_sql_statement' and 'pid_lock_info'
		// In this way, any alarm sent (via email) will have a "full/complete" information both about the RootCause PID and the PID's that are blocked.
		boolean getBlockedPidInfo = true;
		if (getBlockedPidInfo)
		{
			int pos_pid                    = findColumn("pid");
			int pos_has_blocked_pids_info  = findColumn("has_blocked_pids_info");
			int pos_blocked_pids_info      = findColumn("blocked_pids_info");
			int pos_im_blocking_other_pids = findColumn("im_blocking_other_pids");
			int pos_sqlText                = findColumn("last_known_sql_statement");
			int pos_pidLockInfo            = findColumn("pid_lock_info");

			// Loop a seconds time, This to:
			// Fill in the column 'BlockedSpidsInfo'
			// - If this SPID is a BLOCKER - the root cause of blocking other SPID's
			//   Then get: get already collected Showplans etc for SPID's that are BLOCKED (the victims)
			// This will be helpful (to see both side of the story; ROOT cause and the VICTIMS) in a alarm message
			if (pos_blocked_pids_info >= 0)
			{
				for (int rowId=0; rowId < newSample.getRowCount(); rowId++) 
				{
					Object o_im_blocking_other_pids = newSample.getValueAt(rowId, pos_im_blocking_other_pids);

					// MAYBE TODO: possibly check if the 'monSource' is of type "BLOCKER", before getting: getBlockedSpidInfo()
					
					if (o_im_blocking_other_pids != null && o_im_blocking_other_pids instanceof String)
					{
						String str_BlockingOtherPids = (String) o_im_blocking_other_pids;
						if (StringUtil.hasValue(str_BlockingOtherPids))
						{
							List<String> list_BlockingOtherPids = StringUtil.parseCommaStrToList(str_BlockingOtherPids);
							
							String blockedInfoStr = getBlockedPidInfo(newSample, pos_pid, list_BlockingOtherPids, pos_sqlText, pos_pidLockInfo);
							if (StringUtil.hasValue(blockedInfoStr))
							{
								newSample.setValueAt(blockedInfoStr, rowId, pos_blocked_pids_info);
								newSample.setValueAt(true,           rowId, pos_has_blocked_pids_info);
							}
						}
					}
				} // end: loop all rows
			} // end: BlockedSpidsInfo
		} // end: getBlockedPidInfo
		
	} // end: method

	/**
	 * 
	 */
	private String getBlockingListStr(CounterSample counters, int pid, int pos_blockingPid, int pos_pid)
	{
		Set<Integer> pidSet = null;

		// Loop on all rows
		int rows = counters.getRowCount();
		for (int rowId=0; rowId<rows; rowId++)
		{
			String str_blockingPid = counters.getValueAsString(rowId, pos_blockingPid);
			if (StringUtil.hasValue(str_blockingPid))
			{
				List<String> list_blockingPids = StringUtil.parseCommaStrToList(str_blockingPid, true);
				for (String str_blkPid : list_blockingPids)
				{
					int blkPid = StringUtil.parseInt(str_blkPid, -1);
					if (blkPid != -1)
					{
						if (blkPid == pid)
						{
							if (pidSet == null)
								pidSet = new LinkedHashSet<Integer>();

							pidSet.add( counters.getValueAsInteger(rowId, pos_pid) );
						}
					}
				}
			}
		}
		if (pidSet == null)
			return "";
		return StringUtil.toCommaStr(pidSet);
	}

	/** 
	 * Get a HTML Table with information about all PID's that are BLOCKED and what they were doing: SQLText and LockInfo 
	 */
	private String getBlockedPidInfo(CounterSample counters, int pos_pid, List<String> blockedPidList, int pos_sqlText, int pos_pidLockInfo)
	{
		if (blockedPidList == null)   return "";
		if (blockedPidList.isEmpty()) return "";

		if (pos_pid          < 0) return "";
		if (pos_sqlText      < 0) return "";
		if (pos_pidLockInfo  < 0) return "";


		StringBuilder sb = new StringBuilder(1024);

		sb.append("<TABLE BORDER=1>\n");
		sb.append("  <TR> <TH>Blocked PID</TH> <TH>Last Known SQL</TH> <TH>Lock Info</TH> </TR>\n");
		
		int addCount = 0;
		
		// Loop the blockedSpidList
		for (String blockedPidStr : blockedPidList)
		{
			long blockedPid = StringUtil.parseLong(blockedPidStr, Long.MIN_VALUE);
			if (blockedPid == Long.MIN_VALUE)
				continue;
			
			int rowId = getRowIdForPid(counters, blockedPid, pos_pid);

			if (rowId != -1)
			{
				addCount++;
				
				Object o_sqlText  = pos_sqlText     == -1 ? null : counters.getValueAt(rowId, pos_sqlText);
				Object o_lockInfo = pos_pidLockInfo == -1 ? null : counters.getValueAt(rowId, pos_pidLockInfo);
				
				String sqlText  = o_sqlText  == null ? "" : o_sqlText .toString();
				String lockInfo = o_lockInfo == null ? "" : o_lockInfo.toString();

				if (sqlText.startsWith("<html>") && sqlText.endsWith("</html>"))
				{
					sqlText = sqlText.substring("<html>".length());
					sqlText = sqlText.substring(0, sqlText.length() - "</html>".length());
				}

				if (lockInfo.startsWith("<html>") && lockInfo.endsWith("</html>"))
				{
					lockInfo = lockInfo.substring("<html>".length());
					lockInfo = lockInfo.substring(0, lockInfo.length() - "</html>".length());
				}

				sb.append("  <TR>\n");
				sb.append("    <TD><B>").append( blockedPid ).append("</B></TD>\n");
				sb.append("    <TD>"   ).append( sqlText ).append("</TD>\n");
				sb.append("    <TD>"   ).append( lockInfo   ).append("</TD>\n");
				sb.append("  </TR>\n");
			}
		}

		sb.append("</TABLE>\n");
		
		if (addCount == 0)
			return "";

		return sb.toString();
	}
	/** 
	 * Get ROWID for a specififc PID 
	 */
	private int getRowIdForPid(CounterSample counters, long pidToFind, int pos_pid)
	{
		// Loop on all diffData rows
		int rows = counters.getRowCount();
		for (int rowId=0; rowId < rows; rowId++)
		{
			Object o_pid = counters.getValueAt(rowId, pos_pid);
			if (o_pid instanceof Number)
			{
				Number pid = (Number)o_pid;
				if (pid.longValue() == pidToFind)
				{
					return rowId;
				}
			}
		}
		return -1;
	}
	
	
//	@Override
//	public void sendAlarmRequest()
//	{
//		AlarmHelper.sendAlarmRequestForColumn(this, "application_name");
//		AlarmHelper.sendAlarmRequestForColumn(this, "usename");
//	}
//	
//	@Override
//	public List<CmSettingsHelper> getLocalAlarmSettings()
//	{
//		List<CmSettingsHelper> list = new ArrayList<>();
//		
//		list.addAll( AlarmHelper.getLocalAlarmSettingsForColumn(this, "application_name") );
//		list.addAll( AlarmHelper.getLocalAlarmSettingsForColumn(this, "usename") );
//		
//		return list;
//	}

	@Override
	public void sendAlarmRequest()
	{
		AlarmHelper.sendAlarmRequestForColumn(this, "application_name");
		AlarmHelper.sendAlarmRequestForColumn(this, "usename");

		sendAlarmRequestLocal();
	}
	
	public void sendAlarmRequestLocal()
	{
		if ( ! hasDiffData() )
			return;
		
		if ( ! AlarmHandler.hasInstance() )
			return;
		
		// EXIT EARLY if no alarm properties has been specified (since there can be *many* logins)
		boolean isAnyAlarmEnabled = false;
		if (isSystemAlarmsForColumnEnabledAndInTimeRange("StatementExecInSec")) isAnyAlarmEnabled = true;
//		if (isSystemAlarmsForColumnEnabledAndInTimeRange("xxxxxxxxxxx"      )) isAnyAlarmEnabled = true;

		if (isAnyAlarmEnabled == false)
			return;

		boolean debugPrint = Configuration.getCombinedConfiguration().getBooleanProperty("sendAlarmRequest.debug", _logger.isDebugEnabled());

		AlarmHandler alarmHandler = AlarmHandler.getInstance();
		
		CountersModel cm = this;

		for (int r=0; r<cm.getDiffRowCount(); r++)
		{
//			//-------------------------------------------------------
//			// ImBlockingOthersMaxTimeInSec or HasBlockingLocks 
//			//-------------------------------------------------------
//			// TODO: implement this
//			// see: SqlServer CmActiveStatements.sendAlarmRequest()
//			if (isSystemAlarmsForColumnEnabledAndInTimeRange("ImBlockingOthersMaxTimeInSec"))
//			{
//				Object o_ImBlockedBySessionPids       = cm.getRateValue(r, "im_blocked_by_pids");
//				Object o_ImBlockingOthersMaxTimeInSec = cm.getRateValue(r, "im_blocking_others_max_time_in_sec");
//
//				if (o_ImBlockingOthersMaxTimeInSec != null && o_ImBlockingOthersMaxTimeInSec instanceof Number)
//				{
//					String ImBlockedBySessionPids       = o_ImBlockedBySessionPids + "";
//					int    ImBlockingOthersMaxTimeInSec = ((Number)o_ImBlockingOthersMaxTimeInSec).intValue();
//
//					int threshold = Configuration.getCombinedConfiguration().getIntProperty(PROPKEY_alarm_ImBlockingOthersMaxTimeInSec, DEFAULT_alarm_ImBlockingOthersMaxTimeInSec);
//
//					if (debugPrint || _logger.isDebugEnabled())
//						System.out.println("##### sendAlarmRequest("+cm.getName()+"): ImBlockedBySessionPids="+ImBlockedBySessionPids+"; ImBlockingOthersMaxTimeInSec='"+ImBlockingOthersMaxTimeInSec+"', threshold="+threshold+".");
//
//					if (StringUtil.isNullOrBlank(ImBlockedBySessionPids)) // meaning: THIS SPID is responsible for the block (it's not blocked, meaning; the root cause)
//					{
//						List<String> ImBlockingOtherSessionPidsList = StringUtil.commaStrToList(cm.getRateValue(r, "im_blocking_other_pids") + "");
//						String BlockingOtherPidsStr = ImBlockingOtherSessionPidsList + "";
//						int    blockCount           = ImBlockingOtherSessionPidsList.size();
//						long   pid                  = cm.getRateValueAsLong(r, "pid");
//
//						if (debugPrint || _logger.isDebugEnabled())
//							System.out.println("##### sendAlarmRequest("+cm.getName()+"): threshold="+threshold+", ImBlockingOthersMaxTimeInSec='"+ImBlockingOthersMaxTimeInSec+"', ImBlockingOtherSessionIdsList="+ImBlockingOtherSessionPidsList);
//
//						if (ImBlockingOthersMaxTimeInSec > threshold)
//						{
//							String extendedDescText = cm.toTextTableString(DATA_RATE, r);
//							String extendedDescHtml = cm.toHtmlTableString(DATA_RATE, r, true, false, false);
//
//							AlarmEvent ae = new AlarmEventBlockingLockAlarm(cm, threshold, pid, ImBlockingOthersMaxTimeInSec, BlockingOtherPidsStr, blockCount);
//
//							ae.setExtendedDescription(extendedDescText, extendedDescHtml);
//							
//							alarmHandler.addAlarm( ae );
//						}
//					}
//				}
//			}
			//-------------------------------------------------------
			// ImBlockingOthersMaxTimeInSec or HasBlockingLocks 
			//-------------------------------------------------------
			// Lets make it a bit simpler for now ('ImBlockingOthersMaxTimeInSec' is skipped and just focusing that I'm blocking)
			// But: When 'ImBlockingOthersMaxTimeInSec' is showing a *PROPER* value it should be used, so we don't alarm on "short" blocks 
			if (isSystemAlarmsForColumnEnabledAndInTimeRange("ImBlockingOthersMaxTimeInSec"))
			{
				String ImBlockedByPids              = cm.getRateString        (r, "im_blocked_by_pids");
				String ImBlockingOtherPids          = cm.getRateString        (r, "im_blocking_other_pids");
				int    ImBlockingOthersMaxTimeInSec = cm.getRateValueAsInteger(r, "im_blocking_others_max_time_in_sec", -1);

				// BlockingOthers AND isNotBlocked
				if (StringUtil.hasValue(ImBlockingOtherPids) && StringUtil.isNullOrBlank(ImBlockedByPids))
				{
					int threshold = Configuration.getCombinedConfiguration().getIntProperty(PROPKEY_alarm_ImBlockingOthersMaxTimeInSec, DEFAULT_alarm_ImBlockingOthersMaxTimeInSec);

					if (debugPrint || _logger.isDebugEnabled())
						System.out.println("##### sendAlarmRequest("+cm.getName()+"): ImBlockingOtherPids='" + ImBlockingOtherPids + "', ImBlockedByPids='" + ImBlockedByPids + "'; ImBlockingOthersMaxTimeInSec=" + ImBlockingOthersMaxTimeInSec + ", threshold=" + threshold + ".");

					List<String> ImBlockingOtherPidsList = StringUtil.commaStrToList(ImBlockingOtherPids);
					String BlockingOtherPidsStr = ImBlockingOtherPidsList + "";
					int    blockCount           = ImBlockingOtherPidsList.size();
					long   pid                  = cm.getRateValueAsLong(r, "pid");

					if (debugPrint || _logger.isDebugEnabled())
						System.out.println("##### sendAlarmRequest("+cm.getName()+"): threshold="+threshold+", ImBlockingOthersMaxTimeInSec='"+ImBlockingOthersMaxTimeInSec+"', ImBlockingOtherPidsList="+ImBlockingOtherPidsList);

					// If MaxTime is "unknown" or above the threshold (unknown could be: not available in this DBMS Version or we failed to fetch/calculate it)
					if (ImBlockingOthersMaxTimeInSec == -1 || ImBlockingOthersMaxTimeInSec > threshold)
					{
						String extendedDescText = cm.toTextTableString(DATA_RATE, r);
						String extendedDescHtml = cm.toHtmlTableString(DATA_RATE, r, true, false, false);

						AlarmEvent ae = new AlarmEventBlockingLockAlarm(cm, threshold, pid, ImBlockingOthersMaxTimeInSec, BlockingOtherPidsStr, blockCount);

						ae.setExtendedDescription(extendedDescText, extendedDescHtml);
						
						alarmHandler.addAlarm( ae );
					}
				}
			}

			
			//-------------------------------------------------------
			// HoldingXLocksWhileWaitForClientInputInSec 
			//-------------------------------------------------------
			if (isSystemAlarmsForColumnEnabledAndInTimeRange("HoldingXLocksWhileWaitForClientInputInSec"))
			{
				String state             = cm.getRateValue         (r, "state") + "";
				int    pid_exlock_count  = cm.getRateValueAsInteger(r, "pid_exlock_count" , -1);
				int    stmnt_start_sec   = cm.getRateValueAsInteger(r, "stmnt_start_sec"  , -1);

				if (pid_exlock_count > 0 && stmnt_start_sec > 0 && state.startsWith("idle in transaction"))
				{
					int threshold = Configuration.getCombinedConfiguration().getIntProperty(PROPKEY_alarm_HoldingXLocksWhileWaitForClientInputInSec, DEFAULT_alarm_HoldingXLocksWhileWaitForClientInputInSec);

					if (debugPrint || _logger.isDebugEnabled())
						System.out.println("##### sendAlarmRequest("+cm.getName()+"): threshold=" + threshold + ", stmnt_start_sec=" + stmnt_start_sec + ", pid_exlock_count=" + pid_exlock_count + ", state='" + state + "'.");

					if (stmnt_start_sec > threshold)
					{
						long   pid         = cm.getRateValueAsLong(r, "pid", -1L);
						String query_start = cm.getRateValue      (r, "query_start") + "";

						String extendedDescText = cm.toTextTableString(DATA_RATE, r);
						String extendedDescHtml = cm.toHtmlTableString(DATA_RATE, r, true, false, false);

						AlarmEvent ae = new AlarmEventHoldingLocksWhileWaitForClientInput(cm, threshold, pid, stmnt_start_sec, query_start, true);
						
						ae.setExtendedDescription(extendedDescText, extendedDescHtml);
						
						alarmHandler.addAlarm( ae );
					}
				}
			}

			//-------------------------------------------------------
			// HoldingAdvisoryLocksWhileWaitForClientInputInSec 
			//-------------------------------------------------------
			if (isSystemAlarmsForColumnEnabledAndInTimeRange("HoldingAdvisoryLocksWhileWaitForClientInputInSec"))
			{
				String state             = cm.getRateValue         (r, "state") + "";
				int    pid_advlock_count = cm.getRateValueAsInteger(r, "pid_advlock_count", -1);
				int    stmnt_start_sec   = cm.getRateValueAsInteger(r, "stmnt_start_sec"  , -1);

				if (pid_advlock_count > 0 && stmnt_start_sec > 0 && state.startsWith("idle in transaction"))
				{
					int threshold = Configuration.getCombinedConfiguration().getIntProperty(PROPKEY_alarm_HoldingAdvisoryLocksWhileWaitForClientInputInSec, DEFAULT_alarm_HoldingAdvisoryLocksWhileWaitForClientInputInSec);

					if (debugPrint || _logger.isDebugEnabled())
						System.out.println("##### sendAlarmRequest("+cm.getName()+"): threshold=" + threshold + ", stmnt_start_sec=" + stmnt_start_sec + ", pid_advlock_count=" + pid_advlock_count + ", state='" + state + "'.");

					if (stmnt_start_sec > threshold)
					{
						long   pid         = cm.getRateValueAsLong(r, "pid", -1L);
						String query_start = cm.getRateValue      (r, "query_start") + "";

						String extendedDescText = cm.toTextTableString(DATA_RATE, r);
						String extendedDescHtml = cm.toHtmlTableString(DATA_RATE, r, true, false, false);

						AlarmEvent ae = new AlarmEventHoldingLocksWhileWaitForClientInput(cm, threshold, pid, stmnt_start_sec, query_start, true);
						
						ae.setExtendedDescription(extendedDescText, extendedDescHtml);
						
						alarmHandler.addAlarm( ae );
					}
				}
			}

			//-------------------------------------------------------
			// StatementExecInSec 
			//-------------------------------------------------------
			if (isSystemAlarmsForColumnEnabledAndInTimeRange("StatementExecInSec"))
			{
				// Only continue is "state" is "active"
				String state = cm.getDiffValue(r, "state") + "";
				if ("active".equals(state))
				{
					int execTimeInMs = -1;

					// get 'execTimeInMs'
					Object o_execTimeInMs = cm.getDiffValue(r, "execTimeInMs");
					if (o_execTimeInMs != null && o_execTimeInMs instanceof Number)
					{
						execTimeInMs = ((Number)o_execTimeInMs).intValue();
					}
//					else
//					{
//						// ok, lets 
//						Object o_query_start = cm.getDiffValue(r, "query_start");
//						if (o_query_start != null && o_query_start instanceof Timestamp)
//						{
//							// Get approximate server time here (when we last refreshed this CM)
//							execTimeInMs = this.getTimestamp().getTime() - ((Timestamp)o_query_start).getTime();
//						}
//					}
					
					int StatementExecInSec = (int)execTimeInMs / 1000;
					
					int threshold = Configuration.getCombinedConfiguration().getIntProperty(PROPKEY_alarm_StatementExecInSec, DEFAULT_alarm_StatementExecInSec);

					if (debugPrint || _logger.isDebugEnabled())
						System.out.println("##### sendAlarmRequest("+cm.getName()+"): threshold="+threshold+", StatementExecInSec='"+StatementExecInSec+"'.");

					if (StatementExecInSec > threshold)
					{
						// Get config 'skip some known values'
						String skipDbnameRegExp      = Configuration.getCombinedConfiguration().getProperty(PROPKEY_alarm_StatementExecInSecSkipDbname,      DEFAULT_alarm_StatementExecInSecSkipDbname);
						String skipLoginRegExp       = Configuration.getCombinedConfiguration().getProperty(PROPKEY_alarm_StatementExecInSecSkipLogin,       DEFAULT_alarm_StatementExecInSecSkipLogin);
						String skipCmdRegExp         = Configuration.getCombinedConfiguration().getProperty(PROPKEY_alarm_StatementExecInSecSkipCmd,         DEFAULT_alarm_StatementExecInSecSkipCmd);
						String skipBackendTypeRegExp = Configuration.getCombinedConfiguration().getProperty(PROPKEY_alarm_StatementExecInSecSkipBackendType, DEFAULT_alarm_StatementExecInSecSkipBackendType);

						String StatementStartTime = cm.getDiffValue(r, "query_start")  + "";
						String DBName             = cm.getDiffValue(r, "datname")      + "";
						String Login              = cm.getDiffValue(r, "usename")      + "";
						String Command            = cm.getDiffValue(r, "query")        + "";
						String backend_type       = cm.getDiffValue(r, "backend_type") + "";
						
						// note: this must be set to true at start, otherwise all below rules will be disabled (it "stops" processing at first doAlarm==false)
						boolean doAlarm = true;

						// The below could have been done with nested if(!skipXxx), if(!skipYyy) doAlarm=true; 
						// Below is more readable, from a variable context point-of-view, but HARDER to understand
						doAlarm = (doAlarm && (StringUtil.isNullOrBlank(skipDbnameRegExp)      || ! DBName      .matches(skipCmdRegExp        ))); // NO match in the SKIP Cmd      regexp
						doAlarm = (doAlarm && (StringUtil.isNullOrBlank(skipLoginRegExp)       || ! Login       .matches(skipCmdRegExp        ))); // NO match in the SKIP Cmd      regexp
						doAlarm = (doAlarm && (StringUtil.isNullOrBlank(skipCmdRegExp)         || ! Command     .matches(skipCmdRegExp        ))); // NO match in the SKIP Cmd      regexp
						doAlarm = (doAlarm && (StringUtil.isNullOrBlank(skipBackendTypeRegExp) || ! backend_type.matches(skipBackendTypeRegExp))); // NO match in the SKIP TranName regexp

						// NO match in the SKIP regEx
						if (doAlarm)
						{
							String extendedDescText = cm.toTextTableString(DATA_RATE, r);
							String extendedDescHtml = cm.toHtmlTableString(DATA_RATE, r, true, false, false);
													
							// Get a small graph (from CmSummary) about the usage for the last hour
							CountersModel cmSummary = getCounterController().getCmByName(CmSummary.CM_NAME);
							extendedDescHtml += "<br><br>" + cmSummary.getGraphDataHistoryAsHtmlImage(CmSummary.GRAPH_NAME_OLDEST_COMBO_IN_SEC);

							// create the alarm
							AlarmEvent ae = new AlarmEventLongRunningStatement(cm, threshold, StatementExecInSec, StatementStartTime, DBName, Login, Command, backend_type);
							ae.setExtendedDescription(extendedDescText, extendedDescHtml);
						
							alarmHandler.addAlarm( ae );
						}
					} // end: above threshold
				} // end: is number
			} // end: StatementExecInSec

			//-------------------------------------------------------
			// OpenXactInSec 
			//-------------------------------------------------------
			if (isSystemAlarmsForColumnEnabledAndInTimeRange("OpenXactInSec"))
			{
				// Only continue is "state" is "active" or "idle in transaction"
				String state = cm.getDiffValue(r, "state") + "";
				if ("active".equals(state) || "idle in transaction".equals(state))
				{
					int xactTimeInSec = -1;

					// get 'xact_start_sec'
					Object o_xact_start_sec = cm.getDiffValue(r, "xact_start_sec");
					if (o_xact_start_sec != null && o_xact_start_sec instanceof Number)
					{
						xactTimeInSec = ((Number)o_xact_start_sec).intValue();
					}
					
					int threshold = Configuration.getCombinedConfiguration().getIntProperty(PROPKEY_alarm_OpenXactInSec, DEFAULT_alarm_OpenXactInSec);

					if (debugPrint || _logger.isDebugEnabled())
						System.out.println("##### sendAlarmRequest("+cm.getName()+"): threshold="+threshold+", xactTimeInSec='"+xactTimeInSec+"'.");

					if (xactTimeInSec > threshold)
					{
						// Get config 'skip some known values'
						String skipDbnameRegExp      = Configuration.getCombinedConfiguration().getProperty(PROPKEY_alarm_OpenXactInSecSkipDbname,      DEFAULT_alarm_OpenXactInSecSkipDbname);
						String skipLoginRegExp       = Configuration.getCombinedConfiguration().getProperty(PROPKEY_alarm_OpenXactInSecSkipLogin,       DEFAULT_alarm_OpenXactInSecSkipLogin);
						String skipCmdRegExp         = Configuration.getCombinedConfiguration().getProperty(PROPKEY_alarm_OpenXactInSecSkipCmd,         DEFAULT_alarm_OpenXactInSecSkipCmd);
						String skipBackendTypeRegExp = Configuration.getCombinedConfiguration().getProperty(PROPKEY_alarm_OpenXactInSecSkipBackendType, DEFAULT_alarm_OpenXactInSecSkipBackendType);

//						String StatementStartTime = cm.getDiffValue(r, "query_start")   + "";

						String DBName             = cm.getDiffValue(r, "datname")      + "";
						String Login              = cm.getDiffValue(r, "usename")      + "";
						String Command            = cm.getDiffValue(r, "query")        + "";
						String backend_type       = cm.getDiffValue(r, "backend_type") + "";
						
						// note: this must be set to true at start, otherwise all below rules will be disabled (it "stops" processing at first doAlarm==false)
						boolean doAlarm = true;

						// The below could have been done with nested if(!skipXxx), if(!skipYyy) doAlarm=true; 
						// Below is more readable, from a variable context point-of-view, but HARDER to understand
						doAlarm = (doAlarm && (StringUtil.isNullOrBlank(skipDbnameRegExp)      || ! DBName      .matches(skipCmdRegExp        ))); // NO match in the SKIP Cmd      regexp
						doAlarm = (doAlarm && (StringUtil.isNullOrBlank(skipLoginRegExp)       || ! Login       .matches(skipCmdRegExp        ))); // NO match in the SKIP Cmd      regexp
						doAlarm = (doAlarm && (StringUtil.isNullOrBlank(skipCmdRegExp)         || ! Command     .matches(skipCmdRegExp        ))); // NO match in the SKIP Cmd      regexp
						doAlarm = (doAlarm && (StringUtil.isNullOrBlank(skipBackendTypeRegExp) || ! backend_type.matches(skipBackendTypeRegExp))); // NO match in the SKIP backendType regexp

						// NO match in the SKIP regEx
						if (doAlarm)
						{
							String extendedDescText = cm.toTextTableString(DATA_RATE, r);
							String extendedDescHtml = cm.toHtmlTableString(DATA_RATE, r, true, false, false);

							// Get a small graph (from CmSummary) about the usage for the last hour
							CountersModel cmSummary = getCounterController().getCmByName(CmSummary.CM_NAME);
							extendedDescHtml += "<br><br>" + cmSummary.getGraphDataHistoryAsHtmlImage(CmSummary.GRAPH_NAME_OLDEST_COMBO_IN_SEC);

							// create the alarm
							AlarmEvent ae = new AlarmEventLongRunningTransaction(cm, threshold, DBName, xactTimeInSec, backend_type);
							ae.setExtendedDescription(extendedDescText, extendedDescHtml);
						
							alarmHandler.addAlarm( ae );
						}
					} // end: above threshold
				} // end: is number
			} // end: StatementExecInSec

		} // end: loop rows
	}

	public static final String  PROPKEY_alarm_ImBlockingOthersMaxTimeInSec      = CM_NAME + ".alarm.system.if.ImBlockingOthersMaxTimeInSec.gt";
	public static final int     DEFAULT_alarm_ImBlockingOthersMaxTimeInSec      = 60;


	public static final String  PROPKEY_alarm_HoldingXLocksWhileWaitForClientInputInSec  = CM_NAME + ".alarm.system.if.HoldingXLocksWhileWaitForClientInputInSec.gt";
	public static final int     DEFAULT_alarm_HoldingXLocksWhileWaitForClientInputInSec  = 600; // 10 minutes

	public static final String  PROPKEY_alarm_HoldingAdvisoryLocksWhileWaitForClientInputInSec  = CM_NAME + ".alarm.system.if.HoldingAdvisoryLocksWhileWaitForClientInputInSec.gt";
	public static final int     DEFAULT_alarm_HoldingAdvisoryLocksWhileWaitForClientInputInSec  = 1200; // 20 minutes


	public static final String  PROPKEY_alarm_StatementExecInSec                = CM_NAME + ".alarm.system.if.StatementExecInSec.gt";
	public static final int     DEFAULT_alarm_StatementExecInSec                = 3 * 60 * 60; // 3 Hours

	public static final String  PROPKEY_alarm_StatementExecInSecSkipDbname      = CM_NAME + ".alarm.system.if.StatementExecInSec.skip.dbname";
	public static final String  DEFAULT_alarm_StatementExecInSecSkipDbname      = "";

	public static final String  PROPKEY_alarm_StatementExecInSecSkipLogin       = CM_NAME + ".alarm.system.if.StatementExecInSec.skip.login";
	public static final String  DEFAULT_alarm_StatementExecInSecSkipLogin       = "";

	public static final String  PROPKEY_alarm_StatementExecInSecSkipCmd         = CM_NAME + ".alarm.system.if.StatementExecInSec.skip.cmd";
//	public static final String  DEFAULT_alarm_StatementExecInSecSkipCmd         = "^(BACKUP |RESTORE ).*";
	public static final String  DEFAULT_alarm_StatementExecInSecSkipCmd         = "";

	public static final String  PROPKEY_alarm_StatementExecInSecSkipBackendType = CM_NAME + ".alarm.system.if.StatementExecInSec.skip.backendType";
	public static final String  DEFAULT_alarm_StatementExecInSecSkipBackendType = "^(walsender)";


	public static final String  PROPKEY_alarm_OpenXactInSec                     = CM_NAME + ".alarm.system.if.OpenXactInSec.gt";
	public static final int     DEFAULT_alarm_OpenXactInSec                     = 60 * 60; // 1 hours

	public static final String  PROPKEY_alarm_OpenXactInSecSkipDbname           = CM_NAME + ".alarm.system.if.OpenXactInSec.skip.dbname";
	public static final String  DEFAULT_alarm_OpenXactInSecSkipDbname           = "";

	public static final String  PROPKEY_alarm_OpenXactInSecSkipLogin            = CM_NAME + ".alarm.system.if.OpenXactInSec.skip.login";
	public static final String  DEFAULT_alarm_OpenXactInSecSkipLogin            = "";

	public static final String  PROPKEY_alarm_OpenXactInSecSkipCmd              = CM_NAME + ".alarm.system.if.OpenXactInSec.skip.cmd";
//	public static final String  DEFAULT_alarm_OpenXactInSecSkipCmd              = "^(BACKUP |RESTORE ).*";
	public static final String  DEFAULT_alarm_OpenXactInSecSkipCmd              = "";

	public static final String  PROPKEY_alarm_OpenXactInSecSkipBackendType      = CM_NAME + ".alarm.system.if.OpenXactInSec.skip.backendType";
	public static final String  DEFAULT_alarm_OpenXactInSecSkipBackendType      = "^(walsender)"; 


	@Override
	public List<CmSettingsHelper> getLocalAlarmSettings()
	{
		Configuration conf = Configuration.getCombinedConfiguration();
		List<CmSettingsHelper> list = new ArrayList<>();

		CmSettingsHelper.Type isAlarmSwitch = CmSettingsHelper.Type.IS_ALARM_SWITCH;
		
		list.add(new CmSettingsHelper("ImBlockingOthersMaxTimeInSec",              isAlarmSwitch, PROPKEY_alarm_ImBlockingOthersMaxTimeInSec              , Integer.class, conf.getIntProperty(PROPKEY_alarm_ImBlockingOthersMaxTimeInSec              , DEFAULT_alarm_ImBlockingOthersMaxTimeInSec              ), DEFAULT_alarm_ImBlockingOthersMaxTimeInSec              , "If 'ImBlockingOthersMaxTimeInSec' is greater than ## then send 'AlarmEventBlockingLockAlarm'." ));

		list.add(new CmSettingsHelper("HoldingXLocksWhileWaitForClientInputInSec", isAlarmSwitch, PROPKEY_alarm_HoldingXLocksWhileWaitForClientInputInSec , Integer.class, conf.getIntProperty(PROPKEY_alarm_HoldingXLocksWhileWaitForClientInputInSec , DEFAULT_alarm_HoldingXLocksWhileWaitForClientInputInSec ), DEFAULT_alarm_HoldingXLocksWhileWaitForClientInputInSec , "If Client 'idle in transaction' and is Holding Exclusive Locks at DBMS and 'stmnt_start_sec' is greater than ## seconds then send 'AlarmEventHoldingLocksWhileWaitForClientInput'." ));
		list.add(new CmSettingsHelper("HoldingAdvisoryLocksWhileWaitForClientInputInSec", isAlarmSwitch, PROPKEY_alarm_HoldingAdvisoryLocksWhileWaitForClientInputInSec , Integer.class, conf.getIntProperty(PROPKEY_alarm_HoldingAdvisoryLocksWhileWaitForClientInputInSec , DEFAULT_alarm_HoldingAdvisoryLocksWhileWaitForClientInputInSec ), DEFAULT_alarm_HoldingAdvisoryLocksWhileWaitForClientInputInSec , "If Client 'idle in transaction' and is Holding Advisory Locks at DBMS and 'stmnt_start_sec' is greater than ## seconds then send 'AlarmEventHoldingLocksWhileWaitForClientInput'." ));

		list.add(new CmSettingsHelper("StatementExecInSec",                        isAlarmSwitch, PROPKEY_alarm_StatementExecInSec                        , Integer.class, conf.getIntProperty(PROPKEY_alarm_StatementExecInSec                        , DEFAULT_alarm_StatementExecInSec                        ), DEFAULT_alarm_StatementExecInSec                        , "If any SPID's has been executed a single SQL Statement for more than ## seconds, then send alarm 'AlarmEventLongRunningStatement'." ));
		list.add(new CmSettingsHelper("StatementExecInSec SkipDbs",                               PROPKEY_alarm_StatementExecInSecSkipDbname              , String .class, conf.getProperty   (PROPKEY_alarm_StatementExecInSecSkipDbname              , DEFAULT_alarm_StatementExecInSecSkipDbname              ), DEFAULT_alarm_StatementExecInSecSkipDbname              , "If 'StatementExecInSec' is true; Discard 'datname' listed (regexp is used)."      , new RegExpInputValidator()));
		list.add(new CmSettingsHelper("StatementExecInSec SkipLogins",                            PROPKEY_alarm_StatementExecInSecSkipLogin               , String .class, conf.getProperty   (PROPKEY_alarm_StatementExecInSecSkipLogin               , DEFAULT_alarm_StatementExecInSecSkipLogin               ), DEFAULT_alarm_StatementExecInSecSkipLogin               , "If 'StatementExecInSec' is true; Discard 'usename' listed (regexp is used)."      , new RegExpInputValidator()));
		list.add(new CmSettingsHelper("StatementExecInSec SkipCommands",                          PROPKEY_alarm_StatementExecInSecSkipCmd                 , String .class, conf.getProperty   (PROPKEY_alarm_StatementExecInSecSkipCmd                 , DEFAULT_alarm_StatementExecInSecSkipCmd                 ), DEFAULT_alarm_StatementExecInSecSkipCmd                 , "If 'StatementExecInSec' is true; Discard 'query' listed (regexp is used)."        , new RegExpInputValidator()));
		list.add(new CmSettingsHelper("StatementExecInSec SkipBackendType",                       PROPKEY_alarm_StatementExecInSecSkipBackendType         , String .class, conf.getProperty   (PROPKEY_alarm_StatementExecInSecSkipBackendType         , DEFAULT_alarm_StatementExecInSecSkipBackendType         ), DEFAULT_alarm_StatementExecInSecSkipBackendType         , "If 'StatementExecInSec' is true; Discard 'backend_type' listed (regexp is used)." , new RegExpInputValidator()));
                                                                                                                                                                                                                                                                                                                 
		list.add(new CmSettingsHelper("OpenXactInSec",                             isAlarmSwitch, PROPKEY_alarm_OpenXactInSec                             , Integer.class, conf.getIntProperty(PROPKEY_alarm_OpenXactInSec                             , DEFAULT_alarm_OpenXactInSec                             ), DEFAULT_alarm_OpenXactInSec                             , "If any SPID's has an Open Transaction for more than ## seconds, then send alarm 'AlarmEventLongRunningTransaction'." ));
		list.add(new CmSettingsHelper("OpenXactInSec SkipDbs",                                    PROPKEY_alarm_OpenXactInSecSkipDbname                   , String .class, conf.getProperty   (PROPKEY_alarm_OpenXactInSecSkipDbname                   , DEFAULT_alarm_OpenXactInSecSkipDbname                   ), DEFAULT_alarm_OpenXactInSecSkipDbname                   , "If 'StatementExecInSec' is true; Discard 'datname' listed (regexp is used)."      , new RegExpInputValidator()));
		list.add(new CmSettingsHelper("OpenXactInSec SkipLogins",                                 PROPKEY_alarm_OpenXactInSecSkipLogin                    , String .class, conf.getProperty   (PROPKEY_alarm_OpenXactInSecSkipLogin                    , DEFAULT_alarm_OpenXactInSecSkipLogin                    ), DEFAULT_alarm_OpenXactInSecSkipLogin                    , "If 'StatementExecInSec' is true; Discard 'usename' listed (regexp is used)."      , new RegExpInputValidator()));
		list.add(new CmSettingsHelper("OpenXactInSec SkipCommands",                               PROPKEY_alarm_OpenXactInSecSkipCmd                      , String .class, conf.getProperty   (PROPKEY_alarm_OpenXactInSecSkipCmd                      , DEFAULT_alarm_OpenXactInSecSkipCmd                      ), DEFAULT_alarm_OpenXactInSecSkipCmd                      , "If 'StatementExecInSec' is true; Discard 'query' listed (regexp is used)."        , new RegExpInputValidator()));
		list.add(new CmSettingsHelper("OpenXactInSec SkipBackendType",                            PROPKEY_alarm_OpenXactInSecSkipBackendType              , String .class, conf.getProperty   (PROPKEY_alarm_OpenXactInSecSkipBackendType              , DEFAULT_alarm_OpenXactInSecSkipBackendType              ), DEFAULT_alarm_OpenXactInSecSkipBackendType              , "If 'StatementExecInSec' is true; Discard 'backend_type' listed (regexp is used)." , new RegExpInputValidator()));

//TODO;
// // * add alarm: HoldingLocksWhileWaitForClientInput, using 'pid_exlock_count'; 
// // * increase the default for OpenXactInSec to 1h or 2h;
// // * In Alarm "CANCEL" add "Real/Actual/Effected Duration", which also calculates the "incubation" period of an alarm (or adds the "threshold value" for time sensitive alarms;
// // * Replication and LongRunningTransaction... investigate what "background" PID's we should "warn" on
//TODO;
// // * DDL Storage... check if it's a function (not in pg_class) and get/extract TEXT, parse and get/store tables accessed (like we do with views)
// // * DDL Storage... Check for Foreign Data Wrappers... DDL For table must reflect that
// // * Possibly add/use 'pg_stat_progress_cluster' && 'pg_stat_progress_vacuum' or pg_stat_progress_*
// // * look at extension: pg_buffercache
// // * integrate 'pg_stat_ssl' into 'CmPgActivity'
// // * maybe look at https://pgstats.dev/ to find more interesting things
// // 
//TODO SQL-Server;
// // SSMS had a Table Activity Report -- with Logical Reads ... where does it get that information from 
//TODO General;
// // DSR Charts -- Sort them in some way so that the "heaviest/largest" series is the first serie, etc... 
// // DSR Mail Subject -- Possibility to add '${description}' from the SERVER_LIST file... implemented as template tagname '${dbxCentralServerNameDescription}'
// // DbxCentral "buttons" -- add "template" so we can label the buttons with info from SERVER_LIST file... '${serverName} -- ${dbxCentralServerNameDescription}'

		list.addAll( AlarmHelper.getLocalAlarmSettingsForColumn(this, "application_name") );
		list.addAll( AlarmHelper.getLocalAlarmSettingsForColumn(this, "usename") );

		return list;
	}
}
