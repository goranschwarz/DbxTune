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
package com.asetune.cm.rs;

import java.sql.Connection;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

import javax.naming.NameNotFoundException;

import com.asetune.ICounterController;
import com.asetune.IGuiController;
import com.asetune.cm.CounterSetTemplates;
import com.asetune.cm.CounterSetTemplates.Type;
import com.asetune.cm.CountersModel;
import com.asetune.cm.rs.helper.RsDbidStripper;
import com.asetune.config.dict.MonTablesDictionary;
import com.asetune.config.dict.MonTablesDictionaryManager;
import com.asetune.graph.TrendGraphDataPoint;
import com.asetune.graph.TrendGraphDataPoint.LabelType;
import com.asetune.gui.MainFrame;

/**
 * @author Goran Schwarz (goran_schwarz@hotmail.com)
 */
public class CmAdminWhoDsi
extends CountersModel
{
//	private static Logger        _logger          = Logger.getLogger(CmAdminWhoDsi.class);
	private static final long    serialVersionUID = 1L;

	public static final String   CM_NAME          = CmAdminWhoDsi.class.getSimpleName();
	public static final String   SHORT_NAME       = "DSI";
	public static final String   HTML_DESC        = 
		"<html>" +
		"<p>Stable Queue Manager Statistics</p>" +
		"</html>";

	public static final String   GROUP_NAME       = MainFrame.TCP_GROUP_SERVER;
	public static final String   GUI_ICON_FILE    = "images/"+CM_NAME+".png";

	public static final long     NEED_SRV_VERSION = 0;
	public static final long     NEED_CE_VERSION  = 0;

	public static final String[] MON_TABLES       = new String[] {"dsi"};
	public static final String[] NEED_ROLES       = new String[] {};
	public static final String[] NEED_CONFIG      = new String[] {};

	public static final String[] PCT_COLUMNS      = new String[] {};
	public static final String[] DIFF_COLUMNS     = new String[] {
//		"Xact_retry_times",
//		"Cmd_batch_size",
//		"Xact_group_size",
//		"Max_cmds_to_log",
		"Xacts_read",
		"Xacts_ignored",
		"Xacts_skipped",
		"Xacts_succeeded",
		"Xacts_failed",
		"Xacts_retried",
//		"Current Origin DB",
		"Cmds_read",
		"Cmds_parsed_by_sqt",
		"Xacts_Sec_ignored",
//		"NumThreads",
//		"NumLargeThreads",
//		"LargeThreshold",
//		"CacheSize",
//		"Max_Xacts_in_group",
		"Xacts_retried_blk",
//		"CommitMaxChecks",
//		"CommitLogChecks",
//		"CommitCheckIntvl",
//		"RSTicket"
		};

	public static final boolean  NEGATIVE_DIFF_COUNTERS_TO_ZERO = true;
	public static final boolean  IS_SYSTEM_CM                   = true;
	public static final int      DEFAULT_POSTPONE_TIME          = 0;
	public static final int      DEFAULT_QUERY_TIMEOUT          = CountersModel.DEFAULT_sqlQueryTimeout;;

	@Override public int     getDefaultPostponeTime()                 { return DEFAULT_POSTPONE_TIME; }
	@Override public int     getDefaultQueryTimeout()                 { return DEFAULT_QUERY_TIMEOUT; }
	@Override public boolean getDefaultIsNegativeDiffCountersToZero() { return NEGATIVE_DIFF_COUNTERS_TO_ZERO; }
	@Override public Type    getTemplateLevel()                       { return Type.ALL; }

	/**
	 * FACTORY  method to create the object
	 */
	public static CountersModel create(ICounterController counterController, IGuiController guiController)
	{
		if (guiController != null && guiController.hasGUI())
			guiController.splashWindowProgress("Loading: Counter Model '"+CM_NAME+"'");

		return new CmAdminWhoDsi(counterController, guiController);
	}

	public CmAdminWhoDsi(ICounterController counterController, IGuiController guiController)
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
		
		addTrendGraphs();
		
		CounterSetTemplates.register(this);
	}


	//------------------------------------------------------------
	// Implementation
	//------------------------------------------------------------
	public static final String GRAPH_NAME_XACT_SUCCEEDED    = "DsiXactSucceeded";
	public static final String GRAPH_NAME_CMD_READ          = "DsiCmdRead";
	public static final String GRAPH_NAME_CMD_PARSED_BY_SQT = "DsiCmdParsedBySqt";

	private void addTrendGraphs()
	{
////		String[] labels = new String[] { "-added-at-runtime-" };
//		String[] labels = TrendGraphDataPoint.RUNTIME_REPLACED_LABELS;
//		
//		addTrendGraphData(GRAPH_NAME_XACT_SUCCEEDED,     new TrendGraphDataPoint(GRAPH_NAME_XACT_SUCCEEDED,    labels, LabelType.Dynamic));
//		addTrendGraphData(GRAPH_NAME_CMD_READ,           new TrendGraphDataPoint(GRAPH_NAME_CMD_READ,          labels, LabelType.Dynamic));
//		addTrendGraphData(GRAPH_NAME_CMD_PARSED_BY_SQT,  new TrendGraphDataPoint(GRAPH_NAME_CMD_PARSED_BY_SQT, labels, LabelType.Dynamic));

		//-----
		addTrendGraph(GRAPH_NAME_XACT_SUCCEEDED,
			"DSI: Number of Transactions Succeeded (col 'Xacts_succeeded', per second)", // Menu CheckBox text
			"DSI: Number of Transactions Succeeded (col 'Xacts_succeeded', per second)", // Label 
			TrendGraphDataPoint.Y_AXIS_SCALE_LABELS_PERSEC,
			null, 
			LabelType.Dynamic,
			TrendGraphDataPoint.Category.OPERATIONS,
			false, // is Percent Graph
			true,  // visible at start
			0,     // graph is valid from Server Version. 0 = All Versions; >0 = Valid from this version and above 
			-1);   // minimum height

		//-----
		addTrendGraph(GRAPH_NAME_CMD_READ,
			"DSI: Number of Commands Read (col 'Cmds_read', per second)", // Menu CheckBox text
			"DSI: Number of Commands Read (col 'Cmds_read', per second)", // Label 
			TrendGraphDataPoint.Y_AXIS_SCALE_LABELS_PERSEC,
			null, 
			LabelType.Dynamic,
			TrendGraphDataPoint.Category.OPERATIONS,
			false, // is Percent Graph
			false, // visible at start
			0,     // graph is valid from Server Version. 0 = All Versions; >0 = Valid from this version and above 
			-1);   // minimum height

		//-----
		addTrendGraph(GRAPH_NAME_CMD_PARSED_BY_SQT,
			"DSI: Number of Commands Parsed by SQT before being read by the DSI queue (col 'Cmds_parsed_by_sqt', per second)", // Menu CheckBox text
			"DSI: Number of Commands Parsed by SQT before being read by the DSI queue (col 'Cmds_parsed_by_sqt', per second)", // Label 
			TrendGraphDataPoint.Y_AXIS_SCALE_LABELS_PERSEC,
			null, 
			LabelType.Dynamic,
			TrendGraphDataPoint.Category.OPERATIONS,
			false, // is Percent Graph
			false, // visible at start
			0,     // graph is valid from Server Version. 0 = All Versions; >0 = Valid from this version and above 
			-1);   // minimum height
	
//		// if GUI
//		if (getGuiController() != null && getGuiController().hasGUI())
//		{
//			// GRAPH
//			TrendGraph tg = null;
//
//			//-----
//			tg = new TrendGraph(GRAPH_NAME_XACT_SUCCEEDED,
//				"DSI: Number of Transactions Succeeded (col 'Xacts_succeeded', per second)", // Menu CheckBox text
//				"DSI: Number of Transactions Succeeded (col 'Xacts_succeeded', per second)", // Label 
//				labels, 
//				false, // is Percent Graph
//				this, 
//				true,  // visible at start
//				0,     // graph is valid from Server Version. 0 = All Versions; >0 = Valid from this version and above 
//				-1);   // minimum height
//			addTrendGraph(tg.getName(), tg, true);
//
//			//-----
//			tg = new TrendGraph(GRAPH_NAME_CMD_READ,
//					"DSI: Number of Commands Read (col 'Cmds_read', per second)", // Menu CheckBox text
//					"DSI: Number of Commands Read (col 'Cmds_read', per second)", // Label 
//				labels, 
//				false, // is Percent Graph
//				this, 
//				false, // visible at start
//				0,     // graph is valid from Server Version. 0 = All Versions; >0 = Valid from this version and above 
//				-1);   // minimum height
//			addTrendGraph(tg.getName(), tg, true);
//
//			//-----
//			tg = new TrendGraph(GRAPH_NAME_CMD_PARSED_BY_SQT,
//					"DSI: Number of Commands Parsed by SQT before being read by the DSI queue (col 'Cmds_parsed_by_sqt', per second)", // Menu CheckBox text
//					"DSI: Number of Commands Parsed by SQT before being read by the DSI queue (col 'Cmds_parsed_by_sqt', per second)", // Label 
//				labels, 
//				false, // is Percent Graph
//				this, 
//				false, // visible at start
//				0,     // graph is valid from Server Version. 0 = All Versions; >0 = Valid from this version and above 
//				-1);   // minimum height
//			addTrendGraph(tg.getName(), tg, true);
//		}
	}

	private List<Integer> getValidRows()
	{
		ArrayList<Integer> list = new ArrayList<>(this.size()); 
		for (int i = 0; i < this.size(); i++)
		{
			// Records with an empty CurrentOriginQID, shouldn't be in the graph, since nothing has been replicated.
			String CurrentOriginQID = this.getAbsString(i, "Current Origin QID");
			if ( ! "0x000000000000000000000000000000000000000000000000000000000000000000000000".equals(CurrentOriginQID) )
				list.add(i);
		}
		return list;
	}

	@Override
	public void updateGraphData(TrendGraphDataPoint tgdp)
	{
		if (GRAPH_NAME_XACT_SUCCEEDED.equals(tgdp.getName()))
		{
			List<Integer> validRows = getValidRows();
			
			Double[] dArray = new Double[validRows.size()];
			String[] lArray = new String[dArray.length];
			for (int i = 0; i < dArray.length; i++)
			{
				lArray[i] = this.getRateString       (validRows.get(i), "Info");
				dArray[i] = this.getRateValueAsDouble(validRows.get(i), "Xacts_succeeded");

				// Remove DBID and append ('in-q' or 'out-q')
				lArray[i] = RsDbidStripper.stripDbid(lArray[i]); 
			}

			// Set the values
			tgdp.setDataPoint(this.getTimestamp(), lArray, dArray);
		}

		if (GRAPH_NAME_CMD_READ.equals(tgdp.getName()))
		{
			List<Integer> validRows = getValidRows();
			
			Double[] dArray = new Double[validRows.size()];
			String[] lArray = new String[dArray.length];
			for (int i = 0; i < dArray.length; i++)
			{
				lArray[i] = this.getRateString       (validRows.get(i), "Info");
				dArray[i] = this.getRateValueAsDouble(validRows.get(i), "Cmds_read");

				// Remove DBID and append ('in-q' or 'out-q')
				lArray[i] = RsDbidStripper.stripDbid(lArray[i]); 
			}

			// Set the values
			tgdp.setDataPoint(this.getTimestamp(), lArray, dArray);
		}

		if (GRAPH_NAME_CMD_PARSED_BY_SQT.equals(tgdp.getName()))
		{
			List<Integer> validRows = getValidRows();
			
			Double[] dArray = new Double[validRows.size()];
			String[] lArray = new String[dArray.length];
			for (int i = 0; i < dArray.length; i++)
			{
				lArray[i] = this.getRateString       (validRows.get(i), "Info");
				dArray[i] = this.getRateValueAsDouble(validRows.get(i), "Cmds_parsed_by_sqt");

				// Remove DBID and append ('in-q' or 'out-q')
				lArray[i] = RsDbidStripper.stripDbid(lArray[i]); 
			}

			// Set the values
			tgdp.setDataPoint(this.getTimestamp(), lArray, dArray);
		}
	}

