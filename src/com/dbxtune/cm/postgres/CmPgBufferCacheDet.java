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
package com.dbxtune.cm.postgres;

import java.lang.invoke.MethodHandles;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeoutException;

import javax.naming.NameNotFoundException;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.dbxtune.ICounterController;
import com.dbxtune.IGuiController;
import com.dbxtune.cache.DbmsObjectIdCache;
import com.dbxtune.cache.DbmsObjectIdCache.ObjectInfo;
import com.dbxtune.cm.CmSettingsHelper;
import com.dbxtune.cm.CounterSample;
import com.dbxtune.cm.CounterSetTemplates;
import com.dbxtune.cm.CounterSetTemplates.Type;
import com.dbxtune.cm.CountersModel;
import com.dbxtune.cm.postgres.gui.CmPgBufferCacheDetPanel;
import com.dbxtune.config.dict.MonTablesDictionary;
import com.dbxtune.config.dict.MonTablesDictionaryManager;
import com.dbxtune.gui.MainFrame;
import com.dbxtune.gui.TabularCntrPanel;
import com.dbxtune.sql.conn.DbxConnection;
import com.dbxtune.sql.conn.info.DbmsVersionInfo;
import com.dbxtune.utils.Configuration;

/**
 * @author Goran Schwarz (goran_schwarz@hotmail.com)
 */
