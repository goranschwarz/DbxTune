package com.asetune.cm.ase;

import java.sql.Connection;
import java.util.LinkedList;
import java.util.List;

import javax.swing.JCheckBoxMenuItem;

import org.apache.log4j.Logger;

import com.asetune.ICounterController;
import com.asetune.IGuiController;
import com.asetune.TrendGraphDataPoint;
import com.asetune.cm.CounterSetTemplates;
import com.asetune.cm.CounterSetTemplates.Type;
import com.asetune.cm.CountersModel;
import com.asetune.gui.MainFrame;
import com.asetune.gui.TrendGraph;

/**
 * @author Goran Schwarz (goran_schwarz@hotmail.com)
 */
public class CmSysLoad
extends CountersModel
{
	private static Logger        _logger          = Logger.getLogger(CmSysLoad.class);
	private static final long    serialVersionUID = 1L;

	public static final String   CM_NAME          = CmSysLoad.class.getSimpleName();
	public static final String   SHORT_NAME       = "System Load";
	public static final String   HTML_DESC        = 
		"<html>" +
        "The SYSTEM load.<br>" +
        "Here you can see the balancing between engines<br>" +
        "Check 'run queue length', which is almost the same as 'load average' on Unix systems.<br>" +
        "Meaning: How many threads/SPIDs are currently waiting on the run queue before they could be scheduled for execution." +
		"</html>";

	public static final String   GROUP_NAME       = MainFrame.TCP_GROUP_SERVER;
	public static final String   GUI_ICON_FILE    = "images/"+CM_NAME+".png";

	public static final int      NEED_SRV_VERSION = 15500;
	public static final int      NEED_CE_VERSION  = 15020;

	public static final String[] MON_TABLES       = new String[] {"monSysLoad"};
	public static final String[] NEED_ROLES       = new String[] {"sa_role"};
	public static final String[] NEED_CONFIG      = new String[] {};

	public static final String[] PCT_COLUMNS      = new String[] {};
	public static final String[] DIFF_COLUMNS     = new String[] {};

	public static final boolean  NEGATIVE_DIFF_COUNTERS_TO_ZERO = false;
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
			guiController.splashWindowProgress("Loading: Counter Model '"+CM_NAME+"'");

		return new CmSysLoad(counterController, guiController);
	}

	public CmSysLoad(ICounterController counterController, IGuiController guiController)
	{
		super(CM_NAME, GROUP_NAME, /*sql*/null, /*pkList*/null, 
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
	
	public static final String   GRAPH_NAME_AVG_RUN_QUEUE_LENTH    = "AvgRunQLengthGraph";    //String x=GetCounters.CM_GRAPH_NAME__SYS_LOAD__AVG_RUN_QUEUE_LENTH;
	public static final String   GRAPH_NAME_ENGINE_RUN_QUEUE_LENTH = "EngineRunQLengthGraph"; //String x=GetCounters.CM_GRAPH_NAME__SYS_LOAD__ENGINE_RUN_QUEUE_LENTH;

	private void addTrendGraphs()
	{
		String[] avgLabels = new String[] { "Now", "Avg last 1 minute", "Avg last 5 minute", "Max last 1 minute", "Max last 5 minute" };
		String[] engLabels = new String[] { "-runtime-replaced-" };
		
		addTrendGraphData(GRAPH_NAME_AVG_RUN_QUEUE_LENTH,    new TrendGraphDataPoint(GRAPH_NAME_AVG_RUN_QUEUE_LENTH,    avgLabels));
		addTrendGraphData(GRAPH_NAME_ENGINE_RUN_QUEUE_LENTH, new TrendGraphDataPoint(GRAPH_NAME_ENGINE_RUN_QUEUE_LENTH, engLabels));

		// if GUI
		if (getGuiController() != null && getGuiController().hasGUI())
		{
			// GRAPH
			TrendGraph tg = null;
			tg = new TrendGraph(GRAPH_NAME_AVG_RUN_QUEUE_LENTH,
				"Run Queue Length, Server Wide", 	                                    // Menu CheckBox text
				"Run Queue Length, Average for all instances (only in 15.5 and later)", // Label 
				avgLabels, 
				false, // is Percent Graph
				this, 
				false, // visible at start
				-1);  // minimum height
			addTrendGraph(tg.getName(), tg, true);

			tg = new TrendGraph(GRAPH_NAME_ENGINE_RUN_QUEUE_LENTH,
				"Run Queue Length, Per Engine", 	                                               // Menu CheckBox text
				"Run Queue Length, Average over last minute, Per Engine (only in 15.5 and later)", // Label 
				engLabels, 
				false, // is Percent Graph
				this, 
				false, // visible at start
				-1);  // minimum height
			addTrendGraph(tg.getName(), tg, true);
		}
	}

//	@Override
//	protected TabularCntrPanel createGui()
//	{
//		return new CmSysLoadPanel(this);
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

		pkCols.add("StatisticID");
		pkCols.add("EngineNumber");

		return pkCols;
	}

	@Override
	public String getSqlForVersion(Connection conn, int aseVersion, boolean isClusterEnabled)
	{
		String cols1 = "";
		if (isClusterEnabled)
		{
			cols1 += "InstanceID, ";
		}

		String sql = 
			"select "+cols1+"StatisticID, Statistic, EngineNumber, \n" +
			"       Sample, \n" +
			"       Avg_1min, Avg_5min, Avg_15min, " +
			"       SteadyState, \n" +
			"       Peak_Time, Peak, \n" +
			"       Max_1min_Time,  Max_1min, \n" + // try add a SQL that shows number of minutes ago this happened, "Days-HH:MM:SS
			"       Max_5min_Time,  Max_5min, \n" +
			"       Max_15min_Time, Max_15min \n" +
			"from master..monSysLoad \n";
		
		// in ASE 15.7, we get problems if we do the order by
		// com.sybase.jdbc3.jdbc.SybSQLException: Domain error occurred.
		if (aseVersion < 15700)
			sql += "order by StatisticID, EngineNumber" + (isClusterEnabled ? ", InstanceID" : "");

		return sql;
	}

	@Override
	public void updateGraphData(TrendGraphDataPoint tgdp)
	{
		int aseVersion = getServerVersion();

		if (GRAPH_NAME_AVG_RUN_QUEUE_LENTH.equals(tgdp.getName()))
		{
			if (aseVersion < 15500)
			{
				// disable the graph checkbox...
				TrendGraph tg = getTrendGraph(GRAPH_NAME_AVG_RUN_QUEUE_LENTH);
				if (tg != null)
				{
					JCheckBoxMenuItem menuItem = tg.getViewMenuItem();
					if (menuItem.isSelected())
						menuItem.doClick();
				}
			}
			else
			{
				int[] rqRows = this.getAbsRowIdsWhere("Statistic", "run queue length");
				if (rqRows == null)
					_logger.warn("When updateGraphData for '"+tgdp.getName()+"', getAbsRowIdsWhere('Statistic', 'run queue length'), retuned null, so I can't do more here.");
				else
				{
					Double[] arr = new Double[5];
					arr[0] = this.getAbsValueAvg(rqRows, "Sample");
					arr[1] = this.getAbsValueAvg(rqRows, "Avg_1min");
					arr[2] = this.getAbsValueAvg(rqRows, "Avg_5min");
					arr[3] = this.getAbsValueAvg(rqRows, "Max_1min");
					arr[4] = this.getAbsValueAvg(rqRows, "Max_5min");
					_logger.debug("updateGraphData(AvgRunQLengthGraph): Sample='"+arr[0]+"', Avg_1min='"+arr[1]+"', Avg_5min='"+arr[2]+"', Max_1min='"+arr[3]+"', Max_5min='"+arr[4]+"'.");

					// Set the values
					tgdp.setDate(this.getTimestamp());
					tgdp.setData(arr);
				}
			}
		} // end: GRAPH_NAME_AVG_RUN_QUEUE_LENTH

		if (GRAPH_NAME_ENGINE_RUN_QUEUE_LENTH.equals(tgdp.getName()))
		{
			if (aseVersion < 15500)
			{
				// disable the graph checkbox...
				TrendGraph tg = getTrendGraph(GRAPH_NAME_ENGINE_RUN_QUEUE_LENTH);
				if (tg != null)
				{
					JCheckBoxMenuItem menuItem = tg.getViewMenuItem();
					if (menuItem.isSelected())
						menuItem.doClick();
				}
			}
			else
			{
				// Get a array of rowId's where the column 'Statistic' has the value 'run queue length'
				int[] rqRows = this.getAbsRowIdsWhere("Statistic", "run queue length");
				if (rqRows == null)
					_logger.warn("When updateGraphData for '"+tgdp.getName()+"', getAbsRowIdsWhere('Statistic', 'run queue length'), retuned null, so I can't do more here.");
				else
				{
					Double[] data  = new Double[rqRows.length];
					String[] label = new String[rqRows.length];
					for (int i=0; i<rqRows.length; i++)
					{
						int rowId = rqRows[i];

						// get LABEL
						String instanceId   = null;
						if (isClusterEnabled())
							instanceId = this.getAbsString(rowId, "InstanceID");
						String engineNumber = this.getAbsString(rowId, "EngineNumber");

						// in Cluster Edition the labels will look like 'eng-{InstanceId}:{EngineNumber}'
						// in ASE SMP Version the labels will look like 'eng-{EngineNumber}'
						if (instanceId == null)
							label[i] = "eng-" + engineNumber;
						else
							label[i] = "eng-" + instanceId + ":" + engineNumber;

						// get DATA
						data[i]  = this.getAbsValueAsDouble(rowId, "Avg_1min");
					}
					if (_logger.isDebugEnabled())
					{
						String debugStr = "";
						for (int i=0; i<data.length; i++)
							debugStr += label[i] + "='"+data[i]+"', ";
						_logger.debug("updateGraphData(EngineRunQLengthGraph): "+debugStr);
					}

					// Set the values
					tgdp.setDate(this.getTimestamp());
					tgdp.setLabel(label);
					tgdp.setData(data);
				}
			}
		} // end: GRAPH_NAME_ENGINE_RUN_QUEUE_LENTH
	}
}