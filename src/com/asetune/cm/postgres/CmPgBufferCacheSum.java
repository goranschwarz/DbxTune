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

import java.util.LinkedList;
import java.util.List;

import javax.naming.NameNotFoundException;

import org.apache.log4j.Logger;

import com.asetune.ICounterController;
import com.asetune.IGuiController;
import com.asetune.central.pcs.CentralPersistReader;
import com.asetune.cm.CounterSample;
import com.asetune.cm.CounterSetTemplates;
import com.asetune.cm.CounterSetTemplates.Type;
import com.asetune.cm.CountersModel;
import com.asetune.config.dict.MonTablesDictionary;
import com.asetune.config.dict.MonTablesDictionaryManager;
import com.asetune.graph.TrendGraphDataPoint;
import com.asetune.graph.TrendGraphDataPoint.LabelType;
import com.asetune.gui.MainFrame;
import com.asetune.sql.conn.DbxConnection;
import com.asetune.sql.conn.info.DbmsVersionInfo;
import com.asetune.utils.NumberUtils;
import com.asetune.utils.Ver;

/**
 * @author Goran Schwarz (goran_schwarz@hotmail.com)
 */
public class CmPgBufferCacheSum
extends CountersModel
{
	private static Logger        _logger          = Logger.getLogger(CmPgBufferCacheSum.class);
	private static final long    serialVersionUID = 1L;

	public static final String   CM_NAME          = CmPgBufferCacheSum.class.getSimpleName();
	public static final String   SHORT_NAME       = "Buffer Cache Sum";
	public static final String   HTML_DESC        = 
		"<html>" +
		"Buffer Cache overall statistics. (from pg_buffercache_summary() function)<br>" +
		"<br>" +
		"For more details how the 'buffer manager' works, see: <a href='https://www.interdb.jp/pg/pgsql08/04.html'>https://www.interdb.jp/pg/pgsql08/04.html</a> <br>" +
		"<br>" + 
		"<b>Note:</b> Depends on extenstion: pg_buffercache, which is created on first sample (if not exists)<br>" +
		"<code>CREATE EXTENSION IF NOT EXISTS pg_buffercache</code>" +
		"</html>";

	public static final String   GROUP_NAME       = MainFrame.TCP_GROUP_CACHE;
	public static final String   GUI_ICON_FILE    = "images/"+CM_NAME+".png";

	public static final long     NEED_SRV_VERSION = Ver.ver(16);
	public static final long     NEED_CE_VERSION  = 0;

	public static final String[] MON_TABLES       = new String[] {"pg_buffercache_summary"};
	public static final String[] NEED_ROLES       = new String[] {};
//	public static final String[] NEED_CONFIG      = new String[] {"EXTENSION: pg_buffercache"};
	public static final String[] NEED_CONFIG      = new String[] {};

	public static final String[] PCT_COLUMNS      = new String[] {
			"buffers_used_pct", 
			"buffers_unused_pct", 
			"buffers_dirty_pct",
			"buffers_pinned_pct"
	};
	public static final String[] DIFF_COLUMNS     = new String[] {
			"buffers_used_diff",
			"buffers_unused_diff",
			"buffers_dirty_diff",
			"buffers_pinned_diff"
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

		return new CmPgBufferCacheSum(counterController, guiController);
	}

	public CmPgBufferCacheSum(ICounterController counterController, IGuiController guiController)
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
//		return new CmPgBufferCacheSumPanel(this);
//	}
	
	@Override
	public List<String> getPkForVersion(DbxConnection conn, DbmsVersionInfo versionInfo)
	{
		List <String> pkCols = new LinkedList<String>();

//		pkCols.add("reldatabase");
//		pkCols.add("relfilenode");

//		return null; // I can NEVER remember if I should have an empty list or return NULL if there is only 1 row
		return pkCols;
	}

	@Override
	public String getSqlForVersion(DbxConnection conn, DbmsVersionInfo versionInfo)
	{
		String sql = ""
			    + "SELECT \n"

//			    + "     (select CAST(CAST(setting as INT) / 128.0 as DECIMAL(12, 1)) from pg_settings where name = 'shared_buffers') AS cfg_shared_buffers_mb \n"
//			    + "    ,CAST(buffers_used   / 128.0 as DECIMAL(12, 1)) AS buffers_used_mb    \n"
//			    + "    ,CAST(buffers_unused / 128.0 as DECIMAL(12, 1)) AS buffers_unused_mb  \n"
//			    + "    ,CAST(buffers_dirty  / 128.0 as DECIMAL(12, 1)) AS buffers_dirty_mb   \n"
//			    + "    ,CAST(buffers_pinned / 128.0 as DECIMAL(12, 1)) AS buffers_pinned_mb  \n"
			    + "     CAST(-1 as DECIMAL(12, 1)) AS cfg_shared_buffers_mb \n"
			    + "    ,CAST(-1 as DECIMAL(12, 1)) AS buffers_used_mb    \n"
			    + "    ,CAST(-1 as DECIMAL( 9, 1)) AS buffers_used_pct   \n"
			    + "    ,CAST(-1 as DECIMAL(12, 1)) AS buffers_unused_mb  \n"
			    + "    ,CAST(-1 as DECIMAL( 9, 1)) AS buffers_unused_pct \n"
			    + "    ,CAST(-1 as DECIMAL(12, 1)) AS buffers_dirty_mb   \n"
			    + "    ,CAST(-1 as DECIMAL( 9, 3)) AS buffers_dirty_pct  \n"
			    + "    ,CAST(-1 as DECIMAL(12, 1)) AS buffers_pinned_mb  \n"
			    + "    ,CAST(-1 as DECIMAL( 9, 3)) AS buffers_pinned_pct  \n"
			    + "    ,usagecount_avg \n"

			    + "    ,(select CAST(setting as INT) from pg_settings where name = 'shared_buffers') AS cfg_shared_buffers \n"
			    + "    ,buffers_used   \n"
			    + "    ,buffers_unused \n"
			    + "    ,buffers_dirty  \n"
			    + "    ,buffers_pinned \n"

			    + "    ,buffers_used   AS buffers_used_diff   \n"
			    + "    ,buffers_unused AS buffers_unused_diff \n"
			    + "    ,buffers_dirty  AS buffers_dirty_diff  \n"
			    + "    ,buffers_pinned AS buffers_pinned_diff \n"

			    + "FROM pg_buffercache_summary() \n"
			    + "";

		return sql;
	}
	
	@Override
	public void localCalculation(CounterSample newSample)
	{
		if (newSample.getRowCount() != 1)
		{
			_logger.error("Problems in localCalculation(), newSample.getRowCount() should be 1. It is " + newSample.getRowCount() + ". Skipping localCalculation().");
			return;
		}

		int row = 0;

		// Get positions
		int pos__cfg_shared_buffers    = newSample.findColumn("cfg_shared_buffers");
		int pos__cfg_shared_buffers_mb = newSample.findColumn("cfg_shared_buffers_mb");
		int pos__buffers_used          = newSample.findColumn("buffers_used");
		int pos__buffers_used_mb       = newSample.findColumn("buffers_used_mb");
		int pos__buffers_used_pct      = newSample.findColumn("buffers_used_pct");
		int pos__buffers_unused        = newSample.findColumn("buffers_unused");
		int pos__buffers_unused_mb     = newSample.findColumn("buffers_unused_mb");
		int pos__buffers_unused_pct    = newSample.findColumn("buffers_unused_pct");
		int pos__buffers_dirty         = newSample.findColumn("buffers_dirty");
		int pos__buffers_dirty_mb      = newSample.findColumn("buffers_dirty_mb");
		int pos__buffers_dirty_pct     = newSample.findColumn("buffers_dirty_pct");
		int pos__buffers_pinned        = newSample.findColumn("buffers_pinned");
		int pos__buffers_pinned_mb     = newSample.findColumn("buffers_pinned_mb");
		int pos__buffers_pinned_pct    = newSample.findColumn("buffers_pinned_pct");

		// get data
		double cfg_shared_buffers = newSample.getValueAsDouble(row, pos__cfg_shared_buffers, -1D);
		double buffers_used       = newSample.getValueAsDouble(row, pos__buffers_used      , 0D);
		double buffers_unused     = newSample.getValueAsDouble(row, pos__buffers_unused    , 0D);
		double buffers_dirty      = newSample.getValueAsDouble(row, pos__buffers_dirty     , 0D);
		double buffers_pinned     = newSample.getValueAsDouble(row, pos__buffers_pinned    , 0D);

//System.out.println();
//System.out.println("POS: pos__cfg_shared_buffers=" + pos__cfg_shared_buffers + ", pos__cfg_shared_buffers_mb=" + pos__cfg_shared_buffers_mb);
//System.out.println("POS: pos__buffers_used      =" + pos__buffers_used       + ", pos__buffers_used_mb      =" + pos__buffers_used_mb   + ", pos__buffers_used_pct  =" + pos__buffers_used_pct);
//System.out.println("POS: pos__buffers_unused    =" + pos__buffers_unused     + ", pos__buffers_unused_mb    =" + pos__buffers_unused_mb + ", pos__buffers_unused_pct=" + pos__buffers_unused_pct);
//System.out.println("POS: pos__buffers_dirty     =" + pos__buffers_dirty      + ", pos__buffers_dirty_mb     =" + pos__buffers_dirty_mb  + ", pos__buffers_dirty_pct =" + pos__buffers_dirty_pct);
//System.out.println("POS: pos__buffers_pinned    =" + pos__buffers_pinned     + ", pos__buffers_pinned_mb    =" + pos__buffers_pinned_mb + ", pos__buffers_pinned_pct=" + pos__buffers_pinned_pct);
//System.out.println("RAW: cfg_shared_buffers=" + cfg_shared_buffers + ", buffers_used=" + buffers_used + ", buffers_unused=" + buffers_unused + ", buffers_dirty=" + buffers_dirty + ", buffers_pinned=" + buffers_pinned);
		if (cfg_shared_buffers == -1)
		{
			_logger.error("Problems reading column 'cfg_shared_buffers', skipping localCalculation.");
			return;
		}

		// calculate
		double cfg_shared_buffers_mb = NumberUtils.round( (cfg_shared_buffers / 128.0) , 1);
		
		double buffers_used_mb       = NumberUtils.round( (buffers_used   / 128.8) , 1);
		double buffers_unused_mb     = NumberUtils.round( (buffers_unused / 128.8) , 1);
		double buffers_dirty_mb      = NumberUtils.round( (buffers_dirty  / 128.8) , 1);
		double buffers_pinned_mb     = NumberUtils.round( (buffers_pinned / 128.8) , 1);
//System.out.println(" MB: cfg_shared_buffers_mb=" + cfg_shared_buffers_mb + ", buffers_used_mb=" + buffers_used_mb + ", buffers_unused_mb=" + buffers_unused_mb + ", buffers_dirty_mb=" + buffers_dirty_mb + ", buffers_pinned_mb=" + buffers_pinned_mb);

		double buffers_used_pct      = NumberUtils.round( (buffers_used   / cfg_shared_buffers * 100.0) , 1);
		double buffers_unused_pct    = NumberUtils.round( (buffers_unused / cfg_shared_buffers * 100.0) , 1);
		double buffers_dirty_pct     = NumberUtils.round( (buffers_dirty  / cfg_shared_buffers * 100.0) , 3);
		double buffers_pinned_pct    = NumberUtils.round( (buffers_pinned / cfg_shared_buffers * 100.0) , 3);
//System.out.println("PCT: buffers_used_pct=" + buffers_used_pct + ", buffers_unused_pct=" + buffers_unused_pct + ", buffers_dirty_pct=" + buffers_dirty_pct + ", buffers_pinned_pct=" + buffers_pinned_pct);
		
		newSample.setValueAt(cfg_shared_buffers_mb, row, pos__cfg_shared_buffers_mb);
		newSample.setValueAt(buffers_used_mb      , row, pos__buffers_used_mb);
		newSample.setValueAt(buffers_used_pct     , row, pos__buffers_used_pct);
		newSample.setValueAt(buffers_unused_mb    , row, pos__buffers_unused_mb);
		newSample.setValueAt(buffers_unused_pct   , row, pos__buffers_unused_pct);
		newSample.setValueAt(buffers_dirty_mb     , row, pos__buffers_dirty_mb);
		newSample.setValueAt(buffers_dirty_pct    , row, pos__buffers_dirty_pct);
		newSample.setValueAt(buffers_pinned_mb    , row, pos__buffers_pinned_mb);
		newSample.setValueAt(buffers_pinned_pct   , row, pos__buffers_pinned_pct);
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
	
	@Override
	public void addMonTableDictForVersion(DbxConnection conn, DbmsVersionInfo versionInfo)
	{
		String name = "pg_buffercache_summary";
		try 
		{
			MonTablesDictionary mtd = MonTablesDictionaryManager.getInstance();
			mtd.addTable(name, "describeme");

			mtd.addColumn(name, "cfg_shared_buffers",    "<html>Server Config value for 'shared_buffers'.</html>");
			mtd.addColumn(name, "buffers_used",          "<html>Number of used shared buffers</html>");
			mtd.addColumn(name, "buffers_unused",        "<html>Number of unused shared buffers</html>");
			mtd.addColumn(name, "buffers_dirty",         "<html>Number of dirty shared buffers</html>");
			mtd.addColumn(name, "buffers_pinned",        "<html>Number of pinned shared buffers</html>");
			mtd.addColumn(name, "usagecount_avg",        "<html>Average usage count of used shared buffers</html>");

			mtd.addColumn(name, "buffers_used_pct",      "<html>Percent of how much of the cache that is used</html>");
			mtd.addColumn(name, "buffers_unused_pct",    "<html>Percent of how much of the cache that is unused</html>");
			mtd.addColumn(name, "buffers_dirty_pct",     "<html>Percent of how much of the cache that is dirty</html>");
			
			mtd.addColumn(name, "buffers_used_diff",     "<html>Diff calculated: Number of used shared buffers</html>");
			mtd.addColumn(name, "buffers_unused_diff",   "<html>Diff calculated: Number of unused shared buffers</html>");
			mtd.addColumn(name, "buffers_dirty_diff",    "<html>Diff calculated: Number of dirty shared buffers</html>");
			mtd.addColumn(name, "buffers_pinned_diff",   "<html>Diff calculated: Number of pinned shared buffers</html>");
			
			mtd.addColumn(name, "cfg_shared_buffers_mb", "<html>Server Config value for 'shared_buffers' in MB.</html>");
			mtd.addColumn(name, "buffers_used_mb",       "<html>Number of used shared buffers in MB.</html>");
			mtd.addColumn(name, "buffers_unused_mb",     "<html>Number of unused shared buffers in MB.</html>");
			mtd.addColumn(name, "buffers_dirty_mb",      "<html>Number of dirty shared buffers in MB.</html>");
			mtd.addColumn(name, "buffers_pinned_mb",     "<html>Number of pinned shared buffers in MB.</html>");
			
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

	public static final String   GRAPH_NAME_BUFFER_USAGE     = "BufferUsage";
	public static final String   GRAPH_NAME_BUFFER_USAGE_PCT = "BufferUsagePct";
	public static final String   GRAPH_NAME_BUFFER_UNUSED    = "BufferUnused";
	public static final String   GRAPH_NAME_BUFFER_DIRTY     = "BufferDirty";

	private void addTrendGraphs()
	{
		addTrendGraph(GRAPH_NAME_BUFFER_USAGE,
			"Buffer Cache Usage in MB", 	                   // Menu CheckBox text
			"Buffer Cache Usage in MB ("+GROUP_NAME+"->"+SHORT_NAME+")", // Label 
			TrendGraphDataPoint.createGraphProps(TrendGraphDataPoint.Y_AXIS_SCALE_LABELS_MB, CentralPersistReader.SampleType.MAX_OVER_SAMPLES),
			new String[] {"buffers_used_mb", "buffers_unused_mb", "buffers_dirty_mb", "buffers_pinned_mb"}, 
			LabelType.Static, 
			TrendGraphDataPoint.Category.WAITS,
			false, // is Percent Graph
			false, // visible at start
			Ver.ver(16), // graph is valid from Server Version. 0 = All Versions; >0 = Valid from this version and above
			0);  // minimum height

		addTrendGraph(GRAPH_NAME_BUFFER_USAGE_PCT,
			"Buffer Cache Usage in Percent", 	                   // Menu CheckBox text
			"Buffer Cache Usage in Percent ("+GROUP_NAME+"->"+SHORT_NAME+")", // Label 
			TrendGraphDataPoint.createGraphProps(TrendGraphDataPoint.Y_AXIS_SCALE_LABELS_PERCENT, CentralPersistReader.SampleType.MAX_OVER_SAMPLES),
			new String[] {"buffers_used_pct"}, 
			LabelType.Static, 
			TrendGraphDataPoint.Category.WAITS,
			true, // is Percent Graph
			false, // visible at start
			Ver.ver(16), // graph is valid from Server Version. 0 = All Versions; >0 = Valid from this version and above
			0);  // minimum height

		addTrendGraph(GRAPH_NAME_BUFFER_UNUSED,
			"Buffer Cache Free/Unused in MB", 	                   // Menu CheckBox text
			"Buffer Cache Free/Unused in MB ("+GROUP_NAME+"->"+SHORT_NAME+")", // Label 
			TrendGraphDataPoint.createGraphProps(TrendGraphDataPoint.Y_AXIS_SCALE_LABELS_MB, CentralPersistReader.SampleType.MAX_OVER_SAMPLES),
			new String[] {"buffers_unused_mb"}, 
			LabelType.Static, 
			TrendGraphDataPoint.Category.WAITS,
			false, // is Percent Graph
			false, // visible at start
			Ver.ver(16), // graph is valid from Server Version. 0 = All Versions; >0 = Valid from this version and above
			0);  // minimum height

		addTrendGraph(GRAPH_NAME_BUFFER_DIRTY,
			"Buffer Cache Dirty in Pages and MB", 	                   // Menu CheckBox text
			"Buffer Cache Dirty in Pages and MB ("+GROUP_NAME+"->"+SHORT_NAME+")", // Label 
			TrendGraphDataPoint.createGraphProps(TrendGraphDataPoint.Y_AXIS_SCALE_LABELS_COUNT, CentralPersistReader.SampleType.MAX_OVER_SAMPLES),
			new String[] {"buffers_dirty_mb", "buffers_dirty_pages"}, 
			LabelType.Static, 
			TrendGraphDataPoint.Category.WAITS,
			false, // is Percent Graph
			false, // visible at start
			Ver.ver(16), // graph is valid from Server Version. 0 = All Versions; >0 = Valid from this version and above
			0);  // minimum height

	}

	@Override
	public void updateGraphData(TrendGraphDataPoint tgdp)
	{
		String graphName  = tgdp.getName();
		
		//--------------------------------------------------------
		if (GRAPH_NAME_BUFFER_USAGE.equals(graphName))
		{
			Double[] arr = new Double[4];

			arr[0] = this.getAbsValueAsDouble(0, "buffers_used_mb");
			arr[1] = this.getAbsValueAsDouble(0, "buffers_unused_mb");
			arr[2] = this.getAbsValueAsDouble(0, "buffers_dirty_mb");
			arr[3] = this.getAbsValueAsDouble(0, "buffers_pinned_mb");

			// Set the values
			tgdp.setDataPoint(this.getTimestamp(), arr);
		}

		//--------------------------------------------------------
		if (GRAPH_NAME_BUFFER_USAGE_PCT.equals(graphName))
		{
			Double[] arr = new Double[1];

			double buffers_used       = this.getAbsValueAsDouble(0, "buffers_used");
			double cfg_shared_buffers = this.getAbsValueAsDouble(0, "cfg_shared_buffers");

			double calc = buffers_used / cfg_shared_buffers * 100.0;

			arr[0] = NumberUtils.round(calc, 1);

			// Set the values
			tgdp.setDataPoint(this.getTimestamp(), arr);
		}

		//--------------------------------------------------------
		if (GRAPH_NAME_BUFFER_UNUSED.equals(graphName))
		{
			Double[] arr = new Double[1];

			arr[0] = this.getAbsValueAsDouble(0, "buffers_unused_mb");

			// Set the values
			tgdp.setDataPoint(this.getTimestamp(), arr);
		}

		//--------------------------------------------------------
		if (GRAPH_NAME_BUFFER_DIRTY.equals(graphName))
		{
			Double[] arr = new Double[2];

			arr[0] = this.getAbsValueAsDouble(0, "buffers_dirty_mb");
			arr[1] = this.getAbsValueAsDouble(0, "buffers_dirty");

			// Set the values
			tgdp.setDataPoint(this.getTimestamp(), arr);
		}

	}
}
