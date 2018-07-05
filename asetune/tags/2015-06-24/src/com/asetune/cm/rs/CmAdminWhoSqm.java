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
public class CmAdminWhoSqm
extends CountersModel
{
//	private static Logger        _logger          = Logger.getLogger(CmAdminWhoSqm.class);
	private static final long    serialVersionUID = 1L;

	public static final String   CM_NAME          = CmAdminWhoSqm.class.getSimpleName();
	public static final String   SHORT_NAME       = "SQM";
	public static final String   HTML_DESC        = 
		"<html>" +
		"<p>Stable Queue Manager Statistics</p>" +
		"</html>";

	public static final String   GROUP_NAME       = MainFrame.TCP_GROUP_SERVER;
	public static final String   GUI_ICON_FILE    = "images/"+CM_NAME+".png";

	public static final int      NEED_SRV_VERSION = 0;
	public static final int      NEED_CE_VERSION  = 0;

	public static final String[] MON_TABLES       = new String[] {"sqm"};
	public static final String[] NEED_ROLES       = new String[] {};
	public static final String[] NEED_CONFIG      = new String[] {};

	public static final String[] PCT_COLUMNS      = new String[] {};
	public static final String[] DIFF_COLUMNS     = new String[] {
		"Duplicates",
		"Writes",
		"Reads",
		"Bytes",
		"B Writes",
		"B Filled",
		"B Reads",
		"B Cache"
//		"Readers",
//		"Truncs"
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

		return new CmAdminWhoSqm(counterController, guiController);
	}

	public CmAdminWhoSqm(ICounterController counterController, IGuiController guiController)
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
	public static final String GRAPH_NAME_WRITES          = "SqmWrites";
	public static final String GRAPH_NAME_READ            = "SqmRead";
	public static final String GRAPH_NAME_BLK_READS       = "SqmBlocksReads";
	public static final String GRAPH_NAME_BLK_CACHE_READS = "SqmBlocksCacheReads";

	private void addTrendGraphs()
	{
		String[] labels = new String[] { "-added-at-runtime-" };
		
		addTrendGraphData(GRAPH_NAME_WRITES,          new TrendGraphDataPoint(GRAPH_NAME_WRITES,          labels));
		addTrendGraphData(GRAPH_NAME_READ,            new TrendGraphDataPoint(GRAPH_NAME_READ,            labels));
		addTrendGraphData(GRAPH_NAME_BLK_READS,       new TrendGraphDataPoint(GRAPH_NAME_BLK_READS,       labels));
		addTrendGraphData(GRAPH_NAME_BLK_CACHE_READS, new TrendGraphDataPoint(GRAPH_NAME_BLK_CACHE_READS, labels));

		// if GUI
		if (getGuiController() != null && getGuiController().hasGUI())
		{
			// GRAPH
			TrendGraph tg = null;

			//-----
			tg = new TrendGraph(GRAPH_NAME_WRITES,
				"SQM: Number of messages written into the queue (col 'Writes', per second)", // Menu CheckBox text
				"SQM: Number of messages written into the queue (col 'Writes', per second)", // Label 
				labels, 
				false, // is Percent Graph
				this, 
				true,  // visible at start
				0,     // graph is valid from Server Version. 0 = All Versions; >0 = Valid from this version and above 
				-1);   // minimum height
			addTrendGraph(tg.getName(), tg, true);

			//-----
			tg = new TrendGraph(GRAPH_NAME_READ,
					"SQM: Number of messages read from the queue (col 'Read', per second)", // Menu CheckBox text
					"SQM: Number of messages read from the queue (col 'Read', per second)", // Label 
				labels, 
				false, // is Percent Graph
				this, 
				false, // visible at start
				0,     // graph is valid from Server Version. 0 = All Versions; >0 = Valid from this version and above 
				-1);   // minimum height
			addTrendGraph(tg.getName(), tg, true);

			//-----
			tg = new TrendGraph(GRAPH_NAME_BLK_READS,
					"SQM: Number of 16K blocks read (col 'B Reads', per second)", // Menu CheckBox text
					"SQM: Number of 16K blocks read (col 'B Reads', per second)", // Label 
				labels, 
				false, // is Percent Graph
				this, 
				false, // visible at start
				0,     // graph is valid from Server Version. 0 = All Versions; >0 = Valid from this version and above 
				-1);   // minimum height
			addTrendGraph(tg.getName(), tg, true);

			//-----
			tg = new TrendGraph(GRAPH_NAME_BLK_CACHE_READS,
					"SQM: Number of 16K blocks read that are cached (col 'B Cache', per second)", // Menu CheckBox text
					"SQM: Number of 16K blocks read that are cached (col 'B Cache', per second)", // Label 
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
		if (GRAPH_NAME_WRITES.equals(tgdp.getName()))
		{
			// Write 1 "line" for every device
			Double[] dArray = new Double[this.size()];
			String[] lArray = new String[dArray.length];
			for (int i = 0; i < dArray.length; i++)
			{
				lArray[i] = this.getRateString       (i, "Info");
				dArray[i] = this.getRateValueAsDouble(i, "Writes");
			}

			// Set the values
			tgdp.setDate(this.getTimestamp());
			tgdp.setLabel(lArray);
			tgdp.setData(dArray);
		}

		if (GRAPH_NAME_READ.equals(tgdp.getName()))
		{
			// Write 1 "line" for every device
			Double[] dArray = new Double[this.size()];
			String[] lArray = new String[dArray.length];
			for (int i = 0; i < dArray.length; i++)
			{
				lArray[i] = this.getRateString       (i, "Info");
				dArray[i] = this.getRateValueAsDouble(i, "Read");
			}

			// Set the values
			tgdp.setDate(this.getTimestamp());
			tgdp.setLabel(lArray);
			tgdp.setData(dArray);
		}

		if (GRAPH_NAME_BLK_READS.equals(tgdp.getName()))
		{
			// Write 1 "line" for every device
			Double[] dArray = new Double[this.size()];
			String[] lArray = new String[dArray.length];
			for (int i = 0; i < dArray.length; i++)
			{
				lArray[i] = this.getRateString       (i, "Info");
				dArray[i] = this.getRateValueAsDouble(i, "B Reads");
			}

			// Set the values
			tgdp.setDate(this.getTimestamp());
			tgdp.setLabel(lArray);
			tgdp.setData(dArray);
		}

		if (GRAPH_NAME_BLK_CACHE_READS.equals(tgdp.getName()))
		{
			// Write 1 "line" for every device
			Double[] dArray = new Double[this.size()];
			String[] lArray = new String[dArray.length];
			for (int i = 0; i < dArray.length; i++)
			{
				lArray[i] = this.getRateString       (i, "Info");
				dArray[i] = this.getRateValueAsDouble(i, "B Cache");
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
			mtd.addTable("sqm",  "");

			mtd.addColumn("sqm", "Spid",            "<html>RepServer internal <i>thread id</i></html>");
			mtd.addColumn("sqm", "State",           "<html>FIXME: State</html>");
			mtd.addColumn("sqm", "Info",            "<html>FIXME: Info</html>");
			mtd.addColumn("sqm", "Duplicates",      "<html>The number of duplicate messages detected and ignored. There are usually some duplicate messages at start-up.</html>");
			mtd.addColumn("sqm", "Writes",          "<html>The number of messages written into the queue.</html>");
			mtd.addColumn("sqm", "Read",            "<html>The number of messages read from the queue. This usually exceeds the number of writes because the last segment is read at start-up to determine where writing is to begin. Also, long transactions may cause messages to be reread.</html>");
			mtd.addColumn("sqm", "Bytes",           "<html>The number of bytes written.</html>");
			mtd.addColumn("sqm", "B Writes",        "<html>The number of 16K blocks written. It may be greater than Bytes/16K because not every 16K block written is full. You can determine the density of blocks by dividing Bytes by B�Writes.</html>");
			mtd.addColumn("sqm", "B Filled",        "<html>The number of 16K blocks written to disk because they are filled.</html>");
			mtd.addColumn("sqm", "B Reads",         "<html>The number of 16K blocks read.</html>");
			mtd.addColumn("sqm", "B Cache",         "<html>The number of 16K blocks read that are in cache.</html>");
			mtd.addColumn("sqm", "Save_Int:Seg",    "<html>The Save_Int interval and the oldest segment in the Save_Int list. The Save_Int interval is the number of minutes the Replication Server maintains an SQM segment after all messages in the segment have been acknowledged by targets.<br><br>For example, a value of 5:88 indicates a Save_Int interval of 5 minutes, where segment 88 is the oldest segment in the Save_Int list.<br><br>This feature provides redundancy in the event of replication system failure. For example, a Replication Server could lose its disk partitions while receiving data from another Replication Server. The Save_Int feature lets the sending Replication Server re-create all messages saved during the Save_Int interval.<br><br>A Save_Int value of �strict� may be used when a queue is read by more than one reader thread. Replication Server maintains the SQM segment until all threads reading the queue have read the messages on the segment and applied them to their destination.</html>");
			mtd.addColumn("sqm", "First Seg.Block", "<html>The first undeleted segment and block number in the queue. If the figures for First Seg.Block and Last Seg.Block do not match, data remains in the queue for processing.<br><br>This information is useful when dumping queues. For more information, refer to the Replication Server Troubleshooting Guide.</html>");
			mtd.addColumn("sqm", "Last Seg.Block",  "<html>The last segment and block written to the queue. If the figures for First Seg.Block and Last Seg.Block do not match, data remains in the queue for processing.<br><br>This information is useful when dumping queues. For more information, refer to the Replication Server Troubleshooting Guide.</html>");
			mtd.addColumn("sqm", "Next Read",       "<html>The next segment, block, and row to be read from the queue.</html>");
			mtd.addColumn("sqm", "Readers",         "<html>The number of threads that are reading the queue.</html>");
			mtd.addColumn("sqm", "Truncs",          "<html>The number of truncation points for the queue.</html>");
			mtd.addColumn("sqm", "Loss Status",     "<html>FIXME: Loss Status</html>");
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
		String sql = "admin who, sqm, no_trunc ";
		return sql;
	}
}