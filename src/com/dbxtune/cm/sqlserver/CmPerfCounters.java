/*******************************************************************************
 * Copyright (C) 2010-2025 Goran Schwarz
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
package com.dbxtune.cm.sqlserver;

import java.awt.event.MouseEvent;
import java.lang.invoke.MethodHandles;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import javax.naming.NameNotFoundException;

import org.apache.commons.lang3.ArrayUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.dbxtune.ICounterController;
import com.dbxtune.IGuiController;
import com.dbxtune.Version;
import com.dbxtune.alarm.AlarmHandler;
import com.dbxtune.alarm.events.AlarmEvent;
import com.dbxtune.alarm.events.AlarmEvent.Severity;
import com.dbxtune.alarm.events.sqlserver.AlarmEventPerfCounter;
import com.dbxtune.central.pcs.CentralPersistReader;
import com.dbxtune.cm.CmSettingsHelper;
import com.dbxtune.cm.CounterSample;
import com.dbxtune.cm.CounterSetTemplates;
import com.dbxtune.cm.CounterSetTemplates.Type;
import com.dbxtune.cm.CountersModel;
import com.dbxtune.cm.sqlserver.gui.CmPerfCountersPanel;
import com.dbxtune.config.dict.MonTablesDictionary;
import com.dbxtune.config.dict.MonTablesDictionaryManager;
import com.dbxtune.config.dict.SqlServerDmOsPerformanceCountersDictionary;
import com.dbxtune.graph.TrendGraphDataPoint;
import com.dbxtune.graph.TrendGraphDataPoint.LabelType;
import com.dbxtune.gui.MainFrame;
import com.dbxtune.gui.TabularCntrPanel;
import com.dbxtune.gui.TrendGraph;
import com.dbxtune.sql.conn.DbxConnection;
import com.dbxtune.sql.conn.info.DbmsVersionInfo;
import com.dbxtune.sql.conn.info.DbmsVersionInfoSqlServer;
import com.dbxtune.utils.Configuration;
import com.dbxtune.utils.NumberUtils;
import com.dbxtune.utils.StringUtil;
import com.dbxtune.utils.Ver;

/**
 * @author Goran Schwarz (goran_schwarz@hotmail.com)
 */
