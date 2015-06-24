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
import com.asetune.gui.MainFrame;
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
		"</html>";

	public static final String   GROUP_NAME       = MainFrame.TCP_GROUP_CACHE;
	public static final String   GUI_ICON_FILE    = "images/"+CM_NAME+".png";

	public static final int      NEED_SRV_VERSION = 0;
	public static final int      NEED_CE_VERSION  = 0;

	public static final String[] MON_TABLES       = new String[] {"monCachePool"};
	public static final String[] NEED_ROLES       = new String[] {"mon_role"};
	public static final String[] NEED_CONFIG      = new String[] {"enable monitoring=1"};

	public static final String[] PCT_COLUMNS      = new String[] {"CacheUtilization", "CacheEfficiency"};
	public static final String[] DIFF_COLUMNS     = new String[] {
		"PagesRead", "PhysicalReads", "Stalls", "PagesTouched", "BuffersToMRU", "BuffersToLRU", 
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
	
	private void addTrendGraphs()
	{
	}

//	@Override
//	protected TabularCntrPanel createGui()
//	{
//		return new CmCachePoolsPanel(this);
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
			mtd.addColumn("monCachePool",  "Stalls",         "Number of times I/O operations were delayed because no clean buffers were available in the wash area");

			mtd.addColumn("monCachePool",  "SrvPageSize",    "ASE Servers page size (@@maxpagesize)");
			mtd.addColumn("monCachePool",  "PagesPerIO",     "This pools page size (1=SinglePage, 2=, 4=, 8=Extent IO)");
			mtd.addColumn("monCachePool",  "AllocatedPages", "Number of actual pages allocated to this pool. same as 'AllocatedKB' but in pages instead of KB.");
			
			mtd.addColumn("monCachePool",  "CacheUtilization", "<html>" +
			                                                       "If not 100% the cache has to much memory allocated to it.<br>" +
			                                                       "<b>Formula</b>: abs.PagesTouched / abs.AllocatedPages * 100<br>" +
			                                                   "</html>");
			mtd.addColumn("monCachePool",  "CacheEfficiency",  "<html>" +
			                                                       "If less than 100, the cache is to small (pages has been flushed ou from the cache).<br> " +
			                                                       "Pages are read in from the disk, could be by APF Reads (so cacheHitRate is high) but the pages still had to be read from disk.<br>" +
			                                                       "<b>Formula</b>: abs.AllocatedPages / diff.PagesRead * 100<br>" +
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
		pkCols.add("IOBufferSize");

		return pkCols;
	}

	@Override
	public String getSqlForVersion(Connection conn, int aseVersion, boolean isClusterEnabled)
	{
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
		String LogicalReads    = "";
		String PhysicalWrites  = "";
		String APFReads        = "";
		String APFPercentage   = "";
		String WashSize        = "";


//		if (aseVersion >= 15700)
//		if (aseVersion >= 1570000)
		if (aseVersion >= Ver.ver(15,7))
		{
			LogicalReads    = "LogicalReads, \n";
			PhysicalWrites  = "PhysicalWrites, \n";
			APFReads        = "APFReads, \n";
			APFPercentage   = "APFPercentage, \n";
			WashSize        = "WashSize, \n";
		}

		cols1 += "CacheName, \n" +
		         "CacheID, \n" +
		         "SrvPageSize = @@maxpagesize, \n" +
		         "IOBufferSize, \n" +
		         WashSize +
		         APFPercentage +
		         "PagesPerIO = IOBufferSize/@@maxpagesize, \n" +
		         "AllocatedKB, \n" +
		         "AllocatedPages = convert(int,AllocatedKB*(1024.0/@@maxpagesize)), \n" +
		         "PagesRead, \n" +
		         LogicalReads + 
		         APFReads +
		         "PhysicalReads, \n" +
		         PhysicalWrites +
		         "Stalls, \n" +
		         "PagesTouched, \n" +
		         "BuffersToMRU, \n" +
		         "BuffersToLRU, \n" +
		         "CacheUtilization = convert(numeric(12,1), PagesTouched / (AllocatedKB*(1024.0/@@maxpagesize)) * 100.0), \n" +
		         "CacheEfficiency  = CASE \n" +
		         "                      WHEN PagesRead > 0 \n" +
		         "                      THEN convert(numeric(12,1), (AllocatedKB*(1024.0/@@maxpagesize)) / PagesRead * 100.0) \n" +
		         "                      ELSE 0.0 \n" +
		         "                   END \n" +
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
		int AllocatedKB,        PagesTouched,        PagesRead,        SrvPageSize;
		int AllocatedKBId = -1, PagesTouchedId = -1, PagesReadId = -1, SrvPageSizeId = -1;

		int CacheUtilizationId = -1, CacheEfficiencyId = -1;
	
		// Find column Id's
		List<String> colNames = diffData.getColNames();
		if (colNames==null) return;
	
		for (int colId=0; colId < colNames.size(); colId++) 
		{
			String colName = colNames.get(colId);
			if      (colName.equals("AllocatedKB"))        AllocatedKBId        = colId;
			else if (colName.equals("PagesTouched"))       PagesTouchedId       = colId;
			else if (colName.equals("PagesRead"))          PagesReadId          = colId;
			else if (colName.equals("SrvPageSize"))        SrvPageSizeId        = colId;

			else if (colName.equals("CacheUtilization"))   CacheUtilizationId   = colId;
			else if (colName.equals("CacheEfficiency"))    CacheEfficiencyId    = colId;
		}
	
		// Loop on all diffData rows
		for (int rowId=0; rowId < diffData.getRowCount(); rowId++) 
		{
			AllocatedKB  = ((Number)newSample.getValueAt(rowId, AllocatedKBId )).intValue();
			PagesTouched = ((Number)newSample.getValueAt(rowId, PagesTouchedId)).intValue();
			PagesRead    = ((Number)diffData .getValueAt(rowId, PagesReadId   )).intValue();
			SrvPageSize  = ((Number)newSample.getValueAt(rowId, SrvPageSizeId )).intValue();

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
				double dCacheUtilization = PagesTouched / (AllocatedKB*(1024.0/SrvPageSize)) * 100.0;
				double dCacheEfficiency  = (AllocatedKB*(1024.0/SrvPageSize))    / PagesRead * 100.0;

				if ( dCacheEfficiency > 100.0 )
					dCacheEfficiency = 100.0;

				calcCacheUtilization = new BigDecimal( dCacheUtilization ).setScale(1, BigDecimal.ROUND_HALF_EVEN);
				calcCacheEfficiency  = new BigDecimal( dCacheEfficiency  ).setScale(1, BigDecimal.ROUND_HALF_EVEN);
			}

			if (_logger.isDebugEnabled())
				_logger.debug("++++calcCacheUtilization = "+calcCacheUtilization+", calcCacheEfficiency = "+calcCacheEfficiency);
	
			diffData.setValueAt(calcCacheUtilization, rowId, CacheUtilizationId );
			diffData.setValueAt(calcCacheEfficiency,  rowId, CacheEfficiencyId  );
		}
	}
}