package com.asetune.cm.os;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

import org.apache.log4j.Logger;

import com.asetune.ICounterController;
import com.asetune.IGuiController;
import com.asetune.alarm.AlarmHandler;
import com.asetune.alarm.events.AlarmEventOsLoadAverage;
import com.asetune.alarm.events.AlarmEventOsLoadAverage.RangeType;
import com.asetune.cm.CmSettingsHelper;
import com.asetune.cm.CounterModelHostMonitor;
import com.asetune.cm.CounterSetTemplates;
import com.asetune.cm.CounterSetTemplates.Type;
import com.asetune.cm.CountersModel;
import com.asetune.cm.os.gui.CmOsUptimePanel;
import com.asetune.graph.TrendGraphDataPoint;
import com.asetune.graph.TrendGraphDataPoint.LabelType;
import com.asetune.gui.MainFrame;
import com.asetune.gui.TabularCntrPanel;
import com.asetune.hostmon.OsTable;
import com.asetune.utils.Configuration;

public class CmOsUptime
extends CounterModelHostMonitor
{
	private static Logger        _logger          = Logger.getLogger(CmOsUptime.class);
	private static final long    serialVersionUID = 1L;

	public static final int      CM_TYPE          = CounterModelHostMonitor.HOSTMON_UPTIME;
	public static final String   CM_NAME          = CmOsUptime.class.getSimpleName();
	public static final String   SHORT_NAME       = "OS Load Average(uptime)";
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
		
		setDataSource(DATA_ABS, false);
		
		addTrendGraphs();
		
		CounterSetTemplates.register(this);
	}


	//------------------------------------------------------------
	// Implementation
	//------------------------------------------------------------
	public static final String GRAPH_NAME_LOAD_AVERAGE     = "LoadAverage";
	public static final String GRAPH_NAME_ADJ_LOAD_AVERAGE = "AdjLoadAverage";

	private void addTrendGraphs()
	{
//		String[] labels_1  = new String[] { "loadAverage_1Min",    "loadAverage_5Min",    "loadAverage_15Min" };
//		String[] labels_2  = new String[] { "adjLoadAverage_1Min", "adjLoadAverage_5Min", "adjLoadAverage_15Min" };
//		
//		addTrendGraphData(GRAPH_NAME_LOAD_AVERAGE,     new TrendGraphDataPoint(GRAPH_NAME_LOAD_AVERAGE,     labels_1, LabelType.Static));
//		addTrendGraphData(GRAPH_NAME_ADJ_LOAD_AVERAGE, new TrendGraphDataPoint(GRAPH_NAME_ADJ_LOAD_AVERAGE, labels_2, LabelType.Static));

		// GRAPH
		addTrendGraph(GRAPH_NAME_LOAD_AVERAGE,
			"uptime: Load Average", 	                                    // Menu CheckBox text
			"uptime: Load Average ("+GROUP_NAME+"->"+SHORT_NAME+")",    // Label 
			new String[] { "loadAverage_1Min",    "loadAverage_5Min",    "loadAverage_15Min" }, 
			LabelType.Static,
			TrendGraphDataPoint.Category.CPU,
			false, // is Percent Graph
			false, // visible at start
			0,     // graph is valid from Server Version. 0 = All Versions; >0 = Valid from this version and above 
			-1);   // minimum height

		// GRAPH
		addTrendGraph(GRAPH_NAME_ADJ_LOAD_AVERAGE,
			"uptime: Adjusted Load Average", 	                                    // Menu CheckBox text
			"uptime: Adjusted Load Average ("+GROUP_NAME+"->"+SHORT_NAME+")",    // Label 
			new String[] { "adjLoadAverage_1Min", "adjLoadAverage_5Min", "adjLoadAverage_15Min" }, 
			LabelType.Static,
			TrendGraphDataPoint.Category.CPU,
			false, // is Percent Graph
			false, // visible at start
			0,     // graph is valid from Server Version. 0 = All Versions; >0 = Valid from this version and above 
			-1);   // minimum height
		
//		// if GUI
//		if (getGuiController() != null && getGuiController().hasGUI())
//		{
//			TrendGraph tg = null;
//
//			// GRAPH
//			tg = new TrendGraph(GRAPH_NAME_LOAD_AVERAGE,
//				"uptime: Load Average", 	                                    // Menu CheckBox text
//				"uptime: Load Average ("+GROUP_NAME+"->"+SHORT_NAME+")",    // Label 
//				labels_1, 
//				false, // is Percent Graph
//				this, 
//				false, // visible at start
//				0,     // graph is valid from Server Version. 0 = All Versions; >0 = Valid from this version and above 
//				-1);  // minimum height
//			addTrendGraph(tg.getName(), tg, true);
//
//			// GRAPH
//			tg = new TrendGraph(GRAPH_NAME_ADJ_LOAD_AVERAGE,
//				"uptime: Adjusted Load Average", 	                                    // Menu CheckBox text
//				"uptime: Adjusted Load Average ("+GROUP_NAME+"->"+SHORT_NAME+")",    // Label 
//				labels_2, 
//				false, // is Percent Graph
//				this, 
//				false, // visible at start
//				0,     // graph is valid from Server Version. 0 = All Versions; >0 = Valid from this version and above 
//				-1);  // minimum height
//			addTrendGraph(tg.getName(), tg, true);
//		}
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
			Double[] arr = new Double[3];

			// NOTE: only ABS values are present in CounterModelHostMonitor
			arr[0] = this.getAbsValueAvg("loadAverage_1Min");
			arr[1] = this.getAbsValueAvg("loadAverage_5Min");
			arr[2] = this.getAbsValueAvg("loadAverage_15Min");

			tgdp.setDataPoint(this.getTimestamp(), arr);
		}

		if (GRAPH_NAME_ADJ_LOAD_AVERAGE.equals(tgdp.getName()))
		{
			Double[] arr = new Double[3];

			// NOTE: only ABS values are present in CounterModelHostMonitor
			arr[0] = this.getAbsValueAvg("adjLoadAverage_1Min");
			arr[1] = this.getAbsValueAvg("adjLoadAverage_5Min");
			arr[2] = this.getAbsValueAvg("adjLoadAverage_15Min");

			tgdp.setDataPoint(this.getTimestamp(), arr);
		}
	}
	
	@Override
	public void localCalculation(OsTable osSampleTable)
	{
//		System.out.println(getName()+ ": localCalculation(OsTable osSampleTable) " 
//				+ "rowcount="   + osSampleTable.getRowCount()
//				+ ", colCount=" + osSampleTable.getColumnCount()
//				+ ", ColNames=" + osSampleTable.getColNames()
//				+ " !!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!");

		// Check rowcount
		if (osSampleTable.getRowCount() != 1)
		{
			_logger.warn(getName() + ".localCalculation(OsTable) expected number of rows was 1, the table contains "+osSampleTable.getRowCount()+" rows.");
			return;
		}
		
		// Check/get column position
		int pos_loadAverage_1Min     = osSampleTable.findColumn("loadAverage_1Min");
		int pos_loadAverage_5Min     = osSampleTable.findColumn("loadAverage_5Min");
		int pos_loadAverage_15Min    = osSampleTable.findColumn("loadAverage_15Min");
		int pos_nproc                = osSampleTable.findColumn("nproc");
		int pos_adjLoadAverage_1Min  = osSampleTable.findColumn("adjLoadAverage_1Min");
		int pos_adjLoadAverage_5Min  = osSampleTable.findColumn("adjLoadAverage_5Min");
		int pos_adjLoadAverage_15Min = osSampleTable.findColumn("adjLoadAverage_15Min");

		if (    pos_loadAverage_1Min     < 0 
		     || pos_loadAverage_5Min     < 0
		     || pos_loadAverage_15Min    < 0
		     || pos_nproc                < 0
		     || pos_adjLoadAverage_1Min  < 0
		     || pos_adjLoadAverage_5Min  < 0
		     || pos_adjLoadAverage_15Min < 0
		   )
		{
			_logger.warn(getName() + ".localCalculation(OsTable) could not find all desired columns"
				+ ". pos_loadAverage_1Min="     + pos_loadAverage_1Min
				+ ", pos_loadAverage_5Min="     + pos_loadAverage_5Min
				+ ", pos_loadAverage_15Min="    + pos_loadAverage_15Min
				+ ", pos_nproc="                + pos_nproc
				+ ", pos_adjLoadAverage_1Min="  + pos_adjLoadAverage_1Min
				+ ", pos_adjLoadAverage_5Min="  + pos_adjLoadAverage_5Min
				+ ", pos_adjLoadAverage_15Min=" + pos_adjLoadAverage_15Min
				);
			return;
		}

		// Get nproc from the SSH Connection
		int nproc = getCounterController().getHostMonConnection().getNproc();
		
		BigDecimal adjLoadAverage_1Min  = null;
		BigDecimal adjLoadAverage_5Min  = null;
		BigDecimal adjLoadAverage_15Min = null;

		if (nproc > 0)
		{
			BigDecimal loadAverage_1Min  = (BigDecimal) osSampleTable.getValueAt(0, pos_loadAverage_1Min);
			BigDecimal loadAverage_5Min  = (BigDecimal) osSampleTable.getValueAt(0, pos_loadAverage_5Min);
			BigDecimal loadAverage_15Min = (BigDecimal) osSampleTable.getValueAt(0, pos_loadAverage_15Min);

			adjLoadAverage_1Min  = new BigDecimal( loadAverage_1Min .doubleValue() / nproc ).setScale(2, BigDecimal.ROUND_HALF_EVEN);
			adjLoadAverage_5Min  = new BigDecimal( loadAverage_5Min .doubleValue() / nproc ).setScale(2, BigDecimal.ROUND_HALF_EVEN);
			adjLoadAverage_15Min = new BigDecimal( loadAverage_15Min.doubleValue() / nproc ).setScale(2, BigDecimal.ROUND_HALF_EVEN);
		}
		
		// Set nproc
		osSampleTable.setValueAt(nproc,  0, pos_nproc);

		// set adjLoadAverage_#Min
		osSampleTable.setValueAt(adjLoadAverage_1Min,  0, pos_adjLoadAverage_1Min);
		osSampleTable.setValueAt(adjLoadAverage_5Min,  0, pos_adjLoadAverage_5Min);
		osSampleTable.setValueAt(adjLoadAverage_15Min, 0, pos_adjLoadAverage_15Min);
	}

	@Override
	public void sendAlarmRequest()
	{
		CountersModel cm = this;

		if ( ! cm.hasAbsData() )
			return;
		if ( ! cm.getCounterController().isHostMonConnected() )
			return;

		String hostname = cm.getCounterController().getHostMonConnection().getHost();

		boolean debugPrint = System.getProperty("sendAlarmRequest.debug", "false").equalsIgnoreCase("true");
		
		//-------------------------------------------------------
		// Run Queue Length (adjLoadAverage_1Min), Avg Last Minute 
		//-------------------------------------------------------
		Double adjLoadAverage_1Min  = this.getAbsValueAsDouble(0, "adjLoadAverage_1Min");
		Double adjLoadAverage_5Min  = this.getAbsValueAsDouble(0, "adjLoadAverage_5Min");
		Double adjLoadAverage_15Min = this.getAbsValueAsDouble(0, "adjLoadAverage_15Min");

		if (isSystemAlarmsForColumnEnabledAndInTimeRange("adjLoadAverage_1Min"))
		{
			if (adjLoadAverage_1Min != null)
			{
				if (debugPrint || _logger.isDebugEnabled())
					System.out.println("##### sendAlarmRequest("+cm.getName()+"): adjLoadAverage: 1min=" + adjLoadAverage_1Min + ", 5min=" + adjLoadAverage_5Min + ", 15min=" + adjLoadAverage_15Min + ".");

				if (AlarmHandler.hasInstance())
				{
					double threshold = Configuration.getCombinedConfiguration().getDoubleProperty(PROPKEY_alarm_adjLoadAverage_1Min, DEFAULT_alarm_adjLoadAverage_1Min);
					if (adjLoadAverage_1Min > threshold)
						AlarmHandler.getInstance().addAlarm( new AlarmEventOsLoadAverage(cm, threshold, hostname, RangeType.RANGE_1_MINUTE, adjLoadAverage_1Min, adjLoadAverage_5Min, adjLoadAverage_15Min) );
				}
			}
		}

		if (isSystemAlarmsForColumnEnabledAndInTimeRange("adjLoadAverage_5Min"))
		{
			if (adjLoadAverage_5Min != null)
			{
				if (debugPrint || _logger.isDebugEnabled())
					System.out.println("##### sendAlarmRequest("+cm.getName()+"): adjLoadAverage: 1min=" + adjLoadAverage_1Min + ", 5min=" + adjLoadAverage_5Min + ", 15min=" + adjLoadAverage_15Min + ".");

				if (AlarmHandler.hasInstance())
				{
					double threshold = Configuration.getCombinedConfiguration().getDoubleProperty(PROPKEY_alarm_adjLoadAverage_5Min, DEFAULT_alarm_adjLoadAverage_5Min);
					if (adjLoadAverage_5Min > threshold)
						AlarmHandler.getInstance().addAlarm( new AlarmEventOsLoadAverage(cm, threshold, hostname, RangeType.RANGE_5_MINUTE, adjLoadAverage_1Min, adjLoadAverage_5Min, adjLoadAverage_15Min) );
				}
			}
		}

		if (isSystemAlarmsForColumnEnabledAndInTimeRange("adjLoadAverage_15Min"))
		{
			if (adjLoadAverage_15Min != null)
			{
				if (debugPrint || _logger.isDebugEnabled())
					System.out.println("##### sendAlarmRequest("+cm.getName()+"): adjLoadAverage: 1min=" + adjLoadAverage_1Min + ", 5min=" + adjLoadAverage_5Min + ", 15min=" + adjLoadAverage_15Min + ".");

				if (AlarmHandler.hasInstance())
				{
					double threshold = Configuration.getCombinedConfiguration().getDoubleProperty(PROPKEY_alarm_adjLoadAverage_15Min, DEFAULT_alarm_adjLoadAverage_15Min);
					if (adjLoadAverage_15Min > threshold)
						AlarmHandler.getInstance().addAlarm( new AlarmEventOsLoadAverage(cm, threshold, hostname, RangeType.RANGE_15_MINUTE, adjLoadAverage_1Min, adjLoadAverage_5Min, adjLoadAverage_15Min) );
				}
			}
		}
	}

	public static final String  PROPKEY_alarm_adjLoadAverage_1Min = CM_NAME + ".alarm.system.if.adjLoadAverage_1Min.gt";
