package com.asetune.cm.ase;

import java.math.BigDecimal;
import java.sql.Connection;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import javax.naming.NameNotFoundException;

import org.apache.log4j.Logger;

import com.asetune.ICounterController;
import com.asetune.IGuiController;
import com.asetune.cm.CounterSample;
import com.asetune.cm.CounterSetTemplates;
import com.asetune.cm.CounterSetTemplates.Type;
import com.asetune.cm.CountersModel;
import com.asetune.cm.ase.gui.CmOpenDatabasesPanel;
import com.asetune.config.dict.MonTablesDictionary;
import com.asetune.config.dict.MonTablesDictionaryManager;
import com.asetune.graph.TrendGraphDataPoint;
import com.asetune.gui.DbSelectionForGraphsDialog;
import com.asetune.gui.MainFrame;
import com.asetune.gui.TabularCntrPanel;
import com.asetune.gui.TrendGraph;
import com.asetune.utils.AseConnectionUtils;
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
		"    <li>BLUE   - A Database backup is in progress</li>" +
		"    <li>YELLOW - Has a long running transaction issued by a user.</li>" +
		"    <li>PINK   - The transaction log for this database is filled to 90%, and will probably soon be full.</li>" +
		"    <li>RED    - The transaction log for this database is <b>full</b> and users are probably suspended.</li>" +
		"</ul>" +
		"</html>";

	public static final String   GROUP_NAME       = MainFrame.TCP_GROUP_SERVER;
	public static final String   GUI_ICON_FILE    = "images/"+CM_NAME+".png";

	public static final int      NEED_SRV_VERSION = 0;
	public static final int      NEED_CE_VERSION  = 0;

	public static final String[] MON_TABLES       = new String[] {"monOpenDatabases"};
	public static final String[] NEED_ROLES       = new String[] {"mon_role"};
	public static final String[] NEED_CONFIG      = new String[] {"enable monitoring=1"};

	public static final String[] PCT_COLUMNS      = new String[] {"AppendLogContPct", "LogSizeUsedPct", "DataSizeUsedPct"};
	public static final String[] DIFF_COLUMNS     = new String[] {
		"AppendLogRequests", "AppendLogWaits", 
		"PRSUpdateCount", "PRSSelectCount", "PRSRewriteCount",
		"LogSizeFreeInMbDiff", "DataSizeFreeInMbDiff"};

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
	
	public static final String GRAPH_NAME_LOGSEMAPHORE_CONT  = "DbLogSemapContGraph";   //String x=GetCounters.CM_GRAPH_NAME__OPEN_DATABASES__LOGSEMAPHORE_CONT;
	public static final String GRAPH_NAME_LOGSIZE_LEFT_MB    = "DbLogSizeLeftMbGraph";  //String x=GetCounters.CM_GRAPH_NAME__OPEN_DATABASES__LOGSIZE_LEFT;
	public static final String GRAPH_NAME_LOGSIZE_USED_PCT   = "DbLogSizeUsedPctGraph"; //String x=GetCounters.CM_GRAPH_NAME__OPEN_DATABASES__LOGSIZE_USED_PCT;
