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
import com.dbxtune.sql.conn.info.DbmsVersionInfoOracle;
import com.dbxtune.utils.StringUtil;
import com.dbxtune.utils.Ver;

/**
 * Oracle: what is stored in the "DDL Storage" (table <code>MonDdlStorage</code>) for the statements and tables the collector has seen.
 * <p>
 * Two kinds of lookups:
 * <ul>
 *   <li><b>A statement</b> (dbname = <code>statement_cache</code>, objectName = <code>SQL_ID</code> or <code>SQL_ID_PLANHASHVALUE</code>):<br>
 *       the execution plan as text (DBMS_XPLAN.DISPLAY_CURSOR), and the tables the plan uses. The tables are read from V$SQL_PLAN, which has the owner
 *       of every object: so the SQL text is not parsed, and synonyms do not have to be followed. An index gives the table it is on.<br>
 *       Every table is then sent for a lookup of its own.</li>
 *   <li><b>A table, view, materialized view or synonym</b> (dbname = the container / database name, objectName = <code>OWNER.NAME</code> or just <code>NAME</code>):<br>
 *       columns with their optimizer statistics, the table's size and statistics, its indexes and the size of every segment.<br>
 *       A view: its text and what it reads from (DBA_DEPENDENCIES). A synonym: what it points at.</li>
 * </ul>
 * All of it is read from views that need no extra-cost license, and that <code>SELECT_CATALOG_ROLE</code> can read.<br>
 * The texts are stored as text tables (as the Postgres inspector does), <code>OracleAbstract</code> reads them back with <code>ResultSetTableModel.parseTextTables()</code>.
 * <p>
 * Connected to CDB$ROOT the objects of a PDB are read from the CDB_ views (with CON_ID), see {@link #forContainer(String, long)}.
 */
