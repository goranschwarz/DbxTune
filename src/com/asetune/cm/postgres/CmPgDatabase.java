/*******************************************************************************
 * Copyright (C) 2010-2020 Goran Schwarz
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
package com.asetune.cm.postgres;

import java.math.BigDecimal;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;

import javax.naming.NameNotFoundException;

import org.apache.log4j.Logger;

import com.asetune.ICounterController;
import com.asetune.IGuiController;
import com.asetune.alarm.AlarmHandler;
import com.asetune.alarm.events.AlarmEvent;
import com.asetune.alarm.events.AlarmEventConfigResourceIsLow;
import com.asetune.alarm.events.postgres.AlarmEventPgChecksumFailure;
import com.asetune.central.pcs.CentralPersistReader;
import com.asetune.cm.CmSettingsHelper;
import com.asetune.cm.CounterSample;
import com.asetune.cm.CounterSetTemplates;
import com.asetune.cm.CounterSetTemplates.Type;
import com.asetune.cm.CountersModel;
import com.asetune.cm.postgres.gui.CmPgDatabasePanel;
import com.asetune.config.dict.MonTablesDictionary;
import com.asetune.config.dict.MonTablesDictionaryManager;
import com.asetune.graph.TrendGraphDataPoint;
import com.asetune.graph.TrendGraphDataPoint.LabelType;
import com.asetune.gui.MainFrame;
import com.asetune.gui.TabularCntrPanel;
import com.asetune.sql.conn.DbxConnection;
import com.asetune.sql.conn.info.DbmsVersionInfo;
import com.asetune.utils.Configuration;
import com.asetune.utils.MathUtils;
import com.asetune.utils.TimeUtils;
import com.asetune.utils.Ver;

/**
 * @author Goran Schwarz (goran_schwarz@hotmail.com)
 */
// this can be of help: 
//    https://www.datadoghq.com/blog/postgresql-monitoring/
//    https://github.com/DataDog/the-monitor/blob/master/postgresql/postgresql-monitoring.md

