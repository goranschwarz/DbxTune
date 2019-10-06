/*******************************************************************************
 * Copyright (C) 2010-2019 Goran Schwarz
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

import java.awt.event.MouseEvent;
import java.math.BigDecimal;
import java.sql.Connection;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

import javax.naming.NameNotFoundException;

import com.asetune.ICounterController;
import com.asetune.IGuiController;
import com.asetune.cm.CounterSample;
import com.asetune.cm.CounterSetTemplates;
import com.asetune.cm.CounterSetTemplates.Type;
import com.asetune.cm.CountersModel;
import com.asetune.cm.sqlserver.gui.CmWaitStatsPanel;
import com.asetune.config.dict.MonTablesDictionary;
import com.asetune.config.dict.MonTablesDictionaryManager;
import com.asetune.config.dict.SqlServerWaitTypeDictionary;
import com.asetune.graph.TrendGraphDataPoint;
import com.asetune.graph.TrendGraphDataPoint.LabelType;
import com.asetune.gui.MainFrame;
import com.asetune.gui.TabularCntrPanel;
import com.asetune.utils.Configuration;
import com.asetune.utils.StringUtil;

/**
 * @author Goran Schwarz (goran_schwarz@hotmail.com)
 */
public class CmWaitStats
extends CountersModel
{
//	private static Logger        _logger          = Logger.getLogger(CmServiceMemory.class);
	private static final long    serialVersionUID = 1L;

	public static final String   CM_NAME          = CmWaitStats.class.getSimpleName();
	public static final String   SHORT_NAME       = "Wait Stats";
	public static final String   HTML_DESC        = 
		"<html>" +
		"<p>FIXME</p>" +
		"<br><br>" +
		"Table Background colors:" +
		"<ul>" +
		"    <li>DARK BEIGE          - Entry is skipped/discarded from LOCAL Graphs</li>" +
		"    <li>BEIGE               - Entry is skipped/discarded from TREND Graphs</li>" +
		"</ul>" +
		"</html>";

	public static final String   GROUP_NAME       = MainFrame.TCP_GROUP_SERVER;
	public static final String   GUI_ICON_FILE    = "images/"+CM_NAME+".png";

	public static final long     NEED_SRV_VERSION = 0;
	public static final long     NEED_CE_VERSION  = 0;

	public static final String[] MON_TABLES       = new String[] {"dm_os_wait_stats"};
	public static final String[] NEED_ROLES       = new String[] {};  // "VIEW SERVER STATE"
	public static final String[] NEED_CONFIG      = new String[] {};

	public static final String[] PCT_COLUMNS      = new String[] {"percentage"};
	public static final String[] DIFF_COLUMNS     = new String[] {
		"waiting_tasks_count",
		"wait_time_ms",
		"signal_wait_time_ms",
		"resource_ms"
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

		return new CmWaitStats(counterController, guiController);
	}

	public CmWaitStats(ICounterController counterController, IGuiController guiController)
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
		
		localInit();
	}


	//------------------------------------------------------------
	// Implementation
	//------------------------------------------------------------
