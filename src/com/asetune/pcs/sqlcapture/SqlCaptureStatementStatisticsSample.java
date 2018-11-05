package com.asetune.pcs.sqlcapture;

import java.sql.ResultSet;
import java.sql.Types;
import java.util.LinkedHashMap;

import org.h2.tools.SimpleResultSet;

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
	
	private LinkedHashMap<String, StatCounter> _execTimeMap = new LinkedHashMap<>();
	
	public static class StatCounter
	{
		public StatCounter(String name)
		{
			this.name              = name;
			this.count             = 0;
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
		}
		
		public String name;
		public long   count;
		public long   sqlBatchCount;
		public long   inProcCount;
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
	}
	
	public SqlCaptureStatementStatisticsSample()
	{
		synchronized (this)
		{
			_execTimeMap = new LinkedHashMap<>();

			_execTimeMap.put(EXEC_SPAN_0ms_0lr_0pr   , new StatCounter(EXEC_SPAN_0ms_0lr_0pr   ));
			_execTimeMap.put(EXEC_SPAN_0_to_1_ms     , new StatCounter(EXEC_SPAN_0_to_1_ms     ));
			_execTimeMap.put(EXEC_SPAN_1_to_2_ms     , new StatCounter(EXEC_SPAN_1_to_2_ms     ));
			_execTimeMap.put(EXEC_SPAN_2_to_5_ms     , new StatCounter(EXEC_SPAN_2_to_5_ms     ));
			_execTimeMap.put(EXEC_SPAN_5_to_10_ms    , new StatCounter(EXEC_SPAN_5_to_10_ms    ));
			_execTimeMap.put(EXEC_SPAN_10_to_20_ms   , new StatCounter(EXEC_SPAN_10_to_20_ms   ));
			_execTimeMap.put(EXEC_SPAN_20_to_50_ms   , new StatCounter(EXEC_SPAN_20_to_50_ms   ));
			_execTimeMap.put(EXEC_SPAN_50_to_100_ms  , new StatCounter(EXEC_SPAN_50_to_100_ms  ));
			_execTimeMap.put(EXEC_SPAN_100_to_200_ms , new StatCounter(EXEC_SPAN_100_to_200_ms ));
			_execTimeMap.put(EXEC_SPAN_200_to_500_ms , new StatCounter(EXEC_SPAN_200_to_500_ms ));
			_execTimeMap.put(EXEC_SPAN_500_to_1000_ms, new StatCounter(EXEC_SPAN_500_to_1000_ms));
			_execTimeMap.put(EXEC_SPAN_1_to_2_sec    , new StatCounter(EXEC_SPAN_1_to_2_sec    ));
			_execTimeMap.put(EXEC_SPAN_2_to_5_sec    , new StatCounter(EXEC_SPAN_2_to_5_sec    ));
			_execTimeMap.put(EXEC_SPAN_5_to_10_sec   , new StatCounter(EXEC_SPAN_5_to_10_sec   ));
			_execTimeMap.put(EXEC_SPAN_10_to_20_sec  , new StatCounter(EXEC_SPAN_10_to_20_sec  ));
			_execTimeMap.put(EXEC_SPAN_20_to_50_sec  , new StatCounter(EXEC_SPAN_20_to_50_sec  ));
			_execTimeMap.put(EXEC_SPAN_50_to_100_sec , new StatCounter(EXEC_SPAN_50_to_100_sec ));
			_execTimeMap.put(EXEC_SPAN_ABOVE_100_sec , new StatCounter(EXEC_SPAN_ABOVE_100_sec ));
			
			// Summary will always be updated (but this record needs to be discarded by graphs that does SUM over all records in the table)
			// SKIP THIS FOR NOW
			//_execTimeMap.put(EXEC_SPAN_SUMMARY       , new StatCounter(EXEC_SPAN_SUMMARY       ));

		}
	}
	
	private void addExecTimeInternal(String key, int execTimeMs, int logicalReads, int physicalReads, int cpuTime, int waitTime, int rowsAffected, String procName, int lineNumber)
	{
		// if something seems to be from the statement cache, and the LineNumber is 0, then discard the entry
		// it looks like there are always 2 rows in monSysStatement when there are (*ss or *sq) objects
		// The row with lineNumber == 0, does not contain real values for: LogicalReads etc...
		if ( procName != null && lineNumber == 0 && (procName.startsWith("*ss") || procName.startsWith("*sq")) )
		{
			return;
		}

		StatCounter st = _execTimeMap.get(key);
		st.count++;

		st.sumExecTimeMs    += execTimeMs;
		st.sumLogicalReads  += logicalReads;
		st.sumPhysicalReads += physicalReads;
		st.sumCpuTime       += cpuTime;
		st.sumWaitTime      += waitTime;
		st.sumRowsAffected  += rowsAffected;

		st.maxExecTimeMs    = Math.max(st.maxExecTimeMs,    execTimeMs);
		st.maxLogicalReads  = Math.max(st.maxLogicalReads,  logicalReads);
		st.maxPhysicalReads = Math.max(st.maxPhysicalReads, physicalReads);
		st.maxCpuTime       = Math.max(st.maxCpuTime,       cpuTime);
		st.maxWaitTime      = Math.max(st.maxWaitTime,      waitTime);
		st.maxRowsAffected  = Math.max(st.maxRowsAffected,  rowsAffected);

//		if (logicalReads  > 0) st.cntLogicalReads++;
//		if (physicalReads > 0) st.cntPhysicalReads++;
//		if (cpuTime       > 0) st.cntCpuTime++;
//		if (waitTime      > 0) st.cntWaitTime++;
//		if (rowsAffected  > 0) st.cntRowsAffected++;
		
		if (procName == null)
		{
			st.sqlBatchCount++;
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
			else
			{
				st.inProcCount++;
			}
		}

//		if (procedureId > 0)
//		{
//			if (ssqlId > 0)
//				st.inStmntCacheCount++;
//			else
//				st.inProcCount++;
//		}
	}
	
	public void addExecTime(int ms, int logicalReads, int physicalReads, int cpuTime, int waitTime, int rowsAffected, String procName, int lineNumber)
	{
		if      (ms == 0 && logicalReads == 0) addExecTimeInternal(EXEC_SPAN_0ms_0lr_0pr   , ms, logicalReads, physicalReads, cpuTime, waitTime, rowsAffected, procName, lineNumber); 
		else if (ms >= 0     && ms <= 1)       addExecTimeInternal(EXEC_SPAN_0_to_1_ms     , ms, logicalReads, physicalReads, cpuTime, waitTime, rowsAffected, procName, lineNumber); 
		else if (ms >= 1     && ms <= 2)       addExecTimeInternal(EXEC_SPAN_1_to_2_ms     , ms, logicalReads, physicalReads, cpuTime, waitTime, rowsAffected, procName, lineNumber); 
		else if (ms >= 2     && ms <= 5)       addExecTimeInternal(EXEC_SPAN_2_to_5_ms     , ms, logicalReads, physicalReads, cpuTime, waitTime, rowsAffected, procName, lineNumber); 
		else if (ms >= 5     && ms <= 10)      addExecTimeInternal(EXEC_SPAN_5_to_10_ms    , ms, logicalReads, physicalReads, cpuTime, waitTime, rowsAffected, procName, lineNumber); 
		else if (ms >= 10    && ms <= 20)      addExecTimeInternal(EXEC_SPAN_10_to_20_ms   , ms, logicalReads, physicalReads, cpuTime, waitTime, rowsAffected, procName, lineNumber); 
		else if (ms >= 20    && ms <= 50)      addExecTimeInternal(EXEC_SPAN_20_to_50_ms   , ms, logicalReads, physicalReads, cpuTime, waitTime, rowsAffected, procName, lineNumber); 
		else if (ms >= 50    && ms <= 100)     addExecTimeInternal(EXEC_SPAN_50_to_100_ms  , ms, logicalReads, physicalReads, cpuTime, waitTime, rowsAffected, procName, lineNumber); 
		else if (ms >= 100   && ms <= 200)     addExecTimeInternal(EXEC_SPAN_100_to_200_ms , ms, logicalReads, physicalReads, cpuTime, waitTime, rowsAffected, procName, lineNumber); 
		else if (ms >= 200   && ms <= 500)     addExecTimeInternal(EXEC_SPAN_200_to_500_ms , ms, logicalReads, physicalReads, cpuTime, waitTime, rowsAffected, procName, lineNumber); 
		else if (ms >= 500   && ms <= 1000)    addExecTimeInternal(EXEC_SPAN_500_to_1000_ms, ms, logicalReads, physicalReads, cpuTime, waitTime, rowsAffected, procName, lineNumber); 
		else if (ms >= 1000  && ms <= 2000)    addExecTimeInternal(EXEC_SPAN_1_to_2_sec    , ms, logicalReads, physicalReads, cpuTime, waitTime, rowsAffected, procName, lineNumber); 
		else if (ms >= 2000  && ms <= 5000)    addExecTimeInternal(EXEC_SPAN_2_to_5_sec    , ms, logicalReads, physicalReads, cpuTime, waitTime, rowsAffected, procName, lineNumber); 
		else if (ms >= 5000  && ms <= 10000)   addExecTimeInternal(EXEC_SPAN_5_to_10_sec   , ms, logicalReads, physicalReads, cpuTime, waitTime, rowsAffected, procName, lineNumber); 
		else if (ms >= 10000 && ms <= 20000)   addExecTimeInternal(EXEC_SPAN_10_to_20_sec  , ms, logicalReads, physicalReads, cpuTime, waitTime, rowsAffected, procName, lineNumber); 
		else if (ms >= 20000 && ms <= 50000)   addExecTimeInternal(EXEC_SPAN_20_to_50_sec  , ms, logicalReads, physicalReads, cpuTime, waitTime, rowsAffected, procName, lineNumber); 
		else if (ms >= 50000 && ms <= 100000)  addExecTimeInternal(EXEC_SPAN_50_to_100_sec , ms, logicalReads, physicalReads, cpuTime, waitTime, rowsAffected, procName, lineNumber); 
		else if (ms >= 100000)                 addExecTimeInternal(EXEC_SPAN_ABOVE_100_sec , ms, logicalReads, physicalReads, cpuTime, waitTime, rowsAffected, procName, lineNumber); 
		else
			System.out.println("addExecTime(ms="+ms+", logicalReads="+logicalReads+", physicalReads="+physicalReads);

		// Add SUMMARY
		//addExecTimeInternal(EXEC_SPAN_SUMMARY , ms, logicalReads, physicalReads, cpuTime, waitTime, rowsAffected, procedureId, ssqlId);
	}


