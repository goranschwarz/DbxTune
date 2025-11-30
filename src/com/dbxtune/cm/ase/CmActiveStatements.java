/*******************************************************************************
 * Copyright (C) 2010-2025 Goran Schwarz
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
package com.dbxtune.cm.ase;

import java.awt.event.MouseEvent;
import java.lang.invoke.MethodHandles;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import javax.naming.NameNotFoundException;

import org.apache.commons.text.StringEscapeUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.dbxtune.ICounterController;
import com.dbxtune.IGuiController;
import com.dbxtune.alarm.AlarmHandler;
import com.dbxtune.alarm.events.AlarmEvent;
import com.dbxtune.alarm.events.AlarmEventBlockingLockAlarm;
import com.dbxtune.alarm.events.AlarmEventLongRunningStatement;
import com.dbxtune.cache.XmlPlanCache;
import com.dbxtune.cm.CmSettingsHelper;
import com.dbxtune.cm.CmSettingsHelper.RegExpInputValidator;
import com.dbxtune.cm.CounterSample;
import com.dbxtune.cm.CounterSetTemplates;
import com.dbxtune.cm.CounterSetTemplates.Type;
import com.dbxtune.cm.CountersModel;
import com.dbxtune.cm.ase.gui.CmActiveStatementsPanel;
import com.dbxtune.config.dict.MonTablesDictionary;
import com.dbxtune.config.dict.MonTablesDictionaryManager;
import com.dbxtune.gui.AsePlanViewer;
import com.dbxtune.gui.MainFrame;
import com.dbxtune.gui.TabularCntrPanel;
import com.dbxtune.pcs.PcsColumnOptions;
import com.dbxtune.pcs.PcsColumnOptions.ColumnType;
import com.dbxtune.pcs.PersistentCounterHandler;
import com.dbxtune.pcs.sqlcapture.ISqlCaptureBroker;
import com.dbxtune.pcs.sqlcapture.SqlCaptureBrokerAse;
import com.dbxtune.sql.conn.DbxConnection;
import com.dbxtune.sql.conn.info.DbmsVersionInfo;
import com.dbxtune.sql.conn.info.DbmsVersionInfoSybaseAse;
import com.dbxtune.sql.conn.info.DbxConnectionStateInfoAse.LockRecord;
import com.dbxtune.utils.AseConnectionUtils;
import com.dbxtune.utils.Configuration;
import com.dbxtune.utils.StringUtil;
import com.dbxtune.utils.Ver;

/**
 * @author Goran Schwarz (goran_schwarz@hotmail.com)
 */
