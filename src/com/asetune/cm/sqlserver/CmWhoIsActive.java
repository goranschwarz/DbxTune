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
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

import javax.naming.NameNotFoundException;

import org.apache.commons.text.StringEscapeUtils;
import org.apache.log4j.Logger;

import com.asetune.ICounterController;
import com.asetune.IGuiController;
import com.asetune.cm.CmSettingsHelper;
import com.asetune.cm.CounterSample;
import com.asetune.cm.CounterSetTemplates;
import com.asetune.cm.CounterSetTemplates.Type;
import com.asetune.cm.CountersModel;
import com.asetune.cm.IgnoreSqlWarning;
import com.asetune.cm.sqlserver.gui.CmWhoIsActivePanel;
import com.asetune.config.dict.MonTablesDictionary;
import com.asetune.config.dict.MonTablesDictionaryManager;
import com.asetune.gui.MainFrame;
import com.asetune.gui.TabularCntrPanel;
import com.asetune.gui.swing.ColumnHeaderPropsEntry;
import com.asetune.sql.ResultSetMetaDataCached;
import com.asetune.sql.conn.DbxConnection;
import com.asetune.sql.conn.info.DbmsVersionInfo;
import com.asetune.utils.Configuration;
import com.asetune.utils.SqlServerUtils;
import com.asetune.utils.StringUtil;
import com.asetune.utils.TimeUtils;

/**
 * @author Goran Schwarz (goran_schwarz@hotmail.com)
 */
