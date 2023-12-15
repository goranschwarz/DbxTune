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

import java.awt.event.MouseEvent;
import java.math.BigDecimal;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import javax.naming.NameNotFoundException;
import javax.swing.JDialog;
import javax.swing.JOptionPane;

import org.apache.commons.lang3.math.NumberUtils;
import org.apache.log4j.Logger;

import com.asetune.ICounterController;
import com.asetune.IGuiController;
import com.asetune.alarm.AlarmHandler;
import com.asetune.alarm.events.AlarmEvent;
import com.asetune.alarm.events.AlarmEventDatabaseOption;
import com.asetune.alarm.events.AlarmEventFullTranLog;
import com.asetune.alarm.events.AlarmEventLastBackupFailed;
import com.asetune.alarm.events.AlarmEventLongRunningTransaction;
import com.asetune.alarm.events.AlarmEventLowDbFreeSpace;
import com.asetune.alarm.events.AlarmEventLowLogFreeSpace;
import com.asetune.alarm.events.AlarmEventOldBackup;
import com.asetune.alarm.events.AlarmEventOldTranLogBackup;
import com.asetune.central.pcs.CentralPersistReader;
import com.asetune.cm.CmSettingsHelper;
import com.asetune.cm.CmSettingsHelper.MapNumberValidator;
import com.asetune.cm.CmSettingsHelper.RegExpInputValidator;
import com.asetune.cm.CmSybMessageHandler;
import com.asetune.cm.CounterSample;
import com.asetune.cm.CounterSetTemplates;
import com.asetune.cm.CounterSetTemplates.Type;
import com.asetune.cm.CountersModel;
import com.asetune.cm.ase.gui.CmOpenDatabasesPanel;
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
import com.asetune.sql.conn.info.DbmsVersionInfoSybaseAse;
import com.asetune.utils.AseConnectionUtils;
import com.asetune.utils.CollectionUtils;
import com.asetune.utils.Configuration;
import com.asetune.utils.StringUtil;
import com.asetune.utils.Ver;

/**
 * @author Goran Schwarz (goran_schwarz@hotmail.com)
 */
public class CmOpenDatabases
extends CountersModel
{
	private static Logger        _logger          = Logger.getLogger(CmOpenDatabases.class);
	private static final long    serialVersionUID = 1L;

	public static final String   CM_NAME          = CmOpenDatabases.class.getSimpleName();
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
		"    <li>LIGHT_BLUE/PURPLE - A Database backup is in progress</li>" +
		"    <li>YELLOW            - Has a long running transaction issued by a user.</li>" +
		"    <li>PINK              - The transaction log for this database is filled to 90%, and will probably soon be full.</li>" +
		"    <li>RED               - The transaction log for this database is <b>full</b> and users are probably suspended.</li>" +
		"    <li>RED               - Last backup failed.</li>" +
		"</ul>" +
		"</html>";

	public static final String   GROUP_NAME       = MainFrame.TCP_GROUP_SERVER;
	public static final String   GUI_ICON_FILE    = "images/"+CM_NAME+".png";

	public static final long     NEED_SRV_VERSION = 0;
	public static final long     NEED_CE_VERSION  = 0;

	public static final String[] MON_TABLES       = new String[] {"monOpenDatabases"};
	public static final String[] NEED_ROLES       = new String[] {"mon_role"};
	public static final String[] NEED_CONFIG      = new String[] {"enable monitoring=1"};

	public static final String[] PCT_COLUMNS      = new String[] {"AppendLogContPct", "LogSizeUsedPct", "DataSizeUsedPct"};
	public static final String[] DIFF_COLUMNS     = new String[] {
		"AppendLogRequests", "AppendLogWaits", 
		"PRSUpdateCount", "PRSSelectCount", "PRSRewriteCount",
		"LogSizeFreeInMbDiff",  "LogSizeUsedInMbDiff",
		"DataSizeFreeInMbDiff", "DataSizeUsedInMbDiff",
		"ReservedMb", "UsedMb", "UnUsedMb", "DataMb", "DataMbReal", "IndexMb", "IndexMbReal", "LobMb",
		"ReservedPages", "UsedPages", "UnUsedPages", "DataPages", "DataPagesReal", "IndexPages", "IndexPagesReal", "LobPages", "Tables", "RowCountSum", "OamPages", "AllocationUnits"};

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

		return new CmOpenDatabases(counterController, guiController);
	}

	public CmOpenDatabases(ICounterController counterController, IGuiController guiController)
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

	public static final String  PROPKEY_sample_spaceusage            = PROP_PREFIX + ".sample.spaceusage";
	public static final boolean DEFAULT_sample_spaceusage            = false;

	public static final String  PROPKEY_sample_showplan              = PROP_PREFIX + ".sample.showplan";
	public static final boolean DEFAULT_sample_showplan              = true;
	
	public static final String  PROPKEY_sample_monSqlText            = PROP_PREFIX + ".sample.monSqltext";
	public static final boolean DEFAULT_sample_monSqlText            = true;

	public static final String  PROPKEY_sample_locks                 = PROP_PREFIX + ".sample.locks";
	public static final boolean DEFAULT_sample_locks                 = true;

//	public static final String  PROPKEY_spaceusageInMb               = PROP_PREFIX + ".sample.spaceusageInMb";
//	public static final boolean DEFAULT_spaceusageInMb               = false;
	

	public static final String  PROPKEY_disable_spaceusage_onTimeout = PROP_PREFIX + ".disable.spaceusage.onTimeoutException";
	public static final boolean DEFAULT_disable_spaceusage_onTimeout = true;
	
	public static final String GRAPH_NAME_LOGSEMAPHORE_CONT  = "DbLogSemapContGraph";   //String x=GetCounters.CM_GRAPH_NAME__OPEN_DATABASES__LOGSEMAPHORE_CONT;
	public static final String GRAPH_NAME_LOGSIZE_LEFT_MB    = "DbLogSizeLeftMbGraph";  //String x=GetCounters.CM_GRAPH_NAME__OPEN_DATABASES__LOGSIZE_LEFT;
	public static final String GRAPH_NAME_LOGSIZE_USED_MB    = "DbLogSizeUsedMbGraph";
	public static final String GRAPH_NAME_LOGSIZE_USED_PCT   = "DbLogSizeUsedPctGraph"; //String x=GetCounters.CM_GRAPH_NAME__OPEN_DATABASES__LOGSIZE_USED_PCT;
	public static final String GRAPH_NAME_DATASIZE_LEFT_MB   = "DbDataSizeLeftMbGraph";
	public static final String GRAPH_NAME_DATASIZE_USED_MB   = "DbDataSizeUsedMbGraph";
	public static final String GRAPH_NAME_DATASIZE_USED_PCT  = "DbDataSizeUsedPctGraph";
