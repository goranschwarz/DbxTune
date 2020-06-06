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
import java.sql.Connection;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

import javax.naming.NameNotFoundException;

import org.apache.commons.lang3.math.NumberUtils;
import org.apache.log4j.Logger;

import com.asetune.CounterControllerSqlServer;
import com.asetune.ICounterController;
import com.asetune.IGuiController;
import com.asetune.alarm.AlarmHandler;
import com.asetune.alarm.events.AlarmEvent;
import com.asetune.alarm.events.AlarmEventDatabaseState;
import com.asetune.alarm.events.AlarmEventLongRunningTransaction;
import com.asetune.alarm.events.AlarmEventLowDbFreeSpace;
import com.asetune.alarm.events.AlarmEventLowLogFreeSpace;
import com.asetune.alarm.events.AlarmEventLowOsDiskFreeSpace;
import com.asetune.alarm.events.AlarmEventOldBackup;
import com.asetune.alarm.events.AlarmEventOldTranLogBackup;
import com.asetune.cm.CmSettingsHelper;
import com.asetune.cm.CmSettingsHelper.MapNumberValidator;
import com.asetune.cm.CmSettingsHelper.RegExpInputValidator;
import com.asetune.cm.CounterSetTemplates;
import com.asetune.cm.CounterSetTemplates.Type;
import com.asetune.cm.CountersModel;
import com.asetune.cm.sqlserver.gui.CmDatabasesPanel;
import com.asetune.config.dict.MonTablesDictionary;
import com.asetune.config.dict.MonTablesDictionaryManager;
import com.asetune.graph.TrendGraphDataPoint;
import com.asetune.graph.TrendGraphDataPoint.LabelType;
import com.asetune.gui.DbSelectionForGraphsDialog;
import com.asetune.gui.MainFrame;
import com.asetune.gui.TabularCntrPanel;
import com.asetune.utils.Configuration;
import com.asetune.utils.StringUtil;
import com.asetune.utils.Ver;

/**
 * @author Goran Schwarz (goran_schwarz@hotmail.com)
 */
public class CmDatabases
extends CountersModel
{
	private static Logger        _logger          = Logger.getLogger(CmDatabases.class);
	private static final long    serialVersionUID = 1L;

	public static final String   CM_NAME          = CmDatabases.class.getSimpleName();
	public static final String   SHORT_NAME       = "Databases";
	public static final String   HTML_DESC        = 
		"<html>" +
		"Various information on a database level.<br>" +
		"<br>" +
		"<b>Note:</b><br>" +
		"Databases in the attached Graphs can be included or excluded<br>" +
		"Click button \"Set 'Graph' databases\"" +
		"<br><br>" +
		"Table Background colors:" +
		"<ul>" +
//		"    <li>LIGHT_BLUE/PURPLE - A Database backup is in progress</li>" +
		"    <li>YELLOW            - Has a long running transaction issued by a user.</li>" +
//		"    <li>PINK              - The transaction log for this database is filled to 90%, and will probably soon be full.</li>" +
//		"    <li>RED               - The transaction log for this database is <b>full</b> and users are probably suspended.</li>" +
		"</ul>" +
		"</html>";

	public static final String   GROUP_NAME       = MainFrame.TCP_GROUP_SERVER;
	public static final String   GUI_ICON_FILE    = "images/"+CM_NAME+".png";

	public static final long     NEED_SRV_VERSION = 0;
	public static final long     NEED_CE_VERSION  = 0;

	public static final String[] MON_TABLES       = new String[] {
			"CmDatabases", 
			"databases", 
			"dm_db_file_space_usage", 
			"dm_db_log_space_usage", 
			"dm_exec_sessions", 
			"dm_exec_connections", 
			"dm_tran_active_transactions", 
			"dm_exec_sql_text", 
			"dm_exec_query_plan", 
			"dm_os_volume_stats", 
			"backupset"};
	public static final String[] NEED_ROLES       = new String[] {"VIEW SERVER STATE", "CONNECT ANY DATABASE"};
	public static final String[] NEED_CONFIG      = new String[] {};

	public static final String[] PCT_COLUMNS      = new String[] {
			"LogSizeUsedPct", 
			"DataSizeUsedPct", 
			"LogOsDiskUsedPct", 
			"LogOsDiskFreePct", 
			"DataOsDiskUsedPct",
			"DataOsDiskFreePct"
	};
	public static final String[] DIFF_COLUMNS     = new String[] {
			"LogSizeUsedInMbDiff",
			"LogSizeFreeInMbDiff",
			"DataSizeUsedInMbDiff",
			"DataSizeFreeInMbDiff"
	};

	public static final boolean  NEGATIVE_DIFF_COUNTERS_TO_ZERO = false;
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

		return new CmDatabases(counterController, guiController);
	}

	public CmDatabases(ICounterController counterController, IGuiController guiController)
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
		
//		addDependsOnCm(CmXxx.CM_NAME); // CMspinlockSum must have been executed before this cm

		addTrendGraphs();
		
		CounterSetTemplates.register(this);
	}


	//------------------------------------------------------------
	// Implementation
	//------------------------------------------------------------
	private static final String  PROP_PREFIX                          = CM_NAME;

//	public static final String  PROPKEY_sample_spaceusage            = PROP_PREFIX + ".sample.spaceusage";
//	public static final boolean DEFAULT_sample_spaceusage            = false;

	public static final String  PROPKEY_sample_showplan              = PROP_PREFIX + ".sample.showplan";
	public static final boolean DEFAULT_sample_showplan              = true;
	
	public static final String  PROPKEY_sample_monSqlText            = PROP_PREFIX + ".sample.monSqltext";
	public static final boolean DEFAULT_sample_monSqlText            = true;

//	public static final String  PROPKEY_spaceusageInMb               = PROP_PREFIX + ".sample.spaceusageInMb";
//	public static final boolean DEFAULT_spaceusageInMb               = false;
	

//	public static final String  PROPKEY_disable_spaceusage_onTimeout = PROP_PREFIX + ".disable.spaceusage.onTimeoutException";
//	public static final boolean DEFAULT_disable_spaceusage_onTimeout = true;
	
//	public static final String GRAPH_NAME_LOGSEMAPHORE_CONT  = "DbLogSemapContGraph";   //String x=GetCounters.CM_GRAPH_NAME__OPEN_DATABASES__LOGSEMAPHORE_CONT;
	public static final String GRAPH_NAME_DB_SIZE_MB         = "DbSizeMb";
	public static final String GRAPH_NAME_LOGSIZE_LEFT_MB    = "DbLogSizeLeftMbGraph";  //String x=GetCounters.CM_GRAPH_NAME__OPEN_DATABASES__LOGSIZE_LEFT;
	public static final String GRAPH_NAME_LOGSIZE_USED_MB    = "DbLogSizeUsedMbGraph";
	public static final String GRAPH_NAME_LOGSIZE_USED_PCT   = "DbLogSizeUsedPctGraph"; //String x=GetCounters.CM_GRAPH_NAME__OPEN_DATABASES__LOGSIZE_USED_PCT;
	public static final String GRAPH_NAME_DATASIZE_LEFT_MB   = "DbDataSizeLeftMbGraph";
	public static final String GRAPH_NAME_DATASIZE_USED_MB   = "DbDataSizeUsedMbGraph";
	public static final String GRAPH_NAME_DATASIZE_USED_PCT  = "DbDataSizeUsedPctGraph";
//	public static final String GRAPH_NAME_OLDEST_TRAN_IN_SEC = "OldestTranInSecGraph";
	public static final String GRAPH_NAME_TEMPDB_USED_MB     = "TempdbUsedMbGraph";

	public static final String GRAPH_NAME_OS_DISK_FREE_MB    = "OsDiskFreeMb";
	public static final String GRAPH_NAME_OS_DISK_USED_MB    = "OsDiskUsedMb";
	public static final String GRAPH_NAME_OS_DISK_USED_PCT   = "OsDiskUsedPct";
	
	private void addTrendGraphs()
	{
//		addTrendGraph(GRAPH_NAME_LOGSEMAPHORE_CONT,
//			"DB Transaction Log Semaphore Contention",            // Menu CheckBox text
//			"DB Transaction Log Semaphore Contention in Percent ("+GROUP_NAME+"->"+SHORT_NAME+")", // Label 
//			null, 
//			LabelType.Dynamic,
//			TrendGraphDataPoint.Category.DISK,
//			false, // is Percent Graph
//			false, // visible at start
//			0,     // graph is valid from Server Version. 0 = All Versions; >0 = Valid from this version and above 
//			-1);   // minimum height

		addTrendGraph(GRAPH_NAME_DB_SIZE_MB,
			"Database Size in MB",        // Menu CheckBox text
			"Database Size in MB ("+GROUP_NAME+"->"+SHORT_NAME+")", // Label 
			TrendGraphDataPoint.Y_AXIS_SCALE_LABELS_MB,
			null, 
			LabelType.Dynamic,
			TrendGraphDataPoint.Category.SPACE,
			false, // is Percent Graph
			false, // visible at start
			0,     // graph is valid from Server Version. 0 = All Versions; >0 = Valid from this version and above 
			-1);   // minimum height

		addTrendGraph(GRAPH_NAME_LOGSIZE_LEFT_MB,
			"DB Transaction Log Space left in MB",        // Menu CheckBox text
			"DB Transaction Log Space left to use in MB ("+GROUP_NAME+"->"+SHORT_NAME+")", // Label 
			TrendGraphDataPoint.Y_AXIS_SCALE_LABELS_MB,
			null, 
			LabelType.Dynamic,
			TrendGraphDataPoint.Category.SPACE,
			false, // is Percent Graph
			false, // visible at start
			0,     // graph is valid from Server Version. 0 = All Versions; >0 = Valid from this version and above 
			-1);   // minimum height

		addTrendGraph(GRAPH_NAME_LOGSIZE_USED_MB,
			"DB Transaction Log Space used in MB",        // Menu CheckBox text
			"DB Transaction Log Space used in MB ("+GROUP_NAME+"->"+SHORT_NAME+")", // Label 
			TrendGraphDataPoint.Y_AXIS_SCALE_LABELS_MB,
			null, 
			LabelType.Dynamic,
			TrendGraphDataPoint.Category.SPACE,
			false, // is Percent Graph
			false, // visible at start
			0,     // graph is valid from Server Version. 0 = All Versions; >0 = Valid from this version and above 
			-1);   // minimum height

		addTrendGraph(GRAPH_NAME_LOGSIZE_USED_PCT,
			"DB Transaction Log Space used in PCT",     // Menu CheckBox text
			"DB Transaction Log Space used in Percent ("+GROUP_NAME+"->"+SHORT_NAME+")", // Label 
			TrendGraphDataPoint.Y_AXIS_SCALE_LABELS_PERCENT,
			null, 
			LabelType.Dynamic,
			TrendGraphDataPoint.Category.SPACE,
			true,  // is Percent Graph
			false, // visible at start
			0,     // graph is valid from Server Version. 0 = All Versions; >0 = Valid from this version and above 
			-1);   // minimum height

		addTrendGraph(GRAPH_NAME_DATASIZE_LEFT_MB,
			"DB Data Space left in MB",        // Menu CheckBox text
			"DB Data Space left to use in MB ("+GROUP_NAME+"->"+SHORT_NAME+")", // Label 
			TrendGraphDataPoint.Y_AXIS_SCALE_LABELS_MB,
			null, 
			LabelType.Dynamic,
			TrendGraphDataPoint.Category.SPACE,
			false, // is Percent Graph
			false, // visible at start
			0,     // graph is valid from Server Version. 0 = All Versions; >0 = Valid from this version and above 
			-1);   // minimum height

		addTrendGraph(GRAPH_NAME_DATASIZE_USED_MB,
			"DB Data Space used in MB",     // Menu CheckBox text
			"DB Data Space used in MB ("+GROUP_NAME+"->"+SHORT_NAME+")", // Label 
			TrendGraphDataPoint.Y_AXIS_SCALE_LABELS_MB,
			null, 
			LabelType.Dynamic,
			TrendGraphDataPoint.Category.SPACE,
			false,  // is Percent Graph
			false, // visible at start
			0,     // graph is valid from Server Version. 0 = All Versions; >0 = Valid from this version and above 
			-1);   // minimum height

		addTrendGraph(GRAPH_NAME_DATASIZE_USED_PCT,
			"DB Data Space used in PCT",     // Menu CheckBox text
			"DB Data Space used in Percent ("+GROUP_NAME+"->"+SHORT_NAME+")", // Label 
			TrendGraphDataPoint.Y_AXIS_SCALE_LABELS_PERCENT,
			null, 
			LabelType.Dynamic,
			TrendGraphDataPoint.Category.SPACE,
			true,  // is Percent Graph
			false, // visible at start
			0,     // graph is valid from Server Version. 0 = All Versions; >0 = Valid from this version and above 
			-1);   // minimum height

//		addTrendGraph(GRAPH_NAME_OLDEST_TRAN_IN_SEC,
//			"Oldest Open Transaction in any Databases",     // Menu CheckBox text
//			"Oldest Open Transaction in any Databases, in Seconds ("+GROUP_NAME+"->"+SHORT_NAME+")", // Label 
//			new String[] { "Seconds" },
//			LabelType.Static,
//			false, // is Percent Graph
//			false, // visible at start
//			0,     // graph is valid from Server Version. 0 = All Versions; >0 = Valid from this version and above 
//			-1);   // minimum height

		addTrendGraph(GRAPH_NAME_TEMPDB_USED_MB,
			"TempDB Space used in MB",     // Menu CheckBox text
			"TempDB Space used in MB ("+GROUP_NAME+"->"+SHORT_NAME+")", // Label 
			TrendGraphDataPoint.Y_AXIS_SCALE_LABELS_MB,
			null, 
			LabelType.Dynamic,
			TrendGraphDataPoint.Category.SPACE,
			false, // is Percent Graph
			false, // visible at start
			0,     // graph is valid from Server Version. 0 = All Versions; >0 = Valid from this version and above 
			-1);   // minimum height



		addTrendGraph(GRAPH_NAME_OS_DISK_FREE_MB,
			"DB OS Disk Space free/left in MB",        // Menu CheckBox text
			"DB OS Disk Space free/left in MB ("+GROUP_NAME+"->"+SHORT_NAME+")", // Label 
			TrendGraphDataPoint.Y_AXIS_SCALE_LABELS_MB,
			null, 
			LabelType.Dynamic,
			TrendGraphDataPoint.Category.SPACE,
			false, // is Percent Graph
			false, // visible at start
			0,     // graph is valid from Server Version. 0 = All Versions; >0 = Valid from this version and above 
			-1);   // minimum height

		addTrendGraph(GRAPH_NAME_OS_DISK_USED_MB,
			"DB OS Disk Space used in MB",        // Menu CheckBox text
			"DB OS Disk Space used in MB ("+GROUP_NAME+"->"+SHORT_NAME+")", // Label 
			TrendGraphDataPoint.Y_AXIS_SCALE_LABELS_MB,
			null, 
			LabelType.Dynamic,
			TrendGraphDataPoint.Category.SPACE,
			false, // is Percent Graph
			false, // visible at start
			0,     // graph is valid from Server Version. 0 = All Versions; >0 = Valid from this version and above 
			-1);   // minimum height

		addTrendGraph(GRAPH_NAME_OS_DISK_USED_PCT,
			"DB OS Disk Space used in PCT",     // Menu CheckBox text
			"DB OS Disk Space used in Percent ("+GROUP_NAME+"->"+SHORT_NAME+")", // Label 
			TrendGraphDataPoint.Y_AXIS_SCALE_LABELS_PERCENT,
			null, 
			LabelType.Dynamic,
			TrendGraphDataPoint.Category.SPACE,
			true,  // is Percent Graph
			false, // visible at start
			0,     // graph is valid from Server Version. 0 = All Versions; >0 = Valid from this version and above 
			-1);   // minimum height
	}

	@Override
	protected TabularCntrPanel createGui()
	{
		return new CmDatabasesPanel(this);
	}

	@Override
	public String[] getDependsOnConfigForVersion(Connection conn, long srvVersion, boolean isClusterEnabled)
	{
		return NEED_CONFIG;
	}

