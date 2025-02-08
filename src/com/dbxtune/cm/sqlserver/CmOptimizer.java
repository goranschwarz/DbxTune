/*******************************************************************************
 * Copyright (C) 2010-2025 Goran Schwarz
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
package com.dbxtune.cm.sqlserver;

import java.awt.event.MouseEvent;
import java.math.BigDecimal;
import java.util.LinkedList;
import java.util.List;

import com.dbxtune.ICounterController;
import com.dbxtune.IGuiController;
import com.dbxtune.central.pcs.CentralPersistReader;
import com.dbxtune.cm.CounterSample;
import com.dbxtune.cm.CounterSetTemplates;
import com.dbxtune.cm.CounterSetTemplates.Type;
import com.dbxtune.cm.CountersModel;
import com.dbxtune.config.dict.SqlServerOptimizerCounterDictionary;
import com.dbxtune.graph.TrendGraphDataPoint;
import com.dbxtune.graph.TrendGraphDataPoint.LabelType;
import com.dbxtune.gui.MainFrame;
import com.dbxtune.sql.conn.DbxConnection;
import com.dbxtune.sql.conn.info.DbmsVersionInfo;
import com.dbxtune.sql.conn.info.DbmsVersionInfoSqlServer;
import com.dbxtune.utils.Configuration;
import com.dbxtune.utils.StringUtil;

/**
 * @author Goran Schwarz (goran_schwarz@hotmail.com)
 */
public class CmOptimizer
extends CountersModel
{
	private static final long    serialVersionUID = 1L;

	public static final String   CM_NAME          = CmOptimizer.class.getSimpleName();
	public static final String   SHORT_NAME       = "Optimizer Info";
	public static final String   HTML_DESC        = 
		"<html>" +
		"<p>Optimizer Statistic Information</p>" +
		"</html>";

	public static final String   GROUP_NAME       = MainFrame.TCP_GROUP_SERVER;
	public static final String   GUI_ICON_FILE    = "images/"+CM_NAME+".png";

	public static final long     NEED_SRV_VERSION = 0;
	public static final long     NEED_CE_VERSION  = 0;

	public static final String[] MON_TABLES       = new String[] {"dm_exec_query_optimizer_info"};
	public static final String[] NEED_ROLES       = new String[] {"VIEW SERVER STATE"};
	public static final String[] NEED_CONFIG      = new String[] {};

	public static final String[] PCT_COLUMNS      = new String[] {
		"pct"
	};
	public static final String[] DIFF_COLUMNS     = new String[] {
		"occurrence"
	};

	public static final boolean  NEGATIVE_DIFF_COUNTERS_TO_ZERO = false;
	public static final boolean  IS_SYSTEM_CM                   = true;
	public static final int      DEFAULT_POSTPONE_TIME          = 0;
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

		return new CmOptimizer(counterController, guiController);
	}

	public CmOptimizer(ICounterController counterController, IGuiController guiController)
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
	private static final String[] NON_PCT_COUNTERS_CSV = {
		"optimizations", 
		"elapsed time", 
		"final cost", 
		"tasks", 
		"search 0 time", 
		"search 0 tasks", 
		"search 1 time", 
		"search 1 tasks", 
		"search 2 time", 
		"search 2 tasks", 
		"gain stage 0 to stage 1", 
		"gain stage 1 to stage 2", 
		"unnest failed", 
		"tables", 
		"maximum DOP", 
		"maximum recursion level"
	};
	public static final String PROPKEY_nonPctCounters_csv = CM_NAME + ".nonPctCounters.csv";
	public static final String DEFAULT_nonPctCounters_csv = StringUtil.toCommaStr(NON_PCT_COUNTERS_CSV);

	private static final String[] DEFAULT_GRAPH_OPTIMIZER_STAT_KEYS = {
			"optimizations", 
			"trivial plan",
			"search 0",
			"search 1",
			"search 2",
			"timeout",
			"memory limit exceeded",
			"insert stmt",
			"delete stmt",
			"update stmt",
			"merge stmt",
			"contains subquery",
			"hints",
			"order hint",
			"join hint",
			"remote query",
			"indexed views loaded",
			"indexed views matched",
			"indexed views used",
			"indexed views updated",
			"dynamic cursor request",
			"fast forward cursor request"
		};
	public static final String PROPKEY_graph_OptimizerStatKeys_csv = CM_NAME + ".graph.OptimizerStatKeys.csv";
	public static final String DEFAULT_graph_OptimizerStatKeys_csv = StringUtil.toCommaStr(DEFAULT_GRAPH_OPTIMIZER_STAT_KEYS);

	public static final String GRAPH_NAME_OPTIMIZER_STAT_KEY = "OptimizerStatKey";
	
	private void addTrendGraphs()
	{
		addTrendGraph(GRAPH_NAME_OPTIMIZER_STAT_KEY,
				"Optimizer Statistics by Type per Second",        // Menu CheckBox text
				"Optimizer Statistics by Type per Second ("+GROUP_NAME+"->"+SHORT_NAME+")", // Label 
				TrendGraphDataPoint.createGraphProps(TrendGraphDataPoint.Y_AXIS_SCALE_LABELS_COUNT, CentralPersistReader.SampleType.MAX_OVER_SAMPLES),
				null, 
				LabelType.Dynamic,
				TrendGraphDataPoint.Category.SPACE,
				false, // is Percent Graph
				false, // visible at start
				0,     // graph is valid from Server Version. 0 = All Versions; >0 = Valid from this version and above 
				-1);   // minimum height
	}

	@Override
	public void updateGraphData(TrendGraphDataPoint tgdp)
	{
		if (GRAPH_NAME_OPTIMIZER_STAT_KEY.equals(tgdp.getName()))
		{
			String[] keys = DEFAULT_GRAPH_OPTIMIZER_STAT_KEYS;

			// Get what "lines" we want to have in the graph
			// Note: reuse DEFAULT_GRAPH_XXX_VALUES if not changed
			String keysConfigStr = Configuration.getCombinedConfiguration().getProperty(PROPKEY_graph_OptimizerStatKeys_csv, DEFAULT_graph_OptimizerStatKeys_csv);
			if ( ! keysConfigStr.equals(DEFAULT_graph_OptimizerStatKeys_csv) )
			{
				List<String> configList = StringUtil.parseCommaStrToList(keysConfigStr);
				keys = configList.stream().toArray(String[]::new);
			}
			
			// Write 1 "line" for each "key"
			Double[] dArray = new Double[keys.length];
			String[] lArray = new String[keys.length];

			int a = 0;
			for (String key : keys)
			{
				Double dvalue = this.getRateValueAsDouble(key, "occurrence", 0d);

				lArray[a] = key;
				dArray[a] = dvalue;
				a++;
			}

			// Set the values
			tgdp.setDataPoint(this.getTimestamp(), lArray, dArray);
		}
	}
	
