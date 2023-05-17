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
package com.asetune.sql;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

import com.asetune.utils.DbUtils;

public class SqlObjectNameTest
{

	//--------------------------------------------------------------------------
	// Sybase tests
	//--------------------------------------------------------------------------
	@Test
	public void syb_t1_autoDbo()
	{
		String dbProductName                    = DbUtils.DB_PROD_NAME_SYBASE_ASE;
		String dbIdentifierQuoteString          = "\"";
		boolean dbStoresUpperCaseIdentifiers    = false;
		boolean dbStoresLowerCaseIdentifiers    = false;
		boolean dbSupportsSchema                = true;
		boolean autoAddDboForSybaseAndSqlServer = true;
		
		SqlObjectName sqlObj = new SqlObjectName("t1", dbProductName, dbIdentifierQuoteString, dbStoresUpperCaseIdentifiers, dbStoresLowerCaseIdentifiers, dbSupportsSchema, autoAddDboForSybaseAndSqlServer);
		
		// Catalog
		assertEquals( "",   sqlObj.getCatalogName());
		assertEquals( null, sqlObj.getCatalogNameNull());
		assertEquals( "",   sqlObj.getCatalogNameOrigin());
		assertEquals( null, sqlObj.getCatalogNameOriginNull());
		assertEquals( "",   sqlObj.getCatalogNameUnModified());
		assertEquals( null, sqlObj.getCatalogNameUnModifiedNull());

		// Schema
		assertEquals( "dbo",sqlObj.getSchemaName());
		assertEquals( "dbo",sqlObj.getSchemaNameNull());
		assertEquals( "dbo",sqlObj.getSchemaNameOrigin());
		assertEquals( "dbo",sqlObj.getSchemaNameOriginNull());
		assertEquals( "dbo",sqlObj.getSchemaNameUnModified());
		assertEquals( "dbo",sqlObj.getSchemaNameUnModifiedNull());

		// Object
		assertEquals( "t1", sqlObj.getObjectName());
		assertEquals( "t1", sqlObj.getObjectNameNull());
		assertEquals( "t1", sqlObj.getObjectNameOrigin());
		assertEquals( "t1", sqlObj.getObjectNameOriginNull());
		assertEquals( "t1", sqlObj.getObjectNameUnModified());
		assertEquals( "t1", sqlObj.getObjectNameUnModifiedNull());

		// Full Compound
		assertEquals( "dbo.t1", sqlObj.getFullName());
		assertEquals( "dbo.t1", sqlObj.getFullNameOrigin());
		assertEquals( "dbo.t1", sqlObj.getFullNameUnModified());
	}

	@Test
	public void syb_t1_noAutoDbo()
	{
		String dbProductName                    = DbUtils.DB_PROD_NAME_SYBASE_ASE;
		String dbIdentifierQuoteString          = "\"";
		boolean dbStoresUpperCaseIdentifiers    = false;
		boolean dbStoresLowerCaseIdentifiers    = false;
		boolean dbSupportsSchema                = true;
		boolean autoAddDboForSybaseAndSqlServer = false;
		
		SqlObjectName sqlObj = new SqlObjectName("t1", dbProductName, dbIdentifierQuoteString, dbStoresUpperCaseIdentifiers, dbStoresLowerCaseIdentifiers, dbSupportsSchema, autoAddDboForSybaseAndSqlServer);
		
		// Catalog
		assertEquals( "",   sqlObj.getCatalogName());
		assertEquals( null, sqlObj.getCatalogNameNull());
		assertEquals( "",   sqlObj.getCatalogNameOrigin());
		assertEquals( null, sqlObj.getCatalogNameOriginNull());
		assertEquals( "",   sqlObj.getCatalogNameUnModified());
		assertEquals( null, sqlObj.getCatalogNameUnModifiedNull());
		
		// Schema
		assertEquals( "",   sqlObj.getSchemaName());
		assertEquals( null, sqlObj.getSchemaNameNull());
		assertEquals( "",   sqlObj.getSchemaNameOrigin());
		assertEquals( null, sqlObj.getSchemaNameOriginNull());
		assertEquals( "",   sqlObj.getSchemaNameUnModified());
		assertEquals( null, sqlObj.getSchemaNameUnModifiedNull());
		
		// Object
		assertEquals( "t1", sqlObj.getObjectName());
		assertEquals( "t1", sqlObj.getObjectNameNull());
		assertEquals( "t1", sqlObj.getObjectNameOrigin());
		assertEquals( "t1", sqlObj.getObjectNameOriginNull());
		assertEquals( "t1", sqlObj.getObjectNameUnModified());
		assertEquals( "t1", sqlObj.getObjectNameUnModifiedNull());
		
		// Full Compound
		assertEquals( "t1", sqlObj.getFullName());
		assertEquals( "t1", sqlObj.getFullNameOrigin());
		assertEquals( "t1", sqlObj.getFullNameUnModified());
	}

