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

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

import org.apache.log4j.Logger;

import com.asetune.ICounterController;
import com.asetune.IGuiController;
import com.asetune.cache.DbmsObjectIdCache;
import com.asetune.cache.DbmsObjectIdCacheSqlServer;
import com.asetune.cache.DbmsObjectIdCacheUtils;
import com.asetune.cm.CounterSample;
import com.asetune.cm.CounterSampleCatalogIteratorSqlServer;
import com.asetune.cm.CounterSetTemplates;
import com.asetune.cm.CounterSetTemplates.Type;
import com.asetune.cm.CountersModel;
import com.asetune.cm.SortOptions;
import com.asetune.cm.SortOptions.ColumnNameSensitivity;
import com.asetune.cm.SortOptions.DataSortSensitivity;
import com.asetune.cm.SortOptions.SortOrder;
import com.asetune.gui.MainFrame;
import com.asetune.sql.conn.DbxConnection;
import com.asetune.sql.conn.info.DbmsVersionInfo;
import com.asetune.sql.conn.info.DbmsVersionInfoSqlServer;

/**
 * @author Goran Schwarz (goran_schwarz@hotmail.com)
 */
public class CmIndexUsage
extends CountersModel
{
	private static Logger        _logger          = Logger.getLogger(CmIndexUsage.class);
	private static final long    serialVersionUID = 1L;

	public static final String   CM_NAME          = CmIndexUsage.class.getSimpleName();
	public static final String   SHORT_NAME       = "Index Usage";
	public static final String   HTML_DESC        = 
		"<html>"
		+ "Get an overview of what indexes are used the most. (estimates and not actual usages)<br>"
		+ "<br>"
		+ "Note that the counter values here does <b>not</b> reflect <b>actual</b> operators on the tables.<br>"
		+ "Instead the counters reflects what <i>operators</i> the optimizer think it will use, or operators in the execution plan.<br>"
		+ "<br>"
		+ "This DMV (dm_db_index_usage_stats) tells you how many times a query uses an index in its execution plan... but it doesn’t tell you exactly how many times the index was <i>accessed</i>. Only the number of operators referencing it when the plan was run.<br>"
		+ "<br>"
		+ "If you want to view <i>actual</i> counters (what happened during execution), you may look at the collector 'Index Operational' (CmIndexOpStat) instead... which using using the DMV 'dm_db_index_operational_stats' instead.<br>"
		+ "</html>";

	public static final String   GROUP_NAME       = MainFrame.TCP_GROUP_OBJECT_ACCESS;
	public static final String   GUI_ICON_FILE    = "images/"+CM_NAME+".png";

	public static final long     NEED_SRV_VERSION = 0;
	public static final long     NEED_CE_VERSION  = 0;

	public static final String[] MON_TABLES       = new String[] {"dm_db_index_usage_stats"};
	public static final String[] NEED_ROLES       = new String[] {"VIEW SERVER STATE", "CONNECT ANY DATABASE"};
	public static final String[] NEED_CONFIG      = new String[] {};

	public static final String[] PCT_COLUMNS      = new String[] {};
	public static final String[] DIFF_COLUMNS     = new String[] {
//		"database_id",
//		"object_id",
//		"index_id",
		"user_seeks",
		"user_scans",
		"user_lookups",
		"user_updates",
//		"last_user_seek",
//		"last_user_scan",
//		"last_user_lookup",
//		"last_user_update",
		"system_seeks",
		"system_scans",
		"system_lookups",
		"system_updates",
//		"last_system_seek",
//		"last_system_scan",
//		"last_system_lookup",
//		"last_system_update",
		"_last_column_name_only_used_as_a_place_holder_here"
		};

// Microsoft SQL Server 2008 R2 (SP2) - 10.50.4000.0 (X64)  	Jun 28 2012 08:36:30  	Copyright (c) Microsoft Corporation 	Express Edition with Advanced Services (64-bit) on Windows NT 6.1 <X64> (Build 7601: Service Pack 1)

//	RS> Col# Label              JDBC Type Name           Guessed DBMS type
//	RS> ---- ------------------ ------------------------ -----------------
//	RS> 1    dbname             java.sql.Types.NVARCHAR  nvarchar(128)    
//	RS> 2    objectName         java.sql.Types.NVARCHAR  nvarchar(128)    
//	RS> 3    database_id        java.sql.Types.SMALLINT  smallint         
//	RS> 4    object_id          java.sql.Types.INTEGER   int              
//	RS> 5    index_id           java.sql.Types.INTEGER   int              
//	RS> 6    user_seeks         java.sql.Types.BIGINT    bigint           
//	RS> 7    user_scans         java.sql.Types.BIGINT    bigint           
//	RS> 8    user_lookups       java.sql.Types.BIGINT    bigint           
//	RS> 9    user_updates       java.sql.Types.BIGINT    bigint           
//	RS> 10   last_user_seek     java.sql.Types.TIMESTAMP datetime         
//	RS> 11   last_user_scan     java.sql.Types.TIMESTAMP datetime         
//	RS> 12   last_user_lookup   java.sql.Types.TIMESTAMP datetime         
//	RS> 13   last_user_update   java.sql.Types.TIMESTAMP datetime         
//	RS> 14   system_seeks       java.sql.Types.BIGINT    bigint           
//	RS> 15   system_scans       java.sql.Types.BIGINT    bigint           
//	RS> 16   system_lookups     java.sql.Types.BIGINT    bigint           
//	RS> 17   system_updates     java.sql.Types.BIGINT    bigint           
//	RS> 18   last_system_seek   java.sql.Types.TIMESTAMP datetime         
//	RS> 19   last_system_scan   java.sql.Types.TIMESTAMP datetime         
//	RS> 20   last_system_lookup java.sql.Types.TIMESTAMP datetime         
//	RS> 21   last_system_update java.sql.Types.TIMESTAMP datetime         
	
	public static final boolean  NEGATIVE_DIFF_COUNTERS_TO_ZERO = false;
	public static final boolean  IS_SYSTEM_CM                   = true;
	public static final int      DEFAULT_POSTPONE_TIME          = 300;
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

		return new CmIndexUsage(counterController, guiController);
	}

	public CmIndexUsage(ICounterController counterController, IGuiController guiController)
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
	public String[] getDependsOnConfigForVersion(DbxConnection conn, DbmsVersionInfo versionInfo)
	{
		return NEED_CONFIG;
	}

	@Override
	public List<String> getPkForVersion(DbxConnection conn, DbmsVersionInfo versionInfo)
	{
		List <String> pkCols = new LinkedList<String>();

		pkCols.add("database_id");
		pkCols.add("object_id");
		pkCols.add("index_id");

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
			
			_localSortOptions.add(new SortOptions("user_seeks", ColumnNameSensitivity.IN_SENSITIVE, SortOrder.DESCENDING, DataSortSensitivity.IN_SENSITIVE));
		}
		return _localSortOptions;
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
	public String getSqlForVersion(DbxConnection conn, DbmsVersionInfo versionInfo)
	{
		DbmsVersionInfoSqlServer ssVersionInfo = (DbmsVersionInfoSqlServer) versionInfo;

		String dm_db_index_usage_stats = "dm_db_index_usage_stats";
		
		if (ssVersionInfo.isAzureSynapseAnalytics())
			dm_db_index_usage_stats = "dm_pdw_nodes_db_index_usage_stats";

		
//		String sql = "select \n"
//				+ "    dbname     = db_name(database_id), \n"
//				+ "    schemaName = object_schema_name(object_id, database_id), \n"
//				+ "    objectName = object_name(object_id, database_id), \n"
//				+ "    * \n"
//				+ "from sys." + dm_db_index_usage_stats;

		String DbName     = "    DbName     = db_name(database_id), \n";
		String SchemaName = "    SchemaName = (select sys.schemas.name from sys.objects WITH (READUNCOMMITTED) inner join sys.schemas WITH (READUNCOMMITTED) ON sys.schemas.schema_id = sys.objects.schema_id where sys.objects.object_id = BASE.object_id), \n";
		String TableName  = "    TableName  = (select sys.objects.name from sys.objects WITH (READUNCOMMITTED) where sys.objects.object_id = BASE.object_id), \n";
		String IndexName  = "    IndexName  = (select case when sys.indexes.index_id = 0 then 'HEAP' else sys.indexes.name end from sys.indexes WITH (READUNCOMMITTED) where sys.indexes.object_id = BASE.object_id and sys.indexes.index_id = BASE.index_id), \n";

		if (DbmsObjectIdCache.hasInstance() && DbmsObjectIdCache.getInstance().isBulkLoadOnStartEnabled())
		{
			DbName     = "    DbName     = convert(varchar(128), ''), /* using DbmsObjectIdCache to do DbxTune cached lookups */ \n";
			SchemaName = "    SchemaName = convert(varchar(128), ''), /* using DbmsObjectIdCache to do DbxTune cached lookups */ \n";
			TableName  = "    TableName  = convert(varchar(128), ''), /* using DbmsObjectIdCache to do DbxTune cached lookups */ \n";
			IndexName  = "    IndexName  = convert(varchar(128), ''), /* using DbmsObjectIdCache to do DbxTune cached lookups */ \n";
		}
		
		String sql = ""
			    + "-- Note: Below SQL Statement is executed in every database that is 'online', more or less like: sp_msforeachdb \n"
			    + "-- Note: object_schema_name() and object_name() can NOT be used for 'dirty-reads', they may block... hence the 'ugly' fullname sub-selects in the select column list \n"
			    + "-- Note: To enable/disable DbxTune Cached Lookups for ObjectID to name translation is done with property '" + DbmsObjectIdCacheSqlServer.PROPKEY_BulkLoadOnStart + "=true|false'. Current Status=" + (DbmsObjectIdCache.hasInstance() && DbmsObjectIdCache.getInstance().isBulkLoadOnStartEnabled() ? "ENABLED" : "DISABLED") + " \n"
			    + "select /* ${cmCollectorName} */ \n"
			    +      DbName
			    +      SchemaName
			    +      TableName
			    +      IndexName
			    + "    * \n"
			    + "from sys." + dm_db_index_usage_stats + " BASE \n"
			    + "where BASE.database_id = db_id() \n"
			    + "";

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


	@Override
	public void localCalculation(CounterSample newSample)
	{
		// Resolve "database_id", "object_id", "index_id" to real names 
		if (DbmsObjectIdCache.hasInstance() && DbmsObjectIdCache.getInstance().isBulkLoadOnStartEnabled())
		{
			DbmsObjectIdCacheUtils.localCalculation_DbmsObjectIdFiller(this, newSample, 
					"database_id", "object_id", "index_id",             // Source columns to translate into the below columns 
					"DbName", "SchemaName", "TableName", "IndexName");  // Target columns for the above source columns
		}
	}
	

//	@Override
//	public void localCalculation(CounterSample newSample)
//	{
//		// Resolve "database_id", "object_id", "index_id" to real names 
//		if (DbmsObjectIdCache.hasInstance() && DbmsObjectIdCache.getInstance().isBulkLoadOnStartEnabled())
//		{
//			int DbName_pos      = newSample.findColumn("DbName");
//			int SchemaName_pos  = newSample.findColumn("SchemaName");
//			int TableName_pos   = newSample.findColumn("TableName");
//			int IndexName_pos   = newSample.findColumn("IndexName");
//
//			int database_id_pos = newSample.findColumn("database_id");
//			int object_id_pos   = newSample.findColumn("object_id");
//			int index_id_pos    = newSample.findColumn("index_id");
//
//			if (DbName_pos == -1 || SchemaName_pos == -1 || TableName_pos == -1 || IndexName_pos == -1 || database_id_pos == -1 || object_id_pos == -1 || index_id_pos == -1)
//			{
//				_logger.error("Missing mandatory columns in " + getName() + ".localCalculation. DbName_pos=" + DbName_pos + ", SchemaName_pos=" + SchemaName_pos + ", TableName_pos=" + TableName_pos + ", IndexName_pos=" + IndexName_pos + ", database_id_pos=" + database_id_pos + ", object_id_pos=" + object_id_pos + ", index_id_pos=" + index_id_pos + ".");
//				return;
//			}
//
//			// Get the DBMS ID Cache Instance
//			DbmsObjectIdCache idc = DbmsObjectIdCache.getInstance();
//
//			// Loop on all counter rows
//			for (int rowId=0; rowId < newSample.getRowCount(); rowId++) 
//			{
//				// Do the lookup
//				Integer database_id = newSample.getValueAsInteger(rowId, database_id_pos);
//				Integer object_id   = newSample.getValueAsInteger(rowId, object_id_pos);
//				Integer index_id    = newSample.getValueAsInteger(rowId, index_id_pos);
//
//				String DbName      = idc.getDBName(database_id);;
//				String SchemaName  = null;
//				String TableName   = null;
//				String IndexName   = null;
//
//				try
//				{
//					ObjectInfo objInfo = idc.getByObjectId(database_id, object_id);
//					if (objInfo != null)
//					{
//						SchemaName  = objInfo.getSchemaName();
//						TableName   = objInfo.getObjectName();
//						IndexName   = objInfo.getIndexName(index_id);
//					}
//
//					newSample.setValueAt(DbName    , rowId, DbName_pos);
//					newSample.setValueAt(SchemaName, rowId, SchemaName_pos);
//					newSample.setValueAt(TableName , rowId, TableName_pos);
//					newSample.setValueAt(IndexName , rowId, IndexName_pos);
//
////System.out.println(getName() + ": localCalculation: Resolved-DbmsObjectIdCache -- database_id=" + database_id + ", object_id=" + object_id + ", index_id=" + index_id + "  --->>>  DbName='" + DbName + "', SchemaName='" + SchemaName + "', TableName='" + TableName + "', IndexName='" + IndexName + "'.");
//				}
//				catch (TimeoutException ex)
//				{
//					_logger.warn("Timeout exception in localCalculation() when getting id's to names. look up on: database_id=" + database_id + ", object_id=" + object_id + ", index_id=" + index_id + ".", ex);
//				}
//			}
//		}
//	}


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
//		String[] sa = {"DbName", "SchemaName", "TableName"};
		String[] sa = {"DbName", "TableName"};
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
		String[] sa = {"user_seeks", "user_scans", "user_lookups", "user_updates"};
		return sa;
	}
}
