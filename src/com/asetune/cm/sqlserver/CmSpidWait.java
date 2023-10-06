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
package com.asetune.cm.sqlserver;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.swing.JComponent;
import javax.swing.JMenuItem;

import org.apache.log4j.Logger;

import com.asetune.ICounterController;
import com.asetune.IGuiController;
import com.asetune.cm.CmSettingsHelper;
import com.asetune.cm.CounterSample;
import com.asetune.cm.CounterSetTemplates;
import com.asetune.cm.CounterSetTemplates.Type;
import com.asetune.cm.CounterTableModel;
import com.asetune.cm.CountersModel;
import com.asetune.cm.sqlserver.gui.CmSpidWaitPanel;
import com.asetune.config.dict.SqlServerWaitTypeDictionary;
import com.asetune.graph.TrendGraphDataPoint;
import com.asetune.graph.TrendGraphDataPoint.LabelType;
import com.asetune.gui.MainFrame;
import com.asetune.gui.TabularCntrPanel;
import com.asetune.sql.conn.DbxConnection;
import com.asetune.sql.conn.info.DbmsVersionInfo;
import com.asetune.sql.conn.info.DbmsVersionInfoSqlServer;
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
		"<p>What are the SPID's waiting for</p>" +
		"</html>";

	public static final String   GROUP_NAME       = MainFrame.TCP_GROUP_SERVER;
	public static final String   GUI_ICON_FILE    = "images/"+CM_NAME+".png";

	public static final long     NEED_SRV_VERSION = Ver.ver(2016);
	public static final long     NEED_CE_VERSION  = 0;

	public static final String[] MON_TABLES       = new String[] {"dm_exec_session_wait_stats"};
	public static final String[] NEED_ROLES       = new String[] {};//{"VIEW SERVER STATE"};
	public static final String[] NEED_CONFIG      = new String[] {};

	public static final String[] PCT_COLUMNS      = new String[] {};
	public static final String[] DIFF_COLUMNS     = new String[] {
		"waiting_tasks_count",  
		"wait_time_ms", 
//		"max_wait_time_ms", 
		"signal_wait_time_ms"
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

		setShowClearTime(false);

		setCounterController(counterController);
		setGuiController(guiController);
		
		addTrendGraphs();
		
		CounterSetTemplates.register(this);
	}


	//------------------------------------------------------------
	// Implementation
	//------------------------------------------------------------
	private static final String  PROP_PREFIX                         = CM_NAME;

	public static final String  PROPKEY_sample_extraWhereClause      = PROP_PREFIX + ".sample.extraWhereClause";
	public static final String  DEFAULT_sample_extraWhereClause      = "";

	public static final String  PROPKEY_trendGraph_skipWaitTypeList    = PROP_PREFIX + ".trendGraph.skipWaitTypeList";
	public static final String  DEFAULT_trendGraph_skipWaitTypeList    = "";

//	public static final String  PROPKEY_trendGraph_skipWaitClassList = PROP_PREFIX + ".trendGraph.skipWaitClassList";
//	public static final String  DEFAULT_trendGraph_skipWaitClassList = "";

//	public static final String  PROPKEY_trendGraph_skipUserNameList  = PROP_PREFIX + ".trendGraph.skipUserNameList";
//	public static final String  DEFAULT_trendGraph_skipUserNameList  = "";

