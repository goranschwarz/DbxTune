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

import com.asetune.ICounterController;
import com.asetune.IGuiController;
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
public class CmIndexUnused
extends CountersModel
{
//	private static Logger        _logger          = Logger.getLogger(CmIndexUnused.class);
	private static final long    serialVersionUID = 1L;

	public static final String   CM_NAME          = CmIndexUnused.class.getSimpleName();
	public static final String   SHORT_NAME       = "Index Unused";
	public static final String   HTML_DESC        = 
		"<html>" +
		"<p>Unused Indexes</p>" +
				
//		"<p>Take these recomendations with a <i>grain of salt</i> </p>" +
//		
//		"<p>" +
//		"The Create Index Statement (equality, in-equality and include columns) is based on what the optimizer think would be a <i><b>perfect</b></i> index for a specific query.<br>" +
//		"So you could probably <b>strip off</b> a couple of columns especially the <i>incude</i> part, and get a better index/result...<br>" +
//		"That means: including to many columns in the index, makes the index <b>bigger</b> and less effective... and also more work needs to be done maintaining the indexes.<br>" +
//		"</p>" +
		"</html>";

	public static final String   GROUP_NAME       = MainFrame.TCP_GROUP_OBJECT_ACCESS;
	public static final String   GUI_ICON_FILE    = "images/"+CM_NAME+".png";

	public static final long     NEED_SRV_VERSION = 0;
	public static final long     NEED_CE_VERSION  = 0;

	public static final String[] MON_TABLES       = new String[] {"dm_db_index_usage_stats"};
	public static final String[] NEED_ROLES       = new String[] {"VIEW SERVER STATE", "CONNECT ANY DATABASE"};
	public static final String[] NEED_CONFIG      = new String[] {};

	public static final String[] PCT_COLUMNS      = new String[] {};
	public static final String[] DIFF_COLUMNS     = new String[] {};


	public static final boolean  NEGATIVE_DIFF_COUNTERS_TO_ZERO = false;
	public static final boolean  IS_SYSTEM_CM                   = true;
	public static final int      DEFAULT_POSTPONE_TIME          = 3600 * 4; // 4 hour
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

		return new CmIndexUnused(counterController, guiController);
	}

	public CmIndexUnused(ICounterController counterController, IGuiController guiController)
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
//		return new CmIndexMissingPanel(this);
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

		pkCols.add("DbName");
		pkCols.add("SchemaName");
		pkCols.add("TableName");
		pkCols.add("IndexName");
		
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
			
//			_localSortOptions.add(new SortOptions("index_id"    , ColumnNameSensitivity.IN_SENSITIVE, SortOrder.ASCENDING , DataSortSensitivity.IN_SENSITIVE)); // index_id, just for test
			_localSortOptions.add(new SortOptions("user_updates", ColumnNameSensitivity.IN_SENSITIVE, SortOrder.DESCENDING, DataSortSensitivity.IN_SENSITIVE));
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

		String dm_db_index_usage_stats = "sys.dm_db_index_usage_stats";
		String dm_db_partition_stats   = "sys.dm_db_partition_stats";
		
		if (ssVersionInfo.isAzureSynapseAnalytics())
		{
			dm_db_index_usage_stats = "sys.dm_pdw_nodes_db_index_usage_stats";
			dm_db_partition_stats   = "sys.dm_pdw_nodes_db_partition_stats";
		}

