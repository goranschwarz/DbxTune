package com.asetune.cm.ase;

import java.sql.Connection;
import java.util.LinkedList;
import java.util.List;

import com.asetune.ICounterController;
import com.asetune.IGuiController;
import com.asetune.cm.CounterSetTemplates;
import com.asetune.cm.CounterSetTemplates.Type;
import com.asetune.cm.CountersModel;
import com.asetune.cm.ase.gui.CmCachedProcsPanel;
import com.asetune.gui.MainFrame;
import com.asetune.gui.TabularCntrPanel;
import com.asetune.utils.StringUtil;

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
	public static final String[] NEED_ROLES       = new String[] {"sa_role"};
	public static final String[] NEED_CONFIG      = new String[] {"enable monitoring=1", "per object statistics active=1", "statement statistics active=1"};

	public static final String[] PCT_COLUMNS      = new String[] {};
	public static final String[] DIFF_COLUMNS     = new String[] {
		"RequestCnt", "TempdbRemapCnt", "ExecutionCount", "CPUTime", "ExecutionTime", 
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
		if (srvVersion >= 15700)
			return NEED_CONFIG;

		return new String[] {"per object statistics active=1"};
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
		String ExecutionCount = "";
		String CPUTime        = "";
		String ExecutionTime  = "";
		String PhysicalReads  = "";
		String LogicalReads   = "";
		String PhysicalWrites = "";
		String PagesWritten   = "";
		String ase1570_nl     = "";

		if (isClusterEnabled)
		{
			InstanceID = "InstanceID, ";
		}

		if (aseVersion >= 15500 || (aseVersion >= 15030 && isClusterEnabled) )
		{
			orderBy = "order by RequestCnt desc \n";

			RequestCnt         = "RequestCnt, ";
			TempdbRemapCnt     = "TempdbRemapCnt, ";
			AvgTempdbRemapTime = "AvgTempdbRemapTime, ";
			ase1550_nl         = "\n";
		}

		if (aseVersion >= 15700)
		{
			ExecutionCount = "ExecutionCount, ";
			CPUTime        = "CPUTime, ";
			ExecutionTime  = "ExecutionTime, ";
			PhysicalReads  = "PhysicalReads, ";
			LogicalReads   = "LogicalReads, ";
			PhysicalWrites = "PhysicalWrites, ";
			PagesWritten   = "PagesWritten, ";
			ase1570_nl     = "\n";
		}
		
		cols = 
			InstanceID + 
			"PlanID, DBName, ObjectName, ObjectType, MemUsageKB, CompileDate, " +
			ase1550_nl + RequestCnt + TempdbRemapCnt + AvgTempdbRemapTime +
			ase1570_nl + ExecutionCount + CPUTime + ExecutionTime + PhysicalReads + LogicalReads + PhysicalWrites + PagesWritten +
			"";

		// remove last comma
		cols = StringUtil.removeLastComma(cols);

		String sql = 
			"select " + cols + "\n" +
			"from master..monCachedProcedures \n" +
			orderBy;

		return sql;
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
		if (getServerVersion() < 15500)
			return null;

		String[] sa = {"RequestCnt"};
		return sa;
	}
}