//	@Override
//	protected TabularCntrPanel createGui()
//	{
//		return new CmOptimizerPanel(this);
//	}

	@Override
	public String[] getDependsOnConfigForVersion(DbxConnection conn, DbmsVersionInfo versionInfo)
	{
		return NEED_CONFIG;
	}

	@Override
	public List<String> getPkForVersion(DbxConnection conn, DbmsVersionInfo versionInfo)
	{
		List <String> pkCols = new LinkedList<String>();

		pkCols.add("counter");

		return pkCols;
	}

	@Override
	public String getSqlForVersion(DbxConnection conn, DbmsVersionInfo versionInfo)
	{
		DbmsVersionInfoSqlServer ssVersionInfo = (DbmsVersionInfoSqlServer) versionInfo;

		String dm_exec_query_optimizer_info = "dm_exec_query_optimizer_info";
		
		if (ssVersionInfo.isAzureSynapseAnalytics())
			dm_exec_query_optimizer_info = "dm_pdw_nodes_exec_query_optimizer_info";

		List<String> nonPctCounters = StringUtil.parseCommaStrToList(Configuration.getCombinedConfiguration().getProperty(PROPKEY_nonPctCounters_csv, DEFAULT_nonPctCounters_csv));
		
//		String sql = "select *     /* ${cmCollectorName} */ \n" 
//		           + "from sys." + dm_exec_query_optimizer_info;

		String sql = ""
			    + "DECLARE @occurrences bigint =  (SELECT occurrence FROM sys." + dm_exec_query_optimizer_info + " WHERE counter = 'optimizations') \n"
			    + "SELECT \n"
			    + "     counter \n"
			    + "    ,occurrence \n"
			    + "    ,value \n"
//			    + "    ,pct = CAST((occurrence * 100.00)/@occurrences AS DECIMAL(10,1)) \n"
			    + "           /* Do NOT do Percent Calculation on the below 'counters' */ \n"
			    + "           /* This can be changed with: " + PROPKEY_nonPctCounters_csv + " = c1, c2, c3, c4 */ \n"
			    + "    ,pct = CASE WHEN counter IN (" + StringUtil.toCommaStrQuoted("'", nonPctCounters) + ") \n"
			    + "                   THEN NULL \n"
			    + "                   ELSE CAST((occurrence * 100.0)/@occurrences AS DECIMAL(10,1)) \n"
			    + "              END \n"
			    + "FROM sys." + dm_exec_query_optimizer_info + " \n"
			    + "";

		return sql;
	}

	@Override
	public void localCalculation(CounterSample prevSample, CounterSample newSample, CounterSample diffData)
	{
		int pos_occurrence = diffData.findColumn("occurrence");
		int pos_pct        = diffData.findColumn("pct");

		int pkRowId = diffData.getRowNumberForPkValue("optimizations");
		if (pkRowId == -1)
			return;
//		Long totalOccurrences = diffData.getValueAsLong(createPkStr("optimizations"), "occurrence");
		Long totalOccurrences = diffData.getValueAsLong(pkRowId, pos_occurrence);
		
		if (totalOccurrences == null) return;
		if (totalOccurrences <= 0   ) return;

		// Loop on all diffData rows
		for (int rowId = 0; rowId < diffData.getRowCount(); rowId++)
		{
			Double val_pct = diffData.getValueAsDouble(rowId, pos_pct, null);
			if (val_pct != null)
			{
				Long occurrence = diffData.getValueAsLong(rowId, pos_occurrence);
				
				double calc = (occurrence * 100.0) / (totalOccurrences * 1.0);
				BigDecimal calcValue = new BigDecimal(calc).setScale(1, BigDecimal.ROUND_HALF_EVEN);
				
				diffData.setValueAt(calcValue, rowId, pos_pct);
			}
		}
	}
	
	@Override
	public String getToolTipTextOnTableCell(MouseEvent e, String colName, Object cellValue, int modelRow, int modelCol) 
	{
		if ("counter".equals(colName) && cellValue instanceof String)
		{
			return SqlServerOptimizerCounterDictionary.getInstance().getDescriptionHtml((String) cellValue);
		}
		
		return super.getToolTipTextOnTableCell(e, colName, cellValue, modelRow, modelCol);
	}
}