//		String sql = ""
//			    + "declare @cmd varchar(8000) = ' \n"
//			    + "use ?; \n"
//			    + "SELECT \n"
//			    + "	  DbName     = db_name(u.database_id) \n"
//			    + "	, SchemaName = object_schema_name(u.object_id, u.database_id) \n"
//			    + "	, TableName  = o.name \n"
//			    + "	, IndexName  = i.name \n"
//			    + "	, o.create_date \n"
//			    + "	, i.index_id \n"
//			    + "	, u.user_seeks \n"
//			    + "	, u.user_scans \n"
//			    + "	, u.user_lookups \n"
//			    + "	, u.user_updates \n"
//			    + "	, p.TableRows \n"
//			    + "	, DropStatement = ''DROP INDEX ['' + i.name + ''] ON ['' + db_name(u.database_id) + ''].['' + object_schema_name(u.object_id, u.database_id) + ''].['' + o.name + '']'' \n"
//			    + "FROM " + dm_db_index_usage_stats + " u \n"
//			    + "INNER JOIN sys.indexes i ON i.index_id = u.index_id 	AND u.object_id = i.object_id \n"
//			    + "INNER JOIN sys.objects o ON u.object_id = o.object_id \n"
//			    + "INNER JOIN (SELECT SUM(p.rows) TableRows, p.index_id, p.object_id \n"
//			    + "			FROM sys.partitions p \n"
//			    + "			GROUP BY p.index_id, p.object_id) p \n"
//			    + "	ON p.index_id = u.index_id \n"
//			    + "	AND u.object_id = p.object_id \n"
//			    + "WHERE OBJECTPROPERTY(u.object_id,''IsUserTable'') = 1 \n"
//			    + "  AND u.database_id          = DB_ID() \n"
//			    + "  AND i.type_desc            =''nonclustered'' \n"
//			    + "  AND i.is_primary_key       = 0 \n"
//			    + "  AND i.is_unique_constraint = 0 \n"
//			    + "  AND o.is_ms_shipped       != 1 \n"
//			    + " \n"
//			    + "  AND u.user_seeks   = 0 \n"
//			    + "  AND u.user_scans   = 0 \n"
//			    + "  AND u.user_lookups = 0 \n"
//			    + "ORDER BY u.user_updates desc; \n" // NOTE: we are doing LOCAL sorts also: see getLocalSortOptions();
//			    + "use master; \n"
//			    + "' \n"
//			    + "exec sys.sp_MSforeachdb @cmd \n"
//			    + "";

