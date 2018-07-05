package com.asetune.cm.ase;

import java.sql.Connection;
import java.util.LinkedList;
import java.util.List;

import org.apache.log4j.Logger;

import com.asetune.ICounterController;
import com.asetune.IGuiController;
import com.asetune.cm.CounterSetTemplates;
import com.asetune.cm.CounterSetTemplates.Type;
import com.asetune.cm.CountersModel;
import com.asetune.graph.TrendGraphDataPoint;
import com.asetune.graph.TrendGraphDataPoint.LabelType;
import com.asetune.gui.MainFrame;

/**
 * @author Goran Schwarz (goran_schwarz@hotmail.com)
 */
public class CmWorkerThread
extends CountersModel
{
	private static Logger        _logger          = Logger.getLogger(CmWorkerThread.class);
	private static final long    serialVersionUID = 1L;

	public static final String   CM_NAME          = CmWorkerThread.class.getSimpleName();
	public static final String   SHORT_NAME       = "Workers";
	public static final String   HTML_DESC        = 
		"<html>" +
		"Some information about Parallel Worker Threads <br>" +
		"</html>";

	public static final String   GROUP_NAME       = MainFrame.TCP_GROUP_SERVER;
	public static final String   GUI_ICON_FILE    = "images/"+CM_NAME+".png";

	public static final int      NEED_SRV_VERSION = 0;
	public static final int      NEED_CE_VERSION  = 0;

	public static final String[] MON_TABLES       = new String[] {"monSysWorkerThread"};
	public static final String[] NEED_ROLES       = new String[] {"mon_role"};
	public static final String[] NEED_CONFIG      = new String[] {"enable monitoring=1"};

	public static final String[] PCT_COLUMNS      = new String[] {};
	public static final String[] DIFF_COLUMNS     = new String[] {"ParallelQueries", "PlansAltered"};

	public static final boolean  NEGATIVE_DIFF_COUNTERS_TO_ZERO = true;
	public static final boolean  IS_SYSTEM_CM                   = true;
	public static final int      DEFAULT_POSTPONE_TIME          = 0;
	public static final int      DEFAULT_QUERY_TIMEOUT          = CountersModel.DEFAULT_sqlQueryTimeout;

	@Override public int     getDefaultPostponeTime()                 { return DEFAULT_POSTPONE_TIME; }
	@Override public int     getDefaultQueryTimeout()                 { return DEFAULT_QUERY_TIMEOUT; }
	@Override public boolean getDefaultIsNegativeDiffCountersToZero() { return NEGATIVE_DIFF_COUNTERS_TO_ZERO; }
	@Override public Type    getTemplateLevel()                       { return Type.SMALL; }

	/**
	 * FACTORY  method to create the object
	 */
	public static CountersModel create(ICounterController counterController, IGuiController guiController)
	{
		if (guiController != null && guiController.hasGUI())
			guiController.splashWindowProgress("Loading: Counter Model '"+CM_NAME+"'");

		return new CmWorkerThread(counterController, guiController);
	}

	public CmWorkerThread(ICounterController counterController, IGuiController guiController)
	{
		super(counterController,
				CM_NAME, GROUP_NAME, /*sql*/null, /*pkList*/null, 
				DIFF_COLUMNS, PCT_COLUMNS, MON_TABLES, 
				NEED_ROLES, NEED_CONFIG, NEED_SRV_VERSION, NEED_CE_VERSION, 
				NEGATIVE_DIFF_COUNTERS_TO_ZERO, IS_SYSTEM_CM, DEFAULT_POSTPONE_TIME);

		setDisplayName(SHORT_NAME);
		setDescription(HTML_DESC);

		setIconFile(GUI_ICON_FILE);

		setCounterController(counterController);
		setGuiController(guiController);

		addTrendGraphs();
		
		CounterSetTemplates.register(this);
	}


	//------------------------------------------------------------
	// Implementation
	//------------------------------------------------------------
	
	public static final String GRAPH_NAME_ACTIVE_THREADS = "ActiveThreads";

	private void addTrendGraphs()
	{
//		String[] activeThreadsLabels = new String[] { "ThreadsActive", "ParallelQueries" };
//		
//		addTrendGraphData(GRAPH_NAME_ACTIVE_THREADS, new TrendGraphDataPoint(GRAPH_NAME_ACTIVE_THREADS, activeThreadsLabels, LabelType.Static));

		addTrendGraph(GRAPH_NAME_ACTIVE_THREADS,
			"Worker Threads in Use ", 	                                  // Menu CheckBox text
			"Worker Threads in Use & Parallel Queries per second ("+GROUP_NAME+"->"+SHORT_NAME+")", // Label 
			new String[] { "ThreadsActive", "ParallelQueries" }, 
			LabelType.Static,
			TrendGraphDataPoint.Category.CPU,
			false, // is Percent Graph
			false, // visible at start
			0,     // graph is valid from Server Version. 0 = All Versions; >0 = Valid from this version and above 
			-1);   // minimum height

//		// if GUI
//		if (getGuiController() != null && getGuiController().hasGUI())
//		{
//			// GRAPH
//			TrendGraph tg = null;
//			tg = new TrendGraph(GRAPH_NAME_ACTIVE_THREADS,
//				"Worker Threads in Use ", 	                                  // Menu CheckBox text
//				"Worker Threads in Use & Parallel Queries per second ("+GROUP_NAME+"->"+SHORT_NAME+")", // Label 
//				activeThreadsLabels, 
//				false, // is Percent Graph
//				this, 
//				false, // visible at start
//				0,     // graph is valid from Server Version. 0 = All Versions; >0 = Valid from this version and above 
//				-1);   // minimum height
//			addTrendGraph(tg.getName(), tg, true);
//		}
	}

//	@Override
//	protected TabularCntrPanel createGui()
//	{
//		return new CmWorkerThreadPanel(this);
//	}

	@Override
	public String[] getDependsOnConfigForVersion(Connection conn, int srvVersion, boolean isClusterEnabled)
	{
		return NEED_CONFIG;
	}

	@Override
	public List<String> getPkForVersion(Connection conn, int srvVersion, boolean isClusterEnabled)
	{
		List <String> pkCols = new LinkedList<String>();

		if (isClusterEnabled)
			pkCols.add("InstanceID");

		// NONE, when not Cluster Edition

		return pkCols;
	}

	@Override
	public String getSqlForVersion(Connection conn, int aseVersion, boolean isClusterEnabled)
	{
		return "select * from master..monSysWorkerThread";
	}

	@Override
	public void updateGraphData(TrendGraphDataPoint tgdp)
	{
		if (GRAPH_NAME_ACTIVE_THREADS.equals(tgdp.getName()))
		{
			Double[] arr = new Double[2];

			arr[0] = this.getAbsValueSum("ThreadsActive");
			arr[1] = this.getRateValueSum("ParallelQueries");
			
			if (_logger.isDebugEnabled())
				_logger.debug("updateGraphData("+tgdp.getName()+"): ThreadsActive='"+arr[0]+"', ParallelQueries='"+arr[1]+"'.");

			// Set the values
			tgdp.setDataPoint(this.getTimestamp(), arr);
		}
	}
}
