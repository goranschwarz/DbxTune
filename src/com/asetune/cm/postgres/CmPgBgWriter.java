package com.asetune.cm.postgres;

import java.sql.Connection;
import java.util.LinkedList;
import java.util.List;

import com.asetune.ICounterController;
import com.asetune.IGuiController;
import com.asetune.cm.CounterSetTemplates;
import com.asetune.cm.CounterSetTemplates.Type;
import com.asetune.cm.CountersModel;
import com.asetune.gui.MainFrame;

/**
 * @author Goran Schwarz (goran_schwarz@hotmail.com)
 */
public class CmPgBgWriter
extends CountersModel
{
//	private static Logger        _logger          = Logger.getLogger(CmPgActivity.class);
	private static final long    serialVersionUID = 1L;

	public static final String   CM_NAME          = CmPgBgWriter.class.getSimpleName();
	public static final String   SHORT_NAME       = "BG Writer";
	public static final String   HTML_DESC        = 
		"<html>" +
		"One row only, showing statistics about the background writer process's activity." +
		"</html>";

	public static final String   GROUP_NAME       = MainFrame.TCP_GROUP_SERVER;
	public static final String   GUI_ICON_FILE    = "images/"+CM_NAME+".png";

	public static final int      NEED_SRV_VERSION = 0;
	public static final int      NEED_CE_VERSION  = 0;

	public static final String[] MON_TABLES       = new String[] {"pg_stat_bgwriter"};
	public static final String[] NEED_ROLES       = new String[] {};
	public static final String[] NEED_CONFIG      = new String[] {};

	public static final String[] PCT_COLUMNS      = new String[] {};
	public static final String[] DIFF_COLUMNS     = new String[] {
			"checkpoints_timed",
			"checkpoints_req",
			"checkpoint_write_time",
			"checkpoint_sync_time",
			"buffers_checkpoint",
			"buffers_clean",
			"maxwritten_clean",
			"buffers_backend",
			"buffers_backend_fsync",
			"buffers_alloc"
	};
	
//	RS> Col# Label                 JDBC Type Name           Guessed DBMS type Source Table    
//	RS> ---- --------------------- ------------------------ ----------------- ----------------
//	RS> 1    checkpoints_timed     java.sql.Types.BIGINT    int8              pg_stat_bgwriter
//	RS> 2    checkpoints_req       java.sql.Types.BIGINT    int8              pg_stat_bgwriter
//	RS> 3    checkpoint_write_time java.sql.Types.DOUBLE    float8            pg_stat_bgwriter
//	RS> 4    checkpoint_sync_time  java.sql.Types.DOUBLE    float8            pg_stat_bgwriter
//	RS> 5    buffers_checkpoint    java.sql.Types.BIGINT    int8              pg_stat_bgwriter
//	RS> 6    buffers_clean         java.sql.Types.BIGINT    int8              pg_stat_bgwriter
//	RS> 7    maxwritten_clean      java.sql.Types.BIGINT    int8              pg_stat_bgwriter
//	RS> 8    buffers_backend       java.sql.Types.BIGINT    int8              pg_stat_bgwriter
//	RS> 9    buffers_backend_fsync java.sql.Types.BIGINT    int8              pg_stat_bgwriter
//	RS> 10   buffers_alloc         java.sql.Types.BIGINT    int8              pg_stat_bgwriter
//	RS> 11   stats_reset           java.sql.Types.TIMESTAMP timestamptz       pg_stat_bgwriter	
	
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

		return new CmPgBgWriter(counterController, guiController);
	}

	public CmPgBgWriter(ICounterController counterController, IGuiController guiController)
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
	
//	@Override
//	protected TabularCntrPanel createGui()
//	{
//		return new CmActiveStatementsPanel(this);
//	}
	
	@Override
	public List<String> getPkForVersion(Connection conn, int srvVersion, boolean isClusterEnabled)
	{
//		return null;
		List <String> pkCols = new LinkedList<String>();

//		pkCols.add("pk");

		return pkCols;
	}

	@Override
	public String getSqlForVersion(Connection conn, int aseVersion, boolean isClusterEnabled)
	{
//		return "select *, 1 as PK from pg_catalog.pg_stat_bgwriter";
		return "select * from pg_catalog.pg_stat_bgwriter";
	}

	private void addTrendGraphs()
	{
	}
}
