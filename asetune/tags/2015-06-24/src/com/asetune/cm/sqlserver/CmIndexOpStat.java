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
public class CmIndexOpStat
extends CountersModel
{
//	private static Logger        _logger          = Logger.getLogger(CmServiceMemory.class);
	private static final long    serialVersionUID = 1L;

	public static final String   CM_NAME          = CmIndexOpStat.class.getSimpleName();
	public static final String   SHORT_NAME       = "Index Operational";
	public static final String   HTML_DESC        = 
		"<html>" +
		"<p>FIXME</p>" +
		"</html>";

	public static final String   GROUP_NAME       = MainFrame.TCP_GROUP_OBJECT_ACCESS;
	public static final String   GUI_ICON_FILE    = "images/"+CM_NAME+".png";

	public static final int      NEED_SRV_VERSION = 0;
	public static final int      NEED_CE_VERSION  = 0;

	public static final String[] MON_TABLES       = new String[] {"dm_db_index_operational_stats"};
	public static final String[] NEED_ROLES       = new String[] {};//{"VIEW SERVER STATE"};
	public static final String[] NEED_CONFIG      = new String[] {};

	public static final String[] PCT_COLUMNS      = new String[] {};
	public static final String[] DIFF_COLUMNS     = new String[] {
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

		return new CmIndexOpStat(counterController, guiController);
	}

	public CmIndexOpStat(ICounterController counterController, IGuiController guiController)
	{
		super(CM_NAME, GROUP_NAME, /*sql*/null, /*pkList*/null, 
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
	public String[] getDependsOnConfigForVersion(Connection conn, int srvVersion, boolean isClusterEnabled)
	{
		return NEED_CONFIG;
	}

	@Override
	public List<String> getPkForVersion(Connection conn, int srvVersion, boolean isClusterEnabled)
	{
		List <String> pkCols = new LinkedList<String>();

		pkCols.add("database_id");
		pkCols.add("object_id");
		pkCols.add("index_id");
		pkCols.add("partition_number");

		return pkCols;
	}

	@Override
	public String getSqlForVersion(Connection conn, int aseVersion, boolean isClusterEnabled)
	{
		String sql = "select \n"
				+ "    dbname=db_name(database_id), \n"
				+ "    objectName=object_name(object_id, database_id), \n"
				+ "    *\n"
				+ "from sys.dm_db_index_operational_stats(DEFAULT, DEFAULT, DEFAULT, DEFAULT) \n"
				+ "where object_id > 100";

		return sql;
	}
}