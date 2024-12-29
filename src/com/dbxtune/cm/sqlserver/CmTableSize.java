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
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
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

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

import javax.naming.NameNotFoundException;

import com.dbxtune.ICounterController;
import com.dbxtune.IGuiController;
import com.dbxtune.cache.DbmsObjectIdCache;
import com.dbxtune.cache.DbmsObjectIdCacheSqlServer;
import com.dbxtune.cache.DbmsObjectIdCacheUtils;
import com.dbxtune.cm.CounterSample;
import com.dbxtune.cm.CounterSampleCatalogIteratorSqlServer;
import com.dbxtune.cm.CounterSetTemplates;
import com.dbxtune.cm.CountersModel;
import com.dbxtune.cm.SortOptions;
import com.dbxtune.cm.CounterSetTemplates.Type;
import com.dbxtune.cm.SortOptions.ColumnNameSensitivity;
import com.dbxtune.cm.SortOptions.DataSortSensitivity;
import com.dbxtune.cm.SortOptions.SortOrder;
import com.dbxtune.config.dict.MonTablesDictionary;
import com.dbxtune.config.dict.MonTablesDictionaryManager;
import com.dbxtune.gui.MainFrame;
import com.dbxtune.sql.conn.DbxConnection;
import com.dbxtune.sql.conn.info.DbmsVersionInfo;
import com.dbxtune.sql.conn.info.DbmsVersionInfoSqlServer;

public class CmTableSize
extends CountersModel
{
//	private static Logger        _logger          = Logger.getLogger(CmTableSize.class);
	private static final long    serialVersionUID = 1L;

	public static final String   CM_NAME          = CmTableSize.class.getSimpleName();
	public static final String   SHORT_NAME       = "Table Size";
	public static final String   HTML_DESC        = 
		"<html>" +
		"<p>Size of tables in all databases (using statistics, so data might not be 100 percent accurate</p>" +
		"</html>";

	public static final String   GROUP_NAME       = MainFrame.TCP_GROUP_OBJECT_ACCESS;
	public static final String   GUI_ICON_FILE    = "images/"+CM_NAME+".png";

	public static final long     NEED_SRV_VERSION = 0;
	public static final long     NEED_CE_VERSION  = 0;

	public static final String[] MON_TABLES       = new String[] {CM_NAME, "dm_db_partition_stats"};
	public static final String[] NEED_ROLES       = new String[] {"VIEW SERVER STATE", "CONNECT ANY DATABASE"};
	public static final String[] NEED_CONFIG      = new String[] {};

	public static final String[] PCT_COLUMNS      = new String[] {
			 "DataVsIndexPct"
			,"TotalUnUsedPct"
			,"DataUnUsedPct"
			,"LobUnUsedPct"
			,"LobTableContentPct"
			,"IndexUnUsedPct"
	};

	public static final String[] DIFF_COLUMNS     = new String[] {
			 "RowCountDiff"
			
			,"d_reserved_page_count"
			,"d_used_page_count"
			,"d_unused_page_count"
			,"d_in_row_data_page_count"
			,"d_in_row_used_page_count"
			,"d_in_row_reserved_page_count"
			,"d_lob_used_page_count"
			,"d_lob_reserved_page_count"
			,"d_row_overflow_used_page_count"
			,"d_row_overflow_reserved_page_count"
			
			,"i_reserved_page_count"
			,"i_used_page_count"
			,"i_unused_page_count"
			,"i_in_row_data_page_count"
			,"i_in_row_used_page_count"
			,"i_in_row_reserved_page_count"
			,"i_lob_used_page_count"
			,"i_lob_reserved_page_count"
			,"i_row_overflow_used_page_count"
			,"i_row_overflow_reserved_page_count"
		};


	public static final boolean  NEGATIVE_DIFF_COUNTERS_TO_ZERO = false;
	public static final boolean  IS_SYSTEM_CM                   = true;
	public static final int      DEFAULT_POSTPONE_TIME          = 3600; // 1 hour
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

		return new CmTableSize(counterController, guiController);
	}

	public CmTableSize(ICounterController counterController, IGuiController guiController)
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
//	private static final String  PROP_PREFIX                     = CM_NAME;

	private void addTrendGraphs()
	{
	}

