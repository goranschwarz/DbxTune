package com.asetune.cm;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.asetune.gui.SummaryPanel;
import com.asetune.utils.Configuration;
import com.asetune.utils.PropPropEntry;


public class CounterSetTemplates
{
	private static LinkedHashMap<String, String>_nameShortToLongMap; 
	private static LinkedHashMap<String, String>_nameLongToShortMap; 
	static
	{
		_nameShortToLongMap = new LinkedHashMap<String, String>();
		_nameLongToShortMap = new LinkedHashMap<String, String>();

		// SHORT -> LONG
		_nameShortToLongMap.put(SummaryPanel.CM_NAME      , "Summary"                 );
		_nameShortToLongMap.put("CMobjActivity"           , "Objects"                 );
		_nameShortToLongMap.put("CMprocActivity"          , "Processes"               );
		_nameShortToLongMap.put("CMdbActivity"            , "Databases"               );
		_nameShortToLongMap.put("CMTmpdbActivity"         , "Temp Db"                 );
		_nameShortToLongMap.put("CMsysWaitActivity"       , "Waits"                   );
		_nameShortToLongMap.put("CMengineActivity"        , "Engines"                 );
		_nameShortToLongMap.put("CMSysLoad"               , "System Load"             );
		_nameShortToLongMap.put("CMcacheActivity"         , "Data Caches"             );
		_nameShortToLongMap.put("CMpoolActivity"          , "Pools"                   );
		_nameShortToLongMap.put("CMdeviceActivity"        , "Devices"                 );
		_nameShortToLongMap.put("CMioQueueSumAct"         , "IO Sum"                  );
		_nameShortToLongMap.put("CMioQueueActivity"       , "IO Queue"                );
		_nameShortToLongMap.put("CMspinlockSum"           , "Spinlock Sum"            );
		_nameShortToLongMap.put("CMsysmon"                , "Sysmon Raw"              );
		_nameShortToLongMap.put("CMrepAgent"              , "RepAgent"                );
		_nameShortToLongMap.put("CMcachedProcs"           , "Cached Procedures"       );
		_nameShortToLongMap.put("CMprocCache"             , "Procedure Cache"         );
		_nameShortToLongMap.put("CMprocCallStack"         , "Procedure Call Stack"    );
		_nameShortToLongMap.put("CMcachedObjects"         , "Cached Objects"          );
		_nameShortToLongMap.put("CMerrolog"               , "Errorlog"                );
		_nameShortToLongMap.put("CMdeadlock"              , "Deadlock"                );
		_nameShortToLongMap.put("CMpCacheModuleUsage"     , "Proc Cache Module Usage" );
		_nameShortToLongMap.put("CMpCacheMemoryUsage"     , "Proc Cache Memory Usage" );
		_nameShortToLongMap.put("CMstatementCache"        , "Statement Cache"         );
		_nameShortToLongMap.put("CMstmntCacheDetails"     , "Statement Cache Details" );
		_nameShortToLongMap.put("CMActiveObjects"         , "Active Objects"          );
		_nameShortToLongMap.put("CMActiveStatements"          , "Active Statements"       );
		_nameShortToLongMap.put("CMblocking"              , "Blocking"                );
		_nameShortToLongMap.put("CMmissingStats"          , "Missing Statistics"      );
		_nameShortToLongMap.put("CMspMonitorConfig"       , "sp_monitorconfig"        );
		_nameShortToLongMap.put("CMosIostat"              , "OS Disk Stat"            );
		_nameShortToLongMap.put("CMosVmstat"              , "OS CPU(vmstat)"          );
		_nameShortToLongMap.put("CMosMpstat"              , "OS CPU(mpstat)"          );
		
		// LONG -> SHORT
		_nameLongToShortMap.put("Summary"                 , SummaryPanel.CM_NAME      );
		_nameLongToShortMap.put("Objects"                 , "CMobjActivity"           );
		_nameLongToShortMap.put("Processes"               , "CMprocActivity"          );
		_nameLongToShortMap.put("Databases"               , "CMdbActivity"            );
		_nameLongToShortMap.put("Temp Db"                 , "CMTmpdbActivity"         );
		_nameLongToShortMap.put("Waits"                   , "CMsysWaitActivity"       );
		_nameLongToShortMap.put("Engines"                 , "CMengineActivity"        );
		_nameLongToShortMap.put("System Load"             , "CMSysLoad"               );
		_nameLongToShortMap.put("Data Caches"             , "CMcacheActivity"         );
		_nameLongToShortMap.put("Pools"                   , "CMpoolActivity"          );
		_nameLongToShortMap.put("Devices"                 , "CMdeviceActivity"        );
		_nameLongToShortMap.put("IO Sum"                  , "CMioQueueSumAct"         );
		_nameLongToShortMap.put("IO Queue"                , "CMioQueueActivity"       );
		_nameLongToShortMap.put("Spinlock Sum"            , "CMspinlockSum"           );
		_nameLongToShortMap.put("Sysmon Raw"              , "CMsysmon"                );
		_nameLongToShortMap.put("RepAgent"                , "CMrepAgent"              );
		_nameLongToShortMap.put("Cached Procedures"       , "CMcachedProcs"           );
		_nameLongToShortMap.put("Procedure Cache"         , "CMprocCache"             );
		_nameLongToShortMap.put("Procedure Call Stack"    , "CMprocCallStack"         );
		_nameLongToShortMap.put("Cached Objects"          , "CMcachedObjects"         );
		_nameLongToShortMap.put("Errorlog"                , "CMerrolog"               );
		_nameLongToShortMap.put("Deadlock"                , "CMdeadlock"              );
		_nameLongToShortMap.put("Proc Cache Module Usage" , "CMpCacheModuleUsage"     );
		_nameLongToShortMap.put("Proc Cache Memory Usage" , "CMpCacheMemoryUsage"     );
		_nameLongToShortMap.put("Statement Cache"         , "CMstatementCache"        );
		_nameLongToShortMap.put("Statement Cache Details" , "CMstmntCacheDetails"     );
		_nameLongToShortMap.put("Active Objects"          , "CMActiveObjects"         );
		_nameLongToShortMap.put("Active Statements"       , "CMActiveStatements"      );
		_nameLongToShortMap.put("Blocking"                , "CMblocking"              );
		_nameLongToShortMap.put("Missing Statistics"      , "CMmissingStats"          );
		_nameLongToShortMap.put("sp_monitorconfig"        , "CMspMonitorConfig"       );
		_nameLongToShortMap.put("OS Disk Stat"            , "CMosIostat"              );
		_nameLongToShortMap.put("OS CPU(vmstat)"          , "CMosVmstat"              );
		_nameLongToShortMap.put("OS CPU(mpstat)"          , "CMosMpstat"              );

		
		Configuration conf = Configuration.getCombinedConfiguration();

		// SQL: USER DEFINED COUNTERS
		String prefix = "udc.";
		for (String name : conf.getUniqueSubKeys(prefix, false))
		{
			String startKey = prefix + name + ".";

			String  udcName          = conf.getProperty(startKey    + "name");
			String  udcDisplayName   = conf.getProperty(startKey    + "displayName", udcName);

			_nameShortToLongMap.put(udcName,        udcDisplayName);
			_nameLongToShortMap.put(udcDisplayName, udcName);
		}


		// HOST MONITOR: USER DEFINED COUNTERS
		prefix = "hostmon.udc.";
		for (String name : conf.getUniqueSubKeys(prefix, false))
		{
			String startKey = prefix + name + ".";

			String  udcDisplayName   = conf.getProperty(startKey    + "displayName", name);
			
			_nameShortToLongMap.put(name,           udcDisplayName);
			_nameLongToShortMap.put(udcDisplayName, name);
		}
	}

	/**
	 * get the "short" name of a Performance Counter based on it's "long" name 
	 * @param longName The long name of the Performance Counter
	 * @return The "short" name of the Performance Counter, null if it can't be found.
	 */
	public static String getShortName(String longName)
	{
		return _nameLongToShortMap.get(longName);
	}

