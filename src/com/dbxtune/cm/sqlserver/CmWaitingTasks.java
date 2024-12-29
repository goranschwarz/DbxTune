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
package com.dbxtune.cm.sqlserver;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeoutException;

import org.apache.commons.lang3.StringUtils;
import org.apache.log4j.Logger;

import com.dbxtune.ICounterController;
import com.dbxtune.IGuiController;
import com.dbxtune.ICounterController.DbmsOption;
import com.dbxtune.cache.DbmsObjectIdCache;
import com.dbxtune.cache.DbmsObjectIdCache.ObjectInfo;
import com.dbxtune.central.pcs.CentralPersistReader;
import com.dbxtune.cm.CounterSample;
import com.dbxtune.cm.CounterSetTemplates;
import com.dbxtune.cm.CountersModel;
import com.dbxtune.cm.CounterSetTemplates.Type;
import com.dbxtune.cm.sqlserver.gui.CmWaitingTasksPanel;
import com.dbxtune.graph.TrendGraphDataPoint;
import com.dbxtune.graph.TrendGraphDataPoint.LabelType;
import com.dbxtune.gui.MainFrame;
import com.dbxtune.gui.TabularCntrPanel;
import com.dbxtune.pcs.PcsColumnOptions;
import com.dbxtune.pcs.PcsColumnOptions.ColumnType;
import com.dbxtune.sql.conn.DbxConnection;
import com.dbxtune.sql.conn.info.DbmsVersionInfo;
import com.dbxtune.sql.conn.info.DbmsVersionInfoSqlServer;
import com.dbxtune.utils.StringUtil;

/**
 * @author Goran Schwarz (goran_schwarz@hotmail.com)
 */
