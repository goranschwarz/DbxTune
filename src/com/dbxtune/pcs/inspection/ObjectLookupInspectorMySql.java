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
package com.dbxtune.pcs.inspection;

import java.lang.invoke.MethodHandles;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.dbxtune.gui.ResultSetTableModel;
import com.dbxtune.pcs.DdlDetails;
import com.dbxtune.pcs.ObjectLookupQueueEntry;
import com.dbxtune.pcs.PersistentCounterHandler;
import com.dbxtune.sql.conn.DbxConnection;
import com.dbxtune.utils.StringUtil;

/**
 * MySQL: what is stored in the "DDL Storage" (table <code>MonDdlStorage</code>) for the tables the collector has seen (built on ObjectLookupInspectorOracle).
 * <p>
 * dbname = the schema, objectName = the table (or <code>schema.table</code>, as it was written in a statement).
 * <p>
 * The monitoring login has no access to the tables (see DbmsSetup/mysql.txt), so everything is read from the data dictionary:
 * <ul>
 *   <li>Always (PROCESS + SELECT on performance_schema): the InnoDB data dictionary (INNODB_TABLES, INNODB_COLUMNS, INNODB_INDEXES, INNODB_FIELDS,
 *       INNODB_TABLESTATS, INNODB_TABLESPACES): the columns (the data types are approximate), the indexes, the sizes and the row estimate,
 *       and how the indexes are used (performance_schema.table_io_waits_summary_by_index_usage, also the rows read WITHOUT an index).</li>
 *   <li>With the optional <code>GRANT REFERENCES ON *.*</code>: SHOW CREATE TABLE (the real DDL), and information_schema (engine, times, index cardinality).
 *       REFERENCES does not give access to the rows.</li>
 * </ul>
 * No table is read to get any of this. The texts are stored as text tables (as the Oracle and the Postgres inspectors does), MySqlAbstract reads them back.
 * <p>
 * Execution plans are not looked up here: MySQL can only EXPLAIN a statement with its values, and with access to the tables.
 * The plans that CmActiveStatements captured are used instead (in the report and in DbxCentral).
 */
