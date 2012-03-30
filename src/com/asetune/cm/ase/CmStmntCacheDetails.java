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

	public static final int      NEED_SRV_VERSION = 15020;
	public static final int      NEED_CE_VERSION  = 0;

	public static final String[] MON_TABLES       = new String[] {"monCachedStatement"};
	public static final String[] NEED_ROLES       = new String[] {"mon_role"};
	public static final String[] NEED_CONFIG      = new String[] {"enable monitoring=1", "enable stmt cache monitoring=1", "statement cache size"}; // NO default for 'statement cache size' configuration

	public static final String[] PCT_COLUMNS      = new String[] {};
	public static final String[] DIFF_COLUMNS     = new String[] {
		"UseCountDiff", "NumRecompilesPlanFlushes", "NumRecompilesSchemaChanges"};

	public static final boolean  NEGATIVE_DIFF_COUNTERS_TO_ZERO = false;
	public static final boolean  IS_SYSTEM_CM                   = true;
	public static final int      DEFAULT_POSTPONE_TIME          = 0;
	public static final int      DEFAULT_QUERY_TIMEOUT          = 30;

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
		
		if (getQueryTimeout() == CountersModel.DEFAULT_sqlQueryTimeout)
			setQueryTimeout(DEFAULT_QUERY_TIMEOUT);

		addTrendGraphs();
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
		// NOTE: The function 'show_plan(-1,SSQLID,-1,-1)'
		//       returns a Ase Message (MsgNum=0) within the ResultSet
		//       those messages are placed in the column named 'msgAsColValue'
		//       see SamplingCnt.readResultset() for more details
		String sql = 
			"SELECT \n" +
			(isClusterEnabled ? " InstanceID, \n" : "") + //  The Server Instance Identifier (cluster only)
			" DBID, \n" +                       // The database ID from which the statement was cached.
			(aseVersion >= 15024 ? " DBName, \n" : "DBName = db_name(DBID), \n") + //  Name of the database (will be NULL if the database is no longer open)
			(aseVersion >= 15700 ? " StmtType, " : "") + // The type of the cached statement.
			" UserID, SUserID, SUserName = suser_name(SUserID), \n" +
			" SSQLID, \n" +                     // The unique identifier for a statement.
			" Hashkey, \n" +                    // The hashkey over the statement's text.
//			" HasShowplan   = CASE WHEN show_plan(-1,SSQLID,-1,-1) < 0 THEN convert(bit,0) ELSE convert(bit,1) END, \n" +
//			" HasSqltext    = convert(bit,1), \n" +
			(aseVersion >= 15700 ? " OptimizationGoal, " : "") + // The optimization goal stored in the statement cache
			(aseVersion >= 15700 ? " OptimizerLevel, \n" : "") + // The optimizer level stored in the statement cache
			                       " HasSqltext    = RUNTIME_REPLACE::HAS_SQL_TEXT, \n" +
			                       " HasShowplan   = RUNTIME_REPLACE::HAS_SHOWPLAN, \n" +
			(aseVersion >= 15700 ? " HasXmlPlan    = RUNTIME_REPLACE::HAS_XML_PLAN, \n" : "") +
			" UseCount, \n" +                   // The number of times this statement was used.
			" UseCountDiff = UseCount, \n" +    // The number of times this statement was used.
			" MetricsCount, \n" +               // Number of executions over which query metrics were captured.
			" MaxElapsedTime, MinElapsedTime, AvgElapsedTime, \n" + // Elapsed time value.
			" MaxLIO,         MinLIO,         AvgLIO, \n" +         // Logical IO
			" MaxPIO,         MinPIO,         AvgPIO, \n" +         // Physical IO
			" MaxCpuTime,     MinCpuTime,     AvgCpuTime, \n" +     // Execution time.
			" LastUsedDate, \n" +               // Date when this statement was last used.
			" LastRecompiledDate, \n" +         // Date when this statement was last recompiled.
			" CachedDate, \n" +                 // Date when this statement was cached.
			" MinPlanSizeKB, \n" +              // The size of a plan associated with this statement when dormant.
			" MaxPlanSizeKB, \n" +              // The size of a plan associated with this statement when it is in use.
			" CurrentUsageCount, \n" +          // The number of concurrent uses of this statement.
			" MaxUsageCount, \n" +              // The maximum number of times for which this statement was simultaneously used.
			" NumRecompilesSchemaChanges, \n" + // The number of times this statement was recompiled due to schema changes.
			" NumRecompilesPlanFlushes, \n" +   // The number of times this statement was recompiled because no usable plan was found.
			" HasAutoParams       = convert(bit,HasAutoParams), \n" + // Does this statement have any parameterized literals.
			" ParallelDegree, \n" +               // The parallel-degree session setting.
			" QuotedIdentifier    = convert(bit,QuotedIdentifier), \n" + // The quoted identifier session setting.
			(aseVersion < 15026 ? " TableCount, \n" : "") + // describeme
			" TransactionIsolationLevel, \n" +  // The transaction isolation level session setting.
			" TransactionMode, \n" +            // The transaction mode session setting.
			" SAAuthorization     = convert(bit,SAAuthorization), \n" +     // The SA authorization session setting.
			" SystemCatalogUpdate = convert(bit,SystemCatalogUpdate), \n" + // The system catalog update session setting.
			" StatementSize, \n" +              // The size of the statement's text in bytes.
