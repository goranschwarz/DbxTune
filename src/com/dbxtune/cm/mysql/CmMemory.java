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
import java.util.ArrayList;
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
public class CmMemory
extends CountersModel
{
	private static final Logger _logger = LogManager.getLogger(MethodHandles.lookup().lookupClass());
	private static final long    serialVersionUID = 1L;

	public static final String   CM_NAME          = CmMemory.class.getSimpleName();
	public static final String   SHORT_NAME       = "Memory";
	public static final String   HTML_DESC        =
		"<html>" +
		"What the server uses its memory for. One row per memory instrument, for the whole server. " +
		"(performance_schema.memory_summary_global_by_event_name)<br>" +
		"<br>" +
		"<code>CurrentMb</code> is the memory used right now (it is the same in Abs, Diff and Rate). The Diff of <code>AllocMb</code> and <code>FreeMb</code> is what was allocated and freed in the sample.<br>" +
		"The largest part is normally the InnoDB buffer pool (<code>memory/innodb/buf_buf_pool</code>) and performance_schema itself.<br>" +
		"<br>" +
		"<b>Note</b>: Memory that the server allocated before performance_schema was started, and memory that is not instrumented, is not included. So the total is less than what the operating system says the process uses.<br>" +
		"</html>";

	public static final String   GROUP_NAME       = MainFrame.TCP_GROUP_CACHE;
	public static final String   GUI_ICON_FILE    = "images/" + CM_NAME + ".png";

	public static final long     NEED_SRV_VERSION = 0;
	public static final long     NEED_CE_VERSION  = 0;

	public static final String[] MON_TABLES       = new String[] {CM_NAME, "memory_summary_global_by_event_name"};
	public static final String[] NEED_ROLES       = new String[] {};
	public static final String[] NEED_CONFIG      = new String[] {};

	public static final String[] PCT_COLUMNS      = new String[] {};
	public static final String[] DIFF_COLUMNS     = new String[] {
		"AllocCount",
		"FreeCount",
		"AllocMb",
		"FreeMb"
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

		return new CmMemory(counterController, guiController);
	}

	public CmMemory(ICounterController counterController, IGuiController guiController)
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
	public static final String GRAPH_NAME_AREA_MB       = "MemoryAreaMb";
	public static final String GRAPH_NAME_TOP_EVENTS_MB = "MemoryTopEventsMb";

	private void addTrendGraphs()
	{
		addTrendGraph(GRAPH_NAME_AREA_MB,
			"Memory Used per Area, in MB", 	                // Menu CheckBox text
			"Memory Used per Area (innodb, sql, performance_schema...), in MB (" + SHORT_NAME + ")", // Graph Label
			TrendGraphDataPoint.createGraphProps(TrendGraphDataPoint.Y_AXIS_SCALE_LABELS_MB, CentralPersistReader.SampleType.MAX_OVER_SAMPLES),
			null,
			LabelType.Dynamic,
			TrendGraphDataPoint.Category.MEMORY,
			false, // is Percent Graph
			true,  // visible at start
			0,     // graph is valid from Server Version. 0 = All Versions; >0 = Valid from this version and above
			-1);   // minimum height

		addTrendGraph(GRAPH_NAME_TOP_EVENTS_MB,
			"Memory Used, Top 15 Instruments, in MB", 	                // Menu CheckBox text
			"Memory Used by the Top 15 Memory Instruments, in MB (" + SHORT_NAME + ")", // Graph Label
			TrendGraphDataPoint.createGraphProps(TrendGraphDataPoint.Y_AXIS_SCALE_LABELS_MB, CentralPersistReader.SampleType.MAX_OVER_SAMPLES),
			null,
			LabelType.Dynamic,
			TrendGraphDataPoint.Category.MEMORY,
			false, // is Percent Graph
			false, // visible at start
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

	/** If we can't read the view: disable the CM, and tell what to do about it */
	@Override
	public boolean checkDependsOnOther(DbxConnection conn)
	{
		return MySqlCmUtils.checkViewsAreReadable(this, conn, "performance_schema.memory_summary_global_by_event_name");
	}

	@Override
	public String getSqlForVersion(DbxConnection conn, DbmsVersionInfo versionInfo)
	{
		// Only the instruments that has allocated something
		// Area: the second part of the name, memory/<area>/...
		String sql = ""
			+ "select /* ${cmCollectorName} */ \n"
			+ "     EVENT_NAME                                                              as EventName \n"
			+ "    ,SUBSTRING_INDEX(SUBSTRING_INDEX(EVENT_NAME, '/', 2), '/', -1)           as Area \n"
			+ "    ,CAST(CURRENT_NUMBER_OF_BYTES_USED / 1048576.0 as DECIMAL(15,1))         as CurrentMb \n"
			+ "    ,CAST(HIGH_NUMBER_OF_BYTES_USED    / 1048576.0 as DECIMAL(15,1))         as HighMb \n"
			+ "    ,CURRENT_COUNT_USED                                                      as CurrentCount \n"
			+ "    ,COUNT_ALLOC                                                             as AllocCount \n"
			+ "    ,COUNT_FREE                                                              as FreeCount \n"
			+ "    ,CAST(SUM_NUMBER_OF_BYTES_ALLOC    / 1048576.0 as DECIMAL(18,1))         as AllocMb \n"
			+ "    ,CAST(SUM_NUMBER_OF_BYTES_FREE     / 1048576.0 as DECIMAL(18,1))         as FreeMb \n"
			+ "from performance_schema.memory_summary_global_by_event_name \n"
			+ "where COUNT_ALLOC > 0 \n"
			+ "order by EVENT_NAME \n"
			+ "";

		return sql;
	}

	@Override
	public void addMonTableDictForVersion(DbxConnection conn, DbmsVersionInfo versionInfo)
	{
		try
		{
			MonTablesDictionary mtd = MonTablesDictionaryManager.getInstance();

			mtd.addTable(CM_NAME, "What the server uses its memory for. One row per memory instrument, for the whole server.");

			mtd.addColumn(CM_NAME, "EventName",    "<html>The memory instrument, for example <code>memory/innodb/buf_buf_pool</code> (the InnoDB buffer pool).</html>");
			mtd.addColumn(CM_NAME, "Area",         "<html>The second part of the name: innodb, sql, performance_schema, temptable (internal temporary tables), mysys...</html>");
			mtd.addColumn(CM_NAME, "CurrentMb",    "<html>Memory used right now, in MB. (the same value in Abs, Diff and Rate)</html>");
			mtd.addColumn(CM_NAME, "HighMb",       "<html>The most memory it has used, in MB (since the server was started).</html>");
			mtd.addColumn(CM_NAME, "CurrentCount", "<html>Number of memory blocks that are allocated right now.</html>");
			mtd.addColumn(CM_NAME, "AllocCount",   "<html>Number of allocations.</html>");
			mtd.addColumn(CM_NAME, "FreeCount",    "<html>Number of frees.</html>");
			mtd.addColumn(CM_NAME, "AllocMb",      "<html>Memory allocated, in MB.</html>");
			mtd.addColumn(CM_NAME, "FreeMb",       "<html>Memory freed, in MB.</html>");
		}
		catch (NameNotFoundException e)
		{
			_logger.warn("Problems in cm='" + CM_NAME + "', adding addMonTableDictForVersion. Caught: " + e);
		}
	}

	@Override
	public void updateGraphData(TrendGraphDataPoint tgdp)
	{
		//--------------------------------------------------------
		// Memory used per area (sorted by name, so the lines keep their order)
		//--------------------------------------------------------
		if (GRAPH_NAME_AREA_MB.equals(tgdp.getName()))
		{
			Map<String, Double> dataMap = new TreeMap<>();

			for (int r = 0; r < this.size(); r++)
			{
				String area      = this.getAbsString       (r, "Area");
				Double currentMb = this.getAbsValueAsDouble(r, "CurrentMb");

				dataMap.merge(area, currentMb == null ? 0d : currentMb, Double::sum);
			}

			// Skip the areas that use less than 1 MB (there are many, and they would only be lines at 0)
			dataMap.values().removeIf(mb -> mb < 1.0);

			if (dataMap.size() > 0)
				tgdp.setData(this.getTimestamp(), dataMap);
		}

		//--------------------------------------------------------
		// The 15 instruments that use most memory right now
		//--------------------------------------------------------
		if (GRAPH_NAME_TOP_EVENTS_MB.equals(tgdp.getName()))
		{
			List<Integer> rowIds = new ArrayList<>();
			for (int r = 0; r < this.size(); r++)
			{
				Double currentMb = this.getAbsValueAsDouble(r, "CurrentMb");
				if (currentMb != null && currentMb > 0)
					rowIds.add(r);
			}
			rowIds.sort((r1, r2) -> this.getAbsValueAsDouble(r2, "CurrentMb").compareTo(this.getAbsValueAsDouble(r1, "CurrentMb")));

			int top = Math.min(15, rowIds.size());
			if (top > 0)
			{
				Double[] dArray = new Double[top];
				String[] lArray = new String[top];
				for (int i = 0; i < top; i++)
				{
					int r = rowIds.get(i);

					lArray[i] = this.getAbsString(r, "EventName");
					dArray[i] = this.getAbsValueAsDouble(r, "CurrentMb");
				}

				// Set the values
				tgdp.setDataPoint(this.getTimestamp(), lArray, dArray);
			}
		}
	}

	@Override
	public List<CmChartDescriptor> createChartDescriptors()
	{
		List<CmChartDescriptor> list = new ArrayList<>();

		list.add(new CmChartDescriptor()
			.id("memory-by-area")
			.title("Memory Used by Area")
			.chartType(CmChartDescriptor.CHART_TYPE_DUAL_PIE_BAR)
			.labelColumn("EventName")
			.groupByColumn("Area")
			.valueColumns("CurrentMb", "CurrentCount")
			.seriesLabels("Memory Used (MB)", "Memory Blocks")
			.pieOtherLimit(0.05)
			.splitRatio(0.6));

		return list;
	}
}
