package com.asetune.cm.os;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import com.asetune.ICounterController;
import com.asetune.IGuiController;
import com.asetune.cm.CmSettingsHelper;
import com.asetune.cm.CounterModelHostMonitor;
import com.asetune.cm.CounterSetTemplates;
import com.asetune.cm.CounterSetTemplates.Type;
import com.asetune.cm.CountersModel;
import com.asetune.cm.os.gui.CmOsIostatPanel;
import com.asetune.cm.os.gui.IoStatDeviceMapperDialog;
import com.asetune.graph.TrendGraphDataPoint;
import com.asetune.graph.TrendGraphDataPoint.LabelType;
import com.asetune.gui.MainFrame;
import com.asetune.gui.TabularCntrPanel;
import com.asetune.gui.TrendGraph;
import com.asetune.hostmon.HostMonitor;
import com.asetune.hostmon.OsTable;
import com.asetune.utils.Configuration;
import com.asetune.utils.StringUtil;

/**
 * @author Goran Schwarz (goran_schwarz@hotmail.com)
 */
public class CmOsIostat
extends CounterModelHostMonitor
{
//	private static Logger        _logger          = Logger.getLogger(CmOsIostat.class);
	private static final long    serialVersionUID = 1L;

	public static final int      CM_TYPE          = CounterModelHostMonitor.HOSTMON_IOSTAT;
	public static final String   CM_NAME          = CmOsIostat.class.getSimpleName();
	public static final String   SHORT_NAME       = "OS Disk Stat(iostat)";
	public static final String   HTML_DESC        = 
		"<html>" +
		"Executes: 'iostat' on the Operating System" +
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

		return new CmOsIostat(counterController, guiController);
	}

	public CmOsIostat(ICounterController counterController, IGuiController guiController)
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

	public static final String  PROPKEY_linux_opt_N = "CmOsIostat.linux.opt.N";
	public static final boolean DEFAULT_linux_opt_N = false;

	public static final String  PROPKEY_excludeDevices = "CmOsIostat.exclude.devices";
	public static final boolean DEFAULT_excludeDevices = false;

	public static final String  PROPKEY_excludeDevicesRegExp = "CmOsIostat.exclude.devices.regexp";
	public static final String  DEFAULT_excludeDevicesRegExp = "sd[a-z]";

	/** Used by the: Create 'Offline Session' Wizard */
	@Override
	public List<CmSettingsHelper> getLocalSettings()
	{
		Configuration conf = Configuration.getCombinedConfiguration();
		List<CmSettingsHelper> list = new ArrayList<>();
		
		list.add(new CmSettingsHelper("Exclude some devices",           PROPKEY_excludeDevices      , Boolean.class, conf.getBooleanProperty(PROPKEY_excludeDevices      , DEFAULT_excludeDevices      ), DEFAULT_excludeDevices      , "Enable/Disable: Exclude devices by name" ));
		list.add(new CmSettingsHelper("Exclude some devices RegExp",    PROPKEY_excludeDevicesRegExp, String .class, conf.getProperty       (PROPKEY_excludeDevicesRegExp, DEFAULT_excludeDevicesRegExp), DEFAULT_excludeDevicesRegExp, "If Exclude is enabled (true), this is the regular expression to use when testing device names." ));
		list.add(new CmSettingsHelper("iostat switch: -N (Linux Only)", PROPKEY_linux_opt_N         , Boolean.class, conf.getBooleanProperty(PROPKEY_linux_opt_N         , DEFAULT_linux_opt_N         ), DEFAULT_linux_opt_N         , "Add Switch -N to iostat 'Display the registered device mapper names for any device mapper devices'. NOTE: Linux Only" ));

		return list;
	}


	@Override 
	public boolean discardDiffPctHighlighterOnAbsTable() 
	{
		// SHOW PCT values as RED even in ABS samples (because we only have ABD rows in this CM)
		return false; 
	}
	
	@Override
	protected TabularCntrPanel createGui()
	{
		return new CmOsIostatPanel(this);
	}
	
	//------------------------------------------------------------
	// Implementation
	//------------------------------------------------------------	
	public static final String GRAPH_NAME_WaitTime        = "IoWait";
	public static final String GRAPH_NAME_ReadWaitTime    = "IoReadWait";
	public static final String GRAPH_NAME_WriteWaitTime   = "IoWriteWait";
	public static final String GRAPH_NAME_SericeTime      = "IoServiceTime";
	public static final String GRAPH_NAME_QueueLength     = "IoQueueLength";
	public static final String GRAPH_NAME_BusyPct         = "IoBusyPct";
	
	public static final String GRAPH_NAME_ReadOp          = "IoReadOp";
	public static final String GRAPH_NAME_WriteOp         = "IoWriteOp";
	public static final String GRAPH_NAME_ReadKb          = "IoReadKb";
	public static final String GRAPH_NAME_WriteKb         = "IoWriteKb";
	public static final String GRAPH_NAME_AvgReadKbPerIo  = "IoAvgReadKbPerIo";
	public static final String GRAPH_NAME_AvgWriteKbPerIo = "IoAvgWriteKbPerIo";

	private void addTrendGraphs()
	{
////		String[] labels = new String[] { "runtime-replaced" };
//		String[] labels = TrendGraphDataPoint.RUNTIME_REPLACED_LABELS;
//		
//		addTrendGraphData(GRAPH_NAME_WaitTime,        new TrendGraphDataPoint(GRAPH_NAME_WaitTime,        labels, LabelType.Dynamic));
//		addTrendGraphData(GRAPH_NAME_ReadWaitTime,    new TrendGraphDataPoint(GRAPH_NAME_ReadWaitTime,    labels, LabelType.Dynamic));
//		addTrendGraphData(GRAPH_NAME_WriteWaitTime,   new TrendGraphDataPoint(GRAPH_NAME_WriteWaitTime,   labels, LabelType.Dynamic));
//		addTrendGraphData(GRAPH_NAME_SericeTime,      new TrendGraphDataPoint(GRAPH_NAME_SericeTime,      labels, LabelType.Dynamic));
//		addTrendGraphData(GRAPH_NAME_QueueLength,     new TrendGraphDataPoint(GRAPH_NAME_QueueLength,     labels, LabelType.Dynamic));
//		addTrendGraphData(GRAPH_NAME_BusyPct,         new TrendGraphDataPoint(GRAPH_NAME_BusyPct,         labels, LabelType.Dynamic));
//
//		addTrendGraphData(GRAPH_NAME_ReadOp,          new TrendGraphDataPoint(GRAPH_NAME_ReadOp,          labels, LabelType.Dynamic));
//		addTrendGraphData(GRAPH_NAME_WriteOp,         new TrendGraphDataPoint(GRAPH_NAME_WriteOp,         labels, LabelType.Dynamic));
//		addTrendGraphData(GRAPH_NAME_ReadKb,          new TrendGraphDataPoint(GRAPH_NAME_ReadKb,          labels, LabelType.Dynamic));
//		addTrendGraphData(GRAPH_NAME_WriteKb,         new TrendGraphDataPoint(GRAPH_NAME_WriteKb,         labels, LabelType.Dynamic));
//		addTrendGraphData(GRAPH_NAME_AvgReadKbPerIo,  new TrendGraphDataPoint(GRAPH_NAME_AvgReadKbPerIo,  labels, LabelType.Dynamic));
//		addTrendGraphData(GRAPH_NAME_AvgWriteKbPerIo, new TrendGraphDataPoint(GRAPH_NAME_AvgWriteKbPerIo, labels, LabelType.Dynamic));

		// GRAPH
		addTrendGraph(GRAPH_NAME_WaitTime,
			"iostat: Wait Time(await) per Device",                                           // Menu CheckBox text
			"iostat: Wait Time(await) per Device in ms ("+GROUP_NAME+"->"+SHORT_NAME+")",   // Label 
			null, 
			LabelType.Dynamic,
			TrendGraphDataPoint.Category.WAITS,
			false, // is Percent Graph
			false, // visible at start
			0,     // graph is valid from Server Version. 0 = All Versions; >0 = Valid from this version and above 
			-1);   // minimum height

		// GRAPH
		addTrendGraph(GRAPH_NAME_ReadWaitTime,
			"iostat: Read Wait Time(r_await) per Device",                                           // Menu CheckBox text
			"iostat: Read wait Time(r_await) per Device in ms ("+GROUP_NAME+"->"+SHORT_NAME+")",   // Label 
			null, 
			LabelType.Dynamic,
			TrendGraphDataPoint.Category.WAITS,
			false, // is Percent Graph
			false, // visible at start
			0,     // graph is valid from Server Version. 0 = All Versions; >0 = Valid from this version and above 
			-1);   // minimum height

		// GRAPH
		addTrendGraph(GRAPH_NAME_WriteWaitTime,
			"iostat: Write Wait Time(w_await) per Device",                                           // Menu CheckBox text
			"iostat: Write wait Time(w_await) per Device in ms ("+GROUP_NAME+"->"+SHORT_NAME+")",   // Label 
			null, 
			LabelType.Dynamic,
			TrendGraphDataPoint.Category.WAITS,
			false, // is Percent Graph
			false, // visible at start
			0,     // graph is valid from Server Version. 0 = All Versions; >0 = Valid from this version and above 
			-1);   // minimum height

		// GRAPH
		addTrendGraph(GRAPH_NAME_SericeTime,
			"iostat: Service Time(svctm) per Device",                                           // Menu CheckBox text
			"iostat: Service Time(svctm) per Device in ms ("+GROUP_NAME+"->"+SHORT_NAME+")",   // Label 
			null, 
			LabelType.Dynamic,
			TrendGraphDataPoint.Category.WAITS,
			false, // is Percent Graph
			false, // visible at start
			0,     // graph is valid from Server Version. 0 = All Versions; >0 = Valid from this version and above 
			-1);   // minimum height

		// GRAPH
		addTrendGraph(GRAPH_NAME_QueueLength,
			"iostat: Avg Queue Length(avgqu-sz) per Device",                                     // Menu CheckBox text
			"iostat: Avg Queue Length(avgqu-sz) per Device ("+GROUP_NAME+"->"+SHORT_NAME+")",   // Label 
			null, 
			LabelType.Dynamic,
			TrendGraphDataPoint.Category.DISK,
			false, // is Percent Graph
			false, // visible at start
			0,     // graph is valid from Server Version. 0 = All Versions; >0 = Valid from this version and above 
			-1);   // minimum height

		// GRAPH
		addTrendGraph(GRAPH_NAME_BusyPct,
			"iostat: Busy Percent(utilPct) per Device",                                     // Menu CheckBox text
			"iostat: Busy Percent(utilPct) per Device ("+GROUP_NAME+"->"+SHORT_NAME+")",   // Label 
			null, 
			LabelType.Dynamic,
			TrendGraphDataPoint.Category.DISK,
			true,  // is Percent Graph
			false, // visible at start
			0,     // graph is valid from Server Version. 0 = All Versions; >0 = Valid from this version and above 
			-1);   // minimum height

		
		
		// GRAPH
		addTrendGraph(GRAPH_NAME_ReadOp,
			"iostat: Read Operations(readsPerSec) per Device & sec",                                     // Menu CheckBox text
			"iostat: Read Operations(readsPerSec) per Device & sec ("+GROUP_NAME+"->"+SHORT_NAME+")",   // Label 
			null, 
			LabelType.Dynamic,
			TrendGraphDataPoint.Category.DISK,
			false, // is Percent Graph
			false, // visible at start
			0,     // graph is valid from Server Version. 0 = All Versions; >0 = Valid from this version and above 
			-1);   // minimum height

		// GRAPH
		addTrendGraph(GRAPH_NAME_WriteOp,
			"iostat: Write Operations(writesPerSec) per Device & sec",                                     // Menu CheckBox text
			"iostat: Write Operations(writesPerSec) per Device & sec ("+GROUP_NAME+"->"+SHORT_NAME+")",   // Label 
			null, 
			LabelType.Dynamic,
			TrendGraphDataPoint.Category.DISK,
			false, // is Percent Graph
			false, // visible at start
			0,     // graph is valid from Server Version. 0 = All Versions; >0 = Valid from this version and above 
			-1);   // minimum height

		// GRAPH
		addTrendGraph(GRAPH_NAME_ReadKb,
			"iostat: Read KB(kbReadPerSec) per Device & sec",                                     // Menu CheckBox text
			"iostat: Read KB(kbReadPerSec) per Device & sec ("+GROUP_NAME+"->"+SHORT_NAME+")",   // Label 
			null, 
			LabelType.Dynamic,
			TrendGraphDataPoint.Category.DISK,
			false, // is Percent Graph
			false, // visible at start
			0,     // graph is valid from Server Version. 0 = All Versions; >0 = Valid from this version and above 
			-1);   // minimum height

		// GRAPH
		addTrendGraph(GRAPH_NAME_WriteKb,
			"iostat: Write KB(kbWritePerSec) per Device & sec",                                     // Menu CheckBox text
			"iostat: Write KB(kbWritePerSec) per Device & sec ("+GROUP_NAME+"->"+SHORT_NAME+")",   // Label 
			null, 
			LabelType.Dynamic,
			TrendGraphDataPoint.Category.DISK,
			false, // is Percent Graph
			false, // visible at start
			0,     // graph is valid from Server Version. 0 = All Versions; >0 = Valid from this version and above 
			-1);   // minimum height

		// GRAPH
		addTrendGraph(GRAPH_NAME_AvgReadKbPerIo,
			"iostat: Avg Read KB/IO(avgReadKbPerIo) per Device",                                     // Menu CheckBox text
			"iostat: Avg Read KB/IO(avgReadKbPerIo) per Device ("+GROUP_NAME+"->"+SHORT_NAME+")",   // Label 
			null, 
			LabelType.Dynamic,
			TrendGraphDataPoint.Category.DISK,
			false, // is Percent Graph
			false, // visible at start
			0,     // graph is valid from Server Version. 0 = All Versions; >0 = Valid from this version and above 
			-1);   // minimum height

		// GRAPH
		addTrendGraph(GRAPH_NAME_AvgWriteKbPerIo,
			"iostat: Avg Write KB/IO(avgWriteKbPerIo) per Device",                                     // Menu CheckBox text
			"iostat: Avg Write KB/IO(avgWriteKbPerIo) per Device ("+GROUP_NAME+"->"+SHORT_NAME+")",   // Label 
			null, 
			LabelType.Dynamic,
			TrendGraphDataPoint.Category.DISK,
			false, // is Percent Graph
			false, // visible at start
			0,     // graph is valid from Server Version. 0 = All Versions; >0 = Valid from this version and above 
			-1);   // minimum height

//		// if GUI
//		if (getGuiController() != null && getGuiController().hasGUI())
//		{
//			// GRAPH
//			TrendGraph tg = null;
//			tg = new TrendGraph(GRAPH_NAME_WaitTime,
//				"iostat: Wait Time(await) per Device",                                           // Menu CheckBox text
//				"iostat: Wait Time(await) per Device in ms ("+GROUP_NAME+"->"+SHORT_NAME+")",   // Label 
//				labels, 
//				false, // is Percent Graph
//				this, 
//				false, // visible at start
//				0,     // graph is valid from Server Version. 0 = All Versions; >0 = Valid from this version and above 
//				-1);  // minimum height
//			addTrendGraph(tg.getName(), tg, true);
//
//			// GRAPH
//			tg = new TrendGraph(GRAPH_NAME_ReadWaitTime,
//				"iostat: Read Wait Time(r_await) per Device",                                           // Menu CheckBox text
//				"iostat: Read wait Time(r_await) per Device in ms ("+GROUP_NAME+"->"+SHORT_NAME+")",   // Label 
//				labels, 
//				false, // is Percent Graph
//				this, 
//				false, // visible at start
//				0,     // graph is valid from Server Version. 0 = All Versions; >0 = Valid from this version and above 
//				-1);  // minimum height
//			addTrendGraph(tg.getName(), tg, true);
//
//			// GRAPH
//			tg = new TrendGraph(GRAPH_NAME_WriteWaitTime,
//				"iostat: Write Wait Time(w_await) per Device",                                           // Menu CheckBox text
//				"iostat: Write wait Time(w_await) per Device in ms ("+GROUP_NAME+"->"+SHORT_NAME+")",   // Label 
//				labels, 
//				false, // is Percent Graph
//				this, 
//				false, // visible at start
//				0,     // graph is valid from Server Version. 0 = All Versions; >0 = Valid from this version and above 
//				-1);  // minimum height
//			addTrendGraph(tg.getName(), tg, true);
//
//			// GRAPH
//			tg = new TrendGraph(GRAPH_NAME_SericeTime,
//				"iostat: Service Time(svctm) per Device",                                           // Menu CheckBox text
//				"iostat: Service Time(svctm) per Device in ms ("+GROUP_NAME+"->"+SHORT_NAME+")",   // Label 
//				labels, 
//				false, // is Percent Graph
//				this, 
//				false, // visible at start
//				0,     // graph is valid from Server Version. 0 = All Versions; >0 = Valid from this version and above 
//				-1);  // minimum height
//			addTrendGraph(tg.getName(), tg, true);
//
//			// GRAPH
//			tg = new TrendGraph(GRAPH_NAME_QueueLength,
//				"iostat: Avg Queue Length(avgqu-sz) per Device",                                     // Menu CheckBox text
//				"iostat: Avg Queue Length(avgqu-sz) per Device ("+GROUP_NAME+"->"+SHORT_NAME+")",   // Label 
//				labels, 
//				false, // is Percent Graph
//				this, 
//				false, // visible at start
//				0,     // graph is valid from Server Version. 0 = All Versions; >0 = Valid from this version and above 
//				-1);  // minimum height
//			addTrendGraph(tg.getName(), tg, true);
//
//			// GRAPH
//			tg = new TrendGraph(GRAPH_NAME_BusyPct,
//				"iostat: Busy Percent(utilPct) per Device",                                     // Menu CheckBox text
//				"iostat: Busy Percent(utilPct) per Device ("+GROUP_NAME+"->"+SHORT_NAME+")",   // Label 
//				labels, 
//				true, // is Percent Graph
//				this, 
//				false, // visible at start
//				0,     // graph is valid from Server Version. 0 = All Versions; >0 = Valid from this version and above 
//				-1);  // minimum height
//			addTrendGraph(tg.getName(), tg, true);
//
//			
//			
//			// GRAPH
//			tg = new TrendGraph(GRAPH_NAME_ReadOp,
//				"iostat: Read Operations(readsPerSec) per Device & sec",                                     // Menu CheckBox text
//				"iostat: Read Operations(readsPerSec) per Device & sec ("+GROUP_NAME+"->"+SHORT_NAME+")",   // Label 
//				labels, 
//				false, // is Percent Graph
//				this, 
//				false, // visible at start
//				0,     // graph is valid from Server Version. 0 = All Versions; >0 = Valid from this version and above 
//				-1);  // minimum height
//			addTrendGraph(tg.getName(), tg, true);
//
//			// GRAPH
//			tg = new TrendGraph(GRAPH_NAME_WriteOp,
//				"iostat: Write Operations(writesPerSec) per Device & sec",                                     // Menu CheckBox text
//				"iostat: Write Operations(writesPerSec) per Device & sec ("+GROUP_NAME+"->"+SHORT_NAME+")",   // Label 
//				labels, 
//				false, // is Percent Graph
//				this, 
//				false, // visible at start
//				0,     // graph is valid from Server Version. 0 = All Versions; >0 = Valid from this version and above 
//				-1);  // minimum height
//			addTrendGraph(tg.getName(), tg, true);
//
//			// GRAPH
//			tg = new TrendGraph(GRAPH_NAME_ReadKb,
//				"iostat: Read KB(kbReadPerSec) per Device & sec",                                     // Menu CheckBox text
//				"iostat: Read KB(kbReadPerSec) per Device & sec ("+GROUP_NAME+"->"+SHORT_NAME+")",   // Label 
//				labels, 
//				false, // is Percent Graph
//				this, 
//				false, // visible at start
//				0,     // graph is valid from Server Version. 0 = All Versions; >0 = Valid from this version and above 
//				-1);  // minimum height
//			addTrendGraph(tg.getName(), tg, true);
//
//			// GRAPH
//			tg = new TrendGraph(GRAPH_NAME_WriteKb,
//				"iostat: Write KB(kbWritePerSec) per Device & sec",                                     // Menu CheckBox text
//				"iostat: Write KB(kbWritePerSec) per Device & sec ("+GROUP_NAME+"->"+SHORT_NAME+")",   // Label 
//				labels, 
//				false, // is Percent Graph
//				this, 
//				false, // visible at start
//				0,     // graph is valid from Server Version. 0 = All Versions; >0 = Valid from this version and above 
//				-1);  // minimum height
//			addTrendGraph(tg.getName(), tg, true);
//
//			// GRAPH
//			tg = new TrendGraph(GRAPH_NAME_AvgReadKbPerIo,
//				"iostat: Avg Read KB/IO(avgReadKbPerIo) per Device",                                     // Menu CheckBox text
//				"iostat: Avg Read KB/IO(avgReadKbPerIo) per Device ("+GROUP_NAME+"->"+SHORT_NAME+")",   // Label 
//				labels, 
//				false, // is Percent Graph
//				this, 
//				false, // visible at start
//				0,     // graph is valid from Server Version. 0 = All Versions; >0 = Valid from this version and above 
//				-1);  // minimum height
//			addTrendGraph(tg.getName(), tg, true);
//
//			// GRAPH
//			tg = new TrendGraph(GRAPH_NAME_AvgWriteKbPerIo,
//				"iostat: Avg Write KB/IO(avgWriteKbPerIo) per Device",                                     // Menu CheckBox text
//				"iostat: Avg Write KB/IO(avgWriteKbPerIo) per Device ("+GROUP_NAME+"->"+SHORT_NAME+")",   // Label 
//				labels, 
//				false, // is Percent Graph
//				this, 
//				false, // visible at start
//				0,     // graph is valid from Server Version. 0 = All Versions; >0 = Valid from this version and above 
//				-1);  // minimum height
//			addTrendGraph(tg.getName(), tg, true);
//		}
	}
	
	@Override
	public void updateGraphData(TrendGraphDataPoint tgdp)
	{
	    
		TrendGraph tg = getTrendGraph(tgdp.getName());
//		if (tg == null)
//		{
//			_logger.warn("Trend graph with the name '"+tgdp.getName()+"' can't be found in the cm '"+this.getName()+"'.");
//			return;
//		}

		// GRAPH: WAIT TIME
		if (GRAPH_NAME_WaitTime.equals(tgdp.getName()))
		{
			int dataPoint_pos                    = this.findColumn("await"); // Linix
//			if (dataPoint_pos < 0) dataPoint_pos = this.findColumn("wait");  // Solaris

			int device_pos                 = this.findColumn("device"); // HPUX, Linux, Solaris
//			if (device_pos < 0) device_pos = this.findColumn("Disks");  // AIX
//			if (device_pos < 0) device_pos = this.findColumn("name");   // Veritas
			
			int deviceDesc_pos = this.findColumn("deviceDescription");

			if (dataPoint_pos < 0 || device_pos < 0 || deviceDesc_pos < 0)
			{
				String msg = "";
				if (dataPoint_pos  < 0) msg += "'await', ";
				if (device_pos     < 0) msg += "'device', ";
				if (deviceDesc_pos < 0) msg += "'deviceDescription', ";
				
				if (tg != null)
					tg.setWarningLabel("Column(s) "+StringUtil.removeLastComma(msg)+" can't be found. This graph is only supported on Linux systems");
			}
			else
			{
				// Write 1 "line" for each device
				Double[] dArray  = new Double[this.getRowCount()];
				String[] lArray  = new String[dArray.length];
				String[] ldArray = new String[dArray.length];
				
				for (int i = 0; i < dArray.length; i++)
				{
					String deviceDesc = this.getAbsString(i, deviceDesc_pos);
					String deviceName = this.getAbsString(i, device_pos);

					String  label = deviceName;
					if (StringUtil.hasValue(deviceDesc))
						label += " ("+deviceDesc+")";

					Double dataPoint = this.getAbsValueAsDouble(i, dataPoint_pos);

					ldArray[i] = label;
					lArray[i]  = deviceName;
					dArray[i]  = dataPoint;
				}

				// Set the values
				tgdp.setDataPoint(this.getTimestamp(), lArray, ldArray, dArray);
//				tgdp.setDate(this.getTimestamp());
//				tgdp.setLabel(lArray);
//				tgdp.setData(dArray);

				if (tg != null)
					tg.setWarningLabel(null);
			}
		} // end: graph

		// GRAPH: READ WAIT TIME
		if (GRAPH_NAME_ReadWaitTime.equals(tgdp.getName()))
		{
			int dataPoint_pos  = this.findColumn("r_await"); // Linix
			int device_pos     = this.findColumn("device"); // Linux
			int deviceDesc_pos = this.findColumn("deviceDescription");

			if (dataPoint_pos < 0 || device_pos < 0 || deviceDesc_pos < 0)
			{
				String msg = "";
				if (dataPoint_pos  < 0) msg += "'r_await', ";
				if (device_pos     < 0) msg += "'device', ";
				if (deviceDesc_pos < 0) msg += "'deviceDescription', ";

				if (tg != null)
					tg.setWarningLabel("Column(s) "+StringUtil.removeLastComma(msg)+" can't be found. This graph is only supported on Linux systems (where iostat ver >= 9.1.2)");
			}
			else
			{
				// Write 1 "line" for each device
				Double[] dArray  = new Double[this.getRowCount()];
				String[] lArray  = new String[dArray.length];
				String[] ldArray = new String[dArray.length];
				
				for (int i = 0; i < dArray.length; i++)
				{
					String deviceDesc = this.getAbsString(i, deviceDesc_pos);
					String deviceName = this.getAbsString(i, device_pos);

					String  label = deviceName;
					if (StringUtil.hasValue(deviceDesc))
						label += " ("+deviceDesc+")";

					Double dataPoint = this.getAbsValueAsDouble(i, dataPoint_pos);

					ldArray[i] = label;
					lArray[i]  = deviceName;
					dArray[i]  = dataPoint;
				}

				// Set the values
				tgdp.setDataPoint(this.getTimestamp(), lArray, ldArray, dArray);
//				tgdp.setDate(this.getTimestamp());
//				tgdp.setLabel(lArray);
//				tgdp.setData(dArray);

				if (tg != null)
					tg.setWarningLabel(null);
			}
		} // end: graph

		// GRAPH: WRITE WAIT TIME
		if (GRAPH_NAME_WriteWaitTime.equals(tgdp.getName()))
		{
			int dataPoint_pos  = this.findColumn("w_await"); // Linix
			int device_pos     = this.findColumn("device"); // Linux
			int deviceDesc_pos = this.findColumn("deviceDescription");

			if (dataPoint_pos < 0 || device_pos < 0 || deviceDesc_pos < 0)
			{
				String msg = "";
				if (dataPoint_pos  < 0) msg += "'w_await', ";
				if (device_pos     < 0) msg += "'device', ";
				if (deviceDesc_pos < 0) msg += "'deviceDescription', ";

				if (tg != null)
					tg.setWarningLabel("Column(s) "+StringUtil.removeLastComma(msg)+" can't be found. This graph is only supported on Linux systems (where iostat ver >= 9.1.2)");
			}
			else
			{
				// Write 1 "line" for each device
				Double[] dArray  = new Double[this.getRowCount()];
				String[] lArray  = new String[dArray.length];
				String[] ldArray = new String[dArray.length];

				for (int i = 0; i < dArray.length; i++)
				{
					String deviceDesc = this.getAbsString(i, deviceDesc_pos);
					String deviceName = this.getAbsString(i, device_pos);

					String  label = deviceName;
					if (StringUtil.hasValue(deviceDesc))
						label += " ("+deviceDesc+")";

					Double dataPoint = this.getAbsValueAsDouble(i, dataPoint_pos);

					ldArray[i] = label;
					lArray[i]  = deviceName;
					dArray[i]  = dataPoint;
				}

				// Set the values
				tgdp.setDataPoint(this.getTimestamp(), lArray, ldArray, dArray);
//				tgdp.setDate(this.getTimestamp());
//				tgdp.setLabel(lArray);
//				tgdp.setData(dArray);

				if (tg != null)
					tg.setWarningLabel(null);
			}
		} // end: graph

		// GRAPH: SERVICE TIME
		if (GRAPH_NAME_SericeTime.equals(tgdp.getName()))
		{
			int dataPoint_pos                    = this.findColumn("svctm"); // Linix
			if (dataPoint_pos < 0) dataPoint_pos = this.findColumn("svc_t");  // Solaris
//			if (dataPoint_pos < 0) dataPoint_pos = this.findColumn("XXXXX");  // AIX
			if (dataPoint_pos < 0) dataPoint_pos = this.findColumn("msps");  // HP

			int device_pos                 = this.findColumn("device"); // HPUX, Linux, Solaris
			if (device_pos < 0) device_pos = this.findColumn("Disks");  // AIX
			if (device_pos < 0) device_pos = this.findColumn("name");   // Veritas
			
			int deviceDesc_pos = this.findColumn("deviceDescription");

			if (dataPoint_pos < 0 || device_pos < 0 || deviceDesc_pos < 0)
			{
				String msg = "";
				if (dataPoint_pos  < 0) msg += "'svctm|svc_t|msps', ";
				if (device_pos     < 0) msg += "'device|Disk|name', ";
				if (deviceDesc_pos < 0) msg += "'deviceDescription', ";

				if (tg != null)
					tg.setWarningLabel("Column(s) "+StringUtil.removeLastComma(msg)+" can't be found. This graph is only supported on Linux/Solaris/HP systems");
			}
			else
			{
				// Write 1 "line" for each device
				Double[] dArray  = new Double[this.getRowCount()];
				String[] lArray  = new String[dArray.length];
				String[] ldArray = new String[dArray.length];
				
				for (int i = 0; i < dArray.length; i++)
				{
					String deviceDesc = this.getAbsString(i, deviceDesc_pos);
					String deviceName = this.getAbsString(i, device_pos);

					String  label = deviceName;
					if (StringUtil.hasValue(deviceDesc))
						label += " ("+deviceDesc+")";

					Double dataPoint = this.getAbsValueAsDouble(i, dataPoint_pos);

					ldArray[i] = label;
					lArray[i]  = deviceName;
					dArray[i]  = dataPoint;
				}

				// Set the values
				tgdp.setDataPoint(this.getTimestamp(), lArray, ldArray, dArray);
//				tgdp.setDate(this.getTimestamp());
//				tgdp.setLabel(lArray);
//				tgdp.setData(dArray);

				if (tg != null)
					tg.setWarningLabel(null);
			}
		} // end: graph

		// GRAPH: QUEUE LENGTH
		if (GRAPH_NAME_QueueLength.equals(tgdp.getName()))
		{
			int dataPoint_pos                    = this.findColumn("avgqu-sz");  // Linix
			if (dataPoint_pos < 0) dataPoint_pos = this.findColumn("wait");      // Solaris
			if (dataPoint_pos < 0) dataPoint_pos = this.findColumn("Q_avgwqsz"); // AIX
//			if (dataPoint_pos < 0) dataPoint_pos = this.findColumn("???");       // HP

			int device_pos                 = this.findColumn("device"); // HPUX, Linux, Solaris
			if (device_pos < 0) device_pos = this.findColumn("Disks");  // AIX
			if (device_pos < 0) device_pos = this.findColumn("name");   // Veritas
			
			int deviceDesc_pos = this.findColumn("deviceDescription");

			if (dataPoint_pos < 0 || device_pos < 0 || deviceDesc_pos < 0)
			{
				String msg = "";
				if (dataPoint_pos  < 0) msg += "'avgqu-sz|wait|Q_avgwqsz', ";
				if (device_pos     < 0) msg += "'device|Disk|name', ";
				if (deviceDesc_pos < 0) msg += "'deviceDescription', ";

				if (tg != null)
					tg.setWarningLabel("Column(s) "+StringUtil.removeLastComma(msg)+" can't be found. This graph is only supported on Linux/Solaris/AIX systems");
			}
			else
			{
				// Write 1 "line" for each device
				Double[] dArray  = new Double[this.getRowCount()];
				String[] lArray  = new String[dArray.length];
				String[] ldArray = new String[dArray.length];
				
				for (int i = 0; i < dArray.length; i++)
				{
					String deviceDesc = this.getAbsString(i, deviceDesc_pos);
					String deviceName = this.getAbsString(i, device_pos);

					String  label = deviceName;
					if (StringUtil.hasValue(deviceDesc))
						label += " ("+deviceDesc+")";

					Double dataPoint = this.getAbsValueAsDouble(i, dataPoint_pos);

					ldArray[i] = label;
					lArray[i]  = deviceName;
					dArray[i]  = dataPoint;
				}

				// Set the values
				tgdp.setDataPoint(this.getTimestamp(), lArray, ldArray, dArray);
//				tgdp.setDate(this.getTimestamp());
//				tgdp.setLabel(lArray);
//				tgdp.setData(dArray);

				if (tg != null)
					tg.setWarningLabel(null);
			}
		} // end: graph

		// GRAPH: BUSSY PCT
		if (GRAPH_NAME_BusyPct.equals(tgdp.getName()))
		{
			int dataPoint_pos                    = this.findColumn("utilPct");  // Linix
			if (dataPoint_pos < 0) dataPoint_pos = this.findColumn("busyPct");  // Solaris
			if (dataPoint_pos < 0) dataPoint_pos = this.findColumn("X_tm_act"); // AIX
//			if (dataPoint_pos < 0) dataPoint_pos = this.findColumn("???");       // HP

			int device_pos                 = this.findColumn("device"); // HPUX, Linux, Solaris
			if (device_pos < 0) device_pos = this.findColumn("Disks");  // AIX
			if (device_pos < 0) device_pos = this.findColumn("name");   // Veritas
			
			int deviceDesc_pos = this.findColumn("deviceDescription");

			if (dataPoint_pos < 0 || device_pos < 0 || deviceDesc_pos < 0)
			{
				String msg = "";
				if (dataPoint_pos  < 0) msg += "'utilPct|busyPct|X_tm_act', ";
				if (device_pos     < 0) msg += "'device|Disk|name', ";
				if (deviceDesc_pos < 0) msg += "'deviceDescription', ";

				if (tg != null)
					tg.setWarningLabel("Column(s) "+StringUtil.removeLastComma(msg)+" can't be found. This graph is only supported on Linux/Solaris/AIX systems");
			}
			else
			{
				// Write 1 "line" for each device
				Double[] dArray  = new Double[this.getRowCount()];
				String[] lArray  = new String[dArray.length];
				String[] ldArray = new String[dArray.length];
				
				for (int i = 0; i < dArray.length; i++)
				{
					String deviceDesc = this.getAbsString(i, deviceDesc_pos);
					String deviceName = this.getAbsString(i, device_pos);

					String  label = deviceName;
					if (StringUtil.hasValue(deviceDesc))
						label += " ("+deviceDesc+")";

					Double dataPoint = this.getAbsValueAsDouble(i, dataPoint_pos);

					ldArray[i] = label;
					lArray[i]  = deviceName;
					dArray[i]  = dataPoint;
				}

				// Set the values
				tgdp.setDataPoint(this.getTimestamp(), lArray, ldArray, dArray);
//				tgdp.setDate(this.getTimestamp());
//				tgdp.setLabel(lArray);
//				tgdp.setData(dArray);

				if (tg != null)
					tg.setWarningLabel(null);
			}
		} // end: graph


	
	
		// GRAPH: READ OP
		if (GRAPH_NAME_ReadOp.equals(tgdp.getName()))
		{
			int dataPoint_pos                    = this.findColumn("readsPerSec"); // Linix/Solaris
			if (dataPoint_pos < 0) dataPoint_pos = this.findColumn("R_rps");  // AIX
//			if (dataPoint_pos < 0) dataPoint_pos = this.findColumn("msps");  // HP

			int device_pos                 = this.findColumn("device"); // HPUX, Linux, Solaris
			if (device_pos < 0) device_pos = this.findColumn("Disks");  // AIX
			if (device_pos < 0) device_pos = this.findColumn("name");   // Veritas
			
			int deviceDesc_pos = this.findColumn("deviceDescription");

			if (dataPoint_pos < 0 || device_pos < 0 || deviceDesc_pos < 0)
			{
				String msg = "";
				if (dataPoint_pos  < 0) msg += "'readsPerSec|R_rps', ";
				if (device_pos     < 0) msg += "'device|Disk|name', ";
				if (deviceDesc_pos < 0) msg += "'deviceDescription', ";

				if (tg != null)
					tg.setWarningLabel("Column(s) "+StringUtil.removeLastComma(msg)+" can't be found. This graph is only supported on Linux/Solaris/AIX systems");
			}
			else
			{
				// Write 1 "line" for each device
				Double[] dArray  = new Double[this.getRowCount()];
				String[] lArray  = new String[dArray.length];
				String[] ldArray = new String[dArray.length];
				
				for (int i = 0; i < dArray.length; i++)
				{
					String deviceDesc = this.getAbsString(i, deviceDesc_pos);
					String deviceName = this.getAbsString(i, device_pos);

					String  label = deviceName;
					if (StringUtil.hasValue(deviceDesc))
						label += " ("+deviceDesc+")";

					Double dataPoint = this.getAbsValueAsDouble(i, dataPoint_pos);

					ldArray[i] = label;
					lArray[i]  = deviceName;
					dArray[i]  = dataPoint;
				}

				// Set the values
				tgdp.setDataPoint(this.getTimestamp(), lArray, ldArray, dArray);
//				tgdp.setDate(this.getTimestamp());
//				tgdp.setLabel(lArray);
//				tgdp.setData(dArray);

				if (tg != null)
					tg.setWarningLabel(null);
			}
		} // end: graph

		// GRAPH: WRITE OP
		if (GRAPH_NAME_WriteOp.equals(tgdp.getName()))
		{
			int dataPoint_pos                    = this.findColumn("writesPerSec"); // Linix/Solaris
			if (dataPoint_pos < 0) dataPoint_pos = this.findColumn("W_rps");  // AIX
//			if (dataPoint_pos < 0) dataPoint_pos = this.findColumn("msps");  // HP

			int device_pos                 = this.findColumn("device"); // HPUX, Linux, Solaris
			if (device_pos < 0) device_pos = this.findColumn("Disks");  // AIX
			if (device_pos < 0) device_pos = this.findColumn("name");   // Veritas
			
			int deviceDesc_pos = this.findColumn("deviceDescription");

			if (dataPoint_pos < 0 || device_pos < 0 || deviceDesc_pos < 0)
			{
				String msg = "";
				if (dataPoint_pos  < 0) msg += "'writesPerSec|W_rps', ";
				if (device_pos     < 0) msg += "'device|Disk|name', ";
				if (deviceDesc_pos < 0) msg += "'deviceDescription', ";

				if (tg != null)
					tg.setWarningLabel("Column(s) "+StringUtil.removeLastComma(msg)+" can't be found. This graph is only supported on Linux/Solaris/AIX systems");
			}
			else
			{
				// Write 1 "line" for each device
				Double[] dArray  = new Double[this.getRowCount()];
				String[] lArray  = new String[dArray.length];
				String[] ldArray = new String[dArray.length];
				
				for (int i = 0; i < dArray.length; i++)
				{
					String deviceDesc = this.getAbsString(i, deviceDesc_pos);
					String deviceName = this.getAbsString(i, device_pos);

					String  label = deviceName;
					if (StringUtil.hasValue(deviceDesc))
						label += " ("+deviceDesc+")";

					Double dataPoint = this.getAbsValueAsDouble(i, dataPoint_pos);

					ldArray[i] = label;
					lArray[i]  = deviceName;
					dArray[i]  = dataPoint;
				}

				// Set the values
				tgdp.setDataPoint(this.getTimestamp(), lArray, ldArray, dArray);
//				tgdp.setDate(this.getTimestamp());
//				tgdp.setLabel(lArray);
//				tgdp.setData(dArray);

				if (tg != null)
					tg.setWarningLabel(null);
			}
		} // end: graph

		// GRAPH: READ KB
		if (GRAPH_NAME_ReadKb.equals(tgdp.getName()))
		{
			int dataPoint_pos                    = this.findColumn("kbReadPerSec"); // Linix/Solaris
			if (dataPoint_pos < 0) dataPoint_pos = this.findColumn("X_bread");  // AIX
//			if (dataPoint_pos < 0) dataPoint_pos = this.findColumn("xxx");  // HP

			int device_pos                 = this.findColumn("device"); // HPUX, Linux, Solaris
			if (device_pos < 0) device_pos = this.findColumn("Disks");  // AIX
			if (device_pos < 0) device_pos = this.findColumn("name");   // Veritas
			
			int deviceDesc_pos = this.findColumn("deviceDescription");

			if (dataPoint_pos < 0 || device_pos < 0 || deviceDesc_pos < 0)
			{
				String msg = "";
				if (dataPoint_pos  < 0) msg += "'kbReadPerSec|X_bread', ";
				if (device_pos     < 0) msg += "'device|Disk|name', ";
				if (deviceDesc_pos < 0) msg += "'deviceDescription', ";

				if (tg != null)
					tg.setWarningLabel("Column(s) "+StringUtil.removeLastComma(msg)+" can't be found. This graph is only supported on Linux/Solaris/AIX systems");
			}
			else
			{
				// Write 1 "line" for each device
				Double[] dArray  = new Double[this.getRowCount()];
				String[] lArray  = new String[dArray.length];
				String[] ldArray = new String[dArray.length];
				
				for (int i = 0; i < dArray.length; i++)
				{
					String deviceDesc = this.getAbsString(i, deviceDesc_pos);
					String deviceName = this.getAbsString(i, device_pos);

					String  label = deviceName;
					if (StringUtil.hasValue(deviceDesc))
						label += " ("+deviceDesc+")";

					Double dataPoint = this.getAbsValueAsDouble(i, dataPoint_pos);

					ldArray[i] = label;
					lArray[i]  = deviceName;
					dArray[i]  = dataPoint;
				}

				// Set the values
				tgdp.setDataPoint(this.getTimestamp(), lArray, ldArray, dArray);
//				tgdp.setDate(this.getTimestamp());
//				tgdp.setLabel(lArray);
//				tgdp.setData(dArray);

				if (tg != null)
					tg.setWarningLabel(null);
			}
		} // end: graph

		// GRAPH: WRITE KB
		if (GRAPH_NAME_WriteKb.equals(tgdp.getName()))
		{
			int dataPoint_pos                    = this.findColumn("kbWritePerSec"); // Linix/Solaris
			if (dataPoint_pos < 0) dataPoint_pos = this.findColumn("X_bwrtn");  // AIX
//			if (dataPoint_pos < 0) dataPoint_pos = this.findColumn("xxx");  // HP

			int device_pos                 = this.findColumn("device"); // HPUX, Linux, Solaris
			if (device_pos < 0) device_pos = this.findColumn("Disks");  // AIX
			if (device_pos < 0) device_pos = this.findColumn("name");   // Veritas
			
			int deviceDesc_pos = this.findColumn("deviceDescription");

			if (dataPoint_pos < 0 || device_pos < 0 || deviceDesc_pos < 0)
			{
				String msg = "";
				if (dataPoint_pos  < 0) msg += "'kbWritePerSec|X_bwrtn', ";
				if (device_pos     < 0) msg += "'device|Disk|name', ";
				if (deviceDesc_pos < 0) msg += "'deviceDescription', ";

				if (tg != null)
					tg.setWarningLabel("Column(s) "+StringUtil.removeLastComma(msg)+" can't be found. This graph is only supported on Linux/Solaris/AIX systems");
			}
			else
			{
				// Write 1 "line" for each device
				Double[] dArray  = new Double[this.getRowCount()];
				String[] lArray  = new String[dArray.length];
				String[] ldArray = new String[dArray.length];
				
				for (int i = 0; i < dArray.length; i++)
				{
					String deviceDesc = this.getAbsString(i, deviceDesc_pos);
					String deviceName = this.getAbsString(i, device_pos);

					String  label = deviceName;
					if (StringUtil.hasValue(deviceDesc))
						label += " ("+deviceDesc+")";

					Double dataPoint = this.getAbsValueAsDouble(i, dataPoint_pos);

					ldArray[i] = label;
					lArray[i]  = deviceName;
					dArray[i]  = dataPoint;
				}

				// Set the values
				tgdp.setDataPoint(this.getTimestamp(), lArray, ldArray, dArray);
//				tgdp.setDate(this.getTimestamp());
//				tgdp.setLabel(lArray);
//				tgdp.setData(dArray);

				if (tg != null)
					tg.setWarningLabel(null);
			}
		} // end: graph

		// GRAPH: WRITE KB
		if (GRAPH_NAME_AvgReadKbPerIo.equals(tgdp.getName()))
		{
			int dataPoint_pos                    = this.findColumn("avgReadKbPerIo"); // Linix/Solaris
//			if (dataPoint_pos < 0) dataPoint_pos = this.findColumn("X_bwrtn");  // AIX
//			if (dataPoint_pos < 0) dataPoint_pos = this.findColumn("xxx");  // HP

			int device_pos                 = this.findColumn("device"); // HPUX, Linux, Solaris
			if (device_pos < 0) device_pos = this.findColumn("Disks");  // AIX
			if (device_pos < 0) device_pos = this.findColumn("name");   // Veritas
			
			int deviceDesc_pos = this.findColumn("deviceDescription");

			if (dataPoint_pos < 0 || device_pos < 0 || deviceDesc_pos < 0)
			{
				String msg = "";
				if (dataPoint_pos  < 0) msg += "'avgReadKbPerIo', ";
				if (device_pos     < 0) msg += "'device|Disk|name', ";
				if (deviceDesc_pos < 0) msg += "'deviceDescription', ";

				if (tg != null)
					tg.setWarningLabel("Column(s) "+StringUtil.removeLastComma(msg)+" can't be found. This graph is only supported on Linux/Solaris/AIX systems");
			}
			else
			{
				// Write 1 "line" for each device
				Double[] dArray  = new Double[this.getRowCount()];
				String[] lArray  = new String[dArray.length];
				String[] ldArray = new String[dArray.length];
				
				for (int i = 0; i < dArray.length; i++)
				{
					String deviceDesc = this.getAbsString(i, deviceDesc_pos);
					String deviceName = this.getAbsString(i, device_pos);

					String  label = deviceName;
					if (StringUtil.hasValue(deviceDesc))
						label += " ("+deviceDesc+")";

					Double dataPoint = this.getAbsValueAsDouble(i, dataPoint_pos);

					ldArray[i] = label;
					lArray[i]  = deviceName;
					dArray[i]  = dataPoint;
				}

				// Set the values
				tgdp.setDataPoint(this.getTimestamp(), lArray, ldArray, dArray);
//				tgdp.setDate(this.getTimestamp());
//				tgdp.setLabel(lArray);
//				tgdp.setData(dArray);

				if (tg != null)
					tg.setWarningLabel(null);
			}
		} // end: graph

		// GRAPH: WRITE KB
		if (GRAPH_NAME_AvgWriteKbPerIo.equals(tgdp.getName()))
		{
			int dataPoint_pos                    = this.findColumn("avgWriteKbPerIo"); // Linix/Solaris
//			if (dataPoint_pos < 0) dataPoint_pos = this.findColumn("X_bwrtn");  // AIX
//			if (dataPoint_pos < 0) dataPoint_pos = this.findColumn("xxx");  // HP

			int device_pos                 = this.findColumn("device"); // HPUX, Linux, Solaris
			if (device_pos < 0) device_pos = this.findColumn("Disks");  // AIX
			if (device_pos < 0) device_pos = this.findColumn("name");   // Veritas
			
			int deviceDesc_pos = this.findColumn("deviceDescription");

			if (dataPoint_pos < 0 || device_pos < 0 || deviceDesc_pos < 0)
			{
				String msg = "";
				if (dataPoint_pos  < 0) msg += "'avgWriteKbPerIo', ";
				if (device_pos     < 0) msg += "'device|Disk|name', ";
				if (deviceDesc_pos < 0) msg += "'deviceDescription', ";
				
				if (tg != null)
					tg.setWarningLabel("Column(s) "+StringUtil.removeLastComma(msg)+" can't be found. This graph is only supported on Linux/Solaris/AIX systems");
			}
			else
			{
				// Write 1 "line" for each device
				Double[] dArray  = new Double[this.getRowCount()];
				String[] lArray  = new String[dArray.length];
				String[] ldArray = new String[dArray.length];
				
				for (int i = 0; i < dArray.length; i++)
				{
					String deviceDesc = this.getAbsString(i, deviceDesc_pos);
					String deviceName = this.getAbsString(i, device_pos);

					String  label = deviceName;
					if (StringUtil.hasValue(deviceDesc))
						label += " ("+deviceDesc+")";

					Double dataPoint = this.getAbsValueAsDouble(i, dataPoint_pos);

					ldArray[i] = label;
					lArray[i]  = deviceName;
					dArray[i]  = dataPoint;
				}

				// Set the values
				tgdp.setDataPoint(this.getTimestamp(), lArray, ldArray, dArray);
//				tgdp.setDate(this.getTimestamp());
//				tgdp.setLabel(lArray);
//				tgdp.setData(dArray);

				if (tg != null)
					tg.setWarningLabel(null);
			}
		} // end: graph

	} // end: method

	// override xxx and change 'deviceDescription' into a real descriptions
	@Override
	public void localCalculation(OsTable newSample)
	{
		int deviceDescription_pos = -1;
		int device_pos = -1;
		
		int readsPerSec_pos     = -1;
		int writesPerSec_pos    = -1;
		int kbReadPerSec_pos    = -1;
		int kbWritePerSec_pos   = -1;
		int avgReadKbPerIo_pos  = -1;
		int avgWriteKbPerIo_pos = -1;
		
		String hostname = null;
		HostMonitor hostMonitor = (HostMonitor) getClientProperty(HostMonitor.PROPERTY_NAME);
		if (hostMonitor != null)
			hostname = hostMonitor.getHostname();
		
		if (hostname == null)
			return;

//		boolean excludeDevices = false;
		boolean excludeDevices       = Configuration.getCombinedConfiguration().getBooleanProperty(PROPKEY_excludeDevices,       DEFAULT_excludeDevices);
		String  excludeDevicesRegExp = Configuration.getCombinedConfiguration().getProperty(       PROPKEY_excludeDevicesRegExp, DEFAULT_excludeDevicesRegExp);

		// Find column Id's
		List<String> colNames = newSample.getColNames();
		if (colNames == null)
			return;

		for (int colId=0; colId < colNames.size(); colId++) 
		{
			String colName = colNames.get(colId);
			if      (colName.equals("deviceDescription")) deviceDescription_pos = colId;
			else if (colName.equals("Disks"))             device_pos            = colId; // AIX
			else if (colName.equals("device"))            device_pos            = colId; // HPUX, Linux, Solaris
			else if (colName.equals("name"))              device_pos            = colId; // Veritas

			else if (colName.equals("readsPerSec"))       readsPerSec_pos       = colId;
			else if (colName.equals("writesPerSec"))      writesPerSec_pos      = colId;
			else if (colName.equals("kbReadPerSec"))      kbReadPerSec_pos      = colId;
			else if (colName.equals("kbWritePerSec"))     kbWritePerSec_pos     = colId;
			else if (colName.equals("avgReadKbPerIo"))    avgReadKbPerIo_pos    = colId;
			else if (colName.equals("avgWriteKbPerIo"))   avgWriteKbPerIo_pos   = colId;
		}
		
		Set<String> removeSet = null;
		// Loop on all rows
		for (int rowId = 0; rowId < newSample.getRowCount(); rowId++)
		{
			// Set device description, or add it for deletion if isHidden
			if (deviceDescription_pos >= 0 && device_pos >= 0)
			{
				// 'deviceDescription' fix into a real description 
				Object deviceName_obj = newSample.getValueAt(rowId, device_pos);
				if (deviceName_obj != null)
				{
					String deviceName = deviceName_obj.toString();
					
					boolean isHidden    = IoStatDeviceMapperDialog.isHidden(      hostname, deviceName);
					String  description = IoStatDeviceMapperDialog.getDescription(hostname, deviceName);

					if (isHidden)
					{
						if (removeSet == null)
							removeSet = new HashSet<String>();
						removeSet.add(deviceName);
					}
					newSample.setValueAt(description, rowId, deviceDescription_pos);

					// TODO: remove devices that are system devices 'sd[az]'
					if (excludeDevices)
					{
						if (deviceName.matches(excludeDevicesRegExp))
						{
							if (removeSet == null)
								removeSet = new HashSet<String>();
							removeSet.add(deviceName);
						}
					}
				}
			}

			// Calculate Average Read/Write Size per IO (how big IO's do we issue)
			if (readsPerSec_pos >= 0 && writesPerSec_pos >= 0 && kbReadPerSec_pos >= 0 && kbWritePerSec_pos >= 0 && avgReadKbPerIo_pos >= 0 && avgWriteKbPerIo_pos >= 0)
			{
				Object readsPerSec_obj     = newSample.getValueAt(rowId, readsPerSec_pos);
				Object writesPerSec_obj    = newSample.getValueAt(rowId, writesPerSec_pos);
				Object kbReadPerSec_obj    = newSample.getValueAt(rowId, kbReadPerSec_pos);
				Object kbWritePerSec_obj   = newSample.getValueAt(rowId, kbWritePerSec_pos);

				if (readsPerSec_obj instanceof Number && writesPerSec_obj instanceof Number && kbReadPerSec_obj instanceof Number && kbWritePerSec_obj instanceof Number)
				{
					Number readsPerSec   = (Number) readsPerSec_obj;
					Number writesPerSec  = (Number) writesPerSec_obj; 
					Number kbReadPerSec  = (Number) kbReadPerSec_obj; 
					Number kbWritePerSec = (Number) kbWritePerSec_obj; 

					BigDecimal avgReadKbPerIo = null;
					BigDecimal avgWriteKbPerIo = null;

					if (readsPerSec.doubleValue() > 0)
						avgReadKbPerIo = new BigDecimal(kbReadPerSec.doubleValue() / readsPerSec.doubleValue()).setScale(1, BigDecimal.ROUND_HALF_EVEN);
					else
						avgReadKbPerIo = new BigDecimal(0.0);

					if (writesPerSec.doubleValue() > 0)
						avgWriteKbPerIo = new BigDecimal(kbWritePerSec.doubleValue() / writesPerSec.doubleValue()).setScale(1, BigDecimal.ROUND_HALF_EVEN);
					else
						avgWriteKbPerIo = new BigDecimal(0.0);

					newSample.setValueAt(avgReadKbPerIo,  rowId, avgReadKbPerIo_pos);
					newSample.setValueAt(avgWriteKbPerIo, rowId, avgWriteKbPerIo_pos);
				}
			}
		}
		
		// Remove records that should not be part of the list
		if (removeSet != null)
		{
			for (String deviceName : removeSet)
			{
				newSample.removeRowByPk(deviceName);
			}
		}
	}
}