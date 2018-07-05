package com.asetune.cm.ase;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.math.BigDecimal;
import java.sql.Connection;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import javax.naming.NameNotFoundException;
import javax.swing.JComponent;
import javax.swing.JMenuItem;

import org.apache.log4j.Logger;

import com.asetune.ICounterController;
import com.asetune.IGuiController;
import com.asetune.cm.CmSettingsHelper;
import com.asetune.cm.CounterSample;
import com.asetune.cm.CounterSetTemplates;
import com.asetune.cm.CounterSetTemplates.Type;
import com.asetune.cm.CountersModel;
import com.asetune.cm.ase.gui.CmSpidWaitPanel;
import com.asetune.cm.ase.helper.WaitCounterSummary;
import com.asetune.cm.ase.helper.WaitCounterSummary.WaitCounterEntry;
import com.asetune.config.dict.MonTablesDictionary;
import com.asetune.config.dict.MonTablesDictionaryManager;
import com.asetune.graph.TrendGraphDataPoint;
import com.asetune.graph.TrendGraphDataPoint.LabelType;
import com.asetune.gui.MainFrame;
import com.asetune.gui.TabularCntrPanel;
import com.asetune.gui.TrendGraph;
import com.asetune.utils.Configuration;
import com.asetune.utils.StringUtil;
import com.asetune.utils.Ver;

/**
 * @author Goran Schwarz (goran_schwarz@hotmail.com)
 */
