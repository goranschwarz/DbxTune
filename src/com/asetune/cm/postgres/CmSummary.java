package com.asetune.cm.postgres;

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
import com.asetune.cm.postgres.gui.CmSummaryPanel;
import com.asetune.graph.TrendGraphDataPoint;
import com.asetune.graph.TrendGraphDataPoint.LabelType;
import com.asetune.gui.TrendGraph;

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

	public static final String[] MON_TABLES       = new String[] {"pg_stat_bgwriter"};
	public static final String[] NEED_ROLES       = new String[] {};
	public static final String[] NEED_CONFIG      = new String[] {};

	public static final String[] PCT_COLUMNS      = new String[] {};
	public static final String[] DIFF_COLUMNS     = new String[] {
		"xxxx"
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
//	public static final String GRAPH_NAME_AA_NW_PACKET       = "aaPacketGraph";      // String x=GetCounters.CM_GRAPH_NAME__SUMMARY__AA_NW_PACKET;

	private void addTrendGraphs()
	{
		String[] labels_xxx            = new String[] { "Hour", "Minute", "Second"};
//		String[] labels_aaNwPacket       = new String[] { "@@pack_received", "@@pack_sent", "@@packet_errors" };
		
		addTrendGraphData(GRAPH_NAME_XXX,             new TrendGraphDataPoint(GRAPH_NAME_XXX,             labels_xxx, LabelType.Static));
//		addTrendGraphData(GRAPH_NAME_AA_NW_PACKET,       new TrendGraphDataPoint(GRAPH_NAME_AA_NW_PACKET,       labels_aaNwPacket));

		// if GUI
		if (getGuiController() != null && getGuiController().hasGUI())
		{
			TrendGraph tg = null;

			// GRAPH
			tg = new TrendGraph(GRAPH_NAME_XXX,
//				"CPU Summary, Global Variables", 	                        // Menu CheckBox text
//				"CPU Summary for all Engines (using @@cpu_busy, @@cpu_io)", // Label 
				"Dummy Graph", 	                        // Menu CheckBox text
				"Dummy Graph showing hour, minute, second", // Label 
				labels_xxx, 
				true,  // is Percent Graph
				this, 
				true, // visible at start
				0,     // graph is valid from Server Version. 0 = All Versions; >0 = Valid from this version and above 
				-1);   // minimum height
			addTrendGraph(tg.getName(), tg, true);

//			// GRAPH
//			tg = new TrendGraph(GRAPH_NAME_AA_NW_PACKET,
//					"Network Packets received/sent, Global Variables", 	                            // Menu CheckBox text
//					"Network Packets received/sent per second, using @@pack_received, @@pack_sent", // Label 
//					labels_aaNwPacket, 
//					false, // is Percent Graph
//					this, 
//					false, // visible at start
//					0,     // graph is valid from Server Version. 0 = All Versions; >0 = Valid from this version and above 
//					-1);   // minimum height
//				addTrendGraph(tg.getName(), tg, true);

		}
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
	public String getSqlForVersion(Connection conn, int srvVersion, boolean isClusterEnabled)
	{
		return 
				"select \n" + 
				"    *  \n" +
				"    ,cast( version() as varchar(255))   as version \n" +
				"    ,CURRENT_TIMESTAMP                  as time_now \n" + 
				"    ,cast( extract(epoch from (CURRENT_TIMESTAMP - CURRENT_TIMESTAMP AT TIME ZONE 'UTC'))/60 as INT) as utc_minute_diff \n" +
				"    ,pg_postmaster_start_time()         as start_time \n" +
				"from pg_catalog.pg_stat_bgwriter \n" +
				"";
	}
	
	@Override
	public void updateGraphData(TrendGraphDataPoint tgdp)
	{
//		int srvVersion = getServerVersion();

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
		}

//		//---------------------------------
//		// GRAPH:
//		//---------------------------------
//		if (GRAPH_NAME_AA_NW_PACKET.equals(tgdp.getName()))
//		{	
//			Double[] arr = new Double[2];
//
//			arr[0] = this.getRateValueAsDouble (0, "PacketsReceived");
//			arr[1] = this.getRateValueAsDouble (0, "PacketsSent");
////			arr[2] = this.getRateValueAsDouble (0, "packet_errors");
////			_logger.debug("updateGraphData(aaPacketGraph): packet_errors='"+arr[0]+"', total_errors='"+arr[1]+"', packet_errors='"+arr[2]+"'.");
//			_logger.debug("updateGraphData(aaPacketGraph): PacketsReceived='"+arr[0]+"', PacketsSent='"+arr[1]+"'.");
//
//			// Set the values
//			tgdp.setDate(this.getTimestamp());
//			tgdp.setData(arr);
//		}

	}
}
