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
import java.util.LinkedList;
import java.util.List;

import com.asetune.ICounterController;
import com.asetune.IGuiController;
import com.asetune.cm.CounterSetTemplates;
import com.asetune.cm.CounterSetTemplates.Type;
import com.asetune.cm.CountersModel;
import com.asetune.cm.sqlserver.gui.CmSessionsPanel;
import com.asetune.gui.MainFrame;
import com.asetune.gui.TabularCntrPanel;
import com.asetune.utils.Ver;

public class CmSessions
extends CountersModel
{
//	private static Logger        _logger          = Logger.getLogger(CmExecProcedureStats.class);
	private static final long    serialVersionUID = 1L;

	public static final String   CM_NAME          = CmSessions.class.getSimpleName();
	public static final String   SHORT_NAME       = "Sessions";
	public static final String   HTML_DESC        = 
		"<html>"
		+ "<p>"
		+ "Shows one row per authenticated session (user connections), for internal tasks, see: XXX. <br>"
		+ "</p>"
		+ "</html>";

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
				ssu__s_user_objects_deferred_dealloc_page_count = "    ,tmp_user_objects_deferred_dealloc_mb = convert(numeric(12,1), (ssu.user_objects_deferred_dealloc_page_count) / 128.0)  -- SP ?? in 2012 & 2014 \n";

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
			    + "select \n"
			    + "     ec.session_id \n"
			    + "    ,worker_count = (select count(*) from sys.sysprocesses where spid = ec.session_id) - 1 \n"
			    + er__dop
			    + er__parallel_worker_count
			    + "    ,er.blocking_session_id --   -2 = The blocking resource is owned by an orphaned distributed transaction, -3 = The blocking resource is owned by a deferred recovery transaction, -4 = Session ID of the blocking latch owner could not be determined at this time because of internal latch state transitions. \n"
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

			    + "    ,es.status  -- Running, Sleeping, Dormant, Preconnect \n"
			    + "    ,exec_status     = er.status        --- can probably be found somewhere else -- Background, Running, Runnable, Sleeping, Suspended \n"
			    + "    ,er.command       -- \n"
			    + "    ,exec_start_time = er.start_time \n"
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
			    + "    ,last_request_exec_time = datediff(ms, es.last_request_start_time, es.last_request_end_time) -- fix case... if ms are to big \n"
			    + "    ,last_request_age_sec   = datediff(ms, es.last_request_end_time, getdate())                  -- fix case... if ms are to big \n"

			    + "    ,er.sql_handle \n"
			    + "    ,er.statement_start_offset \n"
			    + "    ,er.statement_end_offset \n"
			    + "    ,er.plan_handle \n"
			    + "    ,exec_open_transaction_count    = er.open_transaction_count \n"
			    + "    ,er.open_resultset_count \n"
			    + "    ,er.transaction_id \n"
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
			    + "    ,es.transaction_isolation_level   -- 0=Unspecified, 1=ReadUncommitted, 2=ReadCommitted, 3=RepeatableRead, 4=Serializable, 5=Snapshot \n"
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
			    + "    ,tmp_user_objects_now_mb          = convert(numeric(12,1), (ssu.user_objects_alloc_page_count - ssu.user_objects_dealloc_page_count) / 128.0) \n"
			    + "    ,tmp_user_objects_alloc_mb        = convert(numeric(12,1), (ssu.user_objects_alloc_page_count)       / 128.0) \n"
			    + "    ,tmp_user_objects_dealloc_mb      = convert(numeric(12,1), (ssu.user_objects_dealloc_page_count)     / 128.0) \n"
			    + "    ,tmp_internal_objects_now_mb      = convert(numeric(12,1), (ssu.internal_objects_alloc_page_count - ssu.internal_objects_dealloc_page_count) / 128.0) \n"
			    + "    ,tmp_internal_objects_alloc_mb    = convert(numeric(12,1), (ssu.internal_objects_alloc_page_count)   / 128.0) \n"
			    + "    ,tmp_internal_objects_dealloc_mb  = convert(numeric(12,1), (ssu.internal_objects_dealloc_page_count) / 128.0) \n"
			    + ssu__s_user_objects_deferred_dealloc_page_count
//			    + " \n"
			    + "from sys.dm_exec_connections ec \n"
			    + "inner join sys.dm_exec_sessions es on ec.session_id = es.session_id \n"
//			    + "inner join sys.sysprocesses     sp on ec.session_id = sp.spid \n"
			    + "left outer join sys.dm_exec_requests er on ec.session_id = er.session_id \n"
			    + "left outer join tempdb.sys.dm_db_session_space_usage ssu on ec.session_id = ssu.session_id \n"
			    + "";

		return sql;
	}
}
