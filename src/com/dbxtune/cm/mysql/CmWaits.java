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
package com.dbxtune.cm.mysql;

import java.lang.invoke.MethodHandles;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

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
public class CmWaits
extends CountersModel
{
	private static final Logger _logger = LogManager.getLogger(MethodHandles.lookup().lookupClass());
	private static final long    serialVersionUID = 1L;

	public static final String   CM_NAME          = CmWaits.class.getSimpleName();
	public static final String   SHORT_NAME       = "Wait Events";
	public static final String   HTML_DESC        =
		"<html>" +
		"What are the connections waiting for? One row per Wait Event (instrument), for the whole server. " +
		"(performance_schema.events_waits_summary_global_by_event_name)<br>" +
		"<br>" +
		"<b>Tip</b>: Look at the DIFF or RATE values, sorted on <code>WaitTimeMs</code>. That is where the time is spent right now.<br>" +
		"<br>" +
		"<b>Note</b>: Only the instruments that are ON are counted (column <code>Enabled</code>). By default MySQL has ON: file IO (io/file), table IO (io/table), table locks and metadata locks.<br>" +
		"The mutex/rwlock/cond waits (synch/...) and socket IO are OFF by default, since they cost CPU. Turn them on only for a while, when you look for a specific problem:<br>" +
		"<code>UPDATE performance_schema.setup_instruments SET ENABLED = 'YES', TIMED = 'YES' WHERE NAME LIKE 'wait/synch/%'</code><br>" +
		"<b>Note</b>: InnoDB row lock waits are not Wait Events. See the Summary (row lock time) and the tab 'Active Statements' (who blocks who).<br>" +
		"<b>Note</b>: The table IO time (io/table) includes the file IO it causes (the waits are nested), so the times can't be summed.<br>" +
		"</html>";

	public static final String   GROUP_NAME       = MainFrame.TCP_GROUP_SERVER;
	public static final String   GUI_ICON_FILE    = "images/" + CM_NAME + ".png";

	public static final long     NEED_SRV_VERSION = 0;
	public static final long     NEED_CE_VERSION  = 0;

	public static final String[] MON_TABLES       = new String[] {CM_NAME, "events_waits_summary_global_by_event_name"};
	public static final String[] NEED_ROLES       = new String[] {};
	public static final String[] NEED_CONFIG      = new String[] {};

	public static final String[] PCT_COLUMNS      = new String[] {};
	public static final String[] DIFF_COLUMNS     = new String[] {
		"WaitCount",
		"WaitTimeMs"
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

		return new CmWaits(counterController, guiController);
	}

	public CmWaits(ICounterController counterController, IGuiController guiController)
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
	public static final String GRAPH_NAME_WAIT_CLASS_TIME = "WaitClassTime";
	public static final String GRAPH_NAME_WAIT_EVENT_TIME = "WaitEventTime";
	public static final String GRAPH_NAME_IO_EVENT_AVG_MS = "IoEventAvgMs";

	// The file IO Wait Events in graph 'IoEventAvgMs' (if a event is not found, it has never happened, and the value will be 0)
	private static final String[] IO_EVENTS = new String[] {
		"wait/io/file/innodb/innodb_data_file",
		"wait/io/file/innodb/innodb_log_file",
		"wait/io/file/innodb/innodb_dblwr_file",
		"wait/io/file/innodb/innodb_temp_file",
		"wait/io/file/sql/binlog",
		"wait/io/file/sql/relaylog"
		};
	private static final String[] IO_EVENT_LABELS = new String[] {
		"innodb_data_file",
		"innodb_log_file (redo)",
		"innodb_dblwr_file (doublewrite)",
		"innodb_temp_file",
		"binlog",
		"relaylog"
		};

	private void addTrendGraphs()
	{
		addTrendGraph(GRAPH_NAME_WAIT_CLASS_TIME,
			"Wait Time per Wait Class", 	                // Menu CheckBox text
			"Wait Time in ms per Second, per Wait Class (" + SHORT_NAME + ")", // Graph Label
			TrendGraphDataPoint.createGraphProps(TrendGraphDataPoint.Y_AXIS_SCALE_LABELS_MILLISEC, CentralPersistReader.SampleType.MAX_OVER_SAMPLES),
			null,
			LabelType.Dynamic,
			TrendGraphDataPoint.Category.WAITS,
			false, // is Percent Graph
			true,  // visible at start
			0,     // graph is valid from Server Version. 0 = All Versions; >0 = Valid from this version and above
			-1);   // minimum height

		addTrendGraph(GRAPH_NAME_WAIT_EVENT_TIME,
			"Wait Time per Wait Event, Top 15", 	                // Menu CheckBox text
			"Wait Time in ms per Second, for the Top 15 Wait Events (" + SHORT_NAME + ")", // Graph Label
			TrendGraphDataPoint.createGraphProps(TrendGraphDataPoint.Y_AXIS_SCALE_LABELS_MILLISEC, CentralPersistReader.SampleType.MAX_OVER_SAMPLES),
			null,
			LabelType.Dynamic,
			TrendGraphDataPoint.Category.WAITS,
			false, // is Percent Graph
			false, // visible at start
			0,     // graph is valid from Server Version. 0 = All Versions; >0 = Valid from this version and above
			-1);   // minimum height

		addTrendGraph(GRAPH_NAME_IO_EVENT_AVG_MS,
			"File IO Wait Events, Average ms per Wait", 	                // Menu CheckBox text
			"File IO Wait Events, Average ms per Wait (" + SHORT_NAME + ")", // Graph Label
			TrendGraphDataPoint.createGraphProps(TrendGraphDataPoint.Y_AXIS_SCALE_LABELS_MILLISEC, CentralPersistReader.SampleType.MAX_OVER_SAMPLES),
			IO_EVENT_LABELS,
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

		pkCols.add("EventName");

		return pkCols;
	}

	/** If we can't read the views: disable the CM, and tell what to do about it */
	@Override
	public boolean checkDependsOnOther(DbxConnection conn)
	{
		return MySqlCmUtils.checkViewsAreReadable(this, conn, "performance_schema.events_waits_summary_global_by_event_name", "performance_schema.setup_instruments");
	}

	@Override
	public String getSqlForVersion(DbxConnection conn, DbmsVersionInfo versionInfo)
	{
		// The timers are in picoseconds: / 1000000000 = milliseconds
		// The instruments that are ON, and the ones that are OFF but have counted something (they were ON for a while)
		// 'idle' is skipped: a connection that waits for the client to send the next statement is not a problem
		String sql = ""
			+ "select /* ${cmCollectorName} */ \n"
			+ "     w.EVENT_NAME                                                       as EventName \n"
			+ "    ,SUBSTRING_INDEX(SUBSTRING_INDEX(w.EVENT_NAME, '/', 3), '/', -2)    as WaitClass \n"
			+ "    ,i.ENABLED                                                          as Enabled \n"
			+ "    ,i.TIMED                                                            as Timed \n"
			+ "    ,w.COUNT_STAR                                                       as WaitCount \n"
			+ "    ,CAST(w.SUM_TIMER_WAIT / 1000000000.0 as DECIMAL(18,1))             as WaitTimeMs \n"
			+ "    ,CAST(w.AVG_TIMER_WAIT / 1000000000.0 as DECIMAL(18,3))             as AvgWaitMs \n"
			+ "    ,CAST(w.MAX_TIMER_WAIT / 1000000000.0 as DECIMAL(18,1))             as MaxWaitMs \n"
			+ "from performance_schema.events_waits_summary_global_by_event_name w \n"
			+ "left join performance_schema.setup_instruments i on i.NAME = w.EVENT_NAME \n"
			+ "where w.EVENT_NAME <> 'idle' \n"
			+ "  and (w.COUNT_STAR > 0 or i.ENABLED = 'YES') \n"
			+ "order by w.EVENT_NAME \n"
			+ "";

		return sql;
	}

	@Override
	public void addMonTableDictForVersion(DbxConnection conn, DbmsVersionInfo versionInfo)
	{
		try
		{
			MonTablesDictionary mtd = MonTablesDictionaryManager.getInstance();

			mtd.addTable(CM_NAME, "What are the connections waiting for? One row per Wait Event (instrument), for the whole server.");

			mtd.addColumn(CM_NAME, "EventName",  "<html>The instrument, for example <code>wait/io/file/innodb/innodb_log_file</code> (writes to the redo log).</html>");
			mtd.addColumn(CM_NAME, "WaitClass",  "<html>The second and third part of the name:<br>"
			                                         + "<code>io/file</code>: file IO. <code>io/table</code>: table IO (the rows read and written by the storage engine, it includes the file IO it causes).<br>"
			                                         + "<code>lock/table</code>: table locks. <code>lock/metadata</code>: metadata locks (DDL, LOCK TABLES...).<br>"
			                                         + "<code>synch/...</code>: mutex, rwlock, cond waits (OFF by default). <code>io/socket</code>: network (OFF by default).</html>");
			mtd.addColumn(CM_NAME, "Enabled",    "<html>Is the instrument ON (setup_instruments.ENABLED)? If NO, nothing is counted.</html>");
			mtd.addColumn(CM_NAME, "Timed",      "<html>Is the time measured (setup_instruments.TIMED)? If NO, the waits are counted but the time is 0.</html>");
			mtd.addColumn(CM_NAME, "WaitCount",  "<html>Number of waits.</html>");
			mtd.addColumn(CM_NAME, "WaitTimeMs", "<html>Time waited, in milliseconds.</html>");
			mtd.addColumn(CM_NAME, "AvgWaitMs",  "<html>Average time in milliseconds for ONE wait.<br>"
			                                         + "ABS: since the server was started. DIFF and RATE: for the waits in the last sample.<br>"
			                                         + "<b>Formula</b>: WaitTimeMs / WaitCount</html>");
			mtd.addColumn(CM_NAME, "MaxWaitMs",  "<html>The longest wait, in milliseconds (since the server was started).</html>");
		}
		catch (NameNotFoundException e)
		{
			_logger.warn("Problems in cm='" + CM_NAME + "', adding addMonTableDictForVersion. Caught: " + e);
		}
	}

	/**
	 * AvgWaitMs: The SQL gives the average since the server was started.<br>
	 * For DIFF (and RATE, which gets it from DIFF) we want the average for the waits that happened in this sample
	 */
	@Override
	public void localCalculation(CounterSample prevSample, CounterSample newSample, CounterSample diffData)
	{
		int pos_WaitCount  = diffData.findColumn("WaitCount");
		int pos_WaitTimeMs = diffData.findColumn("WaitTimeMs");
		int pos_AvgWaitMs  = diffData.findColumn("AvgWaitMs");

		if (pos_WaitCount == -1 || pos_WaitTimeMs == -1 || pos_AvgWaitMs == -1)
		{
			_logger.warn(getName() + ": localCalculation(), Problems finding one of the columns: WaitCount, WaitTimeMs, AvgWaitMs.");
			return;
		}

		for (int rowId=0; rowId < diffData.getRowCount(); rowId++)
		{
			long   waits  = diffData.getValueAsLong  (rowId, pos_WaitCount , 0L);
			double waitMs = diffData.getValueAsDouble(rowId, pos_WaitTimeMs, 0d);

			BigDecimal avgWaitMs = (waits <= 0) ? new BigDecimal(0) : MathUtils.roundToBigDecimal(waitMs / waits, 3);

			diffData.setValueAt(avgWaitMs, rowId, pos_AvgWaitMs);
		}
	}

	@Override
	public void updateGraphData(TrendGraphDataPoint tgdp)
	{
		//--------------------------------------------------------
		// Wait time (ms per second) per Wait Class (sorted by name, so the lines keep their order)
		//--------------------------------------------------------
		if (GRAPH_NAME_WAIT_CLASS_TIME.equals(tgdp.getName()))
		{
			Map<String, Double> dataMap = new TreeMap<>();

			for (int r = 0; r < this.size(); r++)
			{
				String waitClass = this.getAbsString        (r, "WaitClass");
				Double msPerSec  = this.getRateValueAsDouble(r, "WaitTimeMs");

				dataMap.merge(waitClass, msPerSec == null ? 0d : msPerSec, Double::sum);
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
				Double msPerSec = this.getRateValueAsDouble(r, "WaitTimeMs");
				if (msPerSec != null && msPerSec > 0)
					rowIds.add(r);
			}
			rowIds.sort((r1, r2) -> this.getRateValueAsDouble(r2, "WaitTimeMs").compareTo(this.getRateValueAsDouble(r1, "WaitTimeMs")));

			int top = Math.min(15, rowIds.size());
			if (top > 0)
			{
				Double[] dArray = new Double[top];
				String[] lArray = new String[top];
				for (int i = 0; i < top; i++)
				{
					int r = rowIds.get(i);

					lArray[i] = this.getAbsString(r, "EventName");
					dArray[i] = this.getRateValueAsDouble(r, "WaitTimeMs");
				}

				// Set the values
				tgdp.setDataPoint(this.getTimestamp(), lArray, dArray);
			}
			else if (tgdp.hasData())
			{
				// Nothing to show in this sample: set the lines we already have to 0 (if we do nothing, the values of the previous sample are kept and stored again)
				Double[] zeros = new Double[tgdp.getLabel().length];
				Arrays.fill(zeros, 0.0);
				tgdp.setDataPoint(this.getTimestamp(), tgdp.getLabel(), zeros);
			}
		}

		//--------------------------------------------------------
		// Average ms per wait, for the waits in this sample (see localCalculation), for the important file IO Wait Events
		//--------------------------------------------------------
		if (GRAPH_NAME_IO_EVENT_AVG_MS.equals(tgdp.getName()))
		{
			Double[] arr = new Double[IO_EVENTS.length];
			for (int i = 0; i < IO_EVENTS.length; i++)
			{
				int    rowId = this.getRowIdWhere(DATA_DIFF, "EventName", IO_EVENTS[i]);
				Double avgMs = (rowId == -1) ? null : this.getDiffValueAsDouble(rowId, "AvgWaitMs");

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
			.labelColumn("EventName")
			.valueColumns("WaitTimeMs", "WaitCount")
			.seriesLabels("Wait Time (ms)", "Wait Count")
			.pieOtherLimit(0.05)
			.splitRatio(0.6));

		list.add(new CmChartDescriptor()
			.id("wait-by-class")
			.title("Wait Distribution by Wait Class")
			.chartType(CmChartDescriptor.CHART_TYPE_DUAL_PIE_BAR)
			.labelColumn("EventName")
			.groupByColumn("WaitClass")
			.valueColumns("WaitTimeMs", "WaitCount")
			.seriesLabels("Wait Time (ms)", "Wait Count")
			.pieOtherLimit(0.05)
			.splitRatio(0.6));

		return list;
	}
}
