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
package com.asetune.cm.oracle;

import java.sql.Connection;
import java.util.Calendar;
import java.util.LinkedList;
import java.util.List;

import javax.swing.JPanel;

import org.apache.log4j.Logger;

import com.asetune.CounterController;
import com.asetune.ICounterController;
import com.asetune.IGuiController;
import com.asetune.cm.CounterSetTemplates;
import com.asetune.cm.CounterSetTemplates.Type;
import com.asetune.cm.CountersModel;
import com.asetune.cm.oracle.gui.CmSummaryPanel;
import com.asetune.graph.TrendGraphDataPoint;
import com.asetune.graph.TrendGraphDataPoint.LabelType;

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

	public static final String[] MON_TABLES       = new String[] {};
	public static final String[] NEED_ROLES       = new String[] {};
	public static final String[] NEED_CONFIG      = new String[] {};

	public static final String[] PCT_COLUMNS      = new String[] {};
	public static final String[] DIFF_COLUMNS     = new String[] {
		"DISTINCT_LOGINS",
		"CONNECTED_USERS",
		"COUNT_BLOCKING_WAIT",
		"MAX_BLOCKING_WAIT_IN_SEC",
		"TRANSACTIONS",
		"ROLLBACKS"
	};

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
	public static final String GRAPH_NAME_XXX                = "xxx";
	public static final String GRAPH_NAME_BLOCKING_LOCKS     = "BlockingLocksGraph";
	public static final String GRAPH_NAME_CONNECTION         = "ConnectionsGraph";   // String x=GetCounters.CM_GRAPH_NAME__SUMMARY__CONNECTION;
	public static final String GRAPH_NAME_OLDEST_TRAN_IN_SEC = "OldestTranInSecGraph";
	public static final String GRAPH_NAME_TRANSACTION        = "TransGraph";               // Transactions, Rollbacks

	private void addTrendGraphs()
	{
//		String[] labels_xxx              = new String[] { "Hour", "Minute", "Second"};
//		String[] labels_blockingLocks    = new String[] { "Blocking Locks" };
//		String[] labels_connection       = new String[] { "UserConnections (abs)", "distinctLogins (abs)" };
//		String[] labels_openTran         = new String[] { "Seconds" };
//		String[] labels_transaction      = new String[] { "user commits", "user rollbacks" };
//		
//		addTrendGraphData(GRAPH_NAME_XXX,                new TrendGraphDataPoint(GRAPH_NAME_XXX,                labels_xxx,           LabelType.Static));
//		addTrendGraphData(GRAPH_NAME_TRANSACTION,        new TrendGraphDataPoint(GRAPH_NAME_TRANSACTION,        labels_transaction,   LabelType.Dynamic));
//		addTrendGraphData(GRAPH_NAME_BLOCKING_LOCKS,     new TrendGraphDataPoint(GRAPH_NAME_BLOCKING_LOCKS,     labels_blockingLocks, LabelType.Static));
//		addTrendGraphData(GRAPH_NAME_CONNECTION,         new TrendGraphDataPoint(GRAPH_NAME_CONNECTION,         labels_connection,    LabelType.Static));
//		addTrendGraphData(GRAPH_NAME_OLDEST_TRAN_IN_SEC, new TrendGraphDataPoint(GRAPH_NAME_OLDEST_TRAN_IN_SEC, labels_openTran,      LabelType.Static));

		// GRAPH
		addTrendGraph(GRAPH_NAME_XXX,
			"Dummy Graph", 	                        // Menu CheckBox text
			"Dummy Graph showing hour, minute, second ("+GROUP_NAME+"->"+SHORT_NAME+")", // Label 
			TrendGraphDataPoint.Y_AXIS_SCALE_LABELS_NORMAL,
			new String[] { "Hour", "Minute", "Second"}, 
			LabelType.Static,
			TrendGraphDataPoint.Category.OTHER,
			true,  // is Percent Graph
			false, // visible at start
			0,     // graph is valid from Server Version. 0 = All Versions; >0 = Valid from this version and above 
			-1);   // minimum height

		addTrendGraph(GRAPH_NAME_TRANSACTION,
			"Transaction per second",    // Menu CheckBox text
			"Transaction per Second ("+GROUP_NAME+"->"+SHORT_NAME+")",    // Label 
			TrendGraphDataPoint.Y_AXIS_SCALE_LABELS_PERSEC,
			null, 
			LabelType.Dynamic,
			TrendGraphDataPoint.Category.OPERATIONS,
			false,   // is Percent Graph
			true,   // visible at start
			0, // graph is valid from Server Version. 0 = All Versions; >0 = Valid from this version and above 
			-1);     // minimum height

		addTrendGraph(GRAPH_NAME_BLOCKING_LOCKS,
			"Blocking Locks", 	                     // Menu CheckBox text
			"Number of Concurrently Blocking Locks ("+GROUP_NAME+"->"+SHORT_NAME+")", // Label 
			TrendGraphDataPoint.Y_AXIS_SCALE_LABELS_NORMAL,
			new String[] { "Blocking Locks" }, 
			LabelType.Static,
			TrendGraphDataPoint.Category.WAITS,
			false, // is Percent Graph
			true, // visible at start
			0,     // graph is valid from Server Version. 0 = All Versions; >0 = Valid from this version and above 
			-1);   // minimum height

		addTrendGraph(GRAPH_NAME_CONNECTION,
			"Connections/Users", 	          // Menu CheckBox text
			"Connections/Users connected to Oracle ("+GROUP_NAME+"->"+SHORT_NAME+")", // Label 
			TrendGraphDataPoint.Y_AXIS_SCALE_LABELS_NORMAL,
			new String[] { "UserConnections (abs)", "distinctLogins (abs)" }, 
			LabelType.Static,
			TrendGraphDataPoint.Category.OPERATIONS,
			false, // is Percent Graph
			true, // visible at start
			0,     // graph is valid from Server Version. 0 = All Versions; >0 = Valid from this version and above 
			-1);   // minimum height

		addTrendGraph(GRAPH_NAME_OLDEST_TRAN_IN_SEC,
			"Oldest Open Transaction",     // Menu CheckBox text
			"Oldest Open Transaction, in Seconds ("+GROUP_NAME+"->"+SHORT_NAME+")", // Label 
			TrendGraphDataPoint.Y_AXIS_SCALE_LABELS_SECONDS,
			new String[] { "Seconds" }, 
			LabelType.Static,
			TrendGraphDataPoint.Category.OPERATIONS,
			false, // is Percent Graph
			true, // visible at start
			0,     // graph is valid from Server Version. 0 = All Versions; >0 = Valid from this version and above 
			-1);   // minimum height
		
		
//		// if GUI
//		if (getGuiController() != null && getGuiController().hasGUI())
//		{
//			TrendGraph tg = null;
//
//			// GRAPH
//			tg = new TrendGraph(GRAPH_NAME_XXX,
////				"CPU Summary, Global Variables", 	                        // Menu CheckBox text
////				"CPU Summary for all Engines (using @@cpu_busy, @@cpu_io)", // Label 
//				"Dummy Graph", 	                        // Menu CheckBox text
//				"Dummy Graph showing hour, minute, second ("+GROUP_NAME+"->"+SHORT_NAME+")", // Label 
//				labels_xxx, 
//				true,  // is Percent Graph
//				this, 
//				false, // visible at start
//				0,     // graph is valid from Server Version. 0 = All Versions; >0 = Valid from this version and above 
//				-1);   // minimum height
//			addTrendGraph(tg.getName(), tg, true);
//
//			tg = new TrendGraph(GRAPH_NAME_TRANSACTION,
//				"Transaction per second",    // Menu CheckBox text
//				"Transaction per Second ("+GROUP_NAME+"->"+SHORT_NAME+")",    // Label 
//				labels_transaction, 
//				false,   // is Percent Graph
//				this, 
//				true,   // visible at start
//				0, // graph is valid from Server Version. 0 = All Versions; >0 = Valid from this version and above 
//				-1);     // minimum height
//			addTrendGraph(tg.getName(), tg, true);
//
//			tg = new TrendGraph(GRAPH_NAME_BLOCKING_LOCKS,
//				"Blocking Locks", 	                     // Menu CheckBox text
//				"Number of Concurrently Blocking Locks ("+GROUP_NAME+"->"+SHORT_NAME+")", // Label 
//				labels_blockingLocks, 
//				false, // is Percent Graph
//				this, 
//				true, // visible at start
//				0,     // graph is valid from Server Version. 0 = All Versions; >0 = Valid from this version and above 
//				-1);   // minimum height
//			addTrendGraph(tg.getName(), tg, true);
//
//			tg = new TrendGraph(GRAPH_NAME_CONNECTION,
//				"Connections/Users", 	          // Menu CheckBox text
//				"Connections/Users connected to Oracle ("+GROUP_NAME+"->"+SHORT_NAME+")", // Label 
//				labels_connection, 
//				false, // is Percent Graph
//				this, 
//				true, // visible at start
//				0,     // graph is valid from Server Version. 0 = All Versions; >0 = Valid from this version and above 
//				-1);   // minimum height
//			addTrendGraph(tg.getName(), tg, true);
//
//			tg = new TrendGraph(GRAPH_NAME_OLDEST_TRAN_IN_SEC,
//				"Oldest Open Transaction",     // Menu CheckBox text
//				"Oldest Open Transaction, in Seconds ("+GROUP_NAME+"->"+SHORT_NAME+")", // Label 
//				labels_openTran, 
//				false, // is Percent Graph
//				this, 
//				true, // visible at start
//				0,     // graph is valid from Server Version. 0 = All Versions; >0 = Valid from this version and above 
//				-1);   // minimum height
//			addTrendGraph(tg.getName(), tg, true);
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
		return 
				"select \n" +
	            "    INSTANCE_NAME, \n" +
	            "    HOST_NAME, \n" +
	            "    VERSION, \n" +
	            "    STARTUP_TIME, \n" +
	            "    CAST (current_date - STARTUP_TIME as INT)                                             as DAYS_RUNNING, \n" +
	            "    current_date                                                                          as TIME_NOW, \n" +
	            "    CAST(sys_extract_utc(SYSTIMESTAMP) AS DATE)                                           as UTC_DATE, \n" +
	            "    CAST( (24*60) * (current_date - CAST(sys_extract_utc(SYSTIMESTAMP) AS DATE)) as int)  as UTC_MINUTE_DIFF, \n" +
	            "    (select count(distinct USERNAME) from V$SESSION)                                      as DISTINCT_LOGINS, \n" +
	            "    (select count(*) from V$SESSION where USERNAME is not null)                           as CONNECTED_USERS, \n" +
	            "    CAST( (24*60*60) * (select current_date - min(START_DATE) from v$transaction) as int) as OLDEST_OPEN_TRAN_IN_SEC, \n" +
	            "    (select min(START_DATE) from v$transaction)                                           as OLDEST_OPEN_TRAN_TIME, \n" +
	            "    CAST( 30 as INT)                                                                      as OLDEST_OPEN_TRAN_IN_SEC_TH, \n" + // TH = THRESHOLD 
	            "    (select max(SECONDS_IN_WAIT) from v$session where BLOCKING_SESSION is not null)       as MAX_BLOCKING_WAIT_IN_SEC, \n" +
	            "    (select count(*) from v$session where BLOCKING_SESSION is not null)                   as COUNT_BLOCKING_WAIT, \n" +
	            "    CAST( 10 as INT)                                                                      as BLOCKING_WAIT_IN_SEC_TH, \n" +  // TH = THRESHOLD
	            "    (select sum(value) from v$sysstat where name = 'user commits')                        as TRANSACTIONS, \n" +
	            "    (select sum(value) from v$sysstat where name = 'user rollbacks')                      as ROLLBACKS \n" +
	            "from V$INSTANCE \n" +
	            "";
	}
	
	@Override
	public void updateGraphData(TrendGraphDataPoint tgdp)
	{
//		long srvVersion = getServerVersion();

		//---------------------------------
		// GRAPH:
		//---------------------------------
		if (GRAPH_NAME_XXX.equals(tgdp.getName()))
		{	
			Double[] arr = new Double[3];

			int ms = (int) (System.currentTimeMillis() % 1000l);
			ms = ms < 0 ? ms+1000 : ms;

			Calendar now = Calendar.getInstance();
			int hour   = now.get(Calendar.HOUR_OF_DAY);
			int minute = now.get(Calendar.MINUTE);
			int second = now.get(Calendar.SECOND);
			
//			arr[0] = this.getAbsValueAsDouble (0, "Connections");
//			arr[1] = this.getAbsValueAsDouble (0, "distinctLogins");
//			arr[2] = this.getDiffValueAsDouble(0, "aaConnections");
			arr[0] = new Double(hour);
			arr[1] = new Double(minute);
			arr[2] = new Double(second);
			_logger.debug("updateGraphData("+tgdp.getName()+"): hour='"+arr[0]+"', minute='"+arr[1]+"', second='"+arr[2]+"'.");

			// Set the values
			tgdp.setDataPoint(this.getTimestamp(), arr);
		}

		//---------------------------------
		// GRAPH:
		//---------------------------------
		if (GRAPH_NAME_TRANSACTION.equals(tgdp.getName()))
		{
			Double[] dArray = new Double[2];
			String[] lArray = new String[] { "user commits", "user rollbacks" };

			dArray[0] = this.getRateValueSum("TRANSACTIONS");
			dArray[1] = this.getRateValueSum("ROLLBACKS");
			_logger.debug("updateGraphData("+tgdp.getName()+"): Transactions='"+dArray[0]+"', Rollbacks='"+dArray[1]+"'.");

			// Set the values
			tgdp.setDataPoint(this.getTimestamp(), lArray, dArray);
		}

		//---------------------------------
		// GRAPH:
		//---------------------------------
		if (GRAPH_NAME_BLOCKING_LOCKS.equals(tgdp.getName()))
		{
			Double[] arr = new Double[1];

			arr[0] = this.getAbsValueAsDouble (0, "COUNT_BLOCKING_WAIT");
			_logger.debug("updateGraphData("+tgdp.getName()+"): LockWait='"+arr[0]+"'.");

			// Set the values
			tgdp.setDataPoint(this.getTimestamp(), arr);
		}

		//---------------------------------
		// GRAPH:
		//---------------------------------
		if (GRAPH_NAME_CONNECTION.equals(tgdp.getName()))
		{	
			Double[] arr = new Double[2];

			arr[0] = this.getAbsValueAsDouble (0, "CONNECTED_USERS");
			arr[1] = this.getAbsValueAsDouble (0, "DISTINCT_LOGINS");
			_logger.debug("updateGraphData("+tgdp.getName()+"): Connections(Abs)='"+arr[0]+"', distinctLogins(Abs)='"+arr[1]+"'.");

			// Set the values
			tgdp.setDataPoint(this.getTimestamp(), arr);
		}

		//---------------------------------
		// GRAPH:
		//---------------------------------
		if (GRAPH_NAME_OLDEST_TRAN_IN_SEC.equals(tgdp.getName()))
		{	
			Double[] arr = new Double[1];

			arr[0] = this.getAbsValueAsDouble(0, "OLDEST_OPEN_TRAN_IN_SEC");
			_logger.debug("updateGraphData("+tgdp.getName()+"): oldestOpenTranInSec='"+arr[0]+"'.");

			// Set the values
			tgdp.setDataPoint(this.getTimestamp(), arr);
		}
	}
}