//	public static final String GRAPH_NAME_OLDEST_TRAN_IN_SEC = "OldestTranInSecGraph";
	public static final String GRAPH_NAME_TEMPDB_USED_MB     = "TempdbUsedMbGraph";

	private void addTrendGraphs()
	{
		addTrendGraph(GRAPH_NAME_LOGSEMAPHORE_CONT,
			"DB Transaction Log Semaphore Contention",            // Menu CheckBox text
			"DB Transaction Log Semaphore Contention in Percent ("+GROUP_NAME+"->"+SHORT_NAME+")", // Label 
			TrendGraphDataPoint.createGraphProps(TrendGraphDataPoint.Y_AXIS_SCALE_LABELS_PERCENT, CentralPersistReader.SampleType.MAX_OVER_SAMPLES),
			null, 
			LabelType.Dynamic,
			TrendGraphDataPoint.Category.DISK,
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
	}

	@Override
	protected TabularCntrPanel createGui()
	{
		return new CmOpenDatabasesPanel(this);
	}

	@Override
	protected CmSybMessageHandler createSybMessageHandler()
	{
		CmSybMessageHandler msgHandler = super.createSybMessageHandler();

		// for getServerVersion() the CM needs to be initialized.
//		if (getServerVersion() >= Ver.ver(16,0,0, 2)) // 16.0 SP2
//		{
//		}

		// in 16.0 SP2 when using: query_text(spid)
		// we might get: Msg=10228, Text=The specified spid value '8' applies to a server internal process, which does not execute a query plan
		msgHandler.addDiscardMsgNum(10228);

		return msgHandler;
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
			mtd.addColumn("monOpenDatabases", "CeDbRecoveryStatus", "<html>" +
			                                                             "1 = The database is currently undergoing <B>node-failure</B> recovery.<br> " +
			                                                             "0 = Normal, <B>not</B> in node-failure recovery." +
			                                                        "</html>");
			mtd.addColumn("monOpenDatabases", "AppendLogContPct",   "<html>" +
			                                                             "Log Semaphore Contention in percent.<br> " +
			                                                             "<b>Formula</b>: Pct = (AppendLogWaits / AppendLogRequests) * 100<br>" +
			                                                        "</html>");
			mtd.addColumn("monOpenDatabases", "LogDataIsMixed",     "<html>Data Pages and Log pages are mixed on some device(s).<br><b>Formula</b>: sysdatabases.status2 has bit 32768 (0x8000) set.</html>");
			mtd.addColumn("monOpenDatabases", "DbSizeInMb",         "<html>Database size in MB</html>");
			mtd.addColumn("monOpenDatabases", "LogSizeInMb",        "<html>" +
			                                                             "Size in MB of the transaction log in the database. <br>" +
			                                                             "<b>Formula</b>: This is simply grabbed by: sum(size) from sysusages where (segmap & 4) = 4<br>" +
			                                                        "</html>");
			mtd.addColumn("monOpenDatabases", "LogSizeUsedInMb",    "<html>" +
			                                                             "How many MB have we used of the Transaction log.<br> " +
			                                                             "<b>Formula</b>: LogSizeInMb - LogSizeFreeInMb<br>" +
			                                                             "<b>Note 1</b>: This is the same formula as sp_helpdb 'dbname' uses to calculate space left.<br>" +
			                                                             "<b>Note 2</b>: This might not work correct for databases with mixed data and log.<br>" +
			                                                        "</html>");
			mtd.addColumn("monOpenDatabases", "LogSizeUsedInMbDiff","<html>Same as column 'LogSizeUsedInMb', but just show the difference from previous sample.</html>");
			mtd.addColumn("monOpenDatabases", "LogSizeFreeInMb",    "<html>" +
			                                                             "How many MB have we got left in the Transaction log.<br> " +
			                                                             "<b>Formula</b>: (lct_admin('logsegment_freepages',DBID)-lct_admin('reserved_for_rollbacks',DBID)) / (1024.0*1024.0/@@maxpagesize)<br>" +
			                                                             "<b>Note 1</b>: This is the same formula as sp_helpdb 'dbname' uses to calculate space left.<br>" +
			                                                             "<b>Note 2</b>: This might not work correct for databases with mixed data and log.<br>" +
			                                                             "<b>Note 3</b>: If column 'TransactionLogFull' then 'LogSizeFreeInMb' is set to 0, this to prevent any faulty calculations or strange values from lct_admin().<br>" +
			                                                        "</html>");
			mtd.addColumn("monOpenDatabases", "LogSizeFreeInMbDiff","<html>Same as column 'LogSizeFreeInMb', but just show the difference from previous sample.</html>");
			mtd.addColumn("monOpenDatabases", "LogSizeUsedPct",     "<html>" +
			                                                            "How many percent have we <b>used</b> of the transaction log. near 100% = Full<br> " +
			                                                            "<b>Formula</b>: Pct = 100.0 - ((oval_LogSizeFreeInMb / oval_LogSizeInMb) * 100.0)<br>" +
			                                                        "</html>");
			mtd.addColumn("monOpenDatabases", "DataSizeInMb",       "<html>" +
			                                                            "Size in MB of the Data Portion in the database. <br>" +
			                                                            "<b>Formula</b>: This is simply grabbed by: sum(size) from sysusages where (segmap & (2147483647-4)) > 0 -- meaning: all segments except 4, the logsegment<br>" +
			                                                        "</html>");
			mtd.addColumn("monOpenDatabases", "DataSizeUsedInMb",   "<html>" +
			                                                            "How many MB have we used of the Data Area (everything with segmap not in 4 'logsegment').<br> " +
			                                                            "<b>Formula</b>: DataSizeInMb - DataSizeFreeInMb<br>" +
			                                                            "<b>Note 1</b>: This is the same formula as sp_helpdb 'dbname' uses to calculate space left.<br>" +
			                                                            "<b>Note 2</b>: This might not work correct for databases with mixed data and log.<br>" +
			                                                         "</html>");
			mtd.addColumn("monOpenDatabases", "DataSizeUsedInMbDiff","<html>Same as column 'DataSizeUsedInMb', but just show the difference from previous sample.</html>");
			mtd.addColumn("monOpenDatabases", "DataSizeFreeInMb",   "<html>" +
			                                                            "How many MB have we got left in the Data Portion.<br> " +
			                                                            "<b>Formula</b>: (select sum(curunreservedpgs(u.dbid, u.lstart, u.unreservedpgs)/(1024.0*1024.0/@@maxpagesize)) from master.dbo.sysusages u readpast where u.dbid = od.DBID and (segmap & (2147483647-4)) > 0) -- -- meaning: all segments except 4, the logsegment<br>" +
			                                                            "<b>Note 1</b>: This is the same formula as sp_helpdb 'dbname' uses to calculate space left.<br>" +
			                                                            "<b>Note 2</b>: This might not work correct for databases with mixed data and log.<br>" +
			                                                        "</html>");
			mtd.addColumn("monOpenDatabases", "DataSizeFreeInMbDiff","<html>Same as column 'DataSizeFreeInMb', but just show the difference from previous sample.</html>");
			mtd.addColumn("monOpenDatabases", "DataSizeUsedPct",     "<html>" +
			                                                            "How many percent have we <b>used</b> of the Data Portion. near 100% = Full<br> " +
			                                                            "<b>Formula</b>: Pct = 100.0 - ((oval_DataSizeFreeInMb / oval_DataSizeInMb) * 100.0)<br>" +
			                                                         "</html>");
			mtd.addColumn("monOpenDatabases", "OldestTranStartTime","<html>" +
			                                                            "Start time of the oldest open transaction in this database.<br> " +
			                                                            "<b>Formula</b>: OldestTranStartTime = column: master.dbo.syslogshold.starttime<br>" +
			                                                        "</html>");
			mtd.addColumn("monOpenDatabases", "OldestTranInSeconds","<html>" +
			                                                            "Number of seconds since the oldest open transaction in this database was started. <br> " +
			                                                            "<b>Formula</b>: OldestTranInSeconds = datediff(ss, master.dbo.syslogshold.starttime, getdate())<br>" +
			                                                        "</html>");
			mtd.addColumn("monOpenDatabases", "OldestTranName",     "<html>" +
			                                                            "Name of the oldest open transaction in this database.<br> " +
			                                                            "<b>Formula</b>: OldestTranName = column: master.dbo.syslogshold.name<br>" +
			                                                        "</html>");
			mtd.addColumn("monOpenDatabases", "OldestTranSpid",     "<html>" +
			                                                            "SPID, which is responsible for the oldest open transaction in this database<br> " +
			                                                            "<b>Formula</b>: OldestTranSpid = column: master.dbo.syslogshold.spid<br>" +
			                                                        "</html>");
			mtd.addColumn("monOpenDatabases", "SPID",               "<html>" +
			                                                            "<b>Note</b>: same column as <code>OldestTranSpid</code>, just as another label to enable right click menu in table<br>" +
			                                                            "SPID, which is responsible for the oldest open transaction in this database<br> " +
			                                                            "<b>Formula</b>: SPID = column: master.dbo.syslogshold.spid<br>" +
			                                                        "</html>");
			mtd.addColumn("monOpenDatabases", "OldestTranProg",     "<html>" +
			                                                             "Application Name, which is resopnsible for the oldest open transaction in this database<br> " +
			                                                             "<b>Formula</b>: OldestTranProg = (select p.program_name from master.dbo.sysprocesses p where p.spid = master.dbo.syslogshold.spid)<br>" +
			                                                        "</html>");
			mtd.addColumn("monOpenDatabases", "OldestTranPage",     "<html>" +
			                                                            "Page number in the transaction log, which holds the oldest open transaction in this database.<br> " +
			                                                            "<b>Note:</b> you can do: dbcc traceon(3604)  dbcc page(dbid, pagenum) to see the content of that page.<br> " +
			                                                            "<b>Formula</b>: OldestTranProg = column: master.dbo.syslogshold.page<br>" +
			                                                        "</html>");

			mtd.addColumn("monOpenDatabases",  "SrvPageSize",    "ASE Servers page size (@@maxpagesize)");

			mtd.addColumn("monOpenDatabases", "RawSpaceUsage",     
					"<html>" +
						"Returns metrics for space use in SAP ASE as a comma-separated string.<br> " +
						"<b>Formula</b>: function: spaceusage(dbid)<br>" +
						"<br>" +
						"Below is a description from the SAP/Sybase manual" +
						"<ul>" +
						"  <li><code>reserved pages</code>   - number of pages reserved for an object, which may include index pages if you selected index IDs based on the input parameters</li>" +
						"  <li><code>used pages</code>       - number of pages used by the object, which may include index pages if you selected index IDs based on the input parameters.<br>"
						                                    + "The value for used pages that spaceusage returns when you specify index_id = 1 (that is, for all-pages clustered indexes) is the used page count for the index layer of the clustered index. However, the value the used_pages function returns when you specify index_id = 1 includes the used page counts for the data and the index layers.</li>" +
						"  <li><code>data pages</code>       - number of data pages used by the object, which may include index pages if you selected index IDs based on the input parameters.</li>" +
						"  <li><code>index pages</code>      - index pages � number of index-only pages, if the input parameters specified processing indexes on the objects. To determine the number of pages used for only the index-level pages, subtract the number of large object (LOB) pages from the number of index pages.</li>" +
						"  <li><code>oam pages</code>        - number of OAM pages for all OAM chains, as selected by the input parameters.<br>"
						                                    + "For example, if you specify:<br>"
						                                    + "<code>spaceusage(database_id, object_id, index_id)</code><br>"
						                                    + "oam pages indicates the number of OAM pages found for this index and any of its local index partitions. If you run spaceusage against a specific object, oam pages returns the amount of overhead for the extra pages used for this object�s space management.<br>"
						                                    + "When you execute spaceusage for an <b>entire database</i>, oam pages returns the total overhead for the number of OAM pages needed to track space across all objects, and their off-row LOB columns.</li>" +
						"  <li><code>allocation units</code> - number of allocation units that hold one or more extents for the specified object, index, or partition. allocation units indicates how many allocation units (or pages) Adaptive Server must scan while accessing all the pages of that object, index, or partition.<br>"
						                                    + "When you run spaceusage against the <b>entire database</b>, allocation units returns the total number of allocation units reserving space for an object. However, because Adaptive Server can share allocation units across objects, this field might show a number greater than the total number of allocation units in the entire database.</li>" +
						"  <li><code>row count</code>        - number of rows in the object or partition. spaceusage reports this row count as 0 when you specify the index_id parameter.</li>" +
						"  <li><code>tables</code>           - total number of tables processed when you execute spaceusage and include only the database_id parameter (that is, when you are investigating space metrics for the entire database).</li>" +
						"  <li><code>LOB pages</code>        - number of off-row large object pages for which the index ID is 255.<br>"
						                                    + "LOB pages returns a nonzero value only when you use spaceusage to determine the space metrics for all indexes, or only the LOB index, on objects that contain off-row LOB data. LOB pages returns 0 when you use spaceusage to examine the space metrics only for tables (which have index IDs of 0).<br>"
						                                    + "When you run spaceusage against the <b>entire database</b>, LOB pages displays the aggregate page counts for all LOB columns occupying off-row storage in all objects.</li>" +
						"  <li><code>syslog pages</code>     - Currently, spaceusage does not report on syslogs</li>" +
						"</ul>" +
						"However, spaceusage does not report on tables that do not occupy space (for example, fake and proxy tables).<br>" +
					"</html>");

			mtd.addColumn("monOpenDatabases", "ReservedMb",
					"<html>" +
						"This is 'reserved pages' represented as MB instead of Pages, output from the function <code>spaceusage(dbid)</code>.<br>" +
						"<code>reserved pages</code> - number of pages reserved for an object, which may include index pages if you selected index IDs based on the input parameters.<br>" +
						"<b>Formula</b>: function: spaceusage(dbid)<br>" +
					"</html>");

			mtd.addColumn("monOpenDatabases", "UsedMb",
					"<html>" +
						"This is 'used pages' represented as MB instead of Pages, output from the function <code>spaceusage(dbid)</code>.<br>" +
						"<code>used pages</code> - number of pages used by the object, which may include index pages if you selected index IDs based on the input parameters.<br>" +
						"The value for used pages that spaceusage returns when you specify index_id = 1 (that is, for all-pages clustered indexes) is the used page count for the index layer of the clustered index. However, the value the used_pages function returns when you specify index_id = 1 includes the used page counts for the data and the index layers.<br>" +
						"<b>Formula</b>: function: spaceusage(dbid)<br>" +
					"</html>");

			mtd.addColumn("monOpenDatabases", "UnUsedMb",
					"<html>" +
						"Calculate how many MB that are Un-Used.<br>" +
						"<b>Formula</b>: (ReservedPages - UsedPages) / (1024*1024/@@maxpagesize)<br>" +
					"</html>");

			mtd.addColumn("monOpenDatabases", "DataMb",
					"<html>" +
						"This is 'data pages' represented as MB instead of Pages, output from the function <code>spaceusage(dbid)</code>.<br>" +
						"<code>data pages</code> - number of data pages used by the object, which may include index pages if you selected index IDs based on the input parameters.<br>" +
						"<b>Formula</b>: function: spaceusage(dbid)<br>" +
					"</html>");

			mtd.addColumn("monOpenDatabases", "DataMbReal",
					"<html>" +
						"Take away Index MB from the data pages and you get <i>actual</i> usage.<br>" +
						"<b>Formula</b>: (DataPages - IndexPages) / (1024*1024/@@maxpagesize)<br>" +
					"</html>");

			mtd.addColumn("monOpenDatabases", "IndexMb",
					"<html>" +
						"This is 'index pages' represented as MB instead of Pages, output from the function <code>spaceusage(dbid)</code>.<br>" +
						"<code>index pages</code> - index pages � number of index-only pages, if the input parameters specified processing indexes on the objects. To determine the number of pages used for only the index-level pages, subtract the number of large object (LOB) pages from the number of index pages.<br>" +
						"<b>Formula</b>: function: spaceusage(dbid)<br>" +
					"</html>");

			mtd.addColumn("monOpenDatabases", "IndexMbReal",
					"<html>" +
						"Take away LOB pages from the index pages and you get <i>actual</i> usage.<br>" +
						"<b>Formula</b>: (IndexPages - LobPages) / (1024*1024/@@maxpagesize)<br>" +
					"</html>");

			mtd.addColumn("monOpenDatabases", "LobMb",
					"<html>" +
						"This is 'LOB pages' represented as MB instead of Pages, output from the function <code>spaceusage(dbid)</code>.<br>" +
						"<code>LOB pages</code> - number of off-row large object pages for which the index ID is 255.<br>" +
                        "LOB pages returns a nonzero value only when you use spaceusage to determine the space metrics for all indexes, or only the LOB index, on objects that contain off-row LOB data. LOB pages returns 0 when you use spaceusage to examine the space metrics only for tables (which have index IDs of 0).<br>" +
                        "When you run spaceusage against the <b>entire database</b>, LOB pages displays the aggregate page counts for all LOB columns occupying off-row storage in all objects.</li>" +
						"<b>Formula</b>: function: spaceusage(dbid)<br>" +
					"</html>");


			mtd.addColumn("monOpenDatabases", "ReservedPages",
					"<html>" +
						"This is 'reserved pages' output from the function <code>spaceusage(dbid)</code>.<br>" +
						"<code>reserved pages</code> - number of pages reserved for an object, which may include index pages if you selected index IDs based on the input parameters.<br>" +
						"<b>Formula</b>: function: spaceusage(dbid)<br>" +
//						"<b>Note</b>: if 'Spaceusage in MB' is checked, this will be in MB, check column 'RawSpaceUsage' for the <i>raw</i> values.<br>" +
					"</html>");

			mtd.addColumn("monOpenDatabases", "UsedPages",
					"<html>" +
						"This is 'used pages' output from the function <code>spaceusage(dbid)</code>.<br>" +
						"<code>used pages</code> - number of pages used by the object, which may include index pages if you selected index IDs based on the input parameters.<br>" +
						"The value for used pages that spaceusage returns when you specify index_id = 1 (that is, for all-pages clustered indexes) is the used page count for the index layer of the clustered index. However, the value the used_pages function returns when you specify index_id = 1 includes the used page counts for the data and the index layers.<br>" +
						"<b>Formula</b>: function: spaceusage(dbid)<br>" +
//						"<b>Note</b>: if 'Spaceusage in MB' is checked, this will be in MB, check column 'RawSpaceUsage' for the <i>raw</i> values.<br>" +
					"</html>");

			mtd.addColumn("monOpenDatabases", "UnUsedPages",
					"<html>" +
						"Calculate how many pages that are Un-Used.<br>" +
						"<b>Formula</b>: ReservedPages - UsedPages<br>" +
					"</html>");

			mtd.addColumn("monOpenDatabases", "DataPages",
					"<html>" +
						"This is 'data pages' output from the function <code>spaceusage(dbid)</code>.<br>" +
						"<code>data pages</code> - number of data pages used by the object, which may include index pages if you selected index IDs based on the input parameters.<br>" +
						"<b>Formula</b>: function: spaceusage(dbid)<br>" +
//						"<b>Note</b>: if 'Spaceusage in MB' is checked, this will be in MB, check column 'RawSpaceUsage' for the <i>raw</i> values.<br>" +
					"</html>");

			mtd.addColumn("monOpenDatabases", "DataPagesReal",
					"<html>" +
						"Take away Index pages from the data pages and you get <i>actual</i> usage.<br>" +
						"<b>Formula</b>: DataPages - IndexPages<br>" +
					"</html>");

			mtd.addColumn("monOpenDatabases", "IndexPages",
					"<html>" +
						"This is 'index pages' output from the function <code>spaceusage(dbid)</code>.<br>" +
						"<code>index pages</code> - index pages � number of index-only pages, if the input parameters specified processing indexes on the objects. To determine the number of pages used for only the index-level pages, subtract the number of large object (LOB) pages from the number of index pages.<br>" +
						"<b>Formula</b>: function: spaceusage(dbid)<br>" +
//						"<b>Note</b>: if 'Spaceusage in MB' is checked, this will be in MB, check column 'RawSpaceUsage' for the <i>raw</i> values.<br>" +
					"</html>");

			mtd.addColumn("monOpenDatabases", "IndexPagesReal",
					"<html>" +
						"Take away LOB pages from the index pages and you get <i>actual</i> usage.<br>" +
						"<b>Formula</b>: IndexPages - LobPages<br>" +
					"</html>");

			mtd.addColumn("monOpenDatabases", "LobPages",
					"<html>" +
						"This is 'LOB pages' output from the function <code>spaceusage(dbid)</code>.<br>" +
						"<code>LOB pages</code> - number of off-row large object pages for which the index ID is 255.<br>" +
                        "LOB pages returns a nonzero value only when you use spaceusage to determine the space metrics for all indexes, or only the LOB index, on objects that contain off-row LOB data. LOB pages returns 0 when you use spaceusage to examine the space metrics only for tables (which have index IDs of 0).<br>" +
                        "When you run spaceusage against the <b>entire database</b>, LOB pages displays the aggregate page counts for all LOB columns occupying off-row storage in all objects.</li>" +
						"<b>Formula</b>: function: spaceusage(dbid)<br>" +
//						"<b>Note</b>: if 'Spaceusage in MB' is checked, this will be in MB, check column 'RawSpaceUsage' for the <i>raw</i> values.<br>" +
					"</html>");

			mtd.addColumn("monOpenDatabases", "Tables",
					"<html>" +
						"This is 'tables' output from the function <code>spaceusage(dbid)</code>.<br>" +
						"<code>tables</code> - total number of tables processed when you execute spaceusage and include only the database_id parameter (that is, when you are investigating space metrics for the entire database).<br>" +
						"<b>Formula</b>: function: spaceusage(dbid)<br>" +
//						"<b>Note</b>: if 'Spaceusage in MB' is checked, this is <b>NOT</b> presenetd as MB.<br>" +
					"</html>");

			mtd.addColumn("monOpenDatabases", "RowCountSum",
					"<html>" +
						"This is 'row count' output from the function <code>spaceusage(dbid)</code>.<br>" +
						"<code>row count</code> - number of rows in the object or partition. spaceusage reports this row count as 0 when you specify the index_id parameter.<br>" +
						"<b>Formula</b>: function: spaceusage(dbid)<br>" +
//						"<b>Note</b>: if 'Spaceusage in MB' is checked, this is <b>NOT</b> presenetd as MB.<br>" +
					"</html>");

			mtd.addColumn("monOpenDatabases", "OamPages",
					"<html>" +
						"This is 'oam pages' output from the function <code>spaceusage(dbid)</code>.<br>" +
						"<code>oam pages</code> - number of OAM pages for all OAM chains, as selected by the input parameters.<br>" +
						"For example, if you specify:<br>" +
						"<code>spaceusage(database_id, object_id, index_id)</code><br>" +
						"oam pages indicates the number of OAM pages found for this index and any of its local index partitions. If you run spaceusage against a specific object, oam pages returns the amount of overhead for the extra pages used for this object�s space management.<br>" +
						"When you execute spaceusage for an <b>entire database</i>, oam pages returns the total overhead for the number of OAM pages needed to track space across all objects, and their off-row LOB columns.<br>" +
						"<b>Formula</b>: function: spaceusage(dbid)<br>" +
//						"<b>Note</b>: if 'Spaceusage in MB' is checked, this will be in MB, check column 'RawSpaceUsage' for the <i>raw</i> values.<br>" +
					"</html>");

			mtd.addColumn("monOpenDatabases", "AllocationUnits",
					"<html>" +
						"This is 'allocation units' output from the function <code>spaceusage(dbid)</code>.<br>" +
						"<code>allocation units</code> - number of allocation units that hold one or more extents for the specified object, index, or partition. allocation units indicates how many allocation units (or pages) Adaptive Server must scan while accessing all the pages of that object, index, or partition.<br>" +
						"When you run spaceusage against the <b>entire database</b>, allocation units returns the total number of allocation units reserving space for an object. However, because Adaptive Server can share allocation units across objects, this field might show a number greater than the total number of allocation units in the entire database.<br>" +
						"<b>Formula</b>: function: spaceusage(dbid)<br>" +
//						"<b>Note</b>: if 'Spaceusage in MB' is checked, this is <b>NOT</b> presenetd as MB.<br>" +
					"</html>");

			mtd.addColumn("monOpenDatabases", "SyslogPages",
					"<html>" +
						"This is 'syslog pages' output from the function <code>spaceusage(dbid)</code>.<br>" +
						"<code>syslog pages</code> - Currently, spaceusage does not report on syslogs.<br>" +
						"<b>Formula</b>: function: spaceusage(dbid)<br>" +
//						"<b>Note</b>: if 'Spaceusage in MB' is checked, this will be in MB, check column 'RawSpaceUsage' for the <i>raw</i> values.<br>" +
					"</html>");

			mtd.addColumn("monOpenDatabases", "LastDbBackupAgeInHours",
					"<html>" +
						"Number of hours since last database backup/dump was done.<br>" +
						"-1 if 'dump database...' has ever been done.<br>" +
						"<b>Formula</b>: datediff(hour, x.BackupStartTime, getdate())<br>" +
					"</html>");

			mtd.addColumn("monOpenDatabases", "LastLogBackupAgeInHours",
					"<html>" +
						"Number of hours since last transaction log backup/dump was done.<br>" +
						"-1 if 'dump tran...' has ever been done.<br>" +
						"<b>Formula</b>: datediff(hour, x.LastTranLogDumpTime, getdate())<br>" +
					"</html>");

			mtd.addColumn("monOpenDatabases", "DBOptions",    "<html>Database options for this database. Values are decoded from the columns 'status*' and 'minLogMode'.</html>");
			mtd.addColumn("monOpenDatabases", "status",       "<html>Integer value saved in master..sysdatabases for keeping various database statuses</html>");
			mtd.addColumn("monOpenDatabases", "status1",      "<html>Integer value saved in master..sysdatabases for keeping various database statuses</html>");
			mtd.addColumn("monOpenDatabases", "status2",      "<html>Integer value saved in master..sysdatabases for keeping various database statuses</html>");
			mtd.addColumn("monOpenDatabases", "status3",      "<html>Integer value saved in master..sysdatabases for keeping various database statuses</html>");
			mtd.addColumn("monOpenDatabases", "status4",      "<html>Integer value saved in master..sysdatabases for keeping various database statuses</html>");
			mtd.addColumn("monOpenDatabases", "status5",      "<html>Integer value saved in master..sysdatabases for keeping various database statuses</html>");
			mtd.addColumn("monOpenDatabases", "minLogMode",   "<html>Integer value saved in master..sysattributes for keeping dboption: 'full logging for {all|select into|alter table|reorg rebuild}'</html>");

		}
		catch (NameNotFoundException e) {/*ignore*/}
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
			map.put("OldestTranLocks"       , new PcsColumnOptions(ColumnType.DICTIONARY_COMPRESSION));

			// Set the map in the super
			setPcsColumnOptions(map);
		}

		return map;
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

		pkCols.add("DBName");

		return pkCols;
	}

	/** Used by the: Create 'Offline Session' Wizard */
	@Override
	public List<CmSettingsHelper> getLocalSettings()
	{
		Configuration conf = Configuration.getCombinedConfiguration();
		List<CmSettingsHelper> list = new ArrayList<>();
		
		list.add(new CmSettingsHelper("Sample Spaceusage Details",     PROPKEY_sample_spaceusage , Boolean.class, conf.getBooleanProperty(PROPKEY_sample_spaceusage , DEFAULT_sample_spaceusage ), DEFAULT_sample_spaceusage, "Execute spaceusage(dbid) on every sample. Only in ASE 16.0 and above." ));
//		list.add(new CmSettingsHelper("Space Usage in MB",         PROPKEY_spaceusageInMb ,    Boolean.class, conf.getBooleanProperty(PROPKEY_spaceusageInMb    , DEFAULT_spaceusageInMb    ), DEFAULT_spaceusageInMb,    "Calculate spaceusage in MB instead of pages."                          ));
		list.add(new CmSettingsHelper("Sample Showplan on Open Trans", PROPKEY_sample_showplan   , Boolean.class, conf.getBooleanProperty(PROPKEY_sample_showplan   , DEFAULT_sample_showplan   ), DEFAULT_sample_showplan  , "Get sp_showplan on on SPID's that has an open transaction." ));
		list.add(new CmSettingsHelper("Sample SQL Text on Open Trans", PROPKEY_sample_monSqlText , Boolean.class, conf.getBooleanProperty(PROPKEY_sample_monSqlText , DEFAULT_sample_monSqlText ), DEFAULT_sample_monSqlText, "Get SQL Text (from monProcessSQLText) on on SPID's that has an open transaction" ));

		return list;
	}


	@Override
	public String getSqlForVersion(DbxConnection conn, DbmsVersionInfo versionInfo)
	{
		DbmsVersionInfoSybaseAse aseVersionInfo = (DbmsVersionInfoSybaseAse) versionInfo;
		long    srvVersion       = aseVersionInfo.getLongVersion();
		boolean isClusterEnabled = aseVersionInfo.isClusterEdition();

		String cols1, cols2, cols3, cols4;
		cols1 = cols2 = cols3 = cols4 = "";

		boolean sampleSpaceusage = Configuration.getCombinedConfiguration().getBooleanProperty(PROPKEY_sample_spaceusage, DEFAULT_sample_spaceusage);
		
		boolean canDoSelectOnSyslogshold = true;

		if (isRuntimeInitialized())
		{
			// Check if we can do select on syslogshold
			canDoSelectOnSyslogshold = AseConnectionUtils.canDoSelectOnTable(conn, "master.dbo.syslogshold");
			if ( ! canDoSelectOnSyslogshold )
				_logger.warn("When trying to initialize Counters Model '"+getName()+"', named '"+getDisplayName()+"'. Problems accessing 'master.dbo.syslogshold' table, columns starting with 'OldestTran*' will not hold valid data.");
		}

		if (isClusterEnabled)
		{
			cols1 += "od.InstanceID, ";
		}

		String ceDbRecoveryStatus = ""; // 
		String QuiesceTag         = "";
		String SuspendedProcesses = "";
//		if (srvVersion >= 12510)
//		if (srvVersion >= 1251000)
		if (srvVersion >= Ver.ver(12,5,1))
		{
			QuiesceTag         = "od.QuiesceTag, ";
			SuspendedProcesses = "od.SuspendedProcesses, ";
		}
		if (isClusterEnabled)
		{
			ceDbRecoveryStatus = "CeDbRecoveryStatus = db_recovery_status(od.DBID), ";
		}
		
		// 15.7 ESD#2
		String PRSUpdateCount  = "";
		String PRSSelectCount  = "";
		String PRSRewriteCount = "";
		String nl_15702        = "";
		if (srvVersion >= Ver.ver(15,7,0,2))
		{
			PRSUpdateCount  = "od.PRSUpdateCount, ";  // Number of updates to PRSes (Precomputed Result Set) caused by IUDs (Insert/Update/Delete) on the base table
			PRSSelectCount  = "od.PRSSelectCount, ";  // Number of times PRSes (Precomputed Result Set) were selected for query rewriting plan during compilation
			PRSRewriteCount = "od.PRSRewriteCount, "; // Number of times PRSes (Precomputed Result Set) were considered valid for query rewriting during compilation
			nl_15702        = " \n";
		}

		// 16.0 (and possibly 15.7 SP64 and SP136) // it might be earlier than 136 but it's not in low SP1xx 
		String RawSpaceUsage   = "";
		String ReservedMb      = "";
		String UsedMb          = "";
		String UnUsedMb        = "";
		String DataMb          = "";
		String DataMbReal      = "";
		String IndexMb         = "";
		String IndexMbReal     = "";
		String LobMb           = "";
		String ReservedPages   = "";
		String UsedPages       = "";
		String UnUsedPages     = "";
		String DataPages       = "";
		String DataPagesReal   = "";
		String IndexPages      = "";
		String IndexPagesReal  = "";
		String LobPages        = "";
		String Tables          = "";
		String RowCountSum     = "";
		String OamPages        = "";
		String AllocationUnits = "";
		String nl_160          = "";
//		if (srvVersion >= Ver.ver(16,0))
		// This is implemeted in ASE 15.7 SP136 and ASE 15.7 SP64 (but not in 15.7 SP100-SP135)
		if (srvVersion >= Ver.ver(15,7,0, 136) || (srvVersion >= Ver.ver(15,7,0, 64) && srvVersion < Ver.ver(15,7,0, 100)) )
		{
			if (sampleSpaceusage)
				RawSpaceUsage   = "RawSpaceUsage = spaceusage(od.DBID), ";
			else
				RawSpaceUsage   = "RawSpaceUsage = convert(varchar(255), '"+PROPKEY_sample_spaceusage+"=false'), ";

			ReservedMb   = "ReservedMb = convert(numeric(12,3), 0), ";   // Value is derived from spaceusage(dbid)
			UsedMb       = "UsedMb = convert(numeric(12,3), 0), ";       // Value is derived from spaceusage(dbid)
			UnUsedMb     = "UnUsedMb = convert(numeric(12,3), 0), ";     // Value is derived from spaceusage(dbid)
			DataMb       = "DataMb = convert(numeric(12,3), 0), ";       // Value is derived from spaceusage(dbid)
			DataMbReal   = "DataMbReal = convert(numeric(12,3), 0), ";   // Value is derived from spaceusage(dbid) but takes away IndexPages
			IndexMb      = "IndexMb = convert(numeric(12,3), 0), ";      // Value is derived from spaceusage(dbid)
			IndexMbReal  = "IndexMbReal = convert(numeric(12,3), 0), ";  // Value is derived from spaceusage(dbid) but takes away LoBPages
			LobMb        = "LobMb = convert(numeric(12,3), 0), ";        // Value is derived from spaceusage(dbid)

			ReservedPages   = "ReservedPages = convert(bigint, 0), ";   // Value is derived from spaceusage(dbid)
			UsedPages       = "UsedPages = convert(bigint, 0), ";       // Value is derived from spaceusage(dbid)
			UnUsedPages     = "UnUsedPages = convert(bigint, 0), ";     // Value is derived from spaceusage(dbid)
			DataPages       = "DataPages = convert(bigint, 0), ";       // Value is derived from spaceusage(dbid)
			DataPagesReal   = "DataPagesReal = convert(bigint, 0), ";   // Value is derived from spaceusage(dbid) but takes away IndexPages
			IndexPages      = "IndexPages = convert(bigint, 0), ";      // Value is derived from spaceusage(dbid)
			IndexPagesReal  = "IndexPagesReal = convert(bigint, 0), ";  // Value is derived from spaceusage(dbid) but takes away LoBPages
			LobPages        = "LobPages = convert(bigint, 0), ";        // Value is derived from spaceusage(dbid)
			Tables          = "Tables = convert(bigint, 0), ";          // Value is derived from spaceusage(dbid)
			RowCountSum     = "RowCountSum = convert(bigint, 0), ";     // Value is derived from spaceusage(dbid)
			OamPages        = "OamPages = convert(bigint, 0), ";        // Value is derived from spaceusage(dbid)
			AllocationUnits = "AllocationUnits = convert(bigint, 0), "; // Value is derived from spaceusage(dbid)
			
			nl_160        = " \n";
		}

		// If we implement the FreeLogSize, then we need to take away databases that are in recovery etc...
		// Also calculate it into MB...
		// The calculation is stolen from: sp_helpdb dbname

		// In ASE 15.7 when doing db shrink, it saves some rows in sysusages with a negative vdevno 
		String DbSizeInMb_extraWhere = srvVersion >= Ver.ver(15, 0) ? " and u.vdevno >= 0" : "";
		String DbSizeInMb             = "DbSizeInMb             = (select sum(u.size/(1024*1024/@@maxpagesize)) from master.dbo.sysusages u readpast where u.dbid = od.DBID" + DbSizeInMb_extraWhere + "), \n";
		String LogDataIsMixed         = "LogDataIsMixed         = (select convert(bit,(db.status2 & 32768)) from master.dbo.sysdatabases db readpast where db.dbid = od.DBID), \n";
		String IsUserTempdb           = "IsUserTempdb           = (select convert(bit,(db.status3 & 256))   from master.dbo.sysdatabases db readpast where db.dbid = od.DBID), \n";
                                                                
		String LogSizeInMb            = "LogSizeInMb            = (select sum(u.size/(1024*1024/@@maxpagesize)) from master.dbo.sysusages u readpast where u.dbid = od.DBID and (u.segmap & 4) = 4), \n";
//		String LogSizeFreeInMb        = "LogSizeFreeInMb        = convert(numeric(10,1), (lct_admin('logsegment_freepages',od.DBID)-lct_admin('reserved_for_rollbacks',od.DBID)) / (1024.0*1024.0/@@maxpagesize)), \n";
		String LogSizeFreeInMb        = "LogSizeFreeInMb        = convert(numeric(10,1), (CASE WHEN od.TransactionLogFull > 0 THEN 0 ELSE lct_admin('logsegment_freepages',od.DBID)-lct_admin('reserved_for_rollbacks',od.DBID) END) / (1024.0*1024.0/@@maxpagesize)), \n";
		String LogSizeFreeInMbDiff    = "LogSizeFreeInMbDiff    = convert(numeric(10,1), 0), /* calculated in AseTune : same as LogSizeFreeInMb */ \n";
		String LogSizeUsedInMb        = "LogSizeUsedInMb        = convert(numeric(10,1), 0), /* calculated in AseTune : LogSizeInMb - LogSizeFreeInMb */ \n";
		String LogSizeUsedInMbDiff    = "LogSizeUsedInMbDiff    = convert(numeric(10,1), 0), /* calculated in AseTune : same as LogSizeUsedInMb */ \n";
		String LogSizeUsedPct         = "LogSizeUsedPct         = convert(numeric(10,1), 0), /* calculated in AseTune */ \n";
                                                                
		String DataSizeInMb           = "DataSizeInMb           = (select sum(u.size/(1024*1024/@@maxpagesize)) from master.dbo.sysusages u readpast where u.dbid = od.DBID and (segmap & (2147483647-4)) > 0), \n";  // (Integer.MAX_VALUE-4) == all segments except 4, the logsegment
		String DataSizeFreeInMb       = "DataSizeFreeInMb       = convert(numeric(10,1), (select sum(curunreservedpgs(u.dbid, u.lstart, u.unreservedpgs)/(1024.0*1024.0/@@maxpagesize)) from master.dbo.sysusages u readpast where u.dbid = od.DBID and (segmap & (2147483647-4)) > 0)), \n";  // (Integer.MAX_VALUE-4) == all segments except 4, the logsegment
		String DataSizeFreeInMbDiff   = "DataSizeFreeInMbDiff   = convert(numeric(10,1), 0), /* calculated in AseTune : same as DataSizeFreeInMb */ \n";
		String DataSizeUsedInMb       = "DataSizeUsedInMb       = convert(numeric(10,1), 0), /* calculated in AseTune : DataSizeInMb - DataSizeFreeInMb*/ \n";
		String DataSizeUsedInMbDiff   = "DataSizeUsedInMbDiff   = convert(numeric(10,1), 0), /* calculated in AseTune : same as DataSizeUsedInMb */ \n";
		String DataSizeUsedPct        = "DataSizeUsedPct        = convert(numeric(10,1), 0), /* calculated in AseTune */ \n";

		String OldestTranStartTime    = "OldestTranStartTime    = h.starttime, \n";
		String OldestTranInSeconds    = "OldestTranInSeconds    = CASE WHEN datediff(day, h.starttime, getdate()) >= 24 THEN -1 ELSE  datediff(ss, h.starttime, getdate()) END, \n"; // protect from: Msg 535: Difference of two datetime fields caused overflow at runtime. above 24 days or so, the MS difference is overflowned
		String OldestTranName         = "OldestTranName         = h.name, \n";
		String OldestTranSpid         = "OldestTranSpid         = h.spid, \n";
		String SPID                   = "SPID                   = h.spid, \n";
		String OldestTranProg         = "OldestTranProg         = (select p.program_name     from master.dbo.sysprocesses p where h.spid = p.spid), \n";
		String OldestTranUser         = "OldestTranUser         = (select suser_name(p.suid) from master.dbo.sysprocesses p where h.spid = p.spid), \n";
		String OldestTranHost         = "OldestTranHost         = (select p.hostname         from master.dbo.sysprocesses p where h.spid = p.spid), \n";
		String OldestTranPage         = "OldestTranPage         = h.page,\n";
		String OldestTranProcName     = "OldestTranProcName     = (select isnull(object_name(p.id, p.dbid), object_name(p.id, 2)) from master.dbo.sysprocesses p where h.spid = p.spid), \n";
		String OldestTranHasSqlText   = "OldestTranHasSqlText   = convert(bit, 0), \n";
		String OldestTranHasShowPlan  = "OldestTranHasShowPlan  = convert(bit, 0), \n";
		String OldestTranHasLocks     = "OldestTranHasLocks     = convert(bit, 0), \n";
		String OldestTranSqlText      = "OldestTranSqlText      = convert(text, null), \n";
		String OldestTranShowPlanText = "OldestTranShowPlanText = convert(text, null), \n";
		String OldestTranLocks        = "OldestTranLocks        = convert(text, null), \n";

//		boolean getMonSqltext = Configuration.getCombinedConfiguration().getBooleanProperty(PROPKEY_sample_monSqlText, DEFAULT_sample_monSqlText);
//		if (getMonSqltext && srvVersion >= Ver.ver(16,0,0, 2)) // 16.0 PL1 did not have query_text()... so lets use 16.0 SP2 as base instead
//		{
//			OldestTranSqlText = "OldestTranSqlText      = CASE WHEN (h.spid is not null AND h.spid > 0) THEN query_text(h.spid) ELSE null END, \n";
//			// The below if we want to discard messages like: CmOpenDatabases: Received a Msg while reading the resultset from 'CmOpenDatabases', This could be mapped to a column by using a column name 'msgAsColValue' in the SELECT statement. Right now it's discarded. The message text: The specified spid value '8' applies to a server internal process, which does not execute a query plan.
//			// OldestTranSqlText = "OldestTranSqlText      = CASE WHEN (h.spid is not null AND h.spid > 0 AND exists(select 1 from master.dbo.sysprocesses p where p.spid = h.spid and p.suid != 0)) THEN query_text(h.spid) ELSE null END, \n"; 
//		}
		
		if ( ! canDoSelectOnSyslogshold )
		{
			OldestTranStartTime    = "OldestTranStartTime    = convert(datetime,    null), \n";
			OldestTranInSeconds    = "OldestTranInSeconds    = convert(int,         -1), \n";
			OldestTranName         = "OldestTranName         = convert(varchar(80), ''), \n";
			OldestTranSpid         = "OldestTranSpid         = convert(int,         -1), \n";
			SPID                   = "SPID                   = convert(int,         -1), \n";
			OldestTranProg         = "OldestTranProg         = convert(varchar(30), ''), \n";
			OldestTranUser         = "OldestTranUser         = convert(varchar(30), ''), \n";
			OldestTranHost         = "OldestTranHost         = convert(varchar(30), ''), \n";
			OldestTranPage         = "OldestTranPage         = convert(int,         -1), \n";
			OldestTranProcName     = "OldestTranProcName     = convert(varchar(30), NULL), \n";
			OldestTranHasSqlText   = "OldestTranHasSqlText   = convert(bit, 0), \n";
			OldestTranHasShowPlan  = "OldestTranHasShowPlan  = convert(bit, 0), \n";
			OldestTranHasLocks     = "OldestTranHasLocks     = convert(bit, 0), \n";
			OldestTranSqlText      = "OldestTranSqlText      = convert(text, null), \n";
			OldestTranShowPlanText = "OldestTranShowPlanText = convert(text, null), \n";
			OldestTranLocks        = "OldestTranLocks        = convert(text, null), \n";
		}

		cols1 += "od.DBName, od.DBID, " + ceDbRecoveryStatus + "od.AppendLogRequests, od.AppendLogWaits, \n" +
		         "AppendLogContPct = CASE \n" +
		         "                      WHEN od.AppendLogRequests > 0 \n" +
		         "                      THEN convert(numeric(10,2), ((od.AppendLogWaits+0.0)/od.AppendLogRequests)*100.0) \n" +
		         "                      ELSE convert(numeric(10,2), 0.0) \n" +
		         "                   END, \n" +
		         DbSizeInMb + 
		         LogDataIsMixed + 
		         IsUserTempdb +
		         LogSizeInMb  + LogSizeUsedInMb + LogSizeFreeInMb + LogSizeUsedInMbDiff + LogSizeFreeInMbDiff  +  
		         LogSizeUsedPct + DataSizeUsedPct +
		         DataSizeInMb + DataSizeUsedInMb + DataSizeFreeInMb + DataSizeUsedInMbDiff + DataSizeFreeInMbDiff + 
		         "od.TransactionLogFull, " + SuspendedProcesses + "\n" +
		         OldestTranStartTime +
		         OldestTranInSeconds +
		         OldestTranName      + 
		         OldestTranSpid      + 
		         SPID                + 
		         OldestTranProg      + 
		         OldestTranUser      +
		         OldestTranHost      +
		         OldestTranPage      + 
		         OldestTranProcName  +
		         OldestTranHasSqlText +
		         OldestTranHasShowPlan +
		         OldestTranHasLocks +
		         PRSUpdateCount + PRSSelectCount + PRSRewriteCount + nl_15702 +
		         ReservedMb + UsedMb + UnUsedMb + nl_160 +
		         DataMbReal + IndexMbReal+ LobMb + nl_160 +
		         ReservedPages + UsedPages + UnUsedPages + nl_160 +
		         DataPagesReal + IndexPagesReal+ LobPages + nl_160 +
		         Tables + RowCountSum + OamPages + AllocationUnits + nl_160 + 
		         DataPages + IndexPages + nl_160 +
		         DataMb + IndexMb + nl_160 +
		         "SrvPageSize = @@maxpagesize, od.BackupInProgress, od.LastBackupFailed, \n" +
		         "od.BackupStartTime, LastDbBackupAgeInHours = isnull(datediff(hour, od.BackupStartTime, getdate()),-1), \n";
		cols2 += "";
		cols3 += QuiesceTag + RawSpaceUsage + nl_160 
				+ OldestTranSqlText + OldestTranShowPlanText + OldestTranLocks;

		if (srvVersion >= Ver.ver(15,0,1) || (srvVersion >= Ver.ver(12,5,4) && srvVersion < Ver.ver(15,0)) )
		{
		}
		if (srvVersion >= Ver.ver(15,0,2,5))
		{
			cols2 += "od.LastTranLogDumpTime, LastLogBackupAgeInHours = isnull(datediff(hour, od.LastTranLogDumpTime, getdate()),-1), \n";
			cols2 += "od.LastCheckpointTime, ";
		}

		// 'database option' here, and alarm if it changes... (all status columns from sysdatabases, and decode them in Java code)
		cols4 = "DBOptions = convert(varchar(255), ''), d.status, d.status2, d.status3, d.status4, \n";
		if (srvVersion >= Ver.ver(16))
		{
			cols4 += "d.status5, minLogMode = (select object_info1 from master.dbo.sysattributes attr where attr.class = 38 and attr.attribute = 0 and attr.object_type = 'D' and attr.object = od.DBID) \n";
		}

		String cols = cols1 + cols2 + cols3 + cols4;
		cols = StringUtil.removeLastComma(cols);

		String sql = 
			"select " + cols + "\n" +
			"from master.dbo.monOpenDatabases od, master.dbo.syslogshold h, master.dbo.sysdatabases d readpast \n" +
			"where od.DBID in (select db.dbid from master.dbo.sysdatabases db readpast \n" + 
			"                  where (db.status  & 32 != 32) and (db.status  & 256 != 256)   /* 32=created with for load option, 256=suspect/not-recovered */ \n" +
			"                    and (db.status2 & 16 != 16) and (db.status2 &  32 != 32)  ) /* 16=offline, 32=offline until recovery completes */ \n" +
			"  and od.DBID *= h.dbid \n" + 
			"  and od.DBID *= d.dbid \n" + 
//			"  and h.name != '$replication_truncation_point' \n" + 
//			"  and h.name not like 'DUMP %' \n" + // DUMP TRANSACTION or DUMP DATABASE
			"  and h.xactid != 0x0 \n" + // instead of name != ...
			"order by od.DBName \n" +
			"";
		// If we don't have permission on syslogshold, then make the SELECT statement simpler
		if ( ! canDoSelectOnSyslogshold )
		{
			sql = 
				"select " + cols + "\n" +
				"from master.dbo.monOpenDatabases od, master.dbo.sysdatabases d \n" +
				"where od.DBID in (select db.dbid from master.dbo.sysdatabases db readpast \n" + 
				"                  where (db.status  & 32 != 32) and (db.status  & 256 != 256)   /* 32=created with for load option, 256=suspect/not-recovered */ \n" +
				"                    and (db.status2 & 16 != 16) and (db.status2 &  32 != 32)  ) /* 16=offline, 32=offline until recovery completes */ \n" +
				"  and od.DBID *= d.dbid \n" + 
				"order by od.DBName \n" +
				"";
		}

		return sql;
	}

	/**
	 * Called when a timeout has been found in the refreshGetData() method
	 */
	@Override
	public void handleTimeoutException()
	{
		Configuration conf = Configuration.getCombinedConfiguration();

		// FIRST try to reset timeout if it's below the default
		if (getQueryTimeout() < getDefaultQueryTimeout())
		{
			if (conf.getBooleanProperty(PROPKEY_disable_spaceusage_onTimeout, DEFAULT_disable_spaceusage_onTimeout))
			{
				setQueryTimeout(getDefaultQueryTimeout(), true);
				_logger.warn("CM='"+getName()+"'. Setting Query Timeout to default of '"+getDefaultQueryTimeout()+"', from method handelTimeoutException().");
				return;
			}
		}

		// SECONDARY Disable the: TabRowCount, NumUsedPages, RowsPerPage
		// It might be that what causing the timeout
		if (conf.getBooleanProperty(PROPKEY_disable_spaceusage_onTimeout, DEFAULT_disable_spaceusage_onTimeout))
		{
			if (conf.getBooleanProperty(PROPKEY_sample_spaceusage, DEFAULT_sample_spaceusage) == true)
			{
				// Need TMP since we are going to save the configuration somewhere
				Configuration tempConf = Configuration.getInstance(Configuration.USER_TEMP);
				if (tempConf == null) 
					return;
				tempConf.setProperty(PROPKEY_sample_spaceusage, false);
				tempConf.save();
				
				// This will force the CM to re-initialize the SQL statement.
				setSql(null);
	
				String key=PROPKEY_sample_spaceusage;
				_logger.warn("CM='"+getName()+"'. Disabling the 'spaceusage' columns, from method handelTimeoutException(). This is done by setting "+key+"=false");
				
				if (getGuiController() != null && getGuiController().hasGUI())
				{
					String dateStr = new SimpleDateFormat("yyyy-MM-dd HH:mm").format(new Date());

					JOptionPane optionPane = new JOptionPane(
							"<html>" +
							"The query for CM '"+getName()+"' took to long... and received a Timeout.<br>" +
							"<br>" +
							"This may be caused by the function spaceusage(dbid), which is used to get spaceusage on a database level.<br>" +
							"This sometimes takes to long and may eat recources.<br>" +
							"<br>" +
							"To Workaround this issue:<br>" +
							"I just disabled option 'Sample Spaceusage'... You can try to enable it again later.<br>" +
							"</html>",
							JOptionPane.INFORMATION_MESSAGE);
					JDialog dialog = optionPane.createDialog(MainFrame.getInstance(), "Disabled 'Sample Spaceusage' @ "+dateStr);
					dialog.setModal(false);
					dialog.setVisible(true);
				}
			}
		}
	}

	@Override
	public void localCalculation(CounterSample newSample)
	{
		int RawSpaceUsage_pos   = -1;
		int SrvPageSize_pos     = -1;
                                
		int ReservedMb_pos      = -1;
		int UsedMb_pos          = -1;
		int UnUsedMb_pos        = -1;
		int DataMb_pos          = -1;
		int DataMbReal_pos      = -1;
		int IndexMb_pos         = -1;
		int IndexMbReal_pos     = -1;
		int LobMb_pos           = -1;

		int ReservedPages_pos   = -1;
		int UsedPages_pos       = -1;
		int UnUsedPages_pos     = -1;
		int DataPages_pos       = -1;
		int DataPagesReal_pos   = -1;
		int IndexPages_pos      = -1;
		int IndexPagesReal_pos  = -1;
		int OamPages_pos        = -1;
		int AllocationUnits_pos = -1;
		int RowCountSum_pos     = -1;
		int Tables_pos          = -1;
		int LobPages_pos        = -1;
//		int SyslogsPages_pos    = -1;

//		boolean spaceusageInMb = Configuration.getCombinedConfiguration().getBooleanProperty(PROPKEY_spaceusageInMb, DEFAULT_spaceusageInMb);

		int LogSizeInMb_pos          = -1;
		int LogSizeUsedInMb_pos      = -1;
		int LogSizeUsedInMbDiff_pos  = -1;
		int LogSizeFreeInMb_pos      = -1;
		int LogSizeFreeInMbDiff_pos  = -1;
		int LogSizeUsedPct_pos       = -1;

		int DataSizeInMb_pos         = -1;
		int DataSizeUsedInMb_pos     = -1;
		int DataSizeUsedInMbDiff_pos = -1;
		int DataSizeFreeInMb_pos     = -1;
		int DataSizeFreeInMbDiff_pos = -1;
		int DataSizeUsedPct_pos      = -1;

		int pos_OldestTranSpid         = -1; 
		int pos_OldestTranHasSqlText   = -1; 
		int pos_OldestTranHasShowPlan  = -1; 
		int pos_OldestTranHasLocks     = -1; 
		int pos_OldestTranSqlText      = -1;
		int pos_OldestTranShowPlanText = -1;
		int pos_OldestTranLocks        = -1;
		
		int pos_DBName     = -1;
		int pos_DBOptions  = -1;
		int pos_status     = -1;
		int pos_status2    = -1;
		int pos_status3    = -1;
		int pos_status4    = -1;
		int pos_status5    = -1;
		int pos_minLogMode = -1;

		// Find column Id's
		List<String> colNames = newSample.getColNames();
		if (colNames == null)
			return;

		for (int colId=0; colId < colNames.size(); colId++) 
		{
			String colName = colNames.get(colId);
			if      (colName.equals("RawSpaceUsage"))        RawSpaceUsage_pos        = colId;
			else if (colName.equals("SrvPageSize"))          SrvPageSize_pos          = colId;

			else if (colName.equals("ReservedMb"))           ReservedMb_pos           = colId;
			else if (colName.equals("UsedMb"))               UsedMb_pos               = colId;
			else if (colName.equals("UnUsedMb"))             UnUsedMb_pos             = colId;
			else if (colName.equals("DataMb"))               DataMb_pos               = colId;
			else if (colName.equals("DataMbReal"))           DataMbReal_pos           = colId;
			else if (colName.equals("IndexMb"))              IndexMb_pos              = colId;
			else if (colName.equals("IndexMbReal"))          IndexMbReal_pos          = colId;
			else if (colName.equals("LobMb"))                LobMb_pos                = colId;

			else if (colName.equals("ReservedPages"))        ReservedPages_pos        = colId;
			else if (colName.equals("UsedPages"))            UsedPages_pos            = colId;
			else if (colName.equals("UnUsedPages"))          UnUsedPages_pos          = colId;
			else if (colName.equals("DataPages"))            DataPages_pos            = colId;
			else if (colName.equals("DataPagesReal"))        DataPagesReal_pos        = colId;
			else if (colName.equals("IndexPages"))           IndexPages_pos           = colId;
			else if (colName.equals("IndexPagesReal"))       IndexPagesReal_pos       = colId;
			else if (colName.equals("OamPages"))             OamPages_pos             = colId;
			else if (colName.equals("AllocationUnits"))      AllocationUnits_pos      = colId;
			else if (colName.equals("RowCountSum"))          RowCountSum_pos          = colId;
			else if (colName.equals("Tables"))               Tables_pos               = colId;
			else if (colName.equals("LobPages"))             LobPages_pos             = colId;
//			else if (colName.equals("SyslogsPages"))         SyslogsPages_pos         = colId;

			else if (colName.equals("LogSizeInMb"))          LogSizeInMb_pos          = colId;
			else if (colName.equals("LogSizeUsedInMb"))      LogSizeUsedInMb_pos      = colId;
			else if (colName.equals("LogSizeUsedInMbDiff"))  LogSizeUsedInMbDiff_pos  = colId;
			else if (colName.equals("LogSizeFreeInMb"))      LogSizeFreeInMb_pos      = colId;
			else if (colName.equals("LogSizeFreeInMbDiff"))  LogSizeFreeInMbDiff_pos  = colId;
			else if (colName.equals("LogSizeUsedPct"))       LogSizeUsedPct_pos       = colId;

			else if (colName.equals("DataSizeInMb"))         DataSizeInMb_pos         = colId;
			else if (colName.equals("DataSizeUsedInMb"))     DataSizeUsedInMb_pos     = colId;
			else if (colName.equals("DataSizeUsedInMbDiff")) DataSizeUsedInMbDiff_pos = colId;
			else if (colName.equals("DataSizeFreeInMb"))     DataSizeFreeInMb_pos     = colId;
			else if (colName.equals("DataSizeFreeInMbDiff")) DataSizeFreeInMbDiff_pos = colId;
			else if (colName.equals("DataSizeUsedPct"))      DataSizeUsedPct_pos      = colId;

			else if (colName.equals("OldestTranSpid"))         pos_OldestTranSpid         = colId;
			else if (colName.equals("OldestTranHasSqlText"))   pos_OldestTranHasSqlText   = colId;
			else if (colName.equals("OldestTranHasShowPlan"))  pos_OldestTranHasShowPlan  = colId;
			else if (colName.equals("OldestTranHasLocks"))     pos_OldestTranHasLocks     = colId;
			else if (colName.equals("OldestTranSqlText"))      pos_OldestTranSqlText      = colId;
			else if (colName.equals("OldestTranShowPlanText")) pos_OldestTranShowPlanText = colId;
			else if (colName.equals("OldestTranLocks"))        pos_OldestTranLocks        = colId;

			else if (colName.equals("DBName"))                 pos_DBName         = colId;
			else if (colName.equals("DBOptions"))              pos_DBOptions      = colId;
			else if (colName.equals("status"))                 pos_status         = colId;
			else if (colName.equals("status2"))                pos_status2        = colId;
			else if (colName.equals("status3"))                pos_status3        = colId;
			else if (colName.equals("status4"))                pos_status4        = colId;
			else if (colName.equals("status5"))                pos_status5        = colId;
			else if (colName.equals("minLogMode"))             pos_minLogMode     = colId;
		}

		// Loop on all rows
		for (int rowId = 0; rowId < newSample.getRowCount(); rowId++)
		{
			// If 'RawSpaceUsage' was found, Only in ASE 16.0 or above
			if (RawSpaceUsage_pos >= 0)
			{
				String RawSpaceUsage = newSample.getValueAt(rowId, RawSpaceUsage_pos)+"";
				
				// Split up the RawSpaceUsage, which looks looks the following:
				// 'reserved pages=4190, used pages=3117, data pages=2985, index pages=456, oam pages=132, allocation units=181, row count=21771, tables=57, LOB pages=15, syslogs pages=0'

//				Map<String, String> map = StringUtil.parseCommaStrToMap(RawSpaceUsage);
//				if (_logger.isDebugEnabled())
//					_logger.debug("RawSpaceUsage(spaceusageInMb="+spaceusageInMb+"): "+map);
//				
//				if (spaceusageInMb)
//				{
//					int SrvPageSize = ((Number)newSample.getValueAt(rowId, SrvPageSize_pos)).intValue();
//					int divideBy = 1024*1024/SrvPageSize;
//					
//					long UnUsedPages    = (StringUtil.parseLong(map.get("reserved pages"),0) - StringUtil.parseLong(map.get("used pages") ,0)) /divideBy;
//					long DataPagesReal  = (StringUtil.parseLong(map.get("data pages")    ,0) - StringUtil.parseLong(map.get("index pages"),0)) /divideBy;
//					long IndexPagesReal = (StringUtil.parseLong(map.get("index pages")   ,0) - StringUtil.parseLong(map.get("LOB pages")  ,0)) /divideBy;
//
//					newSample.setValueAt(UnUsedPages,    rowId, UnUsedPages_pos);
//					newSample.setValueAt(DataPagesReal,  rowId, DataPagesReal_pos);
//					newSample.setValueAt(IndexPagesReal, rowId, IndexPagesReal_pos);
//
//					newSample.setValueAt(StringUtil.parseLong(map.get("reserved pages"),   0)/divideBy, rowId, ReservedPages_pos);
//					newSample.setValueAt(StringUtil.parseLong(map.get("used pages"),       0)/divideBy, rowId, UsedPages_pos);
//					newSample.setValueAt(StringUtil.parseLong(map.get("data pages"),       0)/divideBy, rowId, DataPages_pos);
//					newSample.setValueAt(StringUtil.parseLong(map.get("index pages"),      0)/divideBy, rowId, IndexPages_pos);
//					newSample.setValueAt(StringUtil.parseLong(map.get("oam pages"),        0)         , rowId, OamPages_pos);
//					newSample.setValueAt(StringUtil.parseLong(map.get("allocation units"), 0)         , rowId, AllocationUnits_pos);
//					newSample.setValueAt(StringUtil.parseLong(map.get("row count"),        0)         , rowId, RowCountSum_pos);
//					newSample.setValueAt(StringUtil.parseLong(map.get("tables"),           0)         , rowId, Tables_pos);
//					newSample.setValueAt(StringUtil.parseLong(map.get("LOB pages"),        0)/divideBy, rowId, LobPages_pos);
////					newSample.setValueAt(StringUtil.parseLong(map.get("syslogs pages"),    0)/divideBy, rowId, SyslogsPages_pos);
//				}
//				else
//				{
//					long UnUsedPages    = StringUtil.parseLong(map.get("reserved pages"),0) - StringUtil.parseLong(map.get("used pages") ,0);
//					long DataPagesReal  = StringUtil.parseLong(map.get("data pages")    ,0) - StringUtil.parseLong(map.get("index pages"),0);
//					long IndexPagesReal = StringUtil.parseLong(map.get("index pages")   ,0) - StringUtil.parseLong(map.get("LOB pages")  ,0);
//
//					newSample.setValueAt(UnUsedPages,    rowId, UnUsedPages_pos);
//					newSample.setValueAt(DataPagesReal,  rowId, DataPagesReal_pos);
//					newSample.setValueAt(IndexPagesReal, rowId, IndexPagesReal_pos);
//
//					newSample.setValueAt(StringUtil.parseLong(map.get("reserved pages"),   0), rowId, ReservedPages_pos);
//					newSample.setValueAt(StringUtil.parseLong(map.get("used pages"),       0), rowId, UsedPages_pos);
//					newSample.setValueAt(StringUtil.parseLong(map.get("data pages"),       0), rowId, DataPages_pos);
//					newSample.setValueAt(StringUtil.parseLong(map.get("index pages"),      0), rowId, IndexPages_pos);
//					newSample.setValueAt(StringUtil.parseLong(map.get("oam pages"),        0), rowId, OamPages_pos);
//					newSample.setValueAt(StringUtil.parseLong(map.get("allocation units"), 0), rowId, AllocationUnits_pos);
//					newSample.setValueAt(StringUtil.parseLong(map.get("row count"),        0), rowId, RowCountSum_pos);
//					newSample.setValueAt(StringUtil.parseLong(map.get("tables"),           0), rowId, Tables_pos);
//					newSample.setValueAt(StringUtil.parseLong(map.get("LOB pages"),        0), rowId, LobPages_pos);
////					newSample.setValueAt(StringUtil.parseLong(map.get("syslogs pages"),    0), rowId, SyslogsPages_pos);
//				}

				Map<String, String> map = StringUtil.parseCommaStrToMap(RawSpaceUsage);
				
				int SrvPageSize = ((Number)newSample.getValueAt(rowId, SrvPageSize_pos)).intValue();
				double divideBy = 1024*1024/SrvPageSize;

				long UnUsedPages    = StringUtil.parseLong(map.get("reserved pages"),0) - StringUtil.parseLong(map.get("used pages") ,0);
				long DataPagesReal  = StringUtil.parseLong(map.get("data pages")    ,0) - StringUtil.parseLong(map.get("index pages"),0);
				long IndexPagesReal = StringUtil.parseLong(map.get("index pages")   ,0) - StringUtil.parseLong(map.get("LOB pages")  ,0);

				// Set MB
				newSample.setValueAt(new BigDecimal( UnUsedPages   /divideBy ).setScale(3, BigDecimal.ROUND_HALF_UP), rowId, UnUsedMb_pos);
				newSample.setValueAt(new BigDecimal( DataPagesReal /divideBy ).setScale(3, BigDecimal.ROUND_HALF_UP), rowId, DataMbReal_pos);
				newSample.setValueAt(new BigDecimal( IndexPagesReal/divideBy ).setScale(3, BigDecimal.ROUND_HALF_UP), rowId, IndexMbReal_pos);

				newSample.setValueAt(new BigDecimal( StringUtil.parseLong(map.get("reserved pages"),   0)/divideBy ).setScale(3, BigDecimal.ROUND_HALF_UP), rowId, ReservedMb_pos);
				newSample.setValueAt(new BigDecimal( StringUtil.parseLong(map.get("used pages"),       0)/divideBy ).setScale(3, BigDecimal.ROUND_HALF_UP), rowId, UsedMb_pos);
				newSample.setValueAt(new BigDecimal( StringUtil.parseLong(map.get("data pages"),       0)/divideBy ).setScale(3, BigDecimal.ROUND_HALF_UP), rowId, DataMb_pos);
				newSample.setValueAt(new BigDecimal( StringUtil.parseLong(map.get("index pages"),      0)/divideBy ).setScale(3, BigDecimal.ROUND_HALF_UP), rowId, IndexMb_pos);
				newSample.setValueAt(new BigDecimal( StringUtil.parseLong(map.get("LOB pages"),        0)/divideBy ).setScale(3, BigDecimal.ROUND_HALF_UP), rowId, LobMb_pos);

				// Set Pages
				newSample.setValueAt(UnUsedPages,    rowId, UnUsedPages_pos);
				newSample.setValueAt(DataPagesReal,  rowId, DataPagesReal_pos);
				newSample.setValueAt(IndexPagesReal, rowId, IndexPagesReal_pos);

				newSample.setValueAt(StringUtil.parseLong(map.get("reserved pages"),   0), rowId, ReservedPages_pos);
				newSample.setValueAt(StringUtil.parseLong(map.get("used pages"),       0), rowId, UsedPages_pos);
				newSample.setValueAt(StringUtil.parseLong(map.get("data pages"),       0), rowId, DataPages_pos);
				newSample.setValueAt(StringUtil.parseLong(map.get("index pages"),      0), rowId, IndexPages_pos);
				newSample.setValueAt(StringUtil.parseLong(map.get("oam pages"),        0), rowId, OamPages_pos);
				newSample.setValueAt(StringUtil.parseLong(map.get("allocation units"), 0), rowId, AllocationUnits_pos);
				newSample.setValueAt(StringUtil.parseLong(map.get("row count"),        0), rowId, RowCountSum_pos);
				newSample.setValueAt(StringUtil.parseLong(map.get("tables"),           0), rowId, Tables_pos);
				newSample.setValueAt(StringUtil.parseLong(map.get("LOB pages"),        0), rowId, LobPages_pos);
//				newSample.setValueAt(StringUtil.parseLong(map.get("syslogs pages"),    0), rowId, SyslogsPages_pos);
			}
			

			// Data/Log Size
			int    oval_LogSizeInMb      = ((Number)newSample.getValueAt(rowId, LogSizeInMb_pos     )).intValue();
			double oval_LogSizeFreeInMb  = ((Number)newSample.getValueAt(rowId, LogSizeFreeInMb_pos )).doubleValue();
			int    oval_DataSizeInMb     = ((Number)newSample.getValueAt(rowId, DataSizeInMb_pos    )).intValue();
			double oval_DataSizeFreeInMb = ((Number)newSample.getValueAt(rowId, DataSizeFreeInMb_pos)).doubleValue();

			// COLUMN: LogSizeUsedInMb, LogSizeUsedInMbDiff, LogSizeFreeInMbDiff
			{
				double calc_val = oval_LogSizeInMb - oval_LogSizeFreeInMb;
				
				BigDecimal newVal = new BigDecimal(calc_val).setScale(1, BigDecimal.ROUND_HALF_EVEN);
				newSample.setValueAt(newVal, rowId, LogSizeUsedInMb_pos);
				newSample.setValueAt(newVal, rowId, LogSizeUsedInMbDiff_pos);
				
				newVal = new BigDecimal(oval_LogSizeFreeInMb).setScale(1, BigDecimal.ROUND_HALF_EVEN);
				newSample.setValueAt(newVal, rowId, LogSizeFreeInMbDiff_pos);
			}
			
			// COLUMN: DataSizeUsedInMb, DataSizeUsedInMbDiff, DataSizeFreeInMbDiff
			{
				double calc_val = oval_DataSizeInMb - oval_DataSizeFreeInMb;
				
				BigDecimal newVal = new BigDecimal(calc_val).setScale(1, BigDecimal.ROUND_HALF_EVEN);
				newSample.setValueAt(newVal, rowId, DataSizeUsedInMb_pos);
				newSample.setValueAt(newVal, rowId, DataSizeUsedInMbDiff_pos);

				newVal = new BigDecimal(oval_DataSizeFreeInMb).setScale(1, BigDecimal.ROUND_HALF_EVEN);
				newSample.setValueAt(newVal, rowId, DataSizeFreeInMbDiff_pos);
			}

			// COLUMN: LogSizeUsedPct
			if (oval_LogSizeInMb > 0) // I doubt that oval_LogSizeInMb can be 0
			{
				// Formula: 
				double calc_val = 100.0 - (((oval_LogSizeFreeInMb + 0.0) / oval_LogSizeInMb) * 100.0);
				if (calc_val < 0.0)
					calc_val = 0.0;

				BigDecimal newVal = new BigDecimal(calc_val).setScale(1, BigDecimal.ROUND_HALF_EVEN);
				newSample.setValueAt(newVal, rowId, LogSizeUsedPct_pos);
			}
			else
				newSample.setValueAt(new BigDecimal(0).setScale(1, BigDecimal.ROUND_HALF_EVEN), rowId, LogSizeUsedPct_pos);

			// COLUMN: DataSizeUsedPct
			if (oval_DataSizeInMb > 0) // I doubt that oval_DataSizeInMb can be 0
			{
				// Formula: 
				double calc_val = 100.0 - (((oval_DataSizeFreeInMb + 0.0) / oval_DataSizeInMb) * 100.0);
				if (calc_val < 0.0)
					calc_val = 0.0;

				BigDecimal newVal = new BigDecimal(calc_val).setScale(1, BigDecimal.ROUND_HALF_EVEN);
				newSample.setValueAt(newVal, rowId, DataSizeUsedPct_pos);
			}
			else
				newSample.setValueAt(new BigDecimal(0).setScale(1, BigDecimal.ROUND_HALF_EVEN), rowId, DataSizeUsedPct_pos);

		
//			OldestTranHasSqlText
//			OldestTranHasShowPlan
//			OldestTranSqlText
//			OldestTranShowPlanText
//			OldestTranShowLocks
			Object oval_OldestTranSpid = newSample.getValueAt(rowId, pos_OldestTranSpid);
			if (oval_OldestTranSpid != null && oval_OldestTranSpid instanceof Number)
			{
				Configuration conf = Configuration.getCombinedConfiguration();
				boolean getShowplan   = conf.getBooleanProperty(PROPKEY_sample_showplan,   DEFAULT_sample_showplan);
				boolean getMonSqltext = conf.getBooleanProperty(PROPKEY_sample_monSqlText, DEFAULT_sample_monSqlText);
				boolean getLocks      = conf.getBooleanProperty(PROPKEY_sample_locks,   DEFAULT_sample_locks);

				int OldestTranSpid = ((Number)newSample.getValueAt(rowId, pos_OldestTranSpid)).intValue();

				if (OldestTranSpid > 0) // NULL result from the ASE is translated as 0... so lets not hope that the SPID 0 has issues.
				{
					String sqlText  = "Not properly configured (need 'SQL batch capture' & 'max SQL text monitored').";
					String showplan = "User does not have: sa_role";
					String sysLocks = "This was disabled";

					// OldestTranSqlText
					// if ASE is 16.0 we might already got the SQL Text using query_text(h.spid)
					String curSqlText = (String) newSample.getValueAt(rowId, pos_OldestTranSqlText);
					if (curSqlText != null)
					{
						sqlText = curSqlText;
					}
					else
					{
						if (getMonitorConfig("SQL batch capture") > 0 && getMonitorConfig("max SQL text monitored") > 0)
						{
							// monProcessSQLText; needs 'enable monitoring', 'SQL batch capture' and 'max SQL text monitored' configuration parameters for this monitoring table to collect data.
							if (getMonSqltext)
								sqlText  = AseConnectionUtils.monSqlText(getCounterController().getMonConnection(), OldestTranSpid, true);
							else
								sqlText = "This was disabled";
							if (sqlText == null)
								sqlText = "Not Available";
						}
					}
					
					if (isServerRoleOrPermissionActive(AseConnectionUtils.SA_ROLE))
					{
						if (getShowplan)
							showplan = AseConnectionUtils.getShowplan(getCounterController().getMonConnection(), OldestTranSpid, "Showplan:", true);
						else
							showplan = "This was disabled";
						if (showplan == null || (showplan != null && showplan.matches(".*The query plan for spid '.*' is unavailable.*")) )
							showplan = "Not Available";
					}
					
					if (getLocks)
					{
//						sysLocks  = AseConnectionUtils.getLockSummaryForSpid(getCounterController().getMonConnection(), OldestTranSpid, false, false);
						sysLocks  = AseConnectionUtils.getLockSummaryForSpid(getCounterController().getMonConnection(), OldestTranSpid, true, false);
						if (sysLocks == null)
							sysLocks = "Not Available";
					}

					// Set the values: *Has* and *Text*
					boolean b = true;
					b = !"This was disabled".equals(sqlText)  && !"Not Available".equals(sqlText)  && !sqlText .startsWith("Not properly configured");
					newSample.setValueAt(new Boolean(b), rowId, pos_OldestTranHasSqlText);
					newSample.setValueAt(sqlText,        rowId, pos_OldestTranSqlText);

					b = !"This was disabled".equals(showplan) && !"Not Available".equals(showplan) && !showplan.startsWith("User does not have");
					newSample.setValueAt(new Boolean(b), rowId, pos_OldestTranHasShowPlan);
					newSample.setValueAt(showplan,       rowId, pos_OldestTranShowPlanText);

					b = !"This was disabled".equals(sysLocks) && !"Not Available".equals(sysLocks);
					newSample.setValueAt(new Boolean(b), rowId, pos_OldestTranHasLocks);
					newSample.setValueAt(sysLocks,       rowId, pos_OldestTranLocks);
				}
			}

			// DB Options
			if (pos_DBOptions != -1)
			{
				String dbname  = newSample.getValueAsString(rowId, pos_DBName);
				int status     = pos_status      == -1 ? 0 : newSample.getValueAsInteger(rowId, pos_status);
				int status2    = pos_status2     == -1 ? 0 : newSample.getValueAsInteger(rowId, pos_status2);
				int status3    = pos_status3     == -1 ? 0 : newSample.getValueAsInteger(rowId, pos_status3);
				int status4    = pos_status4     == -1 ? 0 : newSample.getValueAsInteger(rowId, pos_status4);
				int status5    = pos_status5     == -1 ? 0 : newSample.getValueAsInteger(rowId, pos_status5);
				int minLogMode = pos_minLogMode  == -1 ? 0 : newSample.getValueAsInteger(rowId, pos_minLogMode);

				// Decode "statusX" columns into a List 
				List<String> dbOptionsList = AseConnectionUtils.decodeSysDatabasesStatus(dbname, status, status2, status3, status4, status5, minLogMode);
				if (dbOptionsList != null && !dbOptionsList.isEmpty())
					newSample.setValueAt(StringUtil.toCommaStr(dbOptionsList), rowId, pos_DBOptions);
			}
			
			
		} // end: Loop on all rows
	}
	
	/** 
	 * Compute the AppendLogContPct for DIFF values
	 */
	@Override
	public void localCalculation(CounterSample prevSample, CounterSample newSample, CounterSample diffData)
	{
		int AppendLogRequests,         AppendLogWaits;
		int AppendLogRequestsId  = -1, AppendLogWaitsId     = -1;

//		int TransactionLogFull   = 0,  SuspendedProcesses   = 0;
//		int TransactionLogFullId = -1, SuspendedProcessesId = -1;

		double calcAppendLogContPct;
		int AppendLogContPctId = -1;

		// Find column Id's
		List<String> colNames = diffData.getColNames();
		if (colNames == null)
			return;

		for (int colId=0; colId < colNames.size(); colId++) 
		{
			String colName = colNames.get(colId);
			if      (colName.equals("AppendLogContPct"))     AppendLogContPctId       = colId;
			else if (colName.equals("AppendLogRequests"))    AppendLogRequestsId      = colId;
			else if (colName.equals("AppendLogWaits"))       AppendLogWaitsId         = colId;
		}

		// Loop on all diffData rows
		for (int rowId = 0; rowId < diffData.getRowCount(); rowId++)
		{
			AppendLogRequests     = ((Number)diffData.getValueAt(rowId, AppendLogRequestsId )).intValue();
			AppendLogWaits        = ((Number)diffData.getValueAt(rowId, AppendLogWaitsId    )).intValue();

			// COLUMN: AppendLogContPct
			if (AppendLogRequests > 0)
			{
				// Formula: AppendLogContPct = (AppendLogWaits / AppendLogRequests) * 100;
				calcAppendLogContPct = ((AppendLogWaits + 0.0) / AppendLogRequests) * 100.0;

				BigDecimal newVal = new BigDecimal(calcAppendLogContPct).setScale(2, BigDecimal.ROUND_HALF_EVEN);
				diffData.setValueAt(newVal, rowId, AppendLogContPctId);
			}
			else
				diffData.setValueAt(new BigDecimal(0).setScale(2, BigDecimal.ROUND_HALF_EVEN), rowId, AppendLogContPctId);
		}
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
		
		if (GRAPH_NAME_LOGSEMAPHORE_CONT.equals(tgdp.getName()))
		{
			// Write 1 "line" for every database
			Double[] dArray = new Double[dbMap.size()];
			String[] lArray = new String[dbMap.size()];
			int d = 0;
			for (int row : dbMap.values())
			{
				String dbname = this.getAbsString        (row, "DBName");
				Double dvalue = this.getDiffValueAsDouble(row, "AppendLogContPct");

				lArray[d] = dbname;
				dArray[d] = dvalue;
				d++;
			}

			// Set the values
			tgdp.setDataPoint(this.getTimestamp(), lArray, dArray);
//			tgdp.setDate(this.getTimestamp());
//			tgdp.setLabel(lArray);
//			tgdp.setData(dArray);
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
					return (String) cellVal;
			}
		}
		if ("OldestTranShowPlanText".equals(colName))
		{
			return cellValue == null ? null : cellValue.toString();
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
	public void sendAlarmRequest()
	{
		if ( ! hasDiffData() )
			return;
		
		if ( ! AlarmHandler.hasInstance() )
			return;

		AlarmHandler alarmHandler = AlarmHandler.getInstance();
		
		CountersModel cm = this;
		String dbmsSrvName = cm.getServerName();

		boolean debugPrint = Configuration.getCombinedConfiguration().getBooleanProperty("sendAlarmRequest.debug", _logger.isDebugEnabled());

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

			
			//-------------------------------------------------------
			// Full transaction log
			//-------------------------------------------------------
			if (isSystemAlarmsForColumnEnabledAndInTimeRange("TransactionLogFull"))
			{
				Double TransactionLogFull = cm.getAbsValueAsDouble(r, "TransactionLogFull");
				if (TransactionLogFull != null)
				{
					int threshold = Configuration.getCombinedConfiguration().getIntProperty(PROPKEY_alarm_TransactionLogFull, DEFAULT_alarm_TransactionLogFull);

					if (debugPrint || _logger.isDebugEnabled())
						System.out.println("##### sendAlarmRequest("+cm.getName()+"): dbname='"+dbname+"', threshold="+threshold+", TransactionLogFull='"+TransactionLogFull+"'.");

					if (TransactionLogFull.intValue() > threshold)
					{
						String extendedDescText = cm.toTextTableString(DATA_RATE, r);
						String extendedDescHtml = cm.toHtmlTableString(DATA_RATE, r, true, false, false);
						extendedDescHtml += "<br><br>" + cm.getGraphDataHistoryAsHtmlImage(GRAPH_NAME_LOGSIZE_LEFT_MB, dbname);
						extendedDescHtml += "<br><br>" + cm.getGraphDataHistoryAsHtmlImage(GRAPH_NAME_LOGSIZE_USED_PCT, dbname);

						AlarmEvent ae = new AlarmEventFullTranLog(cm, threshold, dbname, null);
						ae.setExtendedDescription(extendedDescText, extendedDescHtml);
						
						alarmHandler.addAlarm( ae );
						//alarmHandler.addAlarm( new AlarmEventFullTranLog(cm, threshold, dbname) );
					}
				}
			}


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


			//-------------------------------------------------------
			// LastBackupFailed
			//-------------------------------------------------------
			if (isSystemAlarmsForColumnEnabledAndInTimeRange("LastBackupFailed"))
			{
				Double val = cm.getAbsValueAsDouble(r, "LastBackupFailed");
				if (val != null)
				{
					int threshold = Configuration.getCombinedConfiguration().getIntProperty(PROPKEY_alarm_LastBackupFailed, DEFAULT_alarm_LastBackupFailed);

					if (debugPrint || _logger.isDebugEnabled())
						System.out.println("##### sendAlarmRequest("+cm.getName()+"): dbname='"+dbname+"', threshold="+threshold+", LastBackupFailed='"+val+"'.");

					if (val.intValue() > threshold)
					{
						String extendedDescText = cm.toTextTableString(DATA_RATE, r);
						String extendedDescHtml = cm.toHtmlTableString(DATA_RATE, r, true, false, false);

						AlarmEvent ae = new AlarmEventLastBackupFailed(cm, dbname, threshold);
						ae.setExtendedDescription(extendedDescText, extendedDescHtml);
						
						alarmHandler.addAlarm( ae );
					}
				}
			}

			//-------------------------------------------------------
			// LastDbBackupAgeInHours
			//-------------------------------------------------------
			if (isSystemAlarmsForColumnEnabledAndInTimeRange("LastDbBackupAgeInHours"))
			{
				Double val = cm.getAbsValueAsDouble(r, "LastDbBackupAgeInHours");
				if (val == null)
					val = -1.0;

				// If we reboot we will get -1 for all databases, so we might kick off some faulty alarms.
				// So lets get when ASE was restarted (get info from CmSummary) then set the LastDbBackupAgeInHours to that value
				// But only do it for the 24 first hours since reboot, if it was a faulty assumption
				if (val == -1.0)
				{
					String aseStartDateStr = "";
					try 
					{
						aseStartDateStr = cm.getCounterController().getSummaryCm().getAbsString(0, "StartDate");

						SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd hh:mm:ss.SSS");
						Date aseStartDate = dateFormat.parse(aseStartDateStr);
						long hoursSinceAseRestart = (System.currentTimeMillis() - aseStartDate.getTime()) / 1000 / 60 / 60;

						if (hoursSinceAseRestart <= 24)
						{
							val = new Double(hoursSinceAseRestart);

							// Not ethe extra space >>>>>>>>>_<<<<< here: this so it lines up with below 'LOG' which will probably also be printed in the log.
							_logger.info("It looks like no DB  Backups has been taken yet (maybe ASE was recently restarted). 'LastDbBackupAgeInHours' was null or -1. hoursSinceAseRestart=" + hoursSinceAseRestart + ", setting LastDbBackupAgeInHours=" + val + " for database '" + dbname + "'.");
						}
					}
					catch(Exception e) 
					{
						_logger.info("Possible that ASE just restarted, but I couldn't parse the ASE Start date '" + aseStartDateStr + "', so the 'LastDbBackupAgeInHours' will be kept at '" + val + "', which may produce an faulty/inproper Alarm...");
					}
				}
				
				
				int threshold = Configuration.getCombinedConfiguration().getIntProperty(PROPKEY_alarm_LastDbBackupAgeInHours, DEFAULT_alarm_LastDbBackupAgeInHours);

				if (debugPrint || _logger.isDebugEnabled())
					System.out.println("##### sendAlarmRequest("+cm.getName()+"): dbname='"+dbname+"', threshold="+threshold+", LastDbBackupAgeInHours='"+val+"'.");

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
						String lastBackupDate = cm.getAbsValue(r, "BackupStartTime") + "";
	
						String extendedDescText = cm.toTextTableString(DATA_RATE, r);
						String extendedDescHtml = cm.toHtmlTableString(DATA_RATE, r, true, false, false);

						AlarmEvent ae = new AlarmEventOldBackup(cm, threshold, dbname, lastBackupDate, val.intValue());
						ae.setExtendedDescription(extendedDescText, extendedDescHtml);
						
						alarmHandler.addAlarm( ae );
					}
				}

//				if (val < 0)
//				{
//					// Alarm on NO DATA backup?
//					fixme
//				}
//				else
//				{
//					int threshold = Configuration.getCombinedConfiguration().getIntProperty(PROPKEY_alarm_LastDbBackupAgeInHours, DEFAULT_alarm_LastDbBackupAgeInHours);
//					if (val.intValue() > threshold)
//					{
//						// Get config 'skip some transaction names'
//						String keepDbRegExp  = Configuration.getCombinedConfiguration().getProperty(PROPKEY_alarm_LastDbBackupAgeInHoursForDbs,  DEFAULT_alarm_LastDbBackupAgeInHoursForDbs);
//						String skipDbRegExp  = Configuration.getCombinedConfiguration().getProperty(PROPKEY_alarm_LastDbBackupAgeInHoursSkipDbs, DEFAULT_alarm_LastDbBackupAgeInHoursSkipDbs);
//						String keepSrvRegExp = Configuration.getCombinedConfiguration().getProperty(PROPKEY_alarm_LastDbBackupAgeInHoursForSrv,  DEFAULT_alarm_LastDbBackupAgeInHoursForSrv);
//						String skipSrvRegExp = Configuration.getCombinedConfiguration().getProperty(PROPKEY_alarm_LastDbBackupAgeInHoursSkipSrv, DEFAULT_alarm_LastDbBackupAgeInHoursSkipSrv);
//
//						// The below could have been done with neasted if(keep-db), if(keep-srv), if(!skipDb), if(!skipSrv) doAlarm=true; 
//						// Below is more readable, from a variable context point-of-view, but HARDER to understand
//						boolean doAlarm = true; // note: this must be set to true at start, otherwise all below rules will be disabled (it "stops" processing at first doAlarm==false)
//						doAlarm = (doAlarm && (StringUtil.isNullOrBlank(keepDbRegExp)  ||   dbname     .matches(keepDbRegExp ))); //     matches the KEEP Db  regexp
//						doAlarm = (doAlarm && (StringUtil.isNullOrBlank(keepSrvRegExp) ||   dbmsSrvName.matches(keepSrvRegExp))); //     matches the KEEP Srv regexp
//						doAlarm = (doAlarm && (StringUtil.isNullOrBlank(skipDbRegExp)  || ! dbname     .matches(skipDbRegExp ))); // NO match in the SKIP Db  regexp
//						doAlarm = (doAlarm && (StringUtil.isNullOrBlank(skipSrvRegExp) || ! dbmsSrvName.matches(skipSrvRegExp))); // NO match in the SKIP Srv regexp
//
//						// NO match in the SKIP regexp
//						if (doAlarm)
//						{
//							alarmHandler.addAlarm( new AlarmEventOldBackup(cm, threshold, "DB", dbname, val.intValue()) );
//						}
//					}
//				}
			}

			//-------------------------------------------------------
			// LastLogBackupAgeInHours
			//-------------------------------------------------------
			if (isSystemAlarmsForColumnEnabledAndInTimeRange("LastLogBackupAgeInHours"))
			{
				Double val = cm.getAbsValueAsDouble(r, "LastLogBackupAgeInHours");
				if (val == null)
					val = -1.0;
				
				// If we reboot we will get -1 for all databases, so we might kick off some faulty alarms.
				// So lets get when ASE was restarted (get info from CmSummary) then set the LastLogBackupAgeInHours to that value
				// But only do it for the 24 first hours since reboot, if it was a faulty assumption
				if (val == -1.0)
				{
					String aseStartDateStr = "";
					try 
					{
						aseStartDateStr = cm.getCounterController().getSummaryCm().getAbsString(0, "StartDate");

						SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd hh:mm:ss.SSS");
						Date aseStartDate = dateFormat.parse(aseStartDateStr);
						long hoursSinceAseRestart = (System.currentTimeMillis() - aseStartDate.getTime()) / 1000 / 60 / 60;

						if (hoursSinceAseRestart <= 24)
						{
    						val = new Double(hoursSinceAseRestart);
    
    						_logger.info("It looks like no LOG Backups has been taken yet (maybe ASE was recently restarted). 'LastLogBackupAgeInHours' was null or -1. hoursSinceAseRestart=" + hoursSinceAseRestart + ", setting LastLogBackupAgeInHours=" + val + " for database '" + dbname + "'.");
						}
					}
					catch(Exception e) 
					{
						_logger.info("Possible that ASE just restarted, but I couldn't parse the ASE Start date '" + aseStartDateStr + "', so the 'LastLogBackupAgeInHours' will be kept at '" + val + "', which may produce an faulty/inproper Alarm...");
					}
				}

				int threshold = Configuration.getCombinedConfiguration().getIntProperty(PROPKEY_alarm_LastLogBackupAgeInHours, DEFAULT_alarm_LastLogBackupAgeInHours);

				if (debugPrint || _logger.isDebugEnabled())
					System.out.println("##### sendAlarmRequest("+cm.getName()+"): dbname='"+dbname+"', threshold="+threshold+", LastLogBackupAgeInHours='"+val+"'.");

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
						String lastBackupDate = cm.getAbsValue(r, "LastTranLogDumpTime") + "";

						String extendedDescText = cm.toTextTableString(DATA_RATE, r);
						String extendedDescHtml = cm.toHtmlTableString(DATA_RATE, r, true, false, false);
						
						AlarmEvent ae = new AlarmEventOldTranLogBackup(cm, threshold, dbname, lastBackupDate, val.intValue());
						ae.setExtendedDescription(extendedDescText, extendedDescHtml);
						
						alarmHandler.addAlarm( ae );
					}
				}