	/**
	 * get the "long" name of a Performance Counter based on it's "short" name 
	 * @param shortName The short name of the Performance Counter
	 * @return The "long" name of the Performance Counter, null if it can't be found.
	 */
	public static String getLongName(String shortName)
	{
		return _nameShortToLongMap.get(shortName);
	}

	/**
	 * Get all "short" names
	 * @return a Set of all "short" names
	 */
	public static Set<String> getShortNames()
	{
		return _nameShortToLongMap.keySet();
	}

	/**
	 * Get all "long" names
	 * @return a Set of all "long" names
	 */
	public static Set<String> getLongNames()
	{
		return _nameLongToShortMap.keySet();
	}

	/**
	 * @return a Map of "short" to "long" values of the Performance Counters
	 */
	public static Map<String, String> getShortToLongMap()
	{
		return _nameShortToLongMap;
	}

	/**
	 * @return a Map of "long" to "short" values of the Performance Counters
	 */
	public static Map<String, String> getLongToShortMap()
	{
		return _nameLongToShortMap;
	}

	/**
	 * 
	 * @param templateName
	 * @return
	 * @throws Exception
	 */
	public static List<String> getTemplateList(String templateName)
	throws Exception
	{
		List<String> activeCmList = new ArrayList<String>();

		activeCmList.add(SummaryPanel.CM_NAME);

		PropPropEntry ppe = null;
		if      (templateName.equalsIgnoreCase("small"))  ppe = CounterSetTemplates.SYSTEM_TEMPLATE_PCS_ON_SMALL;
		else if (templateName.equalsIgnoreCase("medium")) ppe = CounterSetTemplates.SYSTEM_TEMPLATE_PCS_ON_MEDIUM;
		else if (templateName.equalsIgnoreCase("large"))  ppe = CounterSetTemplates.SYSTEM_TEMPLATE_PCS_ON_LARGE;
		else if (templateName.equalsIgnoreCase("all"))
		{
			ppe = new PropPropEntry();
			for (String longName : getLongNames())
			{
				String postpone = SYSTEM_TEMPLATE_PCS_ON_ALL.getProperty(longName, "postpone", "0");
				ppe.put(longName, "storePcs", "true");
				ppe.put(longName, "postpone", postpone);
			}
		}
		else
		{
			throw new Exception("Unknown template name '"+templateName+"'.");
		}

		// Some sort of TEMPLATE was found
		if (ppe != null)
		{
			for (String name : ppe.keySet())
			{
				boolean storePcs = ppe.getBooleanProperty(name, "storePcs", false);
				int     postpone = ppe.getIntProperty    (name, "postpone", 0);
				if (storePcs)
				{
					if (postpone <= 0)
						activeCmList.add(name);
					else
						activeCmList.add(name+":"+postpone);
				}
			}
		}

		return activeCmList;
	}
	
	public final static String systemSummaryCm = 
		"Summary                 ={postpone=0,   paused=false, bg=true,  resetNC20=true,  storePcs=true,  pcsAbs=true, pcsDiff=true, pcsRate=true}; ";

	public final static String systemTemplatePcsOnSmall = 
		"Objects                 ={postpone=0,   paused=false, bg=false, resetNC20=true,  storePcs=false, pcsAbs=true, pcsDiff=true, pcsRate=true}; " +
		"Processes               ={postpone=0,   paused=false, bg=false, resetNC20=true,  storePcs=false, pcsAbs=true, pcsDiff=true, pcsRate=true}; " +
		"Databases               ={postpone=0,   paused=false, bg=false, resetNC20=true,  storePcs=false, pcsAbs=true, pcsDiff=true, pcsRate=true}; " +
		"Temp Db                 ={postpone=0,   paused=false, bg=false, resetNC20=true,  storePcs=false, pcsAbs=true, pcsDiff=true, pcsRate=true}; " +
		"Waits                   ={postpone=0,   paused=false, bg=false, resetNC20=true,  storePcs=false, pcsAbs=true, pcsDiff=true, pcsRate=true}; " +
		"Engines                 ={postpone=0,   paused=false, bg=true,  resetNC20=true,  storePcs=true,  pcsAbs=true, pcsDiff=true, pcsRate=true}; " +
		"System Load             ={postpone=0,   paused=false, bg=false, resetNC20=false, storePcs=false, pcsAbs=true, pcsDiff=true, pcsRate=true}; " +
		"Data Caches             ={postpone=0,   paused=false, bg=true,  resetNC20=true,  storePcs=true,  pcsAbs=true, pcsDiff=true, pcsRate=true}; " +
		"Pools                   ={postpone=0,   paused=false, bg=false, resetNC20=true,  storePcs=false, pcsAbs=true, pcsDiff=true, pcsRate=true}; " +
		"Devices                 ={postpone=0,   paused=false, bg=false, resetNC20=true,  storePcs=false, pcsAbs=true, pcsDiff=true, pcsRate=true}; " +
		"IO Sum                  ={postpone=0,   paused=false, bg=true,  resetNC20=true,  storePcs=true,  pcsAbs=true, pcsDiff=true, pcsRate=true}; " +
		"IO Queue                ={postpone=0,   paused=false, bg=true,  resetNC20=true,  storePcs=true,  pcsAbs=true, pcsDiff=true, pcsRate=true}; " +
		"Spinlock Sum            ={postpone=300, paused=false, bg=false, resetNC20=true,  storePcs=false, pcsAbs=true, pcsDiff=true, pcsRate=true}; " +
		"Sysmon Raw              ={postpone=300, paused=false, bg=false, resetNC20=true,  storePcs=false, pcsAbs=true, pcsDiff=true, pcsRate=true}; " +
		"RepAgent                ={postpone=300, paused=false, bg=false, resetNC20=true,  storePcs=false, pcsAbs=true, pcsDiff=true, pcsRate=true}; " +
		"Cached Procedures       ={postpone=0,   paused=false, bg=false, resetNC20=true,  storePcs=false, pcsAbs=true, pcsDiff=true, pcsRate=true}; " +
		"Procedure Cache         ={postpone=0,   paused=false, bg=true,  resetNC20=true,  storePcs=true,  pcsAbs=true, pcsDiff=true, pcsRate=true}; " +
		"Procedure Call Stack    ={postpone=0,   paused=false, bg=false, resetNC20=true,  storePcs=false, pcsAbs=true, pcsDiff=true, pcsRate=true}; " +
		"Cached Objects          ={postpone=600, paused=false, bg=false, resetNC20=false, storePcs=false, pcsAbs=true, pcsDiff=true, pcsRate=true}; " +
		"Errorlog                ={postpone=0,   paused=false, bg=false, resetNC20=true,  storePcs=false, pcsAbs=true, pcsDiff=false,pcsRate=false};" +
		"Deadlock                ={postpone=0,   paused=false, bg=false, resetNC20=true,  storePcs=false, pcsAbs=true, pcsDiff=false,pcsRate=false};" +
		"Proc Cache Module Usage ={postpone=0,   paused=false, bg=false, resetNC20=false, storePcs=false, pcsAbs=true, pcsDiff=true, pcsRate=true}; " +
		"Proc Cache Memory Usage ={postpone=0,   paused=false, bg=false, resetNC20=false, storePcs=false, pcsAbs=true, pcsDiff=true, pcsRate=true}; " +
		"Statement Cache         ={postpone=0,   paused=false, bg=false, resetNC20=false, storePcs=false, pcsAbs=true, pcsDiff=true, pcsRate=true}; " +
		"Statement Cache Details ={postpone=0,   paused=false, bg=false, resetNC20=false, storePcs=false, pcsAbs=true, pcsDiff=true, pcsRate=true}; " +
		"Active Objects          ={postpone=0,   paused=false, bg=false, resetNC20=false, storePcs=false, pcsAbs=true, pcsDiff=true, pcsRate=true}; " +
		"Active Statements       ={postpone=0,   paused=false, bg=false, resetNC20=true,  storePcs=false, pcsAbs=true, pcsDiff=true, pcsRate=true}; " +
		"Blocking                ={postpone=0,   paused=false, bg=false, resetNC20=false, storePcs=false, pcsAbs=true, pcsDiff=true, pcsRate=true}; " +
		"Missing Statistics      ={postpone=0,   paused=false, bg=false, resetNC20=false, storePcs=false, pcsAbs=true, pcsDiff=true, pcsRate=true}; " +
		"sp_monitorconfig        ={postpone=3600,paused=false, bg=false, resetNC20=false, storePcs=false, pcsAbs=true, pcsDiff=true, pcsRate=true}; " +
		"OS Disk Stat            ={postpone=0,   paused=false, bg=false, resetNC20=false, storePcs=false, pcsAbs=true, pcsDiff=false,pcsRate=false};" +
		"OS CPU(vmstat)          ={postpone=0,   paused=false, bg=false, resetNC20=false, storePcs=false, pcsAbs=true, pcsDiff=false,pcsRate=false};" +
		"OS CPU(mpstat)          ={postpone=0,   paused=false, bg=false, resetNC20=false, storePcs=false, pcsAbs=true, pcsDiff=false,pcsRate=false};";

