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
package com.dbxtune.cm.sqlserver;

import java.lang.invoke.MethodHandles;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.SQLWarning;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.TimeoutException;

import javax.naming.NameNotFoundException;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.dbxtune.CounterController;
import com.dbxtune.ICounterController;
import com.dbxtune.IGuiController;
import com.dbxtune.alarm.AlarmHandler;
import com.dbxtune.alarm.AlarmHelper;
import com.dbxtune.alarm.events.AlarmEvent;
import com.dbxtune.alarm.events.AlarmEventLongRunningStatement;
import com.dbxtune.alarm.events.sqlserver.AlarmEventDacInUse;
import com.dbxtune.alarm.events.sqlserver.AlarmEventDebugWaitInfo;
import com.dbxtune.cache.DbmsObjectIdCache;
import com.dbxtune.cache.DbmsObjectIdCache.ObjectInfo;
import com.dbxtune.cm.CmSettingsHelper;
import com.dbxtune.cm.CmSettingsHelper.RegExpInputValidator;
import com.dbxtune.cm.CounterSample;
import com.dbxtune.cm.CounterSetTemplates;
import com.dbxtune.cm.CounterSetTemplates.Type;
import com.dbxtune.cm.CountersModel;
import com.dbxtune.cm.sqlserver.gui.CmSessionsPanel;
import com.dbxtune.config.dict.MonTablesDictionary;
import com.dbxtune.config.dict.MonTablesDictionaryManager;
import com.dbxtune.gui.MainFrame;
import com.dbxtune.gui.ResultSetTableModel;
import com.dbxtune.gui.TabularCntrPanel;
import com.dbxtune.sql.conn.DbxConnection;
import com.dbxtune.sql.conn.info.DbmsVersionInfo;
import com.dbxtune.sql.conn.info.DbmsVersionInfoSqlServer;
import com.dbxtune.utils.Configuration;
import com.dbxtune.utils.NumberUtils;
import com.dbxtune.utils.StringUtil;
import com.dbxtune.utils.TimeUtils;
import com.dbxtune.utils.Ver;

