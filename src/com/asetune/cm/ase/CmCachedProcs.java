/*******************************************************************************
 * Copyright (C) 2010-2019 Goran Schwarz
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
import java.sql.Connection;
import java.util.LinkedList;
import java.util.List;

import javax.naming.NameNotFoundException;

import com.asetune.ICounterController;
import com.asetune.IGuiController;
import com.asetune.cm.CounterSample;
import com.asetune.cm.CounterSetTemplates;
import com.asetune.cm.CounterSetTemplates.Type;
import com.asetune.cm.CountersModel;
import com.asetune.cm.ase.gui.CmCachedProcsPanel;
import com.asetune.config.dict.MonTablesDictionary;
import com.asetune.config.dict.MonTablesDictionaryManager;
import com.asetune.gui.MainFrame;
import com.asetune.gui.TabularCntrPanel;
import com.asetune.utils.StringUtil;
import com.asetune.utils.Ver;

/**
 * @author Goran Schwarz (goran_schwarz@hotmail.com)
 */
public class CmCachedProcs
extends CountersModel
{
//	private static Logger        _logger          = Logger.getLogger(CmCachedProcs.class);
	private static final long    serialVersionUID = 1L;

	public static final String   CM_NAME          = CmCachedProcs.class.getSimpleName();
	public static final String   SHORT_NAME       = "Cached Procedures";
	public static final String   HTML_DESC        = 
		"<html>" +
		"What Objects is located in the 'procedure cache'.<br>" +
		"From ASE 15.7 you will also have execution times etc.<br>" +
		"</html>";

	public static final String   GROUP_NAME       = MainFrame.TCP_GROUP_CACHE;
	public static final String   GUI_ICON_FILE    = "images/"+CM_NAME+".png";

	public static final long     NEED_SRV_VERSION = 0;
	public static final long     NEED_CE_VERSION  = 0;

	public static final String[] MON_TABLES       = new String[] {"monCachedProcedures"};
	public static final String[] NEED_ROLES       = new String[] {"mon_role"};
	public static final String[] NEED_CONFIG      = new String[] {"enable monitoring=1", "per object statistics active=1", "statement statistics active=1"};

	public static final String[] PCT_COLUMNS      = new String[] {};
	public static final String[] DIFF_COLUMNS     = new String[] {
		"RequestCntDiff", 
		"TempdbRemapCnt", 
		"ExecutionCount", 
		"CPUTime", 
		"ExecutionTime", 
		"PhysicalReads", 
		"LogicalReads", 
		"PhysicalWrites", 
		"PagesWritten",
		"SnapCodegenTime",    // 16.0 SP2
		"SnapJITTime",        // 16.0 SP2
		"SnapExecutionTime",  // 16.0 SP2
		"SnapExecutionCount"  // 16.0 SP2
	};

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

		return new CmCachedProcs(counterController, guiController);
	}

	public CmCachedProcs(ICounterController counterController, IGuiController guiController)
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
		return new CmCachedProcsPanel(this);
	}

	@Override
	public String[] getDependsOnConfigForVersion(Connection conn, long srvVersion, boolean isClusterEnabled)
	{
//		if (srvVersion >= 15700)
//		if (srvVersion >= 1570000)
		if (srvVersion >= Ver.ver(15,7))
			return NEED_CONFIG;

		return new String[] {"per object statistics active=1"};
	}

	@Override
	public void addMonTableDictForVersion(Connection conn, long srvVersion, boolean isClusterEnabled)
	{
		try 
		{
			MonTablesDictionary mtd = MonTablesDictionaryManager.getInstance();
			mtd.addColumn("monCachedProcedures",  "CompileAgeInSec",     "<html>" +
			                                                                     "How many seconds where this plan compiled<br>" +
			                                                                     "<b>Formula</b>: datediff(ss, CompileDate, getdate())<br>" +
			                                                             "</html>");
			mtd.addColumn("monCachedProcedures",  "AvgCPUTime",          "<html>" +
			                                                                     "CPU Time per execution count<br>" +
			                                                                     "<b>Formula</b>: CPUTime / ExecutionCount<br>" +
			                                                             "</html>");
			mtd.addColumn("monCachedProcedures",  "AvgExecutionTime",    "<html>" +
			                                                                     "Execution Time per execution count<br>" +
			                                                                     "<b>Formula</b>: ExecutionTime / ExecutionCount<br>" +
			                                                             "</html>");
			mtd.addColumn("monCachedProcedures",  "AvgPhysicalReads",    "<html>" +
			                                                                     "Physical Reads per execution count<br>" +
			                                                                     "<b>Formula</b>: PhysicalReads / ExecutionCount<br>" +
			                                                             "</html>");
			mtd.addColumn("monCachedProcedures",  "AvgLogicalReads",     "<html>" +
			                                                                     "Logical Reads per execution count<br>" +
			                                                                     "<b>Formula</b>: LogicalReads / ExecutionCount<br>" +
			                                                             "</html>");
			mtd.addColumn("monCachedProcedures",  "AvgPhysicalWrites",   "<html>" +
			                                                                     "Physical Writes per execution count<br>" +
			                                                                     "<b>Formula</b>: PhysicalWrites / ExecutionCount<br>" +
			                                                             "</html>");
			mtd.addColumn("monCachedProcedures",  "AvgPagesWritten",     "<html>" +
			                                                                     "Pages Written per execution count<br>" +
			                                                                     "<b>Formula</b>: PagesWritten / ExecutionCount<br>" +
			                                                             "</html>");

			mtd.addColumn("monCachedProcedures",  "AvgSnapCodegenTime",  "<html>" +
			                                                                  "Time (microseconds) Spent in Code Generation per SNAP execution count. SNAP = Simplfied Native Access Plans<br>" +
			                                                                  "<b>Formula</b>: SnapCodegenTime / SnapExecutionCount<br>" +
			                                                             "</html>");
			mtd.addColumn("monCachedProcedures",  "AvgSnapJITTime",      "<html>" +
			                                                                  "Time (microseconds) Spent in JustInTime Compilation per SNAP execution count. SNAP = Simplfied Native Access Plans<br>" +
			                                                                  "<b>Formula</b>: SnapJITTime / SnapExecutionCount<br>" +
			                                                             "</html>");
			mtd.addColumn("monCachedProcedures",  "AvgSnapExecutionTime","<html>" +
			                                                                  "Time (microseconds) spent in Execution per SNAP execution count. SNAP = Simplfied Native Access Plans<br>" +
			                                                                  "<b>Formula</b>: SnapExecutionTime / SnapExecutionCount<br>" +
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

		pkCols.add("PlanID");

		return pkCols;
	}

	@Override
	public String getSqlForVersion(Connection conn, long srvVersion, boolean isClusterEnabled)
	{
		String cols = "";

		String orderBy = "order by DBName, ObjectName, ObjectType \n";

		// ASE cluster edition
		String InstanceID = "";

		// Split ObjectName *ssXXXXXXXXXX_YYYYYYYYYYss* into SSqlId=X and Hashkey=Y  only after 15.0, since bigint was introduced in 15.0
		String SSQLID  = "";
		String Hashkey = "";
		
		// ASE 15.5 or (15.0.3 in cluster edition) 
		String RequestCnt         = "";
		String TempdbRemapCnt     = "";
		String AvgTempdbRemapTime = "";
		String ase1550_nl         = "";

		// ASE 15.7
		String ExecutionCount    = "";
		String CPUTime           = "";
		String ExecutionTime     = "";
		String PhysicalReads     = "";
		String LogicalReads      = "";
		String PhysicalWrites    = "";
		String PagesWritten      = "";
		String AvgCPUTime        = ""; // xxx / ExecutionCount
		String AvgExecutionTime  = ""; // xxx / ExecutionCount
		String AvgPhysicalReads  = ""; // xxx / ExecutionCount
		String AvgLogicalReads   = ""; // xxx / ExecutionCount
		String AvgPhysicalWrites = ""; // xxx / ExecutionCount
		String AvgPagesWritten   = ""; // xxx / ExecutionCount
		String ase1570_nl        = "";

		// ASE 16.0
		String Active            = ""; // Indicates whether the plan for this procedure is active or not
		
		// ASE 16.0 SP2
		String SnapCodegenTime      = "";
		String SnapJITTime          = "";
		String SnapExecutionTime    = "";
		String SnapExecutionCount   = "";
		String AvgSnapCodegenTime   = ""; // xxx / SnapExecutionCount
		String AvgSnapJITTime       = ""; // xxx / SnapExecutionCount
		String AvgSnapExecutionTime = ""; // xxx / SnapExecutionCount
		String ase160_sp2_nl      = "";

//---------------------------------------------------------------------------------------------------------------------------
//FIXME: Add one of the following to break ObjectName into SSQLID and Hashkey to easier sort/look for hashkey duplicates...
//---------------------------------------------------------------------------------------------------------------------------
//		CASE WHEN ObjectName like '*%' THEN convert(bigint, substring(ObjectName, 4, 10)) ELSE null END as SSQLID, 
//		CASE WHEN ObjectName like '*%' THEN convert(bigint, substring(ObjectName, 15, 10)) ELSE null END as Hashkey, 
//
// as String instead, if bigint isn't supported, or if "jxtable sorting" in combination with NULL values are cases problems...
//		CASE WHEN ObjectName like '*%' THEN substring(ObjectName, 4, 10) ELSE null END as SSQLID_str, 
//		CASE WHEN ObjectName like '*%' THEN substring(ObjectName, 15, 10) ELSE null END as Hashkey_str, 
//---------------------------------------------------------------------------------------------------------------------------
		
		if (isClusterEnabled)
		{
			InstanceID = "InstanceID, ";
		}

		if (srvVersion > Ver.ver(15,0))
		{
			// Split ObjectName *s{s|q}XXXXXXXXXX_YYYYYYYYYYss* into SSqlId=X and Hashkey=Y  only after 15.0, since bigint was introduced in 15.0
			SSQLID  = "CASE WHEN ObjectName like '*s%' THEN convert(bigint, substring(ObjectName,  4, 10)) ELSE -1 END as SSQLID, \n";
			Hashkey = "CASE WHEN ObjectName like '*s%' THEN convert(bigint, substring(ObjectName, 15, 10)) ELSE -1 END as Hashkey, \n";
		}

		if (srvVersion >= Ver.ver(15,5) || (srvVersion >= Ver.ver(15,0,3) && isClusterEnabled) )
		{
			orderBy = "order by RequestCnt desc \n";

			RequestCnt         = "RequestCnt, RequestCntDiff = RequestCnt, ";
			TempdbRemapCnt     = "TempdbRemapCnt, ";
			AvgTempdbRemapTime = "AvgTempdbRemapTime, ";
			ase1550_nl         = "\n";
		}

		if (srvVersion >= Ver.ver(15,7))
		{
			ExecutionCount    = "ExecutionCount, ";
			CPUTime           = "CPUTime, ";
			ExecutionTime     = "ExecutionTime, ";
			PhysicalReads     = "PhysicalReads, ";
			LogicalReads      = "LogicalReads, ";
			PhysicalWrites    = "PhysicalWrites, ";
			PagesWritten      = "PagesWritten, ";

			AvgCPUTime        = "AvgCPUTime        = CASE WHEN ExecutionCount > 0 THEN convert(numeric(16,1), (CPUTime        + 0.0) / (ExecutionCount + 0.0)) ELSE  convert(numeric(16,1), null) END, \n";
			AvgExecutionTime  = "AvgExecutionTime  = CASE WHEN ExecutionCount > 0 THEN convert(numeric(16,1), (ExecutionTime  + 0.0) / (ExecutionCount + 0.0)) ELSE  convert(numeric(16,1), null) END, \n";
			AvgPhysicalReads  = "AvgPhysicalReads  = CASE WHEN ExecutionCount > 0 THEN convert(numeric(16,1), (PhysicalReads  + 0.0) / (ExecutionCount + 0.0)) ELSE  convert(numeric(16,1), null) END, \n";
			AvgLogicalReads   = "AvgLogicalReads   = CASE WHEN ExecutionCount > 0 THEN convert(numeric(16,1), (LogicalReads   + 0.0) / (ExecutionCount + 0.0)) ELSE  convert(numeric(16,1), null) END, \n";
			AvgPhysicalWrites = "AvgPhysicalWrites = CASE WHEN ExecutionCount > 0 THEN convert(numeric(16,1), (PhysicalWrites + 0.0) / (ExecutionCount + 0.0)) ELSE  convert(numeric(16,1), null) END, \n";
			AvgPagesWritten   = "AvgPagesWritten   = CASE WHEN ExecutionCount > 0 THEN convert(numeric(16,1), (PagesWritten   + 0.0) / (ExecutionCount + 0.0)) ELSE  convert(numeric(16,1), null) END, \n";

			ase1570_nl        = "\n";
		}
		
		if (srvVersion >= Ver.ver(16,0))
		{
			Active            = "Active, ";
		}

		if (srvVersion >= Ver.ver(16,0,0, 2))
		{
			SnapCodegenTime      = "SnapCodegenTime, ";
			SnapJITTime          = "SnapJITTime, ";
			SnapExecutionTime    = "SnapExecutionTime, ";
			SnapExecutionCount   = "SnapExecutionCount, ";

			AvgSnapCodegenTime   = "AvgSnapCodegenTime   = CASE WHEN SnapExecutionCount > 0 THEN convert(numeric(16,1), (SnapCodegenTime   + 0.0) / (SnapExecutionCount + 0.0)) ELSE  convert(numeric(16,1), null) END, \n";
			AvgSnapJITTime       = "AvgSnapJITTime       = CASE WHEN SnapExecutionCount > 0 THEN convert(numeric(16,1), (SnapJITTime       + 0.0) / (SnapExecutionCount + 0.0)) ELSE  convert(numeric(16,1), null) END, \n";
			AvgSnapExecutionTime = "AvgSnapExecutionTime = CASE WHEN SnapExecutionCount > 0 THEN convert(numeric(16,1), (SnapExecutionTime + 0.0) / (SnapExecutionCount + 0.0)) ELSE  convert(numeric(16,1), null) END, \n";

			ase160_sp2_nl        = "\n";
		}

		cols = 
			InstanceID + 
			"PlanID, DBName, ObjectName, \n" +
			SSQLID +
			Hashkey +
			"ObjectType, " + Active + "MemUsageKB, CompileDate, \n" + 
//			"CompileAgeInSec=datediff(ss, CompileDate, getdate()), " +
			"CompileAgeInSec = CASE WHEN datediff(day, CompileDate, getdate()) >= 24 THEN -1 ELSE  datediff(ss, CompileDate, getdate()) END, " +
			ase1550_nl + RequestCnt + TempdbRemapCnt + AvgTempdbRemapTime +
			ase1570_nl + ExecutionCount + ase1570_nl +  
			AvgCPUTime + 
			AvgExecutionTime + 
			AvgPhysicalReads + 
			AvgLogicalReads + 
			AvgPhysicalWrites + 
			AvgPagesWritten +
			CPUTime + ExecutionTime + PhysicalReads + LogicalReads + PhysicalWrites + PagesWritten +
			ase160_sp2_nl + SnapExecutionCount + ase160_sp2_nl +
			AvgSnapCodegenTime +
			AvgSnapJITTime +
			AvgSnapExecutionTime + 
			SnapCodegenTime + SnapJITTime + SnapExecutionTime +
			"";

		// remove last comma
		cols = StringUtil.removeLastComma(cols);

		String sql = 
			"select " + cols + "\n" +
			"from master..monCachedProcedures \n" +
			orderBy;

		return sql;
	}

	@Override
	public void localCalculation(CounterSample prevSample, CounterSample newSample, CounterSample diffData)
	{
		long ExecutionCount,          RequestCnt;
		int  ExecutionCount_pos = -1, RequestCnt_pos = -1;

		long    CPUTime,             ExecutionTime,             PhysicalReads,             LogicalReads,             PhysicalWrites,             PagesWritten;
		int     CPUTime_pos = -1,    ExecutionTime_pos = -1,    PhysicalReads_pos = -1,    LogicalReads_pos = -1,    PhysicalWrites_pos = -1,    PagesWritten_pos = -1;
		int  AvgCPUTime_pos = -1, AvgExecutionTime_pos = -1, AvgPhysicalReads_pos = -1, AvgLogicalReads_pos = -1, AvgPhysicalWrites_pos = -1, AvgPagesWritten_pos = -1;

		
		long SnapExecutionCount;
		int  SnapExecutionCount_pos = -1;

		long    SnapCodegenTime,             SnapJITTime,             SnapExecutionTime;
		int     SnapCodegenTime_pos = -1,    SnapJITTime_pos = -1,    SnapExecutionTime_pos    = -1;
		int  AvgSnapCodegenTime_pos = -1, AvgSnapJITTime_pos = -1, AvgSnapExecutionTime_pos = -1;

		// Find column Id's
		List<String> colNames = diffData.getColNames();
		if (colNames == null)
			return;
		for (int colId = 0; colId < colNames.size(); colId++)
		{
			String colName = (String) colNames.get(colId);
			if      (colName.equals("RequestCnt"))           RequestCnt_pos        = colId;
			else if (colName.equals("ExecutionCount"))       ExecutionCount_pos    = colId;
			else if (colName.equals("CPUTime"))              CPUTime_pos           = colId;
			else if (colName.equals("ExecutionTime"))        ExecutionTime_pos     = colId;
			else if (colName.equals("PhysicalReads"))        PhysicalReads_pos     = colId;
			else if (colName.equals("LogicalReads"))         LogicalReads_pos      = colId;
			else if (colName.equals("PhysicalWrites"))       PhysicalWrites_pos    = colId;
			else if (colName.equals("PagesWritten"))         PagesWritten_pos      = colId;
			else if (colName.equals("AvgCPUTime"))           AvgCPUTime_pos        = colId;
			else if (colName.equals("AvgExecutionTime"))     AvgExecutionTime_pos  = colId;
			else if (colName.equals("AvgPhysicalReads"))     AvgPhysicalReads_pos  = colId;
			else if (colName.equals("AvgLogicalReads"))      AvgLogicalReads_pos   = colId;
			else if (colName.equals("AvgPhysicalWrites"))    AvgPhysicalWrites_pos = colId;
			else if (colName.equals("AvgPagesWritten"))      AvgPagesWritten_pos   = colId;

			else if (colName.equals("SnapExecutionCount"))   SnapExecutionCount_pos   = colId;
			else if (colName.equals("SnapCodegenTime"))      SnapCodegenTime_pos      = colId;
			else if (colName.equals("SnapJITTime"))          SnapJITTime_pos          = colId;
			else if (colName.equals("SnapExecutionTime"))    SnapExecutionTime_pos    = colId;
			else if (colName.equals("AvgSnapCodegenTime"))   AvgSnapCodegenTime_pos   = colId;
			else if (colName.equals("AvgSnapJITTime"))       AvgSnapJITTime_pos       = colId;
			else if (colName.equals("AvgSnapExecutionTime")) AvgSnapExecutionTime_pos = colId;
		}

		if (ExecutionCount_pos >= 0)
		{
			// Loop on all diffData rows
			for (int rowId = 0; rowId < diffData.getRowCount(); rowId++)
			{
				RequestCnt     = ((Number) diffData.getValueAt(rowId, RequestCnt_pos    )).longValue();
				ExecutionCount = ((Number) diffData.getValueAt(rowId, ExecutionCount_pos)).longValue();
				CPUTime        = ((Number) diffData.getValueAt(rowId, CPUTime_pos       )).longValue();
				ExecutionTime  = ((Number) diffData.getValueAt(rowId, ExecutionTime_pos )).longValue();
				PhysicalReads  = ((Number) diffData.getValueAt(rowId, PhysicalReads_pos )).longValue();
				LogicalReads   = ((Number) diffData.getValueAt(rowId, LogicalReads_pos  )).longValue();
				PhysicalWrites = ((Number) diffData.getValueAt(rowId, PhysicalWrites_pos)).longValue();
				PagesWritten   = ((Number) diffData.getValueAt(rowId, PagesWritten_pos  )).longValue();
	
				if (ExecutionCount == 0)
					ExecutionCount = RequestCnt;

				doAvgCalculation(diffData, ExecutionCount, CPUTime,        rowId, AvgCPUTime_pos);
				doAvgCalculation(diffData, ExecutionCount, ExecutionTime,  rowId, AvgExecutionTime_pos);
				doAvgCalculation(diffData, ExecutionCount, PhysicalReads,  rowId, AvgPhysicalReads_pos);
				doAvgCalculation(diffData, ExecutionCount, LogicalReads,   rowId, AvgLogicalReads_pos);
				doAvgCalculation(diffData, ExecutionCount, PhysicalWrites, rowId, AvgPhysicalWrites_pos);
				doAvgCalculation(diffData, ExecutionCount, PagesWritten,   rowId, AvgPagesWritten_pos);

				if (SnapExecutionCount_pos >= 0)
				{
					SnapExecutionCount = ((Number) diffData.getValueAt(rowId, SnapExecutionCount_pos)).longValue();
					SnapCodegenTime    = ((Number) diffData.getValueAt(rowId, SnapCodegenTime_pos   )).longValue();
					SnapJITTime        = ((Number) diffData.getValueAt(rowId, SnapJITTime_pos       )).longValue();
					SnapExecutionTime  = ((Number) diffData.getValueAt(rowId, SnapExecutionTime_pos )).longValue();

					doAvgCalculation(diffData, SnapExecutionCount, SnapCodegenTime,   rowId, AvgSnapCodegenTime_pos);
					doAvgCalculation(diffData, SnapExecutionCount, SnapJITTime,       rowId, AvgSnapJITTime_pos);
					doAvgCalculation(diffData, SnapExecutionCount, SnapExecutionTime, rowId, AvgSnapExecutionTime_pos);
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
		if (getServerVersion() < Ver.ver(15,5))
			return null;

		String[] sa = {"RequestCntDiff"};
		return sa;
	}
}