	public final static String systemTemplatePcsOnMedium = 
		"Objects                 ={postpone=0,   paused=false, bg=true,  resetNC20=true,  storePcs=true,  pcsAbs=true, pcsDiff=true, pcsRate=true}; " +
		"Processes               ={postpone=0,   paused=false, bg=true,  resetNC20=true,  storePcs=true,  pcsAbs=true, pcsDiff=true, pcsRate=true}; " +
		"Databases               ={postpone=0,   paused=false, bg=true,  resetNC20=true,  storePcs=true,  pcsAbs=true, pcsDiff=true, pcsRate=true}; " +
		"Temp Db                 ={postpone=0,   paused=false, bg=true,  resetNC20=true,  storePcs=true,  pcsAbs=true, pcsDiff=true, pcsRate=true}; " +
		"Waits                   ={postpone=0,   paused=false, bg=true,  resetNC20=true,  storePcs=true,  pcsAbs=true, pcsDiff=true, pcsRate=true}; " +
		"Engines                 ={postpone=0,   paused=false, bg=true,  resetNC20=true,  storePcs=true,  pcsAbs=true, pcsDiff=true, pcsRate=true}; " +
		"System Load             ={postpone=0,   paused=false, bg=true,  resetNC20=false, storePcs=true,  pcsAbs=true, pcsDiff=true, pcsRate=true}; " +
		"Data Caches             ={postpone=0,   paused=false, bg=true,  resetNC20=true,  storePcs=true,  pcsAbs=true, pcsDiff=true, pcsRate=true}; " +
		"Pools                   ={postpone=0,   paused=false, bg=true,  resetNC20=true,  storePcs=true,  pcsAbs=true, pcsDiff=true, pcsRate=true}; " +
		"Devices                 ={postpone=0,   paused=false, bg=true,  resetNC20=true,  storePcs=true,  pcsAbs=true, pcsDiff=true, pcsRate=true}; " +
		"IO Sum                  ={postpone=0,   paused=false, bg=true,  resetNC20=true,  storePcs=true,  pcsAbs=true, pcsDiff=true, pcsRate=true}; " +
		"IO Queue                ={postpone=0,   paused=false, bg=true,  resetNC20=true,  storePcs=true,  pcsAbs=true, pcsDiff=true, pcsRate=true}; " +
		"Spinlock Sum            ={postpone=300, paused=false, bg=false, resetNC20=true,  storePcs=false, pcsAbs=true, pcsDiff=true, pcsRate=true}; " +
		"Sysmon Raw              ={postpone=300, paused=false, bg=false, resetNC20=true,  storePcs=false, pcsAbs=true, pcsDiff=true, pcsRate=true}; " +
		"RepAgent                ={postpone=300, paused=false, bg=false, resetNC20=true,  storePcs=false, pcsAbs=true, pcsDiff=true, pcsRate=true}; " +
		"Cached Procedures       ={postpone=0,   paused=false, bg=false, resetNC20=true,  storePcs=false, pcsAbs=true, pcsDiff=true, pcsRate=true}; " +
		"Procedure Cache         ={postpone=0,   paused=false, bg=true,  resetNC20=true,  storePcs=true,  pcsAbs=true, pcsDiff=true, pcsRate=true}; " +
		"Procedure Call Stack    ={postpone=0,   paused=false, bg=true,  resetNC20=true,  storePcs=true,  pcsAbs=true, pcsDiff=true, pcsRate=true}; " +
		"Cached Objects          ={postpone=600, paused=false, bg=false, resetNC20=false, storePcs=false, pcsAbs=true, pcsDiff=true, pcsRate=true}; " +
		"Errorlog                ={postpone=0,   paused=false, bg=false, resetNC20=true,  storePcs=false, pcsAbs=true, pcsDiff=false,pcsRate=false};" +
		"Deadlock                ={postpone=0,   paused=false, bg=false, resetNC20=true,  storePcs=false, pcsAbs=true, pcsDiff=false,pcsRate=false};" +
		"Proc Cache Module Usage ={postpone=0,   paused=false, bg=false, resetNC20=false, storePcs=false, pcsAbs=true, pcsDiff=true, pcsRate=true}; " +
		"Proc Cache Memory Usage ={postpone=0,   paused=false, bg=false, resetNC20=false, storePcs=false, pcsAbs=true, pcsDiff=true, pcsRate=true}; " +
		"Statement Cache         ={postpone=0,   paused=false, bg=false, resetNC20=false, storePcs=false, pcsAbs=true, pcsDiff=true, pcsRate=true}; " +
		"Statement Cache Details ={postpone=0,   paused=false, bg=false, resetNC20=false, storePcs=false, pcsAbs=true, pcsDiff=true, pcsRate=true}; " +
		"Active Objects          ={postpone=0,   paused=false, bg=false, resetNC20=false, storePcs=false, pcsAbs=true, pcsDiff=true, pcsRate=true}; " +
		"Active Statements       ={postpone=0,   paused=false, bg=true,  resetNC20=true,  storePcs=true,  pcsAbs=true, pcsDiff=true, pcsRate=true}; " +
		"Blocking                ={postpone=0,   paused=false, bg=false, resetNC20=false, storePcs=false, pcsAbs=true, pcsDiff=true, pcsRate=true}; " +
		"Missing Statistics      ={postpone=0,   paused=false, bg=false, resetNC20=false, storePcs=false, pcsAbs=true, pcsDiff=true, pcsRate=true}; " +
		"sp_monitorconfig        ={postpone=3600,paused=false, bg=true,  resetNC20=false, storePcs=true,  pcsAbs=true, pcsDiff=true, pcsRate=true}; " +
		"OS Disk Stat            ={postpone=0,   paused=false, bg=false, resetNC20=false, storePcs=false, pcsAbs=true, pcsDiff=false,pcsRate=false};" +
		"OS CPU(vmstat)          ={postpone=0,   paused=false, bg=false, resetNC20=false, storePcs=false, pcsAbs=true, pcsDiff=false,pcsRate=false};" +
		"OS CPU(mpstat)          ={postpone=0,   paused=false, bg=false, resetNC20=false, storePcs=false, pcsAbs=true, pcsDiff=false,pcsRate=false};";

