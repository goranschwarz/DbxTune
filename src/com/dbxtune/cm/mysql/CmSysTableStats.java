/*******************************************************************************
 * Copyright (C) 2010-2025 Goran Schwarz
 * 
 * This file is part of DbxTune
 * DbxTune is a family of sub-products *Tune, hence the Dbx
 * Here are some of the tools: AseTune, IqTune, RsTune, RaxTune, HanaTune, 
 *          SqlServerTune, PostgresTune, MySqlTune, MariaDbTune, Db2Tune, ...
 * 
 * DbxTune is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, version 3 of the License.
 * 
 * DbxTune is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License
 * along with DbxTune.  If not, see <http://www.gnu.org/licenses/>.
 ******************************************************************************/
package com.dbxtune.cm.mysql;

import java.util.LinkedList;
import java.util.List;

import javax.naming.NameNotFoundException;

import com.dbxtune.ICounterController;
import com.dbxtune.IGuiController;
import com.dbxtune.cm.CounterSetTemplates;
import com.dbxtune.cm.CountersModel;
import com.dbxtune.cm.CounterSetTemplates.Type;
import com.dbxtune.config.dict.MonTablesDictionary;
import com.dbxtune.config.dict.MonTablesDictionaryManager;
import com.dbxtune.gui.MainFrame;
import com.dbxtune.sql.conn.DbxConnection;
import com.dbxtune.sql.conn.info.DbmsVersionInfo;

/**
 * @author Goran Schwarz (goran_schwarz@hotmail.com)
 */
