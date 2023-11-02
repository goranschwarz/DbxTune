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

import java.util.LinkedList;
import java.util.List;

import com.asetune.ICounterController;
import com.asetune.IGuiController;
import com.asetune.cm.CounterSetTemplates;
import com.asetune.cm.CounterSetTemplates.Type;
import com.asetune.cm.CountersModel;
import com.asetune.gui.MainFrame;
import com.asetune.sql.conn.DbxConnection;
import com.asetune.sql.conn.info.DbmsVersionInfo;
import com.asetune.utils.Configuration;

/**
 * @author Goran Schwarz (goran_schwarz@hotmail.com)
 */
public class CmPlanCacheHistory
extends CountersModel
{
//	private static Logger        _logger          = Logger.getLogger(CmServiceMemory.class);
	private static final long    serialVersionUID = 1L;

	public static final String   CM_NAME          = CmPlanCacheHistory.class.getSimpleName();
	public static final String   SHORT_NAME       = "Plan Cache History";
	public static final String   HTML_DESC        = 
		"<html> \n" +
		"<p>How much Query Plan History does SQL Server have available?</p> \n" +
		"Looking at the top ## plans in the cache doesn't do me much good if: \n" +
		"<ul> \n" +
		"    <li>Someone restarted the server recently</li> \n" +
		"    <li>Someone ran DBCC FREEPROCCACHE</li> \n" +
		"    <li>Somebody's addicted to rebuilding indexes and updating stats (which invalidates plans for all affected objects)</li> \n" +
		"    <li>The server's under extreme memory pressure</li> \n" +
		"    <li>The developers <i>aren't parameterizing their queries</i></li> \n" +
		"    <li>The app has an <i>old version of NHibernate with the parameterization bug</i></li> \n" +
		"    <li>The .NET app calls <i>Parameters.Add without setting the parameter size</i></li> \n" +
//		"    <li>The Java app gets <code>CONVERT_IMPLICIT</code> when call <i><code>stmnt.setString()</code> on columns that has VARCHAR... Workaround: set connection parameter <code>sendStringParametersAsUnicode=false</code></i></li> \n" +
		"</ul> \n" +
		"</html>";

	public static final String   GROUP_NAME       = MainFrame.TCP_GROUP_OBJECT_ACCESS;
	public static final String   GUI_ICON_FILE    = "images/"+CM_NAME+".png";

	public static final long     NEED_SRV_VERSION = 0;
	public static final long     NEED_CE_VERSION  = 0;

	public static final String[] MON_TABLES       = new String[] {"dm_exec_query_stats"};
	public static final String[] NEED_ROLES       = new String[] {"VIEW SERVER STATE"};
	public static final String[] NEED_CONFIG      = new String[] {};

	public static final String[] PCT_COLUMNS      = new String[] {
		 "ObjType_Proc_Pct"
		,"ObjType_Prepared_Pct"
		,"ObjType_Adhoc_Pct"
		,"ObjType_ReplProc_Pct"
		,"ObjType_Trigger_Pct"
		,"ObjType_View_Pct"
		,"ObjType_Default_Pct"
		,"ObjType_UsrTab_Pct"
		,"ObjType_SysTab_Pct"
		,"ObjType_Check_Pct"
		,"ObjType_Rule_Pct"
		,"CacheObjType_CompiledPlan_Pct"
		,"CacheObjType_CompiledPlanStub_Pct"
		,"CacheObjType_ParseTree_Pct"
		,"CacheObjType_ExtendedProc_Pct"
		,"CacheObjType_ClrCompiledFunc_Pct"
		,"CacheObjType_ClrCompiledProc_Pct"
	};
	public static final String[] DIFF_COLUMNS     = new String[] {
		 "plan_count_diff"
		,"exec_count_diff"

		,"total_elapsed_time_ms"
		,"total_worker_time_ms"
		,"total_logical_reads"
		,"total_logical_writes"
		,"total_physical_reads"
		,"total_clr_time_ms"
		,"total_rows"
		,"total_dop"
		,"total_grant_kb"
		,"total_used_grant_kb"
		,"total_ideal_grant_kb"
		,"total_reserved_threads"
		,"total_used_threads"
		,"total_columnstore_segment_reads"
		,"total_columnstore_segment_skips"
		,"total_spills"
		,"total_page_server_reads"
	};

	public static final boolean  NEGATIVE_DIFF_COUNTERS_TO_ZERO = false;
	public static final boolean  IS_SYSTEM_CM                   = true;
	public static final int      DEFAULT_POSTPONE_TIME          = 0;
	public static final int      DEFAULT_QUERY_TIMEOUT          = CountersModel.DEFAULT_sqlQueryTimeout;;

	@Override public int     getDefaultPostponeTime()                 { return 3600; } // 1 Hour
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

		return new CmPlanCacheHistory(counterController, guiController);
	}

	public CmPlanCacheHistory(ICounterController counterController, IGuiController guiController)
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
	private static final String  PROP_PREFIX            = CM_NAME;
	
	public static final String  PROPKEY_sample_top_rows = PROP_PREFIX + ".sample.top";
	public static final int     DEFAULT_sample_top_rows = 1000;
	
	private void addTrendGraphs()
	{
	}

