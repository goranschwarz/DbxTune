/*******************************************************************************
 * Copyright (C) 2010-2027 Goran Schwarz
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

import java.lang.invoke.MethodHandles;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

import javax.naming.NameNotFoundException;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.dbxtune.ICounterController;
import com.dbxtune.IGuiController;
import com.dbxtune.cm.CmSettingsHelper;
import com.dbxtune.cm.CounterSetTemplates;
import com.dbxtune.cm.CounterSetTemplates.Type;
import com.dbxtune.cm.CountersModel;
import com.dbxtune.config.dict.MonTablesDictionary;
import com.dbxtune.config.dict.MonTablesDictionaryManager;
import com.dbxtune.gui.MainFrame;
import com.dbxtune.sql.conn.DbxConnection;
import com.dbxtune.sql.conn.info.DbmsVersionInfo;
import com.dbxtune.utils.Configuration;

/**
 * @author Goran Schwarz (goran_schwarz@hotmail.com)
 */
public class CmSysIndexStats
extends CountersModel
{
	private static final Logger _logger = LogManager.getLogger(MethodHandles.lookup().lookupClass());
	private static final long    serialVersionUID = 1L;

	public static final String   CM_NAME          = CmSysIndexStats.class.getSimpleName();
	public static final String   SHORT_NAME       = "Index Stats";
	public static final String   HTML_DESC        =
		"<html>"
		+ "<h4>Index Statistics</h4>"
		+ "Rows read/inserted/updated/deleted per index (sys.x$schema_index_statistics), as Diff and Rate per sample.<br>"
		+ "Index 'PRIMARY' is the table itself (InnoDB stores the rows in the primary key).<br>"
		+ "Only the most active indexes (since the server started) are shown: the property '" + CM_NAME + ".sample.topRows' (default 1000).<br>"
		+ "The *_latency columns are in <b>picoseconds</b>.<br>"
		+ "</html>";

	public static final String   GROUP_NAME       = MainFrame.TCP_GROUP_OBJECT_ACCESS;
	public static final String   GUI_ICON_FILE    = "images/" + CM_NAME + ".png";

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
			guiController.splashWindowProgress("Loading: Counter Model '" + CM_NAME + "'");

		return new CmSysIndexStats(counterController, guiController);
	}

	public CmSysIndexStats(ICounterController counterController, IGuiController guiController)
	{
		super(counterController, guiController,
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
		catch (NameNotFoundException e)
		{
			_logger.warn("Problems in cm='" + CM_NAME + "', adding addMonTableDictForVersion. Caught: " + e);
		}
	}

	/** If we can't read the view: disable the CM, and tell what to do about it */
	@Override
	public boolean checkDependsOnOther(DbxConnection conn)
	{
		return MySqlCmUtils.checkViewsAreReadable(this, conn, "sys.x$schema_index_statistics");
	}

	public static final String  PROPKEY_sample_topRows = CM_NAME + ".sample.topRows";
	public static final int     DEFAULT_sample_topRows = 1000;

	@Override
	protected void registerDefaultValues()
	{
		super.registerDefaultValues();

		Configuration.registerDefaultValue(PROPKEY_sample_topRows, DEFAULT_sample_topRows);
	}

	/** Used by the: Create 'Offline Session' Wizard */
	@Override
	public List<CmSettingsHelper> getLocalSettings()
	{
		Configuration conf = Configuration.getCombinedConfiguration();
		List<CmSettingsHelper> list = new ArrayList<>();

		list.add(new CmSettingsHelper("Top Rows", PROPKEY_sample_topRows, Integer.class, conf.getIntProperty(PROPKEY_sample_topRows, DEFAULT_sample_topRows), DEFAULT_sample_topRows, "Only keep the ## most active indexes (by select+insert+update+delete latency since the server started). 0 = all indexes." ));

		return list;
	}

	@Override
	public List<String> getPkForVersion(DbxConnection conn, DbmsVersionInfo versionInfo)
	{
		List <String> pkCols = new LinkedList<String>();

		pkCols.add("table_schema");
		pkCols.add("table_name");
		pkCols.add("index_name");

		return pkCols;
	}

	@Override
	public String getSqlForVersion(DbxConnection conn, DbmsVersionInfo versionInfo)
	{
		// On a server with many tables, keep only the most active indexes (2000 tables: 4167 rows, 12 ms)
		int topRows = Configuration.getCombinedConfiguration().getIntProperty(PROPKEY_sample_topRows, DEFAULT_sample_topRows);

		String sql = ""
			+ "select /* ${cmCollectorName} */ \n"
			+ "     table_schema, table_name, index_name \n"
			+ "    ,rows_selected, select_latency \n"
			+ "    ,rows_inserted, insert_latency \n"
			+ "    ,rows_updated,  update_latency \n"
			+ "    ,rows_deleted,  delete_latency \n"
			+ "from sys.`x$schema_index_statistics` \n"
			+ "order by select_latency + insert_latency + update_latency + delete_latency desc \n"
			+ (topRows > 0 ? "limit " + topRows + " \n" : "")
			+ "";

		return sql;
	}
}
