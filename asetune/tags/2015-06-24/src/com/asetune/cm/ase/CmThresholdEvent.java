package com.asetune.cm.ase;

import java.sql.Connection;

import com.asetune.ICounterController;
import com.asetune.IGuiController;
import com.asetune.cm.CounterSetTemplates;
import com.asetune.cm.CounterSetTemplates.Type;
import com.asetune.cm.CountersModel;
import com.asetune.cm.CountersModelAppend;
import com.asetune.gui.MainFrame;
import com.asetune.utils.Ver;

/**
 * @author Goran Schwarz (goran_schwarz@hotmail.com)
 */
public class CmThresholdEvent
extends CountersModelAppend
{
//	private static Logger        _logger          = Logger.getLogger(CmThresholdEvents.class);
	private static final long    serialVersionUID = 1L;

	public static final String   CM_NAME          = CmThresholdEvent.class.getSimpleName();
	public static final String   SHORT_NAME       = "Resource Governor";
	public static final String   HTML_DESC        = 
		"<html>" +
		"If Resource Governor Limitations has been enforced/exceeded.<br>" +
		"<br>" +
		"<b>To use ASE Resource Governor</b><br>" +
		"<code>sp_configure 'allow resource limits', 1</code><br>" +
		"<br>" +
		"<b>Manage Resource Governor can be done with the following procedures</b><br>" +
		"<code>sp_add_resource_limit</code><br>" +
		"<code>sp_modify_resource_limit</code><br>" +
		"<code>sp_drop_resource_limit</code><br>" +
		"<code>sp_help_resource_limit</code><br>" +
		"</html>";

	public static final String   GROUP_NAME       = MainFrame.TCP_GROUP_SERVER;
	public static final String   GUI_ICON_FILE    = "images/"+CM_NAME+".png";

//	public static final int      NEED_SRV_VERSION = 1600000;
	public static final int      NEED_SRV_VERSION = Ver.ver(16,0);
	public static final int      NEED_CE_VERSION  = 0;

	public static final String[] MON_TABLES       = new String[] {"monThresholdEvent"};
	public static final String[] NEED_ROLES       = new String[] {"mon_role"};
	public static final String[] NEED_CONFIG      = new String[] {"allow resource limits", "enable monitoring=1", "threshold event monitoring=1", "threshold event max messages=500"};

//	public static final String[] PCT_COLUMNS      = new String[] {};
//	public static final String[] DIFF_COLUMNS     = new String[] {"XXXdiffCols"};

//	public static final boolean  NEGATIVE_DIFF_COUNTERS_TO_ZERO = true;
	public static final boolean  IS_SYSTEM_CM                   = true;
	public static final int      DEFAULT_POSTPONE_TIME          = 0;
	public static final int      DEFAULT_QUERY_TIMEOUT          = CountersModel.DEFAULT_sqlQueryTimeout;

	@Override public int     getDefaultPostponeTime()                 { return DEFAULT_POSTPONE_TIME; }
	@Override public int     getDefaultQueryTimeout()                 { return DEFAULT_QUERY_TIMEOUT; }
//	@Override public boolean getDefaultIsNegativeDiffCountersToZero() { return NEGATIVE_DIFF_COUNTERS_TO_ZERO; }
	@Override public Type    getTemplateLevel()                       { return Type.OFF; }

	/**
	 * FACTORY  method to create the object
	 */
	public static CountersModel create(ICounterController counterController, IGuiController guiController)
	{
		if (guiController != null && guiController.hasGUI())
			guiController.splashWindowProgress("Loading: Counter Model '"+CM_NAME+"'");

		return new CmThresholdEvent(counterController, guiController);
	}

	public CmThresholdEvent(ICounterController counterController, IGuiController guiController)
	{
		super( CM_NAME, GROUP_NAME, null, MON_TABLES, NEED_ROLES, NEED_CONFIG, NEED_SRV_VERSION, NEED_CE_VERSION, IS_SYSTEM_CM);

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

//	@Override
//	protected TabularCntrPanel createGui()
//	{
//		return new CmThresholdEventsPanel(this);
//	}

	@Override
	public String[] getDependsOnConfigForVersion(Connection conn, int srvVersion, boolean isClusterEnabled)
	{
		return NEED_CONFIG;
	}

	@Override
	public String getSqlForVersion(Connection conn, int aseVersion, boolean isClusterEnabled)
	{
		String cols1, cols2, cols3;
		cols1 = cols2 = cols3 = "";

		cols1 = "*";
		cols2 = "";
		cols3 = "";

		String sql = 
			"select " + cols1 + cols2 + cols3 + "\n" +
			"from master..monThresholdEvent\n";

		return sql;
	}
}
