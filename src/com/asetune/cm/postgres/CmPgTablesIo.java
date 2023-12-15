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
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;

import org.apache.log4j.Logger;

import com.asetune.ICounterController;
import com.asetune.IGuiController;
import com.asetune.central.pcs.CentralPersistReader;
import com.asetune.cm.CmSettingsHelper;
import com.asetune.cm.CounterSample;
import com.asetune.cm.CounterSampleCatalogIteratorPostgres;
import com.asetune.cm.CounterSetTemplates;
import com.asetune.cm.CounterSetTemplates.Type;
import com.asetune.cm.CountersModel;
import com.asetune.cm.postgres.gui.CmPgTablesIoPanel;
import com.asetune.graph.TrendGraphDataPoint;
import com.asetune.graph.TrendGraphDataPoint.LabelType;
import com.asetune.gui.MainFrame;
import com.asetune.gui.TabularCntrPanel;
import com.asetune.sql.conn.DbxConnection;
import com.asetune.sql.conn.info.DbmsVersionInfo;
import com.asetune.utils.Configuration;
import com.asetune.utils.MathUtils;

/**
 * @author Goran Schwarz (goran_schwarz@hotmail.com)
 */
public class CmPgTablesIo
extends CountersModel
{
	private static Logger        _logger          = Logger.getLogger(CmPgTablesIo.class);
	private static final long    serialVersionUID = 1L;

	public static final String   CM_NAME          = CmPgTablesIo.class.getSimpleName();
	public static final String   SHORT_NAME       = "Table IO Info";
	public static final String   HTML_DESC        = 
		"<html>" +
		"One row for each table in the current database, showing statistics about accesses to that specific table." +
		"</html>";

	public static final String   GROUP_NAME       = MainFrame.TCP_GROUP_OBJECT_ACCESS;
	public static final String   GUI_ICON_FILE    = "images/"+CM_NAME+".png";

	public static final long     NEED_SRV_VERSION = 0;
	public static final long     NEED_CE_VERSION  = 0;

	public static final String[] MON_TABLES       = new String[] {"pg_statio_user_tables"};
	public static final String[] NEED_ROLES       = new String[] {};
	public static final String[] NEED_CONFIG      = new String[] {};

	public static final String[] PCT_COLUMNS      = new String[] {
			"cache_hit_pct"
	};
	public static final String[] DIFF_COLUMNS     = new String[] {
			"total_reads",
			"all_blks_read",
			"all_blks_hit",
			
			"heap_blks_read",
			"heap_blks_hit",
			"idx_blks_read",
			"idx_blks_hit",
			"toast_blks_read",
			"toast_blks_hit",
			"tidx_blks_read",
			"tidx_blks_hit"
	};
	
//	1> select * from pg_catalog.pg_statio_user_tables
//	RS> Col# Label           JDBC Type Name         Guessed DBMS type Source Table         
//	RS> ---- --------------- ---------------------- ----------------- ---------------------
//	RS> 1    relid           java.sql.Types.BIGINT  oid               pg_statio_user_tables
//	RS> 2    schemaname      java.sql.Types.VARCHAR name(2147483647)  pg_statio_user_tables
//	RS> 3    relname         java.sql.Types.VARCHAR name(2147483647)  pg_statio_user_tables
//	RS> 4    heap_blks_read  java.sql.Types.BIGINT  int8              pg_statio_user_tables
//	RS> 5    heap_blks_hit   java.sql.Types.BIGINT  int8              pg_statio_user_tables
//	RS> 6    idx_blks_read   java.sql.Types.BIGINT  int8              pg_statio_user_tables
//	RS> 7    idx_blks_hit    java.sql.Types.BIGINT  int8              pg_statio_user_tables
//	RS> 8    toast_blks_read java.sql.Types.BIGINT  int8              pg_statio_user_tables
//	RS> 9    toast_blks_hit  java.sql.Types.BIGINT  int8              pg_statio_user_tables
//	RS> 10   tidx_blks_read  java.sql.Types.BIGINT  int8              pg_statio_user_tables
//	RS> 11   tidx_blks_hit   java.sql.Types.BIGINT  int8              pg_statio_user_tables
	
	public static final boolean  NEGATIVE_DIFF_COUNTERS_TO_ZERO = true;
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

		return new CmPgTablesIo(counterController, guiController);
	}

	public CmPgTablesIo(ICounterController counterController, IGuiController guiController)
	{
		super(counterController,
				CM_NAME, GROUP_NAME, /*sql*/null, /*pkList*/null, 
				DIFF_COLUMNS, PCT_COLUMNS, MON_TABLES, 
				NEED_ROLES, NEED_CONFIG, NEED_SRV_VERSION, NEED_CE_VERSION, 
				NEGATIVE_DIFF_COUNTERS_TO_ZERO, IS_SYSTEM_CM, DEFAULT_POSTPONE_TIME);

		setDisplayName(SHORT_NAME);
		setDescription(HTML_DESC);

		setIconFile(GUI_ICON_FILE);

		setCounterController(counterController);
		setGuiController(guiController);
		
		addTrendGraphs();
		
		CounterSetTemplates.register(this);
	}


	//------------------------------------------------------------
	// Implementation
	//------------------------------------------------------------
	private static final String  PROP_PREFIX                          = CM_NAME;

	public static final String  PROPKEY_sample_systemTables           = PROP_PREFIX + ".sample.systemTables";
	public static final boolean DEFAULT_sample_systemTables           = false;

	public static final String GRAPH_NAME_CACHE_HIT                   = "CacheHit";
	public static final String GRAPH_NAME_CACHE_READS                 = "CacheReads";
	
	@Override
	protected void registerDefaultValues()
	{
		super.registerDefaultValues();

		Configuration.registerDefaultValue(PROPKEY_sample_systemTables,           DEFAULT_sample_systemTables);
	}
	
	/** Used by the: Create 'Offline Session' Wizard */
	@Override
	public List<CmSettingsHelper> getLocalSettings()
	{
		Configuration conf = Configuration.getCombinedConfiguration();
		List<CmSettingsHelper> list = new ArrayList<>();
		
		list.add(new CmSettingsHelper("Sample System Tables", PROPKEY_sample_systemTables , Boolean.class, conf.getBooleanProperty(PROPKEY_sample_systemTables  , DEFAULT_sample_systemTables  ), DEFAULT_sample_systemTables, "Sample System Tables" ));

		return list;
	}


	private void addTrendGraphs()
	{
		addTrendGraph(GRAPH_NAME_CACHE_HIT,
			"Tables Total Cache Hit Percent", 	                // Menu CheckBox text
			"Tables Total Cache Hit in Percent ("+SHORT_NAME+")", // Graph Label 
			TrendGraphDataPoint.createGraphProps(TrendGraphDataPoint.Y_AXIS_SCALE_LABELS_PERCENT, CentralPersistReader.SampleType.MAX_OVER_SAMPLES),
			new String[] {"Data Pages Cache Hit"}, 
			LabelType.Static, 
			TrendGraphDataPoint.Category.CPU,
			true, // is Percent Graph
			true, // visible at start
			0,     // graph is valid from Server Version. 0 = All Versions; >0 = Valid from this version and above 
			-1);   // minimum height

		addTrendGraph(GRAPH_NAME_CACHE_READS,
			"Tables Total Cache Reads", 	                // Menu CheckBox text
			"Tables Total Cache Reads per Second ("+SHORT_NAME+")", // Graph Label 
			TrendGraphDataPoint.createGraphProps(TrendGraphDataPoint.Y_AXIS_SCALE_LABELS_NORMAL, CentralPersistReader.SampleType.MAX_OVER_SAMPLES),
			new String[] {"total_reads", "Reads From Cache", "Reads From Disk"}, 
			LabelType.Static, 
			TrendGraphDataPoint.Category.SRV_CONFIG,
			false, // is Percent Graph
			true,  // visible at start
			0,     // graph is valid from Server Version. 0 = All Versions; >0 = Valid from this version and above 
			-1);   // minimum height
	}

	@Override
	public void updateGraphData(TrendGraphDataPoint tgdp)
	{
		//---------------------------------
		// GRAPH:
		//---------------------------------
		if (GRAPH_NAME_CACHE_HIT.equals(tgdp.getName()))
		{
			Double[] arr = new Double[1];

			Double total_reads   = this.getDiffValueSum("total_reads");
		//	Double all_blks_read = this.getDiffValueSum("all_blks_read");
			Double all_blks_hit  = this.getDiffValueSum("all_blks_hit");
			
			if (total_reads != null && all_blks_hit != null)
			{
				arr[0] = 0d;
				if (total_reads > 0)
					arr[0] = 100.0 * (all_blks_hit / total_reads);

				// Set the values
				tgdp.setDataPoint(this.getTimestamp(), arr);
			}
		}

		//---------------------------------
		// GRAPH:
		//---------------------------------
		if (GRAPH_NAME_CACHE_READS.equals(tgdp.getName()))
		{
			Double[] arr = new Double[3];

			arr[0] = this.getDiffValueSum("total_reads");
			arr[1] = this.getDiffValueSum("all_blks_hit");
			arr[2] = this.getDiffValueSum("all_blks_read");

			// Set the values
			tgdp.setDataPoint(this.getTimestamp(), arr);
		}
	}

	/** 
	 * Compute the 'cache_hit_pct' for DIFF values
	 */
	@Override
	public void localCalculation(CounterSample prevSample, CounterSample newSample, CounterSample diffData)
	{
		int cache_hit_pct_pos = -1;
		int total_reads_pos   = -1;
		int all_blks_hit_pos  = -1;

		// Find column Id's
		List<String> colNames = diffData.getColNames();
		if (colNames == null)
			return;

		for (int colId=0; colId < colNames.size(); colId++) 
		{
			String colName = colNames.get(colId);
			if      (colName.equals("cache_hit_pct")) cache_hit_pct_pos = colId;
			else if (colName.equals("total_reads"))   total_reads_pos   = colId;
			else if (colName.equals("all_blks_hit"))  all_blks_hit_pos  = colId;
		}

		if (cache_hit_pct_pos == -1 || total_reads_pos == -1 || all_blks_hit_pos == -1)
		{
			_logger.warn("localCalculation() in CM='" + getName() + "', could not find all desired column: cache_hit_pct_pos=" + cache_hit_pct_pos + ", total_reads_pos=" + total_reads_pos + ", all_blks_hit_pos=" + all_blks_hit_pos + ".");
			return;
		}
		
		// Loop on all diffData rows
		for (int rowId = 0; rowId < diffData.getRowCount(); rowId++)
		{
//			long total_reads  = ((Number)diffData.getValueAt(rowId, total_reads_pos )).longValue();
//			long all_blks_hit = ((Number)diffData.getValueAt(rowId, all_blks_hit_pos)).longValue();
			long total_reads  = diffData.getValueAsLong(rowId, total_reads_pos, 0L);
			long all_blks_hit = diffData.getValueAsLong(rowId, all_blks_hit_pos, 0L);

//			Double cache_hit_pct = 0d;
			Double cache_hit_pct = null;

			if (total_reads > 0)
			{
				cache_hit_pct = 100.0 * ((all_blks_hit*1.0) / (total_reads*1.0));
//System.out.println("localCalculation(): rowId="+rowId+", cache_hit_pct="+cache_hit_pct+", all_blks_hit="+all_blks_hit+", total_reads="+total_reads+", relname="+diffData.getValueAt(rowId, 2));
				cache_hit_pct = MathUtils.round(cache_hit_pct, 2);
			}
			diffData.setValueAt(cache_hit_pct, rowId, cache_hit_pct_pos);
		}
	}

	@Override
	protected TabularCntrPanel createGui()
	{
		return new CmPgTablesIoPanel(this);
	}


