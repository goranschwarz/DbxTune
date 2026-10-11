/*******************************************************************************
 * Copyright (C) 2010-2027 Goran Schwarz
 *
 * This file is part of DbxTune
 * DbxTune is a family of sub-products *Tune, hence the Dbx
 * Here are some of the tools: AseTune, IqTune, RsTune, RaxTune, HanaTune,
 *          SqlServerTune, PostgresTune, MySqlTune, MariaDbTune, Db2Tune, ...
 *
 * DbxTune is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * DbxTune is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with DbxTune.  If not, see <http://www.gnu.org/licenses/>.
 ******************************************************************************/
package com.dbxtune.cm.oracle;

import java.lang.invoke.MethodHandles;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;

import javax.naming.NameNotFoundException;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.dbxtune.ICounterController;
import com.dbxtune.IGuiController;
import com.dbxtune.central.pcs.CentralPersistReader;
import com.dbxtune.cm.CmChartDescriptor;
import com.dbxtune.cm.CounterSample;
import com.dbxtune.cm.CounterSetTemplates;
import com.dbxtune.cm.CounterSetTemplates.Type;
import com.dbxtune.cm.CountersModel;
import com.dbxtune.config.dict.MonTablesDictionary;
import com.dbxtune.config.dict.MonTablesDictionaryManager;
import com.dbxtune.graph.TrendGraphDataPoint;
import com.dbxtune.graph.TrendGraphDataPoint.LabelType;
import com.dbxtune.gui.MainFrame;
import com.dbxtune.sql.conn.DbxConnection;
import com.dbxtune.sql.conn.info.DbmsVersionInfo;
import com.dbxtune.utils.MathUtils;

/**
 * @author Goran Schwarz (goran_schwarz@hotmail.com)
 */
