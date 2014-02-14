package com.asetune.cm.ase;

import java.awt.event.MouseEvent;
import java.sql.Connection;
import java.util.LinkedList;
import java.util.List;

import javax.naming.NameNotFoundException;

import com.asetune.ICounterController;
import com.asetune.IGuiController;
import com.asetune.MonTablesDictionary;
import com.asetune.cm.CmSybMessageHandler;
import com.asetune.cm.CounterSetTemplates;
import com.asetune.cm.CounterSetTemplates.Type;
import com.asetune.cm.CountersModel;
import com.asetune.cm.ase.gui.CmStmntCacheDetailsPanel;
import com.asetune.gui.MainFrame;
import com.asetune.gui.TabularCntrPanel;
import com.asetune.utils.Configuration;
import com.asetune.utils.StringUtil;

/**
 * @author Goran Schwarz (goran_schwarz@hotmail.com)
 */
public class CmStmntCacheDetails
extends CountersModel
{
//	private static Logger        _logger          = Logger.getLogger(CmStmntCacheDetails.class);
	private static final long    serialVersionUID = 1L;

	public static final String   CM_NAME          = CmStmntCacheDetails.class.getSimpleName();
	public static final String   SHORT_NAME       = "Statement Cache Details";
	public static final String   HTML_DESC        = 
		"<html>" +
		"Get detailed statistics about each SQL statement in the cache.<br>" +
		"This also includes the SQL statement itself<br>" +
		"And the execution plan (showplan) for the SQL statement<br>" +
		"</html>";

	public static final String   GROUP_NAME       = MainFrame.TCP_GROUP_CACHE;
	public static final String   GUI_ICON_FILE    = "images/"+CM_NAME+".png";

//	public static final int      NEED_SRV_VERSION = 15020;
	public static final int      NEED_SRV_VERSION = 1502000;
	public static final int      NEED_CE_VERSION  = 0;

	public static final String[] MON_TABLES       = new String[] {"monCachedStatement"};
	public static final String[] NEED_ROLES       = new String[] {"mon_role"};
	public static final String[] NEED_CONFIG      = new String[] {"enable monitoring=1", "enable stmt cache monitoring=1", "statement cache size"}; // NO default for 'statement cache size' configuration

	public static final String[] PCT_COLUMNS      = new String[] {};
	public static final String[] DIFF_COLUMNS     = new String[] {
		"UseCountDiff", "NumRecompilesPlanFlushes", "NumRecompilesSchemaChanges", 
		"LockWaits", "LockWaitTime", "SortCount", "SortSpilledCount", "TotalSortTime", 
		"ParallelDegreeReduced", "ParallelPlanRanSerial", "WorkerThreadDeficit",
		"TotalPIO", "TotalLIO", "TotalCpuTime", "TotalElapsedTime"};

	public static final boolean  NEGATIVE_DIFF_COUNTERS_TO_ZERO = false;
	public static final boolean  IS_SYSTEM_CM                   = true;
	public static final int      DEFAULT_POSTPONE_TIME          = 0;
	public static final int      DEFAULT_QUERY_TIMEOUT          = 30;

	@Override public int     getDefaultPostponeTime()                 { return DEFAULT_POSTPONE_TIME; }
	@Override public int     getDefaultQueryTimeout()                 { return DEFAULT_QUERY_TIMEOUT; }
	@Override public boolean getDefaultIsNegativeDiffCountersToZero() { return NEGATIVE_DIFF_COUNTERS_TO_ZERO; }
	@Override public Type    getTemplateLevel()                       { return Type.LARGE; }

	/**
	 * FACTORY  method to create the object
	 */
	public static CountersModel create(ICounterController counterController, IGuiController guiController)
	{
		if (guiController != null && guiController.hasGUI())
			guiController.splashWindowProgress("Loading: Counter Model '"+CM_NAME+"'");

		return new CmStmntCacheDetails(counterController, guiController);
	}

	public CmStmntCacheDetails(ICounterController counterController, IGuiController guiController)
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
	private static final String  PROP_PREFIX                         = CM_NAME;

	public static final String  PROPKEY_sample_sqlText               = PROP_PREFIX + ".sample.sqlText";
	public static final boolean DEFAULT_sample_sqlText               = false;

	public static final String  PROPKEY_sample_showplan              = PROP_PREFIX + ".sample.showplan";
	public static final boolean DEFAULT_sample_showplan              = false;

	public static final String  PROPKEY_sample_xmlPlan               = PROP_PREFIX + ".sample.xmlPlan";
	public static final boolean DEFAULT_sample_xmlPlan               = false;

	public static final String  PROPKEY_sample_xmlPlan_levelOfDetail = PROP_PREFIX + ".sample.xmlPlan.levelOfDetail";
	public static final int     DEFAULT_sample_xmlPlan_levelOfDetail = 0;

	public static final String  PROPKEY_sample_metricsCountGtZero    = PROP_PREFIX + ".sample.metricsCountGtZero";
	public static final boolean DEFAULT_sample_metricsCountGtZero    = true;

	@Override
	protected void registerDefaultValues()
	{
		super.registerDefaultValues();

		Configuration.registerDefaultValue(PROPKEY_sample_sqlText,               DEFAULT_sample_sqlText);
		Configuration.registerDefaultValue(PROPKEY_sample_showplan,              DEFAULT_sample_showplan);
		Configuration.registerDefaultValue(PROPKEY_sample_xmlPlan,               DEFAULT_sample_xmlPlan);
		Configuration.registerDefaultValue(PROPKEY_sample_xmlPlan_levelOfDetail, DEFAULT_sample_xmlPlan_levelOfDetail);
		Configuration.registerDefaultValue(PROPKEY_sample_metricsCountGtZero,    DEFAULT_sample_metricsCountGtZero);
	}


	private void addTrendGraphs()
	{
	}

	@Override
	protected TabularCntrPanel createGui()
	{
		return new CmStmntCacheDetailsPanel(this);
	}

	@Override
	public String[] getDependsOnConfigForVersion(Connection conn, int srvVersion, boolean isClusterEnabled)
	{
		return NEED_CONFIG;
	}

	@Override
	protected CmSybMessageHandler createSybMessageHandler()
	{
		CmSybMessageHandler msgHandler = super.createSybMessageHandler();

		//msgHandler.addDiscardMsgStr("Usage information at date and time");
		// Server Message:  Number  11096, Severity  10
		// Server 'GORAN_1_DS', Line 1:
		// The statement id is not valid. 
		msgHandler.addDiscardMsgNum(11096);

		return msgHandler;
	}

	@Override
	public void addMonTableDictForVersion(Connection conn, int aseVersion, boolean isClusterEnabled)
	{
		try 
		{
			String showplanTooltip = "<html>" +
				"The execution plan this SQL statement has.<br>" +
				"<b>Formula</b>: show_plan(-1,SSQLID,-1,-1)<br>" +
				"<b>Note</b>: It is possible for a single entry in the statement cache to be associated with multiple, and possibly different, SQL plans. <b>show_plan</b> displays only one of them." +
				"</html>";
			String sqltextTooltip =	"<html>" +
				"SQL Statement that is assigned to the SSQLID.<br>" +
				"<b>Formula</b>: show_cached_text(SSQLID)<br>" +
				"</html>";
			String xmlPlanTooltip =	"<html>" +
			    "XML Plan of the Statement that is assigned to the SSQLID.<br>" +
			    "<b>Formula</b>: show_cached_plan_in_xml(SSQLID, 0)<br>" +
			    "<b>Note</b>: If the FULL XML plan is not displayed, try to disable the 'Get Showplan' option.</html>";

			MonTablesDictionary mtd = MonTablesDictionary.getInstance();
			mtd.addColumn("monCachedStatement",  "msgAsColValue", showplanTooltip);
			mtd.addColumn("monCachedStatement",  "HasShowplan",   showplanTooltip);
			mtd.addColumn("monCachedStatement",  "sqltext",       sqltextTooltip);
			mtd.addColumn("monCachedStatement",  "HasSqltext",    sqltextTooltip);
			mtd.addColumn("monCachedStatement",  "xmlPlan",       xmlPlanTooltip);
			mtd.addColumn("monCachedStatement",  "HasXmlPlan",    xmlPlanTooltip);
		}
		catch (NameNotFoundException e) {/*ignore*/}
	}

	@Override
	public List<String> getPkForVersion(Connection conn, int aseVersion, boolean isClusterEnabled)
	{
		List <String> pkCols = new LinkedList<String>();

		if (isClusterEnabled)
			pkCols.add("InstanceID");

		pkCols.add("SSQLID");

		return pkCols;
	}

	@Override
	public String getSqlForVersion(Connection conn, int aseVersion, boolean isClusterEnabled)
	{
		Configuration conf = Configuration.getCombinedConfiguration();
		boolean sampleSqlText            = (conf == null) ? DEFAULT_sample_sqlText                 : conf.getBooleanProperty(PROPKEY_sample_sqlText,               DEFAULT_sample_sqlText);
		boolean sampleShowplan           = (conf == null) ? DEFAULT_sample_showplan                : conf.getBooleanProperty(PROPKEY_sample_showplan,              DEFAULT_sample_showplan);
		boolean sampleXmlPlan            = (conf == null) ? DEFAULT_sample_xmlPlan                 : conf.getBooleanProperty(PROPKEY_sample_xmlPlan,               DEFAULT_sample_xmlPlan);
		int     xmlPlan_levelOfDetail    = (conf == null) ? DEFAULT_sample_xmlPlan_levelOfDetail   : conf.getIntProperty(    PROPKEY_sample_xmlPlan_levelOfDetail, DEFAULT_sample_xmlPlan_levelOfDetail);
		boolean sampleMetricsCountGtZero = (conf == null) ? DEFAULT_sample_metricsCountGtZero      : conf.getBooleanProperty(PROPKEY_sample_metricsCountGtZero,    DEFAULT_sample_metricsCountGtZero);

		String comment = "";
		
		//----- SQL TEXT
		comment = "Property: "+PROPKEY_sample_sqlText+" is "+sampleSqlText+".";
		String sql_hasSqlText  = " HasSqltext    = convert(bit,0), -- "+comment+" \n";
		String sql_doSqltext   = " sqltext       = convert(text, '"+comment+"'), \n";
		if ( sampleSqlText )
		{
			sql_hasSqlText     = " HasSqltext    = convert(bit,1), \n";
			sql_doSqltext      = " sqltext       = convert(text, show_cached_text(SSQLID)), \n";
		}

		//----- SHOWPLAN
		comment = "Property: "+PROPKEY_sample_showplan+" is "+sampleShowplan+".";
		String sql_hasShowplan = " HasShowplan   = convert(bit,0), -- "+comment+" \n";
		String sql_doShowplan  = " msgAsColValue = convert(text, '"+comment+"'), \n";
		if ( sampleShowplan )
		{
			sql_hasShowplan    = " HasShowplan   = CASE WHEN show_plan(-1,SSQLID,-1,-1) < 0 THEN convert(bit,0) ELSE convert(bit,1) END, \n";
			sql_doShowplan     = " msgAsColValue = convert(text, ''), \n";
			// msgAsColValue: is the column where the show_plan() function will be placed (this is done in SamplingCnt.java:readResultset())
		}

		//----- XML PLAN
		comment = "Property: "+PROPKEY_sample_xmlPlan+" is "+sampleXmlPlan+".";
		String sql_hasXmlPlan  = " HasXmlPlan    = convert(bit,0), -- "+comment+" \n";
		String sql_doXmlPlan   = " xmlPlan       = convert(text, '"+comment+"'), \n";
//		if (aseVersion >= 15700)
		if (aseVersion >= 1570000)
		{
			if ( sampleXmlPlan )
			{
				sql_hasXmlPlan = " HasXmlPlan    = convert(bit,1), \n";
				sql_doXmlPlan  = " xmlPlan       = show_cached_plan_in_xml(SSQLID, 0, "+xmlPlan_levelOfDetail+"), \n";
			}
		}
		else
		{
			comment = "XML Plan is only available from ASE 15.7.0 or above.";
			sql_hasXmlPlan     = " HasXmlPlan    = convert(bit,0), -- "+comment+" \n";
			sql_doXmlPlan      = " xmlPlan       = convert(text, '"+comment+"'), \n";
		}
		
		//----- SHOWPLAN
		comment = "Property: "+PROPKEY_sample_metricsCountGtZero+" is "+sampleMetricsCountGtZero+".";
		String sql_metricsCountGtZero = "  and MetricsCount > 0 -- "+comment+" \n";
		if ( ! sampleMetricsCountGtZero )
		{
			sql_metricsCountGtZero = "  -- and MetricsCount > 0 -- "+comment+" \n";
		}


		// ASE 15.7.0 ESD#2
		String AvgScanRows            = ""; // Average scanned rows read per execution
		String MaxScanRows            = ""; // Maximum scanned rows read per execution
		String AvgQualifyingReadRows  = ""; // Average qualifying data rows for read DML per execution
		String MaxQualifyingReadRows  = ""; // Maximum qualifying data rows for read DML per execution
		String AvgQualifyingWriteRows = ""; // Average qualifying data rows for write DML per execution
		String MaxQualifyingWriteRows = ""; // Maximum qualifying data rows for write DML per execution
		String LockWaits              = ""; // Total number of lock waits
		String LockWaitTime	          = ""; // Total lock-wait time (ms)
		String SortCount              = ""; // Total number of sort operations
		String SortSpilledCount       = ""; // Total number of sort operations spilling to disk
		String TotalSortTime          = ""; // Total sort time (ms)
		String MaxSortTime            = ""; // Maximum sort time (ms)
		String ParallelDegreeReduced  = ""; // The number of times the degree of parallelism in the plan was reduced but it still executed as a parallel query (parallel plans only)
		String ParallelPlanRanSerial  = ""; // The number of times a parallel query plan was adjusted so that it executed without parallelism (parallel plans only)
		String WorkerThreadDeficit    = ""; // The total thread deficit for all executions of a parallel plan (parallel plans only)
		String nl_15702               = ""; // NL for this section
//		if (aseVersion >= 15702)
		if (aseVersion >= 1570020)
		{
			AvgScanRows			   = "AvgScanRows, ";             // no Diff
			MaxScanRows            = "MaxScanRows, ";             // no Diff
			AvgQualifyingReadRows  = "AvgQualifyingReadRows, ";   // no Diff
			MaxQualifyingReadRows  = "MaxQualifyingReadRows, ";   // no Diff
			AvgQualifyingWriteRows = "AvgQualifyingWriteRows, ";  // no Diff
			MaxQualifyingWriteRows = "MaxQualifyingWriteRows, ";  // no Diff
			LockWaits              = "LockWaits, ";               // DIFF COUNTER
			LockWaitTime           = "LockWaitTime, ";            // DIFF COUNTER
			SortCount              = "SortCount, ";               // DIFF COUNTER
			SortSpilledCount       = "SortSpilledCount, ";        // DIFF COUNTER
			TotalSortTime          = "TotalSortTime, ";           // DIFF COUNTER
			MaxSortTime            = "MaxSortTime, ";             // no diff
			ParallelDegreeReduced  = "ParallelDegreeReduced, ";   // DIFF COUNTER
			ParallelPlanRanSerial  = "ParallelPlanRanSerial, ";   // DIFF COUNTER
			WorkerThreadDeficit    = "WorkerThreadDeficit, ";     // DIFF COUNTER
			nl_15702           = "\n";
		}

		// ASE 16.0
		String TotalPIO            = ""; // The total PIO value.
		String TotalLIO            = ""; // The total LIO value.
		String TotalCpuTime        = ""; // The total execution time value. (ms)
		String TotalElapsedTime    = ""; // The total elapsed time value. (ms)
		if (aseVersion >= 1600000)
		{
			TotalPIO		 = "TotalPIO, ";         // DIFF COUNTER
			TotalLIO         = "TotalLIO, ";         // DIFF COUNTER
			TotalCpuTime     = "TotalCpuTime, ";     // DIFF COUNTER
			TotalElapsedTime = "TotalElapsedTime, "; // DIFF COUNTER
		}

		
		// NOTE: The function 'show_plan(-1,SSQLID,-1,-1)'
		//       returns a Ase Message (MsgNum=0) within the ResultSet
		//       those messages are placed in the column named 'msgAsColValue'
		//       see SamplingCnt.readResultset() for more details
		String cols = 
			(isClusterEnabled ? " InstanceID, \n" : "") + //  The Server Instance Identifier (cluster only)
			" DBID, \n" +                       // The database ID from which the statement was cached.
//			(aseVersion >= 15024 ? " DBName, \n" : "DBName = db_name(DBID), \n") + //  Name of the database (will be NULL if the database is no longer open)
			(aseVersion >= 1502040 ? " DBName, \n" : "DBName = db_name(DBID), \n") + //  Name of the database (will be NULL if the database is no longer open)
//			(aseVersion >= 15700 ? " StmtType, " : "") + // The type of the cached statement.
			(aseVersion >= 1570000 ? " StmtType, " : "") + // The type of the cached statement.
			" UserID, SUserID, SUserName = suser_name(SUserID), \n" +
			" SSQLID, \n" +                     // The unique identifier for a statement.
			" Hashkey, \n" +                    // The hashkey over the statement's text.
//			" HasShowplan   = CASE WHEN show_plan(-1,SSQLID,-1,-1) < 0 THEN convert(bit,0) ELSE convert(bit,1) END, \n" +
//			" HasSqltext    = convert(bit,1), \n" +
//			(aseVersion >= 15700 ? " OptimizationGoal, " : "") + // The optimization goal stored in the statement cache
			(aseVersion >= 1570000 ? " OptimizationGoal, " : "") + // The optimization goal stored in the statement cache
//			(aseVersion >= 15700 ? " OptimizerLevel, \n" : "") + // The optimizer level stored in the statement cache
			(aseVersion >= 1570000 ? " OptimizerLevel, \n" : "") + // The optimizer level stored in the statement cache
			sql_hasSqlText +
			sql_hasShowplan + 
			sql_hasXmlPlan +
			" UseCount, \n" +                   // The number of times this statement was used.
			" UseCountDiff = UseCount, \n" +    // The number of times this statement was used.
			LockWaits + LockWaitTime + nl_15702 +
			SortCount + SortSpilledCount + TotalSortTime + MaxSortTime + nl_15702 + 
			" MetricsCount, \n" +               // Number of executions over which query metrics were captured.
			" MaxElapsedTime, MinElapsedTime, AvgElapsedTime, "+ TotalElapsedTime + "\n" + // Elapsed time value.
			" MaxLIO,         MinLIO,         AvgLIO,         "+ TotalLIO         + "\n" + // Logical IO
			" MaxPIO,         MinPIO,         AvgPIO,         "+ TotalPIO         + "\n" + // Physical IO
			" MaxCpuTime,     MinCpuTime,     AvgCpuTime,     "+ TotalCpuTime     + "\n" + // Execution time.
			" LastUsedDate, \n" +               // Date when this statement was last used.
			" LastRecompiledDate, \n" +         // Date when this statement was last recompiled.
			" CachedDate, \n" +                 // Date when this statement was cached.
			" MinPlanSizeKB, \n" +              // The size of a plan associated with this statement when dormant.
			" MaxPlanSizeKB, \n" +              // The size of a plan associated with this statement when it is in use.
			" CurrentUsageCount, \n" +          // The number of concurrent uses of this statement.
			" MaxUsageCount, \n" +              // The maximum number of times for which this statement was simultaneously used.
			AvgScanRows + MaxScanRows + nl_15702 +
			AvgQualifyingReadRows + MaxQualifyingReadRows + nl_15702 +
			AvgQualifyingWriteRows + MaxQualifyingWriteRows + nl_15702 + 
			" NumRecompilesSchemaChanges, \n" + // The number of times this statement was recompiled due to schema changes.
			" NumRecompilesPlanFlushes, \n" +   // The number of times this statement was recompiled because no usable plan was found.
			" HasAutoParams       = convert(bit,HasAutoParams), \n" + // Does this statement have any parameterized literals.
			" ParallelDegree, \n" +               // The parallel-degree session setting.
			ParallelDegreeReduced + ParallelPlanRanSerial + WorkerThreadDeficit + nl_15702 + 
			" QuotedIdentifier    = convert(bit,QuotedIdentifier), \n" + // The quoted identifier session setting.
//			(aseVersion < 15026 ? " TableCount, \n" : "") + // describeme
			(aseVersion < 1502060 ? " TableCount, \n" : "") + // describeme
			" TransactionIsolationLevel, \n" +  // The transaction isolation level session setting.
			" TransactionMode, \n" +            // The transaction mode session setting.
			" SAAuthorization     = convert(bit,SAAuthorization), \n" +     // The SA authorization session setting.
			" SystemCatalogUpdate = convert(bit,SystemCatalogUpdate), \n" + // The system catalog update session setting.
			" StatementSize, \n" +              // The size of the statement's text in bytes.
			sql_doSqltext + 
			sql_doXmlPlan +
			sql_doShowplan;
		
		// Remove last comma if any
		cols = StringUtil.removeLastComma(cols);
			
		String sql = 
			"SELECT \n" +
			cols + "\n" +
			"FROM master..monCachedStatement \n" +
			"WHERE 1 = 1 \n" +
			sql_metricsCountGtZero;

		return sql;
	}

	/** Used by the: Create 'Offline Session' Wizard */
	@Override
	public Configuration getLocalConfiguration()
	{
		Configuration conf = Configuration.getCombinedConfiguration();
		Configuration lc = new Configuration();

		lc.setProperty(PROPKEY_sample_sqlText,               conf.getBooleanProperty(PROPKEY_sample_sqlText,               DEFAULT_sample_sqlText));
		lc.setProperty(PROPKEY_sample_showplan,              conf.getBooleanProperty(PROPKEY_sample_showplan,              DEFAULT_sample_showplan));
		lc.setProperty(PROPKEY_sample_xmlPlan,               conf.getBooleanProperty(PROPKEY_sample_xmlPlan,               DEFAULT_sample_xmlPlan));
		lc.setProperty(PROPKEY_sample_xmlPlan_levelOfDetail, conf.getIntProperty(    PROPKEY_sample_xmlPlan_levelOfDetail, DEFAULT_sample_xmlPlan_levelOfDetail));
		lc.setProperty(PROPKEY_sample_metricsCountGtZero,    conf.getBooleanProperty(PROPKEY_sample_metricsCountGtZero,    DEFAULT_sample_metricsCountGtZero));
		return lc;
	}

	/** Used by the: Create 'Offline Session' Wizard */
	@Override
	public String getLocalConfigurationDescription(String propName)
	{
		if (propName.equals(PROPKEY_sample_sqlText))               return CmStmntCacheDetailsPanel.TOOLTIP_sample_sqlText;
		if (propName.equals(PROPKEY_sample_showplan))              return CmStmntCacheDetailsPanel.TOOLTIP_sample_showplan;
		if (propName.equals(PROPKEY_sample_xmlPlan))               return CmStmntCacheDetailsPanel.TOOLTIP_sample_xmlPlan;
		if (propName.equals(PROPKEY_sample_xmlPlan_levelOfDetail)) return CmStmntCacheDetailsPanel.TOOLTIP_sample_xmlPlan_levelOfDetail;
		if (propName.equals(PROPKEY_sample_metricsCountGtZero))    return CmStmntCacheDetailsPanel.TOOLTIP_sample_metricsCountGtZero;
		return "";
	}
	@Override
	public String getLocalConfigurationDataType(String propName)
	{
		if (propName.equals(PROPKEY_sample_sqlText))               return Boolean.class.getSimpleName();
		if (propName.equals(PROPKEY_sample_showplan))              return Boolean.class.getSimpleName();
		if (propName.equals(PROPKEY_sample_xmlPlan))               return Boolean.class.getSimpleName();
		if (propName.equals(PROPKEY_sample_xmlPlan_levelOfDetail)) return Integer.class.getSimpleName();
		if (propName.equals(PROPKEY_sample_metricsCountGtZero))    return Boolean.class.getSimpleName();
		return "";
	}

	@Override
	public String getToolTipTextOnTableCell(MouseEvent e, String colName, Object cellValue, int modelRow, int modelCol) 
	{
		// SHOWPLAN
		if ("HasShowplan".equals(colName) || "msgAsColValue".equals(colName))
		{
			int pos_showplanText = findColumn("msgAsColValue");
			if (pos_showplanText > 0)
			{
				Object cellVal = getValueAt(modelRow, pos_showplanText);
				if (cellVal instanceof String)
					return toHtmlString((String) cellVal, true);
			}
		}

		// XML PLAN
		if ("HasXmlPlan".equals(colName) || "xmlPlan".equals(colName))
		{
			int pos_xmlPlanText = findColumn("xmlPlan");
			if (pos_xmlPlanText > 0)
			{
				Object cellVal = getValueAt(modelRow, pos_xmlPlanText);
				if (cellVal instanceof String)
					return toHtmlString((String) cellVal, false);
			}
		}

		// SQLTEXT
		if ("HasSqltext".equals(colName) || "sqltext".equals(colName))
		{
			int pos_sqltext = findColumn("sqltext");
			if (pos_sqltext > 0)
			{
				Object cellVal = getValueAt(modelRow, pos_sqltext);
				if (cellVal instanceof String)
					return toHtmlString((String) cellVal, true);
			}
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

	/** 
	 * Get number of rows to save/request ddl information for 
	 */
	@Override
	public int getMaxNumOfDdlsToPersist()
	{
		return 10;
	}

	@Override
	public String[] getDdlDetailsSortOnColName()
	{
		String[] sa = {"UseCount", "UseCountDiff", "AvgLIO", "AvgElapsedTime"};
		return sa;
	}
}
