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
package com.dbxtune.cm.ase;

import java.lang.invoke.MethodHandles;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

import javax.swing.JCheckBoxMenuItem;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.dbxtune.ICounterController;
import com.dbxtune.IGuiController;
import com.dbxtune.alarm.AlarmHandler;
import com.dbxtune.alarm.events.AlarmEvent;
import com.dbxtune.alarm.events.AlarmEventRunQueueLength;
import com.dbxtune.alarm.events.AlarmEventRunQueueLength.RangeType;
import com.dbxtune.central.pcs.CentralPersistReader;
import com.dbxtune.cm.CmSettingsHelper;
import com.dbxtune.cm.CounterSetTemplates;
import com.dbxtune.cm.CounterSetTemplates.Type;
import com.dbxtune.cm.CountersModel;
import com.dbxtune.graph.TrendGraphDataPoint;
import com.dbxtune.graph.TrendGraphDataPoint.LabelType;
import com.dbxtune.gui.MainFrame;
import com.dbxtune.gui.TrendGraph;
import com.dbxtune.sql.conn.DbxConnection;
import com.dbxtune.sql.conn.info.DbmsVersionInfo;
import com.dbxtune.sql.conn.info.DbmsVersionInfoSybaseAse;
import com.dbxtune.utils.Configuration;
import com.dbxtune.utils.NumberUtils;
import com.dbxtune.utils.Ver;

/**
 * @author Goran Schwarz (goran_schwarz@hotmail.com)
 */