//	public static final String  PROPKEY_sqlSkipFilterWaitTypes       = CM_NAME + ".sql.skip.filter.waitTypes";
//	public static final String  DEFAULT_sqlSkipFilterWaitTypes       = null;

	public static final String  PROPKEY_sqlSkipFilterEnabled         = CM_NAME + ".sql.skip.filter.enabled";
	public static final boolean DEFAULT_sqlSkipFilterEnabled         = false;

	
	@Override
	protected TabularCntrPanel createGui()
	{
		return new CmWaitStatsPanel(this);
	}

	@Override
	public String[] getDependsOnConfigForVersion(Connection conn, long srvVersion, boolean isAzure)
	{
		return NEED_CONFIG;
	}

	@Override
	public List<String> getPkForVersion(Connection conn, long srvVersion, boolean isAzure)
	{
		List <String> pkCols = new LinkedList<String>();

		pkCols.add("wait_type");

		return pkCols;
	}

	private static final int POS_wait_type         = 0;
	private static final int POS_SkipInLocalGraphs = 9;
	private static final int POS_SkipInTrendGraphs = 10;
	
	@Override
	public String getSqlForVersion(Connection conn, long srvVersion, boolean isAzure)
	{
		String dm_os_wait_stats = "dm_os_wait_stats";

		if (isAzure)
			dm_os_wait_stats = "dm_pdw_nodes_os_wait_stats";

		
		List<String> skipList = getDefaultLocalGraphsSkipList();
		String skipWaitTypes = "";
		if (skipList.size() > 0 && Configuration.getCombinedConfiguration().getBooleanProperty(CmWaitStats.PROPKEY_sqlSkipFilterEnabled, CmWaitStats.DEFAULT_sqlSkipFilterEnabled))
			skipWaitTypes = "  and wait_type NOT IN (" + StringUtil.toCommaStrQuoted("'", skipList) + ")";
			
		// ---------------------------------------------------------------------------------------
		// NOTE: If you change the below column order UPDATE THE ABOVE POS_* columns
		// ---------------------------------------------------------------------------------------
		String sql = ""
			+ "select \n"
			+ "    wait_type, \n"
			+ "    WaitClass = convert(varchar(30), null), \n"
			+ "    waiting_tasks_count, \n"
			+ "    wait_time_ms, \n"
			+ "    max_wait_time_ms, \n"
			+ "    signal_wait_time_ms, \n"
			+ "    resource_ms = wait_time_ms - signal_wait_time_ms, \n"
			+ "    percentage = convert(numeric(8,1), 100.0 * wait_time_ms / SUM(wait_time_ms) OVER()), \n"
			+ "    WaitTimePerCount = CASE WHEN waiting_tasks_count > 0 \n"
			+ "                           THEN convert(numeric(18,3), (wait_time_ms + 0.0) / waiting_tasks_count) \n"
			+ "                           ELSE convert(numeric(18,3), 0.0) \n"
			+ "                       END, \n"
			+ "    SkipInLocalGraphs = convert(bit, 1), \n"
			+ "    SkipInTrendGraphs = convert(bit, 1), \n"
			+ "    Description = convert(varchar(1500), '') \n"
			+ "from sys." + dm_os_wait_stats + " \n"
			+ "where waiting_tasks_count > 0 \n"
			+ skipWaitTypes
			+ "";

		return sql;
	}






	@Override
	public void addMonTableDictForVersion(Connection conn, long srvVersion, boolean isAzure)
	{
		try 
		{
			MonTablesDictionary mtd = MonTablesDictionaryManager.getInstance();
			mtd.addColumn("dm_os_wait_stats", "WaitTimePerCount",    "<html>" +
			                                                           "Wait time in ms per wait. <br>" +
			                                                           "<b>formula</b>: diff.wait_time_ms / diff.waiting_tasks_count<br>" +
			                                                         "</html>");
			mtd.addColumn("dm_os_wait_stats", "WaitClass",           "<html>" +
			                                                           "Class this wait_type is of. <br>" +
			                                                           "<b>formula</b>: Local dictionary (NOT-YET-IMPLEMENTED)<br>" +
			                                                         "</html>");
			mtd.addColumn("dm_os_wait_stats", "resource_ms",         "<html>" +
			                                                           "FIXME: Describe this. <br>" +
			                                                           "<b>formula</b>: wait_time_ms - signal_wait_time_ms <br>" +
			                                                         "</html>");
			mtd.addColumn("dm_os_wait_stats", "percentage",          "<html>" +
			                                                           "FIXME: Percentage of the total wait_time_ms. <br>" +
			                                                           "<b>formula ABS</b>:  100.0 * wait_time_ms / SUM(wait_time_ms) <br>" +
			                                                           "<b>formula DIFF</b>: 100.0 * diff.wait_time_ms / SUM(diff.wait_time_ms) <br>" +
			                                                           "<b>formula RATE</b>: same as DIFF <br>" +
			                                                         "</html>");
			mtd.addColumn("dm_os_wait_stats", "SkipInLocalGraphs",   "<html>If this 'wait_type' should be discarded from the <b>Local Graphs</b> to the right of the data table.</html>");
			mtd.addColumn("dm_os_wait_stats", "SkipInTrendGraphs",   "<html>If this 'wait_type' should be discarded from the <b>Trend Graphs</b> in the <b>Summary Tab</b>.</html>");
			mtd.addColumn("dm_os_wait_stats", "Description",         "<html>A Static description (originaly fetched from Microsoft Documentation)</html>");
		}
		catch (NameNotFoundException e) {/*ignore*/}
	}


	private Set<String> _inLocalGraphsSkipSet = new HashSet<>();
	private Set<String> _inTrendGraphsSkipSet = new HashSet<>();
	
	public static final String  PROPKEY_LocalGraphsSkipSet            = CM_NAME + ".local.graph.skip.set";
