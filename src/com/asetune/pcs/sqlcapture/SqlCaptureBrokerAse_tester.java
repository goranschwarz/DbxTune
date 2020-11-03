/*******************************************************************************
 * Copyright (C) 2010-2020 Goran Schwarz
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
package com.asetune.pcs.sqlcapture;

import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.text.ParseException;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;

import org.apache.log4j.PropertyConfigurator;

import com.asetune.gui.ResultSetTableModel;
import com.asetune.pcs.PersistentCounterHandler;
import com.asetune.sql.conn.DbxConnection;
import com.asetune.utils.Configuration;
import com.asetune.utils.StringUtil;
import com.asetune.utils.TimeUtils;

public class SqlCaptureBrokerAse_tester
extends SqlCaptureBrokerAse
{
	private SqlCaptureDetails _pcs;

//	/**
//	 * Test normal, add:
//	 * - 1 SQL Text
//	 * - 1 SQL Plan
//	 * - 1 SQL Statement
//	 * 
//	 * This should produce:
//	 * - 1 PCS entry, with 3 different records in the PCS (1=statement, 1=sqlText, 1=planText)
//	 * - 0 entries in the deferred queue
//	 */
//	public boolean test1(boolean debug)
//	{
//		String p = "\t";
//		System.out.println("test1-START");
//		
//		// set the xxx
//		super._deferredStorageThresholdFor_sqlTextAndPlan = 10;
//
//		Map<PK, List<Object>> sqlTextPkMap   = new HashMap<>();
//		List<List<Object>>    sqlTextRecords = new ArrayList<>();
//
//		Map<PK, List<Object>> planTextPkMap   = new HashMap<>();
//		List<List<Object>>    planTextRecords = new ArrayList<>();
//
//		List<List<Object>> statementRecords = new ArrayList<>();
//		Set<PK> statementPkAdded            = new HashSet<>();
//		Set<PK> statementPkDiscarded        = new HashSet<>();
//
//		PK pk;
//		List<Object> stmntRow;
//		List<Object> sqlTextRow;
//		List<Object> sqlPlanRow;
//
//		// MON_SQL_TEXT:      SPID, KPID, BatchID, ServerLogin, SQLText
//		// MON_SQL_PLAN:      SPID, KPID, PlanID, BatchID, ContextID, DBID, DBName, ProcedureID, PlanText
//		// MON_SQL_STATEMENT: SPID, KPID, DBID, ProcedureID, PlanID, BatchID, ContextID, LineNumber, ObjOwnerID, DBName, HashKey, SsqlId, ProcName, Elapsed_ms, CpuTime, WaitTime, MemUsageKB, PhysicalReads, LogicalReads, RowsAffected, ErrorStatus, ProcNestLevel StatementNumber, QueryOptimizationTime, PagesModified, PacketsSent, PacketsReceived, NetworkPacketSize, PlansAltered, StartTime, EndTime
//		
//		pk = new PK(1, 1, 1); // PK(SPID, KPID, BatchID);
//		stmntRow   = Arrays.asList(new Object[] { MON_SQL_STATEMENT, pk.SPID, pk.KPID, 99,99,99, pk.BatchID, 99,99,99,"master",99,99,"proc", 3000, 2000, 1000, 99, 1234, 12345, 1, 0, 0, 1, 100, 0, 10, 10, 2048, 0, new Timestamp(System.currentTimeMillis()-3000), new Timestamp(System.currentTimeMillis()) });
//		sqlTextRow = Arrays.asList(new Object[] { MON_SQL_TEXT,      pk.SPID, pk.KPID, pk.BatchID, "sa", "SQL-text-str for: "+pk});
//		sqlPlanRow = Arrays.asList(new Object[] { MON_SQL_PLAN,      pk.SPID, pk.KPID, 99, pk.BatchID, 99,1,"master",99, "PLAN-text-str for: "+pk});
//
//		statementRecords.add( stmntRow );
//		sqlTextRecords  .add( sqlTextRow );
//		planTextRecords .add( sqlPlanRow );
//
//		statementPkAdded.add(pk);
////		statementPkDiscarded.add(pk);
//		
//		sqlTextPkMap .put(pk, sqlTextRow);
//		planTextPkMap.put(pk, sqlPlanRow);
//
//
//		//------------------------------------------------
//		// Post processing
//		//------------------------------------------------
//		DeferredSqlAndPlanTextQueueEntry qe = new DeferredSqlAndPlanTextQueueEntry(
//				statementPkAdded, statementPkDiscarded, statementRecords, 
//				sqlTextPkMap, sqlTextRecords, 
//				planTextPkMap, planTextRecords);
//
//		// Add info to the Deferred Queue, which we cleanup in toPcs() when we have passed threshold to do it...
//		_deferredQueueFor_sqlTextAndPlan.add(qe);
//
//		// Remove SQLText and PlanText that is not within filters (for example: execution time is lower than X)  
//		doPostProcessing(qe);
//		
//		// Send counters for storage
//		int count = toPcs(null, qe);
//
//		List<List<Object>> list = _pcs.getList();
//		System.out.println(p+"toPct(): count="+count+", _pcs.list.size="+list.size());
//		for (int i=0; i<list.size(); i++)
//		{
//			System.out.println(p+"_pcs.list["+i+"]="+list.get(i));
//		}
//
//		System.out.println(p+"_deferredQueueFor_sqlTextAndPlan.size()="+_deferredQueueFor_sqlTextAndPlan.size());
//		Map<PK, List<Object>> map;
//		Set<PK> set;
//		int dqi=0;
//		for (DeferredSqlAndPlanTextQueueEntry dqe : _deferredQueueFor_sqlTextAndPlan)
//		{
//			list = dqe._statementRecords; for (int i=0; i<list.size(); i++) { System.out.println(p+"dq["+dqi+"]._statementRecords.list["+i+"]="+list.get(i)); }
//			list = dqe._sqlTextRecords;   for (int i=0; i<list.size(); i++) { System.out.println(p+"dq["+dqi+"]._sqlTextRecords  .list["+i+"]="+list.get(i)); }
//			list = dqe._planTextRecords;  for (int i=0; i<list.size(); i++) { System.out.println(p+"dq["+dqi+"]._planTextRecords .list["+i+"]="+list.get(i)); }
//
//			set  = dqe._statementPkAdded;     System.out.println(p+"dq["+dqi+"]._statementPkAdded ="+set);
//			set  = dqe._statementPkDiscarded; System.out.println(p+"dq["+dqi+"]._statementPkDiscarded ="+set);
//			map  = dqe._sqlTextPkMap;         System.out.println(p+"dq["+dqi+"]._sqlTextPkMap ="+map);
//			map  = dqe._planTextPkMap;        System.out.println(p+"dq["+dqi+"]._planTextPkMap="+map);
//			dqi++;
//		}
//		
//		return true;
//	}
//
//	public boolean test2(boolean debug)
//	{
//		return false;
//	}

	public SqlCaptureBrokerAse_tester(Configuration conf)
	{
		super();
		init(conf);
	}

	@Override
	protected void addToPcs(PersistentCounterHandler pch, SqlCaptureDetails sqlCaptureDetails)
	{
		_pcs = sqlCaptureDetails;
	}

	public static void createTables(DbxConnection conn)
	throws SQLException
	{
		conn.dbExec(conn.quotifySqlString(""
			    + "create table [monSysStatement] ( \n"
			    + "	[SPID]                            int                              not null, \n"
			    + "	[InstanceID]                      tinyint                          not null, \n"
			    + "	[KPID]                            int                              not null, \n"
			    + "	[DBID]                            int                              not null, \n"
			    + "	[ProcedureID]                     int                              not null, \n"
			    + "	[PlanID]                          int                              not null, \n"
			    + "	[BatchID]                         int                              not null, \n"
			    + "	[ContextID]                       int                              not null, \n"
			    + "	[LineNumber]                      int                              not null, \n"
			    + "	[CpuTime]                         int                              not null, \n"
			    + "	[WaitTime]                        int                              not null, \n"
			    + "	[MemUsageKB]                      int                              not null, \n"
			    + "	[PhysicalReads]                   bigint                           not null, \n"
			    + "	[LogicalReads]                    bigint                           not null, \n"
			    + "	[PagesModified]                   bigint                           not null, \n"
			    + "	[PacketsSent]                     int                              not null, \n"
			    + "	[PacketsReceived]                 int                              not null, \n"
			    + "	[NetworkPacketSize]               int                              not null, \n"
			    + "	[PlansAltered]                    int                              not null, \n"
			    + "	[RowsAffected]                    int                              not null, \n"
			    + "	[ErrorStatus]                     int                              not null, \n"
			    + "	[HashKey]                         int                              not null, \n"
			    + "	[SsqlId]                          int                              not null, \n"
			    + "	[ProcNestLevel]                   int                              not null, \n"
			    + "	[StatementNumber]                 int                              not null, \n"
			    + "	[DBName]                          varchar(30)                          null, \n"
			    + "	[StartTime]                       datetime                             null, \n"
			    + "	[EndTime]                         datetime                             null \n"
			    + ") \n"
			    + ""));

		conn.dbExec(conn.quotifySqlString(""
			    + "create table [monSysSQLText] ( \n"
			    + "	[SPID]                            int                              not null, \n"
			    + "	[InstanceID]                      tinyint                          not null, \n"
			    + "	[KPID]                            int                              not null, \n"
			    + "	[ServerUserID]                    int                              not null, \n"
			    + "	[BatchID]                         int                              not null, \n"
			    + "	[SequenceInBatch]                 int                              not null, \n"
			    + "	[SQLText]                         varchar(255)                         null \n"
			    + ") \n"
			    + ""));

	    conn.dbExec(conn.quotifySqlString(""
			    + "create table [monSysPlanText] ( \n"
			    + "	[PlanID]                          int                              not null, \n"
			    + "	[InstanceID]                      tinyint                          not null, \n"
			    + "	[SPID]                            int                              not null, \n"
			    + "	[KPID]                            int                              not null, \n"
			    + "	[BatchID]                         int                              not null, \n"
			    + "	[ContextID]                       int                              not null, \n"
			    + "	[SequenceNumber]                  int                              not null, \n"
			    + "	[DBID]                            int                              not null, \n"
			    + "	[ProcedureID]                     int                              not null, \n"
			    + "	[DBName]                          varchar(30)                          null, \n"
			    + "	[PlanText]                        varchar(160)                         null \n"
			    + ") \n"
			    + ""));

	    conn.dbExec(""
			    + "create table [sysprocesses] ( \n"
			    + "	[spid]                          int                              not null, \n"
			    + "	[program_name]                  varchar(30)                          null  \n"
			    + ") \n"
			    + "");
	}
	public static void truncSourceTables(DbxConnection conn)
	{
		conn.dbExecNoException(conn.quotifySqlString("delete from [monSysStatement]"));
		conn.dbExecNoException(conn.quotifySqlString("delete from [monSysSQLText]"));
		conn.dbExecNoException(conn.quotifySqlString("delete from [monSysPlanText]"));
	}

	public static void insertSysStatement(DbxConnection conn, int SPID, int InstanceID, int KPID, int DBID, int ProcedureID, int PlanID, int BatchID, int ContextID, int LineNumber, int CpuTime, int WaitTime, int MemUsageKB, int PhysicalReads, int LogicalReads, int PagesModified, int PacketsSent, int PacketsReceived, int NetworkPacketSize, int PlansAltered, int RowsAffected, int ErrorStatus, int HashKey, int SsqlId, int ProcNestLevel, int StatementNumber, String DBName, String StartTimeStr, String EndTimeStr)
	{
		try {
			Timestamp StartTime = TimeUtils.parseToTimestampX(StartTimeStr);
			Timestamp EndTime   = TimeUtils.parseToTimestampX(EndTimeStr);
			insertSysStatement(conn, SPID, InstanceID, KPID, DBID, ProcedureID, PlanID, BatchID, ContextID, LineNumber, CpuTime, WaitTime, MemUsageKB, PhysicalReads, LogicalReads, PagesModified, PacketsSent, PacketsReceived, NetworkPacketSize, PlansAltered, RowsAffected, ErrorStatus, HashKey, SsqlId, ProcNestLevel, StatementNumber, DBName, StartTime, EndTime);
		} catch(ParseException ex) {
			ex.printStackTrace();
		}
	}
	public static void insertSysStatement(DbxConnection conn, int SPID, int InstanceID, int KPID, int DBID, int ProcedureID, int PlanID, int BatchID, int ContextID, int LineNumber, int CpuTime, int WaitTime, int MemUsageKB, int PhysicalReads, int LogicalReads, int PagesModified, int PacketsSent, int PacketsReceived, int NetworkPacketSize, int PlansAltered, int RowsAffected, int ErrorStatus, int HashKey, int SsqlId, int ProcNestLevel, int StatementNumber, String DBName, long execTime)
	{
		Timestamp StartTime = new Timestamp( System.currentTimeMillis() - execTime );
		Timestamp EndTime   = new Timestamp( System.currentTimeMillis() );
			
		insertSysStatement(conn, SPID, InstanceID, KPID, DBID, ProcedureID, PlanID, BatchID, ContextID, LineNumber, CpuTime, WaitTime, MemUsageKB, PhysicalReads, LogicalReads, PagesModified, PacketsSent, PacketsReceived, NetworkPacketSize, PlansAltered, RowsAffected, ErrorStatus, HashKey, SsqlId, ProcNestLevel, StatementNumber, DBName, StartTime, EndTime);
	}
	public static void insertSysStatement(DbxConnection conn, int SPID, int InstanceID, int KPID, int DBID, int ProcedureID, int PlanID, int BatchID, int ContextID, int LineNumber, int CpuTime, int WaitTime, int MemUsageKB, int PhysicalReads, int LogicalReads, int PagesModified, int PacketsSent, int PacketsReceived, int NetworkPacketSize, int PlansAltered, int RowsAffected, int ErrorStatus, int HashKey, int SsqlId, int ProcNestLevel, int StatementNumber, String DBName, Timestamp StartTime, Timestamp EndTime)
	{
		conn.dbExecNoException(conn.quotifySqlString("insert into [monSysStatement] ([SPID], [InstanceID], [KPID], [DBID], [ProcedureID], [PlanID], [BatchID], [ContextID], [LineNumber], [CpuTime], [WaitTime], [MemUsageKB], [PhysicalReads], [LogicalReads], [PagesModified], [PacketsSent], [PacketsReceived], [NetworkPacketSize], [PlansAltered], [RowsAffected], [ErrorStatus], [HashKey], [SsqlId], [ProcNestLevel], [StatementNumber], [DBName], [StartTime], [EndTime]) "
				+ "values(" + SPID + ", " + InstanceID + ", " + KPID + ", " + DBID + ", " + ProcedureID + ", " + PlanID + ", " + BatchID + ", " + ContextID + ", " + LineNumber + ", " + CpuTime + ", " + WaitTime + ", " + MemUsageKB + ", " + PhysicalReads + ", " + LogicalReads + ", " + PagesModified + ", " + PacketsSent + ", " + PacketsReceived + ", " + NetworkPacketSize + ", " + PlansAltered + ", " + RowsAffected + ", " + ErrorStatus + ", " + HashKey + ", " + SsqlId + ", " + ProcNestLevel + ", " + StatementNumber + ", '" + DBName + "', '" + StartTime + "', '" + EndTime + "')  "));
	}

	public static void insertSysSQLText(DbxConnection conn, int SPID, int InstanceID, int KPID, int ServerUserID, int BatchID, int SequenceInBatch, String SQLText)
	{
		SQLText = SQLText.replace("'", "''");
		conn.dbExecNoException(conn.quotifySqlString("insert into [monSysSQLText] ([SPID], [InstanceID], [KPID], [ServerUserID], [BatchID], [SequenceInBatch], [SQLText]) "
				+ "values(" + SPID + ", " + InstanceID + ", " + KPID + ", " + ServerUserID + ", " + BatchID + ", " + SequenceInBatch + ", '" + SQLText + "')  "));
	}
	public static void insertSysPlanText(DbxConnection conn, int PlanID, int InstanceID, int SPID, int KPID, int BatchID, int ContextID, int SequenceNumber, int DBID, int ProcedureID, String DBName, String PlanText)
	{
		PlanText = PlanText.replace("'", "''");
		conn.dbExecNoException(conn.quotifySqlString("insert into [monSysPlanText] ([PlanID], [InstanceID], [SPID], [KPID], [BatchID], [ContextID], [SequenceNumber], [DBID], [ProcedureID], [DBName], [PlanText]) "
				+ "values(" + PlanID + ", " + InstanceID + ", " + SPID + ", " + KPID + ", " + BatchID + ", " + ContextID + ", " + SequenceNumber + ", " + DBID + ", " + ProcedureID + ", '" + DBName + "', '" + PlanText + "')   "));
	}
	
	public static void selectFrom(DbxConnection conn, String tabName)
	throws SQLException
	{
		String sql = conn.quotifySqlString("select * from ["+tabName+"]");
		
		try ( Statement stmnt = conn.createStatement(); ResultSet rs = stmnt.executeQuery(sql); )
		{
			ResultSetTableModel rstm = new ResultSetTableModel(rs, "dummy");
			System.out.println("\n#### Table: "+tabName+ "\n" + rstm.toAsciiTableString());
		}
	}
	
	private void printInfo(boolean debug, int indentLvl, String str)
	{
		if ( ! debug )
			return;

//		String info = "debug: ";
		String info = "> ";
		if (indentLvl == 0)
			info = "";
		
		String indent = StringUtil.replicate("\t", indentLvl);
		
		System.out.println( indent + info + str);
	}

	private void printError(String str)
	{
		System.out.println( "    >>> ERROR: " + str);
	}

