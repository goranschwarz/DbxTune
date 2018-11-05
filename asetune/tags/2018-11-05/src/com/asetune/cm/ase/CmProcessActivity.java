package com.asetune.cm.ase;

import java.awt.event.MouseEvent;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;

import javax.naming.NameNotFoundException;

import org.apache.log4j.Logger;

import com.asetune.ICounterController;
import com.asetune.IGuiController;
import com.asetune.Version;
import com.asetune.cm.CmSettingsHelper;
import com.asetune.cm.CounterSample;
import com.asetune.cm.CounterSetTemplates;
import com.asetune.cm.CounterSetTemplates.Type;
import com.asetune.cm.CounterTableModel;
import com.asetune.cm.CountersModel;
import com.asetune.cm.ase.gui.CmProcessActivityPanel;
import com.asetune.config.dict.MonTablesDictionary;
import com.asetune.config.dict.MonTablesDictionaryManager;
import com.asetune.graph.TrendGraphDataPoint;
import com.asetune.graph.TrendGraphDataPoint.LabelType;
import com.asetune.gui.MainFrame;
import com.asetune.gui.TabularCntrPanel;
import com.asetune.sql.conn.DbxConnection;
import com.asetune.utils.Configuration;
import com.asetune.utils.StringUtil;
import com.asetune.utils.Ver;

/**
 * @author Goran Schwarz (goran_schwarz@hotmail.com)
 */
