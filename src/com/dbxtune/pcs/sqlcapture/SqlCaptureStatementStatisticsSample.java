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
package com.dbxtune.pcs.sqlcapture;

import java.sql.ResultSet;
import java.sql.Types;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;

import org.h2.tools.SimpleResultSet;

import com.dbxtune.pcs.PersistentCounterHandler;
import com.dbxtune.utils.StringUtil;
import com.google.gson.Gson;

/**
 * Holds Statement Statistics, which is used by a CM to report exeution times within known spans. 
 * <p>
 * For Example: how many Statements that executed, between
 * <ul>
 *     <li> 0-10 ms       </li>
 *     <li> 10-50 ms      </li>
 *     <li> 50-100 ms     </li>
 *     <li> 100-250 ms    </li>
 *     <li> 250-500 ms    </li>
 *     <li> 500-1000 ms   </li>
 *     <li> 1000-2500 ms  </li>
 *     <li> 2500-5000 ms  </li>
 *     <li> 5-10 sec      </li>
 *     <li> 10-15 sec     </li>
 *     <li> 15-20 sec     </li>
 *     <li> 20-30 sec     </li>
 *     <li> above 30 sec  </li>
 * </ul>
 * 
 */
public class SqlCaptureStatementStatisticsSample
{
	public final static String EXEC_SPAN_0ms_0lr_0pr    = "0 ms, 0 reads";
	public final static String EXEC_SPAN_0_to_1_ms      = "0-1 ms";
	public final static String EXEC_SPAN_1_to_2_ms      = "1-2 ms";
	public final static String EXEC_SPAN_2_to_5_ms      = "2-5 ms";
	public final static String EXEC_SPAN_5_to_10_ms     = "5-10 ms";
	public final static String EXEC_SPAN_10_to_20_ms    = "10-20 ms";
	public final static String EXEC_SPAN_20_to_50_ms    = "20-50 ms";
	public final static String EXEC_SPAN_50_to_100_ms   = "50-100 ms";
	public final static String EXEC_SPAN_100_to_200_ms  = "100-200 ms";
	public final static String EXEC_SPAN_200_to_500_ms  = "200-500 ms";
	public final static String EXEC_SPAN_500_to_1000_ms = "500-1000 ms";
	public final static String EXEC_SPAN_1_to_2_sec     = "1-2 sec";
	public final static String EXEC_SPAN_2_to_5_sec     = "2-5 sec";
	public final static String EXEC_SPAN_5_to_10_sec    = "5-10 sec";
	public final static String EXEC_SPAN_10_to_20_sec   = "10-20 sec";
	public final static String EXEC_SPAN_20_to_50_sec   = "20-50 sec";
	public final static String EXEC_SPAN_50_to_100_sec  = "50-100 sec";
	public final static String EXEC_SPAN_ABOVE_100_sec  = "above 100 sec";

//	public final static String EXEC_SPAN_SUMMARY        = "SUMMARY";
//	public final static String EXEC_SPAN_SUMMARY        = "_Total";

	public final static int    SUMMARY_STAT_ID          = 19;

	public enum StatType
	{
		EXEC_SPAN,
		DBNAME
	};
	
	private LinkedHashMap<String, StatCounter> _execTimeMap = new LinkedHashMap<>();
	
	private HashMap<String, StatCounter> _dbnameMap = new HashMap<>();

	/** When did we call addStatementStats() */
	private long  _lastUpdateTime = -1;

	
	/** When was the last time we update counters in this structure */
	public long getLastUpdateTime()
	{
		return _lastUpdateTime;
	}

	/** When was the last time we update counters in this structure */
	public void setLastUpdateTime()
	{
		_lastUpdateTime = System.currentTimeMillis();
	}

//	/** Get the row counters Map */
//	public LinkedHashMap<String, StatCounter> getStatCounterMap()
//	{
//		return _execTimeMap;
//	}
	
	public static class StatCounter
	{
		public StatCounter(int statId, String name)
		{
			this.statId            = statId;
			this.name              = name;
			this.count             = 0;
			this.errorCount        = 0;
			this.errorCountMap     = null;
			this.sqlBatchCount     = 0;
			this.inProcCount       = 0;
			this.inStmntCacheCount = 0;
			this.dynamicStmntCount = 0;

			this.sumExecTimeMs     = 0;
			this.maxExecTimeMs     = 0;

//			this.cntLogicalReads   = 0;
			this.sumLogicalReads   = 0;
			this.maxLogicalReads   = 0;

//			this.cntPhysicalReads  = 0;
			this.sumPhysicalReads  = 0;
			this.maxPhysicalReads  = 0;

//			this.cntCpuTime        = 0;
			this.sumCpuTime        = 0;
			this.maxCpuTime        = 0;

//			this.cntWaitTime       = 0;
			this.sumWaitTime       = 0;
			this.maxWaitTime       = 0;

//			this.cntRowsAffected   = 0;
			this.sumRowsAffected   = 0;
			this.maxRowsAffected   = 0;
			
			this.cntQueryOptimizationTimeGtZero = 0;
			this.sumQueryOptimizationTime       = 0;
			this.maxQueryOptimizationTime       = 0;
		}
		
		public int    statId;
		public String name;
		public long   count;
		public long   sqlBatchCount;
		public long   errorCount;
//		public Map<Integer, Long>   errorCountMap;
		public Map<Integer, Map<String, Long>> errorCountMap;  // Map<errorNumber, Map<dbname, counter>>
		public long   inProcCount;
		public long   inProcNullCount;
		public long   inStmntCacheCount;
		public long   dynamicStmntCount;

		public long   sumExecTimeMs;
		public int    maxExecTimeMs;

//		public long   cntLogicalReads; // counter where LogicalReads is above 0
		public long   sumLogicalReads;
		public int    maxLogicalReads;
		
//		public long   cntPhysicalReads; // counter where PhysicalReads is above 0
		public long   sumPhysicalReads;
		public int    maxPhysicalReads;

//		public long   cntCpuTime; // counter where CpuTime is above 0
		public long   sumCpuTime;
		public int    maxCpuTime;

//		public long   cntWaitTime; // counter where WaitTime is above 0
		public long   sumWaitTime;
		public int    maxWaitTime;

//		public long   cntRowsAffected; // counter where RowsAffected is above 0
		public long   sumRowsAffected;
		public int    maxRowsAffected;
		
		public long   cntQueryOptimizationTimeGtZero;
		public long   sumQueryOptimizationTime;
		public int    maxQueryOptimizationTime;

//----------------------------------------------
		public long   compileCount;     // Is this a better name than 'cntQueryOptimizationTimeGtZero'
		public long   compileCount_withTime_gt10_lt100;
		public long   compileCount_withTime_gt100_lt1000;
		public long   compileCount_withTime_gt1000;
		public long   sumCompileTime;   // Is this a better name than 'sumQueryOptimizationTime'
		public int    maxCompileTime;   // Is this a better name than 'maxQueryOptimizationTime'

//TODO: If we want to SEPARATE Pure Language, Language in Stmnt Cache, Prepared Statement in Stmnt Cache
		public long   compileCount_pureLang;
		public long   compileCount_pureLang_withTime_gt10_lt100;
		public long   compileCount_pureLang_withTime_gt100_lt1000;
		public long   compileCount_pureLang_withTime_gt1000;
		public long   sumCompileTime_pureLang;
		public int    maxCompileTime_pureLang;

		public long   compileCount_langInStmntCache;
		public long   compileCount_langInStmntCache_withTime_gt10_lt100;
		public long   compileCount_langInStmntCache_withTime_gt100_lt1000;
		public long   compileCount_langInStmntCache_withTime_gt1000;
		public long   sumCompileTime_langInStmntCache;
		public int    maxCompileTime_langInStmntCache;

		public long   compileCount_dynamicInStmntCache;
		public long   compileCount_dynamicInStmntCache_withTime_gt10_lt100;
		public long   compileCount_dynamicInStmntCache_withTime_gt100_lt1000;
		public long   compileCount_dynamicInStmntCache_withTime_gt1000;
		public long   sumCompileTime_dynamicInStmntCache;
		public int    maxCompileTime_dynamicInStmntCache;
//----------------------------------------------
	}
	
	public SqlCaptureStatementStatisticsSample()
	{
		synchronized (this)
		{
			_execTimeMap = new LinkedHashMap<>();

			_execTimeMap.put(EXEC_SPAN_0ms_0lr_0pr   , new StatCounter(1,  EXEC_SPAN_0ms_0lr_0pr   ));
			_execTimeMap.put(EXEC_SPAN_0_to_1_ms     , new StatCounter(2,  EXEC_SPAN_0_to_1_ms     ));
			_execTimeMap.put(EXEC_SPAN_1_to_2_ms     , new StatCounter(3,  EXEC_SPAN_1_to_2_ms     ));
			_execTimeMap.put(EXEC_SPAN_2_to_5_ms     , new StatCounter(4,  EXEC_SPAN_2_to_5_ms     ));
			_execTimeMap.put(EXEC_SPAN_5_to_10_ms    , new StatCounter(5,  EXEC_SPAN_5_to_10_ms    ));
			_execTimeMap.put(EXEC_SPAN_10_to_20_ms   , new StatCounter(6,  EXEC_SPAN_10_to_20_ms   ));
			_execTimeMap.put(EXEC_SPAN_20_to_50_ms   , new StatCounter(7,  EXEC_SPAN_20_to_50_ms   ));
			_execTimeMap.put(EXEC_SPAN_50_to_100_ms  , new StatCounter(8,  EXEC_SPAN_50_to_100_ms  ));
			_execTimeMap.put(EXEC_SPAN_100_to_200_ms , new StatCounter(9,  EXEC_SPAN_100_to_200_ms ));
			_execTimeMap.put(EXEC_SPAN_200_to_500_ms , new StatCounter(10, EXEC_SPAN_200_to_500_ms ));
			_execTimeMap.put(EXEC_SPAN_500_to_1000_ms, new StatCounter(11, EXEC_SPAN_500_to_1000_ms));
			_execTimeMap.put(EXEC_SPAN_1_to_2_sec    , new StatCounter(12, EXEC_SPAN_1_to_2_sec    ));
			_execTimeMap.put(EXEC_SPAN_2_to_5_sec    , new StatCounter(13, EXEC_SPAN_2_to_5_sec    ));
			_execTimeMap.put(EXEC_SPAN_5_to_10_sec   , new StatCounter(14, EXEC_SPAN_5_to_10_sec   ));
			_execTimeMap.put(EXEC_SPAN_10_to_20_sec  , new StatCounter(15, EXEC_SPAN_10_to_20_sec  ));
			_execTimeMap.put(EXEC_SPAN_20_to_50_sec  , new StatCounter(16, EXEC_SPAN_20_to_50_sec  ));
			_execTimeMap.put(EXEC_SPAN_50_to_100_sec , new StatCounter(17, EXEC_SPAN_50_to_100_sec ));
			_execTimeMap.put(EXEC_SPAN_ABOVE_100_sec , new StatCounter(18, EXEC_SPAN_ABOVE_100_sec ));
			
			// NOTE: If we add statId > 18 ... also change: SUMMARY_STAT_ID
			
			// Summary will always be updated (but this record needs to be discarded by graphs that does SUM over all records in the table)
			//_execTimeMap.put(EXEC_SPAN_SUMMARY       , new StatCounter(SUMMARY_STAT_ID, EXEC_SPAN_SUMMARY       ));


			// Create a Map for DBNAME as well
			_dbnameMap = new HashMap<>();
		}
	}

