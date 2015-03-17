package com.asetune.cm.rs;

import java.sql.Connection;
import java.util.LinkedList;
import java.util.List;

import javax.naming.NameNotFoundException;

import com.asetune.ICounterController;
import com.asetune.IGuiController;
import com.asetune.MonTablesDictionary;
import com.asetune.cm.CounterSetTemplates;
import com.asetune.cm.CounterSetTemplates.Type;
import com.asetune.cm.CountersModel;
import com.asetune.gui.MainFrame;

/**
 * @author Goran Schwarz (goran_schwarz@hotmail.com)
 */
public class CmAdminWhoSqm
extends CountersModel
{
//	private static Logger        _logger          = Logger.getLogger(CmAdminWhoSqm.class);
	private static final long    serialVersionUID = 1L;

	public static final String   CM_NAME          = CmAdminWhoSqm.class.getSimpleName();
	public static final String   SHORT_NAME       = "SQM";
	public static final String   HTML_DESC        = 
		"<html>" +
		"<p>Stable Queue Manager Statistics</p>" +
		"</html>";

	public static final String   GROUP_NAME       = MainFrame.TCP_GROUP_SERVER;
	public static final String   GUI_ICON_FILE    = "images/"+CM_NAME+".png";

	public static final int      NEED_SRV_VERSION = 0;
	public static final int      NEED_CE_VERSION  = 0;

	public static final String[] MON_TABLES       = new String[] {"sqm"};
	public static final String[] NEED_ROLES       = new String[] {};
	public static final String[] NEED_CONFIG      = new String[] {};

	public static final String[] PCT_COLUMNS      = new String[] {};
	public static final String[] DIFF_COLUMNS     = new String[] {
		"Duplicates",
		"Writes",
		"Reads",
		"Bytes",
		"B Writes",
		"B Filled",
		"B Reads",
		"B Cache"
//		"Readers",
//		"Truncs"
		};

	public static final boolean  NEGATIVE_DIFF_COUNTERS_TO_ZERO = true;
	public static final boolean  IS_SYSTEM_CM                   = true;
	public static final int      DEFAULT_POSTPONE_TIME          = 0;
	public static final int      DEFAULT_QUERY_TIMEOUT          = CountersModel.DEFAULT_sqlQueryTimeout;;

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

		return new CmAdminWhoSqm(counterController, guiController);
	}

	public CmAdminWhoSqm(ICounterController counterController, IGuiController guiController)
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
			MonTablesDictionary mtd = MonTablesDictionary.getInstance();
			mtd.addTable("sqm",  "");

			mtd.addColumn("sqm", "Spid",            "<html>RepServer internal <i>thread id</i></html>");
			mtd.addColumn("sqm", "State",           "<html>FIXME: State</html>");
			mtd.addColumn("sqm", "Info",            "<html>FIXME: Info</html>");
			mtd.addColumn("sqm", "Duplicates",      "<html>FIXME: Duplicates</html>");
			mtd.addColumn("sqm", "Writes",          "<html>FIXME: Writes</html>");
			mtd.addColumn("sqm", "Reads",           "<html>FIXME: Reads</html>");
			mtd.addColumn("sqm", "Bytes",           "<html>FIXME: Bytes</html>");
			mtd.addColumn("sqm", "B Writes",        "<html>FIXME: B Writes</html>");
			mtd.addColumn("sqm", "B Filled",        "<html>FIXME: B Filled</html>");
			mtd.addColumn("sqm", "B Reads",         "<html>FIXME: B Reads</html>");
			mtd.addColumn("sqm", "B Cache",         "<html>FIXME: B Cache</html>");
			mtd.addColumn("sqm", "Save_Int:Seg",    "<html>FIXME: Save_Int:Seg</html>");
			mtd.addColumn("sqm", "First Seg.Block", "<html>FIXME: First Seg.Block</html>");
			mtd.addColumn("sqm", "Last Seg.Block",  "<html>FIXME: Last Seg.Block</html>");
			mtd.addColumn("sqm", "Next Read",       "<html>FIXME: Next Read</html>");
			mtd.addColumn("sqm", "Readers",         "<html>FIXME: Readers</html>");
			mtd.addColumn("sqm", "Truncs",          "<html>FIXME: Truncs</html>");
			mtd.addColumn("sqm", "Loss Status",     "<html>FIXME: Loss Status</html>");
		}
		catch (NameNotFoundException e) {/*ignore*/}
	}

	@Override
	public List<String> getPkForVersion(Connection conn, int srvVersion, boolean isClusterEnabled)
	{
		List <String> pkCols = new LinkedList<String>();

		pkCols.add("Spid");

		return pkCols;
	}

	@Override
	public String getSqlForVersion(Connection conn, int srvVersion, boolean isClusterEnabled)
	{
		String sql = "admin who, sqm, no_trunc ";
		return sql;
	}
}
