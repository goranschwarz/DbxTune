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
package com.asetune.sql.ddl.model;

import java.sql.DatabaseMetaData;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.commons.lang3.StringUtils;

import com.asetune.sql.conn.DbxConnection;
import com.asetune.utils.StringUtil;

public class Catalog
{
	private String       _catalogName;
	private Set<Schema>  _schemas = new LinkedHashSet<>();
	private List<Table>  _tables  = new ArrayList<>();
	private List<View>   _views   = new ArrayList<>();

	public Set<Schema>  getSchemas() { return _schemas; }
	public List<Table>  getTables()  { return _tables; }
	public List<View>   getViews()   { return _views; }

	public void addSchema(Schema schema) { _schemas.add(schema); }
	public void addTable(Table table)    { _tables .add(table); }
	public void addView (View  view )    { _views  .add(view); }
	
	public static Catalog create(DbxConnection conn, String catalogName, CreateFilter filter)
	throws SQLException
	{
		Catalog cat = new Catalog();

		// Set what Catalog we are supposed to USE
		String enterInCatName = conn.getCatalog();
		if (StringUtil.isNullOrBlank(catalogName))
		{
			catalogName = conn.getCatalog();
		}
		else
		{
			conn.setCatalog(catalogName);
		}
		cat._catalogName = catalogName;

		
		// Get TABLES, VIEWS, etc
		// For each Table... do create a Table object
		// Note: Schema Objects will be created by the Table Object etc...
		String[] tableTypesTable = new String[] {"TABLE"};
		String[] tableTypesView  = new String[] {"VIEW"};
		
		// map of: SchemaNames -> ListOfTables
		Map<String, List<String>> schemaTableMap = getSchemaAndObjects(conn, catalogName, tableTypesTable);
		Map<String, List<String>> schemaViewMap  = getSchemaAndObjects(conn, catalogName, tableTypesView);
		
		
		// Create TABLE and SCHEMA objects
		for (String schemaName : schemaTableMap.keySet())
		{
			Schema schema = new Schema(catalogName, schemaName);
			cat.addSchema(schema);
			
			List<String> tables = schemaTableMap.get(schemaName);
			for (String tableName : tables)
			{
				Table table = Table.create(conn, catalogName, schemaName, tableName);
				
				cat    .addTable(table);
				schema.addTable(table);
			}
		}
		
		// Create VIEW and SCHEMA objects
		for (String schemaName : schemaViewMap.keySet())
		{
			Schema schema = new Schema(catalogName, schemaName);
			cat.addSchema(schema);

			List<String> views = schemaViewMap.get(schemaName);
			for (String viewName : views)
			{
				View view = View.create(conn, catalogName, schemaName, viewName);
				
				cat    .addView(view);
				schema.addView(view);
			}
		}
		
		// If we want to grab definitions for:
		// - Sequences
		// - View text
		// - Procedure text
		// - etc...
		// Then we need to use DBMS specific implementations (which is done/implemented in every DbxConnection class)
		
		// At the end restore to the same catalog as we entered in.
		if (StringUtil.hasValue(enterInCatName) && !enterInCatName.equals(catalogName))
		{
			conn.setCatalog(catalogName);
		}

		//TODO: add SORTING:  "correct ForeignKey" order... 
		//      possibly also "correct View" order

		
		return cat;
	}

	/**
	 * Return a Map with <br>
	 * - key:   SchemaName <br>
	 * - value: List(TableNames) <br>
	 * 
	 * @param conn
	 * @param catalogName
	 * @param tableTypes
	 * @return
	 * @throws SQLException
	 */
	private static Map<String, List<String>> getSchemaAndObjects(DbxConnection conn, String catalogName, String[] tableTypes)
	throws SQLException
	{
		DatabaseMetaData dbmd = conn.getMetaData();
		Map<String, List<String>> schemaTableMap = new HashMap<>();
		
		ResultSet rs = dbmd.getTables(catalogName, null, "%", tableTypes); // get ALL Tables
		while(rs.next())
		{
			String tmpSchemaName = StringUtils.trim(rs.getString(2));
			String tmpTableName  = StringUtils.trim(rs.getString(3));
			
			List<String> tables = schemaTableMap.get(tmpSchemaName);
			if (tables == null)
			{
				tables = new ArrayList<>();
				schemaTableMap.put(tmpSchemaName, tables);
			}
			tables.add(tmpTableName);
		}
		rs.close();
		
		return schemaTableMap;
	}
	