//	@Override
//	protected TabularCntrPanel createGui()
//	{
//		return new CmRaSysmonPanel(this);
//	}

	@Override
	public String[] getDependsOnConfigForVersion(DbxConnection conn, DbmsVersionInfo versionInfo)
	{
		return NEED_CONFIG;
	}

	@Override
	public List<String> getPkForVersion(DbxConnection conn, DbmsVersionInfo versionInfo)
	{
//		return null;
		List <String> pkCols = new LinkedList<String>();

		pkCols.add("creation_date");
		pkCols.add("creation_hour");

		return pkCols;
	}

	@Override
	public String getSqlForVersion(DbxConnection conn, DbmsVersionInfo versionInfo)
	{
//		String dm_exec_query_stats = "dm_exec_query_stats";
		
//		if (isAzure)
//			dm_exec_query_stats = "dm_exec_query_stats";

		// FIXME: possibly we can check for
		//          * Spill to disk
		//			* CONVERT_IMPLICIT 
		//			* memory grant issues
		//			* etc, etc
		//        by "checking" the plans for various warnings/errors
		//        and add each "warning" as a specific column to the below SQL Statement

		int topRows = Configuration.getCombinedConfiguration().getIntProperty(PROPKEY_sample_top_rows, DEFAULT_sample_top_rows);
		
//		String sql = ""
//			    + "/* Below SQL is from -- https://www.brentozar.com/archive/2018/07/tsql2sday-how-much-plan-cache-history-do-you-have/ */ \n"
//			    + "SELECT TOP " + topRows + " /* ${cmCollectorName} */ \n"
//			    + "     creation_date   = CAST(creation_time AS date) \n"
//			    + "    ,creation_hour   = CASE \n"
//			    + "                           WHEN CAST(creation_time AS date) <> CAST(GETDATE() AS date) THEN -1 \n"
//			    + "                           ELSE DATEPART(hh, creation_time) \n"
//			    + "                       END \n"
//
//			    + "    ,plan_count      = SUM(1) \n"
//			    + "    ,exec_count      = SUM(execution_count) \n"
//
//			    + "    ,plan_count_diff = SUM(1) \n"
//			    + "    ,exec_count_diff = SUM(execution_count) \n"
//
//			    + "FROM sys." + dm_exec_query_stats + " \n"
//			    + "GROUP BY CAST(creation_time AS date), \n"
//			    + "         CASE \n"
//			    + "             WHEN CAST(creation_time AS date) <> CAST(GETDATE() AS date) THEN -1 \n"
//			    + "             ELSE DATEPART(hh, creation_time) \n"
//			    + "         END \n"
//			    + "ORDER BY 1 DESC, 2 DESC \n"
//			    + "";

		String sql = ""
			    + "SELECT TOP " + topRows + " /* ${cmCollectorName} */ \n"
			    + "     creation_date   = ISNULL( CAST(qs.creation_time AS date) , '9999-12-31') \n"
			    + "    ,creation_hour   = ISNULL( CASE \n"
			    + "                           WHEN CAST(qs.creation_time AS date) <> CAST(GETDATE() AS date) THEN -1 \n"
			    + "                           ELSE DATEPART(hh, qs.creation_time) \n"
			    + "                       END, -99) \n"
			    + " \n"
			    + "    ,plan_count            = SUM(1) \n"
			    + "    ,exec_count            = SUM(qs.execution_count) \n"
			    + " \n"
			    + "    ,plan_count_diff       = SUM(1) \n"
			    + "    ,exec_count_diff       = SUM(qs.execution_count) \n"
			    + " \n"
			    + "    ,PlanSizeKB            = CAST(SUM(ecp.size_in_bytes / 1024.0) AS numeric(12,1)) \n"
			    + " \n"
			    + "    ,ObjType_Proc_Pct      = CAST(SUM(CASE WHEN objtype = N'Proc'      THEN 1.0 ELSE 0.0 END) / SUM(1.0) * 100.0 as numeric(6,1))\n"
			    + "    ,ObjType_Prepared_Pct  = CAST(SUM(CASE WHEN objtype = N'Prepared'  THEN 1.0 ELSE 0.0 END) / SUM(1.0) * 100.0 as numeric(6,1)) \n"
			    + "    ,ObjType_Adhoc_Pct     = CAST(SUM(CASE WHEN objtype = N'Adhoc'     THEN 1.0 ELSE 0.0 END) / SUM(1.0) * 100.0 as numeric(6,1)) \n"
//			    + "--    ,ObjType_ReplProc_Pct  = CAST(SUM(CASE WHEN objtype = N'ReplProc'  THEN 1.0 ELSE 0.0 END) / SUM(1.0) * 100.0 as numeric(6,1)) \n"
//			    + "--    ,ObjType_Trigger_Pct   = CAST(SUM(CASE WHEN objtype = N'Trigger'   THEN 1.0 ELSE 0.0 END) / SUM(1.0) * 100.0 as numeric(6,1)) \n"
//			    + "--    ,ObjType_View_Pct      = CAST(SUM(CASE WHEN objtype = N'View'      THEN 1.0 ELSE 0.0 END) / SUM(1.0) * 100.0 as numeric(6,1)) \n"
//			    + "--    ,ObjType_Default_Pct   = CAST(SUM(CASE WHEN objtype = N'Default'   THEN 1.0 ELSE 0.0 END) / SUM(1.0) * 100.0 as numeric(6,1)) \n"
//			    + "--    ,ObjType_UsrTab_Pct    = CAST(SUM(CASE WHEN objtype = N'UsrTab'    THEN 1.0 ELSE 0.0 END) / SUM(1.0) * 100.0 as numeric(6,1)) \n"
//			    + "--    ,ObjType_SysTab_Pct    = CAST(SUM(CASE WHEN objtype = N'SysTab'    THEN 1.0 ELSE 0.0 END) / SUM(1.0) * 100.0 as numeric(6,1)) \n"
//			    + "--    ,ObjType_Check_Pct     = CAST(SUM(CASE WHEN objtype = N'Check'     THEN 1.0 ELSE 0.0 END) / SUM(1.0) * 100.0 as numeric(6,1)) \n"
//			    + "--    ,ObjType_Rule_Pct      = CAST(SUM(CASE WHEN objtype = N'Rule'      THEN 1.0 ELSE 0.0 END) / SUM(1.0) * 100.0 as numeric(6,1)) \n"
			    + " \n"
			    + "    ,CacheObjType_CompiledPlan_Pct     = CAST(SUM(CASE WHEN cacheobjtype = N'Compiled Plan'      THEN 1.0 ELSE 0.0 END) / SUM(1.0) as numeric(6,1)) * 100.0 \n"
			    + "    ,CacheObjType_CompiledPlanStub_Pct = CAST(SUM(CASE WHEN cacheobjtype = N'Compiled Plan Stub' THEN 1.0 ELSE 0.0 END) / SUM(1.0) as numeric(6,1)) * 100.0 \n"
//			    + "--    ,CacheObjType_ParseTree_Pct        = CAST(SUM(CASE WHEN cacheobjtype = N'Parse Tree'         THEN 1.0 ELSE 0.0 END) / SUM(1.0) as numeric(6,1)) * 100.0 \n"
//			    + "--    ,CacheObjType_ExtendedProc_Pct     = CAST(SUM(CASE WHEN cacheobjtype = N'Extended Proc'      THEN 1.0 ELSE 0.0 END) / SUM(1.0) as numeric(6,1)) * 100.0 \n"
//			    + "--    ,CacheObjType_ClrCompiledFunc_Pct  = CAST(SUM(CASE WHEN cacheobjtype = N'CLR Compiled Func'  THEN 1.0 ELSE 0.0 END) / SUM(1.0) as numeric(6,1)) * 100.0 \n"
//			    + "--    ,CacheObjType_ClrCompiledProc_Pct  = CAST(SUM(CASE WHEN cacheobjtype = N'CLR Compiled Proc'  THEN 1.0 ELSE 0.0 END) / SUM(1.0) as numeric(6,1)) * 100.0 \n"
//			    + " \n"
//			    + "    ,ExtraColumns                    = '>>>' \n"
			    + " \n"
			    + "    ,total_elapsed_time_ms           = SUM(qs.total_elapsed_time) / 1000 \n"
			    + "    ,total_worker_time_ms            = SUM(qs.total_worker_time) / 1000 \n"
			    + "    ,total_logical_reads             = SUM(qs.total_logical_reads) \n"
			    + "    ,total_logical_writes            = SUM(qs.total_logical_writes) \n"
			    + "    ,total_physical_reads            = SUM(qs.total_physical_reads) \n"
//			    + "--    ,total_clr_time_ms               = SUM(qs.total_clr_time) / 1000 \n"
			    + "    ,total_rows                      = SUM(qs.total_rows) \n"
//			    + "--    ,total_dop                       = SUM(qs.total_dop)                        -- Applies to: SQL Server 2016 (13.x) and later. \n"
//			    + "--    ,total_grant_kb                  = SUM(qs.total_grant_kb)                   -- Applies to: SQL Server 2016 (13.x) and later. \n"
//			    + "--    ,total_used_grant_kb             = SUM(qs.total_used_grant_kb)              -- Applies to: SQL Server 2016 (13.x) and later. \n"
//			    + "--    ,total_ideal_grant_kb            = SUM(qs.total_ideal_grant_kb)             -- Applies to: SQL Server 2016 (13.x) and later. \n"
//			    + "--    ,total_reserved_threads          = SUM(qs.total_reserved_threads)           -- Applies to: SQL Server 2016 (13.x) and later. \n"
//			    + "--    ,total_used_threads              = SUM(qs.total_used_threads)               -- Applies to: SQL Server 2016 (13.x) and later. \n"
//			    + "--    ,total_columnstore_segment_reads = SUM(qs.total_columnstore_segment_reads)  -- Applies to: Starting with SQL Server 2016 (13.x) SP2 and SQL Server 2017 (14.x) CU3 \n"
//			    + "--    ,total_columnstore_segment_skips = SUM(qs.total_columnstore_segment_skips)  -- Applies to: Starting with SQL Server 2016 (13.x) SP2 and SQL Server 2017 (14.x) CU3 \n"
//			    + "--    ,total_spills                    = SUM(qs.total_spills)                     -- Applies to: Starting with SQL Server 2016 (13.x) SP2 and SQL Server 2017 (14.x) CU3 \n"
//			    + "--    ,total_page_server_reads         = SUM(qs.total_page_server_reads)          -- Applies to: Azure SQL Database Hyperscale \n"
			    + " \n"
			    + "FROM sys.dm_exec_query_stats qs \n"
			    + "LEFT OUTER JOIN sys.dm_exec_cached_plans ecp ON qs.plan_handle = ecp.plan_handle \n"
			    + "--GROUP BY CAST(qs.creation_time AS date), \n"
			    + "--         CASE \n"
			    + "--             WHEN CAST(qs.creation_time AS date) <> CAST(GETDATE() AS date) THEN -1 \n"
			    + "--             ELSE DATEPART(hh, qs.creation_time) \n"
			    + "--         END \n"
			    + "--ORDER BY 1 DESC, 2 DESC \n"
			    + " \n"
			    + "GROUP BY GROUPING SETS \n"
			    + "    ( \n"
			    + "       ( CAST(qs.creation_time AS date) \n"
			    + "        ,CASE \n"
			    + "             WHEN CAST(qs.creation_time AS date) <> CAST(GETDATE() AS date) THEN -1 \n"
			    + "             ELSE DATEPART(hh, qs.creation_time) \n"
			    + "         END \n"
			    + "       ) \n"
			    + "       ,() \n"
			    + "    ) \n"
			    + "ORDER BY 1 DESC, 2 DESC \n"
			    + "";

		return sql;
	}
}
