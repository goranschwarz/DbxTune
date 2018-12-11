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
 * Reports connection property information.
 * @author I063869
 *
 */
public class CmSaConnInfo
extends CountersModel
{
//	private static Logger        _logger          = Logger.getLogger(CmAdminWhoSqm.class);
	private static final long    serialVersionUID = 1L;

	public static final String   CM_NAME          = CmSaConnInfo.class.getSimpleName();
	public static final String   SHORT_NAME       = "connection info (sa)";
	public static final String   HTML_DESC        = 
		"<html>" +
		"<h4>sa_conn_info system procedure</h4>" + 
		"Reports connection property information." +
		"</html>";

	public static final String   GROUP_NAME       = MainFrameIq.TCP_GROUP_CATALOG;
	public static final String   GUI_ICON_FILE    = "images/"+CM_NAME+".png";

	public static final long     NEED_SRV_VERSION = 0;
	public static final long     NEED_CE_VERSION  = 0;

	public static final String[] MON_TABLES       = new String[] {"sa_conn_info"};
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

		return new CmSaConnInfo(counterController, guiController);
	}

	public CmSaConnInfo(ICounterController counterController, IGuiController guiController)
	{
		super(counterController,
				CM_NAME, GROUP_NAME, /*sql*/null, /*pkList*/null, 
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
	public String[] getDependsOnConfigForVersion(Connection conn, long srvVersion, boolean isClusterEnabled)
	{
		return NEED_CONFIG;
	}

	@Override
	public void addMonTableDictForVersion(Connection conn, long srvVersion, boolean isClusterEnabled)
	{
		try 
		{
			MonTablesDictionary mtd = MonTablesDictionaryManager.getInstance();
			mtd.addTable("sa_conn_info",  "Reports connection property information.");
			
			mtd.addColumn("sa_conn_info","Number",
					"<html>Returns the connection ID (a number) for the current connection.</html>");
			mtd.addColumn("sa_conn_info","Name",
					"<html>Returns the connection ID (a number) for the current connection. "
					+ "<br/>Temporary connection names have INT: prepended to the connection name.</html>");
			mtd.addColumn("sa_conn_info","Userid",
					"<html>Returns the user ID for the connection.</html>");
			mtd.addColumn("sa_conn_info","DBNumber",
					"<html>Returns the ID number of the database.</html>");
			mtd.addColumn("sa_conn_info","LastReqTime",
					"<html>Returns the time at which the last request for the specified connection started. "
					+ "<br/>This property can return an empty string for internal connections, such as events.</html>");
			mtd.addColumn("sa_conn_info","ReqType",
					"<html>Returns the type of the last request. "
					+ "<br/>If a connection has been cached by connection pooling, its ReqType value is CONNECT_POOL_CACHE.</html>");
			mtd.addColumn("sa_conn_info","CommLink",
					"<html>Returns the communication link for the connection. "
					+ "<br/>This is one of the network protocols supported by SQL Anywhere, or local for a same-computer connection.</html>");
			mtd.addColumn("sa_conn_info","NodeAddr",
					"<html>Returns the address of the client in a client/server connection.</html>");
			mtd.addColumn("sa_conn_info","ClientPort",
					"<html>Returns the client's TCP/IP port number or 0 if the connection isn't a TCP/IP connection.</html>");
			mtd.addColumn("sa_conn_info","ServerPort",
					"<html>Returns the database server's TCP/IP port number or 0.</html>");
			mtd.addColumn("sa_conn_info","BlockedOn",
					"<html>Returns zero if the current connection isn't blocked, or if it is blocked, "
					+ "<br/>the connection number on which the connection is blocked because of a locking conflict.</html>");
			mtd.addColumn("sa_conn_info","LockRowID",
					"<html>Returns the identifier of the locked row. LockRowID is NULL if the connection "
					+ "<br/>is not waiting on a lock associated with a row (that is, it is not waiting on a lock, "
					+ "<br/>or it is waiting on a lock that has no associated row).</html>");
			mtd.addColumn("sa_conn_info","LockIndexID",
					"<html>Returns the identifier of the locked index. "
					+ "<br/>LockIndexID is -1 if the lock is associated with all indexes on the table in LockTable. "
					+ "<br/>LockIndexID is NULL if the connection is not waiting on a lock associated with an index "
					+ "<br/>(that is, it is not waiting on a lock, or it is waiting on a lock that has no associated index).</html>");
			mtd.addColumn("sa_conn_info","LockTable",
					"<html>Returns the name of the table associated with a lock if the connection is currently waiting for a lock. "
					+ "<br/>Otherwise, LockTable returns an empty string.</html>");
			mtd.addColumn("sa_conn_info","UncommitOps",
					"<html>Returns the number of uncommitted operations.</html>");
			mtd.addColumn("sa_conn_info","ParentConnection",
					"<html>Returns the connection ID of the connection that created a temporary connection "
					+ "<br/>to perform a database operation (such as performing a backup or creating a database). "
					+ "<br/>For other types of connections, this property returns NULL.</html>");

		}
		catch (NameNotFoundException e) {/*ignore*/}
	}

	@Override
	public List<String> getPkForVersion(Connection conn, long srvVersion, boolean isClusterEnabled)
	{
		return null;
//		List <String> pkCols = new LinkedList<String>();
//
//		return pkCols;
	}

	@Override
	public String getSqlForVersion(Connection conn, long srvVersion, boolean isClusterEnabled)
	{
		String sql = "select * from sa_conn_info()";

		return sql;
	}
}
