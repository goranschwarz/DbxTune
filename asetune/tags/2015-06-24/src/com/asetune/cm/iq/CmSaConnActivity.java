package com.asetune.cm.iq;

import java.sql.Connection;
import java.util.List;

import javax.naming.NameNotFoundException;

import com.asetune.ICounterController;
import com.asetune.IGuiController;
import com.asetune.cm.CounterSetTemplates;
import com.asetune.cm.CounterSetTemplates.Type;
import com.asetune.cm.CountersModel;
import com.asetune.config.dict.MonTablesDictionary;
import com.asetune.config.dict.MonTablesDictionaryManager;
import com.asetune.gui.MainFrameIq;

/**
 * @author Goran Schwarz (goran_schwarz@hotmail.com)
 */

/**
 * sa_conn_activity system procedure
 * Returns the most recently-prepared SQL statement for each connection to the indicated database on the server.
 * @author I063869
 *
 */
public class CmSaConnActivity
extends CountersModel
{
//	private static Logger        _logger          = Logger.getLogger(CmAdminWhoSqm.class);
	private static final long    serialVersionUID = 1L;

	public static final String   CM_NAME          = CmSaConnActivity.class.getSimpleName();
	public static final String   SHORT_NAME       = "recent SQL statements (sa)";
	public static final String   HTML_DESC        = 
		"<html>" +
		"<h4>sa_conn_activity system procedure</h4>" + 
		"Returns the most recently-prepared SQL statement for each connection to the indicated database on the server." +
		"</html>";

	public static final String   GROUP_NAME       = MainFrameIq.TCP_GROUP_CATALOG;
	public static final String   GUI_ICON_FILE    = "images/"+CM_NAME+".png";

	public static final int      NEED_SRV_VERSION = 0;
	public static final int      NEED_CE_VERSION  = 0;

	public static final String[] MON_TABLES       = new String[] {"sa_conn_activity"};
	public static final String[] NEED_ROLES       = new String[] {};
	public static final String[] NEED_CONFIG      = new String[] {};

	public static final String[] PCT_COLUMNS      = new String[] {};
	public static final String[] DIFF_COLUMNS     = new String[] {};

	public static final boolean  NEGATIVE_DIFF_COUNTERS_TO_ZERO = true;
	public static final boolean  IS_SYSTEM_CM                   = true;
	public static final int      DEFAULT_POSTPONE_TIME          = 0;
	public static final int      DEFAULT_QUERY_TIMEOUT          = 60; //CountersModel.DEFAULT_sqlQueryTimeout;

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

		return new CmSaConnActivity(counterController, guiController);
	}

	public CmSaConnActivity(ICounterController counterController, IGuiController guiController)
	{
		super(CM_NAME, GROUP_NAME, /*sql*/null, /*pkList*/null, 
				DIFF_COLUMNS, PCT_COLUMNS, MON_TABLES, 
				NEED_ROLES, NEED_CONFIG, NEED_SRV_VERSION, NEED_CE_VERSION, 
				NEGATIVE_DIFF_COUNTERS_TO_ZERO, IS_SYSTEM_CM, DEFAULT_POSTPONE_TIME);

		setDisplayName(SHORT_NAME);
		setDescription(HTML_DESC);

		setIconFile(GUI_ICON_FILE);

		setShowClearTime(false);
		setBackgroundDataPollingEnabled(true, false);
		
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
//		return new CmRaSysmonPanel(this);
//	}

	@Override
	public String[] getDependsOnConfigForVersion(Connection conn, int srvVersion, boolean isClusterEnabled)
	{
		return NEED_CONFIG;
	}

	@Override
	public void addMonTableDictForVersion(Connection conn, int srvVersion, boolean isClusterEnabled)
	{
		try 
		{
			MonTablesDictionary mtd = MonTablesDictionaryManager.getInstance();
			mtd.addTable("sa_conn_activity",  "The sp_iqstatus stored procedure provides a variety of IQ status information.");

			mtd.addColumn("sa_conn_activity", "Number",  
					"<html>Returns the connection ID (a number) for the current connection.</html>");
			mtd.addColumn("sa_conn_activity", "Name",  
					"<html>Returns the name of the current connection."
					+ "<br/>Temporary connection names have INT: prepended to the connection name"
					+ "</html>");
			mtd.addColumn("sa_conn_activity", "Userid",  
					"<html>Returns the user ID for the connection.</html>");
			mtd.addColumn("sa_conn_activity", "DBNumber",  
					"<html>Returns the ID number of the database.</html>");
			mtd.addColumn("sa_conn_activity", "LastReqTime",  
					"<html>Returns the time at which the last request for the specified connection started. "
					+ "<br/>This property can return an empty string for internal connections, such as events.</html>");
			mtd.addColumn("sa_conn_activity", "LastStatement",  
					"<html>Returns the most recently prepared SQL statement for the current connection.</html>");
		}
		catch (NameNotFoundException e) {/*ignore*/}
	}

	@Override
	public List<String> getPkForVersion(Connection conn, int srvVersion, boolean isClusterEnabled)
	{
		return null;
//		List <String> pkCols = new LinkedList<String>();
//
//		return pkCols;
	}

	@Override
	public String getSqlForVersion(Connection conn, int aseVersion, boolean isClusterEnabled)
	{
		String sql = "select * from sa_conn_activity()";

		return sql;
	}
}