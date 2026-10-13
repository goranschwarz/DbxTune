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
 * in the "DDL Storage" (table <code>MonDdlStorage</code>, written by <code>ObjectLookupInspectorOracle</code>).
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
	public static class OraIndexInfo
	{
		public String owner;
		public String name;
		public String type;             // NORMAL, BITMAP, FUNCTION-BASED NORMAL, LOB...
		public String uniqueness;       // UNIQUE, NONUNIQUE
		public String constraintType;   // P = it is the index of the primary key, U = of a unique constraint
		public String keys;             // the columns: "C1, C2 DESC"
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
	 * @return key: PLAN_HASH_VALUE, value: {the plan as text (DBMS_XPLAN), the tables of the plan as "OWNER.T1, OWNER.T2", the container the statement ran in}
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
				String tableList  = StringUtil.nullToValue(rs.getString(3), "");
				if (tableList.startsWith("TableList:"))
					tableList = tableList.substring("TableList:".length()).trim();

				plans.put(objectName.contains("_") ? objectName.substring(objectName.indexOf('_') + 1) : "", new String[] { rs.getString(2), tableList, rs.getString(4) });
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

		String select = "select [dbname], [owner], [objectName], [type], [sampleTime], [objectText], [extraInfoText] \nfrom [MonDdlStorage] \n";

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
				list.add(ti);
			}
		}

		return list;
	}

	/**
	 * 'extraInfoText' of a TABLE is three text tables (see ObjectLookupInspectorOracle): the table, its indexes and the size of every segment.<br>
	 * All values are text: numbers are plain digits (not formatted), sizes are in KB.
	 */
	private void parseTableText(OraTableInfo ti)
	{
		if (StringUtil.isNullOrBlank(ti.extraInfoText))
			return;

		List<ResultSetTableModel> rstmList = ResultSetTableModel.parseTextTables(ti.extraInfoText);

		ResultSetTableModel table    = ResultSetTableModel.getTableWithColumnNames(rstmList, false, "OWNER", "TABLE_NAME", "NUM_ROWS");
		ResultSetTableModel indexes  = ResultSetTableModel.getTableWithColumnNames(rstmList, false, "INDEX_OWNER", "INDEX_NAME", "INDEX_KEYS");
		ResultSetTableModel segments = ResultSetTableModel.getTableWithColumnNames(rstmList, false, "SEGMENT_OWNER", "SEGMENT_NAME", "SIZE_KB");

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

			sb.append(", indexCount=").append(ti.indexes.size()).append(")\n");

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
				  .append(", status=").append(ii.status)
				  .append("\n");
			}
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
				if ("ENABLED".equals(ti.compression))
					map.put("Compression", "ENABLED");
				if (StringUtil.hasValue(ti.degree) && ! "1".equals(ti.degree))
					map.put("Parallel Degree", html(ti.degree));
				map.put("Tablespace" , html(ti.tablespace));
				map.put("Sampled"    , ti.sampleTime + "");
				map.put("Index Count", ti.indexes.size() + (ti.indexes.size() > 0 ? "" : " <b><font color='red'>&lt;&lt;-- Warning NO index</font></b>"));
				map.put("Columns"    , getTextAsTooltipDiv(ti.objectText, "Columns and their statistics", getDdlMaxLengthTable()));

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

		StringBuilder sb = new StringBuilder();
		sb.append("<table" + className + "> \n");
		sb.append("<thead> \n");
		sb.append("<tr> \n");
		sb.append(createHtmlTh("Index Name"       , ""));
		sb.append(createHtmlTh("Keys"             , "The columns of the index, in order"));
		sb.append(createHtmlTh("Description"      , "PRIMARY KEY / UNIQUE / NONUNIQUE, and what kind of index it is when it is not a normal B-tree"));
		sb.append(createHtmlTh("RowCount"         , "NUM_ROWS: rows in the index, from the optimizer statistics"));
		sb.append(createHtmlTh("Distinct Keys"    , "DISTINCT_KEYS: how many different values the index has. Close to RowCount = selective. Few = many rows per value"));
		sb.append(createHtmlTh("Leaf Blocks"      , "LEAF_BLOCKS: blocks at the lowest level of the index"));
		sb.append(createHtmlTh("BLevel"           , "BLEVEL: levels above the leaf blocks. A lookup reads BLevel + 1 index blocks"));
		sb.append(createHtmlTh("Clustering Factor", "CLUSTERING_FACTOR: how well the order of the table rows follows the index.\nClose to the number of table BLOCKS = good (an index range scan reads few table blocks).\nClose to the number of ROWS = bad (every row is a new table block), then the optimizer often prefers a full table scan"));
		sb.append(createHtmlTh("Size MB"          , ""));
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
