package com.asetune.cm.os;

import com.asetune.ICounterController;
import com.asetune.IGuiController;
import com.asetune.cm.CounterModelHostMonitor;
import com.asetune.cm.CounterSetTemplates;
import com.asetune.cm.CounterSetTemplates.Type;
import com.asetune.cm.CountersModel;
import com.asetune.cm.os.gui.CmOsUptimePanel;
import com.asetune.graph.TrendGraphDataPoint;
import com.asetune.gui.MainFrame;
import com.asetune.gui.TabularCntrPanel;
import com.asetune.gui.TrendGraph;

public class CmOsUptime
extends CounterModelHostMonitor
{
//	private static Logger        _logger          = Logger.getLogger(CmOsUptime.class);
	private static final long    serialVersionUID = 1L;

	public static final int      CM_TYPE          = CounterModelHostMonitor.HOSTMON_UPTIME;
	public static final String   CM_NAME          = CmOsUptime.class.getSimpleName();
	public static final String   SHORT_NAME       = "OS Load Average";
	public static final String   HTML_DESC        = 
		"<html>" +
		"Executes: 'uptime' on the Operating System" +
		"</html>";

	public static final String   GROUP_NAME       = MainFrame.TCP_GROUP_HOST_MONITOR;
	public static final String   GUI_ICON_FILE    = "images/"+CM_NAME+".png";

//	@Override public int     getDefaultPostponeTime()                 { return DEFAULT_POSTPONE_TIME; }
//	@Override public int     getDefaultQueryTimeout()                 { return DEFAULT_QUERY_TIMEOUT; }
//	@Override public boolean getDefaultIsNegativeDiffCountersToZero() { return NEGATIVE_DIFF_COUNTERS_TO_ZERO; }
	@Override public Type    getTemplateLevel()                       { return Type.LARGE; }

	/**
	 * FACTORY  method to create the object
	 */
	public static CountersModel create(ICounterController counterController, IGuiController guiController)
	{
		if (guiController != null && guiController.hasGUI())
			guiController.splashWindowProgress("Loading: Counter Model '"+CM_NAME+"'");

		return new CmOsUptime(counterController, guiController);
	}

	public CmOsUptime(ICounterController counterController, IGuiController guiController)
	{
		super(CM_NAME, GROUP_NAME, CM_TYPE, null, true);

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
	public static final String GRAPH_NAME_LOAD_AVERAGE = "LoadAverage";

	private void addTrendGraphs()
	{
		String[] labels_1  = new String[] { "loadAverage_1Min" };
		
		addTrendGraphData(GRAPH_NAME_LOAD_AVERAGE, new TrendGraphDataPoint(GRAPH_NAME_LOAD_AVERAGE, labels_1));

		// if GUI
		if (getGuiController() != null && getGuiController().hasGUI())
		{
			// GRAPH
			TrendGraph tg = null;
			tg = new TrendGraph(GRAPH_NAME_LOAD_AVERAGE,
				"Host Monitoring - Load Average", 	               // Menu CheckBox text
				"Host Monitoring - Load Average",                  // Label 
				labels_1, 
				false, // is Percent Graph
				this, 
				false, // visible at start
				0,     // graph is valid from Server Version. 0 = All Versions; >0 = Valid from this version and above 
				-1);  // minimum height
			addTrendGraph(tg.getName(), tg, true);
		}
	}
	
	@Override
	protected TabularCntrPanel createGui()
	{
		return new CmOsUptimePanel(this);
	}

	@Override
	public void updateGraphData(TrendGraphDataPoint tgdp)
	{
		if (GRAPH_NAME_LOAD_AVERAGE.equals(tgdp.getName()))
		{
			Double[] arr = new Double[1];

			// NOTE: only ABS values are present in CounterModelHostMonitor
			arr[0] = this.getAbsValueAvg("loadAverage_1Min");

//System.out.println(GRAPH_NAME_LOAD_AVERAGE + ": arr[0] = " + arr[0]);
			
//			if (_logger.isDebugEnabled())
//				_logger.debug("updateGraphData(OsUptime): loadAverage_1Min='"+arr[0]+"'.");

			// Set the values
			tgdp.setDate(this.getTimestamp());
			tgdp.setData(arr);
		}
	}
}
