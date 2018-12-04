package com.asetune.cm.ase;

import java.math.BigDecimal;
import java.sql.Connection;
import java.util.LinkedList;
import java.util.List;

import javax.naming.NameNotFoundException;

import org.apache.log4j.Logger;

import com.asetune.ICounterController;
import com.asetune.IGuiController;
import com.asetune.cm.CounterSample;
import com.asetune.cm.CounterSetTemplates;
import com.asetune.cm.CounterSetTemplates.Type;
import com.asetune.cm.CountersModel;
import com.asetune.config.dict.MonTablesDictionary;
import com.asetune.config.dict.MonTablesDictionaryManager;
import com.asetune.graph.TrendGraphDataPoint;
import com.asetune.graph.TrendGraphDataPoint.LabelType;
import com.asetune.gui.MainFrame;
import com.asetune.utils.Ver;

/**
 * @author Goran Schwarz (goran_schwarz@hotmail.com)
 */
public class CmDataCaches
extends CountersModel
{
	private static Logger        _logger          = Logger.getLogger(CmDataCaches.class);
	private static final long    serialVersionUID = 1L;

	public static final String   CM_NAME          = CmDataCaches.class.getSimpleName();
	public static final String   SHORT_NAME       = "Caches";
	public static final String   HTML_DESC        = 
		"<html>" +
		"What (user defined) data caches have we got and how many 'chache misses' goes out to disk..." +
		"</html>";

	public static final String   GROUP_NAME       = MainFrame.TCP_GROUP_CACHE;
	public static final String   GUI_ICON_FILE    = "images/"+CM_NAME+".png";

	public static final int      NEED_SRV_VERSION = 0;
	public static final int      NEED_CE_VERSION  = 0;

	public static final String[] MON_TABLES       = new String[] {"monDataCache"};
	public static final String[] NEED_ROLES       = new String[] {"mon_role"};
	public static final String[] NEED_CONFIG      = new String[] {"enable monitoring=1"};

	public static final String[] PCT_COLUMNS      = new String[] {"CacheHitRate", "CASContention"};
	public static final String[] DIFF_COLUMNS     = new String[] {
		"CacheHitRate", "CacheSearches", 
		"PhysicalReads", "LogicalReads", "PhysicalWrites", "Stalls", "APFReads",
		"CASGrabs", "CASSpins", "CASWaits"};

	public static final boolean  NEGATIVE_DIFF_COUNTERS_TO_ZERO = true;
	public static final boolean  IS_SYSTEM_CM                   = true;
	public static final int      DEFAULT_POSTPONE_TIME          = 0;
	public static final int      DEFAULT_QUERY_TIMEOUT          = CountersModel.DEFAULT_sqlQueryTimeout;

	@Override public int     getDefaultPostponeTime()                 { return DEFAULT_POSTPONE_TIME; }
	@Override public int     getDefaultQueryTimeout()                 { return DEFAULT_QUERY_TIMEOUT; }
	@Override public boolean getDefaultIsNegativeDiffCountersToZero() { return NEGATIVE_DIFF_COUNTERS_TO_ZERO; }
	@Override public Type    getTemplateLevel()                       { return Type.SMALL; }

	/**
	 * FACTORY  method to create the object
	 */
	public static CountersModel create(ICounterController counterController, IGuiController guiController)
	{
		if (guiController != null && guiController.hasGUI())
			guiController.splashWindowProgress("Loading: Counter Model '"+CM_NAME+"'");

		return new CmDataCaches(counterController, guiController);
	}

	public CmDataCaches(ICounterController counterController, IGuiController guiController)
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
	
	public static final String GRAPH_NAME_CACHE_ACTIVITY       = "CacheGraph";
	public static final String GRAPH_NAME_CACHE_LOGICAL_READS  = "CacheLReads";
	public static final String GRAPH_NAME_CACHE_PHYSICAL_READS = "CachePReads";
	public static final String GRAPH_NAME_CACHE_APF_READS      = "CacheApfReads";
	public static final String GRAPH_NAME_CACHE_WRITES         = "CacheWrites";

	private void addTrendGraphs()
	{
//		String[] labels  = new String[] { "Logical Reads", "Physical Reads", "Writes" };
//		String[] dynLbls = TrendGraphDataPoint.RUNTIME_REPLACED_LABELS;
//		
//		addTrendGraphData(GRAPH_NAME_CACHE_ACTIVITY,       new TrendGraphDataPoint(GRAPH_NAME_CACHE_ACTIVITY,       labels,  LabelType.Static));
//		addTrendGraphData(GRAPH_NAME_CACHE_LOGICAL_READS,  new TrendGraphDataPoint(GRAPH_NAME_CACHE_LOGICAL_READS,  dynLbls, LabelType.Dynamic));
//		addTrendGraphData(GRAPH_NAME_CACHE_PHYSICAL_READS, new TrendGraphDataPoint(GRAPH_NAME_CACHE_PHYSICAL_READS, dynLbls, LabelType.Dynamic));
//		addTrendGraphData(GRAPH_NAME_CACHE_APF_READS,      new TrendGraphDataPoint(GRAPH_NAME_CACHE_APF_READS,      dynLbls, LabelType.Dynamic));
//		addTrendGraphData(GRAPH_NAME_CACHE_WRITES,         new TrendGraphDataPoint(GRAPH_NAME_CACHE_WRITES,         dynLbls, LabelType.Dynamic));

		addTrendGraph(GRAPH_NAME_CACHE_ACTIVITY,
			"Data Caches Activity", 	               // Menu CheckBox text
			"Activity for All Data Caches per Second ("+GROUP_NAME+"->"+SHORT_NAME+")", // Label 
			new String[] { "Logical Reads", "Physical Reads", "Writes" }, 
			LabelType.Static,
			TrendGraphDataPoint.Category.CACHE,
			false, // is Percent Graph
			true,  // visible at start
			0,     // graph is valid from Server Version. 0 = All Versions; >0 = Valid from this version and above 
			-1);  // minimum height

		// GRAPH
		addTrendGraph(GRAPH_NAME_CACHE_LOGICAL_READS,
			"Data Caches LogicalReads", 	               // Menu CheckBox text
			"Data Caches LogicalReads per Second ("+GROUP_NAME+"->"+SHORT_NAME+")", // Label 
			null, 
			LabelType.Dynamic,
			TrendGraphDataPoint.Category.CACHE,
			false, // is Percent Graph
			false,  // visible at start
			0,     // graph is valid from Server Version. 0 = All Versions; >0 = Valid from this version and above 
			-1);  // minimum height

		// GRAPH
		addTrendGraph(GRAPH_NAME_CACHE_PHYSICAL_READS,
			"Data Caches PhysicalReads", 	               // Menu CheckBox text
			"Data Caches PhysicalReads per Second ("+GROUP_NAME+"->"+SHORT_NAME+")", // Label 
			null, 
			LabelType.Dynamic,
			TrendGraphDataPoint.Category.CACHE,
			false, // is Percent Graph
			false,  // visible at start
			0,     // graph is valid from Server Version. 0 = All Versions; >0 = Valid from this version and above 
			-1);  // minimum height

		// GRAPH
		addTrendGraph(GRAPH_NAME_CACHE_APF_READS,
			"Data Caches ApfReads", 	               // Menu CheckBox text
			"Data Caches ApfReads per Second ("+GROUP_NAME+"->"+SHORT_NAME+")", // Label 
			null, 
			LabelType.Dynamic,
			TrendGraphDataPoint.Category.CACHE,
			false, // is Percent Graph
			false,  // visible at start
			Ver.ver(15,7),     // graph is valid from Server Version. 0 = All Versions; >0 = Valid from this version and above 
			-1);  // minimum height

		// GRAPH
		addTrendGraph(GRAPH_NAME_CACHE_WRITES,
			"Data Caches Writes", 	               // Menu CheckBox text
			"Data Caches Writes per Second ("+GROUP_NAME+"->"+SHORT_NAME+")", // Label 
			null, 
			LabelType.Dynamic,
			TrendGraphDataPoint.Category.CACHE,
			false, // is Percent Graph
			false,  // visible at start
			0,     // graph is valid from Server Version. 0 = All Versions; >0 = Valid from this version and above 
			-1);  // minimum height

//		// if GUI
//		if (getGuiController() != null && getGuiController().hasGUI())
//		{
//			TrendGraph tg = null;
//
//			// GRAPH
//			tg = new TrendGraph(GRAPH_NAME_CACHE_ACTIVITY,
//				"Data Caches Activity", 	               // Menu CheckBox text
//				"Activity for All Data Caches per Second ("+GROUP_NAME+"->"+SHORT_NAME+")", // Label 
//				labels, 
//				false, // is Percent Graph
//				this, 
//				true,  // visible at start
//				0,     // graph is valid from Server Version. 0 = All Versions; >0 = Valid from this version and above 
//				-1);  // minimum height
//			addTrendGraph(tg.getName(), tg, true);
//
//			// GRAPH
//			tg = new TrendGraph(GRAPH_NAME_CACHE_LOGICAL_READS,
//				"Data Caches LogicalReads", 	               // Menu CheckBox text
//				"Data Caches LogicalReads per Second ("+GROUP_NAME+"->"+SHORT_NAME+")", // Label 
//				labels, 
//				false, // is Percent Graph
//				this, 
//				false,  // visible at start
//				0,     // graph is valid from Server Version. 0 = All Versions; >0 = Valid from this version and above 
//				-1);  // minimum height
//			addTrendGraph(tg.getName(), tg, true);
//
//			// GRAPH
//			tg = new TrendGraph(GRAPH_NAME_CACHE_PHYSICAL_READS,
//				"Data Caches PhysicalReads", 	               // Menu CheckBox text
//				"Data Caches PhysicalReads per Second ("+GROUP_NAME+"->"+SHORT_NAME+")", // Label 
//				labels, 
//				false, // is Percent Graph
//				this, 
//				false,  // visible at start
//				0,     // graph is valid from Server Version. 0 = All Versions; >0 = Valid from this version and above 
//				-1);  // minimum height
//			addTrendGraph(tg.getName(), tg, true);
//
//			// GRAPH
//			tg = new TrendGraph(GRAPH_NAME_CACHE_APF_READS,
//				"Data Caches ApfReads", 	               // Menu CheckBox text
//				"Data Caches ApfReads per Second ("+GROUP_NAME+"->"+SHORT_NAME+")", // Label 
//				labels, 
//				false, // is Percent Graph
//				this, 
//				false,  // visible at start
//				Ver.ver(15,7),     // graph is valid from Server Version. 0 = All Versions; >0 = Valid from this version and above 
//				-1);  // minimum height
//			addTrendGraph(tg.getName(), tg, true);
//
//			// GRAPH
//			tg = new TrendGraph(GRAPH_NAME_CACHE_WRITES,
//				"Data Caches Writes", 	               // Menu CheckBox text
//				"Data Caches Writes per Second ("+GROUP_NAME+"->"+SHORT_NAME+")", // Label 
//				labels, 
//				false, // is Percent Graph
//				this, 
//				false,  // visible at start
//				0,     // graph is valid from Server Version. 0 = All Versions; >0 = Valid from this version and above 
//				-1);  // minimum height
//			addTrendGraph(tg.getName(), tg, true);
//		}
	}

//	@Override
//	protected TabularCntrPanel createGui()
//	{
//		return new CmDataCachesPanel(this);
//	}

	@Override
	public String[] getDependsOnConfigForVersion(Connection conn, int srvVersion, boolean isClusterEnabled)
	{
		return NEED_CONFIG;
	}

	@Override
	public void addMonTableDictForVersion(Connection conn, int aseVersion, boolean isClusterEnabled)
	{
		try 
		{
			MonTablesDictionary mtd = MonTablesDictionaryManager.getInstance();
			mtd.addColumn("monDataCache",  "Stalls",       "Number of times I/O operations were delayed because no clean buffers were available in the wash area");

			mtd.addColumn("monDataCache",  "CacheHitRate", "<html>" +
			                                                   "Percent calculation of how many pages was fetched from the cache.<br>" +
			                                                   "<b>Note</b>: APF reads could already be in memory, counted as a 'cache hit', check also 'devices' and APFReads.<br>" +
			                                                   "<b>Formula</b>: 100 - (PhysicalReads/CacheSearches) * 100.0" +
			                                               "</html>");
//			mtd.addColumn("monDataCache",  "Misses",       "fixme");
//			mtd.addColumn("monDataCache",  "Volatility",   "fixme");
			mtd.addColumn("monDataCache",  "CASContention", "<html>" +
			                                                    "Percent calculation the contention.<br>" +
			                                                    "<b>Note</b>: This is calulated for both <i>Absolute</i>, <i>Difference</i> and <i>Rate</i> values.<br>" +
			                                                    "<b>Formula</b>: (CASWaits/CASGrabs) * 100.0" +
			                                                "</html>");
			mtd.addColumn("monDataCache",  "CASSpinsPerWait", "<html>" +
			                                                    "How many <i>spins</i> did we do for every <i>wait</i> .<br>" +
			                                                    "<b>Note</b>: This is calulated for both <i>Absolute</i>, <i>Difference</i> and <i>Rate</i> values.<br>" +
			                                                    "<b>Formula</b>: CASSpins / CASWaits" +
			                                                "</html>");
		}
		catch (NameNotFoundException e) {/*ignore*/}
	}

	@Override
	public List<String> getPkForVersion(Connection conn, int srvVersion, boolean isClusterEnabled)
	{
		List <String> pkCols = new LinkedList<String>();

		if (isClusterEnabled)
			pkCols.add("InstanceID");

		pkCols.add("CacheName");

		return pkCols;
	}

	@Override
	public String getSqlForVersion(Connection conn, int aseVersion, boolean isClusterEnabled)
	{
		String cols1, cols2, cols3;
		cols1 = cols2 = cols3 = "";

		//------------- NEEDS TO BE CALCULATED AFTER EACH SAMPLE
		// HitRate    = CacheSearches  / Logical Reads
		// Misses     = CacheSearches  / Physical Reads
		// Volatility = PhysicalWrites / (PhysicalReads + LogicalReads)
		
		// ASE 15.7
		String Status              = "";
		String Type                = "";
		String CacheSize           = "";
		String ReplacementStrategy = "";
		String APFReads            = "";
		String Overhead            = "";

		// ASE 16.0 SP2
		String CASGrabs            = "";
		String CASSpins            = "";
		String CASWaits            = "";
		String CASContention       = ""; // calculated column
		String CASSpinsPerWait     = ""; // calculated column
		String nl_160_sp2          = ""; // new line

		if (aseVersion >= Ver.ver(15,7))
		{
			Status              = "Status, ";
			Type                = "Type, ";
			CacheSize           = "CacheSize, ";
			ReplacementStrategy = "ReplacementStrategy, ";
			APFReads            = "APFReads, ";
			Overhead            = "Overhead, ";
		}

		if (aseVersion >= Ver.ver(16,0,0, 2))
		{
			CASGrabs            = "CASGrabs, ";
			CASSpins            = "CASSpins, ";
			CASWaits            = "CASWaits, ";
//			CASContention       = "CASContention = convert(numeric(4,1), 0.0), ";
			CASContention       = "CASContention   = CASE \n" +
			                      "                      WHEN CASGrabs > 0 \n" +
			                      "                      THEN convert(numeric(12,2), ((CASWaits + 0.0) / (CASGrabs + 0.0)) * 100.0) \n" +
			                      "                      ELSE convert(numeric(12,2), 0.0) \n" +
			                      "                  END,";
			CASSpinsPerWait     = "CASSpinsPerWait = CASE \n" +
			                      "                      WHEN CASWaits > 0 \n" +
			                      "                      THEN convert(numeric(12,1), (CASSpins + 0.0) / (CASWaits + 0.0) ) \n" +
			                      "                      ELSE convert(numeric(12,1), 0.0 ) \n" +
			                      "                  END,";
			nl_160_sp2 = "\n";
		}

		if (isClusterEnabled)
		{
			cols1 += "InstanceID, ";
		}

		cols1 += "CacheName, CacheID, " +
		         Status + Type + CacheSize + ReplacementStrategy + "\n" +
		         "RelaxedReplacement, CachePartitions, BufferPools, \n" +
		         "CacheSearches, PhysicalReads, LogicalReads, PhysicalWrites, Stalls, \n" +
		         APFReads + Overhead +
		         CASGrabs + CASSpins + CASWaits + nl_160_sp2 +
		         CASContention + nl_160_sp2 +
		         CASSpinsPerWait + nl_160_sp2 +
		         "CacheHitRate = convert(numeric(10,1), 100 - (PhysicalReads*1.0/(CacheSearches+1)) * 100.0)" +
//		         ", HitRate    = convert(numeric(10,1), (CacheSearches * 1.0 / LogicalReads) * 100)" +
//		         ", Misses     = convert(numeric(10,1), (CacheSearches * 1.0 / PhysicalReads) * 1)" +
//		         ", Volatility = convert(numeric(10,1), PhysicalWrites * 1.0 / (PhysicalReads + LogicalReads)* 1)"
		         "";

		String sql = 
			"select " + cols1 + cols2 + cols3 + "\n" +
			"from master..monDataCache \n" +
			"order by 1,2\n";

		return sql;
	}

	@Override
	public void updateGraphData(TrendGraphDataPoint tgdp)
	{
		if (GRAPH_NAME_CACHE_ACTIVITY.equals(tgdp.getName()))
		{
			Double[] arr = new Double[3];

			arr[0] = this.getRateValueSum("LogicalReads");
			arr[1] = this.getRateValueSum("PhysicalReads");
			arr[2] = this.getRateValueSum("PhysicalWrites");
			_logger.debug("updateGraphData(CacheGraph): LogicalReads='"+arr[0]+"', PhysicalReads='"+arr[1]+"', PhysicalWrites='"+arr[2]+"'.");

			// Set the values
			tgdp.setDataPoint(this.getTimestamp(), arr);
		}

		if (GRAPH_NAME_CACHE_LOGICAL_READS.equals(tgdp.getName()))
		{
			// Write 1 "line" for every named cache
			Double[] dArray = new Double[this.size()];
			String[] lArray = new String[dArray.length];
			for (int i = 0; i < dArray.length; i++)
			{
				lArray[i] = this.getRateString       (i, "CacheName");
				dArray[i] = this.getRateValueAsDouble(i, "LogicalReads");
			}

			// Set the values
			tgdp.setDataPoint(this.getTimestamp(), lArray, dArray);
		}

		if (GRAPH_NAME_CACHE_PHYSICAL_READS.equals(tgdp.getName()))
		{
			// Write 1 "line" for every named cache
			Double[] dArray = new Double[this.size()];
			String[] lArray = new String[dArray.length];
			for (int i = 0; i < dArray.length; i++)
			{
				lArray[i] = this.getRateString       (i, "CacheName");
				dArray[i] = this.getRateValueAsDouble(i, "PhysicalReads");
			}

			// Set the values
			tgdp.setDataPoint(this.getTimestamp(), lArray, dArray);
		}

		if (GRAPH_NAME_CACHE_APF_READS.equals(tgdp.getName()))
		{
			// Write 1 "line" for every named cache
			Double[] dArray = new Double[this.size()];
			String[] lArray = new String[dArray.length];
			for (int i = 0; i < dArray.length; i++)
			{
				lArray[i] = this.getRateString       (i, "CacheName");
				dArray[i] = this.getRateValueAsDouble(i, "APFReads");
			}

			// Set the values
			tgdp.setDataPoint(this.getTimestamp(), lArray, dArray);
		}

		if (GRAPH_NAME_CACHE_WRITES.equals(tgdp.getName()))
		{
			// Write 1 "line" for every named cache
			Double[] dArray = new Double[this.size()];
			String[] lArray = new String[dArray.length];
			for (int i = 0; i < dArray.length; i++)
			{
				lArray[i] = this.getRateString       (i, "CacheName");
				dArray[i] = this.getRateValueAsDouble(i, "PhysicalWrites");
			}

			// Set the values
			tgdp.setDataPoint(this.getTimestamp(), lArray, dArray);
		}
	}
	/** 
	 * Compute 
	 */
	@Override
	public void localCalculation(CounterSample prevSample, CounterSample newSample, CounterSample diffData)
	{
//		int CacheSearches,        LogicalReads,        PhysicalReads,        PhysicalWrites;
//		int CacheSearchesId = -1, LogicalReadsId = -1, PhysicalReadsId = -1, PhysicalWritesId = -1;
//
//		int CacheHitRateId = -1, MissesId = -1, VolatilityId = -1;
	
		int CacheSearches,        PhysicalReads;
		int CacheSearchesId = -1, PhysicalReadsId = -1;

		int CacheHitRateId = -1;

		// Find column Id's
		List<String> colNames = diffData.getColNames();
		if (colNames==null) return;
	
		for (int colId=0; colId < colNames.size(); colId++) 
		{
			String colName = colNames.get(colId);
			if      (colName.equals("CacheSearches"))  CacheSearchesId   = colId;
//			else if (colName.equals("LogicalReads"))   LogicalReadsId    = colId;
			else if (colName.equals("PhysicalReads"))  PhysicalReadsId   = colId;
//			else if (colName.equals("PhysicalWrites")) PhysicalWritesId  = colId;

			else if (colName.equals("CacheHitRate"))   CacheHitRateId    = colId;
//			else if (colName.equals("Misses"))         MissesId          = colId;
//			else if (colName.equals("Volatility"))     VolatilityId      = colId;
		}
	
		// Loop on all diffData rows
		for (int rowId=0; rowId < diffData.getRowCount(); rowId++) 
		{
			CacheSearches  = ((Number)diffData.getValueAt(rowId, CacheSearchesId )).intValue();
//			LogicalReads   = ((Number)diffData.getValueAt(rowId, LogicalReadsId  )).intValue();
			PhysicalReads  = ((Number)diffData.getValueAt(rowId, PhysicalReadsId )).intValue();
//			PhysicalWrites = ((Number)diffData.getValueAt(rowId, PhysicalWritesId)).intValue();

//			CacheHitRate   = ((Number)diffData.getValueAt(rowId, CacheHitRateId  )).intValue();
//			Misses         = ((Number)diffData.getValueAt(rowId, MissesId        )).intValue();
//			Volatility     = ((Number)diffData.getValueAt(rowId, VolatilityId    )).intValue();

//			if (_logger.isDebugEnabled())
//				_logger.debug("----CacheSearches = "+CacheSearches+", LogicalReads = "+LogicalReads+", PhysicalReads = "+PhysicalReads+", PhysicalWrites = "+PhysicalWrites);
			if (_logger.isDebugEnabled())
				_logger.debug("----CacheSearches = "+CacheSearches+", PhysicalReads = "+PhysicalReads);

			// Handle divided by 0... (this happens if a engine goes offline
			BigDecimal calcCacheHitRate = null;
//			BigDecimal calcMisses       = null;
//			BigDecimal calcVolatility   = null;

//			", CacheHitRate    = convert(numeric(4,1), 100 - (PhysicalReads*1.0/CacheSearches) * 100.0)" +
//			", CacheHitRate    = convert(numeric(4,1), (CacheSearches * 1.0 / LogicalReads) * 100)" +
//			", Misses     = CacheSearches  * 1.0 / PhysicalReads" +
//			", Volatility = PhysicalWrites * 1.0 / (PhysicalReads + LogicalReads)";

			if (CacheSearches == 0)
				calcCacheHitRate    = new BigDecimal( 0 );
			else
				calcCacheHitRate    = new BigDecimal( 100.0 - (PhysicalReads*1.0/CacheSearches) * 100.0 ).setScale(1, BigDecimal.ROUND_HALF_EVEN);
//				calcCacheHitRate    = new BigDecimal( ((1.0 * CacheSearches) / LogicalReads) * 100 ).setScale(1, BigDecimal.ROUND_HALF_EVEN);
			
//			if (PhysicalReads == 0)
//				calcMisses     = new BigDecimal( 0 );
//			else
//				calcMisses     = new BigDecimal( ((1.0 * CacheSearches) / PhysicalReads) * 1 ).setScale(1, BigDecimal.ROUND_HALF_EVEN);
//			
//			if ((PhysicalReads + LogicalReads) == 0)
//				calcVolatility = new BigDecimal( 0 );
//			else
//				calcVolatility = new BigDecimal( ((1.0 * PhysicalWrites) / (PhysicalReads + LogicalReads)) * 1 ).setScale(1, BigDecimal.ROUND_HALF_EVEN);
//
//			if (_logger.isDebugEnabled())
//				_logger.debug("++++CacheHitRate = "+calcCacheHitRate+", Misses = "+calcMisses+", Volatility = "+calcVolatility);
			
			if (_logger.isDebugEnabled())
				_logger.debug("++++CacheHitRate = "+calcCacheHitRate);
	
			diffData.setValueAt(calcCacheHitRate, rowId, CacheHitRateId );
//			diffData.setValueAt(calcMisses,       rowId, MissesId       );
//			diffData.setValueAt(calcVolatility,   rowId, VolatilityId   );

		}
		
		// do calculations for CAS in ASE 16.0 SP2 and above
		localCalculation_CAS(prevSample, newSample, diffData);
	}

	public void localCalculation_CAS(CounterSample prevSample, CounterSample newSample, CounterSample diffData)
	{
		long Grabs, Waits, Spins;
		int  pos_Grabs = -1, pos_Waits = -1, pos_Spins = -1, pos_Contention = -1, pos_SpinsPerWait = -1;

		// Find column Id's
		List<String> colNames = diffData.getColNames();
		if (colNames == null)
			return;
		for (int colId = 0; colId < colNames.size(); colId++)
		{
			String colName = (String) colNames.get(colId);
			if      (colName.equals("CASGrabs"))        pos_Grabs        = colId;
			else if (colName.equals("CASWaits"))        pos_Waits        = colId;
			else if (colName.equals("CASSpins"))        pos_Spins        = colId;
			else if (colName.equals("CASContention"))   pos_Contention   = colId;
			else if (colName.equals("CASSpinsPerWait")) pos_SpinsPerWait = colId;
		}
		
		// If the CAS columns are not found, just get out of here... it's not ASE 16.0 SP2 or above
		if (pos_Grabs == -1 || pos_Waits == -1 || pos_Spins == -1 || pos_Contention == -1 || pos_SpinsPerWait == -1)
			return;

		// Loop on all diffData rows
		for (int rowId = 0; rowId < diffData.getRowCount(); rowId++)
		{
			Grabs = ((Number) diffData.getValueAt(rowId, pos_Grabs)).longValue();
			Waits = ((Number) diffData.getValueAt(rowId, pos_Waits)).longValue();
			Spins = ((Number) diffData.getValueAt(rowId, pos_Spins)).longValue();

			// contention
			if (Grabs > 0)
			{
				BigDecimal contention = new BigDecimal( ((1.0 * (Waits)) / Grabs) * 100 ).setScale(1, BigDecimal.ROUND_HALF_EVEN);

				// Keep only 3 decimals
				// row.set(AvgServ_msId, new Double (AvgServ_ms/1000) );
				diffData.setValueAt(contention, rowId, pos_Contention);
			}
			else
				diffData.setValueAt(new BigDecimal(0), rowId, pos_Contention);

			// spinsPerWait
			if (Waits > 0)
			{
				BigDecimal spinWarning = new BigDecimal( ((1.0 * (Spins)) / Waits) ).setScale(1, BigDecimal.ROUND_HALF_EVEN);

				diffData.setValueAt(spinWarning, rowId, pos_SpinsPerWait);
			}
			else
				diffData.setValueAt(new BigDecimal(0), rowId, pos_SpinsPerWait);

		}
	}
}