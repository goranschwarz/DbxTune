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
public class CmAdminWhoDist
extends CountersModel
{
//	private static Logger        _logger          = Logger.getLogger(CmAdminWhoDist.class);
	private static final long    serialVersionUID = 1L;

	public static final String   CM_NAME          = CmAdminWhoDist.class.getSimpleName();
	public static final String   SHORT_NAME       = "Dist";
	public static final String   HTML_DESC        = 
		"<html>" +
		"<p>Distributor Statistics</p>" +
		"</html>";

	public static final String   GROUP_NAME       = MainFrame.TCP_GROUP_SERVER;
	public static final String   GUI_ICON_FILE    = "images/"+CM_NAME+".png";

	public static final int      NEED_SRV_VERSION = 0;
	public static final int      NEED_CE_VERSION  = 0;

	public static final String[] MON_TABLES       = new String[] {"dist"};
	public static final String[] NEED_ROLES       = new String[] {};
	public static final String[] NEED_CONFIG      = new String[] {};

	public static final String[] PCT_COLUMNS      = new String[] {};
	public static final String[] DIFF_COLUMNS     = new String[] {
		"PendingCmds",
//		"SqtBlocked",
		"Duplicates",
		"TransProcessed",
		"CmdsProcessed",
		"MaintUserCmds",
		"NoRepdefCmds",
		"CmdsIgnored",
		"CmdMarkers",
		"RSTicket",
		"SqtMaxCache",
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

		return new CmAdminWhoDist(counterController, guiController);
	}

	public CmAdminWhoDist(ICounterController counterController, IGuiController guiController)
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
			mtd.addTable("dist",  "");

			mtd.addColumn("dist", "Spid",           "<html>RepServer internal <i>thread id</i></html>");
			mtd.addColumn("dist", "State",          "<html>FIXME: State</html>");
			mtd.addColumn("dist", "Info",           "<html>FIXME: Info</html>");
			mtd.addColumn("dist", "PrimarySite",    "<html>FIXME: PrimarySite</html>");
			mtd.addColumn("dist", "Type",           "<html>FIXME: Type</html>");
			mtd.addColumn("dist", "Status",         "<html>FIXME: Status</html>");
			mtd.addColumn("dist", "PendingCmds",    "<html>FIXME: PendingCmds</html>");
			mtd.addColumn("dist", "SqtBlocked",     "<html>FIXME: SqtBlocked</html>");
			mtd.addColumn("dist", "Duplicates",     "<html>FIXME: Duplicates</html>");
			mtd.addColumn("dist", "TransProcessed", "<html>FIXME: TransProcessed</html>");
			mtd.addColumn("dist", "CmdsProcessed",  "<html>FIXME: CmdsProcessed</html>");
			mtd.addColumn("dist", "MaintUserCmds",  "<html>FIXME: MaintUserCmds</html>");
			mtd.addColumn("dist", "NoRepdefCmds",   "<html>FIXME: NoRepdefCmds</html>");
			mtd.addColumn("dist", "CmdsIgnored",    "<html>FIXME: CmdsIgnored</html>");
			mtd.addColumn("dist", "CmdMarkers",     "<html>FIXME: CmdMarkers</html>");
			mtd.addColumn("dist", "RSTicket",       "<html>FIXME: RSTicket</html>");
			mtd.addColumn("dist", "SqtMaxCache",    "<html>FIXME: SqtMaxCache</html>");
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
		String sql = "admin who, dist, no_trunc ";
		return sql;
	}
}
