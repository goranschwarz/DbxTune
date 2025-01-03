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

import java.lang.invoke.MethodHandles;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.ConcurrentHashMap;

import org.apache.commons.lang3.math.NumberUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.dbxtune.ICounterController;
import com.dbxtune.IGuiController;
import com.dbxtune.alarm.AlarmHandler;
import com.dbxtune.alarm.events.AlarmEvent;
import com.dbxtune.alarm.events.AlarmEventLowOsDiskFreeSpace;
import com.dbxtune.central.pcs.CentralPersistReader;
import com.dbxtune.cm.CmSettingsHelper;
import com.dbxtune.cm.CmSettingsHelper.MapNumberValidator;
import com.dbxtune.cm.CmSettingsHelper.RegExpInputValidator;
import com.dbxtune.cm.CounterModelHostMonitor;
import com.dbxtune.cm.CounterSetTemplates;
import com.dbxtune.cm.CounterSetTemplates.Type;
import com.dbxtune.cm.CountersModel;
import com.dbxtune.cm.os.gui.CmOsDiskSpacePanel;
import com.dbxtune.graph.TrendGraphDataPoint;
import com.dbxtune.graph.TrendGraphDataPoint.Category;
import com.dbxtune.graph.TrendGraphDataPoint.LabelType;
import com.dbxtune.gui.MainFrame;
import com.dbxtune.gui.TabularCntrPanel;
import com.dbxtune.hostmon.OsTable;
import com.dbxtune.utils.CollectionUtils;
import com.dbxtune.utils.Configuration;
import com.dbxtune.utils.StringUtil;

