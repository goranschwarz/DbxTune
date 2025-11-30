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

import java.lang.invoke.MethodHandles;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import javax.naming.NameNotFoundException;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.dbxtune.ICounterController;
import com.dbxtune.IGuiController;
import com.dbxtune.central.pcs.CentralPersistReader;
import com.dbxtune.cm.CounterSample;
import com.dbxtune.cm.CounterSetTemplates;
import com.dbxtune.cm.CounterSetTemplates.Type;
import com.dbxtune.cm.CountersModel;
import com.dbxtune.config.dict.MonTablesDictionary;
import com.dbxtune.config.dict.MonTablesDictionaryManager;
import com.dbxtune.graph.TrendGraphDataPoint;
import com.dbxtune.graph.TrendGraphDataPoint.LabelType;
import com.dbxtune.gui.MainFrame;
import com.dbxtune.sql.conn.DbxConnection;
import com.dbxtune.sql.conn.info.DbmsVersionInfo;
import com.dbxtune.sql.conn.info.DbmsVersionInfoSqlServer;

/**
 * @author Goran Schwarz (goran_schwarz@hotmail.com)
 */
public class CmIndexOpStatSum
extends CountersModel
{
	private static final Logger _logger = LogManager.getLogger(MethodHandles.lookup().lookupClass());
	private static final long    serialVersionUID = 1L;

	public static final String   CM_NAME          = CmIndexOpStatSum.class.getSimpleName();
	public static final String   SHORT_NAME       = "Index Op Sum";
	public static final String   HTML_DESC        = 
		"<html>" +
		"<p>Get Table Level Information (like Ins/Upd/Del, lock_wait_time/count etc) on a Database Level</p>" +
		"</html>";

	public static final String   GROUP_NAME       = MainFrame.TCP_GROUP_OBJECT_ACCESS;
	public static final String   GUI_ICON_FILE    = "images/"+CM_NAME+".png";

	public static final long     NEED_SRV_VERSION = 0;
	public static final long     NEED_CE_VERSION  = 0;

	public static final String[] MON_TABLES       = new String[] {CM_NAME, "dm_db_index_operational_stats"};
	public static final String[] NEED_ROLES       = new String[] {"VIEW SERVER STATE", "CONNECT ANY DATABASE"};
	public static final String[] NEED_CONFIG      = new String[] {};

	public static final String[] PCT_COLUMNS      = new String[] {};
	public static final String[] DIFF_COLUMNS     = new String[] {
		"leaf_IUD_count",
		"leaf_insert_count",
		"leaf_delete_count",
		"leaf_update_count",
		"leaf_ghost_count",
		"nonleaf_insert_count",
		"nonleaf_delete_count",
		"nonleaf_update_count",
		"leaf_allocation_count",
		"nonleaf_allocation_count",
		"leaf_page_merge_count",
		"nonleaf_page_merge_count",
		"range_scan_count",
		"singleton_lookup_count",
		"forwarded_fetch_count",
		"lob_fetch_in_pages",
		"lob_fetch_in_bytes",
		"lob_orphan_create_count",
		"lob_orphan_insert_count",
		"row_overflow_fetch_in_pages",
		"row_overflow_fetch_in_bytes",
		"column_value_push_off_row_count",
		"column_value_pull_in_row_count",
		"row_lock_count",
		"row_lock_wait_count",
		"row_lock_wait_in_ms",
		"page_lock_count",
		"page_lock_wait_count",
		"page_lock_wait_in_ms",
		"index_lock_promotion_attempt_count",
		"index_lock_promotion_count",
		"page_latch_wait_count",
		"page_latch_wait_in_ms",
		"page_io_latch_wait_count",
		"page_io_latch_wait_in_ms",
		"tree_page_latch_wait_count",
		"tree_page_latch_wait_in_ms",
		"tree_page_io_latch_wait_count",
		"tree_page_io_latch_wait_in_ms",
		"page_compression_attempt_count",
		"page_compression_success_count",
		"_last_column_name_only_used_as_a_place_holder_here"
		};

// Microsoft SQL Server 2008 R2 (SP2) - 10.50.4000.0 (X64)  	Jun 28 2012 08:36:30  	Copyright (c) Microsoft Corporation 	Express Edition with Advanced Services (64-bit) on Windows NT 6.1 <X64> (Build 7601: Service Pack 1)

//	RS> Col# Label                              JDBC Type Name          Guessed DBMS type
//	RS> ---- ---------------------------------- ----------------------- -----------------
//	RS> 1    dbname                             java.sql.Types.NVARCHAR nvarchar(128)    
//	RS> 2    objectName                         java.sql.Types.NVARCHAR nvarchar(128)    
//	RS> 3    database_id                        java.sql.Types.SMALLINT smallint         
//	RS> 4    object_id                          java.sql.Types.INTEGER  int              
//	RS> 5    index_id                           java.sql.Types.INTEGER  int              
//	RS> 6    partition_number                   java.sql.Types.INTEGER  int              
//	RS> 7    leaf_insert_count                  java.sql.Types.BIGINT   bigint           
//	RS> 8    leaf_delete_count                  java.sql.Types.BIGINT   bigint           
//	RS> 9    leaf_update_count                  java.sql.Types.BIGINT   bigint           
//	RS> 10   leaf_ghost_count                   java.sql.Types.BIGINT   bigint           
//	RS> 11   nonleaf_insert_count               java.sql.Types.BIGINT   bigint           
//	RS> 12   nonleaf_delete_count               java.sql.Types.BIGINT   bigint           
//	RS> 13   nonleaf_update_count               java.sql.Types.BIGINT   bigint           
//	RS> 14   leaf_allocation_count              java.sql.Types.BIGINT   bigint           
//	RS> 15   nonleaf_allocation_count           java.sql.Types.BIGINT   bigint           
//	RS> 16   leaf_page_merge_count              java.sql.Types.BIGINT   bigint           
//	RS> 17   nonleaf_page_merge_count           java.sql.Types.BIGINT   bigint           
//	RS> 18   range_scan_count                   java.sql.Types.BIGINT   bigint           
//	RS> 19   singleton_lookup_count             java.sql.Types.BIGINT   bigint           
//	RS> 20   forwarded_fetch_count              java.sql.Types.BIGINT   bigint           
//	RS> 21   lob_fetch_in_pages                 java.sql.Types.BIGINT   bigint           
//	RS> 22   lob_fetch_in_bytes                 java.sql.Types.BIGINT   bigint           
//	RS> 23   lob_orphan_create_count            java.sql.Types.BIGINT   bigint           
//	RS> 24   lob_orphan_insert_count            java.sql.Types.BIGINT   bigint           
//	RS> 25   row_overflow_fetch_in_pages        java.sql.Types.BIGINT   bigint           
//	RS> 26   row_overflow_fetch_in_bytes        java.sql.Types.BIGINT   bigint           
//	RS> 27   column_value_push_off_row_count    java.sql.Types.BIGINT   bigint           
//	RS> 28   column_value_pull_in_row_count     java.sql.Types.BIGINT   bigint           
//	RS> 29   row_lock_count                     java.sql.Types.BIGINT   bigint           
//	RS> 30   row_lock_wait_count                java.sql.Types.BIGINT   bigint           
//	RS> 31   row_lock_wait_in_ms                java.sql.Types.BIGINT   bigint           
//	RS> 32   page_lock_count                    java.sql.Types.BIGINT   bigint           
//	RS> 33   page_lock_wait_count               java.sql.Types.BIGINT   bigint           
//	RS> 34   page_lock_wait_in_ms               java.sql.Types.BIGINT   bigint           
//	RS> 35   index_lock_promotion_attempt_count java.sql.Types.BIGINT   bigint           
//	RS> 36   index_lock_promotion_count         java.sql.Types.BIGINT   bigint           
//	RS> 37   page_latch_wait_count              java.sql.Types.BIGINT   bigint           
//	RS> 38   page_latch_wait_in_ms              java.sql.Types.BIGINT   bigint           
//	RS> 39   page_io_latch_wait_count           java.sql.Types.BIGINT   bigint           
//	RS> 40   page_io_latch_wait_in_ms           java.sql.Types.BIGINT   bigint           
//	RS> 41   tree_page_latch_wait_count         java.sql.Types.BIGINT   bigint           
//	RS> 42   tree_page_latch_wait_in_ms         java.sql.Types.BIGINT   bigint           
//	RS> 43   tree_page_io_latch_wait_count      java.sql.Types.BIGINT   bigint           
//	RS> 44   tree_page_io_latch_wait_in_ms      java.sql.Types.BIGINT   bigint           
//	RS> 45   page_compression_attempt_count     java.sql.Types.BIGINT   bigint           
//	RS> 46   page_compression_success_count     java.sql.Types.BIGINT   bigint           	

	public static final boolean  NEGATIVE_DIFF_COUNTERS_TO_ZERO = true; // entries for 'tempdb' will vary since objects are dropped (which cases negative diff-values)
	public static final boolean  IS_SYSTEM_CM                   = true;
	public static final int      DEFAULT_POSTPONE_TIME          = 0;
//	public static final int      DEFAULT_POSTPONE_TIME          = 300; // 5m -- If we want to align more to CmIndexOpStat & CmExecQueryStatPerDb
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

		return new CmIndexOpStatSum(counterController, guiController);
	}

	public CmIndexOpStatSum(ICounterController counterController, IGuiController guiController)
	{
		super(counterController, guiController,
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
	public static final String GRAPH_NAME_IUD_INSTANCE                              = "OpIudInstance";
	public static final String GRAPH_NAME_IUD_TEMPDB                                = "OpIudTempdb";
//	public static final String GRAPH_NAME_IUD_PER_DB                                = "OpIudPerDb";
                                                                                    
	public static final String GRAPH_NAME_PER_DB_LEAF_IUD_COUNT                     = "OpLeafIudCount";                   // leaf_IUD_count
	public static final String GRAPH_NAME_PER_DB_LEAF_INSERT_COUNT                  = "OpLeafInsertCount";                // leaf_insert_count
	public static final String GRAPH_NAME_PER_DB_LEAF_UPDATE_COUNT                  = "OpLeafUpdateCount";                // leaf_update_count
	public static final String GRAPH_NAME_PER_DB_LEAF_DELETE_COUNT                  = "OpLeafDeleteCount";                // leaf_delete_count
	public static final String GRAPH_NAME_PER_DB_LEAF_GHOST_COUNT                   = "OpLeafGhostCount";                 // leaf_ghost_count

	public static final String GRAPH_NAME_PER_DB_LEAF_ALLOCATION_COUNT              = "OpLeafAllocationCount";            // leaf_allocation_count
	public static final String GRAPH_NAME_PER_DB_LEAF_PAGE_MERGE_COUNT              = "OpLeafPageMergeCount";             // leaf_page_merge_count
	public static final String GRAPH_NAME_PER_DB_RANGE_SCAN_COUNT                   = "OpRangeScanCount";                 // range_scan_count
	public static final String GRAPH_NAME_PER_DB_SINGLETON_LOOKUP_COUNT             = "OpSingletonLookupCount";           // singleton_lookup_count
	public static final String GRAPH_NAME_PER_DB_FORWARD_FETCH_COUNT                = "OpForwardFetchCount";              // forwarded_fetch_count
	public static final String GRAPH_NAME_PER_DB_LOB_FETCH_IN_BYTES                 = "OpLobFetchInBytes";                // lob_fetch_in_bytes
	public static final String GRAPH_NAME_PER_DB_ROW_OVERFLOW_FETCH_IN_BYTES        = "OpRowOverflowFetchInBytes";        // row_overflow_fetch_in_bytes
	public static final String GRAPH_NAME_PER_DB_COLUMN_VALUE_PUSH_OFF_ROW_COUNT    = "OpColumnValuePushOffRowCount";     // column_value_push_off_row_count
	public static final String GRAPH_NAME_PER_DB_COLUMN_VALUE_PULL_IN_ROW_COUNT     = "OpColumnValuePullInRowCount";      // column_value_pull_in_row_count
                                                                                                                          
	public static final String GRAPH_NAME_PER_DB_ROW_LOCK_COUNT                     = "OpRowLockCount";                   // row_lock_count
	public static final String GRAPH_NAME_PER_DB_ROW_LOCK_WAIT_COUNT                = "OpRowLockWaitCount";               // row_lock_wait_count
	public static final String GRAPH_NAME_PER_DB_ROW_LOCK_WAIT_MS                   = "OpRowLockWaitMs";                  // row_lock_wait_in_ms
	public static final String GRAPH_NAME_PER_DB_ROW_LOCK_WAIT_MS_PER_COUNT         = "OpRowLockWaitMsPerCount";          // row_lock_wait_in_ms_per_count
                                                                                                                          
	public static final String GRAPH_NAME_PER_DB_PAGE_LOCK_COUNT                    = "OpPageLockCount";                  // page_lock_count
	public static final String GRAPH_NAME_PER_DB_PAGE_LOCK_WAIT_COUNT               = "OpPageLockWaitCount";              // page_lock_wait_count
	public static final String GRAPH_NAME_PER_DB_PAGE_LOCK_WAIT_MS                  = "OpPageLockWaitMs";                 // page_lock_wait_in_ms
	public static final String GRAPH_NAME_PER_DB_PAGE_LOCK_WAIT_MS_PER_COUNT        = "OpPageLockWaitMsPerCount";         // page_lock_wait_in_ms_per_count
	                                                                                                                      
	public static final String GRAPH_NAME_PER_DB_PAGE_LATCH_WAIT_COUNT              = "OpPageLatchWaitCount";             // page_latch_wait_count
	public static final String GRAPH_NAME_PER_DB_PAGE_LATCH_WAIT_MS                 = "OpPageLatchWaitMs";                // page_latch_wait_in_ms
	public static final String GRAPH_NAME_PER_DB_PAGE_LATCH_WAIT_MS_PER_COUNT       = "OpPageLatchWaitMsPerCount";        // page_latch_wait_in_ms_per_count

	public static final String GRAPH_NAME_PER_DB_PAGE_IO_LATCH_WAIT_COUNT           = "OpPageIoLatchWaitCount";           // page_io_latch_wait_count
	public static final String GRAPH_NAME_PER_DB_PAGE_IO_LATCH_WAIT_MS              = "OpPageIoLatchWaitMs";              // page_io_latch_wait_in_ms
	public static final String GRAPH_NAME_PER_DB_PAGE_IO_LATCH_WAIT_MS_PER_COUNT    = "OpPageIoLatchWaitMsPerCount";      // page_io_latch_wait_in_ms_per_count
                                                                                    
//	public static final String GRAPH_NAME_PER_DB_INDEX_LOCK_PROMATION_ATTEMPT_COUNT = "OpIndexLockPromotionAttemptCount"; // index_lock_promotion_attempt_count
//	public static final String GRAPH_NAME_PER_DB_INDEX_LOCK_PROMATION_COUNT         = "OpIndexLockPromotionCount";        // index_lock_promotion_count
	public static final String GRAPH_NAME_PER_DB_INDEX_LOCK_PROMATION_ATTEMPT_COUNT = "OpIndexLockPromoAttemptCount";     // index_lock_promotion_attempt_count
	public static final String GRAPH_NAME_PER_DB_INDEX_LOCK_PROMATION_COUNT         = "OpIndexLockPromoCount";            // index_lock_promotion_count
	
	public static final String GRAPH_NAME_PER_DB_PAGE_COMPRESSION_ATTEMPT_COUNT     = "OpPageCompressionAttemptCount";    // page_compression_attempt_count
	public static final String GRAPH_NAME_PER_DB_PAGE_COMPRESSION_SUCCESS_COUNT     = "OpPageCompressionSuccessCount";    // page_compression_success_count
	
	private void addTrendGraphs()
	{
		//-----
		addTrendGraph(GRAPH_NAME_IUD_INSTANCE,
			"Insert/Update/Delete Rows Per Second at Instance Level", // Menu CheckBox text
			"Insert/Update/Delete Rows Per Second at Instance Level ("+GROUP_NAME+"->"+SHORT_NAME+")", // Label 
			TrendGraphDataPoint.createGraphProps(TrendGraphDataPoint.Y_AXIS_SCALE_LABELS_COUNT, CentralPersistReader.SampleType.MAX_OVER_SAMPLES),
			new String[] {"IUD_count", "insert_count", "update_count", "delete_count", "delete_ghost_count"}, 
			LabelType.Static,
			TrendGraphDataPoint.Category.OPERATIONS,
			false,  // is Percent Graph
			true,  // visible at start
			0,     // graph is valid from Server Version. 0 = All Versions; >0 = Valid from this version and above 
			-1);   // minimum height

		//-----
		addTrendGraph(GRAPH_NAME_IUD_TEMPDB,
			"Insert/Update/Delete Rows Per Second for tempdb", // Menu CheckBox text
			"Insert/Update/Delete Rows Per Second for tempdb ("+GROUP_NAME+"->"+SHORT_NAME+")", // Label 
			TrendGraphDataPoint.createGraphProps(TrendGraphDataPoint.Y_AXIS_SCALE_LABELS_COUNT, CentralPersistReader.SampleType.MAX_OVER_SAMPLES),
			new String[] {"IUD_count", "insert_count", "update_count", "delete_count", "delete_ghost_count"}, 
			LabelType.Static,
			TrendGraphDataPoint.Category.OPERATIONS,
			false,  // is Percent Graph
			true,  // visible at start
			0,     // graph is valid from Server Version. 0 = All Versions; >0 = Valid from this version and above 
			-1);   // minimum height




		//-----
		addTrendGraph(GRAPH_NAME_PER_DB_LEAF_IUD_COUNT,
			"Insert/Update/Delete Rows Per Second per Database [leaf_IUD_count]", // Menu CheckBox text
			"Insert/Update/Delete Rows Per Second per Database [leaf_IUD_count] ("+GROUP_NAME+"->"+SHORT_NAME+")", // Label 
			TrendGraphDataPoint.createGraphProps(TrendGraphDataPoint.Y_AXIS_SCALE_LABELS_COUNT, CentralPersistReader.SampleType.MAX_OVER_SAMPLES),
			null, 
			LabelType.Dynamic,
			TrendGraphDataPoint.Category.OPERATIONS,
			false,  // is Percent Graph
			false,  // visible at start
			0,     // graph is valid from Server Version. 0 = All Versions; >0 = Valid from this version and above 
			-1);   // minimum height

		//-----
		addTrendGraph(GRAPH_NAME_PER_DB_LEAF_INSERT_COUNT,
			"Insert Rows Per Second per Database [leaf_insert_count]", // Menu CheckBox text
			"Insert Rows Per Second per Database [leaf_insert_count] ("+GROUP_NAME+"->"+SHORT_NAME+")", // Label 
			TrendGraphDataPoint.createGraphProps(TrendGraphDataPoint.Y_AXIS_SCALE_LABELS_COUNT, CentralPersistReader.SampleType.MAX_OVER_SAMPLES),
			null, 
			LabelType.Dynamic,
			TrendGraphDataPoint.Category.OPERATIONS,
			false,  // is Percent Graph
			false,  // visible at start
			0,     // graph is valid from Server Version. 0 = All Versions; >0 = Valid from this version and above 
			-1);   // minimum height

		//-----
		addTrendGraph(GRAPH_NAME_PER_DB_LEAF_UPDATE_COUNT,
			"Update Rows Per Second per Database [leaf_update_count]", // Menu CheckBox text
			"Update Rows Per Second per Database [leaf_update_count] ("+GROUP_NAME+"->"+SHORT_NAME+")", // Label 
			TrendGraphDataPoint.createGraphProps(TrendGraphDataPoint.Y_AXIS_SCALE_LABELS_COUNT, CentralPersistReader.SampleType.MAX_OVER_SAMPLES),
			null, 
			LabelType.Dynamic,
			TrendGraphDataPoint.Category.OPERATIONS,
			false,  // is Percent Graph
			false,  // visible at start
			0,     // graph is valid from Server Version. 0 = All Versions; >0 = Valid from this version and above 
			-1);   // minimum height

		//-----
		addTrendGraph(GRAPH_NAME_PER_DB_LEAF_DELETE_COUNT,
			"Delete Rows Per Second per Database [leaf_delete_count]", // Menu CheckBox text
			"Delete Rows Per Second per Database [leaf_delete_count] ("+GROUP_NAME+"->"+SHORT_NAME+")", // Label 
			TrendGraphDataPoint.createGraphProps(TrendGraphDataPoint.Y_AXIS_SCALE_LABELS_COUNT, CentralPersistReader.SampleType.MAX_OVER_SAMPLES),
			null, 
			LabelType.Dynamic,
			TrendGraphDataPoint.Category.OPERATIONS,
			false,  // is Percent Graph
			false,  // visible at start
			0,     // graph is valid from Server Version. 0 = All Versions; >0 = Valid from this version and above 
			-1);   // minimum height

		//-----
		addTrendGraph(GRAPH_NAME_PER_DB_LEAF_GHOST_COUNT,
			"Ghost Delete Rows Per Second per Database [leaf_ghost_count]", // Menu CheckBox text
			"Ghost Delete Rows Per Second per Database [leaf_ghost_count] ("+GROUP_NAME+"->"+SHORT_NAME+")", // Label 
			TrendGraphDataPoint.createGraphProps(TrendGraphDataPoint.Y_AXIS_SCALE_LABELS_COUNT, CentralPersistReader.SampleType.MAX_OVER_SAMPLES),
			null, 
			LabelType.Dynamic,
			TrendGraphDataPoint.Category.OPERATIONS,
			false,  // is Percent Graph
			false,  // visible at start
			0,     // graph is valid from Server Version. 0 = All Versions; >0 = Valid from this version and above 
			-1);   // minimum height




		//-----
		addTrendGraph(GRAPH_NAME_PER_DB_LEAF_ALLOCATION_COUNT,
			"Page Allocation/Split Per Second per Database [leaf_allocation_count]", // Menu CheckBox text
			"Page Allocation/Split Per Second per Database [leaf_allocation_count] ("+GROUP_NAME+"->"+SHORT_NAME+")", // Label 
			TrendGraphDataPoint.createGraphProps(TrendGraphDataPoint.Y_AXIS_SCALE_LABELS_COUNT, CentralPersistReader.SampleType.MAX_OVER_SAMPLES),
			null, 
			LabelType.Dynamic,
			TrendGraphDataPoint.Category.OPERATIONS,
			false,  // is Percent Graph
			false,  // visible at start
			0,     // graph is valid from Server Version. 0 = All Versions; >0 = Valid from this version and above 
			-1);   // minimum height

		//-----
		addTrendGraph(GRAPH_NAME_PER_DB_LEAF_PAGE_MERGE_COUNT,
			"Page Merges Per Second per Database [leaf_page_merge_count]", // Menu CheckBox text
			"Page Merges Per Second per Database [leaf_page_merge_count] ("+GROUP_NAME+"->"+SHORT_NAME+")", // Label 
			TrendGraphDataPoint.createGraphProps(TrendGraphDataPoint.Y_AXIS_SCALE_LABELS_COUNT, CentralPersistReader.SampleType.MAX_OVER_SAMPLES),
			null, 
			LabelType.Dynamic,
			TrendGraphDataPoint.Category.OPERATIONS,
			false,  // is Percent Graph
			false,  // visible at start
			0,     // graph is valid from Server Version. 0 = All Versions; >0 = Valid from this version and above 
			-1);   // minimum height

		//-----
		addTrendGraph(GRAPH_NAME_PER_DB_RANGE_SCAN_COUNT,
			"Range Scans Per Second per Database [range_scan_count]", // Menu CheckBox text
			"Range Scans Per Second per Database [range_scan_count] ("+GROUP_NAME+"->"+SHORT_NAME+")", // Label 
			TrendGraphDataPoint.createGraphProps(TrendGraphDataPoint.Y_AXIS_SCALE_LABELS_COUNT, CentralPersistReader.SampleType.MAX_OVER_SAMPLES),
			null, 
			LabelType.Dynamic,
			TrendGraphDataPoint.Category.OPERATIONS,
			false,  // is Percent Graph
			false,  // visible at start
			0,     // graph is valid from Server Version. 0 = All Versions; >0 = Valid from this version and above 
			-1);   // minimum height

		//-----
		addTrendGraph(GRAPH_NAME_PER_DB_SINGLETON_LOOKUP_COUNT,
			"Bookmark/Singleton Lookups Per Second per Database [singleton_lookup_count]", // Menu CheckBox text
			"Bookmark/Singleton Lookups Per Second per Database [singleton_lookup_count] ("+GROUP_NAME+"->"+SHORT_NAME+")", // Label 
			TrendGraphDataPoint.createGraphProps(TrendGraphDataPoint.Y_AXIS_SCALE_LABELS_COUNT, CentralPersistReader.SampleType.MAX_OVER_SAMPLES),
			null, 
			LabelType.Dynamic,
			TrendGraphDataPoint.Category.OPERATIONS,
			false,  // is Percent Graph
			false,  // visible at start
			0,     // graph is valid from Server Version. 0 = All Versions; >0 = Valid from this version and above 
			-1);   // minimum height

		//-----
		addTrendGraph(GRAPH_NAME_PER_DB_FORWARD_FETCH_COUNT,
			"Forward Fetch Count Rows Per Second per Database [forwarded_fetch_count]", // Menu CheckBox text
			"Forward Fetch Count Rows Per Second per Database [forwarded_fetch_count] ("+GROUP_NAME+"->"+SHORT_NAME+")", // Label 
			TrendGraphDataPoint.createGraphProps(TrendGraphDataPoint.Y_AXIS_SCALE_LABELS_COUNT, CentralPersistReader.SampleType.MAX_OVER_SAMPLES),
			null, 
			LabelType.Dynamic,
			TrendGraphDataPoint.Category.OPERATIONS,
			false,  // is Percent Graph
			false,  // visible at start
			0,     // graph is valid from Server Version. 0 = All Versions; >0 = Valid from this version and above 
			-1);   // minimum height

		//-----
		addTrendGraph(GRAPH_NAME_PER_DB_LOB_FETCH_IN_BYTES,
			"LOB Fetch In Bytes Per Second per Database [lob_fetch_in_bytes]", // Menu CheckBox text
			"LOB Fetch In Bytes Per Second per Database [lob_fetch_in_bytes] ("+GROUP_NAME+"->"+SHORT_NAME+")", // Label 
			TrendGraphDataPoint.createGraphProps(TrendGraphDataPoint.Y_AXIS_SCALE_LABELS_BYTES, CentralPersistReader.SampleType.MAX_OVER_SAMPLES),
			null, 
			LabelType.Dynamic,
			TrendGraphDataPoint.Category.OPERATIONS,
			false,  // is Percent Graph
			false,  // visible at start
			0,     // graph is valid from Server Version. 0 = All Versions; >0 = Valid from this version and above 
			-1);   // minimum height

		//-----
		addTrendGraph(GRAPH_NAME_PER_DB_ROW_OVERFLOW_FETCH_IN_BYTES,
			"Row Overflow Fetch In Bytes Per Second per Database [row_overflow_fetch_in_bytes]", // Menu CheckBox text
			"Row Overflow Fetch In Bytes Per Second per Database [row_overflow_fetch_in_bytes] ("+GROUP_NAME+"->"+SHORT_NAME+")", // Label 
			TrendGraphDataPoint.createGraphProps(TrendGraphDataPoint.Y_AXIS_SCALE_LABELS_BYTES, CentralPersistReader.SampleType.MAX_OVER_SAMPLES),
			null, 
			LabelType.Dynamic,
			TrendGraphDataPoint.Category.OPERATIONS,
			false,  // is Percent Graph
			false,  // visible at start
			0,     // graph is valid from Server Version. 0 = All Versions; >0 = Valid from this version and above 
			-1);   // minimum height

		//-----
		addTrendGraph(GRAPH_NAME_PER_DB_COLUMN_VALUE_PUSH_OFF_ROW_COUNT,
			"Column Pushed Off-Row Count Per Second per Database [column_value_push_off_row_count]", // Menu CheckBox text
			"Column Pushed Off-Row Count Per Second per Database [column_value_push_off_row_count] ("+GROUP_NAME+"->"+SHORT_NAME+")", // Label 
			TrendGraphDataPoint.createGraphProps(TrendGraphDataPoint.Y_AXIS_SCALE_LABELS_COUNT, CentralPersistReader.SampleType.MAX_OVER_SAMPLES),
			null, 
			LabelType.Dynamic,
			TrendGraphDataPoint.Category.OPERATIONS,
			false,  // is Percent Graph
			false,  // visible at start
			0,     // graph is valid from Server Version. 0 = All Versions; >0 = Valid from this version and above 
			-1);   // minimum height

		//-----
		addTrendGraph(GRAPH_NAME_PER_DB_COLUMN_VALUE_PULL_IN_ROW_COUNT,
			"Column Pulled In-Row Count Per Second per Database [column_value_pull_in_row_count]", // Menu CheckBox text
			"Column Pulled In-Row Count Per Second per Database [column_value_pull_in_row_count] ("+GROUP_NAME+"->"+SHORT_NAME+")", // Label 
			TrendGraphDataPoint.createGraphProps(TrendGraphDataPoint.Y_AXIS_SCALE_LABELS_COUNT, CentralPersistReader.SampleType.MAX_OVER_SAMPLES),
			null, 
			LabelType.Dynamic,
			TrendGraphDataPoint.Category.OPERATIONS,
			false,  // is Percent Graph
			false,  // visible at start
			0,     // graph is valid from Server Version. 0 = All Versions; >0 = Valid from this version and above 
			-1);   // minimum height




		//-----
		addTrendGraph(GRAPH_NAME_PER_DB_ROW_LOCK_COUNT,
			"Row Lock Count Per Second per Database [row_lock_count]", // Menu CheckBox text
			"Row Lock Count Per Second per Database [row_lock_count] ("+GROUP_NAME+"->"+SHORT_NAME+")", // Label 
			TrendGraphDataPoint.createGraphProps(TrendGraphDataPoint.Y_AXIS_SCALE_LABELS_COUNT, CentralPersistReader.SampleType.MAX_OVER_SAMPLES),
			null, 
			LabelType.Dynamic,
			TrendGraphDataPoint.Category.OPERATIONS,
			false,  // is Percent Graph
			false,  // visible at start
			0,     // graph is valid from Server Version. 0 = All Versions; >0 = Valid from this version and above 
			-1);   // minimum height

		//-----
		addTrendGraph(GRAPH_NAME_PER_DB_ROW_LOCK_WAIT_COUNT,
			"Row Lock Wait Count Per Second per Database [row_lock_count]", // Menu CheckBox text
			"Row Lock Wait Count Per Second per Database [row_lock_count] ("+GROUP_NAME+"->"+SHORT_NAME+")", // Label 
			TrendGraphDataPoint.createGraphProps(TrendGraphDataPoint.Y_AXIS_SCALE_LABELS_COUNT, CentralPersistReader.SampleType.MAX_OVER_SAMPLES),
			null, 
			LabelType.Dynamic,
			TrendGraphDataPoint.Category.OPERATIONS,
			false,  // is Percent Graph
			false,  // visible at start
			0,     // graph is valid from Server Version. 0 = All Versions; >0 = Valid from this version and above 
			-1);   // minimum height

		//-----
		addTrendGraph(GRAPH_NAME_PER_DB_ROW_LOCK_WAIT_MS,
			"Row Lock Wait Time in MS Per Second per Database [row_lock_wait_in_ms]", // Menu CheckBox text
			"Row Lock Wait Time in MS Per Second per Database [row_lock_wait_in_ms] ("+GROUP_NAME+"->"+SHORT_NAME+")", // Label 
			TrendGraphDataPoint.createGraphProps(TrendGraphDataPoint.Y_AXIS_SCALE_LABELS_MILLISEC, CentralPersistReader.SampleType.MAX_OVER_SAMPLES),
			null, 
			LabelType.Dynamic,
			TrendGraphDataPoint.Category.OPERATIONS,
			false,  // is Percent Graph
			false,  // visible at start
			0,     // graph is valid from Server Version. 0 = All Versions; >0 = Valid from this version and above 
			-1);   // minimum height

		//-----
		addTrendGraph(GRAPH_NAME_PER_DB_ROW_LOCK_WAIT_MS_PER_COUNT,
			"Avg Row Lock Wait Time in MS Per Count per Database [row_lock_wait_in_ms_per_count]", // Menu CheckBox text
			"Avg Row Lock Wait Time in MS Per Count per Database [row_lock_wait_in_ms_per_count] ("+GROUP_NAME+"->"+SHORT_NAME+")", // Label 
			TrendGraphDataPoint.createGraphProps(TrendGraphDataPoint.Y_AXIS_SCALE_LABELS_MILLISEC, CentralPersistReader.SampleType.MAX_OVER_SAMPLES),
			null, 
			LabelType.Dynamic,
			TrendGraphDataPoint.Category.OPERATIONS,
			false,  // is Percent Graph
			false,  // visible at start
			0,     // graph is valid from Server Version. 0 = All Versions; >0 = Valid from this version and above 
			-1);   // minimum height
		



		//-----
		addTrendGraph(GRAPH_NAME_PER_DB_PAGE_LOCK_COUNT,
			"Page Lock Count Per Second per Database [page_lock_count]", // Menu CheckBox text
			"Page Lock Count Per Second per Database [page_lock_count] ("+GROUP_NAME+"->"+SHORT_NAME+")", // Label 
			TrendGraphDataPoint.createGraphProps(TrendGraphDataPoint.Y_AXIS_SCALE_LABELS_COUNT, CentralPersistReader.SampleType.MAX_OVER_SAMPLES),
			null, 
			LabelType.Dynamic,
			TrendGraphDataPoint.Category.OPERATIONS,
			false,  // is Percent Graph
			false,  // visible at start
			0,     // graph is valid from Server Version. 0 = All Versions; >0 = Valid from this version and above 
			-1);   // minimum height

		//-----
		addTrendGraph(GRAPH_NAME_PER_DB_PAGE_LOCK_WAIT_COUNT,
			"Page Lock Wait Count Per Second per Database [page_lock_count]", // Menu CheckBox text
			"Page Lock Wait Count Per Second per Database [page_lock_count] ("+GROUP_NAME+"->"+SHORT_NAME+")", // Label 
			TrendGraphDataPoint.createGraphProps(TrendGraphDataPoint.Y_AXIS_SCALE_LABELS_COUNT, CentralPersistReader.SampleType.MAX_OVER_SAMPLES),
			null, 
			LabelType.Dynamic,
			TrendGraphDataPoint.Category.OPERATIONS,
			false,  // is Percent Graph
			false,  // visible at start
			0,     // graph is valid from Server Version. 0 = All Versions; >0 = Valid from this version and above 
			-1);   // minimum height

		//-----
		addTrendGraph(GRAPH_NAME_PER_DB_PAGE_LOCK_WAIT_MS,
			"Page Lock Wait Time in MS Per Second per Database [page_lock_wait_in_ms]", // Menu CheckBox text
			"Page Lock Wait Time in MS Per Second per Database [page_lock_wait_in_ms] ("+GROUP_NAME+"->"+SHORT_NAME+")", // Label 
			TrendGraphDataPoint.createGraphProps(TrendGraphDataPoint.Y_AXIS_SCALE_LABELS_MILLISEC, CentralPersistReader.SampleType.MAX_OVER_SAMPLES),
			null, 
			LabelType.Dynamic,
			TrendGraphDataPoint.Category.OPERATIONS,
			false,  // is Percent Graph
			false,  // visible at start
			0,     // graph is valid from Server Version. 0 = All Versions; >0 = Valid from this version and above 
			-1);   // minimum height

		//-----
		addTrendGraph(GRAPH_NAME_PER_DB_PAGE_LOCK_WAIT_MS_PER_COUNT,
			"Avg Page Lock Wait Time in MS Per Count per Database [page_lock_wait_in_ms_per_count]", // Menu CheckBox text
			"Avg Page Lock Wait Time in MS Per Count per Database [page_lock_wait_in_ms_per_count] ("+GROUP_NAME+"->"+SHORT_NAME+")", // Label 
			TrendGraphDataPoint.createGraphProps(TrendGraphDataPoint.Y_AXIS_SCALE_LABELS_MILLISEC, CentralPersistReader.SampleType.MAX_OVER_SAMPLES),
			null, 
			LabelType.Dynamic,
			TrendGraphDataPoint.Category.OPERATIONS,
			false,  // is Percent Graph
			false,  // visible at start
			0,     // graph is valid from Server Version. 0 = All Versions; >0 = Valid from this version and above 
			-1);   // minimum height




		//-----
		addTrendGraph(GRAPH_NAME_PER_DB_PAGE_LATCH_WAIT_COUNT,
			"Page Latch Wait Count Per Second per Database [page_latch_wait_count]", // Menu CheckBox text
			"Page Latch Wait Count Per Second per Database [page_latch_wait_count] ("+GROUP_NAME+"->"+SHORT_NAME+")", // Label 
			TrendGraphDataPoint.createGraphProps(TrendGraphDataPoint.Y_AXIS_SCALE_LABELS_COUNT, CentralPersistReader.SampleType.MAX_OVER_SAMPLES),
			null, 
			LabelType.Dynamic,
			TrendGraphDataPoint.Category.OPERATIONS,
			false,  // is Percent Graph
			false,  // visible at start
			0,     // graph is valid from Server Version. 0 = All Versions; >0 = Valid from this version and above 
			-1);   // minimum height

		//-----
		addTrendGraph(GRAPH_NAME_PER_DB_PAGE_LATCH_WAIT_MS,
			"Page Latch Wait Time in MS Per Second per Database [page_latch_wait_in_ms]", // Menu CheckBox text
			"Page Latch Wait Time in MS Per Second per Database [page_latch_wait_in_ms] ("+GROUP_NAME+"->"+SHORT_NAME+")", // Label 
			TrendGraphDataPoint.createGraphProps(TrendGraphDataPoint.Y_AXIS_SCALE_LABELS_MILLISEC, CentralPersistReader.SampleType.MAX_OVER_SAMPLES),
			null, 
			LabelType.Dynamic,
			TrendGraphDataPoint.Category.OPERATIONS,
			false,  // is Percent Graph
			false,  // visible at start
			0,     // graph is valid from Server Version. 0 = All Versions; >0 = Valid from this version and above 
			-1);   // minimum height

		//-----
		addTrendGraph(GRAPH_NAME_PER_DB_PAGE_LATCH_WAIT_MS_PER_COUNT,
			"Avg Page Latch Wait Time in MS Per Count per Database [page_latch_wait_in_ms_per_count]", // Menu CheckBox text
			"Avg Page Latch Wait Time in MS Per Count per Database [page_latch_wait_in_ms_per_count] ("+GROUP_NAME+"->"+SHORT_NAME+")", // Label 
			TrendGraphDataPoint.createGraphProps(TrendGraphDataPoint.Y_AXIS_SCALE_LABELS_MILLISEC, CentralPersistReader.SampleType.MAX_OVER_SAMPLES),
			null, 
			LabelType.Dynamic,
			TrendGraphDataPoint.Category.OPERATIONS,
			false,  // is Percent Graph
			false,  // visible at start
			0,     // graph is valid from Server Version. 0 = All Versions; >0 = Valid from this version and above 
			-1);   // minimum height




		//-----
		addTrendGraph(GRAPH_NAME_PER_DB_PAGE_IO_LATCH_WAIT_COUNT,
			"Page IO Latch Wait Count Per Second per Database [page_io_latch_wait_count]", // Menu CheckBox text
			"Page IO Latch Wait Count Per Second per Database [page_io_latch_wait_count] ("+GROUP_NAME+"->"+SHORT_NAME+")", // Label 
			TrendGraphDataPoint.createGraphProps(TrendGraphDataPoint.Y_AXIS_SCALE_LABELS_COUNT, CentralPersistReader.SampleType.MAX_OVER_SAMPLES),
			null, 
			LabelType.Dynamic,
			TrendGraphDataPoint.Category.OPERATIONS,
			false,  // is Percent Graph
			false,  // visible at start
			0,     // graph is valid from Server Version. 0 = All Versions; >0 = Valid from this version and above 
			-1);   // minimum height

		//-----
		addTrendGraph(GRAPH_NAME_PER_DB_PAGE_IO_LATCH_WAIT_MS,
			"Page IO Latch Wait Time in MS Per Second per Database [page_io_latch_wait_in_ms]", // Menu CheckBox text
			"Page IO Latch Wait Time in MS Per Second per Database [page_io_latch_wait_in_ms] ("+GROUP_NAME+"->"+SHORT_NAME+")", // Label 
			TrendGraphDataPoint.createGraphProps(TrendGraphDataPoint.Y_AXIS_SCALE_LABELS_MILLISEC, CentralPersistReader.SampleType.MAX_OVER_SAMPLES),
			null, 
			LabelType.Dynamic,
			TrendGraphDataPoint.Category.OPERATIONS,
			false,  // is Percent Graph
			false,  // visible at start
			0,     // graph is valid from Server Version. 0 = All Versions; >0 = Valid from this version and above 
			-1);   // minimum height

		//-----
		addTrendGraph(GRAPH_NAME_PER_DB_PAGE_IO_LATCH_WAIT_MS_PER_COUNT,
			"Avg Page IO Latch Wait Time in MS Per Count per Database [page_io_latch_wait_in_ms_per_count]", // Menu CheckBox text
			"Avg Page IO Latch Wait Time in MS Per Count per Database [page_io_latch_wait_in_ms_per_count] ("+GROUP_NAME+"->"+SHORT_NAME+")", // Label 
			TrendGraphDataPoint.createGraphProps(TrendGraphDataPoint.Y_AXIS_SCALE_LABELS_MILLISEC, CentralPersistReader.SampleType.MAX_OVER_SAMPLES),
			null, 
			LabelType.Dynamic,
			TrendGraphDataPoint.Category.OPERATIONS,
			false,  // is Percent Graph
			false,  // visible at start
			0,     // graph is valid from Server Version. 0 = All Versions; >0 = Valid from this version and above 
			-1);   // minimum height




		//-----
		addTrendGraph(GRAPH_NAME_PER_DB_INDEX_LOCK_PROMATION_ATTEMPT_COUNT,
			"Lock Promotion Attempt Count Per Second per Database [index_lock_promotion_attempt_count]", // Menu CheckBox text
			"Lock Promotion Attempt Count Per Second per Database [index_lock_promotion_attempt_count] ("+GROUP_NAME+"->"+SHORT_NAME+")", // Label 
			TrendGraphDataPoint.createGraphProps(TrendGraphDataPoint.Y_AXIS_SCALE_LABELS_COUNT, CentralPersistReader.SampleType.MAX_OVER_SAMPLES),
			null, 
			LabelType.Dynamic,
			TrendGraphDataPoint.Category.OPERATIONS,
			false,  // is Percent Graph
			false,  // visible at start
			0,     // graph is valid from Server Version. 0 = All Versions; >0 = Valid from this version and above 
			-1);   // minimum height

		//-----
		addTrendGraph(GRAPH_NAME_PER_DB_INDEX_LOCK_PROMATION_COUNT,
			"Lock Promotion Success Count Per Second per Database [index_lock_promotion_count]", // Menu CheckBox text
			"Lock Promotion Success Count Per Second per Database [index_lock_promotion_count] ("+GROUP_NAME+"->"+SHORT_NAME+")", // Label 
			TrendGraphDataPoint.createGraphProps(TrendGraphDataPoint.Y_AXIS_SCALE_LABELS_COUNT, CentralPersistReader.SampleType.MAX_OVER_SAMPLES),
			null, 
			LabelType.Dynamic,
			TrendGraphDataPoint.Category.OPERATIONS,
			false,  // is Percent Graph
			false,  // visible at start
			0,     // graph is valid from Server Version. 0 = All Versions; >0 = Valid from this version and above 
			-1);   // minimum height




		//-----
		addTrendGraph(GRAPH_NAME_PER_DB_PAGE_COMPRESSION_ATTEMPT_COUNT,
			"Page Compression Attempt Count Per Second per Database [page_compression_attempt_count]", // Menu CheckBox text
			"Page Compression Attempt Count Per Second per Database [page_compression_attempt_count] ("+GROUP_NAME+"->"+SHORT_NAME+")", // Label 
			TrendGraphDataPoint.createGraphProps(TrendGraphDataPoint.Y_AXIS_SCALE_LABELS_COUNT, CentralPersistReader.SampleType.MAX_OVER_SAMPLES),
			null, 
			LabelType.Dynamic,
			TrendGraphDataPoint.Category.OPERATIONS,
			false,  // is Percent Graph
			false,  // visible at start
			0,     // graph is valid from Server Version. 0 = All Versions; >0 = Valid from this version and above 
			-1);   // minimum height

		//-----
		addTrendGraph(GRAPH_NAME_PER_DB_PAGE_COMPRESSION_SUCCESS_COUNT,
			"Page Compression Success Count Per Second per Database [page_compression_success_count]", // Menu CheckBox text
			"Page Compression Success Count Per Second per Database [page_compression_success_count] ("+GROUP_NAME+"->"+SHORT_NAME+")", // Label 
			TrendGraphDataPoint.createGraphProps(TrendGraphDataPoint.Y_AXIS_SCALE_LABELS_COUNT, CentralPersistReader.SampleType.MAX_OVER_SAMPLES),
			null, 
			LabelType.Dynamic,
			TrendGraphDataPoint.Category.OPERATIONS,
			false,  // is Percent Graph
			false,  // visible at start
			0,     // graph is valid from Server Version. 0 = All Versions; >0 = Valid from this version and above 
			-1);   // minimum height

	}
	
	@Override
	public void updateGraphData(TrendGraphDataPoint tgdp)
	{
		// -----------------------------------------------------------------------------------------
		if (GRAPH_NAME_IUD_INSTANCE.equals(tgdp.getName()))
		{
			int aggRowId = getAggregatedRowId();
			
			Double[] dArray = new Double[5];

			dArray[0] = this.getRateValueAsDouble(aggRowId, "leaf_IUD_count");
			dArray[1] = this.getRateValueAsDouble(aggRowId, "leaf_insert_count");
			dArray[2] = this.getRateValueAsDouble(aggRowId, "leaf_update_count");
			dArray[3] = this.getRateValueAsDouble(aggRowId, "leaf_delete_count");
			dArray[4] = this.getRateValueAsDouble(aggRowId, "leaf_ghost_count");

			// Set the values
			tgdp.setDataPoint(this.getTimestamp(), dArray);
		}

		// -----------------------------------------------------------------------------------------
		if (GRAPH_NAME_IUD_TEMPDB.equals(tgdp.getName()))
		{
			int rowId = this.getRateRowIdForPkValue("tempdb");
			
			Double[] dArray = new Double[5];

			dArray[0] = this.getRateValueAsDouble(rowId, "leaf_IUD_count");
			dArray[1] = this.getRateValueAsDouble(rowId, "leaf_insert_count");
			dArray[2] = this.getRateValueAsDouble(rowId, "leaf_update_count");
			dArray[3] = this.getRateValueAsDouble(rowId, "leaf_delete_count");
			dArray[4] = this.getRateValueAsDouble(rowId, "leaf_ghost_count");

			// Set the values
			tgdp.setDataPoint(this.getTimestamp(), dArray);
		}

		// -----------------------------------------------------------------------------------------
		if (GRAPH_NAME_PER_DB_LEAF_IUD_COUNT                    .equals(tgdp.getName())) { addGraphData(tgdp, "leaf_IUD_count"); }
		if (GRAPH_NAME_PER_DB_LEAF_INSERT_COUNT                 .equals(tgdp.getName())) { addGraphData(tgdp, "leaf_insert_count"); }
		if (GRAPH_NAME_PER_DB_LEAF_UPDATE_COUNT                 .equals(tgdp.getName())) { addGraphData(tgdp, "leaf_update_count"); }
		if (GRAPH_NAME_PER_DB_LEAF_DELETE_COUNT                 .equals(tgdp.getName())) { addGraphData(tgdp, "leaf_delete_count"); }
		if (GRAPH_NAME_PER_DB_LEAF_GHOST_COUNT                  .equals(tgdp.getName())) { addGraphData(tgdp, "leaf_ghost_count"); }

		if (GRAPH_NAME_PER_DB_LEAF_ALLOCATION_COUNT             .equals(tgdp.getName())) { addGraphData(tgdp, "leaf_allocation_count"); }
		if (GRAPH_NAME_PER_DB_LEAF_PAGE_MERGE_COUNT             .equals(tgdp.getName())) { addGraphData(tgdp, "leaf_page_merge_count"); }
		if (GRAPH_NAME_PER_DB_RANGE_SCAN_COUNT                  .equals(tgdp.getName())) { addGraphData(tgdp, "range_scan_count"); }
		if (GRAPH_NAME_PER_DB_SINGLETON_LOOKUP_COUNT            .equals(tgdp.getName())) { addGraphData(tgdp, "singleton_lookup_count"); }
		if (GRAPH_NAME_PER_DB_FORWARD_FETCH_COUNT               .equals(tgdp.getName())) { addGraphData(tgdp, "forwarded_fetch_count"); }
		if (GRAPH_NAME_PER_DB_LOB_FETCH_IN_BYTES                .equals(tgdp.getName())) { addGraphData(tgdp, "lob_fetch_in_bytes"); }
		if (GRAPH_NAME_PER_DB_ROW_OVERFLOW_FETCH_IN_BYTES       .equals(tgdp.getName())) { addGraphData(tgdp, "row_overflow_fetch_in_bytes"); }
		if (GRAPH_NAME_PER_DB_COLUMN_VALUE_PUSH_OFF_ROW_COUNT   .equals(tgdp.getName())) { addGraphData(tgdp, "column_value_push_off_row_count"); }
		if (GRAPH_NAME_PER_DB_COLUMN_VALUE_PULL_IN_ROW_COUNT    .equals(tgdp.getName())) { addGraphData(tgdp, "column_value_pull_in_row_count"); }

		if (GRAPH_NAME_PER_DB_ROW_LOCK_COUNT                    .equals(tgdp.getName())) { addGraphData(tgdp, "row_lock_count"); }
		if (GRAPH_NAME_PER_DB_ROW_LOCK_WAIT_COUNT               .equals(tgdp.getName())) { addGraphData(tgdp, "row_lock_wait_count"); }
		if (GRAPH_NAME_PER_DB_ROW_LOCK_WAIT_MS                  .equals(tgdp.getName())) { addGraphData(tgdp, "row_lock_wait_in_ms"); }
		if (GRAPH_NAME_PER_DB_ROW_LOCK_WAIT_MS_PER_COUNT        .equals(tgdp.getName())) { addGraphData(tgdp, "row_lock_wait_in_ms_per_count"); }

		if (GRAPH_NAME_PER_DB_PAGE_LOCK_COUNT                   .equals(tgdp.getName())) { addGraphData(tgdp, "page_lock_count"); }
		if (GRAPH_NAME_PER_DB_PAGE_LOCK_WAIT_COUNT              .equals(tgdp.getName())) { addGraphData(tgdp, "page_lock_wait_count"); }
		if (GRAPH_NAME_PER_DB_PAGE_LOCK_WAIT_MS                 .equals(tgdp.getName())) { addGraphData(tgdp, "page_lock_wait_in_ms"); }
		if (GRAPH_NAME_PER_DB_PAGE_LOCK_WAIT_MS_PER_COUNT       .equals(tgdp.getName())) { addGraphData(tgdp, "page_lock_wait_in_ms_per_count"); }

		if (GRAPH_NAME_PER_DB_PAGE_LATCH_WAIT_COUNT             .equals(tgdp.getName())) { addGraphData(tgdp, "page_latch_wait_count"); }
		if (GRAPH_NAME_PER_DB_PAGE_LATCH_WAIT_MS                .equals(tgdp.getName())) { addGraphData(tgdp, "page_latch_wait_in_ms"); }
		if (GRAPH_NAME_PER_DB_PAGE_LATCH_WAIT_MS_PER_COUNT      .equals(tgdp.getName())) { addGraphData(tgdp, "page_latch_wait_in_ms_per_count"); }

		if (GRAPH_NAME_PER_DB_PAGE_IO_LATCH_WAIT_COUNT          .equals(tgdp.getName())) { addGraphData(tgdp, "page_io_latch_wait_count"); }
		if (GRAPH_NAME_PER_DB_PAGE_IO_LATCH_WAIT_MS             .equals(tgdp.getName())) { addGraphData(tgdp, "page_io_latch_wait_in_ms"); }
		if (GRAPH_NAME_PER_DB_PAGE_IO_LATCH_WAIT_MS_PER_COUNT   .equals(tgdp.getName())) { addGraphData(tgdp, "page_io_latch_wait_in_ms_per_count"); }

		if (GRAPH_NAME_PER_DB_INDEX_LOCK_PROMATION_ATTEMPT_COUNT.equals(tgdp.getName())) { addGraphData(tgdp, "index_lock_promotion_attempt_count"); }
		if (GRAPH_NAME_PER_DB_INDEX_LOCK_PROMATION_COUNT        .equals(tgdp.getName())) { addGraphData(tgdp, "index_lock_promotion_count"); }

		if (GRAPH_NAME_PER_DB_PAGE_COMPRESSION_ATTEMPT_COUNT    .equals(tgdp.getName())) { addGraphData(tgdp, "page_compression_attempt_count"); }
		if (GRAPH_NAME_PER_DB_PAGE_COMPRESSION_SUCCESS_COUNT    .equals(tgdp.getName())) { addGraphData(tgdp, "page_compression_success_count"); }
	}

	private void addGraphData(TrendGraphDataPoint tgdp, String colname)
	{
		int pos__colname = this.findColumn(colname);
		if (pos__colname == -1)
		{
			_logger.warn(getName() + ".addGraphData(graphName='" + tgdp.getName() + "', colname='" + colname + "'): missing columns. pos__colname=" + pos__colname + ". Exiting method without adding values to graph.");
			return;
		}
		
		int size = this.size();
		
		// Write 1 "line" for every database
		Double[] dArray = new Double[size];
		String[] lArray = new String[size];

		int aggRowId = getAggregatedRowId();
		dArray[0] = this.getRateValueAsDouble(aggRowId, pos__colname);
		lArray[0] = "_Total";

		int ap = 1;
		for (int r = 0; r < size; r++)
		{
			// The "_Total" is also part of the rows... But we have already added that first so; Skip it
			if (r == aggRowId)
				continue;

			String dbname = this.getAbsString        (r, "dbname");
			Double dvalue = this.getRateValueAsDouble(r, pos__colname);
			
			// Storage for Graph Tables has data type NUMERIC(16,2)
			// So if below that decimal point... but NOT ZERO, then add at least the minimum value, meaning 0.01
			// Possibly this should be implemented at the PCS (Persistent Counter Storage) layer... 
			if (dvalue != null && dvalue > 0.0 && dvalue < 0.01)
				dvalue = 0.01d;

			lArray[ap] = dbname;
			dArray[ap] = dvalue;
			
			ap++;
		}

		// Set the values
		tgdp.setDataPoint(this.getTimestamp(), lArray, dArray);
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
//		long srvVersion = versionInfo.getLongVersion();

		List <String> pkCols = new LinkedList<String>();

		pkCols.add("dbname");
		
		return pkCols;
	}

//	private List<SortOptions> _localSortOptions = null;
//
//	@Override
//	public List<SortOptions> getLocalSortOptions()
//	{
//		// Allocate the sorting specification only first time.
//		if (_localSortOptions == null)
//		{
//			_localSortOptions = new ArrayList<>();
//			
//			_localSortOptions.add(new SortOptions("row_lock_count", ColumnNameSensitivity.IN_SENSITIVE, SortOrder.DESCENDING, DataSortSensitivity.IN_SENSITIVE));
//		}
//		return _localSortOptions;
//	}

	
//TODO; // Add XXX Graphs
//TODO; // Add to DSR


	@Override
	public void addMonTableDictForVersion(DbxConnection conn, DbmsVersionInfo versionInfo)
	{
		try 
		{
			MonTablesDictionary mtd = MonTablesDictionaryManager.getInstance();

			String cmName = this.getName();
			mtd.addTable(cmName, HTML_DESC);

			mtd.addColumn(cmName, "leaf_IUD_count"                                ,"<html>Count of leaf-level inserts.</html>");
			mtd.addColumn(cmName, "leaf_insert_count"                             ,"<html>Count of leaf-level inserts.</html>");
			mtd.addColumn(cmName, "leaf_update_count"                             ,"<html>Count of leaf-level updates.</html>");
			mtd.addColumn(cmName, "leaf_delete_count"                             ,"<html>Count of leaf-level deletes. leaf_delete_count is only incremented for deleted records that are not marked as ghost first. For deleted records that are ghosted first, leaf_ghost_count is incremented instead.</html>");
			mtd.addColumn(cmName, "leaf_ghost_count"                              ,"<html>Count of leaf-level rows that are marked as deleted, but not yet removed. This count does not include records that are immediately deleted without being marked as ghost. These rows are removed by a cleanup thread at set intervals. This value does not include rows that are retained, because of an outstanding snapshot isolation transaction.</html>");
			mtd.addColumn(cmName, "leaf_allocation_count"                         ,"<html>Count of leaf-level page allocations in the index or heap.</html>");
			mtd.addColumn(cmName, "leaf_page_merge_count"                         ,"<html>Count of page merges at the leaf level. Always 0 for columnstore index.</html>");
			mtd.addColumn(cmName, "range_scan_count"                              ,"<html>Count of range and table scans started on the index or heap.</html>");
			mtd.addColumn(cmName, "singleton_lookup_count"                        ,"<html>Count of single row retrievals from the index or heap.</html>");
			mtd.addColumn(cmName, "forwarded_fetch_count"                         ,"<html>Count of rows that were fetched through a forwarding record.</html>");
			mtd.addColumn(cmName, "lob_fetch_in_pages"                            ,"<html>Count of large object (LOB) pages retrieved from the LOB_DATA allocation unit. These pages contain data that is stored in columns of type text, ntext, image, varchar(max), nvarchar(max), varbinary(max), and xml.</html>");
			mtd.addColumn(cmName, "lob_fetch_in_bytes"                            ,"<html>Count of LOB data bytes retrieved.</html>");
			mtd.addColumn(cmName, "lob_orphan_create_count"                       ,"<html>Count of orphan LOB values created for bulk operations.</html>");
			mtd.addColumn(cmName, "lob_orphan_insert_count"                       ,"<html>Count of orphan LOB values inserted during bulk operations.</html>");
			mtd.addColumn(cmName, "row_overflow_fetch_in_pages"                   ,"<html>count of row-overflow data pages retrieved from the ROW_OVERFLOW_DATA allocation unit.<br>These pages contain data stored in columns of type varchar(n), nvarchar(n), varbinary(n), and sql_variant that has been pushed off-row.</html>");
			mtd.addColumn(cmName, "row_overflow_fetch_in_bytes"                   ,"<html>Count of row-overflow data bytes retrieved.</html>");
			mtd.addColumn(cmName, "column_value_push_off_row_count"               ,"<html>Count of column values for LOB data and row-overflow data that is pushed off-row to make an inserted or updated row fit within a page.</html>");
			mtd.addColumn(cmName, "column_value_pull_in_row_count"                ,"<html>Count of column values for LOB data and row-overflow data that is pulled in-row. This occurs when an update operation frees up space in a record and provides an opportunity to pull in one or more off-row values from the LOB_DATA or ROW_OVERFLOW_DATA allocation units to the IN_ROW_DATA allocation unit.</html>");
			mtd.addColumn(cmName, "row_lock_count"                                ,"<html>Number of row locks requested.</html>");
			mtd.addColumn(cmName, "row_lock_wait_count"                           ,"<html>Number of times the Database Engine waited on a row lock.</html>");
			mtd.addColumn(cmName, "row_lock_wait_in_ms"                           ,"<html>Total number of milliseconds the Database Engine waited on a row lock.</html>");
			mtd.addColumn(cmName, "row_lock_wait_in_ms_per_count"                 ,"<html>Avg milliseconds each 'row_lock_wait_count' took.<br><b>Algirithm:</b> row_lock_wait_in_ms / row_lock_wait_count</html>");
			mtd.addColumn(cmName, "page_lock_count"                               ,"<html>Number of page locks requested.</html>");
			mtd.addColumn(cmName, "page_lock_wait_count"                          ,"<html>Number of times the Database Engine waited on a page lock.</html>");
			mtd.addColumn(cmName, "page_lock_wait_in_ms"                          ,"<html>Total number of milliseconds the Database Engine waited on a page lock.</html>");
			mtd.addColumn(cmName, "page_lock_wait_in_ms_per_count"                ,"<html>Avg milliseconds each 'page_lock_wait_count' took.<br><b>Algirithm:</b> page_lock_wait_in_ms / page_lock_wait_count</html>");
			mtd.addColumn(cmName, "page_latch_wait_count"                         ,"<html>number of times the Database Engine waited, because of latch contention.</html>");
			mtd.addColumn(cmName, "page_latch_wait_in_ms"                         ,"<html>Total number of milliseconds the Database Engine waited, because of latch contention.</html>");
			mtd.addColumn(cmName, "page_latch_wait_in_ms_per_count"               ,"<html>Avg milliseconds each 'page_latch_wait_count' took.<br><b>Algirithm:</b> page_latch_wait_in_ms / page_latch_wait_count</html>");
			mtd.addColumn(cmName, "page_io_latch_wait_count"                      ,"<html>Number of times the Database Engine waited on an I/O page latch.</html>");
			mtd.addColumn(cmName, "page_io_latch_wait_in_ms"                      ,"<html>Total number of milliseconds the Database Engine waited on a page I/O latch.</html>");
			mtd.addColumn(cmName, "page_io_latch_wait_in_ms_per_count"            ,"<html>Avg milliseconds each 'page_io_latch_wait_count' took.<br><b>Algirithm:</b> page_io_latch_wait_in_ms / page_io_latch_wait_count</html>");
			mtd.addColumn(cmName, "index_lock_promotion_attempt_count"            ,"<html>Number of times the Database Engine tried to escalate locks.</html>");
			mtd.addColumn(cmName, "index_lock_promotion_count"                    ,"<html>Number of times the Database Engine escalated locks.</html>");
			mtd.addColumn(cmName, "page_compression_attempt_count"                ,"<html>Number of pages that were evaluated for PAGE level compression for specific partitions of a table, index, or indexed view. Includes pages that were not compressed because significant savings could not be achieved. Always 0 for columnstore index.</html>");
			mtd.addColumn(cmName, "page_compression_success_count"                ,"<html>Number of data pages that were compressed by using PAGE compression for specific partitions of a table, index, or indexed view. Always 0 for columnstore index.</html>");
		}
		catch (NameNotFoundException e) 
		{
		//	_logger.warn("Problems in cm='" + CM_NAME + "', adding addMonTableDictForVersion. Caught: " + e); 
			System.out.println("Problems in cm='" + CM_NAME + "', adding addMonTableDictForVersion. Caught: " + e); 
		}
	}

	@Override
	public String getSqlForVersion(DbxConnection conn, DbmsVersionInfo versionInfo)
	{
		DbmsVersionInfoSqlServer ssVersionInfo = (DbmsVersionInfoSqlServer) versionInfo;

		String dm_db_index_operational_stats = "dm_db_index_operational_stats";
		
		if (ssVersionInfo.isAzureSynapseAnalytics())
			dm_db_index_operational_stats = "dm_db_index_operational_stats";   // IS THIS THE SAME NAME IN AZURE ?????

		String sql = ""
			    + "select \n"
			    + "     DB_NAME(database_id)                    as dbname \n"
			    + "    ,SUM(leaf_insert_count) \n"
			    + "     + SUM(leaf_delete_count) \n"
			    + "     + SUM(leaf_update_count) \n"
			    + "     + SUM(leaf_ghost_count)                 AS leaf_IUD_count \n"
			    + "    ,SUM(leaf_insert_count)                  AS leaf_insert_count \n"
			    + "    ,SUM(leaf_update_count)                  AS leaf_update_count \n"
			    + "    ,SUM(leaf_delete_count)                  AS leaf_delete_count \n"
			    + "    ,SUM(leaf_ghost_count)                   AS leaf_ghost_count \n"
			    + "    ,SUM(leaf_allocation_count)              AS leaf_allocation_count \n"
			    + "    ,SUM(leaf_page_merge_count)              AS leaf_page_merge_count \n"
			    + "    ,SUM(range_scan_count)                   AS range_scan_count \n"
			    + "    ,SUM(singleton_lookup_count)             AS singleton_lookup_count \n"
			    + "    ,SUM(forwarded_fetch_count)              AS forwarded_fetch_count \n"
			    + "    ,SUM(lob_fetch_in_pages)                 AS lob_fetch_in_pages \n"
			    + "    ,SUM(lob_fetch_in_bytes)                 AS lob_fetch_in_bytes \n"               // Should we have an option for this (since it might overflow)
			    + "    ,SUM(lob_orphan_create_count)            AS lob_orphan_create_count \n"
			    + "    ,SUM(lob_orphan_insert_count)            AS lob_orphan_insert_count \n"
			    + "    ,SUM(row_overflow_fetch_in_pages)        AS row_overflow_fetch_in_pages \n"
			    + "    ,SUM(row_overflow_fetch_in_bytes)        AS row_overflow_fetch_in_bytes \n"      // Should we have an option for this (since it might overflow)
			    + "    ,SUM(column_value_push_off_row_count)    AS column_value_push_off_row_count \n"
			    + "    ,SUM(column_value_pull_in_row_count)     AS column_value_pull_in_row_count \n"
			    + "    ,SUM(row_lock_count)                     AS row_lock_count \n"
			    + "    ,SUM(row_lock_wait_count)                AS row_lock_wait_count \n"
			    + "    ,SUM(row_lock_wait_in_ms)                AS row_lock_wait_in_ms \n"
			    + "    ,CAST((sum(row_lock_wait_in_ms)*1.0) / nullif(sum(row_lock_wait_count)*1.0,0) as numeric(15,3)) AS row_lock_wait_in_ms_per_count \n"
			    + "    ,SUM(page_lock_count)                    AS page_lock_count \n"
			    + "    ,SUM(page_lock_wait_count)               AS page_lock_wait_count \n"
			    + "    ,SUM(page_lock_wait_in_ms)               AS page_lock_wait_in_ms \n"
			    + "    ,CAST((sum(page_lock_wait_in_ms)*1.0) / nullif(sum(page_lock_wait_count)*1.0,0) as numeric(15,3)) AS page_lock_wait_in_ms_per_count \n"
			    + "    ,SUM(page_latch_wait_count)              AS page_latch_wait_count \n"
			    + "    ,SUM(page_latch_wait_in_ms)              AS page_latch_wait_in_ms \n"
			    + "    ,CAST((sum(page_latch_wait_in_ms)*1.0) / nullif(sum(page_latch_wait_count)*1.0,0) as numeric(15,3)) AS page_latch_wait_in_ms_per_count \n"
			    + "    ,SUM(page_io_latch_wait_count)           AS page_io_latch_wait_count \n"
			    + "    ,SUM(page_io_latch_wait_in_ms)           AS page_io_latch_wait_in_ms \n"
			    + "    ,CAST((sum(page_io_latch_wait_in_ms)*1.0) / nullif(sum(page_io_latch_wait_count)*1.0,0) as numeric(15,3)) AS page_io_latch_wait_in_ms_per_count \n"
			    + "    ,SUM(index_lock_promotion_attempt_count) AS index_lock_promotion_attempt_count \n"
			    + "    ,SUM(index_lock_promotion_count)         AS index_lock_promotion_count \n"
			    + "    ,SUM(page_compression_attempt_count)     AS page_compression_attempt_count \n"
			    + "    ,SUM(page_compression_success_count)     AS page_compression_success_count \n"
			    + "FROM sys." + dm_db_index_operational_stats + "(DEFAULT, DEFAULT, DEFAULT, DEFAULT) \n"
			    + "WHERE index_id in(0, 1) \n"
			    + "GROUP BY database_id \n"
			    + "ORDER BY 1 \n"
			    + "";

		return sql;
	}

	@Override
	public void localCalculation(CounterSample prevSample, CounterSample newSample, CounterSample diffData)
	{
		calcWaitTimePerCount(diffData, "row_lock_wait_count",      "row_lock_wait_in_ms",      "row_lock_wait_in_ms_per_count");
		calcWaitTimePerCount(diffData, "page_lock_wait_count",     "page_lock_wait_in_ms",     "page_lock_wait_in_ms_per_count");
		calcWaitTimePerCount(diffData, "page_latch_wait_count",    "page_latch_wait_in_ms",    "page_latch_wait_in_ms_per_count");
		calcWaitTimePerCount(diffData, "page_io_latch_wait_count", "page_io_latch_wait_in_ms", "page_io_latch_wait_in_ms_per_count");
	}

	private void calcWaitTimePerCount(CounterSample diffData, String countColName, String timeColName, String destColName)
	{
		int pos__countCol = diffData.findColumn(countColName);
		int pos__waitCol  = diffData.findColumn(timeColName);
		int pos__destCol  = diffData.findColumn(destColName);

		if (pos__countCol == -1 || pos__waitCol == -1 || pos__destCol == -1)
		{
			_logger.warn(getName() + ".calcWaitTimePerCount(countColName='" + countColName + "', timeColName='" + timeColName + "', destColName='" + destColName + "'): missing columns. pos__countCol=" + pos__countCol + ", pos__waitCol=" + pos__waitCol + ", pos__destCol=" + pos__destCol + ". Exiting method without actions.");
			return;
		}

		// Loop on all diffData rows
		for (int rowId = 0; rowId < diffData.getRowCount(); rowId++)
		{
			long countVal = diffData.getValueAsLong(rowId, pos__countCol, 0L);
			long timeVal  = diffData.getValueAsLong(rowId, pos__waitCol,  0L);

			// ROW
			if (countVal > 0)
			{
				int roundingDecimals = 3;
				double calc = (timeVal + 0.0) / (countVal + 0.0);

				BigDecimal newVal = new BigDecimal(calc).setScale(roundingDecimals, RoundingMode.HALF_EVEN); 
				diffData.setValueAt(newVal, rowId, pos__destCol);
			}
			else
			{
				diffData.setValueAt(new BigDecimal(0), rowId, pos__destCol);
			}
		}
	}
	
	//--------------------------------------------------------------------------------------
	//--------------------------------------------------------------------------------------
	//-- Aggregation
	//--------------------------------------------------------------------------------------
	//--------------------------------------------------------------------------------------
	@Override
	public Map<String, AggregationType> createAggregateColumns()
	{
		HashMap<String, AggregationType> aggColumns = new HashMap<>(getColumnCount());

		AggregationType tmp;
		
		// Create the columns :::::::::::::::::::::::::::::::::::::::::::::::::::::: And ADD it to the return Map 
		tmp = new AggregationType("leaf_IUD_count"                        , AggregationType.Agg.SUM);          aggColumns.put(tmp.getColumnName(), tmp);
		tmp = new AggregationType("leaf_insert_count"                     , AggregationType.Agg.SUM);          aggColumns.put(tmp.getColumnName(), tmp);
		tmp = new AggregationType("leaf_update_count"                     , AggregationType.Agg.SUM);          aggColumns.put(tmp.getColumnName(), tmp);
		tmp = new AggregationType("leaf_delete_count"                     , AggregationType.Agg.SUM);          aggColumns.put(tmp.getColumnName(), tmp);
		tmp = new AggregationType("leaf_ghost_count"                      , AggregationType.Agg.SUM);          aggColumns.put(tmp.getColumnName(), tmp);
		tmp = new AggregationType("leaf_allocation_count"                 , AggregationType.Agg.SUM);          aggColumns.put(tmp.getColumnName(), tmp);
		tmp = new AggregationType("leaf_page_merge_count"                 , AggregationType.Agg.SUM);          aggColumns.put(tmp.getColumnName(), tmp);
		tmp = new AggregationType("range_scan_count"                      , AggregationType.Agg.SUM);          aggColumns.put(tmp.getColumnName(), tmp);
		tmp = new AggregationType("singleton_lookup_count"                , AggregationType.Agg.SUM);          aggColumns.put(tmp.getColumnName(), tmp);
		tmp = new AggregationType("forwarded_fetch_count"                 , AggregationType.Agg.SUM);          aggColumns.put(tmp.getColumnName(), tmp);
		tmp = new AggregationType("lob_fetch_in_pages"                    , AggregationType.Agg.SUM);          aggColumns.put(tmp.getColumnName(), tmp);
		tmp = new AggregationType("lob_fetch_in_bytes"                    , AggregationType.Agg.SUM);          aggColumns.put(tmp.getColumnName(), tmp);
		tmp = new AggregationType("lob_orphan_create_count"               , AggregationType.Agg.SUM);          aggColumns.put(tmp.getColumnName(), tmp);
		tmp = new AggregationType("lob_orphan_insert_count"               , AggregationType.Agg.SUM);          aggColumns.put(tmp.getColumnName(), tmp);
		tmp = new AggregationType("row_overflow_fetch_in_pages"           , AggregationType.Agg.SUM);          aggColumns.put(tmp.getColumnName(), tmp);
		tmp = new AggregationType("row_overflow_fetch_in_bytes"           , AggregationType.Agg.SUM);          aggColumns.put(tmp.getColumnName(), tmp);
		tmp = new AggregationType("column_value_push_off_row_count"       , AggregationType.Agg.SUM);          aggColumns.put(tmp.getColumnName(), tmp);
		tmp = new AggregationType("column_value_pull_in_row_count"        , AggregationType.Agg.SUM);          aggColumns.put(tmp.getColumnName(), tmp);
		tmp = new AggregationType("row_lock_count"                        , AggregationType.Agg.SUM);          aggColumns.put(tmp.getColumnName(), tmp);
		tmp = new AggregationType("row_lock_wait_count"                   , AggregationType.Agg.SUM);          aggColumns.put(tmp.getColumnName(), tmp);
		tmp = new AggregationType("row_lock_wait_in_ms"                   , AggregationType.Agg.SUM);          aggColumns.put(tmp.getColumnName(), tmp);
		tmp = new AggregationType("row_lock_wait_in_ms_per_count"         , AggregationType.Agg.SUM);          aggColumns.put(tmp.getColumnName(), tmp);
		tmp = new AggregationType("page_lock_count"                       , AggregationType.Agg.SUM);          aggColumns.put(tmp.getColumnName(), tmp);
		tmp = new AggregationType("page_lock_wait_count"                  , AggregationType.Agg.SUM);          aggColumns.put(tmp.getColumnName(), tmp);
		tmp = new AggregationType("page_lock_wait_in_ms"                  , AggregationType.Agg.SUM);          aggColumns.put(tmp.getColumnName(), tmp);
		tmp = new AggregationType("page_lock_wait_in_ms_per_count"        , AggregationType.Agg.SUM);          aggColumns.put(tmp.getColumnName(), tmp);
		tmp = new AggregationType("index_lock_promotion_attempt_count"    , AggregationType.Agg.SUM);          aggColumns.put(tmp.getColumnName(), tmp);
		tmp = new AggregationType("index_lock_promotion_count"            , AggregationType.Agg.SUM);          aggColumns.put(tmp.getColumnName(), tmp);
		tmp = new AggregationType("page_latch_wait_count"                 , AggregationType.Agg.SUM);          aggColumns.put(tmp.getColumnName(), tmp);
		tmp = new AggregationType("page_latch_wait_in_ms"                 , AggregationType.Agg.SUM);          aggColumns.put(tmp.getColumnName(), tmp);
		tmp = new AggregationType("page_latch_wait_in_ms_per_count"       , AggregationType.Agg.SUM);          aggColumns.put(tmp.getColumnName(), tmp);
		tmp = new AggregationType("page_io_latch_wait_count"              , AggregationType.Agg.SUM);          aggColumns.put(tmp.getColumnName(), tmp);
		tmp = new AggregationType("page_io_latch_wait_in_ms"              , AggregationType.Agg.SUM);          aggColumns.put(tmp.getColumnName(), tmp);
		tmp = new AggregationType("page_io_latch_wait_in_ms_per_count"    , AggregationType.Agg.SUM);          aggColumns.put(tmp.getColumnName(), tmp);
		tmp = new AggregationType("page_compression_attempt_count"        , AggregationType.Agg.SUM);          aggColumns.put(tmp.getColumnName(), tmp);
		tmp = new AggregationType("page_compression_success_count"        , AggregationType.Agg.SUM);          aggColumns.put(tmp.getColumnName(), tmp);
		
		return aggColumns;
	}

//	@Override
//	public Object calculateAggregateRow_getAggregatePkColumnDataProvider(CounterSample newSample, String colName, int c, int jdbcType, Object addValue)
//	{
//		if ("xxxx".equalsIgnoreCase(colName)) return Integer.valueOf(-1);
//		
//		return addValue;
//	}

	@Override
	public Object calculateAggregateRow_nonAggregatedColumnDataProvider(CounterSample newSample, String colName, int c, int jdbcType, Object addValue)
	{
		if ("dbname".equalsIgnoreCase(colName))
			return "_Total";

		return null;
	}
}
