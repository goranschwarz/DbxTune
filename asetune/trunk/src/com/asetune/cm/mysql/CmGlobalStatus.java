package com.asetune.cm.mysql;

import java.awt.event.MouseEvent;
import java.math.BigDecimal;
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
import com.asetune.config.dict.MySqlVariablesDictionary;
import com.asetune.graph.TrendGraphDataPoint;
import com.asetune.graph.TrendGraphDataPoint.LabelType;
import com.asetune.gui.MainFrame;
import com.asetune.gui.TrendGraph;

/**
 * @author Goran Schwarz (goran_schwarz@hotmail.com)
 */
public class CmGlobalStatus
extends CountersModel
{
//	private static Logger        _logger          = Logger.getLogger(CmGlobalStatus.class);
	private static final long    serialVersionUID = 1L;

	public static final String   CM_NAME          = CmGlobalStatus.class.getSimpleName();
	public static final String   SHORT_NAME       = "Global Status";
	public static final String   HTML_DESC        = 
		"<html>"
		+ "<h4>Global Status</h4>"
		+ "Simply select from performance_schema.global_status"
		+ "</html>";

	public static final String   GROUP_NAME       = MainFrame.TCP_GROUP_SERVER;
	public static final String   GUI_ICON_FILE    = "images/"+CM_NAME+".png";

	public static final int      NEED_SRV_VERSION = 0;
	public static final int      NEED_CE_VERSION  = 0;

	public static final String[] MON_TABLES       = new String[] {"global_status"};
	public static final String[] NEED_ROLES       = new String[] {};
	public static final String[] NEED_CONFIG      = new String[] {};

	public static final String[] PCT_COLUMNS      = new String[] {};
	public static final String[] DIFF_COLUMNS     = new String[] {"VARIABLE_VALUE"};

	public static final boolean  NEGATIVE_DIFF_COUNTERS_TO_ZERO = true;
	public static final boolean  IS_SYSTEM_CM                   = true;
	public static final int      DEFAULT_POSTPONE_TIME          = 0;
	public static final int      DEFAULT_QUERY_TIMEOUT          = CountersModel.DEFAULT_sqlQueryTimeout;

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

		return new CmGlobalStatus(counterController, guiController);
	}

	public CmGlobalStatus(ICounterController counterController, IGuiController guiController)
	{
		super(counterController,
				CM_NAME, GROUP_NAME, /*sql*/null, /*pkList*/null, 
				DIFF_COLUMNS, PCT_COLUMNS, MON_TABLES, 
				NEED_ROLES, NEED_CONFIG, NEED_SRV_VERSION, NEED_CE_VERSION, 
				NEGATIVE_DIFF_COUNTERS_TO_ZERO, IS_SYSTEM_CM, DEFAULT_POSTPONE_TIME);

		setDisplayName(SHORT_NAME);
		setDescription(HTML_DESC);

		setIconFile(GUI_ICON_FILE);

		setShowClearTime(false);
		setBackgroundDataPollingEnabled(true, false);
		
		setCounterController(counterController);
		setGuiController(guiController);
		
		addTrendGraphs();
		
		CounterSetTemplates.register(this);
	}


	//------------------------------------------------------------
	// Implementation
	//------------------------------------------------------------
	
