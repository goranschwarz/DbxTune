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
import com.asetune.utils.AseSqlScript;
import com.asetune.utils.Configuration;
import com.asetune.utils.StringUtil;

public class ObjectLookupInspectorPostgres
extends ObjectLookupInspectorAbstract
{
	private static Logger _logger = Logger.getLogger(ObjectLookupInspectorPostgres.class);

	public static final String  PROPKEY_xmlPlan_parseAndSendTables = Version.getAppName() + "." + ObjectLookupInspectorPostgres.class.getSimpleName() + ".xmlPlan.parse.send.tables";
	public static final boolean DEFAULT_xmlPlan_parseAndSendTables = true;

	public static final String  PROPKEY_view_parseAndSendTables    = Version.getAppName() + "." + ObjectLookupInspectorPostgres.class.getSimpleName() + ".view.parse.send.tables";
	public static final boolean DEFAULT_view_parseAndSendTables    = true;

//	private long    _dbmsVersion = 0;

	@Override
	public boolean allowInspection(ObjectLookupQueueEntry entry)
	{
		if (entry == null)
			return false;

		// All tables starting with "pg_" must be a Postgres System table... 
		// so do not bother to look this up.
		if (entry._objectName.startsWith("pg_"))
			return false;

		
//		String dbname     = entry._dbname;
//		String objectName = entry._objectName;
		
		// Discard a bunch of entries
//		if (objectName.indexOf("temp worktable") >= 0 ) return false;
//		if (objectName.startsWith("#"))                 return false;
//		if (objectName.startsWith("ObjId:"))            return false;
//		if (objectName.startsWith("Obj="))              return false;

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

System.out.println("doObjectInfoLookup_doWork: dbname='"+dbname+"', objectName='"+objectName+"', source='"+source+"'.");
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
			else if (
			       "v" .equals(type) // view 
			    || "m" .equals(type) // materialized view 
			    || "S" .equals(type) // sequence 
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
				
				// If it's a view, parse the view definition, and extract tables... which is post again to the DDL Storage
			}
			else
			{
				// Unknown type
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
		
		// get TYPE, OWNER CREATION_TIME and DBNAME where the record(s) was found 
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
			}

			return objectList;
		}
		catch (SQLException e)
		{
			_logger.error("Problems Getting basic information about DDL for dbname='" + dbname + "', objectName='" + objectName + "', source='" + source + "', dependLevel=" + dependLevel + ". Skipping DDL Storage of this object. Caught: " + e);
			return Collections.emptyList();
			//return null;
		}
	}
}
