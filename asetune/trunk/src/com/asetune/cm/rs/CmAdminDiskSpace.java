package com.asetune.cm.rs;

import java.sql.Connection;
import java.util.LinkedList;
import java.util.List;

import javax.naming.NameNotFoundException;

import com.asetune.ICounterController;
import com.asetune.IGuiController;
import com.asetune.cm.CounterSetTemplates;
import com.asetune.cm.CounterSetTemplates.Type;
import com.asetune.cm.CountersModel;
import com.asetune.config.dict.MonTablesDictionary;
import com.asetune.config.dict.MonTablesDictionaryManager;
import com.asetune.graph.TrendGraphDataPoint;
import com.asetune.gui.MainFrame;
import com.asetune.gui.TrendGraph;

/**
 * @author Goran Schwarz (goran_schwarz@hotmail.com)
 */
public class CmAdminDiskSpace
extends CountersModel
{
//	private static Logger        _logger          = Logger.getLogger(CmAdminDiskSpace.class);
	private static final long    serialVersionUID = 1L;

	public static final String   CM_NAME          = CmAdminDiskSpace.class.getSimpleName();
	public static final String   SHORT_NAME       = "Partitions";
	public static final String   HTML_DESC        = 
		"<html>" +
		"<p>Replication Server Stable Queueu Partitions Information</p>" +
		"</html>";

	public static final String   GROUP_NAME       = MainFrame.TCP_GROUP_DISK;
	public static final String   GUI_ICON_FILE    = "images/"+CM_NAME+".png";

	public static final int      NEED_SRV_VERSION = 0;
	public static final int      NEED_CE_VERSION  = 0;

	public static final String[] MON_TABLES       = new String[] {"disk_space"};
	public static final String[] NEED_ROLES       = new String[] {};
	public static final String[] NEED_CONFIG      = new String[] {};

	public static final String[] PCT_COLUMNS      = new String[] {};
	public static final String[] DIFF_COLUMNS     = new String[] {
		"Used Segs"
		};

	public static final boolean  NEGATIVE_DIFF_COUNTERS_TO_ZERO = false;
	public static final boolean  IS_SYSTEM_CM                   = true;
	public static final int      DEFAULT_POSTPONE_TIME          = 0;
	public static final int      DEFAULT_QUERY_TIMEOUT          = CountersModel.DEFAULT_sqlQueryTimeout;;

	@Override public int     getDefaultPostponeTime()                 { return DEFAULT_POSTPONE_TIME; }
	@Override public int     getDefaultQueryTimeout()                 { return DEFAULT_QUERY_TIMEOUT; }
	@Override public boolean getDefaultIsNegativeDiffCountersToZero() { return NEGATIVE_DIFF_COUNTERS_TO_ZERO; }
	@Override public Type    getTemplateLevel()                       { return Type.ALL; }

	/**
	 * FACTORY  method to create the object
	 */
	public static CountersModel create(ICounterController counterController, IGuiController guiController)
	{
		if (guiController != null && guiController.hasGUI())
			guiController.splashWindowProgress("Loading: Counter Model '"+CM_NAME+"'");

		return new CmAdminDiskSpace(counterController, guiController);
	}

	public CmAdminDiskSpace(ICounterController counterController, IGuiController guiController)
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
		
		addTrendGraphs();
		
		CounterSetTemplates.register(this);
	}


	//------------------------------------------------------------
	// Implementation
	//------------------------------------------------------------
	public static final String GRAPH_NAME_QUEUE_SIZE = "QueueSize";

	private void addTrendGraphs()
	{
		String[] labels = new String[] { "-added-at-runtime-" };
		
		addTrendGraphData(GRAPH_NAME_QUEUE_SIZE,       new TrendGraphDataPoint(GRAPH_NAME_QUEUE_SIZE,       labels));

		// if GUI
		if (getGuiController() != null && getGuiController().hasGUI())
		{
			// GRAPH
			TrendGraph tg = null;

			//-----
			tg = new TrendGraph(GRAPH_NAME_QUEUE_SIZE,
				"Stable Device Usage, from 'admin disk_space' (Absolute Value)", // Menu CheckBox text
				"Stable Device Usage, from 'admin disk_space' (Absolute Value)", // Label 
				labels, 
				false, // is Percent Graph
				this, 
				true,  // visible at start
				0,     // graph is valid from Server Version. 0 = All Versions; >0 = Valid from this version and above 
				-1);   // minimum height
			addTrendGraph(tg.getName(), tg, true);
		}
	}

	@Override
	public void updateGraphData(TrendGraphDataPoint tgdp)
	{
		if (GRAPH_NAME_QUEUE_SIZE.equals(tgdp.getName()))
		{
			// Write 1 "line" for every device
			Double[] dArray = new Double[this.size() + 1];
			String[] lArray = new String[dArray.length];

			lArray[0] = "Sum-All-Devices";
			dArray[0] = this.getAbsValueSum("Used Segs");

			for (int i = 0; i < this.size(); i++)
			{
				lArray[i+1] = this.getAbsString       (i, "Logical");
				dArray[i+1] = this.getAbsValueAsDouble(i, "Used Segs");
			}

			// Set the values
			tgdp.setDate(this.getTimestamp());
			tgdp.setLabel(lArray);
			tgdp.setData(dArray);
		}
	}
	
