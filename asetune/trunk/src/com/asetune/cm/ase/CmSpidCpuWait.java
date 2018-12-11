package com.asetune.cm.ase;

import java.awt.event.MouseEvent;
import java.math.BigDecimal;
import java.sql.Connection;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

import javax.naming.NameNotFoundException;

import org.apache.log4j.Logger;

import com.asetune.ICounterController;
import com.asetune.IGuiController;
import com.asetune.cm.CmSettingsHelper;
import com.asetune.cm.CounterSample;
import com.asetune.cm.CounterSetTemplates;
import com.asetune.cm.CounterSetTemplates.Type;
import com.asetune.cm.CountersModel;
import com.asetune.cm.ase.gui.CmSpidCpuWaitPanel;
import com.asetune.config.dict.MonTablesDictionary;
import com.asetune.config.dict.MonTablesDictionaryManager;
import com.asetune.gui.MainFrame;
import com.asetune.gui.TabularCntrPanel;
import com.asetune.utils.AseConnectionUtils;
import com.asetune.utils.Configuration;
import com.asetune.utils.StringUtil;
import com.asetune.utils.Ver;

/**
 * @author Goran Schwarz (goran_schwarz@hotmail.com)
 */
public class CmSpidCpuWait
extends CountersModel
{
	private static Logger        _logger          = Logger.getLogger(CmSpidCpuWait.class);
	private static final long    serialVersionUID = 1L;

	public static final String   CM_NAME          = CmSpidCpuWait.class.getSimpleName();
	public static final String   SHORT_NAME       = "SPID Activity";
	public static final String   HTML_DESC        = 
		"<html>" +
		"SPID CPU and Wait Time activity per sample interval and a bunch of other values as well.<br>" +
		"<br>" +
		"<b>Tip:</b> <br>" +
		"Use the filer 'Do NOT show unchanged counter rows', to exclude counters that has <b>not</b> changed.<br>" +
		"<br>" +
		"<b>Note:</b> <br>" +
		"This will take even more resources than the Performance Counter '"+CmSpidWait.SHORT_NAME+"'<br>" +
		"So try the 'Local Options' panel, and the 'Apply Extra Where Clause' to restrict the result set as much as possible." +
		"</html>";

	public static final String   GROUP_NAME       = MainFrame.TCP_GROUP_SERVER;
	public static final String   GUI_ICON_FILE    = "images/"+CM_NAME+".png";

	public static final long     NEED_SRV_VERSION = 0;
	public static final long     NEED_CE_VERSION  = 0;

	public static final String[] MON_TABLES       = new String[] {"monProcessActivity", "monProcessWaits", "monWaitEventInfo", "monWaitClassInfo"};
	public static final String[] NEED_ROLES       = new String[] {"mon_role"};
	public static final String[] NEED_CONFIG      = new String[] {"enable monitoring=1", "wait event timing=1", "process wait events=1"};

	public static final String[] PCT_COLUMNS      = new String[] {};
	public static final String[] DIFF_COLUMNS     = new String[] {
		"CPUTime", "SpidWaitTime", "EventIdWaitTime", "Waits", "LogicalReads", "PhysicalReads", "PhysicalWrites", 
		"PagesRead", "PagesWritten", "TableAccesses", "IndexAccesses", "Transactions", "Commits", "Rollbacks",
		"IOSize1Page", "IOSize2Pages", "IOSize4Pages", "IOSize8Pages"};

	public static final boolean  NEGATIVE_DIFF_COUNTERS_TO_ZERO = true;
	public static final boolean  IS_SYSTEM_CM                   = true;
	public static final int      DEFAULT_POSTPONE_TIME          = 0;
	public static final int      DEFAULT_QUERY_TIMEOUT          = CountersModel.DEFAULT_sqlQueryTimeout;

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

		return new CmSpidCpuWait(counterController, guiController);
	}

	public CmSpidCpuWait(ICounterController counterController, IGuiController guiController)
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
		
		// The flowing columns is part of difference calculation
		// But Disregarded in the filter "Do NOT show unchanged counter rows"
		// this means that even if they HAVE a value, the will be filtered OUT from the JTable
		setDiffDissColumns( new String[] {"SpidWaitTime", "EventIdWaitTime"} );

		addTrendGraphs();
		
		CounterSetTemplates.register(this);
	}


	//------------------------------------------------------------
	// Implementation
	//------------------------------------------------------------
	private static final String  PROP_PREFIX                       = CM_NAME;

	public static final String  PROPKEY_sample_monSqlText         = PROP_PREFIX + ".sample.monSqltext";
	public static final boolean DEFAULT_sample_monSqlText         = false;

	public static final String  PROPKEY_sample_dbccSqlText        = PROP_PREFIX + ".sample.dbccSqltext";
	public static final boolean DEFAULT_sample_dbccSqlText        = false;

	public static final String  PROPKEY_sample_procCallStack      = PROP_PREFIX + ".sample.procCallStack";
	public static final boolean DEFAULT_sample_procCallStack      = false;

	public static final String  PROPKEY_sample_showplan           = PROP_PREFIX + ".sample.showplan";
	public static final boolean DEFAULT_sample_showplan           = false;

	public static final String  PROPKEY_sample_dbccStacktrace     = PROP_PREFIX + ".sample.dbccStacktrace";
	public static final boolean DEFAULT_sample_dbccStacktrace     = false;
	
	public static final String  PROPKEY_sample_freezeMda          = PROP_PREFIX + ".sample.freezeMda";
	public static final boolean DEFAULT_sample_freezeMda          = true;
	public static final long    NEED_SRV_VERSION_sample_freezeMda = Ver.ver(12,5,4);
	
	public static final String  PROPKEY_sample_extraWhereClause   = PROP_PREFIX + ".sample.extraWhereClause";
	public static final String  DEFAULT_sample_extraWhereClause   = "";

	public static final String  PROPKEY_sample_systemSpids        = PROP_PREFIX + ".sample.systemSpids";
	public static final boolean DEFAULT_sample_systemSpids        = true;

	@Override
	protected void registerDefaultValues()
	{
		super.registerDefaultValues();

		Configuration.registerDefaultValue(PROPKEY_sample_monSqlText,       DEFAULT_sample_monSqlText);
		Configuration.registerDefaultValue(PROPKEY_sample_dbccSqlText,      DEFAULT_sample_dbccSqlText);
		Configuration.registerDefaultValue(PROPKEY_sample_procCallStack,    DEFAULT_sample_procCallStack);
		Configuration.registerDefaultValue(PROPKEY_sample_showplan,         DEFAULT_sample_showplan);
		Configuration.registerDefaultValue(PROPKEY_sample_dbccStacktrace,   DEFAULT_sample_dbccStacktrace);
		Configuration.registerDefaultValue(PROPKEY_sample_freezeMda,        DEFAULT_sample_freezeMda);
		Configuration.registerDefaultValue(PROPKEY_sample_extraWhereClause, DEFAULT_sample_extraWhereClause);
		Configuration.registerDefaultValue(PROPKEY_sample_systemSpids,      DEFAULT_sample_systemSpids);
	}

	private void addTrendGraphs()
	{
	}

	@Override
	public int getDefaultDataSource()
	{
		return CountersModel.DATA_DIFF;
	}

	@Override
	protected TabularCntrPanel createGui()
	{
		return new CmSpidCpuWaitPanel(this);
	}

	@Override
	public String[] getDependsOnConfigForVersion(Connection conn, long srvVersion, boolean isClusterEnabled)
	{
		return NEED_CONFIG;
	}

	@Override
	public void addMonTableDictForVersion(Connection conn, long srvVersion, boolean isClusterEnabled)
	{
		try 
		{
			MonTablesDictionary mtd = MonTablesDictionaryManager.getInstance();
			mtd.addColumn("monProcessActivity",  "HasMonSqlText",      "Has values for: select SQLText from master..monProcessSQLText where SPID = <SPID>");
			mtd.addColumn("monProcessActivity",  "MonSqlText",                         "select SQLText from master..monProcessSQLText where SPID = <SPID>");
			mtd.addColumn("monProcessActivity",  "HasDbccSqlText",     "Has values for: DBCC sqltext(<SPID>)");
			mtd.addColumn("monProcessActivity",  "DbccSqlText",                        "DBCC sqltext(<SPID>)");
			mtd.addColumn("monProcessActivity",  "HasShowPlan",        "Has values for: sp_showplan <SPID>, null, null, null");
			mtd.addColumn("monProcessActivity",  "ShowPlanText",                       "sp_showplan <SPID>, null, null, null");
			mtd.addColumn("monProcessActivity",  "HasStacktrace",      "Has values for: DBCC stacktrace(<SPID>)");
			mtd.addColumn("monProcessActivity",  "DbccStacktrace",                     "DBCC stacktrace(<SPID>)");
			mtd.addColumn("monProcessActivity",  "HasProcCallStack",   "Has values for: select * from monProcessProcedures where SPID = <SPID>");
			mtd.addColumn("monProcessActivity",  "ProcCallStack",                      "select * from monProcessProcedures where SPID = <SPID>");
			mtd.addColumn("monProcessActivity",  "sampleTimeInMs",     "Time in milliseconds since last time the counters were refreshed.");
			
			mtd.addColumn("monProcessWaits",     "SpidWaitTime", 
					"<html>" +
						"Wait time in milliseconds for the SPID (fetch from monProcessActivity)<br>" +
						"Can be considered as a sum of all WaitTime for each individual WaitEventID.<br>" +
						"<br>" +
						"<b>Formula</b>: in SQL: A.WaitTime as SpidWaitTime<br>" +
					"</html>");

			mtd.addColumn("monProcessWaits",     "EventIdWaitTime", 
					"<html>" +
						"Wait time in milliseconds for the individual WaitEventID's (fetch from monProcessWaits).<br>" +
						"<br>" +
						"<b>Formula</b>: in SQL: W.WaitTime as EventIdWaitTime<br>" +
					"</html>");

			mtd.addColumn("monProcessWaits",     "WaitTimePerWait", 
				"<html>" +
					"Wait time in milliseconds per Wait, as in WaitEventId.<br>" +
					"<br>" +
					"<b>Formula</b>: diff.EventIdWaitTime / diff.Waits<br>" +
				"</html>");
			
			mtd.addColumn("monProcessWaits", "UserName", 
					"<html>" +
						"Active User Name.<br>" +
						"<br>" +
						"<b>Formula</b>: suser_name(ServerUserID)<br>" +
					"</html>");
			mtd.addColumn("monProcessWaits", "OrigUserName", 
					"<html>" +
						"Original Server User Identifier. This is the Server User Identifier before setting proxy.<br>" +
						"<br>" +
						"<b>Formula</b>: suser_name(OrigServerUserID)<br>" +
					"</html>");
		}
		catch (NameNotFoundException e) {/*ignore*/}
	}

	@Override
	public List<String> getPkForVersion(Connection conn, long srvVersion, boolean isClusterEnabled)
	{
		List <String> pkCols = new LinkedList<String>();

		if (isClusterEnabled)
			pkCols.add("InstanceID");

		pkCols.add("SPID");
		pkCols.add("KPID");
		pkCols.add("WaitEventID");

		return pkCols;
	}
	
	@Override
	public String getSqlForVersion(Connection conn, long srvVersion, boolean isClusterEnabled)
	{
		Configuration conf = Configuration.getCombinedConfiguration();
		boolean sample_freezeMda          = conf.getBooleanProperty(PROPKEY_sample_freezeMda,        DEFAULT_sample_freezeMda);
		String  sample_extraWhereClause   = conf.getProperty       (PROPKEY_sample_extraWhereClause, DEFAULT_sample_extraWhereClause);
		boolean sample_systemSpids        = conf.getBooleanProperty(PROPKEY_sample_systemSpids,      DEFAULT_sample_systemSpids);

		// MDA Freeze
		String sql_sample_freezeMda_declare = "";
		String sql_sample_freezeMda_begin   = "";
		String sql_sample_freezeMda_end     = "";
		if (sample_freezeMda && srvVersion > NEED_SRV_VERSION_sample_freezeMda)
		{
			sql_sample_freezeMda_declare = "declare @status int \n";
			sql_sample_freezeMda_begin   = "select @status = mdaconfig('freeze', 'begin') \n";
			sql_sample_freezeMda_end     = "select @status = mdaconfig('freeze', 'end')   \n";
		}

		// Do we have extra where clauses
		String sql_sample_extraWhereClause = "  -- Extra where clauses will go here. (it will look like: AND the_extra_where_clause) \n";
		if ( ! StringUtil.isNullOrBlank(sample_extraWhereClause) )
			sql_sample_extraWhereClause = "  and " + sample_extraWhereClause + "\n";

		// Should we sample SYSTEM SPID's
		String sql_sample_systemSpids = "";
		if ( ! sample_systemSpids )
			sql_sample_systemSpids = "  and A.ServerUserID > 0 \n";

		// Do the server support optimization goals?
		String optGoalPlan = "";
//		if (srvVersion >= 15020)
//		if (srvVersion >= 1502000)
		if (srvVersion >= Ver.ver(15,0,2))
		{
			optGoalPlan = "plan '(use optgoal allrows_dss)' \n";
		}

		// Get user name, if we are above ASE 12.5.4
		String UserName   = "";
		if (srvVersion >= Ver.ver(12,5,4))
			UserName = "UserName = suser_name(A.ServerUserID), ";
		
		String OrigUserName = ""; // in 16.0 SP2
		if (srvVersion >= Ver.ver(16,0,0, 2))
			OrigUserName = "OrigUserName = isnull(suser_name(A.OrigServerUserID), suser_name(A.ServerUserID)), ";
		
		String cols = "";

		// If ASE CE, add InstanceID
		if (isClusterEnabled)
			cols += "A.InstanceID, ";

		// ASE 15.7.0 ESD#2
		String IOSize1Page        = ""; // Number of 1 page physical reads performed by the process
		String IOSize2Pages       = ""; // Number of 2 pages physical reads performed for the process
		String IOSize4Pages       = ""; // Number of 4 pages physical reads performed for the process
		String IOSize8Pages       = ""; // Number of 8 pages physical reads performed for the process
		String nl_15702           = ""; // NL for this section
//		if (srvVersion >= 15702)
//		if (srvVersion >= 1570020)
		if (srvVersion >= Ver.ver(15,7,0,2))
		{
			IOSize1Page        = "A.IOSize1Page, ";
			IOSize2Pages       = "A.IOSize2Pages, ";
			IOSize4Pages       = "A.IOSize4Pages, ";
			IOSize8Pages       = "A.IOSize8Pages, ";
			nl_15702           = "\n";
		}

		// ASE 16.0 SP3
		String QueryOptimizationTime       = "";
		String ase160_sp3_nl               = "";
		if (srvVersion >= Ver.ver(16,0,0, 3)) // 16.0 SP3
		{
			QueryOptimizationTime       = "  A.QueryOptimizationTime, ";
			ase160_sp3_nl               = "\n";
		}
		
		cols += 
			"  A.SPID, A.KPID, " + UserName + OrigUserName + "\n" +
			"  sampleTimeInMs = convert(int,-1), \n" + // This value is replaced with a real value in class CounterSample
			"  A.CPUTime, A.WaitTime as SpidWaitTime, W.WaitTime as EventIdWaitTime, W.Waits, \n" +
			"  WaitTimePerWait = CASE WHEN W.Waits > 0 THEN convert(numeric(15,3), (W.WaitTime + 0.0) / W.Waits) ELSE convert(numeric(15,3), 0.0) END, \n" +
			"  W.WaitEventID, \n" +
			"  WaitClassDesc = convert(varchar(120),''), -- runtime replaced with cached values from monWaitClassInfo \n" +
			"  WaitEventDesc = convert(varchar(120),''), -- runtime replaced with cached values from monWaitEventInfo \n" +
			"  A.LogicalReads, A.PhysicalReads, A.PhysicalWrites, A.PagesRead, A.PagesWritten, \n" +
			IOSize1Page + IOSize2Pages + IOSize4Pages + IOSize8Pages + nl_15702 +
			"  A.TableAccesses, A.IndexAccesses, A.Transactions, A.Commits, A.Rollbacks, A.LocksHeld, A.MemUsageKB, \n" +
			QueryOptimizationTime + ase160_sp3_nl +
//			(srvVersion >= 15700 ? "  A.HeapMemoryInUseKB, A.HeapMemoryUsedHWM_KB , A.HeapMemoryReservedKB, A.HeapMemoryAllocs, \n" : "") +
//			(srvVersion >= 1570000 ? "  A.HeapMemoryInUseKB, A.HeapMemoryUsedHWM_KB , A.HeapMemoryReservedKB, A.HeapMemoryAllocs, \n" : "") +
			(srvVersion >= Ver.ver(15,7) ? "  A.HeapMemoryInUseKB, A.HeapMemoryUsedHWM_KB , A.HeapMemoryReservedKB, A.HeapMemoryAllocs, \n" : "") +
			"  HasMonSqlText=convert(bit,0), HasDbccSqlText=convert(bit,0), HasProcCallStack=convert(bit,0), \n" +
			"  HasShowPlan=convert(bit,0), HasStacktrace=convert(bit,0), \n" +
			"  MonSqlText=convert(text,null), \n" +
			"  DbccSqlText=convert(text,null), \n" +
			"  ProcCallStack=convert(text,null), \n" +
			"  ShowPlanText=convert(text,null), \n" +
			"  DbccStacktrace=convert(text,null) ";

		// The above I had in the Properties file for a long time
		String sql = 
			sql_sample_freezeMda_declare +
			sql_sample_freezeMda_begin +
			"select " + cols + " \n" +
			"from master..monProcessActivity A, master..monProcessWaits W \n" +
			"where A.SPID = W.SPID \n" +
			"  and A.KPID = W.KPID \n" +
		//	"  and W.WaitTime <= 0 \n" +
			(isClusterEnabled ? "  and A.InstanceID = W.InstanceID \n" : "") +
			"  and A.SPID != @@SPID \n" +
			sql_sample_extraWhereClause +
			sql_sample_systemSpids +
			"order by A.SPID, EventIdWaitTime desc \n" + 
			optGoalPlan +
			sql_sample_freezeMda_end +
			"\n";

		//------------------------------------------------
		// Finally return the SQL
		//------------------------------------------------
		return sql;
	}

	/** Used by the: Create 'Offline Session' Wizard */
	@Override
	public List<CmSettingsHelper> getLocalSettings()
	{
		Configuration conf = Configuration.getCombinedConfiguration();
		List<CmSettingsHelper> list = new ArrayList<>();
		
		list.add(new CmSettingsHelper("Get Monitored SQL Text",   PROPKEY_sample_monSqlText       , Boolean.class, conf.getBooleanProperty(PROPKEY_sample_monSqlText       , DEFAULT_sample_monSqlText      ), DEFAULT_sample_monSqlText      , CmSpidCpuWaitPanel.TOOLTIP_sample_monSqlText       ));
		list.add(new CmSettingsHelper("Get DBCC SQL Text",        PROPKEY_sample_dbccSqlText      , Boolean.class, conf.getBooleanProperty(PROPKEY_sample_dbccSqlText      , DEFAULT_sample_dbccSqlText     ), DEFAULT_sample_dbccSqlText     , CmSpidCpuWaitPanel.TOOLTIP_sample_dbccSqlText      ));
		list.add(new CmSettingsHelper("Get Procedure Call Stack", PROPKEY_sample_procCallStack    , Boolean.class, conf.getBooleanProperty(PROPKEY_sample_procCallStack    , DEFAULT_sample_procCallStack   ), DEFAULT_sample_procCallStack   , CmSpidCpuWaitPanel.TOOLTIP_sample_procCallStack    ));
		list.add(new CmSettingsHelper("Get Showplan",             PROPKEY_sample_showplan         , Boolean.class, conf.getBooleanProperty(PROPKEY_sample_showplan         , DEFAULT_sample_showplan        ), DEFAULT_sample_showplan        , CmSpidCpuWaitPanel.TOOLTIP_sample_showplan         ));
		list.add(new CmSettingsHelper("Get ASE Stacktrace",       PROPKEY_sample_dbccStacktrace   , Boolean.class, conf.getBooleanProperty(PROPKEY_sample_dbccStacktrace   , DEFAULT_sample_dbccStacktrace  ), DEFAULT_sample_dbccStacktrace  , CmSpidCpuWaitPanel.TOOLTIP_sample_dbccStacktrace   ));
		list.add(new CmSettingsHelper("Freeze MDA Counters",      PROPKEY_sample_freezeMda        , Boolean.class, conf.getBooleanProperty(PROPKEY_sample_freezeMda        , DEFAULT_sample_freezeMda       ), DEFAULT_sample_freezeMda       , CmSpidCpuWaitPanel.TOOLTIP_sample_freezeMda        ));
		list.add(new CmSettingsHelper("Extra Where Clause",       PROPKEY_sample_extraWhereClause , String.class,  conf.getProperty       (PROPKEY_sample_extraWhereClause , DEFAULT_sample_extraWhereClause), DEFAULT_sample_extraWhereClause, CmSpidCpuWaitPanel.TOOLTIP_sample_extraWhereClause ));
		list.add(new CmSettingsHelper("Get System SPID's",        PROPKEY_sample_systemSpids      , Boolean.class, conf.getBooleanProperty(PROPKEY_sample_systemSpids      , DEFAULT_sample_systemSpids     ), DEFAULT_sample_systemSpids     , CmSpidCpuWaitPanel.TOOLTIP_sample_systemSpids      ));

		return list;
	}


	@Override
	public String getToolTipTextOnTableCell(MouseEvent e, String colName, Object cellValue, int modelRow, int modelCol) 
	{
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
	/** add HTML around the string, and translate linebreaks into <br> */
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
	 * This so we do not have to do a sub select in the query that gets data
	 * doing it this way, means better performance, since the values are cached locally in memory
	 */
	@Override
	public void localCalculation(CounterSample newSample)
	{
		// Where are various columns located in the Vector 
		int pos_WaitEventID = -1, pos_WaitEventDesc = -1, pos_WaitClassDesc = -1;
		int waitEventID = 0;
		String waitEventDesc = "";
		String waitClassDesc = "";
	
		MonTablesDictionary mtd = MonTablesDictionaryManager.getInstance();
		if (mtd == null)
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
			if      (colName.equals("WaitEventID"))   pos_WaitEventID   = colId;
			else if (colName.equals("WaitEventDesc")) pos_WaitEventDesc = colId;
			else if (colName.equals("WaitClassDesc")) pos_WaitClassDesc = colId;

			// Noo need to continue, we got all our columns
			if (pos_WaitEventID >= 0 && pos_WaitEventDesc >= 0 && pos_WaitClassDesc >= 0)
				break;
		}

		if (pos_WaitEventID < 0 || pos_WaitEventDesc < 0 || pos_WaitClassDesc < 0)
		{
			_logger.debug("Can't find the position for columns ('WaitEventID'="+pos_WaitEventID+", 'WaitEventDesc'="+pos_WaitEventDesc+", 'WaitClassDesc'="+pos_WaitClassDesc+")");
			return;
		}
		
		// Loop on all counter rows
		for (int rowId=0; rowId < newSample.getRowCount(); rowId++) 
		{
			Object o_waitEventId  = newSample.getValueAt(rowId, pos_WaitEventID);

			if (o_waitEventId instanceof Number)
			{
				waitEventID = ((Number)o_waitEventId).intValue();

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

				newSample.setValueAt(waitEventDesc, rowId, pos_WaitEventDesc);
				newSample.setValueAt(waitClassDesc, rowId, pos_WaitClassDesc);
			}
		}
	}

	/** 
	 * here we could fill in the WaitEventDesc column with data from
	 * MonTableDictionary.. transforms a WaitEventId -> text description
	 * This so we do not have to do a subselect in tempdb wish is does now 
	 * the query that gets data doing it now. 
	 * Also do post lookups of dbcc sqltext, sp_showplan, dbcc stacktrace
	 */
	@Override
	public void localCalculation(CounterSample prevSample, CounterSample newSample, CounterSample diffData)
	{
		//Configuration conf = Configuration.getInstance(Configuration.TEMP);
		Configuration conf = Configuration.getCombinedConfiguration();
		boolean getShowplan       = conf == null ? true : conf.getBooleanProperty(PROPKEY_sample_showplan,       DEFAULT_sample_showplan);
		boolean getMonSqltext     = conf == null ? true : conf.getBooleanProperty(PROPKEY_sample_monSqlText,     DEFAULT_sample_monSqlText);
		boolean getDbccSqltext    = conf == null ? false: conf.getBooleanProperty(PROPKEY_sample_dbccSqlText,    DEFAULT_sample_dbccSqlText);
		boolean getProcCallStack  = conf == null ? true : conf.getBooleanProperty(PROPKEY_sample_procCallStack,  DEFAULT_sample_procCallStack);
		boolean getDbccStacktrace = conf == null ? false: conf.getBooleanProperty(PROPKEY_sample_dbccStacktrace, DEFAULT_sample_dbccStacktrace);
		
		// Where are various columns located in the Vector 
//		int pos_SpidWaitTime       = -1;
		int pos_EventIdWaitTime    = -1;
		int pos_Waits              = -1;
		int pos_WaitTimePerWait    = -1;
		int pos_SPID               = -1;
		int pos_UserName           = -1;
		int pos_HasShowPlan        = -1, pos_ShowPlanText   = -1;
		int pos_HasMonSqlText      = -1, pos_MonSqlText     = -1;
		int pos_HasDbccSqlText     = -1, pos_DbccSqlText    = -1;
		int pos_HasProcCallStack   = -1, pos_ProcCallStack  = -1;
		int pos_HasStacktrace      = -1, pos_DbccStacktrace = -1;
		// Find column Id's
		List<String> colNames = diffData.getColNames();
		if (colNames == null)
			return;

		for (int colId=0; colId < colNames.size(); colId++) 
		{
			String colName = colNames.get(colId);
//			if      (colName.equals("SpidWaitTime"))       pos_SpidWaitTime       = colId;
			if      (colName.equals("EventIdWaitTime"))    pos_EventIdWaitTime    = colId;
			else if (colName.equals("WaitTimePerWait"))    pos_WaitTimePerWait    = colId;
			else if (colName.equals("Waits"))              pos_Waits              = colId;
			else if (colName.equals("SPID"))               pos_SPID               = colId;
			else if (colName.equals("UserName"))           pos_UserName           = colId;
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


		int lastSPID = -1;
		
		// Loop on all diffData rows
		for (int rowId = 0; rowId < diffData.getRowCount(); rowId++)
		{
			int SPID            = ((Number)diffData.getValueAt(rowId, pos_SPID           )).intValue();
//			int SpidWaitTime    = ((Number)diffData.getValueAt(rowId, pos_SpidWaitTime   )).intValue();
			int EventIdWaitTime = ((Number)diffData.getValueAt(rowId, pos_EventIdWaitTime)).intValue();
//			int WaitTimePerWait = ((Number)diffData.getValueAt(rowId, pos_WaitTimePerWait)).intValue();
			int Waits           = ((Number)diffData.getValueAt(rowId, pos_Waits          )).intValue();

			if (Waits > 0)
			{
				// WaitTimePerWait = WaitTime / Waits;
				double calc = EventIdWaitTime / (Waits * 1.0);
//				double calc = WaitTimePerWait / (Waits * 1.0);

				BigDecimal newVal = new BigDecimal(calc).setScale(3, BigDecimal.ROUND_HALF_EVEN);;
				diffData.setValueAt(newVal, rowId, pos_WaitTimePerWait);
			}
			else
				diffData.setValueAt(new BigDecimal(0), rowId, pos_WaitTimePerWait);
			
//			String thisRowPk = diffData.getPkValue(rowId);
//			int prevPkRowId = (prevSample == null) ? -1 : prevSample.getRowNumberForPkValue(thisRowPk);
//			boolean prevPkExists = prevPkRowId >= 0;
			
			// Get EXTRA information for each SPID
			// but only ONCE per SPID (otherwise we will flood the system)
			if (SPID == lastSPID)
				continue;

			lastSPID = SPID;

			// If we did not have any Waits, is there any reason to continue with showplan etc...
			if (Waits <= 0)
				continue;

			String monSqlText    = "Not properly configured (need 'SQL batch capture' & 'max SQL text monitored').";
			String dbccSqlText   = "User does not have: sa_role";
			String procCallStack = "User does not have: sa_role";
			String showplan      = "User does not have: sa_role";
			String stacktrace    = "User does not have: sa_role";

			if (getMonitorConfig("SQL batch capture") > 0 && getMonitorConfig("max SQL text monitored") > 0)
			{
				// monProcessSQLText; needs 'enable monitoring', 'SQL batch capture' and 'max SQL text monitored' configuration parameters for this monitoring table to collect data.
				if (getMonSqltext)
					monSqlText  = AseConnectionUtils.monSqlText(getCounterController().getMonConnection(), SPID, true);
				else
					monSqlText = "This was disabled";
				if (monSqlText == null)
					monSqlText = "Not Available";
			}
			if (isServerRoleOrPermissionActive(AseConnectionUtils.SA_ROLE))
			{
				if (getDbccSqltext)
					dbccSqlText  = AseConnectionUtils.dbccSqlText(getCounterController().getMonConnection(), SPID, true);
				else
					dbccSqlText = "This was disabled";
				if (dbccSqlText == null)
					dbccSqlText = "Not Available";

				if (getProcCallStack)
					procCallStack  = AseConnectionUtils.monProcCallStack(getCounterController().getMonConnection(), SPID, true);
				else
					procCallStack = "This was disabled";
				if (procCallStack == null)
					procCallStack = "Not Available";

				if (getShowplan)
				{
					// If UserName is empty, then it's a system SPID, and we can't to sp_showplan on system spids. 
					String UserName = "anyone"; // this will be set to a good value below, if it's a system spid, it will be "" 
					if (pos_UserName >= 0)
						UserName = diffData.getValueAt(rowId, pos_UserName) + "";

					if ( ! StringUtil.isNullOrBlank(UserName) )
						showplan = AseConnectionUtils.getShowplan(getCounterController().getMonConnection(), SPID, "Showplan:", true);
					else
						showplan = "System SPID"; // 
				}
				else
					showplan = "This was disabled";
				if (showplan == null)
					showplan = "Not Available";

				if (getDbccStacktrace)
					stacktrace = AseConnectionUtils.dbccStacktrace(getCounterController().getMonConnection(), SPID, true, -1);
				else
					stacktrace = "This was disabled";
				if (stacktrace == null)
					stacktrace = "Not Available";
			}
			boolean b = true;
			b = !"This was disabled".equals(monSqlText)    && !"Not Available".equals(monSqlText)    && !monSqlText   .startsWith("Not properly configured");
			diffData.setValueAt(new Boolean(b), rowId, pos_HasMonSqlText);
			diffData.setValueAt(monSqlText,     rowId, pos_MonSqlText);

			b = !"This was disabled".equals(dbccSqlText)   && !"Not Available".equals(dbccSqlText)   && !dbccSqlText  .startsWith("User does not have");
			diffData.setValueAt(new Boolean(b), rowId, pos_HasDbccSqlText);
			diffData.setValueAt(dbccSqlText,    rowId, pos_DbccSqlText);

			b = !"This was disabled".equals(procCallStack) && !"Not Available".equals(procCallStack) && !procCallStack.startsWith("User does not have");
			diffData.setValueAt(new Boolean(b), rowId, pos_HasProcCallStack);
			diffData.setValueAt(procCallStack,  rowId, pos_ProcCallStack);

			b = !"This was disabled".equals(showplan)      && !"Not Available".equals(showplan)      && !showplan     .startsWith("User does not have") && !"System SPID".equals(showplan);
			diffData.setValueAt(new Boolean(b), rowId, pos_HasShowPlan);
			diffData.setValueAt(showplan,       rowId, pos_ShowPlanText);

			b = !"This was disabled".equals(stacktrace)    && !"Not Available".equals(stacktrace)    && !stacktrace   .startsWith("User does not have");
			diffData.setValueAt(new Boolean(b), rowId, pos_HasStacktrace);
			diffData.setValueAt(stacktrace,     rowId, pos_DbccStacktrace);

		} // end: loop all rows
	}
}
