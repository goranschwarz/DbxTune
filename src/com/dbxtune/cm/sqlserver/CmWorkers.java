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
import com.dbxtune.gui.TabularCntrPanel;
import com.dbxtune.sql.conn.DbxConnection;
import com.dbxtune.sql.conn.info.DbmsVersionInfo;
import com.dbxtune.sql.conn.info.DbmsVersionInfoSqlServer;
import com.dbxtune.utils.Ver;

/**
 * @author Goran Schwarz (goran_schwarz@hotmail.com)
 */
public class CmWorkers
extends CountersModel
{
	private static final Logger _logger = LogManager.getLogger(MethodHandles.lookup().lookupClass());
	private static final long    serialVersionUID = 1L;

	public static final String   CM_NAME          = CmWorkers.class.getSimpleName();
	public static final String   SHORT_NAME       = "Workers";
	public static final String   HTML_DESC        = 
		"<html>" +
		"<p>FIXME</p>" +
		"</html>";

	public static final String   GROUP_NAME       = MainFrame.TCP_GROUP_SERVER;
	public static final String   GUI_ICON_FILE    = "images/"+CM_NAME+".png";

	public static final long     NEED_SRV_VERSION = Ver.ver(2016);
	public static final long     NEED_CE_VERSION  = 0;

	public static final String[] MON_TABLES       = new String[] {"dm_exec_query_parallel_workers"};
	public static final String[] NEED_ROLES       = new String[] {"VIEW SERVER STATE"};
	public static final String[] NEED_CONFIG      = new String[] {};

	public static final String[] PCT_COLUMNS      = new String[] {};
	public static final String[] DIFF_COLUMNS     = new String[] {
		"used_worker_count_diff"
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

		return new CmWorkers(counterController, guiController);
	}

	public CmWorkers(ICounterController counterController, IGuiController guiController)
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
	public static final String GRAPH_NAME_WORKERS_COUNT = "WorkersCount";
	
	private void addTrendGraphs()
	{
		addTrendGraph(GRAPH_NAME_WORKERS_COUNT,
				"Workers Free, in Use & Parallel Queries", 	                                // Menu CheckBox text
				"Workers Free, in Use & Parallel Queries ("+GROUP_NAME+"->"+SHORT_NAME+")", // Label 
				TrendGraphDataPoint.createGraphProps(TrendGraphDataPoint.Y_AXIS_SCALE_LABELS_NORMAL, CentralPersistReader.SampleType.MAX_OVER_SAMPLES),
				new String[] { "max_worker_count", "free_worker_count", "reserved_worker_count", "used_worker_count" }, 
				LabelType.Static,
				TrendGraphDataPoint.Category.CPU,
				false, // is Percent Graph
				false, // visible at start
				0,     // graph is valid from Server Version. 0 = All Versions; >0 = Valid from this version and above 
				-1);   // minimum height
	}

//	@Override
//	protected TabularCntrPanel createGui()
//	{
//		return new CmRaSysmonPanel(this);
//	}

	@Override
	public boolean checkDependsOnVersion(DbxConnection conn)
	{
		DbmsVersionInfoSqlServer versionInfo = (DbmsVersionInfoSqlServer) conn.getDbmsVersionInfo();
		if (versionInfo.isAzureDb() || versionInfo.isAzureSynapseAnalytics())
		{
			_logger.warn("When trying to initialize Counters Model '" + getName() + "', named '"+getDisplayName() + "', connected to Azure SQL Database or Analytics, which do NOT support this.");

			setActive(false, "This info is NOT available in Azure SQL Database or Azure Synapse/Analytics.");

			TabularCntrPanel tcp = getTabPanel();
			if (tcp != null)
			{
				tcp.setToolTipText("This info is NOT available in Azure SQL Database or Azure Synapse/Analytics.");
			}
			return false;
		}

		return super.checkDependsOnVersion(conn);
	}
	
	@Override
	public void addMonTableDictForVersion(DbxConnection conn, DbmsVersionInfo versionInfo)
	{
		try 
		{
			MonTablesDictionary mtd = MonTablesDictionaryManager.getInstance();
			mtd.addTable("dm_exec_query_parallel_workers",  "worker availability information per node.");

			mtd.addColumn("dm_exec_query_parallel_workers", "node_id",                "<html>NUMA node ID.</html>");
			mtd.addColumn("dm_exec_query_parallel_workers", "scheduler_count",        "<html>Number of schedulers on this node.</html>");
			mtd.addColumn("dm_exec_query_parallel_workers", "max_worker_count",       "<html>Maximum number of workers for parallel queries.</html>");
			mtd.addColumn("dm_exec_query_parallel_workers", "reserved_worker_count",  "<html>Number of workers reserved by parallel queries, plus number of main workers used by all requests.</html>");
			mtd.addColumn("dm_exec_query_parallel_workers", "free_worker_count",      "<html>Number of workers available for tasks.<br><b>Note:</b> every incoming request consumes at least 1 worker, which is subtracted from the free worker count. It is possible that the free worker count can be a negative number on a heavily loaded server.</html>");
			mtd.addColumn("dm_exec_query_parallel_workers", "used_worker_count",      "<html>Number of workers used by parallel queries.</html>");
			mtd.addColumn("dm_exec_query_parallel_workers", "used_worker_count_diff", "<html>Same as 'used_worker_count', but difference calculated.</html>");
		}
		catch (NameNotFoundException e) 
		{
			_logger.warn("Problems in cm='" + CM_NAME + "', adding addMonTableDictForVersion. Caught: " + e); 
		//	System.out.println("Problems in cm='" + CM_NAME + "', adding addMonTableDictForVersion. Caught: " + e); 
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

		pkCols.add("node_id");

		return pkCols;
	}

	@Override
	public String getSqlForVersion(DbxConnection conn, DbmsVersionInfo versionInfo)
	{
		DbmsVersionInfoSqlServer ssVersionInfo = (DbmsVersionInfoSqlServer) versionInfo;

		String dm_exec_query_optimizer_info = "dm_exec_query_parallel_workers";
		
		if (ssVersionInfo.isAzureSynapseAnalytics())
			dm_exec_query_optimizer_info = "dm_exec_query_parallel_workers";
		
		String sql = "select /* ${cmCollectorName} */ \n"
				+ "     node_id \n"
				+ "    ,scheduler_count \n"
				+ "    ,max_worker_count \n"
				+ "    ,reserved_worker_count \n"
				+ "    ,free_worker_count \n"
				+ "    ,used_worker_count \n"
				+ "    ,used_worker_count_diff = used_worker_count \n"
				+ " from sys." + dm_exec_query_optimizer_info + " \n"
				+ "";

		return sql;
	}

	@Override
	public void updateGraphData(TrendGraphDataPoint tgdp)
	{
		if (GRAPH_NAME_WORKERS_COUNT.equals(tgdp.getName()))
		{
			Double[] arr = new Double[4];

			arr[0] = this.getAbsValueSum("max_worker_count");
			arr[1] = this.getAbsValueSum("free_worker_count");
			arr[2] = this.getAbsValueSum("reserved_worker_count");
			arr[3] = this.getRateValueSum("used_worker_count");
			
//			if (_logger.isDebugEnabled())
//				_logger.debug("updateGraphData("+tgdp.getName()+"): ThreadsActive='"+arr[0]+"', ParallelQueries='"+arr[1]+"'.");

			// Set the values
			tgdp.setDataPoint(this.getTimestamp(), arr);
		}
	}
	
	// NOTE: Should we create an alarm if we start to be "low" on 'free_worker_count' 
}
