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
import com.asetune.gui.swing.ColumnHeaderPropsEntry;

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

	public static final long     NEED_SRV_VERSION = 0;
	public static final long     NEED_CE_VERSION  = 0;

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

		pkCols.add("database_id");
		pkCols.add("object_id");
		pkCols.add("index_id");
		pkCols.add("partition_number");

		return pkCols;
	}

	@Override
	public String getSqlForVersion(Connection conn, long srvVersion, boolean isAzure)
	{
//		RS> 29   row_lock_count                     java.sql.Types.BIGINT   bigint           
//		RS> 30   row_lock_wait_count                java.sql.Types.BIGINT   bigint           
//		RS> 31   row_lock_wait_in_ms                java.sql.Types.BIGINT   bigint           
//		RS> 32   page_lock_count                    java.sql.Types.BIGINT   bigint           
//		RS> 33   page_lock_wait_count               java.sql.Types.BIGINT   bigint           
//		RS> 34   page_lock_wait_in_ms               java.sql.Types.BIGINT   bigint           

		int c = 0;
		addPreferredColumnOrder(new ColumnHeaderPropsEntry("dbname",               c++));
		addPreferredColumnOrder(new ColumnHeaderPropsEntry("objectName",           c++));
		addPreferredColumnOrder(new ColumnHeaderPropsEntry("index_id",             c++));
		addPreferredColumnOrder(new ColumnHeaderPropsEntry("row_lock_count",       c++));
		addPreferredColumnOrder(new ColumnHeaderPropsEntry("row_lock_wait_count",  c++));
		addPreferredColumnOrder(new ColumnHeaderPropsEntry("row_lock_wait_in_ms",  c++));
		addPreferredColumnOrder(new ColumnHeaderPropsEntry("page_lock_count",      c++));
		addPreferredColumnOrder(new ColumnHeaderPropsEntry("page_lock_wait_count", c++));
		addPreferredColumnOrder(new ColumnHeaderPropsEntry("page_lock_wait_in_ms", c++));

		String dm_db_index_operational_stats = "dm_db_index_operational_stats";
		
		if (isAzure)
			dm_db_index_operational_stats = "dm_db_index_operational_stats";   // IS THIS THE SAME NAME IN AZURE ?????

		String sql = "select \n"
				+ "    dbname=db_name(database_id), \n"
				+ "    objectName=object_name(object_id, database_id), \n"
				+ "    *\n"
				+ "from sys." + dm_db_index_operational_stats + "(DEFAULT, DEFAULT, DEFAULT, DEFAULT) \n"
				+ "where object_id > 100";

		// NOTE: object_name() function is not the best in SQL-Server it may block...
		//       so the below might be helpfull
//		SELECT DB_NAME() AS DB_NAME, 
//			obj.name AS table_name,
//			ind.name AS index_name, 
//			ind.type_desc,
//			leaf_allocation_count + nonleaf_allocation_count AS splits,
//			range_scan_count, 
//			singleton_lookup_count,
//			leaf_insert_count + nonleaf_insert_count AS inserts,
//			leaf_update_count + nonleaf_update_count AS updates,
//			leaf_delete_count + nonleaf_delete_count AS deletes
//		FROM sys.dm_db_index_operational_stats(DB_ID(),null,null,null) as os
//		INNER JOIN sys.indexes as ind     ON ind.object_id = os.object_id AND ind.index_id = os.index_id
//		INNER JOIN sys.objects as obj     ON obj.object_id = os.object_id
//		WHERE obj.Type NOT LIKE 'S'
		
		return sql;
	}




	
	
	/** 
	 * Get number of rows to save/request ddl information for 
	 * if 0 is return no lookup will be done.
	 */
	@Override
	public int getMaxNumOfDdlsToPersist()
	{
		return 10;
	}

	/** 
	 * Get Column names to where DBName and ObjectName is called, this must always return at least a array with 2 strings. 
	 */
	@Override
	public String[] getDdlDetailsColNames()
	{
		String[] sa = {"dbname", "objectName"};
		return sa;
	}
	/**
	 * Sort descending on this column(s) to get values from the diff/rate structure<br>
	 * One sort for every value will be done, meaning we can do "top" for more than 1 column<br>
	 * So if we want to do top 10 LogicalReads AND top 10 LockContention
	 * If this one returns null, this will not be done
	 * @return
	 */
	@Override
	public String[] getDdlDetailsSortOnColName()
	{
		String[] sa = {"row_lock_count", "row_lock_wait_count", "page_lock_count", "page_lock_wait_count", "page_latch_wait_count"};
		return sa;
	}
}
