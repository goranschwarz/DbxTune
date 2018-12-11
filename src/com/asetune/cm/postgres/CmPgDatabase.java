package com.asetune.cm.postgres;

import java.math.BigDecimal;
import java.sql.Connection;
import java.util.LinkedList;
import java.util.List;

import com.asetune.ICounterController;
import com.asetune.IGuiController;
import com.asetune.cm.CounterSample;
import com.asetune.cm.CounterSetTemplates;
import com.asetune.cm.CounterSetTemplates.Type;
import com.asetune.cm.CountersModel;
import com.asetune.graph.TrendGraphDataPoint;
import com.asetune.graph.TrendGraphDataPoint.LabelType;
import com.asetune.gui.MainFrame;

/**
 * @author Goran Schwarz (goran_schwarz@hotmail.com)
 */
// this can be of help: 
//    https://www.datadoghq.com/blog/postgresql-monitoring/
//    https://github.com/DataDog/the-monitor/blob/master/postgresql/postgresql-monitoring.md

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

	public static final long     NEED_SRV_VERSION = 0;
	public static final long     NEED_CE_VERSION  = 0;

	public static final String[] MON_TABLES       = new String[] {"pg_stat_database"};
	public static final String[] NEED_ROLES       = new String[] {};
	public static final String[] NEED_CONFIG      = new String[] {};

	public static final String[] PCT_COLUMNS      = new String[] { "fetch_efficency_pct" };
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
	public static final String GRAPH_NAME_FETCH_EFFECIENT_PCT     = "FetchEfficientPct";
	public static final String GRAPH_NAME_FETCH_EFFECIENT_ABS_PCT = "FetchEfficientAbsPct";
	public static final String GRAPH_NAME_FETCH_EFFECIENT_SUM     = "FetchEfficientSum";
	public static final String GRAPH_NAME_CONNECTIONS             = "Connections";
	public static final String GRAPH_NAME_CONNECTIONS_SUM         = "ConnectionsSum";
	public static final String GRAPH_NAME_COMMITS                 = "Commits";
	public static final String GRAPH_NAME_ROLLBACKS               = "Rollbacks";
	public static final String GRAPH_NAME_READS                   = "Reads";
	public static final String GRAPH_NAME_CACHE_HITS              = "CacheHits";
	public static final String GRAPH_NAME_ROWS_RETURNED           = "RowsReturned";
	public static final String GRAPH_NAME_ROWS_FETCHED            = "RowsFetched";
	public static final String GRAPH_NAME_ROWS_INSERTS            = "Inserts";
	public static final String GRAPH_NAME_ROWS_UPDATED            = "Updated";
	public static final String GRAPH_NAME_ROWS_DELETED            = "Deleted";
	public static final String GRAPH_NAME_ROWS_CONFLICTS          = "Conflicts";
	public static final String GRAPH_NAME_TEMP_FILES              = "TempFiles";
	public static final String GRAPH_NAME_TEMP_BYTES              = "TempBytes";
	public static final String GRAPH_NAME_DEADLOCKS               = "Deadlocks";
	public static final String GRAPH_NAME_READ_TIME               = "ReadTime";
	public static final String GRAPH_NAME_WRITE_TIME              = "WriteTime";
	public static final String GRAPH_NAME_DBSIZE_MB               = "DbSizeMb";
	
