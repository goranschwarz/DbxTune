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
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import javax.naming.NameNotFoundException;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.dbxtune.ICounterController;
import com.dbxtune.IGuiController;
import com.dbxtune.cm.CounterSample;
import com.dbxtune.cm.CounterSetTemplates;
import com.dbxtune.cm.CounterSetTemplates.Type;
import com.dbxtune.cm.CountersModel;
import com.dbxtune.config.dict.MonTablesDictionary;
import com.dbxtune.config.dict.MonTablesDictionaryManager;
import com.dbxtune.gui.MainFrame;
import com.dbxtune.sql.conn.DbxConnection;
import com.dbxtune.sql.conn.info.DbmsVersionInfo;
import com.dbxtune.utils.Ver;

/**
 * @author Goran Schwarz (goran_schwarz@hotmail.com)
 */
public class CmPgBufferCacheACnt
extends CountersModel
{
	private static final Logger _logger = LogManager.getLogger(MethodHandles.lookup().lookupClass());
	private static final long    serialVersionUID = 1L;

	public static final String   CM_NAME          = CmPgBufferCacheACnt.class.getSimpleName();
	public static final String   SHORT_NAME       = "Buffer Cache Access";
	public static final String   HTML_DESC        = 
		"<html>" +
		"Buffer Cache Clock-sweep access count statistics. (from pg_buffercache_usage_counts() function)<br>" +
		"<br>" +
		"When 'usage_count' is at 0 a page can be <i>evicted</i> or replaced with another page (read from disk into the cache).<br>" + 
		"Higher 'usage_count' means data/pages will stay in cache longer.<br>" + 
		"<br>" + 
		"For more details see: <a href='https://www.interdb.jp/pg/pgsql08/04.html#844-page-replacement-algorithm-clock-sweep'>https://www.interdb.jp/pg/pgsql08/04.html#844-page-replacement-algorithm-clock-sweep</a> <br>" +
		"<br>" + 
		"<b>Note:</b> Depends on extenstion: pg_buffercache, which is created on first sample (if not exists)<br>" +
		"<code>CREATE EXTENSION IF NOT EXISTS pg_buffercache</code>" +
		"</html>";

	public static final String   GROUP_NAME       = MainFrame.TCP_GROUP_CACHE;
	public static final String   GUI_ICON_FILE    = "images/"+CM_NAME+".png";

	public static final long     NEED_SRV_VERSION = Ver.ver(16);
	public static final long     NEED_CE_VERSION  = 0;

	public static final String[] MON_TABLES       = new String[] {"pg_buffercache_usage_counts"};
	public static final String[] NEED_ROLES       = new String[] {};
//	public static final String[] NEED_CONFIG      = new String[] {"EXTENSION: pg_buffercache"};
	public static final String[] NEED_CONFIG      = new String[] {};

	public static final String[] PCT_COLUMNS      = new String[] {};
	public static final String[] DIFF_COLUMNS     = new String[] {
			"buffers_diff",
			"dirty_diff",
			"pinned_diff",
	};

	public static final boolean  NEGATIVE_DIFF_COUNTERS_TO_ZERO = false;
	public static final boolean  IS_SYSTEM_CM                   = true;
	public static final int      DEFAULT_POSTPONE_TIME          = 0;
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

		return new CmPgBufferCacheACnt(counterController, guiController);
	}

	public CmPgBufferCacheACnt(ICounterController counterController, IGuiController guiController)
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
//	@Override
//	protected TabularCntrPanel createGui()
//	{
//		return new CmPgBufferCacheACntPanel(this);
//	}
	
	@Override
	public List<String> getPkForVersion(DbxConnection conn, DbmsVersionInfo versionInfo)
	{
		List <String> pkCols = new LinkedList<String>();

		pkCols.add("usage_count");

		return pkCols;
	}

	@Override
	public String getSqlForVersion(DbxConnection conn, DbmsVersionInfo versionInfo)
	{
		String sql = ""
			    + "SELECT \n"

			    + "     usage_count \n"
			    + "    ,buffers \n"
			    + "    ,dirty  \n"
			    + "    ,pinned \n"

			    + "    ,buffers      AS buffers_diff   \n"
			    + "    ,dirty        AS dirty_diff \n"
			    + "    ,pinned       AS pinned_diff  \n"

			    + "    ,CAST(buffers / 128.0 as DECIMAL(12, 1)) AS buffers_mb \n"
			    + "    ,CAST(dirty   / 128.0 as DECIMAL(12, 1)) AS dirty_mb   \n"
			    + "    ,CAST(pinned  / 128.0 as DECIMAL(12, 1)) AS pinned_mb  \n"

			    + "FROM pg_buffercache_usage_counts() \n"
			    + "";

		return sql;
	}

	@Override
	public String getSqlInitForVersion(DbxConnection conn, DbmsVersionInfo versionInfo)
	{
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
	
	@Override
	public Map<String, AggregationType> createAggregateColumns()
	{
//		DbmsVersionInfo versionInfo = getDbmsVersionInfo();
		
		HashMap<String, AggregationType> aggColumns = new HashMap<>(getColumnCount());

		AggregationType tmp;
		
		// Create the columns :::::::::::::::::::::::::::::::::::::::::::::::::::::: And ADD it to the return Map 
		tmp = new AggregationType("buffers"       , AggregationType.Agg.SUM);   aggColumns.put(tmp.getColumnName(), tmp);
		tmp = new AggregationType("dirty"         , AggregationType.Agg.SUM);   aggColumns.put(tmp.getColumnName(), tmp);
		tmp = new AggregationType("pinned"        , AggregationType.Agg.SUM);   aggColumns.put(tmp.getColumnName(), tmp);

		tmp = new AggregationType("buffers_diff"  , AggregationType.Agg.SUM);   aggColumns.put(tmp.getColumnName(), tmp);
		tmp = new AggregationType("dirty_diff"    , AggregationType.Agg.SUM);   aggColumns.put(tmp.getColumnName(), tmp);
		tmp = new AggregationType("pinned_diff"   , AggregationType.Agg.SUM);   aggColumns.put(tmp.getColumnName(), tmp);

		tmp = new AggregationType("buffers_mb"    , AggregationType.Agg.SUM);   aggColumns.put(tmp.getColumnName(), tmp);
		tmp = new AggregationType("dirty_mb"      , AggregationType.Agg.SUM);   aggColumns.put(tmp.getColumnName(), tmp);
		tmp = new AggregationType("pinned_mb"     , AggregationType.Agg.SUM);   aggColumns.put(tmp.getColumnName(), tmp);

		return aggColumns;
	}

	@Override
	public Object calculateAggregateRow_getAggregatePkColumnDataProvider(CounterSample newSample, String colName, int c, int jdbcType, Object addValue)
	{
		if ("usage_count"  .equalsIgnoreCase(colName)) return Long.valueOf(-1);
		
		return addValue;
	}

	@Override
	public Object calculateAggregateRow_nonAggregatedColumnDataProvider(CounterSample newSample, String colName, int c, int jdbcType, Object addValue)
	{
		if ("usage_count"  .equalsIgnoreCase(colName)) return Long.valueOf(-1);
		
		return null;
	}

	@Override
	public void addMonTableDictForVersion(DbxConnection conn, DbmsVersionInfo versionInfo)
	{
		String name = "pg_buffercache_usage_counts";
		try 
		{
			MonTablesDictionary mtd = MonTablesDictionaryManager.getInstance();
			mtd.addTable(name, "describeme");

			mtd.addColumn(name, "usage_count",   "<html>A possible buffer usage count<br>"
			                                         + "When 'usage_count' is at 0 a page can be <i>evicted</i> or replaced with another page (read from disk into the cache).<br>"
			                                         + "Higher 'usage_count' means data/pages will stay in cache longer.<br>"
			                                         + "</html>");
			mtd.addColumn(name, "buffers",       "<html>Number of buffers with the usage count</html>");
			mtd.addColumn(name, "dirty",         "<html>Number of dirty buffers with the usage count</html>");
			mtd.addColumn(name, "pinned",        "<html>Number of pinned buffers with the usage count</html>");

			mtd.addColumn(name, "buffers_diff",  "<html>Diff Calc of Number of buffers with the usage count</html>");
			mtd.addColumn(name, "dirty_diff",    "<html>Diff Calc of Number of dirty buffers with the usage count</html>");
			mtd.addColumn(name, "pinned_diff",   "<html>Diff Calc of Number of pinned buffers with the usage count</html>");

			mtd.addColumn(name, "buffers_mb",    "<html>In MB the Number of buffers with the usage count</html>");
			mtd.addColumn(name, "dirty_mb",      "<html>In MB the Number of dirty buffers with the usage count</html>");
			mtd.addColumn(name, "pinned_mb",     "<html>In MB the Number of pinned buffers with the usage count</html>");
		}
		catch (NameNotFoundException e) 
		{
			_logger.warn("Problems in cm='" + CM_NAME + "', adding addMonTableDictForVersion. Caught: " + e); 
		//	System.out.println("Problems in cm='" + CM_NAME + "', adding addMonTableDictForVersion. Caught: " + e); 
		}
	}


	
	//---------------------------------------------------------------------------------
	//---------------------------------------------------------------------------------
	// Graph stuff
	//---------------------------------------------------------------------------------
	//---------------------------------------------------------------------------------

//	public static final String   GRAPH_NAME_BUFFER_XXX     = "Xxx";

	private void addTrendGraphs()
	{
	}

//	@Override
//	public void updateGraphData(TrendGraphDataPoint tgdp)
//	{
//		String graphName  = tgdp.getName();
//	}
}
