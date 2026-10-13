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
import java.util.Arrays;
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
public class CmLatch
extends CountersModel
{
	private static final Logger _logger = LogManager.getLogger(MethodHandles.lookup().lookupClass());
	private static final long    serialVersionUID = 1L;

	public static final String   CM_NAME          = CmLatch.class.getSimpleName();
	public static final String   SHORT_NAME       = "Latches and Mutexes";
	public static final String   HTML_DESC        =
		"<html>" +
		"Latches and mutexes are the short locks that Oracle takes on it's own memory structures (the buffer cache, the shared pool, the redo buffer...).<br>" +
		"They are held for microseconds, so normally nobody waits. When many sessions want the same one at the same time they start to spin (CPU) and then to sleep: that is what this tab shows.<br>" +
		"<br>" +
		"Only the latches that someone has had to wait for are listed (<code>MISSES</code> or <code>SLEEPS</code> above 0), and all the mutex locations in <code>V$MUTEX_SLEEP</code>.<br>" +
		"<br>" +
		"<b>Tip</b>: Look at the DIFF or RATE values, sorted on <code>SLEEPS</code> or <code>WAIT_TIME_MICRO</code>. Some usual ones:" +
		"<ul>" +
		"  <li><b>cache buffers chains</b> - many sessions read the same blocks: a hot table or index, often a statement with a bad plan that is run all the time.</li>" +
		"  <li><b>shared pool</b>, <b>library cache</b> mutexes, <b>Cursor Pin</b> - hard parsing: statements without bind variables, or very many child cursors.</li>" +
		"  <li><b>redo allocation</b>, <b>redo copy</b> - very many small commits.</li>" +
		"</ul>" +
		"<b>Note</b>: The tab 'System Events' shows the same thing from the other side: how long the sessions waited on <code>latch: ...</code> and <code>cursor: ...</code> events.<br>" +
		"</html>";

	public static final String   GROUP_NAME       = MainFrame.TCP_GROUP_SERVER;
	public static final String   GUI_ICON_FILE    = "images/" + CM_NAME + ".png";

	public static final long     NEED_SRV_VERSION = 0;
	public static final long     NEED_CE_VERSION  = 0;

	public static final String[] MON_TABLES       = new String[] {CM_NAME, "V$LATCH", "V$MUTEX_SLEEP"};
	public static final String[] NEED_ROLES       = new String[] {};
	public static final String[] NEED_CONFIG      = new String[] {};

	public static final String[] PCT_COLUMNS      = new String[] {};
	public static final String[] DIFF_COLUMNS     = new String[] {
		"GETS",
		"MISSES",
		"SLEEPS",
		"SPIN_GETS",
		"IMMEDIATE_GETS",
		"IMMEDIATE_MISSES",
		"WAIT_TIME_MICRO"
		};

	public static final boolean  NEGATIVE_DIFF_COUNTERS_TO_ZERO = true;
	public static final boolean  IS_SYSTEM_CM                   = true;
	public static final int      DEFAULT_POSTPONE_TIME          = 0;
	public static final int      DEFAULT_QUERY_TIMEOUT          = 10;

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

		return new CmLatch(counterController, guiController);
	}

	public CmLatch(ICounterController counterController, IGuiController guiController)
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
	public static final String GRAPH_NAME_SLEEPS  = "LatchSleeps";
	public static final String GRAPH_NAME_WAIT_MS = "LatchWaitMs";

	private void addTrendGraphs()
	{
		addTrendGraph(GRAPH_NAME_SLEEPS,
			"Latch and Mutex Sleeps per Second, Top 10", 	                // Menu CheckBox text
			"Latch and Mutex Sleeps per Second, Top 10 (" + SHORT_NAME + ")", // Graph Label
			TrendGraphDataPoint.createGraphProps(TrendGraphDataPoint.Y_AXIS_SCALE_LABELS_PERSEC, CentralPersistReader.SampleType.MAX_OVER_SAMPLES),
			null,
			LabelType.Dynamic,
			TrendGraphDataPoint.Category.WAITS,
			false, // is Percent Graph
			false, // visible at start
			0,     // graph is valid from Server Version. 0 = All Versions; >0 = Valid from this version and above
			-1);   // minimum height

		addTrendGraph(GRAPH_NAME_WAIT_MS,
			"Latch and Mutex Wait Time in ms per Second, Top 10", 	                // Menu CheckBox text
			"Latch and Mutex Wait Time in ms per Second, Top 10 (" + SHORT_NAME + ")", // Graph Label
			TrendGraphDataPoint.createGraphProps(TrendGraphDataPoint.Y_AXIS_SCALE_LABELS_MILLISEC, CentralPersistReader.SampleType.MAX_OVER_SAMPLES),
			null,
			LabelType.Dynamic,
			TrendGraphDataPoint.Category.WAITS,
			false, // is Percent Graph
			false, // visible at start
			0,     // graph is valid from Server Version. 0 = All Versions; >0 = Valid from this version and above
			-1);   // minimum height
	}

	@Override
	public List<String> getPkForVersion(DbxConnection conn, DbmsVersionInfo versionInfo)
	{
		List <String> pkCols = new LinkedList<String>();

		pkCols.add("KIND");
		pkCols.add("NAME");

		return pkCols;
	}

	/** If we can't read the views: disable the CM, and tell what to do about it */
	@Override
	public boolean checkDependsOnOther(DbxConnection conn)
	{
		return OracleCmUtils.checkViewsAreReadable(this, conn, "V$LATCH", "V$MUTEX_SLEEP");
	}

	@Override
	public String getSqlForVersion(DbxConnection conn, DbmsVersionInfo versionInfo)
	{
		// One row per latch NAME: in a Container Database V$LATCH has the same latch once per container, they are added up (it is one instance)
		// Only latches that someone has had to wait for: that is 20-100 of the 500-1800 latches
		// Note: CAST(... as NUMBER(8,3)) is needed to keep decimals (a NUMBER without precision is treated as a BIGINT)
		String sql = ""
			+ "select \n"
			+ "     CAST('LATCH' as VARCHAR2(5))                                              as KIND \n"
			+ "    ,CAST(l.NAME  as VARCHAR2(128))                                            as NAME \n"
			+ "    ,sum(l.GETS)                                                               as GETS \n"
			+ "    ,sum(l.MISSES)                                                             as MISSES \n"
			+ "    ,CAST(sum(l.MISSES) * 100 / NULLIF(sum(l.GETS), 0) as NUMBER(8,3))         as MISS_PCT \n"
			+ "    ,sum(l.SLEEPS)                                                             as SLEEPS \n"
			+ "    ,sum(l.WAIT_TIME)                                                          as WAIT_TIME_MICRO \n"
			+ "    ,sum(l.SPIN_GETS)                                                          as SPIN_GETS \n"
			+ "    ,sum(l.IMMEDIATE_GETS)                                                     as IMMEDIATE_GETS \n"
			+ "    ,sum(l.IMMEDIATE_MISSES)                                                   as IMMEDIATE_MISSES \n"
			+ "from V$LATCH l \n"
			+ "group by l.NAME \n"
			+ "having sum(l.MISSES) > 0 or sum(l.IMMEDIATE_MISSES) > 0 or sum(l.SLEEPS) > 0 \n"
			+ "union all \n"
			+ "select \n"
			+ "     CAST('MUTEX' as VARCHAR2(5)) \n"
			+ "    ,CAST(m.MUTEX_TYPE || ': ' || m.LOCATION as VARCHAR2(128)) \n"
			+ "    ,0 \n"
			+ "    ,0 \n"
			+ "    ,CAST(NULL as NUMBER(8,3)) \n"
			+ "    ,sum(m.SLEEPS) \n"
			+ "    ,sum(m.WAIT_TIME) \n"
			+ "    ,0 \n"
			+ "    ,0 \n"
			+ "    ,0 \n"
			+ "from V$MUTEX_SLEEP m \n"
			+ "group by m.MUTEX_TYPE, m.LOCATION \n"
			+ "order by 1, 2 \n"
			+ "";

		return sql;
	}

	@Override
	public void addMonTableDictForVersion(DbxConnection conn, DbmsVersionInfo versionInfo)
	{
		try
		{
			MonTablesDictionary mtd = MonTablesDictionaryManager.getInstance();

			mtd.addTable(CM_NAME, "Latches and mutexes that the sessions have had to wait for: the short locks that Oracle takes on it's own memory structures.");

			mtd.addColumn(CM_NAME, "KIND",             "<html><b>LATCH</b> = a row from V$LATCH. <b>MUTEX</b> = a row from V$MUTEX_SLEEP.</html>");
			mtd.addColumn(CM_NAME, "NAME",             "<html>LATCH: the name of the latch.<br>MUTEX: the type of mutex (Library Cache, Cursor Pin, Cursor Parent, Row Cache...) and the place in Oracle's code where the sleep was.</html>");
			mtd.addColumn(CM_NAME, "GETS",             "<html>Number of times the latch was requested in <i>willing to wait</i> mode. 0 for a MUTEX (Oracle does not count the gets of a mutex).</html>");
			mtd.addColumn(CM_NAME, "MISSES",           "<html>Number of times the latch was requested in <i>willing to wait</i> mode, and it was not free at the first try. The session then spins (uses CPU) and tries again.</html>");
			mtd.addColumn(CM_NAME, "MISS_PCT",         "<html>How often the latch was not free at the first try, in percent.<br>ABS: since the instance was started. DIFF and RATE: in the last sample.<br><b>Formula</b>: MISSES * 100 / GETS</html>");
			mtd.addColumn(CM_NAME, "SLEEPS",           "<html>Number of times a session gave up spinning and went to sleep, waiting for the latch or the mutex. This is where it starts to hurt.</html>");
			mtd.addColumn(CM_NAME, "WAIT_TIME_MICRO",  "<html>Time spent waiting for the latch or the mutex, in microseconds.</html>");
			mtd.addColumn(CM_NAME, "SPIN_GETS",        "<html>Number of times the latch was not free at the first try, but the session got it while spinning (without sleeping).</html>");
			mtd.addColumn(CM_NAME, "IMMEDIATE_GETS",   "<html>Number of times the latch was requested in <i>no wait</i> mode (the session does something else if it is not free).</html>");
			mtd.addColumn(CM_NAME, "IMMEDIATE_MISSES", "<html>Number of times the latch was requested in <i>no wait</i> mode, and it was not free.</html>");
		}
		catch (NameNotFoundException e)
		{
			_logger.warn("Problems in cm='" + CM_NAME + "', adding addMonTableDictForVersion. Caught: " + e);
		}
	}

	/**
	 * MISS_PCT: The SQL gives the percent since the instance was started.<br>
	 * For DIFF (and RATE, which gets it from DIFF) we want the percent for the gets that happened in this sample
	 */
	@Override
	public void localCalculation(CounterSample prevSample, CounterSample newSample, CounterSample diffData)
	{
		int pos_GETS     = diffData.findColumn("GETS");
		int pos_MISSES   = diffData.findColumn("MISSES");
		int pos_MISS_PCT = diffData.findColumn("MISS_PCT");

		if (pos_GETS == -1 || pos_MISSES == -1 || pos_MISS_PCT == -1)
		{
			_logger.warn(getName() + ": localCalculation(), Problems finding one of the columns: GETS, MISSES, MISS_PCT.");
			return;
		}

		for (int rowId=0; rowId < diffData.getRowCount(); rowId++)
		{
			long gets   = diffData.getValueAsLong(rowId, pos_GETS);
			long misses = diffData.getValueAsLong(rowId, pos_MISSES);

			BigDecimal missPct = (gets <= 0) ? new BigDecimal(0) : MathUtils.roundToBigDecimal(misses * 100.0 / gets, 3);

			diffData.setValueAt(missPct, rowId, pos_MISS_PCT);
		}
	}

	@Override
	public void updateGraphData(TrendGraphDataPoint tgdp)
	{
		// The two graphs are the same: one "line" for each of the 10 latches or mutexes with the highest value in this sample
		//   LatchSleeps: sleeps per second
		//   LatchWaitMs: wait time in milliseconds per second
		String colName = null;
		double divider = 1.0;
		if (GRAPH_NAME_SLEEPS .equals(tgdp.getName())) { colName = "SLEEPS"; }
		if (GRAPH_NAME_WAIT_MS.equals(tgdp.getName())) { colName = "WAIT_TIME_MICRO"; divider = 1000.0; }

		if (colName != null)
		{
			String col = colName;

			// Get rows that has a value, then sort them: highest first
			// A row that we see for the first time is skipped: it's "delta" is everything since the instance was started
			List<Integer> rowIds = new ArrayList<>();
			for (int r = 0; r < this.size(); r++)
			{
				Double perSec = this.getRateValueAsDouble(r, col);
				if (perSec != null && perSec > 0 && ! this.isNewDeltaOrRateRow(r))
					rowIds.add(r);
			}
			rowIds.sort((r1, r2) -> this.getRateValueAsDouble(r2, col).compareTo(this.getRateValueAsDouble(r1, col)));

			// KIND + NAME is the Primary Key, so the labels are unique
			LinkedHashMap<String, Double> dataMap = new LinkedHashMap<>();
			for (int i = 0; i < Math.min(10, rowIds.size()); i++)
			{
				int r = rowIds.get(i);
				dataMap.put(this.getAbsString(r, "KIND") + ": " + this.getAbsString(r, "NAME"), this.getRateValueAsDouble(r, col) / divider);
			}

			if (dataMap.size() > 0)
			{
				tgdp.setData(this.getTimestamp(), dataMap);
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
