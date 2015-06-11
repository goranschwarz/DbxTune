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
import com.asetune.gui.MainFrame;

/**
 * @author Goran Schwarz (goran_schwarz@hotmail.com)
 */

/**
 * sp_iqlocks procedure
 * Shows information about locks in the database, for both the IQ store and the catalog store
 * 
 * @author I063869
 *
 */
public class CmIqLocks
extends CountersModel
{
//	private static Logger        _logger          = Logger.getLogger(CmAdminWhoSqm.class);
	private static final long    serialVersionUID = 1L;

	public static final String   CM_NAME          = CmIqLocks.class.getSimpleName();
	public static final String   SHORT_NAME       = "locks";
	public static final String   HTML_DESC        = 
		"<html>" +
		"<h4>sp_iqlocks</h4>"
		+ "Shows information about locks in the database, for both the IQ store and the catalog store" +
		"</html>";

	public static final String   GROUP_NAME       = MainFrame.TCP_GROUP_SERVER;
	public static final String   GUI_ICON_FILE    = "images/"+CM_NAME+".png";

	public static final int      NEED_SRV_VERSION = 0;
	public static final int      NEED_CE_VERSION  = 0;

	public static final String[] MON_TABLES       = new String[] {"sp_iqlocks"};
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

		return new CmIqLocks(counterController, guiController);
	}

	public CmIqLocks(ICounterController counterController, IGuiController guiController)
	{
		super(CM_NAME, GROUP_NAME, /*sql*/null, /*pkList*/null, 
				DIFF_COLUMNS, PCT_COLUMNS, MON_TABLES, 
				NEED_ROLES, NEED_CONFIG, NEED_SRV_VERSION, NEED_CE_VERSION, 
				NEGATIVE_DIFF_COUNTERS_TO_ZERO, IS_SYSTEM_CM, DEFAULT_POSTPONE_TIME);

		setDisplayName(SHORT_NAME);
		setDescription(HTML_DESC);

		setIconFile(GUI_ICON_FILE);

		setShowClearTime(false);
		setBackgroundDataPollingEnabled(false, false);
		
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
			mtd.addTable("sp_iqlocks",  "Shows information about locks in the database, for both the IQ store and the catalog store.");

			mtd.addColumn("sp_iqlocks", "conn_name",  "<html>The name of the current connection</html>");
			mtd.addColumn("sp_iqlocks", "conn_id",  "<html>Connection ID that has the lock.</html>");
			mtd.addColumn("sp_iqlocks", "user_id",  "<html>User associated with this connection ID.</html>");
			mtd.addColumn("sp_iqlocks", "table_type",  "<html>The type of table. This type is either BASE for a table, GLBTMP for global temporary table, or MVIEW for a materialized view. " +
			                                    "<br/>Materialized views are only supported for SQL Anywhere tables in the IQ catalog store.</html>");
			mtd.addColumn("sp_iqlocks", "creator",  "<html>The owner of the table.</html>");
			mtd.addColumn("sp_iqlocks", "table_name",  "<html>Table on which the lock is held.</html>");
			mtd.addColumn("sp_iqlocks", "index_id",  "<html>The index ID or NULL.</html>");		
			mtd.addColumn("sp_iqlocks", "lock_class",  "<html>String of characters indicating the type of lock:"
					+ "<ul><li>S – share."
					+ "<li>SW – share and write."
					+ "<li>EW – exclusive and write."
					+ "<li>E – exclusive."
					+ "<li>P – phantom."
					+ "<li>A – antiphantom."
					+ "<li>W – write.</ul>"
					+ "All locks listed have one of S, E, EW, or SW, and may also have P, A, or both. Phantom and antiphantom locks also have a qualifier of T or *:"
					+ "<ul><li>T – the lock is with respect to a sequential scan."
					+ "<li>* – the lock is with respect to all scans."
					+ "<li>nnn – Index number; the lock is with respect to a particular index."
					+ "Sybase IQ obtains a share lock before a write lock. If a connection has exclusive lock, share lock does not appear. For write locks, if a connection has all-exclusive, share, and write locks, it is EW."
					+ "</html>");
			mtd.addColumn("sp_iqlocks", "lock_duration",  "<html>The duration of the lock. One of Transaction, Position, or Connection.</html>");
			mtd.addColumn("sp_iqlocks", "lock_type",  "<html>Value identifying the lock (dependent on the lock class)</html>");
			mtd.addColumn("sp_iqlocks", "row_identifier",  "<html>The identifier for the row or NULL.</html>");
		}
		catch (NameNotFoundException e) {/*ignore*/}
	}

	@Override
	public List<String> getPkForVersion(Connection conn, int srvVersion, boolean isClusterEnabled)
	{
		return null;

//		List <String> pkCols = new LinkedList<String>();
//
//		pkCols.add("conn_id");
//
//		return pkCols;
	}

	@Override
	public String getSqlForVersion(Connection conn, int aseVersion, boolean isClusterEnabled)
	{
		String sql = "select * from sp_iqlocks()";

		return sql;
	}
}
