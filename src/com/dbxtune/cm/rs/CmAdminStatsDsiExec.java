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
package com.dbxtune.cm.rs;

import java.lang.invoke.MethodHandles;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Map.Entry;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.dbxtune.ICounterController;
import com.dbxtune.IGuiController;
import com.dbxtune.central.pcs.CentralPersistReader;
import com.dbxtune.cm.CounterSetTemplates;
import com.dbxtune.cm.CounterSetTemplates.Type;
import com.dbxtune.cm.CountersModel;
import com.dbxtune.graph.TrendGraphDataPoint;
import com.dbxtune.graph.TrendGraphDataPoint.LabelType;
import com.dbxtune.gui.MainFrameRs;
import com.dbxtune.sql.conn.DbxConnection;
import com.dbxtune.sql.conn.info.DbmsVersionInfo;
import com.dbxtune.utils.StringUtil;

/**
 * @author Goran Schwarz (goran_schwarz@hotmail.com)
 */
public class CmAdminStatsDsiExec
extends CmAdminStatsAbstract
{
	private static final Logger _logger = LogManager.getLogger(MethodHandles.lookup().lookupClass());
	private static final long    serialVersionUID = 1L;

	public static final String   CM_NAME          = CmAdminStatsDsiExec.class.getSimpleName();
	public static final String   MODULE_NAME      = "DSIEXEC";
	public static final String   SHORT_NAME       = "DSI Executor";
	public static final String   HTML_DESC        = 
		"<html>" +
		"<p>RepServer Monitor And Performance Counters</p>" +
		"Fetched using: <code>admin statistics,'ALL'</code>" +
		"<br>" +
		"<b>Note</b>: In Experimental Status (it may work)" +
		"</html>";

	public static final String   GROUP_NAME       = MainFrameRs.TCP_GROUP_MC;
	public static final String   GUI_ICON_FILE    = "images/"+CM_NAME+".png";

	public static final long     NEED_SRV_VERSION = 0;
	public static final long     NEED_CE_VERSION  = 0;

	public static final String[] MON_TABLES       = new String[] {"stats"};
	public static final String[] NEED_ROLES       = new String[] {};
	public static final String[] NEED_CONFIG      = new String[] {};

	public static final String[] PCT_COLUMNS      = new String[] {};
	public static final String[] DIFF_COLUMNS     = new String[] {"dummyColumnToEnableDiffCalc"};

	public static final boolean  NEGATIVE_DIFF_COUNTERS_TO_ZERO = false;
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

		return new CmAdminStatsDsiExec(counterController, guiController);
	}

	public CmAdminStatsDsiExec(ICounterController counterController, IGuiController guiController)
	{
		super(counterController, guiController,
				CM_NAME, GROUP_NAME, /*sql*/null, /*pkList*/null, 
				DIFF_COLUMNS, PCT_COLUMNS, MON_TABLES, 
				NEED_ROLES, NEED_CONFIG, NEED_SRV_VERSION, NEED_CE_VERSION, 
				NEGATIVE_DIFF_COUNTERS_TO_ZERO, IS_SYSTEM_CM, DEFAULT_POSTPONE_TIME);

		setModuleName(MODULE_NAME);
		setDisplayName(SHORT_NAME);
		setDescription(HTML_DESC);

		setIconFile(GUI_ICON_FILE);

		setCounterController(counterController);
		setGuiController(guiController);
		
		addDependsOnCm(CmAdminStats.CM_NAME); // CmAdminStats must have been executed before this cm

		addTrendGraphs();
		
		CounterSetTemplates.register(this);
	}


	//------------------------------------------------------------
	// Implementation
	//------------------------------------------------------------
	@Override
	public String[] getDependsOnConfigForVersion(DbxConnection conn, DbmsVersionInfo versionInfo)
	{
		return NEED_CONFIG;
	}

