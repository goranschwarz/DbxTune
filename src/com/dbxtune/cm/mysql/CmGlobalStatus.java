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
package com.dbxtune.cm.mysql;

import java.awt.event.MouseEvent;
import java.util.LinkedList;
import java.util.List;

import javax.naming.NameNotFoundException;

import com.dbxtune.ICounterController;
import com.dbxtune.IGuiController;
import com.dbxtune.central.pcs.CentralPersistReader;
import com.dbxtune.cm.CounterSetTemplates;
import com.dbxtune.cm.CounterSetTemplates.Type;
import com.dbxtune.cm.CountersModel;
import com.dbxtune.config.dict.MonTablesDictionary;
import com.dbxtune.config.dict.MonTablesDictionaryManager;
import com.dbxtune.config.dict.MySqlVariablesDictionary;
import com.dbxtune.graph.TrendGraphDataPoint;
import com.dbxtune.graph.TrendGraphDataPoint.LabelType;
import com.dbxtune.gui.MainFrame;
import com.dbxtune.sql.conn.DbxConnection;
import com.dbxtune.sql.conn.info.DbmsVersionInfo;

/**
 * @author Goran Schwarz (goran_schwarz@hotmail.com)
 */
public class CmGlobalStatus
extends CountersModel
{
	private static final long    serialVersionUID = 1L;

	public static final String   CM_NAME          = CmGlobalStatus.class.getSimpleName();
	public static final String   SHORT_NAME       = "Global Status";
	public static final String   HTML_DESC        = 
		"<html>"
		+ "<h4>Global Status</h4>"
		+ "Simply select from performance_schema.global_status"
		+ "</html>";

	public static final String   GROUP_NAME       = MainFrame.TCP_GROUP_SERVER;
	public static final String   GUI_ICON_FILE    = "images/"+CM_NAME+".png";

	public static final long     NEED_SRV_VERSION = 0;
	public static final long     NEED_CE_VERSION  = 0;

	public static final String[] MON_TABLES       = new String[] {"global_status"};
	public static final String[] NEED_ROLES       = new String[] {};
	public static final String[] NEED_CONFIG      = new String[] {};

	public static final String[] PCT_COLUMNS      = new String[] {};
	public static final String[] DIFF_COLUMNS     = new String[] {"VARIABLE_VALUE"};

	public static final boolean  NEGATIVE_DIFF_COUNTERS_TO_ZERO = true;
	public static final boolean  IS_SYSTEM_CM                   = true;
	public static final int      DEFAULT_POSTPONE_TIME          = 0;
	public static final int      DEFAULT_QUERY_TIMEOUT          = CountersModel.DEFAULT_sqlQueryTimeout;

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

		return new CmGlobalStatus(counterController, guiController);
	}

	public CmGlobalStatus(ICounterController counterController, IGuiController guiController)
	{
		super(counterController, guiController,
				CM_NAME, GROUP_NAME, /*sql*/null, /*pkList*/null, 
				DIFF_COLUMNS, PCT_COLUMNS, MON_TABLES, 
				NEED_ROLES, NEED_CONFIG, NEED_SRV_VERSION, NEED_CE_VERSION, 
				NEGATIVE_DIFF_COUNTERS_TO_ZERO, IS_SYSTEM_CM, DEFAULT_POSTPONE_TIME);

		setDisplayName(SHORT_NAME);
		setDescription(HTML_DESC);

		setIconFile(GUI_ICON_FILE);

		setShowClearTime(false);
		setBackgroundDataPollingEnabled(true, false);
		
		setCounterController(counterController);
		setGuiController(guiController);
		
		addTrendGraphs();
		
		CounterSetTemplates.register(this);
	}


	//------------------------------------------------------------
	// Implementation
	//------------------------------------------------------------
	
