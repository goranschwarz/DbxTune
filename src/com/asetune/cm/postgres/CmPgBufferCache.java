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
package com.asetune.cm.postgres;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeoutException;

import org.apache.log4j.Logger;

import com.asetune.ICounterController;
import com.asetune.IGuiController;
import com.asetune.cache.DbmsObjectIdCache;
import com.asetune.cache.DbmsObjectIdCache.ObjectInfo;
import com.asetune.cm.CmSettingsHelper;
import com.asetune.cm.CounterSample;
import com.asetune.cm.CounterSetTemplates;
import com.asetune.cm.CounterSetTemplates.Type;
import com.asetune.cm.CountersModel;
import com.asetune.cm.postgres.gui.CmPgBufferCachePanel;
import com.asetune.gui.MainFrame;
import com.asetune.gui.TabularCntrPanel;
import com.asetune.sql.conn.DbxConnection;
import com.asetune.sql.conn.info.DbmsVersionInfo;
import com.asetune.utils.Configuration;

/**
 * @author Goran Schwarz (goran_schwarz@hotmail.com)
 */
public class CmPgBufferCache
extends CountersModel
{
	private static Logger        _logger          = Logger.getLogger(CmPgBufferCache.class);
	private static final long    serialVersionUID = 1L;

	public static final String   CM_NAME          = CmPgBufferCache.class.getSimpleName();
	public static final String   SHORT_NAME       = "Buffer Cache";
	public static final String   HTML_DESC        = 
		"<html>" +
		"Table names and size in the Buffer Cache. (from pg_buffercache)<br>" +
		"<br>" +
		"<b>Note:</b> Depends on extenstion: pg_buffercache, which is created on first sample (if not exists)<br>" +
		"<code>CREATE EXTENSION IF NOT EXISTS pg_buffercache</code>" +
		"</html>";

	public static final String   GROUP_NAME       = MainFrame.TCP_GROUP_CACHE;
	public static final String   GUI_ICON_FILE    = "images/"+CM_NAME+".png";

//	public static final long     NEED_SRV_VERSION = Ver.ver(14);
	public static final long     NEED_SRV_VERSION = 0;
	public static final long     NEED_CE_VERSION  = 0;

	public static final String[] MON_TABLES       = new String[] {"pg_buffercache"};
	public static final String[] NEED_ROLES       = new String[] {};
//	public static final String[] NEED_CONFIG      = new String[] {"EXTENSION: pg_buffercache"};
	public static final String[] NEED_CONFIG      = new String[] {};

	public static final String[] PCT_COLUMNS      = new String[] {};
	public static final String[] DIFF_COLUMNS     = new String[] {
			"buffer_count_diff"
	};

	public static final boolean  NEGATIVE_DIFF_COUNTERS_TO_ZERO = false;
	public static final boolean  IS_SYSTEM_CM                   = true;
	public static final int      DEFAULT_POSTPONE_TIME          = 600; // 10 minutes
	public static final int      DEFAULT_QUERY_TIMEOUT          = CountersModel.DEFAULT_sqlQueryTimeout;

	@Override public int     getDefaultPostponeTime()                 { return DEFAULT_POSTPONE_TIME; }
	@Override public int     getDefaultQueryTimeout()                 { return DEFAULT_QUERY_TIMEOUT; }
	@Override public boolean getDefaultIsNegativeDiffCountersToZero() { return NEGATIVE_DIFF_COUNTERS_TO_ZERO; }
	@Override public Type    getTemplateLevel()                       { return Type.MEDIUM; }

	/**
	 * FACTORY  method to create the object
	 */
	public static CountersModel create(ICounterController counterController, IGuiController guiController)
	{
		if (guiController != null && guiController.hasGUI())
			guiController.splashWindowProgress("Loading: Counter Model '"+CM_NAME+"'");

		return new CmPgBufferCache(counterController, guiController);
	}

	public CmPgBufferCache(ICounterController counterController, IGuiController guiController)
	{
		super(counterController,
				CM_NAME, GROUP_NAME, /*sql*/null, /*pkList*/null, 
				DIFF_COLUMNS, PCT_COLUMNS, MON_TABLES, 
				NEED_ROLES, NEED_CONFIG, NEED_SRV_VERSION, NEED_CE_VERSION, 
				NEGATIVE_DIFF_COUNTERS_TO_ZERO, IS_SYSTEM_CM, DEFAULT_POSTPONE_TIME);

		setDisplayName(SHORT_NAME);
		setDescription(HTML_DESC);

		setIconFile(GUI_ICON_FILE);

//		addDependsOnCm(CmPgTables.CM_NAME); // CmPgTables "must" have been executed before this cm, otherwise dbname, schema_name and table_name wont be set
		
		setCounterController(counterController);
		setGuiController(guiController);
		
		addTrendGraphs();
		
		CounterSetTemplates.register(this);
	}


	//------------------------------------------------------------
	// Implementation
	//------------------------------------------------------------
	public static final String  PROPKEY_sample_topRows                = CM_NAME + ".sample.topRows";
	public static final boolean DEFAULT_sample_topRows                = false;

	public static final String  PROPKEY_sample_topRowsCount           = CM_NAME + ".sample.topRows.count";
	public static final int     DEFAULT_sample_topRowsCount           = 5000;

	@Override
	protected void registerDefaultValues()
	{
		super.registerDefaultValues();

		Configuration.registerDefaultValue(PROPKEY_sample_topRows,                DEFAULT_sample_topRows);
		Configuration.registerDefaultValue(PROPKEY_sample_topRowsCount,           DEFAULT_sample_topRowsCount);
	}
	
	/** Used by the: Create 'Offline Session' Wizard */
	@Override
	public List<CmSettingsHelper> getLocalSettings()
	{
		Configuration conf = Configuration.getCombinedConfiguration();
		List<CmSettingsHelper> list = new ArrayList<>();
		
		list.add(new CmSettingsHelper("Limit num of rows",     PROPKEY_sample_topRows      , Boolean.class, conf.getBooleanProperty(PROPKEY_sample_topRows      , DEFAULT_sample_topRows      ), DEFAULT_sample_topRows     , "Get only first # rows (select top # ...) true or false"   ));
		list.add(new CmSettingsHelper("Limit num of rowcount", PROPKEY_sample_topRowsCount , Integer.class, conf.getIntProperty    (PROPKEY_sample_topRowsCount , DEFAULT_sample_topRowsCount ), DEFAULT_sample_topRowsCount, "Get only first # rows (select top # ...), number of rows" ));

		return list;
	}

	
	@Override
	protected TabularCntrPanel createGui()
	{
		return new CmPgBufferCachePanel(this);
	}
	
	/**
	 * get/fill in 'dbname', 'schema_name' and 'schema_name' from ObjectIdCache based on the 'database:id' and 'relation:id'
	 */
	@Override
	public void localCalculation(CounterSample newSample)
	{
		// Exit early - If the cache isn't available
		if ( ! DbmsObjectIdCache.hasInstance() )
		{
			_logger.info("localCalculation(): No DbmsObjectIdCache available, can't resolv 'database:id' and 'relation:id' into real names.");
			return;
		}

		int database_pos      = newSample.findColumn("reldatabase");
		int relation_pos      = newSample.findColumn("relfilenode");

		int dbname_pos        = newSample.findColumn("dbname");
		int schema_name_pos   = newSample.findColumn("schema_name");
		int relation_name_pos = newSample.findColumn("relation_name");

		// No need to continue if we havn't got the columns we need
		if (database_pos == -1 || relation_pos == -1 || dbname_pos == -1 || schema_name_pos == -1 || relation_name_pos == -1)
		{
			_logger.info("localCalculation(): Desired columns not available (database_pos="+database_pos+", relation_pos="+relation_pos+", dbname_pos="+dbname_pos+", schema_name_pos="+schema_name_pos+", relation_name_pos="+relation_name_pos+"), can't resolv 'database:id' and 'relation:id' into real names.");
			return;
		}

		// Loop on all rows
		for (int rowId = 0; rowId < newSample.getRowCount(); rowId++)
		{
			Long database = newSample.getValueAsLong(rowId, database_pos);
			Long relation = newSample.getValueAsLong(rowId, relation_pos);

//System.out.println(">>>>>>>>>> CmPgLocks.localCalculation(): row="+rowId+", database="+database+", relation="+relation);
			if (database != null && database > 0)
			{
				DbmsObjectIdCache cache = DbmsObjectIdCache.getInstance();
				String dbname        = cache.getDBName(database);
				String schema_name   = null;
				String relation_name = null;
				
				if (relation != null && relation > 0)
				{
					try
					{
						ObjectInfo objInfo = cache.getByObjectId(database, relation);
						if (objInfo != null)
						{
							schema_name   = objInfo.getSchemaName();
							relation_name = objInfo.getObjectName();
						}
					}
					catch (TimeoutException timoutEx)
					{
						// nothing to do... 
					}
//if (relation_name == null)
//{
//	System.out.println("        -- ######################################################################### NOT FOUND RECORD: row="+rowId+", database="+database+", relation="+relation+" :::: dbname='"+dbname+"', schema_name='"+schema_name+"', relation_name='"+relation_name+"'.");
//}

//if (relation_name != null)
//{
//	System.out.println("        ++ ######################################################################### FOUND RECORD: dbname='"+dbname+"', schema_name='"+schema_name+"', relation_name='"+relation_name+"'.");
////	System.out.println(cache.debugPrintAllObject());
//}

				}

//System.out.println("        ++ CmPgLocks.localCalculation(): row="+rowId+", database="+database+", relation="+relation+", dbname='"+dbname+"', schema_name='"+schema_name+"', relation_name='"+relation_name+"'.");

				if (dbname        != null) newSample.setValueAt(dbname       , rowId, dbname_pos);
				if (schema_name   != null) newSample.setValueAt(schema_name  , rowId, schema_name_pos);
				if (relation_name != null) newSample.setValueAt(relation_name, rowId, relation_name_pos);
			}
			else
			{
				newSample.setValueAt(BUFFERS_NOT_USED_STR, rowId, dbname_pos);
				newSample.setValueAt(BUFFERS_NOT_USED_STR, rowId, schema_name_pos);
				newSample.setValueAt(BUFFERS_NOT_USED_STR, rowId, relation_name_pos);
			}
		}
	}
	public static final String BUFFERS_NOT_USED_STR = "-BUFFERS-NOT-USED-";

	@Override
	public Map<String, AggregationType> createAggregateColumns()
	{
//		DbmsVersionInfo versionInfo = getDbmsVersionInfo();
		
		HashMap<String, AggregationType> aggColumns = new HashMap<>(getColumnCount());

		AggregationType tmp;
		
		// Create the columns :::::::::::::::::::::::::::::::::::::::::::::::::::::: And ADD it to the return Map 
		tmp = new AggregationType("buffer_count_diff"  , AggregationType.Agg.SUM);   aggColumns.put(tmp.getColumnName(), tmp);
		tmp = new AggregationType("buffer_count"       , AggregationType.Agg.SUM);   aggColumns.put(tmp.getColumnName(), tmp);
		tmp = new AggregationType("buffer_dirty_count" , AggregationType.Agg.SUM);   aggColumns.put(tmp.getColumnName(), tmp);
		tmp = new AggregationType("buffer_mb"          , AggregationType.Agg.SUM);   aggColumns.put(tmp.getColumnName(), tmp);

		return aggColumns;
	}

	@Override
	public boolean isAggregateRowAppendEnabled()
	{
		return true;
	}

	@Override
	public Object calculateAggregateRow_getAggregatePkColumnDataProvider(CounterSample newSample, String colName, int c, int jdbcType, Object addValue)
	{
		if ("reldatabase"  .equalsIgnoreCase(colName)) return new Long(-1);
		if ("relfilenode"  .equalsIgnoreCase(colName)) return new Long(-1);
		
		return addValue;
	}

	@Override
	public Object calculateAggregateRow_nonAggregatedColumnDataProvider(CounterSample newSample, String colName, int c, int jdbcType, Object addValue)
	{
		if ("dbname"       .equalsIgnoreCase(colName)) return "_Total";
		if ("schema_name"  .equalsIgnoreCase(colName)) return "_Total";
		if ("relation_name".equalsIgnoreCase(colName)) return "_Total";
		if ("reldatabase"  .equalsIgnoreCase(colName)) return new Long(-1);
		if ("relfilenode"  .equalsIgnoreCase(colName)) return new Long(-1);
		
		return null;
	}

	@Override
	public List<String> getPkForVersion(DbxConnection conn, DbmsVersionInfo versionInfo)
	{
		List <String> pkCols = new LinkedList<String>();

		pkCols.add("reldatabase");
		pkCols.add("relfilenode");

		return pkCols;
	}

	@Override
	public String getSqlForVersion(DbxConnection conn, DbmsVersionInfo versionInfo)
	{
		String topRows = "";

		// TOP ROWS
		Configuration conf = Configuration.getCombinedConfiguration();
		if (conf.getBooleanProperty(PROPKEY_sample_topRows, DEFAULT_sample_topRows))
		{
			int rowCount = conf.getIntProperty(PROPKEY_sample_topRowsCount, DEFAULT_sample_topRowsCount);
			topRows = "LIMIT " + rowCount + " \n";

			_logger.warn("CM='"+getName()+"'. Limiting number of rows fetch. Adding phrase '" + topRows.trim() + "' at the end of the SQL Statement.");
		}

		String sql = ""
			    + "SELECT \n"
			    + "     CAST(null                as varchar(128)) AS dbname \n"
			    + "    ,CAST(null                as varchar(128)) AS schema_name \n"
			    + "    ,CAST(null                as varchar(128)) AS relation_name \n"
			    + "    ,reldatabase \n"
			    + "    ,relfilenode \n"
			    + "    ,COUNT(*)                                  AS buffer_count_diff \n"
			    + "    ,COUNT(*)                                  AS buffer_count \n"
			    + "    ,SUM( CASE WHEN isdirty THEN 1 ELSE 0 END) AS buffer_dirty_count \n"
			    + "    ,CAST(COUNT(*) / 128.0 as DECIMAL(12, 1))  AS buffer_mb \n"
			    + "FROM pg_buffercache \n"
			    + "WHERE reldatabase != 0 or reldatabase IS NULL \n" // Some records has  reldatabase = 0 and relfilenode != 0 ... so remove those but keep NULL (since it's "FREE" pages)
			    + "GROUP BY reldatabase, relfilenode \n"
			    + "ORDER BY buffer_count DESC \n"
			    + topRows
			    + "";

		return sql;
	}

	@Override
	public String getSqlInitForVersion(DbxConnection conn, DbmsVersionInfo versionInfo)
	{
		return "CREATE EXTENSION IF NOT EXISTS pg_buffercache";
	}
	
	private void addTrendGraphs()
	{
	}
}
