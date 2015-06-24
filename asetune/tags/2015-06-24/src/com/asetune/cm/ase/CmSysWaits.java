package com.asetune.cm.ase;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.math.BigDecimal;
import java.sql.Connection;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import javax.naming.NameNotFoundException;
import javax.swing.JComponent;
import javax.swing.JMenuItem;

import org.apache.log4j.Logger;

import com.asetune.ICounterController;
import com.asetune.IGuiController;
import com.asetune.cm.CmSybMessageHandler;
import com.asetune.cm.CounterSample;
import com.asetune.cm.CounterSetTemplates;
import com.asetune.cm.CounterSetTemplates.Type;
import com.asetune.cm.CountersModel;
import com.asetune.cm.ase.gui.CmSysWaitsPanel;
import com.asetune.cm.ase.helper.WaitCounterSummary;
import com.asetune.cm.ase.helper.WaitCounterSummary.WaitCounterEntry;
import com.asetune.config.dict.MonTablesDictionary;
import com.asetune.config.dict.MonTablesDictionaryManager;
import com.asetune.graph.TrendGraphDataPoint;
import com.asetune.gui.MainFrame;
import com.asetune.gui.TabularCntrPanel;
import com.asetune.gui.TrendGraph;
import com.asetune.utils.Configuration;
import com.asetune.utils.StringUtil;
import com.asetune.utils.Ver;

/**
 * @author Goran Schwarz (goran_schwarz@hotmail.com)
 */