//	@Override
//	protected TabularCntrPanel createGui()
//	{
//		return new CmGlobalStatusPanel(this);
//	}

	// NOTE: storage table name will be CmName_GraphName, so try to keep the name short
	public static final String GRAPH_NAME_KB_RECV_SENT          = "ClRecvSent"; 
	public static final String GRAPH_NAME_QUESTIONS             = "Questions"; 
	public static final String GRAPH_NAME_INNODB_PAGES          = "IdbPages"; 
	public static final String GRAPH_NAME_SLOW_QUERIES          = "SlowQueries"; 
	public static final String GRAPH_NAME_SLOW_QUERIES_COUNT    = "SlowQCount"; 
	public static final String GRAPH_NAME_INNODB_ROWS_IUD       = "IdbRowsIud";
	public static final String GRAPH_NAME_INNODB_ROWS_READ      = "IdbRowsRead";
	public static final String GRAPH_NAME_INNODB_ROW_LOCK_WAIT  = "IdbRowsLWait";
	public static final String GRAPH_NAME_INNODB_BP_WAIT_FREE   = "IdbBpWaitFree";
	public static final String GRAPH_NAME_OPEN_TABLES           = "OpenTables";
	public static final String GRAPH_NAME_CONNECTIONS           = "Connections";
	public static final String GRAPH_NAME_ABORTED_CONNECTIONS   = "AbortedConn";
	public static final String GRAPH_NAME_TRANSACTIONS          = "Transactions";
	public static final String GRAPH_NAME_HANDLER_READS         = "HandlerReads";
	public static final String GRAPH_NAME_HANDLER_IUD           = "HandlerIud";
	public static final String GRAPH_NAME_TMP_TABLES            = "TmpTables";
	
	
	private void addTrendGraphs()
	{
		// GRAPH
		addTrendGraph(GRAPH_NAME_KB_RECV_SENT,
			"Connections, KBytes Received/Sent per sec", // Menu CheckBox text
			"Connections, KBytes Received/Sent per sec ("+GROUP_NAME+"->"+SHORT_NAME+")", // Label 
			TrendGraphDataPoint.createGraphProps(TrendGraphDataPoint.Y_AXIS_SCALE_LABELS_KB, CentralPersistReader.SampleType.MAX_OVER_SAMPLES),
			new String[] { "KB Received [Bytes_received/1024]", "KB Sent [Bytes_sent/1024]" }, 
			LabelType.Static,
			TrendGraphDataPoint.Category.NETWORK,
			false, // is Percent Graph
			true,  // visible at start
			0,     // graph is valid from Server Version. 0 = All Versions; >0 = Valid from this version and above 
			-1);   // minimum height
		
		addTrendGraph(GRAPH_NAME_QUESTIONS,
			"Number of Statements per sec", // Menu CheckBox text
			"Number of Statements per sec ("+GROUP_NAME+"->"+SHORT_NAME+")", // Label 
			TrendGraphDataPoint.createGraphProps(TrendGraphDataPoint.Y_AXIS_SCALE_LABELS_PERSEC, CentralPersistReader.SampleType.MAX_OVER_SAMPLES),
			new String[] { "Client Statements [Questions]", "Client & Internal Statements [Queries]" }, 
			LabelType.Static,
			TrendGraphDataPoint.Category.OPERATIONS,
			false, // is Percent Graph
			true,  // visible at start
			0,     // graph is valid from Server Version. 0 = All Versions; >0 = Valid from this version and above 
			-1);   // minimum height
		
		addTrendGraph(GRAPH_NAME_INNODB_PAGES,
			"InnoDB Pages per sec", // Menu CheckBox text
			"InnoDB Pages per sec ("+GROUP_NAME+"->"+SHORT_NAME+")", // Label 
			TrendGraphDataPoint.createGraphProps(TrendGraphDataPoint.Y_AXIS_SCALE_LABELS_PERSEC, CentralPersistReader.SampleType.MAX_OVER_SAMPLES),
			new String[] { "Innodb_pages_created", "Innodb_pages_read", "Innodb_pages_written" }, 
			LabelType.Static,
			TrendGraphDataPoint.Category.DISK,
			false, // is Percent Graph
			true,  // visible at start
			0,     // graph is valid from Server Version. 0 = All Versions; >0 = Valid from this version and above 
			-1);   // minimum height
		
		addTrendGraph(GRAPH_NAME_SLOW_QUERIES,
			"Slow Queries per sec", // Menu CheckBox text
			"Slow Queries per sec ("+GROUP_NAME+"->"+SHORT_NAME+")", // Label 
			TrendGraphDataPoint.createGraphProps(TrendGraphDataPoint.Y_AXIS_SCALE_LABELS_PERSEC, CentralPersistReader.SampleType.MAX_OVER_SAMPLES),
			new String[] { "Slow_queries (diff)", "Slow_queries (rate)" }, 
			LabelType.Static,
			TrendGraphDataPoint.Category.OPERATIONS,
			false, // is Percent Graph
			true,  // visible at start
			0,     // graph is valid from Server Version. 0 = All Versions; >0 = Valid from this version and above 
			-1);   // minimum height

		addTrendGraph(GRAPH_NAME_SLOW_QUERIES_COUNT,
			"Slow Queries Count", // Menu CheckBox text
			"Slow Queries Count ("+GROUP_NAME+"->"+SHORT_NAME+")", // Label 
			TrendGraphDataPoint.createGraphProps(TrendGraphDataPoint.Y_AXIS_SCALE_LABELS_NORMAL, CentralPersistReader.SampleType.MAX_OVER_SAMPLES),
			new String[] { "Slow_queries (abs)" }, 
			LabelType.Static,
			TrendGraphDataPoint.Category.OPERATIONS,
			false, // is Percent Graph
			true,  // visible at start
			0,     // graph is valid from Server Version. 0 = All Versions; >0 = Valid from this version and above 
			-1);   // minimum height

		addTrendGraph(GRAPH_NAME_INNODB_ROWS_IUD,
			"InnoDB Rows Ins/Upd/Del per sec", // Menu CheckBox text
			"InnoDB Rows Ins/Upd/Del per sec ("+GROUP_NAME+"->"+SHORT_NAME+")", // Label 
			TrendGraphDataPoint.createGraphProps(TrendGraphDataPoint.Y_AXIS_SCALE_LABELS_PERSEC, CentralPersistReader.SampleType.MAX_OVER_SAMPLES),
			new String[] { "Innodb_rows_inserted", "Innodb_rows_updated", "Innodb_rows_deleted" }, 
			LabelType.Static,
			TrendGraphDataPoint.Category.OPERATIONS,
			false, // is Percent Graph
			true,  // visible at start
			0,     // graph is valid from Server Version. 0 = All Versions; >0 = Valid from this version and above 
			-1);   // minimum height

		addTrendGraph(GRAPH_NAME_INNODB_ROWS_READ,
			"InnoDB Rows Read per sec", // Menu CheckBox text
			"InnoDB Rows Read per sec ("+GROUP_NAME+"->"+SHORT_NAME+")", // Label 
			TrendGraphDataPoint.createGraphProps(TrendGraphDataPoint.Y_AXIS_SCALE_LABELS_PERSEC, CentralPersistReader.SampleType.MAX_OVER_SAMPLES),
			new String[] { "Innodb_rows_read" }, 
			LabelType.Static,
			TrendGraphDataPoint.Category.OPERATIONS,
			false, // is Percent Graph
			true,  // visible at start
			0,     // graph is valid from Server Version. 0 = All Versions; >0 = Valid from this version and above 
			-1);   // minimum height

		addTrendGraph(GRAPH_NAME_INNODB_ROW_LOCK_WAIT,
			"InnoDB Row Lock Wait per sec", // Menu CheckBox text
			"InnoDB Row Lock Wait per sec ("+GROUP_NAME+"->"+SHORT_NAME+")", // Label 
			TrendGraphDataPoint.createGraphProps(TrendGraphDataPoint.Y_AXIS_SCALE_LABELS_PERSEC, CentralPersistReader.SampleType.MAX_OVER_SAMPLES),
			new String[] { "Innodb_row_lock_waits" }, 
			LabelType.Static,
			TrendGraphDataPoint.Category.WAITS,
			false, // is Percent Graph
			true,  // visible at start
			0,     // graph is valid from Server Version. 0 = All Versions; >0 = Valid from this version and above 
			-1);   // minimum height

		addTrendGraph(GRAPH_NAME_INNODB_BP_WAIT_FREE,
			"InnoDB BufferPool Wait Free Count", // Menu CheckBox text
			"InnoDB BufferPool Wait Free Count ("+GROUP_NAME+"->"+SHORT_NAME+")", // Label 
			TrendGraphDataPoint.createGraphProps(TrendGraphDataPoint.Y_AXIS_SCALE_LABELS_NORMAL, CentralPersistReader.SampleType.MAX_OVER_SAMPLES),
			new String[] { "Innodb_buffer_pool_wait_free" }, 
			LabelType.Static,
			TrendGraphDataPoint.Category.WAITS,
			false, // is Percent Graph
			true,  // visible at start
			0,     // graph is valid from Server Version. 0 = All Versions; >0 = Valid from this version and above 
			-1);   // minimum height

		addTrendGraph(GRAPH_NAME_OPEN_TABLES,
			"Open Table Count", // Menu CheckBox text
			"Open Table Count ("+GROUP_NAME+"->"+SHORT_NAME+")", // Label 
			TrendGraphDataPoint.createGraphProps(TrendGraphDataPoint.Y_AXIS_SCALE_LABELS_NORMAL, CentralPersistReader.SampleType.MAX_OVER_SAMPLES),
			new String[] { "Open_tables" }, 
			LabelType.Static,
			TrendGraphDataPoint.Category.OPERATIONS,
			false, // is Percent Graph
			true,  // visible at start
			0,     // graph is valid from Server Version. 0 = All Versions; >0 = Valid from this version and above 
			-1);   // minimum height

		addTrendGraph(GRAPH_NAME_CONNECTIONS,
			"Client Connection Count", // Menu CheckBox text
			"Client Connection Count ("+GROUP_NAME+"->"+SHORT_NAME+")", // Label 
			TrendGraphDataPoint.createGraphProps(TrendGraphDataPoint.Y_AXIS_SCALE_LABELS_NORMAL, CentralPersistReader.SampleType.MAX_OVER_SAMPLES),
			new String[] { "Threads_connected" }, 
			LabelType.Static,
			TrendGraphDataPoint.Category.OPERATIONS,
			false, // is Percent Graph
			true,  // visible at start
			0,     // graph is valid from Server Version. 0 = All Versions; >0 = Valid from this version and above 
			-1);   // minimum height

		addTrendGraph(GRAPH_NAME_ABORTED_CONNECTIONS,
			"Aborted Connections per sec", // Menu CheckBox text
			"Aborted Connections per sec ("+GROUP_NAME+"->"+SHORT_NAME+")", // Label 
			TrendGraphDataPoint.createGraphProps(TrendGraphDataPoint.Y_AXIS_SCALE_LABELS_PERSEC, CentralPersistReader.SampleType.MAX_OVER_SAMPLES),
			new String[] { "Aborted_connects" }, 
			LabelType.Static,
			TrendGraphDataPoint.Category.OPERATIONS,
			false, // is Percent Graph
			true,  // visible at start
			0,     // graph is valid from Server Version. 0 = All Versions; >0 = Valid from this version and above 
			-1);   // minimum height

		addTrendGraph(GRAPH_NAME_TRANSACTIONS,
			"Transactions per sec", // Menu CheckBox text
			"Transactions per sec ("+GROUP_NAME+"->"+SHORT_NAME+")", // Label 
			TrendGraphDataPoint.createGraphProps(TrendGraphDataPoint.Y_AXIS_SCALE_LABELS_PERSEC, CentralPersistReader.SampleType.MAX_OVER_SAMPLES),
			new String[] { "Handler_commit", "Handler_rollback", "Handler_savepoint", "Handler_savepoint_rollback" }, 
			LabelType.Static,
			TrendGraphDataPoint.Category.OPERATIONS,
			false, // is Percent Graph
			true,  // visible at start
			0,     // graph is valid from Server Version. 0 = All Versions; >0 = Valid from this version and above 
			-1);   // minimum height

		addTrendGraph(GRAPH_NAME_HANDLER_READS,
			"Handler Reads per sec", // Menu CheckBox text
			"Handler Reads per sec ("+GROUP_NAME+"->"+SHORT_NAME+")", // Label 
			TrendGraphDataPoint.createGraphProps(TrendGraphDataPoint.Y_AXIS_SCALE_LABELS_PERSEC, CentralPersistReader.SampleType.MAX_OVER_SAMPLES),
			new String[] { "Handler_read_first", "Handler_read_key", "Handler_read_last", "Handler_read_next", "Handler_read_prev", "Handler_read_rnd", "Handler_read_rnd_next", "Handler_mrr_init" }, 
			LabelType.Static,
			TrendGraphDataPoint.Category.OPERATIONS,
			false, // is Percent Graph
			true,  // visible at start
			0,     // graph is valid from Server Version. 0 = All Versions; >0 = Valid from this version and above 
			-1);   // minimum height

		addTrendGraph(GRAPH_NAME_HANDLER_IUD,
			"Handler Ins/Upd/Del per sec", // Menu CheckBox text
			"Handler Ins/Upd/Del per sec ("+GROUP_NAME+"->"+SHORT_NAME+")", // Label 
			TrendGraphDataPoint.createGraphProps(TrendGraphDataPoint.Y_AXIS_SCALE_LABELS_PERSEC, CentralPersistReader.SampleType.MAX_OVER_SAMPLES),
			new String[] { "Handler_write", "Handler_update", "Handler_delete" }, 
			LabelType.Static,
			TrendGraphDataPoint.Category.OPERATIONS,
			false, // is Percent Graph
			true,  // visible at start
			0,     // graph is valid from Server Version. 0 = All Versions; >0 = Valid from this version and above 
			-1);   // minimum height

		addTrendGraph(GRAPH_NAME_TMP_TABLES,
			"Temporary Tables per sec", // Menu CheckBox text
			"Temporary Tables per sec ("+GROUP_NAME+"->"+SHORT_NAME+")", // Label 
			TrendGraphDataPoint.createGraphProps(TrendGraphDataPoint.Y_AXIS_SCALE_LABELS_PERSEC, CentralPersistReader.SampleType.MAX_OVER_SAMPLES),
			new String[] { "Created_tmp_disk_tables", "Created_tmp_tables", "Created_tmp_files" }, 
			LabelType.Static,
			TrendGraphDataPoint.Category.OPERATIONS,
			false, // is Percent Graph
			true,  // visible at start
			0,     // graph is valid from Server Version. 0 = All Versions; >0 = Valid from this version and above 
			-1);   // minimum height
		
	}

	@Override
	public void updateGraphData(TrendGraphDataPoint tgdp)
	{
		if (GRAPH_NAME_KB_RECV_SENT.equals(tgdp.getName()))
		{	
			Double[] arr = new Double[2];

			arr[0] = this.getRateValueAsDouble("Bytes_received", "VARIABLE_VALUE", 0d) / 1024.0;
			arr[1] = this.getRateValueAsDouble("Bytes_sent",     "VARIABLE_VALUE", 0d) / 1024.0;
			
			// Set the values
			tgdp.setDataPoint(this.getTimestamp(), arr);
		}
		
		if (GRAPH_NAME_QUESTIONS.equals(tgdp.getName()))
		{	
			Double[] arr = new Double[2];

			arr[0] = this.getRateValueAsDouble("Questions", "VARIABLE_VALUE");
			arr[1] = this.getRateValueAsDouble("Queries",   "VARIABLE_VALUE");

			// Set the values
			tgdp.setDataPoint(this.getTimestamp(), arr);
		}

		if (GRAPH_NAME_INNODB_PAGES.equals(tgdp.getName()))
		{	
			Double[] arr = new Double[3];

			arr[0] = this.getRateValueAsDouble("Innodb_pages_created", "VARIABLE_VALUE");
			arr[1] = this.getRateValueAsDouble("Innodb_pages_read",    "VARIABLE_VALUE");
			arr[2] = this.getRateValueAsDouble("Innodb_pages_written", "VARIABLE_VALUE");

			// Set the values
			tgdp.setDataPoint(this.getTimestamp(), arr);
		}

		if (GRAPH_NAME_SLOW_QUERIES.equals(tgdp.getName()))
		{	
			Double[] arr = new Double[2];

			arr[0] = this.getDiffValueAsDouble("Slow_queries", "VARIABLE_VALUE"); // DIFF
			arr[1] = this.getRateValueAsDouble("Slow_queries", "VARIABLE_VALUE"); // RATE

			// Set the values
			tgdp.setDataPoint(this.getTimestamp(), arr);
		}

		if (GRAPH_NAME_SLOW_QUERIES_COUNT.equals(tgdp.getName()))
		{	
			Double[] arr = new Double[1];

			arr[0] = this.getAbsValueAsDouble("Slow_queries", "VARIABLE_VALUE");

			// Set the values
			tgdp.setDataPoint(this.getTimestamp(), arr);
		}

		if (GRAPH_NAME_INNODB_ROWS_IUD.equals(tgdp.getName()))
		{	
			Double[] arr = new Double[3];

			arr[0] = this.getRateValueAsDouble("Innodb_rows_inserted", "VARIABLE_VALUE");
			arr[1] = this.getRateValueAsDouble("Innodb_rows_updated",  "VARIABLE_VALUE");
			arr[2] = this.getRateValueAsDouble("Innodb_rows_deleted",  "VARIABLE_VALUE");

			// Set the values
			tgdp.setDataPoint(this.getTimestamp(), arr);
		}

		if (GRAPH_NAME_INNODB_ROWS_READ.equals(tgdp.getName()))
		{	
			Double[] arr = new Double[1];

			arr[0] = this.getRateValueAsDouble("Innodb_rows_read",     "VARIABLE_VALUE");

			// Set the values
			tgdp.setDataPoint(this.getTimestamp(), arr);
		}

		if (GRAPH_NAME_INNODB_ROW_LOCK_WAIT.equals(tgdp.getName()))
		{	
			Double[] arr = new Double[1];

			arr[0] = this.getRateValueAsDouble("Innodb_row_lock_waits", "VARIABLE_VALUE");

			// Set the values
			tgdp.setDataPoint(this.getTimestamp(), arr);
		}

		if (GRAPH_NAME_INNODB_BP_WAIT_FREE.equals(tgdp.getName()))
		{	
			Double[] arr = new Double[1];

			arr[0] = this.getAbsValueAsDouble("Innodb_buffer_pool_wait_free", "VARIABLE_VALUE");

			// Set the values
			tgdp.setDataPoint(this.getTimestamp(), arr);
		}

		if (GRAPH_NAME_OPEN_TABLES.equals(tgdp.getName()))
		{	
			Double[] arr = new Double[1];

			arr[0] = this.getAbsValueAsDouble("Open_tables", "VARIABLE_VALUE");

			// Set the values
			tgdp.setDataPoint(this.getTimestamp(), arr);
		}

		if (GRAPH_NAME_CONNECTIONS.equals(tgdp.getName()))
		{	
			Double[] arr = new Double[1];

			arr[0] = this.getAbsValueAsDouble("Threads_connected", "VARIABLE_VALUE");

			// Set the values
			tgdp.setDataPoint(this.getTimestamp(), arr);
		}

		if (GRAPH_NAME_ABORTED_CONNECTIONS.equals(tgdp.getName()))
		{	
			Double[] arr = new Double[1];

			arr[0] = this.getRateValueAsDouble("Aborted_connects", "VARIABLE_VALUE");

			// Set the values
			tgdp.setDataPoint(this.getTimestamp(), arr);
		}

		if (GRAPH_NAME_TRANSACTIONS.equals(tgdp.getName()))
		{	
			Double[] arr = new Double[4];

			arr[0] = this.getRateValueAsDouble("Handler_commit",             "VARIABLE_VALUE");
			arr[1] = this.getRateValueAsDouble("Handler_rollback",           "VARIABLE_VALUE");
			arr[2] = this.getRateValueAsDouble("Handler_savepoint",          "VARIABLE_VALUE");
			arr[3] = this.getRateValueAsDouble("Handler_savepoint_rollback", "VARIABLE_VALUE");

			// Set the values
			tgdp.setDataPoint(this.getTimestamp(), arr);
		}

		if (GRAPH_NAME_HANDLER_READS.equals(tgdp.getName()))
		{	
			Double[] arr = new Double[8];

			arr[0] = this.getRateValueAsDouble("Handler_read_first",    "VARIABLE_VALUE");
			arr[1] = this.getRateValueAsDouble("Handler_read_key",      "VARIABLE_VALUE");
			arr[2] = this.getRateValueAsDouble("Handler_read_last",     "VARIABLE_VALUE");
			arr[3] = this.getRateValueAsDouble("Handler_read_next",     "VARIABLE_VALUE");
			arr[4] = this.getRateValueAsDouble("Handler_read_prev",     "VARIABLE_VALUE");
			arr[5] = this.getRateValueAsDouble("Handler_read_rnd",      "VARIABLE_VALUE");
			arr[6] = this.getRateValueAsDouble("Handler_read_rnd_next", "VARIABLE_VALUE");
			arr[7] = this.getRateValueAsDouble("Handler_mrr_init",      "VARIABLE_VALUE");

			// Set the values
			tgdp.setDataPoint(this.getTimestamp(), arr);
		}

		if (GRAPH_NAME_HANDLER_IUD.equals(tgdp.getName()))
		{	
			Double[] arr = new Double[3];

			arr[0] = this.getRateValueAsDouble("Handler_write",  "VARIABLE_VALUE");
			arr[1] = this.getRateValueAsDouble("Handler_update", "VARIABLE_VALUE");
			arr[2] = this.getRateValueAsDouble("Handler_delete", "VARIABLE_VALUE");

			// Set the values
			tgdp.setDataPoint(this.getTimestamp(), arr);
		}

		if (GRAPH_NAME_TMP_TABLES.equals(tgdp.getName()))
		{	
			Double[] arr = new Double[3];

			arr[0] = this.getRateValueAsDouble("Created_tmp_disk_tables", "VARIABLE_VALUE");
			arr[1] = this.getRateValueAsDouble("Created_tmp_tables",      "VARIABLE_VALUE");
			arr[2] = this.getRateValueAsDouble("Created_tmp_files",       "VARIABLE_VALUE");

			// Set the values
			tgdp.setDataPoint(this.getTimestamp(), arr);
		}
	}


	@Override
	public String[] getDependsOnConfigForVersion(DbxConnection conn, DbmsVersionInfo versionInfo)
	{
		return NEED_CONFIG;
	}

	@Override
	public void addMonTableDictForVersion(DbxConnection conn, DbmsVersionInfo versionInfo)
	{
		try 
		{
			MonTablesDictionary mtd = MonTablesDictionaryManager.getInstance();
			mtd.addTable("global_status",  "Global status.");

			mtd.addColumn("global_status", "VARIABLE_NAME",  "<html>Name of the variable.</html>");
			mtd.addColumn("global_status", "VARIABLE_VALUE", "<html>Value of the variable.</html>");
		}
		catch (NameNotFoundException e) 
		{
		//	_logger.warn("Problems in cm='" + CM_NAME + "', adding addMonTableDictForVersion. Caught: " + e); 
			System.out.println("Problems in cm='" + CM_NAME + "', adding addMonTableDictForVersion. Caught: " + e); 
		}
	}

	@Override
	public List<String> getPkForVersion(DbxConnection conn, DbmsVersionInfo versionInfo)
	{
		List <String> pkCols = new LinkedList<String>();

		pkCols.add("VARIABLE_NAME");

		return pkCols;
	}

	@Override
	public String getSqlForVersion(DbxConnection conn, DbmsVersionInfo versionInfo)
	{
		String sql = 
			"select VARIABLE_NAME,cast(VARIABLE_VALUE as signed integer) as VARIABLE_VALUE \n" +
			"from performance_schema.global_status \n" +
			"where VARIABLE_VALUE REGEXP '^[[:digit:]]+$' \n" +
//			"  and length(VARIABLE_VALUE) <= 10  -- MaxInt=2,147,483,647 so discard numbers that are *big*... but it might still fail due to larger than 2b \n"
			"  and length(VARIABLE_VALUE) <= 19  -- MaxBigInt=9,223,372,036,854,775,807 so discard numbers that are *big*... Note: the cast to signed int, becomes a bigint \n"
			;

		return sql;
	}

	@Override
	public String getToolTipTextOnTableCell(MouseEvent e, String colName, Object cellValue, int modelRow, int modelCol) 
	{
		if ("VARIABLE_NAME".equals(colName))
		{
			return cellValue == null ? null : MySqlVariablesDictionary.getInstance().getDescriptionHtml(cellValue.toString());
		}

		if ("VARIABLE_VALUE".equals(colName) )
		{
			int pos_key = findColumn("VARIABLE_NAME");
			if (pos_key >= 0)
			{
				Object cellVal = getValueAt(modelRow, pos_key);
				if (cellVal instanceof String)
				{
					return MySqlVariablesDictionary.getInstance().getDescriptionHtml((String) cellVal);
				}
			}
		}
		
		return super.getToolTipTextOnTableCell(e, colName, cellValue, modelRow, modelCol);
	}
}