//	@Override
//	protected TabularCntrPanel createGui()
//	{
//		return new CmGlobalStatusPanel(this);
//	}

	// NOTE: storage table name will be CmName_GraphName, so try to keep the name short
	public static final String GRAPH_NAME_KB_RECV_SENT = "ClRecvSent"; 
	public static final String GRAPH_NAME_QUESTIONS    = "Questions"; 
	
	
	private void addTrendGraphs()
	{
		String[] labels_cl_recv_sent = new String[] { "KB Received [Bytes_received/1024]", "KB Sent [Bytes_sent/1024]" };
		String[] labels_questions    = new String[] { "Client Statements [Questions]", "Client & Internal Statements [Queries]"} ;
				
		addTrendGraphData(GRAPH_NAME_KB_RECV_SENT, new TrendGraphDataPoint(GRAPH_NAME_KB_RECV_SENT, labels_cl_recv_sent, LabelType.Static));
		addTrendGraphData(GRAPH_NAME_QUESTIONS,    new TrendGraphDataPoint(GRAPH_NAME_QUESTIONS,    labels_questions,    LabelType.Static));

		// if GUI
		if (getGuiController() != null && getGuiController().hasGUI())
		{
			// GRAPH
			TrendGraph tg = null;
			tg = new TrendGraph(GRAPH_NAME_KB_RECV_SENT,
				"Connections, KBytes Received/Sent per sec", // Menu CheckBox text
				"Connections, KBytes Received/Sent per sec ("+GROUP_NAME+"->"+SHORT_NAME+")", // Label 
				labels_cl_recv_sent, 
				false, // is Percent Graph
				this, 
				true, // visible at start
				0,     // graph is valid from Server Version. 0 = All Versions; >0 = Valid from this version and above 
				-1);   // minimum height
			addTrendGraph(tg.getName(), tg, true);
			
			tg = new TrendGraph(GRAPH_NAME_QUESTIONS,
				"Number of Statements per sec", // Menu CheckBox text
				"Number of Statements per sec ("+GROUP_NAME+"->"+SHORT_NAME+")", // Label 
				labels_questions, 
				false, // is Percent Graph
				this, 
				true, // visible at start
				0,     // graph is valid from Server Version. 0 = All Versions; >0 = Valid from this version and above 
				-1);   // minimum height
			addTrendGraph(tg.getName(), tg, true);
		}
	}
	
	@Override
	public void updateGraphData(TrendGraphDataPoint tgdp)
	{
		if (GRAPH_NAME_KB_RECV_SENT.equals(tgdp.getName()))
		{	
			Double[] arr = new Double[2];

			arr[0] = this.getRateValueAsDouble("Bytes_received", "VARIABLE_VALUE") / 1024.0;
			arr[1] = this.getRateValueAsDouble("Bytes_sent",     "VARIABLE_VALUE") / 1024.0;

			// Set the values
			tgdp.setDataPoint(this.getTimestamp(), arr);
		}
		
			
		if (GRAPH_NAME_QUESTIONS.equals(tgdp.getName()))
		{	
			Double[] arr = new Double[2];

			arr[0] = this.getRateValueAsDouble("Questions", "VARIABLE_VALUE");
			arr[1] = this.getRateValueAsDouble("Queries",   "VARIABLE_VALUE");

			// Set the values
			tgdp.setDataPoint(this.getTimestamp(), arr);
		}
	}
	

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
			mtd.addTable("global_status",  "Global status.");

			mtd.addColumn("global_status", "VARIABLE_NAME",  "<html>Name of the variable.</html>");
			mtd.addColumn("global_status", "VARIABLE_VALUE", "<html>Value of the variable.</html>");
		}
		catch (NameNotFoundException e) {/*ignore*/}
	}

	@Override
	public List<String> getPkForVersion(Connection conn, int srvVersion, boolean isClusterEnabled)
	{
		List <String> pkCols = new LinkedList<String>();

		pkCols.add("VARIABLE_NAME");

		return pkCols;
	}

	@Override
	public String getSqlForVersion(Connection conn, int aseVersion, boolean isClusterEnabled)
	{
		String sql = 
			"select VARIABLE_NAME,cast(VARIABLE_VALUE as signed integer) as VARIABLE_VALUE \n" +
			"from performance_schema.global_status \n" +
			"where VARIABLE_VALUE REGEXP '^[[:digit:]]+$' \n";

		return sql;
	}

	@Override
	public String getToolTipTextOnTableCell(MouseEvent e, String colName, Object cellValue, int modelRow, int modelCol) 
	{
		if ("VARIABLE_NAME".equals(colName))
		{
			return cellValue == null ? null : MySqlVariablesDictionary.getInstance().getDescriptionHtml(cellValue.toString());
		}

		if ("VARIABLE_VALUE".equals(colName) )
		{
			int pos_key = findColumn("VARIABLE_NAME");
			if (pos_key >= 0)
			{
				Object cellVal = getValueAt(modelRow, pos_key);
				if (cellVal instanceof String)
				{
					return MySqlVariablesDictionary.getInstance().getDescriptionHtml((String) cellVal);
				}
			}
		}
		
		return super.getToolTipTextOnTableCell(e, colName, cellValue, modelRow, modelCol);
	}
}