public class CmSessions
extends CountersModel
{
	private static final Logger _logger = LogManager.getLogger(MethodHandles.lookup().lookupClass());
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

	public static final String[] MON_TABLES       = new String[] {
			"CmSessions"
			,"dm_exec_connections"
			,"dm_exec_sessions"
			,"dm_exec_requests"
			,"dm_tran_session_transactions"
			,"dm_tran_active_transactions"
			,"dm_db_session_space_usage"
			,"dm_exec_sql_text"
		};

	public static final String[] NEED_ROLES       = new String[] {"VIEW SERVER STATE"};
	public static final String[] NEED_CONFIG      = new String[] {};

	public static final String[] PCT_COLUMNS      = new String[] { "percent_complete" };
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

			,"tmp_user_objects_alloc_mb"
			,"tmp_user_objects_dealloc_mb"
			,"tmp_user_objects_deferred_dealloc_mb"
			,"tmp_internal_objects_alloc_mb"
			,"tmp_internal_objects_dealloc_mb"
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
	public void addMonTableDictForVersion(DbxConnection conn, DbmsVersionInfo versionInfo)
	{
		try 
		{
			MonTablesDictionary mtd = MonTablesDictionaryManager.getInstance();
			
			mtd.addTable("CmSessions", "This CM");
			
			mtd.addColumn("CmSessions",  "worker_count",                         "<html>Number of <b>active</i> worker threads that exists in sys.sysprocesses for this session.<br>"
			                                                                         + "<b>Formula</b>: sub-select that does: select count(*) from sys.sysprocesses where spid = ec.session_id"
			                                                                         + "</html>");
			mtd.addColumn("CmSessions",  "mars_count",                           "<html>Number of <i>sessions</i> <i>shares</i> a connection/session_id through MARS (Multiple Active Result Sets)<br>"
			                                                                         + "<i>Multiple Active Result Sets (MARS) is a feature that allows the execution of multiple batches on a single connection. <br>"
			                                                                         + "In previous versions (2005 or 2008), only one batch could be executed at a time against a single connection. <br>"
			                                                                         + "Executing multiple batches with MARS does not imply simultaneous execution of operations.</i>"
			                                                                         + "</html>");
			mtd.addColumn("CmSessions",  "BlockingOtherSpids",                   "<html>This session_id is blocking other session_id's. This is a list of session_id's that I'm blocking.</html>");
			mtd.addColumn("CmSessions",  "connect_time_sec",                     "<html>Number of seconds this session_id connected.</html>");
			mtd.addColumn("CmSessions",  "DBName",                               "<html>Name of the database</html>");
			mtd.addColumn("CmSessions",  "AuthDbName",                           "<html>What database we used to <i>autenticate</i> a login. Normally 'master', but if it's a <i>Database Contained Login/User</i> then this will be the database name used to authenticate.</html>");
			mtd.addColumn("CmSessions",  "tat_transaction_id",                   "<html>The 'transaction_id' column from table 'dm_tran_active_transactions'...<br> ID of the transaction at the instance level, not the database level. It is only unique across all databases within an instance but not unique across all server instances. </html>");
			mtd.addColumn("CmSessions",  "transaction_name",                     "<html>The 'name' column from table 'dm_tran_active_transactions'...<br> Transaction name. This is overwritten if the transaction is marked and the marked name replaces the transaction name. </html>");
			mtd.addColumn("CmSessions",  "exec_status",                          "<html>The 'status' column from table 'dm_exec_requests'...<br>  <p>Status of the request. This can be of the following:</p><ul class=\"unordered\"> <li><p>Background</p></li> <li><p>Running</p></li> <li><p>Runnable</p></li> <li><p>Sleeping</p></li> <li><p>Suspended</p></li></ul><p>Is not nullable.</html>");
			mtd.addColumn("CmSessions",  "ProcName",                             "<html>If it's a Stored Proc that is executing... this is the name of the proc.<br><b>Algorithm:</b> SQL Used: object_name(dest.objectid, dest.dbid)</html>");
			mtd.addColumn("CmSessions",  "StmntStart",                           "<html>If it's a Stored Proc: What's the <i>offset</i> in the procedure (not line number), but <i>character position</i> </html>");
			mtd.addColumn("CmSessions",  "exec_start_time",                      "<html>The 'start_time' column from table 'dm_exec_requests'...<br>Timestamp when the request arrived.</html>");
			mtd.addColumn("CmSessions",  "ExecTimeInMs",                         "<html>How long has this been executed for (in milliseconds)</html>");
			mtd.addColumn("CmSessions",  "total_reads",                          "<html>The 'reads' column from table 'dm_exec_sessions'...<br>Number of reads performed, by requests in this session, during this session.</html>");
			mtd.addColumn("CmSessions",  "total_writes",                         "<html>The 'writes' column from table 'dm_exec_sessions'...<br>Number of writes performed, by requests in this session, during this session.</html>");
			mtd.addColumn("CmSessions",  "total_logical_reads",                  "<html>The 'logical_reads' column from table 'dm_exec_sessions'...<br>Number of logical reads that have been performed on the session.</html>");
			mtd.addColumn("CmSessions",  "total_row_count",                      "<html>The 'row_count' column from table 'dm_exec_sessions'...<br>Number of rows returned on the session up to this point.</html>");
			mtd.addColumn("CmSessions",  "last_request_exec_time",               "<html>Number of milliseconds this statement has been executing. SQL: datediff(ms, es.last_request_start_time, es.last_request_end_time)</html>");
			mtd.addColumn("CmSessions",  "last_request_age_sec",                 "<html>How many seconds has passed since last execition for this session happened. SQL: datediff(ss, es.last_request_end_time, getdate()</html>");
			mtd.addColumn("CmSessions",  "exec_open_transaction_count",          "<html>The 'open_transaction_count' column from table 'dm_exec_requests'...<br>Number of transactions that are open for this request.</html>");
			mtd.addColumn("CmSessions",  "exec_transaction_id",                  "<html>The 'transaction_id' column from table 'dm_exec_requests'...<br>ID of the transaction in which this request executes</html>");
			mtd.addColumn("CmSessions",  "exec_cpu_time",                        "<html>The 'cpu_time' column from table 'dm_exec_requests'...<br>CPU time in milliseconds that is used by the request</html>");
			mtd.addColumn("CmSessions",  "exec_total_elapsed_time",              "<html>The 'total_elapsed_time' column from table 'dm_exec_requests'...<br>Total time elapsed in milliseconds since the request arrived</html>");
			mtd.addColumn("CmSessions",  "exec_reads",                           "<html>The 'reads' column from table 'dm_exec_requests'...<br>Number of reads performed by this request</html>");
			mtd.addColumn("CmSessions",  "exec_writes",                          "<html>The 'writes' column from table 'dm_exec_requests'...<br>Number of writes performed by this request</html>");
			mtd.addColumn("CmSessions",  "exec_logical_reads",                   "<html>The 'logical_reads' column from table 'dm_exec_requests'...<br>Number of logical reads that have been performed by the request</html>");
			mtd.addColumn("CmSessions",  "exec_row_count",                       "<html>The 'row_count' column from table 'dm_exec_requests'...<br>Number of rows that have been returned to the client by this request</html>");
			mtd.addColumn("CmSessions",  "exec_granted_query_memory",            "<html>The 'granted_query_memory' column from table 'dm_exec_requests'...<br>Number of pages allocated to the execution of a query on the request</html>");
			mtd.addColumn("CmSessions",  "net_bytes_received",                   "<html>The 'num_reads' column from table 'dm_exec_connections'...<br>Number of packet reads that have occurred over this connection</html>");
			mtd.addColumn("CmSessions",  "net_bytes_sent",                       "<html>The 'num_writes' column from table 'dm_exec_connections'...<br>Number of data packet writes that have occurred over this connection</html>");
			mtd.addColumn("CmSessions",  "last_read_sec",                        "<html>How many seconds ago did we <i>read</i> from this connection. SQL: datediff(ss, dm_exec_connections.last_read, getdate())</html>");
			mtd.addColumn("CmSessions",  "last_write_sec",                       "<html>How many seconds ago did we <i>write</i> to this connection. SQL: datediff(ss, dm_exec_connections.last_write, getdate())</html>");
			mtd.addColumn("CmSessions",  "endpoint_type",                        "<html>What type of connection is this."
			                                                                         + "<ul>"
			                                                                         + "  <li>DAC - Dedicated Admin Connection (TCP)</li>"
			                                                                         + "  <li>TSQL Local Machine (SHARED_MEMORY)</li>"
			                                                                         + "  <li>TSQL Named Pipes (NAMED_PIPES)</li>"
			                                                                         + "  <li>TSQL Default TCP (TCP)</li>"
			                                                                         + "  <li>TSQL Default VIA (Virtual Interface Adapter)</li>"
			                                                                         + "</ul>"
			                                                                         + "</html>");
			mtd.addColumn("CmSessions",  "tmp_total_used_mb",                    "<html>Total MB this session holds in 'tempdb'. <br><b>Algorithm</b>: 'tmp_user_objects_used_mb' + 'tmp_internal_objects_used_mb'</html>");
			mtd.addColumn("CmSessions",  "tmp_user_objects_used_mb",             "<html>MB in 'tempdb': Used now for <b>user objects</b> by this session. <br><b>Algorithm</b>: (user_objects_alloc_page_count - user_objects_dealloc_page_count - user_objects_deferred_dealloc_page_count) / 128 </html>");
			mtd.addColumn("CmSessions",  "tmp_user_objects_alloc_mb",            "<html>MB in 'tempdb': reserved or allocated for <b>user objects</b> by this session</html>");
			mtd.addColumn("CmSessions",  "tmp_user_objects_dealloc_mb",          "<html>MB in 'tempdb': deallocated and no longer reserved for <b>user objects</b> by this session </html>");
			mtd.addColumn("CmSessions",  "tmp_user_objects_deferred_dealloc_mb", "<html>MB in 'tempdb': which have been marked for deferred deallocation for <b>user objects</b></html>");
			mtd.addColumn("CmSessions",  "tmp_internal_objects_used_mb",         "<html>MB in 'tempdb': Used now for <b>internal objects</b> by this session. <br><b>Algorithm</b>: (internal_objects_alloc_page_count - internal_objects_dealloc_page_count) / 128 </html>");
			mtd.addColumn("CmSessions",  "tmp_internal_objects_alloc_mb",        "<html>MB in 'tempdb': reserved or allocated for <b>internal objects</b> by this session</html>");
			mtd.addColumn("CmSessions",  "tmp_internal_objects_dealloc_mb",      "<html>MB in 'tempdb': deallocated and no longer reserved for <b>internal objects</b> by this session</html>");
		}
		catch (NameNotFoundException e) 
		{
			_logger.warn("Problems in cm='" + CM_NAME + "', adding addMonTableDictForVersion. Caught: " + e); 
		//	System.out.println("Problems in cm='" + CM_NAME + "', adding addMonTableDictForVersion. Caught: " + e); 
		}
	}

	@Override
	public String[] getDependsOnConfigForVersion(DbxConnection conn, DbmsVersionInfo versionInfo)
	{
		return NEED_CONFIG;
	}

	@Override
	public List<String> getPkForVersion(DbxConnection conn, DbmsVersionInfo versionInfo)
	{
		List <String> pkCols = new LinkedList<String>();

		pkCols.add("session_id");
//		pkCols.add("ecid");

		return pkCols;
	}

	@Override
	public String getSqlForVersion(DbxConnection conn, DbmsVersionInfo versionInfo)
	{
		DbmsVersionInfoSqlServer ssVersionInfo = (DbmsVersionInfoSqlServer) versionInfo;
		long srvVersion = ssVersionInfo.getLongVersion();

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
//			ssu__s_user_objects_deferred_dealloc_page_count_CALC = " - ssu.user_objects_deferred_dealloc_page_count";
			ssu__s_user_objects_deferred_dealloc_page_count = "    ,tmp_user_objects_deferred_dealloc_mb = convert(numeric(12,1), (ssu.user_objects_deferred_dealloc_page_count) / 128.0)  -- SP ?? in 2012 & 2014 \n";
		}

		// Is 'context_info_str' enabled (if it causes any problem, it can be disabled)
		String contextInfoStr = "/*    ,context_info_str = replace(cast(er.context_info as varchar(128)),char(0),'') -- " + SqlServerCmUtils.HELPTEXT_howToEnable__context_info_str + " */ \n";
		if (SqlServerCmUtils.isContextInfoStrEnabled())
		{
			// Make the binary 'context_info' into a String
			contextInfoStr = "    ,context_info_str = replace(cast(er.context_info as varchar(128)),char(0),'') /* " + SqlServerCmUtils.HELPTEXT_howToDisable__context_info_str + " */ \n";
		}

		
		//--------------------------------------------------------------------------
		//---- NOTE ---- NOTE ---- NOTE ---- NOTE ---- NOTE ---- NOTE ---- NOTE ----
		//--------------------------------------------------------------------------
		// Lets be careful down here...
		// Some DMV's only contains session_id's (all Parallel Workers are summed up) 
		// And Some DMV's contains session_id + execution_context_id/ecid (which are the actual Parallel Worker "threads")
		// So do NOT join with tables that contains BOTH the session_id + execution_context_id, then you will have duplicates on 'session_id', which is the PK for this Performance Counter
		//--------------------------------------------------------------------------
		String sql = ""
			    + "-- When MARS is enabled we will have several records in dm_exec_connections, so this just summarizes all entries for: num_reads & num_writes \n"
			    + "-- see: https://sqljudo.wordpress.com/2014/03/07/cardinality-of-dm_exec_sessions-and-dm_exec_connections/ \n"
			    + ";WITH ec as ( \n"
			    + "	select /* ${cmCollectorName} */ \n"
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
			    + "select /* ${cmCollectorName} */ \n"
			    + "     ec.session_id \n"
			    + "    ,worker_count = (select count(*) from sys.sysprocesses where spid = ec.session_id) - 1 \n"
			    + er__dop
			    + er__parallel_worker_count
			    + "    ,ec.mars_count \n"
			    + "    ,er.blocking_session_id --   -2 = The blocking resource is owned by an orphaned distributed transaction, -3 = The blocking resource is owned by a deferred recovery transaction, -4 = Session ID of the blocking latch owner could not be determined at this time because of internal latch state transitions. \n"
			    + "    ,BlockingOtherSpids=convert(varchar(512),'') \n"
			    + "    ,es.login_name \n"
			    + "    ,ec.connect_time \n"
//			    + "    ,connect_time_sec = datediff(ss, ec.connect_time, getdate())  -- fix case... if seconds are to big \n"
			    + "    ,connect_time_sec = CASE WHEN datediff(day, ec.connect_time, getdate()) >= 3650 THEN -1 ELSE datediff(ss, ec.connect_time, getdate()) END \n"
			    + "    ,es.host_name \n"
			    + "    ,es.host_process_id \n"
			    + "    ,es.program_name \n"
			    +       contextInfoStr

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
			    + "    ,tran_age_ms     = CASE WHEN datediff(day, tat.transaction_begin_time, getdate()) >= 24 THEN -1 ELSE  datediff(ms, tat.transaction_begin_time, getdate()) END \n"

			    + "    ,es.status  -- Running, Sleeping, Dormant, Preconnect \n"
			    + "    ,exec_status     = er.status        --- can probably be found somewhere else -- Background, Running, Runnable, Sleeping, Suspended \n"
			    + "    ,er.command       -- \n"
				+ "    ,ProcName        = object_name(dest.objectid, dest.dbid) \n"
				+ "    ,StmntStart      = er.statement_start_offset \n"
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
			    + "    ,last_request_exec_time = CASE WHEN datediff(day, es.last_request_start_time, es.last_request_end_time) >= 24 THEN -1 ELSE datediff(ms, es.last_request_start_time, es.last_request_end_time) END \n"
//			    + "    ,last_request_age_sec   = datediff(ss, es.last_request_end_time, getdate())                  -- fix case... if sec are to big \n"
				+ "    ,last_request_age_sec   = CASE WHEN datediff(day, es.last_request_end_time, getdate()) >= 3650 THEN -1 ELSE datediff(ss, es.last_request_end_time, getdate()) END \n"
			    + "    ,er.sql_handle \n"
			    + "    ,er.statement_start_offset \n"
			    + "    ,er.statement_end_offset \n"
			    + "    ,er.plan_handle \n"
			    + "    ,exec_open_transaction_count    = er.open_transaction_count \n"
			    + "    ,er.open_resultset_count \n"
			    + "    ,exec_transaction_id            = er.transaction_id \n"
			    + "    ,er.percent_complete \n"
			    + "    ,est_completion_time_ms     = er.estimated_completion_time \n"
			    + "    ,est_completion_ts          = CASE WHEN er.estimated_completion_time <= 0 THEN NULL ELSE dateadd(second, er.estimated_completion_time/1000, getdate()) END \n"
			    + "    ,est_completion_time        = CASE WHEN er.estimated_completion_time <= 0 THEN NULL ELSE convert(time, dateadd(second, er.estimated_completion_time/1000, '2020-01-01')) END \n"
			    + "    ,exec_cpu_time              = er.cpu_time                        -- DIFF (is that the same as xxxx) \n"
			    + "    ,exec_total_elapsed_time_ms = er.total_elapsed_time \n"
			    + "    ,exec_total_elapsed_time    = CASE WHEN er.total_elapsed_time <= 0 THEN NULL ELSE convert(time, dateadd(second, er.total_elapsed_time/1000, '2020-01-01')) END \n"
			    + "    ,er.scheduler_id \n"
			    + "    ,exec_reads                 = er.reads                           -- DIFF (is that the same as xxxx) \n"
			    + "    ,exec_writes                = er.writes                          -- DIFF (is that the same as xxxx) \n"
			    + "    ,exec_logical_reads         = er.logical_reads                   -- DIFF (is that the same as xxxx) \n"
			    + "    ,exec_row_count             = er.row_count                       -- DIFF (is that the same as xxxx) \n"
			    + "    ,exec_granted_query_memory  = er.granted_query_memory \n"

			    + "    ,net_bytes_received = ec.num_reads                    -- DIFF CALC \n"
			    + "    ,net_bytes_sent     = ec.num_writes                   -- DIFF CALC \n"
			    + "    ,ec.last_read \n"
//			    + "    ,last_read_sec  = datediff(ss, ec.last_read, getdate())  -- fix case... if seconds are to big \n"
				+ "    ,last_read_sec  = CASE WHEN datediff(day, ec.last_read, getdate()) >= 3650 THEN -1 ELSE datediff(ss, ec.last_read, getdate()) END \n"
			    + "    ,ec.last_write \n"
//			    + "    ,last_write_sec = datediff(ss, ec.last_write, getdate())  -- fix case... if seconds are to big \n"
				+ "    ,last_write_sec  = CASE WHEN datediff(day, ec.last_write, getdate()) >= 3650 THEN -1 ELSE datediff(ss, ec.last_write, getdate()) END \n"
			    + "    ,ec.net_packet_size \n"
			    + "    ,endpoint_type = CASE WHEN es.endpoint_id = 1 THEN 'DAC - Dedicated Admin Connection (TCP) - 1' \n"
			    + "                          WHEN es.endpoint_id = 2 THEN 'TSQL Local Machine (SHARED_MEMORY) - 2' \n"
			    + "                          WHEN es.endpoint_id = 3 THEN 'TSQL Named Pipes (NAMED_PIPES) - 3' \n"
			    + "                          WHEN es.endpoint_id = 4 THEN 'TSQL Default TCP (TCP) - 4' \n"
			    + "                          WHEN es.endpoint_id = 5 THEN 'TSQL Default VIA (Virtual Interface Adapter) - 5' \n"
			    + "                          ELSE                         'unknown - ' + cast(es.endpoint_id as varchar(5)) \n"
			    + "                     END \n"
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
			    + "    ,tmp_total_used_mb                    = convert(numeric(12,1), NULL) /* NOTE: all 'tmp_*' are maintained in Java code */ \n"
			    + "    ,tmp_user_objects_used_mb             = convert(numeric(12,1), NULL) \n"
			    + "    ,tmp_internal_objects_used_mb         = convert(numeric(12,1), NULL) \n"
			    + " \n"
			    + "    ,tmp_user_objects_alloc_mb            = convert(numeric(12,1), NULL) \n"
			    + "    ,tmp_user_objects_dealloc_mb          = convert(numeric(12,1), NULL) \n"
				+ "    ,tmp_user_objects_deferred_dealloc_mb = convert(numeric(12,1), NULL) /* only maintained from: SP ?? in 2012 & 2014*/ \n"
			    + " \n"
			    + "    ,tmp_internal_objects_alloc_mb        = convert(numeric(12,1), NULL) \n"
			    + "    ,tmp_internal_objects_dealloc_mb      = convert(numeric(12,1), NULL) \n"
//			    + " \n"
//			    + "FROM sys.dm_exec_connections ec \n"
			    + "FROM ec \n"
			    + "INNER JOIN sys.dm_exec_sessions es ON ec.session_id = es.session_id \n"
//			    + "INNER JOIN sys.sysprocesses     sp ON ec.session_id = sp.spid \n"
			    + "LEFT OUTER join sys.dm_exec_requests                  er ON ec.session_id      = er.session_id \n"
			    + "LEFT OUTER JOIN sys.dm_tran_session_transactions     tst ON es.session_id      = tst.session_id \n"
			    + "LEFT OUTER JOIN sys.dm_tran_active_transactions      tat ON tst.transaction_id = tat.transaction_id \n"
//			    + "LEFT OUTER join tempdb.sys.dm_db_session_space_usage ssu ON ec.session_id      = ssu.session_id \n"
				+ "OUTER APPLY sys.dm_exec_sql_text(er.sql_handle) dest \n"
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
		// make: column 'program_name' with value "SQLAgent - TSQL JobStep (Job 0x38AAD6888E5C5E408DE573B0A25EE970 : Step 1)"
		// into:                                  "SQLAgent - TSQL JobStep (Job '<name-of-the-job>' : Step 1 '<name-of-the-step>')
		SqlServerCmUtils.localCalculation_resolveSqlAgentProgramName(newSample);


		boolean getTempdbSpidUsage = Configuration.getCombinedConfiguration().getBooleanProperty(CmSummary.PROPKEY_sample_tempdbSpidUsage, CmSummary.DEFAULT_sample_tempdbSpidUsage);

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
				
				// Maintain 'tmp_*' columns
				if (getTempdbSpidUsage)
				{
					// 1: Get tempdb info about 'spid', and if we have a value:
					//    2: Set values for columns
					//       - tmp_total_used_mb
					//       - tmp_user_objects_used_mb
					//       - tmp_internal_objects_used_mb
					//
					//       - tmp_user_objects_alloc_mb
					//       - tmp_user_objects_dealloc_mb
					//       - tmp_user_objects_deferred_dealloc_mb
					//
					//       - tmp_internal_objects_alloc_mb
					//       - tmp_internal_objects_dealloc_mb
					//    (if the above columns can't be found... Simply write a message to the error log)
					//
					TempdbUsagePerSpid.TempDbSpaceInfo spaceInfo = TempdbUsagePerSpid.getInstance().getEntryForSpid(spid);
					if (spaceInfo != null)
					{
						newSample.setValueAt(spaceInfo.getTotalSpaceUsedInMb()               , rowId, "tmp_total_used_mb");
						newSample.setValueAt(spaceInfo.getUserObjectSpaceUsedInMb()          , rowId, "tmp_user_objects_used_mb");
						newSample.setValueAt(spaceInfo.getInternalObjectSpaceUsedInMb()      , rowId, "tmp_internal_objects_used_mb");

						newSample.setValueAt(spaceInfo.get_user_objects_alloc_mb()           , rowId, "tmp_user_objects_alloc_mb");
						newSample.setValueAt(spaceInfo.get_user_objects_dealloc_mb()         , rowId, "tmp_user_objects_dealloc_mb");
						newSample.setValueAt(spaceInfo.get_user_objects_deferred_dealloc_mb(), rowId, "tmp_user_objects_deferred_dealloc_mb");

						newSample.setValueAt(spaceInfo.get_internal_objects_alloc_mb()       , rowId, "tmp_internal_objects_alloc_mb");
						newSample.setValueAt(spaceInfo.get_internal_objects_dealloc_mb()     , rowId, "tmp_internal_objects_dealloc_mb");
					}
				}
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

		// Map to store all unique values in "wait_resources"
//		Map<String, List<SpidWaitInfo>> waitResourceMap = null;
		WaitInfo waitInfo = null;

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

					boolean isValidRow = true;

					// Discard some "known long running statements"
					// SQL in CmActiveStatements: "  AND (der.executing_managed_code = 0 OR (der.executing_managed_code = 1 AND der.wait_type != 'SLEEP_TASK')) \n" + // SSIS seems to be executing ALL THE TIME... so discard some of them... (this may be unique to MaxM)
					Object o_executing_managed_code = cm.getDiffValue(r, "executing_managed_code");
					Object o_wait_type              = cm.getDiffValue(r, "wait_type");
					Object o_last_wait_type         = cm.getDiffValue(r, "last_wait_type");
				//	Object o_DBName                 = cm.getDiffValue(r, "DBName"); // should we remove anything with "SSISDB" ???
					if (o_executing_managed_code != null)
					{
						if (o_executing_managed_code instanceof Boolean)
						{
							boolean executing_managed_code = (Boolean) o_executing_managed_code;
							String  wait_type              = "" + o_wait_type;
							String  last_wait_type         = "" + o_last_wait_type;

							if (executing_managed_code && ("SLEEP_TASK".equals(wait_type) || "SLEEP_TASK".equals(last_wait_type)) )
							{
								if (debugPrint || _logger.isDebugEnabled())
									System.out.println("##### sendAlarmRequest("+cm.getName()+"): SKIPPING record: executing_managed_code="+executing_managed_code+", wait_type='"+wait_type+"', last_wait_type='"+last_wait_type+"'.");
								isValidRow = false;
							}
						}
					}

					if (o_wait_type != null)
					{
						String wait_type = o_wait_type.toString();
						if (StringUtil.hasValue(wait_type))
						{
							Object o_wait_resource = cm.getDiffValue(r, "wait_resource");

							if (o_wait_resource != null && StringUtil.hasValue(o_wait_resource.toString()))
							{
								if (waitInfo == null)
									waitInfo = new WaitInfo(this);
								
								Object o_session_id    = cm.getDiffValue(r, "session_id");
								Object o_wait_time     = cm.getDiffValue(r, "wait_time");

								String wait_resource = o_wait_resource.toString();
								int    session_id    = (o_session_id instanceof Number) ? ((Number)o_session_id).intValue() : -1;
								int    wait_time     = (o_wait_time  instanceof Number) ? ((Number)o_wait_time ).intValue() : -1;
								
								waitInfo.add(session_id, wait_type, wait_time, wait_resource);
							}
						}
					}
					
					if (isValidRow && StatementExecInSec > threshold)
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
						doAlarm = (doAlarm && (StringUtil.isNullOrBlank(skipDbnameRegExp)   || ! DBName   .matches(skipDbnameRegExp  ))); // NO match in the SKIP Cmd      regexp
						doAlarm = (doAlarm && (StringUtil.isNullOrBlank(skipLoginRegExp)    || ! Login    .matches(skipLoginRegExp   ))); // NO match in the SKIP Cmd      regexp
						doAlarm = (doAlarm && (StringUtil.isNullOrBlank(skipCmdRegExp)      || ! Command  .matches(skipCmdRegExp     ))); // NO match in the SKIP Cmd      regexp
						doAlarm = (doAlarm && (StringUtil.isNullOrBlank(skipTranNameRegExp) || ! tran_name.matches(skipTranNameRegExp))); // NO match in the SKIP TranName regexp

						// NOTE: DBCC SHRINKDATABASE/SHRINKFILE tends to "change" database name from "the db to shrink" to "tempdb" A LOT
						// So can we do anything about that ... 
						//    - if we simply doAlarm=false, if it's in "tempdb" then alarm will still be cancelled... (which we don't want)  
						//    - if we Change the DBName to "-shrink-" then it will be hard to "track" in what database we have a long running transaction
						//    - we could check what active alarms we have and grab the DBName from there (but if first Alarm is in tempdb... then what...)
						//    - or just DISABLE this alarm for "DbccFilesCompact" ::: if (doAlarm && "DbccFilesCompact".equals(Command)) { doAlarm = false; }
						//    - or we just have to live with "flickering" between RAISE/CANCEL
						// For now: last option: live with it...
							

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

			//-------------------------------------------------------
			// DacInUse 
			//-------------------------------------------------------
			if (isSystemAlarmsForColumnEnabledAndInTimeRange("DacInUse"))
			{
				Object o_endpoint_type = cm.getAbsValue(r, "endpoint_type");
				
				if (o_endpoint_type != null && o_endpoint_type instanceof String)
				{
					String endpoint_type = (String) o_endpoint_type;
					
					boolean threshold = Configuration.getCombinedConfiguration().getBooleanProperty(PROPKEY_alarm_DacInUse, DEFAULT_alarm_DacInUse);

					if (debugPrint || _logger.isDebugEnabled())
						System.out.println("##### sendAlarmRequest("+cm.getName()+"): threshold="+threshold+", endpoint_type='"+endpoint_type+"'.");

					if (endpoint_type.startsWith("DAC") && threshold == true)
					{
						String extendedDescText = cm.toTextTableString(DATA_RATE, r);
						String extendedDescHtml = cm.toHtmlTableString(DATA_RATE, r, true, false, false);

						String spid      = "" + cm.getAbsValue(r, "session_id");
						String fromIp    = "" + cm.getAbsValue(r, "client_net_address");
						String fromHost  = "" + cm.getAbsValue(r, "host_name");
						String asLogin   = "" + cm.getAbsValue(r, "login_name");
						String loginTime = "" + cm.getAbsValue(r, "connect_time");
						String inDBName  = "" + cm.getAbsValue(r, "DBName");

						AlarmEvent ae = new AlarmEventDacInUse(cm, spid, fromIp, fromHost, asLogin, loginTime, inDBName);
						ae.setExtendedDescription(extendedDescText, extendedDescHtml);
					
						alarmHandler.addAlarm( ae );
					}
				}
			}
		} // end: loop rows
		
		// check waitResourceMap
		if (waitInfo != null)
		{
			waitInfo.checkAndSendAlarm();
		}
	}

	public static final String  PROPKEY_alarm_StatementExecInSec             = CM_NAME + ".alarm.system.if.StatementExecInSec.gt";
	public static final int     DEFAULT_alarm_StatementExecInSec             = 3 * 60 * 60; // 3 Hours

	public static final String  PROPKEY_alarm_StatementExecInSecSkipDbname   = CM_NAME + ".alarm.system.if.StatementExecInSec.skip.dbname";
	public static final String  DEFAULT_alarm_StatementExecInSecSkipDbname   = "";

	public static final String  PROPKEY_alarm_StatementExecInSecSkipLogin    = CM_NAME + ".alarm.system.if.StatementExecInSec.skip.login";
	public static final String  DEFAULT_alarm_StatementExecInSecSkipLogin    = "";

	public static final String  PROPKEY_alarm_StatementExecInSecSkipCmd      = CM_NAME + ".alarm.system.if.StatementExecInSec.skip.cmd";
	public static final String  DEFAULT_alarm_StatementExecInSecSkipCmd      = "^(BACKUP |RESTORE ).*";

	public static final String  PROPKEY_alarm_StatementExecInSecSkipTranName = CM_NAME + ".alarm.system.if.StatementExecInSec.skip.tranName";
	public static final String  DEFAULT_alarm_StatementExecInSecSkipTranName = "";
	
	public static final String  PROPKEY_alarm_DacInUse                       = CM_NAME + ".alarm.system.if.EndpointType.is.DAC";
	public static final boolean DEFAULT_alarm_DacInUse                       = true;
	
	public static final String  PROPKEY_alarm_WaitInfo                       = CM_NAME + ".alarm.system.if.WaitInfo";
	public static final boolean DEFAULT_alarm_WaitInfo                       = false;

	public static final String  PROPKEY_alarm_WaitInfo_maxCount_gt           = CM_NAME + ".alarm.system.if.WaitInfo.maxCount.gt";
	public static final int     DEFAULT_alarm_WaitInfo_maxCount_gt           = 2;


	
	@Override
	public List<CmSettingsHelper> getLocalAlarmSettings()
	{
		Configuration conf = Configuration.getCombinedConfiguration();
		List<CmSettingsHelper> list = new ArrayList<>();

		CmSettingsHelper.Type isAlarmSwitch = CmSettingsHelper.Type.IS_ALARM_SWITCH;
		
		list.add(new CmSettingsHelper("StatementExecInSec",           isAlarmSwitch, PROPKEY_alarm_StatementExecInSec            , Integer.class, conf.getIntProperty    (PROPKEY_alarm_StatementExecInSec            , DEFAULT_alarm_StatementExecInSec            ), DEFAULT_alarm_StatementExecInSec            , "If any SPID's has been executed a single SQL Statement for more than ## seconds, then send alarm 'AlarmEventLongRunningStatement'." ));
		list.add(new CmSettingsHelper("StatementExecInSec SkipDbs",                  PROPKEY_alarm_StatementExecInSecSkipDbname  , String .class, conf.getProperty       (PROPKEY_alarm_StatementExecInSecSkipDbname  , DEFAULT_alarm_StatementExecInSecSkipDbname  ), DEFAULT_alarm_StatementExecInSecSkipDbname  , "If 'StatementExecInSec' is true; Discard databases listed (regexp is used).", new RegExpInputValidator()));
		list.add(new CmSettingsHelper("StatementExecInSec SkipLogins",               PROPKEY_alarm_StatementExecInSecSkipLogin   , String .class, conf.getProperty       (PROPKEY_alarm_StatementExecInSecSkipLogin   , DEFAULT_alarm_StatementExecInSecSkipLogin   ), DEFAULT_alarm_StatementExecInSecSkipLogin   , "If 'StatementExecInSec' is true; Discard Logins listed (regexp is used)."   , new RegExpInputValidator()));
		list.add(new CmSettingsHelper("StatementExecInSec SkipCommands",             PROPKEY_alarm_StatementExecInSecSkipCmd     , String .class, conf.getProperty       (PROPKEY_alarm_StatementExecInSecSkipCmd     , DEFAULT_alarm_StatementExecInSecSkipCmd     ), DEFAULT_alarm_StatementExecInSecSkipCmd     , "If 'StatementExecInSec' is true; Discard Commands listed (regexp is used)." , new RegExpInputValidator()));
		list.add(new CmSettingsHelper("StatementExecInSec SkipTranNames",            PROPKEY_alarm_StatementExecInSecSkipTranName, String .class, conf.getProperty       (PROPKEY_alarm_StatementExecInSecSkipTranName, DEFAULT_alarm_StatementExecInSecSkipTranName), DEFAULT_alarm_StatementExecInSecSkipTranName, "If 'StatementExecInSec' is true; Discard TranName listed (regexp is used)." , new RegExpInputValidator()));
		list.add(new CmSettingsHelper("DacInUse",                     isAlarmSwitch, PROPKEY_alarm_DacInUse                      , Boolean.class, conf.getBooleanProperty(PROPKEY_alarm_DacInUse                      , DEFAULT_alarm_DacInUse                      ), DEFAULT_alarm_DacInUse                      , "If any user is connected via the DAC (Dedicated Admin Connection), then send alarm 'AlarmEventDacInUse'." ));
		list.add(new CmSettingsHelper("WaitInfo",                     isAlarmSwitch, PROPKEY_alarm_WaitInfo                      , Boolean.class, conf.getBooleanProperty(PROPKEY_alarm_WaitInfo                      , DEFAULT_alarm_WaitInfo                      ), DEFAULT_alarm_WaitInfo                      , "If any users has 'WaitInfo', then send alarm 'AlarmEventDebugWaitInfo' for debugging purposes." ));
		list.add(new CmSettingsHelper("WaitInfo MaxCount",                           PROPKEY_alarm_WaitInfo_maxCount_gt          , Integer.class, conf.getIntProperty    (PROPKEY_alarm_WaitInfo_maxCount_gt          , DEFAULT_alarm_WaitInfo_maxCount_gt          ), DEFAULT_alarm_WaitInfo_maxCount_gt          , "If 'WaitInfo' is enabled 'maxCount' must be above ##." ));

		list.addAll( AlarmHelper.getLocalAlarmSettingsForColumn(this, "program_name") );
		list.addAll( AlarmHelper.getLocalAlarmSettingsForColumn(this, "login_name") );

		return list;
	}




	// -----------------------------------------------------------------------------------------------------
	// TODO: search for: last-page insert PAGELATCH_EX contention in SQL Server
	//       https://docs.microsoft.com/en-US/troubleshoot/sql/performance/resolve-pagelatch-ex-contention
	//       https://techcommunity.microsoft.com/t5/sql-server-blog/pagelatch-ex-waits-and-heavy-inserts/ba-p/384289
	// how to implement
	//   - loop all rows
	//   - look for 'wait_type' of 'PAGELATCH_EX'
	//     -->> Add records to a list, which we can check *after* the loop of all rows
	//          key: 'wait_resource' -- value: session_id, wait_type, wait_time, wait_resource
	//   - check after loop
	//     if we got "many" threads (say 5) that are waiting on the same resource, and that the SUM wait_time is above "some value", then:
	//        * get more information about the page in the "wait_resource" (by dbcc page or similar)
	//        * Send some ALARM on it
	// POSIBLY:
	//   - The above will also work for "tempdb contention" PFS, GAM and SGAM pages.
	// -----------------------------------------------------------------------------------------------------

	
	// -----------------------------------------------------------------------------------------------------
	// TODO: decode some WaitResource: PAGE and KEY
	// see -- https://littlekendra.com/2016/10/17/decoding-key-and-page-waitresource-for-deadlocks-and-blocking/
	// -----------------------------------------------------------------------------------------------------
	//    CmSessions: ================= WaitInfo ====================== 
	//    CmSessions: WaitResources(5): [PAGE: 6:1:573542, KEY: 6:72057594044153856 (3713510338dc), KEY: 6:72057594044153856 (a360e767ed88), PAGE: 6:1:592565, PAGE: 6:1:541678] 
	//    CmSessions: WaitTypes    (3): [LCK_M_U, LCK_M_IX, LCK_M_S] 
	//    CmSessions: 
	//    CmSessions: Wait Resources -- Count: 
	//    CmSessions:   - count=4 -- WaitResource='PAGE: 6:1:573542' 
	//    CmSessions:   - count=1 -- WaitResource='KEY: 6:72057594044153856 (3713510338dc)' 
	//    CmSessions:   - count=2 -- WaitResource='KEY: 6:72057594044153856 (a360e767ed88)' 
	//    CmSessions:   - count=2 -- WaitResource='PAGE: 6:1:592565' 
	//    CmSessions:   - count=1 -- WaitResource='PAGE: 6:1:541678' 
	//    CmSessions: 
	//    CmSessions: Wait Resources -- Sum WaitTime: 
	//    CmSessions:   - waitTime=146398 ms -- WaitResource='PAGE: 6:1:573542' 
	//    CmSessions:   - waitTime=78312 ms -- WaitResource='KEY: 6:72057594044153856 (3713510338dc)' 
	//    CmSessions:   - waitTime=81128 ms -- WaitResource='KEY: 6:72057594044153856 (a360e767ed88)' 
	//    CmSessions:   - waitTime=146508 ms -- WaitResource='PAGE: 6:1:592565' 
	//    CmSessions:   - waitTime=61677 ms -- WaitResource='PAGE: 6:1:541678' 
	//    CmSessions: 
	//    CmSessions: Wait Types -- Sum WaitTime: 
	//    CmSessions:   - waitTime=81128 ms -- WaitType='LCK_M_U' 
	//    CmSessions:   - waitTime=132778 ms -- WaitType='LCK_M_IX' 
	//    CmSessions:   - waitTime=300117 ms -- WaitType='LCK_M_S' 
	//    CmSessions: ================================================= 
    //    
	//    --To decode: WaitResource='PAGE: <dbid:fileid:page>
	//    ----------------------------------------------------------------
	//    -- In 2019 or later: sys.dm_db_page_info ( DatabaseId, FileId, PageId, Mode )  -- https://learn.microsoft.com/en-us/sql/relational-databases/system-dynamic-management-views/sys-dm-db-page-info-transact-sql?view=sql-server-ver16
	//    -- SELECT sys.dm_db_page_info (#, #, #, 'DETAILED')
	//    ----------------------------------------------------------------
	//    -- DBCC PAGE (DatabaseName, FileNumber, PageNumber, DumpStyle)
	//    -- DBCC PAGE ('dbname', #, #, 2)
	//    ----------------------------------------------------------------
	//    also get the records
	//    SELECT sys.fn_PhysLocFormatter (%%physloc%%), *
	//    FROM dbo.tabname (NOLOCK)
	//    WHERE sys.fn_PhysLocFormatter (%%physloc%%) like '(<dbid>:<page>%'
	//
    //    
	//    ----------------------------------------------------------------
	//    --To decode: WaitResource='KEY: <dbid:hobt (Magic hash)>
	//    ----------------------------------------------------------------
	//    SELECT 
	//        sc.name as schema_name, 
	//        so.name as object_name, 
	//        si.name as index_name
	//    FROM sys.partitions AS p
	//    JOIN sys.objects AS so ON p.object_id  = so.object_id
	//    JOIN sys.indexes AS si ON p.index_id   = si.index_id  AND p.object_id = si.object_id
	//    JOIN sys.schemas AS sc ON so.schema_id = sc.schema_id
	//    WHERE hobt_id = 72057594044153856 ;
	//    GO
    //    
	//    SELECT * FROM meeting_details (NOLOCK) WHERE %%lockres%% = CAST('(3713510338dc)' AS varchar(60)) COLLATE Latin1_General_100_CI_AS_KS_WS_SC;
	//    SELECT * FROM meeting_details (NOLOCK) WHERE %%lockres%% = CAST('(a360e767ed88)' AS varchar(60)) COLLATE Latin1_General_100_CI_AS_KS_WS_SC;
	// -----------------------------------------------------------------------------------------------------
	
	
	
	private static class SpidWaitInfo
	{
		int    _session_id;
		String _wait_type;
		int    _wait_time;
		String _wait_resource;

		String _dbname;
		String _schemaName;
		String _objectName;
		String _indexName;
		String _pageTypeDesc;
		String _extraInfo;
		
		public SpidWaitInfo(int session_id, String wait_type, int wait_time, String wait_resource)
		{
			_session_id    = session_id;   
			_wait_type     = wait_type;    
			_wait_time     = wait_time;    
			_wait_resource = wait_resource;
		}
		@Override
		public String toString()
		{
			return "session_id=" + _session_id + ", wait_type='" + _wait_type + "', wait_time=" + _wait_time + ", wait_resource='" + _wait_resource + "'.";
		}
	}
	

	private static class WaitInfo
	{
		private CountersModel _cm;
		private Map<String, List<SpidWaitInfo>> _waitResourceMap = new HashMap<>();
		private Map<String, Integer>            _waitResourceTime = new HashMap<>();

		private Map<String, List<SpidWaitInfo>> _waitTypeMap     = new HashMap<>();
		private Map<String, Integer>            _waitTypeTime    = new HashMap<>();

		public WaitInfo(CountersModel cm)
		{
			_cm = cm;
		}
		
		public void add(int session_id, String wait_type, int wait_time, String wait_resource)
		{
			SpidWaitInfo wi = new SpidWaitInfo(session_id, wait_type, wait_time, wait_resource);

			// Add: _waitResourceMap
			List<SpidWaitInfo> list = _waitResourceMap.get(wait_resource);
			if (list == null)
			{
				list = new ArrayList<>();
				_waitResourceMap.put(wait_resource, list);
			}
			list.add(wi);

			// Add: _waitResourceTime
			Integer currentTime = _waitResourceTime.get(wait_resource);
			if (currentTime == null)
				currentTime = 0;
			_waitResourceTime.put(wait_resource, currentTime + wait_time);
			
			

			// Add: _waitTypeMap
			list = _waitResourceMap.get(wait_type);
			if (list == null)
			{
				list = new ArrayList<>();
				_waitTypeMap.put(wait_type, list);
			}
			list.add(wi);

			// Add: _waitResourceTime
			currentTime = _waitTypeTime.get(wait_type);
			if (currentTime == null)
				currentTime = 0;
			_waitTypeTime.put(wait_type, currentTime + wait_time);
			

			// Decode the 'wait_resource'
			if (wait_resource.startsWith("KEY: "))   // example='KEY: 6:72057594044153856 (7b4f7e19e103)' -- Database_Id, HOBT_Id ( Magic hash that you can decode with %%lockres%% if you really want)
			{
				// decode: https://littlekendra.com/2016/10/17/decoding-key-and-page-waitresource-for-deadlocks-and-blocking/
				//         in this way we can get: dbname, objectname & the row/values for the resource
				
				if (DbmsObjectIdCache.hasInstance())
				{
					DbmsObjectIdCache cache = DbmsObjectIdCache.getInstance();
					
					String tmp = wait_resource.substring("KEY: ".length()); // Strip off "KEY: "
					int firstPos = tmp.indexOf('(');
					if (firstPos > 0)
					{
						String lockres = tmp.substring(firstPos).trim(); // Copy the "lockresource" ... "(7b4f7e19e103)"

						tmp = tmp.substring(0, firstPos).trim(); // Strip off the end " (7b4f7e19e103)"
						firstPos = tmp.indexOf(':');
						if (firstPos > 0)
						{
							int  dbid   = StringUtil.parseInt (tmp.substring(0,  firstPos), -1); 
							long hobtid = StringUtil.parseLong(tmp.substring(1 + firstPos), -1);
							if (dbid > 0 && hobtid > 0)
							{
								try 
								{
									wi._dbname = cache.getDBName(dbid);
									
									ObjectInfo oi = cache.getByHobtId(dbid, hobtid);
									if (oi != null)
									{
										wi._schemaName = oi.getSchemaName();
										wi._objectName = oi.getObjectName();
									}
								}
								catch (TimeoutException ignore) {}

								// Should we go and get EXTRA INFO (the data in the table that is locked???)
								boolean getLockedTableData = Configuration.getCombinedConfiguration().getBooleanProperty(PROPKEY_ON_WAIT_RESOURCE__KEY__GET_LOCKED_TABLE_DATA, DEFAULT_ON_WAIT_RESOURCE__KEY__GET_LOCKED_TABLE_DATA);
								if (getLockedTableData)
								{
									String fullTableName = "[" + wi._dbname + "].[" + wi._schemaName + "].[" + wi._objectName + "]";
									String sql = "SELECT '" + lockres + "' AS [%%lockres%%], * FROM " + fullTableName + " (NOLOCK) WHERE %%lockres%% = '" + lockres + "'";

									// Example: SELECT '(ce52f92a058c)' AS [%%lockres%%], * FROM [dbname].[schema].[table] (NOLOCK) WHERE %%lockres%% = '(ce52f92a058c)'

									try
									{
										long startTime = System.currentTimeMillis();

										DbxConnection conn = CounterController.getInstance().getMonConnection();
										ResultSetTableModel rstm = ResultSetTableModel.executeQuery(conn, sql, 1, "LockedTableData");
										if (rstm != null)
										{
											wi._extraInfo = rstm.toAsciiTableString() 
													+  "NOTE: This can be disabled with property '" + PROPKEY_ON_WAIT_RESOURCE__KEY__GET_LOCKED_TABLE_DATA + "=false' if it takes to much time/resources or causes any errors.";
										}
										
										// Log message if this takes to long time
										long execTimeInMs = TimeUtils.msDiffNow(startTime);
										if (execTimeInMs > 1_000)
										{
											_logger.warn("Request to get '" + PROPKEY_ON_WAIT_RESOURCE__KEY__GET_LOCKED_TABLE_DATA + "' took " + execTimeInMs + " ms. This was a bit slower than expected. (this message will be logged when requestst take more than 1000 ms).");
										}
									}
									catch(SQLException ex)
									{
										wi._extraInfo = ex.getMessage()
												+  "\nNOTE: This can be disabled with property '" + PROPKEY_ON_WAIT_RESOURCE__KEY__GET_LOCKED_TABLE_DATA + "=false' if it takes to much time/resources or causes any errors.";

										_logger.warn("Problems getting Locked Resource using SQL=|" + sql + "|, skipping this and continuing... This can be disabled with property '" + PROPKEY_ON_WAIT_RESOURCE__KEY__GET_LOCKED_TABLE_DATA + "=false'. Caught: " + ex);
									}
								}
							}
						}
					}
				}
			} // end: KEY
			else if (wait_resource.startsWith("PAGE: "))   // example='PAGE: 6:1:70133' -- Database_Id : FileId : PageNumber
			{
				if (DbmsObjectIdCache.hasInstance())
				{
					DbmsObjectIdCache cache = DbmsObjectIdCache.getInstance();
					
					// 'PAGE: 6:1:70133' -- Database_Id : FileId : PageNumber
					String tmp = wait_resource.substring("PAGE: ".length()); // Strip off "PAGE: "

					String[] sa = tmp.split(":");
					if (sa.length == 3)
					{
						if (sa[0].length() <= 6 && NumberUtils.isNumber(sa[0]))
						{
							int dbid = StringUtil.parseInt(sa[0], -1);
							if (dbid > 0)
							{
								wi._dbname = cache.getDBName(dbid);
								
								if (NumberUtils.isNumber(sa[1]) && NumberUtils.isNumber(sa[2]))
								{
									int fileNum = StringUtil.parseInt(sa[1], -1);
									int pageNum = StringUtil.parseInt(sa[2], -1);
									if (fileNum >= 0 && pageNum >= 0)
									{
										DbxConnection conn = CounterController.getInstance().getMonConnection();

										// Get Object information, typically from the page itself (dm_db_page_info or potentially DBCC page)
										lookupPageNumber(conn, wi, dbid, fileNum, pageNum);
									}
								}
							}
						}
					}
				}
				
			
// https://social.msdn.microsoft.com/Forums/sqlserver/en-US/b98a7841-69a4-47d3-8856-deb310b3ccc7/how-to-identify-if-tempdb-contention-on-page-is-pfsgam-or-sgam?forum=sqldatabaseengine
// https://ramblingsofraju.com/sql-server/breaking-down-tempdb-contention-2/
// http://whoisactive.com/docs/21_tempdb/ -- check the code how it decode page values
//				SELECT 
//					session_id,
//					wait_type,
//					wait_duration_ms,
//					blocking_session_id,
//					resource_description,
//					ResourceType = 
//					CASE
//						WHEN CAST(right(resource_description, len(resource_description) - charindex(':', resource_description, 3)) AS int) - 1 % 8088   = 0 THEN 'Is PFS Page'
//						WHEN CAST(right(resource_description, len(resource_description) - charindex(':', resource_description, 3)) AS int) - 2 % 511232 = 0 THEN 'Is GAM Page'
//						WHEN CAST(right(resource_description, len(resource_description) - charindex(':', resource_description, 3)) AS int) - 3 % 511232 = 0 THEN 'Is SGAM Page'
//						ELSE 'Is Not PFS, GAM, or SGAM page'
//					END
//				FROM  sys.dm_os_waiting_tasks
//				WHERE wait_type LIKE 'PAGE%LATCH_%'
//				  AND resource_description LIKE '2:%'
				
				// NOTE: Double check the below DECODE again...
//				int pageid = StringUtil.parseInt (keyVal.get("pageid"), -1);
//				if (pageid != -1)
//				{
//					// The below algorithm is reused from 'sp_whoIsActive'
//					if      (pageid == 1 ||       pageid % 8088   == 0) decodeKeyVal.put("pageType", "PFS");
//					else if (pageid == 2 ||       pageid % 511232 == 0) decodeKeyVal.put("pageType", "GAM");
//					else if (pageid == 3 || (pageid - 1) % 511232 == 0) decodeKeyVal.put("pageType", "SGAM");
//					else if (pageid == 6 || (pageid - 6) % 511232 == 0) decodeKeyVal.put("pageType", "DCM");
//					else if (pageid == 7 || (pageid - 7) % 511232 == 0) decodeKeyVal.put("pageType", "BCM");
//				}
				
			} // end: PAGE
			else if (wait_resource.startsWith("OBJECT: "))   // example='OBJECT: 7:1429580131:0' -- ???Database_Id??? : ???objectId??? : ???
			{
				// Here is an article I came across to better understand the resource lock partition id in the 
				//    waitresource = "OBJECT: dbid:object_id:resource_lock_partition id"
				//    https://www.microsoftpressstore.com/articles/article.aspx?p=2233327&seqNum=5
				
				if (DbmsObjectIdCache.hasInstance())
				{
					DbmsObjectIdCache cache = DbmsObjectIdCache.getInstance();
					
					String tmp = wait_resource.substring("OBJECT: ".length()); // Strip off "OBJECT: "
					String[] sa = tmp.split(":");
					if (sa.length == 3)
					{
						int dbid     = StringUtil.parseInt(sa[0], -1);
						int objectId = StringUtil.parseInt(sa[1], -1);
					//	int xxxxxxId = StringUtil.parseInt(sa[2], -1);
						
						if (dbid > 0)
							wi._dbname = cache.getDBName(dbid);

						if (dbid > 0 && objectId > 0)
						{
							try 
							{
								ObjectInfo oi = cache.getByObjectId(dbid, objectId);
								if (oi != null)
								{
									wi._schemaName = oi.getSchemaName();
									wi._objectName = oi.getObjectName();
								}
							}
							catch (TimeoutException ignore) {}
						}
					}
				}
			}
			else if (wait_resource.startsWith("RID: "))   // example='RID: 5:1:8400:2' -- dbid:fileid:pageid:??? (guessing ??? is rowOffset/rowId) 
			{
				if (DbmsObjectIdCache.hasInstance())
				{
					DbmsObjectIdCache cache = DbmsObjectIdCache.getInstance();
					
					// 'PAGE: 6:1:70133' -- Database_Id : FileId : PageNumber
					String tmp = wait_resource.substring("RID: ".length()); // Strip off "PAGE: "

					String[] sa = tmp.split(":");
					if (sa.length >= 3)
					{
						if (sa[0].length() <= 6 && NumberUtils.isNumber(sa[0]))
						{
							int dbid = StringUtil.parseInt(sa[0], -1);
							if (dbid > 0)
							{
								wi._dbname = cache.getDBName(dbid);
								
								if (NumberUtils.isNumber(sa[1]) && NumberUtils.isNumber(sa[2]))
								{
									int fileNum = StringUtil.parseInt(sa[1], -1);
									int pageNum = StringUtil.parseInt(sa[2], -1);
									if (fileNum >= 0 && pageNum >= 0)
									{
										DbxConnection conn = CounterController.getInstance().getMonConnection();

										// Get Object information, typically from the page itself (dm_db_page_info or potentially DBCC page)
										lookupPageNumber(conn, wi, dbid, fileNum, pageNum);
									}
								}
							}
						}
					}
				}
			}
			else // OTHERS with no prefix
			{
				if (DbmsObjectIdCache.hasInstance())
				{
					DbmsObjectIdCache cache = DbmsObjectIdCache.getInstance();
					
					// Typically get '20:1:1294371' // which probably is "dbid:fileid:pageid"
					String[] sa = wait_resource.split(":");
					if (sa.length == 3)
					{
						if (sa[0].length() <= 6 && NumberUtils.isNumber(sa[0]))
						{
							int dbid = StringUtil.parseInt(sa[0], -1);
							if (dbid > 0)
							{
								wi._dbname = cache.getDBName(dbid);
								
								if (NumberUtils.isNumber(sa[1]) && NumberUtils.isNumber(sa[2]))
								{
									int fileNum = StringUtil.parseInt(sa[1], -1);
									int pageNum = StringUtil.parseInt(sa[2], -1);
									if (fileNum >= 0 && pageNum >= 0)
									{
										DbxConnection conn = CounterController.getInstance().getMonConnection();

										// Get Object information, typically from the page itself (dm_db_page_info or potentially DBCC page)
										lookupPageNumber(conn, wi, dbid, fileNum, pageNum);
									}
								}
							}
						}
					}
				}
			} // end: What type of wait_resource
		} // end: method: add

		/**
		 * Get information from a specific PAGE in a database (using DBCC: PAGE or DMV: dm_db_page_info)
		 * 
		 * @param conn      Database Connection
		 * @param wi        SpidWaitInfo structure (where we fill in object name etc)
		 * @param dbid      dbid we want to lookup
		 * @param fileNum   file number we want to lookup
		 * @param pageNum   page number we want to lookup
		 */
		private void lookupPageNumber(DbxConnection conn, SpidWaitInfo wi, int dbid, int fileNum, int pageNum)
		{
			// Early exit
			if (conn == null) return;
			if (wi   == null) return;

			// In here we could try to decode the 'page_type_desc' based on page number... PFS, GAM, SGAM... etc 
			// The below algorithm is reused from 'sp_whoIsActive'
			// and description from: https://www.sqlskills.com/blogs/paul/inside-the-storage-engine-gam-sgam-pfs-and-other-allocation-maps/
			// and https://learn.microsoft.com/en-us/sql/relational-databases/pages-and-extents-architecture-guide?view=sql-server-ver16#manage-extent-allocations
			if      (pageNum == 1 ||       pageNum % 8088   == 0) wi._pageTypeDesc = "PFS";  // PFS stands for Page Free Space
			else if (pageNum == 2 ||       pageNum % 511232 == 0) wi._pageTypeDesc = "GAM";  // Global Allocation Map
			else if (pageNum == 3 || (pageNum - 1) % 511232 == 0) wi._pageTypeDesc = "SGAM"; // Shared Global Allocation Map
			else if (pageNum == 6 || (pageNum - 6) % 511232 == 0) wi._pageTypeDesc = "DCM";  // DIFF map page -- DIFF stands for differential. These pages track which extents have been modified since the last full backup was taken
			else if (pageNum == 7 || (pageNum - 7) % 511232 == 0) wi._pageTypeDesc = "BCM";  // ML map page   -- ML stands for Minimally Logged. These pages track which extents have been modified by minimally-logged operations since the last transaction log backup when using the BULK_LOGGED recovery model

			// Check if this is enabled or not (in the configuration)
			boolean isEnabled = Configuration.getCombinedConfiguration().getBooleanProperty(PROPKEY_ON_WAIT_RESOURCE__GET_PAGE_INFO, DEFAULT_ON_WAIT_RESOURCE__GET_PAGE_INFO);

			// Check if this has been disabled on a connection level
			if (conn.hasProperty(PROPKEY_ON_WAIT_RESOURCE__GET_PAGE_INFO))
				isEnabled = false;

			// No need to continue if it's not enabled
			if ( ! isEnabled )
				return;

			// DMV 'dm_db_page_info' exists from SQL Server 2019
			if (conn.getDbmsVersionInfo().getLongVersion() < Ver.ver(2019))
			{
				// if we REALLY WANT, we could do: 
				// dbcc traceon(3604)
				// dbcc page(dbid, fileNum, pageNum, 0)
				// Loop the SQLWarnings
				//   - look for row: |Metadata: PartitionId = 1125899909070848                                 Metadata: IndexId = 4|
				//                                                                                             ^^^^^^^^^^^^^^^^^^^^^
				//   - look for row: |Metadata: ObjectId = 34             m_prevPage = (0:0)                  m_nextPage = (0:0)|
				//                    ^^^^^^^^^^^^^^^^^^^^^^^

				// Turn on traceflag 3604, if we havn't already done that
				if ( ! conn.hasProperty("traceon-3604") )
				{
					try 
					{
						conn.dbExec("dbcc traceon(3604)", false);
						conn.setProperty("traceon-3604", "on");
					} 
					catch (SQLException ex)
					{
						_logger.warn("Problems settin traceflag 3604. SQL=|dbcc traceon(3604)|. Skipping this and continuing. Caught: " + ex);

						// Msg=2571, Text=User 'xxxx' does not have permission to run DBCC TRACEON.							
						if (ex.getErrorCode() == 2571)
						{
							_logger.warn("lookupPageNumber(): Disabling '" + PROPKEY_ON_WAIT_RESOURCE__GET_PAGE_INFO + "' on a Connection level due to Error=2571, MsgText='" + ex.getMessage() + "'.");
							conn.setProperty(PROPKEY_ON_WAIT_RESOURCE__GET_PAGE_INFO, "false");
							return;
						}
					}
				}
				
				// Execute: DBCC PAGE
				String sql = "dbcc page(" + dbid + ", " + fileNum + ", " + pageNum + ", 0)";
				
				// or we can do it with: DBCC PAGE(...) WITH TABLERESULTS
				// column: 'Field' = 'Metadata: ObjectId'
				// column: 'Field' = 'Metadata: IndexId'

				try (Statement stmnt = conn.createStatement())
				{
					long startTime = System.currentTimeMillis();

					stmnt.setQueryTimeout(1);
					int rowsAffected = 0;
					boolean hasRs = stmnt.execute(sql);
					
					do
					{
						if (hasRs)
						{
							ResultSet rs = stmnt.getResultSet();
							while (rs.next())
								; // This just reads the ResultSet and "throws" data
							rs.close();
						}
						else
						{
							rowsAffected = stmnt.getUpdateCount();
						}
						hasRs = stmnt.getMoreResults();
					}
					while (hasRs || rowsAffected != -1);


					int object_id = -1;
					int index_id  = -1;

					SQLWarning sqlWarn = stmnt.getWarnings();
					while (sqlWarn != null)
					{
						String msg = StringUtil.trim(sqlWarn.getMessage());
						
						if (_logger.isDebugEnabled())
							_logger.debug("DBCC PAGE(dbid=" + dbid + ", fileNum=" + fileNum + ", pageNum=" + pageNum + ") ROW=|" + msg + "|");

						if (StringUtil.hasValue(msg))
						{
							if (msg.contains("Metadata: ObjectId = "))
							{
								String dataStr = StringUtil.substringBetweenTwoChars(msg, "Metadata: ObjectId = ", " ");
								object_id = StringUtil.parseInt(dataStr, -1);
							}

							if (msg.contains("Metadata: IndexId = "))
							{
								String dataStr = StringUtil.substringBetweenTwoChars(msg, "Metadata: IndexId = ", " ");
								index_id = StringUtil.parseInt(dataStr, -1);
							}
						}
						
						sqlWarn = sqlWarn.getNextWarning();
					} //end: loop SQL Warnings

					if (_logger.isDebugEnabled())
						_logger.debug("DBCC PAGE(dbid=" + dbid + ", fileNum=" + fileNum + ", pageNum=" + pageNum + ") ----- AFTER-LOOP-ROWS ----- object_id=" + object_id + ", index_id=" + index_id);

					if (object_id > 0)
					{
						try 
						{
							DbmsObjectIdCache cache = DbmsObjectIdCache.getInstance();

							wi._dbname = cache.getDBName(dbid);
							if (object_id == 99) 
								wi._objectName = "ALLOCATION_PAGE[object_id=99]";

							ObjectInfo oi = cache.getByObjectId(dbid, object_id);
							if (oi != null)
							{
								wi._schemaName = oi.getSchemaName();
								wi._objectName = oi.getObjectName();

								if (index_id >= 0)
									wi._indexName = oi.getIndexName(index_id);
							}
							
							if (_logger.isDebugEnabled())
								_logger.debug("DBCC PAGE(dbid=" + dbid + ", fileNum=" + fileNum + ", pageNum=" + pageNum + ") ----- AFTER-DbmsObjectIdCache-LOOKUP ----- dbname='" + wi._schemaName + "', objectName='" + wi._objectName + "', wi._indexName='" + wi._indexName + "'.");
						}
						catch (TimeoutException ignore) {}
					}

					// Log message if this takes to long time
					long execTimeInMs = TimeUtils.msDiffNow(startTime);
					if (execTimeInMs > 1_000)
					{
						_logger.warn("Request to get '" + PROPKEY_ON_WAIT_RESOURCE__GET_PAGE_INFO + "' (dbcc page) took " + execTimeInMs + " ms. This was a bit slower than expected. (this message will be logged when requestst take more than 1000 ms).");
					}
				}
				catch (SQLException ex)
				{
					wi._extraInfo = ex.getMessage()
							+  "\nNOTE: This can be disabled with property '" + PROPKEY_ON_WAIT_RESOURCE__GET_PAGE_INFO + "=false' if it takes to much time/resources or causes any errors.";

					_logger.warn("Problems getting Object PAGE Information using SQL=|" + sql + "|. Skipping this and continuing. Caught: " + ex);

					// Msg=2571, Text=User 'xxxx' does not have permission to run DBCC page.
					if (ex.getErrorCode() == 2571)
					{
						_logger.warn("lookupPageNumber(): Disabling '" + PROPKEY_ON_WAIT_RESOURCE__GET_PAGE_INFO + "' on a Connection level due to Error=2571, MsgText='" + ex.getMessage() + "'.");
						conn.setProperty(PROPKEY_ON_WAIT_RESOURCE__GET_PAGE_INFO, "false");
						return;
					}
				}
				return;
			} // end: version less than SQL Server 2019
			else
			{
				String sql = ""
						+ "SELECT object_id, index_id, partition_id, page_type_desc "
						+ "FROM sys.dm_db_page_info(?, ?, ?, 'DETAILED')";   // 'LIMITED' (do not fill in descriptive fields) or 'DETAILED'

				try (PreparedStatement pstmnt = conn.prepareStatement(sql))
				{
					long startTime = System.currentTimeMillis();
					
					pstmnt.setInt(1, dbid);
					pstmnt.setInt(2, fileNum);
					pstmnt.setInt(3, pageNum);

					pstmnt.setQueryTimeout(1);
					try (ResultSet rs = pstmnt.executeQuery())
					{
						int    object_id      = -1;
						int    index_id       = -1;
					//	long   partition_id   = -1;
						String page_type_desc = null;

						while (rs.next())
						{
							object_id      = rs.getInt   (1);
							index_id       = rs.getInt   (2);
						//	partition_id   = rs.getLong  (3);
							page_type_desc = rs.getString(4);
						}

						// Log message if this takes to long time
						long execTimeInMs = TimeUtils.msDiffNow(startTime);
						if (execTimeInMs > 1_000)
						{
							_logger.warn("Request to get '" + PROPKEY_ON_WAIT_RESOURCE__GET_PAGE_INFO + "' (sys.dm_db_page_info) took " + execTimeInMs + " ms. This was a bit slower than expected. (this message will be logged when requestst take more than 1000 ms).");
						}

						if (object_id > 0)
						{
							wi._pageTypeDesc = page_type_desc;

							try 
							{
								DbmsObjectIdCache cache = DbmsObjectIdCache.getInstance();

								wi._dbname = cache.getDBName(dbid);
								if (object_id == 99) 
									wi._objectName = "ALLOCATION_PAGE[object_id=99]";

								ObjectInfo oi = cache.getByObjectId(dbid, object_id);
								if (oi != null)
								{
									wi._schemaName = oi.getSchemaName();
									wi._objectName = oi.getObjectName();

									if (index_id >= 0)
										wi._indexName = oi.getIndexName(index_id);
								}
							}
							catch (TimeoutException ignore) {}
						}
					}
				}
				catch (SQLException ex)
				{
					wi._extraInfo = ex.getMessage()
							+  "\nNOTE: This can be disabled with property '" + PROPKEY_ON_WAIT_RESOURCE__GET_PAGE_INFO + "=false' if it takes to much time/resources or causes any errors.";

					_logger.warn("Problems getting Object PAGE Information using SQL=|" + sql + "|. Skipping this and continuing. Caught: " + ex);

					// Msg=2571,  Text=User 'xxxx' does not have permission to run ...
					// Msg=15562, Text=The module being executed is not trusted. Either the owner of the database of the module needs to be granted authenticate permission, or the module needs to be digitally signed.					
					if (ex.getErrorCode() == 2571 || ex.getErrorCode() == 15562)
					{
						_logger.warn("lookupPageNumber(): Disabling '" + PROPKEY_ON_WAIT_RESOURCE__GET_PAGE_INFO + "' on a Connection level due to Error=" + ex.getErrorCode() + ", MsgText='" + ex.getMessage() + "'.");
						conn.setProperty(PROPKEY_ON_WAIT_RESOURCE__GET_PAGE_INFO, "false");
						return;
					}
				}
			} // end: SQL Server 2019 or above
		}

		public void checkAndSendAlarm()
		{
			if ( ! AlarmHandler.hasInstance() )
				return;
			
			String infoStr = getInfoString();
			
			if (true)
			{
				System.out.println(infoStr);
			}

//			if (Configuration.getCombinedConfiguration().getBooleanProperty("CmSessions.sendAlarm.AlarmEventDebugWaitInfo.send", true)) 
			if (Configuration.getCombinedConfiguration().getBooleanProperty(PROPKEY_alarm_WaitInfo, DEFAULT_alarm_WaitInfo)) 
			{
//				int alarmOnMaxCountGt = Configuration.getCombinedConfiguration().getIntProperty("CmSessions.sendAlarm.AlarmEventDebugWaitInfo.maxCount.gt", 2);
				int alarmOnMaxCountGt = Configuration.getCombinedConfiguration().getIntProperty(PROPKEY_alarm_WaitInfo_maxCount_gt, DEFAULT_alarm_WaitInfo_maxCount_gt);
				
				if (getMaxCount() > alarmOnMaxCountGt)
				{
					AlarmEvent alarm = new AlarmEventDebugWaitInfo(_cm, infoStr);
					AlarmHandler.getInstance().addAlarm(alarm);
				}
			}
		}
		
		public int getMaxCount()
		{
			int maxCount = 0;
			
			for (Entry<String, List<SpidWaitInfo>> entry : _waitResourceMap.entrySet())
			{
				maxCount = Math.max(maxCount, entry.getValue().size());
			}

			return maxCount;
		}

		public String getObjectInfoForWaitResource(String key)
		{
			List<SpidWaitInfo> list = _waitResourceMap.get(key);
			if (list != null && list.size() > 0)
			{
				SpidWaitInfo wi = list.get(0);
				String dbname     = wi._dbname;
				String schemaName = wi._schemaName;
				String objectName = wi._objectName;
				String indexName  = wi._indexName;
				String pageType   = wi._pageTypeDesc;
				
				if (StringUtil.isNullOrBlank(dbname) && StringUtil.isNullOrBlank(objectName))
					return "-not-found-";
				
				return "dbname='" + dbname + "'" 
					+ (StringUtil.isNullOrBlank(schemaName) ? "" : ", SchemaName='" + schemaName + "'")
					+ (StringUtil.isNullOrBlank(objectName) ? "" : ", ObjectName='" + objectName + "'")
					+ (StringUtil.isNullOrBlank(indexName)  ? "" : ", IndexName='"  + indexName  + "'")
					+ (StringUtil.isNullOrBlank(pageType)   ? "" : ", PageType='"   + pageType   + "'")
					;
			}
			return "";
		}

		public String getExtraInfoForWaitResource(String key)
		{
			List<SpidWaitInfo> list = _waitResourceMap.get(key);
			if (list != null && list.size() > 0)
			{
				SpidWaitInfo wi = list.get(0);
				String extraInfo = wi._extraInfo;
				
				if (StringUtil.isNullOrBlank(extraInfo))
					return "-not-found-";
				
				if (extraInfo.startsWith("+---------"))
					return "\n" + extraInfo;

				return extraInfo;
			}
			return "";
		}
		
		
		public String getInfoString()
		{
			StringBuilder sb = new StringBuilder();
			
			sb.append("CmSessions: ================= WaitInfo ====================== \n");
			sb.append("CmSessions: WaitResources(" + _waitResourceMap.size() + "): " + _waitResourceMap.keySet() + " \n");
			sb.append("CmSessions: WaitTypes    (" + _waitTypeMap    .size() + "): " + _waitTypeMap    .keySet() + " \n");

			sb.append("CmSessions: \n");
			sb.append("CmSessions: Wait Resources -- Count: \n");
			for (Entry<String, List<SpidWaitInfo>> entry : _waitResourceMap.entrySet())
			{
				sb.append("CmSessions:   - count=" + entry.getValue().size() + " -- WaitResource='" + entry.getKey() + "'  ObjectInfo={" + getObjectInfoForWaitResource(entry.getKey()) + "} \n");
			}

			sb.append("CmSessions: \n");
			sb.append("CmSessions: Wait Resources -- Sum WaitTime: \n");
			for (Entry<String, Integer> entry : _waitResourceTime.entrySet())
			{
				sb.append("CmSessions:   - waitTime=" + entry.getValue() + " ms -- WaitResource='" + entry.getKey() + "'  ObjectInfo={" + getObjectInfoForWaitResource(entry.getKey()) + "} \n");
			}

			sb.append("CmSessions: \n");
			sb.append("CmSessions: Wait Types -- Sum WaitTime: \n");
			for (Entry<String, Integer> entry : _waitTypeTime.entrySet())
			{
				sb.append("CmSessions:   - waitTime=" + entry.getValue() + " ms -- WaitType='" + entry.getKey() + "'  ObjectInfo={" + getObjectInfoForWaitResource(entry.getKey()) + "} \n");
			}
			
			// Extra Info
			sb.append("CmSessions: \n");
			sb.append("CmSessions: Extra Info: \n");
			for (Entry<String, List<SpidWaitInfo>> entry : _waitResourceMap.entrySet())
			{
				String extraInfo = getExtraInfoForWaitResource(entry.getKey());
				extraInfo = StringUtil.addPrefix("CmSessions:                ", extraInfo, false);

				sb.append("CmSessions:   - count=" + entry.getValue().size() + " -- WaitResource='" + entry.getKey() + "'  ObjectInfo={" + getObjectInfoForWaitResource(entry.getKey()) + "}  --- ExtraInfo: " + extraInfo + " \n");
			}
			sb.append("CmSessions: ================================================= \n");

			return sb.toString();
		}
	}
	
	public static final String  PROPKEY_ON_WAIT_RESOURCE__KEY__GET_LOCKED_TABLE_DATA = "CmSessions.onWaitResource.KEY.getLockedTableData";
	public static final boolean DEFAULT_ON_WAIT_RESOURCE__KEY__GET_LOCKED_TABLE_DATA = true;

	public static final String  PROPKEY_ON_WAIT_RESOURCE__GET_PAGE_INFO = "CmSessions.onWaitResource.getPageInfo";
	public static final boolean DEFAULT_ON_WAIT_RESOURCE__GET_PAGE_INFO = true;
}
