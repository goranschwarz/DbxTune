package com.asetune.sp_sysmon;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Set;

import com.asetune.CounterController;
import com.asetune.cm.CountersModel;
import com.asetune.cm.ase.CmSpinlockSum;
import com.asetune.utils.StringUtil;

public class DataCache extends AbstractSysmonType
{
	public DataCache(SpSysmon sysmon, CountersModel cm)
	{
		super(sysmon, cm);
	}

	public DataCache(SpSysmon sysmon, int aseVersion, int sampleTimeInMs, List<List<Object>> data, int fieldName_pos, int groupName_pos, int instanceId_pos, int value_pos)
	{
		super(sysmon, aseVersion, sampleTimeInMs, data, fieldName_pos, groupName_pos, instanceId_pos, value_pos);
	}

	@Override
	public String getReportName()
	{
		return "Data Cache Management";
	}

	private static class CacheInfoEntry
	{
		public CacheInfoEntry(String name, int id, int size, String description)
		{
			_name        = name;
			_id          = id;
			_size        = size;
			_description = description;
		}
		public String _name;
		public int    _id;
		public int    _size;
		public String _description;
		public Set<Integer> _ioSizes = new HashSet<Integer>(); 

		public void addPoolSize(int ioSize)
		{
			_ioSizes.add(ioSize);
		}

