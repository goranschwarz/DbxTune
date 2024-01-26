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

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import javax.naming.NameNotFoundException;

import org.apache.commons.lang3.ArrayUtils;
import org.apache.log4j.Logger;

import com.asetune.ICounterController;
import com.asetune.IGuiController;
import com.asetune.Version;
import com.asetune.central.pcs.CentralPersistReader;
import com.asetune.cm.CounterSample;
import com.asetune.cm.CounterSetTemplates;
import com.asetune.cm.CounterSetTemplates.Type;
import com.asetune.cm.CountersModel;
import com.asetune.cm.postgres.gui.CmPgStatementsSumDbPanel;
import com.asetune.config.dict.MonTablesDictionary;
import com.asetune.config.dict.MonTablesDictionaryManager;
import com.asetune.graph.TrendGraphDataPoint;
import com.asetune.graph.TrendGraphDataPoint.LabelType;
import com.asetune.gui.MainFrame;
import com.asetune.gui.TabularCntrPanel;
import com.asetune.sql.conn.DbxConnection;
import com.asetune.sql.conn.info.DbmsVersionInfo;
import com.asetune.utils.Configuration;
import com.asetune.utils.NumberUtils;
import com.asetune.utils.Ver;

/**
 * @author Goran Schwarz (goran_schwarz@hotmail.com)
 */
