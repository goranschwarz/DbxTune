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
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import javax.naming.NameNotFoundException;

import org.apache.log4j.Logger;

import com.asetune.ICounterController;
import com.asetune.IGuiController;
import com.asetune.cache.XmlPlanCache;
import com.asetune.cm.CmSettingsHelper;
import com.asetune.cm.CmSybMessageHandler;
import com.asetune.cm.CounterSample;
import com.asetune.cm.CounterSetTemplates;
import com.asetune.cm.CounterSetTemplates.Type;
import com.asetune.cm.CountersModel;
import com.asetune.cm.ase.gui.CmStmntCacheDetailsPanel;
import com.asetune.config.dict.MonTablesDictionary;
import com.asetune.config.dict.MonTablesDictionaryManager;
import com.asetune.config.dict.RemarkDictionary;
import com.asetune.gui.AsePlanViewer;
import com.asetune.gui.MainFrame;
import com.asetune.gui.TabularCntrPanel;
import com.asetune.pcs.PcsColumnOptions;
import com.asetune.pcs.PcsColumnOptions.ColumnType;
import com.asetune.sql.conn.DbxConnection;
import com.asetune.sql.conn.info.DbmsVersionInfo;
import com.asetune.sql.conn.info.DbmsVersionInfoSybaseAse;
import com.asetune.utils.AseConnectionUtils;
import com.asetune.utils.Configuration;
import com.asetune.utils.StringUtil;
import com.asetune.utils.Ver;

/**
 * @author Goran Schwarz (goran_schwarz@hotmail.com)
 */