	@Test
	public void syb_dbo_tabWithSpace()
	{
		String dbProductName                    = DbUtils.DB_PROD_NAME_SYBASE_ASE;
		String dbIdentifierQuoteString          = "\"";
		boolean dbStoresUpperCaseIdentifiers    = false;
		boolean dbStoresLowerCaseIdentifiers    = false;
		boolean dbSupportsSchema                = true;
		boolean autoAddDboForSybaseAndSqlServer = true;
		
		SqlObjectName sqlObj = new SqlObjectName("dbo.with space", dbProductName, dbIdentifierQuoteString, dbStoresUpperCaseIdentifiers, dbStoresLowerCaseIdentifiers, dbSupportsSchema, autoAddDboForSybaseAndSqlServer);

		// Catalog
		assertEquals( "",   sqlObj.getCatalogName());
		assertEquals( null, sqlObj.getCatalogNameNull());
		assertEquals( "",   sqlObj.getCatalogNameOrigin());
		assertEquals( null, sqlObj.getCatalogNameOriginNull());
		assertEquals( "",   sqlObj.getCatalogNameUnModified());
		assertEquals( null, sqlObj.getCatalogNameUnModifiedNull());
		
		// Schema
		assertEquals( "dbo",sqlObj.getSchemaName());
		assertEquals( "dbo",sqlObj.getSchemaNameNull());
		assertEquals( "dbo",sqlObj.getSchemaNameOrigin());
		assertEquals( "dbo",sqlObj.getSchemaNameOriginNull());
		assertEquals( "dbo",sqlObj.getSchemaNameUnModified());
		assertEquals( "dbo",sqlObj.getSchemaNameUnModifiedNull());
		
		// Object
		assertEquals( "with space", sqlObj.getObjectName());
		assertEquals( "with space", sqlObj.getObjectNameNull());
		assertEquals( "with space", sqlObj.getObjectNameOrigin());
		assertEquals( "with space", sqlObj.getObjectNameOriginNull());
		assertEquals( "with space", sqlObj.getObjectNameUnModified());
		assertEquals( "with space", sqlObj.getObjectNameUnModifiedNull());

		// Full Compound
		assertEquals( "dbo.with space", sqlObj.getFullName());
		assertEquals( "dbo.with space", sqlObj.getFullNameOrigin());
		assertEquals( "dbo.with space", sqlObj.getFullNameUnModified());
	}

