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

import java.util.LinkedList;
import java.util.List;

import javax.naming.NameNotFoundException;

import com.asetune.ICounterController;
import com.asetune.IGuiController;
import com.asetune.cm.CounterSample;
import com.asetune.cm.CounterSampleCatalogIteratorSqlServer;
import com.asetune.cm.CounterSetTemplates;
import com.asetune.cm.CounterSetTemplates.Type;
import com.asetune.cm.CountersModel;
import com.asetune.config.dict.MonTablesDictionary;
import com.asetune.config.dict.MonTablesDictionaryManager;
import com.asetune.gui.MainFrame;
import com.asetune.sql.conn.DbxConnection;
import com.asetune.sql.conn.info.DbmsVersionInfo;

/**
 * @author Goran Schwarz (goran_schwarz@hotmail.com)
 */
public class CmIndexMissing
extends CountersModel
{
//	private static Logger        _logger          = Logger.getLogger(CmIndexMissing.class);
	private static final long    serialVersionUID = 1L;

	public static final String   CM_NAME          = CmIndexMissing.class.getSimpleName();
	public static final String   SHORT_NAME       = "Index Missing";
	public static final String   HTML_DESC        = 
		"<html>" +
		"Missing Indexes" +
		"<br>" +

		"Take these recomendations with a <i>grain of salt</i>" +
		"<br>" +
		
		"The Create Index Statement (equality, in-equality and include columns) is based on what the optimizer think would be a <i><b>perfect</b></i> index for a specific query.<br>" +
		"So you could probably <b>strip off</b> a couple of columns especially the <i>incude</i> part, and get a better index/result...<br>" +
		"That means: including to many columns in the index, makes the index <b>bigger</b> and less effective... and also more work needs to be done maintaining the indexes.<br>" +
		"<br>" +
		"</html>";

	public static final String   GROUP_NAME       = MainFrame.TCP_GROUP_OBJECT_ACCESS;
	public static final String   GUI_ICON_FILE    = "images/"+CM_NAME+".png";

	public static final long     NEED_SRV_VERSION = 0;
	public static final long     NEED_CE_VERSION  = 0;

	public static final String[] MON_TABLES       = new String[] {CmIndexMissing.class.getSimpleName(), "dm_db_missing_index_group_stats", "dm_db_missing_index_groups", "dm_db_missing_index_details"};
	public static final String[] NEED_ROLES       = new String[] {"VIEW SERVER STATE"};
	public static final String[] NEED_CONFIG      = new String[] {};

	public static final String[] PCT_COLUMNS      = new String[] {};
	public static final String[] DIFF_COLUMNS     = new String[] {
		 "unique_compiles"
		,"user_scans"
		,"user_seeks"
		,"system_scans"
		,"system_seeks"
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

		return new CmIndexMissing(counterController, guiController);
	}

	public CmIndexMissing(ICounterController counterController, IGuiController guiController)
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
	public void addMonTableDictForVersion(DbxConnection conn, DbmsVersionInfo versionInfo)
	{
		try 
		{
			String cmName = this.getName();
			
			MonTablesDictionary mtd = MonTablesDictionaryManager.getInstance();
			mtd.addTable(cmName, "");

			mtd.addColumn(cmName,  "Impact",       "<html>The calculated <i>impact</i> this missing index has, also used to sort the output.<br>"
			                                        + "<b>Formula</b>: (avg_total_user_cost * avg_user_impact) * (user_seeks + user_scans)"
			                                        + "</html>");

		}
		catch (NameNotFoundException e) 
		{
		//	_logger.warn("Problems in cm='" + CM_NAME + "', adding addMonTableDictForVersion. Caught: " + e); 
			System.out.println("Problems in cm='" + CM_NAME + "', adding addMonTableDictForVersion. Caught: " + e); 
		}
	}

	@Override
	public List<String> getPkForVersion(DbxConnection conn, DbmsVersionInfo versionInfo)
	{
		List <String> pkCols = new LinkedList<String>();

		pkCols.add("DbName");
		pkCols.add("SchemaName");
		pkCols.add("TableName");
		pkCols.add("index_handle");
		
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
	public String getSqlForVersion(DbxConnection conn, DbmsVersionInfo versionInfo)
	{
		String dm_db_missing_index_group_stats = "sys.dm_db_missing_index_group_stats";
		String dm_db_missing_index_groups      = "sys.dm_db_missing_index_groups";
		String dm_db_missing_index_details     = "sys.dm_db_missing_index_details";
		
//		if (isAzure)
//		{
//			dm_db_missing_index_group_stats = "sys.dm_db_missing_index_group_stats";
//			dm_db_missing_index_groups      = "sys.dm_db_missing_index_groups";
//			dm_db_missing_index_details     = "sys.dm_db_missing_index_details";
//		}

		// FIXME: for 2019 possibly use 'dm_db_missing_index_group_stats_query' to also get 'SQL Text' or 'Query Plan' for the "source" of the missing-index-recommendation
		
		
		String sql = ""
			    + "-- Note: Below SQL Statement is executed in every database that is 'online', more or less like: sp_msforeachdb \n"
			    + "-- Note: object_schema_name() and object_name() can NOT be used for 'dirty-reads', they may block... hence the 'ugly' fullname sub-selects in the select column list \n"
			    + "select /* ${cmCollectorName} */ \n"
			    + "      DbName      = db_name(mid.database_id) \n"
//			    + "    , SchemaName  = object_schema_name(mid.object_id, mid.database_id) \n"
//			    + "    , TableName   = object_name(mid.object_id, mid.database_id) \n"
				+ "    , SchemaName  = (select sys.schemas.name from sys.objects WITH (READUNCOMMITTED) inner join sys.schemas WITH (READUNCOMMITTED) ON sys.schemas.schema_id = sys.objects.schema_id where sys.objects.object_id = mid.object_id) \n"
				+ "    , TableName   = (select sys.objects.name from sys.objects WITH (READUNCOMMITTED) where sys.objects.object_id = mid.object_id) \n"
			    + "    , TableRows   = (select sum(ps.row_count)                                                           FROM sys.dm_db_partition_stats ps WHERE ps.object_id = mid.object_id AND ps.index_id in(0,1) GROUP BY ps.object_id) \n"
			    + "    , TableSizeMB = (select CAST(ROUND(((SUM(ps.used_page_count) * 8) / 1024.00), 1) AS NUMERIC(18, 1)) FROM sys.dm_db_partition_stats ps WHERE ps.object_id = mid.object_id AND ps.index_id in(0,1) GROUP BY ps.object_id) \n"
			    + " \n"
			    + "    , mig.index_handle \n"
			    + " \n"
			    + "    , Impact     = convert(bigint, (migs.avg_total_user_cost * migs.avg_user_impact) * (migs.user_seeks + migs.user_scans) ) \n"
			    + "    , migs.unique_compiles \n"
			    + " \n"
			    + "    , migs.user_scans \n"
			    + "    , migs.user_seeks \n"
			    + "    , migs.last_user_scan \n"
			    + "    , migs.last_user_seek \n"
			    + "    , migs.avg_total_user_cost \n"
			    + "    , migs.avg_user_impact \n"
			    + " \n"
			    + "    , migs.system_scans \n"
			    + "    , migs.system_seeks \n"
			    + "    , migs.last_system_scan \n"
			    + "    , migs.last_system_seek \n"
			    + "    , migs.avg_total_system_cost \n"
			    + "    , migs.avg_system_impact \n"
			    + "    \n"
			    + "    , mid.equality_columns \n"
			    + "    , mid.inequality_columns \n"
			    + "    , mid.included_columns \n"
//			    + "    , CreateIndexStatement = 'CREATE NONCLUSTERED INDEX ' + object_name(mid.object_id, mid.database_id) + '_xxx_ix' + convert(varchar(10), mid.index_handle) \n"
			    + "    , CreateIndexStatement = 'CREATE NONCLUSTERED INDEX ' + (select sys.objects.name from sys.objects WITH (READUNCOMMITTED) where sys.objects.object_id = mid.object_id) COLLATE DATABASE_DEFAULT + '_xxx_ix' + convert(varchar(10), mid.index_handle) \n"
			    + "         + ' ON ' + mid.statement + ' ' \n"
//			    + "--       +  '[' + db_name(mid.database_id)                           COLLATE DATABASE_DEFAULT + ']' \n"
//			    + "--       + '.[' + object_schema_name(mid.object_id, mid.database_id) COLLATE DATABASE_DEFAULT + ']' \n"
//			    + "--       + '.[' + object_name(mid.object_id, mid.database_id)        COLLATE DATABASE_DEFAULT + '] ' \n"
			    + "         + '(' \n"
			    + "         + isnull(mid.equality_columns, '') \n"
			    + "         -- The below is if we should add a ',' after \"equality_columns\" \n"
			    + "         + CASE \n"
			    + "         	WHEN mid.inequality_columns IS NULL THEN '' \n"
			    + "         	ELSE CASE WHEN mid.equality_columns IS NULL THEN '' ELSE ',' END \n"
			    + "           END \n"
			    + "         + isnull(mid.inequality_columns, '') \n"
			    + "         + ')' \n"
			    + "         + CASE WHEN mid.included_columns IS NULL THEN '' \n"
			    + "                ELSE ' INCLUDE (' + mid.included_columns + ')' \n"
			    + "           END \n"
			    + "from "       + dm_db_missing_index_group_stats + " migs \n"
			    + "inner join " + dm_db_missing_index_groups      + " mig on migs.group_handle = mig.index_group_handle \n"
			    + "inner join " + dm_db_missing_index_details     + " mid on mig.index_handle  = mid.index_handle \n"
			    + "where 1 = 1 \n"
			    + "  and  mid.database_id = DB_ID() \n"
			    + "  and migs.group_handle in ( \n"
			    + "                             select top 500 group_handle \n"
			    + "                             from " + dm_db_missing_index_group_stats + " x \n"
			    + "                             order by (x.avg_total_user_cost * x.avg_user_impact) * (x.user_seeks + x.user_scans) \n"
			    + "                           ) \n"
			    + "order by 7 desc \n"
			    + "";
		
		return sql;
	}
}