//	@Override
//	public void addMonTableDictForVersion(Connection conn, long srvVersion, boolean isClusterEnabled)
//	{
//		try 
//		{
//			MonTablesDictionary mtd = MonTablesDictionaryManager.getInstance();
//			mtd.addColumn("monOpenDatabases", "CeDbRecoveryStatus", "<html>" +
//			                                                             "1 = The database is currently undergoing <B>node-failure</B> recovery.<br> " +
//			                                                             "0 = Normal, <B>not</B> in node-failure recovery." +
//			                                                        "</html>");
//			mtd.addColumn("monOpenDatabases", "AppendLogContPct",   "<html>" +
//			                                                             "Log Semaphore Contention in percent.<br> " +
//			                                                             "<b>Formula</b>: Pct = (AppendLogWaits / AppendLogRequests) * 100<br>" +
//			                                                        "</html>");
//			mtd.addColumn("monOpenDatabases", "LogDataIsMixed",     "<html>Data Pages and Log pages are mixed on some device(s).<br><b>Formula</b>: sysdatabases.status2 has bit 32768 (0x8000) set.</html>");
//			mtd.addColumn("monOpenDatabases", "DbSizeInMb",         "<html>Database size in MB</html>");
//			mtd.addColumn("monOpenDatabases", "LogSizeInMb",        "<html>" +
//			                                                             "Size in MB of the transaction log in the database. <br>" +
//			                                                             "<b>Formula</b>: This is simply grabbed by: sum(size) from sysusages where (segmap & 4) = 4<br>" +
//			                                                        "</html>");
//			mtd.addColumn("monOpenDatabases", "LogSizeUsedInMb",    "<html>" +
//			                                                             "How many MB have we used of the Transaction log.<br> " +
//			                                                             "<b>Formula</b>: LogSizeInMb - LogSizeFreeInMb<br>" +
//			                                                             "<b>Note 1</b>: This is the same formula as sp_helpdb 'dbname' uses to calculate space left.<br>" +
//			                                                             "<b>Note 2</b>: This might not work correct for databases with mixed data and log.<br>" +
//			                                                        "</html>");
//			mtd.addColumn("monOpenDatabases", "LogSizeUsedInMbDiff","<html>Same as column 'LogSizeUsedInMb', but just show the difference from previous sample.</html>");
//			mtd.addColumn("monOpenDatabases", "LogSizeFreeInMb",    "<html>" +
//			                                                             "How many MB have we got left in the Transaction log.<br> " +
//			                                                             "<b>Formula</b>: (lct_admin('logsegment_freepages',DBID)-lct_admin('reserved_for_rollbacks',DBID)) / (1024.0*1024.0/@@maxpagesize)<br>" +
//			                                                             "<b>Note 1</b>: This is the same formula as sp_helpdb 'dbname' uses to calculate space left.<br>" +
//			                                                             "<b>Note 2</b>: This might not work correct for databases with mixed data and log.<br>" +
//			                                                             "<b>Note 3</b>: If column 'TransactionLogFull' then 'LogSizeFreeInMb' is set to 0, this to prevent any faulty calculations or strange values from lct_admin().<br>" +
//			                                                        "</html>");
//			mtd.addColumn("monOpenDatabases", "LogSizeFreeInMbDiff","<html>Same as column 'LogSizeFreeInMb', but just show the difference from previous sample.</html>");
//			mtd.addColumn("monOpenDatabases", "LogSizeUsedPct",     "<html>" +
//			                                                            "How many percent have we <b>used</b> of the transaction log. near 100% = Full<br> " +
//			                                                            "<b>Formula</b>: Pct = 100.0 - ((oval_LogSizeFreeInMb / oval_LogSizeInMb) * 100.0)<br>" +
//			                                                        "</html>");
//			mtd.addColumn("monOpenDatabases", "DataSizeInMb",       "<html>" +
//			                                                            "Size in MB of the Data Portion in the database. <br>" +
//			                                                            "<b>Formula</b>: This is simply grabbed by: sum(size) from sysusages where (segmap & (2147483647-4)) > 0 -- meaning: all segments except 4, the logsegment<br>" +
//			                                                        "</html>");
//			mtd.addColumn("monOpenDatabases", "DataSizeUsedInMb",   "<html>" +
//			                                                            "How many MB have we used of the Data Area (everything with segmap not in 4 'logsegment').<br> " +
//			                                                            "<b>Formula</b>: DataSizeInMb - DataSizeFreeInMb<br>" +
//			                                                            "<b>Note 1</b>: This is the same formula as sp_helpdb 'dbname' uses to calculate space left.<br>" +
//			                                                            "<b>Note 2</b>: This might not work correct for databases with mixed data and log.<br>" +
//			                                                         "</html>");
//			mtd.addColumn("monOpenDatabases", "DataSizeUsedInMbDiff","<html>Same as column 'DataSizeUsedInMb', but just show the difference from previous sample.</html>");
//			mtd.addColumn("monOpenDatabases", "DataSizeFreeInMb",   "<html>" +
//			                                                            "How many MB have we got left in the Data Portion.<br> " +
//			                                                            "<b>Formula</b>: (select sum(curunreservedpgs(u.dbid, u.lstart, u.unreservedpgs)/(1024.0*1024.0/@@maxpagesize)) from master.dbo.sysusages u readpast where u.dbid = od.DBID and (segmap & (2147483647-4)) > 0) -- -- meaning: all segments except 4, the logsegment<br>" +
//			                                                            "<b>Note 1</b>: This is the same formula as sp_helpdb 'dbname' uses to calculate space left.<br>" +
//			                                                            "<b>Note 2</b>: This might not work correct for databases with mixed data and log.<br>" +
//			                                                        "</html>");
//			mtd.addColumn("monOpenDatabases", "DataSizeFreeInMbDiff","<html>Same as column 'DataSizeFreeInMb', but just show the difference from previous sample.</html>");
//			mtd.addColumn("monOpenDatabases", "DataSizeUsedPct",     "<html>" +
//			                                                            "How many percent have we <b>used</b> of the Data Portion. near 100% = Full<br> " +
//			                                                            "<b>Formula</b>: Pct = 100.0 - ((oval_DataSizeFreeInMb / oval_DataSizeInMb) * 100.0)<br>" +
//			                                                         "</html>");
//			mtd.addColumn("monOpenDatabases", "OldestTranStartTime","<html>" +
//			                                                            "Start time of the oldest open transaction in this database.<br> " +
//			                                                            "<b>Formula</b>: OldestTranStartTime = column: master.dbo.syslogshold.starttime<br>" +
//			                                                        "</html>");
//			mtd.addColumn("monOpenDatabases", "OldestTranInSeconds","<html>" +
//			                                                            "Number of seconds since the oldest open transaction in this database was started. <br> " +
//			                                                            "<b>Formula</b>: OldestTranInSeconds = datediff(ss, master.dbo.syslogshold.starttime, getdate())<br>" +
//			                                                        "</html>");
//			mtd.addColumn("monOpenDatabases", "OldestTranName",     "<html>" +
//			                                                            "Name of the oldest open transaction in this database.<br> " +
//			                                                            "<b>Formula</b>: OldestTranName = column: master.dbo.syslogshold.name<br>" +
//			                                                        "</html>");
//			mtd.addColumn("monOpenDatabases", "OldestTranSpid",     "<html>" +
//			                                                            "SPID, which is responsible for the oldest open transaction in this database<br> " +
//			                                                            "<b>Formula</b>: OldestTranSpid = column: master.dbo.syslogshold.spid<br>" +
//			                                                        "</html>");
//			mtd.addColumn("monOpenDatabases", "SPID",               "<html>" +
//			                                                            "<b>Note</b>: same column as <code>OldestTranSpid</code>, just as another label to enable right click menu in table<br>" +
//			                                                            "SPID, which is responsible for the oldest open transaction in this database<br> " +
//			                                                            "<b>Formula</b>: SPID = column: master.dbo.syslogshold.spid<br>" +
//			                                                        "</html>");
//			mtd.addColumn("monOpenDatabases", "OldestTranProg",     "<html>" +
//			                                                             "Application Name, which is resopnsible for the oldest open transaction in this database<br> " +
//			                                                             "<b>Formula</b>: OldestTranProg = (select p.program_name from master.dbo.sysprocesses p where p.spid = master.dbo.syslogshold.spid)<br>" +
//			                                                        "</html>");
//			mtd.addColumn("monOpenDatabases", "OldestTranPage",     "<html>" +
//			                                                            "Page number in the transaction log, which holds the oldest open transaction in this database.<br> " +
//			                                                            "<b>Note:</b> you can do: dbcc traceon(3604)  dbcc page(dbid, pagenum) to see the content of that page.<br> " +
//			                                                            "<b>Formula</b>: OldestTranProg = column: master.dbo.syslogshold.page<br>" +
//			                                                        "</html>");
//
//			mtd.addColumn("monOpenDatabases",  "SrvPageSize",    "ASE Servers page size (@@maxpagesize)");
//
//			mtd.addColumn("monOpenDatabases", "RawSpaceUsage",     
//					"<html>" +
//						"Returns metrics for space use in SAP ASE as a comma-separated string.<br> " +
//						"<b>Formula</b>: function: spaceusage(dbid)<br>" +
//						"<br>" +
//						"Below is a description from the SAP/Sybase manual" +
//						"<ul>" +
//						"  <li><code>reserved pages</code>   - number of pages reserved for an object, which may include index pages if you selected index IDs based on the input parameters</li>" +
//						"  <li><code>used pages</code>       - number of pages used by the object, which may include index pages if you selected index IDs based on the input parameters.<br>"
//						                                    + "The value for used pages that spaceusage returns when you specify index_id = 1 (that is, for all-pages clustered indexes) is the used page count for the index layer of the clustered index. However, the value the used_pages function returns when you specify index_id = 1 includes the used page counts for the data and the index layers.</li>" +
//						"  <li><code>data pages</code>       - number of data pages used by the object, which may include index pages if you selected index IDs based on the input parameters.</li>" +
//						"  <li><code>index pages</code>      - index pages – number of index-only pages, if the input parameters specified processing indexes on the objects. To determine the number of pages used for only the index-level pages, subtract the number of large object (LOB) pages from the number of index pages.</li>" +
//						"  <li><code>oam pages</code>        - number of OAM pages for all OAM chains, as selected by the input parameters.<br>"
//						                                    + "For example, if you specify:<br>"
//						                                    + "<code>spaceusage(database_id, object_id, index_id)</code><br>"
//						                                    + "oam pages indicates the number of OAM pages found for this index and any of its local index partitions. If you run spaceusage against a specific object, oam pages returns the amount of overhead for the extra pages used for this object’s space management.<br>"
//						                                    + "When you execute spaceusage for an <b>entire database</i>, oam pages returns the total overhead for the number of OAM pages needed to track space across all objects, and their off-row LOB columns.</li>" +
//						"  <li><code>allocation units</code> - number of allocation units that hold one or more extents for the specified object, index, or partition. allocation units indicates how many allocation units (or pages) Adaptive Server must scan while accessing all the pages of that object, index, or partition.<br>"
//						                                    + "When you run spaceusage against the <b>entire database</b>, allocation units returns the total number of allocation units reserving space for an object. However, because Adaptive Server can share allocation units across objects, this field might show a number greater than the total number of allocation units in the entire database.</li>" +
//						"  <li><code>row count</code>        - number of rows in the object or partition. spaceusage reports this row count as 0 when you specify the index_id parameter.</li>" +
//						"  <li><code>tables</code>           - total number of tables processed when you execute spaceusage and include only the database_id parameter (that is, when you are investigating space metrics for the entire database).</li>" +
//						"  <li><code>LOB pages</code>        - number of off-row large object pages for which the index ID is 255.<br>"
//						                                    + "LOB pages returns a nonzero value only when you use spaceusage to determine the space metrics for all indexes, or only the LOB index, on objects that contain off-row LOB data. LOB pages returns 0 when you use spaceusage to examine the space metrics only for tables (which have index IDs of 0).<br>"
//						                                    + "When you run spaceusage against the <b>entire database</b>, LOB pages displays the aggregate page counts for all LOB columns occupying off-row storage in all objects.</li>" +
//						"  <li><code>syslog pages</code>     - Currently, spaceusage does not report on syslogs</li>" +
//						"</ul>" +
//						"However, spaceusage does not report on tables that do not occupy space (for example, fake and proxy tables).<br>" +
//					"</html>");
//
//			mtd.addColumn("monOpenDatabases", "ReservedMb",
//					"<html>" +
//						"This is 'reserved pages' represented as MB instead of Pages, output from the function <code>spaceusage(dbid)</code>.<br>" +
//						"<code>reserved pages</code> - number of pages reserved for an object, which may include index pages if you selected index IDs based on the input parameters.<br>" +
//						"<b>Formula</b>: function: spaceusage(dbid)<br>" +
//					"</html>");
//
//			mtd.addColumn("monOpenDatabases", "UsedMb",
//					"<html>" +
//						"This is 'used pages' represented as MB instead of Pages, output from the function <code>spaceusage(dbid)</code>.<br>" +
//						"<code>used pages</code> - number of pages used by the object, which may include index pages if you selected index IDs based on the input parameters.<br>" +
//						"The value for used pages that spaceusage returns when you specify index_id = 1 (that is, for all-pages clustered indexes) is the used page count for the index layer of the clustered index. However, the value the used_pages function returns when you specify index_id = 1 includes the used page counts for the data and the index layers.<br>" +
//						"<b>Formula</b>: function: spaceusage(dbid)<br>" +
//					"</html>");
//
//			mtd.addColumn("monOpenDatabases", "UnUsedMb",
//					"<html>" +
//						"Calculate how many MB that are Un-Used.<br>" +
//						"<b>Formula</b>: (ReservedPages - UsedPages) / (1024*1024/@@maxpagesize)<br>" +
//					"</html>");
//
//			mtd.addColumn("monOpenDatabases", "DataMb",
//					"<html>" +
//						"This is 'data pages' represented as MB instead of Pages, output from the function <code>spaceusage(dbid)</code>.<br>" +
//						"<code>data pages</code> - number of data pages used by the object, which may include index pages if you selected index IDs based on the input parameters.<br>" +
//						"<b>Formula</b>: function: spaceusage(dbid)<br>" +
//					"</html>");
//
//			mtd.addColumn("monOpenDatabases", "DataMbReal",
//					"<html>" +
//						"Take away Index MB from the data pages and you get <i>actual</i> usage.<br>" +
//						"<b>Formula</b>: (DataPages - IndexPages) / (1024*1024/@@maxpagesize)<br>" +
//					"</html>");
//
//			mtd.addColumn("monOpenDatabases", "IndexMb",
//					"<html>" +
//						"This is 'index pages' represented as MB instead of Pages, output from the function <code>spaceusage(dbid)</code>.<br>" +
//						"<code>index pages</code> - index pages – number of index-only pages, if the input parameters specified processing indexes on the objects. To determine the number of pages used for only the index-level pages, subtract the number of large object (LOB) pages from the number of index pages.<br>" +
//						"<b>Formula</b>: function: spaceusage(dbid)<br>" +
//					"</html>");
//
//			mtd.addColumn("monOpenDatabases", "IndexMbReal",
//					"<html>" +
//						"Take away LOB pages from the index pages and you get <i>actual</i> usage.<br>" +
//						"<b>Formula</b>: (IndexPages - LobPages) / (1024*1024/@@maxpagesize)<br>" +
//					"</html>");
//
//			mtd.addColumn("monOpenDatabases", "LobMb",
//					"<html>" +
//						"This is 'LOB pages' represented as MB instead of Pages, output from the function <code>spaceusage(dbid)</code>.<br>" +
//						"<code>LOB pages</code> - number of off-row large object pages for which the index ID is 255.<br>" +
//                        "LOB pages returns a nonzero value only when you use spaceusage to determine the space metrics for all indexes, or only the LOB index, on objects that contain off-row LOB data. LOB pages returns 0 when you use spaceusage to examine the space metrics only for tables (which have index IDs of 0).<br>" +
//                        "When you run spaceusage against the <b>entire database</b>, LOB pages displays the aggregate page counts for all LOB columns occupying off-row storage in all objects.</li>" +
//						"<b>Formula</b>: function: spaceusage(dbid)<br>" +
//					"</html>");
//
//
//			mtd.addColumn("monOpenDatabases", "ReservedPages",
//					"<html>" +
//						"This is 'reserved pages' output from the function <code>spaceusage(dbid)</code>.<br>" +
//						"<code>reserved pages</code> - number of pages reserved for an object, which may include index pages if you selected index IDs based on the input parameters.<br>" +
//						"<b>Formula</b>: function: spaceusage(dbid)<br>" +
////						"<b>Note</b>: if 'Spaceusage in MB' is checked, this will be in MB, check column 'RawSpaceUsage' for the <i>raw</i> values.<br>" +
//					"</html>");
//
//			mtd.addColumn("monOpenDatabases", "UsedPages",
//					"<html>" +
//						"This is 'used pages' output from the function <code>spaceusage(dbid)</code>.<br>" +
//						"<code>used pages</code> - number of pages used by the object, which may include index pages if you selected index IDs based on the input parameters.<br>" +
//						"The value for used pages that spaceusage returns when you specify index_id = 1 (that is, for all-pages clustered indexes) is the used page count for the index layer of the clustered index. However, the value the used_pages function returns when you specify index_id = 1 includes the used page counts for the data and the index layers.<br>" +
//						"<b>Formula</b>: function: spaceusage(dbid)<br>" +
////						"<b>Note</b>: if 'Spaceusage in MB' is checked, this will be in MB, check column 'RawSpaceUsage' for the <i>raw</i> values.<br>" +
//					"</html>");
//
//			mtd.addColumn("monOpenDatabases", "UnUsedPages",
//					"<html>" +
//						"Calculate how many pages that are Un-Used.<br>" +
//						"<b>Formula</b>: ReservedPages - UsedPages<br>" +
//					"</html>");
//
//			mtd.addColumn("monOpenDatabases", "DataPages",
//					"<html>" +
//						"This is 'data pages' output from the function <code>spaceusage(dbid)</code>.<br>" +
//						"<code>data pages</code> - number of data pages used by the object, which may include index pages if you selected index IDs based on the input parameters.<br>" +
//						"<b>Formula</b>: function: spaceusage(dbid)<br>" +
////						"<b>Note</b>: if 'Spaceusage in MB' is checked, this will be in MB, check column 'RawSpaceUsage' for the <i>raw</i> values.<br>" +
//					"</html>");
//
//			mtd.addColumn("monOpenDatabases", "DataPagesReal",
//					"<html>" +
//						"Take away Index pages from the data pages and you get <i>actual</i> usage.<br>" +
//						"<b>Formula</b>: DataPages - IndexPages<br>" +
//					"</html>");
//
//			mtd.addColumn("monOpenDatabases", "IndexPages",
//					"<html>" +
//						"This is 'index pages' output from the function <code>spaceusage(dbid)</code>.<br>" +
//						"<code>index pages</code> - index pages – number of index-only pages, if the input parameters specified processing indexes on the objects. To determine the number of pages used for only the index-level pages, subtract the number of large object (LOB) pages from the number of index pages.<br>" +
//						"<b>Formula</b>: function: spaceusage(dbid)<br>" +
////						"<b>Note</b>: if 'Spaceusage in MB' is checked, this will be in MB, check column 'RawSpaceUsage' for the <i>raw</i> values.<br>" +
//					"</html>");
//
//			mtd.addColumn("monOpenDatabases", "IndexPagesReal",
//					"<html>" +
//						"Take away LOB pages from the index pages and you get <i>actual</i> usage.<br>" +
//						"<b>Formula</b>: IndexPages - LobPages<br>" +
//					"</html>");
//
//			mtd.addColumn("monOpenDatabases", "LobPages",
//					"<html>" +
//						"This is 'LOB pages' output from the function <code>spaceusage(dbid)</code>.<br>" +
//						"<code>LOB pages</code> - number of off-row large object pages for which the index ID is 255.<br>" +
//                        "LOB pages returns a nonzero value only when you use spaceusage to determine the space metrics for all indexes, or only the LOB index, on objects that contain off-row LOB data. LOB pages returns 0 when you use spaceusage to examine the space metrics only for tables (which have index IDs of 0).<br>" +
//                        "When you run spaceusage against the <b>entire database</b>, LOB pages displays the aggregate page counts for all LOB columns occupying off-row storage in all objects.</li>" +
//						"<b>Formula</b>: function: spaceusage(dbid)<br>" +
////						"<b>Note</b>: if 'Spaceusage in MB' is checked, this will be in MB, check column 'RawSpaceUsage' for the <i>raw</i> values.<br>" +
//					"</html>");
//
//			mtd.addColumn("monOpenDatabases", "Tables",
//					"<html>" +
//						"This is 'tables' output from the function <code>spaceusage(dbid)</code>.<br>" +
//						"<code>tables</code> - total number of tables processed when you execute spaceusage and include only the database_id parameter (that is, when you are investigating space metrics for the entire database).<br>" +
//						"<b>Formula</b>: function: spaceusage(dbid)<br>" +
////						"<b>Note</b>: if 'Spaceusage in MB' is checked, this is <b>NOT</b> presenetd as MB.<br>" +
//					"</html>");
//
//			mtd.addColumn("monOpenDatabases", "RowCountSum",
//					"<html>" +
//						"This is 'row count' output from the function <code>spaceusage(dbid)</code>.<br>" +
//						"<code>row count</code> - number of rows in the object or partition. spaceusage reports this row count as 0 when you specify the index_id parameter.<br>" +
//						"<b>Formula</b>: function: spaceusage(dbid)<br>" +
////						"<b>Note</b>: if 'Spaceusage in MB' is checked, this is <b>NOT</b> presenetd as MB.<br>" +
//					"</html>");
//
//			mtd.addColumn("monOpenDatabases", "OamPages",
//					"<html>" +
//						"This is 'oam pages' output from the function <code>spaceusage(dbid)</code>.<br>" +
//						"<code>oam pages</code> - number of OAM pages for all OAM chains, as selected by the input parameters.<br>" +
//						"For example, if you specify:<br>" +
//						"<code>spaceusage(database_id, object_id, index_id)</code><br>" +
//						"oam pages indicates the number of OAM pages found for this index and any of its local index partitions. If you run spaceusage against a specific object, oam pages returns the amount of overhead for the extra pages used for this object’s space management.<br>" +
//						"When you execute spaceusage for an <b>entire database</i>, oam pages returns the total overhead for the number of OAM pages needed to track space across all objects, and their off-row LOB columns.<br>" +
//						"<b>Formula</b>: function: spaceusage(dbid)<br>" +
////						"<b>Note</b>: if 'Spaceusage in MB' is checked, this will be in MB, check column 'RawSpaceUsage' for the <i>raw</i> values.<br>" +
//					"</html>");
//
//			mtd.addColumn("monOpenDatabases", "AllocationUnits",
//					"<html>" +
//						"This is 'allocation units' output from the function <code>spaceusage(dbid)</code>.<br>" +
//						"<code>allocation units</code> - number of allocation units that hold one or more extents for the specified object, index, or partition. allocation units indicates how many allocation units (or pages) Adaptive Server must scan while accessing all the pages of that object, index, or partition.<br>" +
//						"When you run spaceusage against the <b>entire database</b>, allocation units returns the total number of allocation units reserving space for an object. However, because Adaptive Server can share allocation units across objects, this field might show a number greater than the total number of allocation units in the entire database.<br>" +
//						"<b>Formula</b>: function: spaceusage(dbid)<br>" +
////						"<b>Note</b>: if 'Spaceusage in MB' is checked, this is <b>NOT</b> presenetd as MB.<br>" +
//					"</html>");
//
//			mtd.addColumn("monOpenDatabases", "SyslogPages",
//					"<html>" +
//						"This is 'syslog pages' output from the function <code>spaceusage(dbid)</code>.<br>" +
//						"<code>syslog pages</code> - Currently, spaceusage does not report on syslogs.<br>" +
//						"<b>Formula</b>: function: spaceusage(dbid)<br>" +
////						"<b>Note</b>: if 'Spaceusage in MB' is checked, this will be in MB, check column 'RawSpaceUsage' for the <i>raw</i> values.<br>" +
//					"</html>");
//
//			mtd.addColumn("monOpenDatabases", "LastDbBackupAgeInHours",
//					"<html>" +
//						"Number of hours since last database backup/dump was done.<br>" +
//						"-1 if 'dump database...' has ever been done.<br>" +
//						"<b>Formula</b>: datediff(hour, x.BackupStartTime, getdate())<br>" +
//					"</html>");
//
//			mtd.addColumn("monOpenDatabases", "LastLogBackupAgeInHours",
//					"<html>" +
//						"Number of hours since last transaction log backup/dump was done.<br>" +
//						"-1 if 'dump tran...' has ever been done.<br>" +
//						"<b>Formula</b>: datediff(hour, x.LastTranLogDumpTime, getdate())<br>" +
//					"</html>");
//
//		}
//		catch (NameNotFoundException e) {/*ignore*/}
//	}

	@Override
	public void addMonTableDictForVersion(Connection conn, long srvVersion, boolean isClusterEnabled)
	{
		try 
		{
			MonTablesDictionary mtd = MonTablesDictionaryManager.getInstance();

			String cmName = this.getName();
			mtd.addTable(cmName, HTML_DESC);

			mtd.addColumn(cmName, "DBName"                  ,"<html>Name of the database</html>");
			mtd.addColumn(cmName, "database_id"             ,"<html>ID of the database</html>");
			mtd.addColumn(cmName, "compatibility_level"     ,"<html>Integer corresponding to the version of SQL Server for which behavior is compatible. (same as sys.database.compatibility_level)</html>");
			mtd.addColumn(cmName, "user_access_desc"        ,"<html>User-access setting, MULTI_USER, SINGLE_USER , RESTRICTED_USER. (same as sys.database.compatibility_level)</html>");
			mtd.addColumn(cmName, "state_desc"              ,"<html>Description of the database state: ONLINE, RESTORING, RECOVERING, RECOVERY_PENDING, SUSPECT, EMERGENCY, OFFLINE, COPYING, OFFLINE_SECONDARY (same as sys.database.state_desc)</html>");
			mtd.addColumn(cmName, "recovery_model_desc"     ,"<html>Description of recovery model selected: FULL, BULK_LOGGED, SIMPLE. (same as sys.database.recovery_model_desc)</html>");
			mtd.addColumn(cmName, "ag_name"                 ,"<html>Availability Group Name</html>");
			mtd.addColumn(cmName, "ag_role"                 ,"<html>Availability Group Role</html>");
			mtd.addColumn(cmName, "ag_primary_server"       ,"<html>Primary Server of the Availability Group (null if none)</html>");
			mtd.addColumn(cmName, "DataFileGroupCount"      ,"<html>How many DATA <i>OS files</i> does the database consist of. Typically just ONE...</html>");
			mtd.addColumn(cmName, "DBOwner"                 ,"<html>Username that ownes the database</html>");
			mtd.addColumn(cmName, "log_reuse_wait"          ,"<html>Reuse of transaction log space depends on <i>this</i>, see 'log_reuse_wait_desc' for a text value (same as sys.database.log_reuse_wait)</html>");
			mtd.addColumn(cmName, "log_reuse_wait_desc"     ,"<html>Reuse of transaction log space depends on <i>this</i>, for example if 'LOG_BACKUP' = We need do a 'backup tran' before the tran log space can be truncated or re-used. (same as sys.database.log_reuse_wait_desc)</html>");

			mtd.addColumn(cmName, "DbSizeInMb"              ,"<html>Total Allocated Size in MB of the database (both data and log). Note: there are probably free space within the total size. (see: 'DataSizeUsedPct/DataSizeUsedInMb/DataSizeFreeInMb' and 'LogSizeUsedPct/LogSizeUsedInMb/LogSizeFreeInMb')</html>");
			mtd.addColumn(cmName, "LogSizeInMb"             ,"<html>Total Allocated Size of the transaction log.</html>");                                                         //  = log.totalLogSizeMb 
			mtd.addColumn(cmName, "DataSizeInMb"            ,"<html>Total Allocated Size of the <i>data</i> part.</html>");                                                        //   = data.totalDataSizeMb 
			mtd.addColumn(cmName, "LogSizeUsedPct"          ,"<html>How much of the allocated LOG space is actually used.</html>");                                                //   = log.usedLogSpaceInPct 
			mtd.addColumn(cmName, "DataSizeUsedPct"         ,"<html>How much of the allocated DATA space is actually used.</html>");                                               //   = data.usedDataPct 
			mtd.addColumn(cmName, "LogOsDiskUsedPct"        ,"<html>On the Operating System, where the LOG file(s) are located. How much space is <b>used</b>.</html>");           //   = osvLog.osUsedPct 
			mtd.addColumn(cmName, "DataOsDiskUsedPct"       ,"<html>On the Operating System, where the DATA file(s) are located. How much space is <b>used</b>.</html>");          //   = osvData.osUsedPct 

			mtd.addColumn(cmName, "LogSizeUsedInMb"         ,"<html>How much of the allocated LOG space is actually used.</html>");                                                //   = log.usedLogSpaceInMb 
			mtd.addColumn(cmName, "LogSizeFreeInMb"         ,"<html>How much of the allocated LOG space is NOT used.</html>");                                                     //   = log.totalLogSizeMb - log.usedLogSpaceInMb 
			mtd.addColumn(cmName, "LogSizeUsedInMbDiff"     ,"<html>Same as 'LogSizeUsedInMb', but this column is diff calculated to see how fast it grows/shrinks.</html>");      //   = log.usedLogSpaceInMb 
			mtd.addColumn(cmName, "LogSizeFreeInMbDiff"     ,"<html>Same as 'LogSizeFreeInMb', but this column is diff calculated to see how fast it grows/shrinks.</html>");      //   = log.totalLogSizeMb - log.usedLogSpaceInMb 

			mtd.addColumn(cmName, "LogOsDisk"               ,"<html>Drive or <i>mount point</i> where the LOG files are located</html>");                                          //   = osvLog.volume_mount_point 
			mtd.addColumn(cmName, "LogOsDiskUsedPct"        ,"<html>How much of the Operating System <i>disk/mount point</i> are USED in Percent</html>");                         //   = osvLog.osUsedPct 
			mtd.addColumn(cmName, "LogOsDiskFreePct"        ,"<html>How much of the Operating System <i>disk/mount point</i> are FREE in Percent</html>");                         //   = osvLog.osFreePct 
			mtd.addColumn(cmName, "LogOsFileName"           ,"<html>Physical name on Operating System. <br>NOTE: Only the <b>first</b> file is visible here.</html>");             //   = osvLog.physical_name 
			mtd.addColumn(cmName, "LogFileName"             ,"<html>SQL-Servers internal name if the <i>file</i>. <br>NOTE: Only the <b>first</b> file is visible here.</html>");  //   = osvLog.name 
			mtd.addColumn(cmName, "logFileId"               ,"<html>SQL-Servers internal ID if the <i>file</i>. <br>NOTE: Only the <b>first</b> file is visible here.</html>");    //   = osvLog.file_id 
			mtd.addColumn(cmName, "LogOsDiskUsedMb"         ,"<html>How much of the Operating System <i>disk/mount point</i> are USED in MB</html>");                              //   = osvLog.osUsedMb 
			mtd.addColumn(cmName, "LogOsDiskFreeMb"         ,"<html>How much of the Operating System <i>disk/mount point</i> are FREE in MB</html>");                              //   = osvLog.osFreeMb 
			mtd.addColumn(cmName, "LogNextGrowthSizeMb"     ,"<html>Next time the database needs to grow the LOG file on the Operating System, how many MB will we grow.</html>"); //   = osvLog.nextGrowSizeMb 

			mtd.addColumn(cmName, "DataSizeUsedInMb"        ,"<html>How much of the allocated DATA space is actually used.</html>");                                               //   = data.usedDataMb 
			mtd.addColumn(cmName, "DataSizeFreeInMb"        ,"<html>How much of the allocated DATA space is NOT used.</html>");                                                    //   = data.freeDataMb 
			mtd.addColumn(cmName, "DataSizeUsedInMbDiff"    ,"<html>Same as 'DataSizeUsedInMb', but this column is diff calculated to see how fast it grows/shrinks.</html>");     //   = data.usedDataMb 
			mtd.addColumn(cmName, "DataSizeFreeInMbDiff"    ,"<html>Same as 'DataSizeFreeInMb', but this column is diff calculated to see how fast it grows/shrinks.</html>");     //   = data.freeDataMb 

			mtd.addColumn(cmName, "DataOsDisk"              ,"<html>Drive or <i>mount point</i> where the DATA files are located</html>");                                         //   = osvData.volume_mount_point 
			mtd.addColumn(cmName, "DataOsDiskUsedPct"       ,"<html>How much of the Operating System <i>disk/mount point</i> are USED in Percent</html>");                         //   = osvData.osUsedPct 
			mtd.addColumn(cmName, "DataOsDiskFreePct"       ,"<html>How much of the Operating System <i>disk/mount point</i> are FREE in Percent</html>");                         //   = osvData.osFreePct 
			mtd.addColumn(cmName, "DataOsFileName"          ,"<html>Physical name on Operating System. <br>NOTE: Only the <b>first</b> file is visible here.</html>");             //   = osvData.physical_name 
			mtd.addColumn(cmName, "DataFileName"            ,"<html>SQL-Servers internal name if the <i>file</i>. <br>NOTE: Only the <b>first</b> file is visible here.</html>");  //   = osvData.name 
			mtd.addColumn(cmName, "DataFileId"              ,"<html>SQL-Servers internal ID if the <i>file</i>. <br>NOTE: Only the <b>first</b> file is visible here.</html>");    //   = osvData.file_id 
			mtd.addColumn(cmName, "DataOsDiskUsedMb"        ,"<html>How much of the Operating System <i>disk/mount point</i> are USED in MB</html>");                              //   = osvData.osUsedMb 
			mtd.addColumn(cmName, "DataOsDiskFreeMb"        ,"<html>How much of the Operating System <i>disk/mount point</i> are FREE in MB</html>");                              //   = osvData.osFreeMb 
			mtd.addColumn(cmName, "DataNextGrowthSizeMb"    ,"<html>Next time the database needs to grow the DATA file on the Operating System, how many MB will we grow.</html>");//   = osvData.nextGrowSizeMb 

			mtd.addColumn(cmName, "OldestTranStartTime"     ,"<html>Start time of the oldest transaction in this database.</html>");                                               //   = oti.last_request_start_time 
			mtd.addColumn(cmName, "OldestTranWaitType"      ,"<html>What are the oldest transaction waiting for?</html>");                                                         //   = oti.wait_type 
			mtd.addColumn(cmName, "OldestTranECT"           ,"<html>Estimated Completion Time of the oldest transaction.</html>");                                                 //   = oti.estimated_completion_time 
			mtd.addColumn(cmName, "OldestTranInSeconds"     ,"<html>Seconds since the oldest transaction started.</html>");                                                        //   = datediff(second, oti.last_request_start_time, getdate()) 
			mtd.addColumn(cmName, "OldestTranName"          ,"<html>Name of the oldest transaction</html>");                                                                       //   = -1 
			mtd.addColumn(cmName, "OldestTranSpid"          ,"<html>What SPID is responsible or started the oldest transaction</html>");                                           //   = oti.session_id 
			mtd.addColumn(cmName, "OldestTranProg"          ,"<html>What <i>program name</i> is responsible or started the oldest transaction</html>");                            //   = oti.program_name 
			mtd.addColumn(cmName, "OldestTranUser"          ,"<html>Name of the <i>user</i> that is responsible or started the oldest transaction</html>");                        //   = oti.login_name 
			mtd.addColumn(cmName, "OldestTranHost"          ,"<html>Name of the <i>host</i> that is responsible or started the oldest transaction</html>");                        //   = oti.host_name 
			mtd.addColumn(cmName, "OldestTranHasSqlText"    ,"<html>SQL Text of the oldest open transaction</html>");                                                              //   = CASE WHEN oti.most_recent_sql_text is not null THEN convert(bit,1) ELSE convert(bit,0) END 
			mtd.addColumn(cmName, "OldestTranHasShowPlan"   ,"<html>Showplan Text of the oldest open transaction</html>");                                                         //   = CASE WHEN oti.plan_text            is not null THEN convert(bit,1) ELSE convert(bit,0) END 

			mtd.addColumn(cmName, "LastDbBackupTime"        ,"<html>Last Date/time a backup was done.</html>");                                                                    //   =         (SELECT bi.last_backup_finish_date                            FROM #backupInfo bi WHERE d.name = bi.database_name AND bi.type = 'D') 
			mtd.addColumn(cmName, "LastDbBackupAgeInHours"  ,"<html>How many hours ago was the last backup taken.</html>");                                                        //   = isnull( (SELECT datediff(hour, bi.last_backup_finish_date, getdate()) FROM #backupInfo bi WHERE d.name = bi.database_name AND bi.type = 'D'), -1) 
			mtd.addColumn(cmName, "LastLogBackupTime"       ,"<html>Last Date/time a LOG backup was done.</html>");                                                                //   =         (SELECT bi.last_backup_finish_date                            FROM #backupInfo bi WHERE d.name = bi.database_name AND bi.type = 'L') 
			mtd.addColumn(cmName, "LastLogBackupAgeInHours" ,"<html>How many hours ago was the last LOG backup taken.</html>");                                                    //   = isnull( (SELECT datediff(hour, bi.last_backup_finish_date, getdate()) FROM #backupInfo bi WHERE d.name = bi.database_name AND bi.type = 'L'), -1) 

			mtd.addColumn(cmName, "OldestTranSqlText"       ,"<html>SQL Text of the oldest open transaction</html>");                                                              //   = oti.most_recent_sql_text 
			mtd.addColumn(cmName, "OldestTranShowPlanText"  ,"<html>Showplan Text of the oldest open transaction</html>");                                                         //   = oti.plan_text 
		}
		catch (NameNotFoundException e) {/*ignore*/}
	}

	@Override
	public List<String> getPkForVersion(Connection conn, long srvVersion, boolean isAzure)
	{
		List <String> pkCols = new LinkedList<String>();

		pkCols.add("database_id");

		return pkCols;
	}

	/** Used by the: Create 'Offline Session' Wizard */
	@Override
	public List<CmSettingsHelper> getLocalSettings()
	{
		Configuration conf = Configuration.getCombinedConfiguration();
		List<CmSettingsHelper> list = new ArrayList<>();
		
		list.add(new CmSettingsHelper("Sample Showplan on Open Trans", PROPKEY_sample_showplan   , Boolean.class, conf.getBooleanProperty(PROPKEY_sample_showplan   , DEFAULT_sample_showplan   ), DEFAULT_sample_showplan  , "Get sp_showplan on on SPID's that has an open transaction." ));
		list.add(new CmSettingsHelper("Sample SQL Text on Open Trans", PROPKEY_sample_monSqlText , Boolean.class, conf.getBooleanProperty(PROPKEY_sample_monSqlText , DEFAULT_sample_monSqlText ), DEFAULT_sample_monSqlText, "Get SQL Text (from monProcessSQLText) on on SPID's that has an open transaction" ));

		return list;
	}


	@Override
	public String getSqlForVersion(Connection conn, long srvVersion, boolean isAzure)
	{
		// Table names are probably different in Normal SQL-Server and Azure SQL-Server
		String dm_db_file_space_usage      = "sys.dm_db_file_space_usage";
		String dm_db_log_space_usage       = "sys.dm_db_log_space_usage";
		String dm_exec_sessions            = "sys.dm_exec_sessions";
		String dm_exec_connections         = "sys.dm_exec_connections";
		String dm_exec_requests            = "sys.dm_exec_requests";
		String dm_tran_active_transactions = "sys.dm_tran_active_transactions";  // NOTE: not yet used
		String dm_exec_sql_text            = "sys.dm_exec_sql_text";
		String dm_exec_query_plan          = "sys.dm_exec_query_plan";
		String master_files                = "sys.master_files";
		String dm_os_volume_stats          = "sys.dm_os_volume_stats";
		String databases                   = "sys.databases";
		String backupset                   = "msdb.dbo.backupset";

		if (isAzure)
		{
			dm_db_file_space_usage      = "sys.dm_pdw_nodes_db_file_space_usage";
			dm_db_log_space_usage       = "sys.dm_db_log_space_usage";               // same as Normal SQL-Server
			dm_exec_sessions            = "sys.dm_pdw_nodes_exec_sessions";
			dm_exec_connections         = "sys.dm_pdw_exec_connections";
			dm_exec_requests            = "sys.dm_exec_requests";                    // same as Normal SQL-Server
			dm_tran_active_transactions = "sys.dm_pdw_nodes_tran_active_transactions";
			dm_exec_sql_text            = "sys.dm_exec_sql_text";                    // same as Normal SQL-Server
			dm_exec_query_plan          = "sys.dm_exec_query_plan";                  // same as Normal SQL-Server
			master_files                = "sys.master_files";                        // same as Normal SQL-Server
			dm_os_volume_stats          = "sys.dm_os_volume_stats";                  // same as Normal SQL-Server
			databases                   = "sys.databases";                           // same as Normal SQL-Server
			backupset                   = "msdb.dbo.backupset";                      // same as Normal SQL-Server
		}

		String availabilityGroupName          = "";
		String availabilityGroupRole          = "";
		String availabilityGroupPrimaryServer = "";
		String whereAvailabilityGroup         = "";
		
		if (srvVersion >= Ver.ver(2012) || isAzure)
		{
			availabilityGroupName          = "    ,ag_name                  = (SELECT ag.name             FROM sys.availability_replicas ar JOIN sys.availability_groups                ag ON ar.group_id = ag.group_id  WHERE ar.replica_id  = d.replica_id) \n"; 
			availabilityGroupRole          = "    ,ag_role                  = (SELECT ars.role_desc       FROM sys.dm_hadr_availability_replica_states ars                                                               WHERE ars.replica_id = d.replica_id) \n";
			availabilityGroupPrimaryServer = "    ,ag_primary_server        = (SELECT ags.primary_replica FROM sys.availability_replicas ar JOIN sys.dm_hadr_availability_group_states ags ON ar.group_id = ags.group_id WHERE ar.replica_id  = d.replica_id) \n";
			whereAvailabilityGroup         = "  OR d.replica_id is not null \n";
		}

//		String sql = ""
//			    + " \n"
//			    + "--------------------------- \n"
//			    + "-- DATA SIZE MB \n"
//			    + "--------------------------- \n"
//			    + "DECLARE @dataSizeMb TABLE (database_id int, fileGroupCount int, totalDataSizeMb numeric(12,1), usedDataMb numeric(12,1), freeDataMb numeric(12,1), usedDataPct numeric(5,1), freeDataPct numeric(5,1)) \n"
//			    + "INSERT INTO @dataSizeMb \n"
//			    + "EXEC sp_MSforeachdb ' \n"
//			    + "SELECT \n"
//			    + "     database_id \n"
//			    + "    ,fileGroupCount = count(*) \n"
//			    + "    ,totalMb        = sum(total_page_count) / 128.0 \n"
//			    + "    ,allocatedMb    = sum(allocated_extent_page_count) / 128.0 \n"
//			    + "    ,unallocatedMb  = sum(unallocated_extent_page_count) / 128.0 \n"
//			    + "    ,usedPct        = (sum(allocated_extent_page_count)  *1.0) / (sum(total_page_count)*1.0) * 100.0 \n"
//			    + "    ,freePct        = (sum(unallocated_extent_page_count)*1.0) / (sum(total_page_count)*1.0) * 100.0 \n"
//			    + "FROM ?." + dm_db_file_space_usage + " GROUP BY database_id' \n"
//			    + " \n"
//			    + "--------------------------- \n"
//			    + "-- LOG SIZE MB \n"
//			    + "--------------------------- \n"
//			    + "DECLARE @logSizeMb TABLE (database_id int, totalLogSizeMb int, usedLogSpaceInMb int, usedLogSpaceInPct numeric(5,1), logSpaceInMbSinceLastBackup int) \n"
//			    + "INSERT INTO @logSizeMb \n"
//			    + "EXEC sp_MSforeachdb ' \n"
//			    + "SELECT \n"
//			    + "     database_id \n"
//			    + "    ,total_log_size_in_bytes/1024/1024 \n"
//			    + "    ,used_log_space_in_bytes/1024/1024 \n"
//			    + "    ,used_log_space_in_percent \n"
//			    + "    ,log_space_in_bytes_since_last_backup/1024/1024 \n"
//			    + "FROM ?." + dm_db_log_space_usage + "' \n"
//			    + " \n"
//			    + "--------------------------- \n"
//			    + "-- Backup Info \n"
//			    + "--------------------------- \n"
//			    + "DECLARE @backupInfo TABLE (database_name nvarchar(128), type char(2), last_backup_finish_date datetime) \n"
//			    + "INSERT INTO @backupInfo \n"
//			    + "    SELECT \n"
//			    + "         bus.database_name \n"
//			    + "        ,bus.type \n"
//			    + "        ,last_backup_finish_date = MAX(bus.backup_finish_date) \n"
//			    + "    FROM msdb.dbo.backupset bus \n"
//			    + "    GROUP BY bus.database_name, bus.type \n"
//			    + "; --- we need a ';' here for SQL-Server to use the Table Variables in the below Statement \n"
//			    + " \n"
//			    + " \n"
//			    + "WITH \n"
////			    + "--------------------------- \n"
////			    + "-- Backup Info \n"
////			    + "--------------------------- \n"
////			    + "bi AS ( \n"
////			    + "    SELECT \n"
////			    + "         bus.database_name \n"
////			    + "        ,bus.type \n"
////			    + "        ,last_backup_finish_date = MAX(bus.backup_finish_date) \n"
////			    + "    FROM " + backupset + " bus \n"
////			    + "    GROUP BY bus.database_name, bus.type \n"
////			    + "), \n"
//			    + "--------------------------- \n"
//			    + "-- Open Transaction Info \n"
//			    + "--------------------------- \n"
//			    + "oti AS ( \n"
//			    + "    SELECT --top 1 \n"
//			    + "         es.session_id \n"
//			    + "        ,es.database_id \n"
//			    + "        ,es.status \n"
//			    + "        ,es.program_name \n"
//			    + "        ,es.login_name \n"
//			    + "        ,es.host_name \n"
//			    + "        ,es.open_transaction_count \n"
//			    + "        ,es.last_request_start_time \n"
//			    + "        ,es.last_request_end_time \n"
//			    + "        ,er.statement_start_offset \n"
//			    + "        ,er.statement_end_offset \n"
//			    + "        ,er.wait_type \n"
//			    + "        ,er.estimated_completion_time \n"
////			    + ",at.transaction_id \n"           // possibly to get active transaction, needs to be tested/investigated
////			    + ",at.name AS tran_name \n"        // possibly to get active transaction, needs to be tested/investigated
////			    + ",at.transaction_begin_time \n"   // possibly to get active transaction, needs to be tested/investigated
//				+ "        ,SUBSTRING(sql_text.text, er.statement_start_offset / 2,  \n"
//				+ "             ( CASE WHEN er.statement_end_offset = -1  \n"
//				+ "                    THEN DATALENGTH(sql_text.text)  \n"
//				+ "                    ELSE er.statement_end_offset  \n"
//				+ "               END - er.statement_start_offset ) / 2) AS most_recent_sql_text \n"
//			    + "        ,plan_text.query_plan as plan_text \n"
//			    + "        ,ROW_NUMBER() OVER (PARTITION BY es.database_id ORDER BY es.last_request_start_time) AS row_num \n"
//			    + "    FROM " + dm_exec_sessions + " es \n"
//			    + "    JOIN " + dm_exec_connections + " ec            ON es.session_id = ec.session_id \n"
//			    + "    LEFT OUTER JOIN " + dm_exec_requests + " er    ON er.session_id = es.session_id \n"
////			    + "LEFT OUTER JOIN " + dm_tran_active_transactions + " at ON er.transaction_id = at.transaction_id \n"  // possibly to get active transaction, needs to be tested/investigated
//			    + "    OUTER APPLY " + dm_exec_sql_text + " (ec.most_recent_sql_handle) AS sql_text \n"
//			    + "    OUTER APPLY " + dm_exec_query_plan + " (er.plan_handle)          AS plan_text \n"
//			    + "    WHERE es.open_transaction_count > 0 \n"
//			    + "    --AND es.status = 'sleeping' \n"
//			    + "      AND es.is_user_process = 1 \n"
//			    + "), \n"
//			    + "--------------------------- \n"
//			    + "-- Operating System Volume DATA \n"
//			    + "--------------------------- \n"
//			    + "osvData as ( \n"
//			    + "    SELECT \n"
//			    + "         mf.database_id \n"
//			    + "        ,mf.file_id \n"
//			    + "        ,mf.size  as sizePg \n"
//			    + "        ,mf.size / 128 as sizeMb \n"
//			    + "        ,mf.max_size \n"
//			    + "        ,mf.growth \n"
//			    + "        ,mf.is_percent_growth \n"
//			    + "        , CASE WHEN mf.max_size = -1 AND mf.is_percent_growth = 1 THEN mf.size * (mf.growth / 100.0) / 128 --'PCT_GROW' \n"
//			    + "               WHEN mf.max_size = -1 AND mf.is_percent_growth = 0 THEN mf.growth / 128                     --'FIXED_GROW' \n"
//			    + "               WHEN mf.max_size > 0  AND mf.size < mf.max_size AND mf.is_percent_growth = 1 THEN mf.size * (mf.growth / 100.0) / 128 --'PCT_GROW   OK for MAX_SIZE' \n"
//			    + "               WHEN mf.max_size > 0  AND mf.size < mf.max_size AND mf.is_percent_growth = 0 THEN mf.growth / 128                     --'FIXED_GROW OK for MAX_SIZE' \n"
//			    + "               ELSE 0 \n"
//			    + "          END AS nextGrowSizeMb \n"
//			    + "        ,mf.type \n"
//			    + "        ,mf.type_desc \n"
//			    + "        ,mf.name \n"
//			    + "        ,mf.physical_name \n"
//			    + "        ,osv.volume_mount_point \n"
//			    + "        ,osv.logical_volume_name \n"
//			    + "        ,osv.total_bytes / 1024 / 1024                         AS osTotalMb \n"
//			    + "        ,osv.available_bytes / 1024 / 1024                     AS osFreeMb \n"
//			    + "        ,(osv.total_bytes - osv.available_bytes) / 1024 / 1024 AS osUsedMb \n" 
//			    + "        ,convert(numeric(5,1), (osv.available_bytes*1.0) / (osv.total_bytes*1.0) * 100.0)           AS osFreePct \n"
//			    + "        ,convert(numeric(5,1), 100.0 - ((osv.available_bytes*1.0) / (osv.total_bytes*1.0) * 100.0)) AS osUsedPct \n" 
//			    + "        ,ROW_NUMBER() OVER (PARTITION BY mf.database_id ORDER BY osv.available_bytes)               AS row_num \n"
//			    + "    FROM " + master_files + " AS mf \n"
//			    + "    CROSS APPLY " + dm_os_volume_stats + " (mf.database_id, mf.file_id) AS osv \n"
//			    + "    WHERE mf.type IN (0) /*0=ROWS, 0=LOG*/ \n"
//			    + "), \n"
//			    + "--------------------------- \n"
//			    + "-- Operating System Volume LOG \n"
//			    + "--------------------------- \n"
//			    + "osvLog as ( \n"
//			    + "    SELECT \n"
//			    + "         mf.database_id \n"
//			    + "        ,mf.file_id \n"
//			    + "        ,mf.size  as sizePg \n"
//			    + "        ,mf.size / 128 as sizeMb \n"
//			    + "        ,mf.max_size \n"
//			    + "        ,mf.growth \n"
//			    + "        ,mf.is_percent_growth \n"
//			    + "        , CASE WHEN mf.max_size = -1 AND mf.is_percent_growth = 1 THEN mf.size * (mf.growth / 100.0) / 128 --'PCT_GROW' \n"
//			    + "               WHEN mf.max_size = -1 AND mf.is_percent_growth = 0 THEN mf.growth / 128                     --'FIXED_GROW' \n"
//			    + "               WHEN mf.max_size > 0  AND mf.size < mf.max_size AND mf.is_percent_growth = 1 THEN mf.size * (mf.growth / 100.0) / 128 --'PCT_GROW   OK for MAX_SIZE' \n"
//			    + "               WHEN mf.max_size > 0  AND mf.size < mf.max_size AND mf.is_percent_growth = 0 THEN mf.growth / 128                     --'FIXED_GROW OK for MAX_SIZE' \n"
//			    + "               ELSE 0 \n"
//			    + "          END AS nextGrowSizeMb \n"
//			    + "        ,mf.type \n"
//			    + "        ,mf.type_desc \n"
//			    + "        ,mf.name \n"
//			    + "        ,mf.physical_name \n"
//			    + "        ,osv.volume_mount_point \n"
//			    + "        ,osv.logical_volume_name \n"
//			    + "        ,osv.total_bytes / 1024 / 1024                         AS osTotalMb \n"
//			    + "        ,osv.available_bytes / 1024 / 1024                     AS osFreeMb \n"
//			    + "        ,(osv.total_bytes - osv.available_bytes) / 1024 / 1024 AS osUsedMb \n" 
//			    + "        ,convert(numeric(5,1), (osv.available_bytes*1.0) / (osv.total_bytes*1.0) * 100.0)           AS osFreePct \n"
//			    + "        ,convert(numeric(5,1), 100.0 - ((osv.available_bytes*1.0) / (osv.total_bytes*1.0) * 100.0)) AS osUsedPct \n" 
//			    + "        ,ROW_NUMBER() OVER (PARTITION BY mf.database_id ORDER BY osv.available_bytes)               AS row_num \n"
//			    + "    FROM " + master_files + " AS mf \n"
//			    + "    CROSS APPLY " + dm_os_volume_stats + " (mf.database_id, mf.file_id) AS osv \n"
//			    + "    WHERE mf.type IN (1) /*0=ROWS, 0=LOG*/ \n"
//			    + ") \n"
//			    + "------------------------------- \n"
//			    + "-- The final select statement   \n"
//			    + "------------------------------- \n"
//			    + "SELECT \n"
//			    + "     DBName                   = d.Name \n"
//			    + "    ,d.database_id \n"
//			    + "    ,compatibility_level      = convert(int, d.compatibility_level) \n"
//			    + "    ,d.user_access_desc \n"
//			    + "    ,d.state_desc \n"
//			    + "    ,d.recovery_model_desc \n"
//			    + availabilityGroupName
//			    + availabilityGroupRole
//			    + availabilityGroupPrimaryServer
//			    + "    ,DataFileGroupCount       = data.fileGroupCount \n"
//			    + "    ,DBOwner                  = suser_name(d.owner_sid) \n"
//			    + "    ,d.log_reuse_wait \n"
//			    + "    ,d.log_reuse_wait_desc \n"
//			    + " \n"
//			    + "    ,DbSizeInMb               = data.totalDataSizeMb + log.totalLogSizeMb \n"
//			    + "    ,LogSizeInMb              = log.totalLogSizeMb \n"
//			    + "    ,DataSizeInMb             = data.totalDataSizeMb \n"
//			    + "    ,LogSizeUsedPct           = log.usedLogSpaceInPct \n"
//			    + "    ,DataSizeUsedPct          = data.usedDataPct \n"
////			    + "    ,LogOsDiskUsedPct         = osvLog.osUsedPct \n"
////			    + "    ,DataOsDiskUsedPct        = osvData.osUsedPct \n"
//			    + " \n"
//			    + "    ,LogSizeUsedInMb          = log.usedLogSpaceInMb \n"
//			    + "    ,LogSizeFreeInMb          = log.totalLogSizeMb - log.usedLogSpaceInMb \n"
//			    + "    ,LogSizeUsedInMbDiff      = log.usedLogSpaceInMb \n"
//			    + "    ,LogSizeFreeInMbDiff      = log.totalLogSizeMb - log.usedLogSpaceInMb \n"
//			    + " \n"
//			    + "    ,LogOsDisk                = osvLog.volume_mount_point \n"
//			    + "    ,LogOsDiskUsedPct         = osvLog.osUsedPct \n"
//			    + "    ,LogOsDiskFreePct         = osvLog.osFreePct \n"
//			    + "    ,LogOsFileName            = osvLog.physical_name \n"
//			    + "    ,LogFileName              = osvLog.name \n"
//			    + "    ,logFileId                = osvLog.file_id \n"
//			    + "    ,LogOsDiskUsedMb          = osvLog.osUsedMb \n"
//			    + "    ,LogOsDiskFreeMb          = osvLog.osFreeMb \n"
//			    + "    ,LogNextGrowthSizeMb      = osvLog.nextGrowSizeMb \n"
//			    + " \n"
//			    + "    ,DataSizeUsedInMb         = data.usedDataMb \n"
//			    + "    ,DataSizeFreeInMb         = data.freeDataMb \n"
//			    + "    ,DataSizeUsedInMbDiff     = data.usedDataMb \n"
//			    + "    ,DataSizeFreeInMbDiff     = data.freeDataMb \n"
//			    + " \n"
//			    + "    ,DataOsDisk               = osvData.volume_mount_point \n"
//			    + "    ,DataOsDiskUsedPct        = osvData.osUsedPct \n"
//			    + "    ,DataOsDiskFreePct        = osvData.osFreePct \n"
//			    + "    ,DataOsFileName           = osvData.physical_name \n"
//			    + "    ,DataFileName             = osvData.name \n"
//			    + "    ,DataFileId               = osvData.file_id \n"
//			    + "    ,DataOsDiskUsedMb         = osvData.osUsedMb \n"
//			    + "    ,DataOsDiskFreeMb         = osvData.osFreeMb \n"
//			    + "    ,DataNextGrowthSizeMb     = osvData.nextGrowSizeMb \n"
//			    + " \n"
////			    + "--	,TransactionLogFull = -1 \n"
////			    + " \n"
//			    + "    ,OldestTranStartTime      = oti.last_request_start_time \n"
//			    + "    ,OldestTranWaitType       = oti.wait_type \n"
//			    + "    ,OldestTranECT            = oti.estimated_completion_time \n"
//			    + "    ,OldestTranInSeconds      = datediff(second, oti.last_request_start_time, getdate()) \n"
//			    + "    ,OldestTranName           = -1 \n"
//			    + "    ,OldestTranSpid           = oti.session_id \n"
//			    + "    ,OldestTranProg           = oti.program_name \n"
//			    + "    ,OldestTranUser           = oti.login_name \n"
//			    + "    ,OldestTranHost           = oti.host_name \n"
////			    + "--  ,OldestTranPage           = -1 \n"
////			    + "--  ,OldestTranProcName       = -1 \n"
//			    + "    ,OldestTranHasSqlText     = CASE WHEN oti.most_recent_sql_text is not null THEN convert(bit,1) ELSE convert(bit,0) END \n"
//			    + "    ,OldestTranHasShowPlan    = CASE WHEN oti.plan_text            is not null THEN convert(bit,1) ELSE convert(bit,0) END \n"
//			    + " \n"
//				+ "    ,LastDbBackupTime         =         (SELECT bi.last_backup_finish_date                            FROM @backupInfo bi WHERE d.name = bi.database_name AND bi.type = 'D') \n"
//				+ "    ,LastDbBackupAgeInHours   = isnull( (SELECT datediff(hour, bi.last_backup_finish_date, getdate()) FROM @backupInfo bi WHERE d.name = bi.database_name AND bi.type = 'D'), -1) \n"
//				+ "    ,LastLogBackupTime        =         (SELECT bi.last_backup_finish_date                            FROM @backupInfo bi WHERE d.name = bi.database_name AND bi.type = 'L') \n"
//				+ "    ,LastLogBackupAgeInHours  = isnull( (SELECT datediff(hour, bi.last_backup_finish_date, getdate()) FROM @backupInfo bi WHERE d.name = bi.database_name AND bi.type = 'L'), -1) \n"
//			    + " \n"
//			    + "    ,OldestTranSqlText        = oti.most_recent_sql_text \n"
//			    + "    ,OldestTranShowPlanText   = oti.plan_text \n"
//			    + "FROM " + databases + " d \n"
//			    + "LEFT OUTER JOIN oti              ON d.database_id = oti    .database_id and oti.row_num = 1 \n"
//			    + "LEFT OUTER JOIN @dataSizeMb data ON d.database_id = data   .database_id \n"
//			    + "LEFT OUTER JOIN @logSizeMb   log ON d.database_id = log    .database_id \n"
//			    + "LEFT OUTER JOIN osvData          ON d.database_id = osvData.database_id and osvData.row_num = 1 \n"
//			    + "LEFT OUTER JOIN osvLog           ON d.database_id = osvLog .database_id and osvLog .row_num = 1 \n"
//			    + "WHERE has_dbaccess(d.name) != 0 \n"
//			    + whereAvailabilityGroup
//			    + " \n"
//			    + "";

		String sql = ""
			    + "-- Drop any of the old temporary tables if they still exists \n" 
			    + "if (object_id('tempdb..#dataSizeMb') is not null) drop table #dataSizeMb \n" 
			    + "if (object_id('tempdb..#logSizeMb')  is not null) drop table #logSizeMb  \n"
			    + "if (object_id('tempdb..#backupInfo') is not null) drop table #backupInfo  \n"
			    + "if (object_id('tempdb..#oti')        is not null) drop table #oti  \n"
			    + "if (object_id('tempdb..#osvData')    is not null) drop table #osvData  \n"
			    + "if (object_id('tempdb..#osvLog')     is not null) drop table #osvLog  \n"
			    + "go \n"
			    + " \n"
			    + "--------------------------- \n"
			    + "-- DATA SIZE MB \n"
			    + "--------------------------- \n"
			    + "CREATE TABLE #dataSizeMb (database_id int, fileGroupCount int, totalDataSizeMb numeric(12,1), usedDataMb numeric(12,1), freeDataMb numeric(12,1), usedDataPct numeric(5,1), freeDataPct numeric(5,1)) \n"
			    + "INSERT INTO #dataSizeMb \n"
			    + "EXEC sp_MSforeachdb ' \n"
			    + "SELECT \n"
			    + "     database_id \n"
			    + "    ,fileGroupCount = count(*) \n"
			    + "    ,totalMb        = sum(total_page_count) / 128.0 \n"
			    + "    ,allocatedMb    = sum(allocated_extent_page_count) / 128.0 \n"
			    + "    ,unallocatedMb  = sum(unallocated_extent_page_count) / 128.0 \n"
			    + "    ,usedPct        = (sum(allocated_extent_page_count)  *1.0) / (sum(total_page_count)*1.0) * 100.0 \n"
			    + "    ,freePct        = (sum(unallocated_extent_page_count)*1.0) / (sum(total_page_count)*1.0) * 100.0 \n"
			    + "FROM ?." + dm_db_file_space_usage + " GROUP BY database_id' \n"
			    + " \n"
			    + "--------------------------- \n"
			    + "-- LOG SIZE MB \n"
			    + "--------------------------- \n"
			    + "CREATE TABLE #logSizeMb (database_id int, totalLogSizeMb int, usedLogSpaceInMb int, usedLogSpaceInPct numeric(5,1), logSpaceInMbSinceLastBackup int) \n"
			    + "INSERT INTO #logSizeMb \n"
			    + "EXEC sp_MSforeachdb ' \n"
			    + "SELECT \n"
			    + "     database_id \n"
			    + "    ,total_log_size_in_bytes/1024/1024 \n"
			    + "    ,used_log_space_in_bytes/1024/1024 \n"
			    + "    ,used_log_space_in_percent \n"
			    + "    ,log_space_in_bytes_since_last_backup/1024/1024 \n"
			    + "FROM ?." + dm_db_log_space_usage + "' \n"
			    + " \n"
			    + "--------------------------- \n"
			    + "-- Backup Info \n"
			    + "--------------------------- \n"
			    + "    SELECT \n"
			    + "         bus.database_name \n"
			    + "        ,bus.type \n"
			    + "        ,last_backup_finish_date = MAX(bus.backup_finish_date) \n"
			    + "    INTO #backupInfo \n"
			    + "    FROM " + backupset + " bus \n"
			    + "    GROUP BY bus.database_name, bus.type \n"
			    + " \n"
			    + " \n"
			    + "--------------------------- \n"
			    + "-- Open Transaction Info \n"
			    + "--------------------------- \n"
			    + "    SELECT --top 1 \n"
			    + "         es.session_id \n"
			    + "        ,es.database_id \n"
			    + "        ,es.status \n"
			    + "        ,es.program_name \n"
			    + "        ,es.login_name \n"
			    + "        ,es.host_name \n"
			    + "        ,es.open_transaction_count \n"
			    + "        ,es.last_request_start_time \n"
			    + "        ,es.last_request_end_time \n"
			    + "        ,er.statement_start_offset \n"
			    + "        ,er.statement_end_offset \n"
			    + "        ,er.wait_type \n"
			    + "        ,er.estimated_completion_time \n"
//			    + ",at.transaction_id \n"           // possibly to get active transaction, needs to be tested/investigated
//			    + ",at.name AS tran_name \n"        // possibly to get active transaction, needs to be tested/investigated
//			    + ",at.transaction_begin_time \n"   // possibly to get active transaction, needs to be tested/investigated
				+ "        ,SUBSTRING(sql_text.text, er.statement_start_offset / 2,  \n"
				+ "             ( CASE WHEN er.statement_end_offset = -1  \n"
				+ "                    THEN DATALENGTH(sql_text.text)  \n"
				+ "                    ELSE er.statement_end_offset  \n"
				+ "               END - er.statement_start_offset ) / 2) AS most_recent_sql_text \n"
			    + "        ,plan_text.query_plan as plan_text \n"
			    + "        ,ROW_NUMBER() OVER (PARTITION BY es.database_id ORDER BY es.last_request_start_time) AS row_num \n"
			    + "    INTO #oti \n"
			    + "    FROM " + dm_exec_sessions + " es \n"
			    + "    JOIN " + dm_exec_connections + " ec            ON es.session_id = ec.session_id \n"
			    + "    LEFT OUTER JOIN " + dm_exec_requests + " er    ON er.session_id = es.session_id \n"
//			    + "LEFT OUTER JOIN " + dm_tran_active_transactions + " at ON er.transaction_id = at.transaction_id \n"  // possibly to get active transaction, needs to be tested/investigated
			    + "    OUTER APPLY " + dm_exec_sql_text + " (ec.most_recent_sql_handle) AS sql_text \n"
			    + "    OUTER APPLY " + dm_exec_query_plan + " (er.plan_handle)          AS plan_text \n"
			    + "    WHERE es.open_transaction_count > 0 \n"
			    + "    --AND es.status = 'sleeping' \n"
			    + "      AND es.is_user_process = 1 \n"
			    + "      AND es.session_id != @@spid \n"
			    + "\n"
			    + "--------------------------- \n"
			    + "-- Operating System Volume DATA \n"
			    + "--------------------------- \n"
			    + "    SELECT \n"
			    + "         mf.database_id \n"
			    + "        ,mf.file_id \n"
			    + "        ,mf.size  as sizePg \n"
			    + "        ,mf.size / 128 as sizeMb \n"
			    + "        ,mf.max_size \n"
			    + "        ,mf.growth \n"
			    + "        ,mf.is_percent_growth \n"
			    + "        , CASE WHEN mf.max_size = -1 AND mf.is_percent_growth = 1 THEN mf.size * (mf.growth / 100.0) / 128 --'PCT_GROW' \n"
			    + "               WHEN mf.max_size = -1 AND mf.is_percent_growth = 0 THEN mf.growth / 128                     --'FIXED_GROW' \n"
			    + "               WHEN mf.max_size > 0  AND mf.size < mf.max_size AND mf.is_percent_growth = 1 THEN mf.size * (mf.growth / 100.0) / 128 --'PCT_GROW   OK for MAX_SIZE' \n"
			    + "               WHEN mf.max_size > 0  AND mf.size < mf.max_size AND mf.is_percent_growth = 0 THEN mf.growth / 128                     --'FIXED_GROW OK for MAX_SIZE' \n"
			    + "               ELSE 0 \n"
			    + "          END AS nextGrowSizeMb \n"
			    + "        ,mf.type \n"
			    + "        ,mf.type_desc \n"
			    + "        ,mf.name \n"
			    + "        ,mf.physical_name \n"
			    + "        ,osv.volume_mount_point \n"
			    + "        ,osv.logical_volume_name \n"
			    + "        ,osv.total_bytes / 1024 / 1024                         AS osTotalMb \n"
			    + "        ,osv.available_bytes / 1024 / 1024                     AS osFreeMb \n"
			    + "        ,(osv.total_bytes - osv.available_bytes) / 1024 / 1024 AS osUsedMb \n" 
			    + "        ,convert(numeric(5,1), (osv.available_bytes*1.0) / (osv.total_bytes*1.0) * 100.0)           AS osFreePct \n"
			    + "        ,convert(numeric(5,1), 100.0 - ((osv.available_bytes*1.0) / (osv.total_bytes*1.0) * 100.0)) AS osUsedPct \n" 
			    + "        ,ROW_NUMBER() OVER (PARTITION BY mf.database_id ORDER BY osv.available_bytes)               AS row_num \n"
			    + "    INTO #osvData \n"
			    + "    FROM " + master_files + " AS mf \n"
			    + "    CROSS APPLY " + dm_os_volume_stats + " (mf.database_id, mf.file_id) AS osv \n"
			    + "    WHERE mf.type IN (0) /*0=ROWS, 0=LOG*/ \n"
			    + "\n"
			    + "--------------------------- \n"
			    + "-- Operating System Volume LOG \n"
			    + "--------------------------- \n"
			    + "    SELECT \n"
			    + "         mf.database_id \n"
			    + "        ,mf.file_id \n"
			    + "        ,mf.size  as sizePg \n"
			    + "        ,mf.size / 128 as sizeMb \n"
			    + "        ,mf.max_size \n"
			    + "        ,mf.growth \n"
			    + "        ,mf.is_percent_growth \n"
			    + "        , CASE WHEN mf.max_size = -1 AND mf.is_percent_growth = 1 THEN mf.size * (mf.growth / 100.0) / 128 --'PCT_GROW' \n"
			    + "               WHEN mf.max_size = -1 AND mf.is_percent_growth = 0 THEN mf.growth / 128                     --'FIXED_GROW' \n"
			    + "               WHEN mf.max_size > 0  AND mf.size < mf.max_size AND mf.is_percent_growth = 1 THEN mf.size * (mf.growth / 100.0) / 128 --'PCT_GROW   OK for MAX_SIZE' \n"
			    + "               WHEN mf.max_size > 0  AND mf.size < mf.max_size AND mf.is_percent_growth = 0 THEN mf.growth / 128                     --'FIXED_GROW OK for MAX_SIZE' \n"
			    + "               ELSE 0 \n"
			    + "          END AS nextGrowSizeMb \n"
			    + "        ,mf.type \n"
			    + "        ,mf.type_desc \n"
			    + "        ,mf.name \n"
			    + "        ,mf.physical_name \n"
			    + "        ,osv.volume_mount_point \n"
			    + "        ,osv.logical_volume_name \n"
			    + "        ,osv.total_bytes / 1024 / 1024                         AS osTotalMb \n"
			    + "        ,osv.available_bytes / 1024 / 1024                     AS osFreeMb \n"
			    + "        ,(osv.total_bytes - osv.available_bytes) / 1024 / 1024 AS osUsedMb \n" 
			    + "        ,convert(numeric(5,1), (osv.available_bytes*1.0) / (osv.total_bytes*1.0) * 100.0)           AS osFreePct \n"
			    + "        ,convert(numeric(5,1), 100.0 - ((osv.available_bytes*1.0) / (osv.total_bytes*1.0) * 100.0)) AS osUsedPct \n" 
			    + "        ,ROW_NUMBER() OVER (PARTITION BY mf.database_id ORDER BY osv.available_bytes)               AS row_num \n"
			    + "    INTO #osvLog \n"
			    + "    FROM " + master_files + " AS mf \n"
			    + "    CROSS APPLY " + dm_os_volume_stats + " (mf.database_id, mf.file_id) AS osv \n"
			    + "    WHERE mf.type IN (1) /*0=ROWS, 0=LOG*/ \n"
			    + "go\n"
			    + "\n"
			    + "------------------------------- \n"
			    + "-- The final select statement   \n"
			    + "------------------------------- \n"
			    + "SELECT \n"
			    + "     DBName                   = d.Name \n"
			    + "    ,d.database_id \n"
			    + "    ,compatibility_level      = convert(int, d.compatibility_level) \n"
			    + "    ,d.user_access_desc \n"
			    + "    ,d.state_desc \n"
			    + "    ,d.recovery_model_desc \n"
			    + availabilityGroupName
			    + availabilityGroupRole
			    + availabilityGroupPrimaryServer
			    + "    ,DataFileGroupCount       = data.fileGroupCount \n"
			    + "    ,DBOwner                  = suser_name(d.owner_sid) \n"
			    + "    ,d.log_reuse_wait \n"
			    + "    ,d.log_reuse_wait_desc \n"
			    + " \n"
			    + "    ,DbSizeInMb               = data.totalDataSizeMb + log.totalLogSizeMb \n"
			    + "    ,LogSizeInMb              = log.totalLogSizeMb \n"
			    + "    ,DataSizeInMb             = data.totalDataSizeMb \n"
			    + "    ,LogSizeUsedPct           = log.usedLogSpaceInPct \n"
			    + "    ,DataSizeUsedPct          = data.usedDataPct \n"
//			    + "    ,LogOsDiskUsedPct         = osvLog.osUsedPct \n"
//			    + "    ,DataOsDiskUsedPct        = osvData.osUsedPct \n"
			    + " \n"
			    + "    ,LogSizeUsedInMb          = log.usedLogSpaceInMb \n"
			    + "    ,LogSizeFreeInMb          = log.totalLogSizeMb - log.usedLogSpaceInMb \n"
			    + "    ,LogSizeUsedInMbDiff      = log.usedLogSpaceInMb \n"
			    + "    ,LogSizeFreeInMbDiff      = log.totalLogSizeMb - log.usedLogSpaceInMb \n"
			    + " \n"
			    + "    ,LogOsDisk                = osvLog.volume_mount_point \n"
			    + "    ,LogOsDiskUsedPct         = osvLog.osUsedPct \n"
			    + "    ,LogOsDiskFreePct         = osvLog.osFreePct \n"
			    + "    ,LogOsFileName            = osvLog.physical_name \n"
			    + "    ,LogFileName              = osvLog.name \n"
			    + "    ,logFileId                = osvLog.file_id \n"
			    + "    ,LogOsDiskUsedMb          = osvLog.osUsedMb \n"
			    + "    ,LogOsDiskFreeMb          = osvLog.osFreeMb \n"
			    + "    ,LogNextGrowthSizeMb      = osvLog.nextGrowSizeMb \n"
			    + " \n"
			    + "    ,DataSizeUsedInMb         = data.usedDataMb \n"
			    + "    ,DataSizeFreeInMb         = data.freeDataMb \n"
			    + "    ,DataSizeUsedInMbDiff     = data.usedDataMb \n"
			    + "    ,DataSizeFreeInMbDiff     = data.freeDataMb \n"
			    + " \n"
			    + "    ,DataOsDisk               = osvData.volume_mount_point \n"
			    + "    ,DataOsDiskUsedPct        = osvData.osUsedPct \n"
			    + "    ,DataOsDiskFreePct        = osvData.osFreePct \n"
			    + "    ,DataOsFileName           = osvData.physical_name \n"
			    + "    ,DataFileName             = osvData.name \n"
			    + "    ,DataFileId               = osvData.file_id \n"
			    + "    ,DataOsDiskUsedMb         = osvData.osUsedMb \n"
			    + "    ,DataOsDiskFreeMb         = osvData.osFreeMb \n"
			    + "    ,DataNextGrowthSizeMb     = osvData.nextGrowSizeMb \n"
			    + " \n"
//			    + "--	,TransactionLogFull = -1 \n"
//			    + " \n"
			    + "    ,OldestTranStartTime      = oti.last_request_start_time \n"
			    + "    ,OldestTranWaitType       = oti.wait_type \n"
			    + "    ,OldestTranECT            = oti.estimated_completion_time \n"
			    + "    ,OldestTranInSeconds      = datediff(second, oti.last_request_start_time, getdate()) \n"
			    + "    ,OldestTranName           = -1 \n"
			    + "    ,OldestTranSpid           = oti.session_id \n"
			    + "    ,OldestTranProg           = oti.program_name \n"
			    + "    ,OldestTranUser           = oti.login_name \n"
			    + "    ,OldestTranHost           = oti.host_name \n"
//			    + "--  ,OldestTranPage           = -1 \n"
//			    + "--  ,OldestTranProcName       = -1 \n"
			    + "    ,OldestTranHasSqlText     = CASE WHEN oti.most_recent_sql_text is not null THEN convert(bit,1) ELSE convert(bit,0) END \n"
			    + "    ,OldestTranHasShowPlan    = CASE WHEN oti.plan_text            is not null THEN convert(bit,1) ELSE convert(bit,0) END \n"
			    + " \n"
				+ "    ,LastDbBackupTime         =         (SELECT bi.last_backup_finish_date                            FROM #backupInfo bi WHERE d.name = bi.database_name AND bi.type = 'D') \n"
				+ "    ,LastDbBackupAgeInHours   = isnull( (SELECT datediff(hour, bi.last_backup_finish_date, getdate()) FROM #backupInfo bi WHERE d.name = bi.database_name AND bi.type = 'D'), -1) \n"
				+ "    ,LastLogBackupTime        =         (SELECT bi.last_backup_finish_date                            FROM #backupInfo bi WHERE d.name = bi.database_name AND bi.type = 'L') \n"
				+ "    ,LastLogBackupAgeInHours  = isnull( (SELECT datediff(hour, bi.last_backup_finish_date, getdate()) FROM #backupInfo bi WHERE d.name = bi.database_name AND bi.type = 'L'), -1) \n"
			    + " \n"
			    + "    ,OldestTranSqlText        = oti.most_recent_sql_text \n"
			    + "    ,OldestTranShowPlanText   = oti.plan_text \n"
			    + "FROM " + databases + " d \n"
			    + "LEFT OUTER JOIN #oti            oti ON d.database_id = oti    .database_id and oti.row_num = 1 \n"
			    + "LEFT OUTER JOIN #dataSizeMb    data ON d.database_id = data   .database_id \n"
			    + "LEFT OUTER JOIN #logSizeMb      log ON d.database_id = log    .database_id \n"
			    + "LEFT OUTER JOIN #osvData    osvData ON d.database_id = osvData.database_id and osvData.row_num = 1 \n"
			    + "LEFT OUTER JOIN #osvLog      osvLog ON d.database_id = osvLog .database_id and osvLog .row_num = 1 \n"
			    + "WHERE has_dbaccess(d.name) != 0 \n"
			    + whereAvailabilityGroup
			    + "go \n"
			    + " \n"
			    + "-- Drop temporary tables \n" 
			    + "drop table #dataSizeMb \n" 
			    + "drop table #logSizeMb  \n"
			    + "drop table #backupInfo  \n"
			    + "drop table #oti  \n"
			    + "drop table #osvData  \n"
			    + "drop table #osvLog  \n"
			    + "go \n"
			    + "";

		return sql;
	}

	@Override
	public void updateGraphData(TrendGraphDataPoint tgdp)
	{
		// Get what databases should be part of the graphs
		Map<String, Integer> dbMap = DbSelectionForGraphsDialog.getDbsInGraphList(this);

		if (dbMap.isEmpty())
		{
			_logger.info("updateGraphData(): Skipping the graphName='"+tgdp.getName()+"' for cm='"+getName()+"', reason: No DB names are availiable in the mapped list. dbMap.isEmpty() == true");
			return;
		}
		
		if (GRAPH_NAME_DB_SIZE_MB.equals(tgdp.getName()))
		{
			// Write 1 "line" for every database
			Double[] dArray = new Double[dbMap.size()];
			String[] lArray = new String[dbMap.size()];
			int d = 0;
			for (int row : dbMap.values())
			{
				String dbname = this.getAbsString       (row, "DBName");
				Double dvalue = this.getAbsValueAsDouble(row, "DbSizeInMb");

				lArray[d] = dbname;
				dArray[d] = dvalue;
				d++;
			}

			// Set the values
			tgdp.setDataPoint(this.getTimestamp(), lArray, dArray);
		}

		if (GRAPH_NAME_LOGSIZE_LEFT_MB.equals(tgdp.getName()))
		{
			// Write 1 "line" for every database
			Double[] dArray = new Double[dbMap.size()];
			String[] lArray = new String[dbMap.size()];
			int d = 0;
			for (int row : dbMap.values())
			{
				String dbname = this.getAbsString       (row, "DBName");
				Double dvalue = this.getAbsValueAsDouble(row, "LogSizeFreeInMb");

				lArray[d] = dbname;
				dArray[d] = dvalue;
				d++;
			}

			// Set the values
			tgdp.setDataPoint(this.getTimestamp(), lArray, dArray);
		}

		if (GRAPH_NAME_LOGSIZE_USED_MB.equals(tgdp.getName()))
		{
			// Write 1 "line" for every database
			Double[] dArray = new Double[dbMap.size()];
			String[] lArray = new String[dbMap.size()];
			int d = 0;
			for (int row : dbMap.values())
			{
				String dbname = this.getAbsString       (row, "DBName");
				Double dvalue = this.getAbsValueAsDouble(row, "LogSizeUsedInMb");

				lArray[d] = dbname;
				dArray[d] = dvalue;
				d++;
			}

			// Set the values
			tgdp.setDataPoint(this.getTimestamp(), lArray, dArray);
		}

		if (GRAPH_NAME_LOGSIZE_USED_PCT.equals(tgdp.getName()))
		{
			// Write 1 "line" for every database
			Double[] dArray = new Double[dbMap.size()];
			String[] lArray = new String[dbMap.size()];
			int d = 0;
			for (int row : dbMap.values())
			{
				String dbname = this.getAbsString        (row, "DBName");
				Double dvalue = this.getDiffValueAsDouble(row, "LogSizeUsedPct");

				lArray[d] = dbname;
				dArray[d] = dvalue;
				d++;
			}

			// Set the values
			tgdp.setDataPoint(this.getTimestamp(), lArray, dArray);
		}

		if (GRAPH_NAME_DATASIZE_LEFT_MB.equals(tgdp.getName()))
		{
			// Write 1 "line" for every database
			Double[] dArray = new Double[dbMap.size()];
			String[] lArray = new String[dbMap.size()];
			int d = 0;
			for (int row : dbMap.values())
			{
				String dbname = this.getAbsString       (row, "DBName");
				Double dvalue = this.getAbsValueAsDouble(row, "DataSizeFreeInMb");

				lArray[d] = dbname;
				dArray[d] = dvalue;
				d++;
			}

			// Set the values
			tgdp.setDataPoint(this.getTimestamp(), lArray, dArray);
		}

		if (GRAPH_NAME_DATASIZE_USED_MB.equals(tgdp.getName()))
		{
			// Write 1 "line" for every database
			Double[] dArray = new Double[dbMap.size()];
			String[] lArray = new String[dbMap.size()];
			int d = 0;
			for (int row : dbMap.values())
			{
				String dbname = this.getAbsString        (row, "DBName");
				Double dvalue = this.getDiffValueAsDouble(row, "DataSizeUsedInMb");

				lArray[d] = dbname;
				dArray[d] = dvalue;
				d++;
			}

			// Set the values
			tgdp.setDataPoint(this.getTimestamp(), lArray, dArray);
		}

		if (GRAPH_NAME_DATASIZE_USED_PCT.equals(tgdp.getName()))
		{
			// Write 1 "line" for every database
			Double[] dArray = new Double[dbMap.size()];
			String[] lArray = new String[dbMap.size()];
			int d = 0;
			for (int row : dbMap.values())
			{
				String dbname = this.getAbsString        (row, "DBName");
				Double dvalue = this.getDiffValueAsDouble(row, "DataSizeUsedPct");

				lArray[d] = dbname;
				dArray[d] = dvalue;
				d++;
			}

			// Set the values
			tgdp.setDataPoint(this.getTimestamp(), lArray, dArray);
		}

//		if (GRAPH_NAME_OLDEST_TRAN_IN_SEC.equals(tgdp.getName()))
//		{
//			Double[] arr = new Double[1];
//			arr[0] = this.getAbsValueMax("OldestTranInSeconds"); // MAX
//			
//			// Set the values
//			tgdp.setDate(this.getTimestamp());
//			tgdp.setData(arr);
//		}

		if (GRAPH_NAME_TEMPDB_USED_MB.equals(tgdp.getName()))
		{
			int tempdbCount = 0;
			for (int row = 0; row < this.size(); row++) // NOTE: loop all table rows (no filters here, since we need to find all tempdb's)
			{
				boolean isUserTempdb = "true".equalsIgnoreCase(this.getAbsString(row, "IsUserTempdb"));
				String  dbname       = this.getAbsString(row, "DBName");

				if (isUserTempdb || dbname.equals("tempdb"))
					tempdbCount++;
			}

			// no databases found... do not do anything
			if (tempdbCount > 0)
			{
				// Write 1 "line" for every 'tempdb' database
				Double[] dArray = new Double[tempdbCount];
				String[] lArray = new String[tempdbCount];
				int d = 0;
				for (int row = 0; row < this.size(); row++)
				{
					boolean isUserTempdb = "true".equalsIgnoreCase(this.getAbsString(row, "IsUserTempdb"));
					String  dbname       = this.getAbsString(row, "DBName");

					if (isUserTempdb || dbname.equals("tempdb"))
					{
						Double dvalue = this.getAbsValueAsDouble(row, "DataSizeUsedInMb");

						lArray[d] = dbname;
						dArray[d] = dvalue;
						d++;
					}
				}

				// Set the values
				tgdp.setDataPoint(this.getTimestamp(), lArray, dArray);
			}
		}

		
		if (GRAPH_NAME_OS_DISK_FREE_MB.equals(tgdp.getName()))
		{
			updateOsDiskGraph(tgdp, "LogOsDiskFreeMb", "DataOsDiskFreeMb");
		}

		if (GRAPH_NAME_OS_DISK_USED_MB.equals(tgdp.getName()))
		{
			updateOsDiskGraph(tgdp, "LogOsDiskUsedMb", "DataOsDiskUsedMb");
		}

		if (GRAPH_NAME_OS_DISK_USED_PCT.equals(tgdp.getName()))
		{
			updateOsDiskGraph(tgdp, "LogOsDiskUsedPct", "DataOsDiskUsedPct");
		}

	}
	
	private void updateOsDiskGraph(TrendGraphDataPoint tgdp, String logColName, String dataColName)
	{
		Map<String, Double> diskMap = new LinkedHashMap<>();
		
		int LogOsDisk_pos           = this.findColumn("LogOsDisk");
		int LogOsFileName_pos       = this.findColumn("LogOsFileName");
		int LogColName_pos          = this.findColumn(logColName);
		
		int DataOsDisk_pos          = this.findColumn("DataOsDisk");
		int DataOsFileName_pos      = this.findColumn("DataOsFileName");
		int DataColName_pos         = this.findColumn(dataColName);

		// Error checking if columns exists
		if (LogOsDisk_pos  == -1 || LogOsFileName_pos  == -1 || LogColName_pos  == -1) { _logger.error(tgdp.getName() + ": Cant find some columns in dataset. LogOsDisk="  + LogOsDisk_pos  + ", LogOsFileName="  + LogOsFileName_pos  + ", " + logColName  + "=" + LogColName_pos ); return; }
		if (DataOsDisk_pos == -1 || DataOsFileName_pos == -1 || DataColName_pos == -1) { _logger.error(tgdp.getName() + ": Cant find some columns in dataset. DataOsDisk=" + DataOsDisk_pos + ", DataOsFileName=" + DataOsFileName_pos + ", " + dataColName + "=" + DataColName_pos); return; }

		// Loop all rows and store values (duplicates will be discarded) in a MAP
		for (int r=0; r<this.size(); r++)
		{
			String LogOsDisk           = this.getAbsString(r, LogOsDisk_pos);
			String LogOsFileName       = this.getAbsString(r, LogOsFileName_pos);
			Double LogColValue         = this.getAbsValueAsDouble(r, LogColName_pos);
			
			String DataOsDisk          = this.getAbsString(r, DataOsDisk_pos);
			String DataOsFileName      = this.getAbsString(r, DataOsFileName_pos);
			Double DataColValue        = this.getAbsValueAsDouble(r, DataColName_pos);

			if (_logger.isDebugEnabled())
			{
				_logger.debug("createDataset():GRAPH-OS-DISK: "+getName()+": "
						+ "LogOsDisk("         + LogOsDisk_pos         + ")='" + LogOsDisk         + "', "
						+ "LogOsFileName("     + LogOsFileName_pos     + ")='" + LogOsFileName     + "', "
						+ logColName + "("     + LogColName_pos        + ")='" + LogColValue       + "', "
						+ "DataOsDisk("        + DataOsDisk_pos        + ")='" + DataOsDisk        + "', "
						+ "DataOsFileName("    + DataOsFileName_pos    + ")='" + DataOsFileName    + "', "
						+ dataColName + "("    + DataColName_pos       + ")='" + DataColValue      + "'.");
			}

			String logOsName  = LogOsDisk;
			String dataOsName = DataOsDisk;
			
			if (StringUtil.isNullOrBlank(logOsName))
				logOsName = CounterControllerSqlServer.resolvFileNameToDirectory(LogOsFileName);

			if (StringUtil.isNullOrBlank(dataOsName))
				dataOsName = CounterControllerSqlServer.resolvFileNameToDirectory(DataOsFileName);
			
			diskMap.put(logOsName,  LogColValue);
			diskMap.put(dataOsName, DataColValue);
		}

		// Write 1 "line" for every OS Disk
		Double[] dArray = new Double[diskMap.size()];
		String[] lArray = new String[diskMap.size()];

		int d = 0;
		for (Entry<String, Double> entry : diskMap.entrySet())
		{
			lArray[d] = entry.getKey();
			dArray[d] = entry.getValue();
			d++;
		}

		// Set the values
		tgdp.setDataPoint(this.getTimestamp(), lArray, dArray);
	}

	@Override
	public Class<?> getColumnClass(int columnIndex)
	{
		// use CHECKBOX for column "HasShowPlan"
		String colName = getColumnName(columnIndex);

		if      ("OldestTranHasSqlText" .equals(colName)) return Boolean.class;
		else if ("OldestTranHasShowPlan".equals(colName)) return Boolean.class;
		else return super.getColumnClass(columnIndex);
	}

	@Override
	public String getToolTipTextOnTableCell(MouseEvent e, String colName, Object cellValue, int modelRow, int modelCol) 
	{
		// SQL TEXT
		if ("OldestTranHasSqlText".equals(colName))
		{
			// Find 'OldestTranSqlText' column, is so get it and set it as the tool tip
			int pos = findColumn("OldestTranSqlText");
			if (pos > 0)
			{
				Object cellVal = getValueAt(modelRow, pos);
				if (cellVal instanceof String)
					return (String) cellVal;
			}
		}
		if ("OldestTranSqlText".equals(colName))
		{
			return cellValue == null ? null : cellValue.toString();
		}
		
		// SHOWPLAN
		if ("OldestTranHasShowPlan".equals(colName))
		{
			// Find 'OldestTranShowPlanText' column, is so get it and set it as the tool tip
			int pos = findColumn("OldestTranShowPlanText");
			if (pos > 0)
			{
				Object cellVal = getValueAt(modelRow, pos);
				if (cellVal instanceof String)
				{
					//return (String) cellVal;
					return ToolTipSupplierSqlServer.createXmlPlanTooltip( (String) cellVal );
				}
			}
		}
		if ("OldestTranShowPlanText".equals(colName))
		{
//			return cellValue == null ? null : cellValue.toString();
			return cellValue == null ? null : ToolTipSupplierSqlServer.createXmlPlanTooltip(cellValue.toString());
		}
		
		
		return super.getToolTipTextOnTableCell(e, colName, cellValue, modelRow, modelCol);
	}
