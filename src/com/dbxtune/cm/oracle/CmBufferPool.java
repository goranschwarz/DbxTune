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
import com.dbxtune.utils.MathUtils;

/**
 * @author Goran Schwarz (goran_schwarz@hotmail.com)
 */
public class CmBufferPool
extends CountersModel
{
	private static final Logger _logger = LogManager.getLogger(MethodHandles.lookup().lookupClass());
	private static final long    serialVersionUID = 1L;

	public static final String   CM_NAME          = CmBufferPool.class.getSimpleName();
	public static final String   SHORT_NAME       = "Buffer Cache";
	public static final String   HTML_DESC        =
		"<html>" +
		"The buffer cache (the data blocks Oracle keeps in memory), one row per buffer pool: how much it is read from memory and from disk, and if sessions have to wait for a buffer.<br>" +
		"<br>" +
		"<ul>" +
		"  <li><code>HIT_PCT</code> - how many of the block reads that was found in memory. In the 'diff' and 'rate' views it is for the sample, in 'abs' since the instance was started.<br>" +
		"      A low value is not a problem by itself (a full scan of a big table reads from disk), a sudden drop is worth a look.</li>" +
		"  <li><code>FREE_BUFFER_WAIT</code>    - a session had to wait for a free buffer: the database writer (DBWR) does not write dirty blocks fast enough, or the cache is too small.</li>" +
		"  <li><code>BUFFER_BUSY_WAIT</code>    - a session had to wait for a block that another session was reading or changing (hot blocks).</li>" +
		"  <li><code>WRITE_COMPLETE_WAIT</code> - a session had to wait for DBWR to finish writing a block.</li>" +
		"</ul>" +
		"Most databases have one pool (DEFAULT). KEEP and RECYCLE pools, and pools for other block sizes, are shown if they are configured.<br>" +
		"</html>";

	public static final String   GROUP_NAME       = MainFrame.TCP_GROUP_CACHE;
	public static final String   GUI_ICON_FILE    = "images/" + CM_NAME + ".png";

	public static final long     NEED_SRV_VERSION = 0;
	public static final long     NEED_CE_VERSION  = 0;

	public static final String[] MON_TABLES       = new String[] {CM_NAME, "V$BUFFER_POOL_STATISTICS"};
	public static final String[] NEED_ROLES       = new String[] {};
	public static final String[] NEED_CONFIG      = new String[] {};

	public static final String[] PCT_COLUMNS      = new String[] {};
	public static final String[] DIFF_COLUMNS     = new String[] {
		"DB_BLOCK_GETS", "CONSISTENT_GETS", "PHYSICAL_READS", "PHYSICAL_WRITES", "DB_BLOCK_CHANGE",
		"FREE_BUFFER_WAIT", "BUFFER_BUSY_WAIT", "WRITE_COMPLETE_WAIT", "FREE_BUFFER_INSPECTED", "DIRTY_BUFFERS_INSPECTED"
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

		return new CmBufferPool(counterController, guiController);
	}

	public CmBufferPool(ICounterController counterController, IGuiController guiController)
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
	public static final String GRAPH_NAME_HIT_PCT = "BufHitPct";
	public static final String GRAPH_NAME_WAITS   = "BufWaits";

	private void addTrendGraphs()
	{
		addTrendGraph(GRAPH_NAME_HIT_PCT,
			"Buffer Cache, Hit Percent", 	                // Menu CheckBox text
			"Buffer Cache Hit Percent in the sample, per Buffer Pool (" + SHORT_NAME + ")", // Graph Label
			TrendGraphDataPoint.createGraphProps(TrendGraphDataPoint.Y_AXIS_SCALE_LABELS_PERCENT, CentralPersistReader.SampleType.MIN_OVER_SAMPLES),
			null,
			LabelType.Dynamic,
			TrendGraphDataPoint.Category.CACHE,
			true,  // is Percent Graph
			false, // visible at start
			0,     // graph is valid from Server Version. 0 = All Versions; >0 = Valid from this version and above
			-1);   // minimum height

		addTrendGraph(GRAPH_NAME_WAITS,
			"Buffer Cache, Waits per Second", 	                // Menu CheckBox text
			"Buffer Cache Waits per Second: Free Buffer, Buffer Busy and Write Complete (" + SHORT_NAME + ")", // Graph Label
			TrendGraphDataPoint.createGraphProps(TrendGraphDataPoint.Y_AXIS_SCALE_LABELS_PERSEC, CentralPersistReader.SampleType.MAX_OVER_SAMPLES),
			new String[] {"Free Buffer Waits", "Buffer Busy Waits", "Write Complete Waits"},
			LabelType.Static,
			TrendGraphDataPoint.Category.CACHE,
			false, // is Percent Graph
			false, // visible at start
			0,     // graph is valid from Server Version. 0 = All Versions; >0 = Valid from this version and above
			-1);   // minimum height
	}

	@Override
	public List<String> getPkForVersion(DbxConnection conn, DbmsVersionInfo versionInfo)
	{
		List <String> pkCols = new LinkedList<String>();

		pkCols.add("ID");

		return pkCols;
	}

	/** If we can't read the views: disable the CM, and tell what to do about it */
	@Override
	public boolean checkDependsOnOther(DbxConnection conn)
	{
		return OracleCmUtils.checkViewsAreReadable(this, conn, "V$BUFFER_POOL_STATISTICS");
	}

	@Override
	public String getSqlForVersion(DbxConnection conn, DbmsVersionInfo versionInfo)
	{
		// One row for every buffer pool (ID is the key, NAME + BLOCK_SIZE is what we show). This looks the same in all versions
		// HIT_PCT is since the instance was started here: for DIFF and RATE it's calculated for the sample in localCalculation()
		// Note: CAST(... as NUMBER(#,1)) is needed to keep decimals (a NUMBER without precision is treated as a BIGINT)
		String sql = ""
			+ "select \n"
			+ "     b.ID \n"
			+ "    ,b.NAME \n"
			+ "    ,b.BLOCK_SIZE \n"
			+ "    ,b.SET_MSIZE                                                                 as BUFFERS \n"
			+ "    ,CAST(b.SET_MSIZE * b.BLOCK_SIZE / 1048576                 as NUMBER(18,1)) as SIZE_MB \n"
			+ "    ,CAST(100 * (1 - b.PHYSICAL_READS / NULLIF(b.DB_BLOCK_GETS + b.CONSISTENT_GETS, 0)) as NUMBER(5,1)) as HIT_PCT \n"
			+ "    ,b.DB_BLOCK_GETS \n"
			+ "    ,b.CONSISTENT_GETS \n"
			+ "    ,b.PHYSICAL_READS \n"
			+ "    ,b.PHYSICAL_WRITES \n"
			+ "    ,b.DB_BLOCK_CHANGE \n"
			+ "    ,b.FREE_BUFFER_WAIT \n"
			+ "    ,b.BUFFER_BUSY_WAIT \n"
			+ "    ,b.WRITE_COMPLETE_WAIT \n"
			+ "    ,b.FREE_BUFFER_INSPECTED \n"
			+ "    ,b.DIRTY_BUFFERS_INSPECTED \n"
			+ "from V$BUFFER_POOL_STATISTICS b \n"
			+ "order by b.ID \n"
			+ "";

		return sql;
	}

	@Override
	public void addMonTableDictForVersion(DbxConnection conn, DbmsVersionInfo versionInfo)
	{
		try
		{
			MonTablesDictionary mtd = MonTablesDictionaryManager.getInstance();

			mtd.addTable(CM_NAME, "The buffer cache, one row per buffer pool: reads from memory and from disk, and waits for buffers.");

			mtd.addColumn(CM_NAME, "ID",                      "<html>Buffer pool id.</html>");
			mtd.addColumn(CM_NAME, "NAME",                    "<html>Name of the buffer pool: DEFAULT, KEEP or RECYCLE.</html>");
			mtd.addColumn(CM_NAME, "BLOCK_SIZE",              "<html>Block size of the pool, in bytes.</html>");
			mtd.addColumn(CM_NAME, "BUFFERS",                 "<html>Number of buffers (blocks) in the pool (SET_MSIZE).</html>");
			mtd.addColumn(CM_NAME, "SIZE_MB",                 "<html>Size of the pool in MB.<br><b>Formula</b>: SET_MSIZE * BLOCK_SIZE / 1024 / 1024</html>");
			mtd.addColumn(CM_NAME, "HIT_PCT",                 "<html>How many of the block reads that was found in memory, in percent. In 'abs': since the instance was started. In 'diff' and 'rate': in the sample.<br><b>Formula</b>: 100 * (1 - PHYSICAL_READS / (DB_BLOCK_GETS + CONSISTENT_GETS))</html>");
			mtd.addColumn(CM_NAME, "DB_BLOCK_GETS",           "<html>Blocks read in 'current mode' (the newest version of the block, done by DML).</html>");
			mtd.addColumn(CM_NAME, "CONSISTENT_GETS",         "<html>Blocks read in 'consistent mode' (as the block looked when the query started, done by queries).</html>");
			mtd.addColumn(CM_NAME, "PHYSICAL_READS",          "<html>Blocks read from disk into the pool.</html>");
			mtd.addColumn(CM_NAME, "PHYSICAL_WRITES",         "<html>Blocks written to disk from the pool.</html>");
			mtd.addColumn(CM_NAME, "DB_BLOCK_CHANGE",         "<html>Changes made to blocks in the pool.</html>");
			mtd.addColumn(CM_NAME, "FREE_BUFFER_WAIT",        "<html>Times a session had to wait for a free buffer (DBWR does not write fast enough, or the cache is too small).</html>");
			mtd.addColumn(CM_NAME, "BUFFER_BUSY_WAIT",        "<html>Times a session had to wait for a block that another session was reading or changing.</html>");
			mtd.addColumn(CM_NAME, "WRITE_COMPLETE_WAIT",     "<html>Times a session had to wait for DBWR to finish writing a block.</html>");
			mtd.addColumn(CM_NAME, "FREE_BUFFER_INSPECTED",   "<html>Buffers a session looked at (and skipped) while searching for a free buffer.</html>");
			mtd.addColumn(CM_NAME, "DIRTY_BUFFERS_INSPECTED", "<html>Dirty buffers a session found while searching for a free buffer. If this grows fast: DBWR is behind.</html>");
		}
		catch (NameNotFoundException e)
		{
			_logger.warn("Problems in cm='" + CM_NAME + "', adding addMonTableDictForVersion. Caught: " + e);
		}
	}

	/**
	 * HIT_PCT: The SQL gives the value since the instance was started.<br>
	 * For DIFF (and RATE, which gets it from DIFF) we want it for the reads that happened in this sample
	 */
	@Override
	public void localCalculation(CounterSample prevSample, CounterSample newSample, CounterSample diffData)
	{
		int pos_DB_BLOCK_GETS   = diffData.findColumn("DB_BLOCK_GETS");
		int pos_CONSISTENT_GETS = diffData.findColumn("CONSISTENT_GETS");
		int pos_PHYSICAL_READS  = diffData.findColumn("PHYSICAL_READS");
		int pos_HIT_PCT         = diffData.findColumn("HIT_PCT");

		if (pos_DB_BLOCK_GETS == -1 || pos_CONSISTENT_GETS == -1 || pos_PHYSICAL_READS == -1 || pos_HIT_PCT == -1)
		{
			_logger.warn(getName() + ": localCalculation(), Problems finding one of the columns: DB_BLOCK_GETS, CONSISTENT_GETS, PHYSICAL_READS, HIT_PCT.");
			return;
		}

		for (int rowId=0; rowId < diffData.getRowCount(); rowId++)
		{
			long gets  = diffData.getValueAsLong(rowId, pos_DB_BLOCK_GETS) + diffData.getValueAsLong(rowId, pos_CONSISTENT_GETS);
			long reads = diffData.getValueAsLong(rowId, pos_PHYSICAL_READS);

			// No reads at all in the sample: 100 (nothing was read from disk). More physical reads than gets (direct reads): 0
			BigDecimal hitPct = (gets <= 0) ? new BigDecimal(100) : MathUtils.roundToBigDecimal(Math.max(0, 100.0 * (1.0 - (reads * 1.0 / gets))), 1);

			diffData.setValueAt(hitPct, rowId, pos_HIT_PCT);
		}
	}

	@Override
	public void updateGraphData(TrendGraphDataPoint tgdp)
	{
		if (this.size() == 0 || ! this.hasDiffData())
			return;

		//--------------------------------------------------------
		// One "line" for every buffer pool: hit percent in the sample (see localCalculation)
		//--------------------------------------------------------
		if (GRAPH_NAME_HIT_PCT.equals(tgdp.getName()))
		{
			LinkedHashMap<String, Double> dataMap = new LinkedHashMap<String, Double>();

			for (int r = 0; r < this.size(); r++)
			{
				String label  = this.getAbsString(r, "NAME") + " " + (this.getAbsValueAsInteger(r, "BLOCK_SIZE", 0) / 1024) + "K";
				Double hitPct = this.getDiffValueAsDouble(r, "HIT_PCT");

				dataMap.put(label, (hitPct == null) ? 0d : hitPct);
			}

			if (dataMap.size() > 0)
				tgdp.setData(this.getTimestamp(), dataMap);
		}

		//--------------------------------------------------------
		// Waits per second, for all pools together
		//--------------------------------------------------------
		if (GRAPH_NAME_WAITS.equals(tgdp.getName()))
		{
			String[] colNames = new String[] {"FREE_BUFFER_WAIT", "BUFFER_BUSY_WAIT", "WRITE_COMPLETE_WAIT"};
			Double[] arr      = new Double[colNames.length];
			for (int i = 0; i < arr.length; i++)
			{
				double sum = 0;
				for (int r = 0; r < this.size(); r++)
				{
					Double val = this.getRateValueAsDouble(r, colNames[i]);
					if (val != null && val > 0)
						sum += val;
				}
				arr[i] = sum;
			}

			// Set the values
			tgdp.setDataPoint(this.getTimestamp(), arr);
		}
	}
}