//	@Override
//	protected TabularCntrPanel createGui()
//	{
//		return new CmTableSizePanel(this);
//	}

	@Override
	public void addMonTableDictForVersion(DbxConnection conn, DbmsVersionInfo versionInfo)
	{
		try 
		{
			MonTablesDictionary mtd = MonTablesDictionaryManager.getInstance();

			String cmName = this.getName();
			mtd.addTable(cmName, HTML_DESC);

			mtd.addColumn(cmName, "DBName"                             ,"<html>Name of the database</html>");
			mtd.addColumn(cmName, "SchemaName"                         ,"<html>Name of the Schema</html>");
			mtd.addColumn(cmName, "TableName"                          ,"<html>Name of the Table</html>");
			
			mtd.addColumn(cmName, "HasClusteredIndex"                  ,"<html>true if we gor a Clustered Index on this table</html>"); 
			mtd.addColumn(cmName, "NcIndexCount"                       ,"<html>How many NON Clustered Indexes do we have on this table... (A rule of thumb: maybe not more than 5 indexes)</html>"); 
			mtd.addColumn(cmName, "PartitionCount"                     ,"<html>How many partitions do this table have. 1=UnPartitioned</html>"); 
			mtd.addColumn(cmName, "RowCountAbs"                        ,"<html>How many rows does this table have</html>"); 
			mtd.addColumn(cmName, "RowCountDiff"                       ,"<html>Diff or Rate value for row count. <br>"
			                                                                  + "If there has been inserts since last sample, this will be a <b>positive</b> number (DIFF=since last sample, RATE=number of insert per second since last sample).<br>"
			                                                                  + "If there has been inserts since last sample, this will be a <b>negative</b> number (DIFF=since last sample, RATE=number of deletes per second since last sample).<br>"
			                                                                  + "</html>"); 
			mtd.addColumn(cmName, "DataRowsPerPage"                    ,"<html>How many rows in average does a page hold. <br><br><b>Algorithm:</b> row_count / used_page_count </html>");
			mtd.addColumn(cmName, "DataVsIndexPct"                     ,"<html>How much index space is used compared to data space (10% = Index size is 10% of data size, 200% = Index size is double the size of data.<br><br><b>Algorithm:</b> ncistat.UsedSizeMB / dstat.UsedSizeMB) * 100.0</html>");
			
			mtd.addColumn(cmName, "TotalReservedSizeMB"                ,"<html>Total 'reseved' size of the table, both data and all indexes.</html>"); 
			mtd.addColumn(cmName, "TotalUsedSizeMB"                    ,"<html>Total 'used' size of the table, both data and all indexes.</html>");
			mtd.addColumn(cmName, "TotalUnUsedSizeMB"                  ,"<html>Total 'un-used' size of the table, both data and all indexes.</html>");
			mtd.addColumn(cmName, "TotalUnUsedPct"                     ,"<html>Total 'un-used' percent of the tables total size.</html>");
			
			mtd.addColumn(cmName, "HasLobData"                         ,"<html>If the table has any columns of LOB data. LOB Large OBjects, large columns that <i>may</i> not be part of the normal data page. So that data may be stored 'off-row' in it's own page chain.</html>"); 

			mtd.addColumn(cmName, "DataReservedSizeMB"                 ,"<html>FIXME</html>"); 
			mtd.addColumn(cmName, "DataUsedSizeMB"                     ,"<html>FIXME</html>"); 
			mtd.addColumn(cmName, "DataUnUsedSizeMB"                   ,"<html>FIXME</html>"); 
			mtd.addColumn(cmName, "DataUnUsedPct"                      ,"<html>FIXME</html>"); 

			mtd.addColumn(cmName, "IndexReservedSizeMB"                ,"<html>FIXME</html>"); 
			mtd.addColumn(cmName, "IndexUsedSizeMB"                    ,"<html>FIXME</html>"); 
			mtd.addColumn(cmName, "IndexUnUsedSizeMB"                  ,"<html>FIXME</html>"); 
			mtd.addColumn(cmName, "IndexUnUsedPct"                     ,"<html>FIXME</html>"); 

			mtd.addColumn(cmName, "LobReservedSizeMB"                  ,"<html>FIXME</html>"); 
			mtd.addColumn(cmName, "LobUsedSizeMB"                      ,"<html>FIXME</html>"); 
			mtd.addColumn(cmName, "LobUnUsedSizeMB"                    ,"<html>FIXME</html>"); 
			mtd.addColumn(cmName, "LobUnUsedPct"                       ,"<html>FIXME</html>"); 
			mtd.addColumn(cmName, "LobTableContentPct"                 ,"<html>FIXME</html>"); 
			
			mtd.addColumn(cmName, "DataStatsUpdated"                   ,"<html>Last date that the statistics was updated.</html>"); 
			mtd.addColumn(cmName, "NcIndexStatsUpdated"                ,"<html>Last date that the statistics was updated on any of the indexes.</html>"); 
			
			mtd.addColumn(cmName, "d_in_row_data_page_count"           ,"<html>Data 'in_row_data_page_count'.</html>"); 
			mtd.addColumn(cmName, "d_in_row_used_page_count"           ,"<html>Data 'in_row_used_page_count'.</html>"); 
			mtd.addColumn(cmName, "d_in_row_reserved_page_count"       ,"<html>Data 'in_row_reserved_page_count'.</html>"); 
			mtd.addColumn(cmName, "d_lob_used_page_count"              ,"<html>Data 'lob_used_page_count'.</html>"); 
			mtd.addColumn(cmName, "d_lob_reserved_page_count"          ,"<html>Data 'lob_reserved_page_count'.</html>"); 
			mtd.addColumn(cmName, "d_row_overflow_used_page_count"     ,"<html>Data 'row_overflow_used_page_count'.</html>"); 
			mtd.addColumn(cmName, "d_row_overflow_reserved_page_count" ,"<html>Data 'row_overflow_reserved_page_count'.</html>");

			mtd.addColumn(cmName, "i_in_row_data_page_count"           ,"<html>Index 'in_row_data_page_count'.</html>"); 
			mtd.addColumn(cmName, "i_in_row_used_page_count"           ,"<html>Index 'in_row_used_page_count'.</html>"); 
			mtd.addColumn(cmName, "i_in_row_reserved_page_count"       ,"<html>Index 'in_row_reserved_page_count'.</html>"); 
			mtd.addColumn(cmName, "i_lob_used_page_count"              ,"<html>Index 'lob_used_page_count'.</html>"); 
			mtd.addColumn(cmName, "i_lob_reserved_page_count"          ,"<html>Index 'lob_reserved_page_count'.</html>"); 
			mtd.addColumn(cmName, "i_row_overflow_used_page_count"     ,"<html>Index 'row_overflow_used_page_count'.</html>"); 
			mtd.addColumn(cmName, "i_row_overflow_reserved_page_count" ,"<html>Index 'row_overflow_reserved_page_count'.</html>"); 

			mtd.addColumn(cmName, "database_id"                        ,"<html>ID of the Database.</html>"); 
			mtd.addColumn(cmName, "object_id"                          ,"<html>ObjectID of the table.</html>"); 
		}
		catch (NameNotFoundException e) 
		{
		//	_logger.warn("Problems in cm='" + CM_NAME + "', adding addMonTableDictForVersion. Caught: " + e); 
			System.out.println("Problems in cm='" + CM_NAME + "', adding addMonTableDictForVersion. Caught: " + e); 
		}
	}

	@Override
	public String[] getDependsOnConfigForVersion(DbxConnection conn, DbmsVersionInfo versionInfo)
	{
		return NEED_CONFIG;
	}

	@Override
	public List<String> getPkForVersion(DbxConnection conn, DbmsVersionInfo versionInfo)
	{
		List <String> pkCols = new LinkedList<String>();

//		pkCols.add("DbName");
//		pkCols.add("SchemaName");
//		pkCols.add("TableName");

		pkCols.add("database_id");
		pkCols.add("object_id");
		
		return pkCols;
	}

	private List<SortOptions> _localSortOptions = null;

	@Override
	public List<SortOptions> getLocalSortOptions()
	{
		// Allocate the sorting specification only first time.
		if (_localSortOptions == null)
		{
			_localSortOptions = new ArrayList<>();
			
			_localSortOptions.add(new SortOptions("TotalReservedSizeMB", ColumnNameSensitivity.IN_SENSITIVE, SortOrder.DESCENDING, DataSortSensitivity.IN_SENSITIVE));
		}
		return _localSortOptions;
	}
	
	/**
	 * Create a special CounterSample, that will iterate over all databases that we will interrogate
	 */
	@Override
	public CounterSample createCounterSample(String name, boolean negativeDiffCountersToZero, String[] diffColumns, CounterSample prevSample)
	{
		// Using DEFAULT_SKIP_DB_LIST: 'master', 'model', 'msdb', 'SSISDB', 'ReportServer', 'ReportServerTempDB'
		return new CounterSampleCatalogIteratorSqlServer(name, negativeDiffCountersToZero, diffColumns, prevSample);
	}

	@Override
	public String getSqlForVersion(DbxConnection conn, DbmsVersionInfo versionInfo)
	{
		DbmsVersionInfoSqlServer ssVersionInfo = (DbmsVersionInfoSqlServer) versionInfo;

		String dm_db_partition_stats   = "sys.dm_db_partition_stats";
		
		if (ssVersionInfo.isAzureSynapseAnalytics())
		{
			dm_db_partition_stats   = "sys.dm_pdw_nodes_db_partition_stats";
		}

		String DbName     = "      DbName              = db_name() \n";
		String SchemaName = "    , SchemaName          = (select sys.schemas.name from sys.objects WITH (READUNCOMMITTED) inner join sys.schemas WITH (READUNCOMMITTED) ON sys.schemas.schema_id = sys.objects.schema_id where sys.objects.object_id = dstat.object_id) \n";
		String TableName  = "    , TableName           = (select sys.objects.name from sys.objects WITH (READUNCOMMITTED) where sys.objects.object_id = dstat.object_id) \n";

		if (DbmsObjectIdCache.hasInstance() && DbmsObjectIdCache.getInstance().isBulkLoadOnStartEnabled())
		{
			DbName     = "      DbName     = convert(varchar(128), '') /* using DbmsObjectIdCache to do DbxTune cached lookups */ \n";
			SchemaName = "    , SchemaName = convert(varchar(128), '') /* using DbmsObjectIdCache to do DbxTune cached lookups */ \n";
			TableName  = "    , TableName  = convert(varchar(128), '') /* using DbmsObjectIdCache to do DbxTune cached lookups */ \n";
		}
		
		String sql = ""
			    + "-- Note: Below SQL Statement is executed in every database that is 'online', more or less like: sp_msforeachdb \n"
			    + "-- Note: object_schema_name() and object_name() can be used for 'dirty-reads', they may block... hence the 'ugly' fullname sub-selects in the select column list \n"
			    + "-- Note: To enable/disable DbxTune Cached Lookups for ObjectID to name translation is done with property '" + DbmsObjectIdCacheSqlServer.PROPKEY_BulkLoadOnStart + "=true|false'. Current Status=" + (DbmsObjectIdCache.hasInstance() && DbmsObjectIdCache.getInstance().isBulkLoadOnStartEnabled() ? "ENABLED" : "DISABLED") + " \n"
			    + " \n"
			    + "-- Drop temp tables (if they already exists \n"
			    + "if (object_id('tempdb..#dstat')   is not null) drop table #dstat \n"
			    + "if (object_id('tempdb..#ncistat') is not null) drop table #ncistat \n"
			    + " \n"
			    + "-- temptable: #dstat -- DATA STAT \n"
			    + "    select /* ${cmCollectorName} */ \n"
			    + "         object_id \n"
			    + "        ,StatsUpdated                     = nullif( max(isnull(stats_date(object_id, index_id), '2000-01-01')), '2000-01-01') -- this to get rid of: Warning: Null value is eliminated by an aggregate or other SET operation. \n"
			    + "        ,HasClusteredIndex                = convert(bit, case when sum(index_id) = 1 then 1 else 0 end) \n"
			    + "        ,PartitionCount                   = count(partition_number) \n"
			    + "        ,row_count                        = sum(row_count) \n"
			    + "        ,RowsPerPage                      = convert(decimal(12,1), sum(row_count)*1.0 / case when sum(used_page_count) = 0 THEN 1 else sum(used_page_count) end) \n"
			    + " \n"
			    + "        ,ReservedSizeMB                   = convert(decimal(12,1), sum(reserved_page_count) / 128.0) \n"
			    + "        ,UsedSizeMB                       = convert(decimal(12,1), sum(used_page_count)     / 128.0) \n"
			    + "        ,UnUsedSizeMB                     = convert(decimal(12,1), (sum(reserved_page_count) - sum(used_page_count)) / 128.0) \n"
			    + " \n"
			    + "        ,LobReservedSizeMB                = convert(decimal(12,1), sum(lob_reserved_page_count) / 128.0) \n"
			    + "        ,LobUsedSizeMB                    = convert(decimal(12,1), sum(lob_used_page_count)     / 128.0) \n"
			    + "        ,LobUnUsedSizeMB                  = convert(decimal(12,1), (sum(lob_reserved_page_count) - sum(lob_used_page_count)) / 128.0) \n"
			    + " \n"
			    + "        ,reserved_page_count              = sum(reserved_page_count) \n"
			    + "        ,used_page_count                  = sum(used_page_count) \n"
			    + "        ,unused_page_count                = sum(reserved_page_count) - sum(used_page_count) \n"
			    + " \n"
			    + "        ,in_row_data_page_count           = sum(in_row_data_page_count) \n"
			    + "        ,in_row_used_page_count           = sum(in_row_used_page_count) \n"
			    + "        ,in_row_reserved_page_count       = sum(in_row_reserved_page_count) \n"
			    + "        ,lob_used_page_count              = sum(lob_used_page_count) \n"
			    + "        ,lob_reserved_page_count          = sum(lob_reserved_page_count) \n"
			    + "        ,row_overflow_used_page_count     = sum(row_overflow_used_page_count) \n"
			    + "        ,row_overflow_reserved_page_count = sum(row_overflow_reserved_page_count) \n"
			    + "    into #dstat \n"
			    + "    from sys.dm_db_partition_stats \n"
			    + "    where index_id in (0,1) \n"
			    + "      and OBJECTPROPERTY(object_id, 'IsUserTable') = 1 \n"
			    + "      and row_count > 0 \n"
			    + "    group by object_id \n"
			    + " \n"
			    + "-- temptable: #ncistat -- NONCLUSTERED INDEX STAT \n"
			    + "    select /* ${cmCollectorName} */ \n"
			    + "         object_id \n"
			    + "        ,StatsUpdated                     = nullif( max(isnull(stats_date(object_id, index_id), '2000-01-01')), '2000-01-01') -- this to get rid of: Warning: Null value is eliminated by an aggregate or other SET operation. \n"
			    + "        ,NcIndexCount                     = count(*) \n"
			    + "        ,ReservedSizeMB                   = convert(decimal(12,1), sum(reserved_page_count) / 128.0) \n"
			    + "        ,UsedSizeMB                       = convert(decimal(12,1), sum(used_page_count)     / 128.0) \n"
			    + "        ,UnUsedSizeMB                     = convert(decimal(12,1), (sum(reserved_page_count) - sum(used_page_count)) / 128.0) \n"
			    + " \n"
			    + "        ,reserved_page_count              = sum(reserved_page_count) \n"
			    + "        ,used_page_count                  = sum(used_page_count) \n"
			    + "        ,unused_page_count                = sum(reserved_page_count) - sum(used_page_count) \n"
			    + " \n"
			    + "        ,in_row_data_page_count           = sum(in_row_data_page_count) \n"
			    + "        ,in_row_used_page_count           = sum(in_row_used_page_count) \n"
			    + "        ,in_row_reserved_page_count       = sum(in_row_reserved_page_count) \n"
			    + "        ,lob_used_page_count              = sum(lob_used_page_count) \n"
			    + "        ,lob_reserved_page_count          = sum(lob_reserved_page_count) \n"
			    + "        ,row_overflow_used_page_count     = sum(row_overflow_used_page_count) \n"
			    + "        ,row_overflow_reserved_page_count = sum(row_overflow_reserved_page_count) \n"
			    + "    into #ncistat \n"
			    + "    from sys.dm_db_partition_stats \n"
			    + "    where index_id >= 2 \n"
			    + "      and OBJECTPROPERTY(object_id, 'IsUserTable') = 1 \n"
			    + "      and row_count > 0 \n"
			    + "    group by object_id \n"
			    + " \n"
			    + "-- Joint the two temp tables \n"
			    + "select /* ${cmCollectorName} */ \n"
//			    + "--      DbName              = db_name() \n"
//			    + "--    , SchemaName          = object_schema_name(dstat.object_id) \n"
//			    + "--    , TableName           = object_name(dstat.object_id) \n"
//			    + "      DbName              = db_name() \n"
//			    + "    , SchemaName          = (select sys.schemas.name from sys.objects inner join sys.schemas ON sys.schemas.schema_id = sys.objects.schema_id where sys.objects.object_id = dstat.object_id) \n"
//			    + "    , TableName           = (select sys.objects.name from sys.objects where sys.objects.object_id = dstat.object_id) \n"
				+        DbName
				+        SchemaName
				+        TableName
			    + "    , dstat.HasClusteredIndex \n"
			    + "    , NcIndexCount        = isnull(ncistat.NcIndexCount, 0) \n"
			    + "    , dstat.PartitionCount \n"
			    + "    , RowCountAbs         = dstat.row_count \n"
			    + "    , RowCountDiff        = dstat.row_count \n"
			    + "    , DataRowsPerPage     = dstat.RowsPerPage \n"
			    + "    , DataVsIndexPct      = convert(decimal(12,1), case when dstat.UsedSizeMB = 0 then null else (ncistat.UsedSizeMB / dstat.UsedSizeMB) * 100.0 end) \n"
			    + " \n"
			    + "    , TotalReservedSizeMB = dstat.ReservedSizeMB + isnull(ncistat.ReservedSizeMB, 0.0) \n"
			    + "    , TotalUsedSizeMB     = dstat.UsedSizeMB     + isnull(ncistat.UsedSizeMB    , 0.0) \n"
			    + "    , TotalUnUsedSizeMB   = dstat.UnUsedSizeMB   + isnull(ncistat.UnUsedSizeMB  , 0.0) \n"
			    + "    -- pct = TotalUnUsedSizeMB/TotalReservedSizeMB \n"
			    + "    , TotalUnUsedPct      = convert(decimal(12,1),  case when dstat.ReservedSizeMB + isnull(ncistat.ReservedSizeMB, 0.0) = 0 then null else (dstat.UnUsedSizeMB + isnull(ncistat.UnUsedSizeMB  , 0.0))/(dstat.ReservedSizeMB + isnull(ncistat.ReservedSizeMB, 0.0)) * 100.0 end) \n"
			    + " \n"
			    + "    , HasLobData          = convert(bit, case when dstat.lob_reserved_page_count > 0 then 1 else 0 end) \n"
			    + " \n"
			    + "    , DataReservedSizeMB  = dstat.ReservedSizeMB \n"
			    + "    , DataUsedSizeMB      = dstat.UsedSizeMB \n"
			    + "    , DataUnUsedSizeMB    = dstat.UnUsedSizeMB \n"
			    + "    , DataUnUsedPct       = convert(decimal(12,1),  case when dstat.ReservedSizeMB = 0 then null else (dstat.UnUsedSizeMB/dstat.ReservedSizeMB) * 100.0 end) \n"
			    + " \n"
			    + "    , IndexReservedSizeMB = ncistat.ReservedSizeMB \n"
			    + "    , IndexUsedSizeMB     = ncistat.UsedSizeMB \n"
			    + "    , IndexUnUsedSizeMB   = ncistat.UnUsedSizeMB \n"
			    + "    , IndexUnUsedPct      = convert(decimal(12,1),  case when ncistat.ReservedSizeMB = 0 then null else (ncistat.UnUsedSizeMB/ncistat.ReservedSizeMB) * 100.0 end) \n"
			    + " \n"
			    + "    , LobReservedSizeMB   = dstat.LobReservedSizeMB \n"
			    + "    , LobUsedSizeMB       = dstat.LobUsedSizeMB \n"
			    + "    , LobUnUsedSizeMB     = dstat.LobUnUsedSizeMB \n"
			    + "    , LobUnUsedPct        = convert(decimal(12,1),  case when dstat.LobReservedSizeMB = 0 then null else (dstat.LobUnUsedSizeMB/dstat.LobReservedSizeMB) * 100.0 end) \n"
			    + "    , LobTableContentPct  = convert(decimal(12,1),  case when dstat.LobReservedSizeMB = 0 then null when dstat.ReservedSizeMB = 0 then 0 else (dstat.LobReservedSizeMB / dstat.ReservedSizeMB) * 100.0 end) \n"
			    + " \n"
			    + "    , DataStatsUpdated    = dstat.StatsUpdated \n"
			    + "    , NcIndexStatsUpdated = ncistat.StatsUpdated \n"
			    + " \n"
			    + "    , d_in_row_data_page_count           = dstat.in_row_data_page_count \n"
			    + "    , d_in_row_used_page_count           = dstat.in_row_used_page_count \n"
			    + "    , d_in_row_reserved_page_count       = dstat.in_row_reserved_page_count \n"
			    + "    , d_lob_used_page_count              = dstat.lob_used_page_count \n"
			    + "    , d_lob_reserved_page_count          = dstat.lob_reserved_page_count \n"
			    + "    , d_row_overflow_used_page_count     = dstat.row_overflow_used_page_count \n"
			    + "    , d_row_overflow_reserved_page_count = dstat.row_overflow_reserved_page_count \n"
			    + " \n"
			    + "    , i_in_row_data_page_count           = ncistat.in_row_data_page_count \n"
			    + "    , i_in_row_used_page_count           = ncistat.in_row_used_page_count \n"
			    + "    , i_in_row_reserved_page_count       = ncistat.in_row_reserved_page_count \n"
			    + "    , i_lob_used_page_count              = ncistat.lob_used_page_count \n"
			    + "    , i_lob_reserved_page_count          = ncistat.lob_reserved_page_count \n"
			    + "    , i_row_overflow_used_page_count     = ncistat.row_overflow_used_page_count \n"
			    + "    , i_row_overflow_reserved_page_count = ncistat.row_overflow_reserved_page_count \n"
			    + " \n"
			    + "    , database_id = db_id()  /* Since we are 'looping' all databases. like sp_msforeachdb */ \n"
			    + "    , dstat.object_id \n"
			    + " \n"
			    + "from #dstat dstat \n"
			    + "left outer join #ncistat ncistat on dstat.object_id = ncistat.object_id \n"
			    + " \n"
			    + "-- Drop temp tables \n"
			    + "drop table #dstat \n"
			    + "drop table #ncistat \n"
			    + "";


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
}
