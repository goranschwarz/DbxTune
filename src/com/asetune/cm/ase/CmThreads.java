//XXX* to config dlg * select * from monThreadPool		--Provides information on the thread pools within the system	
//* to server     * select * from monThread			--Provides information on ASE threads	
//* to disk       * select * from monIOController		--Provides information on I/O controllers	
//* ?  server     * select * from monWorkQueue		--Provides information on work queues	
//* not ?? proces * select * from monTask			--Provides information on tasks subsystem	
//* not           * select * from monServiceTask		--Provides information on service task bindings	
package com.asetune.cm.ase;

import java.sql.Connection;
import java.util.LinkedList;
import java.util.List;

import com.asetune.ICounterController;
import com.asetune.IGuiController;
import com.asetune.cm.CountersModel;
import com.asetune.gui.MainFrame;

/**
 * @author Goran Schwarz (goran_schwarz@hotmail.com)
 */
public class CmThreads
extends CountersModel
{
//	private static Logger        _logger          = Logger.getLogger(CmThreads.class);
	private static final long    serialVersionUID = 1L;

	public static final String   CM_NAME          = CmThreads.class.getSimpleName();
	public static final String   SHORT_NAME       = "Threads";
	public static final String   HTML_DESC        = 
		"<html>" +
		"Provides information on ASE threads" +
		"</html>";

	public static final String   GROUP_NAME       = MainFrame.TCP_GROUP_SERVER;
	public static final String   GUI_ICON_FILE    = "images/"+CM_NAME+".png";

	public static final int      NEED_SRV_VERSION = 15700;
	public static final int      NEED_CE_VERSION  = 0;

	public static final String[] MON_TABLES       = new String[] {"monThread"};
	public static final String[] NEED_ROLES       = new String[] {"sa_role"};
	public static final String[] NEED_CONFIG      = new String[] {};

	public static final String[] PCT_COLUMNS      = new String[] {};
	public static final String[] DIFF_COLUMNS     = new String[] {
		"TaskRuns", "TotalTicks", "IdleTicks", "SleepTicks", "BusyTicks", 
		"UserTime", "SystemTime", 
		"MinorFaults", "MajorFaults", 
		"VoluntaryCtxtSwitches", "NonVoluntaryCtxtSwitches"};

	public static final boolean  NEGATIVE_DIFF_COUNTERS_TO_ZERO = true;
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

		return new CmThreads(counterController, guiController);
	}

	/**
	 * Constructor
	 */
	public CmThreads(ICounterController counterController, IGuiController guiController)
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
	public String[] getDependsOnConfigForVersion(Connection conn, int srvVersion, boolean isClusterEnabled)
	{
		return NEED_CONFIG;
	}

	@Override
	public List<String> getPkForVersion(Connection conn, int srvVersion, boolean isClusterEnabled)
	{
		List <String> pkCols = new LinkedList<String>();

		pkCols.add("InstanceID");
		pkCols.add("ThreadID");

		return pkCols;
	}

	@Override
	public String getSqlForVersion(Connection conn, int aseVersion, boolean isClusterEnabled)
	{
		String sql = "select * from master..monThread";
		return sql;
	}
}