public class CmWaitingTasks
extends CountersModel
{
	private static Logger        _logger          = Logger.getLogger(CmWaitingTasks.class);
	private static final long    serialVersionUID = 1L;

	public static final String   CM_NAME          = CmWaitingTasks.class.getSimpleName();
	public static final String   SHORT_NAME       = "Waiting Tasks";
	public static final String   HTML_DESC        = 
		"<html>" +
		"<p>Tasks that are <b>waiting</b> for something toe become available.</p>" +
		"<br><br>" +
		"Table Background colors:" +
		"<ul>" +
		"    <li>PINK   - SPID is Blocked by some other SPID. This is the Victim.</li>" +
//		"    <li>RED    - SPID is Blocking other SPID's from running, this SPID is Responsible or the Root Cause of a Blocking Lock.</li>" +
		"</ul>" +
		"</html>";

	public static final String   GROUP_NAME       = MainFrame.TCP_GROUP_SERVER;
	public static final String   GUI_ICON_FILE    = "images/"+CM_NAME+".png";

	public static final long     NEED_SRV_VERSION = 0;
	public static final long     NEED_CE_VERSION  = 0;

	public static final String[] MON_TABLES       = new String[] {"dm_os_waiting_tasks"};
	public static final String[] NEED_ROLES       = new String[] {"VIEW SERVER STATE"};
	public static final String[] NEED_CONFIG      = new String[] {};

	public static final String[] PCT_COLUMNS      = new String[] {};
	public static final String[] DIFF_COLUMNS     = new String[] {};
//	public static final String[] DIFF_COLUMNS     = new String[] {
//		"_last_column_name_only_used_as_a_place_holder_here"
//		};

// Microsoft SQL Server 2008 R2 (SP2) - 10.50.4000.0 (X64)  	Jun 28 2012 08:36:30  	Copyright (c) Microsoft Corporation 	Express Edition with Advanced Services (64-bit) on Windows NT 6.1 <X64> (Build 7601: Service Pack 1)

//	RS> Col# Label                    JDBC Type Name           Guessed DBMS type Source Table
//	RS> ---- ------------------------ ------------------------ ----------------- ------------
//	RS> 1    waiting_task_address     java.sql.Types.VARBINARY varbinary(16)     -none-      
//	RS> 2    session_id               java.sql.Types.SMALLINT  smallint          -none-      
//	RS> 3    exec_context_id          java.sql.Types.INTEGER   int               -none-      
//	RS> 4    wait_duration_ms         java.sql.Types.BIGINT    bigint            -none-      
//	RS> 5    wait_type                java.sql.Types.NVARCHAR  nvarchar(60)      -none-      
//	RS> 6    resource_address         java.sql.Types.VARBINARY varbinary(16)     -none-      
//	RS> 7    blocking_task_address    java.sql.Types.VARBINARY varbinary(16)     -none-      
//	RS> 8    blocking_session_id      java.sql.Types.SMALLINT  smallint          -none-      
//	RS> 9    blocking_exec_context_id java.sql.Types.INTEGER   int               -none-      
//	RS> 10   resource_description     java.sql.Types.NVARCHAR  nvarchar(2048)    -none-      

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

		return new CmWaitingTasks(counterController, guiController);
	}

	public CmWaitingTasks(ICounterController counterController, IGuiController guiController)
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
	
	@Override
	protected TabularCntrPanel createGui()
	{
		return new CmWaitingTasksPanel(this);
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
			map.put("currentSqlText"  , new PcsColumnOptions(ColumnType.DICTIONARY_COMPRESSION));
			map.put("fullSqlBatchText", new PcsColumnOptions(ColumnType.DICTIONARY_COMPRESSION));
			map.put("query_plan"      , new PcsColumnOptions(ColumnType.DICTIONARY_COMPRESSION));

			// Set the map in the super
			setPcsColumnOptions(map);
		}

		return map;
	}

	@Override
	public List<String> getPkForVersion(DbxConnection conn, DbmsVersionInfo versionInfo)
	{
//		List <String> pkCols = new LinkedList<String>();
//
//		pkCols.add("session_id");
//		pkCols.add("exec_context_id");
//
//		return pkCols;

		// no need to have PK, since we are NOT using "diff" counters
		return null;
	}

	@Override
	public String getSqlForVersion(DbxConnection conn, DbmsVersionInfo versionInfo)
	{
		DbmsVersionInfoSqlServer ssVersionInfo = (DbmsVersionInfoSqlServer) versionInfo;

//		String sql = 
//			"select * \n" +
//			"from sys.dm_os_waiting_tasks \n" +
//			"where session_id is not null \n" +
//			"";

//		String nolock = "";
//		if (Configuration.getCombinedConfiguration().getBooleanProperty("sqlserver.use.nolock", false))
//		{
//			nolock = " WITH (NOLOCK) ";
//		//	nolock = " WITH (READUNCOMMITTED) ";
//		}
		// the above is done in 'CounterControllerSqlServer.setInRefresh(true): then it does: SET TRANSACTION ISOLATION LEVEL READ UNCOMMITTED  -- at the connection level'

		String dm_os_waiting_tasks = "dm_os_waiting_tasks";
		String dm_os_tasks         = "dm_os_tasks";
		String dm_exec_sessions    = "dm_exec_sessions";
		String dm_exec_requests    = "dm_exec_requests";
		String dm_exec_sql_text    = "dm_exec_sql_text";
		String dm_exec_query_plan  = "dm_exec_query_plan";
		
		// get Actual-Query-Plan instead of Estimated-QueryPlan
		if (isDbmsOptionEnabled(DbmsOption.SQL_SERVER__LAST_QUERY_PLAN_STATS))
		{
			dm_exec_query_plan = "dm_exec_query_plan_stats";
		}

		if (ssVersionInfo.isAzureSynapseAnalytics())
		{
			dm_os_waiting_tasks = "dm_pdw_nodes_os_waiting_tasks";
			dm_os_tasks         = "dm_pdw_nodes_os_tasks";
			dm_exec_sessions    = "dm_pdw_nodes_exec_sessions";
			dm_exec_requests    = "dm_exec_requests";   // SAME NAME IN AZURE ????
			dm_exec_sql_text    = "dm_exec_sql_text";   // SAME NAME IN AZURE ????
			dm_exec_query_plan  = "dm_exec_query_plan"; // SAME NAME IN AZURE ????
		}

		// Is 'context_info_str' enabled (if it causes any problem, it can be disabled)
		String contextInfoStr = "/*    ,replace(cast([er].context_info as varchar(128)),char(0),'') AS [context_info_str] -- " + SqlServerCmUtils.HELPTEXT_howToEnable__context_info_str + " */ \n";
		if (SqlServerCmUtils.isContextInfoStrEnabled())
		{
			// Make the binary 'context_info' into a String
			contextInfoStr = "    ,replace(cast([er].context_info as varchar(128)),char(0),'') AS [context_info_str] /* " + SqlServerCmUtils.HELPTEXT_howToDisable__context_info_str + " */ \n";
		}
		
		String sql = ""
				+ "/*============================================================================ \n"
				+ "  File:     WaitingTasks.sql \n"
				+ " \n"
				+ "  Summary:  Snapshot of waiting tasks \n"
				+ " \n"
				+ "  SQL Server Versions: 2005 onwards \n"
				+ "------------------------------------------------------------------------------ \n"
				+ "  Written by Paul S. Randal, SQLskills.com \n"
				+ " \n"
				+ "  (c) 2015, SQLskills.com. All rights reserved. \n"
				+ " \n"
				+ "  For more scripts and sample code, check out \n"
				+ "    http://www.SQLskills.com \n"
				+ " \n"
				+ "  You may alter this code for your own *non-commercial* purposes. You may \n"
				+ "  republish altered code as long as you include this copyright and give due \n"
				+ "  credit, but you must obtain prior permission before blogging this code. \n"
				+ " \n"
				+ "  THIS CODE AND INFORMATION ARE PROVIDED \"AS IS\" WITHOUT WARRANTY OF \n"
				+ "  ANY KIND, EITHER EXPRESSED OR IMPLIED, INCLUDING BUT NOT LIMITED \n"
				+ "  TO THE IMPLIED WARRANTIES OF MERCHANTABILITY AND/OR FITNESS FOR A \n"
				+ "  PARTICULAR PURPOSE. \n"
				+ " \n"
				+ "  The content has been heavily modified by Goran Schwarz for SqlServerTune \n"
				+ "============================================================================*/ \n"
				+ "SELECT /* ${cmCollectorName} */ \n"
				+ "     [owt].[session_id] \n"
				+ "    ,[owt].[exec_context_id] \n"
				+ "    ,[ot].[scheduler_id] \n"
				+ "    ,[owt].[wait_duration_ms] \n"
				+ "    ,[owt].[wait_type] \n"
				+ "    ,[owt].[blocking_session_id] \n"
				+ "    ,[owt].[blocking_exec_context_id] \n"
				+ "    ,[owt].[blocking_task_address] \n"
				+ "    ,[er].[cpu_time] \n"
				+ "    ,[er].[status] \n"
				+ "    ,[er].[command] \n"
//				+ "    ,object_name([est].objectid, [est].dbid) AS [ProcName] \n"
				+ "    ,CASE WHEN [owt].[exec_context_id] != 0 THEN '' ELSE (select object_name([objectid], [dbid]) from sys." + dm_exec_sql_text   + " ([er].[sql_handle])) END AS [ProcName] \n"
				+ "    ,[er].statement_start_offset             AS [StmntStart] \n"
				+ "    ,[es].[program_name] \n"
			    +       contextInfoStr
				+ "    ,[er].[database_id] \n"
				+ "    ,db_name([er].[database_id]) AS [database_name] \n"
				+ "    ,[owt].[resource_description] \n"
				+ "    ,cast('' as varchar(512)) AS [resource_description_decoded] \n"
				
				+ "    ,CASE [owt].[wait_type] \n"
				+ "        WHEN N'CXPACKET' THEN \n"
				+ "            RIGHT ([owt].[resource_description], \n"
				+ "                CHARINDEX (N'=', REVERSE ([owt].[resource_description])) - 1) \n"
				+ "        ELSE NULL \n"
				+ "     END AS [Node ID] \n"

//				+ "    ,CASE WHEN [owt].[exec_context_id] != 0 THEN '' \n"
//				+ "     ELSE \n"
//				+ "        SUBSTRING([est].text, [er].statement_start_offset / 2,  \n"
//				+ "            ( CASE WHEN [er].statement_end_offset = -1  \n"
//				+ "                   THEN DATALENGTH([est].text)  \n"
//				+ "                   ELSE [er].statement_end_offset  \n"
//				+ "              END - [er].statement_start_offset ) / 2 +2) \n"
//				+ "     END AS [currentSqlText] \n"
				
//				+ "    ,CASE WHEN [owt].[exec_context_id] != 0 THEN '' ELSE [est].text         END AS [fullSqlBatchText] \n"
//				+ "    ,CASE WHEN [owt].[exec_context_id] != 0 THEN '' ELSE [eqp].[query_plan] END AS [query_plan] \n"
				+ "    ,CASE WHEN [owt].[exec_context_id] != 0 THEN '' \n"
				+ "     ELSE \n"
				+ "        (SELECT \n"
				+ "           SUBSTRING([est].text, [er].statement_start_offset / 2,  \n"
				+ "               ( CASE WHEN [er].statement_end_offset = -1  \n"
				+ "                      THEN DATALENGTH([est].text)  \n"
				+ "                      ELSE [er].statement_end_offset  \n"
				+ "                 END - [er].statement_start_offset ) / 2 +2) \n"
				+ "         FROM sys.dm_exec_sql_text ([er].[sql_handle]) [est] \n"
				+ "        ) \n"
				+ "     END AS [currentSqlText] \n"
				+ "    ,CASE WHEN [owt].[exec_context_id] != 0 THEN '' ELSE (select [text]       from sys." + dm_exec_sql_text   + " ([er].[sql_handle]))  END AS [fullSqlBatchText] \n"
				+ "    ,CASE WHEN [owt].[exec_context_id] != 0 THEN '' ELSE (select [query_plan] from sys." + dm_exec_query_plan + " ([er].[plan_handle])) END AS [query_plan] \n"
				+ "FROM sys." + dm_os_waiting_tasks + " [owt] \n"
				+ "INNER JOIN  sys." + dm_os_tasks + "      [ot] ON [owt].[waiting_task_address] = [ot].[task_address] \n"
				+ "INNER JOIN  sys." + dm_exec_sessions + " [es] ON [owt].[session_id] = [es].[session_id] \n"
				+ "INNER JOIN  sys." + dm_exec_requests + " [er] ON [es].[session_id] = [er].[session_id] \n"
//				+ "OUTER APPLY sys." + dm_exec_sql_text + " ([er].[sql_handle]) [est] \n"
//				+ "OUTER APPLY sys." + dm_exec_query_plan + " ([er].[plan_handle]) [eqp] \n"
				+ "WHERE \n"
				+ "    [es].[is_user_process] = 1 \n"
				+ "ORDER BY \n"
				+ "    [owt].[session_id], \n"
				+ "    [owt].[exec_context_id] \n"
				+ "";

//Add - select * from sys.dm_exec_query_profiles --- physical_operator_name, estimated_row_count, row_count, cpu_time_ms, logical/physical_raed_count, etc.

//		NOTE:
//			- Do we need: dm_os_tasks.scheduler_id /not a big table to join on, but anything we can strip might be good)
//	FIXED	- Do we need to: OUTER APPLY sys.dm_exec_query_plan... Or can we do that *AFTER* we have sampled data (or inject data into a temp table, and get dm_exec_query_plan for unique plan_handle's)
//	FIXED	- (same with dm_exec_sql_text, but I think its the PLAN (on all the paralell worker threads) that kills performance when SQL-Server is under heavy load)
//			- Do we only want to look at USER_PROCESSES or do we want to look at ALL waiting_tasks

		return sql;
	}