//	/** add HTML around the string, and translate linebreaks into <br> */
//	private String toHtmlString(String in, boolean breakLines)
//	{
//		String str = in;
//		str = str.replaceAll("<", "&lt;");
//		str = str.replaceAll(">", "&gt;");
//		if (breakLines)
//			str = StringUtil.makeApproxLineBreak(in, 150, 5, "\n");
//		str = str.replaceAll("\\n", "<br>");
//
//		return "<html><pre>" + str + "</pre></html>";
//	}

	@Override
	public void sendAlarmRequest()
	{
		if ( ! hasDiffData() )
			return;
		
		if ( ! AlarmHandler.hasInstance() )
			return;

		AlarmHandler alarmHandler = AlarmHandler.getInstance();
		
		CountersModel cm = this;
		String dbmsSrvName = cm.getServerName();

		boolean debugPrint = System.getProperty("sendAlarmRequest.debug", "false").equalsIgnoreCase("true");

		// If some databases (in Db/Log dump) is not available... we may still want to alarm, due to "any" reason
		// So add those databases to the below list, and make decitions after looping all databases
		List<String> examedDbList  = new ArrayList<>();
		
		for (int r=0; r<cm.getDiffRowCount(); r++)
		{
			String dbname = cm.getAbsString(r, "DBName");

			// add examined DB's to list... so we can detect missing databases, and make alarms *after* the db-loop is done 
			examedDbList.add(dbname);

			//-------------------------------------------------------
			// Long running transaction
			//-------------------------------------------------------
			if (isSystemAlarmsForColumnEnabledAndInTimeRange("OldestTranInSeconds"))
			{
				Double OldestTranInSeconds = cm.getAbsValueAsDouble(r, "OldestTranInSeconds");
				if (OldestTranInSeconds != null)
				{
					if (debugPrint || _logger.isDebugEnabled())
						System.out.println("##### sendAlarmRequest("+cm.getName()+"): dbname='"+dbname+"', OldestTranInSeconds='"+OldestTranInSeconds+"'.");

					int threshold = Configuration.getCombinedConfiguration().getIntProperty(PROPKEY_alarm_OldestTranInSeconds, DEFAULT_alarm_OldestTranInSeconds);
					if (OldestTranInSeconds.intValue() > threshold)
					{
						// Get OldestTranName
//						String OldestTranName = cm.getAbsString(r, "OldestTranName");
						String OldestTranName = "this-is-not-yet-implemented";
						
						// Get config 'skip some transaction names'
						String skipTranNameRegExp = Configuration.getCombinedConfiguration().getProperty(PROPKEY_alarm_OldestTranInSecondsSkipTranName, DEFAULT_alarm_OldestTranInSecondsSkipTranName);

						// send alarm, if...
						if (StringUtil.hasValue(skipTranNameRegExp) && StringUtil.hasValue(OldestTranName))
						{
							if ( ! OldestTranName.matches(skipTranNameRegExp) )
							{
								String extendedDescText = cm.toTextTableString(DATA_RATE, r);
								String extendedDescHtml = cm.toHtmlTableString(DATA_RATE, r, true, false, false);
								AlarmEvent ae = new AlarmEventLongRunningTransaction(cm, threshold, dbname, OldestTranInSeconds, OldestTranName);
								ae.setExtendedDescription(extendedDescText, extendedDescHtml);
								
								alarmHandler.addAlarm( ae );
								//alarmHandler.addAlarm( new AlarmEventLongRunningTransaction(cm, threshold, dbname, OldestTranInSeconds, OldestTranName) );
							}
						}
						else
						{
							String extendedDescText = cm.toTextTableString(DATA_RATE, r);
							String extendedDescHtml = cm.toHtmlTableString(DATA_RATE, r, true, false, false);
							AlarmEvent ae = new AlarmEventLongRunningTransaction(cm, threshold, dbname, OldestTranInSeconds, OldestTranName);
							ae.setExtendedDescription(extendedDescText, extendedDescHtml);
							
							alarmHandler.addAlarm( ae );
							//alarmHandler.addAlarm( new AlarmEventLongRunningTransaction(cm, threshold, dbname, OldestTranInSeconds, OldestTranName) );
						}
					}
				}
			}

			
//			//-------------------------------------------------------
//			// Full transaction log
//			//-------------------------------------------------------
//			if (isSystemAlarmsForColumnEnabledAndInTimeRange("TransactionLogFull"))
//			{
//				Double TransactionLogFull = cm.getAbsValueAsDouble(r, "TransactionLogFull");
//				if (TransactionLogFull != null)
//				{
//					if (debugPrint || _logger.isDebugEnabled())
//						System.out.println("##### sendAlarmRequest("+cm.getName()+"): dbname='"+dbname+"', TransactionLogFull='"+TransactionLogFull+"'.");
//
//					int threshold = Configuration.getCombinedConfiguration().getIntProperty(PROPKEY_alarm_TransactionLogFull, DEFAULT_alarm_TransactionLogFull);
//					if (TransactionLogFull.intValue() > threshold)
//					{
//						String extendedDescText = cm.toTextTableString(DATA_RATE, r);
//						String extendedDescHtml = cm.toHtmlTableString(DATA_RATE, r, true, false, false);
//						AlarmEvent ae = new AlarmEventFullTranLog(cm, threshold, dbname);
//						ae.setExtendedDescription(extendedDescText, extendedDescHtml);
//						
//						alarmHandler.addAlarm( ae );
//						//alarmHandler.addAlarm( new AlarmEventFullTranLog(cm, threshold, dbname) );
//					}
//				}
//			}


			//-------------------------------------------------------
			// DATA Size usage
			//-------------------------------------------------------
			// figgure out a "rule" for how to do this...
			// - skip smaller databases ??? (what is the limit)
			// - Use PCT or Actual MB Left (for example a database with: dataSize=491,520 MB, freeDataSize=35,180 MB, dataSizeUsedPct=92.8) 
			//                              should this be alarmed??? it still got 35 GB space left, but it's at 92 PCT Usage...


			//-------------------------------------------------------
			// LOG Size usage
			//-------------------------------------------------------
			// figgure out a "rule" for how to do this...
			// - Simular situation as above...
			// - BUT if we got little LOG Space left we need to alarm quickly - maybe LogSizeUsedPct can be used here
			//   what about "mixed data and log" should that be considdered.


//			//-------------------------------------------------------
//			// LastBackupFailed
//			//-------------------------------------------------------
//			if (isSystemAlarmsForColumnEnabledAndInTimeRange("LastBackupFailed"))
//			{
//				Double val = cm.getAbsValueAsDouble(r, "LastBackupFailed");
//				if (val != null)
//				{
//					int threshold = Configuration.getCombinedConfiguration().getIntProperty(PROPKEY_alarm_LastBackupFailed, DEFAULT_alarm_LastBackupFailed);
//					if (val.intValue() > threshold)
//					{
//						alarmHandler.addAlarm( new AlarmEventLastBackupFailed(cm, dbname, threshold) );
//					}
//				}
//			}

			//-------------------------------------------------------
			// LastDbBackupAgeInHours
			//-------------------------------------------------------
			if (isSystemAlarmsForColumnEnabledAndInTimeRange("LastDbBackupAgeInHours"))
			{
				Double val = cm.getAbsValueAsDouble(r, "LastDbBackupAgeInHours");
				if (val == null)
					val = -1.0;
				
				int threshold = Configuration.getCombinedConfiguration().getIntProperty(PROPKEY_alarm_LastDbBackupAgeInHours, DEFAULT_alarm_LastDbBackupAgeInHours);
				if (val.intValue() > threshold || val.intValue() < 0)
				{
					// Get config 'skip some transaction names'
					String keepDbRegExp  = Configuration.getCombinedConfiguration().getProperty(PROPKEY_alarm_LastDbBackupAgeInHoursForDbs,  DEFAULT_alarm_LastDbBackupAgeInHoursForDbs);
					String skipDbRegExp  = Configuration.getCombinedConfiguration().getProperty(PROPKEY_alarm_LastDbBackupAgeInHoursSkipDbs, DEFAULT_alarm_LastDbBackupAgeInHoursSkipDbs);
					String keepSrvRegExp = Configuration.getCombinedConfiguration().getProperty(PROPKEY_alarm_LastDbBackupAgeInHoursForSrv,  DEFAULT_alarm_LastDbBackupAgeInHoursForSrv);
					String skipSrvRegExp = Configuration.getCombinedConfiguration().getProperty(PROPKEY_alarm_LastDbBackupAgeInHoursSkipSrv, DEFAULT_alarm_LastDbBackupAgeInHoursSkipSrv);

					// note: this must be set to true at start, otherwise all below rules will be disabled (it "stops" processing at first doAlarm==false)
					boolean doAlarm = true;

					// if -1 (LastTranLogDumpTime=NULL), only alarm if we have anything in the keep* or skip* rules
					if (val.intValue() < 0 && StringUtil.isNullOrBlankForAll(keepDbRegExp, skipDbRegExp, keepSrvRegExp, skipSrvRegExp))
						doAlarm = false;
					
					// The below could have been done with neasted if(keep-db), if(keep-srv), if(!skipDb), if(!skipSrv) doAlarm=true; 
					// Below is more readable, from a variable context point-of-view, but HARDER to understand
					doAlarm = (doAlarm && (StringUtil.isNullOrBlank(keepDbRegExp)  ||   dbname     .matches(keepDbRegExp ))); //     matches the KEEP Db  regexp
					doAlarm = (doAlarm && (StringUtil.isNullOrBlank(keepSrvRegExp) ||   dbmsSrvName.matches(keepSrvRegExp))); //     matches the KEEP Srv regexp
					doAlarm = (doAlarm && (StringUtil.isNullOrBlank(skipDbRegExp)  || ! dbname     .matches(skipDbRegExp ))); // NO match in the SKIP Db  regexp
					doAlarm = (doAlarm && (StringUtil.isNullOrBlank(skipSrvRegExp) || ! dbmsSrvName.matches(skipSrvRegExp))); // NO match in the SKIP Srv regexp

					// NO match in the SKIP regexp
					if (doAlarm)
					{
						alarmHandler.addAlarm( new AlarmEventOldBackup(cm, threshold, dbname, val.intValue()) );
					}
				}
			}

			//-------------------------------------------------------
			// LastLogBackupAgeInHours
			//-------------------------------------------------------
			if (isSystemAlarmsForColumnEnabledAndInTimeRange("LastLogBackupAgeInHours"))
			{
				Double val = cm.getAbsValueAsDouble(r, "LastLogBackupAgeInHours");
				if (val == null)
					val = -1.0;
				
				int threshold = Configuration.getCombinedConfiguration().getIntProperty(PROPKEY_alarm_LastLogBackupAgeInHours, DEFAULT_alarm_LastLogBackupAgeInHours);
				if (val.intValue() > threshold || val.intValue() < 0)
				{
					// Get config 'skip some transaction names'
					String keepDbRegExp  = Configuration.getCombinedConfiguration().getProperty(PROPKEY_alarm_LastLogBackupAgeInHoursForDbs,  DEFAULT_alarm_LastLogBackupAgeInHoursForDbs);
					String skipDbRegExp  = Configuration.getCombinedConfiguration().getProperty(PROPKEY_alarm_LastLogBackupAgeInHoursSkipDbs, DEFAULT_alarm_LastLogBackupAgeInHoursSkipDbs);
					String keepSrvRegExp = Configuration.getCombinedConfiguration().getProperty(PROPKEY_alarm_LastLogBackupAgeInHoursForSrv,  DEFAULT_alarm_LastLogBackupAgeInHoursForSrv);
					String skipSrvRegExp = Configuration.getCombinedConfiguration().getProperty(PROPKEY_alarm_LastLogBackupAgeInHoursSkipSrv, DEFAULT_alarm_LastLogBackupAgeInHoursSkipSrv);

					// note: this must be set to true at start, otherwise all below rules will be disabled (it "stops" processing at first doAlarm==false)
					boolean doAlarm = true;

					// if -1 (BackupStartTime=NULL), only alarm if we have anything in the keep* or skip* rules
					if (val.intValue() < 0 && StringUtil.isNullOrBlankForAll(keepDbRegExp, skipDbRegExp, keepSrvRegExp, skipSrvRegExp))
						doAlarm = false;
					
					// The below could have been done with neasted if(keep-db), if(keep-srv), if(!skipDb), if(!skipSrv) doAlarm=true; 
					// Below is more readable, from a variable context point-of-view, but HARDER to understand
					doAlarm = (doAlarm && (StringUtil.isNullOrBlank(keepDbRegExp)  ||   dbname     .matches(keepDbRegExp ))); //     matches the KEEP Db  regexp
					doAlarm = (doAlarm && (StringUtil.isNullOrBlank(keepSrvRegExp) ||   dbmsSrvName.matches(keepSrvRegExp))); //     matches the KEEP Srv regexp
					doAlarm = (doAlarm && (StringUtil.isNullOrBlank(skipDbRegExp)  || ! dbname     .matches(skipDbRegExp ))); // NO match in the SKIP Db  regexp
					doAlarm = (doAlarm && (StringUtil.isNullOrBlank(skipSrvRegExp) || ! dbmsSrvName.matches(skipSrvRegExp))); // NO match in the SKIP Srv regexp
					
					if (doAlarm)
					{
						alarmHandler.addAlarm( new AlarmEventOldTranLogBackup(cm, threshold, dbname, val.intValue()) );
					}
				}
			}
			
			//-------------------------------------------------------
			// LowDbFreeSpaceInMb
			//-------------------------------------------------------
			if (isSystemAlarmsForColumnEnabledAndInTimeRange("LowDbFreeSpaceInMb"))
			{
				Double freeMb     = cm.getAbsValueAsDouble(r, "DataSizeFreeInMb");
				Double usedPct    = cm.getAbsValueAsDouble(r, "DataSizeUsedPct");
				Number threshold = getDbFreeSpaceThreshold(dbname, _map_alarm_LowDbFreeSpaceInMb); // This uses dbname.matches(map:anyKey)
				if (freeMb != null && usedPct != null && threshold != null)
				{
					if (freeMb.intValue() < threshold.intValue())
					{
						String extendedDescText = cm.toTextTableString(DATA_RATE, r);
						String extendedDescHtml = cm.toHtmlTableString(DATA_RATE, r, true, false, false);
						AlarmEvent ae = new AlarmEventLowDbFreeSpace(cm, dbname, freeMb.intValue(), usedPct, threshold.intValue());
						ae.setExtendedDescription(extendedDescText, extendedDescHtml);
						
						alarmHandler.addAlarm( ae );
						//alarmHandler.addAlarm( new AlarmEventLowDbFreeSpace(cm, dbname, freeMb.intValue(), usedPct, threshold.intValue()) );
					}
				}
			}

			//-------------------------------------------------------
			// LowLogFreeSpaceInMb
			//-------------------------------------------------------
			if (isSystemAlarmsForColumnEnabledAndInTimeRange("LowLogFreeSpaceInMb"))
			{
				Double freeMb     = cm.getAbsValueAsDouble(r, "LogSizeFreeInMb");
				Double usedPct    = cm.getAbsValueAsDouble(r, "LogSizeUsedPct");
				Number threshold = getDbFreeSpaceThreshold(dbname, _map_alarm_LowLogFreeSpaceInMb); // This uses dbname.matches(map:anyKey)
				if (freeMb != null && usedPct != null && threshold != null)
				{
					if (freeMb.intValue() < threshold.intValue())
					{
						String extendedDescText = cm.toTextTableString(DATA_RATE, r);
						String extendedDescHtml = cm.toHtmlTableString(DATA_RATE, r, true, false, false);
						AlarmEvent ae = new AlarmEventLowLogFreeSpace(cm, dbname, freeMb.intValue(), usedPct, threshold.intValue());
						ae.setExtendedDescription(extendedDescText, extendedDescHtml);
						
						alarmHandler.addAlarm( ae );
						//alarmHandler.addAlarm( new AlarmEventLowLogFreeSpace(cm, dbname, freeMb.intValue(), usedPct, threshold.intValue()) );
					}
				}
			}

			//-------------------------------------------------------
			// LowDbFreeSpaceInPct
			//-------------------------------------------------------
			if (isSystemAlarmsForColumnEnabledAndInTimeRange("LowDbFreeSpaceInPct"))
			{
				Double freeMb     = cm.getAbsValueAsDouble(r, "DataSizeFreeInMb");
				Double usedPct    = cm.getAbsValueAsDouble(r, "DataSizeUsedPct");
				Number threshold = getDbFreeSpaceThreshold(dbname, _map_alarm_LowDbFreeSpaceInPct); // This uses dbname.matches(map:anyKey)
				if (freeMb != null && usedPct != null && threshold != null)
				{
					if (usedPct > threshold.doubleValue())
					{
						String extendedDescText = cm.toTextTableString(DATA_RATE, r);
						String extendedDescHtml = cm.toHtmlTableString(DATA_RATE, r, true, false, false);
						AlarmEvent ae = new AlarmEventLowDbFreeSpace(cm, dbname, freeMb.intValue(), usedPct, threshold.doubleValue());
						ae.setExtendedDescription(extendedDescText, extendedDescHtml);
						
						alarmHandler.addAlarm( ae );
						//alarmHandler.addAlarm( new AlarmEventLowDbFreeSpace(cm, dbname, freeMb.intValue(), usedPct, threshold.doubleValue()) );
					}
				}
			}
			
			//-------------------------------------------------------
			// LowLogFreeSpaceInPct
			//-------------------------------------------------------
			if (isSystemAlarmsForColumnEnabledAndInTimeRange("LowLogFreeSpaceInPct"))
			{
				Double freeMb     = cm.getAbsValueAsDouble(r, "LogSizeFreeInMb");
				Double usedPct    = cm.getAbsValueAsDouble(r, "LogSizeUsedPct");
				Number threshold = getDbFreeSpaceThreshold(dbname, _map_alarm_LowLogFreeSpaceInPct); // This uses dbname.matches(map:anyKey)
				if (freeMb != null && usedPct != null && threshold != null)
				{
					if (usedPct > threshold.doubleValue())
					{
						String extendedDescText = cm.toTextTableString(DATA_RATE, r);
						String extendedDescHtml = cm.toHtmlTableString(DATA_RATE, r, true, false, false);
						AlarmEvent ae = new AlarmEventLowLogFreeSpace(cm, dbname, freeMb.intValue(), usedPct, threshold.doubleValue());
						ae.setExtendedDescription(extendedDescText, extendedDescHtml);
						
						alarmHandler.addAlarm( ae );
						//alarmHandler.addAlarm( new AlarmEventLowLogFreeSpace(cm, dbname, freeMb.intValue(), usedPct, threshold.doubleValue()) );
					}
				}
			}

			
			//-------------------------------------------------------
			// LowOsDiskFreeSpaceInMb  -- MB
			// 
			// - for either DATA or LOG
			//   take: data or log with the lowest 'freeMb'
			//   use 'OsVolume' or if dont't exists: use directory of FileName... 
			//-------------------------------------------------------
			if (isSystemAlarmsForColumnEnabledAndInTimeRange("LowOsDiskFreeSpaceInMb"))
			{
				Double dataFreeMb  = cm.getAbsValueAsDouble(r, "DataOsDiskFreeMb");
				Double dataUsedPct = cm.getAbsValueAsDouble(r, "DataOsDiskUsedPct");
				Double logFreeMb   = cm.getAbsValueAsDouble(r, "LogOsDiskFreeMb");
				Double logUsedPct  = cm.getAbsValueAsDouble(r, "LogOsDiskUsedPct");

				Double freeMb     = -99d;
				Double usedPct    = -99d;

				if (dataFreeMb  != null && logFreeMb  != null) freeMb  = Math.min(dataFreeMb,  logFreeMb);
				if (dataUsedPct != null && logUsedPct != null) usedPct = Math.max(dataUsedPct, logUsedPct);
				
				Number threshold = getDbFreeSpaceThreshold(dbname, _map_alarm_LowOsDiskFreeSpaceInMb); // This uses dbname.matches(map:anyKey)
	
				if (freeMb != -99d && usedPct != -99d && threshold != null)
				{
					if (freeMb.intValue() < threshold.intValue()) // FREE-MB CHECK
					{
						String osVolume   = "";
						String osFileName = "";
						if (freeMb == logFreeMb)
						{
							osVolume    = cm.getAbsString(r, "LogOsDisk");
							osFileName  = cm.getAbsString(r, "LogOsFileName");
						}
						if (freeMb == dataFreeMb)
						{
							osVolume    = cm.getAbsString(r, "DataOsDisk");
							osFileName  = cm.getAbsString(r, "DataOsFileName");
						}

						// If 'osVolume' is empty... try to get the File directory...
						String mountPoint = osVolume;
						if (StringUtil.isNullOrBlank(mountPoint))
						{
							mountPoint = "-unknown-";
							if (StringUtil.hasValue(osFileName))
								mountPoint = CounterControllerSqlServer.resolvFileNameToDirectory(osFileName);
						}
						
						String extendedDescText = cm.toTextTableString(DATA_RATE, r);
						String extendedDescHtml = cm.toHtmlTableString(DATA_RATE, r, true, false, false);
						AlarmEvent ae = new AlarmEventLowOsDiskFreeSpace(cm, mountPoint, freeMb.intValue(), usedPct, threshold.intValue()); // NOTE: threshold is Integer = MB
						ae.setExtendedDescription(extendedDescText, extendedDescHtml);
						
						alarmHandler.addAlarm( ae );
					}
				}
			}

			//-------------------------------------------------------
			// LowOsDiskFreeSpaceInPct  -- PCT
			// 
			// - for either DATA or LOG
			//   take: data or log with the highest 'usedPct'
			//   use 'OsVolume' or if dont't exists: use directory of FileName... 
			//-------------------------------------------------------
			if (isSystemAlarmsForColumnEnabledAndInTimeRange("LowOsDiskFreeSpaceInPct"))
			{
				Double dataFreeMb  = cm.getAbsValueAsDouble(r, "DataOsDiskFreeMb");
				Double dataUsedPct = cm.getAbsValueAsDouble(r, "DataOsDiskUsedPct");
				Double logFreeMb   = cm.getAbsValueAsDouble(r, "LogOsDiskFreeMb");
				Double logUsedPct  = cm.getAbsValueAsDouble(r, "LogOsDiskUsedPct");

				Double freeMb     = -99d;
				Double usedPct    = -99d;

				if (dataFreeMb  != null && logFreeMb  != null) freeMb  = Math.min(dataFreeMb,  logFreeMb);
				if (dataUsedPct != null && logUsedPct != null) usedPct = Math.max(dataUsedPct, logUsedPct);
				
				Number threshold = getDbFreeSpaceThreshold(dbname, _map_alarm_LowOsDiskFreeSpaceInPct); // This uses dbname.matches(map:anyKey)
	
				if (freeMb != -99d && usedPct != -99d && threshold != null)
				{
					if (usedPct > threshold.doubleValue()) // Percent CHECK
					{
						String osVolume   = "";
						String osFileName = "";
						if (usedPct == logUsedPct)
						{
							osVolume    = cm.getAbsString(r, "LogOsDisk");
							osFileName  = cm.getAbsString(r, "LogOsFileName");
						}
						if (usedPct == dataUsedPct)
						{
							osVolume    = cm.getAbsString(r, "DataOsDisk");
							osFileName  = cm.getAbsString(r, "DataOsFileName");
						}

						// If 'osVolume' is empty... try to get the File directory...
						String mountPoint = osVolume;
						if (StringUtil.isNullOrBlank(mountPoint))
						{
							mountPoint = "-unknown-";
							if (StringUtil.hasValue(osFileName))
								mountPoint = CounterControllerSqlServer.resolvFileNameToDirectory(osFileName);
						}
						
						String extendedDescText = cm.toTextTableString(DATA_RATE, r);
						String extendedDescHtml = cm.toHtmlTableString(DATA_RATE, r, true, false, false);
						AlarmEvent ae = new AlarmEventLowOsDiskFreeSpace(cm, mountPoint, freeMb.intValue(), usedPct, threshold.doubleValue()); // NOTE: threshold is Double = PCT
						ae.setExtendedDescription(extendedDescText, extendedDescHtml);
						
						alarmHandler.addAlarm( ae );
					}
				}
			}

			
			//-------------------------------------------------------
			// DbState, check: user_access_desc & state_desc
			//-------------------------------------------------------
			if (isSystemAlarmsForColumnEnabledAndInTimeRange("DbState"))
			{
				String user_access_desc = cm.getAbsString(r, "user_access_desc");
				String state_desc       = cm.getAbsString(r, "state_desc");

				if (user_access_desc != null && state_desc != null)
				{
					String alarm = "";

					// This will ALWAYS return a Pattern
					Pattern p = getDbStatePattern(dbname, _map_alarm_DbState);

					// check: 'user_access_desc'
					if (user_access_desc != null && !p.matcher(user_access_desc).matches())
						alarm += "user_access='" + user_access_desc + "'";

					// check: 'state_desc'
					if (state_desc != null && !p.matcher(state_desc).matches())
						alarm += (StringUtil.isNullOrBlank(alarm) ? "" : ", ") + "state='" + state_desc + "'";

					if (StringUtil.hasValue(alarm))
					{
						String extendedDescText = cm.toTextTableString(DATA_RATE, r);
						String extendedDescHtml = cm.toHtmlTableString(DATA_RATE, r, true, false, false);
						AlarmEvent ae = new AlarmEventDatabaseState(cm, dbname, alarm, p.pattern());
						ae.setExtendedDescription(extendedDescText, extendedDescHtml);
						
						alarmHandler.addAlarm( ae );
					}
				}
			}

		} // end: loop dbname(s)
	}

	/**
	 * Helper method to get the Threshold for a specific DB, using direct access to map or by check all key values in map with regexp...
	 * 
	 * @param dbname
	 * @param map
	 * @return
	 */
    private Number getDbFreeSpaceThreshold(String dbname, Map<String, Number> map)
    {
    	if (map == null)
    	{
    		_logger.warn("getDbFreeSpaceThreshold(dbname=|"+dbname+"|, map=|"+map+"|). map is NULL, which wasn't expected... some initialization must have failed.");
    		return null;
    	}

		if (map.isEmpty())
			return null;

    	if (StringUtil.isNullOrBlank(dbname))
    		return null;

    	// Lookup the map for DIRECT match
    	Number num = map.get(dbname);
    	if (num != null)
    		return num;

    	// Check all key in the match and check if they match the REGEXP in the key of the map
    	for (String key : map.keySet())
		{
			if (dbname.matches(key))
				return map.get(key);
		}
    	
    	// no match
    	return null;
    }


	/**
	 * Helper method to get the Compiled Regexp for a specific DB, using direct access to map or by check all key values in map with regexp...
	 * 
	 * @param dbname
	 * @param map
	 * @return
	 */
    private Pattern getDbStatePattern(String dbname, Map<String, Pattern> map)
    {
    	if (map == null)
    	{
    		_logger.warn("getDbStatePattern(dbname=|"+dbname+"|, map=|"+map+"|). map is NULL, which wasn't expected... some initialization must have failed.");
			return Pattern.compile(DEFAULT_alarm_DbState);
    	}

		if (map.isEmpty())
			return Pattern.compile(DEFAULT_alarm_DbState);

    	if (StringUtil.isNullOrBlank(dbname))
			return Pattern.compile(DEFAULT_alarm_DbState);

    	// Lookup the map for DIRECT match
    	Pattern p = map.get(dbname);
    	if (p != null)
    		return p;

    	// Check all key in the match and check if they match the REGEXP in the key of the map
    	for (String key : map.keySet())
		{
			if (dbname.matches(key))
				return map.get(key);
		}
    	
    	// no match
		return Pattern.compile(DEFAULT_alarm_DbState);
    }


