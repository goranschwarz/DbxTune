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
package com.asetune.sql.ddl;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.asetune.sql.conn.DbxConnection;
import com.asetune.sql.ddl.model.Catalog;
import com.asetune.sql.ddl.model.Index;
import com.asetune.sql.ddl.model.Schema;
import com.asetune.sql.ddl.model.Table;
import com.asetune.utils.DbUtils;
import com.asetune.utils.StringUtil;

public class DbmsDdlUtils
{
	public enum DdlType
	{
		SCHEMA, 
		TABLE, 
		INDEX
	};

	/**
	 * Create DDL TEXT for "another" DBMS Vendor (can hopefully be used to migrate database objects)  
	 * 
	 * @param dbxCatalog                  DbxTune Object - (if NULL, getCatalogObjects(conn,null) will be called)
	 * @param ddlVendorTypeConn           The DBMS Vendor we want to generate DDL for - (can NOT be null)
	 * 
	 * @return a map (LinkedHashMap) with DdlType.SCHEMA, DdlType.TABLE or DdlType.INDEX as keys (in that order), where each key contains a List of DDL Statements 
	 * @throws SQLException
	 */
	public static Map<DdlType, List<String>> getDdlFor(Catalog dbxCatalog, DbxConnection ddlVendorTypeConn) 
//	throws SQLException
	{
		Map<DdlType, List<String>> map = new LinkedHashMap<>();

		List<String> ddlSchemas = new ArrayList<>();
		List<String> ddlTables  = new ArrayList<>();
		List<String> ddlIndexes = new ArrayList<>();

		map.put(DdlType.SCHEMA, ddlSchemas);
		map.put(DdlType.TABLE,  ddlTables);
		map.put(DdlType.INDEX,  ddlIndexes);

		Catalog catalog = dbxCatalog;
//		if (catalog == null)
//			catalog = getCatalogObjects(ddlVendorTypeConn, null);

		Set<Schema> usedSchemas = new LinkedHashSet<>();
		for (final Schema schema : catalog.getSchemas())
		{
//			System.out.println("---------------------- AVAILABLE SCHEMA: " + schema.getSchemaName());
			for (final Table table : schema.getTables())
			{
				usedSchemas.add(schema);
			}
		}

		// Get the RESOLVER for the TARGET DBMS, which would be responsible for mapping data types etc...
		IDbmsDdlResolver resolver = ddlVendorTypeConn.getDbmsDdlResolver();

		// Add USED SCHEMAS
		for (final Schema schema : usedSchemas)
		{
//			System.out.println("---------------------- USED - SCHEMA: " + schema.getSchemaName());
			String schemaDdl = resolver.ddlText(schema);
			ddlSchemas.add(schemaDdl);
//			System.out.println(">>> SCHEMA-DDL >>> \n" + schemaDdl);
		}
		
		// Add TABLE/INDEX
		for (final Schema schema : catalog.getSchemas())
		{
//			System.out.println("---------------------- SCHEMA: " + schema.getSchemaName());
			for (final Table table : schema.getTables())
			{
				String tableDdl = resolver.ddlText(table);
				ddlTables.add(tableDdl);
//				System.out.println(">>> TABLE-DDL >>> \n" + tableDdl);

				for (final Index index : table.getIndexes())
				{
					boolean pkAsConstraint = false;
					
					String indexDdl = resolver.ddlText(index, pkAsConstraint);
					ddlIndexes.add(indexDdl);
//					System.out.println(">>> IKNDEX-DDL >>> \n" + indexDdl);
				}
			}
		}
		
		return map;
	}
	
	/**
	 * Generate a DDL statement that works on different DBMS Platforms
	 * <ul>
	 *   <li>H2, Postgres, SqlServer:  ALTER TABLE tabname ALTER COLUMN colname DATATYPE</li>
	 *   <li>ASE, MySql, Oracle:       ALTER TABLE tabname MODIFY       colname DATATYPE</li>
	 * </ul>
	 * 
	 * Example:<br>
	 *   <code>DbmsDdlUtils.getDdlForAlterTableColumn(conn, true, null, "t1", "c1", "varchar(80) null");</code>
	 * <pre>
	 * H2        --> ALTER TABLE "t1" ALTER COLUMN "c1" varchar(80) null</code>
	 * Postgres  --> ALTER TABLE "t1" ALTER COLUMN "c1" varchar(80) null</code>
	 * SqlServer --> ALTER TABLE [t1] ALTER COLUMN [c1] varchar(80) null</code>
	 * ASE       --> ALTER TABLE [t1] MODIFY       [c1] varchar(80) null</code>
	 * Oracle    --> ALTER TABLE "t1" MODIFY       "c1" varchar(80) null</code>
	 * MySql     --> ALTER TABLE `t1` MODIFY       `c1` varchar(80) null</code>
	 * </pre>
	 * 
	 * @param conn                    A connection used to get current database vendor
	 * @param addQuotedIdentifier     If we should add DBMS Vendor specific quotes around the schema/table/column names
	 * @param schema                  Schema Name (can be null)
	 * @param table                   Name of the table
	 * @param column                  Name of the column to change      
	 * @param text                    The "thing" we want to change with the column
	 * 
	 * @return
	 * @throws SQLException if we fail to discover DBMS Vendor
	 */
	public static String getDdlForAlterTableColumn(DbxConnection conn, boolean addQuotedIdentifier, String schema, String table, String column, String text)
	throws SQLException
	{
		// H2, Postgres, SqlServer:  ALTER TABLE tabname ALTER COLUMN colname DATATYPE
		// ASE, MySql, Oracle:       ALTER TABLE tabname MODIFY       colname DATATYPE
		String dbmsSpecificSyntax_beforeColName = "ALTER COLUMN";
		if (DbUtils.isProductName(conn.getDatabaseProductName(), DbUtils.DB_PROD_NAME_SYBASE_ASE, DbUtils.DB_PROD_NAME_MYSQL, DbUtils.DB_PROD_NAME_ORACLE))
			dbmsSpecificSyntax_beforeColName = "MODIFY";

		String lq = "";
		String rq = "";
		if (addQuotedIdentifier)
		{
			lq = conn.getLeftQuote();
			rq = conn.getRightQuote();
		}
		
		String schemaName = StringUtil.isNullOrBlank(schema) ? "" : lq + schema + rq + ".";
		String tableName  = lq + table  + rq;
		String colName    = lq + column + rq;
		
		String sql = "ALTER TABLE " + schemaName + tableName + " " + dbmsSpecificSyntax_beforeColName + " " + colName + " " + text;
		return sql;
	}

}