public class CmWhoIsActive
extends CountersModel
{
	private static Logger        _logger          = Logger.getLogger(CmWhoIsActive.class);
	private static final long    serialVersionUID = 1L;

	public static final String   CM_NAME          = CmWhoIsActive.class.getSimpleName();
	public static final String   SHORT_NAME       = "WhoIsActive";
	public static final String   HTML_DESC        = 
		"<html>" +
			"<p>Simply execute the procedure 'sp_WhoIsActive' from Adam Machanic.</p>" +
			"<br>" +
			"To get more information about this procedure, just Google 'sp_WhoIsActive' and you will know where to dowload it.<br>" +
			"Or go directly to: <br>" +
			"<ul>" + //possibly "href='OPEN-IN-EXTERNAL-BROWSER::https://whoisactive.com" but then we need to install a LISTENER on the Focusable ToolTip...
			"    <li>Home   - <a href='https://whoisactive.com'>https://whoisactive.com/</a></li>" +
			"    <li>Github - <a href='https://github.com/amachanic/sp_whoisactive'>https://github.com/amachanic/sp_whoisactive</a></li>" +
			"</ul>" +
			"<br>" + 
			"Table Background colors:" +
			"<ul>" +
			"    <li>YELLOW      - SPID is a System Processes</li>" +
//			"    <li>GREEN       - SPID is Executing(running) or are in the Run Queue Awaiting a time slot to Execute (runnable)</li>" +
			"    <li>ORANGE      - SPID has an open transaction.</li>" +
			"    <li>LIGHT_GREEN - SPID is Suspended waiting for something, soon it will probably go into running or runnable or finish.</li>" +
			"    <li>PINK        - SPID is Blocked by some other SPID that holds a Lock on a database object Table, Page or Row. This is the Lock Victim.</li>" +
			"    <li>RED         - SPID is Blocking other SPID's from running, this SPID is Responsible or the Root Cause of a Blocking Lock.</li>" +
			"</ul>" +
			// The below just to fill out to 1000 chars so we get "focusable tooltip"
			"&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;" +
			"&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;" +
			"&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;" +
			"&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;" +
		"</html>";

	public static final String   GROUP_NAME       = MainFrame.TCP_GROUP_SERVER;
	public static final String   GUI_ICON_FILE    = "images/"+CM_NAME+".png";

	public static final long     NEED_SRV_VERSION = 0;
	public static final long     NEED_CE_VERSION  = 0;

	public static final String[] MON_TABLES       = new String[] { CM_NAME };
	public static final String[] NEED_ROLES       = new String[] {};
	public static final String[] NEED_CONFIG      = new String[] {};

	public static final String[] PCT_COLUMNS      = new String[] {
		"percent_complete"
	};
	public static final String[] DIFF_COLUMNS     = new String[] {
		"CPU",
		"tempdb_allocations",
		"tempdb_current",
		"reads",
		"writes",
		"context_switches",
		"physical_io",
		"physical_reads",
		"used_memory"
//		"open_tran_count"
	};
	
	// Below is execution with ALL options

	// 1> exec master.dbo.sp_WhoIsActive 
	// 2>      @format_output        = 0
	// 3>     ,@show_system_spids    = 0 -- NO new Columns
	// 4>     ,@get_memory_info      = 1 -- adds column '', '', ''
	// 5>     ,@get_locks            = 1 -- adds column 'locks'
	// 6>     ,@get_transaction_info = 1
	// 7>     ,@get_outer_command    = 1
	// 8>     ,@get_plans            = 1
	// 9>     ,@find_block_leaders   = 1
	// 10>     ,@get_additional_info  = 1
	// 11>     ,@show_sleeping_spids  = 1
	// 12>     ,@get_task_info        = 2
	
	// RS> Col# Label                 JDBC Type Name              Guessed DBMS type Source Table
	// RS> ---- --------------------- --------------------------- ----------------- ------------
	// RS> 1    session_id            java.sql.Types.SMALLINT     smallint          -none-      
	// RS> 2    sql_text              java.sql.Types.NVARCHAR     nvarchar(max)     -none-      
	// RS> 3    sql_command           java.sql.Types.NVARCHAR     nvarchar(max)     -none-      
	// RS> 4    login_name            java.sql.Types.NVARCHAR     nvarchar(128)     -none-      
	// RS> 5    wait_info             java.sql.Types.NVARCHAR     nvarchar(4000)    -none-      
	// RS> 6    tasks                 java.sql.Types.SMALLINT     smallint          -none-      
	// RS> 7    tran_log_writes       java.sql.Types.NVARCHAR     nvarchar(4000)    -none-      
	// RS> 8    CPU                   java.sql.Types.BIGINT       bigint            -none-      
	// RS> 9    tempdb_allocations    java.sql.Types.BIGINT       bigint            -none-      
	// RS> 10   tempdb_current        java.sql.Types.BIGINT       bigint            -none-      
	// RS> 11   blocking_session_id   java.sql.Types.SMALLINT     smallint          -none-      
	// RS> 12   blocked_session_count java.sql.Types.SMALLINT     smallint          -none-      
	// RS> 13   reads                 java.sql.Types.BIGINT       bigint            -none-      
	// RS> 14   writes                java.sql.Types.BIGINT       bigint            -none-      
	// RS> 15   context_switches      java.sql.Types.BIGINT       bigint            -none-      
	// RS> 16   physical_io           java.sql.Types.BIGINT       bigint            -none-      
	// RS> 17   physical_reads        java.sql.Types.BIGINT       bigint            -none-      
	// RS> 18   query_plan            java.sql.Types.LONGNVARCHAR xml               -none-      
	// RS> 19   locks                 java.sql.Types.LONGNVARCHAR xml               -none-      
	// RS> 20   used_memory           java.sql.Types.BIGINT       bigint            -none-      
	// RS> 21   max_used_memory       java.sql.Types.BIGINT       bigint            -none-      
	// RS> 22   requested_memory      java.sql.Types.BIGINT       bigint            -none-      
	// RS> 23   granted_memory        java.sql.Types.BIGINT       bigint            -none-      
	// RS> 24   status                java.sql.Types.VARCHAR      varchar(30)       -none-      
	// RS> 25   tran_start_time       java.sql.Types.TIMESTAMP    datetime          -none-      
	// RS> 26   implicit_tran         java.sql.Types.NVARCHAR     nvarchar(3)       -none-      
	// RS> 27   open_tran_count       java.sql.Types.SMALLINT     smallint          -none-      
	// RS> 28   percent_complete      java.sql.Types.REAL         real              -none-      
	// RS> 29   host_name             java.sql.Types.NVARCHAR     nvarchar(128)     -none-      
	// RS> 30   database_name         java.sql.Types.NVARCHAR     nvarchar(128)     -none-      
	// RS> 31   program_name          java.sql.Types.NVARCHAR     nvarchar(128)     -none-      
	// RS> 32   additional_info       java.sql.Types.LONGNVARCHAR xml               -none-      
	// RS> 33   memory_info           java.sql.Types.LONGNVARCHAR xml               -none-      
	// RS> 34   start_time            java.sql.Types.TIMESTAMP    datetime          -none-      
	// RS> 35   login_time            java.sql.Types.TIMESTAMP    datetime          -none-      
	// RS> 36   request_id            java.sql.Types.INTEGER      int               -none-      
	// RS> 37   collection_time       java.sql.Types.TIMESTAMP    datetime          -none-      
	

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

		return new CmWhoIsActive(counterController, guiController);
	}

	public CmWhoIsActive(ICounterController counterController, IGuiController guiController)
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
		
		localInit();
		
		CounterSetTemplates.register(this);
	}


	//------------------------------------------------------------
	// Implementation
	//------------------------------------------------------------
	private static final String  PROP_PREFIX                     = CM_NAME;

	public static final String  PROPKEY_sample_systemThreads     = PROP_PREFIX + ".sample.systemThreads";
	public static final boolean DEFAULT_sample_systemThreads     = false;

	public static final String  PROPKEY_sample_memoryInfo        = PROP_PREFIX + ".sample.memoryInfo";
	public static final boolean DEFAULT_sample_memoryInfo        = false;

	public static final String  PROPKEY_sample_locks             = PROP_PREFIX + ".sample.locks";
	public static final boolean DEFAULT_sample_locks             = false;

	public static final String  PROPKEY_sample_transactionInfo   = PROP_PREFIX + ".sample.transactionInfo";
	public static final boolean DEFAULT_sample_transactionInfo   = false;

	public static final String  PROPKEY_sample_outerCommand      = PROP_PREFIX + ".sample.outerCommand";
	public static final boolean DEFAULT_sample_outerCommand      = false;

	public static final String  PROPKEY_sample_plans             = PROP_PREFIX + ".sample.plans";
	public static final boolean DEFAULT_sample_plans             = false;

	public static final String  PROPKEY_sample_blockLeaders      = PROP_PREFIX + ".sample.blockLeaders";
	public static final boolean DEFAULT_sample_blockLeaders      = false;

	public static final String  PROPKEY_sample_additionalInfo    = PROP_PREFIX + ".sample.additionalInfo";
	public static final boolean DEFAULT_sample_additionalInfo    = false;

	public static final String  PROPKEY_sample_sleepingSpids     = PROP_PREFIX + ".sample.sleepingSpids";
	public static final boolean DEFAULT_sample_sleepingSpids     = false;

	public static final String  PROPKEY_sample_taskInfo          = PROP_PREFIX + ".sample.taskInfo";
	public static final boolean DEFAULT_sample_taskInfo          = false;

	private static final String FAKE_COL_DURATION = "duration";

	@Override
	protected void registerDefaultValues()
	{
		super.registerDefaultValues();

		Configuration.registerDefaultValue(PROPKEY_sample_systemThreads  , DEFAULT_sample_systemThreads  );
		Configuration.registerDefaultValue(PROPKEY_sample_memoryInfo     , DEFAULT_sample_memoryInfo     );
		Configuration.registerDefaultValue(PROPKEY_sample_locks          , DEFAULT_sample_locks          );
		Configuration.registerDefaultValue(PROPKEY_sample_transactionInfo, DEFAULT_sample_transactionInfo);
		Configuration.registerDefaultValue(PROPKEY_sample_outerCommand   , DEFAULT_sample_outerCommand   );
		Configuration.registerDefaultValue(PROPKEY_sample_plans          , DEFAULT_sample_plans          );
		Configuration.registerDefaultValue(PROPKEY_sample_blockLeaders   , DEFAULT_sample_blockLeaders   );
		Configuration.registerDefaultValue(PROPKEY_sample_additionalInfo , DEFAULT_sample_additionalInfo );
		Configuration.registerDefaultValue(PROPKEY_sample_sleepingSpids  , DEFAULT_sample_sleepingSpids  );
		Configuration.registerDefaultValue(PROPKEY_sample_taskInfo       , DEFAULT_sample_taskInfo       );
	}
	
	/** Used by the: Create 'Offline Session' Wizard */
	@Override
	public List<CmSettingsHelper> getLocalSettings()
	{
		Configuration conf = Configuration.getCombinedConfiguration();
		List<CmSettingsHelper> list = new ArrayList<>();
		
		list.add(new CmSettingsHelper("Show System Spid's"      , PROPKEY_sample_systemThreads   , Boolean.class, conf.getBooleanProperty(PROPKEY_sample_systemThreads   , DEFAULT_sample_systemThreads  ), DEFAULT_sample_systemThreads  , CmWhoIsActivePanel.TOOLTIP_sample_systemThreads   ));
		list.add(new CmSettingsHelper("Show Memory Info"        , PROPKEY_sample_memoryInfo      , Boolean.class, conf.getBooleanProperty(PROPKEY_sample_memoryInfo      , DEFAULT_sample_memoryInfo     ), DEFAULT_sample_memoryInfo     , CmWhoIsActivePanel.TOOLTIP_sample_memoryInfo      ));
		list.add(new CmSettingsHelper("Show Lock Info"          , PROPKEY_sample_locks           , Boolean.class, conf.getBooleanProperty(PROPKEY_sample_locks           , DEFAULT_sample_locks          ), DEFAULT_sample_locks          , CmWhoIsActivePanel.TOOLTIP_sample_locks           ));
		list.add(new CmSettingsHelper("Show Transaction Info"   , PROPKEY_sample_transactionInfo , Boolean.class, conf.getBooleanProperty(PROPKEY_sample_transactionInfo , DEFAULT_sample_transactionInfo), DEFAULT_sample_transactionInfo, CmWhoIsActivePanel.TOOLTIP_sample_transactionInfo ));
		list.add(new CmSettingsHelper("Show Outer Command"      , PROPKEY_sample_outerCommand    , Boolean.class, conf.getBooleanProperty(PROPKEY_sample_outerCommand    , DEFAULT_sample_outerCommand   ), DEFAULT_sample_outerCommand   , CmWhoIsActivePanel.TOOLTIP_sample_outerCommand    ));
		list.add(new CmSettingsHelper("Show Execution Plan"     , PROPKEY_sample_plans           , Boolean.class, conf.getBooleanProperty(PROPKEY_sample_plans           , DEFAULT_sample_plans          ), DEFAULT_sample_plans          , CmWhoIsActivePanel.TOOLTIP_sample_plans           ));
		list.add(new CmSettingsHelper("Show Block Leader"       , PROPKEY_sample_blockLeaders    , Boolean.class, conf.getBooleanProperty(PROPKEY_sample_blockLeaders    , DEFAULT_sample_blockLeaders   ), DEFAULT_sample_blockLeaders   , CmWhoIsActivePanel.TOOLTIP_sample_blockLeaders    ));
		list.add(new CmSettingsHelper("Show Additional Info"    , PROPKEY_sample_additionalInfo  , Boolean.class, conf.getBooleanProperty(PROPKEY_sample_additionalInfo  , DEFAULT_sample_additionalInfo ), DEFAULT_sample_additionalInfo , CmWhoIsActivePanel.TOOLTIP_sample_additionalInfo  ));
		list.add(new CmSettingsHelper("Show Sleeping Spid's"    , PROPKEY_sample_sleepingSpids   , Boolean.class, conf.getBooleanProperty(PROPKEY_sample_sleepingSpids   , DEFAULT_sample_sleepingSpids  ), DEFAULT_sample_sleepingSpids  , CmWhoIsActivePanel.TOOLTIP_sample_sleepingSpids   ));
		list.add(new CmSettingsHelper("Show Extended Task Info" , PROPKEY_sample_taskInfo        , Boolean.class, conf.getBooleanProperty(PROPKEY_sample_taskInfo        , DEFAULT_sample_taskInfo       ), DEFAULT_sample_taskInfo       , CmWhoIsActivePanel.TOOLTIP_sample_taskInfo        ));

		return list;
	}

	private void addTrendGraphs()
	{
	}

	@Override
	public int getDefaultDataSource()
	{
		return DATA_ABS;
	}

	@Override
	protected TabularCntrPanel createGui()
	{
		return new CmWhoIsActivePanel(this);
	}

	@Override
	public String[] getDependsOnConfigForVersion(DbxConnection conn, DbmsVersionInfo versionInfo)
	{
		return NEED_CONFIG;
	}

	@Override
	public void addMonTableDictForVersion(DbxConnection conn, DbmsVersionInfo versionInfo)
	{
		try 
		{
			MonTablesDictionary mtd = MonTablesDictionaryManager.getInstance();
			mtd.addTable(CM_NAME, "sp_WhoIsActive");

			mtd.addColumn(CM_NAME,  "duration",                 "<html>For an active request, time the query has been running<br>For a sleeping session, time since the last batch completed</html>");

			mtd.addColumn(CM_NAME,  "session_id",               "<html>Session ID (a.k.a. SPID)</html>");
			mtd.addColumn(CM_NAME,  "dd hh:mm:ss.mss",          "<html>For an active request, time the query has been running<br>For a sleeping session, time since the last batch completed</html>");
			mtd.addColumn(CM_NAME,  "dd hh:mm:ss.mss (avg)",    "<html>(Requires @get_avg_time option)<BR>How much time has the active portion of the query taken in the past, on average?</html>");
			mtd.addColumn(CM_NAME,  "physical_io",              "<html>Shows the number of physical I/Os, for active requests</html>");
			mtd.addColumn(CM_NAME,  "reads",                    "<html>For an active request, number of reads done for the current query<BR>For a sleeping session, total number of reads done over the lifetime of the session</html>");
			mtd.addColumn(CM_NAME,  "physical_reads",           "<html>For an active request, number of physical reads done for the current query<br>For a sleeping session, total number of physical reads done over the lifetime of the session</html>");
			mtd.addColumn(CM_NAME,  "writes",                   "<html>For an active request, number of writes done for the current query<br>For a sleeping session, total number of writes done over the lifetime of the session</html>");
			mtd.addColumn(CM_NAME,  "tempdb_allocations",       "<html>For an active request, number of TempDB writes done for the current query<br>For a sleeping session, total number of TempDB writes done over the lifetime of the session</html>");
			mtd.addColumn(CM_NAME,  "tempdb_current",           "<html>For an active request, number of TempDB pages currently allocated for the query<BR>For a sleeping session, number of TempDB pages currently allocated for the session</html>");
			mtd.addColumn(CM_NAME,  "CPU",                      "<html>For an active request, total CPU time consumed by the current query<BR>For a sleeping session, total CPU time consumed over the lifetime of the session</html>");
			mtd.addColumn(CM_NAME,  "context_switches",         "<html>Shows the number of context switches, for active requests</html>");
			mtd.addColumn(CM_NAME,  "used_memory",              "<html>(Requires @get_memory_info = 1)<BR>For an active request, total memory consumption for the current query<BR>For a sleeping session, total current memory consumption</html>");
			mtd.addColumn(CM_NAME,  "max_used_memory",          "<html>(Requires @get_memory_info = 1)<BR>For an active request, the maximum amount of memory that has been used during<BR>processing up to the point of observation for the current query</html>");
			mtd.addColumn(CM_NAME,  "requested_memory",         "<html>(Requires @get_memory_info = 1)<BR>For an active request, the amount of memory requested by the query processor for hash, sort, and parallelism operations</html>");
			mtd.addColumn(CM_NAME,  "granted_memory",           "<html>(Requires @get_memory_info = 1)<BR>For an active request, the amount of memory granted to the query processor for hash, sort, and parallelism operations</html>");
			mtd.addColumn(CM_NAME,  "physical_io_delta",        "<html>(Requires @delta_interval option)<BR>Difference between the number of physical I/Os reported on the first and second collections.<BR>If the request started after the first collection, the value will be NULL</html>");
			mtd.addColumn(CM_NAME,  "reads_delta",              "<html>(Requires @delta_interval option)<BR>Difference between the number of reads reported on the first and second collections.<BR>If the request started after the first collection, the value will be NULL</html>");
			mtd.addColumn(CM_NAME,  "physical_reads_delta",     "<html>(Requires @delta_interval option)<BR>Difference between the number of physical reads reported on the first and second collections.<BR>If the request started after the first collection, the value will be NULL</html>");
			mtd.addColumn(CM_NAME,  "writes_delta",             "<html>(Requires @delta_interval option)<BR>Difference between the number of writes reported on the first and second collections.<BR>If the request started after the first collection, the value will be NULL</html>");
			mtd.addColumn(CM_NAME,  "tempdb_allocations_delta", "<html>(Requires @delta_interval option)<BR>Difference between the number of TempDB writes reported on the first and second collections.<BR>If the request started after the first collection, the value will be NULL</html>");
			mtd.addColumn(CM_NAME,  "tempdb_current_delta",     "<html>(Requires @delta_interval option)<BR>Difference between the number of allocated TempDB pages reported on the first and second collection.<BR>If the request started after the first collection, the value will be NULL</html>");
			mtd.addColumn(CM_NAME,  "CPU_delta",                "<html>(Requires @delta_interval option)<BR>Difference between the CPU time reported on the first and second collections.<BR>If the request started after the first collection, the value will be NULL</html>");
			mtd.addColumn(CM_NAME,  "context_switches_delta",   "<html>(Requires @delta_interval option)<BR>Difference between the context switches count reported on the first and second collections<BR>If the request started after the first collection, the value will be NULL</html>");
			mtd.addColumn(CM_NAME,  "used_memory_delta",        "<html>(Requires @delta_interval option)<BR>Difference between the memory usage reported on the first and second collections<BR>If the request started after the first collection, the value will be NULL</html>");
			mtd.addColumn(CM_NAME,  "max_used_memory_delta",    "<html>(Requires @delta_interval option)<BR>Difference between the max memory usage reported on the first and second collections<BR>If the request started after the first collection, the value will be NULL</html>");
			mtd.addColumn(CM_NAME,  "tasks",                    "<html>Number of worker tasks currently allocated, for active requests</html>");
			mtd.addColumn(CM_NAME,  "status",                   "<html>Activity status for the session (running, sleeping, etc)</html>");
			mtd.addColumn(CM_NAME,  "wait_info",                "<html>Aggregates wait information, in the following format: <code>(Ax: Bms/Cms/Dms)E</code>"
			                                                        + "A is the number of waiting tasks currently waiting on resource type E. B/C/D are wait times, in milliseconds. If only one thread is waiting, its wait time will be shown as B.<BR>"
			                                                        + "If two tasks are waiting, each of their wait times will be shown (B/C). If three or more tasks are waiting, the minimum, average, and maximum wait times will be shown (B/C/D).<BR>"
			                                                        + "If wait type E is a page latch wait and the page is of a <i>special</i> type (e.g. PFS, GAM, SGAM), the page type will be identified.<BR>"
			                                                        + "If wait type E is CXPACKET, CXCONSUMER, CXSYNC_PORT, or CXSYNC_CONSUMER the nodeId from the query plan will be identified<BR>"
			                                                        + " </html>");
			mtd.addColumn(CM_NAME,  "locks",                    "<html>(Requires @get_locks option)<BR>Aggregates lock information, in XML format.<BR>The lock XML includes the lock mode, locked object, and aggregates the number of requests.<BR>Attempts are made to identify locked objects by name</html>");
			mtd.addColumn(CM_NAME,  "tran_start_time",          "<html>(Requires @get_transaction_info option)<BR>Date and time that the first transaction opened by a session caused a transaction log write to occur.</html>");
			mtd.addColumn(CM_NAME,  "tran_log_writes",          "<html>(Requires @get_transaction_info option)<BR>"
			                                                        + "Aggregates transaction log write information, in the following format: <code>A:wB (C kB)</code><BR>"
			                                                        + "A is a database that has been touched by an active transaction<BR>"
			                                                        + "B is the number of log writes that have been made in the database as a result of the transaction<BR>"
			                                                        + "C is the number of log kilobytes consumed by the log records<BR>"
			                                                        + "</html>");
			mtd.addColumn(CM_NAME,  "implicit_tran",            "<html>(Requires @get_transaction_info option)<BR>For active read-write transactions, returns on <b>ON</b> the transaction has been started as a result of the session using the implicit_transactions option, or <b>OFF</b> otherwise.</html>");
			mtd.addColumn(CM_NAME,  "open_tran_count",          "<html>Shows the number of open transactions the session has open</html>");
			mtd.addColumn(CM_NAME,  "sql_command",              "<html>(Requires @get_outer_command option)<BR>Shows the <i>outer</i> SQL command, i.e. the text of the batch or RPC sent to the server, if available</html>");
			mtd.addColumn(CM_NAME,  "sql_text",                 "<html>Shows the SQL text for active requests or the last statement executed for sleeping sessions, if available in either case.<BR>"
			                                                        + "If @get_full_inner_text option is set, shows the full text of the batch.<BR>"
			                                                        + "Otherwise, shows only the active statement within the batch.<BR>"
			                                                        + "If the query text is locked, a special timeout message will be sent, in the following format: <code>&lt;timeout_exceeded /&gt;</code><BR>"
			                                                        + "If an error occurs, an error message will be sent, in the following format: <code>&lt;error message=\"message\" /&gt</code><BR>"
			                                                        + "</html>");
			mtd.addColumn(CM_NAME,  "query_plan",               "<html>(Requires @get_plans option)<BR>"
			                                                        + "Shows the query plan for the request, if available.<BR>"
			                                                        + "If the plan is locked, a special timeout message will be sent, in the following format: <code>&lt;timeout_exceeded /&gt;</code><BR>"
			                                                        + "If an error occurs, an error message will be sent, in the following format: <code>&lt;error message=\"message\" /&gt;</code><BR>"
			                                                        + "</html>");
			mtd.addColumn(CM_NAME,  "blocking_session_id",      "<html>When applicable, shows the blocking SPID</html>");
			mtd.addColumn(CM_NAME,  "blocked_session_count",    "<html>(Requires @find_block_leaders option)<BR>The total number of SPIDs blocked by this session, all the way down the blocking chain.</html>");
			mtd.addColumn(CM_NAME,  "percent_complete",         "<html>When applicable, shows the percent complete (e.g. for backups, restores, and some rollbacks)</html>");
			mtd.addColumn(CM_NAME,  "host_name",                "<html>Shows the host name for the connection</html>");
			mtd.addColumn(CM_NAME,  "login_name",               "<html>Shows the login name for the connection</html>");
			mtd.addColumn(CM_NAME,  "database_name",            "<html>Shows the connected database</html>");
			mtd.addColumn(CM_NAME,  "program_name",             "<html>Shows the reported program/application name<BR>NOTE: If it's a SQL Agent job, the 0x... might be translated into it's real name</html>");
			mtd.addColumn(CM_NAME,  "additional_info",          "<html>(Requires @get_additional_info option)<BR>"
			                                                        + "Returns additional non-performance-related session/request information<BR>"
			                                                        + "If the script finds a SQL Agent job running, the name of the job and job step will be reported<BR>"
			                                                        + "If @get_task_info = 2 and the script finds a lock wait, the locked object will be reported<BR>"
			                                                        + "</html>");
			mtd.addColumn(CM_NAME,  "start_time",               "<html>For active requests, shows the time the request started<BR>For sleeping sessions, shows the time the last batch completed</html>");
			mtd.addColumn(CM_NAME,  "login_time",               "<html>Shows the time that the session connected</html>");
			mtd.addColumn(CM_NAME,  "request_id",               "<html>For active requests, shows the request_id<BR>Should be 0 unless MARS is being used</html>");
			mtd.addColumn(CM_NAME,  "collection_time",          "<html>Time that this script's final SELECT ran</html>");
			mtd.addColumn(CM_NAME,  "memory_info",              "<html>(Requires @get_memory_info)<BR>For active queries that require workspace memory, returns information on memory grants, resource semaphores, and the resource governor settings that are impacting the allocation.</html>");
		}
		catch (NameNotFoundException e) {/*ignore*/}
	}

	
	@Override
	public String getToolTipTextOnTableCell(MouseEvent e, String colName, Object cellValue, int modelRow, int modelCol)
	{
		if (StringUtil.containsAny(colName, "locks", "additional_info", "memory_info"))
		{
			String formatted = StringUtil.xmlFormat(cellValue + "");
			String htmlEscaped = "<pre>" + StringEscapeUtils.escapeHtml4(formatted) + "<pre>";

			if ("locks".equals(colName))
			{
				// Make: |request_mode="X"| stand out in color ORANGE
				htmlEscaped = htmlEscaped.replace("request_mode=&quot;X&quot;", "<b><font color='orange'>request_mode=&quot;X&quot;</font></b>");

				// Make: |request_status="WAIT"| stand out in color RED
				htmlEscaped = htmlEscaped.replace("request_status=&quot;WAIT&quot;", "<b><font color='red'>request_status=&quot;WAIT&quot;</font></b>");
			}

			return htmlEscaped;
		}

		if (StringUtil.containsAny(colName, "sql_text", "sql_command"))
		{
			return "<pre>" + StringEscapeUtils.escapeHtml4(cellValue + "") + "<pre>";
		}

		return super.getToolTipTextOnTableCell(e, colName, cellValue, modelRow, modelCol);
	}
	
	@Override
	public ResultSetMetaDataCached modifyResultSetMetaData(ResultSetMetaDataCached rsmdc)
	{
		// OMG: -hack-warning-
		//  - Lets ADD a column (must be at the end to work)
		//  - But we will "move" it later using: cm.addPreferredColumnOrder(new ColumnHeaderPropsEntry(FAKE_COL_DURATION, 0));
		String colName = FAKE_COL_DURATION;
		try
		{
			rsmdc.addColumn(colName, java.sql.Types.VARCHAR, "varchar", true, "java.lang.String", 60);
			
			int colpos = rsmdc.findColumn(colName);
			if (colpos == -1)
				_logger.error("In CM='" + getName() + "', when adding a FAKE Column '" + colName + "' it was not found... (if this happens something is BROKEN).");

			// Mark it as FAKE -- The column wont be read from the ResultSet... and the value will be "null" (not the string but a null value)
			rsmdc.setFakedColumn(colpos, true);
			rsmdc.setFakedColumnDefaultValue(colpos, "-unknown-");
		}
		catch (SQLException ex)
		{
			_logger.error("In CM='" + getName() + "', when adding a FAKE Column '" + colName + "', caught exception... continuing anyway...", ex);
		}

		return rsmdc;
	}

	private void localInit()
	{
		// sp_WhoIsActive produces some SQLWarnings... lets ignore them...
		List<IgnoreSqlWarning> ignoreList = new ArrayList<>();

		ignoreList.add( new IgnoreSqlWarning(8625) ); // Msg=8625, Text=The join order has been enforced because a local join hint is used.
		ignoreList.add( new IgnoreSqlWarning(8153) ); // Msg=8153, Null value is eliminated by an aggregate or other SET operation.
		
		setIgnoreSqlWarnings(ignoreList);
	}

	@Override
	public void localCalculation(CounterSample newSample)
	{
		// make: column 'program_name' with value "SQLAgent - TSQL JobStep (Job 0x38AAD6888E5C5E408DE573B0A25EE970 : Step 1)"
		// into:                                  "SQLAgent - TSQL JobStep (Job '<name-of-the-job>' : Step 1 '<name-of-the-step>')
		SqlServerCmUtils.localCalculation_resolveSqlAgentProgramName(newSample);
		
		// Fill in the "passed time" column
		int pos_duration        = newSample.findColumn(FAKE_COL_DURATION);
		int pos_start_time      = newSample.findColumn("start_time");
		int pos_collection_time = newSample.findColumn("collection_time");

		if (pos_duration == -1 || pos_start_time == -1 || pos_collection_time == -1)
		{
			return;
		}

		// Loop on all newSample rows
		int rowc = newSample.getRowCount();
		for (int rowId=0; rowId < rowc; rowId++) 
		{
			Timestamp start_time      = newSample.getValueAsTimestamp(rowId, pos_start_time);
			Timestamp collection_time = newSample.getValueAsTimestamp(rowId, pos_collection_time);

			if (start_time != null && collection_time != null)
			{
				long msDiff = collection_time.getTime() - start_time.getTime();
				
				String passedTime = TimeUtils.msToTimeStrDHMSms(msDiff);
				newSample.setValueAt(passedTime, rowId, pos_duration);
			}
		}
	}