	@Test
	public void syb_dbo_tabWithSpaceQuotedInput1()
	{
		String dbProductName                    = DbUtils.DB_PROD_NAME_SYBASE_ASE;
		String dbIdentifierQuoteString          = "\"";
		boolean dbStoresUpperCaseIdentifiers    = false;
		boolean dbStoresLowerCaseIdentifiers    = false;
		boolean dbSupportsSchema                = true;
		boolean autoAddDboForSybaseAndSqlServer = true;
		
		SqlObjectName sqlObj = new SqlObjectName("dbo.\"with space\"", dbProductName, dbIdentifierQuoteString, dbStoresUpperCaseIdentifiers, dbStoresLowerCaseIdentifiers, dbSupportsSchema, autoAddDboForSybaseAndSqlServer);
		
		// Catalog
		assertEquals( "",   sqlObj.getCatalogName());
		assertEquals( null, sqlObj.getCatalogNameNull());
		assertEquals( "",   sqlObj.getCatalogNameOrigin());
		assertEquals( null, sqlObj.getCatalogNameOriginNull());
		assertEquals( "",   sqlObj.getCatalogNameUnModified());
		assertEquals( null, sqlObj.getCatalogNameUnModifiedNull());
		
		// Schema
		assertEquals( "dbo",sqlObj.getSchemaName());
		assertEquals( "dbo",sqlObj.getSchemaNameNull());
		assertEquals( "dbo",sqlObj.getSchemaNameOrigin());
		assertEquals( "dbo",sqlObj.getSchemaNameOriginNull());
		assertEquals( "dbo",sqlObj.getSchemaNameUnModified());
		assertEquals( "dbo",sqlObj.getSchemaNameUnModifiedNull());
		
		// Object
		assertEquals( "with space", sqlObj.getObjectName());
		assertEquals( "with space", sqlObj.getObjectNameNull());
		assertEquals( "with space", sqlObj.getObjectNameOrigin());
		assertEquals( "with space", sqlObj.getObjectNameOriginNull());
		assertEquals( "\"with space\"", sqlObj.getObjectNameUnModified());
		assertEquals( "\"with space\"", sqlObj.getObjectNameUnModifiedNull());

		// Full Compound
		assertEquals( "dbo.with space", sqlObj.getFullName());
		assertEquals( "dbo.with space", sqlObj.getFullNameOrigin());
		assertEquals( "dbo.\"with space\"", sqlObj.getFullNameUnModified());
	}

	@Test
	public void syb_dbo_tabWithSpaceQuotedInput2()
	{
		String dbProductName                    = DbUtils.DB_PROD_NAME_SYBASE_ASE;
		String dbIdentifierQuoteString          = "\"";
		boolean dbStoresUpperCaseIdentifiers    = false;
		boolean dbStoresLowerCaseIdentifiers    = false;
		boolean dbSupportsSchema                = true;
		boolean autoAddDboForSybaseAndSqlServer = true;
		
		SqlObjectName sqlObj = new SqlObjectName("dbo.[with space]", dbProductName, dbIdentifierQuoteString, dbStoresUpperCaseIdentifiers, dbStoresLowerCaseIdentifiers, dbSupportsSchema, autoAddDboForSybaseAndSqlServer);

		// Catalog
		assertEquals( "",   sqlObj.getCatalogName());
		assertEquals( null, sqlObj.getCatalogNameNull());
		assertEquals( "",   sqlObj.getCatalogNameOrigin());
		assertEquals( null, sqlObj.getCatalogNameOriginNull());
		assertEquals( "",   sqlObj.getCatalogNameUnModified());
		assertEquals( null, sqlObj.getCatalogNameUnModifiedNull());
		
		// Schema
		assertEquals( "dbo",sqlObj.getSchemaName());
		assertEquals( "dbo",sqlObj.getSchemaNameNull());
		assertEquals( "dbo",sqlObj.getSchemaNameOrigin());
		assertEquals( "dbo",sqlObj.getSchemaNameOriginNull());
		assertEquals( "dbo",sqlObj.getSchemaNameUnModified());
		assertEquals( "dbo",sqlObj.getSchemaNameUnModifiedNull());
		
		// Object
		assertEquals( "with space", sqlObj.getObjectName());
		assertEquals( "with space", sqlObj.getObjectNameNull());
		assertEquals( "with space", sqlObj.getObjectNameOrigin());
		assertEquals( "with space", sqlObj.getObjectNameOriginNull());
		assertEquals( "[with space]", sqlObj.getObjectNameUnModified());
		assertEquals( "[with space]", sqlObj.getObjectNameUnModifiedNull());

		// Full Compound
		assertEquals( "dbo.with space", sqlObj.getFullName());
		assertEquals( "dbo.with space", sqlObj.getFullNameOrigin());
		assertEquals( "dbo.[with space]", sqlObj.getFullNameUnModified());
	}