//			" sqltext       = convert(text, show_cached_text(SSQLID)), \n" +
			                       " sqltext       = RUNTIME_REPLACE::DO_SQL_TEXT, \n" +
			(aseVersion >= 15700 ? " xmlPlan       = RUNTIME_REPLACE::DO_XML_PLAN, \n" : "") +
			                       " msgAsColValue = RUNTIME_REPLACE::DO_SHOWPLAN \n" + // this is the column where the show_plan() function will be placed (this is done in SamplingCnt.java:readResultset())
			"FROM master..monCachedStatement \n" +
			"WHERE MetricsCount > 0 \n";

		return sql;
	}

	/** Used by the: Create 'Offline Session' Wizard */
	@Override
	public Configuration getLocalConfiguration()
	{
		Configuration conf = Configuration.getCombinedConfiguration();
		Configuration lc = new Configuration();

		lc.setProperty(getName()+".sample.sqlText",  conf.getBooleanProperty(getName()+".sample.sqlText",   false));
		lc.setProperty(getName()+".sample.xmlPlan",  conf.getBooleanProperty(getName()+".sample.xmlPlan",   false));
		lc.setProperty(getName()+".sample.showplan", conf.getBooleanProperty(getName()+".sample.showplan",  false));

		return lc;
	}

	/** Used by the: Create 'Offline Session' Wizard */
	@Override
	public String getLocalConfigurationDescription(String propName)
	{
		if (propName.equals(getName()+".sample.sqlText"))  return "Get SQL Text (using: show_cached_text(SSQLID)) assosiated with the Cached Statement.";
		if (propName.equals(getName()+".sample.xmlPlan"))  return "Get Showplan assosiated with the Cached Statement.";
		if (propName.equals(getName()+".sample.showplan")) return "Get XML Plan (using: show_cached_plan_in_xml(SSQLID, 0)) assosiated with the Cached Statement, Note only in 15.7 and above.";
		return "";
	}
	@Override
	public String getLocalConfigurationDataType(String propName)
	{
		if (propName.equals(getName()+".sample.sqlText"))  return Boolean.class.getSimpleName();
		if (propName.equals(getName()+".sample.xmlPlan"))  return Boolean.class.getSimpleName();
		if (propName.equals(getName()+".sample.showplan")) return Boolean.class.getSimpleName();
		return "";
	}

	@Override
	public String getSql()
	{
		String sql = super.getSql();
		
		final String DEFAULT_xmlPlan_levelOfDetail = "0";
		
		Configuration conf = Configuration.getCombinedConfiguration();
		boolean sampleSqlText_chk    = (conf == null) ? false : conf.getBooleanProperty(getName()+".sample.sqlText",  false);
		boolean sampleXmlPlan_chk    = (conf == null) ? false : conf.getBooleanProperty(getName()+".sample.xmlPlan",  false);
		boolean sampleShowplan_chk   = (conf == null) ? false : conf.getBooleanProperty(getName()+".sample.showplan", false);
		String xmlPlan_levelOfDetail = (conf == null) ? "0"   : conf.getProperty(       getName()+".sample.xmlPlan.levelOfDetail", DEFAULT_xmlPlan_levelOfDetail);

		//----- SQL TEXT
		String hasSqlText = "convert(bit,1)";
		String  doSqltext = "convert(text, show_cached_text(SSQLID))";
		if ( ! sampleSqlText_chk )
		{
			hasSqlText = "convert(bit,0)";
			 doSqltext = "convert(text, 'CMstmntCacheDetails.sample.sqlText=false')";
		}
		sql = sql.replace("RUNTIME_REPLACE::HAS_SQL_TEXT", hasSqlText);
		sql = sql.replace("RUNTIME_REPLACE::DO_SQL_TEXT",   doSqltext);

		
		//----- XML PLAN
		if (getServerVersion() >= 15700)
		{
			// Check if the xmlPlan_levelOfDetail is an Integer, if not set it to a default value...
			try { Integer.parseInt(xmlPlan_levelOfDetail); }
			catch (NumberFormatException e) { xmlPlan_levelOfDetail = DEFAULT_xmlPlan_levelOfDetail; }

			String hasXmlPlan = "convert(bit,1)";
			String  doXmlPlan = "show_cached_plan_in_xml(SSQLID, 0, "+xmlPlan_levelOfDetail+")";
			if ( ! sampleXmlPlan_chk )
			{
				hasXmlPlan = "convert(bit,0)";
				 doXmlPlan = "convert(text, 'CMstmntCacheDetails.sample.xmlPlan=false')";
			}
			sql = sql.replace("RUNTIME_REPLACE::HAS_XML_PLAN", hasXmlPlan);
			sql = sql.replace("RUNTIME_REPLACE::DO_XML_PLAN",   doXmlPlan);
		}

		
		//----- SHOWPLAN
		String hasShowplan = "CASE WHEN show_plan(-1,SSQLID,-1,-1) < 0 THEN convert(bit,0) ELSE convert(bit,1) END";
		String  doShowplan = "convert(text, '')";
		if ( ! sampleShowplan_chk )
		{
			hasShowplan = "convert(bit,0)";
			 doShowplan = "convert(text, 'CMstmntCacheDetails.sample.showplan=false')";
		}
		sql = sql.replace("RUNTIME_REPLACE::HAS_SHOWPLAN", hasShowplan);
		sql = sql.replace("RUNTIME_REPLACE::DO_SHOWPLAN",   doShowplan);

		
		return sql;
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