/*
** We might want to take a look at the following counters
**

From youtube (MySQL Performance Tuning Part 1, MySQL Configuration): speedemy.com

Com_begin
Com_commit
Com_delete
Com_insert
Com_select
Com_update

Created_tmp_disk_tables
Created_tmp_tables

Handler_read_first
Handler_read_key
Handler_read_next
Handler_read_prev
Handler_read_rnd_next


Innodb_buffer_pool_page_flushed 		- Number of pages flushed from buffer pool
Innodb_buffer_pool_reads		 		- Number of Disk IO calls to read into the buffer pool
Innodb_data_fsyncs						- Number of fsync() calls executed
Innodb_data_pending_{fsync|read|write}	- Guages showing a number of pending fsync, read or write calls

Innodb_data_{read|write}				- Number of random read/write disk IO
Innodb_history_list_length				- Guage - showing a number of transactions that haven't been cleaned up after
Innodb_ibuf_merges						- Number of insert buffer merge operations. High numbers here could explain intensive IO spikes
Innodb_log_waits						- number of times log buffers was to small
Innodb_lsn_current						- number of bytes written to transaction log (helps to tune inoodb redo log size)
Innodb_mutex_os_waits					- If this is high, you could be having internal mutex contention (a few 100's pr even 100 is high)
Innodb_rows_{read|inserted|deleted|updated}	- helps you understand internal activity - number of rows read, inserted, deleted or updated
Innodb_row_lock_time_{avg|max}			- Show how much time is spent on logical locks


Opened_tables							- Are file caches sized properly
Opened_table_definitions				- 


Qcache_hits								- Qcache_hits to Com_select % hit 
Qcache_lowmem_prunes					- 
Qcache_inserts							- 


Select_full_join						- Number of queries that made a table scan during a join (even if join buffer was used). Shows bad indexing.
Select_full_range_join					- Number of joins that used a range search on reference table. Very rare
Select_range							- very common range access pattern (FYI only)
Select_range_check						- joins without keys, with additional key usage check each time row is read. Rare
Select_scan								- Table scan on the first (or the only) table in the join. Shows bad indexing


Threads_cached							- Current number of cached threads 
Threads_connected						- Threads/users currently connected to the server
Threads_created							- number of threads created (increase thread_cache_size if this is high)
Threads_running							- Guage - most interesting counter of them all. Guage showing a number of threads that are currently executing inside MySQL. Doing anything from waiting to commiting data to disk. If this is spyky, you are most likely having pretty serious MySQL Performace Issues


INNOTOP - Top like innodb centered MySQL Status monitor - github.com/innotop/innotop
*/
