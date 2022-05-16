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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import org.apache.log4j.Logger;

import com.asetune.ICounterController;
import com.asetune.IGuiController;
import com.asetune.alarm.AlarmHandler;
import com.asetune.alarm.events.AlarmEvent;
import com.asetune.alarm.events.sqlserver.AlarmEventTempdbSpidUsage;
import com.asetune.cm.CmSettingsHelper;
import com.asetune.cm.CmSettingsHelper.RegExpInputValidator;
import com.asetune.cm.CounterSetTemplates;
import com.asetune.cm.CounterSetTemplates.Type;
import com.asetune.cm.CountersModel;
import com.asetune.cm.sqlserver.gui.CmTempdbSpidUsagePanel;
import com.asetune.gui.MainFrame;
import com.asetune.gui.TabularCntrPanel;
import com.asetune.pcs.PcsColumnOptions;
import com.asetune.pcs.PcsColumnOptions.ColumnType;
import com.asetune.sql.conn.DbxConnection;
import com.asetune.sql.conn.info.DbmsVersionInfo;
import com.asetune.sql.conn.info.DbmsVersionInfoSqlServer;
import com.asetune.utils.Configuration;
import com.asetune.utils.StringUtil;
import com.asetune.utils.Ver;

/**
 * @author Goran Schwarz (goran_schwarz@hotmail.com)
 */
