package com.asetune.cm.iq;

import java.sql.Connection;
import java.util.LinkedList;
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
 * sa_conn_properties
 * Reports connection property information.
 * Returns the connection ID as Number, and the PropNum, PropName, PropDescription, and Value for each available connection property. Values are returned for all connection properties, database option settings related to connections, and statistics related to connections. Valid properties with NULL values are also returned. 
 * If connidparm is less than zero, then property values for the current connection are returned. If connidparm is not supplied or is NULL, then property values are returned for all connections to the current database. 
 * @author I063869
 *
 */
public class CmSaConnProperties
extends CountersModel
{
//	private static Logger        _logger          = Logger.getLogger(CmAdminWhoSqm.class);
	private static final long    serialVersionUID = 1L;

	public static final String   CM_NAME          = CmSaConnProperties.class.getSimpleName();
	public static final String   SHORT_NAME       = "connection properties (sa)";
	public static final String   HTML_DESC        = 
		"<html>"
		+ "<h4>sa_conn_properties  system procedure</h4>"
		+ "Returns the connection ID as Number, and the PropNum, PropName, PropDescription, "
		+ "<br/>and Value for each available connection property. Values are returned for all connection properties, "
		+ "<br/>database option settings related to connections, and statistics related to connections. "
		+ "<br/>Valid properties with NULL values are also returned. "
		+ "<br/>If connidparm is less than zero, then property values for the current connection are returned. "
		+ "<br/>If connidparm is not supplied or is NULL, then property values are returned for all connections to the current database."
		+ "</html>";

	public static final String   GROUP_NAME       = MainFrameIq.TCP_GROUP_CATALOG;
	public static final String   GUI_ICON_FILE    = "images/"+CM_NAME+".png";

	public static final long     NEED_SRV_VERSION = 0;
	public static final long     NEED_CE_VERSION  = 0;

	public static final String[] MON_TABLES       = new String[] {"sa_conn_properties"};
	public static final String[] NEED_ROLES       = new String[] {};
	public static final String[] NEED_CONFIG      = new String[] {};

	public static final String[] PCT_COLUMNS      = new String[] {};
	public static final String[] DIFF_COLUMNS     = new String[] {"Value"};

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

		return new CmSaConnProperties(counterController, guiController);
	}

	public CmSaConnProperties(ICounterController counterController, IGuiController guiController)
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
			mtd.addTable("sa_conn_properties",  "Reports connection property information.");

			mtd.addColumn("sa_conn_properties", "Number",  
					"<html>Returns the connection ID (a number) for the current connection.</html>");
			mtd.addColumn("sa_conn_properties", "PropNum",  
					"<html>Returns the connection property number.</html>");
			mtd.addColumn("sa_conn_properties", "PropName",  
					"<html>Returns the connection property name.</html>");
			mtd.addColumn("sa_conn_properties", "Value",  
					"<html>Returns the connection property value.</html>");
			mtd.addColumn("sa_conn_properties", "PropDescription",  
					"<html>Returns the connection property description.</html>");
		}
		catch (NameNotFoundException e) {/*ignore*/}
	}

	@Override
	public List<String> getPkForVersion(Connection conn, long srvVersion, boolean isClusterEnabled)
	{
		List <String> pkCols = new LinkedList<String>();

		pkCols.add("ConnHandle");
		pkCols.add("PropNum");
		
		return pkCols;
	}

	@Override
	public String getSqlForVersion(Connection conn, long srvVersion, boolean isClusterEnabled)
	{
		String sql = 
			"select \n" +
			"    ConnHandle = Number, \n" + 
			"    PropNum, \n" +
			"    PropName,  \n" +
			"    Value = CASE \n" + 
			"               WHEN IsNumeric(Value) = 1 THEN convert(numeric(20,5), Value) \n" + 
			"               ELSE null \n" + 
			"            END, \n" +
			"    PropDescription \n" + 
			"from sa_conn_properties()  \n" +
			"where IsNumeric(Value) = 1 \n" +
			"order by 1, 2";

		return sql;
	}
}