//	String dm_exec_requests    = "dm_exec_requests"; == der
//	String dm_exec_sql_text    = "dm_exec_sql_text"; == dest
	
//	"    SUBSTRING(dest.text, der.statement_start_offset / 2,  \n" +
//	"        ( CASE WHEN der.statement_end_offset = -1  \n" +
//	"               THEN DATALENGTH(dest.text)  \n" +
//	"               ELSE der.statement_end_offset  \n" +
//	"          END - der.statement_start_offset ) / 2) AS [lastKnownSql], \n" +

	
	@Override
	public void localCalculation(CounterSample newSample)
	{
		// make: column 'program_name' with value "SQLAgent - TSQL JobStep (Job 0x38AAD6888E5C5E408DE573B0A25EE970 : Step 1)"
		// into:                                  "SQLAgent - TSQL JobStep (Job '<name-of-the-job>' : Step 1 '<name-of-the-step>')
		SqlServerCmUtils.localCalculation_resolveSqlAgentProgramName(newSample);

		
		int resource_description_pos         = newSample.findColumn("resource_description");
		int resource_description_decoded_pos = newSample.findColumn("resource_description_decoded");
		
//		int exec_context_id_pos = newSample.findColumn("exec_context_id");
//		int query_plan_pos      = newSample.findColumn("query_plan");
//		int plan_handle_pos     = newSample.findColumn("plan_handle");

		if (resource_description_pos == -1 || resource_description_decoded_pos == -1)
		{
			_logger.error("Cant find desired columns: resource_description_pos=" + resource_description_pos +", resource_description_decoded_pos=" + resource_description_decoded_pos);
			return;
		}

//		if (exec_context_id_pos == -1 || query_plan_pos == -1 || plan_handle_pos == -1)
//		{
//			_logger.error("Cant find desired columns: exec_context_id_pos=" + exec_context_id_pos + ", query_plan_pos=" + query_plan_pos + ", plan_handle_pos=" + plan_handle_pos);
//			return;
//		}
		
		for (int rowId=0; rowId<newSample.getRowCount(); rowId++)
		{
			//-----------------------------------------------------------------------------------------------------
			// Go and get 'XML Execution Plan' when 'exec_context_id' is 0 -- In a Parallel plan, thats the "owner"
			// option is to 'OUTER APPLY' it in the Collector SQL, but that seems to take to long time, due to the fact that it does it for ALL rows
			// NOTE: The below is not yet tested, I reverted back to modify the SQL Statement using 'CASE WHEN [exec_context_id] != 0 THEN '' ELSE (getPlan) END'
			//-----------------------------------------------------------------------------------------------------
//			Double exec_context_id = newSample.getValueAsDouble(rowId, exec_context_id_pos);
//			if (exec_context_id != null && exec_context_id.intValue() == 0)
//			{
//				String plan_handle = newSample.getValueAsString(rowId, plan_handle_pos);
//				if (StringUtil.hasValue(plan_handle))
//				{
//					String query_plan = SqlServerUtils.getXmlQueryPlanNoThrow(getCounterController().getMonConnection(), plan_handle);
//					newSample.setValueAt(query_plan, rowId, query_plan_pos);
//				}
//			}

			//-----------------------------------------------------------------------------------------------------
			// Try to "decode" the 'resource_description'
			//-----------------------------------------------------------------------------------------------------
			String resource_description = newSample.getValueAsString(rowId, resource_description_pos);
			String resource_description_decoded = "";

			if (StringUtil.hasValue(resource_description))
			{
				// ridlock fileid=1 pageid=392 dbid=2 id=lock14306dd800 mode=X associatedObjectId=2738293458776227840
				// keylock hobtid=72057595465433088 dbid=5 id=lock2403057280 mode=X associatedObjectId=72057595465433088

				//String firstWord = StringUtils.substringBefore(resource_description, " ");
				
				// Parse content based on the first word
//				if ("ridlock".equals(firstWord))
//				{
//				}
//				else if ("keylock".equals(firstWord))
//				{
//				}
//				else if ("zzzzzz".equals(firstWord))
//				{
//				}

				// Or do it a bit more generic based on what "keys=" we fin in the "resource_description" 
				boolean has_dbid               = resource_description.contains("dbid=");
				boolean has_objid              = resource_description.contains("objid=");
				boolean has_associatedObjectId = resource_description.contains("associatedObjectId=");
				boolean has_hobtid             = resource_description.contains("hobt=");

				if (has_dbid && (has_objid || has_associatedObjectId || has_hobtid))
				{
					String firstWord = StringUtils.substringBefore(resource_description, " ");

					// SPLIT into the various parts... which is parsed to a Map
					String keyValStr = firstWord == null ? "" : resource_description.substring(firstWord.length()+1);
					Map<String, String> keyVal       = StringUtil.parseCommaStrToMap(keyValStr, "=", " ");
					Map<String, String> decodeKeyVal = new LinkedHashMap<>();

					int  dbid        = !keyVal.containsKey("dbid")               ? -1 : StringUtil.parseInt (keyVal.get("dbid")              , -1);
					int  objid       = !keyVal.containsKey("objid")              ? -1 : StringUtil.parseInt (keyVal.get("objid")             , -1);
					long partitionId = !keyVal.containsKey("associatedObjectId") ? -1 : StringUtil.parseLong(keyVal.get("associatedObjectId"), -1);
					long hobtId      = !keyVal.containsKey("hobt")               ? -1 : StringUtil.parseLong(keyVal.get("hobt")              , -1);

					if (dbid != -1 && (objid != -1 || partitionId != -1 || hobtId != -1))
					{
						if (DbmsObjectIdCache.hasInstance())
						{
							String dbname = DbmsObjectIdCache.getInstance().getDBName(dbid);
							decodeKeyVal.put("dbname", dbname);

							try
							{
								ObjectInfo oi = null;
								if (oi == null && objid       != -1) oi = DbmsObjectIdCache.getInstance().getByObjectId   (dbid, objid);
								if (oi == null && partitionId != -1) oi = DbmsObjectIdCache.getInstance().getByPartitionId(dbid, partitionId);
								if (oi == null && hobtId      != -1) oi = DbmsObjectIdCache.getInstance().getByHobtId     (dbid, hobtId);
								
								if (oi != null)
								{
									decodeKeyVal.put("schemaName", oi.getSchemaName());
									decodeKeyVal.put("objectName", oi.getObjectName());
								}
							}
							catch (TimeoutException ignore) {}
						}
					}

					if (keyVal.containsKey("pageid"))
					{
						int pageid = StringUtil.parseInt (keyVal.get("pageid"), -1);
						if (pageid != -1)
						{
							// The below algorithm is reused from 'sp_whoIsActive'
							if      (pageid == 1 ||       pageid % 8088   == 0) decodeKeyVal.put("pageType", "PFS");
							else if (pageid == 2 ||       pageid % 511232 == 0) decodeKeyVal.put("pageType", "GAM");
							else if (pageid == 3 || (pageid - 1) % 511232 == 0) decodeKeyVal.put("pageType", "SGAM");
							else if (pageid == 6 || (pageid - 6) % 511232 == 0) decodeKeyVal.put("pageType", "DCM");
							else if (pageid == 7 || (pageid - 7) % 511232 == 0) decodeKeyVal.put("pageType", "BCM");
						}
						
					}

//					TODO: possibly add PFS, GAM, SGAM contention detection for TEMPDB
					//    one source could be: sp_WhoisActive 
					//    ... wait_type Like 'PAGE%LATCH_%' and resource_description Like '2:%'
					//    But we are not sampling enough to find this... 
					//    or add a Extended Event session that does it!
					
					if ( ! decodeKeyVal.isEmpty() )
						resource_description_decoded = decodeKeyVal.toString();
				}
					
				// Set the decoded values
				if (StringUtil.hasValue(resource_description_decoded))
				{
					newSample.setValueAt(resource_description_decoded, rowId, resource_description_decoded_pos);
				}
			}
		}
	}

	//---------------------------------------------------------------------------------
	//---------------------------------------------------------------------------------
	// Graph stuff
	//---------------------------------------------------------------------------------
	//---------------------------------------------------------------------------------
	
	public static final String   GRAPH_NAME_WAIT_COUNT      = "WaitCount";
	public static final String   GRAPH_NAME_WAIT_MAX_TIME   = "WaitMaxTime";
	

	private void addTrendGraphs()
	{
		addTrendGraph(GRAPH_NAME_WAIT_COUNT,
			"Number of Current Wait Tasks", 	                   // Menu CheckBox text
			"Number of Current Wait Tasks ("+GROUP_NAME+"->"+SHORT_NAME+")", // Label 
			TrendGraphDataPoint.createGraphProps(TrendGraphDataPoint.Y_AXIS_SCALE_LABELS_NORMAL, CentralPersistReader.SampleType.MAX_OVER_SAMPLES),
			new String[] {"WaitCount"},
			LabelType.Static,
			TrendGraphDataPoint.Category.WAITS,
			false, // is Percent Graph
			false, // visible at start
			0,    // graph is valid from Server Version. 0 = All Versions; >0 = Valid from this version and above
			-1);  // minimum height

		addTrendGraph(GRAPH_NAME_WAIT_MAX_TIME,
			"Max Wait Time in ms for Current Tasks", 	                   // Menu CheckBox text
			"Max Wait Time in ms for Current Tasks ("+GROUP_NAME+"->"+SHORT_NAME+")", // Label 
			TrendGraphDataPoint.createGraphProps(TrendGraphDataPoint.Y_AXIS_SCALE_LABELS_MILLISEC, CentralPersistReader.SampleType.MAX_OVER_SAMPLES),
			new String[] {"wait_duration_ms"},
			LabelType.Static,
			TrendGraphDataPoint.Category.WAITS,
			false, // is Percent Graph
			false, // visible at start
			0,    // graph is valid from Server Version. 0 = All Versions; >0 = Valid from this version and above
			-1);  // minimum height
	}
	
	@Override
	public void updateGraphData(TrendGraphDataPoint tgdp)
	{
//		long   srvVersion = getServerVersion();
		String graphName  = tgdp.getName();
//System.out.println("graphName='"+graphName+"'");
		
		if (GRAPH_NAME_WAIT_COUNT.equals(graphName))
		{
			Double[] arr = new Double[1];

//System.out.println("graphName='"+graphName+"'. rowCount="+this.getCounterDataAbs().getRowCount());
			arr[0] = new Double( this.getCounterDataAbs().getRowCount() );
			
			// Set the values
			tgdp.setDataPoint(this.getTimestamp(), arr);
		}

		if (GRAPH_NAME_WAIT_MAX_TIME.equals(graphName))
		{
			Double[] arr = new Double[1];

//System.out.println("graphName='"+graphName+"'. wait_duration_ms="+this.getAbsValueMax("wait_duration_ms"));
			arr[0] = this.getAbsValueMax("wait_duration_ms");

			// Set the values
			tgdp.setDataPoint(this.getTimestamp(), arr);
		}
	}

	/** Normal updateGraphData() skips update if no rows in table... but we still want to update */
	@Override
	public void updateGraphData()
	{
		for (TrendGraphDataPoint tgdp : getTrendGraphData().values()) 
			updateGraphData(tgdp);
	}
}
