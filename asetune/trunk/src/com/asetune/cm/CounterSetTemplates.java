package com.asetune.cm;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.asetune.CounterController;
import com.asetune.GetCounters;
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
		_nameShortToLongMap.put(GetCounters.CM_NAME__SUMMARY                 , GetCounters.CM_DESC__SUMMARY                 );
		_nameShortToLongMap.put(GetCounters.CM_NAME__OBJECT_ACTIVITY         , GetCounters.CM_DESC__OBJECT_ACTIVITY         );
		_nameShortToLongMap.put(GetCounters.CM_NAME__PROCESS_ACTIVITY        , GetCounters.CM_DESC__PROCESS_ACTIVITY        );
		_nameShortToLongMap.put(GetCounters.CM_NAME__PROCESS_WAIT            , GetCounters.CM_DESC__PROCESS_WAIT            );
		_nameShortToLongMap.put(GetCounters.CM_NAME__OPEN_DATABASES          , GetCounters.CM_DESC__OPEN_DATABASES          );
		_nameShortToLongMap.put(GetCounters.CM_NAME__TEMPDB_ACTIVITY         , GetCounters.CM_DESC__TEMPDB_ACTIVITY         );
		_nameShortToLongMap.put(GetCounters.CM_NAME__SYS_WAIT                , GetCounters.CM_DESC__SYS_WAIT                );
		_nameShortToLongMap.put(GetCounters.CM_NAME__ENGINE                  , GetCounters.CM_DESC__ENGINE                  );
		_nameShortToLongMap.put(GetCounters.CM_NAME__SYS_LOAD                , GetCounters.CM_DESC__SYS_LOAD                );
		_nameShortToLongMap.put(GetCounters.CM_NAME__DATA_CACHE              , GetCounters.CM_DESC__DATA_CACHE              );
		_nameShortToLongMap.put(GetCounters.CM_NAME__CACHE_POOL              , GetCounters.CM_DESC__CACHE_POOL              );
		_nameShortToLongMap.put(GetCounters.CM_NAME__DEVICE_IO               , GetCounters.CM_DESC__DEVICE_IO               );
		_nameShortToLongMap.put(GetCounters.CM_NAME__IO_QUEUE_SUM            , GetCounters.CM_DESC__IO_QUEUE_SUM            );
		_nameShortToLongMap.put(GetCounters.CM_NAME__IO_QUEUE                , GetCounters.CM_DESC__IO_QUEUE                );
		_nameShortToLongMap.put(GetCounters.CM_NAME__SPINLOCK_SUM            , GetCounters.CM_DESC__SPINLOCK_SUM            );
		_nameShortToLongMap.put(GetCounters.CM_NAME__SYSMON                  , GetCounters.CM_DESC__SYSMON                  );
		_nameShortToLongMap.put(GetCounters.CM_NAME__RA_SENDERS              , GetCounters.CM_DESC__RA_SENDERS              );
		_nameShortToLongMap.put(GetCounters.CM_NAME__RA_LOGACTIVITY          , GetCounters.CM_DESC__RA_LOGACTIVITY          );
		_nameShortToLongMap.put(GetCounters.CM_NAME__RA_SCANNERS             , GetCounters.CM_DESC__RA_SCANNERS             );
		_nameShortToLongMap.put(GetCounters.CM_NAME__RA_SCANNERS_TIME        , GetCounters.CM_DESC__RA_SCANNERS_TIME        );
		_nameShortToLongMap.put(GetCounters.CM_NAME__RA_SQL_ACTIVITY         , GetCounters.CM_DESC__RA_SQL_ACTIVITY         );
		_nameShortToLongMap.put(GetCounters.CM_NAME__RA_SQL_MISSES           , GetCounters.CM_DESC__RA_SQL_MISSES           );
		_nameShortToLongMap.put(GetCounters.CM_NAME__RA_SYSMON               , GetCounters.CM_DESC__RA_SYSMON               );
		_nameShortToLongMap.put(GetCounters.CM_NAME__CACHED_PROC             , GetCounters.CM_DESC__CACHED_PROC             );
		_nameShortToLongMap.put(GetCounters.CM_NAME__PROC_CACHE_LOAD         , GetCounters.CM_DESC__PROC_CACHE_LOAD         );
		_nameShortToLongMap.put(GetCounters.CM_NAME__PROC_CALL_STACK         , GetCounters.CM_DESC__PROC_CALL_STACK         );
		_nameShortToLongMap.put(GetCounters.CM_NAME__CACHED_OBJECTS          , GetCounters.CM_DESC__CACHED_OBJECTS          );
		_nameShortToLongMap.put(GetCounters.CM_NAME__ERRORLOG                , GetCounters.CM_DESC__ERRORLOG                );
		_nameShortToLongMap.put(GetCounters.CM_NAME__DEADLOCK                , GetCounters.CM_DESC__DEADLOCK                );
		_nameShortToLongMap.put(GetCounters.CM_NAME__LOCK_TIMEOUT            , GetCounters.CM_DESC__LOCK_TIMEOUT            );
		_nameShortToLongMap.put(GetCounters.CM_NAME__PROC_CACHE_MODULE_USAGE , GetCounters.CM_DESC__PROC_CACHE_MODULE_USAGE );
		_nameShortToLongMap.put(GetCounters.CM_NAME__PROC_CACHE_MEMORY_USAGE , GetCounters.CM_DESC__PROC_CACHE_MEMORY_USAGE );
		_nameShortToLongMap.put(GetCounters.CM_NAME__STATEMENT_CACHE         , GetCounters.CM_DESC__STATEMENT_CACHE         );
		_nameShortToLongMap.put(GetCounters.CM_NAME__STATEMENT_CACHE_DETAILS , GetCounters.CM_DESC__STATEMENT_CACHE_DETAILS );
		_nameShortToLongMap.put(GetCounters.CM_NAME__ACTIVE_OBJECTS          , GetCounters.CM_DESC__ACTIVE_OBJECTS          );
		_nameShortToLongMap.put(GetCounters.CM_NAME__ACTIVE_STATEMENTS       , GetCounters.CM_DESC__ACTIVE_STATEMENTS       );
		_nameShortToLongMap.put(GetCounters.CM_NAME__BLOCKING                , GetCounters.CM_DESC__BLOCKING                );
		_nameShortToLongMap.put(GetCounters.CM_NAME__TABLE_COMPRESSION       , GetCounters.CM_DESC__TABLE_COMPRESSION       );
		_nameShortToLongMap.put(GetCounters.CM_NAME__MISSING_STATISTICS      , GetCounters.CM_DESC__MISSING_STATISTICS      );
		_nameShortToLongMap.put(GetCounters.CM_NAME__QP_METRICS              , GetCounters.CM_DESC__QP_METRICS              );
		_nameShortToLongMap.put(GetCounters.CM_NAME__SP_MONITOR_CONFIG       , GetCounters.CM_DESC__SP_MONITOR_CONFIG       );
		_nameShortToLongMap.put(GetCounters.CM_NAME__OS_IOSTAT               , GetCounters.CM_DESC__OS_IOSTAT               );
		_nameShortToLongMap.put(GetCounters.CM_NAME__OS_VMSTAT               , GetCounters.CM_DESC__OS_VMSTAT               );
		_nameShortToLongMap.put(GetCounters.CM_NAME__OS_MPSTAT               , GetCounters.CM_DESC__OS_MPSTAT               );
		_nameShortToLongMap.put(GetCounters.CM_NAME__OS_UPTIME               , GetCounters.CM_DESC__OS_UPTIME               );

		// LONG -> SHORT
		_nameLongToShortMap.put(GetCounters.CM_DESC__SUMMARY                 , GetCounters.CM_NAME__SUMMARY                 );
		_nameLongToShortMap.put(GetCounters.CM_DESC__OBJECT_ACTIVITY         , GetCounters.CM_NAME__OBJECT_ACTIVITY         );
		_nameLongToShortMap.put(GetCounters.CM_DESC__PROCESS_ACTIVITY        , GetCounters.CM_NAME__PROCESS_ACTIVITY        );
		_nameLongToShortMap.put(GetCounters.CM_DESC__PROCESS_WAIT            , GetCounters.CM_NAME__PROCESS_WAIT            );
		_nameLongToShortMap.put(GetCounters.CM_DESC__OPEN_DATABASES          , GetCounters.CM_NAME__OPEN_DATABASES          );
		_nameLongToShortMap.put(GetCounters.CM_DESC__TEMPDB_ACTIVITY         , GetCounters.CM_NAME__TEMPDB_ACTIVITY         );
		_nameLongToShortMap.put(GetCounters.CM_DESC__SYS_WAIT                , GetCounters.CM_NAME__SYS_WAIT                );
		_nameLongToShortMap.put(GetCounters.CM_DESC__ENGINE                  , GetCounters.CM_NAME__ENGINE                  );
		_nameLongToShortMap.put(GetCounters.CM_DESC__SYS_LOAD                , GetCounters.CM_NAME__SYS_LOAD                );
		_nameLongToShortMap.put(GetCounters.CM_DESC__DATA_CACHE              , GetCounters.CM_NAME__DATA_CACHE              );
		_nameLongToShortMap.put(GetCounters.CM_DESC__CACHE_POOL              , GetCounters.CM_NAME__CACHE_POOL              );
		_nameLongToShortMap.put(GetCounters.CM_DESC__DEVICE_IO               , GetCounters.CM_NAME__DEVICE_IO               );
		_nameLongToShortMap.put(GetCounters.CM_DESC__IO_QUEUE_SUM            , GetCounters.CM_NAME__IO_QUEUE_SUM            );
		_nameLongToShortMap.put(GetCounters.CM_DESC__IO_QUEUE                , GetCounters.CM_NAME__IO_QUEUE                );
		_nameLongToShortMap.put(GetCounters.CM_DESC__SPINLOCK_SUM            , GetCounters.CM_NAME__SPINLOCK_SUM            );
		_nameLongToShortMap.put(GetCounters.CM_DESC__SYSMON                  , GetCounters.CM_NAME__SYSMON                  );
		_nameShortToLongMap.put(GetCounters.CM_DESC__RA_SENDERS              , GetCounters.CM_NAME__RA_SENDERS              );
		_nameShortToLongMap.put(GetCounters.CM_DESC__RA_LOGACTIVITY          , GetCounters.CM_NAME__RA_LOGACTIVITY          );
		_nameShortToLongMap.put(GetCounters.CM_DESC__RA_SCANNERS             , GetCounters.CM_NAME__RA_SCANNERS             );
		_nameShortToLongMap.put(GetCounters.CM_DESC__RA_SCANNERS_TIME        , GetCounters.CM_NAME__RA_SCANNERS_TIME        );
		_nameShortToLongMap.put(GetCounters.CM_DESC__RA_SQL_ACTIVITY         , GetCounters.CM_NAME__RA_SQL_ACTIVITY         );
		_nameShortToLongMap.put(GetCounters.CM_DESC__RA_SQL_MISSES           , GetCounters.CM_NAME__RA_SQL_MISSES           );
		_nameShortToLongMap.put(GetCounters.CM_DESC__RA_SYSMON               , GetCounters.CM_NAME__RA_SYSMON               );
		_nameLongToShortMap.put(GetCounters.CM_DESC__CACHED_PROC             , GetCounters.CM_NAME__CACHED_PROC             );
		_nameLongToShortMap.put(GetCounters.CM_DESC__PROC_CACHE_LOAD         , GetCounters.CM_NAME__PROC_CACHE_LOAD         );
		_nameLongToShortMap.put(GetCounters.CM_DESC__PROC_CALL_STACK         , GetCounters.CM_NAME__PROC_CALL_STACK         );
		_nameLongToShortMap.put(GetCounters.CM_DESC__CACHED_OBJECTS          , GetCounters.CM_NAME__CACHED_OBJECTS          );
		_nameLongToShortMap.put(GetCounters.CM_DESC__ERRORLOG                , GetCounters.CM_NAME__ERRORLOG                );
		_nameLongToShortMap.put(GetCounters.CM_DESC__DEADLOCK                , GetCounters.CM_NAME__DEADLOCK                );
		_nameLongToShortMap.put(GetCounters.CM_DESC__LOCK_TIMEOUT            , GetCounters.CM_NAME__LOCK_TIMEOUT            );
		_nameLongToShortMap.put(GetCounters.CM_DESC__PROC_CACHE_MODULE_USAGE , GetCounters.CM_NAME__PROC_CACHE_MODULE_USAGE );
		_nameLongToShortMap.put(GetCounters.CM_DESC__PROC_CACHE_MEMORY_USAGE , GetCounters.CM_NAME__PROC_CACHE_MEMORY_USAGE );
		_nameLongToShortMap.put(GetCounters.CM_DESC__STATEMENT_CACHE         , GetCounters.CM_NAME__STATEMENT_CACHE         );
		_nameLongToShortMap.put(GetCounters.CM_DESC__STATEMENT_CACHE_DETAILS , GetCounters.CM_NAME__STATEMENT_CACHE_DETAILS );
		_nameLongToShortMap.put(GetCounters.CM_DESC__ACTIVE_OBJECTS          , GetCounters.CM_NAME__ACTIVE_OBJECTS          );
		_nameLongToShortMap.put(GetCounters.CM_DESC__ACTIVE_STATEMENTS       , GetCounters.CM_NAME__ACTIVE_STATEMENTS       );
		_nameLongToShortMap.put(GetCounters.CM_DESC__BLOCKING                , GetCounters.CM_NAME__BLOCKING                );
		_nameLongToShortMap.put(GetCounters.CM_DESC__TABLE_COMPRESSION       , GetCounters.CM_NAME__TABLE_COMPRESSION       );
		_nameLongToShortMap.put(GetCounters.CM_DESC__MISSING_STATISTICS      , GetCounters.CM_NAME__MISSING_STATISTICS      );
		_nameLongToShortMap.put(GetCounters.CM_DESC__QP_METRICS              , GetCounters.CM_NAME__QP_METRICS              );
		_nameLongToShortMap.put(GetCounters.CM_DESC__SP_MONITOR_CONFIG       , GetCounters.CM_NAME__SP_MONITOR_CONFIG       );
		_nameLongToShortMap.put(GetCounters.CM_DESC__OS_IOSTAT               , GetCounters.CM_NAME__OS_IOSTAT               );
		_nameLongToShortMap.put(GetCounters.CM_DESC__OS_VMSTAT               , GetCounters.CM_NAME__OS_VMSTAT               );
		_nameLongToShortMap.put(GetCounters.CM_DESC__OS_MPSTAT               , GetCounters.CM_NAME__OS_MPSTAT               );
		_nameLongToShortMap.put(GetCounters.CM_DESC__OS_UPTIME               , GetCounters.CM_NAME__OS_UPTIME               );


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

