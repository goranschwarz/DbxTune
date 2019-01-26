/*******************************************************************************
 * Copyright (C) 2010-2019 Goran Schwarz
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
package com.asetune.cm.ase;

import java.awt.Component;
import java.math.BigDecimal;
import java.sql.Connection;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

import javax.swing.JCheckBoxMenuItem;
import javax.swing.JPanel;

import org.apache.log4j.Logger;

import com.asetune.CounterController;
import com.asetune.DbxTune;
import com.asetune.ICounterController;
import com.asetune.IGuiController;
import com.asetune.alarm.AlarmHandler;
import com.asetune.alarm.events.AlarmEventBlockingLockAlarm;
import com.asetune.alarm.events.AlarmEventFullTranLog;
import com.asetune.alarm.events.AlarmEventHighCpuUtilization;
import com.asetune.alarm.events.AlarmEventHighCpuUtilization.CpuType;
import com.asetune.alarm.events.AlarmEventLongRunningTransaction;
import com.asetune.cm.CmSettingsHelper;
import com.asetune.cm.CmSettingsHelper.RegExpInputValidator;
import com.asetune.cm.CounterSample;
import com.asetune.cm.CounterSetTemplates;
import com.asetune.cm.CounterSetTemplates.Type;
import com.asetune.cm.CountersModel;
import com.asetune.cm.ase.gui.CmSummaryPanel;
import com.asetune.graph.TrendGraphDataPoint;
import com.asetune.graph.TrendGraphDataPoint.LabelType;
import com.asetune.gui.MainFrame;
import com.asetune.gui.TrendGraph;
import com.asetune.utils.AseConnectionUtils;
import com.asetune.utils.Configuration;
import com.asetune.utils.StringUtil;
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

	public static final long     NEED_SRV_VERSION = 0;
	public static final long     NEED_CE_VERSION  = 0;

	public static final String[] MON_TABLES       = new String[] {"monState"};
	public static final String[] NEED_ROLES       = new String[] {"mon_role"};
	public static final String[] NEED_CONFIG      = new String[] {};

	public static final String[] PCT_COLUMNS      = new String[] {};
	public static final String[] DIFF_COLUMNS     = new String[] {
		"LockCount", "LockWaits", "CheckPoints", "NumDeadlocks", "Connections", "Transactions", 
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
		
		// THIS IS THE SUMMARY CM, so set this
		counterController.setSummaryCm(this);
		
		addTrendGraphs();

		CounterSetTemplates.register(this);
	}


	//------------------------------------------------------------
	// Implementation
	//------------------------------------------------------------
	private static final String  PROP_PREFIX = CM_NAME;

	public static final String  PROPKEY_sample_lockCount     = PROP_PREFIX + ".sample.lockCount";
	public static final boolean DEFAULT_sample_lockCount     = true;
	
	public static final String  PROPKEY_oldestOpenTranInSecThreshold     = PROP_PREFIX + ".oldestOpenTranInSecThreshold";
	public static final int     DEFAULT_oldestOpenTranInSecThreshold     = 10;
	
	public static final String GRAPH_NAME_AA_CPU             = "aaCpuGraph";         // String x=GetCounters.CM_GRAPH_NAME__SUMMARY__AA_CPU;
	public static final String GRAPH_NAME_BLOCKING_LOCKS     = "BlockingLocksGraph";
	public static final String GRAPH_NAME_CONNECTION         = "ConnectionsGraph";   // String x=GetCounters.CM_GRAPH_NAME__SUMMARY__CONNECTION;
	public static final String GRAPH_NAME_CONNECTION_RATE    = "ConnRateGraph";
	public static final String GRAPH_NAME_AA_DISK_READ_WRITE = "aaReadWriteGraph";   // String x=GetCounters.CM_GRAPH_NAME__SUMMARY__AA_DISK_READ_WRITE;
	public static final String GRAPH_NAME_AA_NW_PACKET       = "aaPacketGraph";      // String x=GetCounters.CM_GRAPH_NAME__SUMMARY__AA_NW_PACKET;
	public static final String GRAPH_NAME_OLDEST_TRAN_IN_SEC = "OldestTranInSecGraph";
	public static final String GRAPH_NAME_LOCK_COUNT         = "SumLockCountGraph";           // LockCount

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
//		String[] labels_aaCpu            = new String[] { "System+User CPU (@@cpu_busy + @@cpu_io)", "System CPU (@@cpu_io)", "User CPU (@@cpu_busy)" };
//		String[] labels_blockingLocks    = new String[] { "Blocking Locks" };
//		String[] labels_connection       = new String[] { "UserConnections (abs)", "distinctLogins (abs)", "@@connections (diff)", "@@connections (rate)" };
//		String[] labels_connRate         = new String[] { "@@connections (rate)" };
//		String[] labels_aaDiskRW         = new String[] { "@@total_read", "@@total_write" };
//		String[] labels_aaNwPacket       = new String[] { "@@pack_received", "@@pack_sent", "@@packet_errors" };
//		String[] labels_openTran         = new String[] { "Seconds" };
//		String[] labels_lockCount        = new String[] { "Lock Count" };
//
//		String[] labels_transaction      = new String[] { "Transactions", "Rollbacks" };
//		String[] labels_selectOperations = new String[] { "Selects" };
//		String[] labels_iudmOperations   = new String[] { "Inserts", "Updates", "Deletes", "Merges" };
//		String[] labels_tabIndAccess     = new String[] { "TableAccesses", "IndexAccesses" };
//		String[] labels_tempdbAccess     = new String[] { "TempDbObjects", "WorkTables" };
//		String[] labels_ulc              = new String[] { "ULCFlushes", "ULCFlushFull", "ULCKBWritten" };
//		String[] labels_ioRw             = new String[] { "PagesRead", "PagesWritten", "PhysicalReads", "PhysicalWrites" };
//		String[] labels_logicalReads     = new String[] { "LogicalReads" };
		
//		addTrendGraphData(GRAPH_NAME_AA_CPU,             new TrendGraphDataPoint(GRAPH_NAME_AA_CPU,             labels_aaCpu,            LabelType.Static));
//		addTrendGraphData(GRAPH_NAME_TRANSACTION,        new TrendGraphDataPoint(GRAPH_NAME_TRANSACTION,        labels_transaction,      LabelType.Dynamic));
//		addTrendGraphData(GRAPH_NAME_SELECT_OPERATIONS,  new TrendGraphDataPoint(GRAPH_NAME_SELECT_OPERATIONS,  labels_selectOperations, LabelType.Static));
//		addTrendGraphData(GRAPH_NAME_IUDM_OPERATIONS,    new TrendGraphDataPoint(GRAPH_NAME_IUDM_OPERATIONS,    labels_iudmOperations,   LabelType.Static));
//		addTrendGraphData(GRAPH_NAME_TAB_IND_ACCESS,     new TrendGraphDataPoint(GRAPH_NAME_TAB_IND_ACCESS,     labels_tabIndAccess,     LabelType.Static));
//		addTrendGraphData(GRAPH_NAME_TEMPDB_ACCESS,      new TrendGraphDataPoint(GRAPH_NAME_TEMPDB_ACCESS,      labels_tempdbAccess,     LabelType.Static));
//		addTrendGraphData(GRAPH_NAME_ULC,                new TrendGraphDataPoint(GRAPH_NAME_ULC,                labels_ulc,              LabelType.Static));
//		addTrendGraphData(GRAPH_NAME_IO_RW,              new TrendGraphDataPoint(GRAPH_NAME_IO_RW,              labels_ioRw,             LabelType.Static));
//		addTrendGraphData(GRAPH_NAME_LOGICAL_READ,       new TrendGraphDataPoint(GRAPH_NAME_LOGICAL_READ,       labels_logicalReads,     LabelType.Static));
//		addTrendGraphData(GRAPH_NAME_BLOCKING_LOCKS,     new TrendGraphDataPoint(GRAPH_NAME_BLOCKING_LOCKS,     labels_blockingLocks,    LabelType.Static));
//		addTrendGraphData(GRAPH_NAME_CONNECTION,         new TrendGraphDataPoint(GRAPH_NAME_CONNECTION,         labels_connection,       LabelType.Static));
//		addTrendGraphData(GRAPH_NAME_CONNECTION_RATE,    new TrendGraphDataPoint(GRAPH_NAME_CONNECTION_RATE,    labels_connRate,         LabelType.Static));
//		addTrendGraphData(GRAPH_NAME_AA_DISK_READ_WRITE, new TrendGraphDataPoint(GRAPH_NAME_AA_DISK_READ_WRITE, labels_aaDiskRW,         LabelType.Static));
//		addTrendGraphData(GRAPH_NAME_AA_NW_PACKET,       new TrendGraphDataPoint(GRAPH_NAME_AA_NW_PACKET,       labels_aaNwPacket,       LabelType.Static));
//		addTrendGraphData(GRAPH_NAME_OLDEST_TRAN_IN_SEC, new TrendGraphDataPoint(GRAPH_NAME_OLDEST_TRAN_IN_SEC, labels_openTran,         LabelType.Static));
//		addTrendGraphData(GRAPH_NAME_LOCK_COUNT,         new TrendGraphDataPoint(GRAPH_NAME_LOCK_COUNT,         labels_lockCount,        LabelType.Static));

		addTrendGraph(GRAPH_NAME_AA_CPU,
			"CPU Summary, Global Variables", 	                                         // Menu CheckBox text
			"CPU Summary for all Engines (using @@cpu_busy, @@cpu_io) ("+SHORT_NAME+")", // Graph Label 
			new String[] { "System+User CPU (@@cpu_busy + @@cpu_io)", "System CPU (@@cpu_io)", "User CPU (@@cpu_busy)" }, 
			LabelType.Static,
			TrendGraphDataPoint.Category.CPU,
			true,  // is Percent Graph
			false, // visible at start
			0,     // graph is valid from Server Version. 0 = All Versions; >0 = Valid from this version and above 
			-1);   // minimum height

		addTrendGraph(GRAPH_NAME_TRANSACTION,
			"ASE Operations - Transaction per second",                         // Menu CheckBox text
			"ASE Operations - Transaction per Second ("+SHORT_NAME+")", // Label 
			new String[] { "Transactions", "Rollbacks" },
			LabelType.Dynamic,
			TrendGraphDataPoint.Category.OPERATIONS,
			false,   // is Percent Graph
			false,   // visible at start
			Ver.ver(15,0,3,3), // graph is valid from Server Version. 0 = All Versions; >0 = Valid from this version and above 
			-1);     // minimum height

		addTrendGraph(GRAPH_NAME_SELECT_OPERATIONS,
			"ASE Operations - Selects per second", 	                     // Menu CheckBox text
			"ASE Operations - Selects per Second ("+SHORT_NAME+")", // Label 
			new String[] { "Selects" }, 
			LabelType.Static,
			TrendGraphDataPoint.Category.OPERATIONS,
			false,   // is Percent Graph
			false,   // visible at start
			Ver.ver(15,7,0,100), // graph is valid from Server Version. 0 = All Versions; >0 = Valid from this version and above 
			-1);     // minimum height

		addTrendGraph(GRAPH_NAME_IUDM_OPERATIONS,
			"ASE Operations - Ins/Upd/Del/Merge per second", 	                   // Menu CheckBox text
			"ASE Operations - Ins/Upd/Del/Merge per Second ("+SHORT_NAME+")", // Label 
			new String[] { "Inserts", "Updates", "Deletes", "Merges" }, 
			LabelType.Static,
			TrendGraphDataPoint.Category.OPERATIONS,
			false,   // is Percent Graph
			false,   // visible at start
			Ver.ver(15,7,0,100), // graph is valid from Server Version. 0 = All Versions; >0 = Valid from this version and above 
			-1);     // minimum height

		addTrendGraph(GRAPH_NAME_TAB_IND_ACCESS,
			"ASE Operations - Table/Index Access per second", 	                    // Menu CheckBox text
			"ASE Operations - Table/Index Access per Second ("+SHORT_NAME+")", // Label 
			new String[] { "TableAccesses", "IndexAccesses" }, 
			LabelType.Static,
			TrendGraphDataPoint.Category.OPERATIONS,
			false,   // is Percent Graph
			false,   // visible at start
			Ver.ver(15,7,0,100), // graph is valid from Server Version. 0 = All Versions; >0 = Valid from this version and above 
			-1);     // minimum height

		addTrendGraph(GRAPH_NAME_TEMPDB_ACCESS,
			"ASE Operations - Tempdb Object, Work Tables per second", 	                        // Menu CheckBox text
			"ASE Operations - Tempdb Objects and Work Tables per Second ("+SHORT_NAME+")", // Label 
			new String[] { "TempDbObjects", "WorkTables" }, 
			LabelType.Static,
			TrendGraphDataPoint.Category.OPERATIONS,
			false,   // is Percent Graph
			false,   // visible at start
			Ver.ver(15,7,0,100), // graph is valid from Server Version. 0 = All Versions; >0 = Valid from this version and above 
			-1);     // minimum height

		addTrendGraph(GRAPH_NAME_ULC,
			"ASE Operations - User Log Cache per second", 	                                // Menu CheckBox text
			"ASE Operations - User Log Cache Information per Second ("+SHORT_NAME+")", // Label 
			new String[] { "ULCFlushes", "ULCFlushFull", "ULCKBWritten" }, 
			LabelType.Static,
			TrendGraphDataPoint.Category.OPERATIONS,
			false,   // is Percent Graph
			false,   // visible at start
			Ver.ver(15,7,0,100), // graph is valid from Server Version. 0 = All Versions; >0 = Valid from this version and above 
			-1);     // minimum height

		addTrendGraph(GRAPH_NAME_IO_RW,
			"ASE Operations - IO's per second", 	                  // Menu CheckBox text
			"ASE Operations - IO's per Second ("+SHORT_NAME+")", // Label 
			new String[] { "PagesRead", "PagesWritten", "PhysicalReads", "PhysicalWrites" }, 
			LabelType.Static,
			TrendGraphDataPoint.Category.DISK,
			false,   // is Percent Graph
			false,   // visible at start
			Ver.ver(15,7,0,100), // graph is valid from Server Version. 0 = All Versions; >0 = Valid from this version and above 
			-1);     // minimum height

		addTrendGraph(GRAPH_NAME_LOGICAL_READ,
			"ASE Operations - Logical Reads per second", 	                   // Menu CheckBox text
			"ASE Operations - Logical Reads per Second ("+SHORT_NAME+")", // Label 
			new String[] { "LogicalReads" }, 
			LabelType.Static,
			TrendGraphDataPoint.Category.CACHE,
			false,   // is Percent Graph
			false,   // visible at start
			Ver.ver(15,7,0,100), // graph is valid from Server Version. 0 = All Versions; >0 = Valid from this version and above 
			-1);     // minimum height

		addTrendGraph(GRAPH_NAME_BLOCKING_LOCKS,
			"Blocking Locks", 	                                     // Menu CheckBox text
			"Number of Concurrently Blocking Locks ("+SHORT_NAME+")", // Label 
			new String[] { "Blocking Locks" }, 
			LabelType.Static,
			TrendGraphDataPoint.Category.LOCK,
			false, // is Percent Graph
			false, // visible at start
			0,     // graph is valid from Server Version. 0 = All Versions; >0 = Valid from this version and above 
			-1);   // minimum height

		addTrendGraph(GRAPH_NAME_CONNECTION,
			"Connections/Users in ASE", 	          // Menu CheckBox text
			"Connections/Users connected to the ASE ("+SHORT_NAME+")", // Label 
			new String[] { "UserConnections (abs)", "distinctLogins (abs)", "@@connections (diff)", "@@connections (rate)" }, 
			LabelType.Static,
			TrendGraphDataPoint.Category.OTHER,
			false, // is Percent Graph
			false, // visible at start
			0,     // graph is valid from Server Version. 0 = All Versions; >0 = Valid from this version and above 
			-1);   // minimum height

		addTrendGraph(GRAPH_NAME_CONNECTION_RATE,
			"Connection Rate in ASE", 	          // Menu CheckBox text
			"Connection Attemtps per Second (source @@connections) ("+SHORT_NAME+")", // Label 
			new String[] { "@@connections (rate)" }, 
			LabelType.Static,
			TrendGraphDataPoint.Category.OTHER,
			false, // is Percent Graph
			false, // visible at start
			0,     // graph is valid from Server Version. 0 = All Versions; >0 = Valid from this version and above 
			-1);   // minimum height

		addTrendGraph(GRAPH_NAME_AA_DISK_READ_WRITE,
			"Disk read/write, Global Variables", 	                         // Menu CheckBox text
			"Disk read/write per second, using @@total_read, @@total_write ("+SHORT_NAME+")", // Label 
			new String[] { "Total (read + write)", "@@total_read", "@@total_write" }, 
			LabelType.Static,
			TrendGraphDataPoint.Category.DISK,
			false, // is Percent Graph
			false, // visible at start
			0,     // graph is valid from Server Version. 0 = All Versions; >0 = Valid from this version and above 
			-1);   // minimum height

		addTrendGraph(GRAPH_NAME_AA_NW_PACKET,
			"Network Packets received/sent, Global Variables", 	                            // Menu CheckBox text
			"Network Packets received/sent per second, using @@pack_received, @@pack_sent ("+SHORT_NAME+")", // Label 
			new String[] { "Total (received + sent + errors)", "@@pack_received", "@@pack_sent", "@@packet_errors" }, 
			LabelType.Static,
			TrendGraphDataPoint.Category.NETWORK,
			false, // is Percent Graph
			false, // visible at start
			0,     // graph is valid from Server Version. 0 = All Versions; >0 = Valid from this version and above 
			-1);   // minimum height

		addTrendGraph(GRAPH_NAME_OLDEST_TRAN_IN_SEC,
			"Oldest Open Transaction in any Databases",     // Menu CheckBox text
			"Oldest Open Transaction in any Databases, in Seconds ("+SHORT_NAME+")", // Label 
			new String[] { "Seconds" }, 
			LabelType.Static,
			TrendGraphDataPoint.Category.LOCK,
			false, // is Percent Graph
			false, // visible at start
			0,     // graph is valid from Server Version. 0 = All Versions; >0 = Valid from this version and above 
			-1);   // minimum height

		addTrendGraph(GRAPH_NAME_LOCK_COUNT,
			"Lock Count", 	                   // Menu CheckBox text
			"Lock Count, number of concurrent locks (from syslocks) ("+SHORT_NAME+")", // Label 
			new String[] { "Lock Count" }, 
			LabelType.Static,
			TrendGraphDataPoint.Category.LOCK,
			false,   // is Percent Graph
			false,   // visible at start
			0, // graph is valid from Server Version. 0 = All Versions; >0 = Valid from this version and above 
			-1);     // minimum height
		

//		// if GUI
//		if (getGuiController() != null && getGuiController().hasGUI())
//		{
//			// GRAPH
//			TrendGraph tg = null;
//			tg = new TrendGraph(GRAPH_NAME_AA_CPU,
//				"CPU Summary, Global Variables", 	                        // Menu CheckBox text
//				"CPU Summary for all Engines (using @@cpu_busy, @@cpu_io) ("+SHORT_NAME+")", // Label 
//				labels_aaCpu, 
//				true,  // is Percent Graph
//				this, 
//				false, // visible at start
//				0,     // graph is valid from Server Version. 0 = All Versions; >0 = Valid from this version and above 
//				-1);   // minimum height
//			addTrendGraph(tg.getName(), tg, true);
//
//			tg = new TrendGraph(GRAPH_NAME_TRANSACTION,
//				"ASE Operations - Transaction per second",                         // Menu CheckBox text
//				"ASE Operations - Transaction per Second ("+SHORT_NAME+")", // Label 
//				labels_transaction, 
//				false,   // is Percent Graph
//				this, 
//				false,   // visible at start
////				1503030, // graph is valid from Server Version. 0 = All Versions; >0 = Valid from this version and above 
//				Ver.ver(15,0,3,3), // graph is valid from Server Version. 0 = All Versions; >0 = Valid from this version and above 
//				-1);     // minimum height
//			addTrendGraph(tg.getName(), tg, true);
//
//			tg = new TrendGraph(GRAPH_NAME_SELECT_OPERATIONS,
//				"ASE Operations - Selects per second", 	                     // Menu CheckBox text
//				"ASE Operations - Selects per Second ("+SHORT_NAME+")", // Label 
//				labels_selectOperations, 
//				false,   // is Percent Graph
//				this, 
//				false,   // visible at start
////				1570100, // graph is valid from Server Version. 0 = All Versions; >0 = Valid from this version and above 
//				Ver.ver(15,7,0,100), // graph is valid from Server Version. 0 = All Versions; >0 = Valid from this version and above 
//				-1);     // minimum height
//			addTrendGraph(tg.getName(), tg, true);
//
//			tg = new TrendGraph(GRAPH_NAME_IUDM_OPERATIONS,
//				"ASE Operations - Ins/Upd/Del/Merge per second", 	                   // Menu CheckBox text
//				"ASE Operations - Ins/Upd/Del/Merge per Second ("+SHORT_NAME+")", // Label 
//				labels_iudmOperations, 
//				false,   // is Percent Graph
//				this, 
//				false,   // visible at start
////				1570100, // graph is valid from Server Version. 0 = All Versions; >0 = Valid from this version and above 
//				Ver.ver(15,7,0,100), // graph is valid from Server Version. 0 = All Versions; >0 = Valid from this version and above 
//				-1);     // minimum height
//			addTrendGraph(tg.getName(), tg, true);
//
//			tg = new TrendGraph(GRAPH_NAME_TAB_IND_ACCESS,
//				"ASE Operations - Table/Index Access per second", 	                    // Menu CheckBox text
//				"ASE Operations - Table/Index Access per Second ("+SHORT_NAME+")", // Label 
//				labels_tabIndAccess, 
//				false,   // is Percent Graph
//				this, 
//				false,   // visible at start
////				1570100, // graph is valid from Server Version. 0 = All Versions; >0 = Valid from this version and above 
//				Ver.ver(15,7,0,100), // graph is valid from Server Version. 0 = All Versions; >0 = Valid from this version and above 
//				-1);     // minimum height
//			addTrendGraph(tg.getName(), tg, true);
//
//			tg = new TrendGraph(GRAPH_NAME_TEMPDB_ACCESS,
//				"ASE Operations - Tempdb Object, Work Tables per second", 	                        // Menu CheckBox text
//				"ASE Operations - Tempdb Objects and Work Tables per Second ("+SHORT_NAME+")", // Label 
//				labels_tempdbAccess, 
//				false,   // is Percent Graph
//				this, 
//				false,   // visible at start
////				1570100, // graph is valid from Server Version. 0 = All Versions; >0 = Valid from this version and above 
//				Ver.ver(15,7,0,100), // graph is valid from Server Version. 0 = All Versions; >0 = Valid from this version and above 
//				-1);     // minimum height
//			addTrendGraph(tg.getName(), tg, true);
//
//			tg = new TrendGraph(GRAPH_NAME_ULC,
//				"ASE Operations - User Log Cache per second", 	                                // Menu CheckBox text
//				"ASE Operations - User Log Cache Information per Second ("+SHORT_NAME+")", // Label 
//				labels_ulc, 
//				false,   // is Percent Graph
//				this, 
//				false,   // visible at start
////				1570100, // graph is valid from Server Version. 0 = All Versions; >0 = Valid from this version and above 
//				Ver.ver(15,7,0,100), // graph is valid from Server Version. 0 = All Versions; >0 = Valid from this version and above 
//				-1);     // minimum height
//			addTrendGraph(tg.getName(), tg, true);
//
//			tg = new TrendGraph(GRAPH_NAME_IO_RW,
//				"ASE Operations - IO's per second", 	                  // Menu CheckBox text
//				"ASE Operations - IO's per Second ("+SHORT_NAME+")", // Label 
//				labels_ioRw, 
//				false,   // is Percent Graph
//				this, 
//				false,   // visible at start
////				1570100, // graph is valid from Server Version. 0 = All Versions; >0 = Valid from this version and above 
//				Ver.ver(15,7,0,100), // graph is valid from Server Version. 0 = All Versions; >0 = Valid from this version and above 
//				-1);     // minimum height
//			addTrendGraph(tg.getName(), tg, true);
//
//			tg = new TrendGraph(GRAPH_NAME_LOGICAL_READ,
//				"ASE Operations - Logical Reads per second", 	                   // Menu CheckBox text
//				"ASE Operations - Logical Reads per Second ("+SHORT_NAME+")", // Label 
//				labels_logicalReads, 
//				false,   // is Percent Graph
//				this, 
//				false,   // visible at start
////				1570100, // graph is valid from Server Version. 0 = All Versions; >0 = Valid from this version and above 
//				Ver.ver(15,7,0,100), // graph is valid from Server Version. 0 = All Versions; >0 = Valid from this version and above 
//				-1);     // minimum height
//			addTrendGraph(tg.getName(), tg, true);
//
//			tg = new TrendGraph(GRAPH_NAME_BLOCKING_LOCKS,
//				"Blocking Locks", 	                                     // Menu CheckBox text
//				"Number of Concurrently Blocking Locks ("+SHORT_NAME+")", // Label 
//				labels_blockingLocks, 
//				false, // is Percent Graph
//				this, 
//				false, // visible at start
//				0,     // graph is valid from Server Version. 0 = All Versions; >0 = Valid from this version and above 
//				-1);   // minimum height
//			addTrendGraph(tg.getName(), tg, true);
//
//			tg = new TrendGraph(GRAPH_NAME_CONNECTION,
//				"Connections/Users in ASE", 	          // Menu CheckBox text
//				"Connections/Users connected to the ASE ("+SHORT_NAME+")", // Label 
//				labels_connection, 
//				false, // is Percent Graph
//				this, 
//				false, // visible at start
//				0,     // graph is valid from Server Version. 0 = All Versions; >0 = Valid from this version and above 
//				-1);   // minimum height
//			addTrendGraph(tg.getName(), tg, true);
//
//			tg = new TrendGraph(GRAPH_NAME_CONNECTION_RATE,
//				"Connection Rate in ASE", 	          // Menu CheckBox text
//				"Connection Attemtps per Second (source @@connections) ("+SHORT_NAME+")", // Label 
//				labels_connRate, 
//				false, // is Percent Graph
//				this, 
//				false, // visible at start
//				0,     // graph is valid from Server Version. 0 = All Versions; >0 = Valid from this version and above 
//				-1);   // minimum height
//			addTrendGraph(tg.getName(), tg, true);
//
//			tg = new TrendGraph(GRAPH_NAME_AA_DISK_READ_WRITE,
//				"Disk read/write, Global Variables", 	                         // Menu CheckBox text
//				"Disk read/write per second, using @@total_read, @@total_write ("+SHORT_NAME+")", // Label 
//				labels_aaDiskRW, 
//				false, // is Percent Graph
//				this, 
//				false, // visible at start
//				0,     // graph is valid from Server Version. 0 = All Versions; >0 = Valid from this version and above 
//				-1);   // minimum height
//			addTrendGraph(tg.getName(), tg, true);
//
//			tg = new TrendGraph(GRAPH_NAME_AA_NW_PACKET,
//				"Network Packets received/sent, Global Variables", 	                            // Menu CheckBox text
//				"Network Packets received/sent per second, using @@pack_received, @@pack_sent ("+SHORT_NAME+")", // Label 
//				labels_aaNwPacket, 
//				false, // is Percent Graph
//				this, 
//				false, // visible at start
//				0,     // graph is valid from Server Version. 0 = All Versions; >0 = Valid from this version and above 
//				-1);   // minimum height
//			addTrendGraph(tg.getName(), tg, true);
//
//			tg = new TrendGraph(GRAPH_NAME_OLDEST_TRAN_IN_SEC,
//				"Oldest Open Transaction in any Databases",     // Menu CheckBox text
//				"Oldest Open Transaction in any Databases, in Seconds ("+SHORT_NAME+")", // Label 
//				labels_openTran, 
//				false, // is Percent Graph
//				this, 
//				false, // visible at start
//				0,     // graph is valid from Server Version. 0 = All Versions; >0 = Valid from this version and above 
//				-1);   // minimum height
//			addTrendGraph(tg.getName(), tg, true);
//
//			tg = new TrendGraph(GRAPH_NAME_LOCK_COUNT,
//				"Lock Count", 	                   // Menu CheckBox text
//				"Lock Count, number of concurrent locks (from syslocks) ("+SHORT_NAME+")", // Label 
//				labels_lockCount, 
//				false,   // is Percent Graph
//				this, 
//				false,   // visible at start
//				0, // graph is valid from Server Version. 0 = All Versions; >0 = Valid from this version and above 
//				-1);     // minimum height
//			addTrendGraph(tg.getName(), tg, true);
//
//		}
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
	public String[] getDependsOnConfigForVersion(Connection conn, long srvVersion, boolean isClusterEnabled)
	{
		return NEED_CONFIG;
	}

	@Override
	public List<String> getPkForVersion(Connection conn, long srvVersion, boolean isClusterEnabled)
	{
		List <String> pkCols = new LinkedList<String>();
		return pkCols;
	}

	@Override
	public String getSqlForVersion(Connection conn, long srvVersion, boolean isClusterEnabled)
	{
		boolean hasMonRole = false;
		boolean isMonitoringEnabled = false;
		boolean canDoSelectOnSyslogshold = true;
		String nwAddrInfo = "'no sa_role'";

		boolean isHaDrSupported = false;
		if (srvVersion >= Ver.ver(16,0,0, 2))
			isHaDrSupported = true;

		if (isRuntimeInitialized())
		{
			if (isServerRoleOrPermissionActive(AseConnectionUtils.SA_ROLE))
			{
				Component guiOwner = DbxTune.hasGui() ? MainFrame.getInstance() : null;
//				nwAddrInfo = "(select min(convert(varchar(255),address_info)) from syslisteners where address_info not like 'localhost%')";
				nwAddrInfo = "'" + AseConnectionUtils.getListeners(conn, false, true, guiOwner) + "'";
			}
			hasMonRole = isServerRoleOrPermissionActive(AseConnectionUtils.MON_ROLE);
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
//		if (srvVersion >= 1570100)
		if (srvVersion >= Ver.ver(15,7,0,100))
		{
			setNonConfiguredMonitoringAllowed(true);
		}


		String utcTimeDiff = "";
//		if (srvVersion >= 1254000)
		if (srvVersion >= Ver.ver(12,5,4))
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
		
		//--------------------------------------------------------------------------------------
		// oldestOpenTranInSec (special thing that is assigned to a variable, that is assigned before the main SQL statement)
		//--------------------------------------------------------------------------------------
//		int default_oldestOpenTranInSecThreshold = Configuration.getCombinedConfiguration().getIntProperty(PROPKEY_oldestOpenTranInSecThreshold, DEFAULT_oldestOpenTranInSecThreshold);
//		String oldestOpenTranInSecThreshold = ", oldestOpenTranInSecThreshold = convert(int, "+default_oldestOpenTranInSecThreshold+") \n";
//
//		String oldestOpenTranInSec = 
//			", oldestOpenTranInSec= (select isnull(max(CASE WHEN datediff(day, h.starttime, getdate()) >= 24 \n" + // protect from: Msg 535: Difference of two datetime fields caused overflow at runtime. above 24 days or so, the MS difference is overflowned
//			"                                               THEN -1 \n" +
//			"                                               ELSE datediff(ss, h.starttime, getdate()) \n" +
//			"                                          END),0) \n" +
//			"                        from master..syslogshold h \n" +
//			"                        where h.name != '$replication_truncation_point' ) \n";
//		if ( ! canDoSelectOnSyslogshold )
//		{
//			oldestOpenTranInSec = ", oldestOpenTranInSec  = -99 \n";
//		}

		// oldestOpenTranInSecThreshold
		int default_oldestOpenTranInSecThreshold = Configuration.getCombinedConfiguration().getIntProperty(PROPKEY_oldestOpenTranInSecThreshold, DEFAULT_oldestOpenTranInSecThreshold);
		String oldestOpenTranInSecThreshold            = ", oldestOpenTranInSecThreshold = @oldestOpenTranInSecThreshold \n";
		String preDeclare_oldestOpenTranInSecThreshold = "declare @oldestOpenTranInSecThreshold int \n";
		String preAssign_oldestOpenTranInSecThreshold  = "select @oldestOpenTranInSecThreshold = "+default_oldestOpenTranInSecThreshold+" \n";

		// oldestOpenTranInSec
		String oldestOpenTranInSec            = ", oldestOpenTranInSec = @oldestOpenTranInSec \n";
		String preDeclare_oldestOpenTranInSec = "declare @oldestOpenTranInSec int \n";
		String preAssign_oldestOpenTranInSec  = "    @oldestOpenTranInSec = isnull(CASE WHEN datediff(day, h.starttime, getdate()) >= 24 \n"
		                                      + "                                       THEN -1 \n"
		                                      + "                                       ELSE datediff(ss, h.starttime, getdate()) \n"
		                                      + "                                  END, 0) \n";


		//--------------------------------------------------------------------------------------
		// Oldest Open Tran Name (special thing that is assigned to a variable, that is assigned before the main SQL statement)
		//--------------------------------------------------------------------------------------

		// oldestOpenTranName
		String oldestOpenTranName            = ", oldestOpenTranName = @oldestOpenTranName \n";

		String preDeclare_oldestOpenTranName = "declare @oldestOpenTranName varchar(80) \n";
		String preAssign_oldestOpenTranName  = "   ,@oldestOpenTranName = h.name \n";


		// oldestOpenTranDbName
		String oldestOpenTranDbName            = ", oldestOpenTranDbName = @oldestOpenTranDbName \n";

		String preDeclare_oldestOpenTranDbName = "declare @oldestOpenTranDbName varchar(30) \n";
		String preAssign_oldestOpenTranDbName  = "   ,@oldestOpenTranDbName = db_name(h.dbid) \n";

		
		// oldestOpenTranSpid
		String oldestOpenTranSpid              = ", oldestOpenTranSpid = @oldestOpenTranSpid \n";

		String preDeclare_oldestOpenTranSpid   = "declare @oldestOpenTranSpid int \n";
		String preAssign_oldestOpenTranSpid    = "   ,@oldestOpenTranSpid = h.spid \n";

		
		// construct PRE-SQL for: ...
		String preAssign_oldestOpenXxx       = "select top 1 \n"
		                                     + preAssign_oldestOpenTranInSec
		                                     + preAssign_oldestOpenTranName
		                                     + preAssign_oldestOpenTranDbName
		                                     + preAssign_oldestOpenTranSpid
		                                     + "from master..syslogshold h \n"
		                                     + "where h.name != '$replication_truncation_point' \n"
		                                     + "order by h.starttime asc \n";

		if ( ! canDoSelectOnSyslogshold )
		{
			preAssign_oldestOpenXxx  = "select @oldestOpenTranInSec = -99, @oldestOpenTranName = '-', @oldestOpenTranDbName = '-', @oldestOpenTranSpid = -1 --not authorized to select on master.dbo.syslogshold \n";
		}

		
		// compose a PRE SQL... 
		String preSqlDeclareVars = "-- Declare variables that are used to store content that are materialized before the SELECT statement\n"
		                         + preDeclare_oldestOpenTranInSecThreshold
		                         + preDeclare_oldestOpenTranInSec
		                         + preDeclare_oldestOpenTranName
		                         + preDeclare_oldestOpenTranDbName
		                         + preDeclare_oldestOpenTranSpid
		                         + "\n";
		String preSqlAssignVars  = "-- Assign the above declared variables \n"
		                         + preAssign_oldestOpenTranInSecThreshold
		                         + preAssign_oldestOpenXxx
		                         + "\n";

		String preSql = preSqlDeclareVars + preSqlAssignVars;
		
		
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
			
			if (srvVersion >= Ver.ver(15,7,0,100))
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

			if (srvVersion >= Ver.ver(16,0,0, 0,5))
			{
				cols1 += ", TotalSyncCommitTime = sum(TotalSyncCommitTime) \n";
			}
			
			if (srvVersion >= Ver.ver(16,0,0, 2))
			{
				cols1 += ", SnapsGenerated      = sum(SnapsGenerated) \n";
				cols1 += ", SnapsExecuted       = sum(SnapsExecuted) \n";
			}
		}

		String LockCount = ", LockCount          = convert(int, -1) \n";
		if (Configuration.getCombinedConfiguration().getBooleanProperty(PROPKEY_sample_lockCount, DEFAULT_sample_lockCount))
		{
			LockCount = ", LockCount          = (select count(*) from master..syslocks) \n";
		}
		
		cols2 = ", srvVersion         = @@version \n" +
				", atAtServerName     = @@servername \n" +
				", clusterInstanceId  = " + (isClusterEnabled ? "convert(varchar(15),@@instanceid)"     : "'Not Enabled'") + " \n" + 
				", clusterCoordId     = " + (isClusterEnabled ? "convert(varchar(3), @@clustercoordid)" : "'Not Enabled'") + " \n" +
//				", hadrModeInt        = 0 \n" + // Just if we want to test, we can set/simulate the @@hadr_mode to 0
				", hadrModeInt        = " + (isHaDrSupported  ? "@@hadr_mode"                           : "-1") + " \n" +
				", hadrStateInt       = " + (isHaDrSupported  ? "@@hadr_state"                          : "0" ) + " \n" +
				", hadrModeStr        = " + (isHaDrSupported  ? "hadr_mode()"                           : "'Not Supported'") + " \n" +
				", hadrStateStr       = " + (isHaDrSupported  ? "hadr_state()"                          : "'Not Supported'") + " \n" +
				", timeIsNow          = getdate() \n" +
				utcTimeDiff +
				", NetworkAddressInfo = convert(varchar(60), " + nwAddrInfo + ") \n" +
				", asePageSize        = @@maxpagesize \n" +

				", bootcount          = @@bootcount \n" + // from 12.5.0.3
//				", recovery_state     = "+ (srvVersion >= 12510 ? "@@recovery_state" : "'Introduced in ASE 12.5.1'") + " \n" +
//				", recovery_state     = "+ (srvVersion >= 1251000 ? "@@recovery_state" : "'Introduced in ASE 12.5.1'") + " \n" +
				", recovery_state     = "+ (srvVersion >= Ver.ver(12,5,1) ? "@@recovery_state" : "'Introduced in ASE 12.5.1'") + " \n" +

				", cpu_busy           = @@cpu_busy \n" +
				", cpu_io             = @@io_busy \n" +
				", cpu_idle           = @@idle \n" +
				", io_total_read      = @@total_read \n" +
				", io_total_write     = @@total_write \n" +

				", aaConnections      = @@connections \n" +
				", distinctLogins     = (select count(distinct suid) from master..sysprocesses) \n" +
				LockCount +

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

//				", oldestOpenTranInSec= (select isnull(max(CASE WHEN datediff(day, h.starttime, getdate()) >= 24 \n" + // protect from: Msg 535: Difference of two datetime fields caused overflow at runtime. above 24 days or so, the MS difference is overflowned
//				"                                               THEN -1 \n" +
//				"                                               ELSE datediff(ss, h.starttime, getdate()) \n" +
//				"                                          END),0) \n" +
//				"                        from master..syslogshold h \n" +
//				"                        where h.name != '$replication_truncation_point' ) \n" +
				oldestOpenTranInSec + 
				oldestOpenTranInSecThreshold +
				oldestOpenTranName +
				oldestOpenTranDbName +
				oldestOpenTranSpid +

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
			if ( ! isServerRoleOrPermissionActive(AseConnectionUtils.MON_ROLE))
			{
				cols1     = "dummyColumn = 1 \n";
				fromTable = "";
			}
		}
		
		String sql = preSql + "select " + cols1 + cols2 + cols3 + fromTable;
		
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
			checkAndSetNc20(counters, rowId, "TotalSyncCommitTime");
			checkAndSetNc20(counters, rowId, "SnapsGenerated");
			checkAndSetNc20(counters, rowId, "SnapsExecuted");
			
			checkAndSetNc20(counters, rowId, "io_total_read");
			checkAndSetNc20(counters, rowId, "io_total_write");
			checkAndSetNc20(counters, rowId, "pack_received");
			checkAndSetNc20(counters, rowId, "pack_sent");
			checkAndSetNc20(counters, rowId, "packet_errors");
		}
		
		if (newSample == null)
			return;

		// Should we DEMAND a refresh of some CM's in this sample (since the Summary CM is first in the list)
		for (int rowId=0; rowId < newSample.getRowCount(); rowId++) 
		{
			int LockWaits                    = getIntValue(newSample, rowId, "LockWaits",                    0);
			int fullTranslogCount            = getIntValue(newSample, rowId, "fullTranslogCount",            0);
			int oldestOpenTranInSec          = getIntValue(newSample, rowId, "oldestOpenTranInSec",          0);
			int oldestOpenTranInSecThreshold = getIntValue(newSample, rowId, "oldestOpenTranInSecThreshold", 10);

			// Check LOCK WAITS and: request refresh of CmBlocking, CmActiveStatements
			if (LockWaits > 0)
			{
				getCounterController().addCmToDemandRefreshList(CmBlocking.CM_NAME);
				getCounterController().addCmToDemandRefreshList(CmActiveStatements.CM_NAME);
			}

			// Check FULL LOGS and: request refresh of CmOpenDatabases
			if (fullTranslogCount > 0)
				getCounterController().addCmToDemandRefreshList(CmOpenDatabases.CM_NAME);

			// Check OLDEST OPEN TRANSACTION and: request refresh of CmOpenDatabases
			if (oldestOpenTranInSec > oldestOpenTranInSecThreshold)
				getCounterController().addCmToDemandRefreshList(CmOpenDatabases.CM_NAME);
		}
	}
	private void checkAndSetNc20(CounterSample counters, int rowId, String columnName)
	{
		int colId = counters.findColumn(columnName);
		if (colId >= 0)
		{
			Object obj  = counters.getValueAt(rowId, colId);
			if (obj != null && obj instanceof Number)
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
	private int getIntValue(CounterSample counters, int rowId, String columnName, int defaultValue)
	{
		int colId = counters.findColumn(columnName);
		if (colId >= 0)
		{
			Object obj  = counters.getValueAt(rowId, colId);
			if (obj != null && obj instanceof Number)
				return ((Number)obj).intValue();
		}
		return defaultValue;
	}

	@Override
	public void updateGraphData(TrendGraphDataPoint tgdp)
	{
		long srvVersion = getServerVersion();

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
				tgdp.setDataPoint(this.getTimestamp(), arr);
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
//			if ( srvVersion < 1503030 || transactions_pos == -1 )
			if ( srvVersion < Ver.ver(15,0,3,3) || transactions_pos == -1 )
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
//				if ( srvVersion >= 1570100 )
				if ( srvVersion >= Ver.ver(15,7,0,100) )
				{
					Double[] dArray = new Double[2];
					String[] lArray = new String[] { "Transactions", "Rollbacks" };

					dArray[0] = this.getRateValueSum("Transactions");
					dArray[1] = this.getRateValueSum("Rollbacks");
					_logger.debug("updateGraphData(TransGraph): Transactions='"+dArray[0]+"', Rollbacks='"+dArray[1]+"'.");

					// Set the values
					tgdp.setDataPoint(this.getTimestamp(), lArray, dArray);
				}
				else
				{
					Double[] dArray = new Double[1];
					String[] lArray = new String[] { "Transactions" };

					dArray[0] = this.getRateValueSum("Transactions");
					_logger.debug("updateGraphData(TransGraph): Transactions='"+dArray[0]+"'.");

					// Set the values
					tgdp.setDataPoint(this.getTimestamp(), lArray, dArray);
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
//			if ( srvVersion < 1570100 || col_pos == -1 )
			if ( srvVersion < Ver.ver(15,7,0,100) || col_pos == -1 )
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
				tgdp.setDataPoint(this.getTimestamp(), arr);
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
//			if ( srvVersion < 1570100 || col_pos == -1 )
			if ( srvVersion < Ver.ver(15,7,0,100) || col_pos == -1 )
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
				tgdp.setDataPoint(this.getTimestamp(), arr);
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
//			if ( srvVersion < 1570100 || col_pos == -1 )
			if ( srvVersion < Ver.ver(15,7,0,100) || col_pos == -1 )
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
				tgdp.setDataPoint(this.getTimestamp(), arr);
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
//			if ( srvVersion < 1570100 || col_pos == -1 )
			if ( srvVersion < Ver.ver(15,7,0,100) || col_pos == -1 )
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
				tgdp.setDataPoint(this.getTimestamp(), arr);
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
//			if ( srvVersion < 1570100 || col_pos == -1 )
			if ( srvVersion < Ver.ver(15,7,0,100) || col_pos == -1 )
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
				tgdp.setDataPoint(this.getTimestamp(), arr);
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
//			if ( srvVersion < 1570100 || col_pos == -1 )
			if ( srvVersion < Ver.ver(15,7,0,100) || col_pos == -1 )
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
				tgdp.setDataPoint(this.getTimestamp(), arr);
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
//			if ( srvVersion < 1570100 || col_pos == -1 )
			if ( srvVersion < Ver.ver(15,7,0,100) || col_pos == -1 )
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
				tgdp.setDataPoint(this.getTimestamp(), arr);
			}
		}

		//---------------------------------
		// GRAPH:
		//---------------------------------
		if (GRAPH_NAME_LOCK_COUNT.equals(tgdp.getName()))
		{
			Double[] arr = new Double[1];

			arr[0] = this.getAbsValueAsDouble (0, "LockCount");
			_logger.debug("updateGraphData("+tgdp.getName()+"): LockCount='"+arr[0]+"'.");

			// Set the values
			tgdp.setDataPoint(this.getTimestamp(), arr);
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
			tgdp.setDataPoint(this.getTimestamp(), arr);
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
			tgdp.setDataPoint(this.getTimestamp(), arr);
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
			tgdp.setDataPoint(this.getTimestamp(), arr);
		}

		//---------------------------------
		// GRAPH:
		//---------------------------------
		if (GRAPH_NAME_AA_DISK_READ_WRITE.equals(tgdp.getName()))
		{	
			Double[] arr = new Double[3];

			Double io_total_read  = this.getRateValueAsDouble (0, "io_total_read");
			Double io_total_write = this.getRateValueAsDouble (0, "io_total_write");
			
			arr[0] = io_total_read + io_total_write;
			arr[1] = io_total_read;
			arr[2] = io_total_write;
			_logger.debug("updateGraphData(aaReadWriteGraph): total='"+arr[0]+"', io_total_read='"+arr[1]+"', io_total_write='"+arr[2]+"'.");

			// Set the values
			tgdp.setDataPoint(this.getTimestamp(), arr);
		}

		//---------------------------------
		// GRAPH:
		//---------------------------------
		if (GRAPH_NAME_AA_NW_PACKET.equals(tgdp.getName()))
		{	
			Double[] arr = new Double[4];

			Double pack_received = this.getRateValueAsDouble (0, "pack_received");
			Double pack_sent     = this.getRateValueAsDouble (0, "pack_sent");
			Double packet_errors = this.getRateValueAsDouble (0, "packet_errors");

			arr[0] = pack_received + pack_sent + packet_errors;
			arr[1] = pack_received;
			arr[2] = pack_sent;
			arr[3] = packet_errors;
			_logger.debug("updateGraphData(aaPacketGraph): total='"+arr[0]+"', pack_received='"+arr[1]+"', pack_sent='"+arr[2]+"', packet_errors='"+arr[3]+"'.");

			// Set the values
			tgdp.setDataPoint(this.getTimestamp(), arr);
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
			tgdp.setDataPoint(this.getTimestamp(), arr);
		}
	}
	
	@Override
	public void sendAlarmRequest()
	{
		if ( ! hasDiffData() )
			return;

		CountersModel cm = this;

		boolean debugPrint = System.getProperty("sendAlarmRequest.debug", "false").equalsIgnoreCase("true");

		//-------------------------------------------------------
		// CPU Usage
		//-------------------------------------------------------
		if (    isSystemAlarmsForColumnEnabledAndInTimeRange("TotalCPUTime") 
		     || isSystemAlarmsForColumnEnabledAndInTimeRange("UserCPUTime") 
		     || isSystemAlarmsForColumnEnabledAndInTimeRange("IoCPUTime")
		   )
		{
			Double cpuUser        = cm.getDiffValueAsDouble(0, "cpu_busy");
			Double cpuSystem      = cm.getDiffValueAsDouble(0, "cpu_io");
			Double cpuIdle        = cm.getDiffValueAsDouble(0, "cpu_idle");

//			System.out.println("##### sendAlarmRequest("+cm.getName()+"): @@cpu_busy='"+cpuUser+"', @@cpu_io='"+cpuSystem+"', @@cpu_idle='"+cpuIdle+"'.");
			
			if (cpuUser != null && cpuSystem != null && cpuIdle != null)
			{
				double CPUTime   = cpuUser  .doubleValue() + cpuSystem.doubleValue() + cpuIdle.doubleValue();
				double CPUUser   = cpuUser  .doubleValue();
				double CPUSystem = cpuSystem.doubleValue();
				double CPUIdle   = cpuIdle  .doubleValue();

				BigDecimal pctCPUTime       = new BigDecimal( ((1.0 * (CPUUser + CPUSystem)) / CPUTime) * 100 ).setScale(1, BigDecimal.ROUND_HALF_EVEN);
				BigDecimal pctUserCPUTime   = new BigDecimal( ((1.0 * (CPUUser            )) / CPUTime) * 100 ).setScale(1, BigDecimal.ROUND_HALF_EVEN);
				BigDecimal pctSystemCPUTime = new BigDecimal( ((1.0 * (CPUSystem          )) / CPUTime) * 100 ).setScale(1, BigDecimal.ROUND_HALF_EVEN);
				BigDecimal pctIdleCPUTime   = new BigDecimal( ((1.0 * (CPUIdle            )) / CPUTime) * 100 ).setScale(1, BigDecimal.ROUND_HALF_EVEN);

				if (debugPrint || _logger.isDebugEnabled())
					System.out.println("##### sendAlarmRequest("+cm.getName()+"): pctCPUTime='"+pctCPUTime+"', pctSystemCPUTime='"+pctUserCPUTime+"', pctUserCPUTime='"+pctSystemCPUTime+"', pctIdleCPUTime='"+pctIdleCPUTime+"'.");

				if (AlarmHandler.hasInstance())
				{
					if (isSystemAlarmsForColumnEnabledAndInTimeRange("TotalCPUTime"))
					{
						double threshold = Configuration.getCombinedConfiguration().getDoubleProperty(PROPKEY_alarm_TotalCPUTime, DEFAULT_alarm_TotalCPUTime);
						if (pctCPUTime.doubleValue() > threshold)
							AlarmHandler.getInstance().addAlarm( new AlarmEventHighCpuUtilization(cm, threshold, CpuType.TOTAL_CPU, pctCPUTime, pctUserCPUTime, pctSystemCPUTime, pctIdleCPUTime) );
					}

					if (isSystemAlarmsForColumnEnabledAndInTimeRange("UserCPUTime"))
					{
						double threshold = Configuration.getCombinedConfiguration().getDoubleProperty(PROPKEY_alarm_UserCPUTime, DEFAULT_alarm_UserCPUTime);
						if (pctUserCPUTime.doubleValue() > threshold)
							AlarmHandler.getInstance().addAlarm( new AlarmEventHighCpuUtilization(cm, threshold, CpuType.USER_CPU, pctCPUTime, pctUserCPUTime, pctSystemCPUTime, pctIdleCPUTime) );
					}

					if (isSystemAlarmsForColumnEnabledAndInTimeRange("IoCPUTime"))
					{
						double threshold = Configuration.getCombinedConfiguration().getDoubleProperty(PROPKEY_alarm_IoCPUTime, DEFAULT_alarm_IoCPUTime);
						if (pctSystemCPUTime.doubleValue() > threshold)
							AlarmHandler.getInstance().addAlarm( new AlarmEventHighCpuUtilization(cm, threshold, CpuType.IO_CPU, pctCPUTime, pctUserCPUTime, pctSystemCPUTime, pctIdleCPUTime) );
					}
				}
			}
			// CmSummary.system.alarm.system.if.CPUTime.gt=90
			// CmSummary.ud.alarm.if.CPUTime.gt=90:AlarmClassName
		}
		
		//-------------------------------------------------------
		// Blocking Locks
		//-------------------------------------------------------
		if (isSystemAlarmsForColumnEnabledAndInTimeRange("LockWaits"))
		{
			Double LockWaits = cm.getAbsValueAsDouble (0, "LockWaits");
			if (LockWaits != null)
			{
				if (debugPrint || _logger.isDebugEnabled())
					System.out.println("##### sendAlarmRequest("+cm.getName()+"): LockWaits='"+LockWaits+"'.");

				if (AlarmHandler.hasInstance())
				{
					int threshold = Configuration.getCombinedConfiguration().getIntProperty(PROPKEY_alarm_LockWaits, DEFAULT_alarm_LockWaits);
					if (LockWaits.intValue() > threshold)
						AlarmHandler.getInstance().addAlarm( new AlarmEventBlockingLockAlarm(cm, threshold, LockWaits) );
				}
			}
		}


		//-------------------------------------------------------
		// Long running transaction
		//-------------------------------------------------------
		if (isSystemAlarmsForColumnEnabledAndInTimeRange("oldestOpenTranInSec"))
		{
			Double oldestOpenTranInSec = cm.getAbsValueAsDouble(0, "oldestOpenTranInSec");
			if (oldestOpenTranInSec != null)
			{
				if (debugPrint || _logger.isDebugEnabled())
					System.out.println("##### sendAlarmRequest("+cm.getName()+"): oldestOpenTranInSec='"+oldestOpenTranInSec+"'.");

				if (AlarmHandler.hasInstance())
				{
					int threshold = Configuration.getCombinedConfiguration().getIntProperty(PROPKEY_alarm_oldestOpenTranInSec, DEFAULT_alarm_oldestOpenTranInSec);
					if (oldestOpenTranInSec.intValue() > threshold)
					{
						// Get OldestTranName
						String OldestTranName   = cm.getAbsString(0, "oldestOpenTranName");
						String OldestTranDbName = cm.getAbsString(0, "oldestOpenTranDbName");
						
						// Get config 'skip some transaction names'
						String skipTranNameRegExp = Configuration.getCombinedConfiguration().getProperty(PROPKEY_alarm_oldestOpenTranInSecSkipTranName, DEFAULT_alarm_oldestOpenTranInSecSkipTranName);

						// send alarm, if...
						if (StringUtil.hasValue(skipTranNameRegExp) && StringUtil.hasValue(OldestTranName))
						{
							if ( ! OldestTranName.matches(skipTranNameRegExp) )
								AlarmHandler.getInstance().addAlarm( new AlarmEventLongRunningTransaction(cm, threshold, OldestTranDbName, oldestOpenTranInSec, OldestTranName) );
						}
						else
						{
							AlarmHandler.getInstance().addAlarm( new AlarmEventLongRunningTransaction(cm, threshold, OldestTranDbName, oldestOpenTranInSec, OldestTranName) );
						}
						
						//AlarmHandler.getInstance().addAlarm( new AlarmEventLongRunningTransaction(cm, oldestOpenTranInSec) );
					}
				}
			}
		}

		
		//-------------------------------------------------------
		// Full transaction log in "any" database
		//-------------------------------------------------------
		if (isSystemAlarmsForColumnEnabledAndInTimeRange("fullTranslogCount"))
		{
			Double fullTranslogCount = cm.getAbsValueAsDouble(0, "fullTranslogCount");
			if (fullTranslogCount != null)
			{
				if (debugPrint || _logger.isDebugEnabled())
					System.out.println("##### sendAlarmRequest("+cm.getName()+"): fullTranslogCount='"+fullTranslogCount+"'.");

				if (AlarmHandler.hasInstance())
				{
					int threshold = Configuration.getCombinedConfiguration().getIntProperty(PROPKEY_alarm_fullTranslogCount, DEFAULT_alarm_fullTranslogCount);
					if (fullTranslogCount.intValue() > threshold)
						AlarmHandler.getInstance().addAlarm( new AlarmEventFullTranLog(cm, threshold, fullTranslogCount) );
				}
			}
		}
	}

	public static final String  PROPKEY_alarm_TotalCPUTime                    = CM_NAME + ".alarm.system.if.TotalCPUTime.gt";
	public static final double  DEFAULT_alarm_TotalCPUTime                    = 99.9;
	                                                                          
	public static final String  PROPKEY_alarm_UserCPUTime                     = CM_NAME + ".alarm.system.if.UserCPUTime.gt";
	public static final double  DEFAULT_alarm_UserCPUTime                     = 80;
	                                                                          
	public static final String  PROPKEY_alarm_IoCPUTime                       = CM_NAME + ".alarm.system.if.IoCPUTime.gt";
	public static final double  DEFAULT_alarm_IoCPUTime                       = 95;
	                                                                          
	public static final String  PROPKEY_alarm_LockWaits                       = CM_NAME + ".alarm.system.if.LockWaits.gt";
	public static final int     DEFAULT_alarm_LockWaits                       = 5;
	                                                                          
	public static final String  PROPKEY_alarm_oldestOpenTranInSec             = CM_NAME + ".alarm.system.if.oldestOpenTranInSec.gt";
	public static final int     DEFAULT_alarm_oldestOpenTranInSec             = 60;
	
	public static final String  PROPKEY_alarm_oldestOpenTranInSecSkipTranName = CM_NAME + ".alarm.system.if.oldestOpenTranInSec.skip.tranName";
	public static final String  DEFAULT_alarm_oldestOpenTranInSecSkipTranName = "^(DUMP |\\$dmpxact).*";
	
	public static final String  PROPKEY_alarm_fullTranslogCount               = CM_NAME + ".alarm.system.if.fullTranslogCount.gt";
	public static final int     DEFAULT_alarm_fullTranslogCount               = 0;
	
	@Override
	public List<CmSettingsHelper> getLocalAlarmSettings()
	{
		Configuration conf = Configuration.getCombinedConfiguration();
		List<CmSettingsHelper> list = new ArrayList<>();
		
		list.add(new CmSettingsHelper("TotalCPUTime",                     PROPKEY_alarm_TotalCPUTime                    , Double .class, conf.getDoubleProperty(PROPKEY_alarm_TotalCPUTime                    , DEFAULT_alarm_TotalCPUTime                   ), DEFAULT_alarm_TotalCPUTime                   , "If 'TotalCPUTime' (user+io cpu-ticks) is greater than ## then send 'AlarmEventHighCpuUtilization'." ));
		list.add(new CmSettingsHelper("UserCPUTime",                      PROPKEY_alarm_UserCPUTime                     , Double .class, conf.getDoubleProperty(PROPKEY_alarm_UserCPUTime                     , DEFAULT_alarm_UserCPUTime                    ), DEFAULT_alarm_UserCPUTime                    , "If 'UserCPUTime' (user cpu-ticks) is greater than ## then send 'AlarmEventHighCpuUtilization'." ));
		list.add(new CmSettingsHelper("IoCPUTime",                        PROPKEY_alarm_IoCPUTime                       , Double .class, conf.getDoubleProperty(PROPKEY_alarm_IoCPUTime                       , DEFAULT_alarm_IoCPUTime                      ), DEFAULT_alarm_IoCPUTime                      , "If 'IoCPUTime' (io cpu-ticks) is greater than ## then send 'AlarmEventHighCpuUtilization'. Note: for ASE 15.7 or above, in threaded-kernel-mode. IO counter is incremented a bit faulty (all engines get IO ticks, while the IO-Controller-thread does the IO. This is CR# 757246)" ));
		list.add(new CmSettingsHelper("LockWaits",                        PROPKEY_alarm_LockWaits                       , Integer.class, conf.getIntProperty   (PROPKEY_alarm_LockWaits                       , DEFAULT_alarm_LockWaits                      ), DEFAULT_alarm_LockWaits                      , "If 'LockWaits' is greater than ## then send 'AlarmEventBlockingLockAlarm'." ));
		list.add(new CmSettingsHelper("oldestOpenTranInSec",              PROPKEY_alarm_oldestOpenTranInSec             , Integer.class, conf.getIntProperty   (PROPKEY_alarm_oldestOpenTranInSec             , DEFAULT_alarm_oldestOpenTranInSec            ), DEFAULT_alarm_oldestOpenTranInSec            , "If 'oldestOpenTranInSec' is greater than ## then send 'AlarmEventLongRunningTransaction'." ));
		list.add(new CmSettingsHelper("oldestOpenTranInSec SkipTranName", PROPKEY_alarm_oldestOpenTranInSecSkipTranName , String .class, conf.getProperty      (PROPKEY_alarm_oldestOpenTranInSecSkipTranName , DEFAULT_alarm_oldestOpenTranInSecSkipTranName), DEFAULT_alarm_oldestOpenTranInSecSkipTranName, "If 'oldestOpenTranInSec' is true; then we can filter out transaction names using a Regular expression... if (tranName.matches('regexp'))... This to remove alarms of 'DUMP DATABASE' or similar. A good place to test your regexp is 'http://www.regexplanet.com/advanced/java/index.html'.", new RegExpInputValidator()));
		list.add(new CmSettingsHelper("fullTranslogCount",                PROPKEY_alarm_fullTranslogCount               , Integer.class, conf.getIntProperty   (PROPKEY_alarm_fullTranslogCount               , DEFAULT_alarm_fullTranslogCount              ), DEFAULT_alarm_fullTranslogCount              , "If 'fullTranslogCount' is greater than ## then send 'AlarmEventFullTranLog'." ));

		return list;
	}
}
