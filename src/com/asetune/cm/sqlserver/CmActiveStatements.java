package com.asetune.cm.sqlserver;

import java.awt.event.MouseEvent;
import java.sql.Connection;
import java.util.LinkedList;
import java.util.List;

import com.asetune.ICounterController;
import com.asetune.IGuiController;
import com.asetune.cm.CounterSetTemplates;
import com.asetune.cm.CounterSetTemplates.Type;
import com.asetune.cm.CountersModel;
import com.asetune.gui.MainFrame;
import com.asetune.utils.StringUtil;
import com.asetune.utils.XmlFormatter;

/**
 * @author Goran Schwarz (goran_schwarz@hotmail.com)
 */
public class CmActiveStatements
extends CountersModel
{
//	private static Logger        _logger          = Logger.getLogger(CmServiceMemory.class);
	private static final long    serialVersionUID = 1L;

	public static final String   CM_NAME          = CmActiveStatements.class.getSimpleName();
	public static final String   SHORT_NAME       = "Active Statements";
	public static final String   HTML_DESC        = 
		"<html>" +
		"<p>FIXME</p>" +
		"</html>";

	public static final String   GROUP_NAME       = MainFrame.TCP_GROUP_OBJECT_ACCESS;
	public static final String   GUI_ICON_FILE    = "images/"+CM_NAME+".png";

	public static final int      NEED_SRV_VERSION = 0;
	public static final int      NEED_CE_VERSION  = 0;

	public static final String[] MON_TABLES       = new String[] {};
	public static final String[] NEED_ROLES       = new String[] {};
	public static final String[] NEED_CONFIG      = new String[] {};

	public static final String[] PCT_COLUMNS      = new String[] {};
	public static final String[] DIFF_COLUMNS     = new String[] {
		"cpu_time",
		"reads",
		"writes"
		};

	public static final boolean  NEGATIVE_DIFF_COUNTERS_TO_ZERO = false;
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

		return new CmActiveStatements(counterController, guiController);
	}

	public CmActiveStatements(ICounterController counterController, IGuiController guiController)
	{
		super(CM_NAME, GROUP_NAME, /*sql*/null, /*pkList*/null, 
				DIFF_COLUMNS, PCT_COLUMNS, MON_TABLES, 
				NEED_ROLES, NEED_CONFIG, NEED_SRV_VERSION, NEED_CE_VERSION, 
				NEGATIVE_DIFF_COUNTERS_TO_ZERO, IS_SYSTEM_CM, DEFAULT_POSTPONE_TIME);

		setDisplayName(SHORT_NAME);
		setDescription(HTML_DESC);

		setIconFile(GUI_ICON_FILE);

		setShowClearTime(false);

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
	public List<String> getPkForVersion(Connection conn, int srvVersion, boolean isClusterEnabled)
	{
		List <String> pkCols = new LinkedList<String>();

		pkCols.add("session_id");

		return pkCols;
	}

	@Override
	public String getSqlForVersion(Connection conn, int aseVersion, boolean isClusterEnabled)
	{
		String sql =
				"SELECT  \n" +
				"	des.session_id , \n" +
				"	des.status , \n" +
				"	des.login_name , \n" +
				"	des.[HOST_NAME] , \n" +
				"	der.blocking_session_id , \n" +
				"	DB_NAME(der.database_id) AS database_name , \n" +
				"	der.command , \n" +
				"	des.cpu_time , \n" +
				"	des.reads , \n" +
				"	des.writes , \n" +
				"	dec.last_write , \n" +
				"	des.[program_name] , \n" +
				"	der.wait_type , \n" +
				"	der.wait_time , \n" +
				"	der.last_wait_type , \n" +
				"	der.wait_resource , \n" +
				"	CASE des.transaction_isolation_level \n" +
				"		WHEN 0 THEN 'Unspecified' \n" +
				"		WHEN 1 THEN 'ReadUncommitted' \n" +
				"		WHEN 2 THEN 'ReadCommitted' \n" +
				"		WHEN 3 THEN 'Repeatable' \n" +
				"		WHEN 4 THEN 'Serializable' \n" +
				"		WHEN 5 THEN 'Snapshot' \n" +
				"	END AS transaction_isolation_level , \n" +
				"	OBJECT_NAME(dest.objectid, der.database_id) AS OBJECT_NAME , \n" +
				"	SUBSTRING(dest.text, der.statement_start_offset / 2,  \n" +
				"		( CASE WHEN der.statement_end_offset = -1  \n" +
				"		       THEN DATALENGTH(dest.text)  \n" +
				"		       ELSE der.statement_end_offset  \n" +
				"		  END - der.statement_start_offset ) / 2) AS [executing_statement] , \n" +
				"	deqp.query_plan \n" +
				"FROM sys.dm_exec_sessions des  \n" +
				"LEFT JOIN sys.dm_exec_requests der ON des.session_id = der.session_id \n" +
				"LEFT JOIN sys.dm_exec_connections dec ON des.session_id = dec.session_id \n" +
				"CROSS APPLY sys.dm_exec_sql_text(der.sql_handle) dest \n" +
				"CROSS APPLY sys.dm_exec_query_plan(der.plan_handle) deqp \n";

		return sql;
	}






	@Override
	public String getToolTipTextOnTableCell(MouseEvent e, String colName, Object cellValue, int modelRow, int modelCol) 
	{
		if ("executing_statement".equals(colName))
		{
			return cellValue == null ? null : toHtmlString( cellValue.toString() );
		}
		
		if ("query_plan".equals(colName))
		{
			if (cellValue == null)
				return null;
			
			String formatedXml = new XmlFormatter().format(cellValue.toString());
			return toHtmlString( formatedXml );
		}
		
		return super.getToolTipTextOnTableCell(e, colName, cellValue, modelRow, modelCol);
	}
	/** add HTML around the string, and translate line breaks into <br> */
	private String toHtmlString(String in)
	{
		if (in.indexOf("<html>")>=0 || in.indexOf("<HTML>")>=0)
			return in;

		String str = StringUtil.makeApproxLineBreak(in, 150, 10, "\n");
		str = str.replace("<","&lt;").replace(">","&gt;");
		str = str.replaceAll("\\n", "<br>");
		
		return "<html><pre>" + str + "</pre></html>";
	}

}
