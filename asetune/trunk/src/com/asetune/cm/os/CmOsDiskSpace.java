package com.asetune.cm.os;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang3.math.NumberUtils;
import org.apache.log4j.Logger;

import com.asetune.ICounterController;
import com.asetune.IGuiController;
import com.asetune.alarm.AlarmHandler;
import com.asetune.alarm.events.AlarmEventLowOsDiskFreeSpace;
import com.asetune.cm.CmSettingsHelper;
import com.asetune.cm.CmSettingsHelper.MapNumberValidator;
import com.asetune.cm.CounterModelHostMonitor;
import com.asetune.cm.CounterSetTemplates;
import com.asetune.cm.CounterSetTemplates.Type;
import com.asetune.cm.CountersModel;
import com.asetune.cm.os.gui.CmOsUptimePanel;
import com.asetune.gui.MainFrame;
import com.asetune.gui.TabularCntrPanel;
import com.asetune.hostmon.OsTable;
import com.asetune.utils.Configuration;
import com.asetune.utils.StringUtil;

public class CmOsDiskSpace
extends CounterModelHostMonitor
{
	private static Logger        _logger          = Logger.getLogger(CmOsDiskSpace.class);
	private static final long    serialVersionUID = 1L;

	public static final int      CM_TYPE          = CounterModelHostMonitor.HOSTMON_DISKSPACE;
	public static final String   CM_NAME          = CmOsDiskSpace.class.getSimpleName();
	public static final String   SHORT_NAME       = "OS Disk Space Usage(df)";
	public static final String   HTML_DESC        = 
		"<html>" +
		"Executes: 'df -k' on the Operating System" +
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

		return new CmOsDiskSpace(counterController, guiController);
	}

	public CmOsDiskSpace(ICounterController counterController, IGuiController guiController)
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


	@Override 
	public boolean discardDiffPctHighlighterOnAbsTable() 
	{
		// SHOW PCT values as RED even in ABS samples (because we only have ABD rows in this CM)
		return false; 
	}
	
	//------------------------------------------------------------
	// Implementation
	//------------------------------------------------------------
