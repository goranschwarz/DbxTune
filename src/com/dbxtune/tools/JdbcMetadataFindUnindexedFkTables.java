/*******************************************************************************
 * Copyright (C) 2010-2019 Goran Schwarz
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
package com.dbxtune.tools;

import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.dbxtune.sql.SqlObjectName;
import com.dbxtune.sql.conn.DbxConnection;
import com.dbxtune.utils.StringUtil;

/**
 * <b>NOTE</b>: This is NOT working to me expectations! <br>
 * I wanted to find Foreign Keys that had missing indexes (for CASCADE deletes etc) <br>
 * <br>
 * But this does NOT do the job... I will have to revisit it at a later stage 
 */
public class JdbcMetadataFindUnindexedFkTables
{
	public static void main(String[] args) 
	throws Exception 
	{
		String url    = "jdbc:sybase:Tds:dba-1-ase:5000/PML";
		String user   = "sa";
		String passwd = "sjhyr564s_Wq26kl73";

		try (Connection conn = DriverManager.getConnection(url, user, passwd)) 
		{
			checkAllTables(conn, null);
		}
	}

	/**
	 * If you want to change how the DDL Statement for CREATE INDEX is built, you can change it a bit.
	 */
	public interface IndexDllCreator
	{
		/** Should we add anything to the START of the index name */
		default String getIndexNamePrefix()
		{
			return "fk_ix";
		}

		/** Should we add anything to the END of the index name */
		default String getIndexNamePostfix()
		{
			return "";
		}

		/** Use this as a "separator" string when building the name */
		default String getIndexNameSeparator()
		{
			return "__";
		}

		/** Use column names or the FK-NAME in the index. */
		default boolean useColumnNames()
		{
			return true;
		}

		/** Should we include the table name in the index */
		default boolean skipTableNameInIndexName(String dbmsProduct)
		{
//			return DbUtils.isProductName(dbmsProduct, DbUtils.DB_PROD_NAME_SYBASE_ASE, DbUtils.DB_PROD_NAME_MSSQL);
			return false;
		}

		/** Create the DDL String */
		default String createIndex(SqlObjectName thisTable, String fkName, List<String> fkCols, SqlObjectName destTable)
		{
//			String indexName = thisTable.getObjectName() + "__" + fkName + "__idx";

			String indexPrefix  = getIndexNamePrefix();
			String indexPostfix = getIndexNamePostfix();
			String sep          = getIndexNameSeparator();
			
			boolean skipTableNameInIndexName = skipTableNameInIndexName(thisTable._dbProductName);
			String tabNameInIndex = skipTableNameInIndexName ? "" : thisTable.getObjectName() + sep;
			String fkNameOrColumnNames = fkName;
			if (useColumnNames())
				fkNameOrColumnNames = String.join(sep, fkCols).replace(' ', '_');

			// construct the name of the index
			String indexName = ""
					+ indexPrefix 
					+ sep 
					+ tabNameInIndex 
					+ destTable.getObjectName()
					+ sep 
					+ fkNameOrColumnNames
					+ ("".equals(indexPostfix) ? "" : sep + indexPostfix)
					;

			// construct the name of the index
			// NOTE: as Quoted Identifiers I use "Faked Quoted Identifiers", which later is translated into the DBMS Specific "Quoted Identifier"
			String ddl = String.format(
				"CREATE INDEX [%s] ON [%s].[%s] (%s)",
				indexName,
				thisTable.getSchemaName(),
				thisTable.getObjectName(),
				StringUtil.toCommaStrQuoted("[", "]", fkCols)
			);

			return ddl;
		}
	}

	public static class MissingIndexInfo
	{
		public String _catalogName;
		public String _schemaName;
		public String _tableName;

		public List<MissingIndexInfoDetails> _details = Collections.emptyList();
	}
	public static class MissingIndexInfoDetails
	{
		public String _catalogName;
		public String _schemaName;
		public String _tableName;

		public String _fkName;
		
		public String _fkCatalogName;
		public String _fkShemaName;
		public String _fkTableName;

		public List<String> _indexColumns;

//		public String _suggestedIndexName;
		public String _suggestedDdl;
	}

	public static List<MissingIndexInfo> checkAllTables(Connection conn, IndexDllCreator factory)
	throws SQLException 
	{
		DatabaseMetaData meta = conn.getMetaData();

		List<MissingIndexInfo> returnList = new ArrayList<>();

		// Get all tables
		try (ResultSet tables = meta.getTables(null, null, "%", new String[]{"TABLE"})) 
		{
			while (tables.next()) 
			{
				String catalog = tables.getString("TABLE_CAT");
				String schema  = tables.getString("TABLE_SCHEM");
				String table   = tables.getString("TABLE_NAME");

				List<MissingIndexInfoDetails> details = checkTableForeignKeys(conn, catalog, schema, table, factory);
				if ( ! details.isEmpty() )
				{
					MissingIndexInfo mi = new MissingIndexInfo();
					mi._catalogName = catalog;
					mi._schemaName  = schema;
					mi._tableName   = table;
					mi._details     = details;

					returnList.add(mi);
				}
			}
		}

		System.out.println("Number of tables with missing indexes: " + returnList.size());
		for (MissingIndexInfo entry : returnList)
		{
			System.out.println();
			System.out.println("---- Table: " + entry._tableName);
			for (MissingIndexInfoDetails details : entry._details)
			{
				System.out.println("       DDL: " + details._suggestedDdl);
			}
		}
		
		return returnList;
	}

