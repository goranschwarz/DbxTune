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
public class CmInnodbBpStats
extends CountersModel
{
//	private static Logger        _logger          = Logger.getLogger(CmGlobalStatus.class);
	private static final long    serialVersionUID = 1L;

	public static final String   CM_NAME          = CmInnodbBpStats.class.getSimpleName();
	public static final String   SHORT_NAME       = "InnoDB Buffer Pool Stat";
	public static final String   HTML_DESC        = 
		"<html>"
		+ "<h4>InnoDB Buffer Pool Stat</h4>"
		+ "Simply select * from information_schema.INNODB_BUFFER_POOL_STATS"
		+ "</html>";

	public static final String   GROUP_NAME       = MainFrame.TCP_GROUP_CACHE;
	public static final String   GUI_ICON_FILE    = "images/"+CM_NAME+".png";

	public static final long     NEED_SRV_VERSION = 0;
	public static final long     NEED_CE_VERSION  = 0;

	public static final String[] MON_TABLES       = new String[] {"INNODB_BUFFER_POOL_STATS"};
	public static final String[] NEED_ROLES       = new String[] {};
	public static final String[] NEED_CONFIG      = new String[] {};

	public static final String[] PCT_COLUMNS      = new String[] {};
	public static final String[] DIFF_COLUMNS     = new String[] {
			 "FREE_BUFFERS"                        // The number of free pages in the InnoDB buffer pool
			,"DATABASE_PAGES"                      // The number of pages in the InnoDB buffer pool containing data. The number includes both dirty and clean pages.
			,"OLD_DATABASE_PAGES"                  // The number of pages in the old buffer pool sublist.
			,"MODIFIED_DATABASE_PAGES"             // The number of modified (dirty) database pages
			,"PENDING_DECOMPRESS"                  // The number of pages pending decompression
			,"PAGES_MADE_YOUNG"                    // The number of pages made young
			,"PAGES_NOT_MADE_YOUNG"                // The number of pages not made young
			,"NUMBER_PAGES_READ"                   // The number of pages read
			,"NUMBER_PAGES_CREATED"                // The number of pages created
			,"NUMBER_PAGES_WRITTEN"                // The number of pages written
			,"NUMBER_PAGES_GET"                    // The number of logical read requests.
			,"YOUNG_MAKE_PER_THOUSAND_GETS"        // The number of pages made young per thousand gets
			,"NOT_YOUNG_MAKE_PER_THOUSAND_GETS"    // The number of pages not made young per thousand gets
			,"NUMBER_PAGES_READ_AHEAD"             // The number of pages read ahead
			,"NUMBER_READ_AHEAD_EVICTED"           // The number of pages read into the InnoDB buffer pool by the read-ahead background thread that were subsequently evicted without having been accessed by queries.
			,"LRU_IO_TOTAL LRU"                    // IO total
			,"UNCOMPRESS_TOTAL"                    // Total number of pages decompressed
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

		return new CmInnodbBpStats(counterController, guiController);
	}

	public CmInnodbBpStats(ICounterController counterController, IGuiController guiController)
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
			mtd.addTable("INNODB_BUFFER_POOL_STATS",  "The INNODB_BUFFER_POOL_STATS table provides much of the same buffer pool information provided in SHOW ENGINE INNODB STATUS output. Much of the same information may also be obtained using InnoDB buffer pool server status variables.");

			mtd.addColumn("INNODB_BUFFER_POOL_STATS", "POOL_ID"                             , "<html>Buffer Pool ID. A unique identifier to distinguish between multiple buffer pool instances.</html>");
			mtd.addColumn("INNODB_BUFFER_POOL_STATS", "POOL_SIZE"                           , "<html>The InnoDB buffer pool size in pages.</html>");
			mtd.addColumn("INNODB_BUFFER_POOL_STATS", "FREE_BUFFERS"                        , "<html>The number of free pages in the InnoDB buffer pool</html>");
			mtd.addColumn("INNODB_BUFFER_POOL_STATS", "DATABASE_PAGES"                      , "<html>The number of pages in the InnoDB buffer pool containing data. The number includes both dirty and clean pages.</html>");
			mtd.addColumn("INNODB_BUFFER_POOL_STATS", "OLD_DATABASE_PAGES"                  , "<html>The number of pages in the old buffer pool sublist.</html>");
			mtd.addColumn("INNODB_BUFFER_POOL_STATS", "MODIFIED_DATABASE_PAGES"             , "<html>The number of modified (dirty) database pages</html>");
			mtd.addColumn("INNODB_BUFFER_POOL_STATS", "PENDING_DECOMPRESS"                  , "<html>The number of pages pending decompression</html>");
			mtd.addColumn("INNODB_BUFFER_POOL_STATS", "PENDING_READS"                       , "<html>The number of pending reads</html>");
			mtd.addColumn("INNODB_BUFFER_POOL_STATS", "PENDING_FLUSH_LRU"                   , "<html>The number of pages pending flush in the LRU</html>");
			mtd.addColumn("INNODB_BUFFER_POOL_STATS", "PENDING_FLUSH_LIST"                  , "<html>The number of pages pending flush in the flush list</html>");
			mtd.addColumn("INNODB_BUFFER_POOL_STATS", "PAGES_MADE_YOUNG"                    , "<html>The number of pages made young</html>");
			mtd.addColumn("INNODB_BUFFER_POOL_STATS", "PAGES_NOT_MADE_YOUNG"                , "<html>The number of pages not made young</html>");
			mtd.addColumn("INNODB_BUFFER_POOL_STATS", "PAGES_MADE_YOUNG_RATE"               , "<html>The number of pages made young per second (pages made young since the last printout / time elapsed)</html>");
			mtd.addColumn("INNODB_BUFFER_POOL_STATS", "PAGES_MADE_NOT_YOUNG_RATE"           , "<html>The number of pages not made per second (pages not made young since the last printout / time elapsed)</html>");
			mtd.addColumn("INNODB_BUFFER_POOL_STATS", "NUMBER_PAGES_READ"                   , "<html>The number of pages read</html>");
			mtd.addColumn("INNODB_BUFFER_POOL_STATS", "NUMBER_PAGES_CREATED"                , "<html>The number of pages created</html>");
			mtd.addColumn("INNODB_BUFFER_POOL_STATS", "NUMBER_PAGES_WRITTEN"                , "<html>The number of pages written</html>");
			mtd.addColumn("INNODB_BUFFER_POOL_STATS", "PAGES_READ_RATE"                     , "<html>The number of pages read per second (pages read since the last printout / time elapsed)</html>");
			mtd.addColumn("INNODB_BUFFER_POOL_STATS", "PAGES_CREATE_RATE"                   , "<html>The number of pages created per second (pages created since the last printout / time elapsed)</html>");
			mtd.addColumn("INNODB_BUFFER_POOL_STATS", "PAGES_WRITTEN_RATE"                  , "<html>The number of pages written per second (pages written since the last printout / time elapsed)</html>");
			mtd.addColumn("INNODB_BUFFER_POOL_STATS", "NUMBER_PAGES_GET"                    , "<html>The number of logical read requests.</html>");
			mtd.addColumn("INNODB_BUFFER_POOL_STATS", "HIT_RATE"                            , "<html>The buffer pool hit rate</html>");
			mtd.addColumn("INNODB_BUFFER_POOL_STATS", "YOUNG_MAKE_PER_THOUSAND_GETS"        , "<html>The number of pages made young per thousand gets</html>");
			mtd.addColumn("INNODB_BUFFER_POOL_STATS", "NOT_YOUNG_MAKE_PER_THOUSAND_GETS"    , "<html>The number of pages not made young per thousand gets</html>");
			mtd.addColumn("INNODB_BUFFER_POOL_STATS", "NUMBER_PAGES_READ_AHEAD"             , "<html>The number of pages read ahead</html>");
			mtd.addColumn("INNODB_BUFFER_POOL_STATS", "NUMBER_READ_AHEAD_EVICTED"           , "<html>The number of pages read into the InnoDB buffer pool by the read-ahead background thread that were subsequently evicted without having been accessed by queries.</html>");
			mtd.addColumn("INNODB_BUFFER_POOL_STATS", "READ_AHEAD_RATE"                     , "<html>The read ahead rate per second (pages read ahead since the last printout / time elapsed)</html>");
			mtd.addColumn("INNODB_BUFFER_POOL_STATS", "READ_AHEAD_EVICTED_RATE"             , "<html>The number of read ahead pages evicted without access per second (read ahead pages not accessed since the last printout / time elapsed)</html>");
			mtd.addColumn("INNODB_BUFFER_POOL_STATS", "LRU_IO_TOTAL LRU"                    , "<html>IO total</html>");
			mtd.addColumn("INNODB_BUFFER_POOL_STATS", "LRU_IO_CURRENT"                      , "<html>LRU IO for the current interval</html>");
			mtd.addColumn("INNODB_BUFFER_POOL_STATS", "UNCOMPRESS_TOTAL"                    , "<html>Total number of pages decompressed</html>");
			mtd.addColumn("INNODB_BUFFER_POOL_STATS", "UNCOMPRESS_CURRENT"                  , "<html>The number of pages decompressed in the current interval</html>");
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

		pkCols.add("POOL_ID");

		return pkCols;
	}

	@Override
	public String getSqlForVersion(DbxConnection conn, DbmsVersionInfo versionInfo)
	{
		String sql = "select * from information_schema.INNODB_BUFFER_POOL_STATS";

		return sql;
	}
}
