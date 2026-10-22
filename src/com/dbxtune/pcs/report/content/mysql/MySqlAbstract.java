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
package com.dbxtune.pcs.report.content.mysql;

import java.io.Writer;
import java.lang.invoke.MethodHandles;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;

import org.apache.commons.text.StringEscapeUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.dbxtune.cm.CountersModel;
import com.dbxtune.gui.ResultSetTableModel;
import com.dbxtune.pcs.inspection.ObjectLookupInspectorMySql;
import com.dbxtune.pcs.report.DailySummaryReportAbstract;
import com.dbxtune.pcs.report.content.IReportEntry;
import com.dbxtune.pcs.report.content.ReportEntryAbstract;
import com.dbxtune.sql.conn.DbxConnection;
import com.dbxtune.utils.Configuration;
import com.dbxtune.utils.DbUtils;
import com.dbxtune.utils.HtmlTableProducer;
import com.dbxtune.utils.StringUtil;

/**
 * What the MySQL report sections have in common (as OracleAbstract / PostgresAbstract): the "Table Information".
 * <p>
 * What a table and its indexes look like is read from the DDL Storage (table <code>MonDdlStorage</code>, written by ObjectLookupInspectorMySql),
 * and how they were used during the recording from CmSysTableStats / CmSysIndexStats.<br>
 * The same reader and renderers are used without a report: DbxCentral's table info and the LLM (the static methods).
 */