	public final static String systemTemplatePcsOnLarge = 
		"Objects                 ={postpone=0,   paused=false, bg=true,  resetNC20=true,  storePcs=true,  pcsAbs=true, pcsDiff=true, pcsRate=true}; " +
		"Processes               ={postpone=0,   paused=false, bg=true,  resetNC20=true,  storePcs=true,  pcsAbs=true, pcsDiff=true, pcsRate=true}; " +
		"Databases               ={postpone=0,   paused=false, bg=true,  resetNC20=true,  storePcs=true,  pcsAbs=true, pcsDiff=true, pcsRate=true}; " +
		"Temp Db                 ={postpone=0,   paused=false, bg=true,  resetNC20=true,  storePcs=true,  pcsAbs=true, pcsDiff=true, pcsRate=true}; " +
		"Waits                   ={postpone=0,   paused=false, bg=true,  resetNC20=true,  storePcs=true,  pcsAbs=true, pcsDiff=true, pcsRate=true}; " +
		"Engines                 ={postpone=0,   paused=false, bg=true,  resetNC20=true,  storePcs=true,  pcsAbs=true, pcsDiff=true, pcsRate=true}; " +
		"System Load             ={postpone=0,   paused=false, bg=true,  resetNC20=false, storePcs=true,  pcsAbs=true, pcsDiff=true, pcsRate=true}; " +
		"Data Caches             ={postpone=0,   paused=false, bg=true,  resetNC20=true,  storePcs=true,  pcsAbs=true, pcsDiff=true, pcsRate=true}; " +
		"Pools                   ={postpone=0,   paused=false, bg=true,  resetNC20=true,  storePcs=true,  pcsAbs=true, pcsDiff=true, pcsRate=true}; " +
		"Devices                 ={postpone=0,   paused=false, bg=true,  resetNC20=true,  storePcs=true,  pcsAbs=true, pcsDiff=true, pcsRate=true}; " +
		"IO Sum                  ={postpone=0,   paused=false, bg=true,  resetNC20=true,  storePcs=true,  pcsAbs=true, pcsDiff=true, pcsRate=true}; " +
		"IO Queue                ={postpone=0,   paused=false, bg=true,  resetNC20=true,  storePcs=true,  pcsAbs=true, pcsDiff=true, pcsRate=true}; " +
		"Spinlock Sum            ={postpone=300, paused=false, bg=false, resetNC20=true,  storePcs=false, pcsAbs=true, pcsDiff=true, pcsRate=true}; " +
		"Sysmon Raw              ={postpone=300, paused=false, bg=false, resetNC20=true,  storePcs=false, pcsAbs=true, pcsDiff=true, pcsRate=true}; " +
		"RepAgent                ={postpone=300, paused=false, bg=false, resetNC20=true,  storePcs=false, pcsAbs=true, pcsDiff=true, pcsRate=true}; " +
		"Cached Procedures       ={postpone=0,   paused=false, bg=false, resetNC20=true,  storePcs=false, pcsAbs=true, pcsDiff=true, pcsRate=true}; " +
		"Procedure Cache         ={postpone=0,   paused=false, bg=true,  resetNC20=true,  storePcs=true,  pcsAbs=true, pcsDiff=true, pcsRate=true}; " +
		"Procedure Call Stack    ={postpone=0,   paused=false, bg=true,  resetNC20=true,  storePcs=true,  pcsAbs=true, pcsDiff=true, pcsRate=true}; " +
		"Cached Objects          ={postpone=600, paused=false, bg=false, resetNC20=false, storePcs=false, pcsAbs=true, pcsDiff=true, pcsRate=true}; " +
		"Errorlog                ={postpone=0,   paused=false, bg=false, resetNC20=true,  storePcs=false, pcsAbs=true, pcsDiff=false,pcsRate=false};" +
		"Deadlock                ={postpone=0,   paused=false, bg=false, resetNC20=true,  storePcs=false, pcsAbs=true, pcsDiff=false,pcsRate=false};" +
		"Proc Cache Module Usage ={postpone=0,   paused=false, bg=true,  resetNC20=false, storePcs=true,  pcsAbs=true, pcsDiff=true, pcsRate=true}; " +
		"Proc Cache Memory Usage ={postpone=0,   paused=false, bg=true,  resetNC20=false, storePcs=true,  pcsAbs=true, pcsDiff=true, pcsRate=true}; " +
		"Statement Cache         ={postpone=0,   paused=false, bg=true,  resetNC20=false, storePcs=true,  pcsAbs=true, pcsDiff=true, pcsRate=true}; " +
		"Statement Cache Details ={postpone=0,   paused=false, bg=true,  resetNC20=false, storePcs=true,  pcsAbs=true, pcsDiff=true, pcsRate=true}; " +
		"Active Objects          ={postpone=0,   paused=false, bg=true,  resetNC20=false, storePcs=true,  pcsAbs=true, pcsDiff=true, pcsRate=true}; " +
		"Active Statements       ={postpone=0,   paused=false, bg=true,  resetNC20=true,  storePcs=true,  pcsAbs=true, pcsDiff=true, pcsRate=true}; " +
		"Blocking                ={postpone=0,   paused=false, bg=true,  resetNC20=false, storePcs=true,  pcsAbs=true, pcsDiff=true, pcsRate=true}; " +
		"Missing Statistics      ={postpone=0,   paused=false, bg=true,  resetNC20=false, storePcs=true,  pcsAbs=true, pcsDiff=true, pcsRate=true}; " +
		"sp_monitorconfig        ={postpone=3600,paused=false, bg=true,  resetNC20=false, storePcs=true,  pcsAbs=true, pcsDiff=true, pcsRate=true}; " +
		"OS Disk Stat            ={postpone=0,   paused=false, bg=true,  resetNC20=false, storePcs=true,  pcsAbs=true, pcsDiff=false,pcsRate=false};" +
		"OS CPU(vmstat)          ={postpone=0,   paused=false, bg=true,  resetNC20=false, storePcs=true,  pcsAbs=true, pcsDiff=false,pcsRate=false};" +
		"OS CPU(mpstat)          ={postpone=0,   paused=false, bg=true,  resetNC20=false, storePcs=true,  pcsAbs=true, pcsDiff=false,pcsRate=false};";