public class CmProcessActivity
extends CountersModel
{
	private static Logger        _logger          = Logger.getLogger(CmProcessActivity.class);
	private static final long    serialVersionUID = 1L;

	public static final String   CM_NAME          = CmProcessActivity.class.getSimpleName();
	public static final String   SHORT_NAME       = "Processes";
	public static final String   HTML_DESC        = 
		"<html>" +
		"<p>What SybasePIDs are doing what.</p>" +
		"<br>" +
		"<b>Tip:</b><br>" +
		"Sort by 'BatchIdDiff', will give you the one that executes the most SQL Batches.<br>" +
		"Or check 'WaitEventDesc' to find out when the SPID is waiting for." +
		"<br><br>" +
		"Table Background colors:" +
		"<ul>" +
		"    <li>DARK BEIGE          - SPID has Worker Processes Connected to it (Parent for a worker thread)</li>" +
		"    <li>BEIGE               - SPID is a Worker Processes</li>" +
		"    <li>YELLOW              - SPID is a System Processes</li>" +
		"    <li>EXTREME_LIGHT_GREEN - SPID is currently running a SQL Statement, Although it might be sleeping waiting for IO or something else.</li>" +
		"    <li>GREEN               - SPID is Executing(running) or are in the Run Queue Awaiting a time slot to Execute (runnable)</li>" +
		"    <li>LIGHT_GREEN         - SPID is Sending Network packets to it's client, soon it will probably go into running or runnable or finish.</li>" +
		"    <li>PINK                - SPID is Blocked by some other SPID that holds a Lock on a database object Table, Page or Row. This is the Lock Victim.</li>" +
		"    <li>RED                 - SPID is Blocking other SPID's from running, this SPID is Responsible or the Root Cause of a Blocking Lock.</li>" +
		"</ul>" +
		"</html>";

	public static final String   GROUP_NAME       = MainFrame.TCP_GROUP_SERVER;
	public static final String   GUI_ICON_FILE    = "images/"+CM_NAME+".png";

	public static final int      NEED_SRV_VERSION = 0;
	public static final int      NEED_CE_VERSION  = 0;

	public static final String[] MON_TABLES       = new String[] {"monProcessActivity", "monProcess", "sysprocesses", "monProcessNetIO", "monProcessStatement"};
	public static final String[] NEED_ROLES       = new String[] {"mon_role"};
	public static final String[] NEED_CONFIG      = new String[] {"enable monitoring=1", "object lockwait timing=1", "wait event timing=1", "per object statistics active=1"};

	public static final String[] PCT_COLUMNS      = new String[] {};
	public static final String[] DIFF_COLUMNS     = new String[] {
		"BatchIdDiff", "cpu", "physical_io", "CPUTime", "WaitTime", 
		"LogicalReads", "PhysicalReads", "PagesRead", "PhysicalWrites", "PagesWritten", 
		"TableAccesses","IndexAccesses", "TempDbObjects", 
		"ULCBytesWritten", "ULCFlushes", "ULCFlushFull", 
		"Transactions", "Commits", "Rollbacks", 
		"PacketsSent", "PacketsReceived", "BytesSent", "BytesReceived", 
		"WorkTables", "pssinfo_tempdb_pages_diff",
		"IOSize1Page", "IOSize2Pages", "IOSize4Pages", "IOSize8Pages",
		"AvgBytesPerSentPacket", "AvgBytesPerReceivedPacket",
//		"HeapMemoryInUseKB", "HeapMemoryReservedKB", "HeapMemoryAllocs", 
		"HeapMemoryAllocs", 
		"RowsAffected"
		};

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

		return new CmProcessActivity(counterController, guiController);
	}

	public CmProcessActivity(ICounterController counterController, IGuiController guiController)
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
		
		setDiffDissColumns( new String[] {"WaitTime"} );

		addTrendGraphs();
		
		CounterSetTemplates.register(this);
	}


	//------------------------------------------------------------
	// Implementation
	//------------------------------------------------------------
	private int _asePageSizeInBytes = -1; // dbxConn.getDbmsPageSizeInKb();
	
	@Override
	public boolean doSqlInit(DbxConnection conn)
	{
		// Call super
		boolean superRc = super.doSqlInit(conn);
		
		// Get page size...
		try
		{
			String pgSizeStr = conn.getDbmsPageSizeInKb();
			_asePageSizeInBytes = StringUtil.parseInt(pgSizeStr, -1);
			if (_asePageSizeInBytes > 0)
				_asePageSizeInBytes = _asePageSizeInBytes * 1024; 
			
			_logger.info("Initializing '"+getName()+"', the ASE Page size is: "+_asePageSizeInBytes);
		}
		catch(SQLException ex)
		{
			_logger.info("Problems getting ASE Page Size. Caught: "+ex);
		}

		return superRc;
	}
	@Override
	public void doSqlClose(DbxConnection conn)
	{
		// Call super
		super.doSqlClose(conn);

		// reset: Get page size...
		_asePageSizeInBytes = -1;
	}
	
	private static final String  PROP_PREFIX                        = CM_NAME;

	public static final String  PROPKEY_sample_systemThreads        = PROP_PREFIX + ".sample.systemThreads";
	public static final boolean DEFAULT_sample_systemThreads        = true;

	public static final String  PROPKEY_summaryGraph_discardDbxTune = PROP_PREFIX + ".summaryGraph.discardDbxTune";
	public static final boolean DEFAULT_summaryGraph_discardDbxTune = true;

	public static final String  PROPKEY_sample_sqlText              = PROP_PREFIX + ".sample.sqlText";
	public static final boolean DEFAULT_sample_sqlText              = false;

	@Override
	protected void registerDefaultValues()
	{
		super.registerDefaultValues();

		Configuration.registerDefaultValue(PROPKEY_sample_systemThreads, DEFAULT_sample_systemThreads);
	}

	private HashMap<Number,Object> _blockingSpids = new HashMap<Number,Object>(); // <(SPID)Integer> <null> indicator that the SPID is BLOCKING some other SPID

	public static final String GRAPH_NAME_CHKPT_HK    = "ChkptHkGraph";
	public static final String GRAPH_NAME_BATCH_COUNT = "BatchCountGraph";
	public static final String GRAPH_NAME_EXEC_TIME   = "ExecTimeGraph";
	public static final String GRAPH_NAME_EXEC_COUNT  = "ExecCountGraph";
	public static final String GRAPH_NAME_TEMPDB_SUM  = "TempdbSumGraph";

	private void addTrendGraphs()
	{
//		String[] labels_chkptHk   = new String[] { "Checkpoint Writes", "HK Wash Writes", "HK GC Writes", "HK Chores Writes" };
//		String[] labels_batch     = new String[] { "SQL Batch/Statement Count" };
//		String[] labels_execTime  = new String[] { "Max Active SQL Execution Time In Seconds" };
//		String[] labels_execCount = new String[] { "Active/Concurrent SQL Statement Execution Count" };
//		
//		addTrendGraphData(GRAPH_NAME_CHKPT_HK,    new TrendGraphDataPoint(GRAPH_NAME_CHKPT_HK,    labels_chkptHk,   LabelType.Static));
//		addTrendGraphData(GRAPH_NAME_BATCH_COUNT, new TrendGraphDataPoint(GRAPH_NAME_BATCH_COUNT, labels_batch,     LabelType.Static));
//		addTrendGraphData(GRAPH_NAME_EXEC_TIME,   new TrendGraphDataPoint(GRAPH_NAME_EXEC_TIME,   labels_execTime,  LabelType.Static));
//		addTrendGraphData(GRAPH_NAME_EXEC_COUNT,  new TrendGraphDataPoint(GRAPH_NAME_EXEC_COUNT,  labels_execCount, LabelType.Static));

		addTrendGraph(GRAPH_NAME_CHKPT_HK,
			"Checkpoint and HK Writes",                     // Menu CheckBox text
			"Checkpoint and Housekeeper Writes Per Second ("+GROUP_NAME+"->"+SHORT_NAME+")", // Label 
			new String[] { "Checkpoint Writes", "HK Wash Writes", "HK GC Writes", "HK Chores Writes" }, 
			LabelType.Static,
			TrendGraphDataPoint.Category.DISK,
			false, // is Percent Graph
			false, // visible at start
			0,     // graph is valid from Server Version. 0 = All Versions; >0 = Valid from this version and above 
			-1);   // minimum height

		// GRAPH
		addTrendGraph(GRAPH_NAME_BATCH_COUNT,
			"SQL Batch/Statement Count",                   // Menu CheckBox text
			"SQL Batches/Statements Processed Per Second ("+GROUP_NAME+"->"+SHORT_NAME+")", // Label 
			new String[] { "SQL Batch/Statement Count" }, 
			LabelType.Static,
			TrendGraphDataPoint.Category.OPERATIONS,
			false, // is Percent Graph
			false, // visible at start
			0,     // graph is valid from Server Version. 0 = All Versions; >0 = Valid from this version and above 
			-1);   // minimum height

		// GRAPH
		addTrendGraph(GRAPH_NAME_EXEC_TIME,
			"Max Active SQL Execution Time In Seconds",                   // Menu CheckBox text
			"Max Active SQL Execution Time In Seconds ("+GROUP_NAME+"->"+SHORT_NAME+")", // Label 
			new String[] { "Max Active SQL Execution Time In Seconds" }, 
			LabelType.Static,
			TrendGraphDataPoint.Category.WAITS,
			false, // is Percent Graph
			false, // visible at start
			0,     // graph is valid from Server Version. 0 = All Versions; >0 = Valid from this version and above 
			-1);   // minimum height

		// GRAPH
		addTrendGraph(GRAPH_NAME_EXEC_COUNT,
			"Active SQL Statement Execution Count",                   // Menu CheckBox text
			"Active SQL Statement Execution Count ("+GROUP_NAME+"->"+SHORT_NAME+")", // Label 
			new String[] { "Active/Concurrent SQL Statement Execution Count" }, 
			LabelType.Static,
			TrendGraphDataPoint.Category.OPERATIONS,
			false, // is Percent Graph
			false, // visible at start
			0,     // graph is valid from Server Version. 0 = All Versions; >0 = Valid from this version and above 
			-1);   // minimum height

		// GRAPH pssinfo(SP.spid, 'tempdb_pages')
		addTrendGraph(GRAPH_NAME_TEMPDB_SUM,
			"Tempdb Usage in MB, using pssinfo",                   // Menu CheckBox text
			"Tempdb Usage in MB, using pssinfo ("+GROUP_NAME+"->"+SHORT_NAME+")", // Label 
			new String[] { "Tempdb usage: sum(pssinfo_tempdb_pages) in MB" }, // pssinfo(SP.spid, 'tempdb_pages') 
			LabelType.Static,
			TrendGraphDataPoint.Category.SPACE,
			false, // is Percent Graph
			false, // visible at start
			0,     // graph is valid from Server Version. 0 = All Versions; >0 = Valid from this version and above 
			-1);   // minimum height

//		// if GUI
//		if (getGuiController() != null && getGuiController().hasGUI())
//		{
//			// GRAPH
//			TrendGraph tg = null;
//			tg = new TrendGraph(GRAPH_NAME_CHKPT_HK,
//				"Checkpoint and HK Writes",                     // Menu CheckBox text
//				"Checkpoint and Housekeeper Writes Per Second ("+GROUP_NAME+"->"+SHORT_NAME+")", // Label 
//				labels_chkptHk, 
//				false, // is Percent Graph
//				this, 
//				false, // visible at start
//				0,     // graph is valid from Server Version. 0 = All Versions; >0 = Valid from this version and above 
//				-1);   // minimum height
//			addTrendGraph(tg.getName(), tg, true);
//
//			// GRAPH
//			tg = null;
//			tg = new TrendGraph(GRAPH_NAME_BATCH_COUNT,
//				"SQL Batch/Statement Count",                   // Menu CheckBox text
//				"SQL Batches/Statements Processed Per Second ("+GROUP_NAME+"->"+SHORT_NAME+")", // Label 
//				labels_batch, 
//				false, // is Percent Graph
//				this, 
//				false, // visible at start
//				0,     // graph is valid from Server Version. 0 = All Versions; >0 = Valid from this version and above 
//				-1);   // minimum height
//			addTrendGraph(tg.getName(), tg, true);
//
//			// GRAPH
//			tg = null;
//			tg = new TrendGraph(GRAPH_NAME_EXEC_TIME,
//				"Max Active SQL Execution Time In Seconds",                   // Menu CheckBox text
//				"Max Active SQL Execution Time In Seconds ("+GROUP_NAME+"->"+SHORT_NAME+")", // Label 
//				labels_execTime, 
//				false, // is Percent Graph
//				this, 
//				false, // visible at start
//				0,     // graph is valid from Server Version. 0 = All Versions; >0 = Valid from this version and above 
//				-1);   // minimum height
//			addTrendGraph(tg.getName(), tg, true);
//
//			// GRAPH
//			tg = null;
//			tg = new TrendGraph(GRAPH_NAME_EXEC_COUNT,
//				"Active SQL Statement Execution Count",                   // Menu CheckBox text
//				"Active SQL Statement Execution Count ("+GROUP_NAME+"->"+SHORT_NAME+")", // Label 
//				labels_execCount, 
//				false, // is Percent Graph
//				this, 
//				false, // visible at start
//				0,     // graph is valid from Server Version. 0 = All Versions; >0 = Valid from this version and above 
//				-1);   // minimum height
//			addTrendGraph(tg.getName(), tg, true);
//		}
	}
	// GRAPH

	@Override
	protected TabularCntrPanel createGui()
	{
		return new CmProcessActivityPanel(this);
	}

	@Override
	public String[] getDependsOnConfigForVersion(Connection conn, int srvVersion, boolean isClusterEnabled)
	{
		return NEED_CONFIG;
	}

	@Override
	public List<String> getPkForVersion(Connection conn, int srvVersion, boolean isClusterEnabled)
	{
		List <String> pkCols = new LinkedList<String>();

		if (isClusterEnabled)
			pkCols.add("InstanceID");

		pkCols.add("SPID");
		pkCols.add("KPID");
		
		return pkCols;
	}

	@Override
	public String getSqlForVersion(Connection conn, int aseVersion, boolean isClusterEnabled)
	{
		Configuration conf = Configuration.getCombinedConfiguration();
		boolean sample_systemThreads  = conf.getBooleanProperty(PROPKEY_sample_systemThreads, DEFAULT_sample_systemThreads);
		boolean sample_sqlText        = conf.getBooleanProperty(PROPKEY_sample_sqlText,       DEFAULT_sample_sqlText);

		// Should we sample SYSTEM SPID's
		String sql_sample_systemThreads = "--and SP.suid > 0 -- Property: "+PROPKEY_sample_systemThreads+" is "+sample_systemThreads+". \n";
		if ( ! sample_systemThreads )
			sql_sample_systemThreads = "  and SP.suid > 0 -- Property: "+PROPKEY_sample_systemThreads+" is "+sample_systemThreads+". \n";

		// If not ASE 16, do not sample SQL Text
		if (aseVersion < Ver.ver(16,0,0, 2)) // 16.0 PL1 didn't have 
			sample_sqlText = false;

		String cols1, cols2, cols3;
		cols1 = cols2 = cols3 = "";

		String optGoalPlan = "";
		if (aseVersion >= Ver.ver(15,0,2))
		{
			optGoalPlan = "plan '(use optgoal allrows_dss)' \n";
		}

		if (isClusterEnabled)
		{
			cols1 += "MP.InstanceID, ";
		}

		String preDropTempTables =
			"/*------ drop tempdb objects if we failed doing that in previous execution -------*/ \n" +
			"if ((select object_id('#monProcessActivity'))  is not null) drop table #monProcessActivity  \n" +
			"if ((select object_id('#monProcess'))          is not null) drop table #monProcess          \n" +
			"if ((select object_id('#monProcessNetIO'))     is not null) drop table #monProcessNetIO     \n" +
			"if ((select object_id('#monProcessStatement')) is not null) drop table #monProcessStatement \n" +
			"go \n" +
			"\n";

		String createTempTables =
			"/*------ Snapshot, the monXXX tables, hopefully this is less expensive than doing the join via CIS -------*/ \n" +
			"select * into #monProcessActivity  from master..monProcessActivity  \n" +
			"select * into #monProcess          from master..monProcess          \n" +
			"select * into #monProcessNetIO     from master..monProcessNetIO     \n" +
			"select * into #monProcessStatement from master..monProcessStatement \n" +
			"go \n" +
			"\n";

		String dropTempTables = 
			"\n" +
			"/*------ drop tempdb objects -------*/ \n" +
			"drop table #monProcessActivity  \n" +
			"drop table #monProcess          \n" +
			"drop table #monProcessNetIO     \n" +
			"drop table #monProcessStatement \n" +
			"\n";

		addDropTempTable("#monProcessActivity");
		addDropTempTable("#monProcess");
		addDropTempTable("#monProcessNetIO");
		addDropTempTable("#monProcessStatement");

		// ASE 12.5.4... and 15.0.2  (15.0 do NOT have this counter)
		String RowsAffected = "";
		String ClientRemotePort = "";
		String sp_1254 = "";
		if (aseVersion >= Ver.ver(15,0,2) || (aseVersion >= Ver.ver(12,5,4) && aseVersion < Ver.ver(15,0)) )
		{
			RowsAffected     = "RowsAffected = isnull(ST.RowsAffected, 0), ";
			ClientRemotePort = "ClientRemotePort = CASE WHEN SP.suid = 0 THEN null ELSE convert(varchar(15), pssinfo(SP.spid, 'ipport')) END, \n";
			sp_1254 = "  ";
		}

		// ASE 15.7.0
		String HeapMemoryInUseKB    = ""; // Amount of heap memory currently used by the process (KB)
		String HeapMemoryUsedHWM_KB = ""; // High-water mark of heap memory used by the process (KB)
		String HeapMemoryReservedKB = ""; // Amount of heap memory currently reserved by the process (KB)
		String HeapMemoryAllocs     = ""; // Number of times the process allocated heap memory
		String nl_1570              = ""; // NL for this section
		String sp_1570              = ""; // column Space padding for this section
		if (aseVersion >= Ver.ver(15,7,0))
		{
			HeapMemoryInUseKB    = "A.HeapMemoryInUseKB, ";
			HeapMemoryUsedHWM_KB = "A.HeapMemoryUsedHWM_KB, ";
			HeapMemoryReservedKB = "A.HeapMemoryReservedKB, ";
			HeapMemoryAllocs     = "A.HeapMemoryAllocs, ";
			nl_1570              = "\n";
			sp_1570              = "  ";
		}

		// ASE 15.7.0 ESD#2
		String IOSize1Page        = ""; // Number of 1 page physical reads performed by the process
		String IOSize2Pages       = ""; // Number of 2 pages physical reads performed for the process
		String IOSize4Pages       = ""; // Number of 4 pages physical reads performed for the process
		String IOSize8Pages       = ""; // Number of 8 pages physical reads performed for the process
		String nl_15702           = ""; // NL for this section
		String sp_15702           = ""; // column Space padding for this section
		if (aseVersion >= Ver.ver(15,7,0,2))
		{
			IOSize1Page        = "A.IOSize1Page, ";
			IOSize2Pages       = "A.IOSize2Pages, ";
			IOSize4Pages       = "A.IOSize4Pages, ";
			IOSize8Pages       = "A.IOSize8Pages, ";
			nl_15702           = "\n";
			sp_15702           = "  ";
		}

		// ASE 16.0
		String SqlText             = "";
		String HasSqlText          = "";
		String ClientDriverVersion = ""; // The version of the connectivity driver used by the client program
		String nl_16000           = ""; // NL for this section
		String sp_16000           = ""; // column Space padding for this section
		if (aseVersion >= Ver.ver(16,0,0, 2)) // 16.0 PL1 did not have query_text()... so lets use 16.0 SP2 as base instead
		{
			if (sample_sqlText)
			{
    			HasSqlText      = "HasSqlText = convert(bit, 0), ";
    			SqlText         = "SqlText = CASE WHEN (SP.suid > 0 AND MP.WaitEventID != 250) THEN query_text(MP.SPID) ELSE null END, ";
			}
			else
			{
    			HasSqlText      = "HasSqlText = convert(bit, 0), ";
    			SqlText         = "SqlText = convert(text, null), ";
			}
			ClientDriverVersion = "MP.ClientDriverVersion, ";
			nl_16000            = "\n";
			sp_16000            = "  ";
		}

		// ASE 16.0 SP3
		String QueryOptimizationTime       = "";
		String ase160_sp3_nl               = "";
		if (aseVersion >= Ver.ver(16,0,0, 3)) // 16.0 SP3
		{
			QueryOptimizationTime       = "  ST.QueryOptimizationTime, ";
			ase160_sp3_nl               = "\n";
		}
		
		cols1+=" MP.FamilyID, MP.SPID, MP.KPID, MP.NumChildren, \n"
			+ "  SP.status, MP.WaitEventID, \n"
			+ "  WaitClassDesc=convert(varchar(120),''), " // value will be replaced in method localCalculation()
			+ "  WaitEventDesc=convert(varchar(120),''), " // value will be replaced in method localCalculation()
			+ "  MP.SecondsWaiting, MP.BlockingSPID, \n"
			+ "  StatementStartTime = ST.StartTime, \n"
//			+ "  StatementExecInMs = datediff(ms, ST.StartTime, getdate()), \n"
			+ "  StatementExecInMs = CASE WHEN datediff(day, ST.StartTime, getdate()) >= 24 THEN -1 ELSE  datediff(ms, ST.StartTime, getdate()) END, \n"
			+ QueryOptimizationTime + ase160_sp3_nl
			+ "  MP.Command, SP.tran_name, "+HasSqlText+"\n"
			+ "  MP.BatchID, BatchIdDiff=convert(int,MP.BatchID), \n" // BatchIdDiff diff calculated
			+ "  procName = isnull(object_name(SP.id, SP.dbid), object_name(SP.id, 2)), \n"
			+ "  SP.stmtnum, SP.linenum, \n"
			+ sp_16000 + ClientDriverVersion + nl_16000 
			+ "  MP.Application, SP.clientname, SP.clienthostname, SP.clientapplname, "
			+ "  SP.hostname, SP.ipaddr, SP.hostprocess, \n"
			+ sp_1254 + ClientRemotePort
			+ "  MP.DBName, MP.Login, SP.suid, MP.SecondsConnected, \n"
			+ "  SP.loggedindatetime, SP.cpu, SP.physical_io, SP.memusage, \n"
			+ "  A.CPUTime, A.WaitTime, " + RowsAffected + "A.LogicalReads, \n"
			+ "  A.PhysicalReads, A.PagesRead, A.PhysicalWrites, A.PagesWritten, \n"
			+ sp_15702 + IOSize1Page + IOSize2Pages + IOSize4Pages + IOSize8Pages + nl_15702;
		cols2 += "";
		if (aseVersion >= Ver.ver(12,5,2))
		{
			cols2+="  A.WorkTables,  \n";
		}
		if (aseVersion >= Ver.ver(15,0,2) || (aseVersion >= Ver.ver(12,5,4) && aseVersion < Ver.ver(15,0)) )
		{
			cols2+="  tempdb_name = db_name(tempdb_id(SP.spid)), \n";
			cols2+="  pssinfo_tempdb_pages      = convert(int, pssinfo(SP.spid, 'tempdb_pages')), \n";
			cols2+="  pssinfo_tempdb_pages_diff = convert(int, pssinfo(SP.spid, 'tempdb_pages')), \n";
		}
		if (aseVersion >= Ver.ver(15,0,2,5))
		{
			cols2+="  N.NetworkEngineNumber, MP.ServerUserID, \n";
		}
		cols3+=""
			+ "  A.TableAccesses, A.IndexAccesses, A.TempDbObjects, \n"
			+ "  A.ULCBytesWritten, A.ULCFlushes, A.ULCFlushFull, \n"
			+ "  A.Transactions, A.Commits, A.Rollbacks, \n"
			+ "  MP.EngineNumber, MP.Priority, \n"
			+ sp_1570 + HeapMemoryInUseKB + HeapMemoryUsedHWM_KB + HeapMemoryReservedKB + HeapMemoryAllocs + nl_1570
			+ "  A.MemUsageKB, N.NetworkPacketSize, \n"
			+ "  N.BytesSent, N.PacketsSent, \n"
			+ "  AvgBytesPerSentPacket     = CASE WHEN N.PacketsSent     > 0 THEN N.BytesSent     / N.PacketsSent     ELSE 0 END, \n" 
			+ "  N.BytesReceived, N.PacketsReceived, \n"
			+ "  AvgBytesPerReceivedPacket = CASE WHEN N.PacketsReceived > 0 THEN N.BytesReceived / N.PacketsReceived ELSE 0 END, \n" 
			+ "  MP.ExecutionClass, MP.EngineGroupName, "
			+ nl_16000 + SqlText;
		cols3 = StringUtil.removeLastComma(cols3);

		String sql = 
			"/*------ SQL to get data -------*/ \n" +
			"select " + cols1 + cols2 + cols3 + "\n" +
			"from #monProcessActivity A, #monProcess MP, master..sysprocesses SP, #monProcessNetIO N, #monProcessStatement ST \n" +
			"where MP.KPID  = SP.kpid \n" +
			"  and MP.KPID  = A.KPID \n" +
			"  and MP.KPID  = N.KPID \n" +
			"  and MP.KPID *= ST.KPID \n" + // Outer Joint
			sql_sample_systemThreads;

		if (isClusterEnabled)
			sql +=
				"  and MP.InstanceID = SP.instanceid \n" + // FIXME: Need to check if this is working...
				"  and MP.InstanceID = A.InstanceID \n" +
				"  and MP.InstanceID = N.InstanceID \n";

		sql += "order by MP.FamilyID, MP.SPID \n" + 
		       optGoalPlan;

		return preDropTempTables + createTempTables + sql + dropTempTables;
	}

	/** Used by the: Create 'Offline Session' Wizard */
	@Override
	public List<CmSettingsHelper> getLocalSettings()
	{
		Configuration conf = Configuration.getCombinedConfiguration();
		List<CmSettingsHelper> list = new ArrayList<>();
		
		list.add(new CmSettingsHelper("Show System Processes",                   PROPKEY_sample_systemThreads ,        Boolean.class, conf.getBooleanProperty(PROPKEY_sample_systemThreads        , DEFAULT_sample_systemThreads        ), DEFAULT_sample_systemThreads       , CmProcessActivityPanel.TOOLTIP_sample_systemThreads        ));
		list.add(new CmSettingsHelper("Discard AseTune Activity in TrendGraphs", PROPKEY_summaryGraph_discardDbxTune , Boolean.class, conf.getBooleanProperty(PROPKEY_summaryGraph_discardDbxTune , DEFAULT_summaryGraph_discardDbxTune ), DEFAULT_summaryGraph_discardDbxTune, CmProcessActivityPanel.TOOLTIP_summaryGraph_discardDbxTune ));
		list.add(new CmSettingsHelper("Get SQL Text from Active SPID's",         PROPKEY_sample_sqlText ,              Boolean.class, conf.getBooleanProperty(PROPKEY_sample_sqlText              , DEFAULT_sample_sqlText              ), DEFAULT_sample_sqlText             , CmProcessActivityPanel.TOOLTIP_sample_sqlText              ));

		return list;
	}


	@Override
	public void addMonTableDictForVersion(Connection conn, int aseVersion, boolean isClusterEnabled)
	{
		try 
		{
			MonTablesDictionary mtd = MonTablesDictionaryManager.getInstance();

			mtd.addColumn("monProcessStatement",  "StatementStartTime", 
			                                              "<html>" +
			                                                   "Start time for the SPID's Currently Running Statement.<br>" +
			                                                   "If this is <code>NULL</code> this SPID do not have any statement that is currently executing..<br>" +
			                                                   "<b>Formula</b>: column 'StartTime' from table 'monProcessStatement'.<br>" +
			                                              "</html>");
			mtd.addColumn("monProcessStatement",  "StatementExecInMs",  
			                                              "<html>" +
			                                                   "For how many milli seconds has the Currently Running Statement been executed for.<br>" +
			                                                   "If this is 0 this SPID do not have any statement that is currently executing.<br>" +
			                                                   "<b>Formula</b>: datediff(ms, ST.StartTime, getdate())<br>" +
			                                              "</html>");
			mtd.addColumn("monProcess", "BatchIdDiff", 
			                                              "<html>" +
			                                                   "Same as the column 'BatchId', but it's only the difference from previous sample.<br>" +
			                                                   "This is usable to determine how many SQL Statements this SPID has been issued.<br>" +
			                                                   "<b>Formula</b>: column 'BatchId' from table 'monProcess'.<br>" +
			                                              "</html>");
			mtd.addColumn("monProcess", "SqlText", 
			                                              "<html>" +
			                                                   "SQL Text that is executing for this SPID.<br>" +
			                                                   "<b>Formula</b>: select query_text(MP.SPID).<br>" +
			                                              "</html>");
			mtd.addColumn("monProcess", "HasSqlText", 
			                                              "<html>" +
			                                                   "SQL Text that is executing for this SPID.<br>" +
			                                                   "<b>Formula</b>: select query_text(MP.SPID).<br>" +
			                                              "</html>");
			mtd.addColumn("sysprocesses", "loggedindatetime", 
			                                              "<html>" +
			                                                   "Date when this SPID connect/login to the ASE..<br>" +
			                                                   "<b>Formula</b>: column 'loggedindatetime' from table 'sysprocesses'.<br>" +
			                                              "</html>");
			mtd.addColumn("monProcessStatement", "AvgBytesPerSentPacket", 
					"<html>" +
							"Average bytes per packet<br>" +
							"<b>Formula</b>: diff.BytesSent / diff.PacketsSent<br>" +
							"<b>Note</b>: This is <b>not</b> rate calculated, it's the diff values even in the <i>rate</i> view<br>" +
					"</html>");

			mtd.addColumn("monProcessStatement", "AvgBytesPerReceivedPacket", 
					"<html>" +
							"Average bytes per packet<br>" +
							"<b>Formula</b>: diff.BytesReceived / diff.PacketsReceived<br>" +
							"<b>Note</b>: This is <b>not</b> rate calculated, it's the diff values even in the <i>rate</i> view<br>" +
					"</html>");

			mtd.addColumn("sysprocesses", "ClientRemotePort", 
                    "<html>" +
                         "The port number at the clients machine.<br>" +
                         "<b>Formula</b>: pssinfo(SP.spid, 'ipport').<br>" +
                    "</html>");
		}
		catch (NameNotFoundException e) {/*ignore*/}
	}

	/** 
	 * Fill in the WaitEventDesc column with data from
	 * MonTableDictionary.. transforms a WaitEventId -> text description
	 * This so we do not have to do a subselect in the query that gets data
	 * doing it this way, means better performance, since the values are cached locally in memory
	 */
	@Override
	public void localCalculation(CounterSample prevSample, CounterSample newSample, CounterSample diffData)
	{
		// Where are various columns located in the Vector 
		int pos_WaitEventID = -1, pos_WaitEventDesc = -1, pos_WaitClassDesc = -1, pos_BlockingSPID = -1;
		int pos_SqlText = -1, pos_HasSqlText = -1;
		int pos_AvgBytesPerSentPacket = -1, pos_AvgBytesPerReceivedPacket = -1;
		int pos_BytesSent             = -1, pos_PacketsSent               = -1;
		int pos_BytesReceived         = -1, pos_PacketsReceived           = -1;

		int waitEventID = 0;
		String waitEventDesc = "";
		String waitClassDesc = "";
		CounterSample counters = diffData;
//		CounterSample counters = chosenData;
	
		MonTablesDictionary mtd = MonTablesDictionaryManager.getInstance();
		if (mtd == null)
			return;

		if (counters == null)
			return;

		// Reset the blockingSpids Map
		_blockingSpids.clear();
		
		// put the pointer to the Map in the Client Property of the JTable, which should be visible for various places
		if (getTabPanel() != null)
			getTabPanel().putTableClientProperty("blockingSpidMap", _blockingSpids);

		// Find column Id's
		List<String> colNames = counters.getColNames();
		if (colNames==null) return;

		for (int colId=0; colId < colNames.size(); colId++) 
		{
			String colName = colNames.get(colId);
			if      (colName.equals("WaitEventID"))               pos_WaitEventID               = colId;
			else if (colName.equals("WaitEventDesc"))             pos_WaitEventDesc             = colId;
			else if (colName.equals("WaitClassDesc"))             pos_WaitClassDesc             = colId;
			else if (colName.equals("BlockingSPID"))              pos_BlockingSPID              = colId;
			else if (colName.equals("SqlText"))                   pos_SqlText                   = colId;
			else if (colName.equals("HasSqlText"))                pos_HasSqlText                = colId;
			else if (colName.equals("AvgBytesPerSentPacket"))     pos_AvgBytesPerSentPacket     = colId;
			else if (colName.equals("AvgBytesPerReceivedPacket")) pos_AvgBytesPerReceivedPacket = colId;
			else if (colName.equals("BytesSent"))                 pos_BytesSent                 = colId;
			else if (colName.equals("PacketsSent"))               pos_PacketsSent               = colId;
			else if (colName.equals("BytesReceived"))             pos_BytesReceived             = colId;
			else if (colName.equals("PacketsReceived"))           pos_PacketsReceived           = colId;
		}

		if (pos_WaitEventID < 0 || pos_WaitEventDesc < 0 || pos_WaitClassDesc < 0)
		{
			_logger.debug("Can't find the position for columns ('WaitEventID'="+pos_WaitEventID+", 'WaitEventDesc'="+pos_WaitEventDesc+", 'WaitClassDesc'="+pos_WaitClassDesc+")");
			return;
		}
		
		if (pos_BlockingSPID < 0)
		{
			_logger.debug("Can't find the position for column ('BlockingSPID'="+pos_BlockingSPID+")");
			return;
		}
		
		if (pos_AvgBytesPerSentPacket < 0 || pos_AvgBytesPerReceivedPacket < 0 || pos_BytesSent < 0 || pos_PacketsSent < 0 || pos_BytesReceived < 0 || pos_PacketsReceived < 0)
		{
			_logger.debug("Can't find the position for column ('pos_AvgBytesPerSentPacket'="+pos_AvgBytesPerSentPacket+", pos_AvgBytesPerReceivedPacket="+pos_AvgBytesPerReceivedPacket+", pos_BytesSent="+pos_BytesSent+", pos_PacketsSent="+pos_PacketsSent+", pos_BytesReceived="+pos_BytesReceived+", pos_PacketsReceived="+pos_PacketsReceived+")");
			return;
		}
		
		// Loop on all diffData rows
		for (int rowId=0; rowId < counters.getRowCount(); rowId++) 
		{
//			Vector row = (Vector)counters.rows.get(rowId);
//			Object o = row.get(pos_WaitEventID);
			Object o_waitEventId  = counters.getValueAt(rowId, pos_WaitEventID);
			Object o_blockingSpid = counters.getValueAt(rowId, pos_BlockingSPID);

			if (o_waitEventId instanceof Number)
			{
				waitEventID	  = ((Number)o_waitEventId).intValue();

				if (mtd.hasWaitEventDescription(waitEventID))
				{
					waitEventDesc = mtd.getWaitEventDescription(waitEventID);
					waitClassDesc = mtd.getWaitEventClassDescription(waitEventID);
				}
				else
				{
					waitEventDesc = "";
					waitClassDesc = "";
				}

				//row.set( pos_WaitEventDesc, waitEventDesc);
				counters.setValueAt(waitEventDesc, rowId, pos_WaitEventDesc);
				counters.setValueAt(waitClassDesc, rowId, pos_WaitClassDesc);
			}

			// SQL Text
			if (pos_SqlText >= 0 && pos_HasSqlText >= 0)
			{
				Object o_sqlText = counters.getValueAt(rowId, pos_SqlText);
				if (o_sqlText != null && !o_sqlText.equals(""))
				{
					counters.setValueAt(new Boolean(true), rowId, pos_HasSqlText);
				}
			}

			// Add any blocking SPIDs to the MAP
			if (o_blockingSpid instanceof Number)
			{
				if (o_blockingSpid != null && ((Number)o_blockingSpid).intValue() != 0 )
					_blockingSpids.put((Number)o_blockingSpid, null);
			}

			// AvgBytesPerSentPacket
			Object o_BytesSent       = counters.getValueAt(rowId, pos_BytesSent);
			Object o_PacketsSent     = counters.getValueAt(rowId, pos_PacketsSent);
			if (o_BytesSent instanceof Number && o_PacketsSent instanceof Number)
			{
				int bytes   = ((Number)o_BytesSent)  .intValue();
				int packets = ((Number)o_PacketsSent).intValue();
				
				counters.setValueAt(new Integer( (packets > 0 ? bytes/packets : 0) ), rowId, pos_AvgBytesPerSentPacket);
			}

			// AvgBytesPerReceivedPacket
			Object o_BytesReceived   = counters.getValueAt(rowId, pos_BytesReceived);
			Object o_PacketsReceived = counters.getValueAt(rowId, pos_PacketsReceived);
			if (o_BytesReceived instanceof Number && o_PacketsReceived instanceof Number)
			{
				int bytes   = ((Number)o_BytesReceived)  .intValue();
				int packets = ((Number)o_PacketsReceived).intValue();
				
				counters.setValueAt(new Integer( (packets > 0 ? bytes/packets : 0) ), rowId, pos_AvgBytesPerReceivedPacket);
			}
		}
	}

	/**
	 * Local adjustments to the rate values 
	 * Use: DIFF values for 
	 *         - AvgBytesPerSentPacket
	 *         - AvgBytesPerReceivedPacket
	 */
	@Override
	public void localCalculationRatePerSec(CounterSample rateData, CounterSample diffData)
	{
		int pos_AvgBytesPerSentPacket = -1, pos_AvgBytesPerReceivedPacket = -1;

		// Find column Id's
		List<String> colNames = rateData.getColNames();
		if (colNames==null) 
			return;

		for (int colId=0; colId < colNames.size(); colId++) 
		{
			String colName = colNames.get(colId);
			if      (colName.equals("AvgBytesPerSentPacket"))     pos_AvgBytesPerSentPacket     = colId;
			else if (colName.equals("AvgBytesPerReceivedPacket")) pos_AvgBytesPerReceivedPacket = colId;
		}

		if (pos_AvgBytesPerSentPacket < 0 || pos_AvgBytesPerReceivedPacket < 0)
		{
			_logger.debug("Can't find the position for column ('pos_AvgBytesPerSentPacket'="+pos_AvgBytesPerSentPacket+", pos_AvgBytesPerReceivedPacket="+pos_AvgBytesPerReceivedPacket+")");
			return;
		}
		
		// Loop on all diffData rows
		for (int rowId=0; rowId < rateData.getRowCount(); rowId++) 
		{
			// AvgBytesPerSentPacket - set to DIFF value
			Object o_diffAvgBytesPerSentPacket = diffData.getValueAt(rowId, pos_AvgBytesPerSentPacket);
			if (o_diffAvgBytesPerSentPacket instanceof Number)
				rateData.setValueAt(((Number) o_diffAvgBytesPerSentPacket).doubleValue(), rowId, pos_AvgBytesPerSentPacket);

			// AvgBytesPerReceivedPacket - set to DIFF value
			Object o_diffAvgBytesPerReceivedPacket = diffData.getValueAt(rowId, pos_AvgBytesPerReceivedPacket);
			if (o_diffAvgBytesPerReceivedPacket instanceof Number)
				rateData.setValueAt(((Number) o_diffAvgBytesPerReceivedPacket).doubleValue(), rowId, pos_AvgBytesPerReceivedPacket);
		}
	}

	@Override
	public String getToolTipTextOnTableCell(MouseEvent e, String colName, Object cellValue, int modelRow, int modelCol) 
	{
		// MON SQL TEXT
		if ("HasSqlText".equals(colName))
		{
			// Find 'MonSqlText' column, is so get it and set it as the tool tip
			int pos_SqlText = findColumn("SqlText");
			if (pos_SqlText > 0)
			{
				Object cellVal = getValueAt(modelRow, pos_SqlText);
				if (cellVal instanceof String)
				{
					return toHtmlString((String) cellVal, true);
					//return (String) cellVal;
				}
			}
		}
		if ("SqlText".equals(colName))
		{
			return cellValue == null ? null : cellValue.toString();
		}
		
		return super.getToolTipTextOnTableCell(e, colName, cellValue, modelRow, modelCol);
	}
	/** add HTML around the string, and translate linebreaks into <br> */
	private String toHtmlString(String in, boolean breakLines)
	{
		String str = in;
		str = str.replace("<", "&lt;");
		str = str.replace(">", "&gt;");
		if (breakLines)
			str = StringUtil.makeApproxLineBreak(in, 150, 5, "\n");
		str = str.replaceAll("\\n", "<br>");

		return "<html><pre>" + str + "</pre></html>";
	}

	@Override
	public void updateGraphData(TrendGraphDataPoint tgdp)
	{
		//
		// Get column 'PhysicalWrites' for the 4 rows, which has the column 
		// Command = 'CHECKPOINT SLEEP', 'HK WASH', 'HK GC', 'HK CHORES'
		//
		// FIXME: this will probably NOT work on Cluster Edition...
		//        Should we just check the local InstanceID or should we do "sum"
		//
		if (GRAPH_NAME_CHKPT_HK.equals(tgdp.getName()))
		{
			int pos_Command = -1, pos_PhysicalWrites = -1;

			CounterTableModel counters = getCounterData(DATA_RATE);
			if (counters == null) 
				return;

			// Find column Id's
			List<String> colNames = counters.getColNames();
			if (colNames == null) 
				return;

			for (int colId=0; colId < colNames.size(); colId++) 
			{
				String colName = colNames.get(colId);
				if      (colName.equals("Command"))        pos_Command        = colId;
				else if (colName.equals("PhysicalWrites")) pos_PhysicalWrites = colId;

				// No need to continue, we got all our columns
				if (pos_Command >= 0 && pos_PhysicalWrites >= 0)
					break;
			}

			if (pos_Command < 0 || pos_PhysicalWrites < 0)
			{
				_logger.debug("Can't find the position for column ('Command'="+pos_Command+", 'PhysicalWrites'="+pos_PhysicalWrites+")");
				return;
			}
			
			Object o_CheckpointWrite = null;
			Object o_HkWashWrite     = null;
			Object o_HkGcWrite       = null;
			Object o_HkChoresWrite   = null;

			// Loop rows
			for (int rowId=0; rowId < counters.getRowCount(); rowId++)
			{
				Object o_Command = counters.getValueAt(rowId, pos_Command);

				if (o_Command instanceof String)
				{
					String command = (String)o_Command;

					if (command.startsWith("CHECKPOINT"))
						o_CheckpointWrite = counters.getValueAt(rowId, pos_PhysicalWrites);

					if (command.startsWith("HK WASH"))
						o_HkWashWrite = counters.getValueAt(rowId, pos_PhysicalWrites);

					if (command.startsWith("HK GC"))
						o_HkGcWrite = counters.getValueAt(rowId, pos_PhysicalWrites);

					if (command.startsWith("HK CHORES"))
						o_HkChoresWrite = counters.getValueAt(rowId, pos_PhysicalWrites);
				}
				// if we have found all of our rows/values we need, end the loop
				if (o_CheckpointWrite != null && o_HkWashWrite != null && o_HkGcWrite != null && o_HkChoresWrite != null)
					break;
			} // end loop rows

			// Now fix the values into a structure and send it of to the graph
			Double[] arr = new Double[4];

			arr[0] = new Double(o_CheckpointWrite == null ? "0" : o_CheckpointWrite.toString());
			arr[1] = new Double(o_HkWashWrite     == null ? "0" : o_HkWashWrite    .toString());
			arr[2] = new Double(o_HkGcWrite       == null ? "0" : o_HkGcWrite      .toString());
			arr[3] = new Double(o_HkChoresWrite   == null ? "0" : o_HkChoresWrite  .toString());
			_logger.debug("updateGraphData(ChkptHkGraph): o_CheckpointWrite='"+o_CheckpointWrite+"', o_HkWashWrite='"+o_HkWashWrite+"', o_HkGcWrite='"+o_HkGcWrite+"', o_HkChoresWrite='"+o_HkChoresWrite+"'.");

			// Set the values
			tgdp.setDataPoint(this.getTimestamp(), arr);
		}

		if (GRAPH_NAME_BATCH_COUNT.equals(tgdp.getName()))
		{
			int pos_BatchIdDiff = -1;
			int pos_Application = -1;

			boolean discardAppnameDbxTune = Configuration.getCombinedConfiguration().getBooleanProperty(PROPKEY_summaryGraph_discardDbxTune, DEFAULT_summaryGraph_discardDbxTune);

			CounterTableModel counters = getCounterData(DATA_RATE);
			if (counters == null) 
				return;

			// Find column Id's
			List<String> colNames = counters.getColNames();
			if (colNames == null) 
				return;

			for (int colId=0; colId < colNames.size(); colId++) 
			{
				String colName = colNames.get(colId);
				if (colName.equals("BatchIdDiff")) pos_BatchIdDiff = colId;
				if (colName.equals("Application")) pos_Application = colId;

				// No need to continue, we got all our columns
				if (pos_BatchIdDiff >= 0 && pos_Application >= 0)
					break;
			}

			if (pos_BatchIdDiff < 0)
			{
				_logger.debug("Can't find the position for column ('BatchIdDiff'="+pos_BatchIdDiff+")");
				return;
			}
			if (pos_Application < 0)
			{
				_logger.debug("Can't find the position for column ('Application'="+pos_Application+")");
				return;
			}
			
			
			double BatchIdDiff_sum = 0;

			// Loop rows
			for (int rowId=0; rowId < counters.getRowCount(); rowId++)
			{
				Object o_BatchIdDiff = counters.getValueAt(rowId, pos_BatchIdDiff);
				Object o_Application = counters.getValueAt(rowId, pos_Application);

				// discard aApplication name 'AseTune'
				if (discardAppnameDbxTune && o_Application != null && o_Application instanceof String)
				{
					if ( ((String)o_Application).startsWith(Version.getAppName()) )
						continue;
				}

				if (o_BatchIdDiff != null && o_BatchIdDiff instanceof Number)
				{
					BatchIdDiff_sum += ((Number)o_BatchIdDiff).doubleValue();
				}
			} // end loop rows

			// Now fix the values into a structure and send it of to the graph
			Double[] arr = new Double[1];

			arr[0] = new Double(BatchIdDiff_sum);

			if (_logger.isDebugEnabled())
				_logger.debug("updateGraphData("+tgdp.getName()+"): BatchIdDiff_sum='"+BatchIdDiff_sum+"'.");

			// Set the values
			tgdp.setDataPoint(this.getTimestamp(), arr);
		}

		if (GRAPH_NAME_EXEC_TIME.equals(tgdp.getName()))
		{
			int pos_StatementExecInMs = -1;
			int pos_WaitEventID       = -1;
			int pos_Application       = -1;
			int pos_suid              = -1;

			boolean discardAppnameDbxTune = Configuration.getCombinedConfiguration().getBooleanProperty(PROPKEY_summaryGraph_discardDbxTune, DEFAULT_summaryGraph_discardDbxTune);

			CounterTableModel counters = getCounterData(DATA_RATE);
			if (counters == null) 
				return;

			// Find column Id's
			List<String> colNames = counters.getColNames();
			if (colNames == null) 
				return;

			for (int colId=0; colId < colNames.size(); colId++) 
			{
				String colName = colNames.get(colId);
				if (colName.equals("suid")             ) pos_suid               = colId;
				if (colName.equals("StatementExecInMs")) pos_StatementExecInMs = colId;
				if (colName.equals("WaitEventID")      ) pos_WaitEventID        = colId;
				if (colName.equals("Application")      ) pos_Application        = colId;

				// No need to continue, we got all our columns
				if (pos_suid >= 0 && pos_StatementExecInMs >= 0 && pos_WaitEventID >= 0 && pos_Application >= 0)
					break;
			}

			if (pos_suid < 0)
			{
				_logger.debug("Can't find the position for column ('suid'="+pos_suid+")");
				return;
			}
			if (pos_StatementExecInMs < 0)
			{
				_logger.debug("Can't find the position for column ('StatementExecInMs'="+pos_StatementExecInMs+")");
				return;
			}
			if (pos_WaitEventID < 0)
			{
				_logger.debug("Can't find the position for column ('WaitEventID'="+pos_WaitEventID+")");
				return;
			}
			if (pos_Application < 0)
			{
				_logger.debug("Can't find the position for column ('Application'="+pos_Application+")");
				return;
			}
			
			
			int maxValue = 0;

			// Loop rows
			for (int rowId=0; rowId < counters.getRowCount(); rowId++)
			{
				Object o_spid              = counters.getValueAt(rowId, pos_suid);
				Object o_StatementExecInMs = counters.getValueAt(rowId, pos_StatementExecInMs);
				Object o_WaitEventID       = counters.getValueAt(rowId, pos_WaitEventID);
				Object o_Application       = counters.getValueAt(rowId, pos_Application);

				// discard system SPID's
				if (o_spid != null && o_spid instanceof Number)
				{
					if (((Number)o_spid).intValue() == 0)
						continue;
				}


				// discard Application name 'AseTune'
				if (discardAppnameDbxTune && o_Application != null && o_Application instanceof String)
				{
					if ( ((String)o_Application).startsWith(Version.getAppName()) )
						continue;
				}

				// discard SPID's waiting for read-from-network
				if (o_WaitEventID != null && o_WaitEventID instanceof Number)
				{
					if (((Number)o_WaitEventID).intValue() == 250)
						continue;
				}

				// only SUM if it's a current execution
				if (o_StatementExecInMs != null && o_StatementExecInMs instanceof Number)
				{
					if (((Number)o_StatementExecInMs).intValue() > 0)
						maxValue = Math.max(maxValue, ((Number)o_StatementExecInMs).intValue());
				}
			} // end loop rows

			// Now fix the values into a structure and send it of to the graph
			Double[] arr = new Double[1];

			arr[0] = new Double(maxValue / 1000.0);

			if (_logger.isDebugEnabled())
				_logger.debug("updateGraphData("+tgdp.getName()+"): StatementExecInMs_maxValue='"+maxValue+"'.");

			// Set the values
			tgdp.setDataPoint(this.getTimestamp(), arr);
		}

		if (GRAPH_NAME_EXEC_COUNT.equals(tgdp.getName()))
		{
			int pos_StatementExecInMs = -1;
			int pos_WaitEventID       = -1;
			int pos_Application       = -1;
			int pos_suid              = -1;

			boolean discardAppnameDbxTune = Configuration.getCombinedConfiguration().getBooleanProperty(PROPKEY_summaryGraph_discardDbxTune, DEFAULT_summaryGraph_discardDbxTune);

			CounterTableModel counters = getCounterData(DATA_RATE);
			if (counters == null) 
				return;

			// Find column Id's
			List<String> colNames = counters.getColNames();
			if (colNames == null) 
				return;

			for (int colId=0; colId < colNames.size(); colId++) 
			{
				String colName = colNames.get(colId);
				if (colName.equals("suid")             ) pos_suid               = colId;
				if (colName.equals("StatementExecInMs")) pos_StatementExecInMs = colId;
				if (colName.equals("WaitEventID")      ) pos_WaitEventID        = colId;
				if (colName.equals("Application")      ) pos_Application        = colId;

				// No need to continue, we got all our columns
				if (pos_suid >= 0 && pos_StatementExecInMs >= 0 && pos_WaitEventID >= 0 && pos_Application >= 0)
					break;
			}

			if (pos_suid < 0)
			{
				_logger.debug("Can't find the position for column ('suid'="+pos_suid+")");
				return;
			}
			if (pos_StatementExecInMs < 0)
			{
				_logger.debug("Can't find the position for column ('StatementExecInMs'="+pos_StatementExecInMs+")");
				return;
			}
			if (pos_WaitEventID < 0)
			{
				_logger.debug("Can't find the position for column ('WaitEventID'="+pos_WaitEventID+")");
				return;
			}
			if (pos_Application < 0)
			{
				_logger.debug("Can't find the position for column ('Application'="+pos_Application+")");
				return;
			}
			
			
			int count = 0;

			// Loop rows
			for (int rowId=0; rowId < counters.getRowCount(); rowId++)
			{
				Object o_spid              = counters.getValueAt(rowId, pos_suid);
				Object o_StatementExecInMs = counters.getValueAt(rowId, pos_StatementExecInMs);
				Object o_WaitEventID       = counters.getValueAt(rowId, pos_WaitEventID);
				Object o_Application       = counters.getValueAt(rowId, pos_Application);

				// discard system SPID's
				if (o_spid != null && o_spid instanceof Number)
				{
					if (((Number)o_spid).intValue() == 0)
						continue;
				}


				// discard Application name 'AseTune'
				if (discardAppnameDbxTune && o_Application != null && o_Application instanceof String)
				{
					if ( ((String)o_Application).startsWith(Version.getAppName()) )
						continue;
				}

				// discard SPID's waiting for read-from-network
				if (o_WaitEventID != null && o_WaitEventID instanceof Number)
				{
					if (((Number)o_WaitEventID).intValue() == 250)
						continue;
				}

				// only SUM if it's a current execution
				if (o_StatementExecInMs != null && o_StatementExecInMs instanceof Number)
				{
					if (((Number)o_StatementExecInMs).intValue() > 0)
						count++;
				}
			} // end loop rows

			// Now fix the values into a structure and send it of to the graph
			Double[] arr = new Double[1];

			arr[0] = new Double(count);

			if (_logger.isDebugEnabled())
				_logger.debug("updateGraphData("+tgdp.getName()+"): StatementExecInMs_count='"+count+"'.");

			// Set the values
			tgdp.setDataPoint(this.getTimestamp(), arr);
		}
		

		if (GRAPH_NAME_TEMPDB_SUM.equals(tgdp.getName()))
		{
			int asePageSizeInBytes = _asePageSizeInBytes;
			if (_asePageSizeInBytes < 0)
			{
				_logger.info("ASE Page size was not found... ASUMIMG 4k, otherwise calculation of TempDB space will be faulty..");
				asePageSizeInBytes = 4096;
			}
			double pagesToMbDivider = 1024.0 * 1024.0 / asePageSizeInBytes;
			
			int pos_pssinfo_tempdb_pages = -1;

			CounterTableModel counters = getCounterData(DATA_RATE);
			if (counters == null) 
				return;

			// Find column Id's
			List<String> colNames = counters.getColNames();
			if (colNames == null) 
				return;

			for (int colId=0; colId < colNames.size(); colId++) 
			{
				String colName = colNames.get(colId);
				if (colName.equals("pssinfo_tempdb_pages")) pos_pssinfo_tempdb_pages = colId;

				// No need to continue, we got all our columns
				if (pos_pssinfo_tempdb_pages >= 0)
					break;
			}

			if (pos_pssinfo_tempdb_pages < 0)
			{
				_logger.debug("Can't find the position for column ('pssinfo_tempdb_pages'="+pos_pssinfo_tempdb_pages+")");
				return;
			}
			
			
//			int count = 0;
			int sum_pssinfo_tempdb_pages = 0;

			// Loop rows
			for (int rowId=0; rowId < counters.getRowCount(); rowId++)
			{
				Object o_pssinfo_tempdb_pages = counters.getValueAt(rowId, pos_pssinfo_tempdb_pages);

				if (o_pssinfo_tempdb_pages != null && o_pssinfo_tempdb_pages instanceof Number)
				{
					int val = ((Number)o_pssinfo_tempdb_pages).intValue();
					if (val > 0)
						sum_pssinfo_tempdb_pages += val;
				}
			} // end loop rows

			// Now fix the values into a structure and send it of to the graph
			Double[] arr = new Double[1];

			arr[0] = new Double(sum_pssinfo_tempdb_pages / pagesToMbDivider);

			if (_logger.isDebugEnabled())
				_logger.debug("updateGraphData("+tgdp.getName()+"): sum_pssinfo_tempdb_pages='"+sum_pssinfo_tempdb_pages+"'.");

			// Set the values
			tgdp.setDataPoint(this.getTimestamp(), arr);
		}
	}
}
