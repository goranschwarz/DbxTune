package com.asetune.cm.ase;

import java.sql.Connection;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;

import javax.naming.NameNotFoundException;

import org.apache.log4j.Logger;

import com.asetune.ICounterController;
import com.asetune.IGuiController;
import com.asetune.Version;
import com.asetune.cm.CounterSample;
import com.asetune.cm.CounterSetTemplates;
import com.asetune.cm.CounterSetTemplates.Type;
import com.asetune.cm.CounterTableModel;
import com.asetune.cm.CountersModel;
import com.asetune.cm.ase.gui.CmProcessActivityPanel;
import com.asetune.config.dict.MonTablesDictionary;
import com.asetune.config.dict.MonTablesDictionaryManager;
import com.asetune.graph.TrendGraphDataPoint;
import com.asetune.gui.MainFrame;
import com.asetune.gui.TabularCntrPanel;
import com.asetune.gui.TrendGraph;
import com.asetune.utils.Configuration;
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
	public static final String[] NEED_CONFIG      = new String[] {"enable monitoring=1", "object lockwait timing=1", "wait event timing=1"};

	public static final String[] PCT_COLUMNS      = new String[] {};
	public static final String[] DIFF_COLUMNS     = new String[] {
		"BatchIdDiff", "cpu", "physical_io", "CPUTime", "WaitTime", 
		"LogicalReads", "PhysicalReads", "PagesRead", "PhysicalWrites", "PagesWritten", 
		"TableAccesses","IndexAccesses", "TempDbObjects", 
		"ULCBytesWritten", "ULCFlushes", "ULCFlushFull", 
		"Transactions", "Commits", "Rollbacks", 
		"PacketsSent", "PacketsReceived", "BytesSent", "BytesReceived", 
		"WorkTables", "pssinfo_tempdb_pages_diff",
		"IOSize1Page", "IOSize2Pages", "IOSize4Pages", "IOSize8Pages"};

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
	//------------------------------------------------------------
	// Implementation
	//------------------------------------------------------------
	private static final String  PROP_PREFIX                        = CM_NAME;

	public static final String  PROPKEY_sample_systemThreads        = PROP_PREFIX + ".sample.systemThreads";
	public static final boolean DEFAULT_sample_systemThreads        = true;

	public static final String  PROPKEY_summaryGraph_discardDbxTune = PROP_PREFIX + ".summaryGraph.discardDbxTune";
	public static final boolean DEFAULT_summaryGraph_discardDbxTune = true;

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

	private void addTrendGraphs()
	{
		String[] labels_chkptHk   = new String[] { "Checkpoint Writes", "HK Wash Writes", "HK GC Writes", "HK Chores Writes" };
		String[] labels_batch     = new String[] { "SQL Batch/Statement Count" };
		String[] labels_execTime  = new String[] { "Max Active SQL Execution Time In Seconds" };
		String[] labels_execCount = new String[] { "Active/Concurrent SQL Statement Execution Count" };
		
		addTrendGraphData(GRAPH_NAME_CHKPT_HK,    new TrendGraphDataPoint(GRAPH_NAME_CHKPT_HK,    labels_chkptHk));
		addTrendGraphData(GRAPH_NAME_BATCH_COUNT, new TrendGraphDataPoint(GRAPH_NAME_BATCH_COUNT, labels_batch));
		addTrendGraphData(GRAPH_NAME_EXEC_TIME,   new TrendGraphDataPoint(GRAPH_NAME_EXEC_TIME,   labels_execTime));
		addTrendGraphData(GRAPH_NAME_EXEC_COUNT,  new TrendGraphDataPoint(GRAPH_NAME_EXEC_COUNT,  labels_execCount));

		// if GUI
		if (getGuiController() != null && getGuiController().hasGUI())
		{
			// GRAPH
			TrendGraph tg = null;
			tg = new TrendGraph(GRAPH_NAME_CHKPT_HK,
				"Checkpoint and HK Writes",                     // Menu CheckBox text
				"Checkpoint and Housekeeper Writes Per Second (from Server->Processes)", // Label 
				labels_chkptHk, 
				false, // is Percent Graph
				this, 
				false, // visible at start
				0,     // graph is valid from Server Version. 0 = All Versions; >0 = Valid from this version and above 
				-1);   // minimum height
			addTrendGraph(tg.getName(), tg, true);

			// GRAPH
			tg = null;
			tg = new TrendGraph(GRAPH_NAME_BATCH_COUNT,
				"SQL Batch/Statement Count",                   // Menu CheckBox text
				"SQL Batches/Statements Processed Per Second (from Server->Processes)", // Label 
				labels_batch, 
				false, // is Percent Graph
				this, 
				false, // visible at start
				0,     // graph is valid from Server Version. 0 = All Versions; >0 = Valid from this version and above 
				-1);   // minimum height
			addTrendGraph(tg.getName(), tg, true);

			// GRAPH
			tg = null;
			tg = new TrendGraph(GRAPH_NAME_EXEC_TIME,
				"Max Active SQL Execution Time In Seconds",                   // Menu CheckBox text
				"Max Active SQL Execution Time In Seconds (from Server->Processes)", // Label 
				labels_execTime, 
				false, // is Percent Graph
				this, 
				false, // visible at start
				0,     // graph is valid from Server Version. 0 = All Versions; >0 = Valid from this version and above 
				-1);   // minimum height
			addTrendGraph(tg.getName(), tg, true);

			// GRAPH
			tg = null;
			tg = new TrendGraph(GRAPH_NAME_EXEC_COUNT,
				"Active SQL Statement Execution Count",                   // Menu CheckBox text
				"Active SQL Statement Execution Count (from Server->Processes)", // Label 
				labels_execCount, 
				false, // is Percent Graph
				this, 
				false, // visible at start
				0,     // graph is valid from Server Version. 0 = All Versions; >0 = Valid from this version and above 
				-1);   // minimum height
			addTrendGraph(tg.getName(), tg, true);
		}
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

		// Should we sample SYSTEM SPID's
		String sql_sample_systemThreads = "--and SP.suid > 0 -- Property: "+PROPKEY_sample_systemThreads+" is "+sample_systemThreads+". \n";
		if ( ! sample_systemThreads )
			sql_sample_systemThreads = "  and SP.suid > 0 -- Property: "+PROPKEY_sample_systemThreads+" is "+sample_systemThreads+". \n";

		String cols1, cols2, cols3;
		cols1 = cols2 = cols3 = "";

		String optGoalPlan = "";
//		if (aseVersion >= 15020)
//		if (aseVersion >= 1502000)
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

		
		// ASE 15.7.0 ESD#2
		String IOSize1Page        = ""; // Number of 1 page physical reads performed by the process
		String IOSize2Pages       = ""; // Number of 2 pages physical reads performed for the process
		String IOSize4Pages       = ""; // Number of 4 pages physical reads performed for the process
		String IOSize8Pages       = ""; // Number of 8 pages physical reads performed for the process
		String nl_15702           = ""; // NL for this section
		String sp_15702           = ""; // column Space padding for this section
//		if (aseVersion >= 15702)
//		if (aseVersion >= 1570020)
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
		String ClientDriverVersion = ""; // The version of the connectivity driver used by the client program
		String nl_16000           = ""; // NL for this section
		String sp_16000           = ""; // column Space padding for this section
//		if (aseVersion >= 1600000)
		if (aseVersion >= Ver.ver(16,0))
		{
			ClientDriverVersion = "MP.ClientDriverVersion, ";
			nl_16000            = "\n";
			sp_16000            = "  ";
		}

		cols1+=" MP.FamilyID, MP.SPID, MP.KPID, MP.NumChildren, \n"
			+ "  SP.status, MP.WaitEventID, \n"
			+ "  WaitClassDesc=convert(varchar(50),''), " // value will be replaced in method localCalculation()
			+ "  WaitEventDesc=convert(varchar(50),''), " // value will be replaced in method localCalculation()
			+ "  MP.SecondsWaiting, MP.BlockingSPID, \n"
			+ "  StatementStartTime = ST.StartTime, \n"
			+ "  StatementExecInMs = datediff(ms, ST.StartTime, getdate()), \n"
			+ "  MP.Command, \n"
			+ "  MP.BatchID, BatchIdDiff=convert(int,MP.BatchID), \n" // BatchIdDiff diff calculated
			+ "  procName = isnull(object_name(SP.id, SP.dbid), object_name(SP.id, 2)), \n"
			+ "  SP.stmtnum, SP.linenum, \n"
			+ sp_16000 + ClientDriverVersion + nl_16000 
			+ "  MP.Application, SP.clientname, SP.clienthostname, SP.clientapplname, "
			+ "  SP.hostname, SP.ipaddr, SP.hostprocess, \n"
			+ "  MP.DBName, MP.Login, SP.suid, MP.SecondsConnected, \n"
			+ "  SP.tran_name, SP.cpu, SP.physical_io, \n"
			+ "  A.CPUTime, A.WaitTime, A.LogicalReads, \n"
			+ "  A.PhysicalReads, A.PagesRead, A.PhysicalWrites, A.PagesWritten, \n"
			+ sp_15702 + IOSize1Page + IOSize2Pages + IOSize4Pages + IOSize8Pages + nl_15702;
		cols2 += "";
//		if (aseVersion >= 12520)
//		if (aseVersion >= 1252000)
		if (aseVersion >= Ver.ver(12,5,2))
		{
			cols2+="  A.WorkTables,  \n";
		}
//		if (aseVersion >= 15020 || (aseVersion >= 12540 && aseVersion < 15000) )
//		if (aseVersion >= 1502000 || (aseVersion >= 1254000 && aseVersion < 1500000) )
		if (aseVersion >= Ver.ver(15,0,2) || (aseVersion >= Ver.ver(12,5,4) && aseVersion < Ver.ver(15,0)) )
		{
			cols2+="  tempdb_name = db_name(tempdb_id(SP.spid)), \n";
			cols2+="  pssinfo_tempdb_pages      = convert(int, pssinfo(SP.spid, 'tempdb_pages')), \n";
			cols2+="  pssinfo_tempdb_pages_diff = convert(int, pssinfo(SP.spid, 'tempdb_pages')), \n";
		}
//		if (aseVersion >= 15025)
//		if (aseVersion >= 1502050)
		if (aseVersion >= Ver.ver(15,0,2,5))
		{
			cols2+="  N.NetworkEngineNumber, MP.ServerUserID, \n";
		}
		cols3+=" A.TableAccesses, A.IndexAccesses, A.TempDbObjects, \n"
			+ "  A.ULCBytesWritten, A.ULCFlushes, A.ULCFlushFull, \n"
			+ "  A.Transactions, A.Commits, A.Rollbacks, \n"
			+ "  MP.EngineNumber, MP.Priority, \n"
			+ "  N.PacketsSent, N.PacketsReceived, N.BytesSent, N.BytesReceived, \n"
			+ "  MP.ExecutionClass, MP.EngineGroupName";
		
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

		sql += "order by MP.SPID \n" + 
		       optGoalPlan;

		return preDropTempTables + createTempTables + sql + dropTempTables;
	}

	/** Used by the: Create 'Offline Session' Wizard */
	@Override
	public Configuration getLocalConfiguration()
	{
		Configuration conf = Configuration.getCombinedConfiguration();
		Configuration lc = new Configuration();

		lc.setProperty(PROPKEY_sample_systemThreads,        conf.getBooleanProperty(PROPKEY_sample_systemThreads,        DEFAULT_sample_systemThreads));
		lc.setProperty(PROPKEY_summaryGraph_discardDbxTune, conf.getBooleanProperty(PROPKEY_summaryGraph_discardDbxTune, DEFAULT_summaryGraph_discardDbxTune));

		return lc;
	}

	/** Used by the: Create 'Offline Session' Wizard */
	@Override
	public String getLocalConfigurationDescription(String propName)
	{
//		if (propName.equals(PROPKEY_sample_systemThreads))        return "Sample System SPID's that executes in the ASE Server";
		if (propName.equals(PROPKEY_sample_systemThreads))        return CmProcessActivityPanel.TOOLTIP_sample_systemThreads;
		if (propName.equals(PROPKEY_summaryGraph_discardDbxTune)) return CmProcessActivityPanel.TOOLTIP_summaryGraph_discardDbxTune;
		return "";
	}
	@Override
	public String getLocalConfigurationDataType(String propName)
	{
		if (propName.equals(PROPKEY_sample_systemThreads))        return Boolean.class.getSimpleName();
		if (propName.equals(PROPKEY_summaryGraph_discardDbxTune)) return Boolean.class.getSimpleName();
		return "";
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
			if      (colName.equals("WaitEventID"))   pos_WaitEventID   = colId;
			else if (colName.equals("WaitEventDesc")) pos_WaitEventDesc = colId;
			else if (colName.equals("WaitClassDesc")) pos_WaitClassDesc = colId;
			else if (colName.equals("BlockingSPID"))  pos_BlockingSPID  = colId;

			// Noo need to continue, we got all our columns
			if (pos_WaitEventID >= 0 && pos_WaitEventDesc >= 0 && pos_WaitClassDesc >= 0 && pos_BlockingSPID >= 0)
				break;
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

			// Add any blocking SPIDs to the MAP
			if (o_blockingSpid instanceof Number)
			{
				if (o_blockingSpid != null && ((Number)o_blockingSpid).intValue() != 0 )
					_blockingSpids.put((Number)o_blockingSpid, null);
			}
		}
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
			tgdp.setDate(this.getTimestamp());
			tgdp.setData(arr);
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
			tgdp.setDate(this.getTimestamp());
			tgdp.setData(arr);
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
			tgdp.setDate(this.getTimestamp());
			tgdp.setData(arr);
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
			tgdp.setDate(this.getTimestamp());
			tgdp.setData(arr);
		}
	}
}
