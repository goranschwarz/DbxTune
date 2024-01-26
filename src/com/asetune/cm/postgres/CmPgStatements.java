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

import java.awt.event.MouseEvent;
import java.sql.Types;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.log4j.Logger;

import com.asetune.ICounterController;
import com.asetune.IGuiController;
import com.asetune.cm.CmSettingsHelper;
import com.asetune.cm.CounterSample;
import com.asetune.cm.CounterSetTemplates;
import com.asetune.cm.CounterSetTemplates.Type;
import com.asetune.cm.CountersModel;
import com.asetune.cm.postgres.gui.CmPgStatementsPanel;
import com.asetune.gui.MainFrame;
import com.asetune.gui.TabularCntrPanel;
import com.asetune.pcs.PcsColumnOptions;
import com.asetune.pcs.PcsColumnOptions.ColumnType;
import com.asetune.pcs.PersistentCounterHandler;
import com.asetune.sql.ResultSetMetaDataCached;
import com.asetune.sql.ResultSetMetaDataCached.Entry;
import com.asetune.sql.SqlParserUtils;
import com.asetune.sql.conn.DbxConnection;
import com.asetune.sql.conn.info.DbmsVersionInfo;
import com.asetune.utils.Configuration;
import com.asetune.utils.NumberUtils;
import com.asetune.utils.StringUtil;
import com.asetune.utils.Ver;

/**
 * @author Goran Schwarz (goran_schwarz@hotmail.com)
 */