//	public static final String GRAPH_NAME_OLDEST_TRAN_IN_SEC = "OldestTranInSecGraph";

	private void addTrendGraphs()
	{
		String[] labels         = new String[] { "runtime-replaced" };
//		String[] openTranLabels = new String[] { "Seconds" };
		
		addTrendGraphData(GRAPH_NAME_LOGSEMAPHORE_CONT,  new TrendGraphDataPoint(GRAPH_NAME_LOGSEMAPHORE_CONT,  labels));
		addTrendGraphData(GRAPH_NAME_LOGSIZE_LEFT_MB,    new TrendGraphDataPoint(GRAPH_NAME_LOGSIZE_LEFT_MB,    labels));
		addTrendGraphData(GRAPH_NAME_LOGSIZE_USED_PCT,   new TrendGraphDataPoint(GRAPH_NAME_LOGSIZE_USED_PCT,   labels));
//		addTrendGraphData(GRAPH_NAME_OLDEST_TRAN_IN_SEC, new TrendGraphDataPoint(GRAPH_NAME_OLDEST_TRAN_IN_SEC, openTranLabels));

		// if GUI
		if (getGuiController() != null && getGuiController().hasGUI())
		{
			// GRAPH
			TrendGraph tg = null;
			tg = new TrendGraph(GRAPH_NAME_LOGSEMAPHORE_CONT,
				"DB Transaction Log Semaphore Contention",            // Menu CheckBox text
				"DB Transaction Log Semaphore Contention in Percent", // Label 
				labels, 
				false, // is Percent Graph
				this, 
				false, // visible at start
				0,     // graph is valid from Server Version. 0 = All Versions; >0 = Valid from this version and above 
				-1);   // minimum height
			addTrendGraph(tg.getName(), tg, true);

			tg = new TrendGraph(GRAPH_NAME_LOGSIZE_LEFT_MB,
				"DB Transaction Log Space left in MB",        // Menu CheckBox text
				"DB Transaction Log Space left to use in MB", // Label 
				labels, 
				false, // is Percent Graph
				this, 
				false, // visible at start
				0,     // graph is valid from Server Version. 0 = All Versions; >0 = Valid from this version and above 
				-1);   // minimum height
			addTrendGraph(tg.getName(), tg, true);

			tg = new TrendGraph(GRAPH_NAME_LOGSIZE_USED_PCT,
				"DB Transaction Log Space used in PCT",     // Menu CheckBox text
				"DB Transaction Log Space used in Percent", // Label 
				labels, 
				true,  // is Percent Graph
				this, 
				false, // visible at start
				0,     // graph is valid from Server Version. 0 = All Versions; >0 = Valid from this version and above 
				-1);   // minimum height
			addTrendGraph(tg.getName(), tg, true);

//			tg = new TrendGraph(GRAPH_NAME_OLDEST_TRAN_IN_SEC,
//				"Oldest Open Transaction in any Databases",     // Menu CheckBox text
//				"Oldest Open Transaction in any Databases, in Seconds", // Label 
//				openTranLabels, 
//				false, // is Percent Graph
//				this, 
//				false, // visible at start
//				0,     // graph is valid from Server Version. 0 = All Versions; >0 = Valid from this version and above 
//				-1);   // minimum height
//			addTrendGraph(tg.getName(), tg, true);
		}
	}

	@Override
	protected TabularCntrPanel createGui()
	{
		return new CmOpenDatabasesPanel(this);
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
			mtd.addColumn("monOpenDatabases", "CeDbRecoveryStatus", "<html>" +
			                                                             "1 = The database is currently undergoing <B>node-failure</B> recovery.<br> " +
			                                                             "0 = Normal, <B>not</B> in node-failure recovery." +
			                                                        "</html>");
			mtd.addColumn("monOpenDatabases", "AppendLogContPct",   "<html>" +
			                                                             "Log Semaphore Contention in percent.<br> " +
			                                                             "<b>Formula</b>: Pct = (AppendLogWaits / AppendLogRequests) * 100<br>" +
			                                                        "</html>");
			mtd.addColumn("monOpenDatabases", "DbSizeInMb",         "<html>Database size in MB</html>");
			mtd.addColumn("monOpenDatabases", "LogSizeInMb",        "<html>" +
			                                                             "Size in MB of the transaction log in the database. <br>" +
			                                                             "<b>Formula</b>: This is simply grabbed by: sum(size) from sysusages where (segmap & 4) = 4<br>" +
			                                                        "</html>");
			mtd.addColumn("monOpenDatabases", "LogSizeFreeInMb",    "<html>" +
			                                                             "How many MB have we got left in the Transaction log.<br> " +
			                                                             "<b>Formula</b>: (lct_admin('logsegment_freepages',DBID)-lct_admin('reserved_for_rollbacks',DBID)) / (1024.0*1024.0/@@maxpagesize)<br>" +
			                                                             "<b>Note 1</b>: This is the same formula as sp_helpdb 'dbname' uses to calculate space left.<br>" +
			                                                             "<b>Note 2</b>: This might not work correct for databases with mixed data and log.<br>" +
			                                                        "</html>");
			mtd.addColumn("monOpenDatabases", "LogSizeFreeInMbDiff","<html>Same as column 'LogSizeFreeInMb', but just show the difference from previous sample.</html>");
			mtd.addColumn("monOpenDatabases", "LogSizeUsedPct",     "<html>" +
			                                                            "How many percent have we <b>used</b> of the transaction log. near 100% = Full<br> " +
			                                                            "<b>Formula</b>: Pct = 100.0 - ((oval_LogSizeFreeInMb / oval_LogSizeInMb) * 100.0)<br>" +
			                                                        "</html>");
			mtd.addColumn("monOpenDatabases", "DataSizeInMb",       "<html>" +
			                                                            "Size in MB of the Data Portion in the database. <br>" +
			                                                            "<b>Formula</b>: This is simply grabbed by: sum(size) from sysusages where (segmap & 2) = 2<br>" +
			                                                        "</html>");
			mtd.addColumn("monOpenDatabases", "DataSizeFreeInMb",   "<html>" +
			                                                            "How many MB have we got left in the Data Portion.<br> " +
			                                                            "<b>Formula</b>: (select sum(curunreservedpgs(u.dbid, u.lstart, u.unreservedpgs)) from master..sysusages u readpast where u.dbid = od.DBID and (u.segmap & 2) = 2) / (1024*1024/@@maxpagesize)<br>" +
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
		}
		catch (NameNotFoundException e) {/*ignore*/}
	}

	@Override
	public List<String> getPkForVersion(Connection conn, int srvVersion, boolean isClusterEnabled)
	{
		List <String> pkCols = new LinkedList<String>();

		if (isClusterEnabled)
			pkCols.add("InstanceID");

		pkCols.add("DBName");

		return pkCols;
	}

	@Override
	public String getSqlForVersion(Connection conn, int aseVersion, boolean isClusterEnabled)
	{
		String cols1, cols2, cols3;
		cols1 = cols2 = cols3 = "";

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
//		if (aseVersion >= 12510)
//		if (aseVersion >= 1251000)
		if (aseVersion >= Ver.ver(12,5,1))
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
//		if (aseVersion >= 15702)
//		if (aseVersion >= 1570020)
		if (aseVersion >= Ver.ver(15,7,0,2))
		{
			PRSUpdateCount  = "od.PRSUpdateCount, ";  // Number of updates to PRSes (Precomputed Result Set) caused by IUDs (Insert/Update/Delete) on the base table
			PRSSelectCount  = "od.PRSSelectCount, ";  // Number of times PRSes (Precomputed Result Set) were selected for query rewriting plan during compilation
			PRSRewriteCount = "od.PRSRewriteCount, "; // Number of times PRSes (Precomputed Result Set) were considered valid for query rewriting during compilation
			nl_15702        = " \n";
		}

		// If we implement the FreeLogSize, then we need to take away databases that are in recovery etc...
		// Also calculate it into MB...
		// The calculation is stolen from: sp_helpdb dbname
		// declare	@pgsPerMb                int
		// select  @pgsPerMb           = 1024*1024 / @@maxpagesize
		// select @pgsPerMb   : 512=2K, 256=4K, 128=8K, 64=16K 
		// select mbUsed = pagesUsed / @pgsPerMb
//		String DbSizeInMb           = "DbSizeInMb           = (select sum(u.size) from master..sysusages u readpast where u.dbid = od.DBID)                                  / (1024*1024/@@maxpagesize), \n";
//
//		String LogSizeInMb          = "LogSizeInMb          = (select sum(u.size) from master..sysusages u readpast where u.dbid = od.DBID and (u.segmap & 4) = 4)           / (1024*1024/@@maxpagesize), \n";
//		String LogSizeFreeInMb      = "LogSizeFreeInMb      = convert(numeric(10,1), (lct_admin('logsegment_freepages',od.DBID)-lct_admin('reserved_for_rollbacks',od.DBID)) / (1024.0*1024.0/@@maxpagesize)), \n";
//		String LogSizeFreeInMbDiff  = "LogSizeFreeInMbDiff  = convert(numeric(10,1), (lct_admin('logsegment_freepages',od.DBID)-lct_admin('reserved_for_rollbacks',od.DBID)) / (1024.0*1024.0/@@maxpagesize)), \n";
//		String LogSizeUsedPct       = "LogSizeUsedPct       = convert(numeric(10,1), 0), /* calculated in AseTune */ \n";
//
//		String DataSizeInMb         = "DataSizeInMb         = (select sum(u.size) from master..sysusages u readpast where u.dbid = od.DBID and (u.segmap & 2) = 2)           / (1024*1024/@@maxpagesize), \n";
//		String DataSizeFreeInMb     = "DataSizeFreeInMb     = convert(numeric(10,1), (select sum(curunreservedpgs(u.dbid, u.lstart, u.unreservedpgs)) from master..sysusages u readpast where u.dbid = od.DBID and (u.segmap & 2) = 2) / (1024.0*1024.0/@@maxpagesize)), \n";
//		String DataSizeFreeInMbDiff = "DataSizeFreeInMbDiff = convert(numeric(10,1), (select sum(curunreservedpgs(u.dbid, u.lstart, u.unreservedpgs)) from master..sysusages u readpast where u.dbid = od.DBID and (u.segmap & 2) = 2) / (1024.0*1024.0/@@maxpagesize)), \n";
//		String DataSizeUsedPct      = "DataSizeUsedPct      = convert(numeric(10,1), 0), /* calculated in AseTune */ \n";

		String DbSizeInMb           = "DbSizeInMb           = (select sum(u.size/(1024*1024/@@maxpagesize)) from master..sysusages u readpast where u.dbid = od.DBID), \n";

		String LogSizeInMb          = "LogSizeInMb          = (select sum(u.size/(1024*1024/@@maxpagesize)) from master..sysusages u readpast where u.dbid = od.DBID and (u.segmap & 4) = 4), \n";
		String LogSizeFreeInMb      = "LogSizeFreeInMb      = convert(numeric(10,1), (lct_admin('logsegment_freepages',od.DBID)-lct_admin('reserved_for_rollbacks',od.DBID)) / (1024.0*1024.0/@@maxpagesize)), \n";
		String LogSizeFreeInMbDiff  = "LogSizeFreeInMbDiff  = convert(numeric(10,1), (lct_admin('logsegment_freepages',od.DBID)-lct_admin('reserved_for_rollbacks',od.DBID)) / (1024.0*1024.0/@@maxpagesize)), \n";
		String LogSizeUsedPct       = "LogSizeUsedPct       = convert(numeric(10,1), 0), /* calculated in AseTune */ \n";

		String DataSizeInMb         = "DataSizeInMb         = (select sum(u.size/(1024*1024/@@maxpagesize)) from master..sysusages u readpast where u.dbid = od.DBID and (u.segmap & 2) = 2), \n";
		String DataSizeFreeInMb     = "DataSizeFreeInMb     = convert(numeric(10,1), (select sum(curunreservedpgs(u.dbid, u.lstart, u.unreservedpgs)/(1024.0*1024.0/@@maxpagesize)) from master..sysusages u readpast where u.dbid = od.DBID and (u.segmap & 2) = 2)), \n";
		String DataSizeFreeInMbDiff = "DataSizeFreeInMbDiff = convert(numeric(10,1), (select sum(curunreservedpgs(u.dbid, u.lstart, u.unreservedpgs)/(1024.0*1024.0/@@maxpagesize)) from master..sysusages u readpast where u.dbid = od.DBID and (u.segmap & 2) = 2)), \n";
		String DataSizeUsedPct      = "DataSizeUsedPct      = convert(numeric(10,1), 0), /* calculated in AseTune */ \n";


		String OldestTranStartTime  = "OldestTranStartTime  = h.starttime, \n";
		String OldestTranInSeconds  = "OldestTranInSeconds  = CASE WHEN datediff(day, h.starttime, getdate()) > 20 THEN -1 ELSE  datediff(ss, h.starttime, getdate()) END, \n"; // protect from: Msg 535: Difference of two datetime fields caused overflow at runtime. above 24 days or so, the MS difference is overflowned
		String OldestTranName       = "OldestTranName       = h.name, \n";
		String OldestTranSpid       = "OldestTranSpid       = h.spid, \n";
		String SPID                 = "SPID                 = h.spid, \n";
		String OldestTranProg       = "OldestTranProg       = (select p.program_name from master..sysprocesses p where h.spid = p.spid), \n";
		String OldestTranPage       = "OldestTranPage       = h.page,\n";

		if ( ! canDoSelectOnSyslogshold )
		{
			OldestTranStartTime  = "OldestTranStartTime  = convert(datetime,    null), \n";
			OldestTranInSeconds  = "OldestTranInSeconds  = convert(int,         -1), \n";
			OldestTranName       = "OldestTranName       = convert(varchar(80), ''), \n";
			OldestTranSpid       = "OldestTranSpid       = convert(int,         -1), \n";
			SPID                 = "SPID                 = convert(int,         -1), \n";
			OldestTranProg       = "OldestTranProg       = convert(varchar(30), ''), \n";
			OldestTranPage       = "OldestTranPage       = convert(int,         -1), \n";
		}

		cols1 += "od.DBName, od.DBID, " + ceDbRecoveryStatus + "od.AppendLogRequests, od.AppendLogWaits, \n" +
		         "AppendLogContPct = CASE \n" +
		         "                      WHEN od.AppendLogRequests > 0 \n" +
		         "                      THEN convert(numeric(10,2), ((od.AppendLogWaits+0.0)/od.AppendLogRequests)*100.0) \n" +
		         "                      ELSE convert(numeric(10,2), 0.0) \n" +
		         "                   END, \n" +
		         DbSizeInMb + 
		         LogSizeInMb  + LogSizeFreeInMb  + LogSizeFreeInMbDiff  +  
		         LogSizeUsedPct + DataSizeUsedPct +
		         DataSizeInMb + DataSizeFreeInMb + DataSizeFreeInMbDiff + 
		         "od.TransactionLogFull, " + SuspendedProcesses + "\n" +
		         OldestTranStartTime +
		         OldestTranInSeconds +
		         OldestTranName      + 
		         OldestTranSpid      + 
		         SPID                + 
		         OldestTranProg      + 
		         OldestTranPage      + 
		         PRSUpdateCount + PRSSelectCount + PRSRewriteCount + nl_15702 +
		         "od.BackupInProgress, od.LastBackupFailed, od.BackupStartTime, ";
		cols2 += "";
		cols3 += QuiesceTag;
//		if (aseVersion >= 15010 || (aseVersion >= 12540 && aseVersion < 15000) )
//		if (aseVersion >= 1501000 || (aseVersion >= 1254000 && aseVersion < 1500000) )
		if (aseVersion >= Ver.ver(15,0,1) || (aseVersion >= Ver.ver(12,5,4) && aseVersion < Ver.ver(15,0)) )
		{
		}
//		if (aseVersion >= 15025)
//		if (aseVersion >= 1502050)
		if (aseVersion >= Ver.ver(15,0,2,5))
		{
			cols2 += "od.LastTranLogDumpTime, od.LastCheckpointTime, ";
		}
		String cols = cols1 + cols2 + cols3;
		cols = StringUtil.removeLastComma(cols);

		String sql = 
			"select " + cols + "\n" +
			"from master..monOpenDatabases od, master..syslogshold h \n" +
			"where od.DBID in (select db.dbid from master..sysdatabases db readpast where (db.status & 32 != 32) and (db.status & 256 != 256)) \n" +
			"  and od.DBID *= h.dbid \n" + 
			"  and h.name != '$replication_truncation_point' \n" + 
			"order by od.DBName \n" +
			"";
		// If we don't have permission on syslogshold, then make the SELECT statement simpler
		if ( ! canDoSelectOnSyslogshold )
		{
			sql = 
				"select " + cols + "\n" +
				"from master..monOpenDatabases od \n" +
				"where od.DBID in (select db.dbid from master..sysdatabases db readpast where (db.status & 32 != 32) and (db.status & 256 != 256)) \n" +
				"order by od.DBName \n" +
				"";
		}

		return sql;
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

		int    oval_LogSizeInMb;
		double oval_LogSizeFreeInMb;
		double calc_LogSizeUsedPct;
		int pos_LogSizeInMb     = -1;
		int pos_LogSizeFreeInMb = -1;
		int pos_LogSizeUsedPct  = -1;

		int    oval_DataSizeInMb;
		double oval_DataSizeFreeInMb;
		double calc_DataSizeUsedPct;
		int pos_DataSizeInMb     = -1;
		int pos_DataSizeFreeInMb = -1;
		int pos_DataSizeUsedPct  = -1;

		// Find column Id's
		List<String> colNames = diffData.getColNames();
		if (colNames == null)
			return;

		for (int colId=0; colId < colNames.size(); colId++) 
		{
			String colName = colNames.get(colId);
			if      (colName.equals("AppendLogContPct"))   AppendLogContPctId   = colId;
			else if (colName.equals("AppendLogRequests"))  AppendLogRequestsId  = colId;
			else if (colName.equals("AppendLogWaits"))     AppendLogWaitsId     = colId;
			else if (colName.equals("LogSizeInMb"))        pos_LogSizeInMb      = colId;
			else if (colName.equals("LogSizeFreeInMb"))    pos_LogSizeFreeInMb  = colId;
			else if (colName.equals("LogSizeUsedPct"))     pos_LogSizeUsedPct   = colId;
			else if (colName.equals("DataSizeInMb"))       pos_DataSizeInMb     = colId;
			else if (colName.equals("DataSizeFreeInMb"))   pos_DataSizeFreeInMb = colId;
			else if (colName.equals("DataSizeUsedPct"))    pos_DataSizeUsedPct  = colId;
//			else if (colName.equals("TransactionLogFull")) TransactionLogFullId = colId;
//			else if (colName.equals("SuspendedProcesses")) SuspendedProcessesId = colId;
		}

		// Loop on all diffData rows
		for (int rowId = 0; rowId < diffData.getRowCount(); rowId++)
		{
			AppendLogRequests     = ((Number)diffData.getValueAt(rowId, AppendLogRequestsId )).intValue();
			AppendLogWaits        = ((Number)diffData.getValueAt(rowId, AppendLogWaitsId    )).intValue();
			oval_LogSizeInMb      = ((Number)diffData.getValueAt(rowId, pos_LogSizeInMb     )).intValue();
			oval_LogSizeFreeInMb  = ((Number)diffData.getValueAt(rowId, pos_LogSizeFreeInMb )).doubleValue();
			oval_DataSizeInMb     = ((Number)diffData.getValueAt(rowId, pos_DataSizeInMb    )).intValue();
			oval_DataSizeFreeInMb = ((Number)diffData.getValueAt(rowId, pos_DataSizeFreeInMb)).doubleValue();

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

			// COLUMN: LogSizeUsedPct
			if (oval_LogSizeInMb > 0) // I doubt that oval_LogSizeInMb can be 0
			{
				// Formula: 
				calc_LogSizeUsedPct = 100.0 - (((oval_LogSizeFreeInMb + 0.0) / oval_LogSizeInMb) * 100.0);
				if (calc_LogSizeUsedPct < 0.0)
					calc_LogSizeUsedPct = 0.0;

				BigDecimal newVal = new BigDecimal(calc_LogSizeUsedPct).setScale(1, BigDecimal.ROUND_HALF_EVEN);
				diffData.setValueAt(newVal, rowId, pos_LogSizeUsedPct);
			}
			else
				diffData.setValueAt(new BigDecimal(0).setScale(1, BigDecimal.ROUND_HALF_EVEN), rowId, pos_LogSizeUsedPct);

			// COLUMN: DataSizeUsedPct
			if (oval_DataSizeInMb > 0) // I doubt that oval_DataSizeInMb can be 0
			{
				// Formula: 
				calc_DataSizeUsedPct = 100.0 - (((oval_DataSizeFreeInMb + 0.0) / oval_DataSizeInMb) * 100.0);
				if (calc_DataSizeUsedPct < 0.0)
					calc_DataSizeUsedPct = 0.0;

				BigDecimal newVal = new BigDecimal(calc_DataSizeUsedPct).setScale(1, BigDecimal.ROUND_HALF_EVEN);
				diffData.setValueAt(newVal, rowId, pos_DataSizeUsedPct);
			}
			else
				diffData.setValueAt(new BigDecimal(0).setScale(1, BigDecimal.ROUND_HALF_EVEN), rowId, pos_DataSizeUsedPct);
		}
	}

	@Override
	public void updateGraphData(TrendGraphDataPoint tgdp)
	{
		// Get what databases should be part of the graphs
		Map<String, Integer> dbMap = DbSelectionForGraphsDialog.getDbsInGraphList(this);

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
			tgdp.setDate(this.getTimestamp());
			tgdp.setLabel(lArray);
			tgdp.setData(dArray);
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
			tgdp.setDate(this.getTimestamp());
			tgdp.setLabel(lArray);
			tgdp.setData(dArray);
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
			tgdp.setDate(this.getTimestamp());
			tgdp.setLabel(lArray);
			tgdp.setData(dArray);
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
	}
}
