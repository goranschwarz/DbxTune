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
package com.dbxtune.cm.os;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

import org.apache.log4j.Logger;

import com.dbxtune.ICounterController;
import com.dbxtune.IGuiController;
import com.dbxtune.alarm.AlarmHandler;
import com.dbxtune.alarm.events.AlarmEventOsLoadAverage;
import com.dbxtune.alarm.events.AlarmEventOsLoadAverage.RangeType;
import com.dbxtune.central.pcs.CentralPersistReader;
import com.dbxtune.cm.CmSettingsHelper;
import com.dbxtune.cm.CounterModelHostMonitor;
import com.dbxtune.cm.CounterSetTemplates;
import com.dbxtune.cm.CountersModel;
import com.dbxtune.cm.CounterSetTemplates.Type;
import com.dbxtune.cm.os.gui.CmOsUptimePanel;
import com.dbxtune.graph.TrendGraphDataPoint;
import com.dbxtune.graph.TrendGraphDataPoint.LabelType;
import com.dbxtune.gui.MainFrame;
import com.dbxtune.gui.TabularCntrPanel;
import com.dbxtune.hostmon.HostMonitor;
import com.dbxtune.hostmon.MonitorUpTimeWindows;
import com.dbxtune.hostmon.OsTable;
import com.dbxtune.hostmon.HostMonitor.OsVendor;
import com.dbxtune.utils.Configuration;
import com.dbxtune.utils.MovingAverageChart;
import com.dbxtune.utils.MovingAverageCounterManager;

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

	public static final boolean  NEGATIVE_DIFF_COUNTERS_TO_ZERO = true;
	public static final boolean  IS_SYSTEM_CM                   = true;
	public static final int      DEFAULT_POSTPONE_TIME          = 0;

	@Override public int     getDefaultPostponeTime()                 { return DEFAULT_POSTPONE_TIME; }
