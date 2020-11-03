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
package com.asetune.cm.sqlserver;

import java.sql.Connection;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import com.asetune.ICounterController;
import com.asetune.IGuiController;
import com.asetune.cm.CmSettingsHelper;
import com.asetune.cm.CounterSetTemplates;
import com.asetune.cm.CounterSetTemplates.Type;
import com.asetune.cm.CountersModel;
import com.asetune.cm.sqlserver.gui.CmTempdbSpidUsagePanel;
import com.asetune.gui.MainFrame;
import com.asetune.gui.TabularCntrPanel;
import com.asetune.pcs.PcsColumnOptions;
import com.asetune.pcs.PcsColumnOptions.ColumnType;
import com.asetune.utils.Configuration;

/**
 * @author Goran Schwarz (goran_schwarz@hotmail.com)
 */
public class CmTempdbSpidUsage
extends CountersModel
{
//	private static Logger        _logger          = Logger.getLogger(CmTempdbSpidUsage.class);
	private static final long    serialVersionUID = 1L;

	public static final String   CM_NAME          = CmTempdbSpidUsage.class.getSimpleName();
	public static final String   SHORT_NAME       = "Tempdb SPID Usage";
	public static final String   HTML_DESC        = 
		"<html>" +
			"<p>What session_id's are using the tempdb.</p>" +
			"<br>" +
			"<b>Tip:</b><br>" +
			"To find SPID's that are using tempdb <b>a lot</b> sort by columns() '*_dealloc', which holds tempdb space that has been used <b>earlier</b> but now has been deleted...<br>" +
			"<br><br>" +
			"Table Background colors:" +
			"<ul>" +
			"    <li>GREEN  - SPID is Executing(running) or are in the Run Queue Awaiting a time slot to Execute (runnable)</li>" +
			"    <li>ORANGE - SPID has an open transaction.</li>" +
			"</ul>" +
		"</html>";

	public static final String   GROUP_NAME       = MainFrame.TCP_GROUP_SERVER;
	public static final String   GUI_ICON_FILE    = "images/"+CM_NAME+".png";

	public static final long     NEED_SRV_VERSION = 0;
	public static final long     NEED_CE_VERSION  = 0;

	public static final String[] MON_TABLES       = new String[] {"dm_db_task_space_usage", "dm_db_session_space_usage", "dm_exec_session", "dm_exec_connections", "dm_exec_sql_text"};
	public static final String[] NEED_ROLES       = new String[] {};
	public static final String[] NEED_CONFIG      = new String[] {};

	public static final String[] PCT_COLUMNS      = new String[] {};
	public static final String[] DIFF_COLUMNS     = new String[] {
			 "TotalUsageMb"
			,"SessUserObjectMb"
			,"SessInternalObjectMb"
			,"TaskUserObjectMb"
			,"TaskInternalObjectMb"
			,"s_user_objects_alloc_MB"
			,"s_user_objects_dealloc_MB"
			,"s_user_objects_deferred_dealloc_MB"
			,"s_internal_objects_alloc_MB"
			,"s_internal_objects_dealloc_MB"
			,"t_user_objects_alloc_MB"
			,"t_user_objects_dealloc_MB"
			,"t_internal_objects_alloc_MB"
			,"t_internal_objects_dealloc_MB"
			,"cpu_time"
			,"reads"
			,"writes"
			,"logical_reads"
			,"row_count"
		};
	// RS> Col# Label                              JDBC Type Name           Guessed DBMS type Source Table
	// RS> ---- ---------------------------------- ------------------------ ----------------- ------------
	// RS> 1    session_id                         java.sql.Types.SMALLINT  smallint          -none-      
	// RS> 2    is_user_process                    java.sql.Types.BIT       bit               -none-      
	// RS> 3    TotalUsageMb_abs                   java.sql.Types.DECIMAL   decimal(18,1)     -none-      
	// RS> 4    TotalUsageMb                       java.sql.Types.DECIMAL   decimal(18,1)     -none-      
	// RS> 5    SessUserObjectMb                   java.sql.Types.DECIMAL   decimal(15,1)     -none-      
	// RS> 6    SessInternalObjectMb               java.sql.Types.DECIMAL   decimal(15,1)     -none-      
	// RS> 7    TaskUserObjectMb                   java.sql.Types.DECIMAL   decimal(15,1)     -none-      
	// RS> 8    TaskInternalObjectMb               java.sql.Types.DECIMAL   decimal(15,1)     -none-      
	// RS> 9    s_user_objects_alloc_MB            java.sql.Types.DECIMAL   decimal(15,1)     -none-      
	// RS> 10   s_user_objects_dealloc_MB          java.sql.Types.DECIMAL   decimal(15,1)     -none-      
	// RS> 11   s_user_objects_deferred_dealloc_MB java.sql.Types.DECIMAL   decimal(15,1)     -none-      
	// RS> 12   s_internal_objects_alloc_MB        java.sql.Types.DECIMAL   decimal(15,1)     -none-      
	// RS> 13   s_internal_objects_dealloc_MB      java.sql.Types.DECIMAL   decimal(15,1)     -none-      
	// RS> 14   t_user_objects_alloc_MB            java.sql.Types.DECIMAL   decimal(15,1)     -none-      
	// RS> 15   t_user_objects_dealloc_MB          java.sql.Types.DECIMAL   decimal(15,1)     -none-      
	// RS> 16   t_internal_objects_alloc_MB        java.sql.Types.DECIMAL   decimal(15,1)     -none-      
	// RS> 17   t_internal_objects_dealloc_MB      java.sql.Types.DECIMAL   decimal(15,1)     -none-      
	// RS> 18   dbname                             java.sql.Types.NVARCHAR  nvarchar(128)     -none-      
	// RS> 19   login_name                         java.sql.Types.NVARCHAR  nvarchar(128)     -none-      
	// RS> 20   program_name                       java.sql.Types.NVARCHAR  nvarchar(128)     -none-      
	// RS> 21   host_name                          java.sql.Types.NVARCHAR  nvarchar(128)     -none-      
	// RS> 22   host_process_id                    java.sql.Types.INTEGER   int               -none-      
	// RS> 23   status                             java.sql.Types.NVARCHAR  nvarchar(30)      -none-      
	// RS> 24   open_transaction_count             java.sql.Types.INTEGER   int               -none-      
	// RS> 25   transaction_isolation_level        java.sql.Types.VARCHAR   varchar(28)       -none-      
	// RS> 26   last_request_start_time            java.sql.Types.TIMESTAMP datetime          -none-      
	// RS> 27   last_request_in_sec                java.sql.Types.INTEGER   int               -none-      
	// RS> 28   cpu_time                           java.sql.Types.INTEGER   int               -none-      
	// RS> 29   memory_usage                       java.sql.Types.INTEGER   int               -none-      
	// RS> 30   reads                              java.sql.Types.BIGINT    bigint            -none-      
	// RS> 31   writes                             java.sql.Types.BIGINT    bigint            -none-      
	// RS> 32   logical_reads                      java.sql.Types.BIGINT    bigint            -none-      
	// RS> 33   row_count                          java.sql.Types.BIGINT    bigint            -none-      
	// RS> 34   LastQueryText                      java.sql.Types.NVARCHAR  nvarchar(max)     -none-      

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

		return new CmTempdbSpidUsage(counterController, guiController);
	}

	public CmTempdbSpidUsage(ICounterController counterController, IGuiController guiController)
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
	private static final String  PROP_PREFIX                       = CM_NAME;

	public static final String  PROPKEY_sample_systemThreads       = PROP_PREFIX + ".sample.systemThreads";
	public static final boolean DEFAULT_sample_systemThreads       = true;

	public static final String  PROPKEY_sample_sqlText             = PROP_PREFIX + ".sample.sqlText";
	public static final boolean DEFAULT_sample_sqlText             = false;

	@Override
	protected void registerDefaultValues()
	{
		super.registerDefaultValues();

//		Configuration.registerDefaultValue(PROPKEY_sample_systemThreads, DEFAULT_sample_systemThreads);
		Configuration.registerDefaultValue(PROPKEY_sample_sqlText, DEFAULT_sample_sqlText);
	}
	
	/** Used by the: Create 'Offline Session' Wizard */
	@Override
	public List<CmSettingsHelper> getLocalSettings()
	{
		Configuration conf = Configuration.getCombinedConfiguration();
		List<CmSettingsHelper> list = new ArrayList<>();
		
		list.add(new CmSettingsHelper("Sample System Threads", PROPKEY_sample_systemThreads , Boolean.class, conf.getBooleanProperty(PROPKEY_sample_systemThreads  , DEFAULT_sample_systemThreads  ), DEFAULT_sample_systemThreads, CmTempdbSpidUsagePanel.TOOLTIP_sample_systemThreads ));
		list.add(new CmSettingsHelper("Sample System Threads", PROPKEY_sample_sqlText       , Boolean.class, conf.getBooleanProperty(PROPKEY_sample_sqlText        , DEFAULT_sample_sqlText        ), DEFAULT_sample_sqlText      , CmTempdbSpidUsagePanel.TOOLTIP_sample_sqlText ));

		return list;
	}


	private void addTrendGraphs()
	{
	}

	@Override
	protected TabularCntrPanel createGui()
	{
		return new CmTempdbSpidUsagePanel(this);
	}

	@Override
	public String[] getDependsOnConfigForVersion(Connection conn, long srvVersion, boolean isAzure)
	{
		return NEED_CONFIG;
	}

	@Override
	public Map<String, PcsColumnOptions> getPcsColumnOptions()
	{
		Map<String, PcsColumnOptions> map = super.getPcsColumnOptions();

		// No settings in the super, create one, and set it at super
		if (map == null)
		{
			map = new HashMap<>();
			map.put("LastQueryText", new PcsColumnOptions(ColumnType.DICTIONARY_COMPRESSION));

			// Set the map in the super
			setPcsColumnOptions(map);
		}

		return map;
	}

	@Override
	public List<String> getPkForVersion(Connection conn, long srvVersion, boolean isAzure)
	{
		List <String> pkCols = new LinkedList<String>();

		pkCols.add("session_id");
//		pkCols.add("request_id");

		return pkCols;
	}

	@Override
	public String getSqlForVersion(Connection conn, long srvVersion, boolean isAzure)
	{
		String dm_db_task_space_usage    = "dm_db_task_space_usage";
		String dm_db_session_space_usage = "dm_db_session_space_usage";
		String dm_exec_sessions          = "dm_exec_sessions";
		String dm_exec_connections       = "dm_exec_connections";
		String dm_exec_sql_text          = "dm_exec_sql_text";
		
		if (isAzure)
		{
			dm_db_task_space_usage    = "dm_pdw_nodes_db_task_space_usage";
			dm_db_session_space_usage = "dm_pdw_nodes_db_session_space_usage";
			dm_exec_sessions          = "dm_exec_sessions";                     // same name
			dm_exec_connections       = "dm_pdw_exec_connections";
			dm_exec_sql_text          = "dm_exec_sql_text";                     // same name
		}

		Configuration conf = Configuration.getCombinedConfiguration();
		boolean sample_systemThreads  = conf.getBooleanProperty(PROPKEY_sample_systemThreads, DEFAULT_sample_systemThreads);
		boolean sample_sqlText        = conf.getBooleanProperty(PROPKEY_sample_sqlText      , DEFAULT_sample_sqlText);


		String sql = ""
			    + "; \n"
			    + "WITH \n"
			    + "tmpSess as ( \n"
			    + "    SELECT \n"
			    + "         session_id \n"
			    + "        ,CAST((user_objects_alloc_page_count - user_objects_dealloc_page_count - user_objects_deferred_dealloc_page_count) / 128.0 AS DECIMAL(15, 1)) AS s_user_objects_MB \n"
			    + "        ,CAST( user_objects_alloc_page_count                                                                               / 128.0 AS DECIMAL(15, 1)) AS s_user_objects_alloc_MB \n"
			    + "        ,CAST( user_objects_dealloc_page_count                                                                             / 128.0 AS DECIMAL(15, 1)) AS s_user_objects_dealloc_MB \n"
			    + "        ,CAST( user_objects_deferred_dealloc_page_count                                                                    / 128.0 AS DECIMAL(15, 1)) AS s_user_objects_deferred_dealloc_MB \n"
			    + "        ,CAST((internal_objects_alloc_page_count - internal_objects_dealloc_page_count)                                    / 128.0 AS DECIMAL(15, 1)) AS s_internal_objects_MB \n"
			    + "        ,CAST( internal_objects_alloc_page_count                                                                           / 128.0 AS DECIMAL(15, 1)) AS s_internal_objects_alloc_MB \n"
			    + "        ,CAST( internal_objects_dealloc_page_count                                                                         / 128.0 AS DECIMAL(15, 1)) AS s_internal_objects_dealloc_MB \n"
			    + "    FROM tempdb.sys." + dm_db_session_space_usage +" \n"
			    + "    WHERE (   user_objects_alloc_page_count            > 0 \n"
			    + "           OR user_objects_dealloc_page_count          > 0 \n"
			    + "           OR user_objects_deferred_dealloc_page_count > 0 \n"
			    + "           OR internal_objects_alloc_page_count        > 0 \n"
			    + "           OR internal_objects_dealloc_page_count      > 0 \n"
			    + "          ) \n"
			    + "      AND database_id = DB_ID('tempdb') \n"
			    + "), \n"
			    + "tmlTasks as ( \n"
			    + "    SELECT \n"
			    + "         session_id \n"
			    + "        ,CAST(SUM(user_objects_alloc_page_count - user_objects_dealloc_page_count        ) / 128.0 AS DECIMAL(15, 1)) AS t_user_objects_MB \n"
			    + "        ,CAST(SUM(user_objects_alloc_page_count                                          ) / 128.0 AS DECIMAL(15, 1)) AS t_user_objects_alloc_MB \n"
			    + "        ,CAST(SUM(user_objects_dealloc_page_count                                        ) / 128.0 AS DECIMAL(15, 1)) AS t_user_objects_dealloc_MB \n"
			    + "        ,CAST(SUM(internal_objects_alloc_page_count - internal_objects_dealloc_page_count) / 128.0 AS DECIMAL(15, 1)) AS t_internal_objects_MB \n"
			    + "        ,CAST(SUM(internal_objects_alloc_page_count                                      ) / 128.0 AS DECIMAL(15, 1)) AS t_internal_objects_alloc_MB \n"
			    + "        ,CAST(SUM(internal_objects_dealloc_page_count                                    ) / 128.0 AS DECIMAL(15, 1)) AS t_internal_objects_dealloc_MB \n"
			    + "    FROM tempdb.sys." + dm_db_task_space_usage + " \n"
			    + "    WHERE (   user_objects_alloc_page_count       > 0 \n"
			    + "           OR user_objects_dealloc_page_count     > 0 \n"
			    + "           OR internal_objects_alloc_page_count   > 0 \n"
			    + "           OR internal_objects_dealloc_page_count > 0 \n"
			    + "          ) \n"
			    + "      AND database_id = DB_ID('tempdb') \n"
			    + "    GROUP BY session_id -- Group by since 'session_id' can have several 'exec_context_id' (parallel workers) \n"
			    + ") \n"
			    + "SELECT \n"
			    + "     COALESCE(s.session_id, t.session_id) as [session_id] \n"
			    + "    ,TotalUsageMb_abs     = isnull(s_user_objects_MB,0) + isnull(s_internal_objects_MB,0) + isnull(t_user_objects_MB,0) + isnull(t_internal_objects_MB,0) \n"
			    + "    ,TotalUsageMb         = isnull(s_user_objects_MB,0) + isnull(s_internal_objects_MB,0) + isnull(t_user_objects_MB,0) + isnull(t_internal_objects_MB,0) \n"
			    + "    ,SessUserObjectMb     = isnull(s_user_objects_MB,0) \n"
			    + "    ,SessInternalObjectMb = isnull(s_internal_objects_MB,0) \n"
			    + "    ,TaskUserObjectMb     = isnull(t_user_objects_MB,0) \n"
			    + "    ,TaskInternalObjectMb = isnull(t_internal_objects_MB,0) \n"
			    + "    ,s_user_objects_alloc_MB \n"
			    + "    ,s_user_objects_dealloc_MB \n"
			    + "    ,s_user_objects_deferred_dealloc_MB \n"
			    + "    ,s_internal_objects_alloc_MB \n"
			    + "    ,s_internal_objects_dealloc_MB \n"
			    + "    ,t_user_objects_alloc_MB \n"
			    + "    ,t_user_objects_dealloc_MB \n"
			    + "    ,t_internal_objects_alloc_MB \n"
			    + "    ,t_internal_objects_dealloc_MB \n"
			    + "    ,ES.is_user_process \n"
			    + "    ,dbname = DB_NAME(ES.database_id) \n"
			    + "    ,ES.login_name \n"
			    + "    ,ES.program_name \n"
			    + "    ,ES.host_name \n"
			    + "    ,ES.host_process_id \n"
			    + "    ,ES.status \n"
			    + "    ,ES.open_transaction_count \n"
			    + "    ,transaction_isolation_level = CASE WHEN ES.transaction_isolation_level = 0 THEN 'Unspecified'     + ' - ' + CAST(ES.transaction_isolation_level as varchar(10)) \n"
			    + "                                        WHEN ES.transaction_isolation_level = 1 THEN 'ReadUncommitted' + ' - ' + CAST(ES.transaction_isolation_level as varchar(10)) \n"
			    + "                                        WHEN ES.transaction_isolation_level = 2 THEN 'ReadCommitted'   + ' - ' + CAST(ES.transaction_isolation_level as varchar(10)) \n"
			    + "                                        WHEN ES.transaction_isolation_level = 3 THEN 'RepeatableRead'  + ' - ' + CAST(ES.transaction_isolation_level as varchar(10)) \n"
			    + "                                        WHEN ES.transaction_isolation_level = 4 THEN 'Serializable'    + ' - ' + CAST(ES.transaction_isolation_level as varchar(10)) \n"
			    + "                                        WHEN ES.transaction_isolation_level = 5 THEN 'Snapshot'        + ' - ' + CAST(ES.transaction_isolation_level as varchar(10)) \n"
			    + "                                        ELSE                                         'UNKNOWN'         + ' - ' + CAST(ES.transaction_isolation_level as varchar(10)) \n"
			    + "                                   END \n"
			    + "    ,ES.last_request_start_time \n"
			    + "    ,last_request_in_sec = datediff(second, ES.last_request_start_time, getdate()) \n"
			    + "    ,ES.cpu_time \n"
			    + "    ,ES.memory_usage \n"
			    + "    ,ES.reads \n"
			    + "    ,ES.writes \n"
			    + "    ,ES.logical_reads \n"
			    + "    ,ES.row_count \n"
			    + (!sample_sqlText ? "    ,CAST('-not-enabled-' as nvarchar(max)) AS [LastQueryText] \n" : "    ,TXT.text AS [LastQueryText] \n")
			    + "FROM tmpSess s \n"
			    + "FULL OUTER JOIN tmlTasks t ON s.session_id = t.session_id \n"
			    + "LEFT OUTER JOIN sys." + dm_exec_sessions + "    ES ON ES.session_id = COALESCE(s.session_id, t.session_id) \n"
			    + (!sample_sqlText      ? "" : "LEFT OUTER JOIN sys." + dm_exec_connections + " CN ON CN.session_id = COALESCE(s.session_id, t.session_id) \n")
			    + (!sample_sqlText      ? "" : "OUTER APPLY sys." + dm_exec_sql_text + "(CN.most_recent_sql_handle) TXT \n")
			    + (sample_systemThreads ? "" : "WHERE ES.is_user_process = 1 \n")
			    + "order by 3 desc \n"
			    + "";

		return sql;
	}
}