//		String sql = ""
//			    + "-- Note: Below SQL Statement is executed in every database that is 'online', more or less like: sp_msforeachdb \n"
//			    + "SELECT \n"
//			    + "	  DbName     = db_name(u.database_id) \n"
//			    + "	, SchemaName = object_schema_name(u.object_id, u.database_id) \n"
//			    + "	, TableName  = o.name \n"
//			    + "	, IndexName  = i.name \n"
//			    + "	, o.create_date \n"
//			    + "	, i.index_id \n"
//			    + "	, u.user_seeks \n"
//			    + "	, u.user_scans \n"
//			    + "	, u.user_lookups \n"
//			    + "	, u.user_updates \n"
//			    + "	, p.TableRows \n"
//			    + "	, DropStatement = 'DROP INDEX [' + i.name + '] ON [' + db_name(u.database_id) + '].[' + object_schema_name(u.object_id, u.database_id) + '].[' + o.name + ']' \n"
//			    + "FROM " + dm_db_index_usage_stats + " u \n"
//			    + "INNER JOIN sys.indexes i ON i.index_id = u.index_id 	AND u.object_id = i.object_id \n"
//			    + "INNER JOIN sys.objects o ON u.object_id = o.object_id \n"
//			    + "INNER JOIN (SELECT SUM(p.rows) TableRows, p.index_id, p.object_id \n"
//			    + "			FROM sys.partitions p \n"
//			    + "			GROUP BY p.index_id, p.object_id) p \n"
//			    + "	ON p.index_id = u.index_id \n"
//			    + "	AND u.object_id = p.object_id \n"
//			    + "WHERE OBJECTPROPERTY(u.object_id,'IsUserTable') = 1 \n"
//			    + "  AND u.database_id          = DB_ID() \n"
//			    + "  AND i.type_desc            ='nonclustered' \n"
//			    + "  AND i.is_primary_key       = 0 \n"
//			    + "  AND i.is_unique_constraint = 0 \n"
//			    + "  AND o.is_ms_shipped       != 1 \n"
//			    + " \n"
//			    + "  AND u.user_seeks   = 0 \n"
//			    + "  AND u.user_scans   = 0 \n"
//			    + "  AND u.user_lookups = 0 \n"
//			    + "ORDER BY u.user_updates desc \n" // NOTE: we are doing LOCAL sorts also: see getLocalSortOptions();
//			    + "";

		String sql = ""
				+ "-- Note: Below SQL Statement is executed in every database that is 'online', more or less like: sp_msforeachdb \n"
				+ "WITH \n"
				+ "key_cols AS \n"
				+ "( \n"
				+ "      SELECT IC2.object_id, /* ${cmCollectorName} */ \n"
				+ "             IC2.index_id, \n"
				+ "             STUFF( ( \n"
				+ "                     SELECT ', [' + C.name + ']' + CASE WHEN MAX(CONVERT(INT, IC1.is_descending_key)) = 1 THEN ' DESC' ELSE '' END \n"
				+ "                     FROM sys.index_columns IC1 \n"
				+ "                     JOIN sys.columns C ON  C.object_id = IC1.object_id AND C.column_id = IC1.column_id AND IC1.is_included_column = 0 \n"
				+ "                     WHERE  IC1.object_id = IC2.object_id \n"
				+ "                       AND IC1.index_id = IC2.index_id \n"
				+ "                     GROUP BY IC1.object_id, C.name, index_id \n"
				+ "                     ORDER BY MAX(IC1.key_ordinal) \n"
				+ "                     FOR XML PATH('') \n"
				+ "                 ), 1, 2, '') AS KeyColumns \n"
				+ "      FROM .sys.index_columns IC2 \n"
				+ "      GROUP BY IC2.object_id, IC2.index_id \n"
				+ "), \n"
				+ "include_cols AS \n"
				+ "( \n"
				+ "      SELECT IC2.object_id, /* ${cmCollectorName} */ \n"
				+ "             IC2.index_id, \n"
				+ "             STUFF( ( \n"
				+ "                     SELECT ', [' + C.name + ']' \n"
				+ "                     FROM sys.index_columns IC1 \n"
				+ "                     JOIN sys.columns C ON  C.object_id = IC1.object_id AND C.column_id = IC1.column_id AND IC1.is_included_column = 1 \n"
				+ "                     WHERE  IC1.object_id = IC2.object_id \n"
				+ "                       AND IC1.index_id = IC2.index_id \n"
				+ "                     GROUP BY IC1.object_id, C.name, index_id \n"
				+ "                     FOR XML PATH('') \n"
				+ "                 ), 1, 2, '') AS IncludedColumns \n"
				+ "      FROM sys.index_columns IC2 \n"
				+ "      GROUP BY IC2.object_id, IC2.index_id \n"
				+ ") \n"
				
				+ "SELECT /* ${cmCollectorName} */ \n"
				+ "       DbName     = db_name(u.database_id) \n"
				+ "     , SchemaName = object_schema_name(u.object_id, u.database_id) \n"
				+ "     , TableName  = o.name \n"
				+ "     , IndexName  = i.name \n"
				+ "     , o.create_date \n"
				+ "     , i.index_id \n"
				+ "     , u.user_seeks \n"
				+ "     , u.user_scans \n"
				+ "     , u.user_lookups \n"
				+ "     , u.user_updates \n"
				+ "     , p.IndexSizeMB \n"
				+ "     , p.TableRows \n"
				+ "     , TableSizeMB  = (select CAST(ROUND(((SUM(ps2.used_page_count) * 8) / 1024.00), 1) AS NUMERIC(18, 1)) \n"
				+ "                       from " + dm_db_partition_stats + " ps2 \n"
				+ "                       where ps2.object_id = u.object_id \n"
				+ "                         and ps2.index_id in(0,1) \n"
				+ "                       group by ps2.object_id, ps2.index_id \n"
				+ "                      ) \n"
				+ "     , DropStatement   = cast('DROP INDEX [' + i.name + '] ON [' + db_name(u.database_id) + '].[' + object_schema_name(u.object_id, u.database_id) + '].[' + o.name + ']' as varchar(1024)) \n"
				+ "     , CreateStatement = cast('CREATE ' + CASE WHEN i.is_unique = 1 THEN ' UNIQUE ' ELSE '' END + i.type_desc COLLATE DATABASE_DEFAULT + ' INDEX ' \n"
				+ "           + '[' + i.name + '] ON [' + db_name(u.database_id) + '].[' + sc.name + '].[' + o.name + '](' + kc.KeyColumns + ')' \n"
				+ "           + ISNULL(' INCLUDE (' + ic.IncludedColumns + ' ) ', '') \n"
				+ "           + ISNULL(' WHERE ' + i.filter_definition, '') \n"
				+ "           + ' WITH (' \n"
				+ "           + CASE WHEN i.is_padded = 1 THEN 'PAD_INDEX = ON, ' ELSE '' END \n"
				+ "           + 'FILLFACTOR = ' + CAST( CASE WHEN i.fill_factor = 0 THEN 100 ELSE i.fill_factor END as varchar(10)) + ', ' \n"
				+ "           + 'SORT_IN_TEMPDB = OFF, ' \n"
				+ "           + CASE WHEN i.ignore_dup_key              = 0 THEN '' ELSE 'IGNORE_DUP_KEY = ON, ' END \n"
				+ "           + CASE WHEN i.allow_row_locks             = 1 THEN '' ELSE 'ALLOW_ROW_LOCKS = OFF, '  END \n"
				+ "           + CASE WHEN i.allow_page_locks            = 1 THEN '' ELSE 'ALLOW_PAGE_LOCKS = OFF, ' END \n"