//	public static final String  PROPKEY_trendGraph_skipSystemThreads = PROP_PREFIX + ".trendGraph.skipSystemThreads";
//	public static final boolean DEFAULT_trendGraph_skipSystemThreads = true;

	@Override
	protected void registerDefaultValues()
	{
		super.registerDefaultValues();

		Configuration.registerDefaultValue(PROPKEY_sample_extraWhereClause,      DEFAULT_sample_extraWhereClause);
		Configuration.registerDefaultValue(PROPKEY_trendGraph_skipWaitTypeList,  DEFAULT_trendGraph_skipWaitTypeList);
//		Configuration.registerDefaultValue(PROPKEY_trendGraph_skipWaitClassList, DEFAULT_trendGraph_skipWaitClassList);
//		Configuration.registerDefaultValue(PROPKEY_trendGraph_skipUserNameList,  DEFAULT_trendGraph_skipUserNameList);
//		Configuration.registerDefaultValue(PROPKEY_trendGraph_skipSystemThreads, DEFAULT_trendGraph_skipSystemThreads);
//		Configuration.registerDefaultValue(PROPKEY_trendGraph_dataSource,        DEFAULT_trendGraph_dataSource);
	}
	
	@Override
	protected TabularCntrPanel createGui()
	{
		return new CmSpidWaitPanel(this);
	}

	@Override
	public boolean checkDependsOnVersion(DbxConnection conn)
	{
		DbmsVersionInfoSqlServer versionInfo = (DbmsVersionInfoSqlServer) conn.getDbmsVersionInfo();
		if (versionInfo.isAzureDb() || versionInfo.isAzureSynapseAnalytics())
			return true;

		return super.checkDependsOnVersion(conn);
	}

	@Override
	public String[] getDependsOnConfigForVersion(DbxConnection conn, DbmsVersionInfo versionInfo)
	{
		return NEED_CONFIG;
	}

	@Override
	public List<String> getPkForVersion(DbxConnection conn, DbmsVersionInfo versionInfo)
	{
		List <String> pkCols = new LinkedList<String>();

		pkCols.add("session_id");
		pkCols.add("wait_type");

		return pkCols;
	}

