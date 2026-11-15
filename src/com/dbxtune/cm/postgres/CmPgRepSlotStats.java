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
package com.dbxtune.cm.postgres;

import java.lang.invoke.MethodHandles;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import javax.naming.NameNotFoundException;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.dbxtune.ICounterController;
import com.dbxtune.IGuiController;
import com.dbxtune.central.pcs.CentralPersistReader;
import com.dbxtune.cm.CounterSetTemplates;
import com.dbxtune.cm.CounterSetTemplates.Type;
import com.dbxtune.cm.CountersModel;
import com.dbxtune.config.dict.MonTablesDictionary;
import com.dbxtune.config.dict.MonTablesDictionaryManager;
import com.dbxtune.graph.TrendGraphDataPoint;
import com.dbxtune.graph.TrendGraphDataPoint.LabelType;
import com.dbxtune.gui.MainFramePostgres;
import com.dbxtune.sql.conn.DbxConnection;
import com.dbxtune.sql.conn.info.DbmsVersionInfo;
import com.dbxtune.utils.Ver;

/**
 * Logical decoding statistics per (logical) replication slot: how much is decoded, and how much had to be spilled to disk or streamed
 * because a transaction was bigger than logical_decoding_work_mem.
 */
public class CmPgRepSlotStats
extends CountersModel
{
	private static final Logger _logger = LogManager.getLogger(MethodHandles.lookup().lookupClass());
	private static final long    serialVersionUID = 1L;

	public static final String   CM_NAME          = CmPgRepSlotStats.class.getSimpleName();
	public static final String   SHORT_NAME       = "Rep Slot Stats";
	public static final String   HTML_DESC        =
		"<html>" +
		"One row per <b>logical</b> replication slot: how much WAL that was decoded, and how much that was spilled to disk or streamed<br>" +
		"because a transaction did not fit in <code>logical_decoding_work_mem</code>.<br>" +
		"A lot of spill means: increase <code>logical_decoding_work_mem</code>, or use <code>streaming = on</code> in the subscription.<br>" +
		"<i>Empty when there are no logical replication slots (physical slots are not in this view).</i>" +
		"</html>";

	public static final String   GROUP_NAME       = MainFramePostgres.TCP_GROUP_REPLICATION;
	public static final String   GUI_ICON_FILE    = "images/" + CM_NAME + ".png";

	public static final long     NEED_SRV_VERSION = Ver.ver(14);
	public static final long     NEED_CE_VERSION  = 0;

	public static final String[] MON_TABLES       = new String[] {"pg_stat_replication_slots"};
	public static final String[] NEED_ROLES       = new String[] {};
	public static final String[] NEED_CONFIG      = new String[] {};

	public static final String[] PCT_COLUMNS      = new String[] {};
	public static final String[] DIFF_COLUMNS     = new String[] {
			"spill_mb",
			"stream_mb",
			"total_mb",
			"spill_txns",
			"spill_count",
			"spill_bytes",
			"stream_txns",
			"stream_count",
			"stream_bytes",
			"mem_exceeded_count",  // 19 and above
			"total_txns",
			"total_bytes",
			"slotsync_skip_count"  // 19 and above
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
			guiController.splashWindowProgress("Loading: Counter Model '" + CM_NAME + "'");

		return new CmPgRepSlotStats(counterController, guiController);
	}

	public CmPgRepSlotStats(ICounterController counterController, IGuiController guiController)
	{
		super(counterController, guiController,
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


	//------------------------------------------------------------
	// Implementation
	//------------------------------------------------------------
	public static final String GRAPH_NAME_SPILL_MB   = "SpillMb";
	public static final String GRAPH_NAME_DECODED_MB = "DecodedMb";

	private void addTrendGraphs()
	{
		addTrendGraph(GRAPH_NAME_SPILL_MB,
				"Logical Decoding Spilled to Disk, MB per Second, per Slot",                    // Menu CheckBox text
				"Logical Decoding Spilled to Disk, MB per Second, per Slot (" + SHORT_NAME + ")", // Graph Label
				TrendGraphDataPoint.createGraphProps(TrendGraphDataPoint.Y_AXIS_SCALE_LABELS_MB, CentralPersistReader.SampleType.MAX_OVER_SAMPLES),
				null, LabelType.Dynamic,
				TrendGraphDataPoint.Category.REPLICATION,
				false, // is Percent Graph
				false, // visible at start
				Ver.ver(14), // graph is valid from Server Version. 0 = All Versions; >0 = Valid from this version and above
				-1);   // minimum height

		addTrendGraph(GRAPH_NAME_DECODED_MB,
				"Logical Decoding Sent to the Output Plugin, MB per Second, per Slot",                    // Menu CheckBox text
				"Logical Decoding Sent to the Output Plugin, MB per Second, per Slot (" + SHORT_NAME + ")", // Graph Label
				TrendGraphDataPoint.createGraphProps(TrendGraphDataPoint.Y_AXIS_SCALE_LABELS_MB, CentralPersistReader.SampleType.MAX_OVER_SAMPLES),
				null, LabelType.Dynamic,
				TrendGraphDataPoint.Category.REPLICATION,
				false, // is Percent Graph
				false, // visible at start
				Ver.ver(14), // graph is valid from Server Version. 0 = All Versions; >0 = Valid from this version and above
				-1);   // minimum height
	}

	@Override
	public void updateGraphData(TrendGraphDataPoint tgdp)
	{
		String colName = null;
		if (GRAPH_NAME_SPILL_MB  .equals(tgdp.getName())) colName = "spill_mb";
		if (GRAPH_NAME_DECODED_MB.equals(tgdp.getName())) colName = "total_mb";
		if (colName == null)
			return;

		// One line per slot
		Map<String, Double> map = new LinkedHashMap<>();
		for (int r = 0; r < this.getRateRowCount(); r++)
		{
			String slotName = this.getRateString(r, "slot_name");
			if (slotName != null)
				map.put(slotName, this.getRateValueAsDouble(r, colName, 0d));
		}
		if ( ! map.isEmpty() )
			tgdp.setData(this.getTimestamp(), map);
	}

	@Override
	public List<String> getPkForVersion(DbxConnection conn, DbmsVersionInfo versionInfo)
	{
		List <String> pkCols = new LinkedList<String>();
		pkCols.add("slot_name");
		return pkCols;
	}

	@Override
	public String getSqlForVersion(DbxConnection conn, DbmsVersionInfo versionInfo)
	{
		// 19 added: mem_exceeded_count, slotsync_skip_count, slotsync_last_skip (picked up by 's.*')
		return ""
			    + "SELECT \n"
			    + "     CAST(s.spill_bytes  / 1024.0 / 1024.0 AS numeric(15,1)) AS spill_mb \n" // slot_name comes with 's.*'
			    + "    ,CAST(s.stream_bytes / 1024.0 / 1024.0 AS numeric(15,1)) AS stream_mb \n"
			    + "    ,CAST(s.total_bytes  / 1024.0 / 1024.0 AS numeric(15,1)) AS total_mb \n"
			    + "    ,r.slot_type \n"
			    + "    ,r.database AS dbname \n"
			    + "    ,r.active \n"
			    + "    ,s.* \n"
			    + "FROM pg_stat_replication_slots s \n"
			    + "LEFT JOIN pg_replication_slots r ON r.slot_name = s.slot_name \n"
			    + "";
	}

	@Override
	public void addMonTableDictForVersion(DbxConnection conn, DbmsVersionInfo versionInfo)
	{
		try
		{
			String tabname = "pg_stat_replication_slots";
			MonTablesDictionary mtd = MonTablesDictionaryManager.getInstance();

			mtd.addTable(tabname, "One row per logical replication slot, with statistics about its usage.");

			mtd.addColumn(tabname, "spill_mb",            "<html>MB spilled to disk (spill_bytes / 1024 / 1024).</html>");
			mtd.addColumn(tabname, "stream_mb",           "<html>MB streamed to the subscriber before the transaction ended (stream_bytes / 1024 / 1024).</html>");
			mtd.addColumn(tabname, "total_mb",            "<html>MB of decoded transaction data sent to the output plugin (total_bytes / 1024 / 1024).</html>");
			mtd.addColumn(tabname, "slot_type",           "<html>From pg_replication_slots: physical or logical (only logical slots are in this view).</html>");
			mtd.addColumn(tabname, "dbname",              "<html>From pg_replication_slots.database: the database the logical slot decodes.</html>");
			mtd.addColumn(tabname, "active",              "<html>From pg_replication_slots: the slot is used right now.</html>");
			mtd.addColumn(tabname, "slot_name",           "<html>A unique, cluster-wide identifier for the replication slot.</html>");
			mtd.addColumn(tabname, "spill_txns",          "<html>Number of transactions spilled to disk once the memory used by logical decoding exceeds <code>logical_decoding_work_mem</code>.</html>");
			mtd.addColumn(tabname, "spill_count",         "<html>Number of times transactions were spilled to disk (a transaction may be spilled more than once).</html>");
			mtd.addColumn(tabname, "spill_bytes",         "<html>Amount of decoded transaction data spilled to disk.</html>");
			mtd.addColumn(tabname, "stream_txns",         "<html>Number of in-progress transactions streamed to the output plugin (the subscription has <code>streaming = on</code>).</html>");
			mtd.addColumn(tabname, "stream_count",        "<html>Number of times in-progress transactions were streamed.</html>");
			mtd.addColumn(tabname, "stream_bytes",        "<html>Amount of transaction data decoded for streaming in-progress transactions.</html>");
			mtd.addColumn(tabname, "mem_exceeded_count",  "<html>Number of times the memory used by logical decoding exceeded <code>logical_decoding_work_mem</code> (Postgres 19 and above).</html>");
			mtd.addColumn(tabname, "total_txns",          "<html>Number of decoded transactions sent to the output plugin.</html>");
			mtd.addColumn(tabname, "total_bytes",         "<html>Amount of transaction data decoded and sent to the output plugin.</html>");
			mtd.addColumn(tabname, "slotsync_skip_count", "<html>Number of times the slot synchronization (to a standby, failover slots) was skipped (Postgres 19 and above).</html>");
			mtd.addColumn(tabname, "slotsync_last_skip",  "<html>When the slot synchronization was last skipped (Postgres 19 and above).</html>");
			mtd.addColumn(tabname, "stats_reset",         "<html>Time at which these statistics were last reset.</html>");
		}
		catch (NameNotFoundException e)
		{
			_logger.warn("Problems in cm='" + CM_NAME + "', adding addMonTableDictForVersion. Caught: " + e);
		}
	}
}
