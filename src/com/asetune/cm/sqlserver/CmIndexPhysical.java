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
import java.util.LinkedList;
import java.util.List;

import com.asetune.ICounterController;
import com.asetune.IGuiController;
import com.asetune.cache.DbmsObjectIdCache;
import com.asetune.cache.DbmsObjectIdCacheSqlServer;
import com.asetune.cache.DbmsObjectIdCacheUtils;
import com.asetune.cm.CmSettingsHelper;
import com.asetune.cm.CounterSample;
import com.asetune.cm.CounterSampleCatalogIteratorSqlServer;
import com.asetune.cm.CounterSetTemplates;
import com.asetune.cm.CounterSetTemplates.Type;
import com.asetune.cm.CountersModel;
import com.asetune.cm.sqlserver.gui.CmIndexPhysicalPanel;
import com.asetune.gui.MainFrame;
import com.asetune.gui.TabularCntrPanel;
import com.asetune.utils.Configuration;

/**
 * @author Goran Schwarz (goran_schwarz@hotmail.com)
 */
public class CmIndexPhysical
extends CountersModel
{
//	private static Logger        _logger          = Logger.getLogger(CmServiceMemory.class);
	private static final long    serialVersionUID = 1L;

	public static final String   CM_NAME          = CmIndexPhysical.class.getSimpleName();
	public static final String   SHORT_NAME       = "Index Physical";
	public static final String   HTML_DESC        = 
		"<html>" +
		"<p>FIXME</p>" +
		"</html>";

	public static final String   GROUP_NAME       = MainFrame.TCP_GROUP_OBJECT_ACCESS;
	public static final String   GUI_ICON_FILE    = "images/"+CM_NAME+".png";

	public static final long     NEED_SRV_VERSION = 0;
	public static final long     NEED_CE_VERSION  = 0;

	public static final String[] MON_TABLES       = new String[] {"dm_db_index_physical_stats"};
	public static final String[] NEED_ROLES       = new String[] {"VIEW SERVER STATE", "CONNECT ANY DATABASE"};
	public static final String[] NEED_CONFIG      = new String[] {};

	public static final String[] PCT_COLUMNS      = new String[] {};
	public static final String[] DIFF_COLUMNS     = new String[] {
			 "pageCountDiff"
			,"rowCountDiff"
	};

	// Microsoft SQL Server 2019 (RTM-CU3) (KB4538853) - 15.0.4023.6 (X64)
	// 
	// RS> Col# Label                                     JDBC Type Name          Guessed DBMS type Source Table
	// RS> ---- ----------------------------------------- ----------------------- ----------------- ------------
	// RS> 1    database_id                               java.sql.Types.SMALLINT smallint          -none-      
	// RS> 2    object_id                                 java.sql.Types.INTEGER  int               -none-      
	// RS> 3    index_id                                  java.sql.Types.INTEGER  int               -none-      
	// RS> 4    partition_number                          java.sql.Types.INTEGER  int               -none-      
	// RS> 5    index_type_desc                           java.sql.Types.NVARCHAR nvarchar(60)      -none-      
	// RS> 6    alloc_unit_type_desc                      java.sql.Types.NVARCHAR nvarchar(60)      -none-      
	// RS> 7    index_depth                               java.sql.Types.TINYINT  tinyint           -none-      
	// RS> 8    index_level                               java.sql.Types.TINYINT  tinyint           -none-      
	// RS> 9    avg_fragmentation_in_percent              java.sql.Types.DOUBLE   float             -none-      
	// RS> 10   fragment_count                            java.sql.Types.BIGINT   bigint            -none-      
	// RS> 11   avg_fragment_size_in_pages                java.sql.Types.DOUBLE   float             -none-      
	// RS> 12   page_count                                java.sql.Types.BIGINT   bigint            -none-      
	// RS> 13   avg_page_space_used_in_percent            java.sql.Types.DOUBLE   float             -none-      
	// RS> 14   record_count                              java.sql.Types.BIGINT   bigint            -none-      
	// RS> 15   ghost_record_count                        java.sql.Types.BIGINT   bigint            -none-      
	// RS> 16   version_ghost_record_count                java.sql.Types.BIGINT   bigint            -none-      
	// RS> 17   min_record_size_in_bytes                  java.sql.Types.INTEGER  int               -none-      
	// RS> 18   max_record_size_in_bytes                  java.sql.Types.INTEGER  int               -none-      
	// RS> 19   avg_record_size_in_bytes                  java.sql.Types.DOUBLE   float             -none-      
	// RS> 20   forwarded_record_count                    java.sql.Types.BIGINT   bigint            -none-      
	// RS> 21   compressed_page_count                     java.sql.Types.BIGINT   bigint            -none-      
	// RS> 22   hobt_id                                   java.sql.Types.BIGINT   bigint            -none-      
	// RS> 23   columnstore_delete_buffer_state           java.sql.Types.TINYINT  tinyint           -none-      
	// RS> 24   columnstore_delete_buffer_state_desc      java.sql.Types.NVARCHAR nvarchar(60)      -none-      
	// RS> 25   version_record_count                      java.sql.Types.BIGINT   bigint            -none-      
	// RS> 26   inrow_version_record_count                java.sql.Types.BIGINT   bigint            -none-      
	// RS> 27   inrow_diff_version_record_count           java.sql.Types.BIGINT   bigint            -none-      
	// RS> 28   total_inrow_version_payload_size_in_bytes java.sql.Types.BIGINT   bigint            -none-      
	// RS> 29   offrow_regular_version_record_count       java.sql.Types.BIGINT   bigint            -none-      
	// RS> 30   offrow_long_term_version_record_count     java.sql.Types.BIGINT   bigint            -none-      	

	public static final boolean  NEGATIVE_DIFF_COUNTERS_TO_ZERO = false;
	public static final boolean  IS_SYSTEM_CM                   = true;
	public static final int      DEFAULT_POSTPONE_TIME          = 3600 * 8; // 8 hour
//	public static final int      DEFAULT_QUERY_TIMEOUT          = CountersModel.DEFAULT_sqlQueryTimeout;
	public static final int      DEFAULT_QUERY_TIMEOUT          = 15 * 60; // 15 minutes

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

		return new CmIndexPhysical(counterController, guiController);
	}

	public CmIndexPhysical(ICounterController counterController, IGuiController guiController)
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
	private static final String  PROP_PREFIX                     = CM_NAME;

	public static final String  PROPKEY_sample_mode              = PROP_PREFIX + ".sample.mode";
	public static final String  DEFAULT_sample_mode              = "'SAMPLED'";
	
	public static final String  PROPKEY_sample_minPageCount      = PROP_PREFIX + ".sample.minPageCount";
	public static final int     DEFAULT_sample_minPageCount      = 1;
	
	private void addTrendGraphs()
	{
	}

	@Override
	protected TabularCntrPanel createGui()
	{
		return new CmIndexPhysicalPanel(this);
	}

	@Override
	public String[] getDependsOnConfigForVersion(Connection conn, long srvVersion, boolean isAzure)
	{
		return NEED_CONFIG;
	}

	/** Used by the: Create 'Offline Session' Wizard */
	@Override
	public List<CmSettingsHelper> getLocalSettings()
	{
		Configuration conf = Configuration.getCombinedConfiguration();
		List<CmSettingsHelper> list = new ArrayList<>();
		
		list.add(new CmSettingsHelper("Detail Level", PROPKEY_sample_mode         , String .class, conf.getProperty(   PROPKEY_sample_mode         , DEFAULT_sample_mode         ), DEFAULT_sample_mode        , "Parameter 5 to the function 'dm_db_index_physical_stats' (valid values are DEFAULT, 'LIMITED', 'SAMPLED' or 'DETAILED')." ));
		list.add(new CmSettingsHelper("Table Size",   PROPKEY_sample_minPageCount , Integer.class, conf.getIntProperty(PROPKEY_sample_minPageCount , DEFAULT_sample_minPageCount ), DEFAULT_sample_minPageCount, "How many data pages should the table have to be included." ));

		return list;
	}


	@Override
	public List<String> getPkForVersion(Connection conn, long srvVersion, boolean isAzure)
	{
		List <String> pkCols = new LinkedList<String>();

		pkCols.add("database_id");
		pkCols.add("object_id");
		pkCols.add("index_id");
		pkCols.add("partition_number");
		pkCols.add("alloc_unit_type_desc");

		return pkCols;
	}

	/**
	 * Create a special CounterSample, that will iterate over all databases that we will interrogate
	 */
	@Override
	public CounterSample createCounterSample(String name, boolean negativeDiffCountersToZero, String[] diffColumns, CounterSample prevSample)
	{
		// Using DEFAULT_SKIP_DB_LIST: 'master', 'model', 'tempdb', 'msdb', 'SSISDB', 'ReportServer', 'ReportServerTempDB'
		return new CounterSampleCatalogIteratorSqlServer(name, negativeDiffCountersToZero, diffColumns, prevSample);
	}

	@Override
	public String getSqlForVersion(Connection conn, long srvVersion, boolean isAzure)
	{
		String dm_db_index_physical_stats = "dm_db_index_physical_stats";
		
//		if (isAzure)
//			dm_db_index_physical_stats = "dm_pdw_nodes_db_index_physical_stats";

		
//		String mode = "DEFAULT";    // LIMITED, SAMPLED, or DETAILED. The default (NULL) is LIMITED.
//		String mode = "'LIMITED'";  // LIMITED, SAMPLED, or DETAILED. The default (NULL) is LIMITED.
//		String mode = "'SAMPLED'";  // LIMITED, SAMPLED, or DETAILED. The default (NULL) is LIMITED.
//		String mode = "'DETAILED'"; // LIMITED, SAMPLED, or DETAILED. The default (NULL) is LIMITED.
		String mode = Configuration.getCombinedConfiguration().getProperty(PROPKEY_sample_mode, DEFAULT_sample_mode);
		
//		int minPageCount = 1;
		int minPageCount = Configuration.getCombinedConfiguration().getIntProperty(PROPKEY_sample_minPageCount, DEFAULT_sample_minPageCount);
		
//		String sql = "exec sys.sp_MSforeachdb '"
//				+ "select \n"
//				+ "     [dbname]         = db_name(database_id) \n"
//				+ "    ,[schemaName]     = object_schema_name(object_id, database_id) \n"
//				+ "    ,[objectName]     = object_name(object_id, database_id) \n"
//				+ "    ,[SizeInMb]       = convert(decimal(12,1), page_count / 128.0) \n"
//				+ "    ,[pageCountDiff]  = page_count \n"
//				+ "    ,[rowCount]       = record_count \n"
//				+ "    ,[rowCountDiff]   = record_count \n"
//				+ "    ,[avgRowsPerPage] = CASE WHEN page_count = 0 THEN 0 ELSE convert(decimal(10,1), (record_count*1.0 / page_count)) END \n"
//				+ "    ,* \n"
//				+ "from sys." + dm_db_index_physical_stats + "(db_id(''?''), DEFAULT, DEFAULT, DEFAULT, " + mode.replace("'", "''") + ") \n"
//				+ "where page_count >= " + minPageCount + " \n"
//				+ "' \n"
//				+ "";

		String DbName     = "      [DbName]     = db_name(database_id) \n";
		String SchemaName = "    , [SchemaName] = (select sys.schemas.name from sys.objects WITH (READUNCOMMITTED) inner join sys.schemas WITH (READUNCOMMITTED) ON sys.schemas.schema_id = sys.objects.schema_id where sys.objects.object_id = BASE.object_id) \n";
		String TableName  = "    , [TableName]  = (select sys.objects.name from sys.objects WITH (READUNCOMMITTED) where sys.objects.object_id = BASE.object_id) \n";
//		String IndexName  = "    , [IndexName]  = (select case when sys.indexes.index_id = 0 then 'HEAP' else sys.indexes.name end from sys.indexes where sys.indexes.object_id = BASE.object_id and sys.indexes.index_id = BASE.index_id) \n";

		if (DbmsObjectIdCache.hasInstance() && DbmsObjectIdCache.getInstance().isBulkLoadOnStartEnabled())
		{
			DbName     = "      [DbName]     = convert(varchar(128), '') /* using DbmsObjectIdCache to do DbxTune cached lookups */ \n";
			SchemaName = "    , [SchemaName] = convert(varchar(128), '') /* using DbmsObjectIdCache to do DbxTune cached lookups */ \n";
			TableName  = "    , [TableName]  = convert(varchar(128), '') /* using DbmsObjectIdCache to do DbxTune cached lookups */ \n";
//			IndexName  = "    , [IndexName]  = convert(varchar(128), '') /* using DbmsObjectIdCache to do DbxTune cached lookups */ \n";
		}

		String sql = ""
			    + "-- Note: Below SQL Statement is executed in every database that is 'online', more or less like: sp_msforeachdb \n"
			    + "-- Note: object_schema_name() and object_name() can NOT be used for 'dirty-reads', they may block... hence the 'ugly' fullname sub-selects in the select column list \n"
			    + "-- Note: To enable/disable DbxTune Cached Lookups for ObjectID to name translation is done with property '" + DbmsObjectIdCacheSqlServer.PROPKEY_BulkLoadOnStart + "=true|false'. Current Status=" + (DbmsObjectIdCache.hasInstance() && DbmsObjectIdCache.getInstance().isBulkLoadOnStartEnabled() ? "ENABLED" : "DISABLED") + " \n"
			    + "select /* ${cmCollectorName} */ \n"
//			    + "      [DbName]         = db_name(database_id) \n"
//			    + "    , [SchemaName]     = (select sys.schemas.name from sys.objects inner join sys.schemas ON sys.schemas.schema_id = sys.objects.schema_id where sys.objects.object_id = BASE.object_id) \n"
//			    + "    , [TableName]      = (select sys.objects.name from sys.objects where sys.objects.object_id = BASE.object_id) \n"
				+        DbName
				+        SchemaName
				+        TableName
				+ "    , [SizeInMb]       = convert(decimal(12,1), page_count / 128.0) \n"
				+ "    , [pageCountDiff]  = page_count \n"
				+ "    , [rowCount]       = record_count \n"
				+ "    , [rowCountDiff]   = record_count \n"
				+ "    , [avgRowsPerPage] = CASE WHEN page_count = 0 THEN 0 ELSE convert(decimal(10,1), (record_count*1.0 / page_count)) END \n"
				+ "    ,* \n"
				+ "from sys." + dm_db_index_physical_stats + "(db_id(), DEFAULT, DEFAULT, DEFAULT, " + mode + ") BASE \n"
				+ "where page_count >= " + minPageCount + " \n"
				+ "";

		// NOTE: object_name() function is not the best in SQL-Server it may block...

		return sql;
	}

	@Override
	public void localCalculation(CounterSample newSample)
	{
		// Resolve "database_id", "object_id", "index_id" to real names 
		if (DbmsObjectIdCache.hasInstance() && DbmsObjectIdCache.getInstance().isBulkLoadOnStartEnabled())
		{
			DbmsObjectIdCacheUtils.localCalculation_DbmsObjectIdFiller(this, newSample, 
					"database_id", "object_id", null,            // Source columns to translate into the below columns 
					"DbName", "SchemaName", "TableName", null);  // Target columns for the above source columns
		}
	}


	
	
//	/** 
//	 * Get number of rows to save/request ddl information for 
//	 * if 0 is return no lookup will be done.
//	 */
//	@Override
//	public int getMaxNumOfDdlsToPersist()
//	{
//		return 10;
//	}
//
//	/** 
//	 * Get Column names to where DBName and ObjectName is called, this must always return at least a array with 2 strings. 
//	 */
//	@Override
//	public String[] getDdlDetailsColNames()
//	{
//		String[] sa = {"dbname", "schemaName", "objectName"};
//		return sa;
//	}
//	/**
//	 * Sort descending on this column(s) to get values from the diff/rate structure<br>
//	 * One sort for every value will be done, meaning we can do "top" for more than 1 column<br>
//	 * So if we want to do top 10 LogicalReads AND top 10 LockContention
//	 * If this one returns null, this will not be done
//	 * @return
//	 */
//	@Override
//	public String[] getDdlDetailsSortOnColName()
//	{
//		String[] sa = {"user_seeks", "user_scans", "user_lookups", "user_updates"};
//		return sa;
//	}
}
