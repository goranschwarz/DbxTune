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
package com.dbxtune.pcs.inspection;

import java.lang.invoke.MethodHandles;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.dbxtune.gui.ResultSetTableModel;
import com.dbxtune.pcs.DdlDetails;
import com.dbxtune.pcs.ObjectLookupQueueEntry;
import com.dbxtune.pcs.PersistentCounterHandler;
import com.dbxtune.sql.conn.DbxConnection;
import com.dbxtune.utils.StringUtil;

/**
 * Db2: what is stored in the "DDL Storage" (table <code>MonDdlStorage</code>) for the tables the collector has seen (built on ObjectLookupInspectorMySql).
 * <p>
 * dbname = the schema, objectName = the table (or <code>schema.table</code>, as it was written in a statement).
 * A statement in the package cache does not say its default schema, so a table without a schema is looked for in all schemas
 * (dbname = {@link #ANY_SCHEMA}): every table with that name is stored (at most {@link #MAX_TABLES_FOR_ANY_SCHEMA}).
 * <p>
 * Everything is read from the catalog (SYSCAT, readable by PUBLIC unless the database is RESTRICTIVE), ADMIN_GET_TAB_INFO (the sizes),
 * and MON_GET_TABLE / MON_GET_INDEX (how they were used since the database was activated, the role DBXTUNE_MON has them). No table is read.
 * Db2 has no SQL that returns the DDL (db2look is a command), so the CREATE TABLE is put together from SYSCAT.COLUMNS, SYSCAT.TABCONST, SYSCAT.REFERENCES and SYSCAT.INDEXES.
 * The other texts are stored as text tables (as the Oracle and MySQL inspectors does), Db2Abstract reads them back.
 * <p>
 * Execution plans are not looked up here: CmActiveStatements captures them (EXPLAIN_FROM_SECTION) while a statement runs.
 */