	@Test
	public void syb_dbo_tabWithSpaceQuotedOutput1()
	{
		String dbProductName                    = DbUtils.DB_PROD_NAME_SYBASE_ASE;
		String dbIdentifierQuoteString          = "\"";
		boolean dbStoresUpperCaseIdentifiers    = false;
		boolean dbStoresLowerCaseIdentifiers    = false;
		boolean dbSupportsSchema                = true;
		boolean autoAddDboForSybaseAndSqlServer = true;
		
		SqlObjectName sqlObj = new SqlObjectName("dbo.with space", dbProductName, dbIdentifierQuoteString, dbStoresUpperCaseIdentifiers, dbStoresLowerCaseIdentifiers, dbSupportsSchema, autoAddDboForSybaseAndSqlServer);
		
		// Catalog
		assertEquals( "",   sqlObj.getCatalogNameQuoted());
		assertEquals( "",   sqlObj.getCatalogNameOriginQuoted());
		
		// Schema
		assertEquals( "[dbo]",sqlObj.getSchemaNameQuoted());
		assertEquals( "[dbo]",sqlObj.getSchemaNameOriginQuoted());
		
		// Object
		assertEquals( "[with space]", sqlObj.getObjectNameQuoted());
		assertEquals( "[with space]", sqlObj.getObjectNameOriginQuoted());

		// Full Compound
		assertEquals( "[dbo].[with space]", sqlObj.getFullNameQuoted());
		assertEquals( "[dbo].[with space]", sqlObj.getFullNameOriginQuoted());
	}

	@Test
	public void syb_dbo_tabWithSpaceQuotedOutput2()
	{
		String dbProductName                    = DbUtils.DB_PROD_NAME_SYBASE_ASE;
		String dbIdentifierQuoteString          = "\"";
		boolean dbStoresUpperCaseIdentifiers    = false;
		boolean dbStoresLowerCaseIdentifiers    = false;
		boolean dbSupportsSchema                = true;
		boolean autoAddDboForSybaseAndSqlServer = true;
		
		SqlObjectName sqlObj = new SqlObjectName("dbo.\"with space\"", dbProductName, dbIdentifierQuoteString, dbStoresUpperCaseIdentifiers, dbStoresLowerCaseIdentifiers, dbSupportsSchema, autoAddDboForSybaseAndSqlServer);
		
		// Catalog
		assertEquals( "",   sqlObj.getCatalogNameQuoted());
		assertEquals( "",   sqlObj.getCatalogNameOriginQuoted());
		
		// Schema
		assertEquals( "[dbo]",sqlObj.getSchemaNameQuoted());
		assertEquals( "[dbo]",sqlObj.getSchemaNameOriginQuoted());
		
		// Object
		assertEquals( "[with space]", sqlObj.getObjectNameQuoted());
		assertEquals( "[with space]", sqlObj.getObjectNameOriginQuoted());

		// Full Compound
		assertEquals( "[dbo].[with space]", sqlObj.getFullNameQuoted());
		assertEquals( "[dbo].[with space]", sqlObj.getFullNameOriginQuoted());
	}
	