//	private Map<String, Number> _map_alarm_LowDbFreeSpaceInMb   = new HashMap<>();
//	private Map<String, Number> _map_alarm_LowLogFreeSpaceInMb  = new HashMap<>();
//	private Map<String, Number> _map_alarm_LowDbFreeSpaceInPct  = new HashMap<>();
//	private Map<String, Number> _map_alarm_LowLogFreeSpaceInPct = new HashMap<>();
	private Map<String, Number> _map_alarm_LowDbFreeSpaceInMb;  // Note: do NOT initialize this here... since the initAlarms() is done in super, if initialized it will be overwritten here...
	private Map<String, Number> _map_alarm_LowLogFreeSpaceInMb; // Note: do NOT initialize this here... since the initAlarms() is done in super, if initialized it will be overwritten here...
	private Map<String, Number> _map_alarm_LowDbFreeSpaceInPct; // Note: do NOT initialize this here... since the initAlarms() is done in super, if initialized it will be overwritten here...
	private Map<String, Number> _map_alarm_LowLogFreeSpaceInPct;// Note: do NOT initialize this here... since the initAlarms() is done in super, if initialized it will be overwritten here...
	
	private Map<String, Number> _map_alarm_LowOsDiskFreeSpaceInMb;  // Note: do NOT initialize this here... since the initAlarms() is done in super, if initialized it will be overwritten here...
	private Map<String, Number> _map_alarm_LowOsDiskFreeSpaceInPct; // Note: do NOT initialize this here... since the initAlarms() is done in super, if initialized it will be overwritten here...

	private Map<String, Pattern> _map_alarm_DbState;


	/**
	 * Initialize stuff that has to do with alarms
	 */
	@Override
	public void initAlarms()
	{
		Configuration conf = Configuration.getCombinedConfiguration();
		String cfgVal;

		_map_alarm_LowDbFreeSpaceInMb   = new HashMap<>();
		_map_alarm_LowLogFreeSpaceInMb  = new HashMap<>();
		_map_alarm_LowDbFreeSpaceInPct  = new HashMap<>();
		_map_alarm_LowLogFreeSpaceInPct = new HashMap<>();

		_map_alarm_LowOsDiskFreeSpaceInMb  = new HashMap<>();
		_map_alarm_LowOsDiskFreeSpaceInPct = new HashMap<>();

		_map_alarm_DbState = new HashMap<>();

		String prefix = "       ";
		
		//--------------------------------------
		// LowDbFreeSpaceInMb
		cfgVal = conf.getProperty(PROPKEY_alarm_LowDbFreeSpaceInMb, DEFAULT_alarm_LowDbFreeSpaceInMb);
		if (StringUtil.hasValue(cfgVal))
		{
			Map<String, String> map = StringUtil.parseCommaStrToMap(cfgVal);
			if (_logger.isDebugEnabled())
				_logger.debug(prefix + "Initializing alarm 'LowDbFreeSpaceInMb'. After parseCommaStrToMap, map looks like: "+map);
			
			for (String key : map.keySet())
			{
				String val = map.get(key);
				
				try
				{
					int mb = NumberUtils.createNumber(val).intValue();
					_map_alarm_LowDbFreeSpaceInMb.put(key, mb);

					_logger.info(prefix + "Initializing alarm. Using 'LowDbFreeSpaceInMb', dbname='"+key+"', mb="+mb);
				}
				catch (NumberFormatException ex)
				{
					_logger.info(prefix + "Initializing alarm. Skipping 'LowDbFreeSpaceInMb' enty dbname='"+key+"', val='"+val+"'. The value is not a number.");
				}
			}
		}
		
		//--------------------------------------
		// LowLogFreeSpaceInMb
		cfgVal = conf.getProperty(PROPKEY_alarm_LowLogFreeSpaceInMb, DEFAULT_alarm_LowLogFreeSpaceInMb);
		if (StringUtil.hasValue(cfgVal))
		{
			Map<String, String> map = StringUtil.parseCommaStrToMap(cfgVal);
			if (_logger.isDebugEnabled())
				_logger.debug(prefix + "Initializing alarm 'LowLogFreeSpaceInMb'. After parseCommaStrToMap, map looks like: "+map);
			
			for (String key : map.keySet())
			{
				String val = map.get(key);
				
				try
				{
					int mb = NumberUtils.createNumber(val).intValue();
					_map_alarm_LowLogFreeSpaceInMb.put(key, mb);

					_logger.info(prefix + "Initializing alarm. Using 'LowLogFreeSpaceInMb', dbname='"+key+"', mb="+mb);
				}
				catch (NumberFormatException ex)
				{
					_logger.info(prefix + "Initializing alarm. Skipping 'LowLogFreeSpaceInMb' enty dbname='"+key+"', val='"+val+"'. The value is not a number.");
				}
			}
		}
		
		//--------------------------------------
		// LowDbFreeSpaceInPct
		cfgVal = conf.getProperty(PROPKEY_alarm_LowDbFreeSpaceInPct, DEFAULT_alarm_LowDbFreeSpaceInPct);
		if (StringUtil.hasValue(cfgVal))
		{
			Map<String, String> map = StringUtil.parseCommaStrToMap(cfgVal);
			if (_logger.isDebugEnabled())
				_logger.debug(prefix + "Initializing alarm 'LowDbFreeSpaceInPct'. After parseCommaStrToMap, map looks like: "+map);
			
			for (String key : map.keySet())
			{
				String val = map.get(key);
				
				try
				{
					double pct = NumberUtils.createNumber(val).doubleValue();
					_map_alarm_LowDbFreeSpaceInPct.put(key, pct);

					_logger.info(prefix + "Initializing alarm. Using 'LowDbFreeSpaceInPct', dbname='"+key+"', pct="+pct);
				}
				catch (NumberFormatException ex)
				{
					_logger.info(prefix + "Initializing alarm. Skipping 'LowDbFreeSpaceInPct' enty dbname='"+key+"', val='"+val+"'. The value is not a number.");
				}
			}
		}
		
		//--------------------------------------
		// LowLogFreeSpaceInPct
		cfgVal = conf.getProperty(PROPKEY_alarm_LowLogFreeSpaceInPct, DEFAULT_alarm_LowLogFreeSpaceInPct);
		if (StringUtil.hasValue(cfgVal))
		{
			Map<String, String> map = StringUtil.parseCommaStrToMap(cfgVal);
			if (_logger.isDebugEnabled())
				_logger.debug(prefix + "Initializing alarm 'LowLogFreeSpaceInPct'. After parseCommaStrToMap, map looks like: "+map);
			
			for (String key : map.keySet())
			{
				String val = map.get(key);
				
				try
				{
					double pct = NumberUtils.createNumber(val).doubleValue();
					_map_alarm_LowDbFreeSpaceInPct.put(key, pct);

					_logger.info(prefix + "Initializing alarm. Using 'LowLogFreeSpaceInPct', dbname='"+key+"', pct="+pct);
				}
				catch (NumberFormatException ex)
				{
					_logger.info(prefix + "Initializing alarm. Skipping 'LowLogFreeSpaceInPct' enty dbname='"+key+"', val='"+val+"'. The value is not a number.");
				}
			}
		}

	
	
		//--------------------------------------
		// LowOsDiskFreeSpaceInMb
		cfgVal = conf.getProperty(PROPKEY_alarm_LowOsDiskFreeSpaceInMb, DEFAULT_alarm_LowOsDiskFreeSpaceInMb);
		if (StringUtil.hasValue(cfgVal))
		{
			Map<String, String> map = StringUtil.parseCommaStrToMap(cfgVal);
			if (_logger.isDebugEnabled())
				_logger.debug(prefix + "Initializing alarm 'LowOsDiskFreeSpaceInMb'. After parseCommaStrToMap, map looks like: "+map);
			
			for (String key : map.keySet())
			{
				String val = map.get(key);
				
				try
				{
					int mb = NumberUtils.createNumber(val).intValue();
					_map_alarm_LowOsDiskFreeSpaceInMb.put(key, mb);

					_logger.info(prefix + "Initializing alarm. Using 'LowOsDiskFreeSpaceInMb', dbname='"+key+"', mb="+mb);
				}
				catch (NumberFormatException ex)
				{
					_logger.info(prefix + "Initializing alarm. Skipping 'LowOsDiskFreeSpaceInMb' enty dbname='"+key+"', val='"+val+"'. The value is not a number.");
				}
			}
		}
		
		//--------------------------------------
		// LowOsDiskFreeSpaceInPct
		cfgVal = conf.getProperty(PROPKEY_alarm_LowOsDiskFreeSpaceInPct, DEFAULT_alarm_LowOsDiskFreeSpaceInPct);
		if (StringUtil.hasValue(cfgVal))
		{
			Map<String, String> map = StringUtil.parseCommaStrToMap(cfgVal);
			if (_logger.isDebugEnabled())
				_logger.debug(prefix + "Initializing alarm 'LowOsDiskFreeSpaceInPct'. After parseCommaStrToMap, map looks like: "+map);
			
			for (String key : map.keySet())
			{
				String val = map.get(key);
				
				try
				{
					double pct = NumberUtils.createNumber(val).doubleValue();
					_map_alarm_LowOsDiskFreeSpaceInPct.put(key, pct);

					_logger.info(prefix + "Initializing alarm. Using 'LowOsDiskFreeSpaceInPct', dbname='"+key+"', pct="+pct);
				}
				catch (NumberFormatException ex)
				{
					_logger.info(prefix + "Initializing alarm. Skipping 'LowOsDiskFreeSpaceInPct' enty dbname='"+key+"', val='"+val+"'. The value is not a number.");
				}
			}
		}

		
		//--------------------------------------
		// DbState
		cfgVal = conf.getProperty(PROPKEY_alarm_DbState, DEFAULT_alarm_DbState);
		if (StringUtil.hasValue(cfgVal))
		{
			Map<String, String> map = StringUtil.parseCommaStrToMap(cfgVal);
			if (_logger.isDebugEnabled())
				_logger.debug(prefix + "Initializing alarm 'DbState'. After parseCommaStrToMap, map looks like: "+map);
			
			for (String key : map.keySet())
			{
				String val = map.get(key);
				
				try
				{
					Pattern pattern = Pattern.compile(val);
					_map_alarm_DbState.put(key, pattern);

					_logger.info(prefix + "Initializing alarm. Using 'DbState', dbname='"+key+"', val="+val);
				}
				catch (PatternSyntaxException ex)
				{
					_logger.info(prefix + "Initializing alarm. Skipping 'DbState' enty dbname='"+key+"', val='"+val+"'. The value is not a Regexp. Instead the default '" + DEFAULT_alarm_DbState + "' will be used. Caught: " + ex);

					Pattern pattern = Pattern.compile(DEFAULT_alarm_DbState);
					_map_alarm_DbState.put(key, pattern);
				}
			}
		}
	}

	public static final String  PROPKEY_alarm_OldestTranInSeconds             = CM_NAME + ".alarm.system.if.OldestTranInSeconds.gt";
	public static final int     DEFAULT_alarm_OldestTranInSeconds             = 60;
	
	public static final String  PROPKEY_alarm_OldestTranInSecondsSkipTranName = CM_NAME + ".alarm.system.if.OldestTranInSeconds.skip.tranName";