public class ObjectLookupInspectorMySql
extends ObjectLookupInspectorAbstract
{
	private static final Logger _logger = LogManager.getLogger(MethodHandles.lookup().lookupClass());

	/** Every statement of a lookup gets this many seconds */
	private static final int QUERY_TIMEOUT_SEC = 30;

	/** The schemas of MySQL itself: their tables are not looked up */
	private static final Set<String> SYSTEM_SCHEMAS = new LinkedHashSet<>(Arrays.asList("mysql", "sys", "performance_schema", "information_schema"));

	@Override
	public boolean allowInspection(ObjectLookupQueueEntry entry)
	{
		if (entry == null || entry._objectName == null)
			return false;

		// A statement: MySQL can't explain a normalized statement (see the class comment)
		if (PersistentCounterHandler.STATEMENT_CACHE_NAME.equals(entry._dbname))
			return false;

		String[] schemaAndTable = splitSchemaAndTable(entry._dbname, entry._objectName);
		return StringUtil.hasValue(schemaAndTable[0]) && ! isSystemObject(schemaAndTable[0], schemaAndTable[1]);
	}

	/**
	 * The names we do NOT look up: the tables of MySQL itself, DUAL, and names that are not tables (x$ views of sys, a derived table...).<br>
	 * Also used by the report: a name like that is not worth a "was not found in the DDL Storage".
	 */
	public static boolean isSystemObject(String schema, String table)
	{
		if (schema != null && SYSTEM_SCHEMAS.contains(schema.toLowerCase()))
			return true;

		return table == null || table.isEmpty() || table.equalsIgnoreCase("DUAL") || table.indexOf('$') >= 0 || table.indexOf('(') >= 0;
	}

	@Override
	public void onConnect(DbxConnection conn)
	{
	}

	@Override
	public List<DdlDetails> doObjectInfoLookup(DbxConnection conn, ObjectLookupQueueEntry qe, PersistentCounterHandler pch)
	{
		try
		{
			return lookupTable(conn, qe, pch);
		}
		catch (SQLException ex)
		{
			_logger.warn("DDL Lookup: Problems with dbname='" + qe._dbname + "', objectName='" + qe._objectName + "', source='" + qe._source + "'. Skipping this object. Caught: " + ex.getMessage().trim());
			return Collections.emptyList();
		}
	}

	//---------------------------------------------------------------------------------------
	// A table (or a view)
	//---------------------------------------------------------------------------------------
	private List<DdlDetails> lookupTable(DbxConnection conn, ObjectLookupQueueEntry qe, PersistentCounterHandler pch)
	throws SQLException
	{
		final String dbname           = qe._dbname;
		final String originObjectName = qe._objectName;

		String[] schemaAndTable = splitSchemaAndTable(dbname, originObjectName);

		//----------------------------------------------------------------------------------
		// Find it: an InnoDB table (the InnoDB dictionary is readable with PROCESS), otherwise information_schema.TABLES (only with REFERENCES)
		// The name as it was written, and if that finds nothing in lower case (lower_case_table_names, or a statement written in upper case)
		//----------------------------------------------------------------------------------
		String  schema      = null;
		String  table       = null;
		long    tableId     = -1;
		String  type        = null;
		boolean partitioned = false;

		for (int attempt = 0; attempt < 2 && type == null; attempt++)
		{
			String trySchema = attempt == 0 ? schemaAndTable[0] : schemaAndTable[0].toLowerCase();
			String tryTable  = attempt == 0 ? schemaAndTable[1] : schemaAndTable[1].toLowerCase();
			if (attempt == 1 && trySchema.equals(schemaAndTable[0]) && tryTable.equals(schemaAndTable[1]))
				break;

			// A partitioned table has one row per partition ('schema/table#p#p0'): the first one has the columns of all of them
			// (but INNODB_INDEXES and INNODB_TABLESTATS have no rows for partitions, checked on 8.0 and 9.7, see the extraInfoText)
			String innodbName = trySchema + "/" + tryTable;
			String sql = ""
				+ "select t.TABLE_ID, t.NAME \n"
				+ "from information_schema.INNODB_TABLES t \n"
				+ "where t.NAME = " + q(innodbName) + " \n"
				+ "   or LEFT(t.NAME, " + (innodbName.length() + 3) + ") = " + q(innodbName + "#p#") + " \n"
				+ "order by t.NAME \n"
				+ "limit 1";

			try (Statement stmnt = conn.createStatement())
			{
				stmnt.setQueryTimeout(QUERY_TIMEOUT_SEC);
				try (ResultSet rs = stmnt.executeQuery(sql))
				{
					while (rs.next())
					{
						tableId     = rs.getLong(1);
						partitioned = rs.getString(2).length() > innodbName.length();
						type        = "TABLE";
					}
				}
			}

			// Not an InnoDB table: a view, or another engine (only seen with REFERENCES)
			if (type == null)
			{
				sql = "select t.TABLE_TYPE from information_schema.TABLES t where t.TABLE_SCHEMA = " + q(trySchema) + " and t.TABLE_NAME = " + q(tryTable);
				try (Statement stmnt = conn.createStatement())
				{
					stmnt.setQueryTimeout(QUERY_TIMEOUT_SEC);
					try (ResultSet rs = stmnt.executeQuery(sql))
					{
						while (rs.next())
							type = "VIEW".equals(rs.getString(1)) ? "VIEW" : "TABLE";
					}
				}
			}

			if (type != null)
			{
				schema = trySchema;
				table  = tryTable;
			}
		}

		if (type == null)
		{
			// In the future, do NOT do lookup of this name
			pch.markDdlDetailsAsDiscarded(dbname, originObjectName);
			_logger.info("DDL Lookup: Can't find any InnoDB table (or a table or view the login can see) for dbname='" + dbname + "', objectName='" + originObjectName + "', source='" + qe._source + "'. Skipping DDL Storage of this object. Also adding it to the 'discard' list.");
			return Collections.emptyList();
		}

		DdlDetails entry = new DdlDetails();
		entry.setDbname          ( schema );
		entry.setSearchDbname    ( dbname );
		entry.setOwner           ( schema );
		entry.setObjectName      ( table );
		entry.setSearchObjectName( originObjectName ); // NOT stored, used for: isDdlDetailsStored(), markDdlDetailsAsStored()
		entry.setType            ( type );
		entry.setSource          ( qe._source );
		entry.setDependParent    ( qe._dependParent );
		entry.setDependLevel     ( qe._dependLevel );
		entry.setSampleTime      ( new Timestamp(System.currentTimeMillis()) );

		String qSchema = q(schema);
		String qTable  = q(table);

		//----------------------------------------------------------------------------------
		// objectText: the DDL (SHOW CREATE needs a privilege on the table: REFERENCES is enough)
		// Without it, an InnoDB table gets its columns from the InnoDB dictionary
		//----------------------------------------------------------------------------------
		String showCreate = null;
		String showCreateError = null;
		try (Statement stmnt = conn.createStatement())
		{
			stmnt.setQueryTimeout(QUERY_TIMEOUT_SEC);
			try (ResultSet rs = stmnt.executeQuery(("VIEW".equals(type) ? "SHOW CREATE VIEW " : "SHOW CREATE TABLE ") + bq(schema) + "." + bq(table)))
			{
				while (rs.next())
					showCreate = rs.getString(2);
			}
		}
		catch (SQLException ex)
		{
			showCreateError = ex.getMessage().trim();
		}

		if (showCreate != null)
		{
			entry.setObjectText(showCreate);
		}
		else if (tableId >= 0)
		{
			// The MySQL type is the low byte of PRTYPE (MYSQL_TYPE_xxx), 256 = NOT NULL, 512 = UNSIGNED, the character set is at bit 16 (63 = binary)
			// LEN is the length in BYTES (a VARCHAR(100) in utf8mb4 is 400)
			String binary = "((c.PRTYPE >> 16) & 255) = 63";
			String sqlColumns = ""
				+ "select \n"
				+ "     CAST(c.POS + 1 as CHAR) as COLUMN_NO \n"
				+ "    ,c.NAME as COLUMN_NAME \n"
				+ "    ,CONCAT(CASE c.PRTYPE & 255 \n"
				+ "         WHEN 1   THEN 'tinyint'     WHEN 2   THEN 'smallint'   WHEN 9   THEN 'mediumint'   WHEN 3   THEN 'int'        WHEN 8   THEN 'bigint' \n"
				+ "         WHEN 4   THEN 'float'       WHEN 5   THEN 'double'     WHEN 246 THEN 'decimal'     WHEN 0   THEN 'decimal' \n"
				+ "         WHEN 10  THEN 'date'        WHEN 14  THEN 'date'       WHEN 11  THEN 'time'        WHEN 19  THEN 'time' \n"
				+ "         WHEN 12  THEN 'datetime'    WHEN 18  THEN 'datetime'   WHEN 7   THEN 'timestamp'   WHEN 17  THEN 'timestamp'  WHEN 13  THEN 'year' \n"
				+ "         WHEN 15  THEN IF(" + binary + ", 'varbinary', 'varchar')  WHEN 253 THEN IF(" + binary + ", 'varbinary', 'varchar') \n"
				+ "         WHEN 254 THEN IF(" + binary + ", 'binary', 'char')         WHEN 16  THEN 'bit' \n"
				+ "         WHEN 249 THEN IF(" + binary + ", 'tinyblob', 'tinytext')   WHEN 250 THEN IF(" + binary + ", 'mediumblob', 'mediumtext') \n"
				+ "         WHEN 251 THEN IF(" + binary + ", 'longblob', 'longtext')   WHEN 252 THEN IF(" + binary + ", 'blob', 'text') \n"
				+ "         WHEN 245 THEN 'json'        WHEN 247 THEN 'enum'       WHEN 248 THEN 'set'         WHEN 255 THEN 'geometry' \n"
				+ "         ELSE CONCAT('type ', c.PRTYPE & 255) END, IF(c.PRTYPE & 512, ' unsigned', '')) as DATA_TYPE \n"
				+ "    ,CAST(c.LEN as CHAR) as LEN_BYTES \n"
				+ "    ,IF(c.PRTYPE & 256, 'NOT NULL', 'NULL') as NULLABLE \n"
				+ "from information_schema.INNODB_COLUMNS c \n"
				+ "where c.TABLE_ID = " + tableId + " \n"
				+ "order by c.POS";

			try
			{
				entry.setObjectText( "-- The login can't run SHOW CREATE TABLE (" + showCreateError + ")\n"
				                   + "-- These are the columns from the InnoDB data dictionary: the data types are approximate, and LEN_BYTES is in bytes.\n"
				                   + "-- 'GRANT REFERENCES ON *.* TO <user>' gives the real DDL, without access to the rows (see DbmsSetup/mysql.txt).\n"
				                   + ResultSetTableModel.executeQuery(conn, sqlColumns, QUERY_TIMEOUT_SEC, "columns").toAsciiTableString() );
			}
			catch (SQLException ex)
			{
				entry.setObjectText( ex.toString() );
				_logger.warn("DDL Lookup. Problems getting the COLUMNS of table " + entry.getSchemaAndObjectName() + ", source='" + qe._source + "'. Caught: " + ex.getMessage().trim());
			}
		}
		else
		{
			// A view: MySQL shows its text (SHOW CREATE VIEW, information_schema.VIEWS and VIEW_TABLE_USAGE) only with SELECT on it, REFERENCES and SHOW VIEW are not enough (checked on 8.0)
			entry.setObjectText( "-- The login can't run SHOW CREATE " + type + ": " + showCreateError + "\n"
			                   + ("VIEW".equals(type) ? "-- MySQL shows the text of a view only to a login with SELECT on it, and the monitoring login has no access to the data (see DbmsSetup/mysql.txt).\n" : "") );
		}

		//----------------------------------------------------------------------------------
		// extraInfoText: the table (sizes, rows, how it is read), and its indexes (with how they are used)
		// NOTE: numbers are made into text by MySQL (CAST ... as CHAR): otherwise they are formatted the way of the collector's country
		//----------------------------------------------------------------------------------
		if (tableId >= 0)
		{
			// All partitions together. The statistics are only valid if they are 'Initialized' (the table has been used since the server started), otherwise NULL
			String innodbName = schema + "/" + table;
			String allParts   = "(t.NAME = " + q(innodbName) + " or LEFT(t.NAME, " + (innodbName.length() + 3) + ") = " + q(innodbName + "#p#") + ")";
			String usage      = "performance_schema.table_io_waits_summary_by_index_usage u where u.OBJECT_SCHEMA = " + qSchema + " and u.OBJECT_NAME = " + qTable;

			String sqlTable = ""
				+ "select \n"
				+ "     " + qSchema + " as SCHEMA_NAME \n"
				+ "    ," + qTable + " as TABLE_NAME \n"
				+ "    ,CAST(count(*) as CHAR) as PARTITIONS \n"
				+ "    ,CAST(IF(sum(st.STATS_INITIALIZED <> 'Initialized') > 0, NULL, sum(st.NUM_ROWS)) as CHAR) as ROWS_ESTIMATE \n"
				+ "    ,CAST(ROUND(sum(ts.FILE_SIZE) / 1024) as CHAR) as FILE_KB \n"
				+ "    ,CAST(IF(sum(st.STATS_INITIALIZED <> 'Initialized') > 0, NULL, ROUND(sum(st.CLUST_INDEX_SIZE) * @@innodb_page_size / 1024)) as CHAR) as DATA_KB \n"
				+ "    ,CAST(IF(sum(st.STATS_INITIALIZED <> 'Initialized') > 0, NULL, ROUND(sum(st.OTHER_INDEX_SIZE) * @@innodb_page_size / 1024)) as CHAR) as INDEX_KB \n"
				+ "    ,CAST(IF(sum(st.STATS_INITIALIZED <> 'Initialized') > 0, NULL, sum(st.MODIFIED_COUNTER)) as CHAR) as MODIFIED_ROWS \n"
				+ "    ,max(t.ROW_FORMAT) as ROW_FORMAT \n"
				+ "    ,(select CAST(u.COUNT_READ   as CHAR) from " + usage + " and u.INDEX_NAME is null) as ROWS_READ_NO_INDEX \n"
				+ "    ,(select CAST(u.COUNT_INSERT as CHAR) from " + usage + " and u.INDEX_NAME is null) as ROWS_INSERTED \n"
				+ "    ,(select CAST(sum(u.COUNT_UPDATE) as CHAR) from " + usage + ") as ROWS_UPDATED \n"
				+ "    ,(select CAST(sum(u.COUNT_DELETE) as CHAR) from " + usage + ") as ROWS_DELETED \n"
				+ "    ,(select x.ENGINE from information_schema.TABLES x where x.TABLE_SCHEMA = " + qSchema + " and x.TABLE_NAME = " + qTable + ") as ENGINE \n"
				+ "    ,(select DATE_FORMAT(x.CREATE_TIME, '%Y-%m-%d %H:%i') from information_schema.TABLES x where x.TABLE_SCHEMA = " + qSchema + " and x.TABLE_NAME = " + qTable + ") as CREATE_TIME \n"
				+ "    ,(select DATE_FORMAT(x.UPDATE_TIME, '%Y-%m-%d %H:%i') from information_schema.TABLES x where x.TABLE_SCHEMA = " + qSchema + " and x.TABLE_NAME = " + qTable + ") as UPDATE_TIME \n"
				+ "from information_schema.INNODB_TABLES t \n"
				+ "left join information_schema.INNODB_TABLESPACES ts on ts.SPACE = t.SPACE and ts.SPACE_TYPE = 'Single' \n"
				+ "left join information_schema.INNODB_TABLESTATS  st on st.TABLE_ID = t.TABLE_ID \n"
				+ "where " + allParts;

			// The indexes of the first partition (they are the same in all), with how they are used since the server started (all partitions)
			// CARDINALITY is only there with REFERENCES (information_schema.STATISTICS)
			String sqlIndexes = ""
				+ "select \n"
				+ "     i.NAME as INDEX_NAME \n"
				+ "    ,CASE WHEN i.TYPE & 32 THEN 'FULLTEXT' \n"
				+ "          WHEN i.TYPE & 64 THEN 'SPATIAL' \n"
				+ "          WHEN i.TYPE & 1 and i.NAME = 'GEN_CLUST_INDEX' THEN 'CLUSTERED (no primary key)' \n"
				+ "          WHEN i.TYPE & 1  THEN 'PRIMARY KEY' \n"
				+ "          WHEN i.TYPE & 2  THEN 'UNIQUE' \n"
				+ "          ELSE 'INDEX' END as INDEX_TYPE \n"
				+ "    ,(select group_concat(f.NAME order by f.POS separator ', ') from information_schema.INNODB_FIELDS f where f.INDEX_ID = i.INDEX_ID) as INDEX_KEYS \n"
				+ "    ,(select CAST(max(s.CARDINALITY) as CHAR) from information_schema.STATISTICS s where s.TABLE_SCHEMA = " + qSchema + " and s.TABLE_NAME = " + qTable + " and s.INDEX_NAME = i.NAME) as CARDINALITY \n"
				+ "    ,(select CAST(u.COUNT_READ as CHAR) from " + usage + " and u.INDEX_NAME = i.NAME) as ROWS_READ \n"
				+ "    ,(select CAST(u.COUNT_STAR as CHAR) from " + usage + " and u.INDEX_NAME = i.NAME) as IO_COUNT \n"
				+ "from information_schema.INNODB_INDEXES i \n"
				+ "where i.TABLE_ID = " + tableId + " \n"
				+ "order by i.TYPE & 1 desc, i.NAME";

			// A partitioned table: INNODB_INDEXES has no rows for the partitions, so the index names come from performance_schema
			// (there once the table has been opened since the server started), and the keys from information_schema.STATISTICS (only with REFERENCES)
			String stats = "information_schema.STATISTICS s where s.TABLE_SCHEMA = " + qSchema + " and s.TABLE_NAME = " + qTable + " and s.INDEX_NAME = u.INDEX_NAME";
			if (partitioned)
			{
				sqlIndexes = ""
					+ "select \n"
					+ "     u.INDEX_NAME \n"
					+ "    ,IF(u.INDEX_NAME = 'PRIMARY', 'PRIMARY KEY', (select IF(max(s.NON_UNIQUE) = 0, 'UNIQUE', 'INDEX') from " + stats + ")) as INDEX_TYPE \n"
					+ "    ,(select group_concat(s.COLUMN_NAME order by s.SEQ_IN_INDEX separator ', ') from " + stats + ") as INDEX_KEYS \n"
					+ "    ,(select CAST(max(s.CARDINALITY) as CHAR) from " + stats + ") as CARDINALITY \n"
					+ "    ,CAST(u.COUNT_READ as CHAR) as ROWS_READ \n"
					+ "    ,CAST(u.COUNT_STAR as CHAR) as IO_COUNT \n"
					+ "from " + usage + " and u.INDEX_NAME is not null \n"
					+ "order by u.INDEX_NAME = 'PRIMARY' desc, u.INDEX_NAME";
			}

			// Each on its own: if one fails, the other is still stored
			String extraInfo = partitioned
				? "-- A partitioned table: MySQL has no InnoDB statistics for partitions (ROWS_ESTIMATE, DATA_KB, INDEX_KB are NULL), FILE_KB is all partitions.\n"
				+ "-- The indexes are from performance_schema (seen once the table has been used since the server started), INDEX_KEYS needs REFERENCES.\n"
				: "";
			for (String[] part : new String[][] { {"table", sqlTable}, {"indexes", sqlIndexes} })
			{
				try
				{
					extraInfo += ResultSetTableModel.executeQuery(conn, part[1], QUERY_TIMEOUT_SEC, part[0]).toAsciiTableString() + "\n";
				}
				catch (SQLException ex)
				{
					extraInfo += "Could not get the " + part[0] + ": " + ex.getMessage().trim() + "\n\n";
					_logger.warn("DDL Lookup. Problems getting the " + part[0].toUpperCase() + " of table " + entry.getSchemaAndObjectName() + ", source='" + qe._source + "'. Caught: " + ex.getMessage().trim());
				}
			}
			entry.setExtraInfoText(extraInfo);
		}

		//----------------------------------------------------------------------------------
		// dependsText: the triggers on the table (only seen with the TRIGGER privilege on the table: most often nothing)
		//----------------------------------------------------------------------------------
		if ("TABLE".equals(type))
		{
			String sqlTriggers = ""
				+ "select g.TRIGGER_NAME, g.ACTION_TIMING, g.EVENT_MANIPULATION, g.ACTION_STATEMENT \n"
				+ "from information_schema.TRIGGERS g \n"
				+ "where g.EVENT_OBJECT_SCHEMA = " + qSchema + " \n"
				+ "  and g.EVENT_OBJECT_TABLE  = " + qTable  + " \n"
				+ "order by g.ACTION_TIMING, g.EVENT_MANIPULATION, g.ACTION_ORDER";

			StringBuilder triggers = new StringBuilder();
			try (Statement stmnt = conn.createStatement())
			{
				stmnt.setQueryTimeout(QUERY_TIMEOUT_SEC);
				try (ResultSet rs = stmnt.executeQuery(sqlTriggers))
				{
					while (rs.next())
						triggers.append("-- ").append(rs.getString(1)).append("\nCREATE TRIGGER ").append(rs.getString(1)).append(" ").append(rs.getString(2)).append(" ").append(rs.getString(3))
						        .append(" ON ").append(table).append(" FOR EACH ROW\n").append(rs.getString(4)).append("\n\n");
				}
			}
			catch (SQLException ex)
			{
				_logger.info("DDL Lookup. Could not read the TRIGGERS of table " + entry.getSchemaAndObjectName() + ", source='" + qe._source + "'. Caught: " + ex.getMessage().trim());
			}
			if (triggers.length() > 0)
				entry.setDependsText(triggers.toString());
		}

		List<DdlDetails> objectList = new ArrayList<>();
		objectList.add(entry);
		return objectList;
	}

	//---------------------------------------------------------------------------------------
	// Helpers
	//---------------------------------------------------------------------------------------

	/**
	 * @return {schema, table}: 'schema.table' gives that schema, otherwise the schema is 'dbname'. Backticks and double quotes are removed
	 */
	public static String[] splitSchemaAndTable(String dbname, String objectName)
	{
		String str = objectName.trim().replace("`", "").replace("\"", "");

		int pos = str.lastIndexOf('.');
		if (pos <= 0)
			return new String[] { dbname == null ? null : dbname.trim(), str };

		return new String[] { str.substring(0, pos), str.substring(pos + 1) };
	}

	/** A SQL string constant */
	private static String q(String str)
	{
		return "'" + str.replace("\\", "\\\\").replace("'", "''") + "'";
	}

	/** A quoted identifier: `name` */
	private static String bq(String str)
	{
		return "`" + str.replace("`", "``") + "`";
	}
}