//	public static final String  DEFAULT_LocalGraphsSkipSet           = "OLEDB, SLEEP_TASK, LAZYWRITER_SLEEP, BROKER_TO_FLUSH, SQLTRACE_INCREMENTAL_FLUSH_SLEEP, REQUEST_FOR_DEADLOCK_SEARCH";
	public static final String  DEFAULT_LocalGraphsSkipSet           = getDefaultLocalGraphsSkipCsv();

	public static final String  PROPKEY_TrendGraphsSkipSet            = CM_NAME + ".trend.graph.skip.set";
//	public static final String  DEFAULT_TrendGraphsSkipSet           = "OLEDB, SLEEP_TASK, LAZYWRITER_SLEEP, BROKER_TO_FLUSH, SQLTRACE_INCREMENTAL_FLUSH_SLEEP, REQUEST_FOR_DEADLOCK_SEARCH";
	public static final String  DEFAULT_TrendGraphsSkipSet           = getDefaultTrendGraphsSkipCsv();

	private void localInit()
	{
		_inLocalGraphsSkipSet = new HashSet<String>(StringUtil.commaStrToList(Configuration.getCombinedConfiguration().getProperty( PROPKEY_LocalGraphsSkipSet, DEFAULT_LocalGraphsSkipSet)));
		_inTrendGraphsSkipSet = new HashSet<String>(StringUtil.commaStrToList(Configuration.getCombinedConfiguration().getProperty( PROPKEY_TrendGraphsSkipSet, DEFAULT_TrendGraphsSkipSet)));
	}
