package com.asetune.cm.ase;

import java.awt.event.MouseEvent;
import java.sql.Connection;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

import javax.naming.NameNotFoundException;

import org.apache.log4j.Logger;

import com.asetune.ICounterController;
import com.asetune.IGuiController;
import com.asetune.cache.XmlPlanCache;
import com.asetune.cm.CmSettingsHelper;
import com.asetune.cm.CounterSample;
import com.asetune.cm.CounterSetTemplates;
import com.asetune.cm.CounterSetTemplates.Type;
import com.asetune.cm.CountersModel;
import com.asetune.cm.ase.gui.CmActiveStatementsPanel;
import com.asetune.config.dict.MonTablesDictionary;
import com.asetune.config.dict.MonTablesDictionaryManager;
import com.asetune.gui.AsePlanViewer;
import com.asetune.gui.MainFrame;
import com.asetune.gui.TabularCntrPanel;
import com.asetune.utils.AseConnectionUtils;
import com.asetune.utils.Configuration;
import com.asetune.utils.StringUtil;
import com.asetune.utils.Ver;

/**
 * @author Goran Schwarz (goran_schwarz@hotmail.com)
 */
public class CmActiveStatements
extends CountersModel
{
	private static Logger        _logger          = Logger.getLogger(CmActiveStatements.class);
	private static final long    serialVersionUID = 1L;

	public static final String   CM_NAME          = CmActiveStatements.class.getSimpleName();
	public static final String   SHORT_NAME       = "Active Statements";
	public static final String   HTML_DESC        = 
		"<html>" +
		"Statemenets that are currently executing in the ASE." +
		"<br><br>" +
		"Table Background colors:" +
		"<ul>" +
		"    <li>YELLOW - SPID this is holding <i>any</i> locks in the system and control is at the client side (WiatEventID=250 'waiting for input from the network', status='recv sleep').</li>" +
		"    <li>ORANGE - SPID was visible in previous sample as well.</li>" +
		"    <li>PINK   - SPID is Blocked by another SPID from running, this SPID is the Victim of a Blocking Lock, which is showned in RED.</li>" +
		"    <li>RED    - SPID is Blocking other SPID's from running, this SPID is Responslibe or the Root Cause of a Blocking Lock.</li>" +
		"</ul>" +
		"</html>";

	public static final String   GROUP_NAME       = MainFrame.TCP_GROUP_OBJECT_ACCESS;
	public static final String   GUI_ICON_FILE    = "images/"+CM_NAME+".png";

	public static final int      NEED_SRV_VERSION = 0;
	public static final int      NEED_CE_VERSION  = 0;

	public static final String[] MON_TABLES       = new String[] {"monProcessStatement", "monProcess"};
	public static final String[] NEED_ROLES       = new String[] {"mon_role"};
	public static final String[] NEED_CONFIG      = new String[] {"enable monitoring=1", "statement statistics active=1", "wait event timing=1"};

	public static final String[] PCT_COLUMNS      = new String[] {};
	public static final String[] DIFF_COLUMNS     = new String[] {
		"MemUsageKB", "PhysicalReads", "LogicalReads", 
		"PagesModified", "PacketsSent", "PacketsReceived", 
		"RowsAffectedDiff", "pssinfo_tempdb_pages"};

	public static final boolean  NEGATIVE_DIFF_COUNTERS_TO_ZERO = true;
	public static final boolean  IS_SYSTEM_CM                   = true;
	public static final int      DEFAULT_POSTPONE_TIME          = 0;
	public static final int      DEFAULT_QUERY_TIMEOUT          = 20;

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

		return new CmActiveStatements(counterController, guiController);
	}

	public CmActiveStatements(ICounterController counterController, IGuiController guiController)
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
	
	private void addTrendGraphs()
	{
	}

	@Override
	protected TabularCntrPanel createGui()
	{
		return new CmActiveStatementsPanel(this);
	}
	
	@Override
	public void addMonTableDictForVersion(Connection conn, int aseVersion, boolean isClusterEnabled)
	{
		try 
		{
			MonTablesDictionary mtd = MonTablesDictionaryManager.getInstance();
			mtd.addColumn("monProcess",  "monSource",          "<html>" +
			                                                       "This is the <i>source</i> of what part of the monitoring query the row resides from." +
			                                                       "<ul>" +
			                                                       "   <li>ACTIVE        - Statements that are currently executing SQL Statements at the server. <br>Basically rows in <code>monProcessStatement</code></li>" +
			                                                       "   <li>BLOCKER       - Statements that are holding <b>blocking</b> locks (stops other SPID's form working). <br><code>select ... from monProcess where WaitEventID = 250 and SPID in (select blocked from sysprocesses where blocked > 0)</code></li>" +
			                                                       "   <li>HOLDING-LOCKS - Statements that are holding <b>any</b> locks (may cause <i>blocking</i> locks soon and are <b>not</b> active in the server, WaitEventID=250). <br><code>select ... from monProcess where WaitEventID = 250 and SPID in (select spid from syslocks)</code></li>" +
			                                                       "</ul>" +
			                                                   "</html>");
			mtd.addColumn("monProcess",  "BlockingOtherSpids", "This SPID is Blocking other SPID's from executing, because this SPID hold lock(s), that some other SPID wants to grab.");
			mtd.addColumn("monProcess",  "multiSampled",       "<html>" +
			                                                       "This indicates that the PrimaryKey (SPID, monSource[, InstanceID]) has been in this table for more than one sample.<br>" +
			                                                       "Also 'StartTime' has to be the <b>same</b> as in the previous sample.<br>" +
			                                                       "The StartTime is the columns, which tells the start time for current SQL statement, or the individual statement inside a Stored Procedure." +
			                                                   "</html>");

			mtd.addColumn("monProcess",  "RowsAffectedDiff",   "Same as 'RowsAffected' but diff calculated");
			mtd.addColumn("monProcess",  "HasMonSqlText",      "Has values for: select SQLText from master.dbo.monProcessSQLText where SPID = <SPID>");
			mtd.addColumn("monProcess",  "MonSqlText",                         "select SQLText from master.dbo.monProcessSQLText where SPID = <SPID>");
			mtd.addColumn("monProcess",  "HasDbccSqlText",     "Has values for: DBCC sqltext(<SPID>)");
			mtd.addColumn("monProcess",  "DbccSqlText",                        "DBCC sqltext(<SPID>)");
			mtd.addColumn("monProcess",  "HasShowPlan",        "Has values for: sp_showplan <SPID>, null, null, null");
			mtd.addColumn("monProcess",  "ShowPlanText",                       "sp_showplan <SPID>, null, null, null");
			mtd.addColumn("monProcess",  "HasStacktrace",      "Has values for: DBCC stacktrace(<SPID>)");
			mtd.addColumn("monProcess",  "DbccStacktrace",                     "DBCC stacktrace(<SPID>)");
			mtd.addColumn("monProcess",  "HasCachedPlanInXml", "Has values for: show_cached_plan_in_xml(<planid>, 0, 0)");
			mtd.addColumn("monProcess",  "CachedPlanInXml",    "show_cached_plan_in_xml(<planid>, 0, 0)");
			mtd.addColumn("monProcess",  "HasProcCallStack",   "Has values for: select * from monProcessProcedures where SPID = <SPID>");
			mtd.addColumn("monProcess",  "ProcCallStack",                      "select * from monProcessProcedures where SPID = <SPID>");
		}
		catch (NameNotFoundException e) {/*ignore*/}
	}

	@Override
	public String[] getDependsOnConfigForVersion(Connection conn, int srvVersion, boolean isClusterEnabled)
	{
		if (srvVersion >= Ver.ver(15,7))
			return new String[] {"enable monitoring=1", "statement statistics active=1", "per object statistics active=1", "wait event timing=1"};

		return NEED_CONFIG;
	}

	@Override
	public List<String> getPkForVersion(Connection conn, int srvVersion, boolean isClusterEnabled)
	{
		List <String> pkCols = new LinkedList<String>();

		if (isClusterEnabled)
			pkCols.add("InstanceID");

		pkCols.add("SPID");
		pkCols.add("monSource");

		return pkCols;
	}

	@Override
	public String getSqlForVersion(Connection conn, int aseVersion, boolean isClusterEnabled)
	{
		String cols1, cols2, cols3;
		cols1 = cols2 = cols3 = "";

		////////////////////////////
		// NOTE: This Counter Model has 2 separete SQL selects
		//       1: The rows in: monProcessStatement
		//       2: The rows in: SPID's that might NOT be in monProcessStatement, BUT still are BLOCKING other spids
		// IMPORTANT: when you add a column to the FIRST you need to add it to the SECOND as well
		////////////////////////////

		boolean showHoldingLocks = Configuration.getCombinedConfiguration().getBooleanProperty(getName()+".sample.holdingLocks", false);

		// ASE 15.7
		String HostName       = "";
		String ClientName     = "";
		String ClientHostName = "";
		String ClientApplName = "";
		String ase1570_nl     = "";

//		if (aseVersion >= 15700)
//		if (aseVersion >= 1570000)
		if (aseVersion >= Ver.ver(15,7))
		{
			HostName       = "P.HostName, ";
			ClientName     = "P.ClientName, ";
			ClientHostName = "P.ClientHostName, ";
			ClientApplName = "P.ClientApplName, ";
			ase1570_nl     = "\n";
		}
		// ASE 16.0
		String ClientDriverVersion = ""; // The version of the connectivity driver used by the client program
//		if (aseVersion >= 1600000)
		if (aseVersion >= Ver.ver(16,0))
		{
			ClientDriverVersion = "P.ClientDriverVersion, ";
		}

		// ASE 16.0 SP3
		String QueryOptimizationTime       = "";
		String QueryOptimizationTime_union = "";
		String ase160_sp3_nl               = "";
		if (aseVersion >= Ver.ver(16,0,0, 3)) // 16.0 SP3
		{
			QueryOptimizationTime       = "S.QueryOptimizationTime, ";
			QueryOptimizationTime_union = "QueryOptimizationTime=-1, ";    // Used be the seconds SQL Statement, just to create a "union"
			ase160_sp3_nl               = "\n";
		}
		
		String optGoalPlan = "";
//		if (aseVersion >= 15020)
//		if (aseVersion >= 1502000)
		if (aseVersion >= Ver.ver(15,0,2))
		{
			optGoalPlan = "plan '(use optgoal allrows_dss)' \n";
		}

		if (isClusterEnabled)
		{
			cols1 += "S.InstanceID, ";
		}

		String dbNameCol  = "dbname=db_name(S.DBID)";
//		if (aseVersion >= 15026) // just a guess what release this was introduced (not in 15.0.2.1, but at least in 15.0.2.6, I have not gathered info for 15022-15025])
//		if (aseVersion >= 1502060) // just a guess what release this was introduced (not in 15.0.2.1, but at least in 15.0.2.6, I have not gathered info for 15022-15025])
		if (aseVersion >= Ver.ver(15,0,2,6)) // just a guess what release this was introduced (not in 15.0.2.1, but at least in 15.0.2.6, I have not gathered info for 15022-15025])
		{
			dbNameCol  = "dbname=S.DBName";
		}

		cols1 += "monSource=convert(varchar(15),'ACTIVE'), \n" +
		         "P.SPID, P.KPID, \n" +
		         "multiSampled=convert(varchar(10),''), \n" +
		         "S.BatchID, S.LineNumber, \n" +
		         dbNameCol+", procname=isnull(isnull(object_name(S.ProcedureID,S.DBID),object_name(S.ProcedureID,2)),''), linenum=S.LineNumber, \n" +
		         "P.Command, P.Application, \n" +
		         HostName + ClientName + ClientHostName + ClientApplName + ClientDriverVersion + ase1570_nl +
		         "S.CpuTime, S.WaitTime, \n" +
		         "ExecTimeInMs    = CASE WHEN datediff(day, S.StartTime, getdate()) >= 24 THEN -1 ELSE  datediff(ms, S.StartTime, getdate()) END, \n" +               // protect from: Msg 535: Difference of two datetime fields caused overflow at runtime. above 24 days or so, the MS difference is overflowned
		         "UsefullExecTime = CASE WHEN datediff(day, S.StartTime, getdate()) >= 24 THEN -1 ELSE (datediff(ms, S.StartTime, getdate()) - S.WaitTime) END, \n" + // protect from: Msg 535: Difference of two datetime fields caused overflow at runtime. above 24 days or so, the MS difference is overflowned
		         QueryOptimizationTime + ase160_sp3_nl +
		         "BlockingOtherSpids=convert(varchar(255),''), P.BlockingSPID, \n" +
		         "P.SecondsWaiting, P.WaitEventID, \n" +
		         "WaitClassDesc=convert(varchar(50),''), \n" +
		         "WaitEventDesc=convert(varchar(50),''), \n" +
		         "HasMonSqlText=convert(bit,0), HasDbccSqlText=convert(bit,0), HasProcCallStack=convert(bit,0), \n" +
		         "HasShowPlan=convert(bit,0), HasStacktrace=convert(bit,0), HasCachedPlanInXml=convert(bit,0), \n" +
		         "S.MemUsageKB, S.PhysicalReads, S.LogicalReads, \n";
		cols2 += "";
		cols3 += "S.PagesModified, S.PacketsSent, S.PacketsReceived, S.NetworkPacketSize, \n" +
		         "S.PlansAltered, S.StartTime, S.PlanID, S.DBID, S.ProcedureID, \n" +
		         "P.SecondsConnected, P.EngineNumber, P.NumChildren, \n" +
		         "MonSqlText=convert(text,null), \n" +
		         "DbccSqlText=convert(text,null), \n" +
		         "ProcCallStack=convert(text,null), \n" +
		         "ShowPlanText=convert(text,null), \n" +
		         "DbccStacktrace=convert(text,null), \n" +
		         "CachedPlanInXml=convert(text,null) \n" +
		         "";

		if (aseVersion >= Ver.ver(15,0,2) || (aseVersion >= Ver.ver(12,5,4) && aseVersion < Ver.ver(15,0)) )
		{
			cols2 += "RowsAffectedDiff = S.RowsAffected, S.RowsAffected, \n" +
			         "tempdb_name = db_name(tempdb_id(S.SPID)), \n" +
			         "pssinfo_tempdb_pages = convert(int, pssinfo(S.SPID, 'tempdb_pages')), \n";
		}

		// in 12.5.4 (esd#9) will produce an "empty" resultset using "S.SPID != @@spid"
		//                   so this will be a workaround for those releses below 15.0.0
		String whereSpidNotMe = "S.SPID != @@spid";
		if (aseVersion < Ver.ver(15,0))
		{
			whereSpidNotMe = "S.SPID != convert(int,@@spid)";
		}

		// The above I had in the Properties file for a long time
		String sql = 
			"select " + cols1 + cols2 + cols3 + "\n" +
			"from master.dbo.monProcessStatement S, master.dbo.monProcess P \n" +
			"where S.KPID = P.KPID \n" +
			"  and P.WaitEventID != 250 -- WaitEventID(250) = 'waiting for input from the network' \n" + // Sometimes the SPID is still in monProcessStatement even if the WaitEventID is 250
			(isClusterEnabled ? "  and S.InstanceID = P.InstanceID \n" : "") +
			"  and "+whereSpidNotMe+"\n" +
			"order by S.LogicalReads desc \n" +
			optGoalPlan;

		//-------------------------------------------
		// Build SECOND SQL, which gets SPID's that blocks other, and might not be within the ABOVE statement
		//-------------------------------------------
		cols1 = cols2 = cols3 = "";

		if (isClusterEnabled)
		{
			cols1 += "P.InstanceID, ";
		}

		dbNameCol  = "dbname=db_name(P.DBID)";
//		if (aseVersion >= 15020) // just a guess what release this was introduced. 15020 is OK
//		if (aseVersion >= 1502000) // just a guess what release this was introduced. 15020 is OK
		if (aseVersion >= Ver.ver(15,0,2)) // just a guess what release this was introduced. 15020 is OK
		{
			dbNameCol  = "dbname=P.DBName";
		}

		// 16.0 SP2 PL5 and 15.7.0 SP138 the column is bigint
		String PhysicalReads = "PhysicalReads=-1";
		String LogicalReads  = "LogicalReads=-1";
		String PagesModified = "PagesModified=-1";
		if (aseVersion >= Ver.ver(16,0,0, 2,5) || (aseVersion >= Ver.ver(15,7,0, 138) && aseVersion < Ver.ver(16,0)) )
		{
			PhysicalReads = "PhysicalReads=convert(bigint,-1)";
			LogicalReads  = "LogicalReads=convert(bigint,-1)";
			PagesModified = "PagesModified=convert(bigint,-1)";
		}

		cols1 += "monSource=convert(varchar(15),'BLOCKER'), \n" +
		         "P.SPID, P.KPID, \n" +
		         "multiSampled=convert(varchar(10),''), \n" +
		         "P.BatchID, P.LineNumber, \n" +
		         dbNameCol+", procname='', linenum=P.LineNumber, \n" +
		         "P.Command, P.Application, \n" +
		         HostName + ClientName + ClientHostName + ClientApplName + ClientDriverVersion + ase1570_nl +
		         "CpuTime=-1, WaitTime=-1, \n" +
		         "ExecTimeInMs    = -1, \n" +
		         "UsefullExecTime = -1, \n" +
		         QueryOptimizationTime_union + ase160_sp3_nl +
		         "BlockingOtherSpids=convert(varchar(255),''), P.BlockingSPID, \n" +
		         "P.SecondsWaiting, P.WaitEventID, \n" +
		         "WaitClassDesc=convert(varchar(50),''), \n" +
		         "WaitEventDesc=convert(varchar(50),''), \n" +
		         "HasMonSqlText=convert(bit,0), HasDbccSqlText=convert(bit,0), HasProcCallStack=convert(bit,0), \n" +
		         "HasShowPlan=convert(bit,0), HasStacktrace=convert(bit,0), HasCachedPlanInXml=convert(bit,0), \n" +
		         "MemUsageKB=-1, "+PhysicalReads+", "+LogicalReads+", \n";
		cols2 += "";
		cols3 += PagesModified+", PacketsSent=-1, PacketsReceived=-1, NetworkPacketSize=-1, \n" +
		         "PlansAltered=-1, StartTime=convert(datetime,NULL), PlanID=-1, P.DBID, ProcedureID=-1, \n" +
		         "P.SecondsConnected, P.EngineNumber, P.NumChildren, \n" +
		         "MonSqlText=convert(text,null), \n" +
		         "DbccSqlText=convert(text,null), \n" +
		         "ProcCallStack=convert(text,null), \n" +
		         "ShowPlanText=convert(text,null), \n" +
		         "DbccStacktrace=convert(text,null), \n" +
		         "CachedPlanInXml=convert(text,null) \n" +
		         "";
		if (aseVersion >= Ver.ver(15,0,2) || (aseVersion >= Ver.ver(12,5,4) && aseVersion < Ver.ver(15,0)) )
		{
			cols2 += "RowsAffectedDiff = convert(int,-1), RowsAffected = convert(int,-1), \n" +
			         "tempdb_name = db_name(tempdb_id(P.SPID)), \n" +
			         "pssinfo_tempdb_pages = convert(int, pssinfo(P.SPID, 'tempdb_pages')), \n";
		}


		// The above I had in the Properties file for a long time
		String sqlShowBlockers = 
			"select " + cols1 + cols2 + cols3 + "\n" +
			"from master.dbo.monProcess P \n" +
			"where 1 = 1 \n" + 
			"  and P.WaitEventID = 250 -- 'waiting for input from the network' \n" + 
			"  and P.SPID in (select blocked from master.dbo.sysprocesses where blocked > 0) \n" +
			optGoalPlan;

		// Construct SQL for SPID's that HOLDS LOCKS
		String sqlShowHoldingLocks = "";
		if (showHoldingLocks)
		{
			// Reuse most of the 'sqlShowBlockers', but replace the monSource='BLOCKER' -->> monSource='HOLDING-LOCKS'
			cols1 = cols1.replace("'BLOCKER'", "'HOLDING-LOCKS'");
			sqlShowHoldingLocks = 
				"select " + cols1 + cols2 + cols3 + "\n" +
				"from master.dbo.monProcess P \n" +
				"where 1 = 1 \n" + 
				"  and P.WaitEventID = 250 -- 'waiting for input from the network' \n" + 
				"  and P.SPID in (select spid from master.dbo.syslocks) \n" +
				optGoalPlan;
		}

		//------------------------------------------------
		// Finally return the SQL
		//------------------------------------------------
		return sql + "\n\n"
			+ "  /* UNION ALL */ \n\n"
			+ sqlShowBlockers
			+ (showHoldingLocks ? "\n\n  /* UNION ALL */ \n\n" : "") 
			+ sqlShowHoldingLocks;
	}

	/** Used by the: Create 'Offline Session' Wizard */
	@Override
	public List<CmSettingsHelper> getLocalSettings()
	{
		Configuration conf = Configuration.getCombinedConfiguration();
		List<CmSettingsHelper> list = new ArrayList<>();
		
		list.add(new CmSettingsHelper("Get Showplan",             getName()+".sample.showplan"        , Boolean.class, conf.getBooleanProperty(getName()+".sample.showplan"        , true ), true , "Do 'sp_showplan spid' on every row in the table."                                      ));
		list.add(new CmSettingsHelper("Get Monitored SQL Text",   getName()+".sample.monSqltext"      , Boolean.class, conf.getBooleanProperty(getName()+".sample.monSqltext"      , true ), true , "Do 'select SQLText from monProcessSQLText where SPID=spid' on every row in the table." ));
		list.add(new CmSettingsHelper("Get DBCC SQL Text",        getName()+".sample.dbccSqltext"     , Boolean.class, conf.getBooleanProperty(getName()+".sample.dbccSqltext"     , false), false, "Do 'dbcc sqltext(spid)' on every row in the table."                                    ));
		list.add(new CmSettingsHelper("Get Procedure Call Stack", getName()+".sample.procCallStack"   , Boolean.class, conf.getBooleanProperty(getName()+".sample.procCallStack"   , false), false, "Do 'select * from monProcessProcedures where SPID=spid' on every row in the table."    ));
		list.add(new CmSettingsHelper("Get ASE Stacktrace",       getName()+".sample.dbccStacktrace"  , Boolean.class, conf.getBooleanProperty(getName()+".sample.dbccStacktrace"  , false), false, "Do 'dbcc stacktrace(spid)' on every row in the table."                                 ));
		list.add(new CmSettingsHelper("Get Cached Plan in XML",   getName()+".sample.cachedPlanInXml" , Boolean.class, conf.getBooleanProperty(getName()+".sample.cachedPlanInXml" , false), false, "Do 'select show_cached_plan_in_xml(planid, 0, 0)' on every row in the table."          ));
		list.add(new CmSettingsHelper("Get SPID's holding locks", getName()+".sample.holdingLocks"    , Boolean.class, conf.getBooleanProperty(getName()+".sample.holdingLocks"    , true ), true , "Include SPID's that holds locks even if that are not active in the server."            ));

		return list;
	}


	@Override
	public String getToolTipTextOnTableCell(MouseEvent e, String colName, Object cellValue, int modelRow, int modelCol) 
	{
		// SHOWPLAN STUFF
		if ("HasShowPlan".equals(colName))
		{
			// Find 'ShowPlanText' column, is so get it and set it as the tool tip
			int pos_ShowPlanText = findColumn("ShowPlanText");
			if (pos_ShowPlanText > 0)
			{
				Object cellVal = getValueAt(modelRow, pos_ShowPlanText);
				if (cellVal instanceof String)
					return (String) cellVal;
			}
		}
		if ("ShowPlanText".equals(colName))
		{
			return cellValue == null ? null : cellValue.toString();
		}
		
		// MON SQL TEXT
		if ("HasMonSqlText".equals(colName))
		{
			// Find 'MonSqlText' column, is so get it and set it as the tool tip
			int pos_MonSqlText = findColumn("MonSqlText");
			if (pos_MonSqlText > 0)
			{
				Object cellVal = getValueAt(modelRow, pos_MonSqlText);
				if (cellVal instanceof String)
				{
					return toHtmlString((String) cellVal, true);
					//return (String) cellVal;
				}
			}
		}
		if ("MonSqlText".equals(colName))
		{
			return cellValue == null ? null : cellValue.toString();
		}
		
		// DBCC SQL TEXT
		if ("HasDbccSqlText".equals(colName))
		{
			// Find 'DbccSqlText' column, is so get it and set it as the tool tip
			int pos_DbccSqlText = findColumn("DbccSqlText");
			if (pos_DbccSqlText > 0)
			{
				Object cellVal = getValueAt(modelRow, pos_DbccSqlText);
				if (cellVal instanceof String)
				{
					return toHtmlString((String) cellVal, true);
					//return (String) cellVal;
				}
			}
		}
		if ("DbccSqlText".equals(colName))
		{
			return cellValue == null ? null : cellValue.toString();
		}
		
		// PROC CALL STACK
		if ("HasProcCallStack".equals(colName))
		{
			// Find 'ProcCallStack' column, is so get it and set it as the tool tip
			int pos_ProcCallStack = findColumn("ProcCallStack");
			if (pos_ProcCallStack > 0)
			{
				Object cellVal = getValueAt(modelRow, pos_ProcCallStack);
				if (cellVal instanceof String)
				{
					return (String) cellVal;
				}
			}
		}
		if ("ProcCallStack".equals(colName))
		{
			return cellValue == null ? null : cellValue.toString();
		}
		
		// DBCC STACKTRACE
		if ("HasStacktrace".equals(colName))
		{
			// Find 'DbccStacktrace' column, is so get it and set it as the tool tip
			int pos_DbccStacktrace = findColumn("DbccStacktrace");
			if (pos_DbccStacktrace > 0)
			{
				Object cellVal = getValueAt(modelRow, pos_DbccStacktrace);
				if (cellVal instanceof String)
				{
					return (String) cellVal;
				}
			}
		}
		if ("DbccStacktrace".equals(colName))
		{
			return cellValue == null ? null : cellValue.toString();
		}
		
		// show_cached_plan_in_xml(planid, 0, 0)
		if ("HasCachedPlanInXml".equals(colName))
		{
			// Find 'CachedPlanInXml' column, is so get it and set it as the tool tip
			int pos_CachedPlanInXml = findColumn("CachedPlanInXml");
			if (pos_CachedPlanInXml > 0)
			{
				Object cellVal = getValueAt(modelRow, pos_CachedPlanInXml);
				if (cellVal instanceof String)
				{
//					return toHtmlString( (String) cellVal, false );
					AsePlanViewer.getInstance().loadXmlDeferred((String) cellVal);
					return null;
				}
			}
		}
		if ("CachedPlanInXml".equals(colName))
		{
			return cellValue == null ? null : cellValue.toString();
		}
		
		return super.getToolTipTextOnTableCell(e, colName, cellValue, modelRow, modelCol);
	}
	/** add HTML around the string, and translate linebreaks into <br> */
	private String toHtmlString(String in, boolean breakLines)
	{
		String str = in;
		str = str.replaceAll("<", "&lt;");
		str = str.replaceAll(">", "&gt;");
		if (breakLines)
			str = StringUtil.makeApproxLineBreak(in, 150, 5, "\n");
		str = str.replaceAll("\\n", "<br>");

		return "<html><pre>" + str + "</pre></html>";
	}