//	@Override
//	protected TabularCntrPanel createGui()
//	{
//		return new CmRaSysmonPanel(this);
//	}

	@Override
	public String[] getDependsOnConfigForVersion(Connection conn, long srvVersion, boolean isClusterEnabled)
	{
		return NEED_CONFIG;
	}

	@Override
	public void addMonTableDictForVersion(Connection conn, long srvVersion, boolean isClusterEnabled)
	{
		try 
		{
			MonTablesDictionary mtd = MonTablesDictionaryManager.getInstance();
			mtd.addTable("dsi",  "");

			mtd.addColumn("dsi", "Spid",                  "<html>RepServer internal <i>thread id</i></html>");
			mtd.addColumn("dsi", "State",                 "<html>FIXME: State</html>");
			mtd.addColumn("dsi", "Info",                  "<html>FIXME: Info</html>");
			mtd.addColumn("dsi", "Maintenance User",      "<html>The login name of the maintenance user applying the transactions.</html>");
			mtd.addColumn("dsi", "Xact_retry_times",      "<html>The number of times a failed transaction is retried if the error action is RETRY_LOG or RETRY_STOP.</html>");
			mtd.addColumn("dsi", "Batch",                 "<html>Indicates if the batch option is on. If it is on, you can submit multiple commands as a batch to the data server.</html>");
			mtd.addColumn("dsi", "Cmd_batch_size",        "<html>The maximum size, in bytes, of a batch of output commands that you can send to the data server.</html>");
			mtd.addColumn("dsi", "Xact_group_size",       "<html>The maximum size, in bytes, of a transaction group consisting of source commands.</html>");
			mtd.addColumn("dsi", "Dump_load",             "<html>Indicates if the dump/load option is on. This configuration option coordinates dumps between primary and replicate databases.</html>");
			mtd.addColumn("dsi", "Max_cmds_to_log",       "<html>Maximum number of commands that can be logged into the exceptions log for a transaction. A value of -1 indicates an unlimited number of commands.</html>");
			mtd.addColumn("dsi", "Xacts_read",            "<html>The number of transactions read by the DSI from the outbound stable queue. This number should increase as the DSI applies transactions. You can use the information to monitor the rate of activity.</html>");
			mtd.addColumn("dsi", "Xacts_ignored",         "<html>The number of transactions determined to be duplicates. Typically, some transactions are ignored at start-up time because they were applied previously. Deletes from the DSI queue are delayed, so at start-up time, duplicates are detected and ignored. If you see a large number of ignored transactions, there is a chance that the rs_lastcommit table is corrupted. For more information, refer to the Replication Server Troubleshooting Guide.</html>");
			mtd.addColumn("dsi", "Xacts_skipped",         "<html>The number of transactions skipped by resuming the connection with skip first transaction.</html>");
			mtd.addColumn("dsi", "Xacts_succeeded",       "<html>The number of transactions applied successfully against the database.</html>");
			mtd.addColumn("dsi", "Xacts_failed",          "<html>The number of transactions that failed. Depending on the error mapping, some transactions may be written into the exceptions log. You should inspect the exceptions log.</html>");
			mtd.addColumn("dsi", "Xacts_retried",         "<html>The number of transactions that were retried.</html>");
			mtd.addColumn("dsi", "Current Origin DB",     "<html>The origin database ID for the current transaction.</html>");
			mtd.addColumn("dsi", "Current Origin QID",    "<html>If the state is Active, it is the Origin Queue ID of the begin log record of the transaction being processed. Otherwise, it is the Origin Queue ID of the begin log record of the last transaction processed.</html>");
			mtd.addColumn("dsi", "Subscription Name",     "<html>If the thread is processing a subscription, this is the name of the subscription.</html>");
			mtd.addColumn("dsi", "Sub Command",           "<html>If the thread is processing a subscription, this is the subscription command: activate, validate, drop, or unknown.</html>");
			mtd.addColumn("dsi", "Current Secondary QID", "<html>If the thread is processing an atomic subscription applied incrementally, this column holds the queue ID of the current transaction.</html>");
			mtd.addColumn("dsi", "Cmds_read",             "<html>The number of commands read from the DSI queue.</html>");
			mtd.addColumn("dsi", "Cmds_parsed_by_sqt",    "<html>The number of commands parsed by SQT before being read by the DSI queue.</html>");
			mtd.addColumn("dsi", "IgnoringStatus",        "<html>Contains “Ignoring” if the DSI is ignoring transactions while waiting for a marker. Contains “Applying” if the DSI is executing transactions in the database.</html>");
			mtd.addColumn("dsi", "Xacts_Sec_ignored",     "<html>In a warm standby application, the number of transactions that were ignored after the switchover.</html>");
			mtd.addColumn("dsi", "GroupingStatus",        "<html>Contains “on” if the DSI is executing transactions in groups. Contains “off” if the DSI is executing transactions one at a time.</html>");
			mtd.addColumn("dsi", "TriggerStatus",         "<html>Contains “on” if set triggers is on. Contains “off” if set triggers is off.</html>");
			mtd.addColumn("dsi", "ReplStatus",            "<html>Indicates whether the Replication Server replicates transactions in the database. The default is “off” for standby databases. The default is “on” for all other databases.</html>");
			mtd.addColumn("dsi", "NumThreads",            "<html>The number of parallel DSI threads in use.</html>");
			mtd.addColumn("dsi", "NumLargeThreads",       "<html>The number of parallel DSI threads reserved for use with large transactions.</html>");
			mtd.addColumn("dsi", "LargeThreshold",        "<html>In a parallel DSI configuration, the number of commands allowed in a transaction before it is considered large.</html>");
			mtd.addColumn("dsi", "CacheSize",             "<html>The maximum SQT cache memory for the database connection, in bytes. The default, 0, means that the current setting of the sqt_max_cache_size parameter is used as the maximum SQT cache memory.</html>");
			mtd.addColumn("dsi", "Serialization",         "<html>The method used to maintain serial consistency when parallel DSI threads are used.</html>");
			mtd.addColumn("dsi", "Max_Xacts_in_group",    "<html>The maximum number of transactions in a group. The default is 20. You can configure this number using the alter connection command.</html>");
			mtd.addColumn("dsi", "Xacts_retried_blk",     "<html>The number of times the DSI rolled back a transaction due to exceeding maximum number of checks for lock contention.</html>");
			mtd.addColumn("dsi", "CommitControl",         "<html>Indicates if commit control is internal or external. Set to true if internal.</html>");
			mtd.addColumn("dsi", "CommitMaxChecks",       "<html>Indicates the maximum number of lock contention attempts before rolling back transaction and retrying.</html>");
			mtd.addColumn("dsi", "CommitLogChecks",       "<html>Indicates the maximum number of lock contention attempts before logging a message.</html>");
			mtd.addColumn("dsi", "CommitCheckIntvl",      "<html>Amount of time, in milliseconds, a transaction waits before issuing a check for lock contention.</html>");
			mtd.addColumn("dsi", "IsolationLevel",        "<html>Database isolation level for DSI connection.</html>");
			mtd.addColumn("dsi", "RSTicket",              "<html>The number of rs_ticket subcommands that have been processed by a DSI queue manager, if the Replication Server stats_sampling parameter is “on”.<br>The default, 0, means that the current setting of sqt_max_cache_size is used as the maximum cache size for the connection.</html>");
			mtd.addColumn("dsi", "dsi_rs_ticket_report", "<html>Determines whether to call function string rs_ticket_report. rs_ticket_report function string is invoked when dsi_rs_ticket_report is set to on.<br>Default: off</html>");
		}
		catch (NameNotFoundException e) {/*ignore*/}
	}

	@Override
	public List<String> getPkForVersion(Connection conn, long srvVersion, boolean isClusterEnabled)
	{
		List <String> pkCols = new LinkedList<String>();

		pkCols.add("Spid");

		return pkCols;
	}

	@Override
	public String getSqlForVersion(Connection conn, long srvVersion, boolean isClusterEnabled)
	{
		String sql = "admin who, dsi, no_trunc ";
		return sql;
	}
}