public class CmSysLoad
extends CountersModel
{
	private static final Logger _logger = LogManager.getLogger(MethodHandles.lookup().lookupClass());
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

//	public static final long     NEED_SRV_VERSION = 15500;
//	public static final long     NEED_SRV_VERSION = 1550000;
	public static final long     NEED_SRV_VERSION = Ver.ver(15,5);
//	public static final long     NEED_CE_VERSION  = 15020;
//	public static final long     NEED_CE_VERSION  = 1502000;
	public static final long     NEED_CE_VERSION  = Ver.ver(15,0,2);

	public static final String[] MON_TABLES       = new String[] {"monSysLoad"};
	public static final String[] NEED_ROLES       = new String[] {"mon_role"};
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
		super(counterController, guiController,
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
	private static final String  PROP_PREFIX = CM_NAME;
	
	public static final String   GRAPH_NAME_AVG_RUN_QUEUE_LENTH    = "AvgRunQLengthGraph";    //String x=GetCounters.CM_GRAPH_NAME__SYS_LOAD__AVG_RUN_QUEUE_LENTH;
	public static final String   GRAPH_NAME_ENGINE_RUN_QUEUE_LENTH = "EngineRunQLengthGraph"; //String x=GetCounters.CM_GRAPH_NAME__SYS_LOAD__ENGINE_RUN_QUEUE_LENTH;
	public static final String   GRAPH_NAME_SUM_OUTSTAND_IO        = "SumOutstandIoGraph";
	public static final String   GRAPH_NAME_ENGINE_NOW_OUTSTAND_IO = "EngineNowOutstandIo";  
	public static final String   GRAPH_NAME_ENGINE_1M_OUTSTAND_IO  = "Engine1MinOutstandIo";  

	private void addTrendGraphs()
	{
		addTrendGraph(GRAPH_NAME_AVG_RUN_QUEUE_LENTH,
			"Run Queue Length, Server Wide", 	                                    // Menu CheckBox text
			"Run Queue Length, Average for all instances ("+GROUP_NAME+"->"+SHORT_NAME+")", // Label 
			TrendGraphDataPoint.createGraphProps(TrendGraphDataPoint.Y_AXIS_SCALE_LABELS_NORMAL, CentralPersistReader.SampleType.MAX_OVER_SAMPLES),
//			new String[] { "Now", "Avg last 1 minute", "Avg last 5 minute", "Max last 1 minute", "Max last 5 minute" }, 
			new String[] { "Now", "Avg last 1 minute", "Avg last 5 minute" }, 
			LabelType.Static,
			TrendGraphDataPoint.Category.CPU,
			false, // is Percent Graph
			false, // visible at start
			Ver.ver(15, 5), // graph is valid from Server Version. 0 = All Versions; >0 = Valid from this version and above 
			-1);   // minimum height

		addTrendGraph(GRAPH_NAME_ENGINE_RUN_QUEUE_LENTH,
			"Run Queue Length, Per Engine", 	                                               // Menu CheckBox text
			"Run Queue Length, Average over last minute, Per Engine ("+GROUP_NAME+"->"+SHORT_NAME+")", // Label 
			TrendGraphDataPoint.createGraphProps(TrendGraphDataPoint.Y_AXIS_SCALE_LABELS_NORMAL, CentralPersistReader.SampleType.MAX_OVER_SAMPLES),
			null, 
			LabelType.Dynamic,
			TrendGraphDataPoint.Category.CPU,
			false, // is Percent Graph
			false, // visible at start
			15500, // graph is valid from Server Version. 0 = All Versions; >0 = Valid from this version and above 
			-1);   // minimum height

		addTrendGraph(GRAPH_NAME_SUM_OUTSTAND_IO,
			"Outstanding IO's, Server Wide", 	                                    // Menu CheckBox text
			"Outstanding IO's, Summary for all instances ("+GROUP_NAME+"->"+SHORT_NAME+")", // Label 
			TrendGraphDataPoint.createGraphProps(TrendGraphDataPoint.Y_AXIS_SCALE_LABELS_NORMAL, CentralPersistReader.SampleType.MAX_OVER_SAMPLES),
			new String[] { "Sum Now", "Sum last 1 minute", "Sum last 5 minute", "Sum last 15 minute" }, 
			LabelType.Static,
			TrendGraphDataPoint.Category.DISK,
			false, // is Percent Graph
			false, // visible at start
			Ver.ver(15, 5), // graph is valid from Server Version. 0 = All Versions; >0 = Valid from this version and above 
			-1);   // minimum height

		addTrendGraph(GRAPH_NAME_ENGINE_NOW_OUTSTAND_IO,
			"Outstanding IO's, Per Engine (at sample)", 	                                   // Menu CheckBox text
			"Outstanding IO's, When the refresh happened, Per Engine ("+GROUP_NAME+"->"+SHORT_NAME+")", // Label 
			TrendGraphDataPoint.createGraphProps(TrendGraphDataPoint.Y_AXIS_SCALE_LABELS_NORMAL, CentralPersistReader.SampleType.MAX_OVER_SAMPLES),
			null, 
			LabelType.Dynamic,
			TrendGraphDataPoint.Category.DISK,
			false, // is Percent Graph
			false, // visible at start
			Ver.ver(15, 5), // graph is valid from Server Version. 0 = All Versions; >0 = Valid from this version and above 
			-1);   // minimum height

		addTrendGraph(GRAPH_NAME_ENGINE_1M_OUTSTAND_IO,
			"Outstanding IO's, Per Engine (avg 1 minute)", 	                                   // Menu CheckBox text
			"Outstanding IO's, Average over last minute, Per Engine ("+GROUP_NAME+"->"+SHORT_NAME+")", // Label 
			TrendGraphDataPoint.createGraphProps(TrendGraphDataPoint.Y_AXIS_SCALE_LABELS_NORMAL, CentralPersistReader.SampleType.MAX_OVER_SAMPLES),
			null, 
			LabelType.Dynamic,
			TrendGraphDataPoint.Category.DISK,
			false, // is Percent Graph
			true, // visible at start
			Ver.ver(15, 5), // graph is valid from Server Version. 0 = All Versions; >0 = Valid from this version and above 
			-1);   // minimum height

	}

//	@Override
//	protected TabularCntrPanel createGui()
//	{
//		return new CmSysLoadPanel(this);
//	}

	@Override
	public String[] getDependsOnConfigForVersion(DbxConnection conn, DbmsVersionInfo versionInfo)
	{
		return NEED_CONFIG;
	}

	@Override
	public List<String> getPkForVersion(DbxConnection conn, DbmsVersionInfo versionInfo)
	{
		DbmsVersionInfoSybaseAse aseVersionInfo = (DbmsVersionInfoSybaseAse) versionInfo;
//		long    srvVersion       = aseVersionInfo.getLongVersion();
		boolean isClusterEnabled = aseVersionInfo.isClusterEdition();

		List <String> pkCols = new LinkedList<String>();

		if (isClusterEnabled)
			pkCols.add("InstanceID");

		pkCols.add("StatisticID");
		pkCols.add("EngineNumber");

		return pkCols;
	}

	@Override
	public String getSqlForVersion(DbxConnection conn, DbmsVersionInfo versionInfo)
	{
		DbmsVersionInfoSybaseAse aseVersionInfo = (DbmsVersionInfoSybaseAse) versionInfo;
		long    srvVersion       = aseVersionInfo.getLongVersion();
		boolean isClusterEnabled = aseVersionInfo.isClusterEdition();

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
		if (srvVersion < Ver.ver(15,7))
			sql += "order by StatisticID, EngineNumber" + (isClusterEnabled ? ", InstanceID" : "");

		return sql;
	}

	@Override
	public void updateGraphData(TrendGraphDataPoint tgdp)
	{
		long srvVersion = getServerVersion();

		if (GRAPH_NAME_AVG_RUN_QUEUE_LENTH.equals(tgdp.getName()))
		{
			if (srvVersion < Ver.ver(15,5))
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
					_logger.warn("When updateGraphData for '"+tgdp.getName()+"', getAbsRowIdsWhere('Statistic', 'run queue length'), returned null, so I can't do more here.");
				else
				{
//					Double[] arr = new Double[5];
//					arr[0] = this.getAbsValueAvg(rqRows, "Sample");
//					arr[1] = this.getAbsValueAvg(rqRows, "Avg_1min");
//					arr[2] = this.getAbsValueAvg(rqRows, "Avg_5min");
//					arr[3] = this.getAbsValueAvg(rqRows, "Max_1min");
//					arr[4] = this.getAbsValueAvg(rqRows, "Max_5min");
//					_logger.debug("updateGraphData("+GRAPH_NAME_AVG_RUN_QUEUE_LENTH+"): Sample='"+arr[0]+"', Avg_1min='"+arr[1]+"', Avg_5min='"+arr[2]+"', Max_1min='"+arr[3]+"', Max_5min='"+arr[4]+"'.");
					
					// label: "Max_1min" and "Max_5min" seems to be "flat" and do not really say anything, so lets remove it (for now)
					Double[] arr = new Double[3];
					arr[0] = this.getAbsValueAvg(rqRows, "Sample");
					arr[1] = this.getAbsValueAvg(rqRows, "Avg_1min");
					arr[2] = this.getAbsValueAvg(rqRows, "Avg_5min");
					_logger.debug("updateGraphData("+GRAPH_NAME_AVG_RUN_QUEUE_LENTH+"): Sample='"+arr[0]+"', Avg_1min='"+arr[1]+"', Avg_5min='"+arr[2]+"'.");

					// Set the values
					tgdp.setDataPoint(this.getTimestamp(), arr);
				}
			}
		} // end: GRAPH_NAME_AVG_RUN_QUEUE_LENTH

		if (GRAPH_NAME_ENGINE_RUN_QUEUE_LENTH.equals(tgdp.getName()))
		{
			if (srvVersion < Ver.ver(15,5))
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
					_logger.warn("When updateGraphData for '"+tgdp.getName()+"', getAbsRowIdsWhere('Statistic', 'run queue length'), returned null, so I can't do more here.");
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
						_logger.debug("updateGraphData("+GRAPH_NAME_ENGINE_RUN_QUEUE_LENTH+"): "+debugStr);
					}

					// Set the values
					tgdp.setDataPoint(this.getTimestamp(), label, data);
				}
			}
		} // end: GRAPH_NAME_ENGINE_RUN_QUEUE_LENTH

		if (GRAPH_NAME_SUM_OUTSTAND_IO.equals(tgdp.getName()))
		{
			if (srvVersion < Ver.ver(15,5))
			{
				// disable the graph checkbox...
				TrendGraph tg = getTrendGraph(GRAPH_NAME_SUM_OUTSTAND_IO);
				if (tg != null)
				{
					JCheckBoxMenuItem menuItem = tg.getViewMenuItem();
					if (menuItem.isSelected())
						menuItem.doClick();
				}
			}
			else
			{
				int[] rqRows = this.getAbsRowIdsWhere("Statistic", "outstanding disk i/os");
				if (rqRows == null)
					_logger.warn("When updateGraphData for '"+tgdp.getName()+"', getAbsRowIdsWhere('Statistic', 'outstanding disk i/os'), returned null, so I can't do more here.");
				else
				{
					Double[] arr = new Double[4];
					arr[0] = this.getAbsValueSum(rqRows, "Sample");
					arr[1] = this.getAbsValueSum(rqRows, "Avg_1min");
					arr[2] = this.getAbsValueSum(rqRows, "Avg_5min");
					arr[3] = this.getAbsValueSum(rqRows, "Avg_15min");
					_logger.debug("updateGraphData("+GRAPH_NAME_SUM_OUTSTAND_IO+"): Sample='"+arr[0]+"', Sum_1min='"+arr[1]+"', Sum_5min='"+arr[2]+"', Sum_15min='"+arr[3]+"'.");

					// Set the values
					tgdp.setDataPoint(this.getTimestamp(), arr);
				}
			}
		} // end: GRAPH_NAME_AVG_RUN_QUEUE_LENTH

		if (GRAPH_NAME_ENGINE_NOW_OUTSTAND_IO.equals(tgdp.getName()))
		{
			if (srvVersion < Ver.ver(15,5))
			{
				// disable the graph checkbox...
				TrendGraph tg = getTrendGraph(GRAPH_NAME_ENGINE_NOW_OUTSTAND_IO);
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
				int[] rqRows = this.getAbsRowIdsWhere("Statistic", "outstanding disk i/os");
				if (rqRows == null)
					_logger.warn("When updateGraphData for '"+tgdp.getName()+"', getAbsRowIdsWhere('Statistic', 'outstanding disk i/os'), returned null, so I can't do more here.");
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
						data[i]  = this.getAbsValueAsDouble(rowId, "Sample");
					}
					if (_logger.isDebugEnabled())
					{
						String debugStr = "";
						for (int i=0; i<data.length; i++)
							debugStr += label[i] + "='"+data[i]+"', ";
						_logger.debug("updateGraphData("+GRAPH_NAME_ENGINE_NOW_OUTSTAND_IO+"): "+debugStr);
					}

					// Set the values
					tgdp.setDataPoint(this.getTimestamp(), label, data);
				}
			}
		} // end: GRAPH_NAME_ENGINE_NOW_OUTSTAND_IO

		if (GRAPH_NAME_ENGINE_1M_OUTSTAND_IO.equals(tgdp.getName()))
		{
			if (srvVersion < Ver.ver(15,5))
			{
				// disable the graph checkbox...
				TrendGraph tg = getTrendGraph(GRAPH_NAME_ENGINE_1M_OUTSTAND_IO);
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
				int[] rqRows = this.getAbsRowIdsWhere("Statistic", "outstanding disk i/os");
				if (rqRows == null)
					_logger.warn("When updateGraphData for '"+tgdp.getName()+"', getAbsRowIdsWhere('Statistic', 'outstanding disk i/os'), returned null, so I can't do more here.");
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
						_logger.debug("updateGraphData("+GRAPH_NAME_ENGINE_1M_OUTSTAND_IO+"): "+debugStr);
					}

					// Set the values
					tgdp.setDataPoint(this.getTimestamp(), label, data);
				}
			}
		} // end: GRAPH_NAME_ENGINE_1M_OUTSTAND_IO
	}

	@Override
	public void sendAlarmRequest()
	{
		if ( ! hasDiffData() )
			return;

		if ( ! AlarmHandler.hasInstance() )
			return;

		CountersModel cm = this;

		boolean debugPrint = Configuration.getCombinedConfiguration().getBooleanProperty("sendAlarmRequest.debug", _logger.isDebugEnabled());

		// If version is below 15.5: do not continue
		long srvVersion = cm.getServerVersion();
		if (srvVersion < Ver.ver(15,5))
			return;


		//-------------------------------------------------------
		// Run Queue Length, Avg Last Minute 
		//-------------------------------------------------------
		if (    isSystemAlarmsForColumnEnabledAndInTimeRange("RunQueueLengthAvg1min") 
		     || isSystemAlarmsForColumnEnabledAndInTimeRange("RunQueueLengthAvg5min") 
		     || isSystemAlarmsForColumnEnabledAndInTimeRange("RunQueueLengthAvg15min") 
		   )
		{
			int[] rqRows = this.getAbsRowIdsWhere("Statistic", "run queue length");
			if (rqRows == null)
				_logger.warn("In sendAlarmRequest for '"+cm.getName()+"', getAbsRowIdsWhere('Statistic', 'run queue length'), returned null, so I can't do more here.");
			else
			{
				// round the double value to 3 decimals
				int decimals = 3;
				Double Avg_1min  = NumberUtils.round( cm.getAbsValueAvg(rqRows, "Avg_1min"),  decimals);
				Double Avg_5min  = NumberUtils.round( cm.getAbsValueAvg(rqRows, "Avg_5min"),  decimals);
				Double Avg_15min = NumberUtils.round( cm.getAbsValueAvg(rqRows, "Avg_15min"), decimals);

				if (isSystemAlarmsForColumnEnabledAndInTimeRange("RunQueueLengthAvg1min"))
				{
					if (Avg_1min != null)
					{
						double threshold = Configuration.getCombinedConfiguration().getDoubleProperty(PROPKEY_alarm_RunQueueLengthAvg1min, DEFAULT_alarm_RunQueueLengthAvg1min);

						if (debugPrint || _logger.isDebugEnabled())
							System.out.println("##### sendAlarmRequest("+cm.getName()+"): RunQueueLengthAvg1min - threshold="+threshold+", RunQueueLength: avg_1min=" + Avg_1min + ", avg_5min="+Avg_5min+", avg_15min="+Avg_15min+".");

						if (Avg_1min > threshold)
						{
							String extendedDescText = "";
							String extendedDescHtml = cm.getGraphDataHistoryAsHtmlImage(GRAPH_NAME_AVG_RUN_QUEUE_LENTH);

							AlarmEvent ae = new AlarmEventRunQueueLength(cm, threshold, RangeType.RANGE_1_MINUTE, Avg_1min, Avg_5min, Avg_15min);
							ae.setExtendedDescription(extendedDescText, extendedDescHtml);

							AlarmHandler.getInstance().addAlarm(ae);
						}
					}
				}

				if (isSystemAlarmsForColumnEnabledAndInTimeRange("RunQueueLengthAvg5min"))
				{
					if (Avg_5min != null)
					{
						double threshold = Configuration.getCombinedConfiguration().getDoubleProperty(PROPKEY_alarm_RunQueueLengthAvg5min, DEFAULT_alarm_RunQueueLengthAvg5min);

						if (debugPrint || _logger.isDebugEnabled())
							System.out.println("##### sendAlarmRequest("+cm.getName()+"): RunQueueLengthAvg5min - threshold="+threshold+", RunQueueLength: avg_1min=" + Avg_1min + ", avg_5min="+Avg_5min+", avg_15min="+Avg_15min+".");

						if (Avg_5min > threshold)
						{
							String extendedDescText = "";
							String extendedDescHtml = cm.getGraphDataHistoryAsHtmlImage(GRAPH_NAME_AVG_RUN_QUEUE_LENTH);

							AlarmEvent ae = new AlarmEventRunQueueLength(cm, threshold, RangeType.RANGE_5_MINUTE, Avg_1min, Avg_5min, Avg_15min);
							ae.setExtendedDescription(extendedDescText, extendedDescHtml);

							AlarmHandler.getInstance().addAlarm(ae);
						}
					}
				}

				if (isSystemAlarmsForColumnEnabledAndInTimeRange("RunQueueLengthAvg15min"))
				{
					if (Avg_15min != null)
					{
						double threshold = Configuration.getCombinedConfiguration().getDoubleProperty(PROPKEY_alarm_RunQueueLengthAvg15min, DEFAULT_alarm_RunQueueLengthAvg15min);

						if (debugPrint || _logger.isDebugEnabled())
							System.out.println("##### sendAlarmRequest("+cm.getName()+"): RunQueueLengthAvg15min - threshold="+threshold+", RunQueueLength: avg_1min=" + Avg_1min + ", avg_5min="+Avg_5min+", avg_15min="+Avg_15min+".");

						if (Avg_15min > threshold)
						{
							String extendedDescText = "";
							String extendedDescHtml = cm.getGraphDataHistoryAsHtmlImage(GRAPH_NAME_AVG_RUN_QUEUE_LENTH);

							AlarmEvent ae = new AlarmEventRunQueueLength(cm, threshold, RangeType.RANGE_15_MINUTE, Avg_1min, Avg_5min, Avg_15min);
							ae.setExtendedDescription(extendedDescText, extendedDescHtml);

							AlarmHandler.getInstance().addAlarm(ae);
						}
					}
				}
			}
		}
	}

	@Override
	public boolean isGraphDataHistoryEnabled(String name)
	{
		// ENABLED for the following graphs
		if (GRAPH_NAME_AVG_RUN_QUEUE_LENTH.equals(name)) return true;

		// default: DISABLED
		return false;
	}
	@Override
	public int getGraphDataHistoryTimeInterval(String name)
	{
		// Keep interval: default is 60 minutes
		return super.getGraphDataHistoryTimeInterval(name);
	}

	
	public static final String  PROPKEY_alarm_RunQueueLengthAvg1min = PROP_PREFIX + ".alarm.system.if.RunQueueLengthAvg1min.gt";
