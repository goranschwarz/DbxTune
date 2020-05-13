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
import com.asetune.gui.MainFrame;

/**
 * @author Goran Schwarz (goran_schwarz@hotmail.com)
 */
public class CmExecSessions
extends CountersModel
{
//	private static Logger        _logger          = Logger.getLogger(CmExecProcedureStats.class);
	private static final long    serialVersionUID = 1L;

	public static final String   CM_NAME          = CmExecSessions.class.getSimpleName();
	public static final String   SHORT_NAME       = "Sessions";
	public static final String   HTML_DESC        = 
		"<html>"
		+ "<p>"
		+ "Shows one row per authenticated session on SQL Server. <span class=\"literal\">sys.dm_exec_sessions</span>?is a server-scope view that shows information about all active user connections and internal tasks. <br>"
		+ "This information includes client version, client program name, client login time, login user, current session setting, and more.<br>"
		+ "Use <i>sys.dm_exec_sessions</i> to first view the current system load and to identify a session of interest, and <i>then</i> learn more information about that session by using other dynamic management views or dynamic management functions."
		+ "</p>"
		+ "<p>"
		+ "The <span class=\"literal\">sys.dm_exec_connections</span>, <span class=\"literal\">sys.dm_exec_sessions</span>, and <span class=\"literal\">sys.dm_exec_requests</span> dynamic management views map to the <a href=\"https://msdn.microsoft.com/en-us/library/ms179881.aspx\">sys.sysprocesses</a> system table. "
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
		 "cpu_time"
		,"total_scheduled_time"
		,"total_elapsed_time"
		,"memory_usage"
		,"reads"
		,"writes"
		,"logical_reads"
//		,"row_count"        // this is NOT affected rows, it's what 'set rowcount ##' is set to
		};
	// RS> Col# Label                       JDBC Type Name           Guessed DBMS type Source Table
	// RS> ---- --------------------------- ------------------------ ----------------- ------------
	// RS> 1    session_id                  java.sql.Types.SMALLINT  smallint          -none-      
	// RS> 2    login_time                  java.sql.Types.TIMESTAMP datetime          -none-      
	// RS> 3    host_name                   java.sql.Types.NVARCHAR  nvarchar(128)     -none-      
	// RS> 4    program_name                java.sql.Types.NVARCHAR  nvarchar(128)     -none-      
	// RS> 5    host_process_id             java.sql.Types.INTEGER   int               -none-      
	// RS> 6    client_version              java.sql.Types.INTEGER   int               -none-      
	// RS> 7    client_interface_name       java.sql.Types.NVARCHAR  nvarchar(32)      -none-      
	// RS> 8    security_id                 java.sql.Types.VARBINARY varbinary(170)    -none-      
	// RS> 9    login_name                  java.sql.Types.NVARCHAR  nvarchar(128)     -none-      
	// RS> 10   nt_domain                   java.sql.Types.NVARCHAR  nvarchar(128)     -none-      
	// RS> 11   nt_user_name                java.sql.Types.NVARCHAR  nvarchar(128)     -none-      
	// RS> 12   status                      java.sql.Types.NVARCHAR  nvarchar(30)      -none-      
	// RS> 13   context_info                java.sql.Types.VARBINARY varbinary(256)    -none-      
	// RS> 14   cpu_time                    java.sql.Types.INTEGER   int               -none-      
	// RS> 15   memory_usage                java.sql.Types.INTEGER   int               -none-      
	// RS> 16   total_scheduled_time        java.sql.Types.INTEGER   int               -none-      
	// RS> 17   total_elapsed_time          java.sql.Types.INTEGER   int               -none-      
	// RS> 18   endpoint_id                 java.sql.Types.INTEGER   int               -none-      
	// RS> 19   last_request_start_time     java.sql.Types.TIMESTAMP datetime          -none-      
	// RS> 20   last_request_end_time       java.sql.Types.TIMESTAMP datetime          -none-      
	// RS> 21   reads                       java.sql.Types.BIGINT    bigint            -none-      
	// RS> 22   writes                      java.sql.Types.BIGINT    bigint            -none-      
	// RS> 23   logical_reads               java.sql.Types.BIGINT    bigint            -none-      
	// RS> 24   is_user_process             java.sql.Types.BIT       bit               -none-      
	// RS> 25   text_size                   java.sql.Types.INTEGER   int               -none-      
	// RS> 26   language                    java.sql.Types.NVARCHAR  nvarchar(128)     -none-      
	// RS> 27   date_format                 java.sql.Types.NVARCHAR  nvarchar(3)       -none-      
	// RS> 28   date_first                  java.sql.Types.SMALLINT  smallint          -none-      
	// RS> 29   quoted_identifier           java.sql.Types.BIT       bit               -none-      
	// RS> 30   arithabort                  java.sql.Types.BIT       bit               -none-      
	// RS> 31   ansi_null_dflt_on           java.sql.Types.BIT       bit               -none-      
	// RS> 32   ansi_defaults               java.sql.Types.BIT       bit               -none-      
	// RS> 33   ansi_warnings               java.sql.Types.BIT       bit               -none-      
	// RS> 34   ansi_padding                java.sql.Types.BIT       bit               -none-      
	// RS> 35   ansi_nulls                  java.sql.Types.BIT       bit               -none-      
	// RS> 36   concat_null_yields_null     java.sql.Types.BIT       bit               -none-      
	// RS> 37   transaction_isolation_level java.sql.Types.SMALLINT  smallint          -none-      
	// RS> 38   lock_timeout                java.sql.Types.INTEGER   int               -none-      
	// RS> 39   deadlock_priority           java.sql.Types.INTEGER   int               -none-      
	// RS> 40   row_count                   java.sql.Types.BIGINT    bigint            -none-      
	// RS> 41   prev_error                  java.sql.Types.INTEGER   int               -none-      
	// RS> 42   original_security_id        java.sql.Types.VARBINARY varbinary(170)    -none-      
	// RS> 43   original_login_name         java.sql.Types.NVARCHAR  nvarchar(128)     -none-      
	// RS> 44   last_successful_logon       java.sql.Types.TIMESTAMP datetime          -none-      
	// RS> 45   last_unsuccessful_logon     java.sql.Types.TIMESTAMP datetime          -none-      
	// RS> 46   unsuccessful_logons         java.sql.Types.BIGINT    bigint            -none-      
	// RS> 47   group_id                    java.sql.Types.INTEGER   int               -none-      

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

		return new CmExecSessions(counterController, guiController);
	}

	public CmExecSessions(ICounterController counterController, IGuiController guiController)
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
	public String[] getDependsOnConfigForVersion(Connection conn, long srvVersion, boolean isAzure)
	{
		return NEED_CONFIG;
	}

	@Override
	public List<String> getPkForVersion(Connection conn, long srvVersion, boolean isAzure)
	{
		List <String> pkCols = new LinkedList<String>();

		pkCols.add("session_id");

		return pkCols;
	}

	@Override
	public String getSqlForVersion(Connection conn, long srvVersion, boolean isAzure)
	{
		String dm_exec_sessions = "dm_exec_sessions";
		
		if (isAzure)
			dm_exec_sessions = "dm_pdw_nodes_exec_sessions";

		// datediff(ms, last_request_start_time, last_request_end_time)
		
		String sql = ""
				+ "select "
				+ "     last_request_ms = datediff(ms, last_request_start_time, last_request_end_time) \n"
				+ "     login_time_ss   = CASE WHEN datediff(day, login_time, getdate()) >= 24 THEN -1 ELSE  datediff(ss, login_time, getdate()) END, \n"
				+ "    ,* \n"
//				+ "    ,dbname                = db_name(database_id) \n"                 // Applies to: SQL Server 2012 (11.x) and later.
//				+ "    ,authenticating_dbname = db_name(authenticating_database_id) \n"  // Applies to: SQL Server 2012 (11.x) and later.
				+ "from sys." + dm_exec_sessions + " \n"
//				+ "where is_user_process = 1 \n"
				+ "";

		return sql;
	}
}
