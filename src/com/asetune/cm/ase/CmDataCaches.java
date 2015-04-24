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
import com.asetune.graph.TrendGraphDataPoint;
import com.asetune.gui.MainFrame;
import com.asetune.gui.TrendGraph;
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

	public static final String[] PCT_COLUMNS      = new String[] {"CacheHitRate"};
	public static final String[] DIFF_COLUMNS     = new String[] {
		"CacheHitRate", "CacheSearches", 
		"PhysicalReads", "LogicalReads", "PhysicalWrites", "Stalls", "APFReads"};

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
		super(CM_NAME, GROUP_NAME, /*sql*/null, /*pkList*/null, 
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
	
	public static final String GRAPH_NAME_CACHE_ACTIVITY = "CacheGraph"; //String x=GetCounters.CM_GRAPH_NAME__DATA_CACHE__ACTIVITY;

	private void addTrendGraphs()
	{
		String[] labels = new String[] { "Logical Reads", "Physical Reads", "Writes" };
		
		addTrendGraphData(GRAPH_NAME_CACHE_ACTIVITY, new TrendGraphDataPoint(GRAPH_NAME_CACHE_ACTIVITY, labels));

		// if GUI
		if (getGuiController() != null && getGuiController().hasGUI())
		{
			// GRAPH
			TrendGraph tg = null;
			tg = new TrendGraph(GRAPH_NAME_CACHE_ACTIVITY,
				"Data Caches Activity", 	               // Menu CheckBox text
				"Activity for All Data Caches per Second", // Label 
				labels, 
				false, // is Percent Graph
				this, 
				true,  // visible at start
				0,     // graph is valid from Server Version. 0 = All Versions; >0 = Valid from this version and above 
				-1);  // minimum height
			addTrendGraph(tg.getName(), tg, true);
		}
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
			MonTablesDictionary mtd = MonTablesDictionary.getInstance();
			mtd.addColumn("monDataCache",  "Stalls",       "Number of times I/O operations were delayed because no clean buffers were available in the wash area");

			mtd.addColumn("monDataCache",  "CacheHitRate", "<html>" +
			                                               "Percent calculation of how many pages was fetched from the cache.<br>" +
			                                               "<b>Note</b>: APF reads could already be in memory, counted as a 'cache hit', check also 'devices' and APFReads.<br>" +
			                                               "<b>Formula</b>: 100 - (PhysicalReads/CacheSearches) * 100.0" +
			                                               "</html>");
//			mtd.addColumn("monDataCache",  "Misses",       "fixme");
//			mtd.addColumn("monDataCache",  "Volatility",   "fixme");
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

//		if (aseVersion >= 15700)
//		if (aseVersion >= 1570000)
		if (aseVersion >= Ver.ver(15,7))
		{
			Status              = "Status, ";
			Type                = "Type, ";
			CacheSize           = "CacheSize, ";
			ReplacementStrategy = "ReplacementStrategy, ";
			APFReads            = "APFReads, ";
			Overhead            = "Overhead, ";
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
			tgdp.setDate(this.getTimestamp());
			tgdp.setData(arr);
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
	}
}