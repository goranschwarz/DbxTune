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
import java.util.LinkedList;
import java.util.List;

import javax.naming.NameNotFoundException;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.dbxtune.ICounterController;
import com.dbxtune.IGuiController;
import com.dbxtune.central.pcs.CentralPersistReader;
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

/**
 * @author Goran Schwarz (goran_schwarz@hotmail.com)
 */
public class CmSysMetric
extends CountersModel
{
	private static final Logger _logger = LogManager.getLogger(MethodHandles.lookup().lookupClass());
	private static final long    serialVersionUID = 1L;

	public static final String   CM_NAME          = CmSysMetric.class.getSimpleName();
	public static final String   SHORT_NAME       = "System Metrics";
	public static final String   HTML_DESC        =
		"<html>" +
		"Oracle's own metrics for the last minute: about 160 ready-made values like response time, hit ratios, rates per second and per transaction.<br>" +
		"<br>" +
		"Oracle calculates them once a minute (from the same counters as in the tabs 'System Stat' and 'Time Model'), so the values here change once a minute.<br>" +
		"<code>BEGIN_TIME</code> and <code>END_TIME</code> shows which minute the values are for, and <code>METRIC_UNIT</code> what the value is.<br>" +
		"<br>" +
		"<b>Note</b>: This is NOT part of the Diagnostics Pack (it reads V$SYSMETRIC, not the AWR history).<br>" +
		"<b>Note</b>: In a Container Database, the values are for the whole CDB.<br>" +
		"</html>";

	public static final String   GROUP_NAME       = MainFrame.TCP_GROUP_SERVER;
	public static final String   GUI_ICON_FILE    = "images/" + CM_NAME + ".png";

	public static final long     NEED_SRV_VERSION = 0;
	public static final long     NEED_CE_VERSION  = 0;

	public static final String[] MON_TABLES       = new String[] {CM_NAME, "V$SYSMETRIC"};
	public static final String[] NEED_ROLES       = new String[] {};
	public static final String[] NEED_CONFIG      = new String[] {};

	public static final String[] PCT_COLUMNS      = new String[] {};
	public static final String[] DIFF_COLUMNS     = new String[] {};

	public static final boolean  NEGATIVE_DIFF_COUNTERS_TO_ZERO = true;
	public static final boolean  IS_SYSTEM_CM                   = true;
	public static final int      DEFAULT_POSTPONE_TIME          = 60;
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

		return new CmSysMetric(counterController, guiController);
	}

	public CmSysMetric(ICounterController counterController, IGuiController guiController)
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
	public static final String GRAPH_NAME_RESPONSE_MS = "MetricResponseMs";
	public static final String GRAPH_NAME_HIT_RATIOS  = "MetricHitRatios";
	public static final String GRAPH_NAME_TIME_RATIOS = "MetricTimeRatios";
	public static final String GRAPH_NAME_LIMITS      = "MetricLimits";

	// The metrics (METRIC_NAME) in each graph. The first two are in centiseconds, the graph shows them as milliseconds
	private static final String[] METRICS_RESPONSE_MS = new String[] {"SQL Service Response Time", "Average Synchronous Single-Block Read Latency"};
	// ('Cursor Cache Hit Ratio' is not here: Oracle's value for it is often far above 100)
	private static final String[] METRICS_HIT_RATIOS  = new String[] {"Buffer Cache Hit Ratio", "Library Cache Hit Ratio", "Soft Parse Ratio", "Memory Sorts Ratio", "Execute Without Parse Ratio"};
	private static final String[] METRICS_TIME_RATIOS = new String[] {"Database CPU Time Ratio", "Database Wait Time Ratio", "Host CPU Utilization (%)"};
	private static final String[] METRICS_LIMITS      = new String[] {"Process Limit %", "Session Limit %", "Shared Pool Free %", "PGA Cache Hit %"};

	private void addTrendGraphs()
	{
		addTrendGraph(GRAPH_NAME_RESPONSE_MS,
			"System Metrics, Response Time in ms", 	                // Menu CheckBox text
			"SQL Response Time per Call, and Single Block Read Time, in Milliseconds (" + SHORT_NAME + ")", // Graph Label
			TrendGraphDataPoint.createGraphProps(TrendGraphDataPoint.Y_AXIS_SCALE_LABELS_MILLISEC, CentralPersistReader.SampleType.MAX_OVER_SAMPLES),
			new String[] {"SQL Service Response Time (ms per call)", "Single-Block Read Latency (ms)"},
			LabelType.Static,
			TrendGraphDataPoint.Category.OPERATIONS,
			false, // is Percent Graph
			false, // visible at start
			0,     // graph is valid from Server Version. 0 = All Versions; >0 = Valid from this version and above
			-1);   // minimum height

		addTrendGraph(GRAPH_NAME_HIT_RATIOS,
			"System Metrics, Hit Ratios", 	                // Menu CheckBox text
			"Hit Ratios in Percent, for the last minute (" + SHORT_NAME + ")", // Graph Label
			TrendGraphDataPoint.createGraphProps(TrendGraphDataPoint.Y_AXIS_SCALE_LABELS_PERCENT, CentralPersistReader.SampleType.MIN_OVER_SAMPLES),
			METRICS_HIT_RATIOS,
			LabelType.Static,
			TrendGraphDataPoint.Category.CACHE,
			true,  // is Percent Graph
			false, // visible at start
			0,     // graph is valid from Server Version. 0 = All Versions; >0 = Valid from this version and above
			-1);   // minimum height

		addTrendGraph(GRAPH_NAME_TIME_RATIOS,
			"System Metrics, CPU and Wait Time Ratio", 	                // Menu CheckBox text
			"How much of the Database Time that is CPU and Wait, and Host CPU, in Percent (" + SHORT_NAME + ")", // Graph Label
			TrendGraphDataPoint.createGraphProps(TrendGraphDataPoint.Y_AXIS_SCALE_LABELS_PERCENT, CentralPersistReader.SampleType.MAX_OVER_SAMPLES),
			METRICS_TIME_RATIOS,
			LabelType.Static,
			TrendGraphDataPoint.Category.CPU,
			true,  // is Percent Graph
			false, // visible at start
			0,     // graph is valid from Server Version. 0 = All Versions; >0 = Valid from this version and above
			-1);   // minimum height

		addTrendGraph(GRAPH_NAME_LIMITS,
			"System Metrics, Limits and Free in Percent", 	                // Menu CheckBox text
			"Used Percent of the Process and Session Limits, Shared Pool Free, PGA Cache Hit (" + SHORT_NAME + ")", // Graph Label
			TrendGraphDataPoint.createGraphProps(TrendGraphDataPoint.Y_AXIS_SCALE_LABELS_PERCENT, CentralPersistReader.SampleType.MAX_OVER_SAMPLES),
			METRICS_LIMITS,
			LabelType.Static,
			TrendGraphDataPoint.Category.SRV_CONFIG,
			true,  // is Percent Graph
			false, // visible at start
			0,     // graph is valid from Server Version. 0 = All Versions; >0 = Valid from this version and above
			-1);   // minimum height
	}

	@Override
	public List<String> getPkForVersion(DbxConnection conn, DbmsVersionInfo versionInfo)
	{
		List <String> pkCols = new LinkedList<String>();

		pkCols.add("METRIC_NAME");

		return pkCols;
	}

	/** If we can't read the views: disable the CM, and tell what to do about it */
	@Override
	public boolean checkDependsOnOther(DbxConnection conn)
	{
		return OracleCmUtils.checkViewsAreReadable(this, conn, "V$SYSMETRIC");
	}

	@Override
	public String getSqlForVersion(DbxConnection conn, DbmsVersionInfo versionInfo)
	{
		// GROUP_ID = 2 is the "long duration" metrics: calculated once a minute, for the last 60 seconds. (GROUP_ID 3 is 15 seconds, but only a few of the metrics)
		// This looks the same in all versions
		// Note: CAST(... as NUMBER(28,4)) is needed to keep decimals (a NUMBER without precision is treated as a BIGINT)
		String sql = ""
			+ "select \n"
			+ "     m.METRIC_NAME \n"
			+ "    ,CAST(m.VALUE as NUMBER(28,4))               as VALUE \n"
			+ "    ,m.METRIC_UNIT \n"
			+ "    ,m.METRIC_ID \n"
			+ "    ,m.BEGIN_TIME \n"
			+ "    ,m.END_TIME \n"
			+ "    ,CAST(m.INTSIZE_CSEC / 100 as NUMBER(10,1))  as INTERVAL_SEC \n"
			+ "from V$SYSMETRIC m \n"
			+ "where m.GROUP_ID = 2 \n"
			+ "order by m.METRIC_NAME \n"
			+ "";

		return sql;
	}

	@Override
	public void addMonTableDictForVersion(DbxConnection conn, DbmsVersionInfo versionInfo)
	{
		try
		{
			MonTablesDictionary mtd = MonTablesDictionaryManager.getInstance();

			mtd.addTable(CM_NAME, "Oracle's own metrics for the last minute: response time, hit ratios, rates per second and per transaction.");

			mtd.addColumn(CM_NAME, "METRIC_NAME",  "<html>Name of the metric.</html>");
			mtd.addColumn(CM_NAME, "VALUE",        "<html>Value of the metric for the minute between BEGIN_TIME and END_TIME, in the unit of column METRIC_UNIT.<br><b>Note</b>: Times are often in centiseconds (1/100 of a second).</html>");
			mtd.addColumn(CM_NAME, "METRIC_UNIT",  "<html>What the value is, for example: '% (LogRead - PhyRead)/LogRead', 'CentiSeconds Per Call', 'Blocks Per Second'.</html>");
			mtd.addColumn(CM_NAME, "METRIC_ID",    "<html>Oracle's id for the metric.</html>");
			mtd.addColumn(CM_NAME, "BEGIN_TIME",   "<html>Start of the minute the value is for.</html>");
			mtd.addColumn(CM_NAME, "END_TIME",     "<html>End of the minute the value is for.</html>");
			mtd.addColumn(CM_NAME, "INTERVAL_SEC", "<html>Length of the interval in seconds (60).</html>");
		}
		catch (NameNotFoundException e)
		{
			_logger.warn("Problems in cm='" + CM_NAME + "', adding addMonTableDictForVersion. Caught: " + e);
		}
	}

	@Override
	public void updateGraphData(TrendGraphDataPoint tgdp)
	{
		if (this.size() == 0)
			return;

		String[] metricNames = null;
		if (GRAPH_NAME_RESPONSE_MS.equals(tgdp.getName())) metricNames = METRICS_RESPONSE_MS;
		if (GRAPH_NAME_HIT_RATIOS .equals(tgdp.getName())) metricNames = METRICS_HIT_RATIOS;
		if (GRAPH_NAME_TIME_RATIOS.equals(tgdp.getName())) metricNames = METRICS_TIME_RATIOS;
		if (GRAPH_NAME_LIMITS     .equals(tgdp.getName())) metricNames = METRICS_LIMITS;

		if (metricNames != null)
		{
			Double[] arr = new Double[metricNames.length];
			for (int i = 0; i < arr.length; i++)
			{
				// A metric that does not exist in this version is shown as 0
				Double val = this.getAbsValueAsDouble(createPkStr(metricNames[i]), "VALUE");
				arr[i] = (val == null) ? 0d : val;
			}

			// 'SQL Service Response Time' is in centiseconds: make it milliseconds (the read latency is already in milliseconds)
			if (GRAPH_NAME_RESPONSE_MS.equals(tgdp.getName()))
			{
				arr[0] = arr[0] * 10;
			}
			else
			{
				// The other graphs are in percent: Oracle's value can be a bit above 100 (seen: 'Database CPU Time Ratio' = 102.81)
				for (int i = 0; i < arr.length; i++)
					arr[i] = Math.min(100d, arr[i]);
			}

			// Set the values
			tgdp.setDataPoint(this.getTimestamp(), arr);
		}
	}
}
