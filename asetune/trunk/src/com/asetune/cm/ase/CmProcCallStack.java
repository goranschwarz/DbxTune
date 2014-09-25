package com.asetune.cm.ase;

import java.sql.Connection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.log4j.Logger;

import com.asetune.ICounterController;
import com.asetune.IGuiController;
import com.asetune.cm.CounterSetTemplates;
import com.asetune.cm.CounterSetTemplates.Type;
import com.asetune.cm.CountersModel;
import com.asetune.cm.SamplingCnt;
import com.asetune.cm.ase.gui.CmProcCallStackPanel;
import com.asetune.gui.MainFrame;
import com.asetune.gui.TabularCntrPanel;
import com.asetune.utils.Ver;

/**
 * @author Goran Schwarz (goran_schwarz@hotmail.com)
 */
public class CmProcCallStack
extends CountersModel
{
	private static Logger        _logger          = Logger.getLogger(CmProcCallStack.class);
	private static final long    serialVersionUID = 1L;

	public static final String   CM_NAME          = CmProcCallStack.class.getSimpleName();
	public static final String   SHORT_NAME       = "Procedure Call Stack";
	public static final String   HTML_DESC        = 
		"<html>" +
		"Nesting levels of currently executed procedures, this can be used as a light 'profiler'" +
		"<br><br>" +
		"Table Background colors:" +
		"<ul>" +
		"    <li>GREEN - Procedure/Trigger/View that is currently executing (end of the stack).</li>" +
		"</ul>" +
	"</html>";

	public static final String   GROUP_NAME       = MainFrame.TCP_GROUP_OBJECT_ACCESS;
	public static final String   GUI_ICON_FILE    = "images/"+CM_NAME+".png";

	public static final int      NEED_SRV_VERSION = 0;
	public static final int      NEED_CE_VERSION  = 0;

	public static final String[] MON_TABLES       = new String[] {"monProcessProcedures"};
	public static final String[] NEED_ROLES       = new String[] {"mon_role"};
	public static final String[] NEED_CONFIG      = new String[] {"enable monitoring=1", "statement statistics active=1"};

	public static final String[] PCT_COLUMNS      = new String[] {};
	public static final String[] DIFF_COLUMNS     = new String[] {};

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

		return new CmProcCallStack(counterController, guiController);
	}

	public CmProcCallStack(ICounterController counterController, IGuiController guiController)
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
		return new CmProcCallStackPanel(this);
	}

	@Override
	public String[] getDependsOnConfigForVersion(Connection conn, int srvVersion, boolean isClusterEnabled)
	{
//		if (srvVersion >= 15700)
//		if (srvVersion >= 1570000)
		if (srvVersion >= Ver.ver(15,7))
			return NEED_CONFIG;

		return new String[] {"enable monitoring=1"};
	}

	@Override
	public List<String> getPkForVersion(Connection conn, int srvVersion, boolean isClusterEnabled)
	{
		// this would mean NO PK and NO DIFF
		return null;
	}

	@Override
	public String getSqlForVersion(Connection conn, int aseVersion, boolean isClusterEnabled)
	{
		String cols1, cols2, cols3;
		cols1 = cols2 = cols3 = "";

		String InstanceID      = "";
		String StatementNumber = "";
		String LineNumber      = "";
		// ase 15.7
		String ExecutionCount = "";
		String CPUTime        = "";
		String ExecutionTime  = "";
		String PhysicalReads  = "";
		String LogicalReads   = "";
		String PhysicalWrites = "";
		String PagesWritten   = "";
		String ase1570_nl     = "";

		if (isClusterEnabled)    InstanceID      = "InstanceID, ";
//		if (aseVersion >= 12530) LineNumber      = "LineNumber, ";
//		if (aseVersion >= 1253000) LineNumber      = "LineNumber, ";
		if (aseVersion >= Ver.ver(12,5,3)) LineNumber      = "LineNumber, ";

//		if (aseVersion >= 15025) StatementNumber = "StatementNumber, ";
//		if (aseVersion >= 1502050) StatementNumber = "StatementNumber, ";
		if (aseVersion >= Ver.ver(15,0,2,5)) StatementNumber = "StatementNumber, ";
		
//		if (aseVersion >= 15700)
//		if (aseVersion >= 1570000)
		if (aseVersion >= Ver.ver(15,7))
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

		cols1 += "SPID, " + InstanceID + "DBName, OwnerName, ObjectName, \n" + 
		         LineNumber + StatementNumber + "ContextID, ObjectType, \n" +
		         ExecutionCount + CPUTime + ExecutionTime + PhysicalReads + LogicalReads + PhysicalWrites + PagesWritten + ase1570_nl +
		         "PlanID, MemUsageKB, CompileDate, KPID, DBID, OwnerUID, ObjectID, \n" +
		         "MaxContextID = convert(int, -1)";
		//"MaxContextID = (select max(ContextID) from master..monProcessProcedures i where o.SPID = i.SPID)";

		String sql = 
			"select " + cols1 + cols2 + cols3 + "\n" +
			"from master..monProcessProcedures o\n" +
			"order by SPID, ContextID desc\n" +
			"";

		return sql;
	}

	/** 
	 * Fill in the MaxContextID column...
	 * basically do SQL: MaxContextID = (select max(ContextID) from monProcessProcedures i where o.SPID = i.SPID)
	 * But this the SQL is not working very good, since the table is changing to fast.
	 */
	@Override
	public void localCalculation(SamplingCnt prevSample, SamplingCnt newSample, SamplingCnt diffData)
	{
		// Where are various columns located in the Vector 
		int pos_SPID = -1, pos_MaxContextID = -1, pos_ContextID = -1;
	
		// Find column Id's
		List<String> colNames = newSample.getColNames();
		if (colNames==null) return;

		for (int colId=0; colId < colNames.size(); colId++) 
		{
			String colName = colNames.get(colId);
			if      (colName.equals("SPID"))         pos_SPID         = colId;
			else if (colName.equals("ContextID"))    pos_ContextID    = colId;
			else if (colName.equals("MaxContextID")) pos_MaxContextID = colId;

			// Noo need to continue, we got all our columns
			if (pos_SPID >= 0 && pos_ContextID >= 0 && pos_MaxContextID >= 0)
				break;
		}

		if (pos_SPID < 0 || pos_ContextID < 0 || pos_MaxContextID < 0)
		{
			_logger.debug("Can't find the position for columns ('SPID'"+pos_SPID+", 'ContextID'="+pos_ContextID+", 'MaxContextID'="+pos_MaxContextID+")");
			return;
		}
		
		// a map that holds <SPID, MaxContextID>
		Map<Integer,Integer> _maxContextIdPerSpid = new HashMap<Integer,Integer>();

		// Loop on all newSample rows, figure out the MAX ContextID for each SPID 
		for (int rowId=0; rowId < newSample.getRowCount(); rowId++) 
		{
			Object o_SPID         = newSample.getValueAt(rowId, pos_SPID);
			Object o_ContextID    = newSample.getValueAt(rowId, pos_ContextID);

			if (o_SPID instanceof Number && o_ContextID instanceof Number)
			{
				Integer spid         = ((Number)o_SPID     ).intValue();
				Integer contextId    = ((Number)o_ContextID).intValue();
				Integer maxContextId = _maxContextIdPerSpid.get(spid);

				if (maxContextId == null)
					maxContextId = 0;

				// write the MAX ContextID in the Map
				_maxContextIdPerSpid.put(spid, Math.max(contextId, maxContextId));
			}
		}

		// Loop on all newSample rows, SET the MaxContextID for each SPID
		for (int rowId=0; rowId < newSample.getRowCount(); rowId++) 
		{
			Object o_SPID = newSample.getValueAt(rowId, pos_SPID);
			
			if (o_SPID instanceof Number)
			{
				Integer spid         = ((Number)o_SPID).intValue();
				Integer maxContextId = _maxContextIdPerSpid.get(spid);

				newSample.setValueAt(maxContextId, rowId, pos_MaxContextID);
			}
		}
	} // end: localCalculation


	/** 
	 * Get number of rows to save/request ddl information for 
	 */
	@Override
	public int getMaxNumOfDdlsToPersist()
	{
		return Integer.MAX_VALUE; // Basically ALL Rows
	}
}
