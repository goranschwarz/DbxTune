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
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

import org.apache.log4j.Logger;

import com.asetune.ICounterController;
import com.asetune.IGuiController;
import com.asetune.cm.CmSettingsHelper;
import com.asetune.cm.CounterSetTemplates;
import com.asetune.cm.CounterSetTemplates.Type;
import com.asetune.cm.CountersModel;
import com.asetune.cm.sqlserver.gui.CmTempdbSpidUsagePanel;
import com.asetune.gui.MainFrame;
import com.asetune.gui.TabularCntrPanel;
import com.asetune.utils.Configuration;

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
//			"<br>" +
//			"<b>Tip:</b><br>" +
//			"Sort by 'BatchIdDiff', will give you the one that executes the most SQL Batches.<br>" +
//			"Or check 'WaitEventDesc' to find out when the SPID is waiting for." +
//			"<br><br>" +
//			"Table Background colors:" +
//			"<ul>" +
//			"    <li>YELLOW - SPID is a System Processes</li>" +
//			"    <li>GREEN  - SPID is Executing(running) or are in the Run Queue Awaiting a time slot to Execute (runnable)</li>" +
//			"    <li>PINK   - SPID is Blocked by some other SPID that holds a Lock on a database object Table, Page or Row. This is the Lock Victim.</li>" +
//			"    <li>ORANGE - SPID has an open transaction.</li>" +
//			"    <li>RED    - SPID is Blocking other SPID's from running, this SPID is Responsible or the Root Cause of a Blocking Lock.</li>" +
//			"</ul>" +
		"</html>";

	public static final String   GROUP_NAME       = MainFrame.TCP_GROUP_SERVER;
	public static final String   GUI_ICON_FILE    = "images/"+CM_NAME+".png";

	public static final long     NEED_SRV_VERSION = 0;
	public static final long     NEED_CE_VERSION  = 0;

	public static final String[] MON_TABLES       = new String[] {"dm_db_task_space_usage", "dm_db_session_space_usage", "dm_exec_connections", "dm_exec_requests", "dm_exec_sql_text"};
	public static final String[] NEED_ROLES       = new String[] {};
	public static final String[] NEED_CONFIG      = new String[] {};

	public static final String[] PCT_COLUMNS      = new String[] {};
	public static final String[] DIFF_COLUMNS     = new String[] {
		"TotalAllocationUserObjectsMb",
		"CurrentAllocationUserObjectsMb",
		"TotalAllocationInternalObjectsMb",
		"CurrentAllocationInternalObjectsMb",
		"TotalAllocationMb",
		"CurrentAllocationMb"
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

	@Override
	protected void registerDefaultValues()
	{
		super.registerDefaultValues();

		Configuration.registerDefaultValue(PROPKEY_sample_systemThreads, DEFAULT_sample_systemThreads);
	}
	
	/** Used by the: Create 'Offline Session' Wizard */
	@Override
	public List<CmSettingsHelper> getLocalSettings()
	{
		Configuration conf = Configuration.getCombinedConfiguration();
		List<CmSettingsHelper> list = new ArrayList<>();
		
		list.add(new CmSettingsHelper("Sample System Threads", PROPKEY_sample_systemThreads , Boolean.class, conf.getBooleanProperty(PROPKEY_sample_systemThreads  , DEFAULT_sample_systemThreads  ), DEFAULT_sample_systemThreads, CmTempdbSpidUsagePanel.TOOLTIP_sample_systemThreads ));

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
		String dm_exec_connections       = "dm_exec_connections";
		
		if (isAzure)
		{
			dm_db_task_space_usage    = "dm_pdw_nodes_db_task_space_usage";
			dm_db_session_space_usage = "dm_pdw_nodes_db_session_space_usage";
			dm_exec_connections       = "dm_pdw_exec_connections";
		}

		Configuration conf = Configuration.getCombinedConfiguration();
		boolean sample_systemThreads  = conf.getBooleanProperty(PROPKEY_sample_systemThreads, DEFAULT_sample_systemThreads);

		// Should we sample SYSTEM SPID's
//		String sql_sample_systemThreads = "--and sid != 0x01 -- Property: "+PROPKEY_sample_systemThreads+" is "+sample_systemThreads+". \n";
//		if ( ! sample_systemThreads )
//			sql_sample_systemThreads = "  and sid != 0x01 -- Property: "+PROPKEY_sample_systemThreads+" is "+sample_systemThreads+". \n";
		String sql_sample_systemThreads = "--and net_address != '' -- Property: "+PROPKEY_sample_systemThreads+" is "+sample_systemThreads+". \n";
		if ( ! sample_systemThreads )
			sql_sample_systemThreads = "  and net_address != '' -- Property: "+PROPKEY_sample_systemThreads+" is "+sample_systemThreads+". \n";

		sql_sample_systemThreads = "";

		// below is grabbed from: https://www.mssqltips.com/sqlservertip/4356/track-sql-server-tempdb-space-usage/
		// But it needs more work:
		//  - Grab user_name in some way from (dm_exec_requests || dm_exec_connections)
		
		String sql = 
			"SELECT  COALESCE(T1.session_id, T2.session_id) as [session_id] , \n" +
			"        T1.request_id , \n" +
			"        COALESCE(T1.database_id, T2.database_id) as [database_id], \n" +
			"        db_name(COALESCE(T1.database_id, T2.database_id)) as [database_name], \n" +
			"        COALESCE(T1.[TotalAllocationUserObjects],       0) + T2.[TotalAllocationUserObjects]       as [TotalAllocationUserObjectsMb], \n" +
			"        COALESCE(T1.[CurrentAllocationUserObjects],     0) + T2.[CurrentAllocationUserObjects]     as [CurrentAllocationUserObjectsMb], \n" +
			"        COALESCE(T1.[TotalAllocationInternalObjects],   0) + T2.[TotalAllocationInternalObjects]   as [TotalAllocationInternalObjectsMb] , \n" +
			"        COALESCE(T1.[CurrentAllocationInternalObjects], 0) + T2.[CurrentAllocationInternalObjects] as [CurrentAllocationInternalObjectsMb] , \n" +
			"        COALESCE(T1.[TotalAllocation],                  0) + T2.[TotalAllocation]                  as [TotalAllocationMb] , \n" +
			"        COALESCE(T1.[CurrentAllocation],                0) + T2.[CurrentAllocation]                as [CurrentAllocationMb] , \n" +
			"        COALESCE(T1.[QueryText], T2.[QueryText])                                                   as [LastQueryText] \n" +
			"FROM    ( SELECT    TS.session_id , \n" +
			"                    TS.request_id , \n" +
			"                    TS.database_id , \n" +
			"                    CAST(  TS.user_objects_alloc_page_count                                                / 128.0 AS DECIMAL(15, 1)) [TotalAllocationUserObjects] , \n" +
			"                    CAST(( TS.user_objects_alloc_page_count     - TS.user_objects_dealloc_page_count )     / 128.0 AS DECIMAL(15, 1)) [CurrentAllocationUserObjects] , \n" +
			"                    CAST(  TS.internal_objects_alloc_page_count                                            / 128.0 AS DECIMAL(15, 1)) [TotalAllocationInternalObjects] , \n" +
			"                    CAST(( TS.internal_objects_alloc_page_count - TS.internal_objects_dealloc_page_count ) / 128.0 AS DECIMAL(15, 1)) [CurrentAllocationInternalObjects] , \n" +
			"                    CAST(( TS.user_objects_alloc_page_count     + TS.internal_objects_alloc_page_count )   / 128.0 AS DECIMAL(15, 1)) [TotalAllocation] , \n" +
			"                    CAST(( TS.user_objects_alloc_page_count     + TS.internal_objects_alloc_page_count - TS.internal_objects_dealloc_page_count - TS.user_objects_dealloc_page_count ) / 128.0 AS DECIMAL(15, 1)) [CurrentAllocation] , \n" +
			"                    T.text [QueryText] \n" +
			"          FROM      tempdb.sys." + dm_db_task_space_usage + " TS \n" +
			"                    INNER JOIN sys.dm_exec_requests ER ON ER.request_id = TS.request_id AND ER.session_id = TS.session_id \n" +
			"                    OUTER APPLY sys.dm_exec_sql_text(ER.sql_handle) T \n" +
			"          WHERE TS.user_objects_alloc_page_count       > 0 \n" + 
			"             OR TS.user_objects_dealloc_page_count     > 0 \n" +
			"             OR TS.internal_objects_alloc_page_count   > 0 \n" +
			"             OR TS.internal_objects_dealloc_page_count > 0 \n" +
			"        ) T1 \n" +
			"        RIGHT JOIN ( SELECT SS.session_id , \n" +
			"                            SS.database_id , \n" +
			"                            CAST(  SS.user_objects_alloc_page_count                                                / 128.0 AS DECIMAL(15, 1)) [TotalAllocationUserObjects] , \n" +
			"                            CAST(( SS.user_objects_alloc_page_count     - SS.user_objects_dealloc_page_count )     / 128.0 AS DECIMAL(15, 1)) [CurrentAllocationUserObjects] , \n" +
			"                            CAST(  SS.internal_objects_alloc_page_count                                            / 128.0 AS DECIMAL(15, 1)) [TotalAllocationInternalObjects] , \n" +
			"                            CAST(( SS.internal_objects_alloc_page_count - SS.internal_objects_dealloc_page_count ) / 128.0 AS DECIMAL(15, 1)) [CurrentAllocationInternalObjects] , \n" +
			"                            CAST(( SS.user_objects_alloc_page_count     + SS.internal_objects_alloc_page_count )   / 128.0 AS DECIMAL(15, 1)) [TotalAllocation] , \n" +
			"                            CAST(( SS.user_objects_alloc_page_count     + SS.internal_objects_alloc_page_count - SS.internal_objects_dealloc_page_count - SS.user_objects_dealloc_page_count ) / 128.0 AS DECIMAL(15, 1)) [CurrentAllocation] , \n" +
			"                            T.text [QueryText] \n" +
			"                     FROM   tempdb.sys." + dm_db_session_space_usage + " SS \n" +
			"                            LEFT JOIN sys." + dm_exec_connections + " CN ON CN.session_id = SS.session_id \n" +
			"                            OUTER APPLY sys.dm_exec_sql_text(CN.most_recent_sql_handle) T \n" +
			"                     WHERE SS.user_objects_alloc_page_count       > 0 \n" + 
			"                        OR SS.user_objects_dealloc_page_count     > 0 \n" +
			"                        OR SS.internal_objects_alloc_page_count   > 0 \n" +
			"                        OR SS.internal_objects_dealloc_page_count > 0 \n" +
			"                   ) T2 ON T1.session_id = T2.session_id \n" +
			sql_sample_systemThreads;

		return sql;
	}
}