//	public static final String  DEFAULT_alarm_OldestTranInSecondsSkipTranName = "^(DUMP |\\$dmpxact).*";
	public static final String  DEFAULT_alarm_OldestTranInSecondsSkipTranName = "";
	
//	public static final String  PROPKEY_alarm_TransactionLogFull              = CM_NAME + ".alarm.system.if.TransactionLogFull.gt";
//	public static final int     DEFAULT_alarm_TransactionLogFull              = 0;

//	public static final String  PROPKEY_alarm_LastBackupFailed                = CM_NAME + ".alarm.system.if.LastBackupFailed.gt";
//	public static final int     DEFAULT_alarm_LastBackupFailed                = 0;

	public static final String  PROPKEY_alarm_LastDbBackupAgeInHours          = CM_NAME + ".alarm.system.if.LastDbBackupAgeInHours.gt";
	public static final int     DEFAULT_alarm_LastDbBackupAgeInHours          = 999_999; // 114 years; more or less disabled
	public static final String  PROPKEY_alarm_LastDbBackupAgeInHoursForDbs    = CM_NAME + ".alarm.system.if.LastDbBackupAgeInHours.for.dbs";
	public static final String  DEFAULT_alarm_LastDbBackupAgeInHoursForDbs    = "";
	public static final String  PROPKEY_alarm_LastDbBackupAgeInHoursSkipDbs   = CM_NAME + ".alarm.system.if.LastDbBackupAgeInHours.skip.dbs";
	public static final String  DEFAULT_alarm_LastDbBackupAgeInHoursSkipDbs   = "";
	public static final String  PROPKEY_alarm_LastDbBackupAgeInHoursForSrv    = CM_NAME + ".alarm.system.if.LastDbBackupAgeInHours.for.srv";
	public static final String  DEFAULT_alarm_LastDbBackupAgeInHoursForSrv    = "";
	public static final String  PROPKEY_alarm_LastDbBackupAgeInHoursSkipSrv   = CM_NAME + ".alarm.system.if.LastDbBackupAgeInHours.skip.srv";
	public static final String  DEFAULT_alarm_LastDbBackupAgeInHoursSkipSrv   = "";

	public static final String  PROPKEY_alarm_LastLogBackupAgeInHours         = CM_NAME + ".alarm.system.if.LastLogBackupAgeInHours.gt";
	public static final int     DEFAULT_alarm_LastLogBackupAgeInHours         = 999_999; // 114 years; more or less disabled
	public static final String  PROPKEY_alarm_LastLogBackupAgeInHoursForDbs   = CM_NAME + ".alarm.system.if.LastLogBackupAgeInHours.for.dbs";
	public static final String  DEFAULT_alarm_LastLogBackupAgeInHoursForDbs   = "";
	public static final String  PROPKEY_alarm_LastLogBackupAgeInHoursSkipDbs  = CM_NAME + ".alarm.system.if.LastLogBackupAgeInHours.skip.dbs";
	public static final String  DEFAULT_alarm_LastLogBackupAgeInHoursSkipDbs  = "";
	public static final String  PROPKEY_alarm_LastLogBackupAgeInHoursForSrv   = CM_NAME + ".alarm.system.if.LastLogBackupAgeInHours.for.srv";
	public static final String  DEFAULT_alarm_LastLogBackupAgeInHoursForSrv   = "";
	public static final String  PROPKEY_alarm_LastLogBackupAgeInHoursSkipSrv  = CM_NAME + ".alarm.system.if.LastLogBackupAgeInHours.skip.srv";
	public static final String  DEFAULT_alarm_LastLogBackupAgeInHoursSkipSrv  = "";

	public static final String  PROPKEY_alarm_LowDbFreeSpaceInMb              = CM_NAME + ".alarm.system.if.LowDbFreeSpaceInMb.lt";