		public boolean hasPoolSize(int ioSize)
		{
			return _ioSizes.contains(ioSize);
		}
	}
	@Override
	public void calc()
	{
		String fieldName   = "";
		String groupName   = "";
		int    instanceid  = -1;
		int    field_id    = -1;
		int    value       = 0;
		String description = "";

		int asePageSize = 0;
		List<CacheInfoEntry> cacheInfo = new ArrayList<CacheInfoEntry>();
		
		int fld_xxx = 0;
		int fld_yyy = 0;
		int TotalSearches = 0;
		int cacheHits = 0;
		int cacheMisses = 0;

		int extendedCacheSize = 0; // Get extended Cache Size from configuration... FIXME: skip this, since it's only for 32 bit Linux, which nobody uses anymore
		int extendedCacheHits = 0;
		int extendedCacheMisses = 0;
		int extendedCacheSearches = 0;
		
		int CacheTurnover_BuffersGrabbed = 0;
		int CacheTurnover_BuffersGrabbedDirty = 0;
		
		int CacheStrategySummary = 0;
		int CacheStrategyMRU = 0;
		int CacheStrategyLRU = 0;

		int LargeIO_req = 0;
		int LargeIO_Performed = 0;
		int LargeIO_DeniedDueTo_prefetch_decrease = 0;
		int LargeIO_DeniedDueTo_prefetch_kept_bp = 0;
		int LargeIO_DeniedDueTo_prefetch_cached_bp = 0;

		int LargeIO_Effectiveness_Cached_sum = 0;
		int LargeIO_Effectiveness_Cached_1pg = 0;
		int LargeIO_Effectiveness_Cached_2pg = 0;
		int LargeIO_Effectiveness_Cached_4pg = 0;
		int LargeIO_Effectiveness_Cached_8pg = 0;
		int LargeIO_Effectiveness_Used_sum = 0;
		int LargeIO_Effectiveness_Used_1pg = 0;
		int LargeIO_Effectiveness_Used_2pg = 0;
		int LargeIO_Effectiveness_Used_4pg = 0;
		int LargeIO_Effectiveness_Used_8pg = 0;

		int APF_Activity_Requested = 0;
		int APF_Activity_Issued = 0;
		int APF_Activity_DeniedDueTo_IoOverloads = 0;
		int APF_Activity_DeniedDueTo_LimitOverloads = 0;
		int APF_Activity_DeniedDueTo_RusedOverloads = 0;
		int APF_Activity_BufferFoundInCache_WithSpinlockHeld = 0;
		int APF_Activity_BufferFoundInCache_WithOutSpinlockHeld = 0;
		int fld_apf_crooked_page_chain = 0;
		int fld_apf_determine_lookahead_length_called = 0;
		int fld_apf_corrected = 0;
		int fld_apf_not_recommended = 0;

		int OtherAPFStats_Used = 0;
		int OtherAPFStats_WaitsForIo = 0;
		int OtherAPFStats_Discards = 0;

		int DirtyReadBehavior_PageRequest = 0;
		int DirtyReadBehavior_ReStarts = 0;

		// Get some basics, from ABSOLUTE values (since the configuration etc are kept in value, and we don't want the difference since it will be 0)
		for (List<Object> row : getAbsData())
		{
			if (_instanceid_pos > 0)
				instanceid = ((Number)row.get(_instanceid_pos)).intValue();
			fieldName = (String)row.get(_fieldName_pos);
			groupName = (String)row.get(_groupName_pos);
			field_id  = ((Number)row.get(_field_id_pos)).intValue();
			value     = ((Number)row.get(_value_pos)).intValue();
			description    = (String) row.get(_description_pos);

//System.out.println("XXXXX: fieldName='"+fieldName+"', groupName='"+groupName+"', field_id="+field_id+", value="+value+", description='"+description+"'.");
			// @@maxpagesize
			if (groupName.equals("ase-global-var") && fieldName.equals("@@maxpagesize"))
			{
//System.out.println("SETTING: @@maxpagesize="+value);
				asePageSize = value;
			}
			
			// Cache names
			if (groupName.equals("ase-cache-info"))
				cacheInfo.add(new CacheInfoEntry(fieldName, field_id, value, description));

			// Cache POOL info
			if (groupName.equals("ase-cache-pool-info"))
			{
				for (CacheInfoEntry cie : cacheInfo)
				{
					String cacheName = fieldName;
					int poolPos = cacheName.indexOf(":pool=");
					if (poolPos >= 0)
						cacheName = cacheName.substring(0, poolPos);

					if (cie._name.equals(cacheName))
					{
//System.out.println("CACHE: name='"+cacheName+"', ioSize="+value);
						cie.addPoolSize(value);
					}
				}
			}
		}

		if (asePageSize == 0)
		{
			addReportLn("Sorry, cant continue, can't find '@@maxpagesize' in configuration");
			return;
		}

		int io1page = 0;
		int io2page = 0;
		int io4page = 0;
		int io8page = 0;
		if (asePageSize == 1024*2) // 2K
		{
			io1page = 2;
			io2page = 4;
			io4page = 8;
			io8page = 16;
		}
		else if (asePageSize == 1024*4) // 4K
		{
			io1page = 4;
			io2page = 8;
			io4page = 16;
			io8page = 32;
		}
		else if (asePageSize == 1024*8) // 8K
		{
			io1page = 8;
			io2page = 16;
			io4page = 32;
			io8page = 64;
		}
		else if (asePageSize == 1024*16) // 16K
		{
			io1page = 16;
			io2page = 32;
			io4page = 64;
			io8page = 128;
		}
		else
		{
			addReportLn("Sorry, cant continue, '@@maxpagesize' is not 2048, 4096, 8192 or 16384. The value '"+asePageSize+"' is unknown.");
			return;
		}
		
		// get data
		for (List<Object> row : getData())
		{
			if (_instanceid_pos > 0)
				instanceid = ((Number)row.get(_instanceid_pos)).intValue();
			fieldName = (String)row.get(_fieldName_pos);
			groupName = (String)row.get(_groupName_pos);
//			field_id  = ((Number)row.get(_field_id_pos)).intValue();
			value     = ((Number)row.get(_value_pos)).intValue();

			//-----------------------------------------------------------------------
			// TotalSearches
			if (groupName.startsWith("buffer_") && fieldName.equals("bufsearch_calls"))
				TotalSearches += value;
			
			// cacheHits
			if (groupName.startsWith("buffer_") && fieldName.equals("bufsearch_finds"))
				cacheHits += value;

			
			//-----------------------------------------------------------------------
			// extCacheSearches
			if (groupName.equals("ecache") && fieldName.equals("ecache_srchcalls"))
				extendedCacheSearches += value;

			// extCacheHits
			if (groupName.equals("ecache") && fieldName.equals("ecache_read"))
				extendedCacheHits += value;

			
			//-----------------------------------------------------------------------
			// CacheTurnover_BuffersGrabbed
			if (groupName.startsWith("buffer_") && fieldName.matches("bufgrab_[0-9]+k") && !fieldName.matches("bufgrab_ref[0-9]+k") )
				CacheTurnover_BuffersGrabbed += value;

			// CacheTurnover_BuffersGrabbedDirty
			if (groupName.startsWith("buffer_") && fieldName.startsWith("bufgrab_dirty_"))
				CacheTurnover_BuffersGrabbedDirty += value;

			
			//-----------------------------------------------------------------------
			// CacheStrategySummary
			if (groupName.startsWith("buffer_") && ( fieldName.equals("bufunkeep_lru") || fieldName.equals("bufunkeep_mru")) )
				CacheStrategySummary += value;

			// CacheStrategyLRU
			if (groupName.startsWith("buffer_") && fieldName.equals("bufunkeep_lru"))
				CacheStrategyLRU += value;

			// CacheStrategyMRU
			if (groupName.startsWith("buffer_") && fieldName.equals("bufunkeep_mru"))
				CacheStrategyMRU += value;

			//-----------------------------------------------------------------------
			// LargeIO_req
			if (groupName.startsWith("buffer_") && fieldName.equals("prefetch_req"))
				LargeIO_req += value;

			// LargeIO_Performed
			if (groupName.startsWith("buffer_") && ( fieldName.equals("prefetch_as_requested") || fieldName.equals("prefetch_page_realign") || fieldName.equals("prefetch_increase")) )
				LargeIO_Performed += value;

			if (groupName.startsWith("buffer_") && fieldName.equals("prefetch_decrease"))
				LargeIO_DeniedDueTo_prefetch_decrease += value;
			if (groupName.startsWith("buffer_") && fieldName.equals("prefetch_kept_bp"))
				LargeIO_DeniedDueTo_prefetch_kept_bp += value;
			if (groupName.startsWith("buffer_") && fieldName.equals("prefetch_cached_bp"))
				LargeIO_DeniedDueTo_prefetch_cached_bp += value;

			//-----------------------------------------------------------------------
			// LargeIO_Effectiveness_Cached
//			if (groupName.startsWith("buffer_") && ( fieldName.equals("bufgrab_"+io1page+"k") || fieldName.equals("bufgrab_"+io2page+"k") || fieldName.equals("bufgrab_"+io4page+"k") || fieldName.equals("bufgrab_"+io8page+"k")) )
//				LargeIO_Effectiveness_Cached_sum += value;

			if (groupName.startsWith("buffer_") && fieldName.equals("bufgrab_"+io1page+"k") ) LargeIO_Effectiveness_Cached_1pg += value * 1; // turn # of masses into # of logical pages
			if (groupName.startsWith("buffer_") && fieldName.equals("bufgrab_"+io2page+"k") ) LargeIO_Effectiveness_Cached_2pg += value * 2; // turn # of masses into # of logical pages
			if (groupName.startsWith("buffer_") && fieldName.equals("bufgrab_"+io4page+"k") ) LargeIO_Effectiveness_Cached_4pg += value * 4; // turn # of masses into # of logical pages
			if (groupName.startsWith("buffer_") && fieldName.equals("bufgrab_"+io8page+"k") ) LargeIO_Effectiveness_Cached_8pg += value * 8; // turn # of masses into # of logical pages

			// LargeIO_Effectiveness_Used
//			if (groupName.startsWith("buffer_") && fieldName.matches("bufgrab_ref_[0-9]+K") && !fieldName.equals("bufgrab_"+io8page+"k") )
//				LargeIO_Effectiveness_Used += value;

			if (groupName.startsWith("buffer_") && fieldName.matches("bufgrab_ref_"+io1page+"K") ) LargeIO_Effectiveness_Used_1pg += value;
			if (groupName.startsWith("buffer_") && fieldName.matches("bufgrab_ref_"+io2page+"K") ) LargeIO_Effectiveness_Used_2pg += value;
			if (groupName.startsWith("buffer_") && fieldName.matches("bufgrab_ref_"+io4page+"K") ) LargeIO_Effectiveness_Used_4pg += value;
			if (groupName.startsWith("buffer_") && fieldName.matches("bufgrab_ref_"+io8page+"K") ) LargeIO_Effectiveness_Used_8pg += value;

			//-----------------------------------------------------------------------
			// APF_Activity_Requested
			if (groupName.equals("access") && (    fieldName.equals("apf_IOs_issued") 
			                                    || fieldName.equals("apf_could_not_start_IO_immediately") 
			                                    || fieldName.equals("apf_configured_limit_exceeded") 
			                                    || fieldName.equals("apf_unused_read_penalty") 
			                                    || fieldName.equals("apf_found_in_cache_with_spinlock") 
			                                    || fieldName.equals("apf_found_in_cache_wo_spinlock") ) )
				APF_Activity_Requested += value;

			// APF_Activity_Issued
			if (groupName.equals("access") && fieldName.equals("apf_IOs_issued"))                     APF_Activity_Issued += value;
				
			// APF_Activity_DeniedDueTo_IoOverloads
			if (groupName.equals("access") && fieldName.equals("apf_could_not_start_IO_immediately")) APF_Activity_DeniedDueTo_IoOverloads += value;
				
			// APF_Activity_DeniedDueTo_LimitOverloads
			if (groupName.equals("access") && fieldName.equals("apf_configured_limit_exceeded"))      APF_Activity_DeniedDueTo_LimitOverloads += value;
				
			// APF_Activity_DeniedDueTo_RusedOverloads
			if (groupName.equals("access") && fieldName.equals("apf_unused_read_penalty"))            APF_Activity_DeniedDueTo_RusedOverloads += value;
				
			// APF_Activity_Issued
			if (groupName.equals("access") && fieldName.equals("apf_found_in_cache_with_spinlock"))   APF_Activity_BufferFoundInCache_WithSpinlockHeld += value;
				
			// APF_Activity_Issued
			if (groupName.equals("access") && fieldName.equals("apf_found_in_cache_wo_spinlock"))     APF_Activity_BufferFoundInCache_WithOutSpinlockHeld += value;

			// SOme extra that isn't used by sp_sysmon, but might be interesting anyway: the most important is fld_apf_crooked_page_chain
			if (groupName.equals("access") && fieldName.equals("apf_crooked_page_chain"))                fld_apf_crooked_page_chain                += value;
			if (groupName.equals("access") && fieldName.equals("apf_determine_lookahead_length_called")) fld_apf_determine_lookahead_length_called += value;
			if (groupName.equals("access") && fieldName.equals("apf_corrected"))                         fld_apf_corrected                         += value;
			if (groupName.equals("access") && fieldName.equals("apf_not_recommended"))                   fld_apf_not_recommended                   += value;

			//-----------------------------------------------------------------------
			// OtherAPFStats_Used
			if (groupName.equals("access") && fieldName.equals("apf_IOs_used"))
				OtherAPFStats_Used += value;

			// OtherAPFStats_WaitsForIo
			if (groupName.equals("access") && fieldName.equals("apf_waited_for_IO_to_complete"))
				OtherAPFStats_WaitsForIo += value;

			// OtherAPFStats_Discards
			if (fieldName.matches("apf.*discard.*"))
				OtherAPFStats_Discards += value;

			//-----------------------------------------------------------------------
			// DirtyReadBehavior_PageRequest
			if (groupName.startsWith("buffer_") && fieldName.equals("level0_bufpredirty"))
				DirtyReadBehavior_PageRequest += value;

			// DirtyReadBehavior_ReStarts
			if (groupName.equals("access") && fieldName.equals("dirty_read_restarts"))
				DirtyReadBehavior_ReStarts += value;
		}
		cacheMisses = TotalSearches - cacheHits;
		extendedCacheMisses = extendedCacheSearches - extendedCacheHits;
		
		// 1pg should not be part of the SUM
		LargeIO_Effectiveness_Cached_sum = LargeIO_Effectiveness_Cached_2pg + LargeIO_Effectiveness_Cached_4pg + LargeIO_Effectiveness_Cached_8pg;
		LargeIO_Effectiveness_Used_sum   = LargeIO_Effectiveness_Used_2pg   + LargeIO_Effectiveness_Used_4pg   + LargeIO_Effectiveness_Used_8pg;
		
		
//		int TotalCacheHits = 0;
//		int TotalCacheMisses = 0;
//		int TotalCacheSearches = 0;
//		if (TotalSearches > 0)
//		{
//			TotalCacheHits     = cacheHits / TotalSearches;
//			TotalCacheMisses   = cacheMisses / TotalSearches;
//			TotalCacheSearches = TotalSearches;
//		}

//		// get some specific values
//		for (List<Object> row : getData())
//		{
//			if (_instanceid_pos > 0)
//				instanceid = ((Number)row.get(_instanceid_pos)).intValue();
//			fieldName = (String)row.get(_fieldName_pos);
//			groupName = (String)row.get(_groupName_pos);
////			field_id  = ((Number)row.get(_field_id_pos)).intValue();
//			value     = ((Number)row.get(_value_pos)).intValue();
//
//			//----------------------------
//			// Memory
//			//----------------------------
//
//			// TotalSearches
//			if (groupName.startsWith("buffer_") && fieldName.equals("bufsearch_calls"))
//				TotalSearches += value;
//			
//			// TotalSearches
//			if (groupName.startsWith("buffer_") && fieldName.equals("bufsearch_finds"))
//				bufsearch_finds += value;
//		}

//		addReportHead("Whatever Header");
//		addReportLnCnt("  Counter X",  fld_xxx);
//		addReportLnCnt("  Counter Y",  fld_yyy);
		
//		addReportLnNotYetImplemented();

		int divideBy = TotalSearches;
		addReportLn   ("  Cache Statistics Summary (All Caches)");
		addReportLn   ("  -------------------------------------");
		addReportLn   ("                                  per sec      per xact       count  % of total");
		addReportLn   ("                             ------------  ------------  ----------  ----------");
		addReportLn   ("  Cache Search Summary");
		addReportLnPct("    Total Cache Hits",      cacheHits,    divideBy);
		addReportLnPct("    Total Cache Misses",    cacheMisses,  divideBy);
		addReportLnSum(); // -------------------------  ------------  ------------  ----------
		addReportLnCnt("  Total Cache Searches",   divideBy);
		if (extendedCacheSize > 0)
		{
			// we can probably skip this, since it's only for 32 bit Linux, which nobody uses anymore
			// http://infocenter.sybase.com/help/index.jsp?topic=/com.sybase.help.ase_12.5.2.newfeatures1252/html/newfeatures1252/newfeatures1252154.htm
			divideBy = extendedCacheSearches;
			addReportLn   ();
			addReportLn   ("  Secondary Cache Search Summary");
			addReportLnPct("    Total Cache Hits",      extendedCacheHits,    divideBy);
			addReportLnPct("    Total Cache Misses",    extendedCacheMisses,  divideBy);
			addReportLnSum(); // -------------------------  ------------  ------------  ----------
			addReportLnCnt("  Total Cache Searches",   divideBy);
		}
		divideBy = CacheTurnover_BuffersGrabbed;
		addReportLn   ();
		addReportLn   ("  Cache Turnover");
		addReportLnCnt("    Buffers Grabbed",   divideBy);
		addReportLnPct("    Buffers Grabbed Dirty", CacheTurnover_BuffersGrabbedDirty, divideBy);
		addReportLn   ();
		addReportLn   ("  Cache Strategy Summary");
		addReportLnPct("    Cached (LRU) Buffers", CacheStrategyLRU,    CacheStrategySummary);
		addReportLnPct("    Discarded (MRU) Buffers", CacheStrategyMRU, CacheStrategySummary);
		addReportLn   ();
		divideBy = LargeIO_req;
		addReportLn   ("  Large I/O Usage"); //NOT_WORKING_CORRECTLY???
		addReportLnPct("    Large I/Os Performed", LargeIO_Performed, divideBy);
		addReportLn   ("### Large I/Os Performed: LargeIO_Performed(prefetch_as_requested|prefetch_page_realign|prefetch_increase)='"+LargeIO_Performed+"', LargeIO_req(prefetch_req)='"+LargeIO_req+"'."); //NOT_WORKING_CORRECTLY???

		addReportLn   ();
		addReportLn   ("    Large I/Os Denied due to");
		addReportLnPct("      Pool < Prefetch Size", LargeIO_DeniedDueTo_prefetch_decrease - (LargeIO_DeniedDueTo_prefetch_kept_bp + LargeIO_DeniedDueTo_prefetch_cached_bp), divideBy);
		addReportLn   ("      Pages Requested");
		addReportLn   ("      Reside in Another");
		addReportLnPct("      Buffer Pool", LargeIO_DeniedDueTo_prefetch_kept_bp + LargeIO_DeniedDueTo_prefetch_cached_bp, divideBy);
		addReportLnSum(); // -------------------------  ------------  ------------  ----------
		addReportLnCnt("  Total Large I/O Requests",   divideBy);
		addReportLn   ();
		addReportLn   ("  Large I/O Effectiveness");
		addReportLnCnt("    Pages by Lrg I/O Cached",   LargeIO_Effectiveness_Cached_sum);
		addReportLnPct("    Pages by Lrg I/O Used",     LargeIO_Effectiveness_Used_sum, LargeIO_Effectiveness_Cached_sum);
		addReportLn   ();
		divideBy = APF_Activity_Requested;
		addReportLn   ("  Asynchronous Prefetch Activity");
		addReportLnPct("    APFs Issued", APF_Activity_Issued, divideBy);
		addReportLn   ("    APFs Denied Due To");
		addReportLnPct("      APF I/O Overloads", APF_Activity_DeniedDueTo_IoOverloads,       divideBy);
		addReportLnPct("      APF Limit Overloads", APF_Activity_DeniedDueTo_LimitOverloads,  divideBy);
		addReportLnPct("      APF Reused Overloads", APF_Activity_DeniedDueTo_RusedOverloads, divideBy);
		addReportLn   ("    APF Buffers Found in Cache");
		addReportLnPct("      With Spinlock Held", APF_Activity_BufferFoundInCache_WithSpinlockHeld,   divideBy);
		addReportLnPct("      W/o Spinlock Held", APF_Activity_BufferFoundInCache_WithOutSpinlockHeld, divideBy);
		addReportLnSum(); // -------------------------  ------------  ------------  ----------
		addReportLnCnt("  Total APFs Requested", divideBy);
		addReportLn   ();
		addReportLn   ("  Other Asynchronous Prefetch Statistics");
		addReportLnCnt("    APFs Used",           OtherAPFStats_Used);
		addReportLnCnt("    APF Waits for I/O",   OtherAPFStats_WaitsForIo);
		addReportLnCnt("    APF Discards",        OtherAPFStats_Discards);
		if (fld_apf_crooked_page_chain                > 0) addReportLnCnt("    APF Extent Jumps",             fld_apf_crooked_page_chain,                ONLY_IN_ASETUNE_SYSMON_REPORT + ", counter='apf_crooked_page_chain'");
		if (fld_apf_determine_lookahead_length_called > 0) addReportLnCnt("    APF Determine LookaheadCalls", fld_apf_determine_lookahead_length_called, ONLY_IN_ASETUNE_SYSMON_REPORT + ", counter='apf_determine_lookahead_length_called'");
		if (fld_apf_corrected                         > 0) addReportLnCnt("    APF Corrected",                fld_apf_corrected,                         ONLY_IN_ASETUNE_SYSMON_REPORT + ", counter='apf_corrected'");
		if (fld_apf_not_recommended                   > 0) addReportLnCnt("    APF Not Recommended",          fld_apf_not_recommended,                   ONLY_IN_ASETUNE_SYSMON_REPORT + ", counter='apf_not_recommended'");
		addReportLn   ();
		addReportLn   ("  Dirty Read Behavior");
		addReportLnCnt("    Page Requests",   DirtyReadBehavior_PageRequest);
		addReportLnPct("    Re-Starts",       DirtyReadBehavior_ReStarts, DirtyReadBehavior_PageRequest);
		
	
//		  Cache Statistics Summary (All Caches)
//		  -------------------------------------
//		                                  per sec      per xact       count  % of total
//		                             ------------  ------------  ----------  ----------
//		 
//		    Cache Search Summary
//		      Total Cache Hits              178.8        1072.6       21452      99.7 %
//		      Total Cache Misses              0.6           3.5          70       0.3 %
//		  -------------------------  ------------  ------------  ----------
//		    Total Cache Searches            179.4        1076.1       21522             
//		 
//		    Cache Turnover
//		      Buffers Grabbed                 1.4           8.1         163       n/a   
//		      Buffers Grabbed Dirty           0.0           0.0           0       0.0 %
//		 
//		    Cache Strategy Summary
//		      Cached (LRU) Buffers          181.5        1088.8       21777     100.0 %
//		      Discarded (MRU) Buffers         0.1           0.4           8       0.0 %
//		 
//		    Large I/O Usage
//		                                      0.0           0.0           0       n/a   
//		 
//		    Large I/O Effectiveness
//		      Pages by Lrg I/O Cached         0.0           0.0           0       n/a   
//		 
//		    Asynchronous Prefetch Activity
//		      APFs Issued                     0.6           3.7          74      42.5 %
//		      APFs Denied Due To                                                        
//		        APF I/O Overloads             0.0           0.0           0       0.0 %
//		        APF Limit Overloads           0.0           0.0           0       0.0 %
//		        APF Reused Overloads          0.0           0.0           0       0.0 %
//		      APF Buffers Found in Cache                                                
//		        With Spinlock Held            0.0           0.1           1       0.6 %
//		        W/o Spinlock Held             0.8           4.9          99      56.9 %
//		  -------------------------  ------------  ------------  ----------
//		    Total APFs Requested              1.5           8.7         174             
//		 
//		    Other Asynchronous Prefetch Statistics
//		      APFs Used                       0.6           3.7          73       n/a   
//		      APF Waits for I/O               0.4           2.4          48       n/a   
//		      APF Discards                    0.0           0.0           0       n/a   
//		 
//		    Dirty Read Behavior
//		      Page Requests                   0.0           0.0           0       n/a   

		
		// for each of the caches
		for (CacheInfoEntry cacheInfoEntry : cacheInfo)
		{
			calcForCache(cacheInfoEntry, TotalSearches, io1page, io2page, io4page, io8page);
		}
	}
	
	
	private void calcForCache(CacheInfoEntry entry, int totalSearches, int io1page, int io2page, int io4page, int io8page)
	{
		String fieldName   = "";
		String groupName   = "";
		int    instanceid  = -1;
		int    field_id    = -1;
		int    value       = 0;
		String description = "";

		String cacheGroupName = "buffer_"+entry._id;
		boolean isLocklessDataCache = entry._description.indexOf("Lockless Data Cache") >= 0;


		int cacheSeaches = 0;
		int cacheHits = 0;
		int cacheHitsInWash = 0;
		int lldc_ELC = 0; // lldc = Lockless Data Cache
		int lldc_WithOutSpinlockheld = 0; // lldc = Lockless Data Cache

		int CacheTurnover_BuffersGrabbed = 0;
		int bufgrab_1pg        = 0;
		int bufgrab_locked_1pg = 0;
		int bufgrab_dirty_1pg  = 0;

		int bufgrab_2pg        = 0;
		int bufgrab_locked_2pg = 0;
		int bufgrab_dirty_2pg  = 0;

		int bufgrab_4pg        = 0;
		int bufgrab_locked_4pg = 0;
		int bufgrab_dirty_4pg  = 0;
		
		int bufgrab_8pg        = 0;
		int bufgrab_locked_8pg = 0;
		int bufgrab_dirty_8pg  = 0;

		int cl_totalrequested = 0;
		int cl_totalretained = 0;
		int cl_totaldeadlocks = 0;
		int cl_totalwaits = 0;
//		int cl_totaltransfers_ac = 0; // ac = All Caches
		int cl_totaltransfers = 0;
		int cl_totaldiskreads = 0;
		int cl_totaldiskwrites = 0;
		int cl_totallockmgrcalls = 0;
		int cl_totallockmgrsent  = 0;

		int bufwash_throughput = 0;
		int bufwash_pass_clean = 0;
		int bufwash_pass_writing = 0;
		int bufwash_write_dirty = 0;

		int bufunkeep_sum = 0;
		int bufunkeep_lru = 0;
		int bufunkeep_mru = 0;

		int LargeIO_req = 0;
		int LargeIO_Performed = 0;
		int LargeIO_DeniedDueTo_prefetch_decrease = 0;
		int LargeIO_DeniedDueTo_prefetch_kept_bp = 0;
		int LargeIO_DeniedDueTo_prefetch_cached_bp = 0;

		int LargeIO_Detail_Cached_1pg = 0;
		int LargeIO_Detail_Cached_2pg = 0;
		int LargeIO_Detail_Cached_4pg = 0;
		int LargeIO_Detail_Cached_8pg = 0;
		int LargeIO_Detail_Used_1pg = 0;
		int LargeIO_Detail_Used_2pg = 0;
		int LargeIO_Detail_Used_4pg = 0;
		int LargeIO_Detail_Used_8pg = 0;

		int DirtyReadBehavior_PageRequest = 0;

		// get data
		for (List<Object> row : getData())
		{
			if (_instanceid_pos > 0)
				instanceid = ((Number)row.get(_instanceid_pos)).intValue();
			fieldName = (String)row.get(_fieldName_pos);
			groupName = (String)row.get(_groupName_pos);
//			field_id  = ((Number)row.get(_field_id_pos)).intValue();
			value     = ((Number)row.get(_value_pos)).intValue();

			//-----------------------------------------------------------------------
			// cacheSeaches
			if (groupName.equals(cacheGroupName) && fieldName.equals("bufsearch_calls"))
				cacheSeaches += value;
			
			// cacheHits
			if (groupName.equals(cacheGroupName) && fieldName.equals("bufsearch_finds"))
				cacheHits += value;

			// cacheHitsInWash
			if (groupName.equals(cacheGroupName) && fieldName.equals("bufsearch_finds_in_wash"))
				cacheHitsInWash += value;

			// lldc_ELC
			if (groupName.equals(cacheGroupName) && fieldName.equals("bufsearch_finds_in_elc"))
				lldc_ELC += value;

			// lldc_WithOutSpinlockheld
			if (groupName.equals(cacheGroupName) && fieldName.equals("buf_cachesearch_lockless_finds"))
				lldc_WithOutSpinlockheld += value;

			//-----------------------------------------------------------------------
			if (groupName.equals(cacheGroupName) && fieldName.matches("bufgrab_[0-9]+k") && !fieldName.matches("bufgrab_ref[0-9]+k") )
				CacheTurnover_BuffersGrabbed += value;
			// bufgrab_1pg
			if (groupName.equals(cacheGroupName) && fieldName.equals("bufgrab_"        + io1page + "k")) bufgrab_1pg        += value;
			if (groupName.equals(cacheGroupName) && fieldName.equals("bufgrab_locked_" + io1page + "k")) bufgrab_locked_1pg += value;
			if (groupName.equals(cacheGroupName) && fieldName.equals("bufgrab_dirty_"  + io1page + "k")) bufgrab_dirty_1pg  += value;
			// bufgrab_2pg
			if (groupName.equals(cacheGroupName) && fieldName.equals("bufgrab_"        + io2page + "k")) bufgrab_2pg        += value;
			if (groupName.equals(cacheGroupName) && fieldName.equals("bufgrab_locked_" + io2page + "k")) bufgrab_locked_2pg += value;
			if (groupName.equals(cacheGroupName) && fieldName.equals("bufgrab_dirty_"  + io2page + "k")) bufgrab_dirty_2pg  += value;
			// bufgrab_4pg
			if (groupName.equals(cacheGroupName) && fieldName.equals("bufgrab_"        + io4page + "k")) bufgrab_4pg        += value;
			if (groupName.equals(cacheGroupName) && fieldName.equals("bufgrab_locked_" + io4page + "k")) bufgrab_locked_4pg += value;
			if (groupName.equals(cacheGroupName) && fieldName.equals("bufgrab_dirty_"  + io4page + "k")) bufgrab_dirty_4pg  += value;
			// bufgrab_8pg
			if (groupName.equals(cacheGroupName) && fieldName.equals("bufgrab_"        + io8page + "k")) bufgrab_8pg        += value;
			if (groupName.equals(cacheGroupName) && fieldName.equals("bufgrab_locked_" + io8page + "k")) bufgrab_locked_8pg += value;
			if (groupName.equals(cacheGroupName) && fieldName.equals("bufgrab_dirty_"  + io8page + "k")) bufgrab_dirty_8pg  += value;

			//-----------------------------------------------------------------------
			if (groupName.equals(cacheGroupName) && fieldName.equals("physical_lock_acquisition")) cl_totalrequested    += value;
			if (groupName.equals(cacheGroupName) && fieldName.equals("physical_lock_retented"))    cl_totalretained     += value;
			if (groupName.equals(cacheGroupName) && fieldName.equals("physical_lock_deadlock"))    cl_totaldeadlocks    += value;
			if (groupName.equals(cacheGroupName) && fieldName.equals("waited_on_tx"))              cl_totalwaits        += value;
//			if (groupName.startsWith("buffer_")  && fieldName.equals("physical_lock_txrecv"))      cl_totaltransfers_ac += value;
			if (groupName.equals(cacheGroupName) && fieldName.equals("physical_lock_txrecv"))      cl_totaltransfers    += value;
			if (groupName.equals(cacheGroupName) && fieldName.equals("diskread_lockmgr"))          cl_totaldiskreads    += value;
			if (groupName.equals(cacheGroupName) && fieldName.equals("diskwrite"))                 cl_totaldiskwrites   += value;
			if (groupName.equals(cacheGroupName) && fieldName.equals("physical_lock_lockmgr"))     cl_totallockmgrcalls += value;
			if (groupName.equals(cacheGroupName) && fieldName.equals("physical_lock_txsend"))      cl_totallockmgrsent  += value;

			//-----------------------------------------------------------------------
			if (groupName.equals(cacheGroupName) && fieldName.equals("bufwash_throughput"))   bufwash_throughput    += value;
			if (groupName.equals(cacheGroupName) && fieldName.equals("bufwash_pass_clean"))   bufwash_pass_clean    += value;
			if (groupName.equals(cacheGroupName) && fieldName.equals("bufwash_pass_writing")) bufwash_pass_writing  += value;
			if (groupName.equals(cacheGroupName) && fieldName.equals("bufwash_write_dirty"))  bufwash_write_dirty   += value;

			//-----------------------------------------------------------------------
			if (groupName.equals(cacheGroupName) && fieldName.equals("bufunkeep_lru"))   bufunkeep_lru    += value;
			if (groupName.equals(cacheGroupName) && fieldName.equals("bufunkeep_mru"))   bufunkeep_mru    += value;

			//-----------------------------------------------------------------------
			// LargeIO_req
			if (groupName.equals(cacheGroupName) && fieldName.equals("prefetch_req")) LargeIO_req += value;

			// LargeIO_Performed
			if (groupName.equals(cacheGroupName) && ( fieldName.equals("prefetch_as_requested") || fieldName.equals("prefetch_page_realign") || fieldName.equals("prefetch_increase")) ) LargeIO_Performed += value;

			if (groupName.equals(cacheGroupName) && fieldName.equals("prefetch_decrease") ) LargeIO_DeniedDueTo_prefetch_decrease  += value;
			if (groupName.equals(cacheGroupName) && fieldName.equals("prefetch_kept_bp")  ) LargeIO_DeniedDueTo_prefetch_kept_bp   += value;
			if (groupName.equals(cacheGroupName) && fieldName.equals("prefetch_cached_bp")) LargeIO_DeniedDueTo_prefetch_cached_bp += value;

			//-----------------------------------------------------------------------
			// LargeIO_Detail_Cached
			if (groupName.equals(cacheGroupName) && fieldName.equals("bufgrab_"+io1page+"k") ) LargeIO_Detail_Cached_1pg += value;
			if (groupName.equals(cacheGroupName) && fieldName.equals("bufgrab_"+io2page+"k") ) LargeIO_Detail_Cached_2pg += value;
			if (groupName.equals(cacheGroupName) && fieldName.equals("bufgrab_"+io4page+"k") ) LargeIO_Detail_Cached_4pg += value;
			if (groupName.equals(cacheGroupName) && fieldName.equals("bufgrab_"+io8page+"k") ) LargeIO_Detail_Cached_8pg += value;

			// LargeIO_Detail_Used
			if (groupName.equals(cacheGroupName) && fieldName.equals("bufgrab_ref_"+io1page+"K") ) LargeIO_Detail_Used_1pg += value;
			if (groupName.equals(cacheGroupName) && fieldName.equals("bufgrab_ref_"+io2page+"K") ) LargeIO_Detail_Used_2pg += value;
			if (groupName.equals(cacheGroupName) && fieldName.equals("bufgrab_ref_"+io4page+"K") ) LargeIO_Detail_Used_4pg += value;
			if (groupName.equals(cacheGroupName) && fieldName.equals("bufgrab_ref_"+io8page+"K") ) LargeIO_Detail_Used_8pg += value;

			//-----------------------------------------------------------------------
			// DirtyReadBehavior_PageRequest
			if (groupName.startsWith("buffer_") && fieldName.equals("level0_bufpredirty"))
				DirtyReadBehavior_PageRequest += value;

		}
		bufunkeep_sum = bufunkeep_lru + bufunkeep_mru;

		// turn # of masses into # of logical pages
		LargeIO_Detail_Cached_1pg = LargeIO_Detail_Cached_1pg * 1;
		LargeIO_Detail_Cached_2pg = LargeIO_Detail_Cached_2pg * 2;
		LargeIO_Detail_Cached_4pg = LargeIO_Detail_Cached_4pg * 4;
		LargeIO_Detail_Cached_8pg = LargeIO_Detail_Cached_8pg * 8;

		CountersModel cmSpinlockSum = CounterController.getInstance().getCmByName(CmSpinlockSum.CM_NAME);
		String spinlockCont = "unknown";
		LinkedHashMap<String, Double> spinlockCachletCont = new LinkedHashMap<String, Double>();
		if (cmSpinlockSum != null)
		{
			spinlockCont = cmSpinlockSum.getDiffString(entry._name, "contention");
			
			int[] rows = cmSpinlockSum == null ? null : cmSpinlockSum.getDiffRowIdsWhere("type", "CACHELET");
			for (int i=0; i<rows.length; i++)
			{
				String name  = cmSpinlockSum.getDiffString(rows[i], "spinName");
				String cont  = cmSpinlockSum.getDiffString(rows[i], "contention");
				double dCont = cmSpinlockSum.getDiffValueAsDouble(rows[i], "contention");
				if (name.startsWith(entry._name + " # "))
				{
//					System.out.println("#####: "+name+" = "+cont);
					spinlockCachletCont.put(name, dCont);
				}
			}
		}

		addReportLn   ();
		addReportLn   ();
		addReportLn   ("  ######################################################");
		addReportLn   ("  Cache: "+entry._name);
		addReportLn   ("  ######################################################");
		addReportLn   ("                                  per sec      per xact       count  % of total");
		addReportLn   ("                             ------------  ------------  ----------  ----------");
		addReportLn   ("  Spinlock Contention: <<<< see 'tab' Spinlock Sum for Spinlock contention >>>>");
		addReportLn   ("  Spinlock Contention Summary:                                       " + StringUtil.right(spinlockCont + " %", 10));
		for (String key : spinlockCachletCont.keySet())
		{
			Double cacheletContention = spinlockCachletCont.get(key);
			addReportLn   ("    Cachlet Spinlock Contention: " + StringUtil.left(key, 35) + " " + StringUtil.right(cacheletContention + " %", 10));
		}
		addReportLn   ();
		addReportLnPc1("  Utilization", cacheSeaches, totalSearches);
		addReportLn   ();
		addReportLn   ("  Cache Searches");
		addReportLnPct("    Total Cache Hits",      cacheHits,       cacheSeaches);
		addReportLnPct("      Found in Wash",       cacheHitsInWash, cacheSeaches);
		if (isLocklessDataCache) // if @c_type = "Lockless Data Cache"
		{
			addReportLnPct("                   ELC",    lldc_ELC, cacheHits);
			addReportLnPct("      w/o spinlockheld",    lldc_WithOutSpinlockheld, cacheHits);
			addReportLnPct("      with spinlockheld",   (cacheHits - lldc_WithOutSpinlockheld - lldc_ELC - cacheHitsInWash), cacheHits);
		}
		addReportLnPct("    Cache Misses",          (cacheSeaches - cacheHits),  cacheSeaches);
		addReportLnSum(); // -------------------------  ------------  ------------  ----------
		addReportLnCnt("  Total Cache Searches",   cacheSeaches);
		addReportLn   ();
		addReportLn   ("  Pool Turnover");
//		if (bufgrab_1pg > 0)
		if (entry.hasPoolSize(io1page))
		{
			addReportLn   ("    "+io1page+" KB Pool");
			addReportLnPct("      LRU Buffer Grab",           bufgrab_1pg,        CacheTurnover_BuffersGrabbed);
			addReportLnPct("        Grabbed Locked Buffer",   bufgrab_locked_1pg, bufgrab_1pg);
			addReportLnPct("        Grabbed Dirty",           bufgrab_dirty_1pg,  bufgrab_1pg);
		}
//		if (bufgrab_2pg > 0)
		if (entry.hasPoolSize(io2page))
		{
			addReportLn   ("    "+io2page+" KB Pool");
			addReportLnPct("      LRU Buffer Grab",           bufgrab_2pg,        CacheTurnover_BuffersGrabbed);
			addReportLnPct("        Grabbed Locked Buffer",   bufgrab_locked_2pg, bufgrab_2pg);
			addReportLnPct("        Grabbed Dirty",           bufgrab_dirty_2pg,  bufgrab_2pg);
		}
//		if (bufgrab_4pg > 0)
		if (entry.hasPoolSize(io4page))
		{
			addReportLn   ("    "+io4page+" KB Pool");
			addReportLnPct("      LRU Buffer Grab",           bufgrab_4pg,        CacheTurnover_BuffersGrabbed);
			addReportLnPct("        Grabbed Locked Buffer",   bufgrab_locked_4pg, bufgrab_4pg);
			addReportLnPct("        Grabbed Dirty",           bufgrab_dirty_4pg,  bufgrab_4pg);
		}
//		if (bufgrab_8pg > 0)
		if (entry.hasPoolSize(io8page))
		{
			addReportLn   ("    "+io8page+" KB Pool");
			addReportLnPct("      LRU Buffer Grab",           bufgrab_8pg,        CacheTurnover_BuffersGrabbed);
			addReportLnPct("        Grabbed Locked Buffer",   bufgrab_locked_8pg, bufgrab_8pg);
			addReportLnPct("        Grabbed Dirty",           bufgrab_dirty_8pg,  bufgrab_8pg);
		}
		addReportLnSum(); // -------------------------  ------------  ------------  ----------
		addReportLnCnt("  Total Cache Turnover",   CacheTurnover_BuffersGrabbed);
		addReportLn   ();
		addReportLn   ("  Cluster Cache Behavior");
		if (cl_totalrequested == 0)
		{
			addReportLn   ("    No physical locks are acquired on buffers in this cache");
		}
		else
		{
			addReportLnPct("    Total Lock Requests",           cl_totalrequested,  cl_totalrequested);
			addReportLnPct("    Retained Locks",                cl_totalretained,   cl_totalrequested);
			addReportLnPct("    Non-retained Locks",            (cl_totalrequested - cl_totalretained),  cl_totalrequested);
			addReportLnPct("    Data Read from Disk",           cl_totaldiskreads,  cl_totalrequested);
			addReportLnPct("    Transfers Received",            cl_totaltransfers,  cl_totalrequested);
//			if (cl_totaltransfers_ac > 0)
//				addReportLnPct("    Transfers Received",        cl_totaltransfers,  cl_totalrequested);
//			else
//				addReportLnPct("    Transfers Received",        0,  0); // 0 %
			addReportLnPct("    Waited for Data Transfer",      cl_totalwaits,      cl_totalrequested);
			addReportLnPct("    Deadlocks",                     cl_totaldeadlocks,  cl_totalrequested);
			addReportLnCnt("    Data Write to Disk",            cl_totaldiskwrites);
			addReportLnCnt("    Transfers Sent",                cl_totallockmgrsent);
			if (cl_totallockmgrcalls > 0)
				addReportLnPc1("    Data Location Efficiency",  1,  1); // 100%
			else
				addReportLnPc1("    Data Location Efficiency",  cl_totaltransfers,  cl_totallockmgrcalls);
		}
		addReportLn   ();
		addReportLn   ("  Buffer Wash Behavior");
		if (bufwash_throughput > 0)
		{
			addReportLnPct("    Buffers Passed Clean",   bufwash_pass_clean,   bufwash_throughput);
			addReportLnPct("    Buffers Already in I/O", bufwash_pass_writing, bufwash_throughput);
			addReportLnPct("    Buffers Washed Dirty",   bufwash_write_dirty,  bufwash_throughput);
		}
		else
		{
			addReportLn   ("    Statistics Not Available - No Buffers Entered Wash Section Yet");
		}
		addReportLn   ();
		addReportLn   ("  Cache Strategy");
		addReportLnPct("    Cached (LRU) Buffers",    bufunkeep_lru, bufunkeep_sum);
		addReportLnPct("    Discarded (MRU) Buffers", bufunkeep_mru, bufunkeep_sum);
		addReportLn   ();
		int divideBy = LargeIO_req;
		addReportLn   ("  Large I/O Usage");
		addReportLnPct("    Large I/Os Performed", LargeIO_Performed, divideBy);
		addReportLn   ();
		addReportLn   ("    Large I/Os Denied due to");
		addReportLnPct("      Pool < Prefetch Size", LargeIO_DeniedDueTo_prefetch_decrease - (LargeIO_DeniedDueTo_prefetch_kept_bp + LargeIO_DeniedDueTo_prefetch_cached_bp), divideBy);
		addReportLn   ("      Pages Requested");
		addReportLn   ("      Reside in Another");
		addReportLnPct("      Buffer Pool", LargeIO_DeniedDueTo_prefetch_kept_bp + LargeIO_DeniedDueTo_prefetch_cached_bp, divideBy);
		addReportLnCnt("  Total Large I/O Requests",   divideBy);
		addReportLn   ();
		addReportLn   ("  Large I/O Detail"); //NOT_WORKING_CORRECTLY
		if (entry.hasPoolSize(io1page))
		{
			addReportLn   ("    "+io1page+" KB Pool");
			addReportLnCnt("      Pages Cached",   LargeIO_Detail_Cached_1pg);
			addReportLnPct("      Pages Used",     LargeIO_Detail_Used_1pg, LargeIO_Detail_Cached_1pg);
addReportLn   ("### "+io1page+" KB Pool: LargeIO_Detail_Cached_1pg='"+LargeIO_Detail_Cached_1pg+"', LargeIO_Detail_Used_1pg='"+LargeIO_Detail_Used_1pg+"'.");
		}
		if (entry.hasPoolSize(io2page))
		{
			addReportLn   ("    "+io2page+" KB Pool");
			addReportLnCnt("      Pages Cached",   LargeIO_Detail_Cached_2pg);
			addReportLnPct("      Pages Used",     LargeIO_Detail_Used_2pg, LargeIO_Detail_Cached_2pg);
addReportLn   ("### "+io2page+" KB Pool: LargeIO_Detail_Cached_2pg='"+LargeIO_Detail_Cached_2pg+"', LargeIO_Detail_Used_2pg='"+LargeIO_Detail_Used_2pg+"'.");
		}
		if (entry.hasPoolSize(io4page))
		{
			addReportLn   ("    "+io4page+" KB Pool");
			addReportLnCnt("      Pages Cached",   LargeIO_Detail_Cached_4pg);
			addReportLnPct("      Pages Used",     LargeIO_Detail_Used_4pg, LargeIO_Detail_Cached_4pg);
addReportLn   ("### "+io4page+" KB Pool: LargeIO_Detail_Cached_4pg='"+LargeIO_Detail_Cached_4pg+"', LargeIO_Detail_Used_4pg='"+LargeIO_Detail_Used_4pg+"'.");
		}
		if (entry.hasPoolSize(io8page))
		{
			addReportLn   ("    "+io8page+" KB Pool");
			addReportLnCnt("      Pages Cached",   LargeIO_Detail_Cached_8pg);
			addReportLnPct("      Pages Used",     LargeIO_Detail_Used_8pg, LargeIO_Detail_Cached_8pg);
addReportLn   ("### "+io8page+" KB Pool: LargeIO_Detail_Cached_8pg='"+LargeIO_Detail_Cached_8pg+"', LargeIO_Detail_Used_8pg='"+LargeIO_Detail_Used_8pg+"'.");
		}
		addReportLn   ();
		addReportLn   ("  Dirty Read Behavior");
		addReportLnCnt("    Page Requests",   DirtyReadBehavior_PageRequest);
		addReportLn   ("    Re-Starts <<< This is only availible in the summary section >>>");
	}
}


