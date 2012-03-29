package com.asetune.cm.ase;

import java.sql.Connection;
import java.util.LinkedList;
import java.util.List;

import com.asetune.ICounterController;
import com.asetune.IGuiController;
import com.asetune.cm.CountersModel;
import com.asetune.cm.ase.gui.CmPCacheMemoryUsagePanel;
import com.asetune.gui.MainFrame;
import com.asetune.gui.TabularCntrPanel;

/**
 * @author Goran Schwarz (goran_schwarz@hotmail.com)
 */
public class CmPCacheMemoryUsage
extends CountersModel
{
//	private static Logger        _logger          = Logger.getLogger(CmProcCacheMemoryUsage.class);
	private static final long    serialVersionUID = 1L;

	public static final String   CM_NAME          = CmPCacheMemoryUsage.class.getSimpleName();
	public static final String   SHORT_NAME       = "Proc Cache Memory Usage";
	public static final String   HTML_DESC        = 
		"<html>" +
		"What module and what 'part' of the modules are using the 'procedure cache' or 'dynamic memory pool'." +
		"</html>";

	public static final String   GROUP_NAME       = MainFrame.TCP_GROUP_CACHE;
	public static final String   GUI_ICON_FILE    = "images/"+CM_NAME+".png";

	public static final int      NEED_SRV_VERSION = 15010;
	public static final int      NEED_CE_VERSION  = 0;

	public static final String[] MON_TABLES       = new String[] {"monProcedureCacheMemoryUsage", "monProcedureCacheModuleUsage"};
	public static final String[] NEED_ROLES       = new String[] {"sa_role"};
	public static final String[] NEED_CONFIG      = new String[] {};

	public static final String[] PCT_COLUMNS      = new String[] {};
	public static final String[] DIFF_COLUMNS     = new String[] {"ActiveDiff", "NumReuseCaused"};

	public static final boolean  NEGATIVE_DIFF_COUNTERS_TO_ZERO = false;
	public static final boolean  IS_SYSTEM_CM                   = true;
	public static final int      DEFAULT_POSTPONE_TIME          = 0;
	public static final int      DEFAULT_QUERY_TIMEOUT          = CountersModel.DEFAULT_sqlQueryTimeout;

	/**
	 * FACTORY  method to create the object
	 */
	public static CountersModel create(ICounterController counterController, IGuiController guiController)
	{
		if (guiController != null && guiController.hasGUI())
			guiController.splashWindowProgress("Loading: Counter Model '"+CM_NAME+"'");

		return new CmPCacheMemoryUsage(counterController, guiController);
	}

	public CmPCacheMemoryUsage(ICounterController counterController, IGuiController guiController)
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
		return new CmPCacheMemoryUsagePanel(this);
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
		pkCols.add("AllocatorID");

		return pkCols;
	}

	@Override
	public String getSqlForVersion(Connection conn, int aseVersion, boolean isClusterEnabled)
	{
		String optGoalPlan = "";
		if (aseVersion >= 15020)
		{
			optGoalPlan = "plan '(use optgoal allrows_dss)' \n";
		}

		//String cols1, cols2, cols3;
		//cols1 = cols2 = cols3 = "";

		String InstanceID     = "";
		String InstanceIDJoin = "";
		if (isClusterEnabled && aseVersion >= 15500)
		{
			InstanceID     = "M.InstanceID, ";
			InstanceIDJoin = "  and M.InstanceID *= C.InstanceID \n";
		}

		String sql = 
			"select M.ModuleName, "+InstanceID+"M.ModuleID, \n" +
			"       AllocatorName = isnull(C.AllocatorName, '-AT-MODULE-LEVEL-'), \n" +
			"       AllocatorID   = isnull(C.AllocatorID,   -1), \n" +
			"       Active        = isnull(C.Active,        M.Active), \n" +
			"       ActiveDiff    = isnull(C.Active,        M.Active), \n" +
			"       HWM           = isnull(C.HWM,           M.HWM), \n" +
			"       ChunkHWM      = isnull(C.ChunkHWM,      -1), \n" +
			"       NumReuseCaused= isnull(C.NumReuseCaused,-1) \n" +
			"from master..monProcedureCacheModuleUsage M, master..monProcedureCacheMemoryUsage C \n" +
			"where M.ModuleID *= C.ModuleID \n" +
			InstanceIDJoin +
			"order by M.ModuleID, C.AllocatorID \n" +
			optGoalPlan;

		return sql;
	}
}