//	/** add HTML around the string, and translate line breaks into <br> */
//	private String toHtmlString(String in)
//	{
//		String str = StringUtil.makeApproxLineBreak(in, 150, 10, "\n");
//		str = str.replaceAll("\\n", "<br>");
//		if (in.indexOf("<html>")>=0 || in.indexOf("<HTML>")>=0)
//			return str;
//		return "<html><pre>" + str + "</pre></html>";
//	}

	@Override
	public Class<?> getColumnClass(int columnIndex)
	{
		// use CHECKBOX for column "HasShowPlan"
		String colName = getColumnName(columnIndex);
		if      ("HasShowPlan"       .equals(colName)) return Boolean.class;
		else if ("HasMonSqlText"     .equals(colName)) return Boolean.class;
		else if ("HasDbccSqlText"    .equals(colName)) return Boolean.class;
		else if ("HasProcCallStack"  .equals(colName)) return Boolean.class;
		else if ("HasStacktrace"     .equals(colName)) return Boolean.class;
		else if ("HasCachedPlanInXml".equals(colName)) return Boolean.class;
		else return super.getColumnClass(columnIndex);
	}

	/** 
	 * Fill in the WaitEventDesc column with data from
	 * MonTableDictionary.. transforms a WaitEventId -> text description
	 * This so we do not have to do a subselect in the query that gets data
	 * doing it this way, means better performance, since the values are cached locally in memory
	 * Also do post lookups of dbcc sqltext, sp_showplan, dbcc stacktrace
	 */
	@Override
	public void localCalculation(CounterSample prevSample, CounterSample newSample)
	{
		Configuration conf = Configuration.getCombinedConfiguration();
		boolean getShowplan        = conf == null ? true : conf.getBooleanProperty(getName()+".sample.showplan",        true);
		boolean getMonSqltext      = conf == null ? true : conf.getBooleanProperty(getName()+".sample.monSqltext",      true);
		boolean getDbccSqltext     = conf == null ? false: conf.getBooleanProperty(getName()+".sample.dbccSqltext",     false);
		boolean getProcCallStack   = conf == null ? true : conf.getBooleanProperty(getName()+".sample.procCallStack",   true);
		boolean getDbccStacktrace  = conf == null ? false: conf.getBooleanProperty(getName()+".sample.dbccStacktrace",  false);
		boolean getCachedPlanInXml = conf == null ? false: conf.getBooleanProperty(getName()+".sample.cachedPlanInXml", false);

		// Where are various columns located in the Vector 
		int pos_WaitEventID        = -1, pos_WaitEventDesc   = -1, pos_WaitClassDesc = -1, pos_SPID = -1;
		int pos_HasShowPlan        = -1, pos_ShowPlanText    = -1;
		int pos_HasMonSqlText      = -1, pos_MonSqlText      = -1;
		int pos_HasDbccSqlText     = -1, pos_DbccSqlText     = -1;
		int pos_HasProcCallStack   = -1, pos_ProcCallStack   = -1;
		int pos_HasStacktrace      = -1, pos_DbccStacktrace  = -1;
		int pos_HasCachedPlanInXml = -1, pos_CachedPlanInXml = -1, pos_procname = -1;
		int pos_BlockingOtherSpids = -1, pos_BlockingSPID    = -1;
		int pos_multiSampled       = -1;
		int pos_StartTime          = -1;
		int pos_BatchID            = -1;
		int waitEventID = 0;
		String waitEventDesc = "";
		String waitClassDesc = "";

		CounterSample counters = newSample;

		if ( ! MonTablesDictionaryManager.hasInstance() )
			return;
		MonTablesDictionary mtd = MonTablesDictionaryManager.getInstance();

		if (counters == null)
			return;

		// Find column Id's
		List<String> colNames = counters.getColNames();
		if (colNames==null) return;

		for (int colId=0; colId < colNames.size(); colId++) 
		{
			String colName = colNames.get(colId);
			if      (colName.equals("WaitEventID"))        pos_WaitEventID        = colId;
			else if (colName.equals("WaitEventDesc"))      pos_WaitEventDesc      = colId;
			else if (colName.equals("WaitClassDesc"))      pos_WaitClassDesc      = colId;
			else if (colName.equals("SPID"))               pos_SPID               = colId;
			else if (colName.equals("HasShowPlan"))        pos_HasShowPlan        = colId;
			else if (colName.equals("ShowPlanText"))       pos_ShowPlanText       = colId;
			else if (colName.equals("HasMonSqlText"))      pos_HasMonSqlText      = colId;
			else if (colName.equals("MonSqlText"))         pos_MonSqlText         = colId;
			else if (colName.equals("HasDbccSqlText"))     pos_HasDbccSqlText     = colId;
			else if (colName.equals("DbccSqlText"))        pos_DbccSqlText        = colId;
			else if (colName.equals("HasProcCallStack"))   pos_HasProcCallStack   = colId;
			else if (colName.equals("ProcCallStack"))      pos_ProcCallStack      = colId;
			else if (colName.equals("HasStacktrace"))      pos_HasStacktrace      = colId;
			else if (colName.equals("DbccStacktrace"))     pos_DbccStacktrace     = colId;
			else if (colName.equals("HasCachedPlanInXml")) pos_HasCachedPlanInXml = colId;
			else if (colName.equals("CachedPlanInXml"))    pos_CachedPlanInXml    = colId;
			else if (colName.equals("procname"))           pos_procname           = colId;
			else if (colName.equals("BlockingOtherSpids")) pos_BlockingOtherSpids = colId;
			else if (colName.equals("BlockingSPID"))       pos_BlockingSPID       = colId;
			else if (colName.equals("multiSampled"))       pos_multiSampled       = colId;
			else if (colName.equals("StartTime"))          pos_StartTime          = colId;
			else if (colName.equals("BatchID"))            pos_BatchID          = colId;

			// No need to continue, we got all our columns
			if (    pos_WaitEventID        >= 0 && pos_WaitEventDesc   >= 0 
			     && pos_WaitClassDesc      >= 0 && pos_SPID            >= 0 
			     && pos_HasShowPlan        >= 0 && pos_ShowPlanText    >= 0 
			     && pos_HasMonSqlText      >= 0 && pos_MonSqlText      >= 0 
			     && pos_HasDbccSqlText     >= 0 && pos_DbccSqlText     >= 0 
			     && pos_HasProcCallStack   >= 0 && pos_ProcCallStack   >= 0 
			     && pos_HasStacktrace      >= 0 && pos_DbccStacktrace  >= 0 
			     && pos_HasCachedPlanInXml >= 0 && pos_CachedPlanInXml >= 0 && pos_procname >= 0
			     && pos_BlockingOtherSpids >= 0 && pos_BlockingSPID    >= 0
			     && pos_multiSampled       >= 0
			     && pos_StartTime          >= 0
			     && pos_BatchID            >= 0
			   )
				break;
		}

		if (pos_WaitEventID < 0 || pos_WaitEventDesc < 0 || pos_WaitClassDesc < 0)
		{
			_logger.debug("Can't find the position for columns ('WaitEventID'="+pos_WaitEventID+", 'WaitEventDesc'="+pos_WaitEventDesc+", 'WaitClassDesc'="+pos_WaitClassDesc+")");
			return;
		}
		
		if (pos_SPID < 0 || pos_HasShowPlan < 0 || pos_ShowPlanText < 0)
		{
			_logger.debug("Can't find the position for columns ('SPID'="+pos_SPID+", 'HasShowPlan'="+pos_HasShowPlan+", 'ShowPlanText'="+pos_ShowPlanText+")");
			return;
		}

		if (pos_HasDbccSqlText < 0 || pos_DbccSqlText < 0)
		{
			_logger.debug("Can't find the position for columns ('HasDbccSqlText'="+pos_HasDbccSqlText+", 'DbccSqlText'="+pos_DbccSqlText+")");
			return;
		}

		if (pos_HasProcCallStack < 0 || pos_ProcCallStack < 0)
		{
			_logger.debug("Can't find the position for columns ('HasProcCallStack'="+pos_HasProcCallStack+", 'ProcCallStack'="+pos_ProcCallStack+")");
			return;
		}

		if (pos_HasMonSqlText < 0 || pos_MonSqlText < 0)
		{
			_logger.debug("Can't find the position for columns (''HasMonSqlText'="+pos_HasMonSqlText+", 'MonSqlText'="+pos_MonSqlText+")");
			return;
		}

		if (pos_HasStacktrace < 0 || pos_DbccStacktrace < 0)
		{
			_logger.debug("Can't find the position for columns ('HasShowplan'="+pos_HasStacktrace+", 'DbccStacktrace'="+pos_DbccStacktrace+")");
			return;
		}
		
//		if (pos_HasCachedPlanInXml < 0 || pos_CachedPlanInXml < 0)
//		{
//			_logger.debug("Can't find the position for columns ('HasCachedPlanInXml'="+pos_HasCachedPlanInXml+", 'CachedPlanInXml'="+pos_CachedPlanInXml+")");
//			return;
//		}
		
//		if (pos_procname < 0)
//		{
//			_logger.debug("Can't find the position for columns ('procname'="+pos_procname+")");
//			return;
//		}
		
		if (pos_BlockingOtherSpids < 0 || pos_BlockingSPID < 0)
		{
			_logger.debug("Can't find the position for columns ('BlockingOtherSpids'="+pos_BlockingOtherSpids+", 'BlockingSPID'="+pos_BlockingSPID+")");
			return;
		}
		
		if (pos_multiSampled < 0)
		{
			_logger.debug("Can't find the position for columns ('multiSampled'="+pos_multiSampled+")");
			return;
		}
		
		if (pos_StartTime < 0)
		{
			_logger.debug("Can't find the position for columns ('StartTime'="+pos_StartTime+")");
			return;
		}
		if (pos_BatchID < 0)
		{
			_logger.debug("Can't find the position for columns ('BatchID'="+pos_BatchID+")");
			return;
		}
		
		// Loop on all diffData rows
		for (int rowId=0; rowId < counters.getRowCount(); rowId++) 
		{
			String thisRowPk = counters.getPkValue(rowId);
			int prevPkRowId = (prevSample == null) ? -1 : prevSample.getRowNumberForPkValue(thisRowPk);
			boolean prevPkExists = prevPkRowId >= 0;

			Object o_waitEventId = counters.getValueAt(rowId, pos_WaitEventID);
			Object o_SPID        = counters.getValueAt(rowId, pos_SPID);
			Object o_procname    = counters.getValueAt(rowId, pos_procname);

			if (prevPkExists)
			{
				Object o_this_StartTime = counters  .getValueAt(rowId,       pos_StartTime);
				Object o_prev_StartTime = prevSample.getValueAt(prevPkRowId, pos_StartTime);

				// Check StartTime, but at a SECOND level instead of MS (since MS isn't 100% accurate)
				// If it's a SQL Batch, we might run several things in a loop... which means it's more or less like a "a here" stored procedure
				if (o_this_StartTime instanceof Timestamp && o_prev_StartTime instanceof Timestamp)
				{
//					long l_this_StartTime = ((Timestamp)o_this_StartTime).getTime() / 1000;
//					long l_prev_StartTime = ((Timestamp)o_prev_StartTime).getTime() / 1000;
//					if (l_this_StartTime == l_prev_StartTime)
//						counters.setValueAt("YES", rowId, pos_multiSampled);
					long l_this_StartTime = ((Timestamp)o_this_StartTime).getTime();
					long l_prev_StartTime = ((Timestamp)o_prev_StartTime).getTime();
					long l_diff = Math.abs(l_this_StartTime - l_prev_StartTime); // turn negative numbers positive
					if (l_diff < 100) // 100ms diff, is treated as "multiSampled" or "sameStartTime" as previously
						counters.setValueAt("YES", rowId, pos_multiSampled);
				}

//				Object o_this_BatchID = counters  .getValueAt(rowId,       pos_BatchID);
//				Object o_prev_BatchID = prevSample.getValueAt(prevPkRowId, pos_BatchID);
//
//				// If it's a Stored Procedure executing, trust 'StartTime' 
//				// Else: ordinary SQL Statements (including StatementCache LWP) trust 'BatchID'
//				if (o_procname != null && !o_procname.equals("") && !((String)o_procname).startsWith("*s"))
//				{
//					if (o_this_StartTime instanceof Timestamp && o_prev_StartTime instanceof Timestamp)
//					{
//						if (o_this_StartTime.equals(o_prev_StartTime))
//							counters.setValueAt("YES", rowId, pos_multiSampled);
//					}
//				}
//				else
//				{
//    				if (o_this_BatchID instanceof Number && o_prev_BatchID instanceof Number)
//    				{
//    					if (o_this_BatchID.equals(o_prev_BatchID))
//    						counters.setValueAt("YES", rowId, pos_multiSampled);
//    				}
//				}
			}

			if (o_waitEventId instanceof Number)
			{
				waitEventID	  = ((Number)o_waitEventId).intValue();

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

				//row.set( pos_WaitEventDesc, waitEventDesc);
				counters.setValueAt(waitEventDesc, rowId, pos_WaitEventDesc);
				counters.setValueAt(waitClassDesc, rowId, pos_WaitClassDesc);
			}

			if (o_procname instanceof String)
			{
				String procname = (String)o_procname;

				String cachedPlanInXml = "User does not have: sa_role";

				if (isServerRoleOrPermissionActive(AseConnectionUtils.SA_ROLE))
				{
					if (getCachedPlanInXml)
//						cachedPlanInXml = AseConnectionUtils.cachedPlanInXml(getCounterController().getMonConnection(), procname, false);
						cachedPlanInXml = XmlPlanCache.getInstance().getPlan(procname);
					else
						cachedPlanInXml = "This was disabled";
					if (cachedPlanInXml == null)
						cachedPlanInXml = "Not Available";
				}

				boolean b = true;
				b = !"This was disabled".equals(cachedPlanInXml) && !"Not Available".equals(cachedPlanInXml) && !cachedPlanInXml.startsWith("User does not have");
				counters.setValueAt(new Boolean(b),  rowId, pos_HasCachedPlanInXml);
				counters.setValueAt(cachedPlanInXml, rowId, pos_CachedPlanInXml);
			}

			if (o_SPID instanceof Number)
			{
				int spid = ((Number)o_SPID).intValue();

				String monSqlText      = "Not properly configured (need 'SQL batch capture' & 'max SQL text monitored').";
				String dbccSqlText     = "User does not have: sa_role";
				String procCallStack   = "User does not have: sa_role";
				String showplan        = "User does not have: sa_role";
				String stacktrace      = "User does not have: sa_role";

				if (getMonitorConfig("SQL batch capture") > 0 && getMonitorConfig("max SQL text monitored") > 0)
				{
					// monProcessSQLText; needs 'enable monitoring', 'SQL batch capture' and 'max SQL text monitored' configuration parameters for this monitoring table to collect data.
					if (getMonSqltext)
						monSqlText  = AseConnectionUtils.monSqlText(getCounterController().getMonConnection(), spid, true);
					else
						monSqlText = "This was disabled";
					if (monSqlText == null)
						monSqlText = "Not Available";
				}
				if (isServerRoleOrPermissionActive(AseConnectionUtils.SA_ROLE))
				{
					if (getDbccSqltext)
						dbccSqlText  = AseConnectionUtils.dbccSqlText(getCounterController().getMonConnection(), spid, true);
					else
						dbccSqlText = "This was disabled";
					if (dbccSqlText == null)
						dbccSqlText = "Not Available";

					if (getProcCallStack)
						procCallStack  = AseConnectionUtils.monProcCallStack(getCounterController().getMonConnection(), spid, true);
					else
						procCallStack = "This was disabled";
					if (procCallStack == null)
						procCallStack = "Not Available";

					if (getShowplan)
						showplan = AseConnectionUtils.getShowplan(getCounterController().getMonConnection(), spid, "Showplan:", true);
					else
						showplan = "This was disabled";
					if (showplan == null || (showplan != null && showplan.matches(".*The query plan for spid '.*' is unavailable.*")) )
						showplan = "Not Available";

					if (getDbccStacktrace)
						stacktrace = AseConnectionUtils.dbccStacktrace(getCounterController().getMonConnection(), spid, true, waitEventID);
					else
						stacktrace = "This was disabled";
					if (stacktrace == null)
						stacktrace = "Not Available";
				}
				boolean b = true;
				b = !"This was disabled".equals(monSqlText)    && !"Not Available".equals(monSqlText)    && !monSqlText   .startsWith("Not properly configured");
				counters.setValueAt(new Boolean(b), rowId, pos_HasMonSqlText);
				counters.setValueAt(monSqlText,     rowId, pos_MonSqlText);

				b = !"This was disabled".equals(dbccSqlText)   && !"Not Available".equals(dbccSqlText)   && !dbccSqlText  .startsWith("User does not have");
				counters.setValueAt(new Boolean(b), rowId, pos_HasDbccSqlText);
				counters.setValueAt(dbccSqlText,    rowId, pos_DbccSqlText);

				b = !"This was disabled".equals(procCallStack) && !"Not Available".equals(procCallStack) && !procCallStack.startsWith("User does not have");
				counters.setValueAt(new Boolean(b), rowId, pos_HasProcCallStack);
				counters.setValueAt(procCallStack,  rowId, pos_ProcCallStack);

				b = !"This was disabled".equals(showplan)      && !"Not Available".equals(showplan)      && !showplan     .startsWith("User does not have");
				counters.setValueAt(new Boolean(b), rowId, pos_HasShowPlan);
				counters.setValueAt(showplan,       rowId, pos_ShowPlanText);

				b = !"This was disabled".equals(stacktrace)    && !"Not Available".equals(stacktrace)    && !stacktrace   .startsWith("User does not have");
				counters.setValueAt(new Boolean(b), rowId, pos_HasStacktrace);
				counters.setValueAt(stacktrace,     rowId, pos_DbccStacktrace);

				// Get LIST of SPID's that I'm blocking
				String blockingList = getBlockingListStr(counters, spid, pos_BlockingSPID, pos_SPID);

				// This could be used to test that PCS.store() will truncate string size to the tables storage size
				//blockingList += "'1:aaa:0', '1:bbb:0', '1:ccc:0', '1:ddd:0', '1:eee:0', '1:fff:0', '1:ggg:0', '1:hhh:0', '1:iii:0', '1:jjj:0', '1:kkk:0', '1:lll:0', '1:mmm:0', '1:nnn:0', '1:ooo:0', '1:ppp:0', '1:qqq:0', '1:rrr:0', '1:sss:0', '1:ttt:0', '1:uuu:0', '1:vvv:0', '1:wwww:0', '1:xxx:0', '1:yyy:0', '1:zzz:0' -end-";

				counters.setValueAt(blockingList,      rowId, pos_BlockingOtherSpids);
			}
		}
	}

