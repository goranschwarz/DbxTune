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
public class CmProcCacheLoad
extends CountersModel
{
	private static Logger        _logger          = Logger.getLogger(CmProcCacheLoad.class);
	private static final long    serialVersionUID = 1L;

	public static final String   CM_NAME          = CmProcCacheLoad.class.getSimpleName();
	public static final String   SHORT_NAME       = "Procedure Cache Load";
	public static final String   HTML_DESC        = 
		"<html>" +
		"This is just a short one to see if we have 'stored procedure' swapping for some reason. <br>" +
		"The reason (like in sp_sysmon) is for the displayed here. <br>" +
		"</html>";

	public static final String   GROUP_NAME       = MainFrame.TCP_GROUP_CACHE;
	public static final String   GUI_ICON_FILE    = "images/"+CM_NAME+".png";

	public static final int      NEED_SRV_VERSION = 0;
	public static final int      NEED_CE_VERSION  = 0;

	public static final String[] MON_TABLES       = new String[] {"monProcedureCache"};
	public static final String[] NEED_ROLES       = new String[] {"mon_role"};
	public static final String[] NEED_CONFIG      = new String[] {"enable monitoring=1"};

	public static final String[] PCT_COLUMNS      = new String[] {};
	public static final String[] DIFF_COLUMNS     = new String[] {"Requests", "Loads", "Writes", "Stalls"};

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

		return new CmProcCacheLoad(counterController, guiController);
	}

	public CmProcCacheLoad(ICounterController counterController, IGuiController guiController)
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
	
	public static final String GRAPH_NAME_REQUEST_PER_SEC = "ProcCacheGraph"; //String x=GetCounters.CM_GRAPH_NAME__PROC_CACHE_LOAD__REQUEST_PER_SEC;

	private void addTrendGraphs()
	{
//		String[] sumLabels = new String[] { "Requests", "Loads" };
//		
//		addTrendGraphData(GRAPH_NAME_REQUEST_PER_SEC, new TrendGraphDataPoint(GRAPH_NAME_REQUEST_PER_SEC, sumLabels, LabelType.Static));

		addTrendGraph(GRAPH_NAME_REQUEST_PER_SEC,
			"Procedure Cache Requests", 	                                  // Menu CheckBox text
			"Number of Procedure Requests per Second (procs,triggers,views) ("+GROUP_NAME+"->"+SHORT_NAME+")", // Label 
			new String[] { "Requests", "Loads" }, 
			LabelType.Static,
			TrendGraphDataPoint.Category.CACHE,
			false, // is Percent Graph
			false, // visible at start
			0,     // graph is valid from Server Version. 0 = All Versions; >0 = Valid from this version and above 
			-1);   // minimum height

//		// if GUI
//		if (getGuiController() != null && getGuiController().hasGUI())
//		{
//			// GRAPH
//			TrendGraph tg = null;
//			tg = new TrendGraph(GRAPH_NAME_REQUEST_PER_SEC,
//				"Procedure Cache Requests", 	                                  // Menu CheckBox text
//				"Number of Procedure Requests per Second (procs,triggers,views) ("+GROUP_NAME+"->"+SHORT_NAME+")", // Label 
//				sumLabels, 
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
//		return new CmProcCacheLoadPanel(this);
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
		String cols1, cols2, cols3;
		cols1 = cols2 = cols3 = "";

		cols2 += "Requests, Loads, Writes, Stalls";
		if (isClusterEnabled)
		{
			cols1 = "InstanceID, ";
		}

		String sql = 
			"select " + cols1 + cols2 + cols3 + "\n" +
			"from master..monProcedureCache \n";

		return sql;
	}

	@Override
	public void updateGraphData(TrendGraphDataPoint tgdp)
	{
		if (GRAPH_NAME_REQUEST_PER_SEC.equals(tgdp.getName()))
		{
			Double[] arr = new Double[2];

			arr[0] = this.getRateValueSum("Requests");
			arr[1] = this.getRateValueSum("Loads");
			_logger.debug("updateGraphData(ProcCacheGraph): Requests='"+arr[0]+"', Loads='"+arr[1]+"'.");

			// Set the values
			tgdp.setDataPoint(this.getTimestamp(), arr);
		}
	}
}
