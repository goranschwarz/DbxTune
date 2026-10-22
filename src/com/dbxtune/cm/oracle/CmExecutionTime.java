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

import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.ArrayList;
import java.util.List;

import com.dbxtune.ICounterController;
import com.dbxtune.IGuiController;
import com.dbxtune.central.pcs.CentralPersistReader;
import com.dbxtune.cm.CmChartDescriptor;
import com.dbxtune.cm.CounterSetTemplates;
import com.dbxtune.cm.CounterSetTemplates.Type;
import com.dbxtune.cm.CountersModel;
import com.dbxtune.graph.TrendGraphDataPoint;
import com.dbxtune.graph.TrendGraphDataPoint.LabelType;
import com.dbxtune.gui.MainFrame;
import com.dbxtune.sql.conn.DbxConnection;
import com.dbxtune.sql.conn.info.DbmsVersionInfo;

/**
 * @author Goran Schwarz (goran_schwarz@hotmail.com)
 */
public class CmExecutionTime
extends CountersModel
{
	private static final long    serialVersionUID = 1L;

	public static final String   CM_NAME          = CmExecutionTime.class.getSimpleName();
	public static final String   SHORT_NAME       = "Time Model";
	public static final String   HTML_DESC        =
		"<html>" +
		"Where is the time spent? The time (in microseconds) the instance has spent in different kind of operations.<br>" +
		"<ul>" +
		"   <li><b>DB time</b>             - Time spent in the database by all user sessions (using CPU or waiting for something, but not idle).</li>" +
		"   <li><b>DB CPU</b>              - The part of 'DB time' that was spent using CPU. The rest of 'DB time' is waiting, see tab 'System Events' for what.</li>" +
		"   <li><b>background ...</b>      - The same for the background processes (DBWR, LGWR...)</li>" +
		"   <li><b>parse time elapsed</b>  - Parsing SQL. If 'hard parse elapsed time' is a big part of 'DB time', the application probably does not use bind variables.</li>" +
		"   <li><b>sql execute elapsed time</b> - Hopefully this is where most of the time is spent.</li>" +
		"</ul>" +
		"<b>Tip</b>: The RATE value divided by 1 000 000 is <i>seconds per second</i>. For 'DB time' that is the same as <i>Average Active Sessions</i>.<br>" +
		"</html>";

	public static final String   GROUP_NAME       = MainFrame.TCP_GROUP_SERVER;
	public static final String   GUI_ICON_FILE    = "images/" + CM_NAME + ".png";

	public static final long     NEED_SRV_VERSION = 0;
	public static final long     NEED_CE_VERSION  = 0;

	public static final String[] MON_TABLES       = new String[] {"V$SYS_TIME_MODEL"};
	public static final String[] NEED_ROLES       = new String[] {};
	public static final String[] NEED_CONFIG      = new String[] {};

	public static final String[] PCT_COLUMNS      = new String[] {};
	public static final String[] DIFF_COLUMNS     = new String[] {"VALUE"};

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

		return new CmExecutionTime(counterController, guiController);
	}

	public CmExecutionTime(ICounterController counterController, IGuiController guiController)
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
	public static final String GRAPH_NAME_TOTALS    = "TimeModelAas";
	public static final String GRAPH_NAME_BREAKDOWN = "TimeModelBreakdown";

	// The "totals" (STAT_NAME in V$SYS_TIME_MODEL): used both as the series label, and to find the row
	private static final String[] STATS_TOTALS = new String[] {"DB time", "DB CPU", "background elapsed time", "background cpu time"};

	private void addTrendGraphs()
	{
		addTrendGraph(GRAPH_NAME_TOTALS,
			"DB Time and CPU, in Seconds per Second", 	                // Menu CheckBox text
			"DB Time and CPU, in Seconds per Second [DB time = Average Active Sessions] (" + SHORT_NAME + ")", // Graph Label
			TrendGraphDataPoint.createGraphProps(TrendGraphDataPoint.Y_AXIS_SCALE_LABELS_NORMAL, CentralPersistReader.SampleType.MAX_OVER_SAMPLES),
			STATS_TOTALS,
			LabelType.Static,
			TrendGraphDataPoint.Category.CPU,
			false, // is Percent Graph
			false, // visible at start
			0,     // graph is valid from Server Version. 0 = All Versions; >0 = Valid from this version and above
			-1);   // minimum height

		addTrendGraph(GRAPH_NAME_BREAKDOWN,
			"Time per Operation Type, in Seconds per Second", 	                // Menu CheckBox text
			"Time per Operation Type (parse, execute, PL/SQL...), in Seconds per Second (" + SHORT_NAME + ")", // Graph Label
			TrendGraphDataPoint.createGraphProps(TrendGraphDataPoint.Y_AXIS_SCALE_LABELS_NORMAL, CentralPersistReader.SampleType.MAX_OVER_SAMPLES),
			null,
			LabelType.Dynamic,
			TrendGraphDataPoint.Category.CPU,
			false, // is Percent Graph
			false, // visible at start
			0,     // graph is valid from Server Version. 0 = All Versions; >0 = Valid from this version and above
			-1);   // minimum height
	}

	@Override
	public List<String> getPkForVersion(DbxConnection conn, DbmsVersionInfo versionInfo)
	{
		List <String> pkCols = new LinkedList<String>();

		// The STAT_NAME is unique, and it is what we use to find the rows for the graphs
		pkCols.add("STAT_NAME");

		return pkCols;
	}

	/** If we can't read the views: disable the CM, and tell what to do about it */
	@Override
	public boolean checkDependsOnOther(DbxConnection conn)
	{
		return OracleCmUtils.checkViewsAreReadable(this, conn, "V$SYS_TIME_MODEL");
	}

	@Override
	public String getSqlForVersion(DbxConnection conn, DbmsVersionInfo versionInfo)
	{
		String sql = ""
			+ "select \n"
			+ "     t.STAT_NAME \n"
			+ "    ,t.VALUE \n"
			+ "    ,t.STAT_ID \n"
			+ "from V$SYS_TIME_MODEL t \n"
			+ "order by t.STAT_NAME \n"
			+ "";

		return sql;
	}

	@Override
	public void updateGraphData(TrendGraphDataPoint tgdp)
	{
		//--------------------------------------------------------
		// The totals: microseconds per second / 1 000 000 = seconds per second
		//--------------------------------------------------------
		if (GRAPH_NAME_TOTALS.equals(tgdp.getName()))
		{
			Double[] arr = new Double[STATS_TOTALS.length];
			for (int i = 0; i < STATS_TOTALS.length; i++)
			{
				Double microsPerSec = this.getRateValueAsDouble(createPkStr(STATS_TOTALS[i]), "VALUE");

				arr[i] = (microsPerSec == null) ? 0.0 : microsPerSec / 1_000_000.0;
			}

			// Set the values
			tgdp.setDataPoint(this.getTimestamp(), arr);
		}

		//--------------------------------------------------------
		// All the other rows, that has ever been used (many of them are always 0, for example the In-Memory ones)
		//--------------------------------------------------------
		if (GRAPH_NAME_BREAKDOWN.equals(tgdp.getName()))
		{
			LinkedHashMap<String, Double> dataMap = new LinkedHashMap<String, Double>();

			for (int r = 0; r < this.size(); r++)
			{
				String statName     = this.getAbsString        (r, "STAT_NAME");
				Double absValue     = this.getAbsValueAsDouble (r, "VALUE");
				Double microsPerSec = this.getRateValueAsDouble(r, "VALUE");

				if (Arrays.asList(STATS_TOTALS).contains(statName))
					continue;

				if (absValue == null || absValue == 0)
					continue;

				dataMap.put(statName, (microsPerSec == null) ? 0.0 : microsPerSec / 1_000_000.0);
			}

			if (dataMap.size() > 0)
				tgdp.setData(this.getTimestamp(), dataMap);
		}
	}

	/** Local graph (beside the table): the biggest statistics of the time model */
	@Override
	public List<CmChartDescriptor> createChartDescriptors()
	{
		List<CmChartDescriptor> list = new ArrayList<>();

		// Bars, not a pie: the statistics are parts of each other ('DB time' holds 'sql execute elapsed time', which holds 'PL/SQL execution elapsed time'...)
		list.add(new CmChartDescriptor()
			.id("time-model")
			.title("Time Model: where the time goes (microseconds)")
			.chartType(CmChartDescriptor.CHART_TYPE_HORIZONTAL_BAR)
			.labelColumn("STAT_NAME")
			.valueColumns("VALUE")
			.seriesLabels("Microseconds")
			.skipZeroRows(true)
			.maxItems(15)
			.splitRatio(0.5));

		return list;
	}
}
