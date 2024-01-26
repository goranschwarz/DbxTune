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
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.TimeoutException;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

import javax.naming.NameNotFoundException;

import org.apache.commons.lang3.math.NumberUtils;
import org.apache.log4j.Logger;

import com.asetune.CounterControllerSqlServer;
import com.asetune.ICounterController;
import com.asetune.ICounterController.DbmsOption;
import com.asetune.IGuiController;
import com.asetune.alarm.AlarmHandler;
import com.asetune.alarm.events.AlarmEvent;
import com.asetune.alarm.events.AlarmEventDatabaseState;
import com.asetune.alarm.events.AlarmEventDbSizeChanged;
import com.asetune.alarm.events.AlarmEventLongRunningTransaction;
import com.asetune.alarm.events.AlarmEventLowDbFreeSpace;
import com.asetune.alarm.events.AlarmEventLowLogFreeSpace;
import com.asetune.alarm.events.AlarmEventLowOsDiskFreeSpace;
import com.asetune.alarm.events.AlarmEventOldBackup;
import com.asetune.alarm.events.AlarmEventOldIncrementalBackup;
import com.asetune.alarm.events.AlarmEventOldTranLogBackup;
import com.asetune.alarm.events.sqlserver.AlarmEventDbNotInHadr;
import com.asetune.alarm.events.sqlserver.AlarmEventDbccCheckdbAge;
import com.asetune.alarm.events.sqlserver.AlarmEventQueryStoreLowFreeSpace;
import com.asetune.alarm.events.sqlserver.AlarmEventQueryStoreUnexpectedState;
import com.asetune.central.pcs.CentralPersistReader;
import com.asetune.cm.CmSettingsHelper;
import com.asetune.cm.CmSettingsHelper.MapNumberValidator;
import com.asetune.cm.CmSettingsHelper.RegExpInputValidator;
import com.asetune.cm.CounterSample;
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
import com.asetune.pcs.PcsColumnOptions;
import com.asetune.pcs.PcsColumnOptions.ColumnType;
import com.asetune.sql.conn.DbxConnection;
import com.asetune.sql.conn.info.DbmsVersionInfo;
import com.asetune.sql.conn.info.DbmsVersionInfoSqlServer;
import com.asetune.utils.CollectionUtils;
import com.asetune.utils.Configuration;
import com.asetune.utils.SqlServerUtils;
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
			"dm_tran_session_transactions", 
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
			"DataOsDiskFreePct",
			"QsUsedPct"
	};
	public static final String[] DIFF_COLUMNS     = new String[] {
			"DbSizeInMbDiff",
			"LogSizeInMbDiff",
			"DataSizeInMbDiff",
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

	public static final String  PROPKEY_sample_locks                 = PROP_PREFIX + ".sample.locks";
	public static final boolean DEFAULT_sample_locks                 = true;

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
		addTrendGraph(GRAPH_NAME_DB_SIZE_MB,
			"Database Size in MB",        // Menu CheckBox text
			"Database Size in MB ("+GROUP_NAME+"->"+SHORT_NAME+")", // Label 
			TrendGraphDataPoint.createGraphProps(TrendGraphDataPoint.Y_AXIS_SCALE_LABELS_MB, CentralPersistReader.SampleType.MAX_OVER_SAMPLES),
			null, 
			LabelType.Dynamic,
			TrendGraphDataPoint.Category.SPACE,
			false, // is Percent Graph
			false, // visible at start
			0,     // graph is valid from Server Version. 0 = All Versions; >0 = Valid from this version and above 
			-1);   // minimum height

		addTrendGraph(GRAPH_NAME_LOGSIZE_LEFT_MB,
			"DB Transaction Log Space Available in MB",        // Menu CheckBox text
			"DB Transaction Log Space Available in MB ("+GROUP_NAME+"->"+SHORT_NAME+")", // Label 
			TrendGraphDataPoint.createGraphProps(TrendGraphDataPoint.Y_AXIS_SCALE_LABELS_MB, CentralPersistReader.SampleType.MIN_OVER_SAMPLES),
			null, 
			LabelType.Dynamic,
			TrendGraphDataPoint.Category.SPACE,
			false, // is Percent Graph
			false, // visible at start
			0,     // graph is valid from Server Version. 0 = All Versions; >0 = Valid from this version and above 
			-1);   // minimum height

		addTrendGraph(GRAPH_NAME_LOGSIZE_USED_MB,
			"DB Transaction Log Space Used in MB",        // Menu CheckBox text
			"DB Transaction Log Space Used in MB ("+GROUP_NAME+"->"+SHORT_NAME+")", // Label 
			TrendGraphDataPoint.createGraphProps(TrendGraphDataPoint.Y_AXIS_SCALE_LABELS_MB, CentralPersistReader.SampleType.MAX_OVER_SAMPLES),
			null, 
			LabelType.Dynamic,
			TrendGraphDataPoint.Category.SPACE,
			false, // is Percent Graph
			false, // visible at start
			0,     // graph is valid from Server Version. 0 = All Versions; >0 = Valid from this version and above 
			-1);   // minimum height

		addTrendGraph(GRAPH_NAME_LOGSIZE_USED_PCT,
			"DB Transaction Log Space Used in PCT",     // Menu CheckBox text
			"DB Transaction Log Space Used in Percent ("+GROUP_NAME+"->"+SHORT_NAME+")", // Label 
			TrendGraphDataPoint.createGraphProps(TrendGraphDataPoint.Y_AXIS_SCALE_LABELS_PERCENT, CentralPersistReader.SampleType.MAX_OVER_SAMPLES),
			null, 
			LabelType.Dynamic,
			TrendGraphDataPoint.Category.SPACE,
			true,  // is Percent Graph
			false, // visible at start
			0,     // graph is valid from Server Version. 0 = All Versions; >0 = Valid from this version and above 
			-1);   // minimum height

		addTrendGraph(GRAPH_NAME_DATASIZE_LEFT_MB,
			"DB Data Space Available in MB",        // Menu CheckBox text
			"DB Data Space Available in MB ("+GROUP_NAME+"->"+SHORT_NAME+")", // Label 
			TrendGraphDataPoint.createGraphProps(TrendGraphDataPoint.Y_AXIS_SCALE_LABELS_MB, CentralPersistReader.SampleType.MIN_OVER_SAMPLES),
			null, 
			LabelType.Dynamic,
			TrendGraphDataPoint.Category.SPACE,
			false, // is Percent Graph
			false, // visible at start
			0,     // graph is valid from Server Version. 0 = All Versions; >0 = Valid from this version and above 
			-1);   // minimum height

		addTrendGraph(GRAPH_NAME_DATASIZE_USED_MB,
			"DB Data Space Used in MB",     // Menu CheckBox text
			"DB Data Space Used in MB ("+GROUP_NAME+"->"+SHORT_NAME+")", // Label 
			TrendGraphDataPoint.createGraphProps(TrendGraphDataPoint.Y_AXIS_SCALE_LABELS_MB, CentralPersistReader.SampleType.MAX_OVER_SAMPLES),
			null, 
			LabelType.Dynamic,
			TrendGraphDataPoint.Category.SPACE,
			false,  // is Percent Graph
			false, // visible at start
			0,     // graph is valid from Server Version. 0 = All Versions; >0 = Valid from this version and above 
			-1);   // minimum height

		addTrendGraph(GRAPH_NAME_DATASIZE_USED_PCT,
			"DB Data Space Used in PCT",     // Menu CheckBox text
			"DB Data Space Used in Percent ("+GROUP_NAME+"->"+SHORT_NAME+")", // Label 
			TrendGraphDataPoint.createGraphProps(TrendGraphDataPoint.Y_AXIS_SCALE_LABELS_PERCENT, CentralPersistReader.SampleType.MAX_OVER_SAMPLES),
			null, 
			LabelType.Dynamic,
			TrendGraphDataPoint.Category.SPACE,
			true,  // is Percent Graph
			false, // visible at start
			0,     // graph is valid from Server Version. 0 = All Versions; >0 = Valid from this version and above 
			-1);   // minimum height

		addTrendGraph(GRAPH_NAME_TEMPDB_USED_MB,
			"TempDB Space Used in MB",     // Menu CheckBox text
			"TempDB Space Used in MB ("+GROUP_NAME+"->"+SHORT_NAME+")", // Label 
			TrendGraphDataPoint.createGraphProps(TrendGraphDataPoint.Y_AXIS_SCALE_LABELS_MB, CentralPersistReader.SampleType.MAX_OVER_SAMPLES),
			null, 
			LabelType.Dynamic,
			TrendGraphDataPoint.Category.SPACE,
			false, // is Percent Graph
			false, // visible at start
			0,     // graph is valid from Server Version. 0 = All Versions; >0 = Valid from this version and above 
			-1);   // minimum height



		addTrendGraph(GRAPH_NAME_OS_DISK_FREE_MB,
			"DB OS Disk Space Available in MB",        // Menu CheckBox text
			"DB OS Disk Space Available in MB ("+GROUP_NAME+"->"+SHORT_NAME+")", // Label 
			TrendGraphDataPoint.createGraphProps(TrendGraphDataPoint.Y_AXIS_SCALE_LABELS_MB, CentralPersistReader.SampleType.MIN_OVER_SAMPLES),
			null, 
			LabelType.Dynamic,
			TrendGraphDataPoint.Category.SPACE,
			false, // is Percent Graph
			false, // visible at start
			0,     // graph is valid from Server Version. 0 = All Versions; >0 = Valid from this version and above 
			-1);   // minimum height

		addTrendGraph(GRAPH_NAME_OS_DISK_USED_MB,
			"DB OS Disk Space Used in MB",        // Menu CheckBox text
			"DB OS Disk Space Used in MB ("+GROUP_NAME+"->"+SHORT_NAME+")", // Label 
			TrendGraphDataPoint.createGraphProps(TrendGraphDataPoint.Y_AXIS_SCALE_LABELS_MB, CentralPersistReader.SampleType.MAX_OVER_SAMPLES),
			null, 
			LabelType.Dynamic,
			TrendGraphDataPoint.Category.SPACE,
			false, // is Percent Graph
			false, // visible at start
			0,     // graph is valid from Server Version. 0 = All Versions; >0 = Valid from this version and above 
			-1);   // minimum height

		addTrendGraph(GRAPH_NAME_OS_DISK_USED_PCT,
			"DB OS Disk Space Used in PCT",     // Menu CheckBox text
			"DB OS Disk Space Used in Percent ("+GROUP_NAME+"->"+SHORT_NAME+")", // Label 
			TrendGraphDataPoint.createGraphProps(TrendGraphDataPoint.Y_AXIS_SCALE_LABELS_PERCENT, CentralPersistReader.SampleType.MAX_OVER_SAMPLES),
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
	public String[] getDependsOnConfigForVersion(DbxConnection conn, DbmsVersionInfo versionInfo)
	{
		return NEED_CONFIG;
	}

//	@Override
//	public void addMonTableDictForVersion(DbxConnection conn, long srvVersion, boolean isClusterEnabled)
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
//						"  <li><code>index pages</code>      - index pages � number of index-only pages, if the input parameters specified processing indexes on the objects. To determine the number of pages used for only the index-level pages, subtract the number of large object (LOB) pages from the number of index pages.</li>" +
//						"  <li><code>oam pages</code>        - number of OAM pages for all OAM chains, as selected by the input parameters.<br>"
//						                                    + "For example, if you specify:<br>"
//						                                    + "<code>spaceusage(database_id, object_id, index_id)</code><br>"
//						                                    + "oam pages indicates the number of OAM pages found for this index and any of its local index partitions. If you run spaceusage against a specific object, oam pages returns the amount of overhead for the extra pages used for this object�s space management.<br>"
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
//						"<code>index pages</code> - index pages � number of index-only pages, if the input parameters specified processing indexes on the objects. To determine the number of pages used for only the index-level pages, subtract the number of large object (LOB) pages from the number of index pages.<br>" +
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
//						"<code>index pages</code> - index pages � number of index-only pages, if the input parameters specified processing indexes on the objects. To determine the number of pages used for only the index-level pages, subtract the number of large object (LOB) pages from the number of index pages.<br>" +
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
//						"oam pages indicates the number of OAM pages found for this index and any of its local index partitions. If you run spaceusage against a specific object, oam pages returns the amount of overhead for the extra pages used for this object�s space management.<br>" +
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
	public void addMonTableDictForVersion(DbxConnection conn, DbmsVersionInfo versionInfo)
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
			mtd.addColumn(cmName, "LogFileId"               ,"<html>SQL-Servers internal ID of the <i>file</i>. <br>NOTE: Only the <b>first</b> file is visible here.</html>");    //   = osvLog.file_id 
			mtd.addColumn(cmName, "LogFileCount"            ,"<html>Number of LOG file(s). <br>NOTE: Only the <b>first</b> file is displayed in the tab, this would be number of files.</html>");    //   = osvLog.file_count 
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
			mtd.addColumn(cmName, "DataFileCount"           ,"<html>Number of DATA file(s) available. <br>NOTE: Only the <b>first</b> file is displayed in the tab, this would be number of files.</html>");    //   = osvData.file_count 
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

			mtd.addColumn(cmName, "LastDbBackupTime"         ,"<html>Last Date/time a backup was done.</html>");                                                                    //   =         (SELECT bi.last_backup_finish_date                            FROM #backupInfo bi WHERE d.name = bi.database_name AND bi.type = 'D') 
			mtd.addColumn(cmName, "LastDbBackupAgeInHours"   ,"<html>How many hours ago was the last backup taken.</html>");                                                        //   = isnull( (SELECT datediff(hour, bi.last_backup_finish_date, getdate()) FROM #backupInfo bi WHERE d.name = bi.database_name AND bi.type = 'D'), -1) 
			mtd.addColumn(cmName, "LastIncDbBackupTime"      ,"<html>Last Date/time a incremental backup was done.</html>");                                                                    //   =         (SELECT bi.last_backup_finish_date                            FROM #backupInfo bi WHERE d.name = bi.database_name AND bi.type = 'D') 
			mtd.addColumn(cmName, "LastIncDbBackupAgeInHours","<html>How many hours ago was the last incremental backup taken.</html>");                                                        //   = isnull( (SELECT datediff(hour, bi.last_backup_finish_date, getdate()) FROM #backupInfo bi WHERE d.name = bi.database_name AND bi.type = 'D'), -1) 
			mtd.addColumn(cmName, "LastLogBackupTime"        ,"<html>Last Date/time a LOG backup was done.</html>");                                                                //   =         (SELECT bi.last_backup_finish_date                            FROM #backupInfo bi WHERE d.name = bi.database_name AND bi.type = 'L') 
			mtd.addColumn(cmName, "LastLogBackupAgeInHours"  ,"<html>How many hours ago was the last LOG backup taken.</html>");                                                    //   = isnull( (SELECT datediff(hour, bi.last_backup_finish_date, getdate()) FROM #backupInfo bi WHERE d.name = bi.database_name AND bi.type = 'L'), -1) 

			mtd.addColumn(cmName, "QsIsEnabled"             ,"<html>If the Query Store is enabled on not for this database</html>"); 
			mtd.addColumn(cmName, "QsIsOk"                  ,"<html>If column '' and '' is in the same state, then this is true/yes, if it's NULL means that no entry was found in the table <i>dbname</i>.sys.database_query_store_options</html>"); 
			mtd.addColumn(cmName, "QsDesiredState"          ,"<html>Indicates the desired operation mode of Query Store, explicitly set by user.</html>"); 
			mtd.addColumn(cmName, "QsActualState"           ,"<html>Indicates the operation mode of Query Store. In addition to list of desired states required by the user, actual state can be an error state.</html>"); 
			mtd.addColumn(cmName, "QsMaxSizeInMb"           ,"<html>Maximum disk size for the Query Store in megabytes (MB)</html>"); 
			mtd.addColumn(cmName, "QsUsedSpaceInMb"         ,"<html>Size of Query Store on disk in megabytes.</html>"); 
			mtd.addColumn(cmName, "QsFreeSpaceInMb"         ,"<html>Free Space in Query Store before it gets full, and state will be set to <i>read-only</i>.</html>"); 
			mtd.addColumn(cmName, "QsUsedPct"               ,"<html>How many Percent of the Query Store MAX Size have we used.</html>"); 
			mtd.addColumn(cmName, "QsReadOnlyReason"        ,"<html>When the <b>desired_state_desc</b> is READ_WRITE and the <b>actual_state_desc</b> is READ_ONLY, <b>readonly_reason</b> returns a bit map to indicate why the Query Store is in readonly mode."
			                                                + "<ul>"
			                                                + "   <li>1 - database is in read-only mode</li>"
			                                                + "   <li>2 - database is in single-user mode</li>"
			                                                + "   <li>4 - database is in emergency mode</li>"
			                                                + "   <li>8 - database is secondary replica (applies to Always On and Azure SQL Database geo-replication). This value can be effectively observed only on readable secondary replicas</li>"
			                                                + "   <li>65536 - the Query Store has reached the size limit set by the MAX_STORAGE_SIZE_MB option. For more information about this option, see ALTER DATABASE SET options (Transact-SQL)</li>"
			                                                + "   <li>131072 - The number of different statements in Query Store has reached the internal memory limit. Consider removing queries that you do not need or upgrading to a higher service tier to enable transferring Query Store to read-write mode.</li>"
			                                                + "   <li>262144 - Size of in-memory items waiting to be persisted on disk has reached the internal memory limit. Query Store will be in read-only mode temporarily until the in-memory items are persisted on disk.</li>"
			                                                + "   <li>524288 - Database has reached disk size limit. Query Store is part of user database, so if there is no more available space for a database, that means that Query Store cannot grow further anymore.</li>"
			                                                + "</ul>"
			                                                + "To switch the Query Store operations mode back to read-write, see Verify Query Store is Collecting Query Data Continuously section of Best Practice with the Query Store."
			                                                + "</html>"); 

			mtd.addColumn(cmName, "OldestTranSqlText"       ,"<html>SQL Text of the oldest open transaction</html>");                                                              //   = oti.most_recent_sql_text 
			mtd.addColumn(cmName, "OldestTranShowPlanText"  ,"<html>Showplan Text of the oldest open transaction</html>");                                                         //   = oti.plan_text 
			mtd.addColumn(cmName, "OldestTranLocks"         ,"<html>Summary of what locks this spid is using.</html>");                                                            //   = oti.plan_text 
			mtd.addColumn(cmName, "LastGoodCheckDbTime"     ,"<html>Timestamp when we last executed a successfull DBCC CHECKDB. <br>NOTE: only in 2016 SP2 and later.</html>"); 
			mtd.addColumn(cmName, "LastGoodCheckDbDays"     ,"<html>How many days since we executed a successfull DBCC CHECKDB. <br>NOTE: only in 2016 SP2 and later.</html>"); 
		}
		catch (NameNotFoundException e) 
		{
			_logger.warn("Problems in cm='" + CM_NAME + "', adding addMonTableDictForVersion. Caught: " + e); 
		//	System.out.println("Problems in cm='" + CM_NAME + "', adding addMonTableDictForVersion. Caught: " + e); 
		}
	}

	@Override
	public Map<String, PcsColumnOptions> getPcsColumnOptions()
	{
		Map<String, PcsColumnOptions> map = super.getPcsColumnOptions();

		// No settings in the super, create one, and set it at super
		if (map == null)
		{
			map = new HashMap<>();
			map.put("OldestTranSqlText"     , new PcsColumnOptions(ColumnType.DICTIONARY_COMPRESSION));
			map.put("OldestTranShowPlanText", new PcsColumnOptions(ColumnType.DICTIONARY_COMPRESSION));
//			map.put("OldestTranLocks"       , new PcsColumnOptions(ColumnType.DICTIONARY_COMPRESSION));

			// Set the map in the super
			setPcsColumnOptions(map);
		}

		return map;
	}

	// Present Database NAME instead of ID for PK tool-tip
	@Override
	public Map<String, String> getPkRewriteMap(int modelRow)
	{
		List<String> pkList = getPk();
		if (pkList == null)
			return null;

		Map<String, String> map = new LinkedHashMap<>();
		for (String pkCol : pkList)
		{
			if ("database_id".equals(pkCol)) { map.put("DBName", getAbsString(modelRow, "DBName"));     }
			else
			{ 
				map.put(pkCol, getAbsString(modelRow, pkCol));        
			}
		}
		return map;
	}

	@Override
	public List<String> getPkForVersion(DbxConnection conn, DbmsVersionInfo versionInfo)
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


//	@Override
//	public String getSqlForVersion(DbxConnection conn, DbmsVersionInfo versionInfo)
//	{
//		DbmsVersionInfoSqlServer versionInfo = (DbmsVersionInfoSqlServer) conn.getDbmsVersionInfo();
//		
//		if (versionInfo.isAzureDb() || versionInfo.isAzureSynapseAnalytics())
//		{
//			return getSqlForVersion_azure(conn, versionInfo);
//		}
//		else
//		{
//			return getSqlForVersion_onPrem(conn, srvVersion, isAzure);
//		}
//	}
//
//	private String getSqlForVersion_azure(DbxConnection conn, DbmsVersionInfoSqlServer versionInfo)
//	{
//		return "not-yet-implemented";
//	}
//
//	private String getSqlForVersion_onPrem(DbxConnection conn, DbmsVersionInfo versionInfo)
//	{
//		return "not-yet-implemented";
//	}