	private void addExecTimeInternal(StatType type, String key, int spid, int batchId, int execTimeMs, int logicalReads, int physicalReads, int cpuTime, int waitTime, int rowsAffected, int errorStatus, int ssqlId, int procedureId, String procName, int contextId, int lineNumber, String dbname, int dbid, int queryOptimizationTime)
	{
		//----------------------------------------------------------------------------------------
		//---- README ---- README ---- README ---- README ---- README ---- README ---- README ----
		//---- README ---- README ---- README ---- README ---- README ---- README ---- README ----
		//----------------------------------------------------------------------------------------
		//
		// records from 'master.dbo.monSysStatement' is a little bit of **MYSTERY** (or a BLODDY MESS)
		// I have NOT found any good documentation of the fields...
		// Especially when comparing:
		//   -- Dynamic Statements called from: C - ct_dynamic and Java - PreparedStatement (held by the Statement Cache)  
		//   -- Language Statement (held by the Statement Cache)  
		//   -- Plain Language Request (that are compiled every time)
		//
		// For instance WHEN could I trust 'QueryOptimizationTime' and 'LogicalReads'
		//
		// Here is a SUMMARY of my findings when testing on ASE 16.0 SP04 PL05
		// -- FIXME
		// It looks like 'ContextID' and 'LineNumber' could differentiate some things
		//
		// ** Dynamic/Prepare Statements
		//    Warning: For Dynamic/Prepare Statements it looks like 'SsqlId' is ALWAYS 0 (but the ProcedureID is filled in)
		//    - At FIRST execution (compile)
		//         +---------------------------+-------+------+-------+-------------+---------+----------+---------------------+------------+-------------------+-----+----------+-------------+----+-------------+----------+-------+---------+----------+-------+--------+----------+-------------+------------+-------------+-----------+---------------+-----------------+------------+------------+-----------+-------+------+-------------+---------------+---------------+-----------+-----------------+------------------+---------------------+------+-------------------+-------------------+
		//         |ProcName                   |BatchID|SsqlId|HashKey|ProcedureID  |ContextID|LineNumber|QueryOptimizationTime|LogicalReads|SEPARATOR          |SPID |InstanceID|KPID         |DBID|ProcedureID  |PlanID    |BatchID|ContextID|LineNumber|CpuTime|WaitTime|MemUsageKB|PhysicalReads|LogicalReads|PagesModified|PacketsSent|PacketsReceived|NetworkPacketSize|PlansAltered|RowsAffected|ErrorStatus|HashKey|SsqlId|ProcNestLevel|StatementNumber|SnapCodegenTime|SnapJITTime|SnapExecutionTime|SnapExecutionCount|QueryOptimizationTime|DBName|StartTime          |EndTime            |
		//         +---------------------------+-------+------+-------+-------------+---------+----------+---------------------+------------+-------------------+-----+----------+-------------+----+-------------+----------+-------+---------+----------+-------+--------+----------+-------------+------------+-------------+-----------+---------------+-----------------+------------+------------+-----------+-------+------+-------------+---------------+---------------+-----------+-----------------+------------------+---------------------+------+-------------------+-------------------+
		//         |*sq1959136698_2018575467ss*|10     |0     |0      |1 959 136 698|1        |1         |9 131                |478 321     |>>> ALL COLUMNS >>>|1 281|0         |1 195 967 046|4   |1 959 136 698|22 933 810|10     |1        |1         |852    |0       |34 476    |114          |478 321     |0            |35         |0              |2 048            |0           |181         |0          |0      |0     |1            |1              |0              |0          |0                |0                 |9 131                |PML   |2025-08-20 17:32:16|2025-08-20 17:32:17|
		//         |(NULL)                     |10     |0     |0      |0            |0        |-1        |9 131                |0           |>>> ALL COLUMNS >>>|1 281|0         |1 195 967 046|4   |0            |0         |10     |0        |-1        |0      |0       |286       |0            |0           |0            |0          |0              |2 048            |0           |0           |0          |0      |0     |0            |0              |0              |0          |0                |0                 |9 131                |PML   |2025-08-20 17:32:07|2025-08-20 17:32:17|
		//         +---------------------------+-------+------+-------+-------------+---------+----------+---------------------+------------+-------------------+-----+----------+-------------+----+-------------+----------+-------+---------+----------+-------+--------+----------+-------------+------------+-------------+-----------+---------------+-----------------+------------+------------+-----------+-------+------+-------------+---------------+---------------+-----------+-----------------+------------------+---------------------+------+-------------------+-------------------+
		//    - At SECOND execution (reusing the cached value)
		//         +---------------------------+-------+------+-------+-------------+---------+----------+---------------------+------------+-------------------+-----+----------+-------------+----+-------------+----------+-------+---------+----------+-------+--------+----------+-------------+------------+-------------+-----------+---------------+-----------------+------------+------------+-----------+-------+------+-------------+---------------+---------------+-----------+-----------------+------------------+---------------------+------+-------------------+-------------------+
		//         |ProcName                   |BatchID|SsqlId|HashKey|ProcedureID  |ContextID|LineNumber|QueryOptimizationTime|LogicalReads|SEPARATOR          |SPID |InstanceID|KPID         |DBID|ProcedureID  |PlanID    |BatchID|ContextID|LineNumber|CpuTime|WaitTime|MemUsageKB|PhysicalReads|LogicalReads|PagesModified|PacketsSent|PacketsReceived|NetworkPacketSize|PlansAltered|RowsAffected|ErrorStatus|HashKey|SsqlId|ProcNestLevel|StatementNumber|SnapCodegenTime|SnapJITTime|SnapExecutionTime|SnapExecutionCount|QueryOptimizationTime|DBName|StartTime          |EndTime            |
		//         +---------------------------+-------+------+-------+-------------+---------+----------+---------------------+------------+-------------------+-----+----------+-------------+----+-------------+----------+-------+---------+----------+-------+--------+----------+-------------+------------+-------------+-----------+---------------+-----------------+------------+------------+-----------+-------+------+-------------+---------------+---------------+-----------+-----------------+------------------+---------------------+------+-------------------+-------------------+
		//         |*sq1959136698_2018575467ss*|13     |0     |0      |1 959 136 698|0        |1         |9 131                |478 321     |>>> ALL COLUMNS >>>|1 281|0         |1 195 967 046|4   |1 959 136 698|22 933 810|13     |0        |1         |793    |0       |914       |114          |478 321     |0            |35         |0              |2 048            |0           |181         |0          |0      |0     |0            |0              |0              |0          |0                |0                 |9 131                |PML   |2025-08-20 17:32:17|2025-08-20 17:32:18|
		//         +---------------------------+-------+------+-------+-------------+---------+----------+---------------------+------------+-------------------+-----+----------+-------------+----+-------------+----------+-------+---------+----------+-------+--------+----------+-------------+------------+-------------+-----------+---------------+-----------------+------------+------------+-----------+-------+------+-------------+---------------+---------------+-----------+-----------------+------------------+---------------------+------+-------------------+-------------------+
		//    - Conclusion
		//         * at compile: ProcName=*sq..., SsqlId=0, ProcedureID=#, ContextID=1, LineNumber=1
		//         * at re-use:  ProcName=*sq..., SsqlId=0, ProcedureID=#, ContextID=0, LineNumber=1   (disregard 'QueryOptimizationTime', since it IS "inherited" from compile-time)
		// 
		// ** Language (when statement cache is enabled)
		//    - At FIRST execution (compile)
		//         +---------------------------+-------+----------+-------------+-----------+---------+----------+---------------------+------------+-------------------+----+----------+-----------+----+-----------+------+-------+---------+----------+-------+--------+----------+-------------+------------+-------------+-----------+---------------+-----------------+------------+------------+-----------+-------------+----------+-------------+---------------+---------------+-----------+-----------------+------------------+---------------------+------+-------------------+-------------------+
		//         |ProcName                   |BatchID|SsqlId    |HashKey      |ProcedureID|ContextID|LineNumber|QueryOptimizationTime|LogicalReads|SEPARATOR          |SPID|InstanceID|KPID       |DBID|ProcedureID|PlanID|BatchID|ContextID|LineNumber|CpuTime|WaitTime|MemUsageKB|PhysicalReads|LogicalReads|PagesModified|PacketsSent|PacketsReceived|NetworkPacketSize|PlansAltered|RowsAffected|ErrorStatus|HashKey      |SsqlId    |ProcNestLevel|StatementNumber|SnapCodegenTime|SnapJITTime|SnapExecutionTime|SnapExecutionCount|QueryOptimizationTime|DBName|StartTime          |EndTime            |
		//         +---------------------------+-------+----------+-------------+-----------+---------+----------+---------------------+------------+-------------------+----+----------+-----------+----+-----------+------+-------+---------+----------+-------+--------+----------+-------------+------------+-------------+-----------+---------------+-----------------+------------+------------+-----------+-------------+----------+-------------+---------------+---------------+-----------+-----------------+------------------+---------------------+------+-------------------+-------------------+
		//         |*ss0059677521_1048078598ss*|16     |59 677 521|1 048 078 598|59 677 521 |2        |0         |7 002                |0           |>>> ALL COLUMNS >>>|430 |0         |134 808 471|4   |59 677 521 |80 299|16     |2        |0         |0      |0       |29 606    |0            |0           |0            |0          |0              |2 048            |0           |0           |0          |1 048 078 598|59 677 521|1            |1              |0              |0          |0                |0                 |7 002                |PML   |2025-08-20 18:01:23|2025-08-20 18:01:23|
		//         |*ss0059677521_1048078598ss*|16     |59 677 521|1 048 078 598|59 677 521 |2        |1         |7 002                |499 519     |>>> ALL COLUMNS >>>|430 |0         |134 808 471|4   |59 677 521 |80 299|16     |2        |1         |761    |0       |718       |114          |499 519     |0            |89         |0              |2 048            |0           |467         |0          |1 048 078 598|59 677 521|1            |2              |0              |0          |0                |0                 |7 002                |PML   |2025-08-20 18:01:23|2025-08-20 18:01:24|
		//         +---------------------------+-------+----------+-------------+-----------+---------+----------+---------------------+------------+-------------------+----+----------+-----------+----+-----------+------+-------+---------+----------+-------+--------+----------+-------------+------------+-------------+-----------+---------------+-----------------+------------+------------+-----------+-------------+----------+-------------+---------------+---------------+-----------+-----------------+------------------+---------------------+------+-------------------+-------------------+
		//    - At SECOND execution (reusing the cached value)
		//         +---------------------------+-------+----------+-------------+-----------+---------+----------+---------------------+------------+-------------------+----+----------+-----------+----+-----------+------+-------+---------+----------+-------+--------+----------+-------------+------------+-------------+-----------+---------------+-----------------+------------+------------+-----------+-------------+----------+-------------+---------------+---------------+-----------+-----------------+------------------+---------------------+------+-------------------+-------------------+
		//         |ProcName                   |BatchID|SsqlId    |HashKey      |ProcedureID|ContextID|LineNumber|QueryOptimizationTime|LogicalReads|SEPARATOR          |SPID|InstanceID|KPID       |DBID|ProcedureID|PlanID|BatchID|ContextID|LineNumber|CpuTime|WaitTime|MemUsageKB|PhysicalReads|LogicalReads|PagesModified|PacketsSent|PacketsReceived|NetworkPacketSize|PlansAltered|RowsAffected|ErrorStatus|HashKey      |SsqlId    |ProcNestLevel|StatementNumber|SnapCodegenTime|SnapJITTime|SnapExecutionTime|SnapExecutionCount|QueryOptimizationTime|DBName|StartTime          |EndTime            |
		//         +---------------------------+-------+----------+-------------+-----------+---------+----------+---------------------+------------+-------------------+----+----------+-----------+----+-----------+------+-------+---------+----------+-------+--------+----------+-------------+------------+-------------+-----------+---------------+-----------------+------------+------------+-----------+-------------+----------+-------------+---------------+---------------+-----------+-----------------+------------------+---------------------+------+-------------------+-------------------+
		//         |*ss0059677521_1048078598ss*|17     |59 677 521|1 048 078 598|59 677 521 |1        |0         |0                    |0           |>>> ALL COLUMNS >>>|430 |0         |134 808 471|4   |59 677 521 |80 299|17     |1        |0         |0      |0       |270       |0            |0           |0            |0          |0              |2 048            |0           |0           |0          |1 048 078 598|59 677 521|1            |1              |0              |0          |0                |0                 |0                    |PML   |2025-08-20 18:01:24|2025-08-20 18:01:24|
		//         |*ss0059677521_1048078598ss*|17     |59 677 521|1 048 078 598|59 677 521 |1        |1         |0                    |499 519     |>>> ALL COLUMNS >>>|430 |0         |134 808 471|4   |59 677 521 |80 299|17     |1        |1         |745    |0       |718       |114          |499 519     |0            |89         |0              |2 048            |0           |467         |0          |1 048 078 598|59 677 521|1            |2              |0              |0          |0                |0                 |0                    |PML   |2025-08-20 18:01:24|2025-08-20 18:01:25|
		//         +---------------------------+-------+----------+-------------+-----------+---------+----------+---------------------+------------+-------------------+----+----------+-----------+----+-----------+------+-------+---------+----------+-------+--------+----------+-------------+------------+-------------+-----------+---------------+-----------------+------------+------------+-----------+-------------+----------+-------------+---------------+---------------+-----------+-----------------+------------------+---------------------+------+-------------------+-------------------+
		//    - Conclusion
		//         * at compile: ProcName=*ss..., SsqlId>0, HashKey>0, ProcedureID=SsqlId, ContextID=2, LineNumber=0 COMPILE (LineNumber=1 EXEC-AFTER-COMPILE)
		//         * at re-use:  ProcName=*ss..., SsqlId>0, HashKey>0, ProcedureID=SsqlId, ContextID=1, LineNumber=1 (QueryOptimizationTime NOT "inherited" from compile-time)
		//
		// ** PURE Language (NOT saved in Statement Cache)
		//    - Not cached, compiled every time
		//    - ProcName=null, SsqlId=0, HashKey=0, ProcedureID=0, ContextID=0
		//    
		//    - At FIRST execution (compile)
		//         +--------+-------+------+-------+-----------+---------+----------+---------------------+------------+-------------------+----+----------+-----------+----+-----------+------+-------+---------+----------+-------+--------+----------+-------------+------------+-------------+-----------+---------------+-----------------+------------+------------+-----------+-------+------+-------------+---------------+---------------+-----------+-----------------+------------------+---------------------+------+-------------------+-------------------+
		//         |ProcName|BatchID|SsqlId|HashKey|ProcedureID|ContextID|LineNumber|QueryOptimizationTime|LogicalReads|SEPARATOR          |SPID|InstanceID|KPID       |DBID|ProcedureID|PlanID|BatchID|ContextID|LineNumber|CpuTime|WaitTime|MemUsageKB|PhysicalReads|LogicalReads|PagesModified|PacketsSent|PacketsReceived|NetworkPacketSize|PlansAltered|RowsAffected|ErrorStatus|HashKey|SsqlId|ProcNestLevel|StatementNumber|SnapCodegenTime|SnapJITTime|SnapExecutionTime|SnapExecutionCount|QueryOptimizationTime|DBName|StartTime          |EndTime            |
		//         +--------+-------+------+-------+-----------+---------+----------+---------------------+------------+-------------------+----+----------+-----------+----+-----------+------+-------+---------+----------+-------+--------+----------+-------------+------------+-------------+-----------+---------------+-----------------+------------+------------+-----------+-------+------+-------------+---------------+---------------+-----------+-----------------+------------------+---------------------+------+-------------------+-------------------+
		//         |(NULL)  |19     |0     |0      |0          |0        |1         |3 996                |499 519     |>>> ALL COLUMNS >>>|430 |0         |134 808 471|4   |0          |0     |19     |0        |1         |702    |0       |25 532    |114          |499 519     |0            |89         |0              |2 048            |0           |467         |0          |0      |0     |0            |0              |0              |0          |0                |0                 |3 996                |PML   |2025-08-20 18:01:29|2025-08-20 18:01:30|
		//         +--------+-------+------+-------+-----------+---------+----------+---------------------+------------+-------------------+----+----------+-----------+----+-----------+------+-------+---------+----------+-------+--------+----------+-------------+------------+-------------+-----------+---------------+-----------------+------------+------------+-----------+-------+------+-------------+---------------+---------------+-----------+-----------------+------------------+---------------------+------+-------------------+-------------------+
		//    - At SECOND execution (reusing the cached value)
		//         +--------+-------+------+-------+-----------+---------+----------+---------------------+------------+-------------------+----+----------+-----------+----+-----------+------+-------+---------+----------+-------+--------+----------+-------------+------------+-------------+-----------+---------------+-----------------+------------+------------+-----------+-------+------+-------------+---------------+---------------+-----------+-----------------+------------------+---------------------+------+-------------------+-------------------+
		//         |ProcName|BatchID|SsqlId|HashKey|ProcedureID|ContextID|LineNumber|QueryOptimizationTime|LogicalReads|SEPARATOR          |SPID|InstanceID|KPID       |DBID|ProcedureID|PlanID|BatchID|ContextID|LineNumber|CpuTime|WaitTime|MemUsageKB|PhysicalReads|LogicalReads|PagesModified|PacketsSent|PacketsReceived|NetworkPacketSize|PlansAltered|RowsAffected|ErrorStatus|HashKey|SsqlId|ProcNestLevel|StatementNumber|SnapCodegenTime|SnapJITTime|SnapExecutionTime|SnapExecutionCount|QueryOptimizationTime|DBName|StartTime          |EndTime            |
		//         +--------+-------+------+-------+-----------+---------+----------+---------------------+------------+-------------------+----+----------+-----------+----+-----------+------+-------+---------+----------+-------+--------+----------+-------------+------------+-------------+-----------+---------------+-----------------+------------+------------+-----------+-------+------+-------------+---------------+---------------+-----------+-----------------+------------------+---------------------+------+-------------------+-------------------+
		//         |(NULL)  |20     |0     |0      |0          |0        |1         |4 058                |499 519     |>>> ALL COLUMNS >>>|430 |0         |134 808 471|4   |0          |0     |20     |0        |1         |774    |2       |25 532    |114          |499 519     |0            |89         |0              |2 048            |0           |467         |0          |0      |0     |0            |0              |0              |0          |0                |0                 |4 058                |PML   |2025-08-20 18:01:34|2025-08-20 18:01:35|
		//         +--------+-------+------+-------+-----------+---------+----------+---------------------+------------+-------------------+----+----------+-----------+----+-----------+------+-------+---------+----------+-------+--------+----------+-------------+------------+-------------+-----------+---------------+-----------------+------------+------------+-----------+-------+------+-------------+---------------+---------------+-----------+-----------------+------------------+---------------------+------+-------------------+-------------------+
		//
		// ** PURE Language -- Calling a Stored Procedure
		//    - The ProcName=null (ProcedureID=0), and BatchID=21 is the Language call
		//    - what the procedure does is: ProcName!=null, ContextID>0    (not sure if QueryOptimizationTime will always be 0 or if it will contain value (on first execution, and also on second execution etc)
		//         +-----------------------+-------+------+-------+-------------+---------+----------+---------------------+------------+-------------------+-----+----------+-----------+------+-------------+------+-------+---------+----------+-------+--------+----------+-------------+------------+-------------+-----------+---------------+-----------------+------------+------------+-----------+-------+------+-------------+---------------+---------------+-----------+-----------------+------------------+---------------------+--------------+-------------------+-------------------+
		//         |ProcName               |BatchID|SsqlId|HashKey|ProcedureID  |ContextID|LineNumber|QueryOptimizationTime|LogicalReads|SEPARATOR          |SPID |InstanceID|KPID       |DBID  |ProcedureID  |PlanID|BatchID|ContextID|LineNumber|CpuTime|WaitTime|MemUsageKB|PhysicalReads|LogicalReads|PagesModified|PacketsSent|PacketsReceived|NetworkPacketSize|PlansAltered|RowsAffected|ErrorStatus|HashKey|SsqlId|ProcNestLevel|StatementNumber|SnapCodegenTime|SnapJITTime|SnapExecutionTime|SnapExecutionCount|QueryOptimizationTime|DBName        |StartTime          |EndTime            |
		//         +-----------------------+-------+------+-------+-------------+---------+----------+---------------------+------------+-------------------+-----+----------+-----------+------+-------------+------+-------+---------+----------+-------+--------+----------+-------------+------------+-------------+-----------+---------------+-----------------+------------+------------+-----------+-------+------+-------------+---------------+---------------+-----------+-----------------+------------------+---------------------+--------------+-------------------+-------------------+
		//         |test_dummy_proc_1      |21     |0     |0      |632 203 563  |1        |4         |0                    |0           |>>> ALL COLUMNS >>>|1 309|0         |136 643 450|2     |632 203 563  |80 637|21     |1        |4         |0      |0       |654       |0            |0           |0            |0          |0              |2 048            |0           |0           |0          |0      |0     |1            |1              |0              |0          |0                |0                 |0                    |tempdb        |2025-08-21 01:11:04|2025-08-21 01:11:04|
		//         |test_dummy_proc_1_inner|21     |0     |0      |616 203 506  |2        |4         |0                    |0           |>>> ALL COLUMNS >>>|1 309|0         |136 643 450|2     |616 203 506  |80 638|21     |2        |4         |0      |0       |140       |0            |0           |0            |0          |0              |2 048            |0           |0           |0          |0      |0     |2            |3              |0              |0          |0                |0                 |0                    |tempdb        |2025-08-21 01:11:04|2025-08-21 01:11:04|
		//         |test_dummy_proc_1_inner|21     |0     |0      |616 203 506  |2        |5         |0                    |1           |>>> ALL COLUMNS >>>|1 309|0         |136 643 450|2     |616 203 506  |80 638|21     |2        |5         |0      |0       |0         |0            |1           |0            |0          |0              |2 048            |0           |1           |0          |0      |0     |2            |4              |0              |0          |0                |0                 |0                    |tempdb        |2025-08-21 01:11:04|2025-08-21 01:11:04|
		//         |test_dummy_proc_1_inner|21     |0     |0      |616 203 506  |2        |6         |0                    |0           |>>> ALL COLUMNS >>>|1 309|0         |136 643 450|2     |616 203 506  |80 638|21     |2        |6         |0      |0       |0         |0            |0           |0            |0          |0              |2 048            |0           |0           |0          |0      |0     |2            |5              |0              |0          |0                |0                 |0                    |tempdb        |2025-08-21 01:11:04|2025-08-21 01:11:04|
		//         |test_dummy_proc_1      |21     |0     |0      |632 203 563  |1        |5         |0                    |0           |>>> ALL COLUMNS >>>|1 309|0         |136 643 450|2     |632 203 563  |80 637|21     |1        |5         |0      |0       |14        |0            |0           |0            |0          |0              |2 048            |0           |0           |0          |0      |0     |1            |2              |0              |0          |0                |0                 |0                    |tempdb        |2025-08-21 01:11:04|2025-08-21 01:11:04|
		//         |test_dummy_proc_1      |21     |0     |0      |632 203 563  |1        |6         |0                    |1           |>>> ALL COLUMNS >>>|1 309|0         |136 643 450|2     |632 203 563  |80 637|21     |1        |6         |0      |0       |0         |0            |1           |0            |0          |0              |2 048            |0           |1           |0          |0      |0     |1            |6              |0              |0          |0                |0                 |0                    |tempdb        |2025-08-21 01:11:04|2025-08-21 01:11:04|
		//         |test_dummy_proc_1      |21     |0     |0      |632 203 563  |1        |7         |0                    |0           |>>> ALL COLUMNS >>>|1 309|0         |136 643 450|2     |632 203 563  |80 637|21     |1        |7         |0      |0       |0         |0            |0           |0            |0          |0              |2 048            |0           |0           |0          |0      |0     |1            |7              |0              |0          |0                |0                 |0                    |tempdb        |2025-08-21 01:11:04|2025-08-21 01:11:04|
		//         |(NULL)                 |21     |0     |0      |0            |0        |1         |0                    |0           |>>> ALL COLUMNS >>>|1 309|0         |136 643 450|4     |0            |0     |21     |0        |1         |0      |0       |12        |0            |0           |0            |0          |0              |2 048            |0           |0           |0          |0      |0     |0            |0              |0              |0          |0                |0                 |0                    |PML           |2025-08-21 01:11:04|2025-08-21 01:11:04|
		//       Below is probably JdbcResultSetMetaData lookups... BatchID is higher, but next SQLText BatchID is 25 ...
		//         |sp_jdbc_datatype_info  |22     |0     |0      |1 086 623 883|1        |5         |0                    |0           |>>> ALL COLUMNS >>>|1 309|0         |136 643 450|31 514|1 086 623 883|2 366 |22     |1        |5         |0      |0       |14        |0            |0           |0            |0          |0              |2 048            |0           |0           |0          |0      |0     |1            |0              |0              |0          |0                |0                 |0                    |sybsystemprocs|2025-08-21 01:11:04|2025-08-21 01:11:04|
		//         |sp_jdbc_datatype_info  |22     |0     |0      |1 086 623 883|1        |7         |0                    |0           |>>> ALL COLUMNS >>>|1 309|0         |136 643 450|31 514|1 086 623 883|2 366 |22     |1        |7         |0      |0       |0         |0            |0           |0            |0          |0              |2 048            |0           |0           |0          |0      |0     |1            |1              |0              |0          |0                |0                 |0                    |sybsystemprocs|2025-08-21 01:11:04|2025-08-21 01:11:04|
		//         |sp_jdbc_datatype_info  |22     |0     |0      |1 086 623 883|1        |10        |0                    |0           |>>> ALL COLUMNS >>>|1 309|0         |136 643 450|31 514|1 086 623 883|2 366 |22     |1        |10        |0      |0       |0         |0            |0           |0            |0          |0              |2 048            |0           |0           |0          |0      |0     |1            |2              |0              |0          |0                |0                 |0                    |sybsystemprocs|2025-08-21 01:11:04|2025-08-21 01:11:04|
		//         |sp_jdbc_datatype_info  |22     |0     |0      |1 086 623 883|1        |16        |0                    |0           |>>> ALL COLUMNS >>>|1 309|0         |136 643 450|31 514|1 086 623 883|2 366 |22     |1        |16        |0      |0       |0         |0            |0           |0            |0          |0              |2 048            |0           |0           |0          |0      |0     |1            |3              |0              |0          |0                |0                 |0                    |sybsystemprocs|2025-08-21 01:11:04|2025-08-21 01:11:04|
		//         |sp_jdbc_datatype_info  |22     |0     |0      |1 086 623 883|1        |18        |0                    |228         |>>> ALL COLUMNS >>>|1 309|0         |136 643 450|31 514|1 086 623 883|2 366 |22     |1        |18        |1      |0       |72        |0            |228         |0            |2          |0              |2 048            |0           |69          |0          |0      |0     |1            |4              |0              |0          |0                |0                 |0                    |sybsystemprocs|2025-08-21 01:11:04|2025-08-21 01:11:04|
		//         |sp_jdbc_datatype_info  |22     |0     |0      |1 086 623 883|1        |135       |0                    |0           |>>> ALL COLUMNS >>>|1 309|0         |136 643 450|31 514|1 086 623 883|2 366 |22     |1        |135       |0      |0       |0         |0            |0           |0            |0          |0              |2 048            |0           |1           |0          |0      |0     |1            |5              |0              |0          |0                |0                 |0                    |sybsystemprocs|2025-08-21 01:11:04|2025-08-21 01:11:04|
		//         |sp_drv_getdbtype       |24     |0     |0      |1 854 626 619|1        |0         |0                    |0           |>>> ALL COLUMNS >>>|1 309|0         |136 643 450|31 514|1 854 626 619|2 358 |24     |1        |0         |0      |0       |70        |0            |0           |0            |0          |0              |2 048            |0           |0           |0          |0      |0     |1            |0              |0              |0          |0                |0                 |0                    |sybsystemprocs|2025-08-21 01:11:04|2025-08-21 01:11:04|
		//         |sp_drv_getdbtype       |24     |0     |0      |1 854 626 619|1        |8         |0                    |3           |>>> ALL COLUMNS >>>|1 309|0         |136 643 450|31 514|1 854 626 619|2 358 |24     |1        |8         |0      |0       |0         |0            |3           |0            |0          |0              |2 048            |0           |1           |0          |0      |0     |1            |1              |0              |0          |0                |0                 |0                    |sybsystemprocs|2025-08-21 01:11:04|2025-08-21 01:11:04|
		//         |sp_drv_getdbtype       |24     |0     |0      |1 854 626 619|1        |9         |0                    |0           |>>> ALL COLUMNS >>>|1 309|0         |136 643 450|31 514|1 854 626 619|2 358 |24     |1        |9         |0      |0       |0         |0            |0           |0            |0          |0              |2 048            |0           |1           |0          |0      |0     |1            |2              |0              |0          |0                |0                 |0                    |sybsystemprocs|2025-08-21 01:11:04|2025-08-21 01:11:04|
		//         +-----------------------+-------+------+-------+-------------+---------+----------+---------------------+------------+-------------------+-----+----------+-----------+------+-------------+------+-------+---------+----------+-------+--------+----------+-------------+------------+-------------+-----------+---------------+-----------------+------------+------------+-----------+-------+------+-------------+---------------+---------------+-----------+-----------------+------------------+---------------------+--------------+-------------------+-------------------+
		//
		// NOTE: I have not yet tested other "handlers" like:
		//         * Cursor requests
		//         * EXEC(...) in a SQL Batch or more important EXEC(...) in a Stored procedure (how is that materialized?... is it a extra/?Special? entry that goes into Stmnt Cache)
		//         * RPC requests (if they show up in here at all)
		//		   * Probably more handlers like ???
		//
		// You may use 'com.dbxtune.test.AseStmntCacheMonSysXxx' to see how records are created...
		//----------------------------------------------------------------------------------------
		//---- README ---- README ---- README ---- README ---- README ---- README ---- README ----
		//---- README ---- README ---- README ---- README ---- README ---- README ---- README ----
		//----------------------------------------------------------------------------------------
		
		// Lets try to figure out WHAT the entry is (so we can update the correct members)
		boolean isDynamic_Compile        = false;
		boolean isDynamic_ReUse          = false;
		boolean isLangStmntCache_Compile = false;
		boolean isLangStmntCache_ReUse   = false;
		boolean isPureLanguage           = false;

		boolean isInProcExecution        = false;

		boolean exitEarly                = false;
		
		// TODO: The below isDynamic* & isLangStmntCache* & isPureLanguage CHECK/LOGIC needs to be verified with a BUNCH of test data (to make sure we are not missing anything)
		if (procName != null && procName.startsWith("*sq") && procedureId != 0)
		{
			if (contextId == 1) isDynamic_Compile = true;
			if (contextId == 0) isDynamic_ReUse   = true;
		}
		else if (procName != null && procName.startsWith("*ss") && procedureId != 0)
		{
			if (contextId == 2 && lineNumber == 0) isLangStmntCache_Compile = true;
			if (contextId == 1 && lineNumber == 1) isLangStmntCache_ReUse   = true;
		}
		else if (procName == null && ssqlId == 0 && procedureId == 0 && contextId == 0)
		{
			if (lineNumber == -1)
			{
				// LineNumber -1 -- Is when a Dynamic Request i compiled
				exitEarly = true;
//				System.out.println("             >>> isPureLanguage... but lineNumber == -1 ... so mark for EarlyExit");
			}
			else
			{
				isPureLanguage = true;
			}
		}
		else if (procedureId != 0 && contextId >= 1 && lineNumber >= 1)
		{
			isInProcExecution = true;
		}

		// Adjust some numbers (some might be "incorrect")
		if (isDynamic_ReUse)
		{
			// on Statement Cache RE-USE -->> The queryOptimizationTime is still PASSED. But I think we should reset that to ZERO (and just use the CpuTime, LogicalReads etc...)
			queryOptimizationTime = 0;
		}


        boolean inDebugDevelopMode = false;
        if (inDebugDevelopMode)
        {
        	if (StatType.DBNAME.equals(type))
        	{
        		String lastKnownSqlText = "";
        		ISqlCaptureBroker sqlCaptureBroker = PersistentCounterHandler.getInstance().getSqlCaptureBroker();
        		if (sqlCaptureBroker != null && sqlCaptureBroker instanceof SqlCaptureBrokerAse)
        		{
        			SqlCaptureBrokerAse aseSqlCaptureBroker = (SqlCaptureBrokerAse) sqlCaptureBroker;
        
        			boolean getAllAvailableBatches = false;
        			lastKnownSqlText = aseSqlCaptureBroker.getSqlText(spid, -1, batchId, getAllAvailableBatches);
        			// NOTE: *last* SQL Text is probably NOT available yet... since this code runs *before* get "SQLText" 
        		}
        		String xType = "";
        		if (isDynamic_Compile)        xType += "Dyn-Compl,";
        		if (isDynamic_ReUse)          xType += "Dyn-ReUse,";
        		if (isLangStmntCache_Compile) xType += "LSC-Compl,";
        		if (isLangStmntCache_ReUse)   xType += "LSC-ReUse,";
        		if (isPureLanguage)           xType += "Pure-Lang,";
        		if (isInProcExecution)        xType += "Exec-Proc,";
        		if ("".equals(xType))         xType += "-unknown-,";
        
        		if ("-unknown-,".equals(xType))
        		{
        			
        		}
        //		System.out.println(" >>> [exitEarly=" + exitEarly + "] xType=" + xType + " SPID=" + spid + ", BatchID=" + batchId + ", procName=" + procName + ", lineNumber=" + lineNumber + ", contextId=" + contextId + " -- isDynamic_Compile=" + isDynamic_Compile + ", isDynamic_ReUse=" + isDynamic_ReUse + ", isLangStmntCache_Compile=" + isLangStmntCache_Compile + ", isLangStmntCache_ReUse=" + isLangStmntCache_ReUse + ", isPureLang=" + isPureLanguage + ", isInProcExec=" + isInProcExecution + ", lastKnownSqlText=" + lastKnownSqlText);
        		System.out.println(" >>> [exitEarly=" + exitEarly + "] xType=" + xType + " SPID=" + spid + ", BatchID=" + batchId + ", procName=" + procName + ", lineNumber=" + lineNumber + ", contextId=" + contextId + ", queryOptimizationTime=" + queryOptimizationTime + ", lastKnownSqlText=" + lastKnownSqlText);
        //		System.out.println(" >>> [exitEarly=" + exitEarly + "] xType=" + xType + " SPID=" + spid + ", BatchID=" + batchId + ", procName=" + procName + ", lineNumber=" + lineNumber + ", contextId=" + contextId);
        	}
        }
		
		// TODO: how do we interprate "isInProcExecution"
		//       And what counters should we increment? (does the OUTER Language call, "SUM UPP" all the sub-procedure counters... I do NOT think so... But I must test it)
		
//TODO; // Check if the BELOW IS STILL VALID (and if it makes us MISS the Compile time)

		// NOTE: Should we keep this "earlyExit" to be "backward compatible" with earlier versions ???
		// if something seems to be from the statement cache, and the LineNumber is 0, then discard the entry
		// it looks like there are always 2 rows in monSysStatement when there are (*ss or *sq) objects
		// The row with lineNumber == 0, does not contain real values for: LogicalReads etc... it's probably the creation of the LWP
		if ( procName != null && lineNumber == 0 && (procName.startsWith("*ss") || procName.startsWith("*sq")) )
		{
			exitEarly = true;
		}

		// Exit early if this entry should be skipped
		if ( exitEarly )
		{
			return;
		}

		
		// Get a StatCounter "entry", which we will add information to
		// It may be: "execTimeDuration" or "dbname" 
		StatCounter st = null;
		if (StatType.EXEC_SPAN.equals(type))
		{
			st = _execTimeMap.get(key);
		}
		else if (StatType.DBNAME.equals(type))
		{
			st = _dbnameMap.get(key);
			if (st == null)
			{
				st = new StatCounter(dbid, key);
				_dbnameMap.put(key, st);
			}
		}
		else
		{
			
		}

		// Compile Count and Time (and in different areas: PureLanguage, LanguageRequestInStatementCache, DynamicRequestInStatementCache
		// Note: We might do early exists on some entries here.
		if (isDynamic_Compile || isLangStmntCache_Compile || isPureLanguage)
		{
			st.sumCompileTime += queryOptimizationTime;
			st.maxCompileTime = Math.max(st.maxCompileTime, queryOptimizationTime);

			st.compileCount++;
			if (queryOptimizationTime >= 10  && queryOptimizationTime <= 100)  st.compileCount_withTime_gt10_lt100++;
			if (queryOptimizationTime >= 100 && queryOptimizationTime <= 1000) st.compileCount_withTime_gt100_lt1000++;
			if (queryOptimizationTime >= 1000)                                 st.compileCount_withTime_gt1000++;

			// Dynamic Request (Prepared Statement) in Statement Cache
			if (isDynamic_Compile)
			{
				st.sumCompileTime_dynamicInStmntCache += queryOptimizationTime;
				st.maxCompileTime_dynamicInStmntCache = Math.max(st.maxCompileTime_dynamicInStmntCache, queryOptimizationTime);

				st.compileCount_dynamicInStmntCache++;
				if (queryOptimizationTime >= 10  && queryOptimizationTime <= 100)  st.compileCount_dynamicInStmntCache_withTime_gt10_lt100++;
				if (queryOptimizationTime >= 100 && queryOptimizationTime <= 1000) st.compileCount_dynamicInStmntCache_withTime_gt100_lt1000++;
				if (queryOptimizationTime >= 1000)                                 st.compileCount_dynamicInStmntCache_withTime_gt1000++;

				// DO NOT Exit early... It look like this entry contains both COMPILE and runtime/execution counters
				exitEarly = false;
			}

			// Language Request in Statement Cache
			if (isLangStmntCache_Compile)
			{
				st.sumCompileTime_langInStmntCache += queryOptimizationTime;
				st.maxCompileTime_langInStmntCache = Math.max(st.maxCompileTime_langInStmntCache, queryOptimizationTime);

				st.compileCount_langInStmntCache++;
				if (queryOptimizationTime >= 10  && queryOptimizationTime <= 100)  st.compileCount_langInStmntCache_withTime_gt10_lt100++;
				if (queryOptimizationTime >= 100 && queryOptimizationTime <= 1000) st.compileCount_langInStmntCache_withTime_gt100_lt1000++;
				if (queryOptimizationTime >= 1000)                                 st.compileCount_langInStmntCache_withTime_gt1000++;
				
				// Exit EARLY... we don't need to record runtime/execution counters if it's a COMPILE (of Language Statement to Statement Cache)
				exitEarly = true;
			}

			// Pure Language Request
			if (isPureLanguage)
			{
				st.sumCompileTime_pureLang += queryOptimizationTime;
				st.maxCompileTime_pureLang = Math.max(st.maxCompileTime_pureLang, queryOptimizationTime);

				st.compileCount_pureLang++;
				if (queryOptimizationTime >= 10  && queryOptimizationTime <= 100)  st.compileCount_pureLang_withTime_gt10_lt100++;
				if (queryOptimizationTime >= 100 && queryOptimizationTime <= 1000) st.compileCount_pureLang_withTime_gt100_lt1000++;
				if (queryOptimizationTime >= 1000)                                 st.compileCount_pureLang_withTime_gt1000++;

				// DO NOT Exit early... Pure Language contains both COMPILE and runtime/execution counters
				exitEarly = false;
			}
		}

		// Exit early if this entry should be skipped
		if ( exitEarly )
		{
			return;
		}

		st.count++;

		st.sumExecTimeMs            += execTimeMs;
		st.sumLogicalReads          += logicalReads;
		st.sumPhysicalReads         += physicalReads;
		st.sumCpuTime               += cpuTime;
		st.sumWaitTime              += waitTime;
		st.sumRowsAffected          += rowsAffected;
		st.sumQueryOptimizationTime += queryOptimizationTime;

		st.maxExecTimeMs            = Math.max(st.maxExecTimeMs,    execTimeMs);
		st.maxLogicalReads          = Math.max(st.maxLogicalReads,  logicalReads);
		st.maxPhysicalReads         = Math.max(st.maxPhysicalReads, physicalReads);
		st.maxCpuTime               = Math.max(st.maxCpuTime,       cpuTime);
		st.maxWaitTime              = Math.max(st.maxWaitTime,      waitTime);
		st.maxRowsAffected          = Math.max(st.maxRowsAffected,  rowsAffected);
		st.maxQueryOptimizationTime = Math.max(st.maxQueryOptimizationTime, queryOptimizationTime);

//		if (logicalReads          > 0) st.cntLogicalReads++;
//		if (physicalReads         > 0) st.cntPhysicalReads++;
//		if (cpuTime               > 0) st.cntCpuTime++;
//		if (waitTime              > 0) st.cntWaitTime++;
//		if (rowsAffected          > 0) st.cntRowsAffected++;
		if (queryOptimizationTime > 0) st.cntQueryOptimizationTimeGtZero++;
		
//		if (procName == null)
		if (procedureId == 0)
		{
			st.sqlBatchCount++;
		}
		else
		{
			if (procName == null)
			{
				st.inProcNullCount++;
			}
			else
			{
				if (procName.startsWith("*ss"))
				{
					st.inStmntCacheCount++;
				}
				else if (procName.startsWith("*sq"))
				{
					st.dynamicStmntCount++;
				}
				else if (procName.startsWith("*##")) // if it's a StatementCahe(we had SsqlId & HashKey) entry (but object_name() returned NULL), see: com.dbxtune.pcs.sqlcapture.SqlCaptureBrokerAse ... ProcName = "..."
				{
					st.inStmntCacheCount++;
				}
				else
				{
					st.inProcCount++;
				}
			}
		}

//		// Compile Count and Time (and in different areas: PureLanguage, LanguageRequestInStatementCache, DynamicRequestInStatementCache
//		if (queryOptimizationTime > 0)
//		{
//			st.sumCompileTime += queryOptimizationTime;
//			st.maxCompileTime = Math.max(st.maxCompileTime, queryOptimizationTime);
//
//			if (queryOptimizationTime > 0)                                     st.compileCount++;
//			if (queryOptimizationTime >= 10  && queryOptimizationTime <= 100)  st.compileCount_withTime_gt10_lt100++;
//			if (queryOptimizationTime >= 100 && queryOptimizationTime <= 1000) st.compileCount_withTime_gt100_lt1000++;
//			if (queryOptimizationTime >= 1000)                                 st.compileCount_withTime_gt1000++;
//
//			// Pure Language Request
//			if (procedureId == 0)
//			{
//				st.sumCompileTime_pureLang += queryOptimizationTime;
//				st.maxCompileTime_pureLang = Math.max(st.maxCompileTime_pureLang, queryOptimizationTime);
//
//				if (queryOptimizationTime > 0)                                     st.compileCount_pureLang++;
//				if (queryOptimizationTime >= 10  && queryOptimizationTime <= 100)  st.compileCount_pureLang_withTime_gt10_lt100++;
//				if (queryOptimizationTime >= 100 && queryOptimizationTime <= 1000) st.compileCount_pureLang_withTime_gt100_lt1000++;
//				if (queryOptimizationTime >= 1000)                                 st.compileCount_pureLang_withTime_gt1000++;
//			}
//
//			// Language Request in Statement Cache
//			// Dynamic Request (Prepared Statement) in Statement Cache
//			if (ssqlId > 0 && procName != null)
//			{
//				// Language Request in Statement Cache
//				if (procName.startsWith("*ss"))
//				{
//					st.sumCompileTime_langInStmntCache += queryOptimizationTime;
//					st.maxCompileTime_langInStmntCache = Math.max(st.maxCompileTime_langInStmntCache, queryOptimizationTime);
//
//					if (queryOptimizationTime > 0)                                     st.compileCount_langInStmntCache++;
//					if (queryOptimizationTime >= 10  && queryOptimizationTime <= 100)  st.compileCount_langInStmntCache_withTime_gt10_lt100++;
//					if (queryOptimizationTime >= 100 && queryOptimizationTime <= 1000) st.compileCount_langInStmntCache_withTime_gt100_lt1000++;
//					if (queryOptimizationTime >= 1000)                                 st.compileCount_langInStmntCache_withTime_gt1000++;
//				}
//
//				// Dynamic Request (Prepared Statement) in Statement Cache
//				if (procName.startsWith("*sq"))
//				{
//					st.sumCompileTime_dynamicInStmntCache += queryOptimizationTime;
//					st.maxCompileTime_dynamicInStmntCache = Math.max(st.maxCompileTime_dynamicInStmntCache, queryOptimizationTime);
//
//					if (queryOptimizationTime > 0)                                     st.compileCount_dynamicInStmntCache++;
//					if (queryOptimizationTime >= 10  && queryOptimizationTime <= 100)  st.compileCount_dynamicInStmntCache_withTime_gt10_lt100++;
//					if (queryOptimizationTime >= 100 && queryOptimizationTime <= 1000) st.compileCount_dynamicInStmntCache_withTime_gt100_lt1000++;
//					if (queryOptimizationTime >= 1000)                                 st.compileCount_dynamicInStmntCache_withTime_gt1000++;
//				}
//			}
//		}

		if (errorStatus > 0)
		{
			st.errorCount++;
			
			// Add details in Map
			if (st.errorCountMap == null)
				st.errorCountMap = new HashMap<>();

			// Each error number will have it's own counter
			// Get it and increment the count, and set it back in the map
//			Long errorNumberCounter = st.errorCountMap.get(errorStatus);
//			if (errorNumberCounter == null)
//				errorNumberCounter = 0L;
//			errorNumberCounter++;
//			st.errorCountMap.put(errorStatus, errorNumberCounter);
			
			// Each error number will have it's own Map with: DBName, counter
			// Get it and increment the count, and set it back in the map
			Map<String, Long> dbMapCounter = st.errorCountMap.get(errorStatus);
			if (dbMapCounter == null)
			{
				dbMapCounter = new HashMap<>();
				dbMapCounter.put(dbname, 0L);

				// st.errorCountMap.put(errorStatus, dbMapCounter); // This is done AT THE END
			}
			Long errorNumberCounter = dbMapCounter.getOrDefault(dbname, 0L);
			
			errorNumberCounter++;
			dbMapCounter.put(dbname, errorNumberCounter);
			
			st.errorCountMap.put(errorStatus, dbMapCounter);
		}

//		if (procedureId > 0)
//		{
//			if (ssqlId > 0)
//				st.inStmntCacheCount++;
//			else
//				st.inProcCount++;
//		}
	}