//	public static final double  DEFAULT_alarm_adjLoadAverage_1Min = 1.7;
	public static final double  DEFAULT_alarm_adjLoadAverage_1Min = 3.0;

	public static final String  PROPKEY_alarm_adjLoadAverage_5Min = CM_NAME + ".alarm.system.if.adjLoadAverage_5Min.gt";
//	public static final double  DEFAULT_alarm_adjLoadAverage_5Min = 1.4;
	public static final double  DEFAULT_alarm_adjLoadAverage_5Min = 2.0;

	public static final String  PROPKEY_alarm_adjLoadAverage_15Min = CM_NAME + ".alarm.system.if.adjLoadAverage_15Min.gt";
//	public static final double  DEFAULT_alarm_adjLoadAverage_15Min = 1.0;
	public static final double  DEFAULT_alarm_adjLoadAverage_15Min = 1.5;

	@Override
	public List<CmSettingsHelper> getLocalAlarmSettings()
	{
		Configuration conf = Configuration.getCombinedConfiguration();
		List<CmSettingsHelper> list = new ArrayList<>();
		
		list.add(new CmSettingsHelper("adjLoadAverage_1Min",  PROPKEY_alarm_adjLoadAverage_1Min  , Double.class, conf.getDoubleProperty(PROPKEY_alarm_adjLoadAverage_1Min  , DEFAULT_alarm_adjLoadAverage_1Min),  DEFAULT_alarm_adjLoadAverage_1Min,  "If 'adjLoadAverage_1Min' is greater than ## then send 'AlarmEventOsLoadAverage'." ));
		list.add(new CmSettingsHelper("adjLoadAverage_5Min",  PROPKEY_alarm_adjLoadAverage_5Min  , Double.class, conf.getDoubleProperty(PROPKEY_alarm_adjLoadAverage_5Min  , DEFAULT_alarm_adjLoadAverage_5Min),  DEFAULT_alarm_adjLoadAverage_5Min,  "If 'adjLoadAverage_5Min' is greater than ## then send 'AlarmEventOsLoadAverage'." ));
		list.add(new CmSettingsHelper("adjLoadAverage_15Min", PROPKEY_alarm_adjLoadAverage_15Min , Double.class, conf.getDoubleProperty(PROPKEY_alarm_adjLoadAverage_15Min , DEFAULT_alarm_adjLoadAverage_15Min), DEFAULT_alarm_adjLoadAverage_15Min, "If 'adjLoadAverage_15Min' is greater than ## then send 'AlarmEventOsLoadAverage'." ));

		return list;
	}
}