public class CmSysWaits
extends CountersModel
{
	private static Logger        _logger          = Logger.getLogger(CmSysWaits.class);
	private static final long    serialVersionUID = 1L;

	public static final String   CM_NAME          = CmSysWaits.class.getSimpleName();
	public static final String   SHORT_NAME       = "Waits";
	public static final String   HTML_DESC        = 
		"<html>" +
		"What different resources are the ASE Server waiting for. <br>" +
		"<br>" +
		"<b>Tip:</b> Hover over the WaitEventID, then you get a tooltip trying to describe the WaitEventID." +
		"</html>";

	public static final String   GROUP_NAME       = MainFrame.TCP_GROUP_SERVER;
	public static final String   GUI_ICON_FILE    = "images/"+CM_NAME+".png";

	public static final int      NEED_SRV_VERSION = 0;
	public static final int      NEED_CE_VERSION  = 0;

	public static final String[] MON_TABLES       = new String[] {"monSysWaits", "monWaitEventInfo", "monWaitClassInfo"};
	public static final String[] NEED_ROLES       = new String[] {"mon_role"};
	public static final String[] NEED_CONFIG      = new String[] {"enable monitoring=1", "wait event timing=1"};

	public static final String[] PCT_COLUMNS      = new String[] {};
	public static final String[] DIFF_COLUMNS     = new String[] {"WaitTime", "Waits"};

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

		return new CmSysWaits(counterController, guiController);
	}

	public CmSysWaits(ICounterController counterController, IGuiController guiController)
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
	private static final String  PROP_PREFIX                         = CM_NAME;
	
	public static final String PROPKEY_trendGraph_skipWaitIdList    = PROP_PREFIX + ".trendGraph.skipWaitIdList";
	public static final String DEFAULT_trendGraph_skipWaitIdList    = "250, 260";

	public static final String PROPKEY_trendGraph_skipWaitClassList = PROP_PREFIX + ".trendGraph.skipWaitClassList";
	public static final String DEFAULT_trendGraph_skipWaitClassList = "";

	public static final String PROPKEY_trendGraph_dataSource        = PROP_PREFIX + ".trendGraph.dataSource";
	public static final String DEFAULT_trendGraph_dataSource        = WaitCounterSummary.Type.WaitTimePerWait.toString();

	@Override
	protected void registerDefaultValues()
	{
		super.registerDefaultValues();

		Configuration.registerDefaultValue(PROPKEY_trendGraph_skipWaitIdList,    DEFAULT_trendGraph_skipWaitIdList);
		Configuration.registerDefaultValue(PROPKEY_trendGraph_skipWaitClassList, DEFAULT_trendGraph_skipWaitClassList);
		Configuration.registerDefaultValue(PROPKEY_trendGraph_dataSource,        DEFAULT_trendGraph_dataSource);
	}

	/** Used by the: Create 'Offline Session' Wizard */
	@Override
	public Configuration getLocalConfiguration()
	{
		Configuration conf = Configuration.getCombinedConfiguration();
		Configuration lc = new Configuration();

		lc.setProperty(PROPKEY_trendGraph_skipWaitIdList,    conf.getProperty(PROPKEY_trendGraph_skipWaitIdList,    DEFAULT_trendGraph_skipWaitIdList));
		lc.setProperty(PROPKEY_trendGraph_skipWaitClassList, conf.getProperty(PROPKEY_trendGraph_skipWaitClassList, DEFAULT_trendGraph_skipWaitClassList));
		lc.setProperty(PROPKEY_trendGraph_dataSource,        conf.getProperty(PROPKEY_trendGraph_dataSource,        DEFAULT_trendGraph_dataSource));

		return lc;
	}

	/** Used by the: Create 'Offline Session' Wizard */
	@Override
	public String getLocalConfigurationDescription(String propName)
	{
		if (propName.equals(PROPKEY_trendGraph_skipWaitIdList))    return "Skip specific WaitEventID's from beeing in ThrendGraph";
		if (propName.equals(PROPKEY_trendGraph_skipWaitClassList)) return "Skip specific Event Clases from beeing in ThrendGraph";
		if (propName.equals(PROPKEY_trendGraph_dataSource))        return "What column should be the source WaitTime, Waits, WaitTimePerWait";
		return "";
	}
	@Override
	public String getLocalConfigurationDataType(String propName)
	{
		if (propName.equals(PROPKEY_trendGraph_skipWaitIdList))    return String .class.getSimpleName();
		if (propName.equals(PROPKEY_trendGraph_skipWaitClassList)) return String .class.getSimpleName();
		if (propName.equals(PROPKEY_trendGraph_dataSource))        return String .class.getSimpleName();
		return "";
	}

	@Override
	protected TabularCntrPanel createGui()
	{
		return new CmSysWaitsPanel(this);
	}

	@Override
	protected CmSybMessageHandler createSybMessageHandler()
	{
		CmSybMessageHandler msgHandler = super.createSybMessageHandler();

		// If ASE is above 15.0.3 esd#1, and dbcc traceon(3604) is given && 'capture missing stats' is 
		// on the 'CMsysWaitActivity' CM will throw an warning which should NOT be throws...
		//if (getServerVersion() >= 15031) // NOTE this is done early in initialization, so getServerVersion() can't be used
		//if (getServerVersion() >= 1503010) // NOTE this is done early in initialization, so getServerVersion() can't be used
		//if (getServerVersion() >= Ver.ver(15,0,3,1)) // NOTE this is done early in initialization, so getServerVersion() can't be used
		msgHandler.addDiscardMsgStr("WaitClassID, WaitEventID");

		return msgHandler;
	}

	@Override
	public void addMonTableDictForVersion(Connection conn, int aseVersion, boolean isClusterEnabled)
	{
		try 
		{
			MonTablesDictionary mtd = MonTablesDictionaryManager.getInstance();
			mtd.addColumn("monSysWaits", "WaitTimePerWait", "<html>" +
			                                                   "Wait time in seconds per wait. formula: diff.WaitTime / diff.Waits<br>" +
			                                                   "Since WaitTime here is in seconds, this value will also be in seconds." +
			                                                "</html>");
		}
		catch (NameNotFoundException e) {/*ignore*/}
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

		if (isClusterEnabled)
			pkCols.add("InstanceID");

		pkCols.add("WaitEventID");

		return pkCols;
	}

	@Override
	public String getSqlForVersion(Connection conn, int aseVersion, boolean isClusterEnabled)
	{
		String cols1, cols2, cols3;
		cols1 = cols2 = cols3 = "";


		if (isClusterEnabled)
		{
			cols1 += "InstanceID, \n";
		}

		cols1 += "WaitClassDesc = convert(varchar(50),''), -- runtime replaced with cached values from monWaitClassInfo \n";
		cols1 += "WaitEventDesc = convert(varchar(50),''), -- runtime replaced with cached values from monWaitEventInfo \n";
		cols1 += "W.WaitEventID, WaitTime, Waits \n";
//		if (aseVersion >= 15010 || (aseVersion >= 12540 && aseVersion < 15000) )
//		if (aseVersion >= 1501000 || (aseVersion >= 1254000 && aseVersion < 1500000) )
		if (aseVersion >= Ver.ver(15,0,1) || (aseVersion >= Ver.ver(12,5,4) && aseVersion < Ver.ver(15,0)) )
		{
		}
		cols2 += ", WaitTimePerWait = CASE WHEN Waits > 0 \n" +
		         "                         THEN convert(numeric(10,3), (WaitTime + 0.0) / Waits) \n" +
		         "                         ELSE convert(numeric(10,3), 0.0) \n" +
		         "                     END \n";

		String sql = 
			"select \n" + cols1 + cols2 + cols3 + "\n" +
			"from master..monSysWaits W \n" +
			"order by " + (isClusterEnabled ? "W.WaitEventID, InstanceID" : "W.WaitEventID") + "\n";

		return sql;
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
	
	public static final String   GRAPH_NAME_EVENT_NAME = "sysWaitName"; 
	public static final String   GRAPH_NAME_CLASS_NAME = "sysClassName"; 
	
	// Keep labels in the same order
//	private Map<Integer, Integer> _labelOrder_eventId         = new LinkedHashMap<Integer, Integer>();
//	private Map<String,  Integer> _labelOrder_className       = new LinkedHashMap<String,  Integer>();
//	private Map<Integer, String>  _labelOrder_aPosToEventName = new LinkedHashMap<Integer, String>();
//	private Map<Integer, String>  _labelOrder_aPosToClassName = new LinkedHashMap<Integer, String>();

	private void addTrendGraphs()
	{
		String[] labels = new String[] { "runtime-replaced" };
		
		addTrendGraphData(GRAPH_NAME_EVENT_NAME, new TrendGraphDataPoint(GRAPH_NAME_EVENT_NAME,   labels));
		addTrendGraphData(GRAPH_NAME_CLASS_NAME, new TrendGraphDataPoint(GRAPH_NAME_CLASS_NAME, labels));

		// if GUI
		if (getGuiController() != null && getGuiController().hasGUI())
		{
			// GRAPH
			TrendGraph tg = null;
			tg = new WaitTrendGraph(GRAPH_NAME_EVENT_NAME,
					"Server Wait, group by EventID, WaitTimePerWait Average", 	                   // Menu CheckBox text
					"Server Wait, group by EventID, WaitTimePerWait Average (from monSysWaits)", // Label 
					labels, 
					false, // is Percent Graph
					this, 
					false, // visible at start
					0,    // graph is valid from Server Version. 0 = All Versions; >0 = Valid from this version and above 
					160);  // minimum height
			addTrendGraph(tg.getName(), tg, true);

			// GRAPH
			tg = new WaitTrendGraph(GRAPH_NAME_CLASS_NAME,
					"Server Wait, group by ClassName, WaitTimePerWait Average", 	                     // Menu CheckBox text
					"Server Wait, group by ClassName, WaitTimePerWait Average (from monSysWaits)", // Label 
					labels, 
					false, // is Percent Graph
					this, 
					false, // visible at start
					0,    // graph is valid from Server Version. 0 = All Versions; >0 = Valid from this version and above 
					-1);  // minimum height
			addTrendGraph(tg.getName(), tg, true);
		}
	}
	
	/**
	 * Local class which extends TrendGraph so we can create an extended menu set...
	 */
	private class WaitTrendGraph
	extends TrendGraph
	{
		public WaitTrendGraph(String name, String chkboxText, String label, String[] seriesNames, boolean pct, CountersModel cm, boolean initialVisible, int validFromVersion, int panelMinHeight)
		{
			super(name, chkboxText, label, seriesNames, pct, cm, initialVisible, validFromVersion, panelMinHeight);
		}
		
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
					CmSysWaitsPanel.openPropertiesEditor();
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
		
		@Override
		public void resetGraph()
		{
			getTrendGraphData(getName()).clear();

//			if (GRAPH_NAME_EVENT_NAME.equals(getName()))
//			{
//				_labelOrder_eventId         = new LinkedHashMap<Integer, Integer>();
//				_labelOrder_aPosToEventName = new LinkedHashMap<Integer, String>();
//			}
//
//			if (GRAPH_NAME_CLASS_NAME.equals(getName()))
//			{
//				_labelOrder_className       = new LinkedHashMap<String,  Integer>();
//				_labelOrder_aPosToClassName = new LinkedHashMap<Integer, String>();
//			}

			super.resetGraph();
		}
	}

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

//		// GET CONFIG: skipUserNameList
//		String skipUserNameListStr = conf.getProperty(PROPKEY_trendGraph_skipUserNameList, DEFAULT_trendGraph_skipUserNameList);
//		List<String> skipUserNameList = StringUtil.commaStrToList(skipUserNameListStr);

//		// GET CONFIG: skipSystemThreads
//		boolean skipSystemThreads = conf.getBooleanProperty(PROPKEY_trendGraph_skipSystemThreads, DEFAULT_trendGraph_skipSystemThreads);

		// GET CONFIG: Graph Data Source
		WaitCounterSummary.Type type = WaitCounterSummary.Type.WaitTimePerWait; 
		String dataSourceStr = conf.getProperty(PROPKEY_trendGraph_dataSource, DEFAULT_trendGraph_dataSource);
		try {type = WaitCounterSummary.Type.valueOf(dataSourceStr);}
		catch (Throwable t) {_logger.warn("CM='"+getName()+"', Problems converting '"+dataSourceStr+"' to WaitCounterSummary.Type. Using '"+type+"' instead.");}

		// Create the Summary object
		WaitCounterSummary wcs = WaitCounterSummary.create(this, skipEventIdList, skipEventClassList, null, false);
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
					tg.setLabel("Server Wait, group by EventID, "+type+" Average (from monSysWaits)");

				HashMap<String, Double> dataMap = new HashMap<String, Double>();
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
				tgdp.setData(this.getTimestamp(), dataMap);

//				// data & label array size = WaitCounterSummary.size or PRIVIOUS max size
//				Double[] dArr = new Double[Math.max(wcs.getEventIdSize(), _labelOrder_eventId.size())];
//				String[] lArr = new String[dArr.length];
//
////System.out.println("dArr & lArr lenth = " + dArr.length + ", wcs.getEventIdSize()="+wcs.getEventIdSize()+", _labelOrder_eventId.size()="+_labelOrder_eventId.size());
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
////System.out.println("NEW @_labelOrder_eventId: aPos="+aPos+", wce.getWaitEventID()="+wce.getWaitEventID()+", wce.getEventNameLabel()='"+wce.getEventNameLabel()+"'.");
//						if (aPos >= dArr.length)
//						{
//							Double[] new_dArr = new Double[aPos + 1];
//							String[] new_lArr = new String[new_dArr.length];
//							System.arraycopy(dArr, 0, new_dArr, 0, dArr.length);
//							System.arraycopy(lArr, 0, new_lArr, 0, lArr.length);
//							dArr = new_dArr;
//							lArr = new_lArr;
////System.out.println("####################### NEW EXTEND(new size="+dArr.length+") @_labelOrder_eventId: aPos="+aPos+", wce.getWaitEventID()="+wce.getWaitEventID()+", wce.getEventNameLabel()='"+wce.getEventNameLabel()+"'.");
//						}
//					}
//
//					// Get the type of data we are going to present
//					double dataValue = 0.0;
//					if      (type == WaitCounterSummary.Type.WaitTime       ) dataValue = wce.getAvgWaitTime();
//					else if (type == WaitCounterSummary.Type.Waits          ) dataValue = wce.getAvgWaits();
//					else if (type == WaitCounterSummary.Type.WaitTimePerWait) dataValue = wce.getAvgWaitTimePerWait();
//
////if (aPos >= dArr.length)
////System.out.println("XXXXXXXXXXXXXXXX: aPos = " + aPos + ", dArr.length="+dArr.length+", wce.getEventNameLabel()='"+wce.getEventNameLabel()+"'.");
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
					tg.setLabel("Server Wait, group by ClassName, "+type+" Average (from monSysWaits)");

				HashMap<String, Double> dataMap = new HashMap<String, Double>();
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