//	private boolean checkPcsAndDq(
//			String testName,
//			boolean debug,
//			SqlCaptureDetails pcsCapDet,
//			DeferredSqlAndPlanTextQueue defQueue,
//			int expectedPcsSize, 
//			int expectedPcsStatementSize, 
//			int expectedPcsSqlSize, 
//			int expectedPcsPlanSize, 
//			int expectedDefQueueSize, 
//			int expectedDefQueueStatementRecords, 
////			int expectedDefQueueSqlRecords,
////			int expectedDefQueuePlanRecords,
//			int expectedDefQueuePkAddedSize,
//			int expectedDefQueuePkDiscardSize,
//			int expectedDefQueueSqlPkSize,
//			int expectedDefQueuePlanPkSize
//			)
//	{
//		boolean error = false;
//
//		//---------------------------------------------
//		// Check expected PCS size
//		if (expectedPcsSize != pcsCapDet.size())
//		{
//			printError("expected_PcsSize="+expectedPcsSize+", actualSize="+pcsCapDet.size());
//			error = true;
//		}
//
//		if (debug || error)
//		{
//			List<List<Object>> list = pcsCapDet.getList();
//			printInfo(debug, 1, "pcsCapDet: pcsCapDet.getList().size()="+list.size());
//			for (int i=0; i<list.size(); i++)
//				printInfo(debug, 2, "pcsCapDet.list["+i+"]="+list.get(i));
//		}
//
//		//---------------------------------------------
//		// Check expected PCS content
//		int pcsStatementRecords     = 0;
//		int pcsSqlTextRecords       = 0;
//		int pcsPlanTextRecords      = 0;
//		
//		for (List<Object> qe : pcsCapDet.getList())
//		{
//			String type = qe.get(0).toString();
//			if (MON_SQL_STATEMENT.equals(type)) pcsStatementRecords++;
//			if (MON_SQL_TEXT     .equals(type)) pcsSqlTextRecords++;
//			if (MON_SQL_PLAN     .equals(type)) pcsPlanTextRecords++;
//		}
//
//		if (expectedPcsStatementSize != pcsStatementRecords) { error = true; printError("expected_Pcs_StatementSize = " + expectedPcsStatementSize +", actualSize = " + pcsStatementRecords); }
//		if (expectedPcsSqlSize       != pcsSqlTextRecords  ) { error = true; printError("expected_Pcs_SqlSize       = " + expectedPcsSqlSize       +", actualSize = " + pcsSqlTextRecords); }
//		if (expectedPcsPlanSize      != pcsPlanTextRecords ) { error = true; printError("expected_Pcs_PlanSize      = " + expectedPcsPlanSize      +", actualSize = " + pcsPlanTextRecords); }
//		
//		if (error)
//		{
//			List<List<Object>> list = pcsCapDet.getList();
//			printInfo(debug, 1, "pcsCapDet: pcsCapDet.getList().size()="+list.size());
//			for (int i=0; i<list.size(); i++)
//				printInfo(debug, 2, "pcsCapDet.list["+i+"]="+list.get(i));
//			
//		}
//		
//		
//		//---------------------------------------------
//		// Check expected DeferredQueueSize size
//		if (expectedDefQueueSize != defQueue.size())
//		{
//			printError("expectedDef_QueueSize = "+expectedDefQueueSize + ", actualSize = " + defQueue.size());
//			error = true;
//		}
//
//		if (debug || error)
//		{
//			printInfo(debug, 1, "dq.size()="+defQueue.size());
//		}
//		
//		//---------------------------------------------
//		// 
//		int dqStatementRecords     = 0;
//		int dqSqlTextRecords       = 0;
//		int dqPlanTextRecords      = 0;
//		int dqStatementPkAdded     = 0;
//		int dqStatementPkDiscarded = 0;
//		int dqSqlTextPkMap         = 0;
//		int dqPlanTextPkMap        = 0;
//		for (DeferredSqlAndPlanTextQueueEntry dqe : defQueue)
//		{
//			List<List<Object>> list;
//
//			list = dqe._statementRecords; dqStatementRecords += list.size();
////			list = dqe._sqlTextRecords;   dqSqlTextRecords   += list.size();
////			list = dqe._planTextRecords;  dqPlanTextRecords  += list.size();
//
//			dqStatementPkAdded     += dqe._statementPkAdded    .size();
//			dqStatementPkDiscarded += dqe._statementPkDiscarded.size();
//			                                
//			dqSqlTextPkMap         += dqe._sqlTextPkMap        .size();
//			dqPlanTextPkMap        += dqe._planTextPkMap       .size();
//		}
//		
//		if (dqStatementRecords     != expectedDefQueueStatementRecords) { error = true; printError("expectedDefQueue_StatementRecords = " + expectedDefQueueStatementRecords + ", actualSize = " + dqStatementRecords); }
////		if (dqSqlTextRecords       != expectedDefQueueSqlRecords      ) { error = true; printError("expectedDefQueue_SqlRecords       = " + expectedDefQueueSqlRecords       + ", actualSize = " + dqSqlTextRecords); }
////		if (dqPlanTextRecords      != expectedDefQueuePlanRecords     ) { error = true; printError("expectedDefQueue_PlanRecords      = " + expectedDefQueuePlanRecords      + ", actualSize = " + dqPlanTextRecords); }
//		if (dqStatementPkAdded     != expectedDefQueuePkAddedSize     ) { error = true; printError("expectedDefQueue_PkAddedSize      = " + expectedDefQueuePkAddedSize      + ", actualSize = " + dqStatementPkAdded); }
//		if (dqStatementPkDiscarded != expectedDefQueuePkDiscardSize   ) { error = true; printError("expectedDefQueue_PkDiscardSize    = " + expectedDefQueuePkDiscardSize    + ", actualSize = " + dqStatementPkDiscarded); }
//		if (dqSqlTextPkMap         != expectedDefQueueSqlPkSize       ) { error = true; printError("expectedDefQueue_SqlPkSize        = " + expectedDefQueueSqlPkSize        + ", actualSize = " + dqSqlTextPkMap); }
//		if (dqPlanTextPkMap        != expectedDefQueuePlanPkSize      ) { error = true; printError("expectedDefQueue_PlanPkSize       = " + expectedDefQueuePlanPkSize       + ", actualSize = " + dqPlanTextPkMap); }
//
//		if (debug || error)
//		{
//			int dqi=0;
//			for (DeferredSqlAndPlanTextQueueEntry dqe : defQueue)
//			{
//				List<List<Object>> list;
//				Map<PK, List<Object>> map;
//				Set<PK> set;
//				
//				list = dqe._statementRecords; printInfo(debug, 2, "dq["+dqi+"]._statementRecords.list.size()="+list.size()); for (int i=0; i<list.size(); i++) { printInfo(debug, 3, "dq["+dqi+"]._statementRecords.list["+i+"]="+list.get(i)); }
////				list = dqe._sqlTextRecords;   printInfo(debug, 2, "dq["+dqi+"]._sqlTextRecords  .list.size()="+list.size()); for (int i=0; i<list.size(); i++) { printInfo(debug, 3, "dq["+dqi+"]._sqlTextRecords  .list["+i+"]="+list.get(i)); }
////				list = dqe._planTextRecords;  printInfo(debug, 2, "dq["+dqi+"]._planTextRecords .list.size()="+list.size()); for (int i=0; i<list.size(); i++) { printInfo(debug, 3, "dq["+dqi+"]._planTextRecords .list["+i+"]="+list.get(i)); }
//	
//				printInfo(debug, 2, "-------------------");
//				set  = dqe._statementPkAdded;     printInfo(debug, 2, "dq["+dqi+"]._statementPkAdded     size="+dqe._statementPkAdded    .size()+": = " + set);
//				set  = dqe._statementPkDiscarded; printInfo(debug, 2, "dq["+dqi+"]._statementPkDiscarded size="+dqe._statementPkDiscarded.size()+": = " + set);
//				printInfo(debug, 2, "-------------------");
//				map  = dqe._sqlTextPkMap;         printInfo(debug, 2, "dq["+dqi+"]._sqlTextPkMap         size="+dqe._sqlTextPkMap        .size()+": = " + map);
//				map  = dqe._planTextPkMap;        printInfo(debug, 2, "dq["+dqi+"]._planTextPkMap        size="+dqe._planTextPkMap       .size()+": = " + map);
//				dqi++;
//			}
//			
//		}
//		
//		return ! error;
//	}
//	
//	/**
//	 * Test normal, add:
//	 * - 1 SQL Text
//	 * - 1 SQL Plan
//	 * - 1 SQL Statement
//	 * 
//	 * This should produce:
//	 * - 3 PCS entry (1=statement, 1=sqlText, 1=planText)
//	 * - 0 entries in the deferred queue
//	 */
//	public boolean test_normal(DbxConnection conn, boolean debug)
//	{
//		String testName = new Object(){}.getClass().getEnclosingMethod().getName();
//		System.out.println("\n" + testName + ": START ------------------------------------------------------");
//		printInfo(debug, 0, testName+": START");
//		
//		PK pk = new PK(1, 1, 1);
//
//		long execTime = 1000;
//		int PhysicalReads = 0;
//		int LogicalReads  = 0;
////		insertSysStatement(conn, SPID,    InstanceID, KPID,    DBID, ProcedureID, PlanID, BatchID,    ContextID, LineNumber, CpuTime, WaitTime, MemUsageKB, PhysicalReads, LogicalReads, PagesModified, PacketsSent, PacketsReceived, NetworkPacketSize, PlansAltered, RowsAffected, ErrorStatus, HashKey, SsqlId, ProcNestLevel, StatementNumber, DBName,   StartTime, EndTime);
//		insertSysStatement(conn, pk.SPID, 99,         pk.KPID, 1,    99,          99,     pk.BatchID, 99,        1,          5,       5,        99,         PhysicalReads, LogicalReads, 1,             1,           1,               2048,              0,            1,            0,           99,      99,     0,             99,              "master", execTime);
//
////		insertSysSQLText(conn, SPID,    InstanceID, KPID,    ServerUserID, BatchID,    SequenceInBatch, SQLText);
//		insertSysSQLText(conn, pk.SPID, 99,         pk.KPID, 99,           pk.BatchID, 1,               "declare @xxx ");
//		insertSysSQLText(conn, pk.SPID, 99,         pk.KPID, 99,           pk.BatchID, 2,               "select @xxx=id from dummy1 ");
//
////		insertSysPlanText(conn, PlanID, InstanceID, SPID,    KPID,    BatchID,    ContextID, SequenceNumber, DBID, ProcedureID, DBName,   PlanText);
//		insertSysPlanText(conn, 99,     99,         pk.SPID, pk.KPID, pk.BatchID, 99,        1,              1,    0,           "master", "dymmy 1 ");
//		insertSysPlanText(conn, 99,     99,         pk.SPID, pk.KPID, pk.BatchID, 99,        2,              1,    0,           "master", "dymmy 2 ");
//		insertSysPlanText(conn, 99,     99,         pk.SPID, pk.KPID, pk.BatchID, 99,        3,              1,    0,           "master", "dymmy 3 ");
//		
//		int count = doSqlCapture(conn, null);
//		
//		truncSourceTables(conn);
//		return checkPcsAndDq(testName, debug, _pcs, _deferredQueueFor_sqlTextAndPlan, 
//				3, // expectedPcsSize, 
//				1, // expectedPcsStatementSize
//				1, // expectedPcsSqlSize
//				1, // expectedPcsPlanSize
//				0, // expectedDefQueueSize, 
//				0, // expectedDefQueueStatementRecords, 
////				0, // expectedDefQueueSqlRecords,
////				0, // expectedDefQueuePlanRecords,
//				0, // expectedDefQueuePkAddedSize,
//				0, // expectedDefQueuePkDiscardSize,
//				0, // expectedDefQueueSqlPkSize,
//				0  // expectedDefQueuePlanPkSize
//				);
//	}
//
//	/**
//	 * SQL Wiy�th 5ms exec time should be FILTERED OUT
//	 * This should produce:
//	 * - 0 PCS entry (0=statement, 0=sqlText, 0=planText)
//	 * - 0 entries in the deferred queue
//	 */
//	public boolean test_short(DbxConnection conn, boolean debug)
//	{
//		String testName = new Object(){}.getClass().getEnclosingMethod().getName();
//		System.out.println("\n" + testName + ": START ------------------------------------------------------");
//		printInfo(debug, 0, testName+": START");
//		
//		PK pk = new PK(1, 1, 1);
//
//		long execTime = 5;
//		int PhysicalReads = 0;
//		int LogicalReads  = 0;
////		insertSysStatement(conn, SPID,    InstanceID, KPID,    DBID, ProcedureID, PlanID, BatchID,    ContextID, LineNumber, CpuTime, WaitTime, MemUsageKB, PhysicalReads, LogicalReads, PagesModified, PacketsSent, PacketsReceived, NetworkPacketSize, PlansAltered, RowsAffected, ErrorStatus, HashKey, SsqlId, ProcNestLevel, StatementNumber, DBName,   StartTime, EndTime);
//		insertSysStatement(conn, pk.SPID, 99,         pk.KPID, 1,    99,          99,     pk.BatchID, 99,        1,          5,       5,        99,         PhysicalReads, LogicalReads, 1,             1,           1,               2048,              0,            1,            0,           99,      99,     0,             99,              "master", execTime);
//
////		insertSysSQLText(conn, SPID,    InstanceID, KPID,    ServerUserID, BatchID,    SequenceInBatch, SQLText);
//		insertSysSQLText(conn, pk.SPID, 99,         pk.KPID, 99,           pk.BatchID, 1,               "declare @xxx ");
//		insertSysSQLText(conn, pk.SPID, 99,         pk.KPID, 99,           pk.BatchID, 2,               "select @xxx=id from dummy1 ");
//
////		insertSysPlanText(conn, PlanID, InstanceID, SPID,    KPID,    BatchID,    ContextID, SequenceNumber, DBID, ProcedureID, DBName,   PlanText);
//		insertSysPlanText(conn, 99,     99,         pk.SPID, pk.KPID, pk.BatchID, 99,        1,              1,    0,           "master", "dymmy 1 ");
//		insertSysPlanText(conn, 99,     99,         pk.SPID, pk.KPID, pk.BatchID, 99,        2,              1,    0,           "master", "dymmy 2 ");
//		insertSysPlanText(conn, 99,     99,         pk.SPID, pk.KPID, pk.BatchID, 99,        3,              1,    0,           "master", "dymmy 3 ");
//		
//		int count = doSqlCapture(conn, null);
//		
//		truncSourceTables(conn);
//		return checkPcsAndDq(testName, debug, _pcs, _deferredQueueFor_sqlTextAndPlan, 
//				0, // expectedPcsSize, 
//				0, // expectedPcsStatementSize
//				0, // expectedPcsSqlSize
//				0, // expectedPcsPlanSize
//				0, // expectedDefQueueSize, 
//				0, // expectedDefQueueStatementRecords, 
////				0, // expectedDefQueueSqlRecords,
////				0, // expectedDefQueuePlanRecords,
//				0, // expectedDefQueuePkAddedSize,
//				0, // expectedDefQueuePkDiscardSize,
//				0, // expectedDefQueueSqlPkSize,
//				0  // expectedDefQueuePlanPkSize
//				);
//	}
//
//	/**
//	 * WAITFOR test... (poll 1):
//	 * - 0 SQL Statement (not yet finnished)
//	 * - 1 SQL Text (sql text/plan will be teher)
//	 * - 1 SQL Plan (sql text/plan will be teher)
//	 * 
//	 * This should produce:
//	 * - 0 PCS entry (0=statement, 0=sqlText, 0=planText)
//	 * - 1 entries in the deferred queue (1=sqlText, 1=planText)
//	 */
//	public boolean test_waitfor_start(DbxConnection conn, boolean debug)
//	{
//		String testName = new Object(){}.getClass().getEnclosingMethod().getName();
//		System.out.println("\n" + testName + ": START ------------------------------------------------------");
//		printInfo(debug, 0, testName+": START");
//		
//		PK pk = new PK(2, 1, 1);
//
//		long execTime = 2000;
//		int PhysicalReads = 0;
//		int LogicalReads  = 0;
////		insertSysStatement(conn, SPID,    InstanceID, KPID,    DBID, ProcedureID, PlanID, BatchID,    ContextID, LineNumber, CpuTime, WaitTime, MemUsageKB, PhysicalReads, LogicalReads, PagesModified, PacketsSent, PacketsReceived, NetworkPacketSize, PlansAltered, RowsAffected, ErrorStatus, HashKey, SsqlId, ProcNestLevel, StatementNumber, DBName,   StartTime, EndTime);
////		insertSysStatement(conn, pk.SPID, 99,         pk.KPID, 1,    99,          99,     pk.BatchID, 99,        1,          5,       5,        99,         PhysicalReads, LogicalReads, 1,             1,           1,               2048,              0,            1,            0,           99,      99,     0,             99,              "master", execTime);
//
////		insertSysSQLText(conn, SPID,    InstanceID, KPID,    ServerUserID, BatchID,    SequenceInBatch, SQLText);
//		insertSysSQLText(conn, pk.SPID, 99,         pk.KPID, 99,           pk.BatchID, 1,               "waitfor delay '00:00:02' ");
//
////		insertSysPlanText(conn, PlanID, InstanceID, SPID,    KPID,    BatchID,    ContextID, SequenceNumber, DBID, ProcedureID, DBName,   PlanText);
//		insertSysPlanText(conn, 99,     99,         pk.SPID, pk.KPID, pk.BatchID, 99,        1,              1,    0,           "master", "PLAN waitfor row 1 ");
//		insertSysPlanText(conn, 99,     99,         pk.SPID, pk.KPID, pk.BatchID, 99,        2,              1,    0,           "master", "PLAN waitfor row 2 ");
//		insertSysPlanText(conn, 99,     99,         pk.SPID, pk.KPID, pk.BatchID, 99,        3,              1,    0,           "master", "PLAN waitfor row 3 ");
//		
//		int count = doSqlCapture(conn, null);
//		
//		truncSourceTables(conn);
//		return checkPcsAndDq(testName, debug, _pcs, _deferredQueueFor_sqlTextAndPlan, 
//				0, // expectedPcs      Size, 
//				0, // expectedPcs      StatementSize
//				0, // expectedPcs      SqlSize
//				0, // expectedPcs      PlanSize
//				1, // expectedDefQueue Size, 
//				0, // expectedDefQueue StatementRecords, 
////				1, // expectedDefQueue SqlRecords,
////				1, // expectedDefQueue PlanRecords,
//				0, // expectedDefQueue PkAddedSize,
//				0, // expectedDefQueue PkDiscardSize,
//				1, // expectedDefQueue SqlPkSize,
//				1  // expectedDefQueue PlanPkSize
//				);
//	}
//
//	/**
//	 * WAITFOR test... (poll 2):
//	 * - 1 SQL Statement (now finnished)
//	 * - 0 SQL Text (already sampled)
//	 * - 0 SQL Plan (already sampled)
//	 * 
//	 * This should produce:
//	 * - 3 PCS entry (1=statement, 1=sqlText, 1=planText)
//	 * - 0 entries in the deferred queue (0=sqlText, 0=planText)
//	 */
//	public boolean test_waitfor_end(DbxConnection conn, boolean debug)
//	{
//		String testName = new Object(){}.getClass().getEnclosingMethod().getName();
//		System.out.println("\n" + testName + ": START ------------------------------------------------------");
//		printInfo(debug, 0, testName+": START");
//		
//		PK pk = new PK(2, 1, 1);
//
//		long execTime = 2000;
//		int PhysicalReads = 0;
//		int LogicalReads  = 0;
////		insertSysStatement(conn, SPID,    InstanceID, KPID,    DBID, ProcedureID, PlanID, BatchID,    ContextID, LineNumber, CpuTime, WaitTime, MemUsageKB, PhysicalReads, LogicalReads, PagesModified, PacketsSent, PacketsReceived, NetworkPacketSize, PlansAltered, RowsAffected, ErrorStatus, HashKey, SsqlId, ProcNestLevel, StatementNumber, DBName,   StartTime, EndTime);
//		insertSysStatement(conn, pk.SPID, 99,         pk.KPID, 1,    99,          99,     pk.BatchID, 99,        1,          5,       5,        99,         PhysicalReads, LogicalReads, 1,             1,           1,               2048,              0,            1,            0,           99,      99,     0,             99,              "master", execTime);
//
////		insertSysSQLText(conn, SPID,    InstanceID, KPID,    ServerUserID, BatchID,    SequenceInBatch, SQLText);
////		insertSysSQLText(conn, pk.SPID, 99,         pk.KPID, 99,           pk.BatchID, 1,               "waitfor delay '00:00:02' ");
//
////		insertSysPlanText(conn, PlanID, InstanceID, SPID,    KPID,    BatchID,    ContextID, SequenceNumber, DBID, ProcedureID, DBName,   PlanText);
////		insertSysPlanText(conn, 99,     99,         pk.SPID, pk.KPID, pk.BatchID, 99,        1,              1,    0,           "master", "PLAN waitfor row 1 ");
////		insertSysPlanText(conn, 99,     99,         pk.SPID, pk.KPID, pk.BatchID, 99,        2,              1,    0,           "master", "PLAN waitfor row 2 ");
////		insertSysPlanText(conn, 99,     99,         pk.SPID, pk.KPID, pk.BatchID, 99,        3,              1,    0,           "master", "PLAN waitfor row 3 ");
//		
//		int count = doSqlCapture(conn, null);
//		
//		truncSourceTables(conn);
//		return checkPcsAndDq(testName, debug, _pcs, _deferredQueueFor_sqlTextAndPlan, 
//				3, // expectedPcsSize, 
//				1, // expectedPcsStatementSize
//				1, // expectedPcsSqlSize
//				1, // expectedPcsPlanSize
//				0, // expectedDefQueueSize, 
//				0, // expectedDefQueueStatementRecords, 
////				0, // expectedDefQueueSqlRecords,
////				0, // expectedDefQueuePlanRecords,
//				0, // expectedDefQueuePkAddedSize,
//				0, // expectedDefQueuePkDiscardSize,
//				0, // expectedDefQueueSqlPkSize,
//				0  // expectedDefQueuePlanPkSize
//				);
//	}
//
//	
//	/**
//	 * remove some static SQL that should not have been enetered by ASE:
//	 * - Simulate one select, which should be passed
//	 * - then add some SQL that should be deleted
//	 *  
//	 * This should produce:
//	 * - 1 PCS entry (1=statement, 1=sqlText, 1=planText)
//	 * - 3 entries in the deferred queue (0=sqlText, 0=planText)
//	 */
//	public boolean test_removeStatSql(DbxConnection conn, boolean debug)
//	{
//		String testName = new Object(){}.getClass().getEnclosingMethod().getName();
//		System.out.println("\n" + testName + ": START ------------------------------------------------------");
//		printInfo(debug, 0, testName+": START");
//		
//		{
//			PK pk = new PK(3, 1, 1);
//			
//			long execTime = 2000;
//			int PhysicalReads = 0;
//			int LogicalReads  = 0;
////			insertSysStatement(conn, SPID,    InstanceID, KPID,    DBID, ProcedureID, PlanID, BatchID,    ContextID, LineNumber, CpuTime, WaitTime, MemUsageKB, PhysicalReads, LogicalReads, PagesModified, PacketsSent, PacketsReceived, NetworkPacketSize, PlansAltered, RowsAffected, ErrorStatus, HashKey, SsqlId, ProcNestLevel, StatementNumber, DBName,   StartTime, EndTime);
//			insertSysStatement(conn, pk.SPID, 99,         pk.KPID, 1,    99,          99,     pk.BatchID, 99,        1,          5,       5,        99,         PhysicalReads, LogicalReads, 1,             1,           1,               2048,              0,            1,            0,           99,      99,     0,             99,              "master", execTime);
//			
////			insertSysSQLText(conn, SPID,    InstanceID, KPID,    ServerUserID, BatchID,    SequenceInBatch, SQLText);
//			insertSysSQLText(conn, pk.SPID, 99,         pk.KPID, 99,           pk.BatchID, 1,               "select 1 from xxx ");
//			
////			insertSysPlanText(conn, PlanID, InstanceID, SPID,    KPID,    BatchID,    ContextID, SequenceNumber, DBID, ProcedureID, DBName,   PlanText);
//			insertSysPlanText(conn, 99,     99,         pk.SPID, pk.KPID, pk.BatchID, 99,        1,              1,    0,           "master", "PLAN xxx row 1 ");
//			insertSysPlanText(conn, 99,     99,         pk.SPID, pk.KPID, pk.BatchID, 99,        2,              1,    0,           "master", "PLAN xxx row 2 ");
//			insertSysPlanText(conn, 99,     99,         pk.SPID, pk.KPID, pk.BatchID, 99,        3,              1,    0,           "master", "PLAN xxx row 3 ");
//		}
//		
//		{
//			PK pk = new PK(3, 2, 1);
//			
//			long execTime = 2000;
//			int PhysicalReads = 0;
//			int LogicalReads  = 0;
////			insertSysStatement(conn, SPID,    InstanceID, KPID,    DBID, ProcedureID, PlanID, BatchID,    ContextID, LineNumber, CpuTime, WaitTime, MemUsageKB, PhysicalReads, LogicalReads, PagesModified, PacketsSent, PacketsReceived, NetworkPacketSize, PlansAltered, RowsAffected, ErrorStatus, HashKey, SsqlId, ProcNestLevel, StatementNumber, DBName,   StartTime, EndTime);
////			insertSysStatement(conn, pk.SPID, 99,         pk.KPID, 1,    99,          99,     pk.BatchID, 99,        1,          5,       5,        99,         PhysicalReads, LogicalReads, 1,             1,           1,               2048,              0,            1,            0,           99,      99,     0,             99,              "master", execTime);
//			
////			insertSysSQLText(conn, SPID,    InstanceID, KPID,    ServerUserID, BatchID,    SequenceInBatch, SQLText);
//			insertSysSQLText(conn, pk.SPID, 99,         pk.KPID, 99,           1, 1,               "DYNAMIC_SQL FetchMetaData: ");
//			insertSysSQLText(conn, pk.SPID, 99,         pk.KPID, 99,           2, 1,               "create proc FetchMetaData as select * from someTableName");
//			insertSysSQLText(conn, pk.SPID, 99,         pk.KPID, 99,           3, 1,               "set string_rtruncation on");
//			insertSysSQLText(conn, pk.SPID, 99,         pk.KPID, 99,           4, 1,               "set textsize 100000");
//			insertSysSQLText(conn, pk.SPID, 99,         pk.KPID, 99,           5, 1,               "DYNAMIC_SQL dyn321");
//			insertSysSQLText(conn, pk.SPID, 99,         pk.KPID, 99,           6, 1,               "DYNAMIC_SQL dyn123");
//		}
//
//		int count = doSqlCapture(conn, null);
//		
//		truncSourceTables(conn);
//		return checkPcsAndDq(testName, debug, _pcs, _deferredQueueFor_sqlTextAndPlan, 
//				3, // expectedPcsSize, 
//				1, // expectedPcsStatementSize
//				1, // expectedPcsSqlSize
//				1, // expectedPcsPlanSize
//				0, // expectedDefQueueSize, 
//				0, // expectedDefQueueStatementRecords, 
////				0, // expectedDefQueueSqlRecords,
////				0, // expectedDefQueuePlanRecords,
//				0, // expectedDefQueuePkAddedSize,
//				0, // expectedDefQueuePkDiscardSize,
//				0, // expectedDefQueueSqlPkSize,
//				0  // expectedDefQueuePlanPkSize
//				);
//	}
//
//
//
//	/**
//	 * long running tran pass 1:
//	 * - insert a SQL (but no statement, which we do in pass 2)
//	 *  
//	 * This should produce:
//	 * - 0 PCS entry (0=statement, 0=sqlText, 0=planText)
//	 * - 2 entries in the deferred queue (1=sqlText, 1=planText)
//	 */
//	public boolean test_longRunning_pass1(DbxConnection conn, boolean debug)
//	{
//		String testName = new Object(){}.getClass().getEnclosingMethod().getName();
//		System.out.println("\n" + testName + ": START ------------------------------------------------------");
//		printInfo(debug, 0, testName+": START");
//		
//		{
//			PK pk = new PK(4, 1, 1);
//			
//			long execTime = 2000;
//			int PhysicalReads = 0;
//			int LogicalReads  = 0;
////			insertSysStatement(conn, SPID,    InstanceID, KPID,    DBID, ProcedureID, PlanID, BatchID,    ContextID, LineNumber, CpuTime, WaitTime, MemUsageKB, PhysicalReads, LogicalReads, PagesModified, PacketsSent, PacketsReceived, NetworkPacketSize, PlansAltered, RowsAffected, ErrorStatus, HashKey, SsqlId, ProcNestLevel, StatementNumber, DBName,   StartTime, EndTime);
////			insertSysStatement(conn, pk.SPID, 99,         pk.KPID, 1,    99,          99,     pk.BatchID, 99,        1,          5,       5,        99,         PhysicalReads, LogicalReads, 1,             1,           1,               2048,              0,            1,            0,           99,      99,     0,             99,              "master", execTime);
//			
////			insertSysSQLText(conn, SPID,    InstanceID, KPID,    ServerUserID, BatchID,    SequenceInBatch, SQLText);
//			insertSysSQLText(conn, pk.SPID, 99,         pk.KPID, 99,           pk.BatchID, 1,               "dump database xxx --which takes 2 hours");
//			
////			insertSysPlanText(conn, PlanID, InstanceID, SPID,    KPID,    BatchID,    ContextID, SequenceNumber, DBID, ProcedureID, DBName,   PlanText);
//			insertSysPlanText(conn, 99,     99,         pk.SPID, pk.KPID, pk.BatchID, 99,        1,              1,    0,           "master", "PLAN xxx row 1 ");
//			insertSysPlanText(conn, 99,     99,         pk.SPID, pk.KPID, pk.BatchID, 99,        2,              1,    0,           "master", "PLAN xxx row 2 ");
//			insertSysPlanText(conn, 99,     99,         pk.SPID, pk.KPID, pk.BatchID, 99,        3,              1,    0,           "master", "PLAN xxx row 3 ");
//		}
//		
//		int count = doSqlCapture(conn, null);
//		
//		truncSourceTables(conn);
//		return checkPcsAndDq(testName, debug, _pcs, _deferredQueueFor_sqlTextAndPlan, 
//				0, // expectedPcsSize, 
//				0, // expectedPcsStatementSize
//				0, // expectedPcsSqlSize
//				0, // expectedPcsPlanSize
//				1, // expectedDefQueueSize, 
//				0, // expectedDefQueueStatementRecords, 
////				1, // expectedDefQueueSqlRecords,
////				1, // expectedDefQueuePlanRecords,
//				0, // expectedDefQueuePkAddedSize,
//				0, // expectedDefQueuePkDiscardSize,
//				1, // expectedDefQueueSqlPkSize,
//				1  // expectedDefQueuePlanPkSize
//				);
//	}
//
//	/**
//	 * long running tran pass 2:
//	 * - do nothing (but the threshold timeout should have passed, and therefor adding SQL/Plan to PCS)
//	 *  
//	 * This should produce:
//	 * - 2 PCS entry (0=statement, 1=sqlText, 1=planText)
//	 * - 0 entries in the deferred queue (0=sqlText, 0=planText)
//	 */
//	public boolean test_longRunning_pass2(DbxConnection conn, boolean debug)
//	{
//		String testName = new Object(){}.getClass().getEnclosingMethod().getName();
//		System.out.println("\n" + testName + ": START ------------------------------------------------------");
//		printInfo(debug, 0, testName+": START");
//		
//		int count = doSqlCapture(conn, null);
//		
//		truncSourceTables(conn);
//		return checkPcsAndDq(testName, debug, _pcs, _deferredQueueFor_sqlTextAndPlan, 
//				2, // expectedPcsSize, 
//				0, // expectedPcsStatementSize
//				1, // expectedPcsSqlSize
//				1, // expectedPcsPlanSize
//				0, // expectedDefQueueSize, 
//				0, // expectedDefQueueStatementRecords, 
////				0, // expectedDefQueueSqlRecords,
////				0, // expectedDefQueuePlanRecords,
//				0, // expectedDefQueuePkAddedSize,
//				0, // expectedDefQueuePkDiscardSize,
//				0, // expectedDefQueueSqlPkSize,
//				0  // expectedDefQueuePlanPkSize
//				);
//	}
//
//
//	/**
//	 * Simulate a precedure call, that has ...
//	 *  
//	 * This should produce:
//	 * - 1 PCS entry (1=statement, 1=sqlText, 1=planText)
//	 * - 3 entries in the deferred queue (0=sqlText, 0=planText)
//	 */
//	public boolean test_proc_maxm_pass1(DbxConnection conn, boolean debug)
//	{
//		String testName = new Object(){}.getClass().getEnclosingMethod().getName();
//		System.out.println("\n" + testName + ": START ------------------------------------------------------");
//		printInfo(debug, 0, testName+": START");
//		
//		{
//			PK pk = new PK(168, 1185874617, 8848);
//			
//			long execTime = 680;
//			int PhysicalReads = 0;
//			int LogicalReads  = 0;
////			insertSysStatement(conn, SPID,    InstanceID, KPID,       DBID, ProcedureID, PlanID,   BatchID,    ContextID, LineNumber, CpuTime, WaitTime, MemUsageKB, PhysicalReads, LogicalReads, PagesModified, PacketsSent, PacketsReceived, NetworkPacketSize, PlansAltered, RowsAffected, ErrorStatus, HashKey, SsqlId, ProcNestLevel, StatementNumber, DBName,   StartTime, EndTime);
////			insertSysStatement(conn, 168,     0,          1185874617, 4,    1036579750,  64479147, 8848,       1,         0,          0,       0,        38,         0,             0,            0,             0,           0,               512,               0,            0,            0,           0,       0,      1,             2,               "PML",    "2018-01-29 16:20:21.863", "2018-01-29 16:20:21.863");
////			insertSysStatement(conn, 168,     0,          1185874617, 4,    1036579750,  64479147, 8848,       1,         10,         0,       0,        40,         0,             0,            0,             0,           0,               512,               0,            1,            0,           0,       0,      1,             3,               "PML",    "2018-01-29 16:20:21.863", "2018-01-29 16:20:21.863");
/////*-680ms->*/insertSysStatement(conn, 168,     0,          1185874617, 4,    1036579750,  64479147, 8848,       1,         11,         0,       0,        0,          0,             0,            0,             0,           0,               512,               0,            0,            0,           0,       0,      1,             4,               "PML",    "2018-01-29 16:20:21.863", "2018-01-29 16:20:22.543");
////			insertSysStatement(conn, 168,     0,          1185874617, 4,    1036579750,  64479147, 8848,       1,         12,         0,       0,        0,          0,             0,            0,             0,           0,               512,               0,            1,            0,           0,       0,      1,             7,               "PML",    "2018-01-29 16:20:22.543", "2018-01-29 16:20:22.543");
////			insertSysStatement(conn, 168,     0,          1185874617, 4,    156524560,   64479149, 8848,       2,         0,          0,       0,        0,          0,             0,            0,             0,           0,               512,               0,            0,            0,           0,       0,      2,             5,               "PML",    "2018-01-29 16:20:21.863", "2018-01-29 16:20:21.863");
/////*-676ms->*/insertSysStatement(conn, 168,     0,          1185874617, 4,    156524560,   64479149, 8848,       2,         8,          1,       678,      42,         0,             0,            0,             7,           0,               512,               0,            0,            0,           0,       0,      2,             6,               "PML",    "2018-01-29 16:20:21.866", "2018-01-29 16:20:22.543");
//			
////			insertSysSQLText(conn, SPID,    InstanceID, KPID,       ServerUserID, BatchID,    SequenceInBatch, SQLText);
//			insertSysSQLText(conn, 168,     0,          1185874617, 3,            8848,       1,               "declare @retvalue int exec @retvalue = sp_pml_reg_ps_user  'webadmin', 'vscuejnzub', '198310192953' select @retvalue");
//			
////			insertSysPlanText(conn, PlanID, InstanceID, SPID,    KPID,    BatchID,    ContextID, SequenceNumber, DBID, ProcedureID, DBName,   PlanText);
//		}
//		
//		int count = doSqlCapture(conn, null);
//		
//		truncSourceTables(conn);
//		return checkPcsAndDq(testName, debug, _pcs, _deferredQueueFor_sqlTextAndPlan, 
//				0, // expectedPcsSize, 
//				0, // expectedPcsStatementSize
//				0, // expectedPcsSqlSize
//				0, // expectedPcsPlanSize
//				1, // expectedDefQueue_Size, 
//				0, // expectedDefQueue_StatementRecords, 
////				1, // expectedDefQueue_SqlRecords,
////				0, // expectedDefQueue_PlanRecords,
//				0, // expectedDefQueue_PkAddedSize,
//				0, // expectedDefQueue_PkDiscardSize,
//				1, // expectedDefQueue_SqlPkSize,
//				0  // expectedDefQueue_PlanPkSize
//				);
//	}
//
//	/**
//	 * Simulate a precedure call, that has ...
//	 *  
//	 * This should produce:
//	 * - 1 PCS entry (1=statement, 1=sqlText, 1=planText)
//	 * - 3 entries in the deferred queue (0=sqlText, 0=planText)
//	 */
//	public boolean test_proc_maxm_pass2(DbxConnection conn, boolean debug)
//	{
//		String testName = new Object(){}.getClass().getEnclosingMethod().getName();
//		System.out.println("\n" + testName + ": START ------------------------------------------------------");
//		printInfo(debug, 0, testName+": START");
//		
//		{
//			PK pk = new PK(168, 1185874617, 8848);
//			
//			long execTime = 680;
//			int PhysicalReads = 0;
//			int LogicalReads  = 0;
////			insertSysStatement(conn, SPID,    InstanceID, KPID,       DBID, ProcedureID, PlanID,   BatchID,    ContextID, LineNumber, CpuTime, WaitTime, MemUsageKB, PhysicalReads, LogicalReads, PagesModified, PacketsSent, PacketsReceived, NetworkPacketSize, PlansAltered, RowsAffected, ErrorStatus, HashKey, SsqlId, ProcNestLevel, StatementNumber, DBName,   StartTime, EndTime);
//			insertSysStatement(conn, 168,     0,          1185874617, 4,    1036579750,  64479147, 8848,       1,         0,          0,       0,        38,         0,             0,            0,             0,           0,               512,               0,            0,            0,           0,       0,      1,             2,               "PML",    "2018-01-29 16:20:21.863", "2018-01-29 16:20:21.863");
//			insertSysStatement(conn, 168,     0,          1185874617, 4,    1036579750,  64479147, 8848,       1,         10,         0,       0,        40,         0,             0,            0,             0,           0,               512,               0,            1,            0,           0,       0,      1,             3,               "PML",    "2018-01-29 16:20:21.863", "2018-01-29 16:20:21.863");
///*-680ms->*/insertSysStatement(conn, 168,     0,          1185874617, 4,    1036579750,  64479147, 8848,       1,         11,         0,       0,        0,          0,             0,            0,             0,           0,               512,               0,            0,            0,           0,       0,      1,             4,               "PML",    "2018-01-29 16:20:21.863", "2018-01-29 16:20:22.543");
//			insertSysStatement(conn, 168,     0,          1185874617, 4,    1036579750,  64479147, 8848,       1,         12,         0,       0,        0,          0,             0,            0,             0,           0,               512,               0,            1,            0,           0,       0,      1,             7,               "PML",    "2018-01-29 16:20:22.543", "2018-01-29 16:20:22.543");
//			insertSysStatement(conn, 168,     0,          1185874617, 4,    156524560,   64479149, 8848,       2,         0,          0,       0,        0,          0,             0,            0,             0,           0,               512,               0,            0,            0,           0,       0,      2,             5,               "PML",    "2018-01-29 16:20:21.863", "2018-01-29 16:20:21.863");
///*-676ms->*/insertSysStatement(conn, 168,     0,          1185874617, 4,    156524560,   64479149, 8848,       2,         8,          1,       678,      42,         0,             0,            0,             7,           0,               512,               0,            0,            0,           0,       0,      2,             6,               "PML",    "2018-01-29 16:20:21.866", "2018-01-29 16:20:22.543");
//			
////			insertSysSQLText(conn, SPID,    InstanceID, KPID,       ServerUserID, BatchID,    SequenceInBatch, SQLText);
////			insertSysSQLText(conn, 168,     0,          1185874617, 3,            8848,       1,               "declare @retvalue int exec @retvalue = sp_pml_reg_ps_user  'webadmin', 'vscuejnzub', '198310192953' select @retvalue");
//			
////			insertSysPlanText(conn, PlanID, InstanceID, SPID,    KPID,    BatchID,    ContextID, SequenceNumber, DBID, ProcedureID, DBName,   PlanText);
//		}
//		
//		int count = doSqlCapture(conn, null);
//		
//		truncSourceTables(conn);
//		return checkPcsAndDq(testName, debug, _pcs, _deferredQueueFor_sqlTextAndPlan, 
//				3, // expectedPcsSize, 
//				2, // expectedPcsStatementSize
//				1, // expectedPcsSqlSize
//				0, // expectedPcsPlanSize
//				0, // expectedDefQueue_Size, 
//				0, // expectedDefQueue_StatementRecords, 
////				0, // expectedDefQueue_SqlRecords,
////				0, // expectedDefQueue_PlanRecords,
//				0, // expectedDefQueue_PkAddedSize,
//				0, // expectedDefQueue_PkDiscardSize,
//				0, // expectedDefQueue_SqlPkSize,
//				0  // expectedDefQueue_PlanPkSize
//				);
//	}
//
//	
//	
//	public static void main(String[] args)
//	{
//		Properties log4jProps = new Properties();
//		log4jProps.setProperty("log4j.rootLogger", "INFO, A1");
////		log4jProps.setProperty("log4j.rootLogger", "DEBUG, A1");
//		log4jProps.setProperty("log4j.appender.A1", "org.apache.log4j.ConsoleAppender");
//		log4jProps.setProperty("log4j.appender.A1.layout", "org.apache.log4j.PatternLayout");
//		log4jProps.setProperty("log4j.appender.A1.layout.ConversionPattern", "%d - %-5p - %-30c{1} - %m%n");
//		PropertyConfigurator.configure(log4jProps);
//		
//		boolean debug = true;
////		debug = false;
////		SqlCaptureBrokerAse_tester aseSqlCap;
//
//		try
//		{
//			DbxConnection conn = DbxConnection.createDbxConnection(DriverManager.getConnection("jdbc:h2:mem:test;DB_CLOSE_DELAY=-1;MODE=MSSQLSERVER;DATABASE_TO_UPPER=false", "user", "password"));
//
////			conn.dbExec("select getdate() as sampleTime");
////			conn.dbExec("select sampleTime=getdate()");
//			
//			createTables(conn);
////			insertSysSQLText(conn, 1, 99, 1, 99, 1, 1, "declare @xxx");
////			insertSysSQLText(conn, 1, 99, 1, 99, 1, 2, "select @xxx=id from dummy1");
////
////			selectFrom(conn, "monSysSQLText");
////			truncSourceTables(conn);
////			selectFrom(conn, "monSysSQLText");
//
//			Configuration conf = new Configuration();
//			conf.setProperty(PersistentCounterHandler.PROPKEY_sqlCap_saveStatement_gt_execTime,         50);
////			conf.setProperty(PersistentCounterHandler.PROPKEY_sqlCap_saveStatement_gt_physicalReads,    100);
////			conf.setProperty(PersistentCounterHandler.PROPKEY_sqlCap_saveStatement_gt_logicalReads,     1000);
//			conf.setProperty(PersistentCounterHandler.PROPKEY_sqlCap_saveStatement_gt_physicalReads,    -1);
//			conf.setProperty(PersistentCounterHandler.PROPKEY_sqlCap_saveStatement_gt_logicalReads,     -1);
//
//			
//			SqlCaptureBrokerAse_tester testObj = new SqlCaptureBrokerAse_tester(conf);
//			testObj._inTestCode       = true;
//			testObj._sampleStatements = true;
//			testObj._sampleSqlText    = true;
//			testObj._samplePlan       = true;
//
//			int sCnt = 0; // Success Count
//			int fCnt = 0; // Fail Count
//			boolean isOk;
//			String test;
//
//			//--------------------------------------
//			test =        "test_normal";
//			isOk = testObj.test_normal(conn, debug);
//			if (isOk) { sCnt++; System.out.println(test + " - OK"); } else { fCnt++; System.out.println(test + " - FAIL"); }
//
//			//--------------------------------------
//			test =        "test_short";
//			isOk = testObj.test_short(conn, debug);
//			if (isOk) { sCnt++; System.out.println(test + " - OK"); } else { fCnt++; System.out.println(test + " - FAIL"); }
//			
//			//--------------------------------------
//			test =        "test_waitfor_start";
//			isOk = testObj.test_waitfor_start(conn, debug);
//			if (isOk) { sCnt++; System.out.println(test + "-  OK"); } else { fCnt++; System.out.println(test + " - FAIL"); }
//
//			try { Thread.sleep(2000); } catch(InterruptedException ignore) {}
//
//			test =        "test_waitfor_end";
//			isOk = testObj.test_waitfor_end(conn, debug);
//			if (isOk) { sCnt++; System.out.println(test + " - OK"); } else { fCnt++; System.out.println(test + " - FAIL"); }
//
//			
//			//--------------------------------------
//			test =        "test_removeStatSql";
//			isOk = testObj.test_removeStatSql(conn, debug);
//			if (isOk) { sCnt++; System.out.println(test + " - OK"); } else { fCnt++; System.out.println(test + " - FAIL"); }
//
//			
//			//--------------------------------------
//			test =        "test_longRunning_pass1";
//			isOk = testObj.test_longRunning_pass1(conn, debug);
//			if (isOk) { sCnt++; System.out.println(test + " - OK"); } else { fCnt++; System.out.println(test + " - FAIL"); }
//
//			try { Thread.sleep(500); } catch(InterruptedException ignore) {}
//			testObj._deferredStorageThresholdFor_sqlTextAndPlan = 0;
//
//			test =        "test_longRunning_pass2";
//			isOk = testObj.test_longRunning_pass2(conn, debug);
//			if (isOk) { sCnt++; System.out.println(test + " - OK"); } else { fCnt++; System.out.println(test + " - FAIL"); }
//
//			testObj._deferredStorageThresholdFor_sqlTextAndPlan = 10;
//
//			
//			//--------------------------------------
//			test =        "test_proc_maxm_pass1";
//			isOk = testObj.test_proc_maxm_pass1(conn, debug);
//			if (isOk) { sCnt++; System.out.println(test + " - OK"); } else { fCnt++; System.out.println(test + " - FAIL"); }
//
//			try { Thread.sleep(500); } catch(InterruptedException ignore) {}
//
//			test =        "test_proc_maxm_pass2";
//			isOk = testObj.test_proc_maxm_pass2(conn, debug);
//			if (isOk) { sCnt++; System.out.println(test + " - OK"); } else { fCnt++; System.out.println(test + " - FAIL"); }
//
//			
//			//--------------------------------------
//			System.out.println();
//			System.out.println("-all-test-DONE- SuccessCount="+sCnt+", FailCount="+fCnt);
//		}
//		catch(SQLException ex)
//		{
//			ex.printStackTrace();
//		}
//		
//		
//	}
	
	

}
