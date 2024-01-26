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

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

import javax.naming.NameNotFoundException;

import com.asetune.ICounterController;
import com.asetune.IGuiController;
import com.asetune.cm.CmSettingsHelper;
import com.asetune.cm.CounterSample;
import com.asetune.cm.CounterSetTemplates;
import com.asetune.cm.CounterSetTemplates.Type;
import com.asetune.cm.CountersModel;
import com.asetune.cm.ase.gui.CmCachedProcsSumPanel;
import com.asetune.config.dict.MonTablesDictionary;
import com.asetune.config.dict.MonTablesDictionaryManager;
import com.asetune.gui.MainFrame;
import com.asetune.gui.TabularCntrPanel;
import com.asetune.sql.conn.DbxConnection;
import com.asetune.sql.conn.info.DbmsVersionInfo;
import com.asetune.utils.Configuration;
import com.asetune.utils.StringUtil;
import com.asetune.utils.Ver;

/**
 * @author Goran Schwarz (goran_schwarz@hotmail.com)
 */
public class CmCachedProcsSum
extends CountersModel
{
//	private static Logger        _logger          = Logger.getLogger(CmCachedProcsSum.class);
	private static final long    serialVersionUID = 1L;

	public static final String   CM_NAME          = CmCachedProcsSum.class.getSimpleName();
	public static final String   SHORT_NAME       = "Cached Procedures Sum";
	public static final String   HTML_DESC        = 
		"<html>" +
		"What Objects is located in the 'procedure cache'.<br>" +
		"Same as 'Cached Procedures' but only <b>one</b> row is displayed per procedure, hence the SUM.<br>" +
		"From ASE 15.7 you will also have execution times etc.<br>" +
		"</html>";

	public static final String   GROUP_NAME       = MainFrame.TCP_GROUP_CACHE;
	public static final String   GUI_ICON_FILE    = "images/"+CM_NAME+".png";

//	public static final long     NEED_SRV_VERSION = 1550000;
	public static final long     NEED_SRV_VERSION = Ver.ver(15,5);
	public static final long     NEED_CE_VERSION  = 0;

	public static final String[] MON_TABLES       = new String[] {"monCachedProcedures"};
	public static final String[] NEED_ROLES       = new String[] {"mon_role"};
	public static final String[] NEED_CONFIG      = new String[] {"enable monitoring=1", "per object statistics active=1", "statement statistics active=1"};

	public static final String[] PCT_COLUMNS      = new String[] {};
	public static final String[] DIFF_COLUMNS     = new String[] {
		"SumRequestCntDiff", 
		"SumTempdbRemapCnt", 
		"SumExecutionCount", 
		"SumCPUTime", 
		"SumExecutionTime", 
		"SumPhysicalReads", 
		"SumLogicalReads", 
		"SumPhysicalWrites", 
		"SumPagesWritten",

		"SumSnapCodegenTime",    // 16.0 SP2
		"SumSnapJITTime",        // 16.0 SP2
		"SumSnapExecutionTime",  // 16.0 SP2
		"SumSnapExecutionCount"  // 16.0 SP2
	};

	public static final boolean  NEGATIVE_DIFF_COUNTERS_TO_ZERO = true;
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

		return new CmCachedProcsSum(counterController, guiController);
	}

	public CmCachedProcsSum(ICounterController counterController, IGuiController guiController)
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
	private static final String  PROP_PREFIX                        = CM_NAME;

	public static final String  PROPKEY_sample_statementCacheObjects = PROP_PREFIX + ".sample.statementCacheObjects";
	public static final boolean DEFAULT_sample_statementCacheObjects = false;

	public static final String  PROPKEY_sample_dynamicSqlObjects     = PROP_PREFIX + ".sample.dynamicSqlObjects";
	public static final boolean DEFAULT_sample_dynamicSqlObjects     = false;

	private void addTrendGraphs()
	{
	}

	@Override
	protected TabularCntrPanel createGui()
	{
		// YES THIS SHOULD BE CmCachedProcsPanel and not CmCachedProcsSumPanel, it's only Icon renders for the JXTable
		return new CmCachedProcsSumPanel(this);
	}

	@Override
	public String[] getDependsOnConfigForVersion(DbxConnection conn, DbmsVersionInfo versionInfo)
	{
		long srvVersion = versionInfo.getLongVersion();

		if (srvVersion >= Ver.ver(15,7))
			return NEED_CONFIG;

		return new String[] {"per object statistics active=1"};
	}

	/** Used by the: Create 'Offline Session' Wizard */
	@Override
	public List<CmSettingsHelper> getLocalSettings()
	{
		Configuration conf = Configuration.getCombinedConfiguration();
		List<CmSettingsHelper> list = new ArrayList<>();
		
		list.add(new CmSettingsHelper("Sample Lightweight Procs from Statement Cache",        PROPKEY_sample_statementCacheObjects, Boolean.class, conf.getBooleanProperty(PROPKEY_sample_statementCacheObjects, DEFAULT_sample_statementCacheObjects), DEFAULT_sample_statementCacheObjects, CmCachedProcsSumPanel.TOOLTIP_sample_statementCacheObjects));
		list.add(new CmSettingsHelper("Sample Lightweight Procs from Dynamic SQL Statements", PROPKEY_sample_dynamicSqlObjects    , Boolean.class, conf.getBooleanProperty(PROPKEY_sample_dynamicSqlObjects    , DEFAULT_sample_dynamicSqlObjects    ), DEFAULT_sample_dynamicSqlObjects    , CmCachedProcsSumPanel.TOOLTIP_sample_dynamicSqlObjects));

		return list;
	}

	@Override
	public void addMonTableDictForVersion(DbxConnection conn, DbmsVersionInfo versionInfo)
	{
		try 
		{
			MonTablesDictionary mtd = MonTablesDictionaryManager.getInstance();
			mtd.addColumn("monCachedProcedures",  "SumActive",             "<html>Number of instances that has the column Active = 'Yes'          <br><b>Formula</b>: sum(CASE WHEN lower(Active) = 'yes' THEN 1 ELSE 0 END) <br> </html>");
			mtd.addColumn("monCachedProcedures",  "NumberOfPlanesInCache", "<html>Number of instances of this procedure in the Procedure Cache    <br><b>Formula</b>: count(*) group by DBName, ObjectName, ObjectType <br> </html>");
			mtd.addColumn("monCachedProcedures",  "SumMemUsageKB",         "<html>SUM MemUsageKB                                                  <br><b>Formula</b>: sum(MemUsageKB)                       <br></html>");
			mtd.addColumn("monCachedProcedures",  "MaxCompileDate",        "<html>MAX CompileDate                                                 <br><b>Formula</b>: max(CompileDate)                      <br></html>");
			mtd.addColumn("monCachedProcedures",  "MinCompileAgeInSec",    "<html>How many seconds where this plan compiled                       <br><b>Formula</b>: datediff(ss, max(CompileDate), getdate())<br></html>");
			mtd.addColumn("monCachedProcedures",  "SumRequestCnt",         "<html>SUM RequestCnt                                                  <br><b>Formula</b>: sum(RequestCnt)                       <br></html>");
			mtd.addColumn("monCachedProcedures",  "SumRequestCntDiff",     "<html>Difference between in SumRequestCnt from prev sample            <br><b>Formula</b>: SumRequestCnt                         <br></html>");
			mtd.addColumn("monCachedProcedures",  "SumTempdbRemapCnt",     "<html>SUM TempdbRemapCnt                                              <br><b>Formula</b>: sum(TempdbRemapCnt)                   <br></html>");
			mtd.addColumn("monCachedProcedures",  "MaxAvgTempdbRemapTime", "<html>MAX AvgTempdbRemapTime                                          <br><b>Formula</b>: max(AvgTempdbRemapTime)               <br></html>");
			mtd.addColumn("monCachedProcedures",  "SumCPUTime",            "<html>SUM CPU Time                                                    <br><b>Formula</b>: sum(CPUTime)                          <br></html>");
			mtd.addColumn("monCachedProcedures",  "SumExecutionTime",      "<html>SUM Execution Time                                              <br><b>Formula</b>: sum(ExecutionTime)                    <br></html>");
			mtd.addColumn("monCachedProcedures",  "SumPhysicalReads",      "<html>SUM Physical Reads                                              <br><b>Formula</b>: sum(PhysicalReads)                    <br></html>");
			mtd.addColumn("monCachedProcedures",  "SumLogicalReads",       "<html>SUM Logical Reads                                               <br><b>Formula</b>: sum(LogicalReads)                     <br></html>");
			mtd.addColumn("monCachedProcedures",  "SumPhysicalWrites",     "<html>SUM Physical Writes                                             <br><b>Formula</b>: sum(PhysicalWrites)                   <br></html>");
			mtd.addColumn("monCachedProcedures",  "SumPagesWritten",       "<html>SUM Pages Written                                               <br><b>Formula</b>: sum(PagesWritten)                     <br></html>");
			mtd.addColumn("monCachedProcedures",  "AvgCPUTime",            "<html>CPU Time per execution count                                    <br><b>Formula</b>: SumCPUTime        / SumExecutionCount <br></html>");
			mtd.addColumn("monCachedProcedures",  "AvgExecutionTime",      "<html>Execution Time per execution count                              <br><b>Formula</b>: SumExecutionTime  / SumExecutionCount <br></html>");
			mtd.addColumn("monCachedProcedures",  "AvgPhysicalReads",      "<html>Physical Reads per execution count                              <br><b>Formula</b>: SumPhysicalReads  / SumExecutionCount <br></html>");
			mtd.addColumn("monCachedProcedures",  "AvgLogicalReads",       "<html>Logical Reads per execution count                               <br><b>Formula</b>: SumLogicalReads   / SumExecutionCount <br></html>");
			mtd.addColumn("monCachedProcedures",  "AvgPhysicalWrites",     "<html>Physical Writes per execution count                             <br><b>Formula</b>: SumPhysicalWrites / SumExecutionCount <br></html>");
			mtd.addColumn("monCachedProcedures",  "AvgPagesWritten",       "<html>Pages Written per execution count                               <br><b>Formula</b>: SumPagesWritten   / SumExecutionCount <br></html>");

			mtd.addColumn("monCachedProcedures",  "SumSnapExecutionCount", "<html>SUM Snap Execution Count                            (SNAP = Simplfied Native Access Plans) <br><b>Formula</b>: sum(SnapExecutionCount)               <br></html>");
			mtd.addColumn("monCachedProcedures",  "SumSnapCodegenTime",    "<html>SUM SNAP Code Generation time (microseconds)        (SNAP = Simplfied Native Access Plans) <br><b>Formula</b>: sum(SnapCodegenTime)                  <br></html>");
			mtd.addColumn("monCachedProcedures",  "SumSnapJITTime",        "<html>SUM SNAP Just In Time Compilation (microseconds)    (SNAP = Simplfied Native Access Plans) <br><b>Formula</b>: sum(SnapJITTime)                      <br></html>");
			mtd.addColumn("monCachedProcedures",  "SumSnapExecutionTime",  "<html>SUM SNAP Execution Time                             (SNAP = Simplfied Native Access Plans) <br><b>Formula</b>: sum(SnapExecutionTime)                <br></html>");
			mtd.addColumn("monCachedProcedures",  "AvgSnapCodegenTime",    "<html>AVG SNAP Code Generation time (microseconds)        (SNAP = Simplfied Native Access Plans) <br><b>Formula</b>: SumSnapCodegenTime   / SumSnapExecutionCount <br></html>");
			mtd.addColumn("monCachedProcedures",  "AvgSnapJITTime",        "<html>AVG SNAP Just In Time Compilation (microseconds)    (SNAP = Simplfied Native Access Plans) <br><b>Formula</b>: SumSnapJITTime       / SumSnapExecutionCount <br></html>");
			mtd.addColumn("monCachedProcedures",  "AvgSnapExecutionTime",  "<html>AVG SNAP Execution Time                             (SNAP = Simplfied Native Access Plans) <br><b>Formula</b>: SumSnapExecutionTime / SumSnapExecutionCount <br></html>");
		}
		catch (NameNotFoundException e) 
		{
		//	_logger.warn("Problems in cm='" + CM_NAME + "', adding addMonTableDictForVersion. Caught: " + e); 
			System.out.println("Problems in cm='" + CM_NAME + "', adding addMonTableDictForVersion. Caught: " + e); 
		}
	}

	@Override
	public List<String> getPkForVersion(DbxConnection conn, DbmsVersionInfo versionInfo)
	{
		List <String> pkCols = new LinkedList<String>();

//		if (isClusterEnabled)
//			pkCols.add("InstanceID");

		pkCols.add("DBName");
		pkCols.add("ObjectName");
		pkCols.add("ObjectType");

		return pkCols;
	}

	@Override
	public String getSqlForVersion(DbxConnection conn, DbmsVersionInfo versionInfo)
	{
		long srvVersion = versionInfo.getLongVersion();

		Configuration conf = Configuration.getCombinedConfiguration();
		boolean sample_statementCacheObjects = conf.getBooleanProperty(PROPKEY_sample_statementCacheObjects, DEFAULT_sample_statementCacheObjects);
		boolean sample_dynamicSqlObjects     = conf.getBooleanProperty(PROPKEY_sample_dynamicSqlObjects,     DEFAULT_sample_dynamicSqlObjects);

		String cols = "";
		int    orderByColumnNumber = 8; // order by column 'SumRequestCnt'

		// ASE 15.7
		String SumExecutionCount = "";
		String SumCPUTime        = "";
		String SumExecutionTime  = "";
		String SumPhysicalReads  = "";
		String SumLogicalReads   = "";
		String SumPhysicalWrites = "";
		String SumPagesWritten   = "";
		String AvgCPUTime        = ""; // xxx / ExecutionCount
		String AvgExecutionTime  = ""; // xxx / ExecutionCount
		String AvgPhysicalReads  = ""; // xxx / ExecutionCount
		String AvgLogicalReads   = ""; // xxx / ExecutionCount
		String AvgPhysicalWrites = ""; // xxx / ExecutionCount
		String AvgPagesWritten   = ""; // xxx / ExecutionCount

//		if (srvVersion >= 1570000)
		if (srvVersion >= Ver.ver(15,7))
		{
			SumExecutionCount = "  SumExecutionCount     = sum(convert(bigint,ExecutionCount)), \n";
			SumCPUTime        = "  SumCPUTime            = sum(convert(bigint,CPUTime)), \n";
			SumExecutionTime  = "  SumExecutionTime      = sum(convert(bigint,ExecutionTime)), \n";
			SumPhysicalReads  = "  SumPhysicalReads      = sum(convert(bigint,PhysicalReads)), \n";
			SumLogicalReads   = "  SumLogicalReads       = sum(convert(bigint,LogicalReads)), \n";
			SumPhysicalWrites = "  SumPhysicalWrites     = sum(convert(bigint,PhysicalWrites)), \n";
			SumPagesWritten   = "  SumPagesWritten       = sum(convert(bigint,PagesWritten)), \n";

			AvgCPUTime        = "  AvgCPUTime            = CASE WHEN sum(convert(bigint,ExecutionCount)) > 0 THEN convert(numeric(16,1), (sum(convert(bigint,CPUTime       )) + 0.0) / (sum(convert(bigint,ExecutionCount)) + 0.0)) ELSE  convert(numeric(16,1), null) END, \n";
			AvgExecutionTime  = "  AvgExecutionTime      = CASE WHEN sum(convert(bigint,ExecutionCount)) > 0 THEN convert(numeric(16,1), (sum(convert(bigint,ExecutionTime )) + 0.0) / (sum(convert(bigint,ExecutionCount)) + 0.0)) ELSE  convert(numeric(16,1), null) END, \n";
			AvgPhysicalReads  = "  AvgPhysicalReads      = CASE WHEN sum(convert(bigint,ExecutionCount)) > 0 THEN convert(numeric(16,1), (sum(convert(bigint,PhysicalReads )) + 0.0) / (sum(convert(bigint,ExecutionCount)) + 0.0)) ELSE  convert(numeric(16,1), null) END, \n";
			AvgLogicalReads   = "  AvgLogicalReads       = CASE WHEN sum(convert(bigint,ExecutionCount)) > 0 THEN convert(numeric(16,1), (sum(convert(bigint,LogicalReads  )) + 0.0) / (sum(convert(bigint,ExecutionCount)) + 0.0)) ELSE  convert(numeric(16,1), null) END, \n";
			AvgPhysicalWrites = "  AvgPhysicalWrites     = CASE WHEN sum(convert(bigint,ExecutionCount)) > 0 THEN convert(numeric(16,1), (sum(convert(bigint,PhysicalWrites)) + 0.0) / (sum(convert(bigint,ExecutionCount)) + 0.0)) ELSE  convert(numeric(16,1), null) END, \n";
			AvgPagesWritten   = "  AvgPagesWritten       = CASE WHEN sum(convert(bigint,ExecutionCount)) > 0 THEN convert(numeric(16,1), (sum(convert(bigint,PagesWritten  )) + 0.0) / (sum(convert(bigint,ExecutionCount)) + 0.0)) ELSE  convert(numeric(16,1), null) END, \n";
		}
		
		// ASE 16.0
		String SumActive      = ""; // Indicates whether the plan for this procedure is active or not
		String ase1600_nl     = "";

		if (srvVersion >= Ver.ver(16,0))
		{
			SumActive      = "  SumActive = sum(CASE WHEN lower(Active) = 'yes' THEN 1 ELSE 0 END), ";
			ase1600_nl     = "\n";

			orderByColumnNumber++;
		}

		// ASE 16.0 SP2
		String SumSnapCodegenTime    = "";
		String SumSnapJITTime        = "";
		String SumSnapExecutionTime  = "";
		String SumSnapExecutionCount = "";
		String AvgSnapCodegenTime    = ""; // xxx / SnapExecutionCount
		String AvgSnapJITTime        = ""; // xxx / SnapExecutionCount
		String AvgSnapExecutionTime  = ""; // xxx / SnapExecutionCount

		if (srvVersion >= Ver.ver(16,0,0, 2))
		{
			SumSnapCodegenTime    = "  SumSnapCodegenTime    = sum(convert(bigint,SnapCodegenTime   )), \n";
			SumSnapJITTime        = "  SumSnapJITTime        = sum(convert(bigint,SnapJITTime       )), \n";
			SumSnapExecutionTime  = "  SumSnapExecutionTime  = sum(convert(bigint,SnapExecutionTime )), \n";
			SumSnapExecutionCount = "  SumSnapExecutionCount = sum(convert(bigint,SnapExecutionCount)), \n";

			AvgSnapCodegenTime    = "  AvgSnapCodegenTime    = CASE WHEN sum(convert(bigint,SnapExecutionCount)) > 0 THEN convert(numeric(16,1), (sum(convert(bigint,SnapCodegenTime  )) + 0.0) / (sum(convert(bigint,SnapExecutionCount)) + 0.0)) ELSE  convert(numeric(16,1), null) END, \n";
			AvgSnapJITTime        = "  AvgSnapJITTime        = CASE WHEN sum(convert(bigint,SnapExecutionCount)) > 0 THEN convert(numeric(16,1), (sum(convert(bigint,SnapJITTime      )) + 0.0) / (sum(convert(bigint,SnapExecutionCount)) + 0.0)) ELSE  convert(numeric(16,1), null) END, \n";
			AvgSnapExecutionTime  = "  AvgSnapExecutionTime  = CASE WHEN sum(convert(bigint,SnapExecutionCount)) > 0 THEN convert(numeric(16,1), (sum(convert(bigint,SnapExecutionTime)) + 0.0) / (sum(convert(bigint,SnapExecutionCount)) + 0.0)) ELSE  convert(numeric(16,1), null) END, \n";
		}

		cols = 
			"  DBName, \n" +
			"  ObjectName, \n" +
			"  ObjectType, \n" +
			"  NumberOfPlanesInCache = count(*), \n" +
			SumActive + ase1600_nl +
			"  SumMemUsageKB         = sum(convert(bigint,MemUsageKB)), \n" +
			"  MaxCompileDate        = max(CompileDate), \n" +
//			"  MinCompileAgeInSec    = datediff(ss, max(CompileDate), getdate()), \n" +
//			"  MinCompileAgeInSec    = CASE WHEN datediff(day, CompileDate, getdate()) >= 24 THEN -1 ELSE  datediff(ss, CompileDate, getdate()) END, \n" +
			"  MinCompileAgeInSec    = min(CASE WHEN datediff(day, CompileDate, getdate()) >= 1095 THEN -1 ELSE  datediff(ss, CompileDate, getdate()) END), \n" + // 1095 days = 3 years... it migt be better to check if value is before 1970 or when seconds is bigger than MAX_INT
			"  SumRequestCnt         = sum(convert(bigint,RequestCnt)), \n" +
			"  SumRequestCntDiff     = sum(convert(bigint,RequestCnt)), \n" +
			"  SumTempdbRemapCnt     = sum(convert(bigint,TempdbRemapCnt)), \n" +
			"  MaxAvgTempdbRemapTime = max(AvgTempdbRemapTime), \n" +
			SumExecutionCount +   
			AvgCPUTime + 
			AvgExecutionTime + 
			AvgPhysicalReads + 
			AvgLogicalReads + 
			AvgPhysicalWrites + 
			AvgPagesWritten +
			SumCPUTime + 
			SumExecutionTime + 
			SumPhysicalReads + 
			SumLogicalReads + 
			SumPhysicalWrites + 
			SumPagesWritten +

			SumSnapExecutionCount +
			AvgSnapCodegenTime +
			AvgSnapJITTime +
			AvgSnapExecutionTime +
			SumSnapCodegenTime +
			SumSnapJITTime +
			SumSnapExecutionTime +
			"";

		// remove last comma
		cols = StringUtil.removeLastComma(cols);

		// Build where clause
		String whereClause = "where 1=1 \n";
		if ( ! sample_statementCacheObjects ) whereClause += "  and ObjectName not like '*ss%' \n";
		if ( ! sample_dynamicSqlObjects     ) whereClause += "  and ObjectName not like '*sq%' \n";
		
		String sql = 
			"select " + cols + "\n" +
			"from master..monCachedProcedures \n" +
			whereClause +
			"group by DBName, ObjectName, ObjectType \n" +
			"order by "+orderByColumnNumber+" desc" +
			"";
//DELETEME
//setNonConfiguredMonitoringAllowed(true);

		return sql;
	}

	@Override
	public void localCalculation(CounterSample prevSample, CounterSample newSample, CounterSample diffData)
	{
		long SumExecutionCount,          SumRequestCnt;
		int  SumExecutionCount_pos = -1, SumRequestCnt_pos = -1;

		long SumCPUTime,          SumExecutionTime,          SumPhysicalReads,          SumLogicalReads,          SumPhysicalWrites,          SumPagesWritten;
		int  SumCPUTime_pos = -1, SumExecutionTime_pos = -1, SumPhysicalReads_pos = -1, SumLogicalReads_pos = -1, SumPhysicalWrites_pos = -1, SumPagesWritten_pos = -1;
		int  AvgCPUTime_pos = -1, AvgExecutionTime_pos = -1, AvgPhysicalReads_pos = -1, AvgLogicalReads_pos = -1, AvgPhysicalWrites_pos = -1, AvgPagesWritten_pos = -1;

		
		long SumSnapExecutionCount;
		int  SumSnapExecutionCount_pos = -1;

		long SumSnapCodegenTime,          SumSnapJITTime,          SumSnapExecutionTime;
		int  SumSnapCodegenTime_pos = -1, SumSnapJITTime_pos = -1, SumSnapExecutionTime_pos = -1;
		int  AvgSnapCodegenTime_pos = -1, AvgSnapJITTime_pos = -1, AvgSnapExecutionTime_pos = -1;

		
		// Find column Id's
		List<String> colNames = diffData.getColNames();
		if (colNames == null)
			return;
		for (int colId = 0; colId < colNames.size(); colId++)
		{
			String colName = (String) colNames.get(colId);
			if      (colName.equals("SumRequestCnt"))         SumRequestCnt_pos         = colId;
			else if (colName.equals("SumExecutionCount"))     SumExecutionCount_pos     = colId;
			else if (colName.equals("SumCPUTime"))            SumCPUTime_pos            = colId;
			else if (colName.equals("SumExecutionTime"))      SumExecutionTime_pos      = colId;
			else if (colName.equals("SumPhysicalReads"))      SumPhysicalReads_pos      = colId;
			else if (colName.equals("SumLogicalReads"))       SumLogicalReads_pos       = colId;
			else if (colName.equals("SumPhysicalWrites"))     SumPhysicalWrites_pos     = colId;
			else if (colName.equals("SumPagesWritten"))       SumPagesWritten_pos       = colId;
			else if (colName.equals("AvgCPUTime"))            AvgCPUTime_pos            = colId;
			else if (colName.equals("AvgExecutionTime"))      AvgExecutionTime_pos      = colId;
			else if (colName.equals("AvgPhysicalReads"))      AvgPhysicalReads_pos      = colId;
			else if (colName.equals("AvgLogicalReads"))       AvgLogicalReads_pos       = colId;
			else if (colName.equals("AvgPhysicalWrites"))     AvgPhysicalWrites_pos     = colId;
			else if (colName.equals("AvgPagesWritten"))       AvgPagesWritten_pos       = colId;

			else if (colName.equals("SumSnapExecutionCount")) SumSnapExecutionCount_pos = colId;
			else if (colName.equals("SumSnapCodegenTime"))    SumSnapCodegenTime_pos    = colId;
			else if (colName.equals("SumSnapJITTime"))        SumSnapJITTime_pos        = colId;
			else if (colName.equals("SumSnapExecutionTime"))  SumSnapExecutionTime_pos  = colId;
			else if (colName.equals("AvgSnapCodegenTime"))    AvgSnapCodegenTime_pos    = colId;
			else if (colName.equals("AvgSnapJITTime"))        AvgSnapJITTime_pos        = colId;
			else if (colName.equals("AvgSnapExecutionTime"))  AvgSnapExecutionTime_pos  = colId;
		}

		if (SumExecutionCount_pos >= 0)
		{
			// Loop on all diffData rows
			for (int rowId = 0; rowId < diffData.getRowCount(); rowId++)
			{
				SumRequestCnt     = ((Number) diffData.getValueAt(rowId, SumRequestCnt_pos    )).longValue();
				SumExecutionCount = ((Number) diffData.getValueAt(rowId, SumExecutionCount_pos)).longValue();
				SumCPUTime        = ((Number) diffData.getValueAt(rowId, SumCPUTime_pos       )).longValue();
				SumExecutionTime  = ((Number) diffData.getValueAt(rowId, SumExecutionTime_pos )).longValue();
				SumPhysicalReads  = ((Number) diffData.getValueAt(rowId, SumPhysicalReads_pos )).longValue();
				SumLogicalReads   = ((Number) diffData.getValueAt(rowId, SumLogicalReads_pos  )).longValue();
				SumPhysicalWrites = ((Number) diffData.getValueAt(rowId, SumPhysicalWrites_pos)).longValue();
				SumPagesWritten   = ((Number) diffData.getValueAt(rowId, SumPagesWritten_pos  )).longValue();
	
				if (SumExecutionCount == 0)
					SumExecutionCount = SumRequestCnt;

				doAvgCalculation(diffData, SumExecutionCount, SumCPUTime,        rowId, AvgCPUTime_pos);
				doAvgCalculation(diffData, SumExecutionCount, SumExecutionTime,  rowId, AvgExecutionTime_pos);
				doAvgCalculation(diffData, SumExecutionCount, SumPhysicalReads,  rowId, AvgPhysicalReads_pos);
				doAvgCalculation(diffData, SumExecutionCount, SumLogicalReads,   rowId, AvgLogicalReads_pos);
				doAvgCalculation(diffData, SumExecutionCount, SumPhysicalWrites, rowId, AvgPhysicalWrites_pos);
				doAvgCalculation(diffData, SumExecutionCount, SumPagesWritten,   rowId, AvgPagesWritten_pos);

				if (SumSnapExecutionCount_pos >= 0)
				{
					SumSnapExecutionCount = ((Number) diffData.getValueAt(rowId, SumSnapExecutionCount_pos)).longValue();
					SumSnapCodegenTime    = ((Number) diffData.getValueAt(rowId, SumSnapCodegenTime_pos   )).longValue();
					SumSnapJITTime        = ((Number) diffData.getValueAt(rowId, SumSnapJITTime_pos       )).longValue();
					SumSnapExecutionTime  = ((Number) diffData.getValueAt(rowId, SumSnapExecutionTime_pos )).longValue();

					doAvgCalculation(diffData, SumSnapExecutionCount, SumSnapCodegenTime,   rowId, AvgSnapCodegenTime_pos);
					doAvgCalculation(diffData, SumSnapExecutionCount, SumSnapJITTime,       rowId, AvgSnapJITTime_pos);
					doAvgCalculation(diffData, SumSnapExecutionCount, SumSnapExecutionTime, rowId, AvgSnapExecutionTime_pos);
				}
			}
		}
	}
	private void doAvgCalculation(CounterSample data, long divByValue, long val, int rowId, int setColPos)
	{
		if (divByValue > 0)
		{
			double calc = (val + 0.0) / (divByValue + 0.0);

			BigDecimal newVal = new BigDecimal(calc).setScale(1, BigDecimal.ROUND_HALF_EVEN);
			data.setValueAt(newVal, rowId, setColPos);
		}
		else
			data.setValueAt(new BigDecimal(0), rowId, setColPos);
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
//		if (getServerVersion() < 15500)
//		if (getServerVersion() < 1550000)
		if (getServerVersion() < Ver.ver(15,5))
			return null;

		String[] sa = {"SumRequestCntDiff"};
		return sa;
	}
}
