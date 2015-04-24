package com.asetune.cm.ase;

import java.sql.Connection;
import java.util.LinkedList;
import java.util.List;

import org.apache.log4j.Logger;

import com.asetune.ICounterController;
import com.asetune.IGuiController;
import com.asetune.cm.CounterSetTemplates;
import com.asetune.cm.CounterSetTemplates.Type;
import com.asetune.cm.CountersModel;
import com.asetune.graph.TrendGraphDataPoint;
import com.asetune.gui.MainFrame;
import com.asetune.gui.TrendGraph;
import com.asetune.utils.Ver;

/**
 * @author Goran Schwarz (goran_schwarz@hotmail.com)
 */
public class CmIoControllers
extends CountersModel
{
	private static Logger        _logger          = Logger.getLogger(CmIoControllers.class);
	private static final long    serialVersionUID = 1L;

	public static final String   CM_NAME          = CmIoControllers.class.getSimpleName();
	public static final String   SHORT_NAME       = "IO Controllers";
	public static final String   HTML_DESC        = 
		"<html>" +
		"Provides information on I/O controllers." +
		"</html>";

	public static final String   GROUP_NAME       = MainFrame.TCP_GROUP_DISK;
	public static final String   GUI_ICON_FILE    = "images/"+CM_NAME+".png";

//	public static final int      NEED_SRV_VERSION = 15700;
//	public static final int      NEED_SRV_VERSION = 1570000;
	public static final int      NEED_SRV_VERSION = Ver.ver(15,7);
	public static final int      NEED_CE_VERSION  = 0;

	public static final String[] MON_TABLES       = new String[] {"monIOController"};
	public static final String[] NEED_ROLES       = new String[] {"mon_role"};
	public static final String[] NEED_CONFIG      = new String[] {};

	public static final String[] PCT_COLUMNS      = new String[] {};
	public static final String[] DIFF_COLUMNS     = new String[] {
		"BlockingPolls", "NonBlockingPolls", "EventPolls", "NonBlockingEventPolls", "FullPolls", 
		"Events", "Completed",   // Pending seems not to be a incremental counter
		"Reads", "Writes", "Deferred"};

	public static final boolean  NEGATIVE_DIFF_COUNTERS_TO_ZERO = true;
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

		return new CmIoControllers(counterController, guiController);
	}

	/**
	 * Constructor
	 */
	public CmIoControllers(ICounterController counterController, IGuiController guiController)
	{
		super(CM_NAME, GROUP_NAME, /*sql*/null, /*pkList*/null, 
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
	
	public static final String   GRAPH_NAME_PENDING_DISK_IO   = "pendingDiskIo"; 
	public static final String   GRAPH_NAME_COMPLETED_DISK_IO = "completedDiskIo"; 

	private void addTrendGraphs()
	{
		String[] labels1 = new String[] { "Pending" };
		String[] labels2 = new String[] { "Completed" };
		
		addTrendGraphData(GRAPH_NAME_PENDING_DISK_IO,   new TrendGraphDataPoint(GRAPH_NAME_PENDING_DISK_IO,   labels1));
		addTrendGraphData(GRAPH_NAME_COMPLETED_DISK_IO, new TrendGraphDataPoint(GRAPH_NAME_COMPLETED_DISK_IO, labels2));

		// if GUI
		if (getGuiController() != null && getGuiController().hasGUI())
		{
			// GRAPH
			TrendGraph tg = null;
			tg = new TrendGraph(GRAPH_NAME_PENDING_DISK_IO,
					"Pending DiskIO's", 	                                 // Menu CheckBox text
					"Pending DiskIO's, or number of outstanding (from IOController)", // Label 
					labels1, 
					false, // is Percent Graph
					this, 
					false, // visible at start
					0,    // graph is valid from Server Version. 0 = All Versions; >0 = Valid from this version and above 
					-1);  // minimum height
			addTrendGraph(tg.getName(), tg, true);

			// GRAPH
			tg = new TrendGraph(GRAPH_NAME_COMPLETED_DISK_IO,
					"Completed DiskIO's", 	                                 // Menu CheckBox text
					"Completed DiskIO's per Seconds (from IOController)", // Label 
					labels2, 
					false, // is Percent Graph
					this, 
					false, // visible at start
					0,    // graph is valid from Server Version. 0 = All Versions; >0 = Valid from this version and above 
					-1);  // minimum height
			addTrendGraph(tg.getName(), tg, true);
		}
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

		pkCols.add("InstanceID");
		pkCols.add("ControllerID");

		return pkCols;
	}

	@Override
	public String getSqlForVersion(Connection conn, int aseVersion, boolean isClusterEnabled)
	{
		String sql = "select * from master..monIOController";
		return sql;
	}

	@Override
	public void updateGraphData(TrendGraphDataPoint tgdp)
	{
		if (GRAPH_NAME_PENDING_DISK_IO.equals(tgdp.getName()))
		{
    		int[] rqRows = this.getAbsRowIdsWhere("Type", "DiskController");
    		if (rqRows == null)
    			_logger.warn("When updateGraphData for '"+tgdp.getName()+"', getAbsRowIdsWhere('Type', 'DiskController'), retuned null, so I can't do more here.");
    		else
    		{
    			Double[] arr = new Double[1];
    			arr[0] = this.getAbsValueSum(rqRows, "Pending");
    			_logger.debug("updateGraphData("+GRAPH_NAME_PENDING_DISK_IO+"): Pending='"+arr[0]+"'.");
    
    			// Set the values
    			tgdp.setDate(this.getTimestamp());
    			tgdp.setData(arr);
    		}
		}

		if (GRAPH_NAME_COMPLETED_DISK_IO.equals(tgdp.getName()))
		{
    		int[] rqRows = this.getAbsRowIdsWhere("Type", "DiskController");
    		if (rqRows == null)
    			_logger.warn("When updateGraphData for '"+tgdp.getName()+"', getAbsRowIdsWhere('Type', 'DiskController'), retuned null, so I can't do more here.");
    		else
    		{
    			Double[] arr = new Double[1];
    			arr[0] = this.getRateValueSum(rqRows, "Completed");
    			_logger.debug("updateGraphData("+GRAPH_NAME_COMPLETED_DISK_IO+"): Completed='"+arr[0]+"'.");
    
    			// Set the values
    			tgdp.setDate(this.getTimestamp());
    			tgdp.setData(arr);
    		}
		}
	}
}