public class CmPgStatementsSumDb
extends CountersModel
{
	private static Logger        _logger          = Logger.getLogger(CmPgStatementsSumDb.class);
	private static final long    serialVersionUID = 1L;

	public static final String   CM_NAME          = CmPgStatementsSumDb.class.getSimpleName();
	public static final String   SHORT_NAME       = "Statements by DB";
	public static final String   HTML_DESC        = 
		"<html>" +
		"The pg_stat_statements module provides a means for tracking execution statistics of all SQL statements executed by a server. <br>" +
		"In here the counters are grouped by database <br>" +
		"</html>";

	public static final String   GROUP_NAME       = MainFrame.TCP_GROUP_OBJECT_ACCESS;
	public static final String   GUI_ICON_FILE    = "images/"+CM_NAME+".png";

	public static final long     NEED_SRV_VERSION = 0;
	public static final long     NEED_CE_VERSION  = 0;

	public static final String[] MON_TABLES       = new String[] {"CmPgStatementsSumDb", "pg_stat_statements"};
	public static final String[] NEED_ROLES       = new String[] {};
	public static final String[] NEED_CONFIG      = new String[] {};

	public static final String[] PCT_COLUMNS      = new String[] { "cache_hit_pct" };
	public static final String[] DIFF_COLUMNS     = new String[] {
			"stmnt_count_diff",
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

		return new CmPgStatementsSumDb(counterController, guiController);
	}

	public CmPgStatementsSumDb(ICounterController counterController, IGuiController guiController)
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


	@Override
	protected TabularCntrPanel createGui()
	{
		return new CmPgStatementsSumDbPanel(this);
	}
	
	//------------------------------------------------------------
	// Implementation
	//------------------------------------------------------------
	private static final String  PROP_PREFIX                = CM_NAME;

	public static final String  PROPKEY_createExtension     = PROP_PREFIX + ".if.missing.pg_stat_statements.createExtension";
	public static final boolean DEFAULT_createExtension     = true; 

	public static final String  PROPKEY_excludeDbxTune      = PROP_PREFIX + ".exclude.dbxtune";
	public static final boolean DEFAULT_excludeDbxTune      = true; 

//	public static final String  PROPKEY_sample_total_time_gt = PROP_PREFIX + ".sample.total_time.gt";
//	public static final int     DEFAULT_sample_total_time_gt = 1000;
//	
//	public static final String  PROPKEY_view_parseAndSendTables = PROP_PREFIX + ".ddlStore.parse.send.tables";
//	public static final boolean DEFAULT_view_parseAndSendTables = true;
//
//	public static final String  PROPKEY_view_parseAndSendTables_topTotalTimeRowcount = PROP_PREFIX + ".ddlStore.parse.send.tables.top.total_time.rowcount";
//	public static final int     DEFAULT_view_parseAndSendTables_topTotalTimeRowcount = 10;
	
	// Structure to "cache" what QueryID's that already has been parsed
//	private Set<String> _alreadyParsedQueryId_cache = new HashSet<>();


	public static final String GRAPH_NAME_NEW_STATEMENTS   = "NewStatements";
	public static final String GRAPH_NAME_CALL_COUNT       = "CallCnt";
	public static final String GRAPH_NAME_TOTAL_TIME       = "TotalTime";
	public static final String GRAPH_NAME_ROWS             = "Rows";
	public static final String GRAPH_NAME_SHARED_BLKS_HIT  = "SharedBlksHit";
	public static final String GRAPH_NAME_SHARED_BLKS_READ = "SharedBlksRead";
	public static final String GRAPH_NAME_TEMP_BLK_READ    = "TmpBlkR";
	public static final String GRAPH_NAME_TEMP_BLK_WRITTEN = "TmpBlkW";
	public static final String GRAPH_NAME_WAL_RECORDS      = "WalRecords";
	public static final String GRAPH_NAME_WAL_BYTES        = "WalBytes";
	
	private void addTrendGraphs()
	{
		addTrendGraph(GRAPH_NAME_NEW_STATEMENTS,
				"SQL Statements [new_statements] per DB", 	                           // Menu CheckBox text
				"SQL Statements [new_statements] per DB per second ("+SHORT_NAME+")", // Graph Label 
				TrendGraphDataPoint.createGraphProps(TrendGraphDataPoint.Y_AXIS_SCALE_LABELS_PERSEC, CentralPersistReader.SampleType.MAX_OVER_SAMPLES),
				null, 
				LabelType.Dynamic, 
				TrendGraphDataPoint.Category.OPERATIONS,
				false, // is Percent Graph
				false, // visible at start
				0,     // graph is valid from Server Version. 0 = All Versions; >0 = Valid from this version and above 
				-1);   // minimum height

		addTrendGraph(GRAPH_NAME_CALL_COUNT,
				"SQL Statements [calls] per DB", 	                           // Menu CheckBox text
				"SQL Statements [calls] per DB per second ("+SHORT_NAME+")", // Graph Label 
				TrendGraphDataPoint.createGraphProps(TrendGraphDataPoint.Y_AXIS_SCALE_LABELS_PERSEC, CentralPersistReader.SampleType.MAX_OVER_SAMPLES),
				null, 
				LabelType.Dynamic, 
				TrendGraphDataPoint.Category.OPERATIONS,
				false, // is Percent Graph
				false, // visible at start
				0,     // graph is valid from Server Version. 0 = All Versions; >0 = Valid from this version and above 
				-1);   // minimum height

		addTrendGraph(GRAPH_NAME_TOTAL_TIME,
				"SQL Statements [total_time] per DB", 	                           // Menu CheckBox text
				"SQL Statements [total_time] per DB per second ("+SHORT_NAME+")", // Graph Label 
				TrendGraphDataPoint.createGraphProps(TrendGraphDataPoint.Y_AXIS_SCALE_LABELS_PERSEC, CentralPersistReader.SampleType.MAX_OVER_SAMPLES),
				null, 
				LabelType.Dynamic, 
				TrendGraphDataPoint.Category.OPERATIONS,
				false, // is Percent Graph
				false, // visible at start
				0,     // graph is valid from Server Version. 0 = All Versions; >0 = Valid from this version and above 
				-1);   // minimum height

		addTrendGraph(GRAPH_NAME_ROWS,
				"SQL Statements [rows] per DB", 	                           // Menu CheckBox text
				"SQL Statements [rows] per DB per second ("+SHORT_NAME+")", // Graph Label 
				TrendGraphDataPoint.createGraphProps(TrendGraphDataPoint.Y_AXIS_SCALE_LABELS_PERSEC, CentralPersistReader.SampleType.MAX_OVER_SAMPLES),
				null, 
				LabelType.Dynamic, 
				TrendGraphDataPoint.Category.OPERATIONS,
				false, // is Percent Graph
				false, // visible at start
				0,     // graph is valid from Server Version. 0 = All Versions; >0 = Valid from this version and above 
				-1);   // minimum height

		addTrendGraph(GRAPH_NAME_SHARED_BLKS_HIT,
				"SQL Statements [shared_blks_hit] per DB", 	                           // Menu CheckBox text
				"SQL Statements [shared_blks_hit] per DB per second ("+SHORT_NAME+")", // Graph Label 
				TrendGraphDataPoint.createGraphProps(TrendGraphDataPoint.Y_AXIS_SCALE_LABELS_PERSEC, CentralPersistReader.SampleType.MAX_OVER_SAMPLES),
				null, 
				LabelType.Dynamic, 
				TrendGraphDataPoint.Category.OPERATIONS,
				false, // is Percent Graph
				false, // visible at start
				0,     // graph is valid from Server Version. 0 = All Versions; >0 = Valid from this version and above 
				-1);   // minimum height

		addTrendGraph(GRAPH_NAME_SHARED_BLKS_READ,
				"SQL Statements [shared_blks_read] per DB", 	                           // Menu CheckBox text
				"SQL Statements [shared_blks_read] per DB per second ("+SHORT_NAME+")", // Graph Label 
				TrendGraphDataPoint.createGraphProps(TrendGraphDataPoint.Y_AXIS_SCALE_LABELS_PERSEC, CentralPersistReader.SampleType.MAX_OVER_SAMPLES),
				null, 
				LabelType.Dynamic, 
				TrendGraphDataPoint.Category.OPERATIONS,
				false, // is Percent Graph
				false, // visible at start
				0,     // graph is valid from Server Version. 0 = All Versions; >0 = Valid from this version and above 
				-1);   // minimum height

		addTrendGraph(GRAPH_NAME_TEMP_BLK_READ,
				"SQL Statements [temp_blks_read] per DB", 	                           // Menu CheckBox text
				"SQL Statements [temp_blks_read] per DB per second ("+SHORT_NAME+")", // Graph Label 
				TrendGraphDataPoint.createGraphProps(TrendGraphDataPoint.Y_AXIS_SCALE_LABELS_PERSEC, CentralPersistReader.SampleType.MAX_OVER_SAMPLES),
				null, 
				LabelType.Dynamic, 
				TrendGraphDataPoint.Category.OPERATIONS,
				false, // is Percent Graph
				false, // visible at start
				0,     // graph is valid from Server Version. 0 = All Versions; >0 = Valid from this version and above 
				-1);   // minimum height

		addTrendGraph(GRAPH_NAME_TEMP_BLK_WRITTEN,
				"SQL Statements [temp_blks_written] per DB", 	                           // Menu CheckBox text
				"SQL Statements [temp_blks_written] per DB per second ("+SHORT_NAME+")", // Graph Label 
				TrendGraphDataPoint.createGraphProps(TrendGraphDataPoint.Y_AXIS_SCALE_LABELS_PERSEC, CentralPersistReader.SampleType.MAX_OVER_SAMPLES),
				null, 
				LabelType.Dynamic, 
				TrendGraphDataPoint.Category.OPERATIONS,
				false, // is Percent Graph
				false, // visible at start
				0,     // graph is valid from Server Version. 0 = All Versions; >0 = Valid from this version and above 
				-1);   // minimum height

		addTrendGraph(GRAPH_NAME_WAL_RECORDS,
				"SQL Statements [wal_records] per DB", 	                           // Menu CheckBox text
				"SQL Statements [wal_records] per DB per second ("+SHORT_NAME+")", // Graph Label 
				TrendGraphDataPoint.createGraphProps(TrendGraphDataPoint.Y_AXIS_SCALE_LABELS_PERSEC, CentralPersistReader.SampleType.MAX_OVER_SAMPLES),
				null, 
				LabelType.Dynamic, 
				TrendGraphDataPoint.Category.OPERATIONS,
				false, // is Percent Graph
				false, // visible at start
				Ver.ver(13),     // graph is valid from Server Version. 0 = All Versions; >0 = Valid from this version and above 
				-1);   // minimum height

		addTrendGraph(GRAPH_NAME_WAL_BYTES,
				"SQL Statements [wal_bytes] per DB", 	                           // Menu CheckBox text
				"SQL Statements [wal_bytes] per DB per second ("+SHORT_NAME+")", // Graph Label 
				TrendGraphDataPoint.createGraphProps(TrendGraphDataPoint.Y_AXIS_SCALE_LABELS_PERSEC, CentralPersistReader.SampleType.MAX_OVER_SAMPLES),
				null, 
				LabelType.Dynamic, 
				TrendGraphDataPoint.Category.OPERATIONS,
				false, // is Percent Graph
				false, // visible at start
				Ver.ver(13),     // graph is valid from Server Version. 0 = All Versions; >0 = Valid from this version and above 
				-1);   // minimum height
	}

	private void localUpdateGraphData(TrendGraphDataPoint tgdp, int dataType, String colname)
	{
		// Get database count (do dot include template databases)
		int size = 0;
		for (int i = 0; i < this.size(); i++)
		{
			String dbname = this.getAbsString(i, "datname");
			if (dbname != null && !dbname.startsWith("template"))
				size++;
		}

		// Write 1 "line" for every db (except for 'template*' databases)
		Double[] dArray = new Double[size];
		String[] lArray = new String[size];

		int ap = 0;
		int rc = this.size();
		for (int r = 0; r < rc; r++) // we still need to loop all rows...
		{
			String dbname = this.getAbsString(r, "dbname");
			if (dbname != null && !dbname.startsWith("template"))
			{
				Double data;
				if      (dataType == CountersModel.DATA_ABS)  data = this.getAbsValueAsDouble (r, colname);
				else if (dataType == CountersModel.DATA_DIFF) data = this.getDiffValueAsDouble(r, colname);
				else if (dataType == CountersModel.DATA_RATE) data = this.getRateValueAsDouble(r, colname);
				else throw new RuntimeException("dataType(tgName=" + tgdp.getName() + "): Unsupported dataType=" + dataType);
				
//System.out.println("     xxx: r=" + r + ", rc=" + rc + ", ap=" + ap + ", size()=" + size() + ", lArray.length=" + lArray.length + ", dArray.length=" + dArray.length + ", dbname='" + dbname + "'.");
				lArray[ap] = dbname;
				dArray[ap] = data;
				ap++;
			}
		}

		// If '_Total' is the LAST entry (which is the normal) -->> Move it to First Entry
		if (lArray[lArray.length-1].equals("_Total"))
		{
			ArrayUtils.shift(lArray, 1);
			ArrayUtils.shift(dArray, 1);
		}

		// Set the values
		tgdp.setDataPoint(this.getTimestamp(), lArray, dArray);
	}
	
	@Override
	public void updateGraphData(TrendGraphDataPoint tgdp)
	{
//		"stmnt_count_diff",
//		"calls",
//		"total_time",
//		"total_plan_time", // from: 13.0
//		"total_exec_time", // from: 13.0 and replaces "total_time"
//		"rows",
//		"shared_blks_hit",
//		"shared_blks_read",
//		"shared_blks_dirtied",
//		"shared_blks_written",
//		"local_blks_hit",
//		"local_blks_read",
//		"local_blks_dirtied",
//		"local_blks_written",
//		"temp_blks_read",
//		"temp_blks_written",
//		"blk_read_time",
//		"blk_write_time",
//		"wal_records",      // from: 13.0
//		"wal_fpi",          // from: 13.0   FPI = Full Page Image
//		"wal_bytes"         // from: 13.0

		long srvVersion = getServerVersion();
		
		if (GRAPH_NAME_NEW_STATEMENTS   .equals(tgdp.getName())) localUpdateGraphData(tgdp, CountersModel.DATA_RATE, "stmnt_count_diff");
		if (GRAPH_NAME_CALL_COUNT       .equals(tgdp.getName())) localUpdateGraphData(tgdp, CountersModel.DATA_RATE, "calls");
		if (GRAPH_NAME_TOTAL_TIME       .equals(tgdp.getName())) localUpdateGraphData(tgdp, CountersModel.DATA_RATE, "total_time");
		if (GRAPH_NAME_ROWS             .equals(tgdp.getName())) localUpdateGraphData(tgdp, CountersModel.DATA_RATE, "rows");
		if (GRAPH_NAME_SHARED_BLKS_HIT  .equals(tgdp.getName())) localUpdateGraphData(tgdp, CountersModel.DATA_RATE, "shared_blks_hit");
		if (GRAPH_NAME_SHARED_BLKS_READ .equals(tgdp.getName())) localUpdateGraphData(tgdp, CountersModel.DATA_RATE, "shared_blks_read");
		if (GRAPH_NAME_TEMP_BLK_READ    .equals(tgdp.getName())) localUpdateGraphData(tgdp, CountersModel.DATA_RATE, "temp_blks_read");
		if (GRAPH_NAME_TEMP_BLK_WRITTEN .equals(tgdp.getName())) localUpdateGraphData(tgdp, CountersModel.DATA_RATE, "temp_blks_written");
		
		if (srvVersion >= Ver.ver(13))
		{
			if (GRAPH_NAME_WAL_RECORDS  .equals(tgdp.getName())) localUpdateGraphData(tgdp, CountersModel.DATA_RATE, "wal_records");
			if (GRAPH_NAME_WAL_BYTES    .equals(tgdp.getName())) localUpdateGraphData(tgdp, CountersModel.DATA_RATE, "wal_bytes");
		}
	}

//	/** Used by the: Create 'Offline Session' Wizard */
//	@Override
//	public List<CmSettingsHelper> getLocalSettings()
//	{
//		Configuration conf = Configuration.getCombinedConfiguration();
//		List<CmSettingsHelper> list = new ArrayList<>();
//		
//		list.add(new CmSettingsHelper("Sample 'total_time' above", PROPKEY_sample_total_time_gt  , Integer.class, conf.getIntProperty(PROPKEY_sample_total_time_gt  , DEFAULT_sample_total_time_gt  ), DEFAULT_sample_total_time_gt  , "Sample 'total_time' above" ));
//
//		return list;
//	}

//	@Override
//	public Map<String, PcsColumnOptions> getPcsColumnOptions()
//	{
//		Map<String, PcsColumnOptions> map = super.getPcsColumnOptions();
//
//		// No settings in the super, create one, and set it at super
//		if (map == null)
//		{
//			map = new HashMap<>();
//			map.put("query", new PcsColumnOptions(ColumnType.DICTIONARY_COMPRESSION));
//
//			// Set the map in the super
//			setPcsColumnOptions(map);
//		}
//
//		return map;
//	}


	
	@Override
	public void addMonTableDictForVersion(DbxConnection conn, DbmsVersionInfo versionInfo)
	{
		try 
		{
			MonTablesDictionary mtd = MonTablesDictionaryManager.getInstance();

			String cmName = this.getName();
			mtd.addTable(cmName, HTML_DESC);

			mtd.addColumn(cmName, "dbname"                     ,"<html>Name of the database. (_Total, is a <i>Aggregated</i> row).</html>");
			mtd.addColumn(cmName, "stmnt_count"                ,"<html>How many Statements was found in 'pg_stat_statements' for this database.</html>");

			mtd.addColumn(cmName, "avg_time_per_call"           ,"<html>How many milliseconds does a call take in average.                <br><br><b>Algorithm:</b> total_time / calls </html>");
			mtd.addColumn(cmName, "avg_rows_per_call"           ,"<html>How many rows is affected by a call in average.                   <br><br><b>Algorithm:</b> rows / calls </html>");
			mtd.addColumn(cmName, "shared_blks_hit_per_row"     ,"<html>How many cached pages is read/used per affected row in average.   <br><br><b>Algorithm:</b> shared_blks_hit / rows </html>");
			mtd.addColumn(cmName, "cache_hit_pct"               ,"<html>How many pages was found in memory (cached by the buffer pool).   <br><br><b>Algorithm:</b> shared_blks_hit / (shared_blks_hit + shared_blks_read) </html>");

		}
		catch (NameNotFoundException e) 
		{
			_logger.warn("Problems in cm='" + CM_NAME + "', adding addMonTableDictForVersion. Caught: " + e); 
		//	System.out.println("Problems in cm='" + CM_NAME + "', adding addMonTableDictForVersion. Caught: " + e); 
		}
	}

	
	
	@Override
	public List<String> getPkForVersion(DbxConnection conn, DbmsVersionInfo versionInfo)
	{
		List <String> pkCols = new LinkedList<String>();

//		pkCols.add("dbid");
		pkCols.add("dbname");

		return pkCols;
	}

//	@Override
//	public String getSqlForVersion(DbxConnection conn, DbmsVersionInfo versionInfo)
//	{
//		long srvVersion = versionInfo.getLongVersion();
//
//		// 'queryid' was introduced in 9.4 so lets simulate a query id in earlier versions
//		String queryid = "";
//		if (srvVersion < Ver.ver(9,4))
//		{
////			queryid = "    ,md5(query) as queryid \n";
//			queryid = "    ,('x'||substr(md5(query),1,16))::bit(64)::bigint as queryid \n";
//		}
//
//		String total_time1 = "s.total_time";
//		String total_time2 = "";
//		if (srvVersion >= Ver.ver(13))
//		{
//			total_time1 = "(s.total_plan_time + s.total_exec_time)";
//			total_time2 = "    ,(s.total_plan_time + s.total_exec_time) AS total_time n";
//		}
//
//		int total_time_gt = Configuration.getCombinedConfiguration().getIntProperty(PROPKEY_sample_total_time_gt, DEFAULT_sample_total_time_gt);
//
//		//TODO: look at 'pg_show_plans' at https://github.com/cybertec-postgresql/pg_show_plans
//		//      to get "runtime" plans (just like sp_showplan)
//		//      Possibly we can incorporate the in the below SQL statement (or get it in a separate collection tab)
//
//		return  "select \n" +
//				"     CASE WHEN s.calls > 0 THEN " + total_time1 + "      / s.calls ELSE 0 END as avg_time_per_call \n" +
//				"    ,CASE WHEN s.calls > 0 THEN s.rows            / s.calls ELSE 0 END as avg_rows_per_call \n" +
//				"    ,CASE WHEN s.rows  > 0 THEN s.shared_blks_hit / s.rows  ELSE 0 END as shared_blks_hit_per_row \n" +
//				"    ,CAST(100.0 * s.shared_blks_hit / nullif(s.shared_blks_hit + s.shared_blks_read, 0) as numeric(5,1)) AS cache_hit_pct \n" +
//				"    ,d.datname \n" +
//				"    ,u.usename \n" +
//				queryid +
//				total_time2 +
//				"    ,s.* \n" +
//				"    ,cast(0 as integer) AS \"dupMergeCount\" \n" +
//				"from pg_stat_statements s \n" +
//				"left outer join pg_catalog.pg_database d ON s.dbid   = d.oid      \n" +
//				"left outer join pg_catalog.pg_user     u ON s.userid = u.usesysid \n" +
////				"where s.calls > 1 \n" +
//				"where " + total_time1 + " > " + total_time_gt + " \n" +
//				"";
//	}

	
	
	@Override
	public String getSqlForVersion(DbxConnection conn, DbmsVersionInfo versionInfo)
	{
		long srvVersion = versionInfo.getLongVersion();

		int pgUserCount = -1;
		if (conn != null)
		{
			String sql = "select count(*) from pg_user";
			try (Statement stmnt = conn.createStatement(); ResultSet rs = stmnt.executeQuery(sql))
			{
				while(rs.next())
					pgUserCount = rs.getInt(1);
			}
			catch(SQLException ex)
			{
				_logger.warn("Problem getting number of Postgres users. SQL='" + sql + "'. Caught: " + ex, ex);
			}
		}
		
		// The function pg_stat_statements() intruduced a flag to NOT include column 'query' in version 9.4
		String pg_stat_statements = "pg_stat_statements";
		if (srvVersion >= Ver.ver(9,4))
		{
			pg_stat_statements = "pg_stat_statements(false)";
		}
		
//		String total_time1 = "s.total_time";
//		String total_time2 = "";
//		if (srvVersion >= Ver.ver(13))
//		{
//			total_time1 = "(s.total_plan_time + s.total_exec_time)";
//			total_time2 = "    ,(s.total_plan_time + s.total_exec_time) AS total_time n";
//		}
		String total_time1 = "        ,cast(sum(s.total_time)          as bigint) as total_time \n";
		String total_time2 = "sum(s.total_time)";
		if (srvVersion >= Ver.ver(13))
		{
			total_time1 = "        ,cast(sum(s.total_plan_time) + sum(s.total_exec_time) as bigint) as total_time \n";
			total_time2 = "(sum(s.total_plan_time) + sum(s.total_exec_time))";
		}

		String wal_records1 = "";
		String wal_records2 = "";
		String wal_bytes1   = "";
		String wal_bytes2   = "";
		if (srvVersion >= Ver.ver(13))
		{
			wal_records1 = "        ,cast(sum(s.wal_records)         as bigint) as wal_records \n";
			wal_bytes1   = "        ,cast(sum(s.wal_bytes)           as bigint) as wal_bytes \n";

			wal_records2 = "    ,stmnt.wal_records \n";
			wal_bytes2   = "    ,stmnt.wal_bytes \n";
		}


		// EXCLUDE DbxTune logins (or exclude all users, which 'PostgresTune' happened to connect as)
		//                         but if 'PostgresTune' is connected as 'postgres' then we DO NOT exclude
		String where_excludeDbxTune = "";
		if (Configuration.getCombinedConfiguration().getBooleanProperty(PROPKEY_excludeDbxTune, DEFAULT_excludeDbxTune))
		{
			if (pgUserCount <= 1)
			{
				_logger.warn("Only " + pgUserCount + " was found in DBMS. So if we exclude users, we wont see anythying in here. Property: '" + PROPKEY_excludeDbxTune + "=true|false'");
				where_excludeDbxTune = "";
			}
			else
			{
				where_excludeDbxTune = "      and s.userid not in (select a.usesysid from pg_stat_activity a where a.application_name = '" + Version.getAppName() + "' and usename != 'postgres') \n";
			}
		}
//		TODO; Can we think of a better alternative here?
//				* id there is only 1 user (everyone is for example using "postgres"), then the above filter will be *bad*
//				* so if we can do select usesysid, count(1) from pg_stat_activity group by usesysid -- or similar to figgure out if we only have 1 user...
//              * and also do: if ("only-1-user") then: -NOT-use-filter- or similar

		
//		String sql = ""
//			    + "select \n"
//			    + "     max(d.datname)             as dbname \n"
//				+ "    ,count(*)                   as stmnt_count \n"
//			    + "    ,sum(s.calls)               as calls \n"
//			    + "    ,sum(s.total_time)          as total_time \n"   // NOTE: This needs to be version sensitive
//			    + "    ,sum(s.rows)                as rows \n"
//			    + "    ,sum(s.shared_blks_hit)     as shared_blks_hit \n"
//			    + "    ,sum(s.shared_blks_read)    as shared_blks_read \n"
//			    + "    ,sum(s.shared_blks_dirtied) as shared_blks_dirtied \n"
//			    + "    ,sum(s.shared_blks_written) as shared_blks_written \n"
//			    + "    ,sum(s.local_blks_hit)      as local_blks_hit \n"
//			    + "    ,sum(s.local_blks_read)     as local_blks_read \n"
//			    + "    ,sum(s.local_blks_dirtied)  as local_blks_dirtied \n"
//			    + "    ,sum(s.local_blks_written)  as local_blks_written \n"
//			    + "    ,sum(s.temp_blks_read)      as temp_blks_read \n"
//			    + "    ,sum(s.temp_blks_written)   as temp_blks_written \n"
//			    + "    ,sum(s.blk_read_time)       as blk_read_time \n"
//			    + "    ,sum(s.blk_write_time)      as blk_write_time \n"
//			    + "    ,s.dbid \n"
//			    + "from " + pg_stat_statements + " s \n"
//			    + "join pg_database d ON s.dbid = d.oid \n"
//			    + where_excludeDbxTune
//			    + "group by s.dbid \n"
//			    + "";

		String sql = ""
			    + "with db as ( \n"
			    + "    select \n"
			    + "           cast(datname as varchar(255)) as dbname \n"
			    + "          ,oid                           as dbid \n"
			    + "    from pg_database \n"
			    + "    where datistemplate = false \n"
			    + "), stmnt as ( \n"
			    + "    select \n"
			    + "         s.dbid \n"
			    + "        ,cast(count(*)                   as bigint) as stmnt_count \n"
			    + "        ,cast(sum(s.calls)               as bigint) as calls \n"
			    + total_time1
			    + "        ,cast(sum(s.rows)                as bigint) as rows \n"
			    + "        ,cast(sum(s.shared_blks_hit)     as bigint) as shared_blks_hit \n"
			    + "        ,cast(sum(s.shared_blks_read)    as bigint) as shared_blks_read \n"
			    + "        ,cast(sum(s.shared_blks_dirtied) as bigint) as shared_blks_dirtied \n"
			    + "        ,cast(sum(s.shared_blks_written) as bigint) as shared_blks_written \n"
			    + "        ,cast(sum(s.local_blks_hit)      as bigint) as local_blks_hit \n"
			    + "        ,cast(sum(s.local_blks_read)     as bigint) as local_blks_read \n"
			    + "        ,cast(sum(s.local_blks_dirtied)  as bigint) as local_blks_dirtied \n"
			    + "        ,cast(sum(s.local_blks_written)  as bigint) as local_blks_written \n"
			    + "        ,cast(sum(s.temp_blks_read)      as bigint) as temp_blks_read \n"
			    + "        ,cast(sum(s.temp_blks_written)   as bigint) as temp_blks_written \n"
			    + "        ,cast(sum(s.blk_read_time)       as bigint) as blk_read_time \n"
			    + "        ,cast(sum(s.blk_write_time)      as bigint) as blk_write_time \n"
			    + wal_records1
			    + wal_bytes1
			    + "        ,cast(100.0 * sum(s.shared_blks_hit) / nullif(sum(s.shared_blks_hit) + sum(s.shared_blks_read), 0) as numeric(5,1)) AS cache_hit_pct \n"
			    + "        ,cast(CASE WHEN sum(s.calls) > 0 THEN " + total_time2 + "    / sum(s.calls) ELSE 0 END as numeric(18,1)) as avg_time_per_call \n"
			    + "        ,cast(CASE WHEN sum(s.calls) > 0 THEN sum(s.rows)            / sum(s.calls) ELSE 0 END as numeric(18,1)) as avg_rows_per_call \n"
			    + "        ,cast(CASE WHEN sum(s.rows)  > 0 THEN sum(s.shared_blks_hit) / sum(s.rows)  ELSE 0 END as numeric(18,1)) as shared_blks_hit_per_row \n"
			    + "    from " + pg_stat_statements + " s \n"
			    + "    where 1 = 1 \n"
			    +      where_excludeDbxTune
			    + "    group by s.dbid \n"
			    + ") \n"
			    + "select \n"
			    + "     db.dbname \n"
			    + "    ,stmnt.stmnt_count \n"
			    + "    ,stmnt.stmnt_count as stmnt_count_diff \n"
			    + "    ,stmnt.calls \n"
			    + "    ,stmnt.total_time \n"
			    + "        ,stmnt.avg_time_per_call \n"
			    + "    ,stmnt.rows \n"
			    + "        ,stmnt.avg_rows_per_call \n"
			    + "    ,stmnt.shared_blks_hit \n"
			    + "        ,stmnt.shared_blks_hit_per_row \n"
			    + "    ,stmnt.shared_blks_read \n"
			    + "    ,stmnt.shared_blks_dirtied \n"
			    + "    ,stmnt.shared_blks_written \n"
			    + "        ,stmnt.cache_hit_pct \n"
			    + "    ,stmnt.local_blks_hit \n"
			    + "    ,stmnt.local_blks_read \n"
			    + "    ,stmnt.local_blks_dirtied \n"
			    + "    ,stmnt.local_blks_written \n"
			    + "    ,stmnt.temp_blks_read \n"
			    + "    ,stmnt.temp_blks_written \n"
			    + "    ,stmnt.blk_read_time \n"
			    + "    ,stmnt.blk_write_time \n"
			    + wal_records2
			    + wal_bytes2
			    + "    ,db.dbid \n"
			    + "from db \n"
			    + "left outer join stmnt ON db.dbid = stmnt.dbid \n"
			    + "";
		
		return sql;
	}

//	@Override
//	public void localCalculation(CounterSample newSample)
//	{
//		try
//		{
//			calculateAggregateRow(newSample);
//		}
//		catch (RuntimeException ex)
//		{
//			_logger.error(getName() + ": Problems when creating a Summary row for CM '" + getName() + "'. Skipping the Sum row, but continuing.", ex);
//		}
//	}

	@Override
	public Map<String, AggregationType> createAggregateColumns()
	{
//		DbmsVersionInfo versionInfo = getDbmsVersionInfo();
		
		HashMap<String, AggregationType> aggColumns = new HashMap<>(getColumnCount());

		AggregationType tmp;
		
		// Create the columns :::::::::::::::::::::::::::::::::::::::::::::::::::::: And ADD it to the return Map 
		tmp = new AggregationType("stmnt_count"        , AggregationType.Agg.SUM);   aggColumns.put(tmp.getColumnName(), tmp);
		tmp = new AggregationType("stmnt_count_diff"   , AggregationType.Agg.SUM);   aggColumns.put(tmp.getColumnName(), tmp);
		tmp = new AggregationType("calls"              , AggregationType.Agg.SUM);   aggColumns.put(tmp.getColumnName(), tmp);
		tmp = new AggregationType("total_time"         , AggregationType.Agg.SUM);   aggColumns.put(tmp.getColumnName(), tmp);
		tmp = new AggregationType("rows"               , AggregationType.Agg.SUM);   aggColumns.put(tmp.getColumnName(), tmp);
		tmp = new AggregationType("shared_blks_hit"    , AggregationType.Agg.SUM);   aggColumns.put(tmp.getColumnName(), tmp);
		tmp = new AggregationType("shared_blks_read"   , AggregationType.Agg.SUM);   aggColumns.put(tmp.getColumnName(), tmp);
		tmp = new AggregationType("shared_blks_dirtied", AggregationType.Agg.SUM);   aggColumns.put(tmp.getColumnName(), tmp);
		tmp = new AggregationType("shared_blks_written", AggregationType.Agg.SUM);   aggColumns.put(tmp.getColumnName(), tmp);
		tmp = new AggregationType("local_blks_hit"     , AggregationType.Agg.SUM);   aggColumns.put(tmp.getColumnName(), tmp);
		tmp = new AggregationType("local_blks_read"    , AggregationType.Agg.SUM);   aggColumns.put(tmp.getColumnName(), tmp);
		tmp = new AggregationType("local_blks_dirtied" , AggregationType.Agg.SUM);   aggColumns.put(tmp.getColumnName(), tmp);
		tmp = new AggregationType("local_blks_written" , AggregationType.Agg.SUM);   aggColumns.put(tmp.getColumnName(), tmp);
		tmp = new AggregationType("temp_blks_read"     , AggregationType.Agg.SUM);   aggColumns.put(tmp.getColumnName(), tmp);
		tmp = new AggregationType("temp_blks_written"  , AggregationType.Agg.SUM);   aggColumns.put(tmp.getColumnName(), tmp);
		tmp = new AggregationType("blk_read_time"      , AggregationType.Agg.SUM);   aggColumns.put(tmp.getColumnName(), tmp);
		tmp = new AggregationType("blk_write_time"     , AggregationType.Agg.SUM);   aggColumns.put(tmp.getColumnName(), tmp);

		tmp = new AggregationType("avg_time_per_call"      , AggregationType.Agg.AVG, false, true);   aggColumns.put(tmp.getColumnName(), tmp);
		tmp = new AggregationType("avg_rows_per_call"      , AggregationType.Agg.AVG, false, true);   aggColumns.put(tmp.getColumnName(), tmp);
		tmp = new AggregationType("shared_blks_hit_per_row", AggregationType.Agg.AVG, false, true);   aggColumns.put(tmp.getColumnName(), tmp);
		tmp = new AggregationType("cache_hit_pct"          , AggregationType.Agg.AVG, false, true);   aggColumns.put(tmp.getColumnName(), tmp);

		return aggColumns;
	}

	@Override
	public Object calculateAggregateRow_nonAggregatedColumnDataProvider(CounterSample newSample, String colName, int c, int jdbcType, Object addValue)
	{
		if ("dbid".equalsIgnoreCase(colName))
			return new Long(-1);
		
		return null;
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

		// Since the AVG in the "TOTAL" ABS Aggregate is a "algorithm" -- we need to do this as well 
		// NOTE: This should be the same logic as for the DIFF-DATA
		boolean doAbsAggregateManualCalc = true;
		if (doAbsAggregateManualCalc && getAggregatedRowId() >= 0)
		{
			int rowId = getAggregatedRowId();
			
			long   rows             = ((Number) newSample.getValueAt(rowId, rows_pos             )).longValue();
			long   calls            = ((Number) newSample.getValueAt(rowId, calls_pos            )).longValue();
			double total_time       = ((Number) newSample.getValueAt(rowId, total_time_pos       )).doubleValue();
			long   shared_blks_hit  = ((Number) newSample.getValueAt(rowId, shared_blks_hit_pos  )).longValue();
			long   shared_blks_read = ((Number) newSample.getValueAt(rowId, shared_blks_read_pos )).longValue();
			
			long   shared_blks_count = shared_blks_hit + shared_blks_read;

			// Calculate new values
			Double avg_time_per_call       = calls             <= 0 ? null : total_time              / calls;
			Double avg_rows_per_call       = calls             <= 0 ? null : 1.0   * rows            / calls;
			Double shared_blks_hit_per_row = rows              <= 0 ? null : 1.0   * shared_blks_hit / rows;
			Double cache_hit_pct           = shared_blks_count <= 0 ? null : 100.0 * shared_blks_hit / shared_blks_count;

			// 1 decimal for 'avg_time_per_call' and 'cache_hit_pct'
			avg_time_per_call       = NumberUtils.round(avg_time_per_call      , 1);
			avg_rows_per_call       = NumberUtils.round(avg_rows_per_call      , 1);
			shared_blks_hit_per_row = NumberUtils.round(shared_blks_hit_per_row, 1);
			cache_hit_pct           = NumberUtils.round(cache_hit_pct          , 1);
			
			// Set new DIFF values
			newSample.setValueAt(avg_time_per_call      , rowId, avg_time_per_call_pos);
			newSample.setValueAt(avg_rows_per_call      , rowId, avg_rows_per_call_pos);
			newSample.setValueAt(shared_blks_hit_per_row, rowId, shared_blks_hit_per_row_pos);
			newSample.setValueAt(cache_hit_pct          , rowId, cache_hit_pct_pos);
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
			Double avg_time_per_call       = calls             <= 0 ? null : total_time              / calls;
			Double avg_rows_per_call       = calls             <= 0 ? null : 1.0   * rows            / calls;
			Double shared_blks_hit_per_row = rows              <= 0 ? null : 1.0   * shared_blks_hit / rows;
			Double cache_hit_pct           = shared_blks_count <= 0 ? null : 100.0 * shared_blks_hit / shared_blks_count;

			// 1 decimal for 'avg_time_per_call' and 'cache_hit_pct'
			avg_time_per_call       = NumberUtils.round(avg_time_per_call      , 1);
			avg_rows_per_call       = NumberUtils.round(avg_rows_per_call      , 1);
			shared_blks_hit_per_row = NumberUtils.round(shared_blks_hit_per_row, 1);
			cache_hit_pct           = NumberUtils.round(cache_hit_pct          , 1);
			
			// Set new DIFF values
			diffData.setValueAt(avg_time_per_call      , rowId, avg_time_per_call_pos);
			diffData.setValueAt(avg_rows_per_call      , rowId, avg_rows_per_call_pos);
			diffData.setValueAt(shared_blks_hit_per_row, rowId, shared_blks_hit_per_row_pos);
			diffData.setValueAt(cache_hit_pct          , rowId, cache_hit_pct_pos);
		}
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

	@Override
	public boolean checkDependsOnOther(DbxConnection conn)
	{
		return PostgresCmHelper.pgStatements_checkDependsOnOther(this, conn);
	}
}
