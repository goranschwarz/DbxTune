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
package com.dbxtune.pcs.report.content.oracle;

import java.io.Writer;
import java.lang.invoke.MethodHandles;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.text.NumberFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

import org.apache.commons.text.StringEscapeUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.dbxtune.cm.CountersModel;
import com.dbxtune.gui.ResultSetTableModel;
import com.dbxtune.pcs.PersistentCounterHandler;
import com.dbxtune.pcs.report.DailySummaryReportAbstract;
import com.dbxtune.pcs.report.content.IReportEntry;
import com.dbxtune.pcs.report.content.ReportEntryAbstract;
import com.dbxtune.sql.conn.DbxConnection;
import com.dbxtune.utils.Configuration;
import com.dbxtune.utils.DbUtils;
import com.dbxtune.utils.HtmlTableProducer;
import com.dbxtune.utils.StringUtil;

/**
 * What the Oracle report sections have in common: the information about tables and execution plans that the collector has put
 * in the "DDL Storage" (table <code>MonDdlStorage</code>, written by <code>ObjectLookupInspectorOracle</code>).<br>
 * To that is added how the table and its indexes were USED during the recording (from <code>CmSegmentStats</code> and <code>CmIndexUsage</code>),
 * as the other DBMS vendors has it: "diff &lt;- abs".
 * <p>
 * It is also used without a report: by <code>TableInfoServlet</code> ("Table Information" in the execution plan dialog of DbxCentral)
 * and by <code>LlmSqlContextBuilder</code> (what the LLM is told about the tables), via the static methods.
 */
