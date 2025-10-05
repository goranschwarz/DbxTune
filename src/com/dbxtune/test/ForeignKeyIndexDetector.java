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

import java.sql.*;
import java.util.*;

public class ForeignKeyIndexDetector {
    
    public static class ForeignKey {
        public String schema;
        public String table;
        public String column;
        public String referencedTable;
        public String referencedColumn;
        public String constraintName;
        
        @Override
        public String toString() {
            return String.format("%s.%s.%s -> %s.%s (%s)", 
                schema, table, column, referencedTable, referencedColumn, constraintName);
        }
        
        @Override
        public boolean equals(Object o) {
            if (!(o instanceof ForeignKey)) return false;
            ForeignKey fk = (ForeignKey) o;
            return Objects.equals(schema, fk.schema) && 
                   Objects.equals(table, fk.table) && 
                   Objects.equals(column, fk.column);
        }
        
        @Override
        public int hashCode() {
            return Objects.hash(schema, table, column);
        }
    }
    
    public static class IndexColumn {
        public String schema;
        public String table;
        public String column;
        public int position; // Position in composite index
        
        @Override
        public boolean equals(Object o) {
            if (!(o instanceof IndexColumn)) return false;
            IndexColumn ic = (IndexColumn) o;
            return Objects.equals(schema, ic.schema) && 
                   Objects.equals(table, ic.table) && 
                   Objects.equals(column, ic.column);
        }
        
        @Override
        public int hashCode() {
            return Objects.hash(schema, table, column);
        }
    }
    
    private Connection connection;
    private DatabaseMetaData metaData;
    private String databaseType;
    
    public ForeignKeyIndexDetector(Connection connection) throws SQLException {
        this.connection = connection;
        this.metaData = connection.getMetaData();
        this.databaseType = metaData.getDatabaseProductName().toLowerCase();
    }
    
    public List<ForeignKey> findMissingIndexes() throws SQLException {
        Set<ForeignKey> foreignKeys = getAllForeignKeys();
        Set<IndexColumn> indexedColumns = getAllIndexedColumns();
        
        List<ForeignKey> missingIndexes = new ArrayList<>();
        
        for (ForeignKey fk : foreignKeys) {
            IndexColumn indexCol = new IndexColumn();
            indexCol.schema = fk.schema;
            indexCol.table = fk.table;
            indexCol.column = fk.column;
            
            if (!indexedColumns.contains(indexCol)) {
                missingIndexes.add(fk);
            }
        }
        
        return missingIndexes;
    }
    
    private Set<ForeignKey> getAllForeignKeys() throws SQLException {
        Set<ForeignKey> foreignKeys = new HashSet<>();
        
        // Get all tables first
        ResultSet tables = metaData.getTables(null, null, "%", new String[]{"TABLE"});
        
        while (tables.next()) {
            String catalog = tables.getString("TABLE_CAT");
            String schema = tables.getString("TABLE_SCHEM");
            String tableName = tables.getString("TABLE_NAME");
            
            // Get imported foreign keys for this table
            ResultSet fkResultSet = metaData.getImportedKeys(catalog, schema, tableName);
            
            while (fkResultSet.next()) {
                ForeignKey fk = new ForeignKey();
                fk.schema = fkResultSet.getString("FKTABLE_SCHEM");
                fk.table = fkResultSet.getString("FKTABLE_NAME");
                fk.column = fkResultSet.getString("FKCOLUMN_NAME");
                fk.referencedTable = fkResultSet.getString("PKTABLE_NAME");
                fk.referencedColumn = fkResultSet.getString("PKCOLUMN_NAME");
                fk.constraintName = fkResultSet.getString("FK_NAME");
                
                foreignKeys.add(fk);
            }
            fkResultSet.close();
        }
        tables.close();
        
        return foreignKeys;
    }
    
    private Set<IndexColumn> getAllIndexedColumns() throws SQLException {
        Set<IndexColumn> indexedColumns = new HashSet<>();
        
        // Get all tables
        ResultSet tables = metaData.getTables(null, null, "%", new String[]{"TABLE"});
        
        while (tables.next()) {
            String catalog = tables.getString("TABLE_CAT");
            String schema = tables.getString("TABLE_SCHEM");
            String tableName = tables.getString("TABLE_NAME");
            
            // Get all indexes for this table
            ResultSet indexInfo = metaData.getIndexInfo(catalog, schema, tableName, false, false);
            
            while (indexInfo.next()) {
                // Skip table statistics (non_unique = null means table stats, not index)
                if (indexInfo.getString("INDEX_NAME") == null) {
                    continue;
                }
                
                IndexColumn indexCol = new IndexColumn();
                indexCol.schema = schema;
                indexCol.table = tableName;
                indexCol.column = indexInfo.getString("COLUMN_NAME");
                indexCol.position = indexInfo.getInt("ORDINAL_POSITION");
                
                // Only consider the first column of composite indexes for FK matching
                // (FK columns need to be the leading column in an index to be useful)
                if (indexCol.position == 1) {
                    indexedColumns.add(indexCol);
                }
            }
            indexInfo.close();
        }
        tables.close();
        
        return indexedColumns;
    }
    
    public void generateIndexSuggestions(List<ForeignKey> missingIndexes) {
        System.out.println("\n=== SUGGESTED INDEX CREATION STATEMENTS ===");
        
        for (ForeignKey fk : missingIndexes) {
            String indexName = String.format("idx_%s_%s", fk.table, fk.column);
            String createIndexSQL = generateCreateIndexSQL(fk, indexName);
            System.out.println(createIndexSQL);
        }
    }
    
    private String generateCreateIndexSQL(ForeignKey fk, String indexName) {
        String tableName = fk.schema != null ? fk.schema + "." + fk.table : fk.table;
        
        // Basic SQL standard syntax
        String sql = String.format("CREATE INDEX %s ON %s (%s);", 
            indexName, tableName, fk.column);
        
        // Database-specific optimizations could be added here
        switch (databaseType) {
            case "postgresql":
                sql = String.format("CREATE INDEX CONCURRENTLY %s ON %s (%s);", 
                    indexName, tableName, fk.column);
                break;
            case "mysql":
                sql = String.format("CREATE INDEX %s ON %s (%s);", 
                    indexName, tableName, fk.column);
                break;
            // Add other database-specific syntax as needed
        }
        
        return sql;
    }
    
    public static void main(String[] args) {
        // Example usage
//        String url = "jdbc:postgresql://localhost:5432/mydb";
//        String username = "user";
//        String password = "password";
		String url      = "jdbc:sybase:Tds:dba-1-ase:5000/PML";
		String username = "sa";
		String password = "sjhyr564s_Wq26kl73";
        
        try (Connection conn = DriverManager.getConnection(url, username, password)) {
            ForeignKeyIndexDetector detector = new ForeignKeyIndexDetector(conn);
            
            System.out.println("Analyzing database for missing foreign key indexes...\n");
            
            List<ForeignKey> missingIndexes = detector.findMissingIndexes();
            
            if (missingIndexes.isEmpty()) {
                System.out.println("OK: All foreign keys have corresponding indexes!");
            } else {
                System.out.println("WARNING: Found " + missingIndexes.size() + " foreign keys without indexes:");
                System.out.println("=".repeat(60));
                
                for (ForeignKey fk : missingIndexes) {
                    System.out.println(fk);
                }
                
                detector.generateIndexSuggestions(missingIndexes);
            }
            
        } catch (SQLException e) {
            System.err.println("Database error: " + e.getMessage());
            e.printStackTrace();
        }
    }
}