public class CmSpidWait
extends CountersModel
{
	private static Logger        _logger          = Logger.getLogger(CmSpidWait.class);
	private static final long    serialVersionUID = 1L;

	public static final String   CM_NAME          = CmSpidWait.class.getSimpleName();
	public static final String   SHORT_NAME       = "SPID Wait";
	public static final String   HTML_DESC        = 
		"<html>" +
		"What different resources are a Server SPID waiting for.<br>" +
		"<br>" +
		"<br>Note</b>: This is in experimental mode, it might take to much resources<br>" +
		"</html>";

	public static final String   GROUP_NAME       = MainFrame.TCP_GROUP_SERVER;
	public static final String   GUI_ICON_FILE    = "images/"+CM_NAME+".png";

	public static final int      NEED_SRV_VERSION = 0;
	public static final int      NEED_CE_VERSION  = 0;

	public static final String[] MON_TABLES       = new String[] {"monProcessWaits", "monWaitEventInfo", "monWaitClassInfo"};
	public static final String[] NEED_ROLES       = new String[] {"mon_role"};
	public static final String[] NEED_CONFIG      = new String[] {"enable monitoring=1", "process wait events=1"};

	public static final String[] PCT_COLUMNS      = new String[] {};
	public static final String[] DIFF_COLUMNS     = new String[] {"WaitTime", "Waits"};

	public static final boolean  NEGATIVE_DIFF_COUNTERS_TO_ZERO = true;
	public static final boolean  IS_SYSTEM_CM                   = true;
	public static final int      DEFAULT_POSTPONE_TIME          = 0;
	public static final int      DEFAULT_QUERY_TIMEOUT          = CountersModel.DEFAULT_sqlQueryTimeout;

	@Override public int     getDefaultPostponeTime()                 { return DEFAULT_POSTPONE_TIME; }
	@Override public int     getDefaultQueryTimeout()                 { return DEFAULT_QUERY_TIMEOUT; }
	@Override public boolean getDefaultIsNegativeDiffCountersToZero() { return NEGATIVE_DIFF_COUNTERS_TO_ZERO; }
	@Override public Type    getTemplateLevel()                       { return Type.LARGE; }

	/**
	 * FACTORY  method to create the object
	 */
	public static CountersModel create(ICounterController counterController, IGuiController guiController)
	{
		if (guiController != null && guiController.hasGUI())
			guiController.splashWindowProgress("Loading: Counter Model '"+CM_NAME+"'");

		return new CmSpidWait(counterController, guiController);
	}

	public CmSpidWait(ICounterController counterController, IGuiController guiController)
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
		
		// The flowing columns is part of difference calculation
		// But Disregarded in the filter "Do NOT show unchanged counter rows"
		// this means that even if they HAVE a value, the will be filtered OUT from the JTable
		setDiffDissColumns( new String[] {"WaitTime"} );

		addTrendGraphs();
		
		CounterSetTemplates.register(this);
	}


	//------------------------------------------------------------
	// Implementation
	//------------------------------------------------------------
	private static final String  PROP_PREFIX                         = CM_NAME;

	public static final String  PROPKEY_sample_extraWhereClause      = PROP_PREFIX + ".sample.extraWhereClause";
	public static final String  DEFAULT_sample_extraWhereClause      = "";

	public static final String  PROPKEY_trendGraph_skipWaitIdList    = PROP_PREFIX + ".trendGraph.skipWaitIdList";
	public static final String  DEFAULT_trendGraph_skipWaitIdList    = "250, 260";

	public static final String  PROPKEY_trendGraph_skipWaitClassList = PROP_PREFIX + ".trendGraph.skipWaitClassList";
	public static final String  DEFAULT_trendGraph_skipWaitClassList = "";

	public static final String  PROPKEY_trendGraph_skipUserNameList  = PROP_PREFIX + ".trendGraph.skipUserNameList";
	public static final String  DEFAULT_trendGraph_skipUserNameList  = "";

	public static final String  PROPKEY_trendGraph_skipSystemThreads = PROP_PREFIX + ".trendGraph.skipSystemThreads";
	public static final boolean DEFAULT_trendGraph_skipSystemThreads = true;

	public static final String PROPKEY_trendGraph_dataSource         = PROP_PREFIX + ".trendGraph.dataSource";
	public static final String DEFAULT_trendGraph_dataSource         = WaitCounterSummary.Type.WaitTimePerWait.toString();

	@Override
	protected void registerDefaultValues()
	{
		super.registerDefaultValues();

		Configuration.registerDefaultValue(PROPKEY_sample_extraWhereClause,      DEFAULT_sample_extraWhereClause);
		Configuration.registerDefaultValue(PROPKEY_trendGraph_skipWaitIdList,    DEFAULT_trendGraph_skipWaitIdList);
		Configuration.registerDefaultValue(PROPKEY_trendGraph_skipWaitClassList, DEFAULT_trendGraph_skipWaitClassList);
		Configuration.registerDefaultValue(PROPKEY_trendGraph_skipUserNameList,  DEFAULT_trendGraph_skipUserNameList);
		Configuration.registerDefaultValue(PROPKEY_trendGraph_skipSystemThreads, DEFAULT_trendGraph_skipSystemThreads);
		Configuration.registerDefaultValue(PROPKEY_trendGraph_dataSource,        DEFAULT_trendGraph_dataSource);
	}

	@Override
	protected TabularCntrPanel createGui()
	{
		return new CmSpidWaitPanel(this);
	}

	@Override
	public String[] getDependsOnConfigForVersion(Connection conn, int srvVersion, boolean isClusterEnabled)
	{
		return NEED_CONFIG;
	}

	@Override
	public void addMonTableDictForVersion(Connection conn, int aseVersion, boolean isClusterEnabled)
	{
		try 
		{
			MonTablesDictionary mtd = MonTablesDictionaryManager.getInstance();
			mtd.addColumn("monProcessWaits", "WaitTimePerWait", 
				"<html>" +
					"Wait time in seconds per wait.<br>" +
					"Since WaitTime here is in seconds, this value will also be in seconds.<br>" +
					"<br>" +
					"<b>Formula</b>: diff.WaitTime / diff.Waits<br>" +
				"</html>");
			mtd.addColumn("monProcessWaits", "UserName", 
					"<html>" +
						"Active User Name.<br>" +
						"<br>" +
						"<b>Formula</b>: suser_name(ServerUserID)<br>" +
					"</html>");
			mtd.addColumn("monProcessWaits", "OrigUserName", 
					"<html>" +
						"Original Server User Identifier. This is the Server User Identifier before setting proxy.<br>" +
						"<br>" +
						"<b>Formula</b>: suser_name(OrigServerUserID)<br>" +
					"</html>");
		}
		catch (NameNotFoundException e) {/*ignore*/}
	}

	@Override
	public List<String> getPkForVersion(Connection conn, int srvVersion, boolean isClusterEnabled)
	{
		List <String> pkCols = new LinkedList<String>();

		if (isClusterEnabled)
			pkCols.add("InstanceID");

		pkCols.add("SPID");
		pkCols.add("KPID");
		pkCols.add("WaitEventID");

		return pkCols;
	}

	@Override
	public String getSqlForVersion(Connection conn, int aseVersion, boolean isClusterEnabled)
	{
		Configuration conf = Configuration.getCombinedConfiguration();
		String  sample_extraWhereClause   = conf.getProperty       (PROPKEY_sample_extraWhereClause, DEFAULT_sample_extraWhereClause);

		// Do we have extra where clauses
		String sql_sample_extraWhereClause = "  -- Extra where clauses will go here. (it will look like: AND the_extra_where_clause) \n";
		if ( ! StringUtil.isNullOrBlank(sample_extraWhereClause) )
			sql_sample_extraWhereClause = "  and " + sample_extraWhereClause + "\n";

		
		String cols = "";

		String InstanceID   = ""; // in cluster
		String UserName     = ""; // in 15.0.2 esd#5
		String OrigUserName = ""; // in 16.0 SP2

		if (isClusterEnabled)
			InstanceID = "W.InstanceID, ";

		if (aseVersion >= Ver.ver(15,0,2,3))
			UserName = "UserName = suser_name(W.ServerUserID), ";

		if (aseVersion >= Ver.ver(16,0,0, 2))
			OrigUserName = "OrigUserName = isnull(suser_name(W.OrigServerUserID), suser_name(W.ServerUserID)), ";

		cols = InstanceID + "W.SPID, W.KPID, " + UserName + OrigUserName + "\n" +
			"WaitClassDesc = convert(varchar(80),''), -- runtime replaced with cached values from monWaitClassInfo \n" +
			"WaitEventDesc = convert(varchar(80),''), -- runtime replaced with cached values from monWaitEventInfo \n" +
			"W.WaitEventID, W.WaitTime, W.Waits, \n" +
			"WaitTimePerWait = CASE WHEN W.Waits > 0 \n" +
			"                       THEN convert(numeric(15,3), (W.WaitTime + 0.0) / W.Waits) \n" +
			"                       ELSE convert(numeric(15,3), 0.0) \n" +
			"                  END \n";

		String sql = 
			"select " + cols +
			"from master..monProcessWaits W \n" +
			"where 1 = 1 \n" +
			sql_sample_extraWhereClause +
			"order by " + (isClusterEnabled ? "W.SPID, W.WaitEventID, W.InstanceID" : "W.SPID, W.WaitEventID") + "\n" +
			"";

		return sql;
	}

	/** Used by the: Create 'Offline Session' Wizard */
	@Override
	public List<CmSettingsHelper> getLocalSettings()
	{
		Configuration conf = Configuration.getCombinedConfiguration();
		List<CmSettingsHelper> list = new ArrayList<>();
		
		list.add(new CmSettingsHelper("Extra Where Caluse",     PROPKEY_sample_extraWhereClause      , String.class,  conf.getProperty       (PROPKEY_sample_extraWhereClause      , DEFAULT_sample_extraWhereClause      ), DEFAULT_sample_extraWhereClause     , CmSpidWaitPanel.TOOLTIP_sample_extraWhereClause                     ));
		list.add(new CmSettingsHelper("Skip Wait ID List",      PROPKEY_trendGraph_skipWaitIdList    , String.class,  conf.getProperty       (PROPKEY_trendGraph_skipWaitIdList    , DEFAULT_trendGraph_skipWaitIdList    ), DEFAULT_trendGraph_skipWaitIdList   , "Skip specific WaitEventID's from beeing in ThrendGraph"            ));
		list.add(new CmSettingsHelper("Skip Wait Class List",   PROPKEY_trendGraph_skipWaitClassList , String.class,  conf.getProperty       (PROPKEY_trendGraph_skipWaitClassList , DEFAULT_trendGraph_skipWaitClassList ), DEFAULT_trendGraph_skipWaitClassList, "Skip specific Event Clases from beeing in ThrendGraph"             ));
		list.add(new CmSettingsHelper("Skip Username List",     PROPKEY_trendGraph_skipUserNameList  , String.class,  conf.getProperty       (PROPKEY_trendGraph_skipUserNameList  , DEFAULT_trendGraph_skipUserNameList  ), DEFAULT_trendGraph_skipUserNameList , "Skip specific users from beeing in ThrendGraph"                    ));
		list.add(new CmSettingsHelper("Skip System Processes",  PROPKEY_trendGraph_skipSystemThreads , Boolean.class, conf.getBooleanProperty(PROPKEY_trendGraph_skipSystemThreads , DEFAULT_trendGraph_skipSystemThreads ), DEFAULT_trendGraph_skipSystemThreads, "Skip System SPID's from beeing in ThrendGraph"                     ));
		list.add(new CmSettingsHelper("Trend Graph Datasource", PROPKEY_trendGraph_dataSource        , String.class,  conf.getProperty       (PROPKEY_trendGraph_dataSource        , DEFAULT_trendGraph_dataSource        ), DEFAULT_trendGraph_dataSource       , "What column should be the source WaitTime, Waits, WaitTimePerWait" ));

		return list;
	}


	/** 
	 * Fill in the WaitEventDesc column with data from
	 * MonTableDictionary.. transforms a WaitEventId -> text description
	 * This so we do not have to do a sub select in the query that gets data
	 * doing it this way, means better performance, since the values are cached locally in memory
	 */
	@Override
	public void localCalculation(CounterSample newSample)
	{
		// Where are various columns located in the Vector 
		int pos_WaitEventID = -1, pos_WaitEventDesc = -1, pos_WaitClassDesc = -1;
		int waitEventID = 0;
		String waitEventDesc = "";
		String waitClassDesc = "";
	
		MonTablesDictionary mtd = MonTablesDictionaryManager.getInstance();
		if (mtd == null)
			return;

		if (newSample == null)
			return;

		// Find column Id's
		List<String> colNames = newSample.getColNames();
		if (colNames==null) 
			return;

		for (int colId=0; colId < colNames.size(); colId++) 
		{
			String colName = colNames.get(colId);
			if      (colName.equals("WaitEventID"))   pos_WaitEventID   = colId;
			else if (colName.equals("WaitEventDesc")) pos_WaitEventDesc = colId;
			else if (colName.equals("WaitClassDesc")) pos_WaitClassDesc = colId;

			// Noo need to continue, we got all our columns
			if (pos_WaitEventID >= 0 && pos_WaitEventDesc >= 0 && pos_WaitClassDesc >= 0)
				break;
		}

		if (pos_WaitEventID < 0 || pos_WaitEventDesc < 0 || pos_WaitClassDesc < 0)
		{
			_logger.debug("Can't find the position for columns ('WaitEventID'="+pos_WaitEventID+", 'WaitEventDesc'="+pos_WaitEventDesc+", 'WaitClassDesc'="+pos_WaitClassDesc+")");
			return;
		}
		
		// Loop on all counter rows
		for (int rowId=0; rowId < newSample.getRowCount(); rowId++) 
		{
			Object o_waitEventId  = newSample.getValueAt(rowId, pos_WaitEventID);

			if (o_waitEventId instanceof Number)
			{
				waitEventID = ((Number)o_waitEventId).intValue();

				if (mtd.hasWaitEventDescription(waitEventID))
				{
					waitEventDesc = mtd.getWaitEventDescription(waitEventID);
					waitClassDesc = mtd.getWaitEventClassDescription(waitEventID);
				}
				else
				{
					waitEventDesc = "";
					waitClassDesc = "";
				}

				newSample.setValueAt(waitEventDesc, rowId, pos_WaitEventDesc);
				newSample.setValueAt(waitClassDesc, rowId, pos_WaitClassDesc);
			}
		}
	}

	/** 
	 * Compute the WaitTimePerWait for diff values
	 */
	@Override
	public void localCalculation(CounterSample prevSample, CounterSample newSample, CounterSample diffData)
	{
		int WaitTime,        Waits;
		int WaitTimeId = -1, WaitsId = -1;

		double calcWaitTimePerWait;
		int WaitTimePerWaitId = -1;

		// Find column Id's
		List<String> colNames = diffData.getColNames();
		if (colNames == null)
			return;

		for (int colId=0; colId < colNames.size(); colId++) 
		{
			String colName = colNames.get(colId);
			if      (colName.equals("WaitTimePerWait")) WaitTimePerWaitId = colId;
			else if (colName.equals("WaitTime"))        WaitTimeId        = colId;
			else if (colName.equals("Waits"))           WaitsId           = colId;
		}

		// Loop on all diffData rows
		for (int rowId = 0; rowId < diffData.getRowCount(); rowId++)
		{
			WaitTime = ((Number)diffData.getValueAt(rowId, WaitTimeId)).intValue();
			Waits    = ((Number)diffData.getValueAt(rowId, WaitsId   )).intValue();

			// int totIo = Reads + APFReads + Writes;
			if (Waits > 0)
			{
				// WaitTimePerWait = WaitTime / Waits;
				calcWaitTimePerWait = WaitTime / (Waits * 1.0);

				BigDecimal newVal = new BigDecimal(calcWaitTimePerWait).setScale(3, BigDecimal.ROUND_HALF_EVEN);;
				diffData.setValueAt(newVal, rowId, WaitTimePerWaitId);
			}
			else
				diffData.setValueAt(new BigDecimal(0), rowId, WaitTimePerWaitId);
		}
	}


	
	
	//---------------------------------------------------------------------------------
	//---------------------------------------------------------------------------------
	// Graph stuff
	//---------------------------------------------------------------------------------
	//---------------------------------------------------------------------------------
	
	public static final String   GRAPH_NAME_EVENT_NAME = "spidWaitName"; 
	public static final String   GRAPH_NAME_CLASS_NAME = "spidClassName"; 
	
	// Keep labels in the same order