	@Test
	public void syb_dbo_tabWithSpaceQuotedOutput3()
	{
		String dbProductName                    = DbUtils.DB_PROD_NAME_SYBASE_ASE;
		String dbIdentifierQuoteString          = "\"";
		boolean dbStoresUpperCaseIdentifiers    = false;
		boolean dbStoresLowerCaseIdentifiers    = false;
		boolean dbSupportsSchema                = true;
		boolean autoAddDboForSybaseAndSqlServer = true;
		
		SqlObjectName sqlObj = new SqlObjectName("dbo.[with space]", dbProductName, dbIdentifierQuoteString, dbStoresUpperCaseIdentifiers, dbStoresLowerCaseIdentifiers, dbSupportsSchema, autoAddDboForSybaseAndSqlServer);
		
		// Catalog
		assertEquals( "",   sqlObj.getCatalogNameQuoted());
		assertEquals( "",   sqlObj.getCatalogNameOriginQuoted());
		
		// Schema
		assertEquals( "[dbo]",sqlObj.getSchemaNameQuoted());
		assertEquals( "[dbo]",sqlObj.getSchemaNameOriginQuoted());
		
		// Object
		assertEquals( "[with space]", sqlObj.getObjectNameQuoted());
		assertEquals( "[with space]", sqlObj.getObjectNameOriginQuoted());

		// Full Compound
		assertEquals( "[dbo].[with space]", sqlObj.getFullNameQuoted());
		assertEquals( "[dbo].[with space]", sqlObj.getFullNameOriginQuoted());
	}

	
	@Test
	public void syb_dbo_quotifyIfNeeded1()
	{
		String dbProductName                    = DbUtils.DB_PROD_NAME_SYBASE_ASE;
		String dbIdentifierQuoteString          = "\"";
		boolean dbStoresUpperCaseIdentifiers    = false;
		boolean dbStoresLowerCaseIdentifiers    = false;
		boolean dbSupportsSchema                = true;
		boolean autoAddDboForSybaseAndSqlServer = true;
		
		SqlObjectName sqlObj = new SqlObjectName("dbo.with space", dbProductName, dbIdentifierQuoteString, dbStoresUpperCaseIdentifiers, dbStoresLowerCaseIdentifiers, dbSupportsSchema, autoAddDboForSybaseAndSqlServer);
		
		// Catalog
		assertEquals( "",   sqlObj.getCatalogNameQuoted());
		assertEquals( "",   sqlObj.getCatalogNameQuotedIfNeeded());
		assertEquals( "",   sqlObj.getCatalogNameOriginQuoted());
		assertEquals( "",   sqlObj.getCatalogNameOriginQuotedIfNeeded());
		
		// Schema
		assertEquals( "[dbo]" , sqlObj.getSchemaNameQuoted());
		assertEquals( "dbo"   , sqlObj.getSchemaNameQuotedIfNeeded());
		assertEquals( "[dbo]" , sqlObj.getSchemaNameOriginQuoted());
		assertEquals( "dbo"   , sqlObj.getSchemaNameOriginQuotedIfNeeded());
		
		// Object
		assertEquals( "[with space]", sqlObj.getObjectNameQuoted());
		assertEquals( "[with space]", sqlObj.getObjectNameQuotedIfNeeded());
		assertEquals( "[with space]", sqlObj.getObjectNameOriginQuoted());
		assertEquals( "[with space]", sqlObj.getObjectNameOriginQuotedIfNeeded());

		// Full Compound
		assertEquals( "[dbo].[with space]" , sqlObj.getFullNameQuoted());
		assertEquals( "dbo.[with space]"   , sqlObj.getFullNameQuotedIfNeeded());
		assertEquals( "[dbo].[with space]" , sqlObj.getFullNameOriginQuoted());
		assertEquals( "dbo.[with space]"   , sqlObj.getFullNameOriginQuotedIfNeeded());
	}

