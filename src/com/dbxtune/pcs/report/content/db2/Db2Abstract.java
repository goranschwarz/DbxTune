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
package com.dbxtune.pcs.report.content.db2;

import java.io.Writer;
import java.lang.invoke.MethodHandles;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.text.StringEscapeUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.dbxtune.cm.CountersModel;
import com.dbxtune.gui.ResultSetTableModel;
import com.dbxtune.pcs.inspection.ObjectLookupInspectorDb2;
import com.dbxtune.pcs.report.DailySummaryReportAbstract;
import com.dbxtune.pcs.report.content.IReportEntry;
import com.dbxtune.pcs.report.content.IndexAnalysisHtml;
import com.dbxtune.pcs.report.content.IndexRedundancy;
import com.dbxtune.pcs.report.content.ReportEntryAbstract;
import com.dbxtune.sql.conn.DbxConnection;
import com.dbxtune.utils.Configuration;
import com.dbxtune.utils.DbUtils;
import com.dbxtune.utils.HtmlTableProducer;
import com.dbxtune.utils.StringUtil;

/**
 * What the Db2 report sections have in common (as OracleAbstract / MySqlAbstract): the "Table Information".
 * <p>
 * What a table and its indexes look like is read from the DDL Storage (table <code>MonDdlStorage</code>, written by ObjectLookupInspectorDb2),
 * and how they were used during the recording from CmMonGetTable / CmMonGetIndex.<br>
 * The same reader and renderers are used without a report: DbxCentral's table info and the LLM (the static methods).
 */
