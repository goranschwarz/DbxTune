package com.asetune.cm.ase;

import java.awt.event.MouseEvent;
import java.sql.Connection;
import java.sql.Timestamp;
import java.util.LinkedList;
import java.util.List;

import javax.naming.NameNotFoundException;

import org.apache.log4j.Logger;

import com.asetune.ICounterController;
import com.asetune.IGuiController;
import com.asetune.MonTablesDictionary;
import com.asetune.cm.CounterSetTemplates;
import com.asetune.cm.CounterSetTemplates.Type;
import com.asetune.cm.CountersModel;
import com.asetune.cm.SamplingCnt;
import com.asetune.cm.ase.gui.CmActiveStatementsPanel;
import com.asetune.gui.MainFrame;
import com.asetune.gui.TabularCntrPanel;
import com.asetune.utils.AseConnectionUtils;
import com.asetune.utils.Configuration;
import com.asetune.utils.StringUtil;

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
		"pssinfo_tempdb_pages"};

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
		super(CM_NAME, GROUP_NAME, /*sql*/null, /*pkList*/null, 
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
			MonTablesDictionary mtd = MonTablesDictionary.getInstance();
			mtd.addColumn("monProcess",  "BlockingOtherSpids", "This SPID is Blocking other SPID's from executing, because this SPID hold lock(s), that some other SPID wants to grab.");
			mtd.addColumn("monProcess",  "multiSampled",       "<html>" +
			                                                       "This indicates that the PrimaryKey (SPID, monSource[, InstanceID]) has been in this table for more than one sample.<br>" +
			                                                       "Also 'StartTime' has to be the <b>same</b> as in the previous sample.<br>" +
			                                                       "The StartTime is the columns, which tells the start time for current SQL statement, or the individual statement inside a Stored Procedure." +
			                                                   "</html>");

			mtd.addColumn("monProcess",  "HasMonSqlText",      "Has values for: select SQLText from master..monProcessSQLText where SPID = <SPID>");
			mtd.addColumn("monProcess",  "MonSqlText",                         "select SQLText from master..monProcessSQLText where SPID = <SPID>");
			mtd.addColumn("monProcess",  "HasDbccSqlText",     "Has values for: DBCC sqltext(<SPID>)");
			mtd.addColumn("monProcess",  "DbccSqlText",                        "DBCC sqltext(<SPID>)");
			mtd.addColumn("monProcess",  "HasShowPlan",        "Has values for: sp_showplan <SPID>, null, null, null");
			mtd.addColumn("monProcess",  "ShowPlanText",                       "sp_showplan <SPID>, null, null, null");
			mtd.addColumn("monProcess",  "HasStacktrace",      "Has values for: DBCC stacktrace(<SPID>)");
			mtd.addColumn("monProcess",  "DbccStacktrace",                     "DBCC stacktrace(<SPID>)");
			mtd.addColumn("monProcess",  "HasProcCallStack",   "Has values for: select * from monProcessProcedures where SPID = <SPID>");
			mtd.addColumn("monProcess",  "ProcCallStack",                      "select * from monProcessProcedures where SPID = <SPID>");
		}
		catch (NameNotFoundException e) {/*ignore*/}
	}

	@Override
	public String[] getDependsOnConfigForVersion(Connection conn, int srvVersion, boolean isClusterEnabled)
	{
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

		// ASE 15.7
		String HostName       = "";
		String ClientName     = "";
		String ClientHostName = "";
		String ClientApplName = "";
		String ase1570_nl     = "";

//		if (aseVersion >= 15700)
		if (aseVersion >= 1570000)
		{
			HostName       = "P.HostName, ";
			ClientName     = "P.ClientName, ";
			ClientHostName = "P.ClientHostName, ";
			ClientApplName = "P.ClientApplName, ";
			ase1570_nl     = "\n";
		}

		String optGoalPlan = "";
//		if (aseVersion >= 15020)
		if (aseVersion >= 1502000)
		{
			optGoalPlan = "plan '(use optgoal allrows_dss)' \n";
		}

		if (isClusterEnabled)
		{
			cols1 += "S.InstanceID, ";
		}

		String dbNameCol  = "dbname=db_name(S.DBID)";
//		if (aseVersion >= 15026) // just a guess what release this was introduced (not in 15.0.2.1, but at least in 15.0.2.6, I have not gathered info for 15022-15025])
		if (aseVersion >= 1502060) // just a guess what release this was introduced (not in 15.0.2.1, but at least in 15.0.2.6, I have not gathered info for 15022-15025])
		{
			dbNameCol  = "dbname=S.DBName";
		}

		cols1 += "monSource=convert(varchar(10),'ACTIVE'), \n" +
		         "P.SPID, P.KPID, \n" +
		         "multiSampled=convert(varchar(10),''), \n" +
		         "S.BatchID, S.LineNumber, \n" +
		         dbNameCol+", procname=isnull(object_name(S.ProcedureID,S.DBID),''), linenum=S.LineNumber, \n" +
		         "P.Command, P.Application, \n" +
		         HostName + ClientName + ClientHostName + ClientApplName + ase1570_nl +
		         "S.CpuTime, S.WaitTime, \n" +
		         "ExecTimeInMs    = CASE WHEN datediff(day, S.StartTime, getdate()) > 20 THEN -1 ELSE  datediff(ms, S.StartTime, getdate()) END, \n" +               // protect from: Msg 535: Difference of two datetime fields caused overflow at runtime. above 24 days or so, the MS difference is overflowned
		         "UsefullExecTime = CASE WHEN datediff(day, S.StartTime, getdate()) > 20 THEN -1 ELSE (datediff(ms, S.StartTime, getdate()) - S.WaitTime) END, \n" + // protect from: Msg 535: Difference of two datetime fields caused overflow at runtime. above 24 days or so, the MS difference is overflowned
		         "BlockingOtherSpids=convert(varchar(255),''), P.BlockingSPID, \n" +
		         "P.SecondsWaiting, P.WaitEventID, \n" +
		         "WaitClassDesc=convert(varchar(50),''), \n" +
		         "WaitEventDesc=convert(varchar(50),''), \n" +
		         "HasMonSqlText=convert(bit,0), HasDbccSqlText=convert(bit,0), HasProcCallStack=convert(bit,0), \n" +
		         "HasShowPlan=convert(bit,0), HasStacktrace=convert(bit,0), \n" +
		         "S.MemUsageKB, S.PhysicalReads, S.LogicalReads, \n";
		cols2 += "";
		cols3 += "S.PagesModified, S.PacketsSent, S.PacketsReceived, S.NetworkPacketSize, \n" +
		         "S.PlansAltered, S.StartTime, S.PlanID, S.DBID, S.ProcedureID, \n" +
		         "P.SecondsConnected, P.EngineNumber, P.NumChildren, \n" +
		         "MonSqlText=convert(text,null), \n" +
		         "DbccSqlText=convert(text,null), \n" +
		         "ProcCallStack=convert(text,null), \n" +
		         "ShowPlanText=convert(text,null), \n" +
		         "DbccStacktrace=convert(text,null)";
//		if (aseVersion >= 15020 || (aseVersion >= 12540 && aseVersion < 15000) )
		if (aseVersion >= 1502000 || (aseVersion >= 1254000 && aseVersion < 1500000) )
		{
			cols2 += "S.RowsAffected, " +
			         "tempdb_name = db_name(tempdb_id(S.SPID)), \n" +
			         "pssinfo_tempdb_pages = convert(int, pssinfo(S.SPID, 'tempdb_pages')), \n";
		}

		// in 12.5.4 (esd#9) will produce an "empty" resultset using "S.SPID != @@spid"
		//                   so this will be a workaround for those releses below 15.0.0
		String whereSpidNotMe = "S.SPID != @@spid";
//		if (aseVersion >= 12540 && aseVersion <= 15000)
//		if (aseVersion < 15000)
		if (aseVersion < 1500000)
		{
			whereSpidNotMe = "S.SPID != convert(int,@@spid)";
		}

		// The above I had in the Properties file for a long time
		String sql = 
			"select " + cols1 + cols2 + cols3 + "\n" +
			"from master..monProcessStatement S, master..monProcess P \n" +
			"where S.KPID = P.KPID \n" +
			(isClusterEnabled ? "  and S.InstanceID = P.InstanceID \n" : "") +
			"  and "+whereSpidNotMe+"\n" +
			"order by S.LogicalReads desc \n" +
			optGoalPlan;

		//-------------------------------------------
		// Build SECOND SQL, which gets SPID's that blocks other, and might not be within the ABOVE ststement
		//-------------------------------------------
		cols1 = cols2 = cols3 = "";

		if (isClusterEnabled)
		{
			cols1 += "P.InstanceID, ";
		}

		dbNameCol  = "dbname=db_name(P.DBID)";
//		if (aseVersion >= 15020) // just a guess what release this was introduced. 15020 is OK
		if (aseVersion >= 1502000) // just a guess what release this was introduced. 15020 is OK
		{
			dbNameCol  = "dbname=P.DBName";
		}

		cols1 += "monSource=convert(varchar(10),'BLOCKER'), \n" +
		         "P.SPID, P.KPID, \n" +
		         "multiSampled=convert(varchar(10),''), \n" +
		         "P.BatchID, P.LineNumber, \n" +
		         dbNameCol+", procname='', linenum=P.LineNumber, \n" +
		         HostName + ClientName + ClientHostName + ClientApplName + ase1570_nl +
		         "P.Command, P.Application, \n" +
		         "CpuTime=-1, WaitTime=-1, \n" +
		         "ExecTimeInMs    = -1, \n" +
		         "UsefullExecTime = -1, \n" +
		         "BlockingOtherSpids=convert(varchar(255),''), P.BlockingSPID, \n" +
		         "P.SecondsWaiting, P.WaitEventID, \n" +
		         "WaitClassDesc=convert(varchar(50),''), \n" +
		         "WaitEventDesc=convert(varchar(50),''), \n" +
		         "HasMonSqlText=convert(bit,0), HasDbccSqlText=convert(bit,0), HasProcCallStack=convert(bit,0), \n" +
		         "HasShowPlan=convert(bit,0), HasStacktrace=convert(bit,0), \n" +
		         "MemUsageKB=-1, PhysicalReads=-1, LogicalReads=-1, \n";
		cols2 += "";
		cols3 += "PagesModified=-1, PacketsSent=-1, PacketsReceived=-1, NetworkPacketSize=-1, \n" +
		         "PlansAltered=-1, StartTime=convert(datetime,NULL), PlanID=-1, P.DBID, ProcedureID=-1, \n" +
		         "P.SecondsConnected, P.EngineNumber, P.NumChildren, \n" +
		         "MonSqlText=convert(text,null), \n" +
		         "DbccSqlText=convert(text,null), \n" +
		         "ProcCallStack=convert(text,null), \n" +
		         "ShowPlanText=convert(text,null), \n" +
		         "DbccStacktrace=convert(text,null)";
//		if (aseVersion >= 15020 || (aseVersion >= 12540 && aseVersion < 15000) )
		if (aseVersion >= 1502000 || (aseVersion >= 1254000 && aseVersion < 1500000) )
		{
			cols2 += "RowsAffected=-1, " +
			         "tempdb_name = db_name(tempdb_id(P.SPID)), \n" +
			         "pssinfo_tempdb_pages = convert(int, pssinfo(P.SPID, 'tempdb_pages')), \n";
		}


		// The above I had in the Properties file for a long time
		String x_sql = 
			"select " + cols1 + cols2 + cols3 + "\n" +
			"from master..monProcess P \n" +
			"where P.SPID in (select blocked from master..sysprocesses where blocked > 0) \n" +
			optGoalPlan;


		//------------------------------------------------
		// Finally return the SQL
		//------------------------------------------------
		return sql + "\n\n"
	       + "  /* UNION ALL */ \n\n"
	       + x_sql;
	}

	/** Used by the: Create 'Offline Session' Wizard */
	@Override
	public Configuration getLocalConfiguration()
	{
		Configuration conf = Configuration.getCombinedConfiguration();
		Configuration lc = new Configuration();

		lc.setProperty(getName()+".sample.showplan",       conf.getBooleanProperty(getName()+".sample.showplan",       true));
		lc.setProperty(getName()+".sample.monSqltext",     conf.getBooleanProperty(getName()+".sample.monSqltext",     true));
		lc.setProperty(getName()+".sample.dbccSqltext",    conf.getBooleanProperty(getName()+".sample.dbccSqltext",    false));
		lc.setProperty(getName()+".sample.procCallStack",  conf.getBooleanProperty(getName()+".sample.procCallStack",  true));
		lc.setProperty(getName()+".sample.dbccStacktrace", conf.getBooleanProperty(getName()+".sample.dbccStacktrace", false));

		return lc;
	}

	@Override
	public String getLocalConfigurationDescription(String propName)
	{
		if (propName.equals(getName()+".sample.showplan"))       return "Do 'select SQLText from monProcessSQLText where SPID=spid' on every row in the table.";
		if (propName.equals(getName()+".sample.monSqltext"))     return "Do 'dbcc sqltext(spid)' on every row in the table.";
		if (propName.equals(getName()+".sample.dbccSqltext"))    return "Do 'select * from monProcessProcedures where SPID=spid' on every row in the table.";
		if (propName.equals(getName()+".sample.procCallStack"))  return "Do 'sp_showplan spid' on every row in the table.";
		if (propName.equals(getName()+".sample.dbccStacktrace")) return "Do 'dbcc stacktrace(spid)' on every row in the table.";
		return "";
	}
	@Override
	public String getLocalConfigurationDataType(String propName)
	{
		if (propName.equals(getName()+".sample.showplan"))       return Boolean.class.getSimpleName();
		if (propName.equals(getName()+".sample.monSqltext"))     return Boolean.class.getSimpleName();
		if (propName.equals(getName()+".sample.dbccSqltext"))    return Boolean.class.getSimpleName();
		if (propName.equals(getName()+".sample.procCallStack"))  return Boolean.class.getSimpleName();
		if (propName.equals(getName()+".sample.dbccStacktrace")) return Boolean.class.getSimpleName();
		return "";
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
					return toHtmlString((String) cellVal);
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
					return toHtmlString((String) cellVal);
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
			// Find 'DbccStacktrace' column, is so get it and set it as the tool tip
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
		
		return super.getToolTipTextOnTableCell(e, colName, cellValue, modelRow, modelCol);
	}
	/** add HTML around the string, and translate line breaks into <br> */
	private String toHtmlString(String in)
	{
		String str = StringUtil.makeApproxLineBreak(in, 150, 10, "\n");
		str = str.replaceAll("\\n", "<br>");
		if (in.indexOf("<html>")>=0 || in.indexOf("<HTML>")>=0)
			return str;
		return "<html><pre>" + str + "</pre></html>";
	}

	@Override
	public Class<?> getColumnClass(int columnIndex)
	{
		// use CHECKBOX for column "HasShowPlan"
		String colName = getColumnName(columnIndex);
		if      ("HasShowPlan"     .equals(colName)) return Boolean.class;
		else if ("HasMonSqlText"   .equals(colName)) return Boolean.class;
		else if ("HasDbccSqlText"  .equals(colName)) return Boolean.class;
		else if ("HasProcCallStack".equals(colName)) return Boolean.class;
		else if ("HasStacktrace"   .equals(colName)) return Boolean.class;
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
	public void localCalculation(SamplingCnt prevSample, SamplingCnt newSample, SamplingCnt diffData)
	{
//		long startTime = System.currentTimeMillis();

//		Configuration conf = Configuration.getInstance(Configuration.TEMP);
		Configuration conf = Configuration.getCombinedConfiguration();
		boolean getShowplan       = conf == null ? true : conf.getBooleanProperty(getName()+".sample.showplan",       true);
		boolean getMonSqltext     = conf == null ? true : conf.getBooleanProperty(getName()+".sample.monSqltext",     true);
		boolean getDbccSqltext    = conf == null ? false: conf.getBooleanProperty(getName()+".sample.dbccSqltext",    false);
		boolean getProcCallStack  = conf == null ? true : conf.getBooleanProperty(getName()+".sample.procCallStack",  true);
		boolean getDbccStacktrace = conf == null ? false: conf.getBooleanProperty(getName()+".sample.dbccStacktrace", false);

		// Where are various columns located in the Vector 
		int pos_WaitEventID        = -1, pos_WaitEventDesc  = -1, pos_WaitClassDesc = -1, pos_SPID = -1;
		int pos_HasShowPlan        = -1, pos_ShowPlanText   = -1;
		int pos_HasMonSqlText      = -1, pos_MonSqlText     = -1;
		int pos_HasDbccSqlText     = -1, pos_DbccSqlText    = -1;
		int pos_HasProcCallStack   = -1, pos_ProcCallStack  = -1;
		int pos_HasStacktrace      = -1, pos_DbccStacktrace = -1;
		int pos_BlockingOtherSpids = -1, pos_BlockingSPID   = -1;
		int pos_multiSampled       = -1;
		int pos_StartTime          = -1;
		int waitEventID = 0;
		String waitEventDesc = "";
		String waitClassDesc = "";
		SamplingCnt counters = diffData;

		if ( ! MonTablesDictionary.hasInstance() )
			return;
		MonTablesDictionary mtd = MonTablesDictionary.getInstance();

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
			else if (colName.equals("BlockingOtherSpids")) pos_BlockingOtherSpids = colId;
			else if (colName.equals("BlockingSPID"))       pos_BlockingSPID       = colId;
			else if (colName.equals("multiSampled"))       pos_multiSampled       = colId;
			else if (colName.equals("StartTime"))          pos_StartTime          = colId;

			// Noo need to continue, we got all our columns
			if (    pos_WaitEventID        >= 0 && pos_WaitEventDesc  >= 0 
			     && pos_WaitClassDesc      >= 0 && pos_SPID >= 0 
			     && pos_HasShowPlan        >= 0 && pos_ShowPlanText   >= 0 
			     && pos_HasMonSqlText      >= 0 && pos_MonSqlText     >= 0 
			     && pos_HasDbccSqlText     >= 0 && pos_DbccSqlText    >= 0 
			     && pos_HasProcCallStack   >= 0 && pos_ProcCallStack  >= 0 
			     && pos_HasStacktrace      >= 0 && pos_DbccStacktrace >= 0 
			     && pos_BlockingOtherSpids >= 0 && pos_BlockingSPID   >= 0
			     && pos_multiSampled       >= 0
			     && pos_StartTime          >= 0
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
		
		// Loop on all diffData rows
		for (int rowId=0; rowId < counters.getRowCount(); rowId++) 
		{
			String thisRowPk = counters.getPkValue(rowId);
			int prevPkRowId = (prevSample == null) ? -1 : prevSample.getRowNumberForPkValue(thisRowPk);
			boolean prevPkExists = prevPkRowId >= 0;

			Object o_waitEventId = counters.getValueAt(rowId, pos_WaitEventID);
			Object o_SPID        = counters.getValueAt(rowId, pos_SPID);

			if (prevPkExists)
			{
				Object o_this_StartTime = counters  .getValueAt(rowId,       pos_StartTime);
				Object o_prev_StartTime = prevSample.getValueAt(prevPkRowId, pos_StartTime);

				if (o_this_StartTime instanceof Timestamp && o_prev_StartTime instanceof Timestamp)
				{
					if (o_this_StartTime.equals(o_prev_StartTime))
						counters.setValueAt("YES", rowId, pos_multiSampled);
				}
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

			if (o_SPID instanceof Number)
			{
				int spid = ((Number)o_SPID).intValue();

				String monSqlText    = "Not properly configured (need 'SQL batch capture' & 'max SQL text monitored').";
				String dbccSqlText   = "User does not have: sa_role";
				String procCallStack = "User does not have: sa_role";
				String showplan      = "User does not have: sa_role";
				String stacktrace    = "User does not have: sa_role";

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
				if (isRoleActive(AseConnectionUtils.SA_ROLE))
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
					if (showplan == null)
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
	
//	@Override
//	protected Object clone() throws CloneNotSupportedException
//	{
//		// TODO Auto-generated method stub
//		return super.clone();
//	}

	private String getBlockingListStr(SamplingCnt counters, int spid, int pos_BlockingSPID, int pos_SPID)
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
