package com.asetune.cm.ase;

import java.math.BigDecimal;
import java.sql.Connection;
import java.util.LinkedList;
import java.util.List;

import org.apache.log4j.Logger;

import com.asetune.ICounterController;
import com.asetune.IGuiController;
import com.asetune.TrendGraphDataPoint;
import com.asetune.cm.CounterSetTemplates;
import com.asetune.cm.CounterSetTemplates.Type;
import com.asetune.cm.CountersModel;
import com.asetune.cm.SamplingCnt;
import com.asetune.gui.MainFrame;
import com.asetune.gui.TrendGraph;

/**
 * @author Goran Schwarz (goran_schwarz@hotmail.com)
 */
public class CmEngines
extends CountersModel
{
	private static Logger        _logger          = Logger.getLogger(CmEngines.class);
	private static final long    serialVersionUID = 1L;

	public static final String   CM_NAME          = CmEngines.class.getSimpleName();
	public static final String   SHORT_NAME       = "Engines";
	public static final String   HTML_DESC        = 
		"<html>" +
		"What ASE Server engine is working. <br>" +
		"In here we can also see what engines are doing/checking for IO's" +
		"</html>";

	public static final String   GROUP_NAME       = MainFrame.TCP_GROUP_SERVER;
	public static final String   GUI_ICON_FILE    = "images/"+CM_NAME+".png";

	public static final int      NEED_SRV_VERSION = 0;
	public static final int      NEED_CE_VERSION  = 0;

	public static final String[] MON_TABLES       = new String[] {"monEngine"};
	public static final String[] NEED_ROLES       = new String[] {"sa_role"};
	public static final String[] NEED_CONFIG      = new String[] {"enable monitoring=1"};

	public static final String[] PCT_COLUMNS      = new String[] {"CPUTime", "SystemCPUTime", "UserCPUTime", "IdleCPUTime", "IOCPUTime"};
	public static final String[] DIFF_COLUMNS     = new String[] {
		"CPUTime", "SystemCPUTime", "UserCPUTime", "IdleCPUTime", "IOCPUTime", "Yields", 
		"DiskIOChecks", "DiskIOPolled", "DiskIOCompleted", "ContextSwitches", 
		"HkgcPendingItems", "HkgcOverflows", "HkgcPendingItemsDcomp", "HkgcOverflowsDcomp"};

	public static final boolean  NEGATIVE_DIFF_COUNTERS_TO_ZERO = true;
	public static final boolean  IS_SYSTEM_CM                   = true;
	public static final int      DEFAULT_POSTPONE_TIME          = 0;
	public static final int      DEFAULT_QUERY_TIMEOUT          = CountersModel.DEFAULT_sqlQueryTimeout;

	@Override public int     getDefaultPostponeTime()                 { return DEFAULT_POSTPONE_TIME; }
	@Override public int     getDefaultQueryTimeout()                 { return DEFAULT_QUERY_TIMEOUT; }
	@Override public boolean getDefaultIsNegativeDiffCountersToZero() { return NEGATIVE_DIFF_COUNTERS_TO_ZERO; }
	@Override public Type    getTemplateLevel()                       { return Type.SMALL; }

	/**
	 * FACTORY  method to create the object
	 */
	public static CountersModel create(ICounterController counterController, IGuiController guiController)
	{
		if (guiController != null && guiController.hasGUI())
			guiController.splashWindowProgress("Loading: Counter Model '"+CM_NAME+"'");

		return new CmEngines(counterController, guiController);
	}

	public CmEngines(ICounterController counterController, IGuiController guiController)
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
	
	public static final String   GRAPH_NAME_CPU_SUM = "cpuSum"; //GetCounters.CM_GRAPH_NAME__ENGINE__CPU_SUM;
	public static final String   GRAPH_NAME_CPU_ENG = "cpuEng"; //GetCounters.CM_GRAPH_NAME__ENGINE__CPU_ENG;

	private void addTrendGraphs()
	{
		String[] sumLabels = new String[] { "System+User CPU", "System CPU", "User CPU" };
		String[] engLabels = new String[] { "eng-0" };
		
		addTrendGraphData(GRAPH_NAME_CPU_SUM, new TrendGraphDataPoint(GRAPH_NAME_CPU_SUM, sumLabels));
		addTrendGraphData(GRAPH_NAME_CPU_ENG, new TrendGraphDataPoint(GRAPH_NAME_CPU_ENG, engLabels));

		// if GUI
		if (getGuiController() != null && getGuiController().hasGUI())
		{
			// GRAPH
			TrendGraph tg = null;
			tg = new TrendGraph(GRAPH_NAME_CPU_SUM,
					"CPU Summary", 	                                 // Menu CheckBox text
					"CPU Summary for all Engines (using monEngine)", // Label 
					sumLabels, 
					true, // is Percent Graph
					this, 
					true, // visible at start
					-1);  // minimum height
			addTrendGraph(tg.getName(), tg, true);

			tg = new TrendGraph(GRAPH_NAME_CPU_ENG,
					"CPU per Engine",                       // Menu CheckBox text
					"CPU Usage per Engine (System + User)", // Label 
					engLabels, 
					true, // is Percent Graph
					this, 
					true, // visible at start
					-1);  // minimum height
			addTrendGraph(tg.getName(), tg, true);
		}
	}

	@Override
	public String[] getDependsOnConfigForVersion(Connection conn, int srvVersion, boolean isClusterEnabled)
	{
		return NEED_CONFIG;
	}

	@Override
	public List<String> getPkForVersion(Connection conn, int srvVersion, boolean isClusterEnabled)
	{
		List <String> pkCols = new LinkedList<String>();

		if (isClusterEnabled)
			pkCols.add("InstanceID");

		pkCols.add("EngineNumber");

		return pkCols;
	}

	@Override
	public String getSqlForVersion(Connection conn, int aseVersion, boolean isClusterEnabled)
	{
		String cols1, cols2, cols3;
		cols1 = cols2 = cols3 = "";

		String ThreadID = "";
		if (aseVersion >= 15700)
		{
			ThreadID = "ThreadID, ";
		}

		if (isClusterEnabled)
		{
			cols1 += "InstanceID, ";
		}

		// 15.7.0.1 counters
		String HkgcPendingItemsDcomp = "";
		String HkgcOverflowsDcomp    = "";
		String nl_15701              = "";
		if (aseVersion >= 15701)
		{
			HkgcPendingItemsDcomp = "HkgcPendingItemsDcomp, ";
			HkgcOverflowsDcomp    = "HkgcOverflowsDcomp, ";
			nl_15701              = "\n";
		}
		
		cols1 += "EngineNumber, CurrentKPID, PreviousKPID, CPUTime, SystemCPUTime, UserCPUTime, \n";
		if (aseVersion >= 15500 || (aseVersion >= 15030 && isClusterEnabled) )
			cols1 += "IOCPUTime, ";
		cols1 += "IdleCPUTime, ContextSwitches, Connections, \n";

		cols2 += "";
		cols3 += "ProcessesAffinitied, Status, StartTime, StopTime, AffinitiedToCPU, "+ThreadID+"OSPID";

		if (aseVersion >= 12532)
		{
			cols2 += "Yields, DiskIOChecks, DiskIOPolled, DiskIOCompleted, \n";
		}
		if (aseVersion >= 15025)
		{
			cols2 += "MaxOutstandingIOs, ";
		}
		if (aseVersion >= 15000)
		{
			cols2 += "HkgcMaxQSize, HkgcPendingItems, HkgcHWMItems, HkgcOverflows, \n";
		}
		cols2 += HkgcPendingItemsDcomp + HkgcOverflowsDcomp + nl_15701;

		String sql = 
			"select " + cols1 + cols2 + cols3 + "\n" +
			"from master..monEngine \n" +
			"where Status = 'online' \n" +
			"order by 1,2\n";

		return sql;
	}

	@Override
	public void updateGraphData(TrendGraphDataPoint tgdp)
	{
		int aseVersion = getServerVersion();
		// NOTE: IOCPUTime was introduced in ASE 15.5

		if (GRAPH_NAME_CPU_SUM.equals(tgdp.getName()))
		{
			Double[] dataArray  = new Double[3];
			String[] labelArray = new String[3];
			if (aseVersion >= 15500)
			{
				dataArray  = new Double[4];
				labelArray = new String[4];
			}

			labelArray[0] = "System+User CPU";
			labelArray[1] = "System CPU";
			labelArray[2] = "User CPU";

			dataArray[0] = this.getDiffValueAvg("CPUTime");
			dataArray[1] = this.getDiffValueAvg("SystemCPUTime");
			dataArray[2] = this.getDiffValueAvg("UserCPUTime");

			if (aseVersion >= 15500)
			{
				labelArray[0] = "System+User+IO CPU";

				dataArray[3]  = this.getDiffValueAvg("IOCPUTime");
				labelArray[3] = "IO CPU";

				if (_logger.isDebugEnabled())
					_logger.debug("updateGraphData(cpuSum): CPUTime='"+dataArray[0]+"', SystemCPUTime='"+dataArray[1]+"', UserCPUTime='"+dataArray[2]+"', IoCPUTime='"+dataArray[3]+"'.");
			}
			else
			{
				if (_logger.isDebugEnabled())
					_logger.debug("updateGraphData(cpuSum): CPUTime='"+dataArray[0]+"', SystemCPUTime='"+dataArray[1]+"', UserCPUTime='"+dataArray[2]+"'.");
			}

			// Set the values
			tgdp.setDate(this.getTimestamp());
			tgdp.setLabel(labelArray);
			tgdp.setData(dataArray);
		}

		if (GRAPH_NAME_CPU_ENG.equals(tgdp.getName()))
		{
			// Set label on the TrendGraph if we are above 15.5
			if (aseVersion >= 15500)
			{
				TrendGraph tg = getTrendGraph(tgdp.getName());
				if (tg != null)
					tg.setLabel("CPU Usage per Engine (System + User + IO)");
			}
			
			Double[] engCpuArray = new Double[this.size()];
			String[] engNumArray = new String[engCpuArray.length];
			for (int i = 0; i < engCpuArray.length; i++)
			{
				String instanceId   = null;
				if (isClusterEnabled())
					instanceId = this.getAbsString(i, "InstanceID");
				String engineNumber = this.getAbsString(i, "EngineNumber");

				engCpuArray[i] = this.getDiffValueAsDouble(i, "CPUTime");
				if (instanceId == null)
					engNumArray[i] = "eng-" + engineNumber;
				else
					engNumArray[i] = "eng-" + instanceId + ":" + engineNumber;
				
				// in Cluster Edition the labels will look like 'eng-{InstanceId}:{EngineNumber}'
				// in ASE SMP Version the labels will look like 'eng-{EngineNumber}'
			}

			// Set the values
			tgdp.setDate(this.getTimestamp());
			tgdp.setLabel(engNumArray);
			tgdp.setData(engCpuArray);
		}
	}


	/** 
	 * Compute the CPU times in pct instead of numbers of usage seconds since last sample
	 */
	@Override
	public void localCalculation(SamplingCnt prevSample, SamplingCnt newSample, SamplingCnt diffData)
	{
		int aseVersion = getServerVersion();
		// NOTE: IOCPUTime was introduced in ASE 15.5
		
		// Compute the avgServ column, which is IOTime/(Reads+APFReads+Writes)
		int CPUTime,        SystemCPUTime,        UserCPUTime,        IOCPUTime,        IdleCPUTime;
		int CPUTimeId = -1, SystemCPUTimeId = -1, UserCPUTimeId = -1, IOCPUTimeId = -1, IdleCPUTimeId = -1;
	
		// Find column Id's
		List<String> colNames = diffData.getColNames();
		if (colNames==null) return;
	
		for (int colId=0; colId < colNames.size(); colId++) 
		{
			String colName = colNames.get(colId);
			if      (colName.equals("CPUTime"))       CPUTimeId       = colId;
			else if (colName.equals("SystemCPUTime")) SystemCPUTimeId = colId;
			else if (colName.equals("UserCPUTime"))   UserCPUTimeId   = colId;
			else if (colName.equals("IOCPUTime"))     IOCPUTimeId     = colId;
			else if (colName.equals("IdleCPUTime"))   IdleCPUTimeId   = colId;
		}
	
		// Loop on all diffData rows
		for (int rowId=0; rowId < diffData.getRowCount(); rowId++) 
		{
			CPUTime	=       ((Number)diffData.getValueAt(rowId, CPUTimeId      )).intValue();
			SystemCPUTime = ((Number)diffData.getValueAt(rowId, SystemCPUTimeId)).intValue();
			UserCPUTime =   ((Number)diffData.getValueAt(rowId, UserCPUTimeId  )).intValue();
			IdleCPUTime =   ((Number)diffData.getValueAt(rowId, IdleCPUTimeId  )).intValue();

			IOCPUTime = 0;
			if (aseVersion >= 15500)
			{
				IOCPUTime =   ((Number)diffData.getValueAt(rowId, IOCPUTimeId  )).intValue();
			}

			if (_logger.isDebugEnabled())
				_logger.debug("----CPUTime = "+CPUTime+", SystemCPUTime = "+SystemCPUTime+", UserCPUTime = "+UserCPUTime+", IOCPUTime = "+IOCPUTime+", IdleCPUTime = "+IdleCPUTime);

			// Handle divided by 0... (this happens if a engine goes offline
			BigDecimal calcCPUTime       = null;
			BigDecimal calcSystemCPUTime = null;
			BigDecimal calcUserCPUTime   = null;
			BigDecimal calcIoCPUTime     = null;
			BigDecimal calcIdleCPUTime   = null;

			if( CPUTime == 0 )
			{
				calcCPUTime       = new BigDecimal( 0 );
				calcSystemCPUTime = new BigDecimal( 0 );
				calcUserCPUTime   = new BigDecimal( 0 );
				calcIoCPUTime     = new BigDecimal( 0 );
				calcIdleCPUTime   = new BigDecimal( 0 );
			}
			else
			{
				int sumSystemUserIo = SystemCPUTime + UserCPUTime + IOCPUTime;
				calcCPUTime       = new BigDecimal( ((1.0 * sumSystemUserIo) / CPUTime) * 100 ).setScale(1, BigDecimal.ROUND_HALF_EVEN);
				calcSystemCPUTime = new BigDecimal( ((1.0 * SystemCPUTime  ) / CPUTime) * 100 ).setScale(1, BigDecimal.ROUND_HALF_EVEN);
				calcUserCPUTime   = new BigDecimal( ((1.0 * UserCPUTime    ) / CPUTime) * 100 ).setScale(1, BigDecimal.ROUND_HALF_EVEN);
				calcIoCPUTime     = new BigDecimal( ((1.0 * IOCPUTime      ) / CPUTime) * 100 ).setScale(1, BigDecimal.ROUND_HALF_EVEN);
				calcIdleCPUTime   = new BigDecimal( ((1.0 * IdleCPUTime    ) / CPUTime) * 100 ).setScale(1, BigDecimal.ROUND_HALF_EVEN);
			}

			if (_logger.isDebugEnabled())
				_logger.debug("++++CPUTime = "+calcCPUTime+", SystemCPUTime = "+calcSystemCPUTime+", UserCPUTime = "+calcUserCPUTime+", IoCPUTime = "+calcIoCPUTime+", IdleCPUTime = "+calcIdleCPUTime);
	
			diffData.setValueAt(calcCPUTime,       rowId, CPUTimeId       );
			diffData.setValueAt(calcSystemCPUTime, rowId, SystemCPUTimeId );
			diffData.setValueAt(calcUserCPUTime,   rowId, UserCPUTimeId   );
			diffData.setValueAt(calcIdleCPUTime,   rowId, IdleCPUTimeId   );
	
			if (aseVersion >= 15500)
			{
				diffData.setValueAt(calcIoCPUTime, rowId, IOCPUTimeId);
			}
		}
	}
}