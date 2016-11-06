package com.asetune.cm.ase;

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.Timestamp;
import java.util.LinkedHashMap;
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
import com.asetune.cm.ase.gui.CmCachePoolsPanel;
import com.asetune.config.dict.MonTablesDictionary;
import com.asetune.config.dict.MonTablesDictionaryManager;
import com.asetune.gui.MainFrame;
import com.asetune.gui.TabularCntrPanel;
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

	public static final int      NEED_SRV_VERSION = 0;
	public static final int      NEED_CE_VERSION  = 0;

	public static final String[] MON_TABLES       = new String[] {"monCachePool"};
	public static final String[] NEED_ROLES       = new String[] {"mon_role"};
	public static final String[] NEED_CONFIG      = new String[] {"enable monitoring=1"};

	public static final String[] PCT_COLUMNS      = new String[] {"PhysicalReadsPct", "APFReadsPct", "CacheUtilization", "CacheEfficiency", "CacheEfficiencySlide", "CacheReplacementPct", "CacheReplacementSlidePct", "CacheHitRate"};
	public static final String[] DIFF_COLUMNS     = new String[] {
		"PagesTouchedDiff", "UsedSizeInMbDiff", "UnUsedSizeInMbDiff", 
		"PagesRead", "PhysicalReads", "Stalls", "BuffersToMRU", "BuffersToLRU", 
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

	public static final String  PROPKEY_CacheSlideTimeInSec = PROP_PREFIX + ".CacheSlideTimeInSec";
	public static final int     DEFAULT_CacheSlideTimeInSec = 900;

	@Override
	protected void registerDefaultValues()
	{
		super.registerDefaultValues();

		Configuration.registerDefaultValue(PROPKEY_CacheSlideTimeInSec, DEFAULT_CacheSlideTimeInSec);
	}

	/** Used by the: Create 'Offline Session' Wizard */
	@Override
	public Configuration getLocalConfiguration()
	{
		Configuration conf = Configuration.getCombinedConfiguration();
		Configuration lc = new Configuration();

		lc.setProperty(PROPKEY_CacheSlideTimeInSec, conf.getIntProperty(PROPKEY_CacheSlideTimeInSec, DEFAULT_CacheSlideTimeInSec));
		
		return lc;
	}
	/** Used by the: Create 'Offline Session' Wizard */
	@Override
	public String getLocalConfigurationDescription(String propName)
	{
		if (propName.equals(PROPKEY_CacheSlideTimeInSec)) return "Set number of seconds the 'slide window time' will keep 'PagesRead' for.";
		return "";
	}
	@Override
	public String getLocalConfigurationDataType(String propName)
	{
		if (propName.equals(PROPKEY_CacheSlideTimeInSec)) return Integer.class.getSimpleName();
		return "";
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

	private void addTrendGraphs()
	{
	}

	@Override
	protected TabularCntrPanel createGui()
	{
		return new CmCachePoolsPanel(this);
	}

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
			
			mtd.addColumn("monCachePool",  "APFReadsPct",    "<html>" +
			                                                       "What's the percentage of the Async Prefetch Reads (APFReads) compared to PhysicalReads.<br>" +
			                                                       "<b>Formula</b>: APFReadsPct / PagesRead * 100<br>" +
			                                                 "</html>");

			mtd.addColumn("monCachePool",  "PhysicalReadsPct", "<html>" +
			                                                       "What's the percentage of the PhysicalReads compared to Async Prefetch Reads (APFReads).<br>" +
			                                                       "<b>Formula</b>: PhysicalReads / PagesRead * 100<br>" +
			                                                 "</html>");

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
			                                                       "<b>Formula</b>: 100 - (PagesRead/LogicalReads) * 100.0" +
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
		String LogicalReads     = "";
		String PhysicalReadsPct = "";
		String PhysicalWrites   = "";
		String APFReads         = "";
		String APFReadsPct      = "";
		String APFPercentage    = "";
		String WashSize         = "";
		String CacheHitRate     = "";


//		if (aseVersion >= 15700)
//		if (aseVersion >= 1570000)
		if (aseVersion >= Ver.ver(15,7))
		{
			LogicalReads     = "LogicalReads, \n";
			PhysicalWrites   = "PhysicalWrites, \n";
			PhysicalReadsPct = "PhysicalReadsPct = CASE WHEN (PagesRead > 0) THEN (((1.0*PhysicalReads*(IOBufferSize/@@maxpagesize))/(1.0*PagesRead)) * 100.0) ELSE 0.0 END,\n";
			APFReads         = "APFReads, \n";
			APFReadsPct      = "APFReadsPct = CASE WHEN (PagesRead > 0) THEN (((1.0*APFReads*(IOBufferSize/@@maxpagesize))/(1.0*PagesRead)) * 100.0) ELSE 0.0 END,\n";
			APFPercentage    = "APFPercentage, \n";
			WashSize         = "WashSize, \n";
			CacheHitRate     = "CacheHitRate = convert(numeric(10,1), 100 - (PagesRead*1.0/(LogicalReads+1)) * 100.0), \n";
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
		         "AllocatedPages     = convert(int,AllocatedKB*(1024.0/@@maxpagesize)), \n" +
		         "PagesTouchedDiff   = PagesTouched, \n" +
		         "UsedSizeInMbDiff   = convert(int, PagesTouched / (1024*1024/@@maxpagesize) ), \n" +
		         "UnUsedSizeInMbDiff = convert(int, ((AllocatedKB*(1024.0/@@maxpagesize)) - PagesTouched) / (1024*1024/@@maxpagesize) ), \n" +
		         "PagesTouched, \n" +
		         "UsedSizeInMb       = convert(int, PagesTouched / (1024*1024/@@maxpagesize) ), \n" +
		         "UnUsedSizeInMb     = convert(int, ((AllocatedKB*(1024.0/@@maxpagesize)) - PagesTouched) / (1024*1024/@@maxpagesize) ), \n" +
		         "CacheUtilization   = convert(numeric(12,1), PagesTouched / (AllocatedKB*(1024.0/@@maxpagesize)) * 100.0), \n" +
		         LogicalReads + 
		         "PagesRead, \n" +
		         CacheHitRate +
		         APFReads +
		         APFReadsPct +
		         "PhysicalReads, \n" +
		         PhysicalReadsPct + 
		         PhysicalWrites +
		         "Stalls, \n" +
		         "BuffersToMRU, \n" +
		         "BuffersToLRU, \n" +
		         "CacheReplacementPct        = convert(numeric(12,1), 1.0*PagesRead / (AllocatedKB*(1024.0/@@maxpagesize))), \n" +
		         "CacheReplacementSlidePct   = convert(numeric(12,1), 0), \n" +
		         "CacheSlideTime             = convert(varchar(30), 'not-for-absulute-values'), \n" +
		         "PagesReadInSlide           = convert(int, 0), \n" +
		         "CacheEfficiency  = CASE \n" +
		         "                      WHEN PagesRead > 0 \n" +
		         "                      THEN convert(numeric(12,1), (AllocatedKB*(1024.0/@@maxpagesize)) / PagesRead * 100.0) \n" +
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
		int AllocatedKB,        PagesTouched,        PagesRead,        SrvPageSize;
		int AllocatedKBId = -1, PagesTouchedId = -1, PagesReadId = -1, SrvPageSizeId = -1;

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
				
				BigDecimal bdVal = new BigDecimal( 1.0*PagesRead / (AllocatedKB*(1024.0/SrvPageSize)) * 100.0).setScale(1, BigDecimal.ROUND_HALF_EVEN);
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

				BigDecimal cReplaceSlidePct  = new BigDecimal(                              slideSumPagesRead / (AllocatedKB*(1024.0/SrvPageSize)) * 100.0).setScale(1, BigDecimal.ROUND_HALF_EVEN);
				BigDecimal cEfficentSlidePct = new BigDecimal( slideSumPagesRead <= 0 ? 0 : (AllocatedKB*(1024.0/SrvPageSize)) / slideSumPagesRead * 100.0).setScale(1, BigDecimal.ROUND_HALF_EVEN);

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

				BigDecimal calc_CacheHitRate = new BigDecimal( LogicalReads <= 0 ? 0 : (100.0 - (PagesRead*1.0/LogicalReads) * 100.0) ).setScale(1, BigDecimal.ROUND_HALF_EVEN);

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
