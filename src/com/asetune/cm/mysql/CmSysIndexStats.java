package com.asetune.cm.mysql;

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
import com.asetune.gui.MainFrame;

/**
 * @author Goran Schwarz (goran_schwarz@hotmail.com)
 */
public class CmSysIndexStats
extends CountersModel
{
//	private static Logger        _logger          = Logger.getLogger(CmSysIndexStats.class);
	private static final long    serialVersionUID = 1L;

	public static final String   CM_NAME          = CmSysIndexStats.class.getSimpleName();
	public static final String   SHORT_NAME       = "Index Stats";
	public static final String   HTML_DESC        = 
		"<html>"
		+ "<h4>Index Statistics</h4>"
		+ "Simply select * from sys.`x$schema_index_statistics`"
		+ "</html>";

	public static final String   GROUP_NAME       = MainFrame.TCP_GROUP_OBJECT_ACCESS;
	public static final String   GUI_ICON_FILE    = "images/"+CM_NAME+".png";

	public static final long     NEED_SRV_VERSION = 0;
	public static final long     NEED_CE_VERSION  = 0;

	public static final String[] MON_TABLES       = new String[] {"x$schema_index_statistics"};
	public static final String[] NEED_ROLES       = new String[] {};
	public static final String[] NEED_CONFIG      = new String[] {};

	public static final String[] PCT_COLUMNS      = new String[] {};
	public static final String[] DIFF_COLUMNS     = new String[] {
			  "rows_selected"  // The total number of rows read using the index.
			, "select_latency" // The total wait time of timed reads using the index.
			, "rows_inserted"  // The total number of rows inserted into the index.
			, "insert_latency" // The total wait time of timed inserts into the index.
			, "rows_updated"   // The total number of rows updated in the index.
			, "update_latency" // The total wait time of timed updates in the index.
			, "rows_deleted"   // The total number of rows deleted from the index.
			, "delete_latency" // The total wait time of timed deletes from the index.
			};

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

		return new CmSysIndexStats(counterController, guiController);
	}

	public CmSysIndexStats(ICounterController counterController, IGuiController guiController)
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
	public String[] getDependsOnConfigForVersion(Connection conn, long srvVersion, boolean isClusterEnabled)
	{
		return NEED_CONFIG;
	}

	@Override
	public void addMonTableDictForVersion(Connection conn, long srvVersion, boolean isClusterEnabled)
	{
		try 
		{
			// https://dev.mysql.com/doc/refman/5.7/en/innodb-buffer-pool-stats-table.html
			
			MonTablesDictionary mtd = MonTablesDictionaryManager.getInstance();
			mtd.addTable("x$schema_index_statistics",  "These views summarize table statistics. By default, rows are sorted by descending total wait time (tables with most contention first).");

			mtd.addColumn("x$schema_index_statistics", "table_schema",   "<html>The schema that contains the table.</html>");
			mtd.addColumn("x$schema_index_statistics", "table_name",     "<html>The table that contains the index.</html>");
			mtd.addColumn("x$schema_index_statistics", "index_name",     "<html>The name of the index.</html>");
			mtd.addColumn("x$schema_index_statistics", "rows_selected",  "<html>The total number of rows read using the index.</html>");
			mtd.addColumn("x$schema_index_statistics", "select_latency", "<html>The total wait time of timed reads using the index.</html>");
			mtd.addColumn("x$schema_index_statistics", "rows_inserted",  "<html>The total number of rows inserted into the index.</html>");
			mtd.addColumn("x$schema_index_statistics", "insert_latency", "<html>The total wait time of timed inserts into the index.</html>");
			mtd.addColumn("x$schema_index_statistics", "rows_updated",   "<html>The total number of rows updated in the index.</html>");
			mtd.addColumn("x$schema_index_statistics", "update_latency", "<html>The total wait time of timed updates in the index.</html>");
			mtd.addColumn("x$schema_index_statistics", "rows_deleted",   "<html>The total number of rows deleted from the index.</html>");
			mtd.addColumn("x$schema_index_statistics", "delete_latency", "<html>The total wait time of timed deletes from the index.</html>");

		}
		catch (NameNotFoundException e) {/*ignore*/}
	}

	@Override
	public List<String> getPkForVersion(Connection conn, long srvVersion, boolean isClusterEnabled)
	{
		List <String> pkCols = new LinkedList<String>();

		pkCols.add("table_schema");
		pkCols.add("table_name");
		pkCols.add("index_name");

		return pkCols;
	}

	@Override
	public String getSqlForVersion(Connection conn, long srvVersion, boolean isClusterEnabled)
	{
		String sql = "select * from sys.`x$schema_index_statistics`";

		return sql;
	}
}