public class CmOsDiskSpace
extends CounterModelHostMonitor
{
	private static final Logger _logger = LogManager.getLogger(MethodHandles.lookup().lookupClass());
	private static final long    serialVersionUID = 1L;

	public static final int      CM_TYPE          = CounterModelHostMonitor.HOSTMON_DISKSPACE;
	public static final String   CM_NAME          = CmOsDiskSpace.class.getSimpleName();
	public static final String   SHORT_NAME       = "OS Disk Space Usage(df)";
	public static final String   HTML_DESC        = 
		"<html>" +
		"Executes: 'df -k' on the Operating System" +
		"</html>";

	public static final String   GROUP_NAME       = MainFrame.TCP_GROUP_HOST_MONITOR;
	public static final String   GUI_ICON_FILE    = "images/" + CM_NAME + ".png";

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
			guiController.splashWindowProgress("Loading: Counter Model '" + CM_NAME + "'");

		return new CmOsDiskSpace(counterController, guiController);
	}

	public CmOsDiskSpace(ICounterController counterController, IGuiController guiController)
	{
		super(CM_NAME, GROUP_NAME, CM_TYPE, null, NEGATIVE_DIFF_COUNTERS_TO_ZERO, IS_SYSTEM_CM, DEFAULT_POSTPONE_TIME);

		setDisplayName(SHORT_NAME);
		setDescription(HTML_DESC);

		setIconFile(GUI_ICON_FILE);

		setCounterController(counterController);
		setGuiController(guiController);
		
		// Normally for HostMonitor is ABS
		setDataSource(DATA_RATE, false);
//		setDataSource(DATA_ABS, false);
		
		addTrendGraphs();
		
		CounterSetTemplates.register(this);
	}

	@Override
	protected TabularCntrPanel createGui()
	{
		return new CmOsDiskSpacePanel(this);
	}

	//------------------------------------------------------------
	// Implementation
	//------------------------------------------------------------
	public static final String GRAPH_NAME_USED_MB      = "FsUsedMb";
	public static final String GRAPH_NAME_USED_PCT     = "FsUsedPct";
	public static final String GRAPH_NAME_AVAILABLE_MB = "FsAvailableMb";

	private void addTrendGraphs()
	{
		// GRAPH
		addTrendGraph(GRAPH_NAME_USED_MB,
			"df: Space Used in MB, at MountPoint", 	                                    // Menu CheckBox text
			"df: Space Used in MB, at MountPoint (" + GROUP_NAME + "->" + SHORT_NAME + ")",    // Label 
			TrendGraphDataPoint.createGraphProps(TrendGraphDataPoint.Y_AXIS_SCALE_LABELS_MB, CentralPersistReader.SampleType.MAX_OVER_SAMPLES),
			null, 
			LabelType.Dynamic,
			Category.DISK,
			false, // is Percent Graph
			false, // visible at start
			0,     // graph is valid from Server Version. 0 = All Versions; >0 = Valid from this version and above 
			-1);   // minimum height

		// GRAPH
		addTrendGraph(GRAPH_NAME_USED_PCT,
			"df: Space Used in Percent, at MountPoint", 	                                    // Menu CheckBox text
			"df: Space Used in Percent, at MountPoint (" + GROUP_NAME + "->" + SHORT_NAME + ")",    // Label 
			TrendGraphDataPoint.createGraphProps(TrendGraphDataPoint.Y_AXIS_SCALE_LABELS_PERCENT, CentralPersistReader.SampleType.MAX_OVER_SAMPLES),
			null, 
			LabelType.Dynamic,
			Category.DISK,
			true,  // is Percent Graph
			false, // visible at start
			0,     // graph is valid from Server Version. 0 = All Versions; >0 = Valid from this version and above 
			-1);   // minimum height

		// GRAPH
		addTrendGraph(GRAPH_NAME_AVAILABLE_MB,
			"df: Space Available in MB, at MountPoint", 	                                    // Menu CheckBox text
			"df: Space Available in MB, at MountPoint (" + GROUP_NAME + "->" + SHORT_NAME + ")",    // Label 
			TrendGraphDataPoint.createGraphProps(TrendGraphDataPoint.Y_AXIS_SCALE_LABELS_MB, CentralPersistReader.SampleType.MIN_OVER_SAMPLES),
			null, 
			LabelType.Dynamic,
			Category.DISK,
			false, // is Percent Graph
			false, // visible at start
			0,     // graph is valid from Server Version. 0 = All Versions; >0 = Valid from this version and above 
			-1);   // minimum height

	}
	
	@Override
	public void localCalculation(OsTable newSample)
	{
//		System.out.println("localCalculation(OsTable thisSample): newSample.getColumnCount()=" + newSample.getColumnCount() + ", " + newSample.getColNames());

		int sizeKB_pos      = newSample.findColumn("Size-KB");
		int usedKB_pos      = newSample.findColumn("Used-KB");
		int availableKB_pos = newSample.findColumn("Available-KB");

		int sizeMB_pos      = newSample.findColumn("Size-MB");
		int usedMB_pos      = newSample.findColumn("Used-MB");
		int availableMB_pos = newSample.findColumn("Available-MB");

		int usedPct_pos     = newSample.findColumn("UsedPct");

		int filesystem_pos  = newSample.findColumn("Filesystem");
		int mountedOn_pos   = newSample.findColumn("MountedOn");

		if (sizeKB_pos == -1 || usedKB_pos == -1 || availableKB_pos == -1 || sizeMB_pos == -1 || usedMB_pos == -1 || availableMB_pos == -1 || usedPct_pos == -1)
		{
			_logger.warn("Column position not available. sizeKB_pos=" + sizeKB_pos + ", usedKB_pos=" + usedKB_pos + ", availableKB_pos=" + availableKB_pos + ", sizeMB_pos=" + sizeMB_pos + ", usedMB_pos=" + usedMB_pos + ", availableMB_pos=" + availableMB_pos + ", usedPct_pos=" + usedPct_pos + ".");
			return;
		}
		
		for (int r=0; r<newSample.getRowCount(); r++)
		{
			Number sizeKB_num      = (Number) newSample.getValueAt(r, sizeKB_pos);
			Number usedKB_num      = (Number) newSample.getValueAt(r, usedKB_pos);
			Number availableKB_num = (Number) newSample.getValueAt(r, availableKB_pos);

			// NOTE: beware of integer overflow... use |(int)(var.longValue()/1024)| instead of |var.intValue()/1024| ... 
			//       Number.intVal() may cause integer overflow and return a negative number
			if (sizeKB_num      != null) newSample.setValueAt(Integer.valueOf((int)(sizeKB_num     .longValue()/1024)), r, sizeMB_pos);
			if (usedKB_num      != null) newSample.setValueAt(Integer.valueOf((int)(usedKB_num     .longValue()/1024)), r, usedMB_pos);
			if (availableKB_num != null) newSample.setValueAt(Integer.valueOf((int)(availableKB_num.longValue()/1024)), r, availableMB_pos);

			// Calculate the Pct value with a higher (scale=1) resolution than df
			if (sizeKB_num != null && usedKB_num != null && availableKB_num != null)
			{
				if (sizeKB_num.longValue() > 0)
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
			
			if (filesystem_pos != -1 && mountedOn_pos != -1)
			{
				String filesystem = (String) newSample.getValueAt(r, filesystem_pos);
				String mountedOn  = (String) newSample.getValueAt(r, mountedOn_pos);
				setDiskDescription(filesystem, mountedOn);
			}
		} // end: loop rows
	} // end: method


	/** A map (specific for Windows machines. Map&lt;DiskDrive, DiskLabel&gt; */
	private Map<String, String> _diskIdToLabel = new ConcurrentHashMap<>();
	
	private void setDiskDescription(String driveName, String labelName)
	{
		if (_diskIdToLabel == null)
			return;

		// Table typically looks like
		// +----------+-------------+-------+------------+---------+-------+------------+-------+-----------------+
		// |Filesystem|Size-KB      |Used-KB|Available-KB|Size-MB  |Used-MB|Available-MB|UsedPct|MountedOn        |
		// +----------+-------------+-------+------------+---------+-------+------------+-------+-----------------+
		// |C:        |  104 292 348|      8|           0|  101 847|      0|           0|   73,2|C:               |
		// |D:        |2 097 148 924|      0|           0|2 047 996|      0|           0|    1,2|D: [SQL Data]    |
		// |E:        |  209 712 124|      0|           0|  204 796|      0|           0|   61,5|E: [SQL Log]     |
		// |F:        |  524 284 924|      0|           0|  511 996|      0|           0|   35,8|F: [SQL Backup]  |
		// |G:        |  104 854 524|      0|           0|  102 396|      0|           0|   41,4|G: [Temp DB Data]|
		// |H:        |   10 482 684|      0|           0|   10 236|      0|           0|   86,2|H: [Temp DB Log] |
		// |I:        |3 145 709 564|      0|           0|3 071 981|      0|           0|     76|I: [Data]        |
		// +----------+-------------+-------+------------+---------+-------+------------+-------+-----------------+

		// get DRIVE-NAME
		String tmpDriveName = driveName.trim();
		if (tmpDriveName.endsWith(":"))
		{
			tmpDriveName = tmpDriveName.substring(0, tmpDriveName.length()-1); // looks like "D:"  -- remove last ":"
			tmpDriveName = tmpDriveName.trim();
		}


		// get LABEL-NAME
		String tmpLabelName = labelName.trim();
		if (tmpLabelName.indexOf('[') != -1 && tmpLabelName.indexOf(']') != -1)
		{
			tmpLabelName = StringUtil.substringBetweenTwoChars(tmpLabelName, "[", "]");
			tmpLabelName = tmpLabelName.trim();
		}
		
		_diskIdToLabel.put(tmpDriveName, tmpLabelName);
	}
	/**
	 * Get a "Label" for a specific disk drive
	 * @param driveName       Name of the drive, for example "D", "D:", "D:\", "1 D:"
	 * @param doFormat        If you want to format in the following way: "$driveName [$label]"
	 * 
	 * @return 
	 * <ul>
	 *   <li>The label name of the driveName</li>
	 *   <li>or if doFormat=true "$driveName [$label]"</li>
	 *   <li>If the driveName can't be resolved, simply return the inputed "driveName"</li>
	 * </ul>
	 * 
	 * This may typically be called from: CmOsIoStat -- to Resolve "1 D:" --> "1 D: [labelName]"
	 */
	public String getDiskDescription(String driveName, boolean doFormat)
	{
		if (StringUtil.isNullOrBlank(driveName))
			return driveName;

		if (_diskIdToLabel == null)
			return driveName;
		
		if (_diskIdToLabel.isEmpty())
			return driveName;
		
		// 
		String tmpDriveName = driveName.trim();
		if (tmpDriveName.matches("^[0-9] .*")) tmpDriveName = tmpDriveName.substring(2);                          // looks like "1 D:" -- remove "1 "
		if (tmpDriveName.endsWith("\\"))       tmpDriveName = tmpDriveName.substring(0, tmpDriveName.length()-1); // looks like "D:\"  -- remove last "\"
		if (tmpDriveName.endsWith(":"))        tmpDriveName = tmpDriveName.substring(0, tmpDriveName.length()-1); // looks like "D:"   -- remove last ":"

		String labelName = _diskIdToLabel.get(tmpDriveName);
		if (labelName == null)
		{
			// NOT Found, return input...
			return driveName;
		}
		else
		{
			if (doFormat)
				return driveName + " [" + labelName + "]";

			return labelName;
		}
	}

	@Override
	public void updateGraphData(TrendGraphDataPoint tgdp)
	{
		if (GRAPH_NAME_USED_MB.equals(tgdp.getName()))
		{
			// Write 1 "line" for every device
			Double[] dArray = new Double[this.getRowCount()];
			String[] lArray = new String[dArray.length];
			for (int i = 0; i < dArray.length; i++)
			{
				lArray[i] = this.getAbsString       (i, "MountedOn");
				dArray[i] = this.getAbsValueAsDouble(i, "Used-MB");
			}

			// Set the values
			tgdp.setDataPoint(this.getTimestamp(), lArray, dArray);
		}
		
		if (GRAPH_NAME_USED_PCT.equals(tgdp.getName()))
		{
			// Write 1 "line" for every device
			Double[] dArray = new Double[this.getRowCount()];
			String[] lArray = new String[dArray.length];
			for (int i = 0; i < dArray.length; i++)
			{
				lArray[i] = this.getAbsString       (i, "MountedOn");
				dArray[i] = this.getAbsValueAsDouble(i, "UsedPct");
			}

			// Set the values
			tgdp.setDataPoint(this.getTimestamp(), lArray, dArray);
		}
		
		if (GRAPH_NAME_AVAILABLE_MB.equals(tgdp.getName()))
		{
			// Write 1 "line" for every device
			Double[] dArray = new Double[this.getRowCount()];
			String[] lArray = new String[dArray.length];
			for (int i = 0; i < dArray.length; i++)
			{
				lArray[i] = this.getAbsString       (i, "MountedOn");
				dArray[i] = this.getAbsValueAsDouble(i, "Available-MB");
			}

			// Set the values
			tgdp.setDataPoint(this.getTimestamp(), lArray, dArray);
		}
	}
	

	@Override
	public void sendAlarmRequest()
	{
		if ( ! hasAbsData() )
			return;
		
		if ( ! AlarmHandler.hasInstance() )
			return;

		boolean debugPrint = Configuration.getCombinedConfiguration().getBooleanProperty("sendAlarmRequest.debug", _logger.isDebugEnabled());

		CountersModel cm = this;
		Configuration conf = Configuration.getCombinedConfiguration();
		
		double sizeMbSkipThreshold = conf.getDoubleProperty(PROPKEY_alarm_SkipSizeBelowMb,    DEFAULT_alarm_SkipSizeBelowMb);
		String skipRegExp          = conf.getProperty(      PROPKEY_alarm_SkipMountNameRegex, DEFAULT_alarm_SkipMountNameRegex);


		//boolean debugPrint = System.getProperty("sendAlarmRequest.debug", "false").equalsIgnoreCase("true");

		for (int r=0; r<cm.getAbsRowCount(); r++)
		{
		//	String fsName     = cm.getAbsString(r, "Filesystem");
			String mountPoint = cm.getAbsString(r, "MountedOn");

			// SKIP: empty
			if (StringUtil.isNullOrBlank(mountPoint))
					continue;

			// SKIP: Read Only mount points
			//NOT-YET-IMPLEMENTED: on init get mount points that are in RO and add them to a "set", which we check here... 
//			if (_roMountPoints != null && _roMountPoints.contains(mountPoint))
//			{
//				if (_logger.isDebugEnabled())
//					_logger.debug("sendAlarmRequest(): skipping[ro-mount-pont]: mountPoint=" + mountPoint + ", _roMountPoints=" + _roMountPoints);
//				continue;
//			}

			// SKIP: NFS mounts 
			//NOT-YET-IMPLEMENTED: (should we do this or not) 
			
			
			// SKIP: if in skipRegExp
			if (StringUtil.hasValue(skipRegExp) && mountPoint.matches(skipRegExp))
			{
				if (debugPrint || _logger.isDebugEnabled())
					_logger.debug("sendAlarmRequest(): skipping[skipRegExp]: mountPoint=" + mountPoint + ", " + PROPKEY_alarm_SkipMountNameRegex + "=" + skipRegExp);
				continue;
			}
			
			// SKIP: Should we skip any mount points with LESS than X MB
			Double sizeMb = cm.getAbsValueAsDouble(r, "Size-MB");
			if (sizeMb != null && sizeMb < sizeMbSkipThreshold)
			{
				if (debugPrint || _logger.isDebugEnabled())
					_logger.debug("sendAlarmRequest(): skipping[sizeMb]: mountPoint=" + mountPoint + ", sizeMb=" + sizeMb + ", less than threshold=" + sizeMbSkipThreshold);
				continue;
			}

			//-------------------------------------------------------
			// LowFreeSpaceInMb
			//-------------------------------------------------------
			if (isSystemAlarmsForColumnEnabledAndInTimeRange("Available-MB"))
			{
				Double freeMb     = cm.getAbsValueAsDouble(r, "Available-MB");
				Double usedPct    = cm.getAbsValueAsDouble(r, "UsedPct");
				Number threshold = getFreeSpaceThreshold(mountPoint, _map_alarm_LowFreeSpaceInMb); // This uses dbname.matches(map:anyKey)

				if (debugPrint || _logger.isDebugEnabled())
					System.out.println("##### sendAlarmRequest(" + cm.getName() + "): Available-MB - threshold=" + threshold + ", mountPoint='" + mountPoint + "', freeMb='" + freeMb + "', usedPct='" + usedPct + "'.");

				if (freeMb != null && usedPct != null && threshold != null)
				{
					if (freeMb.intValue() < threshold.intValue())
					{
						String extendedDescText = cm.toTextTableString(DATA_ABS, r);
						String extendedDescHtml = cm.toHtmlTableString(DATA_ABS, r, true, false, false);

						extendedDescHtml += "<br><br>" + cm.getGraphDataHistoryAsHtmlImage(GRAPH_NAME_AVAILABLE_MB);
						extendedDescHtml += "<br><br>" + cm.getGraphDataHistoryAsHtmlImage(GRAPH_NAME_USED_PCT);

						AlarmEvent ae = new AlarmEventLowOsDiskFreeSpace(cm, mountPoint, freeMb.intValue(), usedPct, threshold.intValue());
						ae.setExtendedDescription(extendedDescText, extendedDescHtml);

						AlarmHandler.getInstance().addAlarm(ae);
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

				if (debugPrint || _logger.isDebugEnabled())
					System.out.println("##### sendAlarmRequest(" + cm.getName() + "): UsedPct - threshold=" + threshold + ", mountPoint='" + mountPoint + "', freeMb='" + freeMb + "', usedPct='" + usedPct + "'.");

				if (freeMb != null && usedPct != null && threshold != null)
				{
					if (usedPct > threshold.doubleValue())
					{
						String extendedDescText = cm.toTextTableString(DATA_ABS, r);
						String extendedDescHtml = cm.toHtmlTableString(DATA_ABS, r, true, false, false);
						       
						extendedDescHtml += "<br><br>" + cm.getGraphDataHistoryAsHtmlImage(GRAPH_NAME_AVAILABLE_MB);
						extendedDescHtml += "<br><br>" + cm.getGraphDataHistoryAsHtmlImage(GRAPH_NAME_USED_PCT);

						AlarmEvent ae = new AlarmEventLowOsDiskFreeSpace(cm, mountPoint, freeMb.intValue(), usedPct, threshold.doubleValue());
						ae.setExtendedDescription(extendedDescText, extendedDescHtml);

						AlarmHandler.getInstance().addAlarm(ae);
					}
				}
			}
		}
	}

	@Override
	public boolean isGraphDataHistoryEnabled(String name)
	{
		// ENABLED for the following graphs
		if (GRAPH_NAME_AVAILABLE_MB.equals(name)) return true;
		if (GRAPH_NAME_USED_PCT    .equals(name)) return true;

		// default: DISABLED
		return false;
	}
	@Override
	public int getGraphDataHistoryTimeInterval(String name)
	{
		// Keep interval: default is 60 minutes
		return super.getGraphDataHistoryTimeInterval(name);
	}

	
	/**
	 * Helper method to get the Threshold for a specific "mount point", using direct access to map or by check all key values in map with regexp...
	 * 
	 * @param dbname
	 * @param map
	 * @return
	 */
	private Number getFreeSpaceThreshold(String name, Map<String, Number> map)
	{
    	if (map == null)
    	{
    		_logger.warn("getFreeSpaceThreshold(name=|" + name + "|, map=|" + map + "|). map is NULL, which wasn't expected... some initialization must have failed.");
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
//		for (String key : map.keySet())
//		{
//			if (name.matches(key))
//				return map.get(key);
//		}

    	// Check all key in the match and check if they match the REGEXP in the key of the map
    	for (String key : map.keySet())
		{
			if (name.matches(key))
			{
				Number val= map.get(key);
				
				if (_logger.isDebugEnabled())
					_logger.debug("<<<--  <<-MATCH: getFreeSpaceThreshold() name='" + name + "', matches='" + key + "', returns: " + val);

				return val;
			}
			else
			{
				if (_logger.isDebugEnabled())
					_logger.debug("   --  NO-MATCH: getFreeSpaceThreshold() name='" + name + "', regex='" + key + "'.");
			}
		}
		
		// no match
		return null;
	}


//	private Map<String, Number> _map_alarm_LowFreeSpaceInMb   = new HashMap<>();
//	private Map<String, Number> _map_alarm_LowFreeSpaceInPct  = new HashMap<>();
	private Map<String, Number> _map_alarm_LowFreeSpaceInMb; // Note: do NOT initialize this here... since the initAlarms() is done in super, if initialized it will be overwritten here...
	private Map<String, Number> _map_alarm_LowFreeSpaceInPct;// Note: do NOT initialize this here... since the initAlarms() is done in super, if initialized it will be overwritten here...
	
//	private Set<String> _roMountPoints;// Note: do NOT initialize this here... since the initAlarms() is done in super, if initialized it will be overwritten here...

	/**
	 * Initialize stuff that has to do with alarms
	 */
	@Override
	public void initAlarms()
	{
		Configuration conf = Configuration.getCombinedConfiguration();
		String cfgVal;

		_map_alarm_LowFreeSpaceInMb   = new LinkedHashMap<>();
		_map_alarm_LowFreeSpaceInPct  = new LinkedHashMap<>();
		
		String prefix = "       ";

		//--------------------------------------
		// TODO: get RO mount points
		// maybe call OS and get mount points that are RO
		//  * Linux: grep "[[:space:]]ro[[:space:],]" /proc/mounts | awk '{print $2}'
// Not sure if actually are connected at this stage...
// Implement this LATER
//		if (CounterController.getInstance().isHostMonConnected())
//		{
//			if (isConnectedToVendor(OsVendor.Linux))
//			{
//				try
//				{
//					SshConnection sshConn = CounterController.getInstance().getHostMonConnection();
//					String roMountPoints = sshConn.execCommandOutputAsStr("grep '[[:space:]]ro[[:space:],]' /proc/mounts | awk '{printf \"%s \", $2}'");
//
//					if (_roMountPoints == null)
//						_roMountPoints = new HashSet<>();
//
//					if (StringUtil.hasValue(roMountPoints))
//					{
//						String[] sa = roMountPoints.split(" ");
//						for (String str : sa)
//						{
//							if (StringUtil.hasValue(str))
//								_roMountPoints.add(str);
//						}
//					}
//				}
//				catch (IOException e)
//				{
//					// TODO Auto-generated catch block
//					e.printStackTrace();
//				}
//			}
//		}



		//--------------------------------------
		// LowFreeSpaceInMb
		cfgVal = conf.getProperty(PROPKEY_alarm_LowFreeSpaceInMb, DEFAULT_alarm_LowFreeSpaceInMb);
		if (StringUtil.hasValue(cfgVal))
		{
			Map<String, String> map = StringUtil.parseCommaStrToMap(cfgVal);
			if (_logger.isDebugEnabled())
				_logger.debug(prefix + "Initializing alarm 'LowFreeSpaceInMb'. After parseCommaStrToMap, map looks like: " + map);
			
			for (String key : map.keySet())
			{
				String val = map.get(key);
				
				try
				{
					int mb = NumberUtils.createNumber(val).intValue();
					_map_alarm_LowFreeSpaceInMb.put(key, mb);

					_logger.info(prefix + "Initializing alarm. Using 'LowFreeSpaceInMb', mountPoint='" + key + "', mb=" + mb);
				}
				catch (NumberFormatException ex)
				{
					_logger.info(prefix + "Initializing alarm. Skipping 'LowFreeSpaceInMb' enty mountPoint='" + key + "', val='" + val + "'. The value is not a number.");
				}
			}
			
			// Sort the MAP by value in descending order (high number first)
			_map_alarm_LowFreeSpaceInMb = CollectionUtils.sortByMapValueNumber(_map_alarm_LowFreeSpaceInMb, false);
			
			// Remove ".*" wild-card and add that to the *end*
			if (_map_alarm_LowFreeSpaceInMb.containsKey(".*"))
			{
				Number num = _map_alarm_LowFreeSpaceInMb.get(".*");
				_map_alarm_LowFreeSpaceInMb.put(".*", num);
			}

			_logger.info(prefix + "Evaluating alarm for 'LowFreeSpaceInMb' in the following order:");
			for (Entry<String, Number> entry : _map_alarm_LowFreeSpaceInMb.entrySet())
			{
				_logger.info(prefix + "    dbname='" + entry.getKey() + "', mb=" + entry.getValue());
			}
		}
		
		//--------------------------------------
		// LowFreeSpaceInPct
		cfgVal = conf.getProperty(PROPKEY_alarm_LowFreeSpaceInPct, DEFAULT_alarm_LowFreeSpaceInPct);
		if (StringUtil.hasValue(cfgVal))
		{
			Map<String, String> map = StringUtil.parseCommaStrToMap(cfgVal);
			if (_logger.isDebugEnabled())
				_logger.debug(prefix + "Initializing alarm 'LowFreeSpaceInPct'. After parseCommaStrToMap, map looks like: " + map);
			
			for (String key : map.keySet())
			{
				String val = map.get(key);
				
				try
				{
					double pct = NumberUtils.createNumber(val).doubleValue();
					_map_alarm_LowFreeSpaceInPct.put(key, pct);

					_logger.info(prefix + "Initializing alarm. Using 'LowFreeSpaceInPct', mountPoint='" + key + "', pct=" + pct);
				}
				catch (NumberFormatException ex)
				{
					_logger.info(prefix + "Initializing alarm. Skipping 'LowFreeSpaceInPct' enty mountPoint='" + key + "', val='" + val + "'. The value is not a number.");
				}
			}
			
			// Sort the MAP by value in descending order (high number first)
			_map_alarm_LowFreeSpaceInPct = CollectionUtils.sortByMapValueNumber(_map_alarm_LowFreeSpaceInPct, false);
			
			// Remove ".*" wild-card and add that to the *end*
			if (_map_alarm_LowFreeSpaceInPct.containsKey(".*"))
			{
				Number num = _map_alarm_LowFreeSpaceInPct.get(".*");
				_map_alarm_LowFreeSpaceInPct.put(".*", num);
			}

			_logger.info(prefix + "Evaluating alarm for 'LowFreeSpaceInPct' in the following order:");
			for (Entry<String, Number> entry : _map_alarm_LowFreeSpaceInPct.entrySet())
			{
				_logger.info(prefix + "    dbname='" + entry.getKey() + "', mb=" + entry.getValue());
			}
		}
	}

	public static final String  PROPKEY_alarm_SkipMountNameRegex            = CM_NAME + ".alarm.system.skip.MountNameRegex";
	public static final String  DEFAULT_alarm_SkipMountNameRegex            = "(/cdrom|/dummy)";

	public static final String  PROPKEY_alarm_SkipSizeBelowMb               = CM_NAME + ".alarm.system.skip.SizeBelowMb";
	public static final int     DEFAULT_alarm_SkipSizeBelowMb               = 512;

	public static final String  PROPKEY_alarm_LowFreeSpaceInMb              = CM_NAME + ".alarm.system.if.Available-MB.lt";
//	public static final String  DEFAULT_alarm_LowFreeSpaceInMb              = ".*=2, tempdb=100";
	public static final String  DEFAULT_alarm_LowFreeSpaceInMb              = ".*=10";

	public static final String  PROPKEY_alarm_LowFreeSpaceInPct             = CM_NAME + ".alarm.system.if.UsedPct.gt";
//	public static final String  DEFAULT_alarm_LowFreeSpaceInPct             = "tempdb=80";
//	public static final String  DEFAULT_alarm_LowFreeSpaceInPct             = ".*=99.1";
	public static final String  DEFAULT_alarm_LowFreeSpaceInPct             = ".*=95.0";

	@Override
	public List<CmSettingsHelper> getLocalAlarmSettings()
	{
		Configuration conf = Configuration.getCombinedConfiguration();
		List<CmSettingsHelper> list = new ArrayList<>();

		CmSettingsHelper.Type isAlarmSwitch = CmSettingsHelper.Type.IS_ALARM_SWITCH;
		CmSettingsHelper.Type isPreCheck    = CmSettingsHelper.Type.IS_PRE_CHECK;

		list.add(new CmSettingsHelper("SkipMountNameRegex", isPreCheck   , PROPKEY_alarm_SkipMountNameRegex, String .class, conf.getProperty   (PROPKEY_alarm_SkipMountNameRegex, DEFAULT_alarm_SkipMountNameRegex), DEFAULT_alarm_SkipMountNameRegex, "Skip entries where 'MountedOn' mathing this regexp", new RegExpInputValidator()));
		list.add(new CmSettingsHelper("SkipSizeBelowMb"   , isPreCheck   , PROPKEY_alarm_SkipSizeBelowMb   , Integer.class, conf.getIntProperty(PROPKEY_alarm_SkipSizeBelowMb   , DEFAULT_alarm_SkipSizeBelowMb   ), DEFAULT_alarm_SkipSizeBelowMb   , "Skip entries where 'Size-MB' is less than this value." ));

		list.add(new CmSettingsHelper("LowFreeSpaceInMb"  , isAlarmSwitch, PROPKEY_alarm_LowFreeSpaceInMb  , String.class,  conf.getProperty   (PROPKEY_alarm_LowFreeSpaceInMb  , DEFAULT_alarm_LowFreeSpaceInMb  ), DEFAULT_alarm_LowFreeSpaceInMb  , "If 'Available-MB' is less than ## then send 'AlarmEventLowOsDiskFreeSpace'. format: mountPoint1=#, mountPoint2=#, mountPoint3=#  (Note: the 'mountPoint' can use regexp)",      new MapNumberValidator()));
		list.add(new CmSettingsHelper("LowFreeSpaceInPct" , isAlarmSwitch, PROPKEY_alarm_LowFreeSpaceInPct , String.class,  conf.getProperty   (PROPKEY_alarm_LowFreeSpaceInPct , DEFAULT_alarm_LowFreeSpaceInPct ), DEFAULT_alarm_LowFreeSpaceInPct , "If 'UsedPct' is greater than ##.# Percent then send 'AlarmEventLowOsDiskFreeSpace'.format: mountPoint1=#, mountPoint2=#, mountPoint3=#  (Note: the 'mountPoint' can use regexp)", new MapNumberValidator()));

		return list;
	}
}