public class CmPgDatabase
extends CountersModel
{
	private static Logger        _logger          = Logger.getLogger(CmPgDatabase.class);
	private static final long    serialVersionUID = 1L;

	public static final String   CM_NAME          = CmPgDatabase.class.getSimpleName();
	public static final String   SHORT_NAME       = "Databases";
	public static final String   HTML_DESC        = 
		"<html>" +
		"One row per database, showing database-wide statistics." +
		"</html>";

	public static final String   GROUP_NAME       = MainFrame.TCP_GROUP_SERVER;
	public static final String   GUI_ICON_FILE    = "images/"+CM_NAME+".png";

	public static final long     NEED_SRV_VERSION = 0;
	public static final long     NEED_CE_VERSION  = 0;

	public static final String[] MON_TABLES       = new String[] {"pg_stat_database"};
	public static final String[] NEED_ROLES       = new String[] {};
	public static final String[] NEED_CONFIG      = new String[] {};

	public static final String[] PCT_COLUMNS      = new String[] { 
			"fetch_efficency_pct", 
			"fetch_efficency_slide_pct",
			"cache_hit_pct"
	};
	public static final String[] DIFF_COLUMNS     = new String[] {
			"numbackends",
			"xact_commit",
			"xact_rollback",
			"total_reads",
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
			"blk_write_time",
			
			// 12
			"checksum_failures_diff",
			
			// 14
			"sessions",
			"session_time",
			"active_time",
			"idle_in_transaction_time"
			
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


//	@Override 
//	public boolean discardDiffPctHighlighterOnAbsTable() 
//	{
//		// SHOW PCT values as RED even in ABS samples (because we calculate total PCT in SQL)
//		return false; 
//	}
//NOTE: to use above it should be split into: discard(Diff|Pct)HighlighterOnAbsTable()
//      right now it's BOTH delta and pct highlighters that are enabled, and we should only do PCT here
	
	//------------------------------------------------------------
	// Implementation
	//------------------------------------------------------------
	private static final String PROP_PREFIX                         = CM_NAME;
                                                                    
	public static final String  PROPKEY_SlideTimeInSec              = PROP_PREFIX + ".SlideTimeInSec";
	public static final int     DEFAULT_SlideTimeInSec              = 900;

	public static final String GRAPH_NAME_FETCH_EFFECIENT_PCT       = "FetchEfficientPct";
	public static final String GRAPH_NAME_FETCH_EFFECIENT_SLIDE_PCT = "FetchEfficientSlidePct";
	public static final String GRAPH_NAME_FETCH_EFFECIENT_ABS_PCT   = "FetchEfficientAbsPct";
	public static final String GRAPH_NAME_FETCH_EFFECIENT_SUM       = "FetchEfficientSum";
	public static final String GRAPH_NAME_CONNECTIONS               = "Connections";
	public static final String GRAPH_NAME_CONNECTIONS_SUM           = "ConnectionsSum";
	public static final String GRAPH_NAME_COMMITS                   = "Commits";
	public static final String GRAPH_NAME_ROLLBACKS                 = "Rollbacks";
	public static final String GRAPH_NAME_CACHE_HIT_PCT             = "CacheHitPct";
	public static final String GRAPH_NAME_TOTAL_READS               = "TotalReads";
	public static final String GRAPH_NAME_READS                     = "Reads";
	public static final String GRAPH_NAME_CACHE_HITS                = "CacheHits";
	public static final String GRAPH_NAME_ROWS_RETURNED             = "RowsReturned";
	public static final String GRAPH_NAME_ROWS_FETCHED              = "RowsFetched";
	public static final String GRAPH_NAME_ROWS_INSERTS              = "Inserts";
	public static final String GRAPH_NAME_ROWS_UPDATED              = "Updated";
	public static final String GRAPH_NAME_ROWS_DELETED              = "Deleted";
	public static final String GRAPH_NAME_ROWS_INS_UPD_DEL          = "InsUpDel";
	public static final String GRAPH_NAME_ROWS_CONFLICTS            = "Conflicts";
	public static final String GRAPH_NAME_TEMP_FILES                = "TempFiles";
	public static final String GRAPH_NAME_TEMP_BYTES                = "TempBytes";
	public static final String GRAPH_NAME_DEADLOCKS                 = "Deadlocks";
	public static final String GRAPH_NAME_READ_TIME                 = "ReadTime";
	public static final String GRAPH_NAME_WRITE_TIME                = "WriteTime";
	public static final String GRAPH_NAME_DBSIZE_MB                 = "DbSizeMb";
	
	@Override
	protected TabularCntrPanel createGui()
	{
		return new CmPgDatabasePanel(this);
	}
	
	@Override
	public List<String> getPkForVersion(DbxConnection conn, DbmsVersionInfo versionInfo)
	{
		List <String> pkCols = new LinkedList<String>();

		pkCols.add("datid");

		return pkCols;
	}

	@Override
	public String getSqlForVersion(DbxConnection conn, DbmsVersionInfo versionInfo)
	{
		String checksum_failures_diff = "";
		if (versionInfo.getLongVersion() >= Ver.ver(12))
			checksum_failures_diff = "    ,checksum_failures              AS checksum_failures_diff \n";
		
		return ""
			+ "SELECT \n"
			+ "     sd.* \n"
			+ "    ,(sd.blks_hit + sd.blks_read)   AS total_reads \n"
			+ "    ,CAST(100.0 * (sd.blks_hit*1.0) / NULLIF((sd.blks_hit + sd.blks_read), 0) as numeric(10,2)) AS cache_hit_pct \n"
			+ "    ,CASE WHEN has_database_privilege(sd.datname, 'CONNECT') THEN pg_database_size(sd.datname) / 1024 /1024 ELSE -1 END AS dbsize_mb \n"
			+ "    ,CAST( CASE WHEN sd.tup_returned > 0 THEN (sd.tup_fetched*1.0)/(sd.tup_returned*1.0)*100.0 ELSE null END as numeric(10,2)) AS fetch_efficency_pct \n"
			+ "    ,CAST( 0.0   as numeric(10,2))  AS fetch_efficency_slide_pct \n"
			+ "    ,CAST( 0     as bigint)         AS tup_fetched_in_slide \n"
			+ "    ,CAST( 0     as bigint)         AS tup_returned_in_slide \n"
			+ "    ,CAST( 'n/a' as varchar(30))    AS slide_time \n"
			+ "    ,(SELECT setting::int FROM pg_settings WHERE name = 'max_connections') AS srv_cfg_max_connections \n"
			+ "    ,d.datconnlimit                 AS db_conn_limit \n"
//			+ "    ,a.rolname                      AS db_owner \n"
			+ "    ,a.usename                      AS db_owner \n"
			+ checksum_failures_diff
			+ "    ,d.datcollate                   AS db_collation \n" 
			+ "    ,CAST(d.datacl as varchar(512)) AS db_access_privileges \n" 
			+ "FROM pg_stat_database sd \n"
			+ "INNER JOIN pg_database d ON sd.datid = d.oid \n"
//			+ "INNER JOIN pg_authid   a ON d.datdba = a.oid \n"  // Possibly use >>> 'pg_user' <<< or 'pg_roles' instead of pg_authid
			+ "INNER JOIN pg_user     a ON d.datdba = a.usesysid \n"
//			+ "WHERE sd.datname NOT LIKE 'template%' \n"
			+ "WHERE NOT d.datistemplate \n"
			+ "";
	}

	@Override
	protected void registerDefaultValues()
	{
		super.registerDefaultValues();

		Configuration.registerDefaultValue(PROPKEY_SlideTimeInSec,             DEFAULT_SlideTimeInSec);
	}

	/** Used by the: Create 'Offline Session' Wizard */
	@Override
	public List<CmSettingsHelper> getLocalSettings()
	{
		Configuration conf = Configuration.getCombinedConfiguration();
		List<CmSettingsHelper> list = new ArrayList<>();
		
		list.add(new CmSettingsHelper("Slide Window Time", PROPKEY_SlideTimeInSec, Integer.class, conf.getIntProperty(PROPKEY_SlideTimeInSec, DEFAULT_SlideTimeInSec), DEFAULT_SlideTimeInSec, "Set number of seconds the 'slide window time' will keep 'tup_fetched' and 'tup_returned' for." ));

		return list;
	}

	@Override
	public void addMonTableDictForVersion(DbxConnection conn, DbmsVersionInfo versionInfo)
	{
		try 
		{
			MonTablesDictionary mtd = MonTablesDictionaryManager.getInstance();
			
			mtd.addColumn("pg_stat_database",  "dbsize_mb",             "<html>" +
			                                                                 "Database size in MB.<br> " +
			                                                                 "<b>Formula</b>: pg_database_size(datname) / 1024 / 1024 as dbsize_mb<br>" +
			                                                            "</html>");
			mtd.addColumn("pg_stat_database",  "fetch_efficency_pct",   "<html>" +
			                                                                 "Indicator if we are using efficent access methods.<br> " +
			                                                                 "tup_returned = 'row read from memory/disk'.<br>" +
			                                                                 "tup_fetched = 'row delivered to client, or not <i>discarded</i> by the executor, due to wrong where clause'.<br>" +
			                                                                 "<b>Note:</b> If we have a high Percent, its efficient.<br>" +
			                                                                 "<b>Formula</b>: (tup_fetched / tup_returned) * 100.0<br>" +
			                                                             "</html>");
			mtd.addColumn("pg_stat_database",  "fetch_efficency_slide_pct",  
			                                                            "<html>" +
			                                                                 "same as 'fetch_efficency_pct' but in a sliding time window of # minutes (default is 15 minutes).<br> " +
			                                                                 "The timespan for this <i>slide</i> is set in the options panel, and it's also displayed in column <code>slide_time</code> if the <i>slide time</i> has not yet reached it's maximum value, you will see the current sime span here.<br>" +
			                                                                 "<br> " +
			                                                                 "Indicator if we are using efficent access methods (in the last # minutes).<br> " +
			                                                                 "tup_returned = 'row read from memory/disk'.<br>" +
			                                                                 "tup_fetched = 'row delivered to client, or not <i>discarded</i> by the executor, due to wrong where clause'.<br>" +
			                                                                 "<b>Note:</b> If we have a high Percent, its efficient.<br>" +
			                                                                 "<b>Formula</b>: (timeSlide.tup_fetched / timeSlide.tup_returned) * 100.0<br>" +
			                                                            "</html>");
			mtd.addColumn("pg_stat_database",  "slide_time",            "<html>" +
			                                                                 "This is the current <i>time span</i> the column <code>fetch_efficency_slide_pct</code> reflects.<br> " +
			                                                                 "<b>Formula</b>: last - first sample time in the <i>slide window</i> <br>" +
			                                                            "</html>");
			mtd.addColumn("pg_stat_database",  "tup_fetched_in_slide",  "<html>" +
			                                                                 "Summary of all <i>diff</i> values for the column <code>tup_fetched</code> within the <i>slide window</i>.<br> " +
			                                                                 "<b>Formula</b>: summary of all the <code>tup_fetched</code> within the <i>slide window</i><br>" +
			                                                            "</html>");
			mtd.addColumn("pg_stat_database",  "tup_returned_in_slide", "<html>" +
			                                                                 "Summary of all <i>diff</i> values for the column <code>tup_returned</code> within the <i>slide window</i>.<br> " +
			                                                                 "<b>Formula</b>: summary of all the <code>tup_returned</code> within the <i>slide window</i><br>" +
			                                                            "</html>");
			mtd.addColumn("pg_stat_database",  "srv_cfg_max_connections", "<html>" +
			                                                                 "How many connections can we make to this Postgres server.<br> " +
			                                                                 "<b>Formula</b>: select setting::int from pg_settings where name='max_connections'<br>" +
			                                                            "</html>");
		}
		catch (NameNotFoundException e) 
		{
			_logger.warn("Problems in cm='" + CM_NAME + "', adding addMonTableDictForVersion. Caught: " + e); 
		//	System.out.println("Problems in cm='" + CM_NAME + "', adding addMonTableDictForVersion. Caught: " + e); 
		}
	}
	

	private static class SlideEntry
	{
		Timestamp _sampleTime;
		long      _tup_fetched;
		long      _tup_returned;

		public SlideEntry() {}
		public SlideEntry(Timestamp sampleTime, long tup_fetched, long tup_returned)
		{
			_sampleTime   = sampleTime;
			_tup_fetched  = tup_fetched;
			_tup_returned = tup_returned;
		}
	}
	LinkedHashMap<String, LinkedList<SlideEntry>> _slidePkMap = new LinkedHashMap<>();

	/** 
	 * Compute the AppendLogContPct for DIFF values
	 */
	@Override
	public void localCalculation(CounterSample prevSample, CounterSample newSample, CounterSample diffData)
	{
		long tup_fetched,           tup_returned;
		int  tup_fetched_pos  = -1, tup_returned_pos     = -1;

		int fetch_efficency_pct_pos       = -1;
		int fetch_efficency_slide_pct_pos = -1;
		int tup_fetched_in_slide_pos      = -1;
		int tup_returned_in_slide_pos     = -1;
		int slide_time_pos                = -1;

		int blks_hit_pos                  = -1;
		int total_reads_pos               = -1;
		int cache_hit_pct_pos             = -1;

		// Find column Id's
		List<String> colNames = diffData.getColNames();
		if (colNames == null)
			return;

		for (int colId=0; colId < colNames.size(); colId++) 
		{
			String colName = colNames.get(colId);
			if      (colName.equals("tup_fetched"))               tup_fetched_pos               = colId;
			else if (colName.equals("tup_returned"))              tup_returned_pos              = colId;
			else if (colName.equals("fetch_efficency_pct"))       fetch_efficency_pct_pos       = colId;
			else if (colName.equals("fetch_efficency_slide_pct")) fetch_efficency_slide_pct_pos = colId;
			else if (colName.equals("tup_fetched_in_slide"))      tup_fetched_in_slide_pos      = colId;
			else if (colName.equals("tup_returned_in_slide"))     tup_returned_in_slide_pos     = colId;
			else if (colName.equals("slide_time"))                slide_time_pos                = colId;

			else if (colName.equals("blks_hit"))                  blks_hit_pos                  = colId;
			else if (colName.equals("total_reads"))               total_reads_pos               = colId;
			else if (colName.equals("cache_hit_pct"))             cache_hit_pct_pos             = colId;
		}

		// Loop on all diffData rows
		for (int rowId = 0; rowId < diffData.getRowCount(); rowId++)
		{
			tup_fetched  = ((Number)diffData.getValueAt(rowId, tup_fetched_pos )).longValue();
			tup_returned = ((Number)diffData.getValueAt(rowId, tup_returned_pos)).longValue();

			Long blks_hit    = ((Number)diffData.getValueAt(rowId, blks_hit_pos   )).longValue();
			Long total_reads = ((Number)diffData.getValueAt(rowId, total_reads_pos)).longValue();

			// COLUMN: cache_hit_pct
			if (blks_hit != null && total_reads != null)
			{
				Double cache_hit_pct = null;
				if (total_reads > 0)
				{
					cache_hit_pct = 100.0 * ((blks_hit*1.0) / (total_reads*1.0));
					cache_hit_pct = MathUtils.round(cache_hit_pct, 3);
				}
				diffData.setValueAt(cache_hit_pct, rowId, cache_hit_pct_pos);
			}
			
			// COLUMN: fetch_efficency_pct
			if (tup_returned > 0)
			{
				// Formula: AppendLogContPct = (AppendLogWaits / AppendLogRequests) * 100;
				Double fetch_efficency_pct = ((tup_fetched * 1.0) / (tup_returned * 1.0)) * 100.0;

				BigDecimal newVal = new BigDecimal(fetch_efficency_pct).setScale(2, BigDecimal.ROUND_HALF_EVEN);
				diffData.setValueAt(newVal, rowId, fetch_efficency_pct_pos);
			}
			else
			{
			//	diffData.setValueAt(null, rowId, fetch_efficency_pct_pos);
				diffData.setValueAt(new BigDecimal(-1).setScale(2, BigDecimal.ROUND_HALF_EVEN), rowId, fetch_efficency_pct_pos);
			}
			
			// fetch_efficency_slide_pct, CacheEfficiencySlide and slide_time
			if (fetch_efficency_slide_pct_pos >= 0 && slide_time_pos >= 0 && tup_fetched_in_slide_pos >= 0 && tup_returned_in_slide_pos >= 0)
			{
				String key = newSample.getPkValue(rowId);
				
				// Add PagesRead to the "Slide Cache"
				LinkedList<SlideEntry> list = _slidePkMap.get(key);
				if (list == null)
				{
					list = new LinkedList<>();
					_slidePkMap.put(key, list);
				}
				list.add(new SlideEntry(newSample.getSampleTime(), tup_fetched, tup_returned));
				
				// Remove entries the the "Slide Cache" that is older than X minutes
				removeOldSlideEntries(list, newSample.getSampleTime());
				
				// Sum last X minutes in from the "Slide Cache" and get the "slide time"
				SlideEntry sumSlideEntry = sumSlideEntries(list);
				long slideSumTupFetched  = sumSlideEntry._tup_fetched;
				long slideSumTupReturned = sumSlideEntry._tup_returned;
				String timeStr           = getTimeSpanSlideEntries(list);

				BigDecimal fetch_efficency_slide_pct = null;
				if (slideSumTupReturned > 0)
				{
					Double tmp_fetch_efficency_slide_pct = ((slideSumTupFetched * 1.0) / (slideSumTupReturned * 1.0)) * 100.0;
					fetch_efficency_slide_pct = new BigDecimal(tmp_fetch_efficency_slide_pct).setScale(2, BigDecimal.ROUND_HALF_EVEN);
				}
				else
				{
					fetch_efficency_slide_pct = new BigDecimal(-1).setScale(2, BigDecimal.ROUND_HALF_EVEN);
				}
				
				diffData.setValueAt(fetch_efficency_slide_pct, rowId, fetch_efficency_slide_pct_pos);
				diffData.setValueAt(slideSumTupFetched,        rowId, tup_fetched_in_slide_pos);
				diffData.setValueAt(slideSumTupReturned,       rowId, tup_returned_in_slide_pos);
				diffData.setValueAt(timeStr,                   rowId, slide_time_pos);
			}
		}
	}

	private void removeOldSlideEntries(LinkedList<SlideEntry> list, Timestamp sampleTime)
	{
		long slideTimeInMs = 1000 * Configuration.getCombinedConfiguration().getIntProperty(PROPKEY_SlideTimeInSec, DEFAULT_SlideTimeInSec);

		while( ! list.isEmpty() )
		{
			long ageInMs = sampleTime.getTime() - list.getFirst()._sampleTime.getTime(); // note: list.add() is adding entries to the *end* of the list
			if (ageInMs > slideTimeInMs)
				list.removeFirst(); // note: list.add() is adding entries to the *end* of the list, so removeFirst() is removing the oldest entry 
			else
				break;
		}
	}
	private SlideEntry sumSlideEntries(LinkedList<SlideEntry> list)
	{
		SlideEntry returnEntry = new SlideEntry();
		
		returnEntry._tup_fetched  = 0;
		returnEntry._tup_returned = 0;
		returnEntry._sampleTime   = null;

		for (SlideEntry entry : list)
		{
			returnEntry._tup_fetched  += entry._tup_fetched;
			returnEntry._tup_returned += entry._tup_returned;
		}
		return returnEntry;
	}
	private String getTimeSpanSlideEntries(LinkedList<SlideEntry> list)
	{
		if (list.isEmpty())
			return "00:00:00";

		SlideEntry firstEntry = list.getFirst(); // oldest entry
		SlideEntry lastEntry  = list.getLast();  // last added

		long timeDiff = lastEntry._sampleTime.getTime() - firstEntry._sampleTime.getTime();
		return TimeUtils.msToTimeStr("%HH:%MM:%SS", timeDiff);
	}
	
	
	private void addTrendGraphs()
	{
		addTrendGraph(GRAPH_NAME_FETCH_EFFECIENT_PCT,
			"Row Fetch Efficiency", 	                // Menu CheckBox text
			"Row Fetch Efficiency (tup_fetched/tup_returned) in Percent ("+SHORT_NAME+")", // Graph Label 
			TrendGraphDataPoint.createGraphProps(TrendGraphDataPoint.Y_AXIS_SCALE_LABELS_PERCENT, CentralPersistReader.SampleType.MAX_OVER_SAMPLES),
			null, LabelType.Dynamic, 
			TrendGraphDataPoint.Category.CPU,
			true, // is Percent Graph
			false, // visible at start
			0,     // graph is valid from Server Version. 0 = All Versions; >0 = Valid from this version and above 
			-1);   // minimum height

		addTrendGraph(GRAPH_NAME_FETCH_EFFECIENT_SLIDE_PCT,
			"Row Fetch Efficiency SLIDE", 	                // Menu CheckBox text
			"Row Fetch Efficiency SLIDE (tup_fetched/tup_returned) in Percent ("+SHORT_NAME+")", // Graph Label 
			TrendGraphDataPoint.createGraphProps(TrendGraphDataPoint.Y_AXIS_SCALE_LABELS_PERCENT, CentralPersistReader.SampleType.MAX_OVER_SAMPLES),
			null, LabelType.Dynamic, 
			TrendGraphDataPoint.Category.CPU,
			true, // is Percent Graph
			true, // visible at start
			0,     // graph is valid from Server Version. 0 = All Versions; >0 = Valid from this version and above 
			-1);   // minimum height

		addTrendGraph(GRAPH_NAME_FETCH_EFFECIENT_ABS_PCT,
			"Row Fetch Efficiency ABS", 	                // Menu CheckBox text
			"Row Fetch Efficiency ABS (tup_fetched/tup_returned) in Percent ("+SHORT_NAME+")", // Graph Label 
			TrendGraphDataPoint.createGraphProps(TrendGraphDataPoint.Y_AXIS_SCALE_LABELS_PERCENT, CentralPersistReader.SampleType.MAX_OVER_SAMPLES),
			null, LabelType.Dynamic, 
			TrendGraphDataPoint.Category.CPU,
			true, // is Percent Graph
			false, // visible at start
			0,     // graph is valid from Server Version. 0 = All Versions; >0 = Valid from this version and above 
			-1);   // minimum height

		addTrendGraph(GRAPH_NAME_FETCH_EFFECIENT_SUM,
			"Row Fetch Efficiency SUM", 	                // Menu CheckBox text
			"Row Fetch Efficiency SUM [tup_returned, tup_fetched] ("+SHORT_NAME+")", // Graph Label 
			TrendGraphDataPoint.createGraphProps(TrendGraphDataPoint.Y_AXIS_SCALE_LABELS_NORMAL, CentralPersistReader.SampleType.MAX_OVER_SAMPLES),
			new String[] {"tup_returned (read)", "tup_fetched (to-client)"}, 
			LabelType.Static, 
			TrendGraphDataPoint.Category.CPU,
			false, // is Percent Graph
			true, // visible at start
			0,     // graph is valid from Server Version. 0 = All Versions; >0 = Valid from this version and above 
			-1);   // minimum height

		addTrendGraph(GRAPH_NAME_CONNECTIONS,
			"Connections", 	                // Menu CheckBox text
			"Connections ("+SHORT_NAME+")", // Graph Label 
			TrendGraphDataPoint.createGraphProps(TrendGraphDataPoint.Y_AXIS_SCALE_LABELS_NORMAL, CentralPersistReader.SampleType.MAX_OVER_SAMPLES),
			null, LabelType.Dynamic, 
			TrendGraphDataPoint.Category.SRV_CONFIG,
			false, // is Percent Graph
			true, // visible at start
			0,     // graph is valid from Server Version. 0 = All Versions; >0 = Valid from this version and above 
			-1);   // minimum height

		addTrendGraph(GRAPH_NAME_CONNECTIONS_SUM,
			"Connections Sum", 	                // Menu CheckBox text
			"Connections Sum ("+SHORT_NAME+")", // Graph Label 
			TrendGraphDataPoint.createGraphProps(TrendGraphDataPoint.Y_AXIS_SCALE_LABELS_NORMAL, CentralPersistReader.SampleType.MAX_OVER_SAMPLES),
			new String[] {"srv_config: max_connections", "numbackends"},
			LabelType.Static, 
			TrendGraphDataPoint.Category.SRV_CONFIG,
			false, // is Percent Graph
			true, // visible at start
			0,     // graph is valid from Server Version. 0 = All Versions; >0 = Valid from this version and above 
			-1);   // minimum height

		addTrendGraph(GRAPH_NAME_COMMITS,
			"Commits", 	                           // Menu CheckBox text
			"Commits per second ("+SHORT_NAME+")", // Graph Label 
			TrendGraphDataPoint.createGraphProps(TrendGraphDataPoint.Y_AXIS_SCALE_LABELS_PERSEC, CentralPersistReader.SampleType.MAX_OVER_SAMPLES),
			null, LabelType.Dynamic, 
			TrendGraphDataPoint.Category.OPERATIONS,
			false, // is Percent Graph
			true, // visible at start
			0,     // graph is valid from Server Version. 0 = All Versions; >0 = Valid from this version and above 
			-1);   // minimum height

		addTrendGraph(GRAPH_NAME_ROLLBACKS,
			"Rollbacks", 	                         // Menu CheckBox text
			"Rollbacks per second ("+SHORT_NAME+")", // Graph Label 
			TrendGraphDataPoint.createGraphProps(TrendGraphDataPoint.Y_AXIS_SCALE_LABELS_PERSEC, CentralPersistReader.SampleType.MAX_OVER_SAMPLES),
			null, LabelType.Dynamic, 
			TrendGraphDataPoint.Category.OPERATIONS,
			false, // is Percent Graph
			true, // visible at start
			0,     // graph is valid from Server Version. 0 = All Versions; >0 = Valid from this version and above 
			-1);   // minimum height

		addTrendGraph(GRAPH_NAME_CACHE_HIT_PCT,
			"Data Cache Hit Percent", 	                          // Menu CheckBox text
			"Data Cache Hit Percent ("+SHORT_NAME+")",            // Graph Label 
			TrendGraphDataPoint.createGraphProps(TrendGraphDataPoint.Y_AXIS_SCALE_LABELS_PERCENT, CentralPersistReader.SampleType.MAX_OVER_SAMPLES),
			null, LabelType.Dynamic, 
			TrendGraphDataPoint.Category.OPERATIONS,
			false, // is Percent Graph
			true, // visible at start
			0,     // graph is valid from Server Version. 0 = All Versions; >0 = Valid from this version and above 
			-1);   // minimum height

		addTrendGraph(GRAPH_NAME_TOTAL_READS,
			"Cache Total Reads (blks_read+blks_hit)", 	                           // Menu CheckBox text
			"Cache Total Reads (blks_read+blks_hit) Per Second ("+SHORT_NAME+")",  // Graph Label 
			TrendGraphDataPoint.createGraphProps(TrendGraphDataPoint.Y_AXIS_SCALE_LABELS_PERSEC, CentralPersistReader.SampleType.MAX_OVER_SAMPLES),
			null, LabelType.Dynamic, 
			TrendGraphDataPoint.Category.OPERATIONS,
			false, // is Percent Graph
			true, // visible at start
			0,     // graph is valid from Server Version. 0 = All Versions; >0 = Valid from this version and above 
			-1);   // minimum height

		addTrendGraph(GRAPH_NAME_READS,
			"Physical Reads (blks_read)", 	                          // Menu CheckBox text
			"Physical Reads (blks_read) per second ("+SHORT_NAME+")", // Graph Label 
			TrendGraphDataPoint.createGraphProps(TrendGraphDataPoint.Y_AXIS_SCALE_LABELS_PERSEC, CentralPersistReader.SampleType.MAX_OVER_SAMPLES),
			null, LabelType.Dynamic, 
			TrendGraphDataPoint.Category.DISK,
			false, // is Percent Graph
			true, // visible at start
			0,     // graph is valid from Server Version. 0 = All Versions; >0 = Valid from this version and above 
			-1);   // minimum height

		addTrendGraph(GRAPH_NAME_CACHE_HITS,
			"Cache Read Hits (blks_hit)", 	                          // Menu CheckBox text
			"Cache Read Hits (blks_hit) per second ("+SHORT_NAME+")", // Graph Label 
			TrendGraphDataPoint.createGraphProps(TrendGraphDataPoint.Y_AXIS_SCALE_LABELS_PERSEC, CentralPersistReader.SampleType.MAX_OVER_SAMPLES),
			null, LabelType.Dynamic, 
			TrendGraphDataPoint.Category.CACHE,
			false, // is Percent Graph
			true, // visible at start
			0,     // graph is valid from Server Version. 0 = All Versions; >0 = Valid from this version and above 
			-1);   // minimum height

		addTrendGraph(GRAPH_NAME_ROWS_RETURNED,
			"Rows Returned (rows used after filtering)", 	                         // Menu CheckBox text
			"Rows Returned (rows used after filtering) per second ("+SHORT_NAME+")", // Graph Label 
			TrendGraphDataPoint.createGraphProps(TrendGraphDataPoint.Y_AXIS_SCALE_LABELS_PERSEC, CentralPersistReader.SampleType.MAX_OVER_SAMPLES),
			null, LabelType.Dynamic, 
			TrendGraphDataPoint.Category.CPU,
			false, // is Percent Graph
			true, // visible at start
			0,     // graph is valid from Server Version. 0 = All Versions; >0 = Valid from this version and above 
			-1);   // minimum height

		addTrendGraph(GRAPH_NAME_ROWS_FETCHED,
			"Rows Fetched (rows read before filter)", 	                          // Menu CheckBox text
			"Rows Fetched (rows read before filter) per second ("+SHORT_NAME+")", // Graph Label 
			TrendGraphDataPoint.createGraphProps(TrendGraphDataPoint.Y_AXIS_SCALE_LABELS_PERSEC, CentralPersistReader.SampleType.MAX_OVER_SAMPLES),
			null, LabelType.Dynamic, 
			TrendGraphDataPoint.Category.CPU,
			false, // is Percent Graph
			true, // visible at start
			0,     // graph is valid from Server Version. 0 = All Versions; >0 = Valid from this version and above 
			-1);   // minimum height

		addTrendGraph(GRAPH_NAME_ROWS_INSERTS,
			"Rows Inserted", 	                         // Menu CheckBox text
			"Rows Inserted per second ("+SHORT_NAME+")", // Graph Label 
			TrendGraphDataPoint.createGraphProps(TrendGraphDataPoint.Y_AXIS_SCALE_LABELS_PERSEC, CentralPersistReader.SampleType.MAX_OVER_SAMPLES),
			null, LabelType.Dynamic, 
			TrendGraphDataPoint.Category.OPERATIONS,
			false, // is Percent Graph
			true, // visible at start
			0,     // graph is valid from Server Version. 0 = All Versions; >0 = Valid from this version and above 
			-1);   // minimum height

		addTrendGraph(GRAPH_NAME_ROWS_UPDATED,
			"Rows Updated", 	                         // Menu CheckBox text
			"Rows Updated per second ("+SHORT_NAME+")", // Graph Label 
			TrendGraphDataPoint.createGraphProps(TrendGraphDataPoint.Y_AXIS_SCALE_LABELS_PERSEC, CentralPersistReader.SampleType.MAX_OVER_SAMPLES),
			null, LabelType.Dynamic, 
			TrendGraphDataPoint.Category.OPERATIONS,
			false, // is Percent Graph
			true, // visible at start
			0,     // graph is valid from Server Version. 0 = All Versions; >0 = Valid from this version and above 
			-1);   // minimum height

		addTrendGraph(GRAPH_NAME_ROWS_DELETED,
			"Rows Deleted", 	                         // Menu CheckBox text
			"Rows Deleted per second ("+SHORT_NAME+")", // Graph Label 
			TrendGraphDataPoint.createGraphProps(TrendGraphDataPoint.Y_AXIS_SCALE_LABELS_PERSEC, CentralPersistReader.SampleType.MAX_OVER_SAMPLES),
			null, LabelType.Dynamic, 
			TrendGraphDataPoint.Category.OPERATIONS,
			false, // is Percent Graph
			true, // visible at start
			0,     // graph is valid from Server Version. 0 = All Versions; >0 = Valid from this version and above 
			-1);   // minimum height

		addTrendGraph(GRAPH_NAME_ROWS_INS_UPD_DEL,
			"Rows Inserted, Updated and Deleted", 	                         // Menu CheckBox text
			"Rows Inserted, Updated and Deleted per second ("+SHORT_NAME+")", // Graph Label 
			TrendGraphDataPoint.createGraphProps(TrendGraphDataPoint.Y_AXIS_SCALE_LABELS_PERSEC, CentralPersistReader.SampleType.MAX_OVER_SAMPLES),
			null, LabelType.Dynamic, 
			TrendGraphDataPoint.Category.OPERATIONS,
			false, // is Percent Graph
			true, // visible at start
			0,     // graph is valid from Server Version. 0 = All Versions; >0 = Valid from this version and above 
			-1);   // minimum height

		addTrendGraph(GRAPH_NAME_ROWS_CONFLICTS,
			"Conflicting Statements", 	                         // Menu CheckBox text
			"Conflicting Statements per second ("+SHORT_NAME+")", // Graph Label 
			TrendGraphDataPoint.createGraphProps(TrendGraphDataPoint.Y_AXIS_SCALE_LABELS_PERSEC, CentralPersistReader.SampleType.MAX_OVER_SAMPLES),
			null, LabelType.Dynamic, 
			TrendGraphDataPoint.Category.OPERATIONS,
			false, // is Percent Graph
			false, // visible at start
			0,     // graph is valid from Server Version. 0 = All Versions; >0 = Valid from this version and above 
			-1);   // minimum height

		addTrendGraph(GRAPH_NAME_TEMP_FILES,
			"Temp Files Created", 	                         // Menu CheckBox text
			"Temp Files Created per second ("+SHORT_NAME+")", // Graph Label 
			TrendGraphDataPoint.createGraphProps(TrendGraphDataPoint.Y_AXIS_SCALE_LABELS_PERSEC, CentralPersistReader.SampleType.MAX_OVER_SAMPLES),
			null, LabelType.Dynamic, 
			TrendGraphDataPoint.Category.DISK,
			false, // is Percent Graph
			true, // visible at start
			0,     // graph is valid from Server Version. 0 = All Versions; >0 = Valid from this version and above 
			-1);   // minimum height

		addTrendGraph(GRAPH_NAME_TEMP_BYTES,
			"Temp Bytes", 	                         // Menu CheckBox text
			"Temp Bytes per second ("+SHORT_NAME+")", // Graph Label 
			TrendGraphDataPoint.createGraphProps(TrendGraphDataPoint.Y_AXIS_SCALE_LABELS_PERSEC, CentralPersistReader.SampleType.MAX_OVER_SAMPLES),
			null, LabelType.Dynamic, 
			TrendGraphDataPoint.Category.DISK,
			false, // is Percent Graph
			true, // visible at start
			0,     // graph is valid from Server Version. 0 = All Versions; >0 = Valid from this version and above 
			-1);   // minimum height

		addTrendGraph(GRAPH_NAME_DEADLOCKS,
			"Deadlocks", 	                         // Menu CheckBox text
			"Deadlocks per second ("+SHORT_NAME+")", // Graph Label 
			TrendGraphDataPoint.createGraphProps(TrendGraphDataPoint.Y_AXIS_SCALE_LABELS_PERSEC, CentralPersistReader.SampleType.MAX_OVER_SAMPLES),
			null, LabelType.Dynamic, 
			TrendGraphDataPoint.Category.OPERATIONS,
			false, // is Percent Graph
			false, // visible at start
			0,     // graph is valid from Server Version. 0 = All Versions; >0 = Valid from this version and above 
			-1);   // minimum height

		addTrendGraph(GRAPH_NAME_READ_TIME,
			"Read Time", 	                         // Menu CheckBox text
			"Read Time per second ("+SHORT_NAME+")", // Graph Label 
			TrendGraphDataPoint.createGraphProps(TrendGraphDataPoint.Y_AXIS_SCALE_LABELS_PERSEC, CentralPersistReader.SampleType.MAX_OVER_SAMPLES),
			null, LabelType.Dynamic, 
			TrendGraphDataPoint.Category.DISK,
			false, // is Percent Graph
			true, // visible at start
			0,     // graph is valid from Server Version. 0 = All Versions; >0 = Valid from this version and above 
			-1);   // minimum height

		addTrendGraph(GRAPH_NAME_WRITE_TIME,
			"Write Time", 	                         // Menu CheckBox text
			"Write Time per second ("+SHORT_NAME+")", // Graph Label 
			TrendGraphDataPoint.createGraphProps(TrendGraphDataPoint.Y_AXIS_SCALE_LABELS_PERSEC, CentralPersistReader.SampleType.MAX_OVER_SAMPLES),
			null, LabelType.Dynamic, 
			TrendGraphDataPoint.Category.DISK,
			false, // is Percent Graph
			true, // visible at start
			0,     // graph is valid from Server Version. 0 = All Versions; >0 = Valid from this version and above 
			-1);   // minimum height

		addTrendGraph(GRAPH_NAME_DBSIZE_MB,
			"DB Size in MB", 	                         // Menu CheckBox text
			"DB Size in MB ("+SHORT_NAME+")", // Graph Label 
			TrendGraphDataPoint.createGraphProps(TrendGraphDataPoint.Y_AXIS_SCALE_LABELS_MB, CentralPersistReader.SampleType.MAX_OVER_SAMPLES),
			null, LabelType.Dynamic, 
			TrendGraphDataPoint.Category.SPACE,
			false, // is Percent Graph
			true, // visible at start
			0,     // graph is valid from Server Version. 0 = All Versions; >0 = Valid from this version and above 
			-1);   // minimum height
	}


	private void localUpdateGraphData(TrendGraphDataPoint tgdp, int dataType, boolean addSummaryRow, boolean doAverageSum, String colname)
	{
//		// Get database count (do dot include template databases)
//		int size = 0;
//		for (int i = 0; i < this.size(); i++)
//		{
//			String dbname = this.getAbsString(i, "datname");
//			if (dbname != null && !dbname.startsWith("template"))
//				size++;
//		}
			
//System.out.println("localUpdateGraphData(): rowCount="+getRowCount()+", size="+(this.size())+", adjSize="+(addSummaryRow ? this.size()+1 : this.size())+", name='"+tgdp.getName()+"', addSummaryRow="+addSummaryRow+", colname='"+colname+"'.");

		// Write 1 "line" for every db (except for 'template*' databases)
		Double[] dArray = new Double[ addSummaryRow ? this.size()+1 : this.size()];
		String[] lArray = new String[dArray.length];
		int ap = addSummaryRow ? 1 : 0;
		double sum = 0;

		int rc = this.size();
		for (int r = 0; r < rc; r++) // we still need to loop all rows...
		{
			String dbname = this.getAbsString(r, "datname");
			if (dbname != null && !dbname.startsWith("template"))
			{
				Double data;
				if      (dataType == CountersModel.DATA_ABS)  data = this.getAbsValueAsDouble (r, colname);
				else if (dataType == CountersModel.DATA_DIFF) data = this.getDiffValueAsDouble(r, colname);
				else if (dataType == CountersModel.DATA_RATE) data = this.getRateValueAsDouble(r, colname);
				else throw new RuntimeException("dataType(tgName="+tgdp.getName()+"): Unsupported dataType="+dataType);
				
				if (data != null)
					sum += data;

//System.out.println("     xxx: r="+r+", rc="+rc+", ap="+ap+", size()="+size()+", lArray.length="+lArray.length+", dArray.length="+dArray.length+", dbname='"+dbname+"'.");
				lArray[ap] = dbname;
				dArray[ap] = data;
				ap++;
			}
		}

		if (addSummaryRow && this.size() > 0)
		{
			String sumLabel = "ALL-DBs";
			if (doAverageSum)
			{
				sumLabel = "AVG-ALL-DBs";
				sum = sum / (this.size() * 1.0);
			}
				
			lArray[0] = sumLabel;
			dArray[0] = sum;
		}

		// Set the values
		tgdp.setDataPoint(this.getTimestamp(), lArray, dArray);
	}
	
	@Override
	public void updateGraphData(TrendGraphDataPoint tgdp)
	{
		if (GRAPH_NAME_FETCH_EFFECIENT_PCT      .equals(tgdp.getName())) localUpdateGraphData(tgdp, CountersModel.DATA_RATE, true , true , "fetch_efficency_pct");
		if (GRAPH_NAME_FETCH_EFFECIENT_SLIDE_PCT.equals(tgdp.getName())) localUpdateGraphData(tgdp, CountersModel.DATA_RATE, true , true , "fetch_efficency_slide_pct");
		if (GRAPH_NAME_FETCH_EFFECIENT_ABS_PCT  .equals(tgdp.getName())) localUpdateGraphData(tgdp, CountersModel.DATA_ABS,  true , true , "fetch_efficency_pct");
		if (GRAPH_NAME_CONNECTIONS              .equals(tgdp.getName())) localUpdateGraphData(tgdp, CountersModel.DATA_ABS,  false, false, "numbackends");
		if (GRAPH_NAME_COMMITS                  .equals(tgdp.getName())) localUpdateGraphData(tgdp, CountersModel.DATA_RATE, true , false, "xact_commit");
		if (GRAPH_NAME_ROLLBACKS                .equals(tgdp.getName())) localUpdateGraphData(tgdp, CountersModel.DATA_RATE, true , false, "xact_rollback");
		if (GRAPH_NAME_CACHE_HIT_PCT            .equals(tgdp.getName())) localUpdateGraphData(tgdp, CountersModel.DATA_RATE, true , true , "cache_hit_pct");
		if (GRAPH_NAME_TOTAL_READS              .equals(tgdp.getName())) localUpdateGraphData(tgdp, CountersModel.DATA_RATE, true , false, "total_reads");
		if (GRAPH_NAME_READS                    .equals(tgdp.getName())) localUpdateGraphData(tgdp, CountersModel.DATA_RATE, true , false, "blks_read");
		if (GRAPH_NAME_CACHE_HITS               .equals(tgdp.getName())) localUpdateGraphData(tgdp, CountersModel.DATA_RATE, true , false, "blks_hit");
		if (GRAPH_NAME_ROWS_RETURNED            .equals(tgdp.getName())) localUpdateGraphData(tgdp, CountersModel.DATA_RATE, true , false, "tup_returned");
		if (GRAPH_NAME_ROWS_FETCHED             .equals(tgdp.getName())) localUpdateGraphData(tgdp, CountersModel.DATA_RATE, true , false, "tup_fetched");
		if (GRAPH_NAME_ROWS_INSERTS             .equals(tgdp.getName())) localUpdateGraphData(tgdp, CountersModel.DATA_RATE, true , false, "tup_inserted");
		if (GRAPH_NAME_ROWS_UPDATED             .equals(tgdp.getName())) localUpdateGraphData(tgdp, CountersModel.DATA_RATE, true , false, "tup_updated");
		if (GRAPH_NAME_ROWS_DELETED             .equals(tgdp.getName())) localUpdateGraphData(tgdp, CountersModel.DATA_RATE, true , false, "tup_deleted");
		if (GRAPH_NAME_ROWS_CONFLICTS           .equals(tgdp.getName())) localUpdateGraphData(tgdp, CountersModel.DATA_RATE, true , false, "conflicts");
		if (GRAPH_NAME_TEMP_FILES               .equals(tgdp.getName())) localUpdateGraphData(tgdp, CountersModel.DATA_RATE, true , false, "temp_files");
		if (GRAPH_NAME_TEMP_BYTES               .equals(tgdp.getName())) localUpdateGraphData(tgdp, CountersModel.DATA_RATE, true , false, "temp_bytes");
		if (GRAPH_NAME_DEADLOCKS                .equals(tgdp.getName())) localUpdateGraphData(tgdp, CountersModel.DATA_RATE, true , false, "deadlocks");
		if (GRAPH_NAME_READ_TIME                .equals(tgdp.getName())) localUpdateGraphData(tgdp, CountersModel.DATA_RATE, true , false, "blk_read_time");
		if (GRAPH_NAME_WRITE_TIME               .equals(tgdp.getName())) localUpdateGraphData(tgdp, CountersModel.DATA_RATE, true , false, "blk_write_time");
		if (GRAPH_NAME_DBSIZE_MB                .equals(tgdp.getName())) localUpdateGraphData(tgdp, CountersModel.DATA_ABS,  true , false, "dbsize_mb");

		if (GRAPH_NAME_ROWS_INS_UPD_DEL         .equals(tgdp.getName())) 
		{
			// This basically does the same as in localUpdateGraphData(), but it gets 3 counters and combine them into 1 data point 
			boolean addSummaryRow = true;

			// Write 1 "line" for every db (except for 'template*' databases)
			Double[] dArray = new Double[ addSummaryRow ? this.size()+1 : this.size()];
			String[] lArray = new String[dArray.length];
			int ap = addSummaryRow ? 1 : 0;
			double sum = 0;

			int rc = this.size();
			for (int r = 0; r < rc; r++) // we still need to loop all rows...
			{
				String dbname = this.getAbsString(r, "datname");
				if (dbname != null && !dbname.startsWith("template"))
				{
					Double tup_inserted = this.getRateValueAsDouble(r, "tup_inserted");
					Double tup_updated  = this.getRateValueAsDouble(r, "tup_updated");
					Double tup_deleted  = this.getRateValueAsDouble(r, "tup_deleted");

					Double data = tup_inserted.doubleValue() + tup_updated.doubleValue() + tup_deleted.doubleValue();
					
					if (data != null)
						sum += data;

					lArray[ap] = dbname;
					dArray[ap] = data;
					ap++;
				}
			}

			if (addSummaryRow && this.size() > 0)
			{
				String sumLabel = "ALL-DBs";
					
				lArray[0] = sumLabel;
				dArray[0] = sum;
			}

			// Set the values
			tgdp.setDataPoint(this.getTimestamp(), lArray, dArray);
		}

		if (GRAPH_NAME_CONNECTIONS_SUM.equals(tgdp.getName()))
		{
			Double[] arr = new Double[2];

			arr[0] = this.getAbsValueMax("srv_cfg_max_connections");
			arr[1] = this.getAbsValueSum("numbackends");

			// Set the values
			tgdp.setDataPoint(this.getTimestamp(), arr);
		}

		if (GRAPH_NAME_FETCH_EFFECIENT_SUM.equals(tgdp.getName()))
		{
			Double[] arr = new Double[2];

			arr[0] = this.getRateValueSum("tup_returned");
			arr[1] = this.getRateValueSum("tup_fetched");

			// Set the values
			tgdp.setDataPoint(this.getTimestamp(), arr);
		}
	}

	@Override
	public void sendAlarmRequest()
	{
		if ( ! hasAbsData() )
			return;
		
		if ( ! AlarmHandler.hasInstance() )
			return;

		boolean debugPrint = Configuration.getCombinedConfiguration().getBooleanProperty("sendAlarmRequest.debug", _logger.isDebugEnabled());

		CountersModel cm = this;

		//-------------------------------------------------------
		// free_connections
		//-------------------------------------------------------
		if (isSystemAlarmsForColumnEnabledAndInTimeRange("free_connections"))
		{
			Double srvCfgMaxConnections = cm.getAbsValueMax("srv_cfg_max_connections");
			Double sumNumbackends       = cm.getAbsValueSum("numbackends");
			
			if (srvCfgMaxConnections != null && sumNumbackends != null)
			{
				double numFree = srvCfgMaxConnections - sumNumbackends;
				double pctUsed = 100.0 - ((sumNumbackends / srvCfgMaxConnections) * 100.0);
				
				int threshold = Configuration.getCombinedConfiguration().getIntProperty(PROPKEY_alarm_FreeConnections, DEFAULT_alarm_FreeConnections);

				if (debugPrint || _logger.isDebugEnabled())
					System.out.println("##### sendAlarmRequest("+cm.getName()+"): free_connections: threshold="+threshold+", numFree='"+numFree+"'.");

				if (numFree < threshold)
				{
					String extendedDescText = "";
					String extendedDescHtml =               cm.getGraphDataHistoryAsHtmlImage(GRAPH_NAME_CONNECTIONS_SUM);
					       extendedDescHtml += "<br><br>" + cm.getGraphDataHistoryAsHtmlImage(GRAPH_NAME_CONNECTIONS);

					AlarmEvent ae = new AlarmEventConfigResourceIsLow(cm, "max_connections", numFree, sumNumbackends, pctUsed, threshold);
					ae.setExtendedDescription(extendedDescText, extendedDescHtml);

					AlarmHandler.getInstance().addAlarm(ae);
				}
			}
		}

		//-------------------------------------------------------
		// Get needed column positions, but do not "fail/skip" if they don't exists (they are not present in some versions)
		int pos__checksum_failures_diff = cm.findColumn("checksum_failures_diff");

		//-------------------------------------------------------
		// Below we need to loop AL the rows
		for (int r=0; r<cm.getDiffRowCount(); r++)
		{
			//-------------------------------------------------------
			// ChecksumFailures (only from version 12)
			//-------------------------------------------------------
			if (isSystemAlarmsForColumnEnabledAndInTimeRange("checksum_failures") && pos__checksum_failures_diff != -1)
			{
				int checksum_failures_diff = cm.getDiffValueAsInteger(r, pos__checksum_failures_diff, 0);

				int threshold = Configuration.getCombinedConfiguration().getIntProperty(PROPKEY_alarm_ChecksumFailures, DEFAULT_alarm_ChecksumFailures);

				if (debugPrint || _logger.isDebugEnabled())
					System.out.println("##### sendAlarmRequest("+cm.getName()+"): checksum_failures: threshold=" + threshold + ", checksum_failures_diff='" + checksum_failures_diff + "'.");

				if (checksum_failures_diff > threshold)
				{
					String dbname                = cm.getDiffString       (r, "datname");
					String checksum_last_failure = cm.getDiffString       (r, "checksum_last_failure");
					int    checksum_failures_tot = cm.getAbsValueAsInteger(r, "checksum_failures", -1);

					AlarmEvent ae = new AlarmEventPgChecksumFailure(cm, dbname, checksum_failures_diff, checksum_last_failure, checksum_failures_tot, threshold);

					AlarmHandler.getInstance().addAlarm(ae);
				}
			} // end: ChecksumFailures

		} // end: loop-rows

	} // end: method

	@Override
	public boolean isGraphDataHistoryEnabled(String name)
	{
		// ENABLED for the following graphs
		if (GRAPH_NAME_CONNECTIONS    .equals(name)) return true;
		if (GRAPH_NAME_CONNECTIONS_SUM.equals(name)) return true;

		// default: DISABLED
		return false;
	}
	@Override
	public int getGraphDataHistoryTimeInterval(String name)
	{
		// Keep interval: default is 60 minutes
		return super.getGraphDataHistoryTimeInterval(name);
	}

	
	public static final String  PROPKEY_alarm_FreeConnections = CM_NAME + ".alarm.system.if.free_connections.lt";
	public static final int     DEFAULT_alarm_FreeConnections = 25;

	public static final String  PROPKEY_alarm_ChecksumFailures = CM_NAME + ".alarm.system.if.checksum_failures_diff.gt";
	public static final int     DEFAULT_alarm_ChecksumFailures = 0;

	@Override
	public List<CmSettingsHelper> getLocalAlarmSettings()
	{
		Configuration conf = Configuration.getCombinedConfiguration();
		List<CmSettingsHelper> list = new ArrayList<>();
		
		CmSettingsHelper.Type isAlarmSwitch = CmSettingsHelper.Type.IS_ALARM_SWITCH;
		
		list.add(new CmSettingsHelper("free_connections" , isAlarmSwitch, PROPKEY_alarm_FreeConnections , Integer.class, conf.getIntProperty(PROPKEY_alarm_FreeConnections , DEFAULT_alarm_FreeConnections ), DEFAULT_alarm_FreeConnections , "If 'free_connections' is less than this value, send 'AlarmEventConfigResourceIsLow'." ));
		list.add(new CmSettingsHelper("checksum_failures", isAlarmSwitch, PROPKEY_alarm_ChecksumFailures, Integer.class, conf.getIntProperty(PROPKEY_alarm_ChecksumFailures, DEFAULT_alarm_ChecksumFailures), DEFAULT_alarm_ChecksumFailures, "If 'checksum_failures_diff' is greater than this value, send 'AlarmEventPgChecksumFailure'." ));

		return list;
	}
}