//	/** 
//	 * Fill in the WaitEventDesc column with data from
//	 * MonTableDictionary.. transforms a WaitEventId -> text description
//	 * This so we do not have to do a subselect in the query that gets data
//	 * doing it this way, means better performance, since the values are cached locally in memory
//	 * Also do post lookups of dbcc sqltext, sp_showplan, dbcc stacktrace
//	 */
//	@Override
//	public void localCalculation(CounterSample prevSample, CounterSample newSample, CounterSample diffData)
//	{
////		long startTime = System.currentTimeMillis();
//
////		Configuration conf = Configuration.getInstance(Configuration.TEMP);
//		Configuration conf = Configuration.getCombinedConfiguration();
//		boolean getShowplan       = conf == null ? true : conf.getBooleanProperty(getName()+".sample.showplan",       true);
//		boolean getMonSqltext     = conf == null ? true : conf.getBooleanProperty(getName()+".sample.monSqltext",     true);
//		boolean getDbccSqltext    = conf == null ? false: conf.getBooleanProperty(getName()+".sample.dbccSqltext",    false);
//		boolean getProcCallStack  = conf == null ? true : conf.getBooleanProperty(getName()+".sample.procCallStack",  true);
//		boolean getDbccStacktrace = conf == null ? false: conf.getBooleanProperty(getName()+".sample.dbccStacktrace", false);
//
//		// Where are various columns located in the Vector 
//		int pos_WaitEventID        = -1, pos_WaitEventDesc  = -1, pos_WaitClassDesc = -1, pos_SPID = -1;
//		int pos_HasShowPlan        = -1, pos_ShowPlanText   = -1;
//		int pos_HasMonSqlText      = -1, pos_MonSqlText     = -1;
//		int pos_HasDbccSqlText     = -1, pos_DbccSqlText    = -1;
//		int pos_HasProcCallStack   = -1, pos_ProcCallStack  = -1;
//		int pos_HasStacktrace      = -1, pos_DbccStacktrace = -1;
//		int pos_BlockingOtherSpids = -1, pos_BlockingSPID   = -1;
//		int pos_multiSampled       = -1;
//		int pos_StartTime          = -1;
//		int waitEventID = 0;
//		String waitEventDesc = "";
//		String waitClassDesc = "";
//		CounterSample counters = diffData;
//
//		if ( ! MonTablesDictionaryManager.hasInstance() )
//			return;
//		MonTablesDictionary mtd = MonTablesDictionaryManager.getInstance();
//
//		if (counters == null)
//			return;
//
//		// Find column Id's
//		List<String> colNames = counters.getColNames();
//		if (colNames==null) return;
//
//		for (int colId=0; colId < colNames.size(); colId++) 
//		{
//			String colName = colNames.get(colId);
//			if      (colName.equals("WaitEventID"))        pos_WaitEventID        = colId;
//			else if (colName.equals("WaitEventDesc"))      pos_WaitEventDesc      = colId;
//			else if (colName.equals("WaitClassDesc"))      pos_WaitClassDesc      = colId;
//			else if (colName.equals("SPID"))               pos_SPID               = colId;
//			else if (colName.equals("HasShowPlan"))        pos_HasShowPlan        = colId;
//			else if (colName.equals("ShowPlanText"))       pos_ShowPlanText       = colId;
//			else if (colName.equals("HasMonSqlText"))      pos_HasMonSqlText      = colId;
//			else if (colName.equals("MonSqlText"))         pos_MonSqlText         = colId;
//			else if (colName.equals("HasDbccSqlText"))     pos_HasDbccSqlText     = colId;
//			else if (colName.equals("DbccSqlText"))        pos_DbccSqlText        = colId;
//			else if (colName.equals("HasProcCallStack"))   pos_HasProcCallStack   = colId;
//			else if (colName.equals("ProcCallStack"))      pos_ProcCallStack      = colId;
//			else if (colName.equals("HasStacktrace"))      pos_HasStacktrace      = colId;
//			else if (colName.equals("DbccStacktrace"))     pos_DbccStacktrace     = colId;
//			else if (colName.equals("BlockingOtherSpids")) pos_BlockingOtherSpids = colId;
//			else if (colName.equals("BlockingSPID"))       pos_BlockingSPID       = colId;
//			else if (colName.equals("multiSampled"))       pos_multiSampled       = colId;
//			else if (colName.equals("StartTime"))          pos_StartTime          = colId;
//
//			// Noo need to continue, we got all our columns
//			if (    pos_WaitEventID        >= 0 && pos_WaitEventDesc  >= 0 
//			     && pos_WaitClassDesc      >= 0 && pos_SPID >= 0 
//			     && pos_HasShowPlan        >= 0 && pos_ShowPlanText   >= 0 
//			     && pos_HasMonSqlText      >= 0 && pos_MonSqlText     >= 0 
//			     && pos_HasDbccSqlText     >= 0 && pos_DbccSqlText    >= 0 
//			     && pos_HasProcCallStack   >= 0 && pos_ProcCallStack  >= 0 
//			     && pos_HasStacktrace      >= 0 && pos_DbccStacktrace >= 0 
//			     && pos_BlockingOtherSpids >= 0 && pos_BlockingSPID   >= 0
//			     && pos_multiSampled       >= 0
//			     && pos_StartTime          >= 0
//			   )
//				break;
//		}
//
//		if (pos_WaitEventID < 0 || pos_WaitEventDesc < 0 || pos_WaitClassDesc < 0)
//		{
//			_logger.debug("Can't find the position for columns ('WaitEventID'="+pos_WaitEventID+", 'WaitEventDesc'="+pos_WaitEventDesc+", 'WaitClassDesc'="+pos_WaitClassDesc+")");
//			return;
//		}
//		
//		if (pos_SPID < 0 || pos_HasShowPlan < 0 || pos_ShowPlanText < 0)
//		{
//			_logger.debug("Can't find the position for columns ('SPID'="+pos_SPID+", 'HasShowPlan'="+pos_HasShowPlan+", 'ShowPlanText'="+pos_ShowPlanText+")");
//			return;
//		}
//
//		if (pos_HasDbccSqlText < 0 || pos_DbccSqlText < 0)
//		{
//			_logger.debug("Can't find the position for columns ('HasDbccSqlText'="+pos_HasDbccSqlText+", 'DbccSqlText'="+pos_DbccSqlText+")");
//			return;
//		}
//
//		if (pos_HasProcCallStack < 0 || pos_ProcCallStack < 0)
//		{
//			_logger.debug("Can't find the position for columns ('HasProcCallStack'="+pos_HasProcCallStack+", 'ProcCallStack'="+pos_ProcCallStack+")");
//			return;
//		}
//
//		if (pos_HasMonSqlText < 0 || pos_MonSqlText < 0)
//		{
//			_logger.debug("Can't find the position for columns (''HasMonSqlText'="+pos_HasMonSqlText+", 'MonSqlText'="+pos_MonSqlText+")");
//			return;
//		}
//
//		if (pos_HasStacktrace < 0 || pos_DbccStacktrace < 0)
//		{
//			_logger.debug("Can't find the position for columns ('HasShowplan'="+pos_HasStacktrace+", 'DbccStacktrace'="+pos_DbccStacktrace+")");
//			return;
//		}
//		
//		if (pos_BlockingOtherSpids < 0 || pos_BlockingSPID < 0)
//		{
//			_logger.debug("Can't find the position for columns ('BlockingOtherSpids'="+pos_BlockingOtherSpids+", 'BlockingSPID'="+pos_BlockingSPID+")");
//			return;
//		}
//		
//		if (pos_multiSampled < 0)
//		{
//			_logger.debug("Can't find the position for columns ('multiSampled'="+pos_multiSampled+")");
//			return;
//		}
//		
//		if (pos_StartTime < 0)
//		{
//			_logger.debug("Can't find the position for columns ('StartTime'="+pos_StartTime+")");
//			return;
//		}
//		
//		// Loop on all diffData rows
//		for (int rowId=0; rowId < counters.getRowCount(); rowId++) 
//		{
//			String thisRowPk = counters.getPkValue(rowId);
//			int prevPkRowId = (prevSample == null) ? -1 : prevSample.getRowNumberForPkValue(thisRowPk);
//			boolean prevPkExists = prevPkRowId >= 0;
//
//			Object o_waitEventId = counters.getValueAt(rowId, pos_WaitEventID);
//			Object o_SPID        = counters.getValueAt(rowId, pos_SPID);
//
//			if (prevPkExists)
//			{
//				Object o_this_StartTime = counters  .getValueAt(rowId,       pos_StartTime);
//				Object o_prev_StartTime = prevSample.getValueAt(prevPkRowId, pos_StartTime);
//
//				if (o_this_StartTime instanceof Timestamp && o_prev_StartTime instanceof Timestamp)
//				{
//					if (o_this_StartTime.equals(o_prev_StartTime))
//						counters.setValueAt("YES", rowId, pos_multiSampled);
//				}
//			}
//
//			if (o_waitEventId instanceof Number)
//			{
//				waitEventID	  = ((Number)o_waitEventId).intValue();
//
//				if (mtd.hasWaitEventDescription(waitEventID))
//				{
//					waitEventDesc = mtd.getWaitEventDescription(waitEventID);
//					waitClassDesc = mtd.getWaitEventClassDescription(waitEventID);
//				}
//				else
//				{
//					waitEventDesc = "";
//					waitClassDesc = "";
//				}
//
//				//row.set( pos_WaitEventDesc, waitEventDesc);
//				counters.setValueAt(waitEventDesc, rowId, pos_WaitEventDesc);
//				counters.setValueAt(waitClassDesc, rowId, pos_WaitClassDesc);
//			}
//
//			if (o_SPID instanceof Number)
//			{
//				int spid = ((Number)o_SPID).intValue();
//
//				String monSqlText    = "Not properly configured (need 'SQL batch capture' & 'max SQL text monitored').";
//				String dbccSqlText   = "User does not have: sa_role";
//				String procCallStack = "User does not have: sa_role";
//				String showplan      = "User does not have: sa_role";
//				String stacktrace    = "User does not have: sa_role";
//
//				if (getMonitorConfig("SQL batch capture") > 0 && getMonitorConfig("max SQL text monitored") > 0)
//				{
//					// monProcessSQLText; needs 'enable monitoring', 'SQL batch capture' and 'max SQL text monitored' configuration parameters for this monitoring table to collect data.
//					if (getMonSqltext)
//						monSqlText  = AseConnectionUtils.monSqlText(getCounterController().getMonConnection(), spid, true);
//					else
//						monSqlText = "This was disabled";
//					if (monSqlText == null)
//						monSqlText = "Not Available";
//				}
//				if (isServerRoleOrPermissionActive(AseConnectionUtils.SA_ROLE))
//				{
//					if (getDbccSqltext)
//						dbccSqlText  = AseConnectionUtils.dbccSqlText(getCounterController().getMonConnection(), spid, true);
//					else
//						dbccSqlText = "This was disabled";
//					if (dbccSqlText == null)
//						dbccSqlText = "Not Available";
//
//					if (getProcCallStack)
//						procCallStack  = AseConnectionUtils.monProcCallStack(getCounterController().getMonConnection(), spid, true);
//					else
//						procCallStack = "This was disabled";
//					if (procCallStack == null)
//						procCallStack = "Not Available";
//
//					if (getShowplan)
//						showplan = AseConnectionUtils.getShowplan(getCounterController().getMonConnection(), spid, "Showplan:", true);
//					else
//						showplan = "This was disabled";
//					if (showplan == null)
//						showplan = "Not Available";
//
//					if (getDbccStacktrace)
//						stacktrace = AseConnectionUtils.dbccStacktrace(getCounterController().getMonConnection(), spid, true, waitEventID);
//					else
//						stacktrace = "This was disabled";
//					if (stacktrace == null)
//						stacktrace = "Not Available";
//				}
//				boolean b = true;
//				b = !"This was disabled".equals(monSqlText)    && !"Not Available".equals(monSqlText)    && !monSqlText   .startsWith("Not properly configured");
//				counters.setValueAt(new Boolean(b), rowId, pos_HasMonSqlText);
//				counters.setValueAt(monSqlText,     rowId, pos_MonSqlText);
//
//				b = !"This was disabled".equals(dbccSqlText)   && !"Not Available".equals(dbccSqlText)   && !dbccSqlText  .startsWith("User does not have");
//				counters.setValueAt(new Boolean(b), rowId, pos_HasDbccSqlText);
//				counters.setValueAt(dbccSqlText,    rowId, pos_DbccSqlText);
//
//				b = !"This was disabled".equals(procCallStack) && !"Not Available".equals(procCallStack) && !procCallStack.startsWith("User does not have");
//				counters.setValueAt(new Boolean(b), rowId, pos_HasProcCallStack);
//				counters.setValueAt(procCallStack,  rowId, pos_ProcCallStack);
//
//				b = !"This was disabled".equals(showplan)      && !"Not Available".equals(showplan)      && !showplan     .startsWith("User does not have");
//				counters.setValueAt(new Boolean(b), rowId, pos_HasShowPlan);
//				counters.setValueAt(showplan,       rowId, pos_ShowPlanText);
//
//				b = !"This was disabled".equals(stacktrace)    && !"Not Available".equals(stacktrace)    && !stacktrace   .startsWith("User does not have");
//				counters.setValueAt(new Boolean(b), rowId, pos_HasStacktrace);
//				counters.setValueAt(stacktrace,     rowId, pos_DbccStacktrace);
//
//				// Get LIST of SPID's that I'm blocking
//				String blockingList = getBlockingListStr(counters, spid, pos_BlockingSPID, pos_SPID);
//
//				// This could be used to test that PCS.store() will truncate string size to the tables storage size
//				//blockingList += "'1:aaa:0', '1:bbb:0', '1:ccc:0', '1:ddd:0', '1:eee:0', '1:fff:0', '1:ggg:0', '1:hhh:0', '1:iii:0', '1:jjj:0', '1:kkk:0', '1:lll:0', '1:mmm:0', '1:nnn:0', '1:ooo:0', '1:ppp:0', '1:qqq:0', '1:rrr:0', '1:sss:0', '1:ttt:0', '1:uuu:0', '1:vvv:0', '1:wwww:0', '1:xxx:0', '1:yyy:0', '1:zzz:0' -end-";
//
//				counters.setValueAt(blockingList,      rowId, pos_BlockingOtherSpids);
//			}
//		}
//	}
	
