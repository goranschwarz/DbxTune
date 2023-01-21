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
package com.asetune.sql.ddl.model;

import java.sql.DatabaseMetaData;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.SortedMap;
import java.util.TreeMap;

import org.apache.commons.lang3.StringUtils;

import com.asetune.sql.conn.DbxConnection;
import com.asetune.sql.ddl.model.Index.IndexType;
import com.asetune.utils.StringUtil;

public class Table
{
	private Catalog _catalog;
	private Schema  _schema;
	
	private String _catalogName;
	private String _schemaName;
	private String _tableName;
	private String _description;
	private String _tabType;
	private String _remark;

	private List<TableColumn> _columns        = new ArrayList<>();
	private List<Index>       _indexes        = new ArrayList<>();
	private List<ForeignKey>  _foreignKeysIn  = new ArrayList<>();   // -->> Incoming FK (from other table pointing to this table)
	private List<ForeignKey>  _foreignKeysOut = new ArrayList<>();   // Outgoing FK -->> (from this table towards other tables)

	private List<String>      _columnNames = new ArrayList<>();

	private List<String>      _pkCols = new ArrayList<>();
	private String            _pkName;

	public Table()
	{
	}
	public Table(String catalogName, String schemaName, String tableName)
	{
		_catalogName = catalogName;
		_schemaName  = schemaName;
		_tableName   = tableName;
	}

	/** Add COLUMN */
	public void add(TableColumn tableColumn)
	{
		// Add column object
		_columns.add(tableColumn);
		
		// Add column the shortcut: _columnNames
		_columnNames.add(tableColumn.getColumnLabel());

		// Set the column position (index position starts at: 1)
		tableColumn._colPos = _columns.size();
	}

	/** Add INDEX */
	public void add(Index index)
	{
		// Add index object
		_indexes.add(index);
	}

	/** Set Primary Key */
	public void setPrimaryKey(String... colNames)
	{
		for (String colName : colNames)
		{
			if ( ! _columnNames.contains(colName) )
				throw new RuntimeException("setPrimaryKey(): column name '" + colName + "' is NOT part of table '" + _tableName + "'. (catalog='" + _catalogName + "', schema='" + _schemaName + "')");

			_pkCols.add(colName);
		}
	}
	
	
	public Catalog getCatalog()     { return _catalog; }
	public Schema  getSchema()      { return _schema; }
	public Schema  getParent()      { return _schema; }

	public String  getCatalogName() { return _catalogName; }
	public String  getSchemaName()  { return _schemaName; }
	public String  getTableName()   { return _tableName; }
	public String  getDescription() { return _description; }
	
	public List<String>      getPkColumns()   { return _pkCols; }
	public String            getPkName()      { return _pkName; }
	public boolean           hasPk()          { return ! _pkCols.isEmpty(); }
	
	public List<TableColumn> getColumns()        { return _columns; }
	public List<Index>       getIndexes()        { return _indexes; }
	public List<ForeignKey>  getForeignKeysIn()  { return _foreignKeysIn; }
	public List<ForeignKey>  getForeignKeysOut() { return _foreignKeysOut; }

	public List<String> getColumnNames()      { return _columnNames; }
	
	/**
	 * Get specification for a specific column
	 * 
	 * @param colName    Name of the column to search for
	 * @return NULL if not found
	 */
	public TableColumn getColumn(String colName)
	{
		for (TableColumn tc : _columns)
		{
			if (tc.getColumnLabel().equals(colName))
				return tc;
		}
		return null;
	}


