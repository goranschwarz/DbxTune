package com.asetune.cm.mysql;

import java.awt.event.MouseEvent;
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
import com.asetune.config.dict.MySqlVariablesDictionary;
import com.asetune.gui.MainFrame;

/**
 * @author Goran Schwarz (goran_schwarz@hotmail.com)
 */
public class CmUserStatus
extends CountersModel
{
//	private static Logger        _logger          = Logger.getLogger(CmGlobalStatus.class);
	private static final long    serialVersionUID = 1L;

	public static final String   CM_NAME          = CmUserStatus.class.getSimpleName();
	public static final String   SHORT_NAME       = "User Status";
	public static final String   HTML_DESC        = 
		"<html>"
		+ "<h4>User Status</h4>"
		+ "Simply select from performance_schema.status_by_user"
		+ "</html>";

	public static final String   GROUP_NAME       = MainFrame.TCP_GROUP_SERVER;
	public static final String   GUI_ICON_FILE    = "images/"+CM_NAME+".png";

	public static final int      NEED_SRV_VERSION = 0;
	public static final int      NEED_CE_VERSION  = 0;

	public static final String[] MON_TABLES       = new String[] {"status_by_user"};
	public static final String[] NEED_ROLES       = new String[] {};
	public static final String[] NEED_CONFIG      = new String[] {};

	public static final String[] PCT_COLUMNS      = new String[] {};
	public static final String[] DIFF_COLUMNS     = new String[] {"VARIABLE_VALUE"};

	public static final boolean  NEGATIVE_DIFF_COUNTERS_TO_ZERO = true;
	public static final boolean  IS_SYSTEM_CM                   = true;
	public static final int      DEFAULT_POSTPONE_TIME          = 0;
	public static final int      DEFAULT_QUERY_TIMEOUT          = CountersModel.DEFAULT_sqlQueryTimeout;

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

		return new CmUserStatus(counterController, guiController);
	}

	public CmUserStatus(ICounterController counterController, IGuiController guiController)
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
//		return new CmGlobalStatusPanel(this);
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
			mtd.addTable("status_by_user",  "Global status.");

			mtd.addColumn("status_by_user", "USER",           "<html>Name of the user.</html>");
			mtd.addColumn("status_by_user", "VARIABLE_NAME",  "<html>Name of the variable.</html>");
			mtd.addColumn("status_by_user", "VARIABLE_VALUE", "<html>Value of the variable.</html>");
		}
		catch (NameNotFoundException e) {/*ignore*/}
	}

	@Override
	public List<String> getPkForVersion(Connection conn, int srvVersion, boolean isClusterEnabled)
	{
		List <String> pkCols = new LinkedList<String>();

		pkCols.add("USER");
		pkCols.add("VARIABLE_NAME");

		return pkCols;
	}

	@Override
	public String getSqlForVersion(Connection conn, int aseVersion, boolean isClusterEnabled)
	{
		String sql = 
			"select IFNULL(USER, '-system-') as USER \n" +
			"      ,VARIABLE_NAME \n" +
			"      ,cast(VARIABLE_VALUE as signed integer) as VARIABLE_VALUE \n" +
			"from performance_schema.status_by_user \n" +
			"where VARIABLE_VALUE REGEXP '^[[:digit:]]+$' \n";

		return sql;
	}

	@Override
	public String getToolTipTextOnTableCell(MouseEvent e, String colName, Object cellValue, int modelRow, int modelCol) 
	{
		if ("VARIABLE_NAME".equals(colName))
		{
			return cellValue == null ? null : MySqlVariablesDictionary.getInstance().getDescriptionHtml(cellValue.toString());
		}

		if ("VARIABLE_VALUE".equals(colName) )
		{
			int pos_key = findColumn("VARIABLE_NAME");
			if (pos_key >= 0)
			{
				Object cellVal = getValueAt(modelRow, pos_key);
				if (cellVal instanceof String)
				{
					return MySqlVariablesDictionary.getInstance().getDescriptionHtml((String) cellVal);
				}
			}
		}
		
		return super.getToolTipTextOnTableCell(e, colName, cellValue, modelRow, modelCol);
	}
}