//	1> admin disk_space
//	+--------------------------------+-------+-------+----------+---------+----------+
//	|Partition                       |Logical|Part.Id|Total Segs|Used Segs|State     |
//	+--------------------------------+-------+-------+----------+---------+----------+
//	|c:\sybase\devices\GORAN_1_RS.sd1|sd1    |101    |200       |0        |ON-LINE///|
//	+--------------------------------+-------+-------+----------+---------+----------+
//	Rows 1
//	(1 rows affected)

//	@Override
//	protected TabularCntrPanel createGui()
//	{
//		return new CmRaSysmonPanel(this);
//	}

	@Override
	public String[] getDependsOnConfigForVersion(Connection conn, int srvVersion, boolean isClusterEnabled)
	{
		return NEED_CONFIG;
	}

	@Override
	public void addMonTableDictForVersion(Connection conn, int srvVersion, boolean isClusterEnabled)
	{
		try 
		{
			MonTablesDictionary mtd = MonTablesDictionaryManager.getInstance();
			mtd.addTable("disk_space",  "");

			mtd.addColumn("disk_space", "Partition",     "<html>Physical name of the Partition</html>");
			mtd.addColumn("disk_space", "Logical",       "<html>Physical name of the Partition</html>");
			mtd.addColumn("disk_space", "Part.Id",       "<html>ID of the Partition</html>");
			mtd.addColumn("disk_space", "Total Segs",    "<html>How many MB does this Partition hold</html>");
			mtd.addColumn("disk_space", "Used Segs",     "<html>Number of <b>used</b> segments in MB<br>Note 1: 0 means it has never been used<br>Note 2: If it has ever been used it can not go below 1, even if it's empty.</html>");
			mtd.addColumn("disk_space", "State",         "<html>In what <i>state</i> the Partition is in.<br>"
					+ "<ul>"
					+ "  <li>ON-LINE  � The device is normal</li>"
					+ "  <li>OFF-LINE � The device cannot be found</li>"
					+ "  <li>DROPPED  � The device has been dropped but has not disappeared (some queues are using it)</li>"
					+ "  <li>AUTO     � The device is automatically resizable. See Automatically Resizable Partitions in the Replication Server Administration Guide Volume 1.</li>"
					+ "</ul>"
					+ "</html>");
		}
		catch (NameNotFoundException e) {/*ignore*/}
	}

	@Override
	public List<String> getPkForVersion(Connection conn, int srvVersion, boolean isClusterEnabled)
	{
		List <String> pkCols = new LinkedList<String>();

		pkCols.add("Partition");

		return pkCols;
	}

	@Override
	public String getSqlForVersion(Connection conn, int srvVersion, boolean isClusterEnabled)
	{
		String sql = "admin disk_space";
		return sql;
	}
}