	public static Table create(DbxConnection conn, String catalogName, String schemaName, String tableName)
	throws SQLException
	{
		return create(conn, catalogName, schemaName, tableName, null);
	}
	public static Table create(DbxConnection conn, String catalogName, String schemaName, String tableName, String[] tableTypes)
	throws SQLException
	{		
		DatabaseMetaData dbmd = conn.getMetaData();
		Table t = new Table();

		// search algorithm
		//  1 - Search for Mixed Case... "as it was passed in"
		//  2 - Search UPPER or LOWER depending on what the DBMS stores it as in the dictionary. 
		
		//--------------------
		// TABLE
		//--------------------
//		String[] tableTypes = null;
		if (tableTypes == null)
//			tableTypes = new String[] {"TABLE", "SYSTEM TABLE"};
			tableTypes = new String[] {"TABLE", "BASE TABLE", "SYSTEM TABLE"};
		
//		String[] DB2_tableTypes        = new String[] {"ALIAS", "HIERARCHY TABLE", "INOPERATIVE VIEW", "MATERIALIZED QUERY TABLE", "NICKNAME", "SYSTEM TABLE", "TABLE", "TYPED TABLE", "TYPED VIEW", "VIEW"};
//		String[] ORACLE_tableTypes     = new String[] {"SYNONYM", "TABLE", "VIEW"};
//		String[] MySQL_tableTypes      = new String[] {"LOCAL TEMPORARY", "SYSTEM TABLE", "SYSTEM VIEW", "TABLE", "VIEW"};
//		String[] POSTGRES_tableTypes   = new String[] {"FOREIGN TABLE", "INDEX", "MATERIALIZED VIEW", "SEQUENCE", "SYSTEM INDEX", "SYSTEM TABLE", "SYSTEM TOAST INDEX", "SYSTEM TOAST TABLE", "SYSTEM VIEW", "TABLE", "TEMPORARY INDEX", "TEMPORARY SEQUENCE", "TEMPORARY TABLE", "TEMPORARY VIEW", "TYPE", "VIEW"};
//		String[] MSSQL_tableTypes      = new String[] {"SYSTEM TABLE", "TABLE", "VIEW"};
//		String[] H2_tableTypes         = new String[] {"EXTERNAL", "SYSTEM TABLE", "TABLE", "TABLE LINK", "VIEW"};
//		String[] SYBASE_ASE_tableTypes = new String[] {"TABLE", "SYSTEM TABLE", "VIEW", "MATERIALIZED VIEW"};
//		String[] XXX_tableTypes        = new String[] {"", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", ""};
		
		ResultSet rs = dbmd.getTables(catalogName, schemaName, tableName, tableTypes);
		int tabCount = 0;
		while(rs.next())
		{
			tabCount++;
			t._catalogName = StringUtils.trim(rs.getString(1));
			t._schemaName  = StringUtils.trim(rs.getString(2));
			t._tableName   = StringUtils.trim(rs.getString(3));
			t._tabType     = StringUtils.trim(rs.getString(4));
			t._remark      = StringUtils.trim(rs.getString(5));
		}
		rs.close();

		if (tabCount > 1)
			throw new SQLException("More than 1 entries was found (tabCount=" + tabCount + ") for: catalogName='" + catalogName + "', schemaName='" + schemaName + "', tableName='" + tableName + "'.");

		if (tabCount == 0)
		{
			boolean storesLowerCaseIdentifiers         = dbmd.storesLowerCaseIdentifiers();
			boolean storesUpperCaseIdentifiers         = dbmd.storesUpperCaseIdentifiers();
			
			String origin_catalogName = catalogName;
			String origin_schemaName  = schemaName ;
			String origin_tableName   = tableName  ;

			if (storesLowerCaseIdentifiers)
			{
				catalogName = origin_catalogName == null ? null : origin_catalogName.toLowerCase();
				schemaName  = origin_schemaName  == null ? null : origin_schemaName .toLowerCase();
				tableName   = origin_tableName   == null ? null : origin_tableName  .toLowerCase();
			}
			if (storesUpperCaseIdentifiers)
			{
				catalogName = origin_catalogName == null ? null : origin_catalogName.toUpperCase();
				schemaName  = origin_schemaName  == null ? null : origin_schemaName .toUpperCase();
				tableName   = origin_tableName   == null ? null : origin_tableName  .toUpperCase();
			}
			
			rs = dbmd.getTables(catalogName, schemaName, tableName, tableTypes);
			tabCount = 0;
			while(rs.next())
			{
				tabCount++;
				t._catalogName = StringUtils.trim(rs.getString(1));
				t._schemaName  = StringUtils.trim(rs.getString(2));
				t._tableName   = StringUtils.trim(rs.getString(3));
				t._tabType     = StringUtils.trim(rs.getString(4));
				t._remark      = StringUtils.trim(rs.getString(5));
			}
			rs.close();

			String msgCatalogName = "catalogName='" + origin_catalogName + "', "; 
			String msgSchemaName  = "schemaName='"  + origin_schemaName  + "', "; 
			String msgTableName   = "tableName='"   + origin_tableName   + "'. "; 
			if (storesLowerCaseIdentifiers || storesUpperCaseIdentifiers)
			{
				msgCatalogName = "catalogName='" + origin_catalogName + "' or '" + catalogName + "', ";
				msgSchemaName  = "schemaName='"  + origin_schemaName  + "' or '" + schemaName  + "', ";
				msgTableName   = "tableName='"   + origin_tableName   + "' or '" + tableName   + "'. ";
			}
			if (tabCount == 0)
				throw new SQLException("No table was found for: " + msgCatalogName + msgSchemaName + msgTableName);
			
			if (tabCount > 1)
				throw new SQLException("More than 1 entries was found (tabCount=" + tabCount + ") for: No table was found for: " + msgCatalogName + msgSchemaName + msgTableName);
		}
		
		//--------------------
		// COLUMNS
		//--------------------
		rs = dbmd.getColumns(catalogName, schemaName, tableName, "%");
		while(rs.next())
		{
			t._catalogName = StringUtils.trim(rs.getString("TABLE_CAT"));
			t._schemaName  = StringUtils.trim(rs.getString("TABLE_SCHEM"));
			t._tableName   = StringUtils.trim(rs.getString("TABLE_NAME"));
			
			TableColumn tc = new TableColumn();
			tc._table          = t;
			tc._colName        = StringUtils.trim(rs.getString("COLUMN_NAME"));
			tc._colPos         =                  rs.getInt   ("ORDINAL_POSITION");
			tc._colJdbcType    =                  rs.getInt   ("DATA_TYPE");
			tc._colType        = StringUtils.trim(rs.getString("TYPE_NAME"));
			tc._colLength      =                  rs.getInt   ("COLUMN_SIZE");
			tc._colIsNullable  =                  rs.getInt   ("NULLABLE");
			tc._colRemark      = StringUtils.trim(rs.getString("REMARKS"));
			tc._colDefault     = StringUtils.trim(rs.getString("COLUMN_DEF"));
			tc._colScale       =                  rs.getInt   ("DECIMAL_DIGITS");
			tc._isColAutoInc   = "yes".equalsIgnoreCase(StringUtils.trim(rs.getString("IS_AUTOINCREMENT")));
//			tc._isColGenerated = "yes".equalsIgnoreCase(StringUtils.trim(rs.getString("IS_GENERATEDCOLUMN")));
			
			t._columns    .add(tc);
			t._columnNames.add(tc._colName);
		}
		rs.close();
		
		//--------------------
		// PK
		//     1 - TABLE_CAT String => table catalog (may be null)
		//     2 - TABLE_SCHEM String => table schema (may be null)
		//     3 - TABLE_NAME String => table name
		//     4 - COLUMN_NAME String => column name
		//     5 - KEY_SEQ short => sequence number within primary key( a value of 1 represents the first column of the primary key, a value of 2 would represent the second column within the primary key).
		//     6 - PK_NAME String => primary key name (may be null)
		//--------------------
		rs = dbmd.getPrimaryKeys(catalogName, schemaName, tableName); 
		SortedMap<Integer, String> sortedPKMap = new TreeMap<>();      // Use SortedMap so we can add 'KEY_SEQ' out of sequence
		while(rs.next())
		{
			String colName = rs.getString("COLUMN_NAME");
			int    colSeq  = rs.getInt   ("KEY_SEQ");
			
			sortedPKMap.put(colSeq, colName);
			
			t._pkName = rs.getString("PK_NAME");
		}
		rs.close();
		t._pkCols = new ArrayList<>(sortedPKMap.values());      // Translate the SortedMap into a List


		//--------------------
		// INDEXES
		//    1  - TABLE_CAT String => table catalog (may be null)
		//    2  - TABLE_SCHEM String => table schema (may be null)
		//    3  - TABLE_NAME String => table name
		//    4  - NON_UNIQUE boolean => Can index values be non-unique. false when TYPE is tableIndexStatistic
		//    5  - INDEX_QUALIFIER String => index catalog (may be null); null when TYPE is tableIndexStatistic
		//    6  - INDEX_NAME String => index name; null when TYPE is tableIndexStatistic
		//    7  - TYPE short => index type:
		//             * tableIndexStatistic - this identifies table statistics that are returned in conjuction with a table's index descriptions
		//             * tableIndexClustered - this is a clustered index
		//             * tableIndexHashed - this is a hashed index
		//             * tableIndexOther - this is some other style of index
		//    8  - ORDINAL_POSITION short => column sequence number within index; zero when TYPE is tableIndexStatistic
		//    9  - COLUMN_NAME String => column name; null when TYPE is tableIndexStatistic
		//    10 - ASC_OR_DESC String => column sort sequence, "A" => ascending, "D" => descending, may be null if sort sequence is not supported; null when TYPE is tableIndexStatistic
		//    11 - CARDINALITY int => When TYPE is tableIndexStatistic, then this is the number of rows in the table; otherwise, it is the number of unique values in the index.
		//    12 - PAGES int => When TYPE is tableIndexStatisic then this is the number of pages used for the table, otherwise it is the number of pages used for the current index.
		//    13 - FILTER_CONDITION String => Filter condition, if any. (may be null)
		//--------------------
		Map<String, Index> tmpIndexMap = new LinkedHashMap<>();
		rs = dbmd.getIndexInfo(t._catalogName, t._schemaName, t._tableName, false, true); // unique=false, approximate=true
		while(rs.next())
		{
			boolean isUnique  = ! rs.getBoolean("NON_UNIQUE");
			String  indexName =   rs.getString ("INDEX_NAME");
			int     indexType =   rs.getInt    ("TYPE");
			int     colSeq    =   rs.getInt    ("ORDINAL_POSITION");
			String  colName   =   rs.getString ("COLUMN_NAME");
			String  ascOrDesc =   rs.getString ("ASC_OR_DESC");

//			CARDINALITY
//			PAGES
//			FILTER_CONDITION
			
			// SKIP STATISTICS
			if (indexType == DatabaseMetaData.tableIndexStatistic)
			{
				continue;
			}

			Index tmpIndex = tmpIndexMap.get(indexName);
			if (tmpIndex == null)
			{
				IndexType indexEnum = IndexType.UNKNOWN;
				if (indexType == DatabaseMetaData.tableIndexOther    ) indexEnum = IndexType.OTHER;
				if (indexType == DatabaseMetaData.tableIndexClustered) indexEnum = IndexType.CLUSTERED;
				if (indexType == DatabaseMetaData.tableIndexHashed   ) indexEnum = IndexType.HASHED;

				tmpIndex = new Index();
				tmpIndex._table    = t;
				tmpIndex._name     = indexName;
				tmpIndex._isUnique = isUnique;
				tmpIndex._type     = indexEnum;

				tmpIndexMap.put(indexName, tmpIndex);
			}
			tmpIndex.addColumnTmp(colSeq, colName, "A".equalsIgnoreCase(ascOrDesc));
		}
		rs.close();
		for (Index tmpIndex : tmpIndexMap.values())
		{
			tmpIndex.addColumnTmpFinalize();
			t._indexes.add(tmpIndex);
		}

		//--------------------
		// FK
		//    1  - PKTABLE_CAT String => primary key table catalog being imported (may be null)
		//    2  - PKTABLE_SCHEM String => primary key table schema being imported (may be null)
		//    3  - PKTABLE_NAME String => primary key table name being imported
		//    4  - PKCOLUMN_NAME String => primary key column name being imported
		//    5  - FKTABLE_CAT String => foreign key table catalog (may be null)
		//    6  - FKTABLE_SCHEM String => foreign key table schema (may be null)
		//    7  - FKTABLE_NAME String => foreign key table name
		//    8  - FKCOLUMN_NAME String => foreign key column name
		//    9  - KEY_SEQ short => sequence number within a foreign key( a value of 1 represents the first column of the foreign key, a value of 2 would represent the second column within the foreign key).
		//    10 - UPDATE_RULE short => What happens to a foreign key when the primary key is updated:
		//              * importedNoAction - do not allow update of primary key if it has been imported
		//              * importedKeyCascade - change imported key to agree with primary key update
		//              * importedKeySetNull - change imported key to NULL if its primary key has been updated
		//              * importedKeySetDefault - change imported key to default values if its primary key has been updated
		//              * importedKeyRestrict - same as importedKeyNoAction (for ODBC 2.x compatibility)
		//    11 - DELETE_RULE short => What happens to the foreign key when primary is deleted.
		//              * importedKeyNoAction - do not allow delete of primary key if it has been imported
		//              * importedKeyCascade - delete rows that import a deleted key
		//              * importedKeySetNull - change imported key to NULL if its primary key has been deleted
		//              * importedKeyRestrict - same as importedKeyNoAction (for ODBC 2.x compatibility)
		//              * importedKeySetDefault - change imported key to default if its primary key has been deleted
		//    12 - FK_NAME String => foreign key name (may be null)
		//    13 - PK_NAME String => primary key name (may be null)
		//    14 - DEFERRABILITY short => can the evaluation of foreign key constraints be deferred until commit
		//              * importedKeyInitiallyDeferred - see SQL92 for definition
		//              * importedKeyInitiallyImmediate - see SQL92 for definition
		//              * importedKeyNotDeferrable - see SQL92 for definition
		//--------------------
		if (StringUtil.hasValue(t._tabType))
		{
//			if ( t._tabType.equalsIgnoreCase("TABLE") || t._tabType.equalsIgnoreCase("SYSTEM") )
			if ( t._tabType.equalsIgnoreCase("TABLE") || t._tabType.equalsIgnoreCase("BASE TABLE") || t._tabType.equalsIgnoreCase("SYSTEM") )
			{
				// (thisTable -->> otherTables)  This table pointing to OTHER tables
				if (true)
				{
					Map<String, ForeignKey> fkMap = new LinkedHashMap<>();

					rs = dbmd.getImportedKeys(t._catalogName, t._schemaName, t._tableName); // unique=false, approximate=false
					while(rs.next())
					{
						String  FK_NAME         = rs.getString("FK_NAME");
						int     DEFERRABILITY   = rs.getInt   ("DEFERRABILITY");
						int     DELETE_RULE     = rs.getInt   ("DELETE_RULE");
						int     UPDATE_RULE     = rs.getInt   ("UPDATE_RULE");
						String  PKTABLE_CAT     = rs.getString("PKTABLE_CAT");
						String  PKTABLE_SCHEM   = rs.getString("PKTABLE_SCHEM");
						String  PKTABLE_NAME    = rs.getString("PKTABLE_NAME");
						String  FKTABLE_CAT     = rs.getString("FKTABLE_CAT");
						String  FKTABLE_SCHEM   = rs.getString("FKTABLE_SCHEM");
						String  FKTABLE_NAME    = rs.getString("FKTABLE_NAME");
						String  FKCOLUMN_NAME   = rs.getString("FKCOLUMN_NAME");
						String  PKCOLUMN_NAME   = rs.getString("PKCOLUMN_NAME");

						if (StringUtil.isNullOrBlank(FK_NAME))
							continue;

						ForeignKey fk = fkMap.get(FK_NAME);
						if (fk == null)
						{
							fk = new ForeignKey(t, 
									FK_NAME, DEFERRABILITY, DELETE_RULE, UPDATE_RULE, 
									PKTABLE_CAT, PKTABLE_SCHEM, PKTABLE_NAME, 
									FKTABLE_CAT, FKTABLE_SCHEM, FKTABLE_NAME);
							fkMap.put(FK_NAME, fk);
						}
						fk.addFkColumn(FKCOLUMN_NAME);
						fk.addPkColumn(PKCOLUMN_NAME);
						
					}
					rs.close();
					t._foreignKeysOut = new ArrayList<>(fkMap.values());      // Translate the SortedMap into a List
				}

				// (otherTables -->> thisTable)  this table is referenced by OTHERS
				if (true)
				{
					Map<String, ForeignKey> fkMap = new LinkedHashMap<>();

					rs = dbmd.getExportedKeys(t._catalogName, t._schemaName, t._tableName); // unique=false, approximate=false
					while(rs.next())
					{
						String  FK_NAME         = rs.getString("FK_NAME");
						int     DEFERRABILITY   = rs.getInt   ("DEFERRABILITY");
						int     DELETE_RULE     = rs.getInt   ("DELETE_RULE");
						int     UPDATE_RULE     = rs.getInt   ("UPDATE_RULE");
						String  PKTABLE_CAT     = rs.getString("PKTABLE_CAT");
						String  PKTABLE_SCHEM   = rs.getString("PKTABLE_SCHEM");
						String  PKTABLE_NAME    = rs.getString("PKTABLE_NAME");
						String  FKTABLE_CAT     = rs.getString("FKTABLE_CAT");
						String  FKTABLE_SCHEM   = rs.getString("FKTABLE_SCHEM");
						String  FKTABLE_NAME    = rs.getString("FKTABLE_NAME");
						String  FKCOLUMN_NAME   = rs.getString("FKCOLUMN_NAME");
						String  PKCOLUMN_NAME   = rs.getString("PKCOLUMN_NAME");

						if (StringUtil.isNullOrBlank(FK_NAME))
							continue;

						ForeignKey fk = fkMap.get(FK_NAME);
						if (fk == null)
						{
							fk = new ForeignKey(t, 
									FK_NAME, DEFERRABILITY, DELETE_RULE, UPDATE_RULE, 
									PKTABLE_CAT, PKTABLE_SCHEM, PKTABLE_NAME, 
									FKTABLE_CAT, FKTABLE_SCHEM, FKTABLE_NAME);
							fkMap.put(FK_NAME, fk);
						}
						fk.addFkColumn(FKCOLUMN_NAME);
						fk.addPkColumn(PKCOLUMN_NAME);
						
					}
					rs.close();
					t._foreignKeysIn = new ArrayList<>(fkMap.values());      // Translate the SortedMap into a List
				}
			}
			else if ( t._tabType.equalsIgnoreCase("VIEW") )
			{
				List<String> viewReferences = conn.getViewReferences(t._catalogName, t._schemaName, t._tableName);
			}
		}

//		if (StringUtil.hasValue(_tabType))
//		{
//			if ( _tabType.equalsIgnoreCase("TABLE") || _tabType.equalsIgnoreCase("SYSTEM") )
//			{
//				//System.out.println("refreshColumnInfo(): _tabCat='"+_tabCat+"', _tabSchema='"+_tabSchema+"', _tabName='"+_tabName+"'.");
//				
////				List<String> pk = TableInfo.getPkOrFirstUniqueIndex(conn, _catalogName, _schemaName, _tableName);
////				_extraInfo = conn.getTableExtraInfo(_catalogName, _schemaName, _tableName);
//
////    			_mdIndex = new ResultSetTableModel( dbmd.getIndexInfo   (_catalogName, _schemaName, _tableName, false, false), "getIndexInfo");
//    			_mdFkOut = new ResultSetTableModel( dbmd.getImportedKeys(_catalogName, _schemaName, _tableName),               "getImportedKeys");
//    			_mdFkIn  = new ResultSetTableModel( dbmd.getExportedKeys(_catalogName, _schemaName, _tableName),               "getExportedKeys");
//			}
//			else if ( _tabType.equalsIgnoreCase("VIEW") )
//			{
//				List<String> viewReferences = conn.getViewReferences(_catalogName, _schemaName, _tableName);
//			}
//		}
		
		return t;
	}




