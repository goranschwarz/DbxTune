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

import java.awt.event.MouseEvent;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

import javax.naming.NameNotFoundException;

import org.apache.log4j.Logger;

import com.asetune.ICounterController;
import com.asetune.IGuiController;
import com.asetune.alarm.AlarmHandler;
import com.asetune.alarm.events.AlarmEvent;
import com.asetune.alarm.events.sqlserver.AlarmEventToxicWait;
import com.asetune.cm.CmSettingsHelper;
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
import com.asetune.sql.conn.DbxConnection;
import com.asetune.sql.conn.info.DbmsVersionInfo;
import com.asetune.sql.conn.info.DbmsVersionInfoSqlServer;
import com.asetune.utils.Configuration;
import com.asetune.utils.MathUtils;
import com.asetune.utils.StringUtil;

/**
 * @author Goran Schwarz (goran_schwarz@hotmail.com)
 */
public class CmWaitStats
extends CountersModel
{
	private static Logger        _logger          = Logger.getLogger(CmWaitStats.class);
	private static final long    serialVersionUID = 1L;

	public static final String   CM_NAME          = CmWaitStats.class.getSimpleName();
//	public static final String   SHORT_NAME       = "Wait Stats";
	public static final String   SHORT_NAME       = "Srv Wait";
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
	public static final boolean DEFAULT_sqlSkipFilterEnabled         = true;

//	// ---- TOXIC -- Wait types is from: https://github.com/BrentOzarULTD/SQL-Server-First-Responder-Kit/blob/dev/sp_BlitzFirst.sql, row: 2972
//	private static final String[] TOXIC_WAIT_TYPES = new String[] {
//			  "THREADPOOL"                          // means SQL Server ran out of worker threads, and new queries thought the SQL Server was frozen solid. During any occurrence of THREADPOOL, your SQL Server will feel like it’s locked up solid – although trickily, your CPU usage might be near zero. You can learn more about this in the THREADPOOL training video [https://training.brentozar.com/courses/1338135/lectures/30707006] (or here if your subscription started 2020 or before. [https://www.brentozar.com/training/mastering-server-tuning-wait-stats-live-3-days-recording/2-2-cpu-waits-threadpool/])
//			, "RESOURCE_SEMAPHORE"                  // means SQL Server ran out of available memory to run queries, and queries had to wait on available memory before they could even start. You can learn more about this in Query Plans: Memory Grants and High Row Estimates [https://www.brentozar.com/archive/2013/08/query-plans-what-happens-when-row-estimates-get-high/], and the RESOURCE_SEMAPHORE training video [https://www.brentozar.com/training/diagnosing-slow-sql-servers-wait-stats/wait-types-resource_semaphore-33m/]
//			, "RESOURCE_SEMAPHORE_QUERY_COMPILE"    // is a lot like RESOURCE_SEMAPHORE, but it means SQL Server didn’t even have enough memory to compile a query plan. This is usually only seen in two situations: "Underpowered servers: think 8-16GB RAM for heavy production workloads, or", "Really complex queries with dozens or hundreds of joins, like I describe in this post about ugly queries and this post as well – and you can hit these even on powerful servers, like 256GB+ memory. Finding the queries causing this is spectacularly hard, though – SQL Server doesn’t make it easy to analyze queries en masse looking for the highest compilation times. Solving it is usually more of a strategic thing: can we simplify our queries overall?"
//			, "CMEMTHREAD"                          // we’ve been seeing a lot of this in servers with a lot of CPU cores that still have their parallelism settings set at their defaults. To fix this issue: * Read my post about setting Cost Threshold for Parallelism and MAXDOP at reasonable defaults – which typically end up at Cost Threshold for Parallelism at 50, and then MAXDOP at around 8 or less depending on your hardware config.   * Look for queries reading a lot of data – run sp_BlitzCache @SortOrder = ‘reads’, and you’re probably going to find queries going parallel to read a lot of data that’s cached in RAM. Try tuning those queries, or tuning the indexes they use.   * After that, if you’re still having the problem, see Microsoft’s blog post on running SQL Servers with >8 cores per NUMA node. If you suspect that CMEMTHREAD is your server’s largest problem, and you meet the symptoms described in that KB article, open up a support ticket with Microsoft to be safe rather than just enabling this trace flag. It’s only $500, and they can give you a double check confirmation that this trace flag makes sense for you
//			, "IO_QUEUE_LIMIT"                      // occurs when your database has too many asynchronous IOs pending. (in Azure SQL DB, this means your database is getting throttled)
//			, "IO_RETRY"                            // a read or write failed due to insufficient resources, and we’re waiting for a retry
//			, "LOG_RATE_GOVERNOR"                   // in Azure SQL DB, this means your database is getting throttled. Your delete/update/insert work simply can’t go any faster due to the limits on your instance size. Before you spend more on a larger instance, read this post: https://www.brentozar.com/archive/2019/02/theres-a-bottleneck-in-azure-sql-db-storage-throughput/
//			, "POOL_LOG_RATE_GOVERNOR"              // see LOG_RATE_GOVERNOR
//			, "PREEMPTIVE_DEBUG"                    // someone probably accidentally hit the DEBUG button in SSMS rather than Execute
//			, "RESMGR_THROTTLED"                    // in Azure SQL DB, this means a new request has come in, but it’s throttled based on the GROUP_MAX_REQUESTS setting
//			, "SE_REPL_CATCHUP_THROTTLE"            // in Azure SQL DB, we’re waiting for the secondary replicas to catch up
//			, "SE_REPL_COMMIT_ACK"                  // in Azure SQL DB, we’re waiting for the secondary replicas to catch up
//			, "SE_REPL_COMMIT_TURN"                 // in Azure SQL DB, we’re waiting for the secondary replicas to catch up
//			, "SE_REPL_ROLLBACK_ACK"                // in Azure SQL DB, we’re waiting for the secondary replicas to catch up
//			, "SE_REPL_SLOW_SECONDARY_THROTTLE"     // in Azure SQL DB, we’re waiting for the secondary replicas to catch up
//			};

	// Toxic Wait Types are initialized later on, when we know what TYPE of SQL Server we are connecting to. (OnPrem or some Azure variant)
	private List<String> _toxicWaitTypes;

	
	@Override
	protected TabularCntrPanel createGui()
	{
		return new CmWaitStatsPanel(this);
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

		pkCols.add("wait_type");

		return pkCols;
	}

	private static final int POS_wait_type         = 0;
	private static final int POS_SkipInLocalGraphs = 9;
	private static final int POS_SkipInTrendGraphs = 10;
	
	@Override
	public String getSqlForVersion(DbxConnection conn, DbmsVersionInfo versionInfo)
	{
		DbmsVersionInfoSqlServer ssVersionInfo = (DbmsVersionInfoSqlServer) versionInfo;

		String dm_os_wait_stats = "dm_os_wait_stats";

		if (ssVersionInfo.isAzureSynapseAnalytics())
			dm_os_wait_stats = "dm_pdw_nodes_os_wait_stats";

		
		List<String> skipList = getDefaultLocalGraphsSkipList();
		String skipWaitTypes = "";
		if (skipList.size() > 0 && Configuration.getCombinedConfiguration().getBooleanProperty(CmWaitStats.PROPKEY_sqlSkipFilterEnabled, CmWaitStats.DEFAULT_sqlSkipFilterEnabled))
			skipWaitTypes = "  and wait_type NOT IN (" + StringUtil.toCommaStrQuoted("'", skipList) + ")";

		// Initialize Toxic Wait types based on the Edition we are connecting to.
		// ---- TOXIC -- Wait types is from: https://github.com/BrentOzarULTD/SQL-Server-First-Responder-Kit/blob/dev/sp_BlitzFirst.sql, row: 2972
		_toxicWaitTypes = new ArrayList<>();
		_toxicWaitTypes.add("THREADPOOL"                      );    // means SQL Server ran out of worker threads, and new queries thought the SQL Server was frozen solid. During any occurrence of THREADPOOL, your SQL Server will feel like it’s locked up solid – although trickily, your CPU usage might be near zero. You can learn more about this in the THREADPOOL training video [https://training.brentozar.com/courses/1338135/lectures/30707006] (or here if your subscription started 2020 or before. [https://www.brentozar.com/training/mastering-server-tuning-wait-stats-live-3-days-recording/2-2-cpu-waits-threadpool/])
		_toxicWaitTypes.add("RESOURCE_SEMAPHORE"              );    // means SQL Server ran out of available memory to run queries, and queries had to wait on available memory before they could even start. You can learn more about this in Query Plans: Memory Grants and High Row Estimates [https://www.brentozar.com/archive/2013/08/query-plans-what-happens-when-row-estimates-get-high/], and the RESOURCE_SEMAPHORE training video [https://www.brentozar.com/training/diagnosing-slow-sql-servers-wait-stats/wait-types-resource_semaphore-33m/]
		_toxicWaitTypes.add("RESOURCE_SEMAPHORE_QUERY_COMPILE");    // is a lot like RESOURCE_SEMAPHORE, but it means SQL Server didn’t even have enough memory to compile a query plan. This is usually only seen in two situations: "Underpowered servers: think 8-16GB RAM for heavy production workloads, or", "Really complex queries with dozens or hundreds of joins, like I describe in this post about ugly queries and this post as well – and you can hit these even on powerful servers, like 256GB+ memory. Finding the queries causing this is spectacularly hard, though – SQL Server doesn’t make it easy to analyze queries en masse looking for the highest compilation times. Solving it is usually more of a strategic thing: can we simplify our queries overall?"
		_toxicWaitTypes.add("CMEMTHREAD"                      );    // we’ve been seeing a lot of this in servers with a lot of CPU cores that still have their parallelism settings set at their defaults. To fix this issue: * Read my post about setting Cost Threshold for Parallelism and MAXDOP at reasonable defaults – which typically end up at Cost Threshold for Parallelism at 50, and then MAXDOP at around 8 or less depending on your hardware config.   * Look for queries reading a lot of data – run sp_BlitzCache @SortOrder = ‘reads’, and you’re probably going to find queries going parallel to read a lot of data that’s cached in RAM. Try tuning those queries, or tuning the indexes they use.   * After that, if you’re still having the problem, see Microsoft’s blog post on running SQL Servers with >8 cores per NUMA node. If you suspect that CMEMTHREAD is your server’s largest problem, and you meet the symptoms described in that KB article, open up a support ticket with Microsoft to be safe rather than just enabling this trace flag. It’s only $500, and they can give you a double check confirmation that this trace flag makes sense for you
		_toxicWaitTypes.add("IO_RETRY"                        );    // a read or write failed due to insufficient resources, and we’re waiting for a retry
		_toxicWaitTypes.add("PREEMPTIVE_DEBUG"                );    // someone probably accidentally hit the DEBUG button in SSMS rather than Execute

		if (ssVersionInfo.isAnyAzure())
		{
			_toxicWaitTypes.add("IO_QUEUE_LIMIT"                  );    // occurs when your database has too many asynchronous IOs pending. (in Azure SQL DB, this means your database is getting throttled)
			_toxicWaitTypes.add("LOG_RATE_GOVERNOR"               );    // in Azure SQL DB, this means your database is getting throttled. Your delete/update/insert work simply can’t go any faster due to the limits on your instance size. Before you spend more on a larger instance, read this post: https://www.brentozar.com/archive/2019/02/theres-a-bottleneck-in-azure-sql-db-storage-throughput/
			_toxicWaitTypes.add("POOL_LOG_RATE_GOVERNOR"          );    // see LOG_RATE_GOVERNOR
			_toxicWaitTypes.add("RESMGR_THROTTLED"                );    // in Azure SQL DB, this means a new request has come in, but it’s throttled based on the GROUP_MAX_REQUESTS setting
			_toxicWaitTypes.add("SE_REPL_CATCHUP_THROTTLE"        );    // in Azure SQL DB, we’re waiting for the secondary replicas to catch up
			_toxicWaitTypes.add("SE_REPL_COMMIT_ACK"              );    // in Azure SQL DB, we’re waiting for the secondary replicas to catch up
			_toxicWaitTypes.add("SE_REPL_COMMIT_TURN"             );    // in Azure SQL DB, we’re waiting for the secondary replicas to catch up
			_toxicWaitTypes.add("SE_REPL_ROLLBACK_ACK"            );    // in Azure SQL DB, we’re waiting for the secondary replicas to catch up
			_toxicWaitTypes.add("SE_REPL_SLOW_SECONDARY_THROTTLE" );    // in Azure SQL DB, we’re waiting for the secondary replicas to catch up
		}

		// ---------------------------------------------------------------------------------------
		// NOTE: If you change the below column order UPDATE THE ABOVE POS_* columns
		// ---------------------------------------------------------------------------------------
		String sql = ""
			+ "select /* ${cmCollectorName} */ \n"
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
	public void addMonTableDictForVersion(DbxConnection conn, DbmsVersionInfo versionInfo)
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
			else if (colName.equals("Description")) pos_Description = colId;
		}

		// Loop on all counter rows
		for (int rowId=0; rowId < newSample.getRowCount(); rowId++)
		{
			Object o_wait_type  = newSample.getValueAt(rowId, pos_wait_type);

			if (o_wait_type instanceof String)
			{
				String wait_type = (String)o_wait_type;

				String desc      = wd.getDescriptionPlain    ( wait_type );
				String className = wd.getWaitClassForWaitType( wait_type );

				if (desc != null)
					newSample.setValueAt(desc, rowId, pos_Description);

				if (className != null)
					newSample.setValueAt(className, rowId, pos_WaitClass);

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
		list.add("PREEMPTIVE_OS_FLUSHFILEBUFFERS");       // maybe removed in SQL-Server 2019
		list.add("PREEMPTIVE_HADR_LEASE_MECHANISM");
		list.add("PREEMPTIVE_SP_SERVER_DIAGNOSTICS");
		list.add("PREEMPTIVE_OS_LIBRARYOPS");
		list.add("PREEMPTIVE_OS_COMOPS");
		list.add("PREEMPTIVE_OS_CRYPTOPS");
		list.add("PREEMPTIVE_OS_PIPEOPS");
		list.add("PREEMPTIVE_OS_AUTHENTICATIONOPS");
		list.add("PREEMPTIVE_OS_GENERICOPS");
		list.add("PREEMPTIVE_OS_VERIFYTRUST");
		list.add("PREEMPTIVE_OS_FILEOPS");
		list.add("PREEMPTIVE_OS_DEVICEOPS");
		list.add("PREEMPTIVE_OS_QUERYREGISTRY");
		list.add("PREEMPTIVE_OS_WRITEFILE");
		list.add("PREEMPTIVE_OS_WRITEFILEGATHER");
		list.add("PREEMPTIVE_XE_CALLBACKEXECUTE");
		list.add("PREEMPTIVE_XE_DISPATCHER");
		list.add("PREEMPTIVE_XE_GETTARGETSTATE");
		list.add("PREEMPTIVE_XE_SESSIONCOMMIT");
		list.add("PREEMPTIVE_XE_TARGETINIT");
		list.add("PREEMPTIVE_XE_TARGETFINALIZE");
		list.add("PWAIT_ALL_COMPONENTS_INITIALIZED");
		list.add("PWAIT_DIRECTLOGCONSUMER_GETNEXT");
		list.add("PWAIT_EXTENSIBILITY_CLEANUP_TASK");     // SQL-Server 2019
		list.add("QDS_PERSIST_TASK_MAIN_LOOP_SLEEP");
		list.add("QDS_ASYNC_QUEUE");
		list.add("QDS_CLEANUP_STALE_QUERIES_TASK_MAIN_LOOP_SLEEP");
		list.add("QDS_SHUTDOWN_QUEUE");                   // maybe removed in SQL-Server 2019
		list.add("REDO_THREAD_PENDING_WORK");             // maybe removed in SQL-Server 2019
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
		list.add("STARTUP_DEPENDENCY_MANAGER");
		list.add("VDI_CLIENT_OTHER");                     // maybe removed in SQL-Server 2019
		list.add("WAIT_FOR_RESULTS");
		list.add("WAITFOR");
		list.add("WAITFOR_TASKSHUTDOWN");
		list.add("WAIT_XTP_HOST_WAIT");
		list.add("WAIT_XTP_OFFLINE_CKPT_NEW_LOG");
		list.add("WAIT_XTP_CKPT_CLOSE");
		list.add("WAIT_XTP_RECOVERY");
		list.add("XE_BUFFERMGR_ALLPROCESSED_EVENT");
		list.add("XE_DISPATCHER_JOIN");
		list.add("XE_DISPATCHER_WAIT");
		list.add("XE_LIVE_TARGET_TVF");
		list.add("XE_TIMER_EVENT");

		// The above list was grabbed from: Glenn Berry - SQL Server 2019 Diagnostic Information Queries
		// https://www.sqlskills.com/blogs/glenn/
		// https://www.dropbox.com/s/p1urkrq5v01cuw3/SQL%20Server%202019%20Diagnostic%20Information%20Queries.sql?dl=0

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
	
	public static final String   GRAPH_NAME_TOXIC_TIME         = "ToxicTime";
	public static final String   GRAPH_NAME_TOXIC_COUNT        = "ToxicCount";
	public static final String   GRAPH_NAME_TOXIC_TPW          = "ToxicTpw"; // Time Per Wait

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

//FIXME; // test/check the below. Also if we should have Alarms on "some" of the Toxic
		addTrendGraph(GRAPH_NAME_TOXIC_TIME,
			"Server Toxic Wait Types, by 'wait_time_ms'", 	                   // Menu CheckBox text
			"Server Toxic Wait Types, by 'wait_time_ms' ("+GROUP_NAME+"->"+SHORT_NAME+")", // Label 
			TrendGraphDataPoint.Y_AXIS_SCALE_LABELS_MILLISEC,
			null,
			LabelType.Dynamic,
			TrendGraphDataPoint.Category.WAITS,
			false, // is Percent Graph
			true,  // visible at start
			0,     // graph is valid from Server Version. 0 = All Versions; >0 = Valid from this version and above
			0);    // minimum height

		addTrendGraph(GRAPH_NAME_TOXIC_COUNT,
			"Server Toxic Wait Types, by 'waiting_tasks_count'", 	                   // Menu CheckBox text
			"Server Toxic Wait Types, by 'waiting_tasks_count' ("+GROUP_NAME+"->"+SHORT_NAME+")", // Label 
			TrendGraphDataPoint.Y_AXIS_SCALE_LABELS_NORMAL,
			null,
			LabelType.Dynamic,
			TrendGraphDataPoint.Category.WAITS,
			false, // is Percent Graph
			false, // visible at start
			0,     // graph is valid from Server Version. 0 = All Versions; >0 = Valid from this version and above
			0);    // minimum height

		addTrendGraph(GRAPH_NAME_TOXIC_TPW,
			"Server Toxic Wait Types, by 'WaitTimePerCount'", 	                   // Menu CheckBox text
			"Server Toxic Wait Types, by 'WaitTimePerCount' ("+GROUP_NAME+"->"+SHORT_NAME+")", // Label 
			TrendGraphDataPoint.Y_AXIS_SCALE_LABELS_MILLISEC,
			null,
			LabelType.Dynamic,
			TrendGraphDataPoint.Category.WAITS,
			false, // is Percent Graph
			false, // visible at start
			0,     // graph is valid from Server Version. 0 = All Versions; >0 = Valid from this version and above
			0);    // minimum height
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

		// ---- TOXIC -- Wait types is from: https://github.com/BrentOzarULTD/SQL-Server-First-Responder-Kit/blob/dev/sp_BlitzFirst.sql, row: 2972
		if (GRAPH_NAME_TOXIC_TIME.equals(graphName) && _toxicWaitTypes != null && !_toxicWaitTypes.isEmpty())
		{
			Double[] dArray = new Double[_toxicWaitTypes.size()];
			String[] lArray = new String[dArray.length];
			
			for (int i=0; i<_toxicWaitTypes.size(); i++)
			{
				String waitType = _toxicWaitTypes.get(i);

				dArray[i] = this.getDiffValueAsDouble(waitType, "wait_time_ms");
				lArray[i] = waitType;
			}
			
			tgdp.setDataPoint(this.getTimestamp(), lArray, dArray);
		}

		// ---- TOXIC -- Wait types is from: https://github.com/BrentOzarULTD/SQL-Server-First-Responder-Kit/blob/dev/sp_BlitzFirst.sql, row: 2972
		if (GRAPH_NAME_TOXIC_COUNT.equals(graphName) && _toxicWaitTypes != null && !_toxicWaitTypes.isEmpty())
		{
			Double[] dArray = new Double[_toxicWaitTypes.size()];
			String[] lArray = new String[dArray.length];
			
			for (int i=0; i<_toxicWaitTypes.size(); i++)
			{
				String waitType = _toxicWaitTypes.get(i);

				dArray[i] = this.getDiffValueAsDouble(waitType, "waiting_tasks_count");
				lArray[i] = waitType;
			}
			
			tgdp.setDataPoint(this.getTimestamp(), lArray, dArray);
		}

		// ---- TOXIC -- Wait types is from: https://github.com/BrentOzarULTD/SQL-Server-First-Responder-Kit/blob/dev/sp_BlitzFirst.sql, row: 2972
		if (GRAPH_NAME_TOXIC_TPW.equals(graphName) && _toxicWaitTypes != null && !_toxicWaitTypes.isEmpty())
		{
			Double[] dArray = new Double[_toxicWaitTypes.size()];
			String[] lArray = new String[dArray.length];
			
			for (int i=0; i<_toxicWaitTypes.size(); i++)
			{
				String waitType = _toxicWaitTypes.get(i);

				dArray[i] = this.getDiffValueAsDouble(waitType, "WaitTimePerCount");
				lArray[i] = waitType;
			}
			
			tgdp.setDataPoint(this.getTimestamp(), lArray, dArray);
		}
	}

	@Override
	public boolean isGraphDataHistoryEnabled(String name)
	{
		// ENABLED for the following graphs
		if (GRAPH_NAME_TOXIC_TIME .equals(name)) return true;  // Used locally
		if (GRAPH_NAME_TOXIC_COUNT.equals(name)) return true;  // Used locally
		if (GRAPH_NAME_TOXIC_TPW  .equals(name)) return true;  // Used locally

		// default: DISABLED
		return false;
	}
	@Override
	public int getGraphDataHistoryTimeInterval(String name)
	{
		// Keep interval: default is 60 minutes
		return super.getGraphDataHistoryTimeInterval(name);
	}

	//--------------------------------------------------------------------------------------
	//--------------------------------------------------------------------------------------
	//-- Alarm Handling
	//--------------------------------------------------------------------------------------
	//--------------------------------------------------------------------------------------
	@Override
	public void sendAlarmRequest()
	{
		if ( ! hasDiffData() )
			return;
		
		if ( ! AlarmHandler.hasInstance() )
			return;

		AlarmHandler alarmHandler = AlarmHandler.getInstance();
		
		CountersModel cm = this;

		boolean debugPrint = Configuration.getCombinedConfiguration().getBooleanProperty("sendAlarmRequest.debug", _logger.isDebugEnabled());

		
		//-------------------------------------------------------
		// WaitTime_RESOURCE_SEMAPHORE   (waiting for "memory_grants" / "workspace memory")
		//-------------------------------------------------------
		if (isSystemAlarmsForColumnEnabledAndInTimeRange("WaitTime_RESOURCE_SEMAPHORE"))
		{
			String wait_type = "RESOURCE_SEMAPHORE";
			int wait_time_ms = cm.getDiffValueAsInteger(wait_type, "wait_time_ms", -1);
			int threshold    = Configuration.getCombinedConfiguration().getIntProperty(PROPKEY_alarm_WaitTime_RESOURCE_SEMAPHORE, DEFAULT_alarm_WaitTime_RESOURCE_SEMAPHORE);

			if (debugPrint || _logger.isDebugEnabled())
				System.out.println("##### sendAlarmRequest(" + cm.getName() + "): threshold=" + threshold + ", wait_type='" + wait_type + "', wait_time_ms='" + wait_time_ms);

			if (wait_time_ms > threshold)
			{
				int    rowId               = cm.getDiffRowIdForPkValue(wait_type);
				int    waiting_tasks_count = cm.getDiffValueAsInteger(rowId, "waiting_tasks_count", -1);
				double waitTimePerCount    = cm.getDiffValueAsDouble (rowId, "WaitTimePerCount"   , -1d);
				
				// Round to 1 decimal point
				waitTimePerCount = MathUtils.round(waitTimePerCount, 1);

				String extendedDescText = "DIFF Counters: \n"          + cm.toTextTableString(DATA_DIFF, rowId) + "\n\n"
				                        + "ABS Counters: \n"           + cm.toTextTableString(DATA_ABS , rowId);
				String extendedDescHtml = "<b>DIFF Counters: </b><br>" + cm.toHtmlTableString(DATA_DIFF, rowId, true, false, false) + "<br><br>"
				                        + "<b>ABS Counters:  </b><br>" + cm.toHtmlTableString(DATA_ABS , rowId, true, false, false);

				// Get a small graph about the usage for the last hour
				extendedDescHtml += "<br><br>" + getGraphDataHistoryAsHtmlImage(GRAPH_NAME_TOXIC_TIME);
				extendedDescHtml += "<br><br>" + getGraphDataHistoryAsHtmlImage(GRAPH_NAME_TOXIC_COUNT);
				extendedDescHtml += "<br><br>" + getGraphDataHistoryAsHtmlImage(GRAPH_NAME_TOXIC_TPW);

				// And CPU and Memory Usage (from CmSummary)
				CountersModel cmSummary = getCounterController().getCmByName(CmSummary.CM_NAME);
				extendedDescHtml += "<br><br>" + cmSummary.getGraphDataHistoryAsHtmlImage(CmSummary.GRAPH_NAME_AA_CPU);
//				extendedDescHtml += "<br><br>" + cmSummary.getGraphDataHistoryAsHtmlImage(CmSummary.GRAPH_NAME_TARGET_AND_TOTAL_MEM_MB);

				// and some info from CmMemoryGrantsSum
				CountersModel cmMemoryGrantsSum = getCounterController().getCmByName(CmMemoryGrantsSum.CM_NAME);
				extendedDescHtml += "<br><br>" + cmMemoryGrantsSum.getGraphDataHistoryAsHtmlImage(CmMemoryGrantsSum.GRAPH_NAME_GRANTED_MEMORY_SUM);
				extendedDescHtml += "<br><br>" + cmMemoryGrantsSum.getGraphDataHistoryAsHtmlImage(CmMemoryGrantsSum.GRAPH_NAME_GRANTEE_WAITER_COUNT);
				extendedDescHtml += "<br><br>" + cmMemoryGrantsSum.getGraphDataHistoryAsHtmlImage(CmMemoryGrantsSum.GRAPH_NAME_GRANTED_MEMORY_PCT);

				// Possibly getting info from CmMemoryGrants
				// This can help to show what SQL Statement(s) are involved 
				CountersModel cmMemoryGrants = getCounterController().getCmByName(CmMemoryGrants.CM_NAME);
				if (cmMemoryGrants.hasAbsData())
				{
					int rowc = cmMemoryGrants.getAbsRowCount();
					extendedDescHtml += "<br><br><b>Current Memory Grants: (" + rowc + " records, one table for each record)</b><br>";
					for (int r=0; r<rowc; r++)
					{
						extendedDescHtml += "<span style='background-color: yellow'>Row " + (r+1) + " of " + rowc + "</span> <br>" 
						                 + cmMemoryGrants.toHtmlTableString(DATA_ABS , r, true, false, false) + "<br>";
					}
				}
				else
				{
					if (cmMemoryGrants.isActive())
						extendedDescHtml += "<br><br><b>Current Memory Grants: (CmMemoryGrants does NOT contain any data)</b><br>";
					else
						extendedDescHtml += "<br><br><b>Current Memory Grants: (CmMemoryGrants is NOT enabled)</b><br>";
				}

				// Create the alarm
				AlarmEvent ae = new AlarmEventToxicWait(cm, threshold, AlarmEvent.Severity.WARNING, AlarmEvent.ServiceState.UP, wait_type, waiting_tasks_count, wait_time_ms, waitTimePerCount);

				ae.setExtendedDescription(extendedDescText, extendedDescHtml);
				
				alarmHandler.addAlarm( ae );
			}
		}
		
		//-------------------------------------------------------
		// WaitTime_THREADPOOL 
		//-------------------------------------------------------
		if (isSystemAlarmsForColumnEnabledAndInTimeRange("WaitTime_THREADPOOL"))
		{
			String wait_type = "THREADPOOL";
			int wait_time_ms = cm.getDiffValueAsInteger(wait_type, "wait_time_ms", -1);
			int threshold    = Configuration.getCombinedConfiguration().getIntProperty(PROPKEY_alarm_WaitTime_THREADPOOL, DEFAULT_alarm_WaitTime_THREADPOOL);

			if (debugPrint || _logger.isDebugEnabled())
				System.out.println("##### sendAlarmRequest(" + cm.getName() + "): threshold=" + threshold + ", wait_type='" + wait_type + "', wait_time_ms='" + wait_time_ms);

			if (wait_time_ms > threshold)
			{
				int    rowId               = cm.getDiffRowIdForPkValue(wait_type);
				int    waiting_tasks_count = cm.getDiffValueAsInteger(rowId, "waiting_tasks_count", -1);
				double waitTimePerCount    = cm.getDiffValueAsDouble (rowId, "WaitTimePerCount"   , -1d);

				// Round to 1 decimal point
				waitTimePerCount = MathUtils.round(waitTimePerCount, 1);

				String extendedDescText = "DIFF Counters: \n"          + cm.toTextTableString(DATA_DIFF, rowId) + "\n\n"
				                        + "ABS Counters: \n"           + cm.toTextTableString(DATA_ABS , rowId);
				String extendedDescHtml = "<b>DIFF Counters: </b><br>" + cm.toHtmlTableString(DATA_DIFF, rowId, true, false, false) + "<br><br>"
				                        + "<b>ABS Counters:  </b><br>" + cm.toHtmlTableString(DATA_ABS , rowId, true, false, false);

				// Get a small graph about the usage for the last hour
				extendedDescHtml += "<br><br>" + getGraphDataHistoryAsHtmlImage(GRAPH_NAME_TOXIC_TIME);
				extendedDescHtml += "<br><br>" + getGraphDataHistoryAsHtmlImage(GRAPH_NAME_TOXIC_COUNT);
				extendedDescHtml += "<br><br>" + getGraphDataHistoryAsHtmlImage(GRAPH_NAME_TOXIC_TPW);

				// And CPU and WorkerThreadsUsage (from CmSummary)
				CountersModel cmSummary = getCounterController().getCmByName(CmSummary.CM_NAME);
				extendedDescHtml = "<br><br>" + cmSummary.getGraphDataHistoryAsHtmlImage(CmSummary.GRAPH_NAME_AA_CPU);
				extendedDescHtml = "<br><br>" + cmSummary.getGraphDataHistoryAsHtmlImage(CmSummary.GRAPH_NAME_WORKER_THREAD_USAGE);


				// Create the alarm
				AlarmEvent ae = new AlarmEventToxicWait(cm, threshold, AlarmEvent.Severity.WARNING, AlarmEvent.ServiceState.AFFECTED, wait_type, waiting_tasks_count, wait_time_ms, waitTimePerCount);

				ae.setExtendedDescription(extendedDescText, extendedDescHtml);
				
				alarmHandler.addAlarm( ae );
			}
		}
	}
//	  "THREADPOOL"                          // means SQL Server ran out of worker threads, and new queries thought the SQL Server was frozen solid. During any occurrence of THREADPOOL, your SQL Server will feel like it’s locked up solid – although trickily, your CPU usage might be near zero. You can learn more about this in the THREADPOOL training video [https://training.brentozar.com/courses/1338135/lectures/30707006] (or here if your subscription started 2020 or before. [https://www.brentozar.com/training/mastering-server-tuning-wait-stats-live-3-days-recording/2-2-cpu-waits-threadpool/])
//	, "RESOURCE_SEMAPHORE"                  // means SQL Server ran out of available memory to run queries, and queries had to wait on available memory before they could even start. You can learn more about this in Query Plans: Memory Grants and High Row Estimates [https://www.brentozar.com/archive/2013/08/query-plans-what-happens-when-row-estimates-get-high/], and the RESOURCE_SEMAPHORE training video [https://www.brentozar.com/training/diagnosing-slow-sql-servers-wait-stats/wait-types-resource_semaphore-33m/]
//	, "RESOURCE_SEMAPHORE_QUERY_COMPILE"    // is a lot like RESOURCE_SEMAPHORE, but it means SQL Server didn’t even have enough memory to compile a query plan. This is usually only seen in two situations: "Underpowered servers: think 8-16GB RAM for heavy production workloads, or", "Really complex queries with dozens or hundreds of joins, like I describe in this post about ugly queries and this post as well – and you can hit these even on powerful servers, like 256GB+ memory. Finding the queries causing this is spectacularly hard, though – SQL Server doesn’t make it easy to analyze queries en masse looking for the highest compilation times. Solving it is usually more of a strategic thing: can we simplify our queries overall?"
//	, "CMEMTHREAD"                          // we’ve been seeing a lot of this in servers with a lot of CPU cores that still have their parallelism settings set at their defaults. To fix this issue: * Read my post about setting Cost Threshold for Parallelism and MAXDOP at reasonable defaults – which typically end up at Cost Threshold for Parallelism at 50, and then MAXDOP at around 8 or less depending on your hardware config.   * Look for queries reading a lot of data – run sp_BlitzCache @SortOrder = ‘reads’, and you’re probably going to find queries going parallel to read a lot of data that’s cached in RAM. Try tuning those queries, or tuning the indexes they use.   * After that, if you’re still having the problem, see Microsoft’s blog post on running SQL Servers with >8 cores per NUMA node. If you suspect that CMEMTHREAD is your server’s largest problem, and you meet the symptoms described in that KB article, open up a support ticket with Microsoft to be safe rather than just enabling this trace flag. It’s only $500, and they can give you a double check confirmation that this trace flag makes sense for you
//	, "IO_QUEUE_LIMIT"                      // occurs when your database has too many asynchronous IOs pending. (in Azure SQL DB, this means your database is getting throttled)
//	, "IO_RETRY"                            // a read or write failed due to insufficient resources, and we’re waiting for a retry
//	, "LOG_RATE_GOVERNOR"                   // in Azure SQL DB, this means your database is getting throttled. Your delete/update/insert work simply can’t go any faster due to the limits on your instance size. Before you spend more on a larger instance, read this post: https://www.brentozar.com/archive/2019/02/theres-a-bottleneck-in-azure-sql-db-storage-throughput/
//	, "POOL_LOG_RATE_GOVERNOR"              // see LOG_RATE_GOVERNOR
//	, "PREEMPTIVE_DEBUG"                    // someone probably accidentally hit the DEBUG button in SSMS rather than Execute
//	, "RESMGR_THROTTLED"                    // in Azure SQL DB, this means a new request has come in, but it’s throttled based on the GROUP_MAX_REQUESTS setting
//	, "SE_REPL_CATCHUP_THROTTLE"            // in Azure SQL DB, we’re waiting for the secondary replicas to catch up
//	, "SE_REPL_COMMIT_ACK"                  // in Azure SQL DB, we’re waiting for the secondary replicas to catch up
//	, "SE_REPL_COMMIT_TURN"                 // in Azure SQL DB, we’re waiting for the secondary replicas to catch up
//	, "SE_REPL_ROLLBACK_ACK"                // in Azure SQL DB, we’re waiting for the secondary replicas to catch up
//	, "SE_REPL_SLOW_SECONDARY_THROTTLE"     // in Azure SQL DB, we’re waiting for the secondary replicas to catch up
//	};

	public static final String  PROPKEY_alarm_WaitTime_RESOURCE_SEMAPHORE  = CM_NAME + ".alarm.system.if.wait_time_ms.RESOURCE_SEMAPHORE.gt";
	public static final int     DEFAULT_alarm_WaitTime_RESOURCE_SEMAPHORE  = 20_000; // 20 seconds

	public static final String  PROPKEY_alarm_WaitTime_THREADPOOL          = CM_NAME + ".alarm.system.if.wait_time_ms.THREADPOOL.gt";
	public static final int     DEFAULT_alarm_WaitTime_THREADPOOL          = 20_000; // 20 seconds
	
	@Override
	public List<CmSettingsHelper> getLocalAlarmSettings()
	{
		Configuration conf = Configuration.getCombinedConfiguration();
		List<CmSettingsHelper> list = new ArrayList<>();

		CmSettingsHelper.Type isAlarmSwitch = CmSettingsHelper.Type.IS_ALARM_SWITCH;
		
		list.add(new CmSettingsHelper("WaitTime_RESOURCE_SEMAPHORE", isAlarmSwitch, PROPKEY_alarm_WaitTime_RESOURCE_SEMAPHORE, Integer.class, conf.getIntProperty(PROPKEY_alarm_WaitTime_RESOURCE_SEMAPHORE, DEFAULT_alarm_WaitTime_RESOURCE_SEMAPHORE), DEFAULT_alarm_WaitTime_RESOURCE_SEMAPHORE, "If 'wait_time_ms' for 'RESOURCE_SEMAPHORE' is greater than ## then send 'AlarmEventFIXME'." ));
		list.add(new CmSettingsHelper("WaitTime_THREADPOOL"        , isAlarmSwitch, PROPKEY_alarm_WaitTime_THREADPOOL        , Integer.class, conf.getIntProperty(PROPKEY_alarm_WaitTime_THREADPOOL        , DEFAULT_alarm_WaitTime_THREADPOOL        ), DEFAULT_alarm_WaitTime_THREADPOOL        , "If 'wait_time_ms' for 'THREADPOOL' is greater than ## then send 'AlarmEventFIXME'." ));

		return list;
	}
}
