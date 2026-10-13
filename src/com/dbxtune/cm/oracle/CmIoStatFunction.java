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
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.ArrayList;
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
public class CmIoStatFunction
extends CountersModel
{
	private static final Logger _logger = LogManager.getLogger(MethodHandles.lookup().lookupClass());
	private static final long    serialVersionUID = 1L;

	public static final String   CM_NAME          = CmIoStatFunction.class.getSimpleName();
	public static final String   SHORT_NAME       = "IO by Function";
	public static final String   HTML_DESC        =
		"<html>" +
		"Disk I/O by what the database is doing it for: who (which part of Oracle) reads and writes.<br>" +
		"<br>" +
		"The most important functions:" +
		"<ul>" +
		"  <li><b>Buffer Cache Reads</b> - blocks read into the buffer cache by the sessions (normal reads by SQL statements).</li>" +
		"  <li><b>Direct Reads</b>       - reads that go around the buffer cache (full scans of big tables, parallel query).</li>" +
		"  <li><b>Direct Writes</b>      - writes that go around the buffer cache (direct path loads, sorts to temp).</li>" +
		"  <li><b>DBWR</b>               - the database writer: changed blocks written to the data files.</li>" +
		"  <li><b>LGWR</b>               - the log writer: redo written to the online redo logs.</li>" +
		"  <li><b>ARCH</b>               - the archiver: copies of the redo logs.</li>" +
		"  <li><b>RMAN</b>               - backup and restore.</li>" +
		"  <li><b>Others</b>             - everything else (for example the control file).</li>" +
		"</ul>" +
		"'Small' is single block requests, 'large' is multi block requests. <code>AVG_WAIT_MS</code> is the average wait for an I/O (in 'diff' and 'rate': for the sample).<br>" +
		"</html>";

	public static final String   GROUP_NAME       = MainFrame.TCP_GROUP_DISK;
	public static final String   GUI_ICON_FILE    = "images/" + CM_NAME + ".png";

	public static final long     NEED_SRV_VERSION = 0;
	public static final long     NEED_CE_VERSION  = 0;

	public static final String[] MON_TABLES       = new String[] {CM_NAME, "V$IOSTAT_FUNCTION"};
	public static final String[] NEED_ROLES       = new String[] {};
	public static final String[] NEED_CONFIG      = new String[] {};

	public static final String[] PCT_COLUMNS      = new String[] {};
	public static final String[] DIFF_COLUMNS     = new String[] {
		"TOTAL_MEGABYTES",
		"TOTAL_REQS",
		"SMALL_READ_MEGABYTES",
		"SMALL_WRITE_MEGABYTES",
		"LARGE_READ_MEGABYTES",
		"LARGE_WRITE_MEGABYTES",
		"SMALL_READ_REQS",
		"SMALL_WRITE_REQS",
		"LARGE_READ_REQS",
		"LARGE_WRITE_REQS",
		"NUMBER_OF_WAITS",
		"WAIT_TIME"
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

		return new CmIoStatFunction(counterController, guiController);
	}

	public CmIoStatFunction(ICounterController counterController, IGuiController guiController)
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
	public static final String GRAPH_NAME_MB_PER_SEC  = "IoFuncMbPerSec";
	public static final String GRAPH_NAME_REQ_PER_SEC = "IoFuncReqPerSec";

	private void addTrendGraphs()
	{
		addTrendGraph(GRAPH_NAME_MB_PER_SEC,
			"IO by Function, MB per Second", 	                // Menu CheckBox text
			"Disk IO in MB per Second, by Function: DBWR, LGWR, Buffer Cache Reads, Direct Reads... (" + SHORT_NAME + ")", // Graph Label
			TrendGraphDataPoint.createGraphProps(TrendGraphDataPoint.Y_AXIS_SCALE_LABELS_MB, CentralPersistReader.SampleType.MAX_OVER_SAMPLES),
			null,
			LabelType.Dynamic,
			TrendGraphDataPoint.Category.DISK,
			false, // is Percent Graph
			false, // visible at start
			0,     // graph is valid from Server Version. 0 = All Versions; >0 = Valid from this version and above
			-1);   // minimum height

		addTrendGraph(GRAPH_NAME_REQ_PER_SEC,
			"IO by Function, Requests per Second", 	                // Menu CheckBox text
			"Disk IO Requests per Second, by Function: DBWR, LGWR, Buffer Cache Reads, Direct Reads... (" + SHORT_NAME + ")", // Graph Label
			TrendGraphDataPoint.createGraphProps(TrendGraphDataPoint.Y_AXIS_SCALE_LABELS_PERSEC, CentralPersistReader.SampleType.MAX_OVER_SAMPLES),
			null,
			LabelType.Dynamic,
			TrendGraphDataPoint.Category.DISK,
			false, // is Percent Graph
			false, // visible at start
			0,     // graph is valid from Server Version. 0 = All Versions; >0 = Valid from this version and above
			-1);   // minimum height
	}

	@Override
	public List<String> getPkForVersion(DbxConnection conn, DbmsVersionInfo versionInfo)
	{
		List <String> pkCols = new LinkedList<String>();

		pkCols.add("FUNCTION_ID");

		return pkCols;
	}

	/** If we can't read the views: disable the CM, and tell what to do about it */
	@Override
	public boolean checkDependsOnOther(DbxConnection conn)
	{
		return OracleCmUtils.checkViewsAreReadable(this, conn, "V$IOSTAT_FUNCTION");
	}

	@Override
	public String getSqlForVersion(DbxConnection conn, DbmsVersionInfo versionInfo)
	{
		// One row for every function (who does the IO). This looks the same in all versions (in a CDB it's for the whole CDB)
		// AVG_WAIT_MS is since the instance was started here: for DIFF and RATE it's calculated for the sample in localCalculation()
		// Note: CAST(... as NUMBER(#,3)) is needed to keep decimals (a NUMBER without precision is treated as a BIGINT)
		String sql = ""
			+ "select \n"
			+ "     f.FUNCTION_ID \n"
			+ "    ,f.FUNCTION_NAME \n"
			+ "    ,f.SMALL_READ_MEGABYTES + f.SMALL_WRITE_MEGABYTES + f.LARGE_READ_MEGABYTES + f.LARGE_WRITE_MEGABYTES as TOTAL_MEGABYTES \n"
			+ "    ,f.SMALL_READ_REQS      + f.SMALL_WRITE_REQS      + f.LARGE_READ_REQS      + f.LARGE_WRITE_REQS      as TOTAL_REQS \n"
			+ "    ,CAST(f.WAIT_TIME / NULLIF(f.NUMBER_OF_WAITS, 0) as NUMBER(18,3))                                    as AVG_WAIT_MS \n"
			+ "    ,f.SMALL_READ_MEGABYTES \n"
			+ "    ,f.SMALL_WRITE_MEGABYTES \n"
			+ "    ,f.LARGE_READ_MEGABYTES \n"
			+ "    ,f.LARGE_WRITE_MEGABYTES \n"
			+ "    ,f.SMALL_READ_REQS \n"
			+ "    ,f.SMALL_WRITE_REQS \n"
			+ "    ,f.LARGE_READ_REQS \n"
			+ "    ,f.LARGE_WRITE_REQS \n"
			+ "    ,f.NUMBER_OF_WAITS \n"
			+ "    ,f.WAIT_TIME \n"
			+ "from V$IOSTAT_FUNCTION f \n"
			+ "order by f.FUNCTION_ID \n"
			+ "";

		return sql;
	}

	@Override
	public void addMonTableDictForVersion(DbxConnection conn, DbmsVersionInfo versionInfo)
	{
		try
		{
			MonTablesDictionary mtd = MonTablesDictionaryManager.getInstance();

			mtd.addTable(CM_NAME, "Disk I/O by what the database is doing it for (DBWR, LGWR, Buffer Cache Reads, Direct Reads, RMAN...).");

			mtd.addColumn(CM_NAME, "FUNCTION_ID",           "<html>Oracle's id for the function.</html>");
			mtd.addColumn(CM_NAME, "FUNCTION_NAME",         "<html>Who does the I/O: RMAN, DBWR, LGWR, ARCH, Buffer Cache Reads, Direct Reads, Direct Writes, Recovery, Data Pump, Others...</html>");
			mtd.addColumn(CM_NAME, "TOTAL_MEGABYTES",       "<html>All I/O in MB.<br><b>Formula</b>: SMALL_READ_MEGABYTES + SMALL_WRITE_MEGABYTES + LARGE_READ_MEGABYTES + LARGE_WRITE_MEGABYTES</html>");
			mtd.addColumn(CM_NAME, "TOTAL_REQS",            "<html>All I/O requests.<br><b>Formula</b>: SMALL_READ_REQS + SMALL_WRITE_REQS + LARGE_READ_REQS + LARGE_WRITE_REQS</html>");
			mtd.addColumn(CM_NAME, "AVG_WAIT_MS",           "<html>Average wait for an I/O, in milliseconds. In 'abs': since the instance was started. In 'diff' and 'rate': in the sample.<br><b>Formula</b>: WAIT_TIME / NUMBER_OF_WAITS</html>");
			mtd.addColumn(CM_NAME, "SMALL_READ_MEGABYTES",  "<html>MB read with single block requests.</html>");
			mtd.addColumn(CM_NAME, "SMALL_WRITE_MEGABYTES", "<html>MB written with single block requests.</html>");
			mtd.addColumn(CM_NAME, "LARGE_READ_MEGABYTES",  "<html>MB read with multi block requests.</html>");
			mtd.addColumn(CM_NAME, "LARGE_WRITE_MEGABYTES", "<html>MB written with multi block requests.</html>");
			mtd.addColumn(CM_NAME, "SMALL_READ_REQS",       "<html>Number of single block read requests.</html>");
			mtd.addColumn(CM_NAME, "SMALL_WRITE_REQS",      "<html>Number of single block write requests.</html>");
			mtd.addColumn(CM_NAME, "LARGE_READ_REQS",       "<html>Number of multi block read requests.</html>");
			mtd.addColumn(CM_NAME, "LARGE_WRITE_REQS",      "<html>Number of multi block write requests.</html>");
			mtd.addColumn(CM_NAME, "NUMBER_OF_WAITS",       "<html>Number of I/O waits.</html>");
			mtd.addColumn(CM_NAME, "WAIT_TIME",             "<html>Total wait time for I/O, in milliseconds.</html>");
		}
		catch (NameNotFoundException e)
		{
			_logger.warn("Problems in cm='" + CM_NAME + "', adding addMonTableDictForVersion. Caught: " + e);
		}
	}

	/**
	 * AVG_WAIT_MS: The SQL gives the average since the instance was started.<br>
	 * For DIFF (and RATE, which gets it from DIFF) we want the average for the waits that happened in this sample
	 */
	@Override
	public void localCalculation(CounterSample prevSample, CounterSample newSample, CounterSample diffData)
	{
		int pos_NUMBER_OF_WAITS = diffData.findColumn("NUMBER_OF_WAITS");
		int pos_WAIT_TIME       = diffData.findColumn("WAIT_TIME");
		int pos_AVG_WAIT_MS     = diffData.findColumn("AVG_WAIT_MS");

		if (pos_NUMBER_OF_WAITS == -1 || pos_WAIT_TIME == -1 || pos_AVG_WAIT_MS == -1)
		{
			_logger.warn(getName() + ": localCalculation(), Problems finding one of the columns: NUMBER_OF_WAITS, WAIT_TIME, AVG_WAIT_MS.");
			return;
		}

		for (int rowId=0; rowId < diffData.getRowCount(); rowId++)
		{
			long waits    = diffData.getValueAsLong(rowId, pos_NUMBER_OF_WAITS);
			long waitTime = diffData.getValueAsLong(rowId, pos_WAIT_TIME);

			BigDecimal avgWaitMs = (waits <= 0) ? new BigDecimal(0) : MathUtils.roundToBigDecimal(waitTime * 1.0 / waits, 3);

			diffData.setValueAt(avgWaitMs, rowId, pos_AVG_WAIT_MS);
		}
	}

	@Override
	public void updateGraphData(TrendGraphDataPoint tgdp)
	{
		if (this.size() == 0 || ! this.hasRateData())
			return;

		// Both graphs: one "line" for every function that has done any IO since the instance was started (so the functions that are not used does not clutter the graph)
		String colName = null;
		if (GRAPH_NAME_MB_PER_SEC .equals(tgdp.getName())) colName = "TOTAL_MEGABYTES";
		if (GRAPH_NAME_REQ_PER_SEC.equals(tgdp.getName())) colName = "TOTAL_REQS";

		if (colName != null)
		{
			LinkedHashMap<String, Double> dataMap = new LinkedHashMap<String, Double>();

			for (int r = 0; r < this.size(); r++)
			{
				if (this.getAbsValueAsDouble(r, "TOTAL_REQS", 0d) <= 0)
					continue;

				Double perSec = this.getRateValueAsDouble(r, colName);
				dataMap.put(this.getAbsString(r, "FUNCTION_NAME"), (perSec == null || perSec < 0) ? 0d : perSec);
			}

			if (dataMap.size() > 0)
				tgdp.setData(this.getTimestamp(), dataMap);
		}
	}

	/** Local graph (beside the table): which part of the database does the I/O */
	@Override
	public List<CmChartDescriptor> createChartDescriptors()
	{
		List<CmChartDescriptor> list = new ArrayList<>();

		list.add(new CmChartDescriptor()
			.id("io-by-function-mb")
			.title("I/O by Function (MB)")
			.chartType(CmChartDescriptor.CHART_TYPE_DUAL_PIE_BAR)
			.labelColumn("FUNCTION_NAME")
			.valueColumns("TOTAL_MEGABYTES")
			.seriesLabels("MB")
			.skipZeroRows(true)
			.pieOtherLimit(0.02)
			.splitRatio(0.6));

		list.add(new CmChartDescriptor()
			.id("io-by-function-reqs")
			.title("I/O by Function (requests)")
			.chartType(CmChartDescriptor.CHART_TYPE_DUAL_PIE_BAR)
			.labelColumn("FUNCTION_NAME")
			.valueColumns("TOTAL_REQS")
			.seriesLabels("Requests")
			.skipZeroRows(true)
			.pieOtherLimit(0.02));

		return list;
	}
}