	public void addExecTime(int spid, int batchId, int ms, int logicalReads, int physicalReads, int cpuTime, int waitTime, int rowsAffected, int errorStatus, int ssqlId, int procedureId, String procName, int contextId, int lineNumber, String dbname, int dbid, int queryOptimizationTime)
	{
		if      (ms == 0 && logicalReads == 0) addExecTimeInternal(StatType.EXEC_SPAN, EXEC_SPAN_0ms_0lr_0pr   , spid, batchId, ms, logicalReads, physicalReads, cpuTime, waitTime, rowsAffected, errorStatus, ssqlId, procedureId, procName, contextId, lineNumber, dbname, dbid, queryOptimizationTime); 
		else if (ms >= 0     && ms <= 1)       addExecTimeInternal(StatType.EXEC_SPAN, EXEC_SPAN_0_to_1_ms     , spid, batchId, ms, logicalReads, physicalReads, cpuTime, waitTime, rowsAffected, errorStatus, ssqlId, procedureId, procName, contextId, lineNumber, dbname, dbid, queryOptimizationTime); 
		else if (ms >= 1     && ms <= 2)       addExecTimeInternal(StatType.EXEC_SPAN, EXEC_SPAN_1_to_2_ms     , spid, batchId, ms, logicalReads, physicalReads, cpuTime, waitTime, rowsAffected, errorStatus, ssqlId, procedureId, procName, contextId, lineNumber, dbname, dbid, queryOptimizationTime); 
		else if (ms >= 2     && ms <= 5)       addExecTimeInternal(StatType.EXEC_SPAN, EXEC_SPAN_2_to_5_ms     , spid, batchId, ms, logicalReads, physicalReads, cpuTime, waitTime, rowsAffected, errorStatus, ssqlId, procedureId, procName, contextId, lineNumber, dbname, dbid, queryOptimizationTime); 
		else if (ms >= 5     && ms <= 10)      addExecTimeInternal(StatType.EXEC_SPAN, EXEC_SPAN_5_to_10_ms    , spid, batchId, ms, logicalReads, physicalReads, cpuTime, waitTime, rowsAffected, errorStatus, ssqlId, procedureId, procName, contextId, lineNumber, dbname, dbid, queryOptimizationTime); 
		else if (ms >= 10    && ms <= 20)      addExecTimeInternal(StatType.EXEC_SPAN, EXEC_SPAN_10_to_20_ms   , spid, batchId, ms, logicalReads, physicalReads, cpuTime, waitTime, rowsAffected, errorStatus, ssqlId, procedureId, procName, contextId, lineNumber, dbname, dbid, queryOptimizationTime); 
		else if (ms >= 20    && ms <= 50)      addExecTimeInternal(StatType.EXEC_SPAN, EXEC_SPAN_20_to_50_ms   , spid, batchId, ms, logicalReads, physicalReads, cpuTime, waitTime, rowsAffected, errorStatus, ssqlId, procedureId, procName, contextId, lineNumber, dbname, dbid, queryOptimizationTime); 
		else if (ms >= 50    && ms <= 100)     addExecTimeInternal(StatType.EXEC_SPAN, EXEC_SPAN_50_to_100_ms  , spid, batchId, ms, logicalReads, physicalReads, cpuTime, waitTime, rowsAffected, errorStatus, ssqlId, procedureId, procName, contextId, lineNumber, dbname, dbid, queryOptimizationTime); 
		else if (ms >= 100   && ms <= 200)     addExecTimeInternal(StatType.EXEC_SPAN, EXEC_SPAN_100_to_200_ms , spid, batchId, ms, logicalReads, physicalReads, cpuTime, waitTime, rowsAffected, errorStatus, ssqlId, procedureId, procName, contextId, lineNumber, dbname, dbid, queryOptimizationTime); 
		else if (ms >= 200   && ms <= 500)     addExecTimeInternal(StatType.EXEC_SPAN, EXEC_SPAN_200_to_500_ms , spid, batchId, ms, logicalReads, physicalReads, cpuTime, waitTime, rowsAffected, errorStatus, ssqlId, procedureId, procName, contextId, lineNumber, dbname, dbid, queryOptimizationTime); 
		else if (ms >= 500   && ms <= 1000)    addExecTimeInternal(StatType.EXEC_SPAN, EXEC_SPAN_500_to_1000_ms, spid, batchId, ms, logicalReads, physicalReads, cpuTime, waitTime, rowsAffected, errorStatus, ssqlId, procedureId, procName, contextId, lineNumber, dbname, dbid, queryOptimizationTime); 
		else if (ms >= 1000  && ms <= 2000)    addExecTimeInternal(StatType.EXEC_SPAN, EXEC_SPAN_1_to_2_sec    , spid, batchId, ms, logicalReads, physicalReads, cpuTime, waitTime, rowsAffected, errorStatus, ssqlId, procedureId, procName, contextId, lineNumber, dbname, dbid, queryOptimizationTime); 
		else if (ms >= 2000  && ms <= 5000)    addExecTimeInternal(StatType.EXEC_SPAN, EXEC_SPAN_2_to_5_sec    , spid, batchId, ms, logicalReads, physicalReads, cpuTime, waitTime, rowsAffected, errorStatus, ssqlId, procedureId, procName, contextId, lineNumber, dbname, dbid, queryOptimizationTime); 
		else if (ms >= 5000  && ms <= 10000)   addExecTimeInternal(StatType.EXEC_SPAN, EXEC_SPAN_5_to_10_sec   , spid, batchId, ms, logicalReads, physicalReads, cpuTime, waitTime, rowsAffected, errorStatus, ssqlId, procedureId, procName, contextId, lineNumber, dbname, dbid, queryOptimizationTime); 
		else if (ms >= 10000 && ms <= 20000)   addExecTimeInternal(StatType.EXEC_SPAN, EXEC_SPAN_10_to_20_sec  , spid, batchId, ms, logicalReads, physicalReads, cpuTime, waitTime, rowsAffected, errorStatus, ssqlId, procedureId, procName, contextId, lineNumber, dbname, dbid, queryOptimizationTime); 
		else if (ms >= 20000 && ms <= 50000)   addExecTimeInternal(StatType.EXEC_SPAN, EXEC_SPAN_20_to_50_sec  , spid, batchId, ms, logicalReads, physicalReads, cpuTime, waitTime, rowsAffected, errorStatus, ssqlId, procedureId, procName, contextId, lineNumber, dbname, dbid, queryOptimizationTime); 
		else if (ms >= 50000 && ms <= 100000)  addExecTimeInternal(StatType.EXEC_SPAN, EXEC_SPAN_50_to_100_sec , spid, batchId, ms, logicalReads, physicalReads, cpuTime, waitTime, rowsAffected, errorStatus, ssqlId, procedureId, procName, contextId, lineNumber, dbname, dbid, queryOptimizationTime); 
		else if (ms >= 100000)                 addExecTimeInternal(StatType.EXEC_SPAN, EXEC_SPAN_ABOVE_100_sec , spid, batchId, ms, logicalReads, physicalReads, cpuTime, waitTime, rowsAffected, errorStatus, ssqlId, procedureId, procName, contextId, lineNumber, dbname, dbid, queryOptimizationTime); 
		else
			System.out.println("addExecTime(ms="+ms+", logicalReads="+logicalReads+", physicalReads="+physicalReads);

		// Add SUMMARY
		//addExecTimeInternal(EXEC_SPAN_SUMMARY , ms, logicalReads, physicalReads, cpuTime, waitTime, rowsAffected, errorStatus, procedureId, procName, lineNumber, dbname);

	
		// Add to DBname map
		if (StringUtil.isNullOrBlank(dbname))
			dbname = "-no-db-context-";

		addExecTimeInternal(StatType.DBNAME, dbname, spid, batchId, ms, logicalReads, physicalReads, cpuTime, waitTime, rowsAffected, errorStatus, ssqlId, procedureId, procName, contextId, lineNumber, dbname, dbid, queryOptimizationTime); 
	}


//	public void addLogicalReads(int lr)
//	{
//	}
//
//	public void addPhysicalReads(int pr)
//	{
//	}

