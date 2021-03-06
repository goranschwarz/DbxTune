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
package com.asetune.cm.ase;

import java.sql.Connection;
import java.util.LinkedList;
import java.util.List;

import com.asetune.ICounterController;
import com.asetune.IGuiController;
import com.asetune.cm.CounterSetTemplates;
import com.asetune.cm.CounterSetTemplates.Type;
import com.asetune.cm.CountersModel;
import com.asetune.graph.TrendGraphDataPoint;
import com.asetune.graph.TrendGraphDataPoint.LabelType;
import com.asetune.gui.MainFrame;
import com.asetune.utils.Ver;

/**
 * @author Goran Schwarz (goran_schwarz@hotmail.com)
 */
public class CmRaLogActivity
extends CountersModel
{
//	private static Logger        _logger          = Logger.getLogger(CmRaLogActivity.class);
	private static final long    serialVersionUID = 1L;

	public static final String   CM_NAME          = CmRaLogActivity.class.getSimpleName();
	public static final String   SHORT_NAME       = "Log Activity";
	public static final String   HTML_DESC        = 
		"<html>" +
		"Provides information on Rep Agent Log Activity." +
		"</html>";

	public static final String   GROUP_NAME       = MainFrame.TCP_GROUP_REP_AGENT;
	public static final String   GUI_ICON_FILE    = "images/"+CM_NAME+".png";

//	public static final long     NEED_SRV_VERSION = 15700;
//	public static final long     NEED_SRV_VERSION = 1570000;
	public static final long     NEED_SRV_VERSION = Ver.ver(15,7);
	public static final long     NEED_CE_VERSION  = 0;

	public static final String[] MON_TABLES       = new String[] {"monRepLogActivity"};
	public static final String[] NEED_ROLES       = new String[] {"mon_role"};
	public static final String[] NEED_CONFIG      = new String[] {"enable rep agent threads"};

	public static final String[] PCT_COLUMNS      = new String[] {};
	public static final String[] DIFF_COLUMNS     = new String[] {
		"LogRecordsScanned", "LogRecordsProcessed", "NumberOfScans", "TotalTimeForLogScans",
		"Updates", "Inserts", "Deletes",
		"StoredProcedures", "SQLStatements", "DDL",
		"Writetxt", "LobColumns",
		"CLRs", "Checkpoints",
		"BeginTransaction", "CommitTransaction", "AbortedTransaction", "PreparedTransaction",
		"DelayedCommit", "MaintenanceUserTransaction",
		"NumberOfLogExtentions", "NumberOfSchemasReused", "NumberOfSchemaFwdLookup",
		"NumberOfSchemaBckwLookup", "NumberOfMempoolAllocates", "NumberOfMempoolFrees"};

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

		return new CmRaLogActivity(counterController, guiController);
	}

	public CmRaLogActivity(ICounterController counterController, IGuiController guiController)
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
	
	public static final String GRAPH_NAME_SUM_LOG_SCAN     = "SumLogScan"; //GetCounters.CM_GRAPH_NAME__TEMPDB_ACTIVITY__LOGSEMAPHORE_CONT;
	public static final String GRAPH_NAME_SUM_LOG_TRAN     = "SumLogTran"; //GetCounters.CM_GRAPH_NAME__TEMPDB_ACTIVITY__LOGSEMAPHORE_CONT;
	public static final String GRAPH_NAME_SUM_LOG_OP_CRUD  = "SumLogOpCrud";
	public static final String GRAPH_NAME_SUM_LOG_OP_OTHER = "SumLogOpOther";
	public static final String GRAPH_NAME_DB_CRUD_OP       = "DbCrudOp";

	private void addTrendGraphs()
	{
//		String[] labels_lscan    = new String[] { "LogRecordsScanned", "LogRecordsProcessed" };
//		String[] labels_tran     = new String[] { "BeginTran", "CommitTran", "AbortedTran", "PreparedTran", "DelayedCommit", "MaintUserTran" };
//		String[] labels_op_crud  = new String[] { "Updates", "Inserts", "Deletes" };
//		String[] labels_op_other = new String[] { "StoredProcedures", "SQLStatements", "DDL", "Writetxt", "LobColumns", "CLRs" };
//		String[] labels_runtime  = TrendGraphDataPoint.RUNTIME_REPLACED_LABELS;
//
//		addTrendGraphData(GRAPH_NAME_SUM_LOG_SCAN,     new TrendGraphDataPoint(GRAPH_NAME_SUM_LOG_SCAN,     labels_lscan,    LabelType.Static));
//		addTrendGraphData(GRAPH_NAME_SUM_LOG_TRAN,     new TrendGraphDataPoint(GRAPH_NAME_SUM_LOG_TRAN,     labels_tran,     LabelType.Static));
//		addTrendGraphData(GRAPH_NAME_SUM_LOG_OP_CRUD,  new TrendGraphDataPoint(GRAPH_NAME_SUM_LOG_OP_CRUD,  labels_op_crud,  LabelType.Static));
//		addTrendGraphData(GRAPH_NAME_SUM_LOG_OP_OTHER, new TrendGraphDataPoint(GRAPH_NAME_SUM_LOG_OP_OTHER, labels_op_other, LabelType.Static));
//		addTrendGraphData(GRAPH_NAME_DB_CRUD_OP,       new TrendGraphDataPoint(GRAPH_NAME_DB_CRUD_OP,       labels_runtime,  LabelType.Dynamic));

		addTrendGraph(GRAPH_NAME_SUM_LOG_SCAN,
			"RA LogScan Records Sum", 	          // Menu CheckBox text
			"RA LogScan Records Sum, per second ("+GROUP_NAME+"->"+SHORT_NAME+")", // Label 
			TrendGraphDataPoint.Y_AXIS_SCALE_LABELS_PERSEC,
			new String[] { "LogRecordsScanned", "LogRecordsProcessed" }, 
			LabelType.Static,
			TrendGraphDataPoint.Category.REPLICATION,
			false, // is Percent Graph
			false, // visible at start
			Ver.ver(15,7),     // graph is valid from Server Version. 0 = All Versions; >0 = Valid from this version and above 
			-1);  // minimum height

		// GRAPH
		addTrendGraph(GRAPH_NAME_SUM_LOG_TRAN,
			"RA Log Transaction Records Sum", 	          // Menu CheckBox text
			"RA Log Transaction Records Sum, per second ("+GROUP_NAME+"->"+SHORT_NAME+")", // Label 
			TrendGraphDataPoint.Y_AXIS_SCALE_LABELS_PERSEC,
			new String[] { "BeginTran", "CommitTran", "AbortedTran", "PreparedTran", "DelayedCommit", "MaintUserTran" }, 
			LabelType.Static,
			TrendGraphDataPoint.Category.REPLICATION,
			false, // is Percent Graph
			false, // visible at start
			Ver.ver(15,7),     // graph is valid from Server Version. 0 = All Versions; >0 = Valid from this version and above 
			-1);  // minimum height

		// GRAPH
		addTrendGraph(GRAPH_NAME_SUM_LOG_OP_CRUD,
			"RA Log CRUD Operations Sum", 	          // Menu CheckBox text
			"RA Log CRUD Operations Sum, per second ("+GROUP_NAME+"->"+SHORT_NAME+")", // Label 
			TrendGraphDataPoint.Y_AXIS_SCALE_LABELS_PERSEC,
			new String[] { "Updates", "Inserts", "Deletes" }, 
			LabelType.Static,
			TrendGraphDataPoint.Category.REPLICATION,
			false, // is Percent Graph
			false, // visible at start
			Ver.ver(15,7),     // graph is valid from Server Version. 0 = All Versions; >0 = Valid from this version and above 
			-1);  // minimum height

		// GRAPH
		addTrendGraph(GRAPH_NAME_SUM_LOG_OP_OTHER,
			"RA Log Other Operations Sum", 	          // Menu CheckBox text
			"RA Log Other Operations Sum, per second ("+GROUP_NAME+"->"+SHORT_NAME+")", // Label 
			TrendGraphDataPoint.Y_AXIS_SCALE_LABELS_PERSEC,
			new String[] { "StoredProcedures", "SQLStatements", "DDL", "Writetxt", "LobColumns", "CLRs" }, 
			LabelType.Static,
			TrendGraphDataPoint.Category.REPLICATION,
			false, // is Percent Graph
			false, // visible at start
			Ver.ver(15,7),     // graph is valid from Server Version. 0 = All Versions; >0 = Valid from this version and above 
			-1);  // minimum height

		// GRAPH
		addTrendGraph(GRAPH_NAME_DB_CRUD_OP,
			"RA CRUD Operations per DB", 	          // Menu CheckBox text
			"RA CRUD Operations per DB, per second ("+GROUP_NAME+"->"+SHORT_NAME+")", // Label 
			TrendGraphDataPoint.Y_AXIS_SCALE_LABELS_PERSEC,
			null,
			LabelType.Dynamic,
			TrendGraphDataPoint.Category.REPLICATION,
			false, // is Percent Graph
			false, // visible at start
			Ver.ver(15,7),     // graph is valid from Server Version. 0 = All Versions; >0 = Valid from this version and above 
			-1);  // minimum height

//		// if GUI
//		if (getGuiController() != null && getGuiController().hasGUI())
//		{
//			TrendGraph tg;
//
//			// GRAPH
//			tg = new TrendGraph(GRAPH_NAME_SUM_LOG_SCAN,
//				"RA LogScan Records Sum", 	          // Menu CheckBox text
//				"RA LogScan Records Sum, per second ("+GROUP_NAME+"->"+SHORT_NAME+")", // Label 
//				labels_lscan, 
//				false, // is Percent Graph
//				this, 
//				false, // visible at start
//				Ver.ver(15,7),     // graph is valid from Server Version. 0 = All Versions; >0 = Valid from this version and above 
//				-1);  // minimum height
//			addTrendGraph(tg.getName(), tg, true);
//
//			// GRAPH
//			tg = new TrendGraph(GRAPH_NAME_SUM_LOG_TRAN,
//				"RA Log Transaction Records Sum", 	          // Menu CheckBox text
//				"RA Log Transaction Records Sum, per second ("+GROUP_NAME+"->"+SHORT_NAME+")", // Label 
//				labels_tran, 
//				false, // is Percent Graph
//				this, 
//				false, // visible at start
//				Ver.ver(15,7),     // graph is valid from Server Version. 0 = All Versions; >0 = Valid from this version and above 
//				-1);  // minimum height
//			addTrendGraph(tg.getName(), tg, true);
//
//			// GRAPH
//			tg = new TrendGraph(GRAPH_NAME_SUM_LOG_OP_CRUD,
//				"RA Log CRUD Operations Sum", 	          // Menu CheckBox text
//				"RA Log CRUD Operations Sum, per second ("+GROUP_NAME+"->"+SHORT_NAME+")", // Label 
//				labels_op_crud, 
//				false, // is Percent Graph
//				this, 
//				false, // visible at start
//				Ver.ver(15,7),     // graph is valid from Server Version. 0 = All Versions; >0 = Valid from this version and above 
//				-1);  // minimum height
//			addTrendGraph(tg.getName(), tg, true);
//
//			// GRAPH
//			tg = new TrendGraph(GRAPH_NAME_SUM_LOG_OP_OTHER,
//				"RA Log Other Operations Sum", 	          // Menu CheckBox text
//				"RA Log Other Operations Sum, per second ("+GROUP_NAME+"->"+SHORT_NAME+")", // Label 
//				labels_op_other, 
//				false, // is Percent Graph
//				this, 
//				false, // visible at start
//				Ver.ver(15,7),     // graph is valid from Server Version. 0 = All Versions; >0 = Valid from this version and above 
//				-1);  // minimum height
//			addTrendGraph(tg.getName(), tg, true);
//
//			// GRAPH
//			tg = new TrendGraph(GRAPH_NAME_DB_CRUD_OP,
//				"RA CRUD Operations per DB", 	          // Menu CheckBox text
//				"RA CRUD Operations per DB, per second ("+GROUP_NAME+"->"+SHORT_NAME+")", // Label 
//				labels_runtime, 
//				false, // is Percent Graph
//				this, 
//				false, // visible at start
//				Ver.ver(15,7),     // graph is valid from Server Version. 0 = All Versions; >0 = Valid from this version and above 
//				-1);  // minimum height
//			addTrendGraph(tg.getName(), tg, true);
//		}
	}

	@Override
	public void updateGraphData(TrendGraphDataPoint tgdp)
	{
		if (GRAPH_NAME_SUM_LOG_SCAN.equals(tgdp.getName()))
		{
			Double[] dArray = new Double[2];
			dArray[0] = this.getRateValueSum("LogRecordsScanned");
			dArray[1] = this.getRateValueSum("LogRecordsProcessed");

			// Set the values
			tgdp.setDataPoint(this.getTimestamp(), dArray);
		}

		if (GRAPH_NAME_SUM_LOG_TRAN.equals(tgdp.getName()))
		{
			Double[] dArray = new Double[6];
			dArray[0] = this.getRateValueSum("BeginTransaction");
			dArray[1] = this.getRateValueSum("CommitTransaction");
			dArray[2] = this.getRateValueSum("AbortedTransaction");
			dArray[3] = this.getRateValueSum("PreparedTransaction");
			dArray[4] = this.getRateValueSum("DelayedCommit");
			dArray[5] = this.getRateValueSum("MaintenanceUserTransaction");

			// Set the values
			tgdp.setDataPoint(this.getTimestamp(), dArray);
		}

		if (GRAPH_NAME_SUM_LOG_OP_CRUD.equals(tgdp.getName()))
		{
			Double[] dArray = new Double[3];
			dArray[0] = this.getRateValueSum("Inserts");
			dArray[1] = this.getRateValueSum("Updates");
			dArray[2] = this.getRateValueSum("Deletes");

			// Set the values
			tgdp.setDataPoint(this.getTimestamp(), dArray);
		}

		if (GRAPH_NAME_SUM_LOG_OP_OTHER.equals(tgdp.getName()))
		{
			Double[] dArray = new Double[6];
			dArray[0] = this.getRateValueSum("StoredProcedures");
			dArray[1] = this.getRateValueSum("SQLStatements");
			dArray[2] = this.getRateValueSum("DDL");
			dArray[3] = this.getRateValueSum("Writetxt");
			dArray[4] = this.getRateValueSum("LobColumns");
			dArray[5] = this.getRateValueSum("CLRs");

			// Set the values
			tgdp.setDataPoint(this.getTimestamp(), dArray);
		}

		if (GRAPH_NAME_DB_CRUD_OP.equals(tgdp.getName()))
		{
			// Write 1 "line" for every pool
			Double[] dArray = new Double[this.size()];
			String[] lArray = new String[dArray.length];
			for (int i = 0; i < dArray.length; i++)
			{
				Double ins = this.getRateValueAsDouble(i, "Inserts");
				Double upd = this.getRateValueAsDouble(i, "Updates");
				Double del = this.getRateValueAsDouble(i, "Deletes");

				lArray[i] = this.getRateString(i, "DBName");
				dArray[i] = new Double(ins + upd + del);
			}

			// Set the values
			tgdp.setDataPoint(this.getTimestamp(), lArray, dArray);
		}
	}

//	@Override
//	protected TabularCntrPanel createGui()
//	{
//		return new CmRaLogActivityPanel(this);
//	}

	@Override
	public String[] getDependsOnConfigForVersion(Connection conn, long srvVersion, boolean isClusterEnabled)
	{
		return NEED_CONFIG;
	}

	@Override
	public List<String> getPkForVersion(Connection conn, long srvVersion, boolean isClusterEnabled)
	{
		List <String> pkCols = new LinkedList<String>();

		pkCols.add("InstanceID");
		pkCols.add("DBID");
		pkCols.add("SPID");

		return pkCols;
	}

	@Override
	public String getSqlForVersion(Connection conn, long srvVersion, boolean isClusterEnabled)
	{
		String sql = "select * from master..monRepLogActivity";
		return sql;
	}
}