public class CmActiveStatements
extends CountersModel
{
	private static final Logger _logger = LogManager.getLogger(MethodHandles.lookup().lookupClass());
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

	public static final long     NEED_SRV_VERSION = 0;
	public static final long     NEED_CE_VERSION  = 0;

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
		super(counterController, guiController,
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
	private static final String  PROP_PREFIX                      = CM_NAME;
	
	public static final String  PROPKEY_sample_showplan           = PROP_PREFIX + ".sample.showplan";
	public static final boolean DEFAULT_sample_showplan           = true;

	public static final String  PROPKEY_sample_monSqlText         = PROP_PREFIX + ".sample.monSqltext";
	public static final boolean DEFAULT_sample_monSqlText         = true;

	public static final String  PROPKEY_sample_dbccSqlText        = PROP_PREFIX + ".sample.dbccSqltext";
	public static final boolean DEFAULT_sample_dbccSqlText        = false;

	public static final String  PROPKEY_sample_procCallStack      = PROP_PREFIX + ".sample.procCallStack";
	public static final boolean DEFAULT_sample_procCallStack      = false;

	public static final String  PROPKEY_sample_dbccStacktrace     = PROP_PREFIX + ".sample.dbccStacktrace";
	public static final boolean DEFAULT_sample_dbccStacktrace     = false;
	
	public static final String  PROPKEY_sample_cachedPlanInXml    = PROP_PREFIX + ".sample.cachedPlanInXml";
	public static final boolean DEFAULT_sample_cachedPlanInXml    = false;
	
	public static final String  PROPKEY_sample_holdingLocks       = PROP_PREFIX + ".sample.holdingLocks";
	public static final boolean DEFAULT_sample_holdingLocks       = true;

	public static final String  PROPKEY_sample_holdingLocks_gt_SecondsWaiting = PROP_PREFIX + ".sample.holdingLocks.gt.SecondsWaiting";
	public static final int     DEFAULT_sample_holdingLocks_gt_SecondsWaiting = 0;
	
	public static final String  PROPKEY_sample_spidLocks          = PROP_PREFIX + ".sample.spidLocks";
	public static final boolean DEFAULT_sample_spidLocks          = true;

	public static final String  PROPKEY_sample_lastKnownSqlText   = PROP_PREFIX + ".sample.lastKnownSqlText";
	public static final boolean DEFAULT_sample_lastKnownSqlText   = true;

	private void addTrendGraphs()
	{
	}

	@Override
	protected TabularCntrPanel createGui()
	{
		return new CmActiveStatementsPanel(this);
	}
	
	@Override
	public void addMonTableDictForVersion(DbxConnection conn, DbmsVersionInfo versionInfo)
	{
		try 
		{
			MonTablesDictionary mtd = MonTablesDictionaryManager.getInstance();
			mtd.addColumn("monProcess",  "monSource",                  "<html>" +
			                                                               "This is the <i>source</i> of what part of the monitoring query the row resides from." +
			                                                               "<ul>" +
			                                                               "   <li>ACTIVE        - Statements that are currently executing SQL Statements at the server. <br>Basically rows in <code>monProcessStatement</code></li>" +
			                                                               "   <li>BLOCKER       - Statements that are holding <b>blocking</b> locks (stops other SPID's form working). <br><code>select ... from monProcess where WaitEventID = 250 and SPID in (select blocked from sysprocesses where blocked > 0)</code></li>" +
			                                                               "   <li>HOLDING-LOCKS - Statements that are holding <b>any</b> locks (may cause <i>blocking</i> locks soon and are <b>not</b> active in the server, WaitEventID=250). <br><code>select ... from monProcess where WaitEventID = 250 and SPID in (select spid from syslocks)</code></li>" +
			                                                               "</ul>" +
			                                                           "</html>");
			mtd.addColumn("monProcess",  "BlockingOtherSpids",         "This SPID is Blocking other SPID's from executing, because this SPID hold lock(s), that some other SPID wants to grab.");
			mtd.addColumn("monProcess",  "BlockingOthersMaxTimeInSec", "Max Time in Seconds this SPID has been Blocking other SPID's from executing, because this SPID hold lock(s), that some other SPID wants to grab.");
			mtd.addColumn("monProcess",  "multiSampled",               "<html>" +
			                                                               "This indicates that the PrimaryKey (SPID, monSource[, InstanceID]) has been in this table for more than one sample.<br>" +
			                                                               "Also 'StartTime' has to be the <b>same</b> as in the previous sample.<br>" +
			                                                               "The StartTime is the columns, which tells the start time for current SQL statement, or the individual statement inside a Stored Procedure." +
			                                                           "</html>");

			mtd.addColumn("monProcess",  "RowsAffectedDiff",           "Same as 'RowsAffected' but diff calculated");
			mtd.addColumn("monProcess",  "HasMonSqlText",              "Has values for: select SQLText from master.dbo.monProcessSQLText where SPID = <SPID>");
			mtd.addColumn("monProcess",  "MonSqlText",                                 "select SQLText from master.dbo.monProcessSQLText where SPID = <SPID>");
			mtd.addColumn("monProcess",  "HasDbccSqlText",             "Has values for: DBCC sqltext(<SPID>)");
			mtd.addColumn("monProcess",  "DbccSqlText",                                "DBCC sqltext(<SPID>)");
			mtd.addColumn("monProcess",  "HasShowPlan",                "Has values for: sp_showplan <SPID>, null, null, null");
			mtd.addColumn("monProcess",  "ShowPlanText",                               "sp_showplan <SPID>, null, null, null");
			mtd.addColumn("monProcess",  "HasStacktrace",              "Has values for: DBCC stacktrace(<SPID>)");
			mtd.addColumn("monProcess",  "DbccStacktrace",                             "DBCC stacktrace(<SPID>)");
			mtd.addColumn("monProcess",  "HasCachedPlanInXml",         "Has values for: show_cached_plan_in_xml(<planid>, 0, 0)");
			mtd.addColumn("monProcess",  "CachedPlanInXml",            "show_cached_plan_in_xml(<planid>, 0, 0)");
			mtd.addColumn("monProcess",  "HasProcCallStack",           "Has values for: select * from monProcessProcedures where SPID = <SPID>");
			mtd.addColumn("monProcess",  "ProcCallStack",                              "select * from monProcessProcedures where SPID = <SPID>");
			mtd.addColumn("monProcess",  "HasSpidLocks",               "This SPID holds the following locks in the database");
			mtd.addColumn("monProcess",  "SpidLockCount",              "This SPID holds number of locks in the database");
			mtd.addColumn("monProcess",  "SpidLocks",                  "This SPID holds the following locks in the database");
			mtd.addColumn("monProcess",  "HasBlockedSpidsInfo",        "Has values in column 'BlockedSpidsInfo'");
			mtd.addColumn("monProcess",  "BlockedSpidsInfo",           "If this SPID is BLOCKING other spid's, then here is a html-table of showplan for the Blocked spid's. (Note: 'Get Showplan' must be enabled)");
			mtd.addColumn("monProcess",  "SrvUserName",                "Login Name: SrvUserName = suser_name(P.ServerUserID)");
			mtd.addColumn("monProcess",  "HasLastKnownSqlText",        "Has values in column 'LastKnownSqlText'");
			mtd.addColumn("monProcess",  "LastKnownSqlText",           "Holds the last known SQL Text executed by this SPID, KPID and BatchId, NOTE: Recording Must be enabled.");
//			mtd.addColumn("monProcess",  "TranName",                   "The transaction name. NOTE: This is get from CmProcessActivity, column 'tran_name', so that CM must be active, otherwise the value will be NULL.");
			mtd.addColumn("monProcess",  "TranName",                   "The transaction name. (from sysprocesses.tran_name");
		}
		catch (NameNotFoundException e) 
		{
			_logger.warn("Problems in cm='" + CM_NAME + "', adding addMonTableDictForVersion. Caught: " + e); 
		//	System.out.println("Problems in cm='" + CM_NAME + "', adding addMonTableDictForVersion. Caught: " + e); 
		}
	}

	@Override
	public String[] getDependsOnConfigForVersion(DbxConnection conn, DbmsVersionInfo versionInfo)
	{
		long srvVersion = versionInfo.getLongVersion();

		if (srvVersion >= Ver.ver(15,7))
			return new String[] {"enable monitoring=1", "statement statistics active=1", "per object statistics active=1", "wait event timing=1"};

		return NEED_CONFIG;
	}

	@Override
	public Map<String, PcsColumnOptions> getPcsColumnOptions()
	{
		Map<String, PcsColumnOptions> map = super.getPcsColumnOptions();

		// No settings in the super, create one, and set it at super
		if (map == null)
		{
			map = new HashMap<>();
			map.put("MonSqlText"      , new PcsColumnOptions(ColumnType.DICTIONARY_COMPRESSION));
			map.put("DbccSqlText"     , new PcsColumnOptions(ColumnType.DICTIONARY_COMPRESSION));
			map.put("ProcCallStack"   , new PcsColumnOptions(ColumnType.DICTIONARY_COMPRESSION));
			map.put("ShowPlanText"    , new PcsColumnOptions(ColumnType.DICTIONARY_COMPRESSION));
			map.put("DbccStacktrace"  , new PcsColumnOptions(ColumnType.DICTIONARY_COMPRESSION));
			map.put("CachedPlanInXml" , new PcsColumnOptions(ColumnType.DICTIONARY_COMPRESSION));
			map.put("SpidLocks"       , new PcsColumnOptions(ColumnType.DICTIONARY_COMPRESSION));
			map.put("BlockedSpidsInfo", new PcsColumnOptions(ColumnType.DICTIONARY_COMPRESSION));

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

		pkCols.add("SPID");
		pkCols.add("monSource");

		return pkCols;
	}

	@Override
	public String getSqlForVersion(DbxConnection conn, DbmsVersionInfo versionInfo)
	{
		String cols1, cols2, cols3;
		cols1 = cols2 = cols3 = "";

		DbmsVersionInfoSybaseAse aseVersionInfo = (DbmsVersionInfoSybaseAse) versionInfo;
		long    srvVersion       = aseVersionInfo.getLongVersion();
		boolean isClusterEnabled = aseVersionInfo.isClusterEdition();

		////////////////////////////
		// NOTE: This Counter Model has 2 separete SQL selects
		//       1: The rows in: monProcessStatement
		//       2: The rows in: SPID's that might NOT be in monProcessStatement, BUT still are BLOCKING other spids
		// IMPORTANT: when you add a column to the FIRST you need to add it to the SECOND as well
		////////////////////////////

		boolean showHoldingLocks                 = Configuration.getCombinedConfiguration().getBooleanProperty(PROPKEY_sample_holdingLocks                  , DEFAULT_sample_holdingLocks);
		int     showHoldingLocksGtSecondsWaiting = Configuration.getCombinedConfiguration().getIntProperty    (PROPKEY_sample_holdingLocks_gt_SecondsWaiting, DEFAULT_sample_holdingLocks_gt_SecondsWaiting);

		// ASE 15.0.2 ESD#2
		String SrvUserName = "";

		if (srvVersion >= Ver.ver(15,0,2, 2))
		{
			SrvUserName = "SrvUserName = suser_name(P.ServerUserID), \n";
		}

		// ASE 15.7
		String HostName       = "";
		String ClientName     = "";
		String ClientHostName = "";
		String ClientApplName = "";
		String ase1570_nl     = "";

		if (srvVersion >= Ver.ver(15,7))
		{
			HostName       = "P.HostName, ";
			ClientName     = "P.ClientName, ";
			ClientHostName = "P.ClientHostName, ";
			ClientApplName = "P.ClientApplName, ";
			ase1570_nl     = "\n";
		}
		// ASE 16.0
		String ClientDriverVersion = ""; // The version of the connectivity driver used by the client program
		if (srvVersion >= Ver.ver(16,0))
		{
			ClientDriverVersion = "P.ClientDriverVersion, ";
		}

		// ASE 16.0 SP3
		String QueryOptimizationTime       = "";
		String QueryOptimizationTime_union = "";
		String ase160_sp3_nl               = "";
		if (srvVersion >= Ver.ver(16,0,0, 3)) // 16.0 SP3
		{
			QueryOptimizationTime       = "S.QueryOptimizationTime, ";
			QueryOptimizationTime_union = "QueryOptimizationTime=-1, ";    // Used be the seconds SQL Statement, just to create a "union"
			ase160_sp3_nl               = "\n";
		}
		
		String optGoalPlan = "";
		if (srvVersion >= Ver.ver(15,0,2))
		{
			optGoalPlan = "plan '(use optgoal allrows_dss)' \n";
		}

		if (isClusterEnabled)
		{
			cols1 += "S.InstanceID, ";
		}

		String dbNameCol  = "dbname=db_name(S.DBID)";
		if (srvVersion >= Ver.ver(15,0,2,6)) // just a guess what release this was introduced (not in 15.0.2.1, but at least in 15.0.2.6, I have not gathered info for 15022-15025])
		{
			dbNameCol  = "dbname=S.DBName";
		}

		cols1 += "monSource=convert(varchar(15),'ACTIVE'), \n" +
		         "P.SPID, P.KPID, \n" +
		         "multiSampled=convert(varchar(10),''), \n" +
		         "S.BatchID, S.LineNumber, \n" +
		         dbNameCol+", procname=isnull(isnull(object_name(S.ProcedureID,S.DBID),object_name(S.ProcedureID,2)),''), linenum=S.LineNumber, \n" +
		         SrvUserName +
//		         "P.Command, TranName = convert(varchar(30), NULL), P.Application, \n" +
		         "P.Command, TranName = (select x.tran_name from master.dbo.sysprocesses x where x.spid = P.SPID and x.kpid = P.KPID), P.Application, \n" +
		         HostName + ClientName + ClientHostName + ClientApplName + ClientDriverVersion + ase1570_nl +
		         "S.CpuTime, \n" +
		         "S.WaitTime, \n" +
		         "ExecTimeInMs    = CASE WHEN datediff(day, S.StartTime, getdate()) >= 24 THEN -1 ELSE  datediff(ms, S.StartTime, getdate()) END, \n" +               // protect from: Msg 535: Difference of two datetime fields caused overflow at runtime. above 24 days or so, the MS difference is overflowned
		         "UsefullExecTime = CASE WHEN datediff(day, S.StartTime, getdate()) >= 24 THEN -1 ELSE (datediff(ms, S.StartTime, getdate()) - S.WaitTime) END, \n" + // protect from: Msg 535: Difference of two datetime fields caused overflow at runtime. above 24 days or so, the MS difference is overflowned
		         QueryOptimizationTime + ase160_sp3_nl +
		         "BlockingOtherSpids=convert(varchar(512),''), BlockingOthersMaxTimeInSec=convert(int, 0), P.BlockingSPID, \n" +
		         "P.SecondsWaiting, P.WaitEventID, \n" +
		         "WaitClassDesc=convert(varchar(120),''), \n" +
		         "WaitEventDesc=convert(varchar(120),''), \n" +
		         "HasMonSqlText=convert(bit,0), HasDbccSqlText=convert(bit,0), HasProcCallStack=convert(bit,0), \n" +
		         "HasShowPlan=convert(bit,0), HasStacktrace=convert(bit,0), HasCachedPlanInXml=convert(bit,0), \n" +
		         "HasSpidLocks=convert(bit,0), \n" +
		         "HasBlockedSpidsInfo=convert(bit,0), \n" +
		         "HasLastKnownSqlText=convert(bit,0), \n" +
		         "SpidLockCount=convert(int,-1), \n" +
		         "S.MemUsageKB, S.PhysicalReads, S.LogicalReads, \n";
		cols2 += "";
		cols3 += "S.PagesModified, S.PacketsSent, S.PacketsReceived, S.NetworkPacketSize, \n" +
		         "S.PlansAltered, S.StartTime, S.PlanID, S.DBID, S.ProcedureID, \n" +
		         "P.SecondsConnected, ConnectionTime=dateadd(second, P.SecondsConnected * -1, getdate()), \n" +
		         "P.EngineNumber, P.NumChildren, \n" +
		         "MonSqlText=convert(text,null), \n" +
		         "DbccSqlText=convert(text,null), \n" +
		         "ProcCallStack=convert(text,null), \n" +
		         "ShowPlanText=convert(text,null), \n" +
		         "DbccStacktrace=convert(text,null), \n" +
		         "CachedPlanInXml=convert(text,null), \n" +
		         "SpidLocks=convert(text,null), \n" +
		         "BlockedSpidsInfo=convert(text,null), \n" +
		         "LastKnownSqlText=convert(text,null) \n" +
		         "";

		if (srvVersion >= Ver.ver(15,0,2) || (srvVersion >= Ver.ver(12,5,4) && srvVersion < Ver.ver(15,0)) )
		{
			cols2 += "RowsAffectedDiff = S.RowsAffected, S.RowsAffected, \n" +
			         "tempdb_name = db_name(tempdb_id(S.SPID)), \n" +
			         "pssinfo_tempdb_pages = convert(int, pssinfo(S.SPID, 'tempdb_pages')), \n";
		}

		// in 12.5.4 (esd#9) will produce an "empty" resultset using "S.SPID != @@spid"
		//                   so this will be a workaround for those releses below 15.0.0
		String whereSpidNotMe = "S.SPID != @@spid";
		if (srvVersion < Ver.ver(15,0))
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
//		if (srvVersion >= 15020) // just a guess what release this was introduced. 15020 is OK
//		if (srvVersion >= 1502000) // just a guess what release this was introduced. 15020 is OK
		if (srvVersion >= Ver.ver(15,0,2)) // just a guess what release this was introduced. 15020 is OK
		{
			dbNameCol  = "dbname=P.DBName";
		}

		// 16.0 SP2 PL5 and 15.7.0 SP138 the column is bigint
		String PhysicalReads = "PhysicalReads=-1";
		String LogicalReads  = "LogicalReads=-1";
		String PagesModified = "PagesModified=-1";
		if (srvVersion >= Ver.ver(16,0,0, 2,5) || (srvVersion >= Ver.ver(15,7,0, 138) && srvVersion < Ver.ver(16,0)) )
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
		         SrvUserName +
//		         "P.Command, TranName = convert(varchar(30), NULL), P.Application, \n" +
		         "P.Command, TranName = (select x.tran_name from master.dbo.sysprocesses x where x.spid = P.SPID and x.kpid = P.KPID), P.Application, \n" +
		         HostName + ClientName + ClientHostName + ClientApplName + ClientDriverVersion + ase1570_nl +
		         "CpuTime         = -1, \n" +
//		         "WaitTime        = -1, \n" +
		         "WaitTime        = 1000 * P.SecondsWaiting, \n" +
		         "ExecTimeInMs    = -1, \n" +
		         "UsefullExecTime = -1, \n" +
		         QueryOptimizationTime_union + ase160_sp3_nl +
		         "BlockingOtherSpids=convert(varchar(512),''), BlockingOthersMaxTimeInSec=convert(int, 0), P.BlockingSPID, \n" +
		         "P.SecondsWaiting, P.WaitEventID, \n" +
		         "WaitClassDesc=convert(varchar(120),''), \n" +
		         "WaitEventDesc=convert(varchar(120),''), \n" +
		         "HasMonSqlText=convert(bit,0), HasDbccSqlText=convert(bit,0), HasProcCallStack=convert(bit,0), \n" +
		         "HasShowPlan=convert(bit,0), HasStacktrace=convert(bit,0), HasCachedPlanInXml=convert(bit,0), \n" +
		         "HasSpidLocks=convert(bit,0), \n" +
		         "HasBlockedSpidsInfo=convert(bit,0), \n" +
		         "HasLastKnownSqlText=convert(bit,0), \n" +
		         "SpidLockCount=convert(int,-1), \n" +
		         "MemUsageKB=-1, "+PhysicalReads+", "+LogicalReads+", \n";
		cols2 += "";
		cols3 += PagesModified+", PacketsSent=-1, PacketsReceived=-1, NetworkPacketSize=-1, \n" +
		         "PlansAltered=-1, StartTime=convert(datetime,NULL), PlanID=-1, P.DBID, ProcedureID=-1, \n" +
		         "P.SecondsConnected, ConnectionTime=dateadd(second, P.SecondsConnected * -1, getdate()), \n" +
		         "P.EngineNumber, P.NumChildren, \n" +
		         "MonSqlText=convert(text,null), \n" +
		         "DbccSqlText=convert(text,null), \n" +
		         "ProcCallStack=convert(text,null), \n" +
		         "ShowPlanText=convert(text,null), \n" +
		         "DbccStacktrace=convert(text,null), \n" +
		         "CachedPlanInXml=convert(text,null), \n" +
		         "SpidLocks=convert(text,null), \n" +
		         "BlockedSpidsInfo=convert(text,null), \n" +
		         "LastKnownSqlText=convert(text,null) \n" +
		         "";
		if (srvVersion >= Ver.ver(15,0,2) || (srvVersion >= Ver.ver(12,5,4) && srvVersion < Ver.ver(15,0)) )
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
				"  and P.WaitEventID    = 250  -- 'waiting for input from the network' \n" + 
				"  and P.SecondsWaiting > " + showHoldingLocksGtSecondsWaiting + "    -- And has been at client side control for more than X seconds. \n" +
				"  and P.SPID in (select spid from master.dbo.syslocks) -- And holds any locks. \n" +
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
		
//		list.add(new CmSettingsHelper("Get Showplan",             getName()+".sample.showplan"        , Boolean.class, conf.getBooleanProperty(getName()+".sample.showplan"        , true ), true , "Do 'sp_showplan spid' on every row in the table."                                      ));
//		list.add(new CmSettingsHelper("Get Monitored SQL Text",   getName()+".sample.monSqltext"      , Boolean.class, conf.getBooleanProperty(getName()+".sample.monSqltext"      , true ), true , "Do 'select SQLText from monProcessSQLText where SPID=spid' on every row in the table." ));
//		list.add(new CmSettingsHelper("Get DBCC SQL Text",        getName()+".sample.dbccSqltext"     , Boolean.class, conf.getBooleanProperty(getName()+".sample.dbccSqltext"     , false), false, "Do 'dbcc sqltext(spid)' on every row in the table."                                    ));
//		list.add(new CmSettingsHelper("Get Procedure Call Stack", getName()+".sample.procCallStack"   , Boolean.class, conf.getBooleanProperty(getName()+".sample.procCallStack"   , false), false, "Do 'select * from monProcessProcedures where SPID=spid' on every row in the table."    ));
//		list.add(new CmSettingsHelper("Get ASE Stacktrace",       getName()+".sample.dbccStacktrace"  , Boolean.class, conf.getBooleanProperty(getName()+".sample.dbccStacktrace"  , false), false, "Do 'dbcc stacktrace(spid)' on every row in the table."                                 ));
//		list.add(new CmSettingsHelper("Get Cached Plan in XML",   getName()+".sample.cachedPlanInXml" , Boolean.class, conf.getBooleanProperty(getName()+".sample.cachedPlanInXml" , false), false, "Do 'select show_cached_plan_in_xml(planid, 0, 0)' on every row in the table."          ));
//		list.add(new CmSettingsHelper("Get SPID's holding locks", getName()+".sample.holdingLocks"    , Boolean.class, conf.getBooleanProperty(getName()+".sample.holdingLocks"    , true ), true , "Include SPID's that holds locks even if that are not active in the server."            ));

		list.add(new CmSettingsHelper("Get Showplan"                               , PROPKEY_sample_showplan                      , Boolean.class, conf.getBooleanProperty(PROPKEY_sample_showplan                      , DEFAULT_sample_showplan                      ), DEFAULT_sample_showplan                      , "Do 'sp_showplan spid' on every row in the table."                                      ));
		list.add(new CmSettingsHelper("Get Monitored SQL Text"                     , PROPKEY_sample_monSqlText                    , Boolean.class, conf.getBooleanProperty(PROPKEY_sample_monSqlText                    , DEFAULT_sample_monSqlText                    ), DEFAULT_sample_monSqlText                    , "Do 'select SQLText from monProcessSQLText where SPID=spid' on every row in the table." ));
		list.add(new CmSettingsHelper("Get DBCC SQL Text"                          , PROPKEY_sample_dbccSqlText                   , Boolean.class, conf.getBooleanProperty(PROPKEY_sample_dbccSqlText                   , DEFAULT_sample_dbccSqlText                   ), DEFAULT_sample_dbccSqlText                   , "Do 'dbcc sqltext(spid)' on every row in the table."                                    ));
		list.add(new CmSettingsHelper("Get Procedure Call Stack"                   , PROPKEY_sample_procCallStack                 , Boolean.class, conf.getBooleanProperty(PROPKEY_sample_procCallStack                 , DEFAULT_sample_procCallStack                 ), DEFAULT_sample_procCallStack                 , "Do 'select * from monProcessProcedures where SPID=spid' on every row in the table."    ));
		list.add(new CmSettingsHelper("Get ASE Stacktrace"                         , PROPKEY_sample_dbccStacktrace                , Boolean.class, conf.getBooleanProperty(PROPKEY_sample_dbccStacktrace                , DEFAULT_sample_dbccStacktrace                ), DEFAULT_sample_dbccStacktrace                , "Do 'dbcc stacktrace(spid)' on every row in the table."                                 ));
		list.add(new CmSettingsHelper("Get Cached Plan in XML"                     , PROPKEY_sample_cachedPlanInXml               , Boolean.class, conf.getBooleanProperty(PROPKEY_sample_cachedPlanInXml               , DEFAULT_sample_cachedPlanInXml               ), DEFAULT_sample_cachedPlanInXml               , "Do 'select show_cached_plan_in_xml(planid, 0, 0)' on every row in the table."          ));
		list.add(new CmSettingsHelper("Get SPID's holding locks"                   , PROPKEY_sample_holdingLocks                  , Boolean.class, conf.getBooleanProperty(PROPKEY_sample_holdingLocks                  , DEFAULT_sample_holdingLocks                  ), DEFAULT_sample_holdingLocks                  , "Include SPID's that holds locks even if that are not active in the server."            ));
		list.add(new CmSettingsHelper("Get SPID's holding locks, idle above X sec" , PROPKEY_sample_holdingLocks_gt_SecondsWaiting, Integer.class, conf.getIntProperty    (PROPKEY_sample_holdingLocks_gt_SecondsWaiting, DEFAULT_sample_holdingLocks_gt_SecondsWaiting), DEFAULT_sample_holdingLocks_gt_SecondsWaiting, "Include SPID's that holds locks even if that are not active in the server, AND has been idle for more than X seconds. Set this to -1 to see even shorter idle times."));
		list.add(new CmSettingsHelper("Get SPID Locks"                             , PROPKEY_sample_spidLocks                     , Boolean.class, conf.getBooleanProperty(PROPKEY_sample_spidLocks                     , DEFAULT_sample_spidLocks                     ), DEFAULT_sample_spidLocks                     , "Do 'select <i>someCols</i> from syslockinfo where spid = ?' on every row in the table. This will help us to diagnose what the current SQL statement is locking."));
		list.add(new CmSettingsHelper("Get Last Known SQL Text"                    , PROPKEY_sample_lastKnownSqlText              , Boolean.class, conf.getBooleanProperty(PROPKEY_sample_lastKnownSqlText              , DEFAULT_sample_lastKnownSqlText              ), DEFAULT_sample_lastKnownSqlText              , "Try to get Last Known SQL Text, NOTE: Recording Must be enabled"     ));


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
		
		if ("HasSpidLocks".equals(colName))
		{
			// Find 'ProcCallStack' column, is so get it and set it as the tool tip
			int pos_SpidLocks = findColumn("SpidLocks");
			if (pos_SpidLocks > 0)
			{
				Object cellVal = getValueAt(modelRow, pos_SpidLocks);
				if (cellVal instanceof String)
				{
					return "<html><pre>" + cellVal + "</pre></html>";
				}
			}
		}
		if ("SpidLocks".equals(colName))
		{
			return cellValue == null ? null : "<html><pre>" + cellValue + "</pre></html>";
		}

		if ("HasBlockedSpidsInfo".equals(colName))
		{
			// Find 'ProcCallStack' column, is so get it and set it as the tool tip
			int pos_BlockedSpidsInfo = findColumn("BlockedSpidsInfo");
			if (pos_BlockedSpidsInfo > 0)
			{
				Object cellVal = getValueAt(modelRow, pos_BlockedSpidsInfo);
				if (cellVal == null)
					return "<html>No value</html>";
				else
					return "<html><pre>" + cellVal + "</pre></html>";
			}
		}
		if ("BlockedSpidsInfo".equals(colName))
		{
			return cellValue == null ? null : "<html><pre>" + cellValue + "</pre></html>";
		}
		
		if ("HasLastKnownSqlText".equals(colName))
		{
			// Find 'ProcCallStack' column, is so get it and set it as the tool tip
			int pos_LastKnownSqlText = findColumn("LastKnownSqlText");
			if (pos_LastKnownSqlText > 0)
			{
				Object cellVal = getValueAt(modelRow, pos_LastKnownSqlText);
				if (cellVal == null)
					return "<html>No value</html>";
				else
					return "<html><pre>" + cellVal + "</pre></html>";
			}
		}
		if ("LastKnownSqlText".equals(colName))
		{
			return cellValue == null ? null : "<html><pre>" + cellValue + "</pre></html>";
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
		if      ("HasShowPlan"        .equals(colName)) return Boolean.class;
		else if ("HasMonSqlText"      .equals(colName)) return Boolean.class;
		else if ("HasDbccSqlText"     .equals(colName)) return Boolean.class;
		else if ("HasProcCallStack"   .equals(colName)) return Boolean.class;
		else if ("HasStacktrace"      .equals(colName)) return Boolean.class;
		else if ("HasCachedPlanInXml" .equals(colName)) return Boolean.class;
		else if ("HasSpidLocks"       .equals(colName)) return Boolean.class;
		else if ("HasBlockedSpidsInfo".equals(colName)) return Boolean.class;
		else if ("HasLastKnownSqlText".equals(colName)) return Boolean.class;
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
//		boolean getShowplan        = conf == null ? true : conf.getBooleanProperty(getName()+".sample.showplan",        true);
//		boolean getMonSqltext      = conf == null ? true : conf.getBooleanProperty(getName()+".sample.monSqltext",      true);
//		boolean getDbccSqltext     = conf == null ? false: conf.getBooleanProperty(getName()+".sample.dbccSqltext",     false);
//		boolean getProcCallStack   = conf == null ? true : conf.getBooleanProperty(getName()+".sample.procCallStack",   true);
//		boolean getDbccStacktrace  = conf == null ? false: conf.getBooleanProperty(getName()+".sample.dbccStacktrace",  false);
//		boolean getCachedPlanInXml = conf == null ? false: conf.getBooleanProperty(getName()+".sample.cachedPlanInXml", false);
		boolean getShowplan        = conf == null ? true : conf.getBooleanProperty(PROPKEY_sample_showplan        , DEFAULT_sample_showplan       );
		boolean getMonSqltext      = conf == null ? true : conf.getBooleanProperty(PROPKEY_sample_monSqlText      , DEFAULT_sample_monSqlText     );
		boolean getDbccSqltext     = conf == null ? false: conf.getBooleanProperty(PROPKEY_sample_dbccSqlText     , DEFAULT_sample_dbccSqlText    );
		boolean getProcCallStack   = conf == null ? true : conf.getBooleanProperty(PROPKEY_sample_procCallStack   , DEFAULT_sample_procCallStack  );
		boolean getDbccStacktrace  = conf == null ? false: conf.getBooleanProperty(PROPKEY_sample_dbccStacktrace  , DEFAULT_sample_dbccStacktrace );
		boolean getCachedPlanInXml = conf == null ? false: conf.getBooleanProperty(PROPKEY_sample_cachedPlanInXml , DEFAULT_sample_cachedPlanInXml);
		boolean getSpidLocks       = conf == null ? false: conf.getBooleanProperty(PROPKEY_sample_spidLocks       , DEFAULT_sample_spidLocks);
		boolean getLastKnownSqlText= conf == null ? false: conf.getBooleanProperty(PROPKEY_sample_lastKnownSqlText, DEFAULT_sample_lastKnownSqlText);

		// Where are various columns located in the Vector 
		int pos_WaitEventID                = -1, pos_WaitEventDesc   = -1, pos_WaitClassDesc = -1, pos_SPID = -1, pos_KPID = -1;
		int pos_HasShowPlan                = -1, pos_ShowPlanText    = -1;
		int pos_HasMonSqlText              = -1, pos_MonSqlText      = -1;
		int pos_HasDbccSqlText             = -1, pos_DbccSqlText     = -1;
		int pos_HasProcCallStack           = -1, pos_ProcCallStack   = -1;
		int pos_HasStacktrace              = -1, pos_DbccStacktrace  = -1;
		int pos_HasCachedPlanInXml         = -1, pos_CachedPlanInXml = -1, pos_procname = -1;
		int pos_HasSpidLocks               = -1, pos_SpidLocks       = -1, pos_SpidLockCount = -1;
		int pos_BlockingOtherSpids         = -1, pos_BlockingSPID    = -1;
		int pos_SecondsWaiting             = -1;
		int pos_BlockingOthersMaxTimeInSec = -1;
		int pos_multiSampled               = -1;
		int pos_StartTime                  = -1;
		int pos_BatchID                    = -1;
		int pos_HasBlockedSpidsInfo        = -1, pos_BlockedSpidsInfo = -1;
		int pos_HasLastKnownSqlText        = -1, pos_LastKnownSqlText = -1;
//		int pos_TranName                   = -1;
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
		if (colNames == null) 
			return;

		for (int colId=0; colId < colNames.size(); colId++) 
		{
			String colName = colNames.get(colId);
			if      (colName.equals("WaitEventID"))                pos_WaitEventID                = colId;
			else if (colName.equals("WaitEventDesc"))              pos_WaitEventDesc              = colId;
			else if (colName.equals("WaitClassDesc"))              pos_WaitClassDesc              = colId;
			else if (colName.equals("SPID"))                       pos_SPID                       = colId;
			else if (colName.equals("KPID"))                       pos_KPID                       = colId;
			else if (colName.equals("HasShowPlan"))                pos_HasShowPlan                = colId;
			else if (colName.equals("ShowPlanText"))               pos_ShowPlanText               = colId;
			else if (colName.equals("HasMonSqlText"))              pos_HasMonSqlText              = colId;
			else if (colName.equals("MonSqlText"))                 pos_MonSqlText                 = colId;
			else if (colName.equals("HasDbccSqlText"))             pos_HasDbccSqlText             = colId;
			else if (colName.equals("DbccSqlText"))                pos_DbccSqlText                = colId;
			else if (colName.equals("HasProcCallStack"))           pos_HasProcCallStack           = colId;
			else if (colName.equals("ProcCallStack"))              pos_ProcCallStack              = colId;
			else if (colName.equals("HasStacktrace"))              pos_HasStacktrace              = colId;
			else if (colName.equals("DbccStacktrace"))             pos_DbccStacktrace             = colId;
			else if (colName.equals("HasCachedPlanInXml"))         pos_HasCachedPlanInXml         = colId;
			else if (colName.equals("CachedPlanInXml"))            pos_CachedPlanInXml            = colId;
			else if (colName.equals("HasSpidLocks"))               pos_HasSpidLocks               = colId;
			else if (colName.equals("SpidLocks"))                  pos_SpidLocks                  = colId;
			else if (colName.equals("SpidLockCount"))              pos_SpidLockCount              = colId;
			else if (colName.equals("procname"))                   pos_procname                   = colId;
			else if (colName.equals("BlockingOtherSpids"))         pos_BlockingOtherSpids         = colId;
			else if (colName.equals("BlockingSPID"))               pos_BlockingSPID               = colId;
			else if (colName.equals("SecondsWaiting"))             pos_SecondsWaiting             = colId;
			else if (colName.equals("BlockingOthersMaxTimeInSec")) pos_BlockingOthersMaxTimeInSec = colId;
			else if (colName.equals("multiSampled"))               pos_multiSampled               = colId;
			else if (colName.equals("StartTime"))                  pos_StartTime                  = colId;
			else if (colName.equals("BatchID"))                    pos_BatchID                    = colId;
			else if (colName.equals("HasBlockedSpidsInfo"))        pos_HasBlockedSpidsInfo        = colId;
			else if (colName.equals("BlockedSpidsInfo"))           pos_BlockedSpidsInfo           = colId;
			else if (colName.equals("HasLastKnownSqlText"))        pos_HasLastKnownSqlText        = colId;
			else if (colName.equals("LastKnownSqlText"))           pos_LastKnownSqlText           = colId;
//			else if (colName.equals("TranName"))                   pos_TranName                   = colId;
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
		
		if (pos_HasSpidLocks < 0 || pos_SpidLocks < 0 || pos_SpidLockCount < 0)
		{
			_logger.debug("Can't find the position for columns ('HasSpidLocks'="+pos_HasSpidLocks+", 'SpidLocks'="+pos_SpidLocks+", 'SpidLockCount'="+pos_SpidLockCount+")");
			return;
		}
		
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
		
		if (pos_SecondsWaiting < 0)
		{
			_logger.debug("Can't find the position for columns ('SecondsWaiting'="+pos_SecondsWaiting+")");
			return;
		}
		
		if (pos_BlockingOthersMaxTimeInSec < 0)
		{
			_logger.debug("Can't find the position for columns ('BlockingOthersMaxTimeInSec'="+pos_BlockingOthersMaxTimeInSec+")");
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
		
		if (pos_BlockedSpidsInfo < 0)
		{
			_logger.debug("Can't find the position for columns ('BlockedSpidsInfo'="+pos_BlockedSpidsInfo+")");
			return;
		}

		if (pos_HasBlockedSpidsInfo < 0)
		{
			_logger.debug("Can't find the position for columns ('HasBlockedSpidsInfo'="+pos_HasBlockedSpidsInfo+")");
			return;
		}

		if (pos_HasLastKnownSqlText < 0)
		{
			_logger.debug("Can't find the position for columns ('HasLastKnownSqlText'="+pos_HasLastKnownSqlText+")");
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
				counters.setValueAt(Boolean.valueOf(b),  rowId, pos_HasCachedPlanInXml);
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
				String spidLocks       = "This was disabled";
				int    spidLockCount   = -1;

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
					{
						// No need to get call stack if it's a Statement Cache or Dynamic SQL entry... because they do not call other procs...
						if (o_procname != null && !o_procname.equals("") && !((String)o_procname).startsWith("*s"))
							procCallStack  = AseConnectionUtils.monProcCallStack(getCounterController().getMonConnection(), spid, true);
					}
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
				
				if (getSpidLocks)
				{
					List<LockRecord> lockList = AseConnectionUtils.getLockSummaryForSpid(getCounterController().getMonConnection(), spid);
					spidLocks = AseConnectionUtils.getLockSummaryForSpid(lockList, true, false);
					if (spidLocks == null)
						spidLocks = "Not Available";
					
					spidLockCount = 0;
					for (LockRecord lockRecord : lockList)
					{
						spidLockCount += lockRecord._lockCount;
					}
				}
				
				// LastKnownSqlText
				if (getLastKnownSqlText && PersistentCounterHandler.hasInstance())
				{
					ISqlCaptureBroker sqlCaptureBroker = PersistentCounterHandler.getInstance().getSqlCaptureBroker();
					if (sqlCaptureBroker != null && sqlCaptureBroker instanceof SqlCaptureBrokerAse)
					{
						SqlCaptureBrokerAse aseSqlCaptureBroker = (SqlCaptureBrokerAse) sqlCaptureBroker;

						int kpid    = counters.getValueAsInteger(rowId, pos_KPID   , -1);
						int batchId = counters.getValueAsInteger(rowId, pos_BatchID, -1);

						if (kpid != -1 && batchId != -1)
						{
							boolean getAllAvailableBatches = true;
							String lastKnownSqlText = aseSqlCaptureBroker.getSqlText(spid, kpid, batchId, getAllAvailableBatches);
							if (lastKnownSqlText != null)
							{
								counters.setValueAt(true            , rowId, pos_HasLastKnownSqlText);
								counters.setValueAt(lastKnownSqlText, rowId, pos_LastKnownSqlText);
							}
						}
					}
				}

//				// Get 'tran_name' from CmProcessActivity by "SPID" and store it in 'TranName'
//				boolean getTranName = true;
//				CountersModel cmProcessActivity = getCounterController().getCmByName(CmProcessActivity.CM_NAME);
//				if (getTranName && cmProcessActivity != null)
//				{
//					int tmpPos_tran_name = cmProcessActivity.findColumn("tran_name");
//					int[] spidRowIds = cmProcessActivity.getAbsRowIdsWhere("SPID", spid);
//					if (spidRowIds != null && spidRowIds.length == 1 && tmpPos_tran_name != -1)
//					{
//						String tran_name = cmProcessActivity.getAbsString(spidRowIds[0], tmpPos_tran_name);
//						counters.setValueAt(tran_name, rowId, pos_TranName);
//					}
//				}
				// The above "lookup" is now done i SQL:  TranName = (select x.tran_name from master.dbo.sysprocesses x where x.spid = P.SPID and x.kpid = P.KPID)
				// It shouldn't be a big overhead, since number of Active SQL Statements should be LOW (between 0 and a handful)

				boolean b = true;
				b = !"This was disabled".equals(monSqlText)    && !"Not Available".equals(monSqlText)    && !monSqlText   .startsWith("Not properly configured");
				counters.setValueAt(Boolean.valueOf(b), rowId, pos_HasMonSqlText);
				counters.setValueAt(monSqlText,         rowId, pos_MonSqlText);

				b = !"This was disabled".equals(dbccSqlText)   && !"Not Available".equals(dbccSqlText)   && !dbccSqlText  .startsWith("User does not have");
				counters.setValueAt(Boolean.valueOf(b), rowId, pos_HasDbccSqlText);
				counters.setValueAt(dbccSqlText,        rowId, pos_DbccSqlText);

				b = !"This was disabled".equals(procCallStack) && !"Not Available".equals(procCallStack) && !procCallStack.startsWith("User does not have");
				counters.setValueAt(Boolean.valueOf(b), rowId, pos_HasProcCallStack);
				counters.setValueAt(procCallStack,      rowId, pos_ProcCallStack);

				b = !"This was disabled".equals(showplan)      && !"Not Available".equals(showplan)      && !showplan     .startsWith("User does not have");
				counters.setValueAt(Boolean.valueOf(b), rowId, pos_HasShowPlan);
				counters.setValueAt(showplan,           rowId, pos_ShowPlanText);

				b = !"This was disabled".equals(stacktrace)    && !"Not Available".equals(stacktrace)    && !stacktrace   .startsWith("User does not have");
				counters.setValueAt(Boolean.valueOf(b), rowId, pos_HasStacktrace);
				counters.setValueAt(stacktrace,         rowId, pos_DbccStacktrace);

				b = !"This was disabled".equals(spidLocks) && !"Not Available".equals(spidLocks);
				counters.setValueAt(Boolean.valueOf(b), rowId, pos_HasSpidLocks);
				counters.setValueAt(spidLocks,          rowId, pos_SpidLocks);
				counters.setValueAt(spidLockCount,      rowId, pos_SpidLockCount);

				// Get LIST of SPID's that I'm blocking
				String blockingList = getBlockingListStrForSpid(counters, spid, pos_BlockingSPID, pos_SPID);

				// Get MaxBlockingTime of SPID's that I'm blocking
				int BlockingOthersMaxTimeInSec = getMaxBlockingTimeInSecForSpid(counters, spid, pos_BlockingSPID, pos_SecondsWaiting);

				// This could be used to test that PCS.store() will truncate string size to the tables storage size
				//blockingList += "'1:aaa:0', '1:bbb:0', '1:ccc:0', '1:ddd:0', '1:eee:0', '1:fff:0', '1:ggg:0', '1:hhh:0', '1:iii:0', '1:jjj:0', '1:kkk:0', '1:lll:0', '1:mmm:0', '1:nnn:0', '1:ooo:0', '1:ppp:0', '1:qqq:0', '1:rrr:0', '1:sss:0', '1:ttt:0', '1:uuu:0', '1:vvv:0', '1:wwww:0', '1:xxx:0', '1:yyy:0', '1:zzz:0' -end-";

				counters.setValueAt(blockingList,               rowId, pos_BlockingOtherSpids);
				counters.setValueAt(BlockingOthersMaxTimeInSec, rowId, pos_BlockingOthersMaxTimeInSec);

			} // end: o_SPID instanceof Number
		} // end: Loop on all diffData rows
		
		// Loop a seconds time, This to:
		// Fill in the column 'BlockedSpidsInfo'
		// - If this SPID is a BLOCKER - the root cause of blocking other SPID's
		//   Then get: get already collected Showplans etc for SPID's that are BLOCKED (the victims)
		// This will be helpful (to see both side of the story; ROOT cause and the VICTIMS) in a alarm message
		if (pos_BlockedSpidsInfo >= 0)
		{
			for (int rowId=0; rowId < counters.getRowCount(); rowId++) 
			{
				Object o_BlockingOtherSpids = counters.getValueAt(rowId, pos_BlockingOtherSpids);

				// MAYBE TODO: possibly check if the 'monSource' is of type "BLOCKER", before getting: getBlockedSpidInfo()
				
				if (o_BlockingOtherSpids != null && o_BlockingOtherSpids instanceof String)
				{
					String str_BlockingOtherSpids = (String) o_BlockingOtherSpids;
					if (StringUtil.hasValue(str_BlockingOtherSpids))
					{
						List<String> list_BlockingOtherSpids = StringUtil.parseCommaStrToList(str_BlockingOtherSpids);
						
						String blockedInfoStr = getBlockedSpidInfo(counters, pos_SPID, list_BlockingOtherSpids, pos_MonSqlText, pos_ShowPlanText);
						if (StringUtil.hasValue(blockedInfoStr))
						{
							counters.setValueAt(blockedInfoStr, rowId, pos_BlockedSpidsInfo);
							counters.setValueAt(true,           rowId, pos_HasBlockedSpidsInfo);
						}
					}
				}
			} // end: loop all rows
		}

	} // end: method


	private String getBlockingListStrForSpid(CounterSample counters, int spid, int pos_BlockingSPID, int pos_SPID)
	{
		StringBuilder sb = new StringBuilder();

		// Loop on all diffData rows
		int rows = counters.getRowCount();
		for (int rowId=0; rowId < rows; rowId++)
		{
			Object o_BlockingSPID = counters.getValueAt(rowId, pos_BlockingSPID);
			if (o_BlockingSPID instanceof Number)
			{
				Number BlockingSPID = (Number)o_BlockingSPID;
				if (BlockingSPID.intValue() == spid)
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

	private int getMaxBlockingTimeInSecForSpid(CounterSample counters, int spid, int pos_BlockingSPID, int pos_SecondsWaiting)
	{
		int maxBlockingTimeInSec = 0;

		// Loop on all diffData rows
		int rows = counters.getRowCount();
		for (int rowId=0; rowId < rows; rowId++)
		{
			Object o_BlockingSPID = counters.getValueAt(rowId, pos_BlockingSPID);
			if (o_BlockingSPID instanceof Number)
			{
				Number BlockingSPID = (Number)o_BlockingSPID;
				if (BlockingSPID.intValue() == spid)
				{
//					Object o_SPID           = counters.getValueAt(rowId, pos_SPID);
					Object o_SecondsWaiting = counters.getValueAt(rowId, pos_SecondsWaiting);
					if (o_SecondsWaiting instanceof Number)
					{
						Number SecondsWaiting = (Number)o_SecondsWaiting;
						maxBlockingTimeInSec = Math.max(SecondsWaiting.intValue(), maxBlockingTimeInSec);
					}
				}
			}
		}
		
		return maxBlockingTimeInSec;
	}

	private String getBlockedSpidInfo(CounterSample counters, int pos_SPID, List<String> blockedSpidList, int pos_MonSqlText, int pos_ShowPlanText)
	{
		if (blockedSpidList == null)   return "";
		if (blockedSpidList.isEmpty()) return "";

		if (pos_SPID         < 0) return "";
		if (pos_MonSqlText   < 0) return "";
		if (pos_ShowPlanText < 0) return "";


		StringBuilder sb = new StringBuilder(1024);

		sb.append("<TABLE BORDER=1 class='dbx-table-basic'>\n");
		sb.append("  <TR> <TH>Blocked SPID</TH> <TH>MonSqlText</TH> <TH>Showplan</TH> </TR>\n");
		
		int addCount = 0;
		
		// Loop the blockedSpidList
		for (String blockedSpidStr : blockedSpidList)
		{
			int blockedSpid = StringUtil.parseInt(blockedSpidStr, Integer.MIN_VALUE);
			if (blockedSpid == Integer.MIN_VALUE)
				continue;
			
			int rowId = getRowIdForSpid(counters, blockedSpid, pos_SPID);

			if (rowId != -1)
			{
				addCount++;
				
				Object o_monSqlText = counters.getValueAt(rowId, pos_MonSqlText);
				Object o_showplan   = counters.getValueAt(rowId, pos_ShowPlanText);
				
				String monSqlText = o_monSqlText == null ? "" : o_monSqlText.toString();
				String showplan   = o_showplan   == null ? "" : o_showplan  .toString();
				
				if (monSqlText.startsWith("<html>") && monSqlText.endsWith("</html>"))
				{
					monSqlText = monSqlText.substring("<html>".length());
					monSqlText = monSqlText.substring(0, monSqlText.length() - "</html>".length());
				}

				if (showplan.startsWith("<html>") && showplan.endsWith("</html>"))
				{
					showplan = showplan.substring("<html>".length());
					showplan = showplan.substring(0, showplan.length() - "</html>".length());
				}

				sb.append("  <TR> <TD><B>").append(blockedSpid).append("</B></TD> <TD>").append(monSqlText).append("</TD> <TD>").append(showplan).append("</TD> </TR>\n");
			}
		}

		sb.append("</TABLE>\n");
		
		if (addCount == 0)
			return "";

		return sb.toString();
	}
	
	private int getRowIdForSpid(CounterSample counters, int spidToFind, int pos_SPID)
	{
		// Loop on all diffData rows
		int rows = counters.getRowCount();
		for (int rowId=0; rowId < rows; rowId++)
		{
			Object o_SPID = counters.getValueAt(rowId, pos_SPID);
			if (o_SPID instanceof Number)
			{
				Number SPID = (Number)o_SPID;
				if (SPID.intValue() == spidToFind)
				{
					return rowId;
				}
			}
		}
		return -1;
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

		for (int r=0; r<cm.getRateRowCount(); r++)
		{
			//-------------------------------------------------------
			// BlockingOthersMaxTimeInSec 
			//-------------------------------------------------------
			if (isSystemAlarmsForColumnEnabledAndInTimeRange("BlockingOthersMaxTimeInSec"))
			{
				Object o_BlockingOthersMaxTimeInSec = cm.getRateValue(r, "BlockingOthersMaxTimeInSec");
				if (o_BlockingOthersMaxTimeInSec != null && o_BlockingOthersMaxTimeInSec instanceof Number)
				{
					int BlockingOthersMaxTimeInSec = ((Number)o_BlockingOthersMaxTimeInSec).intValue();
					
					List<String> BlockingOtherSpidsList = StringUtil.commaStrToList(cm.getRateValue(r, "BlockingOtherSpids") + "");
					String BlockingOtherSpidsStr = BlockingOtherSpidsList + "";
					int    blockCount            = BlockingOtherSpidsList.size();

					int threshold = Configuration.getCombinedConfiguration().getIntProperty(PROPKEY_alarm_BlockingOthersMaxTimeInSec, DEFAULT_alarm_BlockingOthersMaxTimeInSec);

					if (debugPrint || _logger.isDebugEnabled())
						System.out.println("##### sendAlarmRequest("+cm.getName()+"): threshold="+threshold+", BlockingOthersMaxTimeInSec='"+BlockingOthersMaxTimeInSec+"', BlockingOtherSpidsList="+BlockingOtherSpidsList);

					if (BlockingOthersMaxTimeInSec > threshold)
					{
						// Get various names to "skip"
						String currentDbname   = cm.getAbsString(r, "dbname");
						String currentCommand  = cm.getAbsString(r, "Command");
						String currentAppName  = cm.getAbsString(r, "Application");
						String currentUserName = cm.getAbsString(r, "SrvUserName");  // if column name isn't found "" (empty string) will be returned
						String currentHostName = cm.getAbsString(r, "HostName");     // if column name isn't found "" (empty string) will be returned
						
						// Get config 'skip some transaction names'
						String skipDbnameRegExp   = Configuration.getCombinedConfiguration().getProperty(PROPKEY_alarm_BlockingOthersMaxTimeInSecSkipDbname     , DEFAULT_alarm_BlockingOthersMaxTimeInSecSkipDbname);
						String skipCommandRegExp  = Configuration.getCombinedConfiguration().getProperty(PROPKEY_alarm_BlockingOthersMaxTimeInSecSkipCommand    , DEFAULT_alarm_BlockingOthersMaxTimeInSecSkipCommand);
						String skipAppNameRegExp  = Configuration.getCombinedConfiguration().getProperty(PROPKEY_alarm_BlockingOthersMaxTimeInSecSkipApplication, DEFAULT_alarm_BlockingOthersMaxTimeInSecSkipApplication);
						String skipUserNameRegExp = Configuration.getCombinedConfiguration().getProperty(PROPKEY_alarm_BlockingOthersMaxTimeInSecSkipSrvUserName, DEFAULT_alarm_BlockingOthersMaxTimeInSecSkipSrvUserName);
						String skipHostNameRegExp = Configuration.getCombinedConfiguration().getProperty(PROPKEY_alarm_BlockingOthersMaxTimeInSecSkipHostName   , DEFAULT_alarm_BlockingOthersMaxTimeInSecSkipHostName);

						// note: this must be set to true at start, otherwise all below rules will be disabled (it "stops" processing at first doAlarm==false)
						boolean doAlarm = true;

						// The below could have been done with nested if(skip-name), if(skip-prog), if(skip-user), if(skip-host) doAlarm=true; 
						// Below is more readable, from a variable context point-of-view, but HARDER to understand
						// to *continue*: doAlarm needs to be true AND (regExp is empty OR not-matching)
						doAlarm = (doAlarm && (StringUtil.isNullOrBlank(skipDbnameRegExp)   || ! currentDbname  .matches(skipDbnameRegExp)));
						doAlarm = (doAlarm && (StringUtil.isNullOrBlank(skipCommandRegExp)  || ! currentCommand .matches(skipCommandRegExp)));
						doAlarm = (doAlarm && (StringUtil.isNullOrBlank(skipAppNameRegExp)  || ! currentAppName .matches(skipAppNameRegExp)));
						doAlarm = (doAlarm && (StringUtil.isNullOrBlank(skipUserNameRegExp) || ! currentUserName.matches(skipUserNameRegExp)));
						doAlarm = (doAlarm && (StringUtil.isNullOrBlank(skipHostNameRegExp) || ! currentHostName.matches(skipHostNameRegExp)));

						if (doAlarm)
						{
							int spid = cm.getRateValueAsInteger(r, "SPID", -1);

							String extendedDescText = cm.toTextTableString(DATA_RATE, r);
							String extendedDescHtml = "<br><b>Below is all columns from 'Active Statements' for SPID=" + spid + "</b><br>" 
									+ cm.toHtmlTableString(DATA_RATE, r, true, false, false);

							// get ProcessActivity for 'SPID' (this SPID)
							CountersModel cmProcessActivity = getCounterController().getCmByName(CmProcessActivity.CM_NAME);
							if (cmProcessActivity != null)
							{
								int[] cmRows = cmProcessActivity.getAbsRowIdsWhere("SPID", spid);
								if (cmRows != null && cmRows.length > 0)
								{
									extendedDescHtml += "<br><br><b>Process Activity for SPID=" + spid + "</b>";
									for (int cmr=0; cmr<cmRows.length; cmr++)
									{
										extendedDescHtml += "<br><br>" + cmProcessActivity.toHtmlTableString(DATA_ABS, cmRows[cmr], true, false, false);
									}
								}
								else
								{
									extendedDescHtml += "<br><br><b>No record was found in 'Process Activity' for SPID=" + spid + "</b>";
								}
							}

							// get ProcessActivity for 'SPID' (SPID's that are blocked by this SPID)
							if (cmProcessActivity != null)
							{
								for (String bSpidStr : BlockingOtherSpidsList)
								{
									int blockedSpid = StringUtil.parseInt(bSpidStr, -1);
									if (blockedSpid != -1)
									{
										int[] cmRows = cmProcessActivity.getAbsRowIdsWhere("SPID", blockedSpid);
										if (cmRows != null && cmRows.length > 0)
										{
											extendedDescHtml += "<br><br><b>Process Activity for BLOCKED/WAITING SPID=" + blockedSpid + "</b>";
											for (int cmr=0; cmr<cmRows.length; cmr++)
											{
												int blockedSpidRowId = cmRows[cmr];
												extendedDescHtml += "<br><br>" + cmProcessActivity.toHtmlTableString(DATA_ABS, blockedSpidRowId, true, false, false);

												// Get 'LastKnownSqlText'
												if (true && PersistentCounterHandler.hasInstance() )
												{
													ISqlCaptureBroker sqlCaptureBroker = PersistentCounterHandler.getInstance().getSqlCaptureBroker();
													if (sqlCaptureBroker != null && sqlCaptureBroker instanceof SqlCaptureBrokerAse)
													{
														SqlCaptureBrokerAse aseSqlCaptureBroker = (SqlCaptureBrokerAse) sqlCaptureBroker;

														int kpid    = cmProcessActivity.getAbsValueAsInteger(blockedSpidRowId, "KPID"   , -1);
														int batchId = cmProcessActivity.getAbsValueAsInteger(blockedSpidRowId, "BatchID", -1);

														if (kpid != -1 && batchId != -1)
														{
															boolean getAllAvailableBatches = true;
															String lastKnownSqlText = aseSqlCaptureBroker.getSqlText(blockedSpid, kpid, batchId, getAllAvailableBatches);

															extendedDescHtml += "<br><br><b>Last Known SQL Text for BLOCKED/WAITING SPID=" + blockedSpid + "</b>"
																	+ "<br>"
																	+ "<pre><code>"
																	+ StringEscapeUtils.escapeHtml4(lastKnownSqlText)
																	+ "</code></pre>"
																	+ "<br><br>";
														}
													}
												}
											}
										}
									}
								}
							}
							
							// Finally create the alarm
							AlarmEvent ae = new AlarmEventBlockingLockAlarm(cm, threshold, spid, BlockingOthersMaxTimeInSec, BlockingOtherSpidsStr, blockCount);
							ae.setExtendedDescription(extendedDescText, extendedDescHtml);
							
							alarmHandler.addAlarm( ae );
						}
					}
				}
			}

			//-------------------------------------------------------
			// StatementExecInSec 
			//-------------------------------------------------------
			if (isSystemAlarmsForColumnEnabledAndInTimeRange("StatementExecInSec"))
			{
				String monSource      = cm.getRateString(r, "monSource");
				Object o_ExecTimeInMs = cm.getRateValue (r, "ExecTimeInMs");

				if ("ACTIVE".equals(monSource) && o_ExecTimeInMs != null && o_ExecTimeInMs instanceof Number)
				{
					int ExecTimeInInSec = ((Number)o_ExecTimeInMs).intValue() / 1000;

					int threshold = Configuration.getCombinedConfiguration().getIntProperty(PROPKEY_alarm_StatementExecInSec, DEFAULT_alarm_StatementExecInSec);

					if (debugPrint || _logger.isDebugEnabled())
						System.out.println("##### sendAlarmRequest("+cm.getName()+"): threshold="+threshold+", ExecTimeInInSec='"+ExecTimeInInSec+"'.");

					if (ExecTimeInInSec > threshold)
					{
						// Get various fields passed to the AlarmEvent
						String StatementStartTime = cm.getRateString(r, "StartTime");
//						String currentTranName    = "-unknown-";
						String currentTranName    = cm.getRateString(r, "TranName");

						// Get various names to "skip"
						String currentDbname   = cm.getRateString(r, "dbname");
						String currentCommand  = cm.getRateString(r, "Command");
						String currentAppName  = cm.getRateString(r, "Application");
						String currentUserName = cm.getRateString(r, "SrvUserName");  // if column name isn't found "" (empty string) will be returned
						String currentHostName = cm.getRateString(r, "HostName");     // if column name isn't found "" (empty string) will be returned
						
						// Get config 'skip some transaction names'
						String skipDbnameRegExp   = Configuration.getCombinedConfiguration().getProperty(PROPKEY_alarm_StatementExecInSecSkipDbname     , DEFAULT_alarm_StatementExecInSecSkipDbname);
						String skipCommandRegExp  = Configuration.getCombinedConfiguration().getProperty(PROPKEY_alarm_StatementExecInSecSkipCommand    , DEFAULT_alarm_StatementExecInSecSkipCommand);
						String skipAppNameRegExp  = Configuration.getCombinedConfiguration().getProperty(PROPKEY_alarm_StatementExecInSecSkipApplication, DEFAULT_alarm_StatementExecInSecSkipApplication);
						String skipUserNameRegExp = Configuration.getCombinedConfiguration().getProperty(PROPKEY_alarm_StatementExecInSecSkipSrvUserName, DEFAULT_alarm_StatementExecInSecSkipSrvUserName);
						String skipHostNameRegExp = Configuration.getCombinedConfiguration().getProperty(PROPKEY_alarm_StatementExecInSecSkipHostName   , DEFAULT_alarm_StatementExecInSecSkipHostName);

						// note: this must be set to true at start, otherwise all below rules will be disabled (it "stops" processing at first doAlarm==false)
						boolean doAlarm = true;

						// The below could have been done with nested if(skip-name), if(skip-prog), if(skip-user), if(skip-host) doAlarm=true; 
						// Below is more readable, from a variable context point-of-view, but HARDER to understand
						// to *continue*: doAlarm needs to be true AND (regExp is empty OR not-matching)
						doAlarm = (doAlarm && (StringUtil.isNullOrBlank(skipDbnameRegExp)   || ! currentDbname  .matches(skipDbnameRegExp)));
						doAlarm = (doAlarm && (StringUtil.isNullOrBlank(skipCommandRegExp)  || ! currentCommand .matches(skipCommandRegExp)));
						doAlarm = (doAlarm && (StringUtil.isNullOrBlank(skipAppNameRegExp)  || ! currentAppName .matches(skipAppNameRegExp)));
						doAlarm = (doAlarm && (StringUtil.isNullOrBlank(skipUserNameRegExp) || ! currentUserName.matches(skipUserNameRegExp)));
						doAlarm = (doAlarm && (StringUtil.isNullOrBlank(skipHostNameRegExp) || ! currentHostName.matches(skipHostNameRegExp)));

						if (doAlarm)
						{
							String extendedDescText = cm.toTextTableString(DATA_RATE, r);
							String extendedDescHtml = cm.toHtmlTableString(DATA_RATE, r, true, false, false);
							
							AlarmEvent ae = new AlarmEventLongRunningStatement(cm, threshold, ExecTimeInInSec, StatementStartTime, currentDbname, currentUserName, currentCommand, currentTranName);
							ae.setExtendedDescription(extendedDescText, extendedDescHtml);
							
							alarmHandler.addAlarm( ae );
						}
					}
				}
			}

		} // end: loop rows
	}

	public static final String  PROPKEY_alarm_BlockingOthersMaxTimeInSec                = CM_NAME + ".alarm.system.if.BlockingOthersMaxTimeInSec.gt";
	public static final int     DEFAULT_alarm_BlockingOthersMaxTimeInSec                = 60;
	
	public static final String  PROPKEY_alarm_BlockingOthersMaxTimeInSecSkipDbname      = CM_NAME + ".alarm.system.if.BlockingOthersMaxTimeInSec.skip.dbname";
	public static final String  DEFAULT_alarm_BlockingOthersMaxTimeInSecSkipDbname      = "";
	
	public static final String  PROPKEY_alarm_BlockingOthersMaxTimeInSecSkipCommand     = CM_NAME + ".alarm.system.if.BlockingOthersMaxTimeInSec.skip.Command";
	public static final String  DEFAULT_alarm_BlockingOthersMaxTimeInSecSkipCommand     = "^(DUMP DATABASE|DUMP TRANSACTION).*";
	
	public static final String  PROPKEY_alarm_BlockingOthersMaxTimeInSecSkipApplication = CM_NAME + ".alarm.system.if.BlockingOthersMaxTimeInSec.skip.Application";
	public static final String  DEFAULT_alarm_BlockingOthersMaxTimeInSecSkipApplication = "";
	
	public static final String  PROPKEY_alarm_BlockingOthersMaxTimeInSecSkipSrvUserName = CM_NAME + ".alarm.system.if.BlockingOthersMaxTimeInSec.skip.SrvUserName";
	public static final String  DEFAULT_alarm_BlockingOthersMaxTimeInSecSkipSrvUserName = "";
	
	public static final String  PROPKEY_alarm_BlockingOthersMaxTimeInSecSkipHostName    = CM_NAME + ".alarm.system.if.BlockingOthersMaxTimeInSec.skip.HostName";
	public static final String  DEFAULT_alarm_BlockingOthersMaxTimeInSecSkipHostName    = "";


	public static final String  PROPKEY_alarm_StatementExecInSec                        = CM_NAME + ".alarm.system.if.StatementExecInSec.gt";
	public static final int     DEFAULT_alarm_StatementExecInSec                        = 3 * 60 * 60; // 3 hours

	public static final String  PROPKEY_alarm_StatementExecInSecSkipDbname              = CM_NAME + ".alarm.system.if.StatementExecInSec.skip.dbname";
	public static final String  DEFAULT_alarm_StatementExecInSecSkipDbname              = "";

	public static final String  PROPKEY_alarm_StatementExecInSecSkipCommand             = CM_NAME + ".alarm.system.if.StatementExecInSec.skip.Command";
	public static final String  DEFAULT_alarm_StatementExecInSecSkipCommand             = "^(DUMP DATABASE|DUMP TRANSACTION|LOAD DATABASE).*";
	                                                                                    
	public static final String  PROPKEY_alarm_StatementExecInSecSkipApplication         = CM_NAME + ".alarm.system.if.StatementExecInSec.skip.Application";
	public static final String  DEFAULT_alarm_StatementExecInSecSkipApplication         = "";
	                                                                                    
	public static final String  PROPKEY_alarm_StatementExecInSecSkipSrvUserName         = CM_NAME + ".alarm.system.if.StatementExecInSec.skip.SrvUserName";
	public static final String  DEFAULT_alarm_StatementExecInSecSkipSrvUserName         = "";
	                                                                                    
	public static final String  PROPKEY_alarm_StatementExecInSecSkipHostName            = CM_NAME + ".alarm.system.if.StatementExecInSec.skip.HostName";
	public static final String  DEFAULT_alarm_StatementExecInSecSkipHostName            = "";
	
	@Override
	public List<CmSettingsHelper> getLocalAlarmSettings()
	{
		Configuration conf = Configuration.getCombinedConfiguration();
		List<CmSettingsHelper> list = new ArrayList<>();

		CmSettingsHelper.Type isAlarmSwitch = CmSettingsHelper.Type.IS_ALARM_SWITCH;
		
		list.add(new CmSettingsHelper("BlockingOthersMaxTimeInSec", isAlarmSwitch , PROPKEY_alarm_BlockingOthersMaxTimeInSec                , Integer.class, conf.getIntProperty(PROPKEY_alarm_BlockingOthersMaxTimeInSec                , DEFAULT_alarm_BlockingOthersMaxTimeInSec               ), DEFAULT_alarm_BlockingOthersMaxTimeInSec               , "If 'BlockingOthersMaxTimeInSec' is greater than ## then send 'AlarmEventBlockingLockAlarm'." ));
//		list.add(createIsAlarmEnabled("BlockingOthersMaxTimeInSec", this));
//		list.add(createTimeRangeCron ("BlockingOthersMaxTimeInSec", this));
		list.add(new CmSettingsHelper("BlockingOthersMaxTimeInSec SkipDbname"     , PROPKEY_alarm_BlockingOthersMaxTimeInSecSkipDbname      , String .class, conf.getProperty   (PROPKEY_alarm_BlockingOthersMaxTimeInSecSkipDbname      , DEFAULT_alarm_BlockingOthersMaxTimeInSecSkipDbname     ), DEFAULT_alarm_BlockingOthersMaxTimeInSecSkipDbname     , "If 'BlockingOthersMaxTimeInSec' is true; then we can filter out transaction names using a Regular expression... if (dbname  .matches('regexp'))... This to remove alarms of '(db1|db2)' or similar. A good place to test your regexp is 'http://www.regexplanet.com/advanced/java/index.html'."    , new RegExpInputValidator()));
		list.add(new CmSettingsHelper("BlockingOthersMaxTimeInSec SkipCommand"    , PROPKEY_alarm_BlockingOthersMaxTimeInSecSkipCommand     , String .class, conf.getProperty   (PROPKEY_alarm_BlockingOthersMaxTimeInSecSkipCommand     , DEFAULT_alarm_BlockingOthersMaxTimeInSecSkipCommand    ), DEFAULT_alarm_BlockingOthersMaxTimeInSecSkipCommand    , "If 'BlockingOthersMaxTimeInSec' is true; then we can filter out transaction names using a Regular expression... if (tranName.matches('regexp'))... This to remove alarms of 'DUMP DATABASE' or similar. A good place to test your regexp is 'http://www.regexplanet.com/advanced/java/index.html'.", new RegExpInputValidator()));
		list.add(new CmSettingsHelper("BlockingOthersMaxTimeInSec SkipApplication", PROPKEY_alarm_BlockingOthersMaxTimeInSecSkipApplication , String .class, conf.getProperty   (PROPKEY_alarm_BlockingOthersMaxTimeInSecSkipApplication , DEFAULT_alarm_BlockingOthersMaxTimeInSecSkipApplication), DEFAULT_alarm_BlockingOthersMaxTimeInSecSkipApplication, "If 'BlockingOthersMaxTimeInSec' is true; then we can filter out transaction names using a Regular expression... if (tranProg.matches('regexp'))... This to remove alarms of 'SQLAgent.*' or similar. A good place to test your regexp is 'http://www.regexplanet.com/advanced/java/index.html'."   , new RegExpInputValidator()));
		list.add(new CmSettingsHelper("BlockingOthersMaxTimeInSec SkipSrvUserName", PROPKEY_alarm_BlockingOthersMaxTimeInSecSkipSrvUserName , String .class, conf.getProperty   (PROPKEY_alarm_BlockingOthersMaxTimeInSecSkipSrvUserName , DEFAULT_alarm_BlockingOthersMaxTimeInSecSkipSrvUserName), DEFAULT_alarm_BlockingOthersMaxTimeInSecSkipSrvUserName, "If 'BlockingOthersMaxTimeInSec' is true; then we can filter out transaction names using a Regular expression... if (tranUser.matches('regexp'))... This to remove alarms of '(user1|user2)' or similar. A good place to test your regexp is 'http://www.regexplanet.com/advanced/java/index.html'.", new RegExpInputValidator()));
		list.add(new CmSettingsHelper("BlockingOthersMaxTimeInSec SkipHostName"   , PROPKEY_alarm_BlockingOthersMaxTimeInSecSkipHostName    , String .class, conf.getProperty   (PROPKEY_alarm_BlockingOthersMaxTimeInSecSkipHostName    , DEFAULT_alarm_BlockingOthersMaxTimeInSecSkipHostName   ), DEFAULT_alarm_BlockingOthersMaxTimeInSecSkipHostName   , "If 'BlockingOthersMaxTimeInSec' is true; then we can filter out transaction names using a Regular expression... if (tranHost.matches('regexp'))... This to remove alarms of '.*-prod-.*' or similar. A good place to test your regexp is 'http://www.regexplanet.com/advanced/java/index.html'."   , new RegExpInputValidator()));

		list.add(new CmSettingsHelper("StatementExecInSec", isAlarmSwitch         , PROPKEY_alarm_StatementExecInSec                        , Integer.class, conf.getIntProperty(PROPKEY_alarm_StatementExecInSec                        , DEFAULT_alarm_StatementExecInSec                       ), DEFAULT_alarm_StatementExecInSec                       , "If 'ExecTimeInMs/1000' is greater than ## then send 'AlarmEventLongRunningStatement'." ));
//		list.add(createIsAlarmEnabled("StatementExecInSec", this));
//		list.add(createTimeRangeCron ("StatementExecInSec", this));
		list.add(new CmSettingsHelper("StatementExecInSec SkipDbname"             , PROPKEY_alarm_StatementExecInSecSkipDbname              , String .class, conf.getProperty   (PROPKEY_alarm_StatementExecInSecSkipDbname              , DEFAULT_alarm_StatementExecInSecSkipDbname             ), DEFAULT_alarm_StatementExecInSecSkipDbname             , "If 'ExecTimeInMs/1000' is true; then we can filter out transaction names using a Regular expression... if (dbname  .matches('regexp'))... This to remove alarms of '(db1|db2)' or similar. A good place to test your regexp is 'http://www.regexplanet.com/advanced/java/index.html'."    , new RegExpInputValidator()));
		list.add(new CmSettingsHelper("StatementExecInSec SkipCommand"            , PROPKEY_alarm_StatementExecInSecSkipCommand             , String .class, conf.getProperty   (PROPKEY_alarm_StatementExecInSecSkipCommand             , DEFAULT_alarm_StatementExecInSecSkipCommand            ), DEFAULT_alarm_StatementExecInSecSkipCommand            , "If 'ExecTimeInMs/1000' is true; then we can filter out transaction names using a Regular expression... if (tranName.matches('regexp'))... This to remove alarms of 'DUMP DATABASE' or similar. A good place to test your regexp is 'http://www.regexplanet.com/advanced/java/index.html'.", new RegExpInputValidator()));
		list.add(new CmSettingsHelper("StatementExecInSec SkipApplication"        , PROPKEY_alarm_StatementExecInSecSkipApplication         , String .class, conf.getProperty   (PROPKEY_alarm_StatementExecInSecSkipApplication         , DEFAULT_alarm_StatementExecInSecSkipApplication        ), DEFAULT_alarm_StatementExecInSecSkipApplication        , "If 'ExecTimeInMs/1000' is true; then we can filter out transaction names using a Regular expression... if (tranProg.matches('regexp'))... This to remove alarms of 'SQLAgent.*' or similar. A good place to test your regexp is 'http://www.regexplanet.com/advanced/java/index.html'."   , new RegExpInputValidator()));
		list.add(new CmSettingsHelper("StatementExecInSec SkipSrvUserName"        , PROPKEY_alarm_StatementExecInSecSkipSrvUserName         , String .class, conf.getProperty   (PROPKEY_alarm_StatementExecInSecSkipSrvUserName         , DEFAULT_alarm_StatementExecInSecSkipSrvUserName        ), DEFAULT_alarm_StatementExecInSecSkipSrvUserName        , "If 'ExecTimeInMs/1000' is true; then we can filter out transaction names using a Regular expression... if (tranUser.matches('regexp'))... This to remove alarms of '(user1|user2)' or similar. A good place to test your regexp is 'http://www.regexplanet.com/advanced/java/index.html'.", new RegExpInputValidator()));
		list.add(new CmSettingsHelper("StatementExecInSec SkipHostName"           , PROPKEY_alarm_StatementExecInSecSkipHostName            , String .class, conf.getProperty   (PROPKEY_alarm_StatementExecInSecSkipHostName            , DEFAULT_alarm_StatementExecInSecSkipHostName           ), DEFAULT_alarm_StatementExecInSecSkipHostName           , "If 'ExecTimeInMs/1000' is true; then we can filter out transaction names using a Regular expression... if (tranHost.matches('regexp'))... This to remove alarms of '.*-prod-.*' or similar. A good place to test your regexp is 'http://www.regexplanet.com/advanced/java/index.html'."   , new RegExpInputValidator()));

//		CmSettingsHelper.add(this, list, "BlockingOthersMaxTimeInSec", null             , PROPKEY_alarm_BlockingOthersMaxTimeInSec                , Integer.class, conf.getIntProperty(PROPKEY_alarm_BlockingOthersMaxTimeInSec                , DEFAULT_alarm_BlockingOthersMaxTimeInSec               ), DEFAULT_alarm_BlockingOthersMaxTimeInSec               , "If 'BlockingOthersMaxTimeInSec' is greater than ## then send 'AlarmEventBlockingLockAlarm'.", null );
//		CmSettingsHelper.add(this, list, "BlockingOthersMaxTimeInSec", "SkipDbname"     , PROPKEY_alarm_BlockingOthersMaxTimeInSecSkipDbname      , String .class, conf.getProperty   (PROPKEY_alarm_BlockingOthersMaxTimeInSecSkipDbname      , DEFAULT_alarm_BlockingOthersMaxTimeInSecSkipDbname     ), DEFAULT_alarm_BlockingOthersMaxTimeInSecSkipDbname     , "If 'BlockingOthersMaxTimeInSec' is true; then we can filter out transaction names using a Regular expression... if (dbname  .matches('regexp'))... This to remove alarms of '(db1|db2)' or similar. A good place to test your regexp is 'http://www.regexplanet.com/advanced/java/index.html'."    , new RegExpInputValidator());
//		CmSettingsHelper.add(this, list, "BlockingOthersMaxTimeInSec", "SkipCommand"    , PROPKEY_alarm_BlockingOthersMaxTimeInSecSkipCommand     , String .class, conf.getProperty   (PROPKEY_alarm_BlockingOthersMaxTimeInSecSkipCommand     , DEFAULT_alarm_BlockingOthersMaxTimeInSecSkipCommand    ), DEFAULT_alarm_BlockingOthersMaxTimeInSecSkipCommand    , "If 'BlockingOthersMaxTimeInSec' is true; then we can filter out transaction names using a Regular expression... if (tranName.matches('regexp'))... This to remove alarms of 'DUMP DATABASE' or similar. A good place to test your regexp is 'http://www.regexplanet.com/advanced/java/index.html'.", new RegExpInputValidator());
//		CmSettingsHelper.add(this, list, "BlockingOthersMaxTimeInSec", "SkipApplication", PROPKEY_alarm_BlockingOthersMaxTimeInSecSkipApplication , String .class, conf.getProperty   (PROPKEY_alarm_BlockingOthersMaxTimeInSecSkipApplication , DEFAULT_alarm_BlockingOthersMaxTimeInSecSkipApplication), DEFAULT_alarm_BlockingOthersMaxTimeInSecSkipApplication, "If 'BlockingOthersMaxTimeInSec' is true; then we can filter out transaction names using a Regular expression... if (tranProg.matches('regexp'))... This to remove alarms of 'SQLAgent.*' or similar. A good place to test your regexp is 'http://www.regexplanet.com/advanced/java/index.html'."   , new RegExpInputValidator());
//		CmSettingsHelper.add(this, list, "BlockingOthersMaxTimeInSec", "SkipSrvUserName", PROPKEY_alarm_BlockingOthersMaxTimeInSecSkipSrvUserName , String .class, conf.getProperty   (PROPKEY_alarm_BlockingOthersMaxTimeInSecSkipSrvUserName , DEFAULT_alarm_BlockingOthersMaxTimeInSecSkipSrvUserName), DEFAULT_alarm_BlockingOthersMaxTimeInSecSkipSrvUserName, "If 'BlockingOthersMaxTimeInSec' is true; then we can filter out transaction names using a Regular expression... if (tranUser.matches('regexp'))... This to remove alarms of '(user1|user2)' or similar. A good place to test your regexp is 'http://www.regexplanet.com/advanced/java/index.html'.", new RegExpInputValidator());
//		CmSettingsHelper.add(this, list, "BlockingOthersMaxTimeInSec", "SkipHostName"   , PROPKEY_alarm_BlockingOthersMaxTimeInSecSkipHostName    , String .class, conf.getProperty   (PROPKEY_alarm_BlockingOthersMaxTimeInSecSkipHostName    , DEFAULT_alarm_BlockingOthersMaxTimeInSecSkipHostName   ), DEFAULT_alarm_BlockingOthersMaxTimeInSecSkipHostName   , "If 'BlockingOthersMaxTimeInSec' is true; then we can filter out transaction names using a Regular expression... if (tranHost.matches('regexp'))... This to remove alarms of '.*-prod-.*' or similar. A good place to test your regexp is 'http://www.regexplanet.com/advanced/java/index.html'."   , new RegExpInputValidator());
//
//		CmSettingsHelper.add(this, list, "StatementExecInSec", null                     , PROPKEY_alarm_StatementExecInSec                        , Integer.class, conf.getIntProperty(PROPKEY_alarm_StatementExecInSec                        , DEFAULT_alarm_StatementExecInSec                       ), DEFAULT_alarm_StatementExecInSec                       , "If 'ExecTimeInMs/1000' is greater than ## then send 'AlarmEventLongRunningStatement'.", null );
//		CmSettingsHelper.add(this, list, "StatementExecInSec", "SkipDbname"             , PROPKEY_alarm_StatementExecInSecSkipDbname              , String .class, conf.getProperty   (PROPKEY_alarm_StatementExecInSecSkipDbname              , DEFAULT_alarm_StatementExecInSecSkipDbname             ), DEFAULT_alarm_StatementExecInSecSkipDbname             , "If 'ExecTimeInMs/1000' is true; then we can filter out transaction names using a Regular expression... if (dbname  .matches('regexp'))... This to remove alarms of '(db1|db2)' or similar. A good place to test your regexp is 'http://www.regexplanet.com/advanced/java/index.html'."    , new RegExpInputValidator());
//		CmSettingsHelper.add(this, list, "StatementExecInSec", "SkipCommand"            , PROPKEY_alarm_StatementExecInSecSkipCommand             , String .class, conf.getProperty   (PROPKEY_alarm_StatementExecInSecSkipCommand             , DEFAULT_alarm_StatementExecInSecSkipCommand            ), DEFAULT_alarm_StatementExecInSecSkipCommand            , "If 'ExecTimeInMs/1000' is true; then we can filter out transaction names using a Regular expression... if (tranName.matches('regexp'))... This to remove alarms of 'DUMP DATABASE' or similar. A good place to test your regexp is 'http://www.regexplanet.com/advanced/java/index.html'.", new RegExpInputValidator());
//		CmSettingsHelper.add(this, list, "StatementExecInSec", "SkipApplication"        , PROPKEY_alarm_StatementExecInSecSkipApplication         , String .class, conf.getProperty   (PROPKEY_alarm_StatementExecInSecSkipApplication         , DEFAULT_alarm_StatementExecInSecSkipApplication        ), DEFAULT_alarm_StatementExecInSecSkipApplication        , "If 'ExecTimeInMs/1000' is true; then we can filter out transaction names using a Regular expression... if (tranProg.matches('regexp'))... This to remove alarms of 'SQLAgent.*' or similar. A good place to test your regexp is 'http://www.regexplanet.com/advanced/java/index.html'."   , new RegExpInputValidator());
//		CmSettingsHelper.add(this, list, "StatementExecInSec", "SkipSrvUserName"        , PROPKEY_alarm_StatementExecInSecSkipSrvUserName         , String .class, conf.getProperty   (PROPKEY_alarm_StatementExecInSecSkipSrvUserName         , DEFAULT_alarm_StatementExecInSecSkipSrvUserName        ), DEFAULT_alarm_StatementExecInSecSkipSrvUserName        , "If 'ExecTimeInMs/1000' is true; then we can filter out transaction names using a Regular expression... if (tranUser.matches('regexp'))... This to remove alarms of '(user1|user2)' or similar. A good place to test your regexp is 'http://www.regexplanet.com/advanced/java/index.html'.", new RegExpInputValidator());
//		CmSettingsHelper.add(this, list, "StatementExecInSec", "SkipHostName"           , PROPKEY_alarm_StatementExecInSecSkipHostName            , String .class, conf.getProperty   (PROPKEY_alarm_StatementExecInSecSkipHostName            , DEFAULT_alarm_StatementExecInSecSkipHostName           ), DEFAULT_alarm_StatementExecInSecSkipHostName           , "If 'ExecTimeInMs/1000' is true; then we can filter out transaction names using a Regular expression... if (tranHost.matches('regexp'))... This to remove alarms of '.*-prod-.*' or similar. A good place to test your regexp is 'http://www.regexplanet.com/advanced/java/index.html'."   , new RegExpInputValidator());
		
		return list;
	}
}
