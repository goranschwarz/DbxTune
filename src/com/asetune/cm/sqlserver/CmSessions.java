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
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
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
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

import org.apache.log4j.Logger;

import com.asetune.ICounterController;
import com.asetune.IGuiController;
import com.asetune.alarm.AlarmHandler;
import com.asetune.alarm.AlarmHelper;
import com.asetune.alarm.events.AlarmEvent;
import com.asetune.alarm.events.AlarmEventLongRunningStatement;
import com.asetune.cm.CmSettingsHelper;
import com.asetune.cm.CounterSample;
import com.asetune.cm.CounterSetTemplates;
import com.asetune.cm.CounterSetTemplates.Type;
import com.asetune.cm.CountersModel;
import com.asetune.cm.CmSettingsHelper.RegExpInputValidator;
import com.asetune.cm.sqlserver.gui.CmSessionsPanel;
import com.asetune.gui.MainFrame;
import com.asetune.gui.TabularCntrPanel;
import com.asetune.utils.Configuration;
import com.asetune.utils.StringUtil;
import com.asetune.utils.Ver;

public class CmSessions
extends CountersModel
{
	private static Logger        _logger          = Logger.getLogger(CmSessions.class);
	private static final long    serialVersionUID = 1L;

	public static final String   CM_NAME          = CmSessions.class.getSimpleName();
	public static final String   SHORT_NAME       = "Sessions";
	public static final String   HTML_DESC        = 
		"<html>" +
		"Shows one row per authenticated session (user connections), for internal tasks, see: XXX. <br>" +
		"<br><br>" +
		"Table Background colors:" +
		"<ul>" +
//		"    <li>YELLOW              - SPID is a System Processes</li>" +
		"    <li>GREEN               - SPID is Executing(running) or are in the Run Queue Awaiting a time slot to Execute (runnable)</li>" +
		"    <li>EXTREME_LIGHT_GREEN - SPID is currently (suspended) running a SQL Statement, Although it might be sleeping waiting for IO or something else.</li>" +
//		"    <li>LIGHT_GREEN         - SPID is Sending Network packets to it's client, soon it will probably go into running or runnable or finish.</li>" +
		"    <li>ORANGE              - SPID has an Open Transaction. Check column <code>transaction_name</code>, if it says <code>implicit_transaction</code>. The Client has autocommit = false, which bad and will <b>increase</b> chances of blocking locks.</li>" +
		"    <li>PINK                - SPID is Blocked by some other SPID that holds a Lock on a database object Table, Page or Row. This is the Lock Victim.</li>" +
		"    <li>RED                 - SPID is Blocking other SPID's from running, this SPID is Responsible or the Root Cause of a Blocking Lock.</li>" +
		"</ul>" +
		"</html>";

	public static final String   GROUP_NAME       = MainFrame.TCP_GROUP_SERVER;
	public static final String   GUI_ICON_FILE    = "images/"+CM_NAME+".png";

	public static final long     NEED_SRV_VERSION = 0;
	public static final long     NEED_CE_VERSION  = 0;

	public static final String[] MON_TABLES       = new String[] {"dm_exec_sessions"};
	public static final String[] NEED_ROLES       = new String[] {};//{"VIEW SERVER STATE"};
	public static final String[] NEED_CONFIG      = new String[] {};

	public static final String[] PCT_COLUMNS      = new String[] {};
	public static final String[] DIFF_COLUMNS     = new String[] {
			"net_bytes_received"
			,"net_bytes_sent"
			,"cpu_time"
//			,"memory_usage"
			,"total_scheduled_time"
			,"total_reads"
			,"total_writes"
			,"total_logical_reads"
			,"total_row_count"

			,"exec_cpu_time"
			,"exec_reads"
			,"exec_writes"
			,"exec_logical_reads"
			,"exec_row_count"
//			,"exec_granted_query_memory"
			};

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

	@Override
	protected TabularCntrPanel createGui()
	{
		return new CmSessionsPanel(this);
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

		pkCols.add("session_id");
//		pkCols.add("ecid");

		return pkCols;
	}

	@Override
	public String getSqlForVersion(Connection conn, long srvVersion, boolean isAzure)
	{
//		String dm_exec_sessions = "dm_exec_sessions";
//		
//		if (isAzure)
//			dm_exec_sessions = "dm_pdw_nodes_exec_sessions";
//
//		// datediff(ms, last_request_start_time, last_request_end_time)

		String es__last_unsuccessful_logon = "";
		String es__unsuccessful_logons     = "";
		String es__DBName                  = "";
		String es__database_id             = "";
		String es__AuthDbName              = "";
		String es__open_transaction_count  = "";
		String es__page_server_reads       = "";

		String er__statement_sql_handle    = "";
		String er__dop                     = "";
		String er__parallel_worker_count   = "";
		String er__page_resource           = "";
		
		String ssu__s_user_objects_deferred_dealloc_page_count = "";
		String ssu__s_user_objects_deferred_dealloc_page_count_CALC = "";

		if (srvVersion >= Ver.ver(2008)) es__last_unsuccessful_logon = "    ,es.last_unsuccessful_logon        -- 2008 \n";                    
		if (srvVersion >= Ver.ver(2008)) es__unsuccessful_logons     = "    ,es.unsuccessful_logons            -- 2008 \n";                    
		if (srvVersion >= Ver.ver(2012)) es__DBName                  = "    ,DBName = db_name(es.database_id)  -- 2012 \n";                    
		if (srvVersion >= Ver.ver(2012)) es__database_id             = "    ,es.database_id                    -- 2012 \n";                    
		if (srvVersion >= Ver.ver(2012)) es__AuthDbName              = "    ,AuthDbName = db_name(es.authenticating_database_id)     -- 2012  -- ID of the database authenticating the principal. For Logins, the value will be 0. For contained database users, the value will be the database ID of the contained database. \n";
		if (srvVersion >= Ver.ver(2012)) es__open_transaction_count  = "    ,es.open_transaction_count         -- 2012 \n";
		if (srvVersion >= Ver.ver(2019)) es__page_server_reads       = "    ,es.page_server_reads              -- Applies to: Azure SQL Database Hyperscale (but also available at 2019) \n";

		if (srvVersion >= Ver.ver(2014)) er__statement_sql_handle    = "    ,er.statement_sql_handle            -- 2014 \n";
		if (srvVersion >= Ver.ver(2016)) er__dop                     = "    ,er.dop                             -- 2016 \n";
		if (srvVersion >= Ver.ver(2016)) er__parallel_worker_count   = "    ,er.parallel_worker_count           -- 2016 \n";
		if (srvVersion >= Ver.ver(2019)) er__page_resource           = "    ,er.page_resource                   -- 2019 \n";

		// Just guessing here 2014 SP2 and 2012 SP3
		if (srvVersion >= Ver.ver(2014,0,0, 2) || (srvVersion < Ver.ver(2014) && srvVersion >= Ver.ver(2012,0,0, 3)) ) 
		{
			ssu__s_user_objects_deferred_dealloc_page_count_CALC = " - ssu.user_objects_deferred_dealloc_page_count";
			ssu__s_user_objects_deferred_dealloc_page_count = "    ,tmp_user_objects_deferred_dealloc_mb = convert(numeric(12,1), (ssu.user_objects_deferred_dealloc_page_count) / 128.0)  -- SP ?? in 2012 & 2014 \n";
		}

		//--------------------------------------------------------------------------
		//---- NOTE ---- NOTE ---- NOTE ---- NOTE ---- NOTE ---- NOTE ---- NOTE ----
		//--------------------------------------------------------------------------
		// Lets be careful down here...
		// Some DMV's only contains session_id's (all Parallel Workers are summed up) 
		// And Some DMV's contains session_id + execution_context_id/ecid (which are the actual Parallel Worker "threads")
		// So do NOT join with tables that contains BOTH the session_id + execution_context_id, then you will have duplicates on 'session_id', which is the PK for this Performance Counter
		//--------------------------------------------------------------------------
//		String sql = ""
//			    + "select \n"
//			    + "     ec.session_id \n"
//			    + "    ,worker_count = (select count(*) from sys.sysprocesses where spid = ec.session_id) - 1 \n"
//			    + "    ,ec.connect_time \n"
//			    + "    ,connect_time_sec = datediff(ss, ec.connect_time, getdate())  -- fix case... if seconds are to big \n"
//			    + "    ,ec.net_transport \n"
//			    + "    ,ec.protocol_type \n"
//			    + "    ,ec.encrypt_option \n"
//			    + "    ,ec.auth_scheme \n"
//			    + "    ,ec.node_affinity \n"
//			    + "    ,net_bytes_received = ec.num_reads                    -- DIFF CALC \n"
//			    + "    ,net_bytes_sent     = ec.num_writes                   -- DIFF CALC \n"
//			    + "    ,ec.last_read \n"
//			    + "    ,last_read_sec = datediff(ss, ec.last_read, getdate())  -- fix case... if seconds are to big \n"
//			    + "    ,ec.last_write \n"
//			    + "    ,last_write_sec = datediff(ss, ec.last_write, getdate())  -- fix case... if seconds are to big \n"
//			    + "    ,ec.net_packet_size \n"
//			    + "    ,ec.client_net_address \n"
//			    + "    ,ec.client_tcp_port \n"
//			    + "    ,ec.local_net_address \n"
//			    + "    ,ec.local_tcp_port \n"
//			    + "    ,ec.most_recent_sql_handle \n"
//			    + " \n"
//			    + "    ,es.host_name \n"
//			    + "    ,es.host_process_id \n"
//			    + "    ,es.program_name \n"
//			    + "    ,es.client_version \n"
//			    + "    ,es.client_interface_name \n"
//			    + "    ,es.login_name \n"
//			    + "    ,es.nt_domain \n"
//			    + "    ,es.nt_user_name \n"
//			    + "    ,es.status  -- Running, Sleeping, Dormant, Preconnect \n"
//			    + "    ,es.cpu_time                       -- DIFF CALC \n"
//			    + "    ,es.memory_usage                   -- DIFF CALC ?????? (or add another column so we can see both) \n"
//			    + "    ,es.total_scheduled_time           -- DIFF \n"
//			    + "--    ,es.total_elapsed_time \n"
//			    + "    ,es.last_request_start_time \n"
//			    + "    ,es.last_request_end_time \n"
//			    + "    ,last_request_exec_time = datediff(ms, es.last_request_start_time, es.last_request_end_time) -- fix case... if ms are to big \n"
//			    + "    ,last_request_age_sec   = datediff(ms, es.last_request_end_time, getdate())                  -- fix case... if ms are to big \n"
//			    + "    ,total_reads            = es.reads                          -- DIFF \n"
//			    + "    ,total_writes           = es.writes                         -- DIFF \n"
//			    + "    ,total_logical_reads    = es.logical_reads \n"
//			    + "    ,es.transaction_isolation_level   -- 0=Unspecified, 1=ReadUncommitted, 2=ReadCommitted, 3=RepeatableRead, 4=Serializable, 5=Snapshot \n"
//			    + "    ,es.lock_timeout \n"
//			    + "    ,es.deadlock_priority \n"
//			    + "    ,total_row_count        = es.row_count                      -- DIFF \n"
//			    + "    ,es.prev_error \n"
//			    + es__last_unsuccessful_logon
//			    + es__unsuccessful_logons
//			    + es__DBName
//			    + es__database_id
//			    + es__AuthDbName
//			    + es__open_transaction_count
//			    + es__page_server_reads
//			    + " \n"
//			    + "    ,exec_start_time = er.start_time \n"
//			    + "    ,er.request_id \n"
//			    + "    ,exec_status     = er.status        --- can probably be found somewhere else -- Background, Running, Runnable, Sleeping, Suspended \n"
//			    + "    ,er.command       -- \n"
//			    + "    ,er.sql_handle \n"
//			    + "    ,er.statement_start_offset \n"
//			    + "    ,er.statement_end_offset \n"
//			    + "    ,er.plan_handle \n"
//			    + "--    ,er.database_id  -- Can this be different than the \"others\"... ID of the database the request is executing against. Is not nullable. \n"
//			    + "    ,er.blocking_session_id --   -2 = The blocking resource is owned by an orphaned distributed transaction, -3 = The blocking resource is owned by a deferred recovery transaction, -4 = Session ID of the blocking latch owner could not be determined at this time because of internal latch state transitions. \n"
//			    + "    ,er.wait_type \n"
//			    + "    ,er.wait_time \n"
//			    + "    ,er.last_wait_type \n"
//			    + "    ,er.wait_resource \n"
//			    + "    ,exec_open_transaction_count    = er.open_transaction_count \n"
//			    + "    ,er.open_resultset_count \n"
//			    + "    ,er.transaction_id \n"
//			    + "    ,er.percent_complete \n"
//			    + "    ,er.estimated_completion_time \n"
//			    + "    ,exec_cpu_time           = er.cpu_time                        -- DIFF (is that the same as xxxx) \n"
//			    + "    ,exec_total_elapsed_time = er.total_elapsed_time \n"
//			    + "    ,er.scheduler_id \n"
//			    + "    ,exec_reads                = er.reads                           -- DIFF (is that the same as xxxx) \n"
//			    + "    ,exec_writes               = er.writes                          -- DIFF (is that the same as xxxx) \n"
//			    + "    ,exec_logical_reads        = er.logical_reads                   -- DIFF (is that the same as xxxx) \n"
//			    + "    ,exec_row_count            = er.row_count                       -- DIFF (is that the same as xxxx) \n"
//			    + "    ,exec_granted_query_memory = er.granted_query_memory \n"
//			    + "    ,er.executing_managed_code \n"
//			    + "    ,er.query_hash \n"
//			    + "    ,er.query_plan_hash \n"
//			    + er__statement_sql_handle
//			    + er__dop
//			    + er__parallel_worker_count
//			    + er__page_resource
//			    + " \n"
//			    + "	-- MOST OF THIS ARE PROBABLY DUPLICATES (physical_io might not be) \n"
//			    + "--    ,sp.ecid                     -- Execution Context ID, is the ID of any Parallel Worker(s) \n"
//			    + "--    ,sp.cpu \n"
//			    + "--    ,sp.physical_io \n"
//			    + "--    ,sp.memusage \n"
//			    + "--    ,sp.last_batch \n"
//			    + "--    ,sp.open_tran \n"
//			    + "--    ,sp.status                    -- does this has more statuses... spinloop, suspended \n"
//			    + "--    ,sp.hostname \n"
//			    + "--    ,sp.program_name \n"
//			    + "--    ,sp.hostprocess \n"
//			    + "--    ,sp.cmd \n"
//			    + "--    ,sp.loginame \n"
//			    + " \n"
//			    + "    ,s_user_objects_now_page_count          = ssu.user_objects_alloc_page_count - ssu.user_objects_dealloc_page_count \n"
//			    + "    ,s_user_objects_alloc_page_count        = ssu.user_objects_alloc_page_count \n"
//			    + "    ,s_user_objects_dealloc_page_count      = ssu.user_objects_dealloc_page_count \n"
//			    + "    ,s_internal_objects_now_page_count      = ssu.internal_objects_alloc_page_count - ssu.internal_objects_dealloc_page_count \n"
//			    + "    ,s_internal_objects_alloc_page_count    = ssu.internal_objects_alloc_page_count \n"
//			    + "    ,s_internal_objects_dealloc_page_count  = ssu.internal_objects_dealloc_page_count \n"
//			    + ssu__s_user_objects_deferred_dealloc_page_count
//			    + " \n"
////			    + "    ,t_user_objects_now_page_count          = tsu.user_objects_alloc_page_count - tsu.user_objects_dealloc_page_count \n"
////			    + "    ,t_user_objects_alloc_page_count        = tsu.user_objects_alloc_page_count \n"
////			    + "    ,t_user_objects_dealloc_page_count      = tsu.user_objects_dealloc_page_count \n"
////			    + "    ,t_internal_objects_now_page_count      = tsu.internal_objects_alloc_page_count - tsu.internal_objects_dealloc_page_count \n"
////			    + "    ,t_internal_objects_alloc_page_count    = tsu.internal_objects_alloc_page_count \n"
////			    + "    ,t_internal_objects_dealloc_page_count  = tsu.internal_objects_dealloc_page_count \n"
////			    + " \n"
//			    + "from sys.dm_exec_connections ec \n"
//			    + "inner join sys.dm_exec_sessions es on ec.session_id = es.session_id \n"
////			    + "inner join sys.sysprocesses     sp on ec.session_id = sp.spid \n"
//			    + "left outer join sys.dm_exec_requests er on ec.session_id = er.session_id \n"
//			    + "left outer join tempdb.sys.dm_db_session_space_usage ssu on ec.session_id = ssu.session_id \n"
////			    + "left outer join tempdb.sys.dm_db_task_space_usage    tsu on ec.session_id = tsu.session_id and tsu.execution_context_id = sp.ecid \n"
//			    + "";
		
		String sql = ""
			    + "-- When MARS is enabled we will have several records in dm_exec_connections, so this just summarizes all entries for: num_reads & num_writes \n"
			    + "-- see: https://sqljudo.wordpress.com/2014/03/07/cardinality-of-dm_exec_sessions-and-dm_exec_connections/ \n"
			    + ";WITH ec as ( \n"
			    + "	select \n"
			    + "           session_id             = max(session_id) \n"
			    + "          ,mars_count             = sum(CASE WHEN net_transport = 'Session' THEN 1 ELSE 0 END) \n"
			    + "          ,connect_time           = min(connect_time) \n"
			    + "          ,num_reads              = sum(num_reads) \n"
			    + "          ,num_writes             = sum(num_writes) \n"
			    + "          ,last_read              = max(last_read) \n"
			    + "          ,last_write             = max(last_write) \n"
			    + "          ,net_packet_size        = max(net_packet_size) \n"
			    + "          ,net_transport          = max(CASE WHEN net_transport = 'Session' THEN '' ELSE net_transport END) \n"
			    + "          ,protocol_type          = max(protocol_type) \n"
			    + "          ,encrypt_option         = max(encrypt_option) \n"
			    + "          ,auth_scheme            = max(auth_scheme) \n"
			    + "          ,node_affinity          = max(node_affinity) \n"
			    + "          ,client_net_address     = max(client_net_address) \n"
			    + "          ,client_tcp_port        = max(client_tcp_port) \n"
			    + "          ,local_net_address      = max(local_net_address) \n"
			    + "          ,local_tcp_port         = max(local_tcp_port) \n"
			    + "          ,most_recent_sql_handle = max(most_recent_sql_handle) \n"
			    + "	from sys.dm_exec_connections \n"
			    + "	group by session_id \n"
			    + ") \n"
			    + "---------------------------------------\n"
			    + "select \n"
			    + "     ec.session_id \n"
			    + "    ,worker_count = (select count(*) from sys.sysprocesses where spid = ec.session_id) - 1 \n"
			    + er__dop
			    + er__parallel_worker_count
			    + "    ,ec.mars_count \n"
			    + "    ,er.blocking_session_id --   -2 = The blocking resource is owned by an orphaned distributed transaction, -3 = The blocking resource is owned by a deferred recovery transaction, -4 = Session ID of the blocking latch owner could not be determined at this time because of internal latch state transitions. \n"
			    + "    ,BlockingOtherSpids=convert(varchar(512),'') \n"
			    + "    ,es.login_name \n"
			    + "    ,ec.connect_time \n"
			    + "    ,connect_time_sec = datediff(ss, ec.connect_time, getdate())  -- fix case... if seconds are to big \n"
			    + "    ,es.host_name \n"
			    + "    ,es.host_process_id \n"
			    + "    ,es.program_name \n"

			    + es__database_id
			    + es__DBName
			    + es__AuthDbName
			    + es__open_transaction_count
			    + "    ,tat_transaction_id = tat.transaction_id \n"
			    + "    ,transaction_type = CASE WHEN tat.transaction_type = 1 THEN 'Read/write transaction - 1' \n"
			    + "                             WHEN tat.transaction_type = 2 THEN 'Read-only transaction - 2' \n"
			    + "                             WHEN tat.transaction_type = 3 THEN 'System transaction - 3' \n"
			    + "                             WHEN tat.transaction_type = 4 THEN 'Distributed transaction - 4' \n"
			    + "                             ELSE 'UNKNOWN - ' + convert(varchar(10), tat.transaction_type) \n"
			    + "                        END \n"
			    + "    ,tat.name AS transaction_name \n"
			    + "    ,tat.transaction_begin_time \n"

			    + "    ,es.status  -- Running, Sleeping, Dormant, Preconnect \n"
			    + "    ,exec_status     = er.status        --- can probably be found somewhere else -- Background, Running, Runnable, Sleeping, Suspended \n"
			    + "    ,er.command       -- \n"
			    + "    ,exec_start_time = er.start_time \n"
			    + "    ,ExecTimeInMs    = CASE WHEN datediff(day, er.start_time, getdate()) >= 24 THEN -1 ELSE  datediff(ms, er.start_time, getdate()) END \n"
			    + "    ,es.cpu_time                       -- DIFF CALC \n"
			    + "    ,es.memory_usage                   -- DIFF CALC ?????? (or add another column so we can see both) \n"
			    + "    ,es.total_scheduled_time           -- DIFF \n"
			    
			    + "    ,er.wait_type \n"
			    + "    ,er.wait_time \n"
			    + "    ,er.last_wait_type \n"
			    + "    ,er.wait_resource \n"
			    + "    ,total_reads            = es.reads                          -- DIFF \n"
			    + "    ,total_writes           = es.writes                         -- DIFF \n"
			    + "    ,total_logical_reads    = es.logical_reads \n"
			    + "    ,total_row_count        = es.row_count                      -- DIFF \n"
			    
			    + "    ,es.last_request_start_time \n"
			    + "    ,es.last_request_end_time \n"
//CASE WHEN datediff(day, ST.StartTime, getdate()) >= 24 THEN -1 ELSE  datediff(ms, ST.StartTime, getdate()) END			    
			    + "    ,last_request_exec_time = CASE WHEN datediff(day, es.last_request_start_time, es.last_request_end_time) >= 24 THEN -1 ELSE datediff(ms, es.last_request_start_time, es.last_request_end_time) END \n"
//			    + "    ,last_request_exec_time = datediff(ms, es.last_request_start_time, es.last_request_end_time) -- fix case... if ms are to big \n"
			    + "    ,last_request_age_sec   = datediff(ss, es.last_request_end_time, getdate())                  -- fix case... if sec are to big \n"

			    + "    ,er.sql_handle \n"
			    + "    ,er.statement_start_offset \n"
			    + "    ,er.statement_end_offset \n"
			    + "    ,er.plan_handle \n"
			    + "    ,exec_open_transaction_count    = er.open_transaction_count \n"
			    + "    ,er.open_resultset_count \n"
			    + "    ,exec_transaction_id            = er.transaction_id \n"
			    + "    ,er.percent_complete \n"
			    + "    ,er.estimated_completion_time \n"
			    + "    ,exec_cpu_time           = er.cpu_time                        -- DIFF (is that the same as xxxx) \n"
			    + "    ,exec_total_elapsed_time = er.total_elapsed_time \n"
			    + "    ,er.scheduler_id \n"
			    + "    ,exec_reads                = er.reads                           -- DIFF (is that the same as xxxx) \n"
			    + "    ,exec_writes               = er.writes                          -- DIFF (is that the same as xxxx) \n"
			    + "    ,exec_logical_reads        = er.logical_reads                   -- DIFF (is that the same as xxxx) \n"
			    + "    ,exec_row_count            = er.row_count                       -- DIFF (is that the same as xxxx) \n"
			    + "    ,exec_granted_query_memory = er.granted_query_memory \n"

			    + "    ,net_bytes_received = ec.num_reads                    -- DIFF CALC \n"
			    + "    ,net_bytes_sent     = ec.num_writes                   -- DIFF CALC \n"
			    + "    ,ec.last_read \n"
			    + "    ,last_read_sec = datediff(ss, ec.last_read, getdate())  -- fix case... if seconds are to big \n"
			    + "    ,ec.last_write \n"
			    + "    ,last_write_sec = datediff(ss, ec.last_write, getdate())  -- fix case... if seconds are to big \n"
			    + "    ,ec.net_packet_size \n"
			    + "    ,ec.net_transport \n"
			    + "    ,ec.protocol_type \n"
			    + "    ,ec.encrypt_option \n"
			    + "    ,ec.auth_scheme \n"
			    + "    ,ec.node_affinity \n"
			    + "    ,ec.client_net_address \n"
			    + "    ,ec.client_tcp_port \n"
			    + "    ,ec.local_net_address \n"
			    + "    ,ec.local_tcp_port \n"
			    + "    ,ec.most_recent_sql_handle \n"
			    + " \n"
			    + "    ,es.client_version \n"
			    + "    ,es.client_interface_name \n"
			    + "    ,es.nt_domain \n"
			    + "    ,es.nt_user_name \n"
			    + "--    ,es.total_elapsed_time \n"
//			    + "    ,es.transaction_isolation_level   -- 0=Unspecified, 1=ReadUncommitted, 2=ReadCommitted, 3=RepeatableRead, 4=Serializable, 5=Snapshot \n"
				+ "    ,transaction_isolation_level = CASE WHEN es.transaction_isolation_level = 0 THEN 'Unspecified'     + ' - ' + CAST(es.transaction_isolation_level as varchar(10)) \n"
				+ "                                        WHEN es.transaction_isolation_level = 1 THEN 'ReadUncommitted' + ' - ' + CAST(es.transaction_isolation_level as varchar(10)) \n" 
				+ "                                        WHEN es.transaction_isolation_level = 2 THEN 'ReadCommitted'   + ' - ' + CAST(es.transaction_isolation_level as varchar(10)) \n"
				+ "                                        WHEN es.transaction_isolation_level = 3 THEN 'RepeatableRead'  + ' - ' + CAST(es.transaction_isolation_level as varchar(10)) \n"
				+ "                                        WHEN es.transaction_isolation_level = 4 THEN 'Serializable'    + ' - ' + CAST(es.transaction_isolation_level as varchar(10)) \n"
				+ "                                        WHEN es.transaction_isolation_level = 5 THEN 'Snapshot'        + ' - ' + CAST(es.transaction_isolation_level as varchar(10)) \n"
				+ "                                        ELSE                                         'UNKNOWN'         + ' - ' + CAST(es.transaction_isolation_level as varchar(10)) \n"
				+ "                                   END  \n"
			    + "    ,es.lock_timeout \n"
			    + "    ,es.deadlock_priority \n"
			    + "    ,es.prev_error \n"
			    + es__last_unsuccessful_logon
			    + es__unsuccessful_logons
			    + es__page_server_reads
			    + " \n"
			    + "    ,er.request_id \n"
			    + "--    ,er.database_id  -- Can this be different than the \"others\"... ID of the database the request is executing against. Is not nullable. \n"

			    + "    ,er.executing_managed_code \n"
			    + "    ,er.query_hash \n"
			    + "    ,er.query_plan_hash \n"
			    + er__statement_sql_handle
			    + er__page_resource
			    + " \n"
			    + "    ,tmp_total_used_mb                = convert(numeric(12,1), ((ssu.user_objects_alloc_page_count - ssu.user_objects_dealloc_page_count" + ssu__s_user_objects_deferred_dealloc_page_count_CALC + ") + (ssu.internal_objects_alloc_page_count - ssu.internal_objects_dealloc_page_count)) / 128.0) \n"
			    + "    ,tmp_user_objects_now_mb          = convert(numeric(12,1),  (ssu.user_objects_alloc_page_count - ssu.user_objects_dealloc_page_count" + ssu__s_user_objects_deferred_dealloc_page_count_CALC + ") / 128.0) \n"
			    + "    ,tmp_user_objects_alloc_mb        = convert(numeric(12,1),  (ssu.user_objects_alloc_page_count)       / 128.0) \n"
			    + "    ,tmp_user_objects_dealloc_mb      = convert(numeric(12,1),  (ssu.user_objects_dealloc_page_count)     / 128.0) \n"
			    + ssu__s_user_objects_deferred_dealloc_page_count
			    + "    ,tmp_internal_objects_now_mb      = convert(numeric(12,1), (ssu.internal_objects_alloc_page_count - ssu.internal_objects_dealloc_page_count) / 128.0) \n"
			    + "    ,tmp_internal_objects_alloc_mb    = convert(numeric(12,1), (ssu.internal_objects_alloc_page_count)   / 128.0) \n"
			    + "    ,tmp_internal_objects_dealloc_mb  = convert(numeric(12,1), (ssu.internal_objects_dealloc_page_count) / 128.0) \n"
//			    + " \n"
//			    + "FROM sys.dm_exec_connections ec \n"
			    + "FROM ec \n"
			    + "INNER JOIN sys.dm_exec_sessions es ON ec.session_id = es.session_id \n"
//			    + "INNER JOIN sys.sysprocesses     sp ON ec.session_id = sp.spid \n"
			    + "LEFT OUTER join sys.dm_exec_requests                  er ON ec.session_id      = er.session_id \n"
			    + "LEFT OUTER JOIN sys.dm_tran_session_transactions     tst ON es.session_id      = tst.session_id \n"
			    + "LEFT OUTER JOIN sys.dm_tran_active_transactions      tat ON tst.transaction_id = tat.transaction_id \n"
			    + "LEFT OUTER join tempdb.sys.dm_db_session_space_usage ssu ON ec.session_id      = ssu.session_id \n"
//			    + "WHERE ec.net_transport != 'Session' -- do not include MARS sessions, see: https://sqljudo.wordpress.com/2014/03/07/cardinality-of-dm_exec_sessions-and-dm_exec_connections/ \n"
			    + "";

		return sql;
	}

	
//	private HashMap<Number,Object> _blockingSpids = new HashMap<Number,Object>(); // <(SPID)Integer> <null> indicator that the SPID is BLOCKING some other SPID

//	/** 
//	 * Maintain the _blockingSpids Map, which is accessed from the Panel
//	 */
//	@Override
//	public void localCalculation(CounterSample prevSample, CounterSample newSample, CounterSample diffData)
//	{
//		// Where are various columns located in the Vector 
//		int pos_BlockingSPID = -1;
//		CounterSample counters = diffData;
//	
//		if (counters == null)
//			return;
//
//		// Reset the blockingSpids Map
//		_blockingSpids.clear();
//		
//		// put the pointer to the Map in the Client Property of the JTable, which should be visible for various places
//		if (getTabPanel() != null)
//			getTabPanel().putTableClientProperty("blockingSpidMap", _blockingSpids);
//
//		// Find column Id's
//		List<String> colNames = counters.getColNames();
//		if (colNames==null) 
//			return;
//
//		for (int colId=0; colId < colNames.size(); colId++) 
//		{
//			String colName = colNames.get(colId);
//			if (colName.equals("blocking_session_id"))  pos_BlockingSPID  = colId;
//
//			// Noo need to continue, we got all our columns
//			if (pos_BlockingSPID >= 0)
//				break;
//		}
//
//		if (pos_BlockingSPID < 0)
//		{
//			_logger.debug("Can't find the position for columns ('blocking_session_id'="+pos_BlockingSPID+")");
//			return;
//		}
//		
//		// Loop on all diffData rows
//		for (int rowId=0; rowId < counters.getRowCount(); rowId++) 
//		{
//			Object o_blockingSpid = counters.getValueAt(rowId, pos_BlockingSPID);
//
//			// Add any blocking SPIDs to the MAP
//fix-->>			// TODO: for offline recordings it's better to do it in the same way as for 'CmActiveStatements'
//			if (o_blockingSpid instanceof Number)
//			{
//				if (o_blockingSpid != null && ((Number)o_blockingSpid).intValue() != 0 )
//					_blockingSpids.put((Number)o_blockingSpid, null);
//			}
//		}
//	}
	
	@Override
	public void localCalculation(CounterSample newSample)
	{
		int pos_SPID               = newSample.findColumn("session_id");
		int pos_BlockingSPID       = newSample.findColumn("blocking_session_id");
		int pos_BlockingOtherSpids = newSample.findColumn("BlockingOtherSpids");
		
		// Loop on all diffData rows
		for (int rowId=0; rowId < newSample.getRowCount(); rowId++) 
		{
			Object o_SPID        = newSample.getValueAt(rowId, pos_SPID);
			
			if (o_SPID instanceof Number)
			{
				int spid = ((Number)o_SPID).intValue();

				// Get LIST of SPID's that I'm blocking
				String blockingList = getBlockingListStrForSpid(newSample, spid, pos_BlockingSPID, pos_SPID);

				newSample.setValueAt(blockingList, rowId, pos_BlockingOtherSpids);
			}

		}
	}

	private String getBlockingListStrForSpid(CounterSample counters, int spid, int pos_BlockingSPID, int pos_SPID)
	{
		StringBuilder sb = new StringBuilder();

		// Loop on all diffData rows
		int rows = counters.getRowCount();
		for (int rowId=0; rowId < rows; rowId++)
		{
			Object o_BlockingSPID = counters.getValueAt(rowId, pos_BlockingSPID);
			if (o_BlockingSPID instanceof Number)
			{
				Number BlockingSPID = (Number)o_BlockingSPID;
				if (BlockingSPID.intValue() == spid)
				{
					Object o_SPID = counters.getValueAt(rowId, pos_SPID);
					if (sb.length() == 0)
						sb.append(o_SPID);
					else
						sb.append(", ").append(o_SPID);
				}
			}
		}
		return sb.toString();
	}






	@Override
	public void sendAlarmRequest()
	{
		AlarmHelper.sendAlarmRequestForColumn(this, "program_name");
		AlarmHelper.sendAlarmRequestForColumn(this, "login_name");

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
			//-------------------------------------------------------
			// StatementExecInSec 
			// --->>> possibly move/copy this to CmActiveStatements
			//-------------------------------------------------------
			if (isSystemAlarmsForColumnEnabledAndInTimeRange("StatementExecInSec"))
			{
				Object o_ExecTimeInMs = cm.getDiffValue(r, "ExecTimeInMs");
				if (o_ExecTimeInMs != null && o_ExecTimeInMs instanceof Number)
				{
					int StatementExecInSec = ((Number)o_ExecTimeInMs).intValue() / 1000;
					
					int threshold = Configuration.getCombinedConfiguration().getIntProperty(PROPKEY_alarm_StatementExecInSec, DEFAULT_alarm_StatementExecInSec);

					if (debugPrint || _logger.isDebugEnabled())
						System.out.println("##### sendAlarmRequest("+cm.getName()+"): threshold="+threshold+", StatementExecInSec='"+StatementExecInSec+"'.");

					if (StatementExecInSec > threshold)
					{
						// Get config 'skip some known values'
						String skipDbnameRegExp   = Configuration.getCombinedConfiguration().getProperty(PROPKEY_alarm_StatementExecInSecSkipDbname,   DEFAULT_alarm_StatementExecInSecSkipDbname);
						String skipLoginRegExp    = Configuration.getCombinedConfiguration().getProperty(PROPKEY_alarm_StatementExecInSecSkipLogin,    DEFAULT_alarm_StatementExecInSecSkipLogin);
						String skipCmdRegExp      = Configuration.getCombinedConfiguration().getProperty(PROPKEY_alarm_StatementExecInSecSkipCmd,      DEFAULT_alarm_StatementExecInSecSkipCmd);
						String skipTranNameRegExp = Configuration.getCombinedConfiguration().getProperty(PROPKEY_alarm_StatementExecInSecSkipTranName, DEFAULT_alarm_StatementExecInSecSkipTranName);

						String StatementStartTime = cm.getDiffValue(r, "exec_start_time")  + "";
						String DBName             = cm.getDiffValue(r, "DBName")           + "";
						String Login              = cm.getDiffValue(r, "login_name")       + "";
						String Command            = cm.getDiffValue(r, "command")          + "";
						String tran_name          = cm.getDiffValue(r, "transaction_name") + "";
						
						// note: this must be set to true at start, otherwise all below rules will be disabled (it "stops" processing at first doAlarm==false)
						boolean doAlarm = true;

						// The below could have been done with nested if(!skipXxx), if(!skipYyy) doAlarm=true; 
						// Below is more readable, from a variable context point-of-view, but HARDER to understand
						doAlarm = (doAlarm && (StringUtil.isNullOrBlank(skipDbnameRegExp)   || ! DBName   .matches(skipCmdRegExp )));     // NO match in the SKIP Cmd      regexp
						doAlarm = (doAlarm && (StringUtil.isNullOrBlank(skipLoginRegExp)    || ! Login    .matches(skipCmdRegExp )));     // NO match in the SKIP Cmd      regexp
						doAlarm = (doAlarm && (StringUtil.isNullOrBlank(skipCmdRegExp)      || ! Command  .matches(skipCmdRegExp )));     // NO match in the SKIP Cmd      regexp
						doAlarm = (doAlarm && (StringUtil.isNullOrBlank(skipTranNameRegExp) || ! tran_name.matches(skipTranNameRegExp))); // NO match in the SKIP TranName regexp

						// NO match in the SKIP regEx
						if (doAlarm)
						{
							String extendedDescText = cm.toTextTableString(DATA_RATE, r);
							String extendedDescHtml = cm.toHtmlTableString(DATA_RATE, r, true, false, false);
													
							AlarmEvent ae = new AlarmEventLongRunningStatement(cm, threshold, StatementExecInSec, StatementStartTime, DBName, Login, Command, tran_name);
							ae.setExtendedDescription(extendedDescText, extendedDescHtml);
						
							alarmHandler.addAlarm( ae );
						}
					} // end: above threshold
				} // end: is number
			} // end: StatementExecInSec
		} // end: loop rows
	}

	public static final String  PROPKEY_alarm_StatementExecInSec             = CM_NAME + ".alarm.system.if.StatementExecInSec.gt";
	public static final int     DEFAULT_alarm_StatementExecInSec             = 3 * 60 * 60;

	public static final String  PROPKEY_alarm_StatementExecInSecSkipDbname   = CM_NAME + ".alarm.system.if.StatementExecInSec.skip.dbname";
	public static final String  DEFAULT_alarm_StatementExecInSecSkipDbname   = "";

	public static final String  PROPKEY_alarm_StatementExecInSecSkipLogin    = CM_NAME + ".alarm.system.if.StatementExecInSec.skip.login";
	public static final String  DEFAULT_alarm_StatementExecInSecSkipLogin    = "";

	public static final String  PROPKEY_alarm_StatementExecInSecSkipCmd      = CM_NAME + ".alarm.system.if.StatementExecInSec.skip.cmd";
	public static final String  DEFAULT_alarm_StatementExecInSecSkipCmd      = "^(BACKUP ).*";

	public static final String  PROPKEY_alarm_StatementExecInSecSkipTranName = CM_NAME + ".alarm.system.if.StatementExecInSec.skip.tranName";
	public static final String  DEFAULT_alarm_StatementExecInSecSkipTranName = "";
	

	@Override
	public List<CmSettingsHelper> getLocalAlarmSettings()
	{
		Configuration conf = Configuration.getCombinedConfiguration();
		List<CmSettingsHelper> list = new ArrayList<>();

		CmSettingsHelper.Type isAlarmSwitch = CmSettingsHelper.Type.IS_ALARM_SWITCH;
		
		list.add(new CmSettingsHelper("StatementExecInSec",           isAlarmSwitch, PROPKEY_alarm_StatementExecInSec            , Integer.class, conf.getIntProperty(PROPKEY_alarm_StatementExecInSec            , DEFAULT_alarm_StatementExecInSec            ), DEFAULT_alarm_StatementExecInSec            , "If any SPID's has been executed a single SQL Statement for more than ## seconds, then send alarm 'AlarmEventLongRunningStatement'." ));
		list.add(new CmSettingsHelper("StatementExecInSec SkipDbs",                  PROPKEY_alarm_StatementExecInSecSkipDbname  , String .class, conf.getProperty   (PROPKEY_alarm_StatementExecInSecSkipDbname  , DEFAULT_alarm_StatementExecInSecSkipDbname  ), DEFAULT_alarm_StatementExecInSecSkipDbname  , "If 'StatementExecInSec' is true; Discard databases listed (regexp is used).", new RegExpInputValidator()));
		list.add(new CmSettingsHelper("StatementExecInSec SkipLogins",               PROPKEY_alarm_StatementExecInSecSkipLogin   , String .class, conf.getProperty   (PROPKEY_alarm_StatementExecInSecSkipLogin   , DEFAULT_alarm_StatementExecInSecSkipLogin   ), DEFAULT_alarm_StatementExecInSecSkipLogin   , "If 'StatementExecInSec' is true; Discard Logins listed (regexp is used)."   , new RegExpInputValidator()));
		list.add(new CmSettingsHelper("StatementExecInSec SkipCommands",             PROPKEY_alarm_StatementExecInSecSkipCmd     , String .class, conf.getProperty   (PROPKEY_alarm_StatementExecInSecSkipCmd     , DEFAULT_alarm_StatementExecInSecSkipCmd     ), DEFAULT_alarm_StatementExecInSecSkipCmd     , "If 'StatementExecInSec' is true; Discard Commands listed (regexp is used)." , new RegExpInputValidator()));
		list.add(new CmSettingsHelper("StatementExecInSec SkipTranNames",            PROPKEY_alarm_StatementExecInSecSkipTranName, String .class, conf.getProperty   (PROPKEY_alarm_StatementExecInSecSkipTranName, DEFAULT_alarm_StatementExecInSecSkipTranName), DEFAULT_alarm_StatementExecInSecSkipTranName, "If 'StatementExecInSec' is true; Discard TranName listed (regexp is used)." , new RegExpInputValidator()));
		
		list.addAll( AlarmHelper.getLocalAlarmSettingsForColumn(this, "program_name") );
		list.addAll( AlarmHelper.getLocalAlarmSettingsForColumn(this, "login_name") );

		return list;
	}
}