public class CmPgBufferCacheDet
extends CountersModel
{
	private static final Logger _logger = LogManager.getLogger(MethodHandles.lookup().lookupClass());
	private static final long    serialVersionUID = 1L;

	public static final String   CM_NAME          = CmPgBufferCacheDet.class.getSimpleName();
	public static final String   SHORT_NAME       = "Buffer Cache Details";
	public static final String   HTML_DESC        = 
		"<html>" +
		"Table names and size in the Buffer Cache. (from pg_buffercache)<br>" +
		"<br>" +
		"For more details how the 'buffer manager' works, see: <a href='https://www.interdb.jp/pg/pgsql08/04.html'>https://www.interdb.jp/pg/pgsql08/04.html</a> <br>" +
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

		return new CmPgBufferCacheDet(counterController, guiController);
	}

	public CmPgBufferCacheDet(ICounterController counterController, IGuiController guiController)
	{
		super(counterController, guiController,
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
		return new CmPgBufferCacheDetPanel(this);
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
				
				// Skip template
				boolean isTemplateDb = false;
				if (dbname != null && dbname.startsWith("template"))
					isTemplateDb = true;

				if (relation != null && relation > 0 && !isTemplateDb)
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
		tmp = new AggregationType("avg_usagecount"     , AggregationType.Agg.AVG);   aggColumns.put(tmp.getColumnName(), tmp);
		tmp = new AggregationType("buffer_mb"          , AggregationType.Agg.SUM);   aggColumns.put(tmp.getColumnName(), tmp);

		return aggColumns;
	}

	@Override
	public Object calculateAggregateRow_getAggregatePkColumnDataProvider(CounterSample newSample, String colName, int c, int jdbcType, Object addValue)
	{
		if ("reldatabase"  .equalsIgnoreCase(colName)) return Long.valueOf(-1);
		if ("relfilenode"  .equalsIgnoreCase(colName)) return Long.valueOf(-1);
		
		return addValue;
	}

	@Override
	public Object calculateAggregateRow_nonAggregatedColumnDataProvider(CounterSample newSample, String colName, int c, int jdbcType, Object addValue)
	{
		if ("dbname"       .equalsIgnoreCase(colName)) return "_Total";
		if ("schema_name"  .equalsIgnoreCase(colName)) return "_Total";
		if ("relation_name".equalsIgnoreCase(colName)) return "_Total";
		if ("reldatabase"  .equalsIgnoreCase(colName)) return Long.valueOf(-1);
		if ("relfilenode"  .equalsIgnoreCase(colName)) return Long.valueOf(-1);
		
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
			    + "    ,CAST(AVG(usagecount) as DECIMAL(12, 1))   AS avg_usagecount \n"
			    + "    ,CAST(COUNT(*) / 128.0 as DECIMAL(12, 1))  AS buffer_mb \n"
			    + "FROM pg_buffercache \n"
			    + "WHERE reldatabase != 0 or reldatabase IS NULL \n" // Some records has  reldatabase = 0 and relfilenode != 0 ... so remove those but keep NULL (since it's "FREE" pages)
//			    + "WHERE reldatabase not in(0,1,4) or reldatabase IS NULL \n" // Some records has reldatabase = 0 and relfilenode != 0 ... so remove those but keep NULL (since it's "FREE" pages) and also dbid=1,4 (1='template1', 4='template0') 
			    + "GROUP BY reldatabase, relfilenode \n"
			    + "ORDER BY buffer_count DESC \n"
			    + topRows
			    + "";

		return sql;
	}

	@Override
	public String getSqlInitForVersion(DbxConnection conn, DbmsVersionInfo versionInfo)
	{
//		return "CREATE EXTENSION IF NOT EXISTS pg_buffercache";
		String sql = ""
			    + "DO $$ BEGIN \n"
			    + "    IF NOT EXISTS (SELECT * FROM pg_extension WHERE extname = 'pg_buffercache') \n"
			    + "    THEN \n"
			    + "        CREATE EXTENSION pg_buffercache; \n"
			    + "    END IF; \n"
			    + "END; $$; \n"
			    + "";
		return sql;
	}
	
	private void addTrendGraphs()
	{
	}

	@Override
	public void addMonTableDictForVersion(DbxConnection conn, DbmsVersionInfo versionInfo)
	{
		String name = "pg_buffercache";
		try 
		{
			MonTablesDictionary mtd = MonTablesDictionaryManager.getInstance();
			mtd.addTable(name, "describeme");
			
			mtd.addColumn(name, "bufferid",           "<html>ID, in the range 1..shared_buffers</html>");
			mtd.addColumn(name, "relfilenode",        "<html>Filenode number of the relation</html>");
			mtd.addColumn(name, "reltablespace",      "<html>Tablespace OID of the relation</html>");
			mtd.addColumn(name, "reldatabase",        "<html>Database OID of the relation</html>");
			mtd.addColumn(name, "relforknumber",      "<html>Fork number within the relation; see common/relpath.h</html>");
			mtd.addColumn(name, "relblocknumber",     "<html>Page number within the relation</html>");
			mtd.addColumn(name, "isdirty",            "<html>Is the page dirty?</html>");
			mtd.addColumn(name, "usagecount",         "<html>Clock-sweep access count</html>");
			mtd.addColumn(name, "pinning_backends",   "<html>Number of backends pinning this buffer</html>");
			
			mtd.addColumn(name, "dbname",             "<html>Name of the Database</html>");
			mtd.addColumn(name, "schema_name",        "<html>Name of the Schema</html>");
			mtd.addColumn(name, "relation_name",      "<html>Name of the Table</html>");
			mtd.addColumn(name, "buffer_count_diff",  "<html>How many pages/buffers does this table/relation have in memory (diff calculated on previous sample)</html>");
			mtd.addColumn(name, "buffer_count",       "<html>How many pages/buffers does this table/relation have in memory</html>");
			mtd.addColumn(name, "buffer_dirty_count", "<html>How many pages/buffers of this table/relation has changed on not yet been flushed by the checkpoint handler.</html>");
			mtd.addColumn(name, "avg_usagecount",     "<html>Average <i>Clock-sweep access count</i> for this table.</html>");
			mtd.addColumn(name, "buffer_mb",          "<html>In MB, how many pages/buffers does this table have in memory</html>");
		}
		catch (NameNotFoundException e) 
		{
			_logger.warn("Problems in cm='" + CM_NAME + "', adding addMonTableDictForVersion. Caught: " + e); 
		//	System.out.println("Problems in cm='" + CM_NAME + "', adding addMonTableDictForVersion. Caught: " + e); 
		}
	}


}