	public final static String systemTemplatePcsOnAll = 
		"Objects                 ={postpone=0,   paused=false, bg=true,  resetNC20=true,  storePcs=true,  pcsAbs=true, pcsDiff=true, pcsRate=true}; " +
		"Processes               ={postpone=0,   paused=false, bg=true,  resetNC20=true,  storePcs=true,  pcsAbs=true, pcsDiff=true, pcsRate=true}; " +
		"Databases               ={postpone=0,   paused=false, bg=true,  resetNC20=true,  storePcs=true,  pcsAbs=true, pcsDiff=true, pcsRate=true}; " +
		"Temp Db                 ={postpone=0,   paused=false, bg=true,  resetNC20=true,  storePcs=true,  pcsAbs=true, pcsDiff=true, pcsRate=true}; " +
		"Waits                   ={postpone=0,   paused=false, bg=true,  resetNC20=true,  storePcs=true,  pcsAbs=true, pcsDiff=true, pcsRate=true}; " +
		"Engines                 ={postpone=0,   paused=false, bg=true,  resetNC20=true,  storePcs=true,  pcsAbs=true, pcsDiff=true, pcsRate=true}; " +
		"System Load             ={postpone=0,   paused=false, bg=true,  resetNC20=false, storePcs=true,  pcsAbs=true, pcsDiff=true, pcsRate=true}; " +
		"Data Caches             ={postpone=0,   paused=false, bg=true,  resetNC20=true,  storePcs=true,  pcsAbs=true, pcsDiff=true, pcsRate=true}; " +
		"Pools                   ={postpone=0,   paused=false, bg=true,  resetNC20=true,  storePcs=true,  pcsAbs=true, pcsDiff=true, pcsRate=true}; " +
		"Devices                 ={postpone=0,   paused=false, bg=true,  resetNC20=true,  storePcs=true,  pcsAbs=true, pcsDiff=true, pcsRate=true}; " +
		"IO Sum                  ={postpone=0,   paused=false, bg=true,  resetNC20=true,  storePcs=true,  pcsAbs=true, pcsDiff=true, pcsRate=true}; " +
		"IO Queue                ={postpone=0,   paused=false, bg=true,  resetNC20=true,  storePcs=true,  pcsAbs=true, pcsDiff=true, pcsRate=true}; " +
		"Spinlock Sum            ={postpone=300, paused=false, bg=true,  resetNC20=true,  storePcs=true,  pcsAbs=true, pcsDiff=true, pcsRate=true}; " +
		"Sysmon Raw              ={postpone=300, paused=false, bg=true,  resetNC20=true,  storePcs=true,  pcsAbs=true, pcsDiff=true, pcsRate=true}; " +
		"RepAgent                ={postpone=300, paused=false, bg=true,  resetNC20=true,  storePcs=true,  pcsAbs=true, pcsDiff=true, pcsRate=true}; " +
		"Cached Procedures       ={postpone=0,   paused=false, bg=true,  resetNC20=true,  storePcs=true,  pcsAbs=true, pcsDiff=true, pcsRate=true}; " +
		"Procedure Cache         ={postpone=0,   paused=false, bg=true,  resetNC20=true,  storePcs=true,  pcsAbs=true, pcsDiff=true, pcsRate=true}; " +
		"Procedure Call Stack    ={postpone=0,   paused=false, bg=true,  resetNC20=true,  storePcs=true,  pcsAbs=true, pcsDiff=true, pcsRate=true}; " +
		"Cached Objects          ={postpone=600, paused=false, bg=true,  resetNC20=false, storePcs=true,  pcsAbs=true, pcsDiff=true, pcsRate=true}; " +
		"Errorlog                ={postpone=0,   paused=false, bg=false, resetNC20=true,  storePcs=false, pcsAbs=true, pcsDiff=false,pcsRate=false};" +
		"Deadlock                ={postpone=0,   paused=false, bg=false, resetNC20=true,  storePcs=false, pcsAbs=true, pcsDiff=false,pcsRate=false};" +
		"Proc Cache Module Usage ={postpone=0,   paused=false, bg=true,  resetNC20=false, storePcs=true,  pcsAbs=true, pcsDiff=true, pcsRate=true}; " +
		"Proc Cache Memory Usage ={postpone=0,   paused=false, bg=true,  resetNC20=false, storePcs=true,  pcsAbs=true, pcsDiff=true, pcsRate=true}; " +
		"Statement Cache         ={postpone=0,   paused=false, bg=true,  resetNC20=false, storePcs=true,  pcsAbs=true, pcsDiff=true, pcsRate=true}; " +
		"Statement Cache Details ={postpone=0,   paused=false, bg=true,  resetNC20=false, storePcs=true,  pcsAbs=true, pcsDiff=true, pcsRate=true}; " +
		"Active Objects          ={postpone=0,   paused=false, bg=true,  resetNC20=false, storePcs=true,  pcsAbs=true, pcsDiff=true, pcsRate=true}; " +
		"Active Statements       ={postpone=0,   paused=false, bg=true,  resetNC20=true,  storePcs=true,  pcsAbs=true, pcsDiff=true, pcsRate=true}; " +
		"Blocking                ={postpone=0,   paused=false, bg=true,  resetNC20=false, storePcs=true,  pcsAbs=true, pcsDiff=true, pcsRate=true}; " +
		"Missing Statistics      ={postpone=0,   paused=false, bg=true,  resetNC20=false, storePcs=true,  pcsAbs=true, pcsDiff=true, pcsRate=true}; " +
		"sp_monitorconfig        ={postpone=3600,paused=false, bg=true,  resetNC20=false, storePcs=true,  pcsAbs=true, pcsDiff=true, pcsRate=true}; " +
		"OS Disk Stat            ={postpone=0,   paused=false, bg=true,  resetNC20=false, storePcs=true,  pcsAbs=true, pcsDiff=false,pcsRate=false};" +
		"OS CPU(vmstat)          ={postpone=0,   paused=false, bg=true,  resetNC20=false, storePcs=true,  pcsAbs=true, pcsDiff=false,pcsRate=false};" +
		"OS CPU(mpstat)          ={postpone=0,   paused=false, bg=true,  resetNC20=false, storePcs=true,  pcsAbs=true, pcsDiff=false,pcsRate=false};";



	//----------------------------------------------------------------
	// PCS OFF
	//----------------------------------------------------------------

	public final static String systemTemplatePcsOffSmall = 
		"Objects                 ={postpone=0,   paused=false, bg=false, resetNC20=true,  storePcs=false, pcsAbs=true, pcsDiff=true, pcsRate=true}; " +
		"Processes               ={postpone=0,   paused=false, bg=false, resetNC20=true,  storePcs=false, pcsAbs=true, pcsDiff=true, pcsRate=true}; " +
		"Databases               ={postpone=0,   paused=false, bg=false, resetNC20=true,  storePcs=false, pcsAbs=true, pcsDiff=true, pcsRate=true}; " +
		"Temp Db                 ={postpone=0,   paused=false, bg=false, resetNC20=true,  storePcs=false, pcsAbs=true, pcsDiff=true, pcsRate=true}; " +
		"Waits                   ={postpone=0,   paused=false, bg=false, resetNC20=true,  storePcs=false, pcsAbs=true, pcsDiff=true, pcsRate=true}; " +
		"Engines                 ={postpone=0,   paused=false, bg=true,  resetNC20=true,  storePcs=false, pcsAbs=true, pcsDiff=true, pcsRate=true}; " +
		"System Load             ={postpone=0,   paused=false, bg=false, resetNC20=false, storePcs=false, pcsAbs=true, pcsDiff=true, pcsRate=true}; " +
		"Data Caches             ={postpone=0,   paused=false, bg=true,  resetNC20=true,  storePcs=false, pcsAbs=true, pcsDiff=true, pcsRate=true}; " +
		"Pools                   ={postpone=0,   paused=false, bg=false, resetNC20=true,  storePcs=false, pcsAbs=true, pcsDiff=true, pcsRate=true}; " +
		"Devices                 ={postpone=0,   paused=false, bg=false, resetNC20=true,  storePcs=false, pcsAbs=true, pcsDiff=true, pcsRate=true}; " +
		"IO Sum                  ={postpone=0,   paused=false, bg=true,  resetNC20=true,  storePcs=false, pcsAbs=true, pcsDiff=true, pcsRate=true}; " +
		"IO Queue                ={postpone=0,   paused=false, bg=true,  resetNC20=true,  storePcs=false, pcsAbs=true, pcsDiff=true, pcsRate=true}; " +
		"Spinlock Sum            ={postpone=300, paused=false, bg=false, resetNC20=true,  storePcs=false, pcsAbs=true, pcsDiff=true, pcsRate=true}; " +
		"Sysmon Raw              ={postpone=300, paused=false, bg=false, resetNC20=true,  storePcs=false, pcsAbs=true, pcsDiff=true, pcsRate=true}; " +
		"RepAgent                ={postpone=300, paused=false, bg=false, resetNC20=true,  storePcs=false, pcsAbs=true, pcsDiff=true, pcsRate=true}; " +
		"Cached Procedures       ={postpone=0,   paused=false, bg=false, resetNC20=true,  storePcs=false, pcsAbs=true, pcsDiff=true, pcsRate=true}; " +
		"Procedure Cache         ={postpone=0,   paused=false, bg=true,  resetNC20=true,  storePcs=false, pcsAbs=true, pcsDiff=true, pcsRate=true}; " +
		"Procedure Call Stack    ={postpone=0,   paused=false, bg=false, resetNC20=true,  storePcs=false, pcsAbs=true, pcsDiff=true, pcsRate=true}; " +
		"Cached Objects          ={postpone=600, paused=false, bg=false, resetNC20=false, storePcs=false, pcsAbs=true, pcsDiff=true, pcsRate=true}; " +
		"Errorlog                ={postpone=0,   paused=false, bg=false, resetNC20=true,  storePcs=false, pcsAbs=true, pcsDiff=false,pcsRate=false};" +
		"Deadlock                ={postpone=0,   paused=false, bg=false, resetNC20=true,  storePcs=false, pcsAbs=true, pcsDiff=false,pcsRate=false};" +
		"Proc Cache Module Usage ={postpone=0,   paused=false, bg=false, resetNC20=false, storePcs=false, pcsAbs=true, pcsDiff=true, pcsRate=true}; " +
		"Proc Cache Memory Usage ={postpone=0,   paused=false, bg=false, resetNC20=false, storePcs=false, pcsAbs=true, pcsDiff=true, pcsRate=true}; " +
		"Statement Cache         ={postpone=0,   paused=false, bg=false, resetNC20=false, storePcs=false, pcsAbs=true, pcsDiff=true, pcsRate=true}; " +
		"Statement Cache Details ={postpone=0,   paused=false, bg=false, resetNC20=false, storePcs=false, pcsAbs=true, pcsDiff=true, pcsRate=true}; " +
		"Active Objects          ={postpone=0,   paused=false, bg=false, resetNC20=false, storePcs=false, pcsAbs=true, pcsDiff=true, pcsRate=true}; " +
		"Active Statements       ={postpone=0,   paused=false, bg=false, resetNC20=true,  storePcs=false, pcsAbs=true, pcsDiff=true, pcsRate=true}; " +
		"Blocking                ={postpone=0,   paused=false, bg=false, resetNC20=false, storePcs=false, pcsAbs=true, pcsDiff=true, pcsRate=true}; " +
		"Missing Statistics      ={postpone=0,   paused=false, bg=false, resetNC20=false, storePcs=false, pcsAbs=true, pcsDiff=true, pcsRate=true}; " +
		"sp_monitorconfig        ={postpone=3600,paused=false, bg=false, resetNC20=false, storePcs=false, pcsAbs=true, pcsDiff=true, pcsRate=true}; " +
		"OS Disk Stat            ={postpone=0,   paused=false, bg=false, resetNC20=false, storePcs=false, pcsAbs=true, pcsDiff=false,pcsRate=false};" +
		"OS CPU(vmstat)          ={postpone=0,   paused=false, bg=false, resetNC20=false, storePcs=false, pcsAbs=true, pcsDiff=false,pcsRate=false};" +
		"OS CPU(mpstat)          ={postpone=0,   paused=false, bg=false, resetNC20=false, storePcs=false, pcsAbs=true, pcsDiff=false,pcsRate=false};";