//	@Override
//	protected TabularCntrPanel createGui()
//	{
//		return new CmActiveStatementsPanel(this);
//	}
	
	@Override
	public List<String> getPkForVersion(Connection conn, long srvVersion, boolean isClusterEnabled)
	{
		List <String> pkCols = new LinkedList<String>();

		pkCols.add("datid");

		return pkCols;
	}

	@Override
	public String getSqlForVersion(Connection conn, long srvVersion, boolean isClusterEnabled)
	{
		return "select \n"
			+ "    * \n"
			+ "    , pg_database_size(datname) / 1024 /1024 as dbsize_mb \n"
			+ "    , cast( CASE WHEN tup_returned > 0 THEN (tup_fetched*1.0)/(tup_returned*1.0)*100.0 ELSE null END as numeric(10,2)) as fetch_efficency_pct \n"
			+ "from pg_catalog.pg_stat_database \n"
			+ "where datname not like 'template%' \n"
			+ "";
	}

	/** 
	 * Compute the AppendLogContPct for DIFF values
	 */
	@Override
	public void localCalculation(CounterSample prevSample, CounterSample newSample, CounterSample diffData)
	{
		long tup_fetched,           tup_returned;
		int  tup_fetched_pos  = -1, tup_returned_pos     = -1;

		int fetch_efficency_pct_pos = -1;

		// Find column Id's
		List<String> colNames = diffData.getColNames();
		if (colNames == null)
			return;

		for (int colId=0; colId < colNames.size(); colId++) 
		{
			String colName = colNames.get(colId);
			if      (colName.equals("tup_fetched"))         tup_fetched_pos         = colId;
			else if (colName.equals("tup_returned"))        tup_returned_pos        = colId;
			else if (colName.equals("fetch_efficency_pct")) fetch_efficency_pct_pos = colId;
		}

		// Loop on all diffData rows
		for (int rowId = 0; rowId < diffData.getRowCount(); rowId++)
		{
			tup_fetched  = ((Number)diffData.getValueAt(rowId, tup_fetched_pos )).longValue();
			tup_returned = ((Number)diffData.getValueAt(rowId, tup_returned_pos)).longValue();

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
		}
	}

	private void addTrendGraphs()
	{
		addTrendGraph(GRAPH_NAME_FETCH_EFFECIENT_PCT,
			"Row Fetch Efficiency", 	                // Menu CheckBox text
			"Row Fetch Efficiency (tup_fetched/tup_returned) in Percent ("+SHORT_NAME+")", // Graph Label 
			null, LabelType.Dynamic, 
			TrendGraphDataPoint.Category.CPU,
			true, // is Percent Graph
			true, // visible at start
			0,     // graph is valid from Server Version. 0 = All Versions; >0 = Valid from this version and above 
			-1);   // minimum height

		addTrendGraph(GRAPH_NAME_FETCH_EFFECIENT_ABS_PCT,
			"Row Fetch Efficiency ABS", 	                // Menu CheckBox text
			"Row Fetch Efficiency ABS (tup_fetched/tup_returned) in Percent ("+SHORT_NAME+")", // Graph Label 
			null, LabelType.Dynamic, 
			TrendGraphDataPoint.Category.CPU,
			true, // is Percent Graph
			true, // visible at start
			0,     // graph is valid from Server Version. 0 = All Versions; >0 = Valid from this version and above 
			-1);   // minimum height

		addTrendGraph(GRAPH_NAME_FETCH_EFFECIENT_SUM,
			"Row Fetch Efficiency SUM", 	                // Menu CheckBox text
			"Row Fetch Efficiency SUM [tup_returned, tup_fetched] ("+SHORT_NAME+")", // Graph Label 
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
			null, LabelType.Dynamic, 
			TrendGraphDataPoint.Category.SRV_CONFIG,
			false, // is Percent Graph
			true, // visible at start
			0,     // graph is valid from Server Version. 0 = All Versions; >0 = Valid from this version and above 
			-1);   // minimum height

		addTrendGraph(GRAPH_NAME_CONNECTIONS_SUM,
			"Connections Sum", 	                // Menu CheckBox text
			"Connections Sum ("+SHORT_NAME+")", // Graph Label 
			new String[] {"numbackends"}, 
			LabelType.Static, 
			TrendGraphDataPoint.Category.SRV_CONFIG,
			false, // is Percent Graph
			true, // visible at start
			0,     // graph is valid from Server Version. 0 = All Versions; >0 = Valid from this version and above 
			-1);   // minimum height

		addTrendGraph(GRAPH_NAME_COMMITS,
			"Commits", 	                           // Menu CheckBox text
			"Commits per second ("+SHORT_NAME+")", // Graph Label 
			null, LabelType.Dynamic, 
			TrendGraphDataPoint.Category.OPERATIONS,
			false, // is Percent Graph
			true, // visible at start
			0,     // graph is valid from Server Version. 0 = All Versions; >0 = Valid from this version and above 
			-1);   // minimum height

		addTrendGraph(GRAPH_NAME_ROLLBACKS,
			"Rollbacks", 	                         // Menu CheckBox text
			"Rollbacks per second ("+SHORT_NAME+")", // Graph Label 
			null, LabelType.Dynamic, 
			TrendGraphDataPoint.Category.OPERATIONS,
			false, // is Percent Graph
			true, // visible at start
			0,     // graph is valid from Server Version. 0 = All Versions; >0 = Valid from this version and above 
			-1);   // minimum height

		addTrendGraph(GRAPH_NAME_READS,
			"Reads", 	                         // Menu CheckBox text
			"Reads per second ("+SHORT_NAME+")", // Graph Label 
			null, LabelType.Dynamic, 
			TrendGraphDataPoint.Category.DISK,
			false, // is Percent Graph
			true, // visible at start
			0,     // graph is valid from Server Version. 0 = All Versions; >0 = Valid from this version and above 
			-1);   // minimum height

		addTrendGraph(GRAPH_NAME_CACHE_HITS,
			"Cache Hits", 	                         // Menu CheckBox text
			"Cache Hits per second ("+SHORT_NAME+")", // Graph Label 
			null, LabelType.Dynamic, 
			TrendGraphDataPoint.Category.CACHE,
			false, // is Percent Graph
			true, // visible at start
			0,     // graph is valid from Server Version. 0 = All Versions; >0 = Valid from this version and above 
			-1);   // minimum height

		addTrendGraph(GRAPH_NAME_ROWS_RETURNED,
			"Rows Returned", 	                         // Menu CheckBox text
			"Rows Returned per second ("+SHORT_NAME+")", // Graph Label 
			null, LabelType.Dynamic, 
			TrendGraphDataPoint.Category.CPU,
			false, // is Percent Graph
			true, // visible at start
			0,     // graph is valid from Server Version. 0 = All Versions; >0 = Valid from this version and above 
			-1);   // minimum height

		addTrendGraph(GRAPH_NAME_ROWS_FETCHED,
			"Rows Fetched", 	                         // Menu CheckBox text
			"Rows Fetched per second ("+SHORT_NAME+")", // Graph Label 
			null, LabelType.Dynamic, 
			TrendGraphDataPoint.Category.CPU,
			false, // is Percent Graph
			true, // visible at start
			0,     // graph is valid from Server Version. 0 = All Versions; >0 = Valid from this version and above 
			-1);   // minimum height

		addTrendGraph(GRAPH_NAME_ROWS_INSERTS,
			"Rows Inserted", 	                         // Menu CheckBox text
			"Rows Inserted per second ("+SHORT_NAME+")", // Graph Label 
			null, LabelType.Dynamic, 
			TrendGraphDataPoint.Category.OPERATIONS,
			false, // is Percent Graph
			true, // visible at start
			0,     // graph is valid from Server Version. 0 = All Versions; >0 = Valid from this version and above 
			-1);   // minimum height

		addTrendGraph(GRAPH_NAME_ROWS_UPDATED,
			"Rows Updated", 	                         // Menu CheckBox text
			"Rows Updated per second ("+SHORT_NAME+")", // Graph Label 
			null, LabelType.Dynamic, 
			TrendGraphDataPoint.Category.OPERATIONS,
			false, // is Percent Graph
			true, // visible at start
			0,     // graph is valid from Server Version. 0 = All Versions; >0 = Valid from this version and above 
			-1);   // minimum height

		addTrendGraph(GRAPH_NAME_ROWS_DELETED,
			"Rows Deleted", 	                         // Menu CheckBox text
			"Rows Deleted per second ("+SHORT_NAME+")", // Graph Label 
			null, LabelType.Dynamic, 
			TrendGraphDataPoint.Category.OPERATIONS,
			false, // is Percent Graph
			true, // visible at start
			0,     // graph is valid from Server Version. 0 = All Versions; >0 = Valid from this version and above 
			-1);   // minimum height

		addTrendGraph(GRAPH_NAME_ROWS_CONFLICTS,
			"Conflicting Statements", 	                         // Menu CheckBox text
			"Conflicting Statements per second ("+SHORT_NAME+")", // Graph Label 
			null, LabelType.Dynamic, 
			TrendGraphDataPoint.Category.OPERATIONS,
			false, // is Percent Graph
			false, // visible at start
			0,     // graph is valid from Server Version. 0 = All Versions; >0 = Valid from this version and above 
			-1);   // minimum height

		addTrendGraph(GRAPH_NAME_TEMP_FILES,
			"Temp Files Created", 	                         // Menu CheckBox text
			"Temp Files Created per second ("+SHORT_NAME+")", // Graph Label 
			null, LabelType.Dynamic, 
			TrendGraphDataPoint.Category.DISK,
			false, // is Percent Graph
			true, // visible at start
			0,     // graph is valid from Server Version. 0 = All Versions; >0 = Valid from this version and above 
			-1);   // minimum height

		addTrendGraph(GRAPH_NAME_TEMP_BYTES,
			"Temp Bytes", 	                         // Menu CheckBox text
			"Temp Bytes per second ("+SHORT_NAME+")", // Graph Label 
			null, LabelType.Dynamic, 
			TrendGraphDataPoint.Category.DISK,
			false, // is Percent Graph
			true, // visible at start
			0,     // graph is valid from Server Version. 0 = All Versions; >0 = Valid from this version and above 
			-1);   // minimum height

		addTrendGraph(GRAPH_NAME_DEADLOCKS,
			"Deadlocks", 	                         // Menu CheckBox text
			"Deadlocks per second ("+SHORT_NAME+")", // Graph Label 
			null, LabelType.Dynamic, 
			TrendGraphDataPoint.Category.OPERATIONS,
			false, // is Percent Graph
			false, // visible at start
			0,     // graph is valid from Server Version. 0 = All Versions; >0 = Valid from this version and above 
			-1);   // minimum height

		addTrendGraph(GRAPH_NAME_READ_TIME,
			"Read Time", 	                         // Menu CheckBox text
			"Read Time per second ("+SHORT_NAME+")", // Graph Label 
			null, LabelType.Dynamic, 
			TrendGraphDataPoint.Category.DISK,
			false, // is Percent Graph
			true, // visible at start
			0,     // graph is valid from Server Version. 0 = All Versions; >0 = Valid from this version and above 
			-1);   // minimum height

		addTrendGraph(GRAPH_NAME_WRITE_TIME,
			"Write Time", 	                         // Menu CheckBox text
			"Write Time per second ("+SHORT_NAME+")", // Graph Label 
			null, LabelType.Dynamic, 
			TrendGraphDataPoint.Category.DISK,
			false, // is Percent Graph
			true, // visible at start
			0,     // graph is valid from Server Version. 0 = All Versions; >0 = Valid from this version and above 
			-1);   // minimum height

		addTrendGraph(GRAPH_NAME_DBSIZE_MB,
			"DB Size in MB", 	                         // Menu CheckBox text
			"DB Size in MB ("+SHORT_NAME+")", // Graph Label 
			null, LabelType.Dynamic, 
			TrendGraphDataPoint.Category.SPACE,
			false, // is Percent Graph
			true, // visible at start
			0,     // graph is valid from Server Version. 0 = All Versions; >0 = Valid from this version and above 
			-1);   // minimum height
	}


	private void localUpdateGraphData(TrendGraphDataPoint tgdp, int dataType, String colname)
	{
//		// Get database count (do dot include template databases)
//		int size = 0;
//		for (int i = 0; i < this.size(); i++)
//		{
//			String dbname = this.getAbsString(i, "datname");
//			if (dbname != null && !dbname.startsWith("template"))
//				size++;
//		}
			
		
		// Write 1 "line" for every db (except for 'template*' databases)
		Double[] dArray = new Double[this.size()];
		String[] lArray = new String[dArray.length];
		int ap = 0;
		for (int r = 0; r < dArray.length; r++) // we still need to loop all rows...
		{
			String dbname = this.getAbsString(r, "datname");
			if (dbname != null && !dbname.startsWith("template"))
			{
				Double data;
				if      (dataType == CountersModel.DATA_ABS)  data = this.getAbsValueAsDouble (r, colname);
				else if (dataType == CountersModel.DATA_DIFF) data = this.getDiffValueAsDouble(r, colname);
				else if (dataType == CountersModel.DATA_RATE) data = this.getRateValueAsDouble(r, colname);
				else throw new RuntimeException("dataType(tgName="+tgdp.getName()+"): Unsupported dataType="+dataType);
				
				lArray[ap] = dbname;
				dArray[ap] = data;
				ap++;
			}
		}

		// Set the values
		tgdp.setDataPoint(this.getTimestamp(), lArray, dArray);
	}
	
	@Override
	public void updateGraphData(TrendGraphDataPoint tgdp)
	{
		if (GRAPH_NAME_FETCH_EFFECIENT_PCT    .equals(tgdp.getName())) localUpdateGraphData(tgdp, CountersModel.DATA_RATE, "fetch_efficency_pct");
		if (GRAPH_NAME_FETCH_EFFECIENT_ABS_PCT.equals(tgdp.getName())) localUpdateGraphData(tgdp, CountersModel.DATA_ABS,  "fetch_efficency_pct");
		if (GRAPH_NAME_CONNECTIONS            .equals(tgdp.getName())) localUpdateGraphData(tgdp, CountersModel.DATA_ABS,  "numbackends");
		if (GRAPH_NAME_COMMITS                .equals(tgdp.getName())) localUpdateGraphData(tgdp, CountersModel.DATA_RATE, "xact_commit");
		if (GRAPH_NAME_ROLLBACKS              .equals(tgdp.getName())) localUpdateGraphData(tgdp, CountersModel.DATA_RATE, "xact_rollback");
		if (GRAPH_NAME_READS                  .equals(tgdp.getName())) localUpdateGraphData(tgdp, CountersModel.DATA_RATE, "blks_read");
		if (GRAPH_NAME_CACHE_HITS             .equals(tgdp.getName())) localUpdateGraphData(tgdp, CountersModel.DATA_RATE, "blks_hit");
		if (GRAPH_NAME_ROWS_RETURNED          .equals(tgdp.getName())) localUpdateGraphData(tgdp, CountersModel.DATA_RATE, "tup_returned");
		if (GRAPH_NAME_ROWS_FETCHED           .equals(tgdp.getName())) localUpdateGraphData(tgdp, CountersModel.DATA_RATE, "tup_fetched");
		if (GRAPH_NAME_ROWS_INSERTS           .equals(tgdp.getName())) localUpdateGraphData(tgdp, CountersModel.DATA_RATE, "tup_inserted");
		if (GRAPH_NAME_ROWS_UPDATED           .equals(tgdp.getName())) localUpdateGraphData(tgdp, CountersModel.DATA_RATE, "tup_updated");
		if (GRAPH_NAME_ROWS_DELETED           .equals(tgdp.getName())) localUpdateGraphData(tgdp, CountersModel.DATA_RATE, "tup_deleted");
		if (GRAPH_NAME_ROWS_CONFLICTS         .equals(tgdp.getName())) localUpdateGraphData(tgdp, CountersModel.DATA_RATE, "conflicts");
		if (GRAPH_NAME_TEMP_FILES             .equals(tgdp.getName())) localUpdateGraphData(tgdp, CountersModel.DATA_RATE, "temp_files");
		if (GRAPH_NAME_TEMP_BYTES             .equals(tgdp.getName())) localUpdateGraphData(tgdp, CountersModel.DATA_RATE, "temp_bytes");
		if (GRAPH_NAME_DEADLOCKS              .equals(tgdp.getName())) localUpdateGraphData(tgdp, CountersModel.DATA_RATE, "deadlocks");
		if (GRAPH_NAME_READ_TIME              .equals(tgdp.getName())) localUpdateGraphData(tgdp, CountersModel.DATA_RATE, "blk_read_time");
		if (GRAPH_NAME_WRITE_TIME             .equals(tgdp.getName())) localUpdateGraphData(tgdp, CountersModel.DATA_RATE, "blk_write_time");
		if (GRAPH_NAME_DBSIZE_MB              .equals(tgdp.getName())) localUpdateGraphData(tgdp, CountersModel.DATA_ABS,  "dbsize_mb");

		if (GRAPH_NAME_CONNECTIONS_SUM.equals(tgdp.getName()))
		{
			Double[] arr = new Double[1];

			arr[0] = this.getAbsValueSum("numbackends");

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
}