//	public static final String GRAPH_NAME_LOAD_AVERAGE     = "LoadAverage";
//	public static final String GRAPH_NAME_ADJ_LOAD_AVERAGE = "AdjLoadAverage";
//
//	private void addTrendGraphs()
//	{
//		// GRAPH
//		addTrendGraph(GRAPH_NAME_LOAD_AVERAGE,
//			"uptime: Load Average", 	                                    // Menu CheckBox text
//			"uptime: Load Average ("+GROUP_NAME+"->"+SHORT_NAME+")",    // Label 
//			new String[] { "loadAverage_1Min",    "loadAverage_5Min",    "loadAverage_15Min" }, 
//			LabelType.Static,
//			false, // is Percent Graph
//			false, // visible at start
//			0,     // graph is valid from Server Version. 0 = All Versions; >0 = Valid from this version and above 
//			-1);   // minimum height
//
//		// GRAPH
//		addTrendGraph(GRAPH_NAME_ADJ_LOAD_AVERAGE,
//			"uptime: Adjusted Load Average", 	                                    // Menu CheckBox text
//			"uptime: Adjusted Load Average ("+GROUP_NAME+"->"+SHORT_NAME+")",    // Label 
//			new String[] { "adjLoadAverage_1Min", "adjLoadAverage_5Min", "adjLoadAverage_15Min" }, 
//			LabelType.Static,
//			false, // is Percent Graph
//			false, // visible at start
//			0,     // graph is valid from Server Version. 0 = All Versions; >0 = Valid from this version and above 
//			-1);   // minimum height
//	}
	private void addTrendGraphs()
	{
	}
	
	@Override
	protected TabularCntrPanel createGui()
	{
		return new CmOsUptimePanel(this);
	}

	@Override
	public void localCalculation(OsTable newSample)
	{
//		System.out.println("localCalculation(OsTable thisSample): newSample.getColumnCount()="+newSample.getColumnCount()+", "+newSample.getColNames());

		int sizeKB_pos      = newSample.findColumn("Size-KB");
		int usedKB_pos      = newSample.findColumn("Used-KB");
		int availableKB_pos = newSample.findColumn("Available-KB");

		int sizeMB_pos      = newSample.findColumn("Size-MB");
		int usedMB_pos      = newSample.findColumn("Used-MB");
		int availableMB_pos = newSample.findColumn("Available-MB");

		int usedPct_pos     = newSample.findColumn("UsedPct");

		if (sizeKB_pos == -1 || usedKB_pos == -1 || availableKB_pos == -1 || sizeMB_pos == -1 || usedMB_pos == -1 || availableMB_pos == -1 || usedPct_pos == -1)
		{
			_logger.warn("Column position not available. sizeKB_pos="+sizeKB_pos+", usedKB_pos="+usedKB_pos+", availableKB_pos="+availableKB_pos+", sizeMB_pos="+sizeMB_pos+", usedMB_pos="+usedMB_pos+", availableMB_pos="+availableMB_pos+", usedPct_pos="+usedPct_pos+".");
			return;
		}
		
		for (int r=0; r<newSample.getRowCount(); r++)
		{
			Number sizeKB_num      = (Number) newSample.getValueAt(r, sizeKB_pos);
			Number usedKB_num      = (Number) newSample.getValueAt(r, usedKB_pos);
			Number availableKB_num = (Number) newSample.getValueAt(r, availableKB_pos);

			if (sizeKB_num      != null) newSample.setValueAt(new Integer(sizeKB_num     .intValue()/1024), r, sizeMB_pos);
			if (usedKB_num      != null) newSample.setValueAt(new Integer(usedKB_num     .intValue()/1024), r, usedMB_pos);
			if (availableKB_num != null) newSample.setValueAt(new Integer(availableKB_num.intValue()/1024), r, availableMB_pos);

			// Calculate the Pct value with a higher (scale=1) resolution than df
			if (sizeKB_num != null && usedKB_num != null && availableKB_num != null)
			{
				if (sizeKB_num.intValue() > 0)
				{
//					double pct = usedKB_num.doubleValue() / sizeKB_num.doubleValue() * 100.0;
					double pct = 100.0 - (availableKB_num.doubleValue() / sizeKB_num.doubleValue() * 100.0);
					if (pct <= 0)
						pct = 0;
					if (pct > 100)
						pct = 100;

					BigDecimal bd =  new BigDecimal( pct ).setScale(1, BigDecimal.ROUND_HALF_EVEN);
					newSample.setValueAt(bd, r, usedPct_pos);
				}
			}
		}
	}

