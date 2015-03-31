package com.asetune.cm.ase;

import java.awt.Component;
import java.math.BigDecimal;
import java.sql.Connection;
import java.util.LinkedList;
import java.util.List;

import javax.swing.JCheckBoxMenuItem;
import javax.swing.JPanel;

import org.apache.log4j.Logger;

import com.asetune.CounterController;
import com.asetune.DbxTune;
import com.asetune.ICounterController;
import com.asetune.IGuiController;
import com.asetune.TrendGraphDataPoint;
import com.asetune.cm.CounterSample;
import com.asetune.cm.CounterSetTemplates;
import com.asetune.cm.CounterSetTemplates.Type;
import com.asetune.cm.CountersModel;
import com.asetune.cm.ase.gui.CmSummaryPanel;
import com.asetune.gui.MainFrame;
import com.asetune.gui.TrendGraph;
import com.asetune.utils.AseConnectionUtils;
import com.asetune.utils.Ver;

/**
 * @author Goran Schwarz (goran_schwarz@hotmail.com)
 */
public class CmSummary
extends CountersModel
{
	private static Logger        _logger          = Logger.getLogger(CmSummary.class);
	private static final long    serialVersionUID = 1L;

	public static final String   CM_NAME          = CmSummary.class.getSimpleName();
	public static final String   SHORT_NAME       = "Summary";
	public static final String   HTML_DESC        = 
		"<html>" +
		"Overview of how the system performs." +
		"</html>";

	public static final String   GROUP_NAME       = null;
	public static final String   GUI_ICON_FILE    = "images/"+CM_NAME+".png";

	public static final int      NEED_SRV_VERSION = 0;
	public static final int      NEED_CE_VERSION  = 0;

	public static final String[] MON_TABLES       = new String[] {"monState"};
	public static final String[] NEED_ROLES       = new String[] {"mon_role"};
	public static final String[] NEED_CONFIG      = new String[] {};

	public static final String[] PCT_COLUMNS      = new String[] {};
	public static final String[] DIFF_COLUMNS     = new String[] {
		"LockWaits", "CheckPoints", "NumDeadlocks", "Connections", "Transactions", 
		"Rollbacks", "Selects", "Updates", "Inserts", "Deletes", "Merges",              // new in 15.7 SP100
		"TableAccesses", "IndexAccesses", "TempDbObjects", "WorkTables",                // new in 15.7 SP100
		"ULCFlushes", "ULCFlushFull", "ULCKBWritten",                                   // new in 15.7 SP100
		"PagesRead", "PagesWritten", "PhysicalReads", "PhysicalWrites", "LogicalReads", // new in 15.7 SP100 
        "cpu_busy", "cpu_io", "cpu_idle", "io_total_read", "io_total_write", 
        "aaConnections", "distinctLogins", 
        "pack_received", "pack_sent", "packet_errors", "total_errors"};

	public static final boolean  NEGATIVE_DIFF_COUNTERS_TO_ZERO = false;
	public static final boolean  IS_SYSTEM_CM                   = true;
	public static final int      DEFAULT_POSTPONE_TIME          = 0;
	public static final int      DEFAULT_QUERY_TIMEOUT          = CountersModel.DEFAULT_sqlQueryTimeout;

	@Override public int     getDefaultPostponeTime()                 { return DEFAULT_POSTPONE_TIME; }
	@Override public int     getDefaultQueryTimeout()                 { return DEFAULT_QUERY_TIMEOUT; }
	@Override public boolean getDefaultIsNegativeDiffCountersToZero() { return NEGATIVE_DIFF_COUNTERS_TO_ZERO; }
	@Override public Type    getTemplateLevel()                       { return Type.SMALL; }

	/**
	 * FACTORY  method to create the object
	 */
	public static CountersModel create(ICounterController counterController, IGuiController guiController)
	{
		if (guiController != null && guiController.hasGUI())
			guiController.splashWindowProgress("Loading: Counter Model '"+CM_NAME+"'");

		return new CmSummary(counterController, guiController);
	}

	public CmSummary(ICounterController counterController, IGuiController guiController)
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
		
		// THIS IS THE SUMMARY CM, so set this
		counterController.setSummaryCm(this);
		
		addTrendGraphs();

		CounterSetTemplates.register(this);
	}


	//------------------------------------------------------------
	// Implementation
	//------------------------------------------------------------
	public static final String GRAPH_NAME_AA_CPU             = "aaCpuGraph";         // String x=GetCounters.CM_GRAPH_NAME__SUMMARY__AA_CPU;
	public static final String GRAPH_NAME_BLOCKING_LOCKS     = "BlockingLocksGraph";
	public static final String GRAPH_NAME_CONNECTION         = "ConnectionsGraph";   // String x=GetCounters.CM_GRAPH_NAME__SUMMARY__CONNECTION;
	public static final String GRAPH_NAME_CONNECTION_RATE    = "ConnRateGraph";
	public static final String GRAPH_NAME_AA_DISK_READ_WRITE = "aaReadWriteGraph";   // String x=GetCounters.CM_GRAPH_NAME__SUMMARY__AA_DISK_READ_WRITE;
	public static final String GRAPH_NAME_AA_NW_PACKET       = "aaPacketGraph";      // String x=GetCounters.CM_GRAPH_NAME__SUMMARY__AA_NW_PACKET;
	public static final String GRAPH_NAME_OLDEST_TRAN_IN_SEC = "OldestTranInSecGraph";

	public static final String GRAPH_NAME_TRANSACTION        = "TransGraph";               // Transactions, Rollbacks
	public static final String GRAPH_NAME_SELECT_OPERATIONS  = "SelectOperationsGraph";    // Selects
	public static final String GRAPH_NAME_IUDM_OPERATIONS    = "IudmOperationsGraph";      // Inserts, Updates, Deletes, Merges
	public static final String GRAPH_NAME_TAB_IND_ACCESS     = "TabIndAccessGraph";        // TableAccesses, IndexAccesses
	public static final String GRAPH_NAME_TEMPDB_ACCESS      = "TempdbAccessGraph";        // TempDbObjects, WorkTables
	public static final String GRAPH_NAME_ULC                = "UlcGraph";                 // ULCFlushes, ULCFlushFull, ULCKBWritten
	public static final String GRAPH_NAME_IO_RW              = "IoRwGraph";                // PagesRead, PagesWritten, PhysicalReads, PhysicalWrites
	public static final String GRAPH_NAME_LOGICAL_READ       = "LogicalReadGraph";         // LogicalReads

	private void addTrendGraphs()
	{
		String[] labels_aaCpu            = new String[] { "System+User CPU (@@cpu_busy + @@cpu_io)", "System CPU (@@cpu_io)", "User CPU (@@cpu_busy)" };
		String[] labels_blockingLocks    = new String[] { "Blocking Locks" };
		String[] labels_connection       = new String[] { "UserConnections (abs)", "distinctLogins (abs)", "@@connections (diff)", "@@connections (rate)" };
		String[] labels_connRate         = new String[] { "@@connections (rate)" };
		String[] labels_aaDiskRW         = new String[] { "@@total_read", "@@total_write" };
		String[] labels_aaNwPacket       = new String[] { "@@pack_received", "@@pack_sent", "@@packet_errors" };
		String[] labels_openTran         = new String[] { "Seconds" };

		String[] labels_transaction      = new String[] { "Transactions", "Rollbacks" };
		String[] labels_selectOperations = new String[] { "Selects" };
		String[] labels_iudmOperations   = new String[] { "Inserts", "Updates", "Deletes", "Merges" };
		String[] labels_tabIndAccess     = new String[] { "TableAccesses", "IndexAccesses" };
		String[] labels_tempdbAccess     = new String[] { "TempDbObjects", "WorkTables" };
		String[] labels_ulc              = new String[] { "ULCFlushes", "ULCFlushFull", "ULCKBWritten" };
		String[] labels_ioRw             = new String[] { "PagesRead", "PagesWritten", "PhysicalReads", "PhysicalWrites" };
		String[] labels_logicalReads     = new String[] { "LogicalReads" };
		
		addTrendGraphData(GRAPH_NAME_AA_CPU,             new TrendGraphDataPoint(GRAPH_NAME_AA_CPU,             labels_aaCpu));
		addTrendGraphData(GRAPH_NAME_TRANSACTION,        new TrendGraphDataPoint(GRAPH_NAME_TRANSACTION,        labels_transaction));
		addTrendGraphData(GRAPH_NAME_SELECT_OPERATIONS,  new TrendGraphDataPoint(GRAPH_NAME_SELECT_OPERATIONS,  labels_selectOperations));
		addTrendGraphData(GRAPH_NAME_IUDM_OPERATIONS,    new TrendGraphDataPoint(GRAPH_NAME_IUDM_OPERATIONS,    labels_iudmOperations));
		addTrendGraphData(GRAPH_NAME_TAB_IND_ACCESS,     new TrendGraphDataPoint(GRAPH_NAME_TAB_IND_ACCESS,     labels_tabIndAccess));
		addTrendGraphData(GRAPH_NAME_TEMPDB_ACCESS,      new TrendGraphDataPoint(GRAPH_NAME_TEMPDB_ACCESS,      labels_tempdbAccess));
		addTrendGraphData(GRAPH_NAME_ULC,                new TrendGraphDataPoint(GRAPH_NAME_ULC,                labels_ulc));
		addTrendGraphData(GRAPH_NAME_IO_RW,              new TrendGraphDataPoint(GRAPH_NAME_IO_RW,              labels_ioRw));
		addTrendGraphData(GRAPH_NAME_LOGICAL_READ,       new TrendGraphDataPoint(GRAPH_NAME_LOGICAL_READ,       labels_logicalReads));
		addTrendGraphData(GRAPH_NAME_BLOCKING_LOCKS,     new TrendGraphDataPoint(GRAPH_NAME_BLOCKING_LOCKS,     labels_blockingLocks));
		addTrendGraphData(GRAPH_NAME_CONNECTION,         new TrendGraphDataPoint(GRAPH_NAME_CONNECTION,         labels_connection));
		addTrendGraphData(GRAPH_NAME_CONNECTION_RATE,    new TrendGraphDataPoint(GRAPH_NAME_CONNECTION_RATE,    labels_connRate));
		addTrendGraphData(GRAPH_NAME_AA_DISK_READ_WRITE, new TrendGraphDataPoint(GRAPH_NAME_AA_DISK_READ_WRITE, labels_aaDiskRW));
		addTrendGraphData(GRAPH_NAME_AA_NW_PACKET,       new TrendGraphDataPoint(GRAPH_NAME_AA_NW_PACKET,       labels_aaNwPacket));
		addTrendGraphData(GRAPH_NAME_OLDEST_TRAN_IN_SEC, new TrendGraphDataPoint(GRAPH_NAME_OLDEST_TRAN_IN_SEC, labels_openTran));

		// if GUI
		if (getGuiController() != null && getGuiController().hasGUI())
		{
			// GRAPH
			TrendGraph tg = null;
			tg = new TrendGraph(GRAPH_NAME_AA_CPU,
				"CPU Summary, Global Variables", 	                        // Menu CheckBox text
				"CPU Summary for all Engines (using @@cpu_busy, @@cpu_io)", // Label 
				labels_aaCpu, 
				true,  // is Percent Graph
				this, 
				false, // visible at start
				0,     // graph is valid from Server Version. 0 = All Versions; >0 = Valid from this version and above 
				-1);   // minimum height
			addTrendGraph(tg.getName(), tg, true);

			tg = new TrendGraph(GRAPH_NAME_TRANSACTION,
				"ASE Operations - Transaction per second",                         // Menu CheckBox text
				"ASE Operations - Transaction per Second (15.0.3 esd#3 or above)", // Label 
				labels_transaction, 
				false,   // is Percent Graph
				this, 
				false,   // visible at start
//				1503030, // graph is valid from Server Version. 0 = All Versions; >0 = Valid from this version and above 
				Ver.ver(15,0,3,3), // graph is valid from Server Version. 0 = All Versions; >0 = Valid from this version and above 
				-1);     // minimum height
			addTrendGraph(tg.getName(), tg, true);

			tg = new TrendGraph(GRAPH_NAME_SELECT_OPERATIONS,
				"ASE Operations - Selects per second", 	                     // Menu CheckBox text
				"ASE Operations - Selects per Second (15.7 SP100 or above)", // Label 
				labels_selectOperations, 
				false,   // is Percent Graph
				this, 
				false,   // visible at start
//				1570100, // graph is valid from Server Version. 0 = All Versions; >0 = Valid from this version and above 
				Ver.ver(15,7,0,100), // graph is valid from Server Version. 0 = All Versions; >0 = Valid from this version and above 
				-1);     // minimum height
			addTrendGraph(tg.getName(), tg, true);

			tg = new TrendGraph(GRAPH_NAME_IUDM_OPERATIONS,
				"ASE Operations - Ins/Upd/Del/Merge per second", 	                   // Menu CheckBox text
				"ASE Operations - Ins/Upd/Del/Merge per Second (15.7 SP100 or above)", // Label 
				labels_iudmOperations, 
				false,   // is Percent Graph
				this, 
				false,   // visible at start
//				1570100, // graph is valid from Server Version. 0 = All Versions; >0 = Valid from this version and above 
				Ver.ver(15,7,0,100), // graph is valid from Server Version. 0 = All Versions; >0 = Valid from this version and above 
				-1);     // minimum height
			addTrendGraph(tg.getName(), tg, true);

			tg = new TrendGraph(GRAPH_NAME_TAB_IND_ACCESS,
				"ASE Operations - Table/Index Access per second", 	                    // Menu CheckBox text
				"ASE Operations - Table/Index Access per Second (15.7 SP100 or above)", // Label 
				labels_tabIndAccess, 
				false,   // is Percent Graph
				this, 
				false,   // visible at start
//				1570100, // graph is valid from Server Version. 0 = All Versions; >0 = Valid from this version and above 
				Ver.ver(15,7,0,100), // graph is valid from Server Version. 0 = All Versions; >0 = Valid from this version and above 
				-1);     // minimum height
			addTrendGraph(tg.getName(), tg, true);

			tg = new TrendGraph(GRAPH_NAME_TEMPDB_ACCESS,
				"ASE Operations - Tempdb Object, Work Tables per second", 	                        // Menu CheckBox text
				"ASE Operations - Tempdb Objects and Work Tables per Second (15.7 SP100 or above)", // Label 
				labels_tempdbAccess, 
				false,   // is Percent Graph
				this, 
				false,   // visible at start
//				1570100, // graph is valid from Server Version. 0 = All Versions; >0 = Valid from this version and above 
				Ver.ver(15,7,0,100), // graph is valid from Server Version. 0 = All Versions; >0 = Valid from this version and above 
				-1);     // minimum height
			addTrendGraph(tg.getName(), tg, true);

			tg = new TrendGraph(GRAPH_NAME_ULC,
				"ASE Operations - User Log Cache per second", 	                                // Menu CheckBox text
				"ASE Operations - User Log Cache Information per Second (15.7 SP100 or above)", // Label 
				labels_ulc, 
				false,   // is Percent Graph
				this, 
				false,   // visible at start
//				1570100, // graph is valid from Server Version. 0 = All Versions; >0 = Valid from this version and above 
				Ver.ver(15,7,0,100), // graph is valid from Server Version. 0 = All Versions; >0 = Valid from this version and above 
				-1);     // minimum height
			addTrendGraph(tg.getName(), tg, true);

			tg = new TrendGraph(GRAPH_NAME_IO_RW,
				"ASE Operations - IO's per second", 	                  // Menu CheckBox text
				"ASE Operations - IO's per Second (15.7 SP100 or above)", // Label 
				labels_ioRw, 
				false,   // is Percent Graph
				this, 
				false,   // visible at start
//				1570100, // graph is valid from Server Version. 0 = All Versions; >0 = Valid from this version and above 
				Ver.ver(15,7,0,100), // graph is valid from Server Version. 0 = All Versions; >0 = Valid from this version and above 
				-1);     // minimum height
			addTrendGraph(tg.getName(), tg, true);

			tg = new TrendGraph(GRAPH_NAME_LOGICAL_READ,
				"ASE Operations - Logical Reads per second", 	                   // Menu CheckBox text
				"ASE Operations - Logical Reads per Second (15.7 SP100 or above)", // Label 
				labels_logicalReads, 
				false,   // is Percent Graph
				this, 
				false,   // visible at start
//				1570100, // graph is valid from Server Version. 0 = All Versions; >0 = Valid from this version and above 
				Ver.ver(15,7,0,100), // graph is valid from Server Version. 0 = All Versions; >0 = Valid from this version and above 
				-1);     // minimum height
			addTrendGraph(tg.getName(), tg, true);

			tg = new TrendGraph(GRAPH_NAME_BLOCKING_LOCKS,
				"Blocking Locks", 	                                     // Menu CheckBox text
				"Number of Concurrently Blocking Locks (from monState)", // Label 
				labels_blockingLocks, 
				false, // is Percent Graph
				this, 
				false, // visible at start
				0,     // graph is valid from Server Version. 0 = All Versions; >0 = Valid from this version and above 
				-1);   // minimum height
			addTrendGraph(tg.getName(), tg, true);

			tg = new TrendGraph(GRAPH_NAME_CONNECTION,
				"Connections/Users in ASE", 	          // Menu CheckBox text
				"Connections/Users connected to the ASE", // Label 
				labels_connection, 
				false, // is Percent Graph
				this, 
				false, // visible at start
				0,     // graph is valid from Server Version. 0 = All Versions; >0 = Valid from this version and above 
				-1);   // minimum height
			addTrendGraph(tg.getName(), tg, true);

			tg = new TrendGraph(GRAPH_NAME_CONNECTION_RATE,
					"Connection Rate in ASE", 	          // Menu CheckBox text
					"Connection Attemtps per Second (source @@connections)", // Label 
					labels_connRate, 
					false, // is Percent Graph
					this, 
					false, // visible at start
					0,     // graph is valid from Server Version. 0 = All Versions; >0 = Valid from this version and above 
					-1);   // minimum height
				addTrendGraph(tg.getName(), tg, true);

			tg = new TrendGraph(GRAPH_NAME_AA_DISK_READ_WRITE,
				"Disk read/write, Global Variables", 	                         // Menu CheckBox text
				"Disk read/write per second, using @@total_read, @@total_write", // Label 
				labels_aaDiskRW, 
				false, // is Percent Graph
				this, 
				false, // visible at start
				0,     // graph is valid from Server Version. 0 = All Versions; >0 = Valid from this version and above 
				-1);   // minimum height
			addTrendGraph(tg.getName(), tg, true);

			tg = new TrendGraph(GRAPH_NAME_AA_NW_PACKET,
				"Network Packets received/sent, Global Variables", 	                            // Menu CheckBox text
				"Network Packets received/sent per second, using @@pack_received, @@pack_sent", // Label 
				labels_aaNwPacket, 
				false, // is Percent Graph
				this, 
				false, // visible at start
				0,     // graph is valid from Server Version. 0 = All Versions; >0 = Valid from this version and above 
				-1);   // minimum height
			addTrendGraph(tg.getName(), tg, true);

			tg = new TrendGraph(GRAPH_NAME_OLDEST_TRAN_IN_SEC,
				"Oldest Open Transaction in any Databases",     // Menu CheckBox text
				"Oldest Open Transaction in any Databases, in Seconds", // Label 
				labels_openTran, 
				false, // is Percent Graph
				this, 
				false, // visible at start
				0,     // graph is valid from Server Version. 0 = All Versions; >0 = Valid from this version and above 
				-1);   // minimum height
			addTrendGraph(tg.getName(), tg, true);
		}
	}

	@Override
	protected JPanel createGui()
	{
		setTabPanel(null); // Don't think this is necessary, but lets do it anyway...

		CmSummaryPanel summaryPanel = new CmSummaryPanel(this);

		// THIS IS THE SUMMARY CM, so set this
		CounterController.getInstance().setSummaryPanel( summaryPanel );

		// add listener, so that the GUI gets updated when data changes in the CM
		addTableModelListener( summaryPanel );

		return summaryPanel;
	}

	@Override
	public boolean isRefreshable()
	{
		// The SUMMARY should ALWAYS be refreshed
		return true;
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
		return pkCols;
	}

	@Override
	public String getSqlForVersion(Connection conn, int aseVersion, boolean isClusterEnabled)
	{
		boolean hasMonRole = false;
		boolean isMonitoringEnabled = false;
		boolean canDoSelectOnSyslogshold = true;
		String nwAddrInfo = "'no sa_role'";

		if (isRuntimeInitialized())
		{
			if (isRoleActive(AseConnectionUtils.SA_ROLE))
			{
				Component guiOwner = DbxTune.hasGui() ? MainFrame.getInstance() : null;
//				nwAddrInfo = "(select min(convert(varchar(255),address_info)) from syslisteners where address_info not like 'localhost%')";
				nwAddrInfo = "'" + AseConnectionUtils.getListeners(conn, false, true, guiOwner) + "'";
			}
			hasMonRole = isRoleActive(AseConnectionUtils.MON_ROLE);
			isMonitoringEnabled = getMonitorConfig("enable monitoring") > 0;

			// Check if we can do select on syslogshold
			canDoSelectOnSyslogshold = AseConnectionUtils.canDoSelectOnTable(conn, "master.dbo.syslogshold");
			if ( ! canDoSelectOnSyslogshold )
				_logger.warn("When trying to initialize Counters Model '"+getName()+"', named '"+getDisplayName()+"'. Problems accessing 'master.dbo.syslogshold' table, column 'oldestOpenTranInSec' will not hold valid data.");
		}
		else
			nwAddrInfo = "'tcp listeners goes here, if we are connected'";

		
		// Allow This CM to read monState even if 'enable monitoring' is turned off...
		setNonConfiguredMonitoringAllowed(false); // to reset this if we disconnects from SP100, and connects to a lower version
//		if (aseVersion >= 1570100)
		if (aseVersion >= Ver.ver(15,7,0,100))
		{
			setNonConfiguredMonitoringAllowed(true);
		}


		String utcTimeDiff = "";
//		if (aseVersion >= 1254000)
		if (aseVersion >= Ver.ver(12,5,4))
		{
			utcTimeDiff = ", utcTimeDiff        = datediff(mi, getutcdate(), getdate()) \n";
		}
		
		//
		// Compose SQL for fullTranslogCount
		// If user has 'mon_role' and 'enable monitoring' has been configured, get the value from monOpenDatabases
		// Otherwise fallback on using the slower lct_admin('logfull', dbid)
		//
		String fullTranslogCount = 
			", fullTranslogCount  = (select convert(int, sum(lct_admin('logfull', dbid))) \n" +
			"                        from master..sysdatabases readpast \n" +
			"                        where (status & 32   != 32  ) and (status & 256  != 256 ) \n" +
			"                          and (status & 1024 != 1024) and (status & 2048 != 2048) \n" +
			"                          and (status & 4096 != 4096) \n" +
			"                          and (status2 & 16  != 16  ) and (status2 &  32 != 32  ) \n" +
			"                          and name != 'model' " + (isClusterEnabled ? "and @@instanceid = isnull(instanceid,@@instanceid)" : "") + ") \n";
		if (hasMonRole && isMonitoringEnabled)
		{
			fullTranslogCount = 
				", fullTranslogCount  = (select convert(int, sum(od.TransactionLogFull)) \n" +
				"                        from master..monOpenDatabases od ) \n";
		}
		
		//
		//
		//
		String oldestOpenTranInSecThreshold = ", oldestOpenTranInSecThreshold = convert(int, 10) \n";

		String oldestOpenTranInSec = 
			", oldestOpenTranInSec= (select isnull(max(CASE WHEN datediff(day, h.starttime, getdate()) > 20 \n" + // protect from: Msg 535: Difference of two datetime fields caused overflow at runtime. above 24 days or so, the MS difference is overflowned
			"                                               THEN -1 \n" +
			"                                               ELSE datediff(ss, h.starttime, getdate()) \n" +
			"                                          END),0) \n" +
			"                        from master..syslogshold h \n" +
			"                        where h.name != '$replication_truncation_point' ) \n";
		if ( ! canDoSelectOnSyslogshold )
		{
			oldestOpenTranInSec =
				", oldestOpenTranInSec  = -99 \n";
		}
		
		String cols1, cols2, cols3;
		cols1 = cols2 = cols3 = "";

		// get all the columns for SMP servers
		// if Cluster is enabled, one row for every instance will appear
		//                        so lets try to min, max or sum the rows into 1 row...
		//                        which will "simulate" a SMP environment
		cols1 = "* \n";
		if (isClusterEnabled)
		{
			cols1 = " top 1 \n";
			cols1 += "  LockWaitThreshold   = max(LockWaitThreshold) \n";
			cols1 += ", LockWaits           = sum(LockWaits) \n";
			cols1 += ", DaysRunning         = max(DaysRunning) \n";
			cols1 += ", CheckPoints         = sum(CheckPoints) \n";
			cols1 += ", NumDeadlocks        = sum(NumDeadlocks) \n";
			cols1 += ", DiagnosticDumps     = sum(DiagnosticDumps) \n";
			cols1 += ", Connections         = sum(Connections) \n";
			cols1 += ", MaxRecovery         = avg(MaxRecovery) \n";
			cols1 += ", Transactions        = sum(Transactions) \n";
			cols1 += ", StartDate           = min(StartDate) \n";
			cols1 += ", CountersCleared     = max(CountersCleared) \n";
			
//			if (aseVersion >= 1570100)
			if (aseVersion >= Ver.ver(15,7,0,100))
			{
				cols1 += ", Rollbacks          = sum(Rollbacks) \n";	
				cols1 += ", Selects            = sum(Selects) \n";
				cols1 += ", Updates            = sum(Updates) \n";
				cols1 += ", Inserts            = sum(Inserts) \n";
				cols1 += ", Deletes            = sum(Deletes) \n";
				cols1 += ", Merges             = sum(Merges) \n";
				cols1 += ", TableAccesses      = sum(TableAccesses) \n";
				cols1 += ", IndexAccesses      = sum(IndexAccesses) \n";
				cols1 += ", TempDbObjects      = sum(TempDbObjects) \n";
				cols1 += ", WorkTables         = sum(WorkTables) \n";
				cols1 += ", ULCFlushes         = sum(ULCFlushes) \n";
				cols1 += ", ULCFlushFull       = sum(ULCFlushFull) \n";
				cols1 += ", ULCKBWritten       = sum(ULCKBWritten) \n";
				cols1 += ", PagesRead          = sum(PagesRead) \n";
				cols1 += ", PagesWritten       = sum(PagesWritten) \n";
				cols1 += ", PhysicalReads      = sum(PhysicalReads) \n";
				cols1 += ", PhysicalWrites     = sum(PhysicalWrites) \n";
				cols1 += ", LogicalReads       = sum(LogicalReads) \n";
			}
		}

		
		
		cols2 = ", aseVersion         = @@version \n" +
				", atAtServerName     = @@servername \n" +
				", clusterInstanceId  = " + (isClusterEnabled ? "convert(varchar(15),@@instanceid)"     : "'Not Enabled'") + " \n" + 
				", clusterCoordId     = " + (isClusterEnabled ? "convert(varchar(3), @@clustercoordid)" : "'Not Enabled'") + " \n" +
				", timeIsNow          = getdate() \n" +
				utcTimeDiff +
				", NetworkAddressInfo = " + nwAddrInfo + " \n" +
				", asePageSize        = @@maxpagesize \n" +

				", bootcount          = @@bootcount \n" + // from 12.5.0.3
//				", recovery_state     = "+ (aseVersion >= 12510 ? "@@recovery_state" : "'Introduced in ASE 12.5.1'") + " \n" +
//				", recovery_state     = "+ (aseVersion >= 1251000 ? "@@recovery_state" : "'Introduced in ASE 12.5.1'") + " \n" +
				", recovery_state     = "+ (aseVersion >= Ver.ver(12,5,1) ? "@@recovery_state" : "'Introduced in ASE 12.5.1'") + " \n" +

				", cpu_busy           = @@cpu_busy \n" +
				", cpu_io             = @@io_busy \n" +
				", cpu_idle           = @@idle \n" +
				", io_total_read      = @@total_read \n" +
				", io_total_write     = @@total_write \n" +

				", aaConnections      = @@connections \n" +
				", distinctLogins     = (select count(distinct suid) from master..sysprocesses) \n" +

				// ------ column 'stat'
				// 32   - Database created with for load option, or crashed while loading database, instructs recovery not to proceed
				// 256  - Database suspect | Not recovered | Cannot be opened or used | Can be dropped only with dbcc dbrepair
				// 1024 - read only
				// 2048 - dbo use only
				// 4096 - single user
				// ------ column 'stat2'
				// 16 - Database is offline
				// 32 - Database is offline until recovery completes
				// model is used during create database... so skip this one to
//				", fullTranslogCount  = (select sum(lct_admin('logfull', dbid)) \n" +
//				", fullTranslogCount  = (select convert(int, sum(lct_admin('logfull', dbid))) \n" +
//				"                        from master..sysdatabases readpast \n" +
//				"                        where (status & 32   != 32  ) and (status & 256  != 256 ) \n" +
//				"                          and (status & 1024 != 1024) and (status & 2048 != 2048) \n" +
//				"                          and (status & 4096 != 4096) \n" +
//				"                          and (status2 & 16  != 16  ) and (status2 &  32 != 32  ) \n" +
//				"                          and name != 'model' " + (isClusterEnabled ? "and @@instanceid = isnull(instanceid,@@instanceid)" : "") + ") \n" + 
				fullTranslogCount +

//				", oldestOpenTranInSec= (select isnull(max(CASE WHEN datediff(day, h.starttime, getdate()) > 20 \n" + // protect from: Msg 535: Difference of two datetime fields caused overflow at runtime. above 24 days or so, the MS difference is overflowned
//				"                                               THEN -1 \n" +
//				"                                               ELSE datediff(ss, h.starttime, getdate()) \n" +
//				"                                          END),0) \n" +
//				"                        from master..syslogshold h \n" +
//				"                        where h.name != '$replication_truncation_point' ) \n" +
				oldestOpenTranInSec + 
				oldestOpenTranInSecThreshold +

				", pack_received      = @@pack_received \n" +
				", pack_sent          = @@pack_sent \n" +

				", packet_errors      = @@packet_errors \n" +
				", total_errors       = @@total_errors \n" +
				"";

		cols3 = "";

		String fromTable = " from master..monState A \n";

		// if NOT MON_ROLE, revrite the query a bit
		if (isRuntimeInitialized())
		{
			if ( ! isRoleActive(AseConnectionUtils.MON_ROLE))
			{
				cols1     = "dummyColumn = 1 \n";
				fromTable = "";
			}
		}
		
		String sql = "select " + cols1 + cols2 + cols3 + fromTable;
		
		return sql;
	}
	
	/** 
	 * reset some negative counters to 0
	 */
	@Override
	public void localCalculation(CounterSample prevSample, CounterSample newSample, CounterSample diffData)
	{
		CounterSample counters = diffData;
		
		if (counters == null)
			return;

		// SUMMARY only have 1 row, so this was simplest way, do not do like this on CM's that has many rows
		// Loop on all diffData rows
		for (int rowId=0; rowId < counters.getRowCount(); rowId++) 
		{
			checkAndSetNc20(counters, rowId, "Transactions");
			checkAndSetNc20(counters, rowId, "Rollbacks");
			checkAndSetNc20(counters, rowId, "Selects");
			checkAndSetNc20(counters, rowId, "Updates");
			checkAndSetNc20(counters, rowId, "Inserts");
			checkAndSetNc20(counters, rowId, "Deletes");
			checkAndSetNc20(counters, rowId, "Merges");
			checkAndSetNc20(counters, rowId, "TableAccesses");
			checkAndSetNc20(counters, rowId, "IndexAccesses");
			checkAndSetNc20(counters, rowId, "TempDbObjects");
			checkAndSetNc20(counters, rowId, "WorkTables");
			checkAndSetNc20(counters, rowId, "ULCFlushes");
			checkAndSetNc20(counters, rowId, "ULCFlushFull");
			checkAndSetNc20(counters, rowId, "ULCKBWritten");
			checkAndSetNc20(counters, rowId, "PagesRead");
			checkAndSetNc20(counters, rowId, "PagesWritten");
			checkAndSetNc20(counters, rowId, "PhysicalReads");
			checkAndSetNc20(counters, rowId, "PhysicalWrites");
			checkAndSetNc20(counters, rowId, "LogicalReads");
			
			checkAndSetNc20(counters, rowId, "io_total_read");
			checkAndSetNc20(counters, rowId, "io_total_write");
			checkAndSetNc20(counters, rowId, "pack_received");
			checkAndSetNc20(counters, rowId, "pack_sent");
			checkAndSetNc20(counters, rowId, "packet_errors");
		}
	}
	private void checkAndSetNc20(CounterSample counters, int rowId, String columnName)
	{
		int colId = counters.findColumn(columnName);
		if (colId >= 0)
		{
			Object obj  = counters.getValueAt(rowId, colId);
			if (obj instanceof Number)
			{
				//System.out.println("colId="+colId+", name='"+columnName+"', o="+obj);
				if (((Number)obj).intValue() < 0)
				{
					//System.out.println("colId="+colId+", name='"+columnName+"', setting to Integer(0)");
					counters.setValueAt(new Integer(0), rowId, colId);
				}
			}
		}
	}

	@Override
	public void updateGraphData(TrendGraphDataPoint tgdp)
	{
		int aseVersion = getServerVersion();

		//---------------------------------
		// GRAPH:
		//---------------------------------
		if (GRAPH_NAME_AA_CPU.equals(tgdp.getName()))
		{
			// FIXME: in ASE 15.7 threaded mode, the @@cpu_io seems to use the DiskController Thread for all engines...
			//        This leads to a higher IO Usage than it *really* is...
			//        One way to solve this would be to simply use @@cpu_busy and disregard @@cpu_io totally
			Double cpuUser        = getDiffValueAsDouble(0, "cpu_busy");
			Double cpuSystem      = getDiffValueAsDouble(0, "cpu_io");
			Double cpuIdle        = getDiffValueAsDouble(0, "cpu_idle");
			if (cpuUser != null && cpuSystem != null && cpuIdle != null)
			{
				double CPUTime   = cpuUser  .doubleValue() + cpuSystem.doubleValue() + cpuIdle.doubleValue();
				double CPUUser   = cpuUser  .doubleValue();
				double CPUSystem = cpuSystem.doubleValue();
//				double CPUIdle   = cpuIdle  .doubleValue();

				BigDecimal calcCPUTime       = new BigDecimal( ((1.0 * (CPUUser + CPUSystem)) / CPUTime) * 100 ).setScale(1, BigDecimal.ROUND_HALF_EVEN);
				BigDecimal calcUserCPUTime   = new BigDecimal( ((1.0 * (CPUUser            )) / CPUTime) * 100 ).setScale(1, BigDecimal.ROUND_HALF_EVEN);
				BigDecimal calcSystemCPUTime = new BigDecimal( ((1.0 * (CPUSystem          )) / CPUTime) * 100 ).setScale(1, BigDecimal.ROUND_HALF_EVEN);
//				BigDecimal calcIdleCPUTime   = new BigDecimal( ((1.0 * (CPUIdle            )) / CPUTime) * 100 ).setScale(1, BigDecimal.ROUND_HALF_EVEN);

//				_cpuTime_txt          .setText(calcCPUTime      .toString());
//				_cpuUser_txt          .setText(calcUserCPUTime  .toString());
//				_cpuSystem_txt        .setText(calcSystemCPUTime.toString());
//				_cpuIdle_txt          .setText(calcIdleCPUTime  .toString());

				Double[] arr = new Double[3];

				arr[0] = calcCPUTime      .doubleValue();
				arr[1] = calcSystemCPUTime.doubleValue();
				arr[2] = calcUserCPUTime  .doubleValue();
				_logger.debug("updateGraphData(aaCpuGraph): @@cpu_busy+@@cpu_io='"+arr[0]+"', @@cpu_io='"+arr[1]+"', @@cpu_busy='"+arr[2]+"'.");

				// Set the values
				tgdp.setDate(this.getTimestamp());
				tgdp.setData(arr);
			}
		}

		//---------------------------------
		// GRAPH:
		//---------------------------------
		if (GRAPH_NAME_TRANSACTION.equals(tgdp.getName()))
		{
			int transactions_pos = -1;
			if (getCounterDataAbs() != null)
				transactions_pos = getCounterDataAbs().findColumn("Transactions");
			
			// if you don't have 'mon_role', the column 'Transactions' is not part of the result set even if we are above 15.0.3 ESD#3
//			if ( aseVersion < 1503030 || transactions_pos == -1 )
			if ( aseVersion < Ver.ver(15,0,3,3) || transactions_pos == -1 )
			{
				// disable the transactions graph checkbox...
				TrendGraph tg = getTrendGraph(GRAPH_NAME_TRANSACTION);
				if (tg != null)
				{
					JCheckBoxMenuItem menuItem = tg.getViewMenuItem();
					if (menuItem.isSelected())
						menuItem.doClick();
				}
			}
			else
			{
//				if ( aseVersion >= 1570100 )
				if ( aseVersion >= Ver.ver(15,7,0,100) )
				{
					Double[] dArray = new Double[2];
					String[] lArray = new String[] { "Transactions", "Rollbacks" };

					dArray[0] = this.getRateValueSum("Transactions");
					dArray[1] = this.getRateValueSum("Rollbacks");
					_logger.debug("updateGraphData(TransGraph): Transactions='"+dArray[0]+"', Rollbacks='"+dArray[1]+"'.");

					// Set the values
					tgdp.setDate(this.getTimestamp());
					tgdp.setLabel(lArray);
					tgdp.setData(dArray);
				}
				else
				{
					Double[] dArray = new Double[1];
					String[] lArray = new String[] { "Transactions" };

					dArray[0] = this.getRateValueSum("Transactions");
					_logger.debug("updateGraphData(TransGraph): Transactions='"+dArray[0]+"'.");

					// Set the values
					tgdp.setDate(this.getTimestamp());
					tgdp.setLabel(lArray);
					tgdp.setData(dArray);
				}
			}
		}

		//---------------------------------
		// GRAPH:
		//---------------------------------
		if (GRAPH_NAME_SELECT_OPERATIONS.equals(tgdp.getName()))
		{
			int col_pos = -1;
			if (getCounterDataAbs() != null)
				col_pos = getCounterDataAbs().findColumn("Selects");
			
			// if you don't have 'mon_role', the column 'Xxx' is not part of the result set even if we are above 15.7 SP100
//			if ( aseVersion < 1570100 || col_pos == -1 )
			if ( aseVersion < Ver.ver(15,7,0,100) || col_pos == -1 )
			{
				// disable the graph checkbox...
				TrendGraph tg = getTrendGraph(GRAPH_NAME_SELECT_OPERATIONS);
				if (tg != null)
				{
					JCheckBoxMenuItem menuItem = tg.getViewMenuItem();
					if (menuItem.isSelected())
						menuItem.doClick();
				}
			}
			else
			{
				Double[] arr = new Double[1];

				arr[0] = this.getRateValueSum("Selects");
				_logger.debug("updateGraphData("+GRAPH_NAME_SELECT_OPERATIONS+"): Selects='"+arr[0]+"'.");

				// Set the values
				tgdp.setDate(this.getTimestamp());
				tgdp.setData(arr);
			}
		}

		//---------------------------------
		// GRAPH:
		//---------------------------------
		if (GRAPH_NAME_IUDM_OPERATIONS.equals(tgdp.getName()))
		{
			int col_pos = -1;
			if (getCounterDataAbs() != null)
				col_pos = getCounterDataAbs().findColumn("Inserts");
			
			// if you don't have 'mon_role', the column 'Xxx' is not part of the result set even if we are above 15.7 SP100
//			if ( aseVersion < 1570100 || col_pos == -1 )
			if ( aseVersion < Ver.ver(15,7,0,100) || col_pos == -1 )
			{
				// disable the graph checkbox...
				TrendGraph tg = getTrendGraph(GRAPH_NAME_IUDM_OPERATIONS);
				if (tg != null)
				{
					JCheckBoxMenuItem menuItem = tg.getViewMenuItem();
					if (menuItem.isSelected())
						menuItem.doClick();
				}
			}
			else
			{
				Double[] arr = new Double[4];

				arr[0] = this.getRateValueSum("Inserts");
				arr[1] = this.getRateValueSum("Updates");
				arr[2] = this.getRateValueSum("Deletes");
				arr[3] = this.getRateValueSum("Merges");
				_logger.debug("updateGraphData("+GRAPH_NAME_IUDM_OPERATIONS+"): Inserts='"+arr[0]+"', Updates='"+arr[1]+"', Deletes='"+arr[2]+"', Merges='"+arr[3]+"'.");

				// Set the values
				tgdp.setDate(this.getTimestamp());
				tgdp.setData(arr);
			}
		}

		//---------------------------------
		// GRAPH:
		//---------------------------------
		if (GRAPH_NAME_TAB_IND_ACCESS.equals(tgdp.getName()))
		{
			int col_pos = -1;
			if (getCounterDataAbs() != null)
				col_pos = getCounterDataAbs().findColumn("TableAccesses");
			
			// if you don't have 'mon_role', the column 'Xxx' is not part of the result set even if we are above 15.7 SP100
//			if ( aseVersion < 1570100 || col_pos == -1 )
			if ( aseVersion < Ver.ver(15,7,0,100) || col_pos == -1 )
			{
				// disable the graph checkbox...
				TrendGraph tg = getTrendGraph(GRAPH_NAME_TAB_IND_ACCESS);
				if (tg != null)
				{
					JCheckBoxMenuItem menuItem = tg.getViewMenuItem();
					if (menuItem.isSelected())
						menuItem.doClick();
				}
			}
			else
			{
				Double[] arr = new Double[2];

				arr[0] = this.getRateValueSum("TableAccesses");
				arr[1] = this.getRateValueSum("IndexAccesses");
				_logger.debug("updateGraphData("+GRAPH_NAME_TAB_IND_ACCESS+"): TableAccesses='"+arr[0]+"', IndexAccesses='"+arr[1]+"'.");

				// Set the values
				tgdp.setDate(this.getTimestamp());
				tgdp.setData(arr);
			}
		}

		//---------------------------------
		// GRAPH:
		//---------------------------------
		if (GRAPH_NAME_TEMPDB_ACCESS.equals(tgdp.getName()))
		{
			int col_pos = -1;
			if (getCounterDataAbs() != null)
				col_pos = getCounterDataAbs().findColumn("TempDbObjects");
			
			// if you don't have 'mon_role', the column 'Xxx' is not part of the result set even if we are above 15.7 SP100
//			if ( aseVersion < 1570100 || col_pos == -1 )
			if ( aseVersion < Ver.ver(15,7,0,100) || col_pos == -1 )
			{
				// disable the graph checkbox...
				TrendGraph tg = getTrendGraph(GRAPH_NAME_TEMPDB_ACCESS);
				if (tg != null)
				{
					JCheckBoxMenuItem menuItem = tg.getViewMenuItem();
					if (menuItem.isSelected())
						menuItem.doClick();
				}
			}
			else
			{
				Double[] arr = new Double[2];

				arr[0] = this.getRateValueSum("TempDbObjects");
				arr[1] = this.getRateValueSum("WorkTables");
				_logger.debug("updateGraphData("+GRAPH_NAME_TEMPDB_ACCESS+"): TempDbObjects='"+arr[0]+"', WorkTables='"+arr[1]+"'.");

				// Set the values
				tgdp.setDate(this.getTimestamp());
				tgdp.setData(arr);
			}
		}

		//---------------------------------
		// GRAPH:
		//---------------------------------
		if (GRAPH_NAME_ULC.equals(tgdp.getName()))
		{
			int col_pos = -1;
			if (getCounterDataAbs() != null)
				col_pos = getCounterDataAbs().findColumn("ULCFlushes");
			
			// if you don't have 'mon_role', the column 'Xxx' is not part of the result set even if we are above 15.7 SP100
//			if ( aseVersion < 1570100 || col_pos == -1 )
			if ( aseVersion < Ver.ver(15,7,0,100) || col_pos == -1 )
			{
				// disable the graph checkbox...
				TrendGraph tg = getTrendGraph(GRAPH_NAME_ULC);
				if (tg != null)
				{
					JCheckBoxMenuItem menuItem = tg.getViewMenuItem();
					if (menuItem.isSelected())
						menuItem.doClick();
				}
			}
			else
			{
				Double[] arr = new Double[3];

				arr[0] = this.getRateValueSum("ULCFlushes");
				arr[1] = this.getRateValueSum("ULCFlushFull");
				arr[2] = this.getRateValueSum("ULCKBWritten");
				_logger.debug("updateGraphData("+GRAPH_NAME_ULC+"): ULCFlushes='"+arr[0]+"', ULCFlushFull='"+arr[1]+"', ULCKBWritten='"+arr[2]+"'.");

				// Set the values
				tgdp.setDate(this.getTimestamp());
				tgdp.setData(arr);
			}
		}

		//---------------------------------
		// GRAPH:
		//---------------------------------
		if (GRAPH_NAME_IO_RW.equals(tgdp.getName()))
		{
			int col_pos = -1;
			if (getCounterDataAbs() != null)
				col_pos = getCounterDataAbs().findColumn("PagesRead");
			
			// if you don't have 'mon_role', the column 'Xxx' is not part of the result set even if we are above 15.7 SP100
//			if ( aseVersion < 1570100 || col_pos == -1 )
			if ( aseVersion < Ver.ver(15,7,0,100) || col_pos == -1 )
			{
				// disable the graph checkbox...
				TrendGraph tg = getTrendGraph(GRAPH_NAME_IO_RW);
				if (tg != null)
				{
					JCheckBoxMenuItem menuItem = tg.getViewMenuItem();
					if (menuItem.isSelected())
						menuItem.doClick();
				}
			}
			else
			{
				Double[] arr = new Double[4];

				arr[0] = this.getRateValueSum("PagesRead");
				arr[1] = this.getRateValueSum("PagesWritten");
				arr[2] = this.getRateValueSum("PhysicalReads");
				arr[3] = this.getRateValueSum("PhysicalWrites");
				_logger.debug("updateGraphData("+GRAPH_NAME_IO_RW+"): PagesRead='"+arr[0]+"', PagesWritten='"+arr[1]+"', PhysicalReads='"+arr[2]+"', PhysicalWrites='"+arr[3]+"'.");

				// Set the values
				tgdp.setDate(this.getTimestamp());
				tgdp.setData(arr);
			}
		}

		//---------------------------------
		// GRAPH:
		//---------------------------------
		if (GRAPH_NAME_LOGICAL_READ.equals(tgdp.getName()))
		{
			int col_pos = -1;
			if (getCounterDataAbs() != null)
				col_pos = getCounterDataAbs().findColumn("LogicalReads");
			
			// if you don't have 'mon_role', the column 'Xxx' is not part of the result set even if we are above 15.7 SP100
//			if ( aseVersion < 1570100 || col_pos == -1 )
			if ( aseVersion < Ver.ver(15,7,0,100) || col_pos == -1 )
			{
				// disable the graph checkbox...
				TrendGraph tg = getTrendGraph(GRAPH_NAME_LOGICAL_READ);
				if (tg != null)
				{
					JCheckBoxMenuItem menuItem = tg.getViewMenuItem();
					if (menuItem.isSelected())
						menuItem.doClick();
				}
			}
			else
			{
				Double[] arr = new Double[1];

				arr[0] = this.getRateValueSum("LogicalReads");
				_logger.debug("updateGraphData("+GRAPH_NAME_LOGICAL_READ+"): LogicalReads='"+arr[0]+"'.");

				// Set the values
				tgdp.setDate(this.getTimestamp());
				tgdp.setData(arr);
			}
		}

		//---------------------------------
		// GRAPH:
		//---------------------------------
		if (GRAPH_NAME_BLOCKING_LOCKS.equals(tgdp.getName()))
		{
			Double[] arr = new Double[1];

			arr[0] = this.getAbsValueAsDouble (0, "LockWaits");
			_logger.debug("updateGraphData(BlockingLocksGraph): LockWait='"+arr[0]+"'.");

			// Set the values
			tgdp.setDate(this.getTimestamp());
			tgdp.setData(arr);
		}

		//---------------------------------
		// GRAPH:
		//---------------------------------
		if (GRAPH_NAME_CONNECTION.equals(tgdp.getName()))
		{	
			Double[] arr = new Double[4];

			arr[0] = this.getAbsValueAsDouble (0, "Connections");
			arr[1] = this.getAbsValueAsDouble (0, "distinctLogins");
			arr[2] = this.getDiffValueAsDouble(0, "aaConnections");
			arr[3] = this.getRateValueAsDouble(0, "aaConnections");
			_logger.debug("updateGraphData(ConnectionsGraph): Connections(Abs)='"+arr[0]+"', distinctLogins(Abs)='"+arr[1]+"', aaConnections(Diff)='"+arr[2]+"', aaConnections(Rate)='"+arr[3]+"'.");

			// Set the values
			tgdp.setDate(this.getTimestamp());
			tgdp.setData(arr);
		}

		//---------------------------------
		// GRAPH:
		//---------------------------------
		if (GRAPH_NAME_CONNECTION_RATE.equals(tgdp.getName()))
		{	
			Double[] arr = new Double[1];

			arr[0] = this.getRateValueAsDouble(0, "aaConnections");
			_logger.debug("updateGraphData("+GRAPH_NAME_CONNECTION_RATE+"): aaConnections(Rate)='"+arr[0]+"'.");

			// Set the values
			tgdp.setDate(this.getTimestamp());
			tgdp.setData(arr);
		}

		//---------------------------------
		// GRAPH:
		//---------------------------------
		if (GRAPH_NAME_AA_DISK_READ_WRITE.equals(tgdp.getName()))
		{	
			Double[] arr = new Double[2];

			arr[0] = this.getRateValueAsDouble (0, "io_total_read");
			arr[1] = this.getRateValueAsDouble (0, "io_total_write");
			_logger.debug("updateGraphData(aaReadWriteGraph): io_total_read='"+arr[0]+"', io_total_write='"+arr[1]+"'.");

			// Set the values
			tgdp.setDate(this.getTimestamp());
			tgdp.setData(arr);
		}

		//---------------------------------
		// GRAPH:
		//---------------------------------
		if (GRAPH_NAME_AA_NW_PACKET.equals(tgdp.getName()))
		{	
			Double[] arr = new Double[3];

			arr[0] = this.getRateValueAsDouble (0, "pack_received");
			arr[1] = this.getRateValueAsDouble (0, "pack_sent");
			arr[2] = this.getRateValueAsDouble (0, "packet_errors");
			_logger.debug("updateGraphData(aaPacketGraph): packet_errors='"+arr[0]+"', total_errors='"+arr[1]+"', packet_errors='"+arr[2]+"'.");

			// Set the values
			tgdp.setDate(this.getTimestamp());
			tgdp.setData(arr);
		}

		//---------------------------------
		// GRAPH:
		//---------------------------------
		if (GRAPH_NAME_OLDEST_TRAN_IN_SEC.equals(tgdp.getName()))
		{	
			Double[] arr = new Double[1];

			arr[0] = this.getAbsValueAsDouble(0, "oldestOpenTranInSec");
			_logger.debug("updateGraphData("+GRAPH_NAME_OLDEST_TRAN_IN_SEC+"): oldestOpenTranInSec='"+arr[0]+"'.");

			// Set the values
			tgdp.setDate(this.getTimestamp());
			tgdp.setData(arr);
		}
	}
}
