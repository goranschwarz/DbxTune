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
package com.dbxtune.test;

import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.Set;

public class TableFkDependencyResolver
{
	public static void main(String[] args) throws Exception
	{
//		String url    = "jdbc:sybase:Tds:gorans-ub3.home:1600/dummy1?ENCRYPT_PASSWORD=true&ENABLE_SSL=false&SSL_TRUST_ALL_CERTS=false";
		String url    = "jdbc:sybase:Tds:gorans-ub3.home:1600/dummy1";
		String user   = "sa";
		String passwd = "sybase";
		
		if (true)
		{
			url    = "jdbc:postgresql://gorans-ub3.home:5432/six_new_finbas";
			user   = "gorans";
			passwd = "1niss2e";
						
		}
		if (true)
		{
			url    = "jdbc:sqlserver://localhost:1433;databaseName=AdventureWorks2019;encrypt=true;trustServerCertificate=true";
			user   = "gorans_sa";
			passwd = "1niss2e";
						
		}


		System.out.println("Usage: java TableFkDependencyResolver <JDBC_URL> <USERNAME> <PASSWORD>");

		if (args.length >= 1) url    = args[0];
		if (args.length >= 2) user   = args[1];
		if (args.length >= 3) passwd = args[2];

		System.out.println("");
		System.out.println("Using Connection values:");
		System.out.println("  - url:    " + url);
		System.out.println("  - user:   " + user);
		System.out.println("  - passwd: " + passwd);
		System.out.println("");
		
		try (Connection conn = DriverManager.getConnection(url, user, passwd))
		{
			LinkedHashMap<String, List<String>> sortedTables = getAllTables(conn);

			System.out.println("Tables in correct FK order:");
			for (Map.Entry<String, List<String>> entry : sortedTables.entrySet())
			{
				System.out.println("\t" + entry.getKey());
				for (String refTable : entry.getValue())
				{
					System.out.println("\t\tReferences: " + refTable);
				}
			}
		}
	}

	/**
	 * Get all tables in order respecting Foreign Keys
	 * 
	 * @param conn   A connection
	 * @return a LinkedHahMap(tableName, List(referencedTables))
	 * @throws SQLException
	 */
	public static LinkedHashMap<String, List<String>> getAllTables(Connection conn)
	throws SQLException
	{
		return getAllTables(conn, null, null, null);
	}

	/**
	 * Get all tables in order respecting Foreign Keys
	 * 
	 * @param conn           A connection
	 * @param catalogName    Can be null
	 * @param schemaName     Can be null
	 * @param tableName      Can be null (then "%" will be used)
	 * 
	 * @return a LinkedHahMap(tableName, List(referencedTables))
	 * @throws SQLException
	 */
	public static LinkedHashMap<String, List<String>> getAllTables(Connection conn, String inCatalogName, String inSchemaName, String inTableName)
	throws SQLException
	{
		DatabaseMetaData metaData = conn.getMetaData();

		if (inTableName == null)
			inTableName = "%";

		// Step 1: Get all tables
		Map<String, List<String>> dependencies = new HashMap<>();
		Set<String> tables = new HashSet<>();

		try (ResultSet rs = metaData.getTables(inCatalogName, inSchemaName, inTableName, new String[]{"TABLE", "BASE_TABLE"}))
		{
			while (rs.next())
			{
				String schema        = rs.getString("TABLE_SCHEM");
				String tableName     = rs.getString("TABLE_NAME");

				String fullTableName = (schema != null ? schema + "." : "") + tableName;

				dependencies.put(fullTableName, new ArrayList<>());
				tables.add(fullTableName);
			}
		}

		// Step 2: Get foreign key constraints
		for (String fullTable : tables)
		{
			String[] parts = fullTable.split("\\.", 2);
			String schema = parts.length > 1 ? parts[0] : null;
			String table  = parts.length > 1 ? parts[1] : parts[0];

			try (ResultSet rs = metaData.getImportedKeys(null, schema, table))
			{
				while (rs.next())
				{
					String fkSchema = rs.getString("FKTABLE_SCHEM");
					String fkTable  = rs.getString("FKTABLE_NAME");
					String pkSchema = rs.getString("PKTABLE_SCHEM");
					String pkTable  = rs.getString("PKTABLE_NAME");

					String fkFullTable = (fkSchema != null ? fkSchema + "." : "") + fkTable;
					String pkFullTable = (pkSchema != null ? pkSchema + "." : "") + pkTable;

					if (dependencies.containsKey(fkFullTable))
					{
						dependencies.get(fkFullTable).add(pkFullTable);
					}
				}
			}
		}

		// Step 3: Sort tables using Kahn's Algorithm (BFS)
		List<String> sortedTableList = topologicalSort(dependencies);

		// Step 4: Reverse the order for correct dependency order
		Collections.reverse(sortedTableList);

		// Step 5: Create LinkedHashMap to maintain insertion order
		LinkedHashMap<String, List<String>> sortedTables = new LinkedHashMap<>();
		for (String table : sortedTableList)
		{
			sortedTables.put(table, dependencies.get(table));
		}

		return sortedTables;
	}