	/**
	 * FIXME: This needs to be extended to hold various filters that can be used in the future
	 */
	public static class CreateFilter
	{
	}

//	protected List<TableInfo> refreshCompletionForTables(Connection conn, WaitForExecDialog waitDialog, String catalogName, String schemaName, String tableName)
//	throws SQLException
//	{
////System.out.println("SQL: refreshCompletionForTables()");
//		// Obtain a DatabaseMetaData object from our current connection        
//		DatabaseMetaData dbmd = conn.getMetaData();
//
//		// Each table description has the following columns: 
//		// 1:  TABLE_CAT String                 => table catalog (may be null) 
//		// 2:  TABLE_SCHEM String               => table schema (may be null) 
//		// 3:  TABLE_NAME String                => table name 
//		// 4:  TABLE_TYPE String                => table type. Typical types are "TABLE", "VIEW", "SYSTEM TABLE", "GLOBAL TEMPORARY", "LOCAL TEMPORARY", "ALIAS", "SYNONYM". 
//		// 5:  REMARKS String                   => explanatory comment on the table 
//		// 6:  TYPE_CAT String                  => the types catalog (may be null) 
//		// 7:  TYPE_SCHEM String                => the types schema (may be null) 
//		// 8:  TYPE_NAME String                 => type name (may be null) 
//		// 9:  SELF_REFERENCING_COL_NAME String => name of the designated "identifier" column of a typed table (may be null) 
//		// 10: REF_GENERATION String            => specifies how values in SELF_REFERENCING_COL_NAME are created. Values are "SYSTEM", "USER", "DERIVED". (may be null) 
//
//		final String stateMsg = "Getting Table information.";
//		waitDialog.setState(stateMsg);
//
//		ArrayList<TableInfo> tableInfoList = new ArrayList<TableInfo>();
//
//		if (waitDialog.wasCancelPressed())
//			return tableInfoList;
//
//		if (schemaName != null)
//		{
//			schemaName = schemaName.replace('*', '%').trim();
//			if ( isWildcatdMath() && ! schemaName.endsWith("%") )
//				schemaName += "%";
//		}
//
//		if (tableName == null)
//			tableName = "%";
//		else
//		{
//			tableName = tableName.replace('*', '%').trim();
//			if ( isWildcatdMath() && ! tableName.endsWith("%") )
//				tableName += "%";
//		}
//
//		// What table types do we want to retrieve
//		String[] types = getTableTypes(conn);
//
//		if (_logger.isDebugEnabled())
//			_logger.debug("refreshCompletionForTables(): calling dbmd.getTables(catalog='"+catalogName+"', schema='"+schemaName+"', table='"+tableName+"', types='"+StringUtil.toCommaStr(types)+"')");
//
//		ResultSet rs = dbmd.getTables(catalogName, schemaName, tableName, types);
//		
//		MonTablesDictionary mtd = MonTablesDictionaryManager.hasInstance() ? MonTablesDictionaryManager.getInstance() : null;
//		int counter = 0;
//		while(rs.next())
//		{
//			counter++;
//			if ( (counter % 100) == 0 )
//				waitDialog.setState(stateMsg + " (Fetch count "+counter+")");
//
//			TableInfo ti = new TableInfo();
//			ti._tabCat     = StringUtils.trim(rs.getString(1));
//			ti._tabSchema  = StringUtils.trim(rs.getString(2));
//			ti._tabName    = StringUtils.trim(rs.getString(3));
//			ti._tabType    = StringUtils.trim(rs.getString(4));
//			ti._tabRemark  = StringUtils.trim(rs.getString(5));
//
//			// Check with the MonTable dictionary for Descriptions
//			if (mtd != null && StringUtil.isNullOrBlank(ti._tabRemark))
//				ti._tabRemark = mtd.getDescriptionForTable(ti._tabName);
//				
//			// add schemas... this is a Set so duplicates is ignored
//			addSchema(ti._tabCat, ti._tabSchema);
//			
//			// special case for MySQL and DBMS that do not support schemas... just copy dbname into the schema field
//			if ( ! _dbSupportsSchema && StringUtil.isNullOrBlank(ti._tabSchema))
//				ti._tabSchema = ti._tabCat;
//
//			tableInfoList.add(ti);
//			
//			if (waitDialog.wasCancelPressed())
//				return tableInfoList;
//		}
//		rs.close();
//
//		return tableInfoList;
//	}



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
		return result;
	}
	
	/**
	 * Uses member 'dbname' as the equality 
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
		Catalog other = (Catalog) obj;
		if ( _catalogName == null )
		{
			if ( other._catalogName != null )
				return false;
		}
		else if ( !_catalogName.equals(other._catalogName) )
			return false;
		return true;
	}

}
