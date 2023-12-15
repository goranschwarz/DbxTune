/*******************************************************************************
 * Copyright (C) 2010-2020 Goran Schwarz
 * 
 * This file is part of DbxTune
 * DbxTune is a family of sub-products *Tune, hence the Dbx
 * Here are some of the tools: AseTune, IqTune, RsTune, RaxTune, HanaTune, 
 *          SqlServerTune, PostgresTune, MySqlTune, MariaDbTune, Db2Tune, ...
 * 
 * DbxTune is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, version 3 of the License.
 * 
 * DbxTune is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License
 * along with DbxTune.  If not, see <http://www.gnu.org/licenses/>.
 ******************************************************************************/
package com.asetune.cm.ase;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.math.BigDecimal;
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
import com.asetune.central.pcs.CentralPersistReader;
import com.asetune.cm.CmSettingsHelper;
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
import com.asetune.graph.TrendGraphDataPoint.LabelType;
import com.asetune.gui.MainFrame;
import com.asetune.gui.TabularCntrPanel;
import com.asetune.sql.conn.DbxConnection;
import com.asetune.sql.conn.info.DbmsVersionInfo;
import com.asetune.sql.conn.info.DbmsVersionInfoSybaseAse;
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

	public static final long     NEED_SRV_VERSION = 0;
	public static final long     NEED_CE_VERSION  = 0;

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
	public List<CmSettingsHelper> getLocalSettings()
	{
		Configuration conf = Configuration.getCombinedConfiguration();
		List<CmSettingsHelper> list = new ArrayList<>();
		
		list.add(new CmSettingsHelper("Skip Wait ID List",      PROPKEY_trendGraph_skipWaitIdList    , String.class, conf.getProperty(PROPKEY_trendGraph_skipWaitIdList    , DEFAULT_trendGraph_skipWaitIdList    ), DEFAULT_trendGraph_skipWaitIdList   , "Skip specific WaitEventID's from beeing in ThrendGraph"            ));
		list.add(new CmSettingsHelper("Skip Wait Class List",   PROPKEY_trendGraph_skipWaitClassList , String.class, conf.getProperty(PROPKEY_trendGraph_skipWaitClassList , DEFAULT_trendGraph_skipWaitClassList ), DEFAULT_trendGraph_skipWaitClassList, "Skip specific Event Clases from beeing in ThrendGraph"             ));
		list.add(new CmSettingsHelper("Trend Graph Datasource", PROPKEY_trendGraph_dataSource        , String.class, conf.getProperty(PROPKEY_trendGraph_dataSource        , DEFAULT_trendGraph_dataSource        ), DEFAULT_trendGraph_dataSource       , "What column should be the source WaitTime, Waits, WaitTimePerWait" ));

		return list;
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
	public void addMonTableDictForVersion(DbxConnection conn, DbmsVersionInfo versionInfo)
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
	public String[] getDependsOnConfigForVersion(DbxConnection conn, DbmsVersionInfo versionInfo)
	{
		return NEED_CONFIG;
	}

	@Override
	public List<String> getPkForVersion(DbxConnection conn, DbmsVersionInfo versionInfo)
	{
		DbmsVersionInfoSybaseAse aseVersionInfo = (DbmsVersionInfoSybaseAse) versionInfo;
//		long    srvVersion       = aseVersionInfo.getLongVersion();
		boolean isClusterEnabled = aseVersionInfo.isClusterEdition();

		List <String> pkCols = new LinkedList<String>();

		if (isClusterEnabled)
			pkCols.add("InstanceID");

		pkCols.add("WaitEventID");

		return pkCols;
	}

	@Override
	public String getSqlForVersion(DbxConnection conn, DbmsVersionInfo versionInfo)
	{
		DbmsVersionInfoSybaseAse aseVersionInfo = (DbmsVersionInfoSybaseAse) versionInfo;
		long    srvVersion       = aseVersionInfo.getLongVersion();
		boolean isClusterEnabled = aseVersionInfo.isClusterEdition();

		String cols1, cols2, cols3;
		cols1 = cols2 = cols3 = "";


		if (isClusterEnabled)
		{
			cols1 += "InstanceID, \n";
		}

		cols1 += "WaitClassDesc = convert(varchar(120),''), -- runtime replaced with cached values from monWaitClassInfo \n";
		cols1 += "WaitEventDesc = convert(varchar(120),''), -- runtime replaced with cached values from monWaitEventInfo \n";
		cols1 += "W.WaitEventID, WaitTime, Waits \n";
//		if (srvVersion >= 15010 || (srvVersion >= 12540 && srvVersion < 15000) )
//		if (srvVersion >= 1501000 || (srvVersion >= 1254000 && srvVersion < 1500000) )
		if (srvVersion >= Ver.ver(15,0,1) || (srvVersion >= Ver.ver(12,5,4) && srvVersion < Ver.ver(15,0)) )
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
	
//	public static final String   GRAPH_NAME_EVENT_NAME = "sysWaitName"; 
//	public static final String   GRAPH_NAME_CLASS_NAME = "sysClassName"; 
	
	public static final String   GRAPH_NAME_EVENT_WAITS = "sysWaits"; 
	public static final String   GRAPH_NAME_EVENT_WTIME = "sysWTime"; 
	public static final String   GRAPH_NAME_EVENT_WTPW  = "sysWtpw";
	
	public static final String   GRAPH_NAME_CLASS_WAITS = "sysClassWaits"; 
	public static final String   GRAPH_NAME_CLASS_WTIME = "sysClassWTime"; 
	public static final String   GRAPH_NAME_CLASS_WTPW  = "sysClassWtpw"; 

	private void addTrendGraphs()
	{
		//----------------------------------
		// GRAPH - EVENTID
		//----------------------------------
		addTrendGraph(GRAPH_NAME_EVENT_WAITS,
			"Server Wait, group by EventID, Waits Average", 	                   // Menu CheckBox text
			"Server Wait, group by EventID, Waits Average ("+GROUP_NAME+"->"+SHORT_NAME+")", // Label 
			TrendGraphDataPoint.createGraphProps(TrendGraphDataPoint.Y_AXIS_SCALE_LABELS_NORMAL, CentralPersistReader.SampleType.MAX_OVER_SAMPLES),
			null,
			LabelType.Dynamic,
			TrendGraphDataPoint.Category.WAITS,
			false, // is Percent Graph
			false, // visible at start
			0,    // graph is valid from Server Version. 0 = All Versions; >0 = Valid from this version and above 
			160);  // minimum height

		addTrendGraph(GRAPH_NAME_EVENT_WTIME,
			"Server Wait, group by EventID, WaitTime Average", 	                   // Menu CheckBox text
			"Server Wait, group by EventID, WaitTime Average ("+GROUP_NAME+"->"+SHORT_NAME+")", // Label 
			TrendGraphDataPoint.createGraphProps(TrendGraphDataPoint.Y_AXIS_SCALE_LABELS_MILLISEC, CentralPersistReader.SampleType.MAX_OVER_SAMPLES),
			null,
			LabelType.Dynamic,
			TrendGraphDataPoint.Category.WAITS,
			false, // is Percent Graph
			false, // visible at start
			0,    // graph is valid from Server Version. 0 = All Versions; >0 = Valid from this version and above 
			160);  // minimum height

		addTrendGraph(GRAPH_NAME_EVENT_WTPW,
			"Server Wait, group by EventID, WaitTimePerWait Average", 	                   // Menu CheckBox text
			"Server Wait, group by EventID, WaitTimePerWait Average ("+GROUP_NAME+"->"+SHORT_NAME+")", // Label 
			TrendGraphDataPoint.createGraphProps(TrendGraphDataPoint.Y_AXIS_SCALE_LABELS_MILLISEC, CentralPersistReader.SampleType.MAX_OVER_SAMPLES),
			null,
			LabelType.Dynamic,
			TrendGraphDataPoint.Category.WAITS,
			false, // is Percent Graph
			false, // visible at start
			0,    // graph is valid from Server Version. 0 = All Versions; >0 = Valid from this version and above 
			160);  // minimum height

		//----------------------------------
		// GRAPH - CLASS
		//----------------------------------
		addTrendGraph(GRAPH_NAME_CLASS_WAITS,
			"Server Wait, group by ClassName, Waits Average", 	                     // Menu CheckBox text
			"Server Wait, group by ClassName, Waits Average ("+GROUP_NAME+"->"+SHORT_NAME+")", // Label 
			TrendGraphDataPoint.createGraphProps(TrendGraphDataPoint.Y_AXIS_SCALE_LABELS_NORMAL, CentralPersistReader.SampleType.MAX_OVER_SAMPLES),
			null,
			LabelType.Dynamic,
			TrendGraphDataPoint.Category.WAITS,
			false, // is Percent Graph
			false, // visible at start
			0,    // graph is valid from Server Version. 0 = All Versions; >0 = Valid from this version and above 
			-1);  // minimum height

		addTrendGraph(GRAPH_NAME_CLASS_WTIME,
			"Server Wait, group by ClassName, WaitTime Average", 	                     // Menu CheckBox text
			"Server Wait, group by ClassName, WaitTime Average ("+GROUP_NAME+"->"+SHORT_NAME+")", // Label 
			TrendGraphDataPoint.createGraphProps(TrendGraphDataPoint.Y_AXIS_SCALE_LABELS_MILLISEC, CentralPersistReader.SampleType.MAX_OVER_SAMPLES),
			null,
			LabelType.Dynamic,
			TrendGraphDataPoint.Category.WAITS,
			false, // is Percent Graph
			false, // visible at start
			0,    // graph is valid from Server Version. 0 = All Versions; >0 = Valid from this version and above 
			-1);  // minimum height

		addTrendGraph(GRAPH_NAME_CLASS_WTPW,
			"Server Wait, group by ClassName, WaitTimePerWait Average", 	                     // Menu CheckBox text
			"Server Wait, group by ClassName, WaitTimePerWait Average ("+GROUP_NAME+"->"+SHORT_NAME+")", // Label 
			TrendGraphDataPoint.createGraphProps(TrendGraphDataPoint.Y_AXIS_SCALE_LABELS_MILLISEC, CentralPersistReader.SampleType.MAX_OVER_SAMPLES),
			null,
			LabelType.Dynamic,
			TrendGraphDataPoint.Category.WAITS,
			false, // is Percent Graph
			false, // visible at start
			0,    // graph is valid from Server Version. 0 = All Versions; >0 = Valid from this version and above 
			-1);  // minimum height

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
		
//		//------------------------------------------------------------
//		mi = new JMenuItem("Change DataSource/Column...");
//		list.add(mi);
//		mi.addActionListener(new ActionListener()
//		{
//			@Override
//			public void actionPerformed(ActionEvent e)
//			{
//				String before = Configuration.getCombinedConfiguration().getProperty(PROPKEY_trendGraph_dataSource, DEFAULT_trendGraph_dataSource);
//
//				WaitCounterSummary.openDataSourceDialog(PROPKEY_trendGraph_dataSource, DEFAULT_trendGraph_dataSource);
//
//				String after = Configuration.getCombinedConfiguration().getProperty(PROPKEY_trendGraph_dataSource, DEFAULT_trendGraph_dataSource);
//				if ( ! before.equals(after) )
//				{
//					for (TrendGraph tg : getTrendGraphs().values())
//						tg.resetGraph();
//				}
//			}
//		});
		
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
//					CmSysWaitsPanel.openPropertiesEditor();
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
////
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

		Configuration conf = Configuration.getCombinedConfiguration();

		// GET CONFIG: skipEventIdList
		List<Integer> skipEventIdList = new ArrayList<Integer>();
		String skipEventIdListStr = conf.getProperty(PROPKEY_trendGraph_skipWaitIdList, DEFAULT_trendGraph_skipWaitIdList);
		for (String str : StringUtil.commaStrToSet(skipEventIdListStr))
		{
			try { skipEventIdList.add(Integer.parseInt(str)); }
			catch (NumberFormatException nfe) {_logger.info("CmName='"+getName()+"' updateGraphData(): when reading property '"+PROPKEY_trendGraph_skipWaitIdList+"' found a non number('"+str+"') in the list of WaitEventID's to skip. This specific 'ID' will not be added to the list. The full input-list, which has problems looks like '"+skipEventIdListStr+"'.");}
		}
		
		// GET CONFIG: skipEventClassList
		String skipEventClassListStr = conf.getProperty(PROPKEY_trendGraph_skipWaitClassList, DEFAULT_trendGraph_skipWaitClassList);
		List<String> skipEventClassList = StringUtil.commaStrToList(skipEventClassListStr);

		// GET CONFIG: Graph Data Source
		WaitCounterSummary.Type type = WaitCounterSummary.Type.WaitTimePerWait; 
		String dataSourceStr = conf.getProperty(PROPKEY_trendGraph_dataSource, DEFAULT_trendGraph_dataSource);
		try {type = WaitCounterSummary.Type.valueOf(dataSourceStr);}
		catch (Throwable t) {_logger.warn("CM='"+getName()+"', Problems converting '"+dataSourceStr+"' to WaitCounterSummary.Type. Using '"+type+"' instead.");}

		// Create the Summary object
		WaitCounterSummary wcs = WaitCounterSummary.create(this, skipEventIdList, skipEventClassList, null, false);

		// loop the graphs
		for (TrendGraphDataPoint tgdp : trendGraphsData.values()) 
		{
//			if (GRAPH_NAME_EVENT_NAME.equals(tgdp.getName()))
//			{
//				// Set/change the Label....
//				TrendGraph tg = getTrendGraph(tgdp.getName());
//				if (tg != null)
//					tg.setChartLabel("Server Wait, group by EventID, "+type+" Average ("+GROUP_NAME+"->"+SHORT_NAME+")");
//
//				LinkedHashMap<String, Double> dataMap = new LinkedHashMap<String, Double>();
//				for (WaitCounterEntry wce : wcs.getEventIdMap().values())
//				{
//					// Get the type of data we are going to present
//					double dataValue = 0.0;
//					if      (type == WaitCounterSummary.Type.WaitTime       ) dataValue = wce.getAvgWaitTime();
//					else if (type == WaitCounterSummary.Type.Waits          ) dataValue = wce.getAvgWaits();
//					else if (type == WaitCounterSummary.Type.WaitTimePerWait) dataValue = wce.getAvgWaitTimePerWait();
//
//					// set data & label in the array
//					dataMap.put(wce.getEventNameLabel(), dataValue);
//				}
//				if ( ! dataMap.isEmpty() )
//					tgdp.setData(this.getTimestamp(), dataMap);
//
//			}
//
//			if (GRAPH_NAME_CLASS_NAME.equals(tgdp.getName()))
//			{
//				// Set/change the Label....
//				TrendGraph tg = getTrendGraph(tgdp.getName());
//				if (tg != null)
//					tg.setChartLabel("Server Wait, group by ClassName, "+type+" Average ("+GROUP_NAME+"->"+SHORT_NAME+")");
//
//				LinkedHashMap<String, Double> dataMap = new LinkedHashMap<String, Double>();
//				for (WaitCounterEntry wce : wcs.getClassNameMap().values())
//				{
//					// Get the type of data we are going to present
//					double dataValue = 0.0;
//					if      (type == WaitCounterSummary.Type.WaitTime       ) dataValue = wce.getAvgWaitTime();
//					else if (type == WaitCounterSummary.Type.Waits          ) dataValue = wce.getAvgWaits();
//					else if (type == WaitCounterSummary.Type.WaitTimePerWait) dataValue = wce.getAvgWaitTimePerWait();
//
//					// set data & label in the array
//					dataMap.put(wce.getClassName(), dataValue);
//				}
//				if ( ! dataMap.isEmpty() )
//					tgdp.setData(this.getTimestamp(), dataMap);
//			}
//			
//			if (_logger.isDebugEnabled())
//				_logger.debug("cm='"+StringUtil.left(this.getName(),25)+"', trendGraphsData="+tgdp);
//System.out.println("cm='"+StringUtil.left(this.getName(),25)+"', trendGraphsData="+tgdp);

		

			//--------------------------------
			// EventId
			//--------------------------------
			if (GRAPH_NAME_EVENT_WAITS.equals(tgdp.getName()))
			{
				LinkedHashMap<String, Double> dataMap = new LinkedHashMap<String, Double>();
				for (WaitCounterEntry wce : wcs.getEventIdMap().values())
				{
					// set data & label in the array
					dataMap.put(wce.getEventNameLabel(), wce.getAvgWaits());
				}
				if (dataMap.size() > 0)
					tgdp.setData(this.getTimestamp(), dataMap);
			}

			if (GRAPH_NAME_EVENT_WTIME.equals(tgdp.getName()))
			{
				LinkedHashMap<String, Double> dataMap = new LinkedHashMap<String, Double>();
				for (WaitCounterEntry wce : wcs.getEventIdMap().values())
				{
					// set data & label in the array
					dataMap.put(wce.getEventNameLabel(), wce.getAvgWaitTime());
				}
				if (dataMap.size() > 0)
					tgdp.setData(this.getTimestamp(), dataMap);
			}

			if (GRAPH_NAME_EVENT_WTPW.equals(tgdp.getName()))
			{
				LinkedHashMap<String, Double> dataMap = new LinkedHashMap<String, Double>();
				for (WaitCounterEntry wce : wcs.getEventIdMap().values())
				{
					// set data & label in the array
					dataMap.put(wce.getEventNameLabel(), wce.getAvgWaitTimePerWait());
				}
				if (dataMap.size() > 0)
					tgdp.setData(this.getTimestamp(), dataMap);
			}

			//--------------------------------
			// CLASS
			//--------------------------------
			if (GRAPH_NAME_CLASS_WAITS.equals(tgdp.getName()))
			{
				LinkedHashMap<String, Double> dataMap = new LinkedHashMap<String, Double>();
				for (WaitCounterEntry wce : wcs.getClassNameMap().values())
				{
					// set data & label in the array
					dataMap.put(wce.getClassName(), wce.getAvgWaits());
				}
				if (dataMap.size() > 0)
					tgdp.setData(this.getTimestamp(), dataMap);
			}

			if (GRAPH_NAME_CLASS_WTIME.equals(tgdp.getName()))
			{
				LinkedHashMap<String, Double> dataMap = new LinkedHashMap<String, Double>();
				for (WaitCounterEntry wce : wcs.getClassNameMap().values())
				{
					// set data & label in the array
					dataMap.put(wce.getClassName(), wce.getAvgWaitTime());
				}
				if (dataMap.size() > 0)
					tgdp.setData(this.getTimestamp(), dataMap);
			}

			if (GRAPH_NAME_CLASS_WTPW.equals(tgdp.getName()))
			{
				LinkedHashMap<String, Double> dataMap = new LinkedHashMap<String, Double>();
				for (WaitCounterEntry wce : wcs.getClassNameMap().values())
				{
					// set data & label in the array
					dataMap.put(wce.getClassName(), wce.getAvgWaitTimePerWait());
				}
				if (dataMap.size() > 0)
					tgdp.setData(this.getTimestamp(), dataMap);
			}
		}
	}

	@Override
	public void updateGraphData(TrendGraphDataPoint tgdp)
	{
	}
}