//				if (val < 0)
//				{
//					// Alarm on NO LOG backup?
//					fixme
//				}
//				else
//				{
//					int threshold = Configuration.getCombinedConfiguration().getIntProperty(PROPKEY_alarm_LastLogBackupAgeInHours, DEFAULT_alarm_LastLogBackupAgeInHours);
//					if (val.intValue() > threshold)
//					{
//						// Get config 'skip some transaction names'
//						String keepDbRegExp  = Configuration.getCombinedConfiguration().getProperty(PROPKEY_alarm_LastLogBackupAgeInHoursForDbs,  DEFAULT_alarm_LastLogBackupAgeInHoursForDbs);
//						String skipDbRegExp  = Configuration.getCombinedConfiguration().getProperty(PROPKEY_alarm_LastLogBackupAgeInHoursSkipDbs, DEFAULT_alarm_LastLogBackupAgeInHoursSkipDbs);
//						String keepSrvRegExp = Configuration.getCombinedConfiguration().getProperty(PROPKEY_alarm_LastLogBackupAgeInHoursForSrv,  DEFAULT_alarm_LastLogBackupAgeInHoursForSrv);
//						String skipSrvRegExp = Configuration.getCombinedConfiguration().getProperty(PROPKEY_alarm_LastLogBackupAgeInHoursSkipSrv, DEFAULT_alarm_LastLogBackupAgeInHoursSkipSrv);
//
//						// The below could have been done with neasted if(keep-db), if(keep-srv), if(!skipDb), if(!skipSrv) doAlarm=true; 
//						// Below is more readable, from a variable context point-of-view, but HARDER to understand
//						boolean doAlarm = true; // note: this must be set to true at start, otherwise all below rules will be disabled (it "stops" processing at first doAlarm==false)
//						doAlarm = (doAlarm && (StringUtil.isNullOrBlank(keepDbRegExp)  ||   dbname     .matches(keepDbRegExp ))); //     matches the KEEP Db  regexp
//						doAlarm = (doAlarm && (StringUtil.isNullOrBlank(keepSrvRegExp) ||   dbmsSrvName.matches(keepSrvRegExp))); //     matches the KEEP Srv regexp
//						doAlarm = (doAlarm && (StringUtil.isNullOrBlank(skipDbRegExp)  || ! dbname     .matches(skipDbRegExp ))); // NO match in the SKIP Db  regexp
//						doAlarm = (doAlarm && (StringUtil.isNullOrBlank(skipSrvRegExp) || ! dbmsSrvName.matches(skipSrvRegExp))); // NO match in the SKIP Srv regexp
//						
//						if (doAlarm)
//						{
//							alarmHandler.addAlarm( new AlarmEventOldBackup(cm, threshold, "LOG", dbname, val.intValue()) );
//						}
//					}
//				}
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
					System.out.println("##### sendAlarmRequest("+cm.getName()+"): dbname='"+dbname+"', threshold="+threshold+", LowDbFreeSpaceInMb: DataSizeFreeInMb='"+freeMb+"', DataSizeUsedPct='"+usedPct+"'.");

				if (freeMb != null && usedPct != null && threshold != null)
				{
					if (freeMb.intValue() < threshold.intValue())
					{
						String extendedDescText = cm.toTextTableString(DATA_RATE, r);
						String extendedDescHtml = cm.toHtmlTableString(DATA_RATE, r, true, false, false);
						extendedDescHtml += "<br><br>" + cm.getGraphDataHistoryAsHtmlImage(GRAPH_NAME_DATASIZE_LEFT_MB, dbname);
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
					System.out.println("##### sendAlarmRequest("+cm.getName()+"): dbname='"+dbname+"', threshold="+threshold+", LowLogFreeSpaceInMb: LogSizeFreeInMb='"+freeMb+"', LogSizeUsedPct='"+usedPct+"'.");

				if (freeMb != null && usedPct != null && threshold != null)
				{
					if (freeMb.intValue() < threshold.intValue())
					{
						String extendedDescText = cm.toTextTableString(DATA_RATE, r);
						String extendedDescHtml = cm.toHtmlTableString(DATA_RATE, r, true, false, false);
						extendedDescHtml += "<br><br>" + cm.getGraphDataHistoryAsHtmlImage(GRAPH_NAME_LOGSIZE_LEFT_MB, dbname);
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
					System.out.println("##### sendAlarmRequest("+cm.getName()+"): dbname='"+dbname+"', threshold="+threshold+", LowDbFreeSpaceInPct: DataSizeFreeInMb='"+freeMb+"', DataSizeUsedPct='"+usedPct+"'.");

				if (freeMb != null && usedPct != null && threshold != null)
				{
					if (usedPct > threshold.doubleValue())
					{
						String extendedDescText = cm.toTextTableString(DATA_RATE, r);
						String extendedDescHtml = cm.toHtmlTableString(DATA_RATE, r, true, false, false);
						extendedDescHtml += "<br><br>" + cm.getGraphDataHistoryAsHtmlImage(GRAPH_NAME_DATASIZE_LEFT_MB, dbname);
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
					System.out.println("##### sendAlarmRequest("+cm.getName()+"): dbname='"+dbname+"', threshold="+threshold+", LowLogFreeSpaceInPct: LogSizeFreeInMb='"+freeMb+"', LogSizeUsedPct='"+usedPct+"'.");

				if (freeMb != null && usedPct != null && threshold != null)
				{
					if (usedPct > threshold.doubleValue())
					{
						String extendedDescText = cm.toTextTableString(DATA_RATE, r);
						String extendedDescHtml = cm.toHtmlTableString(DATA_RATE, r, true, false, false);
						extendedDescHtml += "<br><br>" + cm.getGraphDataHistoryAsHtmlImage(GRAPH_NAME_LOGSIZE_LEFT_MB, dbname);
						extendedDescHtml += "<br><br>" + cm.getGraphDataHistoryAsHtmlImage(GRAPH_NAME_LOGSIZE_USED_PCT, dbname);

						AlarmEvent ae = new AlarmEventLowLogFreeSpace(cm, dbname, freeMb.intValue(), usedPct, threshold.doubleValue());
						ae.setExtendedDescription(extendedDescText, extendedDescHtml);
						
						alarmHandler.addAlarm( ae );
						//alarmHandler.addAlarm( new AlarmEventLowLogFreeSpace(cm, dbname, freeMb.intValue(), usedPct, threshold.doubleValue()) );
					}
				}
			}

			//-------------------------------------------------------
			// DBOptions
			//-------------------------------------------------------
			if (isSystemAlarmsForColumnEnabledAndInTimeRange("DBOptions"))
			{
				Object o_dbOptions  = cm.getAbsValue(r, "DBOptions");

				if (debugPrint || _logger.isDebugEnabled())
					System.out.println("##### sendAlarmRequest("+cm.getName()+"): dbname='"+dbname+"', dbOptions='"+o_dbOptions+"'.");

				if (o_dbOptions != null)
				{
					String currDbOptions = o_dbOptions.toString();
					String prevDbOptions = _prevDbOptionsMap.get(dbname);
					
//					-- Does this really work: isFirstTimeSample()
//					-- or should we use: hasDiffData()
//					-- or do this somehow different
//					if ( ! cm.isFirstTimeSample() )
					if ( prevDbOptions != null )
					{
						if ( ! currDbOptions.equals(prevDbOptions) )
						{
							// Get config 'skip/allow'
							String keepDbRegExp  = Configuration.getCombinedConfiguration().getProperty(PROPKEY_alarm_DBOptionsForDbs,  DEFAULT_alarm_DBOptionsForDbs);
							String skipDbRegExp  = Configuration.getCombinedConfiguration().getProperty(PROPKEY_alarm_DBOptionsSkipDbs, DEFAULT_alarm_DBOptionsSkipDbs);
							String keepSrvRegExp = Configuration.getCombinedConfiguration().getProperty(PROPKEY_alarm_DBOptionsForSrv,  DEFAULT_alarm_DBOptionsForSrv);
							String skipSrvRegExp = Configuration.getCombinedConfiguration().getProperty(PROPKEY_alarm_DBOptionsSkipSrv, DEFAULT_alarm_DBOptionsSkipSrv);

							// note: this must be set to true at start, otherwise all below rules will be disabled (it "stops" processing at first doAlarm==false)
							boolean doAlarm = true;

							// The below could have been done with neasted if(keep-db), if(keep-srv), if(!skipDb), if(!skipSrv) doAlarm=true; 
							// Below is more readable, from a variable context point-of-view, but HARDER to understand
							doAlarm = (doAlarm && (StringUtil.isNullOrBlank(keepDbRegExp)  ||   dbname     .matches(keepDbRegExp ))); //     matches the KEEP Db  regexp
							doAlarm = (doAlarm && (StringUtil.isNullOrBlank(keepSrvRegExp) ||   dbmsSrvName.matches(keepSrvRegExp))); //     matches the KEEP Srv regexp
							doAlarm = (doAlarm && (StringUtil.isNullOrBlank(skipDbRegExp)  || ! dbname     .matches(skipDbRegExp ))); // NO match in the SKIP Db  regexp
							doAlarm = (doAlarm && (StringUtil.isNullOrBlank(skipSrvRegExp) || ! dbmsSrvName.matches(skipSrvRegExp))); // NO match in the SKIP Srv regexp
							
							if (doAlarm)
							{
								// Parse the CSV Text into a List, this so we can find out what DBOption that was added or removed
								List<String> prevDbOptionsList = StringUtil.parseCommaStrToList(prevDbOptions);
								List<String> currDbOptionsList = StringUtil.parseCommaStrToList(currDbOptions);

								List<String> addedOptions   = new ArrayList<>(currDbOptionsList); 
								addedOptions  .removeAll(prevDbOptionsList);
								
								List<String> removedOptions = new ArrayList<>(prevDbOptionsList); 
								removedOptions.removeAll(currDbOptionsList);

								String extendedDescText = cm.toTextTableString(DATA_RATE, r);
								String extendedDescHtml = cm.toHtmlTableString(DATA_RATE, r, true, false, false);

								AlarmEvent ae = new AlarmEventDatabaseOption(cm, dbname, addedOptions, removedOptions, currDbOptionsList);
								ae.setExtendedDescription(extendedDescText, extendedDescHtml);
								
								alarmHandler.addAlarm( ae );
							}
						}
					}
					
					// Always set/remember the "last" dboptions so we can compare at/with next sample
					_prevDbOptionsMap.put(dbname, currDbOptions);
				}
			}
		} // end: loop dbnames

	
		// Check if all Mandatory databases has been checked
//		if (isSystemAlarmsForColumnEnabledAndInTimeRange("MandatoryDatabaseList"))
//		{
//			String dbListStr = Configuration.getCombinedConfiguration().getProperty(PROPKEY_alarm_MandatoryDatabaseList, DEFAULT_alarm_MandatoryDatabaseList);
//			if (StringUtil.hasValue(dbListStr))
//			{
//				List<String> dbList = StringUtil.parseCommaStrToList(dbListStr);
//				for (String dbNameRegEx : dbList)
//				{
//					List<String> matches = StringUtil.getMatchingStrings(examedDbList, dbNameRegEx);
//					if ( matches.size() != 1 )
//					{
//						System.out.println("ALARM: MandatoryDatabaseList: Missing entry '"+dbNameRegEx+"' [matches.size()="+matches.size()+", matchesEntries="+matches+"], in the list of examed databases '"+examedDbList+"'.");
//						//FIXME: make alarm here
//					}
//				}
//			}
//		}
		
	}
	
	/** Remember DBOptions. key=dbname, val=DBOption, the value will be set at every call to sendAlarmRequest() */
	private Map<String, String> _prevDbOptionsMap = new HashMap<>();
	
	
	@Override
	public boolean isGraphDataHistoryEnabled(String name)
	{
		// ENABLED for the following graphs
		if (GRAPH_NAME_LOGSIZE_LEFT_MB  .equals(name)) return true;
//		if (GRAPH_NAME_LOGSIZE_USED_MB  .equals(name)) return true;
		if (GRAPH_NAME_LOGSIZE_USED_PCT .equals(name)) return true;
		if (GRAPH_NAME_DATASIZE_LEFT_MB .equals(name)) return true;
//		if (GRAPH_NAME_DATASIZE_USED_MB .equals(name)) return true;
		if (GRAPH_NAME_DATASIZE_USED_PCT.equals(name)) return true;
//		if (GRAPH_NAME_TEMPDB_USED_MB   .equals(name)) return true;

		// default: DISABLED
		return false;
	}
	@Override
	public int getGraphDataHistoryTimeInterval(String name)
	{
		// Keep interval: default is 60 minutes
		return super.getGraphDataHistoryTimeInterval(name);
	}

	@Override
	public void reset()
	{
		super.reset();
	
		// Or should this be done in: clear()
		_prevDbOptionsMap = new HashMap<>();
	}
//	@Override
//	public void clear()
//	{
//		// TODO Auto-generated method stub
//		super.clear();
//
//		_prevDbOptionsMap = new HashMap<>();
//	}

	
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


//	private Map<String, Number> _map_alarm_LowDbFreeSpaceInMb   = new HashMap<>();
//	private Map<String, Number> _map_alarm_LowLogFreeSpaceInMb  = new HashMap<>();
//	private Map<String, Number> _map_alarm_LowDbFreeSpaceInPct  = new HashMap<>();
//	private Map<String, Number> _map_alarm_LowLogFreeSpaceInPct = new HashMap<>();
	private Map<String, Number> _map_alarm_LowDbFreeSpaceInMb;  // Note: do NOT initialize this here... since the initAlarms() is done in super, if initialized it will be overwritten here...
	private Map<String, Number> _map_alarm_LowLogFreeSpaceInMb; // Note: do NOT initialize this here... since the initAlarms() is done in super, if initialized it will be overwritten here...
	private Map<String, Number> _map_alarm_LowDbFreeSpaceInPct; // Note: do NOT initialize this here... since the initAlarms() is done in super, if initialized it will be overwritten here...
	private Map<String, Number> _map_alarm_LowLogFreeSpaceInPct;// Note: do NOT initialize this here... since the initAlarms() is done in super, if initialized it will be overwritten here...
	
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
			}
		}
	}

	public static final String  PROPKEY_alarm_OldestTranInSeconds             = CM_NAME + ".alarm.system.if.OldestTranInSeconds.gt";
	public static final int     DEFAULT_alarm_OldestTranInSeconds             = 60;
	
	public static final String  PROPKEY_alarm_OldestTranInSecondsSkipTranName = CM_NAME + ".alarm.system.if.OldestTranInSeconds.skip.tranName";
	public static final String  DEFAULT_alarm_OldestTranInSecondsSkipTranName = "^(DUMP |\\$dmpxact).*";
	
	public static final String  PROPKEY_alarm_OldestTranInSecondsSkipTranProg = CM_NAME + ".alarm.system.if.OldestTranInSeconds.skip.tranProg";
	public static final String  DEFAULT_alarm_OldestTranInSecondsSkipTranProg = "";
	
	public static final String  PROPKEY_alarm_OldestTranInSecondsSkipTranUser = CM_NAME + ".alarm.system.if.OldestTranInSeconds.skip.tranUser";
	public static final String  DEFAULT_alarm_OldestTranInSecondsSkipTranUser = "";
	
	public static final String  PROPKEY_alarm_OldestTranInSecondsSkipTranHost = CM_NAME + ".alarm.system.if.OldestTranInSeconds.skip.tranHost";
	public static final String  DEFAULT_alarm_OldestTranInSecondsSkipTranHost = "";
	
	public static final String  PROPKEY_alarm_TransactionLogFull              = CM_NAME + ".alarm.system.if.TransactionLogFull.gt";
	public static final int     DEFAULT_alarm_TransactionLogFull              = 0;

	public static final String  PROPKEY_alarm_LastBackupFailed                = CM_NAME + ".alarm.system.if.LastBackupFailed.gt";
	public static final int     DEFAULT_alarm_LastBackupFailed                = 0;

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
	public static final String  DEFAULT_alarm_LowDbFreeSpaceInMb              = ".*=2, tempdb=100";
                                                                              
	public static final String  PROPKEY_alarm_LowLogFreeSpaceInMb             = CM_NAME + ".alarm.system.if.LowLogFreeSpaceInMb.lt";
	public static final String  DEFAULT_alarm_LowLogFreeSpaceInMb             = "";
                                                                              
	public static final String  PROPKEY_alarm_LowDbFreeSpaceInPct             = CM_NAME + ".alarm.system.if.LowDbFreeSpaceInPct.gt";
	public static final String  DEFAULT_alarm_LowDbFreeSpaceInPct             = "tempdb=80";
                                                                              
	public static final String  PROPKEY_alarm_LowLogFreeSpaceInPct            = CM_NAME + ".alarm.system.if.LowLogFreeSpaceInPct.gt";
	public static final String  DEFAULT_alarm_LowLogFreeSpaceInPct            = "";

	public static final String  PROPKEY_alarm_DBOptions                       = CM_NAME + ".alarm.system.if.DBOptions.is.changed";
	public static final boolean DEFAULT_alarm_DBOptions                       = true;
	public static final String  PROPKEY_alarm_DBOptionsForDbs                 = CM_NAME + ".alarm.system.if.DBOptions.for.dbs";
	public static final String  DEFAULT_alarm_DBOptionsForDbs                 = "";
	public static final String  PROPKEY_alarm_DBOptionsSkipDbs                = CM_NAME + ".alarm.system.if.DBOptions.skip.dbs";
	public static final String  DEFAULT_alarm_DBOptionsSkipDbs                = "";
	public static final String  PROPKEY_alarm_DBOptionsForSrv                 = CM_NAME + ".alarm.system.if.DBOptions.for.srv";
	public static final String  DEFAULT_alarm_DBOptionsForSrv                 = "";
	public static final String  PROPKEY_alarm_DBOptionsSkipSrv                = CM_NAME + ".alarm.system.if.DBOptions.skip.srv";
	public static final String  DEFAULT_alarm_DBOptionsSkipSrv                = "";

	// A comma separated list of databases that *must* exists, othewise ALARM. (the dbname can be a regexp, but each entry in the list must have a count of 1 after we have looped all records)
	// TODO: NOT YET IMPLEMENTED... think a bit more about this...
