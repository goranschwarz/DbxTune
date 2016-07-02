package com.asetune.cm.postgres;

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

/**
 * @author Goran Schwarz (goran_schwarz@hotmail.com)
 */
public class CmPgStatements
extends CountersModel
{
//	private static Logger        _logger          = Logger.getLogger(CmPgActivity.class);
	private static final long    serialVersionUID = 1L;

	public static final String   CM_NAME          = CmPgStatements.class.getSimpleName();
	public static final String   SHORT_NAME       = "Statemets";
	public static final String   HTML_DESC        = 
		"<html>" +
		"The pg_stat_statements module provides a means for tracking execution statistics of all SQL statements executed by a server." +
		"</html>";

	public static final String   GROUP_NAME       = MainFrame.TCP_GROUP_OBJECT_ACCESS;
	public static final String   GUI_ICON_FILE    = "images/"+CM_NAME+".png";

	public static final int      NEED_SRV_VERSION = 0;
	public static final int      NEED_CE_VERSION  = 0;

	public static final String[] MON_TABLES       = new String[] {"pg_stat_statements"};
	public static final String[] NEED_ROLES       = new String[] {};
	public static final String[] NEED_CONFIG      = new String[] {};

	public static final String[] PCT_COLUMNS      = new String[] {};
	public static final String[] DIFF_COLUMNS     = new String[] {
			"calls",
			"total_time",
			"rows",
			"shared_blks_hit",
			"shared_blks_read",
			"shared_blks_dirtied",
			"shared_blks_written",
			"local_blks_hit",
			"local_blks_read",
			"local_blks_dirtied",
			"local_blks_written",
			"temp_blks_read",
			"temp_blks_written",
			"blk_read_time",
			"blk_write_time"
	};
//	RS> Col# Label               JDBC Type Name         Guessed DBMS type
//	RS> ---- ------------------- ---------------------- -----------------
//	RS> 1    userid              java.sql.Types.BIGINT  oid              
//	RS> 2    dbid                java.sql.Types.BIGINT  oid              
//	RS> 3    queryid             java.sql.Types.BIGINT  int8                //// NOTE introduced in 9.4
//	RS> 4    query               java.sql.Types.VARCHAR text(2147483647) 
//	RS> 5    calls               java.sql.Types.BIGINT  int8             
//	RS> 6    total_time          java.sql.Types.DOUBLE  float8           
//	RS> 7    rows                java.sql.Types.BIGINT  int8             
//	RS> 8    shared_blks_hit     java.sql.Types.BIGINT  int8             
//	RS> 9    shared_blks_read    java.sql.Types.BIGINT  int8             
//	RS> 10   shared_blks_dirtied java.sql.Types.BIGINT  int8             
//	RS> 11   shared_blks_written java.sql.Types.BIGINT  int8             
//	RS> 12   local_blks_hit      java.sql.Types.BIGINT  int8             
//	RS> 13   local_blks_read     java.sql.Types.BIGINT  int8             
//	RS> 14   local_blks_dirtied  java.sql.Types.BIGINT  int8             
//	RS> 15   local_blks_written  java.sql.Types.BIGINT  int8             
//	RS> 16   temp_blks_read      java.sql.Types.BIGINT  int8             
//	RS> 17   temp_blks_written   java.sql.Types.BIGINT  int8             
//	RS> 18   blk_read_time       java.sql.Types.DOUBLE  float8           
//	RS> 19   blk_write_time      java.sql.Types.DOUBLE  float8           

	public static final boolean  NEGATIVE_DIFF_COUNTERS_TO_ZERO = true;
	public static final boolean  IS_SYSTEM_CM                   = true;
	public static final int      DEFAULT_POSTPONE_TIME          = 0;
	public static final int      DEFAULT_QUERY_TIMEOUT          = CountersModel.DEFAULT_sqlQueryTimeout;

	@Override public int     getDefaultPostponeTime()                 { return DEFAULT_POSTPONE_TIME; }
	@Override public int     getDefaultQueryTimeout()                 { return DEFAULT_QUERY_TIMEOUT; }
	@Override public boolean getDefaultIsNegativeDiffCountersToZero() { return NEGATIVE_DIFF_COUNTERS_TO_ZERO; }
	@Override public Type    getTemplateLevel()                       { return Type.MEDIUM; }

	/**
	 * FACTORY  method to create the object
	 */
	public static CountersModel create(ICounterController counterController, IGuiController guiController)
	{
		if (guiController != null && guiController.hasGUI())
			guiController.splashWindowProgress("Loading: Counter Model '"+CM_NAME+"'");

		return new CmPgStatements(counterController, guiController);
	}

	public CmPgStatements(ICounterController counterController, IGuiController guiController)
	{
		super(counterController,
				CM_NAME, GROUP_NAME, /*sql*/null, /*pkList*/null, 
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
//		return new CmActiveStatementsPanel(this);
//	}
	
	@Override
	public List<String> getPkForVersion(Connection conn, int srvVersion, boolean isClusterEnabled)
	{
		List <String> pkCols = new LinkedList<String>();

		pkCols.add("userid");
		pkCols.add("dbid");
		pkCols.add("queryid");

		return pkCols;
	}

	@Override
	public String getSqlForVersion(Connection conn, int aseVersion, boolean isClusterEnabled)
	{
//		return "select * from public.pg_stat_statements";
		return "select * from pg_stat_statements";

// If PostgreSQL version earlier than 9.4, try to emulate the queryid using md5() or something similar
//		select cast(md5(query) as varchar(30)) as ID, char_length(query) as query_length, * from public.pg_stat_statements
	}

	@Override
	public String getToolTipTextOnTableCell(MouseEvent e, String colName, Object cellValue, int modelRow, int modelCol) 
	{
		// query
		if ("query".equals(colName))
		{
			return cellValue == null ? null : toHtmlString(cellValue.toString());
		}
		
		return super.getToolTipTextOnTableCell(e, colName, cellValue, modelRow, modelCol);
	}
	/** add HTML around the string, and translate line breaks into <br> */
	private String toHtmlString(String in)
	{
		String str = StringUtil.makeApproxLineBreak(in, 150, 10, "\n");
		str = str.replaceAll("\\n", "<br>");
		if (in.indexOf("<html>")>=0 || in.indexOf("<HTML>")>=0)
			return str;
		return "<html><pre>" + str + "</pre></html>";
	}
}
