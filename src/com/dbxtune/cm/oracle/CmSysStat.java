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
public class CmSysStat
extends CountersModel
{
	private static final Logger _logger = LogManager.getLogger(MethodHandles.lookup().lookupClass());
	private static final long    serialVersionUID = 1L;

	public static final String   CM_NAME          = CmSysStat.class.getSimpleName();
	public static final String   SHORT_NAME       = "SysStat";
	public static final String   HTML_DESC        =
		"<html>" +
		"System Statistics: counters for <i>what work</i> the instance has done (calls, parses, reads, writes, redo, sorts...).<br>" +
		"One row per statistic, for the whole instance. Statistics that has never been used (the value is 0) are not included.<br>" +
		"<br>" +
		"<b>Tip</b>: Look at the RATE values (per second). The ABS values are since the instance was started.<br>" +
		"</html>";

	public static final String   GROUP_NAME       = MainFrame.TCP_GROUP_SERVER;
	public static final String   GUI_ICON_FILE    = "images/" + CM_NAME + ".png";

	public static final long     NEED_SRV_VERSION = 0;
	public static final long     NEED_CE_VERSION  = 0;

	public static final String[] MON_TABLES       = new String[] {CM_NAME, "V$SYSSTAT"};
	public static final String[] NEED_ROLES       = new String[] {};
	public static final String[] NEED_CONFIG      = new String[] {};

	public static final String[] PCT_COLUMNS      = new String[] {};
	public static final String[] DIFF_COLUMNS     = new String[] {
		"VALUE"
		};

	public static final boolean  NEGATIVE_DIFF_COUNTERS_TO_ZERO = true;
	public static final boolean  IS_SYSTEM_CM                   = true;
	public static final int      DEFAULT_POSTPONE_TIME          = 0;
	public static final int      DEFAULT_QUERY_TIMEOUT          = CountersModel.DEFAULT_sqlQueryTimeout;

	@Override public int     getDefaultPostponeTime()                 { return DEFAULT_POSTPONE_TIME; }
	@Override public int     getDefaultQueryTimeout()                 { return DEFAULT_QUERY_TIMEOUT; }
	@Override public boolean getDefaultIsNegativeDiffCountersToZero() { return NEGATIVE_DIFF_COUNTERS_TO_ZERO; }
	@Override public Type    getTemplateLevel()                       { return Type.LARGE; }

	/**
	 * FACTORY  method to create the object
	 */
	public static CountersModel create(ICounterController counterController, IGuiController guiController)
	{
		if (guiController != null && guiController.hasGUI())
			guiController.splashWindowProgress("Loading: Counter Model '" + CM_NAME + "'");

		return new CmSysStat(counterController, guiController);
	}

	public CmSysStat(ICounterController counterController, IGuiController guiController)
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
	public static final String GRAPH_NAME_CALLS          = "Calls";
	public static final String GRAPH_NAME_PARSE          = "Parse";
	public static final String GRAPH_NAME_LOGICAL_IO     = "LogicalIo";
	public static final String GRAPH_NAME_PHYSICAL_IO    = "PhysicalIo";
	public static final String GRAPH_NAME_PHYSICAL_IO_MB = "PhysicalIoMb";
	public static final String GRAPH_NAME_REDO_KB        = "RedoKb";
	public static final String GRAPH_NAME_REDO_WRITES    = "RedoWrites";
	public static final String GRAPH_NAME_TABLE_SCANS    = "TableScans";
	public static final String GRAPH_NAME_SORTS          = "Sorts";
	public static final String GRAPH_NAME_NETWORK_KB     = "NetworkKb";
	public static final String GRAPH_NAME_ENQUEUE        = "Enqueue";

	// The statistics (NAME in V$SYSSTAT) for each graph: used both as the series label, and to find the row
	private static final String[] STATS_CALLS          = new String[] {"user calls", "recursive calls", "execute count", "SQL*Net roundtrips to/from client"};
	private static final String[] STATS_PARSE          = new String[] {"parse count (total)", "parse count (hard)", "parse count (failures)", "session cursor cache hits"};
	private static final String[] STATS_LOGICAL_IO     = new String[] {"session logical reads", "db block gets", "consistent gets", "db block changes"};
	private static final String[] STATS_PHYSICAL_IO    = new String[] {"physical reads", "physical writes", "physical reads direct", "physical writes direct"};
	private static final String[] STATS_PHYSICAL_IO_MB = new String[] {"physical read total bytes", "physical write total bytes"};
	private static final String[] STATS_REDO_KB        = new String[] {"redo size"};
	private static final String[] STATS_REDO_WRITES    = new String[] {"redo writes", "redo synch writes"};
	private static final String[] STATS_TABLE_SCANS    = new String[] {"table scans (long tables)", "table scans (short tables)", "table fetch by rowid", "table fetch continued row"};
	private static final String[] STATS_SORTS          = new String[] {"sorts (memory)", "sorts (disk)", "workarea executions - optimal", "workarea executions - onepass", "workarea executions - multipass"};
	private static final String[] STATS_NETWORK_KB     = new String[] {"bytes sent via SQL*Net to client", "bytes received via SQL*Net from client"};
	private static final String[] STATS_ENQUEUE        = new String[] {"enqueue requests", "enqueue waits", "enqueue timeouts", "enqueue deadlocks"};

	private void addTrendGraphs()
	{
		addTrendGraph(GRAPH_NAME_CALLS,
			"Calls per Second", 	                // Menu CheckBox text
			"Calls and Executions per Second (" + SHORT_NAME + ")", // Graph Label
			TrendGraphDataPoint.createGraphProps(TrendGraphDataPoint.Y_AXIS_SCALE_LABELS_PERSEC, CentralPersistReader.SampleType.MAX_OVER_SAMPLES),
			STATS_CALLS,
			LabelType.Static,
			TrendGraphDataPoint.Category.OPERATIONS,
			false, // is Percent Graph
			false, // visible at start
			0,     // graph is valid from Server Version. 0 = All Versions; >0 = Valid from this version and above
			-1);   // minimum height

		addTrendGraph(GRAPH_NAME_PARSE,
			"Parses per Second", 	                // Menu CheckBox text
			"SQL Parses per Second (" + SHORT_NAME + ")", // Graph Label
			TrendGraphDataPoint.createGraphProps(TrendGraphDataPoint.Y_AXIS_SCALE_LABELS_PERSEC, CentralPersistReader.SampleType.MAX_OVER_SAMPLES),
			STATS_PARSE,
			LabelType.Static,
			TrendGraphDataPoint.Category.OPERATIONS,
			false, // is Percent Graph
			false, // visible at start
			0,     // graph is valid from Server Version. 0 = All Versions; >0 = Valid from this version and above
			-1);   // minimum height

		addTrendGraph(GRAPH_NAME_LOGICAL_IO,
			"Logical IO per Second", 	                // Menu CheckBox text
			"Logical IO (blocks read from the Buffer Cache) per Second (" + SHORT_NAME + ")", // Graph Label
			TrendGraphDataPoint.createGraphProps(TrendGraphDataPoint.Y_AXIS_SCALE_LABELS_PERSEC, CentralPersistReader.SampleType.MAX_OVER_SAMPLES),
			STATS_LOGICAL_IO,
			LabelType.Static,
			TrendGraphDataPoint.Category.CACHE,
			false, // is Percent Graph
			false, // visible at start
			0,     // graph is valid from Server Version. 0 = All Versions; >0 = Valid from this version and above
			-1);   // minimum height

		addTrendGraph(GRAPH_NAME_PHYSICAL_IO,
			"Physical IO per Second", 	                // Menu CheckBox text
			"Physical IO (blocks read from, or written to disk) per Second (" + SHORT_NAME + ")", // Graph Label
			TrendGraphDataPoint.createGraphProps(TrendGraphDataPoint.Y_AXIS_SCALE_LABELS_PERSEC, CentralPersistReader.SampleType.MAX_OVER_SAMPLES),
			STATS_PHYSICAL_IO,
			LabelType.Static,
			TrendGraphDataPoint.Category.DISK,
			false, // is Percent Graph
			false, // visible at start
			0,     // graph is valid from Server Version. 0 = All Versions; >0 = Valid from this version and above
			-1);   // minimum height

		addTrendGraph(GRAPH_NAME_PHYSICAL_IO_MB,
			"Physical IO in MB per Second", 	                // Menu CheckBox text
			"Physical IO in MB per Second (" + SHORT_NAME + ")", // Graph Label
			TrendGraphDataPoint.createGraphProps(TrendGraphDataPoint.Y_AXIS_SCALE_LABELS_MB, CentralPersistReader.SampleType.MAX_OVER_SAMPLES),
			STATS_PHYSICAL_IO_MB,
			LabelType.Static,
			TrendGraphDataPoint.Category.DISK,
			false, // is Percent Graph
			false, // visible at start
			0,     // graph is valid from Server Version. 0 = All Versions; >0 = Valid from this version and above
			-1);   // minimum height

		addTrendGraph(GRAPH_NAME_REDO_KB,
			"Redo Generated in KB per Second", 	                // Menu CheckBox text
			"Redo Generated in KB per Second (" + SHORT_NAME + ")", // Graph Label
			TrendGraphDataPoint.createGraphProps(TrendGraphDataPoint.Y_AXIS_SCALE_LABELS_KB, CentralPersistReader.SampleType.MAX_OVER_SAMPLES),
			STATS_REDO_KB,
			LabelType.Static,
			TrendGraphDataPoint.Category.DISK,
			false, // is Percent Graph
			false, // visible at start
			0,     // graph is valid from Server Version. 0 = All Versions; >0 = Valid from this version and above
			-1);   // minimum height

		addTrendGraph(GRAPH_NAME_REDO_WRITES,
			"Redo Writes per Second", 	                // Menu CheckBox text
			"Redo Writes per Second (" + SHORT_NAME + ")", // Graph Label
			TrendGraphDataPoint.createGraphProps(TrendGraphDataPoint.Y_AXIS_SCALE_LABELS_PERSEC, CentralPersistReader.SampleType.MAX_OVER_SAMPLES),
			STATS_REDO_WRITES,
			LabelType.Static,
			TrendGraphDataPoint.Category.DISK,
			false, // is Percent Graph
			false, // visible at start
			0,     // graph is valid from Server Version. 0 = All Versions; >0 = Valid from this version and above
			-1);   // minimum height

		addTrendGraph(GRAPH_NAME_TABLE_SCANS,
			"Table Scans and Row Fetches per Second", 	                // Menu CheckBox text
			"Table Scans and Row Fetches per Second (" + SHORT_NAME + ")", // Graph Label
			TrendGraphDataPoint.createGraphProps(TrendGraphDataPoint.Y_AXIS_SCALE_LABELS_PERSEC, CentralPersistReader.SampleType.MAX_OVER_SAMPLES),
			STATS_TABLE_SCANS,
			LabelType.Static,
			TrendGraphDataPoint.Category.OPERATIONS,
			false, // is Percent Graph
			false, // visible at start
			0,     // graph is valid from Server Version. 0 = All Versions; >0 = Valid from this version and above
			-1);   // minimum height

		addTrendGraph(GRAPH_NAME_SORTS,
			"Sorts and Work Areas per Second", 	                // Menu CheckBox text
			"Sorts and Work Area Executions per Second (" + SHORT_NAME + ")", // Graph Label
			TrendGraphDataPoint.createGraphProps(TrendGraphDataPoint.Y_AXIS_SCALE_LABELS_PERSEC, CentralPersistReader.SampleType.MAX_OVER_SAMPLES),
			STATS_SORTS,
			LabelType.Static,
			TrendGraphDataPoint.Category.MEMORY,
			false, // is Percent Graph
			false, // visible at start
			0,     // graph is valid from Server Version. 0 = All Versions; >0 = Valid from this version and above
			-1);   // minimum height

		addTrendGraph(GRAPH_NAME_NETWORK_KB,
			"Network Traffic in KB per Second", 	                // Menu CheckBox text
			"Network Traffic to/from Clients in KB per Second (" + SHORT_NAME + ")", // Graph Label
			TrendGraphDataPoint.createGraphProps(TrendGraphDataPoint.Y_AXIS_SCALE_LABELS_KB, CentralPersistReader.SampleType.MAX_OVER_SAMPLES),
			STATS_NETWORK_KB,
			LabelType.Static,
			TrendGraphDataPoint.Category.NETWORK,
			false, // is Percent Graph
			false, // visible at start
			0,     // graph is valid from Server Version. 0 = All Versions; >0 = Valid from this version and above
			-1);   // minimum height

		addTrendGraph(GRAPH_NAME_ENQUEUE,
			"Enqueue (lock) Requests per Second", 	                // Menu CheckBox text
			"Enqueue (lock) Requests, Waits, Timeouts and Deadlocks per Second (" + SHORT_NAME + ")", // Graph Label
			TrendGraphDataPoint.createGraphProps(TrendGraphDataPoint.Y_AXIS_SCALE_LABELS_PERSEC, CentralPersistReader.SampleType.MAX_OVER_SAMPLES),
			STATS_ENQUEUE,
			LabelType.Static,
			TrendGraphDataPoint.Category.LOCK,
			false, // is Percent Graph
			false, // visible at start
			0,     // graph is valid from Server Version. 0 = All Versions; >0 = Valid from this version and above
			-1);   // minimum height
	}

	@Override
	public List<String> getPkForVersion(DbxConnection conn, DbmsVersionInfo versionInfo)
	{
		List <String> pkCols = new LinkedList<String>();

		// The NAME is unique, and it is what we use to find the rows for the graphs
		pkCols.add("NAME");

		return pkCols;
	}

	@Override
	public String getSqlForVersion(DbxConnection conn, DbmsVersionInfo versionInfo)
	{
		// CLASS is a bitmap: 1=User, 2=Redo, 4=Enqueue, 8=Cache, 16=OS, 32=RAC, 64=SQL, 128=Debug  (a statistic can be in more than one class)
		String sql = ""
			+ "select \n"
			+ "     s.NAME \n"
			+ "    ,CAST(RTRIM(   DECODE(BITAND(s.CLASS,   1),   1, 'User,') \n"
			+ "                || DECODE(BITAND(s.CLASS,   2),   2, 'Redo,') \n"
			+ "                || DECODE(BITAND(s.CLASS,   4),   4, 'Enqueue,') \n"
			+ "                || DECODE(BITAND(s.CLASS,   8),   8, 'Cache,') \n"
			+ "                || DECODE(BITAND(s.CLASS,  16),  16, 'OS,') \n"
			+ "                || DECODE(BITAND(s.CLASS,  32),  32, 'RAC,') \n"
			+ "                || DECODE(BITAND(s.CLASS,  64),  64, 'SQL,') \n"
			+ "                || DECODE(BITAND(s.CLASS, 128), 128, 'Debug,') \n"
			+ "               , ',') as VARCHAR2(40)) as CLASS_NAME \n"
			+ "    ,s.VALUE \n"
			+ "    ,s.CLASS \n"
			+ "    ,s.STAT_ID \n"
			+ "from V$SYSSTAT s \n"
			+ "where s.VALUE <> 0 \n"
			+ "order by s.NAME \n"
			+ "";

		return sql;
	}

	@Override
	public void addMonTableDictForVersion(DbxConnection conn, DbmsVersionInfo versionInfo)
	{
		try
		{
			MonTablesDictionary mtd = MonTablesDictionaryManager.getInstance();

			mtd.addTable(CM_NAME, "System Statistics: counters for what work the instance has done.");

			mtd.addColumn(CM_NAME, "CLASS_NAME", "<html>What area the statistic belongs to: User, Redo, Enqueue, Cache, OS, RAC, SQL, Debug.<br>A statistic can be in more than one class.<br><b>Formula</b>: a translation of the bitmap in column CLASS</html>");
		}
		catch (NameNotFoundException e)
		{
			_logger.warn("Problems in cm='" + CM_NAME + "', adding addMonTableDictForVersion. Caught: " + e);
		}
	}

	/**
	 * Get the RATE (value per second) for some statistics
	 *
	 * @param statNames   The NAME's in V$SYSSTAT (which is the PK)
	 * @param divideBy    1.0 = as is, 1024.0 = bytes to KB, 1048576.0 = bytes to MB
	 * @return one value for each name. A statistic that is not found has the value 0 (only statistics that has a value are fetched)
	 */
	private Double[] getRatePerSec(String[] statNames, double divideBy)
	{
		Double[] arr = new Double[statNames.length];
		for (int i = 0; i < statNames.length; i++)
		{
			Double val = this.getRateValueAsDouble(createPkStr(statNames[i]), "VALUE");

			arr[i] = (val == null) ? 0.0 : val / divideBy;
		}
		return arr;
	}

	@Override
	public void updateGraphData(TrendGraphDataPoint tgdp)
	{
		if (GRAPH_NAME_CALLS         .equals(tgdp.getName())) tgdp.setDataPoint(this.getTimestamp(), getRatePerSec(STATS_CALLS         , 1.0));
		if (GRAPH_NAME_PARSE         .equals(tgdp.getName())) tgdp.setDataPoint(this.getTimestamp(), getRatePerSec(STATS_PARSE         , 1.0));
		if (GRAPH_NAME_LOGICAL_IO    .equals(tgdp.getName())) tgdp.setDataPoint(this.getTimestamp(), getRatePerSec(STATS_LOGICAL_IO    , 1.0));
		if (GRAPH_NAME_PHYSICAL_IO   .equals(tgdp.getName())) tgdp.setDataPoint(this.getTimestamp(), getRatePerSec(STATS_PHYSICAL_IO   , 1.0));
		if (GRAPH_NAME_PHYSICAL_IO_MB.equals(tgdp.getName())) tgdp.setDataPoint(this.getTimestamp(), getRatePerSec(STATS_PHYSICAL_IO_MB, 1024.0 * 1024.0));
		if (GRAPH_NAME_REDO_KB       .equals(tgdp.getName())) tgdp.setDataPoint(this.getTimestamp(), getRatePerSec(STATS_REDO_KB       , 1024.0));
		if (GRAPH_NAME_REDO_WRITES   .equals(tgdp.getName())) tgdp.setDataPoint(this.getTimestamp(), getRatePerSec(STATS_REDO_WRITES   , 1.0));
		if (GRAPH_NAME_TABLE_SCANS   .equals(tgdp.getName())) tgdp.setDataPoint(this.getTimestamp(), getRatePerSec(STATS_TABLE_SCANS   , 1.0));
		if (GRAPH_NAME_SORTS         .equals(tgdp.getName())) tgdp.setDataPoint(this.getTimestamp(), getRatePerSec(STATS_SORTS         , 1.0));
		if (GRAPH_NAME_NETWORK_KB    .equals(tgdp.getName())) tgdp.setDataPoint(this.getTimestamp(), getRatePerSec(STATS_NETWORK_KB    , 1024.0));
		if (GRAPH_NAME_ENQUEUE       .equals(tgdp.getName())) tgdp.setDataPoint(this.getTimestamp(), getRatePerSec(STATS_ENQUEUE       , 1.0));
	}
}
