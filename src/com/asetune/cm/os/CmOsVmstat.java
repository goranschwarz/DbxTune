package com.asetune.cm.os;

import com.asetune.ICounterController;
import com.asetune.IGuiController;
import com.asetune.TrendGraphDataPoint;
import com.asetune.cm.CounterModelHostMonitor;
import com.asetune.cm.CounterSetTemplates;
import com.asetune.cm.CounterSetTemplates.Type;
import com.asetune.cm.CountersModel;
import com.asetune.cm.os.gui.CmOsVmstatPanel;
import com.asetune.gui.MainFrame;
import com.asetune.gui.TabularCntrPanel;
import com.asetune.gui.TrendGraph;

public class CmOsVmstat
extends CounterModelHostMonitor
{
//	private static Logger        _logger          = Logger.getLogger(CmOsVmstat.class);
	private static final long    serialVersionUID = 1L;

	public static final int      CM_TYPE          = CounterModelHostMonitor.HOSTMON_VMSTAT;
	public static final String   CM_NAME          = CmOsVmstat.class.getSimpleName();
	public static final String   SHORT_NAME       = "OS CPU(vmstat)";
	public static final String   HTML_DESC        = 
		"<html>" +
		"Executes: 'vmstat' on the Operating System" +
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

		return new CmOsVmstat(counterController, guiController);
	}

	public CmOsVmstat(ICounterController counterController, IGuiController guiController)
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
	public static final String GRAPH_NAME_CPU_USAGE = "CpuUsage";

	private void addTrendGraphs()
	{
//		String[] labelsCpu  = new String[] { "cpu_sy+cpu_us", "cpu_sy", "cpu_us", "cpu_id" };
		String[] labelsCpu  = new String[] { "-runtime-replaced-" };
		
		addTrendGraphData(GRAPH_NAME_CPU_USAGE, new TrendGraphDataPoint(GRAPH_NAME_CPU_USAGE, labelsCpu));

		// if GUI
		if (getGuiController() != null && getGuiController().hasGUI())
		{
			// GRAPH
			TrendGraph tg = null;
			tg = new TrendGraph(GRAPH_NAME_CPU_USAGE,
				"Host Monitoring - CPU Usage", 	                // Menu CheckBox text
				"Host Monitoring - CPU Usage",                  // Label 
				labelsCpu, 
				true, // is Percent Graph
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
		return new CmOsVmstatPanel(this);
	}

	@Override
	public void updateGraphData(TrendGraphDataPoint tgdp)
	{
		if (GRAPH_NAME_CPU_USAGE.equals(tgdp.getName()))
		{
			boolean hasCpuWa = this.findColumn("cpu_wa") > 0;
			Double[] arr = new Double[4];
			String[] label = new String[] {"cpu_sy+cpu_us", "cpu_sy", "cpu_us", "cpu_id", "cpu_wa"};

			// If we have WA = IO Wait, lets include that one
			if (hasCpuWa)
			{
				arr = new Double[5];
				label = new String[] {"Total (cpu_sy + cpu_us + cpu_wa)", "cpu_sy", "cpu_us", "cpu_id", "cpu_wa"};
			}
			else
			{
				arr = new Double[4];
				label = new String[] {"Total (cpu_sy + cpu_us)", "cpu_sy", "cpu_us", "cpu_id"};
			}
				

			// NOTE: only ABS values are present in CounterModelHostMonitor
//			arr[0] = this.getAbsValueAvg("cpu_us");
			arr[1] = this.getAbsValueAvg("cpu_sy");
			arr[2] = this.getAbsValueAvg("cpu_us");
			arr[3] = this.getAbsValueAvg("cpu_id");
			if (hasCpuWa)
				arr[4] = this.getAbsValueAvg("cpu_wa");

//System.out.println(GRAPH_NAME_CPU_USAGE + ": arr[1] 'sy' = " + arr[1]);
//System.out.println(GRAPH_NAME_CPU_USAGE + ": arr[2] 'us' = " + arr[2]);
//System.out.println(GRAPH_NAME_CPU_USAGE + ": arr[3] 'id' = " + arr[3]);
//if (hasCpuWa)
//	System.out.println(GRAPH_NAME_CPU_USAGE + ": arr[4] 'wa' = " + arr[4]);

			// Calculate "cpu_sy + cpu_us [+ cpu_wa]"
			if (arr[1] != null && arr[2] != null)
				arr[0] = arr[1] + arr[2];
			if (hasCpuWa && arr[1] != null && arr[2] != null && arr[4] != null)
				arr[0] = arr[1] + arr[2] + arr[4];
			
			// Fix if combined is over 100
			if (arr[0] != null && arr[0] > 100.0)
				arr[0] = 100.0;

//			if (_logger.isDebugEnabled())
//				_logger.debug("updateGraphData(OsVmstat): us+sy='"+arr[0]+"', cpu_us='"+arr[1]+"', cpu_sy='"+arr[2]+"', cpu_id='"+arr[3]+"'.");

			// Set the values
			tgdp.setDate(this.getTimestamp());
			tgdp.setLabel(label);
			tgdp.setData(arr);
		}
	}
}
