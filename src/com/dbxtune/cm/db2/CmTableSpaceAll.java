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
 * I/O per table space: logical and physical reads, writes, prefetch (async) and the time it took.
 * <p>
 * The CM name is kept from the old version (CmTableSpaceAll), the tab is now called "Table Space IO".
 *
 * @author Goran Schwarz (goran_schwarz@hotmail.com)
 */
public class CmTableSpaceAll
extends CountersModel
{
	private static final Logger _logger = LogManager.getLogger(MethodHandles.lookup().lookupClass());
	private static final long    serialVersionUID = 1L;

	public static final String   CM_NAME          = CmTableSpaceAll.class.getSimpleName();
	public static final String   SHORT_NAME       = "Table Space IO";
	public static final String   HTML_DESC        =
		"<html>" +
		"<h4>Table Space IO</h4>" +
		"I/O per table space (one row per table space and member): logical reads (from the buffer pool), physical reads (from disk), " +
		"prefetch (asynchronous) reads and writes, direct I/O (LOBs, LONG), and the time it took.<br>" +
		"<br>" +
		"<b>HIT_PCT</b> and <b>AVG_READ_MS</b> are for the last sample in the Diff and Rate view.<br>" +
		"The size of the table spaces is in the tab 'Table Space Size'.<br>" +
		"</html>";

	public static final String   GROUP_NAME       = MainFrame.TCP_GROUP_SERVER;
	public static final String   GUI_ICON_FILE    = "images/" + CM_NAME + ".png";

	public static final long     NEED_SRV_VERSION = 0;
	public static final long     NEED_CE_VERSION  = 0;

	public static final String[] MON_TABLES       = new String[] {"MON_GET_TABLESPACE", CM_NAME};
	public static final String[] NEED_ROLES       = new String[] {};
	public static final String[] NEED_CONFIG      = new String[] {};

	public static final String[] PCT_COLUMNS      = new String[] {};
	public static final String[] DIFF_COLUMNS     = new String[] {
		"L_READS",
		"P_READS",
		"POOL_DATA_L_READS",
		"POOL_DATA_P_READS",
		"POOL_INDEX_L_READS",
		"POOL_INDEX_P_READS",
		"POOL_TEMP_L_READS",
		"POOL_TEMP_P_READS",
		"POOL_ASYNC_READS",
		"POOL_WRITES",
		"POOL_ASYNC_WRITES",
		"POOL_READ_TIME",
		"POOL_WRITE_TIME",
		"POOL_ASYNC_READ_TIME",
		"DIRECT_READS",
		"DIRECT_WRITES",
		"DIRECT_READ_TIME",
		"DIRECT_WRITE_TIME",
		"UNREAD_PREFETCH_PAGES",
		"PREFETCH_WAITS",
		"PREFETCH_WAIT_TIME",
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
	@Override public Type    getTemplateLevel()                       { return Type.MEDIUM; }

	/**
	 * FACTORY  method to create the object
	 */
	public static CountersModel create(ICounterController counterController, IGuiController guiController)
	{
		if (guiController != null && guiController.hasGUI())
			guiController.splashWindowProgress("Loading: Counter Model '" + CM_NAME + "'");

		return new CmTableSpaceAll(counterController, guiController);
	}

	public CmTableSpaceAll(ICounterController counterController, IGuiController guiController)
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
	public static final String GRAPH_NAME_P_READS     = "TbspPhysReads";
	public static final String GRAPH_NAME_AVG_READ_MS = "TbspAvgReadMs";

	private void addTrendGraphs()
	{
		addTrendGraph(GRAPH_NAME_P_READS,
			"Table Space Physical Reads per Second",                       // Menu CheckBox text
			"Table Space Physical Reads (pages read from disk) per Second, per Table Space (" + SHORT_NAME + ")", // Graph Label
			TrendGraphDataPoint.createGraphProps(TrendGraphDataPoint.Y_AXIS_SCALE_LABELS_PERSEC, CentralPersistReader.SampleType.MAX_OVER_SAMPLES),
			null,
			LabelType.Dynamic,
			TrendGraphDataPoint.Category.DISK,
			false, // is Percent Graph
			true,  // visible at start
			0,     // graph is valid from Server Version. 0 = All Versions; >0 = Valid from this version and above
			-1);   // minimum height

		addTrendGraph(GRAPH_NAME_AVG_READ_MS,
			"Table Space Read Time, ms per Physical Read",                 // Menu CheckBox text
			"Table Space Read Time, Average ms per Physical Read, per Table Space (" + SHORT_NAME + ")", // Graph Label
			TrendGraphDataPoint.createGraphProps(TrendGraphDataPoint.Y_AXIS_SCALE_LABELS_MILLISEC, CentralPersistReader.SampleType.MAX_OVER_SAMPLES),
			null,
			LabelType.Dynamic,
			TrendGraphDataPoint.Category.DISK,
			false, // is Percent Graph
			false, // visible at start
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
		return Db2CmUtils.checkFunctionsAreExecutable(this, conn, "MON_GET_TABLESPACE('', -2)");
	}

	@Override
	public void addMonTableDictForVersion(DbxConnection conn, DbmsVersionInfo versionInfo)
	{
		try
		{
			MonTablesDictionary mtd = MonTablesDictionaryManager.getInstance();
			mtd.addTable(CM_NAME,  "I/O per table space.");

			mtd.addColumn(CM_NAME, "TBSP_NAME",             "<html>Table space name.</html>");
			mtd.addColumn(CM_NAME, "TBSP_ID",               "<html>Table space id.</html>");
			mtd.addColumn(CM_NAME, "MEMBER",                "<html>The database member (0 if this is not pureScale or DPF).</html>");
			mtd.addColumn(CM_NAME, "TBSP_TYPE",             "<html>DMS or SMS.</html>");
			mtd.addColumn(CM_NAME, "TBSP_CONTENT_TYPE",     "<html>ANY / LARGE (regular data), SYSTEMP (system temporary), USRTEMP (user temporary).</html>");
			mtd.addColumn(CM_NAME, "L_READS",               "<html>Logical reads: pages read from the buffer pool (data, index, xda, column organized, also temporary).</html>");
			mtd.addColumn(CM_NAME, "P_READS",               "<html>Physical reads: pages that had to be read from disk (by the agent or by prefetch).</html>");
			mtd.addColumn(CM_NAME, "HIT_PCT",               "<html>Buffer pool hit ratio for this table space.<br><b>Formula</b>: 100 - 100 * P_READS / L_READS (Abs: since activation. Diff and Rate: in the sample)</html>");
			mtd.addColumn(CM_NAME, "POOL_DATA_L_READS",     "<html>Logical reads of data pages.</html>");
			mtd.addColumn(CM_NAME, "POOL_DATA_P_READS",     "<html>Physical reads of data pages.</html>");
			mtd.addColumn(CM_NAME, "POOL_INDEX_L_READS",    "<html>Logical reads of index pages.</html>");
			mtd.addColumn(CM_NAME, "POOL_INDEX_P_READS",    "<html>Physical reads of index pages.</html>");
			mtd.addColumn(CM_NAME, "POOL_TEMP_L_READS",     "<html>Logical reads of temporary pages (sorts, hash joins, temporary tables that spilled).</html>");
			mtd.addColumn(CM_NAME, "POOL_TEMP_P_READS",     "<html>Physical reads of temporary pages.</html>");
			mtd.addColumn(CM_NAME, "POOL_ASYNC_READS",      "<html>Pages read by prefetchers (asynchronous: read ahead, the agent did not wait for them).</html>");
			mtd.addColumn(CM_NAME, "POOL_WRITES",           "<html>Pages written to disk (data, index, xda, column organized).</html>");
			mtd.addColumn(CM_NAME, "POOL_ASYNC_WRITES",     "<html>Pages written by the page cleaners (asynchronous). Good: the agents did not have to write them.</html>");
			mtd.addColumn(CM_NAME, "POOL_READ_TIME",        "<html>Time spent on physical reads, in milliseconds (also the prefetchers' reads).</html>");
			mtd.addColumn(CM_NAME, "POOL_WRITE_TIME",       "<html>Time spent on physical writes, in milliseconds.</html>");
			mtd.addColumn(CM_NAME, "POOL_ASYNC_READ_TIME",  "<html>Time the prefetchers spent reading, in milliseconds.</html>");
			mtd.addColumn(CM_NAME, "AVG_READ_MS",           "<html>Average time per physical read, in milliseconds.<br><b>Formula</b>: POOL_READ_TIME / P_READS (Abs: since activation. Diff and Rate: in the sample)</html>");
			mtd.addColumn(CM_NAME, "DIRECT_READS",          "<html>Sectors read without the buffer pool (LOB and LONG data, backup...).</html>");
			mtd.addColumn(CM_NAME, "DIRECT_WRITES",         "<html>Sectors written without the buffer pool.</html>");
			mtd.addColumn(CM_NAME, "DIRECT_READ_TIME",      "<html>Time spent on direct reads, in milliseconds.</html>");
			mtd.addColumn(CM_NAME, "DIRECT_WRITE_TIME",     "<html>Time spent on direct writes, in milliseconds.</html>");
			mtd.addColumn(CM_NAME, "UNREAD_PREFETCH_PAGES", "<html>Pages the prefetchers read, that were never used (thrown out of the buffer pool before they were needed). High: too aggressive prefetch, or a too small buffer pool.</html>");
			mtd.addColumn(CM_NAME, "PREFETCH_WAITS",        "<html>Times an agent had to wait for a prefetcher to bring in a page.</html>");
			mtd.addColumn(CM_NAME, "PREFETCH_WAIT_TIME",    "<html>Time agents waited for prefetchers, in milliseconds.</html>");
			mtd.addColumn(CM_NAME, "FILES_CLOSED",          "<html>Database files that were closed (MAXFILOP reached). It should be 0.</html>");
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

		pkCols.add("TBSP_ID");
		pkCols.add("MEMBER");

		return pkCols;
	}

	@Override
	public String getSqlForVersion(DbxConnection conn, DbmsVersionInfo versionInfo)
	{
		String sql = ""
			+ "select /* ${cmCollectorName} */ \n"
			+ "     TBSP_NAME \n"
			+ "    ,TBSP_ID \n"
			+ "    ,MEMBER \n"
			+ "    ,TBSP_TYPE \n"
			+ "    ,TBSP_CONTENT_TYPE \n"
			+ "    ,(POOL_DATA_L_READS + POOL_INDEX_L_READS + POOL_XDA_L_READS + POOL_COL_L_READS + POOL_TEMP_DATA_L_READS + POOL_TEMP_INDEX_L_READS + POOL_TEMP_XDA_L_READS + POOL_TEMP_COL_L_READS) as L_READS \n"
			+ "    ,(POOL_DATA_P_READS + POOL_INDEX_P_READS + POOL_XDA_P_READS + POOL_COL_P_READS + POOL_TEMP_DATA_P_READS + POOL_TEMP_INDEX_P_READS + POOL_TEMP_XDA_P_READS + POOL_TEMP_COL_P_READS) as P_READS \n"
			+ "    ,CAST(NULL AS DECIMAL(5,1)) as HIT_PCT \n"
			+ "    ,POOL_DATA_L_READS \n"
			+ "    ,POOL_DATA_P_READS \n"
			+ "    ,POOL_INDEX_L_READS \n"
			+ "    ,POOL_INDEX_P_READS \n"
			+ "    ,(POOL_TEMP_DATA_L_READS + POOL_TEMP_INDEX_L_READS + POOL_TEMP_XDA_L_READS + POOL_TEMP_COL_L_READS) as POOL_TEMP_L_READS \n"
			+ "    ,(POOL_TEMP_DATA_P_READS + POOL_TEMP_INDEX_P_READS + POOL_TEMP_XDA_P_READS + POOL_TEMP_COL_P_READS) as POOL_TEMP_P_READS \n"
			+ "    ,(POOL_ASYNC_DATA_READS + POOL_ASYNC_INDEX_READS + POOL_ASYNC_XDA_READS + POOL_ASYNC_COL_READS) as POOL_ASYNC_READS \n"
			+ "    ,(POOL_DATA_WRITES + POOL_INDEX_WRITES + POOL_XDA_WRITES + POOL_COL_WRITES) as POOL_WRITES \n"
			+ "    ,(POOL_ASYNC_DATA_WRITES + POOL_ASYNC_INDEX_WRITES + POOL_ASYNC_XDA_WRITES + POOL_ASYNC_COL_WRITES) as POOL_ASYNC_WRITES \n"
			+ "    ,POOL_READ_TIME \n"
			+ "    ,POOL_WRITE_TIME \n"
			+ "    ,POOL_ASYNC_READ_TIME \n"
			+ "    ,CAST(NULL AS DECIMAL(12,3)) as AVG_READ_MS \n"
			+ "    ,DIRECT_READS \n"
			+ "    ,DIRECT_WRITES \n"
			+ "    ,DIRECT_READ_TIME \n"
			+ "    ,DIRECT_WRITE_TIME \n"
			+ "    ,UNREAD_PREFETCH_PAGES \n"
			+ "    ,PREFETCH_WAITS \n"
			+ "    ,PREFETCH_WAIT_TIME \n"
			+ "    ,FILES_CLOSED \n"
			+ "from table(MON_GET_TABLESPACE('', -2)) t \n"
			+ "order by TBSP_NAME, MEMBER \n"
			+ "";

		return sql;
	}

	/** HIT_PCT and AVG_READ_MS: since activation in the abs data, for the sample in the diff (and rate) data */
	@Override
	public void localCalculation(CounterSample newSample)
	{
		calcRatios(newSample);
	}

	@Override
	public void localCalculation(CounterSample prevSample, CounterSample newSample, CounterSample diffData)
	{
		calcRatios(diffData);
	}

	private void calcRatios(CounterSample data)
	{
		int pos_lReads   = data.findColumn("L_READS");
		int pos_pReads   = data.findColumn("P_READS");
		int pos_readTime = data.findColumn("POOL_READ_TIME");
		int pos_hitPct   = data.findColumn("HIT_PCT");
		int pos_avgRead  = data.findColumn("AVG_READ_MS");

		if (pos_lReads == -1 || pos_pReads == -1 || pos_readTime == -1 || pos_hitPct == -1 || pos_avgRead == -1)
			return;

		for (int rowId=0; rowId < data.getRowCount(); rowId++)
		{
			double lReads   = data.getValueAsDouble(rowId, pos_lReads  , 0d);
			double pReads   = data.getValueAsDouble(rowId, pos_pReads  , 0d);
			double readTime = data.getValueAsDouble(rowId, pos_readTime, 0d);

			data.setValueAt(lReads <= 0 ? null : new BigDecimal(Math.max(0, 100.0 - 100.0 * pReads / lReads)).setScale(1, RoundingMode.HALF_EVEN), rowId, pos_hitPct);
			data.setValueAt(pReads <= 0 ? null : new BigDecimal(readTime / pReads).setScale(3, RoundingMode.HALF_EVEN),                         rowId, pos_avgRead);
		}
	}

	@Override
	public void updateGraphData(TrendGraphDataPoint tgdp)
	{
		if (GRAPH_NAME_P_READS.equals(tgdp.getName()))
		{
			// One line per table space (the sum of the members)
			Map<String, Double> map = new LinkedHashMap<>();
			for (int r = 0; r < this.getRateRowCount(); r++)
			{
				Double val = this.getRateValueAsDouble(r, "P_READS");
				map.merge(this.getRateString(r, "TBSP_NAME"), val == null ? 0d : val, Double::sum);
			}
			tgdp.setDataPoint(this.getTimestamp(), map.keySet().toArray(new String[0]), map.values().toArray(new Double[0]));
		}

		if (GRAPH_NAME_AVG_READ_MS.equals(tgdp.getName()))
		{
			// One line per table space (time / reads, summed over the members)
			Map<String, double[]> map = new LinkedHashMap<>();
			for (int r = 0; r < this.getDiffRowCount(); r++)
			{
				Double reads = this.getDiffValueAsDouble(r, "P_READS");
				Double time  = this.getDiffValueAsDouble(r, "POOL_READ_TIME");
				double[] acc = map.computeIfAbsent(this.getDiffString(r, "TBSP_NAME"), k -> new double[2]);
				acc[0] += reads == null ? 0 : reads;
				acc[1] += time  == null ? 0 : time;
			}
			String[] lArray = new String[map.size()];
			Double[] dArray = new Double[map.size()];
			int i = 0;
			for (Map.Entry<String, double[]> e : map.entrySet())
			{
				lArray[i] = e.getKey();
				dArray[i] = e.getValue()[0] <= 0 ? 0d : e.getValue()[1] / e.getValue()[0];
				i++;
			}
			tgdp.setDataPoint(this.getTimestamp(), lArray, dArray);
		}
	}
}
