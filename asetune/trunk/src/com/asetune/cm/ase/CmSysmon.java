package com.asetune.cm.ase;

import java.sql.Connection;
import java.util.LinkedList;
import java.util.List;

import com.asetune.ICounterController;
import com.asetune.IGuiController;
import com.asetune.cm.CounterSetTemplates;
import com.asetune.cm.CounterSetTemplates.Type;
import com.asetune.cm.CountersModel;
import com.asetune.cm.ase.gui.CmSysmonPanel;
import com.asetune.gui.MainFrame;
import com.asetune.gui.TabularCntrPanel;
import com.asetune.utils.Ver;

/**
 * @author Goran Schwarz (goran_schwarz@hotmail.com)
 */
public class CmSysmon
extends CountersModel
{
//	private static Logger        _logger          = Logger.getLogger(CmSysmon.class);
	private static final long    serialVersionUID = 1L;

	public static final String   CM_NAME          = CmSysmon.class.getSimpleName();
	public static final String   SHORT_NAME       = "Sysmon Raw";
	public static final String   HTML_DESC        = 
		"<html>" +
		"<p>Just grabs the raw counters used in sp_sysmon.</p>" +
		"NOTE: reuses data from 'Spinlock Sum', so this needs to be running as well.<br>" +
		"For the moment consider this as <b>very experimental</b>." +
		"</html>";

	public static final String   GROUP_NAME       = MainFrame.TCP_GROUP_SERVER;
	public static final String   GUI_ICON_FILE    = "images/"+CM_NAME+".png";

	public static final int      NEED_SRV_VERSION = 0;
	public static final int      NEED_CE_VERSION  = 0;

	public static final String[] MON_TABLES       = new String[] {"sysmonitors"};
	public static final String[] NEED_ROLES       = new String[] {"sa_role"};
	public static final String[] NEED_CONFIG      = new String[] {};

	public static final String[] PCT_COLUMNS      = new String[] {};
	public static final String[] DIFF_COLUMNS     = new String[] {"value"};

	public static final boolean  NEGATIVE_DIFF_COUNTERS_TO_ZERO = true;
	public static final boolean  IS_SYSTEM_CM                   = true;
	public static final int      DEFAULT_POSTPONE_TIME          = 300;
	public static final int      DEFAULT_QUERY_TIMEOUT          = 30;

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

		return new CmSysmon(counterController, guiController);
	}

	public CmSysmon(ICounterController counterController, IGuiController guiController)
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

		addDependsOnCm(CmSpinlockSum.CM_NAME); // CMspinlockSum must have been executed before this cm

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
		return new CmSysmonPanel(this);
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
			pkCols.add("instanceid");

		pkCols.add("field_name");
		pkCols.add("group_name");

		return pkCols;
	}

	@Override
	public String getSqlForVersion(Connection conn, int aseVersion, boolean isClusterEnabled)
	{
		String optGoalPlan = "";
//		if (aseVersion >= 15020)
//		if (aseVersion >= 1502000)
		if (aseVersion >= Ver.ver(15,0,2))
		{
			optGoalPlan = "plan '(use optgoal allrows_dss)' \n";
		}

		String discardSpinlocks = "group_name not in ('spinlock_p_0', 'spinlock_w_0', 'spinlock_s_0')";
//		if (aseVersion >= 15700)
//		if (aseVersion >= 1570000)
		if (aseVersion >= Ver.ver(15,7))
			discardSpinlocks = "group_name not in ('spinlock_p', 'spinlock_w', 'spinlock_s')";

		String sql =   
			"SELECT \n" +
			(isClusterEnabled ? "instanceid, \n" : "") +
			"  field_name = convert(varchar(100),field_name), \n" +
			"  group_name = convert(varchar(30),group_name), \n" +
			"  field_id, \n" +
			"  value, \n" +
			"  description  = convert(varchar(255), description) \n" +
			"FROM master..sysmonitors \n" +
			"WHERE " + discardSpinlocks + " \n" +
			"  AND value > 100 \n" +
			"  OR  group_name = 'config' \n" +
			"ORDER BY group_name, field_name" + (isClusterEnabled ? ", instanceid" : "") + "\n" +
			optGoalPlan;

		return sql;
	}
}