public class CmPerfCounters
extends CountersModel
{
	private static final Logger _logger = LogManager.getLogger(MethodHandles.lookup().lookupClass());
	private static final long    serialVersionUID = 1L;

	public static final String   CM_NAME          = CmPerfCounters.class.getSimpleName();
	public static final String   SHORT_NAME       = "Perf Counters";
	public static final String   HTML_DESC        = 
		"<html>" +
	
		"<p>Counter values from: <code>sys.dm_os_performance_counters</code>, which basically is WMI counters used in the Windows Performance Monitor. (WMI=Windows Management Instrumentation)</p>" +
				
		"<p>For the various counter cntr_type_name, see <A HREF='https://blogs.msdn.microsoft.com/psssql/2013/09/23/interpreting-the-counter-values-from-sys-dm_os_performance_counters/'>https://blogs.msdn.microsoft.com/psssql/2013/09/23/interpreting-the-counter-values-from-sys-dm_os_performance_counters/</A></p>" +

		"<table border=1 cellspacing=1 cellpadding=1 width='1000'>" +
		"<tr> <th>#</th> <th>Counter Type               </th> <th> Short Description                                                                                     </th> <th> Long Description </th> </tr>" +
		"<tr> <td>1</td> <td>PERF_LARGE_RAW_BASE        </td> <td> Used as <i>base</i> value for counters of type #2 and #3                                              </td> <td> This counter value is raw data that is used as the denominator of a counter that presents a instantaneous arithmetic fraction. See PERF_LARGE_RAW_FRACTION for more information. </td> </tr>" +
		"<tr> <td>2</td> <td>PERF_LARGE_RAW_FRACTION    </td> <td> (key)VALUE / (key_base)PERF_LARGE_RAW_BASE                                                            </td> <td> This counter value represents a fractional value as a ratio to its corresponding PERF_LARGE_RAW_BASE counter value. </td> </tr>" +
		"<tr> <td>3</td> <td>PERF_AVERAGE_BULK          </td> <td> Needs <i>diff calculation</i> and base_key divitions (A2 - A1) / (B2 - B1)                            </td> <td> This counter value represents an average metric. The cntr_value is cumulative. The base value of type PERF_LARGE_RAW_BASE is used which is also cumulative. The value is obtained by first taking two samples of both the PERF_AVERAGE_BULK value A1 and A2 as well as the  PERF_LARGE_RAW_BASE value B1 and B2. The difference between A1 and A2 and B1 and B2 are calculated. The final value is then calculated as the ratio of the differences. The example below will help make this clearer. </td> </tr>" +
		"<tr> <td>4</td> <td>PERF_COUNTER_BULK_COUNT    </td> <td> Can be used in <b><i>rate</i></b> calculation.<br>Ex: (sample2 - sample1) / (seconds between samples) </td> <td> This counter value represents a rate metric. The cntr_value is cumulative. The value is obtained by taking two samples of the PERF_COUNTER_BULK_COUNT value. The difference between the sample values is divided by the time gap between the samples in seconds. This provides the per second rate. </td> </tr>" +
		"<tr> <td>5</td> <td>PERF_COUNTER_LARGE_RAWCOUNT</td> <td> For instant counter vales<br>Ex: Current 'Total pages' in 'Buffer Manager'                            </td> <td> This counter value shows the last observed value directly. Primarily used to track counts of objects. </td> </tr>" +
		"</table>" +
		"</html>";

	public static final String   GROUP_NAME       = MainFrame.TCP_GROUP_SERVER;
	public static final String   GUI_ICON_FILE    = "images/"+CM_NAME+".png";

	public static final long     NEED_SRV_VERSION = 0;
	public static final long     NEED_CE_VERSION  = 0;

	public static final String[] MON_TABLES       = new String[] {"dm_os_performance_counters"};
	public static final String[] NEED_ROLES       = new String[] {"VIEW SERVER STATE"};
	public static final String[] NEED_CONFIG      = new String[] {};

	public static final String[] PCT_COLUMNS      = new String[] {};
	public static final String[] DIFF_COLUMNS     = new String[] {
		"cntr_value"
		};

	// 2024-04-24 I THINK we should have NEGATIVE_DIFF_COUNTERS_TO_ZERO to TRUE (but not 100% sure). 
	// There might be specific counters that we want to have as negative calculated, but with "ndctz"=FALSE many graphs will have negative values after aSQL Server restart, which might not be the best
	// Lets go with "reset negative counters to zero" for a while and see how it behaves
	public static final boolean  NEGATIVE_DIFF_COUNTERS_TO_ZERO = true;  // See comments above
	public static final boolean  IS_SYSTEM_CM                   = true;
	public static final int      DEFAULT_POSTPONE_TIME          = 0;
	public static final int      DEFAULT_QUERY_TIMEOUT          = CountersModel.DEFAULT_sqlQueryTimeout;;

	@Override public int     getDefaultPostponeTime()                 { return DEFAULT_POSTPONE_TIME; }
	@Override public int     getDefaultQueryTimeout()                 { return DEFAULT_QUERY_TIMEOUT; }
	@Override public boolean getDefaultIsNegativeDiffCountersToZero() { return NEGATIVE_DIFF_COUNTERS_TO_ZERO; }
	@Override public Type    getTemplateLevel()                       { return Type.ALL; }

	/** A list of known databases, populated/refreshed in localCalculation(...), and used for example in updateGraphData() */
	private List<String> _dbnames = new ArrayList<>();

	/**
	 * FACTORY  method to create the object
	 */
	public static CountersModel create(ICounterController counterController, IGuiController guiController)
	{
		if (guiController != null && guiController.hasGUI())
			guiController.splashWindowProgress("Loading: Counter Model '"+CM_NAME+"'");

		return new CmPerfCounters(counterController, guiController);
	}

	public CmPerfCounters(ICounterController counterController, IGuiController guiController)
	{
		super(counterController, guiController,
				CM_NAME, GROUP_NAME, /*sql*/null, /*pkList*/null, 
				DIFF_COLUMNS, PCT_COLUMNS, MON_TABLES, 
				NEED_ROLES, NEED_CONFIG, NEED_SRV_VERSION, NEED_CE_VERSION, 
				NEGATIVE_DIFF_COUNTERS_TO_ZERO, IS_SYSTEM_CM, DEFAULT_POSTPONE_TIME);

		setDisplayName(SHORT_NAME);
		setDescription(HTML_DESC);

		setIconFile(GUI_ICON_FILE);

		setShowClearTime(false);

		setCounterController(counterController);
		setGuiController(guiController);
		
		addTrendGraphs();
		
		CounterSetTemplates.register(this);
	}


	//------------------------------------------------------------
	// Implementation
	//------------------------------------------------------------
	public static final String GRAPH_NAME_CPU_PCT                   = "OsCpu";
	public static final String GRAPH_NAME_CPU_EFFECTIVE_PCT         = "OsCpuEffective";
	public static final String GRAPH_NAME_SQL_BATCHES_SEC           = "SqlBatch";
	public static final String GRAPH_NAME_SQL_BATCHES_TIME_SPAN_ALL = "SqlBatchTSpanAll";
	public static final String GRAPH_NAME_SQL_BATCHES_TIME_SPAN_0   = "SqlBatchTSpan0";
	public static final String GRAPH_NAME_SQL_BATCHES_TIME_SPAN_1   = "SqlBatchTSpan1";
	public static final String GRAPH_NAME_SQL_BATCHES_TIME_SPAN_2   = "SqlBatchTSpan2";
	public static final String GRAPH_NAME_SQL_BATCHES_TIME_SPAN_3   = "SqlBatchTSpan3";
	public static final String GRAPH_NAME_SQL_BATCHES_TIME_SPAN_4   = "SqlBatchTSpan4";

	public static final String GRAPH_NAME_CACHE_HIT_RATE            = "CacheHitRate";
	public static final String GRAPH_NAME_CACHE_PLE                 = "CachePle";  // PLE = Page Life Expectancy
	public static final String GRAPH_NAME_CACHE_READS               = "CacheReads";
	public static final String GRAPH_NAME_CACHE_PHY_READS           = "CachePhyReads";
	public static final String GRAPH_NAME_CACHE_PHY_READ_AHEAD_PCT  = "CachePhyReadAheadPct";
	public static final String GRAPH_NAME_CACHE_WRITES              = "CacheWrites";
	public static final String GRAPH_NAME_CACHE_DATA_SIZE           = "CacheDataSize";
                                                                    
	public static final String GRAPH_NAME_TEMP_STAT                 = "TempStat";
	public static final String GRAPH_NAME_TEMPDB_FREE_SPACE         = "TempdbFreeSpace";
	public static final String GRAPH_NAME_VERSION_STORE             = "VersionStore";
	public static final String GRAPH_NAME_SNAPSHOT_ISOLATION        = "SnapshotIsolation";
                                                                    
	public static final String GRAPH_NAME_LOG_CACHE_HIT_RATE        = "LogCacheHitRate";
	public static final String GRAPH_NAME_LOG_CACHE_READS           = "LogCacheReads";
	public static final String GRAPH_NAME_LOG_POOL_REQUESTS         = "LogPoolReq";
                                                                    
	public static final String GRAPH_NAME_TRANS_SEC                 = "TransSec";
	public static final String GRAPH_NAME_TRANS_WRITE_SEC           = "TransWriteSec";
	public static final String GRAPH_NAME_TRANS_ACTIVE              = "TransActive";
                                                                    
	public static final String GRAPH_NAME_COMPILE                   = "Compile";
	public static final String GRAPH_NAME_RECOMPILE                 = "ReCompile";
                                                                    
	public static final String GRAPH_NAME_BLOCKED_PROCESSES         = "BlockedProcs";
                                                                    
	public static final String GRAPH_NAME_PAGE_SPLITS               = "PageSplits";
	public static final String GRAPH_NAME_FWD_RIDS                  = "FwdRids";
	public static final String GRAPH_NAME_ACCESS_METHODS            = "AccessMethods";
                                                                    
	public static final String GRAPH_NAME_LATCH_WAITS               = "LatchWaits";
	public static final String GRAPH_NAME_LATCH_WAIT_TIME           = "LatchWaitTime";

	public static final String GRAPH_NAME_FREE_LIST_STALLS          = "FreeListStalls";
	public static final String GRAPH_NAME_MEMORY_GRANTS_PENDING     = "MemoryGrantsPending";

	public static final String GRAPH_NAME_PLAN_CACHE_HIT_RATE       = "PlanCacheHitRate";
	public static final String GRAPH_NAME_PLAN_CACHE_MB             = "PlanCacheMb";
	public static final String GRAPH_NAME_PLAN_CACHE_OBJ_CNT        = "PlanCacheObjCnt";
	public static final String GRAPH_NAME_PLAN_CACHE_OBJ_USE        = "PlanCacheObjUse";

	public static final String GRAPH_NAME_MEMORY_MANAGER            = "MemMgr";
	public static final String GRAPH_NAME_MEMORY_TARGET_VS_TOTAL    = "MemTargetVsTotal";
//	public static final String GRAPH_NAME_MEMORY_X2                 = "MemMgr"; xxx
//	public static final String GRAPH_NAME_MEMORY_X3                 = "MemMgr"; xxx

	public static final String GRAPH_NAME_MEMORY_STOLEN             = "MemStolen";
	
	public static final String GRAPH_NAME_QUERY_STORE_TOTAL         = "QsTotal";
	public static final String GRAPH_NAME_QUERY_STORE_DB_CPU        = "QsDbCpu";
	public static final String GRAPH_NAME_QUERY_STORE_DB_PHYS_READ  = "QsDbPhysRead";
	public static final String GRAPH_NAME_QUERY_STORE_DB_LOGI_READ  = "QsDbLogiRead";
	public static final String GRAPH_NAME_QUERY_STORE_DB_LOGI_WRITE = "QsDbLogiWrite";

	public static final String GRAPH_NAME_DEADLOCK_DETAILS          = "DeadlockDetails";
	
	public static final String GRAPH_NAME_COLUMNSTORE_ALL           = "ColumnStoreAll"; // This will PROBABLY have to be split up in sections... But for now at least have SOMETHING for Column Store
	
	public static final String GRAPH_NAME_SQL_ERRORS                = "SqlErrors";
	
	private void addTrendGraphs()
	{
		//-----
		addTrendGraph(GRAPH_NAME_CPU_PCT,
			"CPU Usage in Percent", // Menu CheckBox text
			"CPU Usage in Percent ("+GROUP_NAME+"->"+SHORT_NAME+")", // Label 
			TrendGraphDataPoint.createGraphProps(TrendGraphDataPoint.Y_AXIS_SCALE_LABELS_PERCENT, CentralPersistReader.SampleType.MAX_OVER_SAMPLES),
			new String[] { "CPU usage %" }, 
			LabelType.Static,
			TrendGraphDataPoint.Category.CPU,
			true,  // is Percent Graph
			true,  // visible at start
			0,     // graph is valid from Server Version. 0 = All Versions; >0 = Valid from this version and above 
			-1);   // minimum height

		//-----
		addTrendGraph(GRAPH_NAME_CPU_EFFECTIVE_PCT,
			"CPU Usage Effective in Percent", // Menu CheckBox text
			"CPU Usage Effective in Percent ("+GROUP_NAME+"->"+SHORT_NAME+")", // Label 
			TrendGraphDataPoint.createGraphProps(TrendGraphDataPoint.Y_AXIS_SCALE_LABELS_PERCENT, CentralPersistReader.SampleType.MAX_OVER_SAMPLES),
			new String[] { "CPU effective %" }, 
			LabelType.Static,
			TrendGraphDataPoint.Category.CPU,
			true,  // is Percent Graph
			true,  // visible at start
			0,     // graph is valid from Server Version. 0 = All Versions; >0 = Valid from this version and above 
			-1);   // minimum height

		//-----
		addTrendGraph(GRAPH_NAME_SQL_BATCHES_SEC,
			"SQL Batches Received per Sec", // Menu CheckBox text
			"SQL Batches Received per Sec ("+GROUP_NAME+"->"+SHORT_NAME+")", // Label 
			TrendGraphDataPoint.createGraphProps(TrendGraphDataPoint.Y_AXIS_SCALE_LABELS_PERSEC, CentralPersistReader.SampleType.MAX_OVER_SAMPLES),
			new String[] { "Batch Requests/sec" }, 
			LabelType.Static,
			TrendGraphDataPoint.Category.OPERATIONS,
			false, // is Percent Graph
			true,  // visible at start
			0,     // graph is valid from Server Version. 0 = All Versions; >0 = Valid from this version and above 
			-1);   // minimum height

		//-----
		addTrendGraph(GRAPH_NAME_SQL_BATCHES_TIME_SPAN_ALL,
			"SQL Batches (all) In Time Span Received per Sec", // Menu CheckBox text
			"SQL Batches (all) In Time Span Received per Sec ("+GROUP_NAME+"->"+SHORT_NAME+")", // Label 
			TrendGraphDataPoint.createGraphProps(TrendGraphDataPoint.Y_AXIS_SCALE_LABELS_PERSEC, CentralPersistReader.SampleType.MAX_OVER_SAMPLES),
			new String[] { "<1ms", "1-2ms", "2-5ms", "5-10ms", "10-20ms", "20-50ms", "50-100ms", "100-200ms", "200-500ms", "500ms-1s", "1-2s", "2-5s", "5-10s", "10-20s", "20-50s", "50-100s", ">100s" }, 
			LabelType.Static,
			TrendGraphDataPoint.Category.OPERATIONS,
			false, // is Percent Graph
			true,  // visible at start
			0,     // graph is valid from Server Version. 0 = All Versions; >0 = Valid from this version and above 
			-1);   // minimum height

		//-----
		addTrendGraph(GRAPH_NAME_SQL_BATCHES_TIME_SPAN_0,
			"SQL Batches (0-10ms) In Time Span Received per Sec", // Menu CheckBox text
			"SQL Batches (0-10ms) In Time Span Received per Sec ("+GROUP_NAME+"->"+SHORT_NAME+")", // Label 
			TrendGraphDataPoint.createGraphProps(TrendGraphDataPoint.Y_AXIS_SCALE_LABELS_PERSEC, CentralPersistReader.SampleType.MAX_OVER_SAMPLES),
			new String[] { "<1ms", "1-2ms", "2-5ms", "5-10ms" }, 
			LabelType.Static,
			TrendGraphDataPoint.Category.OPERATIONS,
			false, // is Percent Graph
			true,  // visible at start
			0,     // graph is valid from Server Version. 0 = All Versions; >0 = Valid from this version and above 
			-1);   // minimum height

		//-----
		addTrendGraph(GRAPH_NAME_SQL_BATCHES_TIME_SPAN_1,
			"SQL Batches (10ms-100ms) In Time Span Received per Sec", // Menu CheckBox text
			"SQL Batches (10ms-100ms) In Time Span Received per Sec ("+GROUP_NAME+"->"+SHORT_NAME+")", // Label 
			TrendGraphDataPoint.createGraphProps(TrendGraphDataPoint.Y_AXIS_SCALE_LABELS_PERSEC, CentralPersistReader.SampleType.MAX_OVER_SAMPLES),
			new String[] { "10-20ms", "20-50ms", "50-100ms" }, 
			LabelType.Static,
			TrendGraphDataPoint.Category.OPERATIONS,
			false, // is Percent Graph
			true,  // visible at start
			0,     // graph is valid from Server Version. 0 = All Versions; >0 = Valid from this version and above 
			-1);   // minimum height

		//-----
		addTrendGraph(GRAPH_NAME_SQL_BATCHES_TIME_SPAN_2,
			"SQL Batches (100ms-1s) In Time Span Received per Sec", // Menu CheckBox text
			"SQL Batches (100ms-1s) In Time Span Received per Sec ("+GROUP_NAME+"->"+SHORT_NAME+")", // Label 
			TrendGraphDataPoint.createGraphProps(TrendGraphDataPoint.Y_AXIS_SCALE_LABELS_PERSEC, CentralPersistReader.SampleType.MAX_OVER_SAMPLES),
			new String[] { "100-200ms", "200-500ms", "500ms-1s" }, 
			LabelType.Static,
			TrendGraphDataPoint.Category.OPERATIONS,
			false, // is Percent Graph
			true,  // visible at start
			0,     // graph is valid from Server Version. 0 = All Versions; >0 = Valid from this version and above 
			-1);   // minimum height

		//-----
		addTrendGraph(GRAPH_NAME_SQL_BATCHES_TIME_SPAN_3,
			"SQL Batches (1s-10s) In Time Span Received per Sec", // Menu CheckBox text
			"SQL Batches (1s-10s) In Time Span Received per Sec ("+GROUP_NAME+"->"+SHORT_NAME+")", // Label 
			TrendGraphDataPoint.createGraphProps(TrendGraphDataPoint.Y_AXIS_SCALE_LABELS_PERSEC, CentralPersistReader.SampleType.MAX_OVER_SAMPLES),
			new String[] { "1-2s", "2-5s", "5-10s" }, 
			LabelType.Static,
			TrendGraphDataPoint.Category.OPERATIONS,
			false, // is Percent Graph
			true,  // visible at start
			0,     // graph is valid from Server Version. 0 = All Versions; >0 = Valid from this version and above 
			-1);   // minimum height

		//-----
		addTrendGraph(GRAPH_NAME_SQL_BATCHES_TIME_SPAN_4,
			"SQL Batches (>10s) In Time Span Received per SAMPLE", // Menu CheckBox text
			"SQL Batches (>10s) In Time Span Received per SAMPLE ("+GROUP_NAME+"->"+SHORT_NAME+")", // Label 
			TrendGraphDataPoint.createGraphProps(TrendGraphDataPoint.Y_AXIS_SCALE_LABELS_PERSEC, CentralPersistReader.SampleType.MAX_OVER_SAMPLES),
			new String[] { "10-20s", "20-50s", "50-100s", ">100s" }, 
			LabelType.Static,
			TrendGraphDataPoint.Category.OPERATIONS,
			false, // is Percent Graph
			true,  // visible at start
			0,     // graph is valid from Server Version. 0 = All Versions; >0 = Valid from this version and above 
			-1);   // minimum height

		//-----
		addTrendGraph(GRAPH_NAME_CACHE_HIT_RATE,
			"Buffer Cache Hit Rate", // Menu CheckBox text
			"Buffer Cache Hit Rate, in Percent ("+GROUP_NAME+"->"+SHORT_NAME+")", // Label 
			TrendGraphDataPoint.createGraphProps(TrendGraphDataPoint.Y_AXIS_SCALE_LABELS_PERCENT, CentralPersistReader.SampleType.MAX_OVER_SAMPLES),
			new String[] { "Buffer cache hit ratio (Cache Hit Percent)" }, 
			LabelType.Static,
			TrendGraphDataPoint.Category.CACHE,
			true,  // is Percent Graph
			true,  // visible at start
			0,     // graph is valid from Server Version. 0 = All Versions; >0 = Valid from this version and above 
			-1);   // minimum height

		//-----
		addTrendGraph(GRAPH_NAME_CACHE_PLE,
			"Page Life Expectancy", // Menu CheckBox text
			"Page Life Expectancy ("+GROUP_NAME+"->"+SHORT_NAME+")", // Label 
			TrendGraphDataPoint.createGraphProps(TrendGraphDataPoint.Y_AXIS_SCALE_LABELS_NORMAL, CentralPersistReader.SampleType.MAX_OVER_SAMPLES),
			new String[] { "Page life expectancy (high is good)" }, 
			LabelType.Static,
			TrendGraphDataPoint.Category.CACHE,
			false, // is Percent Graph
			true,  // visible at start
			0,     // graph is valid from Server Version. 0 = All Versions; >0 = Valid from this version and above 
			-1);   // minimum height

		//-----
		addTrendGraph(GRAPH_NAME_CACHE_READS,
			"Buffer Cache Reads", // Menu CheckBox text
			"Buffer Cache Reads per Sec ("+GROUP_NAME+"->"+SHORT_NAME+")", // Label 
			TrendGraphDataPoint.createGraphProps(TrendGraphDataPoint.Y_AXIS_SCALE_LABELS_PERSEC, CentralPersistReader.SampleType.MAX_OVER_SAMPLES),
			new String[] { "Page lookups (LogicalReads)", "Page reads (PhysicalReads)", "Readahead pages (PhysicalReads)" }, 
			LabelType.Static,
			TrendGraphDataPoint.Category.CACHE,
			false, // is Percent Graph
			true,  // visible at start
			0,     // graph is valid from Server Version. 0 = All Versions; >0 = Valid from this version and above 
			-1);   // minimum height

		//-----
		addTrendGraph(GRAPH_NAME_CACHE_PHY_READS,
			"Buffer Cache Physical Reads", // Menu CheckBox text
			"Buffer Cache Physical Reads per Sec ("+GROUP_NAME+"->"+SHORT_NAME+")", // Label 
			TrendGraphDataPoint.createGraphProps(TrendGraphDataPoint.Y_AXIS_SCALE_LABELS_PERSEC, CentralPersistReader.SampleType.MAX_OVER_SAMPLES),
			new String[] { "Page Reads (PhysicalReads)", "Read Ahead Pages (PhysicalReads)" }, 
			LabelType.Static,
			TrendGraphDataPoint.Category.DISK,
			false, // is Percent Graph
			true,  // visible at start
			0,     // graph is valid from Server Version. 0 = All Versions; >0 = Valid from this version and above 
			-1);   // minimum height

		//-----
		addTrendGraph(GRAPH_NAME_CACHE_PHY_READ_AHEAD_PCT,
			"Buffer Cache Physical Readahead Percent", // Menu CheckBox text
			"Buffer Cache Physical Readahead Percent ("+GROUP_NAME+"->"+SHORT_NAME+")", // Label 
			TrendGraphDataPoint.createGraphProps(TrendGraphDataPoint.Y_AXIS_SCALE_LABELS_PERCENT, CentralPersistReader.SampleType.MAX_OVER_SAMPLES),
			new String[] { "Readahead Percent" }, 
			LabelType.Static,
			TrendGraphDataPoint.Category.DISK,
			true,  // is Percent Graph
			true,  // visible at start
			0,     // graph is valid from Server Version. 0 = All Versions; >0 = Valid from this version and above 
			-1);   // minimum height

		//-----
		addTrendGraph(GRAPH_NAME_CACHE_WRITES,
			"Buffer Cache Writes", // Menu CheckBox text
			"Buffer Cache Writes per Sec ("+GROUP_NAME+"->"+SHORT_NAME+")", // Label 
			TrendGraphDataPoint.createGraphProps(TrendGraphDataPoint.Y_AXIS_SCALE_LABELS_PERSEC, CentralPersistReader.SampleType.MAX_OVER_SAMPLES),
			new String[] { "Page writes", "Checkpoint pages", "Background writer pages", "Lazy writes" }, 
			LabelType.Static,
			TrendGraphDataPoint.Category.DISK,
			false, // is Percent Graph
			true,  // visible at start
			0,     // graph is valid from Server Version. 0 = All Versions; >0 = Valid from this version and above 
			-1);   // minimum height

		//-----
		addTrendGraph(GRAPH_NAME_CACHE_DATA_SIZE,
			"Buffer Cache Data Size in MB", // Menu CheckBox text
			"Buffer Cache Data Size in MB ("+GROUP_NAME+"->"+SHORT_NAME+")", // Label 
			TrendGraphDataPoint.createGraphProps(TrendGraphDataPoint.Y_AXIS_SCALE_LABELS_PERSEC, CentralPersistReader.SampleType.MAX_OVER_SAMPLES),
			new String[] { "Database pages" }, 
			LabelType.Static,
			TrendGraphDataPoint.Category.CACHE,
			false, // is Percent Graph
			true,  // visible at start
			0,     // graph is valid from Server Version. 0 = All Versions; >0 = Valid from this version and above 
			-1);   // minimum height

		//-----
		addTrendGraph(GRAPH_NAME_TEMP_STAT,
			"Temp Table Stats", // Menu CheckBox text
			"Temp Table Stats ("+GROUP_NAME+"->"+SHORT_NAME+")", // Label 
			TrendGraphDataPoint.createGraphProps(TrendGraphDataPoint.Y_AXIS_SCALE_LABELS_NORMAL, CentralPersistReader.SampleType.MAX_OVER_SAMPLES),
			new String[] { "Active Temp Tables", "Temp Tables Creation Rate", "Temp Tables For Destruction" }, 
			LabelType.Static,
			TrendGraphDataPoint.Category.DISK,
			false, // is Percent Graph
			true,  // visible at start
			0,     // graph is valid from Server Version. 0 = All Versions; >0 = Valid from this version and above 
			-1);   // minimum height

		//-----
		addTrendGraph(GRAPH_NAME_TEMPDB_FREE_SPACE,
			"Tempdb Free Space", // Menu CheckBox text
			"Tempdb Free Space ("+GROUP_NAME+"->"+SHORT_NAME+")", // Label 
			TrendGraphDataPoint.createGraphProps(TrendGraphDataPoint.Y_AXIS_SCALE_LABELS_KB, CentralPersistReader.SampleType.MAX_OVER_SAMPLES),
			new String[] { "Free Space in tempdb (KB)" }, 
			LabelType.Static,
			TrendGraphDataPoint.Category.SPACE,
			false, // is Percent Graph
			true,  // visible at start
			0,     // graph is valid from Server Version. 0 = All Versions; >0 = Valid from this version and above 
			-1);   // minimum height

		//-----
		addTrendGraph(GRAPH_NAME_VERSION_STORE,
			"Version Store in Tempdb", // Menu CheckBox text
			"Version Store in Tempdb ("+GROUP_NAME+"->"+SHORT_NAME+")", // Label 
			TrendGraphDataPoint.createGraphProps(TrendGraphDataPoint.Y_AXIS_SCALE_LABELS_KB, CentralPersistReader.SampleType.MAX_OVER_SAMPLES),
			new String[] { "Version Store Size (KB)", "Version Generation rate (KB/s)", "Version Cleanup rate (KB/s)" }, 
			LabelType.Static,
			TrendGraphDataPoint.Category.SPACE,
			false, // is Percent Graph
			true,  // visible at start
			0,     // graph is valid from Server Version. 0 = All Versions; >0 = Valid from this version and above 
			-1);   // minimum height

		//-----
		addTrendGraph(GRAPH_NAME_SNAPSHOT_ISOLATION,
			"Snapshot Isolation", // Menu CheckBox text
			"Snapshot Isolation ("+GROUP_NAME+"->"+SHORT_NAME+")", // Label 
			TrendGraphDataPoint.createGraphProps(TrendGraphDataPoint.Y_AXIS_SCALE_LABELS_NORMAL, CentralPersistReader.SampleType.MAX_OVER_SAMPLES),
			new String[] { "Snapshot Transactions", "Update Snapshot Transactions", "NonSnapshot Version Transactions", "Longest Transaction Running Time", "Update conflict ratio" }, 
			LabelType.Static,
			TrendGraphDataPoint.Category.SPACE,
			false, // is Percent Graph
			true,  // visible at start
			0,     // graph is valid from Server Version. 0 = All Versions; >0 = Valid from this version and above 
			-1);   // minimum height

		//-----
		addTrendGraph(GRAPH_NAME_LOG_CACHE_HIT_RATE,
			"Log Cache Hit Rate", // Menu CheckBox text
			"Log Cache Hit Rate, in Percent ("+GROUP_NAME+"->"+SHORT_NAME+")", // Label 
			TrendGraphDataPoint.createGraphProps(TrendGraphDataPoint.Y_AXIS_SCALE_LABELS_PERSEC, CentralPersistReader.SampleType.MAX_OVER_SAMPLES),
			null, 
			LabelType.Dynamic,
			TrendGraphDataPoint.Category.CACHE,
			false, // is Percent Graph
			true,  // visible at start
			0,     // graph is valid from Server Version. 0 = All Versions; >0 = Valid from this version and above 
			-1);   // minimum height

		//-----
		addTrendGraph(GRAPH_NAME_LOG_CACHE_READS,
			"Log Cache Reads", // Menu CheckBox text
			"Log Cache Reads per Sec ("+GROUP_NAME+"->"+SHORT_NAME+")", // Label 
			TrendGraphDataPoint.createGraphProps(TrendGraphDataPoint.Y_AXIS_SCALE_LABELS_PERSEC, CentralPersistReader.SampleType.MAX_OVER_SAMPLES),
			null, 
			LabelType.Dynamic,
			TrendGraphDataPoint.Category.CACHE,
			false, // is Percent Graph
			true,  // visible at start
			0,     // graph is valid from Server Version. 0 = All Versions; >0 = Valid from this version and above 
			-1);   // minimum height

		//-----
		addTrendGraph(GRAPH_NAME_LOG_POOL_REQUESTS,
			"Log Pool Requets", // Menu CheckBox text
			"Log Pool Requets per Sec ("+GROUP_NAME+"->"+SHORT_NAME+")", // Label 
			TrendGraphDataPoint.createGraphProps(TrendGraphDataPoint.Y_AXIS_SCALE_LABELS_PERSEC, CentralPersistReader.SampleType.MAX_OVER_SAMPLES),
			null, 
			LabelType.Dynamic,
			TrendGraphDataPoint.Category.DISK,
			false, // is Percent Graph
			true,  // visible at start
			0,     // graph is valid from Server Version. 0 = All Versions; >0 = Valid from this version and above 
			-1);   // minimum height

		//-----
		addTrendGraph(GRAPH_NAME_TRANS_SEC,
			"Transactions", // Menu CheckBox text
			"Transactions per Sec ("+GROUP_NAME+"->"+SHORT_NAME+")", // Label 
			TrendGraphDataPoint.createGraphProps(TrendGraphDataPoint.Y_AXIS_SCALE_LABELS_PERSEC, CentralPersistReader.SampleType.MAX_OVER_SAMPLES),
			null, 
			LabelType.Dynamic,
			TrendGraphDataPoint.Category.OPERATIONS,
			false, // is Percent Graph
			true,  // visible at start
			0,     // graph is valid from Server Version. 0 = All Versions; >0 = Valid from this version and above 
			-1);   // minimum height

		//-----
		addTrendGraph(GRAPH_NAME_TRANS_WRITE_SEC,
			"Write Transactions", // Menu CheckBox text
			"Write Transactions per Sec ("+GROUP_NAME+"->"+SHORT_NAME+")", // Label 
			TrendGraphDataPoint.createGraphProps(TrendGraphDataPoint.Y_AXIS_SCALE_LABELS_PERSEC, CentralPersistReader.SampleType.MAX_OVER_SAMPLES),
			null, 
			LabelType.Dynamic,
			TrendGraphDataPoint.Category.OPERATIONS,
			false, // is Percent Graph
			true,  // visible at start
			0,     // graph is valid from Server Version. 0 = All Versions; >0 = Valid from this version and above 
			-1);   // minimum height

		//-----
		addTrendGraph(GRAPH_NAME_TRANS_ACTIVE,
			"Active Transactions", // Menu CheckBox text
			"Active Transactions ("+GROUP_NAME+"->"+SHORT_NAME+")", // Label 
			TrendGraphDataPoint.createGraphProps(TrendGraphDataPoint.Y_AXIS_SCALE_LABELS_NORMAL, CentralPersistReader.SampleType.MAX_OVER_SAMPLES),
			null, 
			LabelType.Dynamic,
			TrendGraphDataPoint.Category.OPERATIONS,
			false, // is Percent Graph
			true,  // visible at start
			0,     // graph is valid from Server Version. 0 = All Versions; >0 = Valid from this version and above 
			-1);   // minimum height

		//-----
		addTrendGraph(GRAPH_NAME_COMPILE,
			"SQL Compilations", // Menu CheckBox text
			"SQL Compilations per Sec ("+GROUP_NAME+"->"+SHORT_NAME+")", // Label 
			TrendGraphDataPoint.createGraphProps(TrendGraphDataPoint.Y_AXIS_SCALE_LABELS_PERSEC, CentralPersistReader.SampleType.MAX_OVER_SAMPLES),
			new String[] { "SQL Compilations/sec" }, 
			LabelType.Static,
			TrendGraphDataPoint.Category.OPERATIONS,
			false, // is Percent Graph
			true,  // visible at start
			0,     // graph is valid from Server Version. 0 = All Versions; >0 = Valid from this version and above 
			-1);   // minimum height

		//-----
		addTrendGraph(GRAPH_NAME_RECOMPILE,
			"SQL Re-Compilations", // Menu CheckBox text
			"SQL Re-Compilations per Sec ("+GROUP_NAME+"->"+SHORT_NAME+")", // Label 
			TrendGraphDataPoint.createGraphProps(TrendGraphDataPoint.Y_AXIS_SCALE_LABELS_PERSEC, CentralPersistReader.SampleType.MAX_OVER_SAMPLES),
			new String[] { "SQL Re-Compilations/sec" }, 
			LabelType.Static,
			TrendGraphDataPoint.Category.OPERATIONS,
			false, // is Percent Graph
			true,  // visible at start
			0,     // graph is valid from Server Version. 0 = All Versions; >0 = Valid from this version and above 
			-1);   // minimum height

		//-----
		addTrendGraph(GRAPH_NAME_BLOCKED_PROCESSES,
			"Blocked Processes", // Menu CheckBox text
			"Blocked Processes ("+GROUP_NAME+"->"+SHORT_NAME+")", // Label 
			TrendGraphDataPoint.createGraphProps(TrendGraphDataPoint.Y_AXIS_SCALE_LABELS_NORMAL, CentralPersistReader.SampleType.MAX_OVER_SAMPLES),
			new String[] { "Processes blocked" }, 
			LabelType.Static,
			TrendGraphDataPoint.Category.LOCK,
			false, // is Percent Graph
			true,  // visible at start
			0,     // graph is valid from Server Version. 0 = All Versions; >0 = Valid from this version and above 
			-1);   // minimum height

		//-----
		addTrendGraph(GRAPH_NAME_PAGE_SPLITS,
			"Page Splits", // Menu CheckBox text
			"Page Splits per Sec ("+GROUP_NAME+"->"+SHORT_NAME+")", // Label 
			TrendGraphDataPoint.createGraphProps(TrendGraphDataPoint.Y_AXIS_SCALE_LABELS_PERSEC, CentralPersistReader.SampleType.MAX_OVER_SAMPLES),
			new String[] { "Page Splits/sec" }, 
			LabelType.Static,
			TrendGraphDataPoint.Category.OPERATIONS,
			false, // is Percent Graph
			true,  // visible at start
			0,     // graph is valid from Server Version. 0 = All Versions; >0 = Valid from this version and above 
			-1);   // minimum height

		//-----
		addTrendGraph(GRAPH_NAME_FWD_RIDS,
			"Forwarded Records", // Menu CheckBox text
			"Forwarded Records per Sec ("+GROUP_NAME+"->"+SHORT_NAME+")", // Label 
			TrendGraphDataPoint.createGraphProps(TrendGraphDataPoint.Y_AXIS_SCALE_LABELS_PERSEC, CentralPersistReader.SampleType.MAX_OVER_SAMPLES),
			new String[] { "Forwarded Records/sec" }, 
			LabelType.Static,
			TrendGraphDataPoint.Category.OPERATIONS,
			false, // is Percent Graph
			true,  // visible at start
			0,     // graph is valid from Server Version. 0 = All Versions; >0 = Valid from this version and above 
			-1);   // minimum height

		//-----
		addTrendGraph(GRAPH_NAME_ACCESS_METHODS,
			"Access Methods", // Menu CheckBox text
			"Access Methods per Sec ("+GROUP_NAME+"->"+SHORT_NAME+")", // Label 
			TrendGraphDataPoint.createGraphProps(TrendGraphDataPoint.Y_AXIS_SCALE_LABELS_PERSEC, CentralPersistReader.SampleType.MAX_OVER_SAMPLES),
			new String[] { "Full Scans/sec", "Range Scans/sec", "Probe Scans/sec" }, 
			LabelType.Static,
			TrendGraphDataPoint.Category.OPERATIONS,
			false, // is Percent Graph
			true,  // visible at start
			0,     // graph is valid from Server Version. 0 = All Versions; >0 = Valid from this version and above 
			-1);   // minimum height

		//-----
		addTrendGraph(GRAPH_NAME_LATCH_WAITS,
			"Latch Waits", // Menu CheckBox text
			"Latch Waits per Sec ("+GROUP_NAME+"->"+SHORT_NAME+")", // Label 
			TrendGraphDataPoint.createGraphProps(TrendGraphDataPoint.Y_AXIS_SCALE_LABELS_PERSEC, CentralPersistReader.SampleType.MAX_OVER_SAMPLES),
			new String[] { "Latch Waits/sec" }, 
			LabelType.Static,
			TrendGraphDataPoint.Category.WAITS,
			false, // is Percent Graph
			true,  // visible at start
			0,     // graph is valid from Server Version. 0 = All Versions; >0 = Valid from this version and above 
			-1);   // minimum height

		//-----
		addTrendGraph(GRAPH_NAME_LATCH_WAIT_TIME,
			"Average Latch Wait Time", // Menu CheckBox text
			"Average Latch Wait Time ("+GROUP_NAME+"->"+SHORT_NAME+")", // Label 
			TrendGraphDataPoint.createGraphProps(TrendGraphDataPoint.Y_AXIS_SCALE_LABELS_MILLISEC, CentralPersistReader.SampleType.MAX_OVER_SAMPLES),
			new String[] { "Average Latch Wait Time (ms)" }, 
			LabelType.Static,
			TrendGraphDataPoint.Category.WAITS,
			false, // is Percent Graph
			true,  // visible at start
			0,     // graph is valid from Server Version. 0 = All Versions; >0 = Valid from this version and above 
			-1);   // minimum height

// BEGIN --- FIX NAMES ON THE BELOW
		//-----
		addTrendGraph(GRAPH_NAME_FREE_LIST_STALLS,
			"Buffer Cache - Waiting for free pages per Sec", // Menu CheckBox text
			"Buffer Cache - Waiting for free pages per Sec ("+GROUP_NAME+"->"+SHORT_NAME+")", // Label 
			TrendGraphDataPoint.createGraphProps(TrendGraphDataPoint.Y_AXIS_SCALE_LABELS_NORMAL, CentralPersistReader.SampleType.MAX_OVER_SAMPLES),
			new String[] { "Free list stalls/sec" }, 
			LabelType.Static,
			TrendGraphDataPoint.Category.CACHE,
			false, // is Percent Graph
			false,  // visible at start
			0,     // graph is valid from Server Version. 0 = All Versions; >0 = Valid from this version and above 
			-1);   // minimum height

		//-----
		addTrendGraph(GRAPH_NAME_MEMORY_GRANTS_PENDING,
			"'Memory Grants Pending' per Sec", // Menu CheckBox text
			"'Memory Grants Pending' per Sec ("+GROUP_NAME+"->"+SHORT_NAME+")", // Label 
			TrendGraphDataPoint.createGraphProps(TrendGraphDataPoint.Y_AXIS_SCALE_LABELS_NORMAL, CentralPersistReader.SampleType.MAX_OVER_SAMPLES),
			new String[] { "Memory Grants Pending" }, 
			LabelType.Static,
			TrendGraphDataPoint.Category.MEMORY,
			false, // is Percent Graph
			false,  // visible at start
			0,     // graph is valid from Server Version. 0 = All Versions; >0 = Valid from this version and above 
			-1);   // minimum height
// END --- FIX NAMES ON THE BELOW

		
		addTrendGraph(GRAPH_NAME_PLAN_CACHE_HIT_RATE,
			"Plan Cache Hit Rate", // Menu CheckBox text
			"Plan Cache Hit Rate ("+GROUP_NAME+"->"+SHORT_NAME+")", // Label 
			TrendGraphDataPoint.createGraphProps(TrendGraphDataPoint.Y_AXIS_SCALE_LABELS_PERCENT, CentralPersistReader.SampleType.MAX_OVER_SAMPLES),
			new String[] { "SQL Plans", "Object Plans(sp/tr/func)", "Temporary Tables & Table Variables", "Bound Trees", "Extended Stored Procedures" }, 
			LabelType.Static,
			TrendGraphDataPoint.Category.CACHE,
			true, // is Percent Graph
			true,  // visible at start
			0,     // graph is valid from Server Version. 0 = All Versions; >0 = Valid from this version and above 
			-1);   // minimum height

		//-----
		addTrendGraph(GRAPH_NAME_PLAN_CACHE_MB,
			"Plan Cache Size in MB", // Menu CheckBox text
			"Plan Cache Size in MB ("+GROUP_NAME+"->"+SHORT_NAME+")", // Label 
			TrendGraphDataPoint.createGraphProps(TrendGraphDataPoint.Y_AXIS_SCALE_LABELS_MB, CentralPersistReader.SampleType.MAX_OVER_SAMPLES),
			new String[] { "SQL Plans", "Object Plans", "Temporary Tables & Table Variables", "Bound Trees", "Extended Stored Procedures" }, 
			LabelType.Static,
			TrendGraphDataPoint.Category.CACHE,
			false, // is Percent Graph
			true,  // visible at start
			0,     // graph is valid from Server Version. 0 = All Versions; >0 = Valid from this version and above 
			-1);   // minimum height

		//-----
		addTrendGraph(GRAPH_NAME_PLAN_CACHE_OBJ_CNT,
			"Plan Cache Object Count", // Menu CheckBox text
			"Plan Cache Object Count ("+GROUP_NAME+"->"+SHORT_NAME+")", // Label 
			TrendGraphDataPoint.createGraphProps(TrendGraphDataPoint.Y_AXIS_SCALE_LABELS_NORMAL, CentralPersistReader.SampleType.MAX_OVER_SAMPLES),
			new String[] { "SQL Plans", "Object Plans", "Temporary Tables & Table Variables", "Bound Trees", "Extended Stored Procedures" }, 
			LabelType.Static,
			TrendGraphDataPoint.Category.CACHE,
			false, // is Percent Graph
			false, // visible at start
			0,     // graph is valid from Server Version. 0 = All Versions; >0 = Valid from this version and above 
			-1);   // minimum height

		//-----
		addTrendGraph(GRAPH_NAME_PLAN_CACHE_OBJ_USE,
			"Plan Cache Objects In Use", // Menu CheckBox text
			"Plan Cache Objects In Use ("+GROUP_NAME+"->"+SHORT_NAME+")", // Label 
			TrendGraphDataPoint.createGraphProps(TrendGraphDataPoint.Y_AXIS_SCALE_LABELS_NORMAL, CentralPersistReader.SampleType.MAX_OVER_SAMPLES),
			new String[] { "SQL Plans", "Object Plans", "Temporary Tables & Table Variables", "Bound Trees", "Extended Stored Procedures" }, 
			LabelType.Static,
			TrendGraphDataPoint.Category.CACHE,
			false, // is Percent Graph
			false, // visible at start
			0,     // graph is valid from Server Version. 0 = All Versions; >0 = Valid from this version and above 
			-1);   // minimum height

		//-----
		addTrendGraph(GRAPH_NAME_MEMORY_MANAGER,
			"Memory Manager", // Menu CheckBox text
			"Memory Manager ("+GROUP_NAME+"->"+SHORT_NAME+")", // Label 
			TrendGraphDataPoint.createGraphProps(TrendGraphDataPoint.Y_AXIS_SCALE_LABELS_NORMAL, CentralPersistReader.SampleType.MAX_OVER_SAMPLES),
			null, 
			LabelType.Dynamic,
			TrendGraphDataPoint.Category.MEMORY,
			false, // is Percent Graph
			false, // visible at start
			0,     // graph is valid from Server Version. 0 = All Versions; >0 = Valid from this version and above 
			-1);   // minimum height

		//-----
		addTrendGraph(GRAPH_NAME_MEMORY_TARGET_VS_TOTAL,
			"Target vs Total Memory", // Menu CheckBox text
			"Target vs Total Memory ("+GROUP_NAME+"->"+SHORT_NAME+")", // Label 
			TrendGraphDataPoint.createGraphProps(TrendGraphDataPoint.Y_AXIS_SCALE_LABELS_NORMAL, CentralPersistReader.SampleType.MAX_OVER_SAMPLES),
			new String[] { "Target Server Memory (KB)", "Total Server Memory (KB)" }, 
			LabelType.Static,
			TrendGraphDataPoint.Category.MEMORY,
			false, // is Percent Graph
			false, // visible at start
			0,     // graph is valid from Server Version. 0 = All Versions; >0 = Valid from this version and above 
			-1);   // minimum height

		//-----
		addTrendGraph(GRAPH_NAME_MEMORY_STOLEN,
			"Stolen Server Memory, from Buffer Pool", // Menu CheckBox text
			"Stolen Server Memory, from Buffer Pool ("+GROUP_NAME+"->"+SHORT_NAME+")", // Label 
			TrendGraphDataPoint.createGraphProps(TrendGraphDataPoint.Y_AXIS_SCALE_LABELS_MB, CentralPersistReader.SampleType.MAX_OVER_SAMPLES),
			new String[] { "Stolen Server Memory, in MB" }, 
			LabelType.Static,
			TrendGraphDataPoint.Category.MEMORY,
			false, // is Percent Graph
			true,  // visible at start
			0,     // graph is valid from Server Version. 0 = All Versions; >0 = Valid from this version and above 
			-1);   // minimum height

		//-----
		addTrendGraph(GRAPH_NAME_QUERY_STORE_TOTAL,
			"Query Story Total", // Menu CheckBox text
			"Query Store Total ("+GROUP_NAME+"->"+SHORT_NAME+")", // Label 
			TrendGraphDataPoint.createGraphProps(TrendGraphDataPoint.Y_AXIS_SCALE_LABELS_NORMAL, CentralPersistReader.SampleType.MAX_OVER_SAMPLES),
			new String[] { "CPU Usage", "Physical Reads", "Logical Reads", "Logical Writes" }, 
			LabelType.Static,
			TrendGraphDataPoint.Category.OTHER,
			false,         // is Percent Graph
			false,         // visible at start
			Ver.ver(2016), // graph is valid from Server Version. 0 = All Versions; >0 = Valid from this version and above 
			-1);           // minimum height

		//-----
		addTrendGraph(GRAPH_NAME_QUERY_STORE_DB_CPU,
			"Query Story DB - CPU", // Menu CheckBox text
			"Query Store DB - CPU ("+GROUP_NAME+"->"+SHORT_NAME+")", // Label 
			TrendGraphDataPoint.createGraphProps(TrendGraphDataPoint.Y_AXIS_SCALE_LABELS_NORMAL, CentralPersistReader.SampleType.MAX_OVER_SAMPLES),
			null, 
			LabelType.Dynamic,
			TrendGraphDataPoint.Category.OTHER,
			false,         // is Percent Graph
			false,         // visible at start
			Ver.ver(2016), // graph is valid from Server Version. 0 = All Versions; >0 = Valid from this version and above 
			-1);           // minimum height

		//-----
		addTrendGraph(GRAPH_NAME_QUERY_STORE_DB_PHYS_READ,
			"Query Story DB - Physical Reads", // Menu CheckBox text
			"Query Store DB - Physical Reads ("+GROUP_NAME+"->"+SHORT_NAME+")", // Label 
			TrendGraphDataPoint.createGraphProps(TrendGraphDataPoint.Y_AXIS_SCALE_LABELS_NORMAL, CentralPersistReader.SampleType.MAX_OVER_SAMPLES),
			null, 
			LabelType.Dynamic,
			TrendGraphDataPoint.Category.OTHER,
			false,         // is Percent Graph
			false,         // visible at start
			Ver.ver(2016), // graph is valid from Server Version. 0 = All Versions; >0 = Valid from this version and above 
			-1);           // minimum height

		//-----
		addTrendGraph(GRAPH_NAME_QUERY_STORE_DB_LOGI_READ,
			"Query Story DB - Logical Reads", // Menu CheckBox text
			"Query Store DB - Logical Reads ("+GROUP_NAME+"->"+SHORT_NAME+")", // Label 
			TrendGraphDataPoint.createGraphProps(TrendGraphDataPoint.Y_AXIS_SCALE_LABELS_NORMAL, CentralPersistReader.SampleType.MAX_OVER_SAMPLES),
			null, 
			LabelType.Dynamic,
			TrendGraphDataPoint.Category.OTHER,
			false,         // is Percent Graph
			false,         // visible at start
			Ver.ver(2016), // graph is valid from Server Version. 0 = All Versions; >0 = Valid from this version and above 
			-1);           // minimum height

		//-----
		addTrendGraph(GRAPH_NAME_QUERY_STORE_DB_LOGI_WRITE,
			"Query Story DB - Logical Writes", // Menu CheckBox text
			"Query Store DB - Logical Writes ("+GROUP_NAME+"->"+SHORT_NAME+")", // Label 
			TrendGraphDataPoint.createGraphProps(TrendGraphDataPoint.Y_AXIS_SCALE_LABELS_NORMAL, CentralPersistReader.SampleType.MAX_OVER_SAMPLES),
			null, 
			LabelType.Dynamic,
			TrendGraphDataPoint.Category.OTHER,
			false,         // is Percent Graph
			false,         // visible at start
			Ver.ver(2016), // graph is valid from Server Version. 0 = All Versions; >0 = Valid from this version and above 
			-1);           // minimum height

		//-----
		addTrendGraph(GRAPH_NAME_DEADLOCK_DETAILS,
			"Deadlock Count Details", // Menu CheckBox text
			"Deadlock Count Details ("+GROUP_NAME+"->"+SHORT_NAME+")", // Label 
			TrendGraphDataPoint.createGraphProps(TrendGraphDataPoint.Y_AXIS_SCALE_LABELS_NORMAL, CentralPersistReader.SampleType.MAX_OVER_SAMPLES),
			null, 
			LabelType.Dynamic,
			TrendGraphDataPoint.Category.LOCK,
			false,         // is Percent Graph
			false,         // visible at start
			0,             // graph is valid from Server Version. 0 = All Versions; >0 = Valid from this version and above 
			-1);           // minimum height

		//-----
		addTrendGraph(GRAPH_NAME_COLUMNSTORE_ALL,
			"Column Store Total", // Menu CheckBox text
			"Column Store Total ("+GROUP_NAME+"->"+SHORT_NAME+")", // Label 
			TrendGraphDataPoint.createGraphProps(TrendGraphDataPoint.Y_AXIS_SCALE_LABELS_NORMAL, CentralPersistReader.SampleType.MAX_OVER_SAMPLES),
			new String[] { 
					"Delta Rowgroups Created", 
					"Delta Rowgroups Closed", 
					"Delta Rowgroups Compressed", 
					"Total Rowgroups Compressed", 
					"Total Delete Buffers Migrated", 
					"Total Merge Policy Evaluations", 
					"Total Source Rowgroups Merged", 
					"Total Rowgroups Merge Compressed", 
					"Total Rowgroups Fit For Merge", 
					"Segment Cache Hit Ratio", 
					"Segment Reads/Sec" }, 
			LabelType.Static,
			TrendGraphDataPoint.Category.OTHER,
			false,         // is Percent Graph
			false,         // visible at start
			Ver.ver(2016), // graph is valid from Server Version. 0 = All Versions; >0 = Valid from this version and above 
			-1);           // minimum height

		//-----
		addTrendGraph(GRAPH_NAME_SQL_ERRORS,
			"SQL Errors", // Menu CheckBox text
			"SQL Errors ("+GROUP_NAME+"->"+SHORT_NAME+")", // Label 
			TrendGraphDataPoint.createGraphProps(TrendGraphDataPoint.Y_AXIS_SCALE_LABELS_NORMAL, CentralPersistReader.SampleType.MAX_OVER_SAMPLES),
			new String[] { 
					"Total", 
					"Info Errors", 
					"User Errors", 
					"Kill Connection Errors", 
					"DB Offline Errors" 
			}, 
			LabelType.Static,
			TrendGraphDataPoint.Category.OTHER,
			false,         // is Percent Graph
			false,         // visible at start
			0,             // graph is valid from Server Version. 0 = All Versions; >0 = Valid from this version and above 
			-1);           // minimum height
	}

	@Override
	public boolean isGraphDataHistoryEnabled(String name)
	{
		// ENABLED for the following graphs
		if (GRAPH_NAME_DEADLOCK_DETAILS.equals(name)) return true; // Used by CmSummary

		// default: DISABLED
		return false;
	}
	@Override
	public int getGraphDataHistoryTimeInterval(String name)
	{
		// Keep interval: default is 60 minutes
		return super.getGraphDataHistoryTimeInterval(name);
	}
	
	@Override
	public void updateGraphData(TrendGraphDataPoint tgdp)
	{
//		long srvVersion = getServerVersion();

//		// -----------------------------------------------------------------------------------------
//		if (GRAPH_NAME_CPU_PCT.equals(tgdp.getName()))
//		{
//			Double[] arr = new Double[1];
//			
//			// Note the prefix: 'SQLServer' or 'MSSQL$@@servicename' is removed in SQL query
//			String pk     = createPkStr(":Workload Group Stats", "CPU usage %",      "default");
//			String pkBase = createPkStr(":Workload Group Stats", "CPU usage % base", "default");
//			
//			Number cpuUsage     = (Number) this.getAbsValue(pk,     "cntr_value");
//			Number cpuUsageBase = (Number) this.getAbsValue(pkBase, "cntr_value");
//			
//			if (cpuUsage != null && cpuUsageBase != null)
//			{
//				// Calculate
//				arr[0] = cpuUsage.doubleValue() / cpuUsageBase.doubleValue() * 100.0;
////System.out.println(tgdp.getName()+": calc="+arr[0]+", cpuUsage="+cpuUsage+", cpuUsageBase="+cpuUsageBase);
//
//				// Set the values
//				tgdp.setDataPoint(this.getTimestamp(), arr);
//			}
//			else
//			{
//				TrendGraph tg = getTrendGraph(tgdp.getName());
//				if (tg != null)
//					tg.setWarningLabel("Failed to get value(s) for pk-row: '"+pk+"'='"+cpuUsage+"', or '"+pkBase+"'="+cpuUsageBase+"'.");
//			}
//		}

		// -----------------------------------------------------------------------------------------
		if (GRAPH_NAME_CPU_PCT.equals(tgdp.getName()))
		{
			Double[] arr = new Double[1];
			
			// Note the prefix: 'SQLServer' or 'MSSQL$@@servicename' is removed in SQL query
			String pk     = createPkStr(":Workload Group Stats", "CPU usage %", "default");
			
			Double val = this.getAbsValueAsDouble(pk, "calculated_value");

			// If no data try another PK ("default" -> "internal")
			if (val == null)
			{
				pk  = createPkStr(":Workload Group Stats", "CPU usage %", "internal");
				val = this.getAbsValueAsDouble(pk, "calculated_value");
			}
			
			if (val != null)
			{
				arr[0 ] = val;

				// Set the values
				tgdp.setDataPoint(this.getTimestamp(), arr);
			}
			else
			{
				TrendGraph tg = getTrendGraph(tgdp.getName());
				if (tg != null)
					tg.setWarningLabel("Failed to get value(s) for pk-row: '"+pk+"'='"+val+"'.");
			}
		}

		// -----------------------------------------------------------------------------------------
		if (GRAPH_NAME_CPU_EFFECTIVE_PCT.equals(tgdp.getName()))
		{
			Double[] arr = new Double[1];
			
			// Note the prefix: 'SQLServer' or 'MSSQL$@@servicename' is removed in SQL query
			String pk     = createPkStr(":Workload Group Stats", "CPU effective %", "default");
			
			Double val = this.getAbsValueAsDouble(pk, "calculated_value");
			
			// If no data try another PK ("default" -> "internal")
			if (val == null)
			{
				pk  = createPkStr(":Workload Group Stats", "CPU effective %", "internal");
				val = this.getAbsValueAsDouble(pk, "calculated_value");
			}
			
			if (val != null)
			{
				arr[0 ] = val;

				// Set the values
				tgdp.setDataPoint(this.getTimestamp(), arr);
			}
			else
			{
				TrendGraph tg = getTrendGraph(tgdp.getName());
				if (tg != null)
					tg.setWarningLabel("Failed to get value(s) for pk-row: '"+pk+"'='"+val+"'.");
			}
		}

		// -----------------------------------------------------------------------------------------
		if (GRAPH_NAME_SQL_BATCHES_SEC.equals(tgdp.getName()))
		{
			// TODO: rewrite to use: this.getAbsValueAsDouble(pk, "calculated_value");
			Double[] arr = new Double[1];

			// Note the prefix: 'SQLServer' or 'MSSQL$@@servicename' is removed in SQL query
			String pk = createPkStr(":SQL Statistics", "Batch Requests/sec", "");
			Double val = this.getRateValueAsDouble(pk, "cntr_value");
			
			if (val != null)
			{
				// Calculate
				arr[0] = val;
//System.out.println(tgdp.getName()+": val="+arr[0]);

				// Set the values
				tgdp.setDataPoint(this.getTimestamp(), arr);
			}
			else
			{
				TrendGraph tg = getTrendGraph(tgdp.getName());
				if (tg != null)
					tg.setWarningLabel("Failed to get value for pk-row: '"+pk+"'='"+val+"'.");
			}
		}

		// -----------------------------------------------------------------------------------------
		if (GRAPH_NAME_SQL_BATCHES_TIME_SPAN_ALL.equals(tgdp.getName()))
		{
			// TODO: rewrite to use: this.getAbsValueAsDouble(pk, "calculated_value");
			Double[] arr = new Double[17];

			// Note the prefix: 'SQLServer' or 'MSSQL$@@servicename' is removed in SQL query
			String pk1  = createPkStr(":Batch Resp Statistics", "Batches >=000000ms & <000001ms", "Elapsed Time:Requests");
			String pk2  = createPkStr(":Batch Resp Statistics", "Batches >=000001ms & <000002ms", "Elapsed Time:Requests");
			String pk3  = createPkStr(":Batch Resp Statistics", "Batches >=000002ms & <000005ms", "Elapsed Time:Requests");
			String pk4  = createPkStr(":Batch Resp Statistics", "Batches >=000005ms & <000010ms", "Elapsed Time:Requests");
			String pk5  = createPkStr(":Batch Resp Statistics", "Batches >=000010ms & <000020ms", "Elapsed Time:Requests");
			String pk6  = createPkStr(":Batch Resp Statistics", "Batches >=000020ms & <000050ms", "Elapsed Time:Requests");
			String pk7  = createPkStr(":Batch Resp Statistics", "Batches >=000050ms & <000100ms", "Elapsed Time:Requests");
			String pk8  = createPkStr(":Batch Resp Statistics", "Batches >=000100ms & <000200ms", "Elapsed Time:Requests");
			String pk9  = createPkStr(":Batch Resp Statistics", "Batches >=000200ms & <000500ms", "Elapsed Time:Requests");
			String pk10 = createPkStr(":Batch Resp Statistics", "Batches >=000500ms & <001000ms", "Elapsed Time:Requests");
			String pk11 = createPkStr(":Batch Resp Statistics", "Batches >=001000ms & <002000ms", "Elapsed Time:Requests");
			String pk12 = createPkStr(":Batch Resp Statistics", "Batches >=002000ms & <005000ms", "Elapsed Time:Requests");
			String pk13 = createPkStr(":Batch Resp Statistics", "Batches >=005000ms & <010000ms", "Elapsed Time:Requests");
			String pk14 = createPkStr(":Batch Resp Statistics", "Batches >=010000ms & <020000ms", "Elapsed Time:Requests");
			String pk15 = createPkStr(":Batch Resp Statistics", "Batches >=020000ms & <050000ms", "Elapsed Time:Requests");
			String pk16 = createPkStr(":Batch Resp Statistics", "Batches >=050000ms & <100000ms", "Elapsed Time:Requests");
			String pk17 = createPkStr(":Batch Resp Statistics", "Batches >=100000ms",             "Elapsed Time:Requests");

			Double val1  = this.getRateValueAsDouble(pk1 , "cntr_value");
			Double val2  = this.getRateValueAsDouble(pk2 , "cntr_value");
			Double val3  = this.getRateValueAsDouble(pk3 , "cntr_value");
			Double val4  = this.getRateValueAsDouble(pk4 , "cntr_value");
			Double val5  = this.getRateValueAsDouble(pk5 , "cntr_value");
			Double val6  = this.getRateValueAsDouble(pk6 , "cntr_value");
			Double val7  = this.getRateValueAsDouble(pk7 , "cntr_value");
			Double val8  = this.getRateValueAsDouble(pk8 , "cntr_value");
			Double val9  = this.getRateValueAsDouble(pk9 , "cntr_value");
			Double val10 = this.getRateValueAsDouble(pk10, "cntr_value");
			Double val11 = this.getRateValueAsDouble(pk11, "cntr_value");
			Double val12 = this.getRateValueAsDouble(pk12, "cntr_value");
			Double val13 = this.getRateValueAsDouble(pk13, "cntr_value");
			Double val14 = this.getRateValueAsDouble(pk14, "cntr_value");
			Double val15 = this.getRateValueAsDouble(pk15, "cntr_value");
			Double val16 = this.getRateValueAsDouble(pk16, "cntr_value");
			Double val17 = this.getRateValueAsDouble(pk17, "cntr_value");
			
			if (val1 != null && val2 != null && val3 != null && val4 != null && val5 != null && val6 != null && val7 != null && val8 != null && val9 != null && val10 != null && val11 != null && val12 != null && val13 != null && val14 != null && val15 != null && val16 != null && val17 != null)
			{
				// Calculate
				arr[0 ] = val1 ;
				arr[1 ] = val2 ;
				arr[2 ] = val3 ;
				arr[3 ] = val4 ;
				arr[4 ] = val5 ;
				arr[5 ] = val6 ;
				arr[6 ] = val7 ;
				arr[7 ] = val8 ;
				arr[8 ] = val9 ;
				arr[9 ] = val10;
				arr[10] = val11;
				arr[11] = val12;
				arr[12] = val13;
				arr[13] = val14;
				arr[14] = val15;
				arr[15] = val16;
				arr[16] = val17;

				// Set the values
				tgdp.setDataPoint(this.getTimestamp(), arr);
			}
			else
			{
				TrendGraph tg = getTrendGraph(tgdp.getName());
				if (tg != null)
					tg.setWarningLabel("Failed to get value for 'some' pk-row: '"+pk1+"'='"+val1+"'.");
			}
		}

		// -----------------------------------------------------------------------------------------
		if (GRAPH_NAME_SQL_BATCHES_TIME_SPAN_0.equals(tgdp.getName()))
		{
			// TODO: rewrite to use: this.getAbsValueAsDouble(pk, "calculated_value");
			Double[] arr = new Double[4];

			// Note the prefix: 'SQLServer' or 'MSSQL$@@servicename' is removed in SQL query
			String pk1  = createPkStr(":Batch Resp Statistics", "Batches >=000000ms & <000001ms", "Elapsed Time:Requests");
			String pk2  = createPkStr(":Batch Resp Statistics", "Batches >=000001ms & <000002ms", "Elapsed Time:Requests");
			String pk3  = createPkStr(":Batch Resp Statistics", "Batches >=000002ms & <000005ms", "Elapsed Time:Requests");
			String pk4  = createPkStr(":Batch Resp Statistics", "Batches >=000005ms & <000010ms", "Elapsed Time:Requests");

			Double val1  = this.getRateValueAsDouble(pk1 , "cntr_value");
			Double val2  = this.getRateValueAsDouble(pk2 , "cntr_value");
			Double val3  = this.getRateValueAsDouble(pk3 , "cntr_value");
			Double val4  = this.getRateValueAsDouble(pk4 , "cntr_value");
			
			if (val1 != null && val2 != null && val3 != null && val4 != null)
			{
				// Calculate
				arr[0 ] = val1 ;
				arr[1 ] = val2 ;
				arr[2 ] = val3 ;
				arr[3 ] = val4 ;

				// Set the values
				tgdp.setDataPoint(this.getTimestamp(), arr);
			}
			else
			{
				TrendGraph tg = getTrendGraph(tgdp.getName());
				if (tg != null)
					tg.setWarningLabel("Failed to get value for 'some' pk-row: '"+pk1+"'='"+val1+"'.");
			}
		}

		// -----------------------------------------------------------------------------------------
		if (GRAPH_NAME_SQL_BATCHES_TIME_SPAN_1.equals(tgdp.getName()))
		{
			// TODO: rewrite to use: this.getAbsValueAsDouble(pk, "calculated_value");
			Double[] arr = new Double[3];

			// Note the prefix: 'SQLServer' or 'MSSQL$@@servicename' is removed in SQL query
			String pk5  = createPkStr(":Batch Resp Statistics", "Batches >=000010ms & <000020ms", "Elapsed Time:Requests");
			String pk6  = createPkStr(":Batch Resp Statistics", "Batches >=000020ms & <000050ms", "Elapsed Time:Requests");
			String pk7  = createPkStr(":Batch Resp Statistics", "Batches >=000050ms & <000100ms", "Elapsed Time:Requests");

			Double val5  = this.getRateValueAsDouble(pk5 , "cntr_value");
			Double val6  = this.getRateValueAsDouble(pk6 , "cntr_value");
			Double val7  = this.getRateValueAsDouble(pk7 , "cntr_value");
			
			if (val5 != null && val6 != null && val7 != null)
			{
				// Calculate
				arr[0 ] = val5 ;
				arr[1 ] = val6 ;
				arr[2 ] = val7 ;

				// Set the values
				tgdp.setDataPoint(this.getTimestamp(), arr);
			}
			else
			{
				TrendGraph tg = getTrendGraph(tgdp.getName());
				if (tg != null)
					tg.setWarningLabel("Failed to get value for 'some' pk-row: '"+pk5+"'='"+val5+"'.");
			}
		}

		// -----------------------------------------------------------------------------------------
		if (GRAPH_NAME_SQL_BATCHES_TIME_SPAN_2.equals(tgdp.getName()))
		{
			// TODO: rewrite to use: this.getAbsValueAsDouble(pk, "calculated_value");
			Double[] arr = new Double[3];

			// Note the prefix: 'SQLServer' or 'MSSQL$@@servicename' is removed in SQL query
			String pk8  = createPkStr(":Batch Resp Statistics", "Batches >=000100ms & <000200ms", "Elapsed Time:Requests");
			String pk9  = createPkStr(":Batch Resp Statistics", "Batches >=000200ms & <000500ms", "Elapsed Time:Requests");
			String pk10 = createPkStr(":Batch Resp Statistics", "Batches >=000500ms & <001000ms", "Elapsed Time:Requests");

			Double val8  = this.getRateValueAsDouble(pk8 , "cntr_value");
			Double val9  = this.getRateValueAsDouble(pk9 , "cntr_value");
			Double val10 = this.getRateValueAsDouble(pk10, "cntr_value");
			
			if (val8 != null && val9 != null && val10 != null)
			{
				// Calculate
				arr[0 ] = val8 ;
				arr[1 ] = val9 ;
				arr[2 ] = val10;

				// Set the values
				tgdp.setDataPoint(this.getTimestamp(), arr);
			}
			else
			{
				TrendGraph tg = getTrendGraph(tgdp.getName());
				if (tg != null)
					tg.setWarningLabel("Failed to get value for 'some' pk-row: '"+pk8+"'='"+val8+"'.");
			}
		}

		// -----------------------------------------------------------------------------------------
		if (GRAPH_NAME_SQL_BATCHES_TIME_SPAN_3.equals(tgdp.getName()))
		{
			// TODO: rewrite to use: this.getAbsValueAsDouble(pk, "calculated_value");
			Double[] arr = new Double[3];

			// Note the prefix: 'SQLServer' or 'MSSQL$@@servicename' is removed in SQL query
			String pk11 = createPkStr(":Batch Resp Statistics", "Batches >=001000ms & <002000ms", "Elapsed Time:Requests");
			String pk12 = createPkStr(":Batch Resp Statistics", "Batches >=002000ms & <005000ms", "Elapsed Time:Requests");
			String pk13 = createPkStr(":Batch Resp Statistics", "Batches >=005000ms & <010000ms", "Elapsed Time:Requests");

			Double val11 = this.getRateValueAsDouble(pk11, "cntr_value");
			Double val12 = this.getRateValueAsDouble(pk12, "cntr_value");
			Double val13 = this.getRateValueAsDouble(pk13, "cntr_value");
			
			if (val11 != null && val12 != null && val13 != null)
			{
				// Calculate
				arr[0 ] = val11;
				arr[1 ] = val12;
				arr[2 ] = val13;

				// Set the values
				tgdp.setDataPoint(this.getTimestamp(), arr);
			}
			else
			{
				TrendGraph tg = getTrendGraph(tgdp.getName());
				if (tg != null)
					tg.setWarningLabel("Failed to get value for 'some' pk-row: '"+pk11+"'='"+val11+"'.");
			}
		}

		// -----------------------------------------------------------------------------------------
		if (GRAPH_NAME_SQL_BATCHES_TIME_SPAN_4.equals(tgdp.getName()))
		{
			// TODO: rewrite to use: this.getAbsValueAsDouble(pk, "calculated_value");
			Double[] arr = new Double[4];

			// Note the prefix: 'SQLServer' or 'MSSQL$@@servicename' is removed in SQL query
			String pk14 = createPkStr(":Batch Resp Statistics", "Batches >=010000ms & <020000ms", "Elapsed Time:Requests");
			String pk15 = createPkStr(":Batch Resp Statistics", "Batches >=020000ms & <050000ms", "Elapsed Time:Requests");
			String pk16 = createPkStr(":Batch Resp Statistics", "Batches >=050000ms & <100000ms", "Elapsed Time:Requests");
			String pk17 = createPkStr(":Batch Resp Statistics", "Batches >=100000ms",             "Elapsed Time:Requests");

			Double val14 = this.getDiffValueAsDouble(pk14, "cntr_value");
			Double val15 = this.getDiffValueAsDouble(pk15, "cntr_value");
			Double val16 = this.getDiffValueAsDouble(pk16, "cntr_value");
			Double val17 = this.getDiffValueAsDouble(pk17, "cntr_value");
			
			if (val14 != null && val15 != null && val16 != null && val17 != null)
			{
				// Calculate
				arr[0] = val14;
				arr[1] = val15;
				arr[2] = val16;
				arr[3] = val17;

				// Set the values
				tgdp.setDataPoint(this.getTimestamp(), arr);
			}
			else
			{
				TrendGraph tg = getTrendGraph(tgdp.getName());
				if (tg != null)
					tg.setWarningLabel("Failed to get value for 'some' pk-row: '"+pk14+"'='"+val14+"'.");
			}
		}

		// -----------------------------------------------------------------------------------------
		if (GRAPH_NAME_CACHE_HIT_RATE.equals(tgdp.getName()))
		{
			Double[] arr = new Double[1];
			
			// Note the prefix: 'SQLServer' or 'MSSQL$@@servicename' is removed in SQL query
			String pk     = createPkStr(":Buffer Manager", "Buffer cache hit ratio", "");
			
			Double val = this.getAbsValueAsDouble(pk, "calculated_value");
			
			if (val != null)
			{
				arr[0 ] = val;

				// Set the values
				tgdp.setDataPoint(this.getTimestamp(), arr);
			}
			else
			{
				TrendGraph tg = getTrendGraph(tgdp.getName());
				if (tg != null)
					tg.setWarningLabel("Failed to get value(s) for pk-row: '"+pk+"'='"+val+"'.");
			}
		}

		// -----------------------------------------------------------------------------------------
		if (GRAPH_NAME_CACHE_READS.equals(tgdp.getName()))
		{
			Double[] arr = new Double[3];
			
			// Note the prefix: 'SQLServer' or 'MSSQL$@@servicename' is removed in SQL query
			String pk1 = createPkStr(":Buffer Manager", "Page lookups/sec", "");
			String pk2 = createPkStr(":Buffer Manager", "Page reads/sec", "");
			String pk3 = createPkStr(":Buffer Manager", "Readahead pages/sec", "");
			
			Double val1 = this.getAbsValueAsDouble(pk1, "calculated_value");
			Double val2 = this.getAbsValueAsDouble(pk2, "calculated_value");
			Double val3 = this.getAbsValueAsDouble(pk3, "calculated_value");
			
			if (val1 != null && val2 != null && val3 != null)
			{
				arr[0] = val1;
				arr[1] = val2;
				arr[2] = val3;

				// Set the values
				tgdp.setDataPoint(this.getTimestamp(), arr);
			}
			else
			{
				TrendGraph tg = getTrendGraph(tgdp.getName());
				if (tg != null)
					tg.setWarningLabel("Failed to get value(s) for pk-row: '"+pk1+"'='"+val1+"', '"+pk2+"'='"+val2+"', '"+pk3+"'='"+val3+"'.");
			}
		}

		// -----------------------------------------------------------------------------------------
		if (GRAPH_NAME_CACHE_PHY_READS.equals(tgdp.getName()))
		{
			Double[] arr = new Double[2];
			
			// Note the prefix: 'SQLServer' or 'MSSQL$@@servicename' is removed in SQL query
			String pk1 = createPkStr(":Buffer Manager", "Page reads/sec", "");
			String pk2 = createPkStr(":Buffer Manager", "Readahead pages/sec", "");
			
			Double val1 = this.getAbsValueAsDouble(pk1, "calculated_value");
			Double val2 = this.getAbsValueAsDouble(pk2, "calculated_value");
			
			if (val1 != null && val2 != null)
			{
				arr[0] = val1;
				arr[1] = val2;

				// Set the values
				tgdp.setDataPoint(this.getTimestamp(), arr);
			}
			else
			{
				TrendGraph tg = getTrendGraph(tgdp.getName());
				if (tg != null)
					tg.setWarningLabel("Failed to get value(s) for pk-row: '"+pk1+"'='"+val1+"', '"+pk2+"'='"+val2+"'.");
			}
		}

		// -----------------------------------------------------------------------------------------
		if (GRAPH_NAME_CACHE_PHY_READ_AHEAD_PCT.equals(tgdp.getName()))
		{
			Double[] arr = new Double[1];

			// Note the prefix: 'SQLServer' or 'MSSQL$@@servicename' is removed in SQL query
			String pk1 = createPkStr(":Buffer Manager", "Page reads/sec", "");
			String pk2 = createPkStr(":Buffer Manager", "Readahead pages/sec", "");
			
			Double PageReadsPerSec      = this.getAbsValueAsDouble(pk1, "calculated_value");
			Double ReadaheadPagesPerSec = this.getAbsValueAsDouble(pk2, "calculated_value");
			
			if (PageReadsPerSec != null && PageReadsPerSec != null)
			{
				double raedaheadPct = 0d;
				if (PageReadsPerSec > 0) 
					raedaheadPct = NumberUtils.round(ReadaheadPagesPerSec / PageReadsPerSec * 100.0, 1);

				arr[0] = raedaheadPct;

				// Set the values
				tgdp.setDataPoint(this.getTimestamp(), arr);
			}
			else
			{
				TrendGraph tg = getTrendGraph(tgdp.getName());
				if (tg != null)
					tg.setWarningLabel("Failed to get value(s) for pk-row: '"+pk1+"'='"+PageReadsPerSec+"', '"+pk2+"'='"+ReadaheadPagesPerSec+"'.");
			}
		}

		// -----------------------------------------------------------------------------------------
		if (GRAPH_NAME_CACHE_WRITES.equals(tgdp.getName()))
		{
			Double[] arr = new Double[4];
			
			// Note the prefix: 'SQLServer' or 'MSSQL$@@servicename' is removed in SQL query
			String pk1 = createPkStr(":Buffer Manager", "Page writes/sec",             "");
			String pk2 = createPkStr(":Buffer Manager", "Checkpoint pages/sec",        "");
			String pk3 = createPkStr(":Buffer Manager", "Background writer pages/sec", "");
			String pk4 = createPkStr(":Buffer Manager", "Lazy writes/sec",             "");
			
			
			Double val1 = this.getAbsValueAsDouble(pk1, "calculated_value");
			Double val2 = this.getAbsValueAsDouble(pk2, "calculated_value");
			Double val3 = this.getAbsValueAsDouble(pk3, "calculated_value");
			Double val4 = this.getAbsValueAsDouble(pk4, "calculated_value");
			
			if (val1 != null && val2 != null && val3 != null)
			{
				arr[0] = val1;
				arr[1] = val2;
				arr[2] = val3;
				arr[3] = val4;

				// Set the values
				tgdp.setDataPoint(this.getTimestamp(), arr);
			}
			else
			{
				TrendGraph tg = getTrendGraph(tgdp.getName());
				if (tg != null)
					tg.setWarningLabel("Failed to get value(s) for pk-row: '"+pk1+"'='"+val1+"', '"+pk2+"'='"+val2+"', '"+pk3+"'='"+val3+"', '"+pk4+"'='"+val4+"'.");
			}
		}

		// -----------------------------------------------------------------------------------------
		if (GRAPH_NAME_CACHE_DATA_SIZE.equals(tgdp.getName()))
		{
			Double[] arr = new Double[1];
			
			// Note the prefix: 'SQLServer' or 'MSSQL$@@servicename' is removed in SQL query
			String pk1 = createPkStr(":Buffer Manager", "Database pages", "");
			
			
			Double val1 = this.getAbsValueAsDouble(pk1, "calculated_value");
			
			if (val1 != null)
			{
				arr[0] = new BigDecimal( val1 / 128.0 ).setScale(1, BigDecimal.ROUND_HALF_UP).doubleValue();

				// Set the values
				tgdp.setDataPoint(this.getTimestamp(), arr);
			}
			else
			{
				TrendGraph tg = getTrendGraph(tgdp.getName());
				if (tg != null)
					tg.setWarningLabel("Failed to get value(s) for pk-row: '"+pk1+"'='"+val1+"'.");
			}
		}

		// -----------------------------------------------------------------------------------------
		if (GRAPH_NAME_TEMP_STAT.equals(tgdp.getName()))
		{
			Double[] arr = new Double[3];
			
			// Note the prefix: 'SQLServer' or 'MSSQL$@@servicename' is removed in SQL query
			String pk1 = createPkStr(":General Statistics", "Active Temp Tables",          "");
			String pk2 = createPkStr(":General Statistics", "Temp Tables Creation Rate",   "");
			String pk3 = createPkStr(":General Statistics", "Temp Tables For Destruction", "");
			
			Double val1 = this.getAbsValueAsDouble(pk1, "calculated_value");
			Double val2 = this.getAbsValueAsDouble(pk2, "calculated_value");
			Double val3 = this.getAbsValueAsDouble(pk3, "calculated_value");
			
			if (val1 != null && val2 != null && val3 != null)
			{
				arr[0] = val1;
				arr[1] = val2;
				arr[2] = val3;

				// Set the values
				tgdp.setDataPoint(this.getTimestamp(), arr);
			}
			else
			{
				TrendGraph tg = getTrendGraph(tgdp.getName());
				if (tg != null)
					tg.setWarningLabel("Failed to get value(s) for pk-row: '"+pk1+"'='"+val1+"', '"+pk2+"'='"+val2+"', '"+pk3+"'='"+val3+"'.");
			}
		}

		// -----------------------------------------------------------------------------------------
		if (GRAPH_NAME_TEMPDB_FREE_SPACE.equals(tgdp.getName()))
		{
			Double[] arr = new Double[1];
			
			// Note the prefix: 'SQLServer' or 'MSSQL$@@servicename' is removed in SQL query
			String pk1 = createPkStr(":Transactions", "Free Space in tempdb (KB)", "");
			
			Double val1 = this.getAbsValueAsDouble(pk1, "calculated_value");
			
			if (val1 != null)
			{
				arr[0] = val1;

				// Set the values
				tgdp.setDataPoint(this.getTimestamp(), arr);
			}
			else
			{
				TrendGraph tg = getTrendGraph(tgdp.getName());
				if (tg != null)
					tg.setWarningLabel("Failed to get value(s) for pk-row: '"+pk1+"'='"+val1+"'.");
			}
		}

		// -----------------------------------------------------------------------------------------
		if (GRAPH_NAME_VERSION_STORE.equals(tgdp.getName()))
		{
			Double[] arr = new Double[3];
			
			// Note the prefix: 'SQLServer' or 'MSSQL$@@servicename' is removed in SQL query
			String pk1 = createPkStr(":Transactions", "Version Store Size (KB)",        "");
			String pk2 = createPkStr(":Transactions", "Version Generation rate (KB/s)", "");
			String pk3 = createPkStr(":Transactions", "Version Cleanup rate (KB/s)",    "");
			
			Double val1 = this.getAbsValueAsDouble(pk1, "calculated_value");
			Double val2 = this.getAbsValueAsDouble(pk2, "calculated_value");
			Double val3 = this.getAbsValueAsDouble(pk3, "calculated_value");
			
			if (val1 != null && val2 != null && val3 != null)
			{
				arr[0] = val1;
				arr[1] = val2;
				arr[2] = val3;

				// Set the values
				tgdp.setDataPoint(this.getTimestamp(), arr);
			}
			else
			{
				TrendGraph tg = getTrendGraph(tgdp.getName());
				if (tg != null)
					tg.setWarningLabel("Failed to get value(s) for pk-row: '"+pk1+"'='"+val1+"', '"+pk2+"'='"+val2+"', '"+pk3+"'='"+val3+"'.");
			}
		}

		// -----------------------------------------------------------------------------------------
		if (GRAPH_NAME_SNAPSHOT_ISOLATION.equals(tgdp.getName()))
		{
			Double[] arr = new Double[5];
			
			// Note the prefix: 'SQLServer' or 'MSSQL$@@servicename' is removed in SQL query
			String pk1 = createPkStr(":Transactions", "Snapshot Transactions",            "");
			String pk2 = createPkStr(":Transactions", "Update Snapshot Transactions",     "");
			String pk3 = createPkStr(":Transactions", "NonSnapshot Version Transactions", "");
			String pk4 = createPkStr(":Transactions", "Longest Transaction Running Time", "");
			String pk5 = createPkStr(":Transactions", "Update conflict ratio",            "");
			
			Double val1 = this.getAbsValueAsDouble(pk1, "calculated_value");
			Double val2 = this.getAbsValueAsDouble(pk2, "calculated_value");
			Double val3 = this.getAbsValueAsDouble(pk3, "calculated_value");
			Double val4 = this.getAbsValueAsDouble(pk3, "calculated_value");
			Double val5 = this.getAbsValueAsDouble(pk3, "calculated_value");
			
			if (val1 != null && val2 != null && val3 != null && val4 != null && val5 != null)
			{
				arr[0] = val1;
				arr[1] = val2;
				arr[2] = val3;
				arr[3] = val4;
				arr[4] = val5;

				// Set the values
				tgdp.setDataPoint(this.getTimestamp(), arr);
			}
			else
			{
				TrendGraph tg = getTrendGraph(tgdp.getName());
				if (tg != null)
					tg.setWarningLabel("Failed to get value(s) for pk-row: '"+pk1+"'='"+val1+"', '"+pk2+"'='"+val2+"', '"+pk3+"'='"+val3+"', '"+pk4+"'='"+val4+"', '"+pk5+"'='"+val5+"'.");
			}
		}

		// -----------------------------------------------------------------------------------------
		if (GRAPH_NAME_LOG_CACHE_HIT_RATE.equals(tgdp.getName()))
		{
			String[] larr = new String[_dbnames.size()];
			Double[] darr = new Double[_dbnames.size()];
			
			for (int r=0; r<darr.length; r++)
			{
				String dbname = _dbnames.get(r);

				// Note the prefix: 'SQLServer' or 'MSSQL$@@servicename' is removed in SQL query
				String pk = createPkStr(":Databases", "Log Cache Hit Ratio", dbname);
				Double val = this.getAbsValueAsDouble(pk, "calculated_value");
				
				// should we consider 0 as 100% ??????
//				if (val != null && val == 0.0)
//					val = 100.0;

				// Record not found
				if (val == null)
					val = 0.0;

				larr[r] = dbname;
				darr[r] = val;
			}
			// Set the values
			tgdp.setDataPoint(this.getTimestamp(), larr, darr);
		}

		// -----------------------------------------------------------------------------------------
		if (GRAPH_NAME_CACHE_PLE.equals(tgdp.getName()))
		{
			Double[] arr = new Double[1];
			
			// Note the prefix: 'SQLServer' or 'MSSQL$@@servicename' is removed in SQL query
			String pk     = createPkStr(":Buffer Manager", "Page life expectancy", "");
			
			Double val = this.getAbsValueAsDouble(pk, "calculated_value");
			
			if (val != null)
			{
				arr[0 ] = val;

				// Set the values
				tgdp.setDataPoint(this.getTimestamp(), arr);
			}
			else
			{
				TrendGraph tg = getTrendGraph(tgdp.getName());
				if (tg != null)
					tg.setWarningLabel("Failed to get value(s) for pk-row: '"+pk+"'='"+val+"'.");
			}
		}

		// -----------------------------------------------------------------------------------------
		if (GRAPH_NAME_LOG_CACHE_READS.equals(tgdp.getName()))
		{
			String[] larr = new String[_dbnames.size()];
			Double[] darr = new Double[_dbnames.size()];
			
			for (int r=0; r<darr.length; r++)
			{
				String dbname = _dbnames.get(r);

				// Note the prefix: 'SQLServer' or 'MSSQL$@@servicename' is removed in SQL query
				String pk = createPkStr(":Databases", "Log Cache Reads/sec", dbname);
				Double val = this.getAbsValueAsDouble(pk, "calculated_value");

				// Record not found
				if (val == null)
					val = 0.0;

				larr[r] = dbname;
				darr[r] = val;
			}
			// Set the values
			tgdp.setDataPoint(this.getTimestamp(), larr, darr);
		}

		// -----------------------------------------------------------------------------------------
		if (GRAPH_NAME_LOG_POOL_REQUESTS.equals(tgdp.getName()))
		{
			String[] larr = new String[_dbnames.size()];
			Double[] darr = new Double[_dbnames.size()];
			
			for (int r=0; r<darr.length; r++)
			{
				String dbname = _dbnames.get(r);

				// Note the prefix: 'SQLServer' or 'MSSQL$@@servicename' is removed in SQL query
				String pk = createPkStr(":Databases", "Log Pool Requests/sec", dbname);
				Double val = this.getAbsValueAsDouble(pk, "calculated_value");

				// Record not found
				if (val == null)
					val = 0.0;

				larr[r] = dbname;
				darr[r] = val;
			}
			// Set the values
			tgdp.setDataPoint(this.getTimestamp(), larr, darr);
		}

		// -----------------------------------------------------------------------------------------
		if (GRAPH_NAME_TRANS_SEC.equals(tgdp.getName()))
		{
			String[] larr = new String[_dbnames.size()];
			Double[] darr = new Double[_dbnames.size()];
			
			for (int r=0; r<darr.length; r++)
			{
				String dbname = _dbnames.get(r);

				// Note the prefix: 'SQLServer' or 'MSSQL$@@servicename' is removed in SQL query
				String pk = createPkStr(":Databases", "Transactions/sec", dbname);
				Double val = this.getAbsValueAsDouble(pk, "calculated_value");

				// Record not found
				if (val == null)
					val = 0.0;

				larr[r] = dbname;
				darr[r] = val;
			}
			// Set the values
			tgdp.setDataPoint(this.getTimestamp(), larr, darr);
		}

		// -----------------------------------------------------------------------------------------
		if (GRAPH_NAME_TRANS_WRITE_SEC.equals(tgdp.getName()))
		{
			String[] larr = new String[_dbnames.size()];
			Double[] darr = new Double[_dbnames.size()];
			
			for (int r=0; r<darr.length; r++)
			{
				String dbname = _dbnames.get(r);

				// Note the prefix: 'SQLServer' or 'MSSQL$@@servicename' is removed in SQL query
				String pk = createPkStr(":Databases", "Write Transactions/sec", dbname);
				Double val = this.getAbsValueAsDouble(pk, "calculated_value");

				// Record not found
				if (val == null)
					val = 0.0;

				larr[r] = dbname;
				darr[r] = val;
			}
			// Set the values
			tgdp.setDataPoint(this.getTimestamp(), larr, darr);
		}

		// -----------------------------------------------------------------------------------------
		if (GRAPH_NAME_TRANS_ACTIVE.equals(tgdp.getName()))
		{
			String[] larr = new String[_dbnames.size()];
			Double[] darr = new Double[_dbnames.size()];
			
			for (int r=0; r<darr.length; r++)
			{
				String dbname = _dbnames.get(r);

				// Note the prefix: 'SQLServer' or 'MSSQL$@@servicename' is removed in SQL query
				String pk = createPkStr(":Databases", "Active Transactions", dbname);
				Double val = this.getAbsValueAsDouble(pk, "calculated_value");

				// Record not found
				if (val == null)
					val = 0.0;

				larr[r] = dbname;
				darr[r] = val;
			}
			// Set the values
			tgdp.setDataPoint(this.getTimestamp(), larr, darr);
		}

		// -----------------------------------------------------------------------------------------
		if (GRAPH_NAME_COMPILE.equals(tgdp.getName()))
		{
			Double[] arr = new Double[1];
			
			// Note the prefix: 'SQLServer' or 'MSSQL$@@servicename' is removed in SQL query
			String pk     = createPkStr(":SQL Statistics", "SQL Compilations/sec", "");
			
			Double val = this.getAbsValueAsDouble(pk, "calculated_value");
			
			if (val != null)
			{
				arr[0 ] = val;

				// Set the values
				tgdp.setDataPoint(this.getTimestamp(), arr);
			}
			else
			{
				TrendGraph tg = getTrendGraph(tgdp.getName());
				if (tg != null)
					tg.setWarningLabel("Failed to get value(s) for pk-row: '"+pk+"'='"+val+"'.");
			}
		}

		// -----------------------------------------------------------------------------------------
		if (GRAPH_NAME_RECOMPILE.equals(tgdp.getName()))
		{
			Double[] arr = new Double[1];
			
			// Note the prefix: 'SQLServer' or 'MSSQL$@@servicename' is removed in SQL query
			String pk     = createPkStr(":SQL Statistics", "SQL Re-Compilations/sec", "");
			
			Double val = this.getAbsValueAsDouble(pk, "calculated_value");
			
			if (val != null)
			{
				arr[0 ] = val;

				// Set the values
				tgdp.setDataPoint(this.getTimestamp(), arr);
			}
			else
			{
				TrendGraph tg = getTrendGraph(tgdp.getName());
				if (tg != null)
					tg.setWarningLabel("Failed to get value(s) for pk-row: '"+pk+"'='"+val+"'.");
			}
		}

		// -----------------------------------------------------------------------------------------
		if (GRAPH_NAME_BLOCKED_PROCESSES.equals(tgdp.getName()))
		{
			Double[] arr = new Double[1];
			
			// Note the prefix: 'SQLServer' or 'MSSQL$@@servicename' is removed in SQL query
			String pk     = createPkStr(":General Statistics", "Processes blocked", "");
			
			Double val = this.getAbsValueAsDouble(pk, "calculated_value");
			
			if (val != null)
			{
				arr[0 ] = val;

				// Set the values
				tgdp.setDataPoint(this.getTimestamp(), arr);
			}
			else
			{
				TrendGraph tg = getTrendGraph(tgdp.getName());
				if (tg != null)
					tg.setWarningLabel("Failed to get value(s) for pk-row: '"+pk+"'='"+val+"'.");
			}
		}

		// -----------------------------------------------------------------------------------------
		if (GRAPH_NAME_PAGE_SPLITS.equals(tgdp.getName()))
		{
			Double[] arr = new Double[1];
			
			// Note the prefix: 'SQLServer' or 'MSSQL$@@servicename' is removed in SQL query
			String pk     = createPkStr(":Access Methods", "Page Splits/sec", "");
			
			Double val = this.getAbsValueAsDouble(pk, "calculated_value");
			
			if (val != null)
			{
				arr[0 ] = val;

				// Set the values
				tgdp.setDataPoint(this.getTimestamp(), arr);
			}
			else
			{
				TrendGraph tg = getTrendGraph(tgdp.getName());
				if (tg != null)
					tg.setWarningLabel("Failed to get value(s) for pk-row: '"+pk+"'='"+val+"'.");
			}
		}

		// -----------------------------------------------------------------------------------------
		if (GRAPH_NAME_FWD_RIDS.equals(tgdp.getName()))
		{
			Double[] arr = new Double[1];
			
			// Note the prefix: 'SQLServer' or 'MSSQL$@@servicename' is removed in SQL query
			String pk     = createPkStr(":Access Methods", "Forwarded Records/sec", "");
			
			Double val = this.getAbsValueAsDouble(pk, "calculated_value");
			
			if (val != null)
			{
				arr[0 ] = val;

				// Set the values
				tgdp.setDataPoint(this.getTimestamp(), arr);
			}
			else
			{
				TrendGraph tg = getTrendGraph(tgdp.getName());
				if (tg != null)
					tg.setWarningLabel("Failed to get value(s) for pk-row: '"+pk+"'='"+val+"'.");
			}
		}

		// -----------------------------------------------------------------------------------------
		if (GRAPH_NAME_ACCESS_METHODS.equals(tgdp.getName()))
		{
			Double[] arr = new Double[3];
			
			// Note the prefix: 'SQLServer' or 'MSSQL$@@servicename' is removed in SQL query
			String pk1 = createPkStr(":Access Methods", "Full Scans/sec", "");
			String pk2 = createPkStr(":Access Methods", "Range Scans/sec", "");
			String pk3 = createPkStr(":Access Methods", "Probe Scans/sec", "");
			
			Double val1 = this.getAbsValueAsDouble(pk1, "calculated_value");
			Double val2 = this.getAbsValueAsDouble(pk2, "calculated_value");
			Double val3 = this.getAbsValueAsDouble(pk3, "calculated_value");
			
			if (val1 != null && val2 != null && val3 != null)
			{
				arr[0] = val1;
				arr[1] = val2;
				arr[2] = val3;

				// Set the values
				tgdp.setDataPoint(this.getTimestamp(), arr);
			}
			else
			{
				TrendGraph tg = getTrendGraph(tgdp.getName());
				if (tg != null)
					tg.setWarningLabel("Failed to get value(s) for pk-row: '"+pk1+"'='"+val1+"', '"+pk2+"'='"+val2+"', '"+pk3+"'='"+val3+"'.");
			}
		}

		// -----------------------------------------------------------------------------------------
		if (GRAPH_NAME_LATCH_WAITS.equals(tgdp.getName()))
		{
			Double[] arr = new Double[1];
			
			// Note the prefix: 'SQLServer' or 'MSSQL$@@servicename' is removed in SQL query
			String pk     = createPkStr(":Latches", "Latch Waits/sec", "");
			
			Double val = this.getAbsValueAsDouble(pk, "calculated_value");
			
			if (val != null)
			{
				arr[0 ] = val;

				// Set the values
				tgdp.setDataPoint(this.getTimestamp(), arr);
			}
			else
			{
				TrendGraph tg = getTrendGraph(tgdp.getName());
				if (tg != null)
					tg.setWarningLabel("Failed to get value(s) for pk-row: '"+pk+"'='"+val+"'.");
			}
		}

		// -----------------------------------------------------------------------------------------
		if (GRAPH_NAME_LATCH_WAIT_TIME.equals(tgdp.getName()))
		{
			Double[] arr = new Double[1];
			
			// Note the prefix: 'SQLServer' or 'MSSQL$@@servicename' is removed in SQL query
			String pk     = createPkStr(":Latches", "Average Latch Wait Time (ms)", "");
			
			Double val = this.getAbsValueAsDouble(pk, "calculated_value");
			
			if (val != null)
			{
				arr[0 ] = val;

				// Set the values
				tgdp.setDataPoint(this.getTimestamp(), arr);
			}
			else
			{
				TrendGraph tg = getTrendGraph(tgdp.getName());
				if (tg != null)
					tg.setWarningLabel("Failed to get value(s) for pk-row: '"+pk+"'='"+val+"'.");
			}
		}

		// -----------------------------------------------------------------------------------------
		if (GRAPH_NAME_FREE_LIST_STALLS.equals(tgdp.getName()))
		{
			Double[] arr = new Double[1];
			
			// Note the prefix: 'SQLServer' or 'MSSQL$@@servicename' is removed in SQL query
			String pk     = createPkStr(":Buffer Manager", "Free list stalls/sec", "");
			
			Double val = this.getAbsValueAsDouble(pk, "calculated_value");
			
			if (val != null)
			{
				arr[0 ] = val;

				// Set the values
				tgdp.setDataPoint(this.getTimestamp(), arr);
			}
			else
			{
				TrendGraph tg = getTrendGraph(tgdp.getName());
				if (tg != null)
					tg.setWarningLabel("Failed to get value(s) for pk-row: '"+pk+"'='"+val+"'.");
			}
		}

		// -----------------------------------------------------------------------------------------
		if (GRAPH_NAME_MEMORY_GRANTS_PENDING.equals(tgdp.getName()))
		{
			Double[] arr = new Double[1];
			
			// Note the prefix: 'SQLServer' or 'MSSQL$@@servicename' is removed in SQL query
			String pk     = createPkStr(":Memory Manager", "Memory Grants Pending", "");
			
			Double val = this.getAbsValueAsDouble(pk, "calculated_value");
			
			if (val != null)
			{
				arr[0 ] = val;

				// Set the values
				tgdp.setDataPoint(this.getTimestamp(), arr);
			}
			else
			{
				TrendGraph tg = getTrendGraph(tgdp.getName());
				if (tg != null)
					tg.setWarningLabel("Failed to get value(s) for pk-row: '"+pk+"'='"+val+"'.");
			}
		}
		
		// -----------------------------------------------------------------------------------------
		if (GRAPH_NAME_PLAN_CACHE_HIT_RATE.equals(tgdp.getName()))
		{
			Double[] arr = new Double[5];
			
			// Note the prefix: 'SQLServer' or 'MSSQL$@@servicename' is removed in SQL query
			String pk1 = createPkStr(":Plan Cache", "Cache Hit Ratio", "SQL Plans");
			String pk2 = createPkStr(":Plan Cache", "Cache Hit Ratio", "Object Plans");
			String pk3 = createPkStr(":Plan Cache", "Cache Hit Ratio", "Temporary Tables & Table Variables");
			String pk4 = createPkStr(":Plan Cache", "Cache Hit Ratio", "Bound Trees");
			String pk5 = createPkStr(":Plan Cache", "Cache Hit Ratio", "Extended Stored Procedures");
			
			Double val1 = this.getAbsValueAsDouble(pk1, "calculated_value");
			Double val2 = this.getAbsValueAsDouble(pk2, "calculated_value");
			Double val3 = this.getAbsValueAsDouble(pk3, "calculated_value");
			Double val4 = this.getAbsValueAsDouble(pk3, "calculated_value");
			Double val5 = this.getAbsValueAsDouble(pk3, "calculated_value");
			
			if (val1 != null && val2 != null && val3 != null && val4 != null && val5 != null)
			{
				arr[0] = val1;
				arr[1] = val2;
				arr[2] = val3;
				arr[3] = val4;
				arr[4] = val5;

				// Set the values
				tgdp.setDataPoint(this.getTimestamp(), arr);
			}
			else
			{
				TrendGraph tg = getTrendGraph(tgdp.getName());
				if (tg != null)
					tg.setWarningLabel("Failed to get value(s) for pk-row: '"+pk1+"'='"+val1+"', '"+pk2+"'='"+val2+"', '"+pk3+"'='"+val3+"', '"+pk4+"'='"+val4+"', '"+pk5+"'='"+val5+"'.");
			}
		}

		// -----------------------------------------------------------------------------------------
		if (GRAPH_NAME_PLAN_CACHE_MB.equals(tgdp.getName()))
		{
			Double[] arr = new Double[5];
			
			// Note the prefix: 'SQLServer' or 'MSSQL$@@servicename' is removed in SQL query
			String pk1 = createPkStr(":Plan Cache", "Cache Pages", "SQL Plans");
			String pk2 = createPkStr(":Plan Cache", "Cache Pages", "Object Plans");
			String pk3 = createPkStr(":Plan Cache", "Cache Pages", "Temporary Tables & Table Variables");
			String pk4 = createPkStr(":Plan Cache", "Cache Pages", "Bound Trees");
			String pk5 = createPkStr(":Plan Cache", "Cache Pages", "Extended Stored Procedures");
			
			Double val1 = this.getAbsValueAsDouble(pk1, "calculated_value");
			Double val2 = this.getAbsValueAsDouble(pk2, "calculated_value");
			Double val3 = this.getAbsValueAsDouble(pk3, "calculated_value");
			Double val4 = this.getAbsValueAsDouble(pk3, "calculated_value");
			Double val5 = this.getAbsValueAsDouble(pk3, "calculated_value");
			
			if (val1 != null && val2 != null && val3 != null && val4 != null && val5 != null)
			{
				arr[0] = val1 / 128; // divide by 128 to get MB from 8K Pages (512 = 1024*1024/8192)
				arr[1] = val2 / 128;
				arr[2] = val3 / 128;
				arr[3] = val4 / 128;
				arr[4] = val5 / 128;

				// Set the values
				tgdp.setDataPoint(this.getTimestamp(), arr);
			}
			else
			{
				TrendGraph tg = getTrendGraph(tgdp.getName());
				if (tg != null)
					tg.setWarningLabel("Failed to get value(s) for pk-row: '"+pk1+"'='"+val1+"', '"+pk2+"'='"+val2+"', '"+pk3+"'='"+val3+"', '"+pk4+"'='"+val4+"', '"+pk5+"'='"+val5+"'.");
			}
		}

		// -----------------------------------------------------------------------------------------
		if (GRAPH_NAME_PLAN_CACHE_OBJ_CNT.equals(tgdp.getName()))
		{
			Double[] arr = new Double[5];
			
			// Note the prefix: 'SQLServer' or 'MSSQL$@@servicename' is removed in SQL query
			String pk1 = createPkStr(":Plan Cache", "Cache Object Counts", "SQL Plans");
			String pk2 = createPkStr(":Plan Cache", "Cache Object Counts", "Object Plans");
			String pk3 = createPkStr(":Plan Cache", "Cache Object Counts", "Temporary Tables & Table Variables");
			String pk4 = createPkStr(":Plan Cache", "Cache Object Counts", "Bound Trees");
			String pk5 = createPkStr(":Plan Cache", "Cache Object Counts", "Extended Stored Procedures");
			
			Double val1 = this.getAbsValueAsDouble(pk1, "calculated_value");
			Double val2 = this.getAbsValueAsDouble(pk2, "calculated_value");
			Double val3 = this.getAbsValueAsDouble(pk3, "calculated_value");
			Double val4 = this.getAbsValueAsDouble(pk3, "calculated_value");
			Double val5 = this.getAbsValueAsDouble(pk3, "calculated_value");
			
			if (val1 != null && val2 != null && val3 != null && val4 != null && val5 != null)
			{
				arr[0] = val1;
				arr[1] = val2;
				arr[2] = val3;
				arr[3] = val4;
				arr[4] = val5;

				// Set the values
				tgdp.setDataPoint(this.getTimestamp(), arr);
			}
			else
			{
				TrendGraph tg = getTrendGraph(tgdp.getName());
				if (tg != null)
					tg.setWarningLabel("Failed to get value(s) for pk-row: '"+pk1+"'='"+val1+"', '"+pk2+"'='"+val2+"', '"+pk3+"'='"+val3+"', '"+pk4+"'='"+val4+"', '"+pk5+"'='"+val5+"'.");
			}
		}

		// -----------------------------------------------------------------------------------------
		if (GRAPH_NAME_PLAN_CACHE_OBJ_USE.equals(tgdp.getName()))
		{
			Double[] arr = new Double[5];
			
			// Note the prefix: 'SQLServer' or 'MSSQL$@@servicename' is removed in SQL query
			String pk1 = createPkStr(":Plan Cache", "Cache Objects in use", "SQL Plans");
			String pk2 = createPkStr(":Plan Cache", "Cache Objects in use", "Object Plans");
			String pk3 = createPkStr(":Plan Cache", "Cache Objects in use", "Temporary Tables & Table Variables");
			String pk4 = createPkStr(":Plan Cache", "Cache Objects in use", "Bound Trees");
			String pk5 = createPkStr(":Plan Cache", "Cache Objects in use", "Extended Stored Procedures");
			
			Double val1 = this.getAbsValueAsDouble(pk1, "calculated_value");
			Double val2 = this.getAbsValueAsDouble(pk2, "calculated_value");
			Double val3 = this.getAbsValueAsDouble(pk3, "calculated_value");
			Double val4 = this.getAbsValueAsDouble(pk3, "calculated_value");
			Double val5 = this.getAbsValueAsDouble(pk3, "calculated_value");
			
			if (val1 != null && val2 != null && val3 != null && val4 != null && val5 != null)
			{
				arr[0] = val1;
				arr[1] = val2;
				arr[2] = val3;
				arr[3] = val4;
				arr[4] = val5;

				// Set the values
				tgdp.setDataPoint(this.getTimestamp(), arr);
			}
			else
			{
				TrendGraph tg = getTrendGraph(tgdp.getName());
				if (tg != null)
					tg.setWarningLabel("Failed to get value(s) for pk-row: '"+pk1+"'='"+val1+"', '"+pk2+"'='"+val2+"', '"+pk3+"'='"+val3+"', '"+pk4+"'='"+val4+"', '"+pk5+"'='"+val5+"'.");
			}
		}

		// -----------------------------------------------------------------------------------------
		if (GRAPH_NAME_MEMORY_MANAGER.equals(tgdp.getName()))
		{
			// get all entries from "Memory Manager" section
			int[] rowIds = getDiffRowIdsWhere("object_name", ":Memory Manager");
			
			String[] larr = new String[rowIds.length];
			Double[] darr = new Double[rowIds.length];
			
			for (int r=0; r<darr.length; r++)
			{
				Integer rowId = rowIds[r];

				String label = getDiffString       (rowId, "counter_name");
				Double value = getDiffValueAsDouble(rowId, "calculated_value");

				larr[r] = label;
				darr[r] = value;
			}
			// Set the values
			tgdp.setDataPoint(this.getTimestamp(), larr, darr);
		}

		// -----------------------------------------------------------------------------------------
		if (GRAPH_NAME_MEMORY_TARGET_VS_TOTAL.equals(tgdp.getName()))
		{
//			new String[] { "Target Server Memory (KB)", "Total Server Memory (KB)" }, 
			Double[] arr = new Double[2];
			
			// Note the prefix: 'SQLServer' or 'MSSQL$@@servicename' is removed in SQL query
			String pk1 = createPkStr(":Memory Manager", "Target Server Memory (KB)", "");
			String pk2 = createPkStr(":Memory Manager", "Total Server Memory (KB)", "");
			
			Double val1 = this.getAbsValueAsDouble(pk1, "calculated_value");
			Double val2 = this.getAbsValueAsDouble(pk2, "calculated_value");
			
			if (val1 != null && val2 != null)
			{
				arr[0] = val1;
				arr[1] = val2;

				// Set the values
				tgdp.setDataPoint(this.getTimestamp(), arr);
			}
			else
			{
				TrendGraph tg = getTrendGraph(tgdp.getName());
				if (tg != null)
					tg.setWarningLabel("Failed to get value(s) for pk-row: '"+pk1+"'='"+val1+"', '"+pk2+"'='"+val2+"'.");
			}
		}

		// -----------------------------------------------------------------------------------------
		if (GRAPH_NAME_MEMORY_STOLEN.equals(tgdp.getName()))
		{
			Double[] arr = new Double[1];
			
			// Note the prefix: 'SQLServer' or 'MSSQL$@@servicename' is removed in SQL query
			String pk1 = createPkStr(":Memory Manager", "Stolen Server Memory (KB)", "");
			
			Double val1 = this.getAbsValueAsDouble(pk1, "calculated_value");
			
			if (val1 != null)
			{
				arr[0] = val1 / 1024.0;  // Make KB into MB

				// Set the values
				tgdp.setDataPoint(this.getTimestamp(), arr);
			}
			else
			{
				TrendGraph tg = getTrendGraph(tgdp.getName());
				if (tg != null)
					tg.setWarningLabel("Failed to get value(s) for pk-row: '"+pk1+"'='"+val1+"'.");
			}
		}

		// -----------------------------------------------------------------------------------------
		if (GRAPH_NAME_QUERY_STORE_TOTAL.equals(tgdp.getName()))
		{
			Double[] arr = new Double[4];
			
			// Note the prefix: 'SQLServer' or 'MSSQL$@@servicename' is removed in SQL query
			String pk1 = createPkStr(":Query Store", "Query Store CPU usage",      "_Total");
			String pk2 = createPkStr(":Query Store", "Query Store physical reads", "_Total");
			String pk3 = createPkStr(":Query Store", "Query Store logical reads",  "_Total");
			String pk4 = createPkStr(":Query Store", "Query Store logical writes", "_Total");
			
			Double val1 = this.getAbsValueAsDouble(pk1, "calculated_value");
			Double val2 = this.getAbsValueAsDouble(pk2, "calculated_value");
			Double val3 = this.getAbsValueAsDouble(pk3, "calculated_value");
			Double val4 = this.getAbsValueAsDouble(pk3, "calculated_value");
			
			if (val1 != null && val2 != null && val3 != null && val4 != null)
			{
				arr[0] = val1;
				arr[1] = val2;
				arr[2] = val3;
				arr[3] = val4;

				// Set the values
				tgdp.setDataPoint(this.getTimestamp(), arr);
			}
			else
			{
				TrendGraph tg = getTrendGraph(tgdp.getName());
				if (tg != null)
					tg.setWarningLabel("Failed to get value(s) for pk-row: '"+pk1+"'='"+val1+"', '"+pk2+"'='"+val2+"', '"+pk3+"'='"+val3+"', '"+pk4+"'='"+val4+"'.");
			}
		}

		// -----------------------------------------------------------------------------------------
		if (GRAPH_NAME_QUERY_STORE_DB_CPU.equals(tgdp.getName()))
		{
			// get all entries from "Query Store" -- "Query Store CPU usage" section
			Map<String, Object> map = new HashMap<>();
			map.put("object_name", ":Query Store");
			map.put("counter_name", "Query Store CPU usage");
			List<Integer> rowIds = getDiffRowIdsWhere(map); // Should we use ABS or DIFF here
			
			String[] larr = new String[rowIds.size()];
			Double[] darr = new Double[rowIds.size()];
			
			for (int r=0; r<darr.length; r++)
			{
				Integer rowId = rowIds.get(r);

				String label = getDiffString       (rowId, "instance_name");
				Double value = getDiffValueAsDouble(rowId, "calculated_value");

				larr[r] = label;
				darr[r] = value;
			}
			// Set the values
			tgdp.setDataPoint(this.getTimestamp(), larr, darr);
		}

		// -----------------------------------------------------------------------------------------
		if (GRAPH_NAME_QUERY_STORE_DB_PHYS_READ.equals(tgdp.getName()))
		{
			// get all entries from "Query Store" -- "Query Store physical reads" section
			Map<String, Object> map = new HashMap<>();
			map.put("object_name", ":Query Store");
			map.put("counter_name", "Query Store physical reads");
			List<Integer> rowIds = getDiffRowIdsWhere(map); // Should we use ABS or DIFF here
			
			String[] larr = new String[rowIds.size()];
			Double[] darr = new Double[rowIds.size()];
			
			for (int r=0; r<darr.length; r++)
			{
				Integer rowId = rowIds.get(r);

				String label = getDiffString       (rowId, "instance_name");
				Double value = getDiffValueAsDouble(rowId, "calculated_value");

				larr[r] = label;
				darr[r] = value;
			}
			// Set the values
			tgdp.setDataPoint(this.getTimestamp(), larr, darr);
		}

		// -----------------------------------------------------------------------------------------
		if (GRAPH_NAME_QUERY_STORE_DB_LOGI_READ.equals(tgdp.getName()))
		{
			// get all entries from "Query Store" -- "Query Store logical reads" section
			Map<String, Object> map = new HashMap<>();
			map.put("object_name", ":Query Store");
			map.put("counter_name", "Query Store logical reads");
			List<Integer> rowIds = getDiffRowIdsWhere(map); // Should we use ABS or DIFF here
			
			String[] larr = new String[rowIds.size()];
			Double[] darr = new Double[rowIds.size()];
			
			for (int r=0; r<darr.length; r++)
			{
				Integer rowId = rowIds.get(r);

				String label = getDiffString       (rowId, "instance_name");
				Double value = getDiffValueAsDouble(rowId, "calculated_value");

				larr[r] = label;
				darr[r] = value;
			}
			// Set the values
			tgdp.setDataPoint(this.getTimestamp(), larr, darr);
		}

		// -----------------------------------------------------------------------------------------
		if (GRAPH_NAME_QUERY_STORE_DB_LOGI_WRITE.equals(tgdp.getName()))
		{
			// get all entries from "Query Store" -- "Query Store logical writes" section
			Map<String, Object> map = new HashMap<>();
			map.put("object_name", ":Query Store");
			map.put("counter_name", "Query Store logical writes");
			List<Integer> rowIds = getDiffRowIdsWhere(map); // Should we use ABS or DIFF here
			
			String[] larr = new String[rowIds.size()];
			Double[] darr = new Double[rowIds.size()];
			
			for (int r=0; r<darr.length; r++)
			{
				Integer rowId = rowIds.get(r);

				String label = getDiffString       (rowId, "instance_name");
				Double value = getDiffValueAsDouble(rowId, "calculated_value");

				larr[r] = label;
				darr[r] = value;
			}
			// Set the values
			tgdp.setDataPoint(this.getTimestamp(), larr, darr);
		}

		// -----------------------------------------------------------------------------------------
		if (GRAPH_NAME_DEADLOCK_DETAILS.equals(tgdp.getName()))
		{
			// +---------------+-----------------------+-------------+----------+-----------+
			// |object_name    |counter_name           |instance_name|cntr_value|cntr_type  |
			// +---------------+-----------------------+-------------+----------+-----------+
			// |SQLServer:Locks|Number of Deadlocks/sec|Xact         |         0|272�696�576|
			// |SQLServer:Locks|Number of Deadlocks/sec|RowGroup     |         0|272�696�576|
			// |SQLServer:Locks|Number of Deadlocks/sec|OIB          |         0|272�696�576|
			// |SQLServer:Locks|Number of Deadlocks/sec|AllocUnit    |         0|272�696�576|
			// |SQLServer:Locks|Number of Deadlocks/sec|HoBT         |         0|272�696�576|
			// |SQLServer:Locks|Number of Deadlocks/sec|Metadata     |         0|272�696�576|
			// |SQLServer:Locks|Number of Deadlocks/sec|Application  |         0|272�696�576|
			// |SQLServer:Locks|Number of Deadlocks/sec|RID          |        18|272�696�576|
			// |SQLServer:Locks|Number of Deadlocks/sec|Extent       |         0|272�696�576|
			// |SQLServer:Locks|Number of Deadlocks/sec|Key          |         0|272�696�576|
			// |SQLServer:Locks|Number of Deadlocks/sec|Page         |         0|272�696�576|
			// |SQLServer:Locks|Number of Deadlocks/sec|Object       |         0|272�696�576|
			// |SQLServer:Locks|Number of Deadlocks/sec|File         |         0|272�696�576|
			// |SQLServer:Locks|Number of Deadlocks/sec|Database     |         0|272�696�576|
			// |SQLServer:Locks|Number of Deadlocks/sec|_Total       |        18|272�696�576|
			// +---------------+-----------------------+-------------+----------+-----------+
			// Rows 15
			
			
			
			// get all entries from "Query Store" -- "Query Store logical writes" section
			Map<String, Object> whereMap = new HashMap<>();
			whereMap.put("object_name", ":Locks");
			whereMap.put("counter_name", "Number of Deadlocks/sec");
			List<Integer> rowIds = getDiffRowIdsWhere(whereMap);
			
			String[] larr = new String[rowIds.size()];
			Double[] darr = new Double[rowIds.size()];
			
			for (int r=0; r<darr.length; r++)
			{
				Integer rowId = rowIds.get(r);

				String label = getDiffString       (rowId, "instance_name");
				Double value = getDiffValueAsDouble(rowId, "cntr_value");

				larr[r] = label;
				darr[r] = value;
			}

			// If '_Total' is the LAST entry (which is the normal) -->> Move it to First Entry
			if (larr[larr.length-1].equals("_Total"))
			{
				ArrayUtils.shift(larr, 1);
				ArrayUtils.shift(darr, 1);
			}
			
			// Set the values
			tgdp.setDataPoint(this.getTimestamp(), larr, darr);
		}

//		// -----------------------------------------------------------------------------------------
//		if (GRAPH_NAME_XXX.equals(tgdp.getName()))
//		{
//			// +---------------------+------------------------------+------------------------+----------+-----------+
//			// |object_name          |counter_name                  |instance_name           |cntr_value|cntr_type  |
//			// +---------------------+------------------------------+------------------------+----------+-----------+
//			// |:Memory Broker Clerks|Internal benefit              |Buffer Pool             |         0|     65 792|
//			// |:Memory Broker Clerks|Internal benefit              |Column store object pool|         0|     65 792|
//			// |:Memory Broker Clerks|Memory broker clerk size      |Buffer Pool             | 8 143 501|     65 792|
//			// |:Memory Broker Clerks|Memory broker clerk size      |Column store object pool|     2 652|     65 792|
//			// |:Memory Broker Clerks|Periodic evictions (pages)    |Buffer Pool             |         0|     65 792|
//			// |:Memory Broker Clerks|Periodic evictions (pages)    |Column store object pool|         0|     65 792|
//			// |:Memory Broker Clerks|Pressure evictions (pages/sec)|Buffer Pool             |         0|272 696 576|
//			// |:Memory Broker Clerks|Pressure evictions (pages/sec)|Column store object pool|15 050 839|272 696 576|
//			// |:Memory Broker Clerks|Simulation benefit            |Buffer Pool             |         0|     65 792|
//			// |:Memory Broker Clerks|Simulation benefit            |Column store object pool|         0|     65 792|
//			// |:Memory Broker Clerks|Simulation size               |Buffer Pool             | 2 033 627|     65 792|
//			// |:Memory Broker Clerks|Simulation size               |Column store object pool|       150|     65 792|
//			// +------------------------------+------------------------------+------------------------+----------+-----------+
//		}
		
		// -----------------------------------------------------------------------------------------
		if (GRAPH_NAME_COLUMNSTORE_ALL.equals(tgdp.getName()))
		{
			// +------------+--------------------------------+-------------+----------------+----------------+----------+---------------------------+
			// |object_name |counter_name                    |instance_name|calculated_value|cntr_type_desc  |cntr_value|cntr_type_name             |
			// +------------+--------------------------------+-------------+----------------+----------------+----------+---------------------------+
			// |:Columnstore|Delta Rowgroups Created         |_Total       |               0|absolute counter|         0|PERF_COUNTER_LARGE_RAWCOUNT|
			// |:Columnstore|Delta Rowgroups Closed          |_Total       |               0|absolute counter|         0|PERF_COUNTER_LARGE_RAWCOUNT|
			// |:Columnstore|Delta Rowgroups Compressed      |_Total       |               0|absolute counter|         0|PERF_COUNTER_LARGE_RAWCOUNT|
			// |:Columnstore|Total Rowgroups Compressed      |_Total       |               0|absolute counter|         0|PERF_COUNTER_LARGE_RAWCOUNT|
			// |:Columnstore|Total Delete Buffers Migrated   |_Total       |               0|absolute counter|         0|PERF_COUNTER_LARGE_RAWCOUNT|
			// |:Columnstore|Total Merge Policy Evaluations  |_Total       |               0|absolute counter|         0|PERF_COUNTER_LARGE_RAWCOUNT|
			// |:Columnstore|Total Source Rowgroups Merged   |_Total       |               0|absolute counter|         0|PERF_COUNTER_LARGE_RAWCOUNT|
			// |:Columnstore|Total Rowgroups Merge Compressed|_Total       |               0|absolute counter|         0|PERF_COUNTER_LARGE_RAWCOUNT|
			// |:Columnstore|Total Rowgroups Fit For Merge   |_Total       |               0|absolute counter|         0|PERF_COUNTER_LARGE_RAWCOUNT|
			// |:Columnstore|Segment Cache Hit Ratio         |_Total       |               0|percentage rate |         0|PERF_LARGE_RAW_FRACTION    |
			// |:Columnstore|Segment Cache Hit Ratio Base    |_Total       |                |internal        |         0|PERF_LARGE_RAW_BASE        | << -- NOT INCLUDED
			// |:Columnstore|Segment Reads/Sec               |_Total       |               0|rate per second |         0|PERF_COUNTER_BULK_COUNT    |
			// +------------+--------------------------------+-------------+----------------+----------------+----------+---------------------------+
			
			Double[] arr = new Double[11];
			
			// Note the prefix: 'SQLServer' or 'MSSQL$@@servicename' is removed in SQL query
			String pk1  = createPkStr(":Columnstore", "Delta Rowgroups Created"         , "_Total");
			String pk2  = createPkStr(":Columnstore", "Delta Rowgroups Closed"          , "_Total");
			String pk3  = createPkStr(":Columnstore", "Delta Rowgroups Compressed"      , "_Total");
			String pk4  = createPkStr(":Columnstore", "Total Rowgroups Compressed"      , "_Total");
			String pk5  = createPkStr(":Columnstore", "Total Delete Buffers Migrated"   , "_Total");
			String pk6  = createPkStr(":Columnstore", "Total Merge Policy Evaluations"  , "_Total");
			String pk7  = createPkStr(":Columnstore", "Total Source Rowgroups Merged"   , "_Total");
			String pk8  = createPkStr(":Columnstore", "Total Rowgroups Merge Compressed", "_Total");
			String pk9  = createPkStr(":Columnstore", "Total Rowgroups Fit For Merge"   , "_Total");
			String pk10 = createPkStr(":Columnstore", "Segment Cache Hit Ratio"         , "_Total");
			String pk11 = createPkStr(":Columnstore", "Segment Reads/Sec"               , "_Total");
			
			Double val1  = this.getAbsValueAsDouble(pk1, "calculated_value");
			Double val2  = this.getAbsValueAsDouble(pk2, "calculated_value");
			Double val3  = this.getAbsValueAsDouble(pk3, "calculated_value");
			Double val4  = this.getAbsValueAsDouble(pk3, "calculated_value");
			Double val5  = this.getAbsValueAsDouble(pk3, "calculated_value");
			Double val6  = this.getAbsValueAsDouble(pk3, "calculated_value");
			Double val7  = this.getAbsValueAsDouble(pk3, "calculated_value");
			Double val8  = this.getAbsValueAsDouble(pk3, "calculated_value");
			Double val9  = this.getAbsValueAsDouble(pk3, "calculated_value");
			Double val10 = this.getAbsValueAsDouble(pk3, "calculated_value");
			Double val11 = this.getAbsValueAsDouble(pk3, "calculated_value");
			
			if (val1 != null && val2 != null && val3 != null && val4 != null && val5 != null && val6 != null && val7 != null && val8 != null && val9 != null && val10 != null && val11 != null)
			{
				arr[0]  = val1;
				arr[1]  = val2;
				arr[2]  = val3;
				arr[3]  = val4;
				arr[4]  = val5;
				arr[5]  = val6;
				arr[6]  = val7;
				arr[7]  = val8;
				arr[8]  = val9;
				arr[9]  = val10;
				arr[10] = val11;

				// Set the values
				tgdp.setDataPoint(this.getTimestamp(), arr);
			}
			else
			{
				TrendGraph tg = getTrendGraph(tgdp.getName());
				if (tg != null)
					tg.setWarningLabel("Failed to get value(s) for pk-row: '"+pk1+"'='"+val1+"', '"+pk2+"'='"+val2+"', '"+pk3+"'='"+val3+"', '"+pk4+"'='"+val4+"', '"+pk5+"'='"+val5+"', '"+pk6+"'='"+val6+"', '"+pk7+"'='"+val7+"', '"+pk8+"'='"+val8+"', '"+pk9+"'='"+val9+"', '"+pk10+"'='"+val10+"', '"+pk11+"'='"+val11+"'.");
			}
		}

		// -----------------------------------------------------------------------------------------
		if (GRAPH_NAME_SQL_ERRORS.equals(tgdp.getName()))
		{
			// +-----------+------------+----------------------+----------------+---------------+----------+-----------------------+
			// |object_name|counter_name|instance_name         |calculated_value|cntr_type_desc |cntr_value|cntr_type_name         |
			// +-----------+------------+----------------------+----------------+---------------+----------+-----------------------+
			// |:SQL Errors|Errors/sec  |User Errors           |               0|rate per second|         0|PERF_COUNTER_BULK_COUNT|
			// |:SQL Errors|Errors/sec  |Kill Connection Errors|               0|rate per second|         0|PERF_COUNTER_BULK_COUNT|
			// |:SQL Errors|Errors/sec  |Info Errors           |            0,23|rate per second|         2|PERF_COUNTER_BULK_COUNT|
			// |:SQL Errors|Errors/sec  |DB Offline Errors     |               0|rate per second|         0|PERF_COUNTER_BULK_COUNT|
			// |:SQL Errors|Errors/sec  |_Total                |            0,23|rate per second|         2|PERF_COUNTER_BULK_COUNT|
			// +-----------+------------+----------------------+----------------+---------------+----------+-----------------------+
			
			Double[] arr = new Double[5];
			
			// Note the prefix: 'SQLServer' or 'MSSQL$@@servicename' is removed in SQL query
			String pk1  = createPkStr(":SQL Errors", "Errors/sec" , "_Total");
			String pk2  = createPkStr(":SQL Errors", "Errors/sec" , "Info Errors");
			String pk3  = createPkStr(":SQL Errors", "Errors/sec" , "User Errors");
			String pk4  = createPkStr(":SQL Errors", "Errors/sec" , "Kill Connection Errors");
			String pk5  = createPkStr(":SQL Errors", "Errors/sec" , "DB Offline Errors");
			
			Double val1  = this.getAbsValueAsDouble(pk1, "calculated_value");
			Double val2  = this.getAbsValueAsDouble(pk2, "calculated_value");
			Double val3  = this.getAbsValueAsDouble(pk3, "calculated_value");
			Double val4  = this.getAbsValueAsDouble(pk3, "calculated_value");
			Double val5  = this.getAbsValueAsDouble(pk3, "calculated_value");
			
			if (val1 != null && val2 != null && val3 != null && val4 != null && val5 != null)
			{
				arr[0]  = val1;
				arr[1]  = val2;
				arr[2]  = val3;
				arr[3]  = val4;
				arr[4]  = val5;

				// Set the values
				tgdp.setDataPoint(this.getTimestamp(), arr);
			}
			else
			{
				TrendGraph tg = getTrendGraph(tgdp.getName());
				if (tg != null)
					tg.setWarningLabel("Failed to get value(s) for pk-row: '"+pk1+"'='"+val1+"', '"+pk2+"'='"+val2+"', '"+pk3+"'='"+val3+"', '"+pk4+"'='"+val4+"', '"+pk5+"'='"+val5+"'.");
			}
		}
	}

	
	@Override
	protected TabularCntrPanel createGui()
	{
		return new CmPerfCountersPanel(this);
	}

	@Override
	public String[] getDependsOnConfigForVersion(DbxConnection conn, DbmsVersionInfo versionInfo)
	{
		return NEED_CONFIG;
	}

	@Override
	public List<String> getPkForVersion(DbxConnection conn, DbmsVersionInfo versionInfo)
	{
		List <String> pkCols = new LinkedList<String>();

		pkCols.add("object_name");
		pkCols.add("counter_name");
		pkCols.add("instance_name");

		return pkCols;
	}

	@Override
	public String getSqlForVersion(DbxConnection conn, DbmsVersionInfo versionInfo)
	{
		DbmsVersionInfoSqlServer ssVersionInfo = (DbmsVersionInfoSqlServer) versionInfo;
		
		String dm_os_performance_counters = "dm_os_performance_counters";
		
		if (ssVersionInfo.isAzureSynapseAnalytics())
			dm_os_performance_counters = "dm_pdw_nodes_os_performance_counters";

		// It seems that some extra values for 'cntr_type' has extended a bit:
		// https://tsql.tech/create-perfmon-and-filestats-reports-in-powerbi-part-1/
		//   cntr_type: 65536     -->> PERF_COUNTER_LARGE_RAWCOUNT 
		//   cntr_type: 272696320 -->> PERF_COUNTER_BULK_COUNT 
		
		String sql = 
			  "select /* ${cmCollectorName} */ \n" 
			+ "       object_name   = cast(substring(object_name, charindex(':', object_name),128) as varchar(128)), /* removing 'SQLServer' or 'MSSQL$@@servicename' for easier lookups */ \n"
			+ "       counter_name  = cast(counter_name  as varchar(128)), \n"
			+ "       instance_name = cast(instance_name as varchar(128)), \n"
			+ "       calculated_value = convert(numeric(15,2), CASE WHEN cntr_type = 65792 THEN cntr_value ELSE null END), \n"
			+ "       CASE \n"
			+ "          WHEN cntr_type = 65792      THEN convert(varchar(30), 'absolute counter')\n" 
			+ "          WHEN cntr_type = 65536      THEN convert(varchar(30), 'absolute counter')\n" 
			+ "          WHEN cntr_type = 272696576  THEN convert(varchar(30), 'rate per second') \n" 
			+ "          WHEN cntr_type = 272696320  THEN convert(varchar(30), 'rate per second') \n" 
			+ "          WHEN cntr_type = 537003264  THEN convert(varchar(30), 'percentage rate') \n" 
			+ "          WHEN cntr_type = 1073874176 THEN convert(varchar(30), 'average metric')  \n" 
			+ "          WHEN cntr_type = 1073939712 THEN convert(varchar(30), 'internal')        \n" 
			+ "          ELSE convert(varchar(30), cntr_type) \n"
			+ "       END as cntr_type_desc, \n"
			+ "       cntr_value, \n"
			+ "       CASE \n"
			+ "          WHEN cntr_type = 65792      THEN convert(varchar(30), 'PERF_COUNTER_LARGE_RAWCOUNT') /* provides the last observed value for the counter; for this type of counter, the values in cntr_value can be used directly, making this the most easily usable type  */ \n" 
			+ "          WHEN cntr_type = 65536      THEN convert(varchar(30), 'PERF_COUNTER_LARGE_RAWCOUNT') /* provides the last observed value for the counter; for this type of counter, the values in cntr_value can be used directly, making this the most easily usable type  */ \n" 
			+ "          WHEN cntr_type = 272696576  THEN convert(varchar(30), 'PERF_COUNTER_BULK_COUNT')     /* provides the average number of operations per second. Two readings of cntr_value will be required for this counter type, in order to get the per second averages    */ \n" 
			+ "          WHEN cntr_type = 272696320  THEN convert(varchar(30), 'PERF_COUNTER_BULK_COUNT')     /* provides the average number of operations per second. Two readings of cntr_value will be required for this counter type, in order to get the per second averages    */ \n" 
			+ "          WHEN cntr_type = 537003264  THEN convert(varchar(30), 'PERF_LARGE_RAW_FRACTION')     /* used in conjunction with PERF_LARGE_RAW_BASE to calculate ratio values, such as the cache hit ratio                                                                 */ \n" 
			+ "          WHEN cntr_type = 1073874176 THEN convert(varchar(30), 'PERF_AVERAGE_BULK')           /* used to calculate an average number of operations completed during a time interval; like PERF_LARGE_RAW_FRACTION, it uses PERF_LARGE_RAW_BASE to do the calculation */ \n" 
			+ "          WHEN cntr_type = 1073939712 THEN convert(varchar(30), 'PERF_LARGE_RAW_BASE')         /* used in the translation of PERF_LARGE_RAW_FRACTION and PERF_AVERAGE_BULK values to readable output; should not be displayed alone.                                  */ \n" 
			+ "          ELSE convert(varchar(30), cntr_type) \n"
			+ "       END as cntr_type_name \n"
			+ "from sys." + dm_os_performance_counters;

		return sql;
	}
	
	// here are descriptions for all 'counter_name' fields
	// https://docs.microsoft.com/en-us/sql/relational-databases/performance-monitor/use-sql-server-objects


	@Override
	public void addMonTableDictForVersion(DbxConnection conn, DbmsVersionInfo versionInfo)
	{
		try 
		{
			MonTablesDictionary mtd = MonTablesDictionaryManager.getInstance();
			//mtd.addTable("dm_os_performance_counters",  "already done...");

			mtd.addColumn("dm_os_performance_counters", "cntr_type_name",    "<html>Friendly string name for the 'cntr_type' value.</html>");
			mtd.addColumn("dm_os_performance_counters", "cntr_type_desc",    "<html>Simpler description of what the couter is used for.</html>");
						
			String htmlDesc = 
				"<html>This column is calculated based on the following document: <A HREF='https://blogs.msdn.microsoft.com/psssql/2013/09/23/interpreting-the-counter-values-from-sys-dm_os_performance_counters/'>https://blogs.msdn.microsoft.com/psssql/2013/09/23/interpreting-the-counter-values-from-sys-dm_os_performance_counters/</A><br>"
				+ "In short there are 3 different algorithms, which are based on column 'cntr_type_name'"
				+ "<table>"
				+ "  <tr> <th></th>   <th>cntr_type</th> <th>Description</th> </tr>"
				+ "  <tr> "
				+ "       <td>#5</td>"
				+ "       <td><code>PERF_COUNTER_LARGE_RAWCOUNT</code><br>cntr_type = 65792, 65536</td>"
				+ "       <td>Not calculated at all, simply copies the column 'cntr_value'<br>"
				+ "           This since PERF_COUNTER_LARGE_RAWCOUNT are a counter that goes up/down and represents <i>last observed value</i>.<br>"
				+ "           For example number of <i>pages</i> in the data/buffer cache.<br>"
				+ "       </td>"
				+ "  </tr>"
				+ "  <tr>"
				+ "       <td>#4</td>"
				+ "       <td><code>PERF_COUNTER_BULK_COUNT</code><br>cntr_type = 272696576, 272696320</td>"
				+ "       <td>This counter value represents a rate metric.<br>"
				+ "           The difference between two samples, divided by time between samples<br>"
				+ "           <b>Formula</b>: <code>calculated_value = (Value2 - Value1) / (seconds between samples)</code>"
				+ "       </td>"
				+ "  </tr>"
				+ "  <tr>"
				+ "       <td>#2</td>"
				+ "       <td><code>PERF_LARGE_RAW_FRACTION</code><br>cntr_type = 537003264</td>"
				+ "       <td>This counter type is typically used to calculate percentage rate, for example: cache hit ratio<br>"
				+ "           The below <i>base</i> value for this row is the <i>same</i> 'counter_name' with the string 'base' appended at the end.<br>"
				+ "           <b>Formula</b>: <code>calculated_value = (Value / BaseValue) * 100</code>"
				+ "       </td>"
				+ "  </tr>"
				+ "  <tr>"
				+ "       <td>#3</td>"
				+ "       <td><code>PERF_AVERAGE_BULK</code><br>cntr_type = 1073874176</td>"
				+ "       <td>This counter value represents an average metric.<br>"
				+ "           The difference between two samples, divided by the difference of the <i>base</i> counter value for this 'counter_name'<br>"
				+ "           The <i>base</i> value for this row is the <i>same</i> 'counter_name' with the string 'base' appended at the end.<br>"
				+ "           <b>Formula</b>: <code>calculated_value = (Value2 - Value1) / (BaseValue2 - BaseValue1)</code>"
				+ "       </td>"
				+ "  </tr>"
				+ "  <tr>"
				+ "       <td>#1</td>"
				+ "       <td><code>PERF_LARGE_RAW_BASE</code><br>cntr_type = 1073939712</td>"
				+ "       <td>This is the <i>base</i> row/value whci is used in counter type #2 and #3<br>"
				+ "           It's an internal value that should only be used in calculation of type #2 and #3</td>"
				+ "  </tr>"
				+ "</table>"
				+ "</html>";
			mtd.addColumn("dm_os_performance_counters", "calculated_value",  htmlDesc);
		}
		catch (NameNotFoundException e) 
		{
			_logger.warn("Problems in cm='" + CM_NAME + "', adding addMonTableDictForVersion. Caught: " + e); 
		//	System.out.println("Problems in cm='" + CM_NAME + "', adding addMonTableDictForVersion. Caught: " + e); 
		}
	}

	@Override
	public void localCalculation(CounterSample prevSample, CounterSample newSample, CounterSample diffData)
	{
		// Calculate values for: PERF_LARGE_RAW_FRACTION, PERF_LARGE_RAW_BASE
		// PERF_LARGE_RAW_FRACTION = abs .PERF_LARGE_RAW_FRACTION / abs .PERF_LARGE_RAW_BASE
		// PERF_LARGE_RAW_BASE     = diff.PERF_LARGE_RAW_FRACTION / diff.PERF_LARGE_RAW_BASE

		int object_name_pos      = -1;
		int counter_name_pos     = -1;
		int instance_name_pos    = -1;
		int cntr_value_pos       = -1;
		int calculated_value_pos = -1;
		int cntr_type_name_pos   = -1;
		
		// Find column Id's
		List<String> colNames = diffData.getColNames();
		if (colNames == null)
			return;
		
		for (int colId = 0; colId < colNames.size(); colId++)
		{
			String colName = (String) colNames.get(colId);
			if      (colName.equals("object_name"     )) object_name_pos      = colId;
			else if (colName.equals("counter_name"    )) counter_name_pos     = colId;
			else if (colName.equals("instance_name"   )) instance_name_pos    = colId;
			else if (colName.equals("cntr_value"      )) cntr_value_pos       = colId;
			else if (colName.equals("calculated_value")) calculated_value_pos = colId;
			else if (colName.equals("cntr_type_name"  )) cntr_type_name_pos   = colId;
		}

		if (object_name_pos == -1 || counter_name_pos == -1 || instance_name_pos == -1 || cntr_value_pos == -1 || calculated_value_pos == -1 || cntr_type_name_pos == -1)
		{
			_logger.warn("Can't find all desired columns. object_name_pos="+object_name_pos+", counter_name_pos="+counter_name_pos+", instance_name_pos="+instance_name_pos+", cntr_value_pos="+cntr_value_pos+", calculated_value_pos="+calculated_value_pos+", cntr_type_name_pos="+cntr_type_name_pos+".");
			return;
		}

		int absRowcount  = newSample.getRowCount();
		int diffRowcount = diffData.getRowCount();
		
		if (absRowcount != diffRowcount)
			_logger.warn("Number of rows in ABS and DIFF counters should be the same, otherwise this probably wont work. absRowcount="+absRowcount+", diffRowcount="+diffRowcount);
		
		// Clear the "known databases"
		_dbnames.clear();
		
		// NOTE: the below calculation depends HEAVELY that the data is sorted in "the correct order" (or just as SQL-Server delivers them, no specific 'order by' should be done)
		for (int rowId = 0; rowId < diffData.getRowCount(); rowId++)
		{
			String object_name    = (String) diffData.getValueAt(rowId, object_name_pos);
			String counter_name   = (String) diffData.getValueAt(rowId, counter_name_pos);
			String instance_name  = (String) diffData.getValueAt(rowId, instance_name_pos);
			String cntr_type_name = (String) diffData.getValueAt(rowId, cntr_type_name_pos);

			// Add the records to 
			if (":Databases".equals(object_name) && "Transactions/sec".equals(counter_name))
			{
				if ( ! _dbnames.contains(instance_name) )
					_dbnames.add(instance_name);
			}
			
			// cntr_type_name
			if ("PERF_LARGE_RAW_FRACTION".equals(cntr_type_name))
			{
				// get "next row", which hopefulle is "BASE"
				String nextRow_cntrTypeName = (String) newSample.getValueAt(rowId+1, cntr_type_name_pos);

				// Do extra checks (if NEXT row is of same object/"counter"/instance name) if we are in debug mode
				// but counter_name *will* not be "samish" in many instances 
				if (_logger.isDebugEnabled())
				{
					String nextRow_object_name    = (String) diffData.getValueAt(rowId+1, object_name_pos);
					String nextRow_counter_name   = (String) diffData.getValueAt(rowId+1, counter_name_pos);
					String nextRow_instance_name  = (String) diffData.getValueAt(rowId+1, instance_name_pos);

					if (        object_name != null &&         counter_name != null &&         instance_name != null &&
					    nextRow_object_name != null && nextRow_counter_name != null && nextRow_instance_name != null )
					{
						if (object_name.equals(nextRow_object_name) && nextRow_counter_name.startsWith(counter_name) && instance_name.equals(nextRow_instance_name))
						{
							// match
						}
						else
						{
							_logger.debug("################## >>>>>>>>>>>> ");
							_logger.debug("                                object_name          =|"+object_name+"|");
							_logger.debug("                                counter_name         =|"+counter_name+"|");
							_logger.debug("                                instance_name        =|"+instance_name+"|");
							_logger.debug("                                nextRow_object_name  =|"+nextRow_object_name+"|");
							_logger.debug("                                nextRow_counter_name =|"+nextRow_counter_name+"|");
							_logger.debug("                                nextRow_instance_name=|"+nextRow_instance_name+"|");
						}
					}
				}

				if ("PERF_LARGE_RAW_BASE".equals(nextRow_cntrTypeName))
				{
					// abs.value / abs.base
					long val  = ((Number) newSample.getValueAt(rowId,   cntr_value_pos)).longValue();
					long base = ((Number) newSample.getValueAt(rowId+1, cntr_value_pos)).longValue(); // value from NEXT row
					
					BigDecimal calc;
					if (base > 0)
						calc = new BigDecimal( (val*1.0 / base*1.0) * 100.0 ).setScale(2, BigDecimal.ROUND_HALF_EVEN);
					else
						calc = new BigDecimal(0.0);

					newSample.setValueAt(calc, rowId, calculated_value_pos);
					diffData .setValueAt(calc, rowId, calculated_value_pos);
				}
			}
			else if ("PERF_AVERAGE_BULK".equals(cntr_type_name))
			{
				// get "next row", which hopefulle is "BASE"
				String nextRow_cntrTypeName = (String) diffData.getValueAt(rowId+1, cntr_type_name_pos);

				// Do extra checks (if NEXT row is of same object/"counter"/instance name) if we are in debug mode
				// but counter_name *will* not be "samish" in many instances 
				if (_logger.isDebugEnabled())
				{
//					String object_name            = (String) diffData.getValueAt(rowId, object_name_pos);
//					String counter_name           = (String) diffData.getValueAt(rowId, counter_name_pos);
//					String instance_name          = (String) diffData.getValueAt(rowId, instance_name_pos);

					String nextRow_object_name    = (String) diffData.getValueAt(rowId+1, object_name_pos);
					String nextRow_counter_name   = (String) diffData.getValueAt(rowId+1, counter_name_pos);
					String nextRow_instance_name  = (String) diffData.getValueAt(rowId+1, instance_name_pos);

					if (        object_name != null &&         counter_name != null &&         instance_name != null &&
					    nextRow_object_name != null && nextRow_counter_name != null && nextRow_instance_name != null )
					{
						if (object_name.equals(nextRow_object_name) && nextRow_counter_name.startsWith(counter_name) && instance_name.equals(nextRow_instance_name))
						{
							// match
						}
						else
						{
							_logger.debug("################## >>>>>>>>>>>> ");
							_logger.debug("                                object_name          =|"+object_name+"|");
							_logger.debug("                                counter_name         =|"+counter_name+"|");
							_logger.debug("                                instance_name        =|"+instance_name+"|");
							_logger.debug("                                nextRow_object_name  =|"+nextRow_object_name+"|");
							_logger.debug("                                nextRow_counter_name =|"+nextRow_counter_name+"|");
							_logger.debug("                                nextRow_instance_name=|"+nextRow_instance_name+"|");
						}
					}
				}

				if ("PERF_LARGE_RAW_BASE".equals(nextRow_cntrTypeName))
				{
					// diff.value / diff.base
					long diffVal  = ((Number) diffData.getValueAt(rowId,   cntr_value_pos)).longValue();
					long diffBase = ((Number) diffData.getValueAt(rowId+1, cntr_value_pos)).longValue(); // value from NEXT row
					
					BigDecimal calc;
					if (diffBase > 0)
						calc = new BigDecimal( diffVal*1.0 / diffBase*1.0 ).setScale(2, BigDecimal.ROUND_HALF_EVEN);
					else
						calc = new BigDecimal(0.0);

					newSample.setValueAt(calc, rowId, calculated_value_pos);
					diffData .setValueAt(calc, rowId, calculated_value_pos);
				}
			}
			else if ("PERF_COUNTER_BULK_COUNT".equals(cntr_type_name))
			{
				// Do simple rate calc
				long val  = ((Number) diffData.getValueAt(rowId,   cntr_value_pos)).longValue();
				long interval = getSampleInterval(); 
				
				BigDecimal calc;
				if (interval > 0)
					calc = new BigDecimal( val*1000.0 / interval*1.0 ).setScale(2, BigDecimal.ROUND_HALF_EVEN);
				else
					calc = new BigDecimal(0.0);

				newSample.setValueAt(calc, rowId, calculated_value_pos);
				diffData .setValueAt(calc, rowId, calculated_value_pos);
			}
		} // end: loop on rows
		
		// Sort the dblist
		Collections.sort(_dbnames, String.CASE_INSENSITIVE_ORDER);
//System.out.println("DBNAME-LIST#"+_dbnames.size()+": "+_dbnames);
	} // end: method 

	@Override
	public String getToolTipTextOnTableCell(MouseEvent e, String colName, Object cellValue, int modelRow, int modelCol) 
	{
		if ("object_name".equals(colName) || "counter_name".equals(colName) || "instance_name".equals(colName) || "calculated_value".equals(colName))
		{
			if (cellValue == null)
				return null;
			
			String object_name = getAbsString(modelRow, "object_name");
			if (StringUtil.hasValue(object_name) && object_name.startsWith(":"))
				object_name = object_name.substring(1);

			String counter_name     = getAbsString(modelRow, "counter_name");
			String instance_name    = getAbsString(modelRow, "instance_name");
			String calculated_value = getAbsString(modelRow, "calculated_value");
			
			return SqlServerDmOsPerformanceCountersDictionary.getInstance().getDescriptionHtml(object_name, counter_name, instance_name, calculated_value);
		}
		
		if ("cntr_type_name".equals(colName))
		{
			String cntr_type_name = (String) cellValue;
			String seeAlso = "<br><br><hr>see: <A HREF='https://blogs.msdn.microsoft.com/psssql/2013/09/23/interpreting-the-counter-values-from-sys-dm_os_performance_counters/'>https://blogs.msdn.microsoft.com/psssql/2013/09/23/interpreting-the-counter-values-from-sys-dm_os_performance_counters/</A>";
			
			String desc = "";
			if      ("PERF_COUNTER_LARGE_RAWCOUNT".equals(cntr_type_name)) desc = "Provides the <b>last observed value</b> for the counter; for this type of counter, the values in cntr_value can be used directly, making this the most easily usable type<br>"
			                                                                    + Version.getAppName()+" also helps you calculate the difference from previous sample, look at the <i>diff</i> counters, rr the <i>rate</i> counters...<br>"
			                                                                    + "<br><b>Formula</b>: none, just the counter as it is"
			                                                                    + seeAlso;
			else if ("PERF_COUNTER_BULK_COUNT"    .equals(cntr_type_name)) desc = "Provides the average number of operations per second. <b>Two readings of cntr_value will be required for this counter type, in order to get the per second averages</b><br>"
			                                                                    + Version.getAppName()+" does this for you just select the <i>rate</i> counters and the <i>per second</i> is calculated for you<br>"
			                                                                    + "<br><b>Formula</b>: <code>calculated_value = (Value2 - Value1) / (seconds between samples)</code>"
			                                                                    + seeAlso;
			else if ("PERF_LARGE_RAW_FRACTION"    .equals(cntr_type_name)) desc = "Used in conjunction with PERF_LARGE_RAW_BASE to calculate ratio values, such as the cache hit ratio<br>"
			                                                                    + "in short: PERF_LARGE_RAW_FRACTION / PERF_LARGE_RAW_BASE<br>"
			                                                                    + "<br><b>Formula</b>: <code>calculated_value = (Value / BaseValue) * 100</code>"
			                                                                    + seeAlso;
			else if ("PERF_AVERAGE_BULK"          .equals(cntr_type_name)) desc = "Used to calculate an average number of operations completed during a time interval; like PERF_LARGE_RAW_FRACTION, it uses PERF_LARGE_RAW_BASE to do the calculation<br>"
			                                                                    + "But you also need to diff-calc the A and B values in the formula...<br>"
			                                                                    + "in short: (A2 - A1) / (B2 - B1)<br>"
			                                                                    + "<br><b>Formula</b>: <code>calculated_value = (Value2 - Value1) / (BaseValue2 - BaseValue1)</code>"
			                                                                    + seeAlso;
			else if ("PERF_LARGE_RAW_BASE"        .equals(cntr_type_name)) desc = "Used in the translation of PERF_LARGE_RAW_FRACTION and PERF_AVERAGE_BULK values to readable output; should not be displayed alone.<br>"
			                                                                    + "Internal use."
			                                                                    + seeAlso;
			else desc = "Unknown cntr_type_name value: <b>"+cntr_type_name+"</b>";

			
			return "<html><b>"+cntr_type_name+"</b><br><br>"+desc+"</html>";
		}

		return super.getToolTipTextOnTableCell(e, colName, cellValue, modelRow, modelCol);
	}
	
	@Override
	public void sendAlarmRequest()
	{
		if ( ! hasDiffData() )
			return;
		
		if ( ! AlarmHandler.hasInstance() )
			return;

		boolean debugPrint = Configuration.getCombinedConfiguration().getBooleanProperty("sendAlarmRequest.debug", _logger.isDebugEnabled());
		
		AlarmHandler alarmHandler = AlarmHandler.getInstance();
		
		CountersModel cm = this;

//		boolean debugPrint = System.getProperty("sendAlarmRequest.debug", "false").equalsIgnoreCase("true");


		//-------------------------------------------------------
		// Free list stalls/sec
		//-------------------------------------------------------
		if (isSystemAlarmsForColumnEnabledAndInTimeRange("FreeListStalls"))
		{
			String pk = createPkStr(":Buffer Manager", "Free list stalls/sec", "");
			
			Double freeListStalls = this.getAbsValueAsDouble(pk, "calculated_value");
			
			if (freeListStalls != null)
			{
				double threshold = Configuration.getCombinedConfiguration().getDoubleProperty(PROPKEY_alarm_FreeListStalls, DEFAULT_alarm_FreeListStalls);
				
				if (debugPrint || _logger.isDebugEnabled())
					System.out.println("##### sendAlarmRequest("+cm.getName()+"): FreeListStalls -- threshold="+threshold+", freeListStalls='"+freeListStalls+"'.");

				if (freeListStalls > threshold)
				{
//					AlarmEvent ae = new AlarmEventPerfCounterWarning(cm, "Free list stalls/sec", freeListStalls, "Buffer Cache is probably to small... 'Free list stalls/sec'=" + freeListStalls + ". Waiting for memory to become available before a page can be added into the buffer pool.", threshold);
					AlarmEvent ae = new AlarmEventPerfCounter(cm, Severity.INFO, "Free list stalls/sec", freeListStalls, "Buffer Cache is probably to small... 'Free list stalls/sec'=" + freeListStalls + ". Waiting for memory to become available before a page can be added into the buffer pool.", threshold);
					
					int raiseDelay = Configuration.getCombinedConfiguration().getIntProperty(PROPKEY_alarm_FreeListStalls_delay, DEFAULT_alarm_FreeListStalls_delay);
					ae.setRaiseDelayInSec(raiseDelay);
					
					alarmHandler.addAlarm( ae );
				}
			}
		}

		//-------------------------------------------------------
		// Lazy writes/sec
		//-------------------------------------------------------
		if (isSystemAlarmsForColumnEnabledAndInTimeRange("LazyWrites"))
		{
			String pk = createPkStr(":Buffer Manager", "Lazy writes/sec", "");
			
			Double lazyWrites = this.getAbsValueAsDouble(pk, "calculated_value");
			
			if (lazyWrites != null)
			{
				double threshold = Configuration.getCombinedConfiguration().getDoubleProperty(PROPKEY_alarm_LazyWrites, DEFAULT_alarm_LazyWrites);
				
				if (debugPrint || _logger.isDebugEnabled())
					System.out.println("##### sendAlarmRequest("+cm.getName()+"): LazyWrites -- threshold="+threshold+", lazyWrites='"+lazyWrites+"'.");

				if (lazyWrites > threshold)
				{
//					AlarmEvent ae = new AlarmEventPerfCounterWarning(cm, "Lazy writes/sec", lazyWrites, "Buffer Cache is probably to small... 'Lazy writes/sec'=" + lazyWrites + ". Lazy Writer process moves 'dirty' pages from cache/buffer-pool to disk. This is an indication of 'memory pressure' or 'to little memory'.", threshold);
					AlarmEvent ae = new AlarmEventPerfCounter(cm, Severity.INFO, "Lazy writes/sec", lazyWrites, "Buffer Cache is probably to small... 'Lazy writes/sec'=" + lazyWrites + ". Lazy Writer process moves 'dirty' pages from cache/buffer-pool to disk. This is an indication of 'memory pressure' or 'to little memory'.", threshold);

					int raiseDelay = Configuration.getCombinedConfiguration().getIntProperty(PROPKEY_alarm_LazyWrites_delay, DEFAULT_alarm_LazyWrites_delay);
					ae.setRaiseDelayInSec(raiseDelay);
					
					alarmHandler.addAlarm( ae );
				}
			}
		}
	}

	public static final String  PROPKEY_alarm_FreeListStalls       = CM_NAME + ".alarm.system.if.FreeListStalls.gt";
	public static final double  DEFAULT_alarm_FreeListStalls       = 0.5;
	public static final String  PROPKEY_alarm_FreeListStalls_delay = CM_NAME + ".alarm.system.if.FreeListStalls.raise.delay";
	public static final int     DEFAULT_alarm_FreeListStalls_delay = 120; // 2 minutes

	public static final String  PROPKEY_alarm_LazyWrites           = CM_NAME + ".alarm.system.if.LazyWrites.gt";
	public static final double  DEFAULT_alarm_LazyWrites           = 20;
	public static final String  PROPKEY_alarm_LazyWrites_delay     = CM_NAME + ".alarm.system.if.LazyWrites.raise.delay";
	public static final int     DEFAULT_alarm_LazyWrites_delay     = 180; // 3 minutes

	@Override
	public List<CmSettingsHelper> getLocalAlarmSettings()
	{
		Configuration conf = Configuration.getCombinedConfiguration();
		List<CmSettingsHelper> list = new ArrayList<>();
		
		CmSettingsHelper.Type isAlarmSwitch = CmSettingsHelper.Type.IS_ALARM_SWITCH;
		
		list.add(new CmSettingsHelper("FreeListStalls",            isAlarmSwitch, PROPKEY_alarm_FreeListStalls      , Double .class, conf.getDoubleProperty(PROPKEY_alarm_FreeListStalls      , DEFAULT_alarm_FreeListStalls      ), DEFAULT_alarm_FreeListStalls      , "If 'Free list stalls/sec' is greater than 0.5 then send 'AlarmEventPerfCounterWarning'." ));
		list.add(new CmSettingsHelper("FreeListStalls RaiseDelay",                PROPKEY_alarm_FreeListStalls_delay, Integer.class, conf.getDoubleProperty(PROPKEY_alarm_FreeListStalls_delay, DEFAULT_alarm_FreeListStalls_delay), DEFAULT_alarm_FreeListStalls_delay, "If 'Free list stalls/sec' is true and has been so for 2 minutes then proceed with the Alarm Raise." ));
		list.add(new CmSettingsHelper("LazyWrites",                isAlarmSwitch, PROPKEY_alarm_LazyWrites          , Double.class , conf.getDoubleProperty(PROPKEY_alarm_LazyWrites          , DEFAULT_alarm_LazyWrites          ), DEFAULT_alarm_LazyWrites          , "If 'Lazy writes/sec' is greater than 20 then send 'AlarmEventPerfCounterWarning'." ));
		list.add(new CmSettingsHelper("LazyWrites RaiseDelay",                    PROPKEY_alarm_LazyWrites_delay    , Integer.class, conf.getDoubleProperty(PROPKEY_alarm_LazyWrites_delay    , DEFAULT_alarm_LazyWrites_delay    ), DEFAULT_alarm_LazyWrites_delay    , "If 'Lazy writes/sec' is true and has been so for 3 minutes then proceed with the Alarm Raise." ));

		return list;
	}
}