//	OLEDB, SLEEP_TASK, LAZYWRITER_SLEEP, BROKER_TO_FLUSH, SQLTRACE_INCREMENTAL_FLUSH_SLEEP, REQUEST_FOR_DEADLOCK_SEARCH

	public boolean isEventNameInLocalGraphSkipList(String eventName)
	{
		return _inLocalGraphsSkipSet.contains(eventName);
	}

	public boolean isEventNameInTrendGraphSkipList(String eventName)
	{
		return _inTrendGraphsSkipSet.contains(eventName);
	}

	public void resetLocalGraphSkipSet()
	{
		_inLocalGraphsSkipSet = new HashSet<String>(StringUtil.commaStrToList(DEFAULT_LocalGraphsSkipSet));
		Configuration tmpConf = Configuration.getInstance(Configuration.USER_TEMP);
		if (tmpConf != null)
		{
			tmpConf.setProperty(PROPKEY_LocalGraphsSkipSet, DEFAULT_LocalGraphsSkipSet);
			tmpConf.save();
		}
		for (int r=0; r<getRowCount(); r++)
		{
			String  waitType = (String)  getAbsValue(r, POS_wait_type);

			boolean skip = _inLocalGraphsSkipSet.contains(waitType);

			getDataCollection(DATA_ABS) .get(r).set(POS_SkipInLocalGraphs, skip);
			getDataCollection(DATA_DIFF).get(r).set(POS_SkipInLocalGraphs, skip);
			getDataCollection(DATA_RATE).get(r).set(POS_SkipInLocalGraphs, skip);
		}
		fireTableDataChanged();
	}

	public void resetTrendGraphSkipSet()
	{
		_inTrendGraphsSkipSet = new HashSet<String>(StringUtil.commaStrToList(DEFAULT_TrendGraphsSkipSet));
		Configuration tmpConf = Configuration.getInstance(Configuration.USER_TEMP);
		if (tmpConf != null)
		{
			tmpConf.setProperty(PROPKEY_TrendGraphsSkipSet, DEFAULT_TrendGraphsSkipSet);
			tmpConf.save();
		}
		for (int r=0; r<getRowCount(); r++)
		{
			String  waitType = (String)  getAbsValue(r, POS_wait_type);

			boolean skip = _inTrendGraphsSkipSet.contains(waitType);

			getDataCollection(DATA_ABS) .get(r).set(POS_SkipInTrendGraphs, skip);
			getDataCollection(DATA_DIFF).get(r).set(POS_SkipInTrendGraphs, skip);
			getDataCollection(DATA_RATE).get(r).set(POS_SkipInTrendGraphs, skip);
		}
		fireTableDataChanged();
	}

	/**
	 * Normally NO cells can be changed from a CM... but in this one we can SET what to discard/include in Local Graphs and Trend Graphs
	 */
	@Override
	public boolean isCellEditable(int row, int col)
	{
		if (col == POS_SkipInLocalGraphs) return true;
		if (col == POS_SkipInTrendGraphs) return true;

		return false;
	}

	/**
	 * Normally NO cells can be changed from a CM... but in this one we can SET what to discard/include in Local Graphs and Trend Graphs
	 */
	@Override
	public void setValueAt(Object val, int row, int col)
	{
		if (col == POS_SkipInLocalGraphs || col == POS_SkipInTrendGraphs)
		{
			if (val instanceof Boolean)
			{
				Boolean boolVal = (Boolean) val;
				String waitTypeStr = getValueAt(row, POS_wait_type) + "";
				
				if (col == POS_SkipInLocalGraphs)
				{
					if (boolVal)
						_inLocalGraphsSkipSet.add(waitTypeStr);
					else
						_inLocalGraphsSkipSet.remove(waitTypeStr);
				}
				if (col == POS_SkipInTrendGraphs)
				{
					if (boolVal)
						_inTrendGraphsSkipSet.add(waitTypeStr);
					else
						_inTrendGraphsSkipSet.remove(waitTypeStr);
				}

				
				getDataCollection(DATA_ABS) .get(row).set(col, boolVal);
				getDataCollection(DATA_DIFF).get(row).set(col, boolVal);
				getDataCollection(DATA_RATE).get(row).set(col, boolVal);
				
				//System.out.println("setValueAt(val='"+val+"', row="+col+", col="+col+"): waitTypeStr='"+waitTypeStr+"'.");
				
				Configuration tmpConf = Configuration.getInstance(Configuration.USER_TEMP);
				if (tmpConf != null)
				{
					if (col == POS_SkipInLocalGraphs) tmpConf.setProperty(PROPKEY_LocalGraphsSkipSet, StringUtil.toCommaStr(_inLocalGraphsSkipSet));
					if (col == POS_SkipInTrendGraphs) tmpConf.setProperty(PROPKEY_TrendGraphsSkipSet, StringUtil.toCommaStr(_inTrendGraphsSkipSet));
					
					tmpConf.save();
				}

				if (getTabPanel() != null)
				{
					getTabPanel().updateExtendedInfoPanel();
				}
			}
		}
	}

	/** 
	 * Fill in the Description column with data from
	 * SqlServerWaitNameDictionary.. transforms a wait_type -> text description
	 */
	@Override
	public void localCalculation(CounterSample newSample)
	{
		// Where are various columns located in the Vector 
		int pos_wait_type = -1, pos_Description = -1;
	
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
			else if (colName.equals("Description")) pos_Description = colId;
		}

		// Loop on all counter rows
		for (int rowId=0; rowId < newSample.getRowCount(); rowId++) 
		{
			Object o_wait_type  = newSample.getValueAt(rowId, pos_wait_type);

			if (o_wait_type instanceof String)
			{
				String desc = wd.getDescriptionPlain( (String)o_wait_type );

				if (desc != null)
					newSample.setValueAt(desc, rowId, pos_Description);

				// Set configuration: LocalGraphsSkipSet & TrendGraphsSkipSet
				boolean SkipInLocalGraphs = _inLocalGraphsSkipSet.contains(o_wait_type);
				boolean SkipInTrendGraphs = _inTrendGraphsSkipSet.contains(o_wait_type);

				newSample.setValueAt(SkipInLocalGraphs, rowId, POS_SkipInLocalGraphs);
				newSample.setValueAt(SkipInTrendGraphs, rowId, POS_SkipInTrendGraphs);
			}
		}
	}

	/** 
	 * Compute the WaitTimePerCount for DIFF values, and some others
	 */
	@Override
	public void localCalculation(CounterSample prevSample, CounterSample newSample, CounterSample diffData)
	{
		int pos_wait_time_ms        = -1;
		int pos_waiting_tasks_count = -1;
		int pos_WaitTimePerCount    = -1;
		int pos_signal_wait_time_ms = -1;
		int pos_resource_ms         = -1;
		int pos_percentage          = -1;

		// Find column Id's
		List<String> colNames = diffData.getColNames();
		if (colNames == null)
			return;

		for (int colId=0; colId < colNames.size(); colId++) 
		{
			String colName = colNames.get(colId);
			
			if      (colName.equals("WaitTimePerCount"))    pos_WaitTimePerCount     = colId;
			else if (colName.equals("wait_time_ms"))        pos_wait_time_ms        = colId;
			else if (colName.equals("waiting_tasks_count")) pos_waiting_tasks_count = colId;
			else if (colName.equals("signal_wait_time_ms")) pos_signal_wait_time_ms = colId;
			else if (colName.equals("resource_ms"))         pos_resource_ms         = colId;
			else if (colName.equals("percentage"))          pos_percentage          = colId;
		}

		// Get SUM for 'wait_time_ms' for THIS sample period
		long sum_wait_time_ms = 0;
		for (int rowId = 0; rowId < diffData.getRowCount(); rowId++)
		{
			sum_wait_time_ms += ((Number)diffData.getValueAt(rowId, pos_wait_time_ms)).longValue();
		}
		
		// Loop on all diffData rows
		for (int rowId = 0; rowId < diffData.getRowCount(); rowId++)
		{
			//-------------------------
			// WaitTimePerCount
			long wait_time_ms        = ((Number)diffData.getValueAt(rowId, pos_wait_time_ms       )).longValue();
			long waiting_tasks_count = ((Number)diffData.getValueAt(rowId, pos_waiting_tasks_count)).longValue();

			if (waiting_tasks_count > 0)
			{
				double calcWaitTimePerCount = wait_time_ms / (waiting_tasks_count * 1.0);

				BigDecimal newVal = new BigDecimal(calcWaitTimePerCount).setScale(3, BigDecimal.ROUND_HALF_EVEN);;
				diffData.setValueAt(newVal, rowId, pos_WaitTimePerCount);
			}
			else
				diffData.setValueAt(new BigDecimal(0), rowId, pos_WaitTimePerCount);

			//-------------------------
			// percentage
			if (sum_wait_time_ms > 0)
			{
				BigDecimal newVal = new BigDecimal( 100.0 * wait_time_ms / sum_wait_time_ms ).setScale(1, BigDecimal.ROUND_HALF_EVEN);;
				diffData.setValueAt(newVal, rowId, pos_percentage);
			}
			else
				diffData.setValueAt(new BigDecimal(0), rowId, pos_percentage);
			
			//-------------------------
			// resource_ms
			if (true)
			{
				long signal_wait_time_ms = ((Number)diffData.getValueAt(rowId, pos_signal_wait_time_ms)).longValue();

				long newVal = wait_time_ms - signal_wait_time_ms;
				diffData.setValueAt(newVal, rowId, pos_resource_ms);
			}
		}
	}
	
	
	@Override
	public String getToolTipTextOnTableCell(MouseEvent e, String colName, Object cellValue, int modelRow, int modelCol) 
	{
		if ("Description".equals(colName) )
		{
			int pos_wait_type = findColumn("wait_type");
			if (pos_wait_type >= 0)
			{
				Object cellVal = getValueAt(modelRow, pos_wait_type);
				if (cellVal instanceof String)
				{
					return SqlServerWaitTypeDictionary.getInstance().getDescriptionHtml((String) cellVal);
				}
			}
		}
		
		return super.getToolTipTextOnTableCell(e, colName, cellValue, modelRow, modelCol);
	}
	
	/** DEFAULT Skip List/Set for LOCAL GRAPHS, as a CommaSeparatedValues String */
	public static String getDefaultLocalGraphsSkipCsv()
	{
		return StringUtil.toCommaStr(getDefaultLocalGraphsSkipList());
	}

	/** DEFAULT Skip List/Set for TREND GRAPHS, as a CommaSeparatedValues String */
	public static String getDefaultTrendGraphsSkipCsv()
	{
		return StringUtil.toCommaStr(getDefaultTrendGraphsSkipList());
	}

	/** DEFAULT Skip List/Set for TREND GRAPHS as a List<String> */
	public static List<String> getDefaultTrendGraphsSkipList()
	{
		return getDefaultLocalGraphsSkipList();
	}

	/** DEFAULT Skip List/Set for LOCAL GRAPHS as a List<String> */
	public static List<String> getDefaultLocalGraphsSkipList()
	{
		ArrayList<String> list = new ArrayList<>();
		
		list.add("BROKER_EVENTHANDLER");
		list.add("BROKER_RECEIVE_WAITFOR");
		list.add("BROKER_TASK_STOP");
		list.add("BROKER_TO_FLUSH");
		list.add("BROKER_TRANSMITTER");
		list.add("CHECKPOINT_QUEUE");
		list.add("CHKPT");
		list.add("CLR_AUTO_EVENT");
		list.add("CLR_MANUAL_EVENT");
		list.add("CLR_SEMAPHORE");
		list.add("CXCONSUMER");

		// Maybe comment these four out if you have mirroring issues
		list.add("DBMIRROR_DBM_EVENT");
		list.add("DBMIRROR_EVENTS_QUEUE");
		list.add("DBMIRROR_WORKER_QUEUE");
		list.add("DBMIRRORING_CMD");

		list.add("DIRTY_PAGE_POLL");
		list.add("DISPATCHER_QUEUE_SEMAPHORE");
		list.add("EXECSYNC");
		list.add("FSAGENT");
		list.add("FT_IFTS_SCHEDULER_IDLE_WAIT");
		list.add("FT_IFTSHC_MUTEX");

		// Maybe comment these six out if you have AG issues
		list.add("HADR_CLUSAPI_CALL");
		list.add("HADR_FILESTREAM_IOMGR_IOCOMPLETION");
		list.add("HADR_LOGCAPTURE_WAIT");
		list.add("HADR_NOTIFICATION_DEQUEUE");
		list.add("HADR_TIMER_TASK");
		list.add("HADR_WORK_QUEUE");

		list.add("KSOURCE_WAKEUP");
		list.add("LAZYWRITER_SLEEP");
		list.add("LOGMGR_QUEUE");
		list.add("MEMORY_ALLOCATION_EXT");
		list.add("ONDEMAND_TASK_QUEUE");
		list.add("PARALLEL_REDO_DRAIN_WORKER");
		list.add("PARALLEL_REDO_LOG_CACHE");
		list.add("PARALLEL_REDO_TRAN_LIST");
		list.add("PARALLEL_REDO_WORKER_SYNC");
		list.add("PARALLEL_REDO_WORKER_WAIT_WORK");
		list.add("PREEMPTIVE_OS_FLUSHFILEBUFFERS");
		list.add("PREEMPTIVE_XE_GETTARGETSTATE");
		list.add("PWAIT_ALL_COMPONENTS_INITIALIZED");
		list.add("PWAIT_DIRECTLOGCONSUMER_GETNEXT");
		list.add("QDS_PERSIST_TASK_MAIN_LOOP_SLEEP");
		list.add("QDS_ASYNC_QUEUE");
		list.add("QDS_CLEANUP_STALE_QUERIES_TASK_MAIN_LOOP_SLEEP");
		list.add("QDS_SHUTDOWN_QUEUE");
		list.add("REDO_THREAD_PENDING_WORK");
		list.add("REQUEST_FOR_DEADLOCK_SEARCH");
		list.add("RESOURCE_QUEUE");
		list.add("SERVER_IDLE_CHECK");
		list.add("SLEEP_BPOOL_FLUSH");
		list.add("SLEEP_DBSTARTUP");
		list.add("SLEEP_DCOMSTARTUP");
		list.add("SLEEP_MASTERDBREADY");
		list.add("SLEEP_MASTERMDREADY");
		list.add("SLEEP_MASTERUPGRADED");
		list.add("SLEEP_MSDBSTARTUP");
		list.add("SLEEP_SYSTEMTASK");
		list.add("SLEEP_TASK");
		list.add("SLEEP_TEMPDBSTARTUP");
		list.add("SNI_HTTP_ACCEPT");
		list.add("SOS_WORK_DISPATCHER");
		list.add("SP_SERVER_DIAGNOSTICS_SLEEP");
		list.add("SQLTRACE_BUFFER_FLUSH");
		list.add("SQLTRACE_INCREMENTAL_FLUSH_SLEEP");
		list.add("SQLTRACE_WAIT_ENTRIES");
		list.add("VDI_CLIENT_OTHER");
		list.add("WAIT_FOR_RESULTS");
		list.add("WAITFOR");
		list.add("WAITFOR_TASKSHUTDOWN");
		list.add("WAIT_XTP_RECOVERY");
		list.add("WAIT_XTP_HOST_WAIT");
		list.add("WAIT_XTP_OFFLINE_CKPT_NEW_LOG");
		list.add("WAIT_XTP_CKPT_CLOSE");
		list.add("XE_DISPATCHER_JOIN");
		list.add("XE_DISPATCHER_WAIT");
		list.add("XE_TIMER_EVENT");

		list.sort(String.CASE_INSENSITIVE_ORDER);

		return list;
	}



	//---------------------------------------------------------------------------------
	//---------------------------------------------------------------------------------
	// Graph stuff
	//---------------------------------------------------------------------------------
	//---------------------------------------------------------------------------------
	
	public static final String   GRAPH_NAME_WAIT_TYPE_TIME   = "WaitTypeTime";