//	@Override
//	protected Object clone() throws CloneNotSupportedException
//	{
//		// TODO Auto-generated method stub
//		return super.clone();
//	}

	private String getBlockingListStr(CounterSample counters, int spid, int pos_BlockingSPID, int pos_SPID)
	{
		StringBuilder sb = new StringBuilder();

		// Loop on all diffData rows
		int rows = counters.getRowCount();
		for (int rowId=0; rowId < rows; rowId++)
		{
			Object o_BlockingSPID = counters.getValueAt(rowId, pos_BlockingSPID);
			if (o_BlockingSPID instanceof Number)
			{
				Number thisRow = (Number)o_BlockingSPID;
				if (thisRow.intValue() == spid)
				{
					Object o_SPID = counters.getValueAt(rowId, pos_SPID);
					if (sb.length() == 0)
						sb.append(o_SPID);
					else
						sb.append(", ").append(o_SPID);
				}
			}
		}
		return sb.toString();
	}

	/** 
	 * Get number of rows to save/request ddl information for 
	 */
	@Override
	public int getMaxNumOfDdlsToPersist()
	{
		return Integer.MAX_VALUE; // Basically ALL Rows
	}

	/** 
	 * Get Column names to where DBName and ObjectName is called, this must always return at least a array with 2 strings. 
	 */
	@Override
	public String[] getDdlDetailsColNames()
	{
		String[] sa = {"dbname", "procname"};
		return sa;
	}
}