//	private Map<Integer, Integer> _labelOrder_eventId         = new LinkedHashMap<Integer, Integer>();
//	private Map<String,  Integer> _labelOrder_className       = new LinkedHashMap<String,  Integer>();
//	private Map<Integer, String>  _labelOrder_aPosToEventName = new LinkedHashMap<Integer, String>();
//	private Map<Integer, String>  _labelOrder_aPosToClassName = new LinkedHashMap<Integer, String>();

	private void addTrendGraphs()
	{
////		String[] labels = new String[] { "runtime-replaced" };
//		String[] labels = TrendGraphDataPoint.RUNTIME_REPLACED_LABELS;
//		
//		addTrendGraphData(GRAPH_NAME_EVENT_NAME, new TrendGraphDataPoint(GRAPH_NAME_EVENT_NAME, labels, LabelType.Dynamic));
//		addTrendGraphData(GRAPH_NAME_CLASS_NAME, new TrendGraphDataPoint(GRAPH_NAME_CLASS_NAME, labels, LabelType.Dynamic));

		// GET CONFIG: Graph Data Source
		// In GUI mode we can change the type, but in NO-GUI mode it will be static to what is configured AT-START
		WaitCounterSummary.Type type = WaitCounterSummary.Type.WaitTimePerWait;
		String dataSourceStr = Configuration.getCombinedConfiguration().getProperty(PROPKEY_trendGraph_dataSource, DEFAULT_trendGraph_dataSource);
		try { type = WaitCounterSummary.Type.valueOf(dataSourceStr); }
		catch (Throwable t) { _logger.warn("CM='"+getName()+"', Problems converting '"+dataSourceStr+"' to WaitCounterSummary.Type. Using '"+type+"' instead."); }
		
		addTrendGraph(GRAPH_NAME_EVENT_NAME,
			"SPID Wait, group by EventID, "+type+" Average", 	                   // Menu CheckBox text
			"SPID Wait, group by EventID, "+type+" Average ("+GROUP_NAME+"->"+SHORT_NAME+")", // Label 
			null, 
			LabelType.Dynamic,
			TrendGraphDataPoint.Category.WAITS,
			false, // is Percent Graph
			false, // visible at start
			0,    // graph is valid from Server Version. 0 = All Versions; >0 = Valid from this version and above 
			160);  // minimum height

		// GRAPH
		addTrendGraph(GRAPH_NAME_CLASS_NAME,
			"SPID Wait, group by ClassName, "+type+" Average", 	                     // Menu CheckBox text
			"SPID Wait, group by ClassName, "+type+" Average ("+GROUP_NAME+"->"+SHORT_NAME+")", // Label 
			null, 
			LabelType.Dynamic,
			TrendGraphDataPoint.Category.WAITS,
			false, // is Percent Graph
			false, // visible at start
			0,    // graph is valid from Server Version. 0 = All Versions; >0 = Valid from this version and above 
			-1);  // minimum height

//		// if GUI
//		if (getGuiController() != null && getGuiController().hasGUI())
//		{
//			// GRAPH
//			TrendGraph tg = null;
//			tg = new WaitTrendGraph(GRAPH_NAME_EVENT_NAME,
//					"SPID Wait, group by EventID, WaitTimePerWait Average", 	                   // Menu CheckBox text
//					"SPID Wait, group by EventID, WaitTimePerWait Average ("+GROUP_NAME+"->"+SHORT_NAME+")", // Label 
//					labels, 
//					false, // is Percent Graph
//					this, 
//					false, // visible at start
//					0,    // graph is valid from Server Version. 0 = All Versions; >0 = Valid from this version and above 
//					160);  // minimum height
//			addTrendGraph(tg.getName(), tg, true);
//
//			// GRAPH
//			tg = new WaitTrendGraph(GRAPH_NAME_CLASS_NAME,
//					"SPID Wait, group by ClassName, WaitTimePerWait Average", 	                     // Menu CheckBox text
//					"SPID Wait, group by ClassName, WaitTimePerWait Average ("+GROUP_NAME+"->"+SHORT_NAME+")", // Label 
//					labels, 
//					false, // is Percent Graph
//					this, 
//					false, // visible at start
//					0,    // graph is valid from Server Version. 0 = All Versions; >0 = Valid from this version and above 
//					-1);  // minimum height
//			addTrendGraph(tg.getName(), tg, true);
//		}
	}

	/**
	 * Add some menu items to the right click menu
	 */
	@Override
	public List<JComponent> createGraphSpecificMenuItems()
	{
		ArrayList<JComponent> list = new ArrayList<JComponent>();
		
		//------------------------------------------------------------
		JMenuItem  mi = new JMenuItem("Edit Skip WaitEvents...");
		list.add(mi);
		mi.addActionListener(new ActionListener()
		{
			@Override
			public void actionPerformed(ActionEvent e)
			{
				CmSpidWaitPanel.openPropertiesEditor();
			}
		});
		
		//------------------------------------------------------------
		mi = new JMenuItem("Change DataSource/Column...");
		list.add(mi);
		mi.addActionListener(new ActionListener()
		{
			@Override
			public void actionPerformed(ActionEvent e)
			{
				String before = Configuration.getCombinedConfiguration().getProperty(PROPKEY_trendGraph_dataSource, DEFAULT_trendGraph_dataSource);

				WaitCounterSummary.openDataSourceDialog(PROPKEY_trendGraph_dataSource, DEFAULT_trendGraph_dataSource);

				String after = Configuration.getCombinedConfiguration().getProperty(PROPKEY_trendGraph_dataSource, DEFAULT_trendGraph_dataSource);
				if ( ! before.equals(after) )
				{
					for (TrendGraph tg : getTrendGraphs().values())
						tg.resetGraph();
				}
			}
		});
		
		return list;
	}

