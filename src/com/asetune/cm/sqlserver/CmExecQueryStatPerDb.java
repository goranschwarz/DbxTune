/***************************************"****************************************
 * Copyright (C) 2010-2020 Goran Schwarz
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
package com.asetune.cm.sqlserver;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import javax.naming.NameNotFoundException;
import javax.swing.JPanel;

import org.apache.commons.lang3.ArrayUtils;
import org.apache.log4j.Logger;

import com.asetune.ICounterController;
import com.asetune.IGuiController;
import com.asetune.cm.CmSettingsHelper;
import com.asetune.cm.CounterSample;
import com.asetune.cm.CounterSetTemplates;
import com.asetune.cm.CounterSetTemplates.Type;
import com.asetune.cm.CountersModel;
import com.asetune.cm.sqlserver.gui.CmExecQueryStatPerDbPanel;
import com.asetune.config.dict.MonTablesDictionary;
import com.asetune.config.dict.MonTablesDictionaryManager;
import com.asetune.graph.TrendGraphDataPoint;
import com.asetune.graph.TrendGraphDataPoint.LabelType;
import com.asetune.gui.MainFrame;
import com.asetune.gui.ResultSetTableModel;
import com.asetune.sql.conn.DbxConnection;
import com.asetune.sql.conn.info.DbmsVersionInfo;
import com.asetune.sql.conn.info.DbmsVersionInfoSqlServer;
import com.asetune.utils.Configuration;
import com.asetune.utils.NumberUtils;
import com.asetune.utils.Ver;

/**
 * @author Goran Schwarz (goran_schwarz@hotmail.com)
 */