public abstract class Db2Abstract
extends ReportEntryAbstract
{
	private static final Logger _logger = LogManager.getLogger(MethodHandles.lookup().lookupClass());

	public Db2Abstract(DailySummaryReportAbstract reportingInstance)
	{
		super(reportingInstance);
	}

	//--------------------------------------------------------------------------------------------
	// What the DDL Storage has about a table
	//--------------------------------------------------------------------------------------------

	/** What was done to a table, or an index. -1 = not recorded */
	public static class Db2Activity
	{
		public long rowsRead       = -1; // table: ROWS_READ,     index: INDEX_SCANS
		public long rowsInserted   = -1; // table: ROWS_INSERTED, index: INDEX_ONLY_SCANS
		public long rowsUpdated    = -1; // table: ROWS_UPDATED,  index: KEY_UPDATES
		public long rowsDeleted    = -1; // table: ROWS_DELETED
		public long tableScans     = -1; // table: TABLE_SCANS

		public boolean isRecorded() { return rowsRead >= 0; }
	}

	public static class Db2IndexInfo
	{
		public String name;
		public String schema;
		public String type;          // PRIMARY KEY, UNIQUE, INDEX
		public String keys;
		public String includeCols;
		public String indexType;     // REG, CLUS, DIM, BLOK, XVIL...
		public long   cardinality    = -1; // FULLKEYCARD: -1 = no statistics
		public long   levels         = -1;
		public String clusterPct;
		public String lastUsed;
		public String createTime;    // null = the recording was made by a version that did not store it
		public String reverseScans;  // Y / N: what the CREATE INDEX of a rollback needs to give the same index back
		public String pctFree;
		public String compression;

		public Db2Activity diff  = new Db2Activity(); // rowsRead = index scans, rowsInserted = index only scans, rowsUpdated = key updates
		public Db2Activity abs   = new Db2Activity();
		public Db2Activity atLookup = new Db2Activity(); // since the database was activated, when the table was looked up

		public boolean isUniqueOrPk() { return "PRIMARY KEY".equals(type) || "UNIQUE".equals(type); }
		public boolean isLastUsedNever() { return lastUsed == null || lastUsed.startsWith("0001-01-01"); }
	}

	public static class Db2TableInfo
	{
		public String    schema;
		public String    name;
		public String    type;          // TABLE or VIEW
		public Timestamp sampleTime;
		public String    objectText;    // the CREATE TABLE put together from the catalog, or the text of a view
		public String    extraInfoText;
		public String    dependsText;   // the triggers

		public String    db2Type;       // SYSCAT.TABLES.TYPE: T, S (MQT), G, N...
		public long      rowCountStats  = -1; // CARD: -1 = no statistics
		public long      npages         = -1;
		public long      overflowRows   = -1;
		public long      dataKb         = -1;
		public long      indexKb        = -1;
		public long      lobKb          = -1;
		public long      statsRowsModified = -1;
		public long      partitions     = -1;
		public String    statsTime;
		public String    lastUsed;
		public String    createTime;
		public String    tbspace;
		public String    tableOrg;
		public String    compression;
		public String    status;        // N = normal, C = set integrity pending, X = inoperative
		public String    isVolatile;

		public Db2Activity diff     = new Db2Activity();
		public Db2Activity abs      = new Db2Activity();
		public Db2Activity atLookup = new Db2Activity(); // since the database was activated, when the table was looked up

		public List<Db2IndexInfo> indexes = new ArrayList<>();

		public boolean isTable()        { return "TABLE".equals(type); }
		public boolean hasPrimaryKey()  { for (Db2IndexInfo ii : indexes) { if ("PRIMARY KEY".equals(ii.type)) return true; } return false; }
		public boolean hasStatistics()  { return rowCountStats >= 0; }
		public String  getFullName()    { return schema + "." + name; }

		@Override public int     hashCode()         { return Objects.hash(schema, name); }
		@Override public boolean equals(Object obj) { return obj instanceof Db2TableInfo && Objects.equals(schema, ((Db2TableInfo) obj).schema) && Objects.equals(name, ((Db2TableInfo) obj).name); }
	}

	//--------------------------------------------------------------------------------------------
	// For the report sections
	//--------------------------------------------------------------------------------------------
	@Override
	public String getDbmsTableInfoAsHtmlTable(DbxConnection conn, String currentDbname, Set<String> tableList, boolean includeIndexInfo, String classname)
	{
		Set<Db2TableInfo> tableInfoSet = getTableInformationFromMonDdlStorage(conn, currentDbname, tableList);
		if (tableInfoSet.isEmpty())
			return "";

		return getTableInfoAsHtmlTable(tableInfoSet, tableList, includeIndexInfo, classname);
	}

	//--------------------------------------------------------------------------------------------
	// Without a report (a servlet): the same lookup and rendering, as the other DBMS vendors has it
	//--------------------------------------------------------------------------------------------

	/** An instance just to reach the methods (the lookup and rendering does not use the reporting instance, so null is fine) */
	private static Db2Abstract createInstance()
	{
		return new Db2Abstract(null)
		{
			@Override public boolean hasIssueToReport()       { return false; }
			@Override public String  getSubject()             { return ""; }
			@Override public boolean hasMinimalMessageText()  { return false; }
			@Override public boolean hasShortMessageText()    { return false; }
			@Override public void    writeMessageText(Writer w, IReportEntry.MessageType t) {}
			@Override public void    create(DbxConnection c, String s, Configuration p, Configuration l) {}
		};
	}

	/** Table / index information as HTML. A name in 'tableList' is 'table' (in schema 'dbname', empty = any schema) or 'schema.table' */
	public static String getTableInfoHtml(DbxConnection conn, String dbname, Set<String> tableList, boolean includeIndexInfo, String classname)
	{
		return createInstance().getDbmsTableInfoAsHtmlTable(conn, dbname, tableList, includeIndexInfo, classname);
	}

	/** Table / index information as plain text: what the LLM is told about the tables */
	public static String getTableInfoPlainText(DbxConnection conn, String dbname, Set<String> tableList)
	{
		Db2Abstract inst = createInstance();
		return inst.getTableInfoAsPlainText( inst.getTableInformationFromMonDdlStorage(conn, dbname, tableList) );
	}

	//--------------------------------------------------------------------------------------------
	// Read from the DDL Storage
	//--------------------------------------------------------------------------------------------

	/**
	 * Get what the DDL Storage has about the names in 'tableList'.
	 * <p>
	 * A name is <code>schema.table</code>, or just <code>table</code>: then it is in schema 'dbname', and when 'dbname' is empty it can be in any schema
	 * (the package cache does not say the default schema of a statement, so the collector stored every table with that name).
	 * Names are upper case unless they are in double quotes (as Db2 does). Db2's own tables are skipped.
	 */
	public Set<Db2TableInfo> getTableInformationFromMonDdlStorage(DbxConnection conn, String dbname, Set<String> tableList)
	{
		Set<Db2TableInfo> result = new LinkedHashSet<>();

		if (tableList == null || tableList.isEmpty() || ! DbUtils.checkIfTableExistsNoThrow(conn, null, null, "MonDdlStorage"))
			return result;

		for (String tableName : tableList)
		{
			if (StringUtil.isNullOrBlank(tableName))
				continue;

			String[] schemaAndTable = ObjectLookupInspectorDb2.splitSchemaAndTable(StringUtil.hasValue(dbname) ? dbname : ObjectLookupInspectorDb2.ANY_SCHEMA, tableName);
			if (ObjectLookupInspectorDb2.isSystemObject(schemaAndTable[0], schemaAndTable[1]))
				continue;

			String and_schema = ObjectLookupInspectorDb2.ANY_SCHEMA.equals(schemaAndTable[0]) ? "" : "  and [dbname] = " + DbUtils.safeStr(schemaAndTable[0]) + " \n";
			String sql = ""
				+ "select [dbname], [objectName], [type], [sampleTime], [objectText], [extraInfoText], [dependsText] \n"
				+ "from [MonDdlStorage] \n"
				+ "where [type] in ('TABLE', 'VIEW') \n"
				+ "  and [objectName] = " + DbUtils.safeStr(schemaAndTable[1]) + " \n"
				+ and_schema;

			try (Statement stmnt = conn.createStatement(); ResultSet rs = stmnt.executeQuery(conn.quotifySqlString(sql)))
			{
				while (rs.next())
				{
					Db2TableInfo ti = new Db2TableInfo();
					ti.schema        = rs.getString(1);
					ti.name          = rs.getString(2);
					ti.type          = rs.getString(3);
					ti.sampleTime    = rs.getTimestamp(4);
					ti.objectText    = rs.getString(5);
					ti.extraInfoText = rs.getString(6);
					ti.dependsText   = rs.getString(7);

					if (result.contains(ti))
						continue;

					if (ti.isTable())
					{
						parseTableText(ti);
						readActivity(conn, ti);
					}
					result.add(ti);
				}
			}
			catch (SQLException ex)
			{
				_logger.warn("Problems getting Table Information for dbname='" + dbname + "', table='" + tableName + "' from the DDL Storage. Caught: " + ex);
			}
		}

		return result;
	}

	/**
	 * 'extraInfoText' of a TABLE is four text tables (see ObjectLookupInspectorDb2): the table, its indexes, and how both were used since the database was activated.<br>
	 * All values are text: numbers are plain digits (not formatted), sizes are in KB, (NULL) = not known.
	 */
	private void parseTableText(Db2TableInfo ti)
	{
		if (StringUtil.isNullOrBlank(ti.extraInfoText))
			return;

		List<ResultSetTableModel> rstmList = ResultSetTableModel.parseTextTables(ti.extraInfoText);

		ResultSetTableModel table      = ResultSetTableModel.getTableWithColumnNames(rstmList, false, "SCHEMA_NAME", "TABLE_NAME", "ROW_COUNT_STATS");
		ResultSetTableModel indexes    = ResultSetTableModel.getTableWithColumnNames(rstmList, false, "INDEX_NAME", "INDEX_TYPE", "INDEX_KEYS");
		ResultSetTableModel tableUsage = ResultSetTableModel.getTableWithColumnNames(rstmList, false, "TABLE_SCANS", "ROWS_READ");
		ResultSetTableModel indexUsage = ResultSetTableModel.getTableWithColumnNames(rstmList, false, "INDEX_NAME", "INDEX_SCANS");

		if (table != null && table.getRowCount() > 0)
		{
			ti.db2Type           = nullIfNullText(table.getValueAsString(0, "TABLE_TYPE"));
			ti.rowCountStats     = toLong(table.getValueAsString(0, "ROW_COUNT_STATS"));
			ti.npages            = toLong(table.getValueAsString(0, "NPAGES"));
			ti.overflowRows      = toLong(table.getValueAsString(0, "OVERFLOW_ROWS"));
			ti.dataKb            = toLong(table.getValueAsString(0, "DATA_KB"));
			ti.indexKb           = toLong(table.getValueAsString(0, "INDEX_KB"));
			ti.lobKb             = toLong(table.getValueAsString(0, "LOB_KB"));
			ti.statsRowsModified = toLong(table.getValueAsString(0, "STATS_ROWS_MODIFIED"));
			ti.partitions        = toLong(table.getValueAsString(0, "PARTITIONS"));
			ti.statsTime         = nullIfNullText(table.getValueAsString(0, "STATS_TIME"));
			ti.lastUsed          = nullIfNullText(table.getValueAsString(0, "LASTUSED"));
			ti.createTime        = nullIfNullText(table.getValueAsString(0, "CREATE_TIME"));
			ti.tbspace           = nullIfNullText(table.getValueAsString(0, "TBSPACE"));
			ti.tableOrg          = nullIfNullText(table.getValueAsString(0, "TABLEORG"));
			ti.compression       = nullIfNullText(table.getValueAsString(0, "COMPRESSION"));
			ti.status            = nullIfNullText(table.getValueAsString(0, "STATUS"));
			ti.isVolatile        = nullIfNullText(table.getValueAsString(0, "VOLATILE"));
		}

		if (tableUsage != null && tableUsage.getRowCount() > 0)
		{
			ti.atLookup.tableScans   = toLong(tableUsage.getValueAsString(0, "TABLE_SCANS"));
			ti.atLookup.rowsRead     = toLong(tableUsage.getValueAsString(0, "ROWS_READ"));
			ti.atLookup.rowsInserted = toLong(tableUsage.getValueAsString(0, "ROWS_INSERTED"));
			ti.atLookup.rowsUpdated  = toLong(tableUsage.getValueAsString(0, "ROWS_UPDATED"));
			ti.atLookup.rowsDeleted  = toLong(tableUsage.getValueAsString(0, "ROWS_DELETED"));
		}

		if (indexes != null)
		{
			for (int r = 0; r < indexes.getRowCount(); r++)
			{
				Db2IndexInfo ii = new Db2IndexInfo();
				ii.name        =                indexes.getValueAsString(r, "INDEX_NAME");
				ii.schema      = nullIfNullText(indexes.getValueAsString(r, "INDEX_SCHEMA"));
				ii.type        = nullIfNullText(indexes.getValueAsString(r, "INDEX_TYPE"));
				ii.keys        = nullIfNullText(indexes.getValueAsString(r, "INDEX_KEYS"));
				ii.includeCols = nullIfNullText(indexes.getValueAsString(r, "INCLUDE_COLS"));
				ii.indexType   = nullIfNullText(indexes.getValueAsString(r, "INDEXTYPE"));
				ii.cardinality = toLong(        indexes.getValueAsString(r, "CARDINALITY"));
				ii.levels      = toLong(        indexes.getValueAsString(r, "NLEVELS"));
				ii.clusterPct  = nullIfNullText(indexes.getValueAsString(r, "CLUSTER_PCT"));
				ii.lastUsed    = nullIfNullText(indexes.getValueAsString(r, "LASTUSED"));
				if (indexes.hasColumn("CREATE_TIME"))
				{
					ii.createTime   = nullIfNullText(indexes.getValueAsString(r, "CREATE_TIME"));
					ii.reverseScans = nullIfNullText(indexes.getValueAsString(r, "REVERSE_SCANS"));
					ii.pctFree      = nullIfNullText(indexes.getValueAsString(r, "PCTFREE"));
					ii.compression  = nullIfNullText(indexes.getValueAsString(r, "COMPRESSION"));
				}

				for (int u = 0; indexUsage != null && u < indexUsage.getRowCount(); u++)
				{
					if (ii.name.equals(indexUsage.getValueAsString(u, "INDEX_NAME")))
					{
						ii.atLookup.rowsRead     = toLong(indexUsage.getValueAsString(u, "INDEX_SCANS"));
						ii.atLookup.rowsInserted = toLong(indexUsage.getValueAsString(u, "INDEX_ONLY_SCANS"));
						ii.atLookup.rowsUpdated  = toLong(indexUsage.getValueAsString(u, "KEY_UPDATES"));
					}
				}
				ti.indexes.add(ii);
			}
		}
	}

	/**
	 * What was done to the table and its indexes: during the recording = the sum of <code>CmMonGetTable_diff</code> / <code>CmMonGetIndex_diff</code>,
	 * and since the database was activated = the last sample of the <code>_abs</code> tables. (as the other DBMS vendors has it: "diff &lt;- abs")
	 * <p>
	 * Nothing is set when the CM was not recorded, or when the table was not among the most active ones (the collector only records those).<br>
	 * It is the whole recording, not the report period: this is also used without a report.
	 */
	private void readActivity(DbxConnection conn, Db2TableInfo ti)
	{
		String where = ""
			+ "where [TABSCHEMA] = " + DbUtils.safeStr(ti.schema) + " \n"
			+ "  and [TABNAME]   = " + DbUtils.safeStr(ti.name)   + " \n";
		String and_notNewRow = "  and " + conn.toBitAnd("[CmRowState]", CountersModel.ROW_STATE__IS_DIFF_OR_RATE_ROW) + " = 0 \n";

		// {table, diff or abs, the SQL}. The first time a table is seen, the diff is "since the database was activated": those rows are not counted
		// A partitioned table (and pureScale) has one row per data partition and member: summed
		String tabCols = "sum([ROWS_READ]), sum([ROWS_INSERTED]), sum([ROWS_UPDATED]), sum([ROWS_DELETED]), sum([TABLE_SCANS])";
		String idxCols = "sum([INDEX_SCANS]), sum([INDEX_ONLY_SCANS]), sum([KEY_UPDATES]), cast(null as bigint), cast(null as bigint)";
		String[][] queries = new String[][] {
			{"CmMonGetTable", "diff", "select '' as [INDNAME], " + tabCols + " \nfrom [CmMonGetTable_diff] \n" + where + and_notNewRow},
			{"CmMonGetTable", "abs" , "select '' as [INDNAME], " + tabCols + " \nfrom [CmMonGetTable_abs] \n"  + where + "  and [SessionSampleTime] = (select max([SessionSampleTime]) from [CmMonGetTable_abs]) \n"},
			{"CmMonGetIndex", "diff", "select [INDNAME], " + idxCols + " \nfrom [CmMonGetIndex_diff] \n" + where + and_notNewRow + "group by [INDNAME]"},
			{"CmMonGetIndex", "abs" , "select [INDNAME], " + idxCols + " \nfrom [CmMonGetIndex_abs] \n"  + where + "  and [SessionSampleTime] = (select max([SessionSampleTime]) from [CmMonGetIndex_abs]) \ngroup by [INDNAME]"},
		};

		for (String[] query : queries)
		{
			boolean isDiff = "diff".equals(query[1]);
			if ( ! DbUtils.checkIfTableExistsNoThrow(conn, null, null, query[0] + "_" + query[1]) )
				continue;

			try (Statement stmnt = conn.createStatement())
			{
				stmnt.setQueryTimeout(10);
				try (ResultSet rs = stmnt.executeQuery(conn.quotifySqlString(query[2])))
				{
					while (rs.next())
					{
						// A sum() of no rows is NULL: the table (or index) was not recorded
						if (rs.getObject(2) == null)
							continue;

						String indexName = rs.getString(1);

						Db2Activity activity = null;
						if (StringUtil.isNullOrBlank(indexName))
							activity = isDiff ? ti.diff : ti.abs;

						for (Db2IndexInfo ii : ti.indexes)
						{
							if (StringUtil.hasValue(indexName) && indexName.equals(ii.name))
								activity = isDiff ? ii.diff : ii.abs;
						}

						if (activity == null)
							continue;

						activity.rowsRead     = rs.getLong(2);
						activity.rowsInserted = rs.getLong(3);
						activity.rowsUpdated  = rs.getLong(4);
						activity.rowsDeleted  = rs.getObject(5) == null ? -1 : rs.getLong(5);
						activity.tableScans   = rs.getObject(6) == null ? -1 : rs.getLong(6);
					}
				}
			}
			catch (SQLException ex)
			{
				_logger.warn("Problems getting the activity (" + query[0] + "_" + query[1] + ") of table '" + ti.getFullName() + "'. Skipping and continuing. Caught: " + ex);
			}
		}
	}

	/** @return -1 when there is no value (the text table has "(NULL)" for a NULL) */
	private static long toLong(String str)
	{
		if (StringUtil.isNullOrBlank(str))
			return -1;

		try { return Long.parseLong(str.trim()); }
		catch (NumberFormatException ex) { return -1; }
	}

	private static String nullIfNullText(String str)
	{
		return str == null || "(NULL)".equals(str) ? null : str.trim();
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

	/** "during the recording" and "since the database was activated", written as the other DBMS vendors has it: <b>diff</b> &#9887; <i>abs</i>. Empty when none of them was recorded */
	private String diffAbs(long diff, long abs)
	{
		if (diff < 0 && abs < 0)
			return "";

		if (diff >= 0 && abs >= 0)
			return diffAbsValues(diff, abs);

		return (diff < 0 ? "-" : "<b>" + num(diff) + "</b>") + " &#9887; " + (abs < 0 ? "-" : "<i>" + num(abs) + "</i>");
	}

	/** The value of the recording if it is there, otherwise the one from when the table was looked up */
	private String activity(long diff, long abs, long atLookup)
	{
		if (diff >= 0 || abs >= 0)
			return diffAbs(diff, abs);

		return atLookup < 0 ? "" : num(atLookup) + " <i>(at lookup)</i>";
	}

	/** A label in the "Table Info" with a tooltip */
	private static String label(String text, String tooltip)
	{
		return "<span title='" + tooltip.replace("'", "&#39;") + "'>" + text + "</span>";
	}

	private static final String DIFF_ABS_TOOLTIP = "\n\nTwo values: [during the recording] and [since the database was activated].\n'(at lookup)' = since the database was activated, when the table was looked up (the table was not among the most active tables that CmMonGetTable / CmMonGetIndex records).";

	/** SYSCAT.TABLES.STATUS as a text: empty when it is normal */
	private static String statusText(String status)
	{
		if ("C".equals(status)) return "SET INTEGRITY PENDING";
		if ("X".equals(status)) return "INOPERATIVE";
		return "";
	}

	/**
	 * Plain text, one block per table / view: what the LLM is told about the tables of a statement
	 */
	public String getTableInfoAsPlainText(Set<Db2TableInfo> tableInfoSet)
	{
		if (tableInfoSet == null || tableInfoSet.isEmpty())
			return "";

		StringBuilder sb = new StringBuilder();

		for (Db2TableInfo ti : tableInfoSet)
		{
			if ( ! ti.isTable() )
			{
				sb.append(ti.type).append(": ").append(ti.getFullName()).append("\n");
				sb.append(StringUtil.nullToValue(ti.objectText, "-the text of the view is not known-")).append("\n");
				sb.append("---\n");
				continue;
			}

			sb.append("Table: ").append(ti.getFullName())
			  .append(" (rows=").append(ti.hasStatistics() ? "about " + ti.rowCountStats : "unknown, NO STATISTICS (RUNSTATS has not been run)")
			  .append(", dataMB=").append(ti.dataKb < 0 ? "unknown" : "" + Math.round(ti.dataKb / 1024.0))
			  .append(", indexMB=").append(ti.indexKb < 0 ? "unknown" : "" + Math.round(ti.indexKb / 1024.0));

			if (ti.lobKb > 0)
				sb.append(", lobMB=").append(Math.round(ti.lobKb / 1024.0));
			if ("C".equals(ti.tableOrg))
				sb.append(", COLUMN-ORGANIZED");
			if (ti.partitions > 1)
				sb.append(", dataPartitions=").append(ti.partitions);
			if ( ! ti.hasPrimaryKey() )
				sb.append(", NO PRIMARY KEY");
			if (StringUtil.hasValue(ti.statsTime))
				sb.append(", statisticsCollected=").append(ti.statsTime);
			if (ti.statsRowsModified > 0)
				sb.append(", rowsChangedSinceStatistics=").append(ti.statsRowsModified);
			if (ti.overflowRows > 0)
				sb.append(", overflowRows=").append(ti.overflowRows);
			if ("Y".equals(ti.isVolatile))
				sb.append(", VOLATILE (the optimizer prefers indexes)");
			if (StringUtil.hasValue(statusText(ti.status)))
				sb.append(", STATUS=").append(statusText(ti.status));

			sb.append(", indexCount=").append(ti.indexes.size()).append(")\n");

			// What was done to the table while it was monitored (only when it was among the most active tables)
			Db2Activity act = ti.diff.isRecorded() ? ti.diff : ti.atLookup;
			if (act.isRecorded())
			{
				sb.append(ti.diff.isRecorded() ? "Table activity during the monitored period: " : "Table activity since the database was activated: ")
				  .append("tableScans=").append(act.tableScans)
				  .append(", rowsRead=").append(act.rowsRead)
				  .append(", rowsInserted=").append(act.rowsInserted)
				  .append(", rowsUpdated=").append(act.rowsUpdated)
				  .append(", rowsDeleted=").append(act.rowsDeleted)
				  .append("\n");
			}

			sb.append("DDL (from the catalog):\n").append(StringUtil.nullToValue(ti.objectText, "-not-found-")).append("\n");

			for (Db2IndexInfo ii : ti.indexes)
			{
				sb.append("Index: ").append(ii.name)
				  .append(", keys=(").append(StringUtil.nullToValue(ii.keys, "unknown")).append(")");
				if (StringUtil.hasValue(ii.includeCols))
					sb.append(", include=(").append(ii.includeCols).append(")");
				sb.append(", ").append(ii.type);
				if (ii.cardinality >= 0)
					sb.append(", fullKeyCardinality=").append(ii.cardinality);
				if (ii.levels >= 0)
					sb.append(", levels=").append(ii.levels);
				if (StringUtil.hasValue(ii.clusterPct))
					sb.append(", clusterPct=").append(ii.clusterPct);
				Db2Activity iact = ii.diff.isRecorded() ? ii.diff : ii.atLookup;
				if (iact.isRecorded())
					sb.append(ii.diff.isRecorded() ? ", indexScansDuringTheMonitoredPeriod=" : ", indexScansSinceActivation=").append(iact.rowsRead);
				if (StringUtil.hasValue(ii.lastUsed))
					sb.append(", lastUsed=").append(ii.isLastUsedNever() ? "never" : ii.lastUsed);
				sb.append("\n");
			}

			if (StringUtil.hasValue(ti.dependsText))
				sb.append("Triggers:\n").append(ti.dependsText).append("\n");

			sb.append("---\n");
		}

		return sb.toString();
	}

	/**
	 * HTML: one row per table, with the table to the left and its indexes to the right (as the other DBMS vendors has it)
	 *
	 * @param tableList  the names that were asked for: the ones that were not found are listed first. Can be null.
	 */
	public String getTableInfoAsHtmlTable(Set<Db2TableInfo> tableInfoSet, Set<String> tableList, boolean includeIndexInfo, String classname)
	{
		if (tableInfoSet == null || tableInfoSet.isEmpty())
			return "";

		StringBuilder sb = new StringBuilder();

		// What was asked for, but is not in the DDL Storage
		if (tableList != null)
		{
			for (String tabName : tableList)
			{
				String[] schemaAndTable = ObjectLookupInspectorDb2.splitSchemaAndTable(ObjectLookupInspectorDb2.ANY_SCHEMA, tabName);
				if (ObjectLookupInspectorDb2.isSystemObject(schemaAndTable[0], schemaAndTable[1]))
					continue;

				boolean exists = false;
				for (Db2TableInfo ti : tableInfoSet)
				{
					if (schemaAndTable[1].equals(ti.name))
						exists = true;
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
		for (Db2TableInfo ti : tableInfoSet)
		{
			LinkedHashMap<String, String> map = new LinkedHashMap<>();
			String indexInfo = "";

			if ( ! ti.isTable() )
			{
				map.put("Type"   , "<b>" + ti.type + "</b>");
				map.put("Schema" , html(ti.schema));
				map.put("Name"   , html(ti.name));
				map.put("Sampled", ti.sampleTime + "");
				map.put("Text"   , getTextAsTooltipDiv(ti.objectText, "View Text", getDdlMaxLengthTable()));
			}
			else
			{
				map.put("Schema"     , html(ti.schema));
				map.put("Table"      , html(ti.name) + (ti.partitions > 1 ? " (" + ti.partitions + " data partitions)" : "") + ("S".equals(ti.db2Type) ? " (materialized query table)" : ""));
				if (StringUtil.hasValue(statusText(ti.status)))
					map.put("Status", "<b><font color='red'>" + statusText(ti.status) + "</font></b>");
				map.put(label("Rowcount", "From the statistics (SYSCAT.TABLES.CARD), as of when RUNSTATS was last run"),
						ti.hasStatistics() ? num(ti.rowCountStats) : "<b><font color='red'>No statistics</font></b> (RUNSTATS has not been run: the optimizer guesses)");
				map.put("Data MB"    , mb(ti.dataKb));
				map.put("Index MB"   , mb(ti.indexKb));
				if (ti.lobKb > 0)
					map.put("LOB MB" , mb(ti.lobKb));
				if (StringUtil.hasValue(ti.tbspace))
					map.put("Table Space", html(ti.tbspace));
				if ("C".equals(ti.tableOrg))
					map.put("Organization", "COLUMN (BLU)");
				if ("R".equals(ti.compression) || "B".equals(ti.compression))
					map.put("Compression", "Row compression");

				// The statistics: when, and how much that has changed since
				if (StringUtil.hasValue(ti.statsTime))
				{
					map.put(label("Statistics", "When RUNSTATS was last run (SYSCAT.TABLES.STATS_TIME), and rows changed since then (ADMIN_GET_TAB_INFO.STATS_ROWS_MODIFIED)"),
							html(ti.statsTime) + (ti.statsRowsModified > 0 ? ", changed since: " + num(ti.statsRowsModified) + " rows"
							+ (ti.rowCountStats > 0 ? " = " + NumberFormat.getInstance().format(Math.round(ti.statsRowsModified * 1000.0 / ti.rowCountStats) / 10.0) + " %" : "") : ""));
				}
				if (ti.overflowRows > 0)
				{
					map.put(label("Overflow Rows", "Rows that did not fit on their page after an UPDATE and were moved (a pointer is left): every read of them is two reads. A REORG fixes it."),
							num(ti.overflowRows));
				}
				if ("Y".equals(ti.isVolatile))
					map.put(label("Volatile", "The table is marked VOLATILE: the optimizer uses an index when it can, whatever the statistics say"), "YES");
				if (StringUtil.hasValue(ti.lastUsed))
					map.put(label("Last Used", "The date the table was last used (SYSCAT.TABLES.LASTUSED, updated at most once a day)"), html(ti.lastUsed));
				if (StringUtil.hasValue(ti.createTime))
					map.put("Created", html(ti.createTime));
				map.put("Sampled", ti.sampleTime + "");

				// What was done to the table: during the recording, and since the database was activated
				map.put(label("Table Scans"  , "Scans of the whole table (TABLE_SCANS). Many on a big table = a missing index." + DIFF_ABS_TOOLTIP), activity(ti.diff.tableScans  , ti.abs.tableScans  , ti.atLookup.tableScans));
				map.put(label("Rows Read"    , "Rows read from the table (ROWS_READ)."      + DIFF_ABS_TOOLTIP), activity(ti.diff.rowsRead    , ti.abs.rowsRead    , ti.atLookup.rowsRead));
				map.put(label("Rows Inserted", "Rows inserted into the table."              + DIFF_ABS_TOOLTIP), activity(ti.diff.rowsInserted, ti.abs.rowsInserted, ti.atLookup.rowsInserted));
				map.put(label("Rows Updated" , "Rows updated in the table."                 + DIFF_ABS_TOOLTIP), activity(ti.diff.rowsUpdated , ti.abs.rowsUpdated , ti.atLookup.rowsUpdated));
				map.put(label("Rows Deleted" , "Rows deleted from the table."               + DIFF_ABS_TOOLTIP), activity(ti.diff.rowsDeleted , ti.abs.rowsDeleted , ti.atLookup.rowsDeleted));

				map.put("Index Count", ti.indexes.size() + (ti.hasPrimaryKey() || ti.indexes.isEmpty() ? "" : " <b><font color='red'>&lt;&lt;-- Warning NO primary key</font></b>"));
				map.put("DDL"        , getTextAsTooltipDiv(ti.objectText, "Table DDL (from the catalog)", getDdlMaxLengthTable()));
				if (StringUtil.hasValue(ti.dependsText))
					map.put("Triggers", getTextAsTooltipDiv(ti.dependsText, "Triggers", getDdlMaxLengthTrigger()));

				indexInfo = getIndexInfoAsHtmlTable(ti, classname) + getRedundantIndexInfoAsHtml(ti);
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

	private String getIndexInfoAsHtmlTable(Db2TableInfo ti, String classname)
	{
		if (ti.indexes.isEmpty())
			return "<b>No Index(es) was found</b><br> \n";

		String className = StringUtil.hasValue(classname) ? " class='" + classname + "'" : "";

		// The index scans: from the recording (CmMonGetIndex) when it is there, otherwise from when the table was looked up
		boolean hasRecording = false;
		for (Db2IndexInfo ii : ti.indexes)
		{
			if (ii.diff.isRecorded() || ii.abs.isRecorded())
				hasRecording = true;
		}

		StringBuilder sb = new StringBuilder();
		sb.append("<table" + className + "> \n");
		sb.append("<thead> \n");
		sb.append("<tr> \n");
		sb.append(createHtmlTh("Index Name" , "SQLnnn... = Db2 made the index for a primary key or unique constraint"));
		sb.append(createHtmlTh("Keys"       , "The key columns of the index, in order (and INCLUDE columns)"));
		sb.append(createHtmlTh("Type"       , "PRIMARY KEY / UNIQUE / INDEX, and the kind: REG = regular, CLUS = clustering, DIM / BLOK = MDC, XVIL / XVIP / XRGN = XML"));
		sb.append(createHtmlTh("Cardinality", "Distinct values of the full key (FULLKEYCARD, from the statistics). Close to the Rowcount = selective"));
		sb.append(createHtmlTh("Levels"     , "Levels of the index tree (NLEVELS)"));
		sb.append(createHtmlTh("Cluster %"  , "How well the table rows are in the order of the index (CLUSTERFACTOR or CLUSTERRATIO). Low = a range scan reads many pages"));
		sb.append(createHtmlTh("Last Used"  , "The date the index was last used (LASTUSED, updated at most once a day). 0001-01-01 = never since it was created"));
		String unusedTooltip = "\n0 on an index that is not unique = it only costs (space, and time at every INSERT, UPDATE and DELETE).";
		if (hasRecording)
			sb.append(createHtmlTh("Index Scans", "(diff <- abs) Scans of the index (INDEX_SCANS)." + DIFF_ABS_TOOLTIP + unusedTooltip));
		else
			sb.append(createHtmlTh("Index Scans Since Activation", "Scans of the index since the database was activated, when the table was looked up (MON_GET_INDEX)." + unusedTooltip));
		sb.append("</tr> \n");
		sb.append("</thead> \n");

		sb.append("<tbody> \n");
		for (Db2IndexInfo ii : ti.indexes)
		{
			// Not used: red when the index is not there to stop duplicates
			long    sinceStart = ii.abs.isRecorded() ? ii.abs.rowsRead : ii.atLookup.rowsRead;
			boolean neverUsed  = sinceStart == 0 && ii.diff.rowsRead <= 0;
			String  unused     = ii.isUniqueOrPk() ? "0" : "<b><font color='red'>0</font></b>";

			sb.append("<tr> \n");
			sb.append("  <td>").append( html(ii.name) ).append("</td> \n");
			sb.append("  <td>").append( html(ii.keys) ).append( StringUtil.hasValue(ii.includeCols) ? " <i>INCLUDE (" + html(ii.includeCols) + ")</i>" : "" ).append("</td> \n");
			sb.append("  <td>").append( html(ii.type) ).append( StringUtil.hasValue(ii.indexType) && ! "REG".equals(ii.indexType) ? " (" + html(ii.indexType) + ")" : "" ).append("</td> \n");
			sb.append("  <td>").append( ii.cardinality < 0 ? "<i>no stats</i>" : num(ii.cardinality) ).append("</td> \n");
			sb.append("  <td>").append( num(ii.levels) ).append("</td> \n");
			sb.append("  <td>").append( html(ii.clusterPct) ).append("</td> \n");
			sb.append("  <td>").append( ii.isLastUsedNever() && ! ii.isUniqueOrPk() ? "<b><font color='red'>" + html(ii.lastUsed) + "</font></b>" : html(ii.lastUsed) ).append("</td> \n");
			if (hasRecording)
				sb.append("  <td>").append( neverUsed ? "<b>0</b> &#9887; " + unused : diffAbs(ii.diff.rowsRead, ii.abs.rowsRead) ).append("</td> \n");
			else
				sb.append("  <td>").append( neverUsed ? unused : num(ii.atLookup.rowsRead) ).append("</td> \n");
			sb.append("</tr> \n");
		}
		sb.append("</tbody> \n");
		sb.append("</table> \n");

		return sb.toString();
	}

	//--------------------------------------------------------------------------------------------
	// Index Analysis
	//--------------------------------------------------------------------------------------------
	/** A FOREIGN KEY in the CREATE TABLE text the lookup put together: ALTER TABLE ... FOREIGN KEY (C1, C2) REFERENCES ... */
	private static final Pattern DB2_FK = Pattern.compile("FOREIGN KEY \\(([^)]*)\\) REFERENCES");

	/** A key column of an expression-based index: Db2 stores a generated name (K00, K01...) instead of the expression */
	private static final Pattern DB2_GENERATED_KEY = Pattern.compile("K\\d{2,}");

	/**
	 * Index Analysis for Db2: indexes that can be dropped (De-duplication) and Unused Indexes, with DDL and ROLLBACK scripts.
	 * <ul>
	 *   <li>No hidden key: an index points at the row by its RID. A CLUSTER index only decides where new rows go (a placement index).</li>
	 *   <li>Only drops, no merges: INCLUDE is only for unique indexes. A PRIMARY KEY or UNIQUE index, and the CLUSTER index, are never dropped.</li>
	 *   <li>Every script is ONE compound statement with a savepoint: a failing script changes nothing.</li>
	 * </ul>
	 * @return HTML, or "" if there is nothing to tell
	 */
	public static String getRedundantIndexInfoAsHtml(Db2TableInfo ti)
	{
		if (ti == null || ! ti.isTable() || ti.indexes.isEmpty())
			return "";

		IndexAnalysisHtml ia = new IndexAnalysisHtml();
		ia.capturedTime = String.valueOf(ti.sampleTime);

		// ---- The whole table can't be checked
		String tableReason = null;
		if      ("C".equals(ti.tableOrg))                        tableReason = "a column-organized table: Db2 makes and uses its own indexes";
		else if (ti.db2Type != null && ! "T".equals(ti.db2Type)) tableReason = "not a regular table (type " + ti.db2Type + ")";
		else if (ti.partitions > 1)                              tableReason = "a partitioned table";
		if (tableReason != null)
		{
			for (Db2IndexInfo ii : ti.indexes)
				ia.dedupNotChecked.put(ii.name, tableReason);
			ia.unusedNotChecked = tableReason;
			return ia.toHtml();
		}

		// The FOREIGN KEYs, from the CREATE TABLE text
		List<List<String>> foreignKeys = new ArrayList<>();
		Matcher fkm = DB2_FK.matcher(StringUtil.nullToValue(ti.objectText, ""));
		while (fkm.find())
			foreignKeys.add(StringUtil.parseCommaStrToList(fkm.group(1), true));

		// ---- The indexes we can compare
		Map<IndexRedundancy.Idx, Db2IndexInfo> infoMap    = new LinkedHashMap<>();
		Map<String, String>                    notChecked = new LinkedHashMap<>();
		int pos = 0;
		for (Db2IndexInfo ii : ti.indexes)
		{
			List<String> keys = db2Keys(ii.keys);
			String reason = null;
			if      ( ! "REG".equals(ii.indexType) && ! "CLUS".equals(ii.indexType) ) reason = "index type " + ii.indexType + ", only regular and clustering indexes are compared";
			else if (keys.isEmpty())                                                 reason = "the key columns are not known";
			else if (db2HasExpressionKey(ti, keys))                                  reason = "a key is an expression";

			if (reason != null)
			{
				notChecked.put(ii.name, reason);
				continue;
			}

			IndexRedundancy.Idx idx = new IndexRedundancy.Idx(ii.name, keys, StringUtil.parseCommaStrToList(ii.includeCols, true));
			idx.primaryKey         = "PRIMARY KEY".equals(ii.type);
			idx.unique             = ii.isUniqueOrPk();
			idx.uniqueConstraint   = idx.unique; // never dropped: it enforces a rule (and may be the index of a constraint)
			idx.clustered          = "CLUS".equals(ii.indexType);
			idx.clusteredHoldsRows = false;      // a placement index
			idx.canGetIncludes     = false;      // INCLUDE only on a unique index, and only by a new CREATE INDEX: no merges
			idx.reads              = db2ScansSinceActivation(ii);
			idx.id                 = pos++;
			infoMap.put(idx, ii);
		}

		List<IndexRedundancy.Finding> findings = IndexRedundancy.analyze(new ArrayList<>(infoMap.keySet()), Collections.emptyList());

		Set<String> inDedup = new HashSet<>();
		for (IndexRedundancy.Finding f : findings)
		{
			inDedup.add(f.kept.name);
			for (IndexRedundancy.Idx dr : f.dropped)
				inDedup.add(dr.name);
		}

		// ---- Unused: LASTUSED never (or older than minDays) at the lookup, no scans since the database was activated, and the index is at least minDays old
		int    minDays    = Configuration.getCombinedConfiguration().getIntProperty(IndexAnalysisHtml.PROPKEY_unused_minDays, IndexAnalysisHtml.DEFAULT_unused_minDays);
		String lookupDate = ti.sampleTime == null ? null : new java.text.SimpleDateFormat("yyyy-MM-dd").format(ti.sampleTime);
		String minDate    = ti.sampleTime == null ? null : new java.text.SimpleDateFormat("yyyy-MM-dd").format(new java.util.Date(ti.sampleTime.getTime() - minDays * 24L * 3600 * 1000));

		List<Db2IndexInfo> unused      = new ArrayList<>();
		Set<String>        unusedDedup = new HashSet<>();
		int                tooNew      = 0;
		int                ageUnknown  = 0;
		int                forFk       = 0;
		for (Map.Entry<IndexRedundancy.Idx, Db2IndexInfo> e : infoMap.entrySet())
		{
			IndexRedundancy.Idx idx = e.getKey();
			Db2IndexInfo        ii  = e.getValue();
			if (idx.unique || idx.clustered || minDate == null)
				continue;
			if (idx.reads > 0 || StringUtil.isNullOrBlank(ii.lastUsed))
				continue;

			boolean never = ii.isLastUsedNever();
			if ( ! never && ii.lastUsed.compareTo(minDate) > 0 ) // used the last minDays
				continue;

			// How old is it: an index made yesterday has not been used either
			if (ii.createTime == null)
			{
				if (never) // the recording has no CREATE_TIME (an older version): never used can be just made
				{
					ageUnknown++;
					continue;
				}
			}
			else if (ii.createTime.compareTo(minDate) > 0)
			{
				tooNew++;
				continue;
			}

			if (inDedup.contains(ii.name))
			{
				unusedDedup.add(ii.name);
				continue;
			}

			if (db2OnlyIndexForForeignKey(infoMap, foreignKeys, idx))
			{
				forFk++;
				continue;
			}
			unused.add(ii);
		}

		// ---- The texts and findings, laid out by IndexAnalysisHtml (the same for all vendors)
		ia.notesHtml.add("<b>Last Used</b> (<code>SYSCAT.INDEXES.LASTUSED</code>) is kept over restarts, but Db2 writes it down at most once a day: check that the index isn't needed for something that runs rarely (month end, year end).");
		ia.notesHtml.add("<b>Use on an HADR standby (reads on standby) is not counted on the primary</b>: an index used only there looks unused here.");
		ia.notesHtml.add("Dropping an index can change the plans of other statements, and packages that use it are invalidated (rebound at the next use).");
		ia.notesHtml.add("Each script is ONE compound statement: it first checks that the indexes are there, then changes them. If anything fails, nothing is changed. "
				+ "It ends with <code>@</code>: <code>db2 -td@ -f file.sql</code>. With autocommit off, <code>COMMIT</code> after it.");

		for (IndexRedundancy.Finding f : findings)
		{
			IndexAnalysisHtml.Finding hf = new IndexAnalysisHtml.Finding();
			hf.action = "DROP";
			hf.kept   = f.kept.name;

			List<Db2IndexInfo> dropped = new ArrayList<>();
			hf.lines.add(db2IndexLine("Keep", infoMap.get(f.kept)));
			for (IndexRedundancy.Idx dr : f.dropped)
			{
				hf.dropped.add(dr.name);
				dropped.add(infoMap.get(dr));
				hf.lines.add(db2IndexLine("Drop", infoMap.get(dr)));
			}

			StringBuilder why = new StringBuilder();
			for (String w : f.why)
				why.append(w.replace("is the clustered index, but on this table it is a placement index: it does not hold the data rows, it has the same keys as a nonclustered index.",
				                     "is the CLUSTER index: it only decides where new rows go, it is an index like the others.")).append(" ");
			for (String n : hf.dropped)
				if (unusedDedup.contains(n))
					why.append(n + " has not been used for at least " + minDays + " days. ");
			if (unusedDedup.contains(f.kept.name))
				why.append(f.kept.name + " has not been used for at least " + minDays + " days either: check if it is needed at all. ");
			hf.why    = why.toString();
			hf.script = createDb2IndexScript(ti, infoMap.get(f.kept), dropped);
			ia.dedup.add(hf);
		}
		ia.dedupNotChecked = notChecked;

		ia.unusedIntro = "Not used for at least " + minDays + " days when the table was looked up (" + lookupDate + "): Last Used is never or before " + minDate
				+ ", the index is older than that, and no scans since the database was activated. "
				+ "Not listed: the primary key, unique indexes, the CLUSTER index, and the indexes in Index De-duplication above.";
		ia.unusedNotes.add("Last Used is the date Db2 last used the index for a query (also for an index-only scan). It is not updated when the index is only maintained by INSERT, UPDATE and DELETE.");
		ia.unusedNotes.add("An index made less than " + minDays + " days before the lookup is not listed (" + tooNew + " on this table)."
				+ (ageUnknown == 0 ? "" : " Never used, but the age is not known (recorded by an older version): " + ageUnknown + " not listed."));
		ia.unusedNotes.add("The only index whose first columns are a FOREIGN KEY is not listed (" + forFk + " on this table): without it, a DELETE or key UPDATE in the parent table scans this table.");
		for (Db2IndexInfo ii : unused)
		{
			IndexAnalysisHtml.Finding hf = new IndexAnalysisHtml.Finding();
			hf.action = "DROP";
			hf.dropped.add(ii.name);
			hf.lines.add(db2IndexLine("Index", ii));
			hf.script = createDb2IndexScript(ti, null, Collections.singletonList(ii));
			ia.unused.add(hf);
		}

		return ia.toHtml();
	}

	/** "C1, C2 DESC" -> [C1, C2 DESC] */
	private static List<String> db2Keys(String keys)
	{
		List<String> result = new ArrayList<>();
		if (StringUtil.isNullOrBlank(keys))
			return result;
		for (String k : keys.split(", "))
			if (StringUtil.hasValue(k))
				result.add(k.trim());
		return result;
	}

	/** A key named K00, K01... that is not a column of the table: Db2 made it for an expression */
	private static boolean db2HasExpressionKey(Db2TableInfo ti, List<String> keys)
	{
		for (String k : keys)
		{
			String col = db2ColName(k);
			if (DB2_GENERATED_KEY.matcher(col).matches() && ! StringUtil.nullToValue(ti.objectText, "").contains("\"" + col + "\" "))
				return true;
		}
		return false;
	}

	/** C1 DESC -> C1 */
	private static String db2ColName(String key)
	{
		return key.endsWith(" DESC") ? key.substring(0, key.length() - " DESC".length()) : key;
	}

	/** Index scans (incl. index-only scans) since the database was activated: the newest number there is (at the lookup, the last sample, or during the recording). -1 = not known */
	private static long db2ScansSinceActivation(Db2IndexInfo ii)
	{
		long result = -1;
		for (Db2Activity a : new Db2Activity[] { ii.atLookup, ii.abs, ii.diff })
		{
			result = Math.max(result, a.rowsRead);
			result = Math.max(result, a.rowsInserted); // index-only scans
		}
		return result;
	}

	/** Is this the only (compared) index whose first columns are the columns of a FOREIGN KEY, in any order? */
	private static boolean db2OnlyIndexForForeignKey(Map<IndexRedundancy.Idx, Db2IndexInfo> infoMap, List<List<String>> foreignKeys, IndexRedundancy.Idx index)
	{
		for (List<String> fk : foreignKeys)
		{
			if ( ! db2LeadsWith(index.keys, fk) )
				continue;
			boolean other = false;
			for (IndexRedundancy.Idx i : infoMap.keySet())
				if (i != index && db2LeadsWith(i.keys, fk))
					other = true;
			if ( ! other )
				return true;
		}
		return false;
	}

	private static boolean db2LeadsWith(List<String> keys, List<String> cols)
	{
		if (cols.isEmpty() || cols.size() > keys.size())
			return false;
		Set<String> first = new HashSet<>();
		for (int i = 0; i < cols.size(); i++)
			first.add(db2ColName(keys.get(i)));
		return first.containsAll(cols);
	}

	private static IndexAnalysisHtml.IndexLine db2IndexLine(String label, Db2IndexInfo ii)
	{
		long   scans = db2ScansSinceActivation(ii);
		String name  = ("PRIMARY KEY".equals(ii.type) ? "PRIMARY KEY " : "UNIQUE".equals(ii.type) ? "UNIQUE " : "") + ii.name + ("CLUS".equals(ii.indexType) ? " (CLUSTER)" : "");
		String def   = name + " (" + ii.keys + ")" + (StringUtil.hasValue(ii.includeCols) ? " INCLUDE (" + ii.includeCols + ")" : "");
		String last  = "Last Used " + (ii.isLastUsedNever() ? "never" : ii.lastUsed); // the catalog date: Db2 writes it down later than the scans are counted
		return new IndexAnalysisHtml.IndexLine(label, def, ", " + last + ", scans since activation " + (scans < 0 ? "unknown" : scans));
	}

	/** "name" with any " doubled */
	private static String db2Qi(String name)
	{
		return "\"" + StringUtil.nullToValue(name, "").trim().replace("\"", "\"\"") + "\"";
	}

	/** The CREATE INDEX that gives the (non-unique) index back as it was */
	private static String db2CreateIndex(Db2TableInfo ti, Db2IndexInfo ii)
	{
		List<String> cols = new ArrayList<>();
		for (String k : db2Keys(ii.keys))
			cols.add(db2Qi(db2ColName(k)) + (k.endsWith(" DESC") ? " DESC" : ""));

		StringBuilder sb = new StringBuilder();
		sb.append("CREATE INDEX ").append(db2Qi(ii.schema == null ? ti.schema : ii.schema)).append(".").append(db2Qi(ii.name))
		  .append(" ON ").append(db2Qi(ti.schema)).append(".").append(db2Qi(ti.name)).append(" (").append(String.join(", ", cols)).append(")");
		if (ii.pctFree != null && ! "-1".equals(ii.pctFree))
			sb.append(" PCTFREE ").append(ii.pctFree);
		if ("Y".equals(ii.reverseScans)) sb.append(" ALLOW REVERSE SCANS");
		if ("N".equals(ii.reverseScans)) sb.append(" DISALLOW REVERSE SCANS");
		if ("Y".equals(ii.compression))  sb.append(" COMPRESS YES");
		if ("N".equals(ii.compression))  sb.append(" COMPRESS NO");
		return sb.toString();
	}

	/**
	 * A compound statement that does all or nothing: a savepoint first, and any error rolls back to it.<br>
	 * (Without it, a failing block is undone by the CLP with autocommit, but JDBC with autocommit keeps what was done before the error: tested on Db2 11.5)
	 */
	private static final String DB2_BLOCK_BEGIN = ""
			+ "BEGIN\n"
			+ "    DECLARE EXIT HANDLER FOR SQLEXCEPTION BEGIN ROLLBACK TO SAVEPOINT DBX_INDEX_ANALYSIS; RESIGNAL; END;\n"
			+ "    SAVEPOINT DBX_INDEX_ANALYSIS ON ROLLBACK RETAIN CURSORS;\n";
	private static final String DB2_BLOCK_END = ""
			+ "    RELEASE SAVEPOINT DBX_INDEX_ANALYSIS;\n"
			+ "END@\n";

	/**
	 * DDL CHANGES and ROLLBACK, each ONE compound statement: first the checks (SIGNAL = nothing changed), then the changes, all or nothing.
	 *
	 * @param kept  the index that stays (checked to exist), null for unused indexes
	 */
	private static String createDb2IndexScript(Db2TableInfo ti, Db2IndexInfo kept, List<Db2IndexInfo> dropped)
	{
		boolean oldRecording = false;
		for (Db2IndexInfo d : dropped)
			if (d.createTime == null)
				oldRecording = true;

		StringBuilder sb = new StringBuilder();
		sb.append("-- DDL CHANGES: ").append(ti.getFullName()).append("\n");
		sb.append("-- Captured ").append(ti.sampleTime).append(": check that the indexes still look like this before running.\n");
		sb.append("-- One statement (it ends with the at sign): if anything in it fails, nothing is changed\n");
		sb.append(DB2_BLOCK_BEGIN);
		if (kept != null)
			appendDb2IndexCheck(sb, ti, kept, true);
		for (Db2IndexInfo d : dropped)
			appendDb2IndexCheck(sb, ti, d, true);
		for (Db2IndexInfo d : dropped)
			sb.append("    EXECUTE IMMEDIATE '").append(("DROP INDEX " + db2Qi(d.schema == null ? ti.schema : d.schema) + "." + db2Qi(d.name)).replace("'", "''")).append("';\n");
		sb.append(DB2_BLOCK_END);

		sb.append("\n");
		sb.append("-- ROLLBACK: puts back what the DDL CHANGES did\n");
		if (oldRecording)
			sb.append("-- Recorded by an older version: PCTFREE, REVERSE SCANS and COMPRESS are not known, Db2's defaults are used\n");
		sb.append(DB2_BLOCK_BEGIN);
		if (kept != null)
			appendDb2IndexCheck(sb, ti, kept, true);
		for (Db2IndexInfo d : dropped)
			appendDb2IndexCheck(sb, ti, d, false);
		for (Db2IndexInfo d : dropped)
			sb.append("    EXECUTE IMMEDIATE '").append(db2CreateIndex(ti, d).replace("'", "''")).append("';\n");
		sb.append(DB2_BLOCK_END);
		return sb.toString();
	}

	private static void appendDb2IndexCheck(StringBuilder sb, Db2TableInfo ti, Db2IndexInfo ii, boolean shouldExist)
	{
		String schema = ii.schema == null ? ti.schema : ii.schema;
		String msg    = "Index " + ii.name + (shouldExist ? " does not exist" : " already exists") + ": nothing was changed.";
		if (msg.length() > 70) // the most SIGNAL takes
			msg = (shouldExist ? "An index does not exist" : "An index already exists") + ": nothing was changed.";
		sb.append("    IF ").append(shouldExist ? "NOT EXISTS" : "EXISTS")
		  .append(" (SELECT 1 FROM SYSCAT.INDEXES WHERE INDSCHEMA = '").append(schema.replace("'", "''")).append("' AND INDNAME = '").append(ii.name.replace("'", "''"))
		  .append("') THEN SIGNAL SQLSTATE '75001' SET MESSAGE_TEXT = '").append(msg.replace("'", "''")).append("'; END IF;\n");
	}
}