//	/**
//	 * Local class which extends TrendGraph so we can create an extended menu set...
//	 */
//	private class WaitTrendGraph
//	extends TrendGraph
//	{
//		public WaitTrendGraph(String name, String chkboxText, String label, String[] seriesNames, boolean pct, CountersModel cm, boolean initialVisible, int validFromVersion, int panelMinHeight)
//		{
//			super(name, chkboxText, label, seriesNames, pct, cm, initialVisible, validFromVersion, panelMinHeight);
//		}
//
//		@Override
//		public List<JComponent> createGraphSpecificMenuItems()
//		{
//			ArrayList<JComponent> list = new ArrayList<JComponent>();
//			
//			//------------------------------------------------------------
//			JMenuItem  mi = new JMenuItem("Edit Skip WaitEvents...");
//			list.add(mi);
//			mi.addActionListener(new ActionListener()
//			{
//				@Override
//				public void actionPerformed(ActionEvent e)
//				{
//					CmSpidWaitPanel.openPropertiesEditor();
//				}
//			});
//			
//			//------------------------------------------------------------
//			mi = new JMenuItem("Change DataSource/Column...");
//			list.add(mi);
//			mi.addActionListener(new ActionListener()
//			{
//				@Override
//				public void actionPerformed(ActionEvent e)
//				{
//					String before = Configuration.getCombinedConfiguration().getProperty(PROPKEY_trendGraph_dataSource, DEFAULT_trendGraph_dataSource);
//
//					WaitCounterSummary.openDataSourceDialog(PROPKEY_trendGraph_dataSource, DEFAULT_trendGraph_dataSource);
//
//					String after = Configuration.getCombinedConfiguration().getProperty(PROPKEY_trendGraph_dataSource, DEFAULT_trendGraph_dataSource);
//					if ( ! before.equals(after) )
//					{
//						for (TrendGraph tg : getTrendGraphs().values())
//							tg.resetGraph();
//					}
//				}
//			});
//			
//			return list;
//		}
//		
////		@Override
////		public void resetGraph()
////		{
////			getTrendGraphData(getName()).clear();
////
//////			if (GRAPH_NAME_EVENT_NAME.equals(getName()))
//////			{
//////				_labelOrder_eventId         = new LinkedHashMap<Integer, Integer>();
//////				_labelOrder_aPosToEventName = new LinkedHashMap<Integer, String>();
//////			}
//////
//////			if (GRAPH_NAME_CLASS_NAME.equals(getName()))
//////			{
//////				_labelOrder_className       = new LinkedHashMap<String,  Integer>();
//////				_labelOrder_aPosToClassName = new LinkedHashMap<Integer, String>();
//////			}
////			super.resetGraph();
////		}
//	}

	/** 
	 * Main method to calculate graph data for ALL GRAPHS (you need to loop all the graphs yourself)
	 * Overriding this method instead of: <code>public void updateGraphData(TrendGraphDataPoint tgdp)</code>
	 * Because we don't want to do the *basic*calculation* twice, basic calculation is shared between both graphs.
	 */
	@Override
	public void updateGraphData()
	{
		if ( ! hasRateData() )
			return;

		Map<String, TrendGraphDataPoint> trendGraphsData = getTrendGraphData();
		if (trendGraphsData.size() == 0)
			return;

		// Calculate RAW data for BOTH GRAPHS
//System.out.println("");
//System.out.println("");
//System.out.println("");
//System.out.println("##################################################################################");
//System.out.println("##################################################################################");
//System.out.println("##################################################################################");

		Configuration conf = Configuration.getCombinedConfiguration();

		// GET CONFIG: skipEventIdList
		List<Integer> skipEventIdList = new ArrayList<Integer>();
//		skipEventIdList.add(250); // SKIP Wait EventId 250 -- waiting for incoming network data
//		skipEventIdList.add(260); // SKIP Wait EventId 260 -- waiting for date or time in waitfor command
		String skipEventIdListStr = conf.getProperty(PROPKEY_trendGraph_skipWaitIdList, DEFAULT_trendGraph_skipWaitIdList);
		for (String str : StringUtil.commaStrToSet(skipEventIdListStr))
		{
			try { skipEventIdList.add(Integer.parseInt(str)); }
			catch (NumberFormatException nfe) {_logger.info("CmName='"+getName()+"' updateGraphData(): when reading property '"+PROPKEY_trendGraph_skipWaitIdList+"' found a non number('"+str+"') in the list of WaitEventID's to skip. This specific 'ID' will not be added to the list. The full input-list, which has problems looks like '"+skipEventIdListStr+"'.");}
		}
		
		// GET CONFIG: skipEventClassList
		String skipEventClassListStr = conf.getProperty(PROPKEY_trendGraph_skipWaitClassList, DEFAULT_trendGraph_skipWaitClassList);
		List<String> skipEventClassList = StringUtil.commaStrToList(skipEventClassListStr);

		// GET CONFIG: skipUserNameList
		String skipUserNameListStr = conf.getProperty(PROPKEY_trendGraph_skipUserNameList, DEFAULT_trendGraph_skipUserNameList);
		List<String> skipUserNameList = StringUtil.commaStrToList(skipUserNameListStr);

		// GET CONFIG: skipSystemThreads
		boolean skipSystemThreads = conf.getBooleanProperty(PROPKEY_trendGraph_skipSystemThreads, DEFAULT_trendGraph_skipSystemThreads);

		// GET CONFIG: Graph Data Source
		WaitCounterSummary.Type type = WaitCounterSummary.Type.WaitTimePerWait; 
		String dataSourceStr = conf.getProperty(PROPKEY_trendGraph_dataSource, DEFAULT_trendGraph_dataSource);
		try { type = WaitCounterSummary.Type.valueOf(dataSourceStr); }
		catch (Throwable t) { _logger.warn("CM='"+getName()+"', Problems converting '"+dataSourceStr+"' to WaitCounterSummary.Type. Using '"+type+"' instead."); }

		// Create the Summary object
		WaitCounterSummary wcs = WaitCounterSummary.create(this, skipEventIdList, skipEventClassList, skipUserNameList, skipSystemThreads);
//if (wcs != null)
//	wcs.debugPrint();

		// loop the graphs
		for (TrendGraphDataPoint tgdp : trendGraphsData.values()) 
		{
//System.out.println("GRAPH_NAME='"+tgdp.getName()+"'.");
			if (GRAPH_NAME_EVENT_NAME.equals(tgdp.getName()))
			{
				// Set/change the Label....
				TrendGraph tg = getTrendGraph(tgdp.getName());
				if (tg != null)
					tg.setChartLabel("SPID Wait, group by EventID, "+type+" Average ("+GROUP_NAME+"->"+SHORT_NAME+")");

				LinkedHashMap<String, Double> dataMap = new LinkedHashMap<String, Double>();
				for (WaitCounterEntry wce : wcs.getEventIdMap().values())
				{
					// Get the type of data we are going to present
					double dataValue = 0.0;
					if      (type == WaitCounterSummary.Type.WaitTime       ) dataValue = wce.getAvgWaitTime();
					else if (type == WaitCounterSummary.Type.Waits          ) dataValue = wce.getAvgWaits();
					else if (type == WaitCounterSummary.Type.WaitTimePerWait) dataValue = wce.getAvgWaitTimePerWait();

					// set data & label in the array
					dataMap.put(wce.getEventNameLabel(), dataValue);
				}
				if (dataMap.size() > 0)
					tgdp.setData(this.getTimestamp(), dataMap);


//				// data & label array size = WaitCounterSummary.size or PRIVIOUS max size
//				Double[] dArr = new Double[Math.max(wcs.getEventIdSize(), _labelOrder_eventId.size())];
//				String[] lArr = new String[dArr.length];
//
////System.out.println("dArr & lArr lenth = " + dArr.length);
//				for (WaitCounterEntry wce : wcs.getEventIdMap().values())
//				{
//					// aLoc = get arrayPosition for a specific _WaitEventID
//					// We want to have them in same order...
//					Integer aPos = _labelOrder_eventId.get(wce.getWaitEventID());
//					if (aPos == null)
//					{
//						aPos = new Integer(_labelOrder_eventId.size());
//						_labelOrder_eventId.put(wce.getWaitEventID(), aPos);
//						_labelOrder_aPosToEventName.put(aPos, wce.getEventNameLabel());
//						if (aPos >= dArr.length)
//						{
//							Double[] new_dArr = new Double[aPos + 1];
//							String[] new_lArr = new String[new_dArr.length];
//							System.arraycopy(dArr, 0, new_dArr, 0, dArr.length);
//							System.arraycopy(lArr, 0, new_lArr, 0, lArr.length);
//							dArr = new_dArr;
//							lArr = new_lArr;
//						}
//					}
//					
//					// Get the type of data we are going to present
//					double dataValue = 0.0;
//					if      (type == WaitCounterSummary.Type.WaitTime       ) dataValue = wce.getAvgWaitTime();
//					else if (type == WaitCounterSummary.Type.Waits          ) dataValue = wce.getAvgWaits();
//					else if (type == WaitCounterSummary.Type.WaitTimePerWait) dataValue = wce.getAvgWaitTimePerWait();
//
//					// set data & label in the array
//					dArr[aPos] = dataValue;
//					lArr[aPos] = wce.getEventNameLabel();
//
//					if (_logger.isDebugEnabled())
//						_logger.debug("updateGraphData("+GRAPH_NAME_EVENT_NAME+"): aLoc="+aPos+", data="+dArr[aPos]+", label='"+lArr[aPos]+"'.");
////System.out.println("updateGraphData("+getName()+"."+GRAPH_NAME_EVENT_NAME+"): aLoc="+aPos+", data="+dArr[aPos]+", label='"+lArr[aPos]+"'.");
//				}
//
//				// Fill in empty/blank array entries
//				for (int i=0; i<lArr.length; i++)
//				{
//					if (lArr[i] == null)
//					{
//						dArr[i] = 0.0;
//						lArr[i] = _labelOrder_aPosToEventName.get(i);
//						if (lArr[i] == null)
//							lArr[i] = "-fixme-";
//					}
//				}
//
//				// Set the values
//				tgdp.setDate(this.getTimestamp());
//				tgdp.setLabel(lArr);
//				tgdp.setData (dArr);
			}

			if (GRAPH_NAME_CLASS_NAME.equals(tgdp.getName()))
			{
				// Set/change the Label....
				TrendGraph tg = getTrendGraph(tgdp.getName());
				if (tg != null)
					tg.setChartLabel("SPID Wait, group by ClassName, "+type+" Average  ("+GROUP_NAME+"->"+SHORT_NAME+")");

				LinkedHashMap<String, Double> dataMap = new LinkedHashMap<String, Double>();
				for (WaitCounterEntry wce : wcs.getClassNameMap().values())
				{
					// Get the type of data we are going to present
					double dataValue = 0.0;
					if      (type == WaitCounterSummary.Type.WaitTime       ) dataValue = wce.getAvgWaitTime();
					else if (type == WaitCounterSummary.Type.Waits          ) dataValue = wce.getAvgWaits();
					else if (type == WaitCounterSummary.Type.WaitTimePerWait) dataValue = wce.getAvgWaitTimePerWait();

					// set data & label in the array
					dataMap.put(wce.getClassName(), dataValue);
				}
				if (dataMap.size() > 0)
					tgdp.setData(this.getTimestamp(), dataMap);
				

//				// data & label array size = WaitCounterSummary.size or PRIVIOUS max size
//				Double[] dArr = new Double[Math.max(wcs.getClassNameSize(), _labelOrder_className.size())];
//				String[] lArr = new String[dArr.length];
//
////System.out.println("dArr & lArr lenth = " + dArr.length);
//				for (WaitCounterEntry wce : wcs.getClassNameMap().values())
//				{
//					// aLoc = get arrayPosition for a specific _WaitEventID
//					// We want to have them in same order...
//					Integer aPos = _labelOrder_className.get(wce.getClassName());
//					if (aPos == null)
//					{
//						aPos = new Integer(_labelOrder_className.size());
//						_labelOrder_className.put(wce.getClassName(), aPos);
//						_labelOrder_aPosToClassName.put(aPos, wce.getClassName());
//						if (aPos >= dArr.length)
//						{
//							Double[] new_dArr = new Double[aPos + 1];
//							String[] new_lArr = new String[new_dArr.length];
//							System.arraycopy(dArr, 0, new_dArr, 0, dArr.length);
//							System.arraycopy(lArr, 0, new_lArr, 0, lArr.length);
//							dArr = new_dArr;
//							lArr = new_lArr;
//						}
//					}
//					
//					dArr[aPos] = wce.getAvgWaitTimePerWait();
//					lArr[aPos] = wce.getClassName();
//
//					if (_logger.isDebugEnabled())
//						_logger.debug("updateGraphData("+GRAPH_NAME_CLASS_NAME+"): aLoc="+aPos+", data="+dArr[aPos]+", label='"+lArr[aPos]+"'.");
////System.out.println("updateGraphData("+getName()+"."+GRAPH_NAME_CLASS_NAME+"): aLoc="+aPos+", data="+dArr[aPos]+", label='"+lArr[aPos]+"'.");
//				}
//
//				// Fill in empty/blank array entries
//				for (int i=0; i<lArr.length; i++)
//				{
//					if (lArr[i] == null)
//					{
//						dArr[i] = 0.0;
//						lArr[i] = _labelOrder_aPosToClassName.get(i); 
//						if (lArr[i] == null)
//							lArr[i] = "-fixme-";
//					}
//				}
//
//				// Set the values
//				tgdp.setDate(this.getTimestamp());
//				tgdp.setLabel(lArr);
//				tgdp.setData (dArr);
			}
			
			if (_logger.isDebugEnabled())
				_logger.debug("cm='"+StringUtil.left(this.getName(),25)+"', trendGraphsData="+tgdp);
//System.out.println("cm='"+StringUtil.left(this.getName(),25)+"', trendGraphsData="+tgdp);
		}
	}

	@Override
	public void updateGraphData(TrendGraphDataPoint tgdp)
	{
	}
}