public abstract class OracleAbstract
extends ReportEntryAbstract
{
	private static final Logger _logger = LogManager.getLogger(MethodHandles.lookup().lookupClass());

	public OracleAbstract(DailySummaryReportAbstract reportingInstance)
	{
		super(reportingInstance);
	}

	//--------------------------------------------------------------------------------------------
	// What is in the DDL Storage about a table (or view / synonym), and about one of its indexes
	//--------------------------------------------------------------------------------------------
	/**
	 * What has been done to a segment (a table or an index, all its partitions together), from <code>CmSegmentStats</code> (V$SEGSTAT).<br>
	 * -1 = not recorded: the collector only records the most active segments, and the CM can be turned off.
	 */
	public static class OraSegmentActivity
	{
		public long logicalReads    = -1;
		public long physicalReads   = -1;
		public long physicalWrites  = -1;
		public long blockChanges    = -1;
		public long segmentScans    = -1;
		public long rowLockWaits    = -1;
		public long itlWaits        = -1;
		public long bufferBusyWaits = -1;

		public boolean isRecorded() { return logicalReads >= 0; }
	}

	public static class OraIndexInfo
	{
		public String owner;
		public String name;
		public String type;             // NORMAL, BITMAP, FUNCTION-BASED NORMAL, LOB...
		public String uniqueness;       // UNIQUE, NONUNIQUE
		public String constraintType;   // P = it is the index of the primary key, U = of a unique constraint
		public String keys;             // the columns: "C1, C2 DESC". For a function-based index the expression: UPPER("NAME")
		public long   numRows          = -1;
		public long   distinctKeys     = -1;
		public long   blevel           = -1;
		public long   leafBlocks       = -1;
		public long   clusteringFactor = -1;
		public long   sizeKb           = -1;
		public String status;           // VALID, UNUSABLE, N/A (a partitioned index: the status is on the partitions)
		public String visibility;       // VISIBLE, INVISIBLE
		public String partitioned;
		public String lastAnalyzed;
		public String tablespace;

		// How the index is used (DBA_INDEX_USAGE, 12.2 and above). 'abs' is since Oracle started to track it, 'diff' is during the recording (from CmIndexUsage)
		public boolean usageTracked     = false; // false = the version is below 12.2 (or the view could not be read): we do not know
		public long    absAccessCount   = -1;    // usageTracked and -1: Oracle has no row for the index = it has never been used
		public long    absRowsReturned  = -1;
		public long    diffAccessCount  = -1;
		public long    diffRowsReturned = -1;
		public String  lastUsed;

		// What was done to the index during the recording (diff), and since the instance was started (abs)
		public OraSegmentActivity diff = new OraSegmentActivity();
		public OraSegmentActivity abs  = new OraSegmentActivity();

		public String getDescription()
		{
			return ("P".equals(constraintType) ? "PRIMARY KEY" : "UNIQUE".equals(uniqueness) ? "UNIQUE" : "NONUNIQUE")
					+ ("NORMAL".equals(type)        ? "" : ", " + type)
					+ ("YES".equals(partitioned)    ? ", PARTITIONED" : "")
					+ ("INVISIBLE".equals(visibility) ? ", INVISIBLE" : "");
		}
	}

	public static class OraTableInfo
	{
		public String    dbname;         // the container (PDB), or the database when it is not a Container Database
		public String    owner;
		public String    name;
		public String    type;           // TABLE, VIEW, MATERIALIZED VIEW, SYNONYM
		public Timestamp sampleTime;
		public String    objectText;     // TABLE: the columns as a text table. VIEW: the text of the view. SYNONYM: what it is a synonym for
		public String    extraInfoText;
		public String    dependsText;    // TABLE: the source of its triggers (null when it has none)

		public long      numRows        = -1;
		public long      blocks         = -1;
		public long      avgRowLen      = -1;
		public String    lastAnalyzed;
		public String    staleStats;     // YES, NO or null (no statistics)
		public String    partitioned;
		public long      partitionCount = -1;
		public String    partitionKeys;
		public String    temporary;
		public String    iotType;
		public String    compression;
		public String    degree;
		public String    tablespace;
		public long      tableKb        = -1;
		public long      indexKb        = -1;
		public long      lobKb          = -1;

		// Rows changed since the optimizer statistics were gathered (DBA_TAB_MODIFICATIONS). -1 = Oracle has nothing noted (no changes, or not written down yet)
		public long      modInserts     = -1;
		public long      modUpdates     = -1;
		public long      modDeletes     = -1;

		public boolean   triggersLookedUp    = false; // false = the recording was made by a version that did not look for triggers: we do not know
		public int       triggerCount        = 0;
		public int       triggerEnabledCount = 0;
		public String    triggersText;   // the triggers as a text table, and their source. null = the table has no triggers

		// What was done to the table during the recording (diff), and since the instance was started (abs)
		public OraSegmentActivity diff = new OraSegmentActivity();
		public OraSegmentActivity abs  = new OraSegmentActivity();

		public List<OraIndexInfo> indexes    = new ArrayList<>();
		public List<String>       references = new ArrayList<>(); // VIEW / SYNONYM: what it reads from, as OWNER.NAME

		public boolean isTable()        { return "TABLE".equals(type); }
		public String  getFullName()    { return owner + "." + name; }
		public long    getTotalKb()     { return Math.max(0, tableKb) + Math.max(0, indexKb) + Math.max(0, lobKb); }

		@Override public int     hashCode()         { return Objects.hash(dbname, owner, name); }
		@Override public boolean equals(Object obj) { return obj instanceof OraTableInfo && Objects.equals(dbname, ((OraTableInfo) obj).dbname) && Objects.equals(owner, ((OraTableInfo) obj).owner) && Objects.equals(name, ((OraTableInfo) obj).name); }
	}

	//--------------------------------------------------------------------------------------------
	// For the report sections
	//--------------------------------------------------------------------------------------------
	@Override
	public String getDbmsTableInfoAsHtmlTable(DbxConnection conn, String currentDbname, Set<String> tableList, boolean includeIndexInfo, String classname)
	{
		Set<OraTableInfo> tableInfoSet = getTableInformationFromMonDdlStorage(conn, currentDbname, tableList);
		if (tableInfoSet.isEmpty())
			return "";

		return getTableInfoAsHtmlTable(tableInfoSet, tableList, includeIndexInfo, classname);
	}

	//--------------------------------------------------------------------------------------------
	// Without a report (a servlet): the same lookup and rendering, as SqlServerAbstract / AseAbstract / PostgresAbstract has
	//--------------------------------------------------------------------------------------------

	/** An instance just to reach the methods (the lookup and rendering does not use the reporting instance, so null is fine) */
	private static OracleAbstract createInstance()
	{
		return new OracleAbstract(null)
		{
			@Override public boolean hasIssueToReport()       { return false; }
			@Override public String  getSubject()             { return ""; }
			@Override public boolean hasMinimalMessageText()  { return false; }
			@Override public boolean hasShortMessageText()    { return false; }
			@Override public void    writeMessageText(Writer w, IReportEntry.MessageType t) {}
			@Override public void    create(DbxConnection c, String s, Configuration p, Configuration l) {}
		};
	}

	/** Table / index information as HTML. A name in 'tableList' can be a table, a view, a synonym or an INDEX (which gives its table) */
	public static String getTableInfoHtml(DbxConnection conn, String dbname, Set<String> tableList, boolean includeIndexInfo, String classname)
	{
		return createInstance().getDbmsTableInfoAsHtmlTable(conn, dbname, tableList, includeIndexInfo, classname);
	}

	/** Table / index information as plain text: what the LLM is told about the tables */
	public static String getTableInfoPlainText(DbxConnection conn, String dbname, Set<String> tableList)
	{
		OracleAbstract inst = createInstance();
		return inst.getTableInfoAsPlainText( inst.getTableInformationFromMonDdlStorage(conn, dbname, tableList) );
	}

	/**
	 * The execution plans that are stored for a statement: one per PLAN_HASH_VALUE that was seen during the recording.
	 *
	 * @return key: PLAN_HASH_VALUE, value: {0: the plan as text (DBMS_XPLAN), 1: the tables of the plan as "OWNER.T1, OWNER.T2", 2: the container the statement ran in,
	 *         3: the schema it is parsed in, 4: the module (the application), 5: the PL/SQL unit the statement is written in}. 3 - 5 can be null.
	 */
	public static Map<String, String[]> getExecutionPlans(DbxConnection conn, String sqlId)
	{
		Map<String, String[]> plans = new LinkedHashMap<>();

		if (StringUtil.isNullOrBlank(sqlId) || ! DbUtils.checkIfTableExistsNoThrow(conn, null, null, "MonDdlStorage"))
			return plans;

		String sql = conn.quotifySqlString(""
			+ "select [objectName], [extraInfoText], [objectText], [owner] \n"
			+ "from [MonDdlStorage] \n"
			+ "where [dbname] = " + DbUtils.safeStr(PersistentCounterHandler.STATEMENT_CACHE_NAME) + " \n"
			+ "  and ([objectName] = " + DbUtils.safeStr(sqlId) + " or [objectName] like " + DbUtils.safeStr(sqlId + "\\_%") + " escape '\\') \n"
			+ "order by [sampleTime]");

		try (Statement stmnt = conn.createStatement(); ResultSet rs = stmnt.executeQuery(sql))
		{
			while (rs.next())
			{
				String objectName = rs.getString(1);

				// 'objectText' is one "Name: value" per line (see ObjectLookupInspectorOracle)
				String tableList = "", parsingSchema = null, module = null, program = null;
				for (String line : StringUtil.nullToValue(rs.getString(3), "").split("\n"))
				{
					if (line.startsWith("TableList:"))     tableList     = line.substring("TableList:"    .length()).trim();
					if (line.startsWith("ParsingSchema:")) parsingSchema = line.substring("ParsingSchema:".length()).trim();
					if (line.startsWith("Module:"))        module        = line.substring("Module:"       .length()).trim();
					if (line.startsWith("Program:"))       program       = line.substring("Program:"      .length()).trim();
				}

				plans.put(objectName.contains("_") ? objectName.substring(objectName.indexOf('_') + 1) : "", new String[] { rs.getString(2), tableList, rs.getString(4), parsingSchema, module, program });
			}
		}
		catch (SQLException ex)
		{
			_logger.warn("Problems getting the execution plans of SQL_ID '" + sqlId + "' from the DDL Storage. Caught: " + ex);
		}

		return plans;
	}

	//--------------------------------------------------------------------------------------------
	// Read from the DDL Storage
	//--------------------------------------------------------------------------------------------

	/**
	 * Get what the DDL Storage has about the names in 'tableList'.
	 * <p>
	 * A name can be <code>OWNER.NAME</code> or just <code>NAME</code> (then it can be found in more than one schema), in any case.<br>
	 * It can be a table, a view or a synonym (those two also gives the tables they read from), or an INDEX: an execution plan often names
	 * only the index, and then we want the table it is on.
	 *
	 * @param dbname  the container (PDB) to look in. null or "" = any
	 */
	public Set<OraTableInfo> getTableInformationFromMonDdlStorage(DbxConnection conn, String dbname, Set<String> tableList)
	{
		Set<OraTableInfo> result = new LinkedHashSet<>();

		if (tableList == null || tableList.isEmpty() || ! DbUtils.checkIfTableExistsNoThrow(conn, null, null, "MonDdlStorage"))
			return result;

		for (String tableName : tableList)
		{
			try
			{
				readFromMonDdlStorage(conn, dbname, tableName, 0, result);
			}
			catch (SQLException ex)
			{
				_logger.warn("Problems getting Table Information for dbname='" + dbname + "', table='" + tableName + "' from the DDL Storage. Caught: " + ex);
			}
		}

		return result;
	}

	/** One name. Calls itself for what a view or a synonym reads from (max 20 levels) */
	private void readFromMonDdlStorage(DbxConnection conn, String dbname, String tableName, int level, Set<OraTableInfo> result)
	throws SQLException
	{
		if (StringUtil.isNullOrBlank(tableName) || level > 20)
			return;

		// OWNER.NAME or NAME, without quotes
		String name  = tableName.trim().replace("\"", "");
		String owner = null;
		int    pos   = name.lastIndexOf('.');
		if (pos > 0)
		{
			owner = name.substring(0, pos);
			name  = name.substring(pos + 1);
			if (owner.indexOf('.') >= 0)
				owner = owner.substring(owner.lastIndexOf('.') + 1);
		}

		String and_dbname = StringUtil.isNullOrBlank(dbname) ? "" : "  and upper([dbname]) = " + DbUtils.safeStr(dbname.toUpperCase()) + " \n";
		String and_owner  = owner == null                    ? "" : "  and upper([owner])  = " + DbUtils.safeStr(owner .toUpperCase()) + " \n";

		String select = "select [dbname], [owner], [objectName], [type], [sampleTime], [objectText], [extraInfoText], [dependsText] \nfrom [MonDdlStorage] \n";

		// As a table, view or synonym
		List<OraTableInfo> found = readRows(conn, select
			+ "where [type] in ('TABLE', 'VIEW', 'MATERIALIZED VIEW', 'SYNONYM') \n"
			+ "  and upper([objectName]) = " + DbUtils.safeStr(name.toUpperCase()) + " \n"
			+ and_dbname
			+ and_owner);

		// As an index: the table that has an index with that name (the index table in 'extraInfoText' is then checked, a LIKE can match other things)
		if (found.isEmpty())
		{
			List<OraTableInfo> candidates = readRows(conn, select
				+ "where [type] = 'TABLE' \n"
				+ "  and upper([extraInfoText]) like " + DbUtils.safeStr("%|" + name.toUpperCase() + "%") + " \n"
				+ and_dbname);

			for (OraTableInfo ti : candidates)
			{
				parseTableText(ti);
				for (OraIndexInfo ii : ti.indexes)
				{
					if (name.equalsIgnoreCase(ii.name) && (owner == null || owner.equalsIgnoreCase(ii.owner)))
						found.add(ti);
				}
			}
		}

		for (OraTableInfo ti : found)
		{
			if (result.contains(ti))
				continue;

			if (ti.isTable())
			{
				if (ti.indexes.isEmpty()) // (already parsed when it was found via an index)
					parseTableText(ti);

				// How the table and its indexes were used during the recording
				readSegmentActivity(conn, ti);
				readIndexUsage(conn, ti);

				result.add(ti);
			}
			else
			{
				// VIEW, MATERIALIZED VIEW, SYNONYM: 'extraInfoText' is "TableList: OWNER.T1, OWNER.T2"
				result.add(ti);

				if (ti.extraInfoText != null && ti.extraInfoText.startsWith("TableList:"))
					ti.references = StringUtil.parseCommaStrToList(ti.extraInfoText.substring("TableList:".length()).trim(), true);

				for (String reference : ti.references)
					readFromMonDdlStorage(conn, ti.dbname, reference, level + 1, result);
			}
		}
	}

	private List<OraTableInfo> readRows(DbxConnection conn, String sql)
	throws SQLException
	{
		List<OraTableInfo> list = new ArrayList<>();

		try (Statement stmnt = conn.createStatement(); ResultSet rs = stmnt.executeQuery(conn.quotifySqlString(sql)))
		{
			while (rs.next())
			{
				OraTableInfo ti = new OraTableInfo();
				ti.dbname        = rs.getString(1);
				ti.owner         = rs.getString(2);
				ti.name          = rs.getString(3);
				ti.type          = rs.getString(4);
				ti.sampleTime    = rs.getTimestamp(5);
				ti.objectText    = rs.getString(6);
				ti.extraInfoText = rs.getString(7);
				ti.dependsText   = rs.getString(8);
				list.add(ti);
			}
		}

		return list;
	}

	/**
	 * 'extraInfoText' of a TABLE is four text tables (see ObjectLookupInspectorOracle): the table, its indexes, the size of every segment and its triggers.<br>
	 * All values are text: numbers are plain digits (not formatted), sizes are in KB.<br>
	 * (a recording made by an earlier version has less: no triggers, no index usage, no "rows changed since the statistics")
	 */
	private void parseTableText(OraTableInfo ti)
	{
		if (StringUtil.isNullOrBlank(ti.extraInfoText))
			return;

		List<ResultSetTableModel> rstmList = ResultSetTableModel.parseTextTables(ti.extraInfoText);

		ResultSetTableModel table    = ResultSetTableModel.getTableWithColumnNames(rstmList, false, "OWNER", "TABLE_NAME", "NUM_ROWS");
		ResultSetTableModel indexes  = ResultSetTableModel.getTableWithColumnNames(rstmList, false, "INDEX_OWNER", "INDEX_NAME", "INDEX_KEYS");
		ResultSetTableModel segments = ResultSetTableModel.getTableWithColumnNames(rstmList, false, "SEGMENT_OWNER", "SEGMENT_NAME", "SIZE_KB");
		ResultSetTableModel triggers = ResultSetTableModel.getTableWithColumnNames(rstmList, false, "TRIGGER_OWNER", "TRIGGER_NAME", "TRIGGERING_EVENT");

		ti.triggersLookedUp = triggers != null;
		if (triggers != null && triggers.getRowCount() > 0)
		{
			ti.triggerCount = triggers.getRowCount();
			for (int r = 0; r < triggers.getRowCount(); r++)
			{
				if ("ENABLED".equals(triggers.getValueAsString(r, "STATUS")))
					ti.triggerEnabledCount++;
			}

			ti.triggersText = triggers.toAsciiTableString() + "\n" + StringUtil.nullToValue(ti.dependsText, "");
		}

		if (table != null && table.getRowCount() > 0)
		{
			ti.numRows        = toLong(table.getValueAsString(0, "NUM_ROWS"));
			ti.blocks         = toLong(table.getValueAsString(0, "BLOCKS"));
			ti.avgRowLen      = toLong(table.getValueAsString(0, "AVG_ROW_LEN"));
			ti.lastAnalyzed   =        table.getValueAsString(0, "LAST_ANALYZED");
			ti.staleStats     =        table.getValueAsString(0, "STALE_STATS");
			ti.partitioned    =        table.getValueAsString(0, "PARTITIONED");
			ti.partitionCount = toLong(table.getValueAsString(0, "PARTITION_COUNT"));
			ti.partitionKeys  =        table.getValueAsString(0, "PARTITION_KEYS");
			ti.temporary      =        table.getValueAsString(0, "TEMPORARY");
			ti.iotType        =        table.getValueAsString(0, "IOT_TYPE");
			ti.compression    =        table.getValueAsString(0, "COMPRESSION");
			ti.degree         =        table.getValueAsString(0, "DEGREE");
			ti.tablespace     =        table.getValueAsString(0, "TABLESPACE_NAME");

			if (table.hasColumn("MOD_INSERTS"))
			{
				ti.modInserts = toLong(table.getValueAsString(0, "MOD_INSERTS"));
				ti.modUpdates = toLong(table.getValueAsString(0, "MOD_UPDATES"));
				ti.modDeletes = toLong(table.getValueAsString(0, "MOD_DELETES"));
			}
		}

		if (indexes != null)
		{
			for (int r = 0; r < indexes.getRowCount(); r++)
			{
				OraIndexInfo ii = new OraIndexInfo();
				ii.owner            =        indexes.getValueAsString(r, "INDEX_OWNER");
				ii.name             =        indexes.getValueAsString(r, "INDEX_NAME");
				ii.type             =        indexes.getValueAsString(r, "INDEX_TYPE");
				ii.uniqueness       =        indexes.getValueAsString(r, "UNIQUENESS");
				ii.constraintType   =        indexes.getValueAsString(r, "CONSTRAINT_TYPE");
				ii.keys             =        indexes.getValueAsString(r, "INDEX_KEYS");
				ii.numRows          = toLong(indexes.getValueAsString(r, "NUM_ROWS"));
				ii.distinctKeys     = toLong(indexes.getValueAsString(r, "DISTINCT_KEYS"));
				ii.blevel           = toLong(indexes.getValueAsString(r, "BLEVEL"));
				ii.leafBlocks       = toLong(indexes.getValueAsString(r, "LEAF_BLOCKS"));
				ii.clusteringFactor = toLong(indexes.getValueAsString(r, "CLUSTERING_FACTOR"));
				ii.status           =        indexes.getValueAsString(r, "STATUS");
				ii.visibility       =        indexes.getValueAsString(r, "VISIBILITY");
				ii.partitioned      =        indexes.getValueAsString(r, "PARTITIONED");
				ii.lastAnalyzed     =        indexes.getValueAsString(r, "LAST_ANALYZED");
				ii.tablespace       =        indexes.getValueAsString(r, "TABLESPACE_NAME");

				// 12.2 and above: how the index has been used. (NULL) = Oracle has no row for it = never used since the tracking started
				if (indexes.hasColumn("TOTAL_ACCESS_COUNT"))
				{
					ii.usageTracked    = true;
					ii.absAccessCount  = toLong(indexes.getValueAsString(r, "TOTAL_ACCESS_COUNT"));
					ii.absRowsReturned = toLong(indexes.getValueAsString(r, "TOTAL_ROWS_RETURNED"));
					ii.lastUsed        =        indexes.getValueAsString(r, "LAST_USED");
				}

				// The index of a LOB column is not something you can use (or change): its size is counted with the LOBs below
				if ( ! "LOB".equals(ii.type) )
					ti.indexes.add(ii);
			}
		}

		// The sizes: the table, its indexes and its LOBs (a partitioned object has one segment per partition: they are summed by the lookup)
		if (segments != null)
		{
			for (int r = 0; r < segments.getRowCount(); r++)
			{
				String segName = segments.getValueAsString(r, "SEGMENT_NAME");
				String segType = StringUtil.nullToValue(segments.getValueAsString(r, "SEGMENT_TYPE"), "");
				long   sizeKb  = Math.max(0, toLong(segments.getValueAsString(r, "SIZE_KB")));

				if (segType.startsWith("LOB"))
				{
					ti.lobKb = Math.max(0, ti.lobKb) + sizeKb;
				}
				else if (segType.startsWith("INDEX"))
				{
					ti.indexKb = Math.max(0, ti.indexKb) + sizeKb;
					for (OraIndexInfo ii : ti.indexes)
					{
						if (ii.name != null && ii.name.equals(segName))
							ii.sizeKb = Math.max(0, ii.sizeKb) + sizeKb;
					}
				}
				else
				{
					ti.tableKb = Math.max(0, ti.tableKb) + sizeKb;
				}
			}
		}
	}

	/** 'a', 'b', 'c' for a SQL "in (...)" */
	private static String sqlInList(Set<String> values)
	{
		String list = "";
		for (String value : values)
			list += (list.isEmpty() ? "" : ", ") + DbUtils.safeStr(value);
		return list;
	}

	/**
	 * What was done to the table and its indexes (V$SEGSTAT): during the recording = the sum of <code>CmSegmentStats_diff</code>,
	 * and since the instance was started = the last sample of <code>CmSegmentStats_abs</code>. (as the other DBMS vendors has it: "diff &lt;- abs")
	 * <p>
	 * Nothing is set when the CM was not recorded, or when the segment was not among the most active ones (the collector only records those).<br>
	 * It is the whole recording, not the report period: this is also used without a report (and it is how the other DBMS vendors has it).
	 */
	private void readSegmentActivity(DbxConnection conn, OraTableInfo ti)
	{
		if ( ! DbUtils.checkIfTableExistsNoThrow(conn, null, null, "CmSegmentStats_diff") || ! DbUtils.checkIfTableExistsNoThrow(conn, null, null, "CmSegmentStats_abs") )
			return;

		Set<String> owners = new LinkedHashSet<>();
		Set<String> names  = new LinkedHashSet<>();
		owners.add(ti.owner);
		names .add(ti.name);
		for (OraIndexInfo ii : ti.indexes)
		{
			owners.add(ii.owner);
			names .add(ii.name);
		}

		// A partitioned table or index has one segment per partition: they are summed
		// CON_NAME is NULL when the collector is NOT connected to CDB$ROOT (then there is only one container in the recording)
		String kind   = "case when [OBJECT_TYPE] like 'INDEX%' then 'INDEX' when [OBJECT_TYPE] like 'TABLE%' then 'TABLE' else 'OTHER' end";
		String select = "select [OWNER], [OBJECT_NAME], " + kind + " as [KIND] \n"
			+ "      ,sum([LOGICAL_READS]), sum([PHYSICAL_READS]), sum([PHYSICAL_WRITES]), sum([DB_BLOCK_CHANGES]), sum([SEGMENT_SCANS]), sum([ROW_LOCK_WAITS]), sum([ITL_WAITS]), sum([BUFFER_BUSY_WAITS]) \n";
		String where  = ""
			+ "where [OWNER]       in (" + sqlInList(owners) + ") \n"
			+ "  and [OBJECT_NAME] in (" + sqlInList(names)  + ") \n"
			+ "  and ([CON_NAME] is null or [CON_NAME] = " + DbUtils.safeStr(StringUtil.nullToValue(ti.dbname, "")) + ") \n";
		String groupBy = "group by [OWNER], [OBJECT_NAME], " + kind;

		// {diff or abs, the SQL}. In the "diff": not the first time a segment is seen (then the counters are "since the instance was started")
		String[][] queries = new String[][] {
			{"diff", select + "from [CmSegmentStats_diff] \n" + where + "  and " + conn.toBitAnd("[CmRowState]", CountersModel.ROW_STATE__IS_DIFF_OR_RATE_ROW) + " = 0 \n" + groupBy},
			{"abs" , select + "from [CmSegmentStats_abs] \n"  + where + "  and [SessionSampleTime] = (select max([SessionSampleTime]) from [CmSegmentStats_abs]) \n" + groupBy},
		};

		for (String[] query : queries)
		{
			boolean isDiff = "diff".equals(query[0]);

			try (Statement stmnt = conn.createStatement())
			{
				stmnt.setQueryTimeout(10);
				try (ResultSet rs = stmnt.executeQuery(conn.quotifySqlString(query[1])))
				{
					while (rs.next())
					{
						String owner = rs.getString(1);
						String name  = rs.getString(2);
						String type  = rs.getString(3);

						// Whose numbers is it: the table's, or one of its indexes'
						OraSegmentActivity activity = null;
						if ("TABLE".equals(type) && ti.owner.equals(owner) && ti.name.equals(name))
							activity = isDiff ? ti.diff : ti.abs;

						for (OraIndexInfo ii : ti.indexes)
						{
							if ("INDEX".equals(type) && name.equals(ii.name) && owner.equals(ii.owner))
								activity = isDiff ? ii.diff : ii.abs;
						}

						if (activity == null)
							continue;

						activity.logicalReads    = rs.getLong(4);
						activity.physicalReads   = rs.getLong(5);
						activity.physicalWrites  = rs.getLong(6);
						activity.blockChanges    = rs.getLong(7);
						activity.segmentScans    = rs.getLong(8);
						activity.rowLockWaits    = rs.getLong(9);
						activity.itlWaits        = rs.getLong(10);
						activity.bufferBusyWaits = rs.getLong(11);
					}
				}
			}
			catch (SQLException ex)
			{
				_logger.warn("Problems getting the activity (CmSegmentStats_" + query[0] + ") of table '" + ti.getFullName() + "' and its indexes. Skipping and continuing. Caught: " + ex);
			}
		}
	}

	/**
	 * 12.2 and above, how the indexes are used (DBA_INDEX_USAGE): during the recording = the sum of <code>CmIndexUsage_diff</code>,
	 * and since Oracle started to track it = the last sample of <code>CmIndexUsage_abs</code> (which is newer than what the DDL Storage has: that is from when the table was looked up).
	 * <p>
	 * Nothing is set when the CM was not recorded, or when the index was not among the biggest ones (the collector only records those).
	 */
	private void readIndexUsage(DbxConnection conn, OraTableInfo ti)
	{
		if (ti.indexes.isEmpty())
			return;

		if ( ! DbUtils.checkIfTableExistsNoThrow(conn, null, null, "CmIndexUsage_diff") || ! DbUtils.checkIfTableExistsNoThrow(conn, null, null, "CmIndexUsage_abs") )
			return;

		Set<String> owners = new LinkedHashSet<>();
		Set<String> names  = new LinkedHashSet<>();
		for (OraIndexInfo ii : ti.indexes)
		{
			owners.add(ii.owner);
			names .add(ii.name);
		}

		String where = ""
			+ "where [OWNER]      in (" + sqlInList(owners) + ") \n"
			+ "  and [INDEX_NAME] in (" + sqlInList(names)  + ") \n"
			+ "  and ([CON_NAME] is null or [CON_NAME] = " + DbUtils.safeStr(StringUtil.nullToValue(ti.dbname, "")) + ") \n";

		String[][] queries = new String[][] {
			{"diff", "select [OWNER], [INDEX_NAME], sum([TOTAL_ACCESS_COUNT]), sum([TOTAL_ROWS_RETURNED]), cast(null as timestamp) \nfrom [CmIndexUsage_diff] \n" + where
					+ "  and " + conn.toBitAnd("[CmRowState]", CountersModel.ROW_STATE__IS_DIFF_OR_RATE_ROW) + " = 0 \n"
					+ "group by [OWNER], [INDEX_NAME]"},
			{"abs" , "select [OWNER], [INDEX_NAME], max([TOTAL_ACCESS_COUNT]), max([TOTAL_ROWS_RETURNED]), max([LAST_USED]) \nfrom [CmIndexUsage_abs] \n" + where
					+ "  and [SessionSampleTime] = (select max([SessionSampleTime]) from [CmIndexUsage_abs]) \n"
					+ "group by [OWNER], [INDEX_NAME]"},
		};

		for (String[] query : queries)
		{
			boolean isDiff = "diff".equals(query[0]);

			try (Statement stmnt = conn.createStatement())
			{
				stmnt.setQueryTimeout(10);
				try (ResultSet rs = stmnt.executeQuery(conn.quotifySqlString(query[1])))
				{
					while (rs.next())
					{
						for (OraIndexInfo ii : ti.indexes)
						{
							if ( ! (rs.getString(2).equals(ii.name) && rs.getString(1).equals(ii.owner)) )
								continue;

							if (isDiff)
							{
								ii.diffAccessCount  = rs.getLong(3);
								ii.diffRowsReturned = rs.getLong(4);
							}
							else
							{
								Timestamp lastUsed = rs.getTimestamp(5);

								ii.usageTracked    = true;
								ii.absAccessCount  = rs.getLong(3);
								ii.absRowsReturned = rs.getLong(4);
								ii.lastUsed        = lastUsed == null ? null : new SimpleDateFormat("yyyy-MM-dd HH:mm").format(lastUsed);
							}
						}
					}
				}
			}
			catch (SQLException ex)
			{
				_logger.warn("Problems getting how the indexes of table '" + ti.getFullName() + "' are used (CmIndexUsage_" + query[0] + "). Skipping and continuing. Caught: " + ex);
			}
		}
	}

	/** @return -1 when there is no value (the text table has "(NULL)" for a NULL: for example a table without statistics) */
	private static long toLong(String str)
	{
		if (StringUtil.isNullOrBlank(str))
			return -1;

		try { return Long.parseLong(str.trim()); }
		catch (NumberFormatException ex) { return -1; }
	}

	//--------------------------------------------------------------------------------------------
	// Render
	//--------------------------------------------------------------------------------------------

	private static String num(long value)
	{
		return value < 0 ? "" : NumberFormat.getInstance().format(value);
	}

	private static String mb(long kb)
	{
		return kb < 0 ? "" : NumberFormat.getInstance().format(Math.round(kb / 1024.0));
	}

	private static String html(String str)
	{
		return str == null ? "" : StringEscapeUtils.escapeHtml4(str);
	}

	/** "during the recording" and "since the start", written as the other DBMS vendors has it: <b>diff</b> &#9887; <i>abs</i>. Empty when none of them was recorded */
	private String diffAbs(long diff, long abs)
	{
		if (diff < 0 && abs < 0)
			return "";

		if (diff >= 0 && abs >= 0)
			return diffAbsValues(diff, abs);

		return (diff < 0 ? "-" : "<b>" + num(diff) + "</b>") + " &#9887; " + (abs < 0 ? "-" : "<i>" + num(abs) + "</i>");
	}

	/** A label in the "Table Info" with a tooltip */
	private static String label(String text, String tooltip)
	{
		return "<span title='" + tooltip.replace("'", "&#39;") + "'>" + text + "</span>";
	}

	private static final String DIFF_ABS_TOOLTIP = "\n\nTwo values: [during the recording] and [since the database instance was started].\nEmpty = not recorded (the collector only records the most active segments, see CmSegmentStats)";

	/**
	 * Plain text, one block per table / view: what the LLM is told about the tables of a statement
	 */
	public String getTableInfoAsPlainText(Set<OraTableInfo> tableInfoSet)
	{
		if (tableInfoSet == null || tableInfoSet.isEmpty())
			return "";

		StringBuilder sb = new StringBuilder();

		for (OraTableInfo ti : tableInfoSet)
		{
			if ( ! ti.isTable() )
			{
				sb.append(ti.type).append(": ").append(ti.getFullName()).append("\n");
				sb.append("Text:\n").append(StringUtil.nullToValue(ti.objectText, "-not-found-")).append("\n");
				if ( ! ti.references.isEmpty() )
					sb.append("Reads from: ").append(String.join(", ", ti.references)).append("\n");
				sb.append("---\n");
				continue;
			}

			sb.append("Table: ").append(ti.getFullName())
			  .append(" (rows=").append(ti.numRows < 0 ? "unknown, no optimizer statistics" : "" + ti.numRows)
			  .append(", blocks=").append(ti.blocks)
			  .append(", avgRowLen=").append(ti.avgRowLen)
			  .append(", tableMB=").append(Math.round(Math.max(0, ti.tableKb) / 1024.0))
			  .append(", indexMB=").append(Math.round(Math.max(0, ti.indexKb) / 1024.0))
			  .append(", lobMB=").append(Math.round(Math.max(0, ti.lobKb) / 1024.0))
			  .append(", statisticsGathered=").append(StringUtil.isNullOrBlank(ti.lastAnalyzed) ? "NEVER" : ti.lastAnalyzed)
			  .append(", statisticsStale=").append(StringUtil.nullToValue(ti.staleStats, "unknown"));

			if ("YES".equals(ti.partitioned))
				sb.append(", partitions=").append(ti.partitionCount).append(", partitionKey=(").append(ti.partitionKeys).append(")");
			if ("Y".equals(ti.temporary))
				sb.append(", GLOBAL TEMPORARY");
			if (StringUtil.hasValue(ti.iotType))
				sb.append(", INDEX ORGANIZED");
			if ("ENABLED".equals(ti.compression))
				sb.append(", compressed");
			if (StringUtil.hasValue(ti.degree) && ! "1".equals(ti.degree))
				sb.append(", parallelDegree=").append(ti.degree);
			if (ti.modInserts > 0 || ti.modUpdates > 0 || ti.modDeletes > 0)
				sb.append(", rowsChangedSinceStatistics=(inserts=").append(Math.max(0, ti.modInserts)).append(", updates=").append(Math.max(0, ti.modUpdates)).append(", deletes=").append(Math.max(0, ti.modDeletes)).append(")");

			sb.append(", indexCount=").append(ti.indexes.size()).append(")\n");

			// What was done to the table while it was monitored (only when it was among the most active segments)
			if (ti.diff.isRecorded())
			{
				sb.append("Table activity during the monitored period: logicalReads=").append(ti.diff.logicalReads)
				  .append(", physicalReads=").append(ti.diff.physicalReads)
				  .append(", blockChanges=").append(ti.diff.blockChanges)
				  .append(", fullScans=").append(ti.diff.segmentScans)
				  .append(", rowLockWaits=").append(ti.diff.rowLockWaits)
				  .append("\n");
			}

			sb.append("Columns (with optimizer statistics):\n").append(StringUtil.nullToValue(ti.objectText, "-not-found-")).append("\n");

			for (OraIndexInfo ii : ti.indexes)
			{
				sb.append("Index: ").append(ii.name)
				  .append(", keys=(").append(ii.keys).append(")")
				  .append(", ").append(ii.getDescription())
				  .append(", rows=").append(ii.numRows)
				  .append(", distinctKeys=").append(ii.distinctKeys)
				  .append(", leafBlocks=").append(ii.leafBlocks)
				  .append(", blevel=").append(ii.blevel)
				  .append(", clusteringFactor=").append(ii.clusteringFactor)
				  .append(", sizeMB=").append(Math.round(Math.max(0, ii.sizeKb) / 1024.0))
				  .append(", status=").append(ii.status);

				// 12.2 and above: Oracle tracks how the index is used
				if (ii.usageTracked)
				{
					sb.append(", timesUsed=").append(ii.absAccessCount <= 0 ? "NEVER (since Oracle started to track index usage)" : "" + ii.absAccessCount);
					if (ii.absAccessCount > 0)
						sb.append(", rowsReturned=").append(Math.max(0, ii.absRowsReturned)).append(", lastUsed=").append(ii.lastUsed);
				}
				if (ii.diff.isRecorded())
					sb.append(", duringTheMonitoredPeriod=(logicalReads=").append(ii.diff.logicalReads).append(", blockChanges=").append(ii.diff.blockChanges).append(")");

				sb.append("\n");
			}

			if (ti.triggersText != null)
				sb.append("Triggers:\n").append(ti.triggersText).append("\n");

			sb.append("---\n");
		}

		return sb.toString();
	}

	/**
	 * HTML: one row per table, with the table to the left and its indexes to the right (as the other DBMS vendors has it)
	 *
	 * @param tableList  the names that were asked for: the ones that were not found are listed first. Can be null.
	 */
	public String getTableInfoAsHtmlTable(Set<OraTableInfo> tableInfoSet, Set<String> tableList, boolean includeIndexInfo, String classname)
	{
		if (tableInfoSet == null || tableInfoSet.isEmpty())
			return "";

		StringBuilder sb = new StringBuilder();

		// What was asked for, but is not in the DDL Storage (a name can be a table, or one of its indexes)
		if (tableList != null)
		{
			for (String tabName : tableList)
			{
				String shortName = tabName.replace("\"", "");
				if (shortName.indexOf('.') >= 0)
					shortName = shortName.substring(shortName.lastIndexOf('.') + 1);

				boolean exists = false;
				for (OraTableInfo ti : tableInfoSet)
				{
					if (shortName.equalsIgnoreCase(ti.name))
						exists = true;

					for (OraIndexInfo ii : ti.indexes)
					{
						if (shortName.equalsIgnoreCase(ii.name))
							exists = true;
					}
				}

				if ( ! exists )
					sb.append("&emsp; &bull; <code>").append(html(tabName)).append("</code> was NOT found in the DDL Storage.<br>\n");
			}
		}

		String className = StringUtil.hasValue(classname) ? " class='" + classname + "'" : "";

		sb.append("<table" + className + "> \n");
		sb.append("<thead> \n");
		sb.append("<tr> \n");
		sb.append("  <th>Table Info</th> \n");
		if (includeIndexInfo)
			sb.append("  <th>Index Info</th> \n");
		sb.append("</tr> \n");
		sb.append("</thead> \n");

		sb.append("<tbody> \n");
		for (OraTableInfo ti : tableInfoSet)
		{
			LinkedHashMap<String, String> map = new LinkedHashMap<>();
			String indexInfo = "";

			if ( ! ti.isTable() )
			{
				map.put("Type"     , "<b>" + ti.type + "</b>");
				map.put("Container", html(ti.dbname));
				map.put("Owner"    , html(ti.owner));
				map.put("Name"     , html(ti.name));
				map.put("Sampled"  , ti.sampleTime + "");
				map.put("Text"     , "SYNONYM".equals(ti.type) ? html(ti.objectText) : getFormattedSqlAsTooltipDiv(StringUtil.nullToValue(ti.objectText, ""), "View Text", DbUtils.DB_PROD_NAME_ORACLE));

				if ( ! ti.references.isEmpty() )
				{
					indexInfo += "Reads from: \n<ul> \n";
					for (String reference : ti.references)
						indexInfo += "<li>" + html(reference) + "</li> \n";
					indexInfo += "</ul> \n";
				}
			}
			else
			{
				// Statistics: never gathered, or stale = the optimizer works with wrong numbers
				String statistics = StringUtil.isNullOrBlank(ti.lastAnalyzed)
						? "<b><font color='red'>NEVER gathered</font></b>"
						: html(ti.lastAnalyzed) + ("YES".equals(ti.staleStats) ? " <b><font color='red'>&lt;&lt;-- STALE</font></b>" : "");

				map.put("Container"  , html(ti.dbname));
				map.put("Owner"      , html(ti.owner));
				map.put("Table"      , html(ti.name)
						+ ("Y".equals(ti.temporary)          ? " (GLOBAL TEMPORARY)" : "")
						+ (StringUtil.hasValue(ti.iotType)   ? " (INDEX ORGANIZED)"  : ""));
				map.put("Rowcount"   , ti.numRows < 0 ? "<i>unknown</i>" : num(ti.numRows));
				if ("YES".equals(ti.partitioned))
					map.put("Partitions" , num(ti.partitionCount) + " on (" + html(ti.partitionKeys) + ")");
				map.put("Total MB"   , mb(ti.getTotalKb()));
				map.put("Data MB"    , mb(ti.tableKb));
				map.put("Data Blocks", num(ti.blocks));
				map.put("Index MB"   , mb(ti.indexKb));
				if (ti.lobKb > 0)
					map.put("LOB MB" , mb(ti.lobKb));
				map.put("Avg RowLen" , num(ti.avgRowLen));
				map.put("Statistics" , statistics);

				// How much that has changed since the statistics were gathered (Oracle says they are stale at 10 percent, if nobody has changed that)
				long modified = Math.max(0, ti.modInserts) + Math.max(0, ti.modUpdates) + Math.max(0, ti.modDeletes);
				if (modified > 0)
				{
					map.put(label("Changed since Stats", "Rows changed since the optimizer statistics were gathered (DBA_TAB_MODIFICATIONS).\nOracle writes this down now and then, not at once: so it is 'about'."),
							num(modified) + " rows"
							+ (ti.numRows > 0 ? " = " + NumberFormat.getInstance().format(Math.round(modified * 1000.0 / ti.numRows) / 10.0) + " %" : "")
							+ " <i>(ins " + num(Math.max(0, ti.modInserts)) + ", upd " + num(Math.max(0, ti.modUpdates)) + ", del " + num(Math.max(0, ti.modDeletes)) + ")</i>");
				}

				if ("ENABLED".equals(ti.compression))
					map.put("Compression", "ENABLED");
				if (StringUtil.hasValue(ti.degree) && ! "1".equals(ti.degree))
					map.put("Parallel Degree", html(ti.degree));
				map.put("Tablespace" , html(ti.tablespace));
				map.put("Sampled"    , ti.sampleTime + "");

				// What was done to the table: during the recording, and since the instance was started
				if (ti.diff.isRecorded() || ti.abs.isRecorded())
				{
					map.put(label("Logical Reads"  , "Blocks of the table read from memory or disk." + DIFF_ABS_TOOLTIP), diffAbs(ti.diff.logicalReads  , ti.abs.logicalReads));
					map.put(label("Physical Reads" , "Blocks of the table read from disk."           + DIFF_ABS_TOOLTIP), diffAbs(ti.diff.physicalReads , ti.abs.physicalReads));
					map.put(label("Physical Writes", "Blocks of the table written to disk."          + DIFF_ABS_TOOLTIP), diffAbs(ti.diff.physicalWrites, ti.abs.physicalWrites));
					map.put(label("Block Changes"  , "Changes made to blocks of the table (INSERT, UPDATE, DELETE)." + DIFF_ABS_TOOLTIP), diffAbs(ti.diff.blockChanges, ti.abs.blockChanges));
					map.put(label("Full Scans"     , "Number of times the whole table was scanned."  + DIFF_ABS_TOOLTIP), diffAbs(ti.diff.segmentScans  , ti.abs.segmentScans));
					map.put(label("Row Lock Waits" , "Number of times a session had to wait for a row that was locked by another session." + DIFF_ABS_TOOLTIP), diffAbs(ti.diff.rowLockWaits, ti.abs.rowLockWaits));
					if (ti.diff.itlWaits > 0 || ti.abs.itlWaits > 0)
						map.put(label("ITL Waits"  , "Number of times a session had to wait for a free transaction slot in a block (too many transactions in the same block: a higher INITRANS helps)." + DIFF_ABS_TOOLTIP), diffAbs(ti.diff.itlWaits, ti.abs.itlWaits));
					if (ti.diff.bufferBusyWaits > 0 || ti.abs.bufferBusyWaits > 0)
						map.put(label("Buffer Busy Waits", "Number of times a session had to wait for a block that another session was reading into memory or changing." + DIFF_ABS_TOOLTIP), diffAbs(ti.diff.bufferBusyWaits, ti.abs.bufferBusyWaits));
				}

				map.put("Index Count", ti.indexes.size() + (ti.indexes.size() > 0 ? "" : " <b><font color='red'>&lt;&lt;-- Warning NO index</font></b>"));
				map.put("Columns"    , getTextAsTooltipDiv(ti.objectText, "Columns and their statistics", getDdlMaxLengthTable()));
				if (ti.triggersLookedUp)
				{
					map.put("Triggers", ti.triggersText == null
							? "-no-triggers-"
							: getTextAsTooltipDiv(ti.triggersText, ti.triggerCount + " trigger(s), " + ti.triggerEnabledCount + " enabled", getDdlMaxLengthTrigger()));
				}

				indexInfo = getIndexInfoAsHtmlTable(ti, classname);
			}

			sb.append("<tr> \n");
			sb.append("  <td>").append( HtmlTableProducer.createHtmlTable(map, "dsr-sub-table-other-info", true) ).append("</td> \n");
			if (includeIndexInfo)
				sb.append("  <td>").append( indexInfo ).append("</td> \n");
			sb.append("</tr> \n");
		}
		sb.append("</tbody> \n");
		sb.append("</table> \n");

		return sb.toString();
	}

	private String getIndexInfoAsHtmlTable(OraTableInfo ti, String classname)
	{
		if (ti.indexes.isEmpty())
			return "<b>No Index(es) was found</b><br> \n";

		String className = StringUtil.hasValue(classname) ? " class='" + classname + "'" : "";

		// Columns that are only there when we have something to put in them
		boolean hasUsage    = false; // 12.2 and above: Oracle tracks how the indexes are used
		boolean hasActivity = false; // CmSegmentStats has recorded the table or one of its indexes
		for (OraIndexInfo ii : ti.indexes)
		{
			if (ii.usageTracked)
				hasUsage = true;
			if (ii.diff.isRecorded() || ii.abs.isRecorded())
				hasActivity = true;
		}

		StringBuilder sb = new StringBuilder();
		sb.append("<table" + className + "> \n");
		sb.append("<thead> \n");
		sb.append("<tr> \n");
		sb.append(createHtmlTh("Index Name"       , ""));
		sb.append(createHtmlTh("Keys"             , "The columns of the index, in order. For a function-based index: the expression."));
		sb.append(createHtmlTh("Description"      , "PRIMARY KEY / UNIQUE / NONUNIQUE, and what kind of index it is when it is not a normal B-tree"));
		sb.append(createHtmlTh("RowCount"         , "NUM_ROWS: rows in the index, from the optimizer statistics"));
		sb.append(createHtmlTh("Distinct Keys"    , "DISTINCT_KEYS: how many different values the index has. Close to RowCount = selective. Few = many rows per value"));
		sb.append(createHtmlTh("Leaf Blocks"      , "LEAF_BLOCKS: blocks at the lowest level of the index"));
		sb.append(createHtmlTh("BLevel"           , "BLEVEL: levels above the leaf blocks. A lookup reads BLevel + 1 index blocks"));
		sb.append(createHtmlTh("Clustering Factor", "CLUSTERING_FACTOR: how well the order of the table rows follows the index.\nClose to the number of table BLOCKS = good (an index range scan reads few table blocks).\nClose to the number of ROWS = bad (every row is a new table block), then the optimizer often prefers a full table scan"));
		sb.append(createHtmlTh("Size MB"          , ""));
		if (hasUsage)
		{
			sb.append(createHtmlTh("Times Used"   , "(diff <- abs) How many times the index was used by a statement (DBA_INDEX_USAGE.TOTAL_ACCESS_COUNT).\n[during the recording] and [since Oracle started to track it: it is not reset when the database is restarted].\nOracle samples this, and writes it down every 15 minutes: so it is 'about'.\nNEVER on an index that is not unique = it only costs (space, and time at every INSERT, UPDATE and DELETE)."));
			sb.append(createHtmlTh("Rows Returned", "(diff <- abs) How many rows the index has given (DBA_INDEX_USAGE.TOTAL_ROWS_RETURNED).\nMany rows per use = the index is not selective for how it is used."));
			sb.append(createHtmlTh("Last Used"    , "When the index was used last (DBA_INDEX_USAGE.LAST_USED)"));
		}
		if (hasActivity)
		{
			sb.append(createHtmlTh("Logical Reads"    , "(diff <- abs) Blocks of the index read from memory or disk." + DIFF_ABS_TOOLTIP));
			sb.append(createHtmlTh("Physical Reads"   , "(diff <- abs) Blocks of the index read from disk." + DIFF_ABS_TOOLTIP));
			sb.append(createHtmlTh("Block Changes"    , "(diff <- abs) Changes made to blocks of the index: what it costs to keep the index up to date at INSERT, UPDATE and DELETE." + DIFF_ABS_TOOLTIP));
			sb.append(createHtmlTh("Row Lock Waits"   , "(diff <- abs) Number of times a session had to wait for a row lock on the index (for example two sessions that insert the same unique key)." + DIFF_ABS_TOOLTIP));
			sb.append(createHtmlTh("ITL Waits"        , "(diff <- abs) Number of times a session had to wait for a free transaction slot in a block of the index." + DIFF_ABS_TOOLTIP));
			sb.append(createHtmlTh("Buffer Busy Waits", "(diff <- abs) Number of times a session had to wait for a block of the index that another session was reading or changing.\nTypical for many concurrent inserts at the right hand side of an index on a sequence number." + DIFF_ABS_TOOLTIP));
		}
		sb.append(createHtmlTh("Status"           , "UNUSABLE = the index can not be used until it is rebuilt"));
		sb.append(createHtmlTh("Statistics"       , "LAST_ANALYZED: when the optimizer statistics were gathered"));
		sb.append(createHtmlTh("Tablespace"       , ""));
		sb.append("</tr> \n");
		sb.append("</thead> \n");

		sb.append("<tbody> \n");
		for (OraIndexInfo ii : ti.indexes)
		{
			sb.append("<tr> \n");
			sb.append("  <td>").append( html(ii.name) ).append("</td> \n");
			sb.append("  <td>").append( html(ii.keys) ).append("</td> \n");
			sb.append("  <td>").append( html(ii.getDescription()) ).append("</td> \n");
			sb.append("  <td>").append( num(ii.numRows) ).append("</td> \n");
			sb.append("  <td>").append( num(ii.distinctKeys) ).append("</td> \n");
			sb.append("  <td>").append( num(ii.leafBlocks) ).append("</td> \n");
			sb.append("  <td>").append( num(ii.blevel) ).append("</td> \n");
			sb.append("  <td>").append( num(ii.clusteringFactor) ).append("</td> \n");
			sb.append("  <td>").append( mb(ii.sizeKb) ).append("</td> \n");
			if (hasUsage)
			{
				// Never used, as far as Oracle knows: red when the index is not there to stop duplicates
				boolean neverUsed = ii.usageTracked && ii.absAccessCount <= 0 && ii.diffAccessCount <= 0;

				sb.append("  <td>").append( neverUsed ? ("UNIQUE".equals(ii.uniqueness) ? "NEVER" : "<b><font color='red'>NEVER</font></b>") : diffAbs(ii.diffAccessCount, ii.absAccessCount) ).append("</td> \n");
				sb.append("  <td>").append( neverUsed ? "" : diffAbs(ii.diffRowsReturned, ii.absRowsReturned) ).append("</td> \n");
				sb.append("  <td>").append( html(ii.lastUsed) ).append("</td> \n");
			}
			if (hasActivity)
			{
				sb.append("  <td>").append( diffAbs(ii.diff.logicalReads   , ii.abs.logicalReads)    ).append("</td> \n");
				sb.append("  <td>").append( diffAbs(ii.diff.physicalReads  , ii.abs.physicalReads)   ).append("</td> \n");
				sb.append("  <td>").append( diffAbs(ii.diff.blockChanges   , ii.abs.blockChanges)    ).append("</td> \n");
				sb.append("  <td>").append( diffAbs(ii.diff.rowLockWaits   , ii.abs.rowLockWaits)    ).append("</td> \n");
				sb.append("  <td>").append( diffAbs(ii.diff.itlWaits       , ii.abs.itlWaits)        ).append("</td> \n");
				sb.append("  <td>").append( diffAbs(ii.diff.bufferBusyWaits, ii.abs.bufferBusyWaits) ).append("</td> \n");
			}
			sb.append("  <td>").append( "UNUSABLE".equals(ii.status) ? "<b><font color='red'>UNUSABLE</font></b>" : html(ii.status) ).append("</td> \n");
			sb.append("  <td>").append( StringUtil.isNullOrBlank(ii.lastAnalyzed) ? "<font color='red'>NEVER</font>" : html(ii.lastAnalyzed) ).append("</td> \n");
			sb.append("  <td>").append( html(ii.tablespace) ).append("</td> \n");
			sb.append("</tr> \n");
		}
		sb.append("</tbody> \n");
		sb.append("</table> \n");

		return sb.toString();
	}
}
