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
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * DbxTune is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with DbxTune.  If not, see <http://www.gnu.org/licenses/>.
 ******************************************************************************/
package com.dbxtune.cm.oracle;

import java.lang.invoke.MethodHandles;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
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
import com.dbxtune.cm.CmSettingsHelper;
import com.dbxtune.cm.CounterSetTemplates;
import com.dbxtune.cm.CounterSetTemplates.Type;
import com.dbxtune.cm.CountersModel;
import com.dbxtune.config.dict.MonTablesDictionary;
import com.dbxtune.config.dict.MonTablesDictionaryManager;
import com.dbxtune.graph.TrendGraphDataPoint;
import com.dbxtune.graph.TrendGraphDataPoint.LabelType;
import com.dbxtune.gui.MainFrame;
import com.dbxtune.sql.conn.DbxConnection;
import com.dbxtune.sql.conn.info.DbmsVersionInfo;
import com.dbxtune.sql.conn.info.DbmsVersionInfoOracle;
import com.dbxtune.utils.Configuration;
import com.dbxtune.utils.StringUtil;
import com.dbxtune.utils.Ver;

/**
 * @author Goran Schwarz (goran_schwarz@hotmail.com)
 */
public class CmSegmentStats
extends CountersModel
{
	private static final Logger _logger = LogManager.getLogger(MethodHandles.lookup().lookupClass());
	private static final long    serialVersionUID = 1L;

	public static final String   CM_NAME          = CmSegmentStats.class.getSimpleName();
	public static final String   SHORT_NAME       = "Segment Activity";
	public static final String   HTML_DESC        =
		"<html>" +
		"Which tables and indexes are used most? One row per segment (a table, an index, a partition or a LOB), with what has been done to it since the instance was started.<br>" +
		"<br>" +
		"By default: the 500 segments with most reads and writes (this can be changed in the options), plus the 100 segments that the sessions has waited most on.<br>" +
		"<br>" +
		"<b>Tip</b>: Look at the DIFF or RATE values, sorted on:" +
		"<ul>" +
		"  <li><code>LOGICAL_READS</code> - the segments that the statements reads most blocks from (memory or disk). This is where the CPU goes.</li>" +
		"  <li><code>PHYSICAL_READS</code> - the segments that are read from disk. Together with a high <code>SEGMENT_SCANS</code>: a table that is scanned and does not fit in the buffer cache.</li>" +
		"  <li><code>ROW_LOCK_WAITS</code>, <code>ITL_WAITS</code>, <code>BUFFER_BUSY_WAITS</code> - the segments that the sessions are fighting over.</li>" +
		"</ul>" +
		"A segment without a name (<code>OBJECT_NAME</code> is empty) has been dropped, or it is a temporary segment (not a table or an index), or it is in <code>PDB$SEED</code>.<br>" +
		"Tables that are in a <code>CLUSTER</code> shares one segment, so they are shown as one row with the name of the cluster (for example <code>SYS.C_OBJ#</code>).<br>" +
		"A Global Temporary Table has one row per session that uses it (every session has it's own segment).<br>" +
		"<b>Note</b>: In a Container Database, connect to CDB$ROOT to see all PDBs.<br>" +
		"</html>";

	public static final String   GROUP_NAME       = MainFrame.TCP_GROUP_OBJECT_ACCESS;
	public static final String   GUI_ICON_FILE    = "images/" + CM_NAME + ".png";

	public static final long     NEED_SRV_VERSION = 0;
	public static final long     NEED_CE_VERSION  = 0;

	public static final String[] MON_TABLES       = new String[] {CM_NAME, "V$SEGSTAT", "DBA_OBJECTS"};
	public static final String[] NEED_ROLES       = new String[] {};
	public static final String[] NEED_CONFIG      = new String[] {};

	public static final String[] PCT_COLUMNS      = new String[] {};
	public static final String[] DIFF_COLUMNS     = new String[] {
		"LOGICAL_READS",
		"PHYSICAL_READS",
		"PHYSICAL_READS_DIRECT",
		"PHYSICAL_READ_REQUESTS",
		"PHYSICAL_WRITES",
		"PHYSICAL_WRITES_DIRECT",
		"PHYSICAL_WRITE_REQUESTS",
		"DB_BLOCK_CHANGES",
		"SEGMENT_SCANS",
		"ROW_LOCK_WAITS",
		"ITL_WAITS",
		"BUFFER_BUSY_WAITS"
		};

	public static final boolean  NEGATIVE_DIFF_COUNTERS_TO_ZERO = true;
	public static final boolean  IS_SYSTEM_CM                   = true;
	public static final int      DEFAULT_POSTPONE_TIME          = 300; // (V$SEGSTAT has a row for every segment and statistic: 5 minutes, as the index CMs of SqlServerTune)
	public static final int      DEFAULT_QUERY_TIMEOUT          = 30;

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

		return new CmSegmentStats(counterController, guiController);
	}

	public CmSegmentStats(ICounterController counterController, IGuiController guiController)
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

		setCounterController(counterController);
		setGuiController(guiController);

		addTrendGraphs();

		CounterSetTemplates.register(this);
	}


	//------------------------------------------------------------
	// Implementation
	//------------------------------------------------------------
	public static final String  PROPKEY_sample_topRows = CM_NAME + ".sample.topRows";
	public static final int     DEFAULT_sample_topRows = 500;

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

		list.add(new CmSettingsHelper("Max Number of Rows", PROPKEY_sample_topRows, Integer.class, conf.getIntProperty(PROPKEY_sample_topRows, DEFAULT_sample_topRows), DEFAULT_sample_topRows, "Only get the ## segments that has most reads and writes: LOGICAL_READS + PHYSICAL_READS + PHYSICAL_WRITES since the instance was started (this to limit how much is fetched and stored on systems with many segments). On top of that, the 100 segments with most waits are always fetched: ROW_LOCK_WAITS + ITL_WAITS + BUFFER_BUSY_WAITS." ));

		return list;
	}

	public static final String GRAPH_NAME_LOGICAL_READS  = "SegLogicalReads";
	public static final String GRAPH_NAME_PHYSICAL_READS = "SegPhysicalReads";

	private void addTrendGraphs()
	{
		addTrendGraph(GRAPH_NAME_LOGICAL_READS,
			"Segments, Logical Reads per Second, Top 10", 	                // Menu CheckBox text
			"Logical Reads (blocks) per Second, for the Top 10 Segments (" + SHORT_NAME + ")", // Graph Label
			TrendGraphDataPoint.createGraphProps(TrendGraphDataPoint.Y_AXIS_SCALE_LABELS_PERSEC, CentralPersistReader.SampleType.MAX_OVER_SAMPLES),
			null,
			LabelType.Dynamic,
			TrendGraphDataPoint.Category.CACHE,
			false, // is Percent Graph
			false, // visible at start
			0,     // graph is valid from Server Version. 0 = All Versions; >0 = Valid from this version and above
			-1);   // minimum height

		addTrendGraph(GRAPH_NAME_PHYSICAL_READS,
			"Segments, Physical Reads per Second, Top 10", 	                // Menu CheckBox text
			"Physical Reads (blocks) per Second, for the Top 10 Segments (" + SHORT_NAME + ")", // Graph Label
			TrendGraphDataPoint.createGraphProps(TrendGraphDataPoint.Y_AXIS_SCALE_LABELS_PERSEC, CentralPersistReader.SampleType.MAX_OVER_SAMPLES),
			null,
			LabelType.Dynamic,
			TrendGraphDataPoint.Category.DISK,
			false, // is Percent Graph
			false, // visible at start
			0,     // graph is valid from Server Version. 0 = All Versions; >0 = Valid from this version and above
			-1);   // minimum height
	}

	@Override
	public List<String> getPkForVersion(DbxConnection conn, DbmsVersionInfo versionInfo)
	{
		List <String> pkCols = new LinkedList<String>();

		// This is what identifies a segment in V$SEGSTAT: TS# and DATAOBJ# (DATAOBJ# changes when a table is truncated or moved)
		// OBJ# can NOT be part of it: tables in a CLUSTER shares one segment, and V$SEGSTAT then has the OBJ# of one of the tables, not the same one every time
		// CON_ID is always 0 in a non-CDB (and in 11.2), in a CDB the same DATAOBJ# can exist in many containers
		pkCols.add("CON_ID");
		pkCols.add("TS_ID");
		pkCols.add("DATA_OBJECT_ID");

		return pkCols;
	}

	/** If we can't read the views: disable the CM, and tell what to do about it */
	@Override
	public boolean checkDependsOnOther(DbxConnection conn)
	{
		return OracleCmUtils.checkViewsAreReadable(this, conn, "V$SEGSTAT", "V$TABLESPACE", "DBA_OBJECTS");
	}

	@Override
	public String getSqlForVersion(DbxConnection conn, DbmsVersionInfo versionInfo)
	{
		DbmsVersionInfoOracle oraVersionInfo = (DbmsVersionInfoOracle) versionInfo;

		// 11.2 and above: a non-CDB, or when we are connected to a PDB
		String sql = getSql(false);

		// 12.1 and above, connected to CDB$ROOT: the segments in EVERY container
		if (versionInfo.getLongVersion() >= Ver.ver(12,1) && oraVersionInfo.isCdbRoot())
		{
			String sqlCdb = getSql(true);

			// The SQL for 12.1 and above can NOT be tested on all versions: so "dry-run" it, and if it fails use the 11.2 SQL (which works on all versions)
			// conn is null when we just want to "view" the SQL (the CM Properties dialog)
			if (conn == null)
			{
				sql = sqlCdb;
			}
			else
			{
				try (Statement stmnt = conn.createStatement(); ResultSet rs = stmnt.executeQuery("select * from (" + sqlCdb + ") where 1=2"))
				{
					sql = sqlCdb;
				}
				catch (SQLException ex)
				{
					_logger.warn(CM_NAME + ": The SQL for a CDB (version " + Ver.versionNumToStr(versionInfo.getLongVersion()) + ") failed, only the segments in CDB$ROOT will get a name. Caught: " + ex.getMessage().trim());
				}
			}
		}

		return sql;
	}

	/**
	 * @param cdb   false = DBA_OBJECTS (one container), true = CDB_OBJECTS (all containers, and CON_ID is used)
	 */
	private static String getSql(boolean cdb)
	{
		int topRows = Configuration.getCombinedConfiguration().getIntProperty(PROPKEY_sample_topRows, DEFAULT_sample_topRows);

		String views    = cdb ? "CDB" : "DBA";
		String conId    = cdb ? "CON_ID, "   : "";
		String sConId   = cdb ? "s.CON_ID, " : "";

		// The names of the columns, and the statistic in V$SEGSTAT it comes from
		// The statistic NUMBER is not the same in all versions, so the name is used
		String[][] stats = new String[][] {
			{"LOGICAL_READS",           "logical reads"},
			{"PHYSICAL_READS",          "physical reads"},
			{"PHYSICAL_READS_DIRECT",   "physical reads direct"},
			{"PHYSICAL_READ_REQUESTS",  "physical read requests"},
			{"PHYSICAL_WRITES",         "physical writes"},
			{"PHYSICAL_WRITES_DIRECT",  "physical writes direct"},
			{"PHYSICAL_WRITE_REQUESTS", "physical write requests"},
			{"DB_BLOCK_CHANGES",        "db block changes"},
			{"SEGMENT_SCANS",           "segment scans"},
			{"ROW_LOCK_WAITS",          "row lock waits"},
			{"ITL_WAITS",               "ITL waits"},
			{"BUFFER_BUSY_WAITS",       "buffer busy waits"},
		};

		String statSums  = ""; // one sum() for every statistic: V$SEGSTAT has one ROW per segment and statistic
		String statCols  = ""; // the columns in the outer select
		String statNames = ""; // 'name1', 'name2'...
		for (String[] stat : stats)
		{
			statSums  += "              ,sum(decode(s.STATISTIC_NAME, '" + stat[1] + "', s.VALUE, 0)) as " + stat[0] + " \n";
			statCols  += "    ,x." + stat[0] + " \n";
			statNames += (statNames.isEmpty() ? "" : ", ") + "'" + stat[1] + "'";
		}

		// o: the names. DBA_OBJECTS is read ONCE (materialize) and then joined
		//    Without that it is up to Oracle if it goes to it once for every row: in CDB$ROOT that was 20 seconds instead of 0.2 (18c, 21c and 23)
		// x: one row per segment (TS# + DATAOBJ#): the ## segments with most "reads + writes"
		//    plus the 100 segments that the sessions has waited most on (a small table that everybody updates has few reads, but it is the one we want to see)
		//    (V$SEGMENT_STATISTICS has the names, but it takes seconds instead of milliseconds)
		// k: is the segment a CLUSTER? Then the name of the cluster is used, since OBJ# is just one of the tables in it (and not the same one every time)
		return ""
			+ "with o as (select /*+ materialize */ " + conId + "OBJECT_ID, DATA_OBJECT_ID, OWNER, OBJECT_NAME, SUBOBJECT_NAME, OBJECT_TYPE \n"
			+ "           from " + views + "_OBJECTS \n"
			+ "           where OBJECT_TYPE in ('TABLE', 'TABLE PARTITION', 'TABLE SUBPARTITION', 'INDEX', 'INDEX PARTITION', 'INDEX SUBPARTITION', 'LOB', 'LOB PARTITION', 'LOB SUBPARTITION', 'CLUSTER')) \n"
			+ "select \n"
			+ "     " + (cdb ? "x.CON_ID" : "CAST(0    as NUMBER(10))")    + " as CON_ID \n"
			+ "    ," + (cdb ? "c.NAME"   : "CAST(NULL as VARCHAR2(128))") + " as CON_NAME \n"
			+ "    ,NVL(k.OWNER,       o.OWNER)       as OWNER \n"
			+ "    ,NVL(k.OBJECT_NAME, o.OBJECT_NAME) as OBJECT_NAME \n"
			+ "    ,NVL2(k.OBJECT_NAME, NULL, o.SUBOBJECT_NAME) as SUBOBJECT_NAME \n"
			+ "    ,NVL(k.OBJECT_TYPE, o.OBJECT_TYPE) as OBJECT_TYPE \n"
			+ "    ,t.NAME as TABLESPACE_NAME \n"
			+ statCols
			+ "    ,x.TS_ID \n"
			+ "    ,x.OBJECT_ID \n"
			+ "    ,x.DATA_OBJECT_ID \n"
			+ "from ( \n"
			+ "    select * from ( \n"
			+ "        select " + sConId + "s.TS# as TS_ID, max(s.OBJ#) as OBJECT_ID, s.DATAOBJ# as DATA_OBJECT_ID \n"
			+ statSums
			+ "              ,row_number() over (order by sum(decode(s.STATISTIC_NAME, 'logical reads',  s.VALUE, 'physical reads', s.VALUE, 'physical writes',   s.VALUE, 0)) desc) as RN_ACTIVITY \n"
			+ "              ,row_number() over (order by sum(decode(s.STATISTIC_NAME, 'row lock waits', s.VALUE, 'ITL waits',      s.VALUE, 'buffer busy waits', s.VALUE, 0)) desc) as RN_WAITS \n"
			+ "        from V$SEGSTAT s \n"
			+ "        where s.STATISTIC_NAME in (" + statNames + ") \n"
			+ "        group by " + sConId + "s.TS#, s.DATAOBJ# \n"
			+ "    ) \n"
			+ "    where RN_ACTIVITY <= " + topRows + " \n"
			+ "       or (RN_WAITS <= 100 and ROW_LOCK_WAITS + ITL_WAITS + BUFFER_BUSY_WAITS > 0) \n"
			+ ") x \n"
			+ (cdb ? "left join V$CONTAINERS c on c.CON_ID = x.CON_ID \n" : "")
			+ "left join o              on o.OBJECT_ID      = x.OBJECT_ID"                                      + (cdb ? " and o.CON_ID = x.CON_ID" : "") + " \n"
			+ "left join o k            on k.DATA_OBJECT_ID = x.DATA_OBJECT_ID and k.OBJECT_TYPE = 'CLUSTER'"   + (cdb ? " and k.CON_ID = x.CON_ID" : "") + " \n"
			+ "left join V$TABLESPACE t on t.TS#            = x.TS_ID"                                          + (cdb ? " and t.CON_ID = x.CON_ID" : "") + " \n"
			+ "order by x.LOGICAL_READS desc \n"
			+ "";
	}

	@Override
	public void addMonTableDictForVersion(DbxConnection conn, DbmsVersionInfo versionInfo)
	{
		try
		{
			MonTablesDictionary mtd = MonTablesDictionaryManager.getInstance();

			mtd.addTable(CM_NAME, "Which tables and indexes are used most? One row per segment, with what has been done to it since the instance was started.");

			mtd.addColumn(CM_NAME, "CON_ID",                  "<html>Container ID. 0 = This is not a Container Database (or the version is below 12.1), or we are connected to a PDB.</html>");
			mtd.addColumn(CM_NAME, "CON_NAME",                "<html>Name of the Container (PDB) the segment is in. NULL if CON_ID is 0.</html>");
			mtd.addColumn(CM_NAME, "OWNER",                   "<html>The schema that owns the object. NULL if the object has been dropped, or if it is a temporary segment.</html>");
			mtd.addColumn(CM_NAME, "OBJECT_NAME",             "<html>Name of the table, index, LOB or cluster. NULL if the object has been dropped, or if it is a temporary segment.</html>");
			mtd.addColumn(CM_NAME, "SUBOBJECT_NAME",          "<html>Name of the partition (or subpartition), NULL if this is not a partition.</html>");
			mtd.addColumn(CM_NAME, "OBJECT_TYPE",             "<html>TABLE, INDEX, TABLE PARTITION, INDEX PARTITION, LOB, CLUSTER...<br>CLUSTER = a segment that is shared by more than one table (for example SYS.C_OBJ#, which holds TAB$, IND$, COL$...). The values are for all the tables in it.</html>");
			mtd.addColumn(CM_NAME, "TABLESPACE_NAME",         "<html>The tablespace that the segment is in.</html>");
			mtd.addColumn(CM_NAME, "LOGICAL_READS",           "<html>Number of blocks read from the segment: from memory (the buffer cache) or from disk.<br>This is what costs CPU.</html>");
			mtd.addColumn(CM_NAME, "PHYSICAL_READS",          "<html>Number of blocks read from disk.</html>");
			mtd.addColumn(CM_NAME, "PHYSICAL_READS_DIRECT",   "<html>Number of blocks read from disk that did NOT go through the buffer cache (direct path reads: big scans, parallel query).<br>They are also included in PHYSICAL_READS.</html>");
			mtd.addColumn(CM_NAME, "PHYSICAL_READ_REQUESTS",  "<html>Number of read requests to the disk. One request can read many blocks.<br>PHYSICAL_READS / PHYSICAL_READ_REQUESTS is about 1 for index lookups, and much higher for scans.</html>");
			mtd.addColumn(CM_NAME, "PHYSICAL_WRITES",         "<html>Number of blocks written to disk.</html>");
			mtd.addColumn(CM_NAME, "PHYSICAL_WRITES_DIRECT",  "<html>Number of blocks written to disk that did NOT go through the buffer cache (direct path loads).<br>They are also included in PHYSICAL_WRITES.</html>");
			mtd.addColumn(CM_NAME, "PHYSICAL_WRITE_REQUESTS", "<html>Number of write requests to the disk. One request can write many blocks.</html>");
			mtd.addColumn(CM_NAME, "DB_BLOCK_CHANGES",        "<html>Number of changes made to blocks of the segment (INSERT, UPDATE, DELETE). This is what makes redo.</html>");
			mtd.addColumn(CM_NAME, "SEGMENT_SCANS",           "<html>Number of times the whole segment has been scanned (full table scan, or index fast full scan).</html>");
			mtd.addColumn(CM_NAME, "ROW_LOCK_WAITS",          "<html>Number of times a session had to wait for a row that was locked by another session.</html>");
			mtd.addColumn(CM_NAME, "ITL_WAITS",               "<html>Number of times a session had to wait for a free slot in the Interested Transaction List of a block.<br>That is: too many transactions want to change the same block at the same time. Fix: a higher INITRANS for the segment.</html>");
			mtd.addColumn(CM_NAME, "BUFFER_BUSY_WAITS",       "<html>Number of times a session had to wait for a block that another session was reading into memory or changing.<br>Typical for many concurrent inserts into the same block (the right hand side of an index on a sequence number).</html>");
			mtd.addColumn(CM_NAME, "TS_ID",                   "<html>Tablespace number (TS# in V$SEGSTAT)</html>");
			mtd.addColumn(CM_NAME, "OBJECT_ID",               "<html>Dictionary object number (OBJ# in V$SEGSTAT, OBJECT_ID in DBA_OBJECTS).<br>For a CLUSTER this is one of the tables in it, and not the same one every time.</html>");
			mtd.addColumn(CM_NAME, "DATA_OBJECT_ID",          "<html>Number of the segment (DATAOBJ# in V$SEGSTAT). It changes when a table is truncated or moved, or when an index is rebuilt: then the segment is seen as new.</html>");
		}
		catch (NameNotFoundException e)
		{
			_logger.warn("Problems in cm='" + CM_NAME + "', adding addMonTableDictForVersion. Caught: " + e);
		}
	}

	@Override
	public void updateGraphData(TrendGraphDataPoint tgdp)
	{
		// The two graphs are the same: one "line" for each of the 10 segments with the highest value in this sample
		String colName = null;
		if (GRAPH_NAME_LOGICAL_READS .equals(tgdp.getName())) colName = "LOGICAL_READS";
		if (GRAPH_NAME_PHYSICAL_READS.equals(tgdp.getName())) colName = "PHYSICAL_READS";

		if (colName != null)
		{
			// Value per object name. Normally one row per name, but a Global Temporary Table has one row per session, and a truncated table can have both it's old and new segment
			// A segment that we see for the first time is skipped: it's "delta" is everything since the instance was started
			// A segment without a name is skipped: it is dropped, or it is a temporary segment (they are still in the table)
			Map<String, Double> perName = new HashMap<>();
			for (int r = 0; r < this.size(); r++)
			{
				Double perSec = this.getRateValueAsDouble(r, colName);
				if (perSec == null || perSec <= 0 || this.isNewDeltaOrRateRow(r))
					continue;

				// [container:]owner.object[:partition]
				String conName    = this.getAbsString(r, "CON_NAME");
				String owner      = this.getAbsString(r, "OWNER");
				String objectName = this.getAbsString(r, "OBJECT_NAME");
				String subName    = this.getAbsString(r, "SUBOBJECT_NAME");

				if (StringUtil.isNullOrBlank(objectName))
					continue;

				String label = owner + "." + objectName;
				if (StringUtil.hasValue(subName)) label = label + ":" + subName;
				if (StringUtil.hasValue(conName)) label = conName + ":" + label;

				perName.put(label, perSec + perName.getOrDefault(label, 0.0));
			}

			// Sort: highest first, and take the first 10
			List<String> names = new ArrayList<>(perName.keySet());
			names.sort((n1, n2) -> perName.get(n2).compareTo(perName.get(n1)));

			LinkedHashMap<String, Double> dataMap = new LinkedHashMap<>();
			for (int i = 0; i < Math.min(10, names.size()); i++)
				dataMap.put(names.get(i), perName.get(names.get(i)));

			if (dataMap.size() > 0)
			{
				tgdp.setData(this.getTimestamp(), dataMap);
			}
			else if (tgdp.hasData())
			{
				// Nothing to show in this sample: set the lines we already have to 0 (if we do nothing, the values of the previous sample are kept and stored again)
				Double[] zeros = new Double[tgdp.getLabel().length];
				Arrays.fill(zeros, 0.0);
				tgdp.setDataPoint(this.getTimestamp(), tgdp.getLabel(), zeros);
			}
		}
	}
}