//	@Override
//	public String getSqlForVersion(DbxConnection conn, DbmsVersionInfo versionInfo)
//	{
//		// Table names are probably different in Normal SQL-Server and Azure SQL-Server
//		String dm_db_file_space_usage        = "sys.dm_db_file_space_usage";
//		String dm_db_log_space_usage         = "sys.dm_db_log_space_usage";
//		String dm_exec_sessions              = "sys.dm_exec_sessions";
//		String dm_exec_connections           = "sys.dm_exec_connections";
//		String dm_exec_requests              = "sys.dm_exec_requests";
//		String dm_tran_database_transactions = "sys.dm_tran_database_transactions";
//		String dm_tran_session_transactions  = "sys.dm_tran_session_transactions";
//		String dm_tran_active_transactions   = "sys.dm_tran_active_transactions";
//		String dm_exec_sql_text              = "sys.dm_exec_sql_text";
//		String dm_exec_query_plan            = "sys.dm_exec_query_plan";
//		String master_files                  = "sys.master_files";
//		String dm_os_volume_stats            = "sys.dm_os_volume_stats";
//		String databases                     = "sys.databases";
//		String backupset                     = "msdb.dbo.backupset";
//
//		// get Actual-Query-Plan instead of Estimated-QueryPlan
//		if (isDbmsOptionEnabled(DbmsOption.SQL_SERVER__LAST_QUERY_PLAN_STATS))
//		{
//			dm_exec_query_plan = "sys.dm_exec_query_plan_stats";
//		}
//
//		if (isAzure)
//		{
//			dm_db_file_space_usage       = "sys.dm_pdw_nodes_db_file_space_usage";
//			dm_db_log_space_usage        = "sys.dm_db_log_space_usage";               // same as Normal SQL-Server
//			dm_exec_sessions             = "sys.dm_pdw_nodes_exec_sessions";
//			dm_exec_connections          = "sys.dm_pdw_exec_connections";
//			dm_exec_requests             = "sys.dm_exec_requests";                    // same as Normal SQL-Server
//			dm_tran_session_transactions = "sys.dm_pdw_nodes_tran_session_transactions";
//			dm_tran_active_transactions  = "sys.dm_pdw_nodes_tran_active_transactions";
//			dm_exec_sql_text             = "sys.dm_exec_sql_text";                    // same as Normal SQL-Server
//			dm_exec_query_plan           = "sys.dm_exec_query_plan";                  // same as Normal SQL-Server
//			master_files                 = "sys.master_files";                        // same as Normal SQL-Server
//			dm_os_volume_stats           = "sys.dm_os_volume_stats";                  // same as Normal SQL-Server
//			databases                    = "sys.databases";                           // same as Normal SQL-Server
//			backupset                    = "msdb.dbo.backupset";                      // same as Normal SQL-Server
//		}
//
//		// Special thing for Azure SQL Database
//		DbmsVersionInfoSqlServer versionInfo = (DbmsVersionInfoSqlServer) conn.getDbmsVersionInfo();
//		if (versionInfo.isAzureDb() || versionInfo.isAzureSynapseAnalytics())
//		{
//			// NOTE: for Azure SQL Database, tempdb will have faulty 'devicename' and 'physical_name' (but lets fix that LATER)
//		}
//
//		// ----- SQL-Server 2012 and above
//		String availabilityGroupName          = "";
//		String availabilityGroupRole          = "";
//		String availabilityGroupPrimaryServer = "";
//		String whereAvailabilityGroup         = "";
//		
//		if (srvVersion >= Ver.ver(2012) || isAzure)
//		{
//			availabilityGroupName          = "    ,ag_name                  = (SELECT ag.name             FROM sys.availability_replicas ar JOIN sys.availability_groups                ag ON ar.group_id = ag.group_id  WHERE ar.replica_id  = d.replica_id) \n"; 
//			availabilityGroupRole          = "    ,ag_role                  = (SELECT ars.role_desc       FROM sys.dm_hadr_availability_replica_states ars                                                               WHERE ars.replica_id = d.replica_id) \n";
//			availabilityGroupPrimaryServer = "    ,ag_primary_server        = (SELECT ags.primary_replica FROM sys.availability_replicas ar JOIN sys.dm_hadr_availability_group_states ags ON ar.group_id = ags.group_id WHERE ar.replica_id  = d.replica_id) \n";
//			whereAvailabilityGroup         = "  OR d.replica_id is not null \n";
//		}
//
//		// ----- SQL-Server 2014 and above
//		String user_objects_deferred_dealloc_page_count = "0";
//		if (srvVersion >= Ver.ver(2014))
//		{
//			user_objects_deferred_dealloc_page_count = "user_objects_deferred_dealloc_page_count";
//		}
//
//		// ----- SQL-Server 2016 and above
//		String queryStoreCreateTempTable   = "";
//		String queryStoreColumns           = "";
//		String queryStoreJoin              = "";
//		String queryStoreDropTempTable1    = "";
//		String queryStoreDropTempTable2    = "";
//
//		if (srvVersion >= Ver.ver(2016))
//		{
//			queryStoreCreateTempTable   = "--------------------------- \n"
//			                            + "-- Query Store \n"
//			                            + "--------------------------- \n"
//			                            + "CREATE TABLE #queryStore (database_id int, is_enabled bit, state_is_ok varchar(3), desired_state_desc varchar(60), actual_state_desc varchar(60), max_storage_size_mb bigint, current_storage_size_mb bigint, usedPct numeric(10,1), readonly_reason int) \n"
//			                            + "INSERT INTO #queryStore \n"
//			                            + "EXEC sp_MSforeachdb ' \n"
//			                            + "SELECT /* ${cmCollectorName} */ \n"
//			                            + "     DB_ID(''?'')   as database_id \n"
//			                            + "    ,CASE WHEN (desired_state = 0 /*0=OFF*/ ) THEN cast(0 as bit) ELSE cast(1 as bit) END as is_enabled \n"
//			                            + "    ,CASE WHEN (desired_state = actual_state) THEN ''YES''        ELSE ''NO''         END as state_is_ok \n"
//			                            + "    ,desired_state_desc \n"
//			                            + "    ,actual_state_desc \n"
//			                            + "    ,max_storage_size_mb \n"
//			                            + "    ,current_storage_size_mb \n"
//			                            + "    ,cast(((current_storage_size_mb*1.0)/(max_storage_size_mb*1.0))*100.0 as numeric(10,1)) as usedPct \n"
//			                            + "    ,readonly_reason \n"
//			                            + "FROM [?].sys.database_query_store_options' \n"
//			                            + " \n";
//
//			queryStoreColumns           = "    ,QsIsEnabled              = qs.is_enabled \n"
//			                            + "    ,QsIsOk                   = qs.state_is_ok \n"
//			                            + "    ,QsDesiredState           = qs.desired_state_desc \n"
//			                            + "    ,QsActualState            = qs.actual_state_desc \n"
//			                            + "    ,QsMaxSizeInMb            = qs.max_storage_size_mb \n"
//			                            + "    ,QsUsedSpaceInMb          = qs.current_storage_size_mb \n"
//			                            + "    ,QsFreeSpaceInMb          = qs.max_storage_size_mb - qs.current_storage_size_mb \n"
//			                            + "    ,QsUsedPct                = qs.usedPct \n"
//			                            + "    ,QsReadOnlyReason         = qs.readonly_reason \n"
//			                            + "\n";
//
//			queryStoreJoin              = "LEFT OUTER JOIN #queryStore  qs     ON d.database_id = qs     .database_id \n";
//			queryStoreDropTempTable1    = "if (object_id('tempdb..#queryStore') is not null) drop table #queryStore \n";
//			queryStoreDropTempTable2    = "drop table #queryStore \n";
//		}
//
//		// ----- SQL-Server 2016, SP2 and above
//		String lastGoodCheckDbTime   = "";
//		String lastGoodCheckDbDays   = "";
//		if (srvVersion >= Ver.ver(2016,0,0, 2)) // 2016 SP2
//		{
//			lastGoodCheckDbTime += "\n";
//			lastGoodCheckDbTime += "    ,LastGoodCheckDbTime      = convert(datetime, DATABASEPROPERTYEX(d.Name, 'LastGoodCheckDbTime')) \n";
//			lastGoodCheckDbDays  = "    ,LastGoodCheckDbDays      = datediff(day, convert(datetime, DATABASEPROPERTYEX(d.Name, 'LastGoodCheckDbTime')), getdate()) \n";
//		}
//
//
//		//		String sql = ""
////			    + " \n"
////			    + "--------------------------- \n"
////			    + "-- DATA SIZE MB \n"
////			    + "--------------------------- \n"
////			    + "DECLARE @dataSizeMb TABLE (database_id int, fileGroupCount int, totalDataSizeMb numeric(12,1), usedDataMb numeric(12,1), freeDataMb numeric(12,1), usedDataPct numeric(5,1), freeDataPct numeric(5,1)) \n"
////			    + "INSERT INTO @dataSizeMb \n"
////			    + "EXEC sp_MSforeachdb ' \n"
////			    + "SELECT \n"
////			    + "     database_id \n"
////			    + "    ,fileGroupCount = count(*) \n"
////			    + "    ,totalMb        = sum(total_page_count) / 128.0 \n"
////			    + "    ,allocatedMb    = sum(allocated_extent_page_count) / 128.0 \n"
////			    + "    ,unallocatedMb  = sum(unallocated_extent_page_count) / 128.0 \n"
////			    + "    ,usedPct        = (sum(allocated_extent_page_count)  *1.0) / (sum(total_page_count)*1.0) * 100.0 \n"
////			    + "    ,freePct        = (sum(unallocated_extent_page_count)*1.0) / (sum(total_page_count)*1.0) * 100.0 \n"
////			    + "FROM [?]." + dm_db_file_space_usage + " GROUP BY database_id' \n"
////			    + " \n"
////			    + "--------------------------- \n"
////			    + "-- LOG SIZE MB \n"
////			    + "--------------------------- \n"
////			    + "DECLARE @logSizeMb TABLE (database_id int, totalLogSizeMb int, usedLogSpaceInMb int, usedLogSpaceInPct numeric(5,1), logSpaceInMbSinceLastBackup int) \n"
////			    + "INSERT INTO @logSizeMb \n"
////			    + "EXEC sp_MSforeachdb ' \n"
////			    + "SELECT \n"
////			    + "     database_id \n"
////			    + "    ,total_log_size_in_bytes/1024/1024 \n"
////			    + "    ,used_log_space_in_bytes/1024/1024 \n"
////			    + "    ,used_log_space_in_percent \n"
////			    + "    ,log_space_in_bytes_since_last_backup/1024/1024 \n"
////			    + "FROM [?]." + dm_db_log_space_usage + "' \n"
////			    + " \n"
////			    + "--------------------------- \n"
////			    + "-- Backup Info \n"
////			    + "--------------------------- \n"
////			    + "DECLARE @backupInfo TABLE (database_name nvarchar(128), type char(2), last_backup_finish_date datetime) \n"
////			    + "INSERT INTO @backupInfo \n"
////			    + "    SELECT \n"
////			    + "         bus.database_name \n"
////			    + "        ,bus.type \n"
////			    + "        ,last_backup_finish_date = MAX(bus.backup_finish_date) \n"
////			    + "    FROM msdb.dbo.backupset bus \n"
////			    + "    GROUP BY bus.database_name, bus.type \n"
////			    + "; --- we need a ';' here for SQL-Server to use the Table Variables in the below Statement \n"
////			    + " \n"
////			    + " \n"
////			    + "WITH \n"
//////			    + "--------------------------- \n"
//////			    + "-- Backup Info \n"
//////			    + "--------------------------- \n"
//////			    + "bi AS ( \n"
//////			    + "    SELECT \n"
//////			    + "         bus.database_name \n"
//////			    + "        ,bus.type \n"
//////			    + "        ,last_backup_finish_date = MAX(bus.backup_finish_date) \n"
//////			    + "    FROM " + backupset + " bus \n"
//////			    + "    GROUP BY bus.database_name, bus.type \n"
//////			    + "), \n"
////			    + "--------------------------- \n"
////			    + "-- Open Transaction Info \n"
////			    + "--------------------------- \n"
////			    + "oti AS ( \n"
////			    + "    SELECT --top 1 \n"
////			    + "         es.session_id \n"
////			    + "        ,es.database_id \n"
////			    + "        ,es.status \n"
////			    + "        ,es.program_name \n"
////			    + "        ,es.login_name \n"
////			    + "        ,es.host_name \n"
////			    + "        ,es.open_transaction_count \n"
////			    + "        ,es.last_request_start_time \n"
////			    + "        ,es.last_request_end_time \n"
////			    + "        ,er.statement_start_offset \n"
////			    + "        ,er.statement_end_offset \n"
////			    + "        ,er.wait_type \n"
////			    + "        ,er.estimated_completion_time \n"
//////			    + ",at.transaction_id \n"           // possibly to get active transaction, needs to be tested/investigated
//////			    + ",at.name AS tran_name \n"        // possibly to get active transaction, needs to be tested/investigated
//////			    + ",at.transaction_begin_time \n"   // possibly to get active transaction, needs to be tested/investigated
////				+ "        ,SUBSTRING(sql_text.text, er.statement_start_offset / 2,  \n"
////				+ "             ( CASE WHEN er.statement_end_offset = -1  \n"
////				+ "                    THEN DATALENGTH(sql_text.text)  \n"
////				+ "                    ELSE er.statement_end_offset  \n"
////				+ "               END - er.statement_start_offset ) / 2) AS most_recent_sql_text \n"
////			    + "        ,plan_text.query_plan as plan_text \n"
////			    + "        ,ROW_NUMBER() OVER (PARTITION BY es.database_id ORDER BY es.last_request_start_time) AS row_num \n"
////			    + "    FROM " + dm_exec_sessions + " es \n"
////			    + "    JOIN " + dm_exec_connections + " ec            ON es.session_id = ec.session_id \n"
////			    + "    LEFT OUTER JOIN " + dm_exec_requests + " er    ON er.session_id = es.session_id \n"
//////			    + "LEFT OUTER JOIN " + dm_tran_active_transactions + " at ON er.transaction_id = at.transaction_id \n"  // possibly to get active transaction, needs to be tested/investigated
////			    + "    OUTER APPLY " + dm_exec_sql_text + " (ec.most_recent_sql_handle) AS sql_text \n"
////			    + "    OUTER APPLY " + dm_exec_query_plan + " (er.plan_handle)          AS plan_text \n"
////			    + "    WHERE es.open_transaction_count > 0 \n"
////			    + "    --AND es.status = 'sleeping' \n"
////			    + "      AND es.is_user_process = 1 \n"
////			    + "), \n"
////			    + "--------------------------- \n"
////			    + "-- Operating System Volume DATA \n"
////			    + "--------------------------- \n"
////			    + "osvData as ( \n"
////			    + "    SELECT \n"
////			    + "         mf.database_id \n"
////			    + "        ,mf.file_id \n"
////			    + "        ,mf.size  as sizePg \n"
////			    + "        ,mf.size / 128 as sizeMb \n"
////			    + "        ,mf.max_size \n"
////			    + "        ,mf.growth \n"
////			    + "        ,mf.is_percent_growth \n"
////			    + "        , CASE WHEN mf.max_size = -1 AND mf.is_percent_growth = 1 THEN mf.size * (mf.growth / 100.0) / 128 --'PCT_GROW' \n"
////			    + "               WHEN mf.max_size = -1 AND mf.is_percent_growth = 0 THEN mf.growth / 128                     --'FIXED_GROW' \n"
////			    + "               WHEN mf.max_size > 0  AND mf.size < mf.max_size AND mf.is_percent_growth = 1 THEN mf.size * (mf.growth / 100.0) / 128 --'PCT_GROW   OK for MAX_SIZE' \n"
////			    + "               WHEN mf.max_size > 0  AND mf.size < mf.max_size AND mf.is_percent_growth = 0 THEN mf.growth / 128                     --'FIXED_GROW OK for MAX_SIZE' \n"
////			    + "               ELSE 0 \n"
////			    + "          END AS nextGrowSizeMb \n"
////			    + "        ,mf.type \n"
////			    + "        ,mf.type_desc \n"
////			    + "        ,mf.name \n"
////			    + "        ,mf.physical_name \n"
////			    + "        ,osv.volume_mount_point \n"
////			    + "        ,osv.logical_volume_name \n"
////			    + "        ,osv.total_bytes / 1024 / 1024                         AS osTotalMb \n"
////			    + "        ,osv.available_bytes / 1024 / 1024                     AS osFreeMb \n"
////			    + "        ,(osv.total_bytes - osv.available_bytes) / 1024 / 1024 AS osUsedMb \n" 
////			    + "        ,convert(numeric(5,1), (osv.available_bytes*1.0) / (osv.total_bytes*1.0) * 100.0)           AS osFreePct \n"
////			    + "        ,convert(numeric(5,1), 100.0 - ((osv.available_bytes*1.0) / (osv.total_bytes*1.0) * 100.0)) AS osUsedPct \n" 
////			    + "        ,ROW_NUMBER() OVER (PARTITION BY mf.database_id ORDER BY osv.available_bytes)               AS row_num \n"
////			    + "    FROM " + master_files + " AS mf \n"
////			    + "    CROSS APPLY " + dm_os_volume_stats + " (mf.database_id, mf.file_id) AS osv \n"
////			    + "    WHERE mf.type IN (0) /*0=ROWS, 0=LOG*/ \n"
////			    + "), \n"
////			    + "--------------------------- \n"
////			    + "-- Operating System Volume LOG \n"
////			    + "--------------------------- \n"
////			    + "osvLog as ( \n"
////			    + "    SELECT \n"
////			    + "         mf.database_id \n"
////			    + "        ,mf.file_id \n"
////			    + "        ,mf.size  as sizePg \n"
////			    + "        ,mf.size / 128 as sizeMb \n"
////			    + "        ,mf.max_size \n"
////			    + "        ,mf.growth \n"
////			    + "        ,mf.is_percent_growth \n"
////			    + "        , CASE WHEN mf.max_size = -1 AND mf.is_percent_growth = 1 THEN mf.size * (mf.growth / 100.0) / 128 --'PCT_GROW' \n"
////			    + "               WHEN mf.max_size = -1 AND mf.is_percent_growth = 0 THEN mf.growth / 128                     --'FIXED_GROW' \n"
////			    + "               WHEN mf.max_size > 0  AND mf.size < mf.max_size AND mf.is_percent_growth = 1 THEN mf.size * (mf.growth / 100.0) / 128 --'PCT_GROW   OK for MAX_SIZE' \n"
////			    + "               WHEN mf.max_size > 0  AND mf.size < mf.max_size AND mf.is_percent_growth = 0 THEN mf.growth / 128                     --'FIXED_GROW OK for MAX_SIZE' \n"
////			    + "               ELSE 0 \n"
////			    + "          END AS nextGrowSizeMb \n"
////			    + "        ,mf.type \n"
////			    + "        ,mf.type_desc \n"
////			    + "        ,mf.name \n"
////			    + "        ,mf.physical_name \n"
////			    + "        ,osv.volume_mount_point \n"
////			    + "        ,osv.logical_volume_name \n"
////			    + "        ,osv.total_bytes / 1024 / 1024                         AS osTotalMb \n"
////			    + "        ,osv.available_bytes / 1024 / 1024                     AS osFreeMb \n"
////			    + "        ,(osv.total_bytes - osv.available_bytes) / 1024 / 1024 AS osUsedMb \n" 
////			    + "        ,convert(numeric(5,1), (osv.available_bytes*1.0) / (osv.total_bytes*1.0) * 100.0)           AS osFreePct \n"
////			    + "        ,convert(numeric(5,1), 100.0 - ((osv.available_bytes*1.0) / (osv.total_bytes*1.0) * 100.0)) AS osUsedPct \n" 
////			    + "        ,ROW_NUMBER() OVER (PARTITION BY mf.database_id ORDER BY osv.available_bytes)               AS row_num \n"
////			    + "    FROM " + master_files + " AS mf \n"
////			    + "    CROSS APPLY " + dm_os_volume_stats + " (mf.database_id, mf.file_id) AS osv \n"
////			    + "    WHERE mf.type IN (1) /*0=ROWS, 0=LOG*/ \n"
////			    + ") \n"
////			    + "------------------------------- \n"
////			    + "-- The final select statement   \n"
////			    + "------------------------------- \n"
////			    + "SELECT \n"
////			    + "     DBName                   = d.Name \n"
////			    + "    ,d.database_id \n"
////			    + "    ,compatibility_level      = convert(int, d.compatibility_level) \n"
////			    + "    ,d.user_access_desc \n"
////			    + "    ,d.state_desc \n"
////			    + "    ,d.recovery_model_desc \n"
////			    + availabilityGroupName
////			    + availabilityGroupRole
////			    + availabilityGroupPrimaryServer
////			    + "    ,DataFileGroupCount       = data.fileGroupCount \n"
////			    + "    ,DBOwner                  = suser_name(d.owner_sid) \n"
////			    + "    ,d.log_reuse_wait \n"
////			    + "    ,d.log_reuse_wait_desc \n"
////			    + " \n"
////			    + "    ,DbSizeInMb               = data.totalDataSizeMb + log.totalLogSizeMb \n"
////			    + "    ,LogSizeInMb              = log.totalLogSizeMb \n"
////			    + "    ,DataSizeInMb             = data.totalDataSizeMb \n"
////			    + "    ,LogSizeUsedPct           = log.usedLogSpaceInPct \n"
////			    + "    ,DataSizeUsedPct          = data.usedDataPct \n"
//////			    + "    ,LogOsDiskUsedPct         = osvLog.osUsedPct \n"
//////			    + "    ,DataOsDiskUsedPct        = osvData.osUsedPct \n"
////			    + " \n"
////			    + "    ,LogSizeUsedInMb          = log.usedLogSpaceInMb \n"
////			    + "    ,LogSizeFreeInMb          = log.totalLogSizeMb - log.usedLogSpaceInMb \n"
////			    + "    ,LogSizeUsedInMbDiff      = log.usedLogSpaceInMb \n"
////			    + "    ,LogSizeFreeInMbDiff      = log.totalLogSizeMb - log.usedLogSpaceInMb \n"
////			    + " \n"
////			    + "    ,LogOsDisk                = osvLog.volume_mount_point \n"
////			    + "    ,LogOsDiskUsedPct         = osvLog.osUsedPct \n"
////			    + "    ,LogOsDiskFreePct         = osvLog.osFreePct \n"
////			    + "    ,LogOsFileName            = osvLog.physical_name \n"
////			    + "    ,LogFileName              = osvLog.name \n"
////			    + "    ,logFileId                = osvLog.file_id \n"
////			    + "    ,LogOsDiskUsedMb          = osvLog.osUsedMb \n"
////			    + "    ,LogOsDiskFreeMb          = osvLog.osFreeMb \n"
////			    + "    ,LogNextGrowthSizeMb      = osvLog.nextGrowSizeMb \n"
////			    + " \n"
////			    + "    ,DataSizeUsedInMb         = data.usedDataMb \n"
////			    + "    ,DataSizeFreeInMb         = data.freeDataMb \n"
////			    + "    ,DataSizeUsedInMbDiff     = data.usedDataMb \n"
////			    + "    ,DataSizeFreeInMbDiff     = data.freeDataMb \n"
////			    + " \n"
////			    + "    ,DataOsDisk               = osvData.volume_mount_point \n"
////			    + "    ,DataOsDiskUsedPct        = osvData.osUsedPct \n"
////			    + "    ,DataOsDiskFreePct        = osvData.osFreePct \n"
////			    + "    ,DataOsFileName           = osvData.physical_name \n"
////			    + "    ,DataFileName             = osvData.name \n"
////			    + "    ,DataFileId               = osvData.file_id \n"
////			    + "    ,DataOsDiskUsedMb         = osvData.osUsedMb \n"
////			    + "    ,DataOsDiskFreeMb         = osvData.osFreeMb \n"
////			    + "    ,DataNextGrowthSizeMb     = osvData.nextGrowSizeMb \n"
////			    + " \n"
//////			    + "--	,TransactionLogFull = -1 \n"
//////			    + " \n"
////			    + "    ,OldestTranStartTime      = oti.last_request_start_time \n"
////			    + "    ,OldestTranWaitType       = oti.wait_type \n"
////			    + "    ,OldestTranECT            = oti.estimated_completion_time \n"
////			    + "    ,OldestTranInSeconds      = datediff(second, oti.last_request_start_time, getdate()) \n"
////			    + "    ,OldestTranName           = -1 \n"
////			    + "    ,OldestTranSpid           = oti.session_id \n"
////			    + "    ,OldestTranProg           = oti.program_name \n"
////			    + "    ,OldestTranUser           = oti.login_name \n"
////			    + "    ,OldestTranHost           = oti.host_name \n"
//////			    + "--  ,OldestTranPage           = -1 \n"
//////			    + "--  ,OldestTranProcName       = -1 \n"
////			    + "    ,OldestTranHasSqlText     = CASE WHEN oti.most_recent_sql_text is not null THEN convert(bit,1) ELSE convert(bit,0) END \n"
////			    + "    ,OldestTranHasShowPlan    = CASE WHEN oti.plan_text            is not null THEN convert(bit,1) ELSE convert(bit,0) END \n"
////			    + " \n"
////				+ "    ,LastDbBackupTime         =         (SELECT bi.last_backup_finish_date                            FROM @backupInfo bi WHERE d.name = bi.database_name AND bi.type = 'D') \n"
////				+ "    ,LastDbBackupAgeInHours   = isnull( (SELECT datediff(hour, bi.last_backup_finish_date, getdate()) FROM @backupInfo bi WHERE d.name = bi.database_name AND bi.type = 'D'), -1) \n"
////				+ "    ,LastLogBackupTime        =         (SELECT bi.last_backup_finish_date                            FROM @backupInfo bi WHERE d.name = bi.database_name AND bi.type = 'L') \n"
////				+ "    ,LastLogBackupAgeInHours  = isnull( (SELECT datediff(hour, bi.last_backup_finish_date, getdate()) FROM @backupInfo bi WHERE d.name = bi.database_name AND bi.type = 'L'), -1) \n"
////			    + " \n"
////			    + "    ,OldestTranSqlText        = oti.most_recent_sql_text \n"
////			    + "    ,OldestTranShowPlanText   = oti.plan_text \n"
////			    + "FROM " + databases + " d \n"
////			    + "LEFT OUTER JOIN oti              ON d.database_id = oti    .database_id and oti.row_num = 1 \n"
////			    + "LEFT OUTER JOIN @dataSizeMb data ON d.database_id = data   .database_id \n"
////			    + "LEFT OUTER JOIN @logSizeMb   log ON d.database_id = log    .database_id \n"
////			    + "LEFT OUTER JOIN osvData          ON d.database_id = osvData.database_id and osvData.row_num = 1 \n"
////			    + "LEFT OUTER JOIN osvLog           ON d.database_id = osvLog .database_id and osvLog .row_num = 1 \n"
////			    + "WHERE has_dbaccess(d.name) != 0 \n"
////			    + whereAvailabilityGroup
////			    + " \n"
////			    + "";
//
//		String sql = ""
//			    + "-- Drop any of the old temporary tables if they still exists \n" 
//			    + "if (object_id('tempdb..#dataSizeMb') is not null) drop table #dataSizeMb \n" 
//			    + "if (object_id('tempdb..#logSizeMb')  is not null) drop table #logSizeMb  \n"
//			    + "if (object_id('tempdb..#backupInfo') is not null) drop table #backupInfo  \n"
//			    + "if (object_id('tempdb..#oti')        is not null) drop table #oti  \n"
//			    + "if (object_id('tempdb..#osvData')    is not null) drop table #osvData  \n"
//			    + "if (object_id('tempdb..#osvLog')     is not null) drop table #osvLog  \n"
//			    + queryStoreDropTempTable1
//			    + "go \n"
//			    + " \n"
//			    + "--------------------------- \n"
//			    + "-- DATA SIZE MB \n"
//			    + "--------------------------- \n"
//			    + "CREATE TABLE #dataSizeMb (database_id int, fileGroupCount int, totalDataSizeMb numeric(12,1), usedDataMb numeric(12,1), freeDataMb numeric(12,1), usedDataPct numeric(5,1), freeDataPct numeric(5,1)) \n"
//			    + "INSERT INTO #dataSizeMb \n"
//			    + "EXEC sp_MSforeachdb ' \n"
//			    + "SELECT /* ${cmCollectorName} */ \n"
//			    + "     database_id \n"
//			    + "    ,fileGroupCount = count(*) \n"
//			    + "    ,totalMb        = sum(total_page_count) / 128.0 \n"
//			    + "    ,allocatedMb    = sum(allocated_extent_page_count) / 128.0 \n"
//			    + "    ,unallocatedMb  = sum(unallocated_extent_page_count) / 128.0 \n"
//			    + "    ,usedPct        = (sum(allocated_extent_page_count)  *1.0) / (sum(total_page_count)*1.0) * 100.0 \n"
//			    + "    ,freePct        = (sum(unallocated_extent_page_count)*1.0) / (sum(total_page_count)*1.0) * 100.0 \n"
//			    + "FROM [?]." + dm_db_file_space_usage + " GROUP BY database_id' \n"
//			    + " \n"
//			    + "--------------------------- \n"
//			    + "-- LOG SIZE MB \n"
//			    + "--------------------------- \n"
//			    + "CREATE TABLE #logSizeMb (database_id int, totalLogSizeMb int, usedLogSpaceInMb int, usedLogSpaceInPct numeric(5,1), logSpaceInMbSinceLastBackup int) \n"
//			    + "INSERT INTO #logSizeMb \n"
//			    + "EXEC sp_MSforeachdb ' \n"
//			    + "SELECT /* ${cmCollectorName} */ \n"
//			    + "     database_id \n"
//			    + "    ,total_log_size_in_bytes/1024/1024 \n"
//			    + "    ,used_log_space_in_bytes/1024/1024 \n"
//			    + "    ,used_log_space_in_percent \n"
//			    + "    ,log_space_in_bytes_since_last_backup/1024/1024 \n"
//			    + "FROM [?]." + dm_db_log_space_usage + "' \n"
//			    + " \n"
//			    
//			    // Query Store (if 2016 or above)
//			    + queryStoreCreateTempTable
//			    
//			    + "--------------------------- \n"
//			    + "-- Backup Info \n"
//			    + "--------------------------- \n"
//			    + "    SELECT /* ${cmCollectorName} */ \n"
//			    + "         bus.database_name \n"
//			    + "        ,bus.type \n"
//			    + "        ,last_backup_finish_date = MAX(bus.backup_finish_date) \n"
//			    + "    INTO #backupInfo \n"
//			    + "    FROM " + backupset + " bus \n"
//			    + "    GROUP BY bus.database_name, bus.type \n"
//			    + " \n"
//			    + " \n"
//			    + "--------------------------- \n"
//			    + "-- Open Transaction Info \n"
//			    + "--------------------------- \n"
////add the below "somhow" to get TempdbUsageMb
////				"    ,TempdbUsageMb       = (select \n" + 
////				"                            CAST( ( \n" + // The below calculations also used in: CmTempdbSpidUsage
////				"                                        (user_objects_alloc_page_count - user_objects_dealloc_page_count - " + user_objects_deferred_dealloc_page_count + ") \n" + 
////				"                                      + (internal_objects_alloc_page_count - internal_objects_dealloc_page_count) \n" + 
////				"                                  ) / 128.0 AS decimal(12,1) \n" + 
////				"                                ) \n" + 
////				"                            from tempdb.sys.dm_db_session_space_usage ts \n" + 
////				"                            where ts.session_id = des.session_id \n" +
////				"                           ) \n" +
//			    
////			    + "    SELECT --top 1 \n"
////			    + "         es.session_id \n"
////			    + "        ,es.database_id \n"
////			    + "        ,es.status \n"
////			    + "        ,es.program_name \n"
////			    + "        ,es.login_name \n"
////			    + "        ,es.host_name \n"
////			    + "        ,es.open_transaction_count \n"
////			    
////			    + "        ,tat.transaction_id \n"
////			    + "        ,tat.name AS transaction_name  \n"
////			    + "        ,tat.transaction_begin_time \n"
////
////			    + "        ,es.last_request_start_time \n"
////			    + "        ,es.last_request_end_time \n"
////			    + "        ,er.statement_start_offset \n"
////			    + "        ,er.statement_end_offset \n"
////			    + "        ,er.wait_type \n"
////			    + "        ,er.estimated_completion_time \n"
////				+ "        ,SUBSTRING(sql_text.text, er.statement_start_offset / 2,  \n"
////				+ "             ( CASE WHEN er.statement_end_offset = -1  \n"
////				+ "                    THEN DATALENGTH(sql_text.text)  \n"
////				+ "                    ELSE er.statement_end_offset  \n"
////				+ "               END - er.statement_start_offset ) / 2 +2) AS most_recent_sql_text \n"
////			    + "        ,plan_text.query_plan as plan_text \n"
//////			    + "        ,ROW_NUMBER() OVER (PARTITION BY es.database_id ORDER BY es.last_request_start_time) AS row_num \n"
////			    + "        ,ROW_NUMBER() OVER (PARTITION BY es.database_id ORDER BY tat.transaction_begin_time) AS row_num \n"
////			    + "    INTO #oti \n"
////			    + "    FROM " + dm_exec_sessions + " es \n"
////			    + "    JOIN " + dm_exec_connections + " ec            ON es.session_id = ec.session_id \n"
////			    + "    LEFT OUTER JOIN " + dm_exec_requests + " er    ON er.session_id = es.session_id \n"
////			    + "    LEFT OUTER JOIN " + dm_tran_session_transactions + " tst ON es.session_id = tst.session_id \n"
////			    + "    LEFT OUTER JOIN " + dm_tran_active_transactions  + " tat ON tst.transaction_id = tat.transaction_id \n"
////			    + "    OUTER APPLY " + dm_exec_sql_text + " (ec.most_recent_sql_handle) AS sql_text \n"
////			    + "    OUTER APPLY " + dm_exec_query_plan + " (er.plan_handle)          AS plan_text \n"
////			    + "    WHERE es.open_transaction_count > 0 \n"
////			    + "    --AND es.status = 'sleeping' \n"
////			    + "      AND es.is_user_process = 1 \n"
////			    + "      AND es.session_id != @@spid \n"
////			    + "\n"
//			    + "    SELECT /* ${cmCollectorName} */ \n"
//			    + "         tst.session_id \n"
//			    + "        ,tdt.database_id \n"
//			    + "        ,es.status \n"
//			    + "        ,es.program_name \n"
//			    + "        ,es.login_name \n"
//			    + "        ,es.host_name \n"
//			    + "        ,es.open_transaction_count \n"
//			    + "\n"
//			    + "        ,tat.transaction_id \n"
//			    + "        ,tat.name AS transaction_name \n"
//			    + "        ,tat.transaction_begin_time \n"
//			    + "        ,tdt.database_transaction_begin_time \n"
//			    + "\n"
//			    + "        ,es.last_request_start_time \n"
//			    + "        ,es.last_request_end_time \n"
//			    + "        ,er.statement_start_offset \n"
//			    + "        ,er.statement_end_offset \n"
//			    + "        ,er.wait_type \n"
//			    + "        ,er.estimated_completion_time \n"
//			    + "        ,SUBSTRING(sql_text.text, er.statement_start_offset / 2, \n"
//			    + "             ( CASE WHEN er.statement_end_offset = -1 \n"
//			    + "                    THEN DATALENGTH(sql_text.text) \n"
//			    + "                    ELSE er.statement_end_offset \n"
//			    + "               END - er.statement_start_offset ) / 2 +2) AS most_recent_sql_text \n"
//			    + "        ,plan_text.query_plan as plan_text \n"
//			    + "        ,ROW_NUMBER() OVER (PARTITION BY tdt.database_id ORDER BY tdt.database_transaction_begin_time) AS row_num \n"
//			    + "        ,COUNT(*)     OVER (PARTITION BY tdt.database_id)                                              AS active_tran_count \n"
//			    + "    INTO #oti \n"
//			    + "    FROM " + dm_tran_database_transactions + "            tdt \n"
//			    + "    INNER JOIN      " + dm_tran_session_transactions + "             tst ON tdt.transaction_id = tst.transaction_id \n"
//			    + "    INNER JOIN      " + dm_tran_active_transactions  + "             tat ON tst.transaction_id = tat.transaction_id \n"
//			    + "    LEFT OUTER JOIN " + dm_exec_sessions             + "              es ON tst.session_id     =  es.session_id \n"
//			    + "    LEFT OUTER JOIN " + dm_exec_connections          + "              ec ON tst.session_id     =  ec.session_id \n"
//			    + "    LEFT OUTER JOIN " + dm_exec_requests             + "              er ON tst.session_id     =  er.session_id \n"
//			    + "    OUTER APPLY     " + dm_exec_sql_text   + "(ec.most_recent_sql_handle) AS sql_text \n"
//			    + "    OUTER APPLY     " + dm_exec_query_plan + "(er.plan_handle)            AS plan_text \n"
//			    + "\n"
//			    + "    WHERE tdt.database_transaction_begin_time IS NOT NULL \n"
//			    + "      AND tdt.database_transaction_type  = 1 /* 1=read/write transaction, 2=Read-only transaction, 3=System transaction */ \n"
//			    + "      AND tdt.database_transaction_log_record_count > 0 \n"
//			    + "      AND tst.session_id != @@spid \n"
//			    + "\n"
//
//			    + "--------------------------- \n"
//			    + "-- Operating System Volume DATA \n"
//			    + "--------------------------- \n"
//			    + "    SELECT /* ${cmCollectorName} */ \n"
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
//			    + "        ,COUNT(*)     OVER (PARTITION BY mf.database_id)                                            AS file_count \n"
//			    + "    INTO #osvData \n"
//			    + "    FROM " + master_files + " AS mf \n"
//			    + "    CROSS APPLY " + dm_os_volume_stats + " (mf.database_id, mf.file_id) AS osv \n"
//			    + "    WHERE mf.type IN (0)   /* 0=ROWS, 1=LOG, 2=FILESTREAM */ \n"
//			    + "\n"
//			    + "--------------------------- \n"
//			    + "-- Operating System Volume LOG \n"
//			    + "--------------------------- \n"
//			    + "    SELECT /* ${cmCollectorName} */ \n"
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
//			    + "        ,COUNT(*)     OVER (PARTITION BY mf.database_id)                                            AS file_count \n"
//			    + "    INTO #osvLog \n"
//			    + "    FROM " + master_files + " AS mf \n"
//			    + "    CROSS APPLY " + dm_os_volume_stats + " (mf.database_id, mf.file_id) AS osv \n"
//			    + "    WHERE mf.type IN (1)   /* 0=ROWS, 1=LOG, 2=FILESTREAM */ \n"
//			    + "go\n"
//			    + "\n"
//			    + "------------------------------- \n"
//			    + "-- The final select statement   \n"
//			    + "------------------------------- \n"
//			    + "SELECT /* ${cmCollectorName} */ \n"
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
//			    + "    ,LogFileId                = osvLog.file_id \n"
//			    + "    ,LogFileCount             = osvLog.file_count \n"
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
//			    + "    ,DataFileCount            = osvData.file_count \n"
//			    + "    ,DataOsDiskUsedMb         = osvData.osUsedMb \n"
//			    + "    ,DataOsDiskFreeMb         = osvData.osFreeMb \n"
//			    + "    ,DataNextGrowthSizeMb     = osvData.nextGrowSizeMb \n"
//			    + " \n"
////			    + "--	,TransactionLogFull = -1 \n"
////			    + " \n"
////			    + "    ,OldestTranStartTime      = oti.last_request_start_time \n"
//			    + "    ,OldestTranStartTime      = oti.transaction_begin_time \n"
//			    + "    ,OldestTranWaitType       = oti.wait_type \n"
//			    + "    ,OldestTranECT            = oti.estimated_completion_time \n"
////			    + "    ,OldestTranInSeconds      = datediff(second, oti.last_request_start_time, getdate()) \n"
//			    + "    ,OldestTranInSeconds      = datediff(second, oti.transaction_begin_time, getdate()) \n"
//			    + "    ,OldestTranName           = oti.transaction_name \n"
//			    + "    ,OldestTranId             = oti.transaction_id \n"
//			    + "    ,OldestTranSpid           = oti.session_id \n"
//				+ "    ,OldestTranTempdbUsageMb  = (select \n" 
//				+ "                                 CAST( ( \n"  // The below calculations also used in: CmTempdbSpidUsage
//				+ "                                             (ts.user_objects_alloc_page_count - ts.user_objects_dealloc_page_count - " + user_objects_deferred_dealloc_page_count + ") \n"  
//				+ "                                           + (ts.internal_objects_alloc_page_count - ts.internal_objects_dealloc_page_count) \n" 
//				+ "                                       ) / 128.0 AS decimal(12,1) \n" 
//				+ "                                     ) \n" 
//				+ "                                 from tempdb.sys.dm_db_session_space_usage ts \n" 
//				+ "                                 where ts.session_id = oti.session_id \n"
//				+ "                                ) \n"
////Possibly move above (OldestTranTempdbUsageMb) to section; "oti"
//			    + "    ,OldestTranProg           = oti.program_name \n"
//			    + "    ,OldestTranUser           = oti.login_name \n"
//			    + "    ,OldestTranHost           = oti.host_name \n"
////			    + "--  ,OldestTranPage           = -1 \n"
////			    + "--  ,OldestTranProcName       = -1 \n"
//			    + "    ,OldestTranHasSqlText     = CASE WHEN oti.most_recent_sql_text is not null THEN convert(bit,1) ELSE convert(bit,0) END \n"
//			    + "    ,OldestTranHasLocks       = convert(bit,0) \n"
//			    + "    ,OldestTranHasShowPlan    = CASE WHEN oti.plan_text            is not null THEN convert(bit,1) ELSE convert(bit,0) END \n"
//			    + " \n"
//				+ "    ,LastDbBackupTime          =         (SELECT bi.last_backup_finish_date                            FROM #backupInfo bi WHERE d.name = bi.database_name AND bi.type = 'D') \n"
//				+ "    ,LastDbBackupAgeInHours    = isnull( (SELECT datediff(hour, bi.last_backup_finish_date, getdate()) FROM #backupInfo bi WHERE d.name = bi.database_name AND bi.type = 'D'), -1) \n"
//				+ "    ,LastIncDbBackupTime       =         (SELECT bi.last_backup_finish_date                            FROM #backupInfo bi WHERE d.name = bi.database_name AND bi.type = 'I') \n"
//				+ "    ,LastIncDbBackupAgeInHours = isnull( (SELECT datediff(hour, bi.last_backup_finish_date, getdate()) FROM #backupInfo bi WHERE d.name = bi.database_name AND bi.type = 'I'), -1) \n"
//				+ "    ,LastLogBackupTime         =         (SELECT bi.last_backup_finish_date                            FROM #backupInfo bi WHERE d.name = bi.database_name AND bi.type = 'L') \n"
//				+ "    ,LastLogBackupAgeInHours   = isnull( (SELECT datediff(hour, bi.last_backup_finish_date, getdate()) FROM #backupInfo bi WHERE d.name = bi.database_name AND bi.type = 'L'), -1) \n"
//			    + lastGoodCheckDbTime
//			    + lastGoodCheckDbDays
//			    + " \n"
//			    + queryStoreColumns
//			    + "    ,OldestTranSqlText        = oti.most_recent_sql_text \n"
//			    + "    ,OldestTranLocks          = convert(varchar(max), null) \n" // NOTE: This will be deferred and fetched at: localCalculation()
//			    + "    ,OldestTranShowPlanText   = oti.plan_text \n"
//			    + "FROM " + databases + " d \n"
//			    + "LEFT OUTER JOIN #oti            oti ON d.database_id = oti    .database_id and oti.row_num = 1 \n"
//			    + "LEFT OUTER JOIN #dataSizeMb    data ON d.database_id = data   .database_id \n"
//			    + "LEFT OUTER JOIN #logSizeMb      log ON d.database_id = log    .database_id \n"
//			    + "LEFT OUTER JOIN #osvData    osvData ON d.database_id = osvData.database_id and osvData.row_num = 1 \n"
//			    + "LEFT OUTER JOIN #osvLog      osvLog ON d.database_id = osvLog .database_id and osvLog .row_num = 1 \n"
//			    + queryStoreJoin
//			    + "WHERE has_dbaccess(d.name) != 0 \n"
//			    + whereAvailabilityGroup
//			    + "go \n"
//			    + " \n"
//			    + "-- Drop temporary tables \n" 
//			    + "drop table #dataSizeMb \n" 
//			    + "drop table #logSizeMb  \n"
//			    + "drop table #backupInfo  \n"
//			    + "drop table #oti  \n"
//			    + "drop table #osvData  \n"
//			    + "drop table #osvLog  \n"
//			    + queryStoreDropTempTable2
//			    + "go \n"
//			    + "";
//
//		return sql;
//	}

	@Override
	public String getSqlForVersion(DbxConnection conn, DbmsVersionInfo versionInfo)
	{
		DbmsVersionInfoSqlServer ssVersionInfo = (DbmsVersionInfoSqlServer) versionInfo;
		long srvVersion = ssVersionInfo.getLongVersion();

		// Table names are probably different in Normal SQL-Server and Azure SQL-Server
		String dm_db_file_space_usage        = "sys.dm_db_file_space_usage";
		String dm_db_log_space_usage         = "sys.dm_db_log_space_usage";
		String dm_exec_sessions              = "sys.dm_exec_sessions";
		String dm_exec_connections           = "sys.dm_exec_connections";
		String dm_exec_requests              = "sys.dm_exec_requests";
		String dm_tran_database_transactions = "sys.dm_tran_database_transactions";
		String dm_tran_session_transactions  = "sys.dm_tran_session_transactions";
		String dm_tran_active_transactions   = "sys.dm_tran_active_transactions";
		String dm_exec_sql_text              = "sys.dm_exec_sql_text";
		String dm_exec_query_plan            = "sys.dm_exec_query_plan";
		String master_files                  = "sys.master_files";
		String dm_os_volume_stats            = "sys.dm_os_volume_stats";
		String databases                     = "sys.databases";
		String backupset                     = "msdb.dbo.backupset";

		// get Actual-Query-Plan instead of Estimated-QueryPlan
		if (isDbmsOptionEnabled(DbmsOption.SQL_SERVER__LAST_QUERY_PLAN_STATS))
		{
			dm_exec_query_plan = "sys.dm_exec_query_plan_stats";
		}

//		if (isAzure)
//		{
//			dm_db_file_space_usage       = "sys.dm_pdw_nodes_db_file_space_usage";
//			dm_db_log_space_usage        = "sys.dm_db_log_space_usage";               // same as Normal SQL-Server
//			dm_exec_sessions             = "sys.dm_pdw_nodes_exec_sessions";
//			dm_exec_connections          = "sys.dm_pdw_exec_connections";
//			dm_exec_requests             = "sys.dm_exec_requests";                    // same as Normal SQL-Server
//			dm_tran_session_transactions = "sys.dm_pdw_nodes_tran_session_transactions";
//			dm_tran_active_transactions  = "sys.dm_pdw_nodes_tran_active_transactions";
//			dm_exec_sql_text             = "sys.dm_exec_sql_text";                    // same as Normal SQL-Server
//			dm_exec_query_plan           = "sys.dm_exec_query_plan";                  // same as Normal SQL-Server
//			master_files                 = "sys.master_files";                        // same as Normal SQL-Server
//			dm_os_volume_stats           = "sys.dm_os_volume_stats";                  // same as Normal SQL-Server
//			databases                    = "sys.databases";                           // same as Normal SQL-Server
//			backupset                    = "msdb.dbo.backupset";                      // same as Normal SQL-Server
//		}

		String foreachDbBegin = "EXEC sp_MSforeachdb ' \n";
		String foreachDbEnd   = "FROM [?].${TABLE_NAME}' \n";

		
		// Special thing for Azure SQL Database
		if (ssVersionInfo.isAzureDb() || ssVersionInfo.isAzureSynapseAnalytics())
		{
			foreachDbBegin = "";
			foreachDbEnd   = "FROM ${TABLE_NAME} \n";
		}

		// ----- SQL-Server 2012 and above
		String availabilityGroupName          = "";
		String availabilityGroupRole          = "";
		String availabilityGroupPrimaryServer = "";
		String whereAvailabilityGroup         = "";

		if (srvVersion >= Ver.ver(2012))
		{
			availabilityGroupName          = "    ,ag_name                  = (SELECT ag.name             FROM sys.availability_replicas ar JOIN sys.availability_groups                ag ON ar.group_id = ag.group_id  WHERE ar.replica_id  = d.replica_id) \n"; 
			availabilityGroupRole          = "    ,ag_role                  = (SELECT ars.role_desc       FROM sys.dm_hadr_availability_replica_states ars                                                               WHERE ars.replica_id = d.replica_id) \n";
			availabilityGroupPrimaryServer = "    ,ag_primary_server        = (SELECT ags.primary_replica FROM sys.availability_replicas ar JOIN sys.dm_hadr_availability_group_states ags ON ar.group_id = ags.group_id WHERE ar.replica_id  = d.replica_id) \n";
			whereAvailabilityGroup         = "  OR d.replica_id is not null \n";
		}

		// ----- SQL-Server 2014 and above
//		String user_objects_deferred_dealloc_page_count = "0";
//		if (srvVersion >= Ver.ver(2014))
//		{
//			user_objects_deferred_dealloc_page_count = "user_objects_deferred_dealloc_page_count";
//		}

		// ----- SQL-Server 2016 and above
		String queryStoreCreateTempTable   = "";
		String queryStoreColumns           = "";
		String queryStoreJoin              = "";
		String queryStoreDropTempTable1    = "";
		String queryStoreDropTempTable2    = "";

		if (srvVersion >= Ver.ver(2016))
		{
			String foreachDbId = "     DB_ID(''?'')   as database_id \n";
			String foreachDbSq = "''"; // sq == SingleQuote
			if (ssVersionInfo.isAzureDb() || ssVersionInfo.isAzureSynapseAnalytics())
			{
				foreachDbId = "     DB_ID()   as database_id \n";
				foreachDbSq = "'";
			}

			queryStoreCreateTempTable   = "--------------------------- \n"
			                            + "-- Query Store \n"
			                            + "--------------------------- \n"
			                            + "CREATE TABLE #queryStore (database_id int, is_enabled bit, state_is_ok varchar(3), desired_state_desc varchar(60), actual_state_desc varchar(60), max_storage_size_mb bigint, current_storage_size_mb bigint, usedPct numeric(10,1), readonly_reason int) \n"
			                            + "INSERT INTO #queryStore \n"
			                            + foreachDbBegin
			                            + "SELECT /* ${cmCollectorName} */ \n"
			                            + foreachDbId
			                            + "    ,CASE WHEN (desired_state = 0 /*0=OFF*/ ) THEN cast(0 as bit) ELSE cast(1 as bit) END as is_enabled \n"
			                            + "    ,CASE WHEN (desired_state = actual_state) THEN #YES#        ELSE #NO#         END as state_is_ok \n".replace("#", foreachDbSq)
			                            + "    ,desired_state_desc \n"
			                            + "    ,actual_state_desc \n"
			                            + "    ,max_storage_size_mb \n"
			                            + "    ,current_storage_size_mb \n"
			                            + "    ,cast(((current_storage_size_mb*1.0)/(max_storage_size_mb*1.0))*100.0 as numeric(10,1)) as usedPct \n"
			                            + "    ,readonly_reason \n"
			                            + foreachDbEnd.replace("${TABLE_NAME}", "sys.database_query_store_options")
			                            + " \n";

			queryStoreColumns           = "    ,QsIsEnabled              = qs.is_enabled \n"
			                            + "    ,QsIsOk                   = qs.state_is_ok \n"
			                            + "    ,QsDesiredState           = qs.desired_state_desc \n"
			                            + "    ,QsActualState            = qs.actual_state_desc \n"
			                            + "    ,QsMaxSizeInMb            = qs.max_storage_size_mb \n"
			                            + "    ,QsUsedSpaceInMb          = qs.current_storage_size_mb \n"
			                            + "    ,QsFreeSpaceInMb          = qs.max_storage_size_mb - qs.current_storage_size_mb \n"
			                            + "    ,QsUsedPct                = qs.usedPct \n"
			                            + "    ,QsReadOnlyReason         = qs.readonly_reason \n"
			                            + "\n";

			queryStoreJoin              = "LEFT OUTER JOIN #queryStore  qs     ON d.database_id = qs     .database_id \n";
			queryStoreDropTempTable1    = "if (object_id('tempdb..#queryStore') is not null) drop table #queryStore \n";
			queryStoreDropTempTable2    = "drop table #queryStore \n";
		}

		// ----- SQL-Server 2016, SP2 and above
		String lastGoodCheckDbTime   = "";
		String lastGoodCheckDbDays   = "";
		if (srvVersion >= Ver.ver(2016,0,0, 2)) // 2016 SP2
		{
			lastGoodCheckDbTime += "\n";
			lastGoodCheckDbTime += "    ,LastGoodCheckDbTime      = convert(datetime, DATABASEPROPERTYEX(d.Name, 'LastGoodCheckDbTime')) \n";
			lastGoodCheckDbDays  = "    ,LastGoodCheckDbDays      = datediff(day, convert(datetime, DATABASEPROPERTYEX(d.Name, 'LastGoodCheckDbTime')), getdate()) \n";
		}

		// Is 'context_info_str' enabled (if it causes any problem, it can be disabled)
		String contextInfoStr_1 = "/*        ,context_info_str = replace(cast(es.context_info as varchar(128)),char(0),'') -- " + SqlServerCmUtils.HELPTEXT_howToEnable__context_info_str + " */ \n";
		String contextInfoStr_2 = "/*    ,OldestTranContextInfoStr = oti.context_info_str -- " + SqlServerCmUtils.HELPTEXT_howToEnable__context_info_str + " */ \n";
		if (SqlServerCmUtils.isContextInfoStrEnabled())
		{
			// Make the binary 'context_info' into a String
			contextInfoStr_1 = "        ,context_info_str = replace(cast(es.context_info as varchar(128)),char(0),'') /* " + SqlServerCmUtils.HELPTEXT_howToDisable__context_info_str + " */ \n";
			contextInfoStr_2 = "    ,OldestTranContextInfoStr = oti.context_info_str /* " + SqlServerCmUtils.HELPTEXT_howToDisable__context_info_str + " */ \n";
		}
		
		// ---- BACKUP INFORMATION
		String backupInfo = ""
				+ "--------------------------- \n"
				+ "-- Backup Info \n"
				+ "--------------------------- \n"
				+ "    SELECT /* ${cmCollectorName} */ \n"
				+ "         bus.database_name \n"
				+ "        ,backup_type             = bus.type \n"
				+ "        ,last_backup_finish_date = MAX(bus.backup_finish_date) \n"
				+ "    INTO #backupInfo \n"
				+ "    FROM " + backupset + " bus \n"
				+ "    GROUP BY bus.database_name, bus.type \n"
				+ " \n"
				+ " \n"
				+ "";

		if (ssVersionInfo.isAzureDb() || ssVersionInfo.isAzureSynapseAnalytics())
		{
			// Azure SQL Database
			backupInfo = ""
					+ "--------------------------- \n"
					+ "-- Backup Info \n"
					+ "--------------------------- \n"
					+ "    SELECT /* ${cmCollectorName} */ \n"
					+ "            db_name() as database_name \n"
					+ "           ,backup_type \n"
					+ "           ,last_backup_finish_date = MAX(bus.backup_finish_date) \n"
					+ "    INTO #backupInfo \n"
					+ "    FROM sys.dm_database_backups bus \n"
					+ "    GROUP BY bus.backup_type \n"
					+ " \n"
					+ " \n"
					+ "";
		}

		
		// Add: OS DATA disk information (not available in Azure)
		String osvDataCreateTempTable = ""
			    + "--------------------------- \n"
			    + "-- Operating System Volume DATA \n"
			    + "--------------------------- \n"
			    + "    SELECT /* ${cmCollectorName} */ \n"
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
			    + "        ,COUNT(*)     OVER (PARTITION BY mf.database_id)                                            AS file_count \n"
			    + "    INTO #osvData \n"
			    + "    FROM " + master_files + " AS mf \n"
			    + "    CROSS APPLY " + dm_os_volume_stats + " (mf.database_id, mf.file_id) AS osv \n"
			    + "    WHERE mf.type IN (0)   /* 0=ROWS, 1=LOG, 2=FILESTREAM */ \n"
			    + "\n"
			    + "";
		String osvDataColumns = " \n"
			    + "    ,DataOsDiskLabel          = osvData.logical_volume_name \n"
			    + "    ,DataOsDisk               = osvData.volume_mount_point \n"
			    + "    ,DataOsDiskUsedPct        = osvData.osUsedPct \n"
			    + "    ,DataOsDiskFreePct        = osvData.osFreePct \n"
			    + "    ,DataOsFileName           = osvData.physical_name \n"
			    + "    ,DataFileName             = osvData.name \n"
			    + "    ,DataFileId               = osvData.file_id \n"
			    + "    ,DataFileCount            = osvData.file_count \n"
			    + "    ,DataOsDiskUsedMb         = osvData.osUsedMb \n"
			    + "    ,DataOsDiskFreeMb         = osvData.osFreeMb \n"
			    + "    ,DataNextGrowthSizeMb     = osvData.nextGrowSizeMb \n"
			    + "";
		String osvDataJoin              = "LEFT OUTER JOIN #osvData    osvData ON d.database_id = osvData.database_id and osvData.row_num = 1 \n";
		String osvDataDropTempTable1    = "if (object_id('tempdb..#osvData')    is not null) drop table #osvData  \n";
		String osvDataDropTempTable2    = "drop table #osvData  \n";

		// Add: OS LOG disk information (not available in Azure)
		String osvLogCreateTempTable = ""
			    + "--------------------------- \n"
			    + "-- Operating System Volume LOG \n"
			    + "--------------------------- \n"
			    + "    SELECT /* ${cmCollectorName} */ \n"
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
			    + "        ,COUNT(*)     OVER (PARTITION BY mf.database_id)                                            AS file_count \n"
			    + "    INTO #osvLog \n"
			    + "    FROM " + master_files + " AS mf \n"
			    + "    CROSS APPLY " + dm_os_volume_stats + " (mf.database_id, mf.file_id) AS osv \n"
			    + "    WHERE mf.type IN (1)   /* 0=ROWS, 1=LOG, 2=FILESTREAM */ \n"
			    + "go\n"
			    + "\n"
			    + "";
		String osvLogColumns = " \n"
			    + "    ,LogOsDiskLabel           = osvLog.logical_volume_name \n"
			    + "    ,LogOsDisk                = osvLog.volume_mount_point \n"
			    + "    ,LogOsDiskUsedPct         = osvLog.osUsedPct \n"
			    + "    ,LogOsDiskFreePct         = osvLog.osFreePct \n"
			    + "    ,LogOsFileName            = osvLog.physical_name \n"
			    + "    ,LogFileName              = osvLog.name \n"
			    + "    ,LogFileId                = osvLog.file_id \n"
			    + "    ,LogFileCount             = osvLog.file_count \n"
			    + "    ,LogOsDiskUsedMb          = osvLog.osUsedMb \n"
			    + "    ,LogOsDiskFreeMb          = osvLog.osFreeMb \n"
			    + "    ,LogNextGrowthSizeMb      = osvLog.nextGrowSizeMb \n"
			    + "";
		String osvLogJoin               = "LEFT OUTER JOIN #osvLog      osvLog ON d.database_id = osvLog .database_id and osvLog .row_num = 1 \n";
		String osvLogDropTempTable1     = "if (object_id('tempdb..#osvLog')     is not null) drop table #osvLog  \n";
		String osvLogDropTempTable2     = "drop table #osvLog  \n";

		if (ssVersionInfo.isAzureDb() || ssVersionInfo.isAzureSynapseAnalytics())
		{
			osvDataCreateTempTable   = "";
			osvDataColumns           = "";
			osvDataJoin              = "";
			osvDataDropTempTable1    = "";
			osvDataDropTempTable2    = "";
			
			osvLogCreateTempTable    = "";
			osvLogColumns            = "";
			osvLogJoin               = "";
			osvLogDropTempTable1     = "";
			osvLogDropTempTable2     = "";
		}

		String sql = ""
			    + "-- Drop any of the old temporary tables if they still exists \n" 
			    + "if (object_id('tempdb..#dataSizeMb') is not null) drop table #dataSizeMb \n" 
			    + "if (object_id('tempdb..#logSizeMb')  is not null) drop table #logSizeMb  \n"
			    + "if (object_id('tempdb..#backupInfo') is not null) drop table #backupInfo  \n"
			    + "if (object_id('tempdb..#oti')        is not null) drop table #oti  \n"
			    + osvDataDropTempTable1
			    + osvLogDropTempTable1
			    + queryStoreDropTempTable1
			    + "go \n"
			    + " \n"
			    + "--------------------------- \n"
			    + "-- DATA SIZE MB \n"
			    + "--------------------------- \n"
			    + "CREATE TABLE #dataSizeMb (database_id int, fileGroupCount int, totalDataSizeMb numeric(12,1), usedDataMb numeric(12,1), freeDataMb numeric(12,1), usedDataPct numeric(5,1), freeDataPct numeric(5,1)) \n"
			    + "INSERT INTO #dataSizeMb \n"
			    + foreachDbBegin
			    + "SELECT /* ${cmCollectorName} */ \n"
			    + "     database_id \n"
			    + "    ,fileGroupCount = count(*) \n"
			    + "    ,totalMb        = sum(total_page_count) / 128.0 \n"
			    + "    ,allocatedMb    = sum(allocated_extent_page_count) / 128.0 \n"
			    + "    ,unallocatedMb  = sum(unallocated_extent_page_count) / 128.0 \n"
			    + "    ,usedPct        = (sum(allocated_extent_page_count)  *1.0) / (sum(total_page_count)*1.0) * 100.0 \n"
			    + "    ,freePct        = (sum(unallocated_extent_page_count)*1.0) / (sum(total_page_count)*1.0) * 100.0 \n"
			    + foreachDbEnd.replace("${TABLE_NAME}", dm_db_file_space_usage + " GROUP BY database_id")
			    + " \n"
			    + "--------------------------- \n"
			    + "-- LOG SIZE MB \n"
			    + "--------------------------- \n"
			    + "CREATE TABLE #logSizeMb (database_id int, totalLogSizeMb int, usedLogSpaceInMb int, usedLogSpaceInPct numeric(5,1), logSpaceInMbSinceLastBackup int) \n"
			    + "INSERT INTO #logSizeMb \n"
			    + foreachDbBegin
			    + "SELECT /* ${cmCollectorName} */ \n"
			    + "     database_id \n"
			    + "    ,total_log_size_in_bytes/1024/1024 \n"
			    + "    ,used_log_space_in_bytes/1024/1024 \n"
			    + "    ,used_log_space_in_percent \n"
			    + "    ,log_space_in_bytes_since_last_backup/1024/1024 \n"
			    + foreachDbEnd.replace("${TABLE_NAME}", dm_db_log_space_usage)
			    + " \n"
			    
			    // Query Store (if 2016 or above)
			    + queryStoreCreateTempTable

			    // Backup information
			    + backupInfo

			    // Add: Open Transaction Info
			    + "--------------------------- \n"
			    + "-- Open Transaction Info \n"
			    + "--------------------------- \n"
			    + "    SELECT /* ${cmCollectorName} */ \n"
			    + "         tst.session_id \n"
			    + "        ,tdt.database_id \n"
			    + "        ,es.status \n"
			    + "        ,es.program_name \n"
			    + "        ,es.login_name \n"
			    + "        ,es.host_name \n"
			    + "        ,es.open_transaction_count \n"
			    + "\n"
			    + "        ,tat.transaction_id \n"
			    + "        ,tat.name AS transaction_name \n"
			    + "        ,tat.transaction_begin_time \n"
			    + "        ,tdt.database_transaction_begin_time \n"
			    + "\n"
			    + "        ,es.last_request_start_time \n"
			    + "        ,es.last_request_end_time \n"
				+ contextInfoStr_1
			    + "        ,er.statement_start_offset \n"
			    + "        ,er.statement_end_offset \n"
			    + "        ,er.wait_type \n"
			    + "        ,er.estimated_completion_time \n"
			    + "        ,SUBSTRING(sql_text.text, er.statement_start_offset / 2, \n"
			    + "             ( CASE WHEN er.statement_end_offset = -1 \n"
			    + "                    THEN DATALENGTH(sql_text.text) \n"
			    + "                    ELSE er.statement_end_offset \n"
			    + "               END - er.statement_start_offset ) / 2 +2) AS most_recent_sql_text \n"
			    + "        ,plan_text.query_plan as plan_text \n"
			    + "        ,ROW_NUMBER() OVER (PARTITION BY tdt.database_id ORDER BY tdt.database_transaction_begin_time) AS row_num \n"
			    + "        ,COUNT(*)     OVER (PARTITION BY tdt.database_id)                                              AS active_tran_count \n"
			    + "    INTO #oti \n"
			    + "    FROM " + dm_tran_database_transactions + "            tdt \n"
			    + "    INNER JOIN      " + dm_tran_session_transactions + "             tst ON tdt.transaction_id = tst.transaction_id \n"
			    + "    INNER JOIN      " + dm_tran_active_transactions  + "             tat ON tst.transaction_id = tat.transaction_id \n"
			    + "    LEFT OUTER JOIN " + dm_exec_sessions             + "              es ON tst.session_id     =  es.session_id \n"
			    + "    LEFT OUTER JOIN " + dm_exec_connections          + "              ec ON tst.session_id     =  ec.session_id \n"
			    + "    LEFT OUTER JOIN " + dm_exec_requests             + "              er ON tst.session_id     =  er.session_id \n"
			    + "    OUTER APPLY     " + dm_exec_sql_text   + "(ec.most_recent_sql_handle) AS sql_text \n"
			    + "    OUTER APPLY     " + dm_exec_query_plan + "(er.plan_handle)            AS plan_text \n"
			    + "\n"
			    + "    WHERE tdt.database_transaction_begin_time IS NOT NULL \n"
			    + "      AND tdt.database_transaction_type  = 1 /* 1=read/write transaction, 2=Read-only transaction, 3=System transaction */ \n"
			    + "      AND tdt.database_transaction_log_record_count > 0 \n"
			    + "      AND tst.session_id != @@spid \n"
			    + "\n"
			    + ""

			    // Add: OS Data/Log info (not available in Azure)
			    + osvDataCreateTempTable
			    + osvLogCreateTempTable
			    
			    + "------------------------------- \n"
			    + "-- The final select statement   \n"
			    + "------------------------------- \n"
			    + "SELECT /* ${cmCollectorName} */ \n"
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
			    + "    ,DBOwner                  = suser_sname(d.owner_sid) \n" // or possibly: original_login(), but that does not take a parameter
			    + "    ,d.collation_name \n"
			    + "    ,d.log_reuse_wait \n"
			    + "    ,d.log_reuse_wait_desc \n"
			    + " \n"
			    + "    ,DbSizeInMbDiff           = data.totalDataSizeMb + log.totalLogSizeMb \n"
			    + "    ,LogSizeInMbDiff          = log.totalLogSizeMb \n"
			    + "    ,DataSizeInMbDiff         = data.totalDataSizeMb \n"
			    + " \n"
			    + "    ,DbSizeInMb               = data.totalDataSizeMb + log.totalLogSizeMb \n"
			    + "    ,LogSizeInMb              = log.totalLogSizeMb \n"
			    + "    ,DataSizeInMb             = data.totalDataSizeMb \n"
			    + "    ,LogSizeUsedPct           = log.usedLogSpaceInPct \n"
			    + "    ,DataSizeUsedPct          = data.usedDataPct \n"
			    + " \n"
			    + "    ,LogSizeUsedInMb          = log.usedLogSpaceInMb \n"
			    + "    ,LogSizeFreeInMb          = log.totalLogSizeMb - log.usedLogSpaceInMb \n"
			    + "    ,LogSizeUsedInMbDiff      = log.usedLogSpaceInMb \n"
			    + "    ,LogSizeFreeInMbDiff      = log.totalLogSizeMb - log.usedLogSpaceInMb \n"
			    + osvLogColumns
			    + " \n"
			    + "    ,DataSizeUsedInMb         = data.usedDataMb \n"
			    + "    ,DataSizeFreeInMb         = data.freeDataMb \n"
			    + "    ,DataSizeUsedInMbDiff     = data.usedDataMb \n"
			    + "    ,DataSizeFreeInMbDiff     = data.freeDataMb \n"
			    + osvDataColumns
			    + " \n"
//			    + "--	,TransactionLogFull = -1 \n"
//			    + " \n"
//			    + "    ,OldestTranStartTime      = oti.last_request_start_time \n"
			    + "    ,OldestTranStartTime      = oti.transaction_begin_time \n"
			    + "    ,OldestTranWaitType       = oti.wait_type \n"
			    + "    ,OldestTranECT            = oti.estimated_completion_time \n"
//			    + "    ,OldestTranInSeconds      = datediff(second, oti.last_request_start_time, getdate()) \n"
			    + "    ,OldestTranInSeconds      = datediff(second, oti.transaction_begin_time, getdate()) \n"
			    + "    ,OldestTranName           = oti.transaction_name \n"
			    + "    ,OldestTranId             = oti.transaction_id \n"
			    + "    ,OldestTranSpid           = oti.session_id \n"
//			    + "    ,OldestTranTempdbUsageMb         = convert(numeric(12,1), -1) \n" 
			    + "    ,OldestTranTempdbUsageMbAll      = convert(numeric(12,1), NULL) \n" 
			    + "    ,OldestTranTempdbUsageMbUser     = convert(numeric(12,1), NULL) \n" 
			    + "    ,OldestTranTempdbUsageMbInternal = convert(numeric(12,1), NULL) \n" 
//			    + "    ,OldestTranTempdbUsageMb  = (select \n" 
//			    + "                                 CAST( ( \n"  // The below calculations also used in: CmTempdbSpidUsage
//			    + "                                             (ts.user_objects_alloc_page_count - ts.user_objects_dealloc_page_count - " + user_objects_deferred_dealloc_page_count + ") \n"  
//			    + "                                           + (ts.internal_objects_alloc_page_count - ts.internal_objects_dealloc_page_count) \n" 
//			    + "                                       ) / 128.0 AS decimal(12,1) \n" 
//			    + "                                     ) \n" 
//			    + "                                 from tempdb.sys.dm_db_session_space_usage ts \n" 
//			    + "                                 where ts.session_id = oti.session_id \n"
//			    + "                                ) \n"
//Possibly move above (OldestTranTempdbUsageMb) to section; "oti"
			    + "    ,OldestTranProg           = oti.program_name \n"
			    + contextInfoStr_2
			    + "    ,OldestTranUser           = oti.login_name \n"
			    + "    ,OldestTranHost           = oti.host_name \n"
//			    + "--  ,OldestTranPage           = -1 \n"
//			    + "--  ,OldestTranProcName       = -1 \n"
			    + "    ,OldestTranHasSqlText     = CASE WHEN oti.most_recent_sql_text is not null THEN convert(bit,1) ELSE convert(bit,0) END \n"
			    + "    ,OldestTranHasLocks       = convert(bit,0) \n"
			    + "    ,OldestTranHasShowPlan    = CASE WHEN oti.plan_text            is not null THEN convert(bit,1) ELSE convert(bit,0) END \n"
			    + " \n"
			    + "    ,LastDbBackupTime          =         (SELECT bi.last_backup_finish_date                            FROM #backupInfo bi WHERE d.name = bi.database_name AND bi.backup_type = 'D') \n"
			    + "    ,LastDbBackupAgeInHours    = isnull( (SELECT datediff(hour, bi.last_backup_finish_date, getdate()) FROM #backupInfo bi WHERE d.name = bi.database_name AND bi.backup_type = 'D'), -1) \n"
			    + "    ,LastIncDbBackupTime       =         (SELECT bi.last_backup_finish_date                            FROM #backupInfo bi WHERE d.name = bi.database_name AND bi.backup_type = 'I') \n"
			    + "    ,LastIncDbBackupAgeInHours = isnull( (SELECT datediff(hour, bi.last_backup_finish_date, getdate()) FROM #backupInfo bi WHERE d.name = bi.database_name AND bi.backup_type = 'I'), -1) \n"
			    + "    ,LastLogBackupTime         =         (SELECT bi.last_backup_finish_date                            FROM #backupInfo bi WHERE d.name = bi.database_name AND bi.backup_type = 'L') \n"
			    + "    ,LastLogBackupAgeInHours   = isnull( (SELECT datediff(hour, bi.last_backup_finish_date, getdate()) FROM #backupInfo bi WHERE d.name = bi.database_name AND bi.backup_type = 'L'), -1) \n"
			    + lastGoodCheckDbTime
			    + lastGoodCheckDbDays
			    + " \n"
			    + queryStoreColumns
			    + "    ,OldestTranSqlText        = oti.most_recent_sql_text \n"
			    + "    ,OldestTranLocks          = convert(varchar(max), null) \n" // NOTE: This will be deferred and fetched at: localCalculation()
			    + "    ,OldestTranShowPlanText   = oti.plan_text \n"
			    + "FROM " + databases + " d \n"
			    + "LEFT OUTER JOIN #oti            oti ON d.database_id = oti    .database_id and oti.row_num = 1 \n"
			    + "LEFT OUTER JOIN #dataSizeMb    data ON d.database_id = data   .database_id \n"
			    + "LEFT OUTER JOIN #logSizeMb      log ON d.database_id = log    .database_id \n"
			    + osvDataJoin
			    + osvLogJoin
			    + queryStoreJoin
			    + "WHERE has_dbaccess(d.name) != 0 \n"
			    + whereAvailabilityGroup
			    + "go \n"
			    + " \n"
			    + "-- Drop temporary tables \n" 
			    + "drop table #dataSizeMb \n" 
			    + "drop table #logSizeMb  \n"
			    + "drop table #backupInfo  \n"
			    + "drop table #oti  \n"
			    + osvDataDropTempTable2
			    + osvLogDropTempTable2
			    + queryStoreDropTempTable2
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
		
		int LogOsDiskLabel_pos      = this.findColumn("LogOsDiskLabel");
		int LogOsDisk_pos           = this.findColumn("LogOsDisk");
		int LogOsFileName_pos       = this.findColumn("LogOsFileName");
		int LogColName_pos          = this.findColumn(logColName);
		
		int DataOsDiskLabel_pos     = this.findColumn("DataOsDiskLabel");
		int DataOsDisk_pos          = this.findColumn("DataOsDisk");
		int DataOsFileName_pos      = this.findColumn("DataOsFileName");
		int DataColName_pos         = this.findColumn(dataColName);

		// Error checking if columns exists
//		if (LogOsDisk_pos  == -1 || LogOsFileName_pos  == -1 || LogColName_pos  == -1) { _logger.error(tgdp.getName() + ": Cant find some columns in dataset. LogOsDisk="  + LogOsDisk_pos  + ", LogOsFileName="  + LogOsFileName_pos  + ", " + logColName  + "=" + LogColName_pos ); return; }
//		if (DataOsDisk_pos == -1 || DataOsFileName_pos == -1 || DataColName_pos == -1) { _logger.error(tgdp.getName() + ": Cant find some columns in dataset. DataOsDisk=" + DataOsDisk_pos + ", DataOsFileName=" + DataOsFileName_pos + ", " + dataColName + "=" + DataColName_pos); return; }
		if (LogOsDisk_pos  == -1 || LogOsFileName_pos  == -1 || LogColName_pos  == -1) { return; } // Azure do NOT have OS Data/Log information
		if (DataOsDisk_pos == -1 || DataOsFileName_pos == -1 || DataColName_pos == -1) { return; } // Azure do NOT have OS Data/Log information

		// Loop all rows and store values (duplicates will be discarded) in a MAP
		for (int r=0; r<this.size(); r++)
		{
			String LogOsDiskLabel      = ((LogOsDiskLabel_pos == -1) ? "" : this.getAbsString(r, LogOsDiskLabel_pos));
			String LogOsDisk           = this.getAbsString(r, LogOsDisk_pos);
			String LogOsFileName       = this.getAbsString(r, LogOsFileName_pos);
			Double LogColValue         = this.getAbsValueAsDouble(r, LogColName_pos);
			
			String DataOsDiskLabel     = ((DataOsDiskLabel_pos == -1) ? "" : this.getAbsString(r, DataOsDiskLabel_pos));
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
			
			if (StringUtil.isNullOrBlank(logOsName))  logOsName  = CounterControllerSqlServer.resolvFileNameToDirectory(LogOsFileName);
			if (StringUtil.isNullOrBlank(dataOsName)) dataOsName = CounterControllerSqlServer.resolvFileNameToDirectory(DataOsFileName);
			
			if (StringUtil.hasValue(LogOsDiskLabel))  logOsName  += " [" + LogOsDiskLabel  + "]";
			if (StringUtil.hasValue(DataOsDiskLabel)) dataOsName += " [" + DataOsDiskLabel + "]";

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
		
		// LOCKS
		if ("OldestTranHasLocks".equals(colName))
		{
			// Find 'OldestTranLocks' column, is so get it and set it as the tool tip
			int pos = findColumn("OldestTranLocks");
			if (pos > 0)
			{
				Object cellVal = getValueAt(modelRow, pos);
				if (cellVal instanceof String)
					//return (String) cellVal;
					return "<html><pre>" + cellVal + "</pre></html>";
			}
		}
		if ("OldestTranLocks".equals(colName))
		{
			return cellValue == null ? null : "<html><pre>" + cellValue + "</pre></html>";
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
	public void localCalculation(CounterSample newSample)
	{
		// make: column 'program_name' with value "SQLAgent - TSQL JobStep (Job 0x38AAD6888E5C5E408DE573B0A25EE970 : Step 1)"
		// into:                                  "SQLAgent - TSQL JobStep (Job '<name-of-the-job>' : Step 1 '<name-of-the-step>')
		SqlServerCmUtils.localCalculation_resolveSqlAgentProgramName(newSample);


		boolean getTempdbSpidUsage = Configuration.getCombinedConfiguration().getBooleanProperty(CmSummary.PROPKEY_sample_tempdbSpidUsage, CmSummary.DEFAULT_sample_tempdbSpidUsage);

		int pos_OldestTranSpid         = -1; 
		int pos_OldestTranHasLocks     = -1; 
		int pos_OldestTranLocks        = -1;
		
		// Find column Id's
		List<String> colNames = newSample.getColNames();
		if (colNames == null)
			return;

		for (int colId=0; colId < colNames.size(); colId++) 
		{
			String colName = colNames.get(colId);

			if      (colName.equals("OldestTranSpid"))         pos_OldestTranSpid         = colId;
			else if (colName.equals("OldestTranHasLocks"))     pos_OldestTranHasLocks     = colId;
			else if (colName.equals("OldestTranLocks"))        pos_OldestTranLocks        = colId;
		}

		// Loop on all rows
		for (int rowId = 0; rowId < newSample.getRowCount(); rowId++)
		{
//			OldestTranLocks
			Object oval_OldestTranSpid = newSample.getValueAt(rowId, pos_OldestTranSpid);
			if (oval_OldestTranSpid != null && oval_OldestTranSpid instanceof Number)
			{
				Configuration conf = Configuration.getCombinedConfiguration();
//				boolean getShowplan   = conf.getBooleanProperty(PROPKEY_sample_showplan,   DEFAULT_sample_showplan);
//				boolean getMonSqltext = conf.getBooleanProperty(PROPKEY_sample_monSqlText, DEFAULT_sample_monSqlText);
				boolean getLocks      = conf.getBooleanProperty(PROPKEY_sample_locks,   DEFAULT_sample_locks);

				int OldestTranSpid = ((Number)newSample.getValueAt(rowId, pos_OldestTranSpid)).intValue();

				if (OldestTranSpid > 0) // NULL result from the DBMS is translated as 0... so lets not hope that the SPID 0 has issues.
				{
					//---------------------------------------------
					// Get locks
					//---------------------------------------------
					String sysLocks = "This was disabled";
					if (getLocks)
					{
						try
						{
							sysLocks  = SqlServerUtils.getLockSummaryForSpid(getCounterController().getMonConnection(), OldestTranSpid, true, false);
							if (sysLocks == null)
								sysLocks = NO_LOCKS_WAS_FOUND; // "No locks was found"; //sysLocks = "Not Available";
						}
						catch (TimeoutException ex)
						{
							sysLocks = "Timeout - when getting lock information";
						}						
					}

					// Set the values: *Has* and *Text*
					boolean b = true;

					b = !"This was disabled".equals(sysLocks) && !NO_LOCKS_WAS_FOUND.equals(sysLocks) && !"Timeout - when getting lock information".equals(sysLocks);
					newSample.setValueAt(new Boolean(b), rowId, pos_OldestTranHasLocks);
					newSample.setValueAt(sysLocks,       rowId, pos_OldestTranLocks);

					//---------------------------------------------
					// Get tempdb space usage
					//---------------------------------------------
					if (getTempdbSpidUsage)
					{
						// 1: Get tempdb info about 'oldestOpenTranSpid', and if we have a value:
						//    2: Set values for columns
						//       - OldestTranTempdbUsageMbAll
						//       - OldestTranTempdbUsageMbUser
						//       - OldestTranTempdbUsageMbInternal
						//       (if the above columns can't be found... Simply write a message to the error log)
						TempdbUsagePerSpid.TempDbSpaceInfo spaceInfo = TempdbUsagePerSpid.getInstance().getEntryForSpid(OldestTranSpid);
						if (spaceInfo != null)
						{
							newSample.setValueAt(spaceInfo.getTotalSpaceUsedInMb()         , rowId, "OldestTranTempdbUsageMbAll");
							newSample.setValueAt(spaceInfo.getUserObjectSpaceUsedInMb()    , rowId, "OldestTranTempdbUsageMbUser");
							newSample.setValueAt(spaceInfo.getInternalObjectSpaceUsedInMb(), rowId, "OldestTranTempdbUsageMbInternal");
						}
					} // end: getTempdbSpidUsage
				} // end: OldestTranSpid > 0
			} // end: HAS: oval_OldestTranSpid
		} // end: Loop on all rows
	}
	
	private static final String NO_LOCKS_WAS_FOUND = "No locks was found";
	
	
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

		
		
		// Lists used by Availability Group checks
		List<String> agMandaroryDbnamesList           = Collections.emptyList();
		List<String> agMandaroryDbnamesSkipList       = Collections.emptyList();
		List<String> agMandaroryDbnamesSkipSystemList = Collections.emptyList();

		int dbCountWithAg = 0; // Count number of databases that IS PART of a Availability Group
		if (hasColumn("ag_name"))
		{
			for (int r=0; r<cm.getDiffRowCount(); r++)
			{
				String ag_name = cm.getAbsString(r, "ag_name");
				if (StringUtil.hasValue(ag_name))
					dbCountWithAg++;
			}

			// Get list of Availability Group -- database names which might be "Mandatory" or part of the "SkipList"
			if (dbCountWithAg > 0)
			{
				agMandaroryDbnamesList           = StringUtil.parseCommaStrToList(Configuration.getCombinedConfiguration().getProperty(PROPKEY_alarm_AgMandatoryDbnames          , DEFAULT_alarm_AgMandatoryDbnames          ), true);
				agMandaroryDbnamesSkipList       = StringUtil.parseCommaStrToList(Configuration.getCombinedConfiguration().getProperty(PROPKEY_alarm_AgMandatoryDbnamesSkip      , DEFAULT_alarm_AgMandatoryDbnamesSkip      ), true);
				agMandaroryDbnamesSkipSystemList = StringUtil.parseCommaStrToList(Configuration.getCombinedConfiguration().getProperty(PROPKEY_alarm_AgMandatoryDbnamesSkipSystem, DEFAULT_alarm_AgMandatoryDbnamesSkipSystem), true);
			}
		}
		
		
		
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
					// Check for "No locks was found" 
					String OldestTranLocks = cm.getAbsString(r, "OldestTranLocks");
					if (NO_LOCKS_WAS_FOUND.equals(OldestTranLocks))
					{
						// Reset OldestTranInSeconds to -1
						if (debugPrint || _logger.isDebugEnabled())
							System.out.println("##### sendAlarmRequest("+cm.getName()+"): dbname='"+dbname+"', OldestTranInSeconds='"+OldestTranInSeconds+"'. BUT OldestTranLocks='" + NO_LOCKS_WAS_FOUND + "' -- Setting: OldestTranInSeconds to: -1");
						OldestTranInSeconds = new Double(-1);
					}

					int threshold = Configuration.getCombinedConfiguration().getIntProperty(PROPKEY_alarm_OldestTranInSeconds, DEFAULT_alarm_OldestTranInSeconds);

					if (debugPrint || _logger.isDebugEnabled())
						System.out.println("##### sendAlarmRequest("+cm.getName()+"): threshold="+threshold+", dbname='"+dbname+"', OldestTranInSeconds='"+OldestTranInSeconds+"'.");

					if (OldestTranInSeconds.intValue() > threshold)
					{
						// Get OldestTranName
						String OldestTranName = cm.getAbsString(r, "OldestTranName");
						String OldestTranProg = cm.getAbsString(r, "OldestTranProg");
						String OldestTranUser = cm.getAbsString(r, "OldestTranUser");
						String OldestTranHost = cm.getAbsString(r, "OldestTranHost");
						
						// Get config 'skip some transaction names'
						String skipTranNameRegExp = Configuration.getCombinedConfiguration().getProperty(PROPKEY_alarm_OldestTranInSecondsSkipTranName, DEFAULT_alarm_OldestTranInSecondsSkipTranName);
						String skipTranProgRegExp = Configuration.getCombinedConfiguration().getProperty(PROPKEY_alarm_OldestTranInSecondsSkipTranProg, DEFAULT_alarm_OldestTranInSecondsSkipTranProg);
						String skipTranUserRegExp = Configuration.getCombinedConfiguration().getProperty(PROPKEY_alarm_OldestTranInSecondsSkipTranUser, DEFAULT_alarm_OldestTranInSecondsSkipTranUser);
						String skipTranHostRegExp = Configuration.getCombinedConfiguration().getProperty(PROPKEY_alarm_OldestTranInSecondsSkipTranHost, DEFAULT_alarm_OldestTranInSecondsSkipTranHost);

						// note: this must be set to true at start, otherwise all below rules will be disabled (it "stops" processing at first doAlarm==false)
						boolean doAlarm = true;

						// The below could have been done with nested if(skip-name), if(skip-prog), if(skip-user), if(skip-host) doAlarm=true; 
						// Below is more readable, from a variable context point-of-view, but HARDER to understand
						// to *continue*: doAlarm needs to be true AND (regExp is empty OR not-matching)
						doAlarm = (doAlarm && (StringUtil.isNullOrBlank(skipTranNameRegExp) || ! OldestTranName.matches(skipTranNameRegExp)));
						doAlarm = (doAlarm && (StringUtil.isNullOrBlank(skipTranProgRegExp) || ! OldestTranProg.matches(skipTranProgRegExp)));
						doAlarm = (doAlarm && (StringUtil.isNullOrBlank(skipTranUserRegExp) || ! OldestTranUser.matches(skipTranUserRegExp)));
						doAlarm = (doAlarm && (StringUtil.isNullOrBlank(skipTranHostRegExp) || ! OldestTranHost.matches(skipTranHostRegExp)));

						if (doAlarm)
						{
							String extendedDescText = cm.toTextTableString(DATA_RATE, r);
							String extendedDescHtml = cm.toHtmlTableString(DATA_RATE, r, true, false, false);
							
							AlarmEvent ae = new AlarmEventLongRunningTransaction(cm, threshold, dbname, OldestTranInSeconds, OldestTranName);
							ae.setExtendedDescription(extendedDescText, extendedDescHtml);
							
							alarmHandler.addAlarm( ae );
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

				if (debugPrint || _logger.isDebugEnabled())
					System.out.println("##### sendAlarmRequest("+cm.getName()+"): LastDbBackupAgeInHours -- dbname='"+dbname+"', threshold="+threshold+", val='"+val+"'.");

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
					
					// The below could have been done with nested if(keep-db), if(keep-srv), if(!skipDb), if(!skipSrv) doAlarm=true; 
					// Below is more readable, from a variable context point-of-view, but HARDER to understand
					doAlarm = (doAlarm && (StringUtil.isNullOrBlank(keepDbRegExp)  ||   dbname     .matches(keepDbRegExp ))); //     matches the KEEP Db  regexp
					doAlarm = (doAlarm && (StringUtil.isNullOrBlank(keepSrvRegExp) ||   dbmsSrvName.matches(keepSrvRegExp))); //     matches the KEEP Srv regexp
					doAlarm = (doAlarm && (StringUtil.isNullOrBlank(skipDbRegExp)  || ! dbname     .matches(skipDbRegExp ))); // NO match in the SKIP Db  regexp
					doAlarm = (doAlarm && (StringUtil.isNullOrBlank(skipSrvRegExp) || ! dbmsSrvName.matches(skipSrvRegExp))); // NO match in the SKIP Srv regexp

					// NO match in the SKIP regexp
					if (doAlarm)
					{
						String lastBackupDate = cm.getAbsValue(r, "LastDbBackupTime") + "";
						
						String extendedDescText = cm.toTextTableString(DATA_RATE, r);
						String extendedDescHtml = cm.toHtmlTableString(DATA_RATE, r, true, false, false);
						
						AlarmEvent ae = new AlarmEventOldBackup(cm, threshold, dbname, lastBackupDate, val.intValue());
						ae.setExtendedDescription(extendedDescText, extendedDescHtml);
						
						alarmHandler.addAlarm( ae );
					}
				}
			}

			//-------------------------------------------------------
			// LastDbBackupAgeInHours
			//-------------------------------------------------------
			if (isSystemAlarmsForColumnEnabledAndInTimeRange("LastIncDbBackupAgeInHours"))
			{
				Double val = cm.getAbsValueAsDouble(r, "LastIncDbBackupAgeInHours");
				if (val == null)
					val = -1.0;
				
				int threshold = Configuration.getCombinedConfiguration().getIntProperty(PROPKEY_alarm_LastIncDbBackupAgeInHours, DEFAULT_alarm_LastIncDbBackupAgeInHours);

				if (debugPrint || _logger.isDebugEnabled())
					System.out.println("##### sendAlarmRequest("+cm.getName()+"): LastIncDbBackupAgeInHours -- dbname='"+dbname+"', threshold="+threshold+", val='"+val+"'.");

				if (val.intValue() > threshold || val.intValue() < 0)
				{
					// Get config 'skip some transaction names'
					String keepDbRegExp  = Configuration.getCombinedConfiguration().getProperty(PROPKEY_alarm_LastIncDbBackupAgeInHoursForDbs,  DEFAULT_alarm_LastIncDbBackupAgeInHoursForDbs);
					String skipDbRegExp  = Configuration.getCombinedConfiguration().getProperty(PROPKEY_alarm_LastIncDbBackupAgeInHoursSkipDbs, DEFAULT_alarm_LastIncDbBackupAgeInHoursSkipDbs);
					String keepSrvRegExp = Configuration.getCombinedConfiguration().getProperty(PROPKEY_alarm_LastIncDbBackupAgeInHoursForSrv,  DEFAULT_alarm_LastIncDbBackupAgeInHoursForSrv);
					String skipSrvRegExp = Configuration.getCombinedConfiguration().getProperty(PROPKEY_alarm_LastIncDbBackupAgeInHoursSkipSrv, DEFAULT_alarm_LastIncDbBackupAgeInHoursSkipSrv);

					// note: this must be set to true at start, otherwise all below rules will be disabled (it "stops" processing at first doAlarm==false)
					boolean doAlarm = true;

					// if -1 (LastTranLogDumpTime=NULL), only alarm if we have anything in the keep* or skip* rules
					if (val.intValue() < 0 && StringUtil.isNullOrBlankForAll(keepDbRegExp, skipDbRegExp, keepSrvRegExp, skipSrvRegExp))
						doAlarm = false;
					
					// The below could have been done with nested if(keep-db), if(keep-srv), if(!skipDb), if(!skipSrv) doAlarm=true; 
					// Below is more readable, from a variable context point-of-view, but HARDER to understand
					doAlarm = (doAlarm && (StringUtil.isNullOrBlank(keepDbRegExp)  ||   dbname     .matches(keepDbRegExp ))); //     matches the KEEP Db  regexp
					doAlarm = (doAlarm && (StringUtil.isNullOrBlank(keepSrvRegExp) ||   dbmsSrvName.matches(keepSrvRegExp))); //     matches the KEEP Srv regexp
					doAlarm = (doAlarm && (StringUtil.isNullOrBlank(skipDbRegExp)  || ! dbname     .matches(skipDbRegExp ))); // NO match in the SKIP Db  regexp
					doAlarm = (doAlarm && (StringUtil.isNullOrBlank(skipSrvRegExp) || ! dbmsSrvName.matches(skipSrvRegExp))); // NO match in the SKIP Srv regexp

					// NO match in the SKIP regexp
					if (doAlarm)
					{
						String lastBackupDate = cm.getAbsValue(r, "LastIncDbBackupTime") + "";
						
						String extendedDescText = cm.toTextTableString(DATA_RATE, r);
						String extendedDescHtml = cm.toHtmlTableString(DATA_RATE, r, true, false, false);
						
						AlarmEvent ae = new AlarmEventOldIncrementalBackup(cm, threshold, dbname, lastBackupDate, val.intValue());
						ae.setExtendedDescription(extendedDescText, extendedDescHtml);
						
						alarmHandler.addAlarm( ae );
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

				if (debugPrint || _logger.isDebugEnabled())
					System.out.println("##### sendAlarmRequest("+cm.getName()+"): LastLogBackupAgeInHours -- dbname='"+dbname+"', threshold="+threshold+", val='"+val+"'.");

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
						String lastBackupDate = cm.getAbsValue(r, "LastLogBackupTime") + "";
						
						String extendedDescText = cm.toTextTableString(DATA_RATE, r);
						String extendedDescHtml = cm.toHtmlTableString(DATA_RATE, r, true, false, false);
						
						AlarmEvent ae = new AlarmEventOldTranLogBackup(cm, threshold, dbname, lastBackupDate, val.intValue());
						ae.setExtendedDescription(extendedDescText, extendedDescHtml);
						
						alarmHandler.addAlarm( ae );
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

				if (debugPrint || _logger.isDebugEnabled())
					System.out.println("##### sendAlarmRequest("+cm.getName()+"): LowDbFreeSpaceInMb -- dbname='"+dbname+"', threshold="+threshold+", freeMb='"+freeMb+"', usedPct='"+usedPct+"'.");

				if (freeMb != null && usedPct != null && threshold != null)
				{
					if (freeMb.intValue() < threshold.intValue())
					{
						String extendedDescText = cm.toTextTableString(DATA_RATE, r);
						String extendedDescHtml = cm.toHtmlTableString(DATA_RATE, r, true, false, false);

						extendedDescHtml += "<br><br>" + cm.getGraphDataHistoryAsHtmlImage(GRAPH_NAME_DATASIZE_LEFT_MB , dbname);
						extendedDescHtml += "<br><br>" + cm.getGraphDataHistoryAsHtmlImage(GRAPH_NAME_DATASIZE_USED_PCT, dbname);
						
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

				if (debugPrint || _logger.isDebugEnabled())
					System.out.println("##### sendAlarmRequest("+cm.getName()+"): LowLogFreeSpaceInMb -- dbname='"+dbname+"', threshold="+threshold+", freeMb='"+freeMb+"', usedPct='"+usedPct+"'.");

				if (freeMb != null && usedPct != null && threshold != null)
				{
					if (freeMb.intValue() < threshold.intValue())
					{
						String extendedDescText = cm.toTextTableString(DATA_RATE, r);
						String extendedDescHtml = cm.toHtmlTableString(DATA_RATE, r, true, false, false);

						extendedDescHtml += "<br><br>" + cm.getGraphDataHistoryAsHtmlImage(GRAPH_NAME_LOGSIZE_LEFT_MB , dbname);
						extendedDescHtml += "<br><br>" + cm.getGraphDataHistoryAsHtmlImage(GRAPH_NAME_LOGSIZE_USED_PCT, dbname);
						
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

				if (debugPrint || _logger.isDebugEnabled())
					System.out.println("##### sendAlarmRequest("+cm.getName()+"): LowDbFreeSpaceInPct -- dbname='"+dbname+"', threshold="+threshold+", freeMb='"+freeMb+"', usedPct='"+usedPct+"'.");

				if (freeMb != null && usedPct != null && threshold != null)
				{
					if (usedPct > threshold.doubleValue())
					{
						String extendedDescText = cm.toTextTableString(DATA_RATE, r);
						String extendedDescHtml = cm.toHtmlTableString(DATA_RATE, r, true, false, false);

						extendedDescHtml += "<br><br>" + cm.getGraphDataHistoryAsHtmlImage(GRAPH_NAME_DATASIZE_LEFT_MB , dbname);
						extendedDescHtml += "<br><br>" + cm.getGraphDataHistoryAsHtmlImage(GRAPH_NAME_DATASIZE_USED_PCT, dbname);
						
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

				if (debugPrint || _logger.isDebugEnabled())
					System.out.println("##### sendAlarmRequest("+cm.getName()+"): LowLogFreeSpaceInPct -- dbname='"+dbname+"', threshold="+threshold+", freeMb='"+freeMb+"', usedPct='"+usedPct+"'.");

				if (freeMb != null && usedPct != null && threshold != null)
				{
					if (usedPct > threshold.doubleValue())
					{
						String extendedDescText = cm.toTextTableString(DATA_RATE, r);
						String extendedDescHtml = cm.toHtmlTableString(DATA_RATE, r, true, false, false);

						extendedDescHtml += "<br><br>" + cm.getGraphDataHistoryAsHtmlImage(GRAPH_NAME_LOGSIZE_LEFT_MB , dbname);
						extendedDescHtml += "<br><br>" + cm.getGraphDataHistoryAsHtmlImage(GRAPH_NAME_LOGSIZE_USED_PCT, dbname);
						
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

//FIXME; Double is OBJECT and not a 'double' ... so we need to do xxx.equal(yyy) 
//And this issue id probably in ALOT of places...

				if (dataFreeMb  != null && logFreeMb  != null) freeMb  = Math.min(dataFreeMb,  logFreeMb);
				if (dataUsedPct != null && logUsedPct != null) usedPct = Math.max(dataUsedPct, logUsedPct);
				
				Number threshold = getDbFreeSpaceThreshold(dbname, _map_alarm_LowOsDiskFreeSpaceInMb); // This uses dbname.matches(map:anyKey)

				if (debugPrint || _logger.isDebugEnabled())
					System.out.println("##### sendAlarmRequest("+cm.getName()+"): LowOsDiskFreeSpaceInMb -- dbname='"+dbname+"', threshold="+threshold+", freeMb='"+freeMb+"', usedPct='"+usedPct+"'.");

				if (freeMb != -99d && usedPct != -99d && threshold != null)
				{
					if (freeMb.intValue() < threshold.intValue()) // FREE-MB CHECK
					{
						String osVolume   = "";
						String osFileName = "";

						if (freeMb.equals(logFreeMb))
						{
							String lbl  = cm.getAbsString(r, "LogOsDiskLabel");
							osVolume    = cm.getAbsString(r, "LogOsDisk");
							osFileName  = cm.getAbsString(r, "LogOsFileName");

							if (StringUtil.hasValue(osVolume) && StringUtil.hasValue(lbl))
								osVolume += " [" + lbl + "]";
						}

						if (freeMb.equals(dataFreeMb))
						{
							String lbl  = cm.getAbsString(r, "DataOsDiskLabel");
							osVolume    = cm.getAbsString(r, "DataOsDisk");
							osFileName  = cm.getAbsString(r, "DataOsFileName");

							if (StringUtil.hasValue(osVolume) && StringUtil.hasValue(lbl))
								osVolume += " [" + lbl + "]";
						}
						
						// If 'osVolume' is empty... try to get the File directory...
						String mountPoint = osVolume;
						if (StringUtil.isNullOrBlank(mountPoint))
						{
							mountPoint = "-unknown-";
							if (StringUtil.hasValue(osFileName))
								mountPoint = CounterControllerSqlServer.resolvFileNameToDirectory(osFileName);
						}
//_logger.info("---DEBUG---: LowOsDiskFreeSpaceInMb --- "
//		+ "freeMb="      + freeMb      + ", usedPct="    + usedPct    + ", "
//		+ "dataFreeMb="  + dataFreeMb  + ", logFreeMb="  + logFreeMb  + ", "
//		+ "dataUsedPct=" + dataUsedPct + ", logUsedPct=" + logUsedPct + ", "
//		+ "osVolume="    + osVolume    + ", osFileName=" + osFileName + ", "
//		+ "mountPoint="  + mountPoint  + ", threshold="  + threshold  + ".");
						
						String extendedDescText = cm.toTextTableString(DATA_RATE, r);
						String extendedDescHtml = cm.toHtmlTableString(DATA_RATE, r, true, false, false);

						extendedDescHtml += "<br><br>" + cm.getGraphDataHistoryAsHtmlImage(GRAPH_NAME_OS_DISK_FREE_MB);
						extendedDescHtml += "<br><br>" + cm.getGraphDataHistoryAsHtmlImage(GRAPH_NAME_OS_DISK_USED_PCT);
						
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
	
				if (debugPrint || _logger.isDebugEnabled())
					System.out.println("##### sendAlarmRequest("+cm.getName()+"): LowOsDiskFreeSpaceInPct -- dbname='"+dbname+"', threshold="+threshold+", freeMb='"+freeMb+"', usedPct='"+usedPct+"'.");

				if (freeMb != -99d && usedPct != -99d && threshold != null)
				{
					if (usedPct > threshold.doubleValue()) // Percent CHECK
					{
						String osVolume   = "";
						String osFileName = "";
						if (usedPct.equals(logUsedPct))
						{
							String lbl  = cm.getAbsString(r, "LogOsDiskLabel");
							osVolume    = cm.getAbsString(r, "LogOsDisk");
							osFileName  = cm.getAbsString(r, "LogOsFileName");

							if (StringUtil.hasValue(osVolume) && StringUtil.hasValue(lbl))
								osVolume += " [" + lbl + "]";
						}
						if (usedPct.equals(dataUsedPct))
						{
							String lbl  = cm.getAbsString(r, "DataOsDiskLabel");
							osVolume    = cm.getAbsString(r, "DataOsDisk");
							osFileName  = cm.getAbsString(r, "DataOsFileName");

							if (StringUtil.hasValue(osVolume) && StringUtil.hasValue(lbl))
								osVolume += " [" + lbl + "]";
						}

						// If 'osVolume' is empty... try to get the File directory...
						String mountPoint = osVolume;
						if (StringUtil.isNullOrBlank(mountPoint))
						{
							mountPoint = "-unknown-";
							if (StringUtil.hasValue(osFileName))
								mountPoint = CounterControllerSqlServer.resolvFileNameToDirectory(osFileName);
						}
						
//_logger.info("---DEBUG---: LowOsDiskFreeSpaceInPct --- "
//		+ "freeMb="      + freeMb      + ", usedPct="    + usedPct    + ", "
//		+ "dataFreeMb="  + dataFreeMb  + ", logFreeMb="  + logFreeMb  + ", "
//		+ "dataUsedPct=" + dataUsedPct + ", logUsedPct=" + logUsedPct + ", "
//		+ "osVolume="    + osVolume    + ", osFileName=" + osFileName + ", "
//		+ "mountPoint="  + mountPoint  + ", threshold="  + threshold  + ".");
												
						String extendedDescText = cm.toTextTableString(DATA_RATE, r);
						String extendedDescHtml = cm.toHtmlTableString(DATA_RATE, r, true, false, false);

						extendedDescHtml += "<br><br>" + cm.getGraphDataHistoryAsHtmlImage(GRAPH_NAME_OS_DISK_FREE_MB);
						extendedDescHtml += "<br><br>" + cm.getGraphDataHistoryAsHtmlImage(GRAPH_NAME_OS_DISK_USED_PCT);
						
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

			//-------------------------------------------------------
			// QsIsOk
			//-------------------------------------------------------
//TestThis;
			if (isSystemAlarmsForColumnEnabledAndInTimeRange("QsIsOk") && isQueryStoreEnabledForDatabaseAtRow(r))
			{
				String QsIsOk = cm.getAbsString(r, "QsIsOk");
				
				if (QsIsOk != null)
				{
					String expectedStr = Configuration.getCombinedConfiguration().getProperty(PROPKEY_alarm_QsIsOk, DEFAULT_alarm_QsIsOk);

					if (debugPrint || _logger.isDebugEnabled())
						System.out.println("##### sendAlarmRequest("+cm.getName()+"): QsUsedSpaceInPct -- dbname='"+dbname+"', expectedStr="+expectedStr+", QsIsOk='"+QsIsOk+"'.");

					if ( ! QsIsOk.equals(expectedStr) ) // NOT EQUAL
					{
						String qsDesiredState   = cm.getAbsString       (r, "QsDesiredState");
						String qsActualState    = cm.getAbsString       (r, "QsActualState");
						Double qsReadOnlyReason = cm.getAbsValueAsDouble(r, "QsReadOnlyReason");

						String extendedDescText = cm.toTextTableString(DATA_RATE, r);
						String extendedDescHtml = cm.toHtmlTableString(DATA_RATE, r, true, false, false);
						
						AlarmEvent ae = new AlarmEventQueryStoreUnexpectedState(cm, dbname, qsDesiredState, qsActualState, qsReadOnlyReason, expectedStr);
						ae.setExtendedDescription(extendedDescText, extendedDescHtml);
						
						alarmHandler.addAlarm( ae );
					}
				}
			}
			//-------------------------------------------------------
			// QsUsedSpaceInPct
			//-------------------------------------------------------
//TestThis;
			if (isSystemAlarmsForColumnEnabledAndInTimeRange("QsUsedSpaceInPct") && isQueryStoreEnabledForDatabaseAtRow(r))
			{
				Double QsUsedSpaceInPct = cm.getAbsValueAsDouble(r, "QsUsedSpaceInPct");

				if (QsUsedSpaceInPct != null)
				{
					Double threshold = Configuration.getCombinedConfiguration().getDoubleProperty(PROPKEY_alarm_QsUsedSpaceInPct, DEFAULT_alarm_QsUsedSpaceInPct);

					if (debugPrint || _logger.isDebugEnabled())
						System.out.println("##### sendAlarmRequest("+cm.getName()+"): QsUsedSpaceInPct -- dbname='"+dbname+"', threshold="+threshold+", QsUsedSpaceInPct='"+QsUsedSpaceInPct+"'.");

					Double QsMaxSizeInMb   = cm.getAbsValueAsDouble(r, "QsMaxSizeInMb");
					Double QsUsedSpaceInMb = cm.getAbsValueAsDouble(r, "QsUsedSpaceInMb");
					Double QsFreeSpaceInMb = cm.getAbsValueAsDouble(r, "QsFreeSpaceInMb");
					
					if (QsUsedSpaceInPct > threshold.doubleValue())
					{
						String extendedDescText = cm.toTextTableString(DATA_RATE, r);
						String extendedDescHtml = cm.toHtmlTableString(DATA_RATE, r, true, false, false);

						// For the moment there is NO chart for QueryStore Size
						//extendedDescHtml += "<br><br>" + cm.getGraphDataHistoryAsHtmlImage(GRAPH_NAME_XXX, dbname);
						
						AlarmEvent ae = new AlarmEventQueryStoreLowFreeSpace(cm, dbname, QsUsedSpaceInPct, QsMaxSizeInMb, QsUsedSpaceInMb, QsFreeSpaceInMb, AlarmEventQueryStoreLowFreeSpace.Type.PCT, threshold);
						ae.setExtendedDescription(extendedDescText, extendedDescHtml);

						alarmHandler.addAlarm( ae );
					}
				}
			}
			//-------------------------------------------------------
			// QsFreeSpaceInMb
			//-------------------------------------------------------
//TestThis;
			if (isSystemAlarmsForColumnEnabledAndInTimeRange("QsFreeSpaceInMb") && isQueryStoreEnabledForDatabaseAtRow(r))
			{
				Double QsFreeSpaceInMb = cm.getAbsValueAsDouble(r, "QsFreeSpaceInMb");

				if (QsFreeSpaceInMb != null)
				{
					Double threshold = Configuration.getCombinedConfiguration().getDoubleProperty(PROPKEY_alarm_QsFreeSpaceInMb, DEFAULT_alarm_QsFreeSpaceInMb);

					if (debugPrint || _logger.isDebugEnabled())
						System.out.println("##### sendAlarmRequest("+cm.getName()+"): QsUsedSpaceInPct -- dbname='"+dbname+"', threshold="+threshold+", QsFreeSpaceInMb='"+QsFreeSpaceInMb+"'.");

					Double QsMaxSizeInMb    = cm.getAbsValueAsDouble(r, "QsMaxSizeInMb");
					Double QsUsedSpaceInMb  = cm.getAbsValueAsDouble(r, "QsUsedSpaceInMb");
					Double QsUsedSpaceInPct = cm.getAbsValueAsDouble(r, "QsUsedPct");

					// Only check this if the MAX size is well above the threshold (otherwise if Query Store is SMALL then we will always get this alarm)  
					if (QsMaxSizeInMb > (threshold * 2))
					{
						if (QsFreeSpaceInMb < threshold.doubleValue())
						{
							String extendedDescText = cm.toTextTableString(DATA_RATE, r);
							String extendedDescHtml = cm.toHtmlTableString(DATA_RATE, r, true, false, false);
							
							// For the moment there is NO chart for QueryStore Size
							//extendedDescHtml += "<br><br>" + cm.getGraphDataHistoryAsHtmlImage(GRAPH_NAME_XXX, dbname);
							
							AlarmEvent ae = new AlarmEventQueryStoreLowFreeSpace(cm, dbname, QsUsedSpaceInPct, QsMaxSizeInMb, QsUsedSpaceInMb, QsFreeSpaceInMb, AlarmEventQueryStoreLowFreeSpace.Type.MB, threshold);
							ae.setExtendedDescription(extendedDescText, extendedDescHtml);
							
							alarmHandler.addAlarm( ae );
						}
					}
				}
			}

			//-------------------------------------------------------
			// LastGoodCheckDbDays
			//-------------------------------------------------------
//TestThis;
			if (isSystemAlarmsForColumnEnabledAndInTimeRange("LastGoodCheckDbDays"))
			{
				// Skip tempdb, model
				if ( ! StringUtil.equalsAny(dbname, "tempdb", "model") )
				{
					// Only if we has the column... which was introduced in: 2016 SP2
					if (cm.findColumn("LastGoodCheckDbDays") != -1)
					{
						Double LastGoodCheckDbDays = cm.getAbsValueAsDouble(r, "LastGoodCheckDbDays");
						String LastGoodCheckDbTime = cm.getAbsValue(r, "LastGoodCheckDbTime") + "";

//						int threshold = Configuration.getCombinedConfiguration().getIntProperty(PROPKEY_alarm_LastGoodCheckDbDays, DEFAULT_alarm_LastGoodCheckDbDays);
						Number threshold = getDbFreeSpaceThreshold(dbname, _map_alarm_LastGoodCheckDbDays); // This uses dbname.matches(map:anyKey)

						if (debugPrint || _logger.isDebugEnabled())
							System.out.println("##### sendAlarmRequest("+cm.getName()+"): LastGoodCheckDbDays -- dbname='"+dbname+"', threshold="+threshold+", LastGoodCheckDbDays='"+LastGoodCheckDbDays+"'.");

						if (threshold != null && LastGoodCheckDbDays != null && LastGoodCheckDbDays.intValue() > threshold.intValue())
						{
							String extendedDescText = cm.toTextTableString(DATA_RATE, r);
							String extendedDescHtml = cm.toHtmlTableString(DATA_RATE, r, true, false, false);
							
							AlarmEvent ae = new AlarmEventDbccCheckdbAge(cm, dbname, LastGoodCheckDbDays.intValue(), LastGoodCheckDbTime, threshold.intValue());
							ae.setExtendedDescription(extendedDescText, extendedDescHtml);
							
							alarmHandler.addAlarm( ae );
						}
					}
				}
			} // end: LastGoodCheckDbDays
			
			//-------------------------------------------------------
			// AgMandatoryDbnames
			//-------------------------------------------------------
			if (isSystemAlarmsForColumnEnabledAndInTimeRange("AgMandatoryDbnames") && dbCountWithAg > 0)
			{
				String ag_name = cm.getAbsString(r, "ag_name");
				if (StringUtil.isNullOrBlank(ag_name))
				{
					if ( StringUtil.matchesAny(dbname, agMandaroryDbnamesList) )
					{
						String info = "part-of-mandatory-list=" + agMandaroryDbnamesList;
						AlarmEvent ae = new AlarmEventDbNotInHadr(cm, dbname, info);

						alarmHandler.addAlarm( ae );
					}
				}
			} // end: MandatoryDbnames

			//-------------------------------------------------------
			// AgMandatoryDbnamesSkip
			//-------------------------------------------------------
			if (isSystemAlarmsForColumnEnabledAndInTimeRange("AgMandatoryDbnamesSkip") && dbCountWithAg > 0)
			{
				// Skip System databases like: master, tempdb, model, msdb, SSISDB, ReportServer, ReportServerTempDB
				if ( ! StringUtil.matchesAny(dbname, agMandaroryDbnamesSkipSystemList) )
				{
					String ag_name = cm.getAbsString(r, "ag_name");
					if (StringUtil.isNullOrBlank(ag_name))
					{
						if ( StringUtil.matchesAny(dbname, agMandaroryDbnamesSkipList) )
						{
							_logger.debug("Database '" + dbname + "' does NOT belong to a Availability Group. But NO Alarm since it's part of the SKIP List: " + agMandaroryDbnamesSkipList);
						}
						else
						{
							String info = "not-part-of-skip-list=" + agMandaroryDbnamesSkipList;
							AlarmEvent ae = new AlarmEventDbNotInHadr(cm, dbname, info);

							alarmHandler.addAlarm( ae );
						}
					}
				}
			} // end: MandatoryDbnamesSkip


			//-------------------------------------------------------
			// DbGrow/LogGrow (when the database increases in size)
			//-------------------------------------------------------
			//-------------------------------------------------------
			// DbSizeInMbDiff
			//-------------------------------------------------------
			if (isSystemAlarmsForColumnEnabledAndInTimeRange("DbSizeInMbDiff"))
			{
				Double val = cm.getDiffValueAsDouble(r, "DbSizeInMbDiff");
				if (val == null)
					val = 0.0;
				
				int threshold = Configuration.getCombinedConfiguration().getIntProperty(PROPKEY_alarm_DbSizeInMbDiff, DEFAULT_alarm_DbSizeInMbDiff);

				if (debugPrint || _logger.isDebugEnabled())
					System.out.println("##### sendAlarmRequest("+cm.getName()+"): DbSizeInMbDiff -- dbname='"+dbname+"', threshold="+threshold+", val='"+val+"'.");

				// If database size is lower than earlier (negative), then turn it into a positive number
				if (val < 0)
					val = Math.abs(val);

				if (val.intValue() > threshold)
				{
					// Get config 'skip some transaction names'
					String keepDbRegExp  = Configuration.getCombinedConfiguration().getProperty(PROPKEY_alarm_DbSizeInMbDiffForDbs,  DEFAULT_alarm_DbSizeInMbDiffForDbs);
					String skipDbRegExp  = Configuration.getCombinedConfiguration().getProperty(PROPKEY_alarm_DbSizeInMbDiffSkipDbs, DEFAULT_alarm_DbSizeInMbDiffSkipDbs);
					String keepSrvRegExp = Configuration.getCombinedConfiguration().getProperty(PROPKEY_alarm_DbSizeInMbDiffForSrv,  DEFAULT_alarm_DbSizeInMbDiffForSrv);
					String skipSrvRegExp = Configuration.getCombinedConfiguration().getProperty(PROPKEY_alarm_DbSizeInMbDiffSkipSrv, DEFAULT_alarm_DbSizeInMbDiffSkipSrv);

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
					
					Double DbSizeInMbAbs  = cm.getAbsValueAsDouble (r, "DbSizeInMb");
					Double DbSizeInMbDiff = cm.getDiffValueAsDouble(r, "DbSizeInMbDiff");
//					boolean isNewDeltaOrRateRow = cm.isNewDeltaOrRateRow(r);
					
					// If DbSize is equal in ABS and DIFF values, it can be:
					//   - a new database
					//   - or that is has been "detach" copy the files and then "attached" (seems like some applications are using that as a "backup" strategy)
					// If the size is "big enough", lets NOT alarm on it
					// But if it's a small size, it's probably a new database (and it might be good to have a "notification" about that)
					if (DbSizeInMbAbs.equals(DbSizeInMbDiff))
					{
						if (DbSizeInMbAbs.intValue() > 512)
							doAlarm = false;
					}

					if (doAlarm)
					{
						Double DataSizeInMbDiff = cm.getDiffValueAsDouble(r, "DataSizeInMbDiff");
						Double LogSizeInMbDiff  = cm.getDiffValueAsDouble(r, "LogSizeInMbDiff");
						
						Double DataSizeInMbAbs = cm.getAbsValueAsDouble(r, "DataSizeInMb");
						Double LogSizeInMbAbs  = cm.getAbsValueAsDouble(r, "LogSizeInMb");
						
						String extendedDescText = cm.toTextTableString(DATA_DIFF, r);
						String extendedDescHtml = cm.toHtmlTableString(DATA_DIFF, r, true, false, false);

						extendedDescHtml += "<br><br>" + cm.getGraphDataHistoryAsHtmlImage(GRAPH_NAME_DB_SIZE_MB      , dbname);
						extendedDescHtml += "<br><br>" + cm.getGraphDataHistoryAsHtmlImage(GRAPH_NAME_DATASIZE_LEFT_MB, dbname);
						extendedDescHtml += "<br><br>" + cm.getGraphDataHistoryAsHtmlImage(GRAPH_NAME_LOGSIZE_LEFT_MB , dbname);
						
						AlarmEvent ae = new AlarmEventDbSizeChanged(cm, threshold, dbname, 
								DbSizeInMbDiff, DataSizeInMbDiff, LogSizeInMbDiff, 
								DbSizeInMbAbs , DataSizeInMbAbs , LogSizeInMbAbs);
						ae.setExtendedDescription(extendedDescText, extendedDescHtml);

						alarmHandler.addAlarm( ae );
					}
				}
			}
			
			
		} // end: loop dbname(s)
	}

	@Override
	public boolean isGraphDataHistoryEnabled(String name)
	{
		// ENABLED for the following graphs
		if (GRAPH_NAME_DB_SIZE_MB       .equals(name)) return true;
		if (GRAPH_NAME_LOGSIZE_LEFT_MB  .equals(name)) return true;
//		if (GRAPH_NAME_LOGSIZE_USED_MB  .equals(name)) return true;
		if (GRAPH_NAME_LOGSIZE_USED_PCT .equals(name)) return true;
		if (GRAPH_NAME_DATASIZE_LEFT_MB .equals(name)) return true;
//		if (GRAPH_NAME_DATASIZE_USED_MB .equals(name)) return true;
		if (GRAPH_NAME_DATASIZE_USED_PCT.equals(name)) return true;
//		if (GRAPH_NAME_TEMPDB_USED_MB   .equals(name)) return true;

		if (GRAPH_NAME_OS_DISK_FREE_MB  .equals(name)) return true;
//		if (GRAPH_NAME_OS_DISK_USED_MB  .equals(name)) return true;
		if (GRAPH_NAME_OS_DISK_USED_PCT .equals(name)) return true;

		// default: DISABLED
		return false;
	}
	@Override
	public int getGraphDataHistoryTimeInterval(String name)
	{
		// Keep interval: default is 60 minutes
		return super.getGraphDataHistoryTimeInterval(name);
	}

	
	/** Helper method to check if a Query Store is enabled */
	private boolean isQueryStoreEnabledForDatabaseAtRow(int row)
	{
		Object o_isEnabled = getAbsValue(row, "QsIsEnabled");
		if (o_isEnabled == null)
			return false;
		
		if (o_isEnabled instanceof Boolean)
		{
			return (Boolean) o_isEnabled;
		}
		return false;
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

//    	// Check all key in the match and check if they match the REGEXP in the key of the map
//    	for (String key : map.keySet())
//		{
//			if (dbname.matches(key))
//				return map.get(key);
//		}
    	
    	// Check all key in the match and check if they match the REGEXP in the key of the map
    	for (String key : map.keySet())
		{
			if (dbname.matches(key))
			{
				Number val= map.get(key);
				
				if (_logger.isDebugEnabled())
					_logger.debug("<<<--  <<-MATCH: getDbFreeSpaceThreshold() dbname='" + dbname + "', matches='" + key + "', returns: " + val);

				return val;
			}
			else
			{
				if (_logger.isDebugEnabled())
					_logger.debug("   --  NO-MATCH: getDbFreeSpaceThreshold() dbname='" + dbname + "', regex='" + key + "'.");
			}
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

	private Map<String, Number> _map_alarm_LastGoodCheckDbDays; // Note: do NOT initialize this here... since the initAlarms() is done in super, if initialized it will be overwritten here...

	private Map<String, Pattern> _map_alarm_DbState;


	/**
	 * Initialize stuff that has to do with alarms
	 */
	@Override
	public void initAlarms()
	{
		Configuration conf = Configuration.getCombinedConfiguration();
		String cfgVal;

		_map_alarm_LowDbFreeSpaceInMb   = new LinkedHashMap<>();
		_map_alarm_LowLogFreeSpaceInMb  = new LinkedHashMap<>();
		_map_alarm_LowDbFreeSpaceInPct  = new LinkedHashMap<>();
		_map_alarm_LowLogFreeSpaceInPct = new LinkedHashMap<>();

		_map_alarm_LowOsDiskFreeSpaceInMb  = new LinkedHashMap<>();
		_map_alarm_LowOsDiskFreeSpaceInPct = new LinkedHashMap<>();

		_map_alarm_LastGoodCheckDbDays     = new LinkedHashMap<>();

		_map_alarm_DbState = new LinkedHashMap<>();

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
			
			// Sort the MAP by value in descending order (high number first)
			_map_alarm_LowDbFreeSpaceInMb = CollectionUtils.sortByMapValueNumber(_map_alarm_LowDbFreeSpaceInMb, false);
			
			// Remove ".*" wild-card and add that to the *end*
			if (_map_alarm_LowDbFreeSpaceInMb.containsKey(".*"))
			{
				Number num = _map_alarm_LowDbFreeSpaceInMb.get(".*");
				_map_alarm_LowDbFreeSpaceInMb.put(".*", num);
			}

			_logger.info(prefix + "Evaluating alarm for 'LowDbFreeSpaceInMb' in the following order:");
			for (Entry<String, Number> entry : _map_alarm_LowDbFreeSpaceInMb.entrySet())
			{
				_logger.info(prefix + "    dbname='" + entry.getKey() + "', mb=" + entry.getValue());
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
			
			// Sort the MAP by value in descending order (high number first)
			_map_alarm_LowLogFreeSpaceInMb = CollectionUtils.sortByMapValueNumber(_map_alarm_LowLogFreeSpaceInMb, false);
			
			// Remove ".*" wild-card and add that to the *end*
			if (_map_alarm_LowLogFreeSpaceInMb.containsKey(".*"))
			{
				Number num = _map_alarm_LowLogFreeSpaceInMb.get(".*");
				_map_alarm_LowLogFreeSpaceInMb.put(".*", num);
			}

			_logger.info(prefix + "Evaluating alarm for 'LowLogFreeSpaceInMb' in the following order:");
			for (Entry<String, Number> entry : _map_alarm_LowLogFreeSpaceInMb.entrySet())
			{
				_logger.info(prefix + "    dbname='" + entry.getKey() + "', mb=" + entry.getValue());
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
			
			// Sort the MAP by value in ascending order (low number first)
			_map_alarm_LowDbFreeSpaceInPct = CollectionUtils.sortByMapValueNumber(_map_alarm_LowDbFreeSpaceInPct, true);
			
			// Remove ".*" wild-card and add that to the *end*
			if (_map_alarm_LowDbFreeSpaceInPct.containsKey(".*"))
			{
				Number num = _map_alarm_LowDbFreeSpaceInPct.get(".*");
				_map_alarm_LowDbFreeSpaceInPct.put(".*", num);
			}

			_logger.info(prefix + "Evaluating alarm for 'LowDbFreeSpaceInPct' in the following order:");
			for (Entry<String, Number> entry : _map_alarm_LowDbFreeSpaceInPct.entrySet())
			{
				_logger.info(prefix + "    dbname='" + entry.getKey() + "', mb=" + entry.getValue());
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
					_map_alarm_LowLogFreeSpaceInPct.put(key, pct);

					_logger.info(prefix + "Initializing alarm. Using 'LowLogFreeSpaceInPct', dbname='"+key+"', pct="+pct);
				}
				catch (NumberFormatException ex)
				{
					_logger.info(prefix + "Initializing alarm. Skipping 'LowLogFreeSpaceInPct' enty dbname='"+key+"', val='"+val+"'. The value is not a number.");
				}
			}

			// Sort the MAP by value in ascending order (low number first)
			_map_alarm_LowLogFreeSpaceInPct = CollectionUtils.sortByMapValueNumber(_map_alarm_LowLogFreeSpaceInPct, true);
			
			// Remove ".*" wild-card and add that to the *end*
			if (_map_alarm_LowLogFreeSpaceInPct.containsKey(".*"))
			{
				Number num = _map_alarm_LowLogFreeSpaceInPct.get(".*");
				_map_alarm_LowLogFreeSpaceInPct.put(".*", num);
			}

			_logger.info(prefix + "Evaluating alarm for 'LowLogFreeSpaceInPct' in the following order:");
			for (Entry<String, Number> entry : _map_alarm_LowLogFreeSpaceInPct.entrySet())
			{
				_logger.info(prefix + "    dbname='" + entry.getKey() + "', mb=" + entry.getValue());
			}		}

	
	
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

			// Sort the MAP by value in descending order (high number first)
			_map_alarm_LowOsDiskFreeSpaceInMb = CollectionUtils.sortByMapValueNumber(_map_alarm_LowOsDiskFreeSpaceInMb, false);
			
			// Remove ".*" wild-card and add that to the *end*
			if (_map_alarm_LowOsDiskFreeSpaceInMb.containsKey(".*"))
			{
				Number num = _map_alarm_LowOsDiskFreeSpaceInMb.get(".*");
				_map_alarm_LowOsDiskFreeSpaceInMb.put(".*", num);
			}

			_logger.info(prefix + "Evaluating alarm for 'LowOsDiskFreeSpaceInMb' in the following order:");
			for (Entry<String, Number> entry : _map_alarm_LowOsDiskFreeSpaceInMb.entrySet())
			{
				_logger.info(prefix + "    dbname='" + entry.getKey() + "', mb=" + entry.getValue());
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
			
			// Sort the MAP by value in ascending order (low number first)
			_map_alarm_LowOsDiskFreeSpaceInPct = CollectionUtils.sortByMapValueNumber(_map_alarm_LowOsDiskFreeSpaceInPct, true);
			
			// Remove ".*" wild-card and add that to the *end*
			if (_map_alarm_LowOsDiskFreeSpaceInPct.containsKey(".*"))
			{
				Number num = _map_alarm_LowOsDiskFreeSpaceInPct.get(".*");
				_map_alarm_LowOsDiskFreeSpaceInPct.put(".*", num);
			}

			_logger.info(prefix + "Evaluating alarm for 'LowOsDiskFreeSpaceInPct' in the following order:");
			for (Entry<String, Number> entry : _map_alarm_LowOsDiskFreeSpaceInPct.entrySet())
			{
				_logger.info(prefix + "    dbname='" + entry.getKey() + "', mb=" + entry.getValue());
			}
		}

		
		//--------------------------------------
		// LastGoodCheckDbDays
		cfgVal = conf.getProperty(PROPKEY_alarm_LastGoodCheckDbDays, DEFAULT_alarm_LastGoodCheckDbDays);
		if (StringUtil.hasValue(cfgVal))
		{
			Map<String, String> map = StringUtil.parseCommaStrToMap(cfgVal);
			if (_logger.isDebugEnabled())
				_logger.debug(prefix + "Initializing alarm 'LastGoodCheckDbDays'. After parseCommaStrToMap, map looks like: "+map);
			
			for (String key : map.keySet())
			{
				String val = map.get(key);
				
				try
				{
					int age = NumberUtils.createNumber(val).intValue();
					_map_alarm_LastGoodCheckDbDays.put(key, age);

					_logger.info(prefix + "Initializing alarm. Using 'LastGoodCheckDbDays', dbname='"+key+"', age="+age);
				}
				catch (NumberFormatException ex)
				{
					_logger.info(prefix + "Initializing alarm. Skipping 'LastGoodCheckDbDays' enty dbname='"+key+"', val='"+val+"'. The value is not a number.");
				}
			}
			
			// Sort the MAP by value in ascending order (low number first)
			_map_alarm_LastGoodCheckDbDays = CollectionUtils.sortByMapValueNumber(_map_alarm_LastGoodCheckDbDays, true);
			
			// Remove ".*" wild-card and add that to the *end*
			if (_map_alarm_LastGoodCheckDbDays.containsKey(".*"))
			{
				Number num = _map_alarm_LastGoodCheckDbDays.get(".*");
				_map_alarm_LastGoodCheckDbDays.put(".*", num);
			}

			_logger.info(prefix + "Evaluating alarm for 'LastGoodCheckDbDays' in the following order:");
			for (Entry<String, Number> entry : _map_alarm_LastGoodCheckDbDays.entrySet())
			{
				_logger.info(prefix + "    dbname='" + entry.getKey() + "', age=" + entry.getValue());
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

			// Remove ".*" wild-card and add that to the *end*
			if (_map_alarm_DbState.containsKey(".*"))
			{
				Pattern pat = _map_alarm_DbState.get(".*");
				_map_alarm_DbState.put(".*", pat);
			}

			_logger.info(prefix + "Evaluating alarm for 'DbState' in the following order:");
			for (Entry<String, Pattern> entry : _map_alarm_DbState.entrySet())
			{
				_logger.info(prefix + "    dbname='" + entry.getKey() + "', pattern=" + entry.getValue());
			}
		}
	}

	public static final String  PROPKEY_alarm_OldestTranInSeconds                = CM_NAME + ".alarm.system.if.OldestTranInSeconds.gt";
	public static final int     DEFAULT_alarm_OldestTranInSeconds                = 60;

	public static final String  PROPKEY_alarm_OldestTranInSecondsSkipTranName    = CM_NAME + ".alarm.system.if.OldestTranInSeconds.skip.tranName";
	public static final String  DEFAULT_alarm_OldestTranInSecondsSkipTranName    = "";

	public static final String  PROPKEY_alarm_OldestTranInSecondsSkipTranProg    = CM_NAME + ".alarm.system.if.OldestTranInSeconds.skip.tranProg";
	public static final String  DEFAULT_alarm_OldestTranInSecondsSkipTranProg    = "";

	public static final String  PROPKEY_alarm_OldestTranInSecondsSkipTranUser    = CM_NAME + ".alarm.system.if.OldestTranInSeconds.skip.tranUser";
	public static final String  DEFAULT_alarm_OldestTranInSecondsSkipTranUser    = "";

	public static final String  PROPKEY_alarm_OldestTranInSecondsSkipTranHost    = CM_NAME + ".alarm.system.if.OldestTranInSeconds.skip.tranHost";
	public static final String  DEFAULT_alarm_OldestTranInSecondsSkipTranHost    = "";

//	public static final String  PROPKEY_alarm_TransactionLogFull                 = CM_NAME + ".alarm.system.if.TransactionLogFull.gt";
//	public static final int     DEFAULT_alarm_TransactionLogFull                 = 0;

//	public static final String  PROPKEY_alarm_LastBackupFailed                   = CM_NAME + ".alarm.system.if.LastBackupFailed.gt";
//	public static final int     DEFAULT_alarm_LastBackupFailed                   = 0;

	public static final String  PROPKEY_alarm_LastDbBackupAgeInHours             = CM_NAME + ".alarm.system.if.LastDbBackupAgeInHours.gt";
	public static final int     DEFAULT_alarm_LastDbBackupAgeInHours             = 999_999; // 114 years; more or less disabled
	public static final String  PROPKEY_alarm_LastDbBackupAgeInHoursForDbs       = CM_NAME + ".alarm.system.if.LastDbBackupAgeInHours.for.dbs";
	public static final String  DEFAULT_alarm_LastDbBackupAgeInHoursForDbs       = "";
	public static final String  PROPKEY_alarm_LastDbBackupAgeInHoursSkipDbs      = CM_NAME + ".alarm.system.if.LastDbBackupAgeInHours.skip.dbs";
	public static final String  DEFAULT_alarm_LastDbBackupAgeInHoursSkipDbs      = "";
	public static final String  PROPKEY_alarm_LastDbBackupAgeInHoursForSrv       = CM_NAME + ".alarm.system.if.LastDbBackupAgeInHours.for.srv";
	public static final String  DEFAULT_alarm_LastDbBackupAgeInHoursForSrv       = "";
	public static final String  PROPKEY_alarm_LastDbBackupAgeInHoursSkipSrv      = CM_NAME + ".alarm.system.if.LastDbBackupAgeInHours.skip.srv";
	public static final String  DEFAULT_alarm_LastDbBackupAgeInHoursSkipSrv      = "";

	public static final String  PROPKEY_alarm_LastIncDbBackupAgeInHours          = CM_NAME + ".alarm.system.if.LastIncDbBackupAgeInHours.gt";
	public static final int     DEFAULT_alarm_LastIncDbBackupAgeInHours          = 999_999; // 114 years; more or less disabled
	public static final String  PROPKEY_alarm_LastIncDbBackupAgeInHoursForDbs    = CM_NAME + ".alarm.system.if.LastIncDbBackupAgeInHours.for.dbs";
	public static final String  DEFAULT_alarm_LastIncDbBackupAgeInHoursForDbs    = "";
	public static final String  PROPKEY_alarm_LastIncDbBackupAgeInHoursSkipDbs   = CM_NAME + ".alarm.system.if.LastIncDbBackupAgeInHours.skip.dbs";
	public static final String  DEFAULT_alarm_LastIncDbBackupAgeInHoursSkipDbs   = "";
	public static final String  PROPKEY_alarm_LastIncDbBackupAgeInHoursForSrv    = CM_NAME + ".alarm.system.if.LastIncDbBackupAgeInHours.for.srv";
	public static final String  DEFAULT_alarm_LastIncDbBackupAgeInHoursForSrv    = "";
	public static final String  PROPKEY_alarm_LastIncDbBackupAgeInHoursSkipSrv   = CM_NAME + ".alarm.system.if.LastIncDbBackupAgeInHours.skip.srv";
	public static final String  DEFAULT_alarm_LastIncDbBackupAgeInHoursSkipSrv   = "";

	public static final String  PROPKEY_alarm_LastLogBackupAgeInHours            = CM_NAME + ".alarm.system.if.LastLogBackupAgeInHours.gt";
	public static final int     DEFAULT_alarm_LastLogBackupAgeInHours            = 999_999; // 114 years; more or less disabled
	public static final String  PROPKEY_alarm_LastLogBackupAgeInHoursForDbs      = CM_NAME + ".alarm.system.if.LastLogBackupAgeInHours.for.dbs";
	public static final String  DEFAULT_alarm_LastLogBackupAgeInHoursForDbs      = "";
	public static final String  PROPKEY_alarm_LastLogBackupAgeInHoursSkipDbs     = CM_NAME + ".alarm.system.if.LastLogBackupAgeInHours.skip.dbs";
	public static final String  DEFAULT_alarm_LastLogBackupAgeInHoursSkipDbs     = "";
	public static final String  PROPKEY_alarm_LastLogBackupAgeInHoursForSrv      = CM_NAME + ".alarm.system.if.LastLogBackupAgeInHours.for.srv";
	public static final String  DEFAULT_alarm_LastLogBackupAgeInHoursForSrv      = "";
	public static final String  PROPKEY_alarm_LastLogBackupAgeInHoursSkipSrv     = CM_NAME + ".alarm.system.if.LastLogBackupAgeInHours.skip.srv";
	public static final String  DEFAULT_alarm_LastLogBackupAgeInHoursSkipSrv     = "";

	public static final String  PROPKEY_alarm_LowDbFreeSpaceInMb                 = CM_NAME + ".alarm.system.if.LowDbFreeSpaceInMb.lt";
//	public static final String  DEFAULT_alarm_LowDbFreeSpaceInMb                 = ".*=2, tempdb=100";
	public static final String  DEFAULT_alarm_LowDbFreeSpaceInMb                 = "";

	public static final String  PROPKEY_alarm_LowLogFreeSpaceInMb                = CM_NAME + ".alarm.system.if.LowLogFreeSpaceInMb.lt";
	public static final String  DEFAULT_alarm_LowLogFreeSpaceInMb                = "";

	public static final String  PROPKEY_alarm_LowDbFreeSpaceInPct                = CM_NAME + ".alarm.system.if.LowDbFreeSpaceInPct.gt";
//	public static final String  DEFAULT_alarm_LowDbFreeSpaceInPct                = "tempdb=80";
	public static final String  DEFAULT_alarm_LowDbFreeSpaceInPct                = "";

	public static final String  PROPKEY_alarm_LowLogFreeSpaceInPct               = CM_NAME + ".alarm.system.if.LowLogFreeSpaceInPct.gt";
	public static final String  DEFAULT_alarm_LowLogFreeSpaceInPct               = "";

	public static final String  PROPKEY_alarm_LowOsDiskFreeSpaceInMb             = CM_NAME + ".alarm.system.if.LowOsDiskFreeSpaceInMb.lt";
	public static final String  DEFAULT_alarm_LowOsDiskFreeSpaceInMb             = ".*=500";

	public static final String  PROPKEY_alarm_LowOsDiskFreeSpaceInPct            = CM_NAME + ".alarm.system.if.LowOsDiskFreeSpaceInPct.gt";
	public static final String  DEFAULT_alarm_LowOsDiskFreeSpaceInPct            = ".*=95";

	// A comma separated list of databases that *must* exists, othewise ALARM. (the dbname can be a regexp, but each entry in the list must have a count of 1 after we have looped all records)
	// TODO: NOT YET IMPLEMENTED... think a bit more about this...
//	public static final String  PROPKEY_alarm_MandatoryDatabaseList              = CM_NAME + ".alarm.system.MandatoryDatabaseList";
//	public static final String  DEFAULT_alarm_MandatoryDatabaseList              = "";

	public static final String  PROPKEY_alarm_DbState                            = CM_NAME + ".alarm.system.if.state.not.in";
	public static final String  DEFAULT_alarm_DbState                            = ".*=(MULTI_USER|ONLINE)";

	public static final String  PROPKEY_alarm_QsIsOk                             = CM_NAME + ".alarm.system.if.QsIsOk.ne";
	public static final String  DEFAULT_alarm_QsIsOk                             = "YES";

	public static final String  PROPKEY_alarm_QsUsedSpaceInPct                   = CM_NAME + ".alarm.system.if.QsUsedPct.gt";
	public static final double  DEFAULT_alarm_QsUsedSpaceInPct                   = 90.0;

	public static final String  PROPKEY_alarm_QsFreeSpaceInMb                    = CM_NAME + ".alarm.system.if.QsFreeSpaceInMb.lt";
	public static final int     DEFAULT_alarm_QsFreeSpaceInMb                    = 20;

	public static final String  PROPKEY_alarm_LastGoodCheckDbDays                = CM_NAME + ".alarm.system.if.LastGoodCheckDbDays.gt";
	public static final String  DEFAULT_alarm_LastGoodCheckDbDays                = ".*=99999"; // 99_999; // Basically turned OFF

	public static final String  PROPKEY_alarm_AgMandatoryDbnames                 = CM_NAME + ".alarm.system.ag.mandatory.dbnames";
	public static final String  DEFAULT_alarm_AgMandatoryDbnames                 = "";

	public static final String  PROPKEY_alarm_AgMandatoryDbnamesSkip             = CM_NAME + ".alarm.system.ag.mandatory.dbnames.skip";
	public static final String  DEFAULT_alarm_AgMandatoryDbnamesSkip             = "";

	public static final String  PROPKEY_alarm_AgMandatoryDbnamesSkipSystem       = CM_NAME + ".alarm.system.ag.mandatory.dbnames.skip.system";
	public static final String  DEFAULT_alarm_AgMandatoryDbnamesSkipSystem       = "master, tempdb, model, msdb, SSISDB, ReportServer, ReportServerTempDB";

	public static final String  PROPKEY_alarm_DbSizeInMbDiff                     = CM_NAME + ".alarm.system.if.DbSizeInMbDiff.gt";
	public static final int     DEFAULT_alarm_DbSizeInMbDiff                     = 10; // set this to a *high* value to disable
	public static final String  PROPKEY_alarm_DbSizeInMbDiffForDbs               = CM_NAME + ".alarm.system.if.DbSizeInMbDiff.for.dbs";
	public static final String  DEFAULT_alarm_DbSizeInMbDiffForDbs               = "";
	public static final String  PROPKEY_alarm_DbSizeInMbDiffSkipDbs              = CM_NAME + ".alarm.system.if.DbSizeInMbDiff.skip.dbs";
	public static final String  DEFAULT_alarm_DbSizeInMbDiffSkipDbs              = "";
	public static final String  PROPKEY_alarm_DbSizeInMbDiffForSrv               = CM_NAME + ".alarm.system.if.DbSizeInMbDiff.for.srv";
	public static final String  DEFAULT_alarm_DbSizeInMbDiffForSrv               = "";
	public static final String  PROPKEY_alarm_DbSizeInMbDiffSkipSrv              = CM_NAME + ".alarm.system.if.DbSizeInMbDiff.skip.srv";
	public static final String  DEFAULT_alarm_DbSizeInMbDiffSkipSrv              = "";
	
	@Override
	public List<CmSettingsHelper> getLocalAlarmSettings()
	{
		Configuration conf = Configuration.getCombinedConfiguration();
		List<CmSettingsHelper> list = new ArrayList<>();
		
		CmSettingsHelper.Type isAlarmSwitch = CmSettingsHelper.Type.IS_ALARM_SWITCH;
		
		list.add(new CmSettingsHelper("OldestTranInSeconds",              isAlarmSwitch, PROPKEY_alarm_OldestTranInSeconds             , Integer.class, conf.getIntProperty   (PROPKEY_alarm_OldestTranInSeconds             , DEFAULT_alarm_OldestTranInSeconds             ), DEFAULT_alarm_OldestTranInSeconds             , "If 'OldestTranInSeconds' is greater than ## then send 'AlarmEventLongRunningTransaction'." ));
		list.add(new CmSettingsHelper("OldestTranInSeconds SkipTranName",                PROPKEY_alarm_OldestTranInSecondsSkipTranName , String .class, conf.getProperty      (PROPKEY_alarm_OldestTranInSecondsSkipTranName , DEFAULT_alarm_OldestTranInSecondsSkipTranName ), DEFAULT_alarm_OldestTranInSecondsSkipTranName , "If 'OldestTranInSeconds' is true; then we can filter out transaction names using a Regular expression... if (tranName.matches('regexp'))... This to remove alarms of 'DUMP DATABASE' or similar. A good place to test your regexp is 'http://www.regexplanet.com/advanced/java/index.html'.", new RegExpInputValidator()));
		list.add(new CmSettingsHelper("OldestTranInSeconds SkipTranProg",                PROPKEY_alarm_OldestTranInSecondsSkipTranProg , String .class, conf.getProperty      (PROPKEY_alarm_OldestTranInSecondsSkipTranProg , DEFAULT_alarm_OldestTranInSecondsSkipTranProg ), DEFAULT_alarm_OldestTranInSecondsSkipTranProg , "If 'OldestTranInSeconds' is true; then we can filter out transaction names using a Regular expression... if (tranProg.matches('regexp'))... This to remove alarms of 'SQLAgent.*' or similar. A good place to test your regexp is 'http://www.regexplanet.com/advanced/java/index.html'.", new RegExpInputValidator()));
		list.add(new CmSettingsHelper("OldestTranInSeconds SkipTranUser",                PROPKEY_alarm_OldestTranInSecondsSkipTranUser , String .class, conf.getProperty      (PROPKEY_alarm_OldestTranInSecondsSkipTranUser , DEFAULT_alarm_OldestTranInSecondsSkipTranUser ), DEFAULT_alarm_OldestTranInSecondsSkipTranUser , "If 'OldestTranInSeconds' is true; then we can filter out transaction names using a Regular expression... if (tranUser.matches('regexp'))... This to remove alarms of '(user1|user2)' or similar. A good place to test your regexp is 'http://www.regexplanet.com/advanced/java/index.html'.", new RegExpInputValidator()));
		list.add(new CmSettingsHelper("OldestTranInSeconds SkipTranHost",                PROPKEY_alarm_OldestTranInSecondsSkipTranHost , String .class, conf.getProperty      (PROPKEY_alarm_OldestTranInSecondsSkipTranHost , DEFAULT_alarm_OldestTranInSecondsSkipTranHost ), DEFAULT_alarm_OldestTranInSecondsSkipTranHost , "If 'OldestTranInSeconds' is true; then we can filter out transaction names using a Regular expression... if (tranHost.matches('regexp'))... This to remove alarms of '.*-prod-.*' or similar. A good place to test your regexp is 'http://www.regexplanet.com/advanced/java/index.html'.", new RegExpInputValidator()));

//		list.add(new CmSettingsHelper("TransactionLogFull",               isAlarmSwitch, PROPKEY_alarm_TransactionLogFull              , Integer.class, conf.getIntProperty   (PROPKEY_alarm_TransactionLogFull              , DEFAULT_alarm_TransactionLogFull              ), DEFAULT_alarm_TransactionLogFull              , "If 'TransactionLogFull' is greater than ## then send 'AlarmEventFullTranLog'." ));

//		list.add(new CmSettingsHelper("LastBackupFailed",                 isAlarmSwitch, PROPKEY_alarm_LastBackupFailed                , Integer.class, conf.getIntProperty   (PROPKEY_alarm_LastBackupFailed                , DEFAULT_alarm_LastBackupFailed                ), DEFAULT_alarm_LastBackupFailed                , "If 'LastBackupFailed' is greater than ## then send 'AlarmEventLastBackupFailed'." ));

		list.add(new CmSettingsHelper("LastDbBackupAgeInHours",           isAlarmSwitch, PROPKEY_alarm_LastDbBackupAgeInHours          , Integer.class, conf.getIntProperty   (PROPKEY_alarm_LastDbBackupAgeInHours          , DEFAULT_alarm_LastDbBackupAgeInHours          ), DEFAULT_alarm_LastDbBackupAgeInHours          , "If 'LastDbBackupAgeInHours' is greater than ## then send 'AlarmEventOldBackup'." ));
		list.add(new CmSettingsHelper("LastDbBackupAgeInHours ForDbs",                   PROPKEY_alarm_LastDbBackupAgeInHoursForDbs    , String .class, conf.getProperty      (PROPKEY_alarm_LastDbBackupAgeInHoursForDbs    , DEFAULT_alarm_LastDbBackupAgeInHoursForDbs    ), DEFAULT_alarm_LastDbBackupAgeInHoursForDbs    , "If 'LastDbBackupAgeInHours' is true; Only for the databases listed (regexp is used, blank=for-all-dbs). After this rule the 'skip' rule is evaluated.", new RegExpInputValidator()));
		list.add(new CmSettingsHelper("LastDbBackupAgeInHours SkipDbs",                  PROPKEY_alarm_LastDbBackupAgeInHoursSkipDbs   , String .class, conf.getProperty      (PROPKEY_alarm_LastDbBackupAgeInHoursSkipDbs   , DEFAULT_alarm_LastDbBackupAgeInHoursSkipDbs   ), DEFAULT_alarm_LastDbBackupAgeInHoursSkipDbs   , "If 'LastDbBackupAgeInHours' is true; Discard databases listed (regexp is used). Before this rule the 'for/keep' rule is evaluated",                     new RegExpInputValidator()));
		list.add(new CmSettingsHelper("LastDbBackupAgeInHours ForSrv",                   PROPKEY_alarm_LastDbBackupAgeInHoursForSrv    , String .class, conf.getProperty      (PROPKEY_alarm_LastDbBackupAgeInHoursForSrv    , DEFAULT_alarm_LastDbBackupAgeInHoursForSrv    ), DEFAULT_alarm_LastDbBackupAgeInHoursForSrv    , "If 'LastDbBackupAgeInHours' is true; Only for the servers listed (regexp is used, blank=for-all-srv). After this rule the 'skip' rule is evaluated.",   new RegExpInputValidator()));
		list.add(new CmSettingsHelper("LastDbBackupAgeInHours SkipSrv",                  PROPKEY_alarm_LastDbBackupAgeInHoursSkipSrv   , String .class, conf.getProperty      (PROPKEY_alarm_LastDbBackupAgeInHoursSkipSrv   , DEFAULT_alarm_LastDbBackupAgeInHoursSkipSrv   ), DEFAULT_alarm_LastDbBackupAgeInHoursSkipSrv   , "If 'LastDbBackupAgeInHours' is true; Discard servers listed (regexp is used). Before this rule the 'for/keep' rule is evaluated",                       new RegExpInputValidator()));

		list.add(new CmSettingsHelper("LastIncDbBackupAgeInHours",        isAlarmSwitch, PROPKEY_alarm_LastIncDbBackupAgeInHours       , Integer.class, conf.getIntProperty   (PROPKEY_alarm_LastIncDbBackupAgeInHours       , DEFAULT_alarm_LastIncDbBackupAgeInHours       ), DEFAULT_alarm_LastIncDbBackupAgeInHours       , "If 'LastIncDbBackupAgeInHours' is greater than ## then send 'AlarmEventOldIncrementalBackup'." ));
		list.add(new CmSettingsHelper("LastIncDbBackupAgeInHours ForDbs",                PROPKEY_alarm_LastIncDbBackupAgeInHoursForDbs , String .class, conf.getProperty      (PROPKEY_alarm_LastIncDbBackupAgeInHoursForDbs , DEFAULT_alarm_LastIncDbBackupAgeInHoursForDbs ), DEFAULT_alarm_LastIncDbBackupAgeInHoursForDbs , "If 'LastIncDbBackupAgeInHours' is true; Only for the databases listed (regexp is used, blank=for-all-dbs). After this rule the 'skip' rule is evaluated.", new RegExpInputValidator()));
		list.add(new CmSettingsHelper("LastIncDbBackupAgeInHours SkipDbs",               PROPKEY_alarm_LastIncDbBackupAgeInHoursSkipDbs, String .class, conf.getProperty      (PROPKEY_alarm_LastIncDbBackupAgeInHoursSkipDbs, DEFAULT_alarm_LastIncDbBackupAgeInHoursSkipDbs), DEFAULT_alarm_LastIncDbBackupAgeInHoursSkipDbs, "If 'LastIncDbBackupAgeInHours' is true; Discard databases listed (regexp is used). Before this rule the 'for/keep' rule is evaluated",                     new RegExpInputValidator()));
		list.add(new CmSettingsHelper("LastIncDbBackupAgeInHours ForSrv",                PROPKEY_alarm_LastIncDbBackupAgeInHoursForSrv , String .class, conf.getProperty      (PROPKEY_alarm_LastIncDbBackupAgeInHoursForSrv , DEFAULT_alarm_LastIncDbBackupAgeInHoursForSrv ), DEFAULT_alarm_LastIncDbBackupAgeInHoursForSrv , "If 'LastIncDbBackupAgeInHours' is true; Only for the servers listed (regexp is used, blank=for-all-srv). After this rule the 'skip' rule is evaluated.",   new RegExpInputValidator()));
		list.add(new CmSettingsHelper("LastIncDbBackupAgeInHours SkipSrv",               PROPKEY_alarm_LastIncDbBackupAgeInHoursSkipSrv, String .class, conf.getProperty      (PROPKEY_alarm_LastIncDbBackupAgeInHoursSkipSrv, DEFAULT_alarm_LastIncDbBackupAgeInHoursSkipSrv), DEFAULT_alarm_LastIncDbBackupAgeInHoursSkipSrv, "If 'LastIncDbBackupAgeInHours' is true; Discard servers listed (regexp is used). Before this rule the 'for/keep' rule is evaluated",                       new RegExpInputValidator()));

		list.add(new CmSettingsHelper("LastLogBackupAgeInHours",          isAlarmSwitch, PROPKEY_alarm_LastLogBackupAgeInHours         , Integer.class, conf.getIntProperty   (PROPKEY_alarm_LastLogBackupAgeInHours         , DEFAULT_alarm_LastLogBackupAgeInHours         ), DEFAULT_alarm_LastLogBackupAgeInHours         , "If 'LastLogBackupAgeInHours' is greater than ## then send 'AlarmEventOldBackup'." ));
		list.add(new CmSettingsHelper("LastLogBackupAgeInHours ForDbs",                  PROPKEY_alarm_LastLogBackupAgeInHoursForDbs   , String .class, conf.getProperty      (PROPKEY_alarm_LastLogBackupAgeInHoursForDbs   , DEFAULT_alarm_LastLogBackupAgeInHoursForDbs   ), DEFAULT_alarm_LastLogBackupAgeInHoursForDbs   , "If 'LastLogBackupAgeInHours' is true; Only for the databases listed (regexp is used, blank=skip-no-dbs). After this rule the 'skip' rule is evaluated.", new RegExpInputValidator()));
		list.add(new CmSettingsHelper("LastLogBackupAgeInHours SkipDbs",                 PROPKEY_alarm_LastLogBackupAgeInHoursSkipDbs  , String .class, conf.getProperty      (PROPKEY_alarm_LastLogBackupAgeInHoursSkipDbs  , DEFAULT_alarm_LastLogBackupAgeInHoursSkipDbs  ), DEFAULT_alarm_LastLogBackupAgeInHoursSkipDbs  , "If 'LastLogBackupAgeInHours' is true; Discard databases listed (regexp is used). Before this rule the 'for/keep' rule is evaluated",                     new RegExpInputValidator()));
		list.add(new CmSettingsHelper("LastLogBackupAgeInHours ForSrv",                  PROPKEY_alarm_LastLogBackupAgeInHoursForSrv   , String .class, conf.getProperty      (PROPKEY_alarm_LastLogBackupAgeInHoursForSrv   , DEFAULT_alarm_LastLogBackupAgeInHoursForSrv   ), DEFAULT_alarm_LastLogBackupAgeInHoursForSrv   , "If 'LastLogBackupAgeInHours' is true; Only for the servers listed (regexp is used, blank=skip-no-srv). After this rule the 'skip' rule is evaluated.",   new RegExpInputValidator()));
		list.add(new CmSettingsHelper("LastLogBackupAgeInHours SkipSrv",                 PROPKEY_alarm_LastLogBackupAgeInHoursSkipSrv  , String .class, conf.getProperty      (PROPKEY_alarm_LastLogBackupAgeInHoursSkipSrv  , DEFAULT_alarm_LastLogBackupAgeInHoursSkipSrv  ), DEFAULT_alarm_LastLogBackupAgeInHoursSkipSrv  , "If 'LastLogBackupAgeInHours' is true; Discard servers listed (regexp is used). Before this rule the 'for/keep' rule is evaluated",                       new RegExpInputValidator()));

		list.add(new CmSettingsHelper("LowDbFreeSpaceInMb",               isAlarmSwitch, PROPKEY_alarm_LowDbFreeSpaceInMb              , String .class, conf.getProperty      (PROPKEY_alarm_LowDbFreeSpaceInMb              , DEFAULT_alarm_LowDbFreeSpaceInMb              ), DEFAULT_alarm_LowDbFreeSpaceInMb              , "If 'LowDbFreeSpaceInMb' is greater than ## MB then send 'AlarmEventLowDbFreeSpace'. format: db1=#, db2=#, db3=#  (Note: the 'dbname' can use regexp)"     , new MapNumberValidator()));
		list.add(new CmSettingsHelper("LowLogFreeSpaceInMb",              isAlarmSwitch, PROPKEY_alarm_LowLogFreeSpaceInMb             , String .class, conf.getProperty      (PROPKEY_alarm_LowLogFreeSpaceInMb             , DEFAULT_alarm_LowLogFreeSpaceInMb             ), DEFAULT_alarm_LowLogFreeSpaceInMb             , "If 'LowLogFreeSpaceInMb' is greater than ## MB then send 'AlarmEventLowLogFreeSpace'.format: db1=#, db2=#, db3=#  (Note: the 'dbname' can use regexp)"    , new MapNumberValidator()));
		list.add(new CmSettingsHelper("LowDbFreeSpaceInPct",              isAlarmSwitch, PROPKEY_alarm_LowDbFreeSpaceInPct             , String .class, conf.getProperty      (PROPKEY_alarm_LowDbFreeSpaceInPct             , DEFAULT_alarm_LowDbFreeSpaceInPct             ), DEFAULT_alarm_LowDbFreeSpaceInPct             , "If 'LowDbFreeSpaceInPct' is less than ## Percent then send 'AlarmEventLowDbFreeSpace'.format: db1=#, db2=#, db3=#  (Note: the 'dbname' can use regexp)"   , new MapNumberValidator()));
		list.add(new CmSettingsHelper("LowLogFreeSpaceInPct",             isAlarmSwitch, PROPKEY_alarm_LowLogFreeSpaceInPct            , String .class, conf.getProperty      (PROPKEY_alarm_LowLogFreeSpaceInPct            , DEFAULT_alarm_LowLogFreeSpaceInPct            ), DEFAULT_alarm_LowLogFreeSpaceInPct            , "If 'LowLogFreeSpaceInPct' is less than ## Percent then send 'AlarmEventLowLogFreeSpace'.format: db1=#, db2=#, db3=#  (Note: the 'dbname' can use regexp)" , new MapNumberValidator())); 

		list.add(new CmSettingsHelper("LowOsDiskFreeSpaceInMb",           isAlarmSwitch, PROPKEY_alarm_LowOsDiskFreeSpaceInMb          , String .class, conf.getProperty      (PROPKEY_alarm_LowOsDiskFreeSpaceInMb          , DEFAULT_alarm_LowOsDiskFreeSpaceInMb          ), DEFAULT_alarm_LowOsDiskFreeSpaceInMb          , "If 'LogOsDiskFreeMb' or 'DataOsDiskFreeMb' is less than ## MB then send 'AlarmEventLowOsDiskFreeSpace'.format: db1=#, db2=#, db3=#  (Note: the 'dbname' can use regexp)"        , new MapNumberValidator()));
		list.add(new CmSettingsHelper("LowOsDiskFreeSpaceInPct",          isAlarmSwitch, PROPKEY_alarm_LowOsDiskFreeSpaceInPct         , String .class, conf.getProperty      (PROPKEY_alarm_LowOsDiskFreeSpaceInPct         , DEFAULT_alarm_LowOsDiskFreeSpaceInPct         ), DEFAULT_alarm_LowOsDiskFreeSpaceInPct         , "If 'LogOsDiskFreePct' or 'DataOsDiskFreePct' is less than ## Percent then send 'AlarmEventLowOsDiskFreeSpace'.format: db1=#, db2=#, db3=#  (Note: the 'dbname' can use regexp)" , new MapNumberValidator()));

//		list.add(new CmSettingsHelper("MandatoryDatabaseList",                           PROPKEY_alarm_MandatoryDatabaseList           , String .class, conf.getProperty      (PROPKEY_alarm_MandatoryDatabaseList           , DEFAULT_alarm_MandatoryDatabaseList           ), DEFAULT_alarm_MandatoryDatabaseList           , "A list of databases that needs to be present. This is a comma separated list of databases (each name can contain regex)" ));

		list.add(new CmSettingsHelper("DbState",                          isAlarmSwitch, PROPKEY_alarm_DbState                         , String .class, conf.getProperty      (PROPKEY_alarm_DbState                         , DEFAULT_alarm_DbState                         ), DEFAULT_alarm_DbState                         , "If 'user_access_desc' or 'state_desc' do NOT match the regexp then send 'AlarmEventDatabaseState'. format: db1=(MULTI_USER|RESTRICTED_USER|ONLINE|OFFLINE ), db2=(MULTI_USER|ONLINE|RESTORING)  (Note: the 'dbname' can aslo be a regexp)" , new RegExpInputValidator()));

		list.add(new CmSettingsHelper("QsIsOk",                           isAlarmSwitch, PROPKEY_alarm_QsIsOk                          , String .class, conf.getProperty      (PROPKEY_alarm_QsIsOk                          , DEFAULT_alarm_QsIsOk                          ), DEFAULT_alarm_QsIsOk                          , "If 'QsIsOk' is not 'YES' (meaning 'QsDesiredState' and 'QsActualState' do not match) then send 'AlarmEventQueryStoreUnexpectedState'. Note: This is not configurabe per database name"));
		list.add(new CmSettingsHelper("QsUsedPct",                        isAlarmSwitch, PROPKEY_alarm_QsUsedSpaceInPct                , Double .class, conf.getDoubleProperty(PROPKEY_alarm_QsUsedSpaceInPct                , DEFAULT_alarm_QsUsedSpaceInPct                ), DEFAULT_alarm_QsUsedSpaceInPct                , "If 'QsUsedSpaceInPct' more than ## Percent then send 'AlarmEventQueryStoreLowFreeSpace'. Note: This is not configurabe per database name"));
		list.add(new CmSettingsHelper("QsFreeSpaceInMb",                  isAlarmSwitch, PROPKEY_alarm_QsFreeSpaceInMb                 , Integer.class, conf.getIntProperty   (PROPKEY_alarm_QsFreeSpaceInMb                 , DEFAULT_alarm_QsFreeSpaceInMb                 ), DEFAULT_alarm_QsFreeSpaceInMb                 , "If 'QsFreeSpaceInMb' is less that ## MB then send 'AlarmEventQueryStoreLowFreeSpace'. Note: This is not configurabe per database name"));

		list.add(new CmSettingsHelper("LastGoodCheckDbDays",              isAlarmSwitch, PROPKEY_alarm_LastGoodCheckDbDays             , String .class, conf.getProperty      (PROPKEY_alarm_LastGoodCheckDbDays             , DEFAULT_alarm_LastGoodCheckDbDays             ), DEFAULT_alarm_LastGoodCheckDbDays             , "If DBCC CHECKDB hasn't been executed for X days... ('LastGoodCheckDbDays' is greater that ## DAYS) then send 'AlarmEventDbccCheckdbAge'. format: db1=#, db2=#, db3=#  (Note: the 'dbname' can use regexp)" , new MapNumberValidator()));

		list.add(new CmSettingsHelper("AgMandatoryDbnames",               isAlarmSwitch, PROPKEY_alarm_AgMandatoryDbnames              , String .class, conf.getProperty      (PROPKEY_alarm_AgMandatoryDbnames              , DEFAULT_alarm_AgMandatoryDbnames              ), DEFAULT_alarm_AgMandatoryDbnames              , "If database do NOT belong to an Availability Group and are part of this list, then send 'AlarmEventDbNotInHadr'. format: Comma Separated List (Note: the 'dbname' can use regexp)" , new RegExpInputValidator()));
		list.add(new CmSettingsHelper("AgMandatoryDbnamesSkip",           isAlarmSwitch, PROPKEY_alarm_AgMandatoryDbnamesSkip          , String .class, conf.getProperty      (PROPKEY_alarm_AgMandatoryDbnamesSkip          , DEFAULT_alarm_AgMandatoryDbnamesSkip          ), DEFAULT_alarm_AgMandatoryDbnamesSkip          , "If database do NOT belong to an Availability Group and are NOT part of this list, then send 'AlarmEventDbNotInHadr'. format: Comma Separated List (Note: the 'dbname' can use regexp)" , new RegExpInputValidator()));
		list.add(new CmSettingsHelper("AgMandatoryDbnamesSkipSystem",                    PROPKEY_alarm_AgMandatoryDbnamesSkipSystem    , String .class, conf.getProperty      (PROPKEY_alarm_AgMandatoryDbnamesSkipSystem    , DEFAULT_alarm_AgMandatoryDbnamesSkipSystem    ), DEFAULT_alarm_AgMandatoryDbnamesSkipSystem    , "A list of SQL Server System databases to NOT check for Availability Group. format: Comma Separated List (Note: the 'dbname' can use regexp)" , new RegExpInputValidator()));

		// Add alarm for Database Grow (INFO Alarm every time a database grows/shrinks)
		list.add(new CmSettingsHelper("DbSizeInMbDiff",                   isAlarmSwitch, PROPKEY_alarm_DbSizeInMbDiff                  , Integer.class, conf.getIntProperty   (PROPKEY_alarm_DbSizeInMbDiff                  , DEFAULT_alarm_DbSizeInMbDiff                  ), DEFAULT_alarm_DbSizeInMbDiff                  , "If 'DbSizeInMbDiff' is changed more than this value. (both grow and shrink sizes are respected."));
		list.add(new CmSettingsHelper("DbSizeInMbDiff ForDbs",                           PROPKEY_alarm_DbSizeInMbDiffForDbs            , String .class, conf.getProperty      (PROPKEY_alarm_DbSizeInMbDiffForDbs            , DEFAULT_alarm_DbSizeInMbDiffForDbs            ), DEFAULT_alarm_DbSizeInMbDiffForDbs            , "If 'DbSizeInMbDiff' is true; Only for the databases listed (regexp is used, blank=skip-no-dbs). After this rule the 'skip' rule is evaluated.", new RegExpInputValidator()));
		list.add(new CmSettingsHelper("DbSizeInMbDiff SkipDbs",                          PROPKEY_alarm_DbSizeInMbDiffSkipDbs           , String .class, conf.getProperty      (PROPKEY_alarm_DbSizeInMbDiffSkipDbs           , DEFAULT_alarm_DbSizeInMbDiffSkipDbs           ), DEFAULT_alarm_DbSizeInMbDiffSkipDbs           , "If 'DbSizeInMbDiff' is true; Discard databases listed (regexp is used). Before this rule the 'for/keep' rule is evaluated",                     new RegExpInputValidator()));
		list.add(new CmSettingsHelper("DbSizeInMbDiff ForSrv",                           PROPKEY_alarm_DbSizeInMbDiffForSrv            , String .class, conf.getProperty      (PROPKEY_alarm_DbSizeInMbDiffForSrv            , DEFAULT_alarm_DbSizeInMbDiffForSrv            ), DEFAULT_alarm_DbSizeInMbDiffForSrv            , "If 'DbSizeInMbDiff' is true; Only for the servers listed (regexp is used, blank=skip-no-srv). After this rule the 'skip' rule is evaluated.",   new RegExpInputValidator()));
		list.add(new CmSettingsHelper("DbSizeInMbDiff SkipSrv",                          PROPKEY_alarm_DbSizeInMbDiffSkipSrv           , String .class, conf.getProperty      (PROPKEY_alarm_DbSizeInMbDiffSkipSrv           , DEFAULT_alarm_DbSizeInMbDiffSkipSrv           ), DEFAULT_alarm_DbSizeInMbDiffSkipSrv           , "If 'DbSizeInMbDiff' is true; Discard servers listed (regexp is used). Before this rule the 'for/keep' rule is evaluated",                       new RegExpInputValidator()));

		// TODO: Possibly: Database Options, like we do in ASE (get options from SCOPED Database Options)
		
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