//		activeCmList.add(SummaryPanel.CM_NAME);
		activeCmList.add(CounterController.getSummaryCmName());

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
		GetCounters.CM_DESC__SUMMARY                 +"={queryTimeout=10, postpone=0,   paused=false, bg=false, resetNC20=true,  storePcs=true,  pcsAbs=true, pcsDiff=true, pcsRate=true}; ";

	public final static String systemTemplatePcsOnSmall =
		GetCounters.CM_DESC__OBJECT_ACTIVITY         +"={queryTimeout=10, postpone=0,   paused=false, bg=false, resetNC20=true,  storePcs=false, pcsAbs=true, pcsDiff=true, pcsRate=true}; " +
		GetCounters.CM_DESC__PROCESS_ACTIVITY        +"={queryTimeout=10, postpone=0,   paused=false, bg=false, resetNC20=true,  storePcs=false, pcsAbs=true, pcsDiff=true, pcsRate=true}; " +
		GetCounters.CM_DESC__PROCESS_WAIT            +"={queryTimeout=10, postpone=0,   paused=false, bg=false, resetNC20=true,  storePcs=false, pcsAbs=true, pcsDiff=true, pcsRate=true}; " +
		GetCounters.CM_DESC__OPEN_DATABASES          +"={queryTimeout=10, postpone=0,   paused=false, bg=false, resetNC20=true,  storePcs=false, pcsAbs=true, pcsDiff=true, pcsRate=true}; " +
		GetCounters.CM_DESC__TEMPDB_ACTIVITY         +"={queryTimeout=10, postpone=0,   paused=false, bg=false, resetNC20=true,  storePcs=false, pcsAbs=true, pcsDiff=true, pcsRate=true}; " +
		GetCounters.CM_DESC__SYS_WAIT                +"={queryTimeout=10, postpone=0,   paused=false, bg=false, resetNC20=true,  storePcs=false, pcsAbs=true, pcsDiff=true, pcsRate=true}; " +
		GetCounters.CM_DESC__ENGINE                  +"={queryTimeout=10, postpone=0,   paused=false, bg=false, resetNC20=true,  storePcs=true,  pcsAbs=true, pcsDiff=true, pcsRate=true}; " +
		GetCounters.CM_DESC__SYS_LOAD                +"={queryTimeout=10, postpone=0,   paused=false, bg=false, resetNC20=false, storePcs=false, pcsAbs=true, pcsDiff=true, pcsRate=true}; " +
		GetCounters.CM_DESC__DATA_CACHE              +"={queryTimeout=10, postpone=0,   paused=false, bg=false, resetNC20=true,  storePcs=true,  pcsAbs=true, pcsDiff=true, pcsRate=true}; " +
		GetCounters.CM_DESC__CACHE_POOL              +"={queryTimeout=10, postpone=0,   paused=false, bg=false, resetNC20=true,  storePcs=false, pcsAbs=true, pcsDiff=true, pcsRate=true}; " +
		GetCounters.CM_DESC__DEVICE_IO               +"={queryTimeout=10, postpone=0,   paused=false, bg=false, resetNC20=true,  storePcs=false, pcsAbs=true, pcsDiff=true, pcsRate=true}; " +
		GetCounters.CM_DESC__IO_QUEUE_SUM            +"={queryTimeout=10, postpone=0,   paused=false, bg=false, resetNC20=true,  storePcs=true,  pcsAbs=true, pcsDiff=true, pcsRate=true}; " +
		GetCounters.CM_DESC__IO_QUEUE                +"={queryTimeout=10, postpone=0,   paused=false, bg=false, resetNC20=true,  storePcs=true,  pcsAbs=true, pcsDiff=true, pcsRate=true}; " +
		GetCounters.CM_DESC__SPINLOCK_SUM            +"={queryTimeout=30, postpone=300, paused=false, bg=false, resetNC20=true,  storePcs=false, pcsAbs=true, pcsDiff=true, pcsRate=true}; " +
		GetCounters.CM_DESC__SYSMON                  +"={queryTimeout=30, postpone=300, paused=false, bg=false, resetNC20=true,  storePcs=false, pcsAbs=true, pcsDiff=true, pcsRate=true}; " +
		GetCounters.CM_DESC__RA_SENDERS              +"={queryTimeout=10, postpone=0,   paused=false, bg=false, resetNC20=true,  storePcs=false, pcsAbs=true, pcsDiff=true, pcsRate=true}; " +
		GetCounters.CM_DESC__RA_LOGACTIVITY          +"={queryTimeout=10, postpone=0,   paused=false, bg=false, resetNC20=true,  storePcs=false, pcsAbs=true, pcsDiff=true, pcsRate=true}; " +
		GetCounters.CM_DESC__RA_SCANNERS             +"={queryTimeout=10, postpone=0,   paused=false, bg=false, resetNC20=true,  storePcs=false, pcsAbs=true, pcsDiff=true, pcsRate=true}; " +
		GetCounters.CM_DESC__RA_SCANNERS_TIME        +"={queryTimeout=10, postpone=0,   paused=false, bg=false, resetNC20=true,  storePcs=false, pcsAbs=true, pcsDiff=true, pcsRate=true}; " +
		GetCounters.CM_DESC__RA_SQL_ACTIVITY         +"={queryTimeout=10, postpone=0,   paused=false, bg=false, resetNC20=true,  storePcs=false, pcsAbs=true, pcsDiff=true, pcsRate=true}; " +
		GetCounters.CM_DESC__RA_SQL_MISSES           +"={queryTimeout=10, postpone=0,   paused=false, bg=false, resetNC20=true,  storePcs=false, pcsAbs=true, pcsDiff=true, pcsRate=true}; " +
		GetCounters.CM_DESC__RA_SYSMON               +"={queryTimeout=30, postpone=300, paused=false, bg=false, resetNC20=true,  storePcs=false, pcsAbs=true, pcsDiff=true, pcsRate=true}; " +
		GetCounters.CM_DESC__CACHED_PROC             +"={queryTimeout=10, postpone=0,   paused=false, bg=false, resetNC20=true,  storePcs=false, pcsAbs=true, pcsDiff=true, pcsRate=true}; " +
		GetCounters.CM_DESC__PROC_CACHE_LOAD         +"={queryTimeout=10, postpone=0,   paused=false, bg=false, resetNC20=true,  storePcs=true,  pcsAbs=true, pcsDiff=true, pcsRate=true}; " +
		GetCounters.CM_DESC__PROC_CALL_STACK         +"={queryTimeout=10, postpone=0,   paused=false, bg=false, resetNC20=true,  storePcs=false, pcsAbs=true, pcsDiff=true, pcsRate=true}; " +
		GetCounters.CM_DESC__CACHED_OBJECTS          +"={queryTimeout=30, postpone=600, paused=false, bg=false, resetNC20=false, storePcs=false, pcsAbs=true, pcsDiff=true, pcsRate=true}; " +
		GetCounters.CM_DESC__ERRORLOG                +"={queryTimeout=10, postpone=0,   paused=false, bg=false, resetNC20=true,  storePcs=false, pcsAbs=true, pcsDiff=false,pcsRate=false};" +
		GetCounters.CM_DESC__DEADLOCK                +"={queryTimeout=10, postpone=0,   paused=false, bg=false, resetNC20=true,  storePcs=false, pcsAbs=true, pcsDiff=false,pcsRate=false};" +
		GetCounters.CM_DESC__LOCK_TIMEOUT            +"={queryTimeout=10, postpone=0,   paused=false, bg=false, resetNC20=true,  storePcs=false, pcsAbs=true, pcsDiff=false,pcsRate=false};" +
		GetCounters.CM_DESC__PROC_CACHE_MODULE_USAGE +"={queryTimeout=10, postpone=0,   paused=false, bg=false, resetNC20=false, storePcs=false, pcsAbs=true, pcsDiff=true, pcsRate=true}; " +
		GetCounters.CM_DESC__PROC_CACHE_MEMORY_USAGE +"={queryTimeout=10, postpone=0,   paused=false, bg=false, resetNC20=false, storePcs=false, pcsAbs=true, pcsDiff=true, pcsRate=true}; " +
		GetCounters.CM_DESC__STATEMENT_CACHE         +"={queryTimeout=10, postpone=0,   paused=false, bg=false, resetNC20=false, storePcs=false, pcsAbs=true, pcsDiff=true, pcsRate=true}; " +
		GetCounters.CM_DESC__STATEMENT_CACHE_DETAILS +"={queryTimeout=30, postpone=0,   paused=false, bg=false, resetNC20=false, storePcs=false, pcsAbs=true, pcsDiff=true, pcsRate=true}; " +
		GetCounters.CM_DESC__ACTIVE_OBJECTS          +"={queryTimeout=10, postpone=0,   paused=false, bg=false, resetNC20=false, storePcs=false, pcsAbs=true, pcsDiff=true, pcsRate=true}; " +
		GetCounters.CM_DESC__ACTIVE_STATEMENTS       +"={queryTimeout=10, postpone=0,   paused=false, bg=false, resetNC20=true,  storePcs=false, pcsAbs=true, pcsDiff=true, pcsRate=true}; " +
		GetCounters.CM_DESC__BLOCKING                +"={queryTimeout=10, postpone=0,   paused=false, bg=false, resetNC20=false, storePcs=false, pcsAbs=true, pcsDiff=true, pcsRate=true}; " +
		GetCounters.CM_DESC__TABLE_COMPRESSION       +"={queryTimeout=10, postpone=0,   paused=false, bg=false, resetNC20=false, storePcs=false, pcsAbs=true, pcsDiff=true, pcsRate=true}; " +
		GetCounters.CM_DESC__MISSING_STATISTICS      +"={queryTimeout=30, postpone=0,   paused=false, bg=false, resetNC20=false, storePcs=false, pcsAbs=true, pcsDiff=true, pcsRate=true}; " +
		GetCounters.CM_DESC__QP_METRICS              +"={queryTimeout=30, postpone=60,  paused=false, bg=false, resetNC20=false, storePcs=false, pcsAbs=true, pcsDiff=true, pcsRate=true}; " +
		GetCounters.CM_DESC__SP_MONITOR_CONFIG       +"={queryTimeout=10, postpone=3600,paused=false, bg=false, resetNC20=false, storePcs=false, pcsAbs=true, pcsDiff=true, pcsRate=true}; " +
		GetCounters.CM_DESC__OS_IOSTAT               +"={queryTimeout=10, postpone=0,   paused=false, bg=false, resetNC20=false, storePcs=false, pcsAbs=true, pcsDiff=false,pcsRate=false};" +
		GetCounters.CM_DESC__OS_VMSTAT               +"={queryTimeout=10, postpone=0,   paused=false, bg=false, resetNC20=false, storePcs=false, pcsAbs=true, pcsDiff=false,pcsRate=false};" +
		GetCounters.CM_DESC__OS_MPSTAT               +"={queryTimeout=10, postpone=0,   paused=false, bg=false, resetNC20=false, storePcs=false, pcsAbs=true, pcsDiff=false,pcsRate=false};" +
		GetCounters.CM_DESC__OS_UPTIME               +"={queryTimeout=10, postpone=0,   paused=false, bg=false, resetNC20=false, storePcs=false, pcsAbs=true, pcsDiff=false,pcsRate=false};";

	public final static String systemTemplatePcsOnMedium =
		GetCounters.CM_DESC__OBJECT_ACTIVITY         +"={queryTimeout=10, postpone=0,   paused=false, bg=false, resetNC20=true,  storePcs=true,  pcsAbs=true, pcsDiff=true, pcsRate=true}; " +
		GetCounters.CM_DESC__PROCESS_ACTIVITY        +"={queryTimeout=10, postpone=0,   paused=false, bg=false, resetNC20=true,  storePcs=true,  pcsAbs=true, pcsDiff=true, pcsRate=true}; " +
		GetCounters.CM_DESC__PROCESS_WAIT            +"={queryTimeout=10, postpone=0,   paused=false, bg=false, resetNC20=true,  storePcs=false, pcsAbs=true, pcsDiff=true, pcsRate=true}; " +
		GetCounters.CM_DESC__OPEN_DATABASES          +"={queryTimeout=10, postpone=0,   paused=false, bg=false, resetNC20=true,  storePcs=true,  pcsAbs=true, pcsDiff=true, pcsRate=true}; " +
		GetCounters.CM_DESC__TEMPDB_ACTIVITY         +"={queryTimeout=10, postpone=0,   paused=false, bg=false, resetNC20=true,  storePcs=true,  pcsAbs=true, pcsDiff=true, pcsRate=true}; " +
		GetCounters.CM_DESC__SYS_WAIT                +"={queryTimeout=10, postpone=0,   paused=false, bg=false, resetNC20=true,  storePcs=true,  pcsAbs=true, pcsDiff=true, pcsRate=true}; " +
		GetCounters.CM_DESC__ENGINE                  +"={queryTimeout=10, postpone=0,   paused=false, bg=false, resetNC20=true,  storePcs=true,  pcsAbs=true, pcsDiff=true, pcsRate=true}; " +
		GetCounters.CM_DESC__SYS_LOAD                +"={queryTimeout=10, postpone=0,   paused=false, bg=false, resetNC20=false, storePcs=true,  pcsAbs=true, pcsDiff=true, pcsRate=true}; " +
		GetCounters.CM_DESC__DATA_CACHE              +"={queryTimeout=10, postpone=0,   paused=false, bg=false, resetNC20=true,  storePcs=true,  pcsAbs=true, pcsDiff=true, pcsRate=true}; " +
		GetCounters.CM_DESC__CACHE_POOL              +"={queryTimeout=10, postpone=0,   paused=false, bg=false, resetNC20=true,  storePcs=true,  pcsAbs=true, pcsDiff=true, pcsRate=true}; " +
		GetCounters.CM_DESC__DEVICE_IO               +"={queryTimeout=10, postpone=0,   paused=false, bg=false, resetNC20=true,  storePcs=true,  pcsAbs=true, pcsDiff=true, pcsRate=true}; " +
		GetCounters.CM_DESC__IO_QUEUE_SUM            +"={queryTimeout=10, postpone=0,   paused=false, bg=false, resetNC20=true,  storePcs=true,  pcsAbs=true, pcsDiff=true, pcsRate=true}; " +
		GetCounters.CM_DESC__IO_QUEUE                +"={queryTimeout=10, postpone=0,   paused=false, bg=false, resetNC20=true,  storePcs=true,  pcsAbs=true, pcsDiff=true, pcsRate=true}; " +
		GetCounters.CM_DESC__SPINLOCK_SUM            +"={queryTimeout=30, postpone=300, paused=false, bg=false, resetNC20=true,  storePcs=false, pcsAbs=true, pcsDiff=true, pcsRate=true}; " +
		GetCounters.CM_DESC__SYSMON                  +"={queryTimeout=30, postpone=300, paused=false, bg=false, resetNC20=true,  storePcs=false, pcsAbs=true, pcsDiff=true, pcsRate=true}; " +
		GetCounters.CM_DESC__RA_SENDERS              +"={queryTimeout=10, postpone=0,   paused=false, bg=false, resetNC20=true,  storePcs=false, pcsAbs=true, pcsDiff=true, pcsRate=true}; " +
		GetCounters.CM_DESC__RA_LOGACTIVITY          +"={queryTimeout=10, postpone=0,   paused=false, bg=false, resetNC20=true,  storePcs=false, pcsAbs=true, pcsDiff=true, pcsRate=true}; " +
		GetCounters.CM_DESC__RA_SCANNERS             +"={queryTimeout=10, postpone=0,   paused=false, bg=false, resetNC20=true,  storePcs=false, pcsAbs=true, pcsDiff=true, pcsRate=true}; " +
		GetCounters.CM_DESC__RA_SCANNERS_TIME        +"={queryTimeout=10, postpone=0,   paused=false, bg=false, resetNC20=true,  storePcs=false, pcsAbs=true, pcsDiff=true, pcsRate=true}; " +
		GetCounters.CM_DESC__RA_SQL_ACTIVITY         +"={queryTimeout=10, postpone=0,   paused=false, bg=false, resetNC20=true,  storePcs=false, pcsAbs=true, pcsDiff=true, pcsRate=true}; " +
		GetCounters.CM_DESC__RA_SQL_MISSES           +"={queryTimeout=10, postpone=0,   paused=false, bg=false, resetNC20=true,  storePcs=false, pcsAbs=true, pcsDiff=true, pcsRate=true}; " +
		GetCounters.CM_DESC__RA_SYSMON               +"={queryTimeout=30, postpone=300, paused=false, bg=false, resetNC20=true,  storePcs=false, pcsAbs=true, pcsDiff=true, pcsRate=true}; " +
		GetCounters.CM_DESC__CACHED_PROC             +"={queryTimeout=10, postpone=0,   paused=false, bg=false, resetNC20=true,  storePcs=false, pcsAbs=true, pcsDiff=true, pcsRate=true}; " +
		GetCounters.CM_DESC__PROC_CACHE_LOAD         +"={queryTimeout=10, postpone=0,   paused=false, bg=false, resetNC20=true,  storePcs=true,  pcsAbs=true, pcsDiff=true, pcsRate=true}; " +
		GetCounters.CM_DESC__PROC_CALL_STACK         +"={queryTimeout=10, postpone=0,   paused=false, bg=false, resetNC20=true,  storePcs=true,  pcsAbs=true, pcsDiff=true, pcsRate=true}; " +
		GetCounters.CM_DESC__CACHED_OBJECTS          +"={queryTimeout=30, postpone=600, paused=false, bg=false, resetNC20=false, storePcs=false, pcsAbs=true, pcsDiff=true, pcsRate=true}; " +
		GetCounters.CM_DESC__ERRORLOG                +"={queryTimeout=10, postpone=0,   paused=false, bg=false, resetNC20=true,  storePcs=false, pcsAbs=true, pcsDiff=false,pcsRate=false};" +
		GetCounters.CM_DESC__DEADLOCK                +"={queryTimeout=10, postpone=0,   paused=false, bg=false, resetNC20=true,  storePcs=false, pcsAbs=true, pcsDiff=false,pcsRate=false};" +
		GetCounters.CM_DESC__LOCK_TIMEOUT            +"={queryTimeout=10, postpone=0,   paused=false, bg=false, resetNC20=true,  storePcs=false, pcsAbs=true, pcsDiff=false,pcsRate=false};" +
		GetCounters.CM_DESC__PROC_CACHE_MODULE_USAGE +"={queryTimeout=10, postpone=0,   paused=false, bg=false, resetNC20=false, storePcs=false, pcsAbs=true, pcsDiff=true, pcsRate=true}; " +
		GetCounters.CM_DESC__PROC_CACHE_MEMORY_USAGE +"={queryTimeout=10, postpone=0,   paused=false, bg=false, resetNC20=false, storePcs=false, pcsAbs=true, pcsDiff=true, pcsRate=true}; " +
		GetCounters.CM_DESC__STATEMENT_CACHE         +"={queryTimeout=10, postpone=0,   paused=false, bg=false, resetNC20=false, storePcs=false, pcsAbs=true, pcsDiff=true, pcsRate=true}; " +
		GetCounters.CM_DESC__STATEMENT_CACHE_DETAILS +"={queryTimeout=30, postpone=0,   paused=false, bg=false, resetNC20=false, storePcs=false, pcsAbs=true, pcsDiff=true, pcsRate=true}; " +
		GetCounters.CM_DESC__ACTIVE_OBJECTS          +"={queryTimeout=10, postpone=0,   paused=false, bg=false, resetNC20=false, storePcs=false, pcsAbs=true, pcsDiff=true, pcsRate=true}; " +
		GetCounters.CM_DESC__ACTIVE_STATEMENTS       +"={queryTimeout=10, postpone=0,   paused=false, bg=false, resetNC20=true,  storePcs=true,  pcsAbs=true, pcsDiff=true, pcsRate=true}; " +
		GetCounters.CM_DESC__BLOCKING                +"={queryTimeout=10, postpone=0,   paused=false, bg=false, resetNC20=false, storePcs=false, pcsAbs=true, pcsDiff=true, pcsRate=true}; " +
		GetCounters.CM_DESC__TABLE_COMPRESSION       +"={queryTimeout=10, postpone=0,   paused=false, bg=false, resetNC20=false, storePcs=false, pcsAbs=true, pcsDiff=true, pcsRate=true}; " +
		GetCounters.CM_DESC__MISSING_STATISTICS      +"={queryTimeout=30, postpone=0,   paused=false, bg=false, resetNC20=false, storePcs=false, pcsAbs=true, pcsDiff=true, pcsRate=true}; " +
		GetCounters.CM_DESC__QP_METRICS              +"={queryTimeout=30, postpone=60,  paused=false, bg=false, resetNC20=false, storePcs=false, pcsAbs=true, pcsDiff=true, pcsRate=true}; " +
		GetCounters.CM_DESC__SP_MONITOR_CONFIG       +"={queryTimeout=10, postpone=3600,paused=false, bg=false, resetNC20=false, storePcs=true,  pcsAbs=true, pcsDiff=true, pcsRate=true}; " +
		GetCounters.CM_DESC__OS_IOSTAT               +"={queryTimeout=10, postpone=0,   paused=false, bg=false, resetNC20=false, storePcs=false, pcsAbs=true, pcsDiff=false,pcsRate=false};" +
		GetCounters.CM_DESC__OS_VMSTAT               +"={queryTimeout=10, postpone=0,   paused=false, bg=false, resetNC20=false, storePcs=false, pcsAbs=true, pcsDiff=false,pcsRate=false};" +
		GetCounters.CM_DESC__OS_MPSTAT               +"={queryTimeout=10, postpone=0,   paused=false, bg=false, resetNC20=false, storePcs=false, pcsAbs=true, pcsDiff=false,pcsRate=false};" +
		GetCounters.CM_DESC__OS_UPTIME               +"={queryTimeout=10, postpone=0,   paused=false, bg=false, resetNC20=false, storePcs=false, pcsAbs=true, pcsDiff=false,pcsRate=false};";

	public final static String systemTemplatePcsOnLarge =
		GetCounters.CM_DESC__OBJECT_ACTIVITY         +"={queryTimeout=10, postpone=0,   paused=false, bg=false, resetNC20=true,  storePcs=true,  pcsAbs=true, pcsDiff=true, pcsRate=true}; " +
		GetCounters.CM_DESC__PROCESS_ACTIVITY        +"={queryTimeout=10, postpone=0,   paused=false, bg=false, resetNC20=true,  storePcs=true,  pcsAbs=true, pcsDiff=true, pcsRate=true}; " +
		GetCounters.CM_DESC__PROCESS_WAIT            +"={queryTimeout=10, postpone=0,   paused=false, bg=false, resetNC20=true,  storePcs=false, pcsAbs=true, pcsDiff=true, pcsRate=true}; " +
		GetCounters.CM_DESC__OPEN_DATABASES          +"={queryTimeout=10, postpone=0,   paused=false, bg=false, resetNC20=true,  storePcs=true,  pcsAbs=true, pcsDiff=true, pcsRate=true}; " +
		GetCounters.CM_DESC__TEMPDB_ACTIVITY         +"={queryTimeout=10, postpone=0,   paused=false, bg=false, resetNC20=true,  storePcs=true,  pcsAbs=true, pcsDiff=true, pcsRate=true}; " +
		GetCounters.CM_DESC__SYS_WAIT                +"={queryTimeout=10, postpone=0,   paused=false, bg=false, resetNC20=true,  storePcs=true,  pcsAbs=true, pcsDiff=true, pcsRate=true}; " +
		GetCounters.CM_DESC__ENGINE                  +"={queryTimeout=10, postpone=0,   paused=false, bg=false, resetNC20=true,  storePcs=true,  pcsAbs=true, pcsDiff=true, pcsRate=true}; " +
		GetCounters.CM_DESC__SYS_LOAD                +"={queryTimeout=10, postpone=0,   paused=false, bg=false, resetNC20=false, storePcs=true,  pcsAbs=true, pcsDiff=true, pcsRate=true}; " +
		GetCounters.CM_DESC__DATA_CACHE              +"={queryTimeout=10, postpone=0,   paused=false, bg=false, resetNC20=true,  storePcs=true,  pcsAbs=true, pcsDiff=true, pcsRate=true}; " +
		GetCounters.CM_DESC__CACHE_POOL              +"={queryTimeout=10, postpone=0,   paused=false, bg=false, resetNC20=true,  storePcs=true,  pcsAbs=true, pcsDiff=true, pcsRate=true}; " +
		GetCounters.CM_DESC__DEVICE_IO               +"={queryTimeout=10, postpone=0,   paused=false, bg=false, resetNC20=true,  storePcs=true,  pcsAbs=true, pcsDiff=true, pcsRate=true}; " +
		GetCounters.CM_DESC__IO_QUEUE_SUM            +"={queryTimeout=10, postpone=0,   paused=false, bg=false, resetNC20=true,  storePcs=true,  pcsAbs=true, pcsDiff=true, pcsRate=true}; " +
		GetCounters.CM_DESC__IO_QUEUE                +"={queryTimeout=10, postpone=0,   paused=false, bg=false, resetNC20=true,  storePcs=true,  pcsAbs=true, pcsDiff=true, pcsRate=true}; " +
		GetCounters.CM_DESC__SPINLOCK_SUM            +"={queryTimeout=30, postpone=300, paused=false, bg=false, resetNC20=true,  storePcs=false, pcsAbs=true, pcsDiff=true, pcsRate=true}; " +
		GetCounters.CM_DESC__SYSMON                  +"={queryTimeout=30, postpone=300, paused=false, bg=false, resetNC20=true,  storePcs=false, pcsAbs=true, pcsDiff=true, pcsRate=true}; " +
		GetCounters.CM_DESC__RA_SENDERS              +"={queryTimeout=10, postpone=0,   paused=false, bg=false, resetNC20=true,  storePcs=false, pcsAbs=true, pcsDiff=true, pcsRate=true}; " +
		GetCounters.CM_DESC__RA_LOGACTIVITY          +"={queryTimeout=10, postpone=0,   paused=false, bg=false, resetNC20=true,  storePcs=false, pcsAbs=true, pcsDiff=true, pcsRate=true}; " +
		GetCounters.CM_DESC__RA_SCANNERS             +"={queryTimeout=10, postpone=0,   paused=false, bg=false, resetNC20=true,  storePcs=false, pcsAbs=true, pcsDiff=true, pcsRate=true}; " +
		GetCounters.CM_DESC__RA_SCANNERS_TIME        +"={queryTimeout=10, postpone=0,   paused=false, bg=false, resetNC20=true,  storePcs=false, pcsAbs=true, pcsDiff=true, pcsRate=true}; " +
		GetCounters.CM_DESC__RA_SQL_ACTIVITY         +"={queryTimeout=10, postpone=0,   paused=false, bg=false, resetNC20=true,  storePcs=false, pcsAbs=true, pcsDiff=true, pcsRate=true}; " +
		GetCounters.CM_DESC__RA_SQL_MISSES           +"={queryTimeout=10, postpone=0,   paused=false, bg=false, resetNC20=true,  storePcs=false, pcsAbs=true, pcsDiff=true, pcsRate=true}; " +
		GetCounters.CM_DESC__RA_SYSMON               +"={queryTimeout=30, postpone=300, paused=false, bg=false, resetNC20=true,  storePcs=false, pcsAbs=true, pcsDiff=true, pcsRate=true}; " +
		GetCounters.CM_DESC__CACHED_PROC             +"={queryTimeout=10, postpone=0,   paused=false, bg=false, resetNC20=true,  storePcs=false, pcsAbs=true, pcsDiff=true, pcsRate=true}; " +
		GetCounters.CM_DESC__PROC_CACHE_LOAD         +"={queryTimeout=10, postpone=0,   paused=false, bg=false, resetNC20=true,  storePcs=true,  pcsAbs=true, pcsDiff=true, pcsRate=true}; " +
		GetCounters.CM_DESC__PROC_CALL_STACK         +"={queryTimeout=10, postpone=0,   paused=false, bg=false, resetNC20=true,  storePcs=true,  pcsAbs=true, pcsDiff=true, pcsRate=true}; " +
		GetCounters.CM_DESC__CACHED_OBJECTS          +"={queryTimeout=30, postpone=600, paused=false, bg=false, resetNC20=false, storePcs=false, pcsAbs=true, pcsDiff=true, pcsRate=true}; " +
		GetCounters.CM_DESC__ERRORLOG                +"={queryTimeout=10, postpone=0,   paused=false, bg=false, resetNC20=true,  storePcs=false, pcsAbs=true, pcsDiff=false,pcsRate=false};" +
		GetCounters.CM_DESC__DEADLOCK                +"={queryTimeout=10, postpone=0,   paused=false, bg=false, resetNC20=true,  storePcs=false, pcsAbs=true, pcsDiff=false,pcsRate=false};" +
		GetCounters.CM_DESC__LOCK_TIMEOUT            +"={queryTimeout=10, postpone=0,   paused=false, bg=false, resetNC20=true,  storePcs=false, pcsAbs=true, pcsDiff=false,pcsRate=false};" +
		GetCounters.CM_DESC__PROC_CACHE_MODULE_USAGE +"={queryTimeout=10, postpone=0,   paused=false, bg=false, resetNC20=false, storePcs=true,  pcsAbs=true, pcsDiff=true, pcsRate=true}; " +
		GetCounters.CM_DESC__PROC_CACHE_MEMORY_USAGE +"={queryTimeout=10, postpone=0,   paused=false, bg=false, resetNC20=false, storePcs=true,  pcsAbs=true, pcsDiff=true, pcsRate=true}; " +
		GetCounters.CM_DESC__STATEMENT_CACHE         +"={queryTimeout=10, postpone=0,   paused=false, bg=false, resetNC20=false, storePcs=true,  pcsAbs=true, pcsDiff=true, pcsRate=true}; " +
		GetCounters.CM_DESC__STATEMENT_CACHE_DETAILS +"={queryTimeout=30, postpone=0,   paused=false, bg=false, resetNC20=false, storePcs=true,  pcsAbs=true, pcsDiff=true, pcsRate=true}; " +
		GetCounters.CM_DESC__ACTIVE_OBJECTS          +"={queryTimeout=10, postpone=0,   paused=false, bg=false, resetNC20=false, storePcs=true,  pcsAbs=true, pcsDiff=true, pcsRate=true}; " +
		GetCounters.CM_DESC__ACTIVE_STATEMENTS       +"={queryTimeout=10, postpone=0,   paused=false, bg=false, resetNC20=true,  storePcs=true,  pcsAbs=true, pcsDiff=true, pcsRate=true}; " +
		GetCounters.CM_DESC__BLOCKING                +"={queryTimeout=10, postpone=0,   paused=false, bg=false, resetNC20=false, storePcs=true,  pcsAbs=true, pcsDiff=true, pcsRate=true}; " +
		GetCounters.CM_DESC__TABLE_COMPRESSION       +"={queryTimeout=10, postpone=0,   paused=false, bg=false, resetNC20=false, storePcs=true,  pcsAbs=true, pcsDiff=true, pcsRate=true}; " +
		GetCounters.CM_DESC__MISSING_STATISTICS      +"={queryTimeout=30, postpone=0,   paused=false, bg=false, resetNC20=false, storePcs=true,  pcsAbs=true, pcsDiff=true, pcsRate=true}; " +
		GetCounters.CM_DESC__QP_METRICS              +"={queryTimeout=30, postpone=60,  paused=false, bg=false, resetNC20=false, storePcs=true,  pcsAbs=true, pcsDiff=true, pcsRate=true}; " +
		GetCounters.CM_DESC__SP_MONITOR_CONFIG       +"={queryTimeout=10, postpone=3600,paused=false, bg=false, resetNC20=false, storePcs=true,  pcsAbs=true, pcsDiff=true, pcsRate=true}; " +
		GetCounters.CM_DESC__OS_IOSTAT               +"={queryTimeout=10, postpone=0,   paused=false, bg=false, resetNC20=false, storePcs=true,  pcsAbs=true, pcsDiff=false,pcsRate=false};" +
		GetCounters.CM_DESC__OS_VMSTAT               +"={queryTimeout=10, postpone=0,   paused=false, bg=false, resetNC20=false, storePcs=true,  pcsAbs=true, pcsDiff=false,pcsRate=false};" +
		GetCounters.CM_DESC__OS_MPSTAT               +"={queryTimeout=10, postpone=0,   paused=false, bg=false, resetNC20=false, storePcs=true,  pcsAbs=true, pcsDiff=false,pcsRate=false};" +
		GetCounters.CM_DESC__OS_UPTIME               +"={queryTimeout=10, postpone=0,   paused=false, bg=false, resetNC20=false, storePcs=true,  pcsAbs=true, pcsDiff=false,pcsRate=false};";

	public final static String systemTemplatePcsOnAll =
		GetCounters.CM_DESC__OBJECT_ACTIVITY         +"={queryTimeout=10, postpone=0,   paused=false, bg=false, resetNC20=true,  storePcs=true,  pcsAbs=true, pcsDiff=true, pcsRate=true}; " +
		GetCounters.CM_DESC__PROCESS_ACTIVITY        +"={queryTimeout=10, postpone=0,   paused=false, bg=false, resetNC20=true,  storePcs=true,  pcsAbs=true, pcsDiff=true, pcsRate=true}; " +
		GetCounters.CM_DESC__PROCESS_WAIT            +"={queryTimeout=10, postpone=0,   paused=false, bg=false, resetNC20=true,  storePcs=true,  pcsAbs=true, pcsDiff=true, pcsRate=true}; " +
		GetCounters.CM_DESC__OPEN_DATABASES          +"={queryTimeout=10, postpone=0,   paused=false, bg=false, resetNC20=true,  storePcs=true,  pcsAbs=true, pcsDiff=true, pcsRate=true}; " +
		GetCounters.CM_DESC__TEMPDB_ACTIVITY         +"={queryTimeout=10, postpone=0,   paused=false, bg=false, resetNC20=true,  storePcs=true,  pcsAbs=true, pcsDiff=true, pcsRate=true}; " +
		GetCounters.CM_DESC__SYS_WAIT                +"={queryTimeout=10, postpone=0,   paused=false, bg=false, resetNC20=true,  storePcs=true,  pcsAbs=true, pcsDiff=true, pcsRate=true}; " +
		GetCounters.CM_DESC__ENGINE                  +"={queryTimeout=10, postpone=0,   paused=false, bg=false, resetNC20=true,  storePcs=true,  pcsAbs=true, pcsDiff=true, pcsRate=true}; " +
		GetCounters.CM_DESC__SYS_LOAD                +"={queryTimeout=10, postpone=0,   paused=false, bg=false, resetNC20=false, storePcs=true,  pcsAbs=true, pcsDiff=true, pcsRate=true}; " +
		GetCounters.CM_DESC__DATA_CACHE              +"={queryTimeout=10, postpone=0,   paused=false, bg=false, resetNC20=true,  storePcs=true,  pcsAbs=true, pcsDiff=true, pcsRate=true}; " +
		GetCounters.CM_DESC__CACHE_POOL              +"={queryTimeout=10, postpone=0,   paused=false, bg=false, resetNC20=true,  storePcs=true,  pcsAbs=true, pcsDiff=true, pcsRate=true}; " +
		GetCounters.CM_DESC__DEVICE_IO               +"={queryTimeout=10, postpone=0,   paused=false, bg=false, resetNC20=true,  storePcs=true,  pcsAbs=true, pcsDiff=true, pcsRate=true}; " +
		GetCounters.CM_DESC__IO_QUEUE_SUM            +"={queryTimeout=10, postpone=0,   paused=false, bg=false, resetNC20=true,  storePcs=true,  pcsAbs=true, pcsDiff=true, pcsRate=true}; " +
		GetCounters.CM_DESC__IO_QUEUE                +"={queryTimeout=10, postpone=0,   paused=false, bg=false, resetNC20=true,  storePcs=true,  pcsAbs=true, pcsDiff=true, pcsRate=true}; " +
		GetCounters.CM_DESC__SPINLOCK_SUM            +"={queryTimeout=30, postpone=300, paused=false, bg=false, resetNC20=true,  storePcs=true,  pcsAbs=true, pcsDiff=true, pcsRate=true}; " +
		GetCounters.CM_DESC__SYSMON                  +"={queryTimeout=30, postpone=300, paused=false, bg=false, resetNC20=true,  storePcs=true,  pcsAbs=true, pcsDiff=true, pcsRate=true}; " +
		GetCounters.CM_DESC__RA_SENDERS              +"={queryTimeout=10, postpone=0,   paused=false, bg=false, resetNC20=true,  storePcs=true,  pcsAbs=true, pcsDiff=true, pcsRate=true}; " +
		GetCounters.CM_DESC__RA_LOGACTIVITY          +"={queryTimeout=10, postpone=0,   paused=false, bg=false, resetNC20=true,  storePcs=true,  pcsAbs=true, pcsDiff=true, pcsRate=true}; " +
		GetCounters.CM_DESC__RA_SCANNERS             +"={queryTimeout=10, postpone=0,   paused=false, bg=false, resetNC20=true,  storePcs=true,  pcsAbs=true, pcsDiff=true, pcsRate=true}; " +
		GetCounters.CM_DESC__RA_SCANNERS_TIME        +"={queryTimeout=10, postpone=0,   paused=false, bg=false, resetNC20=true,  storePcs=true,  pcsAbs=true, pcsDiff=true, pcsRate=true}; " +
		GetCounters.CM_DESC__RA_SQL_ACTIVITY         +"={queryTimeout=10, postpone=0,   paused=false, bg=false, resetNC20=true,  storePcs=true,  pcsAbs=true, pcsDiff=true, pcsRate=true}; " +
		GetCounters.CM_DESC__RA_SQL_MISSES           +"={queryTimeout=10, postpone=0,   paused=false, bg=false, resetNC20=true,  storePcs=true,  pcsAbs=true, pcsDiff=true, pcsRate=true}; " +
		GetCounters.CM_DESC__RA_SYSMON               +"={queryTimeout=30, postpone=300, paused=false, bg=false, resetNC20=true,  storePcs=true,  pcsAbs=true, pcsDiff=true, pcsRate=true}; " +
		GetCounters.CM_DESC__CACHED_PROC             +"={queryTimeout=10, postpone=0,   paused=false, bg=false, resetNC20=true,  storePcs=true,  pcsAbs=true, pcsDiff=true, pcsRate=true}; " +
		GetCounters.CM_DESC__PROC_CACHE_LOAD         +"={queryTimeout=10, postpone=0,   paused=false, bg=false, resetNC20=true,  storePcs=true,  pcsAbs=true, pcsDiff=true, pcsRate=true}; " +
		GetCounters.CM_DESC__PROC_CALL_STACK         +"={queryTimeout=10, postpone=0,   paused=false, bg=false, resetNC20=true,  storePcs=true,  pcsAbs=true, pcsDiff=true, pcsRate=true}; " +
		GetCounters.CM_DESC__CACHED_OBJECTS          +"={queryTimeout=30, postpone=600, paused=false, bg=false, resetNC20=false, storePcs=true,  pcsAbs=true, pcsDiff=true, pcsRate=true}; " +
		GetCounters.CM_DESC__ERRORLOG                +"={queryTimeout=10, postpone=0,   paused=false, bg=false, resetNC20=true,  storePcs=false, pcsAbs=true, pcsDiff=false,pcsRate=false};" +
		GetCounters.CM_DESC__DEADLOCK                +"={queryTimeout=10, postpone=0,   paused=false, bg=false, resetNC20=true,  storePcs=false, pcsAbs=true, pcsDiff=false,pcsRate=false};" +
		GetCounters.CM_DESC__LOCK_TIMEOUT            +"={queryTimeout=10, postpone=0,   paused=false, bg=false, resetNC20=true,  storePcs=false, pcsAbs=true, pcsDiff=false,pcsRate=false};" +
		GetCounters.CM_DESC__PROC_CACHE_MODULE_USAGE +"={queryTimeout=10, postpone=0,   paused=false, bg=false, resetNC20=false, storePcs=true,  pcsAbs=true, pcsDiff=true, pcsRate=true}; " +
		GetCounters.CM_DESC__PROC_CACHE_MEMORY_USAGE +"={queryTimeout=10, postpone=0,   paused=false, bg=false, resetNC20=false, storePcs=true,  pcsAbs=true, pcsDiff=true, pcsRate=true}; " +
		GetCounters.CM_DESC__STATEMENT_CACHE         +"={queryTimeout=10, postpone=0,   paused=false, bg=false, resetNC20=false, storePcs=true,  pcsAbs=true, pcsDiff=true, pcsRate=true}; " +
		GetCounters.CM_DESC__STATEMENT_CACHE_DETAILS +"={queryTimeout=30, postpone=0,   paused=false, bg=false, resetNC20=false, storePcs=true,  pcsAbs=true, pcsDiff=true, pcsRate=true}; " +
		GetCounters.CM_DESC__ACTIVE_OBJECTS          +"={queryTimeout=10, postpone=0,   paused=false, bg=false, resetNC20=false, storePcs=true,  pcsAbs=true, pcsDiff=true, pcsRate=true}; " +
		GetCounters.CM_DESC__ACTIVE_STATEMENTS       +"={queryTimeout=10, postpone=0,   paused=false, bg=false, resetNC20=true,  storePcs=true,  pcsAbs=true, pcsDiff=true, pcsRate=true}; " +
		GetCounters.CM_DESC__BLOCKING                +"={queryTimeout=10, postpone=0,   paused=false, bg=false, resetNC20=false, storePcs=true,  pcsAbs=true, pcsDiff=true, pcsRate=true}; " +
		GetCounters.CM_DESC__TABLE_COMPRESSION       +"={queryTimeout=10, postpone=0,   paused=false, bg=false, resetNC20=false, storePcs=true,  pcsAbs=true, pcsDiff=true, pcsRate=true}; " +
		GetCounters.CM_DESC__MISSING_STATISTICS      +"={queryTimeout=30, postpone=0,   paused=false, bg=false, resetNC20=false, storePcs=true,  pcsAbs=true, pcsDiff=true, pcsRate=true}; " +
		GetCounters.CM_DESC__QP_METRICS              +"={queryTimeout=30, postpone=60,  paused=false, bg=false, resetNC20=false, storePcs=true,  pcsAbs=true, pcsDiff=true, pcsRate=true}; " +
		GetCounters.CM_DESC__SP_MONITOR_CONFIG       +"={queryTimeout=10, postpone=3600,paused=false, bg=false, resetNC20=false, storePcs=true,  pcsAbs=true, pcsDiff=true, pcsRate=true}; " +
		GetCounters.CM_DESC__OS_IOSTAT               +"={queryTimeout=10, postpone=0,   paused=false, bg=false, resetNC20=false, storePcs=true,  pcsAbs=true, pcsDiff=false,pcsRate=false};" +
		GetCounters.CM_DESC__OS_VMSTAT               +"={queryTimeout=10, postpone=0,   paused=false, bg=false, resetNC20=false, storePcs=true,  pcsAbs=true, pcsDiff=false,pcsRate=false};" +
		GetCounters.CM_DESC__OS_MPSTAT               +"={queryTimeout=10, postpone=0,   paused=false, bg=false, resetNC20=false, storePcs=true,  pcsAbs=true, pcsDiff=false,pcsRate=false};" +
		GetCounters.CM_DESC__OS_UPTIME               +"={queryTimeout=10, postpone=0,   paused=false, bg=false, resetNC20=false, storePcs=true,  pcsAbs=true, pcsDiff=false,pcsRate=false};";



	//----------------------------------------------------------------
	// PCS OFF
	//----------------------------------------------------------------

	public final static String systemTemplatePcsOffSmall =
		GetCounters.CM_DESC__OBJECT_ACTIVITY         +"={queryTimeout=10, postpone=0,   paused=false, bg=false, resetNC20=true,  storePcs=false, pcsAbs=true, pcsDiff=true, pcsRate=true}; " +
		GetCounters.CM_DESC__PROCESS_ACTIVITY        +"={queryTimeout=10, postpone=0,   paused=false, bg=false, resetNC20=true,  storePcs=false, pcsAbs=true, pcsDiff=true, pcsRate=true}; " +
		GetCounters.CM_DESC__PROCESS_WAIT            +"={queryTimeout=10, postpone=0,   paused=false, bg=false, resetNC20=true,  storePcs=false, pcsAbs=true, pcsDiff=true, pcsRate=true}; " +
		GetCounters.CM_DESC__OPEN_DATABASES          +"={queryTimeout=10, postpone=0,   paused=false, bg=false, resetNC20=true,  storePcs=false, pcsAbs=true, pcsDiff=true, pcsRate=true}; " +
		GetCounters.CM_DESC__TEMPDB_ACTIVITY         +"={queryTimeout=10, postpone=0,   paused=false, bg=false, resetNC20=true,  storePcs=false, pcsAbs=true, pcsDiff=true, pcsRate=true}; " +
		GetCounters.CM_DESC__SYS_WAIT                +"={queryTimeout=10, postpone=0,   paused=false, bg=false, resetNC20=true,  storePcs=false, pcsAbs=true, pcsDiff=true, pcsRate=true}; " +
		GetCounters.CM_DESC__ENGINE                  +"={queryTimeout=10, postpone=0,   paused=false, bg=false, resetNC20=true,  storePcs=false, pcsAbs=true, pcsDiff=true, pcsRate=true}; " +
		GetCounters.CM_DESC__SYS_LOAD                +"={queryTimeout=10, postpone=0,   paused=false, bg=false, resetNC20=false, storePcs=false, pcsAbs=true, pcsDiff=true, pcsRate=true}; " +
		GetCounters.CM_DESC__DATA_CACHE              +"={queryTimeout=10, postpone=0,   paused=false, bg=false, resetNC20=true,  storePcs=false, pcsAbs=true, pcsDiff=true, pcsRate=true}; " +
		GetCounters.CM_DESC__CACHE_POOL              +"={queryTimeout=10, postpone=0,   paused=false, bg=false, resetNC20=true,  storePcs=false, pcsAbs=true, pcsDiff=true, pcsRate=true}; " +
		GetCounters.CM_DESC__DEVICE_IO               +"={queryTimeout=10, postpone=0,   paused=false, bg=false, resetNC20=true,  storePcs=false, pcsAbs=true, pcsDiff=true, pcsRate=true}; " +
		GetCounters.CM_DESC__IO_QUEUE_SUM            +"={queryTimeout=10, postpone=0,   paused=false, bg=false, resetNC20=true,  storePcs=false, pcsAbs=true, pcsDiff=true, pcsRate=true}; " +
		GetCounters.CM_DESC__IO_QUEUE                +"={queryTimeout=10, postpone=0,   paused=false, bg=false, resetNC20=true,  storePcs=false, pcsAbs=true, pcsDiff=true, pcsRate=true}; " +
		GetCounters.CM_DESC__SPINLOCK_SUM            +"={queryTimeout=30, postpone=300, paused=false, bg=false, resetNC20=true,  storePcs=false, pcsAbs=true, pcsDiff=true, pcsRate=true}; " +
		GetCounters.CM_DESC__SYSMON                  +"={queryTimeout=30, postpone=300, paused=false, bg=false, resetNC20=true,  storePcs=false, pcsAbs=true, pcsDiff=true, pcsRate=true}; " +
		GetCounters.CM_DESC__RA_SENDERS              +"={queryTimeout=10, postpone=0,   paused=false, bg=false, resetNC20=true,  storePcs=false, pcsAbs=true, pcsDiff=true, pcsRate=true}; " +
		GetCounters.CM_DESC__RA_LOGACTIVITY          +"={queryTimeout=10, postpone=0,   paused=false, bg=false, resetNC20=true,  storePcs=false, pcsAbs=true, pcsDiff=true, pcsRate=true}; " +
		GetCounters.CM_DESC__RA_SCANNERS             +"={queryTimeout=10, postpone=0,   paused=false, bg=false, resetNC20=true,  storePcs=false, pcsAbs=true, pcsDiff=true, pcsRate=true}; " +
		GetCounters.CM_DESC__RA_SCANNERS_TIME        +"={queryTimeout=10, postpone=0,   paused=false, bg=false, resetNC20=true,  storePcs=false, pcsAbs=true, pcsDiff=true, pcsRate=true}; " +
		GetCounters.CM_DESC__RA_SQL_ACTIVITY         +"={queryTimeout=10, postpone=0,   paused=false, bg=false, resetNC20=true,  storePcs=false, pcsAbs=true, pcsDiff=true, pcsRate=true}; " +
		GetCounters.CM_DESC__RA_SQL_MISSES           +"={queryTimeout=10, postpone=0,   paused=false, bg=false, resetNC20=true,  storePcs=false, pcsAbs=true, pcsDiff=true, pcsRate=true}; " +
		GetCounters.CM_DESC__RA_SYSMON               +"={queryTimeout=30, postpone=300, paused=false, bg=false, resetNC20=true,  storePcs=false, pcsAbs=true, pcsDiff=true, pcsRate=true}; " +
		GetCounters.CM_DESC__CACHED_PROC             +"={queryTimeout=10, postpone=0,   paused=false, bg=false, resetNC20=true,  storePcs=false, pcsAbs=true, pcsDiff=true, pcsRate=true}; " +
		GetCounters.CM_DESC__PROC_CACHE_LOAD         +"={queryTimeout=10, postpone=0,   paused=false, bg=false, resetNC20=true,  storePcs=false, pcsAbs=true, pcsDiff=true, pcsRate=true}; " +
		GetCounters.CM_DESC__PROC_CALL_STACK         +"={queryTimeout=10, postpone=0,   paused=false, bg=false, resetNC20=true,  storePcs=false, pcsAbs=true, pcsDiff=true, pcsRate=true}; " +
		GetCounters.CM_DESC__CACHED_OBJECTS          +"={queryTimeout=30, postpone=600, paused=false, bg=false, resetNC20=false, storePcs=false, pcsAbs=true, pcsDiff=true, pcsRate=true}; " +
		GetCounters.CM_DESC__ERRORLOG                +"={queryTimeout=10, postpone=0,   paused=false, bg=false, resetNC20=true,  storePcs=false, pcsAbs=true, pcsDiff=false,pcsRate=false};" +
		GetCounters.CM_DESC__DEADLOCK                +"={queryTimeout=10, postpone=0,   paused=false, bg=false, resetNC20=true,  storePcs=false, pcsAbs=true, pcsDiff=false,pcsRate=false};" +
		GetCounters.CM_DESC__LOCK_TIMEOUT            +"={queryTimeout=10, postpone=0,   paused=false, bg=false, resetNC20=true,  storePcs=false, pcsAbs=true, pcsDiff=false,pcsRate=false};" +
		GetCounters.CM_DESC__PROC_CACHE_MODULE_USAGE +"={queryTimeout=10, postpone=0,   paused=false, bg=false, resetNC20=false, storePcs=false, pcsAbs=true, pcsDiff=true, pcsRate=true}; " +
		GetCounters.CM_DESC__PROC_CACHE_MEMORY_USAGE +"={queryTimeout=10, postpone=0,   paused=false, bg=false, resetNC20=false, storePcs=false, pcsAbs=true, pcsDiff=true, pcsRate=true}; " +
		GetCounters.CM_DESC__STATEMENT_CACHE         +"={queryTimeout=10, postpone=0,   paused=false, bg=false, resetNC20=false, storePcs=false, pcsAbs=true, pcsDiff=true, pcsRate=true}; " +
		GetCounters.CM_DESC__STATEMENT_CACHE_DETAILS +"={queryTimeout=30, postpone=0,   paused=false, bg=false, resetNC20=false, storePcs=false, pcsAbs=true, pcsDiff=true, pcsRate=true}; " +
		GetCounters.CM_DESC__ACTIVE_OBJECTS          +"={queryTimeout=10, postpone=0,   paused=false, bg=false, resetNC20=false, storePcs=false, pcsAbs=true, pcsDiff=true, pcsRate=true}; " +
		GetCounters.CM_DESC__ACTIVE_STATEMENTS       +"={queryTimeout=10, postpone=0,   paused=false, bg=false, resetNC20=true,  storePcs=false, pcsAbs=true, pcsDiff=true, pcsRate=true}; " +
		GetCounters.CM_DESC__BLOCKING                +"={queryTimeout=10, postpone=0,   paused=false, bg=false, resetNC20=false, storePcs=false, pcsAbs=true, pcsDiff=true, pcsRate=true}; " +
		GetCounters.CM_DESC__TABLE_COMPRESSION       +"={queryTimeout=10, postpone=0,   paused=false, bg=false, resetNC20=false, storePcs=false, pcsAbs=true, pcsDiff=true, pcsRate=true}; " +
		GetCounters.CM_DESC__MISSING_STATISTICS      +"={queryTimeout=30, postpone=0,   paused=false, bg=false, resetNC20=false, storePcs=false, pcsAbs=true, pcsDiff=true, pcsRate=true}; " +
		GetCounters.CM_DESC__QP_METRICS              +"={queryTimeout=30, postpone=60,  paused=false, bg=false, resetNC20=false, storePcs=false, pcsAbs=true, pcsDiff=true, pcsRate=true}; " +
		GetCounters.CM_DESC__SP_MONITOR_CONFIG       +"={queryTimeout=10, postpone=3600,paused=false, bg=false, resetNC20=false, storePcs=false, pcsAbs=true, pcsDiff=true, pcsRate=true}; " +
		GetCounters.CM_DESC__OS_IOSTAT               +"={queryTimeout=10, postpone=0,   paused=false, bg=false, resetNC20=false, storePcs=false, pcsAbs=true, pcsDiff=false,pcsRate=false};" +
		GetCounters.CM_DESC__OS_VMSTAT               +"={queryTimeout=10, postpone=0,   paused=false, bg=false, resetNC20=false, storePcs=false, pcsAbs=true, pcsDiff=false,pcsRate=false};" +
		GetCounters.CM_DESC__OS_MPSTAT               +"={queryTimeout=10, postpone=0,   paused=false, bg=false, resetNC20=false, storePcs=false, pcsAbs=true, pcsDiff=false,pcsRate=false};" +
		GetCounters.CM_DESC__OS_UPTIME               +"={queryTimeout=10, postpone=0,   paused=false, bg=false, resetNC20=false, storePcs=false, pcsAbs=true, pcsDiff=false,pcsRate=false};";

	public final static String systemTemplatePcsOffMedium =
		GetCounters.CM_DESC__OBJECT_ACTIVITY         +"={queryTimeout=10, postpone=0,   paused=false, bg=false, resetNC20=true,  storePcs=false, pcsAbs=true, pcsDiff=true, pcsRate=true}; " +
		GetCounters.CM_DESC__PROCESS_ACTIVITY        +"={queryTimeout=10, postpone=0,   paused=false, bg=false, resetNC20=true,  storePcs=false, pcsAbs=true, pcsDiff=true, pcsRate=true}; " +
		GetCounters.CM_DESC__PROCESS_WAIT            +"={queryTimeout=10, postpone=0,   paused=false, bg=false, resetNC20=true,  storePcs=false, pcsAbs=true, pcsDiff=true, pcsRate=true}; " +
		GetCounters.CM_DESC__OPEN_DATABASES          +"={queryTimeout=10, postpone=0,   paused=false, bg=false, resetNC20=true,  storePcs=false, pcsAbs=true, pcsDiff=true, pcsRate=true}; " +
		GetCounters.CM_DESC__TEMPDB_ACTIVITY         +"={queryTimeout=10, postpone=0,   paused=false, bg=false, resetNC20=true,  storePcs=false, pcsAbs=true, pcsDiff=true, pcsRate=true}; " +
		GetCounters.CM_DESC__SYS_WAIT                +"={queryTimeout=10, postpone=0,   paused=false, bg=false, resetNC20=true,  storePcs=false, pcsAbs=true, pcsDiff=true, pcsRate=true}; " +
		GetCounters.CM_DESC__ENGINE                  +"={queryTimeout=10, postpone=0,   paused=false, bg=false, resetNC20=true,  storePcs=false, pcsAbs=true, pcsDiff=true, pcsRate=true}; " +
		GetCounters.CM_DESC__SYS_LOAD                +"={queryTimeout=10, postpone=0,   paused=false, bg=false, resetNC20=false, storePcs=false, pcsAbs=true, pcsDiff=true, pcsRate=true}; " +
		GetCounters.CM_DESC__DATA_CACHE              +"={queryTimeout=10, postpone=0,   paused=false, bg=false, resetNC20=true,  storePcs=false, pcsAbs=true, pcsDiff=true, pcsRate=true}; " +
		GetCounters.CM_DESC__CACHE_POOL              +"={queryTimeout=10, postpone=0,   paused=false, bg=false, resetNC20=true,  storePcs=false, pcsAbs=true, pcsDiff=true, pcsRate=true}; " +
		GetCounters.CM_DESC__DEVICE_IO               +"={queryTimeout=10, postpone=0,   paused=false, bg=false, resetNC20=true,  storePcs=false, pcsAbs=true, pcsDiff=true, pcsRate=true}; " +
		GetCounters.CM_DESC__IO_QUEUE_SUM            +"={queryTimeout=10, postpone=0,   paused=false, bg=false, resetNC20=true,  storePcs=false, pcsAbs=true, pcsDiff=true, pcsRate=true}; " +
		GetCounters.CM_DESC__IO_QUEUE                +"={queryTimeout=10, postpone=0,   paused=false, bg=false, resetNC20=true,  storePcs=false, pcsAbs=true, pcsDiff=true, pcsRate=true}; " +
		GetCounters.CM_DESC__SPINLOCK_SUM            +"={queryTimeout=30, postpone=300, paused=false, bg=false, resetNC20=true,  storePcs=false, pcsAbs=true, pcsDiff=true, pcsRate=true}; " +
		GetCounters.CM_DESC__SYSMON                  +"={queryTimeout=30, postpone=300, paused=false, bg=false, resetNC20=true,  storePcs=false, pcsAbs=true, pcsDiff=true, pcsRate=true}; " +
		GetCounters.CM_DESC__RA_SENDERS              +"={queryTimeout=10, postpone=0,   paused=false, bg=false, resetNC20=true,  storePcs=false, pcsAbs=true, pcsDiff=true, pcsRate=true}; " +
		GetCounters.CM_DESC__RA_LOGACTIVITY          +"={queryTimeout=10, postpone=0,   paused=false, bg=false, resetNC20=true,  storePcs=false, pcsAbs=true, pcsDiff=true, pcsRate=true}; " +
		GetCounters.CM_DESC__RA_SCANNERS             +"={queryTimeout=10, postpone=0,   paused=false, bg=false, resetNC20=true,  storePcs=false, pcsAbs=true, pcsDiff=true, pcsRate=true}; " +
		GetCounters.CM_DESC__RA_SCANNERS_TIME        +"={queryTimeout=10, postpone=0,   paused=false, bg=false, resetNC20=true,  storePcs=false, pcsAbs=true, pcsDiff=true, pcsRate=true}; " +
		GetCounters.CM_DESC__RA_SQL_ACTIVITY         +"={queryTimeout=10, postpone=0,   paused=false, bg=false, resetNC20=true,  storePcs=false, pcsAbs=true, pcsDiff=true, pcsRate=true}; " +
		GetCounters.CM_DESC__RA_SQL_MISSES           +"={queryTimeout=10, postpone=0,   paused=false, bg=false, resetNC20=true,  storePcs=false, pcsAbs=true, pcsDiff=true, pcsRate=true}; " +
		GetCounters.CM_DESC__RA_SYSMON               +"={queryTimeout=30, postpone=300, paused=false, bg=false, resetNC20=true,  storePcs=false, pcsAbs=true, pcsDiff=true, pcsRate=true}; " +
		GetCounters.CM_DESC__CACHED_PROC             +"={queryTimeout=10, postpone=0,   paused=false, bg=false, resetNC20=true,  storePcs=false, pcsAbs=true, pcsDiff=true, pcsRate=true}; " +
		GetCounters.CM_DESC__PROC_CACHE_LOAD         +"={queryTimeout=10, postpone=0,   paused=false, bg=false, resetNC20=true,  storePcs=false, pcsAbs=true, pcsDiff=true, pcsRate=true}; " +
		GetCounters.CM_DESC__PROC_CALL_STACK         +"={queryTimeout=10, postpone=0,   paused=false, bg=false, resetNC20=true,  storePcs=false, pcsAbs=true, pcsDiff=true, pcsRate=true}; " +
		GetCounters.CM_DESC__CACHED_OBJECTS          +"={queryTimeout=30, postpone=600, paused=false, bg=false, resetNC20=false, storePcs=false, pcsAbs=true, pcsDiff=true, pcsRate=true}; " +
		GetCounters.CM_DESC__ERRORLOG                +"={queryTimeout=10, postpone=0,   paused=false, bg=false, resetNC20=true,  storePcs=false, pcsAbs=true, pcsDiff=false,pcsRate=false};" +
		GetCounters.CM_DESC__DEADLOCK                +"={queryTimeout=10, postpone=0,   paused=false, bg=false, resetNC20=true,  storePcs=false, pcsAbs=true, pcsDiff=false,pcsRate=false};" +
		GetCounters.CM_DESC__LOCK_TIMEOUT            +"={queryTimeout=10, postpone=0,   paused=false, bg=false, resetNC20=true,  storePcs=false, pcsAbs=true, pcsDiff=false,pcsRate=false};" +
		GetCounters.CM_DESC__PROC_CACHE_MODULE_USAGE +"={queryTimeout=10, postpone=0,   paused=false, bg=false, resetNC20=false, storePcs=false, pcsAbs=true, pcsDiff=true, pcsRate=true}; " +
		GetCounters.CM_DESC__PROC_CACHE_MEMORY_USAGE +"={queryTimeout=10, postpone=0,   paused=false, bg=false, resetNC20=false, storePcs=false, pcsAbs=true, pcsDiff=true, pcsRate=true}; " +
		GetCounters.CM_DESC__STATEMENT_CACHE         +"={queryTimeout=10, postpone=0,   paused=false, bg=false, resetNC20=false, storePcs=false, pcsAbs=true, pcsDiff=true, pcsRate=true}; " +
		GetCounters.CM_DESC__STATEMENT_CACHE_DETAILS +"={queryTimeout=30, postpone=0,   paused=false, bg=false, resetNC20=false, storePcs=false, pcsAbs=true, pcsDiff=true, pcsRate=true}; " +
		GetCounters.CM_DESC__ACTIVE_OBJECTS          +"={queryTimeout=10, postpone=0,   paused=false, bg=false, resetNC20=false, storePcs=false, pcsAbs=true, pcsDiff=true, pcsRate=true}; " +
		GetCounters.CM_DESC__ACTIVE_STATEMENTS       +"={queryTimeout=10, postpone=0,   paused=false, bg=false, resetNC20=true,  storePcs=false, pcsAbs=true, pcsDiff=true, pcsRate=true}; " +
		GetCounters.CM_DESC__BLOCKING                +"={queryTimeout=10, postpone=0,   paused=false, bg=false, resetNC20=false, storePcs=false, pcsAbs=true, pcsDiff=true, pcsRate=true}; " +
		GetCounters.CM_DESC__TABLE_COMPRESSION       +"={queryTimeout=10, postpone=0,   paused=false, bg=false, resetNC20=false, storePcs=false, pcsAbs=true, pcsDiff=true, pcsRate=true}; " +
		GetCounters.CM_DESC__MISSING_STATISTICS      +"={queryTimeout=30, postpone=0,   paused=false, bg=false, resetNC20=false, storePcs=false, pcsAbs=true, pcsDiff=true, pcsRate=true}; " +
		GetCounters.CM_DESC__QP_METRICS              +"={queryTimeout=30, postpone=60,  paused=false, bg=false, resetNC20=false, storePcs=false, pcsAbs=true, pcsDiff=true, pcsRate=true}; " +
		GetCounters.CM_DESC__SP_MONITOR_CONFIG       +"={queryTimeout=10, postpone=3600,paused=false, bg=false, resetNC20=false, storePcs=false, pcsAbs=true, pcsDiff=true, pcsRate=true}; " +
		GetCounters.CM_DESC__OS_IOSTAT               +"={queryTimeout=10, postpone=0,   paused=false, bg=false, resetNC20=false, storePcs=false, pcsAbs=true, pcsDiff=false,pcsRate=false};" +
		GetCounters.CM_DESC__OS_VMSTAT               +"={queryTimeout=10, postpone=0,   paused=false, bg=false, resetNC20=false, storePcs=false, pcsAbs=true, pcsDiff=false,pcsRate=false};" +
		GetCounters.CM_DESC__OS_MPSTAT               +"={queryTimeout=10, postpone=0,   paused=false, bg=false, resetNC20=false, storePcs=false, pcsAbs=true, pcsDiff=false,pcsRate=false};" +
		GetCounters.CM_DESC__OS_UPTIME               +"={queryTimeout=10, postpone=0,   paused=false, bg=false, resetNC20=false, storePcs=false, pcsAbs=true, pcsDiff=false,pcsRate=false};";

	public final static String systemTemplatePcsOffLarge =
		GetCounters.CM_DESC__OBJECT_ACTIVITY         +"={queryTimeout=10, postpone=0,   paused=false, bg=false, resetNC20=true,  storePcs=false, pcsAbs=true, pcsDiff=true, pcsRate=true}; " +
		GetCounters.CM_DESC__PROCESS_ACTIVITY        +"={queryTimeout=10, postpone=0,   paused=false, bg=false, resetNC20=true,  storePcs=false, pcsAbs=true, pcsDiff=true, pcsRate=true}; " +
		GetCounters.CM_DESC__PROCESS_WAIT            +"={queryTimeout=10, postpone=0,   paused=false, bg=false, resetNC20=true,  storePcs=false, pcsAbs=true, pcsDiff=true, pcsRate=true}; " +
		GetCounters.CM_DESC__OPEN_DATABASES          +"={queryTimeout=10, postpone=0,   paused=false, bg=false, resetNC20=true,  storePcs=false, pcsAbs=true, pcsDiff=true, pcsRate=true}; " +
		GetCounters.CM_DESC__TEMPDB_ACTIVITY         +"={queryTimeout=10, postpone=0,   paused=false, bg=false, resetNC20=true,  storePcs=false, pcsAbs=true, pcsDiff=true, pcsRate=true}; " +
		GetCounters.CM_DESC__SYS_WAIT                +"={queryTimeout=10, postpone=0,   paused=false, bg=false, resetNC20=true,  storePcs=false, pcsAbs=true, pcsDiff=true, pcsRate=true}; " +
		GetCounters.CM_DESC__ENGINE                  +"={queryTimeout=10, postpone=0,   paused=false, bg=false, resetNC20=true,  storePcs=false, pcsAbs=true, pcsDiff=true, pcsRate=true}; " +
		GetCounters.CM_DESC__SYS_LOAD                +"={queryTimeout=10, postpone=0,   paused=false, bg=false, resetNC20=false, storePcs=false, pcsAbs=true, pcsDiff=true, pcsRate=true}; " +
		GetCounters.CM_DESC__DATA_CACHE              +"={queryTimeout=10, postpone=0,   paused=false, bg=false, resetNC20=true,  storePcs=false, pcsAbs=true, pcsDiff=true, pcsRate=true}; " +
		GetCounters.CM_DESC__CACHE_POOL              +"={queryTimeout=10, postpone=0,   paused=false, bg=false, resetNC20=true,  storePcs=false, pcsAbs=true, pcsDiff=true, pcsRate=true}; " +
		GetCounters.CM_DESC__DEVICE_IO               +"={queryTimeout=10, postpone=0,   paused=false, bg=false, resetNC20=true,  storePcs=false, pcsAbs=true, pcsDiff=true, pcsRate=true}; " +
		GetCounters.CM_DESC__IO_QUEUE_SUM            +"={queryTimeout=10, postpone=0,   paused=false, bg=false, resetNC20=true,  storePcs=false, pcsAbs=true, pcsDiff=true, pcsRate=true}; " +
		GetCounters.CM_DESC__IO_QUEUE                +"={queryTimeout=10, postpone=0,   paused=false, bg=false, resetNC20=true,  storePcs=false, pcsAbs=true, pcsDiff=true, pcsRate=true}; " +
		GetCounters.CM_DESC__SPINLOCK_SUM            +"={queryTimeout=30, postpone=300, paused=false, bg=false, resetNC20=true,  storePcs=false, pcsAbs=true, pcsDiff=true, pcsRate=true}; " +
		GetCounters.CM_DESC__SYSMON                  +"={queryTimeout=30, postpone=300, paused=false, bg=false, resetNC20=true,  storePcs=false, pcsAbs=true, pcsDiff=true, pcsRate=true}; " +
		GetCounters.CM_DESC__RA_SENDERS              +"={queryTimeout=10, postpone=0,   paused=false, bg=false, resetNC20=true,  storePcs=false, pcsAbs=true, pcsDiff=true, pcsRate=true}; " +
		GetCounters.CM_DESC__RA_LOGACTIVITY          +"={queryTimeout=10, postpone=0,   paused=false, bg=false, resetNC20=true,  storePcs=false, pcsAbs=true, pcsDiff=true, pcsRate=true}; " +
		GetCounters.CM_DESC__RA_SCANNERS             +"={queryTimeout=10, postpone=0,   paused=false, bg=false, resetNC20=true,  storePcs=false, pcsAbs=true, pcsDiff=true, pcsRate=true}; " +
		GetCounters.CM_DESC__RA_SCANNERS_TIME        +"={queryTimeout=10, postpone=0,   paused=false, bg=false, resetNC20=true,  storePcs=false, pcsAbs=true, pcsDiff=true, pcsRate=true}; " +
		GetCounters.CM_DESC__RA_SQL_ACTIVITY         +"={queryTimeout=10, postpone=0,   paused=false, bg=false, resetNC20=true,  storePcs=false, pcsAbs=true, pcsDiff=true, pcsRate=true}; " +
		GetCounters.CM_DESC__RA_SQL_MISSES           +"={queryTimeout=10, postpone=0,   paused=false, bg=false, resetNC20=true,  storePcs=false, pcsAbs=true, pcsDiff=true, pcsRate=true}; " +
		GetCounters.CM_DESC__RA_SYSMON               +"={queryTimeout=30, postpone=300, paused=false, bg=false, resetNC20=true,  storePcs=false, pcsAbs=true, pcsDiff=true, pcsRate=true}; " +
		GetCounters.CM_DESC__CACHED_PROC             +"={queryTimeout=10, postpone=0,   paused=false, bg=false, resetNC20=true,  storePcs=false, pcsAbs=true, pcsDiff=true, pcsRate=true}; " +
		GetCounters.CM_DESC__PROC_CACHE_LOAD         +"={queryTimeout=10, postpone=0,   paused=false, bg=false, resetNC20=true,  storePcs=false, pcsAbs=true, pcsDiff=true, pcsRate=true}; " +
		GetCounters.CM_DESC__PROC_CALL_STACK         +"={queryTimeout=10, postpone=0,   paused=false, bg=false, resetNC20=true,  storePcs=false, pcsAbs=true, pcsDiff=true, pcsRate=true}; " +
		GetCounters.CM_DESC__CACHED_OBJECTS          +"={queryTimeout=30, postpone=600, paused=false, bg=false, resetNC20=false, storePcs=false, pcsAbs=true, pcsDiff=true, pcsRate=true}; " +
		GetCounters.CM_DESC__ERRORLOG                +"={queryTimeout=10, postpone=0,   paused=false, bg=false, resetNC20=true,  storePcs=false, pcsAbs=true, pcsDiff=false,pcsRate=false};" +
		GetCounters.CM_DESC__DEADLOCK                +"={queryTimeout=10, postpone=0,   paused=false, bg=false, resetNC20=true,  storePcs=false, pcsAbs=true, pcsDiff=false,pcsRate=false};" +
		GetCounters.CM_DESC__LOCK_TIMEOUT            +"={queryTimeout=10, postpone=0,   paused=false, bg=false, resetNC20=true,  storePcs=false, pcsAbs=true, pcsDiff=false,pcsRate=false};" +
		GetCounters.CM_DESC__PROC_CACHE_MODULE_USAGE +"={queryTimeout=10, postpone=0,   paused=false, bg=false, resetNC20=false, storePcs=false, pcsAbs=true, pcsDiff=true, pcsRate=true}; " +
		GetCounters.CM_DESC__PROC_CACHE_MEMORY_USAGE +"={queryTimeout=10, postpone=0,   paused=false, bg=false, resetNC20=false, storePcs=false, pcsAbs=true, pcsDiff=true, pcsRate=true}; " +
		GetCounters.CM_DESC__STATEMENT_CACHE         +"={queryTimeout=10, postpone=0,   paused=false, bg=false, resetNC20=false, storePcs=false, pcsAbs=true, pcsDiff=true, pcsRate=true}; " +
		GetCounters.CM_DESC__STATEMENT_CACHE_DETAILS +"={queryTimeout=30, postpone=0,   paused=false, bg=false, resetNC20=false, storePcs=false, pcsAbs=true, pcsDiff=true, pcsRate=true}; " +
		GetCounters.CM_DESC__ACTIVE_OBJECTS          +"={queryTimeout=10, postpone=0,   paused=false, bg=false, resetNC20=false, storePcs=false, pcsAbs=true, pcsDiff=true, pcsRate=true}; " +
		GetCounters.CM_DESC__ACTIVE_STATEMENTS       +"={queryTimeout=10, postpone=0,   paused=false, bg=false, resetNC20=true,  storePcs=false, pcsAbs=true, pcsDiff=true, pcsRate=true}; " +
		GetCounters.CM_DESC__BLOCKING                +"={queryTimeout=10, postpone=0,   paused=false, bg=false, resetNC20=false, storePcs=false, pcsAbs=true, pcsDiff=true, pcsRate=true}; " +
		GetCounters.CM_DESC__TABLE_COMPRESSION       +"={queryTimeout=10, postpone=0,   paused=false, bg=false, resetNC20=false, storePcs=false, pcsAbs=true, pcsDiff=true, pcsRate=true}; " +
		GetCounters.CM_DESC__MISSING_STATISTICS      +"={queryTimeout=30, postpone=0,   paused=false, bg=false, resetNC20=false, storePcs=false, pcsAbs=true, pcsDiff=true, pcsRate=true}; " +
		GetCounters.CM_DESC__QP_METRICS              +"={queryTimeout=30, postpone=60,  paused=false, bg=false, resetNC20=false, storePcs=false, pcsAbs=true, pcsDiff=true, pcsRate=true}; " +
		GetCounters.CM_DESC__SP_MONITOR_CONFIG       +"={queryTimeout=10, postpone=3600,paused=false, bg=false, resetNC20=false, storePcs=false, pcsAbs=true, pcsDiff=true, pcsRate=true}; " +
		GetCounters.CM_DESC__OS_IOSTAT               +"={queryTimeout=10, postpone=0,   paused=false, bg=false, resetNC20=false, storePcs=false, pcsAbs=true, pcsDiff=false,pcsRate=false};" +
		GetCounters.CM_DESC__OS_VMSTAT               +"={queryTimeout=10, postpone=0,   paused=false, bg=false, resetNC20=false, storePcs=false, pcsAbs=true, pcsDiff=false,pcsRate=false};" +
		GetCounters.CM_DESC__OS_MPSTAT               +"={queryTimeout=10, postpone=0,   paused=false, bg=false, resetNC20=false, storePcs=false, pcsAbs=true, pcsDiff=false,pcsRate=false};" +
		GetCounters.CM_DESC__OS_UPTIME               +"={queryTimeout=10, postpone=0,   paused=false, bg=false, resetNC20=false, storePcs=false, pcsAbs=true, pcsDiff=false,pcsRate=false};";

	public final static String systemTemplatePcsOffAll =
		GetCounters.CM_DESC__OBJECT_ACTIVITY         +"={queryTimeout=10, postpone=0,   paused=false, bg=false, resetNC20=true,  storePcs=false, pcsAbs=true, pcsDiff=true, pcsRate=true}; " +
		GetCounters.CM_DESC__PROCESS_ACTIVITY        +"={queryTimeout=10, postpone=0,   paused=false, bg=false, resetNC20=true,  storePcs=false, pcsAbs=true, pcsDiff=true, pcsRate=true}; " +
		GetCounters.CM_DESC__PROCESS_WAIT            +"={queryTimeout=10, postpone=0,   paused=false, bg=false, resetNC20=true,  storePcs=false, pcsAbs=true, pcsDiff=true, pcsRate=true}; " +
		GetCounters.CM_DESC__OPEN_DATABASES          +"={queryTimeout=10, postpone=0,   paused=false, bg=false, resetNC20=true,  storePcs=false, pcsAbs=true, pcsDiff=true, pcsRate=true}; " +
		GetCounters.CM_DESC__TEMPDB_ACTIVITY         +"={queryTimeout=10, postpone=0,   paused=false, bg=false, resetNC20=true,  storePcs=false, pcsAbs=true, pcsDiff=true, pcsRate=true}; " +
		GetCounters.CM_DESC__SYS_WAIT                +"={queryTimeout=10, postpone=0,   paused=false, bg=false, resetNC20=true,  storePcs=false, pcsAbs=true, pcsDiff=true, pcsRate=true}; " +
		GetCounters.CM_DESC__ENGINE                  +"={queryTimeout=10, postpone=0,   paused=false, bg=false, resetNC20=true,  storePcs=false, pcsAbs=true, pcsDiff=true, pcsRate=true}; " +
		GetCounters.CM_DESC__SYS_LOAD                +"={queryTimeout=10, postpone=0,   paused=false, bg=false, resetNC20=false, storePcs=false, pcsAbs=true, pcsDiff=true, pcsRate=true}; " +
		GetCounters.CM_DESC__DATA_CACHE              +"={queryTimeout=10, postpone=0,   paused=false, bg=false, resetNC20=true,  storePcs=false, pcsAbs=true, pcsDiff=true, pcsRate=true}; " +
		GetCounters.CM_DESC__CACHE_POOL              +"={queryTimeout=10, postpone=0,   paused=false, bg=false, resetNC20=true,  storePcs=false, pcsAbs=true, pcsDiff=true, pcsRate=true}; " +
		GetCounters.CM_DESC__DEVICE_IO               +"={queryTimeout=10, postpone=0,   paused=false, bg=false, resetNC20=true,  storePcs=false, pcsAbs=true, pcsDiff=true, pcsRate=true}; " +
		GetCounters.CM_DESC__IO_QUEUE_SUM            +"={queryTimeout=10, postpone=0,   paused=false, bg=false, resetNC20=true,  storePcs=false, pcsAbs=true, pcsDiff=true, pcsRate=true}; " +
		GetCounters.CM_DESC__IO_QUEUE                +"={queryTimeout=10, postpone=0,   paused=false, bg=false, resetNC20=true,  storePcs=false, pcsAbs=true, pcsDiff=true, pcsRate=true}; " +
		GetCounters.CM_DESC__SPINLOCK_SUM            +"={queryTimeout=30, postpone=300, paused=false, bg=false, resetNC20=true,  storePcs=false, pcsAbs=true, pcsDiff=true, pcsRate=true}; " +
		GetCounters.CM_DESC__SYSMON                  +"={queryTimeout=30, postpone=300, paused=false, bg=false, resetNC20=true,  storePcs=false, pcsAbs=true, pcsDiff=true, pcsRate=true}; " +
		GetCounters.CM_DESC__RA_SENDERS              +"={queryTimeout=10, postpone=0,   paused=false, bg=false, resetNC20=true,  storePcs=false, pcsAbs=true, pcsDiff=true, pcsRate=true}; " +
		GetCounters.CM_DESC__RA_LOGACTIVITY          +"={queryTimeout=10, postpone=0,   paused=false, bg=false, resetNC20=true,  storePcs=false, pcsAbs=true, pcsDiff=true, pcsRate=true}; " +
		GetCounters.CM_DESC__RA_SCANNERS             +"={queryTimeout=10, postpone=0,   paused=false, bg=false, resetNC20=true,  storePcs=false, pcsAbs=true, pcsDiff=true, pcsRate=true}; " +
		GetCounters.CM_DESC__RA_SCANNERS_TIME        +"={queryTimeout=10, postpone=0,   paused=false, bg=false, resetNC20=true,  storePcs=false, pcsAbs=true, pcsDiff=true, pcsRate=true}; " +
		GetCounters.CM_DESC__RA_SQL_ACTIVITY         +"={queryTimeout=10, postpone=0,   paused=false, bg=false, resetNC20=true,  storePcs=false, pcsAbs=true, pcsDiff=true, pcsRate=true}; " +
		GetCounters.CM_DESC__RA_SQL_MISSES           +"={queryTimeout=10, postpone=0,   paused=false, bg=false, resetNC20=true,  storePcs=false, pcsAbs=true, pcsDiff=true, pcsRate=true}; " +
		GetCounters.CM_DESC__RA_SYSMON               +"={queryTimeout=30, postpone=300, paused=false, bg=false, resetNC20=true,  storePcs=false, pcsAbs=true, pcsDiff=true, pcsRate=true}; " +
		GetCounters.CM_DESC__CACHED_PROC             +"={queryTimeout=10, postpone=0,   paused=false, bg=false, resetNC20=true,  storePcs=false, pcsAbs=true, pcsDiff=true, pcsRate=true}; " +
		GetCounters.CM_DESC__PROC_CACHE_LOAD         +"={queryTimeout=10, postpone=0,   paused=false, bg=false, resetNC20=true,  storePcs=false, pcsAbs=true, pcsDiff=true, pcsRate=true}; " +
		GetCounters.CM_DESC__PROC_CALL_STACK         +"={queryTimeout=10, postpone=0,   paused=false, bg=false, resetNC20=true,  storePcs=false, pcsAbs=true, pcsDiff=true, pcsRate=true}; " +
		GetCounters.CM_DESC__CACHED_OBJECTS          +"={queryTimeout=30, postpone=600, paused=false, bg=false, resetNC20=false, storePcs=false, pcsAbs=true, pcsDiff=true, pcsRate=true}; " +
		GetCounters.CM_DESC__ERRORLOG                +"={queryTimeout=10, postpone=0,   paused=false, bg=false, resetNC20=true,  storePcs=false, pcsAbs=true, pcsDiff=false,pcsRate=false};" +
		GetCounters.CM_DESC__DEADLOCK                +"={queryTimeout=10, postpone=0,   paused=false, bg=false, resetNC20=true,  storePcs=false, pcsAbs=true, pcsDiff=false,pcsRate=false};" +
		GetCounters.CM_DESC__LOCK_TIMEOUT            +"={queryTimeout=10, postpone=0,   paused=false, bg=false, resetNC20=true,  storePcs=false, pcsAbs=true, pcsDiff=false,pcsRate=false};" +
		GetCounters.CM_DESC__PROC_CACHE_MODULE_USAGE +"={queryTimeout=10, postpone=0,   paused=false, bg=false, resetNC20=false, storePcs=false, pcsAbs=true, pcsDiff=true, pcsRate=true}; " +
		GetCounters.CM_DESC__PROC_CACHE_MEMORY_USAGE +"={queryTimeout=10, postpone=0,   paused=false, bg=false, resetNC20=false, storePcs=false, pcsAbs=true, pcsDiff=true, pcsRate=true}; " +
		GetCounters.CM_DESC__STATEMENT_CACHE         +"={queryTimeout=10, postpone=0,   paused=false, bg=false, resetNC20=false, storePcs=false, pcsAbs=true, pcsDiff=true, pcsRate=true}; " +
		GetCounters.CM_DESC__STATEMENT_CACHE_DETAILS +"={queryTimeout=30, postpone=0,   paused=false, bg=false, resetNC20=false, storePcs=false, pcsAbs=true, pcsDiff=true, pcsRate=true}; " +
		GetCounters.CM_DESC__ACTIVE_OBJECTS          +"={queryTimeout=10, postpone=0,   paused=false, bg=false, resetNC20=false, storePcs=false, pcsAbs=true, pcsDiff=true, pcsRate=true}; " +
		GetCounters.CM_DESC__ACTIVE_STATEMENTS       +"={queryTimeout=10, postpone=0,   paused=false, bg=false, resetNC20=true,  storePcs=false, pcsAbs=true, pcsDiff=true, pcsRate=true}; " +
		GetCounters.CM_DESC__BLOCKING                +"={queryTimeout=10, postpone=0,   paused=false, bg=false, resetNC20=false, storePcs=false, pcsAbs=true, pcsDiff=true, pcsRate=true}; " +
		GetCounters.CM_DESC__TABLE_COMPRESSION       +"={queryTimeout=10, postpone=0,   paused=false, bg=false, resetNC20=false, storePcs=false, pcsAbs=true, pcsDiff=true, pcsRate=true}; " +
		GetCounters.CM_DESC__MISSING_STATISTICS      +"={queryTimeout=30, postpone=0,   paused=false, bg=false, resetNC20=false, storePcs=false, pcsAbs=true, pcsDiff=true, pcsRate=true}; " +
		GetCounters.CM_DESC__QP_METRICS              +"={queryTimeout=30, postpone=60,  paused=false, bg=false, resetNC20=false, storePcs=false, pcsAbs=true, pcsDiff=true, pcsRate=true}; " +
		GetCounters.CM_DESC__SP_MONITOR_CONFIG       +"={queryTimeout=10, postpone=3600,paused=false, bg=false, resetNC20=false, storePcs=false, pcsAbs=true, pcsDiff=true, pcsRate=true}; " +
		GetCounters.CM_DESC__OS_IOSTAT               +"={queryTimeout=10, postpone=0,   paused=false, bg=false, resetNC20=false, storePcs=false, pcsAbs=true, pcsDiff=false,pcsRate=false};" +
		GetCounters.CM_DESC__OS_VMSTAT               +"={queryTimeout=10, postpone=0,   paused=false, bg=false, resetNC20=false, storePcs=false, pcsAbs=true, pcsDiff=false,pcsRate=false};" +
		GetCounters.CM_DESC__OS_MPSTAT               +"={queryTimeout=10, postpone=0,   paused=false, bg=false, resetNC20=false, storePcs=false, pcsAbs=true, pcsDiff=false,pcsRate=false};" +
		GetCounters.CM_DESC__OS_UPTIME               +"={queryTimeout=10, postpone=0,   paused=false, bg=false, resetNC20=false, storePcs=false, pcsAbs=true, pcsDiff=false,pcsRate=false};";


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