//				+ "           + CASE WHEN i.optimize_for_sequential_key = 0 THEN '' ELSE 'OPTIMIZE_FOR_SEQUENTIAL_KEY = OFF, ' END \n" // NOTE: this is new in 2019
				+ "           + 'DATA_COMPRESSION = ?, ' \n"
				+ "           + 'MAXDOP = 0, ' \n"
				+ "           + 'ONLINE = OFF' \n"
				+ "           + ')' as varchar(2048)) \n"
				+ "\n"
				+ "FROM " + dm_db_index_usage_stats + " u \n"
				+ "INNER JOIN sys.indexes i WITH (READUNCOMMITTED) ON i.index_id = u.index_id 	AND u.object_id = i.object_id \n"
				+ "INNER JOIN sys.objects o WITH (READUNCOMMITTED) ON u.object_id = o.object_id \n"
				+ "INNER JOIN (SELECT \n"
				+ "                 ps.index_id \n"
				+ "               , ps.object_id \n"
				+ "               , max(ps.row_count) AS TableRows \n"
				+ "               , CAST(ROUND(((SUM(ps.used_page_count) * 8) / 1024.00), 1) AS NUMERIC(18, 1)) AS IndexSizeMB \n"
				+ "            FROM " + dm_db_partition_stats + " ps \n"
				+ "            GROUP BY ps.index_id, ps.object_id \n"
				+ "           ) p	ON p.index_id = u.index_id AND p.object_id = u.object_id \n"
				+ "LEFT OUTER JOIN sys.schemas  sc ON o.schema_id = sc.schema_id \n"
				+ "LEFT OUTER JOIN key_cols     kc ON o.object_id = kc.object_id and i.index_id = kc.index_id \n"
				+ "LEFT OUTER JOIN include_cols ic ON o.object_id = ic.object_id and i.index_id = ic.index_id \n"
				+ "WHERE OBJECTPROPERTY(u.object_id,'IsUserTable') = 1 \n"
				+ "  AND u.database_id          = DB_ID() \n"
				+ "  AND i.type_desc            ='nonclustered' \n"
				+ "  AND i.is_primary_key       = 0 \n"
				+ "  AND i.is_unique_constraint = 0 \n"
				+ "  AND o.is_ms_shipped       != 1 \n"
				+ " \n"
				+ "  AND u.user_seeks   = 0 \n"
				+ "  AND u.user_scans   = 0 \n"
				+ "  AND u.user_lookups = 0 \n"
				+ "ORDER BY u.user_updates desc \n"
				+ " \n"
				+ "";
		
		return sql;
	}
}
