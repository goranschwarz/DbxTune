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
package com.dbxtune.cm.db2;

import java.lang.invoke.MethodHandles;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;

import javax.naming.NameNotFoundException;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.dbxtune.ICounterController;
import com.dbxtune.IGuiController;
import com.dbxtune.central.pcs.CentralPersistReader;
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

/**
 * Where the time of the database goes: the time spent metrics of MON_GET_DATABASE_DETAILS as rows (MON_FORMAT_XML_TIMES_BY_ROW).
 * The request time is split into wait time (per kind of wait) and processing time (per kind of work).
 *
 * @author Goran Schwarz (goran_schwarz@hotmail.com)
 */
public class CmTimeSpent
extends CountersModel
{
	private static final Logger _logger = LogManager.getLogger(MethodHandles.lookup().lookupClass());
	private static final long    serialVersionUID = 1L;

	public static final String   CM_NAME          = CmTimeSpent.class.getSimpleName();
	public static final String   SHORT_NAME       = "Time Spent";
	public static final String   HTML_DESC        =
		"<html>" +
		"<p>Where the time of the database goes (the time spent metrics, one row per metric).</p>" +
		"<p><b>TOTAL_RQST_TIME</b> is all the time the database worked on requests from the clients. It is <b>TOTAL_WAIT_TIME</b> (split into the kinds of waits: " +
		"locks, reading pages, writing the log, the network...) plus the processing time (TOTAL_SECTION_PROC_TIME = executing statements, TOTAL_COMPILE_PROC_TIME, TOTAL_COMMIT_PROC_TIME...).</p>" +
		"<p>The wait with most time is usually what to look at first. <b>PCT_OF_RQST_TIME</b> is how much of the request time it is.</p>" +
		"<p>CLIENT_IDLE_WAIT_TIME is the time the connections waited for the clients: it is not a part of the request time.</p>" +
		"</html>";

	public static final String   GROUP_NAME       = MainFrame.TCP_GROUP_SERVER;
	public static final String   GUI_ICON_FILE    = "images/" + CM_NAME + ".png";

	public static final long     NEED_SRV_VERSION = 0;
	public static final long     NEED_CE_VERSION  = 0;

	public static final String[] MON_TABLES       = new String[] {CM_NAME, "MON_GET_DATABASE_DETAILS", "MON_FORMAT_XML_TIMES_BY_ROW"};
	public static final String[] NEED_ROLES       = new String[] {};
	public static final String[] NEED_CONFIG      = new String[] {};

	public static final String[] PCT_COLUMNS      = new String[] {};
	public static final String[] DIFF_COLUMNS     = new String[] {
		"TOTAL_TIME_MS",
		"COUNT"
	};

	// The counters starts from 0 when the database is activated again
	public static final boolean  NEGATIVE_DIFF_COUNTERS_TO_ZERO = true;
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
			guiController.splashWindowProgress("Loading: Counter Model '" + CM_NAME + "'");

		return new CmTimeSpent(counterController, guiController);
	}

	public CmTimeSpent(ICounterController counterController, IGuiController guiController)
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
	public static final String GRAPH_NAME_RQST_TIME  = "RqstTime";
	public static final String GRAPH_NAME_WAIT_TIME  = "WaitTime";

	private void addTrendGraphs()
	{
		addTrendGraph(GRAPH_NAME_RQST_TIME,
			"Request Time: Wait and Processing, ms per Second",            // Menu CheckBox text
			"Request Time: Wait and Processing, in ms per Second (" + SHORT_NAME + ")", // Graph Label
			TrendGraphDataPoint.createGraphProps(TrendGraphDataPoint.Y_AXIS_SCALE_LABELS_MILLISEC, CentralPersistReader.SampleType.AUTO),
			new String[] { "Wait", "Section Processing", "Compile", "Commit and Rollback", "Other Processing" },
			LabelType.Static,
			TrendGraphDataPoint.Category.WAITS,
			false, // is Percent Graph
			true,  // visible at start
			0,     // graph is valid from Server Version. 0 = All Versions; >0 = Valid from this version and above
			-1);   // minimum height

		addTrendGraph(GRAPH_NAME_WAIT_TIME,
			"Wait Time per Kind of Wait, Top 15",                          // Menu CheckBox text
			"Wait Time in ms per Second, for the Top 15 Kinds of Wait (" + SHORT_NAME + ")", // Graph Label
			TrendGraphDataPoint.createGraphProps(TrendGraphDataPoint.Y_AXIS_SCALE_LABELS_MILLISEC, CentralPersistReader.SampleType.MAX_OVER_SAMPLES),
			null,
			LabelType.Dynamic,
			TrendGraphDataPoint.Category.WAITS,
			false, // is Percent Graph
			true,  // visible at start
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

	@Override
	public String getSqlForVersion(DbxConnection conn, DbmsVersionInfo versionInfo)
	{
		// NOTE: The times are in milliseconds. One row per metric and member: summed over the members
		// NOTE: COUNT and PARENT_METRIC_NAME are NULL for some metrics (an aggregate over a NULL gives the warning SQLSTATE 01003, so they are COALESCE:d)
		String sql = ""
			+ "select /* ${cmCollectorName} */ \n"
			+ "     t.METRIC_NAME \n"
			+ "    ,COALESCE(t.PARENT_METRIC_NAME, '')                             as PARENT_METRIC_NAME \n"
			+ "    ,CAST(CASE WHEN t.METRIC_NAME in ('TOTAL_RQST_TIME', 'TOTAL_WAIT_TIME') THEN 'TOTAL' \n"
			+ "               WHEN t.METRIC_NAME = 'CLIENT_IDLE_WAIT_TIME'                 THEN 'IDLE' \n"
			+ "               WHEN t.METRIC_NAME like '%PROC_TIME'                          THEN 'PROCESSING' \n"
			+ "               ELSE 'WAIT' END AS VARCHAR(10))                              as TIME_TYPE \n"
			+ "    ,CAST(CASE WHEN COALESCE(t.PARENT_METRIC_NAME, '') in ('TOTAL_WAIT_TIME', 'TOTAL_RQST_TIME', '') THEN 1 ELSE 2 END AS SMALLINT) as LEVEL \n"
			+ "    ,SUM(COALESCE(t.TOTAL_TIME_VALUE, 0))                           as TOTAL_TIME_MS \n"
			+ "    ,SUM(COALESCE(t.COUNT, 0))                                      as COUNT \n"
			+ "    ,CAST(SUM(COALESCE(t.TOTAL_TIME_VALUE, 0)) * 1.0 / NULLIF(SUM(COALESCE(t.COUNT, 0)), 0) AS DECIMAL(18,3)) as AVG_TIME_MS \n"
			+ "    ,CAST(NULL AS DECIMAL(5,1))                                     as PCT_OF_RQST_TIME \n"
			+ "from table(MON_GET_DATABASE_DETAILS(-2)) d, \n"
			+ "     table(MON_FORMAT_XML_TIMES_BY_ROW(d.DETAILS)) t \n"
			+ "group by t.METRIC_NAME, COALESCE(t.PARENT_METRIC_NAME, '') \n"
			+ "";

		return sql;
	}

	@Override
	public boolean checkDependsOnOther(DbxConnection conn)
	{
		return Db2CmUtils.checkFunctionsAreExecutable(this, conn, "MON_GET_DATABASE_DETAILS(-2)", "MON_FORMAT_XML_TIMES_BY_ROW(CAST(NULL AS BLOB(8M)))");
	}

	@Override
	public void addMonTableDictForVersion(DbxConnection conn, DbmsVersionInfo versionInfo)
	{
		try
		{
			MonTablesDictionary mtd = MonTablesDictionaryManager.getInstance();
			mtd.addTable(CM_NAME, "Where the time of the database goes: the time spent metrics (MON_GET_DATABASE_DETAILS, one row per metric).");

			mtd.addColumn(CM_NAME, "METRIC_NAME",        "<html>The time spent metric, for example LOCK_WAIT_TIME, POOL_READ_TIME, LOG_DISK_WAIT_TIME, TOTAL_SECTION_PROC_TIME.</html>");
			mtd.addColumn(CM_NAME, "PARENT_METRIC_NAME", "<html>The metric this is a part of: TOTAL_RQST_TIME for the wait and processing totals, TOTAL_WAIT_TIME for the kinds of waits.</html>");
			mtd.addColumn(CM_NAME, "TIME_TYPE",          "<html>WAIT, PROCESSING, TOTAL (TOTAL_RQST_TIME, TOTAL_WAIT_TIME) or IDLE (CLIENT_IDLE_WAIT_TIME, not a part of the request time).</html>");
			mtd.addColumn(CM_NAME, "LEVEL",              "<html>1 = a part of TOTAL_RQST_TIME or TOTAL_WAIT_TIME (these add up). 2 = a part of a level 1 metric (for example LOCK_WAIT_TIME_GLOBAL is a part of LOCK_WAIT_TIME).</html>");
			mtd.addColumn(CM_NAME, "TOTAL_TIME_MS",      "<html>The time, in milliseconds.</html>");
			mtd.addColumn(CM_NAME, "COUNT",              "<html>Number of times (for example number of lock waits, or of page reads). 0 for the metrics that have no count.</html>");
			mtd.addColumn(CM_NAME, "AVG_TIME_MS",        "<html>Time per count, in milliseconds (for example ms per page read).<br>Abs: since the database was activated. Diff/Rate: for this sample.</html>");
			mtd.addColumn(CM_NAME, "PCT_OF_RQST_TIME",   "<html>How much of TOTAL_RQST_TIME this is, in percent (in Diff/Rate: of this sample).</html>");
		}
		catch (NameNotFoundException e)
		{
			_logger.warn("Problems in cm='" + CM_NAME + "', adding addMonTableDictForVersion. Caught: " + e);
		}
	}

	/** AVG_TIME_MS and PCT_OF_RQST_TIME: in Abs since the database was activated */
	@Override
	public void localCalculation(CounterSample newSample)
	{
		setPctOfRqstTime(newSample);
	}

	/** AVG_TIME_MS and PCT_OF_RQST_TIME: in the diff (and rate) data, for this sample only */
	@Override
	public void localCalculation(CounterSample prevSample, CounterSample newSample, CounterSample diffData)
	{
		int pos_TotalTimeMs = diffData.findColumn("TOTAL_TIME_MS");
		int pos_Count       = diffData.findColumn("COUNT");
		int pos_AvgTimeMs   = diffData.findColumn("AVG_TIME_MS");

		if (pos_TotalTimeMs == -1 || pos_Count == -1 || pos_AvgTimeMs == -1)
			return;

		for (int r = 0; r < diffData.getRowCount(); r++)
		{
			long count = diffData.getValueAsLong(r, pos_Count, 0L);
			BigDecimal avg = count <= 0 ? null : new BigDecimal(diffData.getValueAsDouble(r, pos_TotalTimeMs, 0d) / count).setScale(3, RoundingMode.HALF_EVEN);
			diffData.setValueAt(avg, r, pos_AvgTimeMs);
		}

		setPctOfRqstTime(diffData);
	}

	/** PCT_OF_RQST_TIME = TOTAL_TIME_MS / the TOTAL_TIME_MS of the row TOTAL_RQST_TIME * 100 */
	private void setPctOfRqstTime(CounterSample sample)
	{
		int pos_MetricName  = sample.findColumn("METRIC_NAME");
		int pos_TotalTimeMs = sample.findColumn("TOTAL_TIME_MS");
		int pos_Pct         = sample.findColumn("PCT_OF_RQST_TIME");

		if (pos_MetricName == -1 || pos_TotalTimeMs == -1 || pos_Pct == -1)
			return;

		double rqstTime = 0;
		for (int r = 0; r < sample.getRowCount(); r++)
			if ("TOTAL_RQST_TIME".equals(sample.getValueAsString(r, pos_MetricName)))
				rqstTime = sample.getValueAsDouble(r, pos_TotalTimeMs, 0d);

		for (int r = 0; r < sample.getRowCount(); r++)
		{
			String name = sample.getValueAsString(r, pos_MetricName);
			BigDecimal pct = (rqstTime <= 0 || "CLIENT_IDLE_WAIT_TIME".equals(name)) ? null : new BigDecimal(sample.getValueAsDouble(r, pos_TotalTimeMs, 0d) * 100.0 / rqstTime).setScale(1, RoundingMode.HALF_EVEN);
			sample.setValueAt(pct, r, pos_Pct);
		}
	}

	@Override
	public void updateGraphData(TrendGraphDataPoint tgdp)
	{
		//--------------------------------------------------------
		// The request time: wait + the processing times (ms per second)
		//--------------------------------------------------------
		if (GRAPH_NAME_RQST_TIME.equals(tgdp.getName()))
		{
			double wait = 0, section = 0, compile = 0, commit = 0, other = 0;
			for (int r = 0; r < this.getRateRowCount(); r++)
			{
				String name   = this.getRateString(r, "METRIC_NAME");
				String parent = this.getRateString(r, "PARENT_METRIC_NAME");
				Double val    = this.getRateValueAsDouble(r, "TOTAL_TIME_MS");
				if (val == null || ! "TOTAL_RQST_TIME".equals(parent))
					continue;

				if      ("TOTAL_WAIT_TIME"         .equals(name)) wait    += val;
				else if ("TOTAL_SECTION_PROC_TIME" .equals(name)) section += val;
				else if ("TOTAL_COMPILE_PROC_TIME" .equals(name) || "TOTAL_IMPLICIT_COMPILE_PROC_TIME".equals(name)) compile += val;
				else if ("TOTAL_COMMIT_PROC_TIME"  .equals(name) || "TOTAL_ROLLBACK_PROC_TIME"        .equals(name)) commit  += val;
				else                                               other   += val;
			}

			Double[] arr = new Double[] { round1(wait), round1(section), round1(compile), round1(commit), round1(other) };
			tgdp.setDataPoint(this.getTimestamp(), arr);
		}

		//--------------------------------------------------------
		// Wait time (ms per second) for the 15 kinds of waits with most wait time in this sample (the parts of TOTAL_WAIT_TIME)
		//--------------------------------------------------------
		if (GRAPH_NAME_WAIT_TIME.equals(tgdp.getName()))
		{
			// Get rows that has waited, then sort on wait time: highest first
			List<Integer> rowIds = new ArrayList<>();
			for (int r = 0; r < this.getRateRowCount(); r++)
			{
				Double val = this.getRateValueAsDouble(r, "TOTAL_TIME_MS");
				if (val != null && val > 0 && "TOTAL_WAIT_TIME".equals(this.getRateString(r, "PARENT_METRIC_NAME")))
					rowIds.add(r);
			}
			rowIds.sort((r1, r2) -> this.getRateValueAsDouble(r2, "TOTAL_TIME_MS").compareTo(this.getRateValueAsDouble(r1, "TOTAL_TIME_MS")));

			int top = Math.min(15, rowIds.size());
			if (top > 0)
			{
				Double[] dArray = new Double[top];
				String[] lArray = new String[top];
				for (int i = 0; i < top; i++)
				{
					int r = rowIds.get(i);

					lArray[i] = this.getRateString(r, "METRIC_NAME");
					dArray[i] = round1(this.getRateValueAsDouble(r, "TOTAL_TIME_MS"));
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
	}
}