//	public static final String  PROPKEY_alarm_MandatoryDatabaseList           = CM_NAME + ".alarm.system.MandatoryDatabaseList";
//	public static final String  DEFAULT_alarm_MandatoryDatabaseList           = "";
	
	@Override
	public List<CmSettingsHelper> getLocalAlarmSettings()
	{
		Configuration conf = Configuration.getCombinedConfiguration();
		List<CmSettingsHelper> list = new ArrayList<>();
		
		CmSettingsHelper.Type isAlarmSwitch = CmSettingsHelper.Type.IS_ALARM_SWITCH;
		
		list.add(new CmSettingsHelper("OldestTranInSeconds",              isAlarmSwitch, PROPKEY_alarm_OldestTranInSeconds             , Integer.class, conf.getIntProperty(PROPKEY_alarm_OldestTranInSeconds             , DEFAULT_alarm_OldestTranInSeconds            ), DEFAULT_alarm_OldestTranInSeconds            , "If 'OldestTranInSeconds' is greater than ## then send 'AlarmEventLongRunningTransaction'." ));
		list.add(new CmSettingsHelper("OldestTranInSeconds SkipTranName",                PROPKEY_alarm_OldestTranInSecondsSkipTranName , String .class, conf.getProperty   (PROPKEY_alarm_OldestTranInSecondsSkipTranName , DEFAULT_alarm_OldestTranInSecondsSkipTranName), DEFAULT_alarm_OldestTranInSecondsSkipTranName, "If 'OldestTranInSeconds' is true; then we can filter out transaction names using a Regular expression... if (tranName.matches('regexp'))... This to remove alarms of 'DUMP DATABASE' or similar. A good place to test your regexp is 'http://www.regexplanet.com/advanced/java/index.html'.", new RegExpInputValidator()));
		list.add(new CmSettingsHelper("OldestTranInSeconds SkipTranProg",                PROPKEY_alarm_OldestTranInSecondsSkipTranProg , String .class, conf.getProperty   (PROPKEY_alarm_OldestTranInSecondsSkipTranProg , DEFAULT_alarm_OldestTranInSecondsSkipTranProg), DEFAULT_alarm_OldestTranInSecondsSkipTranProg, "If 'OldestTranInSeconds' is true; then we can filter out transaction names using a Regular expression... if (tranProg.matches('regexp'))... This to remove alarms of 'SQLAgent.*' or similar. A good place to test your regexp is 'http://www.regexplanet.com/advanced/java/index.html'.", new RegExpInputValidator()));
		list.add(new CmSettingsHelper("OldestTranInSeconds SkipTranUser",                PROPKEY_alarm_OldestTranInSecondsSkipTranUser , String .class, conf.getProperty   (PROPKEY_alarm_OldestTranInSecondsSkipTranUser , DEFAULT_alarm_OldestTranInSecondsSkipTranUser), DEFAULT_alarm_OldestTranInSecondsSkipTranUser, "If 'OldestTranInSeconds' is true; then we can filter out transaction names using a Regular expression... if (tranUser.matches('regexp'))... This to remove alarms of '(user1|user2)' or similar. A good place to test your regexp is 'http://www.regexplanet.com/advanced/java/index.html'.", new RegExpInputValidator()));
		list.add(new CmSettingsHelper("OldestTranInSeconds SkipTranHost",                PROPKEY_alarm_OldestTranInSecondsSkipTranHost , String .class, conf.getProperty   (PROPKEY_alarm_OldestTranInSecondsSkipTranHost , DEFAULT_alarm_OldestTranInSecondsSkipTranHost), DEFAULT_alarm_OldestTranInSecondsSkipTranHost, "If 'OldestTranInSeconds' is true; then we can filter out transaction names using a Regular expression... if (tranHost.matches('regexp'))... This to remove alarms of '.*-prod-.*' or similar. A good place to test your regexp is 'http://www.regexplanet.com/advanced/java/index.html'.", new RegExpInputValidator()));

		list.add(new CmSettingsHelper("TransactionLogFull",               isAlarmSwitch, PROPKEY_alarm_TransactionLogFull              , Integer.class, conf.getIntProperty(PROPKEY_alarm_TransactionLogFull              , DEFAULT_alarm_TransactionLogFull             ), DEFAULT_alarm_TransactionLogFull             , "If 'TransactionLogFull' is greater than ## then send 'AlarmEventFullTranLog'." ));

		list.add(new CmSettingsHelper("LastBackupFailed",                 isAlarmSwitch, PROPKEY_alarm_LastBackupFailed                , Integer.class, conf.getIntProperty(PROPKEY_alarm_LastBackupFailed                , DEFAULT_alarm_LastBackupFailed               ), DEFAULT_alarm_LastBackupFailed               , "If 'LastBackupFailed' is greater than ## then send 'AlarmEventLastBackupFailed'." ));

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

		list.add(new CmSettingsHelper("LowDbFreeSpaceInMb",               isAlarmSwitch, PROPKEY_alarm_LowDbFreeSpaceInMb              , String.class, conf.getProperty    (PROPKEY_alarm_LowDbFreeSpaceInMb              , DEFAULT_alarm_LowDbFreeSpaceInMb             ), DEFAULT_alarm_LowDbFreeSpaceInMb             , "If 'LowDbFreeSpaceInMb' is greater than ## then send 'AlarmEventLowDbFreeSpace'. format: db1=#, db2=#, db3=#  (Note: the 'dbname' can use regexp)"        , new MapNumberValidator()));
		list.add(new CmSettingsHelper("LowLogFreeSpaceInMb",              isAlarmSwitch, PROPKEY_alarm_LowLogFreeSpaceInMb             , String.class, conf.getProperty    (PROPKEY_alarm_LowLogFreeSpaceInMb             , DEFAULT_alarm_LowLogFreeSpaceInMb            ), DEFAULT_alarm_LowLogFreeSpaceInMb            , "If 'LowLogFreeSpaceInMb' is greater than ## then send 'AlarmEventLowLogFreeSpace'.format: db1=#, db2=#, db3=#  (Note: the 'dbname' can use regexp)"       , new MapNumberValidator()));
		list.add(new CmSettingsHelper("LowDbFreeSpaceInPct",              isAlarmSwitch, PROPKEY_alarm_LowDbFreeSpaceInPct             , String.class, conf.getProperty    (PROPKEY_alarm_LowDbFreeSpaceInPct             , DEFAULT_alarm_LowDbFreeSpaceInPct            ), DEFAULT_alarm_LowDbFreeSpaceInPct            , "If 'LowDbFreeSpaceInPct' is less than ## Percent then send 'AlarmEventLowDbFreeSpace'.format: db1=#, db2=#, db3=#  (Note: the 'dbname' can use regexp)"   , new MapNumberValidator()));
		list.add(new CmSettingsHelper("LowLogFreeSpaceInPct",             isAlarmSwitch, PROPKEY_alarm_LowLogFreeSpaceInPct            , String.class, conf.getProperty    (PROPKEY_alarm_LowLogFreeSpaceInPct            , DEFAULT_alarm_LowLogFreeSpaceInPct           ), DEFAULT_alarm_LowLogFreeSpaceInPct           , "If 'LowLogFreeSpaceInPct' is less than ## Percent then send 'AlarmEventLowLogFreeSpace'.format: db1=#, db2=#, db3=#  (Note: the 'dbname' can use regexp)" , new MapNumberValidator()));

//		list.add(new CmSettingsHelper("MandatoryDatabaseList",            PROPKEY_alarm_MandatoryDatabaseList           , String.class, conf.getProperty    (PROPKEY_alarm_MandatoryDatabaseList           , DEFAULT_alarm_MandatoryDatabaseList          ), DEFAULT_alarm_MandatoryDatabaseList          , "A list of databases that needs to be present. This is a comma separated list of databases (each name can contain regex)" ));
		
		list.add(new CmSettingsHelper("DBOptions",                        isAlarmSwitch, PROPKEY_alarm_DBOptions                       , Boolean.class, conf.getBooleanProperty(PROPKEY_alarm_DBOptions                    , DEFAULT_alarm_DBOptions                     ), DEFAULT_alarm_DBOptions                      , "If 'DBOptions' is changed then send 'AlarmEventDatabaseOption'."));
		list.add(new CmSettingsHelper("DBOptions ForDbs",                                PROPKEY_alarm_DBOptionsForDbs                 , String .class, conf.getProperty       (PROPKEY_alarm_DBOptionsForDbs              , DEFAULT_alarm_DBOptionsForDbs               ), DEFAULT_alarm_DBOptionsForDbs                , "If 'DBOptions' is changed; Only for the databases listed (regexp is used, blank=for-all-dbs). After this rule the 'skip' rule is evaluated.", new RegExpInputValidator()));
		list.add(new CmSettingsHelper("DBOptions SkipDbs",                               PROPKEY_alarm_DBOptionsSkipDbs                , String .class, conf.getProperty       (PROPKEY_alarm_DBOptionsSkipDbs             , DEFAULT_alarm_DBOptionsSkipDbs              ), DEFAULT_alarm_DBOptionsSkipDbs               , "If 'DBOptions' is changed; Discard databases listed (regexp is used). Before this rule the 'for/keep' rule is evaluated",                     new RegExpInputValidator()));
		list.add(new CmSettingsHelper("DBOptions ForSrv",                                PROPKEY_alarm_DBOptionsForSrv                 , String .class, conf.getProperty       (PROPKEY_alarm_DBOptionsForSrv              , DEFAULT_alarm_DBOptionsForSrv               ), DEFAULT_alarm_DBOptionsForSrv                , "If 'DBOptions' is changed; Only for the servers listed (regexp is used, blank=for-all-srv). After this rule the 'skip' rule is evaluated.",   new RegExpInputValidator()));
		list.add(new CmSettingsHelper("DBOptions SkipSrv",                               PROPKEY_alarm_DBOptionsSkipSrv                , String .class, conf.getProperty       (PROPKEY_alarm_DBOptionsSkipSrv             , DEFAULT_alarm_DBOptionsSkipSrv              ), DEFAULT_alarm_DBOptionsSkipSrv               , "If 'DBOptions' is changed; Discard servers listed (regexp is used). Before this rule the 'for/keep' rule is evaluated",                       new RegExpInputValidator()));

		return list;
	}

//	static void test(long srvVersion)
//	{
//		boolean okVersion =  (srvVersion >= Ver.ver(15,7,0, 136) || (srvVersion >= Ver.ver(15,7,0, 64) && srvVersion < Ver.ver(15,7,0, 100)) );
//		System.out.println("version="+srvVersion+", ok="+okVersion);
//		
//	}
//	public static void main(String[] args)
//	{
//		test(Ver.ver(15,7,0));
//		test(Ver.ver(15,7,0, 63));
//		test(Ver.ver(15,7,0, 64));
//		test(Ver.ver(15,7,0, 65));
//		test(Ver.ver(15,7,0, 99));
//		test(Ver.ver(15,7,0, 100));
//		test(Ver.ver(15,7,0, 135));
//		test(Ver.ver(15,7,0, 136));
//		test(Ver.ver(15,7,0, 138));
//		test(Ver.ver(16,0));
//	}
}
