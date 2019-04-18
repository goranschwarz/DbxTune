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
public class CmExecRequests
extends CountersModel
{
//	private static Logger        _logger          = Logger.getLogger(CmServiceMemory.class);
	private static final long    serialVersionUID = 1L;

	public static final String   CM_NAME          = CmExecRequests.class.getSimpleName();
	public static final String   SHORT_NAME       = "Exec Requests";
	public static final String   HTML_DESC        = 
		"<html>" +
		"<p>FIXME</p>" +
		"</html>";

	public static final String   GROUP_NAME       = MainFrame.TCP_GROUP_SERVER;
	public static final String   GUI_ICON_FILE    = "images/"+CM_NAME+".png";

	public static final long     NEED_SRV_VERSION = 0;
	public static final long     NEED_CE_VERSION  = 0;

	public static final String[] MON_TABLES       = new String[] {"dm_exec_requests"};
	public static final String[] NEED_ROLES       = new String[] {};//{"VIEW SERVER STATE"};
	public static final String[] NEED_CONFIG      = new String[] {};

	public static final String[] PCT_COLUMNS      = new String[] {};
	public static final String[] DIFF_COLUMNS     = new String[] {
//		"session_id",                      // java.sql.Types.SMALLINT  smallint            
//		"request_id",                      // java.sql.Types.INTEGER   int                 
//		"start_time",                      // java.sql.Types.TIMESTAMP datetime            
//		"status",                          // java.sql.Types.NVARCHAR  nvarchar(30)        
//		"command",                         // java.sql.Types.NVARCHAR  nvarchar(16)        
//		"sql_handle",                      // java.sql.Types.VARBINARY varbinary(128)      
//		"statement_start_offset",          // java.sql.Types.INTEGER   int                 
//		"statement_end_offset",            // java.sql.Types.INTEGER   int                 
//		"plan_handle",                     // java.sql.Types.VARBINARY varbinary(128)      
//		"database_id",                     // java.sql.Types.SMALLINT  smallint            
//		"user_id",                         // java.sql.Types.INTEGER   int                 
//		"connection_id",                   // java.sql.Types.CHAR      uniqueidentifier(36)
//		"blocking_session_id",             // java.sql.Types.SMALLINT  smallint            
//		"wait_type",                       // java.sql.Types.NVARCHAR  nvarchar(60)        
//		"wait_time",                       // java.sql.Types.INTEGER   int                 
//		"last_wait_type",                  // java.sql.Types.NVARCHAR  nvarchar(60)        
//		"wait_resource",                   // java.sql.Types.NVARCHAR  nvarchar(256)       
//		"open_transaction_count",          // java.sql.Types.INTEGER   int                 
//		"open_resultset_count",            // java.sql.Types.INTEGER   int                 
//		"transaction_id",                  // java.sql.Types.BIGINT    bigint              
//		"context_info",                    // java.sql.Types.VARBINARY varbinary(256)      
//		"percent_complete",                // java.sql.Types.REAL      real                
//		"estimated_completion_time",       // java.sql.Types.BIGINT    bigint              
		"cpu_time",                        // java.sql.Types.INTEGER   int                 
//		"total_elapsed_time",              // java.sql.Types.INTEGER   int                 
//		"scheduler_id",                    // java.sql.Types.INTEGER   int                 
//		"task_address",                    // java.sql.Types.VARBINARY varbinary(16)       
		"reads",                           // java.sql.Types.BIGINT    bigint              
		"writes",                          // java.sql.Types.BIGINT    bigint              
		"logical_reads",                   // java.sql.Types.BIGINT    bigint              
//		"text_size",                       // java.sql.Types.INTEGER   int                 
//		"language",                        // java.sql.Types.NVARCHAR  nvarchar(128)       
//		"date_format",                     // java.sql.Types.NVARCHAR  nvarchar(3)         
//		"date_first",                      // java.sql.Types.SMALLINT  smallint            
//		"quoted_identifier",               // java.sql.Types.BIT       bit                 
//		"arithabort",                      // java.sql.Types.BIT       bit                 
//		"ansi_null_dflt_on",               // java.sql.Types.BIT       bit                 
//		"ansi_defaults",                   // java.sql.Types.BIT       bit                 
//		"ansi_warnings",                   // java.sql.Types.BIT       bit                 
//		"ansi_padding",                    // java.sql.Types.BIT       bit                 
//		"ansi_nulls",                      // java.sql.Types.BIT       bit                 
//		"concat_null_yields_null",         // java.sql.Types.BIT       bit                 
//		"transaction_isolation_level",     // java.sql.Types.SMALLINT  smallint            
//		"lock_timeout",                    // java.sql.Types.INTEGER   int                 
//		"deadlock_priority",               // java.sql.Types.INTEGER   int                 
//		"row_count",                       // java.sql.Types.BIGINT    bigint              
//		"prev_error",                      // java.sql.Types.INTEGER   int                 
//		"nest_level",                      // java.sql.Types.INTEGER   int                 
//		"granted_query_memory",            // java.sql.Types.INTEGER   int                 
//		"executing_managed_code",          // java.sql.Types.BIT       bit                 
//		"group_id",                        // java.sql.Types.INTEGER   int                 
//		"query_hash",                      // java.sql.Types.BINARY    binary(16)          
//		"query_plan_hash",                 // java.sql.Types.BINARY    binary(16)          
		"_last_column_name_only_used_as_a_place_holder_here"
		};

// Microsoft SQL Server 2008 R2 (SP2) - 10.50.4000.0 (X64)  	Jun 28 2012 08:36:30  	Copyright (c) Microsoft Corporation 	Express Edition with Advanced Services (64-bit) on Windows NT 6.1 <X64> (Build 7601: Service Pack 1)

//	RS> Col# Label                       JDBC Type Name           Guessed DBMS type   
//	RS> ---- --------------------------- ------------------------ --------------------
//	RS> 1    session_id                  java.sql.Types.SMALLINT  smallint            
//	RS> 2    request_id                  java.sql.Types.INTEGER   int                 
//	RS> 3    start_time                  java.sql.Types.TIMESTAMP datetime            
//	RS> 4    status                      java.sql.Types.NVARCHAR  nvarchar(30)        
//	RS> 5    command                     java.sql.Types.NVARCHAR  nvarchar(16)        
//	RS> 6    sql_handle                  java.sql.Types.VARBINARY varbinary(128)      
//	RS> 7    statement_start_offset      java.sql.Types.INTEGER   int                 
//	RS> 8    statement_end_offset        java.sql.Types.INTEGER   int                 
//	RS> 9    plan_handle                 java.sql.Types.VARBINARY varbinary(128)      
//	RS> 10   database_id                 java.sql.Types.SMALLINT  smallint            
//	RS> 11   user_id                     java.sql.Types.INTEGER   int                 
//	RS> 12   connection_id               java.sql.Types.CHAR      uniqueidentifier(36)
//	RS> 13   blocking_session_id         java.sql.Types.SMALLINT  smallint            
//	RS> 14   wait_type                   java.sql.Types.NVARCHAR  nvarchar(60)        
//	RS> 15   wait_time                   java.sql.Types.INTEGER   int                 
//	RS> 16   last_wait_type              java.sql.Types.NVARCHAR  nvarchar(60)        
//	RS> 17   wait_resource               java.sql.Types.NVARCHAR  nvarchar(256)       
//	RS> 18   open_transaction_count      java.sql.Types.INTEGER   int                 
//	RS> 19   open_resultset_count        java.sql.Types.INTEGER   int                 
//	RS> 20   transaction_id              java.sql.Types.BIGINT    bigint              
//	RS> 21   context_info                java.sql.Types.VARBINARY varbinary(256)      
//	RS> 22   percent_complete            java.sql.Types.REAL      real                
//	RS> 23   estimated_completion_time   java.sql.Types.BIGINT    bigint              
//	RS> 24   cpu_time                    java.sql.Types.INTEGER   int                 
//	RS> 25   total_elapsed_time          java.sql.Types.INTEGER   int                 
//	RS> 26   scheduler_id                java.sql.Types.INTEGER   int                 
//	RS> 27   task_address                java.sql.Types.VARBINARY varbinary(16)       
//	RS> 28   reads                       java.sql.Types.BIGINT    bigint              
//	RS> 29   writes                      java.sql.Types.BIGINT    bigint              
//	RS> 30   logical_reads               java.sql.Types.BIGINT    bigint              
//	RS> 31   text_size                   java.sql.Types.INTEGER   int                 
//	RS> 32   language                    java.sql.Types.NVARCHAR  nvarchar(128)       
//	RS> 33   date_format                 java.sql.Types.NVARCHAR  nvarchar(3)         
//	RS> 34   date_first                  java.sql.Types.SMALLINT  smallint            
//	RS> 35   quoted_identifier           java.sql.Types.BIT       bit                 
//	RS> 36   arithabort                  java.sql.Types.BIT       bit                 
//	RS> 37   ansi_null_dflt_on           java.sql.Types.BIT       bit                 
//	RS> 38   ansi_defaults               java.sql.Types.BIT       bit                 
//	RS> 39   ansi_warnings               java.sql.Types.BIT       bit                 
//	RS> 40   ansi_padding                java.sql.Types.BIT       bit                 
//	RS> 41   ansi_nulls                  java.sql.Types.BIT       bit                 
//	RS> 42   concat_null_yields_null     java.sql.Types.BIT       bit                 
//	RS> 43   transaction_isolation_level java.sql.Types.SMALLINT  smallint            
//	RS> 44   lock_timeout                java.sql.Types.INTEGER   int                 
//	RS> 45   deadlock_priority           java.sql.Types.INTEGER   int                 
//	RS> 46   row_count                   java.sql.Types.BIGINT    bigint              
//	RS> 47   prev_error                  java.sql.Types.INTEGER   int                 
//	RS> 48   nest_level                  java.sql.Types.INTEGER   int                 
//	RS> 49   granted_query_memory        java.sql.Types.INTEGER   int                 
//	RS> 50   executing_managed_code      java.sql.Types.BIT       bit                 
//	RS> 51   group_id                    java.sql.Types.INTEGER   int                 
//	RS> 52   query_hash                  java.sql.Types.BINARY    binary(16)          
//	RS> 53   query_plan_hash             java.sql.Types.BINARY    binary(16)          	

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

		return new CmExecRequests(counterController, guiController);
	}

	public CmExecRequests(ICounterController counterController, IGuiController guiController)
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
		String dm_exec_requests = "dm_exec_requests";
		
		if (isAzure)
			dm_exec_requests = "dm_exec_requests";  // SAME NAME IN AZURE ???

		
		String sql = "select * from sys." + dm_exec_requests;

		return sql;
	}
}