	public static List<MissingIndexInfoDetails> checkTableForeignKeys(Connection conn, String catalog, String schema, String table) 
	throws SQLException 
	{
		return checkTableForeignKeys(conn, catalog, schema, table, null);
	}

	public static List<MissingIndexInfoDetails> checkTableForeignKeys(Connection conn, String catalog, String schema, String table, IndexDllCreator factory) 
	throws SQLException 
	{
		if (factory == null)
		{
			factory = new IndexDllCreator()	{};
		}

		DatabaseMetaData meta = conn.getMetaData();

		// Map FK name -> list of columns in order
		Map<String, List<String>> fkColumns = new HashMap<>();

		// Map FK name -> Destination Schema/Table
		Map<String, SqlObjectName> fkDestObj = new HashMap<>();

		try (ResultSet fks = meta.getImportedKeys(catalog, schema, table)) 
		{
			while (fks.next()) 
			{
				String fkName  = fks.getString("FK_NAME");
				short  seq     = fks.getShort ("KEY_SEQ"); // ordering of columns in FK
				String colName = fks.getString("FKCOLUMN_NAME");

				String destCat    = fks.getString("PKTABLE_CAT");
				String destSchema = fks.getString("PKTABLE_SCHEM");
				String destTable  = fks.getString("PKTABLE_NAME");

				fkColumns.computeIfAbsent(fkName, k -> new ArrayList<>());
				fkDestObj.computeIfAbsent(fkName, k -> new SqlObjectName(conn, destCat, destSchema, destTable));

				// ensure ordered insert
				List<String> cols = fkColumns.get(fkName);
				while (cols.size() < seq) 
					cols.add(null);
				cols.set(seq - 1, colName);
			}
		}

		if (fkColumns.isEmpty()) 
			return Collections.emptyList();

		// what we should return
		List<MissingIndexInfoDetails> returnList = new ArrayList<>();
		
		// Collect indexes: indexName -> ordered list of columns
		Map<String, List<String>> indexes = new HashMap<>();
		try (ResultSet idx = meta.getIndexInfo(null, schema, table, false, false)) 
		{
			while (idx.next()) 
			{
				String idxName = idx.getString("INDEX_NAME");
				String colName = idx.getString("COLUMN_NAME");
				short  pos     = idx.getShort ("ORDINAL_POSITION");

				if (idxName == null || colName == null) 
					continue;

				indexes.computeIfAbsent(idxName, k -> new ArrayList<>());
				List<String> cols = indexes.get(idxName);

				while (cols.size() < pos) 
					cols.add(null);
				cols.set(pos - 1, colName);
			}
		}

		// Now check each FK
		for (Map.Entry<String, List<String>> entry : fkColumns.entrySet()) 
		{
			String       fkName = entry.getKey();
			List<String> fkCols = entry.getValue();

			boolean supported = indexes.values().stream()
					.anyMatch(idxCols -> startsWith(idxCols, fkCols));

			if ( ! supported ) 
			{
				SqlObjectName thisTable = new SqlObjectName(conn, catalog, schema, table);
				SqlObjectName destTable = fkDestObj.get(fkName);
				
//				String indexName = table + "__" + fkName + "__idx";
//				String ddl = String.format(
//					"CREATE INDEX %s ON %s.%s (%s);",
//					indexName,
//					schema,
//					table,
//					String.join(", ", fkCols)
//				);
//				String ddl = factory.createIndex(catalog, schema, table, fkName, fkCols);
				String ddl = factory.createIndex(thisTable, fkName, fkCols, destTable);

				// Translate "Faked Quoted Identifier" to DBMS Specific Quoted Identifier
				if (conn instanceof DbxConnection)
				{
					DbxConnection dbxConn = (DbxConnection) conn;
					ddl = dbxConn.quotifySqlString(ddl);
				}

				System.out.println("");
				System.out.println("-------------------------------------------------------------------------------");
				System.out.printf("Table '%s.%s': ForeignKey '%s' on columns %s to table '%s.%s' has NO supporting index! \n",
						schema, table, fkName, fkCols, destTable.getSchemaName(), destTable.getObjectName());

				System.out.println("Suggested DDL: " + ddl);
				
				// Create entry
				MissingIndexInfoDetails details = new MissingIndexInfoDetails();
				details._catalogName = catalog;
				details._schemaName  = schema;
				details._tableName   = table;

				details._fkName      = fkName;

				details._fkCatalogName = destTable.getCatalogName();
				details._fkShemaName   = destTable.getSchemaName();
				details._fkTableName   = destTable.getObjectName();

				details._indexColumns = fkCols;
				details._suggestedDdl = ddl;

				returnList.add(details);
			}
		}
		
		return returnList;
	}

	private static boolean startsWith(List<String> idxCols, List<String> fkCols) 
	{
		if (idxCols.size() < fkCols.size()) 
			return false;

		for (int i = 0; i < fkCols.size(); i++) 
		{
			if (!fkCols.get(i).equalsIgnoreCase(idxCols.get(i))) 
			{
				return false;
			}
		}
		return true;
	}
}
