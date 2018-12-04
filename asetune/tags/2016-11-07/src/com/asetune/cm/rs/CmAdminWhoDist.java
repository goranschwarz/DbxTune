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
public class CmAdminWhoDist
extends CountersModel
{
//	private static Logger        _logger          = Logger.getLogger(CmAdminWhoDist.class);
	private static final long    serialVersionUID = 1L;

	public static final String   CM_NAME          = CmAdminWhoDist.class.getSimpleName();
	public static final String   SHORT_NAME       = "Dist";
	public static final String   HTML_DESC        = 
		"<html>" +
		"<p>Distributor Statistics</p>" +
		"</html>";

	public static final String   GROUP_NAME       = MainFrame.TCP_GROUP_SERVER;
	public static final String   GUI_ICON_FILE    = "images/"+CM_NAME+".png";

	public static final int      NEED_SRV_VERSION = 0;
	public static final int      NEED_CE_VERSION  = 0;

	public static final String[] MON_TABLES       = new String[] {"dist"};
	public static final String[] NEED_ROLES       = new String[] {};
	public static final String[] NEED_CONFIG      = new String[] {};

	public static final String[] PCT_COLUMNS      = new String[] {};
	public static final String[] DIFF_COLUMNS     = new String[] {
		"PendingCmds",
//		"SqtBlocked",
		"Duplicates",
		"TransProcessed",
		"CmdsProcessed",
		"MaintUserCmds",
		"NoRepdefCmds",
		"CmdsIgnored",
		"CmdMarkers",
		"RSTicket",
		"SqtMaxCache",
		};

	public static final boolean  NEGATIVE_DIFF_COUNTERS_TO_ZERO = true;
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

		return new CmAdminWhoDist(counterController, guiController);
	}

	public CmAdminWhoDist(ICounterController counterController, IGuiController guiController)
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
	public static final String GRAPH_NAME_TRANS_PROCESSED    = "DistTransactions";
	public static final String GRAPH_NAME_CMD_PROCESSED      = "DistCommands";

	private void addTrendGraphs()
	{
		String[] labels = new String[] { "-added-at-runtime-" };
		
		addTrendGraphData(GRAPH_NAME_TRANS_PROCESSED,     new TrendGraphDataPoint(GRAPH_NAME_TRANS_PROCESSED,     labels));
		addTrendGraphData(GRAPH_NAME_CMD_PROCESSED,       new TrendGraphDataPoint(GRAPH_NAME_CMD_PROCESSED,       labels));

		// if GUI
		if (getGuiController() != null && getGuiController().hasGUI())
		{
			// GRAPH
			TrendGraph tg = null;

			//-----
			tg = new TrendGraph(GRAPH_NAME_TRANS_PROCESSED,
				"DIST: Number of Transactions (per second)", // Menu CheckBox text
				"DIST: Number of Transactions (per second)", // Label 
				labels, 
				false, // is Percent Graph
				this, 
				true,  // visible at start
				0,     // graph is valid from Server Version. 0 = All Versions; >0 = Valid from this version and above 
				-1);   // minimum height
			addTrendGraph(tg.getName(), tg, true);

			//-----
			tg = new TrendGraph(GRAPH_NAME_CMD_PROCESSED,
					"DIST: Number of Commands (per second)", // Menu CheckBox text
					"DIST: Number of Commands (per second)", // Label 
				labels, 
				false, // is Percent Graph
				this, 
				false, // visible at start
				0,     // graph is valid from Server Version. 0 = All Versions; >0 = Valid from this version and above 
				-1);   // minimum height
			addTrendGraph(tg.getName(), tg, true);

		}
	}

	@Override
	public void updateGraphData(TrendGraphDataPoint tgdp)
	{
		if (GRAPH_NAME_TRANS_PROCESSED.equals(tgdp.getName()))
		{
			// Write 1 "line" for every device
			Double[] dArray = new Double[this.size()];
			String[] lArray = new String[dArray.length];
			for (int i = 0; i < dArray.length; i++)
			{
				lArray[i] = this.getRateString       (i, "Info");
				dArray[i] = this.getRateValueAsDouble(i, "TransProcessed");
			}

			// Set the values
			tgdp.setDate(this.getTimestamp());
			tgdp.setLabel(lArray);
			tgdp.setData(dArray);
		}

		if (GRAPH_NAME_CMD_PROCESSED.equals(tgdp.getName()))
		{
			// Write 1 "line" for every device
			Double[] dArray = new Double[this.size()];
			String[] lArray = new String[dArray.length];
			for (int i = 0; i < dArray.length; i++)
			{
				lArray[i] = this.getRateString       (i, "Info");
				dArray[i] = this.getRateValueAsDouble(i, "CmdsProcessed");
			}

			// Set the values
			tgdp.setDate(this.getTimestamp());
			tgdp.setLabel(lArray);
			tgdp.setData(dArray);
		}
	}

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
			mtd.addTable("dist",  "");

			mtd.addColumn("dist", "Spid",           "<html>RepServer internal <i>thread id</i></html>");
			mtd.addColumn("dist", "State",          "<html>FIXME: State</html>");
			mtd.addColumn("dist", "Info",           "<html>FIXME: Info</html>");
			mtd.addColumn("dist", "PrimarySite",    "<html>The ID of the primary database for the SQT thread.</html>");
			mtd.addColumn("dist", "Type",           "<html>The thread is a physical or logical connection.</html>");
			mtd.addColumn("dist", "Status",         "<html>The thread has a status of �normal� or �ignoring.�</html>");
			mtd.addColumn("dist", "PendingCmds",    "<html>The number of commands that are pending for the thread.</html>");
			mtd.addColumn("dist", "SqtBlocked",     "<html>Whether or not the thread is waiting for the SQT.</html>");
			mtd.addColumn("dist", "Duplicates",     "<html>The number of duplicate commands the thread has seen and dropped.</html>");
			mtd.addColumn("dist", "TransProcessed", "<html>The number of transactions that have been processed by the thread.</html>");
			mtd.addColumn("dist", "CmdsProcessed",  "<html>The number of commands that have been processed by the thread.</html>");
			mtd.addColumn("dist", "MaintUserCmds",  "<html>The number of commands belonging to the maintenance user.</html>");
			mtd.addColumn("dist", "NoRepdefCmds",   "<html>The number of commands dropped because no corresponding table replication definitions were defined.<br><br>In the case of Warm Standby, it is possible to have Rep Server create the replication definition. In multi-site availability (MSA), one defines database replication definitions. In either of these cases, if the replicated data originates from a source without a table replication definition, the counter is increased and replicated data proceeds to the target.</html>");
			mtd.addColumn("dist", "CmdsIgnored",    "<html>The number of commands dropped before the status became \"normal.\"</html>");
			mtd.addColumn("dist", "CmdMarkers",     "<html>The number of special markers that have been processed.</html>");
			mtd.addColumn("dist", "RSTicket",       "<html>The number of rs_ticket subcommands that have been processed by a DIST thread, if the Replication Server stats_sampling parameter is on.</html>");
			mtd.addColumn("dist", "SqtMaxCache",    "<html>Maximum SQT (Stable Queue Transaction interface) cache memory for the database connection, in bytes.<br>The default, 0, means that the current setting of sqt_max_cache_size is used as the maximum cache size for the connection.</html>");
		}
		catch (NameNotFoundException e) {/*ignore*/}
	}

	@Override
	public List<String> getPkForVersion(Connection conn, int srvVersion, boolean isClusterEnabled)
	{
		List <String> pkCols = new LinkedList<String>();

		pkCols.add("Spid");

		return pkCols;
	}

	@Override
	public String getSqlForVersion(Connection conn, int srvVersion, boolean isClusterEnabled)
	{
		String sql = "admin who, dist, no_trunc ";
		return sql;
	}
}