//	@Override
//	public void updateGraphData(TrendGraphDataPoint tgdp)
//	{
//		if (GRAPH_NAME_LOAD_AVERAGE.equals(tgdp.getName()))
//		{
//			Double[] arr = new Double[3];
//
//			// NOTE: only ABS values are present in CounterModelHostMonitor
//			arr[0] = this.getAbsValueAvg("loadAverage_1Min");
//			arr[1] = this.getAbsValueAvg("loadAverage_5Min");
//			arr[2] = this.getAbsValueAvg("loadAverage_15Min");
//
//			tgdp.setDataPoint(this.getTimestamp(), arr);
//		}
//
//		if (GRAPH_NAME_ADJ_LOAD_AVERAGE.equals(tgdp.getName()))
//		{
//			Double[] arr = new Double[3];
//
//			// NOTE: only ABS values are present in CounterModelHostMonitor
//			arr[0] = this.getAbsValueAvg("adjLoadAverage_1Min");
//			arr[1] = this.getAbsValueAvg("adjLoadAverage_5Min");
//			arr[2] = this.getAbsValueAvg("adjLoadAverage_15Min");
//
//			tgdp.setDataPoint(this.getTimestamp(), arr);
//		}
//	}
	

	@Override
	public void sendAlarmRequest()
	{
		if ( ! hasAbsData() )
			return;
		
		if ( ! AlarmHandler.hasInstance() )
			return;

		CountersModel cm = this;

		//boolean debugPrint = System.getProperty("sendAlarmRequest.debug", "false").equalsIgnoreCase("true");

		for (int r=0; r<cm.getAbsRowCount(); r++)
		{
		//	String fsName     = cm.getAbsString(r, "Filesystem");
			String mountPoint = cm.getAbsString(r, "MountedOn");

			//-------------------------------------------------------
			// LowFreeSpaceInMb
			//-------------------------------------------------------
			if (isSystemAlarmsForColumnEnabledAndInTimeRange("Available-MB"))
			{
				Double freeMb     = cm.getAbsValueAsDouble(r, "Available-MB");
				Double usedPct    = cm.getAbsValueAsDouble(r, "UsedPct");
				Number threshold = getFreeSpaceThreshold(mountPoint, _map_alarm_LowFreeSpaceInMb); // This uses dbname.matches(map:anyKey)
				if (usedPct != null && usedPct != null && threshold != null)
				{
					if (freeMb.intValue() < threshold.intValue())
					{
						AlarmHandler.getInstance().addAlarm( new AlarmEventLowOsDiskFreeSpace(cm, mountPoint, freeMb.intValue(), usedPct, threshold.intValue()) );
					}
				}
			}

			//-------------------------------------------------------
			// LowFreeSpaceInPct
			//-------------------------------------------------------
			if (isSystemAlarmsForColumnEnabledAndInTimeRange("UsedPct"))
			{
				Double freeMb     = cm.getAbsValueAsDouble(r, "Available-MB");
				Double usedPct    = cm.getAbsValueAsDouble(r, "UsedPct");
				Number threshold = getFreeSpaceThreshold(mountPoint, _map_alarm_LowFreeSpaceInPct); // This uses dbname.matches(map:anyKey)
				if (usedPct != null && usedPct != null && threshold != null)
				{
					if (usedPct > threshold.doubleValue())
					{
						AlarmHandler.getInstance().addAlarm( new AlarmEventLowOsDiskFreeSpace(cm, mountPoint, freeMb.intValue(), usedPct, threshold.doubleValue()) );
					}
				}
			}
		}
	}

	/**
	 * Helper method to get the Threshold for a specific DB, using direct access to map or by check all key values in map with regexp...
	 * 
	 * @param dbname
	 * @param map
	 * @return
	 */
	private Number getFreeSpaceThreshold(String name, Map<String, Number> map)
	{
    	if (map == null)
    	{
    		_logger.warn("getFreeSpaceThreshold(name=|"+name+"|, map=|"+map+"|). map is NULL, which wasn't expected... some initialization must have failed.");
    		return null;
    	}

		if (map.isEmpty())
			return null;

		if (StringUtil.isNullOrBlank(name))
			return null;

		// Lookup the map for DIRECT match
		Number num = map.get(name);
		if (num != null)
			return num;

		// Check all key in the match and check if they match the REGEXP in the key of the map
		for (String key : map.keySet())
		{
			if (name.matches(key))
				return map.get(key);
		}
		
		// no match
		return null;
	}