//	public static final String  DEFAULT_alarm_LowDbFreeSpaceInMb              = ".*=2, tempdb=100";
	public static final String  DEFAULT_alarm_LowDbFreeSpaceInMb              = "";
                                                                              
	public static final String  PROPKEY_alarm_LowLogFreeSpaceInMb             = CM_NAME + ".alarm.system.if.LowLogFreeSpaceInMb.lt";
	public static final String  DEFAULT_alarm_LowLogFreeSpaceInMb             = "";
                                                                              
	public static final String  PROPKEY_alarm_LowDbFreeSpaceInPct             = CM_NAME + ".alarm.system.if.LowDbFreeSpaceInPct.gt";
//	public static final String  DEFAULT_alarm_LowDbFreeSpaceInPct             = "tempdb=80";
	public static final String  DEFAULT_alarm_LowDbFreeSpaceInPct             = "";
                                                                              
	public static final String  PROPKEY_alarm_LowLogFreeSpaceInPct            = CM_NAME + ".alarm.system.if.LowLogFreeSpaceInPct.gt";
	public static final String  DEFAULT_alarm_LowLogFreeSpaceInPct            = "";

	public static final String  PROPKEY_alarm_LowOsDiskFreeSpaceInMb          = CM_NAME + ".alarm.system.if.LowOsDiskFreeSpaceInMb.lt";
	public static final String  DEFAULT_alarm_LowOsDiskFreeSpaceInMb          = ".*=500";
                                                                              
	public static final String  PROPKEY_alarm_LowOsDiskFreeSpaceInPct         = CM_NAME + ".alarm.system.if.LowOsDiskFreeSpaceInPct.gt";
	public static final String  DEFAULT_alarm_LowOsDiskFreeSpaceInPct         = ".*=95";
                                                                              
	// A comma separated list of databases that *must* exists, othewise ALARM. (the dbname can be a regexp, but each entry in the list must have a count of 1 after we have looped all records)
	// TODO: NOT YET IMPLEMENTED... think a bit more about this...