//-----------------------------------------------------------------------------------------------------------------------------------------------------------------------
// sp_sysmon_dcache_sum, from: Adaptive Server Enterprise/16.0 SP02 PL02/EBF 25320 SMP/P/x86_64/Enterprise Linux/ase160sp02plx/2492/64-bit/FBO/Sat Nov 21 04:05:39 2015 
//-----------------------------------------------------------------------------------------------------------------------------------------------------------------------

//use sybsystemprocs
//go
//IF EXISTS (SELECT 1 FROM sysobjects
//           WHERE name = 'sp_sysmon_dcache_sum'
//             AND id = object_id('sp_sysmon_dcache_sum')
//             AND type = 'P')
//	DROP PROCEDURE sp_sysmon_dcache_sum
//go
//
//create or replace procedure sp_sysmon_dcache_sum
//        @NumEngines smallint,    /* number of engines online */
//        @NumElapsedMs int,      /* for "per Elapsed second" calculations */
//        @NumXacts int,          /* for per transactions calculations */
//        @Reco   char(1),        /* Flag for recommendations             */
//	@instid smallint = NULL	/* optional SDC instance id */
//
//as
//
///* --------- declare local variables --------- */
//declare @TotalSearches bigint	/* Total Cache Searches on All Caches */
//declare @j smallint		/* loop index to iterate through multi-counter 
//				** counters (pool...) */
//declare @tmp_cntr varchar(35)	/* temp var for build field_name's 
//				** ie. bufgrab_Nk */
//declare @tmp_int bigint		/* temp var for integer storage */
//declare @tmp_int2 int		/* temp var for integer storage */
//declare @tmp_int3 int           /* temp var for integer storage used to read 
//                                ** value of counter 'prefetch_kept_bp' */
//declare @tmp_int4 int           /* temp var for integer storage used to read
//                                ** value of counter 'prefetch_cached_bp' */
//declare @tmp_int_sum int        /* temp var for integer storage
//                                ** @tmp_int_sum = @tmp_int3 + @tmp_int4 */
//declare @tmp_total bigint	/* temp var for summing 'total #s' data */
//declare @tmp_float float        /* temp var for float storage */
//declare @tmp_float2 float       /* temp var for float storage */
//declare @sum2line char(67)	/* string to delimit total lines with 
//				** percent calc on printout */
//declare @numKBperpg int		/* number of kilobytes per logical page */
//declare @blankline char(1)	/* to print blank line */
//declare @psign char(3)		/* hold a percent sign (%) for print out */
//declare @na_str char(3)		/* holds 'n/a' for 'not applicable' strings */
//declare @zero_str char(80)	/* hold an output string for zero "  0.0" for 
//				** printing zero "% of total" */
//declare @rptline char(80)	/* formatted stats line for print statement */
//declare @section char(80)	/* string to delimit sections on printout */
//
///* ------------- Variables for Tuning Recommendations ------------*/
//declare @recotxt char(80)
//declare @recoline char(80)
//declare @reco_hdr_prn bit
//declare	@ecache_size	int	/* configured size of extended cache */
//declare @ecache_searches int	/* total number of searches in ecache */
//
///* --------- Setup Environment --------- */
//set nocount on			/* disable row counts being sent to client */
//
//select @sum2line   = "  -------------------------  ------------  ------------  ----------"
//select @blankline  = " "
//select @psign      = " %%"		/* extra % symbol because '%' is escape char in print statement */
//select @na_str     = "n/a"
//select @zero_str   = "                                      0.0           0.0           0       n/a"
//select @section = "==============================================================================="
//
//print @section
//print @blankline
//print "Data Cache Management"
//print "---------------------"
//print @blankline
//print "  Cache Statistics Summary (All Caches)"
//print "  -------------------------------------"
//print "                                  per sec      per xact       count  %% of total"
//print "                             ------------  ------------  ----------  ----------"
//print @blankline
//print "    Cache Search Summary"
//
///*
//** get total cache searches on all caches 
//*/
//select @TotalSearches = SUM(convert(bigint, value))	
//  from #tempmonitors
//  where group_name like "buffer_%" and
//		field_name = "bufsearch_calls" 
//
//select @tmp_int = SUM(convert(bigint, value))		/* get cache hits on all caches */
//  from #tempmonitors
//  where group_name like "buffer_%" and
//		field_name = "bufsearch_finds"
//
//select @tmp_int2 = @TotalSearches - @tmp_int  /* calc total cache misses */
//
///* Initilize some variables to avoid divide by zero error */
//if @NumElapsedMs = 0
//begin
//	select @NumElapsedMs = 1
//end
//
//if @NumXacts = 0
//begin
//	select @NumXacts = 1
//end
//
//if @TotalSearches = 0			/* Avoid Divide by Zero Errors */
//	print @zero_str
//else
//  begin
//
//	select @rptline = "      Total Cache Hits" + space(7) +
//			str(@tmp_int / (@NumElapsedMs / 1000.0),12,1) +
//			space(2) +
//			str(@tmp_int / convert(real, @NumXacts),12,1) +
//			space(1) +
//			str(@tmp_int, 11) + space(5) +
//			str(100.0 * @tmp_int / @TotalSearches,5,1) + 
//			@psign
//	print @rptline
//
//	select @rptline="      Total Cache Misses" + space(5) +
//			str(@tmp_int2 / (@NumElapsedMs / 1000.0),12,1) +
//			space(2) +
//			str(@tmp_int2 / convert(real, @NumXacts),12,1) +
//			space(1) +
//			str(@tmp_int2, 11) + space(5) +
//			str(100.0 * @tmp_int2 / @TotalSearches,5,1) + 
//			@psign
//	print @rptline
//
//	print @sum2line 
//	select @rptline = "    Total Cache Searches" + space(5) +
//			str(@TotalSearches / (@NumElapsedMs / 1000.0),12,1) + 
//			space(2) +
//			str(@TotalSearches / convert(real, @NumXacts),12,1) + 
//			space(2) +
//			str(@TotalSearches, 10)
//	print @rptline
//  end
//
///* Print extended cache statistics if extended cache is configured */
//if (@instid is NULL)
//	select @ecache_size = value
//		from master.dbo.sysconfigures
//		where comment = 'extended cache size'
//else
//begin
//	select @ecache_size = value
//		from master.dbo.sysconfigures
//		where comment = 'extended cache size'
//
//
//	if (@ecache_size is NULL)
//		select @ecache_size = value
//			from master.dbo.sysconfigures
//			where comment = 'extended cache size'
//
//end
//
//if @ecache_size > 0
//begin /* { */
//	print @blankline
//	print "    Secondary Cache Search Summary"
//
//	/*
//	** Ecache hit % = (ecache_read / ecache_srchcalls) * 100
//	*/
//
//	/* extended cache search calls */	
//	select @ecache_searches = value
//	  from #tempmonitors
//	  where group_name = "ecache" and
//			field_name = "ecache_srchcalls"
//
//	/* extended cache search hits */	
//	select @tmp_int = value
//	  from #tempmonitors
//	  where group_name = "ecache" and
//			field_name = "ecache_read"
//
//	/* extended cache misses */	
//	select @tmp_int2 = @ecache_searches - @tmp_int
//
//	if @ecache_searches = 0		/* Avoid Divide by Zero Errors */
//		print @zero_str
//	else
//	  begin
//		select @rptline="      Total Cache Hits" + space(7) +
//				str(@tmp_int / (@NumElapsedMs / 1000.0),12,1) +
//				space(2) +
//				str(@tmp_int / convert(real, @NumXacts),12,1) +
//				space(1) +
//				str(@tmp_int, 11) + space(5) +
//				str(100.0 * @tmp_int / @ecache_searches,5,1) + 
//				@psign
//		print @rptline
//
//		select @rptline="      Total Cache Misses" + space(5) +
//				str(@tmp_int2 / (@NumElapsedMs / 1000.0),12,1) +
//				space(2) +
//				str(@tmp_int2 / convert(real, @NumXacts),12,1) +
//				space(1) +
//				str(@tmp_int2, 11) + space(5) +
//				str(100.0 * @tmp_int2 / @ecache_searches,5,1) + 
//				@psign
//		print @rptline
//
//		print @sum2line 
//		select @rptline="    Total Cache Searches" + space(5) +
//				str(@ecache_searches / (@NumElapsedMs / 1000.0),12,1) + 
//				space(2) +
//				str(@ecache_searches / convert(real, @NumXacts),12,1) + 
//				space(2) +
//				str(@ecache_searches, 10)
//		print @rptline
//	  end
//end /* } */
//print @blankline
//
//print "    Cache Turnover"
//
//select @tmp_total = SUM(convert(bigint, value))
//  from #tempmonitors
//  where group_name like "buffer_%" and
//		field_name like "bufgrab_%k" and
//		field_name not like "bufgrab_ref%k"
//
//select @rptline = "      Buffers Grabbed" + space(8) +
//			str(@tmp_total / (@NumElapsedMs / 1000.0),12,1) + 
//			space(2) +
//			str(@tmp_total / convert(real, @NumXacts),12,1) + 
//			space(1) +
//			str(@tmp_total, 11) + space(7) +
//			@na_str
//print @rptline
//
//if @tmp_total != 0			/* Avoid Divide by Zero Errors */
//  begin
//
//	select @tmp_int = SUM(convert(bigint, value))
//	  from #tempmonitors
//	  where group_name like "buffer_%" and
//			field_name like "bufgrab_dirty_%"
//
//	select @rptline = "      Buffers Grabbed Dirty" + space(2) +
//				str(@tmp_int / (@NumElapsedMs / 1000.0),12,1) +
//				space(2) +
//				str(@tmp_int / convert(real, @NumXacts),12,1) +
//				space(1) +
//				str(@tmp_int, 11) + space(5) +
//				str(100.0 * @tmp_int / @tmp_total,5,1) + @psign
//	print @rptline
//  end
//print @blankline
//
//print "    Cache Strategy Summary"
//
///*
//** Sum all buf unkeeps to look at % of buffers following 
//** MRU vs Discard Strategy 
//*/
//
//select @tmp_total = SUM(convert(bigint, value))
//  from #tempmonitors
//  where group_name like "buffer_%" and
//		field_name IN ("bufunkeep_lru", "bufunkeep_mru")
//
//if @tmp_total = 0			/* Avoid Divide by Zero Errors */
//	print @zero_str
//else
//  begin
//
//	select @tmp_int = SUM(convert(bigint, value))
//	  from #tempmonitors
//	  where group_name like "buffer_%" and
//			field_name = "bufunkeep_lru"
//
//	select @rptline = "      Cached (LRU) Buffers" + space(3) +
//				str(@tmp_int / (@NumElapsedMs / 1000.0),12,1) +
//				space(2) +
//				str(@tmp_int / convert(real, @NumXacts),12,1) +
//				space(1) +
//				str(@tmp_int, 11) + space(5) +
//				str(100.0 * @tmp_int / @tmp_total,5,1) + 
//				@psign
//	print @rptline
//
//	select @tmp_int = SUM(convert(bigint, value))
//	  from #tempmonitors
//	  where group_name like "buffer_%" and
//			field_name = "bufunkeep_mru"
//
//	select @rptline = "      Discarded (MRU) Buffers" +
//				str(@tmp_int / (@NumElapsedMs / 1000.0),12,1) +
//				space(2) +
//				str(@tmp_int / convert(real, @NumXacts),12,1) +
//				space(1) +
//				str(@tmp_int, 11) + space(5) +
//				str(100.0 * @tmp_int / @tmp_total,5,1) + 
//				@psign
//	print @rptline
//  end
//print @blankline
//
//print "    Large I/O Usage"
//
//select @tmp_total = SUM(convert(bigint, value))
//  from #tempmonitors
//  where group_name like "buffer_%" and
//		field_name = "prefetch_req"
//
//if @tmp_total = 0			/* Avoid Divide by Zero Errors */
//	print @zero_str
//else
//  begin
//
//	select @tmp_int = SUM(convert(bigint, value))
//	  from #tempmonitors
//	  where group_name like "buffer_%" and field_name IN
//		 ("prefetch_as_requested", "prefetch_page_realign", "prefetch_increase")
//
//	select @rptline = "      Large I/Os Performed" + space(3) +
//				str(@tmp_int / (@NumElapsedMs / 1000.0),12,1) +
//				space(2) +
//				str(@tmp_int / convert(real, @NumXacts),12,1) +
//				space(1) +
//				str(@tmp_int, 11) + space(5) +
//				str(100.0 * @tmp_int / @tmp_total,5,1) + 
//				@psign
//	print @rptline
//	print @blankline
//
//	select @rptline="      Large I/Os Denied due to"
//	print @rptline
//
//	select @tmp_int = SUM(convert(bigint, value))
//	  from #tempmonitors
//	  where group_name like "buffer_%" and
//		field_name = "prefetch_decrease"
//
//        select @tmp_int3 = SUM(convert(bigint, value))
//          from #tempmonitors
//          where group_name like "buffer_%" and
//                field_name = "prefetch_kept_bp"
//
//        select @tmp_int4 = SUM(convert(bigint, value))
//          from #tempmonitors
//                where group_name like "buffer_%" and
//                        field_name = "prefetch_cached_bp"
//
//	select @tmp_int_sum = @tmp_int3 + @tmp_int4
//
//        select @tmp_int = @tmp_int - @tmp_int_sum
//
//	select @rptline = "        Pool < Prefetch Size" + space(1) + 
//			str(@tmp_int / (@NumElapsedMs / 1000.0),12,1) +
//			space(2) +
//			str(@tmp_int / convert(real, @NumXacts),12,1) +
//			space(1) +
//			str(@tmp_int, 11) + space(5) +
//			str(100.0 * @tmp_int / @tmp_total,5,1) + 
//			@psign
//	print @rptline
//
//        select @rptline = "        Pages Requested"
//        print @rptline
//        select @rptline = "        Reside in Another"
//        print @rptline
//
//        select @rptline = "        Buffer Pool" + space(10) +
//                        str(@tmp_int_sum / 
//				(@NumElapsedMs / 
//				1000.0),12,1) +
//                        	space(2) +
//                        	str(@tmp_int_sum / 
//					convert(real, @NumXacts),12,1) +
//                        	space(2) +
//                        	str(@tmp_int_sum, 10) + 
//				space(5) +
//                        	str(100.0 * 
//					@tmp_int_sum / 	
//					@tmp_total,5,1) +
//                        		@psign
//        print @rptline
//
//	print @sum2line 
//	select @rptline = "    Total Large I/O Requests " +
//			str(@tmp_total / (@NumElapsedMs / 1000.0),12,1) + 
//			space(2) +
//			str(@tmp_total / convert(real, @NumXacts),12,1) + 
//			space(2) +
//			str(@tmp_total,10)
//	print @rptline
//  end
//
//print @blankline
//
//print "    Large I/O Effectiveness"
//
///*
//**  calc total # of (logical) pages brought into all caches from all 
//**  large I/Os ( > logical pagesize)
//*/
//
///*
//**  init loop var's to loop through all possible pool sizes 
//*/
//select @numKBperpg = @@maxpagesize/1024
//select @tmp_total = 0, @j = 2*@numKBperpg
//
//while (@j <= 8*@numKBperpg)
//  begin
//
//	/* build pool specific counter name, bufgrab_Nk (ie bufgrab_16k) */
//	select @tmp_cntr = "bufgrab_" + convert(varchar(3), @j) + "k"
//
//	select @tmp_total = @tmp_total + (SUM(convert(bigint, value)) * (@j / @numKBperpg))
//	  from #tempmonitors
//	  where group_name like "buffer_%" and
//			field_name = @tmp_cntr
//
//	select @j = @j * 2
//  end
//  
//select @rptline = "      Pages by Lrg I/O Cached" +
//			str(@tmp_total / (@NumElapsedMs / 1000.0),12,1) + 
//			space(2) +
//			str(@tmp_total / convert(real, @NumXacts),12,1) + 
//			space(1) +
//			str(@tmp_total,11) + space(7) +
//			@na_str
//print @rptline
//
//select @tmp_cntr = "bufgrab_ref_" + convert(varchar(3), @numKBperpg) + "K"
//
//if @tmp_total != 0	/* Avoid Divide by Zero Errors after printout */
//  begin
//	select @tmp_int = SUM(convert(bigint, value))
//	  from #tempmonitors
//	  where group_name like "buffer_%" and
//			field_name like "bufgrab_ref_%K" and
//			field_name != @tmp_cntr
//
//	select @rptline = "      Pages by Lrg I/O Used" + space(2) +
//				str(@tmp_int / (@NumElapsedMs / 1000.0),12,1) +
//				space(2) +
//				str(@tmp_int / convert(real, @NumXacts),12,1) +
//				space(1) +
//				str(@tmp_int, 11) + space(5) +
//				str(100.0 * @tmp_int / @tmp_total,5,1) + 
//				@psign
//	print @rptline
//  end
//print @blankline
//
//
//if exists(select *
//                from #tempmonitors
//                where group_name = "access" and
//                field_name like "apf%")
//begin /*{*/
//
//
//		print "    Asynchronous Prefetch Activity"
//
//		select @tmp_total= SUM(convert(bigint, value))
//		  from #tempmonitors
//		  where group_name = "access" and
//				field_name in ("apf_IOs_issued", "apf_could_not_start_IO_immediately",
//						"apf_configured_limit_exceeded", "apf_unused_read_penalty",
//						"apf_found_in_cache_with_spinlock", "apf_found_in_cache_wo_spinlock")
//
//		if @tmp_total = 0                       /* Avoid Divide by Zero Errors */
//			print @zero_str
//		else
//		  begin
//			select @tmp_int = value
//			  from #tempmonitors
//			  where group_name = "access"
//			  and field_name = "apf_IOs_issued"
//
//			select @rptline = "      APFs Issued" + space(12) +
//						str(@tmp_int / (@NumElapsedMs / 1000.0),12,1) +
//						space(2) +
//						str(@tmp_int / convert(real, @NumXacts),12,1) +
//						space(1) +
//						str(@tmp_int, 11) + space(5) +
//						str(100.0 * @tmp_int / @tmp_total,5,1) +
//						@psign
//			print @rptline
//
//			select @rptline = "      APFs Denied Due To" 
//			print @rptline
//
//
//			select @tmp_int = value
//			  from #tempmonitors
//			  where group_name = "access"
//			  and field_name = "apf_could_not_start_IO_immediately"
//			
//			select @rptline = "        APF I/O Overloads " + space(3) +
//					       str(@tmp_int / (@NumElapsedMs / 1000.0),12,1) +
//					       space(2) +
//					       str(@tmp_int / convert(real, @NumXacts),12,1) +
//					       space(1) +
//					       str(@tmp_int, 11) + space(5) +
//					       str(100.0 * @tmp_int / @tmp_total,5,1) +
//					       @psign
//			print @rptline
//
//			select @tmp_int = value
//			  from #tempmonitors
//			  where group_name = "access"
//			  and field_name = "apf_configured_limit_exceeded"
//
//			select @rptline = "        APF Limit Overloads " + space(1) +
//					       str(@tmp_int / (@NumElapsedMs / 1000.0),12,1) +
//					       space(2) +
//					       str(@tmp_int / convert(real, @NumXacts),12,1) +
//					       space(1) +
//					       str(@tmp_int, 11) + space(5) +
//					       str(100.0 * @tmp_int / @tmp_total,5,1) + 
//					       @psign
//			print @rptline
//
//			select @tmp_int = value
//			  from #tempmonitors
//			  where group_name = "access"
//			  and field_name = "apf_unused_read_penalty"
//			
//			select @rptline = "        APF Reused Overloads " + 
//					       str(@tmp_int / (@NumElapsedMs / 1000.0),12,1) +
//					       space(2) +
//					       str(@tmp_int / convert(real, @NumXacts),12,1) +
//					       space(1) +
//					       str(@tmp_int, 11) + space(5) +
//					       str(100.0 * @tmp_int / @tmp_total,5,1) +
//					       @psign
//			print @rptline
//
//			select @rptline = "      APF Buffers Found in Cache" 
//			print @rptline
//
//			select @tmp_int = value
//			  from #tempmonitors
//			  where group_name = "access"
//			  and field_name = "apf_found_in_cache_with_spinlock"
//			
//			select @rptline = "        With Spinlock Held" + space(3) +
//					       str(@tmp_int / (@NumElapsedMs / 1000.0),12,1) +
//					       space(2) +
//					       str(@tmp_int / convert(real, @NumXacts),12,1) +
//					       space(1) +
//					       str(@tmp_int, 11) + space(5) +
//					       str(100.0 * @tmp_int / @tmp_total,5,1) +
//					       @psign
//			print @rptline
//
//			select @tmp_int = value
//			  from #tempmonitors
//			  where group_name = "access"
//			  and field_name = "apf_found_in_cache_wo_spinlock"
//			
//			select @rptline = "        W/o Spinlock Held " + space(3) +
//					       str(@tmp_int / (@NumElapsedMs / 1000.0),12,1) +
//					       space(2) +
//					       str(@tmp_int / convert(real, @NumXacts),12,1) +
//					       space(1) +
//					       str(@tmp_int, 11) + space(5) +
//					       str(100.0 * @tmp_int / @tmp_total,5,1) +
//					       @psign
//			print @rptline
//
//			print @sum2line
//			select @rptline = "    Total APFs Requested " + space(4) +
//					str(@tmp_total / (@NumElapsedMs / 1000.0),12,1) +
//					space(2) +
//					str(@tmp_total / convert(real, @NumXacts),12,1) +
//					space(2) +
//					str(@tmp_total,10)
//			print @rptline
//		  end
//
//		print @blankline
//
//		print "    Other Asynchronous Prefetch Statistics"
//
//		select @tmp_int = value
//			  from #tempmonitors
//			  where group_name = "access"
//			  and field_name = "apf_IOs_used"
//
//		select @rptline = "      APFs Used" + space(14) +
//						str(@tmp_int / (@NumElapsedMs / 1000.0),12,1) +
//						space(2) +
//						str(@tmp_int / convert(real, @NumXacts),12,1) +
//						space(1) +
//						str(@tmp_int, 11) + space(7) +
//						@na_str
//		print @rptline
//
//		select @tmp_int = value
//			from #tempmonitors
//			where group_name = "access"
//			and field_name = "apf_waited_for_IO_to_complete"
//
//		select @rptline = "      APF Waits for I/O" + space(6) +
//			       str(@tmp_int / (@NumElapsedMs / 1000.0),12,1) +
//			       space(2) +
//			       str(@tmp_int / convert(real, @NumXacts),12,1) +
//			       space(1) +
//			       str(@tmp_int, 11) + space(7) +
//			       @na_str
//		print @rptline
//
//		select @tmp_int = SUM(convert(bigint, value))
//			from #tempmonitors
//			where field_name like "apf%discard%"
//
//		select @rptline = "      APF Discards" + space(11) +
//			       str(@tmp_int / (@NumElapsedMs / 1000.0),12,1) +
//			       space(2) +
//			       str(@tmp_int / convert(real, @NumXacts),12,1) +
//			       space(1) +
//			       str(@tmp_int, 11) + space(7) +
//			       @na_str
//		print @rptline
//end /*}*/
//print @blankline
//
//
//print "    Dirty Read Behavior"
//	
//select @tmp_total = SUM(convert(bigint, value))
//  from #tempmonitors
//  where group_name like "buffer_%" and
//		field_name = "level0_bufpredirty"
//
//select @rptline = "      Page Requests" + space(10) +
//			str(@tmp_total / (@NumElapsedMs / 1000.0),12,1) + 
//			space(2) +
//			str(@tmp_total / convert(real, @NumXacts),12,1) + 
//			space(1) +
//			str(@tmp_total,11) + space(7) +
//			@na_str
//print @rptline
//
//if @tmp_total != 0	/* Avoid Divide by Zero Errors after printout */
//  begin
//
//	select @tmp_int = value
//	  from #tempmonitors
//	  where group_name = "access" and
//			field_name = "dirty_read_restarts"
//
//	if @tmp_int != 0
//	 begin
//		select @rptline = "      Re-Starts" + space(10) +
//				str(@tmp_int / (@NumElapsedMs / 1000.0),12,1) +
//				space(2) +
//				str(@tmp_int / convert(real, @NumXacts),12,1) +
//				space(1) +
//				str(@tmp_int, 11) + space(5) +
//				str(100.0 * @tmp_int / @tmp_total,5,1) + 
//				@psign
//		print @rptline
//	 end
//  end
//print @blankline
//
///*
//** If requested, print global cache recommendations (if any)
//*/
//
//if @Reco = 'Y'
// begin
//	select @recotxt =     "  Tuning Recommendations for All Caches"
//	select @recoline = "  -------------------------------------"
//        select @reco_hdr_prn = 0
//        /* recommendations for apf */
//        select @tmp_float = convert(int, (100.0*a.value/b.value))
//        	from #tempmonitors a, #tempmonitors b
//        	where   a.group_name = "access"
//        	and     a.field_name = "apf_IOs_issued"
//        	and     b.group_name = "access"
//        	and     b.field_name in ("apf_IOs_issued", "apf_could_not_start_IO_immediately",
//                                        "apf_configured_limit_exceeded", "apf_unused_read_penalty",
//                                        "apf_found_in_cache_with_spinlock", "apf_found_in_cache_wo_spinlock")
//		and 	b.value != 0 
//	 if (@tmp_float is not null)
//	 begin
//        	select @tmp_int = value
//        	from #tempmonitors
//        	where   group_name = "access"
//        	and     field_name = "apf_configured_limit_exceeded"
//
//		/*
//		** If the number of APF I/O's issued is greater
//		** than 80% and if the APF configured limit 
//		** exceeded 0 during the sampling interval
//		** consider increasing the apf limits for the
//		** pool or globally for all the pools
//		*/
//        	if (@tmp_float > 80.0 and @tmp_int > 0)
//         	begin
//               	 	if (@reco_hdr_prn = 0)
//               	 	begin
//                       		 print @recotxt
//                       		 print @recoline
//                       	 	 select @reco_hdr_prn = 1
//                	end
//                	print "  - Consider increasing the 'global asynchronous prefetch limit' parameter"
//                	print "    (and the 'local asynchronous prefetch limit' parameter for each pool " 
//                	print "    for which this was overidden) by 10%%." 
//                	print @blankline
//         	end
// 	 end
// end
//
//return 0
//go