	//-----------------------------------------------------------------------
	//-----------------------------------------------------------------------
	// Basic methods (generated by eclipse)
	//-----------------------------------------------------------------------
	//-----------------------------------------------------------------------
	@Override
	public int hashCode()
	{
		final int prime  = 31;
		int       result = 1;
		result = prime * result + ((_catalogName == null) ? 0 : _catalogName.hashCode());
		result = prime * result + ((_schemaName == null) ? 0 : _schemaName.hashCode());
		result = prime * result + ((_tableName == null) ? 0 : _tableName.hashCode());
		return result;
	}
	
	/**
	 * Uses member 'catalogName', 'schemaName', 'tableName' as the equality 
	 */
	@Override
	public boolean equals(Object obj)
	{
		if ( this == obj )
			return true;
		if ( obj == null )
			return false;
		if ( getClass() != obj.getClass() )
			return false;
		Table other = (Table) obj;
		if ( _catalogName == null )
		{
			if ( other._catalogName != null )
				return false;
		}
		else if ( !_catalogName.equals(other._catalogName) )
			return false;
		if ( _schemaName == null )
		{
			if ( other._schemaName != null )
				return false;
		}
		else if ( !_schemaName.equals(other._schemaName) )
			return false;
		if ( _tableName == null )
		{
			if ( other._tableName != null )
				return false;
		}
		else if ( !_tableName.equals(other._tableName) )
			return false;
		return true;
	}
}
