package com.asetune.cm.iq;

import java.sql.Connection;
import java.util.LinkedList;
import java.util.List;

import javax.naming.NameNotFoundException;

import com.asetune.ICounterController;
import com.asetune.IGuiController;
import com.asetune.MonTablesDictionary;
import com.asetune.TrendGraphDataPoint;
import com.asetune.cm.CounterSetTemplates;
import com.asetune.cm.CounterSetTemplates.Type;
import com.asetune.cm.CountersModel;
import com.asetune.cm.sql.VersionInfo;
import com.asetune.gui.MainFrame;
import com.asetune.gui.TrendGraph;
import com.asetune.utils.AseConnectionUtils;
import com.asetune.utils.Ver;

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

	public static final int      NEED_SRV_VERSION = 0;
	public static final int      NEED_CE_VERSION  = 0;

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
		super(CM_NAME, GROUP_NAME, /*sql*/null, /*pkList*/null, 
				DIFF_COLUMNS, PCT_COLUMNS, MON_TABLES, 
				NEED_ROLES, NEED_CONFIG, NEED_SRV_VERSION, NEED_CE_VERSION, 
				NEGATIVE_DIFF_COUNTERS_TO_ZERO, IS_SYSTEM_CM, DEFAULT_POSTPONE_TIME);

		setDisplayName(SHORT_NAME);
		setDescription(HTML_DESC);

		setIconFile(GUI_ICON_FILE);
		
		setBackgroundDataPollingEnabled(false, false);
		
		setCounterController(counterController);
		setGuiController(guiController);
		
		addTrendGraphs();
		
		CounterSetTemplates.register(this);
	}


	//------------------------------------------------------------
	// Implementation
	//------------------------------------------------------------
	public static final String GRAPH_NAME_CACHE2 = "IqStatisticsMemoryGraph"; 

	private void addTrendGraphs()
	{
		String[] labels_memo = new String[] { "MemoryAllocated", "MemoryMaxAllocated", "MainCacheCurrentSize", "TempCacheCurrentSize", "CurrentCacheSize", "MaxCacheSize", "MinCacheSize"} ;
		
		addTrendGraphData(GRAPH_NAME_CACHE2, new TrendGraphDataPoint(GRAPH_NAME_CACHE2, labels_memo));

		// if GUI
		if (getGuiController() != null && getGuiController().hasGUI())
		{
			// GRAPH
			TrendGraph tg = null;
			tg = new TrendGraph(GRAPH_NAME_CACHE2,
				"Memory overview",                     // Menu CheckBox text
				"Memory overview", // Label 
				labels_memo, 
				false, // is Percent Graph
				this, 
				false, // visible at start
				0,     // graph is valid from Server Version. 0 = All Versions; >0 = Valid from this version and above 
				-1);   // minimum height
			addTrendGraph(tg.getName(), tg, true);
				
		}
	}
	
	@Override
	public void updateGraphData(TrendGraphDataPoint tgdp)
	{	
		if (GRAPH_NAME_CACHE2.equals(tgdp.getName()))
		{	
			Double[] arr = new Double[7];
			arr[0] = this.getAbsValue("MemoryAllocated" , "PropValue");
			arr[1] = this.getAbsValue("MemoryMaxAllocated", "PropValue");
			arr[2] = this.getAbsValue("MainCacheCurrentSize", "PropValue");
			arr[3] = this.getAbsValue("TempCacheCurrentSize", "PropValue");
			arr[4] = this.getAbsValue("CurrentCacheSize", "PropValue");
			arr[5] = this.getAbsValue("MaxCacheSize", "PropValue");
			arr[6] = this.getAbsValue("MinCacheSize", "PropValue");
			
			// Set the values
			tgdp.setDate(this.getTimestamp());
			tgdp.setData(arr);
		}
	}
	

	@Override
	public String[] getDependsOnConfigForVersion(Connection conn, int srvVersion, boolean isClusterEnabled)
	{
		return NEED_CONFIG;
	}

	@Override
	public void addMonTableDictForVersion(Connection conn, int srvVersion, boolean isClusterEnabled)
	{
		try 
		{
			MonTablesDictionary mtd = MonTablesDictionary.getInstance();
			mtd.addTable("iq_cache_custom",  "Various caches information.");

			mtd.addColumn("iq_cache_custom", "CacheType",  
					"<html>Catalog or IQ</html>");
			mtd.addColumn("iq_cache_custom", "PropNum",  
					"<html>Number (PropNum in sa_eng_properties and stat_num in sp_iqstatistics)</html>");
			mtd.addColumn("iq_cache_custom", "PropName",  
					"<html>Name</html>");
			mtd.addColumn("iq_cache_custom", "PropDescription",  
					"<html>Description</html>");
			mtd.addColumn("iq_cache_custom", "PropValue",  
					"<html>Value</html>");
			mtd.addColumn("iq_cache_custom", "PropUnit",  
					"<html>Unit of measure</html>");
		}
		catch (NameNotFoundException e) {/*ignore*/}
	}

	@Override
	public List<String> getPkForVersion(Connection conn, int srvVersion, boolean isClusterEnabled)
	{
		List <String> pkCols = new LinkedList<String>();
		
		pkCols.add("PropName");
		
		return pkCols;
	}

	@Override
	public String getSqlForVersion(Connection conn, int aseVersion, boolean isClusterEnabled)
	{
		String sql = "select 'Catalog' CacheType, PropNum, PropName, replace(PropDescription, 'kilobytes', 'megabytes') PropDescription, case PropName when 'CachePanics' then Value else cast(Value/1024 as decimal(10,2)) end PropValue, case PropName when 'CachePanics' then '' else 'kb' end as PropUnit from sa_eng_properties() where PropName in ('CurrentCacheSize', 'MaxCacheSize', 'MinCacheSize', 'PageSize', 'CachePanics')"
					+ "union all "
					+ "select 'IQ' CacheType, stat_num PropNum, stat_name PropName, stat_desc PropDescription, stat_value PropValue, stat_unit PropUnit "
					+ "from sp_iqstatistics() where stat_name in ( "
					+ "'MainCacheCurrentSize', 'MainCacheFinds', 'MainCacheHits', 'MainCachePagesPinned', 'MainCachePagesPinnedPercentage', 'MainCachePagesDirtyPercentage', 'MainCachePagesInUsePercentage', 'TempCacheCurrentSize', 'TempCacheFinds', 'TempCacheHits', 'TempCachePagesPinned', 'TempCachePagesPinnedPercentage', 'TempCachePagesDirtyPercentage', 'TempCachePagesInUsePercentage', 'MemoryAllocated', 'MemoryMaxAllocated' "
					+ ")";

		return sql;
	}
}