//	public static final String  PROPKEY_alarm_MandatoryDatabaseList           = CM_NAME + ".alarm.system.MandatoryDatabaseList";
//	public static final String  DEFAULT_alarm_MandatoryDatabaseList           = "";
	
	public static final String  PROPKEY_alarm_DbState                         = CM_NAME + ".alarm.system.if.state.not.in";
	public static final String  DEFAULT_alarm_DbState                         = ".*=(MULTI_USER|ONLINE)";
	
	@Override
	public List<CmSettingsHelper> getLocalAlarmSettings()
	{
		Configuration conf = Configuration.getCombinedConfiguration();
		List<CmSettingsHelper> list = new ArrayList<>();
		
		CmSettingsHelper.Type isAlarmSwitch = CmSettingsHelper.Type.IS_ALARM_SWITCH;
		
		list.add(new CmSettingsHelper("OldestTranInSeconds",              isAlarmSwitch, PROPKEY_alarm_OldestTranInSeconds             , Integer.class, conf.getIntProperty(PROPKEY_alarm_OldestTranInSeconds             , DEFAULT_alarm_OldestTranInSeconds            ), DEFAULT_alarm_OldestTranInSeconds            , "If 'OldestTranInSeconds' is greater than ## then send 'AlarmEventLongRunningTransaction'." ));
//		list.add(new CmSettingsHelper("OldestTranInSeconds SkipTranName",                PROPKEY_alarm_OldestTranInSecondsSkipTranName , String .class, conf.getProperty   (PROPKEY_alarm_OldestTranInSecondsSkipTranName , DEFAULT_alarm_OldestTranInSecondsSkipTranName), DEFAULT_alarm_OldestTranInSecondsSkipTranName, "If 'OldestTranInSeconds' is true; then we can filter out transaction names using a Regular expression... if (tranName.matches('regexp'))... This to remove alarms of 'DUMP DATABASE' or similar. A good place to test your regexp is 'http://www.regexplanet.com/advanced/java/index.html'.", new RegExpInputValidator()));
//		list.add(new CmSettingsHelper("TransactionLogFull",               isAlarmSwitch, PROPKEY_alarm_TransactionLogFull              , Integer.class, conf.getIntProperty(PROPKEY_alarm_TransactionLogFull              , DEFAULT_alarm_TransactionLogFull             ), DEFAULT_alarm_TransactionLogFull             , "If 'TransactionLogFull' is greater than ## then send 'AlarmEventFullTranLog'." ));

//		list.add(new CmSettingsHelper("LastBackupFailed",                 isAlarmSwitch, PROPKEY_alarm_LastBackupFailed                , Integer.class, conf.getIntProperty(PROPKEY_alarm_LastBackupFailed                , DEFAULT_alarm_LastBackupFailed               ), DEFAULT_alarm_LastBackupFailed               , "If 'LastBackupFailed' is greater than ## then send 'AlarmEventLastBackupFailed'." ));

		list.add(new CmSettingsHelper("LastDbBackupAgeInHours",           isAlarmSwitch, PROPKEY_alarm_LastDbBackupAgeInHours          , Integer.class, conf.getIntProperty(PROPKEY_alarm_LastDbBackupAgeInHours          , DEFAULT_alarm_LastDbBackupAgeInHours         ), DEFAULT_alarm_LastDbBackupAgeInHours         , "If 'LastDbBackupAgeInHours' is greater than ## then send 'AlarmEventOldBackup'." ));
		list.add(new CmSettingsHelper("LastDbBackupAgeInHours ForDbs",                   PROPKEY_alarm_LastDbBackupAgeInHoursForDbs    , String .class, conf.getProperty   (PROPKEY_alarm_LastDbBackupAgeInHoursForDbs    , DEFAULT_alarm_LastDbBackupAgeInHoursForDbs   ), DEFAULT_alarm_LastDbBackupAgeInHoursForDbs   , "If 'LastDbBackupAgeInHours' is true; Only for the databases listed (regexp is used, blank=for-all-dbs). After this rule the 'skip' rule is evaluated.", new RegExpInputValidator()));
		list.add(new CmSettingsHelper("LastDbBackupAgeInHours SkipDbs",                  PROPKEY_alarm_LastDbBackupAgeInHoursSkipDbs   , String .class, conf.getProperty   (PROPKEY_alarm_LastDbBackupAgeInHoursSkipDbs   , DEFAULT_alarm_LastDbBackupAgeInHoursSkipDbs  ), DEFAULT_alarm_LastDbBackupAgeInHoursSkipDbs  , "If 'LastDbBackupAgeInHours' is true; Discard databases listed (regexp is used). Before this rule the 'for/keep' rule is evaluated",                     new RegExpInputValidator()));
		list.add(new CmSettingsHelper("LastDbBackupAgeInHours ForSrv",                   PROPKEY_alarm_LastDbBackupAgeInHoursForSrv    , String .class, conf.getProperty   (PROPKEY_alarm_LastDbBackupAgeInHoursForSrv    , DEFAULT_alarm_LastDbBackupAgeInHoursForSrv   ), DEFAULT_alarm_LastDbBackupAgeInHoursForSrv   , "If 'LastDbBackupAgeInHours' is true; Only for the servers listed (regexp is used, blank=for-all-srv). After this rule the 'skip' rule is evaluated.",   new RegExpInputValidator()));
		list.add(new CmSettingsHelper("LastDbBackupAgeInHours SkipSrv",                  PROPKEY_alarm_LastDbBackupAgeInHoursSkipSrv   , String .class, conf.getProperty   (PROPKEY_alarm_LastDbBackupAgeInHoursSkipSrv   , DEFAULT_alarm_LastDbBackupAgeInHoursSkipSrv  ), DEFAULT_alarm_LastDbBackupAgeInHoursSkipSrv  , "If 'LastDbBackupAgeInHours' is true; Discard servers listed (regexp is used). Before this rule the 'for/keep' rule is evaluated",                       new RegExpInputValidator()));

		list.add(new CmSettingsHelper("LastLogBackupAgeInHours",          isAlarmSwitch, PROPKEY_alarm_LastLogBackupAgeInHours         , Integer.class, conf.getIntProperty(PROPKEY_alarm_LastLogBackupAgeInHours         , DEFAULT_alarm_LastLogBackupAgeInHours        ), DEFAULT_alarm_LastLogBackupAgeInHours        , "If 'LastLogBackupAgeInHours' is greater than ## then send 'AlarmEventOldBackup'." ));
		list.add(new CmSettingsHelper("LastLogBackupAgeInHours ForDbs",                  PROPKEY_alarm_LastLogBackupAgeInHoursForDbs   , String .class, conf.getProperty   (PROPKEY_alarm_LastLogBackupAgeInHoursForDbs   , DEFAULT_alarm_LastLogBackupAgeInHoursForDbs  ), DEFAULT_alarm_LastLogBackupAgeInHoursForDbs  , "If 'LastLogBackupAgeInHours' is true; Only for the databases listed (regexp is used, blank=skip-no-dbs). After this rule the 'skip' rule is evaluated.", new RegExpInputValidator()));
		list.add(new CmSettingsHelper("LastLogBackupAgeInHours SkipDbs",                 PROPKEY_alarm_LastLogBackupAgeInHoursSkipDbs  , String .class, conf.getProperty   (PROPKEY_alarm_LastLogBackupAgeInHoursSkipDbs  , DEFAULT_alarm_LastLogBackupAgeInHoursSkipDbs ), DEFAULT_alarm_LastLogBackupAgeInHoursSkipDbs , "If 'LastLogBackupAgeInHours' is true; Discard databases listed (regexp is used). Before this rule the 'for/keep' rule is evaluated",                     new RegExpInputValidator()));
		list.add(new CmSettingsHelper("LastLogBackupAgeInHours ForSrv",                  PROPKEY_alarm_LastLogBackupAgeInHoursForSrv   , String .class, conf.getProperty   (PROPKEY_alarm_LastLogBackupAgeInHoursForSrv   , DEFAULT_alarm_LastLogBackupAgeInHoursForSrv  ), DEFAULT_alarm_LastLogBackupAgeInHoursForSrv  , "If 'LastLogBackupAgeInHours' is true; Only for the servers listed (regexp is used, blank=skip-no-srv). After this rule the 'skip' rule is evaluated.",   new RegExpInputValidator()));
		list.add(new CmSettingsHelper("LastLogBackupAgeInHours SkipSrv",                 PROPKEY_alarm_LastLogBackupAgeInHoursSkipSrv  , String .class, conf.getProperty   (PROPKEY_alarm_LastLogBackupAgeInHoursSkipSrv  , DEFAULT_alarm_LastLogBackupAgeInHoursSkipSrv ), DEFAULT_alarm_LastLogBackupAgeInHoursSkipSrv , "If 'LastLogBackupAgeInHours' is true; Discard servers listed (regexp is used). Before this rule the 'for/keep' rule is evaluated",                       new RegExpInputValidator()));

		list.add(new CmSettingsHelper("LowDbFreeSpaceInMb",               isAlarmSwitch, PROPKEY_alarm_LowDbFreeSpaceInMb              , String.class, conf.getProperty    (PROPKEY_alarm_LowDbFreeSpaceInMb              , DEFAULT_alarm_LowDbFreeSpaceInMb             ), DEFAULT_alarm_LowDbFreeSpaceInMb             , "If 'LowDbFreeSpaceInMb' is greater than ## MB then send 'AlarmEventLowDbFreeSpace'. format: db1=#, db2=#, db3=#  (Note: the 'dbname' can use regexp)"     , new MapNumberValidator()));
		list.add(new CmSettingsHelper("LowLogFreeSpaceInMb",              isAlarmSwitch, PROPKEY_alarm_LowLogFreeSpaceInMb             , String.class, conf.getProperty    (PROPKEY_alarm_LowLogFreeSpaceInMb             , DEFAULT_alarm_LowLogFreeSpaceInMb            ), DEFAULT_alarm_LowLogFreeSpaceInMb            , "If 'LowLogFreeSpaceInMb' is greater than ## MB then send 'AlarmEventLowLogFreeSpace'.format: db1=#, db2=#, db3=#  (Note: the 'dbname' can use regexp)"    , new MapNumberValidator()));
		list.add(new CmSettingsHelper("LowDbFreeSpaceInPct",              isAlarmSwitch, PROPKEY_alarm_LowDbFreeSpaceInPct             , String.class, conf.getProperty    (PROPKEY_alarm_LowDbFreeSpaceInPct             , DEFAULT_alarm_LowDbFreeSpaceInPct            ), DEFAULT_alarm_LowDbFreeSpaceInPct            , "If 'LowDbFreeSpaceInPct' is less than ## Percent then send 'AlarmEventLowDbFreeSpace'.format: db1=#, db2=#, db3=#  (Note: the 'dbname' can use regexp)"   , new MapNumberValidator()));
		list.add(new CmSettingsHelper("LowLogFreeSpaceInPct",             isAlarmSwitch, PROPKEY_alarm_LowLogFreeSpaceInPct            , String.class, conf.getProperty    (PROPKEY_alarm_LowLogFreeSpaceInPct            , DEFAULT_alarm_LowLogFreeSpaceInPct           ), DEFAULT_alarm_LowLogFreeSpaceInPct           , "If 'LowLogFreeSpaceInPct' is less than ## Percent then send 'AlarmEventLowLogFreeSpace'.format: db1=#, db2=#, db3=#  (Note: the 'dbname' can use regexp)" , new MapNumberValidator()));

		list.add(new CmSettingsHelper("LowOsDiskFreeSpaceInMb",           isAlarmSwitch, PROPKEY_alarm_LowOsDiskFreeSpaceInMb          , String.class, conf.getProperty    (PROPKEY_alarm_LowOsDiskFreeSpaceInMb          , DEFAULT_alarm_LowOsDiskFreeSpaceInMb         ), DEFAULT_alarm_LowOsDiskFreeSpaceInMb         , "If 'LogOsDiskFreeMb' or 'DataOsDiskFreeMb' is less than ## MB then send 'AlarmEventLowOsDiskFreeSpace'.format: db1=#, db2=#, db3=#  (Note: the 'dbname' can use regexp)"        , new MapNumberValidator()));
		list.add(new CmSettingsHelper("LowOsDiskFreeSpaceInPct",          isAlarmSwitch, PROPKEY_alarm_LowOsDiskFreeSpaceInPct         , String.class, conf.getProperty    (PROPKEY_alarm_LowOsDiskFreeSpaceInPct         , DEFAULT_alarm_LowOsDiskFreeSpaceInPct        ), DEFAULT_alarm_LowOsDiskFreeSpaceInPct        , "If 'LogOsDiskFreePct' or 'DataOsDiskFreePct' is less than ## Percent then send 'AlarmEventLowOsDiskFreeSpace'.format: db1=#, db2=#, db3=#  (Note: the 'dbname' can use regexp)" , new MapNumberValidator()));

//		list.add(new CmSettingsHelper("MandatoryDatabaseList",                           PROPKEY_alarm_MandatoryDatabaseList           , String.class, conf.getProperty    (PROPKEY_alarm_MandatoryDatabaseList           , DEFAULT_alarm_MandatoryDatabaseList          ), DEFAULT_alarm_MandatoryDatabaseList          , "A list of databases that needs to be present. This is a comma separated list of databases (each name can contain regex)" ));
		
		list.add(new CmSettingsHelper("DbState",                          isAlarmSwitch, PROPKEY_alarm_DbState                         , String.class, conf.getProperty    (PROPKEY_alarm_DbState                         , DEFAULT_alarm_DbState                        ), DEFAULT_alarm_DbState                        , "If 'user_access_desc' or 'state_desc' do NOT match the regexp then send 'AlarmEventDatabaseState'. format: db1=(MULTI_USER|RESTRICTED_USER|ONLINE|OFFLINE ), db2=(MULTI_USER|ONLINE|RESTORING)  (Note: the 'dbname' can aslo be a regexp)" , new RegExpInputValidator()));

		return list;
	}
	
	
