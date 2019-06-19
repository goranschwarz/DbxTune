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
import java.util.List;

import com.asetune.ICounterController;
import com.asetune.IGuiController;
import com.asetune.cm.CounterSetTemplates;
import com.asetune.cm.CounterSetTemplates.Type;
import com.asetune.cm.CountersModel;
import com.asetune.graph.TrendGraphDataPoint;
import com.asetune.graph.TrendGraphDataPoint.LabelType;
import com.asetune.gui.MainFrame;

/**
 * @author Goran Schwarz (goran_schwarz@hotmail.com)
 */
public class CmWaitingTasks
extends CountersModel
{
//	private static Logger        _logger          = Logger.getLogger(CmWaitingTasks.class);
	private static final long    serialVersionUID = 1L;

	public static final String   CM_NAME          = CmWaitingTasks.class.getSimpleName();
	public static final String   SHORT_NAME       = "Waiting Tasks";
	public static final String   HTML_DESC        = 
		"<html>" +
		"<p>FIXME</p>" +
		"</html>";

	public static final String   GROUP_NAME       = MainFrame.TCP_GROUP_SERVER;
	public static final String   GUI_ICON_FILE    = "images/"+CM_NAME+".png";

	public static final long     NEED_SRV_VERSION = 0;
	public static final long     NEED_CE_VERSION  = 0;

	public static final String[] MON_TABLES       = new String[] {"dm_os_waiting_tasks"};
	public static final String[] NEED_ROLES       = new String[] {};//{"VIEW SERVER STATE"};
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
	
//	@Override
//	protected TabularCntrPanel createGui()
//	{
//		return new CmWaitingTasksPanel(this);
//	}

	@Override
	public String[] getDependsOnConfigForVersion(Connection conn, long srvVersion, boolean isAzure)
	{
		return NEED_CONFIG;
	}

	@Override
	public List<String> getPkForVersion(Connection conn, long srvVersion, boolean isAzure)
	{
//		List <String> pkCols = new LinkedList<String>();
//
//		pkCols.add("session_id");
//
//		return pkCols;

		// no need to have PK, since we are NOT using "diff" counters
		return null;
	}

	@Override
	public String getSqlForVersion(Connection conn, long srvVersion, boolean isAzure)
	{
//		String sql = 
//			"select * \n" +
//			"from sys.dm_os_waiting_tasks \n" +
//			"where session_id is not null \n" +
//			"";

		String dm_os_waiting_tasks = "dm_os_waiting_tasks";
		String dm_os_tasks         = "dm_os_tasks";
		String dm_exec_sessions    = "dm_exec_sessions";
		String dm_exec_requests    = "dm_exec_requests";
		String dm_exec_sql_text    = "dm_exec_sql_text";
		String dm_exec_query_plan  = "dm_exec_query_plan";
		
		if (isAzure)
		{
			dm_os_waiting_tasks = "dm_pdw_nodes_os_waiting_tasks";
			dm_os_tasks         = "dm_pdw_nodes_os_tasks";
			dm_exec_sessions    = "dm_pdw_nodes_exec_sessions";
			dm_exec_requests    = "dm_exec_requests";            // SAME NAME IN AZURE ????
			dm_exec_sql_text    = "dm_exec_sql_text";            // SAME NAME IN AZURE ????
			dm_exec_query_plan  = "dm_exec_query_plan";          // SAME NAME IN AZURE ????
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
			    + "============================================================================*/ \n"
			    + "SELECT \n"
			    + "    [owt].[session_id], \n"
			    + "    [owt].[exec_context_id], \n"
			    + "    [ot].[scheduler_id], \n"
			    + "    [owt].[wait_duration_ms], \n"
			    + "    [owt].[wait_type], \n"
			    + "    [owt].[blocking_session_id], \n"
			    + "    [owt].[resource_description], \n"
			    + "    CASE [owt].[wait_type] \n"
			    + "        WHEN N'CXPACKET' THEN \n"
			    + "            RIGHT ([owt].[resource_description], \n"
			    + "                CHARINDEX (N'=', REVERSE ([owt].[resource_description])) - 1) \n"
			    + "        ELSE NULL \n"
			    + "    END AS [Node ID], \n"
			    + "    [es].[program_name], \n"
			    + "    [est].text, \n"
			    + "    [er].[database_id], \n"
			    + "    [eqp].[query_plan], \n"
			    + "    [er].[cpu_time] \n"
			    + "FROM sys." + dm_os_waiting_tasks + " [owt] \n"
			    + "INNER JOIN sys." + dm_os_tasks + " [ot] ON \n"
			    + "    [owt].[waiting_task_address] = [ot].[task_address] \n"
			    + "INNER JOIN sys." + dm_exec_sessions + " [es] ON \n"
			    + "    [owt].[session_id] = [es].[session_id] \n"
			    + "INNER JOIN sys." + dm_exec_requests + " [er] ON \n"
			    + "    [es].[session_id] = [er].[session_id] \n"
			    + "OUTER APPLY sys." + dm_exec_sql_text + " ([er].[sql_handle]) [est] \n"
			    + "OUTER APPLY sys." + dm_exec_query_plan + " ([er].[plan_handle]) [eqp] \n"
			    + "WHERE \n"
			    + "    [es].[is_user_process] = 1 \n"
			    + "ORDER BY \n"
			    + "    [owt].[session_id], \n"
			    + "    [owt].[exec_context_id] \n"
			    + "";

		return sql;
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
			TrendGraphDataPoint.Y_AXIS_SCALE_LABELS_NORMAL,
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
			TrendGraphDataPoint.Y_AXIS_SCALE_LABELS_MILLISEC,
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