	@Test
	public void syb_dbo_quotifyIfNeeded2()
	{
		String dbProductName                    = DbUtils.DB_PROD_NAME_SYBASE_ASE;
		String dbIdentifierQuoteString          = "\"";
		boolean dbStoresUpperCaseIdentifiers    = false;
		boolean dbStoresLowerCaseIdentifiers    = false;
		boolean dbSupportsSchema                = true;
		boolean autoAddDboForSybaseAndSqlServer = true;
		
		SqlObjectName sqlObj = new SqlObjectName("db$name|x.dbo.with space", dbProductName, dbIdentifierQuoteString, dbStoresUpperCaseIdentifiers, dbStoresLowerCaseIdentifiers, dbSupportsSchema, autoAddDboForSybaseAndSqlServer);
		
		// Catalog
		assertEquals( "[db$name|x]",   sqlObj.getCatalogNameQuoted());
		assertEquals( "[db$name|x]",   sqlObj.getCatalogNameQuotedIfNeeded());
		assertEquals( "[db$name|x]",   sqlObj.getCatalogNameOriginQuoted());
		assertEquals( "[db$name|x]",   sqlObj.getCatalogNameOriginQuotedIfNeeded());
		
		// Schema
		assertEquals( "[dbo]" , sqlObj.getSchemaNameQuoted());
		assertEquals( "dbo"   , sqlObj.getSchemaNameQuotedIfNeeded());
		assertEquals( "[dbo]" , sqlObj.getSchemaNameOriginQuoted());
		assertEquals( "dbo"   , sqlObj.getSchemaNameOriginQuotedIfNeeded());
		
		// Object
		assertEquals( "[with space]", sqlObj.getObjectNameQuoted());
		assertEquals( "[with space]", sqlObj.getObjectNameQuotedIfNeeded());
		assertEquals( "[with space]", sqlObj.getObjectNameOriginQuoted());
		assertEquals( "[with space]", sqlObj.getObjectNameOriginQuotedIfNeeded());

		// Full Compound
		assertEquals( "[db$name|x].[dbo].[with space]" , sqlObj.getFullNameQuoted());
		assertEquals( "[db$name|x].dbo.[with space]"   , sqlObj.getFullNameQuotedIfNeeded());
		assertEquals( "[db$name|x].[dbo].[with space]" , sqlObj.getFullNameOriginQuoted());
		assertEquals( "[db$name|x].dbo.[with space]"   , sqlObj.getFullNameOriginQuotedIfNeeded());
	}

	
	//--------------------------------------------------------------------------
	// H2 tests
	//  - H2 normally stores all table and column names as UPPERCASE
	//  - But if it's quoted identifiers, we need the CamelCaseTableNames
	//--------------------------------------------------------------------------
	@Test
	public void h2_t1()
	{
		String dbProductName                    = DbUtils.DB_PROD_NAME_H2;
		String dbIdentifierQuoteString          = "\"";
		boolean dbStoresUpperCaseIdentifiers    = true;
		boolean dbStoresLowerCaseIdentifiers    = false;
		boolean dbSupportsSchema                = true;
		
		SqlObjectName sqlObj = new SqlObjectName("tab1", dbProductName, dbIdentifierQuoteString, dbStoresUpperCaseIdentifiers, dbStoresLowerCaseIdentifiers, dbSupportsSchema);
		
		// Catalog
		assertEquals( "",   sqlObj.getCatalogName());
		assertEquals( null, sqlObj.getCatalogNameNull());
		assertEquals( "",   sqlObj.getCatalogNameOrigin());
		assertEquals( null, sqlObj.getCatalogNameOriginNull());
		assertEquals( "",   sqlObj.getCatalogNameUnModified());
		assertEquals( null, sqlObj.getCatalogNameUnModifiedNull());

		assertEquals( "",   sqlObj.getCatalogNameQuoted());
		assertEquals( "",   sqlObj.getCatalogNameOriginQuoted());

		// Schema
		assertEquals( "",   sqlObj.getSchemaName());
		assertEquals( null, sqlObj.getSchemaNameNull());
		assertEquals( "",   sqlObj.getSchemaNameOrigin());
		assertEquals( null, sqlObj.getSchemaNameOriginNull());
		assertEquals( "",   sqlObj.getSchemaNameUnModified());
		assertEquals( null, sqlObj.getSchemaNameUnModifiedNull());

		assertEquals( "",   sqlObj.getSchemaNameQuoted());
		assertEquals( "",   sqlObj.getSchemaNameOriginQuoted());

		// Object
		assertEquals( "TAB1", sqlObj.getObjectName());
		assertEquals( "TAB1", sqlObj.getObjectNameNull());
		assertEquals( "tab1", sqlObj.getObjectNameOrigin());
		assertEquals( "tab1", sqlObj.getObjectNameOriginNull());
		assertEquals( "tab1", sqlObj.getObjectNameUnModified());
		assertEquals( "tab1", sqlObj.getObjectNameUnModifiedNull());

		// Full Compound
		assertEquals( "TAB1", sqlObj.getFullName());
		assertEquals( "tab1", sqlObj.getFullNameOrigin());
		assertEquals( "tab1", sqlObj.getFullNameUnModified());

		assertEquals( "\"TAB1\"", sqlObj.getFullNameQuoted());
		assertEquals( "\"tab1\"", sqlObj.getFullNameOriginQuoted());

		assertEquals( "TAB1", sqlObj.getFullNameQuotedIfNeeded());
		assertEquals( "tab1", sqlObj.getFullNameOriginQuotedIfNeeded());
	}


	//--------------------------------------------------------------------------
	// MySQL tests
	//--------------------------------------------------------------------------

	//--------------------------------------------------------------------------
	// Postgres tests
	//--------------------------------------------------------------------------
}