	public final static String systemTemplatePcsOffMedium = 
		"Objects                 ={postpone=0,   paused=false, bg=false, resetNC20=true,  storePcs=false, pcsAbs=true, pcsDiff=true, pcsRate=true}; " +
		"Processes               ={postpone=0,   paused=false, bg=false, resetNC20=true,  storePcs=false, pcsAbs=true, pcsDiff=true, pcsRate=true}; " +
		"Databases               ={postpone=0,   paused=false, bg=false, resetNC20=true,  storePcs=false, pcsAbs=true, pcsDiff=true, pcsRate=true}; " +
		"Temp Db                 ={postpone=0,   paused=false, bg=false, resetNC20=true,  storePcs=false, pcsAbs=true, pcsDiff=true, pcsRate=true}; " +
		"Waits                   ={postpone=0,   paused=false, bg=false, resetNC20=true,  storePcs=false, pcsAbs=true, pcsDiff=true, pcsRate=true}; " +
		"Engines                 ={postpone=0,   paused=false, bg=true,  resetNC20=true,  storePcs=false, pcsAbs=true, pcsDiff=true, pcsRate=true}; " +
		"System Load             ={postpone=0,   paused=false, bg=false, resetNC20=false, storePcs=false, pcsAbs=true, pcsDiff=true, pcsRate=true}; " +
		"Data Caches             ={postpone=0,   paused=false, bg=true,  resetNC20=true,  storePcs=false, pcsAbs=true, pcsDiff=true, pcsRate=true}; " +
		"Pools                   ={postpone=0,   paused=false, bg=false, resetNC20=true,  storePcs=false, pcsAbs=true, pcsDiff=true, pcsRate=true}; " +
		"Devices                 ={postpone=0,   paused=false, bg=false, resetNC20=true,  storePcs=false, pcsAbs=true, pcsDiff=true, pcsRate=true}; " +
		"IO Sum                  ={postpone=0,   paused=false, bg=true,  resetNC20=true,  storePcs=false, pcsAbs=true, pcsDiff=true, pcsRate=true}; " +
		"IO Queue                ={postpone=0,   paused=false, bg=true,  resetNC20=true,  storePcs=false, pcsAbs=true, pcsDiff=true, pcsRate=true}; " +
		"Spinlock Sum            ={postpone=300, paused=false, bg=false, resetNC20=true,  storePcs=false, pcsAbs=true, pcsDiff=true, pcsRate=true}; " +
		"Sysmon Raw              ={postpone=300, paused=false, bg=false, resetNC20=true,  storePcs=false, pcsAbs=true, pcsDiff=true, pcsRate=true}; " +
		"RepAgent                ={postpone=300, paused=false, bg=false, resetNC20=true,  storePcs=false, pcsAbs=true, pcsDiff=true, pcsRate=true}; " +
		"Cached Procedures       ={postpone=0,   paused=false, bg=false, resetNC20=true,  storePcs=false, pcsAbs=true, pcsDiff=true, pcsRate=true}; " +
		"Procedure Cache         ={postpone=0,   paused=false, bg=true,  resetNC20=true,  storePcs=false, pcsAbs=true, pcsDiff=true, pcsRate=true}; " +
		"Procedure Call Stack    ={postpone=0,   paused=false, bg=false, resetNC20=true,  storePcs=false, pcsAbs=true, pcsDiff=true, pcsRate=true}; " +
		"Cached Objects          ={postpone=600, paused=false, bg=false, resetNC20=false, storePcs=false, pcsAbs=true, pcsDiff=true, pcsRate=true}; " +
		"Errorlog                ={postpone=0,   paused=false, bg=false, resetNC20=true,  storePcs=false, pcsAbs=true, pcsDiff=false,pcsRate=false};" +
		"Deadlock                ={postpone=0,   paused=false, bg=false, resetNC20=true,  storePcs=false, pcsAbs=true, pcsDiff=false,pcsRate=false};" +
		"Proc Cache Module Usage ={postpone=0,   paused=false, bg=false, resetNC20=false, storePcs=false, pcsAbs=true, pcsDiff=true, pcsRate=true}; " +
		"Proc Cache Memory Usage ={postpone=0,   paused=false, bg=false, resetNC20=false, storePcs=false, pcsAbs=true, pcsDiff=true, pcsRate=true}; " +
		"Statement Cache         ={postpone=0,   paused=false, bg=false, resetNC20=false, storePcs=false, pcsAbs=true, pcsDiff=true, pcsRate=true}; " +
		"Statement Cache Details ={postpone=0,   paused=false, bg=false, resetNC20=false, storePcs=false, pcsAbs=true, pcsDiff=true, pcsRate=true}; " +
		"Active Objects          ={postpone=0,   paused=false, bg=false, resetNC20=false, storePcs=false, pcsAbs=true, pcsDiff=true, pcsRate=true}; " +
		"Active Statements       ={postpone=0,   paused=false, bg=false, resetNC20=true,  storePcs=false, pcsAbs=true, pcsDiff=true, pcsRate=true}; " +
		"Blocking                ={postpone=0,   paused=false, bg=false, resetNC20=false, storePcs=false, pcsAbs=true, pcsDiff=true, pcsRate=true}; " +
		"Missing Statistics      ={postpone=0,   paused=false, bg=false, resetNC20=false, storePcs=false, pcsAbs=true, pcsDiff=true, pcsRate=true}; " +
		"sp_monitorconfig        ={postpone=3600,paused=false, bg=false, resetNC20=false, storePcs=false, pcsAbs=true, pcsDiff=true, pcsRate=true}; " +
		"OS Disk Stat            ={postpone=0,   paused=false, bg=false, resetNC20=false, storePcs=false, pcsAbs=true, pcsDiff=false,pcsRate=false};" +
		"OS CPU(vmstat)          ={postpone=0,   paused=false, bg=false, resetNC20=false, storePcs=false, pcsAbs=true, pcsDiff=false,pcsRate=false};" +
		"OS CPU(mpstat)          ={postpone=0,   paused=false, bg=false, resetNC20=false, storePcs=false, pcsAbs=true, pcsDiff=false,pcsRate=false};";