public class CmStmntCacheDetails
extends CountersModel
{
	private static Logger        _logger          = Logger.getLogger(CmStmntCacheDetails.class);
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

//	public static final long     NEED_SRV_VERSION = 15020;
//	public static final long     NEED_SRV_VERSION = 1502000;
	public static final long     NEED_SRV_VERSION = Ver.ver(15,0,2);
	public static final long     NEED_CE_VERSION  = 0;

	public static final String[] MON_TABLES       = new String[] {"monCachedStatement"};
	public static final String[] NEED_ROLES       = new String[] {"mon_role"};
	public static final String[] NEED_CONFIG      = new String[] {"enable monitoring=1", "enable stmt cache monitoring=1", "statement cache size"}; // NO default for 'statement cache size' configuration

	public static final String[] PCT_COLUMNS      = new String[] {};
	public static final String[] DIFF_COLUMNS     = new String[] {
		"UseCountDiff", "NumRecompilesPlanFlushes", "NumRecompilesSchemaChanges", 
		"LockWaitsDiff", "LockWaitTimeDiff", "SortCountDiff", "SortSpilledCount", "TotalSortTimeDiff", 
		"ParallelDegreeReduced", "ParallelPlanRanSerial", "WorkerThreadDeficit",
		"TotalPioDiff", "TotalLioDiff", "TotalCpuTimeDiff", "TotalElapsedTimeDiff", 
		"TotalEstWaitTimeDiff"};

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


	public static final String  PROPKEY_xmlPlan_bulkThreshold        = PROP_PREFIX + ".xmlPlan.bulkThreshold";
	public static final int     DEFAULT_xmlPlan_bulkThreshold        = 200;

	@Override
	protected void registerDefaultValues()
	{
		super.registerDefaultValues();

		Configuration.registerDefaultValue(PROPKEY_sample_sqlText,               DEFAULT_sample_sqlText);
		Configuration.registerDefaultValue(PROPKEY_sample_showplan,              DEFAULT_sample_showplan);
		Configuration.registerDefaultValue(PROPKEY_sample_xmlPlan,               DEFAULT_sample_xmlPlan);
		Configuration.registerDefaultValue(PROPKEY_sample_xmlPlan_levelOfDetail, DEFAULT_sample_xmlPlan_levelOfDetail);
		Configuration.registerDefaultValue(PROPKEY_sample_metricsCountGtZero,    DEFAULT_sample_metricsCountGtZero);
		
		Configuration.registerDefaultValue(PROPKEY_xmlPlan_bulkThreshold,        DEFAULT_xmlPlan_bulkThreshold);
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
	public String[] getDependsOnConfigForVersion(DbxConnection conn, DbmsVersionInfo versionInfo)
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
	public void addMonTableDictForVersion(DbxConnection conn, DbmsVersionInfo versionInfo)
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

			MonTablesDictionary mtd = MonTablesDictionaryManager.getInstance();
			mtd.addColumn("monCachedStatement",  "msgAsColValue", showplanTooltip);
			mtd.addColumn("monCachedStatement",  "HasShowplan",   showplanTooltip);
			mtd.addColumn("monCachedStatement",  "sqltext",       sqltextTooltip);
			mtd.addColumn("monCachedStatement",  "HasSqltext",    sqltextTooltip);
			mtd.addColumn("monCachedStatement",  "xmlPlan",       xmlPlanTooltip);
			mtd.addColumn("monCachedStatement",  "HasXmlPlan",    xmlPlanTooltip);
			mtd.addColumn("monCachedStatement",  "Remark",        "<html>Some tip of what's happening with this PerformanceCounter.<br><b>Tip</b>: \"Hover\" over the cell to get more information on the Tip.</html>");

			mtd.addColumn("monCachedStatement",  "TotalEstWaitTime",     "<html>Total Estimated Wait Time <br><b>Algorithm</b>: TotalElapsedTime - TotalCpuTime </html>");
			mtd.addColumn("monCachedStatement",  "TotalEstWaitTimeDiff", "<html>Total Estimated Wait Time <br><b>Algorithm</b>: TotalElapsedTime - TotalCpuTime </html>");

			mtd.addColumn("monCachedStatement",  "AvgReadEfficiancy",  "<html>A Percent Calculation of AvgScanRows and AvgQualifyingReadRows.  (if -1 then: AvgScanRows=0) <br><b> AvgQualifyingReadRows  / AvgScanRows * 100 </html>");
			mtd.addColumn("monCachedStatement",  "AvgWriteEfficiancy", "<html>A Percent Calculation of AvgScanRows and AvgQualifyingWriteRows. (if -1 then: AvgScanRows=0) <br><b> AvgQualifyingWriteRows / AvgScanRows * 100 </html>");
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
			map.put("sqltext"      , new PcsColumnOptions(ColumnType.DICTIONARY_COMPRESSION));
			map.put("xmlPlan"      , new PcsColumnOptions(ColumnType.DICTIONARY_COMPRESSION));
			map.put("msgAsColValue", new PcsColumnOptions(ColumnType.DICTIONARY_COMPRESSION));

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

		pkCols.add("SSQLID");

		return pkCols;
	}

	@Override
	public String getSqlForVersion(DbxConnection conn, DbmsVersionInfo versionInfo)
	{
		DbmsVersionInfoSybaseAse aseVersionInfo = (DbmsVersionInfoSybaseAse) versionInfo;
		long    srvVersion       = aseVersionInfo.getLongVersion();
		boolean isClusterEnabled = aseVersionInfo.isClusterEdition();

		Configuration conf = Configuration.getCombinedConfiguration();
		boolean sampleSqlText            = (conf == null) ? DEFAULT_sample_sqlText                 : conf.getBooleanProperty(PROPKEY_sample_sqlText,               DEFAULT_sample_sqlText);
		boolean sampleShowplan           = (conf == null) ? DEFAULT_sample_showplan                : conf.getBooleanProperty(PROPKEY_sample_showplan,              DEFAULT_sample_showplan);
		boolean sampleXmlPlan            = (conf == null) ? DEFAULT_sample_xmlPlan                 : conf.getBooleanProperty(PROPKEY_sample_xmlPlan,               DEFAULT_sample_xmlPlan);
//		int     xmlPlan_levelOfDetail    = (conf == null) ? DEFAULT_sample_xmlPlan_levelOfDetail   : conf.getIntProperty(    PROPKEY_sample_xmlPlan_levelOfDetail, DEFAULT_sample_xmlPlan_levelOfDetail);
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
			// msgAsColValue: is the column where the show_plan() function will be placed (this is done in CounterSample.java:readResultset())
		}

		//----- XML PLAN
		comment = "Property: "+PROPKEY_sample_xmlPlan+" is "+sampleXmlPlan+".";
		String sql_hasXmlPlan  = " HasXmlPlan    = convert(bit,0), -- "+comment+" \n";
		String sql_doXmlPlan   = " xmlPlan       = convert(text, '"+comment+"'), \n";
		if (srvVersion >= Ver.ver(15,7))
		{
			if ( sampleXmlPlan )
			{
				sql_hasXmlPlan = " HasXmlPlan    = convert(bit,1), \n";
//				sql_doXmlPlan  = " xmlPlan       = show_cached_plan_in_xml(SSQLID, 0, "+xmlPlan_levelOfDetail+"), \n";
				sql_doXmlPlan  = " xmlPlan       = convert(text, 'use:XmlPlanCache'), \n";
			}
		}
		else
		{
			comment = "XML Plan is only available from ASE 15.7.0 or above.";
			sql_hasXmlPlan = " HasXmlPlan    = convert(bit,0), -- "+comment+" \n";
			sql_doXmlPlan  = " xmlPlan       = convert(text, '"+comment+"'), \n";
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
		String AvgReadEfficiancy      = "";
		String AvgWriteEfficiancy     = "";
		String LockWaits              = ""; // Total number of lock waits
		String LockWaitsDiff          = ""; // Total number of lock waits
		String LockWaitTime	          = ""; // Total lock-wait time (ms)
		String LockWaitTimeDiff	      = ""; // Total lock-wait time (ms)
		String SortCount              = ""; // Total number of sort operations
		String SortCountDiff          = ""; // Total number of sort operations
		String SortSpilledCount       = ""; // Total number of sort operations spilling to disk
		String TotalSortTime          = ""; // Total sort time (ms)
		String TotalSortTimeDiff      = "";
		String MaxSortTime            = ""; // Maximum sort time (ms)
		String ParallelDegreeReduced  = ""; // The number of times the degree of parallelism in the plan was reduced but it still executed as a parallel query (parallel plans only)
		String ParallelPlanRanSerial  = ""; // The number of times a parallel query plan was adjusted so that it executed without parallelism (parallel plans only)
		String WorkerThreadDeficit    = ""; // The total thread deficit for all executions of a parallel plan (parallel plans only)
		String nl_15702               = ""; // NL for this section
//		if (srvVersion >= 15702)
//		if (srvVersion >= 1570020)
		if (srvVersion >= Ver.ver(15,7,0,2))
		{
			AvgScanRows			   = "AvgScanRows, ";                       // no Diff
			MaxScanRows            = "MaxScanRows, ";                       // no Diff
			AvgQualifyingReadRows  = "AvgQualifyingReadRows, ";             // no Diff
			MaxQualifyingReadRows  = "MaxQualifyingReadRows, ";             // no Diff
			AvgQualifyingWriteRows = "AvgQualifyingWriteRows, ";            // no Diff
			MaxQualifyingWriteRows = "MaxQualifyingWriteRows, ";            // no Diff
			AvgReadEfficiancy      = "AvgReadEfficiancy  = convert(numeric(9,1), isnull(AvgQualifyingReadRows*1.0  / nullif(1.0*AvgScanRows, 0) * 100.0, -1)), "; // no DIFF
			AvgWriteEfficiancy     = "AvgWriteEfficiancy = convert(numeric(9,1), isnull(AvgQualifyingWriteRows*1.0 / nullif(1.0*AvgScanRows, 0) * 100.0, -1)), "; // no DIFF
			LockWaits              = "LockWaits, ";                         // no Diff
			LockWaitsDiff          = "LockWaitsDiff = LockWaits, ";         // DIFF COUNTER
			LockWaitTime           = "LockWaitTime, ";                      // no Diff
			LockWaitTimeDiff       = "LockWaitTimeDiff = LockWaitTime, ";   // DIFF COUNTER
			SortCount              = "SortCount, ";                         // no Diff
			SortCountDiff          = "SortCountDiff = SortCount, ";         // DIFF COUNTER
			SortSpilledCount       = "SortSpilledCount, ";                  // DIFF COUNTER
			TotalSortTime          = "TotalSortTime, ";                     // no Diff
			TotalSortTimeDiff      = "TotalSortTimeDiff = TotalSortTime, "; // DIFF COUNTER
			MaxSortTime            = "MaxSortTime, ";                       // no diff
			ParallelDegreeReduced  = "ParallelDegreeReduced, ";             // DIFF COUNTER
			ParallelPlanRanSerial  = "ParallelPlanRanSerial, ";             // DIFF COUNTER
			WorkerThreadDeficit    = "WorkerThreadDeficit, ";               // DIFF COUNTER
			nl_15702           = "\n";
		}

		// ASE 16.0 (back ported to 15.7 SP130)
		String TotalPIO                = ""; // The total PIO value.
		String TotalLIO                = ""; // The total LIO value.
		String TotalCpuTime            = ""; // The total execution time value. (ms)
		String TotalElapsedTime        = ""; // The total elapsed time value. (ms)
		String TotalEstWaitTime        = ""; // TotalElapsedTime - TotalCpuTime
		String TotalPioDiff            = ""; // The total PIO value.
		String TotalLioDiff            = ""; // The total LIO value.
		String TotalCpuTimeDiff        = ""; // The total execution time value. (ms)
		String TotalElapsedTimeDiff    = ""; // The total elapsed time value. (ms)
		String TotalEstWaitTimeDiff    = ""; // TotalElapsedTime - TotalCpuTime
//		if (srvVersion >= Ver.ver(16,0))
		if (srvVersion >= Ver.ver(15,7,0, 130))
		{
			TotalPIO		     = "TotalPIO,         ";
			TotalLIO             = "TotalLIO,         ";
			TotalCpuTime         = "TotalCpuTime,     ";
			TotalElapsedTime     = "TotalElapsedTime, ";
			TotalEstWaitTime     = "TotalEstWaitTime = TotalElapsedTime - TotalCpuTime, \n";

			TotalPioDiff		 = "TotalPioDiff         = TotalPIO, ";         // DIFF COUNTER
			TotalLioDiff         = "TotalLioDiff         = TotalLIO, ";         // DIFF COUNTER
			TotalCpuTimeDiff     = "TotalCpuTimeDiff     = TotalCpuTime, ";     // DIFF COUNTER
			TotalElapsedTimeDiff = "TotalElapsedTimeDiff = TotalElapsedTime, "; // DIFF COUNTER
			TotalEstWaitTimeDiff = "TotalEstWaitTimeDiff = TotalElapsedTime - TotalCpuTime, \n";
		}

		
		// NOTE: The function 'show_plan(-1,SSQLID,-1,-1)'
		//       returns a Ase Message (MsgNum=0) within the ResultSet
		//       those messages are placed in the column named 'msgAsColValue'
		//       see CounterSample.readResultset() for more details
		String cols = 
			(isClusterEnabled ? " InstanceID, \n" : "") + //  The Server Instance Identifier (cluster only)
			" DBID, \n" +                       // The database ID from which the statement was cached.
			(srvVersion >= Ver.ver(15,0,2,4) ? " DBName, \n" : "DBName = db_name(DBID), \n") + //  Name of the database (will be NULL if the database is no longer open)
			(srvVersion >= Ver.ver(15,7) ? " StmtType, " : "") + // The type of the cached statement. (1=Batch, 2=Cursor, 3=Dynamic)
			" UserID, SUserID, SUserName = suser_name(SUserID), \n" +
			" ObjectName = object_name(SSQLID, 2), \n" +
			" SSQLID, \n" +                     // The unique identifier for a statement.
			" Hashkey, \n" +                    // The hashkey over the statement's text.
//			" HasShowplan   = CASE WHEN show_plan(-1,SSQLID,-1,-1) < 0 THEN convert(bit,0) ELSE convert(bit,1) END, \n" +
//			" HasSqltext    = convert(bit,1), \n" +
			(srvVersion >= Ver.ver(15,7) ? " OptimizationGoal, " : "") + // The optimization goal stored in the statement cache
			(srvVersion >= Ver.ver(15,7) ? " OptimizerLevel, \n" : "") + // The optimizer level stored in the statement cache
			"Remark = convert(varchar(60), ''), \n" + // Display some findings...
			sql_hasSqlText +
			sql_hasShowplan + 
			sql_hasXmlPlan +
			" UseCount, \n" +                   // The number of times this statement was used.
			" UseCountDiff = UseCount, \n" +    // The number of times this statement was used.
			LockWaits + LockWaitTime + LockWaitsDiff + LockWaitTimeDiff + nl_15702 +
			MaxSortTime + SortSpilledCount + SortCount + TotalSortTime + SortCountDiff + TotalSortTimeDiff + nl_15702 + 
			" MetricsCount, \n" +               // Number of executions over which query metrics were captured.
			" MaxElapsedTime, MinElapsedTime, AvgElapsedTime, "+ TotalElapsedTime + TotalElapsedTimeDiff + "\n" + // Elapsed time value.
			" MaxLIO,         MinLIO,         AvgLIO,         "+ TotalLIO         + TotalLioDiff         + "\n" + // Logical IO
			" MaxPIO,         MinPIO,         AvgPIO,         "+ TotalPIO         + TotalPioDiff         + "\n" + // Physical IO
			" MaxCpuTime,     MinCpuTime,     AvgCpuTime,     "+ TotalCpuTime     + TotalCpuTimeDiff     + "\n" + // Execution time.
			TotalEstWaitTime +
			TotalEstWaitTimeDiff +
			" LastUsedDate, \n" +               // Date when this statement was last used.
			" LastRecompiledDate, \n" +         // Date when this statement was last recompiled.
			" CachedDate, \n" +                 // Date when this statement was cached.
			" MinPlanSizeKB, \n" +              // The size of a plan associated with this statement when dormant.
			" MaxPlanSizeKB, \n" +              // The size of a plan associated with this statement when it is in use.
			" CurrentUsageCount, \n" +          // The number of concurrent uses of this statement.
			" MaxUsageCount, \n" +              // The maximum number of times for which this statement was simultaneously used.
			AvgScanRows + MaxScanRows + nl_15702 +
			AvgQualifyingReadRows     + nl_15702 +
			AvgReadEfficiancy         + nl_15702 +
			MaxQualifyingReadRows     + nl_15702 +
			AvgQualifyingWriteRows    + nl_15702 +
			AvgWriteEfficiancy        + nl_15702 +
			MaxQualifyingWriteRows    + nl_15702 + 
			" NumRecompilesSchemaChanges, \n" + // The number of times this statement was recompiled due to schema changes.
			" NumRecompilesPlanFlushes, \n" +   // The number of times this statement was recompiled because no usable plan was found.
			" HasAutoParams       = convert(bit,HasAutoParams), \n" + // Does this statement have any parameterized literals.
			" ParallelDegree, \n" +               // The parallel-degree session setting.
			ParallelDegreeReduced + ParallelPlanRanSerial + WorkerThreadDeficit + nl_15702 + 
			" QuotedIdentifier    = convert(bit,QuotedIdentifier), \n" + // The quoted identifier session setting.
			(srvVersion < Ver.ver(15,0,2,6) ? " TableCount, \n" : "") + // describeme
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

	/**
	 * Get the XML Plan for each row, but it's using the cache so that we don't have to issue so many show_cached_plan_in_xml()
	 */
	@Override
	public void localCalculation(CounterSample newSample)
	{
		Configuration conf = Configuration.getCombinedConfiguration();
		boolean sampleXmlPlan         = (conf == null) ? DEFAULT_sample_xmlPlan        : conf.getBooleanProperty(PROPKEY_sample_xmlPlan,        DEFAULT_sample_xmlPlan);
		int     xmlPlan_bulkThreshold = (conf == null) ? DEFAULT_xmlPlan_bulkThreshold : conf.getIntProperty    (PROPKEY_xmlPlan_bulkThreshold, DEFAULT_xmlPlan_bulkThreshold);

		if ( ! sampleXmlPlan )
			return;

		if (getServerVersion() < Ver.ver(15,7))
			return;

		CounterSample counters = newSample;
		if (counters == null)
			return;

		// Where are various columns located in the Vector 
		int pos_xmlPlan    = -1;
		int pos_HasXmlPlan = -1;
		int pos_ObjectName = -1;

		// Find column Id's
		List<String> colNames = counters.getColNames();
		if (colNames==null) 
			return;

		for (int colId=0; colId < colNames.size(); colId++) 
		{
			String colName = colNames.get(colId);
			if (colName.equals("xmlPlan")   ) pos_xmlPlan    = colId;
			if (colName.equals("HasXmlPlan")) pos_HasXmlPlan = colId;
			if (colName.equals("ObjectName")) pos_ObjectName = colId;

			// No need to continue, we got all our columns
			if ( pos_xmlPlan >= 0 && pos_HasXmlPlan >= 0 && pos_ObjectName >= 0)
				break;
		}

		if (pos_xmlPlan < 0 || pos_HasXmlPlan < 0 || pos_ObjectName < 0)
		{
			_logger.debug("Can't find the position for columns (xmlPlan="+pos_xmlPlan+", HasXmlPlan="+pos_HasXmlPlan+", ObjectName="+pos_ObjectName+")");
			return;
		}

		// Get the XmlPlanCache
		XmlPlanCache xmlPlanCache = XmlPlanCache.getInstance();
		
		// Count how many records we need to do Physical Reads for
		int cacheMissCount = 0;
		for (int rowId=0; rowId < counters.getRowCount(); rowId++) 
		{
			Object o_ObjectName = counters.getValueAt(rowId, pos_ObjectName);

			if (o_ObjectName instanceof String)
			{
				String ObjectName = (String)o_ObjectName;
				if ( ! xmlPlanCache.isPlanCached(ObjectName) )
					cacheMissCount++;
			}
		}
		
		// If number of misses is more that X number (default DEFAULT_xmlPlan_bulkThreshold = 200)
		// Then it's probably more efficient to get all records in a more efficient way and store the records in the cache 
		// Next loop will the read everything cached (so this is just a Efficient PreFetch of data) 
		if (cacheMissCount > xmlPlan_bulkThreshold)
		{
			if (isServerRoleOrPermissionActive(AseConnectionUtils.MON_ROLE))
				xmlPlanCache.getPlanBulk(null); // null means everything
		}

		// Loop on all diffData rows and update 
		for (int rowId=0; rowId < counters.getRowCount(); rowId++) 
		{
			Object o_ObjectName = counters.getValueAt(rowId, pos_ObjectName);

			if (o_ObjectName instanceof String)
			{
				String ObjectName = (String)o_ObjectName;

				String cachedPlanInXml = "User does not have: mon_role";

				if (isServerRoleOrPermissionActive(AseConnectionUtils.MON_ROLE))
				{
					if (sampleXmlPlan)
//						cachedPlanInXml = AseConnectionUtils.cachedPlanInXml(getCounterController().getMonConnection(), procname, false);
						cachedPlanInXml = xmlPlanCache.getPlan(ObjectName);
					else
						cachedPlanInXml = "This was disabled";
					if (cachedPlanInXml == null)
						cachedPlanInXml = "Not Available";
				}

				boolean b = true;
				b = !"This was disabled".equals(cachedPlanInXml) && !"Not Available".equals(cachedPlanInXml) && !cachedPlanInXml.startsWith("User does not have");
				counters.setValueAt(cachedPlanInXml, rowId, pos_xmlPlan);
				counters.setValueAt(new Boolean(b),  rowId, pos_HasXmlPlan);
			}
		}
	}

	/** Used by the: Create 'Offline Session' Wizard */
	@Override
	public List<CmSettingsHelper> getLocalSettings()
	{
		Configuration conf = Configuration.getCombinedConfiguration();
		List<CmSettingsHelper> list = new ArrayList<>();
		
		list.add(new CmSettingsHelper("Get SQL Text",           PROPKEY_sample_sqlText               , Boolean.class, conf.getBooleanProperty(PROPKEY_sample_sqlText               , DEFAULT_sample_sqlText               ), DEFAULT_sample_sqlText              , CmStmntCacheDetailsPanel.TOOLTIP_sample_sqlText                ));
		list.add(new CmSettingsHelper("Get Showplan",           PROPKEY_sample_showplan              , Boolean.class, conf.getBooleanProperty(PROPKEY_sample_showplan              , DEFAULT_sample_showplan              ), DEFAULT_sample_showplan             , CmStmntCacheDetailsPanel.TOOLTIP_sample_showplan               ));
		list.add(new CmSettingsHelper("Get XML Plan",           PROPKEY_sample_xmlPlan               , Boolean.class, conf.getBooleanProperty(PROPKEY_sample_xmlPlan               , DEFAULT_sample_xmlPlan               ), DEFAULT_sample_xmlPlan              , CmStmntCacheDetailsPanel.TOOLTIP_sample_xmlPlan                ));
		list.add(new CmSettingsHelper("XML Level of Details",   PROPKEY_sample_xmlPlan_levelOfDetail , Integer.class, conf.getIntProperty    (PROPKEY_sample_xmlPlan_levelOfDetail , DEFAULT_sample_xmlPlan_levelOfDetail ), DEFAULT_sample_xmlPlan_levelOfDetail, CmStmntCacheDetailsPanel.TOOLTIP_sample_xmlPlan_levelOfDetail  ));
		list.add(new CmSettingsHelper("Where MetricsCount > 0", PROPKEY_sample_metricsCountGtZero    , Boolean.class, conf.getBooleanProperty(PROPKEY_sample_metricsCountGtZero    , DEFAULT_sample_metricsCountGtZero    ), DEFAULT_sample_metricsCountGtZero   , CmStmntCacheDetailsPanel.TOOLTIP_sample_metricsCountGtZero     ));

		return list;
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
				{
//					return toHtmlString((String) cellVal, false);
					AsePlanViewer.getInstance().loadXmlDeferred((String) cellVal);
					return null;
				}
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

//	/**
//	 * Calculate some stuff 'Remarks' for the ABSOLUTE values
//	 */
//	@Override
//	public void localCalculation(CounterSample prevSample, CounterSample newSample)
//	{
//	}
	/**
	 * Calculate some stuff 'Remarks' for both the ABSOLUTE and the DELTA values
	 */
	@Override
	public void localCalculation(CounterSample prevSample, CounterSample newSample, CounterSample diffData)
	{
		// set some "Remark", below is columns user to draw some conclutions 
		int Remark_pos       = -1;
		int UseCount_pos     = -1;
		int MinLIO_pos       = -1;
		int MaxLIO_pos       = -1;
		
		
		// Find column Id's
		List<String> colNames = diffData.getColNames();
		if (colNames == null)
			return;

		for (int colId=0; colId < colNames.size(); colId++) 
		{
			String colName = colNames.get(colId);
			if      (colName.equals("Remark"))       Remark_pos       = colId;
			else if (colName.equals("UseCount"))     UseCount_pos     = colId;
			else if (colName.equals("MinLIO"))       MinLIO_pos       = colId;
			else if (colName.equals("MaxLIO"))       MaxLIO_pos       = colId;
		}

		// Loop on all diffData rows
		for (int rowId = 0; rowId < diffData.getRowCount(); rowId++)
		{
//			int absUseCount  = ((Number)newSample.getValueAt(rowId, UseCount_pos)).intValue();
//			int diffUseCount = ((Number)diffData .getValueAt(rowId, UseCount_pos)).intValue();
//			if (absUseCount <= 10)
//				continue;
//			if (diffUseCount == 0)
//				continue;
//
//			// Check MaxLIO & MinLIO
//			// If the gap are to big, then we probably have a "faulty plan" due to: data screw...
//			// Meaning: one is using a cheap plan, the other one is using an expensive, probably does to many lookups at DataPages from the NotClustered index scan leaf pages
//			int diffMinLIO = ((Number)diffData.getValueAt(rowId, MinLIO_pos)).intValue();
//			int diffMaxLIO = ((Number)diffData.getValueAt(rowId, MaxLIO_pos)).intValue();
//
//			int absMinLIO = ((Number)newSample.getValueAt(rowId, MinLIO_pos)).intValue();
//			int absMaxLIO = ((Number)newSample.getValueAt(rowId, MaxLIO_pos)).intValue();
			
//			fixme
//			also do "same" for CachedProcedures..absMaxLIO.
			//------------------------------
			// CALC: Remark
			// A LOT of stuff can be included here...
			if (Remark_pos >= 0 && UseCount_pos >= 0 && MinLIO_pos >= 0 && MaxLIO_pos >= 0)
			{
//				int UsedCount    = ((Number)diffData.getValueAt(rowId, UsedCount_pos   )).intValue();
//				int IndexID      = ((Number)diffData.getValueAt(rowId, IndexID_pos     )).intValue();
//				int LogicalReads = ((Number)diffData.getValueAt(rowId, LogicalReads_pos)).intValue();
//				int RowsInserted = ((Number)diffData.getValueAt(rowId, RowsInserted_pos)).intValue();
//				int PagesRead    = ((Number)diffData.getValueAt(rowId, PagesRead_pos   )).intValue();
				
				int absUseCount  = ((Number)newSample.getValueAt(rowId, UseCount_pos)).intValue();
				int diffUseCount = ((Number)diffData .getValueAt(rowId, UseCount_pos)).intValue();
				if (absUseCount <= 10)
					continue;
				if (diffUseCount == 0)
					continue;

//				// Check MaxLIO & MinLIO
//				// If the gap are to big, then we probably have a "faulty plan" due to: data screw...
//				// Meaning: one is using a cheap plan, the other one is using an expensive, probably does to many lookups at DataPages from the NotClustered index scan leaf pages
				int diffMinLIO = ((Number)diffData.getValueAt(rowId, MinLIO_pos)).intValue();
				int diffMaxLIO = ((Number)diffData.getValueAt(rowId, MaxLIO_pos)).intValue();
	
				int absMinLIO = ((Number)newSample.getValueAt(rowId, MinLIO_pos)).intValue();
				int absMaxLIO = ((Number)newSample.getValueAt(rowId, MaxLIO_pos)).intValue();

				String diffRemark = null;
				String absRemark  = null;
	
				double calcVal =  ((absMinLIO*1.0) / (absMaxLIO*1.0)) * 100.0;
//System.out.println("abs: calcVal="+calcVal+": absMinLIO="+absMinLIO+", absMaxLIO="+absMaxLIO+".");
				if ( calcVal < 10.0 )
				{
					absRemark = RemarkDictionary.SKEWED_EXEC_PLAN_ABS;
				}

				calcVal =  ((diffMinLIO*1.0) / (diffMaxLIO*1.0)) * 100.0;
//System.out.println("diff: calcVal="+calcVal+": diffMinLIO="+diffMinLIO+", diffMaxLIO="+diffMaxLIO+".");
				if ( calcVal < 10.0 )
				{
					diffRemark = RemarkDictionary.SKEWED_EXEC_PLAN_DIFF;
				}

//				if (IndexID == 0 && UsedCount > 0 && LogicalReads > 0)
//				{
////					remark = RemarkDictionary.T_SCAN_OR_HTAB_INS;
//					remark = RemarkDictionary.TABLE_SCAN;
//
//					// Allow up to 10% variance of inserts and still consider it to be a "Table Scan"
//					// But if it's more than that 10% inserts, then consider it as a "Heap Table Insert"
//					// pctNear is just: 10% more or 10% less than the baseValue(UsedCount)
//					if ( MathUtils.pctNear(10, UsedCount, RowsInserted) )
//						remark = RemarkDictionary.HEAP_TAB_INS;
//				}
				
				// set ABS - Remark
				if ( ! StringUtil.isNullOrBlank(absRemark) )
					newSample.setValueAt(absRemark, rowId, Remark_pos);

				// set DIFF - Remark
				if ( ! StringUtil.isNullOrBlank(diffRemark) )
					diffData.setValueAt(diffRemark, rowId, Remark_pos);
			}

		}
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
		String[] sa = {"UseCount", "UseCountDiff", "AvgLIO", "AvgElapsedTime", "TotalEstWaitTime"};
		return sa;
	}
}


//-------------------------------------------------------------------------------
//SAP - Documentation
//https://help.sap.com/viewer/ad4a1ddf1bf34768841bd09d1eddf434/16.0.4.1/en-US/ab9cd3c8bc2b1014b077e45dbad89abe.html?q=monCachedStatement
//-------------------------------------------------------------------------------
//  Names                          Datatypes     Description
//  ------------------------------ ------------- --------------------------------------------------
//  InstanceID                     tinyint       (Cluster environments only) ID of an instance in a shared-disk cluster.
//  SSQLID                         int           Unique identifier for each cached statement. This value is treated as a primary key for monCachedStatement, and is used in functions. show_cached_text uses SSQLID to refer to individual statements in the cache.
//  Hashkey                        int           Hash value of the SQL text of the cached statement. A hash key is generated based on a statement�s text, and can be used as an approximate key for searching other monitoring tables.
//  StmtType                       tinyint       
//  UserID                         int           User ID of the user who initiated the statement that has been cached.
//  SUserID                        int           Server ID of the user who initiated the cached statement.
//  DBID                           smallint      Database ID of the database from which the statement was cached.
//  UseCount                       int           Number of times the statement was accessed after it was cached.
//  StatementSize                  int           Size of the cached statement, in bytes.
//  MinPlanSizeKB                  int           Size of the plan when it is not in use, in kilobytes.
//  MaxPlanSizeKB                  int           Size of the plan when it is in use, in kilobytes.
//  CurrentUsageCount              int           Number of concurrent users of the cached statement. Attribute is counter.
//  MaxUsageCount                  int           Maximum number of times the cached statement�s text was simultaneously accessed. Attribute is counter.
//  NumRecompilesSchemaChanges     int           Number of times the statement was recompiled due to schema changes. Running update statistics on a table may result in changes to the best plan. This change is treated as a minor schema change. Recompiling a statement many times indicates that it is not effective to cache this particular statement, and that you may want to delete the statement from the statement cache to make space for some other, more stable, statement. Attribute is counter.
//  NumRecompilesPlanFlushes       int           Number of times the cached statement was recompiled because a plan was not found in the cache. Attribute is counter.
//  HasAutoParams                  tinyint       �true� if the statement has any parameterized literals, �false� if it does not.
//  ParallelDegree                 tinyint       Degree of parallelism used by the query that is stored for this statement
//  QuotedIdentifier               tinyint       Specifies whether the plan compiled with set quoted_identifier is enabled.
//  TransactionIsolationLevel      tinyint       Transaction isolation level for which the statement was compiled.
//  TransactionMode                tinyint       Specifies whether �chained transaction mode� is enabled for the statement.
//  SAAuthorization                tinyint       Specifies whether the plan was compiled with sa_role authorization.
//  SystemCatalogUpdate            tinyint       Specifies whether allow catalog updates was enabled when the plan was compiled.
//  MetricsCount                   int           Number of times metrics were aggregated for this statement.
//  MinPIO                         int           Maximum physical I/Os that occurred during any execution of this statement.
//  MaxPIO                         int           Maximum physical I/Os that occurred during any execution of this statement.
//  AvgPIO                         int           Average number of physical I/Os that occurred during execution of this statement.
//  MinLIO                         int           Minimum logical I/Os that occurred during any execution of this statement.
//  MaxLIO                         int           Maximum logical I/Os that occurred during any one execution of this statement.
//  AvgLIO                         int           Average number of logical I/Os that occurred during execution of this statement.
//  MinCpuTime                     int           The minimum amount of CPU time, in milliseconds, consumed by any execution of this statement.
//  MaxCpuTime                     int           The maximum amount of CPU time, in milliseconds, consumed by any execution of this statement.
//  AvgCpuTime                     int           The average amount of CPU time, in milliseconds, consumed by this statement.
//  MinElapsedTime                 int           Minimum elapsed execution time for this statement.
//  MaxElapsedTime                 int           Maximum elapsed execution time for this statement.
//  AvgElapsedTime                 int           Average elapsed execution time for this statement.
//  AvgScanRows                    int           Average number of scanned rows read per execution
//  MaxScanRows                    int           Maximum number of scanned rows read per execution
//  AvgQualifyingReadRows          int           Average number of qualifying data rows per read command execution
//  MaxQualifyingReadRows          int           Maximum number of qualifying data rows per query execution
//  AvgQualifyingWriteRows         int           Average number of qualifying data rows per query execution
//  MaxQualifyingWriteRows         int           Maximum number of qualifying data rows per query execution
//  LockWaits                      int           Total number of lock waits
//  LockWaitTime                   int           Total amount of time, in milliseconds, spent waiting for locks
//  SortCount                      int           Total number of sort operations
//  SortSpilledCount               int           Total number of sort operations spilled to disk
//  TotalSortTime                  int           Total amount of time, in milliseconds, spent in sorts
//  MaxSortTime                    int           Maximum amount of time, in milliseconds, spent in a sort
//  DBName                         varchar(30)   Name of database from which the statement was cached. Attribute is null.
//  CachedDate                     datetime      Timestamp of the date and time when the statement was first cached.
//  LastUsedDate                   datetime      Timestamp of the date and time when the cached statement was last used. Use this information with CachedDate to determine how frequently this statement is used, and whether it is helpful to have it cached.
//  LastRecompiledDate             datetime      Date when the statement was last recompiled, because of schema changes or because the statement was not found in the statement cache.
//  OptimizationGoal               varchar(30)   The optimization goal used to optimize this statement.
//  OptimizerLevel                 varchar(30)   The optimizer level used to optimize this statement.
//  ParallelDegreeReduced          int           Indicates if an insufficient number of worker threads were available to execute the query with the full degree of parallelism the query plan calls for, but the query did execute with some parallelism.
//  ParallelPlanRanSerial          int           Indicates if an insufficient number of worker threads were available to execute the query in parallel so the query was executed serially.
//  WorkerThreadDeficit            int           Indicates that the cumulative total number of worker threads were unavailable to execute this query since it was added to the statement cache.
//  TotalLIO                       bigint        Cumulative logical I/O
//  TotalPIO                       bigint        Cumulative physical I/O
//  TotalCpuTime                   bigint        Cumulative elapsed time, in seconds, this statement spent using CPU
//  TotalElapsedTime               bigint        Cumulative amount of time, in seconds spent executing this statement
//-------------------------------------------------------------------------------
