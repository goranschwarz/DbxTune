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
package com.asetune.cm.ase;

import java.math.BigDecimal;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;

import javax.naming.NameNotFoundException;

import org.apache.log4j.Logger;

import com.asetune.ICounterController;
import com.asetune.IGuiController;
import com.asetune.central.pcs.CentralPersistReader;
import com.asetune.cm.CmSettingsHelper;
import com.asetune.cm.CounterSample;
import com.asetune.cm.CounterSetTemplates;
import com.asetune.cm.CounterSetTemplates.Type;
import com.asetune.cm.CountersModel;
import com.asetune.cm.ase.gui.CmCachePoolsPanel;
import com.asetune.config.dict.MonTablesDictionary;
import com.asetune.config.dict.MonTablesDictionaryManager;
import com.asetune.graph.TrendGraphDataPoint;
import com.asetune.graph.TrendGraphDataPoint.LabelType;
import com.asetune.gui.MainFrame;
import com.asetune.gui.TabularCntrPanel;
import com.asetune.sql.conn.DbxConnection;
import com.asetune.sql.conn.info.DbmsVersionInfo;
import com.asetune.sql.conn.info.DbmsVersionInfoSybaseAse;
import com.asetune.utils.Configuration;
import com.asetune.utils.TimeUtils;
import com.asetune.utils.Ver;

/**
 * @author Goran Schwarz (goran_schwarz@hotmail.com)
 */
