package com.asetune.cm.ase;

import java.sql.Connection;
import java.sql.ResultSetMetaData;
import java.util.LinkedList;
import java.util.List;

import javax.naming.NameNotFoundException;

import com.asetune.ICounterController;
import com.asetune.IGuiController;
import com.asetune.cm.CmSybMessageHandler;
import com.asetune.cm.CounterSample;
import com.asetune.cm.CounterSetTemplates;
import com.asetune.cm.CounterSetTemplates.Type;
import com.asetune.cm.CountersModel;
import com.asetune.cm.ase.gui.CmQpMetricsPanel;
import com.asetune.cm.sql.VersionInfo;
import com.asetune.config.dict.MonTablesDictionary;
import com.asetune.gui.MainFrame;
import com.asetune.gui.TabularCntrPanel;
import com.asetune.sql.ResultSetMetaDataChangable;
import com.asetune.utils.AseConnectionUtils;
import com.asetune.utils.Ver;

/**
 * @author Goran Schwarz (goran_schwarz@hotmail.com)
 */
public class CmQpMetrics
extends CountersModel
{
//	private static Logger        _logger          = Logger.getLogger(CmQpMetrics.class);
	private static final long    serialVersionUID = 1L;

	public static final String   CM_NAME          = CmQpMetrics.class.getSimpleName();
	public static final String   SHORT_NAME       = "QP Metrics";
	public static final String   HTML_DESC        = 
		"<html>" +
		"Query Processing Metrics<br>" +
		"<br>" +
		"<b>Note:</b> <code>sp_configure 'enable metrics capture'</code>, must be enabled." +
		"</html>";

	public static final String   GROUP_NAME       = MainFrame.TCP_GROUP_OBJECT_ACCESS;
	public static final String   GUI_ICON_FILE    = "images/"+CM_NAME+".png";

//	public static final int      NEED_SRV_VERSION = 15020;
//	public static final int      NEED_SRV_VERSION = 1502000;
	public static final int      NEED_SRV_VERSION = Ver.ver(15,0,2);
	public static final int      NEED_CE_VERSION  = 0;

	public static final String[] MON_TABLES       = new String[] {"sysquerymetrics"};
	public static final String[] NEED_ROLES       = new String[] {"sa_role"};
	public static final String[] NEED_CONFIG      = new String[] {"enable metrics capture"};

	public static final String[] PCT_COLUMNS      = new String[] {};
	public static final String[] DIFF_COLUMNS     = new String[] {"cnt", "abort_cnt"};

	public static final boolean  NEGATIVE_DIFF_COUNTERS_TO_ZERO = false;
	public static final boolean  IS_SYSTEM_CM                   = true;
	public static final int      DEFAULT_POSTPONE_TIME          = 60;
	public static final int      DEFAULT_QUERY_TIMEOUT          = 30;

	@Override public int     getDefaultPostponeTime()                 { return DEFAULT_POSTPONE_TIME; }
	@Override public int     getDefaultQueryTimeout()                 { return DEFAULT_QUERY_TIMEOUT; }
	@Override public boolean getDefaultIsNegativeDiffCountersToZero() { return NEGATIVE_DIFF_COUNTERS_TO_ZERO; }
	@Override public Type    getTemplateLevel()                       { return Type.LARGE; }

	/**
	 * FACTORY  method to create the object
	 */
	public static CountersModel create(ICounterController counterController, IGuiController guiController)
	{
		if (guiController != null && guiController.hasGUI())
			guiController.splashWindowProgress("Loading: Counter Model '"+CM_NAME+"'");

		return new CmQpMetrics(counterController, guiController);
	}

	public CmQpMetrics(ICounterController counterController, IGuiController guiController)
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
		
		// Need stored proc 'sp_missing_stats'
		// check if it exists: if not it will be created in super.init(conn)
		addDependsOnStoredProc("sybsystemprocs", "sp_asetune_qp_metrics", 
				VersionInfo.SP_ASETUNE_QP_METRICS_CRDATE, VersionInfo.class, 
				"sp_asetune_qp_metrics.sql", AseConnectionUtils.SA_ROLE, NEED_SRV_VERSION);

		addTrendGraphs();
		
		CounterSetTemplates.register(this);
	}


	//------------------------------------------------------------
	// Implementation
	//------------------------------------------------------------
	
	private void addTrendGraphs()
	{
	}

	@Override
	protected TabularCntrPanel createGui()
	{
		return new CmQpMetricsPanel(this);
	}

	@Override
	public String[] getDependsOnConfigForVersion(Connection conn, int srvVersion, boolean isClusterEnabled)
	{
		return NEED_CONFIG;
	}

	@Override
	protected CmSybMessageHandler createSybMessageHandler()
	{
		CmSybMessageHandler msgHandler = super.createSybMessageHandler();

		// Msg 2528: DBCC execution completed. If DBCC printed error messages, contact a user with System Administrator (SA) role.
		msgHandler.addDiscardMsgNum(2528);

		// Msg 0: FLUSHMETRICS
		msgHandler.addDiscardMsgNum(0);

		return msgHandler;
	}

	@Override
	public void addMonTableDictForVersion(Connection conn, int aseVersion, boolean isClusterEnabled)
	{
		try 
		{
			MonTablesDictionary mtd = MonTablesDictionary.getInstance();
			mtd.addTable("sysquerymetrics",  "Holds about Query Plan Metrics.");

			mtd.addColumn("sysquerymetrics", "DBName",       "<html>Database name</html>");
			mtd.addColumn("sysquerymetrics", "uid",          "<html>User ID</html>");
			mtd.addColumn("sysquerymetrics", "gid",          "<html>Group ID</html>");
			mtd.addColumn("sysquerymetrics", "id",           "<html>Unique ID</html>");
			mtd.addColumn("sysquerymetrics", "hashkey",      "<html>The hashkey over the SQL query text</html>");
			mtd.addColumn("sysquerymetrics", "sequence",     "<html>Sequence number for a row when multiple rows are required for the SQL text</html>");
			mtd.addColumn("sysquerymetrics", "exec_min",     "<html>Minimum execution time</html>");
			mtd.addColumn("sysquerymetrics", "exec_max",     "<html>Maximum execution time</html>");
			mtd.addColumn("sysquerymetrics", "exec_avg",     "<html>Average execution time</html>");
			mtd.addColumn("sysquerymetrics", "elap_min",     "<html>Minimum elapsed time</html>");
			mtd.addColumn("sysquerymetrics", "elap_max",     "<html>Maximum elapsed time</html>");
			mtd.addColumn("sysquerymetrics", "elap_avg",     "<html>Average elapsed time</html>");
			mtd.addColumn("sysquerymetrics", "lio_min",      "<html>Minimum logical IO</html>");
			mtd.addColumn("sysquerymetrics", "lio_max",      "<html>Maximum logical IO</html>");
			mtd.addColumn("sysquerymetrics", "lio_avg",      "<html>Average logical IO</html>");
			mtd.addColumn("sysquerymetrics", "pio_min",      "<html>Minumum physical IO</html>");
			mtd.addColumn("sysquerymetrics", "pio_max",      "<html>Maximum physical IO</html>");
			mtd.addColumn("sysquerymetrics", "pio_avg",      "<html>Average physical IO</html>");
			mtd.addColumn("sysquerymetrics", "cnt",          "<html>Number of times the query has been executed.</html>");
			mtd.addColumn("sysquerymetrics", "abort_cnt",    "<html>Number of times a query was aborted by Resource Governor as a resource limit was exceeded.</html>");
			mtd.addColumn("sysquerymetrics", "qtext",        "<html>query text</html>");
		}
		catch (NameNotFoundException e) {/*ignore*/}
	}

	@Override
	public List<String> getPkForVersion(Connection conn, int aseVersion, boolean isClusterEnabled)
	{
		List <String> pkCols = new LinkedList<String>();

		pkCols.add("DBName");
		pkCols.add("id");
		pkCols.add("sequence");

		return pkCols;
	}

	@Override
	public String getSqlForVersion(Connection conn, int aseVersion, boolean isClusterEnabled)
	{
		String sql = "exec sp_asetune_qp_metrics";
		return sql;
	}

	@Override
	/** override this so we can change datatype for column 'qtext' from varchar(255) -> text */
	public void setResultSetMetaData(ResultSetMetaData rsmd)
	{
		int qtext_pos = AseConnectionUtils.findColumn(rsmd, "qtext");
		if (qtext_pos == -1)
		{
			super.setResultSetMetaData(rsmd);
		}
		else
		{
			ResultSetMetaDataChangable xe = new ResultSetMetaDataChangable(rsmd);
			xe.setExtendedEntry(qtext_pos, java.sql.Types.CLOB);
			super.setResultSetMetaData(xe);
		}
	}

	/** "collapse" all rows for DBName, id, sequence... the column "qtext" into one row (the first row DBName, id, sequence=0) */
	@Override
	public boolean hookInSqlRefreshBeforeAddRow(CounterSample cnt, List<Object> thisRow, List<Object> prevRow)
	{
		if (thisRow == null || prevRow == null)
			return true;

		int DBName_pos   = cnt.findColumn("DBName");
		int id_pos       = cnt.findColumn("id");
		int sequence_pos = cnt.findColumn("sequence");
		int qtext_pos    = cnt.findColumn("qtext");
		
		String DBName   = (String) thisRow.get(DBName_pos);
		Number id       = (Number) thisRow.get(id_pos);
		Number sequence = (Number) thisRow.get(sequence_pos);
		String qtext    = (String) thisRow.get(qtext_pos);

		if (sequence.intValue() > 0)
		{
			String prev_DBName   = (String) prevRow.get(DBName_pos);
			Number prev_id       = (Number) prevRow.get(id_pos);
			Number prev_sequence = (Number) prevRow.get(sequence_pos);
			String prev_qtext    = (String) prevRow.get(qtext_pos);

			if (DBName.equals(prev_DBName) && id.equals(prev_id) && prev_sequence.intValue() == 0)
			{
//System.out.println(" < hookInSqlRefreshBeforeAddRow("+cnt.getName()+"): collapsing DBName='"+DBName+"', id="+id+", sequence="+sequence+", qtext='"+qtext+"'.");
				prevRow.set(qtext_pos, prev_qtext + qtext);
//System.out.println("   hookInSqlRefreshBeforeAddRow("+cnt.getName()+"): privRow:   DBName='"+DBName+"', id="+id+", sequence="+prevRow.get(sequence_pos)+", qtext='"+prev_qtext+"'.");
//System.out.println("   hookInSqlRefreshBeforeAddRow("+cnt.getName()+"): newPrivRow:DBName='"+DBName+"', id="+id+", sequence="+prevRow.get(sequence_pos)+", qtext='"+prevRow.get(qtext_pos)+"'.");
				return false;
			}
		}
		return true;
	}
}