//	@Override
//	public boolean isStringTrimEnabled()
//	{
//		return true;
//	}
	
	@Override
	public List<String> getPkForVersion(DbxConnection conn, DbmsVersionInfo versionInfo)
	{
		List <String> pkCols = new LinkedList<String>();

		pkCols.add("session_id");

		return pkCols;
	}

	@Override
	public String getSqlForVersion(DbxConnection conn, DbmsVersionInfo versionInfo)
	{
		// Let the FAKE column 'duration' be first...
		addPreferredColumnOrder(new ColumnHeaderPropsEntry(FAKE_COL_DURATION, 0));

		
		String dbname = SqlServerUtils.findProcNameInAnyDatabases(conn, "sp_WhoIsActive", "", true);

		Configuration conf = Configuration.getCombinedConfiguration();

		boolean sample_systemThreads   = conf.getBooleanProperty(PROPKEY_sample_systemThreads  , DEFAULT_sample_systemThreads  );
		boolean sample_memoryInfo      = conf.getBooleanProperty(PROPKEY_sample_memoryInfo     , DEFAULT_sample_memoryInfo     );
		boolean sample_locks           = conf.getBooleanProperty(PROPKEY_sample_locks          , DEFAULT_sample_locks          );
		boolean sample_transactionInfo = conf.getBooleanProperty(PROPKEY_sample_transactionInfo, DEFAULT_sample_transactionInfo);
		boolean sample_outerCommand    = conf.getBooleanProperty(PROPKEY_sample_outerCommand   , DEFAULT_sample_outerCommand   );
		boolean sample_plans           = conf.getBooleanProperty(PROPKEY_sample_plans          , DEFAULT_sample_plans          );
		boolean sample_blockLeaders    = conf.getBooleanProperty(PROPKEY_sample_blockLeaders   , DEFAULT_sample_blockLeaders   );
		boolean sample_additionalInfo  = conf.getBooleanProperty(PROPKEY_sample_additionalInfo , DEFAULT_sample_additionalInfo );
		boolean sample_sleepingSpids   = conf.getBooleanProperty(PROPKEY_sample_sleepingSpids  , DEFAULT_sample_sleepingSpids  );
		boolean sample_taskInfo        = conf.getBooleanProperty(PROPKEY_sample_taskInfo       , DEFAULT_sample_taskInfo       );
		
		String sql = 
			"exec " + dbname + ".dbo.sp_WhoIsActive \n" +
			"     @format_output        = 0 \n" +
			"    ,@show_system_spids    = " + (sample_systemThreads   ? 1 : 0) + " \n" +
			"    ,@get_memory_info      = " + (sample_memoryInfo      ? 1 : 0) + " \n" +
			"    ,@get_locks            = " + (sample_locks           ? 1 : 0) + " \n" +
			"    ,@get_transaction_info = " + (sample_transactionInfo ? 1 : 0) + " \n" +
			"    ,@get_outer_command    = " + (sample_outerCommand    ? 1 : 0) + " \n" +
			"    ,@get_plans            = " + (sample_plans           ? 1 : 0) + " \n" +
			"    ,@find_block_leaders   = " + (sample_blockLeaders    ? 1 : 0) + " \n" +
			"    ,@get_additional_info  = " + (sample_additionalInfo  ? 1 : 0) + " \n" +
			"    ,@show_sleeping_spids  = " + (sample_sleepingSpids   ? 2 : 1) + " \n" + // 0='does not pull any sleeping SPIDs', 1='pulls only those sleeping SPIDs that also have an open transaction', 2='pulls all sleeping SPIDs'
			"    ,@get_task_info        = " + (sample_taskInfo        ? 2 : 1) + " \n" + // 0='does not pull any task-related information', 1='is a lightweight mode that pulls the top non-CXPACKET wait, giving preference to blockers', 2='pulls all available task-based metrics, including: number of active tasks, current wait stats, physical I/O, context switches, and blocker information'
			"";
		
		return sql;
	}

	//-------------------------------------------------------------------------------------------
	// Default output with: @format_output = 0
	//-------------------------------------------------------------------------------------------
	// RS> Col# Label               JDBC Type Name           Guessed DBMS type Source Table
	// RS> ---- ------------------- ------------------------ ----------------- ------------
	// RS> 1    session_id          java.sql.Types.SMALLINT  smallint          -none-      
	// RS> 2    sql_text            java.sql.Types.NVARCHAR  nvarchar(max)     -none-      
	// RS> 3    login_name          java.sql.Types.NVARCHAR  nvarchar(128)     -none-      
	// RS> 4    wait_info           java.sql.Types.NVARCHAR  nvarchar(4000)    -none-      
	// RS> 5    CPU                 java.sql.Types.BIGINT    bigint            -none-      
	// RS> 6    tempdb_allocations  java.sql.Types.BIGINT    bigint            -none-      
	// RS> 7    tempdb_current      java.sql.Types.BIGINT    bigint            -none-      
	// RS> 8    blocking_session_id java.sql.Types.SMALLINT  smallint          -none-      
	// RS> 9    reads               java.sql.Types.BIGINT    bigint            -none-      
	// RS> 10   writes              java.sql.Types.BIGINT    bigint            -none-      
	// RS> 11   physical_reads      java.sql.Types.BIGINT    bigint            -none-      
	// RS> 12   used_memory         java.sql.Types.BIGINT    bigint            -none-      
	// RS> 13   status              java.sql.Types.VARCHAR   varchar(30)       -none-      
	// RS> 14   open_tran_count     java.sql.Types.SMALLINT  smallint          -none-      
	// RS> 15   percent_complete    java.sql.Types.REAL      real              -none-      
	// RS> 16   host_name           java.sql.Types.NVARCHAR  nvarchar(128)     -none-      
	// RS> 17   database_name       java.sql.Types.NVARCHAR  nvarchar(128)     -none-      
	// RS> 18   program_name        java.sql.Types.NVARCHAR  nvarchar(128)     -none-      
	// RS> 19   start_time          java.sql.Types.TIMESTAMP datetime          -none-      
	// RS> 20   login_time          java.sql.Types.TIMESTAMP datetime          -none-      
	// RS> 21   request_id          java.sql.Types.INTEGER   int               -none-      
	// RS> 22   collection_time     java.sql.Types.TIMESTAMP datetime          -none-      	
}