//	public void addLogicalReads(int lr)
//	{
//	}
//
//	public void addPhysicalReads(int pr)
//	{
//	}

	private static final int DISCARD_OUT_OF_BOUNDS__MAX_PHYSICAL_READS = Integer.MAX_VALUE - 10;
	
	public void addStatementStats(int execTimeMs, int logicalReads, int physicalReads, int cpuTime, int waitTime, int rowsAffected, String procName, int lineNumber)
	{
		if (physicalReads >= DISCARD_OUT_OF_BOUNDS__MAX_PHYSICAL_READS)
		{
			// should we LOG anything about this... 
			physicalReads = 0;
		}

		addExecTime(execTimeMs, logicalReads, physicalReads, cpuTime, waitTime, rowsAffected, procName, lineNumber);
//		addLogicalReads(logicalReads);
//		addPhysicalReads(physicalReads);
	}
	
	
	public ResultSet toResultSet()
	{
		SimpleResultSet rs = new SimpleResultSet();
		rs.addColumn("name",                 Types.VARCHAR, 30, 0);
		                                   
		rs.addColumn("totalCount",           Types.BIGINT,   0, 0);
		rs.addColumn("sqlBatchCount",        Types.BIGINT,   0, 0);
		rs.addColumn("inStmntCacheCount",    Types.BIGINT,   0, 0);
		rs.addColumn("dynamicStmntCount",    Types.BIGINT,   0, 0);
		rs.addColumn("inProcedureCount",     Types.BIGINT,   0, 0);

		rs.addColumn("totalCountAbs",        Types.BIGINT,   0, 0);
		rs.addColumn("sqlBatchCountAbs",     Types.BIGINT,   0, 0);
		rs.addColumn("inStmntCacheCountAbs", Types.BIGINT,   0, 0);
		rs.addColumn("dynamicStmntCountAbs", Types.BIGINT,   0, 0);
		rs.addColumn("inProcedureCountAbs",  Types.BIGINT,   0, 0);
		                                   
		rs.addColumn("sumExecTimeMs",        Types.BIGINT,   0, 0);
		rs.addColumn("sumExecTimeMsAbs",     Types.BIGINT,   0, 0);
		rs.addColumn("avgExecTimeMs",        Types.INTEGER,  0, 0);
		rs.addColumn("maxExecTimeMs",        Types.INTEGER,  0, 0);
                                             
		rs.addColumn("sumLogicalReads",      Types.BIGINT,   0, 0);
		rs.addColumn("sumLogicalReadsAbs",   Types.BIGINT,   0, 0);
		rs.addColumn("avgLogicalReads",      Types.INTEGER,  0, 0);
		rs.addColumn("maxLogicalReads",      Types.INTEGER,  0, 0);
                                             
		rs.addColumn("sumPhysicalReads",     Types.BIGINT,   0, 0);
		rs.addColumn("sumPhysicalReadsAbs",  Types.BIGINT,   0, 0);
		rs.addColumn("avgPhysicalReads",     Types.INTEGER,  0, 0);
		rs.addColumn("maxPhysicalReads",     Types.INTEGER,  0, 0);
                                             
		rs.addColumn("sumCpuTime",           Types.BIGINT,   0, 0);
		rs.addColumn("sumCpuTimeAbs",        Types.BIGINT,   0, 0);
		rs.addColumn("avgCpuTime",           Types.INTEGER,  0, 0);
		rs.addColumn("maxCpuTime",           Types.INTEGER,  0, 0);
                                             
		rs.addColumn("sumWaitTime",          Types.BIGINT,   0, 0);
		rs.addColumn("sumWaitTimeAbs",       Types.BIGINT,   0, 0);
		rs.addColumn("avgWaitTime",          Types.INTEGER,  0, 0);
		rs.addColumn("maxWaitTime",          Types.INTEGER,  0, 0);
                                             
		rs.addColumn("sumRowsAffected",      Types.BIGINT,   0, 0);
		rs.addColumn("sumRowsAffectedAbs",   Types.BIGINT,   0, 0);
		rs.addColumn("avgRowsAffected",      Types.INTEGER,  0, 0);
		rs.addColumn("maxRowsAffected",      Types.INTEGER,  0, 0);

		for (StatCounter sc : _execTimeMap.values())
		{
			rs.addRow(
				sc.name, 

				sc.count,                                                                     // totalCount
//				sc.count - (sc.inStmntCacheCount + sc.dynamicStmntCount + sc.inProcCount),    // inSqlBatchCount
				sc.sqlBatchCount,                                                             // sqlBatchCount
				sc.inStmntCacheCount,                                                         // inStmntCacheCount
				sc.dynamicStmntCount,                                                         // dynamicStmntCount
				sc.inProcCount,                                                               // inProcedureCount

				sc.count,                                                                     // totalCount        - ABS
//				sc.count - (sc.inStmntCacheCount + sc.dynamicStmntCount + sc.inProcCount),    // inSqlBatchCount   - ABS
				sc.sqlBatchCount,                                                             // sqlBatchCount     - ABS
				sc.inStmntCacheCount,                                                         // inStmntCacheCount - ABS
				sc.dynamicStmntCount,                                                         // dynamicStmntCount - ABS
				sc.inProcCount,                                                               // inProcedureCount  - ABS

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
				sc.maxRowsAffected
			);
		}

		return rs;
	}
}