public class CmTempdbSpidUsage
extends CountersModel
{
	private static Logger        _logger          = Logger.getLogger(CmTempdbSpidUsage.class);
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

	public static final String  PROPKEY_sample_TotalUsageMb_includeInternalObjects = PROP_PREFIX + ".sample.TotalUsageMb.includeInternalObjects";
	public static final boolean DEFAULT_sample_TotalUsageMb_includeInternalObjects = false;

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
		
		list.add(new CmSettingsHelper("Sample System Threads"                     , PROPKEY_sample_systemThreads                      , Boolean.class, conf.getBooleanProperty(PROPKEY_sample_systemThreads                      , DEFAULT_sample_systemThreads                      ), DEFAULT_sample_systemThreads                      , CmTempdbSpidUsagePanel.TOOLTIP_sample_systemThreads ));
		list.add(new CmSettingsHelper("Sample SQL Text"                           , PROPKEY_sample_sqlText                            , Boolean.class, conf.getBooleanProperty(PROPKEY_sample_sqlText                            , DEFAULT_sample_sqlText                            ), DEFAULT_sample_sqlText                            , CmTempdbSpidUsagePanel.TOOLTIP_sample_sqlText ));
		list.add(new CmSettingsHelper("Include 'InternalObject' in 'TotalUsageMb'", PROPKEY_sample_TotalUsageMb_includeInternalObjects, Boolean.class, conf.getBooleanProperty(PROPKEY_sample_TotalUsageMb_includeInternalObjects, DEFAULT_sample_TotalUsageMb_includeInternalObjects), DEFAULT_sample_TotalUsageMb_includeInternalObjects, CmTempdbSpidUsagePanel.TOOLTIP_sample_TotalUsageMb_includeInternalObjects ));

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
	public String[] getDependsOnConfigForVersion(DbxConnection conn, DbmsVersionInfo versionInfo)
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
	public List<String> getPkForVersion(DbxConnection conn, DbmsVersionInfo versionInfo)
	{
		List <String> pkCols = new LinkedList<String>();

		pkCols.add("session_id");
//		pkCols.add("request_id");

		return pkCols;
	}

	@Override
	public String getSqlForVersion(DbxConnection conn, DbmsVersionInfo versionInfo)
	{
		DbmsVersionInfoSqlServer ssVersionInfo = (DbmsVersionInfoSqlServer) versionInfo;
		long srvVersion = ssVersionInfo.getLongVersion();

		String dm_db_task_space_usage    = "dm_db_task_space_usage";
		String dm_db_session_space_usage = "dm_db_session_space_usage";
		String dm_exec_sessions          = "dm_exec_sessions";
		String dm_exec_connections       = "dm_exec_connections";
		String dm_exec_sql_text          = "dm_exec_sql_text";
		
		if (ssVersionInfo.isAzureSynapseAnalytics())
		{
			dm_db_task_space_usage    = "dm_pdw_nodes_db_task_space_usage";
			dm_db_session_space_usage = "dm_pdw_nodes_db_session_space_usage";
			dm_exec_sessions          = "dm_exec_sessions";                     // same name
			dm_exec_connections       = "dm_pdw_exec_connections";
			dm_exec_sql_text          = "dm_exec_sql_text";                     // same name
		}

		Configuration conf = Configuration.getCombinedConfiguration();
		boolean sample_systemThreads          = conf.getBooleanProperty(PROPKEY_sample_systemThreads                      , DEFAULT_sample_systemThreads);
		boolean sample_sqlText                = conf.getBooleanProperty(PROPKEY_sample_sqlText                            , DEFAULT_sample_sqlText);
		boolean sample_includeInternalObjects = conf.getBooleanProperty(PROPKEY_sample_TotalUsageMb_includeInternalObjects, DEFAULT_sample_TotalUsageMb_includeInternalObjects);


		String user_objects_deferred_dealloc_page_count = "0";
		if (srvVersion >= Ver.ver(2014))
		{
			user_objects_deferred_dealloc_page_count = "user_objects_deferred_dealloc_page_count";
		}

		// TotalUsageMb, TotalUsageMb_abs
		String TotalUsageMb     = "    ,TotalUsageMb         = isnull(s_user_objects_MB,0) + isnull(t_user_objects_MB,0) \n";
		String TotalUsageMb_abs = "    ,TotalUsageMb_abs     = isnull(s_user_objects_MB,0) + isnull(t_user_objects_MB,0) \n";
		if (sample_includeInternalObjects)
		{
			TotalUsageMb     = "    ,TotalUsageMb         = isnull(s_user_objects_MB,0) + isnull(s_internal_objects_MB,0) + isnull(t_user_objects_MB,0) + isnull(t_internal_objects_MB,0) \n";
			TotalUsageMb_abs = "    ,TotalUsageMb_abs     = isnull(s_user_objects_MB,0) + isnull(s_internal_objects_MB,0) + isnull(t_user_objects_MB,0) + isnull(t_internal_objects_MB,0) \n";
		}
		                                                                                                                                                                                                         
		String sql = ""
			    + "; \n"
			    + "WITH \n"
			    + "tmpSess as ( \n"
			    + "    SELECT /* ${cmCollectorName} */ \n"
			    + "         session_id \n"
			    + "        ,CAST((user_objects_alloc_page_count - user_objects_dealloc_page_count - " + user_objects_deferred_dealloc_page_count + ") / 128.0 AS DECIMAL(15, 1)) AS s_user_objects_MB \n"
			    + "        ,CAST( user_objects_alloc_page_count                                                                               / 128.0 AS DECIMAL(15, 1)) AS s_user_objects_alloc_MB \n"
			    + "        ,CAST( user_objects_dealloc_page_count                                                                             / 128.0 AS DECIMAL(15, 1)) AS s_user_objects_dealloc_MB \n"
			    + "        ,CAST( " + user_objects_deferred_dealloc_page_count + "                                                                    / 128.0 AS DECIMAL(15, 1)) AS s_user_objects_deferred_dealloc_MB \n"
			    + "        ,CAST((internal_objects_alloc_page_count - internal_objects_dealloc_page_count)                                    / 128.0 AS DECIMAL(15, 1)) AS s_internal_objects_MB \n"
			    + "        ,CAST( internal_objects_alloc_page_count                                                                           / 128.0 AS DECIMAL(15, 1)) AS s_internal_objects_alloc_MB \n"
			    + "        ,CAST( internal_objects_dealloc_page_count                                                                         / 128.0 AS DECIMAL(15, 1)) AS s_internal_objects_dealloc_MB \n"
			    + "    FROM tempdb.sys." + dm_db_session_space_usage +" \n"
			    + "    WHERE (   user_objects_alloc_page_count            > 0 \n"
			    + "           OR user_objects_dealloc_page_count          > 0 \n"
			    + "           OR " + user_objects_deferred_dealloc_page_count + " > 0 \n"
			    + "           OR internal_objects_alloc_page_count        > 0 \n"
			    + "           OR internal_objects_dealloc_page_count      > 0 \n"
			    + "          ) \n"
			    + "      AND database_id = DB_ID('tempdb') \n"
			    + "), \n"
			    + "tmlTasks as ( \n"
			    + "    SELECT /* ${cmCollectorName} */ \n"
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
			    + "SELECT /* ${cmCollectorName} */ \n"
			    + "     COALESCE(s.session_id, t.session_id) as [session_id] \n"
			    + TotalUsageMb_abs
			    + TotalUsageMb
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
	@Override
	public void sendAlarmRequest()
	{
		if ( ! hasDiffData() )
			return;
		
		if ( ! AlarmHandler.hasInstance() )
			return;
		
		// EXIT EARLY if no alarm properties has been specified (since there can be *many* logins)
		boolean isAnyAlarmEnabled = false;
		if (isSystemAlarmsForColumnEnabledAndInTimeRange("TotalUsageMb_abs")) isAnyAlarmEnabled = true;
//		if (isSystemAlarmsForColumnEnabledAndInTimeRange("xxxxxxxxxxx"     )) isAnyAlarmEnabled = true;

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
			if (isSystemAlarmsForColumnEnabledAndInTimeRange("TotalUsageMb_abs"))
			{
				Object o_TotalUsageMb_abs = cm.getAbsValue(r, "TotalUsageMb_abs");
				if (o_TotalUsageMb_abs != null && o_TotalUsageMb_abs instanceof Number)
				{
					int TotalUsageMb_abs = ((Number)o_TotalUsageMb_abs).intValue();
					
					int threshold = Configuration.getCombinedConfiguration().getIntProperty(PROPKEY_alarm_TotalUsageMb_abs, DEFAULT_alarm_TotalUsageMb_abs);

					if (debugPrint || _logger.isDebugEnabled())
						System.out.println("##### sendAlarmRequest("+cm.getName()+"): threshold="+threshold+", TotalUsageMb_abs='"+TotalUsageMb_abs+"'.");

					if (TotalUsageMb_abs > threshold)
					{
						// Get config 'skip some known values'
						String skipLoginRegExp    = Configuration.getCombinedConfiguration().getProperty(PROPKEY_alarm_TotalUsageMb_abs_SkipLogin,    DEFAULT_alarm_TotalUsageMb_abs_SkipLogin);
						String skipProgramRegExp  = Configuration.getCombinedConfiguration().getProperty(PROPKEY_alarm_TotalUsageMb_abs_SkipProgram,  DEFAULT_alarm_TotalUsageMb_abs_SkipProgram);
//						String skipCmdRegExp      = Configuration.getCombinedConfiguration().getProperty(PROPKEY_alarm_TotalUsageMb_abs_SkipCmd,      DEFAULT_alarm_TotalUsageMb_abs_SkipCmd);
//						String skipTranNameRegExp = Configuration.getCombinedConfiguration().getProperty(PROPKEY_alarm_TotalUsageMb_abs_SkipTranName, DEFAULT_alarm_TotalUsageMb_abs_SkipTranName);

						String session_id              = cm.getAbsValue(r, "session_id")              + "";
						String last_request_start_time = cm.getAbsValue(r, "last_request_start_time") + "";
						String last_request_in_sec     = cm.getAbsValue(r, "last_request_in_sec") + "";
//						String StatementStartTime      = cm.getAbsValue(r, "exec_start_time")         + "";
						String login_name              = cm.getAbsValue(r, "login_name")              + "";
						String program_name            = cm.getAbsValue(r, "program_name")            + "";
//						String Command                 = cm.getAbsValue(r, "command")                 + "";
//						String tran_name               = cm.getAbsValue(r, "transaction_name")        + "";
						
						// note: this must be set to true at start, otherwise all below rules will be disabled (it "stops" processing at first doAlarm==false)
						boolean doAlarm = true;

						// The below could have been done with nested if(!skipXxx), if(!skipYyy) doAlarm=true; 
						// Below is more readable, from a variable context point-of-view, but HARDER to understand
						doAlarm = (doAlarm && (StringUtil.isNullOrBlank(skipLoginRegExp)    || ! login_name  .matches(skipLoginRegExp   ))); // NO match in the SKIP Cmd          regexp
						doAlarm = (doAlarm && (StringUtil.isNullOrBlank(skipProgramRegExp)  || ! program_name.matches(skipProgramRegExp ))); // NO match in the SKIP program_name regexp
//						doAlarm = (doAlarm && (StringUtil.isNullOrBlank(skipCmdRegExp)      || ! Command     .matches(skipCmdRegExp     ))); // NO match in the SKIP Cmd      regexp
//						doAlarm = (doAlarm && (StringUtil.isNullOrBlank(skipTranNameRegExp) || ! tran_name   .matches(skipTranNameRegExp))); // NO match in the SKIP TranName regexp

						// NO match in the SKIP regEx
						if (doAlarm)
						{
							String extendedDescText = cm.toTextTableString(DATA_RATE, r);
							String extendedDescHtml = cm.toHtmlTableString(DATA_RATE, r, true, false, false);
													
							AlarmEvent ae = new AlarmEventTempdbSpidUsage(cm, threshold, session_id, TotalUsageMb_abs, last_request_start_time, last_request_in_sec, login_name, program_name);
							ae.setExtendedDescription(extendedDescText, extendedDescHtml);
						
							alarmHandler.addAlarm( ae );
						}
					} // end: above threshold
				} // end: is number
			} // end: StatementExecInSec

		} // end: loop rows
	}

	public static final String  PROPKEY_alarm_TotalUsageMb_abs               = CM_NAME + ".alarm.system.if.TotalUsageMb_abs.gt";
	public static final int     DEFAULT_alarm_TotalUsageMb_abs               = 20 * 1024; // 20 GB

	public static final String  PROPKEY_alarm_TotalUsageMb_abs_SkipLogin    = CM_NAME + ".alarm.system.if.TotalUsageMb_abs.skip.login";
	public static final String  DEFAULT_alarm_TotalUsageMb_abs_SkipLogin    = "";

	public static final String  PROPKEY_alarm_TotalUsageMb_abs_SkipProgram  = CM_NAME + ".alarm.system.if.TotalUsageMb_abs.skip.program_name";
	public static final String  DEFAULT_alarm_TotalUsageMb_abs_SkipProgram  = "";

//	public static final String  PROPKEY_alarm_TotalUsageMb_abs_SkipCmd      = CM_NAME + ".alarm.system.if.TotalUsageMb_abs.skip.cmd";
//	public static final String  DEFAULT_alarm_TotalUsageMb_abs_SkipCmd      = "";
//
//	public static final String  PROPKEY_alarm_TotalUsageMb_abs_SkipTranName = CM_NAME + ".alarm.system.if.TotalUsageMb_abs.skip.tranName";
//	public static final String  DEFAULT_alarm_TotalUsageMb_abs_SkipTranName = "";

	@Override
	public List<CmSettingsHelper> getLocalAlarmSettings()
	{
		Configuration conf = Configuration.getCombinedConfiguration();
		List<CmSettingsHelper> list = new ArrayList<>();

		CmSettingsHelper.Type isAlarmSwitch = CmSettingsHelper.Type.IS_ALARM_SWITCH;

		list.add(new CmSettingsHelper("TotalUsageMb_abs",           isAlarmSwitch, PROPKEY_alarm_TotalUsageMb_abs             , Integer.class, conf.getIntProperty    (PROPKEY_alarm_TotalUsageMb_abs             , DEFAULT_alarm_TotalUsageMb_abs             ), DEFAULT_alarm_TotalUsageMb_abs             , "If any SPID's is using TotalUsageMb_abs more than #### MB, then send alarm 'AlarmEventTempdbSpidUsage'." ));
		list.add(new CmSettingsHelper("TotalUsageMb_abs SkipLogins",               PROPKEY_alarm_TotalUsageMb_abs_SkipLogin   , String .class, conf.getProperty       (PROPKEY_alarm_TotalUsageMb_abs_SkipLogin   , DEFAULT_alarm_TotalUsageMb_abs_SkipLogin   ), DEFAULT_alarm_TotalUsageMb_abs_SkipLogin   , "If 'TotalUsageMb_abs' is true; Discard Logins listed (regexp is used)."         , new RegExpInputValidator()));
		list.add(new CmSettingsHelper("TotalUsageMb_abs SkipCommands",             PROPKEY_alarm_TotalUsageMb_abs_SkipProgram , String .class, conf.getProperty       (PROPKEY_alarm_TotalUsageMb_abs_SkipProgram , DEFAULT_alarm_TotalUsageMb_abs_SkipProgram ), DEFAULT_alarm_TotalUsageMb_abs_SkipProgram , "If 'TotalUsageMb_abs' is true; Discard program_name's listed (regexp is used)." , new RegExpInputValidator()));
//		list.add(new CmSettingsHelper("TotalUsageMb_abs SkipCommands",             PROPKEY_alarm_TotalUsageMb_abs_SkipCmd     , String .class, conf.getProperty       (PROPKEY_alarm_TotalUsageMb_abs_SkipCmd     , DEFAULT_alarm_TotalUsageMb_abs_SkipCmd     ), DEFAULT_alarm_TotalUsageMb_abs_SkipCmd     , "If 'TotalUsageMb_abs' is true; Discard Commands listed (regexp is used)." , new RegExpInputValidator()));
//		list.add(new CmSettingsHelper("TotalUsageMb_abs SkipTranNames",            PROPKEY_alarm_TotalUsageMb_abs_SkipTranName, String .class, conf.getProperty       (PROPKEY_alarm_TotalUsageMb_abs_SkipTranName, DEFAULT_alarm_TotalUsageMb_abs_SkipTranName), DEFAULT_alarm_TotalUsageMb_abs_SkipTranName, "If 'TotalUsageMb_abs' is true; Discard TranName listed (regexp is used)." , new RegExpInputValidator()));

		return list;
	}
}
