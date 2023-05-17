/*******************************************************************************
 * Copyright (C) 2010-2020 Goran Schwarz
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
package com.asetune.pcs.inspection;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;

import org.apache.log4j.Logger;

import com.asetune.Version;
import com.asetune.cm.CounterSampleCatalogIteratorPostgres;
import com.asetune.pcs.DdlDetails;
import com.asetune.pcs.ObjectLookupQueueEntry;
import com.asetune.pcs.PersistentCounterHandler;
import com.asetune.sql.SqlObjectName;
import com.asetune.sql.SqlParserUtils;
import com.asetune.sql.conn.DbxConnection;
import com.asetune.sql.conn.info.DbmsVersionInfo;
import com.asetune.utils.AseSqlScript;
import com.asetune.utils.Configuration;
import com.asetune.utils.DbUtils;
import com.asetune.utils.StringUtil;
import com.asetune.utils.Ver;

public class ObjectLookupInspectorPostgres
extends ObjectLookupInspectorAbstract
{
	private static Logger _logger = Logger.getLogger(ObjectLookupInspectorPostgres.class);

	public static final String  PROPKEY_xmlPlan_parseAndSendTables  = Version.getAppName() + "." + ObjectLookupInspectorPostgres.class.getSimpleName() + ".xmlPlan.parse.send.tables";
	public static final boolean DEFAULT_xmlPlan_parseAndSendTables  = true;

	public static final String  PROPKEY_cursor_parseAndSendTables   = Version.getAppName() + "." + ObjectLookupInspectorPostgres.class.getSimpleName() + ".cursor.parse.send.tables";
	public static final boolean DEFAULT_cursor_parseAndSendTables   = true;

	public static final String  PROPKEY_view_parseAndSendTables     = Version.getAppName() + "." + ObjectLookupInspectorPostgres.class.getSimpleName() + ".view.parse.send.tables";
	public static final boolean DEFAULT_view_parseAndSendTables     = true;

	public static final String  PROPKEY_function_parseAndSendTables = Version.getAppName() + "." + ObjectLookupInspectorPostgres.class.getSimpleName() + ".function.parse.send.tables";
	public static final boolean DEFAULT_function_parseAndSendTables = true;

//	private long    _dbmsVersion = 0;

	@Override
	public boolean allowInspection(ObjectLookupQueueEntry entry)
	{
		if (entry == null)
			return false;

		SqlObjectName sqlObj = new SqlObjectName(entry._objectName, DbUtils.DB_PROD_NAME_POSTGRES, "\"", false, true, true);
		entry._sqlObject = sqlObj;

		String schemaName = sqlObj.getSchemaName();
		String objectName = sqlObj.getObjectName();

		// schema 'pg_catalog' or 'information_schema' do not lookup
		if ("pg_catalog"        .equalsIgnoreCase(schemaName)) return false;
		if ("information_schema".equalsIgnoreCase(schemaName)) return false;

		// All tables starting with "pg_" must be a Postgres System table... 
		// so do not bother to look this up.
		if (StringUtil.startsWithIgnoreBlankIgnoreCase(objectName, "pg_"))
			return false;
		
		// allow inspection
		return true;
	}

	/**
	 * When a new connection has been made, install some extra "stuff" in ASE if it doesn't already exists
	 */
	@Override
	public void onConnect(DbxConnection conn)
	{
//		_dbmsVersion = conn.getDbmsVersionNumber();
	}

	/**
	 * This is a wrapper method, used to check out/in connections to a connection pool!
	 */
	@Override
	public List<DdlDetails> doObjectInfoLookup(DbxConnection sourceConn, ObjectLookupQueueEntry qe, PersistentCounterHandler pch)
	{
		final String dbname = qe._dbname;

		DbxConnection conn = null;
		try
		{
			// Using the Connection pool already created in 'CounterSampleCatalogIteratorPostgres'
			conn =  CounterSampleCatalogIteratorPostgres.getConnection(null, sourceConn, dbname);

			return doObjectInfoLookup_doWork(conn, qe, pch);
		}
		catch (Exception ex)
		{
			_logger.error("DDL Lookup: Problems getting a connection to Postgres database '" + dbname + "'.", ex);
			return Collections.emptyList();   // <<<<<<<<<<------------<<<<<<<<<<------------<<<<<<<<<<------------
		}
		finally
		{
			CounterSampleCatalogIteratorPostgres.releaseConnection(null, conn, dbname);
		}
	}
	
	public List<DdlDetails> doObjectInfoLookup_doWork(DbxConnection conn, ObjectLookupQueueEntry qe, PersistentCounterHandler pch)
	{
		final String dbname       = qe._dbname;
		final String objectName   = qe._objectName;
		final String source       = qe._source;
		final String dependParent = qe._dependParent;
		final int    dependLevel  = qe._dependLevel;

//System.out.println("doObjectInfoLookup_doWork: dbname='"+dbname+"', objectName='"+objectName+"', source='"+source+"'.");
//boolean notYetImplemented = true;
//if (notYetImplemented)
//	return Collections.emptyList();   // <<<<<<<<<<------------<<<<<<<<<<------------<<<<<<<<<<------------

		// In Postgres we need 1 connection per database we want to look things up (no cross database visibility)
		// Can we re-use the connection pool in CounterSampleCatalogIteratorPostgres, to get a database specific connection
		// NOTE: only if it can be done safely (so we don't execute Statements at same time from CmXxxx objects and here

		// Get a list of DBMS Object we want to get information for!
		List<DdlDetails> objectList = getDbmsObjectList(conn, dbname, objectName, source, dependParent, dependLevel);

		// The object was NOT found
		if (objectList.isEmpty())
		{
			// If the future, do NOT do lookup of this table.
			pch.markDdlDetailsAsDiscarded(dbname, objectName);

			_logger.info("DDL Lookup: Can't find any information for dbname='" + dbname + "', objectName='" + objectName + "', source='" + source + "', dependLevel=" + dependLevel + ". Skipping DDL Storage of this object. Also adding it to the 'discard' list.");
			return Collections.emptyList();   // <<<<<<<<<<------------<<<<<<<<<<------------<<<<<<<<<<------------
		}

		// Add entries that should be stored to this list
		List<DdlDetails> returnList = new ArrayList<>();

		//----------------------------------------------------------------------------------
		// Do lookups of the entries found in the DBMS Dictionary
		//----------------------------------------------------------------------------------
		for (DdlDetails storeEntry : objectList)
		{
			// from https://www.postgresql.org/docs/14/catalog-pg-class.html
			//--------------------------------------------------
			//   r = ordinary table, 
			//   i = index, 
			//   S = sequence, 
			//   t = TOAST table, 
			//   v = view, 
			//   m = materialized view, 
			//   c = composite type, 
			//   f = foreign table, 
			//   p = partitioned table, 
			//   I = partitioned index
			//--------------------------------------------------
			//  TR = Trigger       --- added in method: getDbmsObjectList
			//  FN = function      --- added in method: getDbmsObjectList
			//  P  = procedure     --- added in method: getDbmsObjectList
			//--------------------------------------------------
			String type = storeEntry.getType();

			if ( "r".equals(type) || "p".equals(type) )
			{
				// This should be stored
				returnList.add(storeEntry);

				//--------------------------------------------
				// get Table definition (DDL or column specification)
				String sql_tableColumnInfo = ""
				    + "select \n"
				    + "     ordinal_position \n"
				    + "    ,column_name \n"
				    + "    ,data_type \n"
				    + "    ,character_maximum_length \n"
				    + "    ,numeric_precision \n"
				    + "    ,numeric_scale \n"
				    + "    ,datetime_precision \n"
				    + "    ,column_default \n"
				    + "    ,is_nullable \n"
				    + "from INFORMATION_SCHEMA.COLUMNS \n"
				    + "where 1=1 \n"
				    + "  and table_schema = '" + storeEntry.getSchemaName() + "' \n"   // the *real* identifier (Lower or Mixed case) is fetched in getDbmsObjectList(...)
				    + "  and table_name   = '" + storeEntry.getObjectName() + "' \n"   // the *real* identifier (Lower or Mixed case) is fetched in getDbmsObjectList(...)
				    + "";

				try	(AseSqlScript ss = new AseSqlScript(conn, 10)) 
				{
					ss.setRsAsAsciiTable(true);
					storeEntry.setObjectText( ss.executeSqlStr(sql_tableColumnInfo, true) ); 
				} 
				catch (SQLException e) 
				{ 
					storeEntry.setObjectText( e.toString() ); 
					_logger.warn("DDL Lookup. Problems Getting TABLE BASIC INFO information for: TableName='" + storeEntry.getObjectName() + "', dbname='" + dbname + "', objectName='" + objectName + "', source='" + source + "', dependLevel=" + dependLevel + ". Caught: " + e);
				}


				//--------------------------------------------
				// get Table and Index SIZE (and index columns/DDL)
				String sql_tableSize = ""
					+ "SELECT \n"
					+ "     current_database() as dbname \n"
					+ "    ,n.nspname     AS schema_name \n"
					+ "    ,c.relname     AS table_name \n"
					+ "    ,c.reltuples AS row_estimate \n"
					+ "    ,-1          AS partition_count \n"
					+ "    ,c.oid \n"
					+ "    ,pg_total_relation_size(c.oid)         / 1024.0 / 1024.0 AS total_size_mb \n"
					+ "    ,(pg_total_relation_size(c.oid) - pg_indexes_size(c.oid) - coalesce(pg_total_relation_size(reltoastrelid), 0)) / 1024.0 / 1024.0 AS table_size_mb \n"
					+ "    ,c.relpages::bigint         AS table_size_pgs \n"
					+ "    ,pg_indexes_size(c.oid)                / 1024.0 / 1024.0 AS index_size_mb \n"
					+ "    ,pg_total_relation_size(reltoastrelid) / 1024.0 / 1024.0 AS toast_size_mb \n"
					+ "FROM pg_class c \n"
					+ "LEFT JOIN pg_namespace n ON n.oid = c.relnamespace \n"
					+ "WHERE c.relkind = 'r' \n"
					+ "  AND n.nspname = '" + storeEntry.getSchemaName() + "' \n"   // the *real* identifier (Lower or Mixed case) is fetched in getDbmsObjectList(...)
					+ "  AND c.relname = '" + storeEntry.getObjectName() + "' \n"   // the *real* identifier (Lower or Mixed case) is fetched in getDbmsObjectList(...)
					+ "";

				String sql_indexInfo = ""
					+ "SELECT \n"
					+ "     current_database()         AS dbname \n"
					+ "    ,n.nspname                  AS schema_name \n"
					+ "    ,c.relname                  AS table_name \n"
					+ "    ,i.relname                  AS index_name \n"
					+ "    ,i.reltuples                AS row_estimate \n"
					+ "    ,i.oid \n"
					+ "    ,i.relpages::bigint         AS size_pages \n"
					+ "    ,i.relpages::bigint / 128.0 AS size_mb \n"
					+ "    ,t.spcname                  AS tablespace \n"
					+ "    ,'unique=' || x.indisunique || ', PK=' || x.indisprimary || ', excl=' || x.indisexclusion || ', clustered=' || x.indisclustered as description \n"
					+ "    ,array_to_string(array(select a.attname from pg_attribute a where a.attrelid = x.indexrelid order by attnum), ', ') AS index_keys \n"
					+ "    ,pg_get_indexdef(i.oid)     AS ddl \n"
					+ "FROM pg_index x \n"
					+ "INNER JOIN pg_class      c ON c.oid = x.indrelid \n"
					+ "INNER JOIN pg_class      i ON i.oid = x.indexrelid \n"
					+ "LEFT  JOIN pg_namespace  n ON n.oid = c.relnamespace \n"
					+ "LEFT  JOIN pg_tablespace t ON t.oid = i.reltablespace \n"
					+ "WHERE c.relkind in('r', 'm') \n"
					+ "  AND i.relkind = 'i' \n"
					+ "  AND n.nspname = '" + storeEntry.getSchemaName() + "' \n"   // the *real* identifier (Lower or Mixed case) is fetched in getDbmsObjectList(...)
					+ "  AND c.relname = '" + storeEntry.getObjectName() + "' \n"   // the *real* identifier (Lower or Mixed case) is fetched in getDbmsObjectList(...)
					+ "";

				try	(AseSqlScript ss = new AseSqlScript(conn, 10)) 
				{
					ss.setRsAsAsciiTable(true);

					String res_tableSize = ss.executeSqlStr(sql_tableSize, true);
					String res_indexInfo = ss.executeSqlStr(sql_indexInfo, true);

					storeEntry.setExtraInfoText(res_tableSize + "\n" + res_indexInfo); 
				}
				catch (SQLException e) 
				{ 
					storeEntry.setExtraInfoText( e.toString() ); 
					_logger.warn("DDL Lookup. Problems Getting TABLE EXTRA INFO information for: TableName='" + storeEntry.getObjectName() + "', dbname='" + dbname + "', objectName='" + objectName + "', source='" + source + "', dependLevel=" + dependLevel + ". Caught: " + e);
				}

				
				//--------------------------------------------
				// GET sp__optdiag
				if (pch.getConfig_doGetStatistics())
				{
				}
	
				//--------------------------------------------
				// GET SOME OTHER STATISTICS
				// Size of tables etc
			}

			//-----------------------------------------------------------------------------------
			//-- FOREIGN Table (or REMOTE/PROXY table)
			//-----------------------------------------------------------------------------------
			else if ( "f".equals(type) ) 
			{
				// This should be stored
				returnList.add(storeEntry);

				String tableSchema = storeEntry.getSchemaName();
				String tableName   = storeEntry.getObjectName();
				
				// Get FOREIGN TABLE Columns
				String sql_getForeignTableColumns = ""
					    + "select \n"
					    + "     ordinal_position \n"
					    + "    ,column_name \n"
					    + "    ,data_type \n"
					    + "    ,character_maximum_length \n"
					    + "    ,numeric_precision \n"
					    + "    ,numeric_scale \n"
					    + "    ,datetime_precision \n"
					    + "    ,column_default \n"
					    + "    ,is_nullable \n"
					    + "from INFORMATION_SCHEMA.COLUMNS \n"
					    + "where 1=1 \n"
					    + "  and table_schema = '" + tableSchema + "' \n"
					    + "  and table_name   = '" + tableName   + "' \n"
					    + "";

				// Get FOREIGN TABLE Info (what table is the remote table pointing to: schema.localForeignTable -> srvname.cat.schema.remoteTable)
				String sql_getForeignTableOptions = ""
					    + "select \n"
					    + "     foreign_table_catalog \n"
					    + "    ,foreign_table_schema \n"
					    + "    ,foreign_table_name \n"
					    + "    ,option_name \n"
					    + "    ,option_value \n"
					    + "from information_schema.foreign_table_options \n"
					    + "where foreign_table_schema = '" + tableSchema + "' \n"
					    + "  and foreign_table_name   = '" + tableName   + "' \n"
					    + "";


				// Get FOREIGN SERVER NAME
				String sql_getForeignServerName = ""
					    + "select \n"
					    + "     foreign_server_name \n"
					    + "    ,foreign_data_wrapper_name \n"
					    + "    ,foreign_server_type \n"
					    + "    ,foreign_server_version \n"
					    + "    ,authorization_identifier \n"
					    + "from information_schema.foreign_servers \n"
					    + "where foreign_server_name = ( \n"
					    + "        select foreign_server_name \n"
					    + "        from information_schema.foreign_tables \n"
					    + "        where foreign_table_schema = '" + tableSchema + "' \n"
					    + "          and foreign_table_name   = '" + tableName   + "' \n"
					    + "	) \n"
					    + "";

				// Get FOREIGN SERVER OPTIONS
				String sql_getForeignServerOptions = ""
					    + "select \n"
					    + "      foreign_server_name \n"
					    + "     ,option_name \n"
					    + "     ,option_value \n"
					    + "from information_schema.foreign_server_options \n"
					    + "where foreign_server_name = ( \n"
					    + "        select foreign_server_name \n"
					    + "        from information_schema.foreign_tables \n"
					    + "        where foreign_table_schema = '" + tableSchema + "' \n"
					    + "          and foreign_table_name   = '" + tableName   + "' \n"
					    + "	) \n"
					    + "";

				try	(AseSqlScript ss = new AseSqlScript(conn, 10)) 
				{
					ss.setRsAsAsciiTable(true);

					String res_foreignTableColumns  = ss.executeSqlStr(sql_getForeignTableColumns , true);
					String res_foreignTableOptions  = ss.executeSqlStr(sql_getForeignTableOptions , true);
					String res_foreignServerName    = ss.executeSqlStr(sql_getForeignServerName   , true);
					String res_foreignServerOptions = ss.executeSqlStr(sql_getForeignServerOptions, true);

					storeEntry.setObjectText(""
						+ res_foreignTableColumns  + "\n" 
						+ res_foreignTableOptions  + "\n" 
						+ res_foreignServerName    + "\n"
						+ res_foreignServerOptions + "\n"
						); 
				}
				catch (SQLException e) 
				{ 
					storeEntry.setObjectText( e.toString() ); 
					_logger.warn("DDL Lookup. Problems Getting FOREIGN TABLE INFO information for: TableName='" + storeEntry.getObjectName() + "', dbname='" + dbname + "', objectName='" + objectName + "', source='" + source + "', dependLevel=" + dependLevel + ". Caught: " + e);
				}
				
			}

			//-----------------------------------------------------------------------------------
			//-- view, materialized view, sequence, TRIGGER, FUNCTION
			//-----------------------------------------------------------------------------------
			else if (
			       "v" .equals(type) // view 
			    || "m" .equals(type) // materialized view 
			    || "S" .equals(type) // sequence 
			    || "TR".equals(type) // TRIGGER   (added by method: getDbmsObjectList) 
			    || "FN".equals(type) // FUNCTION  (added by method: getDbmsObjectList) 
			    || "P" .equals(type) // PROCEDURE (added by method: getDbmsObjectList) 
			   )
			{
				// This should be stored
				returnList.add(storeEntry);

				//--------------------------------------------
				// GET OBJECT TEXT
				//--------------------------------------------
				if ("v".equals(type) || "m".equals(type))
				{
					String sqlText = "";
					String sql = "select pg_get_viewdef(" + storeEntry.getObjectId() + ", true)";

					try (Statement statement = conn.createStatement(); ResultSet rs = statement.executeQuery(sql) )
					{
						while(rs.next())
						{
							sqlText += rs.getString(1);
						}

						storeEntry.setObjectText( sqlText );
					}
					catch (SQLException e)
					{
						storeEntry.setObjectText( e.toString() );
						_logger.warn("DDL Lookup. Problems Getting VIEW information for: ViewName='" + storeEntry.getObjectName() + "', dbname='" + dbname + "', objectName='" + objectName + "', source='" + source + "', dependLevel=" + dependLevel + ". Caught: " + e);
					}

					//--------------------------------------------
					// if VIEW: Possibly parse the SQL statement and extract Tables, send those tables to 'DDL Storage'...
					if (StringUtil.hasValue(sqlText))
					{
						if (Configuration.getCombinedConfiguration().getBooleanProperty(PROPKEY_view_parseAndSendTables, DEFAULT_view_parseAndSendTables))
						{
							Set<String> tableList = SqlParserUtils.getTables(sqlText);

							// Post to DDL Storage, for lookup
							for (String tableName : tableList)
							{
								pch.addDdl(dbname, tableName, this.getClass().getSimpleName() + ".resolve.view");
							}

							// Add table list to the saved entry
							storeEntry.setExtraInfoText( "TableList: " + StringUtil.toCommaStr(tableList) ); 
						}
					}
				}

				//--------------------------------------------
				if ("TR".equals(type))
				{
					String sqlText = "";
					String sql     = ""
						    + "select \n"
						    + "    pg_get_triggerdef(t.oid, true) AS trigger_ddl \n"
						    + "   ,pg_get_functiondef(t.tgfoid)   AS function_ddl \n"
						    + "from pg_trigger t \n"
						    + "where t.oid = " + storeEntry.getObjectId() + " \n"
						    + "";

					try (Statement statement = conn.createStatement(); ResultSet rs = statement.executeQuery(sql) )
					{
						while(rs.next())
						{
							String trigger_ddl  = rs.getString(1);
							String function_ddl = rs.getString(2);

							sqlText += (""
									+ "---------------------- \n"
									+ "-- TRIGGER-DDL: \n"
									+ "---------------------- \n"
									+ trigger_ddl + "\n"
									+ "\n"
									+ "---------------------- \n"
									+ "-- FUNCTION-DDL: \n"
									+ "---------------------- \n"
									+ function_ddl + "\n"
									+ "\n"
									+ "");
						}

						storeEntry.setObjectText( sqlText );
					}
					catch (SQLException e)
					{
						storeEntry.setObjectText( e.toString() );
						_logger.warn("DDL Lookup. Problems Getting TRIGGER information for: triggerName='" + storeEntry.getObjectName() + "', dbname='" + dbname + "', objectName='" + objectName + "', source='" + source + "', dependLevel=" + dependLevel + ". Caught: " + e);
					}

					//--------------------------------------------
					// if TRIGGER: Possibly parse the SQL statement and extract Tables, send those tables to 'DDL Storage'...
				}
				
				//--------------------------------------------
				if ( "FN".equals(type) || "P".equals(type) )
				{
					String sqlText = "";
					String sql     = "select pg_get_functiondef(" + storeEntry.getObjectId() + ")";

//System.out.println("YYYYYYYYYYY: Lookup(type='"+type+"'): sql=|"+sql+"|, storeEntry="+storeEntry);
					try (Statement statement = conn.createStatement(); ResultSet rs = statement.executeQuery(sql) )
					{
						while(rs.next())
						{
							String function_ddl = rs.getString(1);

							sqlText += (""
									+ "---------------------- \n"
									+ ("FN".equals(type) ? "-- FUNCTION-DDL: \n" : "-- PROCEDURE-DDL: \n")
									+ "---------------------- \n"
									+ function_ddl + "\n"
									+ "\n"
									+ "");
						}

//System.out.println("YYYYYYYYYYY: Lookup(type='"+type+"'): DDL="+sqlText);
						storeEntry.setObjectText( sqlText );
					}
					catch (SQLException e)
					{
						storeEntry.setObjectText( e.toString() );
						_logger.warn("DDL Lookup. Problems Getting FUNCTION/PROCEDURE information for: functionName='" + storeEntry.getObjectName() + "', dbname='" + dbname + "', objectName='" + objectName + "', source='" + source + "', dependLevel=" + dependLevel + ". Caught: " + e);
					}

					//--------------------------------------------
					// if FUNCTION: Possibly parse the SQL statement and extract Tables, send those tables to 'DDL Storage'...
					//--------------------------------------------
					if (StringUtil.hasValue(sqlText))
					{
						if (Configuration.getCombinedConfiguration().getBooleanProperty(PROPKEY_function_parseAndSendTables, DEFAULT_function_parseAndSendTables))
						{
							Set<String> tableList = SqlParserUtils.getTables(sqlText);

							// Post to DDL Storage, for lookup
							for (String tableName : tableList)
							{
								if ("FN".equals(type))
									pch.addDdl(dbname, tableName, this.getClass().getSimpleName() + ".resolve.function");
								else
									pch.addDdl(dbname, tableName, this.getClass().getSimpleName() + ".resolve.procedure");
							}

							// Add table list to the saved entry
							storeEntry.setExtraInfoText( "TableList: " + StringUtil.toCommaStr(tableList) ); 
//System.out.println("YYYYYYYYYYY: Lookup(type='"+type+"'): TableList: "+storeEntry.getExtraInfoText());
						}
					}
				}
			}
			//-----------------------------------------------------------------------------------
			//-- CURSORS (possibly used by FDW -- Foreign Data Wrappers)
			//>>>>>>> NOTE: This does NOT seems to work (cursor info from FDW is not visible in pg_cursors <<<<<
			//>>>>>>>       But lets keep it anyway
			//-----------------------------------------------------------------------------------
			else if ( "CU" .equals(type) ) // CURSOR (added by method: getDbmsObjectList) 
			{
				// This should be stored
				returnList.add(storeEntry);

				//--------------------------------------------
				// GET CURSOR INFO/TEXT
				//--------------------------------------------
				if ("CU".equals(type))
				{
					// 1> select * from pg_catalog.pg_cursors
					// RS> Col# Label         JDBC Type Name           Guessed DBMS type Source Table
					// RS> ---- ------------- ------------------------ ----------------- ------------
					// RS> 1    name          java.sql.Types.VARCHAR   text              pg_cursors   <<--- The name of the cursor
					// RS> 2    statement     java.sql.Types.VARCHAR   text              pg_cursors   <<--- The verbatim query string submitted to declare this cursor
					// RS> 3    is_holdable   java.sql.Types.BIT       bool              pg_cursors   <<--- true if the cursor is holdable (that is, it can be accessed after the transaction that declared the cursor has committed); false otherwise
					// RS> 4    is_binary     java.sql.Types.BIT       bool              pg_cursors   <<--- true if the cursor was declared BINARY; false otherwise
					// RS> 5    is_scrollable java.sql.Types.BIT       bool              pg_cursors   <<--- true if the cursor is scrollable (that is, it allows rows to be retrieved in a nonsequential manner); false otherwise
					// RS> 6    creation_time java.sql.Types.TIMESTAMP timestamptz       pg_cursors   <<--- The time at which the cursor was declared
					// +----+-----------------------------------+-----------+---------+-------------+--------------------------+
					// |name|statement                          |is_holdable|is_binary|is_scrollable|creation_time             |
					// +----+-----------------------------------+-----------+---------+-------------+--------------------------+
					// |    |select * from pg_catalog.pg_cursors|false      |false    |false        |2023-04-11 17:37:00.599671|
					// +----+-----------------------------------+-----------+---------+-------------+--------------------------+

					String sqlText = "";
					String sqlTextWithComments = "";
					
					String tmpCursorName = objectName;
					if (tmpCursorName.startsWith("CURSOR: "))
						tmpCursorName = objectName.substring("CURSOR: ".length());

					String sql = ""
						    + "SELECT \n"
						    + "     statement \n"
						    + "    ,is_holdable \n"
						    + "    ,is_binary \n"
						    + "    ,is_scrollable \n"
						    + "    ,creation_time \n"
						    + "FROM pg_cursors c \n"
						    + "WHERE name IN('" + tmpCursorName.toLowerCase() + "', '" + tmpCursorName + "') \n" // Not 100% sure if this will stored "as is" or the "plain name" (pg normally stored it in lower, but I'm not sure for cursors)
						    + "";

					try (Statement statement = conn.createStatement(); ResultSet rs = statement.executeQuery(sql) )
					{
						boolean   is_holdable   = false; 
						boolean   is_binary     = false; 
						boolean   is_scrollable = false; 
						Timestamp creation_time = null; 

						while(rs.next())
						{
							sqlText      += rs.getString   (1);
							is_holdable   = rs.getBoolean  (2);
							is_binary     = rs.getBoolean  (3);
							is_scrollable = rs.getBoolean  (4);
							creation_time = rs.getTimestamp(5);
						}

						if (StringUtil.hasValue(sqlText))
						{
							sqlTextWithComments = sqlText
									+ "\n\n"
									+ "------------------------------------------------------ \n"
									+ "-- Cursor properties from 'pg_cursors' \n"
									+ "------------------------------------------------------ \n"
									+ "-- is_holdable   = " + is_holdable   + "\n"
									+ "-- is_binary     = " + is_binary     + "\n"
									+ "-- is_scrollable = " + is_scrollable + "\n"
									+ "-- creation_time = " + creation_time + "\n"
									+ "";
						}

						storeEntry.setObjectText( sqlTextWithComments );
					}
					catch (SQLException e)
					{
						storeEntry.setObjectText( e.toString() );
						_logger.warn("DDL Lookup. Problems Getting CURSOR information for: CursorName='" + storeEntry.getObjectName() + "', dbname='" + dbname + "', objectName='" + objectName + "', source='" + source + "', dependLevel=" + dependLevel + ". Caught: " + e);
					}

					//--------------------------------------------
					// if CURSOR: Possibly parse the SQL statement and extract Tables, send those tables to 'DDL Storage'...
					if (StringUtil.hasValue(sqlText))
					{
						if (Configuration.getCombinedConfiguration().getBooleanProperty(PROPKEY_cursor_parseAndSendTables, DEFAULT_cursor_parseAndSendTables))
						{
							Set<String> tableList = SqlParserUtils.getTables(sqlText);

							// Post to DDL Storage, for lookup
							for (String tableName : tableList)
							{
								pch.addDdl(dbname, tableName, this.getClass().getSimpleName() + ".resolve.cursor");
							}

							// Add table list to the saved entry
							storeEntry.setExtraInfoText( "TableList: " + StringUtil.toCommaStr(tableList) ); 
						}
					}
				}
			}
			else
			{
				// Unknown type
				_logger.warn("doObjectInfoLookup_doWork(), unhandled OBJECT TYPE '" + type + "'. This entry will simply be skipped. storeEntry="+storeEntry);
				continue;
			}

			//--------------------------------------------
			// GET sp_depends
			if (pch.getConfig_doSpDepends())
			{
				//storeEntry.setDependsText( ... ); 
				
			} // end: doSpDepends

		} // end: for (DdlDetails entry : objectList)

//for (DdlDetails ddlDetails : returnList)
//	System.out.println("\n\n######################################################################################\n" + ddlDetails.toStringDebug());

		// Return the list of objects to be STORED in DDL Storage
		return returnList;
	}

	private List<DdlDetails> getDbmsObjectList(DbxConnection conn, String dbname, String objectName, String source, String dependParent, int dependLevel)
	{
		String originObjectName = objectName;

		// Keep a list of objects we need to work with
		// because: if we get more than one proc/table with different owners
		ArrayList<DdlDetails> objectList = new ArrayList<DdlDetails>();
		
		// Remove any "dbname" or "schemaName" from the objectName if it has any
		// The lookup below will add entries for ALL tables with the name (if there are several tables with same name, the lookup will add ALL tables)
		SqlObjectName sqlObjectName = new SqlObjectName(conn, objectName);
		objectName = sqlObjectName.getObjectName();
		
		DbmsVersionInfo dbmsVerInfo = conn.getDbmsVersionInfo();
		
		//----------------------------------------------------------------
		// FDW Cursor -- Foreign Data Wrapper  
		//>>>>>>> NOTE: This does NOT seems to work (cursor info from FDW is not visible in pg_cursors <<<<<
		//>>>>>>>       But lets keep it anyway
		//----------------------------------------------------------------
		if (objectName != null && objectName.startsWith("CURSOR: "))
		{
			// 1> select * from pg_catalog.pg_cursors
			// RS> Col# Label         JDBC Type Name           Guessed DBMS type Source Table
			// RS> ---- ------------- ------------------------ ----------------- ------------
			// RS> 1    name          java.sql.Types.VARCHAR   text              pg_cursors   <<--- The name of the cursor
			// RS> 2    statement     java.sql.Types.VARCHAR   text              pg_cursors   <<--- The verbatim query string submitted to declare this cursor
			// RS> 3    is_holdable   java.sql.Types.BIT       bool              pg_cursors   <<--- true if the cursor is holdable (that is, it can be accessed after the transaction that declared the cursor has committed); false otherwise
			// RS> 4    is_binary     java.sql.Types.BIT       bool              pg_cursors   <<--- true if the cursor was declared BINARY; false otherwise
			// RS> 5    is_scrollable java.sql.Types.BIT       bool              pg_cursors   <<--- true if the cursor is scrollable (that is, it allows rows to be retrieved in a nonsequential manner); false otherwise
			// RS> 6    creation_time java.sql.Types.TIMESTAMP timestamptz       pg_cursors   <<--- The time at which the cursor was declared
			// +----+-----------------------------------+-----------+---------+-------------+--------------------------+
			// |name|statement                          |is_holdable|is_binary|is_scrollable|creation_time             |
			// +----+-----------------------------------+-----------+---------+-------------+--------------------------+
			// |    |select * from pg_catalog.pg_cursors|false      |false    |false        |2023-04-11 17:37:00.599671|
			// +----+-----------------------------------+-----------+---------+-------------+--------------------------+
			
			String tmpCursorName = objectName.substring("CURSOR: ".length());
			String sql = ""
				    + "SELECT \n"
				    + "     current_database() AS dbname \n"
				    + "    ,name \n"
				    + "    ,creation_time \n"
				    + "FROM pg_cursors c \n"
				    + "WHERE name IN('" + tmpCursorName.toLowerCase() + "', '" + tmpCursorName + "') \n" // Not 100% sure if this will stored "as is" or the "plain name" (pg normally stored it in lower, but I'm not sure for cursors)
				    + "";

			try ( Statement statement = conn.createStatement(); ResultSet rs = statement.executeQuery(sql) )
			{
				while(rs.next())
				{
					DdlDetails addEntry = new DdlDetails();
					
					String    currentDbname = rs.getString   (1);
					String    cursorName    = rs.getString   (2);
					Timestamp cursorCrDate  = rs.getTimestamp(3);

					addEntry.setType( "CU" );
					
					addEntry.setDbname          ( currentDbname );
					addEntry.setSearchDbname    ( dbname );
					addEntry.setObjectName      ( cursorName );
					addEntry.setSearchObjectName( originObjectName ); // NOT Stored in DDL Store, used for: isDdlDetailsStored(), markDdlDetailsAsStored()
					addEntry.setSource          ( source );
					addEntry.setDependParent    ( dependParent );
					addEntry.setDependLevel     ( dependLevel );
					addEntry.setSampleTime      ( new Timestamp(System.currentTimeMillis()) );
					addEntry.setCrdate          ( cursorCrDate );

				//	addEntry.setOwner           ( lookupEntry.getOwner() );
				//	addEntry.setObjectId        ( ??? );

					objectList.add(addEntry);
				}

				// We should return here... since 
				return objectList;
/*<<---*/
			}
			catch (SQLException e)
			{
				_logger.error("Problems Getting basic information about DDL for dbname='" + dbname + "', objectName='" + objectName + "', source='" + source + "', dependLevel=" + dependLevel + ". Skipping DDL Storage of this object. Caught: " + e);
				return Collections.emptyList();
			}
		}

		//----------------------------------------------------------------
		// RELATION -- get TYPE, OWNER CREATION_TIME and DBNAME where the record(s) was found 
		//----------------------------------------------------------------
		String sql = ""
			    + "SELECT \n"
			    + "     current_database() AS dbname \n"
			    + "    ,n.nspname          AS schema_name \n"
			    + "    ,c.relname          AS object_name \n"
			    + "    ,c.oid              AS object_id \n"
			    + "    ,c.relkind          AS object_type \n"
			    + "FROM pg_class c \n"
			    + "LEFT JOIN pg_namespace n ON n.oid = c.relnamespace \n"
//			    + "WHERE relname = '" + objectName + "' \n"
			    + "WHERE relname IN('" + objectName.toLowerCase() + "', '" + objectName + "') \n" // as LowerCase or OriginText (Postgres stores non-quoted-identifiers as LowerCase and "quoted-identifiers" as whatever was given)
			    + "";
			
		try ( Statement statement = conn.createStatement(); ResultSet rs = statement.executeQuery(sql) )
		{
			while(rs.next())
			{
				DdlDetails entry = new DdlDetails();

				entry.setSearchDbname    ( dbname ); // NOT Stored in DDL Store, used for: isDdlDetailsStored(), markDdlDetailsAsStored()
				entry.setObjectName      ( objectName );
				entry.setSearchObjectName( originObjectName ); // NOT Stored in DDL Store, used for: isDdlDetailsStored(), markDdlDetailsAsStored()
				entry.setSource          ( source );
				entry.setDependParent    ( dependParent );
				entry.setDependLevel     ( dependLevel );
				entry.setSampleTime      ( new Timestamp(System.currentTimeMillis()) );

				entry.setDbname          ( rs.getString   (1) );
				entry.setSchemaName      ( rs.getString   (2) );
				entry.setObjectName      ( rs.getString   (3) ); // Use the object name stored in the DBMS (for Postgres it can be LowerCase for non-quoted-identifiers and the actual name for "quoted-identifiers")
				entry.setObjectId        ( rs.getInt      (4) );
				entry.setType            ( rs.getString   (5) );

				objectList.add(entry);
			}
		}
		catch (SQLException e)
		{
			_logger.error("Problems Getting basic information about DDL for dbname='" + dbname + "', objectName='" + objectName + "', source='" + source + "', dependLevel=" + dependLevel + ". Skipping DDL Storage of this object. Caught: " + e);
			return Collections.emptyList();
		}


		//----------------------------------------------------------------
		// No TABLE/RELATION was found... try to get info from FUNCTIONS/PROCEDURES
		//----------------------------------------------------------------
		if (objectList.isEmpty())
		{
//TODO; // test the proc stuff again... and remove below comments
//TODO; // test FAILED... debug why there is NO PROCEDURE in the report...
//TODO; // Should we check/handle PROCEDURE... the type 'FN' works even if it's a procedure, but the HTML output says 'FUNCTION' even if it's a PROCEDURE
//TODO; // possibly use prokind --- 'f' for a normal function, 'p' for a procedure, 'a' for an aggregate function, or 'w' for a window function
//	    + "    ,CASE WHEN c.prokind = 'f' THEN 'FN' \n"  // in SQL Server: FN = Scalar function, TF = Table function
//	    + "          WHEN c.prokind = 'p' THEN 'P' \n"   // in SQL Server: P  = Stored procedure
//		+ "          WHEN c.prokind = 'a' THEN 'AF' \n"  // in SQL Server: AF = Aggregate function (CLR)
//		+ "          WHEN c.prokind = 'w' THEN 'WF' \n"  // in SQL Server: >>> Window Function do not seems to exists <<<
//	    + "          ELSE                      'FN' \n"
//	    + "     END AS object_type \n"

			// Column "prokind" was added in Postgres 11, use that if possible otherwise fallback to 'FN'
			String objectType = "    ,'FN'               AS object_type \n";
			if (dbmsVerInfo.getLongVersion() >= Ver.ver(11))
			{
				objectType = ""
					    + "    ,CASE WHEN c.prokind = 'f' THEN 'FN' \n"  // in SQL Server: FN = Scalar function, TF = Table function
					    + "          WHEN c.prokind = 'p' THEN 'P' \n"   // in SQL Server: P  = Stored procedure
						+ "          WHEN c.prokind = 'a' THEN 'AF' \n"  // in SQL Server: AF = Aggregate function (CLR)
						+ "          WHEN c.prokind = 'w' THEN 'WF' \n"  // in SQL Server: >>> Window Function do not seems to exists <<<
					    + "          ELSE                      'FN' \n"
					    + "     END AS object_type \n"
					    + "";
			}
			
			sql = ""
				    + "SELECT \n"
				    + "     current_database() AS dbname \n"
				    + "    ,n.nspname          AS schema_name \n"
				    + "    ,c.proname          AS object_name \n"
				    + "    ,c.oid              AS object_id \n"
				    + objectType
				    + "FROM pg_proc c \n"
				    + "LEFT JOIN pg_namespace n ON n.oid = c.pronamespace \n"
				    + "WHERE proname IN('" + objectName.toLowerCase() + "', '" + objectName + "') \n" // as LowerCase or OriginText (Postgres stores non-quoted-identifiers as LowerCase and "quoted-identifiers" as whatever was given)
				    + "";
			
			try ( Statement statement = conn.createStatement(); ResultSet rs = statement.executeQuery(sql) )
			{
				while(rs.next())
				{
					DdlDetails entry = new DdlDetails();

					entry.setSearchDbname    ( dbname );
					entry.setObjectName      ( objectName );
					entry.setSearchObjectName( originObjectName ); // NOT Stored in DDL Store, used for: isDdlDetailsStored(), markDdlDetailsAsStored()
					entry.setSource          ( source );
					entry.setDependParent    ( dependParent );
					entry.setDependLevel     ( dependLevel );
					entry.setSampleTime      ( new Timestamp(System.currentTimeMillis()) );

					entry.setDbname          ( rs.getString   (1) );
					entry.setSchemaName      ( rs.getString   (2) );
					entry.setObjectName      ( rs.getString   (3) ); // Use the object name stored in the DBMS (for Postgres it can be LowerCase for non-quoted-identifiers and the actual name for "quoted-identifiers")
					entry.setObjectId        ( rs.getInt      (4) );
					entry.setType            ( rs.getString   (5) );

					objectList.add(entry);
//System.out.println("XXXXXXXXXX: FUNCTION/PROCEDURE: entry="+entry.toStringDebug(120));
				}
			}
			catch (SQLException e)
			{
				_logger.error("Problems Getting FUNCTION/PROCEDURE information about DDL for dbname='" + dbname + "', objectName='" + objectName + "', source='" + source + "', dependLevel=" + dependLevel + ". Skipping DDL Storage of this object. Caught: " + e);
				return Collections.emptyList();
			}
		} // end: No TABLE/RELATION was found


		//-------------------------------------------------------------
		// Do some extra lookups... to get triggers for USER Tables
		// Store TRIGGER name in UserTable dependsList
		// and ADD the trigger name to the List of things that needs to be extracted
		ArrayList<DdlDetails> triggerList = null;
		for (DdlDetails lookupEntry : objectList)
		{
			// r=ordinary table, p=partitioned table
			if ("r".equals(lookupEntry.getType()) || "p".equals(lookupEntry.getType()))
			{
				// Get TRIGGERS
				sql = ""
					    + "SELECT \n"
					    + "     t.tgname AS trigger_name \n"
					    + "    ,t.oid    AS trigger_object_id \n"
					    + "FROM pg_trigger t \n"
					    + "WHERE t.tgrelid = " + lookupEntry.getObjectId() + " \n"
					    + "";
				
				try ( Statement statement = conn.createStatement(); ResultSet rs = statement.executeQuery(sql) )
				{
					while(rs.next())
					{
						DdlDetails addEntry = new DdlDetails();

						String    triggerName   =  rs.getString   (1);
						int       triggerObjId  =  rs.getInt      (2);
						Timestamp triggerCrDate =  null;
						
						addEntry.setSearchDbname    ( dbname );
						addEntry.setObjectName      ( triggerName );
						addEntry.setSearchObjectName( triggerName ); // NOT Stored in DDL Store, used for: isDdlDetailsStored(), markDdlDetailsAsStored()
						addEntry.setSource          ( source );
						addEntry.setDependParent    ( dependParent );
						addEntry.setDependLevel     ( dependLevel );
						addEntry.setSampleTime      ( new Timestamp(System.currentTimeMillis()) );

						addEntry.setType            ( "TR" );
						addEntry.setOwner           ( lookupEntry.getOwner() );
						addEntry.setObjectName      ( triggerName ); // Use the object name stored in the DBMS (for Sybase ASE it will always be stored as it was originally created)
						addEntry.setObjectId        ( triggerObjId );
						addEntry.setCrdate          ( triggerCrDate );
						addEntry.setDbname          ( dbname );

						// Add triggerName to >>>> TABLE's <<<< depends list
						lookupEntry.addDependList("TR:" + triggerName);

						if (triggerList == null)
							triggerList = new ArrayList<DdlDetails>();
						triggerList.add(addEntry);
					}
				}
				catch (SQLException e)
				{
					_logger.error("Problems Getting Table TRIGGER information about DDL for dbname='" + dbname + "', objectName='" + lookupEntry.getObjectName() + "'. Skipping TRIGGER information. Caught: " + e);
				}
				
				// Get VIEWS depends on this table...
				// Should we do this or NOT... (lets skip this now)
			}
		}
		if (triggerList != null)
			objectList.addAll(triggerList);

		return objectList;
	}
}
