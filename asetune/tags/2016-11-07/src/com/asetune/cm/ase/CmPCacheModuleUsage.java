package com.asetune.cm.ase;

import java.sql.Connection;
import java.util.LinkedList;
import java.util.List;

import com.asetune.ICounterController;
import com.asetune.IGuiController;
import com.asetune.cm.CounterSetTemplates;
import com.asetune.cm.CounterSetTemplates.Type;
import com.asetune.cm.CountersModel;
import com.asetune.cm.ase.gui.CmPCacheModuleUsagePanel;
import com.asetune.graph.TrendGraphDataPoint;
import com.asetune.gui.MainFrame;
import com.asetune.gui.TabularCntrPanel;
import com.asetune.gui.TrendGraph;
import com.asetune.utils.Ver;

/**
 * @author Goran Schwarz (goran_schwarz@hotmail.com)
 */
public class CmPCacheModuleUsage
extends CountersModel
{
//	private static Logger        _logger          = Logger.getLogger(CmProcCacheModuleUsage.class);
	private static final long    serialVersionUID = 1L;

	public static final String   CM_NAME          = CmPCacheModuleUsage.class.getSimpleName();
	public static final String   SHORT_NAME       = "Proc Cache Module Usage";
	public static final String   HTML_DESC        = 
		"<html>" +
		"What module of the ASE Server is using the 'procedure cache' or 'dynamic memory pool'" +
		"</html>";

	public static final String   GROUP_NAME       = MainFrame.TCP_GROUP_CACHE;
	public static final String   GUI_ICON_FILE    = "images/"+CM_NAME+".png";

//	public static final int      NEED_SRV_VERSION = 15010;
//	public static final int      NEED_SRV_VERSION = 1501000;
	public static final int      NEED_SRV_VERSION = Ver.ver(15,0,1);
	public static final int      NEED_CE_VERSION  = 0;

	public static final String[] MON_TABLES       = new String[] {"monProcedureCacheModuleUsage"};
	public static final String[] NEED_ROLES       = new String[] {"mon_role"};
	public static final String[] NEED_CONFIG      = new String[] {};

	public static final String[] PCT_COLUMNS      = new String[] {};
	public static final String[] DIFF_COLUMNS     = new String[] {"ActiveDiff", "NumPagesReused"};

	public static final boolean  NEGATIVE_DIFF_COUNTERS_TO_ZERO = false;
	public static final boolean  IS_SYSTEM_CM                   = true;
	public static final int      DEFAULT_POSTPONE_TIME          = 0;
	public static final int      DEFAULT_QUERY_TIMEOUT          = CountersModel.DEFAULT_sqlQueryTimeout;

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

		return new CmPCacheModuleUsage(counterController, guiController);
	}

	public CmPCacheModuleUsage(ICounterController counterController, IGuiController guiController)
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
	
	public static final String GRAPH_NAME_MODULE_USAGE = "Graph"; //String x=GetCounters.CM_GRAPH_NAME__PROC_CACHE_MODULE_USAGE__ABS_USAGE;

	private void addTrendGraphs()
	{
		String[] labels = new String[] { "runtime-replaced" };
		
		addTrendGraphData(GRAPH_NAME_MODULE_USAGE, new TrendGraphDataPoint(GRAPH_NAME_MODULE_USAGE, labels));

		// if GUI
		if (getGuiController() != null && getGuiController().hasGUI())
		{
			// GRAPH
			TrendGraph tg = null;
			tg = new TrendGraph(GRAPH_NAME_MODULE_USAGE,
				"Procedure Cache Module Usage", 	                                 // Menu CheckBox text
				"Procedure Cache Module Usage (in page count)", // Label 
				labels, 
				false, // is Percent Graph
				this, 
				false, // visible at start
				0,     // graph is valid from Server Version. 0 = All Versions; >0 = Valid from this version and above 
				300);  // minimum height
			addTrendGraph(tg.getName(), tg, true);
		}
	}

	@Override
	protected TabularCntrPanel createGui()
	{
		return new CmPCacheModuleUsagePanel(this);
	}

	@Override
	public String[] getDependsOnConfigForVersion(Connection conn, int srvVersion, boolean isClusterEnabled)
	{
		return NEED_CONFIG;
	}

	@Override
	public List<String> getPkForVersion(Connection conn, int aseVersion, boolean isClusterEnabled)
	{
		List <String> pkCols = new LinkedList<String>();

		if (isClusterEnabled)
			pkCols.add("InstanceID");

		pkCols.add("ModuleID");

		return pkCols;
	}

	@Override
	public String getSqlForVersion(Connection conn, int aseVersion, boolean isClusterEnabled)
	{
		String InstanceID = "";
//		if (isClusterEnabled && aseVersion >= 15500)
//		if (isClusterEnabled && aseVersion >= 1550000)
		if (isClusterEnabled && aseVersion >= Ver.ver(15,5))
		{
			InstanceID = "InstanceID, ";
		}

		String sql = 
			"select "+InstanceID+"ModuleName, ModuleID, Active, ActiveDiff = Active, HWM, NumPagesReused \n" +
			"from master..monProcedureCacheModuleUsage\n" +
			"order by ModuleID";

		return sql;
	}

	@Override
	public void updateGraphData(TrendGraphDataPoint tgdp)
	{
		if (GRAPH_NAME_MODULE_USAGE.equals(tgdp.getName()))
		{
			// Write 1 "line" for every database
			Double[] dArray = new Double[this.size()];
			String[] lArray = new String[dArray.length];
			for (int i = 0; i < dArray.length; i++)
			{
				lArray[i] = this.getAbsString       (i, "ModuleName");
				dArray[i] = this.getAbsValueAsDouble(i, "Active");
			}

			// Set the values
			tgdp.setDate(this.getTimestamp());
			tgdp.setLabel(lArray);
			tgdp.setData(dArray);
		}
	}
}