public class CmExecQueryStatPerDb
extends CountersModel
{
	private static Logger        _logger          = Logger.getLogger(CmExecQueryStatPerDb.class);
	private static final long    serialVersionUID = 1L;

	public static final String   CM_NAME          = CmExecQueryStatPerDb.class.getSimpleName();
	public static final String   SHORT_NAME       = "Query Stat/DB";
	public static final String   HTML_DESC        = 
		"<html>" +
		"<p>Activity Grouped by Database</p>" +
		"<p>Information is fetched from dm_exec_query_stats</p>" +
		"</html>";

	public static final String   GROUP_NAME       = MainFrame.TCP_GROUP_OBJECT_ACCESS;
	public static final String   GUI_ICON_FILE    = "images/"+CM_NAME+".png";

	public static final long     NEED_SRV_VERSION = 0;
	public static final long     NEED_CE_VERSION  = 0;

	public static final String[] MON_TABLES       = new String[] {"CmExecQueryStatPerDb", "dm_exec_query_stats"};
	public static final String[] NEED_ROLES       = new String[] {};//{"VIEW SERVER STATE"};
	public static final String[] NEED_CONFIG      = new String[] {};

	public static final String[] PCT_COLUMNS      = new String[] { "est_wait_time_pct" };
	public static final String[] DIFF_COLUMNS     = new String[] {
		"est_wait_time_ms",
		"plan_count",
		"execution_count",
		"total_worker_time",
		"total_worker_time_ms",
//		"last_worker_time",
//		"min_worker_time",
//		"max_worker_time",
		"total_physical_reads",
		"total_physical_reads_mb",
//		"last_physical_reads",
//		"min_physical_reads",
//		"max_physical_reads",
		"total_logical_writes",
		"total_logical_writes_mb",
//		"last_logical_writes",
//		"min_logical_writes",
//		"max_logical_writes",
		"total_logical_reads",
		"total_logical_reads_mb",
//		"last_logical_reads",
//		"min_logical_reads",
//		"max_logical_reads",
		"total_clr_time",
		"total_clr_time_ms",
//		"last_clr_time",
//		"min_clr_time",
//		"max_clr_time",
		"total_elapsed_time",
		"total_elapsed_time_ms",
//		"last_elapsed_time",
//		"min_elapsed_time",
//		"max_elapsed_time",
//		"query_hash",
//		"query_plan_hash",
		"total_rows",
//		"last_rows",
//		"min_rows",
//		"max_rows",
		"total_dop",
		"total_grant_kb",
		"total_used_grant_kb",
		"total_ideal_grant_kb",
		"total_reserved_threads",
		"total_used_threads",
		"total_columnstore_segment_reads",
		"total_columnstore_segment_skips",
		"total_spills",
		"total_spills_mb",
		"total_num_physical_reads",
		"total_page_server_reads",
		"total_num_page_server_reads",
		"_last_column_name_only_used_as_a_place_holder_here_"
		};

	public static final boolean  NEGATIVE_DIFF_COUNTERS_TO_ZERO = false;
	public static final boolean  IS_SYSTEM_CM                   = true;
	public static final int      DEFAULT_POSTPONE_TIME          = 300; // every 5 minute
	public static final int      DEFAULT_QUERY_TIMEOUT          = CountersModel.DEFAULT_sqlQueryTimeout;;

	@Override public int     getDefaultPostponeTime()                 { return DEFAULT_POSTPONE_TIME; }
	@Override public int     getDefaultQueryTimeout()                 { return DEFAULT_QUERY_TIMEOUT; }
	@Override public boolean getDefaultIsNegativeDiffCountersToZero() { return NEGATIVE_DIFF_COUNTERS_TO_ZERO; }
	@Override public Type    getTemplateLevel()                       { return Type.ALL; }

	/**
	 * FACTORY  method to create the object
	 */
	public static CountersModel create(ICounterController counterController, IGuiController guiController)
	{
		if (guiController != null && guiController.hasGUI())
			guiController.splashWindowProgress("Loading: Counter Model '"+CM_NAME+"'");

		return new CmExecQueryStatPerDb(counterController, guiController);
	}

	public CmExecQueryStatPerDb(ICounterController counterController, IGuiController guiController)
	{
		super(counterController,
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
	private static final String  PROP_PREFIX                      = CM_NAME;

	public static final String  PROPKEY_sample_MsResourceDb = PROP_PREFIX + ".sample.ms.resourcedb";
	public static final boolean DEFAULT_sample_MsResourceDb = false;
	

	public static final String GRAPH_NAME_SQL_STATEMENT_DB_PLAN_COUNT    = "DbPlanCnt";
	public static final String GRAPH_NAME_SQL_STATEMENT_DB_PLAN_DIFF     = "DbPlanDiff";
	public static final String GRAPH_NAME_SQL_STATEMENT_DB_EXEC_SEC      = "DbExecCnt";
	public static final String GRAPH_NAME_SQL_STATEMENT_DB_ELAPSED_TIME  = "DbElapsedTime";
	public static final String GRAPH_NAME_SQL_STATEMENT_DB_WORKER_TIME   = "DbWorkerTime";
	public static final String GRAPH_NAME_SQL_STATEMENT_DB_LOGICAL_WRITE = "DbLWrite";
	public static final String GRAPH_NAME_SQL_STATEMENT_DB_LOGICAL_READ  = "DbLRead";
	public static final String GRAPH_NAME_SQL_STATEMENT_DB_PHYSICAL_READ = "DbPRead";
	public static final String GRAPH_NAME_SQL_STATEMENT_DB_ROWS_AFFECTED = "DbRows";


	
	@Override
	protected void registerDefaultValues()
	{
		super.registerDefaultValues();

		Configuration.registerDefaultValue(PROPKEY_sample_MsResourceDb, DEFAULT_sample_MsResourceDb);
	}


	/** Used by the: Create 'Offline Session' Wizard */
	@Override
	public List<CmSettingsHelper> getLocalSettings()
	{
		Configuration conf = Configuration.getCombinedConfiguration();
		List<CmSettingsHelper> list = new ArrayList<>();
		
		list.add(new CmSettingsHelper("Sample MS Resource DB", PROPKEY_sample_MsResourceDb, Boolean.class, conf.getBooleanProperty(PROPKEY_sample_MsResourceDb, DEFAULT_sample_MsResourceDb), DEFAULT_ALARM_isAlarmsEnabled, CmExecQueryStatPerDbPanel.TOOLTIP_sample_resourcedb));

		return list;
	}



	private void addTrendGraphs()
	{
		//---------------------------------------------------------------------------
		addTrendGraph(GRAPH_NAME_SQL_STATEMENT_DB_PLAN_COUNT,
				"SQL Statements Per DB - Plan Count", // Menu CheckBox text
				"SQL Statements Per DB - Plan Count ("+GROUP_NAME+"->"+SHORT_NAME+")", // Label 
				TrendGraphDataPoint.Y_AXIS_SCALE_LABELS_PERSEC,
				null, 
				LabelType.Dynamic,
				TrendGraphDataPoint.Category.OPERATIONS,
				false, // is Percent Graph
				true,  // visible at start
				0,     // graph is valid from Server Version. 0 = All Versions; >0 = Valid from this version and above 	
				-1);   // minimum height

		//---------------------------------------------------------------------------
		addTrendGraph(GRAPH_NAME_SQL_STATEMENT_DB_PLAN_DIFF,
				"SQL Statements Per DB - Plan Count Diff", // Menu CheckBox text
				"SQL Statements Per DB - Plan Count Diff ("+GROUP_NAME+"->"+SHORT_NAME+")", // Label 
				TrendGraphDataPoint.Y_AXIS_SCALE_LABELS_PERSEC,
				null, 
				LabelType.Dynamic,
				TrendGraphDataPoint.Category.OPERATIONS,
				false, // is Percent Graph
				false,  // visible at start
				0,     // graph is valid from Server Version. 0 = All Versions; >0 = Valid from this version and above 	
				-1);   // minimum height

		//---------------------------------------------------------------------------
		addTrendGraph(GRAPH_NAME_SQL_STATEMENT_DB_EXEC_SEC,
				"SQL Statements Per DB - Execution Count/sec", // Menu CheckBox text
				"SQL Statements Per DB - Execution Count/sec ("+GROUP_NAME+"->"+SHORT_NAME+")", // Label 
				TrendGraphDataPoint.Y_AXIS_SCALE_LABELS_PERSEC,
				null, 
				LabelType.Dynamic,
				TrendGraphDataPoint.Category.OPERATIONS,
				false, // is Percent Graph
				true,  // visible at start
				0,     // graph is valid from Server Version. 0 = All Versions; >0 = Valid from this version and above 	
				-1);   // minimum height

		//---------------------------------------------------------------------------
		addTrendGraph(GRAPH_NAME_SQL_STATEMENT_DB_ELAPSED_TIME,
				"SQL Statements Per DB - Sum Elapsed Time in ms", // Menu CheckBox text
				"SQL Statements Per DB - Sum Elapsed Time in ms ("+GROUP_NAME+"->"+SHORT_NAME+")", // Label 
				TrendGraphDataPoint.Y_AXIS_SCALE_LABELS_PERSEC,
				null, 
				LabelType.Dynamic,
				TrendGraphDataPoint.Category.OPERATIONS,
				false, // is Percent Graph
				true,  // visible at start
				0,     // graph is valid from Server Version. 0 = All Versions; >0 = Valid from this version and above 	
				-1);   // minimum height

		//---------------------------------------------------------------------------
		addTrendGraph(GRAPH_NAME_SQL_STATEMENT_DB_WORKER_TIME,
				"SQL Statements Per DB - Sum CPU/Worker Time in ms", // Menu CheckBox text
				"SQL Statements Per DB - Sum CPU/Worker Time in ms ("+GROUP_NAME+"->"+SHORT_NAME+")", // Label 
				TrendGraphDataPoint.Y_AXIS_SCALE_LABELS_PERSEC,
				null, 
				LabelType.Dynamic,
				TrendGraphDataPoint.Category.OPERATIONS,
				false, // is Percent Graph
				true,  // visible at start
				0,     // graph is valid from Server Version. 0 = All Versions; >0 = Valid from this version and above 	
				-1);   // minimum height

		//---------------------------------------------------------------------------
		addTrendGraph(GRAPH_NAME_SQL_STATEMENT_DB_LOGICAL_WRITE,
				"SQL Statements Per DB - Sum Logical Writes/sec", // Menu CheckBox text
				"SQL Statements Per DB - Sum Logical Writes/sec ("+GROUP_NAME+"->"+SHORT_NAME+")", // Label 
				TrendGraphDataPoint.Y_AXIS_SCALE_LABELS_PERSEC,
				null, 
				LabelType.Dynamic,
				TrendGraphDataPoint.Category.OPERATIONS,
				false, // is Percent Graph
				false,  // visible at start
				0,     // graph is valid from Server Version. 0 = All Versions; >0 = Valid from this version and above 	
				-1);   // minimum height

		//---------------------------------------------------------------------------
		addTrendGraph(GRAPH_NAME_SQL_STATEMENT_DB_LOGICAL_READ,
				"SQL Statements Per DB - Sum Logical Reads/sec", // Menu CheckBox text
				"SQL Statements Per DB - Sum Logical Reads/sec ("+GROUP_NAME+"->"+SHORT_NAME+")", // Label 
				TrendGraphDataPoint.Y_AXIS_SCALE_LABELS_PERSEC,
				null, 
				LabelType.Dynamic,
				TrendGraphDataPoint.Category.OPERATIONS,
				false, // is Percent Graph
				true,  // visible at start
				0,     // graph is valid from Server Version. 0 = All Versions; >0 = Valid from this version and above 	
				-1);   // minimum height

		//---------------------------------------------------------------------------
		addTrendGraph(GRAPH_NAME_SQL_STATEMENT_DB_PHYSICAL_READ,
				"SQL Statements Per DB - Sum Physical Reads/sec", // Menu CheckBox text
				"SQL Statements Per DB - Sum Physical Reads/sec ("+GROUP_NAME+"->"+SHORT_NAME+")", // Label 
				TrendGraphDataPoint.Y_AXIS_SCALE_LABELS_PERSEC,
				null, 
				LabelType.Dynamic,
				TrendGraphDataPoint.Category.OPERATIONS,
				false, // is Percent Graph
				false,  // visible at start
				0,     // graph is valid from Server Version. 0 = All Versions; >0 = Valid from this version and above 	
				-1);   // minimum height

		//---------------------------------------------------------------------------
		addTrendGraph(GRAPH_NAME_SQL_STATEMENT_DB_ROWS_AFFECTED,
				"SQL Statements Per DB - Sum Rows Affected/sec", // Menu CheckBox text
				"SQL Statements Per DB - Sum Rows Affected/sec ("+GROUP_NAME+"->"+SHORT_NAME+")", // Label 
				TrendGraphDataPoint.Y_AXIS_SCALE_LABELS_PERSEC,
				null, 
				LabelType.Dynamic,
				TrendGraphDataPoint.Category.OPERATIONS,
				false, // is Percent Graph
				false,  // visible at start
				0,     // graph is valid from Server Version. 0 = All Versions; >0 = Valid from this version and above 	
				-1);   // minimum height

	}

	@Override
	public void updateGraphData(TrendGraphDataPoint tgdp)
	{
		// -----------------------------------------------------------------------------------------
		if (GRAPH_NAME_SQL_STATEMENT_DB_PLAN_COUNT   .equals(tgdp.getName())) private_updateGraphData(tgdp, CountersModel.DATA_ABS , "plan_count_abs");
		if (GRAPH_NAME_SQL_STATEMENT_DB_PLAN_DIFF    .equals(tgdp.getName())) private_updateGraphData(tgdp, CountersModel.DATA_DIFF, "plan_count");
		if (GRAPH_NAME_SQL_STATEMENT_DB_EXEC_SEC     .equals(tgdp.getName())) private_updateGraphData(tgdp, CountersModel.DATA_RATE, "execution_count");
		if (GRAPH_NAME_SQL_STATEMENT_DB_ELAPSED_TIME .equals(tgdp.getName())) private_updateGraphData(tgdp, CountersModel.DATA_RATE, "total_elapsed_time_ms");
		if (GRAPH_NAME_SQL_STATEMENT_DB_WORKER_TIME  .equals(tgdp.getName())) private_updateGraphData(tgdp, CountersModel.DATA_RATE, "total_worker_time_ms");
		if (GRAPH_NAME_SQL_STATEMENT_DB_LOGICAL_WRITE.equals(tgdp.getName())) private_updateGraphData(tgdp, CountersModel.DATA_RATE, "total_logical_writes");
		if (GRAPH_NAME_SQL_STATEMENT_DB_LOGICAL_READ .equals(tgdp.getName())) private_updateGraphData(tgdp, CountersModel.DATA_RATE, "total_logical_reads");
		if (GRAPH_NAME_SQL_STATEMENT_DB_PHYSICAL_READ.equals(tgdp.getName())) private_updateGraphData(tgdp, CountersModel.DATA_RATE, "total_physical_reads");
		if (GRAPH_NAME_SQL_STATEMENT_DB_ROWS_AFFECTED.equals(tgdp.getName())) private_updateGraphData(tgdp, CountersModel.DATA_RATE, "total_rows");
	}

	private void private_updateGraphData(TrendGraphDataPoint tgdp, int whatData, String serieName)
	{
		Double[] dArray = new Double[this.size()];
		String[] lArray = new String[dArray.length];
		for (int i = 0; i < dArray.length; i++)
		{
//			lArray[i] = this.getValueAsString(whatData, i, "dbname");
//			dArray[i] = this.getValueAsDouble(whatData, i, serieName);
			
			if (whatData == CountersModel.DATA_ABS)
			{
				lArray[i] = this.getAbsString       (i, "dbname");
				dArray[i] = this.getAbsValueAsDouble(i, serieName);
			}
			else if (whatData == CountersModel.DATA_DIFF)
			{
				lArray[i] = this.getDiffString       (i, "dbname");
				dArray[i] = this.getDiffValueAsDouble(i, serieName);
			}
			else if (whatData == CountersModel.DATA_RATE)
			{
				lArray[i] = this.getRateString       (i, "dbname");
				dArray[i] = this.getRateValueAsDouble(i, serieName);
			}
		}

		// If '_Total' is the LAST entry (which is the normal) -->> Move it to First Entry
		if (lArray[lArray.length-1].equals("_Total"))
		{
			ArrayUtils.shift(lArray, 1);
			ArrayUtils.shift(dArray, 1);
		}

		// Set the values
		tgdp.setDataPoint(this.getTimestamp(), lArray, dArray);
	}


	@Override
	protected JPanel createGui()
	{
		//return super.createGui();
		return new CmExecQueryStatPerDbPanel(this);
	}


	@Override
	public String[] getDependsOnConfigForVersion(DbxConnection conn, DbmsVersionInfo versionInfo)
	{
		return NEED_CONFIG;
	}

	@Override
	public List<String> getPkForVersion(DbxConnection conn, DbmsVersionInfo versionInfo)
	{
		List <String> pkCols = new LinkedList<String>();

		pkCols.add("dbname");

		return pkCols;
	}

	@Override
	public Map<String, AggregationType> createAggregateColumns()
	{
		HashMap<String, AggregationType> aggColumns = new HashMap<>(getColumnCount());

		AggregationType tmp;

		//Should we do this;
		//I think there will be a POTENTIAL data overflow... 
		//This has NOT YET been tested;
		
		// Create the columns :::::::::::::::::::::::::::::::::::::::::::::::::::::: And ADD it to the return Map 
		tmp = new AggregationType("plan_count_abs"                 , AggregationType.Agg.SUM);   aggColumns.put(tmp.getColumnName(), tmp);
		tmp = new AggregationType("plan_count"                     , AggregationType.Agg.SUM);   aggColumns.put(tmp.getColumnName(), tmp);
		tmp = new AggregationType("execution_count"                , AggregationType.Agg.SUM);   aggColumns.put(tmp.getColumnName(), tmp);
		tmp = new AggregationType("total_elapsed_time_ms"          , AggregationType.Agg.SUM);   aggColumns.put(tmp.getColumnName(), tmp);
		tmp = new AggregationType("total_worker_time_ms"           , AggregationType.Agg.SUM);   aggColumns.put(tmp.getColumnName(), tmp);
		tmp = new AggregationType("est_wait_time_ms"               , AggregationType.Agg.SUM);   aggColumns.put(tmp.getColumnName(), tmp);
		tmp = new AggregationType("est_wait_time_pct"              , AggregationType.Agg.AVG);   aggColumns.put(tmp.getColumnName(), tmp);
		tmp = new AggregationType("total_logical_reads"            , AggregationType.Agg.SUM);   aggColumns.put(tmp.getColumnName(), tmp);
		tmp = new AggregationType("total_logical_reads_mb"         , AggregationType.Agg.SUM);   aggColumns.put(tmp.getColumnName(), tmp);
		tmp = new AggregationType("total_physical_reads"           , AggregationType.Agg.SUM);   aggColumns.put(tmp.getColumnName(), tmp);
		tmp = new AggregationType("total_logical_writes"           , AggregationType.Agg.SUM);   aggColumns.put(tmp.getColumnName(), tmp);
		tmp = new AggregationType("total_rows"                     , AggregationType.Agg.SUM);   aggColumns.put(tmp.getColumnName(), tmp);
		tmp = new AggregationType("total_clr_time_ms"              , AggregationType.Agg.SUM);   aggColumns.put(tmp.getColumnName(), tmp);
		tmp = new AggregationType("total_dop"                      , AggregationType.Agg.SUM);   aggColumns.put(tmp.getColumnName(), tmp);
//		tmp = new AggregationType("dop"                            , AggregationType.Agg.AVG);   aggColumns.put(tmp.getColumnName(), tmp);
		tmp = new AggregationType("total_grant_kb"                 , AggregationType.Agg.SUM);   aggColumns.put(tmp.getColumnName(), tmp);
		tmp = new AggregationType("total_used_grant_kb"            , AggregationType.Agg.SUM);   aggColumns.put(tmp.getColumnName(), tmp);
		tmp = new AggregationType("total_ideal_grant_kb"           , AggregationType.Agg.SUM);   aggColumns.put(tmp.getColumnName(), tmp);
		tmp = new AggregationType("total_reserved_threads"         , AggregationType.Agg.SUM);   aggColumns.put(tmp.getColumnName(), tmp);
		tmp = new AggregationType("total_used_threads"             , AggregationType.Agg.SUM);   aggColumns.put(tmp.getColumnName(), tmp);
		tmp = new AggregationType("total_columnstore_segment_reads", AggregationType.Agg.SUM);   aggColumns.put(tmp.getColumnName(), tmp);
		tmp = new AggregationType("total_columnstore_segment_skips", AggregationType.Agg.SUM);   aggColumns.put(tmp.getColumnName(), tmp);
		tmp = new AggregationType("total_spills"                   , AggregationType.Agg.SUM);   aggColumns.put(tmp.getColumnName(), tmp);
		tmp = new AggregationType("total_spills_mb"                , AggregationType.Agg.SUM);   aggColumns.put(tmp.getColumnName(), tmp);
		tmp = new AggregationType("total_page_server_reads"        , AggregationType.Agg.SUM);   aggColumns.put(tmp.getColumnName(), tmp);
		
		tmp = new AggregationType("est_wait_time_ms"               , AggregationType.Agg.SUM);   aggColumns.put(tmp.getColumnName(), tmp);

		return aggColumns;
	}

	@Override
	public boolean isAggregateRowAppendEnabled()
	{
		return true;
	}

	@Override
	public Object calculateAggregateRow_nonAggregatedColumnDataProvider(CounterSample newSample, String colName, int c, int jdbcType, Object addValue)
	{
		if ("dbid".equalsIgnoreCase(colName))
			return new Long(-1);
		
		return null;
	}

	@Override
	public void addMonTableDictForVersion(DbxConnection conn, DbmsVersionInfo versionInfo)
	{
		try 
		{
			String tn = this.getClass().getSimpleName();
			MonTablesDictionary mtd = MonTablesDictionaryManager.getInstance();
			mtd.addTable(this.getClass().getSimpleName(),  "xxx");

			mtd.addColumn(tn, "dbname"                         ,  "<html>Name of the database</html>");
			mtd.addColumn(tn, "dbid"                           ,  "<html>ID of the database</html>");
			mtd.addColumn(tn, "plan_count_abs"                 ,  "<html>How many Plan entries there is for this database</html>");
			mtd.addColumn(tn, "plan_count"                     ,  "<html>Difference in plan count between the previous sample of how many Plan entries there is for this database</html>");
			mtd.addColumn(tn, "execution_count"                ,  "<html>Number of plans executed for this database.</html>");
			mtd.addColumn(tn, "total_elapsed_time_ms"          ,  "<html>Elapsed Execution time (in milliseconds) for execution plans in this database</html>");
			mtd.addColumn(tn, "total_worker_time_ms"           ,  "<html>CPU time (in milliseconds) used to Execute plans for this database</html>");
			mtd.addColumn(tn, "est_wait_time_ms"               ,  "<html>Estimated Wait time (in milliseconds) when executing plans for this database.<br>Algorithm: total_elapsed_time_ms - total_elapsed_time_ms<br>NOTE: If DOP (parallism) is above 1 (not running in serial mode) this value will be OFF (it will be set to NULL if DOP is above 1.0)</html>");
			mtd.addColumn(tn, "est_wait_time_pct"              ,  "<html>Percentage calculation of how much time we are waiting, compared to 'total_elapsed_time_ms'.</html>");
			mtd.addColumn(tn, "total_logical_reads"            ,  "<html>Number of pages read from the buffer cache for this database.</html>");
			mtd.addColumn(tn, "total_logical_reads_mb"         ,  "<html>Number of MB from the buffer cache for this database.<br>Algorithm: total_logical_reads/128.0</html>");
			mtd.addColumn(tn, "total_logical_writes"           ,  "<html>Number of buffer pool pages dirtied. After a page is read, the page becomes dirty only the first time it is modified. When a page becomes dirty, this number is incremented. Subsequent modifications of an already dirty page do not affect this number</html>");
			mtd.addColumn(tn, "total_physical_reads"           ,  "<html>Number of physical page reads (from disk) due to that the page was not found in the buffer cache for this database.</html>");
			mtd.addColumn(tn, "total_rows"                     ,  "<html>Number of rows affected by SQL Statements (select/insert/update/delete)</html>");
			mtd.addColumn(tn, "total_clr_time_ms"              ,  "<html>Time consumed/reported (in milliseconds) for execution inside MS .NET Framework Common Language Runtime (CLR). This can be Stored Procedures, functions, triggers etc implemented as <i>external</i> objects called from the SQL Server logic.</html>");
			mtd.addColumn(tn, "total_dop"                      ,  "<html>Total DOP - Degree Of Parallism used for plans in the database.<br>NOTE: If above 1.0 means that <i>some</i> of the plans where executed using parallism.</html>");
			mtd.addColumn(tn, "dop"                            ,  "<html>Average DOP - Degree Of Parallism used for plans in the database.<br>Algorithm: total_dop/execution_count<br>NOTE: 1.0 means ALL plans has been executing in serial mode.</html>");
			mtd.addColumn(tn, "total_grant_kb"                 ,  "<html>Total amount of <b>reserved</b> <i>Memory Grants</i> in KB that all plans for this database.<br>Note: To get an average, you must divide by 'execution_count'</html>");
			mtd.addColumn(tn, "total_used_grant_kb"            ,  "<html>Total amount of <b>used</b> <i>Memory Grants</i> in KB that all plans for this database.<br>Note: To get an average, you must divide by 'execution_count'</html>");
			mtd.addColumn(tn, "total_ideal_grant_kb"           ,  "<html>Total amount of <b>ideal</b> <i>Memory Grants</i> in KB that all plans for this database.<br>Note: To get an average, you must divide by 'execution_count'</html>");
			mtd.addColumn(tn, "total_reserved_threads"         ,  "<html>The total sum of reserved parallel threads this plan used for this database.</html>");
			mtd.addColumn(tn, "total_used_threads"             ,  "<html>The total sum of used parallel threads this plan used for this database.</html>");
			mtd.addColumn(tn, "total_columnstore_segment_reads",  "<html>The total sum of columnstore segments <b>read</b> by plans in this database</html>");
			mtd.addColumn(tn, "total_columnstore_segment_skips",  "<html>The total sum of columnstore segments <b>skipped</b> by plans in this database</html>");
			mtd.addColumn(tn, "total_spills"                   ,  "<html>The total number of pages spilled by execution (to tempdb) in this database</html>");
			mtd.addColumn(tn, "total_spills_mb"                ,  "<html>The total number of MB spilled by execution (to tempdb) in this database</html>");
			mtd.addColumn(tn, "total_page_server_reads"        ,  "<html>Total number of remote page server reads performed by executions in this database<br>NOTE: Only in Azure SQL Database Hyperscale</html>");
		}
		catch (NameNotFoundException e) {/*ignore*/}
	}


	/**
	 * Used in below localCalculation(prevSample, newSample, diffData)
	 * 
	 * @param cs
	 * @param rowId
	 * @param colPos
	 */
	private void resetToZeroIfNegativeValue(CounterSample cs, int rowId, int colPos)
	{
		if (colPos < 0 || rowId < 0) 
			return;

		Object o_val = cs.getValueAsObject(rowId, colPos);
		if (o_val == null)
			return;

		if (o_val instanceof Number)
		{
			Number val = (Number) o_val;
			if (val.doubleValue() < 0.0)
			{
_logger.warn("resetToZeroIfNegativeValue(): Resetting rowId=" + rowId + ", colPos=" + colPos + ", val=" + val + ", [dbname='" + cs.getValueAsString(rowId, "dbname") + "', coName='" + cs.getColumnName(colPos) + "'] to 0 (zero) for CounterSample='" + cs.getName() + "'.");
				_logger.debug("resetToZeroIfNegativeValue(): Resetting rowId=" + rowId + ", colPos=" + colPos + ", val=" + val + ", [dbname='" + cs.getValueAsString(rowId, "dbname") + "', coName='" + cs.getColumnName(colPos) + "'] to 0 (zero) for CounterSample='" + cs.getName() + "'.");

				Number zeroVal = NumberUtils.toNumberValue(val, 0);
				cs.setValueAt(zeroVal, rowId, colPos);
			}
		}
	}

	@Override
	public void localCalculation(CounterSample prevSample, CounterSample newSample, CounterSample diffData)
	{
		//-----------------------------------------------------------
		// resetSomeColumnsNegativeValues
		//-----------------------------------------------------------
		boolean resetSomeColumnsNegativeValues = true;
		if (resetSomeColumnsNegativeValues)
		{
			// NOTE: 'plan_count' should NOT be reseted, since it will/may contain negative values
			int pos_execution_count                 = diffData.findColumn("execution_count"                );
			int pos_total_worker_time_ms            = diffData.findColumn("total_worker_time_ms"           );
			int pos_total_physical_reads            = diffData.findColumn("total_physical_reads"           );
			int pos_total_logical_writes            = diffData.findColumn("total_logical_writes"           );
			int pos_total_logical_reads             = diffData.findColumn("total_logical_reads"            );
			int pos_total_logical_reads_mb          = diffData.findColumn("total_logical_reads_mb"         );
			int pos_total_clr_time_ms               = diffData.findColumn("total_clr_time_ms"              );
			int pos_total_elapsed_time_ms           = diffData.findColumn("total_elapsed_time_ms"          );
			int pos_total_rows                      = diffData.findColumn("total_rows"                     );
			int pos_total_dop                       = diffData.findColumn("total_dop"                      );
			int pos_total_grant_kb                  = diffData.findColumn("total_grant_kb"                 );
			int pos_total_used_grant_kb             = diffData.findColumn("total_used_grant_kb"            );
			int pos_total_ideal_grant_kb            = diffData.findColumn("total_ideal_grant_kb"           );
			int pos_total_reserved_threads          = diffData.findColumn("total_reserved_threads"         );
			int pos_total_used_threads              = diffData.findColumn("total_used_threads"             );
			int pos_total_columnstore_segment_reads = diffData.findColumn("total_columnstore_segment_reads");
			int pos_total_columnstore_segment_skips = diffData.findColumn("total_columnstore_segment_skips");
			int pos_total_spills                    = diffData.findColumn("total_spills"                   );
			int pos_total_spills_mb                 = diffData.findColumn("total_spills_mb"                );
			int pos_total_page_server_reads         = diffData.findColumn("total_page_server_reads"        );
			
			int pos_est_wait_time_ms                = diffData.findColumn("est_wait_time_ms"               );

			
			int rowc = diffData.getRowCount();
			for (int r=0; r<rowc; r++)
			{
				// NOTE: Aggregated row should also be reset. (so do NOT Skip Aggregated row) 
				//if (r == getAggregatedRowId())
				//	continue;

				resetToZeroIfNegativeValue(diffData, r, pos_execution_count);
				resetToZeroIfNegativeValue(diffData, r, pos_total_worker_time_ms);
				resetToZeroIfNegativeValue(diffData, r, pos_total_physical_reads);
				resetToZeroIfNegativeValue(diffData, r, pos_total_logical_writes);
				resetToZeroIfNegativeValue(diffData, r, pos_total_logical_reads);
				resetToZeroIfNegativeValue(diffData, r, pos_total_logical_reads_mb);
				resetToZeroIfNegativeValue(diffData, r, pos_total_clr_time_ms);
				resetToZeroIfNegativeValue(diffData, r, pos_total_elapsed_time_ms);
				resetToZeroIfNegativeValue(diffData, r, pos_total_rows);
				resetToZeroIfNegativeValue(diffData, r, pos_total_dop);
				resetToZeroIfNegativeValue(diffData, r, pos_total_grant_kb);
				resetToZeroIfNegativeValue(diffData, r, pos_total_used_grant_kb);
				resetToZeroIfNegativeValue(diffData, r, pos_total_ideal_grant_kb);
				resetToZeroIfNegativeValue(diffData, r, pos_total_reserved_threads);
				resetToZeroIfNegativeValue(diffData, r, pos_total_used_threads);
				resetToZeroIfNegativeValue(diffData, r, pos_total_columnstore_segment_reads);
				resetToZeroIfNegativeValue(diffData, r, pos_total_columnstore_segment_skips);
				resetToZeroIfNegativeValue(diffData, r, pos_total_spills);
				resetToZeroIfNegativeValue(diffData, r, pos_total_spills_mb);
				resetToZeroIfNegativeValue(diffData, r, pos_total_page_server_reads);

				resetToZeroIfNegativeValue(diffData, r, pos_est_wait_time_ms);

			} // end: loop rows
		}

		//-----------------------------------------------------------
		// setActualDop
		//-----------------------------------------------------------
		boolean setActualDop = true;
		if (setActualDop)
		{
			int pos_execution_count  = diffData.findColumn("execution_count");
			int pos_total_dop        = diffData.findColumn("total_dop");
			int pos_dop              = diffData.findColumn("dop");
			
			if (pos_execution_count == -1 || pos_total_dop == -1 || pos_dop == -1)
			{
				_logger.debug(getName() + ":localCalculation(prevSample, newSample, diffData). Can't find all expected columns"
						+ ". pos_execution_count"  + "=" + pos_execution_count 
						+ ", pos_total_dop"        + "=" + pos_total_dop
						+ ", pos_all_serial"       + "=" + pos_dop 
						);
			}
			else
			{
				int rowc = diffData.getRowCount();
				for (int r=0; r<rowc; r++)
				{
					// Skip Aggregated row
					if (r == getAggregatedRowId())
						continue;

					Long execution_count = diffData.getValueAsLong(r, pos_execution_count, -1L);
					Long total_dop       = diffData.getValueAsLong(r, pos_total_dop , -1L);

					if (execution_count > 0)
					{
						BigDecimal avg_dop = new BigDecimal( (total_dop * 1.0) / (execution_count * 1.0) ).setScale(1, RoundingMode.UP);

						diffData.setValueAt(avg_dop, r, pos_dop);
					}

				} // end: loop rows
			} // end: has columns
		} // end: setEstWaitTimeMs

		//-----------------------------------------------------------
		// setEstWaitTimeMs
		//-----------------------------------------------------------
		boolean setEstWaitTimeMs = true;
		if (setEstWaitTimeMs)
		{
			int pos_total_elapsed_time_ms = diffData.findColumn("total_elapsed_time_ms");
			int pos_total_worker_time_ms  = diffData.findColumn("total_worker_time_ms");
			int pos_est_wait_time_ms      = diffData.findColumn("est_wait_time_ms");
			int pos_est_wait_time_pct     = diffData.findColumn("est_wait_time_pct");
//			int pos_dop                   = diffData.findColumn("dop");
			
			if (pos_total_elapsed_time_ms == -1 || pos_total_worker_time_ms == -1 || pos_est_wait_time_ms == -1 || pos_est_wait_time_pct == -1)
			{
				_logger.debug(getName() + ":localCalculation(prevSample, newSample, diffData). Can't find all expected columns"
						+ ". pos_total_elapsed_time_ms"  + "=" + pos_total_elapsed_time_ms 
						+ ", pos_total_worker_time_ms"   + "=" + pos_total_worker_time_ms
						+ ", pos_est_wait_time_ms"       + "=" + pos_est_wait_time_ms 
						+ ", pos_est_wait_time_pct"      + "=" + pos_est_wait_time_pct
//						+ ", pos_dop"                    + "=" + pos_dop
						);
			}
			else
			{
				int rowc = diffData.getRowCount();
				for (int r=0; r<rowc; r++)
				{
					// Skip Aggregated row
//					if (r == getAggregatedRowId())
//						continue;

					Long   total_elapsed_time_ms = diffData.getValueAsLong  (r, pos_total_elapsed_time_ms, -1L);
					Long   total_worker_time_ms  = diffData.getValueAsLong  (r, pos_total_worker_time_ms , -1L);
//					Double dop                   = diffData.getValueAsDouble(r, pos_dop                  , -1d);
					
					// Calc: est_wait_time_ms
					Long est_wait_time_ms = total_elapsed_time_ms - total_worker_time_ms;
					if (est_wait_time_ms < 0)
						est_wait_time_ms = 0L;

					// Calc: est_wait_time_pct
					BigDecimal est_wait_time_pct = null; 
					if (est_wait_time_ms > 0 && total_elapsed_time_ms > 0)
					{
						est_wait_time_pct = new BigDecimal( (est_wait_time_ms * 1.0) / (total_elapsed_time_ms * 1.0) * 100.0 ).setScale(1, RoundingMode.HALF_EVEN);
					}
					
//					// If DOP is NOT Serial (1.0) then reset: wait* columns
//					if (dop != 1.0)
//					{
//						est_wait_time_ms  = null; 
//						est_wait_time_pct = null; 
//					}

					diffData.setValueAt(est_wait_time_ms , r, pos_est_wait_time_ms);
					diffData.setValueAt(est_wait_time_pct, r, pos_est_wait_time_pct);

				} // end: loop rows
			} // end: has columns
		} // end: setEstWaitTimeMs

		//-----------------------------------------------------------
		// adjustEstWaitTimeMs
		//-----------------------------------------------------------
		boolean adjustEstWaitTimeMs = true;
		if (adjustEstWaitTimeMs)
		{
			int pos_execution_count  = diffData.findColumn("execution_count");
			int pos_total_dop        = diffData.findColumn("total_dop");
			int pos_est_wait_time_ms = diffData.findColumn("est_wait_time_ms");

			if (pos_execution_count == -1 || pos_total_dop == -1 || pos_est_wait_time_ms == -1)
			{
				_logger.debug(getName() + ":localCalculation(prevSample, newSample, diffData). Can't find all expected columns"
						+ ". pos_execution_count"  + "=" + pos_execution_count 
						+ ", pos_total_dop"        + "=" + pos_total_dop
						+ ", pos_est_wait_time_ms" + "=" + pos_est_wait_time_ms 
						);
			}
			else
			{
				int rowc = diffData.getRowCount();
				for (int r=0; r<rowc; r++)
				{
					// Skip Aggregated row
					if (r == getAggregatedRowId())
						continue;

					Double execution_count = diffData.getValueAsDouble(r, pos_execution_count);
					Double total_dop       = diffData.getValueAsDouble(r, pos_total_dop);

					if (execution_count == null || total_dop == null)
						continue;

					if (execution_count == 0.0)
						continue;
					
					
					double avg_dop = total_dop / execution_count;
//System.out.println("rowId=" + r + ", dbname='" + diffData.getValueAsString(rowc, "dbname") + "', total_dop=" + total_dop + ", execution_count=" + execution_count + ", avg_dop=" + avg_dop);
					if (avg_dop != 1.0)
					{
//System.out.println("                     rowId=" + r + ", dbname='" + diffData.getValueAsString(rowc, "dbname") + "' --- set to NULL");
						// since this period had PARALLEL statements... set 'est_wait_time_ms' to NULL (in both the newSample and diffData)
						newSample.setValueAt(null, r, pos_est_wait_time_ms);
						diffData .setValueAt(null, r, pos_est_wait_time_ms);
					}
				} // end: loop rows
			} // end: has columns
		} // end: adjustEstWaitTimeMs
	} // end: method

//TODO; Test the new code lr_mb, spills_mb, est_wait_PCT;

	@Override
	public String getSqlForVersion(DbxConnection conn, DbmsVersionInfo versionInfo)
	{
		DbmsVersionInfoSqlServer ssVersionInfo = (DbmsVersionInfoSqlServer) versionInfo;
		long ver = ssVersionInfo.getLongVersion();

		String dm_exec_query_stats = "dm_exec_query_stats";
		
		if (ssVersionInfo.isAzureSynapseAnalytics())
			dm_exec_query_stats = "dm_pdw_nodes_exec_query_stats";

		String sql = "";
		ResultSetTableModel rstm = null;
		if (conn != null)
		{
			try
			{
				sql = "SELECT top 1 * from sys." + dm_exec_query_stats + " WHERE 1=2";
				rstm = ResultSetTableModel.executeQuery(conn, sql, "getColumnNames");
			}
			catch (SQLException ex)
			{
				_logger.warn("Problems getting column names for table ''. using SQL=|" + sql + "|", ex);
			}
		}

		// Exclude 
		String excludeMsResourceDb  = ""; // sample
		if ( ! Configuration.getCombinedConfiguration().getBooleanProperty(PROPKEY_sample_MsResourceDb, DEFAULT_sample_MsResourceDb) )
			excludeMsResourceDb = "  AND CAST(atr.value AS INT) != 32767 \n"; // do NOT sample

		
		// NOTE: MAX Value for bigint/long is (precision=19): 9 223 372 036 854 775 807
		//       When we do sum on so MANY rows, should we convert it to a numeric(25,0) or similar
		//       Especially to hold _Total summary

		String col_execution_count                 =                                                           "    ,     SUM(st.execution_count                 )            AS execution_count                 \n";
		String col_total_elapsed_time_ms           =                                                           "    ,CAST(SUM(st.total_elapsed_time     / 1000.0) as bigint)  AS total_elapsed_time_ms           \n";
		String col_total_worker_time_ms            =                                                           "    ,CAST(SUM(st.total_worker_time      / 1000.0) as bigint)  AS total_worker_time_ms            \n";
		String col_est_wait_time_ms                =                                                           "    ,CAST(SUM(st.total_elapsed_time/1000.0) - SUM(st.total_worker_time/1000.0) as bigint) AS est_wait_time_ms \n";
		String col_est_wait_time_pct               =                                                           "    ,CAST(100.0 - (SUM(st.total_worker_time/1000.0) / nullif(SUM(st.total_elapsed_time/1000.0),0) * 100.0) as numeric(9,1)) AS est_wait_time_pct \n"; // nullif(val, 0) to get away with divide by 0
		String col_total_logical_reads             =                                                           "    ,     SUM(st.total_logical_reads             )            AS total_logical_reads             \n";
		String col_total_logical_reads_mb          =                                                           "    ,CAST(SUM(st.total_logical_reads)/128.0 as numeric(15,1)) AS total_logical_reads_mb          \n";
		String col_total_physical_reads            =                                                           "    ,     SUM(st.total_physical_reads            )            AS total_physical_reads            \n";
		String col_total_logical_writes            =                                                           "    ,     SUM(st.total_logical_writes            )            AS total_logical_writes            \n";
		String col_total_rows                      =                                                           "    ,     SUM(st.total_rows                      )            AS total_rows                      \n";
		String col_total_clr_time_ms               =                                                           "    ,CAST(SUM(st.total_clr_time          / 1000.0) as bigint) AS total_clr_time_ms               \n";
		String col_total_dop                       = (ver >= Ver.ver(2016))                                  ? "    ,     SUM(st.total_dop                       )            AS total_dop                       \n" : "";
		String col_avg_dop                         = (ver >= Ver.ver(2016))                                  ? "    ,CAST(NULL as numeric(5,1))                               AS dop                             \n" : "";
		String col_total_grant_kb                  = (ver >= Ver.ver(2016))                                  ? "    ,     SUM(st.total_grant_kb                  )            AS total_grant_kb                  \n" : "";
		String col_total_used_grant_kb             = (ver >= Ver.ver(2016))                                  ? "    ,     SUM(st.total_used_grant_kb             )            AS total_used_grant_kb             \n" : "";
		String col_total_ideal_grant_kb            = (ver >= Ver.ver(2016))                                  ? "    ,     SUM(st.total_ideal_grant_kb            )            AS total_ideal_grant_kb            \n" : "";
		String col_total_reserved_threads          = (ver >= Ver.ver(2016))                                  ? "    ,     SUM(st.total_reserved_threads          )            AS total_reserved_threads          \n" : "";
		String col_total_used_threads              = (ver >= Ver.ver(2016))                                  ? "    ,     SUM(st.total_used_threads              )            AS total_used_threads              \n" : "";
		String col_total_columnstore_segment_reads = (ver >= Ver.ver(2016, 2) || ver >= Ver.ver(2017, 0, 3)) ? "    ,     SUM(st.total_columnstore_segment_reads )            AS total_columnstore_segment_reads \n" : "";
		String col_total_columnstore_segment_skips = (ver >= Ver.ver(2016, 2) || ver >= Ver.ver(2017, 0, 3)) ? "    ,     SUM(st.total_columnstore_segment_skips )            AS total_columnstore_segment_skips \n" : "";
		String col_total_spills                    = (ver >= Ver.ver(2016, 2) || ver >= Ver.ver(2017, 0, 3)) ? "    ,     SUM(st.total_spills                    )            AS total_spills                    \n" : "";
		String col_total_spills_mb                 = (ver >= Ver.ver(2016, 2) || ver >= Ver.ver(2017, 0, 3)) ? "    ,CAST(SUM(st.total_spills)/128.0        as numeric(15,1)) AS total_spills_mb                 \n" : "";
		String col_total_page_server_reads         = (ssVersionInfo.isAzureSynapseAnalytics())               ? "    ,     SUM(st.total_page_server_reads         )            AS total_page_server_reads         \n" : "";

		// if we have a connection, check that the column really exists, if NOT reset the column
		if (rstm != null)
		{
			if ( ! rstm.hasColumn("total_dop"                      ) ) col_total_dop                       = "";
			if ( ! rstm.hasColumn("total_dop"                      ) ) col_avg_dop                         = "";
			if ( ! rstm.hasColumn("total_grant_kb"                 ) ) col_total_grant_kb                  = "";
			if ( ! rstm.hasColumn("total_used_grant_kb"            ) ) col_total_used_grant_kb             = "";
			if ( ! rstm.hasColumn("total_ideal_grant_kb"           ) ) col_total_ideal_grant_kb            = "";
			if ( ! rstm.hasColumn("total_reserved_threads"         ) ) col_total_reserved_threads          = "";
			if ( ! rstm.hasColumn("total_used_threads"             ) ) col_total_used_threads              = "";
			if ( ! rstm.hasColumn("total_columnstore_segment_reads") ) col_total_columnstore_segment_reads = "";
			if ( ! rstm.hasColumn("total_columnstore_segment_skips") ) col_total_columnstore_segment_skips = "";
			if ( ! rstm.hasColumn("total_spills"                   ) ) col_total_spills                    = "";
			if ( ! rstm.hasColumn("total_page_server_reads"        ) ) col_total_page_server_reads         = "";
		}

		sql = ""
			    + "SELECT \n"
			    + "     isnull(DB_NAME(CAST(atr.value AS INT)), '__dbid=' + CAST(atr.value as varchar(10))+'__') AS dbname \n"
			    + "    ,CAST(atr.value AS INT) AS dbid \n"
			    + "    ,COUNT(*) AS plan_count_abs \n"
			    + "    ,COUNT(*) AS plan_count \n"
			    +       col_execution_count
				+       col_total_elapsed_time_ms
				+       col_total_worker_time_ms
				+       col_est_wait_time_ms
				+       col_est_wait_time_pct
				+       col_total_logical_reads
				+       col_total_logical_reads_mb
				+       col_total_physical_reads
				+       col_total_logical_writes
				+       col_total_rows
				+       col_total_spills
				+       col_total_spills_mb
				+       col_total_dop
				+       col_avg_dop
				+       col_total_grant_kb
				+       col_total_used_grant_kb
				+       col_total_ideal_grant_kb
				+       col_total_reserved_threads
				+       col_total_used_threads
				+       col_total_clr_time_ms
				+       col_total_columnstore_segment_reads
				+       col_total_columnstore_segment_skips
				+       col_total_page_server_reads
			    + "FROM sys." + dm_exec_query_stats + " st \n"
			    + "CROSS APPLY sys.dm_exec_plan_attributes (st.plan_handle) atr \n"
			    + "WHERE atr.attribute = N'dbid' \n"
			    + excludeMsResourceDb
			    + "GROUP BY atr.value \n"
			    + "ORDER BY SUM(st.total_elapsed_time) DESC \n"
			    + "";

		// We could possibly add:
		// But it might be "overkill" and possibly adds extra time/load to get this info
//	    ,CAST(SUM(ecp.size_in_bytes / 1024.0) AS numeric(12,1)) AS PlanSizeKB		
//		LEFT OUTER JOIN sys.dm_exec_cached_plans ecp ON st.plan_handle = ecp.plan_handle
		// Possibly also: count different ecp.objtype: 'Proc', 'Prepared', 'Adhoc', 'ReplProc', 'Trigger', 'View', 'Default', 'UsrTab', 'SysTab', 'Check', 'Rule'
		// look at CmPlanCacheHistory...
//			    + "    ,ObjType_Proc_Cnt      = SUM(CASE WHEN ecp.objtype = N'Proc'      THEN 1.0 ELSE 0.0 END) \n"
//			    + "    ,ObjType_Prepared_Cnt  = SUM(CASE WHEN ecp.objtype = N'Prepared'  THEN 1.0 ELSE 0.0 END) \n"
//			    + "    ,ObjType_Adhoc_Cnt     = SUM(CASE WHEN ecp.objtype = N'Adhoc'     THEN 1.0 ELSE 0.0 END) \n"
//			    + "    ,ObjType_ReplProc_Cnt  = SUM(CASE WHEN ecp.objtype = N'ReplProc'  THEN 1.0 ELSE 0.0 END) \n"
//			    + "    ,ObjType_Trigger_Cnt   = SUM(CASE WHEN ecp.objtype = N'Trigger'   THEN 1.0 ELSE 0.0 END) \n"
//			    + "    ,ObjType_View_Cnt      = SUM(CASE WHEN ecp.objtype = N'View'      THEN 1.0 ELSE 0.0 END) \n"
//			    + "    ,ObjType_Default_Cnt   = SUM(CASE WHEN ecp.objtype = N'Default'   THEN 1.0 ELSE 0.0 END) \n"
//			    + "    ,ObjType_UsrTab_Cnt    = SUM(CASE WHEN ecp.objtype = N'UsrTab'    THEN 1.0 ELSE 0.0 END) \n"
//			    + "    ,ObjType_SysTab_Cnt    = SUM(CASE WHEN ecp.objtype = N'SysTab'    THEN 1.0 ELSE 0.0 END) \n"
//			    + "    ,ObjType_Check_Cnt     = SUM(CASE WHEN ecp.objtype = N'Check'     THEN 1.0 ELSE 0.0 END) \n"
//			    + "    ,ObjType_Rule_Cnt      = SUM(CASE WHEN ecp.objtype = N'Rule'      THEN 1.0 ELSE 0.0 END) \n"
//			    + " \n"
//			    + "    ,CacheObjType_CompiledPlan_Cnt     = SUM(CASE WHEN ecp.cacheobjtype = N'Compiled Plan'      THEN 1.0 ELSE 0.0 END) \n"
//			    + "    ,CacheObjType_CompiledPlanStub_Cnt = SUM(CASE WHEN ecp.cacheobjtype = N'Compiled Plan Stub' THEN 1.0 ELSE 0.0 END) \n"
//			    + "    ,CacheObjType_ParseTree_Cnt        = SUM(CASE WHEN ecp.cacheobjtype = N'Parse Tree'         THEN 1.0 ELSE 0.0 END) \n"
//			    + "    ,CacheObjType_ExtendedProc_Cnt     = SUM(CASE WHEN ecp.cacheobjtype = N'Extended Proc'      THEN 1.0 ELSE 0.0 END) \n"
//			    + "    ,CacheObjType_ClrCompiledFunc_Cnt  = SUM(CASE WHEN ecp.cacheobjtype = N'CLR Compiled Func'  THEN 1.0 ELSE 0.0 END) \n"
//			    + "    ,CacheObjType_ClrCompiledProc_Cnt  = SUM(CASE WHEN ecp.cacheobjtype = N'CLR Compiled Proc'  THEN 1.0 ELSE 0.0 END) \n"

		
		return sql;
	}
}
