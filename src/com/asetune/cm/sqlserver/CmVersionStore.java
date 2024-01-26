/*******************************************************************************
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

import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import javax.naming.NameNotFoundException;

import org.apache.commons.lang3.ArrayUtils;

import com.asetune.ICounterController;
import com.asetune.IGuiController;
import com.asetune.central.pcs.CentralPersistReader;
import com.asetune.cm.CounterSample;
import com.asetune.cm.CounterSetTemplates;
import com.asetune.cm.CounterSetTemplates.Type;
import com.asetune.cm.CountersModel;
import com.asetune.config.dict.MonTablesDictionary;
import com.asetune.config.dict.MonTablesDictionaryManager;
import com.asetune.graph.TrendGraphDataPoint;
import com.asetune.graph.TrendGraphDataPoint.LabelType;
import com.asetune.gui.MainFrame;
import com.asetune.sql.conn.DbxConnection;
import com.asetune.sql.conn.info.DbmsVersionInfo;
import com.asetune.utils.Ver;

/**
 * @author Goran Schwarz (goran_schwarz@hotmail.com)
 */
public class CmVersionStore
extends CountersModel
{
//	private static Logger        _logger          = Logger.getLogger(CmTempdbUsage.class);
	private static final long    serialVersionUID = 1L;

	public static final String   CM_NAME          = CmVersionStore.class.getSimpleName();
	public static final String   SHORT_NAME       = "Version Store";
	public static final String   HTML_DESC        = 
		"<html>" +
			"<p>How much is the Version Storage Used per Database in tempdb.</p>" +
		"</html>";

	public static final String   GROUP_NAME       = MainFrame.TCP_GROUP_SERVER;
	public static final String   GUI_ICON_FILE    = "images/"+CM_NAME+".png";

	public static final long     NEED_SRV_VERSION = Ver.ver(2016,0,0, 2); // 2016 SP2
	public static final long     NEED_CE_VERSION  = 0;

	public static final String[] MON_TABLES       = new String[] {"dm_tran_version_store_space_usage"};
	public static final String[] NEED_ROLES       = new String[] {"VIEW SERVER STATE"};
	public static final String[] NEED_CONFIG      = new String[] {};

	public static final String[] PCT_COLUMNS      = new String[] {};
	public static final String[] DIFF_COLUMNS     = new String[] {
		"reserved_page_count_diff"
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

		return new CmVersionStore(counterController, guiController);
	}

	public CmVersionStore(ICounterController counterController, IGuiController guiController)
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
//	private static final String  PROP_PREFIX                       = CM_NAME;

	public static final String GRAPH_NAME_VERSION_STORE_USAGE    = "VsUsage";

	private void addTrendGraphs()
	{
		//-----
		addTrendGraph(GRAPH_NAME_VERSION_STORE_USAGE,
			"Version Store Usage by DBName, in MB", // Menu CheckBox text
			"Version Store Usage by DBName, in MB ("+GROUP_NAME+"->"+SHORT_NAME+")", // Label 
			TrendGraphDataPoint.createGraphProps(TrendGraphDataPoint.Y_AXIS_SCALE_LABELS_MB, CentralPersistReader.SampleType.MAX_OVER_SAMPLES),
			null, 
			LabelType.Dynamic,
			TrendGraphDataPoint.Category.SPACE,
			false,  // is Percent Graph
			false,  // visible at start
			Ver.ver(2016,0,0, 2),     // 2016 SP2 -- graph is valid from Server Version. 0 = All Versions; >0 = Valid from this version and above 
			-1);   // minimum height
	}

	@Override
	public void updateGraphData(TrendGraphDataPoint tgdp)
	{
		// -----------------------------------------------------------------------------------------
		if (GRAPH_NAME_VERSION_STORE_USAGE.equals(tgdp.getName()))
		{
			Double[] dArray = new Double[this.size()];
			String[] lArray = new String[dArray.length];
			for (int i = 0; i < dArray.length; i++)
			{
				lArray[i] = this.getAbsString       (i, "database_name");
				dArray[i] = this.getAbsValueAsDouble(i, "reserved_space_mb");
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
	}



//	@Override
//	protected TabularCntrPanel createGui()
//	{
//		return new CmVersionStorePanel(this);
//	}

	@Override
	public void addMonTableDictForVersion(DbxConnection conn, DbmsVersionInfo versionInfo)
	{
		try 
		{
			MonTablesDictionary mtd = MonTablesDictionaryManager.getInstance();
			mtd.addTable("dm_tran_version_store_space_usage",  "Returns a table that displays total space in tempdb used by version store records for each database. sys.dm_tran_version_store_space_usage is efficient and not expensive to run, as it does not navigate through individual version store records, and returns aggregated version store space consumed in tempdb per database.");

			mtd.addColumn("dm_tran_version_store_space_usage",  "database_id"             , "<html>Just the database ID</html>");
			mtd.addColumn("dm_tran_version_store_space_usage",  "database_name"           , "<html>Name of the database</html>");
			mtd.addColumn("dm_tran_version_store_space_usage",  "reserved_space_mb"       , "<html>How many MB are we using.<br><b>Algorithm</b>: reserved_space_kb / 1024.0</html>");
			mtd.addColumn("dm_tran_version_store_space_usage",  "reserved_space_kb"       , "<html>How many KB are we using.</html>");
			mtd.addColumn("dm_tran_version_store_space_usage",  "reserved_page_count"     , "<html>How many PAGES are we using.</html>");
			mtd.addColumn("dm_tran_version_store_space_usage",  "reserved_page_count_diff", "<html>How many PAGES are we using.</html>");
		}
		catch (NameNotFoundException e) 
		{
		//	_logger.warn("Problems in cm='" + CM_NAME + "', adding addMonTableDictForVersion. Caught: " + e); 
			System.out.println("Problems in cm='" + CM_NAME + "', adding addMonTableDictForVersion. Caught: " + e); 
		}
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

		pkCols.add("database_name");
		
		return pkCols;
	}

	@Override
	public String getSqlForVersion(DbxConnection conn, DbmsVersionInfo versionInfo)
	{
		String sql = ""
			    + "select /* ${cmCollectorName} */ \n"
			    + "     database_id \n"
			    + "    ,database_name     = db_name(database_id) \n"
			    + "    ,reserved_space_mb = convert(numeric(12,1), reserved_space_kb*1.0 / 1024.0) \n"
			    + "    ,reserved_space_kb \n"
			    + "    ,reserved_page_count \n"
			    + "    ,reserved_page_count_diff = reserved_page_count \n"
			    + "from sys.dm_tran_version_store_space_usage \n"
			    + "";

		return sql;
	}

	@Override
	public Map<String, AggregationType> createAggregateColumns()
	{
		HashMap<String, AggregationType> aggColumns = new HashMap<>(getColumnCount());

		AggregationType tmp;
		
		// Create the columns :::::::::::::::::::::::::::::::::::::::::::::::::::::: And ADD it to the return Map 
		tmp = new AggregationType("reserved_space_mb"       , AggregationType.Agg.SUM);   aggColumns.put(tmp.getColumnName(), tmp);
		tmp = new AggregationType("reserved_space_kb"       , AggregationType.Agg.SUM);   aggColumns.put(tmp.getColumnName(), tmp);
		tmp = new AggregationType("reserved_page_count"     , AggregationType.Agg.SUM);   aggColumns.put(tmp.getColumnName(), tmp);
		tmp = new AggregationType("reserved_page_count_diff", AggregationType.Agg.SUM);   aggColumns.put(tmp.getColumnName(), tmp);

		return aggColumns;
	}

	@Override
	public Object calculateAggregateRow_nonAggregatedColumnDataProvider(CounterSample newSample, String colName, int c, int jdbcType, Object addValue)
	{
		if ("database_id".equalsIgnoreCase(colName))
			return new Long(-1);
		
		return null;
	}
}