	private static final int DISCARD_OUT_OF_BOUNDS__MAX_PHYSICAL_READS = Integer.MAX_VALUE - 10;
	
	public void addStatementStats(int spid, int batchId, int elapsedTime, int logicalReads, int physicalReads, int cpuTime, int waitTime, int rowsAffected, int errorStatus, int ssqlId, int procedureId, String procName, int contextId, int lineNumber, String dbname, int dbid, int queryOptimizationTime)
	{
		if (physicalReads >= DISCARD_OUT_OF_BOUNDS__MAX_PHYSICAL_READS)
		{
			// should we LOG anything about this... 
			physicalReads = 0;
		}

		addExecTime(spid, batchId, elapsedTime, logicalReads, physicalReads, cpuTime, waitTime, rowsAffected, errorStatus, ssqlId, procedureId, procName, contextId, lineNumber, dbname, dbid, queryOptimizationTime);
//		addLogicalReads(logicalReads);
//		addPhysicalReads(physicalReads);
	}
	
	
	public ResultSet toResultSet()
	{
		SimpleResultSet rs = new SimpleResultSet();
		rs.addColumn("name",                        Types.VARCHAR, 30, 0);

		rs.addColumn("totalCount",                  Types.BIGINT,   0, 0);
		rs.addColumn("sqlBatchCount",               Types.BIGINT,   0, 0);
		rs.addColumn("errorCount",                  Types.BIGINT,   0, 0);
		rs.addColumn("inStmntCacheCount",           Types.BIGINT,   0, 0);
		rs.addColumn("dynamicStmntCount",           Types.BIGINT,   0, 0);
		rs.addColumn("inProcedureCount",            Types.BIGINT,   0, 0);
		rs.addColumn("inProcNameNullCount",         Types.BIGINT,   0, 0);

		rs.addColumn("totalCountAbs",               Types.BIGINT,   0, 0);
		rs.addColumn("sqlBatchCountAbs",            Types.BIGINT,   0, 0);
		rs.addColumn("inStmntCacheCountAbs",        Types.BIGINT,   0, 0);
		rs.addColumn("dynamicStmntCountAbs",        Types.BIGINT,   0, 0);
		rs.addColumn("inProcedureCountAbs",         Types.BIGINT,   0, 0);
		rs.addColumn("inProcNameNullCountAbs",      Types.BIGINT,   0, 0);

		rs.addColumn("sumExecTimeMs",               Types.BIGINT,   0, 0);
		rs.addColumn("sumExecTimeMsAbs",            Types.BIGINT,   0, 0);
		rs.addColumn("avgExecTimeMs",               Types.INTEGER,  0, 0);
		rs.addColumn("maxExecTimeMs",               Types.INTEGER,  0, 0);

		rs.addColumn("sumLogicalReads",             Types.BIGINT,   0, 0);
		rs.addColumn("sumLogicalReadsAbs",          Types.BIGINT,   0, 0);
		rs.addColumn("avgLogicalReads",             Types.INTEGER,  0, 0);
		rs.addColumn("maxLogicalReads",             Types.INTEGER,  0, 0);

		rs.addColumn("sumPhysicalReads",            Types.BIGINT,   0, 0);
		rs.addColumn("sumPhysicalReadsAbs",         Types.BIGINT,   0, 0);
		rs.addColumn("avgPhysicalReads",            Types.INTEGER,  0, 0);
		rs.addColumn("maxPhysicalReads",            Types.INTEGER,  0, 0);

		rs.addColumn("sumCpuTime",                  Types.BIGINT,   0, 0);
		rs.addColumn("sumCpuTimeAbs",               Types.BIGINT,   0, 0);
		rs.addColumn("avgCpuTime",                  Types.INTEGER,  0, 0);
		rs.addColumn("maxCpuTime",                  Types.INTEGER,  0, 0);

		rs.addColumn("sumWaitTime",                 Types.BIGINT,   0, 0);
		rs.addColumn("sumWaitTimeAbs",              Types.BIGINT,   0, 0);
		rs.addColumn("avgWaitTime",                 Types.INTEGER,  0, 0);
		rs.addColumn("maxWaitTime",                 Types.INTEGER,  0, 0);

		rs.addColumn("sumRowsAffected",             Types.BIGINT,   0, 0);
		rs.addColumn("sumRowsAffectedAbs",          Types.BIGINT,   0, 0);
		rs.addColumn("avgRowsAffected",             Types.INTEGER,  0, 0);
		rs.addColumn("maxRowsAffected",             Types.INTEGER,  0, 0);

		rs.addColumn("cntQueryOptimizationTimeGtZero",    Types.BIGINT,   0, 0);
		rs.addColumn("cntQueryOptimizationTimeGtZeroAbs", Types.BIGINT,   0, 0);
		rs.addColumn("sumQueryOptimizationTime",          Types.BIGINT,   0, 0);
		rs.addColumn("sumQueryOptimizationTimeAbs",       Types.BIGINT,   0, 0);
		rs.addColumn("avgQueryOptimizationTime",          Types.INTEGER,  0, 0);
		rs.addColumn("maxQueryOptimizationTime",          Types.INTEGER,  0, 0);
                                               
		rs.addColumn("errorMsgCountMap",            Types.VARCHAR, 1024, 0);

		rs.addColumn("statId",                      Types.INTEGER,  0, 0);

		Gson gson = new Gson(); 

		for (StatCounter sc : _execTimeMap.values())
		{
			rs.addRow(
				sc.name, 

				sc.count,                                                                     // totalCount
//				sc.count - (sc.inStmntCacheCount + sc.dynamicStmntCount + sc.inProcCount),    // inSqlBatchCount
				sc.sqlBatchCount,                                                             // sqlBatchCount
				sc.errorCount,                                                                // errorCount
				sc.inStmntCacheCount,                                                         // inStmntCacheCount
				sc.dynamicStmntCount,                                                         // dynamicStmntCount
				sc.inProcCount,                                                               // inProcedureCount
				sc.inProcNullCount,                                                           // inProcNameNullCount

				sc.count,                                                                     // totalCount        - ABS
//				sc.count - (sc.inStmntCacheCount + sc.dynamicStmntCount + sc.inProcCount),    // inSqlBatchCount   - ABS
				sc.sqlBatchCount,                                                             // sqlBatchCount     - ABS
				sc.inStmntCacheCount,                                                         // inStmntCacheCount - ABS
				sc.dynamicStmntCount,                                                         // dynamicStmntCount - ABS
				sc.inProcCount,                                                               // inProcedureCount  - ABS
				sc.inProcNullCount,                                                           // inProcNameNullCount - ABS

				sc.sumExecTimeMs, 
				sc.sumExecTimeMs,                                      // ABS
				sc.count == 0 ? 0 : sc.sumExecTimeMs / sc.count,       // AVG
				sc.maxExecTimeMs, 

				sc.sumLogicalReads, 
				sc.sumLogicalReads,                                    // ABS
				sc.count == 0 ? 0 : sc.sumLogicalReads / sc.count,     // AVG
				sc.maxLogicalReads, 

				sc.sumPhysicalReads,
				sc.sumPhysicalReads,                                   // ABS
				sc.count == 0 ? 0 : sc.sumPhysicalReads / sc.count,    // AVG
				sc.maxPhysicalReads,

				sc.sumCpuTime,
				sc.sumCpuTime,                                         // ABS
				sc.count == 0 ? 0 : sc.sumCpuTime / sc.count,          // AVG
				sc.maxCpuTime,

				sc.sumWaitTime,
				sc.sumWaitTime,                                        // ABS
				sc.count == 0 ? 0 : sc.sumWaitTime / sc.count,         // AVG
				sc.maxWaitTime,
				
				sc.sumRowsAffected,
				sc.sumRowsAffected,                                    // ABS
				sc.count == 0 ? 0 : sc.sumRowsAffected / sc.count,     // AVG
				sc.maxRowsAffected,

				sc.cntQueryOptimizationTimeGtZero,     // CNT -- How many times have we done 'QueryOptimizationTime' > 0 
				sc.cntQueryOptimizationTimeGtZero,     // ABS 
				sc.sumQueryOptimizationTime,
				sc.sumQueryOptimizationTime,                                    // ABS
				sc.cntQueryOptimizationTimeGtZero == 0 ? 0 : sc.sumQueryOptimizationTime / sc.cntQueryOptimizationTimeGtZero,     // AVG
				sc.maxQueryOptimizationTime,

				sc.errorCountMap == null ? null : gson.toJson(sc.errorCountMap),

				sc.statId
			);
		}

		return rs;
	}