public class CmSysTableStats
extends CountersModel
{
//	private static Logger        _logger          = Logger.getLogger(CmSysTableStats.class);
	private static final long    serialVersionUID = 1L;

	public static final String   CM_NAME          = CmSysTableStats.class.getSimpleName();
	public static final String   SHORT_NAME       = "Table Stats";
	public static final String   HTML_DESC        = 
		"<html>"
		+ "<h4>Table Statistics</h4>"
		+ "Simply select * from sys.`x$schema_table_statistics`"
		+ "</html>";

	public static final String   GROUP_NAME       = MainFrame.TCP_GROUP_OBJECT_ACCESS;
	public static final String   GUI_ICON_FILE    = "images/"+CM_NAME+".png";

	public static final long     NEED_SRV_VERSION = 0;
	public static final long     NEED_CE_VERSION  = 0;

	public static final String[] MON_TABLES       = new String[] {"x$schema_table_statistics"};
	public static final String[] NEED_ROLES       = new String[] {};
	public static final String[] NEED_CONFIG      = new String[] {};

	public static final String[] PCT_COLUMNS      = new String[] {};
	public static final String[] DIFF_COLUMNS     = new String[] {
			  "total_latency"     // The total wait time of timed I/O events for the table.
			, "rows_fetched"      // The total number of rows read from the table.
			, "fetch_latency"     // The total wait time of timed read I/O events for the table.
			, "rows_inserted"     // The total number of rows inserted into the table.
			, "insert_latency"    // The total wait time of timed insert I/O events for the table.
			, "rows_updated"      // The total number of rows updated in the table.
			, "update_latency"    // The total wait time of timed update I/O events for the table.
			, "rows_deleted"      // The total number of rows deleted from the table.
			, "delete_latency"    // The total wait time of timed delete I/O events for the table.
			, "io_read_requests"  // The total number of read requests for the table.
			, "io_read"           // The total number of bytes read from the table.
			, "io_read_latency"   // The total wait time of reads from the table.
			, "io_write_requests" // The total number of write requests for the table.
			, "io_write"          // The total number of bytes written to the table.
			, "io_write_latency"  // The total wait time of writes to the table.
			, "io_misc_requests"  // The total number of miscellaneous I/O requests for the table.
			, "io_misc_latency"   // The total wait time of miscellaneous I/O requests for the table.
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

		return new CmSysTableStats(counterController, guiController);
	}

	public CmSysTableStats(ICounterController counterController, IGuiController guiController)
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
	public String[] getDependsOnConfigForVersion(DbxConnection conn, DbmsVersionInfo versionInfo)
	{
		return NEED_CONFIG;
	}

	@Override
	public void addMonTableDictForVersion(DbxConnection conn, DbmsVersionInfo versionInfo)
	{
		try 
		{
			// https://dev.mysql.com/doc/refman/5.7/en/innodb-buffer-pool-stats-table.html
			
			MonTablesDictionary mtd = MonTablesDictionaryManager.getInstance();
			mtd.addTable("x$schema_table_statistics",  "These views summarize table statistics. By default, rows are sorted by descending total wait time (tables with most contention first).");

			mtd.addColumn("x$schema_table_statistics", "table_schema",      "<html>The schema that contains the table.</html>");
			mtd.addColumn("x$schema_table_statistics", "table_name",        "<html>The table name.</html>");
			mtd.addColumn("x$schema_table_statistics", "total_latency",     "<html>The total wait time of timed I/O events for the table.</html>");
			mtd.addColumn("x$schema_table_statistics", "rows_fetched",      "<html>The total number of rows read from the table.</html>");
			mtd.addColumn("x$schema_table_statistics", "fetch_latency",     "<html>The total wait time of timed read I/O events for the table.</html>");
			mtd.addColumn("x$schema_table_statistics", "rows_inserted",     "<html>The total number of rows inserted into the table.</html>");
			mtd.addColumn("x$schema_table_statistics", "insert_latency",    "<html>The total wait time of timed insert I/O events for the table.</html>");
			mtd.addColumn("x$schema_table_statistics", "rows_updated",      "<html>The total number of rows updated in the table.</html>");
			mtd.addColumn("x$schema_table_statistics", "update_latency",    "<html>The total wait time of timed update I/O events for the table.</html>");
			mtd.addColumn("x$schema_table_statistics", "rows_deleted",      "<html>The total number of rows deleted from the table.</html>");
			mtd.addColumn("x$schema_table_statistics", "delete_latency",    "<html>The total wait time of timed delete I/O events for the table.</html>");
			mtd.addColumn("x$schema_table_statistics", "io_read_requests",  "<html>The total number of read requests for the table.</html>");
			mtd.addColumn("x$schema_table_statistics", "io_read",           "<html>The total number of bytes read from the table.</html>");
			mtd.addColumn("x$schema_table_statistics", "io_read_latency",   "<html>The total wait time of reads from the table.</html>");
			mtd.addColumn("x$schema_table_statistics", "io_write_requests", "<html>The total number of write requests for the table.</html>");
			mtd.addColumn("x$schema_table_statistics", "io_write",          "<html>The total number of bytes written to the table.</html>");
			mtd.addColumn("x$schema_table_statistics", "io_write_latency",  "<html>The total wait time of writes to the table.</html>");
			mtd.addColumn("x$schema_table_statistics", "io_misc_requests",  "<html>The total number of miscellaneous I/O requests for the table.</html>");
			mtd.addColumn("x$schema_table_statistics", "io_misc_latency",   "<html>The total wait time of miscellaneous I/O requests for the table.</html>");
		}
		catch (NameNotFoundException e) 
		{
		//	_logger.warn("Problems in cm='" + CM_NAME + "', adding addMonTableDictForVersion. Caught: " + e); 
			System.out.println("Problems in cm='" + CM_NAME + "', adding addMonTableDictForVersion. Caught: " + e); 
		}
	}

	@Override
	public List<String> getPkForVersion(DbxConnection conn, DbmsVersionInfo versionInfo)
	{
		List <String> pkCols = new LinkedList<String>();

		pkCols.add("table_schema");
		pkCols.add("table_name");

		return pkCols;
	}

	@Override
	public String getSqlForVersion(DbxConnection conn, DbmsVersionInfo versionInfo)
	{
		String sql = "select * from sys.`x$schema_table_statistics`";

		return sql;
	}
}