//	RS> Col# Label               JDBC Type Name          Guessed DBMS type Source Table
//	RS> ---- ------------------- ----------------------- ----------------- ------------
//	RS> 1    session_id          java.sql.Types.SMALLINT smallint          -none-      
//	RS> 2    wait_type           java.sql.Types.NVARCHAR nvarchar(60)      -none-      
//	RS> 3    waiting_tasks_count java.sql.Types.BIGINT   bigint            -none-      
//	RS> 4    wait_time_ms        java.sql.Types.BIGINT   bigint            -none-      
//	RS> 5    max_wait_time_ms    java.sql.Types.BIGINT   bigint            -none-      
//	RS> 6    signal_wait_time_ms java.sql.Types.BIGINT   bigint            -none-
	
	@Override
	public String getSqlForVersion(DbxConnection conn, DbmsVersionInfo versionInfo)
	{
		DbmsVersionInfoSqlServer ssVersionInfo = (DbmsVersionInfoSqlServer) versionInfo;
		
		String dm_exec_session_wait_stats = "sys.dm_exec_session_wait_stats";
//		String dm_exec_connections        = "sys.dm_exec_connections";
		
		if (ssVersionInfo.isAzureSynapseAnalytics())
		{
			dm_exec_session_wait_stats = "sys.dm_exec_session_wait_stats";                   // Same name in Azure ???
//			dm_exec_connections        = "sys.dm_pdw_exec_connections";
		}

		Configuration conf = Configuration.getCombinedConfiguration();
		String  sample_extraWhereClause = conf.getProperty(PROPKEY_sample_extraWhereClause, DEFAULT_sample_extraWhereClause);

		// Do we have extra where clauses
		String sql_sample_extraWhereClause = "  -- Extra where clauses will go here. (it will look like: AND the_extra_where_clause) \n";
		if ( ! StringUtil.isNullOrBlank(sample_extraWhereClause) )
			sql_sample_extraWhereClause = "  and " + sample_extraWhereClause + "\n";

		String sql = 
				"select /* ${cmCollectorName} */ \n" +
				"    session_id, \n" +
				"    wait_type, \n" +
				"    WaitClass = convert(varchar(30), null), \n" +
				"    waiting_tasks_count, \n" +
				"    wait_time_ms, \n" +
				"    max_wait_time_ms, \n" +
				"    signal_wait_time_ms, \n" +
				"    WaitTimePerCount = CASE WHEN waiting_tasks_count > 0 \n" +
				"                          THEN convert(numeric(15,3), (wait_time_ms + 0.0) /waiting_tasks_count) \n" +
				"                          ELSE convert(numeric(15,3), 0.0) \n" +
				"                      END \n" +
				"from " + dm_exec_session_wait_stats + " \n" +
				"where 1 = 1 \n" +
//				"  and session_id in (select session_id from " + dm_exec_connections + ")" +
				sql_sample_extraWhereClause +
				"order by session_id, wait_type \n" +
				"";

		// TODO: Possibly add 'dbname' -- So we can get statistics on Wait's per Database
//		   ,dbname = DB_NAME(es.database_id)
//				   FROM sys.dm_exec_session_wait_stats ws 
//				   LEFT OUTER JOIN sys.dm_exec_sessions es ON ws.session_id = es.session_id
		

		return sql;
	}

	/** Used by the: Create 'Offline Session' Wizard */
	@Override
	public List<CmSettingsHelper> getLocalSettings()
	{
		Configuration conf = Configuration.getCombinedConfiguration();
		List<CmSettingsHelper> list = new ArrayList<>();
		
		list.add(new CmSettingsHelper("Extra Where Clause",     PROPKEY_sample_extraWhereClause      , String.class,  conf.getProperty       (PROPKEY_sample_extraWhereClause      , DEFAULT_sample_extraWhereClause      ), DEFAULT_sample_extraWhereClause     , CmSpidWaitPanel.TOOLTIP_sample_extraWhereClause                     ));
		list.add(new CmSettingsHelper("Skip wait_type List",    PROPKEY_trendGraph_skipWaitTypeList  , String.class,  conf.getProperty       (PROPKEY_trendGraph_skipWaitTypeList  , DEFAULT_trendGraph_skipWaitTypeList  ), DEFAULT_trendGraph_skipWaitTypeList , "Skip specific 'wait_type' from beeing in ThrendGraph"              ));
//		list.add(new CmSettingsHelper("Skip Wait Class List",   PROPKEY_trendGraph_skipWaitClassList , String.class,  conf.getProperty       (PROPKEY_trendGraph_skipWaitClassList , DEFAULT_trendGraph_skipWaitClassList ), DEFAULT_trendGraph_skipWaitClassList, "Skip specific Event Clases from beeing in ThrendGraph"             ));
//		list.add(new CmSettingsHelper("Skip Username List",     PROPKEY_trendGraph_skipUserNameList  , String.class,  conf.getProperty       (PROPKEY_trendGraph_skipUserNameList  , DEFAULT_trendGraph_skipUserNameList  ), DEFAULT_trendGraph_skipUserNameList , "Skip specific users from beeing in ThrendGraph"                    ));
//		list.add(new CmSettingsHelper("Skip System Processes",  PROPKEY_trendGraph_skipSystemThreads , Boolean.class, conf.getBooleanProperty(PROPKEY_trendGraph_skipSystemThreads , DEFAULT_trendGraph_skipSystemThreads ), DEFAULT_trendGraph_skipSystemThreads, "Skip System SPID's from beeing in ThrendGraph"                     ));

		return list;
	}


	/** 
	 * Fill in the WaitClass column with data from
	 * SqlServerWaitNameDictionary.. transforms a wait_type -> WaitClass
	 */
	@Override
	public void localCalculation(CounterSample newSample)
	{
		// Where are various columns located in the Vector 
		int pos_wait_type = -1;
		int pos_WaitClass = -1;
	
		SqlServerWaitTypeDictionary wd = SqlServerWaitTypeDictionary.getInstance();
		if (wd == null)
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

			if      (colName.equals("wait_type"))   pos_wait_type   = colId;
			else if (colName.equals("WaitClass"))   pos_WaitClass   = colId;
		}

		// Loop on all counter rows
		for (int rowId=0; rowId < newSample.getRowCount(); rowId++)
		{
			Object o_wait_type  = newSample.getValueAt(rowId, pos_wait_type);

			if (o_wait_type instanceof String)
			{
				String wait_type = (String)o_wait_type;

				String className = wd.getWaitClassForWaitType( wait_type );

				if (className != null)
					newSample.setValueAt(className, rowId, pos_WaitClass);
			}
		}
	}

	/** 
	 * Compute the WaitTimePerCount for diff values
	 */
	@Override
	public void localCalculation(CounterSample prevSample, CounterSample newSample, CounterSample diffData)
	{
		long wait_time_ms,          waiting_tasks_count;
		int  wait_time_ms_pos = -1, waiting_tasks_count_pos = -1;

		int WaitTimePerCount_pos = -1;

		// Find column Id's
		List<String> colNames = diffData.getColNames();
		if (colNames == null)
			return;

		for (int colId=0; colId < colNames.size(); colId++) 
		{
			String colName = colNames.get(colId);
			
			if      (colName.equals("WaitTimePerCount"))    WaitTimePerCount_pos     = colId;
			else if (colName.equals("wait_time_ms"))        wait_time_ms_pos        = colId;
			else if (colName.equals("waiting_tasks_count")) waiting_tasks_count_pos = colId;
		}

		// Loop on all diffData rows
		for (int rowId = 0; rowId < diffData.getRowCount(); rowId++)
		{
			wait_time_ms        = ((Number)diffData.getValueAt(rowId, wait_time_ms_pos       )).longValue();
			waiting_tasks_count = ((Number)diffData.getValueAt(rowId, waiting_tasks_count_pos)).longValue();

			// 
			if (waiting_tasks_count > 0)
			{
				double calcWaitTimePerCount = wait_time_ms / (waiting_tasks_count * 1.0);

				BigDecimal newVal = new BigDecimal(calcWaitTimePerCount).setScale(3, BigDecimal.ROUND_HALF_EVEN);;
				diffData.setValueAt(newVal, rowId, WaitTimePerCount_pos);
			}
			else
				diffData.setValueAt(new BigDecimal(0), rowId, WaitTimePerCount_pos);
		}
	}




	//---------------------------------------------------------------------------------
	//---------------------------------------------------------------------------------
	// Graph stuff
	//---------------------------------------------------------------------------------
	//---------------------------------------------------------------------------------
	
	public static final String   GRAPH_NAME_WAIT_TIME_MS        = "WaitTimeMs"; 
	public static final String   GRAPH_NAME_WAITING_TASKS_COUNT = "WaitTaskCnt"; 
	public static final String   GRAPH_NAME_WaitTimePerCount    = "WaitTimePerCnt"; 
	
	private void addTrendGraphs()
	{
		addTrendGraph(GRAPH_NAME_WAIT_TIME_MS,
			"SPID Wait, group by wait_type - wait_time_ms, Average", 	                   // Menu CheckBox text
			"SPID Wait, group by wait_type - wait_time_ms, Average ("+GROUP_NAME+"->"+SHORT_NAME+")", // Label 
			TrendGraphDataPoint.Y_AXIS_SCALE_LABELS_MILLISEC,
			null, 
			LabelType.Dynamic,
			TrendGraphDataPoint.Category.WAITS,
			false, // is Percent Graph
			false, // visible at start
			0,    // graph is valid from Server Version. 0 = All Versions; >0 = Valid from this version and above 
			160);  // minimum height

		addTrendGraph(GRAPH_NAME_WAITING_TASKS_COUNT,
			"SPID Wait, group by wait_type - waiting_tasks_count, Average", 	                   // Menu CheckBox text
			"SPID Wait, group by wait_type - waiting_tasks_count, Average ("+GROUP_NAME+"->"+SHORT_NAME+")", // Label 
			TrendGraphDataPoint.Y_AXIS_SCALE_LABELS_NORMAL,
			null, 
			LabelType.Dynamic,
			TrendGraphDataPoint.Category.WAITS,
			false, // is Percent Graph
			false, // visible at start
			0,    // graph is valid from Server Version. 0 = All Versions; >0 = Valid from this version and above 
			160);  // minimum height

		addTrendGraph(GRAPH_NAME_WaitTimePerCount,
			"SPID Wait, group by wait_type - WaitTimePerCount, Average", 	                   // Menu CheckBox text
			"SPID Wait, group by wait_type - WaitTimePerCount, Average ("+GROUP_NAME+"->"+SHORT_NAME+")", // Label 
			TrendGraphDataPoint.Y_AXIS_SCALE_LABELS_MILLISEC,
			null, 
			LabelType.Dynamic,
			TrendGraphDataPoint.Category.WAITS,
			false, // is Percent Graph
			false, // visible at start
			0,    // graph is valid from Server Version. 0 = All Versions; >0 = Valid from this version and above 
			160);  // minimum height
	}

	/**
	 * Add some menu items to the right click menu
	 */
	@Override
	public List<JComponent> createGraphSpecificMenuItems()
	{
		ArrayList<JComponent> list = new ArrayList<JComponent>();
		
		//------------------------------------------------------------
		JMenuItem  mi = new JMenuItem("Edit Skip 'wait_type'...");
		list.add(mi);
		mi.addActionListener(new ActionListener()
		{
			@Override
			public void actionPerformed(ActionEvent e)
			{
				CmSpidWaitPanel.openPropertiesEditor();
			}
		});
		
		return list;
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
		Configuration conf = Configuration.getCombinedConfiguration();

		// GET CONFIG: skipEventIdList
		String skipWaitTypeListStr = conf.getProperty(PROPKEY_trendGraph_skipWaitTypeList, DEFAULT_trendGraph_skipWaitTypeList);
		Set<String> skipWaitTypeSet = StringUtil.commaStrToSet(skipWaitTypeListStr);
		
//		// GET CONFIG: skipUserNameList
//		String skipUserNameListStr = conf.getProperty(PROPKEY_trendGraph_skipUserNameList, DEFAULT_trendGraph_skipUserNameList);
//		List<String> skipUserNameList = StringUtil.commaStrToList(skipUserNameListStr);
//
//		// GET CONFIG: skipSystemThreads
//		boolean skipSystemThreads = conf.getBooleanProperty(PROPKEY_trendGraph_skipSystemThreads, DEFAULT_trendGraph_skipSystemThreads);

		// GET CONFIG: Graph Data Source

		CounterTableModel dataTable = getCounterDataRate();
		
		int WaitType_pos         = dataTable.findColumn("wait_type");
		int WaitTime_pos         = dataTable.findColumn("wait_time_ms"); 
		int WaitCount_pos        = dataTable.findColumn("waiting_tasks_count");
		int WaitTimePerCount_pos = dataTable.findColumn("WaitTimePerCount"); 

		// Create the Summary object
		Map<String, Integer> eventWaitTypeCount     = new LinkedHashMap<String, Integer>();
		Map<String, Double>  eventWaitTime          = new LinkedHashMap<String, Double>();
		Map<String, Double>  eventWaitCount         = new LinkedHashMap<String, Double>();
		Map<String, Double>  eventWaitTimePerCount  = new LinkedHashMap<String, Double>();

		for(int r=0; r<dataTable.getRowCount(); r++)
		{
			String WaitType         = (String)dataTable.getValueAt(r, WaitType_pos);
			String WaitTypeLower    = WaitType.toLowerCase();
			Number WaitTime         = (Number)dataTable.getValueAt(r, WaitTime_pos);
			Number WaitCount        = (Number)dataTable.getValueAt(r, WaitCount_pos);
			Number WaitTimePerCount = (Number)dataTable.getValueAt(r, WaitTimePerCount_pos);

			// UserName was introduced in ASE 15.0.2 ESD#3, so if the column wasn't found dont get it 
//			if (UserName_pos > 0)
//				UserName = (String)dataTable.getValueAt(r, UserName_pos);

			// SKIP Wait EventId 250
//			if ( ! includeWaitId250 && WaitEventID.intValue() == 250)
//				continue;

			// SKIP System Threads
//			if ( ! includeSystemThreads && "".equals(UserName) )
//				continue;

			// SKIP event_type
			if ( skipWaitTypeSet.contains(WaitType) )
			{
				if (_logger.isDebugEnabled())
					_logger.debug("createDataset():GRAPH-DATA: "+getName()+": WaitType["+WaitType_pos+"]='"+WaitType+"', WaitTime["+WaitTime_pos+"]='"+WaitTime+"', WaitCount["+WaitCount_pos+"]='"+WaitCount+"', WaitTimePerCount["+WaitTimePerCount_pos+"]='"+WaitTimePerCount+"'.");
				continue;
			}

//			if (_logger.isDebugEnabled())
//				_logger.debug("createDataset():GRAPH-DATA: "+getName()+": ClassName("+ClassName_pos+")='"+ClassName+"', EventName("+EventName_pos+")='"+EventName+"', WaitTime("+WaitTime_pos+")='"+WaitTime+"', Waits("+Waits_pos+")='"+Waits+"', WaitTimePerCount("+WaitTimePerCount_pos+")='"+WaitTimePerCount+"'.");
			if (_logger.isDebugEnabled())
				_logger.debug("updateGraphData():GRAPH-DATA: "+getName()+": Skipping - WaitType["+WaitType_pos+"]='"+WaitType+".");

			Double sumWaitTime         = eventWaitTime        .get(WaitTypeLower);
			Double sumWaitCount        = eventWaitCount       .get(WaitTypeLower);
			Double sumWaitTimePerCount = eventWaitTimePerCount.get(WaitTypeLower);

			eventWaitTime        .put(WaitTypeLower, new Double(sumWaitTime        ==null ? WaitTime        .doubleValue() : sumWaitTime         + WaitTime        .doubleValue()) );
			eventWaitCount       .put(WaitTypeLower, new Double(sumWaitCount       ==null ? WaitCount       .doubleValue() : sumWaitCount        + WaitCount       .doubleValue()) );
			eventWaitTimePerCount.put(WaitTypeLower, new Double(sumWaitTimePerCount==null ? WaitTimePerCount.doubleValue() : sumWaitTimePerCount + WaitTimePerCount.doubleValue()) );

			// Increment: waitTypeCount --- every WaitType has it's own counter so we can do average of the sum values later on
			Integer waitTypeCount = eventWaitTypeCount.get(WaitTypeLower);
			if (waitTypeCount == null) 
				waitTypeCount = 0;
			waitTypeCount++;
			eventWaitTypeCount.put(WaitTypeLower, waitTypeCount);
		} // end: loop dataTable


		// Should we calculate AVERAGE or should we have the SUM
		boolean calculateAverage = true;
		if (calculateAverage)
		{
			Map<String, Double> map;
			
			map = eventWaitTime;
			for (String key : map.keySet())
			{
				Double  val = map               .get(key);
				Integer cnt = eventWaitTypeCount.get(key);

				// Set Average value
				map.put(key, val / cnt);
			}
			
			map = eventWaitCount;
			for (String key : map.keySet())
			{
				Double  val = map               .get(key);
				Integer cnt = eventWaitTypeCount.get(key);

				// Set Average value
				map.put(key, val / cnt);
			}
			
			map = eventWaitTimePerCount;
			for (String key : map.keySet())
			{
				Double  val = map               .get(key);
				Integer cnt = eventWaitTypeCount.get(key);

				// Set Average value
				map.put(key, val / cnt);
			}
		}
		
		
		// loop the graphs
		for (TrendGraphDataPoint tgdp : trendGraphsData.values()) 
		{
			//System.out.println("GRAPH_NAME='"+tgdp.getName()+"'.");
			if (_logger.isDebugEnabled())
				_logger.debug("cm='"+StringUtil.left(this.getName(),25)+"', trendGraphsData="+tgdp);
			
			if (GRAPH_NAME_WAIT_TIME_MS.equals(tgdp.getName()))
			{
				if (eventWaitTime.size() > 0)
					tgdp.setData(this.getTimestamp(), eventWaitTime);
			}

			if (GRAPH_NAME_WAITING_TASKS_COUNT.equals(tgdp.getName()))
			{
				if (eventWaitCount.size() > 0)
					tgdp.setData(this.getTimestamp(), eventWaitCount);
			}

			if (GRAPH_NAME_WaitTimePerCount.equals(tgdp.getName()))
			{
				if (eventWaitTimePerCount.size() > 0)
					tgdp.setData(this.getTimestamp(), eventWaitTimePerCount);
			}
		}
	}

	@Override
	public void updateGraphData(TrendGraphDataPoint tgdp)
	{
	}
}