//	public static final double  DEFAULT_alarm_RunQueueLengthAvg1min = 1.7;
	public static final double  DEFAULT_alarm_RunQueueLengthAvg1min = 3.0;

	public static final String  PROPKEY_alarm_RunQueueLengthAvg5min = PROP_PREFIX + ".alarm.system.if.RunQueueLengthAvg5min.gt";
//	public static final double  DEFAULT_alarm_RunQueueLengthAvg5min = 1.4;
	public static final double  DEFAULT_alarm_RunQueueLengthAvg5min = 2.0;

	public static final String  PROPKEY_alarm_RunQueueLengthAvg15min = PROP_PREFIX + ".alarm.system.if.RunQueueLengthAvg15min.gt";
//	public static final double  DEFAULT_alarm_RunQueueLengthAvg15min = 1.0;
	public static final double  DEFAULT_alarm_RunQueueLengthAvg15min = 1.5;

	@Override
	public List<CmSettingsHelper> getLocalAlarmSettings()
	{
		Configuration conf = Configuration.getCombinedConfiguration();
		List<CmSettingsHelper> list = new ArrayList<>();

		CmSettingsHelper.Type isAlarmSwitch = CmSettingsHelper.Type.IS_ALARM_SWITCH;
		
		list.add(new CmSettingsHelper("RunQueueLengthAvg1min" , isAlarmSwitch, PROPKEY_alarm_RunQueueLengthAvg1min , Double.class, conf.getDoubleProperty(PROPKEY_alarm_RunQueueLengthAvg1min , DEFAULT_alarm_RunQueueLengthAvg1min) , DEFAULT_alarm_RunQueueLengthAvg1min , "If 'RunQueueLength: Avg_1min' is greater than ## then send 'AlarmEventRunQueueLength'." ));
		list.add(new CmSettingsHelper("RunQueueLengthAvg5min" , isAlarmSwitch, PROPKEY_alarm_RunQueueLengthAvg5min , Double.class, conf.getDoubleProperty(PROPKEY_alarm_RunQueueLengthAvg5min , DEFAULT_alarm_RunQueueLengthAvg5min) , DEFAULT_alarm_RunQueueLengthAvg5min , "If 'RunQueueLength: Avg_5min' is greater than ## then send 'AlarmEventRunQueueLength'." ));
		list.add(new CmSettingsHelper("RunQueueLengthAvg15min", isAlarmSwitch, PROPKEY_alarm_RunQueueLengthAvg15min, Double.class, conf.getDoubleProperty(PROPKEY_alarm_RunQueueLengthAvg15min, DEFAULT_alarm_RunQueueLengthAvg15min), DEFAULT_alarm_RunQueueLengthAvg15min, "If 'RunQueueLength: Avg_15min' is greater than ## then send 'AlarmEventRunQueueLength'." ));

		return list;
	}
}
