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
public class CmAdminWhoSqt
extends CountersModel
{
//	private static Logger        _logger          = Logger.getLogger(CmAdminWhoSqt.class);
	private static final long    serialVersionUID = 1L;

	public static final String   CM_NAME          = CmAdminWhoSqt.class.getSimpleName();
	public static final String   SHORT_NAME       = "SQT";
	public static final String   HTML_DESC        = 
		"<html>" +
		"<p>Stable Queue Manager Statistics</p>" +
		"</html>";

	public static final String   GROUP_NAME       = MainFrame.TCP_GROUP_SERVER;
	public static final String   GUI_ICON_FILE    = "images/"+CM_NAME+".png";

	public static final int      NEED_SRV_VERSION = 0;
	public static final int      NEED_CE_VERSION  = 0;

	public static final String[] MON_TABLES       = new String[] {"sqt"};
	public static final String[] NEED_ROLES       = new String[] {};
	public static final String[] NEED_CONFIG      = new String[] {};

	public static final String[] PCT_COLUMNS      = new String[] {};
	public static final String[] DIFF_COLUMNS     = new String[] {
		"Closed",
		"Read",
		"Open",
		"Trunc",
		"Removed",
		"Full",
		"SQM Blocked",
		"Parsed",
		"SQM Reader",
		"Change Oqids",
		"Detect Orphans"
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

		return new CmAdminWhoSqt(counterController, guiController);
	}

	public CmAdminWhoSqt(ICounterController counterController, IGuiController guiController)
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
	public static final String GRAPH_NAME_CLOSED  = "SqtClosed";
	public static final String GRAPH_NAME_READ    = "SqtRead";
	public static final String GRAPH_NAME_OPEN    = "SqtOpen";
	public static final String GRAPH_NAME_TRUNC   = "SqtTrunc";
	public static final String GRAPH_NAME_REMOVED = "SqtRemoved";
	public static final String GRAPH_NAME_PARSED  = "SqtParsed";

	private void addTrendGraphs()
	{
		String[] labels = new String[] { "-added-at-runtime-" };
		
		addTrendGraphData(GRAPH_NAME_CLOSED,  new TrendGraphDataPoint(GRAPH_NAME_CLOSED,  labels));
		addTrendGraphData(GRAPH_NAME_READ,    new TrendGraphDataPoint(GRAPH_NAME_READ,    labels));
		addTrendGraphData(GRAPH_NAME_OPEN,    new TrendGraphDataPoint(GRAPH_NAME_OPEN,    labels));
		addTrendGraphData(GRAPH_NAME_TRUNC,   new TrendGraphDataPoint(GRAPH_NAME_TRUNC,   labels));
		addTrendGraphData(GRAPH_NAME_REMOVED, new TrendGraphDataPoint(GRAPH_NAME_REMOVED, labels));
		addTrendGraphData(GRAPH_NAME_PARSED,  new TrendGraphDataPoint(GRAPH_NAME_PARSED,  labels));

		// if GUI
		if (getGuiController() != null && getGuiController().hasGUI())
		{
			// GRAPH
			TrendGraph tg = null;

			//-----
			tg = new TrendGraph(GRAPH_NAME_CLOSED,
				"SQM: Number of committed transactions in the SQT cache (col 'Closed', absolute)", // Menu CheckBox text
				"SQM: Number of committed transactions in the SQT cache (col 'Closed', absolute)", // Label 
				labels, 
				false, // is Percent Graph
				this, 
				true,  // visible at start
				0,     // graph is valid from Server Version. 0 = All Versions; >0 = Valid from this version and above 
				-1);   // minimum height
			addTrendGraph(tg.getName(), tg, true);

			//-----
			tg = new TrendGraph(GRAPH_NAME_READ,
					"SQM: Number of transactions processed, but not yet deleted (col 'Read', absolute)", // Menu CheckBox text
					"SQM: Number of transactions processed, but not yet deleted (col 'Read', absolute)", // Label 
				labels, 
				false, // is Percent Graph
				this, 
				false, // visible at start
				0,     // graph is valid from Server Version. 0 = All Versions; >0 = Valid from this version and above 
				-1);   // minimum height
			addTrendGraph(tg.getName(), tg, true);

			//-----
			tg = new TrendGraph(GRAPH_NAME_OPEN,
					"SQM: Number of uncommitted or unaborted transactions (col 'Open', absolute)", // Menu CheckBox text
					"SQM: Number of uncommitted or unaborted transactions (col 'Open', absolute)", // Label 
				labels, 
				false, // is Percent Graph
				this, 
				false, // visible at start
				0,     // graph is valid from Server Version. 0 = All Versions; >0 = Valid from this version and above 
				-1);   // minimum height
			addTrendGraph(tg.getName(), tg, true);

			//-----
			tg = new TrendGraph(GRAPH_NAME_TRUNC,
					"SQM: Number of transactions in the SQT cache, sum of Closed, Read, and Open (col 'Trunc', absolute)", // Menu CheckBox text
					"SQM: Number of transactions in the SQT cache, sum of Closed, Read, and Open (col 'Trunc', absolute)", // Label 
				labels, 
				false, // is Percent Graph
				this, 
				false, // visible at start
				0,     // graph is valid from Server Version. 0 = All Versions; >0 = Valid from this version and above 
				-1);   // minimum height
			addTrendGraph(tg.getName(), tg, true);

			//-----
			tg = new TrendGraph(GRAPH_NAME_REMOVED,
					"SQM: Number of transactions removed from memory (col 'Removed', absolute)", // Menu CheckBox text
					"SQM: Number of transactions removed from memory (col 'Removed', absolute)", // Label 
				labels, 
				false, // is Percent Graph
				this, 
				false, // visible at start
				0,     // graph is valid from Server Version. 0 = All Versions; >0 = Valid from this version and above 
				-1);   // minimum height
			addTrendGraph(tg.getName(), tg, true);

			//-----
			tg = new TrendGraph(GRAPH_NAME_PARSED,
					"SQM: Number of transactions that have been parsed (col 'Parsed', per second)", // Menu CheckBox text
					"SQM: Number of transactions that have been parsed (col 'Parsed', per second)", // Label 
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
		if (GRAPH_NAME_CLOSED.equals(tgdp.getName()))
		{
			// Write 1 "line" for every device
			Double[] dArray = new Double[this.size()];
			String[] lArray = new String[dArray.length];
			for (int i = 0; i < dArray.length; i++)
			{
				lArray[i] = this.getAbsString       (i, "Info");
				dArray[i] = this.getAbsValueAsDouble(i, "Closed");
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
				lArray[i] = this.getAbsString       (i, "Info");
				dArray[i] = this.getAbsValueAsDouble(i, "Read");
			}

			// Set the values
			tgdp.setDate(this.getTimestamp());
			tgdp.setLabel(lArray);
			tgdp.setData(dArray);
		}

		if (GRAPH_NAME_OPEN.equals(tgdp.getName()))
		{
			// Write 1 "line" for every device
			Double[] dArray = new Double[this.size()];
			String[] lArray = new String[dArray.length];
			for (int i = 0; i < dArray.length; i++)
			{
				lArray[i] = this.getAbsString       (i, "Info");
				dArray[i] = this.getAbsValueAsDouble(i, "Open");
			}

			// Set the values
			tgdp.setDate(this.getTimestamp());
			tgdp.setLabel(lArray);
			tgdp.setData(dArray);
		}

		if (GRAPH_NAME_TRUNC.equals(tgdp.getName()))
		{
			// Write 1 "line" for every device
			Double[] dArray = new Double[this.size()];
			String[] lArray = new String[dArray.length];
			for (int i = 0; i < dArray.length; i++)
			{
				lArray[i] = this.getAbsString       (i, "Info");
				dArray[i] = this.getAbsValueAsDouble(i, "Trunc");
			}

			// Set the values
			tgdp.setDate(this.getTimestamp());
			tgdp.setLabel(lArray);
			tgdp.setData(dArray);
		}

		if (GRAPH_NAME_REMOVED.equals(tgdp.getName()))
		{
			// Write 1 "line" for every device
			Double[] dArray = new Double[this.size()];
			String[] lArray = new String[dArray.length];
			for (int i = 0; i < dArray.length; i++)
			{
				lArray[i] = this.getAbsString       (i, "Info");
				dArray[i] = this.getAbsValueAsDouble(i, "Removed");
			}

			// Set the values
			tgdp.setDate(this.getTimestamp());
			tgdp.setLabel(lArray);
			tgdp.setData(dArray);
		}

		if (GRAPH_NAME_PARSED.equals(tgdp.getName()))
		{
			// Write 1 "line" for every device
			Double[] dArray = new Double[this.size()];
			String[] lArray = new String[dArray.length];
			for (int i = 0; i < dArray.length; i++)
			{
				lArray[i] = this.getRateString       (i, "Info");
				dArray[i] = this.getRateValueAsDouble(i, "Parsed");
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
			mtd.addTable("sqt",  "");

			mtd.addColumn("sqt", "Spid",           "<html>RepServer internal <i>thread id</i></html>");
			mtd.addColumn("sqt", "State",          "<html>FIXME: State</html>");
			mtd.addColumn("sqt", "Info",           "<html>FIXME: Info</html>");
			mtd.addColumn("sqt", "Closed",         "<html>The number of committed transactions in the SQT cache. The transactions have been read from the stable queue and await processing.</html>");
			mtd.addColumn("sqt", "Read",           "<html>The number of transactions processed, but not yet deleted from the queue.</html>");
			mtd.addColumn("sqt", "Open",           "<html>The number of uncommitted or unaborted transactions in the SQT cache.</html>");
			mtd.addColumn("sqt", "Trunc",          "<html>The number of transactions in the transaction cache. Trunc is the sum of the Closed, Read, and Open columns.</html>");
			mtd.addColumn("sqt", "Removed",        "<html>The number of transactions whose constituent messages have been removed from memory. This happens when the SQT processes large transactions. The messages are reread from the stable queue.</html>");
			mtd.addColumn("sqt", "Full",           "<html>Indicates that the SQT has exhausted the memory in its cache. This is not a problem as long as there are closed or read transactions still awaiting processing. If the SQT cache is often full, consider raising its configured size. To do this, see \"alter connection.\"</html>");
			mtd.addColumn("sqt", "SQM Blocked",    "<html>1 if the SQT is waiting on SQM to read a message. This state should be transitory unless there are no closed transactions.</html>");
			mtd.addColumn("sqt", "First Trans",    "<html>This column contains information about the first transaction in the queue and can be used to determine if it is an unterminated transaction. The column has three pieces of information:<br><br>ST: Followed by O (open), C (closed), R (read), or D (deleted)<br><br>Cmds: Followed by the number of commands in the first transaction<br><br>qid: Followed by the segment, block, and row of the first transaction</html>");
			mtd.addColumn("sqt", "Parsed",         "<html>The number of transactions that have been parsed.</html>");
			mtd.addColumn("sqt", "SQM Reader",     "<html>The index of the SQM reader handle.</html>");
			mtd.addColumn("sqt", "Change Oqids",   "<html>Indicates that the origin queue ID has changed.</html>");
			mtd.addColumn("sqt", "Detect Orphans", "<html>Indicates that it is doing orphan detection.</html>");
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
		String sql = "admin who, sqt, no_trunc ";
		return sql;
	}
}