// NOTE: NOT ALL of the below will be used in Upcoming Release... Some are just to SEE that will be the best "candidate"
	public static final String GRAPH_NAME_BATCH_COUNT    = "DSIEOCmdCount";       // Number of output commands in command batches submitted by a DSI. -- But I think it's number of SQL Batches Sent
	public static final String GRAPH_NAME_CMDS_SUCCEED   = "DSIECmdsSucceed";     // Commands successfully applied to the target database by a DSI/E
	public static final String GRAPH_NAME_BYTES_SUCCEED  = "DSIEBytesSucceed";    // Bytes successfully applied to the target database by a DSI/E.
	public static final String GRAPH_NAME_SEND_TIME      = "SendTime";            // Time, in milli-seconds, spent in sending command buffers to the RDS
	public static final String GRAPH_NAME_EXEC_CMD_TIME  = "DSIEExecCmdTime";     // The amount of time taken by a DSI/E to execute commands. This process includes creating command batches, flushing them, handling errors, etc.
	public static final String GRAPH_NAME_RESULT_TIME    = "DSIEResultTime";      // Time, in milli-seconds, to process the results of command batches submitted by a DSI.
	public static final String GRAPH_NAME_TRAN_TIME      = "DSIETranTime";        // Time, in milli-seconds, to process transactions by a DSI/E thread. This includes function string mapping, sending and processing results. A transaction may span command batches
	

	private void addTrendGraphs()
	{
		addTrendGraph(GRAPH_NAME_BATCH_COUNT,
				"DSI-Exec: Number of SQL Batches Sent to RDS per Second [DSIEOCmdCount]", // Menu CheckBox text
				"DSI-Exec: Number of SQL Batches Sent to RDS per Second [DSIEOCmdCount]", // Label 
				TrendGraphDataPoint.createGraphProps(TrendGraphDataPoint.Y_AXIS_SCALE_LABELS_COUNT, CentralPersistReader.SampleType.MAX_OVER_SAMPLES),
				null, 
				LabelType.Dynamic,
				TrendGraphDataPoint.Category.SPACE,
				false, // is Percent Graph
				false, // visible at start
				0,     // graph is valid from Server Version. 0 = All Versions; >0 = Valid from this version and above 
				-1);   // minimum height

		addTrendGraph(GRAPH_NAME_CMDS_SUCCEED,
				"DSI-Exec: Number of Commands Applied to RDS per Second [DSIECmdsSucceed]", // Menu CheckBox text
				"DSI-Exec: Number of Commands Applied to RDS per Second [DSIECmdsSucceed]", // Label 
				TrendGraphDataPoint.createGraphProps(TrendGraphDataPoint.Y_AXIS_SCALE_LABELS_COUNT, CentralPersistReader.SampleType.MAX_OVER_SAMPLES),
				null, 
				LabelType.Dynamic,
				TrendGraphDataPoint.Category.SPACE,
				false, // is Percent Graph
				false, // visible at start
				0,     // graph is valid from Server Version. 0 = All Versions; >0 = Valid from this version and above 
				-1);   // minimum height

		addTrendGraph(GRAPH_NAME_BYTES_SUCCEED,
				"DSI-Exec: Number of Bytes Applied to RDS per Second [DSIEBytesSucceed]", // Menu CheckBox text
				"DSI-Exec: Number of Bytes Applied to RDS per Second [DSIEBytesSucceed]", // Label 
				TrendGraphDataPoint.createGraphProps(TrendGraphDataPoint.Y_AXIS_SCALE_LABELS_BYTES, CentralPersistReader.SampleType.MAX_OVER_SAMPLES),
				null, 
				LabelType.Dynamic,
				TrendGraphDataPoint.Category.SPACE,
				false, // is Percent Graph
				false, // visible at start
				0,     // graph is valid from Server Version. 0 = All Versions; >0 = Valid from this version and above 
				-1);   // minimum height

		addTrendGraph(GRAPH_NAME_SEND_TIME,
				"DSI-Exec: Time Spent ms per Sec Sending Command Buffers to RDS [SendTime]", // Menu CheckBox text
				"DSI-Exec: Time Spent ms per Sec Sending Command Buffers to RDS [SendTime]", // Label 
				TrendGraphDataPoint.createGraphProps(TrendGraphDataPoint.Y_AXIS_SCALE_LABELS_MILLISEC, CentralPersistReader.SampleType.MAX_OVER_SAMPLES),
				null, 
				LabelType.Dynamic,
				TrendGraphDataPoint.Category.SPACE,
				false, // is Percent Graph
				false, // visible at start
				0,     // graph is valid from Server Version. 0 = All Versions; >0 = Valid from this version and above 
				-1);   // minimum height

		addTrendGraph(GRAPH_NAME_EXEC_CMD_TIME,
				"DSI-Exec: Time Spent ms per Sec Executing Command Batches to RDS [DSIEExecCmdTime]", // Menu CheckBox text
				"DSI-Exec: Time Spent ms per Sec Executing Command Batches to RDS [DSIEExecCmdTime]", // Label 
				TrendGraphDataPoint.createGraphProps(TrendGraphDataPoint.Y_AXIS_SCALE_LABELS_MILLISEC, CentralPersistReader.SampleType.MAX_OVER_SAMPLES),
				null, 
				LabelType.Dynamic,
				TrendGraphDataPoint.Category.SPACE,
				false, // is Percent Graph
				false, // visible at start
				0,     // graph is valid from Server Version. 0 = All Versions; >0 = Valid from this version and above 
				-1);   // minimum height

		addTrendGraph(GRAPH_NAME_RESULT_TIME,
				"DSI-Exec: Time Spent ms per Sec Process the Results from RDS [DSIEResultTime]", // Menu CheckBox text
				"DSI-Exec: Time Spent ms per Sec Process the Results from RDS [DSIEResultTime]", // Label 
				TrendGraphDataPoint.createGraphProps(TrendGraphDataPoint.Y_AXIS_SCALE_LABELS_MILLISEC, CentralPersistReader.SampleType.MAX_OVER_SAMPLES),
				null, 
				LabelType.Dynamic,
				TrendGraphDataPoint.Category.SPACE,
				false, // is Percent Graph
				false, // visible at start
				0,     // graph is valid from Server Version. 0 = All Versions; >0 = Valid from this version and above 
				-1);   // minimum height

		addTrendGraph(GRAPH_NAME_TRAN_TIME,
				"DSI-Exec: Time Spent ms per Sec Process the Transactions to RDS [DSIETranTime]", // Menu CheckBox text
				"DSI-Exec: Time Spent ms per Sec Process the Transactions to RDS [DSIETranTime]", // Label 
				TrendGraphDataPoint.createGraphProps(TrendGraphDataPoint.Y_AXIS_SCALE_LABELS_MILLISEC, CentralPersistReader.SampleType.MAX_OVER_SAMPLES),
				null, 
				LabelType.Dynamic,
				TrendGraphDataPoint.Category.SPACE,
				false, // is Percent Graph
				false, // visible at start
				0,     // graph is valid from Server Version. 0 = All Versions; >0 = Valid from this version and above 
				-1);   // minimum height

	}

	@Override
	public void updateGraphData(TrendGraphDataPoint tgdp)
	{
		if (GRAPH_NAME_BATCH_COUNT   .equals(tgdp.getName())) addGraphDataForColumn("DSIEOCmdCount"   , tgdp);
		if (GRAPH_NAME_CMDS_SUCCEED  .equals(tgdp.getName())) addGraphDataForColumn("DSIECmdsSucceed" , tgdp);
		if (GRAPH_NAME_BYTES_SUCCEED .equals(tgdp.getName())) addGraphDataForColumn("DSIEBytesSucceed", tgdp);

		if (GRAPH_NAME_SEND_TIME     .equals(tgdp.getName())) addGraphDataForColumn("SendTime"        , tgdp);
		if (GRAPH_NAME_EXEC_CMD_TIME .equals(tgdp.getName())) addGraphDataForColumn("DSIEExecCmdTime" , tgdp);
		if (GRAPH_NAME_RESULT_TIME   .equals(tgdp.getName())) addGraphDataForColumn("DSIEResultTime"  , tgdp);
		if (GRAPH_NAME_TRAN_TIME     .equals(tgdp.getName())) addGraphDataForColumn("DSIETranTime"    , tgdp);
	}

	private void addGraphDataForColumn(String colName, TrendGraphDataPoint tgdp)
	{
		if ( ! hasColumn(colName) )
		{
			_logger.error("No column '" + colName+ "' was found in CM='" + this.getName() + "'. Skipping updateGraphData for graph name '" + tgdp.getName() + "'.");
			return;
		}

		// ServerName to Value
		Map<String, Double> s2v = new LinkedHashMap<>();

		// Save to 's2v' if we HAVE Counter Value
		for (int i = 0; i < this.size(); i++)
		{
			String instance = this.getAbsString        (i, "Instance");
			Double colValue = this.getRateValueAsDouble(i, colName);
			
			if (colValue != null)
			{
				String srvName = StringUtil.lastWord(instance);
				s2v.put(srvName, colValue); // NOTE: This will NOT work if we have MULTIPLE Executors (Parallel DSI)
			}
		}

		if ( ! s2v.isEmpty() )
		{
			Double[] dArray = new Double[s2v.size()];
			String[] lArray = new String[dArray.length];

			int ae = 0;
			for (Entry<String, Double> entry : s2v.entrySet())
			{
				lArray[ae] = entry.getKey();
				dArray[ae] = entry.getValue();

				ae++;
			}

			tgdp.setDataPoint(this.getTimestamp(), lArray, dArray);
		}
	}

}
