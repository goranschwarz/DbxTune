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
package com.dbxtune.cm.db2;

import java.lang.invoke.MethodHandles;
import java.sql.CallableStatement;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.sql.Types;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.dbxtune.sql.conn.DbxConnection;
import com.dbxtune.utils.StringUtil;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * Execution plans of Db2 statements that are in the package cache, without access to the data:
 * <code>EXPLAIN_FROM_SECTION</code> writes the plan of a section into the explain tables, it is read from there into JSON, and the rows are deleted again.
 * <p>
 * The login needs EXECUTE on SYSPROC.EXPLAIN_FROM_SECTION, and the explain tables in its own schema (or in SYSTOOLS).
 * DbxTune never creates the explain tables (see DbmsSetup/db2.txt).
 * <p>
 * The JSON is what is stored (column ExecPlan) and what the plan viewer in DbxCentral draws. {@link #planJsonToText(String)} makes a text tree of it
 * (like db2exfmt), for tooltips, the Daily Summary Report and the LLM.
 */
public class Db2ExplainUtils
{
	private static final Logger _logger = LogManager.getLogger(MethodHandles.lookup().lookupClass());

	/** The format of the JSON (in case it is changed later) */
	public static final String PLAN_FORMAT = "db2-explain-tables-1";

	/** Hex string (as HEX(EXECUTABLE_ID) returns it) to bytes */
	public static byte[] hexToBytes(String hex)
	{
		if (hex == null)
			return null;
		hex = hex.trim();
		if (hex.startsWith("x'") || hex.startsWith("X'"))
			hex = hex.substring(2, hex.length() - 1);
		byte[] bytes = new byte[hex.length() / 2];
		for (int i = 0; i < bytes.length; i++)
			bytes[i] = (byte) Integer.parseInt(hex.substring(i * 2, i * 2 + 2), 16);
		return bytes;
	}

	/**
	 * Explain a section that is in the package cache, and return the plan as JSON.
	 *
	 * @param conn            The monitoring connection
	 * @param explainSchema   Where the explain tables are (the login's own schema, or SYSTOOLS)
	 * @param executableIdHex The EXECUTABLE_ID of the statement, as a hex string
	 * @param member          The member where the section is (0 if this is not pureScale or DPF)
	 * @param queryTimeoutSec Query timeout for each statement
	 * @return The plan as JSON
	 * @throws SQLException For example SQLCODE -20501 (the section is no longer in the package cache), -551 (no EXECUTE on EXPLAIN_FROM_SECTION), -204 (no explain tables)
	 */
	public static String getExecPlanJson(DbxConnection conn, String explainSchema, String executableIdHex, int member, int queryTimeoutSec)
	throws SQLException
	{
		String    requester = null;
		Timestamp explTime  = null;
		String    srcName   = null;
		String    srcSchema = null;
		String    srcVer    = null;

		// Explain it (writes rows into the explain tables)
		try (CallableStatement cs = conn.prepareCall("CALL SYSPROC.EXPLAIN_FROM_SECTION(?, 'M', NULL, ?, ?, ?, ?, ?, ?, ?)"))
		{
			cs.setQueryTimeout(queryTimeoutSec);
			cs.setBytes (1, hexToBytes(executableIdHex));
			cs.setInt   (2, member);
			cs.setString(3, explainSchema);
			cs.registerOutParameter(4, Types.VARCHAR);
			cs.registerOutParameter(5, Types.TIMESTAMP);
			cs.registerOutParameter(6, Types.VARCHAR);
			cs.registerOutParameter(7, Types.VARCHAR);
			cs.registerOutParameter(8, Types.VARCHAR);
			cs.execute();

			requester = cs.getString   (4);
			explTime  = cs.getTimestamp(5);
			srcName   = cs.getString   (6);
			srcSchema = cs.getString   (7);
			srcVer    = cs.getString   (8);
		}

		String schema = explainSchema.trim();
		String keyWhere = " where EXPLAIN_REQUESTER = ? and EXPLAIN_TIME = ? and SOURCE_NAME = ? and SOURCE_SCHEMA = ? and SOURCE_VERSION = ?";
		Object[] key = { requester, explTime, srcName, srcSchema, srcVer };

		Map<String, Object> plan = new LinkedHashMap<>();
		plan.put("planFormat",   PLAN_FORMAT);
		plan.put("source",       "EXPLAIN_FROM_SECTION");
		plan.put("executableId", executableIdHex);
		plan.put("explainTime",  explTime == null ? null : explTime.toString());

		try
		{
			// Instance: version, optimization level, isolation
			queryRows(conn, queryTimeoutSec, "select DB2_VERSION, QUERYOPT, ISOLATION from " + schema + ".EXPLAIN_INSTANCE" + keyWhere, key, rs -> {
				plan.put("db2Version", trim(rs.getString(1)));
				plan.put("queryOpt",   rs.getInt(2));
				plan.put("isolation",  trim(rs.getString(3)));
			});

			// Statement: the text and the total cost
			// EXPLAIN_LEVEL: 'O' = the original text, 'P' = the plan of an EXPLAIN statement, 'S' = the plan of a section (EXPLAIN_FROM_SECTION). The other tables only have the plan level.
			queryRows(conn, queryTimeoutSec, "select EXPLAIN_LEVEL, STATEMENT_TYPE, TOTAL_COST, CAST(STATEMENT_TEXT AS VARCHAR(32000)) from " + schema + ".EXPLAIN_STATEMENT" + keyWhere + " order by EXPLAIN_LEVEL desc", key, rs -> {
				if ( ! plan.containsKey("statementText") || "O".equals(trim(rs.getString(1))) )
					plan.put("statementText", trim(rs.getString(4)));
				if ( ! "O".equals(trim(rs.getString(1))) )
				{
					plan.put("statementType", trim(rs.getString(2)));
					plan.put("totalCost",     rs.getDouble(3));
				}
			});

			// Operators
			Map<Integer, Map<String, Object>> operators = new LinkedHashMap<>();
			queryRows(conn, queryTimeoutSec, "select OPERATOR_ID, OPERATOR_TYPE, TOTAL_COST, IO_COST, CPU_COST, FIRST_ROW_COST, BUFFERS from " + schema + ".EXPLAIN_OPERATOR" + keyWhere + " and EXPLAIN_LEVEL <> 'O' order by OPERATOR_ID", key, rs -> {
				Map<String, Object> op = new LinkedHashMap<>();
				op.put("id",           rs.getInt(1));
				op.put("type",         trim(rs.getString(2)));
				op.put("totalCost",    rs.getDouble(3));
				op.put("ioCost",       rs.getDouble(4));
				op.put("cpuCost",      rs.getDouble(5));
				op.put("firstRowCost", rs.getDouble(6));
				op.put("buffers",      rs.getDouble(7));
				op.put("args",         new ArrayList<Map<String, Object>>());
				op.put("predicates",   new ArrayList<Map<String, Object>>());
				operators.put(rs.getInt(1), op);
			});

			// Operator arguments (for example the join type, the sort keys, the scan direction)
			queryRows(conn, queryTimeoutSec, "select OPERATOR_ID, ARGUMENT_TYPE, ARGUMENT_VALUE, CAST(LONG_ARGUMENT_VALUE AS VARCHAR(4000)) from " + schema + ".EXPLAIN_ARGUMENT" + keyWhere + " and EXPLAIN_LEVEL <> 'O' order by OPERATOR_ID, ARGUMENT_TYPE", key, rs -> {
				Map<String, Object> op = operators.get(rs.getInt(1));
				if (op == null)
					return;
				Map<String, Object> arg = new LinkedHashMap<>();
				arg.put("type",  trim(rs.getString(2)));
				arg.put("value", trim(rs.getString(4) != null ? rs.getString(4) : rs.getString(3)));
				listOf(op, "args").add(arg);
			});

			// Predicates
			queryRows(conn, queryTimeoutSec, "select OPERATOR_ID, PREDICATE_ID, HOW_APPLIED, WHEN_EVALUATED, FILTER_FACTOR, CAST(PREDICATE_TEXT AS VARCHAR(4000)) from " + schema + ".EXPLAIN_PREDICATE" + keyWhere + " and EXPLAIN_LEVEL <> 'O' order by OPERATOR_ID, PREDICATE_ID", key, rs -> {
				Map<String, Object> op = operators.get(rs.getInt(1));
				if (op == null)
					return;
				Map<String, Object> pred = new LinkedHashMap<>();
				pred.put("id",            rs.getInt(2));
				pred.put("howApplied",    trim(rs.getString(3)));
				pred.put("whenEvaluated", trim(rs.getString(4)));
				pred.put("filterFactor",  rs.getDouble(5));
				pred.put("text",          trim(rs.getString(6)));
				listOf(op, "predicates").add(pred);
			});
			plan.put("operators", new ArrayList<>(operators.values()));

			// Streams: what flows from an operator (or a table/index) into another operator, and the estimated number of rows
			List<Map<String, Object>> streams = new ArrayList<>();
			queryRows(conn, queryTimeoutSec, "select STREAM_ID, SOURCE_TYPE, SOURCE_ID, TARGET_TYPE, TARGET_ID, OBJECT_SCHEMA, OBJECT_NAME, STREAM_COUNT, COLUMN_COUNT, CAST(COLUMN_NAMES AS VARCHAR(4000)) from " + schema + ".EXPLAIN_STREAM" + keyWhere + " and EXPLAIN_LEVEL <> 'O' order by STREAM_ID", key, rs -> {
				Map<String, Object> s = new LinkedHashMap<>();
				s.put("id",           rs.getInt(1));
				s.put("sourceType",   trim(rs.getString(2)));
				s.put("sourceId",     rs.getInt(3));
				s.put("targetType",   trim(rs.getString(4)));
				s.put("targetId",     rs.getInt(5));
				s.put("objectSchema", trim(rs.getString(6)));
				s.put("objectName",   trim(rs.getString(7)));
				s.put("rows",         rs.getDouble(8));
				s.put("columnCount",  rs.getInt(9));
				s.put("columns",      trim(rs.getString(10)));
				streams.add(s);
			});
			plan.put("streams", streams);

			// Objects: the tables and indexes the plan uses, with the statistics the optimizer had
			// (a table that is used twice, for example in a self join, has two rows: it is only added once)
			List<Map<String, Object>> objects = new ArrayList<>();
			Set<String> objectNames = new HashSet<>();
			queryRows(conn, queryTimeoutSec, "select OBJECT_SCHEMA, OBJECT_NAME, OBJECT_TYPE, ROW_COUNT, PAGES, STATISTICS_TIME, NLEAF, NLEVELS, FULLKEYCARD, TABLESPACE_NAME from " + schema + ".EXPLAIN_OBJECT" + keyWhere + " and EXPLAIN_LEVEL <> 'O' order by OBJECT_SCHEMA, OBJECT_NAME", key, rs -> {
				if ( ! objectNames.add(trim(rs.getString(1)) + "." + trim(rs.getString(2))) )
					return;
				Map<String, Object> o = new LinkedHashMap<>();
				o.put("schema",      trim(rs.getString(1)));
				o.put("name",        trim(rs.getString(2)));
				o.put("type",        trim(rs.getString(3)));
				o.put("rows",        rs.getLong(4));
				o.put("pages",       rs.getLong(5));
				o.put("statsTime",   rs.getTimestamp(6) == null ? null : rs.getTimestamp(6).toString());
				o.put("nleaf",       rs.getLong(7));
				o.put("nlevels",     rs.getInt(8));
				o.put("fullKeyCard", rs.getLong(9));
				o.put("tablespace",  trim(rs.getString(10)));
				objects.add(o);
			});
			plan.put("objects", objects);
		}
		finally
		{
			// Remove our own rows (the other explain tables cascade from EXPLAIN_INSTANCE). A failure here is not a reason to lose the plan
			try (PreparedStatement ps = conn.prepareStatement("delete from " + schema + ".EXPLAIN_INSTANCE" + keyWhere))
			{
				ps.setQueryTimeout(queryTimeoutSec);
				for (int i = 0; i < key.length; i++)
					ps.setObject(i + 1, key[i]);
				ps.executeUpdate();
			}
			catch (SQLException ex)
			{
				_logger.info("Problems deleting the explain rows from '" + schema + ".EXPLAIN_INSTANCE' (they stay in the table). Caught: " + ex);
			}
		}

		try
		{
			return new ObjectMapper().writeValueAsString(plan);
		}
		catch (Exception ex)
		{
			throw new SQLException("Problems making JSON of the execution plan: " + ex, ex);
		}
	}

	@SuppressWarnings("unchecked")
	private static List<Map<String, Object>> listOf(Map<String, Object> map, String key)
	{
		return (List<Map<String, Object>>) map.get(key);
	}

	private static String trim(String str)
	{
		return str == null ? null : str.trim();
	}

	private interface RowHandler
	{
		void handle(ResultSet rs) throws SQLException;
	}

	private static void queryRows(DbxConnection conn, int queryTimeoutSec, String sql, Object[] key, RowHandler handler)
	throws SQLException
	{
		try (PreparedStatement ps = conn.prepareStatement(sql))
		{
			ps.setQueryTimeout(queryTimeoutSec);
			for (int i = 0; i < key.length; i++)
				ps.setObject(i + 1, key[i]);
			try (ResultSet rs = ps.executeQuery())
			{
				while (rs.next())
					handler.handle(rs);
			}
		}
	}

	/**
	 * A text tree of the plan (like db2exfmt): one line per operator with the estimated rows and the cost, the tables and indexes it reads, and the predicates.
	 *
	 * @param json The JSON from {@link #getExecPlanJson}
	 * @return The text, or the JSON itself if it can't be parsed
	 */
	public static String planJsonToText(String json)
	{
		if (StringUtil.isNullOrBlank(json))
			return "";
		try
		{
			JsonNode plan = new ObjectMapper().readTree(json);

			Map<Integer, JsonNode> ops = new LinkedHashMap<>();
			for (JsonNode op : plan.path("operators"))
				ops.put(op.path("id").asInt(), op);

			// The tables and indexes, by name
			Map<String, JsonNode> objects = new LinkedHashMap<>();
			for (JsonNode o : plan.path("objects"))
				objects.put(o.path("schema").asText() + "." + o.path("name").asText(), o);

			StringBuilder sb = new StringBuilder();
			sb.append("Total Cost: ").append(fmt(plan.path("totalCost").asDouble())).append("   (estimated, in timerons)\n");
			if (plan.hasNonNull("queryOpt"))
				sb.append("Optimization level: ").append(plan.path("queryOpt").asInt()).append(", isolation: ").append(plan.path("isolation").asText()).append("\n");
			sb.append("\n");

			// The root: the operator that does not feed any other operator (normally RETURN, id 1)
			Set<Integer> feedsOther = new HashSet<>();
			for (JsonNode s : plan.path("streams"))
				if ("O".equals(s.path("sourceType").asText()) && "O".equals(s.path("targetType").asText()))
					feedsOther.add(s.path("sourceId").asInt());

			Set<Integer> visited = new HashSet<>();
			for (Integer id : ops.keySet())
			{
				if ( ! feedsOther.contains(id) )
					appendOperator(sb, plan, ops, objects, id, 0, visited);
			}
			return sb.toString();
		}
		catch (Exception ex)
		{
			_logger.debug("planJsonToText(): problems parsing the plan. Caught: " + ex);
			return json;
		}
	}

	private static void appendOperator(StringBuilder sb, JsonNode plan, Map<Integer, JsonNode> ops, Map<String, JsonNode> objects, int id, int level, Set<Integer> visited)
	{
		JsonNode op = ops.get(id);
		if (op == null)
			return;

		// A shared result (a TEMP that is read twice): its inputs are shown under the first reader only
		if ( ! visited.add(id) )
		{
			sb.append("  ".repeat(level)).append(op.path("type").asText()).append(" (").append(id).append(")   (a shared result: see above)\n");
			return;
		}

		// The rows that come out of this operator
		Double rows = null;
		for (JsonNode s : plan.path("streams"))
			if ("O".equals(s.path("sourceType").asText()) && s.path("sourceId").asInt() == id)
				rows = s.path("rows").asDouble();

		// NOTE: a section explain has no I/O cost (it is -1)
		String indent = "  ".repeat(level);
		sb.append(indent).append(op.path("type").asText()).append(" (").append(id).append(")")
		  .append("   rows: ").append(rows == null ? "?" : fmt(rows))
		  .append("   cost: ").append(fmt(op.path("totalCost").asDouble()))
		  .append(op.path("ioCost").asDouble() >= 0 ? "   io: " + fmt(op.path("ioCost").asDouble()) : "")
		  .append("\n");

		for (JsonNode pred : op.path("predicates"))
			sb.append(indent).append("      [").append(pred.path("howApplied").asText()).append("] ").append(pred.path("text").asText().replaceAll("\\s+", " ")).append("\n");

		// The inputs: other operators, or tables/indexes
		for (JsonNode s : plan.path("streams"))
		{
			if ( ! "O".equals(s.path("targetType").asText()) || s.path("targetId").asInt() != id )
				continue;

			if ("O".equals(s.path("sourceType").asText()))
			{
				appendOperator(sb, plan, ops, objects, s.path("sourceId").asInt(), level + 1, visited);
			}
			else
			{
				String   name = s.path("objectSchema").asText() + "." + s.path("objectName").asText();
				JsonNode obj  = objects.get(name);
				sb.append(indent).append("  ").append(name);
				if (obj != null)
				{
					sb.append("   (").append(obj.path("type").asText()).append(", ").append(obj.path("rows").asLong()).append(" rows");
					if (obj.path("statsTime").isNull() || obj.path("statsTime").asText().isEmpty())
						sb.append(", NO statistics");
					else
						sb.append(", statistics from ").append(obj.path("statsTime").asText());
					sb.append(")");
				}
				sb.append("\n");
			}
		}
	}

	private static String fmt(double val)
	{
		if (val == Math.rint(val) && Math.abs(val) < 1e15)
			return String.format(Locale.ENGLISH, "%,d", (long) val);
		return String.format(Locale.ENGLISH, "%,.1f", val);
	}
}
