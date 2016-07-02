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
public class CmPgDatabase
extends CountersModel
{
//	private static Logger        _logger          = Logger.getLogger(CmPgActivity.class);
	private static final long    serialVersionUID = 1L;

	public static final String   CM_NAME          = CmPgDatabase.class.getSimpleName();
	public static final String   SHORT_NAME       = "Databases";
	public static final String   HTML_DESC        = 
		"<html>" +
		"One row per database, showing database-wide statistics." +
		"</html>";

	public static final String   GROUP_NAME       = MainFrame.TCP_GROUP_SERVER;
	public static final String   GUI_ICON_FILE    = "images/"+CM_NAME+".png";

	public static final int      NEED_SRV_VERSION = 0;
	public static final int      NEED_CE_VERSION  = 0;

	public static final String[] MON_TABLES       = new String[] {"pg_stat_database"};
	public static final String[] NEED_ROLES       = new String[] {};
	public static final String[] NEED_CONFIG      = new String[] {};

	public static final String[] PCT_COLUMNS      = new String[] {};
	public static final String[] DIFF_COLUMNS     = new String[] {
			"numbackends",
			"xact_commit",
			"xact_rollback",
			"blks_read",
			"blks_hit",
			"tup_returned",
			"tup_fetched",
			"tup_inserted",
			"tup_updated",
			"tup_deleted",
			"conflicts",
			"temp_files",
			"temp_bytes",
			"deadlocks",
			"blk_read_time",
			"blk_write_time"
	};
//	RS> Col# Label          JDBC Type Name           Guessed DBMS type
//	RS> ---- -------------- ------------------------ -----------------
//	RS> 1    datid          java.sql.Types.BIGINT    oid              
//	RS> 2    datname        java.sql.Types.VARCHAR   name(2147483647) 
//	RS> 3    numbackends    java.sql.Types.INTEGER   int4             
//	RS> 4    xact_commit    java.sql.Types.BIGINT    int8             
//	RS> 5    xact_rollback  java.sql.Types.BIGINT    int8             
//	RS> 6    blks_read      java.sql.Types.BIGINT    int8             
//	RS> 7    blks_hit       java.sql.Types.BIGINT    int8             
//	RS> 8    tup_returned   java.sql.Types.BIGINT    int8             
//	RS> 9    tup_fetched    java.sql.Types.BIGINT    int8             
//	RS> 10   tup_inserted   java.sql.Types.BIGINT    int8             
//	RS> 11   tup_updated    java.sql.Types.BIGINT    int8             
//	RS> 12   tup_deleted    java.sql.Types.BIGINT    int8             
//	RS> 13   conflicts      java.sql.Types.BIGINT    int8             
//	RS> 14   temp_files     java.sql.Types.BIGINT    int8             
//	RS> 15   temp_bytes     java.sql.Types.BIGINT    int8             
//	RS> 16   deadlocks      java.sql.Types.BIGINT    int8             
//	RS> 17   blk_read_time  java.sql.Types.DOUBLE    float8           
//	RS> 18   blk_write_time java.sql.Types.DOUBLE    float8           
//	RS> 19   stats_reset    java.sql.Types.TIMESTAMP timestamptz      
	
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

		return new CmPgDatabase(counterController, guiController);
	}

	public CmPgDatabase(ICounterController counterController, IGuiController guiController)
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

		pkCols.add("datid");

		return pkCols;
	}

	@Override
	public String getSqlForVersion(Connection conn, int aseVersion, boolean isClusterEnabled)
	{
		return "select * from pg_catalog.pg_stat_database";
	}
}