//	@Override
//	protected TabularCntrPanel createGui()
//	{
//		return new CmActiveStatementsPanel(this);
//	}
	
	@Override
	public List<String> getPkForVersion(DbxConnection conn, DbmsVersionInfo versionInfo)
	{
		List <String> pkCols = new LinkedList<String>();

		pkCols.add("dbname");
		pkCols.add("schemaname");
		pkCols.add("relname");

		return pkCols;
	}

	/**
	 * Create a special CounterSample, that will iterate over all databases that we will interrogate
	 */
	@Override
	public CounterSample createCounterSample(String name, boolean negativeDiffCountersToZero, String[] diffColumns, CounterSample prevSample)
	{
		List<String> fallbackDbList = Arrays.asList( new String[]{"postgres"} );
		return new CounterSampleCatalogIteratorPostgres(name, negativeDiffCountersToZero, diffColumns, prevSample, fallbackDbList);
	}

	@Override
	public String getSqlForVersion(DbxConnection conn, DbmsVersionInfo versionInfo)
	{
		String tabName = "pg_catalog.pg_statio_user_tables";

		// Sample System Tables
		if (Configuration.getCombinedConfiguration().getBooleanProperty(PROPKEY_sample_systemTables, DEFAULT_sample_systemTables))
		{
			tabName = "pg_catalog.pg_statio_all_tables";
		}

		String all_read   = "(heap_blks_read + idx_blks_read + toast_blks_read + tidx_blks_read)";
		String all_hit    = "(heap_blks_hit  + idx_blks_hit  + toast_blks_hit  + tidx_blks_hit )";
		String total_read = "(" + all_read + " + " + all_hit + ")";
		return ""
				+ "select \n"
				+ "     current_database() AS dbname \n"
				+ "    ,schemaname \n"
				+ "    ,relname \n"

//			    ,CAST( 100.0 * (sum("heap_blks_hit") + sum("idx_blks_hit") + sum("toast_blks_hit") + sum("tidx_blks_hit")) / nullif((sum("heap_blks_hit") + sum("idx_blks_hit") + sum("toast_blks_hit") + sum("tidx_blks_hit")) + (sum("heap_blks_read") + sum("idx_blks_read") + sum("toast_blks_read") + sum("tidx_blks_read")), 0) AS DECIMAL(5,1) ) as "cache_hit_pct" 
				
				+ "    ," + total_read + "    AS total_reads \n"
				+ "    ,CAST( 100.0 * ((" + all_hit + "*1.0) / nullif((" + total_read + "*1.0), 0)) AS DECIMAL(6,2) )    AS cache_hit_pct \n"
				+ "    ," + all_read + "    AS all_blks_read \n"
				+ "    ," + all_hit  + "    AS all_blks_hit \n"

				+ "    ,heap_blks_read \n"
				+ "    ,heap_blks_hit \n"

				+ "    ,idx_blks_read \n"
				+ "    ,idx_blks_hit \n"

				+ "    ,toast_blks_read \n"
				+ "    ,toast_blks_hit \n"

				+ "    ,tidx_blks_read \n"
				+ "    ,tidx_blks_hit \n"

				+ "    ,relid \n"
				+ "from " + tabName;
	}
}