public class ObjectLookupInspectorDb2
extends ObjectLookupInspectorAbstract
{
	private static final Logger _logger = LogManager.getLogger(MethodHandles.lookup().lookupClass());

	/** Every statement of a lookup gets this many seconds */
	private static final int QUERY_TIMEOUT_SEC = 30;

	/** dbname for a table that was written without a schema: it is looked for in all schemas */
	public static final String ANY_SCHEMA = "*";

	/** A table without a schema can be in several schemas: at most this many are stored */
	private static final int MAX_TABLES_FOR_ANY_SCHEMA = 5;

	@Override
	public boolean allowInspection(ObjectLookupQueueEntry entry)
	{
		if (entry == null || entry._objectName == null)
			return false;

		// A statement: the plans are captured by CmActiveStatements (see the class comment)
		if (PersistentCounterHandler.STATEMENT_CACHE_NAME.equals(entry._dbname))
			return false;

		String[] schemaAndTable = splitSchemaAndTable(entry._dbname, entry._objectName);
		return StringUtil.hasValue(schemaAndTable[0]) && ! isSystemObject(schemaAndTable[0], schemaAndTable[1]);
	}

	/**
	 * The names we do NOT look up: the catalog and the other schemas of Db2 itself (they all start with SYS), declared temporary tables (SESSION),
	 * and names that are not tables (a table function: <code>TABLE(MON_GET_...)</code>, DUAL in Oracle mode).<br>
	 * Also used by the report: a name like that is not worth a "was not found in the DDL Storage".
	 */
	public static boolean isSystemObject(String schema, String table)
	{
		if (schema != null && (schema.toUpperCase().startsWith("SYS") || schema.equalsIgnoreCase("SESSION")))
			return true;

		return table == null || table.isEmpty() || table.equalsIgnoreCase("DUAL") || table.indexOf('(') >= 0 || table.indexOf(' ') >= 0;
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
		boolean  anySchema      = ANY_SCHEMA.equals(schemaAndTable[0]);

		//----------------------------------------------------------------------------------
		// Find it: {schema, table, type}. An alias (synonym) is followed to its table
		//----------------------------------------------------------------------------------
		String sql = ""
			+ "select RTRIM(t.TABSCHEMA), t.TABNAME, t.TYPE, RTRIM(t.BASE_TABSCHEMA), t.BASE_TABNAME \n"
			+ "from SYSCAT.TABLES t \n"
			+ "where t.TABNAME = " + q(schemaAndTable[1]) + " \n"
			+ (anySchema ? "  and t.TABSCHEMA not like 'SYS%' and t.TABSCHEMA <> 'SESSION' \n" : "  and t.TABSCHEMA = " + q(schemaAndTable[0]) + " \n")
			+ "order by t.TABSCHEMA \n"
			+ "fetch first " + MAX_TABLES_FOR_ANY_SCHEMA + " rows only";

		List<String[]> found = new ArrayList<>();
		try (Statement stmnt = conn.createStatement())
		{
			stmnt.setQueryTimeout(QUERY_TIMEOUT_SEC);
			try (ResultSet rs = stmnt.executeQuery(sql))
			{
				while (rs.next())
					found.add(new String[] { rs.getString(1), rs.getString(2), rs.getString(3), rs.getString(4), rs.getString(5) });
			}
		}

		List<DdlDetails> objectList = new ArrayList<>();
		for (String[] row : found)
		{
			String schema = row[0];
			String table  = row[1];
			String type   = row[2];

			// An alias: its table (an alias can point to another alias)
			for (int level = 0; "A".equals(type) && level < 10; level++)
			{
				String baseSql = "select RTRIM(t.TABSCHEMA), t.TABNAME, t.TYPE, RTRIM(t.BASE_TABSCHEMA), t.BASE_TABNAME from SYSCAT.TABLES t where t.TABSCHEMA = " + q(row[3]) + " and t.TABNAME = " + q(row[4]);
				type = null;
				try (Statement stmnt = conn.createStatement())
				{
					stmnt.setQueryTimeout(QUERY_TIMEOUT_SEC);
					try (ResultSet rs = stmnt.executeQuery(baseSql))
					{
						while (rs.next())
						{
							row    = new String[] { rs.getString(1), rs.getString(2), rs.getString(3), rs.getString(4), rs.getString(5) };
							schema = row[0];
							table  = row[1];
							type   = row[2];
						}
					}
				}
			}
			if (type == null || "A".equals(type) || isSystemObject(schema, table))
				continue;

			objectList.add(createEntry(conn, qe, schema, table, type));
		}

		if (objectList.isEmpty())
		{
			// In the future, do NOT do lookup of this name
			pch.markDdlDetailsAsDiscarded(dbname, originObjectName);
			_logger.info("DDL Lookup: Can't find any table or view for dbname='" + dbname + "', objectName='" + originObjectName + "', source='" + qe._source + "'. Skipping DDL Storage of this object. Also adding it to the 'discard' list.");
		}
		return objectList;
	}

	/**
	 * @param db2Type  SYSCAT.TABLES.TYPE: T = table, V = view, S = materialized query table, G = created temporary table, N = nickname, U / H = typed table...
	 */
	private DdlDetails createEntry(DbxConnection conn, ObjectLookupQueueEntry qe, String schema, String table, String db2Type)
	throws SQLException
	{
		boolean isView = "V".equals(db2Type) || "W".equals(db2Type);

		DdlDetails entry = new DdlDetails();
		entry.setDbname          ( schema );
		entry.setSearchDbname    ( qe._dbname );
		entry.setOwner           ( schema );
		entry.setObjectName      ( table );
		entry.setSearchObjectName( qe._objectName ); // NOT stored, used for: isDdlDetailsStored(), markDdlDetailsAsStored()
		entry.setType            ( isView ? "VIEW" : "TABLE" );
		entry.setSource          ( qe._source );
		entry.setDependParent    ( qe._dependParent );
		entry.setDependLevel     ( qe._dependLevel );
		entry.setSampleTime      ( new Timestamp(System.currentTimeMillis()) );

		String qSchema = q(schema);
		String qTable  = q(table);
		String where   = "where x.TABSCHEMA = " + qSchema + " and x.TABNAME = " + qTable;

		//----------------------------------------------------------------------------------
		// objectText: the text of a view (and of a materialized query table), or the CREATE TABLE put together from the catalog
		//----------------------------------------------------------------------------------
		String viewText = null;
		if (isView || "S".equals(db2Type))
		{
			try (Statement stmnt = conn.createStatement())
			{
				stmnt.setQueryTimeout(QUERY_TIMEOUT_SEC);
				try (ResultSet rs = stmnt.executeQuery("select x.TEXT from SYSCAT.VIEWS x where x.VIEWSCHEMA = " + qSchema + " and x.VIEWNAME = " + qTable))
				{
					while (rs.next())
						viewText = rs.getString(1);
				}
			}
			catch (SQLException ex)
			{
				viewText = "-- Could not read the text from SYSCAT.VIEWS: " + ex.getMessage().trim();
			}
		}

		if (isView)
		{
			entry.setObjectText(viewText);
			return entry; // a view has no sizes or indexes
		}

		try
		{
			entry.setObjectText( (viewText == null ? "" : "-- A materialized query table:\n" + viewText + "\n\n")
			                   + getCreateTableText(conn, schema, table, where) );
		}
		catch (SQLException ex)
		{
			entry.setObjectText( "-- Could not read the columns of the table from SYSCAT.COLUMNS: " + ex.getMessage().trim() );
			_logger.warn("DDL Lookup. Problems getting the COLUMNS of table " + entry.getSchemaAndObjectName() + ", source='" + qe._source + "'. Caught: " + ex.getMessage().trim());
		}

		//----------------------------------------------------------------------------------
		// extraInfoText: the table (rows, sizes, statistics), its indexes, and how both were used since the database was activated
		// NOTE: numbers are made into text by Db2 (VARCHAR): otherwise they are formatted the way of the collector's country
		//----------------------------------------------------------------------------------
		String tabInfo = "table(SYSPROC.ADMIN_GET_TAB_INFO(t.TABSCHEMA, t.TABNAME)) a";
		String sqlTable = ""
			+ "select \n"
			+ "     RTRIM(t.TABSCHEMA)                                       as SCHEMA_NAME \n"
			+ "    ,t.TABNAME                                                as TABLE_NAME \n"
			+ "    ,t.TYPE                                                   as TABLE_TYPE \n"
			+ "    ,VARCHAR(t.CARD)                                          as ROW_COUNT_STATS \n"
			+ "    ,VARCHAR(t.NPAGES)                                        as NPAGES \n"
			+ "    ,VARCHAR(t.OVERFLOW)                                      as OVERFLOW_ROWS \n"
			+ "    ,(select VARCHAR(sum(a.DATA_OBJECT_P_SIZE + COALESCE(a.COL_OBJECT_P_SIZE, 0))) from " + tabInfo + ") as DATA_KB \n"
			+ "    ,(select VARCHAR(sum(a.INDEX_OBJECT_P_SIZE)) from " + tabInfo + ")                                     as INDEX_KB \n"
			+ "    ,(select VARCHAR(sum(a.LONG_OBJECT_P_SIZE + a.LOB_OBJECT_P_SIZE + a.XML_OBJECT_P_SIZE)) from " + tabInfo + ") as LOB_KB \n"
			+ "    ,(select VARCHAR(sum(COALESCE(a.STATS_ROWS_MODIFIED, 0))) from " + tabInfo + ")                       as STATS_ROWS_MODIFIED \n"
			+ "    ,(select VARCHAR(count(*)) from SYSCAT.DATAPARTITIONS p where p.TABSCHEMA = t.TABSCHEMA and p.TABNAME = t.TABNAME) as PARTITIONS \n"
			+ "    ,VARCHAR_FORMAT(t.STATS_TIME, 'YYYY-MM-DD HH24:MI')       as STATS_TIME \n"
			+ "    ,VARCHAR(t.LASTUSED)                                      as LASTUSED \n"
			+ "    ,VARCHAR_FORMAT(t.CREATE_TIME, 'YYYY-MM-DD HH24:MI')      as CREATE_TIME \n"
			+ "    ,VARCHAR_FORMAT(t.ALTER_TIME, 'YYYY-MM-DD HH24:MI')       as ALTER_TIME \n"
			+ "    ,t.TBSPACE \n"
			+ "    ,t.TABLEORG \n"
			+ "    ,t.COMPRESSION \n"
			+ "    ,t.STATUS \n"
			+ "    ,t.VOLATILE \n"
			+ "from SYSCAT.TABLES t \n"
			+ "where t.TABSCHEMA = " + qSchema + " and t.TABNAME = " + qTable;

		// The key columns: '+COL1-COL2' in SYSCAT.INDEXES.COLNAMES, so they are read from SYSCAT.INDEXCOLUSE (COLORDER: A, D, or I = an INCLUDE column)
		String sqlIndexes = ""
			+ "select \n"
			+ "     x.INDNAME                                                as INDEX_NAME \n"
			+ "    ,CASE x.UNIQUERULE WHEN 'P' THEN 'PRIMARY KEY' WHEN 'U' THEN 'UNIQUE' ELSE 'INDEX' END as INDEX_TYPE \n"
			+ "    ,(select LISTAGG(u.COLNAME || CASE WHEN u.COLORDER = 'D' THEN ' DESC' ELSE '' END, ', ') WITHIN GROUP (ORDER BY u.COLSEQ) \n"
			+ "      from SYSCAT.INDEXCOLUSE u where u.INDSCHEMA = x.INDSCHEMA and u.INDNAME = x.INDNAME and u.COLORDER <> 'I') as INDEX_KEYS \n"
			+ "    ,(select LISTAGG(u.COLNAME, ', ') WITHIN GROUP (ORDER BY u.COLSEQ) \n"
			+ "      from SYSCAT.INDEXCOLUSE u where u.INDSCHEMA = x.INDSCHEMA and u.INDNAME = x.INDNAME and u.COLORDER = 'I') as INCLUDE_COLS \n"
			+ "    ,x.INDEXTYPE \n"
			+ "    ,VARCHAR(x.FULLKEYCARD)                                   as CARDINALITY \n"
			+ "    ,VARCHAR(x.NLEAF)                                         as NLEAF \n"
			+ "    ,VARCHAR(x.NLEVELS)                                       as NLEVELS \n"
			+ "    ,VARCHAR(DECIMAL(CASE WHEN x.CLUSTERFACTOR > 0 THEN x.CLUSTERFACTOR * 100 WHEN x.CLUSTERRATIO >= 0 THEN x.CLUSTERRATIO END, 5, 1)) as CLUSTER_PCT \n"   // -1 = no statistics

			+ "    ,VARCHAR(x.LASTUSED)                                      as LASTUSED \n"
			+ "    ,RTRIM(x.INDSCHEMA)                                       as INDEX_SCHEMA \n"
			+ "from SYSCAT.INDEXES x \n"
			+ where + " \n"
			+ "order by CASE x.UNIQUERULE WHEN 'P' THEN 0 WHEN 'U' THEN 1 ELSE 2 END, x.INDNAME";

		// How they were used since the database was activated (a table that has not been used since then has no rows in MON_GET_TABLE)
		String sqlTableUsage = ""
			+ "select \n"
			+ "     VARCHAR(sum(m.TABLE_SCANS))                              as TABLE_SCANS \n"
			+ "    ,VARCHAR(sum(m.ROWS_READ))                                as ROWS_READ \n"
			+ "    ,VARCHAR(sum(m.ROWS_INSERTED))                            as ROWS_INSERTED \n"
			+ "    ,VARCHAR(sum(m.ROWS_UPDATED))                             as ROWS_UPDATED \n"
			+ "    ,VARCHAR(sum(m.ROWS_DELETED))                             as ROWS_DELETED \n"
			+ "    ,VARCHAR(sum(m.LOCK_WAITS))                               as LOCK_WAITS \n"
			+ "from table(MON_GET_TABLE(" + qSchema + ", " + qTable + ", -2)) m";

		String sqlIndexUsage = ""
			+ "select \n"
			+ "     x.INDNAME                                                as INDEX_NAME \n"
			+ "    ,VARCHAR(sum(m.INDEX_SCANS))                              as INDEX_SCANS \n"
			+ "    ,VARCHAR(sum(m.INDEX_ONLY_SCANS))                         as INDEX_ONLY_SCANS \n"
			+ "    ,VARCHAR(sum(m.KEY_UPDATES))                              as KEY_UPDATES \n"
			+ "from table(MON_GET_INDEX(" + qSchema + ", " + qTable + ", -2)) m \n"
			+ "join SYSCAT.INDEXES x on x.TABSCHEMA = m.TABSCHEMA and x.TABNAME = m.TABNAME and x.IID = m.IID \n"
			+ "group by x.INDNAME";

		// Each on its own: if one fails, the others are still stored
		String extraInfo = "";
		for (String[] part : new String[][] { {"table", sqlTable}, {"indexes", sqlIndexes}, {"table usage", sqlTableUsage}, {"index usage", sqlIndexUsage} })
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

		//----------------------------------------------------------------------------------
		// dependsText: the triggers on the table
		//----------------------------------------------------------------------------------
		StringBuilder triggers = new StringBuilder();
		try (Statement stmnt = conn.createStatement())
		{
			stmnt.setQueryTimeout(QUERY_TIMEOUT_SEC);
			try (ResultSet rs = stmnt.executeQuery("select x.TRIGNAME, x.TEXT from SYSCAT.TRIGGERS x " + where + " and x.VALID = 'Y' order by x.TRIGTIME, x.TRIGEVENT, x.TRIGNAME"))
			{
				while (rs.next())
					triggers.append("-- ").append(rs.getString(1)).append("\n").append(rs.getString(2)).append("\n\n");
			}
		}
		catch (SQLException ex)
		{
			_logger.info("DDL Lookup. Could not read the TRIGGERS of table " + entry.getSchemaAndObjectName() + ", source='" + qe._source + "'. Caught: " + ex.getMessage().trim());
		}
		if (triggers.length() > 0)
			entry.setDependsText(triggers.toString());

		return entry;
	}

	/**
	 * CREATE TABLE, the constraints (primary key, unique, foreign keys, checks) and the indexes the user created, put together from the catalog.<br>
	 * Close to what db2look writes, but not all of it (partitioning, distribution, security labels, storage options...).
	 */
	private static String getCreateTableText(DbxConnection conn, String schema, String table, String where)
	throws SQLException
	{
		String fullName = qi(schema) + "." + qi(table);
		StringBuilder sb = new StringBuilder();
		sb.append("-- Put together from the catalog (SYSCAT): 'db2look -d <db> -e -z ").append(schema).append(" -t ").append(table).append("' gives the exact DDL\n");
		sb.append("CREATE TABLE ").append(fullName).append(" (\n");

		// The columns
		String sqlColumns = ""
			+ "select x.COLNAME, RTRIM(x.TYPESCHEMA), x.TYPENAME, x.LENGTH, x.SCALE, x.NULLS, x.DEFAULT, x.IDENTITY, x.GENERATED, x.TEXT, x.CODEPAGE, x.HIDDEN \n"
			+ "from SYSCAT.COLUMNS x \n"
			+ where + " \n"
			+ "order by x.COLNO";
		try (Statement stmnt = conn.createStatement())
		{
			stmnt.setQueryTimeout(QUERY_TIMEOUT_SEC);
			try (ResultSet rs = stmnt.executeQuery(sqlColumns))
			{
				String comma = " ";
				while (rs.next())
				{
					String colName    = rs.getString(1);
					String typeSchema = rs.getString(2);
					String typeName   = rs.getString(3);
					int    length     = rs.getInt(4);
					int    scale      = rs.getInt(5);
					String nulls      = rs.getString(6);
					String defaultVal = rs.getString(7);
					String identity   = rs.getString(8);
					String generated  = StringUtil.nullToValue(rs.getString(9), "").trim();
					String genText    = rs.getString(10);
					int    codepage   = rs.getInt(11);
					String hidden     = StringUtil.nullToValue(rs.getString(12), "").trim();

					// The data type: a distinct type (or another user type) has its schema
					String dataType = typeName;
					if ( ! "SYSIBM".equals(typeSchema) )
						dataType = qi(typeSchema) + "." + qi(typeName);
					else if ("CHARACTER".equals(typeName) || "VARCHAR".equals(typeName))
						dataType = typeName + "(" + length + ")" + (codepage == 0 ? " FOR BIT DATA" : "");
					else if ("GRAPHIC".equals(typeName) || "VARGRAPHIC".equals(typeName) || "BINARY".equals(typeName) || "VARBINARY".equals(typeName)
					      || "CLOB".equals(typeName) || "BLOB".equals(typeName) || "DBCLOB".equals(typeName))
						dataType = typeName + "(" + length + ")";
					else if ("DECIMAL".equals(typeName))
						dataType = "DECIMAL(" + length + "," + scale + ")";
					else if ("TIMESTAMP".equals(typeName) && scale != 6)
						dataType = "TIMESTAMP(" + scale + ")";
					else if ("DECFLOAT".equals(typeName))
						dataType = length == 8 ? "DECFLOAT(16)" : "DECFLOAT(34)";

					sb.append("    ").append(comma).append(qi(colName)).append(" ").append(dataType);
					if ("N".equals(nulls))
						sb.append(" NOT NULL");
					if ("Y".equals(identity))
						sb.append(" GENERATED ").append("A".equals(generated) ? "ALWAYS" : "BY DEFAULT").append(" AS IDENTITY");
					else if (StringUtil.hasValue(generated) && StringUtil.hasValue(genText))
						sb.append(" GENERATED ").append("A".equals(generated) ? "ALWAYS" : "BY DEFAULT").append(" ").append(genText.trim());
					else if (defaultVal != null)
						sb.append(" WITH DEFAULT ").append(defaultVal.trim());
					if ("I".equals(hidden))
						sb.append(" IMPLICITLY HIDDEN");
					sb.append("\n");
					comma = ",";
				}
			}
		}
		sb.append(")");

		// Where it is stored
		try (Statement stmnt = conn.createStatement())
		{
			stmnt.setQueryTimeout(QUERY_TIMEOUT_SEC);
			try (ResultSet rs = stmnt.executeQuery("select x.TBSPACE, x.INDEX_TBSPACE, x.LONG_TBSPACE, x.TABLEORG, x.COMPRESSION from SYSCAT.TABLES x " + where))
			{
				while (rs.next())
				{
					if (StringUtil.hasValue(rs.getString(1))) sb.append("\nIN ").append(qi(rs.getString(1).trim()));
					if (StringUtil.hasValue(rs.getString(2))) sb.append(" INDEX IN ").append(qi(rs.getString(2).trim()));
					if (StringUtil.hasValue(rs.getString(3))) sb.append(" LONG IN ").append(qi(rs.getString(3).trim()));
					sb.append("\nORGANIZE BY ").append("C".equals(rs.getString(4)) ? "COLUMN" : "ROW");
					if ("R".equals(rs.getString(5)) || "B".equals(rs.getString(5)))
						sb.append("\nCOMPRESS YES");
				}
			}
		}
		sb.append(";\n");

		// Primary key, unique constraints and foreign keys (the indexes Db2 made for them are not written below)
		String sqlConstraints = ""
			+ "select x.CONSTNAME, x.TYPE \n"
			+ "    ,(select LISTAGG(k.COLNAME, ', ') WITHIN GROUP (ORDER BY k.COLSEQ) from SYSCAT.KEYCOLUSE k where k.CONSTNAME = x.CONSTNAME and k.TABSCHEMA = x.TABSCHEMA and k.TABNAME = x.TABNAME) \n"
			+ "    ,RTRIM(r.REFTABSCHEMA), r.REFTABNAME \n"
			+ "    ,(select LISTAGG(k.COLNAME, ', ') WITHIN GROUP (ORDER BY k.COLSEQ) from SYSCAT.KEYCOLUSE k where k.CONSTNAME = r.REFKEYNAME and k.TABSCHEMA = r.REFTABSCHEMA and k.TABNAME = r.REFTABNAME) \n"
			+ "    ,r.DELETERULE \n"
			+ "    ,c.TEXT \n"
			+ "from SYSCAT.TABCONST x \n"
			+ "left join SYSCAT.REFERENCES r on r.CONSTNAME = x.CONSTNAME and r.TABSCHEMA = x.TABSCHEMA and r.TABNAME = x.TABNAME \n"
			+ "left join SYSCAT.CHECKS     c on c.CONSTNAME = x.CONSTNAME and c.TABSCHEMA = x.TABSCHEMA and c.TABNAME = x.TABNAME and c.TYPE = 'C' \n"
			+ where + " \n"
			+ "  and x.TYPE in ('P', 'U', 'F', 'K') \n"
			+ "order by CASE x.TYPE WHEN 'P' THEN 0 WHEN 'U' THEN 1 WHEN 'F' THEN 2 ELSE 3 END, x.CONSTNAME";
		try (Statement stmnt = conn.createStatement())
		{
			stmnt.setQueryTimeout(QUERY_TIMEOUT_SEC);
			try (ResultSet rs = stmnt.executeQuery(sqlConstraints))
			{
				while (rs.next())
				{
					String constName = rs.getString(1);
					String type      = rs.getString(2);
					String keys      = rs.getString(3);

					sb.append("ALTER TABLE ").append(fullName).append(" ADD CONSTRAINT ").append(qi(constName));
					if      ("P".equals(type)) sb.append(" PRIMARY KEY (").append(keys).append(")");
					else if ("U".equals(type)) sb.append(" UNIQUE (").append(keys).append(")");
					else if ("F".equals(type)) sb.append(" FOREIGN KEY (").append(keys).append(") REFERENCES ").append(qi(rs.getString(4))).append(".").append(qi(rs.getString(5)))
					                             .append(" (").append(rs.getString(6)).append(")")
					                             .append("C".equals(rs.getString(7)) ? " ON DELETE CASCADE" : "N".equals(rs.getString(7)) ? " ON DELETE SET NULL" : "R".equals(rs.getString(7)) ? " ON DELETE RESTRICT" : "");
					else                       sb.append(" CHECK (").append(StringUtil.nullToValue(rs.getString(8), "?")).append(")");
					sb.append(";\n");
				}
			}
		}

		// The indexes the user created (USER_DEFINED = 0: Db2 made it for a constraint)
		String sqlIndexes = ""
			+ "select RTRIM(x.INDSCHEMA), x.INDNAME, x.UNIQUERULE \n"
			+ "    ,(select LISTAGG(u.COLNAME || CASE WHEN u.COLORDER = 'D' THEN ' DESC' ELSE '' END, ', ') WITHIN GROUP (ORDER BY u.COLSEQ) \n"
			+ "      from SYSCAT.INDEXCOLUSE u where u.INDSCHEMA = x.INDSCHEMA and u.INDNAME = x.INDNAME and u.COLORDER <> 'I') \n"
			+ "    ,(select LISTAGG(u.COLNAME, ', ') WITHIN GROUP (ORDER BY u.COLSEQ) \n"
			+ "      from SYSCAT.INDEXCOLUSE u where u.INDSCHEMA = x.INDSCHEMA and u.INDNAME = x.INDNAME and u.COLORDER = 'I') \n"
			+ "    ,x.INDEXTYPE \n"
			+ "from SYSCAT.INDEXES x \n"
			+ where + " \n"
			+ "  and x.USER_DEFINED = 1 \n"
			+ "order by x.INDNAME";
		try (Statement stmnt = conn.createStatement())
		{
			stmnt.setQueryTimeout(QUERY_TIMEOUT_SEC);
			try (ResultSet rs = stmnt.executeQuery(sqlIndexes))
			{
				while (rs.next())
				{
					sb.append("CREATE ").append("U".equals(rs.getString(3)) || "P".equals(rs.getString(3)) ? "UNIQUE " : "").append("INDEX ")
					  .append(qi(rs.getString(1))).append(".").append(qi(rs.getString(2))).append(" ON ").append(fullName)
					  .append(" (").append(rs.getString(4)).append(")");
					if (StringUtil.hasValue(rs.getString(5)))
						sb.append(" INCLUDE (").append(rs.getString(5)).append(")");
					if ("CLUS".equals(StringUtil.nullToValue(rs.getString(6), "").trim()))
						sb.append(" CLUSTER");
					sb.append(";\n");
				}
			}
		}

		return sb.toString();
	}

	//---------------------------------------------------------------------------------------
	// Helpers
	//---------------------------------------------------------------------------------------

	/**
	 * @return {schema, table}: 'schema.table' gives that schema, otherwise the schema is 'dbname'.<br>
	 * As Db2 does: a name in double quotes keeps its case (the quotes are removed), other names are made upper case.
	 */
	public static String[] splitSchemaAndTable(String dbname, String objectName)
	{
		String str = objectName.trim();

		// The last dot that is not inside double quotes
		int     pos     = -1;
		boolean inQuote = false;
		for (int i = 0; i < str.length(); i++)
		{
			char c = str.charAt(i);
			if (c == '"')
				inQuote = ! inQuote;
			else if (c == '.' && ! inQuote)
				pos = i;
		}

		if (pos <= 0)
			return new String[] { toDb2Name(dbname), toDb2Name(str) };

		return new String[] { toDb2Name(str.substring(0, pos)), toDb2Name(str.substring(pos + 1)) };
	}

	/** "Name" keeps its case (the quotes are removed), other names are made upper case */
	private static String toDb2Name(String name)
	{
		if (name == null)
			return null;

		name = name.trim();
		if (name.length() >= 2 && name.startsWith("\"") && name.endsWith("\""))
			return name.substring(1, name.length() - 1).replace("\"\"", "\"");

		return name.toUpperCase();
	}

	/** A SQL string constant */
	private static String q(String str)
	{
		return "'" + str.replace("'", "''") + "'";
	}

	/** A quoted identifier: "name" */
	private static String qi(String str)
	{
		return "\"" + StringUtil.nullToValue(str, "").trim().replace("\"", "\"\"") + "\"";
	}
}