	public ResultSet toResultSetDbName()
	{
		SimpleResultSet rs = new SimpleResultSet();
		rs.addColumn("dbname",                      Types.VARCHAR, 30, 0);

		rs.addColumn("totalCount",                  Types.BIGINT,   0, 0);
		rs.addColumn("sqlBatchCount",               Types.BIGINT,   0, 0);
		rs.addColumn("errorCount",                  Types.BIGINT,   0, 0);
		rs.addColumn("inStmntCacheCount",           Types.BIGINT,   0, 0);
		rs.addColumn("dynamicStmntCount",           Types.BIGINT,   0, 0);
		rs.addColumn("inProcedureCount",            Types.BIGINT,   0, 0);
		rs.addColumn("inProcNameNullCount",         Types.BIGINT,   0, 0);

		rs.addColumn("totalCountAbs",               Types.BIGINT,   0, 0);
		rs.addColumn("sqlBatchCountAbs",            Types.BIGINT,   0, 0);
		rs.addColumn("inStmntCacheCountAbs",        Types.BIGINT,   0, 0);
		rs.addColumn("dynamicStmntCountAbs",        Types.BIGINT,   0, 0);
		rs.addColumn("inProcedureCountAbs",         Types.BIGINT,   0, 0);
		rs.addColumn("inProcNameNullCountAbs",      Types.BIGINT,   0, 0);

		rs.addColumn("sumExecTimeMs",               Types.BIGINT,   0, 0);
		rs.addColumn("sumExecTimeMsAbs",            Types.BIGINT,   0, 0);
		rs.addColumn("avgExecTimeMs",               Types.INTEGER,  0, 0);
		rs.addColumn("maxExecTimeMs",               Types.INTEGER,  0, 0);

		rs.addColumn("sumLogicalReads",             Types.BIGINT,   0, 0);
		rs.addColumn("sumLogicalReadsAbs",          Types.BIGINT,   0, 0);
		rs.addColumn("avgLogicalReads",             Types.INTEGER,  0, 0);
		rs.addColumn("maxLogicalReads",             Types.INTEGER,  0, 0);

		rs.addColumn("sumPhysicalReads",            Types.BIGINT,   0, 0);
		rs.addColumn("sumPhysicalReadsAbs",         Types.BIGINT,   0, 0);
		rs.addColumn("avgPhysicalReads",            Types.INTEGER,  0, 0);
		rs.addColumn("maxPhysicalReads",            Types.INTEGER,  0, 0);

		rs.addColumn("sumCpuTime",                  Types.BIGINT,   0, 0);
		rs.addColumn("sumCpuTimeAbs",               Types.BIGINT,   0, 0);
		rs.addColumn("avgCpuTime",                  Types.INTEGER,  0, 0);
		rs.addColumn("maxCpuTime",                  Types.INTEGER,  0, 0);

		rs.addColumn("sumWaitTime",                 Types.BIGINT,   0, 0);
		rs.addColumn("sumWaitTimeAbs",              Types.BIGINT,   0, 0);
		rs.addColumn("avgWaitTime",                 Types.INTEGER,  0, 0);
		rs.addColumn("maxWaitTime",                 Types.INTEGER,  0, 0);

		rs.addColumn("sumRowsAffected",             Types.BIGINT,   0, 0);
		rs.addColumn("sumRowsAffectedAbs",          Types.BIGINT,   0, 0);
		rs.addColumn("avgRowsAffected",             Types.INTEGER,  0, 0);
		rs.addColumn("maxRowsAffected",             Types.INTEGER,  0, 0);

		rs.addColumn("cntQueryOptimizationTimeGtZero",    Types.BIGINT,   0, 0);
		rs.addColumn("cntQueryOptimizationTimeGtZeroAbs", Types.BIGINT,   0, 0);
		rs.addColumn("sumQueryOptimizationTime",          Types.BIGINT,   0, 0);
		rs.addColumn("sumQueryOptimizationTimeAbs",       Types.BIGINT,   0, 0);
		rs.addColumn("avgQueryOptimizationTime",          Types.INTEGER,  0, 0);
		rs.addColumn("maxQueryOptimizationTime",          Types.INTEGER,  0, 0);

//TODO; // Should we clean this up ???
//TODO; // Should we also get rid of above '{cnt|sum|avg|max}QueryOptimizationTime*'
        rs.addColumn("compileCount",                                           Types.BIGINT,   0, 0);
        rs.addColumn("compileCountAbs",                                        Types.BIGINT,   0, 0);
        rs.addColumn("compileCount_withTime_gt10_lt100",                       Types.BIGINT,   0, 0);
        rs.addColumn("compileCount_withTime_gt100_lt1000",                     Types.BIGINT,   0, 0);
        rs.addColumn("compileCount_withTime_gt1000",                           Types.BIGINT,   0, 0);
        rs.addColumn("sumCompileTime",                                         Types.BIGINT,   0, 0);
        rs.addColumn("sumCompileTimeAbs",                                      Types.BIGINT,   0, 0);
        rs.addColumn("avgCompileTime",                                         Types.INTEGER,  0, 0);
        rs.addColumn("maxCompileTime",                                         Types.INTEGER,  0, 0);
        
        rs.addColumn("compileCount_pureLang",                                  Types.BIGINT,   0, 0);
        rs.addColumn("compileCount_pureLangAbs",                               Types.BIGINT,   0, 0);
        rs.addColumn("compileCount_pureLang_withTime_gt10_lt100",              Types.BIGINT,   0, 0);
        rs.addColumn("compileCount_pureLang_withTime_gt100_lt1000",            Types.BIGINT,   0, 0);
        rs.addColumn("compileCount_pureLang_withTime_gt1000",                  Types.BIGINT,   0, 0);
        rs.addColumn("sumCompileTime_pureLang",                                Types.BIGINT,   0, 0);
        rs.addColumn("sumCompileTime_pureLangAbs",                             Types.BIGINT,   0, 0);
        rs.addColumn("avgCompileTime_pureLang",                                Types.INTEGER,  0, 0);
        rs.addColumn("maxCompileTime_pureLang",                                Types.INTEGER,  0, 0);
        
        rs.addColumn("compileCount_langInStmntCache",                          Types.BIGINT,   0, 0);
        rs.addColumn("compileCount_langInStmntCacheAbs",                       Types.BIGINT,   0, 0);
        rs.addColumn("compileCount_langInStmntCache_withTime_gt10_lt100",      Types.BIGINT,   0, 0);
        rs.addColumn("compileCount_langInStmntCache_withTime_gt100_lt1000",    Types.BIGINT,   0, 0);
        rs.addColumn("compileCount_langInStmntCache_withTime_gt1000",          Types.BIGINT,   0, 0);
        rs.addColumn("sumCompileTime_langInStmntCache",                        Types.BIGINT,   0, 0);
        rs.addColumn("sumCompileTime_langInStmntCacheAbs",                     Types.BIGINT,   0, 0);
        rs.addColumn("avgCompileTime_langInStmntCache",                        Types.INTEGER,  0, 0);
        rs.addColumn("maxCompileTime_langInStmntCache",                        Types.INTEGER,  0, 0);
        
        rs.addColumn("compileCount_dynamicInStmntCache",                       Types.BIGINT,   0, 0);
        rs.addColumn("compileCount_dynamicInStmntCacheAbs",                    Types.BIGINT,   0, 0);
        rs.addColumn("compileCount_dynamicInStmntCache_withTime_gt10_lt100",   Types.BIGINT,   0, 0);
        rs.addColumn("compileCount_dynamicInStmntCache_withTime_gt100_lt1000", Types.BIGINT,   0, 0);
        rs.addColumn("compileCount_dynamicInStmntCache_withTime_gt1000",       Types.BIGINT,   0, 0);
        rs.addColumn("sumCompileTime_dynamicInStmntCache",                     Types.BIGINT,   0, 0);
        rs.addColumn("sumCompileTime_dynamicInStmntCacheAbs",                  Types.BIGINT,   0, 0);
        rs.addColumn("avgCompileTime_dynamicInStmntCache",                     Types.INTEGER,  0, 0);
        rs.addColumn("maxCompileTime_dynamicInStmntCache",                     Types.INTEGER,  0, 0);

		rs.addColumn("errorMsgCountMap",            Types.VARCHAR, 1024, 0);

		rs.addColumn("dbid",                        Types.INTEGER,  0, 0);

		Gson gson = new Gson(); 

		for (StatCounter sc : _dbnameMap.values())
		{
			rs.addRow(
				sc.name, 

				sc.count,                                                                     // totalCount
//				sc.count - (sc.inStmntCacheCount + sc.dynamicStmntCount + sc.inProcCount),    // inSqlBatchCount
				sc.sqlBatchCount,                                                             // sqlBatchCount
				sc.errorCount,                                                                // errorCount
				sc.inStmntCacheCount,                                                         // inStmntCacheCount
				sc.dynamicStmntCount,                                                         // dynamicStmntCount
				sc.inProcCount,                                                               // inProcedureCount
				sc.inProcNullCount,                                                           // inProcNameNullCount

				sc.count,                                                                     // totalCount        - ABS
//				sc.count - (sc.inStmntCacheCount + sc.dynamicStmntCount + sc.inProcCount),    // inSqlBatchCount   - ABS
				sc.sqlBatchCount,                                                             // sqlBatchCount     - ABS
				sc.inStmntCacheCount,                                                         // inStmntCacheCount - ABS
				sc.dynamicStmntCount,                                                         // dynamicStmntCount - ABS
				sc.inProcCount,                                                               // inProcedureCount  - ABS
				sc.inProcNullCount,                                                           // inProcNameNullCount - ABS

				sc.sumExecTimeMs, 
				sc.sumExecTimeMs,                                      // ABS
				sc.count == 0 ? 0 : sc.sumExecTimeMs / sc.count,       // AVG
				sc.maxExecTimeMs, 

				sc.sumLogicalReads, 
				sc.sumLogicalReads,                                    // ABS
				sc.count == 0 ? 0 : sc.sumLogicalReads / sc.count,     // AVG
				sc.maxLogicalReads, 

				sc.sumPhysicalReads,
				sc.sumPhysicalReads,                                   // ABS
				sc.count == 0 ? 0 : sc.sumPhysicalReads / sc.count,    // AVG
				sc.maxPhysicalReads,

				sc.sumCpuTime,
				sc.sumCpuTime,                                         // ABS
				sc.count == 0 ? 0 : sc.sumCpuTime / sc.count,          // AVG
				sc.maxCpuTime,

				sc.sumWaitTime,
				sc.sumWaitTime,                                        // ABS
				sc.count == 0 ? 0 : sc.sumWaitTime / sc.count,         // AVG
				sc.maxWaitTime,
				
				sc.sumRowsAffected,
				sc.sumRowsAffected,                                    // ABS
				sc.count == 0 ? 0 : sc.sumRowsAffected / sc.count,     // AVG
				sc.maxRowsAffected,

				sc.cntQueryOptimizationTimeGtZero,     // CNT -- How many times have we done 'QueryOptimizationTime' > 0 
				sc.cntQueryOptimizationTimeGtZero,     // ABS 
				sc.sumQueryOptimizationTime,
				sc.sumQueryOptimizationTime,                                    // ABS
				sc.cntQueryOptimizationTimeGtZero == 0 ? 0 : sc.sumQueryOptimizationTime / sc.cntQueryOptimizationTimeGtZero,     // AVG
				sc.maxQueryOptimizationTime,

//---------------------------------------------
				sc.compileCount,
				sc.compileCount, // ABS
				sc.compileCount_withTime_gt10_lt100,
				sc.compileCount_withTime_gt100_lt1000,
				sc.compileCount_withTime_gt1000,
				sc.sumCompileTime, // value to be diff calc
				sc.sumCompileTime, // ABS
				sc.sumCompileTime == 0 ? 0 : sc.sumCompileTime / sc.compileCount, // AVG
				sc.maxCompileTime, // MAX

				sc.compileCount_pureLang,
				sc.compileCount_pureLang, // ABS
				sc.compileCount_pureLang_withTime_gt10_lt100,
				sc.compileCount_pureLang_withTime_gt100_lt1000,
				sc.compileCount_pureLang_withTime_gt1000,
				sc.sumCompileTime_pureLang, // value to be diff calc
				sc.sumCompileTime_pureLang, // ABS
				sc.sumCompileTime_pureLang == 0 ? 0 : sc.sumCompileTime_pureLang / sc.compileCount_pureLang, // AVG
				sc.maxCompileTime_pureLang, // MAX

				sc.compileCount_langInStmntCache,
				sc.compileCount_langInStmntCache, // ABS
				sc.compileCount_langInStmntCache_withTime_gt10_lt100,
				sc.compileCount_langInStmntCache_withTime_gt100_lt1000,
				sc.compileCount_langInStmntCache_withTime_gt1000,
				sc.sumCompileTime_langInStmntCache, // value to be diff calc
				sc.sumCompileTime_langInStmntCache, // ABS
				sc.sumCompileTime_langInStmntCache == 0 ? 0 : sc.sumCompileTime_langInStmntCache / sc.compileCount_langInStmntCache, // AVG
				sc.maxCompileTime_langInStmntCache, // MAX

				sc.compileCount_dynamicInStmntCache,
				sc.compileCount_dynamicInStmntCache, // ABS
				sc.compileCount_dynamicInStmntCache_withTime_gt10_lt100,
				sc.compileCount_dynamicInStmntCache_withTime_gt100_lt1000,
				sc.compileCount_dynamicInStmntCache_withTime_gt1000,
				sc.sumCompileTime_dynamicInStmntCache, // value to be diff calc
				sc.sumCompileTime_dynamicInStmntCache, // ABS
				sc.sumCompileTime_dynamicInStmntCache == 0 ? 0 : sc.sumCompileTime_dynamicInStmntCache / sc.compileCount_dynamicInStmntCache, // AVG
				sc.maxCompileTime_dynamicInStmntCache, // MAX
//---------------------------------------------
				
				sc.errorCountMap == null ? null : gson.toJson(sc.errorCountMap),

				sc.statId
			);
		}

		return rs;
	}
}