	public final static String systemTemplatePcsOffLarge = 
		"Objects                 ={postpone=0,   paused=false, bg=false, resetNC20=true,  storePcs=false, pcsAbs=true, pcsDiff=true, pcsRate=true}; " +
		"Processes               ={postpone=0,   paused=false, bg=false, resetNC20=true,  storePcs=false, pcsAbs=true, pcsDiff=true, pcsRate=true}; " +
		"Databases               ={postpone=0,   paused=false, bg=false, resetNC20=true,  storePcs=false, pcsAbs=true, pcsDiff=true, pcsRate=true}; " +
		"Temp Db                 ={postpone=0,   paused=false, bg=false, resetNC20=true,  storePcs=false, pcsAbs=true, pcsDiff=true, pcsRate=true}; " +
		"Waits                   ={postpone=0,   paused=false, bg=false, resetNC20=true,  storePcs=false, pcsAbs=true, pcsDiff=true, pcsRate=true}; " +
		"Engines                 ={postpone=0,   paused=false, bg=true,  resetNC20=true,  storePcs=false, pcsAbs=true, pcsDiff=true, pcsRate=true}; " +
		"System Load             ={postpone=0,   paused=false, bg=false, resetNC20=false, storePcs=false, pcsAbs=true, pcsDiff=true, pcsRate=true}; " +
		"Data Caches             ={postpone=0,   paused=false, bg=true,  resetNC20=true,  storePcs=false, pcsAbs=true, pcsDiff=true, pcsRate=true}; " +
		"Pools                   ={postpone=0,   paused=false, bg=false, resetNC20=true,  storePcs=false, pcsAbs=true, pcsDiff=true, pcsRate=true}; " +
		"Devices                 ={postpone=0,   paused=false, bg=false, resetNC20=true,  storePcs=false, pcsAbs=true, pcsDiff=true, pcsRate=true}; " +
		"IO Sum                  ={postpone=0,   paused=false, bg=true,  resetNC20=true,  storePcs=false, pcsAbs=true, pcsDiff=true, pcsRate=true}; " +
		"IO Queue                ={postpone=0,   paused=false, bg=true,  resetNC20=true,  storePcs=false, pcsAbs=true, pcsDiff=true, pcsRate=true}; " +
		"Spinlock Sum            ={postpone=300, paused=false, bg=false, resetNC20=true,  storePcs=false, pcsAbs=true, pcsDiff=true, pcsRate=true}; " +
		"Sysmon Raw              ={postpone=300, paused=false, bg=false, resetNC20=true,  storePcs=false, pcsAbs=true, pcsDiff=true, pcsRate=true}; " +
		"RepAgent                ={postpone=300, paused=false, bg=false, resetNC20=true,  storePcs=false, pcsAbs=true, pcsDiff=true, pcsRate=true}; " +
		"Cached Procedures       ={postpone=0,   paused=false, bg=false, resetNC20=true,  storePcs=false, pcsAbs=true, pcsDiff=true, pcsRate=true}; " +
		"Procedure Cache         ={postpone=0,   paused=false, bg=true,  resetNC20=true,  storePcs=false, pcsAbs=true, pcsDiff=true, pcsRate=true}; " +
		"Procedure Call Stack    ={postpone=0,   paused=false, bg=false, resetNC20=true,  storePcs=false, pcsAbs=true, pcsDiff=true, pcsRate=true}; " +
		"Cached Objects          ={postpone=600, paused=false, bg=false, resetNC20=false, storePcs=false, pcsAbs=true, pcsDiff=true, pcsRate=true}; " +
		"Errorlog                ={postpone=0,   paused=false, bg=false, resetNC20=true,  storePcs=false, pcsAbs=true, pcsDiff=false,pcsRate=false};" +
		"Deadlock                ={postpone=0,   paused=false, bg=false, resetNC20=true,  storePcs=false, pcsAbs=true, pcsDiff=false,pcsRate=false};" +
		"Proc Cache Module Usage ={postpone=0,   paused=false, bg=false, resetNC20=false, storePcs=false, pcsAbs=true, pcsDiff=true, pcsRate=true}; " +
		"Proc Cache Memory Usage ={postpone=0,   paused=false, bg=false, resetNC20=false, storePcs=false, pcsAbs=true, pcsDiff=true, pcsRate=true}; " +
		"Statement Cache         ={postpone=0,   paused=false, bg=false, resetNC20=false, storePcs=false, pcsAbs=true, pcsDiff=true, pcsRate=true}; " +
		"Statement Cache Details ={postpone=0,   paused=false, bg=false, resetNC20=false, storePcs=false, pcsAbs=true, pcsDiff=true, pcsRate=true}; " +
		"Active Objects          ={postpone=0,   paused=false, bg=false, resetNC20=false, storePcs=false, pcsAbs=true, pcsDiff=true, pcsRate=true}; " +
		"Active Statements       ={postpone=0,   paused=false, bg=false, resetNC20=true,  storePcs=false, pcsAbs=true, pcsDiff=true, pcsRate=true}; " +
		"Blocking                ={postpone=0,   paused=false, bg=false, resetNC20=false, storePcs=false, pcsAbs=true, pcsDiff=true, pcsRate=true}; " +
		"Missing Statistics      ={postpone=0,   paused=false, bg=false, resetNC20=false, storePcs=false, pcsAbs=true, pcsDiff=true, pcsRate=true}; " +
		"sp_monitorconfig        ={postpone=3600,paused=false, bg=false, resetNC20=false, storePcs=false, pcsAbs=true, pcsDiff=true, pcsRate=true}; " +
		"OS Disk Stat            ={postpone=0,   paused=false, bg=false, resetNC20=false, storePcs=false, pcsAbs=true, pcsDiff=false,pcsRate=false};" +
		"OS CPU(vmstat)          ={postpone=0,   paused=false, bg=false, resetNC20=false, storePcs=false, pcsAbs=true, pcsDiff=false,pcsRate=false};" +
		"OS CPU(mpstat)          ={postpone=0,   paused=false, bg=false, resetNC20=false, storePcs=false, pcsAbs=true, pcsDiff=false,pcsRate=false};";