public class CmCachePools
extends CountersModel
{
	private static Logger        _logger          = Logger.getLogger(CmCachePools.class);
	private static final long    serialVersionUID = 1L;

	public static final String   CM_NAME          = CmCachePools.class.getSimpleName();
	public static final String   SHORT_NAME       = "Pools";
	public static final String   HTML_DESC        = 
		"<html>" +
		"The cahces has 2K or 16K pools, how are they behaving?" +
		"<br><br>" +
		"Table Background colors:" +
		"<ul>" +
		"    <li>RED - Column 'Stalls' is above 0. Number of times I/O operation was delayed because no clean buffers were available in the 'wash area'.</li>" +
		"</ul>" +
		"</html>";

	public static final String   GROUP_NAME       = MainFrame.TCP_GROUP_CACHE;
	public static final String   GUI_ICON_FILE    = "images/"+CM_NAME+".png";

	public static final long     NEED_SRV_VERSION = 0;
	public static final long     NEED_CE_VERSION  = 0;

	public static final String[] MON_TABLES       = new String[] {"monCachePool"};
	public static final String[] NEED_ROLES       = new String[] {"mon_role"};
	public static final String[] NEED_CONFIG      = new String[] {"enable monitoring=1"};

	public static final String[] PCT_COLUMNS      = new String[] {"PhysicalReadsPct", "APFReadsPct", "CacheUtilization", "CacheEfficiency", "CacheEfficiencySlide", "CacheReplacementPct", "CacheReplacementSlidePct", "CacheHitRate"};
	public static final String[] DIFF_COLUMNS     = new String[] {
		"PagesTouchedDiff", "UsedSizeInMbDiff", "UnUsedSizeInMbDiff", 
		"RealPagesRead", "PagesRead", "RealPhysicalReads", "PhysicalReads", "Stalls", "BuffersToMRU", "BuffersToLRU", 
		"LogicalReads", "PhysicalWrites", "APFReads"};

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

		return new CmCachePools(counterController, guiController);
	}

	public CmCachePools(ICounterController counterController, IGuiController guiController)
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
	private static final String PROP_PREFIX                       = CM_NAME;

	public static final String  PROPKEY_CacheSlideTimeInSec             = PROP_PREFIX + ".CacheSlideTimeInSec";
	public static final int     DEFAULT_CacheSlideTimeInSec             = 900;

	public static final String  PROPKEY_CacheHitRateTo100PctOnZeroReads = PROP_PREFIX + ".CacheHitRateTo100PctOnZeroReads";
	public static final boolean DEFAULT_CacheHitRateTo100PctOnZeroReads = true;

	public static final String GRAPH_NAME_POOL_HIT_RATE           = "PoolHitRate";
	public static final String GRAPH_NAME_POOL_UTIL               = "PoolUtil";
	public static final String GRAPH_NAME_POOL_USED_MB            = "PoolUsedMb";
	public static final String GRAPH_NAME_POOL_FREE_MB            = "PoolFreeMb";
	public static final String GRAPH_NAME_POOL_TO_MRU             = "PoolToMru";
	public static final String GRAPH_NAME_POOL_TO_LRU             = "PoolToLru";
	public static final String GRAPH_NAME_POOL_LOGICAL_READ       = "PoolLogicalRead";
	public static final String GRAPH_NAME_POOL_REAL_PHYSICAL_READ = "PoolRealPhysicalRead";
	public static final String GRAPH_NAME_POOL_PHYSICAL_READ      = "PoolPhysicalRead";
	public static final String GRAPH_NAME_POOL_APF_READ           = "PoolApfRead";
	public static final String GRAPH_NAME_POOL_APF_PCT            = "PoolApfPct";
	public static final String GRAPH_NAME_POOL_REPLACE_SLIDE      = "PoolReplaceSlide";

	private void addTrendGraphs()
	{
		addTrendGraph(GRAPH_NAME_POOL_HIT_RATE,
			"Cache Pools Hit Rate", 	               // Menu CheckBox text
			"Cache Pools Hit Rate Percent ("+GROUP_NAME+"->"+SHORT_NAME+")", // Label 
			TrendGraphDataPoint.createGraphProps(TrendGraphDataPoint.Y_AXIS_SCALE_LABELS_PERCENT, CentralPersistReader.SampleType.MAX_OVER_SAMPLES),
			null, 
			LabelType.Dynamic,
			TrendGraphDataPoint.Category.CACHE,
			true, // is Percent Graph
			false,  // visible at start
			Ver.ver(15,7),     // graph is valid from Server Version. 0 = All Versions; >0 = Valid from this version and above 
			-1);  // minimum height

		// GRAPH
		addTrendGraph(GRAPH_NAME_POOL_UTIL,
			"Cache Pools Utilization", 	               // Menu CheckBox text
			"Cache Pools Utilization Percent ("+GROUP_NAME+"->"+SHORT_NAME+")", // Label 
			TrendGraphDataPoint.createGraphProps(TrendGraphDataPoint.Y_AXIS_SCALE_LABELS_PERCENT, CentralPersistReader.SampleType.MAX_OVER_SAMPLES),
			null, 
			LabelType.Dynamic,
			TrendGraphDataPoint.Category.CACHE,
			true, // is Percent Graph
			false,  // visible at start
			0,     // graph is valid from Server Version. 0 = All Versions; >0 = Valid from this version and above 
			-1);  // minimum height

		// GRAPH
		addTrendGraph(GRAPH_NAME_POOL_USED_MB,
			"Cache Pools Used MB", 	               // Menu CheckBox text
			"Cache Pools Used MB ("+GROUP_NAME+"->"+SHORT_NAME+")", // Label 
			TrendGraphDataPoint.createGraphProps(TrendGraphDataPoint.Y_AXIS_SCALE_LABELS_MB, CentralPersistReader.SampleType.MAX_OVER_SAMPLES),
			null, 
			LabelType.Dynamic,
			TrendGraphDataPoint.Category.CACHE,
			false, // is Percent Graph
			false,  // visible at start
			0,     // graph is valid from Server Version. 0 = All Versions; >0 = Valid from this version and above 
			-1);  // minimum height

		// GRAPH
		addTrendGraph(GRAPH_NAME_POOL_FREE_MB,
			"Cache Pools Free MB", 	               // Menu CheckBox text
			"Cache Pools Free MB ("+GROUP_NAME+"->"+SHORT_NAME+")", // Label 
			TrendGraphDataPoint.createGraphProps(TrendGraphDataPoint.Y_AXIS_SCALE_LABELS_MB, CentralPersistReader.SampleType.MIN_OVER_SAMPLES),
			null, 
			LabelType.Dynamic,
			TrendGraphDataPoint.Category.CACHE,
			false, // is Percent Graph
			false,  // visible at start
			0,     // graph is valid from Server Version. 0 = All Versions; >0 = Valid from this version and above 
			-1);  // minimum height

		// GRAPH
		addTrendGraph(GRAPH_NAME_POOL_TO_MRU,
			"Cache Pools MRU Replacement", 	               // Menu CheckBox text
			"Cache Pools MRU Replacement per Second ("+GROUP_NAME+"->"+SHORT_NAME+")", // Label 
			TrendGraphDataPoint.createGraphProps(TrendGraphDataPoint.Y_AXIS_SCALE_LABELS_PERSEC, CentralPersistReader.SampleType.MAX_OVER_SAMPLES),
			null, 
			LabelType.Dynamic,
			TrendGraphDataPoint.Category.CACHE,
			false, // is Percent Graph
			false,  // visible at start
			0,     // graph is valid from Server Version. 0 = All Versions; >0 = Valid from this version and above 
			-1);  // minimum height

		// GRAPH
		addTrendGraph(GRAPH_NAME_POOL_TO_LRU,
			"Cache Pools LRU fetch-and-discard Placement", 	               // Menu CheckBox text
			"Cache Pools LRU fetch-and-discard Placement per Second ("+GROUP_NAME+"->"+SHORT_NAME+")", // Label 
			TrendGraphDataPoint.createGraphProps(TrendGraphDataPoint.Y_AXIS_SCALE_LABELS_PERSEC, CentralPersistReader.SampleType.MAX_OVER_SAMPLES),
			null, 
			LabelType.Dynamic,
			TrendGraphDataPoint.Category.CACHE,
			false, // is Percent Graph
			true,  // visible at start
			0,     // graph is valid from Server Version. 0 = All Versions; >0 = Valid from this version and above 
			-1);  // minimum height

		// GRAPH
		addTrendGraph(GRAPH_NAME_POOL_LOGICAL_READ,
			"Cache Pools Logical Reads", 	               // Menu CheckBox text
			"Cache Pools Logical Reads per Second ("+GROUP_NAME+"->"+SHORT_NAME+")", // Label 
			TrendGraphDataPoint.createGraphProps(TrendGraphDataPoint.Y_AXIS_SCALE_LABELS_PERSEC, CentralPersistReader.SampleType.MAX_OVER_SAMPLES),
			null, 
			LabelType.Dynamic,
			TrendGraphDataPoint.Category.CACHE,
			false, // is Percent Graph
			false,  // visible at start
			Ver.ver(15,7),     // graph is valid from Server Version. 0 = All Versions; >0 = Valid from this version and above 
			-1);  // minimum height

		// GRAPH
		addTrendGraph(GRAPH_NAME_POOL_REAL_PHYSICAL_READ,
			"Cache Pools Real Physical Reads (Physical+APF)", 	               // Menu CheckBox text
			"Cache Pools Real Physical Reads (Physical+APF) per Second ("+GROUP_NAME+"->"+SHORT_NAME+")", // Label 
			TrendGraphDataPoint.createGraphProps(TrendGraphDataPoint.Y_AXIS_SCALE_LABELS_PERSEC, CentralPersistReader.SampleType.MAX_OVER_SAMPLES),
			null, 
			LabelType.Dynamic,
			TrendGraphDataPoint.Category.CACHE,
			false, // is Percent Graph
			false,  // visible at start
			0,     // graph is valid from Server Version. 0 = All Versions; >0 = Valid from this version and above 
			-1);  // minimum height

		// GRAPH
		addTrendGraph(GRAPH_NAME_POOL_PHYSICAL_READ,
			"Cache Pools Physical Reads", 	               // Menu CheckBox text
			"Cache Pools Physical Reads per Second ("+GROUP_NAME+"->"+SHORT_NAME+")", // Label 
			TrendGraphDataPoint.createGraphProps(TrendGraphDataPoint.Y_AXIS_SCALE_LABELS_PERSEC, CentralPersistReader.SampleType.MAX_OVER_SAMPLES),
			null, 
			LabelType.Dynamic,
			TrendGraphDataPoint.Category.CACHE,
			false, // is Percent Graph
			false,  // visible at start
			0,     // graph is valid from Server Version. 0 = All Versions; >0 = Valid from this version and above 
			-1);  // minimum height

		// GRAPH
		addTrendGraph(GRAPH_NAME_POOL_APF_READ,
			"Cache Pools APF Reads", 	               // Menu CheckBox text
			"Cache Pools APF Reads per Second ("+GROUP_NAME+"->"+SHORT_NAME+")", // Label 
			TrendGraphDataPoint.createGraphProps(TrendGraphDataPoint.Y_AXIS_SCALE_LABELS_PERSEC, CentralPersistReader.SampleType.MAX_OVER_SAMPLES),
			null, 
			LabelType.Dynamic,
			TrendGraphDataPoint.Category.CACHE,
			false, // is Percent Graph
			false,  // visible at start
			Ver.ver(15,7),     // graph is valid from Server Version. 0 = All Versions; >0 = Valid from this version and above 
			-1);  // minimum height

		// GRAPH
		addTrendGraph(GRAPH_NAME_POOL_APF_PCT,
			"Cache Pools APF Reads Percent", 	               // Menu CheckBox text
			"Cache Pools APF Reads Percent ("+GROUP_NAME+"->"+SHORT_NAME+")", // Label 
			TrendGraphDataPoint.createGraphProps(TrendGraphDataPoint.Y_AXIS_SCALE_LABELS_PERCENT, CentralPersistReader.SampleType.MAX_OVER_SAMPLES),
			null, 
			LabelType.Dynamic,
			TrendGraphDataPoint.Category.CACHE,
			true, // is Percent Graph
			false,  // visible at start
			Ver.ver(15,7),     // graph is valid from Server Version. 0 = All Versions; >0 = Valid from this version and above 
			-1);  // minimum height

		// GRAPH
		addTrendGraph(GRAPH_NAME_POOL_REPLACE_SLIDE,
			"Cache Pools Replacement Slide", 	               // Menu CheckBox text
			"Cache Pools Replacement Slide Percent ("+GROUP_NAME+"->"+SHORT_NAME+")", // Label 
			TrendGraphDataPoint.createGraphProps(TrendGraphDataPoint.Y_AXIS_SCALE_LABELS_PERCENT, CentralPersistReader.SampleType.MAX_OVER_SAMPLES),
			null, 
			LabelType.Dynamic,
			TrendGraphDataPoint.Category.CACHE,
			false, // is Percent Graph (this can be more than 100%)
			false,  // visible at start
			0,     // graph is valid from Server Version. 0 = All Versions; >0 = Valid from this version and above 
			-1);  // minimum height

	}

	private String getLabel(int row)
	{
		StringBuilder sb = new StringBuilder();
//		sb.append(this.getRateString(row, "CacheName")).append("[").append(this.getRateValueAsDouble(row, "IOBufferSize")/1024).append("K]");
		sb.append(this.getRateString(row, "CacheName")).append("[").append(this.getRateString(row, "PagesPerIO")).append("-pg]");
		return sb.toString();
	}
	@Override
	public void updateGraphData(TrendGraphDataPoint tgdp)
	{
		if (GRAPH_NAME_POOL_HIT_RATE.equals(tgdp.getName()))
		{
			// Write 1 "line" for every pool
			Double[] dArray = new Double[this.size()];
			String[] lArray = new String[dArray.length];
			for (int i = 0; i < dArray.length; i++)
			{
				lArray[i] = getLabel(i);
				dArray[i] = this.getRateValueAsDouble(i, "CacheHitRate");
			}

			// Set the values
			tgdp.setDataPoint(this.getTimestamp(), lArray, dArray);
		}

		if (GRAPH_NAME_POOL_UTIL.equals(tgdp.getName()))
		{
			// Write 1 "line" for every pool
			Double[] dArray = new Double[this.size()];
			String[] lArray = new String[dArray.length];
			for (int i = 0; i < dArray.length; i++)
			{
				lArray[i] = getLabel(i);
				dArray[i] = this.getRateValueAsDouble(i, "CacheUtilization");
			}

			// Set the values
			tgdp.setDataPoint(this.getTimestamp(), lArray, dArray);
		}

		if (GRAPH_NAME_POOL_USED_MB.equals(tgdp.getName()))
		{
			// Write 1 "line" for every pool
			Double[] dArray = new Double[this.size()];
			String[] lArray = new String[dArray.length];
			for (int i = 0; i < dArray.length; i++)
			{
				lArray[i] = getLabel(i);
				dArray[i] = this.getRateValueAsDouble(i, "UsedSizeInMb");
			}

			// Set the values
			tgdp.setDataPoint(this.getTimestamp(), lArray, dArray);
		}

		if (GRAPH_NAME_POOL_FREE_MB.equals(tgdp.getName()))
		{
			// Write 1 "line" for every pool
			Double[] dArray = new Double[this.size()];
			String[] lArray = new String[dArray.length];
			for (int i = 0; i < dArray.length; i++)
			{
				lArray[i] = getLabel(i);
				dArray[i] = this.getRateValueAsDouble(i, "UnUsedSizeInMb");
			}

			// Set the values
			tgdp.setDataPoint(this.getTimestamp(), lArray, dArray);
		}

		if (GRAPH_NAME_POOL_TO_MRU.equals(tgdp.getName()))
		{
			// Write 1 "line" for every pool
			Double[] dArray = new Double[this.size()];
			String[] lArray = new String[dArray.length];
			for (int i = 0; i < dArray.length; i++)
			{
				lArray[i] = getLabel(i);
				dArray[i] = this.getRateValueAsDouble(i, "BuffersToMRU");
			}

			// Set the values
			tgdp.setDataPoint(this.getTimestamp(), lArray, dArray);
		}

		if (GRAPH_NAME_POOL_TO_LRU.equals(tgdp.getName()))
		{
			// Write 1 "line" for every pool
			Double[] dArray = new Double[this.size()];
			String[] lArray = new String[dArray.length];
			for (int i = 0; i < dArray.length; i++)
			{
				lArray[i] = getLabel(i);
				dArray[i] = this.getRateValueAsDouble(i, "BuffersToLRU");
			}

			// Set the values
			tgdp.setDataPoint(this.getTimestamp(), lArray, dArray);
		}

		if (GRAPH_NAME_POOL_LOGICAL_READ.equals(tgdp.getName()))
		{
			// Write 1 "line" for every pool
			Double[] dArray = new Double[this.size()];
			String[] lArray = new String[dArray.length];
			for (int i = 0; i < dArray.length; i++)
			{
				lArray[i] = getLabel(i);
				dArray[i] = this.getRateValueAsDouble(i, "LogicalReads");
			}

			// Set the values
			tgdp.setDataPoint(this.getTimestamp(), lArray, dArray);
		}

		if (GRAPH_NAME_POOL_REAL_PHYSICAL_READ.equals(tgdp.getName()))
		{
			// Write 1 "line" for every pool
			Double[] dArray = new Double[this.size()];
			String[] lArray = new String[dArray.length];
			for (int i = 0; i < dArray.length; i++)
			{
				lArray[i] = getLabel(i);
				dArray[i] = this.getRateValueAsDouble(i, "RealPhysicalReads");
			}

			// Set the values
			tgdp.setDataPoint(this.getTimestamp(), lArray, dArray);
		}

		if (GRAPH_NAME_POOL_PHYSICAL_READ.equals(tgdp.getName()))
		{
			// Write 1 "line" for every pool
			Double[] dArray = new Double[this.size()];
			String[] lArray = new String[dArray.length];
			for (int i = 0; i < dArray.length; i++)
			{
				lArray[i] = getLabel(i);
				dArray[i] = this.getRateValueAsDouble(i, "PhysicalReads");
			}

			// Set the values
			tgdp.setDataPoint(this.getTimestamp(), lArray, dArray);
		}

		if (GRAPH_NAME_POOL_APF_READ.equals(tgdp.getName()))
		{
			// Write 1 "line" for every pool
			Double[] dArray = new Double[this.size()];
			String[] lArray = new String[dArray.length];
			for (int i = 0; i < dArray.length; i++)
			{
				lArray[i] = getLabel(i);
				dArray[i] = this.getRateValueAsDouble(i, "APFReads");
			}

			// Set the values
			tgdp.setDataPoint(this.getTimestamp(), lArray, dArray);
		}

		if (GRAPH_NAME_POOL_APF_PCT.equals(tgdp.getName()))
		{
			// Write 1 "line" for every pool
			Double[] dArray = new Double[this.size()];
			String[] lArray = new String[dArray.length];
			for (int i = 0; i < dArray.length; i++)
			{
				lArray[i] = getLabel(i);
				dArray[i] = this.getRateValueAsDouble(i, "APFReadsPct");
			}

			// Set the values
			tgdp.setDataPoint(this.getTimestamp(), lArray, dArray);
		}

		if (GRAPH_NAME_POOL_REPLACE_SLIDE.equals(tgdp.getName()))
		{
			// Write 1 "line" for every pool
			Double[] dArray = new Double[this.size()];
			String[] lArray = new String[dArray.length];
			for (int i = 0; i < dArray.length; i++)
			{
				lArray[i] = getLabel(i);
				dArray[i] = this.getRateValueAsDouble(i, "CacheReplacementSlidePct");
			}

			// Set the values
			tgdp.setDataPoint(this.getTimestamp(), lArray, dArray);
		}
	}

	@Override
	protected void registerDefaultValues()
	{
		super.registerDefaultValues();

		Configuration.registerDefaultValue(PROPKEY_CacheSlideTimeInSec,             DEFAULT_CacheSlideTimeInSec);
		Configuration.registerDefaultValue(PROPKEY_CacheHitRateTo100PctOnZeroReads, DEFAULT_CacheHitRateTo100PctOnZeroReads);
	}

	/** Used by the: Create 'Offline Session' Wizard */
	@Override
	public List<CmSettingsHelper> getLocalSettings()
	{
		Configuration conf = Configuration.getCombinedConfiguration();
		List<CmSettingsHelper> list = new ArrayList<>();
		
		list.add(new CmSettingsHelper("Slide Window Time",                     PROPKEY_CacheSlideTimeInSec             , Integer.class, conf.getIntProperty    (PROPKEY_CacheSlideTimeInSec             , DEFAULT_CacheSlideTimeInSec             ), DEFAULT_CacheSlideTimeInSec            , "Set number of seconds the 'slide window time' will keep 'PagesRead' for." ));
		list.add(new CmSettingsHelper("CacheHitRate to 100% if LogReads is 0", PROPKEY_CacheHitRateTo100PctOnZeroReads , Boolean.class, conf.getBooleanProperty(PROPKEY_CacheHitRateTo100PctOnZeroReads , DEFAULT_CacheHitRateTo100PctOnZeroReads ), DEFAULT_CacheHitRateTo100PctOnZeroReads, "When LogicalReads is Zero, set CacheHitRate to 100% instead of 0%"        ));

		return list;
	}



	private static class CacheSlideEntry
	{
		Timestamp _sampleTime;
		int       _pagesRead;

		public CacheSlideEntry(Timestamp sampleTime, int pagesRead)
		{
			_sampleTime = sampleTime;
			_pagesRead  = pagesRead;
		}
	}
	LinkedHashMap<String, LinkedList<CacheSlideEntry>> _slideCache = new LinkedHashMap<String, LinkedList<CacheSlideEntry>>();

	@Override
	protected TabularCntrPanel createGui()
	{
		return new CmCachePoolsPanel(this);
	}

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
			MonTablesDictionary mtd = MonTablesDictionaryManager.getInstance();
			mtd.addColumn("monCachePool",  "Stalls",         "Number of times I/O operations were delayed because no clean buffers were available in the wash area");

			mtd.addColumn("monCachePool",  "SrvPageSize",    "ASE Servers page size (@@maxpagesize)");
			mtd.addColumn("monCachePool",  "PagesPerIO",     "This pools page size (1=SinglePage, 2=, 4=, 8=Extent IO)");
			mtd.addColumn("monCachePool",  "AllocatedPages", "Number of actual pages allocated to this pool. same as 'AllocatedKB' but in pages instead of KB.");
			
			mtd.addColumn("monCachePool",  "APFReadsPct",    "<html>" +
			                                                       "What's the percentage of the Async Prefetch Reads (APFReads) compared to PhysicalReads.<br>" +
			                                                       "<b>Formula</b>: APFReadsPct / PagesRead * 100<br>" +
			                                                 "</html>");

			mtd.addColumn("monCachePool",  "PhysicalReadsPct", "<html>" +
			                                                       "What's the percentage of the PhysicalReads compared to Async Prefetch Reads (APFReads).<br>" +
			                                                       "<b>Formula</b>: PhysicalReads / PagesRead * 100<br>" +
			                                                 "</html>");

			mtd.addColumn("monCachePool",  "RealPhysicalReads", "<html>"
			                                                      + "Actual or Real Physical Reads <br>"
			                                                      + "<b>Formula</b>: PhysicalReads + APFReads <br>"
			                                                      + "</html>");

			mtd.addColumn("monCachePool",  "RealPagesRead", "<html>"
			                                                      + "Actual or Real Number of Pages read into the pool. <br>"
			                                                      + "For a 1 Page Pool, this is the same value as PagesRead. <br>"
			                                                      + "But for a 2, 4 or 8 Page Pool, the PagesRead is a bit missleading, yes it number of pages, but not Physical IO's read.<br>"
			                                                      + "To get Physical IO's read you need to divide by number of PagesPerIO in the Pool. (since we are reading X pages with One IO Operation) <br>"
			                                                      + "<b>Formula</b>: PagesRead / PagesPerIO <br>"
			                                                      + "</html>");

			mtd.addColumn("monCachePool",  "AllocatedMb",    "<html>" +
			                                                       "Same as AlllocatedKB but as MB instead.<br>" +
			                                                       "<b>Formula</b>: AllocatedKB / 1024 <br>" +
			                                                 "</html>");

			mtd.addColumn("monCachePool",  "PagesTouchedDiff",   "<html>" +
			                                                       "Same a 'PagesTouched' (pages in use), but difference calculated.<br>" +
			                                                       "<b>Formula</b>: PagesTouched<br>" +
			                                                     "</html>");

			mtd.addColumn("monCachePool",  "UnUsedSizeInMbDiff", "<html>" +
			                                                       "How many MB of the cache is not used, same as 'UnUsedSizeInMb' but diff calculated.<br>" +
			                                                       "If anything is unused, it means that we have more space in the cache.<br>" +
			                                                       "<b>Formula</b>: abs.AllocatedPages / abs.PagesTouched ... but in MB instead of pages.<br>" +
			                                                     "</html>");

			mtd.addColumn("monCachePool",  "UsedSizeInMbDiff",   "<html>" +
			                                                       "How many MB of the cache is <b>used</b>, same as 'UsedSizeInMb' but diff calculated.<br>" +
			                                                       "Also look at the value <code>UnUsedSizeInMb</code> which is the <i>same</i> thing, but the other way around.<br>" +
			                                                       "<b>Formula</b>: abs.PagesTouched ... but in MB instead of pages.<br>" +
			                                                     "</html>");

			mtd.addColumn("monCachePool",  "UnUsedSizeInMb", "<html>" +
			                                                       "How many MB of the cache is not used.<br>" +
			                                                       "If anything is unused, it means that we have more space in the cache.<br>" +
			                                                       "<b>Formula</b>: abs.AllocatedPages / abs.PagesTouched ... but in MB instead of pages.<br>" +
			                                                 "</html>");

			mtd.addColumn("monCachePool",  "UsedSizeInMb",   "<html>" +
			                                                       "How many MB of the cache is <b>used</b>.<br>" +
			                                                       "Also look at the value <code>UnUsedSizeInMb</code> which is the <i>same</i> thing, but the other way around.<br>" +
			                                                       "<b>Formula</b>: abs.PagesTouched ... but in MB instead of pages.<br>" +
			                                                 "</html>");

			mtd.addColumn("monCachePool",  "CacheUtilization", "<html>" +
			                                                       "If not 100% the cache has to much memory allocated to it.<br>" +
			                                                       "Or if it's a <i>fetch-and-discard</i> (BuffersToLRU) where all pages is <i>inserted</i> at the <i>wash marker</i> (WashSize), which means that it's probably a Table Scan...<br>" +
			                                                       "<b>Formula</b>: abs.PagesTouched / abs.AllocatedPages * 100<br>" +
			                                                   "</html>");
			
			mtd.addColumn("monCachePool",  "CacheEfficiency",  "<html>" +
			                                                       "If less than 100, the cache is to small (pages has been flushed out from the cache).<br> " +
			                                                       "Pages are read in from the disk, could be by APF Reads (so cacheHitRate is high) but the pages still had to be read from disk.<br>" +
			                                                       "<b>Note:</b>" +
			                                                       "<ul>" +
			                                                       "  <li>If ABS Counters are selected, this is from when the server started, which is probably a long time.</li>" +
			                                                       "  <li>If Diff/Rate Counters are selected, this is for <b>last</b> sample, which is to short period.</li>" +
			                                                       "  <li>So you might want to look at <code>CacheEfficiencySlide</code> which is for an hour or whatever you decide.</li>" +
			                                                       "</ul>" +
			                                                       "<b>Formula</b>: abs.AllocatedPages / diff.PagesRead * 100<br>" +
			                                                   "</html>");
			mtd.addColumn("monCachePool",  "CacheEfficiencySlide",  
			                                                   "<html>" +
			                                                       "If less than 100, the cache is to small (pages has been flushed out from the cache).<br> " +
			                                                       "Pages are read in from the disk, could be by APF Reads (so cacheHitRate is high) but the pages still had to be read from disk.<br>" +
			                                                       "<b>Formula</b>: abs.AllocatedPages / timeSlide.PagesRead * 100<br>" +
			                                                   "</html>");
			mtd.addColumn("monCachePool",  "CacheReplacementPct",  
			                                                   "<html>" +
			                                                       "How much of the cache was replaced by new pages (Turnover in sp_sysmon).<br> " +
			                                                       "This is sensitive to Absolute and Diff/Rate calculations.<br>" +
			                                                       "<b>Note:</b> If this is above 100, it simply means that the cache/pool has been replaced that many times. So 600% is 6 times the cache size since server was started. <br>" +
			                                                       "<b>Formula</b>: PagesRead / (AllocatedKB*(1024.0/@@maxpagesize))<br>" +
			                                                   "</html>");
			mtd.addColumn("monCachePool",  "CacheReplacementSlidePct",  
			                                                   "<html>" +
			                                                       "How much of the cache was replaced by new pages (Turnover in sp_sysmon).<br> " +
			                                                       "The timespan for this <i>slide</i> is set in the options panel, and it's alos displayed in column <code>CacheSlideTime</code> if the <i>slide time</i> has not yet reached it's maximum value, you will see the current sime span here.<br>" +
			                                                       "<b>Note:</b> If this is above 100, it simply means that the cache/pool has been replaced that many times. So 200% is 2 times the cache size within this <i>time slide window</i> <br>" +
			                                                       "<b>Formula</b>: timeSlide.PagesRead / (AllocatedKB*(1024.0/@@maxpagesize))<br>" +
			                                                   "</html>");
			mtd.addColumn("monCachePool",  "CacheSlideTime",   "<html>" +
			                                                       "This is the current <i>time span</i> the column <code>CacheReplacementSlidePct</code> reflects.<br> " +
			                                                       "<b>Formula</b>: last - first sample time in the <i>slide window</i> <br>" +
			                                                   "</html>");
			mtd.addColumn("monCachePool",  "PagesReadInSlide", "<html>" +
			                                                       "Summary of all <i>diff</i> values for the column <code>PagesRead</code> within the <i>slide window</i>.<br> " +
			                                                       "<b>Formula</b>: summary of all the <code>PagesRead</code> within the <i>slide window</i><br>" +
			                                                   "</html>");
			mtd.addColumn("monCachePool",  "CacheHitRate",     "<html>" +
			                                                       "Percent calculation of how many pages was fetched from the cache pool.<br>" +
			                                                       "<b>Formula</b>: 100 - (RealPagesRead/LogicalReads) * 100.0" +
			                                                   "</html>");
		}
		catch (NameNotFoundException e) {/*ignore*/}
	}

	@Override
	public List<String> getPkForVersion(DbxConnection conn, DbmsVersionInfo versionInfo)
	{
		DbmsVersionInfoSybaseAse aseVersionInfo = (DbmsVersionInfoSybaseAse) versionInfo;
//		long    srvVersion       = aseVersionInfo.getLongVersion();
		boolean isClusterEnabled = aseVersionInfo.isClusterEdition();

		List <String> pkCols = new LinkedList<String>();

		if (isClusterEnabled)
			pkCols.add("InstanceID");

		pkCols.add("CacheName");
		pkCols.add("IOBufferSize");

		return pkCols;
	}

	@Override
	public String getSqlForVersion(DbxConnection conn, DbmsVersionInfo versionInfo)
	{
		DbmsVersionInfoSybaseAse aseVersionInfo = (DbmsVersionInfoSybaseAse) versionInfo;
		long    srvVersion       = aseVersionInfo.getLongVersion();
		boolean isClusterEnabled = aseVersionInfo.isClusterEdition();

		String cols1, cols2, cols3;
		cols1 = cols2 = cols3 = "";

		//------------- NEEDS TO BE CALCULATED AFTER EACH SAMPLE
		// CacheUsage%      = AllocatedKB / (PagesTouched * @@maxpagesize)
		// CacheEfficiency% = PagesRead   / (PagesTouched * @@mxapagesize)

		if (isClusterEnabled)
		{
			cols1 += "InstanceID, ";
		}

		// ASE 15.7
		String LogicalReads      = "";
		String PhysicalReadsPct  = "";
		String PhysicalWrites    = "";
		String RealPhysicalReads = "";
		String APFReads          = "";
		String APFReadsPct       = "";
		String APFPercentage     = "";
		String WashSize          = "";
		String CacheHitRate      = "";

		String calcAllocatedPages = "(AllocatedKB*(1024.0/@@maxpagesize))";
		String calcDivPgsToMb     = "(1024*1024/@@maxpagesize)";                   // for 2K=512, 4K=256, 8K=128, 16K=64 
		String calcPagesPerIO     = "(IOBufferSize/@@maxpagesize)";
		String calcPagesRead      = "(PagesRead/"+calcPagesPerIO+")";
//		String calcPhysicalReads  = "PhysicalReads";

//		if (srvVersion >= 15700)
//		if (srvVersion >= 1570000)
		if (srvVersion >= Ver.ver(15,7))
		{
//			calcPhysicalReads = "(PhysicalReads+APFReads)";

			LogicalReads      = "LogicalReads, \n";
			PhysicalWrites    = "PhysicalWrites, \n";
			RealPhysicalReads = "RealPhysicalReads = convert(bigint, PhysicalReads) + convert(bigint, APFReads), \n";
			PhysicalReadsPct  = "PhysicalReadsPct = CASE WHEN (PagesRead > 0) THEN convert(numeric(10,1), (((1.0*PhysicalReads*"+calcPagesPerIO+")/(1.0*PagesRead)) * 100.0)) ELSE 0.0 END,\n";
			APFReads          = "APFReads, \n";
			APFReadsPct       = "APFReadsPct = CASE WHEN (PagesRead > 0) THEN convert(numeric(10,1), (((1.0*APFReads*"+calcPagesPerIO+")/(1.0*PagesRead)) * 100.0)) ELSE 0.0 END,\n";
			APFPercentage     = "APFPercentage, \n";
			WashSize          = "WashSize, \n";
			CacheHitRate      = "CacheHitRate = convert(numeric(10,1), 100 - ("+calcPagesRead+"*1.0/(LogicalReads+1)) * 100.0), \n";
		}
		
		cols1 += "CacheName, \n" +
		         "CacheID, \n" +
		         "SrvPageSize        = @@maxpagesize, \n" +
		         "IOBufferSize, \n" +
		         WashSize +
		         APFPercentage +
		         "PagesPerIO         = IOBufferSize/@@maxpagesize, \n" +
		         "AllocatedMb        = AllocatedKB / 1024, \n" +
		         "AllocatedKB, \n" +
		         "AllocatedPages     = convert(int,"+calcAllocatedPages+"), \n" +
		         "PagesTouchedDiff   = PagesTouched, \n" +
		         "UsedSizeInMbDiff   = convert(int, PagesTouched / "+calcDivPgsToMb+" ), \n" +
		         "UnUsedSizeInMbDiff = convert(int, ("+calcAllocatedPages+" - PagesTouched) / "+calcDivPgsToMb+" ), \n" +
		         "PagesTouched, \n" +
		         "UsedSizeInMb       = convert(int, PagesTouched / "+calcDivPgsToMb+" ), \n" +
		         "UnUsedSizeInMb     = convert(int, ("+calcAllocatedPages+" - PagesTouched) / "+calcDivPgsToMb+" ), \n" +
		         "CacheUtilization   = convert(numeric(12,1), PagesTouched / "+calcAllocatedPages+" * 100.0), \n" +
		         LogicalReads + 
		         "RealPagesRead      = "+calcPagesRead+", \n" +
		         "PagesRead, \n" +
		         CacheHitRate +
		         APFReads +
		         APFReadsPct +
		         RealPhysicalReads +
		         "PhysicalReads, \n" +
		         PhysicalReadsPct + 
		         PhysicalWrites +
		         "Stalls, \n" +
		         "BuffersToMRU, \n" +
		         "BuffersToLRU, \n" +
		         "CacheReplacementPct        = convert(numeric(12,1), 1.0*PagesRead / "+calcAllocatedPages+"), \n" +
		         "CacheReplacementSlidePct   = convert(numeric(12,1), 0), \n" +
		         "CacheSlideTime             = convert(varchar(30), 'not-for-absolute-values'), \n" +
		         "PagesReadInSlide           = convert(int, 0), \n" +
		         "CacheEfficiency  = CASE \n" +
		         "                      WHEN PagesRead > 0 \n" +
		         "                      THEN convert(numeric(12,1), "+calcAllocatedPages+" / PagesRead * 100.0) \n" +
		         "                      ELSE 0.0 \n" +
		         "                   END, \n" +
		         "CacheEfficiencySlide      = convert(numeric(5,1), 0) \n" +
		         "";

		String sql = 
			"select " + cols1 + cols2 + cols3 + "\n" +
			"from master..monCachePool \n" +
			"order by CacheName, IOBufferSize\n";

		return sql;
	}

	/** 
	 * Compute 
	 */
	@Override
	public void localCalculation(CounterSample prevSample, CounterSample newSample, CounterSample diffData)
	{
		int AllocatedKB,        PagesTouched,        RealPagesRead,        PagesRead,        SrvPageSize;
		int AllocatedKBId = -1, PagesTouchedId = -1, RealPagesReadId = -1, PagesReadId = -1, SrvPageSizeId = -1;

		int PhysicalReadsId = -1, APFReadsId = -1, PhysicalReadsPctId = -1, APFReadsPctId = -1;
		int PagesPerIOId = -1;

		int CacheUtilizationId = -1, CacheEfficiencyId = -1;
		
		int CacheHitRateId = -1, LogicalReadsId = -1;
		int CacheEfficiencySlideId = -1;
		int CacheReplacementPctId = -1;
		int CacheReplacementSlidePctId = -1, CacheSlideTimeId = -1, PagesReadInSlideId = -1;

		// Find column Id's
		List<String> colNames = diffData.getColNames();
		if (colNames==null) return;
	
		for (int colId=0; colId < colNames.size(); colId++) 
		{
			String colName = colNames.get(colId);
			if      (colName.equals("AllocatedKB"))               AllocatedKBId               = colId;
			else if (colName.equals("PagesTouched"))              PagesTouchedId              = colId;
			else if (colName.equals("RealPagesRead"))             RealPagesReadId             = colId;
			else if (colName.equals("PagesRead"))                 PagesReadId                 = colId;
			else if (colName.equals("SrvPageSize"))               SrvPageSizeId               = colId;
			else if (colName.equals("PhysicalReads"))             PhysicalReadsId             = colId;
			else if (colName.equals("PhysicalReadsPct"))          PhysicalReadsPctId          = colId;
			else if (colName.equals("APFReads"))                  APFReadsId                  = colId;
			else if (colName.equals("APFReadsPct"))               APFReadsPctId               = colId;
			else if (colName.equals("PagesPerIO"))                PagesPerIOId                = colId;
			else if (colName.equals("CacheUtilization"))          CacheUtilizationId          = colId;
			else if (colName.equals("CacheReplacementPct"))       CacheReplacementPctId       = colId;
			else if (colName.equals("CacheReplacementSlidePct"))  CacheReplacementSlidePctId  = colId;
			else if (colName.equals("CacheSlideTime"))            CacheSlideTimeId            = colId;
			else if (colName.equals("PagesReadInSlide"))          PagesReadInSlideId          = colId;
			else if (colName.equals("CacheEfficiency"))           CacheEfficiencyId           = colId;
			else if (colName.equals("CacheEfficiencySlide"))      CacheEfficiencySlideId      = colId;
			else if (colName.equals("CacheHitRate"))              CacheHitRateId              = colId;
			else if (colName.equals("LogicalReads"))              LogicalReadsId              = colId;
		}
	
		// Loop on all diffData rows
		for (int rowId=0; rowId < diffData.getRowCount(); rowId++) 
		{
			AllocatedKB   = ((Number)newSample.getValueAt(rowId, AllocatedKBId  )).intValue();
			PagesTouched  = ((Number)newSample.getValueAt(rowId, PagesTouchedId )).intValue();
			RealPagesRead = ((Number)diffData .getValueAt(rowId, RealPagesReadId)).intValue();
			PagesRead     = ((Number)diffData .getValueAt(rowId, PagesReadId    )).intValue();
			SrvPageSize   = ((Number)newSample.getValueAt(rowId, SrvPageSizeId  )).intValue();
			
			double allocatedPages = AllocatedKB*(1024.0/SrvPageSize);

			if (_logger.isDebugEnabled())
				_logger.debug("----AllocatedKB = "+AllocatedKB+", PagesTouched = "+PagesTouched+", PagesRead = "+PagesRead+", SrvPageSize = "+SrvPageSize);

			// Handle divided by 0... (this happens if a engine goes offline
			BigDecimal calcCacheUtilization = null;
			BigDecimal calcCacheEfficiency  = null;

//			", CacheUtilization = convert(numeric( 4,1), PagesTouched / (AllocatedKB*(1024.0/@@maxpagesize)) * 100.0)" +
//			", CacheEfficiency  = convert(numeric(12,1), (AllocatedKB*(1024.0/@@maxpagesize)) / PagesRead    * 100.0)" +

			if( AllocatedKB == 0 )
			{
				calcCacheUtilization = new BigDecimal( 0 );
				calcCacheEfficiency  = new BigDecimal( 0 );
			}
			else
			{
				double dCacheUtilization = PagesTouched   / allocatedPages * 100.0;
				double dCacheEfficiency  = allocatedPages / PagesRead      * 100.0;

				if ( dCacheEfficiency > 100.0 )
					dCacheEfficiency = 100.0;

				calcCacheUtilization = new BigDecimal( dCacheUtilization ).setScale(1, BigDecimal.ROUND_HALF_EVEN);
				calcCacheEfficiency  = new BigDecimal( dCacheEfficiency  ).setScale(1, BigDecimal.ROUND_HALF_EVEN);
			}
			
			if (_logger.isDebugEnabled())
				_logger.debug("++++calcCacheUtilization = "+calcCacheUtilization+", calcCacheEfficiency = "+calcCacheEfficiency);
	
			diffData.setValueAt(calcCacheUtilization, rowId, CacheUtilizationId );
			diffData.setValueAt(calcCacheEfficiency,  rowId, CacheEfficiencyId  );


			// PhysicalReadsPct
			if (PhysicalReadsPctId >= 0 && PhysicalReadsId >= 0 && PagesPerIOId >= 0)
			{
				int PagesPerIO    = ((Number)diffData.getValueAt(rowId, PagesPerIOId)).intValue();
				int PhysicalReads = ((Number)diffData.getValueAt(rowId, PhysicalReadsId)).intValue();

				BigDecimal bdVal = (PagesRead <= 0) ? new BigDecimal(0) : new BigDecimal((1.0*PhysicalReads*PagesPerIO)/(1.0*PagesRead) * 100.0).setScale(1, BigDecimal.ROUND_HALF_EVEN);
				diffData.setValueAt(bdVal, rowId, PhysicalReadsPctId );
			}
			
			// APFReadsPct
			if (APFReadsPctId >= 0 && APFReadsId >= 0 && PagesPerIOId >= 0)
			{
				int PagesPerIO    = ((Number)diffData.getValueAt(rowId, PagesPerIOId)).intValue();
				int APFReads = ((Number)diffData.getValueAt(rowId, APFReadsId)).intValue();

				BigDecimal bdVal = (PagesRead <= 0) ? new BigDecimal(0) : new BigDecimal((1.0*APFReads*PagesPerIO)/(1.0*PagesRead) * 100.0).setScale(1, BigDecimal.ROUND_HALF_EVEN);
				diffData.setValueAt(bdVal, rowId, APFReadsPctId );
			}

			// CacheReplacementPct
			if (CacheReplacementPctId >= 0)
			{
				//SQL: "CacheReplacementPct        = convert(numeric(12,1), 1.0*PagesRead / (AllocatedKB*(1024.0/@@maxpagesize))), \n" +
				
				// PagesRead/AllocatedPages
				BigDecimal bdVal = new BigDecimal( 1.0*PagesRead / allocatedPages * 100.0).setScale(1, BigDecimal.ROUND_HALF_EVEN);
				diffData.setValueAt(bdVal, rowId, CacheReplacementPctId);
			}

			// CacheReplacementSlidePct, CacheEfficiencySlide and CacheSlideTime
			if (CacheReplacementSlidePctId >= 0 && CacheEfficiencySlideId >= 0 && CacheSlideTimeId >= 0)
			{
				String key = getAbsPkValue(rowId);
				
				// Add PagesRead to the "Slide Cache"
				LinkedList<CacheSlideEntry> list = _slideCache.get(key);
				if (list == null)
				{
					list = new LinkedList<CmCachePools.CacheSlideEntry>();
					_slideCache.put(key, list);
				}
				list.add(new CacheSlideEntry(newSample.getSampleTime(), PagesRead));
				
				// Remove entries the the "Slide Cache" that is older than X minutes
				removeOldCacheSlideEntries(list, newSample.getSampleTime());
				
				// Sum last X minutes in from the "Slide Cache" and get the "slide time"
				int slideSumPagesRead = sumCacheSlideEntries(list, newSample.getSampleTime());
				String timeStr        = getTimeSpanCacheSlideEntries(list, newSample.getSampleTime());

				BigDecimal cReplaceSlidePct  = new BigDecimal(                              slideSumPagesRead / allocatedPages * 100.0).setScale(1, BigDecimal.ROUND_HALF_EVEN);
				BigDecimal cEfficentSlidePct = new BigDecimal( slideSumPagesRead <= 0 ? 0 : allocatedPages / slideSumPagesRead * 100.0).setScale(1, BigDecimal.ROUND_HALF_EVEN);

				if ( cEfficentSlidePct.doubleValue() > 100.0 )
					cEfficentSlidePct = new BigDecimal(100.0);

				diffData.setValueAt(cReplaceSlidePct,  rowId, CacheReplacementSlidePctId);
				diffData.setValueAt(cEfficentSlidePct, rowId, CacheEfficiencySlideId);
				diffData.setValueAt(slideSumPagesRead, rowId, PagesReadInSlideId);
				diffData.setValueAt(timeStr,           rowId, CacheSlideTimeId);
			}
			
			if (CacheHitRateId >= 0 && LogicalReadsId >= 0)
			{
				// SQL: "CacheHitRate = convert(numeric(10,1), 100 - (LogicalReads*1.0/(PagesRead+1)) * 100.0), \n";

				int LogicalReads = ((Number)diffData.getValueAt(rowId, LogicalReadsId)).intValue();

				int usePctValueOnZeroReads = 0;
				if (Configuration.getCombinedConfiguration().getBooleanProperty(PROPKEY_CacheHitRateTo100PctOnZeroReads, DEFAULT_CacheHitRateTo100PctOnZeroReads))
					usePctValueOnZeroReads = 100;
				
				BigDecimal calc_CacheHitRate = new BigDecimal( LogicalReads <= 0 ? usePctValueOnZeroReads : (100.0 - (RealPagesRead*1.0/LogicalReads) * 100.0) ).setScale(1, BigDecimal.ROUND_HALF_EVEN);

				diffData.setValueAt(calc_CacheHitRate, rowId, CacheHitRateId );
			}
		}
	}
	private void removeOldCacheSlideEntries(LinkedList<CacheSlideEntry> list, Timestamp sampleTime)
	{
		long cacheSlideTimeInMs = 1000 * Configuration.getCombinedConfiguration().getIntProperty(PROPKEY_CacheSlideTimeInSec, DEFAULT_CacheSlideTimeInSec);

		while( ! list.isEmpty() )
		{
			long ageInMs = sampleTime.getTime() - list.getFirst()._sampleTime.getTime(); // note: list.add() is adding entries to the *end* of the list
			if (ageInMs > cacheSlideTimeInMs)
				list.removeFirst(); // note: list.add() is adding entries to the *end* of the list, so removeFirst() is removing the oldest entry 
			else
				break;
		}
	}
	private int sumCacheSlideEntries(LinkedList<CacheSlideEntry> list, Timestamp sampleTime)
	{
		int sum = 0;
		for (CacheSlideEntry entry : list)
			sum += entry._pagesRead;
		return sum;
	}
	private String getTimeSpanCacheSlideEntries(LinkedList<CacheSlideEntry> list, Timestamp sampleTime)
	{
		if (list.isEmpty())
			return "00:00:00";

		CacheSlideEntry firstEntry = list.getFirst(); // oldest entry
		CacheSlideEntry lastEntry  = list.getLast();  // last added

		long timeDiff = lastEntry._sampleTime.getTime() - firstEntry._sampleTime.getTime();
		return TimeUtils.msToTimeStr("%HH:%MM:%SS", timeDiff);
	}
}