public class ObjectLookupInspectorOracle
extends ObjectLookupInspectorAbstract
{
	private static final Logger _logger = LogManager.getLogger(MethodHandles.lookup().lookupClass());

	/** The type of a stored execution plan (the same as the other inspectors use) */
	public static final String TYPE_EXECUTION_PLAN = "SS";

	/** Every statement of a lookup gets this many seconds */
	private static final int QUERY_TIMEOUT_SEC = 30;

	/** A name without owner can exist in many schemas: how many of them we look up */
	private static final int MAX_OBJECTS_PER_NAME = 5;

	/**
	 * The schemas that Oracle itself owns: their objects are not looked up (the statements Oracle runs for itself are often among the top ones,
	 * and their tables are nothing a DBA can do anything about).<br>
	 * From 12.1 the lookup also asks Oracle (DBA_OBJECTS.ORACLE_MAINTAINED), which knows the schemas that are not in this list.<br>
	 * PUBLIC is not in the list: a synonym that someone has created for PUBLIC is followed, like any other synonym.
	 */
	private static final Set<String> ORACLE_OWNERS = new LinkedHashSet<>(Arrays.asList(
			"SYS", "SYSTEM", "XDB", "MDSYS", "CTXSYS", "ORDSYS", "ORDDATA", "ORDPLUGINS", "OLAPSYS", "WMSYS", "LBACSYS", "DVSYS", "DVF",
			"GSMADMIN_INTERNAL", "APPQOSSYS", "OJVMSYS", "AUDSYS", "DBSNMP", "DBSFWUSER", "OUTLN", "EXFSYS", "SI_INFORMTN_SCHEMA", "FLOWS_FILES", "GGSYS", "SYSMAN", "VECSYS"));

	@Override
	public boolean allowInspection(ObjectLookupQueueEntry entry)
	{
		if (entry == null || entry._dbname == null || entry._objectName == null)
			return false;

		// A statement: its execution plan
		if (PersistentCounterHandler.STATEMENT_CACHE_NAME.equals(entry._dbname))
		{
			entry.setStatementCacheEntry(true);
			return true;
		}

		return ! isDictionaryObject(entry._objectName);
	}

	/**
	 * The names we do NOT look up: the data dictionary and the dynamic performance views (SYS.xxx, DBA_xxx, V$xxx...), DUAL,
	 * and a table at the other end of a database link.<br>
	 * Also used by the report: a name like that is not worth a "was not found in the DDL Storage".
	 *
	 * @param objectName  NAME or OWNER.NAME, as it was written in a SQL text (any case, with or without double quotes)
	 */
	public static boolean isDictionaryObject(String objectName)
	{
		String[] ownerAndName = splitOwnerAndName(objectName);
		String   owner        = ownerAndName[0];
		String   name         = ownerAndName[1].toUpperCase();

		if (owner != null && (ORACLE_OWNERS.contains(owner.toUpperCase()) || owner.toUpperCase().startsWith("APEX_")))
			return true;

		if (name.equals("DUAL") || name.startsWith("V$") || name.startsWith("GV$") || name.startsWith("X$") || name.startsWith("V_$") || name.startsWith("GV_$")
				|| name.startsWith("DBA_") || name.startsWith("ALL_") || name.startsWith("USER_") || name.startsWith("CDB_"))
			return true;

		// TABLE is what a SQL parser makes of: from table(DBMS_XPLAN.DISPLAY_CURSOR(...))
		return name.indexOf('@') >= 0 || name.equals("TABLE");
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
			if (qe.isStatementCacheEntry() || PersistentCounterHandler.STATEMENT_CACHE_NAME.equals(qe._dbname))
				return lookupExecutionPlan(conn, qe, pch);

			return lookupObject(conn, qe, pch);
		}
		catch (SQLException ex)
		{
			_logger.warn("DDL Lookup: Problems with dbname='" + qe._dbname + "', objectName='" + qe._objectName + "', source='" + qe._source + "'. Skipping this object. Caught: " + ex.getMessage().trim());
			return Collections.emptyList();
		}
	}

	//---------------------------------------------------------------------------------------
	// A statement: the execution plan, and the tables it uses
	//---------------------------------------------------------------------------------------
	private List<DdlDetails> lookupExecutionPlan(DbxConnection conn, ObjectLookupQueueEntry qe, PersistentCounterHandler pch)
	throws SQLException
	{
		// objectName: SQL_ID or SQL_ID_PLANHASHVALUE (a statement can have more than one plan during a day: every plan is stored)
		String sqlId         = qe._objectName;
		String planHashValue = null;
		int    pos           = qe._objectName.indexOf('_');
		if (pos > 0)
		{
			sqlId         = qe._objectName.substring(0, pos);
			planHashValue = qe._objectName.substring(pos + 1);
		}

		// This goes into SQL as text: so nothing but what a SQL_ID and a plan hash value can be
		if ( ! sqlId.matches("[0-9a-z]{13}") || (planHashValue != null && ! planHashValue.matches("[0-9]+")) )
		{
			pch.markDdlDetailsAsDiscarded(PersistentCounterHandler.STATEMENT_CACHE_NAME, qe._objectName);
			return Collections.emptyList();
		}

		boolean hasConId = conn.getDbmsVersionInfo().getLongVersion() >= Ver.ver(12,1);

		// One of the child cursors that has this plan: the one that was used last
		long childNumber = -1;
		long conId       = 0;
		String sql = ""
			+ "select * from ( \n"
			+ "    select q.CHILD_NUMBER, " + (hasConId ? "q.CON_ID" : "0") + " \n"
			+ "    from V$SQL q \n"
			+ "    where q.SQL_ID = '" + sqlId + "' \n"
			+ (planHashValue == null ? "" : "      and q.PLAN_HASH_VALUE = " + planHashValue + " \n")
			+ "    order by q.LAST_ACTIVE_TIME desc \n"
			+ ") where rownum = 1";

		try (Statement stmnt = conn.createStatement())
		{
			stmnt.setQueryTimeout(QUERY_TIMEOUT_SEC);
			try (ResultSet rs = stmnt.executeQuery(sql))
			{
				while (rs.next())
				{
					childNumber = rs.getLong(1);
					conId       = rs.getLong(2);
				}
			}
		}

		// The statement is no longer in the shared pool: nothing to get (and no need to ask again)
		if (childNumber == -1)
		{
			pch.markDdlDetailsAsDiscarded(PersistentCounterHandler.STATEMENT_CACHE_NAME, qe._objectName);
			_logger.info("DDL Lookup: The statement SQL_ID='" + sqlId + "', PLAN_HASH_VALUE=" + planHashValue + " is not in V$SQL any more. No execution plan is stored for it. source='" + qe._source + "'.");
			return Collections.emptyList();
		}

		// The plan, as Oracle prints it
		StringBuilder planText = new StringBuilder();
		try (Statement stmnt = conn.createStatement())
		{
			stmnt.setQueryTimeout(QUERY_TIMEOUT_SEC);
			try (ResultSet rs = stmnt.executeQuery("select PLAN_TABLE_OUTPUT from table(DBMS_XPLAN.DISPLAY_CURSOR('" + sqlId + "', " + childNumber + ", 'TYPICAL'))"))
			{
				while (rs.next())
					planText.append(rs.getString(1)).append("\n");
			}
		}

		// "SQL_ID: xxx, child number: n cannot be found": the cursor went away between the two statements
		if (planText.indexOf("Plan hash value") == -1)
		{
			pch.markDdlDetailsAsDiscarded(PersistentCounterHandler.STATEMENT_CACHE_NAME, qe._objectName);
			return Collections.emptyList();
		}

		// The container the statement runs in: that is the "dbname" of its tables
		// (12.1 and above: V$CONTAINERS has one row also when this is NOT a Container Database, with CON_ID 0)
		String dbname = null;
		try (Statement stmnt = conn.createStatement())
		{
			stmnt.setQueryTimeout(QUERY_TIMEOUT_SEC);
			try (ResultSet rs = stmnt.executeQuery(hasConId ? "select c.NAME from V$CONTAINERS c where c.CON_ID = " + conId : "select SYS_CONTEXT('USERENV', 'DB_NAME') from DUAL"))
			{
				while (rs.next())
					dbname = rs.getString(1);
			}
		}

		// The objects of the plan. Tables and views as they are, an index gives its table
		Set<String> tables  = new LinkedHashSet<>();
		Set<String> indexes = new LinkedHashSet<>();
		sql = ""
			+ "select distinct p.OBJECT_OWNER, p.OBJECT_NAME, p.OBJECT_TYPE \n"
			+ "from V$SQL_PLAN p \n"
			+ "where p.SQL_ID       = '" + sqlId + "' \n"
			+ "  and p.CHILD_NUMBER = " + childNumber + " \n"
			+ (hasConId ? "  and p.CON_ID       = " + conId + " \n" : "")
			+ "  and p.OBJECT_OWNER is not null \n"
			+ "  and p.OBJECT_NAME  is not null";

		try (Statement stmnt = conn.createStatement())
		{
			stmnt.setQueryTimeout(QUERY_TIMEOUT_SEC);
			try (ResultSet rs = stmnt.executeQuery(sql))
			{
				while (rs.next())
				{
					String owner = rs.getString(1);
					String name  = rs.getString(2);
					String type  = rs.getString(3);

					// Oracle's own schemas, the X$ tables ('TABLE (FIXED)', 'INDEX (FIXED)'), and a name with ':' which is not an object
					// (a transformation of the optimizer, like ':BF0000' for a Bloom filter)
					if (isDictionaryObject(owner + "." + name) || name.startsWith(":") || (type != null && type.contains("FIXED")))
						continue;

					if (type != null && type.startsWith("INDEX"))
						indexes.add(q(owner) + ", " + q(name));
					else
						tables.add(owner + "." + name);
				}
			}
		}

		if ( ! indexes.isEmpty() )
		{
			sql = forContainer(""
				+ "select distinct i.TABLE_OWNER, i.TABLE_NAME \n"
				+ "from DBA_INDEXES i \n"
				+ "where (i.OWNER, i.INDEX_NAME) in ((" + StringUtil.toCommaStr(indexes, "), (") + ")) /*CON:i*/", getContainerIdForLookup(conn, conId));

			try (Statement stmnt = conn.createStatement())
			{
				stmnt.setQueryTimeout(QUERY_TIMEOUT_SEC);
				try (ResultSet rs = stmnt.executeQuery(sql))
				{
					while (rs.next())
						tables.add(rs.getString(1) + "." + rs.getString(2));
				}
			}
			catch (SQLException ex)
			{
				_logger.warn("DDL Lookup: Problems getting the tables of the indexes in the plan of SQL_ID='" + sqlId + "'. The tables that are only read via an index are not looked up. Caught: " + ex.getMessage().trim());
			}
		}

		// Send every table for a lookup of its own
		if (StringUtil.hasValue(dbname))
		{
			for (String table : tables)
				pch.addDdl(dbname, table, this.getClass().getSimpleName() + ".plan", qe._objectName, qe._dependLevel + 1);
		}

		// What to store
		Timestamp now = new Timestamp(System.currentTimeMillis());

		DdlDetails storeEntry = new DdlDetails(PersistentCounterHandler.STATEMENT_CACHE_NAME, qe._objectName);
		storeEntry.setCrdate      ( now );
		storeEntry.setSampleTime  ( now );
		storeEntry.setSource      ( qe._source );
		storeEntry.setDependLevel ( qe._dependLevel );
		storeEntry.setOwner       ( dbname );                // where the statement runs (as the SQL Server inspector does)
		storeEntry.setType        ( TYPE_EXECUTION_PLAN );
		storeEntry.setExtraInfoText( planText.toString() );  // the plan (in 'extraInfoText', as the other inspectors does)
		storeEntry.setObjectText  ( "TableList: " + StringUtil.toCommaStr(tables) );

		List<DdlDetails> storeList = new ArrayList<>();
		storeList.add(storeEntry);
		return storeList;
	}

	//---------------------------------------------------------------------------------------
	// A table, view, materialized view or synonym
	//---------------------------------------------------------------------------------------
	private List<DdlDetails> lookupObject(DbxConnection conn, ObjectLookupQueueEntry qe, PersistentCounterHandler pch)
	throws SQLException
	{
		final String dbname           = qe._dbname;
		final String originObjectName = qe._objectName;

		String[] ownerAndName = splitOwnerAndName(originObjectName);
		String   owner        = ownerAndName[0];
		String   name         = ownerAndName[1];

		// Connected to CDB$ROOT: which container is it. Otherwise 0 (we read the DBA_ views of where we are)
		long conId = 0;
		if (((DbmsVersionInfoOracle) conn.getDbmsVersionInfo()).isCdbRoot())
		{
			try (Statement stmnt = conn.createStatement())
			{
				stmnt.setQueryTimeout(QUERY_TIMEOUT_SEC);
				try (ResultSet rs = stmnt.executeQuery("select c.CON_ID from V$CONTAINERS c where c.NAME = " + q(dbname)))
				{
					while (rs.next())
						conId = rs.getLong(1);
				}
			}

			if (conId == 0)
			{
				pch.markDdlDetailsAsDiscarded(dbname, originObjectName);
				_logger.info("DDL Lookup: Can't find the container '" + dbname + "' in V$CONTAINERS, for objectName='" + originObjectName + "', source='" + qe._source + "'. Skipping DDL Storage of this object.");
				return Collections.emptyList();
			}
		}

		//----------------------------------------------------------------------------------
		// What is it: a table, a view, a materialized view or a synonym
		// The name as it was given, and if that finds nothing in UPPERCASE (which is how Oracle stores a name that was not quoted)
		// (a materialized view is in DBA_OBJECTS both as a TABLE and as a MATERIALIZED VIEW: the order by makes the TABLE come first)
		//----------------------------------------------------------------------------------
		List<DdlDetails> objectList = new ArrayList<>();
		Set<String>      seen       = new LinkedHashSet<>();

		// 12.1 and above: Oracle tells which objects are its own
		boolean hasOracleMaintained = conn.getDbmsVersionInfo().getLongVersion() >= Ver.ver(12,1);

		for (int attempt = 0; attempt < 2 && objectList.isEmpty(); attempt++)
		{
			String tryOwner = owner == null ? null : (attempt == 0 ? owner : owner.toUpperCase());
			String tryName  =                         attempt == 0 ? name  : name .toUpperCase();
			if (attempt == 1 && tryName.equals(name) && StringUtil.equalsAny(tryOwner, owner))
				break;

			String sql = forContainer(""
				+ "select o.OWNER, o.OBJECT_NAME, o.OBJECT_TYPE, o.OBJECT_ID, o.CREATED \n"
				+ "from DBA_OBJECTS o \n"
				+ "where o.OBJECT_NAME = " + q(tryName) + " \n"
				+ (tryOwner != null ? "  and o.OWNER = " + q(tryOwner) + " \n" : "  and o.OWNER not in ('" + String.join("', '", ORACLE_OWNERS) + "') \n")
				+ (hasOracleMaintained ? "  and o.ORACLE_MAINTAINED = 'N' \n" : "")
				+ "  and o.OBJECT_TYPE in ('TABLE', 'VIEW', 'MATERIALIZED VIEW', 'SYNONYM') /*CON:o*/ \n"
				+ "order by case o.OBJECT_TYPE when 'TABLE' then 1 when 'MATERIALIZED VIEW' then 2 when 'VIEW' then 3 else 4 end, o.OWNER", conId);

			try (Statement stmnt = conn.createStatement())
			{
				stmnt.setQueryTimeout(QUERY_TIMEOUT_SEC);
				try (ResultSet rs = stmnt.executeQuery(sql))
				{
					while (rs.next() && objectList.size() < MAX_OBJECTS_PER_NAME)
					{
						if ( ! seen.add(rs.getString(1) + "." + rs.getString(2)) )
							continue;

						DdlDetails entry = new DdlDetails();
						entry.setDbname          ( dbname );
						entry.setSearchDbname    ( dbname );
						entry.setOwner           ( rs.getString(1) );
						entry.setObjectName      ( rs.getString(2) );
						entry.setSearchObjectName( originObjectName ); // NOT stored, used for: isDdlDetailsStored(), markDdlDetailsAsStored()
						entry.setType            ( rs.getString(3) );
						entry.setObjectId        ( rs.getInt(4) );
						entry.setCrdate          ( rs.getTimestamp(5) );
						entry.setSource          ( qe._source );
						entry.setDependParent    ( qe._dependParent );
						entry.setDependLevel     ( qe._dependLevel );
						entry.setSampleTime      ( new Timestamp(System.currentTimeMillis()) );
						objectList.add(entry);
					}
				}
			}
		}

		if (objectList.isEmpty())
		{
			// In the future, do NOT do lookup of this name
			pch.markDdlDetailsAsDiscarded(dbname, originObjectName);
			_logger.info("DDL Lookup: Can't find any table, view or synonym for dbname='" + dbname + "', objectName='" + originObjectName + "', source='" + qe._source + "', dependLevel=" + qe._dependLevel + ". Skipping DDL Storage of this object. Also adding it to the 'discard' list.");
			return Collections.emptyList();
		}

		//----------------------------------------------------------------------------------
		// Get the details of what we found
		//----------------------------------------------------------------------------------
		for (DdlDetails entry : objectList)
		{
			String type   = entry.getType();
			String qOwner = q(entry.getOwner());
			String qName  = q(entry.getObjectName());

			if ("TABLE".equals(type))
			{
				//--------------------------------------------
				// objectText: the columns, with the optimizer statistics of each
				// NOTE for all the text tables: numbers and dates are made into text by Oracle (TO_CHAR), and sizes are whole KB.
				//      Otherwise they are formatted the way of the collector's country (340 000 and 128,3 in Sweden), and the one that reads the text may be in another.
				String sqlColumns = forContainer(""
					+ "select \n"
					+ "     TO_CHAR(c.COLUMN_ID) as COLUMN_ID \n"
					+ "    ,c.COLUMN_NAME \n"
					+ "    ,c.DATA_TYPE \n"
					+ "         || case when c.DATA_TYPE in ('VARCHAR2', 'NVARCHAR2', 'CHAR', 'NCHAR') then '(' || c.CHAR_LENGTH || case c.CHAR_USED when 'C' then ' CHAR' end || ')' \n"
					+ "                 when c.DATA_TYPE = 'RAW'                                       then '(' || c.DATA_LENGTH || ')' \n"
					+ "                 when c.DATA_TYPE = 'NUMBER' and c.DATA_PRECISION is not null   then '(' || c.DATA_PRECISION || case when c.DATA_SCALE > 0 then ',' || c.DATA_SCALE end || ')' \n"
					+ "            end as DATA_TYPE \n"
					+ "    ,c.NULLABLE \n"
					+ "    ,TO_CHAR(c.NUM_DISTINCT)                         as NUM_DISTINCT \n"
					+ "    ,TO_CHAR(c.NUM_NULLS)                            as NUM_NULLS \n"
					+ "    ,c.HISTOGRAM \n"
					+ "    ,TO_CHAR(c.AVG_COL_LEN)                          as AVG_COL_LEN \n"
					+ "    ,TO_CHAR(c.LAST_ANALYZED, 'YYYY-MM-DD HH24:MI')  as LAST_ANALYZED \n"
					+ "from DBA_TAB_COLUMNS c \n"
					+ "where c.OWNER      = " + qOwner + " \n"
					+ "  and c.TABLE_NAME = " + qName  + " /*CON:c*/ \n"
					+ "order by c.COLUMN_ID", conId);

				try
				{
					entry.setObjectText( ResultSetTableModel.executeQuery(conn, sqlColumns, QUERY_TIMEOUT_SEC, "columns").toAsciiTableString() );
				}
				catch (SQLException ex)
				{
					entry.setObjectText( ex.toString() );
					_logger.warn("DDL Lookup. Problems getting the COLUMNS of table " + entry.getSchemaAndObjectName() + " in '" + dbname + "', source='" + qe._source + "'. Caught: " + ex.getMessage().trim());
				}

				//--------------------------------------------
				// extraInfoText: the table, its indexes, and the size of every segment (the table, its indexes and its LOBs)
				String sqlTable = forContainer(""
					+ "select \n"
					+ "     t.OWNER \n"
					+ "    ,t.TABLE_NAME \n"
					+ "    ,TO_CHAR(t.NUM_ROWS)                             as NUM_ROWS \n"
					+ "    ,TO_CHAR(t.BLOCKS)                               as BLOCKS \n"
					+ "    ,TO_CHAR(t.AVG_ROW_LEN)                          as AVG_ROW_LEN \n"
					+ "    ,TO_CHAR(t.LAST_ANALYZED, 'YYYY-MM-DD HH24:MI')  as LAST_ANALYZED \n"
					+ "    ,(select max(s.STALE_STATS) from DBA_TAB_STATISTICS s where s.OWNER = t.OWNER and s.TABLE_NAME = t.TABLE_NAME and s.OBJECT_TYPE = 'TABLE' /*CON:s*/) as STALE_STATS \n"
					+ "    ,t.PARTITIONED \n"
					+ "    ,(select TO_CHAR(count(*)) from DBA_TAB_PARTITIONS p where p.TABLE_OWNER = t.OWNER and p.TABLE_NAME = t.TABLE_NAME /*CON:p*/) as PARTITION_COUNT \n"
					+ "    ,(select listagg(k.COLUMN_NAME, ', ') within group (order by k.COLUMN_POSITION) from DBA_PART_KEY_COLUMNS k where k.OWNER = t.OWNER and k.NAME = t.TABLE_NAME and k.OBJECT_TYPE = 'TABLE' /*CON:k*/) as PARTITION_KEYS \n"
					+ "    ,t.TEMPORARY \n"
					+ "    ,t.IOT_TYPE \n"
					+ "    ,t.COMPRESSION \n"
					+ "    ,trim(t.DEGREE) as DEGREE \n"
					+ "    ,t.TABLESPACE_NAME \n"
					+ "from DBA_TABLES t \n"
					+ "where t.OWNER      = " + qOwner + " \n"
					+ "  and t.TABLE_NAME = " + qName  + " /*CON:t*/", conId);

				String sqlIndexes = forContainer(""
					+ "select \n"
					+ "     i.OWNER as INDEX_OWNER \n"
					+ "    ,i.INDEX_NAME \n"
					+ "    ,i.INDEX_TYPE \n"
					+ "    ,i.UNIQUENESS \n"
					+ "    ,(select max(k.CONSTRAINT_TYPE) from DBA_CONSTRAINTS k where k.OWNER = i.TABLE_OWNER and k.TABLE_NAME = i.TABLE_NAME and k.INDEX_NAME = i.INDEX_NAME and k.CONSTRAINT_TYPE in ('P', 'U') /*CON:k*/) as CONSTRAINT_TYPE \n"
					+ "    ,(select listagg(ic.COLUMN_NAME || case ic.DESCEND when 'DESC' then ' DESC' end, ', ') within group (order by ic.COLUMN_POSITION) \n"
					+ "      from DBA_IND_COLUMNS ic where ic.INDEX_OWNER = i.OWNER and ic.INDEX_NAME = i.INDEX_NAME /*CON:ic*/) as INDEX_KEYS \n"
					+ "    ,TO_CHAR(i.NUM_ROWS)                             as NUM_ROWS \n"
					+ "    ,TO_CHAR(i.DISTINCT_KEYS)                        as DISTINCT_KEYS \n"
					+ "    ,TO_CHAR(i.BLEVEL)                               as BLEVEL \n"
					+ "    ,TO_CHAR(i.LEAF_BLOCKS)                          as LEAF_BLOCKS \n"
					+ "    ,TO_CHAR(i.CLUSTERING_FACTOR)                    as CLUSTERING_FACTOR \n"
					+ "    ,i.STATUS \n"
					+ "    ,i.VISIBILITY \n"
					+ "    ,i.PARTITIONED \n"
					+ "    ,i.COMPRESSION \n"
					+ "    ,TO_CHAR(i.LAST_ANALYZED, 'YYYY-MM-DD HH24:MI')  as LAST_ANALYZED \n"
					+ "    ,i.TABLESPACE_NAME \n"
					+ "from DBA_INDEXES i \n"
					+ "where i.TABLE_OWNER = " + qOwner + " \n"
					+ "  and i.TABLE_NAME  = " + qName  + " /*CON:i*/ \n"
					+ "order by i.INDEX_NAME", conId);

				// ONE read of DBA_SEGMENTS for all the segments (that view is the expensive one)
				String sqlSegments = forContainer(""
					+ "select \n"
					+ "     s.OWNER as SEGMENT_OWNER \n"
					+ "    ,s.SEGMENT_NAME \n"
					+ "    ,s.SEGMENT_TYPE \n"
					+ "    ,TO_CHAR(count(*))                   as SEGMENT_COUNT \n"
					+ "    ,TO_CHAR(round(sum(s.BYTES) / 1024)) as SIZE_KB \n"
					+ "from DBA_SEGMENTS s \n"
					+ "where (s.OWNER, s.SEGMENT_NAME) in ( \n"
					+ "          select t.OWNER, t.TABLE_NAME   from DBA_TABLES  t where t.OWNER       = " + qOwner + " and t.TABLE_NAME = " + qName + " /*CON:t*/ \n"
					+ "          union all \n"
					+ "          select i.OWNER, i.INDEX_NAME   from DBA_INDEXES i where i.TABLE_OWNER = " + qOwner + " and i.TABLE_NAME = " + qName + " /*CON:i*/ \n"
					+ "          union all \n"
					+ "          select l.OWNER, l.SEGMENT_NAME from DBA_LOBS    l where l.OWNER       = " + qOwner + " and l.TABLE_NAME = " + qName + " /*CON:l*/ \n"
					+ "      ) /*CON:s*/ \n"
					+ "group by s.OWNER, s.SEGMENT_NAME, s.SEGMENT_TYPE \n"
					+ "order by s.SEGMENT_TYPE desc, s.SEGMENT_NAME", conId);

				// Each of the three on its own: if one fails, the others are still stored
				String extraInfo = "";
				for (String[] part : new String[][] { {"table", sqlTable}, {"indexes", sqlIndexes}, {"segments", sqlSegments} })
				{
					try
					{
						extraInfo += ResultSetTableModel.executeQuery(conn, part[1], QUERY_TIMEOUT_SEC, part[0]).toAsciiTableString() + "\n";
					}
					catch (SQLException ex)
					{
						extraInfo += "Could not get the " + part[0] + ": " + ex.getMessage().trim() + "\n\n";
						_logger.warn("DDL Lookup. Problems getting the " + part[0].toUpperCase() + " of table " + entry.getSchemaAndObjectName() + " in '" + dbname + "', source='" + qe._source + "'. Caught: " + ex.getMessage().trim());
					}
				}
				entry.setExtraInfoText(extraInfo);
			}
			else if ("VIEW".equals(type) || "MATERIALIZED VIEW".equals(type))
			{
				//--------------------------------------------
				// objectText: the text of the view
				// The CDB_ views do not have the LONG column TEXT: there it is TEXT_VC, which is the first 4000 characters
				String sqlText = "VIEW".equals(type)
					? forContainer("select " + (conId > 0 ? "v.TEXT_VC" : "v.TEXT") + " from DBA_VIEWS v where v.OWNER = " + qOwner + " and v.VIEW_NAME = " + qName + " /*CON:v*/", conId)
					: forContainer("select " + (conId > 0 ? "v.QUERY"   : "v.QUERY") + " from DBA_MVIEWS v where v.OWNER = " + qOwner + " and v.MVIEW_NAME = " + qName + " /*CON:v*/", conId);

				try (Statement stmnt = conn.createStatement())
				{
					stmnt.setQueryTimeout(QUERY_TIMEOUT_SEC);
					try (ResultSet rs = stmnt.executeQuery(sqlText))
					{
						while (rs.next())
							entry.setObjectText( rs.getString(1) );
					}
				}
				catch (SQLException ex)
				{
					entry.setObjectText( ex.toString() );
					_logger.warn("DDL Lookup. Problems getting the TEXT of " + type + " " + entry.getSchemaAndObjectName() + " in '" + dbname + "', source='" + qe._source + "'. Caught: " + ex.getMessage().trim());
				}

				//--------------------------------------------
				// extraInfoText: what it reads from (Oracle knows that: no need to parse the text). Each of them is sent for a lookup of its own
				String sqlDepends = forContainer(""
					+ "select distinct d.REFERENCED_OWNER, d.REFERENCED_NAME \n"
					+ "from DBA_DEPENDENCIES d \n"
					+ "where d.OWNER = " + qOwner + " \n"
					+ "  and d.NAME  = " + qName  + " \n"
					+ "  and d.TYPE  = " + q(type) + " \n"
					+ "  and d.REFERENCED_TYPE in ('TABLE', 'VIEW', 'MATERIALIZED VIEW', 'SYNONYM') \n"
					+ "  and not (d.REFERENCED_OWNER = d.OWNER and d.REFERENCED_NAME = d.NAME) /*CON:d*/", conId); // (a materialized view depends on its own table)

				entry.setExtraInfoText( "TableList: " + StringUtil.toCommaStr(lookupReferences(conn, sqlDepends, entry, pch)) );
			}
			else if ("SYNONYM".equals(type))
			{
				// What it points at. Something at the other end of a database link is not looked up
				String sqlSynonym = forContainer(""
					+ "select s.TABLE_OWNER, s.TABLE_NAME \n"
					+ "from DBA_SYNONYMS s \n"
					+ "where s.OWNER        = " + qOwner + " \n"
					+ "  and s.SYNONYM_NAME = " + qName  + " \n"
					+ "  and s.DB_LINK is null /*CON:s*/", conId);

				List<String> target = lookupReferences(conn, sqlSynonym, entry, pch);

				entry.setObjectText   ( "SYNONYM " + entry.getSchemaAndObjectName() + " FOR " + (target.isEmpty() ? "something at the other end of a database link" : target.get(0)) );
				entry.setExtraInfoText( "TableList: " + StringUtil.toCommaStr(target) );
			}
		}

		return objectList;
	}

	/**
	 * Run a SQL that returns OWNER, NAME of what a view or a synonym refers to, and send each of them for a lookup of its own
	 * @return the references, as OWNER.NAME
	 */
	private List<String> lookupReferences(DbxConnection conn, String sql, DdlDetails entry, PersistentCounterHandler pch)
	{
		List<String> references = new ArrayList<>();

		try (Statement stmnt = conn.createStatement())
		{
			stmnt.setQueryTimeout(QUERY_TIMEOUT_SEC);
			try (ResultSet rs = stmnt.executeQuery(sql))
			{
				while (rs.next())
					references.add(rs.getString(1) + "." + rs.getString(2));
			}
		}
		catch (SQLException ex)
		{
			_logger.warn("DDL Lookup. Problems getting what " + entry.getType() + " " + entry.getSchemaAndObjectName() + " in '" + entry.getDbname() + "' refers to. Caught: " + ex.getMessage().trim());
		}

		for (String reference : references)
			pch.addDdl(entry.getDbname(), reference, this.getClass().getSimpleName() + ".resolve." + entry.getType().toLowerCase().replace(' ', '_'), entry.getSchemaAndObjectName(), entry.getDependLevel() + 1);

		return references;
	}

	//---------------------------------------------------------------------------------------
	// Helpers
	//---------------------------------------------------------------------------------------

	/** @return {owner, name}: owner is null when the name has none. Double quotes are removed */
	private static String[] splitOwnerAndName(String objectName)
	{
		String str = objectName.trim().replace("\"", "");

		int pos = str.lastIndexOf('.');
		if (pos <= 0)
			return new String[] { null, str };

		// DB.OWNER.NAME: the owner is the part next to the name
		String owner = str.substring(0, pos);
		if (owner.indexOf('.') >= 0)
			owner = owner.substring(owner.lastIndexOf('.') + 1);

		return new String[] { owner, str.substring(pos + 1) };
	}

	/** A SQL string constant */
	private static String q(String str)
	{
		return "'" + str.replace("'", "''") + "'";
	}

	/** The container to read the dictionary of: only when we are connected to CDB$ROOT (otherwise 0: the DBA_ views of where we are) */
	private static long getContainerIdForLookup(DbxConnection conn, long conId)
	{
		return ((DbmsVersionInfoOracle) conn.getDbmsVersionInfo()).isCdbRoot() ? conId : 0;
	}

	/**
	 * The SQL of the lookups are written for the DBA_ views, with a marker <code>/*CON:alias*&#47;</code> after the where clause of every view.
	 * <ul>
	 *   <li>conId = 0: the marker is removed, the SQL is used as it is (not a Container Database, or connected to the PDB itself)</li>
	 *   <li>conId > 0: connected to CDB$ROOT. DBA_ becomes CDB_ and the marker becomes: <code>and alias.CON_ID = n</code></li>
	 * </ul>
	 */
	private static String forContainer(String sql, long conId)
	{
		if (conId <= 0)
			return sql.replaceAll("/\\*CON:\\w+\\*/", "");

		return sql.replace("DBA_", "CDB_").replaceAll("/\\*CON:(\\w+)\\*/", "and $1.CON_ID = " + conId);
	}
}