public class CmPgStatements
extends CountersModel
{
	private static Logger        _logger          = Logger.getLogger(CmPgStatements.class);
	private static final long    serialVersionUID = 1L;

	public static final String   CM_NAME          = CmPgStatements.class.getSimpleName();
	public static final String   SHORT_NAME       = "Statements";
	public static final String   HTML_DESC        = 
		"<html>" +
		"The pg_stat_statements module provides a means for tracking execution statistics of all SQL statements executed by a server." +
		"</html>";

	public static final String   GROUP_NAME       = MainFrame.TCP_GROUP_OBJECT_ACCESS;
	public static final String   GUI_ICON_FILE    = "images/"+CM_NAME+".png";

	public static final long     NEED_SRV_VERSION = 0;
	public static final long     NEED_CE_VERSION  = 0;

	public static final String[] MON_TABLES       = new String[] {"pg_stat_statements"};
	public static final String[] NEED_ROLES       = new String[] {};
	public static final String[] NEED_CONFIG      = new String[] {};

	public static final String[] PCT_COLUMNS      = new String[] { "cache_hit_pct" };
	public static final String[] DIFF_COLUMNS     = new String[] {
			"calls",
			"total_time",
			"total_plan_time", // from: 13.0
			"total_exec_time", // from: 13.0 and replaces "total_time"
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
			"blk_write_time",
			"wal_records",      // from: 13.0
			"wal_fpi",          // from: 13.0   FPI = Full Page Image
			"wal_bytes"         // from: 13.0
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

	// Column History: pg_stat_statements

	// ------- 8.4 -> 9.0 -- 8 new column
	//                      new: shared_blks_hit     -- Total number of shared blocks hits by the statement
	//                      new: shared_blks_read    -- Total number of shared blocks reads by the statement
	//                      new: shared_blks_written -- Total number of shared blocks writes by the statement
	//                      new: local_blks_hit      -- Total number of local blocks hits by the statement
	//                      new: local_blks_read     -- Total number of local blocks reads by the statement
	//                      new: local_blks_written  -- Total number of local blocks writes by the statement
	//                      new: temp_blks_read      -- Total number of temp blocks reads by the statement
	//                      new: temp_blks_written   -- Total number of temp blocks writes by the statement	
	// ------- 9.0 -> 9.1 -- No Change 
	// ------- 9.1 -> 9.2 -- 5 new column
	//                      new: shared_blks_dirtied -- Total number of shared blocks dirtied by the statement
	//                      new: shared_blks_written -- Total number of shared blocks written by the statement
	//                      new: local_blks_dirtied	 -- Total number of local blocks dirtied by the statement
	//                      new: blk_read_time	     -- Total time the statement spent reading blocks, in milliseconds (if track_io_timing is enabled, otherwise zero)
	//                      new: blk_write_time	     -- Total time the statement spent writing blocks, in milliseconds (if track_io_timing is enabled, otherwise zero)
	// ------- 9.2 -> 9.3 -- No Change 
	// ------- 9.3 -> 9.4 -- 1 new column
	//                      new: queryid             -- Internal hash code, computed from the statement's parse tree
	// ------- 9.4 -> 9.5 -- 4 new column            
	//                      new: min_time            -- Minimum time spent in the statement, in milliseconds
	//                      new: max_time            -- Maximum time spent in the statement, in milliseconds
	//                      new: mean_time           -- Mean time spent in the statement, in milliseconds
	//                      new: stddev_time         -- Population standard deviation of time spent in the statement, in milliseconds
	// ------- 9.5 -> 9.6 -- No Change 
	// ------- 9.6 -> 10 -- No Change 
	// -------  10 -> 11 -- No Change 
	// -------  11 -> 12 -- No Change 
	// -------  12 -> 13 -- 9 new column, 6 renamed columns
	//                      new: plans               -- Number of times the statement was planned                  (if pg_stat_statements.track_planning is enabled, otherwise zero)
	//                      new: total_plan_time     -- Total time spent planning the statement, in milliseconds   (if pg_stat_statements.track_planning is enabled, otherwise zero)
	//                      new: min_plan_time       -- Minimum time spent planning the statement, in milliseconds (if pg_stat_statements.track_planning is enabled, otherwise zero)
	//                      new: max_plan_time       -- Maximum time spent planning the statement, in milliseconds (if pg_stat_statements.track_planning is enabled, otherwise zero)
	//                      new: mean_plan_time      -- Mean time spent planning the statement, in milliseconds    (if pg_stat_statements.track_planning is enabled, otherwise zero)
	//                      new: stddev_plan_time    -- Population standard deviation of time spent planning the statement, in milliseconds (if pg_stat_statements.track_planning is enabled, otherwise zero)
	//                  renamed: total_time          >> changed to 'total_exec_time'
	//                  renamed: min_time            >> changed to 'min_exec_time'
	//                  renamed: max_time            >> changed to 'max_exec_time'
	//                  renamed: mean_time           >> changed to 'mean_exec_time'
	//                  renamed: stddev_time         >> changed to 'stddev_exec_time'
	//                      new: total_exec_time     -- Total   time spent executing the statement, in milliseconds
	//                      new: min_exec_time       -- Minimum time spent executing the statement, in milliseconds
	//                      new: max_exec_time       -- Maximum time spent executing the statement, in milliseconds
	//                      new: mean_exec_time      -- Mean    time spent executing the statement, in milliseconds
	//                      new: stddev_exec_time    -- Population standard deviation of time spent executing the statement, in milliseconds
	//                      new: wal_records         -- Total number of WAL records generated by the statement
	//                      new: wal_fpi             -- Total number of WAL full page images generated by the statement
	//                      new: wal_bytes           -- Total amount of WAL generated by the statement in bytes
	// -------  13 -> 14 -- 1 new column
	//                      new: toplevel            -- True if the query was executed as a top-level statement (always true if pg_stat_statements.track is set to top)
	// -------  14 -> 15 -- 15 not yet released 
	
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
		
//		addTrendGraphs();
		
		CounterSetTemplates.register(this);
	}


	@Override
	protected TabularCntrPanel createGui()
	{
		return new CmPgStatementsPanel(this);
	}
	
	//------------------------------------------------------------
	// Implementation
	//------------------------------------------------------------
	private static final String  PROP_PREFIX                = CM_NAME;

	public static final String  PROPKEY_createExtension     = PROP_PREFIX + ".if.missing.pg_stat_statements.createExtension";
	public static final boolean DEFAULT_createExtension     = true; 

	public static final String  PROPKEY_sample_total_time_gt = PROP_PREFIX + ".sample.total_time.gt";
	public static final int     DEFAULT_sample_total_time_gt = 1000;
	
	public static final String  PROPKEY_view_parseAndSendTables = PROP_PREFIX + ".ddlStore.parse.send.tables";
	public static final boolean DEFAULT_view_parseAndSendTables = true;

	public static final String  PROPKEY_view_parseAndSendTables_topTotalTimeRowcount = PROP_PREFIX + ".ddlStore.parse.send.tables.top.total_time.rowcount";
	public static final int     DEFAULT_view_parseAndSendTables_topTotalTimeRowcount = 10;
	
	// Structure to "cache" what QueryID's that already has been parsed
	private Set<String> _alreadyParsedQueryId_cache = new HashSet<>();


//	public static final String GRAPH_NAME_SQL_STATEMENTS = "SqlStatements";
	

//	private void addTrendGraphs()
//	{
//		addTrendGraph(GRAPH_NAME_SQL_STATEMENTS,
//			"SQL Statement Calls", 	                           // Menu CheckBox text
//			"SQL Statement Calls per second ("+SHORT_NAME+")", // Graph Label 
//			TrendGraphDataPoint.Y_AXIS_SCALE_LABELS_PERSEC,
//			new String[] {"calls"}, 
//			LabelType.Static, 
//			TrendGraphDataPoint.Category.OPERATIONS,
//			false, // is Percent Graph
//			false, // visible at start
//			0,     // graph is valid from Server Version. 0 = All Versions; >0 = Valid from this version and above 
//			-1);   // minimum height
//	}
//
//	@Override
//	public void updateGraphData(TrendGraphDataPoint tgdp)
//	{
//		if (GRAPH_NAME_SQL_STATEMENTS.equals(tgdp.getName()))
//		{
//			Double[] arr = new Double[1];
//
//			arr[0] = this.getRateValueSum("calls");
//
//			// Set the values
//			tgdp.setDataPoint(this.getTimestamp(), arr);
//		}
//	}

	/** Used by the: Create 'Offline Session' Wizard */
	@Override
	public List<CmSettingsHelper> getLocalSettings()
	{
		Configuration conf = Configuration.getCombinedConfiguration();
		List<CmSettingsHelper> list = new ArrayList<>();
		
		list.add(new CmSettingsHelper("Sample 'total_time' above", PROPKEY_sample_total_time_gt  , Integer.class, conf.getIntProperty(PROPKEY_sample_total_time_gt  , DEFAULT_sample_total_time_gt  ), DEFAULT_sample_total_time_gt  , "Sample 'total_time' above" ));

		return list;
	}

	@Override
	public Map<String, PcsColumnOptions> getPcsColumnOptions()
	{
		Map<String, PcsColumnOptions> map = super.getPcsColumnOptions();

		// No settings in the super, create one, and set it at super
		if (map == null)
		{
			map = new HashMap<>();
			map.put("query", new PcsColumnOptions(ColumnType.DICTIONARY_COMPRESSION));

			// Set the map in the super
			setPcsColumnOptions(map);
		}

		return map;
	}

	
	@Override
	public List<String> getPkForVersion(DbxConnection conn, DbmsVersionInfo versionInfo)
	{
		List <String> pkCols = new LinkedList<String>();

		pkCols.add("userid");
		pkCols.add("dbid");
		pkCols.add("queryid");

		return pkCols;
	}

	@Override
	public String getSqlForVersion(DbxConnection conn, DbmsVersionInfo versionInfo)
	{
		long srvVersion = versionInfo.getLongVersion();

		// 'queryid' was introduced in 9.4 so lets simulate a query id in earlier versions
		String queryid = "";
		if (srvVersion < Ver.ver(9,4))
		{
//			queryid = "    ,md5(query) as queryid \n";
			queryid = "    ,('x'||substr(md5(query),1,16))::bit(64)::bigint as queryid \n";
		}

		String total_time1 = "s.total_time";
		String total_time2 = "";
		if (srvVersion >= Ver.ver(13))
		{
			total_time1 = "(s.total_plan_time + s.total_exec_time)";
			total_time2 = "    ,(s.total_plan_time + s.total_exec_time) AS total_time \n";
		}

		int total_time_gt = Configuration.getCombinedConfiguration().getIntProperty(PROPKEY_sample_total_time_gt, DEFAULT_sample_total_time_gt);

		//TODO: look at 'pg_show_plans' at https://github.com/cybertec-postgresql/pg_show_plans
		//      to get "runtime" plans (just like sp_showplan)
		//      Possibly we can incorporate the in the below SQL statement (or get it in a separate collection tab)

		return  "select \n" +
				"     CASE WHEN s.calls > 0 THEN " + total_time1 + "      / s.calls ELSE 0 END as avg_time_per_call \n" +
				"    ,CASE WHEN s.calls > 0 THEN s.rows            / s.calls ELSE 0 END as avg_rows_per_call \n" +
				"    ,CASE WHEN s.rows  > 0 THEN s.shared_blks_hit / s.rows  ELSE 0 END as shared_blks_hit_per_row \n" +
				"    ,CAST(100.0 * s.shared_blks_hit / nullif(s.shared_blks_hit + s.shared_blks_read, 0) as numeric(5,1)) AS cache_hit_pct \n" +
				"    ,d.datname \n" +
				"    ,u.usename \n" +
				queryid +
				total_time2 +
				"    ,s.* \n" +
				"    ,cast(0 as integer) AS \"dupMergeCount\" \n" +
				"from pg_stat_statements s \n" +
				"left outer join pg_catalog.pg_database d ON s.dbid   = d.oid      \n" +
				"left outer join pg_catalog.pg_user     u ON s.userid = u.usesysid \n" +
//				"where s.calls > 1 \n" +
				"where " + total_time1 + " > " + total_time_gt + " \n" +
				"";
	}
	
	@Override
	public ResultSetMetaDataCached modifyResultSetMetaData(ResultSetMetaDataCached rsmdc)
	{
		for (Entry entry : rsmdc.getEntries())
		{
			if (    "wal_bytes".equals(entry.getColumnLabel()) 
			     && entry.getColumnType() == Types.NUMERIC 
			     && entry.getPrecision() == 0 
			     && entry.getScale() == 0
			   ) 
			{
				entry.setPrecision(38);
				entry.setScale(0);

				_logger.info("modifyResultSetMetaData: Cm='" + getName() + "', columnName='" + entry.getColumnLabel() + "', changing data type PRECISION from 0 to 38");
			}
		}
		
		return rsmdc;
	}

	@Override
	public void localCalculation(CounterSample prevSample, CounterSample newSample, CounterSample diffData)
	{
		// Reset missing columns count so we can check it later.
		findCsColumn_resetMissingCounter();

		// Get column positions
		int avg_time_per_call_pos       = findCsColumn(diffData, true, "avg_time_per_call");
		int avg_rows_per_call_pos       = findCsColumn(diffData, true, "avg_rows_per_call");
		int shared_blks_hit_per_row_pos = findCsColumn(diffData, true, "shared_blks_hit_per_row");
		int cache_hit_pct_pos           = findCsColumn(diffData, true, "cache_hit_pct");

		int rows_pos                    = findCsColumn(diffData, true, "rows");
		int calls_pos                   = findCsColumn(diffData, true, "calls");
		int total_time_pos              = findCsColumn(diffData, true, "total_time", "total_exec_time"); // any of the two: total_exec_time is in pg version 13 and above
		int shared_blks_hit_pos         = findCsColumn(diffData, true, "shared_blks_hit");
		int shared_blks_read_pos        = findCsColumn(diffData, true, "shared_blks_read");


		// return if we are missing any columns.
		if (findCsColumn_hasMissingColumns())
		{
			_logger.error(getName() + ": Missing " + findCsColumn_getMissingColumns() + " columns when doing localCalculation(prevSample, newSample, diffData) skipping the local Calculation.");
			return;
		}
		
		// Loop all rows and calculate new values for: avg_time_per_call, avg_rows_per_call, shared_blks_hit_per_row
		for (int rowId = 0; rowId < diffData.getRowCount(); rowId++)
		{
			long   rows             = ((Number) diffData.getValueAt(rowId, rows_pos             )).longValue();
			long   calls            = ((Number) diffData.getValueAt(rowId, calls_pos            )).longValue();
			double total_time       = ((Number) diffData.getValueAt(rowId, total_time_pos       )).doubleValue();
			long   shared_blks_hit  = ((Number) diffData.getValueAt(rowId, shared_blks_hit_pos  )).longValue();
			long   shared_blks_read = ((Number) diffData.getValueAt(rowId, shared_blks_read_pos )).longValue();
			
			long   shared_blks_count = shared_blks_hit + shared_blks_read;

			// Calculate new values
			Double avg_time_per_call       = calls             <= 0 ? null : total_time      / calls;
			Long   avg_rows_per_call       = calls             <= 0 ? null : rows            / calls;
			Long   shared_blks_hit_per_row = rows              <= 0 ? null : shared_blks_hit / rows;
			Double cache_hit_pct           = shared_blks_count <= 0 ? null : 100.0 * shared_blks_hit / shared_blks_count;

			// 1 decimal for 'avg_time_per_call' and 'cache_hit_pct'
			avg_time_per_call = NumberUtils.round(avg_time_per_call, 1);
			cache_hit_pct     = NumberUtils.round(cache_hit_pct      , 1);
			
			// Set new DIFF values
			diffData.setValueAt(avg_time_per_call      , rowId, avg_time_per_call_pos);
			diffData.setValueAt(avg_rows_per_call      , rowId, avg_rows_per_call_pos);
			diffData.setValueAt(shared_blks_hit_per_row, rowId, shared_blks_hit_per_row_pos);
			diffData.setValueAt(cache_hit_pct          , rowId, cache_hit_pct_pos);
		}
		
		// Parse SQL Text and send of tables for DDL Storage
		if (Configuration.getCombinedConfiguration().getBooleanProperty(PROPKEY_view_parseAndSendTables, DEFAULT_view_parseAndSendTables))
		{
			parseSqlTextAndSendTablesForDdlStorage(newSample, diffData);
		}
	}

	
	/**
	 * Parse SQL Text for top "total_time" since "last sample" and send of tables for DDL Storage
	 * 
	 * @param newSample
	 * @param diffData
	 */
	private void parseSqlTextAndSendTablesForDdlStorage(CounterSample newSample, CounterSample diffData)
	{
		Configuration conf = Configuration.getCombinedConfiguration();
		PersistentCounterHandler pch = PersistentCounterHandler.getInstance();
		
		if (pch == null)
			return;

		// First we need to SORT the DIFF or RATE data on "total_time"
		final String sort_colName = "total_time";
		final int    sort_colPos  = diffData.findColumn(sort_colName);
		if (sort_colPos == -1)
		{
			_logger.error("parseSqlTextAndSendTablesForDdlStorage() cmName='" + diffData.getName() + "', Cant find column '" + sort_colName + "'.");
			return;
		}

		// Take a copy of the data, and sort it...
		ArrayList<List<Object>> sorted = new ArrayList<List<Object>>( diffData.getDataCollection() );
		Collections.sort(sorted,
			new Comparator<List<Object>>()
			{
				@Override
				public int compare(List<Object> o1, List<Object> o2)
				{
					Object objVal1 = o1.get(sort_colPos);
					Object objVal2 = o2.get(sort_colPos);
					
					if (objVal1 instanceof Number && objVal2 instanceof Number)
					{
						if ( ((Number)objVal1).doubleValue() < ((Number)objVal2).doubleValue() ) return 1;
						if ( ((Number)objVal1).doubleValue() > ((Number)objVal2).doubleValue() ) return -1;
						return 0;
					}
					_logger.warn("CM='" + getName() + "', NOT A NUMBER colName='" + sort_colName + "', colPos=" + sort_colPos + ": objVal1=" + objVal1.getClass().getName() + ", objVal2=" + objVal2.getClass().getName());
					return 0;
				}
			});
		
		
		int query_pos   = diffData.findColumn("query");
		int datname_pos = diffData.findColumn("datname");
		int queryid_pos = diffData.findColumn("queryid");

		if (query_pos == -1 || datname_pos == -1)
		{
			_logger.error("parseSqlTextAndSendTablesForDdlStorage() cmName='" + diffData.getName() + "', Cant find column 'query' or 'datname'.");
			return;
		}

		// How many rows should we DO
		int topRowcount   = conf.getIntProperty(PROPKEY_view_parseAndSendTables_topTotalTimeRowcount, DEFAULT_view_parseAndSendTables_topTotalTimeRowcount);
		int maxRowsToSend = Math.min(topRowcount, sorted.size());
		
		// Loop first ## rows on SORTED data, extract table names and send to DDL Storage
		for (int rowId = 0; rowId < maxRowsToSend; rowId++)
		{
			// Skipping 'total_time' that is ZERO
			Object obj_total_time = sorted.get(rowId).get(sort_colPos);
			if (obj_total_time instanceof Number && ((Number)obj_total_time).doubleValue() == 0d)
				continue;
			
			String sqlText = "" + sorted.get(rowId).get(query_pos);
			String dbname  = "" + sorted.get(rowId).get(datname_pos);
			String queryid = "" + sorted.get(rowId).get(queryid_pos);

			// If we already has parsed this QueryID, we don't need to do it again!
			if (_alreadyParsedQueryId_cache.contains(queryid))
				continue;

			// mark this QueryID as parsed
			_alreadyParsedQueryId_cache.add(queryid);

			// PARSE
			Set<String> tableList = SqlParserUtils.getTables(sqlText);
			
			// Foreign Data Wrapper (postgres_fdw) uses cursors to fetch remote data.
			// which typically looks like 'FETCH 100 FROM c22' where 'c22' will be parsed as the tableName
			//>>>>>>> NOTE: This does NOT seems to work (cursor info from FDW is not visible in pg_cursors <<<<<
			//>>>>>>>       also the cursor name seems to be "reused"
			//>>>>>>>       But lets keep it anyway (look for this text in 'ObjectLookupInspectorPostgres' and 'PostgresAbstract' (look for CURSOR) if you want to remove it totally)
			boolean isCursorFetch = sqlText.startsWith("FETCH ");

			// Post to DDL Storage, for lookup
			for (String tableName : tableList)
			{
				// All tables starting with "pg_" must be a Postgres System table... so do not bother to look this up.
				if (tableName.startsWith("pg_"))                    continue;
				if (tableName.startsWith("$"))                      continue;
				if (tableName.equalsIgnoreCase("CLOCK_TIMESTAMP"))  continue;

				if (isCursorFetch)
					tableName = "CURSOR: " + tableName;
				
				pch.addDdl(dbname, tableName, CM_NAME + ".ddlStore.parse.send.tables");
			}
		}
	}

	@Override
	public void prepareForPcsDatabaseRollover()
	{
		_logger.info("Clearing 'alreadyParsedQueryId_cache' due to 'PCS database rollover'.");

		// Clear any local caches
		_alreadyParsedQueryId_cache = new HashSet<>();
	}

	@Override
	public void reset()
	{
		// IMPORTANT: call super
		super.reset();

		// Clear any local caches
		_alreadyParsedQueryId_cache = new HashSet<>();
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


	/**
	 * Called before <code>refreshGetData(conn)</code> where we can make various checks
	 * <p>
	 * Note: this is a special case since SIX is recreating the Postgres Server 
	 *       (every now and then) during the day/night...
	 *       We need to check/create the extension before polling data from it!
	 */
	@Override
	public boolean beforeRefreshGetData(DbxConnection conn) throws Exception
	{
		return PostgresCmHelper.pgStatements_beforeRefreshGetData(this, conn);
	}

//	@Override
//	public boolean checkDependsOnOther(DbxConnection conn)
//	{
//		return CmPgStatementsUtils.checkDependsOnOther(this, conn);
//	}

}