public abstract class MySqlAbstract
extends ReportEntryAbstract
{
	private static final Logger _logger = LogManager.getLogger(MethodHandles.lookup().lookupClass());

	public MySqlAbstract(DailySummaryReportAbstract reportingInstance)
	{
		super(reportingInstance);
	}

	//--------------------------------------------------------------------------------------------
	// What the DDL Storage has about a table
	//--------------------------------------------------------------------------------------------

	/** What was done to a table, or an index. -1 = not recorded (the collector only records the most active tables and indexes) */
	public static class MyActivity
	{
		public long rowsRead       = -1; // table: rows_fetched, index: rows_selected
		public long rowsInserted   = -1;
		public long rowsUpdated    = -1;
		public long rowsDeleted    = -1;
		public long latencyMs      = -1; // table: total_latency, index: select_latency (picoseconds in the recording)

		public boolean isRecorded() { return rowsRead >= 0; }
	}

	public static class MyIndexInfo
	{
		public String name;
		public String type;          // PRIMARY KEY, UNIQUE, INDEX, FULLTEXT, SPATIAL, CLUSTERED (no primary key)
		public String keys;          // null = not known (a partitioned table, without REFERENCES)
		public long   cardinality    = -1; // only with REFERENCES
		public long   rowsReadAtLookup = -1; // since the server started, when the table was looked up (performance_schema)

		public MyActivity diff = new MyActivity();
		public MyActivity abs  = new MyActivity();

		public boolean isUniqueOrPk() { return "PRIMARY KEY".equals(type) || "UNIQUE".equals(type) || (type != null && type.startsWith("CLUSTERED")); }
	}

	public static class MyTableInfo
	{
		public String    schema;
		public String    name;
		public String    type;          // TABLE or VIEW
		public Timestamp sampleTime;
		public String    objectText;    // SHOW CREATE TABLE, or the columns from the InnoDB data dictionary (as a text table)
		public String    extraInfoText;
		public String    dependsText;   // the triggers

		public long      partitions     = -1;
		public long      rowsEstimate   = -1;
		public long      fileKb         = -1;
		public long      dataKb         = -1;
		public long      indexKb        = -1;
		public long      modifiedRows   = -1; // changed since the statistics were updated
		public String    rowFormat;
		public String    engine;
		public String    createTime;
		public String    updateTime;
		public long      rowsReadNoIndex = -1; // since the server started, when the table was looked up

		public MyActivity diff = new MyActivity();
		public MyActivity abs  = new MyActivity();

		public List<MyIndexInfo> indexes = new ArrayList<>();

		public boolean isTable()        { return "TABLE".equals(type); }
		public boolean hasRealDdl()     { return objectText != null && objectText.startsWith("CREATE"); }
		public boolean hasPrimaryKey()  { for (MyIndexInfo ii : indexes) { if ("PRIMARY KEY".equals(ii.type)) return true; } return false; }
		public String  getFullName()    { return schema + "." + name; }

		@Override public int     hashCode()         { return Objects.hash(schema, name); }
		@Override public boolean equals(Object obj) { return obj instanceof MyTableInfo && Objects.equals(schema, ((MyTableInfo) obj).schema) && Objects.equals(name, ((MyTableInfo) obj).name); }
	}

	//--------------------------------------------------------------------------------------------
	// For the report sections
	//--------------------------------------------------------------------------------------------
	@Override
	public String getDbmsTableInfoAsHtmlTable(DbxConnection conn, String currentDbname, Set<String> tableList, boolean includeIndexInfo, String classname)
	{
		Set<MyTableInfo> tableInfoSet = getTableInformationFromMonDdlStorage(conn, currentDbname, tableList);
		if (tableInfoSet.isEmpty())
			return "";

		return getTableInfoAsHtmlTable(tableInfoSet, tableList, includeIndexInfo, classname);
	}

	//--------------------------------------------------------------------------------------------
	// Without a report (a servlet): the same lookup and rendering, as the other DBMS vendors has it
	//--------------------------------------------------------------------------------------------

	/** An instance just to reach the methods (the lookup and rendering does not use the reporting instance, so null is fine) */
	private static MySqlAbstract createInstance()
	{
		return new MySqlAbstract(null)
		{
			@Override public boolean hasIssueToReport()       { return false; }
			@Override public String  getSubject()             { return ""; }
			@Override public boolean hasMinimalMessageText()  { return false; }
			@Override public boolean hasShortMessageText()    { return false; }
			@Override public void    writeMessageText(Writer w, IReportEntry.MessageType t) {}
			@Override public void    create(DbxConnection c, String s, Configuration p, Configuration l) {}
		};
	}

	/** Table / index information as HTML. A name in 'tableList' is 'table' (in schema 'dbname') or 'schema.table' */
	public static String getTableInfoHtml(DbxConnection conn, String dbname, Set<String> tableList, boolean includeIndexInfo, String classname)
	{
		return createInstance().getDbmsTableInfoAsHtmlTable(conn, dbname, tableList, includeIndexInfo, classname);
	}

	/** Table / index information as plain text: what the LLM is told about the tables */
	public static String getTableInfoPlainText(DbxConnection conn, String dbname, Set<String> tableList)
	{
		MySqlAbstract inst = createInstance();
		return inst.getTableInfoAsPlainText( inst.getTableInformationFromMonDdlStorage(conn, dbname, tableList) );
	}

	//--------------------------------------------------------------------------------------------
	// Read from the DDL Storage
	//--------------------------------------------------------------------------------------------

	/**
	 * Get what the DDL Storage has about the names in 'tableList'.
	 * <p>
	 * A name is <code>schema.table</code>, or just <code>table</code>: then it is in schema 'dbname' (the default schema of the statement),
	 * and when 'dbname' is empty it can be found in any schema. Backticks are removed. MySQL's own tables are skipped.
	 */
	public Set<MyTableInfo> getTableInformationFromMonDdlStorage(DbxConnection conn, String dbname, Set<String> tableList)
	{
		Set<MyTableInfo> result = new LinkedHashSet<>();

		if (tableList == null || tableList.isEmpty() || ! DbUtils.checkIfTableExistsNoThrow(conn, null, null, "MonDdlStorage"))
			return result;

		for (String tableName : tableList)
		{
			if (StringUtil.isNullOrBlank(tableName))
				continue;

			String[] schemaAndTable = ObjectLookupInspectorMySql.splitSchemaAndTable(StringUtil.nullToValue(dbname, ""), tableName);
			if (ObjectLookupInspectorMySql.isSystemObject(schemaAndTable[0], schemaAndTable[1]))
				continue;

			// The name as it was written in the statement: the case can differ from the table's (lower_case_table_names)
			String and_schema = StringUtil.isNullOrBlank(schemaAndTable[0]) ? "" : "  and upper([dbname]) = " + DbUtils.safeStr(schemaAndTable[0].toUpperCase()) + " \n";
			String sql = ""
				+ "select [dbname], [objectName], [type], [sampleTime], [objectText], [extraInfoText], [dependsText] \n"
				+ "from [MonDdlStorage] \n"
				+ "where [type] in ('TABLE', 'VIEW') \n"
				+ "  and upper([objectName]) = " + DbUtils.safeStr(schemaAndTable[1].toUpperCase()) + " \n"
				+ and_schema;

			try (Statement stmnt = conn.createStatement(); ResultSet rs = stmnt.executeQuery(conn.quotifySqlString(sql)))
			{
				while (rs.next())
				{
					MyTableInfo ti = new MyTableInfo();
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
	 * 'extraInfoText' of a TABLE is two text tables (see ObjectLookupInspectorMySql): the table and its indexes.<br>
	 * All values are text: numbers are plain digits (not formatted), sizes are in KB, (NULL) = not known.
	 */
	private void parseTableText(MyTableInfo ti)
	{
		if (StringUtil.isNullOrBlank(ti.extraInfoText))
			return;

		List<ResultSetTableModel> rstmList = ResultSetTableModel.parseTextTables(ti.extraInfoText);

		ResultSetTableModel table   = ResultSetTableModel.getTableWithColumnNames(rstmList, false, "SCHEMA_NAME", "TABLE_NAME", "ROWS_ESTIMATE");
		ResultSetTableModel indexes = ResultSetTableModel.getTableWithColumnNames(rstmList, false, "INDEX_NAME", "INDEX_TYPE", "INDEX_KEYS");

		if (table != null && table.getRowCount() > 0)
		{
			ti.partitions      = toLong(table.getValueAsString(0, "PARTITIONS"));
			ti.rowsEstimate    = toLong(table.getValueAsString(0, "ROWS_ESTIMATE"));
			ti.fileKb          = toLong(table.getValueAsString(0, "FILE_KB"));
			ti.dataKb          = toLong(table.getValueAsString(0, "DATA_KB"));
			ti.indexKb         = toLong(table.getValueAsString(0, "INDEX_KB"));
			ti.modifiedRows    = toLong(table.getValueAsString(0, "MODIFIED_ROWS"));
			ti.rowFormat       = nullIfNullText(table.getValueAsString(0, "ROW_FORMAT"));
			ti.engine          = nullIfNullText(table.getValueAsString(0, "ENGINE"));
			ti.createTime      = nullIfNullText(table.getValueAsString(0, "CREATE_TIME"));
			ti.updateTime      = nullIfNullText(table.getValueAsString(0, "UPDATE_TIME"));
			ti.rowsReadNoIndex = toLong(table.getValueAsString(0, "ROWS_READ_NO_INDEX"));
		}

		if (indexes != null)
		{
			for (int r = 0; r < indexes.getRowCount(); r++)
			{
				MyIndexInfo ii = new MyIndexInfo();
				ii.name             =                indexes.getValueAsString(r, "INDEX_NAME");
				ii.type             = nullIfNullText(indexes.getValueAsString(r, "INDEX_TYPE"));
				ii.keys             = nullIfNullText(indexes.getValueAsString(r, "INDEX_KEYS"));
				ii.cardinality      = toLong(        indexes.getValueAsString(r, "CARDINALITY"));
				ii.rowsReadAtLookup = toLong(        indexes.getValueAsString(r, "ROWS_READ"));
				ti.indexes.add(ii);
			}
		}
	}

	/**
	 * What was done to the table and its indexes: during the recording = the sum of <code>CmSysTableStats_diff</code> / <code>CmSysIndexStats_diff</code>,
	 * and since the server was started = the last sample of the <code>_abs</code> tables. (as the other DBMS vendors has it: "diff &lt;- abs")
	 * <p>
	 * Nothing is set when the CM was not recorded, or when the table was not among the most active ones (the collector only records those).<br>
	 * It is the whole recording, not the report period: this is also used without a report.
	 */
	private void readActivity(DbxConnection conn, MyTableInfo ti)
	{
		String where = ""
			+ "where [table_schema] = " + DbUtils.safeStr(ti.schema) + " \n"
			+ "  and [table_name]   = " + DbUtils.safeStr(ti.name)   + " \n";
		String and_notNewRow = "  and " + conn.toBitAnd("[CmRowState]", CountersModel.ROW_STATE__IS_DIFF_OR_RATE_ROW) + " = 0 \n";

		// {table, diff or abs, the SQL}. The first time a table is seen, the diff is "since the server started": those rows are not counted
		String tabCols = "sum([rows_fetched]), sum([rows_inserted]), sum([rows_updated]), sum([rows_deleted]), sum([total_latency]) / 1000000000.0";
		String idxCols = "sum([rows_selected]), sum([rows_inserted]), sum([rows_updated]), sum([rows_deleted]), sum([select_latency]) / 1000000000.0";
		String[][] queries = new String[][] {
			{"CmSysTableStats", "diff", "select '' as [index_name], " + tabCols + " \nfrom [CmSysTableStats_diff] \n" + where + and_notNewRow},
			{"CmSysTableStats", "abs" , "select '' as [index_name], " + tabCols + " \nfrom [CmSysTableStats_abs] \n"  + where + "  and [SessionSampleTime] = (select max([SessionSampleTime]) from [CmSysTableStats_abs]) \n"},
			{"CmSysIndexStats", "diff", "select [index_name], " + idxCols + " \nfrom [CmSysIndexStats_diff] \n" + where + and_notNewRow + "group by [index_name]"},
			{"CmSysIndexStats", "abs" , "select [index_name], " + idxCols + " \nfrom [CmSysIndexStats_abs] \n"  + where + "  and [SessionSampleTime] = (select max([SessionSampleTime]) from [CmSysIndexStats_abs]) \ngroup by [index_name]"},
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

						MyActivity activity = null;
						if (StringUtil.isNullOrBlank(indexName))
							activity = isDiff ? ti.diff : ti.abs;

						for (MyIndexInfo ii : ti.indexes)
						{
							if (StringUtil.hasValue(indexName) && indexName.equals(ii.name))
								activity = isDiff ? ii.diff : ii.abs;
						}

						if (activity == null)
							continue;

						activity.rowsRead     = rs.getLong(2);
						activity.rowsInserted = rs.getLong(3);
						activity.rowsUpdated  = rs.getLong(4);
						activity.rowsDeleted  = rs.getLong(5);
						activity.latencyMs    = rs.getLong(6);
					}
				}
			}
			catch (SQLException ex)
			{
				_logger.warn("Problems getting the activity (" + query[0] + "_" + query[1] + ") of table '" + ti.getFullName() + "'. Skipping and continuing. Caught: " + ex);
			}
		}
	}

	/** @return -1 when there is no value (the text table has "(NULL)" for a NULL: for example a table whose statistics are not loaded) */
	private static long toLong(String str)
	{
		if (StringUtil.isNullOrBlank(str))
			return -1;

		try { return Long.parseLong(str.trim()); }
		catch (NumberFormatException ex) { return -1; }
	}

	private static String nullIfNullText(String str)
	{
		return str == null || "(NULL)".equals(str) ? null : str;
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

	private static final String DIFF_ABS_TOOLTIP = "\n\nTwo values: [during the recording] and [since the MySQL server was started].\nEmpty = not recorded (the collector only records the most active tables and indexes, see CmSysTableStats and CmSysIndexStats)";

	/**
	 * Plain text, one block per table / view: what the LLM is told about the tables of a statement
	 */
	public String getTableInfoAsPlainText(Set<MyTableInfo> tableInfoSet)
	{
		if (tableInfoSet == null || tableInfoSet.isEmpty())
			return "";

		StringBuilder sb = new StringBuilder();

		for (MyTableInfo ti : tableInfoSet)
		{
			if ( ! ti.isTable() )
			{
				sb.append(ti.type).append(": ").append(ti.getFullName()).append(" (the text of the view is not known: the monitoring login has no SELECT on it)\n");
				sb.append("---\n");
				continue;
			}

			sb.append("Table: ").append(ti.getFullName())
			  .append(" (rows=").append(ti.rowsEstimate < 0 ? "unknown" : "about " + ti.rowsEstimate)
			  .append(", dataMB=").append(ti.dataKb < 0 ? "unknown" : "" + Math.round(ti.dataKb / 1024.0))
			  .append(", indexMB=").append(ti.indexKb < 0 ? "unknown" : "" + Math.round(ti.indexKb / 1024.0))
			  .append(", fileMB=").append(ti.fileKb < 0 ? "unknown" : "" + Math.round(ti.fileKb / 1024.0));

			if (StringUtil.hasValue(ti.engine))
				sb.append(", engine=").append(ti.engine);
			if (ti.partitions > 1)
				sb.append(", partitions=").append(ti.partitions);
			if ( ! ti.hasPrimaryKey() )
				sb.append(", NO PRIMARY KEY");
			if (ti.modifiedRows > 0)
				sb.append(", rowsChangedSinceStatistics=").append(ti.modifiedRows);
			if (ti.rowsReadNoIndex >= 0)
				sb.append(", rowsReadWithoutAnIndexSinceServerStart=").append(ti.rowsReadNoIndex);

			sb.append(", indexCount=").append(ti.indexes.size()).append(")\n");

			// What was done to the table while it was monitored (only when it was among the most active tables)
			if (ti.diff.isRecorded())
			{
				sb.append("Table activity during the monitored period: rowsRead=").append(ti.diff.rowsRead)
				  .append(", rowsInserted=").append(ti.diff.rowsInserted)
				  .append(", rowsUpdated=").append(ti.diff.rowsUpdated)
				  .append(", rowsDeleted=").append(ti.diff.rowsDeleted)
				  .append(", tableIoTimeMs=").append(ti.diff.latencyMs)
				  .append("\n");
			}

			sb.append(ti.hasRealDdl() ? "DDL:\n" : "Columns (from the InnoDB data dictionary, the data types are approximate):\n").append(StringUtil.nullToValue(ti.objectText, "-not-found-")).append("\n");

			for (MyIndexInfo ii : ti.indexes)
			{
				sb.append("Index: ").append(ii.name)
				  .append(", keys=(").append(StringUtil.nullToValue(ii.keys, "unknown")).append(")")
				  .append(", ").append(ii.type);
				if (ii.cardinality >= 0)
					sb.append(", cardinality=").append(ii.cardinality);
				if (ii.rowsReadAtLookup >= 0)
					sb.append(", rowsReadSinceServerStart=").append(ii.rowsReadAtLookup);
				if (ii.diff.isRecorded())
					sb.append(", rowsReadDuringTheMonitoredPeriod=").append(ii.diff.rowsRead);
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
	public String getTableInfoAsHtmlTable(Set<MyTableInfo> tableInfoSet, Set<String> tableList, boolean includeIndexInfo, String classname)
	{
		if (tableInfoSet == null || tableInfoSet.isEmpty())
			return "";

		StringBuilder sb = new StringBuilder();

		// What was asked for, but is not in the DDL Storage
		if (tableList != null)
		{
			for (String tabName : tableList)
			{
				String[] schemaAndTable = ObjectLookupInspectorMySql.splitSchemaAndTable("", tabName);
				if (ObjectLookupInspectorMySql.isSystemObject(schemaAndTable[0], schemaAndTable[1]))
					continue;

				boolean exists = false;
				for (MyTableInfo ti : tableInfoSet)
				{
					if (schemaAndTable[1].equalsIgnoreCase(ti.name))
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
		for (MyTableInfo ti : tableInfoSet)
		{
			LinkedHashMap<String, String> map = new LinkedHashMap<>();
			String indexInfo = "";

			if ( ! ti.isTable() )
			{
				map.put("Type"   , "<b>" + ti.type + "</b>");
				map.put("Schema" , html(ti.schema));
				map.put("Name"   , html(ti.name));
				map.put("Sampled", ti.sampleTime + "");
				map.put("Text"   , label("<i>not known</i>", "MySQL shows the text of a view only to a login with SELECT on it, and the monitoring login has no access to the data."));
			}
			else
			{
				map.put("Schema"     , html(ti.schema));
				map.put("Table"      , html(ti.name) + (ti.partitions > 1 ? " (" + ti.partitions + " partitions)" : ""));
				if (StringUtil.hasValue(ti.engine))
					map.put("Engine" , html(ti.engine));
				map.put(label("Rowcount", "An estimate, from the InnoDB statistics (unknown for a partitioned table, or a table that has not been used since the server started)"),
						ti.rowsEstimate < 0 ? "<i>unknown</i>" : num(ti.rowsEstimate));
				map.put("Data MB"    , mb(ti.dataKb));
				map.put("Index MB"   , mb(ti.indexKb));
				map.put(label("File MB", "The size of the table's file(s) on disk (all partitions)"), mb(ti.fileKb));
				if (StringUtil.hasValue(ti.rowFormat))
					map.put("Row Format", html(ti.rowFormat));

				// How much that has changed since the statistics were updated (InnoDB updates them after 10 percent, when innodb_stats_auto_recalc is ON)
				if (ti.modifiedRows > 0)
				{
					map.put(label("Changed since Stats", "Rows changed since InnoDB updated the statistics of the table (INNODB_TABLESTATS.MODIFIED_COUNTER)."),
							num(ti.modifiedRows) + " rows" + (ti.rowsEstimate > 0 ? " = " + NumberFormat.getInstance().format(Math.round(ti.modifiedRows * 1000.0 / ti.rowsEstimate) / 10.0) + " %" : ""));
				}

				// Rows read without an index, since the server started: a full scan (or a scan of the whole clustered index)
				if (ti.rowsReadNoIndex > 0)
				{
					map.put(label("Rows Read No Index", "Rows read WITHOUT an index since the server was started, when the table was looked up (performance_schema.table_io_waits_summary_by_index_usage, the row with INDEX_NAME NULL).\nMany = full table scans."),
							num(ti.rowsReadNoIndex));
				}

				if (StringUtil.hasValue(ti.createTime))
					map.put("Created", html(ti.createTime));
				if (StringUtil.hasValue(ti.updateTime))
					map.put(label("Last Changed", "When the table was last changed (information_schema.TABLES.UPDATE_TIME): NULL after a restart, until the next change"), html(ti.updateTime));
				map.put("Sampled", ti.sampleTime + "");

				// What was done to the table: during the recording, and since the server was started
				if (ti.diff.isRecorded() || ti.abs.isRecorded())
				{
					map.put(label("Rows Read"    , "Rows read from the table (rows_fetched)."    + DIFF_ABS_TOOLTIP), diffAbs(ti.diff.rowsRead    , ti.abs.rowsRead));
					map.put(label("Rows Inserted", "Rows inserted into the table."               + DIFF_ABS_TOOLTIP), diffAbs(ti.diff.rowsInserted, ti.abs.rowsInserted));
					map.put(label("Rows Updated" , "Rows updated in the table."                  + DIFF_ABS_TOOLTIP), diffAbs(ti.diff.rowsUpdated , ti.abs.rowsUpdated));
					map.put(label("Rows Deleted" , "Rows deleted from the table."                + DIFF_ABS_TOOLTIP), diffAbs(ti.diff.rowsDeleted , ti.abs.rowsDeleted));
					map.put(label("Table I/O ms" , "Time of the table's I/O calls: every row read or written through the storage engine (total_latency), in milliseconds.\nIt includes the waits for row locks." + DIFF_ABS_TOOLTIP), diffAbs(ti.diff.latencyMs, ti.abs.latencyMs));
				}

				map.put("Index Count", ti.indexes.size() + (ti.hasPrimaryKey() || ti.indexes.isEmpty() ? "" : " <b><font color='red'>&lt;&lt;-- Warning NO primary key</font></b>"));
				map.put(ti.hasRealDdl() ? "DDL" : "Columns", getTextAsTooltipDiv(ti.objectText, ti.hasRealDdl() ? "Table DDL" : "Columns (from the InnoDB data dictionary)", getDdlMaxLengthTable()));
				if (StringUtil.hasValue(ti.dependsText))
					map.put("Triggers", getTextAsTooltipDiv(ti.dependsText, "Triggers", getDdlMaxLengthTrigger()));

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

	private String getIndexInfoAsHtmlTable(MyTableInfo ti, String classname)
	{
		if (ti.indexes.isEmpty())
			return "<b>No Index(es) was found</b>" + (ti.partitions > 1 ? " <i>(a partitioned table: the index names are only known once the table has been used since the server started)</i>" : "") + "<br> \n";

		String className = StringUtil.hasValue(classname) ? " class='" + classname + "'" : "";

		// Columns that are only there when we have something to put in them
		boolean hasCardinality = false; // only with REFERENCES
		boolean hasActivity    = false; // CmSysIndexStats has recorded the table's indexes
		for (MyIndexInfo ii : ti.indexes)
		{
			if (ii.cardinality >= 0)
				hasCardinality = true;
			if (ii.diff.isRecorded() || ii.abs.isRecorded())
				hasActivity = true;
		}

		StringBuilder sb = new StringBuilder();
		sb.append("<table" + className + "> \n");
		sb.append("<thead> \n");
		sb.append("<tr> \n");
		sb.append(createHtmlTh("Index Name"     , "GEN_CLUST_INDEX = the table has no primary key: InnoDB made a hidden one"));
		sb.append(createHtmlTh("Keys"           , "The columns of the index, in order. Empty = not known (a partitioned table, and the login has no REFERENCES)"));
		sb.append(createHtmlTh("Type"           , "PRIMARY KEY / UNIQUE / INDEX / FULLTEXT / SPATIAL"));
		if (hasCardinality)
			sb.append(createHtmlTh("Cardinality", "An estimate of how many different values the index has (information_schema.STATISTICS). Close to the Rowcount = selective"));
		// The rows read using the index: from the recording (CmSysIndexStats) when it is there, otherwise from when the table was looked up
		// (MySQL counts inserts, updates and deletes on the table, not per index: so only the reads are shown)
		String unusedTooltip = "\n0 on an index that is not unique = it only costs (space, and time at every INSERT, UPDATE and DELETE).";
		if (hasActivity)
			sb.append(createHtmlTh("Rows Read", "(diff <- abs) Rows read using the index (rows_selected)." + DIFF_ABS_TOOLTIP + unusedTooltip));
		else
			sb.append(createHtmlTh("Rows Read Since Start", "Rows read using the index since the server was started, when the table was looked up (performance_schema)." + unusedTooltip));
		sb.append("</tr> \n");
		sb.append("</thead> \n");

		sb.append("<tbody> \n");
		for (MyIndexInfo ii : ti.indexes)
		{
			// Not used since the server started: red when the index is not there to stop duplicates
			long    sinceStart = ii.abs.isRecorded() ? ii.abs.rowsRead : ii.rowsReadAtLookup;
			boolean neverUsed  = sinceStart == 0 && ii.diff.rowsRead <= 0;
			String  unused     = ii.isUniqueOrPk() ? "0" : "<b><font color='red'>0</font></b>";

			sb.append("<tr> \n");
			sb.append("  <td>").append( "GEN_CLUST_INDEX".equals(ii.name) ? "<b><font color='red'>GEN_CLUST_INDEX</font></b>" : html(ii.name) ).append("</td> \n");
			sb.append("  <td>").append( html(ii.keys) ).append("</td> \n");
			sb.append("  <td>").append( html(ii.type) ).append("</td> \n");
			if (hasCardinality)
				sb.append("  <td>").append( num(ii.cardinality) ).append("</td> \n");
			if (hasActivity)
				sb.append("  <td>").append( neverUsed ? "<b>0</b> &#9887; " + unused : diffAbs(ii.diff.rowsRead, ii.abs.rowsRead) ).append("</td> \n");
			else
				sb.append("  <td>").append( neverUsed ? unused : num(ii.rowsReadAtLookup) ).append("</td> \n");
			sb.append("</tr> \n");
		}
		sb.append("</tbody> \n");
		sb.append("</table> \n");

		return sb.toString();
	}
}