	// Topological sorting using Kahn's Algorithm (BFS)
	private static List<String> topologicalSort(Map<String, List<String>> dependencies)
	{
		Map<String, Integer> inDegree = new HashMap<>();
		for (String table : dependencies.keySet())
		{
			inDegree.put(table, 0);
		}
		for (List<String> deps : dependencies.values())
		{
			for (String dep : deps)
			{
				inDegree.put(dep, inDegree.getOrDefault(dep, 0) + 1);
			}
		}

		Queue<String> queue = new LinkedList<>();
		for (String table : inDegree.keySet())
		{
			if (inDegree.get(table) == 0)
			{
				queue.add(table);
			}
		}

		List<String> sortedOrder = new ArrayList<>();
		Set<String> cyclicTables = new HashSet<>();

		while (!queue.isEmpty())
		{
			String table = queue.poll();
			sortedOrder.add(table);
			for (String dependent : dependencies.get(table))
			{
				inDegree.put(dependent, inDegree.get(dependent) - 1);
				if (inDegree.get(dependent) == 0)
				{
					queue.add(dependent);
				}
			}
		}

		// Detect cycles
		for (Map.Entry<String, Integer> entry : inDegree.entrySet())
		{
			if (entry.getValue() > 0) // Still has incoming edges -> cyclic dependency
			{
				cyclicTables.add(entry.getKey());
			}
		}

		if (!cyclicTables.isEmpty())
		{
//			throw new RuntimeException("Cycle detected in table dependencies! The following tables are involved in a cycle: " + cyclicTables);
			System.out.println(">>>> WARNING >>>> Cycle detected in table dependencies! The following tables are involved in a cycle: " + cyclicTables);
		}

		return sortedOrder;
	}

//	// Topological sorting using DFS for cycle detection
//	private static List<String> topologicalSort(Map<String, List<String>> dependencies)
//	{
//		List<String> sortedOrder = new ArrayList<>();
//		Set<String>  visited     = new HashSet<>();
//		Set<String>  stack       = new HashSet<>();
//		List<String> cyclePath   = new ArrayList<>();
//
//		for (String table : dependencies.keySet())
//		{
//			if (!visited.contains(table))
//			{
//				if (detectCycleDFS(table, dependencies, visited, stack, sortedOrder, cyclePath))
//				{
//					throw new RuntimeException("Cycle detected in table dependencies! Cycle: " + String.join(" -> ", cyclePath));
//				}
//			}
//		}
//
//		Collections.reverse(sortedOrder);
//		return sortedOrder;
//	}
//
//	// DFS-based cycle detection
//	private static boolean detectCycleDFS(String table, Map<String, List<String>> dependencies, Set<String> visited,
//										  Set<String> stack, List<String> sortedOrder, List<String> cyclePath)
//	{
//		visited  .add(table);
//		stack    .add(table);
//		cyclePath.add(table);
//
//		for (String dependent : dependencies.get(table))
//		{
//			if ( ! visited.contains(dependent) )
//			{
//				if (detectCycleDFS(dependent, dependencies, visited, stack, sortedOrder, cyclePath))
//				{
//					return true; // Cycle found
//				}
//			}
//			else if (stack.contains(dependent))
//			{
//				// Cycle detected, rebuild cycle path
//				cyclePath.add(dependent);
//
//				return true;
//			}
//		}
//
//		stack      .remove(table);
//		cyclePath  .remove(table);
//		sortedOrder.add(table);
//
//		return false;
//	}
}