	public final static String systemTemplatePcsOffAll = 
		"Objects                 ={postpone=0,   paused=false, bg=false, resetNC20=true,  storePcs=false, pcsAbs=true, pcsDiff=true, pcsRate=true}; " +
		"Processes               ={postpone=0,   paused=false, bg=false, resetNC20=true,  storePcs=false, pcsAbs=true, pcsDiff=true, pcsRate=true}; " +
		"Databases               ={postpone=0,   paused=false, bg=false, resetNC20=true,  storePcs=false, pcsAbs=true, pcsDiff=true, pcsRate=true}; " +
		"Temp Db                 ={postpone=0,   paused=false, bg=false, resetNC20=true,  storePcs=false, pcsAbs=true, pcsDiff=true, pcsRate=true}; " +
		"Waits                   ={postpone=0,   paused=false, bg=false, resetNC20=true,  storePcs=false, pcsAbs=true, pcsDiff=true, pcsRate=true}; " +
		"Engines                 ={postpone=0,   paused=false, bg=true,  resetNC20=true,  storePcs=false, pcsAbs=true, pcsDiff=true, pcsRate=true}; " +
		"System Load             ={postpone=0,   paused=false, bg=false, resetNC20=false, storePcs=false, pcsAbs=true, pcsDiff=true, pcsRate=true}; " +
		"Data Caches             ={postpone=0,   paused=false, bg=true,  resetNC20=true,  storePcs=false, pcsAbs=true, pcsDiff=true, pcsRate=true}; " +
		"Pools                   ={postpone=0,   paused=false, bg=false, resetNC20=true,  storePcs=false, pcsAbs=true, pcsDiff=true, pcsRate=true}; " +
		"Devices                 ={postpone=0,   paused=false, bg=false, resetNC20=true,  storePcs=false, pcsAbs=true, pcsDiff=true, pcsRate=true}; " +
		"IO Sum                  ={postpone=0,   paused=false, bg=true,  resetNC20=true,  storePcs=false, pcsAbs=true, pcsDiff=true, pcsRate=true}; " +
		"IO Queue                ={postpone=0,   paused=false, bg=true,  resetNC20=true,  storePcs=false, pcsAbs=true, pcsDiff=true, pcsRate=true}; " +
		"Spinlock Sum            ={postpone=300, paused=false, bg=false, resetNC20=true,  storePcs=false, pcsAbs=true, pcsDiff=true, pcsRate=true}; " +
		"Sysmon Raw              ={postpone=300, paused=false, bg=false, resetNC20=true,  storePcs=false, pcsAbs=true, pcsDiff=true, pcsRate=true}; " +
		"RepAgent                ={postpone=300, paused=false, bg=false, resetNC20=true,  storePcs=false, pcsAbs=true, pcsDiff=true, pcsRate=true}; " +
		"Cached Procedures       ={postpone=0,   paused=false, bg=false, resetNC20=true,  storePcs=false, pcsAbs=true, pcsDiff=true, pcsRate=true}; " +
		"Procedure Cache         ={postpone=0,   paused=false, bg=true,  resetNC20=true,  storePcs=false, pcsAbs=true, pcsDiff=true, pcsRate=true}; " +
		"Procedure Call Stack    ={postpone=0,   paused=false, bg=false, resetNC20=true,  storePcs=false, pcsAbs=true, pcsDiff=true, pcsRate=true}; " +
		"Cached Objects          ={postpone=600, paused=false, bg=false, resetNC20=false, storePcs=false, pcsAbs=true, pcsDiff=true, pcsRate=true}; " +
		"Errorlog                ={postpone=0,   paused=false, bg=false, resetNC20=true,  storePcs=false, pcsAbs=true, pcsDiff=false,pcsRate=false};" +
		"Deadlock                ={postpone=0,   paused=false, bg=false, resetNC20=true,  storePcs=false, pcsAbs=true, pcsDiff=false,pcsRate=false};" +
		"Proc Cache Module Usage ={postpone=0,   paused=false, bg=false, resetNC20=false, storePcs=false, pcsAbs=true, pcsDiff=true, pcsRate=true}; " +
		"Proc Cache Memory Usage ={postpone=0,   paused=false, bg=false, resetNC20=false, storePcs=false, pcsAbs=true, pcsDiff=true, pcsRate=true}; " +
		"Statement Cache         ={postpone=0,   paused=false, bg=false, resetNC20=false, storePcs=false, pcsAbs=true, pcsDiff=true, pcsRate=true}; " +
		"Statement Cache Details ={postpone=0,   paused=false, bg=false, resetNC20=false, storePcs=false, pcsAbs=true, pcsDiff=true, pcsRate=true}; " +
		"Active Objects          ={postpone=0,   paused=false, bg=false, resetNC20=false, storePcs=false, pcsAbs=true, pcsDiff=true, pcsRate=true}; " +
		"Active Statements       ={postpone=0,   paused=false, bg=false, resetNC20=true,  storePcs=false, pcsAbs=true, pcsDiff=true, pcsRate=true}; " +
		"Blocking                ={postpone=0,   paused=false, bg=false, resetNC20=false, storePcs=false, pcsAbs=true, pcsDiff=true, pcsRate=true}; " +
		"Missing Statistics      ={postpone=0,   paused=false, bg=false, resetNC20=false, storePcs=false, pcsAbs=true, pcsDiff=true, pcsRate=true}; " +
		"sp_monitorconfig        ={postpone=3600,paused=false, bg=false, resetNC20=false, storePcs=false, pcsAbs=true, pcsDiff=true, pcsRate=true}; " +
		"OS Disk Stat            ={postpone=0,   paused=false, bg=false, resetNC20=false, storePcs=false, pcsAbs=true, pcsDiff=false,pcsRate=false};" +
		"OS CPU(vmstat)          ={postpone=0,   paused=false, bg=false, resetNC20=false, storePcs=false, pcsAbs=true, pcsDiff=false,pcsRate=false};" +
		"OS CPU(mpstat)          ={postpone=0,   paused=false, bg=false, resetNC20=false, storePcs=false, pcsAbs=true, pcsDiff=false,pcsRate=false};";


	public final static PropPropEntry SYSTEM_TEMPLATE_SUMMARY_CM     = new PropPropEntry(systemSummaryCm);

	public final static PropPropEntry SYSTEM_TEMPLATE_PCS_ON_SMALL   = new PropPropEntry(systemTemplatePcsOnSmall);
	public final static PropPropEntry SYSTEM_TEMPLATE_PCS_ON_MEDIUM  = new PropPropEntry(systemTemplatePcsOnMedium);
	public final static PropPropEntry SYSTEM_TEMPLATE_PCS_ON_LARGE   = new PropPropEntry(systemTemplatePcsOnLarge);
	public final static PropPropEntry SYSTEM_TEMPLATE_PCS_ON_ALL     = new PropPropEntry(systemTemplatePcsOnAll);

	public final static PropPropEntry SYSTEM_TEMPLATE_PCS_OFF_SMALL  = new PropPropEntry(systemTemplatePcsOffSmall);
	public final static PropPropEntry SYSTEM_TEMPLATE_PCS_OFF_MEDIUM = new PropPropEntry(systemTemplatePcsOffMedium);
	public final static PropPropEntry SYSTEM_TEMPLATE_PCS_OFF_LARGE  = new PropPropEntry(systemTemplatePcsOffLarge);
	public final static PropPropEntry SYSTEM_TEMPLATE_PCS_OFF_ALL    = new PropPropEntry(systemTemplatePcsOffAll);

	public static void main(String[] args)
	{
		System.out.println("SYSTEM_TEMPLATE_PCS_ON_SMALL  : "+SYSTEM_TEMPLATE_PCS_ON_SMALL);
		System.out.println("SYSTEM_TEMPLATE_PCS_ON_MEDIUM : "+SYSTEM_TEMPLATE_PCS_ON_MEDIUM);
		System.out.println("SYSTEM_TEMPLATE_PCS_ON_LARGE  : "+SYSTEM_TEMPLATE_PCS_ON_LARGE);
		System.out.println("SYSTEM_TEMPLATE_PCS_ON_ALL    : "+SYSTEM_TEMPLATE_PCS_ON_ALL);

		System.out.println();
		System.out.println("SYSTEM_TEMPLATE_PCS_OFF_SMALL : "+SYSTEM_TEMPLATE_PCS_OFF_SMALL);
		System.out.println("SYSTEM_TEMPLATE_PCS_OFF_MEDIUM: "+SYSTEM_TEMPLATE_PCS_OFF_MEDIUM);
		System.out.println("SYSTEM_TEMPLATE_PCS_OFF_LARGE : "+SYSTEM_TEMPLATE_PCS_OFF_LARGE);
		System.out.println("SYSTEM_TEMPLATE_PCS_OFF_ALL   : "+SYSTEM_TEMPLATE_PCS_OFF_ALL);
	}
}