//	private Map<String, Number> _map_alarm_LowFreeSpaceInMb   = new HashMap<>();
//	private Map<String, Number> _map_alarm_LowFreeSpaceInPct  = new HashMap<>();
	private Map<String, Number> _map_alarm_LowFreeSpaceInMb; // Note: do NOT initialize this here... since the initAlarms() is done in super, if initialized it will be overwritten here...
	private Map<String, Number> _map_alarm_LowFreeSpaceInPct;// Note: do NOT initialize this here... since the initAlarms() is done in super, if initialized it will be overwritten here...
	
	/**
	 * Initialize stuff that has to do with alarms
	 */
	@Override
	public void initAlarms()
	{
		Configuration conf = Configuration.getCombinedConfiguration();
		String cfgVal;

		_map_alarm_LowFreeSpaceInMb   = new HashMap<>();
		_map_alarm_LowFreeSpaceInPct  = new HashMap<>();
		
		String prefix = "       ";

		//--------------------------------------
		// LowFreeSpaceInMb
		cfgVal = conf.getProperty(PROPKEY_alarm_LowFreeSpaceInMb, DEFAULT_alarm_LowFreeSpaceInMb);
		if (StringUtil.hasValue(cfgVal))
		{
			Map<String, String> map = StringUtil.parseCommaStrToMap(cfgVal);
			if (_logger.isDebugEnabled())
				_logger.debug(prefix + "Initializing alarm 'LowFreeSpaceInMb'. After parseCommaStrToMap, map looks like: "+map);
			
			for (String key : map.keySet())
			{
				String val = map.get(key);
				
				try
				{
					int mb = NumberUtils.createNumber(val).intValue();
					_map_alarm_LowFreeSpaceInMb.put(key, mb);

					_logger.info(prefix + "Initializing alarm. Using 'LowFreeSpaceInMb', mountPoint='"+key+"', mb="+mb);
				}
				catch (NumberFormatException ex)
				{
					_logger.info(prefix + "Initializing alarm. Skipping 'LowFreeSpaceInMb' enty mountPoint='"+key+"', val='"+val+"'. The value is not a number.");
				}
			}
		}
		
		//--------------------------------------
		// LowFreeSpaceInPct
		cfgVal = conf.getProperty(PROPKEY_alarm_LowFreeSpaceInPct, DEFAULT_alarm_LowFreeSpaceInPct);
		if (StringUtil.hasValue(cfgVal))
		{
			Map<String, String> map = StringUtil.parseCommaStrToMap(cfgVal);
			if (_logger.isDebugEnabled())
				_logger.debug(prefix + "Initializing alarm 'LowFreeSpaceInPct'. After parseCommaStrToMap, map looks like: "+map);
			
			for (String key : map.keySet())
			{
				String val = map.get(key);
				
				try
				{
					double pct = NumberUtils.createNumber(val).doubleValue();
					_map_alarm_LowFreeSpaceInPct.put(key, pct);

					_logger.info(prefix + "Initializing alarm. Using 'LowFreeSpaceInPct', mountPoint='"+key+"', pct="+pct);
				}
				catch (NumberFormatException ex)
				{
					_logger.info(prefix + "Initializing alarm. Skipping 'LowFreeSpaceInPct' enty mountPoint='"+key+"', val='"+val+"'. The value is not a number.");
				}
			}
		}
	}

	public static final String  PROPKEY_alarm_LowFreeSpaceInMb              = CM_NAME + ".alarm.system.if.Available-MB.lt";
//	public static final String  DEFAULT_alarm_LowFreeSpaceInMb              = ".*=2, tempdb=100";
	public static final String  DEFAULT_alarm_LowFreeSpaceInMb              = "";
                                                                              
	public static final String  PROPKEY_alarm_LowFreeSpaceInPct             = CM_NAME + ".alarm.system.if.UsedPct.gt";
//	public static final String  DEFAULT_alarm_LowFreeSpaceInPct             = "tempdb=80";
//	public static final String  DEFAULT_alarm_LowFreeSpaceInPct             = ".*=99.1";
	public static final String  DEFAULT_alarm_LowFreeSpaceInPct             = "";
                                                                              
	@Override
	public List<CmSettingsHelper> getLocalAlarmSettings()
	{
		Configuration conf = Configuration.getCombinedConfiguration();
		List<CmSettingsHelper> list = new ArrayList<>();
		
		list.add(new CmSettingsHelper("LowFreeSpaceInMb",  PROPKEY_alarm_LowFreeSpaceInMb , String.class, conf.getProperty(PROPKEY_alarm_LowFreeSpaceInMb , DEFAULT_alarm_LowFreeSpaceInMb ), DEFAULT_alarm_LowFreeSpaceInMb , "If 'Available-MB' is less than ## then send 'AlarmEventLowOsDiskFreeSpace'. format: mountPoint1=#, mountPoint2=#, mountPoint3=#  (Note: the 'mountPoint' can use regexp)",      new MapNumberValidator()));
		list.add(new CmSettingsHelper("LowFreeSpaceInPct", PROPKEY_alarm_LowFreeSpaceInPct, String.class, conf.getProperty(PROPKEY_alarm_LowFreeSpaceInPct, DEFAULT_alarm_LowFreeSpaceInPct), DEFAULT_alarm_LowFreeSpaceInPct, "If 'UsedPct' is greater than ##.# Percent then send 'AlarmEventLowOsDiskFreeSpace'.format: mountPoint1=#, mountPoint2=#, mountPoint3=#  (Note: the 'mountPoint' can use regexp)", new MapNumberValidator()));

		return list;
	}
}