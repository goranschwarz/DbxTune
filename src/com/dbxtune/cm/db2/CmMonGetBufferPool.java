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
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

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
 * The buffer pools: size, hit ratios, physical reads (and how much the prefetchers did), writes, and the page cleaners.
 *
 * @author Goran Schwarz (goran_schwarz@hotmail.com)
 */
public class CmMonGetBufferPool
extends CountersModel
{
	private static final Logger _logger = LogManager.getLogger(MethodHandles.lookup().lookupClass());
	private static final long    serialVersionUID = 1L;

	public static final String   CM_NAME          = CmMonGetBufferPool.class.getSimpleName();
	public static final String   SHORT_NAME       = "BufferPool";
	public static final String   HTML_DESC        =
		"<html>" +
		"<h4>Buffer Pools</h4>" +
		"The buffer pools (one row per buffer pool and member): size, hit ratios, physical reads, how much of it the prefetchers did, writes and the page cleaners.<br>" +
		"<br>" +
		"The <code>_PCT</code> and <code>AVG_</code> columns are for the last sample in the Diff and Rate view, and since the database was activated in the Abs view.<br>" +
		"<b>Note</b>: IBMSYSTEMBPxxK are hidden buffer pools, only used when a real buffer pool could not be allocated at activation (then there is an ADM6073W in the notification log).<br>" +
		"</html>";

	public static final String   GROUP_NAME       = MainFrame.TCP_GROUP_CACHE;
	public static final String   GUI_ICON_FILE    = "images/" + CM_NAME + ".png";

	public static final long     NEED_SRV_VERSION = 0;
	public static final long     NEED_CE_VERSION  = 0;

	public static final String[] MON_TABLES       = new String[] {"MON_GET_BUFFERPOOL", CM_NAME};
	public static final String[] NEED_ROLES       = new String[] {};
	public static final String[] NEED_CONFIG      = new String[] {};

	public static final String[] PCT_COLUMNS      = new String[] {};
	public static final String[] DIFF_COLUMNS     = new String[] {
		"L_READS",
		"P_READS",
		"DATA_L_READS",
		"DATA_P_READS",
		"INDEX_L_READS",
		"INDEX_P_READS",
		"TEMP_L_READS",
		"TEMP_P_READS",
		"ASYNC_READS",
		"WRITES",
		"ASYNC_WRITES",
		"POOL_READ_TIME",
		"POOL_WRITE_TIME",
		"POOL_NO_VICTIM_BUFFER",
		"POOL_DRTY_PG_STEAL_CLNS",
		"POOL_DRTY_PG_THRSH_CLNS",
		"POOL_LSN_GAP_CLNS",
		"UNREAD_PREFETCH_PAGES",
		"PREFETCH_WAITS",
		"PREFETCH_WAIT_TIME",
		"DIRECT_READS",
		"DIRECT_WRITES",
		"FILES_CLOSED"
	};

	// The counters starts from 0 when the database is activated again
	public static final boolean  NEGATIVE_DIFF_COUNTERS_TO_ZERO = true;
	public static final boolean  IS_SYSTEM_CM                   = true;
	public static final int      DEFAULT_POSTPONE_TIME          = 0;
	public static final int      DEFAULT_QUERY_TIMEOUT          = 60; //CountersModel.DEFAULT_sqlQueryTimeout;

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

		return new CmMonGetBufferPool(counterController, guiController);
	}

	public CmMonGetBufferPool(ICounterController counterController, IGuiController guiController)
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
	public static final String GRAPH_NAME_HIT_PCT = "BpHitPct";
	public static final String GRAPH_NAME_P_READS = "BpPhysReads";

	private void addTrendGraphs()
	{
		addTrendGraph(GRAPH_NAME_HIT_PCT,
			"Buffer Pool Hit Ratio, per Buffer Pool",                       // Menu CheckBox text
			"Buffer Pool Hit Ratio in Percent, per Buffer Pool (" + SHORT_NAME + ")", // Graph Label
			TrendGraphDataPoint.createGraphProps(TrendGraphDataPoint.Y_AXIS_SCALE_LABELS_PERCENT, CentralPersistReader.SampleType.MIN_OVER_SAMPLES),
			null,
			LabelType.Dynamic,
			TrendGraphDataPoint.Category.CACHE,
			true,  // is Percent Graph
			true,  // visible at start
			0,     // graph is valid from Server Version. 0 = All Versions; >0 = Valid from this version and above
			-1);   // minimum height

		addTrendGraph(GRAPH_NAME_P_READS,
			"Buffer Pool Physical Reads per Second, per Buffer Pool",       // Menu CheckBox text
			"Buffer Pool Physical Reads (pages read from disk) per Second, per Buffer Pool (" + SHORT_NAME + ")", // Graph Label
			TrendGraphDataPoint.createGraphProps(TrendGraphDataPoint.Y_AXIS_SCALE_LABELS_PERSEC, CentralPersistReader.SampleType.MAX_OVER_SAMPLES),
			null,
			LabelType.Dynamic,
			TrendGraphDataPoint.Category.DISK,
			false, // is Percent Graph
			true,  // visible at start
			0,     // graph is valid from Server Version. 0 = All Versions; >0 = Valid from this version and above
			-1);   // minimum height
	}

	@Override
	public String[] getDependsOnConfigForVersion(DbxConnection conn, DbmsVersionInfo versionInfo)
	{
		return NEED_CONFIG;
	}

	@Override
	public boolean checkDependsOnOther(DbxConnection conn)
	{
		return Db2CmUtils.checkFunctionsAreExecutable(this, conn, "MON_GET_BUFFERPOOL('', -2)");
	}

	@Override
	public void addMonTableDictForVersion(DbxConnection conn, DbmsVersionInfo versionInfo)
	{
		try
		{
			MonTablesDictionary mtd = MonTablesDictionaryManager.getInstance();
			mtd.addTable(CM_NAME,  "The buffer pools (MON_GET_BUFFERPOOL).");

			mtd.addColumn(CM_NAME, "BP_NAME",                 "<html>Buffer pool name.</html>");
			mtd.addColumn(CM_NAME, "MEMBER",                  "<html>The database member (0 if this is not pureScale or DPF).</html>");
			mtd.addColumn(CM_NAME, "AUTOMATIC",               "<html>1 = the size is tuned by STMM (self tuning memory).</html>");
			mtd.addColumn(CM_NAME, "PAGESIZE",                "<html>Page size in bytes.</html>");
			mtd.addColumn(CM_NAME, "SIZE_PAGES",              "<html>The current size, in pages (BP_CUR_BUFFSZ).</html>");
			mtd.addColumn(CM_NAME, "SIZE_MB",                 "<html>The current size, in MB.</html>");
			mtd.addColumn(CM_NAME, "TBSP_USE_COUNT",          "<html>Number of table spaces that use this buffer pool.</html>");
			mtd.addColumn(CM_NAME, "L_READS",                 "<html>Logical reads: pages read from the buffer pool (data, index, xda, column organized, also temporary).</html>");
			mtd.addColumn(CM_NAME, "P_READS",                 "<html>Physical reads: pages that had to be read from disk (by the agent or by prefetch).</html>");
			mtd.addColumn(CM_NAME, "HIT_PCT",                 "<html>Hit ratio: logical reads that did not need a disk read.<br><b>Formula</b>: 100 - 100 * P_READS / L_READS</html>");
			mtd.addColumn(CM_NAME, "DATA_L_READS",            "<html>Logical reads of data pages (also temporary).</html>");
			mtd.addColumn(CM_NAME, "DATA_P_READS",            "<html>Physical reads of data pages (also temporary).</html>");
			mtd.addColumn(CM_NAME, "DATA_HIT_PCT",            "<html>Hit ratio for data pages.<br><b>Formula</b>: 100 - 100 * DATA_P_READS / DATA_L_READS</html>");
			mtd.addColumn(CM_NAME, "INDEX_L_READS",           "<html>Logical reads of index pages (also temporary).</html>");
			mtd.addColumn(CM_NAME, "INDEX_P_READS",           "<html>Physical reads of index pages (also temporary).</html>");
			mtd.addColumn(CM_NAME, "INDEX_HIT_PCT",           "<html>Hit ratio for index pages. It should be high (above 95%) in an OLTP system.<br><b>Formula</b>: 100 - 100 * INDEX_P_READS / INDEX_L_READS</html>");
			mtd.addColumn(CM_NAME, "TEMP_L_READS",            "<html>Logical reads of temporary pages (sorts, hash joins that spilled).</html>");
			mtd.addColumn(CM_NAME, "TEMP_P_READS",            "<html>Physical reads of temporary pages.</html>");
			mtd.addColumn(CM_NAME, "ASYNC_READS",             "<html>Pages read by the prefetchers (asynchronous, read ahead).</html>");
			mtd.addColumn(CM_NAME, "ASYNC_READ_PCT",          "<html>Physical reads that were done by the prefetchers, in percent. High = scans (good for big reports, bad for OLTP).<br><b>Formula</b>: 100 * ASYNC_READS / P_READS</html>");
			mtd.addColumn(CM_NAME, "WRITES",                  "<html>Pages written to disk.</html>");
			mtd.addColumn(CM_NAME, "ASYNC_WRITES",            "<html>Pages written by the page cleaners (asynchronous). Good: the agents did not have to write them.</html>");
			mtd.addColumn(CM_NAME, "POOL_READ_TIME",          "<html>Time spent on physical reads, in milliseconds.</html>");
			mtd.addColumn(CM_NAME, "POOL_WRITE_TIME",         "<html>Time spent on physical writes, in milliseconds.</html>");
			mtd.addColumn(CM_NAME, "AVG_READ_MS",             "<html>Average time per physical read, in milliseconds.<br><b>Formula</b>: POOL_READ_TIME / P_READS</html>");
			mtd.addColumn(CM_NAME, "POOL_NO_VICTIM_BUFFER",   "<html>Times an agent did not find a free page to use (it had to wait or write one itself). Above 0: the buffer pool is too small, or the page cleaners can not keep up.</html>");
			mtd.addColumn(CM_NAME, "POOL_DRTY_PG_STEAL_CLNS", "<html>Times the page cleaners were started because an agent needed a page and found only dirty ones. Should be 0.</html>");
			mtd.addColumn(CM_NAME, "POOL_DRTY_PG_THRSH_CLNS", "<html>Times the page cleaners were started because the dirty page threshold (CHNGPGS_THRESH) was reached.</html>");
			mtd.addColumn(CM_NAME, "POOL_LSN_GAP_CLNS",       "<html>Times the page cleaners were started because the log (SOFTMAX / PAGE_AGE_TRGT_MCR) was too far ahead of the oldest dirty page.</html>");
			mtd.addColumn(CM_NAME, "UNREAD_PREFETCH_PAGES",   "<html>Pages the prefetchers read, that were never used.</html>");
			mtd.addColumn(CM_NAME, "PREFETCH_WAITS",          "<html>Times an agent waited for a prefetcher.</html>");
			mtd.addColumn(CM_NAME, "PREFETCH_WAIT_TIME",      "<html>Time agents waited for prefetchers, in milliseconds.</html>");
			mtd.addColumn(CM_NAME, "DIRECT_READS",            "<html>Direct reads (LOB, LONG data).</html>");
			mtd.addColumn(CM_NAME, "DIRECT_WRITES",           "<html>Direct writes (LOB, LONG data).</html>");
			mtd.addColumn(CM_NAME, "FILES_CLOSED",            "<html>Database files that were closed (MAXFILOP reached). It should be 0.</html>");
		}
		catch (NameNotFoundException e)
		{
			_logger.warn("Problems in cm='" + CM_NAME + "', adding addMonTableDictForVersion. Caught: " + e);
		}
	}

	@Override
	public List<String> getPkForVersion(DbxConnection conn, DbmsVersionInfo versionInfo)
	{
		List <String> pkCols = new LinkedList<String>();

		pkCols.add("BP_NAME");
		pkCols.add("MEMBER");

		return pkCols;
	}

	@Override
	public String getSqlForVersion(DbxConnection conn, DbmsVersionInfo versionInfo)
	{
		// The _PCT and AVG_ columns are calculated in localCalculation()
		String sql = ""
			+ "select /* ${cmCollectorName} */ \n"
			+ "     RTRIM(b.BP_NAME) as BP_NAME \n"
			+ "    ,b.MEMBER \n"
			+ "    ,b.AUTOMATIC \n"
			+ "    ,c.PAGESIZE \n"
			+ "    ,b.BP_CUR_BUFFSZ as SIZE_PAGES \n"
			+ "    ,CAST(b.BP_CUR_BUFFSZ * COALESCE(c.PAGESIZE, 4096) / 1048576.0 AS DECIMAL(15,1)) as SIZE_MB \n"
			+ "    ,b.BP_TBSP_USE_COUNT as TBSP_USE_COUNT \n"
			+ "    ,(b.POOL_DATA_L_READS + b.POOL_INDEX_L_READS + b.POOL_XDA_L_READS + b.POOL_COL_L_READS + b.POOL_TEMP_DATA_L_READS + b.POOL_TEMP_INDEX_L_READS + b.POOL_TEMP_XDA_L_READS + b.POOL_TEMP_COL_L_READS) as L_READS \n"
			+ "    ,(b.POOL_DATA_P_READS + b.POOL_INDEX_P_READS + b.POOL_XDA_P_READS + b.POOL_COL_P_READS + b.POOL_TEMP_DATA_P_READS + b.POOL_TEMP_INDEX_P_READS + b.POOL_TEMP_XDA_P_READS + b.POOL_TEMP_COL_P_READS) as P_READS \n"
			+ "    ,CAST(NULL AS DECIMAL(5,1)) as HIT_PCT \n"
			+ "    ,(b.POOL_DATA_L_READS  + b.POOL_TEMP_DATA_L_READS)  as DATA_L_READS \n"
			+ "    ,(b.POOL_DATA_P_READS  + b.POOL_TEMP_DATA_P_READS)  as DATA_P_READS \n"
			+ "    ,CAST(NULL AS DECIMAL(5,1)) as DATA_HIT_PCT \n"
			+ "    ,(b.POOL_INDEX_L_READS + b.POOL_TEMP_INDEX_L_READS) as INDEX_L_READS \n"
			+ "    ,(b.POOL_INDEX_P_READS + b.POOL_TEMP_INDEX_P_READS) as INDEX_P_READS \n"
			+ "    ,CAST(NULL AS DECIMAL(5,1)) as INDEX_HIT_PCT \n"
			+ "    ,(b.POOL_TEMP_DATA_L_READS + b.POOL_TEMP_INDEX_L_READS + b.POOL_TEMP_XDA_L_READS + b.POOL_TEMP_COL_L_READS) as TEMP_L_READS \n"
			+ "    ,(b.POOL_TEMP_DATA_P_READS + b.POOL_TEMP_INDEX_P_READS + b.POOL_TEMP_XDA_P_READS + b.POOL_TEMP_COL_P_READS) as TEMP_P_READS \n"
			+ "    ,(b.POOL_ASYNC_DATA_READS + b.POOL_ASYNC_INDEX_READS + b.POOL_ASYNC_XDA_READS + b.POOL_ASYNC_COL_READS) as ASYNC_READS \n"
			+ "    ,CAST(NULL AS DECIMAL(5,1)) as ASYNC_READ_PCT \n"
			+ "    ,(b.POOL_DATA_WRITES + b.POOL_INDEX_WRITES + b.POOL_XDA_WRITES + b.POOL_COL_WRITES) as WRITES \n"
			+ "    ,(b.POOL_ASYNC_DATA_WRITES + b.POOL_ASYNC_INDEX_WRITES + b.POOL_ASYNC_XDA_WRITES + b.POOL_ASYNC_COL_WRITES) as ASYNC_WRITES \n"
			+ "    ,b.POOL_READ_TIME \n"
			+ "    ,b.POOL_WRITE_TIME \n"
			+ "    ,CAST(NULL AS DECIMAL(12,3)) as AVG_READ_MS \n"
			+ "    ,b.POOL_NO_VICTIM_BUFFER \n"
			+ "    ,b.POOL_DRTY_PG_STEAL_CLNS \n"
			+ "    ,b.POOL_DRTY_PG_THRSH_CLNS \n"
			+ "    ,b.POOL_LSN_GAP_CLNS \n"
			+ "    ,b.UNREAD_PREFETCH_PAGES \n"
			+ "    ,b.PREFETCH_WAITS \n"
			+ "    ,b.PREFETCH_WAIT_TIME \n"
			+ "    ,b.DIRECT_READS \n"
			+ "    ,b.DIRECT_WRITES \n"
			+ "    ,b.FILES_CLOSED \n"
			+ "from table(MON_GET_BUFFERPOOL('', -2)) b \n"
			+ "left join SYSCAT.BUFFERPOOLS c on c.BPNAME = b.BP_NAME \n"
			+ "order by b.BP_NAME, b.MEMBER \n"
			+ "";

		return sql;
	}

	/** The _PCT and AVG_ columns: since activation in the abs data */
	@Override
	public void localCalculation(CounterSample newSample)
	{
		calcRatios(newSample);
	}

	/** The _PCT and AVG_ columns: for the sample in the diff (and rate) data */
	@Override
	public void localCalculation(CounterSample prevSample, CounterSample newSample, CounterSample diffData)
	{
		calcRatios(diffData);
	}

	private void calcRatios(CounterSample data)
	{
		for (int rowId=0; rowId < data.getRowCount(); rowId++)
		{
			setPct(data, rowId, "HIT_PCT",        "P_READS",       "L_READS",       true);
			setPct(data, rowId, "DATA_HIT_PCT",   "DATA_P_READS",  "DATA_L_READS",  true);
			setPct(data, rowId, "INDEX_HIT_PCT",  "INDEX_P_READS", "INDEX_L_READS", true);
			setPct(data, rowId, "ASYNC_READ_PCT", "ASYNC_READS",   "P_READS",       false);

			int pos_avg   = data.findColumn("AVG_READ_MS");
			int pos_time  = data.findColumn("POOL_READ_TIME");
			int pos_reads = data.findColumn("P_READS");
			if (pos_avg != -1 && pos_time != -1 && pos_reads != -1)
			{
				double reads = data.getValueAsDouble(rowId, pos_reads, 0d);
				double time  = data.getValueAsDouble(rowId, pos_time,  0d);
				data.setValueAt(reads <= 0 ? null : new BigDecimal(time / reads).setScale(3, RoundingMode.HALF_EVEN), rowId, pos_avg);
			}
		}
	}

	/** pctCol = (hitRatio ? 100 - 100 * part / total : 100 * part / total), NULL if total is 0 */
	private static void setPct(CounterSample data, int rowId, String pctCol, String partCol, String totalCol, boolean hitRatio)
	{
		int pos_pct   = data.findColumn(pctCol);
		int pos_part  = data.findColumn(partCol);
		int pos_total = data.findColumn(totalCol);
		if (pos_pct == -1 || pos_part == -1 || pos_total == -1)
			return;

		double part  = data.getValueAsDouble(rowId, pos_part,  0d);
		double total = data.getValueAsDouble(rowId, pos_total, 0d);

		BigDecimal pct = null;
		if (total > 0)
		{
			double val = hitRatio ? 100.0 - 100.0 * part / total : 100.0 * part / total;
			pct = new BigDecimal(Math.max(0, Math.min(100, val))).setScale(1, RoundingMode.HALF_EVEN);
		}
		data.setValueAt(pct, rowId, pos_pct);
	}

	@Override
	public void updateGraphData(TrendGraphDataPoint tgdp)
	{
		if (GRAPH_NAME_HIT_PCT.equals(tgdp.getName()))
		{
			// One line per buffer pool that had logical reads in the sample (the hidden IBMSYSTEMBPxxK ones are normally never used)
			Map<String, double[]> map = new LinkedHashMap<>();
			for (int r = 0; r < this.getDiffRowCount(); r++)
			{
				Double lReads = this.getDiffValueAsDouble(r, "L_READS");
				Double pReads = this.getDiffValueAsDouble(r, "P_READS");
				double[] acc = map.computeIfAbsent(this.getDiffString(r, "BP_NAME"), k -> new double[2]);
				acc[0] += lReads == null ? 0 : lReads;
				acc[1] += pReads == null ? 0 : pReads;
			}
			map.values().removeIf(acc -> acc[0] <= 0);
			if ( ! map.isEmpty() )
			{
				String[] lArray = map.keySet().toArray(new String[0]);
				Double[] dArray = new Double[lArray.length];
				int i = 0;
				for (double[] acc : map.values())
					dArray[i++] = Math.max(0, 100.0 - 100.0 * acc[1] / acc[0]);
				tgdp.setDataPoint(this.getTimestamp(), lArray, dArray);
			}
		}

		if (GRAPH_NAME_P_READS.equals(tgdp.getName()))
		{
			// One line per buffer pool (the sum of the members), without the hidden ones
			Map<String, Double> map = new LinkedHashMap<>();
			for (int r = 0; r < this.getRateRowCount(); r++)
			{
				String name = this.getRateString(r, "BP_NAME");
				if (name.startsWith("IBMSYSTEMBP"))
					continue;
				Double val = this.getRateValueAsDouble(r, "P_READS");
				map.merge(name, val == null ? 0d : val, Double::sum);
			}
			tgdp.setDataPoint(this.getTimestamp(), map.keySet().toArray(new String[0]), map.values().toArray(new Double[0]));
		}
	}
}
