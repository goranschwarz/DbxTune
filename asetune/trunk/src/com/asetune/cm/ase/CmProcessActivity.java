package com.asetune.cm.ase;

import java.sql.Connection;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;

import org.apache.log4j.Logger;

import com.asetune.ICounterController;
import com.asetune.IGuiController;
import com.asetune.MonTablesDictionary;
import com.asetune.TrendGraphDataPoint;
import com.asetune.cm.CounterTableModel;
import com.asetune.cm.CountersModel;
import com.asetune.cm.SamplingCnt;
import com.asetune.cm.ase.gui.CmProcessActivityPanel;
import com.asetune.gui.MainFrame;
import com.asetune.gui.TabularCntrPanel;
import com.asetune.gui.TrendGraph;
import com.asetune.utils.Configuration;

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
		"    <li>YELLOW - SPID is a System Processes</li>" +
		"    <li>GREEN  - SPID is Executing(running) or are in the Run Queue Awaiting a time slot to Execute (runnable)</li>" +
		"    <li>PINK   - SPID is Blocked by some other SPID that holds a Lock on a database object Table, Page or Row. This is the Lock Victim.</li>" +
		"    <li>RED    - SPID is Blocking other SPID's from running, this SPID is Responslibe or the Root Cause of a Blocking Lock.</li>" +
		"</ul>" +
		"</html>";

	public static final String   GROUP_NAME       = MainFrame.TCP_GROUP_SERVER;
	public static final String   GUI_ICON_FILE    = "images/"+CM_NAME+".png";

	public static final int      NEED_SRV_VERSION = 0;
	public static final int      NEED_CE_VERSION  = 0;

	public static final String[] MON_TABLES       = new String[] {"monProcessActivity", "monProcess", "sysprocesses", "monProcessNetIO"};
	public static final String[] NEED_ROLES       = new String[] {"mon_role"};
	public static final String[] NEED_CONFIG      = new String[] {"enable monitoring=1", "object lockwait timing=1", "wait event timing=1"};

	public static final String[] PCT_COLUMNS      = new String[] {};
	public static final String[] DIFF_COLUMNS     = new String[] {"BatchIdDiff", "cpu", "physical_io", "CPUTime", "WaitTime", "LogicalReads", "PhysicalReads", "PagesRead", "PhysicalWrites", "PagesWritten", "TableAccesses","IndexAccesses", "TempDbObjects", "ULCBytesWritten", "ULCFlushes", "ULCFlushFull", "Transactions", "Commits", "Rollbacks", "PacketsSent", "PacketsReceived", "BytesSent", "BytesReceived", "WorkTables", "pssinfo_tempdb_pages"};

	public static final boolean  NEGATIVE_DIFF_COUNTERS_TO_ZERO = true;
	public static final boolean  IS_SYSTEM_CM                   = true;
	public static final int      DEFAULT_POSTPONE_TIME          = 0;
	public static final int      DEFAULT_QUERY_TIMEOUT          = CountersModel.DEFAULT_sqlQueryTimeout;

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
		super(CM_NAME, GROUP_NAME, /*sql*/null, /*pkList*/null, 
				DIFF_COLUMNS, PCT_COLUMNS, MON_TABLES, 
				NEED_ROLES, NEED_CONFIG, NEED_SRV_VERSION, NEED_CE_VERSION, 
				NEGATIVE_DIFF_COUNTERS_TO_ZERO, IS_SYSTEM_CM, DEFAULT_POSTPONE_TIME);

		setDisplayName(SHORT_NAME);
		setDescription(HTML_DESC);

		setIconFile(GUI_ICON_FILE);

		setCounterController(counterController);
		setGuiController(guiController);
		
		if (getQueryTimeout() == CountersModel.DEFAULT_sqlQueryTimeout)
			setQueryTimeout(DEFAULT_QUERY_TIMEOUT);

		setDiffDissColumns( new String[] {"WaitTime"} );

		addTrendGraphs();
	}


	//------------------------------------------------------------
	// Implementation
	//------------------------------------------------------------
	private HashMap<Number,Object> _blockingSpids = new HashMap<Number,Object>(); // <(SPID)Integer> <null> indicator that the SPID is BLOCKING some other SPID

	public static final String GRAPH_NAME_CHKPT_HK = "ChkptHkGraph"; //String x=GetCounters.CM_GRAPH_NAME__PROCESS_ACTIVITY__CHKPT_HK;

	private void addTrendGraphs()
	{
		String[] labels = new String[] { "Checkpoint Writes", "HK Wash Writes", "HK GC Writes", "HK Chores Writes" };
		
		addTrendGraphData(GRAPH_NAME_CHKPT_HK, new TrendGraphDataPoint(GRAPH_NAME_CHKPT_HK, labels));

		// if GUI
		if (getGuiController() != null && getGuiController().hasGUI())
		{
			// GRAPH
			TrendGraph tg = null;
			tg = new TrendGraph(GRAPH_NAME_CHKPT_HK,
				"Checkpoint and HK Writes",                     // Menu CheckBox text
				"Checkpoint and Housekeeper Writes Per Second", // Label 
				labels, 
				false, // is Percent Graph
				this, 
				false, // visible at start
				-1);  // minimum height
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
		String cols1, cols2, cols3;
		cols1 = cols2 = cols3 = "";

		String optGoalPlan = "";
		if (aseVersion >= 15020)
		{
			optGoalPlan = "plan '(use optgoal allrows_dss)' \n";
		}

		if (isClusterEnabled)
		{
			cols1 += "MP.InstanceID, ";
		}

		String preDropTempTables =
			"/*------ drop tempdb objects if we failed doing that in previous execution -------*/ \n" +
			"if ((select object_id('#monProcessActivity')) is not null) drop table #monProcessActivity \n" +
			"if ((select object_id('#monProcess'))         is not null) drop table #monProcess         \n" +
			"if ((select object_id('#monProcessNetIO'))    is not null) drop table #monProcessNetIO    \n" +
			"go \n" +
			"\n";

		String createTempTables =
			"/*------ Snapshot, the monXXX tables, hopefully this is less expensive than doing the join via CIS -------*/ \n" +
			"select * into #monProcessActivity from master..monProcessActivity \n" +
			"select * into #monProcess         from master..monProcess         \n" +
			"select * into #monProcessNetIO    from master..monProcessNetIO    \n" +
			"\n";

		String dropTempTables = 
			"\n" +
			"/*------ drop tempdb objects -------*/ \n" +
			"drop table #monProcessActivity \n" +
			"drop table #monProcess         \n" +
			"drop table #monProcessNetIO    \n" +
			"\n";

		
		cols1+=" MP.FamilyID, MP.SPID, MP.KPID, MP.NumChildren, \n"
			+ "  SP.status, MP.WaitEventID, \n"
			+ "  WaitClassDesc=convert(varchar(50),''), " // value will be replaced in method localCalculation()
			+ "  WaitEventDesc=convert(varchar(50),''), " // value will be replaced in method localCalculation()
			+ "  MP.SecondsWaiting, MP.BlockingSPID, MP.Command, \n"
			+ "  MP.BatchID, BatchIdDiff=convert(int,MP.BatchID), \n" // BatchIdDiff diff calculated
			+ "  procName = object_name(SP.id, SP.dbid), SP.stmtnum, SP.linenum, \n"
			+ "  MP.Application, SP.clientname, SP.clienthostname, SP.clientapplname, "
			+ "  SP.hostname, SP.ipaddr, SP.hostprocess, \n"
			+ "  MP.DBName, MP.Login, SP.suid, MP.SecondsConnected, \n"
			+ "  SP.tran_name, SP.cpu, SP.physical_io, \n"
			+ "  A.CPUTime, A.WaitTime, A.LogicalReads, \n"
			+ "  A.PhysicalReads, A.PagesRead, A.PhysicalWrites, A.PagesWritten, \n";
		cols2 += "";
		if (aseVersion >= 12520)
		{
			cols2+="  A.WorkTables,  \n";
		}
		if (aseVersion >= 15020 || (aseVersion >= 12540 && aseVersion <= 15000) )
		{
			cols2+="  tempdb_name = db_name(tempdb_id(SP.spid)), pssinfo_tempdb_pages = convert(int, pssinfo(SP.spid, 'tempdb_pages')), \n";
		}
		if (aseVersion >= 15025)
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
			"from #monProcessActivity A, #monProcess MP, master..sysprocesses SP, #monProcessNetIO N \n" +
			"where MP.KPID = SP.kpid \n" +
			"  and MP.KPID = A.KPID \n" +
			"  and MP.KPID = N.KPID \n" +
			"  RUNTIME_REPLACE::SAMPLE_SYSTEM_THREADS \n"; // this is replaced in getSql()

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

		lc.setProperty(getName()+".sample.systemThreads",  conf.getBooleanProperty(getName()+".sample.systemThreads", true));

		return lc;
	}

	/** Used by the: Create 'Offline Session' Wizard */
	@Override
	public String getLocalConfigurationDescription(String propName)
	{
		if (propName.equals(getName()+".sample.systemThreads")) return "Sample System SPID's that executes in the ASE Server";
		return "";
	}
	@Override
	public String getLocalConfigurationDataType(String propName)
	{
		if (propName.equals(getName()+".sample.systemThreads")) return Boolean.class.getSimpleName();
		return "";
	}

	@Override
	public String getSql()
	{
		String sql = super.getSql();
		
		Configuration conf = Configuration.getCombinedConfiguration();
		boolean sampleSystemThreads_chk = (conf == null) ? true : conf.getBooleanProperty(getName()+".sample.systemThreads", true);

		String sampleSystemThreadSql = "";
		if ( sampleSystemThreads_chk == false )
			sampleSystemThreadSql = "  and SP.suid > 0 ";

		sql = sql.replace("RUNTIME_REPLACE::SAMPLE_SYSTEM_THREADS", sampleSystemThreadSql);
		
		return sql;
	}

	/** 
	 * Fill in the WaitEventDesc column with data from
	 * MonTableDictionary.. transforms a WaitEventId -> text description
	 * This so we do not have to do a subselect in the query that gets data
	 * doing it this way, means better performance, since the values are cached locally in memory
	 */
	@Override
	public void localCalculation(SamplingCnt prevSample, SamplingCnt newSample, SamplingCnt diffData)
	{
		// Where are various columns located in the Vector 
		int pos_WaitEventID = -1, pos_WaitEventDesc = -1, pos_WaitClassDesc = -1, pos_BlockingSPID = -1;
		int waitEventID = 0;
		String waitEventDesc = "";
		String waitClassDesc = "";
		SamplingCnt counters = diffData;
//		SamplingCnt counters = chosenData;
	
		MonTablesDictionary mtd = MonTablesDictionary.getInstance();
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
	}
}
