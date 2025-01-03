/*******************************************************************************
 * Copyright (C) 2010-2025 Goran Schwarz
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
package com.dbxtune.sql.ddl;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.slf4j.bridge.SLF4JBridgeHandler;

import com.dbxtune.sql.conn.DbxConnection;

import schemacrawler.schema.Catalog;
import schemacrawler.schema.Schema;
import schemacrawler.schema.Table;
import schemacrawler.schema.View;
import schemacrawler.schemacrawler.SchemaCrawlerException;
import schemacrawler.schemacrawler.SchemaCrawlerOptions;
import schemacrawler.schemacrawler.SchemaCrawlerOptionsBuilder;
import schemacrawler.schemacrawler.SchemaInfoLevelBuilder;
import schemacrawler.utility.SchemaCrawlerUtility;

public class SchemaCrawlerUtils
{
	static boolean _isInitialized = false;
	/**
	 * This will bind JUL (java.util.logging) to use SLF4J (Simple Logging Facade for Java), 
	 * then SLF4J uses any logger instance in class path to use a "real" implementation.
	 * <p>
	 * For example using 'slf4j-log4j12-1.7.29.jar' in the class path binds SLF4J to LOG4J
	 * 
	 * @param logLevel the default JUL log level that we should <i>pass on</i> to SLF4J, if null is passed, java.util.logging.Level.INFO will be used.
	 */
	public static final void initLog(java.util.logging.Level logLevel)
	{
		if (_isInitialized)
			return;

		if (logLevel == null)
			logLevel = java.util.logging.Level.INFO;

//		System.setProperty("java.util.logging.manager", "org.apache.logging.log4j.jul.LogManager");

		// JUL (java.util.logging) --->>> SLF4J (Simple Logging Facade 4 Java)... using jar: jul-to-slf4j-1.7.29
		SLF4JBridgeHandler.removeHandlersForRootLogger();
		SLF4JBridgeHandler.install();
		
		String logger = ""; // empty is "Root logger"
//		logger = "logger"; // I think "logger" is used by all SchemaCrawler classes

	}

	public static Catalog getAllCatalogObjects(DbxConnection conn) 
	throws SchemaCrawlerException
	{
		return getCatalogObjects(conn, null);
	}
	
	/**
	 * Get database information
	 * 
	 * @param conn        A database connection
	 * @param options     SchemaCrawler options (if null, <code>SchemaInfoLevelBuilder.standard()</code> is applied)
	 * @return
	 * @throws SchemaCrawlerException
	 */
	public static Catalog getCatalogObjects(DbxConnection conn, SchemaCrawlerOptions options) 
	throws SchemaCrawlerException
	{
		// initialize the log subsystem (if not already done)
		initLog(null);

		if (options == null)
		{
			// Create the options
			SchemaCrawlerOptionsBuilder optionsBuilder = SchemaCrawlerOptionsBuilder.builder();

			// Set what details are required in the schema - this affects the time taken to crawl the schema
			optionsBuilder.withSchemaInfoLevel(SchemaInfoLevelBuilder.standard());
//			optionsBuilder.includeSchemas(new RegularExpressionInclusionRule("PUBLIC.BOOKS"));
//			optionsBuilder.includeTables(tableFullName -> !tableFullName.contains("XXXX"));

			options = optionsBuilder.toOptions();
		}

		// Get the schema definition
//		ResultsColumns rsColumns = SchemaCrawlerUtility.getResultsColumns(null);
		final Catalog catalog = SchemaCrawlerUtility.getCatalog(conn, options);
//		Collection<Schema> schemas = catalog.getSchemas();

		return catalog;
	}
	
	public enum DdlType
	{
		SCHEMA, 
		TABLE, 
		INDEX
	};
	/**
	 * Create DDL TEXT for "another" DBMS Vendor (can hopefully be used to migrate database objects)  
	 * 
	 * @param schemaCrawlerCatalog        SchemaCrawler Object - (if NULL, getCatalogObjects(conn,null) will be called)
	 * @param ddlVendorTypeConn           The DBMS Vendor we want to generate DDL for - (can NOT be null)
	 * 
	 * @return a map (LinkedHashMap) with DdlType.SCHEMA, DdlType.TABLE or DdlType.INDEX as keys (in that order), where each key contains a List of DDL Statements 
	 * @throws SchemaCrawlerException
	 */
	public static Map<DdlType, List<String>> getDdlFor(Catalog schemaCrawlerCatalog, DbxConnection ddlVendorTypeConn) 
	throws SchemaCrawlerException
	{
		// initialize the log subsystem (if not already done)
		initLog(null);

		Map<DdlType, List<String>> map = new LinkedHashMap<>();

		List<String> ddlSchemas = new ArrayList<>();
		List<String> ddlTables  = new ArrayList<>();
		List<String> ddlIndexes = new ArrayList<>();

		map.put(DdlType.SCHEMA, ddlSchemas);
		map.put(DdlType.TABLE,  ddlTables);
		map.put(DdlType.INDEX,  ddlIndexes);

//		IDbmsDdlResolver resolver = ddlVendorTypeConn.getDbmsDdlResolver();
		
		Catalog catalog = schemaCrawlerCatalog;
		if (catalog == null)
			catalog = getCatalogObjects(ddlVendorTypeConn, null);

		Set<Schema> usedSchemas = new LinkedHashSet<>();
		for (final Schema schema : catalog.getSchemas())
		{
//			System.out.println("---------------------- SCHEMA: " + schema);
			for (final Table table : catalog.getTables(schema))
			{
				if (table instanceof View)
					continue;

				usedSchemas.add(schema);
			}
		}

//		for (final Schema schema : usedSchemas)
//		{
////			System.out.println("---------------------- USED - SCHEMA: " + schema);
//			String schemaDdl = resolver.ddlText(schema);
//			ddlSchemas.add(schemaDdl);
//			// System.out.println(schemaDdl);
//		}
//		
//		for (final Schema schema : catalog.getSchemas())
//		{
////			System.out.println("---------------------- SCHEMA: " + schema);
//			for (final Table table : catalog.getTables(schema))
//			{
//				if (table instanceof View)
//					continue;
//
//				String tableDdl = resolver.ddlText(table);
//				ddlTables.add(tableDdl);
//				// System.out.println(tableDdl);
//
//				for (final Index index : table.getIndexes())
//				{
//					String indexDdl = resolver.ddlText(index);
//					ddlIndexes.add(indexDdl);
//					// System.out.println(indexDdl);
//				}
//			}
//		}
		
		return map;
	}
}