public class CmSystemEvent
extends CountersModel
{
	private static final Logger _logger = LogManager.getLogger(MethodHandles.lookup().lookupClass());
	private static final long    serialVersionUID = 1L;

	public static final String   CM_NAME          = CmSystemEvent.class.getSimpleName();
	public static final String   SHORT_NAME       = "System Events";
	public static final String   HTML_DESC        =
		"<html>" +
		"What are the sessions waiting for? One row per Wait Event, for the whole instance.<br>" +
		"<br>" +
		"A session is either using CPU or it is <i>waiting</i> for something (disk, lock, network...), and every wait is registered as a Wait Event.<br>" +
		"Wait Events in the Wait Class <b>Idle</b> are NOT included (a session that waits for the client to send the next statement is not a problem).<br>" +
		"<br>" +
		"<b>Tip</b>: Look at the DIFF or RATE values, sorted on <code>TIME_WAITED_MICRO</code>. That is where the time is spent right now.<br>" +
		"</html>";

	public static final String   GROUP_NAME       = MainFrame.TCP_GROUP_SERVER;
	public static final String   GUI_ICON_FILE    = "images/" + CM_NAME + ".png";

	public static final long     NEED_SRV_VERSION = 0;
	public static final long     NEED_CE_VERSION  = 0;

	public static final String[] MON_TABLES       = new String[] {CM_NAME, "V$SYSTEM_EVENT"};
	public static final String[] NEED_ROLES       = new String[] {};
	public static final String[] NEED_CONFIG      = new String[] {};

	public static final String[] PCT_COLUMNS      = new String[] {};
	public static final String[] DIFF_COLUMNS     = new String[] {
		"TOTAL_WAITS",
		"TOTAL_TIMEOUTS",
		"TIME_WAITED_MICRO",
		"TOTAL_WAITS_FG",
		"TOTAL_TIMEOUTS_FG",
		"TIME_WAITED_MICRO_FG"
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
			guiController.splashWindowProgress("Loading: Counter Model '" + CM_NAME + "'");

		return new CmSystemEvent(counterController, guiController);
	}

	public CmSystemEvent(ICounterController counterController, IGuiController guiController)
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

		setCounterController(counterController);
		setGuiController(guiController);

		addTrendGraphs();

		CounterSetTemplates.register(this);
	}


	//------------------------------------------------------------
	// Implementation
	//------------------------------------------------------------
	public static final String GRAPH_NAME_WAIT_CLASS_AAS  = "WaitClassAas";
	public static final String GRAPH_NAME_WAIT_EVENT_TIME = "WaitEventTime";
	public static final String GRAPH_NAME_IO_EVENT_AVG_MS = "IoEventAvgMs";

	// The Wait Events in graph 'IoEventAvgMs' (if a event is not found, it has never happened, and the value will be 0)
	private static final String[] IO_EVENTS = new String[] {
		"log file sync",
		"log file parallel write",
		"db file sequential read",
		"db file scattered read",
		"direct path read",
		"db file parallel write"
		};

	private void addTrendGraphs()
	{
		addTrendGraph(GRAPH_NAME_WAIT_CLASS_AAS,
			"User Sessions Waiting, per Wait Class", 	                // Menu CheckBox text
			"Average Number of User Sessions Waiting, per Wait Class (" + SHORT_NAME + ")", // Graph Label
			TrendGraphDataPoint.createGraphProps(TrendGraphDataPoint.Y_AXIS_SCALE_LABELS_NORMAL, CentralPersistReader.SampleType.MAX_OVER_SAMPLES),
			null,
			LabelType.Dynamic,
			TrendGraphDataPoint.Category.WAITS,
			false, // is Percent Graph
			true,  // visible at start
			0,     // graph is valid from Server Version. 0 = All Versions; >0 = Valid from this version and above
			-1);   // minimum height

		addTrendGraph(GRAPH_NAME_WAIT_EVENT_TIME,
			"Wait Time per Wait Event, Top 15", 	                // Menu CheckBox text
			"Wait Time in ms per Second, for the Top 15 Wait Events, all Processes (" + SHORT_NAME + ")", // Graph Label
			TrendGraphDataPoint.createGraphProps(TrendGraphDataPoint.Y_AXIS_SCALE_LABELS_MILLISEC, CentralPersistReader.SampleType.MAX_OVER_SAMPLES),
			null,
			LabelType.Dynamic,
			TrendGraphDataPoint.Category.WAITS,
			false, // is Percent Graph
			false, // visible at start
			0,     // graph is valid from Server Version. 0 = All Versions; >0 = Valid from this version and above
			-1);   // minimum height

		addTrendGraph(GRAPH_NAME_IO_EVENT_AVG_MS,
			"IO Wait Events, Average ms per Wait", 	                // Menu CheckBox text
			"Disk IO Wait Events, Average ms per Wait (" + SHORT_NAME + ")", // Graph Label
			TrendGraphDataPoint.createGraphProps(TrendGraphDataPoint.Y_AXIS_SCALE_LABELS_MILLISEC, CentralPersistReader.SampleType.MAX_OVER_SAMPLES),
			IO_EVENTS,
			LabelType.Static,
			TrendGraphDataPoint.Category.DISK,
			false, // is Percent Graph
			true,  // visible at start
			0,     // graph is valid from Server Version. 0 = All Versions; >0 = Valid from this version and above
			-1);   // minimum height
	}

	@Override
	public List<String> getPkForVersion(DbxConnection conn, DbmsVersionInfo versionInfo)
	{
		List <String> pkCols = new LinkedList<String>();

		pkCols.add("EVENT_ID");

		return pkCols;
	}

	@Override
	public String getSqlForVersion(DbxConnection conn, DbmsVersionInfo versionInfo)
	{
		// Note: CAST(... as NUMBER(18,3)) is needed to keep decimals (a NUMBER without precision is treated as a BIGINT)
		String sql = ""
			+ "select \n"
			+ "     e.EVENT_ID \n"
			+ "    ,e.WAIT_CLASS \n"
			+ "    ,e.EVENT \n"
			+ "    ,e.TOTAL_WAITS \n"
			+ "    ,e.TOTAL_TIMEOUTS \n"
			+ "    ,e.TIME_WAITED_MICRO \n"
			+ "    ,CAST(e.TIME_WAITED_MICRO    / 1000 / NULLIF(e.TOTAL_WAITS   , 0) as NUMBER(18,3)) as AVG_WAIT_MS \n"
			+ "    ,e.TOTAL_WAITS_FG \n"
			+ "    ,e.TOTAL_TIMEOUTS_FG \n"
			+ "    ,e.TIME_WAITED_MICRO_FG \n"
			+ "    ,CAST(e.TIME_WAITED_MICRO_FG / 1000 / NULLIF(e.TOTAL_WAITS_FG, 0) as NUMBER(18,3)) as AVG_WAIT_MS_FG \n"
			+ "from V$SYSTEM_EVENT e \n"
			+ "where e.WAIT_CLASS <> 'Idle' \n"
			+ "order by e.WAIT_CLASS, e.EVENT \n"
			+ "";

		return sql;
	}

	@Override
	public void addMonTableDictForVersion(DbxConnection conn, DbmsVersionInfo versionInfo)
	{
		try
		{
			MonTablesDictionary mtd = MonTablesDictionaryManager.getInstance();

			mtd.addTable(CM_NAME, "What are the sessions waiting for? One row per Wait Event, for the whole instance.");

			mtd.addColumn(CM_NAME, "AVG_WAIT_MS",    "<html>Average time in milliseconds for ONE wait.<br>"
			                                             + "ABS: since the instance was started. DIFF and RATE: for the waits in the last sample.<br>"
			                                             + "<b>Formula</b>: TIME_WAITED_MICRO / 1000 / TOTAL_WAITS</html>");
			mtd.addColumn(CM_NAME, "AVG_WAIT_MS_FG", "<html>Average time in milliseconds for ONE wait, for foreground sessions (the users).<br>"
			                                             + "ABS: since the instance was started. DIFF and RATE: for the waits in the last sample.<br>"
			                                             + "<b>Formula</b>: TIME_WAITED_MICRO_FG / 1000 / TOTAL_WAITS_FG</html>");
		}
		catch (NameNotFoundException e)
		{
			_logger.warn("Problems in cm='" + CM_NAME + "', adding addMonTableDictForVersion. Caught: " + e);
		}
	}

	/**
	 * AVG_WAIT_MS and AVG_WAIT_MS_FG: The SQL gives the average since the instance was started.<br>
	 * For DIFF (and RATE, which gets it from DIFF) we want the average for the waits that happened in this sample
	 */
	@Override
	public void localCalculation(CounterSample prevSample, CounterSample newSample, CounterSample diffData)
	{
		int pos_TOTAL_WAITS          = diffData.findColumn("TOTAL_WAITS");
		int pos_TIME_WAITED_MICRO    = diffData.findColumn("TIME_WAITED_MICRO");
		int pos_AVG_WAIT_MS          = diffData.findColumn("AVG_WAIT_MS");
		int pos_TOTAL_WAITS_FG       = diffData.findColumn("TOTAL_WAITS_FG");
		int pos_TIME_WAITED_MICRO_FG = diffData.findColumn("TIME_WAITED_MICRO_FG");
		int pos_AVG_WAIT_MS_FG       = diffData.findColumn("AVG_WAIT_MS_FG");

		if (pos_TOTAL_WAITS == -1 || pos_TIME_WAITED_MICRO == -1 || pos_AVG_WAIT_MS == -1 || pos_TOTAL_WAITS_FG == -1 || pos_TIME_WAITED_MICRO_FG == -1 || pos_AVG_WAIT_MS_FG == -1)
		{
			_logger.warn(getName() + ": localCalculation(), Problems finding one of the columns: TOTAL_WAITS, TIME_WAITED_MICRO, AVG_WAIT_MS, TOTAL_WAITS_FG, TIME_WAITED_MICRO_FG, AVG_WAIT_MS_FG.");
			return;
		}

		for (int rowId=0; rowId < diffData.getRowCount(); rowId++)
		{
			long waits   = diffData.getValueAsLong(rowId, pos_TOTAL_WAITS);
			long micro   = diffData.getValueAsLong(rowId, pos_TIME_WAITED_MICRO);
			long waitsFg = diffData.getValueAsLong(rowId, pos_TOTAL_WAITS_FG);
			long microFg = diffData.getValueAsLong(rowId, pos_TIME_WAITED_MICRO_FG);

			BigDecimal avgWaitMs   = (waits   <= 0) ? new BigDecimal(0) : MathUtils.roundToBigDecimal(micro   / 1000.0 / waits  , 3);
			BigDecimal avgWaitMsFg = (waitsFg <= 0) ? new BigDecimal(0) : MathUtils.roundToBigDecimal(microFg / 1000.0 / waitsFg, 3);

			diffData.setValueAt(avgWaitMs  , rowId, pos_AVG_WAIT_MS);
			diffData.setValueAt(avgWaitMsFg, rowId, pos_AVG_WAIT_MS_FG);
		}
	}

	@Override
	public void updateGraphData(TrendGraphDataPoint tgdp)
	{
		//--------------------------------------------------------
		// How many user sessions are (on average) waiting, per Wait Class
		// 1 second of wait time per second == 1 session that was waiting all the time
		// Foreground (_FG) only: what the users are waiting for. Then "CPU + this" is about the same as 'DB time' (see CmExecutionTime)
		//                        The background processes (LGWR, DBWR...) are not included, since the users wait for them (for example 'log file sync')
		//--------------------------------------------------------
		if (GRAPH_NAME_WAIT_CLASS_AAS.equals(tgdp.getName()))
		{
			LinkedHashMap<String, Double> dataMap = new LinkedHashMap<String, Double>();

			for (int r = 0; r < this.size(); r++)
			{
				String waitClass    = this.getAbsString        (r, "WAIT_CLASS");
				Double microsPerSec = this.getRateValueAsDouble(r, "TIME_WAITED_MICRO_FG");

				Double sum = dataMap.get(waitClass);
				if (sum == null)
					sum = Double.valueOf(0);

				if (microsPerSec != null)
					sum += microsPerSec / 1_000_000.0;

				dataMap.put(waitClass, sum);
			}

			if (dataMap.size() > 0)
				tgdp.setData(this.getTimestamp(), dataMap);
		}

		//--------------------------------------------------------
		// Wait time (ms per second) for the 15 Wait Events with most wait time in this sample
		//--------------------------------------------------------
		if (GRAPH_NAME_WAIT_EVENT_TIME.equals(tgdp.getName()))
		{
			// Get rows that has waited, then sort on wait time: highest first
			List<Integer> rowIds = new ArrayList<>();
			for (int r = 0; r < this.size(); r++)
			{
				Double microsPerSec = this.getRateValueAsDouble(r, "TIME_WAITED_MICRO");
				if (microsPerSec != null && microsPerSec > 0)
					rowIds.add(r);
			}
			rowIds.sort((r1, r2) -> this.getRateValueAsDouble(r2, "TIME_WAITED_MICRO").compareTo(this.getRateValueAsDouble(r1, "TIME_WAITED_MICRO")));

			int top = Math.min(15, rowIds.size());
			if (top > 0)
			{
				Double[] dArray = new Double[top];
				String[] lArray = new String[top];
				for (int i = 0; i < top; i++)
				{
					int r = rowIds.get(i);

					lArray[i] = this.getAbsString(r, "WAIT_CLASS") + ": " + this.getAbsString(r, "EVENT");
					dArray[i] = this.getRateValueAsDouble(r, "TIME_WAITED_MICRO") / 1000.0;
				}

				// Set the values
				tgdp.setDataPoint(this.getTimestamp(), lArray, dArray);
			}
		}

		//--------------------------------------------------------
		// Average ms per wait, for the waits in this sample (see localCalculation), for some important disk IO Wait Events
		//--------------------------------------------------------
		if (GRAPH_NAME_IO_EVENT_AVG_MS.equals(tgdp.getName()))
		{
			Double[] arr = new Double[IO_EVENTS.length];
			for (int i = 0; i < IO_EVENTS.length; i++)
			{
				int    rowId = this.getRowIdWhere(DATA_DIFF, "EVENT", IO_EVENTS[i]);
				Double avgMs = (rowId == -1) ? null : this.getDiffValueAsDouble(rowId, "AVG_WAIT_MS");

				arr[i] = (avgMs == null) ? 0.0 : avgMs;
			}

			// Set the values
			tgdp.setDataPoint(this.getTimestamp(), arr);
		}
	}

	@Override
	public List<CmChartDescriptor> createChartDescriptors()
	{
		List<CmChartDescriptor> list = new ArrayList<>();

		list.add(new CmChartDescriptor()
			.id("wait-by-event")
			.title("Wait Distribution by Event")
			.chartType(CmChartDescriptor.CHART_TYPE_DUAL_PIE_BAR)
			.labelColumn("EVENT")
			.valueColumns("TIME_WAITED_MICRO", "TOTAL_WAITS")
			.seriesLabels("Wait Time (microseconds)", "Wait Count")
			.pieOtherLimit(0.05)
			.splitRatio(0.6));

		list.add(new CmChartDescriptor()
			.id("wait-by-class")
			.title("Wait Distribution by Wait Class")
			.chartType(CmChartDescriptor.CHART_TYPE_DUAL_PIE_BAR)
			.labelColumn("EVENT")
			.groupByColumn("WAIT_CLASS")
			.valueColumns("TIME_WAITED_MICRO", "TOTAL_WAITS")
			.seriesLabels("Wait Time (microseconds)", "Wait Count")
			.pieOtherLimit(0.05)
			.splitRatio(0.6));

		return list;
	}
}
