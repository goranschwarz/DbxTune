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
package com.asetune.cm.iq;

import java.sql.Connection;
import java.util.LinkedList;
import java.util.List;

import javax.naming.NameNotFoundException;

import com.asetune.ICounterController;
import com.asetune.IGuiController;
import com.asetune.cm.CounterSetTemplates;
import com.asetune.cm.CounterSetTemplates.Type;
import com.asetune.cm.CountersModel;
import com.asetune.config.dict.MonTablesDictionary;
import com.asetune.config.dict.MonTablesDictionaryManager;
import com.asetune.graph.TrendGraphDataPoint;
import com.asetune.graph.TrendGraphDataPoint.LabelType;
import com.asetune.gui.MainFrame;

/**
 * @author Goran Schwarz (goran_schwarz@hotmail.com)
 */

/**
 * 
 * @author I063869
 *
 */
public class CmIqCache2
extends CountersModel
{
//	private static Logger        _logger          = Logger.getLogger(CmAdminWhoSqm.class);
	private static final long    serialVersionUID = 1L;

	public static final String   CM_NAME          = CmIqCache2.class.getSimpleName();
	public static final String   SHORT_NAME       = "caches";
	public static final String   HTML_DESC        = 
		"<html>" +
		"<h4>cache - custom</h4>" + 
		"based on values from sa_eng_properties and sp_iq_statistics." +
		"</html>";

	public static final String   GROUP_NAME       = MainFrame.TCP_GROUP_CACHE;
	public static final String   GUI_ICON_FILE    = "images/"+CM_NAME+".png";

	public static final long     NEED_SRV_VERSION = 0;
	public static final long     NEED_CE_VERSION  = 0;

	public static final String[] MON_TABLES       = new String[] {"iq_cache_custom"};
	public static final String[] NEED_ROLES       = new String[] {};
	public static final String[] NEED_CONFIG      = new String[] {};

	public static final String[] PCT_COLUMNS      = new String[] {};
	public static final String[] DIFF_COLUMNS     = new String[] {
		"PropValue"
	};

	public static final boolean  NEGATIVE_DIFF_COUNTERS_TO_ZERO = true;
	public static final boolean  IS_SYSTEM_CM                   = true;
	public static final int      DEFAULT_POSTPONE_TIME          = 0;
	public static final int      DEFAULT_QUERY_TIMEOUT          = 60; //CountersModel.DEFAULT_sqlQueryTimeout;
	
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

		return new CmIqCache2(counterController, guiController);
	}

	public CmIqCache2(ICounterController counterController, IGuiController guiController)
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
		setBackgroundDataPollingEnabled(false, false);
		
		setCounterController(counterController);
		setGuiController(guiController);
		
		addTrendGraphs();
		
		CounterSetTemplates.register(this);
	}


	//------------------------------------------------------------
	// Implementation
	//------------------------------------------------------------
	// NOTE: storage table name will be CmName_GraphName, so try to keep the name short
	public static final String GRAPH_NAME_CACHE2 = "MemoryGraph";  

	private void addTrendGraphs()
	{
//		String[] labels_memo = new String[] { "MemoryAllocated", "MemoryMaxAllocated", "MainCacheCurrentSize", "TempCacheCurrentSize", "CurrentCacheSize", "MaxCacheSize", "MinCacheSize"} ;
//		
//		addTrendGraphData(GRAPH_NAME_CACHE2, new TrendGraphDataPoint(GRAPH_NAME_CACHE2, labels_memo, LabelType.Static));

		addTrendGraph(GRAPH_NAME_CACHE2,
			"Memory overview",                     // Menu CheckBox text
			"Memory overview ("+GROUP_NAME+"->"+SHORT_NAME+")", // Label 
			TrendGraphDataPoint.Y_AXIS_SCALE_LABELS_MB,
			new String[] { "MemoryAllocated", "MemoryMaxAllocated", "MainCacheCurrentSize", "TempCacheCurrentSize", "CurrentCacheSize", "MaxCacheSize", "MinCacheSize"},
			LabelType.Static,
			TrendGraphDataPoint.Category.CACHE,
			false, // is Percent Graph
			false, // visible at start
			0,     // graph is valid from Server Version. 0 = All Versions; >0 = Valid from this version and above 
			-1);   // minimum height

//		// if GUI
//		if (getGuiController() != null && getGuiController().hasGUI())
//		{
//			// GRAPH
//			TrendGraph tg = null;
//			tg = new TrendGraph(GRAPH_NAME_CACHE2,
//				"Memory overview",                     // Menu CheckBox text
//				"Memory overview ("+GROUP_NAME+"->"+SHORT_NAME+")", // Label 
//				labels_memo, 
//				false, // is Percent Graph
//				this, 
//				false, // visible at start
//				0,     // graph is valid from Server Version. 0 = All Versions; >0 = Valid from this version and above 
//				-1);   // minimum height
//			addTrendGraph(tg.getName(), tg, true);
//		}
	}
	
	@Override
	public void updateGraphData(TrendGraphDataPoint tgdp)
	{	
		if (GRAPH_NAME_CACHE2.equals(tgdp.getName()))
		{	
			Double[] arr = new Double[7];
			arr[0] = this.getAbsValueAsDouble("MemoryAllocated" ,     "PropValue");
			arr[1] = this.getAbsValueAsDouble("MemoryMaxAllocated",   "PropValue");
			arr[2] = this.getAbsValueAsDouble("MainCacheCurrentSize", "PropValue");
			arr[3] = this.getAbsValueAsDouble("TempCacheCurrentSize", "PropValue");
			arr[4] = this.getAbsValueAsDouble("CurrentCacheSize",     "PropValue");
			arr[5] = this.getAbsValueAsDouble("MaxCacheSize",         "PropValue");
			arr[6] = this.getAbsValueAsDouble("MinCacheSize",         "PropValue");
			
			// Set the values
			tgdp.setDataPoint(this.getTimestamp(), arr);
//			tgdp.setDate(this.getTimestamp());
//			tgdp.setData(arr);
		}
	}
	

	@Override
	public String[] getDependsOnConfigForVersion(Connection conn, long srvVersion, boolean isClusterEnabled)
	{
		return NEED_CONFIG;
	}

	@Override
	public void addMonTableDictForVersion(Connection conn, long srvVersion, boolean isClusterEnabled)
	{
		try 
		{
			MonTablesDictionary mtd = MonTablesDictionaryManager.getInstance();
			mtd.addTable("iq_cache_custom",  "Various caches information.");

			mtd.addColumn("iq_cache_custom", "CacheType",       "<html>Catalog or IQ</html>");
			mtd.addColumn("iq_cache_custom", "PropNum",         "<html>Number (PropNum in sa_eng_properties and stat_num in sp_iqstatistics)</html>");
			mtd.addColumn("iq_cache_custom", "PropName",        "<html>Name</html>");
			mtd.addColumn("iq_cache_custom", "PropDescription", "<html>Description</html>");
			mtd.addColumn("iq_cache_custom", "PropValue",       "<html>Value</html>");
			mtd.addColumn("iq_cache_custom", "PropUnit",        "<html>Unit of measure</html>");
		}
		catch (NameNotFoundException e) {/*ignore*/}
	}

	@Override
	public List<String> getPkForVersion(Connection conn, long srvVersion, boolean isClusterEnabled)
	{
		List <String> pkCols = new LinkedList<String>();
		
		pkCols.add("PropName");
		
		return pkCols;
	}

	@Override
	public String getSqlForVersion(Connection conn, long srvVersion, boolean isClusterEnabled)
	{
		String sql = 
			"select  \n" +
			"    'Catalog' as CacheType,  \n" +
			"    PropNum,  \n" +
			"    PropName,  \n" +
			"    cast(replace(PropDescription, 'kilobytes', 'megabytes') as varchar(255)) as PropDescription,  \n" +
			"    CASE PropName WHEN 'CachePanics' THEN cast(Value as decimal(20,2)) ELSE cast(Value/1024 as decimal(10,2)) END as PropValue,  \n" +
			"    CASE PropName WHEN 'CachePanics' THEN ''                           ELSE 'kb'                              END as PropUnit  \n" +
			"from sa_eng_properties()  \n" +
			"where PropName in ('CurrentCacheSize', 'MaxCacheSize', 'MinCacheSize', 'PageSize', 'CachePanics') \n" +
			"union all  \n" +
			"select  \n" +
			"    'IQ'       as CacheType,  \n" +
			"    stat_num   as PropNum,  \n" +
			"    stat_name  as PropName,  \n" +
			"    cast(stat_desc as varchar(255))   as PropDescription,  \n" +
			"    cast(stat_value as decimal(20,2)) as PropValue,  \n" +
			"    stat_unit  as PropUnit  \n" +
			"from sp_iqstatistics()  \n" +
			"where stat_name in (  \n" +
			"    'MainCacheCurrentSize', 'MainCacheFinds', 'MainCacheHits', 'MainCachePagesPinned', 'MainCachePagesPinnedPercentage', 'MainCachePagesDirtyPercentage', 'MainCachePagesInUsePercentage',  \n" +
			"    'TempCacheCurrentSize', 'TempCacheFinds', 'TempCacheHits', 'TempCachePagesPinned', 'TempCachePagesPinnedPercentage', 'TempCachePagesDirtyPercentage', 'TempCachePagesInUsePercentage',  \n" +
			"    'MemoryAllocated', 'MemoryMaxAllocated' ) \n";

		return sql;
	}
}