//	public static final String   GRAPH_NAME_WAIT_TYPE_COUNT  = "WaitTypeCount";
//	public static final String   GRAPH_NAME_WAIT_TYPE_TPW    = "WaitTypeTpw"; // Time Per Wait
//	public static final String   GRAPH_NAME_WAIT_CLASS_TIME  = "WaitClassTime"; 
//	public static final String   GRAPH_NAME_WAIT_CLASS_COUNT = "WaitClassCount"; 
//	public static final String   GRAPH_NAME_WAIT_CLASS_TPW   = "WaitClassTpw"; 

	public static final String   GRAPH_NAME_KNOWN_TOP_10_TIME  = "KnownTop10Time";
	public static final String   GRAPH_NAME_KNOWN_TOP_10_COUNT = "KnownTop10Count";
	

	private void addTrendGraphs()
	{
		addTrendGraph(GRAPH_NAME_WAIT_TYPE_TIME,
			"Server Wait, group by 'wait_type', 'wait_time_ms' Average", 	                   // Menu CheckBox text
			"Server Wait, group by 'wait_type', 'wait_time_ms' Average ("+GROUP_NAME+"->"+SHORT_NAME+")", // Label 
			TrendGraphDataPoint.Y_AXIS_SCALE_LABELS_MILLISEC,
			null,
			LabelType.Dynamic,
			TrendGraphDataPoint.Category.WAITS,
			false, // is Percent Graph
			false, // visible at start
			0,    // graph is valid from Server Version. 0 = All Versions; >0 = Valid from this version and above
			160);  // minimum height

//		addTrendGraph(GRAPH_NAME_WAIT_CLASS_TIME,
//			"Server Wait, group by 'WaitClass', 'wait_time_ms' Average", 	                   // Menu CheckBox text
//			"Server Wait, group by 'WaitClass', 'wait_time_ms' Average ("+GROUP_NAME+"->"+SHORT_NAME+")", // Label 
//			null,
//			LabelType.Dynamic,
//			TrendGraphDataPoint.Category.WAITS,
//			false, // is Percent Graph
//			false, // visible at start
//			0,    // graph is valid from Server Version. 0 = All Versions; >0 = Valid from this version and above
//			160);  // minimum height

		addTrendGraph(GRAPH_NAME_KNOWN_TOP_10_TIME,
			"Server Wait, Known Top 10 that could cause issues, by 'wait_time_ms'", 	                   // Menu CheckBox text
			"Server Wait, Known Top 10 that could cause issues, by 'wait_time_ms' ("+GROUP_NAME+"->"+SHORT_NAME+")", // Label 
			TrendGraphDataPoint.Y_AXIS_SCALE_LABELS_MILLISEC,
			new String[] {"LCK_M_X", "LCK_M_U", "WRITELOG", "LCK_M_IX", "LATCH_EX", "ASYNC_NETWORK_IO", "SOS_SCHEDULER_YIELD", "PAGEIOLATCH_SH", "LCK_M_S", "CXPACKET"},
			LabelType.Static,
			TrendGraphDataPoint.Category.WAITS,
			false, // is Percent Graph
			false, // visible at start
			0,    // graph is valid from Server Version. 0 = All Versions; >0 = Valid from this version and above
			0);  // minimum height

		addTrendGraph(GRAPH_NAME_KNOWN_TOP_10_COUNT,
			"Server Wait, Known Top 10 that could cause issues, by 'waiting_tasks_count'", 	                   // Menu CheckBox text
			"Server Wait, Known Top 10 that could cause issues, by 'waiting_tasks_count' ("+GROUP_NAME+"->"+SHORT_NAME+")", // Label 
			TrendGraphDataPoint.Y_AXIS_SCALE_LABELS_NORMAL,
			new String[] {"LCK_M_X", "LCK_M_U", "WRITELOG", "LCK_M_IX", "LATCH_EX", "ASYNC_NETWORK_IO", "SOS_SCHEDULER_YIELD", "PAGEIOLATCH_SH", "LCK_M_S", "CXPACKET"},
			LabelType.Static,
			TrendGraphDataPoint.Category.WAITS,
			false, // is Percent Graph
			false, // visible at start
			0,    // graph is valid from Server Version. 0 = All Versions; >0 = Valid from this version and above
			0);  // minimum height
	}
	
	@Override
	public void updateGraphData(TrendGraphDataPoint tgdp)
	{
//		long   srvVersion = getServerVersion();
		String graphName  = tgdp.getName();
		
		// ---- WAIT_TYPE_TIME
		if (GRAPH_NAME_WAIT_TYPE_TIME.equals(graphName))
		{
			// Start With: 1 "line" for every wait_type
			// but some are SKIPPED, so we need to make the array SHORTER at the end
			Double[] tmp_dArray = new Double[this.size()];
			String[] tmp_lArray = new String[tmp_dArray.length];
			
			int ai = 0; // Array Index
			for (int r=0; r<tmp_dArray.length; r++)
			{
				String wait_type           =          this.getRateString(r, "wait_type");
				int    waiting_tasks_count = ((Number)this.getDiffValue (r, "waiting_tasks_count")).intValue();

				// SKIP "unwanted" wiat_types
				if (_inTrendGraphsSkipSet.contains(wait_type))
					continue;

				// SKIP rows with NO waiting_tasks_count
				if (waiting_tasks_count <= 0)
					continue;

				tmp_lArray[ai] = wait_type.toLowerCase();
				tmp_dArray[ai] = this.getRateValueAsDouble(r, "wait_time_ms");
				
				// Increment ArrayIndex
				ai++;
			}

			// If we have NO records to report on... get out of here
			if (ai == 0)
				return;

			// Create new ARRAYS and copy 
			Double[] dArray = new Double[ai];
			String[] lArray = new String[ai];
			
			// Copy the "used parts" of tmp_xArray into xArray  
			System.arraycopy(tmp_dArray, 0, dArray, 0, ai);
			System.arraycopy(tmp_lArray, 0, lArray, 0, ai);

			// Set the values
			tgdp.setDataPoint(this.getTimestamp(), lArray, dArray);
		}

		// ---- KNOWN_TOP_10_TIME
		if (GRAPH_NAME_KNOWN_TOP_10_TIME.equals(graphName))
		{
			Double[] dArray = new Double[10];
			
			dArray[0] = this.getDiffValueAsDouble("LCK_M_X"            , "wait_time_ms");
			dArray[1] = this.getDiffValueAsDouble("LCK_M_U"            , "wait_time_ms");
			dArray[2] = this.getDiffValueAsDouble("WRITELOG"           , "wait_time_ms");
			dArray[3] = this.getDiffValueAsDouble("LCK_M_IX"           , "wait_time_ms");
			dArray[4] = this.getDiffValueAsDouble("LATCH_EX"           , "wait_time_ms");
			dArray[5] = this.getDiffValueAsDouble("ASYNC_NETWORK_IO"   , "wait_time_ms");
			dArray[6] = this.getDiffValueAsDouble("SOS_SCHEDULER_YIELD", "wait_time_ms");
			dArray[7] = this.getDiffValueAsDouble("PAGEIOLATCH_SH"     , "wait_time_ms");
			dArray[8] = this.getDiffValueAsDouble("LCK_M_S"            , "wait_time_ms");
			dArray[9] = this.getDiffValueAsDouble("CXPACKET"           , "wait_time_ms");

			tgdp.setDataPoint(this.getTimestamp(), dArray);
		}

		// ---- KNOWN_TOP_10_COUNT
		if (GRAPH_NAME_KNOWN_TOP_10_COUNT.equals(graphName))
		{
			Double[] dArray = new Double[10];
			
			dArray[0] = this.getDiffValueAsDouble("LCK_M_X"            , "waiting_tasks_count");
			dArray[1] = this.getDiffValueAsDouble("LCK_M_U"            , "waiting_tasks_count");
			dArray[2] = this.getDiffValueAsDouble("WRITELOG"           , "waiting_tasks_count");
			dArray[3] = this.getDiffValueAsDouble("LCK_M_IX"           , "waiting_tasks_count");
			dArray[4] = this.getDiffValueAsDouble("LATCH_EX"           , "waiting_tasks_count");
			dArray[5] = this.getDiffValueAsDouble("ASYNC_NETWORK_IO"   , "waiting_tasks_count");
			dArray[6] = this.getDiffValueAsDouble("SOS_SCHEDULER_YIELD", "waiting_tasks_count");
			dArray[7] = this.getDiffValueAsDouble("PAGEIOLATCH_SH"     , "waiting_tasks_count");
			dArray[8] = this.getDiffValueAsDouble("LCK_M_S"            , "waiting_tasks_count");
			dArray[9] = this.getDiffValueAsDouble("CXPACKET"           , "waiting_tasks_count");

			tgdp.setDataPoint(this.getTimestamp(), dArray);
		}
	}
}