//	@Override public int     getDefaultQueryTimeout()                 { return DEFAULT_QUERY_TIMEOUT; }
	@Override public boolean getDefaultIsNegativeDiffCountersToZero() { return NEGATIVE_DIFF_COUNTERS_TO_ZERO; }
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
		super(CM_NAME, GROUP_NAME, CM_TYPE, null, NEGATIVE_DIFF_COUNTERS_TO_ZERO, IS_SYSTEM_CM, DEFAULT_POSTPONE_TIME);

		setDisplayName(SHORT_NAME);
		setDescription(HTML_DESC);

		setIconFile(GUI_ICON_FILE);

		setCounterController(counterController);
		setGuiController(guiController);
		
		setDataSource(DATA_ABS, false);
		
		addTrendGraphs();
		
		CounterSetTemplates.register(this);
	}

	@Override
	protected TabularCntrPanel createGui()
	{
		return new CmOsUptimePanel(this);
	}


	//------------------------------------------------------------
	// Implementation
	//------------------------------------------------------------
	public static final String GRAPH_NAME_LOAD_AVERAGE     = "LoadAverage";
	public static final String GRAPH_NAME_ADJ_LOAD_AVERAGE = "AdjLoadAverage";
	public static final String GRAPH_NAME_WIN_LOAD_AVERAGE = "WinLoadAverage";

	private void addTrendGraphs()
	{
		// GRAPH
		addTrendGraph(GRAPH_NAME_LOAD_AVERAGE,
			"uptime: Load Average", 	                                    // Menu CheckBox text
			"uptime: Load Average ("+GROUP_NAME+"->"+SHORT_NAME+")",    // Label 
			TrendGraphDataPoint.createGraphProps(TrendGraphDataPoint.Y_AXIS_SCALE_LABELS_NORMAL, CentralPersistReader.SampleType.MAX_OVER_SAMPLES),
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
			TrendGraphDataPoint.createGraphProps(TrendGraphDataPoint.Y_AXIS_SCALE_LABELS_NORMAL, CentralPersistReader.SampleType.MAX_OVER_SAMPLES),
			new String[] { "adjLoadAverage_1Min", "adjLoadAverage_5Min", "adjLoadAverage_15Min" }, 
			LabelType.Static,
			TrendGraphDataPoint.Category.CPU,
			false, // is Percent Graph
			false, // visible at start
			0,     // graph is valid from Server Version. 0 = All Versions; >0 = Valid from this version and above 
			-1);   // minimum height

		// GRAPH
		addTrendGraph(GRAPH_NAME_WIN_LOAD_AVERAGE,
			"uptime: Windows Processor Queue Length", 	                                    // Menu CheckBox text
			"uptime: Windows Processor Queue Length ("+GROUP_NAME+"->"+SHORT_NAME+")",    // Label 
			TrendGraphDataPoint.createGraphProps(TrendGraphDataPoint.Y_AXIS_SCALE_LABELS_NORMAL, CentralPersistReader.SampleType.MAX_OVER_SAMPLES),
			new String[] { "Processor Queue Length" }, 
			LabelType.Static,
			TrendGraphDataPoint.Category.CPU,
			false, // is Percent Graph
			false, // visible at start
			0,     // graph is valid from Server Version. 0 = All Versions; >0 = Valid from this version and above 
			-1);   // minimum height
	}

	@Override
	public void updateGraphData(TrendGraphDataPoint tgdp)
	{
		// Windows
		if (isConnectedToVendor(OsVendor.Windows))
		{
			if (GRAPH_NAME_WIN_LOAD_AVERAGE.equals(tgdp.getName()))
			{
				Double[] arr = new Double[1];

				// NOTE: only ABS values are present in CounterModelHostMonitor
				arr[0] = this.getAbsValueAvg("Processor Queue Length");

				tgdp.setDataPoint(this.getTimestamp(), arr);
			}

			// For Windows I generate/calculate the 1,5,15 minute load average
			// return; // So do NOT return here
		}

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
		if (osSampleTable.getRowCount() == 0)
			return;

		// Windows ... fill in load Average
		if (isConnectedToVendor(OsVendor.Windows))
		{
			HostMonitor hostMonitor = (HostMonitor) getClientProperty(HostMonitor.PROPERTY_NAME);
			if (hostMonitor != null)
			{
				MonitorUpTimeWindows monitorUpTimeWindows = (MonitorUpTimeWindows) hostMonitor;
				monitorUpTimeWindows.setLoadAverageColumns(osSampleTable);
			}

			return;
		}

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
		int nproc = getCounterController().getHostMonConnection().getOsCoreCount();
		
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
	public void reset()
	{
		// Reset 5 minute average counters
		MovingAverageCounterManager.getInstance(this.getName(), "1Min" , 60).reset();
		MovingAverageCounterManager.getInstance(this.getName(), "5Min" , 60).reset();
		MovingAverageCounterManager.getInstance(this.getName(), "15Min", 60).reset();
		
		super.reset();
	}

	@Override
	public void sendAlarmRequest()
	{
		// Windows do NOT (for the moment) have any localCalculations
		if (isConnectedToVendor(OsVendor.Windows))
		{
			return;
		}

		CountersModel cm = this;
		String groupName = this.getName();

		if ( ! cm.hasAbsData() )
			return;

		if ( ! cm.getCounterController().isHostMonConnected() )
			return;

		if ( ! AlarmHandler.hasInstance() )
			return;

		String hostname = cm.getCounterController().getHostMonConnection().getHostname();

		boolean debugPrint = Configuration.getCombinedConfiguration().getBooleanProperty("sendAlarmRequest.debug", _logger.isDebugEnabled());
		
		//-------------------------------------------------------
		// Run Queue Length (adjLoadAverage_1Min), Avg Last Minute 
		//-------------------------------------------------------
		Double adjLoadAverage_1Min  = this.getAbsValueAsDouble(0, "adjLoadAverage_1Min" , 0d);
		Double adjLoadAverage_5Min  = this.getAbsValueAsDouble(0, "adjLoadAverage_5Min" , 0d);
		Double adjLoadAverage_15Min = this.getAbsValueAsDouble(0, "adjLoadAverage_15Min", 0d);

		// Add counters to a in-memory-storage, so we can "graph" them on Alarms
		MovingAverageCounterManager.getInstance(groupName, "1Min",  60).add(adjLoadAverage_1Min);
		MovingAverageCounterManager.getInstance(groupName, "5Min",  60).add(adjLoadAverage_5Min);
		MovingAverageCounterManager.getInstance(groupName, "15Min", 60).add(adjLoadAverage_15Min);
		

		if (isSystemAlarmsForColumnEnabledAndInTimeRange("adjLoadAverage_1Min"))
		{
			if (adjLoadAverage_1Min != null)
			{
				double threshold = Configuration.getCombinedConfiguration().getDoubleProperty(PROPKEY_alarm_adjLoadAverage_1Min, DEFAULT_alarm_adjLoadAverage_1Min);

				if (debugPrint || _logger.isDebugEnabled())
					System.out.println("##### sendAlarmRequest("+cm.getName()+"): adjLoadAverage_1Min: threshold="+threshold+", 1min=" + adjLoadAverage_1Min + ", 5min=" + adjLoadAverage_5Min + ", 15min=" + adjLoadAverage_15Min + ".");

				if (adjLoadAverage_1Min > threshold)
				{
					String htmlChartImage = MovingAverageChart.getChartAsHtmlImage("Adjusted Load Average (1 hour)", // Note make the chart on 1 hour to see more info
							MovingAverageCounterManager.getInstance(groupName, "1Min",  60),
							MovingAverageCounterManager.getInstance(groupName, "5Min",  60),
							MovingAverageCounterManager.getInstance(groupName, "15Min", 60));
					
					AlarmEventOsLoadAverage alarm = new AlarmEventOsLoadAverage(cm, threshold, hostname, RangeType.RANGE_1_MINUTE, adjLoadAverage_1Min, adjLoadAverage_5Min, adjLoadAverage_15Min);

					alarm.setExtendedDescription(null, htmlChartImage);

					AlarmHandler.getInstance().addAlarm( alarm );
				}
			}
		}

		if (isSystemAlarmsForColumnEnabledAndInTimeRange("adjLoadAverage_5Min"))
		{
			if (adjLoadAverage_5Min != null)
			{
				double threshold = Configuration.getCombinedConfiguration().getDoubleProperty(PROPKEY_alarm_adjLoadAverage_5Min, DEFAULT_alarm_adjLoadAverage_5Min);

				if (debugPrint || _logger.isDebugEnabled())
					System.out.println("##### sendAlarmRequest("+cm.getName()+"): adjLoadAverage_5Min: threshold="+threshold+", 1min=" + adjLoadAverage_1Min + ", 5min=" + adjLoadAverage_5Min + ", 15min=" + adjLoadAverage_15Min + ".");

				if (adjLoadAverage_5Min > threshold)
				{
					String htmlChartImage = MovingAverageChart.getChartAsHtmlImage("Adjusted Load Average (1 hour)", // Note make the chart on 1 hour to see more info
							MovingAverageCounterManager.getInstance(groupName, "1Min",  60),
							MovingAverageCounterManager.getInstance(groupName, "5Min",  60),
							MovingAverageCounterManager.getInstance(groupName, "15Min", 60));
					
					AlarmEventOsLoadAverage alarm = new AlarmEventOsLoadAverage(cm, threshold, hostname, RangeType.RANGE_5_MINUTE, adjLoadAverage_1Min, adjLoadAverage_5Min, adjLoadAverage_15Min);			

					alarm.setExtendedDescription(null, htmlChartImage);

					AlarmHandler.getInstance().addAlarm( alarm );
				}
			}
		}

		if (isSystemAlarmsForColumnEnabledAndInTimeRange("adjLoadAverage_15Min"))
		{
			if (adjLoadAverage_15Min != null)
			{
				double threshold = Configuration.getCombinedConfiguration().getDoubleProperty(PROPKEY_alarm_adjLoadAverage_15Min, DEFAULT_alarm_adjLoadAverage_15Min);

				if (debugPrint || _logger.isDebugEnabled())
					System.out.println("##### sendAlarmRequest("+cm.getName()+"): adjLoadAverage_15Min: threshold="+threshold+", 1min=" + adjLoadAverage_1Min + ", 5min=" + adjLoadAverage_5Min + ", 15min=" + adjLoadAverage_15Min + ".");

				if (adjLoadAverage_15Min > threshold)
				{
					String htmlChartImage = MovingAverageChart.getChartAsHtmlImage("Adjusted Load Average (1 hour)", // Note make the chart on 1 hour to see more info
							MovingAverageCounterManager.getInstance(groupName, "1Min",  60),
							MovingAverageCounterManager.getInstance(groupName, "5Min",  60),
							MovingAverageCounterManager.getInstance(groupName, "15Min", 60));
					
					AlarmEventOsLoadAverage alarm = new AlarmEventOsLoadAverage(cm, threshold, hostname, RangeType.RANGE_15_MINUTE, adjLoadAverage_1Min, adjLoadAverage_5Min, adjLoadAverage_15Min);

					alarm.setExtendedDescription(null, htmlChartImage);

					AlarmHandler.getInstance().addAlarm( alarm );
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

	// The names are used "elsewhere", this makes it less buggy if we change the name
	public static final String  ALARM_NAME_adjLoadAverage_1Min     = "adjLoadAverage_1Min";
	public static final String  ALARM_NAME_adjLoadAverage_5Min     = "adjLoadAverage_5Min";
	public static final String  ALARM_NAME_adjLoadAverage_15Min    = "adjLoadAverage_15Min";

	@Override
	public List<CmSettingsHelper> getLocalAlarmSettings()
	{
		Configuration conf = Configuration.getCombinedConfiguration();
		List<CmSettingsHelper> list = new ArrayList<>();
		
		CmSettingsHelper.Type isAlarmSwitch = CmSettingsHelper.Type.IS_ALARM_SWITCH;
		
		list.add(new CmSettingsHelper("adjLoadAverage_1Min",  isAlarmSwitch, PROPKEY_alarm_adjLoadAverage_1Min  , Double.class, conf.getDoubleProperty(PROPKEY_alarm_adjLoadAverage_1Min  , DEFAULT_alarm_adjLoadAverage_1Min),  DEFAULT_alarm_adjLoadAverage_1Min,  "If 'adjLoadAverage_1Min' is greater than ## then send 'AlarmEventOsLoadAverage'." ));
		list.add(new CmSettingsHelper("adjLoadAverage_5Min",  isAlarmSwitch, PROPKEY_alarm_adjLoadAverage_5Min  , Double.class, conf.getDoubleProperty(PROPKEY_alarm_adjLoadAverage_5Min  , DEFAULT_alarm_adjLoadAverage_5Min),  DEFAULT_alarm_adjLoadAverage_5Min,  "If 'adjLoadAverage_5Min' is greater than ## then send 'AlarmEventOsLoadAverage'." ));
		list.add(new CmSettingsHelper("adjLoadAverage_15Min", isAlarmSwitch, PROPKEY_alarm_adjLoadAverage_15Min , Double.class, conf.getDoubleProperty(PROPKEY_alarm_adjLoadAverage_15Min , DEFAULT_alarm_adjLoadAverage_15Min), DEFAULT_alarm_adjLoadAverage_15Min, "If 'adjLoadAverage_15Min' is greater than ## then send 'AlarmEventOsLoadAverage'." ));

		return list;
	}
}
