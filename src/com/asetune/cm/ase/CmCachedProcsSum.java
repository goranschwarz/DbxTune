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

	public static final int      NEED_SRV_VERSION = 1550000;
	public static final int      NEED_CE_VERSION  = 0;

	public static final String[] MON_TABLES       = new String[] {"monCachedProcedures"};
	public static final String[] NEED_ROLES       = new String[] {"mon_role"};
	public static final String[] NEED_CONFIG      = new String[] {"enable monitoring=1", "per object statistics active=1", "statement statistics active=1"};

	public static final String[] PCT_COLUMNS      = new String[] {};
	public static final String[] DIFF_COLUMNS     = new String[] {
		"SumRequestCntDiff", "SumTempdbRemapCnt", "SumExecutionCount", "SumCPUTime", "SumExecutionTime", 
		"SumPhysicalReads", "SumLogicalReads", "SumPhysicalWrites", "SumPagesWritten"};

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
		// YES THIS SHOULD BE CmCachedProcsPanel and not CmCachedProcsSumPanel, it's only Icon renders for the JXTable
		return new CmCachedProcsPanel(this);
	}

	@Override
	public String[] getDependsOnConfigForVersion(Connection conn, int srvVersion, boolean isClusterEnabled)
	{
		if (srvVersion >= 1570000)
			return NEED_CONFIG;

		return new String[] {"per object statistics active=1"};
	}

	@Override
	public void addMonTableDictForVersion(Connection conn, int aseVersion, boolean isClusterEnabled)
	{
		try 
		{
			MonTablesDictionary mtd = MonTablesDictionary.getInstance();
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
		}
		catch (NameNotFoundException e) {/*ignore*/}
	}

	@Override
	public List<String> getPkForVersion(Connection conn, int srvVersion, boolean isClusterEnabled)
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
	public String getSqlForVersion(Connection conn, int aseVersion, boolean isClusterEnabled)
	{
		String cols = "";

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

		if (aseVersion >= 1570000)
		{
			SumExecutionCount = "SumExecutionCount = sum(ExecutionCount), \n";
			SumCPUTime        = "SumCPUTime        = sum(CPUTime), \n";
			SumExecutionTime  = "SumExecutionTime  = sum(ExecutionTime), \n";
			SumPhysicalReads  = "SumPhysicalReads  = sum(PhysicalReads), \n";
			SumLogicalReads   = "SumLogicalReads   = sum(LogicalReads), \n";
			SumPhysicalWrites = "SumPhysicalWrites = sum(PhysicalWrites), \n";
			SumPagesWritten   = "SumPagesWritten   = sum(PagesWritten), \n";

			AvgCPUTime        = "AvgCPUTime        = CASE WHEN sum(ExecutionCount) > 0 THEN convert(numeric(16,1), (sum(CPUTime       ) + 0.0) / (sum(ExecutionCount) + 0.0)) ELSE  convert(numeric(16,1), null) END, \n";
			AvgExecutionTime  = "AvgExecutionTime  = CASE WHEN sum(ExecutionCount) > 0 THEN convert(numeric(16,1), (sum(ExecutionTime ) + 0.0) / (sum(ExecutionCount) + 0.0)) ELSE  convert(numeric(16,1), null) END, \n";
			AvgPhysicalReads  = "AvgPhysicalReads  = CASE WHEN sum(ExecutionCount) > 0 THEN convert(numeric(16,1), (sum(PhysicalReads ) + 0.0) / (sum(ExecutionCount) + 0.0)) ELSE  convert(numeric(16,1), null) END, \n";
			AvgLogicalReads   = "AvgLogicalReads   = CASE WHEN sum(ExecutionCount) > 0 THEN convert(numeric(16,1), (sum(LogicalReads  ) + 0.0) / (sum(ExecutionCount) + 0.0)) ELSE  convert(numeric(16,1), null) END, \n";
			AvgPhysicalWrites = "AvgPhysicalWrites = CASE WHEN sum(ExecutionCount) > 0 THEN convert(numeric(16,1), (sum(PhysicalWrites) + 0.0) / (sum(ExecutionCount) + 0.0)) ELSE  convert(numeric(16,1), null) END, \n";
			AvgPagesWritten   = "AvgPagesWritten   = CASE WHEN sum(ExecutionCount) > 0 THEN convert(numeric(16,1), (sum(PagesWritten  ) + 0.0) / (sum(ExecutionCount) + 0.0)) ELSE  convert(numeric(16,1), null) END, \n";
		}
		
		cols = 
			"DBName, ObjectName, ObjectType, NumberOfPlanesInCache=count(*), \n" +
			"SumMemUsageKB=sum(MemUsageKB), \n" +
			"MaxCompileDate=max(CompileDate), MinCompileAgeInSec=datediff(ss, max(CompileDate), getdate()), \n" +
			"SumRequestCnt=sum(RequestCnt), SumRequestCntDiff = sum(RequestCnt), \n" +
			"SumTempdbRemapCnt=sum(TempdbRemapCnt), MaxAvgTempdbRemapTime=max(AvgTempdbRemapTime), \n" +
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
			"";

		// remove last comma
		cols = StringUtil.removeLastComma(cols);

		String sql = 
			"select " + cols + "\n" +
			"from master..monCachedProcedures \n" +
			"group by DBName, ObjectName, ObjectType \n" +
			"order by 8 desc" +
			"";
//DELETEME
//setNonConfiguredMonitoringAllowed(true);

		return sql;
	}

	@Override
	public void localCalculation(SamplingCnt prevSample, SamplingCnt newSample, SamplingCnt diffData)
	{
		long SumExecutionCount,          SumRequestCnt;
		int  SumExecutionCount_pos = -1, SumRequestCnt_pos = -1;

		long SumCPUTime,          SumExecutionTime,          SumPhysicalReads,          SumLogicalReads,          SumPhysicalWrites,          SumPagesWritten;
		int  SumCPUTime_pos = -1, SumExecutionTime_pos = -1, SumPhysicalReads_pos = -1, SumLogicalReads_pos = -1, SumPhysicalWrites_pos = -1, SumPagesWritten_pos = -1;
		int  AvgCPUTime_pos = -1, AvgExecutionTime_pos = -1, AvgPhysicalReads_pos = -1, AvgLogicalReads_pos = -1, AvgPhysicalWrites_pos = -1, AvgPagesWritten_pos = -1;

		// Find column Id's
		List<String> colNames = diffData.getColNames();
		if (colNames == null)
			return;
		for (int colId = 0; colId < colNames.size(); colId++)
		{
			String colName = (String) colNames.get(colId);
			if      (colName.equals("SumRequestCnt"))     SumRequestCnt_pos        = colId;
			else if (colName.equals("SumExecutionCount")) SumExecutionCount_pos    = colId;
			else if (colName.equals("SumCPUTime"))        SumCPUTime_pos           = colId;
			else if (colName.equals("SumExecutionTime"))  SumExecutionTime_pos     = colId;
			else if (colName.equals("SumPhysicalReads"))  SumPhysicalReads_pos     = colId;
			else if (colName.equals("SumLogicalReads"))   SumLogicalReads_pos      = colId;
			else if (colName.equals("SumPhysicalWrites")) SumPhysicalWrites_pos    = colId;
			else if (colName.equals("SumPagesWritten"))   SumPagesWritten_pos      = colId;
			else if (colName.equals("AvgCPUTime"))        AvgCPUTime_pos        = colId;
			else if (colName.equals("AvgExecutionTime"))  AvgExecutionTime_pos  = colId;
			else if (colName.equals("AvgPhysicalReads"))  AvgPhysicalReads_pos  = colId;
			else if (colName.equals("AvgLogicalReads"))   AvgLogicalReads_pos   = colId;
			else if (colName.equals("AvgPhysicalWrites")) AvgPhysicalWrites_pos = colId;
			else if (colName.equals("AvgPagesWritten"))   AvgPagesWritten_pos   = colId;
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
		if (getServerVersion() < 1550000)
			return null;

		String[] sa = {"RequestCntDiff"};
		return sa;
	}
}