//-----------------------------------------------------------------------------------------------------------------------------------------------------------------------
//sp_sysmon_dcache_dtl, from: Adaptive Server Enterprise/16.0 SP02 PL02/EBF 25320 SMP/P/x86_64/Enterprise Linux/ase160sp02plx/2492/64-bit/FBO/Sat Nov 21 04:05:39 2015 
//-----------------------------------------------------------------------------------------------------------------------------------------------------------------------

//use sybsystemprocs
//go
//IF EXISTS (SELECT 1 FROM sysobjects
//           WHERE name = 'sp_sysmon_dcache_dtl'
//             AND id = object_id('sp_sysmon_dcache_dtl')
//             AND type = 'P')
//	DROP PROCEDURE sp_sysmon_dcache_dtl
//go
//
//
//create or replace procedure sp_sysmon_dcache_dtl
//        @NumEngines smallint,    /* number of engines online */
//        @NumElapsedMs int,      /* for "per Elapsed second" calculations */
//        @NumXacts int,          /* for per transactions calculations */
//        @Reco   char(1)         /* Flag for recommendations             */
//as
///* --------- declare local variables --------- */
//declare @CacheName varchar(255)  /* Cache Name from cache id lookup */
//declare @CacheID smallint       /* Cache ID to map to buffer_N group */
//declare @NumCaches smallint     /* Number of Caches to Report On */
//declare @TotalSearches bigint   /* Total Cache Searches on All Caches */
//declare @j smallint             /* loop index to iterate through multi-counter
//                                ** counters (pool...) */
//declare @lrgpool tinyint        /* boolean (0=No, 1=yes) logic to print
//                                ** "Lrg Pool Not Used" Msg
//                                */
//declare @gtlogpgszpool tinyint    /* Boolean set while looking for a pool 
//                                **  > logical pagesize in current cache
//                                **      0 : Did not find pool
//                                **      1 : Did find a pool of current size.
//                                */
//declare @tmp_grp varchar(25)    /* temp var for build group_name's
//                                ** ie. engine_N, disk_N */
//declare @cfg_repl varchar(24)   /* configured value of replacement policy */
//declare @run_repl varchar(24)   /* run value of replacement policy */
//declare @c_status varchar(24)   /* Cache status */
//declare @c_type   varchar(24)   /* Cache type */
//declare @tmp_cntr varchar(35)   /* temp var for build field_name's
//                                ** ie. bufgrab_Nk */
//				/* temp var for building fieldname for
//				** searches found in buffer pool */
//declare @srchfound_cntr varchar(35)
//declare @tmp_int bigint         /* temp var for integer storage */
//declare @tmp_int2 int           /* temp var for integer storage */
//declare @tmp_elc int            /* temp var for integer storage */
//declare @tmp_lockless int       /* temp var for integer storage */
//declare @tmp_wash int           /* temp var for integer storage */
//declare @tmp_int3 int           /* temp var for integer storage used to read 
//                                ** value of counter 'prefetch_kept_bp' */
//declare @tmp_int4 int           /* temp var for integer storage used to read 
//                                ** value of counter 'prefetch_cached_bp' */
//declare @tmp_int_sum int        /* temp var for integer storage
//                                ** @tmp_int_sum = @tmp_int3 + @tmp_int4 */
//declare @tmp_total bigint       /* temp var for summing 'total #s' data */
//declare @tmp_total_send bigint  /* temp var for summing 'total #s' data */
//declare @tmp_total_recv bigint  /* temp var for summing 'total #s' data */
//declare @tmp_float float        /* temp var for float storage */
//declare @tmp_float2 float       /* temp var for float storage */
//declare @numKBperpg int		/* number of kilobytes per logical page */
//declare @subsection char(80)    /* string to delimit subsections on printout */
//declare @sum1line char(80)      /* string to delimit total lines without
//                                ** percent calc on printout */
//declare @sum2line char(67)      /* string to delimit total lines with
//                                ** percent calc on printout */
//declare @blankline char(1)      /* to print blank line */
//declare @psign char(3)          /* hold a percent sign (%) for print out */
//declare @na_str char(3)         /* holds 'n/a' for 'not applicable' strings */
//declare @zero_str char(80)      /* hold an output string for zero "  0.0" for
//                                ** printing zero "% of total" */
//declare @rptline varchar(530)    /* formatted stats line for print statement */
//declare @section char(80)       /* string to delimit sections on printout */
//declare @totalrequested bigint	/* total no of physical locks requested */
//declare @totalretained bigint	/* total no of physical locks retained */
//declare @totaldiskreads bigint	/* total no of diskreads completed for physical
//				** lock acquisition.*/
//declare @totaldeadlocks bigint  /* total no of deadlocks occured while taking
//				** physical lock. */
//declare @totaltransfers bigint	/* total no. of buffers transferred to this
//				** instance. */
//declare @totalwaits bigint	/* totan no. of times tasks had to wait for a
//				** transfer to this instance. */
//declare @totaldiskwrites bigint	/* total no of disk writes when a transfer
//				** is requested.*/
//declare @totallockmgrcalls bigint	/* total no. of times lock manager was 
//				** consulted for physical lock. */
//declare @NumElapsedSec real	/* No. of elapsed seconds */
//
///* ------------- Variables for Tuning Recommendations ------------*/
//declare @recotxt varchar(300)
//declare @recoline char(80)
//declare @reco_hdr_prn bit
//declare @spinlock_contention float
//
///* --------- Setup Environment --------- */
//set nocount on			/* disable row counts being sent to client */
//
//select @subsection = "-------------------------------------------------------------------------------"
//select @sum1line   = "  -------------------------  ------------  ------------  ----------  ----------"
//select @sum2line   = "  -------------------------  ------------  ------------  ----------"
//select @blankline  = " "
//select @psign      = " %%"              /* extra % symbol because '%' is escape char in print statement */
//select @na_str     = "n/a"
//select @zero_str   = "                                      0.0           0.0           0       n/a"
//select @section = "==============================================================================="
//
///*
//** Declare cursor to walk temp cache table in cache name
//** order to print cache-specific statistics
//*/
//select @TotalSearches = SUM(convert(bigint, value))
//  from #tempmonitors
//  where group_name like "buffer_%" and
//                field_name = "bufsearch_calls"
//
//declare cache_info cursor for
//	select cid, name, group_name, cache_type,
//		ltrim(rtrim(config_replacement)),ltrim(rtrim(run_replacement))
//	  from #cachemap
//	  order by name
//	  for read only
//
//open cache_info
//fetch cache_info into @CacheID, @CacheName, @tmp_grp, @c_type, @cfg_repl,@run_repl
//
///* 
//** Get all Spinlock related counters and cache names
//** for printing potential spinlock contention.
//*/
//select  P.field_name as name,
//	P.value as grabs,
//	W.value as waits,
//	S.value as spins into #foo
//from #tempmonitors P, #tempmonitors W, #tempmonitors S
//where
//	    P.group_name = "spinlock_p"
//	and W.group_name = "spinlock_w"
//	and S.group_name = "spinlock_s"
//	and P.field_id = W.field_id
//	and P.field_id = S.field_id
//	and P.field_name in ( select name from #cachemap )
//
///* Create a #temp table to store information on currently configured
//** pools for each cache.
//*/
// 
///* Initilize some variables to avoid divide by zero error */
//if @NumElapsedMs = 0
//begin
//	select @NumElapsedMs = 1
//end
//
//if @NumXacts = 0
//begin
//	select @NumXacts = 1
//end
//
//select @NumElapsedSec = @NumElapsedMs / 1000.0
//
//while (@@sqlstatus = 0) /* { */
//  begin
//	print @subsection
//	select @rptline = space(2) + "Cache: " + @CacheName
//	print @rptline
//	print "                                  per sec      per xact       count  %% of total"
//	print @sum1line
//
//	/* Print spinlock contention */
//
//	select @spinlock_contention =
//			isnull(100.0*(sum(convert(float,waits))/sum
//				(case when grabs >= 0 then convert(float,grabs)
//				else (power(2.0,32) + convert(float,grabs)) end)), 0)
//	  from #foo
//	 where name = @CacheName
//	   and grabs != 0
// 
//	select @rptline = "    Spinlock Contention" + space(15) +
//						@na_str + space(11) +
//						@na_str + space(9) +
//						@na_str + space(5) +
//						str( @spinlock_contention,5,1) 
//							+ @psign
//	print @rptline
//	print @blankline
//
//	if @TotalSearches != 0
//	  begin
//		select @tmp_int = value
//		  from #tempmonitors
//		  where group_name = @tmp_grp and
//				field_name = "bufsearch_calls"
//
//		select @rptline = "    Utilization " + space(22) +
//						@na_str + space(11) +
//						@na_str + space(9) +
//						@na_str + space(5) +
//						str(100.0 * @tmp_int / 
//							@TotalSearches,5,1) + 
//						@psign
//
//		print @rptline
//		print @blankline
//	  end
//
//	print "    Cache Searches"
//
//	select @tmp_total = value
//	  from #tempmonitors
//	  where group_name = @tmp_grp and
//			field_name = "bufsearch_calls"
//
//	if @tmp_total = 0		/* Avoid Divide by Zero Errors */
// 	begin
//		select @rptline = "      Total Cache Searches            0.0           0.0           0       n/a"
//  		print @rptline
// 	end
//	else
//	  begin
//
//		select @tmp_int = value
//		  from #tempmonitors
//		  where group_name = @tmp_grp and
//				field_name = "bufsearch_finds"
//
//		select @rptline = "      Cache Hits" + space(13) +
//				str(@tmp_int / (@NumElapsedMs / 1000.0),12,1) +
//				space(2) +
//				str(@tmp_int / convert(real, @NumXacts),12,1) +
//				space(1) +
//				str(@tmp_int, 11) + space(5) +
//				str(100.0 * @tmp_int / @tmp_total,5,1) + 
//				@psign
//		print @rptline
//
//		/* save hits for wash % and missed calc */
//		select @tmp_int2 = @tmp_int
//
//		select @tmp_int = value
//		  from #tempmonitors
//		  where group_name = @tmp_grp and
//				field_name = "bufsearch_finds_in_wash"
//	
//		if @tmp_int2 != 0
//		begin
//			select @rptline = "         Found in Wash" + space(7) +
//				str(@tmp_int / (@NumElapsedMs / 1000.0),12,1) +
//				space(2) +
//				str(@tmp_int / convert(real, @NumXacts),12,1) +
//				space(1) +
//				str(@tmp_int, 11) + space(5) +
//				str(100.0 * @tmp_int / @tmp_int2, 5, 1) + 
//				@psign
//			print @rptline
//		end
//
//		/*
//		** Print the split between all the cache hits only for the case of 
//		** Lockless data caches
//		*/
//		if @c_type = "Lockless Data Cache"
//		begin
//			select @tmp_wash = @tmp_int
//	
//			select @tmp_int = value
//			  from #tempmonitors
//			  where group_name = @tmp_grp and
//				field_name = "bufsearch_finds_in_elc"
//	
//			if @tmp_int2 != 0
//			begin
//				select @rptline = "                   ELC" + space(7) +
//					str(@tmp_int / (@NumElapsedMs / 1000.0),12,1) +
//					space(2) +
//					str(@tmp_int / convert(real, @NumXacts),12,1) +
//					space(1) +
//					str(@tmp_int, 11) + space(5) +
//					str(100.0 * @tmp_int / @tmp_int2, 5, 1) + 
//					@psign
//				print @rptline
//			end
//
//			select @tmp_elc = @tmp_int
//
//			select @tmp_int = value
//			  from #tempmonitors
//		  	where group_name = @tmp_grp and
//				field_name = "buf_cachesearch_lockless_finds"
//	
//			if @tmp_int2 != 0
//			begin
//				select @rptline = "      w/o spinlockheld" + space(7) +
//					str(@tmp_int / (@NumElapsedMs / 1000.0),12,1) +
//					space(2) +
//					str(@tmp_int / convert(real, @NumXacts),12,1) +
//					space(1) +
//					str(@tmp_int, 11) + space(5) +
//					str(100.0 * @tmp_int / @tmp_int2, 5, 1) + 
//					@psign
//				print @rptline
//			end
//
//			select @tmp_lockless = @tmp_int
//
//			if @tmp_int2 != 0
//			begin
//				select @tmp_int = @tmp_int2 - @tmp_lockless - @tmp_elc - @tmp_wash
//				select @rptline = "     with spinlockheld" + space(7) +
//					str(@tmp_int / (@NumElapsedMs / 1000.0),12,1) +
//					space(2) +
//					str(@tmp_int / convert(real, @NumXacts),12,1) +
//					space(1) +
//					str(@tmp_int, 11) + space(5) +
//					str(100.0 * @tmp_int / @tmp_int2, 5, 1) + 
//					@psign
//				print @rptline
//			end
//		end
//		select @tmp_int = @tmp_total - @tmp_int2  /* missed searches */
//
//		select @rptline = "      Cache Misses" + space(11) +
//				str(@tmp_int / (@NumElapsedMs / 1000.0),12,1) +
//				space(2) +
//				str(@tmp_int / convert(real, @NumXacts),12,1) +
//				space(1) +
//				str(@tmp_int, 11) + space(5) +
//				str(100.0 * @tmp_int / @tmp_total,5,1) + 
//				@psign
//		print @rptline
//      end	/* else @tmp_total = 0 */
//
//	print @sum2line
//	select @rptline = "    Total Cache Searches" + space(5) +
//			str(@tmp_total / (@NumElapsedMs / 1000.0),12,1) + 
//			space(2) +
//			str(@tmp_total / convert(real, @NumXacts),12,1) + 
//			space(2) +
//			str(@tmp_total,10)
//	print @rptline
//
//	print @blankline
//
//
//	select @tmp_total = SUM(convert(bigint, value))
//	  from #tempmonitors
//	  where group_name = @tmp_grp and
//			field_name like "bufgrab_%k" and field_name not like "bufgrab_ref%k"
//
//	select @numKBperpg = @@maxpagesize/1024
//	if @tmp_total = 0
// 	begin
//		select @rptline = "    Pool Turnover                     0.0           0.0           0       n/a"
//  		print @rptline
// 	end
//	else
//          begin /* { */
//		print "    Pool Turnover"
//		/* init loop ctr to loop through all pool sizes */
//		select @j = @numKBperpg
//                while (@j <= 8*@numKBperpg)        /* { */
//		  begin
//
//			/* 
//			**  build pool specific counter name, 
//			**  bufgrab_Nk (ie bufgrab_16k) 
//			*/
//			select @tmp_cntr = "bufgrab_" + rtrim(convert(varchar(3), @j)) + "k"
//
//			select @tmp_int = value
//			  from #tempmonitors
//			  where group_name = @tmp_grp and
//					field_name = @tmp_cntr
//
//                        if @tmp_int != 0        /* { */
//			  begin
//
//				select @rptline = space(6) + 
//					convert(char(3),@j) + "Kb Pool"
//				print @rptline
//
//				select @rptline = "          LRU Buffer Grab" +
//					space(4) +
//					str(@tmp_int / (@NumElapsedMs / 
//						1000.0),12,1) + 
//					space(2) +
//					str(@tmp_int / 
//						convert(real, @NumXacts),12,1)+
//					space(1) +
//					str(@tmp_int, 11) + space(5) +
//					str(100.0 * @tmp_int / @tmp_total,5,1)+
//					@psign
//				print @rptline
//
//				select @tmp_cntr = "bufgrab_locked_" + 
//					convert(varchar(3), @j) + "k"
//
//				select @tmp_int2 = value
//				  from #tempmonitors
//				  where group_name = @tmp_grp and
//						field_name = @tmp_cntr
//
//				select @rptline = space(12) + "Grabbed Locked Buffer" +
//					space(1) +
//					str(@tmp_int2 / (@NumElapsedMs / 
//						1000.0),7,1) + 
//					space(2) +
//					str(@tmp_int2 / 
//						convert(real, @NumXacts),12,1)+
//					space(1) +
//					str(@tmp_int2, 11) + space(5) +
//					str(100.0 * @tmp_int2 / @tmp_int,5,1) +
//					@psign
//				print @rptline
//
//				select @tmp_cntr = "bufgrab_dirty_" + 
//					convert(varchar(3), @j) + "k"
//
//				select @tmp_int2 = value
//				  from #tempmonitors
//				  where group_name = @tmp_grp and
//						field_name = @tmp_cntr
//
//				select @rptline = space(12) + "Grabbed Dirty" +
//					space(4) +
//					str(@tmp_int2 / (@NumElapsedMs / 
//						1000.0),12,1) + 
//					space(2) +
//					str(@tmp_int2 / 
//						convert(real, @NumXacts),12,1)+
//					space(1) +
//					str(@tmp_int2, 11) + space(5) +
//					str(100.0 * @tmp_int2 / @tmp_int,5,1) +
//					@psign
//				print @rptline
//
//                          end   /* } if @tmp_int != 0 */
//			  
//			/* get next pool size (power of 2) */
//			select @j = @j * 2 
//
//                end             /* } while */
//
//
//		print @sum2line		/* calc cache turnover percent of all caches */
//		select @rptline = "    Total Cache Turnover" + space(5) +
//			str(@tmp_total / (@NumElapsedMs / 1000.0),12,1) + 
//			space(2) +
//			str(@tmp_total / convert(real, @NumXacts),12,1) + 
//			space(2) +
//			str(@tmp_total,10)
//		print @rptline
//          end   /* } else @tmp_total != 0 */
//	print @blankline
//
//	print "    Cluster Cache Behavior"
//
//	select @totalrequested = SUM(convert(bigint, value))
//		from #tempmonitors
//		where group_name = @tmp_grp and
//			field_name like "physical_lock_acquisition"
//
//	select @totalretained = SUM(convert(bigint, value))
//		from #tempmonitors
//		where group_name = @tmp_grp and
//				field_name = "physical_lock_retented"
//
//	select @totaldeadlocks = SUM(convert(bigint, value))
//		from #tempmonitors
//		where group_name = @tmp_grp and
//				field_name = "physical_lock_deadlock"
//
//	select @totalwaits = SUM(convert(bigint, value))
//		from #tempmonitors
//		where group_name = @tmp_grp and
//				field_name = "waited_on_tx"
//
//	select @totaltransfers = SUM(convert(bigint, value))
//		from #tempmonitors
//		where group_name = @tmp_grp and
//			field_name = "physical_lock_txrecv"
//
//	select @totaldiskreads = SUM(convert(bigint, value))
//		from #tempmonitors
//		where group_name = @tmp_grp and
//				field_name = "diskread_lockmgr"
//
//	select @totaldiskwrites = SUM(convert(bigint, value))
//		from #tempmonitors
//		where group_name = @tmp_grp and
//				field_name = "diskwrite"
//
//	select @totallockmgrcalls = SUM(convert(bigint, value))
//		from #tempmonitors
//		where group_name = @tmp_grp and
//			field_name = "physical_lock_lockmgr"
//
//	if @totalrequested = 0
//	begin
//		print "      No physical locks are acquired on buffers in this cache"
//	end
//	else
//	begin
//		select @rptline = "      Total Lock Requests    " +
//			str(@totalrequested / @NumElapsedSec,12,1) +
//			space(2) +
//			str(@totalrequested / convert(real, @NumXacts),12,1) +
//			space(1) +
//			str(@totalrequested, 11) + space(5) +
//			str(100.0 * @totalrequested / @totalrequested,5,1) +
//			@psign
//		print @rptline
//		
//		select @rptline = "      Retained Locks         " +
//			str(@totalretained / @NumElapsedSec,12,1) +
//			space(2) +
//			str(@totalretained / convert(real, @NumXacts),12,1) +
//			space(1) +
//			str(@totalretained, 11) + space(5) +
//			str(100.0 * @totalretained / @totalrequested,5,1) +
//			@psign
//		print @rptline
//		
//		select @rptline = "      Non-retained Locks     " +
//			str((@totalrequested - @totalretained) / 
//					@NumElapsedSec,12,1) +
//			space(2) +
//			str((@totalrequested - @totalretained) / 
//					convert(real, @NumXacts),12,1) +
//			space(1) +
//			str((@totalrequested - @totalretained), 11) + space(5) +
//			str(100.0 * (@totalrequested - @totalretained) / 
//					@totalrequested,5,1) +
//			@psign
//		print @rptline
//		
//		select @rptline = "      Data Read from Disk    " +
//			str(@totaldiskreads / @NumElapsedSec,12,1) +
//			space(2) +
//			str(@totaldiskreads / convert(real, @NumXacts),12,1) +
//			space(1) +
//			str(@totaldiskreads, 11) + space(5) +
//			str(100.0 * @totaldiskreads / @totalrequested,5,1) +
//			@psign
//		print @rptline
//		
//		select @tmp_total_recv = SUM(convert(bigint, value))
//			from #tempmonitors
//			where group_name like "buffer_%" and
//				field_name = "physical_lock_txrecv"
//
//		if @tmp_total_recv > 0
//		begin
//			select @rptline = "      Transfers Received     " +
//				str(@totaltransfers / @NumElapsedSec,12,1) +
//				space(2) +
//				str(@totaltransfers / 
//					convert(real, @NumXacts),12,1) +
//				space(1) +
//				str(@totaltransfers, 11) + space(5) +
//				str(100.0 * @totaltransfers / 
//					@totalrequested,5,1) + 
//				@psign
//			print @rptline
//		end
//		else
//		begin
//			select @rptline = "      Transfers Received     " +
//				str(0.0,12,1) +
//				space(2) +
//				str(0.0,12,1) +
//				space(1) +
//				str(0.0, 11) + space(5) + 
//				str(0.0,5,1) + @psign
//			print @rptline
//		end
//
//		select @rptline = "      Waited for Data Transfer" +
//			str(@totalwaits / @NumElapsedSec,11,1) +
//			space(2) +
//			str(@totalwaits / convert(real, @NumXacts),12,1) +
//			space(1) +
//			str(@totalwaits, 11) + space(5) +
//			str(100.0 * @totalwaits / @totalrequested,5,1) +
//			@psign
//		print @rptline
//		
//		select @rptline = "      Deadlocks              " +
//			str(@totaldeadlocks / @NumElapsedSec,12,1) +
//			space(2) +
//			str(@totaldeadlocks / convert(real, @NumXacts),12,1) +
//			space(1) +
//			str(@totaldeadlocks, 11) + space(5) +
//			str(100.0 * @totaldeadlocks / @totalrequested,5,1) +
//			@psign
//		print @rptline
//		
//		select @rptline = "      Data Write to Disk     " +
//			str(@totaldiskwrites / @NumElapsedSec,12,1) +
//			space(2) +
//			str(@totaldiskwrites / convert(real, @NumXacts),12,1) +
//			space(1) +
//			str(@totaldiskwrites, 11) + space(5) + "  n/a "
//		print @rptline
//		
//		select @tmp_total_send = SUM(convert(bigint, value))
//			from #tempmonitors
//			where group_name like "buffer_%" and
//				field_name = "physical_lock_txsend"
//
//		if @tmp_total_send > 0
//		begin
//			select @tmp_int = SUM(convert(bigint, value))
//				from #tempmonitors
//				where group_name = @tmp_grp and
//					field_name = "physical_lock_txsend"
//
//			select @rptline = "      Transfers Sent         " +
//				str(@tmp_int / @NumElapsedSec,12,1) +
//				space(2) +
//				str(@tmp_int / convert(real, @NumXacts),12,1) +
//				space(1) +
//				str(@tmp_int, 11) + space(5) + "  n/a "
//			print @rptline
//		end
//		else
//		begin
//			select @rptline = "      Transfers Sent         " +
//				str(0.0,12,1) +
//				space(2) +
//				str(0.0,12,1) +
//				space(1) +
//				str(0.0, 11) + space(5) + "  n/a "
//			print @rptline
//		end
//
//		if (@totallockmgrcalls) = 0
//		begin
//			select @rptline = "      Data Location Efficiency " +
//					space(7) +
//					@na_str + space(11) +
//					@na_str + space(9) +
//					@na_str + space(5) +
//					str(100.0,5,1) +
//					@psign
//		end
//		else
//		begin
//			select @rptline = "      Data Location Efficiency " +
//					space(7) +
//					@na_str + space(11) +
//					@na_str + space(9) +
//					@na_str + space(5) +
//					str(100.0 - (100.0 * @totaltransfers /
//					(@totallockmgrcalls)),5,1) +
//					@psign
//		end
//		print @rptline
//	end
//
//	print @blankline
//
//	print "    Buffer Wash Behavior"
//
//	select @tmp_total = value
//	  from #tempmonitors
//	  where group_name = @tmp_grp and
//			field_name = "bufwash_throughput"
//
//	if @tmp_total != 0	/* any buffers move through wash yet? */
//	  begin
//
//		select @tmp_int = value
//		  from #tempmonitors
//		  where group_name = @tmp_grp and
//				field_name = "bufwash_pass_clean"
//
//		select @rptline = "      Buffers Passed Clean" + space(3) +
//				str(@tmp_int / (@NumElapsedMs / 1000.0),12,1) +
//				space(2) +
//				str(@tmp_int / convert(real, @NumXacts),12,1) +
//				space(1) +
//				str(@tmp_int, 11) + space(5) +
//				str(100.0 * @tmp_int / @tmp_total,5,1) + 
//				@psign
//		print @rptline
//
//		select @tmp_int = value
//		  from #tempmonitors
//		  where group_name = @tmp_grp and
//				field_name = "bufwash_pass_writing"
//
//		select @rptline = "      Buffers Already in I/O " +
//				str(@tmp_int / (@NumElapsedMs / 1000.0),12,1) +
//				space(2) +
//				str(@tmp_int / convert(real, @NumXacts),12,1) +
//				space(1) +
//				str(@tmp_int, 11) + space(5) +
//				str(100.0 * @tmp_int / @tmp_total,5,1) + 
//				@psign
//		print @rptline
//
//		select @tmp_int = value
//		  from #tempmonitors
//		  where group_name = @tmp_grp and
//				field_name = "bufwash_write_dirty"
//
//		select @rptline = "      Buffers Washed Dirty   " +
//				str(@tmp_int / (@NumElapsedMs / 1000.0),12,1) +
//				space(2) +
//				str(@tmp_int / convert(real, @NumXacts),12,1) +
//				space(1) +
//				str(@tmp_int, 11) + space(5) +
//				str(100.0 * @tmp_int / @tmp_total,5,1) + 
//				@psign
//		print @rptline
//	  end
//	else
//		print "      Statistics Not Available - No Buffers Entered Wash Section Yet"
//
//	print @blankline
//	
//	print "    Cache Strategy"
//
//	/* 
//	** Sum all buf unkeeps to look at % of buffers following 
//	** MRU vs Discard Strategy 
//	*/
//	select @tmp_total = SUM(convert(bigint, value))
//	  from #tempmonitors
//	  where group_name = @tmp_grp and
//			field_name IN ("bufunkeep_lru", "bufunkeep_mru")
//
//	if @tmp_total != 0
//	  begin
//		select @tmp_int = value
//		  from #tempmonitors
//		  where group_name = @tmp_grp and
//				field_name = "bufunkeep_lru"
//
//		select @rptline = "      Cached (LRU) Buffers   " +
//				str(@tmp_int / (@NumElapsedMs / 1000.0),12,1) +
//				space(2) +
//				str(@tmp_int / convert(real, @NumXacts),12,1) +
//				space(1) +
//				str(@tmp_int, 11) + space(5) +
//				str(100.0 * @tmp_int / @tmp_total,5,1) + 
//				@psign
//		print @rptline
//
//		select @tmp_int = value
//		  from #tempmonitors
//		  where group_name = @tmp_grp and
//				field_name = "bufunkeep_mru"
//
//		select @rptline = "      Discarded (MRU) Buffers"+
//				str(@tmp_int / (@NumElapsedMs / 1000.0),12,1) +
//				space(2) +
//				str(@tmp_int / convert(real, @NumXacts),12,1) +
//				space(1) +
//				str(@tmp_int, 11) + space(5) +
//				str(100.0 * @tmp_int / @tmp_total,5,1) + 
//				@psign
//		print @rptline
//	  end
//	else
//		print "      Statistics Not Available - No Buffers Displaced Yet"
//
//	print @blankline
//
//	print "    Large I/O Usage"	
//
//	select @tmp_total = value
//	  from #tempmonitors
//	  where group_name = @tmp_grp and
//			field_name = "prefetch_req"
//
//	if @tmp_total = 0
// 	begin
//		select @rptline = "      Total Large I/O Requests        0.0           0.0           0       n/a"
//  		print @rptline
// 	end
//	else
//	  begin
//
//		select @tmp_int = SUM(convert(bigint, value))
//		  from #tempmonitors
//		  where group_name = @tmp_grp and field_name IN
//			 ("prefetch_as_requested", "prefetch_page_realign", "prefetch_increase")
//
//		select @rptline = "      Large I/Os Performed" + space(3) +
//				str(@tmp_int / (@NumElapsedMs / 1000.0),12,1) +
//				space(2) +
//				str(@tmp_int / convert(real, @NumXacts),12,1) +
//				space(1) +
//				str(@tmp_int, 11) + space(5) +
//				str(100.0 * @tmp_int / @tmp_total,5,1) + 
//				@psign
//		print @rptline
//		print @blankline
//		
//		select @rptline="      Large I/Os Denied due to"
//		print @rptline
//
//		select @tmp_int = value
//		  from #tempmonitors
//		  where group_name = @tmp_grp and
//			field_name = "prefetch_decrease"
//
//		select @tmp_int3 = value
//		  from #tempmonitors
//		  where group_name = @tmp_grp and
//			field_name = "prefetch_kept_bp"
//
//		select @tmp_int4 = value
//		  from #tempmonitors
//		  where group_name = @tmp_grp and
//			field_name = "prefetch_cached_bp"
//
//		select @tmp_int_sum = @tmp_int3 + @tmp_int4
//		select @tmp_int = @tmp_int - @tmp_int_sum
//
//		select @rptline = "        Pool < Prefetch Size" + space(1) + 
//				str(@tmp_int / (@NumElapsedMs / 1000.0),12,1) +
//				space(2) +
//				str(@tmp_int / convert(real, @NumXacts),12,1) +
//				space(1) +
//				str(@tmp_int, 11) + space(5) +
//				str(100.0 * @tmp_int / @tmp_total,5,1) + 
//				@psign
//		print @rptline
//
//		select @rptline = "        Pages Requested"
//		print @rptline
//		select @rptline = "        Reside in Another"
//		print @rptline
//
//		select @rptline = "        Buffer Pool" + space(10) + 
//				str(@tmp_int_sum / 
//					(@NumElapsedMs / 	
//					1000.0),12,1) +
//					space(2) +
//					str(@tmp_int_sum / 
//					convert(real, @NumXacts),12,1) +
//					space(2) +
//					str(@tmp_int_sum, 10) + 
//						space(5) +
//					str(100.0 * 
//						@tmp_int_sum/ 
//						@tmp_total,5,1) + 
//						@psign
//		print @rptline
//		print @sum2line 
//		select @rptline = "    Total Large I/O Requests " +
//			str(@tmp_total / (@NumElapsedMs / 1000.0),12,1) +
//			space(2) +
//			str(@tmp_total / convert(real, @NumXacts),12,1) +
//			space(2) +
//			str(@tmp_total,10)
//		print @rptline
//	  end	/* else */
//
//	print @blankline
//
//	print "    Large I/O Detail"
//
//	/*
//	**  default to NO large pools found for this cache 
//	*/
//	select @lrgpool = 0	
//	/* 
//	** init loop counter to loop through all large I/O pool 
//	*/
//	select @j = @numKBperpg*2
//        while (@j <= 8*@numKBperpg)        /* { */
//	  begin
//
//          /* Check that the current cache has a pool configured of size @j */
//          select @gtlogpgszpool = count(*)
//          from #pool_detail_per_cache pd
//          where pd.io_size = convert(varchar(8), @j)
//	  and name = @CacheName	
// 
//          if (@gtlogpgszpool > 0)
//          begin                 /* { */
// 
//                /* Remember that we _did_ find a large I/O pool */
//                select @lrgpool = 1
//
//		/* 
//		** build pool specific counter name, 
//		** bufgrab_Nk (ie bufgrab_16k) 
//		*/
//		select @tmp_cntr = "bufgrab_" + convert(varchar(3), @j) + "k"
//
//		select @tmp_total = value
//		  from #tempmonitors
//		  where group_name = @tmp_grp and
//				field_name = @tmp_cntr
//
//                select @rptline = space(5) + convert(char(4),@j) +
//			"Kb Pool"
//		print @rptline
//
//                if @tmp_total = 0
//                begin
//                        select @rptline = "        Pages Cached" + space(18) +
//                                          ltrim(@zero_str)
//                        print @rptline
// 
//                        select @rptline = "        Pages Used" + space(20) +
//                                          ltrim(@zero_str)
//                        print @rptline
//                end
// 
//                else
//                  begin
//
//			/* turn # of masses into # of logical pages */
//			select @tmp_total = @tmp_total * (@j / @numKBperpg)	
//
//			select @rptline = "        Pages Cached" + space(9) +
//				str(@tmp_total / 
//					(@NumElapsedMs / 1000.0),12,1) + 
//				space(2) +
//				str(@tmp_total / 
//					convert(real, @NumXacts),12,1) + 
//				space(1) +
//				str(@tmp_total,11) + space(7) +
//				@na_str
//			print @rptline
//
//			select @tmp_cntr = "bufgrab_ref_" + 
//				convert(varchar(3), @j) + "K"
//
//			select @tmp_int = value
//			  from #tempmonitors
//			  where group_name = @tmp_grp and
//					field_name = @tmp_cntr
//
//			select @rptline = "        Pages Used" + space(11) +
//				str(@tmp_int / (@NumElapsedMs / 1000.0),12,1) +
//				space(2) +
//				str(@tmp_int / convert(real, @NumXacts),12,1) +
//				space(1) +
//				str(@tmp_int, 11) + space(5) +
//				str(100.0 * @tmp_int / @tmp_total,5,1) + @psign
//			print @rptline
//		  end
//
//                end     /* } if @gtlogpgszpool > 0 */
//
//		select @j = @j * 2	/* get next pool size */
//	  end /* } @j <= 8*@numKBperpg */
//
//	if @lrgpool = 0		/* No large pools in this cache */
//	  begin
//		print "      No Large Pool(s) In This Cache"
//	  end
//
//        print @blankline
//
//	print "    Dirty Read Behavior"
//	
//	select @tmp_total = value
//	  from #tempmonitors
//	  where group_name = @tmp_grp and
//			field_name = "level0_bufpredirty"
//
//	select @rptline = "	  Page Requests" + space(6) +
//			str(@tmp_total / (@NumElapsedMs / 1000.0),12,1) + 
//			space(2) +
//			str(@tmp_total / convert(real, @NumXacts),12,1) + 
//			space(1) +
//			str(@tmp_total,11) + space(7) +
//			@na_str
//	print @rptline
//	print @blankline
//
//	if @Reco = 'Y'
// 	begin /* { */
//		select @recotxt =   "    Tuning Recommendations for Data cache : "+@CacheName
//		select @recoline =  "    -------------------------------------"
//       	 	select @reco_hdr_prn = 0
//
//		/* recommendations for cache replacement policy */
//
//		select @tmp_float = convert(int, (100.0*a.value/b.value))
//		  from #tempmonitors a, #tempmonitors b
//		  where a.group_name = @tmp_grp and
//		  	b.group_name = @tmp_grp and
//			a.group_name = b.group_name and
//			a.field_name = "bufsearch_finds" and
//			b.field_name = "bufsearch_calls" and
//			b.value != 0
//		  if (@tmp_float is not null)
//		  begin /* { */
//			select @tmp_float2 = 0
//			select @tmp_float2 = (100.0*a.value)/b.value
//	  		 from #tempmonitors a, #tempmonitors b
//	  		 where a.group_name = @tmp_grp and
//		  		b.group_name = @tmp_grp and
//				a.group_name = b.group_name and
//				a.field_name = "bufwash_write_dirty" and
//				b.field_name = "bufwash_throughput" and
//				b.value != 0
//
//			if (@tmp_float2 is null)
//				select @tmp_float2 = 0
//
//			/* 
//			** If the Cache Hit Rate is greater than 95% and
//			** the replacement is less than 5% and if the
//			** existing replacement policy is "strict LRU"
//			** then consider using "relaxed lru replacement"
//			** policy for this cache.
//			*/
//			if ((@tmp_float >= 95.0 and @tmp_float2 <= 5.0) and
//				@NumEngines > 1)
//         		begin /* { */
//				if (@run_repl = "strict LRU")
//				 begin /* { */
//                			if (@reco_hdr_prn = 0)
//                			begin /* { */
//                       				print @recotxt
//						print @recoline
//                        			select @reco_hdr_prn = 1
//                			end /* } */
//                			print "    - Consider using 'relaxed LRU replacement policy'"
//                			print "      for this cache." 
//					print @blankline
//				 end /* } */
//         		end /* } */
//			else
//         		begin /* { */
//			/* 
//			** If the Cache Hit Rate is less than 95% and
//			** the replacement is greater than 5% and if the
//			** existing replacement policy is "relaxed LRU"
//			** then consider using "Strict lru replacement"
//			** policy for this cache.
//			*/
//				if (@run_repl = "relaxed LRU")
//			 	begin /* { */
//                			if (@reco_hdr_prn = 0)
//                			begin /* { */
//                       				print @recotxt
//						print @recoline
//                        			select @reco_hdr_prn = 1
//                			end /* } */
//                			print "    - Consider using 'strict LRU replacement policy'."
//                			print "      for this cache." 
//					print @blankline
//			 	end /* } */
//         		end /* } */
// 		end /* } */
//
//		/* recommendations for pool wash size */
//
//		select @tmp_int = SUM(convert(bigint, value))
//	  		from #tempmonitors
//	 		 where group_name = @tmp_grp  and
//			field_name like "bufgrab_dirty_%"
//
//		  if (@tmp_int is not null)
//		  begin /* { */
//			select @j = @numKBperpg
//                	while (@j <= 8*@numKBperpg)    
//		  	begin    /* { */
//
//				/* 
//				**  build pool specific counter name, 
//				**  bufgrab_Nk (ie bufgrab_16k) 
//				*/
//				select @tmp_cntr = "bufgrab_dirty_" + 
//						convert(varchar(3), @j) + "k"
//
//				select @tmp_int = value
//			  		from #tempmonitors
//			  		where group_name = @tmp_grp and
//					field_name = @tmp_cntr
//
//                        	if @tmp_int != 0  
//				begin /* { */
//                			if (@reco_hdr_prn = 0)
//                			begin /* { */
//                       				print @recotxt
//						print @recoline
//                        			select @reco_hdr_prn = 1
//                			end /* } */
//					/*
//					** If We grabbed a buffer that was
//					** dirty from this pool consider increasing
//					** the wash size for this buffer pool
//					*/
//                			select @rptline = "    - Consider increasing the 'wash size' of the "+ltrim(str(@j,3))+"k pool for this cache."
//					print @rptline
//					print @blankline
//
//				end /* } */
//				/* get next pool size (power of 2) */
//				select @j = @j * 2 
//			end /* } */	
//				
//		  end /* } */
//
//		/* recommendations for pool addition */
//		if (select value from #tempmonitors
//	 		 where group_name = @tmp_grp  and
//			field_name like "bufopt_lrgmass_reqd") > 0
//                begin /* { */
//                        if (@reco_hdr_prn = 0)
//                        begin /* { */
//                                print @recotxt
//				print @recoline
//                                select @reco_hdr_prn = 1
//                        end /* } */
//			/*
//			** If the optimizer wanted to do large I/O but could
//			** not find a buffer pool configured to be able
//			** to do this large I/O consider having a large I/O
//			** pool for this cache
//			*/
//                        print "    - Consider adding a large I/O pool for this cache."
//			print @blankline
//                end /* } */
//
//
//		/* recommendations for pool removal */
//		select @j = @numKBperpg*2
//		select @tmp_cntr = "bufgrab_" + 
//					convert(varchar(3), @j) + "k"
//		select @srchfound_cntr = "bufsearch_finds_" +
//					convert(varchar(3), @j) + "k"
//
//		/*
//		** The recommendation to remove a large buffer pool will be
//		** printed only when the bufgrabs and bufsearch_finds are 0
//		** for the buffer pool. This is to avoid
//		** printing this message when the data is entirely cached
//		** in the buffer pool and hence not having any grabs.
//		*/
//		if ((select value from #tempmonitors
//	 		 where group_name = @tmp_grp  and
//			field_name like @tmp_cntr) = 0
//		   and (select value from #tempmonitors
//			where group_name = @tmp_grp  and
//			field_name like @srchfound_cntr) = 0
//         	   and exists (select * from #pool_detail_per_cache 
//          		where io_size = convert(varchar(8), @j)
//          		and name = @CacheName))
//		begin /* { */
//			if (@reco_hdr_prn = 0)
//			begin /* { */
//				print @recotxt
//				print @recoline
//				select @reco_hdr_prn = 1
//			end /* } */
//			/*
//			** If there are no grabs for this buffer pool
//			** consider removing this buffer pool.
//			*/
//                	select @rptline = "    - Consider removing the "+ltrim(str(@j,3))+"k pool for this cache."
//			print @rptline
//			print @blankline
//		end /* } */
//
//		select @j = @j*2
//		select @tmp_cntr = "bufgrab_" + convert(varchar(3), @j) + "k"
//		select @srchfound_cntr = "bufsearch_finds_" +
//					convert(varchar(3), @j) + "k"
//
//		if ((select value from #tempmonitors
//	 		 where group_name = @tmp_grp  and
//			field_name like @tmp_cntr) = 0
//		   and (select value from #tempmonitors
//			where group_name = @tmp_grp  and
//			field_name like @srchfound_cntr) = 0
//         	   and exists (select * from #pool_detail_per_cache 
//          		where io_size = convert(varchar(8), @j)
//          		and name = @CacheName))
//		begin /* { */
//			if (@reco_hdr_prn = 0)
//			begin /* { */
//				print @recotxt
//				print @recoline
//				select @reco_hdr_prn = 1
//			end /* } */
//			/*
//			** If there are no grabs for this buffer pool
//			** consider removing this buffer pool.
//			*/
//                	select @rptline = "    - Consider removing the "+ltrim(str(@j,3))+"k pool for this cache."
//			print @rptline
//			print @blankline
//		end /* } */
//
//		select @j = @j*2
//		select @tmp_cntr = "bufgrab_" + convert(varchar(3), @j) + "k"
//		select @srchfound_cntr = "bufsearch_finds_" +
//					convert(varchar(3), @j) + "k"
//
//		if ((select value from #tempmonitors
//	 		 where group_name = @tmp_grp  and
//			field_name like @tmp_cntr) = 0
//		   and (select value from #tempmonitors
//			where group_name = @tmp_grp  and
//			field_name like @srchfound_cntr) = 0
//         	   and exists (select * from #pool_detail_per_cache 
//          		where io_size = convert(varchar(8), @j)
//          		and name = @CacheName))
//		begin /* { */
//			if (@reco_hdr_prn = 0)
//			begin /* { */
//				print @recotxt
//				print @recoline
//				select @reco_hdr_prn = 1
//			end /* } */
//			/*
//			** If there are no grabs for this buffer pool
//			** consider removing this buffer pool.
//			*/
//                	select @rptline = "    - Consider removing the "+ltrim(str(@j,3))+"k pool for this cache."
//			print @rptline
//			print @blankline
//		end /* } */
//
//		/* recommendations for cache splitting  */
//	
//		/*
//		** If the number of engines is > 1 
//		** and if the contention on the buffer
//		** manager spinlock is > 10%
//		** consider using cache partitions or named caches
//		** or both
//		** Also there are potential conditions where the waits or
//		** grabs might go negative because of the counter overflowing
//		** what an integer can hold; Cover for those cases as well.
//		*/	
//		if (@NumEngines > 1 and (@spinlock_contention >= 10  or
//					@spinlock_contention < 0))
//		 begin /* { */
//                	if (@reco_hdr_prn = 0)
//                	begin /* { */
//                    		print @recotxt
//				print @recoline
//                     		select @reco_hdr_prn = 1
//               		end /* } */
//            		print "    - Consider using Named Caches or Cache partitions or both."
//			print @blankline
//		 end /* } */
//	end /* } */
//
//	fetch cache_info into @CacheID, @CacheName, @tmp_grp, @c_type, @cfg_repl, @run_repl
//  end  /* } while @@sqlstatus */
//
//close cache_info
//deallocate cursor cache_info
//
//return 0
//go
