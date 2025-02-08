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
package com.dbxtune.cm.postgres;

import java.lang.invoke.MethodHandles;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import javax.naming.NameNotFoundException;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.dbxtune.ICounterController;
import com.dbxtune.IGuiController;
import com.dbxtune.central.pcs.CentralPersistReader;
import com.dbxtune.cm.CounterSample;
import com.dbxtune.cm.CounterSetTemplates;
import com.dbxtune.cm.CounterSetTemplates.Type;
import com.dbxtune.cm.CountersModel;
import com.dbxtune.config.dict.MonTablesDictionary;
import com.dbxtune.config.dict.MonTablesDictionaryManager;
import com.dbxtune.graph.TrendGraphDataPoint;
import com.dbxtune.graph.TrendGraphDataPoint.LabelType;
import com.dbxtune.gui.MainFrame;
import com.dbxtune.gui.TrendGraph;
import com.dbxtune.sql.conn.DbxConnection;
import com.dbxtune.sql.conn.info.DbmsVersionInfo;
import com.dbxtune.utils.Ver;

/**
 * @author Goran Schwarz (goran_schwarz@hotmail.com)
 */
public class CmPgIo
extends CountersModel
{
	private static final Logger _logger = LogManager.getLogger(MethodHandles.lookup().lookupClass());
	private static final long    serialVersionUID = 1L;

	public static final String   CM_NAME          = CmPgIo.class.getSimpleName();
	public static final String   SHORT_NAME       = "IO Activity";
	public static final String   HTML_DESC        = 
		"<html>" +
		"IO Statistics by each backend type in the Postgres Instance. (from pg_stat_io) <br>" +
		"A good intro how <code>pg_stat_io</code> is working look at: https://www.youtube.com/watch?v=rCzSNdUOEdg <br>" + 
		"</html>";

	public static final String   GROUP_NAME       = MainFrame.TCP_GROUP_SERVER;
	public static final String   GUI_ICON_FILE    = "images/"+CM_NAME+".png";

	public static final long     NEED_SRV_VERSION = Ver.ver(16);
	public static final long     NEED_CE_VERSION  = 0;

	public static final String[] MON_TABLES       = new String[] {"pg_stat_io", CM_NAME};
	public static final String[] NEED_ROLES       = new String[] {};
	public static final String[] NEED_CONFIG      = new String[] {};

	public static final String[] PCT_COLUMNS      = new String[] {
			"cache_hit_pct"
	};
	public static final String[] DIFF_COLUMNS     = new String[] {
			"hits",
			"evictions",
			"reuses",
			"reads",
			"read_time_ms",
			"writes",
			"write_time_ms",
			"writebacks",
			"writeback_time_ms",
			"extends",
			"extend_time_ms",
			"fsyncs",
			"fsync_time_ms"
	};

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

		return new CmPgIo(counterController, guiController);
	}

	public CmPgIo(ICounterController counterController, IGuiController guiController)
	{
		super(counterController, guiController,
				CM_NAME, GROUP_NAME, /*sql*/null, /*pkList*/null, 
				DIFF_COLUMNS, PCT_COLUMNS, MON_TABLES, 
				NEED_ROLES, NEED_CONFIG, NEED_SRV_VERSION, NEED_CE_VERSION, 
				NEGATIVE_DIFF_COUNTERS_TO_ZERO, IS_SYSTEM_CM, DEFAULT_POSTPONE_TIME);

		setDisplayName(SHORT_NAME);
		setDescription(HTML_DESC);

		setIconFile(GUI_ICON_FILE);

//		addDependsOnCm(CmXxx.CM_NAME); // CmXxx "must" have been executed before this cm
		
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
//		return new CmPgIoPanel(this);
//	}
	
	@Override
	public void addMonTableDictForVersion(DbxConnection conn, DbmsVersionInfo versionInfo)
	{
		try 
		{
			MonTablesDictionary mtd = MonTablesDictionaryManager.getInstance();

			mtd.addTable(CM_NAME, HTML_DESC);
			
			// LOOK AT: Additional IO Observability in Postgres with pg_stat_io | Citus Con 2023
			//          https://www.youtube.com/watch?v=rCzSNdUOEdg
			
			mtd.addColumn(CM_NAME, "backend_type"          ,"<html>Type of backend (e.g. background worker, autovacuum worker). <br>See pg_stat_activity for more information on backend_types. <br>Some backend_types do not accumulate I/O operation statistics and will not be included in the view.</html>");
			mtd.addColumn(CM_NAME, "object"                ,"<html>Target object of an I/O operation. Possible values are: 'relation', 'temp relation'</html>");
			mtd.addColumn(CM_NAME, "context"               ,"<html>The context of an I/O operation. Possible values are:"
			                                                     + "<ul>"
			                                                     + "  <li><b>normal</b>    <br>The default or standard context for a type of I/O operation. <br>For example, by default, relation data is read into and written out from shared buffers. <br>Thus, reads and writes of relation data to and from shared buffers are tracked in context normal.<br>&nbsp;<br></li>"
			                                                     + "  <li><b>vacuum</b>    <br>I/O operations performed outside of shared buffers while vacuuming and analyzing permanent relations. <br>Temporary table vacuums use the same local buffer pool as other temporary table I/O operations and are tracked in context normal.<br>&nbsp;<br></li>"
			                                                     + "  <li><b>bulkread</b>  <br>Certain large read I/O operations done outside of shared buffers, for example, a sequential scan of a large table.<br>&nbsp;<br></li>"
			                                                     + "  <li><b>bulkwrite</b> <br>Certain large write I/O operations done outside of shared buffers, such as COPY.<br>&nbsp;<br></li>"
			                                                     + "</ul>"		
			                                                     + "</html>");
			mtd.addColumn(CM_NAME, "cache_hit_pct"         ,"<html>"
			                                                     + "Cache hit percent. (most valid for: <code>backend_type = 'client backend' and object = 'relation' and context = 'normal'</code><br>"
			                                                     + "<b>Algorithm</b>: <code>(hits/(reads + hits)::float) * 100</code>"
			                                                     + "</html>");
			mtd.addColumn(CM_NAME, "hits"                  ,"<html>The number of times a desired block was found in a shared buffer.</html>");
			mtd.addColumn(CM_NAME, "evictions"             ,"<html>Number of times a block has been written out from a shared or local buffer in order to make it available for another use. "
			                                                     + "<br>In context normal, this counts the number of times a block was evicted from a buffer and replaced with another block. <br>"
			                                                     + "In contexts bulkwrite, bulkread, and vacuum, this counts the number of times a block was evicted from shared buffers in order <br>"
			                                                     + "to add the shared buffer to a separate, size-limited ring buffer for use in a bulk I/O operation."
			                                                     + "</html>");
			mtd.addColumn(CM_NAME, "reuses"                ,"<html>The number of times an existing buffer in a size-limited ring buffer outside of shared buffers was reused as part of an I/O operation in the bulkread, bulkwrite, or vacuum contexts.</html>");
			mtd.addColumn(CM_NAME, "reads"                 ,"<html>Number of read operations, each of the size specified in op_bytes.</html>");
			mtd.addColumn(CM_NAME, "read_time_ms"          ,"<html>Time spent in read operations in milliseconds (if track_io_timing is enabled, otherwise zero)</html>");
			mtd.addColumn(CM_NAME, "read_time_avg_ms"      ,"<html>Average 'read_time_ms' per 'reads' <br><b>Formula</b>: read_time_ms / reads</html>");
			mtd.addColumn(CM_NAME, "writes"                ,"<html>Number of write operations, each of the size specified in op_bytes.</html>");
			mtd.addColumn(CM_NAME, "write_time_ms"         ,"<html>Time spent in write operations in milliseconds (if track_io_timing is enabled, otherwise zero)</html>");
			mtd.addColumn(CM_NAME, "write_time_avg_ms"     ,"<html>Average 'write_time_ms' per 'writes' <br><b>Formula</b>: write_time_ms / writes</html>");
			mtd.addColumn(CM_NAME, "writebacks"            ,"<html>Number of units of size op_bytes which the process requested the kernel write out to permanent storage.</html>");
			mtd.addColumn(CM_NAME, "writeback_time_ms"     ,"<html>Time spent in writeback operations in milliseconds (if track_io_timing is enabled, otherwise zero). <br>This includes the time spent queueing write-out requests and, potentially, the time spent to write out the dirty data.</html>");
			mtd.addColumn(CM_NAME, "writeback_time_avg_ms" ,"<html>Average 'writeback_time_ms' per 'writebacks' <br><b>Formula</b>: writeback_time_ms / writebacks</html>");
			mtd.addColumn(CM_NAME, "extends"               ,"<html>Number of relation extend operations (or file growth), each of the size specified in op_bytes.</html>");
			mtd.addColumn(CM_NAME, "extend_time_ms"        ,"<html>Time spent in extend operations in milliseconds (if track_io_timing is enabled, otherwise zero)</html>");
			mtd.addColumn(CM_NAME, "extend_time_avg_ms"    ,"<html>Average 'extend_time_ms' per 'extends' <br><b>Formula</b>: extend_time_ms / extends</html>");
			mtd.addColumn(CM_NAME, "fsyncs"                ,"<html>Number of fsync calls. These are only tracked in context normal.</html>");
			mtd.addColumn(CM_NAME, "fsync_time_ms"         ,"<html>Time spent in fsync operations in milliseconds (if track_io_timing is enabled, otherwise zero)</html>");
			mtd.addColumn(CM_NAME, "fsync_time_avg_ms"     ,"<html>Average 'fsync_time_ms' per 'fsyncs' <br><b>Formula</b>: fsync_time_ms / fsyncs</html>");
			mtd.addColumn(CM_NAME, "op_bytes"              ,"<html>The number of bytes per unit of I/O read, written, or extended. <br>Relation data reads, writes, and extends are done in block_size units, derived from the build-time parameter BLCKSZ, which is 8192 by default.</html>");
			mtd.addColumn(CM_NAME, "stats_reset"           ,"<html>Time at which these statistics were last reset.</html>");

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

		pkCols.add("backend_type");
		pkCols.add("object");
		pkCols.add("context");

		return pkCols;
	}

	@Override
	public void localCalculation(CounterSample prevSample, CounterSample newSample, CounterSample diffData)
	{
		int pos_cache_hit_pct         = diffData.findColumn("cache_hit_pct");
		int pos_hits                  = diffData.findColumn("hits");

		int pos_reads                 = diffData.findColumn("reads");
		int pos_read_time_ms          = diffData.findColumn("read_time_ms");
		int pos_read_time_avg_ms      = diffData.findColumn("read_time_avg_ms");

		int pos_writes                = diffData.findColumn("writes");
		int pos_write_time_ms         = diffData.findColumn("write_time_ms");
		int pos_write_time_avg_ms     = diffData.findColumn("write_time_avg_ms");
		
		int pos_writebacks            = diffData.findColumn("writebacks");
		int pos_writeback_time_ms     = diffData.findColumn("writeback_time_ms");
		int pos_writeback_time_avg_ms = diffData.findColumn("writeback_time_avg_ms");
		
		int pos_extends               = diffData.findColumn("extends");
		int pos_extend_time_ms        = diffData.findColumn("extend_time_ms");
		int pos_extend_time_avg_ms    = diffData.findColumn("extend_time_avg_ms");

		int pos_fsyncs                = diffData.findColumn("fsyncs");
		int pos_fsync_time_ms         = diffData.findColumn("fsync_time_ms");
		int pos_fsync_time_avg_ms     = diffData.findColumn("fsync_time_avg_ms");

		// Loop on all diffData rows
		for (int rowId = 0; rowId < diffData.getRowCount(); rowId++)
		{
			long val_reads             = diffData.getValueAsLong(rowId, pos_reads       , 0L);
			long val_read_time_ms      = diffData.getValueAsLong(rowId, pos_read_time_ms, 0L);
			diffData.setValueAt(calcMsPerOp(val_reads, val_read_time_ms), rowId, pos_read_time_avg_ms);

			long val_writes            = diffData.getValueAsLong(rowId, pos_writes       , 0L);
			long val_write_time_ms     = diffData.getValueAsLong(rowId, pos_write_time_ms, 0L);
			diffData.setValueAt(calcMsPerOp(val_writes, val_write_time_ms), rowId, pos_write_time_avg_ms);

			long val_writebacks        = diffData.getValueAsLong(rowId, pos_writebacks       , 0L);
			long val_writeback_time_ms = diffData.getValueAsLong(rowId, pos_writeback_time_ms, 0L);
			diffData.setValueAt(calcMsPerOp(val_writebacks, val_writeback_time_ms), rowId, pos_writeback_time_avg_ms);

			long val_extends           = diffData.getValueAsLong(rowId, pos_extends       , 0L);
			long val_extend_time_ms    = diffData.getValueAsLong(rowId, pos_extend_time_ms, 0L);
			diffData.setValueAt(calcMsPerOp(val_extends, val_extend_time_ms), rowId, pos_extend_time_avg_ms);

			long val_fsyncs            = diffData.getValueAsLong(rowId, pos_fsyncs       , 0L);
			long val_fsync_time_ms     = diffData.getValueAsLong(rowId, pos_fsync_time_ms, 0L);
			diffData.setValueAt(calcMsPerOp(val_fsyncs, val_fsync_time_ms), rowId, pos_fsync_time_avg_ms);
			
			// Cache hits
			long val_hits              = diffData.getValueAsLong(rowId, pos_hits       , 0L);
		//	long val_reads             = diffData.getValueAsLong(rowId, pos_reads       , 0L);
			diffData.setValueAt(calcHitRatePct(val_hits, val_reads), rowId, pos_cache_hit_pct);
		}
	}

	private BigDecimal calcMsPerOp(long cnt, long ms)
	{
		if (cnt > 0)
		{
			double calc = (ms * 1.0) / (cnt * 1.0);
			return new BigDecimal(calc).setScale(1, BigDecimal.ROUND_HALF_EVEN);
		}
		
		return new BigDecimal(0);
	}

	private BigDecimal calcHitRatePct(long hits, long reads)
	{
		if ((hits + reads) > 0)
		{
			double calc = (hits*1.0 / (reads*1.0 + hits*1.0)) * 100.0;
			return new BigDecimal(calc).setScale(3, BigDecimal.ROUND_HALF_EVEN);
		}
		
//		return new BigDecimal(0);
		return null;
	}

	@Override
	public Map<String, AggregationType> createAggregateColumns()
	{
		// Create a specific callback to do User Defined Average Aggregation on 'granted_memory_pct' and 'used_memory_pct'
		AggregationAverageCallback aggCb = new AggregationAverageCallback()
		{
			@Override
			public Object doAvgCalc(CountersModel countersModel, CounterSample cs, int counterType, int aggRowId, String colName, int jdbcType)
			{
				if ("cache_hit_pct".equals(colName))
				{
					long val_hits  = cs.getValueAsLong(aggRowId, "hits" , true, 0L);
					long val_reads = cs.getValueAsLong(aggRowId, "reads", true, 0L);

					return calcHitRatePct(val_hits, val_reads);
				}
		
				//-----------------------------------------------------------------------------------------------
				// Calculate 'read_time_avg_ms' based on the values from the the values in the Aggregated Columns
				//-----------------------------------------------------------------------------------------------
				if ("read_time_avg_ms".equals(colName))
				{
					long val_counter = cs.getValueAsLong(aggRowId, "reads"         , true, 0L);
					long val_time_ms = cs.getValueAsLong(aggRowId, "read_time_ms"  , true, 0L);

					return calcMsPerOp(val_counter, val_time_ms);
				}
		
				if ("write_time_avg_ms".equals(colName))
				{
					long val_counter = cs.getValueAsLong(aggRowId, "writes"         , true, 0L);
					long val_time_ms = cs.getValueAsLong(aggRowId, "write_time_ms"  , true, 0L);

					return calcMsPerOp(val_counter, val_time_ms);
				}
		
				if ("writeback_time_avg_ms".equals(colName))
				{
					long val_counter = cs.getValueAsLong(aggRowId, "writebacks"         , true, 0L);
					long val_time_ms = cs.getValueAsLong(aggRowId, "writeback_time_ms"  , true, 0L);

					return calcMsPerOp(val_counter, val_time_ms);
				}

				if ("extend_time_avg_ms".equals(colName))
				{
					long val_counter = cs.getValueAsLong(aggRowId, "extends"         , true, 0L);
					long val_time_ms = cs.getValueAsLong(aggRowId, "extend_time_ms"  , true, 0L);

					return calcMsPerOp(val_counter, val_time_ms);
				}

				if ("fsync_time_avg_ms".equals(colName))
				{
					long val_counter = cs.getValueAsLong(aggRowId, "fsyncs"         , true, 0L);
					long val_time_ms = cs.getValueAsLong(aggRowId, "fsync_time_ms"  , true, 0L);

					return calcMsPerOp(val_counter, val_time_ms);
				}
		
				
				throw new RuntimeException("in doAvgCalc() for CM='" + getName() + "' unhandled column name '" + colName + "'.");
			}
		};
		
		HashMap<String, AggregationType> aggColumns = new HashMap<>(getColumnCount());

		AggregationType tmp;
		
		// Create the columns :::::::::::::::::::::::::::::::::::::::::::::::::::::: And ADD it to the return Map 
		tmp = new AggregationType("cache_hit_pct"        , AggregationType.Agg.AVG, aggCb);   aggColumns.put(tmp.getColumnName(), tmp);
		tmp = new AggregationType("hits"                 , AggregationType.Agg.SUM);          aggColumns.put(tmp.getColumnName(), tmp);
		tmp = new AggregationType("evictions"            , AggregationType.Agg.SUM);          aggColumns.put(tmp.getColumnName(), tmp);
		tmp = new AggregationType("reuses"               , AggregationType.Agg.SUM);          aggColumns.put(tmp.getColumnName(), tmp);
		tmp = new AggregationType("reads"                , AggregationType.Agg.SUM);          aggColumns.put(tmp.getColumnName(), tmp);
		tmp = new AggregationType("read_time_ms"         , AggregationType.Agg.SUM);          aggColumns.put(tmp.getColumnName(), tmp);
		tmp = new AggregationType("read_time_avg_ms"     , AggregationType.Agg.AVG, aggCb);   aggColumns.put(tmp.getColumnName(), tmp);
		tmp = new AggregationType("writes"               , AggregationType.Agg.SUM);          aggColumns.put(tmp.getColumnName(), tmp);
		tmp = new AggregationType("write_time_ms"        , AggregationType.Agg.SUM);          aggColumns.put(tmp.getColumnName(), tmp);
		tmp = new AggregationType("write_time_avg_ms"    , AggregationType.Agg.AVG, aggCb);   aggColumns.put(tmp.getColumnName(), tmp);
		tmp = new AggregationType("writebacks"           , AggregationType.Agg.SUM);          aggColumns.put(tmp.getColumnName(), tmp);
		tmp = new AggregationType("writeback_time_ms"    , AggregationType.Agg.SUM);          aggColumns.put(tmp.getColumnName(), tmp);
		tmp = new AggregationType("writeback_time_avg_ms", AggregationType.Agg.AVG, aggCb);   aggColumns.put(tmp.getColumnName(), tmp);
		tmp = new AggregationType("extends"              , AggregationType.Agg.SUM);          aggColumns.put(tmp.getColumnName(), tmp);
		tmp = new AggregationType("extend_time_ms"       , AggregationType.Agg.SUM);          aggColumns.put(tmp.getColumnName(), tmp);
		tmp = new AggregationType("extend_time_avg_ms"   , AggregationType.Agg.AVG, aggCb);   aggColumns.put(tmp.getColumnName(), tmp);
		tmp = new AggregationType("fsyncs"               , AggregationType.Agg.SUM);          aggColumns.put(tmp.getColumnName(), tmp);
		tmp = new AggregationType("fsync_time_ms"        , AggregationType.Agg.SUM);          aggColumns.put(tmp.getColumnName(), tmp);
		tmp = new AggregationType("fsync_time_avg_ms"    , AggregationType.Agg.AVG, aggCb);   aggColumns.put(tmp.getColumnName(), tmp);
		tmp = new AggregationType("op_bytes"             , AggregationType.Agg.AVG);          aggColumns.put(tmp.getColumnName(), tmp);


		return aggColumns;
	}

	@Override
	public Object calculateAggregateRow_getAggregatePkColumnDataProvider(CounterSample newSample, String colName, int c, int jdbcType, Object addValue)
	{
		if ("backend_type".equalsIgnoreCase(colName)) return "_Total";
		if ("object"      .equalsIgnoreCase(colName)) return "_Total";
		if ("context"     .equalsIgnoreCase(colName)) return "_Total";
		
		return addValue;
	}

	// TODO: PROBABLY Calculate AVG values better: use: sum_time / sum_value

//	@Override
//	public Object calculateAggregateRow_nonAggregatedColumnDataProvider(CounterSample newSample, String colName, int c, int jdbcType, Object addValue)
//	{
//		if ("stats_reset"  .equalsIgnoreCase(colName)) return null; // OR: new Timestamp(...);
//		
//		return null;
//	}

	@Override
	public String getSqlForVersion(DbxConnection conn, DbmsVersionInfo versionInfo)
	{
		// Build SQL
		String sql = ""
			    + "select \n"
			    + "     CAST(backend_type   as varchar(60))                         AS backend_type \n"
			    + "    ,CAST(object         as varchar(60))                         AS object \n"
			    + "    ,CAST(context        as varchar(60))                         AS context \n"

			    + "    ,CAST((hits/(nullif(reads + hits, 0))::float) * 100 as numeric(10,3)) AS cache_hit_pct \n"
			    + "    ,hits \n"
			    + "    ,evictions \n"
			    + "    ,reuses \n"

			    + "    ,reads \n"
			    + "    ,CAST(read_time      as bigint)                              AS read_time_ms \n"
			    + "    ,CAST(read_time      / nullif(reads     ,0) as numeric(8,1)) AS read_time_avg_ms \n"

			    + "    ,writes \n"
			    + "    ,CAST(write_time     as bigint)                              AS write_time_ms \n"
			    + "    ,CAST(write_time     / nullif(writes    ,0) as numeric(8,1)) AS write_time_avg_ms \n"

			    + "    ,writebacks \n"
			    + "    ,CAST(writeback_time as bigint)                              AS writeback_time_ms \n"
			    + "    ,CAST(writeback_time / nullif(writebacks,0) as numeric(8,1)) AS writeback_time_avg_ms \n"

			    + "    ,extends \n"
			    + "    ,CAST(extend_time    as bigint)                              AS extend_time_ms \n"
			    + "    ,CAST(extend_time    / nullif(extends   ,0) as numeric(8,1)) AS extend_time_avg_ms \n"

			    + "    ,fsyncs \n"
			    + "    ,CAST(fsync_time     as bigint)                              AS fsync_time_ms \n"
			    + "    ,CAST(fsync_time     / nullif(fsyncs    ,0) as numeric(8,1)) AS  fsync_time_avg_ms \n"

			    + "    ,op_bytes \n"
			    + "    ,stats_reset \n"
			    + "from pg_stat_io \n"
			    + "order by context, backend_type, object \n"
			    + "";
		
		return sql;
	}

	public static final String GRAPH_NAME_CACHE_HIT_NORMAL_PCT = "IoCacheHitNormalPct";
	public static final String GRAPH_NAME_CACHE_HIT_ALL_PCT    = "IoCacheHitAllPct";
	public static final String GRAPH_NAME_HITS                 = "IoHitsPerPk";
	public static final String GRAPH_NAME_EVICTIONS            = "IoEvectionsPerPk";
	public static final String GRAPH_NAME_REUSES               = "IoReusesPerPk";

	public static final String GRAPH_NAME_READS                = "IoReadsPerPk";
	public static final String GRAPH_NAME_READ_TIME            = "IoReadTimePerPk";
	public static final String GRAPH_NAME_WRITES               = "IoWritesPerPk";
	public static final String GRAPH_NAME_WRITE_TIME           = "IoWriteTimePerPk";
	public static final String GRAPH_NAME_WRITEBACKS           = "IoWritebacksPerPk";
	public static final String GRAPH_NAME_WRITEBACK_TIME       = "IoWritebackTimePerPk";
	public static final String GRAPH_NAME_EXTENDS              = "IoExtendsPerPk";
	public static final String GRAPH_NAME_EXTEND_TIME          = "IoExtendTimePerPk";
	public static final String GRAPH_NAME_FSYNCS               = "IoFsyncsPerPk";
	public static final String GRAPH_NAME_FSYNC_TIME           = "IoFsyncTimePerPk";

//	TODO; // Use this "somehow" in Daily Summary Report

	private void addTrendGraphs()
	{
		//-----------------------------
		addTrendGraph(GRAPH_NAME_CACHE_HIT_NORMAL_PCT,
				"Buffer 'Cache Hit' By 'Client Table Access' in Percent", 	                // Menu CheckBox text
				"Buffer 'Cache Hit' By 'Client Table Access' in Percent ("+SHORT_NAME+")", // Graph Label 
				TrendGraphDataPoint.createGraphProps(TrendGraphDataPoint.Y_AXIS_SCALE_LABELS_COUNT, CentralPersistReader.SampleType.MAX_OVER_SAMPLES),
				new String[] {"client backend::relation::normal"}, 
				LabelType.Static, 
				TrendGraphDataPoint.Category.DISK,
				true, // is Percent Graph
				true, // visible at start
				Ver.ver(16),     // graph is valid from Server Version. 0 = All Versions; >0 = Valid from this version and above 
				-1);   // minimum height

		addTrendGraph(GRAPH_NAME_CACHE_HIT_ALL_PCT,
				"Buffer 'Cache Hit' By 'backend_type:object:context' in Percent", 	                // Menu CheckBox text
				"Buffer 'Cache Hit' By 'backend_type:object:context' in Percent ("+SHORT_NAME+")", // Graph Label 
				TrendGraphDataPoint.createGraphProps(TrendGraphDataPoint.Y_AXIS_SCALE_LABELS_COUNT, CentralPersistReader.SampleType.MAX_OVER_SAMPLES),
				null, 
				LabelType.Dynamic, 
				TrendGraphDataPoint.Category.DISK,
				true, // is Percent Graph
				true, // visible at start
				Ver.ver(16),     // graph is valid from Server Version. 0 = All Versions; >0 = Valid from this version and above 
				-1);   // minimum height

		//-----------------------------
		addTrendGraph(GRAPH_NAME_HITS,
				"Buffer 'Hits' By 'backend_type:object:context' per Second", 	                // Menu CheckBox text
				"Buffer 'Hits' By 'backend_type:object:context' per Second ("+SHORT_NAME+")", // Graph Label 
				TrendGraphDataPoint.createGraphProps(TrendGraphDataPoint.Y_AXIS_SCALE_LABELS_COUNT, CentralPersistReader.SampleType.MAX_OVER_SAMPLES),
				null, 
				LabelType.Dynamic, 
				TrendGraphDataPoint.Category.DISK,
				false, // is Percent Graph
				true, // visible at start
				Ver.ver(16),     // graph is valid from Server Version. 0 = All Versions; >0 = Valid from this version and above 
				-1);   // minimum height

		addTrendGraph(GRAPH_NAME_EVICTIONS,
				"Buffer 'Evictions' By 'backend_type:object:context' per Second", 	                // Menu CheckBox text
				"Buffer 'Evictions' By 'backend_type:object:context' per Second ("+SHORT_NAME+")", // Graph Label 
				TrendGraphDataPoint.createGraphProps(TrendGraphDataPoint.Y_AXIS_SCALE_LABELS_COUNT, CentralPersistReader.SampleType.MAX_OVER_SAMPLES),
				null, 
				LabelType.Dynamic, 
				TrendGraphDataPoint.Category.DISK,
				false, // is Percent Graph
				true, // visible at start
				Ver.ver(16),     // graph is valid from Server Version. 0 = All Versions; >0 = Valid from this version and above 
				-1);   // minimum height

		addTrendGraph(GRAPH_NAME_REUSES,
				"Buffer 'Reuses' By 'backend_type:object:context' per Second", 	                // Menu CheckBox text
				"Buffer 'Reuses' By 'backend_type:object:context' per Second ("+SHORT_NAME+")", // Graph Label 
				TrendGraphDataPoint.createGraphProps(TrendGraphDataPoint.Y_AXIS_SCALE_LABELS_COUNT, CentralPersistReader.SampleType.MAX_OVER_SAMPLES),
				null, 
				LabelType.Dynamic, 
				TrendGraphDataPoint.Category.DISK,
				false, // is Percent Graph
				false, // visible at start
				Ver.ver(16),     // graph is valid from Server Version. 0 = All Versions; >0 = Valid from this version and above 
				-1);   // minimum height

		//-----------------------------
		addTrendGraph(GRAPH_NAME_READS,
				"IO 'Reads' By 'backend_type:object:context' per Second", 	                // Menu CheckBox text
				"IO 'Reads' By 'backend_type:object:context' per Second ("+SHORT_NAME+")", // Graph Label 
				TrendGraphDataPoint.createGraphProps(TrendGraphDataPoint.Y_AXIS_SCALE_LABELS_COUNT, CentralPersistReader.SampleType.MAX_OVER_SAMPLES),
				null, 
				LabelType.Dynamic, 
				TrendGraphDataPoint.Category.DISK,
				false, // is Percent Graph
				true, // visible at start
				Ver.ver(16),     // graph is valid from Server Version. 0 = All Versions; >0 = Valid from this version and above 
				-1);   // minimum height

		addTrendGraph(GRAPH_NAME_READ_TIME,
				"IO 'Read Time' in ms By 'backend_type:object:context' per Operation", 	                // Menu CheckBox text
				"IO 'Read Time' in ms By 'backend_type:object:context' per Operation ("+SHORT_NAME+")", // Graph Label 	
				TrendGraphDataPoint.createGraphProps(TrendGraphDataPoint.Y_AXIS_SCALE_LABELS_COUNT, CentralPersistReader.SampleType.MAX_OVER_SAMPLES),
				null, 
				LabelType.Dynamic, 
				TrendGraphDataPoint.Category.DISK,
				false, // is Percent Graph
				true, // visible at start
				Ver.ver(16),     // graph is valid from Server Version. 0 = All Versions; >0 = Valid from this version and above 	
				-1);   // minimum height


		//-----------------------------
		addTrendGraph(GRAPH_NAME_WRITES,
				"IO 'Writes' By 'backend_type:object:context' per Second", 	                // Menu CheckBox text
				"IO 'Writes' By 'backend_type:object:context' per Second ("+SHORT_NAME+")", // Graph Label 
				TrendGraphDataPoint.createGraphProps(TrendGraphDataPoint.Y_AXIS_SCALE_LABELS_COUNT, CentralPersistReader.SampleType.MAX_OVER_SAMPLES),
				null, 
				LabelType.Dynamic, 
				TrendGraphDataPoint.Category.DISK,
				false, // is Percent Graph
				true, // visible at start
				Ver.ver(16),     // graph is valid from Server Version. 0 = All Versions; >0 = Valid from this version and above 
				-1);   // minimum height

		addTrendGraph(GRAPH_NAME_WRITE_TIME,
				"IO 'Write Time' in ms By 'backend_type:object:context' per Operation", 	                // Menu CheckBox text
				"IO 'Write Time' in ms By 'backend_type:object:context' per Operation ("+SHORT_NAME+")", // Graph Label 	
				TrendGraphDataPoint.createGraphProps(TrendGraphDataPoint.Y_AXIS_SCALE_LABELS_COUNT, CentralPersistReader.SampleType.MAX_OVER_SAMPLES),
				null, 
				LabelType.Dynamic, 
				TrendGraphDataPoint.Category.DISK,
				false, // is Percent Graph
				true, // visible at start
				Ver.ver(16),     // graph is valid from Server Version. 0 = All Versions; >0 = Valid from this version and above 	
				-1);   // minimum height


		//-----------------------------
		addTrendGraph(GRAPH_NAME_WRITEBACKS,
				"IO 'Writebacks' By 'backend_type:object:context' per Second", 	                // Menu CheckBox text
				"IO 'Writebacks' By 'backend_type:object:context' per Second ("+SHORT_NAME+")", // Graph Label 
				TrendGraphDataPoint.createGraphProps(TrendGraphDataPoint.Y_AXIS_SCALE_LABELS_COUNT, CentralPersistReader.SampleType.MAX_OVER_SAMPLES),
				null, 
				LabelType.Dynamic, 
				TrendGraphDataPoint.Category.DISK,
				false, // is Percent Graph
				false, // visible at start
				Ver.ver(16),     // graph is valid from Server Version. 0 = All Versions; >0 = Valid from this version and above 
				-1);   // minimum height

		addTrendGraph(GRAPH_NAME_WRITEBACK_TIME,
				"IO 'Writeback Time' in ms By 'backend_type:object:context' per Operation", 	                // Menu CheckBox text
				"IO 'Writeback Time' in ms By 'backend_type:object:context' per Operation ("+SHORT_NAME+")", // Graph Label 	
				TrendGraphDataPoint.createGraphProps(TrendGraphDataPoint.Y_AXIS_SCALE_LABELS_COUNT, CentralPersistReader.SampleType.MAX_OVER_SAMPLES),
				null, 
				LabelType.Dynamic, 
				TrendGraphDataPoint.Category.DISK,
				false, // is Percent Graph
				true, // visible at start
				Ver.ver(16),     // graph is valid from Server Version. 0 = All Versions; >0 = Valid from this version and above 	
				-1);   // minimum height


		//-----------------------------
		addTrendGraph(GRAPH_NAME_EXTENDS,
				"IO 'Extends' By 'backend_type:object:context' per Second", 	                // Menu CheckBox text
				"IO 'Extends' By 'backend_type:object:context' per Second ("+SHORT_NAME+")", // Graph Label 
				TrendGraphDataPoint.createGraphProps(TrendGraphDataPoint.Y_AXIS_SCALE_LABELS_COUNT, CentralPersistReader.SampleType.MAX_OVER_SAMPLES),
				null, 
				LabelType.Dynamic, 
				TrendGraphDataPoint.Category.DISK,
				false, // is Percent Graph
				false, // visible at start
				Ver.ver(16),     // graph is valid from Server Version. 0 = All Versions; >0 = Valid from this version and above 
				-1);   // minimum height

		addTrendGraph(GRAPH_NAME_EXTEND_TIME,
				"IO 'Extend Time' in ms By 'backend_type:object:context' per Operation", 	                // Menu CheckBox text
				"IO 'Extend Time' in ms By 'backend_type:object:context' per Operation ("+SHORT_NAME+")", // Graph Label 	
				TrendGraphDataPoint.createGraphProps(TrendGraphDataPoint.Y_AXIS_SCALE_LABELS_COUNT, CentralPersistReader.SampleType.MAX_OVER_SAMPLES),
				null, 
				LabelType.Dynamic, 
				TrendGraphDataPoint.Category.DISK,
				false, // is Percent Graph
				true, // visible at start
				Ver.ver(16),     // graph is valid from Server Version. 0 = All Versions; >0 = Valid from this version and above 	
				-1);   // minimum height


		//-----------------------------
		addTrendGraph(GRAPH_NAME_FSYNCS,
				"IO 'FSyncs' By 'backend_type:object:context' per Second", 	                // Menu CheckBox text
				"IO 'FSyncs' By 'backend_type:object:context' per Second ("+SHORT_NAME+")", // Graph Label 
				TrendGraphDataPoint.createGraphProps(TrendGraphDataPoint.Y_AXIS_SCALE_LABELS_COUNT, CentralPersistReader.SampleType.MAX_OVER_SAMPLES),
				null, 
				LabelType.Dynamic, 
				TrendGraphDataPoint.Category.DISK,
				false, // is Percent Graph
				false, // visible at start
				Ver.ver(16),     // graph is valid from Server Version. 0 = All Versions; >0 = Valid from this version and above 
				-1);   // minimum height

		addTrendGraph(GRAPH_NAME_FSYNC_TIME,
				"IO 'FSync Time' in ms By 'backend_type:object:context' per Operation", 	                // Menu CheckBox text
				"IO 'FSync Time' in ms By 'backend_type:object:context' per Operation ("+SHORT_NAME+")", // Graph Label 	
				TrendGraphDataPoint.createGraphProps(TrendGraphDataPoint.Y_AXIS_SCALE_LABELS_COUNT, CentralPersistReader.SampleType.MAX_OVER_SAMPLES),
				null, 
				LabelType.Dynamic, 
				TrendGraphDataPoint.Category.DISK,
				false, // is Percent Graph
				true, // visible at start
				Ver.ver(16),     // graph is valid from Server Version. 0 = All Versions; >0 = Valid from this version and above 	
				-1);   // minimum height
	}

	@Override
	public void updateGraphData(TrendGraphDataPoint tgdp)
	{
		if (GRAPH_NAME_CACHE_HIT_NORMAL_PCT.equals(tgdp.getName())) 
		{ 
			Double[] data = new Double[1];
			
			String pk = createPkStr("client backend", "relation", "normal");
			
			Double val = this.getDiffValueAsDouble(pk, "cache_hit_pct");
			
			if (val != null)
			{
				data[0 ] = val;

				// Set the values
				tgdp.setDataPoint(this.getTimestamp(), data);
			}
			else
			{
				TrendGraph tg = getTrendGraph(tgdp.getName());
				if (tg != null)
					tg.setWarningLabel("Failed to get value(s) for pk-row: '" + pk + "'='" + val + "'.");
			}
		}

		//---------------------------------
		if (GRAPH_NAME_CACHE_HIT_ALL_PCT.equals(tgdp.getName())) { doGraphByPk(tgdp, "cache_hit_pct"        ); }

		//---------------------------------
		if (GRAPH_NAME_HITS             .equals(tgdp.getName())) { doGraphByPk(tgdp, "hits"                 ); }
		if (GRAPH_NAME_EVICTIONS        .equals(tgdp.getName())) { doGraphByPk(tgdp, "evictions"            ); }
		if (GRAPH_NAME_REUSES           .equals(tgdp.getName())) { doGraphByPk(tgdp, "reuses"               ); }

		//---------------------------------
		if (GRAPH_NAME_READS            .equals(tgdp.getName())) { doGraphByPk(tgdp, "reads"                ); }
		if (GRAPH_NAME_READ_TIME        .equals(tgdp.getName())) { doGraphByPk(tgdp, "read_time_avg_ms"     ); }

		if (GRAPH_NAME_WRITES           .equals(tgdp.getName())) { doGraphByPk(tgdp, "writes"               ); }
		if (GRAPH_NAME_WRITE_TIME       .equals(tgdp.getName())) { doGraphByPk(tgdp, "write_time_avg_ms"    ); }

		if (GRAPH_NAME_WRITEBACKS       .equals(tgdp.getName())) { doGraphByPk(tgdp, "writebacks"           ); }
		if (GRAPH_NAME_WRITEBACK_TIME   .equals(tgdp.getName())) { doGraphByPk(tgdp, "writeback_time_avg_ms"); }

		if (GRAPH_NAME_EXTENDS          .equals(tgdp.getName())) { doGraphByPk(tgdp, "extends"              ); }
		if (GRAPH_NAME_EXTEND_TIME      .equals(tgdp.getName())) { doGraphByPk(tgdp, "extend_time_avg_ms"   ); }

		if (GRAPH_NAME_FSYNCS           .equals(tgdp.getName())) { doGraphByPk(tgdp, "fsyncs"               ); }
		if (GRAPH_NAME_FSYNC_TIME       .equals(tgdp.getName())) { doGraphByPk(tgdp, "fsync_time_avg_ms"    ); }
	}

//	private void doGraphOneCol(TrendGraphDataPoint tgdp, String colName)
//	{
//		Double[] data = new Double[1];
//
//		int aggRowId = getAggregatedRowId();
//		
//		data[0] = this.getRateValueAsDouble(aggRowId, colName, true, 0d);
//
//		// Set the values
//		tgdp.setDataPoint(this.getTimestamp(), data);
//	}
	
	private void doGraphByPk(TrendGraphDataPoint tgdp, String colName)
	{
		int rc = this.size();
		int aggRowId = getAggregatedRowId();

		// What rows has values (above 0)
		List<Integer> validRowIds = new ArrayList<>(rc);
		for (int r = 0; r < rc; r++) // we still need to loop all rows...
		{
			if (r == aggRowId)
				continue;

			double val = this.getRateValueAsDouble(r, colName, true, 0d);
			if (val > 0)
				validRowIds.add(r);
		}

		// No rows: no nothing
//		if (validRowIds.isEmpty())
//			return;
		
		int arrSize = validRowIds.size() + 1; // add size for "Total" 
		String[] label = new String[arrSize];
		Double[] data  = new Double[arrSize];

		// First ADD "Total"
		label[0] = "_Total";
		data [0] = this.getRateValueAsDouble(aggRowId, colName, true, 0d);

		// Then ADD one row per "valid" rows
		int arrPos = 1;
		for (Integer validRowId : validRowIds)
		{
			String labelStr = this.getRatePkValue(validRowId);
			if (labelStr.endsWith("|"))
				labelStr = labelStr.substring(0, labelStr.length() - 1);
			
			labelStr = labelStr.replace('|', ':');
			
			label[arrPos] = labelStr;
			data [arrPos] = this.getRateValueAsDouble(validRowId, colName, true, 0d);
			
			arrPos++;
		}

		// Set the values
		tgdp.setDataPoint(this.getTimestamp(), label, data);
	}
	
}
