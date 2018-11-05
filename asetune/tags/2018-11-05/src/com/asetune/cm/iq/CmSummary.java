package com.asetune.cm.iq;

import java.math.BigDecimal;
import java.sql.Connection;
import java.util.Calendar;
import java.util.LinkedList;
import java.util.List;

import javax.swing.JPanel;

import org.apache.log4j.Logger;

import com.asetune.CounterController;
import com.asetune.ICounterController;
import com.asetune.IGuiController;
import com.asetune.cm.CounterSetTemplates;
import com.asetune.cm.CounterSetTemplates.Type;
import com.asetune.cm.CountersModel;
import com.asetune.cm.iq.gui.CmSummaryPanel;
import com.asetune.graph.TrendGraphDataPoint;
import com.asetune.graph.TrendGraphDataPoint.LabelType;

/**
 * @author Goran Schwarz (goran_schwarz@hotmail.com)
 */
public class CmSummary
extends CountersModel
{
	private static Logger        _logger          = Logger.getLogger(CmSummary.class);
	private static final long    serialVersionUID = 1L;

	public static final String   CM_NAME          = CmSummary.class.getSimpleName();
	public static final String   SHORT_NAME       = "Summary";
	public static final String   HTML_DESC        = 
		"<html>" +
		"Overview of how the system performs." +
		"</html>";

	public static final String   GROUP_NAME       = null;
	public static final String   GUI_ICON_FILE    = "images/"+CM_NAME+".png";

	public static final int      NEED_SRV_VERSION = 0;
	public static final int      NEED_CE_VERSION  = 0;

	public static final String[] MON_TABLES       = new String[] {};
	public static final String[] NEED_ROLES       = new String[] {};
	public static final String[] NEED_CONFIG      = new String[] {};

	public static final String[] PCT_COLUMNS      = new String[] {};
	public static final String[] DIFF_COLUMNS     = new String[] {
		"ActiveReq",
		"BytesReceived",
		"BytesSent",
		"Chkpt",
		"CurrentCacheSize_Kb",
		"LockedHeapPages",
		"MainHeapBytes",
		"MainHeapPages",
		"MaxCacheSize",
		"MinCacheSize",
		"MultiPacketsReceived",
		"MultiPacketsSent",
		"NumProcessorsAvail",
		"PacketsReceived",
		"PacketsSent",
		"PageSize",
		"PeakCacheSize",
		"ProcessCPU",
		"ProcessCPUSystem",
		"ProcessCPUUser",
		"threads",
		"ConnCount",
		"UnschReq",
		"NumLogicalProcessorsUsed",
		"NumPhysicalProcessorsUsed",
		"Commit",
		"RequestsReceived"
	};

	public static final boolean  NEGATIVE_DIFF_COUNTERS_TO_ZERO = false;
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

		return new CmSummary(counterController, guiController);
	}

	public CmSummary(ICounterController counterController, IGuiController guiController)
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
		
		// THIS IS THE SUMMARY CM, so set this
		counterController.setSummaryCm(this);
		
		addTrendGraphs();

		CounterSetTemplates.register(this);
	}


	//------------------------------------------------------------
	// Implementation
	//------------------------------------------------------------
	public static final String GRAPH_NAME_XXX             = "xxx";
	public static final String GRAPH_NAME_CPU             = "CpuGraph";
	public static final String GRAPH_NAME_NW_PACKET       = "NwPacketsGraph";
	public static final String GRAPH_NAME_COMMITS         = "CommitsGraph";
	public static final String GRAPH_NAME_REQUESTS        = "RequestsGraph";

	private void addTrendGraphs()
	{
////		String[] labels_xxx            = new String[] { "System+User CPU (@@cpu_busy + @@cpu_io)", "System CPU (@@cpu_io)", "User CPU (@@cpu_busy)" };
//		String[] labels_xxx            = new String[] { "Hour", "Minute", "Second"};
//		String[] labels_cpu            = new String[] { "System+User CPU", "System CPU", "User CPU" };
//		String[] labels_aaNwPacket     = new String[] { "PacketsReceived", "PacketsSent" };
//		String[] labels_commits        = new String[] { "Commits" };
//		String[] labels_requests       = new String[] { "ConnectionsActive", "OperationsWaiting", "OperationsActive", "OperationsActiveLoadTableStatements" };
//		
//		addTrendGraphData(GRAPH_NAME_XXX,             new TrendGraphDataPoint(GRAPH_NAME_XXX,             labels_xxx,        LabelType.Static));
//		addTrendGraphData(GRAPH_NAME_CPU,             new TrendGraphDataPoint(GRAPH_NAME_CPU,             labels_cpu,        LabelType.Static));
//		addTrendGraphData(GRAPH_NAME_NW_PACKET,       new TrendGraphDataPoint(GRAPH_NAME_NW_PACKET,       labels_aaNwPacket, LabelType.Static));
//		addTrendGraphData(GRAPH_NAME_COMMITS,         new TrendGraphDataPoint(GRAPH_NAME_COMMITS,         labels_commits,    LabelType.Static));
//		addTrendGraphData(GRAPH_NAME_REQUESTS,        new TrendGraphDataPoint(GRAPH_NAME_REQUESTS,        labels_requests,   LabelType.Static));

		// GRAPH
		addTrendGraph(GRAPH_NAME_XXX,
			"Dummy Graph", 	                        // Menu CheckBox text
			"Dummy Graph showing hour, minute, second ("+GROUP_NAME+"->"+SHORT_NAME+")", // Label 
			new String[] { "Hour", "Minute", "Second"}, 
			LabelType.Static,
			TrendGraphDataPoint.Category.OTHER,
			true,  // is Percent Graph
			true,  // visible at start
			0,     // graph is valid from Server Version. 0 = All Versions; >0 = Valid from this version and above 
			-1);   // minimum height

		// GRAPH
		addTrendGraph(GRAPH_NAME_CPU,
			"CPU Summary", 	                        // Menu CheckBox text
			"CPU Summary, using property('ProcessCPUSystem') and property('ProcessCPUUser') ("+GROUP_NAME+"->"+SHORT_NAME+")", // Label 
			new String[] { "System+User CPU", "System CPU", "User CPU" }, 
			LabelType.Static,
			TrendGraphDataPoint.Category.CPU,
			true,  // is Percent Graph
			true,  // visible at start
			0,     // graph is valid from Server Version. 0 = All Versions; >0 = Valid from this version and above 
			-1);   // minimum height

		// GRAPH
		addTrendGraph(GRAPH_NAME_NW_PACKET,
			"Network Packets received/sent", 	                            // Menu CheckBox text
			"Network Packets received/sent per second ("+GROUP_NAME+"->"+SHORT_NAME+")", // Label 
			new String[] { "PacketsReceived", "PacketsSent" }, 
			LabelType.Static,
			TrendGraphDataPoint.Category.NETWORK,
			false, // is Percent Graph
			true,  // visible at start
			0,     // graph is valid from Server Version. 0 = All Versions; >0 = Valid from this version and above 
			-1);   // minimum height

		// GRAPH
		addTrendGraph(GRAPH_NAME_COMMITS,
			"Commits", 	                            // Menu CheckBox text
			"Commits per second ("+GROUP_NAME+"->"+SHORT_NAME+")", // Label 
			new String[] { "Commits" }, 
			LabelType.Static,
			TrendGraphDataPoint.Category.OPERATIONS,
			false, // is Percent Graph
			true,  // visible at start
			0,     // graph is valid from Server Version. 0 = All Versions; >0 = Valid from this version and above 
			-1);   // minimum height

		// GRAPH
		addTrendGraph(GRAPH_NAME_REQUESTS,
			"RequestsReceived", 	            // Menu CheckBox text
			"RequestsReceived per second ("+GROUP_NAME+"->"+SHORT_NAME+")", 		// Label 
			new String[] { "ConnectionsActive", "OperationsWaiting", "OperationsActive", "OperationsActiveLoadTableStatements" }, 
			LabelType.Static,
			TrendGraphDataPoint.Category.OPERATIONS,
			false, // is Percent Graph
			true,  // visible at start
			0,     // graph is valid from Server Version. 0 = All Versions; >0 = Valid from this version and above 
			-1);   // minimum height

//		// if GUI
//		if (getGuiController() != null && getGuiController().hasGUI())
//		{
//			TrendGraph tg = null;
//
//			// GRAPH
//			tg = new TrendGraph(GRAPH_NAME_XXX,
////				"CPU Summary, Global Variables", 	                        // Menu CheckBox text
////				"CPU Summary for all Engines (using @@cpu_busy, @@cpu_io)", // Label 
//				"Dummy Graph", 	                        // Menu CheckBox text
//				"Dummy Graph showing hour, minute, second ("+GROUP_NAME+"->"+SHORT_NAME+")", // Label 
//				labels_xxx, 
//				true,  // is Percent Graph
//				this, 
//				true, // visible at start
//				0,     // graph is valid from Server Version. 0 = All Versions; >0 = Valid from this version and above 
//				-1);   // minimum height
//			addTrendGraph(tg.getName(), tg, true);
//
//			// GRAPH
//			tg = new TrendGraph(GRAPH_NAME_CPU,
//				"CPU Summary", 	                        // Menu CheckBox text
//				"CPU Summary, using property('ProcessCPUSystem') and property('ProcessCPUUser') ("+GROUP_NAME+"->"+SHORT_NAME+")", // Label 
//				labels_cpu, 
//				true,  // is Percent Graph
//				this, 
//				true, // visible at start
//				0,     // graph is valid from Server Version. 0 = All Versions; >0 = Valid from this version and above 
//				-1);   // minimum height
//			addTrendGraph(tg.getName(), tg, true);
//
//			// GRAPH
//			tg = new TrendGraph(GRAPH_NAME_NW_PACKET,
//				"Network Packets received/sent", 	                            // Menu CheckBox text
//				"Network Packets received/sent per second ("+GROUP_NAME+"->"+SHORT_NAME+")", // Label 
//				labels_aaNwPacket, 
//				false, // is Percent Graph
//				this, 
//				true, // visible at start
//				0,     // graph is valid from Server Version. 0 = All Versions; >0 = Valid from this version and above 
//				-1);   // minimum height
//			addTrendGraph(tg.getName(), tg, true);
//
//			// GRAPH
//			tg = new TrendGraph(GRAPH_NAME_COMMITS,
//				"Commits", 	                            // Menu CheckBox text
//				"Commits per second ("+GROUP_NAME+"->"+SHORT_NAME+")", // Label 
//				labels_commits, 
//				false, // is Percent Graph
//				this, 
//				true, // visible at start
//				0,     // graph is valid from Server Version. 0 = All Versions; >0 = Valid from this version and above 
//				-1);   // minimum height
//			addTrendGraph(tg.getName(), tg, true);
//
//			// GRAPH
//			tg = new TrendGraph(GRAPH_NAME_REQUESTS,
//				"RequestsReceived", 	            // Menu CheckBox text
//				"RequestsReceived per second ("+GROUP_NAME+"->"+SHORT_NAME+")", 		// Label 
//				labels_requests, 
//				false, // is Percent Graph
//				this, 
//				true, // visible at start
//				0,     // graph is valid from Server Version. 0 = All Versions; >0 = Valid from this version and above 
//				-1);   // minimum height
//			addTrendGraph(tg.getName(), tg, true);
//		}
	}

	@Override
	protected JPanel createGui()
	{
		setTabPanel(null); // Don't think this is necessary, but lets do it anyway...

		CmSummaryPanel summaryPanel = new CmSummaryPanel(this);

		// THIS IS THE SUMMARY CM, so set this
		CounterController.getInstance().setSummaryPanel( summaryPanel );

		// add listener, so that the GUI gets updated when data changes in the CM
		addTableModelListener( summaryPanel );

		return summaryPanel;
	}

	@Override
	public boolean isRefreshable()
	{
		// The SUMMARY should ALWAYS be refreshed
		return true;
	}

	@Override
	public String[] getDependsOnConfigForVersion(Connection conn, int srvVersion, boolean isClusterEnabled)
	{
		return NEED_CONFIG;
	}

	@Override
	public List<String> getPkForVersion(Connection conn, int srvVersion, boolean isClusterEnabled)
	{
		List <String> pkCols = new LinkedList<String>();
		return pkCols;
	}

	@Override
	public String getSqlForVersion(Connection conn, int aseVersion, boolean isClusterEnabled)
	{
//		String cols1, cols2, cols3;
//		cols1 = cols2 = cols3 = "";
//
//		cols1 = "* \n";
//		cols3 = "";
//
//		String fromTable = " from master..monState A \n";
//
//		String sql = "select " + cols1 + cols2 + cols3 + fromTable;
//		
//		return sql;

		return 
			"select \n"
			+ " 'atAtServerName'            = @@servername,                                                     \n"
			+ " 'atAtVersion'               = @@version,                                                        \n"
			+ " 'timeIsNow'                 = getdate(),                                                        \n"
			+ " 'utcTimeDiff'               = datediff(mi, convert(datetime,current utc timestamp), getdate()), \n"
			+ " 'srvListeners'              = convert(varchar(255),  property   ('TcpIpAddresses')),            \n"

			+ " 'ProcessID'                 = convert(int,           property   ('ProcessID')),                 \n"
			+ " 'PageSize'                  = convert(int,           property   ('PageSize')),                  \n"

			+ " 'ActiveReq'                 = convert(int,           property   ('ActiveReq')),                 \n"
			+ " 'BytesReceived'             = convert(double,        property   ('BytesReceived')),             \n"
			+ " 'BytesSent'                 = convert(double,        property   ('BytesSent')),                 \n"
			+ " 'Chkpt'                     = convert(int,           db_property('Chkpt')),                     \n"
			+ " 'CurrentCacheSize_Kb'       = convert(int,           property   ('CurrentCacheSize')),          \n"
			+ " 'LockedHeapPages'           = convert(int,           property   ('LockedHeapPages')),           \n"
			+ " 'MainHeapBytes'             = convert(double,        property   ('MainHeapBytes')),             \n"
			+ " 'MainHeapPages'             = convert(double,        property   ('MainHeapPages')),             \n"
			+ " 'MaxCacheSize'              = convert(double,        property   ('MaxCacheSize')),              \n"
			+ " 'MinCacheSize'              = convert(double,        property   ('MinCacheSize')),              \n"
			+ " 'MultiPacketsReceived'      = convert(double,        property   ('MultiPacketsReceived')),      \n"
			+ " 'MultiPacketsSent'          = convert(double,        property   ('MultiPacketsSent')),          \n"
			+ " 'NumProcessorsAvail'        = convert(int,           property   ('NumLogicalProcessorsUsed')),  \n"
			+ " 'PacketsReceived'           = convert(double,        property   ('PacketsReceived')),           \n"
			+ " 'PacketsSent'               = convert(double,        property   ('PacketsSent')),               \n"
			+ " 'PeakCacheSize'             = convert(double,        property   ('PeakCacheSize')),             \n"
			+ " 'ProcessCPU'                = convert(double,        property   ('ProcessCPU')),                \n"
			+ " 'ProcessCPUSystem'          = convert(double,        property   ('ProcessCPUSystem')),          \n"
			+ " 'ProcessCPUUser'            = convert(double,        property   ('ProcessCPUUser')),            \n"
			+ " 'ProductVersion'            =                        property   ('ProductVersion'),             \n"
			+ " 'threads'                   = convert(int,           property   ('threads')),                   \n"
			+ " 'ConnCount'                 = convert(int,           db_property('ConnCount')),                 \n"
			+ " 'UnschReq'                  = convert(int,           property   ('UnschReq')),                  \n"
			+ " 'NumLogicalProcessorsUsed'  = convert(int,           property   ('NumLogicalProcessorsUsed')),  \n"
			+ " 'NumPhysicalProcessorsUsed' = convert(int,           property   ('NumPhysicalProcessorsUsed')), \n"
			+ " 'Commit'                    = convert(double, isnull(property   ('Commit'),0)),                 \n"
			+ " 'RequestsReceived'          = convert(double, isnull(property   ('RequestsReceived'),0))        \n"
			+ "";
	}
	
	@Override
	public void updateGraphData(TrendGraphDataPoint tgdp)
	{
		int srvVersion = getServerVersion();

		//---------------------------------
		// GRAPH:
		//---------------------------------
		if (GRAPH_NAME_XXX.equals(tgdp.getName()))
		{	
			Double[] arr = new Double[3];

			int ms = (int) (System.currentTimeMillis() % 1000l);
			ms = ms < 0 ? ms+1000 : ms;

			Calendar now = Calendar.getInstance();
			int hour   = now.get(Calendar.HOUR_OF_DAY);
			int minute = now.get(Calendar.MINUTE);
			int second = now.get(Calendar.SECOND);
			
//			arr[0] = this.getAbsValueAsDouble (0, "Connections");
//			arr[1] = this.getAbsValueAsDouble (0, "distinctLogins");
//			arr[2] = this.getDiffValueAsDouble(0, "aaConnections");
			arr[0] = new Double(hour);
			arr[1] = new Double(minute);
			arr[2] = new Double(second);
			_logger.debug("updateGraphData("+tgdp.getName()+"): hour='"+arr[0]+"', minute='"+arr[1]+"', second='"+arr[2]+"'.");

			// Set the values
			tgdp.setDataPoint(this.getTimestamp(), arr);
//			tgdp.setDate(this.getTimestamp());
//			tgdp.setData(arr);
		}

		//---------------------------------
		// GRAPH:
		//---------------------------------
		if (GRAPH_NAME_CPU.equals(tgdp.getName()))
		{	
			Double[] arr = new Double[3];

			Double ProcessCPU        = getDiffValueAsDouble(0, "ProcessCPU");
			Double ProcessCPUSystem  = getDiffValueAsDouble(0, "ProcessCPUSystem");
			Double ProcessCPUUser    = getDiffValueAsDouble(0, "ProcessCPUUser");
			double interval  = getLastSampleInterval();

			if (ProcessCPU != null && ProcessCPUSystem != null && ProcessCPUUser != null)
			{
				double msCPU       = ProcessCPU      .doubleValue() * 1000;
				double msCPUUser   = ProcessCPUUser  .doubleValue() * 1000;
				double msCPUSystem = ProcessCPUSystem.doubleValue() * 1000;
				
				BigDecimal pctCPU       = new BigDecimal( (msCPU       / interval) * 100 ).setScale(1, BigDecimal.ROUND_HALF_EVEN);
				BigDecimal pctUserCPU   = new BigDecimal( (msCPUUser   / interval) * 100 ).setScale(1, BigDecimal.ROUND_HALF_EVEN);
				BigDecimal pctSystemCPU = new BigDecimal( (msCPUSystem / interval) * 100 ).setScale(1, BigDecimal.ROUND_HALF_EVEN);

				arr[0] = pctCPU      .doubleValue();
				arr[1] = pctSystemCPU.doubleValue();
				arr[2] = pctUserCPU  .doubleValue();
				_logger.debug("updateGraphData("+tgdp.getName()+"): pctCPU='"+arr[0]+"', pctSystemCPU='"+arr[1]+"', pctUserCPU='"+arr[2]+"'.");

			}
			else
			{
				arr[0] = 0.0;
				arr[1] = 0.0;
				arr[2] = 0.0;
				_logger.debug("updateGraphData("+tgdp.getName()+"): some-value-was-null... ProcessCPU='"+ProcessCPU+"', ProcessCPUSystem='"+ProcessCPUSystem+"', ProcessCPUUser='"+ProcessCPUUser+"'. Adding a 0 pct CPU Usage.");
			}
			// Set the values
			tgdp.setDataPoint(this.getTimestamp(), arr);
//			tgdp.setDate(this.getTimestamp());
//			tgdp.setData(arr);
		}


		//---------------------------------
		// GRAPH:
		//---------------------------------
		if (GRAPH_NAME_NW_PACKET.equals(tgdp.getName()))
		{	
			Double[] arr = new Double[2];

			arr[0] = this.getRateValueAsDouble (0, "PacketsReceived");
			arr[1] = this.getRateValueAsDouble (0, "PacketsSent");
			_logger.debug("updateGraphData("+tgdp.getName()+"): PacketsReceived='"+arr[0]+"', PacketsSent='"+arr[1]+"'.");

			// Set the values
			tgdp.setDataPoint(this.getTimestamp(), arr);
//			tgdp.setDate(this.getTimestamp());
//			tgdp.setData(arr);
		}

		//---------------------------------
		// GRAPH:
		//---------------------------------
		if (GRAPH_NAME_COMMITS.equals(tgdp.getName()))
		{	
			Double[] arr = new Double[1];

			arr[0] = this.getRateValueAsDouble (0, "Commit");
			_logger.debug("updateGraphData("+tgdp.getName()+"): Commit='"+arr[0]+"'.");

			// Set the values
			tgdp.setDataPoint(this.getTimestamp(), arr);
//			tgdp.setDate(this.getTimestamp());
//			tgdp.setData(arr);
		}

		//---------------------------------
		// GRAPH:
		//---------------------------------
		if (GRAPH_NAME_REQUESTS.equals(tgdp.getName()))
		{	
			Double[] arr = new Double[4];
			CountersModel _cm         = CounterController.getInstance().getCmByName(CmIqStatistics .CM_NAME);

			arr[0] = _cm.getAbsValueAsDouble("ConnectionsActive",                  "stat_value");
			arr[1] = _cm.getAbsValueAsDouble("OperationsWaiting",                  "stat_value");
			arr[2] = _cm.getAbsValueAsDouble("OperationsActive",                   "stat_value");
			arr[3] = _cm.getAbsValueAsDouble("OperationsActiveloadTableStatement", "stat_value");
		
			_logger.debug("updateGraphData("+tgdp.getName()+"): ConnectionsActive='"+arr[0]+"'.");

			// Set the values
			tgdp.setDataPoint(this.getTimestamp(), arr);
//			tgdp.setDate(this.getTimestamp());
//			tgdp.setData(arr);
		}

	}
}
