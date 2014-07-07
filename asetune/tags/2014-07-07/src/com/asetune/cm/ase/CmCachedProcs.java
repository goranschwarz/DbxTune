package com.asetune.cm.ase;

import java.math.BigDecimal;
import java.sql.Connection;
import java.util.LinkedList;
import java.util.List;

import javax.naming.NameNotFoundException;

import com.asetune.ICounterController;
import com.asetune.IGuiController;
import com.asetune.MonTablesDictionary;
import com.asetune.cm.CounterSetTemplates;
import com.asetune.cm.CounterSetTemplates.Type;
import com.asetune.cm.CountersModel;
import com.asetune.cm.SamplingCnt;
import com.asetune.cm.ase.gui.CmCachedProcsPanel;
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

	public static final int      NEED_SRV_VERSION = 0;
	public static final int      NEED_CE_VERSION  = 0;

	public static final String[] MON_TABLES       = new String[] {"monCachedProcedures"};
	public static final String[] NEED_ROLES       = new String[] {"mon_role"};
	public static final String[] NEED_CONFIG      = new String[] {"enable monitoring=1", "per object statistics active=1", "statement statistics active=1"};

	public static final String[] PCT_COLUMNS      = new String[] {};
	public static final String[] DIFF_COLUMNS     = new String[] {
		"RequestCntDiff", "TempdbRemapCnt", "ExecutionCount", "CPUTime", "ExecutionTime", 
		"PhysicalReads", "LogicalReads", "PhysicalWrites", "PagesWritten"};

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
		return new CmCachedProcsPanel(this);
	}

	@Override
	public String[] getDependsOnConfigForVersion(Connection conn, int srvVersion, boolean isClusterEnabled)
	{
//		if (srvVersion >= 15700)
//		if (srvVersion >= 1570000)
		if (srvVersion >= Ver.ver(15,7))
			return NEED_CONFIG;

		return new String[] {"per object statistics active=1"};
	}

	@Override
	public void addMonTableDictForVersion(Connection conn, int aseVersion, boolean isClusterEnabled)
	{
		try 
		{
			MonTablesDictionary mtd = MonTablesDictionary.getInstance();
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
		}
		catch (NameNotFoundException e) {/*ignore*/}
	}

	@Override
	public List<String> getPkForVersion(Connection conn, int srvVersion, boolean isClusterEnabled)
	{
		List <String> pkCols = new LinkedList<String>();

		if (isClusterEnabled)
			pkCols.add("InstanceID");

		pkCols.add("PlanID");

		return pkCols;
	}

	@Override
	public String getSqlForVersion(Connection conn, int aseVersion, boolean isClusterEnabled)
	{
		String cols = "";

		String orderBy = "order by DBName, ObjectName, ObjectType \n";

		// ASE cluster edition
		String InstanceID = "";

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
		
		
		if (isClusterEnabled)
		{
			InstanceID = "InstanceID, ";
		}

//		if (aseVersion >= 15500 || (aseVersion >= 15030 && isClusterEnabled) )
//		if (aseVersion >= 1550000 || (aseVersion >= 1503000 && isClusterEnabled) )
		if (aseVersion >= Ver.ver(15,5) || (aseVersion >= Ver.ver(15,0,3) && isClusterEnabled) )
		{
			orderBy = "order by RequestCnt desc \n";

			RequestCnt         = "RequestCnt, RequestCntDiff = RequestCnt, ";
			TempdbRemapCnt     = "TempdbRemapCnt, ";
			AvgTempdbRemapTime = "AvgTempdbRemapTime, ";
			ase1550_nl         = "\n";
		}

//		if (aseVersion >= 15700)
//		if (aseVersion >= 1570000)
		if (aseVersion >= Ver.ver(15,7))
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
		
//		if (aseVersion >= 1600000)
		if (aseVersion >= Ver.ver(16,0))
		{
			Active            = "Active, ";
		}

		cols = 
			InstanceID + 
			"PlanID, DBName, ObjectName, ObjectType, " + Active + "MemUsageKB, CompileDate, CompileAgeInSec=datediff(ss, CompileDate, getdate()), " +
			ase1550_nl + RequestCnt + TempdbRemapCnt + AvgTempdbRemapTime +
			ase1570_nl + ExecutionCount + ase1570_nl +  
			AvgCPUTime + 
			AvgExecutionTime + 
			AvgPhysicalReads + 
			AvgLogicalReads + 
			AvgPhysicalWrites + 
			AvgPagesWritten +
			CPUTime + ExecutionTime + PhysicalReads + LogicalReads + PhysicalWrites + PagesWritten +
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
	public void localCalculation(SamplingCnt prevSample, SamplingCnt newSample, SamplingCnt diffData)
	{
		long ExecutionCount,          RequestCnt;
		int  ExecutionCount_pos = -1, RequestCnt_pos = -1;

		long CPUTime,             ExecutionTime,             PhysicalReads,             LogicalReads,             PhysicalWrites,             PagesWritten;
		int  CPUTime_pos    = -1, ExecutionTime_pos    = -1, PhysicalReads_pos    = -1, LogicalReads_pos    = -1, PhysicalWrites_pos    = -1, PagesWritten_pos    = -1;
		int  AvgCPUTime_pos = -1, AvgExecutionTime_pos = -1, AvgPhysicalReads_pos = -1, AvgLogicalReads_pos = -1, AvgPhysicalWrites_pos = -1, AvgPagesWritten_pos = -1;

		// Find column Id's
		List<String> colNames = diffData.getColNames();
		if (colNames == null)
			return;
		for (int colId = 0; colId < colNames.size(); colId++)
		{
			String colName = (String) colNames.get(colId);
			if      (colName.equals("RequestCnt"))        RequestCnt_pos        = colId;
			else if (colName.equals("ExecutionCount"))    ExecutionCount_pos    = colId;
			else if (colName.equals("CPUTime"))           CPUTime_pos           = colId;
			else if (colName.equals("ExecutionTime"))     ExecutionTime_pos     = colId;
			else if (colName.equals("PhysicalReads"))     PhysicalReads_pos     = colId;
			else if (colName.equals("LogicalReads"))      LogicalReads_pos      = colId;
			else if (colName.equals("PhysicalWrites"))    PhysicalWrites_pos    = colId;
			else if (colName.equals("PagesWritten"))      PagesWritten_pos      = colId;
			else if (colName.equals("AvgCPUTime"))        AvgCPUTime_pos        = colId;
			else if (colName.equals("AvgExecutionTime"))  AvgExecutionTime_pos  = colId;
			else if (colName.equals("AvgPhysicalReads"))  AvgPhysicalReads_pos  = colId;
			else if (colName.equals("AvgLogicalReads"))   AvgLogicalReads_pos   = colId;
			else if (colName.equals("AvgPhysicalWrites")) AvgPhysicalWrites_pos = colId;
			else if (colName.equals("AvgPagesWritten"))   AvgPagesWritten_pos   = colId;
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
			}
		}
	}
	private void doAvgCalculation(SamplingCnt data, long divByValue, long val, int rowId, int setColPos)
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

		String[] sa = {"RequestCntDiff"};
		return sa;
	}
}