//	public static String getParentDir(String name)
//	{
//		File f = new File(name.trim());
//		return f.getParent();
//	}
//	public static void main(String[] args)
//	{
//		System.out.println(getParentDir("/var/opt/mssql/data/master.mdf      "));
//		System.out.println(getParentDir("/var/opt/mssql/data/mastlog.ldf     "));
//		System.out.println(getParentDir("C:\\var\\opt\\mssql\\data\\tempdb.mdf    "));
//		System.out.println(getParentDir("C:\\var\\opt\\mssql\\data\\templog.ldf   "));
//		System.out.println(getParentDir("C:\\var\\opt\\mssql\\data\\model.mdf     "));
//		System.out.println(getParentDir("C:\\var\\opt\\mssql\\data\\modellog.ldf  "));
//		System.out.println(getParentDir("C:\\var\\opt\\mssql\\data\\MSDBData.mdf  "));
//		System.out.println(getParentDir("C:\\var\\opt\\mssql\\data\\MSDBLog.ldf   "));
//		System.out.println(getParentDir("C:\\var\\opt\\mssql\\data\\gorans.mdf    "));
//		System.out.println(getParentDir("C:\\var\\opt\\mssql\\data\\gorans_log.ldf"));
//		System.out.println(getParentDir("/var/opt/mssql/data/gorans_data2.mdf"));
//		System.out.println(getParentDir("/var/opt/mssql/data/sek_emdb.mdf    "));
//		System.out.println(getParentDir("/var/opt/mssql/data/sek_emdb_log.ldf"));	
//	}
}

